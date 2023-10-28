package com.android.server.display.color;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManagerInternal;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.display.ColorDisplayManager;
import android.hardware.display.IColorDisplayManager;
import android.hardware.display.Time;
import android.net.Uri;
import android.opengl.Matrix;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.MathUtils;
import android.util.Slog;
import android.util.SparseIntArray;
import android.view.SurfaceControl;
import android.view.animation.AnimationUtils;
import com.android.internal.util.DumpUtils;
import com.android.server.DisplayThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.app.GameManagerService;
import com.android.server.twilight.TwilightListener;
import com.android.server.twilight.TwilightManager;
import com.android.server.twilight.TwilightState;
import com.transsion.hubcore.server.display.ITranColorDisplayService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
/* loaded from: classes.dex */
public final class ColorDisplayService extends SystemService {
    private static final ColorMatrixEvaluator COLOR_MATRIX_EVALUATOR;
    private static final float[] MATRIX_GRAYSCALE;
    static final float[] MATRIX_IDENTITY;
    private static final float[] MATRIX_INVERT_COLOR;
    private static final int MSG_APPLY_DISPLAY_WHITE_BALANCE = 5;
    private static final int MSG_APPLY_GLOBAL_SATURATION = 4;
    private static final int MSG_APPLY_NIGHT_DISPLAY_ANIMATED = 3;
    private static final int MSG_APPLY_NIGHT_DISPLAY_IMMEDIATE = 2;
    private static final int MSG_APPLY_REDUCE_BRIGHT_COLORS = 6;
    private static final int MSG_SET_UP = 1;
    private static final int MSG_USER_CHANGED = 0;
    private static final int NOT_SET = -1;
    static final String TAG = "ColorDisplayService";
    private static final long TRANSITION_DURATION = 3000;
    private static final boolean TRAN_DISPLAY_COLOR_MODEL_SUPPORT;
    private static final boolean TRAN_DISPLAY_COLOR_TEMPERATURE_NODE_SUPPORT;
    private final AppSaturationController mAppSaturationController;
    private boolean mBootCompleted;
    private SparseIntArray mColorModeCompositionColorSpaces;
    private ContentObserver mContentObserver;
    private int mCurrentUser;
    private DisplayWhiteBalanceListener mDisplayWhiteBalanceListener;
    final DisplayWhiteBalanceTintController mDisplayWhiteBalanceTintController;
    private final TintController mGlobalSaturationTintController;
    final Handler mHandler;
    private NightDisplayAutoMode mNightDisplayAutoMode;
    private final NightDisplayTintController mNightDisplayTintController;
    private ReduceBrightColorsListener mReduceBrightColorsListener;
    private final ReduceBrightColorsTintController mReduceBrightColorsTintController;
    private ContentObserver mUserSetupObserver;
    private ITranColorDisplayService tranColorDisplayService;

    /* loaded from: classes.dex */
    public interface ColorTransformController {
        void applyAppSaturation(float[] fArr, float[] fArr2);
    }

    /* loaded from: classes.dex */
    public interface DisplayWhiteBalanceListener {
        void onDisplayWhiteBalanceStatusChanged(boolean z);
    }

    /* loaded from: classes.dex */
    public interface ReduceBrightColorsListener {
        void onReduceBrightColorsActivationChanged(boolean z, boolean z2);

        void onReduceBrightColorsStrengthChanged(int i);
    }

    static {
        float[] fArr = new float[16];
        MATRIX_IDENTITY = fArr;
        Matrix.setIdentityM(fArr, 0);
        COLOR_MATRIX_EVALUATOR = new ColorMatrixEvaluator();
        MATRIX_GRAYSCALE = new float[]{0.2126f, 0.2126f, 0.2126f, 0.0f, 0.7152f, 0.7152f, 0.7152f, 0.0f, 0.0722f, 0.0722f, 0.0722f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f};
        MATRIX_INVERT_COLOR = new float[]{0.402f, -0.598f, -0.599f, 0.0f, -1.174f, -0.174f, -1.175f, 0.0f, -0.228f, -0.228f, 0.772f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f};
        TRAN_DISPLAY_COLOR_MODEL_SUPPORT = "1".equals(SystemProperties.get("ro.tran.display.colormode.support", ""));
        TRAN_DISPLAY_COLOR_TEMPERATURE_NODE_SUPPORT = "1".equals(SystemProperties.get("ro.tran.display.color.temperature.support", ""));
    }

    public ColorDisplayService(Context context) {
        super(context);
        this.mDisplayWhiteBalanceTintController = new DisplayWhiteBalanceTintController();
        this.mNightDisplayTintController = new NightDisplayTintController();
        this.mGlobalSaturationTintController = new GlobalSaturationTintController();
        this.mReduceBrightColorsTintController = new ReduceBrightColorsTintController();
        this.mAppSaturationController = new AppSaturationController();
        this.mCurrentUser = -10000;
        this.mColorModeCompositionColorSpaces = null;
        this.tranColorDisplayService = null;
        this.mHandler = new TintHandler(DisplayThread.get().getLooper());
        if (TRAN_DISPLAY_COLOR_MODEL_SUPPORT || TRAN_DISPLAY_COLOR_TEMPERATURE_NODE_SUPPORT) {
            this.tranColorDisplayService = ITranColorDisplayService.Instance();
        }
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("color_display", new BinderService());
        publishLocalService(ColorDisplayServiceInternal.class, new ColorDisplayServiceInternal());
        publishLocalService(DisplayTransformManager.class, new DisplayTransformManager());
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase >= 1000) {
            this.mBootCompleted = true;
            if (this.mCurrentUser != -10000 && this.mUserSetupObserver == null) {
                this.mHandler.sendEmptyMessage(1);
            }
        }
    }

    @Override // com.android.server.SystemService
    public void onUserStarting(SystemService.TargetUser user) {
        if (this.mCurrentUser == -10000) {
            Message message = this.mHandler.obtainMessage(0);
            message.arg1 = user.getUserIdentifier();
            this.mHandler.sendMessage(message);
        }
    }

    @Override // com.android.server.SystemService
    public void onUserSwitching(SystemService.TargetUser from, SystemService.TargetUser to) {
        Message message = this.mHandler.obtainMessage(0);
        message.arg1 = to.getUserIdentifier();
        this.mHandler.sendMessage(message);
    }

    @Override // com.android.server.SystemService
    public void onUserStopping(SystemService.TargetUser user) {
        if (this.mCurrentUser == user.getUserIdentifier()) {
            Message message = this.mHandler.obtainMessage(0);
            message.arg1 = -10000;
            this.mHandler.sendMessage(message);
        }
    }

    void onUserChanged(int userHandle) {
        final ContentResolver cr = getContext().getContentResolver();
        if (this.mCurrentUser != -10000) {
            ContentObserver contentObserver = this.mUserSetupObserver;
            if (contentObserver != null) {
                cr.unregisterContentObserver(contentObserver);
                this.mUserSetupObserver = null;
            } else if (this.mBootCompleted) {
                tearDown();
            }
        }
        this.mCurrentUser = userHandle;
        if (userHandle != -10000) {
            if (!isUserSetupCompleted(cr, userHandle)) {
                this.mUserSetupObserver = new ContentObserver(this.mHandler) { // from class: com.android.server.display.color.ColorDisplayService.1
                    @Override // android.database.ContentObserver
                    public void onChange(boolean selfChange, Uri uri) {
                        if (ColorDisplayService.isUserSetupCompleted(cr, ColorDisplayService.this.mCurrentUser)) {
                            cr.unregisterContentObserver(this);
                            ColorDisplayService.this.mUserSetupObserver = null;
                            if (ColorDisplayService.this.mBootCompleted) {
                                ColorDisplayService.this.setUp();
                            }
                        }
                    }
                };
                cr.registerContentObserver(Settings.Secure.getUriFor("user_setup_complete"), false, this.mUserSetupObserver, this.mCurrentUser);
            } else if (this.mBootCompleted) {
                setUp();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isUserSetupCompleted(ContentResolver cr, int userHandle) {
        return Settings.Secure.getIntForUser(cr, "user_setup_complete", 0, userHandle) == 1;
    }

    private void setUpDisplayCompositionColorSpaces(Resources res) {
        int[] compSpaces;
        this.mColorModeCompositionColorSpaces = null;
        int[] colorModes = res.getIntArray(17236035);
        if (colorModes == null || (compSpaces = res.getIntArray(17236036)) == null) {
            return;
        }
        if (colorModes.length != compSpaces.length) {
            Slog.e(TAG, "Number of composition color spaces doesn't match specified color modes");
            return;
        }
        this.mColorModeCompositionColorSpaces = new SparseIntArray(colorModes.length);
        for (int i = 0; i < colorModes.length; i++) {
            this.mColorModeCompositionColorSpaces.put(colorModes[i], compSpaces[i]);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setUp() {
        ITranColorDisplayService iTranColorDisplayService;
        Slog.d(TAG, "setUp: currentUser=" + this.mCurrentUser);
        if (this.mContentObserver == null) {
            this.mContentObserver = new ContentObserver(this.mHandler) { // from class: com.android.server.display.color.ColorDisplayService.2
                @Override // android.database.ContentObserver
                public void onChange(boolean selfChange, Uri uri) {
                    super.onChange(selfChange, uri);
                    String setting = uri == null ? null : uri.getLastPathSegment();
                    if (setting != null) {
                        char c = 65535;
                        switch (setting.hashCode()) {
                            case -2038150513:
                                if (setting.equals("night_display_auto_mode")) {
                                    c = 2;
                                    break;
                                }
                                break;
                            case -1761668069:
                                if (setting.equals("night_display_custom_end_time")) {
                                    c = 4;
                                    break;
                                }
                                break;
                            case -969458956:
                                if (setting.equals("night_display_color_temperature")) {
                                    c = 1;
                                    break;
                                }
                                break;
                            case -686921934:
                                if (setting.equals("accessibility_display_daltonizer_enabled")) {
                                    c = 7;
                                    break;
                                }
                                break;
                            case -551230169:
                                if (setting.equals("accessibility_display_inversion_enabled")) {
                                    c = 6;
                                    break;
                                }
                                break;
                            case 483353904:
                                if (setting.equals("accessibility_display_daltonizer")) {
                                    c = '\b';
                                    break;
                                }
                                break;
                            case 800115245:
                                if (setting.equals("night_display_activated")) {
                                    c = 0;
                                    break;
                                }
                                break;
                            case 1113469195:
                                if (setting.equals("display_white_balance_enabled")) {
                                    c = '\t';
                                    break;
                                }
                                break;
                            case 1300110529:
                                if (setting.equals("reduce_bright_colors_level")) {
                                    c = 11;
                                    break;
                                }
                                break;
                            case 1561688220:
                                if (setting.equals("display_color_mode")) {
                                    c = 5;
                                    break;
                                }
                                break;
                            case 1578271348:
                                if (setting.equals("night_display_custom_start_time")) {
                                    c = 3;
                                    break;
                                }
                                break;
                            case 1656644750:
                                if (setting.equals("reduce_bright_colors_activated")) {
                                    c = '\n';
                                    break;
                                }
                                break;
                        }
                        switch (c) {
                            case 0:
                                boolean activated = ColorDisplayService.this.mNightDisplayTintController.isActivatedSetting();
                                if (ColorDisplayService.this.mNightDisplayTintController.isActivatedStateNotSet() || ColorDisplayService.this.mNightDisplayTintController.isActivated() != activated) {
                                    ColorDisplayService.this.mNightDisplayTintController.setActivated(Boolean.valueOf(activated));
                                    return;
                                }
                                return;
                            case 1:
                                int temperature = ColorDisplayService.this.mNightDisplayTintController.getColorTemperatureSetting();
                                if (ColorDisplayService.this.mNightDisplayTintController.getColorTemperature() != temperature) {
                                    ColorDisplayService.this.mNightDisplayTintController.onColorTemperatureChanged(temperature);
                                    return;
                                }
                                return;
                            case 2:
                                ColorDisplayService colorDisplayService = ColorDisplayService.this;
                                colorDisplayService.onNightDisplayAutoModeChanged(colorDisplayService.getNightDisplayAutoModeInternal());
                                return;
                            case 3:
                                ColorDisplayService colorDisplayService2 = ColorDisplayService.this;
                                colorDisplayService2.onNightDisplayCustomStartTimeChanged(colorDisplayService2.getNightDisplayCustomStartTimeInternal().getLocalTime());
                                return;
                            case 4:
                                ColorDisplayService colorDisplayService3 = ColorDisplayService.this;
                                colorDisplayService3.onNightDisplayCustomEndTimeChanged(colorDisplayService3.getNightDisplayCustomEndTimeInternal().getLocalTime());
                                return;
                            case 5:
                                ColorDisplayService colorDisplayService4 = ColorDisplayService.this;
                                colorDisplayService4.onDisplayColorModeChanged(colorDisplayService4.getColorModeInternal());
                                return;
                            case 6:
                                ColorDisplayService.this.onAccessibilityInversionChanged();
                                ColorDisplayService.this.onAccessibilityActivated();
                                return;
                            case 7:
                                ColorDisplayService.this.onAccessibilityDaltonizerChanged();
                                ColorDisplayService.this.onAccessibilityActivated();
                                return;
                            case '\b':
                                ColorDisplayService.this.onAccessibilityDaltonizerChanged();
                                return;
                            case '\t':
                                ColorDisplayService.this.updateDisplayWhiteBalanceStatus();
                                return;
                            case '\n':
                                ColorDisplayService.this.onReduceBrightColorsActivationChanged(true);
                                ColorDisplayService.this.mHandler.sendEmptyMessage(6);
                                return;
                            case 11:
                                ColorDisplayService.this.onReduceBrightColorsStrengthLevelChanged();
                                ColorDisplayService.this.mHandler.sendEmptyMessage(6);
                                return;
                            default:
                                return;
                        }
                    }
                }
            };
        }
        ContentResolver cr = getContext().getContentResolver();
        cr.registerContentObserver(Settings.Secure.getUriFor("night_display_activated"), false, this.mContentObserver, this.mCurrentUser);
        cr.registerContentObserver(Settings.Secure.getUriFor("night_display_color_temperature"), false, this.mContentObserver, this.mCurrentUser);
        cr.registerContentObserver(Settings.Secure.getUriFor("night_display_auto_mode"), false, this.mContentObserver, this.mCurrentUser);
        cr.registerContentObserver(Settings.Secure.getUriFor("night_display_custom_start_time"), false, this.mContentObserver, this.mCurrentUser);
        cr.registerContentObserver(Settings.Secure.getUriFor("night_display_custom_end_time"), false, this.mContentObserver, this.mCurrentUser);
        cr.registerContentObserver(Settings.System.getUriFor("display_color_mode"), false, this.mContentObserver, this.mCurrentUser);
        cr.registerContentObserver(Settings.Secure.getUriFor("accessibility_display_inversion_enabled"), false, this.mContentObserver, this.mCurrentUser);
        cr.registerContentObserver(Settings.Secure.getUriFor("accessibility_display_daltonizer_enabled"), false, this.mContentObserver, this.mCurrentUser);
        cr.registerContentObserver(Settings.Secure.getUriFor("accessibility_display_daltonizer"), false, this.mContentObserver, this.mCurrentUser);
        cr.registerContentObserver(Settings.Secure.getUriFor("display_white_balance_enabled"), false, this.mContentObserver, this.mCurrentUser);
        cr.registerContentObserver(Settings.Secure.getUriFor("reduce_bright_colors_activated"), false, this.mContentObserver, this.mCurrentUser);
        cr.registerContentObserver(Settings.Secure.getUriFor("reduce_bright_colors_level"), false, this.mContentObserver, this.mCurrentUser);
        onAccessibilityInversionChanged();
        onAccessibilityDaltonizerChanged();
        setUpDisplayCompositionColorSpaces(getContext().getResources());
        onDisplayColorModeChanged(getColorModeInternal());
        DisplayTransformManager dtm = (DisplayTransformManager) getLocalService(DisplayTransformManager.class);
        if (this.mNightDisplayTintController.isAvailable(getContext())) {
            this.mNightDisplayTintController.setActivated(null);
            this.mNightDisplayTintController.setUp(getContext(), dtm.needsLinearColorMatrix());
            NightDisplayTintController nightDisplayTintController = this.mNightDisplayTintController;
            nightDisplayTintController.setMatrix(nightDisplayTintController.getColorTemperatureSetting());
            onNightDisplayAutoModeChanged(getNightDisplayAutoModeInternal());
            if (this.mNightDisplayTintController.isActivatedStateNotSet()) {
                NightDisplayTintController nightDisplayTintController2 = this.mNightDisplayTintController;
                nightDisplayTintController2.setActivated(Boolean.valueOf(nightDisplayTintController2.isActivatedSetting()));
            }
        }
        if (this.mDisplayWhiteBalanceTintController.isAvailable(getContext())) {
            this.mDisplayWhiteBalanceTintController.setUp(getContext(), true);
            updateDisplayWhiteBalanceStatus();
        }
        if (this.mReduceBrightColorsTintController.isAvailable(getContext())) {
            this.mReduceBrightColorsTintController.setUp(getContext(), dtm.needsLinearColorMatrix());
            onReduceBrightColorsStrengthLevelChanged();
            boolean reset = resetReduceBrightColors();
            if (!reset) {
                onReduceBrightColorsActivationChanged(false);
                this.mHandler.sendEmptyMessage(6);
            }
        }
        boolean reset2 = TRAN_DISPLAY_COLOR_MODEL_SUPPORT;
        if ((reset2 || TRAN_DISPLAY_COLOR_TEMPERATURE_NODE_SUPPORT) && (iTranColorDisplayService = this.tranColorDisplayService) != null) {
            iTranColorDisplayService.init(getContext(), this.mCurrentUser);
        }
    }

    private void tearDown() {
        Slog.d(TAG, "tearDown: currentUser=" + this.mCurrentUser);
        if (this.mContentObserver != null) {
            getContext().getContentResolver().unregisterContentObserver(this.mContentObserver);
        }
        if (this.mNightDisplayTintController.isAvailable(getContext())) {
            NightDisplayAutoMode nightDisplayAutoMode = this.mNightDisplayAutoMode;
            if (nightDisplayAutoMode != null) {
                nightDisplayAutoMode.onStop();
                this.mNightDisplayAutoMode = null;
            }
            this.mNightDisplayTintController.endAnimator();
        }
        if (this.mDisplayWhiteBalanceTintController.isAvailable(getContext())) {
            this.mDisplayWhiteBalanceTintController.endAnimator();
        }
        if (this.mGlobalSaturationTintController.isAvailable(getContext())) {
            this.mGlobalSaturationTintController.setActivated(null);
        }
        if (this.mReduceBrightColorsTintController.isAvailable(getContext())) {
            this.mReduceBrightColorsTintController.setActivated(null);
        }
    }

    private boolean resetReduceBrightColors() {
        if (this.mCurrentUser == -10000) {
            return false;
        }
        boolean isSettingActivated = Settings.Secure.getIntForUser(getContext().getContentResolver(), "reduce_bright_colors_activated", 0, this.mCurrentUser) == 1;
        boolean shouldResetOnReboot = Settings.Secure.getIntForUser(getContext().getContentResolver(), "reduce_bright_colors_persist_across_reboots", 0, this.mCurrentUser) == 0;
        if (isSettingActivated && this.mReduceBrightColorsTintController.isActivatedStateNotSet() && shouldResetOnReboot) {
            return Settings.Secure.putIntForUser(getContext().getContentResolver(), "reduce_bright_colors_activated", 0, this.mCurrentUser);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onNightDisplayAutoModeChanged(int autoMode) {
        Slog.d(TAG, "onNightDisplayAutoModeChanged: autoMode=" + autoMode);
        NightDisplayAutoMode nightDisplayAutoMode = this.mNightDisplayAutoMode;
        if (nightDisplayAutoMode != null) {
            nightDisplayAutoMode.onStop();
            this.mNightDisplayAutoMode = null;
        }
        if (autoMode == 1) {
            this.mNightDisplayAutoMode = new CustomNightDisplayAutoMode();
        } else if (autoMode == 2) {
            this.mNightDisplayAutoMode = new TwilightNightDisplayAutoMode();
        }
        NightDisplayAutoMode nightDisplayAutoMode2 = this.mNightDisplayAutoMode;
        if (nightDisplayAutoMode2 != null) {
            nightDisplayAutoMode2.onStart();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onNightDisplayCustomStartTimeChanged(LocalTime startTime) {
        Slog.d(TAG, "onNightDisplayCustomStartTimeChanged: startTime=" + startTime);
        NightDisplayAutoMode nightDisplayAutoMode = this.mNightDisplayAutoMode;
        if (nightDisplayAutoMode != null) {
            nightDisplayAutoMode.onCustomStartTimeChanged(startTime);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onNightDisplayCustomEndTimeChanged(LocalTime endTime) {
        Slog.d(TAG, "onNightDisplayCustomEndTimeChanged: endTime=" + endTime);
        NightDisplayAutoMode nightDisplayAutoMode = this.mNightDisplayAutoMode;
        if (nightDisplayAutoMode != null) {
            nightDisplayAutoMode.onCustomEndTimeChanged(endTime);
        }
    }

    private int getCompositionColorSpace(int mode) {
        SparseIntArray sparseIntArray = this.mColorModeCompositionColorSpaces;
        if (sparseIntArray == null) {
            return -1;
        }
        return sparseIntArray.get(mode, -1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onDisplayColorModeChanged(int mode) {
        if (mode == -1) {
            return;
        }
        this.mNightDisplayTintController.cancelAnimator();
        this.mDisplayWhiteBalanceTintController.cancelAnimator();
        if (this.mNightDisplayTintController.isAvailable(getContext())) {
            DisplayTransformManager dtm = (DisplayTransformManager) getLocalService(DisplayTransformManager.class);
            this.mNightDisplayTintController.setUp(getContext(), dtm.needsLinearColorMatrix(mode));
            NightDisplayTintController nightDisplayTintController = this.mNightDisplayTintController;
            nightDisplayTintController.setMatrix(nightDisplayTintController.getColorTemperatureSetting());
        }
        DisplayTransformManager dtm2 = (DisplayTransformManager) getLocalService(DisplayTransformManager.class);
        dtm2.setColorMode(mode, this.mNightDisplayTintController.getMatrix(), getCompositionColorSpace(mode));
        if (this.mDisplayWhiteBalanceTintController.isAvailable(getContext())) {
            updateDisplayWhiteBalanceStatus();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onAccessibilityActivated() {
        onDisplayColorModeChanged(getColorModeInternal());
    }

    private boolean isAccessiblityDaltonizerEnabled() {
        return Settings.Secure.getIntForUser(getContext().getContentResolver(), "accessibility_display_daltonizer_enabled", 0, this.mCurrentUser) != 0;
    }

    private boolean isAccessiblityInversionEnabled() {
        return Settings.Secure.getIntForUser(getContext().getContentResolver(), "accessibility_display_inversion_enabled", 0, this.mCurrentUser) != 0;
    }

    private boolean isAccessibilityEnabled() {
        return isAccessiblityDaltonizerEnabled() || isAccessiblityInversionEnabled();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onAccessibilityDaltonizerChanged() {
        int daltonizerMode;
        if (this.mCurrentUser == -10000) {
            return;
        }
        if (isAccessiblityDaltonizerEnabled()) {
            daltonizerMode = Settings.Secure.getIntForUser(getContext().getContentResolver(), "accessibility_display_daltonizer", 12, this.mCurrentUser);
        } else {
            daltonizerMode = -1;
        }
        DisplayTransformManager dtm = (DisplayTransformManager) getLocalService(DisplayTransformManager.class);
        if (daltonizerMode == 0) {
            dtm.setColorMatrix(200, MATRIX_GRAYSCALE);
            dtm.setDaltonizerMode(-1);
            return;
        }
        dtm.setColorMatrix(200, null);
        dtm.setDaltonizerMode(daltonizerMode);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onAccessibilityInversionChanged() {
        if (this.mCurrentUser == -10000) {
            return;
        }
        DisplayTransformManager dtm = (DisplayTransformManager) getLocalService(DisplayTransformManager.class);
        dtm.setColorMatrix(300, isAccessiblityInversionEnabled() ? MATRIX_INVERT_COLOR : null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onReduceBrightColorsActivationChanged(boolean userInitiated) {
        if (this.mCurrentUser == -10000) {
            return;
        }
        boolean activated = Settings.Secure.getIntForUser(getContext().getContentResolver(), "reduce_bright_colors_activated", 0, this.mCurrentUser) == 1;
        this.mReduceBrightColorsTintController.setActivated(Boolean.valueOf(activated));
        ReduceBrightColorsListener reduceBrightColorsListener = this.mReduceBrightColorsListener;
        if (reduceBrightColorsListener != null) {
            reduceBrightColorsListener.onReduceBrightColorsActivationChanged(activated, userInitiated);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onReduceBrightColorsStrengthLevelChanged() {
        if (this.mCurrentUser == -10000) {
            return;
        }
        int strength = Settings.Secure.getIntForUser(getContext().getContentResolver(), "reduce_bright_colors_level", -1, this.mCurrentUser);
        if (strength == -1) {
            strength = getContext().getResources().getInteger(17694922);
        }
        this.mReduceBrightColorsTintController.setMatrix(strength);
        ReduceBrightColorsListener reduceBrightColorsListener = this.mReduceBrightColorsListener;
        if (reduceBrightColorsListener != null) {
            reduceBrightColorsListener.onReduceBrightColorsStrengthChanged(strength);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applyTint(final TintController tintController, boolean immediate) {
        tintController.cancelAnimator();
        final DisplayTransformManager dtm = (DisplayTransformManager) getLocalService(DisplayTransformManager.class);
        float[] from = dtm.getColorMatrix(tintController.getLevel());
        final float[] to = tintController.getMatrix();
        if (immediate) {
            dtm.setColorMatrix(tintController.getLevel(), to);
            return;
        }
        ColorMatrixEvaluator colorMatrixEvaluator = COLOR_MATRIX_EVALUATOR;
        Object[] objArr = new Object[2];
        objArr[0] = from == null ? MATRIX_IDENTITY : from;
        objArr[1] = to;
        TintValueAnimator valueAnimator = TintValueAnimator.ofMatrix(colorMatrixEvaluator, objArr);
        tintController.setAnimator(valueAnimator);
        valueAnimator.setDuration(3000L);
        valueAnimator.setInterpolator(AnimationUtils.loadInterpolator(getContext(), 17563661));
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.display.color.ColorDisplayService$$ExternalSyntheticLambda0
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                ColorDisplayService.lambda$applyTint$0(DisplayTransformManager.this, tintController, valueAnimator2);
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() { // from class: com.android.server.display.color.ColorDisplayService.3
            private boolean mIsCancelled;

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
                this.mIsCancelled = true;
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                TintValueAnimator t = (TintValueAnimator) animator;
                Slog.d(ColorDisplayService.TAG, tintController.getClass().getSimpleName() + " Animation cancelled: " + this.mIsCancelled + " to matrix: " + TintController.matrixToString(to, 16) + " min matrix coefficients: " + TintController.matrixToString(t.getMin(), 16) + " max matrix coefficients: " + TintController.matrixToString(t.getMax(), 16));
                if (!this.mIsCancelled) {
                    dtm.setColorMatrix(tintController.getLevel(), to);
                }
                tintController.setAnimator(null);
            }
        });
        valueAnimator.start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$applyTint$0(DisplayTransformManager dtm, TintController tintController, ValueAnimator animator) {
        float[] value = (float[]) animator.getAnimatedValue();
        dtm.setColorMatrix(tintController.getLevel(), value);
        ((TintValueAnimator) animator).updateMinMaxComponents();
    }

    static LocalDateTime getDateTimeBefore(LocalTime localTime, LocalDateTime compareTime) {
        LocalDateTime ldt = LocalDateTime.of(compareTime.getYear(), compareTime.getMonth(), compareTime.getDayOfMonth(), localTime.getHour(), localTime.getMinute());
        return ldt.isAfter(compareTime) ? ldt.minusDays(1L) : ldt;
    }

    static LocalDateTime getDateTimeAfter(LocalTime localTime, LocalDateTime compareTime) {
        LocalDateTime ldt = LocalDateTime.of(compareTime.getYear(), compareTime.getMonth(), compareTime.getDayOfMonth(), localTime.getHour(), localTime.getMinute());
        return ldt.isBefore(compareTime) ? ldt.plusDays(1L) : ldt;
    }

    void updateDisplayWhiteBalanceStatus() {
        boolean oldActivated = this.mDisplayWhiteBalanceTintController.isActivated();
        DisplayTransformManager dtm = (DisplayTransformManager) getLocalService(DisplayTransformManager.class);
        this.mDisplayWhiteBalanceTintController.setActivated(Boolean.valueOf(isDisplayWhiteBalanceSettingEnabled() && !this.mNightDisplayTintController.isActivated() && !isAccessibilityEnabled() && dtm.needsLinearColorMatrix()));
        boolean activated = this.mDisplayWhiteBalanceTintController.isActivated();
        DisplayWhiteBalanceListener displayWhiteBalanceListener = this.mDisplayWhiteBalanceListener;
        if (displayWhiteBalanceListener != null && oldActivated != activated) {
            displayWhiteBalanceListener.onDisplayWhiteBalanceStatusChanged(activated);
        }
        if (!activated) {
            this.mHandler.sendEmptyMessage(5);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setDisplayWhiteBalanceSettingEnabled(boolean enabled) {
        if (this.mCurrentUser == -10000) {
            return false;
        }
        return Settings.Secure.putIntForUser(getContext().getContentResolver(), "display_white_balance_enabled", enabled ? 1 : 0, this.mCurrentUser);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isDisplayWhiteBalanceSettingEnabled() {
        if (this.mCurrentUser == -10000) {
            return false;
        }
        return Settings.Secure.getIntForUser(getContext().getContentResolver(), "display_white_balance_enabled", getContext().getResources().getBoolean(17891605) ? 1 : 0, this.mCurrentUser) == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setReduceBrightColorsActivatedInternal(boolean activated) {
        if (this.mCurrentUser == -10000) {
            return false;
        }
        return Settings.Secure.putIntForUser(getContext().getContentResolver(), "reduce_bright_colors_activated", activated ? 1 : 0, this.mCurrentUser);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setReduceBrightColorsStrengthInternal(int strength) {
        if (this.mCurrentUser == -10000) {
            return false;
        }
        return Settings.Secure.putIntForUser(getContext().getContentResolver(), "reduce_bright_colors_level", strength, this.mCurrentUser);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isDeviceColorManagedInternal() {
        DisplayTransformManager dtm = (DisplayTransformManager) getLocalService(DisplayTransformManager.class);
        return dtm.isDeviceColorManaged();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getTransformCapabilitiesInternal() {
        int availabilityFlags = 0;
        if (SurfaceControl.getProtectedContentSupport()) {
            availabilityFlags = 0 | 1;
        }
        Resources res = getContext().getResources();
        if (res.getBoolean(17891741)) {
            availabilityFlags |= 2;
        }
        if (res.getBoolean(17891742)) {
            return availabilityFlags | 4;
        }
        return availabilityFlags;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setNightDisplayAutoModeInternal(int autoMode) {
        if (getNightDisplayAutoModeInternal() != autoMode) {
            Settings.Secure.putStringForUser(getContext().getContentResolver(), "night_display_last_activated_time", null, this.mCurrentUser);
        }
        return Settings.Secure.putIntForUser(getContext().getContentResolver(), "night_display_auto_mode", autoMode, this.mCurrentUser);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getNightDisplayAutoModeInternal() {
        int autoMode = getNightDisplayAutoModeRawInternal();
        if (autoMode == -1) {
            autoMode = getContext().getResources().getInteger(17694787);
        }
        if (autoMode != 0 && autoMode != 1 && autoMode != 2) {
            Slog.e(TAG, "Invalid autoMode: " + autoMode);
            return 0;
        }
        return autoMode;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getNightDisplayAutoModeRawInternal() {
        if (this.mCurrentUser == -10000) {
            return -1;
        }
        return Settings.Secure.getIntForUser(getContext().getContentResolver(), "night_display_auto_mode", -1, this.mCurrentUser);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Time getNightDisplayCustomStartTimeInternal() {
        int startTimeValue = Settings.Secure.getIntForUser(getContext().getContentResolver(), "night_display_custom_start_time", -1, this.mCurrentUser);
        if (startTimeValue == -1) {
            startTimeValue = getContext().getResources().getInteger(17694789);
        }
        return new Time(LocalTime.ofSecondOfDay(startTimeValue / 1000));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setNightDisplayCustomStartTimeInternal(Time startTime) {
        return Settings.Secure.putIntForUser(getContext().getContentResolver(), "night_display_custom_start_time", startTime.getLocalTime().toSecondOfDay() * 1000, this.mCurrentUser);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Time getNightDisplayCustomEndTimeInternal() {
        int endTimeValue = Settings.Secure.getIntForUser(getContext().getContentResolver(), "night_display_custom_end_time", -1, this.mCurrentUser);
        if (endTimeValue == -1) {
            endTimeValue = getContext().getResources().getInteger(17694788);
        }
        return new Time(LocalTime.ofSecondOfDay(endTimeValue / 1000));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setNightDisplayCustomEndTimeInternal(Time endTime) {
        return Settings.Secure.putIntForUser(getContext().getContentResolver(), "night_display_custom_end_time", endTime.getLocalTime().toSecondOfDay() * 1000, this.mCurrentUser);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public LocalDateTime getNightDisplayLastActivatedTimeSetting() {
        ContentResolver cr = getContext().getContentResolver();
        String lastActivatedTime = Settings.Secure.getStringForUser(cr, "night_display_last_activated_time", getContext().getUserId());
        if (lastActivatedTime != null) {
            try {
                return LocalDateTime.parse(lastActivatedTime);
            } catch (DateTimeParseException e) {
                try {
                    return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(lastActivatedTime)), ZoneId.systemDefault());
                } catch (NumberFormatException | DateTimeException e2) {
                }
            }
        }
        return LocalDateTime.MIN;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSaturationLevelInternal(int saturationLevel) {
        Message message = this.mHandler.obtainMessage(4);
        message.arg1 = saturationLevel;
        this.mHandler.sendMessage(message);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setAppSaturationLevelInternal(String callingPackageName, String affectedPackageName, int saturationLevel) {
        return this.mAppSaturationController.setSaturationLevel(callingPackageName, affectedPackageName, this.mCurrentUser, saturationLevel);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setColorModeInternal(int colorMode) {
        if (!isColorModeAvailable(colorMode)) {
            throw new IllegalArgumentException("Invalid colorMode: " + colorMode);
        }
        Settings.System.putIntForUser(getContext().getContentResolver(), "display_color_mode", colorMode, this.mCurrentUser);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getColorModeInternal() {
        int a11yColorMode;
        ContentResolver cr = getContext().getContentResolver();
        if (isAccessibilityEnabled() && (a11yColorMode = getContext().getResources().getInteger(17694728)) >= 0) {
            return a11yColorMode;
        }
        int colorMode = Settings.System.getIntForUser(cr, "display_color_mode", -1, this.mCurrentUser);
        if (colorMode == -1) {
            colorMode = getCurrentColorModeFromSystemProperties();
        }
        if (!isColorModeAvailable(colorMode)) {
            int[] mappedColorModes = getContext().getResources().getIntArray(17236089);
            if (colorMode != -1 && mappedColorModes.length > colorMode && isColorModeAvailable(mappedColorModes[colorMode])) {
                return mappedColorModes[colorMode];
            }
            int[] availableColorModes = getContext().getResources().getIntArray(17235996);
            if (availableColorModes.length > 0) {
                return availableColorModes[0];
            }
            return -1;
        }
        return colorMode;
    }

    private int getCurrentColorModeFromSystemProperties() {
        int displayColorSetting = SystemProperties.getInt("persist.sys.sf.native_mode", 0);
        if (displayColorSetting == 0) {
            return GameManagerService.GamePackageConfiguration.GameModeConfiguration.DEFAULT_SCALING.equals(SystemProperties.get("persist.sys.sf.color_saturation")) ? 0 : 1;
        } else if (displayColorSetting == 1) {
            return 2;
        } else {
            if (displayColorSetting == 2) {
                return 3;
            }
            if (displayColorSetting >= 256 && displayColorSetting <= 511) {
                return displayColorSetting;
            }
            return -1;
        }
    }

    private boolean isColorModeAvailable(int colorMode) {
        int[] availableColorModes = getContext().getResources().getIntArray(17235996);
        if (availableColorModes != null) {
            for (int mode : availableColorModes) {
                if (mode == colorMode) {
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpInternal(PrintWriter pw) {
        pw.println("COLOR DISPLAY MANAGER dumpsys (color_display)");
        pw.println("Night display:");
        if (this.mNightDisplayTintController.isAvailable(getContext())) {
            pw.println("    Activated: " + this.mNightDisplayTintController.isActivated());
            pw.println("    Color temp: " + this.mNightDisplayTintController.getColorTemperature());
        } else {
            pw.println("    Not available");
        }
        pw.println("Global saturation:");
        if (this.mGlobalSaturationTintController.isAvailable(getContext())) {
            pw.println("    Activated: " + this.mGlobalSaturationTintController.isActivated());
        } else {
            pw.println("    Not available");
        }
        this.mAppSaturationController.dump(pw);
        pw.println("Display white balance:");
        if (this.mDisplayWhiteBalanceTintController.isAvailable(getContext())) {
            pw.println("    Activated: " + this.mDisplayWhiteBalanceTintController.isActivated());
            this.mDisplayWhiteBalanceTintController.dump(pw);
        } else {
            pw.println("    Not available");
        }
        pw.println("Reduce bright colors:");
        if (this.mReduceBrightColorsTintController.isAvailable(getContext())) {
            pw.println("    Activated: " + this.mReduceBrightColorsTintController.isActivated());
            this.mReduceBrightColorsTintController.dump(pw);
        } else {
            pw.println("    Not available");
        }
        pw.println("Color mode: " + getColorModeInternal());
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public abstract class NightDisplayAutoMode {
        public abstract void onActivated(boolean z);

        public abstract void onStart();

        public abstract void onStop();

        private NightDisplayAutoMode() {
        }

        public void onCustomStartTimeChanged(LocalTime startTime) {
        }

        public void onCustomEndTimeChanged(LocalTime endTime) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class CustomNightDisplayAutoMode extends NightDisplayAutoMode implements AlarmManager.OnAlarmListener {
        private final AlarmManager mAlarmManager;
        private LocalTime mEndTime;
        private LocalDateTime mLastActivatedTime;
        private LocalTime mStartTime;
        private final BroadcastReceiver mTimeChangedReceiver;

        CustomNightDisplayAutoMode() {
            super();
            this.mAlarmManager = (AlarmManager) ColorDisplayService.this.getContext().getSystemService("alarm");
            this.mTimeChangedReceiver = new BroadcastReceiver() { // from class: com.android.server.display.color.ColorDisplayService.CustomNightDisplayAutoMode.1
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    CustomNightDisplayAutoMode.this.updateActivated();
                }
            };
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateActivated() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = ColorDisplayService.getDateTimeBefore(this.mStartTime, now);
            LocalDateTime end = ColorDisplayService.getDateTimeAfter(this.mEndTime, start);
            boolean activate = now.isBefore(end);
            LocalDateTime localDateTime = this.mLastActivatedTime;
            if (localDateTime != null && localDateTime.isBefore(now) && this.mLastActivatedTime.isAfter(start) && (this.mLastActivatedTime.isAfter(end) || now.isBefore(end))) {
                activate = ColorDisplayService.this.mNightDisplayTintController.isActivatedSetting();
            }
            if (ColorDisplayService.this.mNightDisplayTintController.isActivatedStateNotSet() || ColorDisplayService.this.mNightDisplayTintController.isActivated() != activate) {
                ColorDisplayService.this.mNightDisplayTintController.setActivated(Boolean.valueOf(activate), activate ? start : end);
            }
            updateNextAlarm(Boolean.valueOf(ColorDisplayService.this.mNightDisplayTintController.isActivated()), now);
        }

        private void updateNextAlarm(Boolean activated, LocalDateTime now) {
            if (activated != null) {
                LocalDateTime next = activated.booleanValue() ? ColorDisplayService.getDateTimeAfter(this.mEndTime, now) : ColorDisplayService.getDateTimeAfter(this.mStartTime, now);
                long millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                this.mAlarmManager.setExact(1, millis, ColorDisplayService.TAG, this, null);
            }
        }

        @Override // com.android.server.display.color.ColorDisplayService.NightDisplayAutoMode
        public void onStart() {
            IntentFilter intentFilter = new IntentFilter("android.intent.action.TIME_SET");
            intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
            ColorDisplayService.this.getContext().registerReceiver(this.mTimeChangedReceiver, intentFilter);
            this.mStartTime = ColorDisplayService.this.getNightDisplayCustomStartTimeInternal().getLocalTime();
            this.mEndTime = ColorDisplayService.this.getNightDisplayCustomEndTimeInternal().getLocalTime();
            this.mLastActivatedTime = ColorDisplayService.this.getNightDisplayLastActivatedTimeSetting();
            updateActivated();
        }

        @Override // com.android.server.display.color.ColorDisplayService.NightDisplayAutoMode
        public void onStop() {
            ColorDisplayService.this.getContext().unregisterReceiver(this.mTimeChangedReceiver);
            this.mAlarmManager.cancel(this);
            this.mLastActivatedTime = null;
        }

        @Override // com.android.server.display.color.ColorDisplayService.NightDisplayAutoMode
        public void onActivated(boolean activated) {
            this.mLastActivatedTime = ColorDisplayService.this.getNightDisplayLastActivatedTimeSetting();
            updateNextAlarm(Boolean.valueOf(activated), LocalDateTime.now());
        }

        @Override // com.android.server.display.color.ColorDisplayService.NightDisplayAutoMode
        public void onCustomStartTimeChanged(LocalTime startTime) {
            this.mStartTime = startTime;
            this.mLastActivatedTime = null;
            updateActivated();
        }

        @Override // com.android.server.display.color.ColorDisplayService.NightDisplayAutoMode
        public void onCustomEndTimeChanged(LocalTime endTime) {
            this.mEndTime = endTime;
            this.mLastActivatedTime = null;
            updateActivated();
        }

        @Override // android.app.AlarmManager.OnAlarmListener
        public void onAlarm() {
            Slog.d(ColorDisplayService.TAG, "onAlarm");
            updateActivated();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class TwilightNightDisplayAutoMode extends NightDisplayAutoMode implements TwilightListener {
        private LocalDateTime mLastActivatedTime;
        private final TwilightManager mTwilightManager;

        TwilightNightDisplayAutoMode() {
            super();
            this.mTwilightManager = (TwilightManager) ColorDisplayService.this.getLocalService(TwilightManager.class);
        }

        private void updateActivated(TwilightState state) {
            if (state == null) {
                return;
            }
            boolean activate = state.isNight();
            if (this.mLastActivatedTime != null) {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime sunrise = state.sunrise();
                LocalDateTime sunset = state.sunset();
                if (this.mLastActivatedTime.isBefore(now) && (this.mLastActivatedTime.isBefore(sunrise) ^ this.mLastActivatedTime.isBefore(sunset))) {
                    activate = ColorDisplayService.this.mNightDisplayTintController.isActivatedSetting();
                }
            }
            if (ColorDisplayService.this.mNightDisplayTintController.isActivatedStateNotSet() || ColorDisplayService.this.mNightDisplayTintController.isActivated() != activate) {
                ColorDisplayService.this.mNightDisplayTintController.setActivated(Boolean.valueOf(activate));
            }
        }

        @Override // com.android.server.display.color.ColorDisplayService.NightDisplayAutoMode
        public void onActivated(boolean activated) {
            this.mLastActivatedTime = ColorDisplayService.this.getNightDisplayLastActivatedTimeSetting();
        }

        @Override // com.android.server.display.color.ColorDisplayService.NightDisplayAutoMode
        public void onStart() {
            this.mTwilightManager.registerListener(this, ColorDisplayService.this.mHandler);
            this.mLastActivatedTime = ColorDisplayService.this.getNightDisplayLastActivatedTimeSetting();
            updateActivated(this.mTwilightManager.getLastTwilightState());
        }

        @Override // com.android.server.display.color.ColorDisplayService.NightDisplayAutoMode
        public void onStop() {
            this.mTwilightManager.unregisterListener(this);
            this.mLastActivatedTime = null;
        }

        @Override // com.android.server.twilight.TwilightListener
        public void onTwilightStateChanged(TwilightState state) {
            Slog.d(ColorDisplayService.TAG, "onTwilightStateChanged: isNight=" + (state == null ? null : Boolean.valueOf(state.isNight())));
            updateActivated(state);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class TintValueAnimator extends ValueAnimator {
        private float[] max;
        private float[] min;

        TintValueAnimator() {
        }

        public static TintValueAnimator ofMatrix(ColorMatrixEvaluator evaluator, Object... values) {
            TintValueAnimator anim = new TintValueAnimator();
            anim.setObjectValues(values);
            anim.setEvaluator(evaluator);
            if (values == null || values.length == 0) {
                return null;
            }
            float[] m = (float[]) values[0];
            anim.min = new float[m.length];
            anim.max = new float[m.length];
            for (int i = 0; i < m.length; i++) {
                anim.min[i] = Float.MAX_VALUE;
                anim.max[i] = Float.MIN_VALUE;
            }
            return anim;
        }

        public void updateMinMaxComponents() {
            float[] value = (float[]) getAnimatedValue();
            if (value == null) {
                return;
            }
            for (int i = 0; i < value.length; i++) {
                float[] fArr = this.min;
                fArr[i] = Math.min(fArr[i], value[i]);
                float[] fArr2 = this.max;
                fArr2[i] = Math.max(fArr2[i], value[i]);
            }
        }

        public float[] getMin() {
            return this.min;
        }

        public float[] getMax() {
            return this.max;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ColorMatrixEvaluator implements TypeEvaluator<float[]> {
        private final float[] mResultMatrix;

        private ColorMatrixEvaluator() {
            this.mResultMatrix = new float[16];
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.animation.TypeEvaluator
        public float[] evaluate(float fraction, float[] startValue, float[] endValue) {
            int i = 0;
            while (true) {
                float[] fArr = this.mResultMatrix;
                if (i < fArr.length) {
                    fArr[i] = MathUtils.lerp(startValue[i], endValue[i], fraction);
                    i++;
                } else {
                    return fArr;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class NightDisplayTintController extends TintController {
        private Integer mColorTemp;
        private final float[] mColorTempCoefficients;
        private Boolean mIsAvailable;
        private final float[] mMatrix;

        private NightDisplayTintController() {
            this.mMatrix = new float[16];
            this.mColorTempCoefficients = new float[9];
        }

        @Override // com.android.server.display.color.TintController
        public void setUp(Context context, boolean needsLinear) {
            int i;
            Resources resources = context.getResources();
            if (needsLinear) {
                i = 17236098;
            } else {
                i = 17236099;
            }
            String[] coefficients = resources.getStringArray(i);
            for (int i2 = 0; i2 < 9 && i2 < coefficients.length; i2++) {
                this.mColorTempCoefficients[i2] = Float.parseFloat(coefficients[i2]);
            }
        }

        @Override // com.android.server.display.color.TintController
        public void setMatrix(int cct) {
            float[] fArr = this.mMatrix;
            if (fArr.length != 16) {
                Slog.d(ColorDisplayService.TAG, "The display transformation matrix must be 4x4");
                return;
            }
            Matrix.setIdentityM(fArr, 0);
            float squareTemperature = cct * cct;
            float[] fArr2 = this.mColorTempCoefficients;
            float red = (fArr2[0] * squareTemperature) + (cct * fArr2[1]) + fArr2[2];
            float green = (fArr2[3] * squareTemperature) + (cct * fArr2[4]) + fArr2[5];
            float blue = (fArr2[6] * squareTemperature) + (cct * fArr2[7]) + fArr2[8];
            float[] fArr3 = this.mMatrix;
            fArr3[0] = red;
            fArr3[5] = green;
            fArr3[10] = blue;
        }

        @Override // com.android.server.display.color.TintController
        public float[] getMatrix() {
            return isActivated() ? this.mMatrix : ColorDisplayService.MATRIX_IDENTITY;
        }

        @Override // com.android.server.display.color.TintController
        public void setActivated(Boolean activated) {
            setActivated(activated, LocalDateTime.now());
        }

        public void setActivated(Boolean activated, LocalDateTime lastActivationTime) {
            if (activated == null) {
                super.setActivated(null);
                return;
            }
            boolean activationStateChanged = activated.booleanValue() != isActivated();
            if (!isActivatedStateNotSet() && activationStateChanged) {
                Settings.Secure.putStringForUser(ColorDisplayService.this.getContext().getContentResolver(), "night_display_last_activated_time", lastActivationTime.toString(), ColorDisplayService.this.mCurrentUser);
            }
            if (isActivatedStateNotSet() || activationStateChanged) {
                super.setActivated(activated);
                if (isActivatedSetting() != activated.booleanValue()) {
                    Settings.Secure.putIntForUser(ColorDisplayService.this.getContext().getContentResolver(), "night_display_activated", activated.booleanValue() ? 1 : 0, ColorDisplayService.this.mCurrentUser);
                }
                onActivated(activated.booleanValue());
            }
        }

        @Override // com.android.server.display.color.TintController
        public int getLevel() {
            return 100;
        }

        @Override // com.android.server.display.color.TintController
        public boolean isAvailable(Context context) {
            if (this.mIsAvailable == null) {
                this.mIsAvailable = Boolean.valueOf(ColorDisplayManager.isNightDisplayAvailable(context));
            }
            return this.mIsAvailable.booleanValue();
        }

        private void onActivated(boolean activated) {
            Slog.i(ColorDisplayService.TAG, activated ? "Turning on night display" : "Turning off night display");
            if (ColorDisplayService.this.mNightDisplayAutoMode != null) {
                ColorDisplayService.this.mNightDisplayAutoMode.onActivated(activated);
            }
            if (ColorDisplayService.this.mDisplayWhiteBalanceTintController.isAvailable(ColorDisplayService.this.getContext())) {
                ColorDisplayService.this.updateDisplayWhiteBalanceStatus();
            }
            ColorDisplayService.this.mHandler.sendEmptyMessage(3);
        }

        int getColorTemperature() {
            Integer num = this.mColorTemp;
            return num != null ? clampNightDisplayColorTemperature(num.intValue()) : getColorTemperatureSetting();
        }

        boolean setColorTemperature(int temperature) {
            this.mColorTemp = Integer.valueOf(temperature);
            boolean success = Settings.Secure.putIntForUser(ColorDisplayService.this.getContext().getContentResolver(), "night_display_color_temperature", temperature, ColorDisplayService.this.mCurrentUser);
            onColorTemperatureChanged(temperature);
            return success;
        }

        void onColorTemperatureChanged(int temperature) {
            setMatrix(temperature);
            ColorDisplayService.this.mHandler.sendEmptyMessage(2);
        }

        boolean isActivatedSetting() {
            return ColorDisplayService.this.mCurrentUser != -10000 && Settings.Secure.getIntForUser(ColorDisplayService.this.getContext().getContentResolver(), "night_display_activated", 0, ColorDisplayService.this.mCurrentUser) == 1;
        }

        int getColorTemperatureSetting() {
            if (ColorDisplayService.this.mCurrentUser == -10000) {
                return -1;
            }
            return clampNightDisplayColorTemperature(Settings.Secure.getIntForUser(ColorDisplayService.this.getContext().getContentResolver(), "night_display_color_temperature", -1, ColorDisplayService.this.mCurrentUser));
        }

        private int clampNightDisplayColorTemperature(int colorTemperature) {
            if (colorTemperature == -1) {
                colorTemperature = ColorDisplayService.this.getContext().getResources().getInteger(17694893);
            }
            int minimumTemperature = ColorDisplayManager.getMinimumColorTemperature(ColorDisplayService.this.getContext());
            int maximumTemperature = ColorDisplayManager.getMaximumColorTemperature(ColorDisplayService.this.getContext());
            if (colorTemperature < minimumTemperature) {
                return minimumTemperature;
            }
            if (colorTemperature > maximumTemperature) {
                return maximumTemperature;
            }
            return colorTemperature;
        }
    }

    /* loaded from: classes.dex */
    public class ColorDisplayServiceInternal {
        public ColorDisplayServiceInternal() {
        }

        public boolean setDisplayWhiteBalanceColorTemperature(int cct) {
            ColorDisplayService.this.mDisplayWhiteBalanceTintController.setMatrix(cct);
            if (ColorDisplayService.this.mDisplayWhiteBalanceTintController.isActivated()) {
                ColorDisplayService.this.mHandler.sendEmptyMessage(5);
                return true;
            }
            return false;
        }

        public float getDisplayWhiteBalanceLuminance() {
            return ColorDisplayService.this.mDisplayWhiteBalanceTintController.getLuminance();
        }

        public boolean resetDisplayWhiteBalanceColorTemperature() {
            int temperatureDefault = ColorDisplayService.this.getContext().getResources().getInteger(17694809);
            Slog.d(ColorDisplayService.TAG, "resetDisplayWhiteBalanceColorTemperature: " + temperatureDefault);
            return setDisplayWhiteBalanceColorTemperature(temperatureDefault);
        }

        public boolean setDisplayWhiteBalanceListener(DisplayWhiteBalanceListener listener) {
            ColorDisplayService.this.mDisplayWhiteBalanceListener = listener;
            return ColorDisplayService.this.mDisplayWhiteBalanceTintController.isActivated();
        }

        public boolean isDisplayWhiteBalanceEnabled() {
            return ColorDisplayService.this.isDisplayWhiteBalanceSettingEnabled();
        }

        public boolean setReduceBrightColorsListener(ReduceBrightColorsListener listener) {
            ColorDisplayService.this.mReduceBrightColorsListener = listener;
            return ColorDisplayService.this.mReduceBrightColorsTintController.isActivated();
        }

        public boolean isReduceBrightColorsActivated() {
            return ColorDisplayService.this.mReduceBrightColorsTintController.isActivated();
        }

        public float getReduceBrightColorsAdjustedBrightnessNits(float nits) {
            return ColorDisplayService.this.mReduceBrightColorsTintController.getAdjustedBrightness(nits);
        }

        public boolean attachColorTransformController(String packageName, int userId, WeakReference<ColorTransformController> controller) {
            return ColorDisplayService.this.mAppSaturationController.addColorTransformController(packageName, userId, controller);
        }
    }

    /* loaded from: classes.dex */
    private final class TintHandler extends Handler {
        private TintHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    ColorDisplayService.this.onUserChanged(msg.arg1);
                    return;
                case 1:
                    ColorDisplayService.this.setUp();
                    return;
                case 2:
                    ColorDisplayService colorDisplayService = ColorDisplayService.this;
                    colorDisplayService.applyTint(colorDisplayService.mNightDisplayTintController, true);
                    return;
                case 3:
                    ColorDisplayService colorDisplayService2 = ColorDisplayService.this;
                    colorDisplayService2.applyTint(colorDisplayService2.mNightDisplayTintController, false);
                    return;
                case 4:
                    ColorDisplayService.this.mGlobalSaturationTintController.setMatrix(msg.arg1);
                    ColorDisplayService colorDisplayService3 = ColorDisplayService.this;
                    colorDisplayService3.applyTint(colorDisplayService3.mGlobalSaturationTintController, false);
                    return;
                case 5:
                    ColorDisplayService colorDisplayService4 = ColorDisplayService.this;
                    colorDisplayService4.applyTint(colorDisplayService4.mDisplayWhiteBalanceTintController, false);
                    return;
                case 6:
                    ColorDisplayService colorDisplayService5 = ColorDisplayService.this;
                    colorDisplayService5.applyTint(colorDisplayService5.mReduceBrightColorsTintController, true);
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    final class BinderService extends IColorDisplayManager.Stub {
        BinderService() {
        }

        public void setColorMode(int colorMode) {
            ColorDisplayService.this.getContext().enforceCallingOrSelfPermission("android.permission.CONTROL_DISPLAY_COLOR_TRANSFORMS", "Permission required to set display color mode");
            long token = Binder.clearCallingIdentity();
            try {
                ColorDisplayService.this.setColorModeInternal(colorMode);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public int getColorMode() {
            long token = Binder.clearCallingIdentity();
            try {
                return ColorDisplayService.this.getColorModeInternal();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean isDeviceColorManaged() {
            long token = Binder.clearCallingIdentity();
            try {
                return ColorDisplayService.this.isDeviceColorManagedInternal();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean setSaturationLevel(int level) {
            boolean hasTransformsPermission = ColorDisplayService.this.getContext().checkCallingPermission("android.permission.CONTROL_DISPLAY_COLOR_TRANSFORMS") == 0;
            boolean hasLegacyPermission = ColorDisplayService.this.getContext().checkCallingPermission("android.permission.CONTROL_DISPLAY_SATURATION") == 0;
            if (!hasTransformsPermission && !hasLegacyPermission) {
                throw new SecurityException("Permission required to set display saturation level");
            }
            long token = Binder.clearCallingIdentity();
            try {
                ColorDisplayService.this.setSaturationLevelInternal(level);
                return true;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean isSaturationActivated() {
            boolean z;
            ColorDisplayService.this.getContext().enforceCallingPermission("android.permission.CONTROL_DISPLAY_COLOR_TRANSFORMS", "Permission required to get display saturation level");
            long token = Binder.clearCallingIdentity();
            try {
                if (!ColorDisplayService.this.mGlobalSaturationTintController.isActivatedStateNotSet()) {
                    if (ColorDisplayService.this.mGlobalSaturationTintController.isActivated()) {
                        z = true;
                        return z;
                    }
                }
                z = false;
                return z;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean setAppSaturationLevel(String packageName, int level) {
            ColorDisplayService.this.getContext().enforceCallingPermission("android.permission.CONTROL_DISPLAY_COLOR_TRANSFORMS", "Permission required to set display saturation level");
            String callingPackageName = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getNameForUid(Binder.getCallingUid());
            long token = Binder.clearCallingIdentity();
            try {
                return ColorDisplayService.this.setAppSaturationLevelInternal(callingPackageName, packageName, level);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public int getTransformCapabilities() {
            ColorDisplayService.this.getContext().enforceCallingPermission("android.permission.CONTROL_DISPLAY_COLOR_TRANSFORMS", "Permission required to query transform capabilities");
            long token = Binder.clearCallingIdentity();
            try {
                return ColorDisplayService.this.getTransformCapabilitiesInternal();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean setNightDisplayActivated(boolean activated) {
            ColorDisplayService.this.getContext().enforceCallingOrSelfPermission("android.permission.CONTROL_DISPLAY_COLOR_TRANSFORMS", "Permission required to set night display activated");
            long token = Binder.clearCallingIdentity();
            try {
                ColorDisplayService.this.mNightDisplayTintController.setActivated(Boolean.valueOf(activated));
                return true;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean isNightDisplayActivated() {
            long token = Binder.clearCallingIdentity();
            try {
                return ColorDisplayService.this.mNightDisplayTintController.isActivated();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean setNightDisplayColorTemperature(int temperature) {
            ColorDisplayService.this.getContext().enforceCallingOrSelfPermission("android.permission.CONTROL_DISPLAY_COLOR_TRANSFORMS", "Permission required to set night display temperature");
            long token = Binder.clearCallingIdentity();
            try {
                return ColorDisplayService.this.mNightDisplayTintController.setColorTemperature(temperature);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public int getNightDisplayColorTemperature() {
            long token = Binder.clearCallingIdentity();
            try {
                return ColorDisplayService.this.mNightDisplayTintController.getColorTemperature();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean setNightDisplayAutoMode(int autoMode) {
            ColorDisplayService.this.getContext().enforceCallingOrSelfPermission("android.permission.CONTROL_DISPLAY_COLOR_TRANSFORMS", "Permission required to set night display auto mode");
            long token = Binder.clearCallingIdentity();
            try {
                return ColorDisplayService.this.setNightDisplayAutoModeInternal(autoMode);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public int getNightDisplayAutoMode() {
            ColorDisplayService.this.getContext().enforceCallingOrSelfPermission("android.permission.CONTROL_DISPLAY_COLOR_TRANSFORMS", "Permission required to get night display auto mode");
            long token = Binder.clearCallingIdentity();
            try {
                return ColorDisplayService.this.getNightDisplayAutoModeInternal();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public int getNightDisplayAutoModeRaw() {
            long token = Binder.clearCallingIdentity();
            try {
                return ColorDisplayService.this.getNightDisplayAutoModeRawInternal();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean setNightDisplayCustomStartTime(Time startTime) {
            ColorDisplayService.this.getContext().enforceCallingOrSelfPermission("android.permission.CONTROL_DISPLAY_COLOR_TRANSFORMS", "Permission required to set night display custom start time");
            long token = Binder.clearCallingIdentity();
            try {
                return ColorDisplayService.this.setNightDisplayCustomStartTimeInternal(startTime);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public Time getNightDisplayCustomStartTime() {
            long token = Binder.clearCallingIdentity();
            try {
                return ColorDisplayService.this.getNightDisplayCustomStartTimeInternal();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean setNightDisplayCustomEndTime(Time endTime) {
            ColorDisplayService.this.getContext().enforceCallingOrSelfPermission("android.permission.CONTROL_DISPLAY_COLOR_TRANSFORMS", "Permission required to set night display custom end time");
            long token = Binder.clearCallingIdentity();
            try {
                return ColorDisplayService.this.setNightDisplayCustomEndTimeInternal(endTime);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public Time getNightDisplayCustomEndTime() {
            long token = Binder.clearCallingIdentity();
            try {
                return ColorDisplayService.this.getNightDisplayCustomEndTimeInternal();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean setDisplayWhiteBalanceEnabled(boolean enabled) {
            ColorDisplayService.this.getContext().enforceCallingOrSelfPermission("android.permission.CONTROL_DISPLAY_COLOR_TRANSFORMS", "Permission required to set night display activated");
            long token = Binder.clearCallingIdentity();
            try {
                return ColorDisplayService.this.setDisplayWhiteBalanceSettingEnabled(enabled);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean isDisplayWhiteBalanceEnabled() {
            long token = Binder.clearCallingIdentity();
            try {
                return ColorDisplayService.this.isDisplayWhiteBalanceSettingEnabled();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean isReduceBrightColorsActivated() {
            long token = Binder.clearCallingIdentity();
            try {
                return ColorDisplayService.this.mReduceBrightColorsTintController.isActivated();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean setReduceBrightColorsActivated(boolean activated) {
            ColorDisplayService.this.getContext().enforceCallingOrSelfPermission("android.permission.CONTROL_DISPLAY_COLOR_TRANSFORMS", "Permission required to set reduce bright colors activation state");
            long token = Binder.clearCallingIdentity();
            try {
                return ColorDisplayService.this.setReduceBrightColorsActivatedInternal(activated);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public int getReduceBrightColorsStrength() {
            long token = Binder.clearCallingIdentity();
            try {
                return ColorDisplayService.this.mReduceBrightColorsTintController.getStrength();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public float getReduceBrightColorsOffsetFactor() {
            long token = Binder.clearCallingIdentity();
            try {
                return ColorDisplayService.this.mReduceBrightColorsTintController.getOffsetFactor();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean setReduceBrightColorsStrength(int strength) {
            ColorDisplayService.this.getContext().enforceCallingOrSelfPermission("android.permission.CONTROL_DISPLAY_COLOR_TRANSFORMS", "Permission required to set reduce bright colors strength");
            long token = Binder.clearCallingIdentity();
            try {
                return ColorDisplayService.this.setReduceBrightColorsStrengthInternal(strength);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (!DumpUtils.checkDumpPermission(ColorDisplayService.this.getContext(), ColorDisplayService.TAG, pw)) {
                return;
            }
            long token = Binder.clearCallingIdentity();
            try {
                ColorDisplayService.this.dumpInternal(pw);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.display.color.ColorDisplayService$BinderService */
        /* JADX WARN: Multi-variable type inference failed */
        public int handleShellCommand(ParcelFileDescriptor in, ParcelFileDescriptor out, ParcelFileDescriptor err, String[] args) {
            ColorDisplayService.this.getContext().enforceCallingOrSelfPermission("android.permission.CONTROL_DISPLAY_COLOR_TRANSFORMS", "Permission required to use ADB color transform commands");
            long token = Binder.clearCallingIdentity();
            try {
                return new ColorDisplayShellCommand(ColorDisplayService.this).exec(this, in.getFileDescriptor(), out.getFileDescriptor(), err.getFileDescriptor(), args);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }
}
