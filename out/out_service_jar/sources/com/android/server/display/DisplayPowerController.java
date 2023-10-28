package com.android.server.display;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.AmbientBrightnessDayStats;
import android.hardware.display.BrightnessChangeEvent;
import android.hardware.display.BrightnessConfiguration;
import android.hardware.display.BrightnessInfo;
import android.hardware.display.DisplayManagerInternal;
import android.metrics.LogMaker;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UEventObserver;
import android.provider.Settings;
import android.util.Log;
import android.util.MathUtils;
import android.util.MutableFloat;
import android.util.MutableInt;
import android.util.Slog;
import android.util.TimeUtils;
import android.view.Display;
import com.android.internal.app.IBatteryStats;
import com.android.internal.display.BrightnessSynchronizer;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.RingBuffer;
import com.android.server.LocalServices;
import com.android.server.am.BatteryStatsService;
import com.android.server.display.AutomaticBrightnessController;
import com.android.server.display.BrightnessSetting;
import com.android.server.display.DisplayDeviceConfig;
import com.android.server.display.HighBrightnessModeController;
import com.android.server.display.RampAnimator;
import com.android.server.display.color.ColorDisplayService;
import com.android.server.display.utils.SensorUtils;
import com.android.server.display.whitebalance.DisplayWhiteBalanceController;
import com.android.server.display.whitebalance.DisplayWhiteBalanceFactory;
import com.android.server.display.whitebalance.DisplayWhiteBalanceSettings;
import com.android.server.policy.WindowManagerPolicy;
import com.transsion.hubcore.server.display.ITranDisplayPowerController;
import com.transsion.hubcore.server.tranhbm.ITranHBMManager;
import com.transsion.hubsdk.trancare.trancare.TranTrancareManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class DisplayPowerController implements AutomaticBrightnessController.Callbacks, DisplayWhiteBalanceController.Callbacks {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int BRIGHTNESS_CHANGE_STATSD_REPORT_INTERVAL_MS = 500;
    private static final int COLOR_FADE_OFF_ANIMATION_DURATION_MILLIS = 100;
    private static final int COLOR_FADE_ON_ANIMATION_DURATION_MILLIS = 250;
    private static final boolean DEBUG_FOLD;
    private static final boolean DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT = false;
    private static final long LCM_TRACK_TID = 108960000016L;
    private static final int MSG_BRIGHTNESS_RAMP_DONE = 12;
    private static final int MSG_CONFIGURE_BRIGHTNESS = 5;
    private static final int MSG_EXIT_BY_POWERKEY = 18;
    private static final int MSG_IGNORE_PROXIMITY = 8;
    private static final int MSG_PROXIMITY_SENSOR_DEBOUNCED = 2;
    private static final int MSG_SCREEN_OFF_UNBLOCKED = 4;
    private static final int MSG_SCREEN_ON_UNBLOCKED = 3;
    private static final int MSG_SET_TEMPORARY_AUTO_BRIGHTNESS_ADJUSTMENT = 7;
    private static final int MSG_SET_TEMPORARY_BRIGHTNESS = 6;
    private static final int MSG_STATSD_HBM_BRIGHTNESS = 13;
    private static final int MSG_STOP = 9;
    private static final int MSG_UPDATE_BRIGHTNESS = 10;
    private static final int MSG_UPDATE_POWER_STATE = 1;
    private static final int MSG_UPDATE_RBC = 11;
    private static final boolean OS_FOLDABLE_SCREEN_SUPPORT;
    private static final int PROXIMITY_NEGATIVE = 0;
    private static final int PROXIMITY_POSITIVE = 1;
    private static final int PROXIMITY_SENSOR_NEGATIVE_DEBOUNCE_DELAY = 25;
    private static final int PROXIMITY_SENSOR_POSITIVE_DEBOUNCE_DELAY = 0;
    private static final int PROXIMITY_UNKNOWN = -1;
    private static final int RAMP_STATE_SKIP_AUTOBRIGHT = 2;
    private static final int RAMP_STATE_SKIP_INITIAL = 1;
    private static final int RAMP_STATE_SKIP_NONE = 0;
    private static final int REPORTED_TO_POLICY_SCREEN_OFF = 0;
    private static final int REPORTED_TO_POLICY_SCREEN_ON = 2;
    private static final int REPORTED_TO_POLICY_SCREEN_TURNING_OFF = 3;
    private static final int REPORTED_TO_POLICY_SCREEN_TURNING_ON = 1;
    private static final int REPORTED_TO_POLICY_UNREPORTED = -1;
    private static final int RINGBUFFER_MAX = 100;
    private static final float SCREEN_ANIMATION_RATE_MINIMUM = 0.0f;
    private static final String SCREEN_OFF_BLOCKED_TRACE_NAME = "Screen off blocked";
    private static final String SCREEN_ON_BLOCKED_TRACE_NAME = "Screen on blocked";
    private static final boolean TRAN_BACKLIGHT_4095_LEVEL;
    private static final boolean TRAN_BACKLIGHT_OPTIMIZATION_SUPPORT;
    private static final boolean TRAN_LIGHTSENSOR_OPTIMIZATION_SUPPORT;
    private static final boolean TRAN_LUCID_OPTIMIZATION_SUPPORT;
    private static final boolean TRAN_PHYSICAL_BACKLIGHT_OPTIMIZATION_SUPPORT;
    private static final boolean TRAN_TEMP_BACKLIGHT_SUPPORT;
    private static final float TYPICAL_PROXIMITY_THRESHOLD = 5.0f;
    private static final boolean USE_COLOR_FADE_ON_ANIMATION;
    private static final boolean mIsLucidDisabled;
    private static boolean mMultipleDisplayFlipSupport;
    private final String TAG;
    private final boolean mAllowAutoBrightnessWhileDozingConfig;
    private boolean mAppAutoEnableProcessing;
    private boolean mAppliedAutoBrightness;
    private boolean mAppliedBrightnessBoost;
    private boolean mAppliedDimming;
    private boolean mAppliedLowPower;
    private boolean mAppliedScreenBrightnessOverride;
    private boolean mAppliedTemporaryAutoBrightnessAdjustment;
    private boolean mAppliedTemporaryBrightness;
    private boolean mAppliedThrottling;
    private float mAutoBrightnessAdjustment;
    private AutomaticBrightnessController mAutomaticBrightnessController;
    private final IBatteryStats mBatteryStats;
    private final DisplayBlanker mBlanker;
    private final boolean mBrightnessBucketsInDozeConfig;
    private BrightnessConfiguration mBrightnessConfiguration;
    private RingBuffer<BrightnessEvent> mBrightnessEventRingBuffer;
    private long mBrightnessRampDecreaseMaxTimeMillis;
    private long mBrightnessRampIncreaseMaxTimeMillis;
    private float mBrightnessRampRateFastDecrease;
    private float mBrightnessRampRateFastIncrease;
    private float mBrightnessRampRateSlowDecrease;
    private float mBrightnessRampRateSlowIncrease;
    private final BrightnessSetting mBrightnessSetting;
    private BrightnessSetting.BrightnessSettingListener mBrightnessSettingListener;
    private final BrightnessThrottler mBrightnessThrottler;
    private final BrightnessTracker mBrightnessTracker;
    private final DisplayManagerInternal.DisplayPowerCallbacks mCallbacks;
    private final ColorDisplayService.ColorDisplayServiceInternal mCdsi;
    private final boolean mColorFadeEnabled;
    private final boolean mColorFadeFadesConfig;
    private ObjectAnimator mColorFadeOffAnimator;
    private ObjectAnimator mColorFadeOnAnimator;
    private final Context mContext;
    private float mCurrentScreenBrightnessSetting;
    private final boolean mDisplayBlanksAfterDozeConfig;
    private DisplayDevice mDisplayDevice;
    private DisplayDeviceConfig mDisplayDeviceConfig;
    private final int mDisplayId;
    private boolean mDisplayReadyLocked;
    private int mDisplayStatsId;
    private final DisplayWhiteBalanceController mDisplayWhiteBalanceController;
    private final DisplayWhiteBalanceSettings mDisplayWhiteBalanceSettings;
    private boolean mDozing;
    private boolean mEventTrackFlag;
    private final DisplayControllerHandler mHandler;
    private final HighBrightnessModeController mHbmController;
    private BrightnessMappingStrategy mIdleModeBrightnessMapper;
    private boolean mIgnoreProximityUntilChanged;
    private float mInitialAutoBrightness;
    private BrightnessMappingStrategy mInteractiveModeBrightnessMapper;
    private boolean mIsRbcActive;
    private float mLastAmbientLuxFloat;
    private final BrightnessEvent mLastBrightnessEvent;
    private float mLastBrightnessState;
    private float mLastRate;
    private float mLastTrackBrightnessFloat;
    private Sensor mLightSensor;
    private final LogicalDisplay mLogicalDisplay;
    private float[] mNitsRange;
    private final Runnable mOnBrightnessChangeRunnable;
    private int mOnProximityNegativeMessages;
    private int mOnProximityPositiveMessages;
    private boolean mOnStateChangedPending;
    private float mPendingAutoBrightnessAdjustment;
    private boolean mPendingRequestChangedLocked;
    private DisplayManagerInternal.DisplayPowerRequest mPendingRequestLocked;
    private float mPendingScreenBrightnessSetting;
    private boolean mPendingScreenOff;
    private ScreenOffUnblocker mPendingScreenOffUnblocker;
    private ScreenOnUnblocker mPendingScreenOnUnblocker;
    private boolean mPendingUpdatePowerStateLocked;
    private boolean mPendingWaitForNegativeProximityLocked;
    private DisplayManagerInternal.DisplayPowerRequest mPowerRequest;
    private DisplayPowerState mPowerState;
    private Sensor mProximitySensor;
    private boolean mProximitySensorEnabled;
    private float mProximityThreshold;
    private final float mScreenBrightnessDefault;
    private final float mScreenBrightnessDimConfig;
    private final float mScreenBrightnessDozeConfig;
    private float mScreenBrightnessForVr;
    private final float mScreenBrightnessForVrDefault;
    private final float mScreenBrightnessForVrRangeMaximum;
    private final float mScreenBrightnessForVrRangeMinimum;
    private final float mScreenBrightnessMinimumDimAmount;
    private RampAnimator.DualRampAnimator<DisplayPowerState> mScreenBrightnessRampAnimator;
    private boolean mScreenOffBecauseOfProximity;
    private long mScreenOffBlockStartRealTime;
    private long mScreenOnBlockStartRealTime;
    private final SensorManager mSensorManager;
    private final SettingsObserver mSettingsObserver;
    private boolean mShouldUpdateBrightnessSettingLater;
    private final boolean mSkipScreenOnBrightnessRamp;
    private boolean mStopped;
    private final String mSuspendBlockerIdOnStateChanged;
    private final String mSuspendBlockerIdProxDebounce;
    private final String mSuspendBlockerIdProxNegative;
    private final String mSuspendBlockerIdProxPositive;
    private final String mSuspendBlockerIdUnfinishedBusiness;
    private final BrightnessEvent mTempBrightnessEvent;
    private float mTemporaryAutoBrightnessAdjustment;
    private float mTemporaryScreenBrightness;
    private boolean mUnfinishedBusiness;
    private String mUniqueDisplayId;
    private boolean mUseSoftwareAutoBrightnessConfig;
    private boolean mWaitingForNegativeProximity;
    private final WindowManagerPolicy mWindowManagerPolicy;
    private static final boolean DEBUG = SystemProperties.getBoolean("dbg.dms.dpc", false);
    private static final boolean MTK_DEBUG = "eng".equals(Build.TYPE);
    private final Object mLock = new Object();
    private final CachedBrightnessInfo mCachedBrightnessInfo = new CachedBrightnessInfo();
    private int mProximity = -1;
    private int mPendingProximity = -1;
    private long mPendingProximityDebounceTime = -1;
    private int mReportedScreenStateToPolicy = -1;
    private final BrightnessReason mBrightnessReason = new BrightnessReason();
    private final BrightnessReason mBrightnessReasonTemp = new BrightnessReason();
    private float mLastStatsBrightness = SCREEN_ANIMATION_RATE_MINIMUM;
    private int mSkipRampState = 0;
    private float mLastUserSetScreenBrightness = Float.NaN;
    private boolean mIsLucidUpdate = false;
    private int mBeforeLucidBrightness = -1;
    private int mAfterLucidBrightness = -1;
    private int mLucidAnimationBrightness = -1;
    private boolean mIsLucidBrightnessLuxReset = false;
    private boolean mIsLucidBrightnessSet = false;
    private int mPreLucidBrightness = -1;
    private boolean mLucidBeginAnimation = false;
    private boolean PROXIMITY_POWER_KEY_SUPPORT = "1".equals(SystemProperties.get("ro.proximity.powerkey.support"));
    private boolean mHandleByMessage = false;
    private boolean mProximityStatus = false;
    private int mBrighnessUpdateReversal = 1;
    private boolean mScreenOffManualAutoMatic = false;
    private boolean mScreenOffDozeDefaultAutoMatic = false;
    private boolean mPrevScreenOffBecauseOfProximity = false;
    private boolean mProximityPositiveToNegative = false;
    private float mBrightnessFactor = 1.0f;
    private boolean mIsThresholdBrightnessTemp = false;
    private boolean mIsRestBrightnessFactorTemp = false;
    private final float BACKLIGHT_200_LEVEL = 0.04884005f;
    private UEventObserver mValidTempObserver = new UEventObserver() { // from class: com.android.server.display.DisplayPowerController.3
        public void onUEvent(UEventObserver.UEvent event) {
            DisplayPowerController.this.mIsThresholdBrightnessTemp = "1".equals(event.get("TOO_HOT"));
            DisplayPowerController.this.mIsRestBrightnessFactorTemp = "1".equals(event.get("COOL_DOWN"));
            Slog.i(DisplayPowerController.this.TAG, "mValidTempObserver : onUEvent: " + event.toString() + " mIsThresholdBrightnessTemp=" + DisplayPowerController.this.mIsThresholdBrightnessTemp + " mIsRestBrightnessFactorTemp=" + DisplayPowerController.this.mIsRestBrightnessFactorTemp);
            if (DisplayPowerController.this.mIsThresholdBrightnessTemp || DisplayPowerController.this.mIsRestBrightnessFactorTemp) {
                DisplayPowerController.this.m3417x12b1bc01();
            }
        }
    };
    private final Animator.AnimatorListener mAnimatorListener = new Animator.AnimatorListener() { // from class: com.android.server.display.DisplayPowerController.4
        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animation) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animation) {
            DisplayPowerController.this.m3417x12b1bc01();
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationRepeat(Animator animation) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animation) {
        }
    };
    private final RampAnimator.Listener mRampAnimatorListener = new RampAnimator.Listener() { // from class: com.android.server.display.DisplayPowerController.5
        @Override // com.android.server.display.RampAnimator.Listener
        public void onAnimationEnd() {
            DisplayPowerController.this.m3417x12b1bc01();
            Message msg = DisplayPowerController.this.mHandler.obtainMessage(12);
            DisplayPowerController.this.mHandler.sendMessage(msg);
        }
    };
    private boolean mScreenOffBecauseofDisplayTransition = false;
    private final Runnable mCleanListener = new Runnable() { // from class: com.android.server.display.DisplayPowerController$$ExternalSyntheticLambda1
        @Override // java.lang.Runnable
        public final void run() {
            DisplayPowerController.this.m3417x12b1bc01();
        }
    };
    private final Runnable mOnStateChangedRunnable = new Runnable() { // from class: com.android.server.display.DisplayPowerController.7
        @Override // java.lang.Runnable
        public void run() {
            DisplayPowerController.this.mOnStateChangedPending = false;
            DisplayPowerController.this.mCallbacks.onStateChanged();
            DisplayPowerController.this.mCallbacks.releaseSuspendBlocker(DisplayPowerController.this.mSuspendBlockerIdOnStateChanged);
        }
    };
    private final Runnable mOnProximityPositiveRunnable = new Runnable() { // from class: com.android.server.display.DisplayPowerController.8
        @Override // java.lang.Runnable
        public void run() {
            DisplayPowerController displayPowerController = DisplayPowerController.this;
            displayPowerController.mOnProximityPositiveMessages--;
            DisplayPowerController.this.mCallbacks.onProximityPositive();
            DisplayPowerController.this.mCallbacks.releaseSuspendBlocker(DisplayPowerController.this.mSuspendBlockerIdProxPositive);
        }
    };
    private final Runnable mOnProximityNegativeRunnable = new Runnable() { // from class: com.android.server.display.DisplayPowerController.9
        @Override // java.lang.Runnable
        public void run() {
            DisplayPowerController displayPowerController = DisplayPowerController.this;
            displayPowerController.mOnProximityNegativeMessages--;
            DisplayPowerController.this.mCallbacks.onProximityNegative();
            DisplayPowerController.this.mCallbacks.releaseSuspendBlocker(DisplayPowerController.this.mSuspendBlockerIdProxNegative);
        }
    };
    private final SensorEventListener mProximitySensorListener = new SensorEventListener() { // from class: com.android.server.display.DisplayPowerController.10
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            if (DisplayPowerController.this.mProximitySensorEnabled) {
                long time = SystemClock.uptimeMillis();
                boolean positive = false;
                float distance = event.values[0];
                if (distance >= DisplayPowerController.SCREEN_ANIMATION_RATE_MINIMUM && distance < DisplayPowerController.this.mProximityThreshold) {
                    positive = true;
                }
                DisplayPowerController.this.handleProximitySensorEvent(time, positive);
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    static native int calcLucidBright(int i);

    static {
        boolean z = SystemProperties.getBoolean("ro.os_foldable_screen_support", false);
        OS_FOLDABLE_SCREEN_SUPPORT = z;
        DEBUG_FOLD = z;
        mMultipleDisplayFlipSupport = SystemProperties.getInt("ro.product.multiple_display_flip.support", 0) == 1;
        USE_COLOR_FADE_ON_ANIMATION = IDisplayManagerServiceLice.Instance().useColorFadeOnAnimation(false);
        mIsLucidDisabled = SystemProperties.get("persist.product.lucid.disabled").equals("1");
        TRAN_LUCID_OPTIMIZATION_SUPPORT = "1".equals(SystemProperties.get("persist.transsion.lucid.optimization", "0"));
        TRAN_BACKLIGHT_OPTIMIZATION_SUPPORT = "1".equals(SystemProperties.get("ro.transsion.backlight.optimization", "0"));
        TRAN_PHYSICAL_BACKLIGHT_OPTIMIZATION_SUPPORT = "1".equals(SystemProperties.get("ro.transsion.physical.backlight.optimization", "0"));
        TRAN_LIGHTSENSOR_OPTIMIZATION_SUPPORT = "1".equals(SystemProperties.get("ro.transsion.lightsensor_optimiz_support", "0"));
        TRAN_TEMP_BACKLIGHT_SUPPORT = "1".equals(SystemProperties.get("ro.tran_temp_backlight_optimiz", "0"));
        TRAN_BACKLIGHT_4095_LEVEL = "4095".equals(SystemProperties.get("ro.transsion.backlight.level", "4095"));
    }

    /* JADX WARN: Removed duplicated region for block: B:27:0x02b0  */
    /* JADX WARN: Removed duplicated region for block: B:31:0x02c9  */
    /* JADX WARN: Removed duplicated region for block: B:40:0x02f0  */
    /* JADX WARN: Removed duplicated region for block: B:41:0x02f7  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public DisplayPowerController(Context context, DisplayManagerInternal.DisplayPowerCallbacks callbacks, Handler handler, SensorManager sensorManager, DisplayBlanker blanker, LogicalDisplay logicalDisplay, BrightnessTracker brightnessTracker, BrightnessSetting brightnessSetting, Runnable onBrightnessChangeRunnable) {
        DisplayWhiteBalanceController displayWhiteBalanceController;
        Boolean res;
        this.mLogicalDisplay = logicalDisplay;
        int displayIdLocked = logicalDisplay.getDisplayIdLocked();
        this.mDisplayId = displayIdLocked;
        String displayIdStr = "[" + displayIdLocked + "]";
        this.TAG = "DisplayPowerController" + displayIdStr;
        this.mSuspendBlockerIdUnfinishedBusiness = displayIdStr + "unfinished business";
        this.mSuspendBlockerIdOnStateChanged = displayIdStr + "on state changed";
        this.mSuspendBlockerIdProxPositive = displayIdStr + "prox positive";
        this.mSuspendBlockerIdProxNegative = displayIdStr + "prox negative";
        this.mSuspendBlockerIdProxDebounce = displayIdStr + "prox debounce";
        this.mDisplayDevice = logicalDisplay.getPrimaryDisplayDeviceLocked();
        String uniqueId = logicalDisplay.getPrimaryDisplayDeviceLocked().getUniqueId();
        this.mUniqueDisplayId = uniqueId;
        this.mDisplayStatsId = uniqueId.hashCode();
        DisplayControllerHandler displayControllerHandler = new DisplayControllerHandler(handler.getLooper());
        this.mHandler = displayControllerHandler;
        this.mLastBrightnessEvent = new BrightnessEvent(displayIdLocked);
        this.mTempBrightnessEvent = new BrightnessEvent(displayIdLocked);
        if (displayIdLocked == 0) {
            this.mBatteryStats = BatteryStatsService.getService();
        } else {
            this.mBatteryStats = null;
        }
        this.mSettingsObserver = new SettingsObserver(displayControllerHandler);
        this.mCallbacks = callbacks;
        this.mSensorManager = sensorManager;
        this.mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        this.mBlanker = blanker;
        this.mContext = context;
        this.mBrightnessTracker = brightnessTracker;
        this.mBrightnessSetting = brightnessSetting;
        this.mOnBrightnessChangeRunnable = onBrightnessChangeRunnable;
        PowerManager pm = (PowerManager) context.getSystemService(PowerManager.class);
        Resources resources = context.getResources();
        this.mScreenBrightnessDozeConfig = clampAbsoluteBrightness(pm.getBrightnessConstraint(4));
        this.mScreenBrightnessDimConfig = clampAbsoluteBrightness(pm.getBrightnessConstraint(3));
        this.mScreenBrightnessMinimumDimAmount = resources.getFloat(17105103);
        this.mScreenBrightnessDefault = clampAbsoluteBrightness(logicalDisplay.getDisplayInfoLocked().brightnessDefault);
        this.mScreenBrightnessForVrDefault = clampAbsoluteBrightness(pm.getBrightnessConstraint(7));
        this.mScreenBrightnessForVrRangeMaximum = clampAbsoluteBrightness(pm.getBrightnessConstraint(6));
        this.mScreenBrightnessForVrRangeMinimum = clampAbsoluteBrightness(pm.getBrightnessConstraint(5));
        this.mUseSoftwareAutoBrightnessConfig = resources.getBoolean(17891379) && displayIdLocked == 0;
        this.mAllowAutoBrightnessWhileDozingConfig = resources.getBoolean(17891347);
        this.mDisplayDeviceConfig = logicalDisplay.getPrimaryDisplayDeviceLocked().getDisplayDeviceConfig();
        loadBrightnessRampRates();
        this.mSkipScreenOnBrightnessRamp = resources.getBoolean(17891756);
        this.mHbmController = createHbmControllerLocked();
        this.mBrightnessThrottler = createBrightnessThrottlerLocked();
        if (ITranDisplayPowerController.Instance().flexibleDisplayBrightnessSupport(displayIdLocked)) {
            ITranDisplayPowerController.Instance().flexibleDisplayBrightnessInit(context);
            ITranDisplayPowerController.Instance().setFlexibleDisplayBrightnessActive(true);
        }
        saveBrightnessInfo(getScreenBrightnessSetting());
        DisplayWhiteBalanceSettings displayWhiteBalanceSettings = null;
        DisplayWhiteBalanceController displayWhiteBalanceController2 = null;
        if (displayIdLocked != 0) {
            displayWhiteBalanceController = null;
        } else {
            try {
                displayWhiteBalanceSettings = new DisplayWhiteBalanceSettings(context, displayControllerHandler);
                displayWhiteBalanceController = DisplayWhiteBalanceFactory.create(displayControllerHandler, sensorManager, resources);
            } catch (Exception e) {
                e = e;
            }
            try {
                displayWhiteBalanceSettings.setCallbacks(this);
                displayWhiteBalanceController.setCallbacks(this);
            } catch (Exception e2) {
                e = e2;
                displayWhiteBalanceController2 = displayWhiteBalanceController;
                Slog.e(this.TAG, "failed to set up display white-balance: " + e);
                displayWhiteBalanceController = displayWhiteBalanceController2;
                this.mDisplayWhiteBalanceSettings = displayWhiteBalanceSettings;
                this.mDisplayWhiteBalanceController = displayWhiteBalanceController;
                loadNitsRange(resources);
                if (this.mDisplayId == 0) {
                }
                setUpAutoBrightness(resources, handler);
                if (ActivityManager.isLowRamDeviceStatic()) {
                }
                this.mColorFadeEnabled = r0;
                res = IDisplayManagerServiceLice.Instance().onSkipColorFadeFadesConfig(this.mDisplayId, r0);
                if (res == null) {
                }
                this.mDisplayBlanksAfterDozeConfig = resources.getBoolean(17891602);
                this.mBrightnessBucketsInDozeConfig = resources.getBoolean(17891603);
                loadProximitySensor();
                this.mCurrentScreenBrightnessSetting = getScreenBrightnessSetting();
                this.mScreenBrightnessForVr = getScreenBrightnessForVrSetting();
                this.mAutoBrightnessAdjustment = getAutoBrightnessAdjustmentSetting();
                this.mTemporaryScreenBrightness = Float.NaN;
                this.mPendingScreenBrightnessSetting = Float.NaN;
                this.mTemporaryAutoBrightnessAdjustment = Float.NaN;
                this.mPendingAutoBrightnessAdjustment = Float.NaN;
            }
        }
        this.mDisplayWhiteBalanceSettings = displayWhiteBalanceSettings;
        this.mDisplayWhiteBalanceController = displayWhiteBalanceController;
        loadNitsRange(resources);
        if (this.mDisplayId == 0) {
            this.mCdsi = null;
        } else {
            ColorDisplayService.ColorDisplayServiceInternal colorDisplayServiceInternal = (ColorDisplayService.ColorDisplayServiceInternal) LocalServices.getService(ColorDisplayService.ColorDisplayServiceInternal.class);
            this.mCdsi = colorDisplayServiceInternal;
            boolean active = colorDisplayServiceInternal.setReduceBrightColorsListener(new ColorDisplayService.ReduceBrightColorsListener() { // from class: com.android.server.display.DisplayPowerController.1
                @Override // com.android.server.display.color.ColorDisplayService.ReduceBrightColorsListener
                public void onReduceBrightColorsActivationChanged(boolean activated, boolean userInitiated) {
                    DisplayPowerController.this.applyReduceBrightColorsSplineAdjustment();
                }

                @Override // com.android.server.display.color.ColorDisplayService.ReduceBrightColorsListener
                public void onReduceBrightColorsStrengthChanged(int strength) {
                    DisplayPowerController.this.applyReduceBrightColorsSplineAdjustment();
                }
            });
            if (active) {
                applyReduceBrightColorsSplineAdjustment();
            }
        }
        setUpAutoBrightness(resources, handler);
        boolean z = ActivityManager.isLowRamDeviceStatic() && !this.mLogicalDisplay.isDualDisplayPowerController();
        this.mColorFadeEnabled = z;
        res = IDisplayManagerServiceLice.Instance().onSkipColorFadeFadesConfig(this.mDisplayId, z);
        if (res == null) {
            this.mColorFadeFadesConfig = res.booleanValue();
        } else {
            this.mColorFadeFadesConfig = resources.getBoolean(17891367);
        }
        this.mDisplayBlanksAfterDozeConfig = resources.getBoolean(17891602);
        this.mBrightnessBucketsInDozeConfig = resources.getBoolean(17891603);
        loadProximitySensor();
        this.mCurrentScreenBrightnessSetting = getScreenBrightnessSetting();
        this.mScreenBrightnessForVr = getScreenBrightnessForVrSetting();
        this.mAutoBrightnessAdjustment = getAutoBrightnessAdjustmentSetting();
        this.mTemporaryScreenBrightness = Float.NaN;
        this.mPendingScreenBrightnessSetting = Float.NaN;
        this.mTemporaryAutoBrightnessAdjustment = Float.NaN;
        this.mPendingAutoBrightnessAdjustment = Float.NaN;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applyReduceBrightColorsSplineAdjustment() {
        this.mHandler.obtainMessage(11).sendToTarget();
        m3417x12b1bc01();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleRbcChanged() {
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController == null) {
            return;
        }
        if ((!automaticBrightnessController.isInIdleMode() && this.mInteractiveModeBrightnessMapper == null) || (this.mAutomaticBrightnessController.isInIdleMode() && this.mIdleModeBrightnessMapper == null)) {
            Log.w(this.TAG, "No brightness mapping available to recalculate splines for this mode");
            return;
        }
        float[] adjustedNits = new float[this.mNitsRange.length];
        int i = 0;
        while (true) {
            float[] fArr = this.mNitsRange;
            if (i < fArr.length) {
                adjustedNits[i] = this.mCdsi.getReduceBrightColorsAdjustedBrightnessNits(fArr[i]);
                i++;
            } else {
                boolean isReduceBrightColorsActivated = this.mCdsi.isReduceBrightColorsActivated();
                this.mIsRbcActive = isReduceBrightColorsActivated;
                this.mAutomaticBrightnessController.recalculateSplines(isReduceBrightColorsActivated, adjustedNits);
                return;
            }
        }
    }

    public boolean isProximitySensorAvailable() {
        return this.mProximitySensor != null;
    }

    public ParceledListSlice<BrightnessChangeEvent> getBrightnessEvents(int userId, boolean includePackage) {
        BrightnessTracker brightnessTracker = this.mBrightnessTracker;
        if (brightnessTracker == null) {
            return null;
        }
        return brightnessTracker.getEvents(userId, includePackage);
    }

    public void onSwitchUser(int newUserId) {
        handleSettingsChange(true);
        BrightnessTracker brightnessTracker = this.mBrightnessTracker;
        if (brightnessTracker != null) {
            brightnessTracker.onSwitchUser(newUserId);
        }
    }

    public ParceledListSlice<AmbientBrightnessDayStats> getAmbientBrightnessStats(int userId) {
        BrightnessTracker brightnessTracker = this.mBrightnessTracker;
        if (brightnessTracker == null) {
            return null;
        }
        return brightnessTracker.getAmbientBrightnessStats(userId);
    }

    public void persistBrightnessTrackerState() {
        BrightnessTracker brightnessTracker = this.mBrightnessTracker;
        if (brightnessTracker != null) {
            brightnessTracker.persistBrightnessTrackerState();
        }
    }

    public void preResumeLcm() {
        if (this.mLogicalDisplay.getPhase() == 0) {
            Slog.w(this.TAG, "don't startPreWakeup while in transition");
        } else {
            this.mPowerState.startPreWakeup();
        }
    }

    public boolean requestPowerState(DisplayManagerInternal.DisplayPowerRequest request, boolean waitForNegativeProximity) {
        boolean z = DEBUG;
        if (z) {
            Slog.d(this.TAG, "requestPowerState: " + request + ", waitForNegativeProximity=" + waitForNegativeProximity);
        }
        synchronized (this.mLock) {
            if (this.mStopped) {
                return true;
            }
            boolean changed = false;
            if (waitForNegativeProximity && !this.mPendingWaitForNegativeProximityLocked) {
                this.mPendingWaitForNegativeProximityLocked = true;
                changed = true;
            }
            DisplayManagerInternal.DisplayPowerRequest displayPowerRequest = this.mPendingRequestLocked;
            if (displayPowerRequest == null) {
                this.mPendingRequestLocked = new DisplayManagerInternal.DisplayPowerRequest(request);
                changed = true;
            } else if (!displayPowerRequest.equals(request)) {
                this.mPendingRequestLocked.copyFrom(request);
                changed = true;
            }
            if (changed) {
                this.mDisplayReadyLocked = false;
                if (!this.mPendingRequestChangedLocked) {
                    this.mPendingRequestChangedLocked = true;
                    sendUpdatePowerStateLocked();
                }
            }
            if ((MTK_DEBUG && changed) || z) {
                Slog.d(this.TAG, "requestPowerState: " + request + ", waitForNegativeProximity=" + waitForNegativeProximity + ", changed=" + changed);
            }
            return this.mDisplayReadyLocked;
        }
    }

    public BrightnessConfiguration getDefaultBrightnessConfiguration() {
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController == null) {
            return null;
        }
        return automaticBrightnessController.getDefaultConfig();
    }

    public void updatePowerStateForLucid() {
        if (!mIsLucidDisabled && TRAN_LUCID_OPTIMIZATION_SUPPORT) {
            this.mIsLucidUpdate = true;
            Slog.d(this.TAG, "sendUpdatePowerState:UpdatePowerStateForLucid ");
        }
        m3417x12b1bc01();
    }

    public void onDisplayChanged() {
        final DisplayDevice device = this.mLogicalDisplay.getPrimaryDisplayDeviceLocked();
        if (device == null) {
            Slog.wtf(this.TAG, "Display Device is null in DisplayPowerController for display: " + this.mLogicalDisplay.getDisplayIdLocked());
            return;
        }
        final String uniqueId = device.getUniqueId();
        final DisplayDeviceConfig config = device.getDisplayDeviceConfig();
        final IBinder token = device.getDisplayTokenLocked();
        final DisplayDeviceInfo info = device.getDisplayDeviceInfoLocked();
        this.mHandler.post(new Runnable() { // from class: com.android.server.display.DisplayPowerController$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                DisplayPowerController.this.m3418xfba08000(device, uniqueId, config, token, info);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onDisplayChanged$0$com-android-server-display-DisplayPowerController  reason: not valid java name */
    public /* synthetic */ void m3418xfba08000(DisplayDevice device, String uniqueId, DisplayDeviceConfig config, IBinder token, DisplayDeviceInfo info) {
        if (this.mDisplayDevice != device) {
            this.mDisplayDevice = device;
            this.mUniqueDisplayId = uniqueId;
            this.mDisplayStatsId = uniqueId.hashCode();
            this.mDisplayDeviceConfig = config;
            loadFromDisplayDeviceConfig(token, info);
            boolean z = DEBUG;
            if (z) {
                Trace.beginAsyncSection("DisplayPowerController#updatePowerState", 0);
            }
            updatePowerState();
            if (z) {
                Trace.endAsyncSection("DisplayPowerController#updatePowerState", 0);
            }
        }
    }

    public void onDeviceStateTransition() {
        m3417x12b1bc01();
    }

    public void stop() {
        synchronized (this.mLock) {
            this.mStopped = true;
            Message msg = this.mHandler.obtainMessage(9);
            this.mHandler.sendMessage(msg);
            DisplayWhiteBalanceController displayWhiteBalanceController = this.mDisplayWhiteBalanceController;
            if (displayWhiteBalanceController != null) {
                displayWhiteBalanceController.setEnabled(false);
            }
            AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
            if (automaticBrightnessController != null) {
                automaticBrightnessController.stop();
            }
            BrightnessSetting brightnessSetting = this.mBrightnessSetting;
            if (brightnessSetting != null) {
                brightnessSetting.unregisterListener(this.mBrightnessSettingListener);
            }
            UEventObserver uEventObserver = this.mValidTempObserver;
            if (uEventObserver != null) {
                uEventObserver.stopObserving();
            }
            this.mContext.getContentResolver().unregisterContentObserver(this.mSettingsObserver);
            if (ITranHBMManager.TRAN_HIGH_BRIGHTNESS_MODE_SUPPORT) {
                ITranHBMManager.Instance().destroy(this.mDisplayId);
            }
            if (this.mDisplayId == 0) {
                ITranDisplayPowerController.Instance().destroyTranBacklightTemperatureController();
            }
        }
    }

    private void loadFromDisplayDeviceConfig(IBinder token, DisplayDeviceInfo info) {
        loadBrightnessRampRates();
        loadProximitySensor();
        loadNitsRange(this.mContext.getResources());
        setUpAutoBrightness(this.mContext.getResources(), this.mHandler);
        reloadReduceBrightColours();
        RampAnimator.DualRampAnimator<DisplayPowerState> dualRampAnimator = this.mScreenBrightnessRampAnimator;
        if (dualRampAnimator != null) {
            dualRampAnimator.setAnimationTimeLimits(this.mBrightnessRampIncreaseMaxTimeMillis, this.mBrightnessRampDecreaseMaxTimeMillis);
        }
        this.mHbmController.resetHbmData(info.width, info.height, token, info.uniqueId, this.mDisplayDeviceConfig.getHighBrightnessModeData(), new HighBrightnessModeController.HdrBrightnessDeviceConfig() { // from class: com.android.server.display.DisplayPowerController.2
            @Override // com.android.server.display.HighBrightnessModeController.HdrBrightnessDeviceConfig
            public float getHdrBrightnessFromSdr(float sdrBrightness) {
                return DisplayPowerController.this.mDisplayDeviceConfig.getHdrBrightnessFromSdr(sdrBrightness);
            }
        });
        this.mBrightnessThrottler.resetThrottlingData(this.mDisplayDeviceConfig.getBrightnessThrottlingData());
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: sendUpdatePowerState */
    public void m3417x12b1bc01() {
        synchronized (this.mLock) {
            sendUpdatePowerStateLocked();
        }
    }

    private void sendUpdatePowerStateLocked() {
        if (!this.mStopped && !this.mPendingUpdatePowerStateLocked) {
            this.mPendingUpdatePowerStateLocked = true;
            Message msg = this.mHandler.obtainMessage(1);
            this.mHandler.sendMessage(msg);
        }
    }

    private void initialize(int displayState) {
        this.mPowerState = new DisplayPowerState(this.mBlanker, this.mColorFadeEnabled ? new ColorFade(this.mDisplayId) : null, this.mDisplayId, displayState);
        if (ITranHBMManager.TRAN_HIGH_BRIGHTNESS_MODE_SUPPORT) {
            ITranHBMManager.Instance().makeTranHbmDisplayPowerState(this.mDisplayId, displayState);
        }
        if (this.mColorFadeEnabled) {
            ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this.mPowerState, DisplayPowerState.COLOR_FADE_LEVEL, SCREEN_ANIMATION_RATE_MINIMUM, 1.0f);
            this.mColorFadeOnAnimator = ofFloat;
            ofFloat.setDuration(250L);
            this.mColorFadeOnAnimator.addListener(this.mAnimatorListener);
            ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(this.mPowerState, DisplayPowerState.COLOR_FADE_LEVEL, 1.0f, SCREEN_ANIMATION_RATE_MINIMUM);
            this.mColorFadeOffAnimator = ofFloat2;
            ofFloat2.setDuration(100L);
            this.mColorFadeOffAnimator.addListener(this.mAnimatorListener);
            IDisplayManagerServiceLice.Instance().onInitializeColorFadeAnimator(this.mDisplayId, this.mColorFadeOnAnimator, this.mColorFadeOffAnimator);
            TranFoldDisplayCustody.instance().onInitializeColorFadeAnimator(this.mDisplayId, this.mColorFadeOnAnimator, this.mColorFadeOffAnimator);
        }
        RampAnimator.DualRampAnimator<DisplayPowerState> dualRampAnimator = new RampAnimator.DualRampAnimator<>(this.mPowerState, DisplayPowerState.SCREEN_BRIGHTNESS_FLOAT, DisplayPowerState.SCREEN_SDR_BRIGHTNESS_FLOAT);
        this.mScreenBrightnessRampAnimator = dualRampAnimator;
        dualRampAnimator.setAnimationTimeLimits(this.mBrightnessRampIncreaseMaxTimeMillis, this.mBrightnessRampDecreaseMaxTimeMillis);
        this.mScreenBrightnessRampAnimator.setListener(this.mRampAnimatorListener);
        noteScreenState(this.mPowerState.getScreenState());
        noteScreenBrightness(this.mPowerState.getScreenBrightness());
        float brightness = convertToNits(this.mPowerState.getScreenBrightness());
        if (brightness >= SCREEN_ANIMATION_RATE_MINIMUM) {
            this.mBrightnessTracker.start(brightness);
        }
        BrightnessSetting.BrightnessSettingListener brightnessSettingListener = new BrightnessSetting.BrightnessSettingListener() { // from class: com.android.server.display.DisplayPowerController$$ExternalSyntheticLambda5
            @Override // com.android.server.display.BrightnessSetting.BrightnessSettingListener
            public final void onBrightnessChanged(float f) {
                DisplayPowerController.this.m3416xcf269e40(f);
            }
        };
        this.mBrightnessSettingListener = brightnessSettingListener;
        this.mBrightnessSetting.registerListener(brightnessSettingListener);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("screen_brightness_for_vr_float"), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("screen_auto_brightness_adj"), false, this.mSettingsObserver, -1);
        if (TRAN_TEMP_BACKLIGHT_SUPPORT && new File("/sys/devices/virtual/thermal/thermal_zone1").exists()) {
            synchronized (this.mLock) {
                this.mValidTempObserver.startObserving("DEVPATH=/devices/virtual/thermal/thermal_zone1");
            }
            this.mIsThresholdBrightnessTemp = isDefaultThermalTempExceed();
            Slog.i(this.TAG, "mValidTempObserver startObserving");
        }
        if (this.mDisplayId == 0) {
            ITranDisplayPowerController.Instance().initTranBacklightTemperatureController(this.mContext, new Runnable() { // from class: com.android.server.display.DisplayPowerController$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    DisplayPowerController.this.m3417x12b1bc01();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$initialize$1$com-android-server-display-DisplayPowerController  reason: not valid java name */
    public /* synthetic */ void m3416xcf269e40(float brightnessValue) {
        Message msg = this.mHandler.obtainMessage(10, Float.valueOf(brightnessValue));
        this.mHandler.sendMessage(msg);
    }

    private boolean isDefaultThermalTempExceed() {
        boolean status = false;
        try {
            char[] buffer = new char[1024];
            FileReader file = new FileReader("/sys/class/thermal/thermal_zone1/temp");
            int len = file.read(buffer, 0, 1024);
            float temp = Float.parseFloat(new String(buffer, 0, len).trim());
            status = temp >= 42000.0f;
            Slog.d(this.TAG, "isDefaultThermalTempExceed temp=" + temp + " status=" + status);
            file.close();
            return status;
        } catch (FileNotFoundException e) {
            Slog.w(this.TAG, "isDefaultThermalTempExceed FileNotFoundException");
            return status;
        } catch (IOException e2) {
            Slog.e(this.TAG, "isDefaultThermalTempExceed IOException", e2);
            return status;
        }
    }

    private void setUpAutoBrightness(Resources resources, Handler handler) {
        int initialLightSensorRate;
        if (!this.mUseSoftwareAutoBrightnessConfig) {
            return;
        }
        boolean isIdleScreenBrightnessEnabled = resources.getBoolean(17891637);
        this.mInteractiveModeBrightnessMapper = BrightnessMappingStrategy.create(resources, this.mDisplayDeviceConfig, this.mDisplayWhiteBalanceController);
        if (isIdleScreenBrightnessEnabled) {
            this.mIdleModeBrightnessMapper = BrightnessMappingStrategy.createForIdleMode(resources, this.mDisplayDeviceConfig, this.mDisplayWhiteBalanceController);
        }
        if (this.mInteractiveModeBrightnessMapper != null) {
            float dozeScaleFactor = resources.getFraction(18022406, 1, 1);
            int[] ambientBrighteningThresholds = resources.getIntArray(17235981);
            int[] ambientDarkeningThresholds = resources.getIntArray(17235982);
            int[] ambientThresholdLevels = resources.getIntArray(17235983);
            float ambientDarkeningMinThreshold = this.mDisplayDeviceConfig.getAmbientLuxDarkeningMinThreshold();
            float ambientBrighteningMinThreshold = this.mDisplayDeviceConfig.getAmbientLuxBrighteningMinThreshold();
            HysteresisLevels ambientBrightnessThresholds = new HysteresisLevels(ambientBrighteningThresholds, ambientDarkeningThresholds, ambientThresholdLevels, ambientDarkeningMinThreshold, ambientBrighteningMinThreshold);
            int[] screenBrighteningThresholds = resources.getIntArray(17236122);
            int[] screenDarkeningThresholds = resources.getIntArray(17236125);
            int[] screenThresholdLevels = resources.getIntArray(17236126);
            float screenDarkeningMinThreshold = this.mDisplayDeviceConfig.getScreenDarkeningMinThreshold();
            float screenBrighteningMinThreshold = this.mDisplayDeviceConfig.getScreenBrighteningMinThreshold();
            HysteresisLevels screenBrightnessThresholds = new HysteresisLevels(screenBrighteningThresholds, screenDarkeningThresholds, screenThresholdLevels, screenDarkeningMinThreshold, screenBrighteningMinThreshold);
            long brighteningLightDebounce = resources.getInteger(17694738);
            long darkeningLightDebounce = resources.getInteger(17694739);
            boolean autoBrightnessResetAmbientLuxAfterWarmUp = resources.getBoolean(17891373);
            int lightSensorWarmUpTimeConfig = resources.getInteger(17694854);
            int lightSensorRate = resources.getInteger(17694741);
            int initialLightSensorRate2 = resources.getInteger(17694740);
            if (initialLightSensorRate2 == -1) {
                initialLightSensorRate = lightSensorRate;
            } else {
                if (initialLightSensorRate2 > lightSensorRate) {
                    Slog.w(this.TAG, "Expected config_autoBrightnessInitialLightSensorRate (" + initialLightSensorRate2 + ") to be less than or equal to config_autoBrightnessLightSensorRate (" + lightSensorRate + ").");
                }
                initialLightSensorRate = initialLightSensorRate2;
            }
            loadAmbientLightSensor();
            BrightnessTracker brightnessTracker = this.mBrightnessTracker;
            if (brightnessTracker != null) {
                brightnessTracker.setLightSensor(this.mLightSensor);
            }
            AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
            if (automaticBrightnessController != null) {
                automaticBrightnessController.stop();
            }
            this.mAutomaticBrightnessController = new AutomaticBrightnessController(this, handler.getLooper(), this.mSensorManager, this.mLightSensor, this.mInteractiveModeBrightnessMapper, lightSensorWarmUpTimeConfig, SCREEN_ANIMATION_RATE_MINIMUM, 1.0f, dozeScaleFactor, lightSensorRate, initialLightSensorRate, brighteningLightDebounce, darkeningLightDebounce, autoBrightnessResetAmbientLuxAfterWarmUp, ambientBrightnessThresholds, screenBrightnessThresholds, this.mContext, this.mHbmController, this.mBrightnessThrottler, this.mIdleModeBrightnessMapper, this.mDisplayDeviceConfig.getAmbientHorizonShort(), this.mDisplayDeviceConfig.getAmbientHorizonLong());
            if (ITranHBMManager.TRAN_HIGH_BRIGHTNESS_MODE_SUPPORT) {
                ITranHBMManager.Instance().init(this.mDisplayId, this.mContext, handler);
                ITranHBMManager.Instance().makeTranAutomaticBrightnessController(this.mDisplayId, this.mSensorManager, lightSensorWarmUpTimeConfig, dozeScaleFactor, lightSensorRate, initialLightSensorRate, brighteningLightDebounce, darkeningLightDebounce, autoBrightnessResetAmbientLuxAfterWarmUp, ambientBrightnessThresholds, screenBrightnessThresholds);
            }
            this.mBrightnessEventRingBuffer = new RingBuffer<>(BrightnessEvent.class, 100);
            return;
        }
        this.mUseSoftwareAutoBrightnessConfig = false;
    }

    private void loadBrightnessRampRates() {
        this.mBrightnessRampRateFastDecrease = this.mDisplayDeviceConfig.getBrightnessRampFastDecrease();
        this.mBrightnessRampRateFastIncrease = this.mDisplayDeviceConfig.getBrightnessRampFastIncrease();
        this.mBrightnessRampRateSlowDecrease = this.mDisplayDeviceConfig.getBrightnessRampSlowDecrease();
        this.mBrightnessRampRateSlowIncrease = this.mDisplayDeviceConfig.getBrightnessRampSlowIncrease();
        this.mBrightnessRampDecreaseMaxTimeMillis = this.mDisplayDeviceConfig.getBrightnessRampDecreaseMaxMillis();
        this.mBrightnessRampIncreaseMaxTimeMillis = this.mDisplayDeviceConfig.getBrightnessRampIncreaseMaxMillis();
    }

    private void loadNitsRange(Resources resources) {
        DisplayDeviceConfig displayDeviceConfig = this.mDisplayDeviceConfig;
        if (displayDeviceConfig != null && displayDeviceConfig.getNits() != null) {
            this.mNitsRange = this.mDisplayDeviceConfig.getNits();
            return;
        }
        Slog.w(this.TAG, "Screen brightness nits configuration is unavailable; falling back");
        this.mNitsRange = BrightnessMappingStrategy.getFloatArray(resources.obtainTypedArray(17236124));
    }

    private void reloadReduceBrightColours() {
        ColorDisplayService.ColorDisplayServiceInternal colorDisplayServiceInternal = this.mCdsi;
        if (colorDisplayServiceInternal != null && colorDisplayServiceInternal.isReduceBrightColorsActivated()) {
            applyReduceBrightColorsSplineAdjustment();
        }
    }

    public void setAutomaticScreenBrightnessMode(boolean isIdle) {
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController != null) {
            if (isIdle) {
                automaticBrightnessController.switchToIdleMode();
            } else {
                automaticBrightnessController.switchToInteractiveScreenBrightnessMode();
            }
        }
        DisplayWhiteBalanceController displayWhiteBalanceController = this.mDisplayWhiteBalanceController;
        if (displayWhiteBalanceController != null) {
            displayWhiteBalanceController.setStrongModeEnabled(isIdle);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cleanupHandlerThreadAfterStop() {
        float brightness;
        setProximitySensorEnabled(false);
        this.mHbmController.stop();
        this.mBrightnessThrottler.stop();
        this.mHandler.removeCallbacksAndMessages(null);
        if (this.mUnfinishedBusiness) {
            this.mCallbacks.releaseSuspendBlocker(this.mSuspendBlockerIdUnfinishedBusiness);
            this.mUnfinishedBusiness = false;
        }
        if (this.mOnStateChangedPending) {
            this.mCallbacks.releaseSuspendBlocker(this.mSuspendBlockerIdOnStateChanged);
            this.mOnStateChangedPending = false;
        }
        for (int i = 0; i < this.mOnProximityPositiveMessages; i++) {
            this.mCallbacks.releaseSuspendBlocker(this.mSuspendBlockerIdProxPositive);
        }
        this.mOnProximityPositiveMessages = 0;
        for (int i2 = 0; i2 < this.mOnProximityNegativeMessages; i2++) {
            this.mCallbacks.releaseSuspendBlocker(this.mSuspendBlockerIdProxNegative);
        }
        this.mOnProximityNegativeMessages = 0;
        DisplayPowerState displayPowerState = this.mPowerState;
        if (displayPowerState != null) {
            brightness = displayPowerState.getScreenBrightness();
        } else {
            brightness = SCREEN_ANIMATION_RATE_MINIMUM;
        }
        reportStats(brightness);
        DisplayPowerState displayPowerState2 = this.mPowerState;
        if (displayPowerState2 != null) {
            displayPowerState2.stop();
            this.mPowerState = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:160:0x022c, code lost:
        if (com.android.server.display.TranFoldDisplayCustody.instance().needBrightnessOff(r47.mDisplayId, r7, r47.mPendingScreenOnUnblocker != null) != false) goto L738;
     */
    /* JADX WARN: Code restructure failed: missing block: B:627:0x0af2, code lost:
        if (r0 != r44) goto L560;
     */
    /* JADX WARN: Removed duplicated region for block: B:335:0x051f  */
    /* JADX WARN: Removed duplicated region for block: B:352:0x0560  */
    /* JADX WARN: Removed duplicated region for block: B:357:0x0589  */
    /* JADX WARN: Removed duplicated region for block: B:362:0x0596  */
    /* JADX WARN: Removed duplicated region for block: B:367:0x05a7  */
    /* JADX WARN: Removed duplicated region for block: B:380:0x05e3  */
    /* JADX WARN: Removed duplicated region for block: B:386:0x0604  */
    /* JADX WARN: Removed duplicated region for block: B:394:0x0640  */
    /* JADX WARN: Removed duplicated region for block: B:448:0x0705  */
    /* JADX WARN: Removed duplicated region for block: B:453:0x071b  */
    /* JADX WARN: Removed duplicated region for block: B:513:0x0918  */
    /* JADX WARN: Removed duplicated region for block: B:587:0x0a20  */
    /* JADX WARN: Removed duplicated region for block: B:606:0x0ab0  */
    /* JADX WARN: Removed duplicated region for block: B:622:0x0ae4  */
    /* JADX WARN: Removed duplicated region for block: B:659:0x0b39  */
    /* JADX WARN: Removed duplicated region for block: B:660:0x0b3e  */
    /* JADX WARN: Removed duplicated region for block: B:677:0x0b73  */
    /* JADX WARN: Removed duplicated region for block: B:679:0x0b92  */
    /* JADX WARN: Removed duplicated region for block: B:682:0x0b99  */
    /* JADX WARN: Removed duplicated region for block: B:683:0x0ba3  */
    /* JADX WARN: Removed duplicated region for block: B:686:0x0baf A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:695:0x0c24  */
    /* JADX WARN: Removed duplicated region for block: B:701:0x0c38  */
    /* JADX WARN: Removed duplicated region for block: B:708:0x0c89  */
    /* JADX WARN: Removed duplicated region for block: B:714:0x0c9f A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:719:0x0cb8  */
    /* JADX WARN: Removed duplicated region for block: B:722:0x0cca  */
    /* JADX WARN: Removed duplicated region for block: B:725:0x0cd1  */
    /* JADX WARN: Removed duplicated region for block: B:733:0x0cf2  */
    /* JADX WARN: Removed duplicated region for block: B:745:0x0d16  */
    /* JADX WARN: Removed duplicated region for block: B:751:0x0d24  */
    /* JADX WARN: Removed duplicated region for block: B:757:0x0d33  */
    /* JADX WARN: Removed duplicated region for block: B:760:0x0d44  */
    /* JADX WARN: Removed duplicated region for block: B:764:0x0d4c  */
    /* JADX WARN: Removed duplicated region for block: B:767:0x0d5f A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:783:0x0d83  */
    /* JADX WARN: Removed duplicated region for block: B:793:0x0da5  */
    /* JADX WARN: Removed duplicated region for block: B:796:0x0db0  */
    /* JADX WARN: Removed duplicated region for block: B:808:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void updatePowerState() {
        int previousPolicy;
        int state;
        int autoBrightnessState;
        float autoBrightnessAdjustment;
        int brightnessAdjustmentFlags;
        int brightnessAdjustmentFlags2;
        boolean updateImmediately;
        int brightnessAdjustmentFlags3;
        boolean updateImmediately2;
        boolean slowChange;
        float rate;
        float rate2;
        float rate3;
        boolean slowChange2;
        boolean slowChange3;
        float rate4;
        boolean slowChange4;
        float rate5;
        float rate6;
        float rate7;
        boolean z;
        int brightnessAdjustmentFlags4;
        int previousPolicy2;
        boolean mustNotify;
        boolean hadUserBrightnessPoint;
        boolean isLucidTempDisabled;
        int state2;
        float brightnessState;
        boolean isDisplayContentVisible;
        int state3;
        int brightnessAdjustmentFlags5;
        int i;
        boolean ready;
        boolean finished;
        boolean z2;
        boolean z3;
        RingBuffer<BrightnessEvent> ringBuffer;
        float animateValue;
        float currentSdrBrightness;
        boolean slowChange5;
        boolean isLucidAnimation;
        float rate8;
        float rate9;
        float rate10;
        float rampSpeed;
        AutomaticBrightnessController automaticBrightnessController;
        boolean userInitiatedChange;
        AutomaticBrightnessController automaticBrightnessController2;
        boolean isLucidAnimation2;
        boolean isLucidAnimation3;
        int i2;
        int i3;
        float rate11;
        boolean updateScreenBrightnessSetting;
        float rate12;
        boolean slowChange6;
        boolean slowChange7;
        boolean updateScreenBrightnessSetting2;
        float rate13;
        AutomaticBrightnessController automaticBrightnessController3;
        boolean mustInitialize = false;
        this.mBrightnessReasonTemp.set(null);
        this.mTempBrightnessEvent.reset();
        synchronized (this.mLock) {
            try {
                if (this.mStopped) {
                    return;
                }
                this.mPendingUpdatePowerStateLocked = false;
                if (this.mPendingRequestLocked == null) {
                    return;
                }
                DisplayManagerInternal.DisplayPowerRequest displayPowerRequest = this.mPowerRequest;
                if (displayPowerRequest == null) {
                    this.mPowerRequest = new DisplayManagerInternal.DisplayPowerRequest(this.mPendingRequestLocked);
                    updatePendingProximityRequestsLocked();
                    this.mPendingRequestChangedLocked = false;
                    mustInitialize = true;
                    previousPolicy = 3;
                } else if (this.mPendingRequestChangedLocked) {
                    previousPolicy = displayPowerRequest.policy;
                    this.mPowerRequest.copyFrom(this.mPendingRequestLocked);
                    updatePendingProximityRequestsLocked();
                    this.mPendingRequestChangedLocked = false;
                    this.mDisplayReadyLocked = false;
                } else {
                    previousPolicy = displayPowerRequest.policy;
                }
                try {
                    boolean mustNotify2 = !this.mDisplayReadyLocked;
                    float brightnessState2 = Float.NaN;
                    boolean performScreenOffTransition = false;
                    float rate14 = -1.0f;
                    boolean z4 = TRAN_BACKLIGHT_OPTIMIZATION_SUPPORT;
                    if (z4) {
                        if (this.mBrightnessReason.reason == 5) {
                            rate14 = computeScreenBrightnessRate(SCREEN_ANIMATION_RATE_MINIMUM);
                        } else if (this.mAppAutoEnableProcessing) {
                            if (this.mScreenBrightnessRampAnimator.isAnimating()) {
                                if (this.mScreenOffManualAutoMatic) {
                                    if (!this.mPrevScreenOffBecauseOfProximity || this.mScreenOffBecauseOfProximity) {
                                        rate14 = computeScreenBrightnessRate(TRAN_LIGHTSENSOR_OPTIMIZATION_SUPPORT ? 0.1f : 1.6f);
                                    } else {
                                        float rate15 = computeScreenBrightnessRate(SCREEN_ANIMATION_RATE_MINIMUM);
                                        this.mScreenOffManualAutoMatic = false;
                                        rate14 = rate15;
                                    }
                                    this.mPrevScreenOffBecauseOfProximity = false;
                                } else if (this.mScreenOffDozeDefaultAutoMatic) {
                                    rate14 = computeScreenBrightnessRate(0.6f);
                                    this.mScreenOffDozeDefaultAutoMatic = false;
                                } else {
                                    rate14 = computeScreenBrightnessRate(4.0f);
                                }
                            } else {
                                this.mAppAutoEnableProcessing = false;
                                this.mPrevScreenOffBecauseOfProximity = false;
                                this.mScreenOffManualAutoMatic = false;
                                this.mScreenOffDozeDefaultAutoMatic = false;
                            }
                        }
                    }
                    switch (this.mPowerRequest.policy) {
                        case 0:
                            state = 1;
                            performScreenOffTransition = true;
                            break;
                        case 1:
                            if (this.mPowerRequest.dozeScreenState != 0) {
                                state = this.mPowerRequest.dozeScreenState;
                            } else {
                                state = 3;
                            }
                            if (!this.mAllowAutoBrightnessWhileDozingConfig) {
                                brightnessState2 = this.mPowerRequest.dozeScreenBrightness;
                                if (z4) {
                                    rate14 = computeScreenBrightnessRate(3.0f);
                                }
                                this.mBrightnessReasonTemp.setReason(2);
                                break;
                            }
                            break;
                        case 2:
                        case 3:
                        default:
                            state = 2;
                            break;
                        case 4:
                            state = 5;
                            break;
                    }
                    ITranDisplayPowerController.Instance().hookCurrentPowerRequestPolicy(this.mPowerRequest.policy);
                    if (this.mProximitySensor != null) {
                        if (this.mPowerRequest.useProximitySensor && state != 1) {
                            setProximitySensorEnabled(true);
                            if (this.PROXIMITY_POWER_KEY_SUPPORT && this.mProximity == 0 && this.mProximityStatus) {
                                this.mWindowManagerPolicy.setProximityPowerKeyDown(false);
                                this.mProximityStatus = false;
                            }
                            if (!this.mScreenOffBecauseOfProximity && this.mProximity == 1 && !this.mIgnoreProximityUntilChanged) {
                                this.mScreenOffBecauseOfProximity = true;
                                this.mProximityPositiveToNegative = false;
                                sendOnProximityPositiveWithWakelock();
                            }
                        } else if (this.mWaitingForNegativeProximity && this.mScreenOffBecauseOfProximity && this.mProximity == 1 && state != 1) {
                            setProximitySensorEnabled(true);
                        } else {
                            if (this.PROXIMITY_POWER_KEY_SUPPORT) {
                                this.mProximityStatus = true;
                            }
                            setProximitySensorEnabled(false);
                            this.mWaitingForNegativeProximity = false;
                            this.mProximityPositiveToNegative = false;
                        }
                        boolean z5 = this.mScreenOffBecauseOfProximity;
                        if (z5 && (this.mProximity != 1 || this.mIgnoreProximityUntilChanged)) {
                            this.mPrevScreenOffBecauseOfProximity = z5;
                            this.mScreenOffBecauseOfProximity = false;
                            this.mProximityPositiveToNegative = true;
                            sendOnProximityNegativeWithWakelock();
                            if (this.PROXIMITY_POWER_KEY_SUPPORT && !this.mHandleByMessage) {
                                Message msg = this.mHandler.obtainMessage(18);
                                this.mHandler.sendMessageDelayed(msg, 1200L);
                            }
                        } else if (this.PROXIMITY_POWER_KEY_SUPPORT && z5) {
                            this.mHandler.removeMessages(18);
                        }
                    } else {
                        this.mWaitingForNegativeProximity = false;
                        this.mIgnoreProximityUntilChanged = false;
                    }
                    state = (!this.mLogicalDisplay.isEnabled() || this.mScreenOffBecauseOfProximity) ? 1 : 1;
                    if (this.PROXIMITY_POWER_KEY_SUPPORT) {
                        if (!this.mWindowManagerPolicy.isProximityPowerKeyDown() && this.mScreenOffBecauseOfProximity) {
                            state = 1;
                        }
                    } else if (this.mScreenOffBecauseOfProximity) {
                        state = 1;
                    }
                    if (state == 1) {
                        this.mScreenOffBecauseofDisplayTransition = false;
                    }
                    if (this.mLogicalDisplay.getPhase() == 0 && state != 1) {
                        this.mScreenOffBecauseofDisplayTransition = true;
                        state = 1;
                    }
                    if (mMultipleDisplayFlipSupport) {
                        if (this.mLogicalDisplay.getDeviceState() == 1 || this.mLogicalDisplay.getDeviceState() == 2) {
                            if (this.mDisplayId == 1) {
                                state = 1;
                            }
                        } else if (this.mLogicalDisplay.getDeviceState() == 0 && this.mDisplayId == 0) {
                            state = 1;
                        }
                    }
                    if (mustInitialize) {
                        initialize(state);
                    }
                    ITranDisplayPowerController.Instance().setPowerRequest(this.mPowerRequest.policy);
                    int oldState = this.mPowerState.getScreenState();
                    animateScreenStateChange(state, performScreenOffTransition);
                    int state4 = this.mPowerState.getScreenState();
                    ITranDisplayPowerController.Instance().hookCurDisplayState(oldState, state4);
                    if (state4 != 1) {
                    }
                    brightnessState2 = -1.0f;
                    if (z4) {
                        rate14 = computeScreenBrightnessRate(SCREEN_ANIMATION_RATE_MINIMUM);
                    }
                    this.mBrightnessReasonTemp.setReason(5);
                    if (!mIsLucidDisabled && TRAN_LUCID_OPTIMIZATION_SUPPORT) {
                        if (this.mAutomaticBrightnessController != null && this.mPowerRequest.useAutoBrightness) {
                            this.mAutomaticBrightnessController.recoverLucidData();
                        } else if (this.mIsLucidBrightnessLuxReset) {
                            float lucidAnimationBrightness = BrightnessSynchronizer.brightnessIntToFloat(this.mBeforeLucidBrightness);
                            Slog.v(this.TAG, "lucid screen off reset to mBeforeLucidBrightness:" + this.mBeforeLucidBrightness + "(" + lucidAnimationBrightness + ")");
                            setBrightness(lucidAnimationBrightness);
                            this.mIsLucidBrightnessLuxReset = false;
                        }
                    }
                    if (state4 == 5) {
                        brightnessState2 = this.mScreenBrightnessForVr;
                        if (z4) {
                            rate14 = computeScreenBrightnessRate(SCREEN_ANIMATION_RATE_MINIMUM);
                        }
                        this.mBrightnessReasonTemp.setReason(6);
                    }
                    if (!Float.isNaN(brightnessState2) || !isValidBrightnessValue(this.mPowerRequest.screenBrightnessOverride)) {
                        this.mAppliedScreenBrightnessOverride = false;
                    } else {
                        brightnessState2 = this.mPowerRequest.screenBrightnessOverride;
                        if (z4) {
                            rate14 = computeScreenBrightnessRate(1.0f);
                        }
                        this.mBrightnessReasonTemp.setReason(7);
                        this.mAppliedScreenBrightnessOverride = true;
                        if (!mIsLucidDisabled && TRAN_LUCID_OPTIMIZATION_SUPPORT && (automaticBrightnessController3 = this.mAutomaticBrightnessController) != null) {
                            automaticBrightnessController3.recoverLucidData();
                        }
                    }
                    boolean autoBrightnessEnabledInDoze = this.mAllowAutoBrightnessWhileDozingConfig && Display.isDozeState(state4);
                    boolean autoBrightnessEnabled = this.mPowerRequest.useAutoBrightness && (state4 == 2 || autoBrightnessEnabledInDoze) && Float.isNaN(brightnessState2) && this.mAutomaticBrightnessController != null;
                    boolean autoBrightnessDisabledDueToDisplayOff = (!this.mPowerRequest.useAutoBrightness || state4 == 2 || autoBrightnessEnabledInDoze) ? false : true;
                    if (autoBrightnessEnabled) {
                        autoBrightnessState = 1;
                    } else if (autoBrightnessDisabledDueToDisplayOff) {
                        autoBrightnessState = 3;
                    } else {
                        autoBrightnessState = 2;
                    }
                    boolean userSetBrightnessChanged = updateUserSetScreenBrightness();
                    if ((state4 == 1 || this.mCurrentScreenBrightnessSetting == this.mTemporaryScreenBrightness) && isValidBrightnessValue(this.mTemporaryScreenBrightness)) {
                        this.mTemporaryScreenBrightness = Float.NaN;
                    }
                    if (!isValidBrightnessValue(this.mTemporaryScreenBrightness)) {
                        this.mAppliedTemporaryBrightness = false;
                    } else {
                        brightnessState2 = this.mTemporaryScreenBrightness;
                        this.mAppliedTemporaryBrightness = true;
                        if (z4) {
                            rate14 = computeScreenBrightnessRate(1.0f);
                        }
                        this.mBrightnessReasonTemp.setReason(8);
                        this.mIsLucidBrightnessLuxReset = false;
                    }
                    boolean autoBrightnessAdjustmentChanged = updateAutoBrightnessAdjustment();
                    if (autoBrightnessAdjustmentChanged) {
                        this.mTemporaryAutoBrightnessAdjustment = Float.NaN;
                    }
                    if (!Float.isNaN(this.mTemporaryAutoBrightnessAdjustment)) {
                        float autoBrightnessAdjustment2 = this.mTemporaryAutoBrightnessAdjustment;
                        this.mAppliedTemporaryAutoBrightnessAdjustment = true;
                        autoBrightnessAdjustment = autoBrightnessAdjustment2;
                        brightnessAdjustmentFlags = 1;
                    } else {
                        float autoBrightnessAdjustment3 = this.mAutoBrightnessAdjustment;
                        this.mAppliedTemporaryAutoBrightnessAdjustment = false;
                        autoBrightnessAdjustment = autoBrightnessAdjustment3;
                        brightnessAdjustmentFlags = 2;
                    }
                    if (!this.mPowerRequest.boostScreenBrightness || brightnessState2 == -1.0f) {
                        brightnessAdjustmentFlags2 = brightnessAdjustmentFlags;
                        this.mAppliedBrightnessBoost = false;
                    } else {
                        brightnessState2 = 1.0f;
                        if (z4) {
                            rate14 = computeScreenBrightnessRate(SCREEN_ANIMATION_RATE_MINIMUM);
                        }
                        brightnessAdjustmentFlags2 = brightnessAdjustmentFlags;
                        this.mBrightnessReasonTemp.setReason(9);
                        this.mAppliedBrightnessBoost = true;
                    }
                    boolean userInitiatedChange2 = Float.isNaN(brightnessState2) && (autoBrightnessAdjustmentChanged || userSetBrightnessChanged);
                    boolean hadUserBrightnessPoint2 = false;
                    AutomaticBrightnessController automaticBrightnessController4 = this.mAutomaticBrightnessController;
                    if (automaticBrightnessController4 == null) {
                        updateImmediately = true;
                    } else {
                        boolean hadUserBrightnessPoint3 = automaticBrightnessController4.hasUserDataPoints();
                        updateImmediately = true;
                        this.mAutomaticBrightnessController.configure(autoBrightnessState, this.mBrightnessConfiguration, this.mLastUserSetScreenBrightness, userSetBrightnessChanged, autoBrightnessAdjustment, autoBrightnessAdjustmentChanged, this.mPowerRequest.policy);
                        hadUserBrightnessPoint2 = hadUserBrightnessPoint3;
                    }
                    BrightnessTracker brightnessTracker = this.mBrightnessTracker;
                    if (brightnessTracker != null) {
                        brightnessTracker.setBrightnessConfiguration(this.mBrightnessConfiguration);
                    }
                    boolean slowChange8 = false;
                    if (z4) {
                        this.mShouldUpdateBrightnessSettingLater = false;
                    }
                    boolean updateScreenBrightnessSetting3 = false;
                    if (!Float.isNaN(brightnessState2)) {
                        brightnessState2 = clampScreenBrightness(brightnessState2);
                        this.mAppliedAutoBrightness = false;
                        brightnessAdjustmentFlags3 = 0;
                        updateImmediately2 = updateImmediately;
                    } else {
                        float newAutoBrightnessAdjustment = autoBrightnessAdjustment;
                        if (autoBrightnessEnabled) {
                            slowChange6 = false;
                            brightnessState2 = this.mAutomaticBrightnessController.getAutomaticScreenBrightness(this.mTempBrightnessEvent);
                            newAutoBrightnessAdjustment = this.mAutomaticBrightnessController.getAutomaticScreenBrightnessAdjustment();
                        } else {
                            slowChange6 = false;
                        }
                        if (!isValidBrightnessValue(brightnessState2) && brightnessState2 != -1.0f) {
                            this.mAppliedAutoBrightness = false;
                            updateScreenBrightnessSetting2 = false;
                            slowChange8 = slowChange6;
                        } else {
                            brightnessState2 = clampScreenBrightness(brightnessState2);
                            if (this.mAppliedAutoBrightness && !autoBrightnessAdjustmentChanged) {
                                slowChange7 = true;
                            } else {
                                slowChange7 = slowChange6;
                            }
                            boolean updateScreenBrightnessSetting4 = this.mCurrentScreenBrightnessSetting != brightnessState2;
                            boolean updateScreenBrightnessSetting5 = updateScreenBrightnessSetting4;
                            this.mAppliedAutoBrightness = true;
                            boolean slowChange9 = slowChange7;
                            this.mBrightnessReasonTemp.setReason(4);
                            if (z4 && rate14 < SCREEN_ANIMATION_RATE_MINIMUM) {
                                if (this.mLastBrightnessState >= brightnessState2) {
                                    rate13 = TRAN_PHYSICAL_BACKLIGHT_OPTIMIZATION_SUPPORT ? computeScreenBrightnessRate(7.5f) : computeScreenBrightnessRate(30.0f);
                                } else {
                                    rate13 = TRAN_PHYSICAL_BACKLIGHT_OPTIMIZATION_SUPPORT ? computeScreenBrightnessRate(1.5f) : computeScreenBrightnessRate(4.2f);
                                }
                                rate14 = rate13;
                                updateImmediately = false;
                                updateScreenBrightnessSetting2 = updateScreenBrightnessSetting5;
                                slowChange8 = slowChange9;
                            } else {
                                updateScreenBrightnessSetting2 = updateScreenBrightnessSetting5;
                                slowChange8 = slowChange9;
                            }
                        }
                        if (autoBrightnessAdjustment != newAutoBrightnessAdjustment) {
                            putAutoBrightnessAdjustmentSetting(newAutoBrightnessAdjustment);
                        } else {
                            brightnessAdjustmentFlags2 = 0;
                        }
                        updateScreenBrightnessSetting3 = updateScreenBrightnessSetting2;
                        brightnessAdjustmentFlags3 = brightnessAdjustmentFlags2;
                        updateImmediately2 = updateImmediately;
                    }
                    boolean slowChange10 = Float.isNaN(brightnessState2);
                    if (!slowChange10) {
                        slowChange = slowChange8;
                        rate = rate14;
                    } else if (Display.isDozeState(state4)) {
                        float autoBrightnessAdjustment4 = this.mScreenBrightnessDozeConfig;
                        float brightnessState3 = clampScreenBrightness(autoBrightnessAdjustment4);
                        if (SCREEN_ANIMATION_RATE_MINIMUM != this.mScreenBrightnessDozeConfig) {
                            slowChange = slowChange8;
                            rate12 = rate14;
                            brightnessState2 = brightnessState3;
                        } else {
                            float brightnessState4 = getScreenBrightnessSetting();
                            slowChange = slowChange8;
                            rate12 = rate14;
                            Slog.i(this.TAG, "in doze set new brightnessState:" + brightnessState4);
                            brightnessState2 = brightnessState4;
                        }
                        if (z4) {
                            rate2 = computeScreenBrightnessRate(SCREEN_ANIMATION_RATE_MINIMUM);
                        } else {
                            rate2 = rate12;
                        }
                        this.mBrightnessReasonTemp.setReason(3);
                        if (Float.isNaN(brightnessState2)) {
                            brightnessState2 = clampScreenBrightness(this.mCurrentScreenBrightnessSetting);
                            if (z4 && rate2 < SCREEN_ANIMATION_RATE_MINIMUM) {
                                rate2 = (this.mBrightnessReasonTemp.reason == 0 && this.mBrightnessReason.reason == 3) ? computeScreenBrightnessRate(SCREEN_ANIMATION_RATE_MINIMUM) : computeScreenBrightnessRate(2.0f);
                            }
                            if (brightnessState2 == this.mCurrentScreenBrightnessSetting) {
                                updateScreenBrightnessSetting = updateScreenBrightnessSetting3;
                            } else {
                                updateScreenBrightnessSetting = true;
                            }
                            updateScreenBrightnessSetting3 = updateScreenBrightnessSetting;
                            this.mBrightnessReasonTemp.setReason(1);
                        }
                        float unthrottledBrightnessState = brightnessState2;
                        if (!this.mBrightnessThrottler.isThrottled()) {
                            rate3 = rate2;
                            this.mTempBrightnessEvent.thermalMax = this.mBrightnessThrottler.getBrightnessCap();
                            brightnessState2 = Math.min(brightnessState2, this.mBrightnessThrottler.getBrightnessCap());
                            this.mBrightnessReasonTemp.addModifier(8);
                            if (this.mAppliedThrottling) {
                                slowChange2 = slowChange;
                            } else {
                                slowChange2 = false;
                            }
                            this.mAppliedThrottling = true;
                        } else {
                            rate3 = rate2;
                            if (this.mAppliedThrottling) {
                                this.mAppliedThrottling = false;
                            }
                            slowChange2 = slowChange;
                        }
                        if (updateScreenBrightnessSetting3) {
                            updateScreenBrightnessSetting(brightnessState2);
                            if (z4) {
                                this.mShouldUpdateBrightnessSettingLater = true;
                            }
                        }
                        boolean slowChange11 = slowChange2;
                        if (this.mPowerRequest.policy != 2) {
                            if (brightnessState2 <= SCREEN_ANIMATION_RATE_MINIMUM) {
                                rate4 = rate3;
                            } else {
                                float brightnessState5 = Math.max(Math.min(brightnessState2 - this.mScreenBrightnessMinimumDimAmount, this.mScreenBrightnessDimConfig), (float) SCREEN_ANIMATION_RATE_MINIMUM);
                                if (z4) {
                                    rate4 = computeScreenBrightnessRate(1.0f);
                                } else {
                                    rate4 = rate3;
                                }
                                this.mBrightnessReasonTemp.addModifier(1);
                                brightnessState2 = brightnessState5;
                            }
                            if (this.mAppliedDimming) {
                                slowChange3 = slowChange11;
                            } else {
                                slowChange3 = false;
                            }
                            this.mAppliedDimming = true;
                            brightnessState2 = brightnessState2;
                        } else {
                            boolean slowChange12 = this.mAppliedDimming;
                            if (!slowChange12) {
                                slowChange3 = slowChange11;
                                rate4 = rate3;
                            } else {
                                slowChange3 = false;
                                this.mAppliedDimming = false;
                                rate4 = rate3;
                            }
                        }
                        boolean slowChange13 = slowChange3;
                        float rate16 = rate4;
                        if (ITranDisplayPowerController.Instance().isSourceConnectPolicy(this.mPowerRequest.policy)) {
                            if (this.mPowerRequest.lowPowerMode) {
                                if (brightnessState2 <= SCREEN_ANIMATION_RATE_MINIMUM) {
                                    rate5 = rate16;
                                } else {
                                    float brightnessFactor = Math.min(this.mPowerRequest.screenLowPowerBrightnessFactor, 1.0f);
                                    float lowPowerBrightnessFloat = brightnessState2 * brightnessFactor;
                                    float brightnessState6 = Math.max(lowPowerBrightnessFloat, (float) SCREEN_ANIMATION_RATE_MINIMUM);
                                    if (z4) {
                                        rate16 = computeScreenBrightnessRate(2.0f);
                                    }
                                    this.mBrightnessReasonTemp.addModifier(2);
                                    brightnessState2 = brightnessState6;
                                    rate5 = rate16;
                                }
                                if (this.mAppliedLowPower) {
                                    slowChange4 = slowChange13;
                                } else {
                                    slowChange4 = false;
                                }
                                this.mAppliedLowPower = true;
                            } else {
                                boolean slowChange14 = this.mAppliedLowPower;
                                if (slowChange14) {
                                    slowChange4 = false;
                                    this.mAppliedLowPower = false;
                                    rate5 = rate16;
                                }
                                slowChange4 = slowChange13;
                                rate5 = rate16;
                            }
                        } else if (brightnessState2 > SCREEN_ANIMATION_RATE_MINIMUM) {
                            brightnessState2 = SCREEN_ANIMATION_RATE_MINIMUM;
                            if (z4) {
                                rate11 = computeScreenBrightnessRate(0.5f);
                            } else {
                                rate11 = rate16;
                            }
                            Slog.i(this.TAG, "sourceConnect set dim. brightnessState=" + SCREEN_ANIMATION_RATE_MINIMUM);
                            this.mBrightnessReasonTemp.addModifier(1);
                            slowChange4 = slowChange13;
                            rate5 = rate11;
                        } else {
                            slowChange4 = slowChange13;
                            rate5 = rate16;
                        }
                        float rate17 = rate5;
                        if (this.mBrightnessReasonTemp.reason == 1 && this.mBrightnessReason.reason == 5 && z4) {
                            this.mScreenOffManualAutoMatic = true;
                        }
                        if (this.mBrightnessReasonTemp.reason == 3 && this.mBrightnessReason.reason == 5 && z4) {
                            this.mScreenOffDozeDefaultAutoMatic = true;
                        }
                        if (!z4 && this.mBrightnessReasonTemp.reason == 4 && this.mBrightnessReason.reason != 4) {
                            if (this.mScreenOffManualAutoMatic) {
                                if (!this.mPrevScreenOffBecauseOfProximity || this.mScreenOffBecauseOfProximity) {
                                    rate6 = computeScreenBrightnessRate(TRAN_LIGHTSENSOR_OPTIMIZATION_SUPPORT ? 0.1f : 1.6f);
                                } else {
                                    rate6 = computeScreenBrightnessRate(SCREEN_ANIMATION_RATE_MINIMUM);
                                }
                            } else {
                                rate6 = this.mScreenOffDozeDefaultAutoMatic ? computeScreenBrightnessRate(0.6f) : computeScreenBrightnessRate(4.0f);
                            }
                            this.mAppAutoEnableProcessing = true;
                        } else {
                            rate6 = rate17;
                        }
                        if (!z4) {
                            rate7 = rate6;
                            if (this.mBrightnessReasonTemp.reason == 1 && this.mBrightnessReason.reason == 4) {
                                this.mAutomaticBrightnessController.resetShortTermModel();
                            }
                        } else {
                            rate7 = rate6;
                        }
                        boolean isLucidTempDisabled2 = "1".equals(SystemProperties.get("persist.sys.tran.lucid.disabled"));
                        z = mIsLucidDisabled;
                        if (!z || !TRAN_LUCID_OPTIMIZATION_SUPPORT || isLucidTempDisabled2) {
                            brightnessAdjustmentFlags4 = brightnessAdjustmentFlags3;
                            previousPolicy2 = previousPolicy;
                            mustNotify = mustNotify2;
                            hadUserBrightnessPoint = hadUserBrightnessPoint2;
                        } else {
                            int brightness = BrightnessSynchronizer.brightnessFloatToInt(brightnessState2);
                            previousPolicy2 = previousPolicy;
                            if (this.mIsLucidBrightnessSet) {
                                if (this.mPreLucidBrightness != brightness || (i3 = this.mBeforeLucidBrightness) <= brightness) {
                                    brightnessAdjustmentFlags4 = brightnessAdjustmentFlags3;
                                    mustNotify = mustNotify2;
                                    hadUserBrightnessPoint = hadUserBrightnessPoint2;
                                } else {
                                    this.mLucidAnimationBrightness = i3;
                                    float lucidAnimationBrightness2 = BrightnessSynchronizer.brightnessIntToFloat(i3);
                                    mustNotify = mustNotify2;
                                    brightnessAdjustmentFlags4 = brightnessAdjustmentFlags3;
                                    hadUserBrightnessPoint = hadUserBrightnessPoint2;
                                    Slog.v(this.TAG, "lucid set not apply! reset Brightness:" + brightness + "(" + brightnessState2 + ") to mBeforeLucidBrightness:" + this.mBeforeLucidBrightness + "(" + lucidAnimationBrightness2 + ")");
                                    setBrightness(lucidAnimationBrightness2);
                                    this.mIsLucidBrightnessLuxReset = false;
                                }
                            } else {
                                brightnessAdjustmentFlags4 = brightnessAdjustmentFlags3;
                                mustNotify = mustNotify2;
                                hadUserBrightnessPoint = hadUserBrightnessPoint2;
                                if (this.mIsLucidUpdate && (this.mBrightnessReasonTemp.reason == 1 || this.mBrightnessReasonTemp.reason == 4 || this.mBrightnessReasonTemp.reason == 8)) {
                                    this.mPreLucidBrightness = brightness;
                                    this.mLucidAnimationBrightness = -1;
                                    if (brightness > 0 && brightness < 250) {
                                        if (this.mAfterLucidBrightness == brightness) {
                                            this.mAfterLucidBrightness = calcLucidBright(this.mBeforeLucidBrightness);
                                            Slog.v(this.TAG, "Brightness mAfterLucidBrightness before lucid:" + brightness + " after lucid:" + this.mAfterLucidBrightness + " mBeforeLucidBrightness:" + this.mBeforeLucidBrightness);
                                        } else {
                                            this.mBeforeLucidBrightness = brightness;
                                            int calcLucidBright = calcLucidBright(brightness);
                                            this.mAfterLucidBrightness = calcLucidBright;
                                            int i4 = this.mBeforeLucidBrightness;
                                            if (calcLucidBright > i4) {
                                                this.mAfterLucidBrightness = i4;
                                            }
                                            Slog.v(this.TAG, "Brightness before lucid:" + this.mBeforeLucidBrightness + " Brightness after lucid:" + this.mAfterLucidBrightness);
                                        }
                                        int i5 = this.mPreLucidBrightness;
                                        int i6 = this.mAfterLucidBrightness;
                                        if (i5 != i6) {
                                            this.mLucidAnimationBrightness = i6;
                                            float lucidAnimationBrightness3 = BrightnessSynchronizer.brightnessIntToFloat(i6);
                                            Slog.v(this.TAG, "lucid Brightness:" + brightness + "(" + brightnessState2 + ") to mLucidAnimationBrightness:" + this.mLucidAnimationBrightness + "(" + lucidAnimationBrightness3 + ")");
                                            setBrightness(lucidAnimationBrightness3);
                                            this.mIsLucidBrightnessLuxReset = true;
                                            this.mIsLucidBrightnessSet = true;
                                            if (this.mBrightnessReason.reason == 1 || this.mBrightnessReason.reason == 4 || this.mBrightnessReason.reason == 8) {
                                                this.mLucidBeginAnimation = true;
                                            }
                                            AutomaticBrightnessController automaticBrightnessController5 = this.mAutomaticBrightnessController;
                                            if (automaticBrightnessController5 != null && autoBrightnessEnabled && this.mAfterLucidBrightness != this.mBeforeLucidBrightness) {
                                                automaticBrightnessController5.saveLucidData(brightnessState2, lucidAnimationBrightness3);
                                            }
                                        }
                                    }
                                    this.mIsLucidUpdate = false;
                                } else {
                                    int i7 = this.mLucidAnimationBrightness;
                                    if (i7 > 0 && brightness == i7 && this.mLucidBeginAnimation) {
                                        Slog.v(this.TAG, "lucid animation Brightness:" + brightness + "(" + brightnessState2 + ")");
                                        isLucidTempDisabled = true;
                                        this.mHbmController.onBrightnessChanged(brightnessState2, unthrottledBrightnessState, this.mBrightnessThrottler.getBrightnessMaxReason());
                                        if (this.mPendingScreenOff) {
                                            state2 = state4;
                                            brightnessState = brightnessState2;
                                            isDisplayContentVisible = saveBrightnessInfo(getScreenBrightnessSetting());
                                        } else {
                                            if (this.mSkipScreenOnBrightnessRamp) {
                                                if (state4 != 2) {
                                                    this.mSkipRampState = 0;
                                                } else {
                                                    int i8 = this.mSkipRampState;
                                                    if (i8 == 0 && this.mDozing) {
                                                        this.mInitialAutoBrightness = brightnessState2;
                                                        this.mSkipRampState = 1;
                                                    } else {
                                                        if (i8 != 1 || !this.mUseSoftwareAutoBrightnessConfig) {
                                                            i2 = 2;
                                                        } else if (!BrightnessSynchronizer.floatEquals(brightnessState2, this.mInitialAutoBrightness)) {
                                                            this.mSkipRampState = 2;
                                                        } else {
                                                            i2 = 2;
                                                        }
                                                        if (this.mSkipRampState == i2) {
                                                            this.mSkipRampState = 0;
                                                        }
                                                    }
                                                }
                                            }
                                            boolean wasOrWillBeInVr = state4 == 5 || oldState == 5;
                                            boolean initialRampSkip = state4 == 2 && this.mSkipRampState != 0;
                                            boolean hasBrightnessBuckets = Display.isDozeState(state4) && this.mBrightnessBucketsInDozeConfig;
                                            boolean brightnessAdjusted = this.mColorFadeEnabled;
                                            boolean isDisplayContentVisible2 = brightnessAdjusted && this.mPowerState.getColorFadeLevel() == 1.0f;
                                            boolean brightnessIsTemporary = this.mAppliedTemporaryBrightness || this.mAppliedTemporaryAutoBrightnessAdjustment;
                                            float animateValue2 = clampScreenBrightness(brightnessState2);
                                            state2 = state4;
                                            if (this.mHbmController.getHighBrightnessMode() == 2 && ((this.mBrightnessReason.modifier & 1) == 0 || (this.mBrightnessReason.modifier & 2) == 0)) {
                                                animateValue = this.mHbmController.getHdrBrightnessValue();
                                            } else {
                                                animateValue = animateValue2;
                                            }
                                            float currentBrightness = this.mPowerState.getScreenBrightness();
                                            boolean userInitiatedChange3 = userInitiatedChange2;
                                            float currentSdrBrightness2 = this.mPowerState.getSdrScreenBrightness();
                                            float brightnessState7 = brightnessState2;
                                            if (!TRAN_TEMP_BACKLIGHT_SUPPORT) {
                                                currentSdrBrightness = currentSdrBrightness2;
                                                slowChange5 = slowChange4;
                                                isLucidAnimation = isLucidTempDisabled;
                                                rate8 = rate7;
                                            } else {
                                                slowChange5 = slowChange4;
                                                if (this.mIsThresholdBrightnessTemp) {
                                                    boolean z6 = isLucidTempDisabled;
                                                    if (animateValue > 0.9d) {
                                                        this.mBrightnessFactor = 0.9f;
                                                        animateValue = this.mBrightnessFactor;
                                                        float rate18 = computeScreenBrightnessRate(6.0f);
                                                        currentSdrBrightness = currentSdrBrightness2;
                                                        Slog.i(this.TAG, "Temp above 42 set mBrightnessFactor to  0.9 animateValue=" + animateValue);
                                                        rate8 = rate18;
                                                        isLucidAnimation3 = z6;
                                                        isLucidAnimation = isLucidAnimation3;
                                                        if (this.mIsRestBrightnessFactorTemp) {
                                                            isLucidAnimation = isLucidAnimation3;
                                                            if (this.mBrightnessFactor == 0.9f) {
                                                                this.mBrightnessFactor = 1.0f;
                                                                this.mIsThresholdBrightnessTemp = false;
                                                                this.mIsRestBrightnessFactorTemp = false;
                                                                float rate19 = computeScreenBrightnessRate(6.0f);
                                                                Slog.i(this.TAG, "Temp below 37 reset mBrightnessFactor to 1.0 animateValue=" + animateValue);
                                                                rate8 = rate19;
                                                                isLucidAnimation = isLucidAnimation3;
                                                            }
                                                        }
                                                    } else {
                                                        currentSdrBrightness = currentSdrBrightness2;
                                                        isLucidAnimation2 = z6;
                                                    }
                                                } else {
                                                    currentSdrBrightness = currentSdrBrightness2;
                                                    isLucidAnimation2 = isLucidTempDisabled;
                                                }
                                                rate8 = rate7;
                                                isLucidAnimation3 = isLucidAnimation2;
                                                isLucidAnimation = isLucidAnimation3;
                                                if (this.mIsRestBrightnessFactorTemp) {
                                                }
                                            }
                                            if (this.mDisplayId != 0) {
                                                rate9 = rate8;
                                            } else {
                                                float fAmbientLux = -1.0f;
                                                if (autoBrightnessEnabled) {
                                                    fAmbientLux = this.mAutomaticBrightnessController.getAmbientLux();
                                                }
                                                if (!DEBUG) {
                                                    rate9 = rate8;
                                                } else {
                                                    rate9 = rate8;
                                                    Slog.i(this.TAG, "mAutomaticBrightnessController.getAmbientLux()" + fAmbientLux);
                                                }
                                                float brightnessUnderBacklightTemperatureControl = ITranDisplayPowerController.Instance().calculateBrightnessUnderBacklightTemperatureControl(animateValue, fAmbientLux);
                                                if (isValidBrightnessValue(brightnessUnderBacklightTemperatureControl)) {
                                                    animateValue = brightnessUnderBacklightTemperatureControl;
                                                    this.mBrightnessFactor = brightnessUnderBacklightTemperatureControl;
                                                    rate10 = computeScreenBrightnessRate(6.0f);
                                                    if (!z4) {
                                                        if (isValidBrightnessValue(animateValue)) {
                                                            if (initialRampSkip || hasBrightnessBuckets || wasOrWillBeInVr || !isDisplayContentVisible2 || brightnessIsTemporary) {
                                                                animateScreenBrightness(animateValue, rate10, updateImmediately2);
                                                            } else if (isLucidAnimation && !z && TRAN_LUCID_OPTIMIZATION_SUPPORT) {
                                                                animateScreenBrightness(animateValue, 0.02f, updateImmediately2);
                                                            } else {
                                                                animateScreenBrightness(animateValue, rate10, updateImmediately2);
                                                            }
                                                        }
                                                    } else if (isValidBrightnessValue(animateValue)) {
                                                        float sdrAnimateValue = animateValue == currentBrightness ? animateValue2 : animateValue2;
                                                        if (initialRampSkip || hasBrightnessBuckets || wasOrWillBeInVr || !isDisplayContentVisible2 || brightnessIsTemporary) {
                                                            animateScreenBrightness(animateValue, sdrAnimateValue, SCREEN_ANIMATION_RATE_MINIMUM);
                                                        } else {
                                                            boolean isIncreasing = animateValue > currentBrightness;
                                                            if (isLucidAnimation && !z && TRAN_LUCID_OPTIMIZATION_SUPPORT) {
                                                                rampSpeed = 0.02f;
                                                            } else if (isIncreasing && slowChange5) {
                                                                rampSpeed = this.mBrightnessRampRateSlowIncrease;
                                                            } else if (isIncreasing && !slowChange5) {
                                                                rampSpeed = this.mBrightnessRampRateFastIncrease;
                                                            } else if (!isIncreasing && slowChange5) {
                                                                rampSpeed = this.mBrightnessRampRateSlowDecrease;
                                                            } else {
                                                                rampSpeed = this.mBrightnessRampRateFastDecrease;
                                                            }
                                                            animateScreenBrightness(animateValue, sdrAnimateValue, rampSpeed);
                                                        }
                                                    }
                                                    if (z4) {
                                                        brightnessState = brightnessState7;
                                                    } else {
                                                        brightnessState = brightnessState7;
                                                        this.mLastBrightnessState = brightnessState;
                                                    }
                                                    if (brightnessIsTemporary && (automaticBrightnessController = this.mAutomaticBrightnessController) != null) {
                                                        if (!automaticBrightnessController.isInIdleMode()) {
                                                            if (userInitiatedChange3 && ((automaticBrightnessController2 = this.mAutomaticBrightnessController) == null || !automaticBrightnessController2.hasValidAmbientLux())) {
                                                                userInitiatedChange = false;
                                                            } else {
                                                                userInitiatedChange = userInitiatedChange3;
                                                            }
                                                            notifyBrightnessTrackerChanged(brightnessState, userInitiatedChange, hadUserBrightnessPoint);
                                                        }
                                                    }
                                                    boolean brightnessAdjusted2 = saveBrightnessInfo(getScreenBrightnessSetting(), animateValue);
                                                    isDisplayContentVisible = brightnessAdjusted2;
                                                }
                                            }
                                            rate10 = rate9;
                                            if (!z4) {
                                            }
                                            if (z4) {
                                            }
                                            if (brightnessIsTemporary) {
                                            }
                                            boolean brightnessAdjusted22 = saveBrightnessInfo(getScreenBrightnessSetting(), animateValue);
                                            isDisplayContentVisible = brightnessAdjusted22;
                                        }
                                        if (isDisplayContentVisible) {
                                            postBrightnessChangeRunnable();
                                        }
                                        if (this.mDisplayId != 0) {
                                            state3 = state2;
                                        } else {
                                            state3 = state2;
                                            ITranDisplayPowerController.Instance().setTranBacklightTemperatureControllerScreenState(state3);
                                        }
                                        if (this.mBrightnessReasonTemp.equals(this.mBrightnessReason) || brightnessAdjustmentFlags4 != 0) {
                                            brightnessAdjustmentFlags5 = brightnessAdjustmentFlags4;
                                            Slog.v(this.TAG, "Brightness [" + brightnessState + "] reason changing to: '" + this.mBrightnessReasonTemp.toString(brightnessAdjustmentFlags5) + "', previous reason: '" + this.mBrightnessReason + "'.");
                                            boolean z7 = this.mBrightnessReasonTemp.reason != 1 && this.mBrightnessReason.reason == 8;
                                            this.mEventTrackFlag = z7;
                                            i = 4;
                                            this.mEventTrackFlag = z7 | (this.mBrightnessReasonTemp.reason != 4 && this.mBrightnessReason.reason == 8);
                                            this.mBrightnessReason.set(this.mBrightnessReasonTemp);
                                        } else if (this.mBrightnessReasonTemp.reason != 1 || !userSetBrightnessChanged) {
                                            brightnessAdjustmentFlags5 = brightnessAdjustmentFlags4;
                                            i = 4;
                                        } else {
                                            Slog.v(this.TAG, "Brightness [" + brightnessState + "] manual adjustment.");
                                            brightnessAdjustmentFlags5 = brightnessAdjustmentFlags4;
                                            i = 4;
                                        }
                                        this.mTempBrightnessEvent.time = System.currentTimeMillis();
                                        this.mTempBrightnessEvent.brightness = brightnessState;
                                        this.mTempBrightnessEvent.reason.set(this.mBrightnessReason);
                                        this.mTempBrightnessEvent.hbmMax = this.mHbmController.getCurrentBrightnessMax();
                                        this.mTempBrightnessEvent.hbmMode = this.mHbmController.getHighBrightnessMode();
                                        this.mTempBrightnessEvent.flags |= this.mIsRbcActive ? 1 : 0;
                                        boolean tempToTempTransition = this.mTempBrightnessEvent.reason.reason != 8 && this.mLastBrightnessEvent.reason.reason == 8;
                                        if ((!this.mTempBrightnessEvent.equalsMainData(this.mLastBrightnessEvent) && !tempToTempTransition) || brightnessAdjustmentFlags5 != 0) {
                                            this.mLastBrightnessEvent.copyFrom(this.mTempBrightnessEvent);
                                            BrightnessEvent newEvent = new BrightnessEvent(this.mTempBrightnessEvent);
                                            newEvent.adjustmentFlags = brightnessAdjustmentFlags5;
                                            int i9 = newEvent.flags;
                                            if (!userSetBrightnessChanged) {
                                                i = 0;
                                            }
                                            newEvent.flags = i | i9;
                                            Slog.i(this.TAG, newEvent.toString(false));
                                            ringBuffer = this.mBrightnessEventRingBuffer;
                                            if (ringBuffer != null) {
                                                ringBuffer.append(newEvent);
                                            }
                                        }
                                        if (this.mDisplayWhiteBalanceController != null) {
                                            if (state3 == 2 && this.mDisplayWhiteBalanceSettings.isEnabled()) {
                                                this.mDisplayWhiteBalanceController.setEnabled(true);
                                                this.mDisplayWhiteBalanceController.updateDisplayColorTemperature();
                                            } else {
                                                this.mDisplayWhiteBalanceController.setEnabled(false);
                                            }
                                        }
                                        ready = this.mPendingScreenOnUnblocker != null && !(this.mColorFadeEnabled && (this.mColorFadeOnAnimator.isStarted() || this.mColorFadeOffAnimator.isStarted())) && this.mPowerState.waitUntilClean(this.mCleanListener);
                                        finished = (ready || this.mScreenBrightnessRampAnimator.isAnimating()) ? false : true;
                                        if (ready && state3 != 1 && this.mReportedScreenStateToPolicy == 1) {
                                            setReportedScreenState(2);
                                            if (DEBUG_FOLD) {
                                                Slog.d(this.TAG, "mWindowManagerPolicy.screenTurnedOn() for new request.");
                                            }
                                            this.mWindowManagerPolicy.screenTurnedOn(this.mDisplayId);
                                        }
                                        if (!finished && !this.mUnfinishedBusiness) {
                                            if (DEBUG) {
                                                Slog.d(this.TAG, "Unfinished business...");
                                            }
                                            this.mCallbacks.acquireSuspendBlocker(this.mSuspendBlockerIdUnfinishedBusiness);
                                            this.mUnfinishedBusiness = true;
                                        }
                                        if (ready || !mustNotify) {
                                            z2 = true;
                                        } else {
                                            synchronized (this.mLock) {
                                                if (this.mPendingRequestChangedLocked) {
                                                    z2 = true;
                                                } else {
                                                    z2 = true;
                                                    this.mDisplayReadyLocked = true;
                                                    if (DEBUG) {
                                                        Slog.d(this.TAG, "Display ready!");
                                                    }
                                                }
                                            }
                                            sendOnStateChangedWithWakelock();
                                        }
                                        if (finished || !this.mUnfinishedBusiness) {
                                            z3 = false;
                                        } else {
                                            if (DEBUG) {
                                                Slog.d(this.TAG, "Finished business...");
                                            }
                                            eventTrackLcm(brightnessState);
                                            z3 = false;
                                            this.mUnfinishedBusiness = false;
                                            this.mCallbacks.releaseSuspendBlocker(this.mSuspendBlockerIdUnfinishedBusiness);
                                        }
                                        if (state3 == 2) {
                                            z2 = z3;
                                        }
                                        this.mDozing = z2;
                                        if (previousPolicy2 == this.mPowerRequest.policy) {
                                            logDisplayPolicyChanged(this.mPowerRequest.policy);
                                            return;
                                        }
                                        return;
                                    }
                                    this.mLucidAnimationBrightness = -1;
                                    this.mLucidBeginAnimation = false;
                                }
                            }
                        }
                        isLucidTempDisabled = false;
                        this.mHbmController.onBrightnessChanged(brightnessState2, unthrottledBrightnessState, this.mBrightnessThrottler.getBrightnessMaxReason());
                        if (this.mPendingScreenOff) {
                        }
                        if (isDisplayContentVisible) {
                        }
                        if (this.mDisplayId != 0) {
                        }
                        if (this.mBrightnessReasonTemp.equals(this.mBrightnessReason)) {
                        }
                        brightnessAdjustmentFlags5 = brightnessAdjustmentFlags4;
                        Slog.v(this.TAG, "Brightness [" + brightnessState + "] reason changing to: '" + this.mBrightnessReasonTemp.toString(brightnessAdjustmentFlags5) + "', previous reason: '" + this.mBrightnessReason + "'.");
                        if (this.mBrightnessReasonTemp.reason != 1) {
                        }
                        this.mEventTrackFlag = z7;
                        i = 4;
                        this.mEventTrackFlag = z7 | (this.mBrightnessReasonTemp.reason != 4 && this.mBrightnessReason.reason == 8);
                        this.mBrightnessReason.set(this.mBrightnessReasonTemp);
                        this.mTempBrightnessEvent.time = System.currentTimeMillis();
                        this.mTempBrightnessEvent.brightness = brightnessState;
                        this.mTempBrightnessEvent.reason.set(this.mBrightnessReason);
                        this.mTempBrightnessEvent.hbmMax = this.mHbmController.getCurrentBrightnessMax();
                        this.mTempBrightnessEvent.hbmMode = this.mHbmController.getHighBrightnessMode();
                        this.mTempBrightnessEvent.flags |= this.mIsRbcActive ? 1 : 0;
                        boolean tempToTempTransition2 = this.mTempBrightnessEvent.reason.reason != 8 && this.mLastBrightnessEvent.reason.reason == 8;
                        if (!this.mTempBrightnessEvent.equalsMainData(this.mLastBrightnessEvent)) {
                            this.mLastBrightnessEvent.copyFrom(this.mTempBrightnessEvent);
                            BrightnessEvent newEvent2 = new BrightnessEvent(this.mTempBrightnessEvent);
                            newEvent2.adjustmentFlags = brightnessAdjustmentFlags5;
                            int i92 = newEvent2.flags;
                            if (!userSetBrightnessChanged) {
                            }
                            newEvent2.flags = i | i92;
                            Slog.i(this.TAG, newEvent2.toString(false));
                            ringBuffer = this.mBrightnessEventRingBuffer;
                            if (ringBuffer != null) {
                            }
                            if (this.mDisplayWhiteBalanceController != null) {
                            }
                            ready = this.mPendingScreenOnUnblocker != null && !(this.mColorFadeEnabled && (this.mColorFadeOnAnimator.isStarted() || this.mColorFadeOffAnimator.isStarted())) && this.mPowerState.waitUntilClean(this.mCleanListener);
                            finished = (ready || this.mScreenBrightnessRampAnimator.isAnimating()) ? false : true;
                            if (ready) {
                                setReportedScreenState(2);
                                if (DEBUG_FOLD) {
                                }
                                this.mWindowManagerPolicy.screenTurnedOn(this.mDisplayId);
                            }
                            if (!finished) {
                                if (DEBUG) {
                                }
                                this.mCallbacks.acquireSuspendBlocker(this.mSuspendBlockerIdUnfinishedBusiness);
                                this.mUnfinishedBusiness = true;
                            }
                            if (ready) {
                            }
                            z2 = true;
                            if (finished) {
                            }
                            z3 = false;
                            if (state3 == 2) {
                            }
                            this.mDozing = z2;
                            if (previousPolicy2 == this.mPowerRequest.policy) {
                            }
                        }
                        this.mLastBrightnessEvent.copyFrom(this.mTempBrightnessEvent);
                        BrightnessEvent newEvent22 = new BrightnessEvent(this.mTempBrightnessEvent);
                        newEvent22.adjustmentFlags = brightnessAdjustmentFlags5;
                        int i922 = newEvent22.flags;
                        if (!userSetBrightnessChanged) {
                        }
                        newEvent22.flags = i | i922;
                        Slog.i(this.TAG, newEvent22.toString(false));
                        ringBuffer = this.mBrightnessEventRingBuffer;
                        if (ringBuffer != null) {
                        }
                        if (this.mDisplayWhiteBalanceController != null) {
                        }
                        ready = this.mPendingScreenOnUnblocker != null && !(this.mColorFadeEnabled && (this.mColorFadeOnAnimator.isStarted() || this.mColorFadeOffAnimator.isStarted())) && this.mPowerState.waitUntilClean(this.mCleanListener);
                        finished = (ready || this.mScreenBrightnessRampAnimator.isAnimating()) ? false : true;
                        if (ready) {
                        }
                        if (!finished) {
                        }
                        if (ready) {
                        }
                        z2 = true;
                        if (finished) {
                        }
                        z3 = false;
                        if (state3 == 2) {
                        }
                        this.mDozing = z2;
                        if (previousPolicy2 == this.mPowerRequest.policy) {
                        }
                    } else {
                        slowChange = slowChange8;
                        rate = rate14;
                    }
                    rate2 = rate;
                    if (Float.isNaN(brightnessState2)) {
                    }
                    float unthrottledBrightnessState2 = brightnessState2;
                    if (!this.mBrightnessThrottler.isThrottled()) {
                    }
                    if (updateScreenBrightnessSetting3) {
                    }
                    boolean slowChange112 = slowChange2;
                    if (this.mPowerRequest.policy != 2) {
                    }
                    boolean slowChange132 = slowChange3;
                    float rate162 = rate4;
                    if (ITranDisplayPowerController.Instance().isSourceConnectPolicy(this.mPowerRequest.policy)) {
                    }
                    float rate172 = rate5;
                    if (this.mBrightnessReasonTemp.reason == 1) {
                        this.mScreenOffManualAutoMatic = true;
                    }
                    if (this.mBrightnessReasonTemp.reason == 3) {
                        this.mScreenOffDozeDefaultAutoMatic = true;
                    }
                    if (!z4) {
                    }
                    rate6 = rate172;
                    if (!z4) {
                    }
                    boolean isLucidTempDisabled22 = "1".equals(SystemProperties.get("persist.sys.tran.lucid.disabled"));
                    z = mIsLucidDisabled;
                    if (!z) {
                    }
                    brightnessAdjustmentFlags4 = brightnessAdjustmentFlags3;
                    previousPolicy2 = previousPolicy;
                    mustNotify = mustNotify2;
                    hadUserBrightnessPoint = hadUserBrightnessPoint2;
                    isLucidTempDisabled = false;
                    this.mHbmController.onBrightnessChanged(brightnessState2, unthrottledBrightnessState2, this.mBrightnessThrottler.getBrightnessMaxReason());
                    if (this.mPendingScreenOff) {
                    }
                    if (isDisplayContentVisible) {
                    }
                    if (this.mDisplayId != 0) {
                    }
                    if (this.mBrightnessReasonTemp.equals(this.mBrightnessReason)) {
                    }
                    brightnessAdjustmentFlags5 = brightnessAdjustmentFlags4;
                    Slog.v(this.TAG, "Brightness [" + brightnessState + "] reason changing to: '" + this.mBrightnessReasonTemp.toString(brightnessAdjustmentFlags5) + "', previous reason: '" + this.mBrightnessReason + "'.");
                    if (this.mBrightnessReasonTemp.reason != 1) {
                    }
                    this.mEventTrackFlag = z7;
                    i = 4;
                    this.mEventTrackFlag = z7 | (this.mBrightnessReasonTemp.reason != 4 && this.mBrightnessReason.reason == 8);
                    this.mBrightnessReason.set(this.mBrightnessReasonTemp);
                    this.mTempBrightnessEvent.time = System.currentTimeMillis();
                    this.mTempBrightnessEvent.brightness = brightnessState;
                    this.mTempBrightnessEvent.reason.set(this.mBrightnessReason);
                    this.mTempBrightnessEvent.hbmMax = this.mHbmController.getCurrentBrightnessMax();
                    this.mTempBrightnessEvent.hbmMode = this.mHbmController.getHighBrightnessMode();
                    this.mTempBrightnessEvent.flags |= this.mIsRbcActive ? 1 : 0;
                    boolean tempToTempTransition22 = this.mTempBrightnessEvent.reason.reason != 8 && this.mLastBrightnessEvent.reason.reason == 8;
                    if (!this.mTempBrightnessEvent.equalsMainData(this.mLastBrightnessEvent)) {
                    }
                    this.mLastBrightnessEvent.copyFrom(this.mTempBrightnessEvent);
                    BrightnessEvent newEvent222 = new BrightnessEvent(this.mTempBrightnessEvent);
                    newEvent222.adjustmentFlags = brightnessAdjustmentFlags5;
                    int i9222 = newEvent222.flags;
                    if (!userSetBrightnessChanged) {
                    }
                    newEvent222.flags = i | i9222;
                    Slog.i(this.TAG, newEvent222.toString(false));
                    ringBuffer = this.mBrightnessEventRingBuffer;
                    if (ringBuffer != null) {
                    }
                    if (this.mDisplayWhiteBalanceController != null) {
                    }
                    ready = this.mPendingScreenOnUnblocker != null && !(this.mColorFadeEnabled && (this.mColorFadeOnAnimator.isStarted() || this.mColorFadeOffAnimator.isStarted())) && this.mPowerState.waitUntilClean(this.mCleanListener);
                    finished = (ready || this.mScreenBrightnessRampAnimator.isAnimating()) ? false : true;
                    if (ready) {
                    }
                    if (!finished) {
                    }
                    if (ready) {
                    }
                    z2 = true;
                    if (finished) {
                    }
                    z3 = false;
                    if (state3 == 2) {
                    }
                    this.mDozing = z2;
                    if (previousPolicy2 == this.mPowerRequest.policy) {
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    @Override // com.android.server.display.AutomaticBrightnessController.Callbacks
    public void updateBrightness() {
        m3417x12b1bc01();
    }

    @Override // com.android.server.display.AutomaticBrightnessController.Callbacks
    public boolean proximityPositiveToNegative() {
        if (TRAN_LIGHTSENSOR_OPTIMIZATION_SUPPORT) {
            return this.mProximityPositiveToNegative;
        }
        return true;
    }

    public void ignoreProximitySensorUntilChanged() {
        this.mHandler.sendEmptyMessage(8);
    }

    public void setBrightnessConfiguration(BrightnessConfiguration c) {
        Message msg = this.mHandler.obtainMessage(5, c);
        msg.sendToTarget();
    }

    public void setTemporaryBrightness(float brightness) {
        Message msg = this.mHandler.obtainMessage(6, Float.floatToIntBits(brightness), 0);
        msg.sendToTarget();
    }

    public void setTemporaryAutoBrightnessAdjustment(float adjustment) {
        Message msg = this.mHandler.obtainMessage(7, Float.floatToIntBits(adjustment), 0);
        msg.sendToTarget();
    }

    public BrightnessInfo getBrightnessInfo() {
        BrightnessInfo brightnessInfo;
        synchronized (this.mCachedBrightnessInfo) {
            brightnessInfo = new BrightnessInfo(this.mCachedBrightnessInfo.brightness.value, this.mCachedBrightnessInfo.adjustedBrightness.value, this.mCachedBrightnessInfo.brightnessMin.value, this.mCachedBrightnessInfo.brightnessMax.value, this.mCachedBrightnessInfo.hbmMode.value, this.mCachedBrightnessInfo.hbmTransitionPoint.value, this.mCachedBrightnessInfo.brightnessMaxReason.value);
        }
        return brightnessInfo;
    }

    private boolean saveBrightnessInfo(float brightness) {
        return saveBrightnessInfo(brightness, brightness);
    }

    private boolean saveBrightnessInfo(float brightness, float adjustedBrightness) {
        boolean changed;
        synchronized (this.mCachedBrightnessInfo) {
            float minBrightness = Math.min(this.mHbmController.getCurrentBrightnessMin(), this.mBrightnessThrottler.getBrightnessCap());
            float maxBrightness = Math.min(this.mHbmController.getCurrentBrightnessMax(), this.mBrightnessThrottler.getBrightnessCap());
            CachedBrightnessInfo cachedBrightnessInfo = this.mCachedBrightnessInfo;
            boolean changed2 = false | cachedBrightnessInfo.checkAndSetFloat(cachedBrightnessInfo.brightness, brightness);
            CachedBrightnessInfo cachedBrightnessInfo2 = this.mCachedBrightnessInfo;
            boolean changed3 = changed2 | cachedBrightnessInfo2.checkAndSetFloat(cachedBrightnessInfo2.adjustedBrightness, adjustedBrightness);
            CachedBrightnessInfo cachedBrightnessInfo3 = this.mCachedBrightnessInfo;
            boolean changed4 = changed3 | cachedBrightnessInfo3.checkAndSetFloat(cachedBrightnessInfo3.brightnessMin, minBrightness);
            CachedBrightnessInfo cachedBrightnessInfo4 = this.mCachedBrightnessInfo;
            boolean changed5 = changed4 | cachedBrightnessInfo4.checkAndSetFloat(cachedBrightnessInfo4.brightnessMax, maxBrightness);
            CachedBrightnessInfo cachedBrightnessInfo5 = this.mCachedBrightnessInfo;
            boolean changed6 = changed5 | cachedBrightnessInfo5.checkAndSetInt(cachedBrightnessInfo5.hbmMode, this.mHbmController.getHighBrightnessMode());
            CachedBrightnessInfo cachedBrightnessInfo6 = this.mCachedBrightnessInfo;
            boolean changed7 = changed6 | cachedBrightnessInfo6.checkAndSetFloat(cachedBrightnessInfo6.hbmTransitionPoint, this.mHbmController.getTransitionPoint());
            CachedBrightnessInfo cachedBrightnessInfo7 = this.mCachedBrightnessInfo;
            changed = changed7 | cachedBrightnessInfo7.checkAndSetInt(cachedBrightnessInfo7.brightnessMaxReason, this.mBrightnessThrottler.getBrightnessMaxReason());
        }
        return changed;
    }

    void postBrightnessChangeRunnable() {
        this.mHandler.post(this.mOnBrightnessChangeRunnable);
    }

    private HighBrightnessModeController createHbmControllerLocked() {
        float pmMinBrightness;
        DisplayDevice device = this.mLogicalDisplay.getPrimaryDisplayDeviceLocked();
        DisplayDeviceConfig ddConfig = device.getDisplayDeviceConfig();
        IBinder displayToken = this.mLogicalDisplay.getPrimaryDisplayDeviceLocked().getDisplayTokenLocked();
        String displayUniqueId = this.mLogicalDisplay.getPrimaryDisplayDeviceLocked().getUniqueId();
        DisplayDeviceConfig.HighBrightnessModeData hbmData = ddConfig != null ? ddConfig.getHighBrightnessModeData() : null;
        DisplayDeviceInfo info = device.getDisplayDeviceInfoLocked();
        PowerManager pm = (PowerManager) this.mContext.getSystemService(PowerManager.class);
        float pmMinBrightness2 = pm.getBrightnessConstraint(0);
        if (pmMinBrightness2 >= SCREEN_ANIMATION_RATE_MINIMUM) {
            pmMinBrightness = pmMinBrightness2;
        } else {
            pmMinBrightness = 0.0f;
        }
        Slog.i(this.TAG, "createHbmControllerLocked pmMinBrightness=" + pmMinBrightness);
        return new HighBrightnessModeController(this.mHandler, info.width, info.height, displayToken, displayUniqueId, pmMinBrightness, 1.0f, hbmData, new HighBrightnessModeController.HdrBrightnessDeviceConfig() { // from class: com.android.server.display.DisplayPowerController.6
            @Override // com.android.server.display.HighBrightnessModeController.HdrBrightnessDeviceConfig
            public float getHdrBrightnessFromSdr(float sdrBrightness) {
                return DisplayPowerController.this.mDisplayDeviceConfig.getHdrBrightnessFromSdr(sdrBrightness);
            }
        }, new Runnable() { // from class: com.android.server.display.DisplayPowerController$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DisplayPowerController.this.m3414xf8d9c315();
            }
        }, this.mContext);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$createHbmControllerLocked$3$com-android-server-display-DisplayPowerController  reason: not valid java name */
    public /* synthetic */ void m3414xf8d9c315() {
        sendUpdatePowerStateLocked();
        postBrightnessChangeRunnable();
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController != null) {
            automaticBrightnessController.update();
        }
    }

    private BrightnessThrottler createBrightnessThrottlerLocked() {
        DisplayDevice device = this.mLogicalDisplay.getPrimaryDisplayDeviceLocked();
        DisplayDeviceConfig ddConfig = device.getDisplayDeviceConfig();
        DisplayDeviceConfig.BrightnessThrottlingData data = ddConfig != null ? ddConfig.getBrightnessThrottlingData() : null;
        return new BrightnessThrottler(this.mHandler, data, new Runnable() { // from class: com.android.server.display.DisplayPowerController$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                DisplayPowerController.this.m3413xba62a01e();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$createBrightnessThrottlerLocked$4$com-android-server-display-DisplayPowerController  reason: not valid java name */
    public /* synthetic */ void m3413xba62a01e() {
        sendUpdatePowerStateLocked();
        postBrightnessChangeRunnable();
    }

    private void blockScreenOn() {
        if (this.mPendingScreenOnUnblocker == null) {
            Trace.asyncTraceBegin(131072L, SCREEN_ON_BLOCKED_TRACE_NAME, 0);
            this.mPendingScreenOnUnblocker = new ScreenOnUnblocker();
            this.mScreenOnBlockStartRealTime = SystemClock.elapsedRealtime();
            Slog.i(this.TAG, "Blocking screen on until initial contents have been drawn.");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unblockScreenOn() {
        if (this.mPendingScreenOnUnblocker != null) {
            this.mPendingScreenOnUnblocker = null;
            long delay = SystemClock.elapsedRealtime() - this.mScreenOnBlockStartRealTime;
            Slog.i(this.TAG, "Unblocked screen on after " + delay + " ms");
            Trace.asyncTraceEnd(131072L, SCREEN_ON_BLOCKED_TRACE_NAME, 0);
        }
    }

    private void blockScreenOff() {
        if (this.mPendingScreenOffUnblocker == null) {
            Trace.asyncTraceBegin(131072L, SCREEN_OFF_BLOCKED_TRACE_NAME, 0);
            this.mPendingScreenOffUnblocker = new ScreenOffUnblocker();
            this.mScreenOffBlockStartRealTime = SystemClock.elapsedRealtime();
            Slog.i(this.TAG, "Blocking screen off");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unblockScreenOff() {
        if (this.mPendingScreenOffUnblocker != null) {
            this.mPendingScreenOffUnblocker = null;
            long delay = SystemClock.elapsedRealtime() - this.mScreenOffBlockStartRealTime;
            Slog.i(this.TAG, "Unblocked screen off after " + delay + " ms");
            Trace.asyncTraceEnd(131072L, SCREEN_OFF_BLOCKED_TRACE_NAME, 0);
        }
    }

    private boolean setScreenState(int state) {
        return setScreenState(state, false);
    }

    private boolean setScreenState(int state, boolean reportOnly) {
        int i;
        boolean isOff = state == 1;
        boolean z = DEBUG_FOLD;
        if (z) {
            Slog.i(this.TAG, "setScreenState-state:" + state + "   /reportOnly:" + reportOnly + "   /mPowerState.getScreenState:" + this.mPowerState.getScreenState() + "   /mReportedScreenStateToPolicy:" + this.mReportedScreenStateToPolicy + "   /mScreenOffBecauseOfProximity:" + this.mScreenOffBecauseOfProximity + "   /mPendingScreenOffUnblocker:" + this.mPendingScreenOffUnblocker + "    /mPreResumePending:" + this.mPowerState.isPreResumePending());
        }
        if (OS_FOLDABLE_SCREEN_SUPPORT && isOff && this.mPowerState.isPreResumePending()) {
            this.mPowerState.stopPreWakeup();
        }
        boolean isTransition = this.mLogicalDisplay.getPhase() == 0;
        if (this.mPowerState.getScreenState() != state || this.mReportedScreenStateToPolicy == -1) {
            if (isOff && !this.mScreenOffBecauseOfProximity) {
                int i2 = this.mReportedScreenStateToPolicy;
                if (i2 == 2 || ((i2 == 1 && isTransition) || i2 == -1)) {
                    setReportedScreenState(3);
                    blockScreenOff();
                    if (z) {
                        Slog.d(this.TAG, "mWindowManagerPolicy.screenTurningOff()-isTransition:" + isTransition);
                    }
                    this.mWindowManagerPolicy.screenTurningOff(this.mDisplayId, this.mPendingScreenOffUnblocker, isTransition);
                    unblockScreenOff();
                } else if (this.mPendingScreenOffUnblocker != null) {
                    return false;
                }
            }
            if (!reportOnly && this.mPowerState.getScreenState() != state) {
                Trace.traceCounter(131072L, "ScreenState", state);
                SystemProperties.set("debug.tracing.screen_state", String.valueOf(state));
                this.mPowerState.setScreenState(state);
                if (ITranHBMManager.TRAN_HIGH_BRIGHTNESS_MODE_SUPPORT) {
                    ITranHBMManager.Instance().setScreenState(this.mDisplayId, state);
                }
                noteScreenState(state);
            }
        }
        if (isOff && this.mReportedScreenStateToPolicy != 0 && !this.mScreenOffBecauseOfProximity) {
            setReportedScreenState(0);
            unblockScreenOn();
            if (this.mLogicalDisplay.getPhase() != 0) {
                Slog.d(this.TAG, "mWindowManagerPolicy.screenTurnedOff()");
                this.mWindowManagerPolicy.screenTurnedOff(this.mDisplayId);
            }
        } else if (!isOff && this.mReportedScreenStateToPolicy == 3) {
            unblockScreenOff();
            if (z) {
                Slog.d(this.TAG, "mWindowManagerPolicy.screenTurnedOff()(transitional)");
            }
            this.mWindowManagerPolicy.screenTurnedOff(this.mDisplayId);
            setReportedScreenState(0);
        }
        if (!isOff && ((i = this.mReportedScreenStateToPolicy) == 0 || i == -1)) {
            setReportedScreenState(1);
            if (this.mPowerState.getColorFadeLevel() == SCREEN_ANIMATION_RATE_MINIMUM) {
                blockScreenOn();
            } else {
                unblockScreenOn();
            }
            if (z) {
                Slog.d(this.TAG, "mWindowManagerPolicy.screenTurningOn() +");
            }
            this.mWindowManagerPolicy.screenTurningOn(this.mDisplayId, this.mPendingScreenOnUnblocker, this.mPowerState.getScreenState(), this.mScreenOffBecauseofDisplayTransition);
        }
        return this.mPendingScreenOnUnblocker == null;
    }

    private void setReportedScreenState(int state) {
        Trace.traceCounter(131072L, "ReportedScreenStateToPolicy", state);
        this.mReportedScreenStateToPolicy = state;
    }

    private void loadAmbientLightSensor() {
        DisplayDeviceConfig.SensorData lightSensor = this.mDisplayDeviceConfig.getAmbientLightSensor();
        int fallbackType = this.mDisplayId == 0 ? 5 : 0;
        this.mLightSensor = SensorUtils.findSensor(this.mSensorManager, lightSensor.type, lightSensor.name, fallbackType);
    }

    private void loadProximitySensor() {
        DisplayDeviceConfig.SensorData proxSensor = this.mDisplayDeviceConfig.getProximitySensor();
        int fallbackType = this.mDisplayId == 0 ? 8 : 0;
        Sensor findSensor = SensorUtils.findSensor(this.mSensorManager, proxSensor.type, proxSensor.name, fallbackType);
        this.mProximitySensor = findSensor;
        if (findSensor != null) {
            this.mProximityThreshold = Math.min(findSensor.getMaximumRange(), (float) TYPICAL_PROXIMITY_THRESHOLD);
        }
    }

    private float clampScreenBrightnessForVr(float value) {
        return MathUtils.constrain(value, this.mScreenBrightnessForVrRangeMinimum, this.mScreenBrightnessForVrRangeMaximum);
    }

    private float clampScreenBrightness(float value) {
        if (Float.isNaN(value)) {
            value = SCREEN_ANIMATION_RATE_MINIMUM;
        }
        if (ITranDisplayPowerController.Instance().isConnectPolicyBrightness(this.mPowerRequest.policy, value)) {
            return SCREEN_ANIMATION_RATE_MINIMUM;
        }
        return MathUtils.constrain(value, this.mHbmController.getCurrentBrightnessMin(), this.mHbmController.getCurrentBrightnessMax());
    }

    private boolean isValidBrightnessValue(float brightness) {
        return brightness >= SCREEN_ANIMATION_RATE_MINIMUM && brightness <= 1.0f;
    }

    private void animateScreenBrightness(float target, float sdrTarget, float rate) {
        if (DEBUG) {
            Slog.d(this.TAG, "Animating brightness: target=" + target + ", sdrTarget=" + sdrTarget + ", rate=" + rate);
        }
        boolean systemBrightnessAnimate = false;
        float systemBrightnessDuration = SCREEN_ANIMATION_RATE_MINIMUM;
        this.mScreenBrightnessRampAnimator.setDisplayId(this.mDisplayId);
        if (this.mScreenBrightnessRampAnimator.animateTo(target, sdrTarget, rate)) {
            Trace.traceCounter(131072L, "TargetScreenBrightness", (int) target);
            SystemProperties.set("debug.tracing.screen_brightness", String.valueOf(target));
            noteScreenBrightness(target);
            systemBrightnessAnimate = true;
            float currentBrightness = this.mScreenBrightnessRampAnimator.currentBrightness();
            systemBrightnessDuration = Math.abs(currentBrightness - target) / rate;
        }
        if (ITranHBMManager.TRAN_HIGH_BRIGHTNESS_MODE_SUPPORT) {
            ITranHBMManager.Instance().onSystemBrightnessChanged(this.mDisplayId, target, systemBrightnessAnimate, systemBrightnessDuration);
        }
    }

    private void animateScreenStateChange(int target, boolean performScreenOffTransition) {
        if (DEBUG_FOLD) {
            Slog.d(this.TAG, "animateScreenStateChange: target=" + target + ", mLogicalDisplay.isEnabled()=" + this.mLogicalDisplay.isEnabled());
        }
        if (this.mColorFadeEnabled && (this.mColorFadeOnAnimator.isStarted() || this.mColorFadeOffAnimator.isStarted())) {
            if (target != 2) {
                return;
            }
            this.mPendingScreenOff = false;
        }
        if (this.mDisplayBlanksAfterDozeConfig && Display.isDozeState(this.mPowerState.getScreenState()) && !Display.isDozeState(target)) {
            this.mPowerState.prepareColorFade(this.mContext, this.mColorFadeFadesConfig ? 2 : 0);
            ObjectAnimator objectAnimator = this.mColorFadeOffAnimator;
            if (objectAnimator != null) {
                objectAnimator.end();
            }
            setScreenState(1, target != 1);
        }
        if (this.mPendingScreenOff && target != 1) {
            setScreenState(1);
            this.mPendingScreenOff = false;
            this.mPowerState.dismissColorFadeResources();
        }
        if (target == 2) {
            if (!setScreenState(2)) {
                return;
            }
            if (!USE_COLOR_FADE_ON_ANIMATION || !this.mColorFadeEnabled || !this.mPowerRequest.isBrightOrDim()) {
                this.mPowerState.setColorFadeLevel(1.0f);
                this.mPowerState.dismissColorFade();
            } else if (IDisplayManagerServiceLice.Instance().isReadyToFadeOn(this.mDisplayId)) {
                if (this.mPowerState.getColorFadeLevel() == 1.0f) {
                    this.mPowerState.dismissColorFade();
                } else if (this.mColorFadeOnAnimator.isStarted()) {
                    Slog.d(this.TAG, "FadeOnAnimator is already started");
                } else {
                    if (this.mPowerState.prepareColorFade(this.mContext, this.mColorFadeFadesConfig ? 2 : 0)) {
                        this.mColorFadeOnAnimator.start();
                    } else {
                        this.mColorFadeOnAnimator.end();
                    }
                }
            } else {
                return;
            }
        } else if (target == 5) {
            if ((!this.mScreenBrightnessRampAnimator.isAnimating() || this.mPowerState.getScreenState() != 2) && setScreenState(5)) {
                this.mPowerState.setColorFadeLevel(1.0f);
                this.mPowerState.dismissColorFade();
            } else {
                return;
            }
        } else if (target == 3) {
            if ((!this.mScreenBrightnessRampAnimator.isAnimating() || this.mPowerState.getScreenState() != 2) && setScreenState(3)) {
                this.mPowerState.setColorFadeLevel(1.0f);
                this.mPowerState.dismissColorFade();
            } else {
                return;
            }
        } else if (target == 4) {
            if (!this.mScreenBrightnessRampAnimator.isAnimating() || this.mPowerState.getScreenState() == 4) {
                if (this.mPowerState.getScreenState() != 4) {
                    if (!setScreenState(3)) {
                        return;
                    }
                    setScreenState(4);
                }
                this.mPowerState.setColorFadeLevel(1.0f);
                this.mPowerState.dismissColorFade();
            } else {
                return;
            }
        } else if (target == 6) {
            if (!this.mScreenBrightnessRampAnimator.isAnimating() || this.mPowerState.getScreenState() == 6) {
                if (this.mPowerState.getScreenState() != 6) {
                    if (!setScreenState(2)) {
                        return;
                    }
                    setScreenState(6);
                }
                this.mPowerState.setColorFadeLevel(1.0f);
                this.mPowerState.dismissColorFade();
            } else {
                return;
            }
        } else {
            this.mPendingScreenOff = true;
            if (!this.mColorFadeEnabled) {
                this.mPowerState.setColorFadeLevel(SCREEN_ANIMATION_RATE_MINIMUM);
            }
            if (this.mPowerState.getColorFadeLevel() == SCREEN_ANIMATION_RATE_MINIMUM) {
                setScreenState(1);
                this.mPendingScreenOff = false;
                this.mPowerState.dismissColorFadeResources();
            } else {
                if (performScreenOffTransition) {
                    if (this.mPowerState.prepareColorFade(this.mContext, this.mColorFadeFadesConfig ? 2 : 1) && this.mPowerState.getScreenState() != 1) {
                        this.mColorFadeOffAnimator.start();
                    }
                }
                this.mColorFadeOffAnimator.end();
            }
        }
        IDisplayManagerServiceLice.Instance().animateScreenStateChangeLeave(this.mDisplayId, target, this.mPowerState.getColorFadeLevel());
    }

    private void setProximitySensorEnabled(boolean enable) {
        if (enable) {
            if (!this.mProximitySensorEnabled) {
                this.mProximitySensorEnabled = true;
                this.mIgnoreProximityUntilChanged = false;
                this.mSensorManager.registerListener(this.mProximitySensorListener, this.mProximitySensor, 3, this.mHandler);
            }
        } else if (this.mProximitySensorEnabled) {
            this.mProximitySensorEnabled = false;
            this.mProximity = -1;
            this.mIgnoreProximityUntilChanged = false;
            this.mPendingProximity = -1;
            this.mHandler.removeMessages(2);
            this.mSensorManager.unregisterListener(this.mProximitySensorListener);
            clearPendingProximityDebounceTime();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleProximitySensorEvent(long time, boolean positive) {
        if (this.mProximitySensorEnabled) {
            int i = this.mPendingProximity;
            if (i == 0 && !positive) {
                return;
            }
            if (i == 1 && positive) {
                return;
            }
            this.mHandler.removeMessages(2);
            if (positive) {
                this.mPendingProximity = 1;
                setPendingProximityDebounceTime(0 + time);
            } else {
                this.mPendingProximity = 0;
                setPendingProximityDebounceTime(25 + time);
            }
            debounceProximitySensor();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void debounceProximitySensor() {
        if (this.mProximitySensorEnabled && this.mPendingProximity != -1 && this.mPendingProximityDebounceTime >= 0) {
            long now = SystemClock.uptimeMillis();
            if (this.mPendingProximityDebounceTime <= now) {
                if (this.mProximity != this.mPendingProximity) {
                    this.mIgnoreProximityUntilChanged = false;
                    Slog.i(this.TAG, "No longer ignoring proximity [" + this.mPendingProximity + "]");
                }
                this.mProximity = this.mPendingProximity;
                updatePowerState();
                BrightnessTracker brightnessTracker = this.mBrightnessTracker;
                if (brightnessTracker != null) {
                    boolean disableLightSensor = false;
                    if (this.mProximity == 1) {
                        disableLightSensor = true;
                    }
                    brightnessTracker.adjustSensorListenerWithProximity(disableLightSensor);
                }
                clearPendingProximityDebounceTime();
                return;
            }
            Message msg = this.mHandler.obtainMessage(2);
            this.mHandler.sendMessageAtTime(msg, this.mPendingProximityDebounceTime);
        }
    }

    private void clearPendingProximityDebounceTime() {
        if (this.mPendingProximityDebounceTime >= 0) {
            this.mPendingProximityDebounceTime = -1L;
            this.mCallbacks.releaseSuspendBlocker(this.mSuspendBlockerIdProxDebounce);
        }
    }

    private void setPendingProximityDebounceTime(long debounceTime) {
        if (this.mPendingProximityDebounceTime < 0) {
            this.mCallbacks.acquireSuspendBlocker(this.mSuspendBlockerIdProxDebounce);
        }
        this.mPendingProximityDebounceTime = debounceTime;
    }

    private void sendOnStateChangedWithWakelock() {
        if (!this.mOnStateChangedPending) {
            this.mOnStateChangedPending = true;
            this.mCallbacks.acquireSuspendBlocker(this.mSuspendBlockerIdOnStateChanged);
            this.mHandler.post(this.mOnStateChangedRunnable);
        }
    }

    private void logDisplayPolicyChanged(int newPolicy) {
        LogMaker log = new LogMaker(1696);
        log.setType(6);
        log.setSubtype(newPolicy);
        MetricsLogger.action(log);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSettingsChange(boolean userSwitch) {
        this.mIsLucidBrightnessSet = false;
        float screenBrightnessSetting = getScreenBrightnessSetting();
        this.mPendingScreenBrightnessSetting = screenBrightnessSetting;
        if (userSwitch) {
            setCurrentScreenBrightness(screenBrightnessSetting);
            AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
            if (automaticBrightnessController != null) {
                automaticBrightnessController.resetShortTermModel();
            }
        }
        this.mPendingAutoBrightnessAdjustment = getAutoBrightnessAdjustmentSetting();
        this.mScreenBrightnessForVr = getScreenBrightnessForVrSetting();
        m3417x12b1bc01();
    }

    private float getAutoBrightnessAdjustmentSetting() {
        float adj = Settings.System.getFloatForUser(this.mContext.getContentResolver(), "screen_auto_brightness_adj", SCREEN_ANIMATION_RATE_MINIMUM, -2);
        return Float.isNaN(adj) ? SCREEN_ANIMATION_RATE_MINIMUM : clampAutoBrightnessAdjustment(adj);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getScreenBrightnessSetting() {
        float brightness = this.mBrightnessSetting.getBrightness();
        if (Float.isNaN(brightness)) {
            brightness = this.mScreenBrightnessDefault;
        }
        return clampAbsoluteBrightness(brightness);
    }

    private float getScreenBrightnessForVrSetting() {
        float brightnessFloat = Settings.System.getFloatForUser(this.mContext.getContentResolver(), "screen_brightness_for_vr_float", this.mScreenBrightnessForVrDefault, -2);
        return clampScreenBrightnessForVr(brightnessFloat);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setBrightness(float brightnessValue) {
        this.mBrightnessSetting.setBrightness(brightnessValue);
    }

    private void updateScreenBrightnessSetting(float brightnessValue) {
        if (!isValidBrightnessValue(brightnessValue) || brightnessValue == this.mCurrentScreenBrightnessSetting) {
            return;
        }
        setCurrentScreenBrightness(brightnessValue);
        this.mBrightnessSetting.setBrightness(brightnessValue);
    }

    private void setCurrentScreenBrightness(float brightnessValue) {
        if (brightnessValue != this.mCurrentScreenBrightnessSetting) {
            this.mCurrentScreenBrightnessSetting = brightnessValue;
            postBrightnessChangeRunnable();
        }
    }

    private void putAutoBrightnessAdjustmentSetting(float adjustment) {
        if (this.mDisplayId == 0) {
            this.mAutoBrightnessAdjustment = adjustment;
            Settings.System.putFloatForUser(this.mContext.getContentResolver(), "screen_auto_brightness_adj", adjustment, -2);
        }
    }

    private void putScreenBrightnessAnimationSetting(float target, float duration) {
        Settings.System.putFloatForUser(this.mContext.getContentResolver(), "tran_animate_brightness_time", duration, -2);
        Settings.System.putIntForUser(this.mContext.getContentResolver(), "tran_animate_brightness_update", this.mBrighnessUpdateReversal, -2);
        int i = this.mBrighnessUpdateReversal + 1;
        this.mBrighnessUpdateReversal = i;
        this.mBrighnessUpdateReversal = i % 3;
    }

    private void putScreenBrightnessAnimationSetting(float target) {
        Settings.System.putIntForUser(this.mContext.getContentResolver(), "tran_animate_brightness_update", this.mBrighnessUpdateReversal, -2);
        int i = this.mBrighnessUpdateReversal + 1;
        this.mBrighnessUpdateReversal = i;
        this.mBrighnessUpdateReversal = i % 3;
    }

    private float computeScreenBrightnessRate(float duration) {
        return (Float.isNaN(duration) || duration == SCREEN_ANIMATION_RATE_MINIMUM) ? SCREEN_ANIMATION_RATE_MINIMUM : (4095.0f / duration) / 4095.0f;
    }

    private void animateScreenBrightness(float target, float rate, boolean updateImmediately) {
        float duration;
        if (DEBUG) {
            Slog.d(this.TAG, "Animating brightness: target=" + target + ", rate=" + rate);
        }
        float rateHigh = SCREEN_ANIMATION_RATE_MINIMUM;
        float rateLow = SCREEN_ANIMATION_RATE_MINIMUM;
        if (rate > SCREEN_ANIMATION_RATE_MINIMUM) {
            rateLow = rate / 3.0f;
            rateHigh = rate;
        }
        ITranDisplayPowerController.Instance().animateFpDimLayerBrightness(target, this.mDisplayId);
        boolean systemBrightnessAnimate = false;
        float systemBrightnessDuration = SCREEN_ANIMATION_RATE_MINIMUM;
        if (this.mScreenBrightnessRampAnimator.animateTo(target, rateHigh, rateLow, updateImmediately)) {
            Slog.d(this.TAG, "animateScreenBrightness: target=" + target + ", rate=" + rate + "    mShouldUpdateBrightnessSettingLater=" + this.mShouldUpdateBrightnessSettingLater);
            if (this.mShouldUpdateBrightnessSettingLater) {
                float currentBrightness = this.mScreenBrightnessRampAnimator.currentBrightness();
                boolean adapterRate = !TRAN_BACKLIGHT_4095_LEVEL && currentBrightness > target && currentBrightness > 0.3f;
                if ((this.mScreenBrightnessRampAnimator.isRateUpdate() || adapterRate) && rate > SCREEN_ANIMATION_RATE_MINIMUM) {
                    if (currentBrightness > 0.04884005f && target < 0.04884005f) {
                        duration = (Math.abs(currentBrightness - 0.04884005f) / rateHigh) + (Math.abs(0.04884005f - target) / rateLow);
                    } else {
                        duration = Math.abs(currentBrightness - target) / rate;
                    }
                    if (duration < 0.5f) {
                        duration = 0.5f;
                    }
                    systemBrightnessAnimate = true;
                    systemBrightnessDuration = duration;
                    Slog.d(this.TAG, "animateScreenBrightness: putScreenBrightnessSetting currentBrightness=" + currentBrightness + ", duration=" + duration);
                    putScreenBrightnessAnimationSetting(target, duration);
                } else {
                    Slog.d(this.TAG, "animateScreenBrightness: putScreenBrightnessSetting without duration update.");
                    putScreenBrightnessAnimationSetting(target);
                }
            }
            if (this.mBrightnessReasonTemp.reason == 1 && this.mBrightnessReasonTemp.equals(this.mBrightnessReason)) {
                putScreenBrightnessAnimationSetting(target, SCREEN_ANIMATION_RATE_MINIMUM);
                systemBrightnessDuration = 0.0f;
                systemBrightnessAnimate = false;
            }
            Trace.traceCounter(131072L, "TargetScreenBrightness", (int) target);
            SystemProperties.set("debug.tracing.screen_brightness", String.valueOf(target));
            IBatteryStats iBatteryStats = this.mBatteryStats;
            if (iBatteryStats != null) {
                try {
                    iBatteryStats.noteScreenBrightness(BrightnessSynchronizer.brightnessFloatToInt(target));
                } catch (RemoteException e) {
                }
            }
            if (ITranHBMManager.TRAN_HIGH_BRIGHTNESS_MODE_SUPPORT) {
                ITranHBMManager.Instance().onSystemBrightnessChanged(this.mDisplayId, target, systemBrightnessAnimate, systemBrightnessDuration);
            }
        }
    }

    private boolean updateAutoBrightnessAdjustment() {
        if (Float.isNaN(this.mPendingAutoBrightnessAdjustment)) {
            return false;
        }
        float f = this.mAutoBrightnessAdjustment;
        float f2 = this.mPendingAutoBrightnessAdjustment;
        if (f == f2) {
            this.mPendingAutoBrightnessAdjustment = Float.NaN;
            return false;
        }
        this.mAutoBrightnessAdjustment = f2;
        this.mPendingAutoBrightnessAdjustment = Float.NaN;
        return true;
    }

    private boolean updateUserSetScreenBrightness() {
        if (!Float.isNaN(this.mPendingScreenBrightnessSetting)) {
            float f = this.mPendingScreenBrightnessSetting;
            if (f >= SCREEN_ANIMATION_RATE_MINIMUM) {
                if (this.mCurrentScreenBrightnessSetting == f) {
                    this.mPendingScreenBrightnessSetting = Float.NaN;
                    this.mTemporaryScreenBrightness = Float.NaN;
                    return false;
                }
                setCurrentScreenBrightness(f);
                this.mLastUserSetScreenBrightness = this.mPendingScreenBrightnessSetting;
                this.mPendingScreenBrightnessSetting = Float.NaN;
                this.mTemporaryScreenBrightness = Float.NaN;
                return true;
            }
        }
        return false;
    }

    private void notifyBrightnessTrackerChanged(float brightness, boolean userInitiated, boolean hadUserDataPoint) {
        float powerFactor;
        float brightnessInNits = convertToNits(brightness);
        if (this.mPowerRequest.useAutoBrightness && brightnessInNits >= SCREEN_ANIMATION_RATE_MINIMUM && this.mAutomaticBrightnessController != null) {
            if (this.mPowerRequest.lowPowerMode) {
                powerFactor = this.mPowerRequest.screenLowPowerBrightnessFactor;
            } else {
                powerFactor = 1.0f;
            }
            this.mBrightnessTracker.notifyBrightnessChanged(brightnessInNits, userInitiated, powerFactor, hadUserDataPoint, this.mAutomaticBrightnessController.isDefaultConfig(), this.mUniqueDisplayId);
        }
    }

    private float convertToNits(float brightness) {
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController == null) {
            return -1.0f;
        }
        return automaticBrightnessController.convertToNits(brightness);
    }

    private void updatePendingProximityRequestsLocked() {
        this.mWaitingForNegativeProximity |= this.mPendingWaitForNegativeProximityLocked;
        this.mPendingWaitForNegativeProximityLocked = false;
        if (this.mIgnoreProximityUntilChanged) {
            this.mWaitingForNegativeProximity = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void ignoreProximitySensorUntilChangedInternal() {
        if (!this.mIgnoreProximityUntilChanged && this.mProximity == 1) {
            this.mIgnoreProximityUntilChanged = true;
            Slog.i(this.TAG, "Ignoring proximity");
            updatePowerState();
        }
    }

    private void sendOnProximityPositiveWithWakelock() {
        this.mCallbacks.acquireSuspendBlocker(this.mSuspendBlockerIdProxPositive);
        this.mHandler.post(this.mOnProximityPositiveRunnable);
        this.mOnProximityPositiveMessages++;
    }

    private void sendOnProximityNegativeWithWakelock() {
        this.mOnProximityNegativeMessages++;
        this.mCallbacks.acquireSuspendBlocker(this.mSuspendBlockerIdProxNegative);
        this.mHandler.post(this.mOnProximityNegativeRunnable);
    }

    public void dump(final PrintWriter pw) {
        synchronized (this.mLock) {
            pw.println();
            pw.println("Display Power Controller:");
            pw.println("  mDisplayId=" + this.mDisplayId);
            pw.println("  mLightSensor=" + this.mLightSensor);
            pw.println();
            pw.println("Display Power Controller Locked State:");
            pw.println("  mDisplayReadyLocked=" + this.mDisplayReadyLocked);
            pw.println("  mPendingRequestLocked=" + this.mPendingRequestLocked);
            pw.println("  mPendingRequestChangedLocked=" + this.mPendingRequestChangedLocked);
            pw.println("  mPendingWaitForNegativeProximityLocked=" + this.mPendingWaitForNegativeProximityLocked);
            pw.println("  mPendingUpdatePowerStateLocked=" + this.mPendingUpdatePowerStateLocked);
        }
        pw.println();
        pw.println("Display Power Controller Configuration:");
        pw.println("  mScreenBrightnessRangeDefault=" + this.mScreenBrightnessDefault);
        pw.println("  mScreenBrightnessDozeConfig=" + this.mScreenBrightnessDozeConfig);
        pw.println("  mScreenBrightnessDimConfig=" + this.mScreenBrightnessDimConfig);
        pw.println("  mScreenBrightnessForVrRangeMinimum=" + this.mScreenBrightnessForVrRangeMinimum);
        pw.println("  mScreenBrightnessForVrRangeMaximum=" + this.mScreenBrightnessForVrRangeMaximum);
        pw.println("  mScreenBrightnessForVrDefault=" + this.mScreenBrightnessForVrDefault);
        pw.println("  mUseSoftwareAutoBrightnessConfig=" + this.mUseSoftwareAutoBrightnessConfig);
        pw.println("  mAllowAutoBrightnessWhileDozingConfig=" + this.mAllowAutoBrightnessWhileDozingConfig);
        pw.println("  mSkipScreenOnBrightnessRamp=" + this.mSkipScreenOnBrightnessRamp);
        pw.println("  mColorFadeFadesConfig=" + this.mColorFadeFadesConfig);
        pw.println("  mColorFadeEnabled=" + this.mColorFadeEnabled);
        synchronized (this.mCachedBrightnessInfo) {
            pw.println("  mCachedBrightnessInfo.brightness=" + this.mCachedBrightnessInfo.brightness.value);
            pw.println("  mCachedBrightnessInfo.adjustedBrightness=" + this.mCachedBrightnessInfo.adjustedBrightness.value);
            pw.println("  mCachedBrightnessInfo.brightnessMin=" + this.mCachedBrightnessInfo.brightnessMin.value);
            pw.println("  mCachedBrightnessInfo.brightnessMax=" + this.mCachedBrightnessInfo.brightnessMax.value);
            pw.println("  mCachedBrightnessInfo.hbmMode=" + this.mCachedBrightnessInfo.hbmMode.value);
            pw.println("  mCachedBrightnessInfo.hbmTransitionPoint=" + this.mCachedBrightnessInfo.hbmTransitionPoint.value);
            pw.println("  mCachedBrightnessInfo.brightnessMaxReason =" + this.mCachedBrightnessInfo.brightnessMaxReason.value);
        }
        pw.println("  mDisplayBlanksAfterDozeConfig=" + this.mDisplayBlanksAfterDozeConfig);
        pw.println("  mBrightnessBucketsInDozeConfig=" + this.mBrightnessBucketsInDozeConfig);
        this.mHandler.runWithScissors(new Runnable() { // from class: com.android.server.display.DisplayPowerController$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                DisplayPowerController.this.m3415lambda$dump$5$comandroidserverdisplayDisplayPowerController(pw);
            }
        }, 1000L);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: dumpLocal */
    public void m3415lambda$dump$5$comandroidserverdisplayDisplayPowerController(PrintWriter pw) {
        pw.println();
        pw.println("Display Power Controller Thread State:");
        pw.println("  mPowerRequest=" + this.mPowerRequest);
        pw.println("  mUnfinishedBusiness=" + this.mUnfinishedBusiness);
        pw.println("  mWaitingForNegativeProximity=" + this.mWaitingForNegativeProximity);
        pw.println("  mProximitySensor=" + this.mProximitySensor);
        pw.println("  mProximitySensorEnabled=" + this.mProximitySensorEnabled);
        pw.println("  mProximityThreshold=" + this.mProximityThreshold);
        pw.println("  mProximity=" + proximityToString(this.mProximity));
        pw.println("  mPendingProximity=" + proximityToString(this.mPendingProximity));
        pw.println("  mPendingProximityDebounceTime=" + TimeUtils.formatUptime(this.mPendingProximityDebounceTime));
        pw.println("  mScreenOffBecauseOfProximity=" + this.mScreenOffBecauseOfProximity);
        pw.println("  mLastUserSetScreenBrightness=" + this.mLastUserSetScreenBrightness);
        pw.println("  mPendingScreenBrightnessSetting=" + this.mPendingScreenBrightnessSetting);
        pw.println("  mTemporaryScreenBrightness=" + this.mTemporaryScreenBrightness);
        pw.println("  mAutoBrightnessAdjustment=" + this.mAutoBrightnessAdjustment);
        pw.println("  mBrightnessReason=" + this.mBrightnessReason);
        pw.println("  mTemporaryAutoBrightnessAdjustment=" + this.mTemporaryAutoBrightnessAdjustment);
        pw.println("  mPendingAutoBrightnessAdjustment=" + this.mPendingAutoBrightnessAdjustment);
        pw.println("  mScreenBrightnessForVrFloat=" + this.mScreenBrightnessForVr);
        pw.println("  mAppliedAutoBrightness=" + this.mAppliedAutoBrightness);
        pw.println("  mAppliedDimming=" + this.mAppliedDimming);
        pw.println("  mAppliedLowPower=" + this.mAppliedLowPower);
        pw.println("  mAppliedThrottling=" + this.mAppliedThrottling);
        pw.println("  mAppliedScreenBrightnessOverride=" + this.mAppliedScreenBrightnessOverride);
        pw.println("  mAppliedTemporaryBrightness=" + this.mAppliedTemporaryBrightness);
        pw.println("  mAppliedTemporaryAutoBrightnessAdjustment=" + this.mAppliedTemporaryAutoBrightnessAdjustment);
        pw.println("  mAppliedBrightnessBoost=" + this.mAppliedBrightnessBoost);
        pw.println("  mDozing=" + this.mDozing);
        pw.println("  mSkipRampState=" + skipRampStateToString(this.mSkipRampState));
        pw.println("  mScreenOnBlockStartRealTime=" + this.mScreenOnBlockStartRealTime);
        pw.println("  mScreenOffBlockStartRealTime=" + this.mScreenOffBlockStartRealTime);
        pw.println("  mPendingScreenOnUnblocker=" + this.mPendingScreenOnUnblocker);
        pw.println("  mPendingScreenOffUnblocker=" + this.mPendingScreenOffUnblocker);
        pw.println("  mPendingScreenOff=" + this.mPendingScreenOff);
        pw.println("  mReportedToPolicy=" + reportedToPolicyToString(this.mReportedScreenStateToPolicy));
        pw.println("  mIsRbcActive=" + this.mIsRbcActive);
        pw.println("  mOnStateChangePending=" + this.mOnStateChangedPending);
        pw.println("  mOnProximityPositiveMessages=" + this.mOnProximityPositiveMessages);
        pw.println("  mOnProximityNegativeMessages=" + this.mOnProximityNegativeMessages);
        if (this.mScreenBrightnessRampAnimator != null) {
            pw.println("  mScreenBrightnessRampAnimator.isAnimating()=" + this.mScreenBrightnessRampAnimator.isAnimating());
        }
        if (this.mColorFadeOnAnimator != null) {
            pw.println("  mColorFadeOnAnimator.isStarted()=" + this.mColorFadeOnAnimator.isStarted());
        }
        if (this.mColorFadeOffAnimator != null) {
            pw.println("  mColorFadeOffAnimator.isStarted()=" + this.mColorFadeOffAnimator.isStarted());
        }
        DisplayPowerState displayPowerState = this.mPowerState;
        if (displayPowerState != null) {
            displayPowerState.dump(pw);
        }
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController != null) {
            automaticBrightnessController.dump(pw);
            dumpBrightnessEvents(pw);
        }
        HighBrightnessModeController highBrightnessModeController = this.mHbmController;
        if (highBrightnessModeController != null) {
            highBrightnessModeController.dump(pw);
        }
        BrightnessThrottler brightnessThrottler = this.mBrightnessThrottler;
        if (brightnessThrottler != null) {
            brightnessThrottler.dump(pw);
        }
        pw.println();
        DisplayWhiteBalanceController displayWhiteBalanceController = this.mDisplayWhiteBalanceController;
        if (displayWhiteBalanceController != null) {
            displayWhiteBalanceController.dump(pw);
            this.mDisplayWhiteBalanceSettings.dump(pw);
        }
    }

    private static String proximityToString(int state) {
        switch (state) {
            case -1:
                return "Unknown";
            case 0:
                return "Negative";
            case 1:
                return "Positive";
            default:
                return Integer.toString(state);
        }
    }

    private static String reportedToPolicyToString(int state) {
        switch (state) {
            case 0:
                return "REPORTED_TO_POLICY_SCREEN_OFF";
            case 1:
                return "REPORTED_TO_POLICY_SCREEN_TURNING_ON";
            case 2:
                return "REPORTED_TO_POLICY_SCREEN_ON";
            default:
                return Integer.toString(state);
        }
    }

    private static String skipRampStateToString(int state) {
        switch (state) {
            case 0:
                return "RAMP_STATE_SKIP_NONE";
            case 1:
                return "RAMP_STATE_SKIP_INITIAL";
            case 2:
                return "RAMP_STATE_SKIP_AUTOBRIGHT";
            default:
                return Integer.toString(state);
        }
    }

    private void dumpBrightnessEvents(PrintWriter pw) {
        int size = this.mBrightnessEventRingBuffer.size();
        if (size < 1) {
            pw.println("No Automatic Brightness Adjustments");
            return;
        }
        pw.println("Automatic Brightness Adjustments Last " + size + " Events: ");
        BrightnessEvent[] eventArray = (BrightnessEvent[]) this.mBrightnessEventRingBuffer.toArray();
        for (int i = 0; i < this.mBrightnessEventRingBuffer.size(); i++) {
            pw.println("  " + eventArray[i].toString());
        }
    }

    private static float clampAbsoluteBrightness(float value) {
        return MathUtils.constrain(value, (float) SCREEN_ANIMATION_RATE_MINIMUM, 1.0f);
    }

    private static float clampAutoBrightnessAdjustment(float value) {
        return MathUtils.constrain(value, -1.0f, 1.0f);
    }

    private void noteScreenState(int screenState) {
        IBatteryStats iBatteryStats = this.mBatteryStats;
        if (iBatteryStats != null) {
            try {
                iBatteryStats.noteScreenState(screenState);
            } catch (RemoteException e) {
            }
        }
    }

    private void noteScreenBrightness(float brightness) {
        IBatteryStats iBatteryStats = this.mBatteryStats;
        if (iBatteryStats != null) {
            try {
                iBatteryStats.noteScreenBrightness(BrightnessSynchronizer.brightnessFloatToInt(brightness));
            } catch (RemoteException e) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportStats(float brightness) {
        if (this.mLastStatsBrightness == brightness) {
            return;
        }
        synchronized (this.mCachedBrightnessInfo) {
            if (this.mCachedBrightnessInfo.hbmTransitionPoint == null) {
                return;
            }
            float hbmTransitionPoint = this.mCachedBrightnessInfo.hbmTransitionPoint.value;
            boolean aboveTransition = brightness > hbmTransitionPoint;
            boolean oldAboveTransition = this.mLastStatsBrightness > hbmTransitionPoint;
            if (aboveTransition || oldAboveTransition) {
                this.mLastStatsBrightness = brightness;
                this.mHandler.removeMessages(13);
                if (aboveTransition != oldAboveTransition) {
                    logHbmBrightnessStats(brightness, this.mDisplayStatsId);
                    return;
                }
                Message msg = this.mHandler.obtainMessage();
                msg.what = 13;
                msg.arg1 = Float.floatToIntBits(brightness);
                msg.arg2 = this.mDisplayStatsId;
                this.mHandler.sendMessageDelayed(msg, 500L);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logHbmBrightnessStats(float brightness, int displayStatsId) {
        synchronized (this.mHandler) {
            FrameworkStatsLog.write((int) FrameworkStatsLog.DISPLAY_HBM_BRIGHTNESS_CHANGED, displayStatsId, brightness);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class BrightnessEvent {
        static final int FLAG_DOZE_SCALE = 3;
        static final int FLAG_INVALID_LUX = 2;
        static final int FLAG_RBC = 1;
        static final int FLAG_USER_SET = 4;
        public int adjustmentFlags;
        public float brightness;
        public int displayId;
        public int flags;
        public float hbmMax;
        public int hbmMode;
        public float lux;
        public float preThresholdBrightness;
        public float preThresholdLux;
        public final BrightnessReason reason;
        public float recommendedBrightness;
        public float thermalMax;
        public long time;

        BrightnessEvent(BrightnessEvent that) {
            this.reason = new BrightnessReason();
            copyFrom(that);
        }

        BrightnessEvent(int displayId) {
            this.reason = new BrightnessReason();
            this.displayId = displayId;
            reset();
        }

        void copyFrom(BrightnessEvent that) {
            this.displayId = that.displayId;
            this.time = that.time;
            this.lux = that.lux;
            this.preThresholdLux = that.preThresholdLux;
            this.brightness = that.brightness;
            this.recommendedBrightness = that.recommendedBrightness;
            this.preThresholdBrightness = that.preThresholdBrightness;
            this.hbmMax = that.hbmMax;
            this.thermalMax = that.thermalMax;
            this.flags = that.flags;
            this.hbmMode = that.hbmMode;
            this.reason.set(that.reason);
            this.adjustmentFlags = that.adjustmentFlags;
        }

        void reset() {
            this.time = SystemClock.uptimeMillis();
            this.brightness = Float.NaN;
            this.recommendedBrightness = Float.NaN;
            this.lux = DisplayPowerController.SCREEN_ANIMATION_RATE_MINIMUM;
            this.preThresholdLux = DisplayPowerController.SCREEN_ANIMATION_RATE_MINIMUM;
            this.preThresholdBrightness = Float.NaN;
            this.hbmMax = 1.0f;
            this.thermalMax = 1.0f;
            this.flags = 0;
            this.hbmMode = 0;
            this.reason.set(null);
            this.adjustmentFlags = 0;
        }

        boolean equalsMainData(BrightnessEvent that) {
            return this.displayId == that.displayId && Float.floatToRawIntBits(this.brightness) == Float.floatToRawIntBits(that.brightness) && Float.floatToRawIntBits(this.recommendedBrightness) == Float.floatToRawIntBits(that.recommendedBrightness) && Float.floatToRawIntBits(this.preThresholdBrightness) == Float.floatToRawIntBits(that.preThresholdBrightness) && Float.floatToRawIntBits(this.lux) == Float.floatToRawIntBits(that.lux) && Float.floatToRawIntBits(this.preThresholdLux) == Float.floatToRawIntBits(that.preThresholdLux) && Float.floatToRawIntBits(this.hbmMax) == Float.floatToRawIntBits(that.hbmMax) && this.hbmMode == that.hbmMode && Float.floatToRawIntBits(this.thermalMax) == Float.floatToRawIntBits(that.thermalMax) && this.flags == that.flags && this.adjustmentFlags == that.adjustmentFlags && this.reason.equals(that.reason);
        }

        public String toString(boolean includeTime) {
            return (includeTime ? TimeUtils.formatForLogging(this.time) + " - " : "") + "BrightnessEvent: disp=" + this.displayId + ", brt=" + this.brightness + ((this.flags & 4) != 0 ? "(user_set)" : "") + ", rcmdBrt=" + this.recommendedBrightness + ", preBrt=" + this.preThresholdBrightness + ", lux=" + this.lux + ", preLux=" + this.preThresholdLux + ", hbmMax=" + this.hbmMax + ", hbmMode=" + BrightnessInfo.hbmToString(this.hbmMode) + ", thrmMax=" + this.thermalMax + ", flags=" + flagsToString() + ", reason=" + this.reason.toString(this.adjustmentFlags);
        }

        public String toString() {
            return toString(true);
        }

        private String flagsToString() {
            return ((this.flags & 4) != 0 ? "user_set " : "") + ((this.flags & 1) != 0 ? "rbc " : "") + ((this.flags & 2) != 0 ? "invalid_lux " : "") + ((this.flags & 3) != 0 ? "doze_scale " : "");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DisplayControllerHandler extends Handler {
        public DisplayControllerHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    DisplayPowerController.this.updatePowerState();
                    return;
                case 2:
                    DisplayPowerController.this.debounceProximitySensor();
                    return;
                case 3:
                    if (DisplayPowerController.this.mPendingScreenOnUnblocker == msg.obj) {
                        DisplayPowerController.this.unblockScreenOn();
                        DisplayPowerController.this.updatePowerState();
                        return;
                    }
                    return;
                case 4:
                    if (DisplayPowerController.this.mPendingScreenOffUnblocker == msg.obj) {
                        DisplayPowerController.this.unblockScreenOff();
                        DisplayPowerController.this.updatePowerState();
                        return;
                    }
                    return;
                case 5:
                    DisplayPowerController.this.mBrightnessConfiguration = (BrightnessConfiguration) msg.obj;
                    DisplayPowerController.this.updatePowerState();
                    return;
                case 6:
                    DisplayPowerController.this.mTemporaryScreenBrightness = Float.intBitsToFloat(msg.arg1);
                    DisplayPowerController.this.updatePowerState();
                    return;
                case 7:
                    DisplayPowerController.this.mTemporaryAutoBrightnessAdjustment = Float.intBitsToFloat(msg.arg1);
                    DisplayPowerController.this.updatePowerState();
                    return;
                case 8:
                    DisplayPowerController.this.ignoreProximitySensorUntilChangedInternal();
                    return;
                case 9:
                    DisplayPowerController.this.cleanupHandlerThreadAfterStop();
                    return;
                case 10:
                    if (DisplayPowerController.this.mStopped) {
                        return;
                    }
                    DisplayPowerController.this.handleSettingsChange(false);
                    return;
                case 11:
                    DisplayPowerController.this.handleRbcChanged();
                    return;
                case 12:
                    if (DisplayPowerController.this.mPowerState != null) {
                        float brightness = DisplayPowerController.this.mPowerState.getScreenBrightness();
                        DisplayPowerController.this.reportStats(brightness);
                        return;
                    }
                    return;
                case 13:
                    DisplayPowerController.this.logHbmBrightnessStats(Float.intBitsToFloat(msg.arg1), msg.arg2);
                    return;
                case 14:
                case 15:
                case 16:
                case 17:
                default:
                    return;
                case 18:
                    DisplayPowerController.this.mWindowManagerPolicy.setProximityPowerKeyDown(false);
                    DisplayPowerController.this.mHandleByMessage = true;
                    DisplayPowerController.this.updatePowerState();
                    DisplayPowerController.this.mHandleByMessage = false;
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            DisplayPowerController.this.handleSettingsChange(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ScreenOnUnblocker implements WindowManagerPolicy.ScreenOnListener {
        private ScreenOnUnblocker() {
        }

        @Override // com.android.server.policy.WindowManagerPolicy.ScreenOnListener
        public void onScreenOn() {
            Message msg = DisplayPowerController.this.mHandler.obtainMessage(3, this);
            DisplayPowerController.this.mHandler.sendMessage(msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ScreenOffUnblocker implements WindowManagerPolicy.ScreenOffListener {
        private ScreenOffUnblocker() {
        }

        @Override // com.android.server.policy.WindowManagerPolicy.ScreenOffListener
        public void onScreenOff() {
            Message msg = DisplayPowerController.this.mHandler.obtainMessage(4, this);
            DisplayPowerController.this.mHandler.sendMessage(msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAutoBrightnessLoggingEnabled(boolean enabled) {
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController != null) {
            automaticBrightnessController.setLoggingEnabled(enabled);
        }
    }

    @Override // com.android.server.display.whitebalance.DisplayWhiteBalanceController.Callbacks
    public void updateWhiteBalance() {
        m3417x12b1bc01();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDisplayWhiteBalanceLoggingEnabled(boolean enabled) {
        DisplayWhiteBalanceController displayWhiteBalanceController = this.mDisplayWhiteBalanceController;
        if (displayWhiteBalanceController != null) {
            displayWhiteBalanceController.setLoggingEnabled(enabled);
            this.mDisplayWhiteBalanceSettings.setLoggingEnabled(enabled);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAmbientColorTemperatureOverride(float cct) {
        DisplayWhiteBalanceController displayWhiteBalanceController = this.mDisplayWhiteBalanceController;
        if (displayWhiteBalanceController != null) {
            displayWhiteBalanceController.setAmbientColorTemperatureOverride(cct);
            m3417x12b1bc01();
        }
    }

    public void eventTrackLcm(float brightnessState) {
        int beforeBrightnessInt;
        int beforeBrightnessInt2;
        if (!this.mEventTrackFlag || !TranTrancareManager.isEnabled((long) LCM_TRACK_TID)) {
            return;
        }
        this.mEventTrackFlag = false;
        Bundle bundle = new Bundle();
        int brightnessMode = this.mBrightnessReason.reason == 1 ? 0 : 1;
        float beforeBrightnessFloat = this.mLastTrackBrightnessFloat;
        this.mLastTrackBrightnessFloat = brightnessState;
        float beforeLuxFloat = SCREEN_ANIMATION_RATE_MINIMUM;
        if (TRAN_BACKLIGHT_4095_LEVEL) {
            beforeBrightnessInt = BrightnessSynchronizer.brightnessFloatTo4095Int(this.mContext, beforeBrightnessFloat);
            beforeBrightnessInt2 = BrightnessSynchronizer.brightnessFloatTo4095Int(this.mContext, brightnessState);
            bundle.putInt("brightness_value_mode", 1);
        } else {
            int beforeBrightnessInt3 = BrightnessSynchronizer.brightnessFloatToInt(beforeBrightnessFloat);
            int afterBrightnessInt = BrightnessSynchronizer.brightnessFloatToInt(brightnessState);
            bundle.putInt("brightness_value_mode", 0);
            beforeBrightnessInt = beforeBrightnessInt3;
            beforeBrightnessInt2 = afterBrightnessInt;
        }
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController != null) {
            beforeLuxFloat = this.mLastAmbientLuxFloat;
            this.mLastAmbientLuxFloat = automaticBrightnessController.getTrackAmbientLux();
        }
        ComponentName topActivityComponent = null;
        try {
            topActivityComponent = ActivityTaskManager.getService().getTopActivityComponent();
        } catch (RemoteException e) {
        }
        bundle.putString("before_light_sensor_value", Float.toString(beforeLuxFloat));
        bundle.putString("before_brightness_value", Integer.toString(beforeBrightnessInt));
        bundle.putString("before_brightness_scale_value", Float.toString(beforeBrightnessFloat));
        bundle.putString("after_light_sensor_value", Float.toString(this.mLastAmbientLuxFloat));
        bundle.putString("after_brightness_value", Integer.toString(beforeBrightnessInt2));
        bundle.putString("after_brightness_scale_value", Float.toString(this.mLastTrackBrightnessFloat));
        if (topActivityComponent != null) {
            bundle.putString("package_name", topActivityComponent.getPackageName());
            bundle.putString("activity_name", topActivityComponent.getShortClassName());
        } else {
            bundle.putString("package_name", "");
            bundle.putString("activity_name", "");
        }
        bundle.putLong("adjust_time", System.currentTimeMillis());
        bundle.putLong("adjust_real_time", SystemClock.elapsedRealtime());
        bundle.putInt("screen_brightness_mode", brightnessMode);
        TranTrancareManager.serverLog(Long.valueOf((long) LCM_TRACK_TID), "lcm_brightness", 1, bundle);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class BrightnessReason {
        static final int ADJUSTMENT_AUTO = 2;
        static final int ADJUSTMENT_AUTO_TEMP = 1;
        static final int MODIFIER_DIMMED = 1;
        static final int MODIFIER_HDR = 4;
        static final int MODIFIER_LOW_POWER = 2;
        static final int MODIFIER_MASK = 15;
        static final int MODIFIER_THROTTLED = 8;
        static final int REASON_AUTOMATIC = 4;
        static final int REASON_BOOST = 9;
        static final int REASON_DOZE = 2;
        static final int REASON_DOZE_DEFAULT = 3;
        static final int REASON_MANUAL = 1;
        static final int REASON_MAX = 9;
        static final int REASON_OVERRIDE = 7;
        static final int REASON_SCREEN_OFF = 5;
        static final int REASON_TEMPORARY = 8;
        static final int REASON_UNKNOWN = 0;
        static final int REASON_VR = 6;
        public int modifier;
        public int reason;

        private BrightnessReason() {
        }

        public void set(BrightnessReason other) {
            setReason(other == null ? 0 : other.reason);
            setModifier(other != null ? other.modifier : 0);
        }

        public void setReason(int reason) {
            if (reason < 0 || reason > 9) {
                Slog.w(DisplayPowerController.this.TAG, "brightness reason out of bounds: " + reason);
            } else {
                this.reason = reason;
            }
        }

        public void setModifier(int modifier) {
            if ((modifier & (-16)) != 0) {
                Slog.w(DisplayPowerController.this.TAG, "brightness modifier out of bounds: 0x" + Integer.toHexString(modifier));
            } else {
                this.modifier = modifier;
            }
        }

        public void addModifier(int modifier) {
            setModifier(this.modifier | modifier);
        }

        public boolean equals(Object obj) {
            if (obj instanceof BrightnessReason) {
                BrightnessReason other = (BrightnessReason) obj;
                return other.reason == this.reason && other.modifier == this.modifier;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(Integer.valueOf(this.reason), Integer.valueOf(this.modifier));
        }

        public String toString() {
            return toString(0);
        }

        public String toString(int adjustments) {
            StringBuilder sb = new StringBuilder();
            sb.append(reasonToString(this.reason));
            sb.append(" [");
            if ((adjustments & 1) != 0) {
                sb.append(" temp_adj");
            }
            if ((adjustments & 2) != 0) {
                sb.append(" auto_adj");
            }
            if ((this.modifier & 2) != 0) {
                sb.append(" low_pwr");
            }
            if ((this.modifier & 1) != 0) {
                sb.append(" dim");
            }
            if ((this.modifier & 4) != 0) {
                sb.append(" hdr");
            }
            if ((this.modifier & 8) != 0) {
                sb.append(" throttled");
            }
            int strlen = sb.length();
            if (sb.charAt(strlen - 1) == '[') {
                sb.setLength(strlen - 2);
            } else {
                sb.append(" ]");
            }
            return sb.toString();
        }

        private String reasonToString(int reason) {
            switch (reason) {
                case 1:
                    return "manual";
                case 2:
                    return "doze";
                case 3:
                    return "doze_default";
                case 4:
                    return "automatic";
                case 5:
                    return "screen_off";
                case 6:
                    return "vr";
                case 7:
                    return "override";
                case 8:
                    return "temporary";
                case 9:
                    return "boost";
                default:
                    return Integer.toString(reason);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class CachedBrightnessInfo {
        public MutableFloat brightness = new MutableFloat(Float.NaN);
        public MutableFloat adjustedBrightness = new MutableFloat(Float.NaN);
        public MutableFloat brightnessMin = new MutableFloat(Float.NaN);
        public MutableFloat brightnessMax = new MutableFloat(Float.NaN);
        public MutableInt hbmMode = new MutableInt(0);
        public MutableFloat hbmTransitionPoint = new MutableFloat(Float.POSITIVE_INFINITY);
        public MutableInt brightnessMaxReason = new MutableInt(0);

        CachedBrightnessInfo() {
        }

        public boolean checkAndSetFloat(MutableFloat mf, float f) {
            if (mf.value != f) {
                mf.value = f;
                return true;
            }
            return false;
        }

        public boolean checkAndSetInt(MutableInt mi, int i) {
            if (mi.value != i) {
                mi.value = i;
                return true;
            }
            return false;
        }
    }
}
