package com.android.server.policy;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityTaskManager;
import android.app.AppOpsManager;
import android.app.IActivityTaskManager;
import android.app.IApplicationThread;
import android.app.IUiModeManager;
import android.app.NotificationManager;
import android.app.ProfilerInfo;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.app.UiModeManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.hardware.devicestate.DeviceStateManagerInternal;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.hdmi.HdmiAudioSystemClient;
import android.hardware.hdmi.HdmiControlManager;
import android.hardware.hdmi.HdmiPlaybackClient;
import android.hardware.input.InputManager;
import android.hardware.input.InputManagerInternal;
import android.media.AudioManagerInternal;
import android.media.AudioSystem;
import android.media.IAudioService;
import android.media.session.MediaSessionLegacyHelper;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.DeviceIdleManager;
import android.os.FactoryTest;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.service.dreams.DreamManagerInternal;
import android.service.dreams.IDreamManager;
import android.service.vr.IPersistentVrStateCallbacks;
import android.telecom.TelecomManager;
import android.util.Log;
import android.util.MutableBoolean;
import android.util.Pair;
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import android.view.IDisplayFoldListener;
import android.view.IWindowManager;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.WindowManagerPolicyConstants;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.autofill.AutofillManagerInternal;
import com.android.internal.accessibility.AccessibilityShortcutController;
import com.android.internal.accessibility.util.AccessibilityUtils;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.RoSystemProperties;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.policy.IShortcutService;
import com.android.internal.policy.KeyInterceptionInfo;
import com.android.internal.policy.LogDecelerateInterpolator;
import com.android.internal.policy.PhoneWindow;
import com.android.internal.policy.TransitionAnimation;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.ExtconStateObserver;
import com.android.server.ExtconUEventObserver;
import com.android.server.GestureLauncherService;
import com.android.server.LocalServices;
import com.android.server.SystemServiceManager;
import com.android.server.am.HostingRecord;
import com.android.server.display.TranFoldDisplayCustody;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.server.policy.KeyCombinationManager;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.policy.SingleKeyGestureDetector;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.policy.keyguard.KeyguardServiceDelegate;
import com.android.server.policy.keyguard.KeyguardStateMonitor;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.usb.descriptors.UsbACInterface;
import com.android.server.usb.descriptors.UsbDescriptor;
import com.android.server.vr.VrManagerInternal;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.DisplayPolicy;
import com.android.server.wm.DisplayRotation;
import com.android.server.wm.WindowManagerDebugConfig;
import com.android.server.wm.WindowManagerInternal;
import com.android.server.wm.WindowManagerService;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.wm.WindowManagerDebugger;
import com.transsion.hubcore.server.ITranInput;
import com.transsion.hubcore.server.biometrics.fingerprint.ITranFingerprintService;
import com.transsion.hubcore.server.inputmethod.ITranInputMethodManagerService;
import com.transsion.hubcore.server.policy.ITranPhoneWindowManager;
import com.transsion.hubcore.server.wm.ITranDisplayPolicy;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntSupplier;
/* loaded from: classes2.dex */
public class PhoneWindowManager implements WindowManagerPolicy {
    public static final String ACTION_TINNO_REDUCE_SAR_ESCAPE = "com.tinno.action.reduce_sar_escape";
    public static final String ACTION_TINNO_REDUCE_SAR_SENSOR = "com.tinno.action.reduce_sar_sensor";
    private static final String ACTION_VOICE_ASSIST_RETAIL = "android.intent.action.VOICE_ASSIST_RETAIL";
    private static final String ANTI_THEFT_RINGING = "ro.anti_theft";
    private static final String AOD_TRIGGER_MODE_TOUCH = "aod_trigger_mode_touch";
    private static final int BRIGHTNESS_STEPS = 10;
    private static final long BUGREPORT_TV_GESTURE_TIMEOUT_MILLIS = 1000;
    static final int DOUBLE_PRESS_PRIMARY_NOTHING = 0;
    static final int DOUBLE_PRESS_PRIMARY_SWITCH_RECENT_APP = 1;
    static final int DOUBLE_TAP_HOME_NOTHING = 0;
    static final int DOUBLE_TAP_HOME_PIP_MENU = 2;
    static final int DOUBLE_TAP_HOME_RECENT_SYSTEM_UI = 1;
    static final boolean ENABLE_DESK_DOCK_HOME_CAPTURE = false;
    static final boolean ENABLE_VR_HEADSET_HOME_CAPTURE = true;
    private static final VibrationAttributes HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES;
    private static final float KEYGUARD_SCREENSHOT_CHORD_DELAY_MULTIPLIER = 2.5f;
    private static final int KEY_FOR_FINGERPRINT_F28 = 354;
    private static final int KEY_FOR_GESTURE = 355;
    static final int LAST_LONG_PRESS_HOME_BEHAVIOR = 3;
    static final int LONG_PRESS_BACK_GO_TO_VOICE_ASSIST = 1;
    static final int LONG_PRESS_BACK_NOTHING = 0;
    static final int LONG_PRESS_HOME_ALL_APPS = 1;
    static final int LONG_PRESS_HOME_ASSIST = 2;
    static final int LONG_PRESS_HOME_NOTHING = 0;
    static final int LONG_PRESS_HOME_NOTIFICATION_PANEL = 3;
    static final int LONG_PRESS_POWER_ASSISTANT = 5;
    static final int LONG_PRESS_POWER_GLOBAL_ACTIONS = 1;
    static final int LONG_PRESS_POWER_GO_TO_VOICE_ASSIST = 4;
    static final int LONG_PRESS_POWER_NOTHING = 0;
    static final int LONG_PRESS_POWER_SHUT_OFF = 2;
    static final int LONG_PRESS_POWER_SHUT_OFF_NO_CONFIRM = 3;
    static final int LONG_PRESS_PRIMARY_LAUNCH_VOICE_ASSISTANT = 1;
    static final int LONG_PRESS_PRIMARY_NOTHING = 0;
    private static final int MSG_ACCESSIBILITY_SHORTCUT = 17;
    private static final int MSG_ACCESSIBILITY_TV = 19;
    private static final int MSG_BUGREPORT_TV = 18;
    private static final int MSG_DISPATCH_BACK_KEY_TO_AUTOFILL = 20;
    private static final int MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK = 4;
    private static final int MSG_DISPATCH_MEDIA_KEY_WITH_WAKE_LOCK = 3;
    private static final int MSG_DISPATCH_SHOW_GLOBAL_ACTIONS = 10;
    private static final int MSG_DISPATCH_SHOW_RECENTS = 9;
    private static final int MSG_HANDLE_ALL_APPS = 22;
    private static final int MSG_HIDE_BOOT_MESSAGE = 11;
    private static final int MSG_KEYGUARD_DRAWN_COMPLETE = 5;
    private static final int MSG_KEYGUARD_DRAWN_TIMEOUT = 6;
    private static final int MSG_KEYGUARD_HIDE_COMPLETE = 31;
    private static final int MSG_LAUNCH_ASSIST = 23;
    private static final int MSG_LAUNCH_VOICE_ASSIST_WITH_WAKE_LOCK = 12;
    private static final int MSG_POWER_PRESS_WAKE_UP_AIVA = 360;
    private static final int MSG_RINGER_TOGGLE_CHORD = 24;
    private static final int MSG_SCREENSHOT_CHORD = 16;
    private static final int MSG_SHOW_PICTURE_IN_PICTURE_MENU = 15;
    private static final int MSG_SYSTEM_KEY_PRESS = 21;
    private static final int MSG_WINDOW_MANAGER_DRAWN_COMPLETE = 7;
    private static final boolean MTK_AOD_SUPPORT;
    static final int MULTI_PRESS_POWER_BRIGHTNESS_BOOST = 2;
    private static final long MULTI_PRESS_POWER_KEY_EMERG_TIMEOUT = 1300;
    static final int MULTI_PRESS_POWER_LAUNCH_TARGET_ACTIVITY = 3;
    static final int MULTI_PRESS_POWER_NOTHING = 0;
    static final int MULTI_PRESS_POWER_THEATER_MODE = 1;
    private static final int NOTIFY_SHOW_AOD = 1;
    private static final boolean OPTICAL_FINGERPRINT;
    public static final String PARA_SAR = "para_sar";
    static final int PENDING_KEY_NULL = -1;
    private static final VibrationAttributes PHYSICAL_EMULATION_VIBRATION_ATTRIBUTES;
    private static final int POWER_BUTTON_SUPPRESSION_DELAY_DEFAULT_MILLIS = 800;
    static final int POWER_VOLUME_UP_BEHAVIOR_GLOBAL_ACTIONS = 2;
    static final int POWER_VOLUME_UP_BEHAVIOR_MUTE = 1;
    static final int POWER_VOLUME_UP_BEHAVIOR_NOTHING = 0;
    private static final int POWER_WAKEUP_TRAN_ASSIS_VALUE = 500;
    static final int SHORT_PRESS_POWER_CLOSE_IME_OR_GO_HOME = 5;
    static final int SHORT_PRESS_POWER_GO_HOME = 4;
    static final int SHORT_PRESS_POWER_GO_TO_SLEEP = 1;
    static final int SHORT_PRESS_POWER_LOCK_OR_SLEEP = 6;
    static final int SHORT_PRESS_POWER_NOTHING = 0;
    static final int SHORT_PRESS_POWER_REALLY_GO_TO_SLEEP = 2;
    static final int SHORT_PRESS_POWER_REALLY_GO_TO_SLEEP_AND_GO_HOME = 3;
    static final int SHORT_PRESS_PRIMARY_LAUNCH_ALL_APPS = 1;
    static final int SHORT_PRESS_PRIMARY_NOTHING = 0;
    static final int SHORT_PRESS_SLEEP_GO_TO_SLEEP = 0;
    static final int SHORT_PRESS_SLEEP_GO_TO_SLEEP_AND_GO_HOME = 1;
    static final int SHORT_PRESS_WINDOW_NOTHING = 0;
    static final int SHORT_PRESS_WINDOW_PICTURE_IN_PICTURE = 1;
    public static final String SYSTEM_DIALOG_REASON_ASSIST = "assist";
    public static final String SYSTEM_DIALOG_REASON_GESTURE_NAV = "gestureNav";
    public static final String SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS = "globalactions";
    public static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
    public static final String SYSTEM_DIALOG_REASON_KEY = "reason";
    public static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
    public static final String SYSTEM_DIALOG_REASON_SCREENSHOT = "screenshot";
    static final String TAG = "WindowManager";
    private static final String TALKBACK_LABEL = "TalkBack";
    public static final int TOAST_WINDOW_ANIM_BUFFER = 600;
    public static final int TOAST_WINDOW_TIMEOUT = 4100;
    private static final VibrationAttributes TOUCH_VIBRATION_ATTRIBUTES;
    private static final boolean TRAN_EMERGENCY_MESSAGE_SUPPORT;
    static final int TRIPLE_PRESS_PRIMARY_NOTHING = 0;
    static final int TRIPLE_PRESS_PRIMARY_TOGGLE_ACCESSIBILITY = 1;
    static final int VERY_LONG_PRESS_POWER_GLOBAL_ACTIONS = 1;
    static final int VERY_LONG_PRESS_POWER_NOTHING = 0;
    static final int WAITING_FOR_DRAWN_TIMEOUT = 1000;
    static final IntSupplier WAITING_FOR_DRAWN_TIMEOUT_SUPPLIER;
    private static final int[] WINDOW_TYPES_WHERE_HOME_DOESNT_WORK;
    private static final boolean mMultipleDisplayFlipSupport;
    AccessibilityManager mAccessibilityManager;
    private AccessibilityShortcutController mAccessibilityShortcutController;
    ActivityManagerInternal mActivityManagerInternal;
    ActivityTaskManagerInternal mActivityTaskManagerInternal;
    boolean mAllowStartActivityForLongPressOnPowerDuringSetup;
    private boolean mAllowTheaterModeWakeFromCameraLens;
    private boolean mAllowTheaterModeWakeFromKey;
    private boolean mAllowTheaterModeWakeFromLidSwitch;
    private boolean mAllowTheaterModeWakeFromMotion;
    private boolean mAllowTheaterModeWakeFromMotionWhenNotDreaming;
    private boolean mAllowTheaterModeWakeFromPowerKey;
    private boolean mAllowTheaterModeWakeFromWakeGesture;
    AppOpsManager mAppOpsManager;
    AudioManagerInternal mAudioManagerInternal;
    AutofillManagerInternal mAutofillManagerInternal;
    volatile boolean mBackKeyHandled;
    volatile boolean mBootAnimationDismissable;
    boolean mBootMessageNeedsHiding;
    PowerManager.WakeLock mBroadcastWakeLock;
    BurnInProtectionHelper mBurnInProtectionHelper;
    volatile boolean mCameraGestureTriggered;
    volatile boolean mCameraGestureTriggeredDuringGoingToSleep;
    Intent mCarDockIntent;
    Context mContext;
    private int mCurrentUserId;
    Display mDefaultDisplay;
    DisplayPolicy mDefaultDisplayPolicy;
    DisplayRotation mDefaultDisplayRotation;
    Intent mDeskDockIntent;
    volatile boolean mDeviceGoingToSleep;
    private DeviceStateManagerInternal mDeviceStateManagerInternal;
    private volatile boolean mDismissImeOnBackKeyPressed;
    private DisplayFoldController mDisplayFoldController;
    DisplayManager mDisplayManager;
    DisplayManagerInternal mDisplayManagerInternal;
    int mDoublePressOnPowerBehavior;
    int mDoublePressOnStemPrimaryBehavior;
    private int mDoubleTapOnHomeBehavior;
    DreamManagerInternal mDreamManagerInternal;
    volatile boolean mEndCallKeyHandled;
    int mEndcallBehavior;
    private GestureLauncherService mGestureLauncherService;
    private GlobalActions mGlobalActions;
    private GlobalKeyManager mGlobalKeyManager;
    private boolean mGoToSleepOnButtonPressTheaterMode;
    private boolean mHadHideAod;
    private boolean mHandleVolumeKeysInWM;
    private Handler mHandler;
    boolean mHapticTextHandleEnabled;
    private boolean mHasFeatureAuto;
    private boolean mHasFeatureHdmiCec;
    private boolean mHasFeatureLeanback;
    private boolean mHasFeatureWatch;
    boolean mHaveBuiltInKeyboard;
    boolean mHavePendingMediaKeyRepeatWithWakeLock;
    HdmiControl mHdmiControl;
    Intent mHomeIntent;
    int mIncallBackBehavior;
    int mIncallPowerBehavior;
    InputManagerInternal mInputManagerInternal;
    InputMethodManagerInternal mInputMethodManagerInternal;
    private KeyCombinationManager mKeyCombinationManager;
    private boolean mKeyf25Down;
    private boolean mKeyguardBound;
    private KeyguardServiceDelegate mKeyguardDelegate;
    private boolean mKeyguardDrawnOnce;
    private boolean mKeyguardOccludedChanged;
    int mLidKeyboardAccessibility;
    int mLidNavigationAccessibility;
    private LockPatternUtils mLockPatternUtils;
    int mLockScreenTimeout;
    boolean mLockScreenTimerActive;
    MetricsLogger mLogger;
    int mLongPressOnBackBehavior;
    private int mLongPressOnHomeBehavior;
    long mLongPressOnPowerAssistantTimeoutMs;
    int mLongPressOnPowerBehavior;
    int mLongPressOnStemPrimaryBehavior;
    private long mMenuDownTime;
    ModifierShortcutManager mModifierShortcutManager;
    PackageManager mPackageManager;
    boolean mPendingCapsLockToggle;
    private boolean mPendingKeyguardOccluded;
    boolean mPendingMetaAction;
    volatile boolean mPictureInPictureVisible;
    ComponentName mPowerDoublePressTargetActivity;
    volatile boolean mPowerKeyHandled;
    PowerManager.WakeLock mPowerKeyWakeLock;
    PowerManager mPowerManager;
    PowerManagerInternal mPowerManagerInternal;
    int mPowerVolUpBehavior;
    boolean mPreloadedRecentApps;
    int mRecentAppsHeldModifiers;
    volatile boolean mRecentsVisible;
    volatile boolean mRequestedOrSleepingDefaultDisplay;
    boolean mSafeMode;
    long[] mSafeModeEnabledVibePattern;
    private ActivityTaskManagerInternal.SleepTokenAcquirer mScreenOffSleepTokenAcquirer;
    SearchManager mSearchManager;
    SettingsObserver mSettingsObserver;
    int mShortPressOnPowerBehavior;
    int mShortPressOnSleepBehavior;
    int mShortPressOnStemPrimaryBehavior;
    int mShortPressOnWindowBehavior;
    SideFpsEventHandler mSideFpsEventHandler;
    private SingleKeyGestureDetector mSingleKeyGestureDetector;
    StatusBarManagerInternal mStatusBarManagerInternal;
    IStatusBarService mStatusBarService;
    private boolean mSupportLongPressPowerWhenNonInteractive;
    boolean mSystemBooted;
    boolean mSystemNavigationKeysEnabled;
    boolean mSystemReady;
    int mTriplePressOnPowerBehavior;
    int mTriplePressOnStemPrimaryBehavior;
    int mUiMode;
    IUiModeManager mUiModeManager;
    boolean mUseTvRouting;
    int mVeryLongPressOnPowerBehavior;
    Vibrator mVibrator;
    Intent mVrHeadsetHomeIntent;
    volatile VrManagerInternal mVrManagerInternal;
    private boolean mWakeAodEnabledSetting;
    boolean mWakeGestureEnabledSetting;
    MyWakeGestureListener mWakeGestureListener;
    boolean mWakeOnAssistKeyPress;
    boolean mWakeOnBackKeyPress;
    boolean mWakeOnDpadKeyPress;
    long mWakeUpToLastStateTimeout;
    IWindowManager mWindowManager;
    WindowManagerPolicy.WindowManagerFuncs mWindowManagerFuncs;
    WindowManagerInternal mWindowManagerInternal;
    static boolean localLOGV = false;
    static boolean DEBUG_INPUT = WindowManagerDebugConfig.DEBUG_INPUT;
    static boolean DEBUG_KEYGUARD = WindowManagerDebugConfig.DEBUG_KEYGUARD;
    static boolean DEBUG_SPLASH_SCREEN = WindowManagerDebugConfig.DEBUG_SPLASH_SCREEN;
    static boolean DEBUG_WAKEUP = WindowManagerDebugConfig.DEBUG_WAKEUP;
    static boolean SHOW_SPLASH_SCREENS = true;
    private static final boolean TRAN_FACEID_SUPPORT = "1".equals(SystemProperties.get("ro.faceid.support"));
    private boolean PROXIMITY_POWER_KEY_SUPPORT = "1".equals(SystemProperties.get("ro.proximity.powerkey.support"));
    private boolean mIsProximityPowerKeyDown = false;
    private WindowManagerDebugger mWindowManagerDebugger = MtkSystemServiceFactory.getInstance().makeWindowManagerDebugger();
    private boolean isPowerKeyDown = false;
    private final Object mLock = new Object();
    private final SparseArray<WindowManagerPolicy.ScreenOnListener> mScreenOnListeners = new SparseArray<>();
    final Object mServiceAcquireLock = new Object();
    boolean mEnableShiftMenuBugReports = false;
    private boolean mEnableCarDockHomeCapture = true;
    final KeyguardServiceDelegate.DrawnListener mKeyguardDrawnCallback = new KeyguardServiceDelegate.DrawnListener() { // from class: com.android.server.policy.PhoneWindowManager.1
        @Override // com.android.server.policy.keyguard.KeyguardServiceDelegate.DrawnListener
        public void onDrawn() {
            if (PhoneWindowManager.DEBUG_WAKEUP) {
                Slog.d("WindowManager", "mKeyguardDelegate.ShowListener.onDrawn.");
            }
            if (!ITranPhoneWindowManager.Instance().isPreWakeupInProgress()) {
                PhoneWindowManager.this.mHandler.sendEmptyMessage(5);
            }
        }
    };
    volatile boolean mNavBarVirtualKeyHapticFeedbackEnabled = true;
    volatile int mPendingWakeKey = -1;
    int mCameraLensCoverState = -1;
    boolean mHasSoftInput = false;
    private HashSet<Integer> mAllowLockscreenWhenOnDisplays = new HashSet<>();
    int mRingerToggleChord = 0;
    private final SparseArray<KeyCharacterMap.FallbackAction> mFallbackActions = new SparseArray<>();
    private final LogDecelerateInterpolator mLogDecelerateInterpolator = new LogDecelerateInterpolator(100, 0);
    private boolean mPerDisplayFocusEnabled = false;
    private volatile int mTopFocusedDisplayId = -1;
    private int mPowerButtonSuppressionDelayMillis = 800;
    private boolean mLockNowPending = false;
    private long[] mEmergHits = new long[5];
    private boolean mTranVoiceAssistantSupport = SystemProperties.getBoolean("ro.sys.tran.assist_support", false);
    private boolean POWER_PRESS_WAKE_UP_AIVA_FLAG = false;
    private boolean mNeedHideAodWin = false;
    private UEventObserver mHDMIObserver = new UEventObserver() { // from class: com.android.server.policy.PhoneWindowManager.2
        public void onUEvent(UEventObserver.UEvent event) {
            PhoneWindowManager.this.mDefaultDisplayPolicy.setHdmiPlugged("1".equals(event.get("SWITCH_STATE")));
        }
    };
    final IPersistentVrStateCallbacks mPersistentVrModeListener = new IPersistentVrStateCallbacks.Stub() { // from class: com.android.server.policy.PhoneWindowManager.3
        public void onPersistentVrStateChanged(boolean enabled) {
            PhoneWindowManager.this.mDefaultDisplayPolicy.setPersistentVrModeEnabled(enabled);
        }
    };
    private final Runnable mEndCallLongPress = new Runnable() { // from class: com.android.server.policy.PhoneWindowManager.4
        @Override // java.lang.Runnable
        public void run() {
            PhoneWindowManager.this.mEndCallKeyHandled = true;
            PhoneWindowManager.this.performHapticFeedback(0, false, "End Call - Long Press - Show Global Actions");
            PhoneWindowManager.this.showGlobalActionsInternal();
        }
    };
    private final SparseArray<DisplayHomeButtonHandler> mDisplayHomeButtonHandlers = new SparseArray<>();
    BroadcastReceiver mDockReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.PhoneWindowManager.13
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.DOCK_EVENT".equals(intent.getAction())) {
                PhoneWindowManager.this.mDefaultDisplayPolicy.setDockMode(intent.getIntExtra("android.intent.extra.DOCK_STATE", 0));
            } else {
                try {
                    IUiModeManager uiModeService = IUiModeManager.Stub.asInterface(ServiceManager.getService("uimode"));
                    PhoneWindowManager.this.mUiMode = uiModeService.getCurrentModeType();
                } catch (RemoteException e) {
                }
            }
            PhoneWindowManager.this.updateRotation(true);
            PhoneWindowManager.this.mDefaultDisplayRotation.updateOrientationListener();
        }
    };
    BroadcastReceiver mDreamReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.PhoneWindowManager.14
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.DREAMING_STARTED".equals(intent.getAction())) {
                if (PhoneWindowManager.this.mKeyguardDelegate != null) {
                    PhoneWindowManager.this.mKeyguardDelegate.onDreamingStarted();
                }
            } else if ("android.intent.action.DREAMING_STOPPED".equals(intent.getAction()) && PhoneWindowManager.this.mKeyguardDelegate != null) {
                PhoneWindowManager.this.mKeyguardDelegate.onDreamingStopped();
            }
        }
    };
    BroadcastReceiver mMultiuserReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.PhoneWindowManager.15
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                PhoneWindowManager.this.mSettingsObserver.onChange(false);
                PhoneWindowManager.this.mDefaultDisplayRotation.onUserSwitch();
                PhoneWindowManager.this.mWindowManagerFuncs.onUserSwitched();
            }
        }
    };
    private boolean mScreenOnBecacuseOfDisplayTransition = false;
    ProgressDialog mBootMsgDialog = null;
    final ScreenLockTimeout mScreenLockTimeout = new ScreenLockTimeout();

    static {
        boolean z = false;
        if (SystemProperties.getInt("ro.product.multiple_display_flip.support", 0) == 1) {
            z = true;
        }
        mMultipleDisplayFlipSupport = z;
        OPTICAL_FINGERPRINT = "1".equals(SystemProperties.get("ro.optical_fingerprint_support", ""));
        MTK_AOD_SUPPORT = "1".equals(SystemProperties.get("ro.vendor.mtk_aod_support", ""));
        TOUCH_VIBRATION_ATTRIBUTES = VibrationAttributes.createForUsage(18);
        PHYSICAL_EMULATION_VIBRATION_ATTRIBUTES = VibrationAttributes.createForUsage(34);
        HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES = VibrationAttributes.createForUsage(50);
        WAITING_FOR_DRAWN_TIMEOUT_SUPPLIER = new IntSupplier() { // from class: com.android.server.policy.PhoneWindowManager$$ExternalSyntheticLambda2
            @Override // java.util.function.IntSupplier
            public final int getAsInt() {
                int waitingForDrawnTimeout;
                waitingForDrawnTimeout = TranFoldDisplayCustody.instance().getWaitingForDrawnTimeout(1000);
                return waitingForDrawnTimeout;
            }
        };
        TRAN_EMERGENCY_MESSAGE_SUPPORT = "1".equals(SystemProperties.get("ro.tran_emergency_message_support"));
        WINDOW_TYPES_WHERE_HOME_DOESNT_WORK = new int[]{2003, 2010};
    }

    /* loaded from: classes2.dex */
    private class PolicyHandler extends Handler {
        private PolicyHandler() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 3:
                    PhoneWindowManager.this.dispatchMediaKeyWithWakeLock((KeyEvent) msg.obj);
                    return;
                case 4:
                    PhoneWindowManager.this.dispatchMediaKeyRepeatWithWakeLock((KeyEvent) msg.obj);
                    return;
                case 5:
                    if (PhoneWindowManager.DEBUG_WAKEUP) {
                        Slog.w("WindowManager", "Setting mKeyguardDrawComplete");
                    }
                    PhoneWindowManager.this.finishKeyguardDrawn();
                    return;
                case 6:
                    Slog.w("WindowManager", "Keyguard drawn timeout. Setting mKeyguardDrawComplete");
                    PhoneWindowManager.this.finishKeyguardDrawn();
                    return;
                case 7:
                    if (PhoneWindowManager.DEBUG_WAKEUP) {
                        Slog.w("WindowManager", "Setting mWindowManagerDrawComplete");
                    }
                    PhoneWindowManager.this.finishWindowsDrawn(msg.arg1);
                    return;
                case 9:
                    PhoneWindowManager.this.showRecentApps(false);
                    return;
                case 10:
                    PhoneWindowManager.this.showGlobalActionsInternal();
                    return;
                case 11:
                    PhoneWindowManager.this.handleHideBootMessage();
                    return;
                case 12:
                    PhoneWindowManager.this.launchVoiceAssistWithWakeLock();
                    return;
                case 15:
                    PhoneWindowManager.this.showPictureInPictureMenuInternal();
                    return;
                case 16:
                    PhoneWindowManager.this.handleScreenShot(msg.arg1, msg.arg2);
                    return;
                case 17:
                    PhoneWindowManager.this.accessibilityShortcutActivated();
                    return;
                case 18:
                    PhoneWindowManager.this.requestBugreportForTv();
                    return;
                case 19:
                    if (PhoneWindowManager.this.mAccessibilityShortcutController.isAccessibilityShortcutAvailable(false)) {
                        PhoneWindowManager.this.accessibilityShortcutActivated();
                        return;
                    }
                    return;
                case 20:
                    PhoneWindowManager.this.mAutofillManagerInternal.onBackKeyPressed();
                    return;
                case 21:
                    PhoneWindowManager.this.sendSystemKeyToStatusBar(msg.arg1);
                    return;
                case 22:
                    PhoneWindowManager.this.launchAllAppsAction();
                    return;
                case 23:
                    int deviceId = msg.arg1;
                    Long eventTime = (Long) msg.obj;
                    PhoneWindowManager.this.launchAssistAction(null, deviceId, eventTime.longValue(), 0);
                    return;
                case 24:
                    PhoneWindowManager.this.handleRingerChordGesture();
                    return;
                case 31:
                    Slog.i("WindowManager", "  Keyguard hided, Setting mKeyguardDrawComplete");
                    PhoneWindowManager.this.finishKeyguardDrawn();
                    return;
                case PhoneWindowManager.MSG_POWER_PRESS_WAKE_UP_AIVA /* 360 */:
                    Log.d("WindowManager", "MSG_POWER_PRESS_WAKE_UP_AIVA: " + msg.toString());
                    if (PhoneWindowManager.this.mTranVoiceAssistantSupport && !PhoneWindowManager.this.mKeyCombinationManager.isPowerKeyIntercepted()) {
                        PhoneWindowManager.this.controlAIVA("wakeup_by_power_key", true);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void controlAIVA(String type, boolean isPowerKeyWakeUp) {
        Log.d("WindowManager", "controlAIVA type is" + type);
        Intent intent = new Intent();
        intent.setAction("com.transsion.aivoiceassistant.action.AWAKE");
        intent.setPackage("com.transsion.aivoiceassistant");
        intent.putExtra("wakeup_type", type);
        PackageManager pm = this.mContext.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(intent, 0);
        Log.d("WindowManager", "resolveInfo");
        if (resolveInfo == null || resolveInfo.size() != 1) {
            Log.d("WindowManager", "not find FloatWindowService");
            return;
        }
        Log.d("WindowManager", "startService");
        this.POWER_PRESS_WAKE_UP_AIVA_FLAG = isPowerKeyWakeUp;
        this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
    }

    /* loaded from: classes2.dex */
    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = PhoneWindowManager.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("end_button_behavior"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("incall_power_button_behavior"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("incall_back_button_behavior"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("wake_gesture_enabled"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("screen_off_timeout"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("default_input_method"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("volume_hush_gesture"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("system_navigation_keys_enabled"), false, this, -1);
            resolver.registerContentObserver(Settings.Global.getUriFor("power_button_long_press"), false, this, -1);
            resolver.registerContentObserver(Settings.Global.getUriFor("power_button_long_press_duration_ms"), false, this, -1);
            resolver.registerContentObserver(Settings.Global.getUriFor("power_button_very_long_press"), false, this, -1);
            resolver.registerContentObserver(Settings.Global.getUriFor("key_chord_power_volume_up"), false, this, -1);
            resolver.registerContentObserver(Settings.Global.getUriFor("power_button_suppression_delay_after_gesture_wake"), false, this, -1);
            resolver.registerContentObserver(Settings.Global.getUriFor("keyguard_gesture"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor(PhoneWindowManager.AOD_TRIGGER_MODE_TOUCH), false, this, -1);
            PhoneWindowManager.this.updateSettings();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            PhoneWindowManager.this.updateSettings();
            PhoneWindowManager.this.updateRotation(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class MyWakeGestureListener extends WakeGestureListener {
        MyWakeGestureListener(Context context, Handler handler) {
            super(context, handler);
        }

        @Override // com.android.server.policy.WakeGestureListener
        public void onWakeUp() {
            synchronized (PhoneWindowManager.this.mLock) {
                if (PhoneWindowManager.this.shouldEnableWakeGestureLp()) {
                    PhoneWindowManager.this.performHapticFeedback(1, false, "Wake Up");
                    PhoneWindowManager.this.wakeUp(SystemClock.uptimeMillis(), PhoneWindowManager.this.mAllowTheaterModeWakeFromWakeGesture, 4, "android.policy:GESTURE");
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleRingerChordGesture() {
        if (this.mRingerToggleChord == 0) {
            return;
        }
        getAudioManagerInternal();
        this.mAudioManagerInternal.silenceRingerModeInternal("volume_hush");
        Settings.Secure.putInt(this.mContext.getContentResolver(), "hush_gesture_used", 1);
        this.mLogger.action(1440, this.mRingerToggleChord);
    }

    IStatusBarService getStatusBarService() {
        IStatusBarService iStatusBarService;
        synchronized (this.mServiceAcquireLock) {
            if (this.mStatusBarService == null) {
                this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
            }
            iStatusBarService = this.mStatusBarService;
        }
        return iStatusBarService;
    }

    StatusBarManagerInternal getStatusBarManagerInternal() {
        StatusBarManagerInternal statusBarManagerInternal;
        synchronized (this.mServiceAcquireLock) {
            if (this.mStatusBarManagerInternal == null) {
                this.mStatusBarManagerInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
            }
            statusBarManagerInternal = this.mStatusBarManagerInternal;
        }
        return statusBarManagerInternal;
    }

    AudioManagerInternal getAudioManagerInternal() {
        AudioManagerInternal audioManagerInternal;
        synchronized (this.mServiceAcquireLock) {
            if (this.mAudioManagerInternal == null) {
                this.mAudioManagerInternal = (AudioManagerInternal) LocalServices.getService(AudioManagerInternal.class);
            }
            audioManagerInternal = this.mAudioManagerInternal;
        }
        return audioManagerInternal;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean backKeyPress() {
        TelecomManager telecomManager;
        this.mLogger.count("key_back_press", 1);
        boolean handled = this.mBackKeyHandled;
        if (this.mHasFeatureWatch && (telecomManager = getTelecommService()) != null) {
            if (telecomManager.isRinging()) {
                telecomManager.silenceRinger();
                return false;
            } else if ((1 & this.mIncallBackBehavior) != 0 && telecomManager.isInCall()) {
                return telecomManager.endCall();
            }
        }
        if (this.mAutofillManagerInternal != null) {
            Handler handler = this.mHandler;
            handler.sendMessage(handler.obtainMessage(20));
        }
        return handled;
    }

    private void interceptPowerKeyDown(KeyEvent event, boolean interactive) {
        if (!this.mPowerKeyWakeLock.isHeld()) {
            this.mPowerKeyWakeLock.acquire();
        }
        this.mWindowManagerFuncs.onPowerKeyDown(interactive);
        boolean z = true;
        if (this.PROXIMITY_POWER_KEY_SUPPORT) {
            this.mIsProximityPowerKeyDown = true;
        }
        TelecomManager telecomManager = getTelecommService();
        boolean hungUp = false;
        if (telecomManager != null) {
            if (telecomManager.isRinging()) {
                telecomManager.silenceRinger();
            } else if ((this.mIncallPowerBehavior & 2) != 0 && telecomManager.isInCall() && interactive) {
                hungUp = telecomManager.endCall();
            }
        }
        boolean handledByPowerManager = this.mPowerManagerInternal.interceptPowerKeyDown(event);
        Log.d("WindowManager", "mTranVoiceAssistantSupport: " + this.mTranVoiceAssistantSupport);
        this.POWER_PRESS_WAKE_UP_AIVA_FLAG = false;
        if (this.mTranVoiceAssistantSupport) {
            boolean isPowerWakeup = SystemProperties.getBoolean("persist.sys.tran_assist_power_wakeup", false);
            Log.d("WindowManager", "interceptPowerKeyDown: isPowerWakeup" + isPowerWakeup);
            if (isPowerWakeup) {
                Message message = this.mHandler.obtainMessage(MSG_POWER_PRESS_WAKE_UP_AIVA);
                message.setAsynchronous(true);
                this.mHandler.sendMessageDelayed(message, 500L);
                Log.d("WindowManager", "interceptPowerKeyDown: sendMessageDelayed MSG_POWER_PRESS_WAKE_UP_AIVA");
            }
        }
        sendSystemKeyToStatusBarAsync(event.getKeyCode());
        if (!this.mPowerKeyHandled && !hungUp && !handledByPowerManager && !this.mKeyCombinationManager.isPowerKeyIntercepted()) {
            z = false;
        }
        this.mPowerKeyHandled = z;
        if (!this.mPowerKeyHandled) {
            if (!interactive) {
                wakeUpFromPowerKey(event.getDownTime());
            }
        } else if (!this.mSingleKeyGestureDetector.isKeyIntercepted(26)) {
            this.mSingleKeyGestureDetector.reset();
        }
    }

    private void interceptPowerKeyUp(KeyEvent event, boolean canceled) {
        boolean handled = canceled || this.mPowerKeyHandled;
        if (this.mTranVoiceAssistantSupport) {
            this.mHandler.removeMessages(MSG_POWER_PRESS_WAKE_UP_AIVA);
            Log.d("WindowManager", "removeMessages: MSG_POWER_PRESS_WAKE_UP_AIVA");
        }
        if (!handled) {
            if ((event.getFlags() & 128) == 0) {
                Handler handler = this.mHandler;
                final WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs = this.mWindowManagerFuncs;
                Objects.requireNonNull(windowManagerFuncs);
                handler.post(new Runnable() { // from class: com.android.server.policy.PhoneWindowManager$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        WindowManagerPolicy.WindowManagerFuncs.this.triggerAnimationFailsafe();
                    }
                });
            }
        } else if (!this.mSingleKeyGestureDetector.isKeyIntercepted(26)) {
            this.mSingleKeyGestureDetector.reset();
        }
        finishPowerKeyPress();
    }

    private void finishPowerKeyPress() {
        this.mPowerKeyHandled = false;
        if (this.mPowerKeyWakeLock.isHeld()) {
            this.mPowerKeyWakeLock.release();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void powerPress(long eventTime, int count, boolean beganFromNonInteractive) {
        if (this.mDefaultDisplayPolicy.isScreenOnEarly() && !this.mDefaultDisplayPolicy.isScreenOnFully()) {
            Slog.i("WindowManager", "Suppressed redundant power key press while already in the process of turning the screen on.");
            return;
        }
        boolean interactive = Display.isOnState(this.mDefaultDisplay.getState()) || (mMultipleDisplayFlipSupport && Display.isOnState(this.mDisplayManager.getDisplay(1).getState()));
        Slog.d("WindowManager", "powerPress: eventTime=" + eventTime + " interactive=" + interactive + " count=" + count + " beganFromNonInteractive=" + beganFromNonInteractive + " mShortPressOnPowerBehavior=" + this.mShortPressOnPowerBehavior);
        if (count == 2) {
            powerMultiPressAction(eventTime, interactive, this.mDoublePressOnPowerBehavior);
        } else if (count == 3) {
            powerMultiPressAction(eventTime, interactive, this.mTriplePressOnPowerBehavior);
        } else if (count > 3 && count <= getMaxMultiPressPowerCount()) {
            Slog.d("WindowManager", "No behavior defined for power press count " + count);
        } else if (count == 1 && interactive && !beganFromNonInteractive) {
            if (this.mSideFpsEventHandler.onSinglePressDetected(eventTime)) {
                Slog.i("WindowManager", "Suppressing power key because the user is interacting with the fingerprint sensor");
                return;
            }
            switch (this.mShortPressOnPowerBehavior) {
                case 0:
                default:
                    return;
                case 1:
                    if (this.mTranVoiceAssistantSupport) {
                        Log.d("WindowManager", "POWER_PRESS_WAKE_UP_AIVA_FLAG: " + this.POWER_PRESS_WAKE_UP_AIVA_FLAG);
                        if (this.POWER_PRESS_WAKE_UP_AIVA_FLAG) {
                            this.POWER_PRESS_WAKE_UP_AIVA_FLAG = false;
                            return;
                        }
                    }
                    if (!ITranPhoneWindowManager.Instance().isInterceptPowerShortPressGoToSleep()) {
                        sleepDefaultDisplayFromPowerButton(eventTime, 0);
                        return;
                    }
                    return;
                case 2:
                    sleepDefaultDisplayFromPowerButton(eventTime, 1);
                    return;
                case 3:
                    if (sleepDefaultDisplayFromPowerButton(eventTime, 1)) {
                        launchHomeFromHotKey(0);
                        return;
                    }
                    return;
                case 4:
                    shortPressPowerGoHome();
                    return;
                case 5:
                    if (this.mDismissImeOnBackKeyPressed) {
                        if (this.mInputMethodManagerInternal == null) {
                            this.mInputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
                        }
                        InputMethodManagerInternal inputMethodManagerInternal = this.mInputMethodManagerInternal;
                        if (inputMethodManagerInternal != null) {
                            inputMethodManagerInternal.hideCurrentInputMethod(16);
                            return;
                        }
                        return;
                    }
                    shortPressPowerGoHome();
                    return;
                case 6:
                    KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
                    if (keyguardServiceDelegate == null || !keyguardServiceDelegate.hasKeyguard() || !this.mKeyguardDelegate.isSecure(this.mCurrentUserId) || keyguardOn()) {
                        sleepDefaultDisplayFromPowerButton(eventTime, 0);
                        return;
                    } else {
                        lockNow(null);
                        return;
                    }
            }
        }
    }

    private boolean sleepDefaultDisplayFromPowerButton(long eventTime, int flags) {
        PowerManager.WakeData lastWakeUp = this.mPowerManagerInternal.getLastWakeup();
        if (lastWakeUp != null && lastWakeUp.wakeReason == 4) {
            Settings.Global.getInt(this.mContext.getContentResolver(), "power_button_suppression_delay_after_gesture_wake", 800);
            long now = SystemClock.uptimeMillis();
            if (this.mPowerButtonSuppressionDelayMillis > 0 && now < lastWakeUp.wakeTime + this.mPowerButtonSuppressionDelayMillis) {
                Slog.i("WindowManager", "Sleep from power button suppressed. Time since gesture: " + (now - lastWakeUp.wakeTime) + "ms");
                return false;
            }
        }
        sleepDefaultDisplay(eventTime, 4, flags);
        return true;
    }

    private void sleepDefaultDisplay(long eventTime, int reason, int flags) {
        this.mRequestedOrSleepingDefaultDisplay = true;
        this.mPowerManager.goToSleep(eventTime, reason, flags);
    }

    private void shortPressPowerGoHome() {
        launchHomeFromHotKey(0, true, false);
        if (isKeyguardShowingAndNotOccluded()) {
            this.mKeyguardDelegate.onShortPowerPressedGoHome();
        }
    }

    private void powerMultiPressAction(long eventTime, boolean interactive, int behavior) {
        switch (behavior) {
            case 0:
            default:
                return;
            case 1:
                if (!isUserSetupComplete()) {
                    Slog.i("WindowManager", "Ignoring toggling theater mode - device not setup.");
                    return;
                } else if (isTheaterModeEnabled()) {
                    Slog.i("WindowManager", "Toggling theater mode off.");
                    Settings.Global.putInt(this.mContext.getContentResolver(), "theater_mode_on", 0);
                    if (!interactive) {
                        wakeUpFromPowerKey(eventTime);
                        return;
                    }
                    return;
                } else {
                    Slog.i("WindowManager", "Toggling theater mode on.");
                    Settings.Global.putInt(this.mContext.getContentResolver(), "theater_mode_on", 1);
                    if (this.mGoToSleepOnButtonPressTheaterMode && interactive) {
                        sleepDefaultDisplay(eventTime, 4, 0);
                        return;
                    }
                    return;
                }
            case 2:
                Slog.i("WindowManager", "Starting brightness boost.");
                if (!interactive) {
                    wakeUpFromPowerKey(eventTime);
                }
                this.mPowerManager.boostScreenBrightness(eventTime);
                return;
            case 3:
                launchTargetActivityOnMultiPressPower();
                return;
        }
    }

    private void launchTargetActivityOnMultiPressPower() {
        if (DEBUG_INPUT) {
            Slog.d("WindowManager", "Executing the double press power action.");
        }
        if (this.mPowerDoublePressTargetActivity != null) {
            Intent intent = new Intent();
            intent.setComponent(this.mPowerDoublePressTargetActivity);
            boolean z = false;
            ResolveInfo resolveInfo = this.mContext.getPackageManager().resolveActivity(intent, 0);
            if (resolveInfo != null) {
                KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
                if (keyguardServiceDelegate != null && keyguardServiceDelegate.isShowing()) {
                    z = true;
                }
                boolean keyguardActive = z;
                intent.addFlags(270532608);
                if (!keyguardActive) {
                    startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
                    return;
                } else {
                    this.mKeyguardDelegate.dismissKeyguardToLaunch(intent);
                    return;
                }
            }
            Slog.e("WindowManager", "Could not resolve activity with : " + this.mPowerDoublePressTargetActivity.flattenToString() + " name.");
        }
    }

    private int getLidBehavior() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "lid_behavior", 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getMaxMultiPressPowerCount() {
        if (this.mHasFeatureWatch && GestureLauncherService.isEmergencyGestureSettingEnabled(this.mContext, ActivityManager.getCurrentUser())) {
            return 5;
        }
        if (this.mTriplePressOnPowerBehavior != 0) {
            return 3;
        }
        if (this.mDoublePressOnPowerBehavior != 0) {
            return 2;
        }
        return 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void powerLongPress(long eventTime) {
        int behavior = getResolvedLongPressOnPowerBehavior();
        Slog.d("WindowManager", "powerLongPress: eventTime=" + eventTime + " mLongPressOnPowerBehavior=" + this.mLongPressOnPowerBehavior + ", behavior = " + behavior);
        switch (behavior) {
            case 0:
            default:
                return;
            case 1:
                this.mPowerKeyHandled = true;
                performHapticFeedback(FrameworkStatsLog.MOBILE_BYTES_TRANSFER_BY_FG_BG, false, "Power - Long Press - Global Actions");
                showGlobalActions();
                resetLockoutAttemptDeadline(this.mCurrentUserId);
                if (this.mTranVoiceAssistantSupport) {
                    controlAIVA("close_by_power_off", false);
                    return;
                }
                return;
            case 2:
            case 3:
                this.mPowerKeyHandled = true;
                performHapticFeedback(FrameworkStatsLog.MOBILE_BYTES_TRANSFER_BY_FG_BG, false, "Power - Long Press - Shut Off");
                resetLockoutAttemptDeadline(this.mCurrentUserId);
                sendCloseSystemWindows(SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS);
                this.mWindowManagerFuncs.shutdown(behavior == 2);
                return;
            case 4:
                this.mPowerKeyHandled = true;
                performHapticFeedback(FrameworkStatsLog.MOBILE_BYTES_TRANSFER_BY_FG_BG, false, "Power - Long Press - Go To Voice Assist");
                launchVoiceAssist(this.mAllowStartActivityForLongPressOnPowerDuringSetup);
                return;
            case 5:
                this.mPowerKeyHandled = true;
                performHapticFeedback(FrameworkStatsLog.MOBILE_BYTES_TRANSFER, false, "Power - Long Press - Go To Assistant");
                launchAssistAction(null, Integer.MIN_VALUE, eventTime, 6);
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void powerVeryLongPress() {
        switch (this.mVeryLongPressOnPowerBehavior) {
            case 0:
            default:
                return;
            case 1:
                this.mPowerKeyHandled = true;
                performHapticFeedback(FrameworkStatsLog.MOBILE_BYTES_TRANSFER_BY_FG_BG, false, "Power - Very Long Press - Show Global Actions");
                showGlobalActions();
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void backLongPress() {
        this.mBackKeyHandled = true;
        switch (this.mLongPressOnBackBehavior) {
            case 0:
            default:
                return;
            case 1:
                launchVoiceAssist(false);
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void accessibilityShortcutActivated() {
        this.mAccessibilityShortcutController.performAccessibilityShortcut();
    }

    private void sleepPress() {
        if (this.mShortPressOnSleepBehavior == 1) {
            launchHomeFromHotKey(0, false, true);
        }
    }

    private void sleepRelease(long eventTime) {
        switch (this.mShortPressOnSleepBehavior) {
            case 0:
            case 1:
                Slog.i("WindowManager", "sleepRelease() calling goToSleep(GO_TO_SLEEP_REASON_SLEEP_BUTTON)");
                sleepDefaultDisplay(eventTime, 6, 0);
                return;
            default:
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getResolvedLongPressOnPowerBehavior() {
        if (FactoryTest.isLongPressOnPowerOffEnabled()) {
            return 3;
        }
        if (this.mLongPressOnPowerBehavior == 5 && !isDeviceProvisioned()) {
            return 1;
        }
        if (this.mLongPressOnPowerBehavior == 4 && !isLongPressToAssistantEnabled(this.mContext)) {
            return 0;
        }
        return this.mLongPressOnPowerBehavior;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stemPrimaryPress(int count) {
        if (DEBUG_INPUT) {
            Slog.d("WindowManager", "stemPrimaryPress: " + count);
        }
        if (count == 3) {
            stemPrimaryTriplePressAction(this.mTriplePressOnStemPrimaryBehavior);
        } else if (count == 2) {
            stemPrimaryDoublePressAction(this.mDoublePressOnStemPrimaryBehavior);
        } else if (count == 1) {
            stemPrimarySinglePressAction(this.mShortPressOnStemPrimaryBehavior);
        }
    }

    private void stemPrimarySinglePressAction(int behavior) {
        switch (behavior) {
            case 0:
            default:
                return;
            case 1:
                if (DEBUG_INPUT) {
                    Slog.d("WindowManager", "Executing stem primary short press action behavior.");
                }
                KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
                boolean keyguardActive = keyguardServiceDelegate != null && keyguardServiceDelegate.isShowing();
                if (!keyguardActive) {
                    Intent intent = new Intent("android.intent.action.ALL_APPS");
                    intent.addFlags(270532608);
                    startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
                    return;
                }
                this.mKeyguardDelegate.onSystemKeyPressed(264);
                return;
        }
    }

    private void stemPrimaryDoublePressAction(int behavior) {
        boolean keyguardActive;
        switch (behavior) {
            case 0:
            default:
                return;
            case 1:
                if (DEBUG_INPUT) {
                    Slog.d("WindowManager", "Executing stem primary double press action behavior.");
                }
                KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
                if (keyguardServiceDelegate == null) {
                    keyguardActive = false;
                } else {
                    keyguardActive = keyguardServiceDelegate.isShowing();
                }
                if (!keyguardActive) {
                    switchRecentTask();
                    return;
                }
                return;
        }
    }

    private void stemPrimaryTriplePressAction(int behavior) {
        switch (behavior) {
            case 0:
            default:
                return;
            case 1:
                if (DEBUG_INPUT) {
                    Slog.d("WindowManager", "Executing stem primary triple press action behavior.");
                }
                toggleTalkBack();
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stemPrimaryLongPress() {
        if (DEBUG_INPUT) {
            Slog.d("WindowManager", "Executing stem primary long press action behavior.");
        }
        switch (this.mLongPressOnStemPrimaryBehavior) {
            case 0:
            default:
                return;
            case 1:
                launchVoiceAssist(false);
                return;
        }
    }

    private void toggleTalkBack() {
        ComponentName componentName = getTalkbackComponent();
        if (componentName == null) {
            return;
        }
        Set<ComponentName> enabledServices = AccessibilityUtils.getEnabledServicesFromSettings(this.mContext, this.mCurrentUserId);
        AccessibilityUtils.setAccessibilityServiceState(this.mContext, componentName, !enabledServices.contains(componentName));
    }

    private ComponentName getTalkbackComponent() {
        AccessibilityManager accessibilityManager = (AccessibilityManager) this.mContext.getSystemService(AccessibilityManager.class);
        List<AccessibilityServiceInfo> serviceInfos = accessibilityManager.getInstalledAccessibilityServiceList();
        for (AccessibilityServiceInfo service : serviceInfos) {
            ServiceInfo serviceInfo = service.getResolveInfo().serviceInfo;
            if (isTalkback(serviceInfo)) {
                return new ComponentName(serviceInfo.packageName, serviceInfo.name);
            }
        }
        return null;
    }

    private boolean isTalkback(ServiceInfo info) {
        String label = info.loadLabel(this.mPackageManager).toString();
        return label.equals(TALKBACK_LABEL);
    }

    private void switchRecentTask() {
        ActivityManager.RecentTaskInfo targetTask = this.mActivityTaskManagerInternal.getMostRecentTaskFromBackground();
        if (targetTask == null) {
            if (DEBUG_INPUT) {
                Slog.w("WindowManager", "No recent task available! Show watch face.");
            }
            goHome();
            return;
        }
        if (DEBUG_INPUT) {
            Slog.d("WindowManager", "Starting task from recents. id=" + targetTask.id + ", persistentId=" + targetTask.persistentId + ", topActivity=" + targetTask.topActivity + ", baseIntent=" + targetTask.baseIntent);
        }
        try {
            ActivityManager.getService().startActivityFromRecents(targetTask.persistentId, (Bundle) null);
        } catch (RemoteException | IllegalArgumentException e) {
            Slog.e("WindowManager", "Failed to start task " + targetTask.persistentId + " from recents", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getMaxMultiPressStemPrimaryCount() {
        switch (this.mTriplePressOnStemPrimaryBehavior) {
            case 1:
                if (Settings.System.getIntForUser(this.mContext.getContentResolver(), "wear_accessibility_gesture_enabled", 0, -2) == 1) {
                    return 3;
                }
                break;
        }
        return this.mDoublePressOnStemPrimaryBehavior != 0 ? 2 : 1;
    }

    private boolean hasLongPressOnPowerBehavior() {
        return getResolvedLongPressOnPowerBehavior() != 0;
    }

    private boolean hasVeryLongPressOnPowerBehavior() {
        return this.mVeryLongPressOnPowerBehavior != 0;
    }

    private boolean hasLongPressOnBackBehavior() {
        return this.mLongPressOnBackBehavior != 0;
    }

    private boolean hasLongPressOnStemPrimaryBehavior() {
        return this.mLongPressOnStemPrimaryBehavior != 0;
    }

    private boolean hasStemPrimaryBehavior() {
        return getMaxMultiPressStemPrimaryCount() > 1 || hasLongPressOnStemPrimaryBehavior() || this.mShortPressOnStemPrimaryBehavior != 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void interceptScreenshotChord(int type, int source, long pressDelay) {
        this.mHandler.removeMessages(16);
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(handler.obtainMessage(16, type, source), pressDelay);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void interceptAccessibilityShortcutChord() {
        this.mHandler.removeMessages(17);
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(handler.obtainMessage(17), getAccessibilityShortcutTimeout());
    }

    private boolean isAntiTheftRing(Context context) {
        return 1 == Settings.Secure.getInt(context.getContentResolver(), ANTI_THEFT_RINGING, 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void interceptRingerToggleChord() {
        Log.i("WindowManager", "isKeyguardShowing:" + isKeyguardShowing() + "\nisKeyguardSecure:" + isKeyguardSecure(this.mCurrentUserId) + "\nisAntiTheftRing:" + isAntiTheftRing(this.mContext));
        if (isKeyguardShowing() && isKeyguardSecure(this.mCurrentUserId) && isAntiTheftRing(this.mContext)) {
            return;
        }
        this.mHandler.removeMessages(24);
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(handler.obtainMessage(24), getRingerToggleChordDelay());
    }

    private long getAccessibilityShortcutTimeout() {
        ViewConfiguration config = ViewConfiguration.get(this.mContext);
        boolean hasDialogShown = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_shortcut_dialog_shown", 0, this.mCurrentUserId) != 0;
        boolean skipTimeoutRestriction = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "skip_accessibility_shortcut_dialog_timeout_restriction", 0, this.mCurrentUserId) != 0;
        if (hasDialogShown || skipTimeoutRestriction) {
            return config.getAccessibilityShortcutKeyTimeoutAfterConfirmation();
        }
        return config.getAccessibilityShortcutKeyTimeout();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public long getScreenshotChordLongPressDelay() {
        long delayMs = DeviceConfig.getLong("systemui", "screenshot_keychord_delay", ViewConfiguration.get(this.mContext).getScreenshotChordKeyTimeout());
        if (this.mKeyguardDelegate.isShowing()) {
            return ((float) delayMs) * KEYGUARD_SCREENSHOT_CHORD_DELAY_MULTIPLIER;
        }
        return delayMs;
    }

    private long getRingerToggleChordDelay() {
        return ViewConfiguration.getTapTimeout();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelPendingScreenshotChordAction() {
        this.mHandler.removeMessages(16);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelPendingAccessibilityShortcutAction() {
        this.mHandler.removeMessages(17);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelPendingRingerToggleChordAction() {
        this.mHandler.removeMessages(24);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleScreenShot(int type, int source) {
        this.mDefaultDisplayPolicy.takeScreenshot(type, source);
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void showGlobalActions() {
        this.mHandler.removeMessages(10);
        this.mHandler.sendEmptyMessage(10);
    }

    void showGlobalActionsInternal() {
        if (this.mGlobalActions == null) {
            this.mGlobalActions = new GlobalActions(this.mContext, this.mWindowManagerFuncs);
        }
        boolean keyguardShowing = isKeyguardShowingAndNotOccluded();
        this.mGlobalActions.showDialog(keyguardShowing, isDeviceProvisioned());
        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelGlobalActionsAction() {
        this.mHandler.removeMessages(10);
    }

    boolean isDeviceProvisioned() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public boolean isUserSetupComplete() {
        boolean isSetupComplete = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
        if (this.mHasFeatureLeanback) {
            return isSetupComplete & isTvUserSetupComplete();
        }
        if (this.mHasFeatureAuto) {
            return isSetupComplete & isAutoUserSetupComplete();
        }
        return isSetupComplete;
    }

    private boolean isAutoUserSetupComplete() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "android.car.SETUP_WIZARD_IN_PROGRESS", 0, -2) == 0;
    }

    private boolean isTvUserSetupComplete() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "tv_user_setup_complete", 0, -2) != 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleShortPressOnHome(int displayId) {
        HdmiControl hdmiControl = getHdmiControl();
        if (hdmiControl != null) {
            hdmiControl.turnOnTv();
        }
        DreamManagerInternal dreamManagerInternal = this.mDreamManagerInternal;
        if (dreamManagerInternal != null && dreamManagerInternal.isDreaming()) {
            this.mDreamManagerInternal.stopDream(false);
        } else {
            launchHomeFromHotKey(displayId);
        }
    }

    private HdmiControl getHdmiControl() {
        if (this.mHdmiControl == null) {
            if (!this.mHasFeatureHdmiCec) {
                return null;
            }
            HdmiControlManager manager = (HdmiControlManager) this.mContext.getSystemService("hdmi_control");
            HdmiPlaybackClient client = null;
            if (manager != null) {
                client = manager.getPlaybackClient();
            }
            this.mHdmiControl = new HdmiControl(client);
        }
        return this.mHdmiControl;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class HdmiControl {
        private final HdmiPlaybackClient mClient;

        private HdmiControl(HdmiPlaybackClient client) {
            this.mClient = client;
        }

        public void turnOnTv() {
            HdmiPlaybackClient hdmiPlaybackClient = this.mClient;
            if (hdmiPlaybackClient == null) {
                return;
            }
            hdmiPlaybackClient.oneTouchPlay(new HdmiPlaybackClient.OneTouchPlayCallback() { // from class: com.android.server.policy.PhoneWindowManager.HdmiControl.1
                public void onComplete(int result) {
                    if (result != 0) {
                        Log.w("WindowManager", "One touch play failed: " + result);
                    }
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void launchAllAppsAction() {
        Intent intent = new Intent("android.intent.action.ALL_APPS");
        if (this.mHasFeatureLeanback) {
            Intent intentLauncher = new Intent("android.intent.action.MAIN");
            intentLauncher.addCategory("android.intent.category.HOME");
            ResolveInfo resolveInfo = this.mPackageManager.resolveActivityAsUser(intentLauncher, 1048576, this.mCurrentUserId);
            if (resolveInfo != null) {
                intent.setPackage(resolveInfo.activityInfo.packageName);
            }
        }
        startActivityAsUser(intent, UserHandle.CURRENT);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void toggleNotificationPanel() {
        IStatusBarService statusBarService = getStatusBarService();
        if (statusBarService != null) {
            try {
                statusBarService.togglePanel();
            } catch (RemoteException e) {
            }
        }
    }

    private void showPictureInPictureMenu(KeyEvent event) {
        if (DEBUG_INPUT) {
            Log.d("WindowManager", "showPictureInPictureMenu event=" + event);
        }
        this.mHandler.removeMessages(15);
        Message msg = this.mHandler.obtainMessage(15);
        msg.setAsynchronous(true);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showPictureInPictureMenuInternal() {
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar != null) {
            statusbar.showPictureInPictureMenu();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class DisplayHomeButtonHandler {
        private final int mDisplayId;
        private boolean mHomeConsumed;
        private boolean mHomeDoubleTapPending;
        private final Runnable mHomeDoubleTapTimeoutRunnable = new Runnable() { // from class: com.android.server.policy.PhoneWindowManager.DisplayHomeButtonHandler.1
            @Override // java.lang.Runnable
            public void run() {
                if (DisplayHomeButtonHandler.this.mHomeDoubleTapPending) {
                    DisplayHomeButtonHandler.this.mHomeDoubleTapPending = false;
                    PhoneWindowManager.this.handleShortPressOnHome(DisplayHomeButtonHandler.this.mDisplayId);
                }
            }
        };
        private boolean mHomePressed;

        DisplayHomeButtonHandler(int displayId) {
            this.mDisplayId = displayId;
        }

        int handleHomeButton(IBinder focusedToken, final KeyEvent event) {
            int[] iArr;
            boolean keyguardOn = PhoneWindowManager.this.keyguardOn();
            int repeatCount = event.getRepeatCount();
            boolean down = event.getAction() == 0;
            boolean canceled = event.isCanceled();
            if (PhoneWindowManager.DEBUG_INPUT) {
                Log.d("WindowManager", String.format("handleHomeButton in display#%d mHomePressed = %b", Integer.valueOf(this.mDisplayId), Boolean.valueOf(this.mHomePressed)));
            }
            if (!down) {
                if (this.mDisplayId == 0) {
                    PhoneWindowManager.this.cancelPreloadRecentApps();
                }
                this.mHomePressed = false;
                if (this.mHomeConsumed) {
                    this.mHomeConsumed = false;
                    return -1;
                } else if (canceled) {
                    Log.i("WindowManager", "Ignoring HOME; event canceled.");
                    return -1;
                } else if (PhoneWindowManager.this.mDoubleTapOnHomeBehavior != 0 && (PhoneWindowManager.this.mDoubleTapOnHomeBehavior != 2 || PhoneWindowManager.this.mPictureInPictureVisible)) {
                    PhoneWindowManager.this.mHandler.removeCallbacks(this.mHomeDoubleTapTimeoutRunnable);
                    this.mHomeDoubleTapPending = true;
                    PhoneWindowManager.this.mHandler.postDelayed(this.mHomeDoubleTapTimeoutRunnable, ViewConfiguration.getDoubleTapTimeout());
                    return -1;
                } else {
                    PhoneWindowManager.this.mHandler.post(new Runnable() { // from class: com.android.server.policy.PhoneWindowManager$DisplayHomeButtonHandler$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            PhoneWindowManager.DisplayHomeButtonHandler.this.m5995x1953488f();
                        }
                    });
                    return -1;
                }
            }
            KeyInterceptionInfo info = PhoneWindowManager.this.mWindowManagerInternal.getKeyInterceptionInfoFromToken(focusedToken);
            if (info != null) {
                if (info.layoutParamsType == 2009 || (info.layoutParamsType == 2040 && PhoneWindowManager.this.isKeyguardShowing())) {
                    return 0;
                }
                for (int t : PhoneWindowManager.WINDOW_TYPES_WHERE_HOME_DOESNT_WORK) {
                    if (info.layoutParamsType == t) {
                        return -1;
                    }
                }
            }
            if (repeatCount == 0) {
                this.mHomePressed = true;
                if (this.mHomeDoubleTapPending) {
                    this.mHomeDoubleTapPending = false;
                    PhoneWindowManager.this.mHandler.removeCallbacks(this.mHomeDoubleTapTimeoutRunnable);
                    PhoneWindowManager.this.mHandler.post(new Runnable() { // from class: com.android.server.policy.PhoneWindowManager$DisplayHomeButtonHandler$$ExternalSyntheticLambda1
                        @Override // java.lang.Runnable
                        public final void run() {
                            PhoneWindowManager.DisplayHomeButtonHandler.this.handleDoubleTapOnHome();
                        }
                    });
                } else if (PhoneWindowManager.this.mDoubleTapOnHomeBehavior == 1 && this.mDisplayId == 0) {
                    PhoneWindowManager.this.preloadRecentApps();
                }
            } else if ((event.getFlags() & 128) != 0 && !keyguardOn) {
                PhoneWindowManager.this.mHandler.post(new Runnable() { // from class: com.android.server.policy.PhoneWindowManager$DisplayHomeButtonHandler$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        PhoneWindowManager.DisplayHomeButtonHandler.this.m5996xfc7efbd0(event);
                    }
                });
            }
            return -1;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$handleHomeButton$0$com-android-server-policy-PhoneWindowManager$DisplayHomeButtonHandler  reason: not valid java name */
        public /* synthetic */ void m5995x1953488f() {
            PhoneWindowManager.this.handleShortPressOnHome(this.mDisplayId);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$handleHomeButton$1$com-android-server-policy-PhoneWindowManager$DisplayHomeButtonHandler  reason: not valid java name */
        public /* synthetic */ void m5996xfc7efbd0(KeyEvent event) {
            handleLongPressOnHome(event.getDeviceId(), event.getEventTime());
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void handleDoubleTapOnHome() {
            if (this.mHomeConsumed) {
                return;
            }
            switch (PhoneWindowManager.this.mDoubleTapOnHomeBehavior) {
                case 1:
                    this.mHomeConsumed = true;
                    PhoneWindowManager.this.toggleRecentApps();
                    return;
                case 2:
                    this.mHomeConsumed = true;
                    PhoneWindowManager.this.showPictureInPictureMenuInternal();
                    return;
                default:
                    Log.w("WindowManager", "No action or undefined behavior for double tap home: " + PhoneWindowManager.this.mDoubleTapOnHomeBehavior);
                    return;
            }
        }

        private void handleLongPressOnHome(int deviceId, long eventTime) {
            if (this.mHomeConsumed || PhoneWindowManager.this.mLongPressOnHomeBehavior == 0) {
                return;
            }
            this.mHomeConsumed = true;
            PhoneWindowManager.this.performHapticFeedback(0, false, "Home - Long Press");
            switch (PhoneWindowManager.this.mLongPressOnHomeBehavior) {
                case 1:
                    PhoneWindowManager.this.launchAllAppsAction();
                    return;
                case 2:
                    PhoneWindowManager.this.launchAssistAction(null, deviceId, eventTime, 5);
                    return;
                case 3:
                    PhoneWindowManager.this.toggleNotificationPanel();
                    return;
                default:
                    Log.w("WindowManager", "Undefined long press on home behavior: " + PhoneWindowManager.this.mLongPressOnHomeBehavior);
                    return;
            }
        }

        public String toString() {
            return String.format("mDisplayId = %d, mHomePressed = %b", Integer.valueOf(this.mDisplayId), Boolean.valueOf(this.mHomePressed));
        }
    }

    private boolean isRoundWindow() {
        return this.mContext.getResources().getConfiguration().isScreenRound();
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void setDefaultDisplay(WindowManagerPolicy.DisplayContentInfo displayContentInfo) {
        this.mDefaultDisplay = displayContentInfo.getDisplay();
        DisplayRotation displayRotation = displayContentInfo.getDisplayRotation();
        this.mDefaultDisplayRotation = displayRotation;
        this.mDefaultDisplayPolicy = displayRotation.getDisplayPolicy();
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void init(Context context, IWindowManager windowManager, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs) {
        int minHorizontal;
        int maxHorizontal;
        int minVertical;
        int maxVertical;
        int maxRadius;
        this.mContext = context;
        this.mWindowManager = windowManager;
        this.mWindowManagerFuncs = windowManagerFuncs;
        this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mActivityTaskManagerInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        this.mInputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
        this.mDreamManagerInternal = (DreamManagerInternal) LocalServices.getService(DreamManagerInternal.class);
        this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        this.mDisplayManager = (DisplayManager) this.mContext.getSystemService(DisplayManager.class);
        this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        this.mDeviceStateManagerInternal = (DeviceStateManagerInternal) LocalServices.getService(DeviceStateManagerInternal.class);
        PackageManager packageManager = this.mContext.getPackageManager();
        this.mPackageManager = packageManager;
        this.mHasFeatureWatch = packageManager.hasSystemFeature("android.hardware.type.watch");
        this.mHasFeatureLeanback = this.mPackageManager.hasSystemFeature("android.software.leanback");
        this.mHasFeatureAuto = this.mPackageManager.hasSystemFeature("android.hardware.type.automotive");
        this.mHasFeatureHdmiCec = this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec");
        this.mAccessibilityShortcutController = new AccessibilityShortcutController(this.mContext, new Handler(), this.mCurrentUserId);
        this.mLogger = new MetricsLogger();
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mScreenOffSleepTokenAcquirer = this.mActivityTaskManagerInternal.createSleepTokenAcquirer("ScreenOff");
        Resources res = this.mContext.getResources();
        this.mWakeOnDpadKeyPress = res.getBoolean(17891832);
        this.mWakeOnAssistKeyPress = res.getBoolean(17891830);
        this.mWakeOnBackKeyPress = res.getBoolean(17891831);
        boolean burnInProtectionEnabled = context.getResources().getBoolean(17891628);
        boolean burnInProtectionDevMode = SystemProperties.getBoolean("persist.debug.force_burn_in", false);
        if (burnInProtectionEnabled || burnInProtectionDevMode) {
            if (burnInProtectionDevMode) {
                minHorizontal = -8;
                maxHorizontal = 8;
                minVertical = -8;
                maxVertical = -4;
                maxRadius = isRoundWindow() ? 6 : -1;
            } else {
                Resources resources = context.getResources();
                int minHorizontal2 = resources.getInteger(17694762);
                int maxHorizontal2 = resources.getInteger(17694759);
                int minVertical2 = resources.getInteger(17694763);
                int maxVertical2 = resources.getInteger(17694761);
                minHorizontal = minHorizontal2;
                maxHorizontal = maxHorizontal2;
                minVertical = minVertical2;
                maxVertical = maxVertical2;
                maxRadius = resources.getInteger(17694760);
            }
            this.mBurnInProtectionHelper = new BurnInProtectionHelper(context, minHorizontal, maxHorizontal, minVertical, maxVertical, maxRadius);
        }
        PolicyHandler policyHandler = new PolicyHandler();
        this.mHandler = policyHandler;
        this.mWakeGestureListener = new MyWakeGestureListener(this.mContext, policyHandler);
        SettingsObserver settingsObserver = new SettingsObserver(this.mHandler);
        this.mSettingsObserver = settingsObserver;
        settingsObserver.observe();
        this.mModifierShortcutManager = new ModifierShortcutManager(context);
        this.mUiMode = context.getResources().getInteger(17694801);
        Intent intent = new Intent("android.intent.action.MAIN", (Uri) null);
        this.mHomeIntent = intent;
        intent.addCategory("android.intent.category.HOME");
        this.mHomeIntent.addFlags(270532608);
        this.mEnableCarDockHomeCapture = context.getResources().getBoolean(17891629);
        Intent intent2 = new Intent("android.intent.action.MAIN", (Uri) null);
        this.mCarDockIntent = intent2;
        intent2.addCategory("android.intent.category.CAR_DOCK");
        this.mCarDockIntent.addFlags(270532608);
        Intent intent3 = new Intent("android.intent.action.MAIN", (Uri) null);
        this.mDeskDockIntent = intent3;
        intent3.addCategory("android.intent.category.DESK_DOCK");
        this.mDeskDockIntent.addFlags(270532608);
        Intent intent4 = new Intent("android.intent.action.MAIN", (Uri) null);
        this.mVrHeadsetHomeIntent = intent4;
        intent4.addCategory("android.intent.category.VR_HOME");
        this.mVrHeadsetHomeIntent.addFlags(270532608);
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mPowerManager = powerManager;
        this.mBroadcastWakeLock = powerManager.newWakeLock(1, "PhoneWindowManager.mBroadcastWakeLock");
        this.mPowerKeyWakeLock = this.mPowerManager.newWakeLock(1, "PhoneWindowManager.mPowerKeyWakeLock");
        this.mEnableShiftMenuBugReports = "1".equals(SystemProperties.get("ro.debuggable"));
        this.mLidKeyboardAccessibility = this.mContext.getResources().getInteger(17694851);
        this.mLidNavigationAccessibility = this.mContext.getResources().getInteger(17694852);
        boolean z = this.mContext.getResources().getBoolean(17891356);
        this.mAllowTheaterModeWakeFromKey = z;
        this.mAllowTheaterModeWakeFromPowerKey = z || this.mContext.getResources().getBoolean(17891360);
        this.mAllowTheaterModeWakeFromMotion = this.mContext.getResources().getBoolean(17891358);
        this.mAllowTheaterModeWakeFromMotionWhenNotDreaming = this.mContext.getResources().getBoolean(17891359);
        this.mAllowTheaterModeWakeFromCameraLens = this.mContext.getResources().getBoolean(17891353);
        this.mAllowTheaterModeWakeFromLidSwitch = this.mContext.getResources().getBoolean(17891357);
        this.mAllowTheaterModeWakeFromWakeGesture = this.mContext.getResources().getBoolean(17891355);
        this.mGoToSleepOnButtonPressTheaterMode = this.mContext.getResources().getBoolean(17891672);
        this.mSupportLongPressPowerWhenNonInteractive = this.mContext.getResources().getBoolean(17891771);
        this.mLongPressOnBackBehavior = this.mContext.getResources().getInteger(17694856);
        this.mShortPressOnPowerBehavior = this.mContext.getResources().getInteger(17694946);
        this.mLongPressOnPowerBehavior = this.mContext.getResources().getInteger(17694858);
        this.mLongPressOnPowerAssistantTimeoutMs = this.mContext.getResources().getInteger(17694859);
        this.mVeryLongPressOnPowerBehavior = this.mContext.getResources().getInteger(17694968);
        this.mDoublePressOnPowerBehavior = this.mContext.getResources().getInteger(17694817);
        this.mPowerDoublePressTargetActivity = ComponentName.unflattenFromString(this.mContext.getResources().getString(17039962));
        this.mTriplePressOnPowerBehavior = this.mContext.getResources().getInteger(17694961);
        this.mShortPressOnSleepBehavior = this.mContext.getResources().getInteger(17694947);
        this.mAllowStartActivityForLongPressOnPowerDuringSetup = this.mContext.getResources().getBoolean(17891352);
        this.mHapticTextHandleEnabled = this.mContext.getResources().getBoolean(17891636);
        this.mUseTvRouting = AudioSystem.getPlatformType(this.mContext) == 2;
        this.mHandleVolumeKeysInWM = this.mContext.getResources().getBoolean(17891676);
        this.mPerDisplayFocusEnabled = this.mContext.getResources().getBoolean(17891332);
        this.mWakeUpToLastStateTimeout = this.mContext.getResources().getInteger(17694974);
        readConfigurationDependentBehaviors();
        this.mDisplayFoldController = DisplayFoldController.create(context, 0);
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
        IntentFilter filter = new IntentFilter();
        filter.addAction(UiModeManager.ACTION_ENTER_CAR_MODE);
        filter.addAction(UiModeManager.ACTION_EXIT_CAR_MODE);
        filter.addAction(UiModeManager.ACTION_ENTER_DESK_MODE);
        filter.addAction(UiModeManager.ACTION_EXIT_DESK_MODE);
        filter.addAction("android.intent.action.DOCK_EVENT");
        Intent intent5 = context.registerReceiver(this.mDockReceiver, filter);
        if (intent5 != null) {
            this.mDefaultDisplayPolicy.setDockMode(intent5.getIntExtra("android.intent.extra.DOCK_STATE", 0));
        }
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("android.intent.action.DREAMING_STARTED");
        filter2.addAction("android.intent.action.DREAMING_STOPPED");
        context.registerReceiver(this.mDreamReceiver, filter2);
        context.registerReceiver(this.mMultiuserReceiver, new IntentFilter("android.intent.action.USER_SWITCHED"));
        this.mVibrator = (Vibrator) context.getSystemService("vibrator");
        this.mSafeModeEnabledVibePattern = getLongIntArray(this.mContext.getResources(), 17236121);
        this.mGlobalKeyManager = new GlobalKeyManager(this.mContext);
        initializeHdmiState();
        if (!this.mPowerManager.isInteractive()) {
            startedGoingToSleep(2);
            finishedGoingToSleep(2);
        }
        this.mWindowManagerInternal.registerAppTransitionListener(new WindowManagerInternal.AppTransitionListener() { // from class: com.android.server.policy.PhoneWindowManager.5
            @Override // com.android.server.wm.WindowManagerInternal.AppTransitionListener
            public int onAppTransitionStartingLocked(boolean keyguardGoingAway, boolean keyguardOccluding, long duration, long statusBarAnimationStartTime, long statusBarAnimationDuration) {
                if (keyguardGoingAway) {
                    duration = ITranPhoneWindowManager.Instance().onHandleStartTransitionForKeyguardLw(PhoneWindowManager.this.mDefaultDisplayPolicy, duration);
                }
                return PhoneWindowManager.this.handleStartTransitionForKeyguardLw(keyguardGoingAway && !WindowManagerService.sEnableRemoteKeyguardGoingAwayAnimation, keyguardOccluding, duration);
            }

            @Override // com.android.server.wm.WindowManagerInternal.AppTransitionListener
            public void onAppTransitionCancelledLocked(boolean keyguardGoingAway) {
                PhoneWindowManager.this.handleStartTransitionForKeyguardLw(keyguardGoingAway, false, 0L);
            }
        });
        this.mKeyguardDelegate = new KeyguardServiceDelegate(this.mContext, new KeyguardStateMonitor.StateCallback() { // from class: com.android.server.policy.PhoneWindowManager.6
            @Override // com.android.server.policy.keyguard.KeyguardStateMonitor.StateCallback
            public void onTrustedChanged() {
                PhoneWindowManager.this.mWindowManagerFuncs.notifyKeyguardTrustedChanged();
            }

            @Override // com.android.server.policy.keyguard.KeyguardStateMonitor.StateCallback
            public void onShowingChanged() {
                PhoneWindowManager.this.mWindowManagerFuncs.onKeyguardShowingAndNotOccludedChanged();
            }
        });
        initKeyCombinationRules();
        initSingleKeyGestureRules();
        this.mSideFpsEventHandler = new SideFpsEventHandler(this.mContext, this.mHandler, this.mPowerManager);
        ITranPhoneWindowManager.Instance().onInit(context);
        ITranInput.Instance().init(context);
    }

    private void initKeyCombinationRules() {
        this.mKeyCombinationManager = new KeyCombinationManager(this.mHandler);
        boolean screenshotChordEnabled = this.mContext.getResources().getBoolean(17891646);
        if (screenshotChordEnabled) {
            this.mKeyCombinationManager.addRule(new KeyCombinationManager.TwoKeysCombinationRule(25, 26) { // from class: com.android.server.policy.PhoneWindowManager.7
                /* JADX INFO: Access modifiers changed from: package-private */
                @Override // com.android.server.policy.KeyCombinationManager.TwoKeysCombinationRule
                public void execute() {
                    PhoneWindowManager.this.mPowerKeyHandled = true;
                    PhoneWindowManager phoneWindowManager = PhoneWindowManager.this;
                    phoneWindowManager.interceptScreenshotChord(1, 1, phoneWindowManager.getScreenshotChordLongPressDelay());
                }

                /* JADX INFO: Access modifiers changed from: package-private */
                @Override // com.android.server.policy.KeyCombinationManager.TwoKeysCombinationRule
                public void cancel() {
                    PhoneWindowManager.this.cancelPendingScreenshotChordAction();
                }
            });
        }
        this.mKeyCombinationManager.addRule(new KeyCombinationManager.TwoKeysCombinationRule(25, 24) { // from class: com.android.server.policy.PhoneWindowManager.8
            @Override // com.android.server.policy.KeyCombinationManager.TwoKeysCombinationRule
            boolean preCondition() {
                return PhoneWindowManager.this.mAccessibilityShortcutController.isAccessibilityShortcutAvailable(PhoneWindowManager.this.isKeyguardLocked());
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // com.android.server.policy.KeyCombinationManager.TwoKeysCombinationRule
            public void execute() {
                PhoneWindowManager.this.interceptAccessibilityShortcutChord();
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // com.android.server.policy.KeyCombinationManager.TwoKeysCombinationRule
            public void cancel() {
                PhoneWindowManager.this.cancelPendingAccessibilityShortcutAction();
            }
        });
        this.mKeyCombinationManager.addRule(new KeyCombinationManager.TwoKeysCombinationRule(24, 26) { // from class: com.android.server.policy.PhoneWindowManager.9
            @Override // com.android.server.policy.KeyCombinationManager.TwoKeysCombinationRule
            boolean preCondition() {
                switch (PhoneWindowManager.this.mPowerVolUpBehavior) {
                    case 1:
                        return PhoneWindowManager.this.mRingerToggleChord != 0;
                    default:
                        return true;
                }
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // com.android.server.policy.KeyCombinationManager.TwoKeysCombinationRule
            public void execute() {
                switch (PhoneWindowManager.this.mPowerVolUpBehavior) {
                    case 1:
                        PhoneWindowManager.this.interceptRingerToggleChord();
                        PhoneWindowManager.this.mPowerKeyHandled = true;
                        return;
                    case 2:
                        PhoneWindowManager.this.performHapticFeedback(FrameworkStatsLog.MOBILE_BYTES_TRANSFER_BY_FG_BG, false, "Power + Volume Up - Global Actions");
                        PhoneWindowManager.this.showGlobalActions();
                        PhoneWindowManager.this.mPowerKeyHandled = true;
                        return;
                    default:
                        return;
                }
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            @Override // com.android.server.policy.KeyCombinationManager.TwoKeysCombinationRule
            public void cancel() {
                switch (PhoneWindowManager.this.mPowerVolUpBehavior) {
                    case 1:
                        PhoneWindowManager.this.cancelPendingRingerToggleChordAction();
                        return;
                    case 2:
                        PhoneWindowManager.this.cancelGlobalActionsAction();
                        return;
                    default:
                        return;
                }
            }
        });
        if (this.mHasFeatureLeanback) {
            this.mKeyCombinationManager.addRule(new KeyCombinationManager.TwoKeysCombinationRule(4, 20) { // from class: com.android.server.policy.PhoneWindowManager.10
                /* JADX INFO: Access modifiers changed from: package-private */
                @Override // com.android.server.policy.KeyCombinationManager.TwoKeysCombinationRule
                public void execute() {
                    PhoneWindowManager.this.mBackKeyHandled = true;
                    PhoneWindowManager.this.interceptAccessibilityGestureTv();
                }

                /* JADX INFO: Access modifiers changed from: package-private */
                @Override // com.android.server.policy.KeyCombinationManager.TwoKeysCombinationRule
                public void cancel() {
                    PhoneWindowManager.this.cancelAccessibilityGestureTv();
                }

                @Override // com.android.server.policy.KeyCombinationManager.TwoKeysCombinationRule
                long getKeyInterceptDelayMs() {
                    return 0L;
                }
            });
            this.mKeyCombinationManager.addRule(new KeyCombinationManager.TwoKeysCombinationRule(23, 4) { // from class: com.android.server.policy.PhoneWindowManager.11
                /* JADX INFO: Access modifiers changed from: package-private */
                @Override // com.android.server.policy.KeyCombinationManager.TwoKeysCombinationRule
                public void execute() {
                    PhoneWindowManager.this.mBackKeyHandled = true;
                    PhoneWindowManager.this.interceptBugreportGestureTv();
                }

                /* JADX INFO: Access modifiers changed from: package-private */
                @Override // com.android.server.policy.KeyCombinationManager.TwoKeysCombinationRule
                public void cancel() {
                    PhoneWindowManager.this.cancelBugreportGestureTv();
                }

                @Override // com.android.server.policy.KeyCombinationManager.TwoKeysCombinationRule
                long getKeyInterceptDelayMs() {
                    return 0L;
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class PowerKeyRule extends SingleKeyGestureDetector.SingleKeyRule {
        PowerKeyRule(int gestures) {
            super(26, gestures);
        }

        @Override // com.android.server.policy.SingleKeyGestureDetector.SingleKeyRule
        int getMaxMultiPressCount() {
            return PhoneWindowManager.this.getMaxMultiPressPowerCount();
        }

        @Override // com.android.server.policy.SingleKeyGestureDetector.SingleKeyRule
        void onPress(long downTime) {
            PhoneWindowManager phoneWindowManager = PhoneWindowManager.this;
            phoneWindowManager.powerPress(downTime, 1, phoneWindowManager.mSingleKeyGestureDetector.beganFromNonInteractive());
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.android.server.policy.SingleKeyGestureDetector.SingleKeyRule
        public long getLongPressTimeoutMs() {
            if (PhoneWindowManager.this.getResolvedLongPressOnPowerBehavior() == 5) {
                return PhoneWindowManager.this.mLongPressOnPowerAssistantTimeoutMs;
            }
            return super.getLongPressTimeoutMs();
        }

        @Override // com.android.server.policy.SingleKeyGestureDetector.SingleKeyRule
        void onLongPress(long eventTime) {
            if (PhoneWindowManager.this.mSingleKeyGestureDetector.beganFromNonInteractive() && !PhoneWindowManager.this.mSupportLongPressPowerWhenNonInteractive) {
                Slog.v("WindowManager", "Not support long press power when device is not interactive.");
            } else {
                PhoneWindowManager.this.powerLongPress(eventTime);
            }
        }

        @Override // com.android.server.policy.SingleKeyGestureDetector.SingleKeyRule
        void onVeryLongPress(long eventTime) {
            PhoneWindowManager.this.mActivityManagerInternal.prepareForPossibleShutdown();
            PhoneWindowManager.this.powerVeryLongPress();
        }

        @Override // com.android.server.policy.SingleKeyGestureDetector.SingleKeyRule
        void onMultiPress(long downTime, int count) {
            PhoneWindowManager phoneWindowManager = PhoneWindowManager.this;
            phoneWindowManager.powerPress(downTime, count, phoneWindowManager.mSingleKeyGestureDetector.beganFromNonInteractive());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class BackKeyRule extends SingleKeyGestureDetector.SingleKeyRule {
        BackKeyRule(int gestures) {
            super(4, gestures);
        }

        @Override // com.android.server.policy.SingleKeyGestureDetector.SingleKeyRule
        int getMaxMultiPressCount() {
            return 1;
        }

        @Override // com.android.server.policy.SingleKeyGestureDetector.SingleKeyRule
        void onPress(long downTime) {
            PhoneWindowManager.this.mBackKeyHandled |= PhoneWindowManager.this.backKeyPress();
        }

        @Override // com.android.server.policy.SingleKeyGestureDetector.SingleKeyRule
        void onLongPress(long downTime) {
            PhoneWindowManager.this.backLongPress();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class StemPrimaryKeyRule extends SingleKeyGestureDetector.SingleKeyRule {
        StemPrimaryKeyRule(int gestures) {
            super(264, gestures);
        }

        @Override // com.android.server.policy.SingleKeyGestureDetector.SingleKeyRule
        int getMaxMultiPressCount() {
            return PhoneWindowManager.this.getMaxMultiPressStemPrimaryCount();
        }

        @Override // com.android.server.policy.SingleKeyGestureDetector.SingleKeyRule
        void onPress(long downTime) {
            PhoneWindowManager.this.stemPrimaryPress(1);
        }

        @Override // com.android.server.policy.SingleKeyGestureDetector.SingleKeyRule
        void onLongPress(long eventTime) {
            PhoneWindowManager.this.stemPrimaryLongPress();
        }

        @Override // com.android.server.policy.SingleKeyGestureDetector.SingleKeyRule
        void onMultiPress(long downTime, int count) {
            PhoneWindowManager.this.stemPrimaryPress(count);
        }
    }

    private void initSingleKeyGestureRules() {
        this.mSingleKeyGestureDetector = SingleKeyGestureDetector.get(this.mContext);
        int powerKeyGestures = 0;
        if (hasVeryLongPressOnPowerBehavior()) {
            powerKeyGestures = 0 | 4;
        }
        if (hasLongPressOnPowerBehavior()) {
            powerKeyGestures |= 2;
        }
        this.mSingleKeyGestureDetector.addRule(new PowerKeyRule(powerKeyGestures));
        if (hasLongPressOnBackBehavior()) {
            this.mSingleKeyGestureDetector.addRule(new BackKeyRule(2));
        }
        if (hasStemPrimaryBehavior()) {
            int stemPrimaryKeyGestures = 0;
            if (hasLongPressOnStemPrimaryBehavior()) {
                stemPrimaryKeyGestures = 0 | 2;
            }
            this.mSingleKeyGestureDetector.addRule(new StemPrimaryKeyRule(stemPrimaryKeyGestures));
        }
    }

    private void readConfigurationDependentBehaviors() {
        Resources res = this.mContext.getResources();
        int integer = res.getInteger(17694857);
        this.mLongPressOnHomeBehavior = integer;
        if (integer < 0 || integer > 3) {
            this.mLongPressOnHomeBehavior = 0;
        }
        int integer2 = res.getInteger(17694819);
        this.mDoubleTapOnHomeBehavior = integer2;
        if (integer2 < 0 || integer2 > 2) {
            this.mDoubleTapOnHomeBehavior = 0;
        }
        this.mShortPressOnWindowBehavior = 0;
        if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
            this.mShortPressOnWindowBehavior = 1;
        }
        this.mShortPressOnStemPrimaryBehavior = this.mContext.getResources().getInteger(17694948);
        this.mLongPressOnStemPrimaryBehavior = this.mContext.getResources().getInteger(17694860);
        this.mDoublePressOnStemPrimaryBehavior = this.mContext.getResources().getInteger(17694818);
        this.mTriplePressOnStemPrimaryBehavior = this.mContext.getResources().getInteger(17694962);
    }

    public void updateSettings() {
        ContentResolver resolver = this.mContext.getContentResolver();
        boolean updateRotation = false;
        synchronized (this.mLock) {
            this.mEndcallBehavior = Settings.System.getIntForUser(resolver, "end_button_behavior", 2, -2);
            this.mIncallPowerBehavior = Settings.Secure.getIntForUser(resolver, "incall_power_button_behavior", 1, -2);
            this.mIncallBackBehavior = Settings.Secure.getIntForUser(resolver, "incall_back_button_behavior", 0, -2);
            this.mSystemNavigationKeysEnabled = Settings.Secure.getIntForUser(resolver, "system_navigation_keys_enabled", 0, -2) == 1;
            this.mRingerToggleChord = Settings.Secure.getIntForUser(resolver, "volume_hush_gesture", 0, -2);
            this.mPowerButtonSuppressionDelayMillis = Settings.Global.getInt(resolver, "power_button_suppression_delay_after_gesture_wake", 800);
            if (!this.mContext.getResources().getBoolean(17891829)) {
                this.mRingerToggleChord = 0;
            }
            boolean wakeGestureEnabledSetting = Settings.Secure.getIntForUser(resolver, "wake_gesture_enabled", 0, -2) != 0;
            if (this.mWakeGestureEnabledSetting != wakeGestureEnabledSetting) {
                this.mWakeGestureEnabledSetting = wakeGestureEnabledSetting;
                updateWakeGestureListenerLp();
            }
            this.mLockScreenTimeout = Settings.System.getIntForUser(resolver, "screen_off_timeout", 0, -2);
            String imId = Settings.Secure.getStringForUser(resolver, "default_input_method", -2);
            boolean hasSoftInput = imId != null && imId.length() > 0;
            if (this.mHasSoftInput != hasSoftInput) {
                this.mHasSoftInput = hasSoftInput;
                updateRotation = true;
            }
            this.mLongPressOnPowerBehavior = Settings.Global.getInt(resolver, "power_button_long_press", this.mContext.getResources().getInteger(17694858));
            this.mLongPressOnPowerAssistantTimeoutMs = Settings.Global.getLong(this.mContext.getContentResolver(), "power_button_long_press_duration_ms", this.mContext.getResources().getInteger(17694859));
            this.mVeryLongPressOnPowerBehavior = Settings.Global.getInt(resolver, "power_button_very_long_press", this.mContext.getResources().getInteger(17694968));
            this.mPowerVolUpBehavior = Settings.Global.getInt(resolver, "key_chord_power_volume_up", this.mContext.getResources().getInteger(17694846));
            this.mWakeAodEnabledSetting = Settings.Secure.getIntForUser(resolver, AOD_TRIGGER_MODE_TOUCH, 1, -2) == 1;
        }
        if (updateRotation) {
            updateRotation(true);
        }
    }

    private void updateWakeGestureListenerLp() {
        if (shouldEnableWakeGestureLp()) {
            this.mWakeGestureListener.requestWakeUpTrigger();
        } else {
            this.mWakeGestureListener.cancelWakeUpTrigger();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean shouldEnableWakeGestureLp() {
        return this.mWakeGestureEnabledSetting && !this.mDefaultDisplayPolicy.isAwake() && !(getLidBehavior() == 1 && this.mDefaultDisplayPolicy.getLidState() == 0) && this.mWakeGestureListener.isSupported();
    }

    /* JADX WARN: Removed duplicated region for block: B:66:0x00be  */
    /* JADX WARN: Removed duplicated region for block: B:80:? A[RETURN, SYNTHETIC] */
    @Override // com.android.server.policy.WindowManagerPolicy
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public int checkAddPermission(int type, boolean isRoundedCornerOverlay, String packageName, int[] outAppOp) {
        ApplicationInfo appInfo;
        if (!isRoundedCornerOverlay || this.mContext.checkCallingOrSelfPermission("android.permission.INTERNAL_SYSTEM_WINDOW") == 0) {
            outAppOp[0] = -1;
            if ((type >= 1 && type <= 99) || ((type >= 1000 && type <= 1999) || (type >= 2000 && type <= 2999))) {
                if (type >= 2000 && type <= 2999) {
                    if (!WindowManager.LayoutParams.isSystemAlertWindowType(type)) {
                        switch (type) {
                            case 2005:
                                outAppOp[0] = 45;
                                return 0;
                            case 2011:
                            case 2013:
                            case 2024:
                            case 2030:
                            case 2031:
                            case 2032:
                            case 2035:
                            case 2037:
                                return 0;
                            default:
                                return this.mContext.checkCallingOrSelfPermission("android.permission.INTERNAL_SYSTEM_WINDOW") == 0 ? 0 : -8;
                        }
                    }
                    outAppOp[0] = 24;
                    int callingUid = Binder.getCallingUid();
                    if (UserHandle.getAppId(callingUid) == 1000) {
                        return 0;
                    }
                    try {
                        appInfo = this.mPackageManager.getApplicationInfoAsUser(packageName, 0, UserHandle.getUserId(callingUid));
                    } catch (PackageManager.NameNotFoundException e) {
                        appInfo = null;
                        if (appInfo != null) {
                        }
                        if (this.mContext.checkCallingOrSelfPermission("android.permission.INTERNAL_SYSTEM_WINDOW") != 0) {
                        }
                    }
                    if (appInfo != null || (type != 2038 && appInfo.targetSdkVersion >= 26)) {
                        return this.mContext.checkCallingOrSelfPermission("android.permission.INTERNAL_SYSTEM_WINDOW") != 0 ? 0 : -8;
                    } else if (this.mContext.checkCallingOrSelfPermission("android.permission.SYSTEM_APPLICATION_OVERLAY") == 0) {
                        return 0;
                    } else {
                        int mode = this.mAppOpsManager.noteOpNoThrow(outAppOp[0], callingUid, packageName, (String) null, "check-add");
                        switch (mode) {
                            case 0:
                            case 1:
                                return 0;
                            case 2:
                                return appInfo.targetSdkVersion < 23 ? 0 : -8;
                            default:
                                return this.mContext.checkCallingOrSelfPermission("android.permission.SYSTEM_ALERT_WINDOW") == 0 ? 0 : -8;
                        }
                    }
                }
                return 0;
            }
            return -10;
        }
        return -8;
    }

    void readLidState() {
        this.mDefaultDisplayPolicy.setLidState(this.mWindowManagerFuncs.getLidState());
    }

    private void readCameraLensCoverState() {
        this.mCameraLensCoverState = this.mWindowManagerFuncs.getCameraLensCoverState();
    }

    private boolean isHidden(int accessibilityMode) {
        int lidState = this.mDefaultDisplayPolicy.getLidState();
        switch (accessibilityMode) {
            case 1:
                return lidState == 0;
            case 2:
                return lidState == 1;
            default:
                return false;
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void adjustConfigurationLw(Configuration config, int keyboardPresence, int navigationPresence) {
        this.mHaveBuiltInKeyboard = (keyboardPresence & 1) != 0;
        readConfigurationDependentBehaviors();
        readLidState();
        if (config.keyboard == 1 || (keyboardPresence == 1 && isHidden(this.mLidKeyboardAccessibility))) {
            config.hardKeyboardHidden = 2;
            if (!this.mHasSoftInput) {
                config.keyboardHidden = 2;
            }
        }
        if (config.navigation == 1 || (navigationPresence == 1 && isHidden(this.mLidNavigationAccessibility))) {
            config.navigationHidden = 2;
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public boolean isKeyguardHostWindow(WindowManager.LayoutParams attrs) {
        return attrs.type == 2040;
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public Animation createHiddenByKeyguardExit(boolean onWallpaper, boolean goingToNotificationShade, boolean subtleAnimation) {
        return TransitionAnimation.createHiddenByKeyguardExit(this.mContext, this.mLogDecelerateInterpolator, onWallpaper, goingToNotificationShade, subtleAnimation);
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public Animation createKeyguardWallpaperExit(boolean goingToNotificationShade) {
        if (goingToNotificationShade || ITranPhoneWindowManager.Instance().canHideByFingerprint()) {
            return null;
        }
        return AnimationUtils.loadAnimation(this.mContext, 17432691);
    }

    private static void awakenDreams() {
        IDreamManager dreamManager = getDreamManager();
        if (dreamManager != null) {
            try {
                dreamManager.awaken();
            } catch (RemoteException e) {
            }
        }
    }

    static IDreamManager getDreamManager() {
        return IDreamManager.Stub.asInterface(ServiceManager.checkService("dreams"));
    }

    TelecomManager getTelecommService() {
        return (TelecomManager) this.mContext.getSystemService("telecom");
    }

    NotificationManager getNotificationService() {
        return (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
    }

    static IAudioService getAudioService() {
        IAudioService audioService = IAudioService.Stub.asInterface(ServiceManager.checkService("audio"));
        if (audioService == null) {
            Log.w("WindowManager", "Unable to find IAudioService interface.");
        }
        return audioService;
    }

    boolean keyguardOn() {
        return isKeyguardShowingAndNotOccluded() || inKeyguardRestrictedKeyInputMode();
    }

    /* JADX WARN: Removed duplicated region for block: B:133:0x02ce  */
    /* JADX WARN: Removed duplicated region for block: B:134:0x02d0  */
    /* JADX WARN: Removed duplicated region for block: B:215:0x03f2  */
    @Override // com.android.server.policy.WindowManagerPolicy
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public long interceptKeyBeforeDispatching(IBinder focusedToken, KeyEvent event, int policyFlags) {
        InputDevice d;
        IStatusBarService service;
        boolean down;
        boolean keyguardOn = keyguardOn();
        int keyCode = event.getKeyCode();
        int repeatCount = event.getRepeatCount();
        int metaState = event.getMetaState();
        int flags = event.getFlags();
        boolean down2 = event.getAction() == 0;
        boolean canceled = event.isCanceled();
        int displayId = event.getDisplayId();
        if (DEBUG_INPUT) {
            Log.d("WindowManager", "interceptKeyTi keyCode=" + keyCode + " down=" + down2 + " repeatCount=" + repeatCount + " keyguardOn=" + keyguardOn + " canceled=" + canceled);
        }
        Pair<Boolean, Long> retLice = ITranPhoneWindowManager.Instance().onInterceptKeyBeforeDispatchingInner(event, policyFlags);
        if (((Boolean) retLice.first).booleanValue()) {
            return ((Long) retLice.second).longValue();
        }
        if (this.mKeyCombinationManager.isKeyConsumed(event)) {
            return -1L;
        }
        if ((flags & 1024) == 0) {
            long now = SystemClock.uptimeMillis();
            long interceptTimeout = this.mKeyCombinationManager.getKeyInterceptTimeout(keyCode);
            if (now < interceptTimeout) {
                return interceptTimeout - now;
            }
        }
        if (this.mPendingMetaAction && !KeyEvent.isMetaKey(keyCode)) {
            this.mPendingMetaAction = false;
        }
        if (this.mPendingCapsLockToggle && !KeyEvent.isMetaKey(keyCode) && !KeyEvent.isAltKey(keyCode)) {
            this.mPendingCapsLockToggle = false;
        }
        if (!isUserSetupComplete() || keyguardOn || !this.mModifierShortcutManager.interceptKey(event)) {
            switch (keyCode) {
                case 3:
                    DisplayHomeButtonHandler handler = this.mDisplayHomeButtonHandlers.get(displayId);
                    if (handler == null) {
                        handler = new DisplayHomeButtonHandler(displayId);
                        this.mDisplayHomeButtonHandlers.put(displayId, handler);
                    }
                    return handler.handleHomeButton(focusedToken, event);
                case 24:
                case 25:
                case 164:
                    if (this.mUseTvRouting || this.mHandleVolumeKeysInWM) {
                        dispatchDirectAudioEvent(event);
                        return -1L;
                    }
                    if (this.mDefaultDisplayPolicy.isPersistentVrModeEnabled() && (d = event.getDevice()) != null && !d.isExternal()) {
                        return -1L;
                    }
                    return ((isValidGlobalKey(keyCode) || !this.mGlobalKeyManager.handleGlobalKey(this.mContext, keyCode, event)) && (65536 & metaState) == 0) ? 0L : -1L;
                case 42:
                    if (down2 && event.isMetaPressed() && (service = getStatusBarService()) != null) {
                        try {
                            service.expandNotificationsPanel();
                        } catch (RemoteException e) {
                        }
                        return -1L;
                    }
                    if (isValidGlobalKey(keyCode)) {
                        break;
                    }
                case 47:
                    if (down2 && event.isMetaPressed() && event.isCtrlPressed() && repeatCount == 0) {
                        int type = event.isShiftPressed() ? 2 : 1;
                        interceptScreenshotChord(type, 2, 0L);
                        return -1L;
                    }
                    if (isValidGlobalKey(keyCode)) {
                    }
                case 57:
                case 58:
                    if (!down2) {
                        int i = this.mRecentAppsHeldModifiers;
                        if (i != 0 && (i & metaState) == 0) {
                            this.mRecentAppsHeldModifiers = 0;
                            hideRecentApps(true, false);
                            return -1L;
                        } else if (this.mPendingCapsLockToggle) {
                            this.mInputManagerInternal.toggleCapsLock(event.getDeviceId());
                            this.mPendingCapsLockToggle = false;
                            return -1L;
                        }
                    } else if (event.isMetaPressed()) {
                        this.mPendingCapsLockToggle = true;
                        this.mPendingMetaAction = false;
                    } else {
                        this.mPendingCapsLockToggle = false;
                    }
                    if (isValidGlobalKey(keyCode)) {
                    }
                case 61:
                    if (event.isMetaPressed()) {
                        return 0L;
                    }
                    if (down2 && repeatCount == 0 && this.mRecentAppsHeldModifiers == 0 && !keyguardOn && isUserSetupComplete()) {
                        int shiftlessModifiers = event.getModifiers() & (-194);
                        if (KeyEvent.metaStateHasModifiers(shiftlessModifiers, 2)) {
                            this.mRecentAppsHeldModifiers = shiftlessModifiers;
                            showRecentApps(true);
                            return -1L;
                        }
                    }
                    if (isValidGlobalKey(keyCode)) {
                    }
                case 62:
                    down = down2;
                    if ((458752 & metaState) == 0) {
                        return 0L;
                    }
                    if (down && repeatCount == 0) {
                        int direction = (metaState & 193) == 0 ? -1 : 1;
                        this.mWindowManagerFuncs.switchKeyboardLayout(event.getDeviceId(), direction);
                        return -1L;
                    }
                    if (isValidGlobalKey(keyCode)) {
                    }
                case 76:
                    if (down2 && repeatCount == 0 && event.isMetaPressed() && !keyguardOn) {
                        toggleKeyboardShortcutsMenu(event.getDeviceId());
                        return -1L;
                    }
                    if (isValidGlobalKey(keyCode)) {
                    }
                case 82:
                    if (down2 && repeatCount == 0 && this.mEnableShiftMenuBugReports && (metaState & 1) == 1) {
                        Intent intent = new Intent("android.intent.action.BUG_REPORT");
                        this.mContext.sendOrderedBroadcastAsUser(intent, UserHandle.CURRENT, null, null, null, 0, null, null);
                        return -1L;
                    }
                    if (isValidGlobalKey(keyCode)) {
                    }
                case 83:
                    if (!down2) {
                        toggleNotificationPanel();
                    }
                    return -1L;
                case 117:
                case 118:
                    if (down2) {
                        if (event.isAltPressed()) {
                            this.mPendingCapsLockToggle = true;
                            this.mPendingMetaAction = false;
                        } else {
                            this.mPendingCapsLockToggle = false;
                            this.mPendingMetaAction = true;
                        }
                    } else if (this.mPendingCapsLockToggle) {
                        this.mInputManagerInternal.toggleCapsLock(event.getDeviceId());
                        this.mPendingCapsLockToggle = false;
                    } else if (this.mPendingMetaAction) {
                        if (!canceled) {
                            launchAssistAction("android.intent.extra.ASSIST_INPUT_HINT_KEYBOARD", event.getDeviceId(), event.getEventTime(), 0);
                        }
                        this.mPendingMetaAction = false;
                    }
                    return -1L;
                case FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__CREDENTIAL_MANAGEMENT_APP_REMOVED /* 187 */:
                    if (!keyguardOn) {
                        if (isInMidtest()) {
                            sendToMenuKey();
                        } else if (down2 && repeatCount == 0) {
                            preloadRecentApps();
                        } else if (!down2) {
                            toggleRecentApps();
                        }
                    }
                    return -1L;
                case 204:
                    down = down2;
                    if (down) {
                        int direction2 = (metaState & 193) == 0 ? -1 : 1;
                        this.mWindowManagerFuncs.switchKeyboardLayout(event.getDeviceId(), direction2);
                        return -1L;
                    }
                    if (isValidGlobalKey(keyCode)) {
                    }
                case 219:
                    Slog.wtf("WindowManager", "KEYCODE_ASSIST should be handled in interceptKeyBeforeQueueing");
                    return -1L;
                case UsbDescriptor.CLASSID_DIAGNOSTIC /* 220 */:
                case 221:
                    if (down2) {
                        int direction3 = keyCode == 221 ? 1 : -1;
                        int auto = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, -3);
                        if (auto != 0) {
                            Settings.System.putIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, -3);
                        }
                        float min = this.mPowerManager.getBrightnessConstraint(0);
                        float max = this.mPowerManager.getBrightnessConstraint(1);
                        float step = ((max - min) / 10.0f) * direction3;
                        int screenDisplayId = displayId < 0 ? 0 : displayId;
                        float brightness = this.mDisplayManager.getBrightness(screenDisplayId);
                        this.mDisplayManager.setBrightness(screenDisplayId, Math.max(min, Math.min(max, brightness + step)));
                        startActivityAsUser(new Intent("com.android.intent.action.SHOW_BRIGHTNESS_DIALOG"), UserHandle.CURRENT_OR_SELF);
                    }
                    return -1L;
                case 231:
                    Slog.wtf("WindowManager", "KEYCODE_VOICE_ASSIST should be handled in interceptKeyBeforeQueueing");
                    return -1L;
                case 284:
                    if (!down2) {
                        this.mHandler.removeMessages(22);
                        Message msg = this.mHandler.obtainMessage(22);
                        msg.setAsynchronous(true);
                        msg.sendToTarget();
                    }
                    return -1L;
                case 289:
                case 290:
                case 291:
                case 292:
                case 293:
                case 294:
                case 295:
                case 296:
                case 297:
                case FrameworkStatsLog.BLOB_COMMITTED /* 298 */:
                case FrameworkStatsLog.BLOB_LEASED /* 299 */:
                case 300:
                case FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_ALARM_MANAGER_ALARM_CLOCK /* 301 */:
                case FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_ALARM_MANAGER_WHILE_IDLE /* 302 */:
                case FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_SERVICE_LAUNCH /* 303 */:
                case FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_KEY_CHAIN /* 304 */:
                    Slog.wtf("WindowManager", "KEYCODE_APP_X should be handled in interceptKeyBeforeQueueing");
                    return -1L;
                case FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_DOMAIN_VERIFICATION_V1 /* 307 */:
                case 308:
                    KeyInterceptionInfo info = this.mWindowManagerInternal.getKeyInterceptionInfoFromToken(focusedToken);
                    String title = info == null ? "<unknown>" : info.windowTitle;
                    if (title != null && title.contains("com.google.android.youtube")) {
                        return -1L;
                    }
                    if (isValidGlobalKey(keyCode)) {
                    }
                default:
                    if (isValidGlobalKey(keyCode)) {
                    }
            }
        }
        dismissKeyboardShortcutsMenu();
        this.mPendingMetaAction = false;
        this.mPendingCapsLockToggle = false;
        return -1L;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void interceptBugreportGestureTv() {
        this.mHandler.removeMessages(18);
        Message msg = Message.obtain(this.mHandler, 18);
        msg.setAsynchronous(true);
        this.mHandler.sendMessageDelayed(msg, 1000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelBugreportGestureTv() {
        this.mHandler.removeMessages(18);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void interceptAccessibilityGestureTv() {
        this.mHandler.removeMessages(19);
        Message msg = Message.obtain(this.mHandler, 19);
        msg.setAsynchronous(true);
        this.mHandler.sendMessageDelayed(msg, getAccessibilityShortcutTimeout());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelAccessibilityGestureTv() {
        this.mHandler.removeMessages(19);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void requestBugreportForTv() {
        if ("1".equals(SystemProperties.get("ro.debuggable")) || Settings.Global.getInt(this.mContext.getContentResolver(), "development_settings_enabled", 0) == 1) {
            try {
                if (!ActivityManager.getService().launchBugReportHandlerApp()) {
                    ActivityManager.getService().requestInteractiveBugReport();
                }
            } catch (RemoteException e) {
                Slog.e("WindowManager", "Error taking bugreport", e);
            }
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public KeyEvent dispatchUnhandledKey(IBinder focusedToken, KeyEvent event, int policyFlags) {
        KeyCharacterMap.FallbackAction fallbackAction;
        if (DEBUG_INPUT) {
            KeyInterceptionInfo info = this.mWindowManagerInternal.getKeyInterceptionInfoFromToken(focusedToken);
            String title = info == null ? "<unknown>" : info.windowTitle;
            Slog.d("WindowManager", "Unhandled key: inputToken=" + focusedToken + ", title=" + title + ", action=" + event.getAction() + ", flags=" + event.getFlags() + ", keyCode=" + event.getKeyCode() + ", scanCode=" + event.getScanCode() + ", metaState=" + event.getMetaState() + ", repeatCount=" + event.getRepeatCount() + ", policyFlags=" + policyFlags);
        }
        if (interceptUnhandledKey(event)) {
            return null;
        }
        KeyEvent fallbackEvent = null;
        if ((event.getFlags() & 1024) == 0) {
            KeyCharacterMap kcm = event.getKeyCharacterMap();
            int keyCode = event.getKeyCode();
            int metaState = event.getMetaState();
            boolean initialDown = event.getAction() == 0 && event.getRepeatCount() == 0;
            if (initialDown) {
                fallbackAction = kcm.getFallbackAction(keyCode, metaState);
            } else {
                fallbackAction = this.mFallbackActions.get(keyCode);
            }
            if (fallbackAction != null) {
                if (DEBUG_INPUT) {
                    Slog.d("WindowManager", "Fallback: keyCode=" + fallbackAction.keyCode + " metaState=" + Integer.toHexString(fallbackAction.metaState));
                }
                int flags = event.getFlags() | 1024;
                KeyEvent fallbackEvent2 = KeyEvent.obtain(event.getDownTime(), event.getEventTime(), event.getAction(), fallbackAction.keyCode, event.getRepeatCount(), fallbackAction.metaState, event.getDeviceId(), event.getScanCode(), flags, event.getSource(), event.getDisplayId(), null);
                if (interceptFallback(focusedToken, fallbackEvent2, policyFlags)) {
                    fallbackEvent = fallbackEvent2;
                } else {
                    fallbackEvent2.recycle();
                    fallbackEvent = null;
                }
                if (initialDown) {
                    this.mFallbackActions.put(keyCode, fallbackAction);
                } else if (event.getAction() == 1) {
                    this.mFallbackActions.remove(keyCode);
                    fallbackAction.recycle();
                }
            }
        }
        if (DEBUG_INPUT) {
            if (fallbackEvent == null) {
                Slog.d("WindowManager", "No fallback.");
            } else {
                Slog.d("WindowManager", "Performing fallback: " + fallbackEvent);
            }
        }
        return fallbackEvent;
    }

    private boolean interceptUnhandledKey(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int repeatCount = event.getRepeatCount();
        boolean down = event.getAction() == 0;
        int metaState = event.getModifiers();
        switch (keyCode) {
            case 54:
                if (down && KeyEvent.metaStateHasModifiers(metaState, UsbACInterface.FORMAT_II_AC3) && this.mAccessibilityShortcutController.isAccessibilityShortcutAvailable(isKeyguardLocked())) {
                    Handler handler = this.mHandler;
                    handler.sendMessage(handler.obtainMessage(17));
                    return true;
                }
                break;
            case 62:
                if (down && repeatCount == 0 && KeyEvent.metaStateHasModifiers(metaState, 4096)) {
                    int direction = (metaState & 193) != 0 ? -1 : 1;
                    this.mWindowManagerFuncs.switchKeyboardLayout(event.getDeviceId(), direction);
                    return true;
                }
                break;
            case 120:
                if (down && repeatCount == 0) {
                    interceptScreenshotChord(1, 2, 0L);
                }
                return true;
        }
        return false;
    }

    private boolean interceptFallback(IBinder focusedToken, KeyEvent fallbackEvent, int policyFlags) {
        int actions = interceptKeyBeforeQueueing(fallbackEvent, policyFlags);
        if ((actions & 1) != 0) {
            long delayMillis = interceptKeyBeforeDispatching(focusedToken, fallbackEvent, policyFlags);
            if (delayMillis == 0 && !interceptUnhandledKey(fallbackEvent)) {
                return true;
            }
            return false;
        }
        return false;
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void setTopFocusedDisplay(int displayId) {
        this.mTopFocusedDisplayId = displayId;
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void registerDisplayFoldListener(IDisplayFoldListener listener) {
        DisplayFoldController displayFoldController = this.mDisplayFoldController;
        if (displayFoldController != null) {
            displayFoldController.registerDisplayFoldListener(listener);
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void unregisterDisplayFoldListener(IDisplayFoldListener listener) {
        DisplayFoldController displayFoldController = this.mDisplayFoldController;
        if (displayFoldController != null) {
            displayFoldController.unregisterDisplayFoldListener(listener);
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void setOverrideFoldedArea(Rect area) {
        DisplayFoldController displayFoldController = this.mDisplayFoldController;
        if (displayFoldController != null) {
            displayFoldController.setOverrideFoldedArea(area);
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public Rect getFoldedArea() {
        DisplayFoldController displayFoldController = this.mDisplayFoldController;
        if (displayFoldController != null) {
            return displayFoldController.getFoldedArea();
        }
        return new Rect();
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void onDefaultDisplayFocusChangedLw(WindowManagerPolicy.WindowState newFocus) {
        DisplayFoldController displayFoldController = this.mDisplayFoldController;
        if (displayFoldController != null) {
            displayFoldController.onDefaultDisplayFocusChanged(newFocus != null ? newFocus.getOwningPackage() : null);
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void registerShortcutKey(long shortcutCode, IShortcutService shortcutService) throws RemoteException {
        synchronized (this.mLock) {
            this.mModifierShortcutManager.registerShortcutKey(shortcutCode, shortcutService);
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void onKeyguardOccludedChangedLw(boolean occluded) {
        if (Build.IS_DEBUG_ENABLE) {
            Slog.d("WindowManager", "mKeyguardDelegate.isShowing() = " + this.mKeyguardDelegate.isShowing() + " occluded = " + occluded);
        }
        KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
        if (keyguardServiceDelegate != null && keyguardServiceDelegate.isShowing() && !WindowManagerService.sEnableShellTransitions) {
            this.mPendingKeyguardOccluded = occluded;
            this.mKeyguardOccludedChanged = true;
            return;
        }
        setKeyguardOccludedLw(occluded, false, false);
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public int applyKeyguardOcclusionChange(boolean transitionStarted) {
        if (this.mKeyguardOccludedChanged) {
            if (DEBUG_KEYGUARD) {
                Slog.d("WindowManager", "transition/occluded changed occluded=" + this.mPendingKeyguardOccluded);
            }
            if (Build.IS_DEBUG_ENABLE) {
                Slog.d("WindowManager", "mPendingKeyguardOccluded = " + this.mPendingKeyguardOccluded);
            }
            if (setKeyguardOccludedLw(this.mPendingKeyguardOccluded, false, transitionStarted)) {
                return 5;
            }
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int handleStartTransitionForKeyguardLw(boolean keyguardGoingAway, boolean keyguardOccluding, long duration) {
        int redoLayout = applyKeyguardOcclusionChange(keyguardOccluding);
        if (redoLayout != 0) {
            return redoLayout;
        }
        if (keyguardGoingAway) {
            if (DEBUG_KEYGUARD) {
                Slog.d("WindowManager", "Starting keyguard exit animation");
            }
            startKeyguardExitAnimation(SystemClock.uptimeMillis(), duration);
            return 0;
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void launchAssistAction(String hint, int deviceId, long eventTime, int invocationType) {
        sendCloseSystemWindows(SYSTEM_DIALOG_REASON_ASSIST);
        if (!isUserSetupComplete()) {
            return;
        }
        Bundle args = new Bundle();
        if (deviceId > Integer.MIN_VALUE) {
            args.putInt("android.intent.extra.ASSIST_INPUT_DEVICE_ID", deviceId);
        }
        if (hint != null) {
            args.putBoolean(hint, true);
        }
        args.putLong("android.intent.extra.TIME", eventTime);
        args.putInt("invocation_type", invocationType);
        ((SearchManager) this.mContext.createContextAsUser(UserHandle.of(this.mCurrentUserId), 0).getSystemService("search")).launchAssist(args);
    }

    private void launchVoiceAssist(boolean allowDuringSetup) {
        KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
        boolean keyguardActive = keyguardServiceDelegate != null && keyguardServiceDelegate.isShowing();
        if (!keyguardActive) {
            if (this.mHasFeatureWatch && isInRetailMode()) {
                launchRetailVoiceAssist(allowDuringSetup);
                return;
            } else {
                startVoiceAssistIntent(allowDuringSetup);
                return;
            }
        }
        this.mKeyguardDelegate.dismissKeyguardToLaunch(new Intent("android.intent.action.VOICE_ASSIST"));
    }

    private void launchRetailVoiceAssist(boolean allowDuringSetup) {
        Intent retailIntent = new Intent(ACTION_VOICE_ASSIST_RETAIL);
        ResolveInfo resolveInfo = this.mContext.getPackageManager().resolveActivity(retailIntent, 0);
        if (resolveInfo != null) {
            retailIntent.setComponent(new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
            startActivityAsUser(retailIntent, null, UserHandle.CURRENT_OR_SELF, allowDuringSetup);
            return;
        }
        Slog.w("WindowManager", "Couldn't find an app to process android.intent.action.VOICE_ASSIST_RETAIL. Fall back to start android.intent.action.VOICE_ASSIST");
        startVoiceAssistIntent(allowDuringSetup);
    }

    private void startVoiceAssistIntent(boolean allowDuringSetup) {
        Intent intent = new Intent("android.intent.action.VOICE_ASSIST");
        startActivityAsUser(intent, null, UserHandle.CURRENT_OR_SELF, allowDuringSetup);
    }

    private boolean isInRetailMode() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "device_demo_mode", 0) == 1;
    }

    private void startActivityAsUser(Intent intent, UserHandle handle) {
        startActivityAsUser(intent, null, handle);
    }

    private void startActivityAsUser(Intent intent, Bundle bundle, UserHandle handle) {
        startActivityAsUser(intent, bundle, handle, false);
    }

    private void startActivityAsUser(Intent intent, Bundle bundle, UserHandle handle, boolean allowDuringSetup) {
        if (allowDuringSetup || isUserSetupComplete()) {
            this.mContext.startActivityAsUser(intent, bundle, handle);
        } else {
            Slog.i("WindowManager", "Not starting activity because user setup is in progress: " + intent);
        }
    }

    private SearchManager getSearchManager() {
        if (this.mSearchManager == null) {
            this.mSearchManager = (SearchManager) this.mContext.getSystemService("search");
        }
        return this.mSearchManager;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void preloadRecentApps() {
        this.mPreloadedRecentApps = true;
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar != null) {
            statusbar.preloadRecentApps();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelPreloadRecentApps() {
        if (this.mPreloadedRecentApps) {
            this.mPreloadedRecentApps = false;
            StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
            if (statusbar != null) {
                statusbar.cancelPreloadRecentApps();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void toggleRecentApps() {
        this.mPreloadedRecentApps = false;
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar != null) {
            statusbar.toggleRecentApps();
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void showRecentApps() {
        this.mHandler.removeMessages(9);
        this.mHandler.obtainMessage(9).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showRecentApps(boolean triggeredFromAltTab) {
        this.mPreloadedRecentApps = false;
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar != null) {
            statusbar.showRecentApps(triggeredFromAltTab);
        }
    }

    private void toggleKeyboardShortcutsMenu(int deviceId) {
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar != null) {
            statusbar.toggleKeyboardShortcutsMenu(deviceId);
        }
    }

    private void dismissKeyboardShortcutsMenu() {
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar != null) {
            statusbar.dismissKeyboardShortcutsMenu();
        }
    }

    private void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHome) {
        this.mPreloadedRecentApps = false;
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar != null) {
            statusbar.hideRecentApps(triggeredFromAltTab, triggeredFromHome);
        }
    }

    void launchHomeFromHotKey(int displayId) {
        launchHomeFromHotKey(displayId, true, true);
    }

    void launchHomeFromHotKey(final int displayId, final boolean awakenFromDreams, boolean respectKeyguard) {
        if (respectKeyguard) {
            if (isKeyguardShowingAndNotOccluded()) {
                return;
            }
            if (!isKeyguardOccluded() && this.mKeyguardDelegate.isInputRestricted()) {
                this.mKeyguardDelegate.verifyUnlock(new WindowManagerPolicy.OnKeyguardExitResult() { // from class: com.android.server.policy.PhoneWindowManager.12
                    @Override // com.android.server.policy.WindowManagerPolicy.OnKeyguardExitResult
                    public void onKeyguardExitResult(boolean success) {
                        if (success) {
                            PhoneWindowManager.this.startDockOrHome(displayId, true, awakenFromDreams);
                        }
                    }
                });
                return;
            }
        }
        if (this.mRecentsVisible) {
            try {
                ActivityManager.getService().stopAppSwitches();
            } catch (RemoteException e) {
            }
            if (awakenFromDreams) {
                awakenDreams();
            }
            hideRecentApps(false, true);
            return;
        }
        startDockOrHome(displayId, true, awakenFromDreams);
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void setRecentsVisibilityLw(boolean visible) {
        this.mRecentsVisible = visible;
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void setPipVisibilityLw(boolean visible) {
        this.mPictureInPictureVisible = visible;
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void setNavBarVirtualKeyHapticFeedbackEnabledLw(boolean enabled) {
        this.mNavBarVirtualKeyHapticFeedbackEnabled = enabled;
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void applyAodPolicyLw(WindowManagerPolicy.WindowState win) {
        if (win != null) {
            if (this.mNeedHideAodWin || this.mDefaultDisplayPolicy.mAodHideByFingerprint) {
                win.hideLw(false);
                if (Build.TRANCARE_SUPPORT && OPTICAL_FINGERPRINT && !this.mHadHideAod && this.mDefaultDisplayPolicy.mAodHideByFingerprint) {
                    this.mHadHideAod = true;
                    ITranFingerprintService.Instance().updateUnlockTime(0L);
                    return;
                }
                return;
            }
            win.showLw(false);
            this.mHadHideAod = false;
        }
    }

    private boolean setKeyguardOccludedLw(boolean isOccluded, boolean force, boolean transitionStarted) {
        boolean animate;
        if (DEBUG_KEYGUARD) {
            Slog.d("WindowManager", "setKeyguardOccluded occluded=" + isOccluded);
        }
        boolean notify = false;
        this.mKeyguardOccludedChanged = false;
        if (isKeyguardOccluded() == isOccluded && !force) {
            return false;
        }
        boolean showing = this.mKeyguardDelegate.isShowing();
        if (!showing || isOccluded) {
            animate = false;
        } else {
            animate = true;
        }
        if (!WindowManagerService.sEnableRemoteKeyguardOccludeAnimation || !transitionStarted) {
            notify = true;
        }
        if (Build.IS_DEBUG_ENABLE) {
            Slog.d("WindowManager", "isOccluded = " + isOccluded + ", animate = " + animate + ", notify = " + notify);
        }
        this.mKeyguardDelegate.setOccluded(isOccluded, animate, notify);
        return showing;
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void notifyLidSwitchChanged(long whenNanos, boolean lidOpen) {
        if (lidOpen != this.mDefaultDisplayPolicy.getLidState()) {
            this.mDefaultDisplayPolicy.setLidState(lidOpen ? 1 : 0);
            applyLidSwitchState();
            updateRotation(true);
            if (!lidOpen) {
                if (getLidBehavior() != 1) {
                    this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
                    return;
                }
                return;
            }
            wakeUp(SystemClock.uptimeMillis(), this.mAllowTheaterModeWakeFromLidSwitch, 9, "android.policy:LID");
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void notifyCameraLensCoverSwitchChanged(long whenNanos, boolean lensCovered) {
        Intent intent;
        if (this.mCameraLensCoverState == lensCovered || !this.mContext.getResources().getBoolean(17891690)) {
            return;
        }
        if (this.mCameraLensCoverState == 1 && !lensCovered) {
            KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
            boolean keyguardActive = keyguardServiceDelegate == null ? false : keyguardServiceDelegate.isShowing();
            if (keyguardActive) {
                intent = new Intent("android.media.action.STILL_IMAGE_CAMERA_SECURE");
            } else {
                intent = new Intent("android.media.action.STILL_IMAGE_CAMERA");
            }
            wakeUp(whenNanos / 1000000, this.mAllowTheaterModeWakeFromCameraLens, 5, "android.policy:CAMERA_COVER");
            startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
        }
        this.mCameraLensCoverState = lensCovered ? 1 : 0;
    }

    void initializeHdmiState() {
        int oldMask = StrictMode.allowThreadDiskReadsMask();
        try {
            initializeHdmiStateInternal();
        } finally {
            StrictMode.setThreadPolicyMask(oldMask);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3946=4] */
    void initializeHdmiStateInternal() {
        boolean plugged = false;
        if (new File("/sys/devices/virtual/switch/hdmi/state").exists()) {
            this.mHDMIObserver.startObserving("DEVPATH=/devices/virtual/switch/hdmi");
            FileReader reader = null;
            try {
                try {
                    try {
                        reader = new FileReader("/sys/class/switch/hdmi/state");
                        char[] buf = new char[15];
                        int n = reader.read(buf);
                        if (n > 1) {
                            plugged = Integer.parseInt(new String(buf, 0, n + (-1))) != 0;
                        }
                        reader.close();
                    } catch (Throwable th) {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e) {
                            }
                        }
                        throw th;
                    }
                } catch (IOException ex) {
                    Slog.w("WindowManager", "Couldn't read hdmi state from /sys/class/switch/hdmi/state: " + ex);
                    if (reader != null) {
                        reader.close();
                    }
                } catch (NumberFormatException ex2) {
                    Slog.w("WindowManager", "Couldn't read hdmi state from /sys/class/switch/hdmi/state: " + ex2);
                    if (reader != null) {
                        reader.close();
                    }
                }
            } catch (IOException e2) {
            }
        } else {
            List<ExtconUEventObserver.ExtconInfo> extcons = ExtconUEventObserver.ExtconInfo.getExtconInfoForTypes(new String[]{ExtconUEventObserver.ExtconInfo.EXTCON_HDMI});
            if (!extcons.isEmpty()) {
                HdmiVideoExtconUEventObserver observer = new HdmiVideoExtconUEventObserver();
                plugged = observer.init(extcons.get(0));
                this.mHDMIObserver = observer;
            } else if (localLOGV) {
                Slog.v("WindowManager", "Not observing HDMI plug state because HDMI was not found.");
            }
        }
        this.mDefaultDisplayPolicy.setHdmiPlugged(plugged, true);
    }

    void sendKey(int key) {
        Intent intent = null;
        Log.d("sendKey", ">>>>>>>>>> key : " + key);
        switch (key) {
            case 3:
                intent = new Intent("com.reallytek.wg.KeyTestActivity_home");
                break;
            case 26:
                Log.d("powerKey", ">>>>>>>>>> sendKey : powerkey");
                intent = new Intent("com.reallytek.wg.KeyTestActivity_power");
                break;
            case 219:
                intent = new Intent("com.reallytek.wg.KeyTestActivity_assist");
                break;
        }
        if (intent != null) {
            this.mContext.sendBroadcast(intent);
        }
    }

    boolean isKeysTest() {
        if (SystemProperties.get("sys.midtest.in_factory", "0").equals("0")) {
            return false;
        }
        ActivityManager am = (ActivityManager) this.mContext.getSystemService(HostingRecord.HOSTING_TYPE_ACTIVITY);
        List<ActivityManager.RunningTaskInfo> runningTasks = am.getRunningTasks(1);
        ComponentName comName = runningTasks.get(0).topActivity;
        Log.d("test", ">>>>>>>>>>>>>>>>> comName:" + comName.getClassName());
        return "com.reallytek.wg.activity.KeyTestActivity".equals(comName.getClassName()) || "com.reallytek.qrcode.QrCodeInfoActivity".equals(comName.getClassName());
    }

    private void sendToMenuKey() {
        sendMenuEvent(0, 0);
    }

    private void sendMenuEvent(int action, int flags) {
        int repeatCount = (flags & 128) != 0 ? 1 : 0;
        long when = SystemClock.uptimeMillis();
        KeyEvent ev = new KeyEvent(this.mMenuDownTime, when, action, 82, repeatCount, 0, -1, 0, flags | 8 | 64, 257);
        InputManager.getInstance().injectInputEvent(ev, 0);
    }

    private ComponentName getTopActivity() {
        ActivityManager am = (ActivityManager) this.mContext.getSystemService(HostingRecord.HOSTING_TYPE_ACTIVITY);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
        if (list != null && list.size() != 0) {
            ActivityManager.RunningTaskInfo topRunningTask = list.get(0);
            return topRunningTask.topActivity;
        }
        return null;
    }

    boolean isInMidtest() {
        ComponentName comName;
        return (SystemProperties.get("sys.midtest.in_factory", "0").equals("0") || (comName = getTopActivity()) == null || !comName.getClassName().startsWith("com.reallytek.wg")) ? false : true;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4694=4] */
    /* JADX WARN: Removed duplicated region for block: B:257:0x0563  */
    /* JADX WARN: Removed duplicated region for block: B:261:0x05d9  */
    /* JADX WARN: Removed duplicated region for block: B:368:0x0813  */
    @Override // com.android.server.policy.WindowManagerPolicy
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public int interceptKeyBeforeQueueing(KeyEvent event, int policyFlags) {
        boolean isWakeKey;
        int result;
        int displayId;
        boolean canceled;
        boolean interactiveAndOn;
        int i;
        boolean interactive;
        String str;
        boolean down;
        int keyCode;
        PhoneWindowManager phoneWindowManager;
        boolean useHapticFeedback;
        int audioMode;
        boolean down2;
        boolean z;
        boolean down3;
        int displayId2;
        HdmiControl hdmiControl;
        int keyCode2 = event.getKeyCode();
        boolean down4 = event.getAction() == 0;
        boolean isWakeKey2 = (policyFlags & 1) != 0 || event.isWakeKey();
        if (keyCode2 == 308) {
            if (down4) {
                Slog.d("WindowManager", "interceptKeyBeforeQueueing keycode = KEYCODE_HALLDOWN");
                if (this.mDeviceStateManagerInternal != null) {
                    ITranPhoneWindowManager.Instance().onInterceptKeyBeforeQueueing(event);
                    this.mDeviceStateManagerInternal.onHallKeyUp(false);
                }
            }
            return 1;
        } else if (keyCode2 == 307) {
            if (down4) {
                Slog.d("WindowManager", "interceptKeyBeforeQueueing keycode = KEYCODE_HALLUP");
                if (this.mDeviceStateManagerInternal != null) {
                    ITranPhoneWindowManager.Instance().onInterceptKeyBeforeQueueing(event);
                    this.mDeviceStateManagerInternal.onHallKeyUp(true);
                }
            }
            return 1;
        } else if (!this.mSystemBooted) {
            boolean shouldTurnOnTv = false;
            if (down4 && (keyCode2 == 26 || keyCode2 == 177)) {
                wakeUpFromPowerKey(event.getDownTime());
                shouldTurnOnTv = true;
            } else if (down4 && ((isWakeKey2 || keyCode2 == 224) && isWakeKeyWhenScreenOff(keyCode2))) {
                wakeUpFromWakeKey(event);
                shouldTurnOnTv = true;
            }
            if (shouldTurnOnTv && (hdmiControl = getHdmiControl()) != null) {
                hdmiControl.turnOnTv();
            }
            return 0;
        } else {
            boolean interactive2 = (536870912 & policyFlags) != 0;
            boolean canceled2 = event.isCanceled();
            int displayId3 = event.getDisplayId();
            boolean isInjected = (16777216 & policyFlags) != 0;
            KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
            boolean keyguardActive = keyguardServiceDelegate != null && (!interactive2 ? !keyguardServiceDelegate.isShowing() : !isKeyguardShowingAndNotOccluded());
            switch (keyCode2) {
                case 24:
                case 25:
                case 26:
                    if (!((Boolean) ITranPhoneWindowManager.Instance().onInterceptKeyBeforeQueueing(event).first).booleanValue()) {
                        ITranPhoneWindowManager.Instance().onInterceptFingerprintKeyByPowerBeforeQueueing(event);
                        break;
                    } else {
                        this.mPowerKeyHandled = true;
                        this.mSingleKeyGestureDetector.reset();
                        this.mKeyCombinationManager.reset();
                        return 0;
                    }
                case 355:
                    KeyguardServiceDelegate keyguardServiceDelegate2 = this.mKeyguardDelegate;
                    if (keyguardServiceDelegate2 != null) {
                        boolean iskeyguardlocked = keyguardServiceDelegate2.isShowing();
                        Log.d("WindowManager", "KEY_FOR_GESTURE iskeyguardlocked=" + iskeyguardlocked + ", down=" + down4);
                        if (down4 && iskeyguardlocked) {
                            ITranPhoneWindowManager.Instance().onInterceptKeyBeforeQueueing(event);
                            break;
                        }
                    } else {
                        Log.d("WindowManager", "KEY_FOR_GESTURE mKeyguardDelegate=null");
                        break;
                    }
                    break;
            }
            ITranPhoneWindowManager.Instance().onInterceptFingerprintKeyBeforeQueueing(event);
            if (MTK_AOD_SUPPORT && this.mWakeAodEnabledSetting && keyCode2 == 305 && down4) {
                this.mDreamManagerInternal.notifyAodAction(1);
            }
            ITranPhoneWindowManager.Instance().onInterceptScrollShot(event);
            if (DEBUG_INPUT) {
                Log.d("WindowManager", "interceptKeyTq keycode=" + keyCode2 + " interactive=" + interactive2 + " keyguardActive=" + keyguardActive + " policyFlags=" + Integer.toHexString(policyFlags));
            }
            if (interactive2 || (isInjected && !isWakeKey2)) {
                int result2 = 1;
                if (interactive2) {
                    if (keyCode2 == this.mPendingWakeKey && !down4) {
                        result2 = 0;
                    }
                    this.mPendingWakeKey = -1;
                    isWakeKey = false;
                    result = result2;
                } else {
                    isWakeKey = false;
                    result = 1;
                }
            } else if (shouldDispatchInputWhenNonInteractive(displayId3, keyCode2)) {
                this.mPendingWakeKey = -1;
                isWakeKey = isWakeKey2;
                result = 1;
            } else {
                if (isWakeKey2 && (!down4 || !isWakeKeyWhenScreenOff(keyCode2))) {
                    isWakeKey2 = false;
                }
                if (isWakeKey2 && down4) {
                    this.mPendingWakeKey = keyCode2;
                }
                isWakeKey = isWakeKey2;
                result = 0;
            }
            if (isValidGlobalKey(keyCode2) && this.mGlobalKeyManager.shouldHandleGlobalKey(keyCode2)) {
                if (!interactive2 && isWakeKey && down4 && this.mGlobalKeyManager.shouldDispatchFromNonInteractive(keyCode2)) {
                    this.mGlobalKeyManager.setBeganFromNonInteractive();
                    result = 1;
                    this.mPendingWakeKey = -1;
                }
                if (isWakeKey) {
                    wakeUpFromWakeKey(event);
                }
                return result;
            }
            HdmiControlManager hdmiControlManager = getHdmiControlManager();
            if (keyCode2 == 177 && this.mHasFeatureLeanback && (hdmiControlManager == null || !hdmiControlManager.shouldHandleTvPowerKey())) {
                return interceptKeyBeforeQueueing(KeyEvent.obtain(event.getDownTime(), event.getEventTime(), event.getAction(), 26, event.getRepeatCount(), event.getMetaState(), event.getDeviceId(), event.getScanCode(), event.getFlags(), event.getSource(), event.getDisplayId(), null), policyFlags);
            }
            boolean isDefaultDisplayOn = Display.isOnState(this.mDefaultDisplay.getState()) || (mMultipleDisplayFlipSupport && Display.isOnState(this.mDisplayManager.getDisplay(1).getState()));
            boolean interactiveAndOn2 = interactive2 && isDefaultDisplayOn;
            if (ITranPhoneWindowManager.Instance().isFpInterceptPower(event) && (event.getFlags() & 1024) == 0) {
                handleKeyGesture(event, interactiveAndOn2);
            }
            boolean isNavBarVirtKey = (event.getFlags() & 64) != 0;
            boolean useHapticFeedback2 = down4 && (policyFlags & 2) != 0 && (!isNavBarVirtKey || this.mNavBarVirtualKeyHapticFeedbackEnabled) && event.getRepeatCount() == 0;
            if (WindowManagerDebugger.WMS_DEBUG_ENG || WindowManagerDebugger.WMS_DEBUG_USER_DEBUG) {
                displayId = displayId3;
                canceled = canceled2;
                interactiveAndOn = interactiveAndOn2;
                i = 26;
                interactive = interactive2;
                str = "WindowManager";
                down = down4;
                keyCode = keyCode2;
                this.mWindowManagerDebugger.debugInterceptKeyBeforeQueueing("WindowManager", keyCode2, interactive2, keyguardActive, policyFlags, down4, canceled, isWakeKey, result, useHapticFeedback2, isInjected);
            } else {
                interactiveAndOn = interactiveAndOn2;
                displayId = displayId3;
                canceled = canceled2;
                interactive = interactive2;
                str = "WindowManager";
                down = down4;
                keyCode = keyCode2;
                i = 26;
            }
            switch (keyCode) {
                case 3:
                    phoneWindowManager = this;
                    Pair<Boolean, Integer> liceRet = ITranPhoneWindowManager.Instance().onInterceptKeyBeforeQueueing(event);
                    if (liceRet.second != null) {
                        result &= ((Integer) liceRet.second).intValue();
                    }
                    ((Boolean) liceRet.first).booleanValue();
                    useHapticFeedback = useHapticFeedback2;
                    break;
                case 4:
                    phoneWindowManager = this;
                    Pair<Boolean, Integer> liceRet2 = ITranPhoneWindowManager.Instance().onInterceptKeyBeforeQueueing(event);
                    if (liceRet2.second != null) {
                        result &= ((Integer) liceRet2.second).intValue();
                    }
                    if (!((Boolean) liceRet2.first).booleanValue()) {
                        if (!down) {
                            if (!hasLongPressOnBackBehavior()) {
                                phoneWindowManager.mBackKeyHandled |= backKeyPress();
                            }
                            if (phoneWindowManager.mBackKeyHandled) {
                                result &= -2;
                                useHapticFeedback = useHapticFeedback2;
                                break;
                            }
                        } else {
                            phoneWindowManager.mBackKeyHandled = false;
                        }
                    }
                    useHapticFeedback = useHapticFeedback2;
                    break;
                case 5:
                    phoneWindowManager = this;
                    if (down) {
                        TelecomManager telecomManager = getTelecommService();
                        if (telecomManager != null && telecomManager.isRinging()) {
                            Log.i(str, "interceptKeyBeforeQueueing: CALL key-down while ringing: Answer the call!");
                            telecomManager.acceptRingingCall();
                            result &= -2;
                        }
                        useHapticFeedback = useHapticFeedback2;
                        break;
                    }
                    useHapticFeedback = useHapticFeedback2;
                    break;
                case 6:
                    phoneWindowManager = this;
                    boolean interactive3 = interactive;
                    boolean canceled3 = canceled;
                    result &= -2;
                    if (!down) {
                        if (!phoneWindowManager.mEndCallKeyHandled) {
                            phoneWindowManager.mHandler.removeCallbacks(phoneWindowManager.mEndCallLongPress);
                            if (!canceled3 && (((phoneWindowManager.mEndcallBehavior & 1) == 0 || !goHome()) && (phoneWindowManager.mEndcallBehavior & 2) != 0)) {
                                phoneWindowManager.sleepDefaultDisplay(event.getEventTime(), 4, 0);
                                isWakeKey = false;
                                useHapticFeedback = useHapticFeedback2;
                                break;
                            }
                        }
                    } else {
                        TelecomManager telecomManager2 = getTelecommService();
                        boolean hungUp = telecomManager2 != null ? telecomManager2.endCall() : false;
                        if (!interactive3 || hungUp) {
                            phoneWindowManager.mEndCallKeyHandled = true;
                        } else {
                            phoneWindowManager.mEndCallKeyHandled = false;
                            phoneWindowManager.mHandler.postDelayed(phoneWindowManager.mEndCallLongPress, ViewConfiguration.get(phoneWindowManager.mContext).getDeviceGlobalActionKeyTimeout());
                        }
                    }
                    useHapticFeedback = useHapticFeedback2;
                    break;
                case 24:
                case 25:
                case 164:
                    phoneWindowManager = this;
                    boolean down5 = down;
                    if (phoneWindowManager.mTranVoiceAssistantSupport && ((keyCode == 25 || keyCode == 24) && down5)) {
                        Handler handler = phoneWindowManager.mHandler;
                        if (handler == null || !handler.hasMessages(MSG_POWER_PRESS_WAKE_UP_AIVA)) {
                            Log.d(str, "mHandler != null: " + (phoneWindowManager.mHandler != null));
                        } else {
                            phoneWindowManager.mHandler.removeMessages(MSG_POWER_PRESS_WAKE_UP_AIVA);
                            Log.d(str, "KEYCODE_VOLUME DOWN & UP removeMessages: MSG_POWER_PRESS_WAKE_UP_AIVA");
                        }
                    }
                    boolean handled = ITranPhoneWindowManager.Instance().handleVolumeKey(phoneWindowManager.mContext, event, getTelecommService(), true);
                    if (handled) {
                        Log.d(str, " handleVolumeKey handle:" + handled);
                        return 0;
                    }
                    if (down5) {
                        phoneWindowManager.sendSystemKeyToStatusBarAsync(event.getKeyCode());
                        NotificationManager nm = getNotificationService();
                        if (nm != null && !phoneWindowManager.mHandleVolumeKeysInWM) {
                            nm.silenceNotificationSound();
                        }
                        TelecomManager telecomManager3 = getTelecommService();
                        if (telecomManager3 != null && !phoneWindowManager.mHandleVolumeKeysInWM && telecomManager3.isRinging()) {
                            Log.i(str, "interceptKeyBeforeQueueing: VOLUME key-down while ringing: Silence ringer!");
                            telecomManager3.silenceRinger();
                            result &= -2;
                            useHapticFeedback = useHapticFeedback2;
                            break;
                        } else {
                            try {
                                audioMode = getAudioService().getMode();
                            } catch (Exception e) {
                                Log.e(str, "Error getting AudioService in interceptKeyBeforeQueueing.", e);
                                audioMode = 0;
                            }
                            boolean isInCall = (telecomManager3 != null && telecomManager3.isInCall()) || audioMode == 3;
                            if (isInCall && (result & 1) == 0) {
                                MediaSessionLegacyHelper.getHelper(phoneWindowManager.mContext).sendVolumeKeyEvent(event, Integer.MIN_VALUE, false);
                                useHapticFeedback = useHapticFeedback2;
                                break;
                            }
                        }
                    }
                    if (phoneWindowManager.mUseTvRouting || phoneWindowManager.mHandleVolumeKeysInWM) {
                        result |= 1;
                        useHapticFeedback = useHapticFeedback2;
                        break;
                    } else {
                        if ((result & 1) == 0) {
                            MediaSessionLegacyHelper.getHelper(phoneWindowManager.mContext).sendVolumeKeyEvent(event, Integer.MIN_VALUE, true);
                        }
                        useHapticFeedback = useHapticFeedback2;
                    }
                    break;
                case 26:
                    phoneWindowManager = this;
                    boolean down6 = down;
                    EventLogTags.writeInterceptPower(KeyEvent.actionToString(event.getAction()), phoneWindowManager.mPowerKeyHandled ? 1 : 0, phoneWindowManager.mSingleKeyGestureDetector.getKeyPressCounter(i));
                    result &= -2;
                    isWakeKey = false;
                    if (TRAN_EMERGENCY_MESSAGE_SUPPORT) {
                        interceptEmergencyContactBroadcast(event);
                    }
                    ITranPhoneWindowManager.Instance().onInterceptPowerKeyBeforeQueueing(event, interactive);
                    Log.e("power_debug", "ITranPhoneWindowManager.Instance().makeScreenOnByPower down: " + down6);
                    if (ITranPhoneWindowManager.Instance().makeScreenOnByPower(down6)) {
                        if (down6) {
                            if (isKeysTest()) {
                                phoneWindowManager.sendKey(i);
                            } else {
                                phoneWindowManager.interceptPowerKeyDown(event, interactiveAndOn);
                            }
                        } else if (!isKeysTest()) {
                            phoneWindowManager.interceptPowerKeyUp(event, canceled);
                        }
                    }
                    useHapticFeedback = useHapticFeedback2;
                    break;
                case 79:
                case 85:
                case 86:
                case 87:
                case 88:
                case 89:
                case 90:
                case 91:
                case 126:
                case FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_AUTO_TIME /* 127 */:
                case 130:
                case 222:
                    phoneWindowManager = this;
                    down2 = down;
                    if (MediaSessionLegacyHelper.getHelper(phoneWindowManager.mContext).isGlobalPriorityActive()) {
                        result &= -2;
                    }
                    if ((result & 1) == 0) {
                        phoneWindowManager.mBroadcastWakeLock.acquire();
                        Message msg = phoneWindowManager.mHandler.obtainMessage(3, new KeyEvent(event));
                        msg.setAsynchronous(true);
                        msg.sendToTarget();
                    }
                    useHapticFeedback = useHapticFeedback2;
                    break;
                case 111:
                    z = false;
                    phoneWindowManager = this;
                    down3 = down;
                    if (!SystemProperties.getBoolean("persist.jcf.ft.sar", z)) {
                        Slog.d(str, "SAR_ESCAPE-SarKeyEvent " + event.getKeyCode() + " " + KeyEvent.actionToString(event.getAction()) + " " + event.getDevice());
                        if ("ant_det".equals(event.getDevice().getName())) {
                            phoneWindowManager.mBroadcastWakeLock.acquire(1000L);
                            Intent intent = new Intent();
                            intent.setAction(ACTION_TINNO_REDUCE_SAR_ESCAPE);
                            intent.putExtra(PARA_SAR, down3);
                            phoneWindowManager.mContext.sendBroadcast(intent);
                            result &= -2;
                            useHapticFeedback = useHapticFeedback2;
                            break;
                        }
                    }
                    useHapticFeedback = useHapticFeedback2;
                    break;
                case 141:
                case 354:
                    phoneWindowManager = this;
                    boolean down7 = down;
                    Pair<Boolean, Integer> liceRet3 = ITranPhoneWindowManager.Instance().onInterceptKeyBeforeQueueing(event);
                    if (liceRet3 != null && ((Boolean) liceRet3.first).booleanValue()) {
                        down = down7;
                        if (down) {
                        }
                    }
                    useHapticFeedback = useHapticFeedback2;
                    break;
                case FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__CROSS_PROFILE_SETTINGS_PAGE_USER_DECLINED_CONSENT /* 171 */:
                    phoneWindowManager = this;
                    boolean down8 = down;
                    if (phoneWindowManager.mShortPressOnWindowBehavior == 1 && phoneWindowManager.mPictureInPictureVisible) {
                        if (!down8) {
                            showPictureInPictureMenu(event);
                        }
                        result &= -2;
                        useHapticFeedback = useHapticFeedback2;
                        break;
                    }
                    useHapticFeedback = useHapticFeedback2;
                    break;
                case 177:
                    phoneWindowManager = this;
                    down2 = down;
                    result &= -2;
                    isWakeKey = false;
                    if (down2 && hdmiControlManager != null) {
                        hdmiControlManager.toggleAndFollowTvPower();
                    }
                    useHapticFeedback = useHapticFeedback2;
                    break;
                case FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__CREDENTIAL_MANAGEMENT_APP_REMOVED /* 187 */:
                    phoneWindowManager = this;
                    down2 = down;
                    Pair<Boolean, Integer> liceRet4 = ITranPhoneWindowManager.Instance().onInterceptKeyBeforeQueueing(event);
                    if (liceRet4.second != null) {
                        result &= ((Integer) liceRet4.second).intValue();
                    }
                    ((Boolean) liceRet4.first).booleanValue();
                    useHapticFeedback = useHapticFeedback2;
                    break;
                case 219:
                    phoneWindowManager = this;
                    boolean down9 = down;
                    Pair<Boolean, Integer> liceRet5 = ITranPhoneWindowManager.Instance().onInterceptKeyBeforeQueueing(event);
                    if (liceRet5.second != null) {
                        result &= ((Integer) liceRet5.second).intValue();
                    }
                    if (!((Boolean) liceRet5.first).booleanValue()) {
                        if (isInMidtest()) {
                            phoneWindowManager.sendKey(219);
                        } else {
                            boolean longPressed = event.getRepeatCount() > 0;
                            if (down9 && !longPressed) {
                                Message msg2 = phoneWindowManager.mHandler.obtainMessage(23, event.getDeviceId(), 0, Long.valueOf(event.getEventTime()));
                                msg2.setAsynchronous(true);
                                msg2.sendToTarget();
                            }
                        }
                        result &= -2;
                        useHapticFeedback = useHapticFeedback2;
                        break;
                    } else {
                        useHapticFeedback = useHapticFeedback2;
                        break;
                    }
                case FrameworkStatsLog.EXCLUSION_RECT_STATE_CHANGED /* 223 */:
                    phoneWindowManager = this;
                    boolean down10 = down;
                    result &= -2;
                    isWakeKey = false;
                    useHapticFeedback = !phoneWindowManager.mPowerManager.isInteractive() ? false : useHapticFeedback2;
                    if (down10) {
                        sleepPress();
                    } else {
                        phoneWindowManager.sleepRelease(event.getEventTime());
                    }
                    break;
                case UsbDescriptor.CLASSID_WIRELESS /* 224 */:
                    phoneWindowManager = this;
                    result &= -2;
                    isWakeKey = true;
                    useHapticFeedback = useHapticFeedback2;
                    break;
                case 231:
                    phoneWindowManager = this;
                    if (!down) {
                        phoneWindowManager.mBroadcastWakeLock.acquire();
                        Message msg3 = phoneWindowManager.mHandler.obtainMessage(12);
                        msg3.setAsynchronous(true);
                        msg3.sendToTarget();
                    }
                    result &= -2;
                    useHapticFeedback = useHapticFeedback2;
                    break;
                case FrameworkStatsLog.TV_TUNER_STATE_CHANGED /* 276 */:
                    phoneWindowManager = this;
                    down2 = down;
                    result &= -2;
                    isWakeKey = false;
                    if (!down2) {
                        phoneWindowManager.mPowerManagerInternal.setUserInactiveOverrideFromWindowManager();
                    }
                    useHapticFeedback = useHapticFeedback2;
                    break;
                case FrameworkStatsLog.TV_CAS_SESSION_OPEN_STATUS /* 280 */:
                case FrameworkStatsLog.ASSISTANT_INVOCATION_REPORTED /* 281 */:
                case FrameworkStatsLog.DISPLAY_WAKE_REPORTED /* 282 */:
                case 283:
                    phoneWindowManager = this;
                    result &= -2;
                    interceptSystemNavigationKey(event);
                    useHapticFeedback = useHapticFeedback2;
                    break;
                case 289:
                case 290:
                case 291:
                case 292:
                case 293:
                case 294:
                case 295:
                case 296:
                case 297:
                case FrameworkStatsLog.BLOB_COMMITTED /* 298 */:
                case FrameworkStatsLog.BLOB_LEASED /* 299 */:
                case 300:
                case FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_ALARM_MANAGER_ALARM_CLOCK /* 301 */:
                case FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_ALARM_MANAGER_WHILE_IDLE /* 302 */:
                case FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_SERVICE_LAUNCH /* 303 */:
                case FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_KEY_CHAIN /* 304 */:
                    phoneWindowManager = this;
                    result &= -2;
                    useHapticFeedback = useHapticFeedback2;
                    break;
                case 309:
                    phoneWindowManager = this;
                    Log.d(str, "voice wakeup " + keyCode);
                    if (!down && phoneWindowManager.mTranVoiceAssistantSupport) {
                        phoneWindowManager.controlAIVA("wakeup_by_voice", false);
                    }
                    result &= -2;
                    useHapticFeedback = useHapticFeedback2;
                    break;
                case 310:
                case FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_PACKAGE_REPLACED /* 311 */:
                case FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_LOCATION_PROVIDER /* 312 */:
                    if (!SystemProperties.getBoolean("persist.jcf.ft.sar", false)) {
                        phoneWindowManager = this;
                        z = false;
                        down3 = down;
                        if (!SystemProperties.getBoolean("persist.jcf.ft.sar", z)) {
                        }
                        useHapticFeedback = useHapticFeedback2;
                        break;
                    } else {
                        Slog.d(str, "SAR_SENSOR-SarKeyEvent " + event.getKeyCode() + " " + KeyEvent.actionToString(event.getAction()) + " " + event.getDevice());
                        Intent intent2 = new Intent();
                        intent2.setAction(ACTION_TINNO_REDUCE_SAR_SENSOR);
                        intent2.putExtra(PARA_SAR, event);
                        phoneWindowManager = this;
                        phoneWindowManager.mContext.sendBroadcast(intent2);
                        result &= -2;
                        useHapticFeedback = useHapticFeedback2;
                        break;
                    }
                    break;
                default:
                    phoneWindowManager = this;
                    useHapticFeedback = useHapticFeedback2;
                    break;
            }
            if (useHapticFeedback) {
                phoneWindowManager.performHapticFeedback(1, false, "Virtual Key - Press");
            }
            if (isWakeKey) {
                wakeUpFromWakeKey(event);
            }
            if ((result & 1) != 0 && !phoneWindowManager.mPerDisplayFocusEnabled && (displayId2 = displayId) != -1 && displayId2 != phoneWindowManager.mTopFocusedDisplayId) {
                Log.i(str, "Moving non-focused display " + displayId2 + " to top because a key is targeting it");
                phoneWindowManager.mWindowManagerFuncs.moveDisplayToTop(displayId2);
            }
            return result;
        }
    }

    private void handleKeyGesture(KeyEvent event, boolean interactive) {
        if (event.getKeyCode() == 310 || event.getKeyCode() == 311 || event.getKeyCode() == 312) {
            Slog.d("WindowManager", "Key code is not power, is sar, so not handle gesture!!!");
        } else if (this.mKeyCombinationManager.interceptKey(event, interactive)) {
            this.mSingleKeyGestureDetector.reset();
        } else {
            if (event.getKeyCode() == 26 && event.getAction() == 0) {
                this.mPowerKeyHandled = handleCameraGesture(event, interactive);
                if (this.mPowerKeyHandled) {
                    this.mSingleKeyGestureDetector.reset();
                    return;
                }
            }
            this.mSingleKeyGestureDetector.interceptKey(event, interactive);
        }
    }

    private boolean handleCameraGesture(KeyEvent event, boolean interactive) {
        if (this.mGestureLauncherService == null) {
            return false;
        }
        this.mCameraGestureTriggered = false;
        MutableBoolean outLaunched = new MutableBoolean(false);
        boolean intercept = this.mGestureLauncherService.interceptPowerKeyDown(event, interactive, outLaunched);
        if (!outLaunched.value) {
            return intercept;
        }
        this.mCameraGestureTriggered = true;
        if (this.mRequestedOrSleepingDefaultDisplay) {
            this.mCameraGestureTriggeredDuringGoingToSleep = true;
        }
        return true;
    }

    private void interceptSystemNavigationKey(KeyEvent event) {
        if (event.getAction() == 1) {
            if ((!this.mAccessibilityManager.isEnabled() || !this.mAccessibilityManager.sendFingerprintGesture(event.getKeyCode())) && this.mSystemNavigationKeysEnabled) {
                sendSystemKeyToStatusBarAsync(event.getKeyCode());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendSystemKeyToStatusBar(int keyCode) {
        IStatusBarService statusBar = getStatusBarService();
        if (statusBar != null) {
            try {
                statusBar.handleSystemKey(keyCode);
            } catch (RemoteException e) {
            }
        }
    }

    private void sendSystemKeyToStatusBarAsync(int keyCode) {
        Message message = this.mHandler.obtainMessage(21, keyCode, 0);
        message.setAsynchronous(true);
        this.mHandler.sendMessage(message);
    }

    private static boolean isValidGlobalKey(int keyCode) {
        switch (keyCode) {
            case 26:
            case FrameworkStatsLog.EXCLUSION_RECT_STATE_CHANGED /* 223 */:
            case UsbDescriptor.CLASSID_WIRELESS /* 224 */:
                return false;
            default:
                return true;
        }
    }

    private boolean isWakeKeyWhenScreenOff(int keyCode) {
        switch (keyCode) {
            case 4:
                return this.mWakeOnBackKeyPress;
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
                return this.mWakeOnDpadKeyPress;
            case 24:
            case 25:
            case 164:
                return this.mDefaultDisplayPolicy.getDockMode() != 0;
            case 79:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 126:
            case FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_AUTO_TIME /* 127 */:
            case 130:
            case 222:
                return false;
            case 219:
                return this.mWakeOnAssistKeyPress;
            default:
                return true;
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public int interceptMotionBeforeQueueingNonInteractive(int displayId, long whenNanos, int policyFlags) {
        if (ITranPhoneWindowManager.Instance().interceptMotionBeforeQueueingNonInteractive(policyFlags)) {
            return 0;
        }
        if ((policyFlags & 1) == 0 || !wakeUp(whenNanos / 1000000, this.mAllowTheaterModeWakeFromMotion, 7, "android.policy:MOTION")) {
            if (shouldDispatchInputWhenNonInteractive(displayId, 0)) {
                return 1;
            }
            if (isTheaterModeEnabled() && (policyFlags & 1) != 0) {
                wakeUp(whenNanos / 1000000, this.mAllowTheaterModeWakeFromMotionWhenNotDreaming, 7, "android.policy:MOTION");
            }
            return 0;
        }
        return 0;
    }

    private boolean shouldDispatchInputWhenNonInteractive(int displayId, int keyCode) {
        Display display;
        IDreamManager dreamManager;
        boolean isDefaultDisplay = displayId == 0 || displayId == -1;
        if (isDefaultDisplay) {
            display = this.mDefaultDisplay;
        } else {
            display = this.mDisplayManager.getDisplay(displayId);
        }
        boolean displayOff = display == null || display.getState() == 1;
        if (displayOff && !this.mHasFeatureWatch) {
            return false;
        }
        if (!isKeyguardShowingAndNotOccluded() || displayOff) {
            if ((!this.mHasFeatureWatch || (keyCode != 4 && keyCode != 264 && keyCode != 265 && keyCode != 266 && keyCode != 267)) && isDefaultDisplay && (dreamManager = getDreamManager()) != null) {
                try {
                    if (dreamManager.isDreaming()) {
                        return !MTK_AOD_SUPPORT;
                    }
                } catch (RemoteException e) {
                    Slog.e("WindowManager", "RemoteException when checking if dreaming", e);
                }
            }
            return false;
        }
        return true;
    }

    private void dispatchDirectAudioEvent(KeyEvent event) {
        HdmiAudioSystemClient audioSystemClient;
        HdmiControlManager hdmiControlManager = getHdmiControlManager();
        if (hdmiControlManager != null && !hdmiControlManager.getSystemAudioMode() && shouldCecAudioDeviceForwardVolumeKeysSystemAudioModeOff() && (audioSystemClient = hdmiControlManager.getAudioSystemClient()) != null) {
            audioSystemClient.sendKeyEvent(event.getKeyCode(), event.getAction() == 0);
            return;
        }
        try {
            getAudioService().handleVolumeKey(event, this.mUseTvRouting, this.mContext.getOpPackageName(), "WindowManager");
        } catch (Exception e) {
            Log.e("WindowManager", "Error dispatching volume key in handleVolumeKey for event:" + event, e);
        }
    }

    private HdmiControlManager getHdmiControlManager() {
        if (!this.mHasFeatureHdmiCec) {
            return null;
        }
        return (HdmiControlManager) this.mContext.getSystemService(HdmiControlManager.class);
    }

    private boolean shouldCecAudioDeviceForwardVolumeKeysSystemAudioModeOff() {
        return RoSystemProperties.CEC_AUDIO_DEVICE_FORWARD_VOLUME_KEYS_SYSTEM_AUDIO_MODE_OFF;
    }

    void dispatchMediaKeyWithWakeLock(KeyEvent event) {
        if (DEBUG_INPUT) {
            Slog.d("WindowManager", "dispatchMediaKeyWithWakeLock: " + event);
        }
        if (this.mHavePendingMediaKeyRepeatWithWakeLock) {
            if (DEBUG_INPUT) {
                Slog.d("WindowManager", "dispatchMediaKeyWithWakeLock: canceled repeat");
            }
            this.mHandler.removeMessages(4);
            this.mHavePendingMediaKeyRepeatWithWakeLock = false;
            this.mBroadcastWakeLock.release();
        }
        dispatchMediaKeyWithWakeLockToAudioService(event);
        if (event.getAction() == 0 && event.getRepeatCount() == 0) {
            this.mHavePendingMediaKeyRepeatWithWakeLock = true;
            Message msg = this.mHandler.obtainMessage(4, event);
            msg.setAsynchronous(true);
            this.mHandler.sendMessageDelayed(msg, ViewConfiguration.getKeyRepeatTimeout());
            return;
        }
        this.mBroadcastWakeLock.release();
    }

    void dispatchMediaKeyRepeatWithWakeLock(KeyEvent event) {
        this.mHavePendingMediaKeyRepeatWithWakeLock = false;
        KeyEvent repeatEvent = KeyEvent.changeTimeRepeat(event, SystemClock.uptimeMillis(), 1, event.getFlags() | 128);
        if (DEBUG_INPUT) {
            Slog.d("WindowManager", "dispatchMediaKeyRepeatWithWakeLock: " + repeatEvent);
        }
        dispatchMediaKeyWithWakeLockToAudioService(repeatEvent);
        this.mBroadcastWakeLock.release();
    }

    void dispatchMediaKeyWithWakeLockToAudioService(KeyEvent event) {
        if (this.mActivityManagerInternal.isSystemReady()) {
            MediaSessionLegacyHelper.getHelper(this.mContext).sendMediaButtonEvent(event, true);
        }
    }

    void launchVoiceAssistWithWakeLock() {
        Intent voiceIntent;
        sendCloseSystemWindows(SYSTEM_DIALOG_REASON_ASSIST);
        if (!keyguardOn()) {
            voiceIntent = new Intent("android.speech.action.WEB_SEARCH");
        } else {
            DeviceIdleManager dim = (DeviceIdleManager) this.mContext.getSystemService(DeviceIdleManager.class);
            if (dim != null) {
                dim.endIdle("voice-search");
            }
            Intent voiceIntent2 = new Intent("android.speech.action.VOICE_SEARCH_HANDS_FREE");
            voiceIntent2.putExtra("android.speech.extras.EXTRA_SECURE", true);
            voiceIntent = voiceIntent2;
        }
        startActivityAsUser(voiceIntent, UserHandle.CURRENT_OR_SELF);
        this.mBroadcastWakeLock.release();
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void startedGoingToSleep(int pmSleepReason) {
        if (DEBUG_WAKEUP) {
            Slog.i("WindowManager", "Started going to sleep... (why=" + WindowManagerPolicyConstants.offReasonToString(WindowManagerPolicyConstants.translateSleepReasonToOffReason(pmSleepReason)) + ")");
        }
        this.mDeviceGoingToSleep = true;
        this.mRequestedOrSleepingDefaultDisplay = true;
        ITranPhoneWindowManager.Instance().onStartedGoingToSleep(this.mKeyguardDelegate);
        KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
        if (keyguardServiceDelegate != null) {
            keyguardServiceDelegate.onStartedGoingToSleep(pmSleepReason);
        }
        ITranPhoneWindowManager.Instance().onStartedGoingToSleep(this.mKeyguardDelegate);
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void finishedGoingToSleep(int pmSleepReason) {
        EventLogTags.writeScreenToggled(0);
        if (DEBUG_WAKEUP) {
            Slog.i("WindowManager", "Finished going to sleep... (why=" + WindowManagerPolicyConstants.offReasonToString(WindowManagerPolicyConstants.translateSleepReasonToOffReason(pmSleepReason)) + ")");
        }
        MetricsLogger.histogram(this.mContext, "screen_timeout", this.mLockScreenTimeout / 1000);
        this.mDeviceGoingToSleep = false;
        this.mRequestedOrSleepingDefaultDisplay = false;
        this.mDefaultDisplayPolicy.setAwake(false);
        ITranPhoneWindowManager.Instance().onFinishedGoingToSleep();
        synchronized (this.mLock) {
            updateWakeGestureListenerLp();
            updateLockScreenTimeout();
        }
        this.mDefaultDisplayRotation.updateOrientationListener();
        KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
        if (keyguardServiceDelegate != null) {
            keyguardServiceDelegate.onFinishedGoingToSleep(pmSleepReason, this.mCameraGestureTriggeredDuringGoingToSleep);
        }
        DisplayFoldController displayFoldController = this.mDisplayFoldController;
        if (displayFoldController != null) {
            displayFoldController.finishedGoingToSleep();
        }
        this.mCameraGestureTriggeredDuringGoingToSleep = false;
        this.mCameraGestureTriggered = false;
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void onPowerGroupWakefulnessChanged(int groupId, int wakefulness, int pmSleepReason, int globalWakefulness) {
        KeyguardServiceDelegate keyguardServiceDelegate;
        if (wakefulness != globalWakefulness && wakefulness != 1 && groupId == 0 && (keyguardServiceDelegate = this.mKeyguardDelegate) != null) {
            keyguardServiceDelegate.doKeyguardTimeout(null);
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void startedWakingUp(int pmWakeReason) {
        EventLogTags.writeScreenToggled(1);
        if (DEBUG_WAKEUP) {
            Slog.i("WindowManager", "Started waking up... (why=" + WindowManagerPolicyConstants.onReasonToString(WindowManagerPolicyConstants.translateWakeReasonToOnReason(pmWakeReason)) + ")");
        }
        ITranPhoneWindowManager.Instance().onStartedWakingUp();
        this.mActivityTaskManagerInternal.notifyWakingUp();
        this.mDefaultDisplayPolicy.setAwake(true);
        synchronized (this.mLock) {
            updateWakeGestureListenerLp();
            updateLockScreenTimeout();
        }
        this.mDefaultDisplayRotation.updateOrientationListener();
        KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
        if (keyguardServiceDelegate != null) {
            keyguardServiceDelegate.onStartedWakingUp(pmWakeReason, this.mCameraGestureTriggered);
        }
        this.mCameraGestureTriggered = false;
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void finishedWakingUp(int pmWakeReason) {
        if (DEBUG_WAKEUP) {
            Slog.i("WindowManager", "Finished waking up... (why=" + WindowManagerPolicyConstants.onReasonToString(WindowManagerPolicyConstants.translateWakeReasonToOnReason(pmWakeReason)) + ")");
        }
        KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
        if (keyguardServiceDelegate != null) {
            keyguardServiceDelegate.onFinishedWakingUp();
        }
        DisplayFoldController displayFoldController = this.mDisplayFoldController;
        if (displayFoldController != null) {
            displayFoldController.finishedWakingUp();
        }
        ITranPhoneWindowManager.Instance().onFinishedWakingUp(this.mKeyguardDelegate);
    }

    private boolean shouldWakeUpWithHomeIntent() {
        if (this.mWakeUpToLastStateTimeout <= 0) {
            return false;
        }
        long sleepDuration = this.mPowerManagerInternal.getLastWakeup().sleepDuration;
        if (DEBUG_WAKEUP) {
            Log.i("WindowManager", "shouldWakeUpWithHomeIntent: sleepDuration= " + sleepDuration + " mWakeUpToLastStateTimeout= " + this.mWakeUpToLastStateTimeout);
        }
        return sleepDuration > this.mWakeUpToLastStateTimeout;
    }

    private void wakeUpFromPowerKey(long eventTime) {
        if (wakeUp(eventTime, this.mAllowTheaterModeWakeFromPowerKey, 1, "android.policy:POWER") && shouldWakeUpWithHomeIntent()) {
            startDockOrHome(0, false, true, PowerManager.wakeReasonToString(1));
        }
    }

    private void wakeUpFromWakeKey(KeyEvent event) {
        if (wakeUp(event.getEventTime(), this.mAllowTheaterModeWakeFromKey, 6, "android.policy:KEY") && shouldWakeUpWithHomeIntent() && event.getKeyCode() == 3) {
            startDockOrHome(0, true, true, PowerManager.wakeReasonToString(6));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean wakeUp(long wakeTime, boolean wakeInTheaterMode, int reason, String details) {
        boolean theaterModeEnabled = isTheaterModeEnabled();
        if (!wakeInTheaterMode && theaterModeEnabled) {
            return false;
        }
        if (theaterModeEnabled) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "theater_mode_on", 0);
        }
        this.mPowerManager.wakeUp(wakeTime, reason, details);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void finishKeyguardDrawn() {
        ITranPhoneWindowManager.Instance().onFinishKeyguardDrawn();
        if (!this.mDefaultDisplayPolicy.finishKeyguardDrawn()) {
            TranFoldDisplayCustody.instance().keyguardDrawnTimeout();
            return;
        }
        synchronized (this.mLock) {
            ITranPhoneWindowManager.Instance().onFinishKeyguardDrawnLock();
            if (this.mKeyguardDelegate != null) {
                this.mHandler.removeMessages(6);
            }
        }
        this.mWindowManagerInternal.waitForAllWindowsDrawn(new Runnable() { // from class: com.android.server.policy.PhoneWindowManager$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                PhoneWindowManager.this.m5989xe9424e8d();
            }
        }, WAITING_FOR_DRAWN_TIMEOUT_SUPPLIER.getAsInt(), -1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$finishKeyguardDrawn$1$com-android-server-policy-PhoneWindowManager  reason: not valid java name */
    public /* synthetic */ void m5989xe9424e8d() {
        if (DEBUG_WAKEUP) {
            Slog.i("WindowManager", "All windows ready for every display");
        }
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(7, -1, 0));
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void screenTurnedOff(int displayId) {
        if (DEBUG_WAKEUP) {
            Slog.i("WindowManager", "Display" + displayId + " turned off...");
        }
        if (displayId == 0) {
            updateScreenOffSleepToken(true);
            this.mRequestedOrSleepingDefaultDisplay = false;
            this.mDefaultDisplayPolicy.screenTurnedOff();
            synchronized (this.mLock) {
                KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
                if (keyguardServiceDelegate != null) {
                    keyguardServiceDelegate.onScreenTurnedOff();
                }
                ITranPhoneWindowManager.Instance().onScreenTurnedOff();
            }
            this.mDefaultDisplayRotation.updateOrientationListener();
            reportScreenStateToVrManager(false);
            if (this.mCameraGestureTriggeredDuringGoingToSleep) {
                wakeUp(SystemClock.uptimeMillis(), this.mAllowTheaterModeWakeFromPowerKey, 5, "com.android.systemui:CAMERA_GESTURE_PREVENT_LOCK");
            }
        }
    }

    private long getKeyguardDrawnTimeout() {
        boolean bootCompleted = ((SystemServiceManager) LocalServices.getService(SystemServiceManager.class)).isBootCompleted();
        return bootCompleted ? 1000L : 5000L;
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void screenTurningOn(final int displayId, WindowManagerPolicy.ScreenOnListener screenOnListener) {
        KeyguardServiceDelegate keyguardServiceDelegate;
        if (DEBUG_WAKEUP) {
            Slog.i("WindowManager", "Display " + displayId + " turning on...   mScreenOnBecacuseOfDisplayTransition=" + this.mScreenOnBecacuseOfDisplayTransition);
        }
        if (displayId == 0) {
            Trace.asyncTraceBegin(32L, "screenTurningOn", 0);
            updateScreenOffSleepToken(false);
            this.mDefaultDisplayPolicy.screenTurnedOn(screenOnListener);
            this.mBootAnimationDismissable = false;
            if (TRAN_FACEID_SUPPORT && (keyguardServiceDelegate = this.mKeyguardDelegate) != null && keyguardServiceDelegate.hasKeyguard()) {
                this.mKeyguardDelegate.onStartFaceUnlock();
            }
            synchronized (this.mLock) {
                KeyguardServiceDelegate keyguardServiceDelegate2 = this.mKeyguardDelegate;
                if (keyguardServiceDelegate2 != null && keyguardServiceDelegate2.hasKeyguard()) {
                    this.mHandler.removeMessages(6);
                    long keyguardDrawnTimeout = getKeyguardDrawnTimeout();
                    if (this.mScreenOnBecacuseOfDisplayTransition && !isKeyguardLocked()) {
                        keyguardDrawnTimeout = 0;
                    }
                    this.mHandler.sendEmptyMessageDelayed(6, keyguardDrawnTimeout);
                    this.mKeyguardDelegate.onScreenTurningOn(this.mKeyguardDrawnCallback);
                } else {
                    if (DEBUG_WAKEUP) {
                        Slog.d("WindowManager", "null mKeyguardDelegate: setting mKeyguardDrawComplete.");
                    }
                    this.mHandler.sendEmptyMessage(5);
                }
                ITranPhoneWindowManager.Instance().onScreenTurningOn(this.mKeyguardDelegate);
                ITranPhoneWindowManager.Instance().onScreenTurningOnLock(this.mKeyguardDelegate);
            }
            return;
        }
        this.mScreenOnListeners.put(displayId, screenOnListener);
        this.mWindowManagerInternal.waitForAllWindowsDrawn(new Runnable() { // from class: com.android.server.policy.PhoneWindowManager$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                PhoneWindowManager.this.m5990xf2d1e5a5(displayId);
            }
        }, WAITING_FOR_DRAWN_TIMEOUT_SUPPLIER.getAsInt(), displayId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$screenTurningOn$2$com-android-server-policy-PhoneWindowManager  reason: not valid java name */
    public /* synthetic */ void m5990xf2d1e5a5(int displayId) {
        if (DEBUG_WAKEUP) {
            Slog.i("WindowManager", "All windows ready for display: " + displayId);
        }
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(7, displayId, 0));
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void resetDreamAnimationFeatureEnabledState() {
        if (ITranDisplayPolicy.Instance().isNeedAodTransparent()) {
            this.mNeedHideAodWin = false;
            this.mDefaultDisplayPolicy.mAodHideByFingerprint = false;
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void screenTurningOn(int displayId, WindowManagerPolicy.ScreenOnListener screenOnListener, int screenState, boolean screenOnBecauseDisplayTransition) {
        this.mScreenOnBecacuseOfDisplayTransition = screenOnBecauseDisplayTransition;
        screenTurningOn(displayId, screenOnListener);
        this.mScreenOnBecacuseOfDisplayTransition = false;
        if (MTK_AOD_SUPPORT) {
            this.mNeedHideAodWin = screenState == 2;
            Slog.d("WindowManager", "screenTurningOn mNeedHideAodWin:" + this.mNeedHideAodWin + " ,screenState:" + screenState);
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void screenTurnedOn(int displayId) {
        if (DEBUG_WAKEUP) {
            Slog.i("WindowManager", "Display " + displayId + " turned on...");
        }
        if (displayId != 0) {
            return;
        }
        synchronized (this.mLock) {
            KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
            if (keyguardServiceDelegate != null) {
                keyguardServiceDelegate.onScreenTurnedOn();
            }
            ITranPhoneWindowManager.Instance().onScreenTurnedOn();
        }
        reportScreenStateToVrManager(true);
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void screenTurningOff(int displayId, WindowManagerPolicy.ScreenOffListener screenOffListener, boolean isTransition) {
        this.mWindowManagerFuncs.screenTurningOff(displayId, screenOffListener, isTransition);
        if (displayId != 0) {
            return;
        }
        this.mNeedHideAodWin = false;
        this.mDefaultDisplayPolicy.mAodHideByFingerprint = false;
        this.mRequestedOrSleepingDefaultDisplay = true;
        synchronized (this.mLock) {
            KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
            if (keyguardServiceDelegate != null) {
                keyguardServiceDelegate.onScreenTurningOff();
            }
        }
    }

    private void reportScreenStateToVrManager(boolean isScreenOn) {
        if (this.mVrManagerInternal == null) {
            return;
        }
        this.mVrManagerInternal.onScreenStateChanged(isScreenOn);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void finishWindowsDrawn(int displayId) {
        if (displayId != 0 && displayId != -1) {
            WindowManagerPolicy.ScreenOnListener screenOnListener = (WindowManagerPolicy.ScreenOnListener) this.mScreenOnListeners.removeReturnOld(displayId);
            if (screenOnListener != null) {
                screenOnListener.onScreenOn();
            }
        } else if (!this.mDefaultDisplayPolicy.finishWindowsDrawn()) {
        } else {
            finishScreenTurningOn();
        }
    }

    private void finishScreenTurningOn() {
        this.mDefaultDisplayRotation.updateOrientationListener();
        WindowManagerPolicy.ScreenOnListener listener = this.mDefaultDisplayPolicy.getScreenOnListener();
        if (!this.mDefaultDisplayPolicy.finishScreenTurningOn()) {
            return;
        }
        Trace.asyncTraceEnd(32L, "screenTurningOn", 0);
        enableScreen(listener, true);
    }

    private void enableScreen(WindowManagerPolicy.ScreenOnListener listener, boolean report) {
        boolean enableScreen;
        boolean awake = this.mDefaultDisplayPolicy.isAwake();
        synchronized (this.mLock) {
            if (!this.mKeyguardDrawnOnce && awake) {
                this.mKeyguardDrawnOnce = true;
                enableScreen = true;
                if (this.mBootMessageNeedsHiding) {
                    this.mBootMessageNeedsHiding = false;
                    hideBootMessages();
                }
            } else {
                enableScreen = false;
            }
        }
        if (report && listener != null) {
            listener.onScreenOn();
        }
        if (enableScreen) {
            try {
                this.mWindowManager.enableScreenIfNeeded();
            } catch (RemoteException e) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleHideBootMessage() {
        synchronized (this.mLock) {
            if (!this.mKeyguardDrawnOnce) {
                this.mBootMessageNeedsHiding = true;
            } else if (this.mBootMsgDialog != null) {
                if (DEBUG_WAKEUP) {
                    Slog.d("WindowManager", "handleHideBootMessage: dismissing");
                }
                this.mBootMsgDialog.dismiss();
                this.mBootMsgDialog = null;
            }
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public boolean isScreenOn() {
        return this.mDefaultDisplayPolicy.isScreenOnEarly();
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public boolean okToAnimate(boolean ignoreScreenOn) {
        return ((ignoreScreenOn || isScreenOn()) && !this.mDeviceGoingToSleep) || ITranPhoneWindowManager.Instance().isWindowAnimationForDream();
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void enableKeyguard(boolean enabled) {
        KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
        if (keyguardServiceDelegate != null) {
            keyguardServiceDelegate.setKeyguardEnabled(enabled);
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void exitKeyguardSecurely(WindowManagerPolicy.OnKeyguardExitResult callback) {
        KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
        if (keyguardServiceDelegate != null) {
            keyguardServiceDelegate.verifyUnlock(callback);
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public boolean isKeyguardShowing() {
        KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
        if (keyguardServiceDelegate == null) {
            return false;
        }
        return keyguardServiceDelegate.isShowing();
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public boolean isKeyguardShowingAndNotOccluded() {
        KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
        return (keyguardServiceDelegate == null || !keyguardServiceDelegate.isShowing() || isKeyguardOccluded()) ? false : true;
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public boolean isKeyguardTrustedLw() {
        KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
        if (keyguardServiceDelegate == null) {
            return false;
        }
        return keyguardServiceDelegate.isTrusted();
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public boolean isKeyguardLocked() {
        return keyguardOn();
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public boolean isKeyguardSecure(int userId) {
        KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
        if (keyguardServiceDelegate == null) {
            return false;
        }
        return keyguardServiceDelegate.isSecure(userId);
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public boolean isKeyguardOccluded() {
        KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
        if (keyguardServiceDelegate == null) {
            return false;
        }
        return keyguardServiceDelegate.isOccluded();
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public boolean inKeyguardRestrictedKeyInputMode() {
        KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
        if (keyguardServiceDelegate == null) {
            return false;
        }
        return keyguardServiceDelegate.isInputRestricted();
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public boolean isKeyguardUnoccluding() {
        return keyguardOn() && !this.mWindowManagerFuncs.isAppTransitionStateIdle();
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void dismissKeyguardLw(IKeyguardDismissCallback callback, CharSequence message) {
        KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
        if (keyguardServiceDelegate != null && keyguardServiceDelegate.isShowing()) {
            if (DEBUG_KEYGUARD) {
                Slog.d("WindowManager", "PWM.dismissKeyguardLw");
            }
            this.mKeyguardDelegate.dismiss(callback, message);
        } else if (callback != null) {
            try {
                callback.onDismissError();
            } catch (RemoteException e) {
                Slog.w("WindowManager", "Failed to call callback", e);
            }
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public boolean isKeyguardDrawnLw() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mKeyguardDrawnOnce;
        }
        return z;
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void startKeyguardExitAnimation(long startTime, long fadeoutDuration) {
        if (this.mKeyguardDelegate != null) {
            if (DEBUG_KEYGUARD) {
                Slog.d("WindowManager", "PWM.startKeyguardExitAnimation");
            }
            this.mKeyguardDelegate.startKeyguardExitAnimation(startTime, fadeoutDuration);
        }
    }

    void sendCloseSystemWindows() {
        PhoneWindow.sendCloseSystemWindows(this.mContext, (String) null);
    }

    void sendCloseSystemWindows(String reason) {
        PhoneWindow.sendCloseSystemWindows(this.mContext, reason);
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void setSafeMode(boolean safeMode) {
        this.mSafeMode = safeMode;
        if (safeMode) {
            performHapticFeedback(FrameworkStatsLog.WIFI_BYTES_TRANSFER_BY_FG_BG, true, "Safe Mode Enabled");
        }
    }

    static long[] getLongIntArray(Resources r, int resid) {
        return ArrayUtils.convertToLongArray(r.getIntArray(resid));
    }

    private void bindKeyguard() {
        synchronized (this.mLock) {
            if (this.mKeyguardBound) {
                return;
            }
            this.mKeyguardBound = true;
            this.mKeyguardDelegate.bindService(this.mContext);
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void onSystemUiStarted() {
        bindKeyguard();
        ITranWindowManagerService.Instance().onSystemUiStarted();
    }

    boolean isOOBEInit() {
        int oobeValue = SystemProperties.getInt("persist.sys.oobe", 3);
        ComponentName cn = new ComponentName("com.google.android.setupwizard", "com.google.android.setupwizard.SetupWizardActivity");
        if (this.mContext.getPackageManager().getComponentEnabledSetting(cn) == 2 || oobeValue != 3) {
            return oobeValue == 2 || oobeValue == 3;
        }
        return false;
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void systemReady() {
        this.mKeyguardDelegate.onSystemReady();
        this.mVrManagerInternal = (VrManagerInternal) LocalServices.getService(VrManagerInternal.class);
        if (this.mVrManagerInternal != null) {
            this.mVrManagerInternal.addPersistentVrModeStateListener(this.mPersistentVrModeListener);
        }
        readCameraLensCoverState();
        updateUiMode();
        this.mDefaultDisplayRotation.updateOrientationListener();
        synchronized (this.mLock) {
            this.mSystemReady = true;
            this.mHandler.post(new Runnable() { // from class: com.android.server.policy.PhoneWindowManager.16
                @Override // java.lang.Runnable
                public void run() {
                    PhoneWindowManager.this.updateSettings();
                }
            });
            if (this.mSystemBooted) {
                this.mKeyguardDelegate.onBootCompleted();
            }
        }
        this.mAutofillManagerInternal = (AutofillManagerInternal) LocalServices.getService(AutofillManagerInternal.class);
        if (this.mDreamManagerInternal == null) {
            this.mDreamManagerInternal = (DreamManagerInternal) LocalServices.getService(DreamManagerInternal.class);
        }
        this.mGestureLauncherService = (GestureLauncherService) LocalServices.getService(GestureLauncherService.class);
        if (isOOBEInit()) {
            try {
                ComponentName cn = new ComponentName("com.google.android.setupwizard", "com.google.android.setupwizard.SetupWizardActivity");
                if ((!isUserSetupComplete() || !isDeviceProvisioned()) && this.mContext.getPackageManager().getComponentEnabledSetting(cn) == 2) {
                    Settings.Secure.putInt(this.mContext.getContentResolver(), "user_setup_complete", 1);
                    Settings.Global.putInt(this.mContext.getContentResolver(), "device_provisioned", 1);
                }
            } catch (Exception e) {
                Log.e("WindowManager", "Exception : " + e);
            }
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void systemBooted() {
        boolean defaultDisplayOn;
        boolean defaultScreenTurningOn;
        bindKeyguard();
        synchronized (this.mLock) {
            this.mSystemBooted = true;
            if (this.mSystemReady) {
                this.mKeyguardDelegate.onBootCompleted();
            }
            ITranPhoneWindowManager.Instance().onSystemBooted();
        }
        this.mSideFpsEventHandler.onFingerprintSensorReady();
        startedWakingUp(0);
        finishedWakingUp(0);
        int defaultDisplayState = this.mDisplayManager.getDisplay(0).getState();
        if (defaultDisplayState == 2) {
            defaultDisplayOn = true;
        } else {
            defaultDisplayOn = false;
        }
        if (this.mDefaultDisplayPolicy.getScreenOnListener() != null) {
            defaultScreenTurningOn = true;
        } else {
            defaultScreenTurningOn = false;
        }
        if (defaultDisplayOn || defaultScreenTurningOn) {
            screenTurningOn(0, this.mDefaultDisplayPolicy.getScreenOnListener());
            screenTurnedOn(0);
        } else {
            this.mBootAnimationDismissable = true;
            enableScreen(null, false);
        }
        ITranPhoneWindowManager.Instance().onSystemBootedEnd();
        this.mDeviceStateManagerInternal.onSystemBootedEnd();
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public boolean canDismissBootAnimation() {
        return this.mDefaultDisplayPolicy.isKeyguardDrawComplete() || this.mBootAnimationDismissable;
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void showBootMessage(final CharSequence msg, boolean always) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.policy.PhoneWindowManager.17
            @Override // java.lang.Runnable
            public void run() {
                int theme;
                if (PhoneWindowManager.this.mBootMsgDialog == null) {
                    if (PhoneWindowManager.this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                        theme = 16974893;
                    } else {
                        theme = 0;
                    }
                    PhoneWindowManager.this.mBootMsgDialog = new ProgressDialog(PhoneWindowManager.this.mContext, theme) { // from class: com.android.server.policy.PhoneWindowManager.17.1
                        @Override // android.app.Dialog, android.view.Window.Callback
                        public boolean dispatchKeyEvent(KeyEvent event) {
                            return true;
                        }

                        @Override // android.app.Dialog, android.view.Window.Callback
                        public boolean dispatchKeyShortcutEvent(KeyEvent event) {
                            return true;
                        }

                        @Override // android.app.Dialog, android.view.Window.Callback
                        public boolean dispatchTouchEvent(MotionEvent ev) {
                            return true;
                        }

                        @Override // android.app.Dialog, android.view.Window.Callback
                        public boolean dispatchTrackballEvent(MotionEvent ev) {
                            return true;
                        }

                        @Override // android.app.Dialog, android.view.Window.Callback
                        public boolean dispatchGenericMotionEvent(MotionEvent ev) {
                            return true;
                        }

                        @Override // android.app.Dialog, android.view.Window.Callback
                        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
                            return true;
                        }
                    };
                    if (PhoneWindowManager.this.mPackageManager.isDeviceUpgrading()) {
                        PhoneWindowManager.this.mBootMsgDialog.setTitle(17039671);
                    } else {
                        PhoneWindowManager.this.mBootMsgDialog.setTitle(17039664);
                    }
                    PhoneWindowManager.this.mBootMsgDialog.setProgressStyle(0);
                    PhoneWindowManager.this.mBootMsgDialog.setIndeterminate(true);
                    PhoneWindowManager.this.mBootMsgDialog.getWindow().setType(2021);
                    PhoneWindowManager.this.mBootMsgDialog.getWindow().addFlags(258);
                    PhoneWindowManager.this.mBootMsgDialog.getWindow().setDimAmount(1.0f);
                    WindowManager.LayoutParams lp = PhoneWindowManager.this.mBootMsgDialog.getWindow().getAttributes();
                    lp.screenOrientation = 5;
                    lp.setFitInsetsTypes(0);
                    PhoneWindowManager.this.mBootMsgDialog.getWindow().setAttributes(lp);
                    PhoneWindowManager.this.mBootMsgDialog.setCancelable(false);
                    PhoneWindowManager.this.mBootMsgDialog.show();
                }
                PhoneWindowManager.this.mBootMsgDialog.setMessage(msg);
            }
        });
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void hideBootMessages() {
        this.mHandler.sendEmptyMessage(11);
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void userActivity() {
        synchronized (this.mScreenLockTimeout) {
            if (this.mLockScreenTimerActive) {
                this.mHandler.removeCallbacks(this.mScreenLockTimeout);
                this.mHandler.postDelayed(this.mScreenLockTimeout, this.mLockScreenTimeout);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class ScreenLockTimeout implements Runnable {
        Bundle options;

        ScreenLockTimeout() {
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (this) {
                if (PhoneWindowManager.localLOGV) {
                    Log.v("WindowManager", "mScreenLockTimeout activating keyguard");
                }
                if (PhoneWindowManager.this.mKeyguardDelegate != null) {
                    PhoneWindowManager.this.mKeyguardDelegate.doKeyguardTimeout(this.options);
                }
                PhoneWindowManager.this.mLockScreenTimerActive = false;
                PhoneWindowManager.this.mLockNowPending = false;
                this.options = null;
            }
        }

        public void setLockOptions(Bundle options) {
            this.options = options;
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void lockNow(Bundle options) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
        this.mHandler.removeCallbacks(this.mScreenLockTimeout);
        if (options != null) {
            this.mScreenLockTimeout.setLockOptions(options);
        }
        this.mHandler.post(this.mScreenLockTimeout);
        synchronized (this.mScreenLockTimeout) {
            this.mLockNowPending = true;
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void setAllowLockscreenWhenOn(int displayId, boolean allow) {
        if (allow) {
            this.mAllowLockscreenWhenOnDisplays.add(Integer.valueOf(displayId));
        } else {
            this.mAllowLockscreenWhenOnDisplays.remove(Integer.valueOf(displayId));
        }
        updateLockScreenTimeout();
    }

    private void updateLockScreenTimeout() {
        KeyguardServiceDelegate keyguardServiceDelegate;
        synchronized (this.mScreenLockTimeout) {
            if (this.mLockNowPending) {
                Log.w("WindowManager", "lockNow pending, ignore updating lockscreen timeout");
                return;
            }
            boolean enable = !this.mAllowLockscreenWhenOnDisplays.isEmpty() && this.mDefaultDisplayPolicy.isAwake() && (keyguardServiceDelegate = this.mKeyguardDelegate) != null && keyguardServiceDelegate.isSecure(this.mCurrentUserId);
            if (this.mLockScreenTimerActive != enable) {
                if (enable) {
                    if (localLOGV) {
                        Log.v("WindowManager", "setting lockscreen timer");
                    }
                    this.mHandler.removeCallbacks(this.mScreenLockTimeout);
                    this.mHandler.postDelayed(this.mScreenLockTimeout, this.mLockScreenTimeout);
                } else {
                    if (localLOGV) {
                        Log.v("WindowManager", "clearing lockscreen timer");
                    }
                    this.mHandler.removeCallbacks(this.mScreenLockTimeout);
                }
                this.mLockScreenTimerActive = enable;
            }
        }
    }

    private void updateScreenOffSleepToken(boolean acquire) {
        if (acquire) {
            this.mScreenOffSleepTokenAcquirer.acquire(0);
        } else {
            this.mScreenOffSleepTokenAcquirer.release(0);
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void enableScreenAfterBoot() {
        readLidState();
        applyLidSwitchState();
        updateRotation(true);
    }

    private void applyLidSwitchState() {
        int lidState = this.mDefaultDisplayPolicy.getLidState();
        if (lidState == 0) {
            int lidBehavior = getLidBehavior();
            switch (lidBehavior) {
                case 1:
                    sleepDefaultDisplay(SystemClock.uptimeMillis(), 3, 1);
                    break;
                case 2:
                    this.mWindowManagerFuncs.lockDeviceNow();
                    break;
            }
        }
        synchronized (this.mLock) {
            updateWakeGestureListenerLp();
        }
    }

    void updateUiMode() {
        if (this.mUiModeManager == null) {
            this.mUiModeManager = IUiModeManager.Stub.asInterface(ServiceManager.getService("uimode"));
        }
        try {
            this.mUiMode = this.mUiModeManager.getCurrentModeType();
        } catch (RemoteException e) {
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public int getUiMode() {
        return this.mUiMode;
    }

    void updateRotation(boolean alwaysSendConfiguration) {
        try {
            this.mWindowManager.updateRotation(alwaysSendConfiguration, false);
        } catch (RemoteException e) {
        }
    }

    Intent createHomeDockIntent() {
        Intent intent = null;
        int i = this.mUiMode;
        if (i == 3) {
            if (this.mEnableCarDockHomeCapture) {
                intent = this.mCarDockIntent;
            }
        } else if (i != 2) {
            if (i == 6) {
                int dockMode = this.mDefaultDisplayPolicy.getDockMode();
                if (dockMode == 1 || dockMode == 4 || dockMode == 3) {
                    intent = this.mDeskDockIntent;
                }
            } else if (i == 7) {
                intent = this.mVrHeadsetHomeIntent;
            }
        }
        if (intent == null) {
            return null;
        }
        ActivityInfo ai = null;
        ResolveInfo info = this.mPackageManager.resolveActivityAsUser(intent, 65664, this.mCurrentUserId);
        if (info != null) {
            ai = info.activityInfo;
        }
        if (ai == null || ai.metaData == null || !ai.metaData.getBoolean("android.dock_home")) {
            return null;
        }
        Intent intent2 = new Intent(intent);
        intent2.setClassName(ai.packageName, ai.name);
        return intent2;
    }

    void startDockOrHome(int displayId, boolean fromHomeKey, boolean awakenFromDreams, String startReason) {
        try {
            ActivityManager.getService().stopAppSwitches();
        } catch (RemoteException e) {
        }
        sendCloseSystemWindows(SYSTEM_DIALOG_REASON_HOME_KEY);
        if (awakenFromDreams) {
            awakenDreams();
        }
        if (!this.mHasFeatureAuto && !isUserSetupComplete()) {
            Slog.i("WindowManager", "Not going home because user setup is in progress.");
            return;
        }
        Intent dock = createHomeDockIntent();
        if (dock != null) {
            if (fromHomeKey) {
                try {
                    dock.putExtra("android.intent.extra.FROM_HOME_KEY", fromHomeKey);
                } catch (ActivityNotFoundException e2) {
                }
            }
            startActivityAsUser(dock, UserHandle.CURRENT);
            return;
        }
        if (DEBUG_WAKEUP) {
            Log.d("WindowManager", "startDockOrHome: startReason= " + startReason);
        }
        this.mActivityTaskManagerInternal.startHomeOnDisplay(this.mCurrentUserId, startReason, displayId, true, fromHomeKey);
    }

    void startDockOrHome(int displayId, boolean fromHomeKey, boolean awakenFromDreams) {
        startDockOrHome(displayId, fromHomeKey, awakenFromDreams, "startDockOrHome");
    }

    boolean goHome() {
        int result;
        if (!isUserSetupComplete()) {
            Slog.i("WindowManager", "Not going home because user setup is in progress.");
            return false;
        }
        try {
            if (SystemProperties.getInt("persist.sys.uts-test-mode", 0) == 1) {
                Log.d("WindowManager", "UTS-TEST-MODE");
            } else {
                ActivityManager.getService().stopAppSwitches();
                sendCloseSystemWindows();
                Intent dock = createHomeDockIntent();
                if (dock != null) {
                    int result2 = ActivityTaskManager.getService().startActivityAsUser((IApplicationThread) null, this.mContext.getOpPackageName(), this.mContext.getAttributionTag(), dock, dock.resolveTypeIfNeeded(this.mContext.getContentResolver()), (IBinder) null, (String) null, 0, 1, (ProfilerInfo) null, (Bundle) null, -2);
                    if (result2 == 1) {
                        return false;
                    }
                }
            }
            IActivityTaskManager service = ActivityTaskManager.getService();
            String opPackageName = this.mContext.getOpPackageName();
            String attributionTag = this.mContext.getAttributionTag();
            Intent intent = this.mHomeIntent;
            result = service.startActivityAsUser((IApplicationThread) null, opPackageName, attributionTag, intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), (IBinder) null, (String) null, 0, 1, (ProfilerInfo) null, (Bundle) null, -2);
        } catch (RemoteException e) {
        }
        return result != 1;
    }

    private boolean isTheaterModeEnabled() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "theater_mode_on", 0) == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean performHapticFeedback(int effectId, boolean always, String reason) {
        return performHapticFeedback(Process.myUid(), this.mContext.getOpPackageName(), effectId, always, reason);
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public boolean performHapticFeedback(int uid, String packageName, int effectId, boolean always, String reason) {
        VibrationEffect effect;
        if (this.mVibrator.hasVibrator() && (effect = getVibrationEffect(effectId)) != null) {
            VibrationAttributes attrs = getVibrationAttributes(effectId);
            if (always) {
                attrs = new VibrationAttributes.Builder(attrs).setFlags(2).build();
            }
            this.mVibrator.vibrate(uid, packageName, effect, reason, attrs);
            return true;
        }
        return false;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private VibrationEffect getVibrationEffect(int effectId) {
        switch (effectId) {
            case 0:
            case 14:
            case FrameworkStatsLog.MOBILE_BYTES_TRANSFER_BY_FG_BG /* 10003 */:
                return VibrationEffect.get(5);
            case 1:
            case 3:
            case 12:
            case 15:
            case 16:
                return VibrationEffect.get(0);
            case 4:
                break;
            case 5:
                return VibrationEffect.get(0);
            case 6:
            case 13:
                return VibrationEffect.get(2);
            case 7:
            case 8:
            case 10:
            case 11:
                return VibrationEffect.get(2, false);
            case 9:
                if (!this.mHapticTextHandleEnabled) {
                    return null;
                }
                break;
            case 17:
                return VibrationEffect.get(1);
            case FrameworkStatsLog.WIFI_BYTES_TRANSFER_BY_FG_BG /* 10001 */:
                long[] pattern = this.mSafeModeEnabledVibePattern;
                if (pattern.length == 0) {
                    return null;
                }
                if (pattern.length == 1) {
                    return VibrationEffect.createOneShot(pattern[0], -1);
                }
                return VibrationEffect.createWaveform(pattern, -1);
            case FrameworkStatsLog.MOBILE_BYTES_TRANSFER /* 10002 */:
                if (this.mVibrator.areAllPrimitivesSupported(4)) {
                    return VibrationEffect.startComposition().addPrimitive(4, 0.25f).addPrimitive(7, 1.0f, 50).compose();
                }
                return VibrationEffect.get(5);
            default:
                return null;
        }
        return VibrationEffect.get(21);
    }

    private VibrationAttributes getVibrationAttributes(int effectId) {
        switch (effectId) {
            case 14:
            case 15:
                return PHYSICAL_EMULATION_VIBRATION_ATTRIBUTES;
            case 18:
            case 19:
            case 20:
            case FrameworkStatsLog.MOBILE_BYTES_TRANSFER /* 10002 */:
            case FrameworkStatsLog.MOBILE_BYTES_TRANSFER_BY_FG_BG /* 10003 */:
                return HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES;
            default:
                return TOUCH_VIBRATION_ATTRIBUTES;
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void keepScreenOnStartedLw() {
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void keepScreenOnStoppedLw() {
        if (isKeyguardShowingAndNotOccluded()) {
            this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public boolean hasNavigationBar() {
        return this.mDefaultDisplayPolicy.hasNavigationBar();
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void setDismissImeOnBackKeyPressed(boolean newValue) {
        this.mDismissImeOnBackKeyPressed = newValue;
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void setCurrentUserLw(int newUserId) {
        this.mCurrentUserId = newUserId;
        ITranPhoneWindowManager.Instance().onSetCurrentUserLw(newUserId);
        KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
        if (keyguardServiceDelegate != null) {
            keyguardServiceDelegate.setCurrentUser(newUserId);
        }
        AccessibilityShortcutController accessibilityShortcutController = this.mAccessibilityShortcutController;
        if (accessibilityShortcutController != null) {
            accessibilityShortcutController.setCurrentUser(newUserId);
        }
        StatusBarManagerInternal statusBar = getStatusBarManagerInternal();
        if (statusBar != null) {
            statusBar.setCurrentUser(newUserId);
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void setSwitchingUser(boolean switching) {
        this.mKeyguardDelegate.setSwitchingUser(switching);
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(CompanionMessage.TYPE, this.mDefaultDisplayRotation.getUserRotationMode());
        proto.write(1159641169923L, this.mDefaultDisplayRotation.getUserRotation());
        proto.write(1159641169924L, this.mDefaultDisplayRotation.getCurrentAppOrientation());
        proto.write(1133871366149L, this.mDefaultDisplayPolicy.isScreenOnFully());
        proto.write(1133871366150L, this.mDefaultDisplayPolicy.isKeyguardDrawComplete());
        proto.write(1133871366151L, this.mDefaultDisplayPolicy.isWindowManagerDrawComplete());
        proto.write(1133871366156L, isKeyguardOccluded());
        proto.write(1133871366157L, this.mKeyguardOccludedChanged);
        proto.write(1133871366158L, this.mPendingKeyguardOccluded);
        KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
        if (keyguardServiceDelegate != null) {
            keyguardServiceDelegate.dumpDebug(proto, 1146756268052L);
        }
        proto.end(token);
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void dump(String prefix, PrintWriter pw, String[] args) {
        pw.print(prefix);
        pw.print("mSafeMode=");
        pw.print(this.mSafeMode);
        pw.print(" mSystemReady=");
        pw.print(this.mSystemReady);
        pw.print(" mSystemBooted=");
        pw.println(this.mSystemBooted);
        pw.print(prefix);
        pw.print("mCameraLensCoverState=");
        pw.println(WindowManagerPolicy.WindowManagerFuncs.cameraLensStateToString(this.mCameraLensCoverState));
        pw.print(prefix);
        pw.print("mWakeGestureEnabledSetting=");
        pw.println(this.mWakeGestureEnabledSetting);
        pw.print(prefix);
        pw.print("mUiMode=");
        pw.print(Configuration.uiModeToString(this.mUiMode));
        pw.print("mEnableCarDockHomeCapture=");
        pw.println(this.mEnableCarDockHomeCapture);
        pw.print(prefix);
        pw.print("mLidKeyboardAccessibility=");
        pw.print(this.mLidKeyboardAccessibility);
        pw.print(" mLidNavigationAccessibility=");
        pw.print(this.mLidNavigationAccessibility);
        pw.print(" getLidBehavior=");
        pw.println(lidBehaviorToString(getLidBehavior()));
        pw.print(prefix);
        pw.print("mLongPressOnBackBehavior=");
        pw.println(longPressOnBackBehaviorToString(this.mLongPressOnBackBehavior));
        pw.print(prefix);
        pw.print("mLongPressOnHomeBehavior=");
        pw.println(longPressOnHomeBehaviorToString(this.mLongPressOnHomeBehavior));
        pw.print(prefix);
        pw.print("mDoubleTapOnHomeBehavior=");
        pw.println(doubleTapOnHomeBehaviorToString(this.mDoubleTapOnHomeBehavior));
        pw.print(prefix);
        pw.print("mShortPressOnPowerBehavior=");
        pw.println(shortPressOnPowerBehaviorToString(this.mShortPressOnPowerBehavior));
        pw.print(prefix);
        pw.print("mLongPressOnPowerBehavior=");
        pw.println(longPressOnPowerBehaviorToString(this.mLongPressOnPowerBehavior));
        pw.print(prefix);
        pw.print("mLongPressOnPowerAssistantTimeoutMs=");
        pw.println(this.mLongPressOnPowerAssistantTimeoutMs);
        pw.print(prefix);
        pw.print("mVeryLongPressOnPowerBehavior=");
        pw.println(veryLongPressOnPowerBehaviorToString(this.mVeryLongPressOnPowerBehavior));
        pw.print(prefix);
        pw.print("mDoublePressOnPowerBehavior=");
        pw.println(multiPressOnPowerBehaviorToString(this.mDoublePressOnPowerBehavior));
        pw.print(prefix);
        pw.print("mTriplePressOnPowerBehavior=");
        pw.println(multiPressOnPowerBehaviorToString(this.mTriplePressOnPowerBehavior));
        pw.print(prefix);
        pw.print("mPowerVolUpBehavior=");
        pw.println(powerVolumeUpBehaviorToString(this.mPowerVolUpBehavior));
        pw.print(prefix);
        pw.print("mShortPressOnSleepBehavior=");
        pw.println(shortPressOnSleepBehaviorToString(this.mShortPressOnSleepBehavior));
        pw.print(prefix);
        pw.print("mShortPressOnWindowBehavior=");
        pw.println(shortPressOnWindowBehaviorToString(this.mShortPressOnWindowBehavior));
        pw.print(prefix);
        pw.print("mShortPressOnStemPrimaryBehavior=");
        pw.println(shortPressOnStemPrimaryBehaviorToString(this.mShortPressOnStemPrimaryBehavior));
        pw.print(prefix);
        pw.print("mDoublePressOnStemPrimaryBehavior=");
        pw.println(doublePressOnStemPrimaryBehaviorToString(this.mDoublePressOnStemPrimaryBehavior));
        pw.print(prefix);
        pw.print("mTriplePressOnStemPrimaryBehavior=");
        pw.println(triplePressOnStemPrimaryBehaviorToString(this.mTriplePressOnStemPrimaryBehavior));
        pw.print(prefix);
        pw.print("mLongPressOnStemPrimaryBehavior=");
        pw.println(longPressOnStemPrimaryBehaviorToString(this.mLongPressOnStemPrimaryBehavior));
        pw.print(prefix);
        pw.print("mAllowStartActivityForLongPressOnPowerDuringSetup=");
        pw.println(this.mAllowStartActivityForLongPressOnPowerDuringSetup);
        pw.print(prefix);
        pw.print("mHasSoftInput=");
        pw.print(this.mHasSoftInput);
        pw.print(" mHapticTextHandleEnabled=");
        pw.println(this.mHapticTextHandleEnabled);
        pw.print(prefix);
        pw.print("mDismissImeOnBackKeyPressed=");
        pw.print(this.mDismissImeOnBackKeyPressed);
        pw.print(" mIncallPowerBehavior=");
        pw.println(incallPowerBehaviorToString(this.mIncallPowerBehavior));
        pw.print(prefix);
        pw.print("mIncallBackBehavior=");
        pw.print(incallBackBehaviorToString(this.mIncallBackBehavior));
        pw.print(" mEndcallBehavior=");
        pw.println(endcallBehaviorToString(this.mEndcallBehavior));
        pw.print(prefix);
        pw.print("mDisplayHomeButtonHandlers=");
        for (int i = 0; i < this.mDisplayHomeButtonHandlers.size(); i++) {
            int key = this.mDisplayHomeButtonHandlers.keyAt(i);
            pw.println(this.mDisplayHomeButtonHandlers.get(key));
        }
        pw.print(prefix);
        pw.print("mKeyguardOccluded=");
        pw.print(isKeyguardOccluded());
        pw.print(" mKeyguardOccludedChanged=");
        pw.print(this.mKeyguardOccludedChanged);
        pw.print(" mPendingKeyguardOccluded=");
        pw.println(this.mPendingKeyguardOccluded);
        pw.print(prefix);
        pw.print("mAllowLockscreenWhenOnDisplays=");
        pw.print(!this.mAllowLockscreenWhenOnDisplays.isEmpty());
        pw.print(" mLockScreenTimeout=");
        pw.print(this.mLockScreenTimeout);
        pw.print(" mLockScreenTimerActive=");
        pw.println(this.mLockScreenTimerActive);
        this.mGlobalKeyManager.dump(prefix, pw);
        this.mKeyCombinationManager.dump(prefix, pw);
        this.mSingleKeyGestureDetector.dump(prefix, pw);
        MyWakeGestureListener myWakeGestureListener = this.mWakeGestureListener;
        if (myWakeGestureListener != null) {
            myWakeGestureListener.dump(pw, prefix);
        }
        BurnInProtectionHelper burnInProtectionHelper = this.mBurnInProtectionHelper;
        if (burnInProtectionHelper != null) {
            burnInProtectionHelper.dump(prefix, pw);
        }
        KeyguardServiceDelegate keyguardServiceDelegate = this.mKeyguardDelegate;
        if (keyguardServiceDelegate != null) {
            keyguardServiceDelegate.dump(prefix, pw);
        }
        pw.print(prefix);
        pw.println("Looper state:");
        this.mHandler.getLooper().dump(new PrintWriterPrinter(pw), prefix + "  ");
    }

    private static String endcallBehaviorToString(int behavior) {
        StringBuilder sb = new StringBuilder();
        if ((behavior & 1) != 0) {
            sb.append("home|");
        }
        if ((behavior & 2) != 0) {
            sb.append("sleep|");
        }
        int N = sb.length();
        if (N == 0) {
            return "<nothing>";
        }
        return sb.substring(0, N - 1);
    }

    private static String incallPowerBehaviorToString(int behavior) {
        if ((behavior & 2) != 0) {
            return "hangup";
        }
        return "sleep";
    }

    private static String incallBackBehaviorToString(int behavior) {
        if ((behavior & 1) != 0) {
            return "hangup";
        }
        return "<nothing>";
    }

    private static String longPressOnBackBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "LONG_PRESS_BACK_NOTHING";
            case 1:
                return "LONG_PRESS_BACK_GO_TO_VOICE_ASSIST";
            default:
                return Integer.toString(behavior);
        }
    }

    private static String longPressOnHomeBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "LONG_PRESS_HOME_NOTHING";
            case 1:
                return "LONG_PRESS_HOME_ALL_APPS";
            case 2:
                return "LONG_PRESS_HOME_ASSIST";
            case 3:
                return "LONG_PRESS_HOME_NOTIFICATION_PANEL";
            default:
                return Integer.toString(behavior);
        }
    }

    private static String doubleTapOnHomeBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "DOUBLE_TAP_HOME_NOTHING";
            case 1:
                return "DOUBLE_TAP_HOME_RECENT_SYSTEM_UI";
            case 2:
                return "DOUBLE_TAP_HOME_PIP_MENU";
            default:
                return Integer.toString(behavior);
        }
    }

    private static String shortPressOnPowerBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "SHORT_PRESS_POWER_NOTHING";
            case 1:
                return "SHORT_PRESS_POWER_GO_TO_SLEEP";
            case 2:
                return "SHORT_PRESS_POWER_REALLY_GO_TO_SLEEP";
            case 3:
                return "SHORT_PRESS_POWER_REALLY_GO_TO_SLEEP_AND_GO_HOME";
            case 4:
                return "SHORT_PRESS_POWER_GO_HOME";
            case 5:
                return "SHORT_PRESS_POWER_CLOSE_IME_OR_GO_HOME";
            default:
                return Integer.toString(behavior);
        }
    }

    private static String longPressOnPowerBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "LONG_PRESS_POWER_NOTHING";
            case 1:
                return "LONG_PRESS_POWER_GLOBAL_ACTIONS";
            case 2:
                return "LONG_PRESS_POWER_SHUT_OFF";
            case 3:
                return "LONG_PRESS_POWER_SHUT_OFF_NO_CONFIRM";
            case 4:
                return "LONG_PRESS_POWER_GO_TO_VOICE_ASSIST";
            case 5:
                return "LONG_PRESS_POWER_ASSISTANT";
            default:
                return Integer.toString(behavior);
        }
    }

    private static String veryLongPressOnPowerBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "VERY_LONG_PRESS_POWER_NOTHING";
            case 1:
                return "VERY_LONG_PRESS_POWER_GLOBAL_ACTIONS";
            default:
                return Integer.toString(behavior);
        }
    }

    private static String powerVolumeUpBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "POWER_VOLUME_UP_BEHAVIOR_NOTHING";
            case 1:
                return "POWER_VOLUME_UP_BEHAVIOR_MUTE";
            case 2:
                return "POWER_VOLUME_UP_BEHAVIOR_GLOBAL_ACTIONS";
            default:
                return Integer.toString(behavior);
        }
    }

    private static String multiPressOnPowerBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "MULTI_PRESS_POWER_NOTHING";
            case 1:
                return "MULTI_PRESS_POWER_THEATER_MODE";
            case 2:
                return "MULTI_PRESS_POWER_BRIGHTNESS_BOOST";
            case 3:
                return "MULTI_PRESS_POWER_LAUNCH_TARGET_ACTIVITY";
            default:
                return Integer.toString(behavior);
        }
    }

    private static String shortPressOnSleepBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "SHORT_PRESS_SLEEP_GO_TO_SLEEP";
            case 1:
                return "SHORT_PRESS_SLEEP_GO_TO_SLEEP_AND_GO_HOME";
            default:
                return Integer.toString(behavior);
        }
    }

    private static String shortPressOnWindowBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "SHORT_PRESS_WINDOW_NOTHING";
            case 1:
                return "SHORT_PRESS_WINDOW_PICTURE_IN_PICTURE";
            default:
                return Integer.toString(behavior);
        }
    }

    private static String shortPressOnStemPrimaryBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "SHORT_PRESS_PRIMARY_NOTHING";
            case 1:
                return "SHORT_PRESS_PRIMARY_LAUNCH_ALL_APPS";
            default:
                return Integer.toString(behavior);
        }
    }

    private static String doublePressOnStemPrimaryBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "DOUBLE_PRESS_PRIMARY_NOTHING";
            case 1:
                return "DOUBLE_PRESS_PRIMARY_SWITCH_RECENT_APP";
            default:
                return Integer.toString(behavior);
        }
    }

    private static String triplePressOnStemPrimaryBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "TRIPLE_PRESS_PRIMARY_NOTHING";
            case 1:
                return "TRIPLE_PRESS_PRIMARY_TOGGLE_ACCESSIBILITY";
            default:
                return Integer.toString(behavior);
        }
    }

    private static String longPressOnStemPrimaryBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "LONG_PRESS_PRIMARY_NOTHING";
            case 1:
                return "LONG_PRESS_PRIMARY_LAUNCH_VOICE_ASSISTANT";
            default:
                return Integer.toString(behavior);
        }
    }

    private static String lidBehaviorToString(int behavior) {
        switch (behavior) {
            case 0:
                return "LID_BEHAVIOR_NONE";
            case 1:
                return "LID_BEHAVIOR_SLEEP";
            case 2:
                return "LID_BEHAVIOR_LOCK";
            default:
                return Integer.toString(behavior);
        }
    }

    public static boolean isLongPressToAssistantEnabled(Context context) {
        ContentResolver resolver = context.getContentResolver();
        int longPressToAssistant = Settings.System.getIntForUser(resolver, "clockwork_long_press_to_assistant_enabled", 1, -2);
        if (Log.isLoggable("WindowManager", 3)) {
            Log.d("WindowManager", "longPressToAssistant = " + longPressToAssistant);
        }
        return longPressToAssistant == 1;
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void setProximityPowerKeyDown(boolean powerKeyDown) {
        this.mIsProximityPowerKeyDown = powerKeyDown;
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public boolean isProximityPowerKeyDown() {
        return this.mIsProximityPowerKeyDown;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class HdmiVideoExtconUEventObserver extends ExtconStateObserver<Boolean> {
        private static final String HDMI_EXIST = "HDMI=1";
        private static final String NAME = "hdmi";

        private HdmiVideoExtconUEventObserver() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean init(ExtconUEventObserver.ExtconInfo hdmi) {
            boolean plugged = false;
            try {
                plugged = parseStateFromFile(hdmi).booleanValue();
            } catch (FileNotFoundException e) {
                Slog.w("WindowManager", hdmi.getStatePath() + " not found while attempting to determine initial state", e);
            } catch (IOException e2) {
                Slog.e("WindowManager", "Error reading " + hdmi.getStatePath() + " while attempting to determine initial state", e2);
            }
            startObserving(hdmi);
            return plugged;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // com.android.server.ExtconStateObserver
        public void updateState(ExtconUEventObserver.ExtconInfo extconInfo, String eventName, Boolean state) {
            PhoneWindowManager.this.mDefaultDisplayPolicy.setHdmiPlugged(state.booleanValue());
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // com.android.server.ExtconStateObserver
        public Boolean parseState(ExtconUEventObserver.ExtconInfo extconIfno, String state) {
            return Boolean.valueOf(state.contains(HDMI_EXIST));
        }
    }

    private void interceptEmergencyContactBroadcast(KeyEvent event) {
        if (!isUserSetupComplete()) {
            Slog.i("WindowManager", "Not trigger emergency contact because user setup is in progress.");
            return;
        }
        int keyCode = event.getKeyCode();
        int action = event.getAction();
        if (keyCode == 26 && action == 1) {
            sendEmergencyContact();
        }
    }

    public void sendEmergencyContact() {
        long[] jArr = this.mEmergHits;
        System.arraycopy(jArr, 1, jArr, 0, jArr.length - 1);
        long[] jArr2 = this.mEmergHits;
        jArr2[jArr2.length - 1] = SystemClock.uptimeMillis();
        if (this.mEmergHits[0] >= SystemClock.uptimeMillis() - MULTI_PRESS_POWER_KEY_EMERG_TIMEOUT) {
            if (!"IN".equals(SystemProperties.get("persist.sys.oobe_country"))) {
                Intent intent = new Intent("com.transsion.action.EMERGENCY_CONTACT_MODE");
                intent.addFlags(1073741824);
                this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                Slog.d("WindowManager", "emergencyContact  have send");
            }
            long[] jArr3 = this.mEmergHits;
            jArr3[0] = 0;
            jArr3[1] = 0;
            jArr3[2] = 0;
            jArr3[3] = 0;
            jArr3[4] = 0;
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void startedNotifyFaceunlock(int reason) {
        KeyguardServiceDelegate keyguardServiceDelegate;
        if (TRAN_FACEID_SUPPORT && (keyguardServiceDelegate = this.mKeyguardDelegate) != null) {
            keyguardServiceDelegate.onStartedNotifyFaceunlock(reason);
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void startFaceUnlock() {
        KeyguardServiceDelegate keyguardServiceDelegate;
        if (TRAN_FACEID_SUPPORT && (keyguardServiceDelegate = this.mKeyguardDelegate) != null && keyguardServiceDelegate.hasKeyguard()) {
            Slog.d("WindowManager", "start FaceUnlock...");
            this.mKeyguardDelegate.onStartFaceUnlock();
        }
    }

    private void resetLockoutAttemptDeadline(int userId) {
        LockPatternUtils lockPatternUtils = this.mLockPatternUtils;
        if (lockPatternUtils != null) {
            long deadline = lockPatternUtils.getLockoutAttemptDeadline(this.mCurrentUserId);
            Log.i("WindowManager", "The lockout attempt deadline is updated since powerkey is long pressed, and deadline=" + deadline);
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy
    public void setConnectScreenActive(boolean active, boolean powerOn) {
        ITranPhoneWindowManager.Instance().setConnectScreenActive(active);
        this.mInputManagerInternal.setInteractive(!active && powerOn);
        this.mInputManagerInternal.setConnectScreenActive(active);
        ITranInputMethodManagerService.Instance().setTouchFromScreen(!active);
    }
}
