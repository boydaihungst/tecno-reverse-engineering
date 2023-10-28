package com.android.server.power;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.SynchronousUserSwitchObserver;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.SensorManager;
import android.hardware.SystemSensorManager;
import android.hardware.devicestate.DeviceStateManager;
import android.hardware.display.AmbientDisplayConfiguration;
import android.hardware.display.DisplayManagerInternal;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.BatteryManagerInternal;
import android.os.BatterySaverPolicyConfig;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.IWakeLockCallback;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelDuration;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.PowerSaveState;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.Settings;
import android.service.dreams.DreamManagerInternal;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.sysprop.InitProperties;
import android.util.ArrayMap;
import android.util.KeyValueListParser;
import android.util.LongArray;
import android.util.MathUtils;
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayInfo;
import android.view.KeyEvent;
import com.android.internal.app.IBatteryStats;
import com.android.internal.display.BrightnessSynchronizer;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.LatencyTracker;
import com.android.internal.util.Preconditions;
import com.android.server.AdaptiveSmartNetworkService;
import com.android.server.EventLogTags;
import com.android.server.LockGuard;
import com.android.server.RescueParty;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.UiThread;
import com.android.server.UserspaceRebootLogger;
import com.android.server.Watchdog;
import com.android.server.am.BatteryStatsService;
import com.android.server.am.HostingRecord;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.job.controllers.JobStatus;
import com.android.server.lights.LightsManager;
import com.android.server.lights.LogicalLight;
import com.android.server.location.gnss.hal.GnssNative;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.power.LowPowerStandbyController;
import com.android.server.power.PowerGroup;
import com.android.server.power.batterysaver.BatterySaverController;
import com.android.server.power.batterysaver.BatterySaverPolicy;
import com.android.server.power.batterysaver.BatterySaverStateMachine;
import com.android.server.power.batterysaver.BatterySavingStats;
import com.mediatek.powerhalmgr.PowerHalMgr;
import com.mediatek.powerhalmgr.PowerHalMgrFactory;
import com.transsion.hubcore.griffin.ITranGriffinFeature;
import com.transsion.hubcore.griffin.lib.provider.TranStateListener;
import com.transsion.hubcore.server.eventtrack.ITranAIChargingEventTrackExt;
import com.transsion.hubcore.server.policy.ITranPhoneWindowManager;
import com.transsion.hubcore.server.power.ITranPowerManagerService;
import dalvik.annotation.optimization.NeverCompile;
import defpackage.CompanionAppsPermissions;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public final class PowerManagerService extends SystemService implements Watchdog.Monitor {
    private static final int BLACK_WAKE_LOCK_TIMEOUT_MS = 300000;
    private static final int CHECK_BLACK_WAKE_LOCK_DELAY = 315000;
    private static final int DEFAULT_DOUBLE_TAP_TO_WAKE = 0;
    private static final int DEFAULT_SCREEN_OFF_TIMEOUT = 15000;
    private static final int DEFAULT_SLEEP_TIMEOUT = -1;
    private static final int DIRTY_ACTUAL_DISPLAY_POWER_STATE_UPDATED = 8;
    private static final int DIRTY_ATTENTIVE = 16384;
    private static final int DIRTY_BATTERY_STATE = 256;
    private static final int DIRTY_BOOT_COMPLETED = 16;
    private static final int DIRTY_DISPLAY_GROUP_WAKEFULNESS = 65536;
    private static final int DIRTY_DOCK_STATE = 1024;
    private static final int DIRTY_IS_POWERED = 64;
    private static final int DIRTY_PROXIMITY_POSITIVE = 512;
    private static final int DIRTY_QUIESCENT = 4096;
    private static final int DIRTY_SCREEN_BRIGHTNESS_BOOST = 2048;
    private static final int DIRTY_SETTINGS = 32;
    private static final int DIRTY_STAY_ON = 128;
    private static final int DIRTY_USER_ACTIVITY = 4;
    private static final int DIRTY_VR_MODE_CHANGED = 8192;
    private static final int DIRTY_WAKEFULNESS = 2;
    private static final int DIRTY_WAKE_LOCKS = 1;
    private static final long ENHANCED_DISCHARGE_PREDICTION_BROADCAST_MIN_DELAY_MS = 60000;
    private static final long ENHANCED_DISCHARGE_PREDICTION_TIMEOUT_MS = 1800000;
    private static final int HALT_MODE_REBOOT = 1;
    private static final int HALT_MODE_REBOOT_SAFE_MODE = 2;
    private static final int HALT_MODE_SHUTDOWN = 0;
    private static final String HOLDING_DISPLAY_SUSPEND_BLOCKER = "holding display";
    private static final float INVALID_BRIGHTNESS_IN_CONFIG = -2.0f;
    static final long MIN_LONG_WAKE_CHECK_INTERVAL = 60000;
    private static final int MSG_ATTENTIVE_TIMEOUT = 5;
    private static final int MSG_CHECK_FOR_LONG_WAKELOCKS = 4;
    private static final int MSG_SANDMAN = 2;
    private static final int MSG_SCREEN_BRIGHTNESS_BOOST_TIMEOUT = 3;
    private static final int MSG_SCREEN_ON_BOOST = 101;
    private static final int MSG_USER_ACTIVITY_TIMEOUT = 1;
    private static final int MTKPOWER_HINT_ALWAYS_ENABLE = 268435455;
    private static final int MTKPOWER_HINT_SCREENOFF_LIMIT = 52;
    private static final int NOTIFY_HIDE_AOD = 3;
    private static final String REASON_BATTERY_THERMAL_STATE = "shutdown,thermal,battery";
    private static final String REASON_LOW_BATTERY = "shutdown,battery";
    private static final String REASON_REBOOT = "reboot";
    private static final String REASON_SHUTDOWN = "shutdown";
    private static final String REASON_THERMAL_SHUTDOWN = "shutdown,thermal";
    private static final String REASON_USERREQUESTED = "shutdown,userrequested";
    private static final int SCREEN_BRIGHTNESS_BOOST_TIMEOUT = 5000;
    private static final int SCREEN_ON_LATENCY_WARNING_MS = 200;
    private static final String SYSTEM_PROPERTY_QUIESCENT = "ro.boot.quiescent";
    private static final String SYSTEM_PROPERTY_REBOOT_REASON = "sys.boot.reason";
    private static final String SYSTEM_PROPERTY_RETAIL_DEMO_ENABLED = "sys.retaildemo.enabled";
    private static final String TAG = "PowerManagerService";
    static final String TRACE_SCREEN_ON = "Screen turning on";
    private static final String TRAN_AUDIOMIX_WAKELOCK = "AudioMix";
    static final int USER_ACTIVITY_SCREEN_BRIGHT = 1;
    static final int USER_ACTIVITY_SCREEN_DIM = 2;
    static final int USER_ACTIVITY_SCREEN_DREAM = 4;
    static final int WAKE_LOCK_BUTTON_BRIGHT = 8;
    static final int WAKE_LOCK_CPU = 1;
    static final int WAKE_LOCK_DOZE = 64;
    static final int WAKE_LOCK_DRAW = 128;
    static final int WAKE_LOCK_PROXIMITY_SCREEN_OFF = 16;
    static final int WAKE_LOCK_SCREEN_BRIGHT = 2;
    static final int WAKE_LOCK_SCREEN_DIM = 4;
    static final int WAKE_LOCK_STAY_AWAKE = 32;
    private static final int WAKE_LOCK_TIMEOUT_MS = 10000;
    private static PowerHalMgr mPowerHalService;
    private static boolean sQuiescent;
    private final int MSG_CHECK_BLACK_WAKELOCK;
    private final int MSG_CHECK_WAKELOCK;
    private AdaptiveSmartNetworkService adaptivesmartnetwork;
    private final Handler mAdaptiveSmartNetworkHandler;
    private boolean mAlwaysOnEnabled;
    private final AmbientDisplayConfiguration mAmbientDisplayConfiguration;
    private final AmbientDisplaySuppressionController mAmbientDisplaySuppressionController;
    private AppOpsManager mAppOpsManager;
    private final AttentionDetector mAttentionDetector;
    private LogicalLight mAttentionLight;
    private int mAttentiveTimeoutConfig;
    private long mAttentiveTimeoutSetting;
    private long mAttentiveWarningDurationConfig;
    private int mBatteryLevel;
    private boolean mBatteryLevelLow;
    private int mBatteryLevelWhenDreamStarted;
    private BatteryManagerInternal mBatteryManagerInternal;
    private final BatterySaverController mBatterySaverController;
    private final BatterySaverPolicy mBatterySaverPolicy;
    private final BatterySaverStateMachine mBatterySaverStateMachine;
    private final BatterySavingStats mBatterySavingStats;
    private IBatteryStats mBatteryStats;
    private final BinderService mBinderService;
    private boolean mBootCompleted;
    private final SuspendBlocker mBootingSuspendBlocker;
    private final Clock mClock;
    final Constants mConstants;
    private final Context mContext;
    private boolean mDecoupleHalAutoSuspendModeFromDisplayConfig;
    private boolean mDecoupleHalInteractiveModeFromDisplayConfig;
    private boolean mDeviceIdleMode;
    int[] mDeviceIdleTempWhitelist;
    int[] mDeviceIdleWhitelist;
    private int mDirty;
    private DisplayManagerInternal mDisplayManagerInternal;
    private final DisplayManagerInternal.DisplayPowerCallbacks mDisplayPowerCallbacks;
    private final SuspendBlocker mDisplaySuspendBlocker;
    private int mDockState;
    private boolean mDoubleTapWakeEnabled;
    private boolean mDozeAfterScreenOff;
    private int mDozeScreenBrightnessOverrideFromDreamManager;
    private float mDozeScreenBrightnessOverrideFromDreamManagerFloat;
    private int mDozeScreenStateOverrideFromDreamManager;
    private boolean mDozeStartInProgress;
    private boolean mDrawWakeLockOverrideFromSidekick;
    private DreamManagerInternal mDreamManager;
    private boolean mDreamsActivateOnDockSetting;
    private boolean mDreamsActivateOnSleepSetting;
    private boolean mDreamsActivatedOnDockByDefaultConfig;
    private boolean mDreamsActivatedOnSleepByDefaultConfig;
    private int mDreamsBatteryLevelDrainCutoffConfig;
    private int mDreamsBatteryLevelMinimumWhenNotPoweredConfig;
    private int mDreamsBatteryLevelMinimumWhenPoweredConfig;
    private boolean mDreamsEnabledByDefaultConfig;
    private boolean mDreamsEnabledOnBatteryConfig;
    private boolean mDreamsEnabledSetting;
    private boolean mDreamsSupportedConfig;
    private boolean mEnhancedDischargePredictionIsPersonalized;
    private long mEnhancedDischargeTimeElapsed;
    private final Object mEnhancedDischargeTimeLock;
    private final FaceDownDetector mFaceDownDetector;
    private String mFocusPackageName;
    private boolean mForceSuspendActive;
    private int mForegroundProfile;
    private boolean mHalAutoSuspendModeEnabled;
    private boolean mHalInteractiveModeEnabled;
    private final Handler mHandler;
    private final ServiceThread mHandlerThread;
    private WatchWakelockHandler mHandlerWatchWakelock;
    private boolean mHoldingBootingSuspendBlocker;
    private boolean mHoldingDisplaySuspendBlocker;
    private boolean mHoldingWakeLockSuspendBlocker;
    private final InattentiveSleepWarningController mInattentiveSleepWarningOverlayController;
    private final Injector mInjector;
    private boolean mInterceptedPowerKeyForProximity;
    private boolean mIsFaceDown;
    private boolean mIsNotifyScreenOn;
    private boolean mIsPowered;
    private boolean mIsVrModeEnabled;
    private long mLastEnhancedDischargeTimeUpdatedElapsed;
    private long mLastFlipTime;
    private int mLastGlobalSleepReason;
    private long mLastGlobalSleepTime;
    private int mLastGlobalWakeReason;
    private long mLastGlobalWakeTime;
    private long mLastInteractivePowerHintTime;
    private long mLastScreenBrightnessBoostTime;
    private int mLastUserActivityLogTime;
    private int mLastUserActivityNativeTime;
    private int mLastWakeLockLogTime;
    private long mLastWarningAboutUserActivityPermission;
    private boolean mLightDeviceIdleMode;
    private LightsManager mLightsManager;
    private final LocalService mLocalService;
    private final Object mLock;
    private boolean mLowPowerStandbyActive;
    int[] mLowPowerStandbyAllowlist;
    private final LowPowerStandbyController mLowPowerStandbyController;
    private long mMaximumScreenDimDurationConfig;
    private float mMaximumScreenDimRatioConfig;
    private long mMaximumScreenOffTimeoutFromDeviceAdmin;
    private long mMinimumScreenOffTimeoutConfig;
    private final NativeWrapper mNativeWrapper;
    private Notifier mNotifier;
    private long mNotifyDimDuration;
    private long mNotifyLongDispatched;
    private long mNotifyLongNextCheck;
    private long mNotifyLongScheduled;
    private long mOverriddenTimeout;
    private int mPlugType;
    String mPnpProp;
    private WindowManagerPolicy mPolicy;
    private final PowerGroupWakefulnessChangeListener mPowerGroupWakefulnessChangeListener;
    private final SparseArray<PowerGroup> mPowerGroups;
    private final SparseArray<ProfilePowerState> mProfilePowerState;
    private boolean mProximityPositive;
    private boolean mRequestWaitForNegativeProximity;
    private boolean mSandmanScheduled;
    private boolean mScreenBrightnessBoostInProgress;
    public final float mScreenBrightnessDefault;
    public final float mScreenBrightnessDefaultVr;
    public final float mScreenBrightnessDim;
    public final float mScreenBrightnessDoze;
    public final float mScreenBrightnessMaximum;
    public final float mScreenBrightnessMaximumVr;
    public final float mScreenBrightnessMinimum;
    public final float mScreenBrightnessMinimumVr;
    private int mScreenBrightnessModeSetting;
    private float mScreenBrightnessOverrideFromWindowManager;
    private final int mScreenLogInterval;
    private long mScreenOffTime;
    private long mScreenOffTimeoutSetting;
    private final ScreenUndimDetector mScreenUndimDetector;
    private SettingsObserver mSettingsObserver;
    private long mSleepTimeoutSetting;
    private TranStateListener mStateListener;
    private boolean mStayOn;
    private int mStayOnWhilePluggedInSetting;
    private boolean mSupportsDoubleTapWakeConfig;
    private final ArrayList<SuspendBlocker> mSuspendBlockers;
    private boolean mSuspendWhenScreenOffDueToProximityConfig;
    private final SystemPropertiesWrapper mSystemProperties;
    private boolean mSystemReady;
    private boolean mTheaterModeEnabled;
    private HandlerThread mThreadWatchWakelock;
    private final SparseArray<UidState> mUidState;
    private boolean mUidsChanged;
    private boolean mUidsChanging;
    private boolean mUpdatePowerStateInProgress;
    private long mUserActivityTimeoutOverrideFromWindowManager;
    private int mUserId;
    private boolean mUserInactiveOverrideFromWindowManager;
    private final IVrStateCallbacks mVrStateCallbacks;
    private final List<String> mWakeLockPkgs;
    private int mWakeLockSummary;
    private final SuspendBlocker mWakeLockSuspendBlocker;
    private final ArrayList<WakeLock> mWakeLocks;
    private boolean mWakeUpWhenPluggedOrUnpluggedConfig;
    private boolean mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig;
    private boolean mWakefulnessChanging;
    private int mWakefulnessRaw;
    private WirelessChargerDetector mWirelessChargerDetector;
    private static final boolean mIsLucidDisabled = SystemProperties.get("ro.lucid.disabled").equals("1");
    private static final boolean TRAN_LUCID_OPTIMIZATION_SUPPORT = "1".equals(SystemProperties.get("persist.transsion.lucid.optimization", "0"));
    private static final boolean TRAN_AOD_SUPPORT = "1".equals(SystemProperties.get("ro.vendor.mtk_aod_support", ""));
    private static boolean DEBUG = false;
    private static boolean DEBUG_SPEW = false;
    private static boolean sTranSlmSupport = "1".equals(SystemProperties.get("ro.sleep_master"));
    private static boolean sWakelockDebug = false;
    private static final boolean TRAN_AI_CHARGING_EVENT_TRACK_SUPPORT = "1".equals(SystemProperties.get("ro.tran_ai_charging_event_track"));
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    private static int powerHintHandl = 0;
    private static boolean mTranIsScreenOff = false;
    private static int mTranAuidoMixWLCount = 0;
    private static final boolean mIsPnPMgrSupport = SystemProperties.get("ro.vendor.pnpmgr.support").equals("1");
    private static final boolean mIsPnPScreenCtl = SystemProperties.get("ro.vendor.pnpmgr.screen.ctl").equals("enable");

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface Clock {
        long uptimeMillis();
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface HaltMode {
    }

    /* renamed from: -$$Nest$smnativeForceSuspend  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m6170$$Nest$smnativeForceSuspend() {
        return nativeForceSuspend();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeAcquireSuspendBlocker(String str);

    private static native boolean nativeForceSuspend();

    /* JADX INFO: Access modifiers changed from: private */
    public native void nativeInit();

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeReleaseSuspendBlocker(String str);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetAutoSuspend(boolean z);

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeSetPowerBoost(int i, int i2);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean nativeSetPowerMode(int i, boolean z);

    static native void setLucidFactor(int i);

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class PowerGroupWakefulnessChangeListener implements PowerGroup.PowerGroupListener {
        private PowerGroupWakefulnessChangeListener() {
        }

        @Override // com.android.server.power.PowerGroup.PowerGroupListener
        public void onWakefulnessChangedLocked(int groupId, int wakefulness, long eventTime, int reason, int uid, int opUid, String opPackageName, String details) {
            if (wakefulness == 1) {
                int flags = reason != 13 ? 0 : 1;
                PowerManagerService powerManagerService = PowerManagerService.this;
                powerManagerService.userActivityNoUpdateLocked((PowerGroup) powerManagerService.mPowerGroups.get(groupId), eventTime, 0, flags, uid);
            }
            PowerManagerService.this.mDirty |= 65536;
            PowerManagerService.this.updateGlobalWakefulnessLocked(eventTime, reason, uid, opUid, opPackageName, details);
            PowerManagerService.this.mNotifier.onPowerGroupWakefulnessChanged(groupId, wakefulness, reason, PowerManagerService.this.getGlobalWakefulnessLocked());
            PowerManagerService.this.updatePowerStateLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class DisplayGroupPowerChangeListener implements DisplayManagerInternal.DisplayGroupListener {
        static final int DISPLAY_GROUP_ADDED = 0;
        static final int DISPLAY_GROUP_CHANGED = 2;
        static final int DISPLAY_GROUP_REMOVED = 1;

        private DisplayGroupPowerChangeListener() {
        }

        public void onDisplayGroupAdded(int groupId) {
            synchronized (PowerManagerService.this.mLock) {
                if (PowerManagerService.this.mPowerGroups.contains(groupId)) {
                    Slog.e(PowerManagerService.TAG, "Tried to add already existing group:" + groupId);
                    return;
                }
                boolean supportsSandman = groupId == 0;
                PowerGroup powerGroup = new PowerGroup(groupId, PowerManagerService.this.mPowerGroupWakefulnessChangeListener, PowerManagerService.this.mNotifier, PowerManagerService.this.mDisplayManagerInternal, 1, false, supportsSandman, PowerManagerService.this.mClock.uptimeMillis());
                PowerManagerService.this.mPowerGroups.append(groupId, powerGroup);
                PowerManagerService.this.onPowerGroupEventLocked(0, powerGroup);
            }
        }

        public void onDisplayGroupRemoved(int groupId) {
            synchronized (PowerManagerService.this.mLock) {
                if (groupId == 0) {
                    Slog.wtf(PowerManagerService.TAG, "Tried to remove default display group: " + groupId);
                } else if (!PowerManagerService.this.mPowerGroups.contains(groupId)) {
                    Slog.e(PowerManagerService.TAG, "Tried to remove non-existent group:" + groupId);
                } else {
                    PowerManagerService powerManagerService = PowerManagerService.this;
                    powerManagerService.onPowerGroupEventLocked(1, (PowerGroup) powerManagerService.mPowerGroups.get(groupId));
                }
            }
        }

        public void onDisplayGroupChanged(int groupId) {
            synchronized (PowerManagerService.this.mLock) {
                if (!PowerManagerService.this.mPowerGroups.contains(groupId)) {
                    Slog.e(PowerManagerService.TAG, "Tried to change non-existent group: " + groupId);
                    return;
                }
                PowerManagerService powerManagerService = PowerManagerService.this;
                powerManagerService.onPowerGroupEventLocked(2, (PowerGroup) powerManagerService.mPowerGroups.get(groupId));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class ForegroundProfileObserver extends SynchronousUserSwitchObserver {
        private ForegroundProfileObserver() {
        }

        public void onUserSwitching(int newUserId) throws RemoteException {
            synchronized (PowerManagerService.this.mLock) {
                PowerManagerService.this.mUserId = newUserId;
            }
        }

        public void onForegroundProfileSwitch(int newProfileId) throws RemoteException {
            long now = PowerManagerService.this.mClock.uptimeMillis();
            synchronized (PowerManagerService.this.mLock) {
                PowerManagerService.this.mForegroundProfile = newProfileId;
                PowerManagerService.this.maybeUpdateForegroundProfileLastActivityLocked(now);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class ProfilePowerState {
        long mLastUserActivityTime;
        boolean mLockingNotified;
        long mScreenOffTimeout;
        final int mUserId;
        int mWakeLockSummary;

        public ProfilePowerState(int userId, long screenOffTimeout, long now) {
            this.mUserId = userId;
            this.mScreenOffTimeout = screenOffTimeout;
            this.mLastUserActivityTime = now;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class Constants extends ContentObserver {
        private static final boolean DEFAULT_NO_CACHED_WAKE_LOCKS = true;
        private static final String KEY_NO_CACHED_WAKE_LOCKS = "no_cached_wake_locks";
        public boolean NO_CACHED_WAKE_LOCKS;
        private final KeyValueListParser mParser;
        private ContentResolver mResolver;

        public Constants(Handler handler) {
            super(handler);
            this.NO_CACHED_WAKE_LOCKS = true;
            this.mParser = new KeyValueListParser(',');
        }

        public void start(ContentResolver resolver) {
            this.mResolver = resolver;
            resolver.registerContentObserver(Settings.Global.getUriFor("power_manager_constants"), false, this);
            updateConstants();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            updateConstants();
        }

        private void updateConstants() {
            synchronized (PowerManagerService.this.mLock) {
                try {
                    this.mParser.setString(Settings.Global.getString(this.mResolver, "power_manager_constants"));
                } catch (IllegalArgumentException e) {
                    Slog.e(PowerManagerService.TAG, "Bad alarm manager settings", e);
                }
                this.NO_CACHED_WAKE_LOCKS = this.mParser.getBoolean(KEY_NO_CACHED_WAKE_LOCKS, true);
            }
        }

        void dump(PrintWriter pw) {
            pw.println("  Settings power_manager_constants:");
            pw.print("    ");
            pw.print(KEY_NO_CACHED_WAKE_LOCKS);
            pw.print("=");
            pw.println(this.NO_CACHED_WAKE_LOCKS);
        }

        void dumpProto(ProtoOutputStream proto) {
            long constantsToken = proto.start(1146756268033L);
            proto.write(1133871366145L, this.NO_CACHED_WAKE_LOCKS);
            proto.end(constantsToken);
        }
    }

    /* loaded from: classes2.dex */
    public static class NativeWrapper {
        public void nativeInit(PowerManagerService service) {
            service.nativeInit();
        }

        public void nativeAcquireSuspendBlocker(String name) {
            PowerManagerService.nativeAcquireSuspendBlocker(name);
        }

        public void nativeReleaseSuspendBlocker(String name) {
            PowerManagerService.nativeReleaseSuspendBlocker(name);
        }

        public void nativeSetAutoSuspend(boolean enable) {
            PowerManagerService.nativeSetAutoSuspend(enable);
        }

        public void nativeSetPowerBoost(int boost, int durationMs) {
            PowerManagerService.nativeSetPowerBoost(boost, durationMs);
        }

        public boolean nativeSetPowerMode(int mode, boolean enabled) {
            return PowerManagerService.nativeSetPowerMode(mode, enabled);
        }

        public boolean nativeForceSuspend() {
            return PowerManagerService.m6170$$Nest$smnativeForceSuspend();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Injector {
        Injector() {
        }

        Notifier createNotifier(Looper looper, Context context, IBatteryStats batteryStats, SuspendBlocker suspendBlocker, WindowManagerPolicy policy, FaceDownDetector faceDownDetector, ScreenUndimDetector screenUndimDetector, Executor backgroundExecutor) {
            return new Notifier(looper, context, batteryStats, suspendBlocker, policy, faceDownDetector, screenUndimDetector, backgroundExecutor);
        }

        SuspendBlocker createSuspendBlocker(PowerManagerService service, String name) {
            Objects.requireNonNull(service);
            SuspendBlockerImpl suspendBlockerImpl = new SuspendBlockerImpl(name);
            service.mSuspendBlockers.add(suspendBlockerImpl);
            return suspendBlockerImpl;
        }

        BatterySaverPolicy createBatterySaverPolicy(Object lock, Context context, BatterySavingStats batterySavingStats) {
            return new BatterySaverPolicy(lock, context, batterySavingStats);
        }

        BatterySaverController createBatterySaverController(Object lock, Context context, BatterySaverPolicy batterySaverPolicy, BatterySavingStats batterySavingStats) {
            return new BatterySaverController(lock, context, BackgroundThread.get().getLooper(), batterySaverPolicy, batterySavingStats);
        }

        BatterySaverStateMachine createBatterySaverStateMachine(Object lock, Context context, BatterySaverController batterySaverController) {
            return new BatterySaverStateMachine(lock, context, batterySaverController);
        }

        NativeWrapper createNativeWrapper() {
            return new NativeWrapper();
        }

        WirelessChargerDetector createWirelessChargerDetector(SensorManager sensorManager, SuspendBlocker suspendBlocker, Handler handler) {
            return new WirelessChargerDetector(sensorManager, suspendBlocker, handler);
        }

        AmbientDisplayConfiguration createAmbientDisplayConfiguration(Context context) {
            return new AmbientDisplayConfiguration(context);
        }

        AmbientDisplaySuppressionController createAmbientDisplaySuppressionController(Context context) {
            return new AmbientDisplaySuppressionController(context);
        }

        InattentiveSleepWarningController createInattentiveSleepWarningController() {
            return new InattentiveSleepWarningController();
        }

        public SystemPropertiesWrapper createSystemPropertiesWrapper() {
            return new SystemPropertiesWrapper() { // from class: com.android.server.power.PowerManagerService.Injector.1
                @Override // com.android.server.power.SystemPropertiesWrapper
                public String get(String key, String def) {
                    return SystemProperties.get(key, def);
                }

                @Override // com.android.server.power.SystemPropertiesWrapper
                public void set(String key, String val) {
                    SystemProperties.set(key, val);
                }
            };
        }

        Clock createClock() {
            return new Clock() { // from class: com.android.server.power.PowerManagerService$Injector$$ExternalSyntheticLambda1
                @Override // com.android.server.power.PowerManagerService.Clock
                public final long uptimeMillis() {
                    return SystemClock.uptimeMillis();
                }
            };
        }

        Handler createHandler(Looper looper, Handler.Callback callback) {
            return new Handler(looper, callback, true);
        }

        void invalidateIsInteractiveCaches() {
            PowerManager.invalidateIsInteractiveCaches();
        }

        LowPowerStandbyController createLowPowerStandbyController(Context context, Looper looper) {
            return new LowPowerStandbyController(context, looper, new LowPowerStandbyController.Clock() { // from class: com.android.server.power.PowerManagerService$Injector$$ExternalSyntheticLambda0
                @Override // com.android.server.power.LowPowerStandbyController.Clock
                public final long elapsedRealtime() {
                    return SystemClock.elapsedRealtime();
                }
            });
        }

        AppOpsManager createAppOpsManager(Context context) {
            return (AppOpsManager) context.getSystemService(AppOpsManager.class);
        }
    }

    public PowerManagerService(Context context) {
        this(context, new Injector());
    }

    /* JADX WARN: Removed duplicated region for block: B:46:0x031d A[Catch: all -> 0x033f, TryCatch #0 {, blocks: (B:32:0x02a9, B:34:0x02c3, B:36:0x02cb, B:37:0x02d3, B:39:0x02ec, B:44:0x0305, B:46:0x031d, B:47:0x0324, B:48:0x0327), top: B:54:0x02a9 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    PowerManagerService(Context context, Injector injector) {
        super(context);
        boolean z;
        this.MSG_CHECK_WAKELOCK = 1;
        this.MSG_CHECK_BLACK_WAKELOCK = 2;
        this.mFocusPackageName = "";
        this.mScreenOffTime = JobStatus.NO_LATEST_RUNTIME;
        this.mStateListener = new TranStateListener() { // from class: com.android.server.power.PowerManagerService.1
            @Override // com.transsion.hubcore.griffin.lib.provider.TranStateListener
            public void onAppLaunch(ActivityInfo current, ActivityInfo next, int nextPid, boolean pausing, int nextType) {
                if (current != null && next != null) {
                    String currPackName = current.getComponentName().getPackageName();
                    String nextPackName = next.getComponentName().getPackageName();
                    if (nextPackName != null && !nextPackName.equals(currPackName)) {
                        if (PowerManagerService.sWakelockDebug) {
                            Slog.d(PowerManagerService.TAG, String.format("onAppLaunch: currentPackage=%s, nextPackage=%s.", currPackName, nextPackName));
                        }
                        PowerManagerService.this.mFocusPackageName = nextPackName;
                    }
                }
            }

            @Override // com.transsion.hubcore.griffin.lib.provider.TranStateListener
            public void onScreenChanged(boolean status) {
                if (PowerManagerService.sWakelockDebug) {
                    Slog.d(PowerManagerService.TAG, "onScreenChanged " + status);
                }
                if (!status) {
                    PowerManagerService.this.mScreenOffTime = SystemClock.uptimeMillis();
                } else {
                    PowerManagerService.this.mScreenOffTime = JobStatus.NO_LATEST_RUNTIME;
                }
                super.onScreenChanged(status);
            }
        };
        this.adaptivesmartnetwork = null;
        Object installNewLock = LockGuard.installNewLock(1);
        this.mLock = installNewLock;
        this.mSuspendBlockers = new ArrayList<>();
        this.mWakeLocks = new ArrayList<>();
        this.mEnhancedDischargeTimeLock = new Object();
        this.mDockState = 0;
        this.mMaximumScreenOffTimeoutFromDeviceAdmin = JobStatus.NO_LATEST_RUNTIME;
        this.mIsFaceDown = false;
        this.mLastFlipTime = 0L;
        this.mScreenBrightnessOverrideFromWindowManager = Float.NaN;
        this.mOverriddenTimeout = -1L;
        this.mUserActivityTimeoutOverrideFromWindowManager = -1L;
        this.mDozeScreenStateOverrideFromDreamManager = 0;
        this.mDozeScreenBrightnessOverrideFromDreamManager = -1;
        this.mDozeScreenBrightnessOverrideFromDreamManagerFloat = Float.NaN;
        this.mLastWarningAboutUserActivityPermission = Long.MIN_VALUE;
        this.mDeviceIdleWhitelist = new int[0];
        this.mDeviceIdleTempWhitelist = new int[0];
        this.mLowPowerStandbyAllowlist = new int[0];
        this.mUidState = new SparseArray<>();
        this.mPowerGroups = new SparseArray<>();
        this.mPnpProp = null;
        this.mIsNotifyScreenOn = false;
        this.mNotifyDimDuration = BackupAgentTimeoutParameters.DEFAULT_QUOTA_EXCEEDED_TIMEOUT_MILLIS;
        this.mWakeLockPkgs = new ArrayList();
        this.mLastWakeLockLogTime = 0;
        this.mLastUserActivityLogTime = 0;
        this.mLastUserActivityNativeTime = 0;
        this.mScreenLogInterval = 1000;
        this.mProfilePowerState = new SparseArray<>();
        this.mDisplayPowerCallbacks = new DisplayManagerInternal.DisplayPowerCallbacks() { // from class: com.android.server.power.PowerManagerService.3
            public void onStateChanged() {
                synchronized (PowerManagerService.this.mLock) {
                    PowerManagerService.this.mDirty |= 8;
                    PowerManagerService.this.updatePowerStateLocked();
                }
            }

            public void onProximityPositive() {
                synchronized (PowerManagerService.this.mLock) {
                    PowerManagerService.this.mProximityPositive = true;
                    PowerManagerService.this.mDirty |= 512;
                    PowerManagerService.this.updatePowerStateLocked();
                }
            }

            public void onProximityNegative() {
                synchronized (PowerManagerService.this.mLock) {
                    PowerManagerService.this.mProximityPositive = false;
                    PowerManagerService.this.mInterceptedPowerKeyForProximity = false;
                    PowerManagerService.this.mDirty |= 512;
                    PowerManagerService powerManagerService = PowerManagerService.this;
                    powerManagerService.userActivityNoUpdateLocked((PowerGroup) powerManagerService.mPowerGroups.get(0), PowerManagerService.this.mClock.uptimeMillis(), 0, 0, 1000);
                    PowerManagerService.this.updatePowerStateLocked();
                }
            }

            public void onDisplayStateChange(boolean allInactive, boolean allOff) {
                synchronized (PowerManagerService.this.mLock) {
                    PowerManagerService.this.setPowerModeInternal(9, allInactive);
                    if (allOff) {
                        if (!PowerManagerService.this.mDecoupleHalInteractiveModeFromDisplayConfig) {
                            PowerManagerService.this.setHalInteractiveModeLocked(false);
                        }
                        if (!PowerManagerService.this.mDecoupleHalAutoSuspendModeFromDisplayConfig) {
                            PowerManagerService.this.setHalAutoSuspendModeLocked(true);
                        }
                    } else {
                        if (!PowerManagerService.this.mDecoupleHalAutoSuspendModeFromDisplayConfig) {
                            PowerManagerService.this.setHalAutoSuspendModeLocked(false);
                        }
                        if (!PowerManagerService.this.mDecoupleHalInteractiveModeFromDisplayConfig) {
                            PowerManagerService.this.setHalInteractiveModeLocked(true);
                        }
                    }
                }
            }

            public void acquireSuspendBlocker(String name) {
                PowerManagerService.this.mDisplaySuspendBlocker.acquire(name);
            }

            public void releaseSuspendBlocker(String name) {
                PowerManagerService.this.mDisplaySuspendBlocker.release(name);
            }
        };
        this.mVrStateCallbacks = new IVrStateCallbacks.Stub() { // from class: com.android.server.power.PowerManagerService.6
            public void onVrStateChanged(boolean enabled) {
                PowerManagerService.this.setPowerModeInternal(4, enabled);
                synchronized (PowerManagerService.this.mLock) {
                    if (PowerManagerService.this.mIsVrModeEnabled != enabled) {
                        PowerManagerService.this.setVrModeEnabled(enabled);
                        PowerManagerService.this.mDirty |= 8192;
                        PowerManagerService.this.updatePowerStateLocked();
                    }
                }
            }
        };
        this.mContext = context;
        this.mBinderService = new BinderService();
        this.mLocalService = new LocalService();
        NativeWrapper createNativeWrapper = injector.createNativeWrapper();
        this.mNativeWrapper = createNativeWrapper;
        SystemPropertiesWrapper createSystemPropertiesWrapper = injector.createSystemPropertiesWrapper();
        this.mSystemProperties = createSystemPropertiesWrapper;
        this.mClock = injector.createClock();
        this.mInjector = injector;
        ServiceThread serviceThread = new ServiceThread(TAG, -4, false);
        this.mHandlerThread = serviceThread;
        serviceThread.start();
        Handler createHandler = injector.createHandler(serviceThread.getLooper(), new PowerManagerHandlerCallback());
        this.mHandler = createHandler;
        this.mConstants = new Constants(createHandler);
        this.mAmbientDisplayConfiguration = injector.createAmbientDisplayConfiguration(context);
        this.mAmbientDisplaySuppressionController = injector.createAmbientDisplaySuppressionController(context);
        this.mAttentionDetector = new AttentionDetector(new Runnable() { // from class: com.android.server.power.PowerManagerService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                PowerManagerService.this.onUserAttention();
            }
        }, installNewLock);
        this.mFaceDownDetector = new FaceDownDetector(new Consumer() { // from class: com.android.server.power.PowerManagerService$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                PowerManagerService.this.onFlip(((Boolean) obj).booleanValue());
            }
        });
        this.mScreenUndimDetector = new ScreenUndimDetector();
        BatterySavingStats batterySavingStats = new BatterySavingStats(installNewLock);
        this.mBatterySavingStats = batterySavingStats;
        BatterySaverPolicy createBatterySaverPolicy = injector.createBatterySaverPolicy(installNewLock, context, batterySavingStats);
        this.mBatterySaverPolicy = createBatterySaverPolicy;
        BatterySaverController createBatterySaverController = injector.createBatterySaverController(installNewLock, context, createBatterySaverPolicy, batterySavingStats);
        this.mBatterySaverController = createBatterySaverController;
        this.mBatterySaverStateMachine = injector.createBatterySaverStateMachine(installNewLock, context, createBatterySaverController);
        this.mLowPowerStandbyController = injector.createLowPowerStandbyController(context, Looper.getMainLooper());
        this.mInattentiveSleepWarningOverlayController = injector.createInattentiveSleepWarningController();
        this.mAppOpsManager = injector.createAppOpsManager(context);
        this.mPowerGroupWakefulnessChangeListener = new PowerGroupWakefulnessChangeListener();
        ITranPowerManagerService.Instance().onConstruct(this, context);
        if (sTranSlmSupport) {
            Slog.d(TAG, " PowerManagerService. init watch wakelock start.");
            HandlerThread handlerThread = new HandlerThread("watch_wakelock", 10);
            this.mThreadWatchWakelock = handlerThread;
            handlerThread.start();
            this.mHandlerWatchWakelock = new WatchWakelockHandler(this.mThreadWatchWakelock.getLooper());
            Slog.d(TAG, "PowerManagerService. init watch wakelock end.");
        }
        float min = context.getResources().getFloat(17105109);
        float max = context.getResources().getFloat(17105108);
        float def = context.getResources().getFloat(17105104);
        float doze = context.getResources().getFloat(17105102);
        float dim = context.getResources().getFloat(17105101);
        if (min == INVALID_BRIGHTNESS_IN_CONFIG || max == INVALID_BRIGHTNESS_IN_CONFIG || def == INVALID_BRIGHTNESS_IN_CONFIG) {
            this.mScreenBrightnessMinimum = BrightnessSynchronizer.brightnessIntToFloat(context.getResources().getInteger(17694937));
            this.mScreenBrightnessMaximum = BrightnessSynchronizer.brightnessIntToFloat(context.getResources().getInteger(17694936));
            this.mScreenBrightnessDefault = BrightnessSynchronizer.brightnessIntToFloat(context.getResources().getInteger(17694935));
        } else {
            this.mScreenBrightnessMinimum = min;
            this.mScreenBrightnessMaximum = max;
            this.mScreenBrightnessDefault = def;
        }
        if (doze == INVALID_BRIGHTNESS_IN_CONFIG) {
            this.mScreenBrightnessDoze = BrightnessSynchronizer.brightnessIntToFloat(context.getResources().getInteger(17694931));
        } else {
            this.mScreenBrightnessDoze = doze;
        }
        if (dim == INVALID_BRIGHTNESS_IN_CONFIG) {
            this.mScreenBrightnessDim = BrightnessSynchronizer.brightnessIntToFloat(context.getResources().getInteger(17694930));
        } else {
            this.mScreenBrightnessDim = dim;
        }
        float vrMin = context.getResources().getFloat(17105107);
        float vrMax = context.getResources().getFloat(17105106);
        float vrDef = context.getResources().getFloat(17105105);
        if (vrMin == INVALID_BRIGHTNESS_IN_CONFIG || vrMax == INVALID_BRIGHTNESS_IN_CONFIG || vrDef == INVALID_BRIGHTNESS_IN_CONFIG) {
            this.mScreenBrightnessMinimumVr = BrightnessSynchronizer.brightnessIntToFloat(context.getResources().getInteger(17694934));
            this.mScreenBrightnessMaximumVr = BrightnessSynchronizer.brightnessIntToFloat(context.getResources().getInteger(17694933));
            this.mScreenBrightnessDefaultVr = BrightnessSynchronizer.brightnessIntToFloat(context.getResources().getInteger(17694932));
        } else {
            this.mScreenBrightnessMinimumVr = vrMin;
            this.mScreenBrightnessMaximumVr = vrMax;
            this.mScreenBrightnessDefaultVr = vrDef;
        }
        synchronized (installNewLock) {
            SuspendBlocker createSuspendBlocker = injector.createSuspendBlocker(this, "PowerManagerService.Booting");
            this.mBootingSuspendBlocker = createSuspendBlocker;
            this.mWakeLockSuspendBlocker = injector.createSuspendBlocker(this, "PowerManagerService.WakeLocks");
            SuspendBlocker createSuspendBlocker2 = injector.createSuspendBlocker(this, "PowerManagerService.Display");
            this.mDisplaySuspendBlocker = createSuspendBlocker2;
            if (createSuspendBlocker != null) {
                createSuspendBlocker.acquire();
                this.mHoldingBootingSuspendBlocker = true;
            }
            if (createSuspendBlocker2 != null) {
                createSuspendBlocker2.acquire(HOLDING_DISPLAY_SUSPEND_BLOCKER);
                this.mHoldingDisplaySuspendBlocker = true;
            }
            this.mHalAutoSuspendModeEnabled = false;
            this.mHalInteractiveModeEnabled = true;
            this.mWakefulnessRaw = 1;
            if (!createSystemPropertiesWrapper.get(SYSTEM_PROPERTY_QUIESCENT, "0").equals("1") && !((Boolean) InitProperties.userspace_reboot_in_progress().orElse(false)).booleanValue()) {
                z = false;
                sQuiescent = z;
                createNativeWrapper.nativeInit(this);
                createNativeWrapper.nativeSetAutoSuspend(false);
                createNativeWrapper.nativeSetPowerMode(7, true);
                createNativeWrapper.nativeSetPowerMode(0, false);
                injector.invalidateIsInteractiveCaches();
                if (sTranSlmSupport) {
                    ITranPowerManagerService.Instance().onPowerManagerExtSingleton();
                }
                injector.invalidateIsInteractiveCaches();
            }
            z = true;
            sQuiescent = z;
            createNativeWrapper.nativeInit(this);
            createNativeWrapper.nativeSetAutoSuspend(false);
            createNativeWrapper.nativeSetPowerMode(7, true);
            createNativeWrapper.nativeSetPowerMode(0, false);
            injector.invalidateIsInteractiveCaches();
            if (sTranSlmSupport) {
            }
            injector.invalidateIsInteractiveCaches();
        }
        this.adaptivesmartnetwork = new AdaptiveSmartNetworkService(context);
        Handler handler = new Handler();
        this.mAdaptiveSmartNetworkHandler = handler;
        handler.post(new Runnable() { // from class: com.android.server.power.PowerManagerService.2
            @Override // java.lang.Runnable
            public void run() {
                PowerManagerService.this.adaptivesmartnetwork.startListener();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onFlip(boolean isFaceDown) {
        long millisUntilNormalTimeout = 0;
        synchronized (this.mLock) {
            try {
                if (this.mBootCompleted) {
                    this.mIsFaceDown = isFaceDown;
                    if (isFaceDown) {
                        long currentTime = this.mClock.uptimeMillis();
                        this.mLastFlipTime = currentTime;
                        long sleepTimeout = getSleepTimeoutLocked(-1L);
                        long screenOffTimeout = getScreenOffTimeoutLocked(sleepTimeout, -1L);
                        PowerGroup powerGroup = this.mPowerGroups.get(0);
                        long millisUntilNormalTimeout2 = (powerGroup.getLastUserActivityTimeLocked() + screenOffTimeout) - currentTime;
                        try {
                            userActivityInternal(0, currentTime, 5, 0, 1000);
                            millisUntilNormalTimeout = millisUntilNormalTimeout2;
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                    if (isFaceDown) {
                        this.mFaceDownDetector.setMillisSaved(millisUntilNormalTimeout);
                    }
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("power", this.mBinderService, false, 9);
        publishLocalService(PowerManagerInternal.class, this.mLocalService);
        Watchdog.getInstance().addMonitor(this);
        Watchdog.getInstance().addThread(this.mHandler);
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 500) {
            systemReady();
        } else if (phase == 600) {
            incrementBootCount();
        } else if (phase == 1000) {
            synchronized (this.mLock) {
                long now = this.mClock.uptimeMillis();
                this.mBootCompleted = true;
                this.mDirty |= 16;
                this.mBatterySaverStateMachine.onBootCompleted();
                userActivityNoUpdateLocked(now, 0, 0, 1000);
                updatePowerStateLocked();
                if (sQuiescent) {
                    sleepPowerGroupLocked(this.mPowerGroups.get(0), this.mClock.uptimeMillis(), 10, 1000);
                }
                ((DeviceStateManager) this.mContext.getSystemService(DeviceStateManager.class)).registerCallback(new HandlerExecutor(this.mHandler), new DeviceStateListener());
            }
        }
    }

    private void systemReady() {
        synchronized (this.mLock) {
            this.mSystemReady = true;
            this.mDreamManager = (DreamManagerInternal) getLocalService(DreamManagerInternal.class);
            this.mDisplayManagerInternal = (DisplayManagerInternal) getLocalService(DisplayManagerInternal.class);
            this.mPolicy = (WindowManagerPolicy) getLocalService(WindowManagerPolicy.class);
            this.mBatteryManagerInternal = (BatteryManagerInternal) getLocalService(BatteryManagerInternal.class);
            this.mAttentionDetector.systemReady(this.mContext);
            SensorManager sensorManager = new SystemSensorManager(this.mContext, this.mHandler.getLooper());
            this.mBatteryStats = BatteryStatsService.getService();
            this.mNotifier = this.mInjector.createNotifier(Looper.getMainLooper(), this.mContext, this.mBatteryStats, this.mInjector.createSuspendBlocker(this, "PowerManagerService.Broadcasts"), this.mPolicy, this.mFaceDownDetector, this.mScreenUndimDetector, BackgroundThread.getExecutor());
            this.mPowerGroups.append(0, new PowerGroup(1, this.mPowerGroupWakefulnessChangeListener, this.mNotifier, this.mDisplayManagerInternal, this.mClock.uptimeMillis()));
            DisplayGroupPowerChangeListener displayGroupPowerChangeListener = new DisplayGroupPowerChangeListener();
            this.mDisplayManagerInternal.registerDisplayGroupListener(displayGroupPowerChangeListener);
            Injector injector = this.mInjector;
            this.mWirelessChargerDetector = injector.createWirelessChargerDetector(sensorManager, injector.createSuspendBlocker(this, "PowerManagerService.WirelessChargerDetector"), this.mHandler);
            this.mSettingsObserver = new SettingsObserver(this.mHandler);
            LightsManager lightsManager = (LightsManager) getLocalService(LightsManager.class);
            this.mLightsManager = lightsManager;
            this.mAttentionLight = lightsManager.getLight(5);
            this.mDisplayManagerInternal.initPowerManagement(this.mDisplayPowerCallbacks, this.mHandler, sensorManager);
            try {
                ForegroundProfileObserver observer = new ForegroundProfileObserver();
                ActivityManager.getService().registerUserSwitchObserver(observer, TAG);
            } catch (RemoteException e) {
            }
            this.mLowPowerStandbyController.systemReady();
            readConfigurationLocked();
            updateSettingsLocked();
            this.mDirty |= 256;
            updatePowerStateLocked();
        }
        ContentResolver resolver = this.mContext.getContentResolver();
        this.mConstants.start(resolver);
        this.mBatterySaverController.systemReady();
        this.mBatterySaverPolicy.systemReady();
        this.mFaceDownDetector.systemReady(this.mContext);
        this.mScreenUndimDetector.systemReady(this.mContext);
        resolver.registerContentObserver(Settings.Secure.getUriFor("screensaver_enabled"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.Secure.getUriFor("screensaver_activate_on_sleep"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.Secure.getUriFor("screensaver_activate_on_dock"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.System.getUriFor("screen_off_timeout"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.Secure.getUriFor("sleep_timeout"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.Secure.getUriFor("attentive_timeout"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.Global.getUriFor("stay_on_while_plugged_in"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.System.getUriFor("screen_brightness_mode"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.System.getUriFor("screen_auto_brightness_adj"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.Global.getUriFor("theater_mode_on"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.Secure.getUriFor("doze_always_on"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.Secure.getUriFor("double_tap_to_wake"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Settings.Global.getUriFor("device_demo_mode"), false, this.mSettingsObserver, 0);
        IVrManager vrManager = IVrManager.Stub.asInterface(getBinderService("vrmanager"));
        if (vrManager != null) {
            try {
                vrManager.registerListener(this.mVrStateCallbacks);
            } catch (RemoteException e2) {
                Slog.e(TAG, "Failed to register VR mode state listener: " + e2);
            }
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        filter.setPriority(1000);
        this.mContext.registerReceiver(new BatteryReceiver(), filter, null, this.mHandler);
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("android.intent.action.DREAMING_STARTED");
        filter2.addAction("android.intent.action.DREAMING_STOPPED");
        this.mContext.registerReceiver(new DreamReceiver(), filter2, null, this.mHandler);
        IntentFilter filter3 = new IntentFilter();
        filter3.addAction("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiver(new UserSwitchedReceiver(), filter3, null, this.mHandler);
        IntentFilter filter4 = new IntentFilter();
        filter4.addAction("android.intent.action.DOCK_EVENT");
        this.mContext.registerReceiver(new DockReceiver(), filter4, null, this.mHandler);
        if (sTranSlmSupport) {
            try {
                Slog.d(TAG, "power manager service ext init when system user unlocked.");
                ITranPowerManagerService.Instance().onPowerManagerExtInit();
            } catch (IllegalStateException e3) {
                Slog.d(TAG, "power manager service ext init fail.");
            }
            if (ITranGriffinFeature.Instance().getHooker() != null) {
                ITranGriffinFeature.Instance().getHooker().registerListener(this.mStateListener);
            }
        }
        if (TRAN_AI_CHARGING_EVENT_TRACK_SUPPORT) {
            Slog.i(TAG, "initial AI ChargingInit");
            ITranAIChargingEventTrackExt.Instance().init(this.mContext);
        }
    }

    void readConfigurationLocked() {
        Resources resources = this.mContext.getResources();
        this.mDecoupleHalAutoSuspendModeFromDisplayConfig = resources.getBoolean(17891724);
        this.mDecoupleHalInteractiveModeFromDisplayConfig = resources.getBoolean(17891725);
        this.mWakeUpWhenPluggedOrUnpluggedConfig = resources.getBoolean(17891805);
        this.mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig = resources.getBoolean(17891361);
        this.mSuspendWhenScreenOffDueToProximityConfig = resources.getBoolean(17891789);
        this.mAttentiveTimeoutConfig = resources.getInteger(17694736);
        this.mAttentiveWarningDurationConfig = resources.getInteger(17694737);
        this.mDreamsSupportedConfig = resources.getBoolean(17891620);
        this.mDreamsEnabledByDefaultConfig = resources.getBoolean(17891617);
        this.mDreamsActivatedOnSleepByDefaultConfig = resources.getBoolean(17891616);
        this.mDreamsActivatedOnDockByDefaultConfig = resources.getBoolean(17891615);
        this.mDreamsEnabledOnBatteryConfig = resources.getBoolean(17891618);
        this.mDreamsBatteryLevelMinimumWhenPoweredConfig = resources.getInteger(17694826);
        this.mDreamsBatteryLevelMinimumWhenNotPoweredConfig = resources.getInteger(17694825);
        this.mDreamsBatteryLevelDrainCutoffConfig = resources.getInteger(17694824);
        this.mDozeAfterScreenOff = resources.getBoolean(17891609);
        this.mMinimumScreenOffTimeoutConfig = resources.getInteger(17694879);
        this.mMaximumScreenDimDurationConfig = resources.getInteger(17694874);
        this.mMaximumScreenDimRatioConfig = resources.getFraction(18022402, 1, 1);
        this.mSupportsDoubleTapWakeConfig = resources.getBoolean(17891770);
    }

    private void updateSettingsLocked() {
        ContentResolver resolver = this.mContext.getContentResolver();
        this.mDreamsEnabledSetting = Settings.Secure.getIntForUser(resolver, "screensaver_enabled", this.mDreamsEnabledByDefaultConfig ? 1 : 0, -2) != 0;
        this.mDreamsActivateOnSleepSetting = Settings.Secure.getIntForUser(resolver, "screensaver_activate_on_sleep", this.mDreamsActivatedOnSleepByDefaultConfig ? 1 : 0, -2) != 0;
        this.mDreamsActivateOnDockSetting = Settings.Secure.getIntForUser(resolver, "screensaver_activate_on_dock", this.mDreamsActivatedOnDockByDefaultConfig ? 1 : 0, -2) != 0;
        int screenOffTimeoutSetting = Settings.System.getIntForUser(resolver, "screen_off_timeout", 15000, -2);
        if (screenOffTimeoutSetting != this.mScreenOffTimeoutSetting) {
            Slog.i(TAG, "updateSettingsLocked SCREEN_OFF_TIMEOUT val=" + screenOffTimeoutSetting);
            this.mScreenOffTimeoutSetting = screenOffTimeoutSetting;
        }
        this.mSleepTimeoutSetting = Settings.Secure.getIntForUser(resolver, "sleep_timeout", -1, -2);
        this.mAttentiveTimeoutSetting = Settings.Secure.getIntForUser(resolver, "attentive_timeout", this.mAttentiveTimeoutConfig, -2);
        this.mStayOnWhilePluggedInSetting = Settings.Global.getInt(resolver, "stay_on_while_plugged_in", 1);
        this.mTheaterModeEnabled = Settings.Global.getInt(this.mContext.getContentResolver(), "theater_mode_on", 0) == 1;
        this.mAlwaysOnEnabled = this.mAmbientDisplayConfiguration.alwaysOnEnabled(-2);
        if (this.mSupportsDoubleTapWakeConfig) {
            boolean doubleTapWakeEnabled = Settings.Secure.getIntForUser(resolver, "double_tap_to_wake", 0, -2) != 0;
            if (doubleTapWakeEnabled != this.mDoubleTapWakeEnabled) {
                this.mDoubleTapWakeEnabled = doubleTapWakeEnabled;
                this.mNativeWrapper.nativeSetPowerMode(0, doubleTapWakeEnabled);
            }
        }
        String retailDemoValue = UserManager.isDeviceInDemoMode(this.mContext) ? "1" : "0";
        if (!retailDemoValue.equals(this.mSystemProperties.get(SYSTEM_PROPERTY_RETAIL_DEMO_ENABLED, null))) {
            this.mSystemProperties.set(SYSTEM_PROPERTY_RETAIL_DEMO_ENABLED, retailDemoValue);
        }
        this.mScreenBrightnessModeSetting = Settings.System.getIntForUser(resolver, "screen_brightness_mode", 0, -2);
        this.mDirty |= 32;
    }

    void handleSettingsChangedLocked() {
        updateSettingsLocked();
        updatePowerStateLocked();
    }

    private void tranScreenOffCpuLimit() {
        PowerHalMgr powerHalMgr;
        int i;
        if (mPowerHalService == null) {
            mPowerHalService = PowerHalMgrFactory.getInstance().makePowerHalMgr();
        }
        Slog.d(TAG, "tranScreenOffCpuLimit mTranIsScreenOff = " + mTranIsScreenOff + ", mTranAuidoMixWLCount = " + mTranAuidoMixWLCount);
        boolean z = mTranIsScreenOff;
        if (z && mTranAuidoMixWLCount == 0) {
            PowerHalMgr powerHalMgr2 = mPowerHalService;
            if (powerHalMgr2 != null) {
                powerHintHandl = powerHalMgr2.perfCusLockHint(52, (int) MTKPOWER_HINT_ALWAYS_ENABLE);
                Slog.d(TAG, "tranScreenOffCpuLimit set cpu limit with handl = " + powerHintHandl);
            }
        } else if ((!z || mTranAuidoMixWLCount > 0) && (powerHalMgr = mPowerHalService) != null && -1 != (i = powerHintHandl)) {
            powerHalMgr.perfLockRelease(i);
            powerHintHandl = -1;
            Slog.d(TAG, "tranScreenOffCpuLimit release cpu limit");
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1839=7] */
    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:14:0x002f  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void acquireWakeLockInternal(IBinder lock, int displayId, int flags, String tag, String packageName, WorkSource ws, String historyTag, int uid, int pid, IWakeLockCallback callback) {
        boolean isCallerPrivileged;
        Object obj;
        Object obj2;
        boolean isCallerPrivileged2;
        boolean notifyAcquire;
        WakeLock wakeLock;
        UidState state;
        ApplicationInfo appInfo;
        boolean z;
        try {
            appInfo = this.mContext.getPackageManager().getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L));
        } catch (PackageManager.NameNotFoundException e) {
            isCallerPrivileged = false;
        }
        if (appInfo.uid == uid) {
            if (appInfo.isPrivilegedApp()) {
                z = true;
                boolean isCallerPrivileged3 = z;
                isCallerPrivileged = isCallerPrivileged3;
                obj = this.mLock;
                synchronized (obj) {
                    try {
                        if (displayId != -1) {
                            try {
                                DisplayInfo displayInfo = this.mSystemReady ? this.mDisplayManagerInternal.getDisplayInfo(displayId) : null;
                                if (displayInfo == null) {
                                    Slog.wtf(TAG, "Tried to acquire wake lock for invalid display: " + displayId);
                                } else if (!displayInfo.hasAccess(uid)) {
                                    throw new SecurityException("Caller does not have access to display");
                                }
                            } catch (Throwable th) {
                                th = th;
                                obj2 = obj;
                                throw th;
                            }
                        }
                        try {
                            if (DEBUG_SPEW) {
                                try {
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                                try {
                                    try {
                                        Slog.d(TAG, "acquireWakeLockInternal: lock=" + Objects.hashCode(lock) + ", flags=0x" + Integer.toHexString(flags) + ", tag=\"" + tag + "\", ws=" + ws + ", uid=" + uid + ", pid=" + pid);
                                    } catch (Throwable th3) {
                                        th = th3;
                                        obj2 = obj;
                                        throw th;
                                    }
                                } catch (Throwable th4) {
                                    th = th4;
                                    obj2 = obj;
                                    throw th;
                                }
                            }
                            int index = findWakeLockIndexLocked(lock);
                            try {
                                if (index >= 0) {
                                    try {
                                        WakeLock wakeLock2 = this.mWakeLocks.get(index);
                                        if (wakeLock2.hasSameProperties(flags, tag, ws, uid, pid, callback)) {
                                            obj2 = obj;
                                            isCallerPrivileged2 = isCallerPrivileged;
                                        } else {
                                            obj2 = obj;
                                            isCallerPrivileged2 = isCallerPrivileged;
                                            notifyWakeLockChangingLocked(wakeLock2, flags, tag, packageName, uid, pid, ws, historyTag, callback);
                                            wakeLock2.updateProperties(flags, tag, packageName, ws, historyTag, uid, pid, callback);
                                        }
                                        notifyAcquire = false;
                                        wakeLock = wakeLock2;
                                    } catch (Throwable th5) {
                                        th = th5;
                                        obj2 = obj;
                                        throw th;
                                    }
                                } else {
                                    obj2 = obj;
                                    isCallerPrivileged2 = isCallerPrivileged;
                                    try {
                                        UidState state2 = this.mUidState.get(uid);
                                        if (state2 == null) {
                                            UidState state3 = new UidState(uid);
                                            state3.mProcState = 20;
                                            this.mUidState.put(uid, state3);
                                            state = state3;
                                        } else {
                                            state = state2;
                                        }
                                        state.mNumWakeLocks++;
                                        try {
                                            wakeLock = new WakeLock(lock, displayId, flags, tag, packageName, ws, historyTag, uid, pid, state, callback);
                                            this.mWakeLocks.add(wakeLock);
                                            this.mWakeLockPkgs.add(packageName);
                                            setWakeLockDisabledStateLocked(wakeLock);
                                            notifyAcquire = true;
                                            if (sTranSlmSupport) {
                                                try {
                                                    wakeLock.mActiveSince = SystemClock.uptimeMillis();
                                                } catch (Throwable th6) {
                                                    th = th6;
                                                    throw th;
                                                }
                                            }
                                        } catch (Throwable th7) {
                                            th = th7;
                                            throw th;
                                        }
                                    } catch (Throwable th8) {
                                        th = th8;
                                        throw th;
                                    }
                                }
                                if (TRAN_AUDIOMIX_WAKELOCK.equals(wakeLock.mTag)) {
                                    int i = mTranAuidoMixWLCount + 1;
                                    mTranAuidoMixWLCount = i;
                                    if (i == 1) {
                                        tranScreenOffCpuLimit();
                                    }
                                }
                                if (sTranSlmSupport && this.mWakefulnessRaw == 0) {
                                    sendWakeLockCheckMessage(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                                }
                                applyWakeLockFlagsOnAcquireLocked(wakeLock, isCallerPrivileged2);
                                this.mDirty |= 1;
                                updatePowerStateLocked();
                                if (notifyAcquire) {
                                    notifyWakeLockAcquiredLocked(wakeLock);
                                }
                                return;
                            } catch (Throwable th9) {
                                th = th9;
                            }
                        } catch (Throwable th10) {
                            th = th10;
                            obj2 = obj;
                        }
                    } catch (Throwable th11) {
                        th = th11;
                    }
                }
                return;
            }
        }
        z = false;
        boolean isCallerPrivileged32 = z;
        isCallerPrivileged = isCallerPrivileged32;
        obj = this.mLock;
        synchronized (obj) {
        }
    }

    private void sendWakeLockCheckMessage(long delayMills) {
        this.mHandlerWatchWakelock.removeMessages(1);
        Message msg = this.mHandlerWatchWakelock.obtainMessage(1);
        this.mHandlerWatchWakelock.sendMessageDelayed(msg, delayMills);
    }

    private static boolean isScreenLock(WakeLock wakeLock) {
        switch (wakeLock.mFlags & GnssNative.GNSS_AIDING_TYPE_ALL) {
            case 6:
            case 10:
            case 26:
                return true;
            default:
                return false;
        }
    }

    private static WorkSource.WorkChain getFirstNonEmptyWorkChain(WorkSource workSource) {
        if (workSource.getWorkChains() == null) {
            return null;
        }
        for (WorkSource.WorkChain workChain : workSource.getWorkChains()) {
            if (workChain.getSize() > 0) {
                return workChain;
            }
        }
        return null;
    }

    private boolean isAcquireCausesWakeupFlagAllowed(String opPackageName, int opUid, boolean isCallerPrivileged) {
        if (opPackageName == null) {
            return false;
        }
        if (isCallerPrivileged) {
            if (DEBUG_SPEW) {
                Slog.d(TAG, "Allowing device wake-up for privileged app, call attributed to " + opPackageName);
            }
            return true;
        } else if (this.mAppOpsManager.checkOpNoThrow(61, opUid, opPackageName) == 0) {
            if (DEBUG_SPEW) {
                Slog.d(TAG, "Allowing device wake-up for app with special access " + opPackageName);
            }
            return true;
        } else {
            if (DEBUG_SPEW) {
                Slog.d(TAG, "Not allowing device wake-up for " + opPackageName);
            }
            return false;
        }
    }

    private void applyWakeLockFlagsOnAcquireLocked(WakeLock wakeLock, boolean isCallerPrivileged) {
        String opPackageName;
        int opUid;
        int opUid2;
        String opPackageName2;
        if ((wakeLock.mFlags & 268435456) != 0 && isScreenLock(wakeLock)) {
            if (wakeLock.mWorkSource != null && !wakeLock.mWorkSource.isEmpty()) {
                WorkSource workSource = wakeLock.mWorkSource;
                WorkSource.WorkChain workChain = getFirstNonEmptyWorkChain(workSource);
                if (workChain != null) {
                    opPackageName2 = workChain.getAttributionTag();
                    opUid2 = workChain.getAttributionUid();
                } else {
                    String opPackageName3 = workSource.getPackageName(0) != null ? workSource.getPackageName(0) : wakeLock.mPackageName;
                    String str = opPackageName3;
                    opUid2 = workSource.getUid(0);
                    opPackageName2 = str;
                }
                opPackageName = opPackageName2;
                opUid = opUid2;
            } else {
                String opPackageName4 = wakeLock.mPackageName;
                opPackageName = opPackageName4;
                opUid = wakeLock.mOwnerUid;
            }
            Integer powerGroupId = wakeLock.getPowerGroupId();
            if (powerGroupId != null && isAcquireCausesWakeupFlagAllowed(opPackageName, opUid, isCallerPrivileged)) {
                if (powerGroupId.intValue() == -1) {
                    if (DEBUG_SPEW) {
                        Slog.d(TAG, "Waking up all power groups");
                    }
                    for (int idx = 0; idx < this.mPowerGroups.size(); idx++) {
                        wakePowerGroupLocked(this.mPowerGroups.valueAt(idx), this.mClock.uptimeMillis(), 2, wakeLock.mTag, opUid, opPackageName, opUid);
                    }
                } else if (this.mPowerGroups.contains(powerGroupId.intValue())) {
                    if (DEBUG_SPEW) {
                        Slog.d(TAG, "Waking up power group " + powerGroupId);
                    }
                    wakePowerGroupLocked(this.mPowerGroups.get(powerGroupId.intValue()), this.mClock.uptimeMillis(), 2, wakeLock.mTag, opUid, opPackageName, opUid);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void releaseWakeLockInternal(IBinder lock, int flags) {
        synchronized (this.mLock) {
            int index = findWakeLockIndexLocked(lock);
            if (index < 0) {
                if (DEBUG_SPEW) {
                    Slog.d(TAG, "releaseWakeLockInternal: lock=" + Objects.hashCode(lock) + " [not found], flags=0x" + Integer.toHexString(flags));
                }
                return;
            }
            WakeLock wakeLock = this.mWakeLocks.get(index);
            if (sTranSlmSupport) {
                wakeLock.mTotalTime = SystemClock.uptimeMillis() - wakeLock.mActiveSince;
            }
            if (DEBUG_SPEW) {
                Slog.d(TAG, "releaseWakeLockInternal: lock=" + Objects.hashCode(lock) + " [" + wakeLock.mTag + "], flags=0x" + Integer.toHexString(flags));
            }
            if ((flags & 1) != 0) {
                this.mRequestWaitForNegativeProximity = true;
            }
            wakeLock.unlinkToDeath();
            wakeLock.setDisabled(true);
            removeWakeLockLocked(wakeLock, index);
            if (TRAN_AUDIOMIX_WAKELOCK.equals(wakeLock.mTag)) {
                int i = mTranAuidoMixWLCount - 1;
                mTranAuidoMixWLCount = i;
                if (i == 0) {
                    tranScreenOffCpuLimit();
                }
            }
            if (sTranSlmSupport && this.mWakefulnessRaw == 0) {
                boolean hasCheckMsg = this.mHandlerWatchWakelock.hasMessages(1);
                int numWakeLocks = this.mWakeLocks.size();
                if (numWakeLocks == 0 && hasCheckMsg) {
                    this.mHandlerWatchWakelock.removeMessages(1);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleWakeLockDeath(WakeLock wakeLock) {
        synchronized (this.mLock) {
            if (DEBUG_SPEW) {
                Slog.d(TAG, "handleWakeLockDeath: lock=" + Objects.hashCode(wakeLock.mLock) + " [" + wakeLock.mTag + "]");
            }
            int index = this.mWakeLocks.indexOf(wakeLock);
            if (index < 0) {
                return;
            }
            removeWakeLockLocked(wakeLock, index);
            if (sTranSlmSupport && this.mWakefulnessRaw == 0) {
                boolean hasCheckMsg = this.mHandlerWatchWakelock.hasMessages(1);
                int numWakeLocks = this.mWakeLocks.size();
                if (numWakeLocks == 0 && hasCheckMsg) {
                    this.mHandlerWatchWakelock.removeMessages(1);
                }
            }
        }
    }

    private void removeWakeLockLocked(WakeLock wakeLock, int index) {
        this.mWakeLocks.remove(index);
        try {
            this.mWakeLockPkgs.remove(index);
        } catch (Exception e) {
        }
        UidState state = wakeLock.mUidState;
        state.mNumWakeLocks--;
        if (state.mNumWakeLocks <= 0 && state.mProcState == 20) {
            this.mUidState.remove(state.mUid);
        }
        notifyWakeLockReleasedLocked(wakeLock);
        applyWakeLockFlagsOnReleaseLocked(wakeLock);
        this.mDirty |= 1;
        updatePowerStateLocked();
    }

    private void applyWakeLockFlagsOnReleaseLocked(WakeLock wakeLock) {
        if ((wakeLock.mFlags & 536870912) != 0 && isScreenLock(wakeLock)) {
            userActivityNoUpdateLocked(this.mClock.uptimeMillis(), 0, 1, wakeLock.mOwnerUid);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateWakeLockWorkSourceInternal(IBinder lock, WorkSource ws, String historyTag, int callingUid) {
        synchronized (this.mLock) {
            try {
                try {
                    int index = findWakeLockIndexLocked(lock);
                    try {
                        if (index >= 0) {
                            WakeLock wakeLock = this.mWakeLocks.get(index);
                            if (DEBUG_SPEW) {
                                Slog.d(TAG, "updateWakeLockWorkSourceInternal: lock=" + Objects.hashCode(lock) + " [" + wakeLock.mTag + "], ws=" + ws);
                            }
                            if (!wakeLock.hasSameWorkSource(ws)) {
                                notifyWakeLockChangingLocked(wakeLock, wakeLock.mFlags, wakeLock.mTag, wakeLock.mPackageName, wakeLock.mOwnerUid, wakeLock.mOwnerPid, ws, historyTag, null);
                                wakeLock.mHistoryTag = historyTag;
                                wakeLock.updateWorkSource(ws);
                            }
                            return;
                        }
                        try {
                            if (DEBUG_SPEW) {
                                Slog.d(TAG, "updateWakeLockWorkSourceInternal: lock=" + Objects.hashCode(lock) + " [not found], ws=" + ws);
                            }
                            if (sTranSlmSupport) {
                                Slog.d(TAG, "updateWakeLockWorkSourceInternal remove the wakelock");
                                return;
                            }
                            try {
                                throw new IllegalArgumentException("Wake lock not active: " + lock + " from uid " + callingUid);
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                    }
                } catch (Throwable th4) {
                    th = th4;
                }
            } catch (Throwable th5) {
                th = th5;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateWakeLockCallbackInternal(IBinder lock, IWakeLockCallback callback, int callingUid) {
        synchronized (this.mLock) {
            try {
                try {
                    int index = findWakeLockIndexLocked(lock);
                    if (index >= 0) {
                        WakeLock wakeLock = this.mWakeLocks.get(index);
                        if (DEBUG_SPEW) {
                            Slog.d(TAG, "updateWakeLockCallbackInternal: lock=" + Objects.hashCode(lock) + " [" + wakeLock.mTag + "]");
                        }
                        if (!isSameCallback(callback, wakeLock.mCallback)) {
                            notifyWakeLockChangingLocked(wakeLock, wakeLock.mFlags, wakeLock.mTag, wakeLock.mPackageName, wakeLock.mOwnerUid, wakeLock.mOwnerPid, wakeLock.mWorkSource, wakeLock.mHistoryTag, callback);
                            wakeLock.mCallback = callback;
                        }
                        return;
                    }
                    if (DEBUG_SPEW) {
                        Slog.d(TAG, "updateWakeLockCallbackInternal: lock=" + Objects.hashCode(lock) + " [not found]");
                    }
                    try {
                        throw new IllegalArgumentException("Wake lock not active: " + lock + " from uid " + callingUid);
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    private int findWakeLockIndexLocked(IBinder lock) {
        int count = this.mWakeLocks.size();
        for (int i = 0; i < count; i++) {
            if (this.mWakeLocks.get(i).mLock == lock) {
                return i;
            }
        }
        return -1;
    }

    WakeLock findWakeLockLocked(IBinder lock) {
        int index = findWakeLockIndexLocked(lock);
        if (index == -1) {
            return null;
        }
        return this.mWakeLocks.get(index);
    }

    private void notifyWakeLockAcquiredLocked(WakeLock wakeLock) {
        if (this.mSystemReady && !wakeLock.mDisabled) {
            wakeLock.mNotifiedAcquired = true;
            this.mNotifier.onWakeLockAcquired(wakeLock.mFlags, wakeLock.mTag, wakeLock.mPackageName, wakeLock.mOwnerUid, wakeLock.mOwnerPid, wakeLock.mWorkSource, wakeLock.mHistoryTag, wakeLock.mCallback);
            restartNofifyLongTimerLocked(wakeLock);
        }
    }

    private void enqueueNotifyLongMsgLocked(long time) {
        this.mNotifyLongScheduled = time;
        Message msg = this.mHandler.obtainMessage(4);
        msg.setAsynchronous(true);
        this.mHandler.sendMessageAtTime(msg, time);
    }

    private void restartNofifyLongTimerLocked(WakeLock wakeLock) {
        wakeLock.mAcquireTime = this.mClock.uptimeMillis();
        if ((wakeLock.mFlags & GnssNative.GNSS_AIDING_TYPE_ALL) == 1 && this.mNotifyLongScheduled == 0) {
            enqueueNotifyLongMsgLocked(wakeLock.mAcquireTime + 60000);
        }
    }

    private void notifyWakeLockLongStartedLocked(WakeLock wakeLock) {
        if (this.mSystemReady && !wakeLock.mDisabled) {
            wakeLock.mNotifiedLong = true;
            this.mNotifier.onLongPartialWakeLockStart(wakeLock.mTag, wakeLock.mOwnerUid, wakeLock.mWorkSource, wakeLock.mHistoryTag);
        }
    }

    private void notifyWakeLockLongFinishedLocked(WakeLock wakeLock) {
        if (wakeLock.mNotifiedLong) {
            wakeLock.mNotifiedLong = false;
            this.mNotifier.onLongPartialWakeLockFinish(wakeLock.mTag, wakeLock.mOwnerUid, wakeLock.mWorkSource, wakeLock.mHistoryTag);
        }
    }

    private void notifyWakeLockChangingLocked(WakeLock wakeLock, int flags, String tag, String packageName, int uid, int pid, WorkSource ws, String historyTag, IWakeLockCallback callback) {
        if (this.mSystemReady && wakeLock.mNotifiedAcquired) {
            this.mNotifier.onWakeLockChanging(wakeLock.mFlags, wakeLock.mTag, wakeLock.mPackageName, wakeLock.mOwnerUid, wakeLock.mOwnerPid, wakeLock.mWorkSource, wakeLock.mHistoryTag, wakeLock.mCallback, flags, tag, packageName, uid, pid, ws, historyTag, callback);
            notifyWakeLockLongFinishedLocked(wakeLock);
            restartNofifyLongTimerLocked(wakeLock);
        }
    }

    private void notifyWakeLockReleasedLocked(WakeLock wakeLock) {
        if (this.mSystemReady && wakeLock.mNotifiedAcquired) {
            wakeLock.mNotifiedAcquired = false;
            wakeLock.mAcquireTime = 0L;
            this.mNotifier.onWakeLockReleased(wakeLock.mFlags, wakeLock.mTag, wakeLock.mPackageName, wakeLock.mOwnerUid, wakeLock.mOwnerPid, wakeLock.mWorkSource, wakeLock.mHistoryTag, wakeLock.mCallback);
            notifyWakeLockLongFinishedLocked(wakeLock);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isWakeLockLevelSupportedInternal(int level) {
        synchronized (this.mLock) {
            boolean z = false;
            try {
                switch (level) {
                    case 1:
                    case 6:
                    case 10:
                    case 26:
                    case 64:
                    case 128:
                        return true;
                    case 32:
                        if (this.mSystemReady && this.mDisplayManagerInternal.isProximitySensorAvailable()) {
                            z = true;
                        }
                        return z;
                    default:
                        return false;
                }
            } finally {
            }
        }
    }

    private void userActivityFromNative(long eventTime, int event, int displayId, int flags) {
        if (Build.IS_DEBUG_ENABLE && this.mClock.uptimeMillis() - this.mLastUserActivityNativeTime >= 1000) {
            this.mLastUserActivityNativeTime = (int) this.mClock.uptimeMillis();
            Slog.i(TAG, "userActivityFromNative: eventTime=" + eventTime + " event=" + event + " flags=" + flags + " displayId=" + displayId);
        }
        userActivityInternal(displayId, eventTime, event, flags, 1000);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void userActivityInternal(int displayId, long eventTime, int event, int flags, int uid) {
        synchronized (this.mLock) {
            if (displayId == -1) {
                if (userActivityNoUpdateLocked(eventTime, event, flags, uid)) {
                    updatePowerStateLocked();
                }
                return;
            }
            DisplayInfo displayInfo = this.mDisplayManagerInternal.getDisplayInfo(displayId);
            if (displayInfo == null) {
                return;
            }
            int groupId = displayInfo.displayGroupId;
            if (groupId == -1) {
                return;
            }
            if (userActivityNoUpdateLocked(this.mPowerGroups.get(groupId), eventTime, event, flags, uid)) {
                updatePowerStateLocked();
            }
            if (groupId == 0) {
                this.mIsNotifyScreenOn = false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUserAttention() {
        synchronized (this.mLock) {
            if (userActivityNoUpdateLocked(this.mPowerGroups.get(0), this.mClock.uptimeMillis(), 4, 0, 1000)) {
                updatePowerStateLocked();
            }
        }
    }

    private boolean userActivityNoUpdateLocked(long eventTime, int event, int flags, int uid) {
        boolean updatePowerState = false;
        for (int idx = 0; idx < this.mPowerGroups.size(); idx++) {
            if (userActivityNoUpdateLocked(this.mPowerGroups.valueAt(idx), eventTime, event, flags, uid)) {
                updatePowerState = true;
            }
        }
        return updatePowerState;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2378=5] */
    /* JADX INFO: Access modifiers changed from: private */
    public boolean userActivityNoUpdateLocked(PowerGroup powerGroup, long eventTime, int event, int flags, int uid) {
        int groupId = powerGroup.getGroupId();
        if (DEBUG_SPEW) {
            Slog.d(TAG, "userActivityNoUpdateLocked: groupId=" + groupId + ", eventTime=" + eventTime + ", event=" + event + ", flags=0x" + Integer.toHexString(flags) + ", uid=" + uid);
        }
        if (eventTime < powerGroup.getLastSleepTimeLocked() || eventTime < powerGroup.getLastWakeTimeLocked() || !this.mSystemReady) {
            return false;
        }
        Trace.traceBegin(131072L, "userActivity");
        try {
            if (eventTime > this.mLastInteractivePowerHintTime) {
                setPowerBoostInternal(0, 0);
                this.mLastInteractivePowerHintTime = eventTime;
            }
            this.mNotifier.onUserActivity(powerGroup.getGroupId(), event, uid);
            this.mAttentionDetector.onUserActivity(eventTime, event);
            if (this.mUserInactiveOverrideFromWindowManager) {
                this.mUserInactiveOverrideFromWindowManager = false;
                this.mOverriddenTimeout = -1L;
            }
            int wakefulness = powerGroup.getWakefulnessLocked();
            if (wakefulness != 0 && wakefulness != 3 && (flags & 2) == 0) {
                maybeUpdateForegroundProfileLastActivityLocked(eventTime);
                if ((flags & 1) != 0) {
                    if (eventTime > powerGroup.getLastUserActivityTimeNoChangeLightsLocked() && eventTime > powerGroup.getLastUserActivityTimeLocked()) {
                        powerGroup.setLastUserActivityTimeNoChangeLightsLocked(eventTime);
                        int i = this.mDirty | 4;
                        this.mDirty = i;
                        if (event == 1) {
                            this.mDirty = i | 4096;
                        }
                        return true;
                    }
                } else if (eventTime > powerGroup.getLastUserActivityTimeLocked()) {
                    powerGroup.setLastUserActivityTimeLocked(eventTime);
                    int i2 = this.mDirty | 4;
                    this.mDirty = i2;
                    if (event == 1) {
                        this.mDirty = i2 | 4096;
                    }
                    return true;
                }
                return false;
            }
            return false;
        } finally {
            Trace.traceEnd(131072L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeUpdateForegroundProfileLastActivityLocked(long eventTime) {
        ProfilePowerState profile = this.mProfilePowerState.get(this.mForegroundProfile);
        if (profile != null && eventTime > profile.mLastUserActivityTime) {
            profile.mLastUserActivityTime = eventTime;
        }
    }

    private void boostScreenOn(String state) {
        StringBuilder sb;
        FileWriter stateWriter = null;
        try {
            try {
                File file = new File("/sys/power/pnpmgr/ams/state");
                if (file.exists()) {
                    stateWriter = new FileWriter("/sys/power/pnpmgr/ams/state");
                } else {
                    stateWriter = new FileWriter("/sys/pnpmgr/ams/state");
                }
                stateWriter.write(state);
                try {
                    stateWriter.close();
                } catch (IOException e) {
                    e = e;
                    sb = new StringBuilder();
                    Slog.e(TAG, sb.append("close failed").append(e).toString());
                }
            } catch (Throwable th) {
                if (stateWriter != null) {
                    try {
                        stateWriter.close();
                    } catch (IOException e2) {
                        Slog.e(TAG, "close failed" + e2);
                    }
                }
                throw th;
            }
        } catch (IOException e3) {
            Slog.e(TAG, "boost for screen on failed" + e3);
            if (stateWriter != null) {
                try {
                    stateWriter.close();
                } catch (IOException e4) {
                    e = e4;
                    sb = new StringBuilder();
                    Slog.e(TAG, sb.append("close failed").append(e).toString());
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void wakePowerGroupLocked(PowerGroup powerGroup, long eventTime, int reason, String details, int uid, String opPackageName, int opUid) {
        if (DEBUG_SPEW) {
            Slog.d(TAG, "wakePowerGroupLocked: eventTime=" + eventTime + ", groupId=" + powerGroup.getGroupId() + ", uid=" + uid);
        }
        if (this.mForceSuspendActive || !this.mSystemReady) {
            return;
        }
        if (details != null && details.equals("android.policy:BIOMETRIC_OPTIMIZE")) {
            Slog.d(TAG, "wakePowerGroupLocked: startPreWakeupInProgress");
            ITranPhoneWindowManager.Instance().startPreWakeupInProgress(this.mPolicy, false);
        }
        DisplayManagerInternal displayManagerInternal = this.mDisplayManagerInternal;
        if (displayManagerInternal != null && this.mWakefulnessRaw != 1) {
            displayManagerInternal.onWakeDisplayCalled();
        }
        if (sTranSlmSupport) {
            this.mHandlerWatchWakelock.removeMessages(1);
            this.mHandlerWatchWakelock.removeMessages(2);
            ITranPowerManagerService.Instance().doWakePowerGroupLocked();
        }
        if (mIsPnPMgrSupport && mIsPnPScreenCtl) {
            ITranPowerManagerService.Instance().boostScreenOnLocked("1");
            Message msg = this.mHandler.obtainMessage(101);
            msg.setAsynchronous(true);
            this.mHandler.sendMessageDelayed(msg, BackupAgentTimeoutParameters.DEFAULT_QUOTA_EXCEEDED_TIMEOUT_MILLIS);
            Slog.d(TAG, "boost when screen on");
            if (this.mPnpProp == null) {
                SystemProperties.set("persist.sys.pnp.screen.optimize", "on");
                this.mPnpProp = "on";
            }
        }
        if (ITranPowerManagerService.Instance().getIsConnectSource() || ITranPowerManagerService.Instance().getIsConnectingState() || ITranPowerManagerService.Instance().getOldConnectingState()) {
            if (ITranPowerManagerService.Instance().getOldConnectingState()) {
                ITranPowerManagerService.Instance().setOldConnectingState(false);
            }
            ITranPowerManagerService.Instance().setIsConnectSource(false);
            this.mPolicy.setConnectScreenActive(false, true);
        }
        mTranIsScreenOff = false;
        tranScreenOffCpuLimit();
        powerGroup.wakeUpLocked(eventTime, reason, details, uid, opPackageName, opUid, LatencyTracker.getInstance(this.mContext));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean dreamPowerGroupLocked(PowerGroup powerGroup, long eventTime, int uid) {
        if (DEBUG_SPEW) {
            Slog.d(TAG, "dreamPowerGroup: groupId=" + powerGroup.getGroupId() + ", eventTime=" + eventTime + ", uid=" + uid);
        }
        if (!this.mBootCompleted || !this.mSystemReady) {
            return false;
        }
        return powerGroup.dreamLocked(eventTime, uid);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean dozePowerGroupLocked(PowerGroup powerGroup, long eventTime, int reason, int uid) {
        if (DEBUG_SPEW) {
            Slog.d(TAG, "dozePowerGroup: eventTime=" + eventTime + ", groupId=" + powerGroup.getGroupId() + ", reason=" + reason + ", uid=" + uid);
        }
        if (!this.mSystemReady || !this.mBootCompleted) {
            return false;
        }
        if (sTranSlmSupport) {
            startCheckWakelock();
            startCheckBlackWakelock();
        }
        return powerGroup.dozeLocked(eventTime, uid, reason);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean sleepPowerGroupLocked(PowerGroup powerGroup, long eventTime, int reason, int uid) {
        if (DEBUG_SPEW) {
            Slog.d(TAG, "sleepPowerGroup: eventTime=" + eventTime + ", uid=" + uid);
        }
        if (this.mBootCompleted && this.mSystemReady) {
            if (reason == 8) {
                Slog.d(TAG, "go to sleep due to quick shutdown");
                this.mDirty |= 32;
            }
            if (sTranSlmSupport) {
                ITranPowerManagerService.Instance().disconnectCameraClient();
                ITranPowerManagerService.Instance().disconnectAudioClient();
            }
            if (sTranSlmSupport) {
                startCheckWakelock();
                startCheckBlackWakelock();
            }
            ITranPhoneWindowManager.Instance().stopPreWakeupInProgress(this.mPolicy);
            if (ITranPowerManagerService.Instance().getIsConnectSource() || ITranPowerManagerService.Instance().getIsConnectingState() || ITranPowerManagerService.Instance().getOldConnectingState()) {
                if (ITranPowerManagerService.Instance().getOldConnectingState()) {
                    ITranPowerManagerService.Instance().setOldConnectingState(false);
                }
                ITranPowerManagerService.Instance().setIsConnectSource(false);
                this.mPolicy.setConnectScreenActive(false, false);
            }
            mTranIsScreenOff = true;
            tranScreenOffCpuLimit();
            return powerGroup.sleepLocked(eventTime, uid, reason);
        }
        return false;
    }

    void setWakefulnessLocked(int groupId, int wakefulness, long eventTime, int uid, int reason, int opUid, String opPackageName, String details) {
        this.mPowerGroups.get(groupId).setWakefulnessLocked(wakefulness, eventTime, uid, reason, opUid, opPackageName, details);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateGlobalWakefulnessLocked(long eventTime, int reason, int uid, int opUid, String opPackageName, String details) {
        String traceMethodName;
        long j;
        int newWakefulness = recalculateGlobalWakefulnessLocked();
        int currentWakefulness = getGlobalWakefulnessLocked();
        if (currentWakefulness == newWakefulness) {
            return;
        }
        boolean z = true;
        switch (newWakefulness) {
            case 0:
                Slog.i(TAG, "Sleeping (uid " + uid + ")...");
                if (currentWakefulness != 3) {
                    this.mLastGlobalSleepTime = eventTime;
                    this.mLastGlobalSleepReason = reason;
                }
                traceMethodName = "reallyGoToSleep";
                break;
            case 1:
                Slog.i(TAG, "Waking up from " + PowerManagerInternal.wakefulnessToString(currentWakefulness) + " (uid=" + uid + ", reason=" + PowerManager.wakeReasonToString(reason) + ", details=" + details + ")...");
                this.mLastGlobalWakeTime = eventTime;
                this.mLastGlobalWakeReason = reason;
                traceMethodName = "wakeUp";
                break;
            case 2:
                Slog.i(TAG, "Nap time (uid " + uid + ")...");
                traceMethodName = "nap";
                break;
            case 3:
                Slog.i(TAG, "Going to sleep due to " + PowerManager.sleepReasonToString(reason) + " (uid " + uid + ")...");
                this.mLastGlobalSleepTime = eventTime;
                this.mLastGlobalSleepReason = reason;
                this.mDozeStartInProgress = true;
                traceMethodName = "goToSleep";
                break;
            case 4:
                Slog.i(TAG, "connectting ...");
                traceMethodName = "connectting";
                break;
            default:
                throw new IllegalArgumentException("Unexpected wakefulness: " + newWakefulness);
        }
        Trace.traceBegin(131072L, traceMethodName);
        try {
            this.mInjector.invalidateIsInteractiveCaches();
            this.mWakefulnessRaw = newWakefulness;
            this.mWakefulnessChanging = true;
            this.mDirty |= 2;
            boolean z2 = this.mDozeStartInProgress;
            if (newWakefulness != 3) {
                z = false;
            }
            this.mDozeStartInProgress = z2 & z;
            Notifier notifier = this.mNotifier;
            if (notifier != null) {
                notifier.onWakefulnessChangeStarted(newWakefulness, reason, eventTime);
            }
            this.mAttentionDetector.onWakefulnessChangeStarted(newWakefulness);
            try {
                switch (newWakefulness) {
                    case 0:
                    case 3:
                        j = 131072;
                        if (PowerManagerInternal.isInteractive(currentWakefulness)) {
                            int numWakeLocksCleared = 0;
                            int numWakeLocks = this.mWakeLocks.size();
                            for (int i = 0; i < numWakeLocks; i++) {
                                WakeLock wakeLock = this.mWakeLocks.get(i);
                                switch (wakeLock.mFlags & GnssNative.GNSS_AIDING_TYPE_ALL) {
                                    case 6:
                                    case 10:
                                    case 26:
                                        numWakeLocksCleared++;
                                        break;
                                }
                            }
                            EventLogTags.writePowerSleepRequested(numWakeLocksCleared);
                            break;
                        } else {
                            break;
                        }
                        break;
                    case 1:
                        j = 131072;
                        this.mNotifier.onWakeUp(reason, details, uid, opPackageName, opUid);
                        if (sQuiescent) {
                            this.mDirty |= 4096;
                            break;
                        }
                        break;
                    case 2:
                    default:
                        j = 131072;
                        break;
                }
                Trace.traceEnd(j);
            } catch (Throwable th) {
                th = th;
                Trace.traceEnd(131072L);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    int getGlobalWakefulnessLocked() {
        return this.mWakefulnessRaw;
    }

    int getWakefulnessLocked(int groupId) {
        return this.mPowerGroups.get(groupId).getWakefulnessLocked();
    }

    int recalculateGlobalWakefulnessLocked() {
        int deviceWakefulness = 0;
        for (int i = 0; i < this.mPowerGroups.size(); i++) {
            int wakefulness = this.mPowerGroups.valueAt(i).getWakefulnessLocked();
            if (wakefulness == 1) {
                return 1;
            }
            if (wakefulness == 4) {
                return 4;
            }
            if (wakefulness == 2 && (deviceWakefulness == 0 || deviceWakefulness == 3)) {
                deviceWakefulness = 2;
            } else if (wakefulness == 3 && deviceWakefulness == 0) {
                deviceWakefulness = 3;
            }
        }
        return deviceWakefulness;
    }

    void onPowerGroupEventLocked(int event, PowerGroup powerGroup) {
        int reason;
        int groupId = powerGroup.getGroupId();
        if (event == 1) {
            this.mPowerGroups.delete(groupId);
        }
        int oldWakefulness = getGlobalWakefulnessLocked();
        int newWakefulness = recalculateGlobalWakefulnessLocked();
        if (event == 0 && newWakefulness == 1) {
            userActivityNoUpdateLocked(powerGroup, this.mClock.uptimeMillis(), 0, 0, 1000);
        }
        if (oldWakefulness != newWakefulness) {
            int i = 11;
            switch (newWakefulness) {
                case 1:
                    if (event == 0) {
                        i = 10;
                    }
                    reason = i;
                    break;
                case 2:
                default:
                    reason = 0;
                    break;
                case 3:
                    if (event != 1) {
                        i = 12;
                    }
                    reason = i;
                    break;
            }
            updateGlobalWakefulnessLocked(this.mClock.uptimeMillis(), reason, 1000, 1000, this.mContext.getOpPackageName(), "groupId: " + groupId);
        }
        int reason2 = this.mDirty;
        this.mDirty = reason2 | 65536;
        updatePowerStateLocked();
    }

    private void logSleepTimeoutRecapturedLocked() {
        long now = this.mClock.uptimeMillis();
        long savedWakeTimeMs = this.mOverriddenTimeout - now;
        if (savedWakeTimeMs >= 0) {
            EventLogTags.writePowerSoftSleepRequested(savedWakeTimeMs);
            this.mOverriddenTimeout = -1L;
        }
    }

    private void finishWakefulnessChangeIfNeededLocked() {
        if (this.mWakefulnessChanging && areAllPowerGroupsReadyLocked()) {
            if (getGlobalWakefulnessLocked() == 3 && (this.mWakeLockSummary & 64) == 0) {
                return;
            }
            this.mDozeStartInProgress = false;
            if (getGlobalWakefulnessLocked() == 3 || getGlobalWakefulnessLocked() == 0) {
                logSleepTimeoutRecapturedLocked();
            }
            this.mWakefulnessChanging = false;
            this.mNotifier.onWakefulnessChangeFinished();
        }
    }

    private boolean areAllPowerGroupsReadyLocked() {
        int size = this.mPowerGroups.size();
        for (int i = 0; i < size; i++) {
            if (!this.mPowerGroups.valueAt(i).isReadyLocked()) {
                return false;
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePowerStateLocked() {
        int dirtyPhase1;
        if (!this.mSystemReady || this.mDirty == 0 || this.mUpdatePowerStateInProgress) {
            return;
        }
        if (!Thread.holdsLock(this.mLock)) {
            Slog.wtf(TAG, "Power manager lock was not held when calling updatePowerStateLocked");
        }
        Trace.traceBegin(131072L, "updatePowerState");
        this.mUpdatePowerStateInProgress = true;
        try {
            updateIsPoweredLocked(this.mDirty);
            updateStayOnLocked(this.mDirty);
            updateScreenBrightnessBoostLocked(this.mDirty);
            long now = this.mClock.uptimeMillis();
            int dirtyPhase2 = 0;
            do {
                dirtyPhase1 = this.mDirty;
                dirtyPhase2 |= dirtyPhase1;
                this.mDirty = 0;
                updateWakeLockSummaryLocked(dirtyPhase1);
                updateUserActivitySummaryLocked(now, dirtyPhase1);
                updateAttentiveStateLocked(now, dirtyPhase1);
            } while (updateWakefulnessLocked(dirtyPhase1));
            updateProfilesLocked(now);
            boolean powerGroupsBecameReady = updatePowerGroupsLocked(dirtyPhase2);
            updateDreamLocked(dirtyPhase2, powerGroupsBecameReady);
            finishWakefulnessChangeIfNeededLocked();
            updateSuspendBlockerLocked();
        } finally {
            Trace.traceEnd(131072L);
            this.mUpdatePowerStateInProgress = false;
        }
    }

    private void updateProfilesLocked(long now) {
        int numProfiles = this.mProfilePowerState.size();
        for (int i = 0; i < numProfiles; i++) {
            ProfilePowerState profile = this.mProfilePowerState.valueAt(i);
            if (isProfileBeingKeptAwakeLocked(profile, now)) {
                profile.mLockingNotified = false;
            } else if (!profile.mLockingNotified) {
                profile.mLockingNotified = true;
                this.mNotifier.onProfileTimeout(profile.mUserId);
            }
        }
    }

    private boolean isProfileBeingKeptAwakeLocked(ProfilePowerState profile, long now) {
        return profile.mLastUserActivityTime + profile.mScreenOffTimeout > now || (profile.mWakeLockSummary & 32) != 0 || (this.mProximityPositive && (profile.mWakeLockSummary & 16) != 0);
    }

    private void updateIsPoweredLocked(int dirty) {
        int i;
        if ((dirty & 256) != 0) {
            boolean wasPowered = this.mIsPowered;
            int oldPlugType = this.mPlugType;
            this.mIsPowered = this.mBatteryManagerInternal.isPowered(15);
            this.mPlugType = this.mBatteryManagerInternal.getPlugType();
            this.mBatteryLevel = this.mBatteryManagerInternal.getBatteryLevel();
            this.mBatteryLevelLow = this.mBatteryManagerInternal.getBatteryLevelLow();
            if (DEBUG_SPEW) {
                Slog.d(TAG, "updateIsPoweredLocked: wasPowered=" + wasPowered + ", mIsPowered=" + this.mIsPowered + ", oldPlugType=" + oldPlugType + ", mPlugType=" + this.mPlugType + ", mBatteryLevel=" + this.mBatteryLevel);
            }
            boolean z = this.mIsPowered;
            if (wasPowered != z || oldPlugType != this.mPlugType) {
                this.mDirty |= 64;
                boolean dockedOnWirelessCharger = this.mWirelessChargerDetector.update(z, this.mPlugType);
                long now = this.mClock.uptimeMillis();
                if (!shouldWakeUpWhenPluggedOrUnpluggedLocked(wasPowered, oldPlugType, dockedOnWirelessCharger)) {
                    i = 0;
                } else {
                    i = 0;
                    wakePowerGroupLocked(this.mPowerGroups.get(0), now, 3, "android.server.power:PLUGGED:" + this.mIsPowered, 1000, this.mContext.getOpPackageName(), 1000);
                }
                userActivityNoUpdateLocked(this.mPowerGroups.get(i), now, 0, 0, 1000);
                if (this.mBootCompleted) {
                    if (this.mIsPowered && !BatteryManager.isPlugWired(oldPlugType) && BatteryManager.isPlugWired(this.mPlugType)) {
                        this.mNotifier.onWiredChargingStarted(this.mUserId);
                    } else if (dockedOnWirelessCharger) {
                        this.mNotifier.onWirelessChargingStarted(this.mBatteryLevel, this.mUserId);
                    }
                }
            }
            this.mBatterySaverStateMachine.setBatteryStatus(this.mIsPowered, this.mBatteryLevel, this.mBatteryLevelLow);
        }
    }

    private boolean shouldWakeUpWhenPluggedOrUnpluggedLocked(boolean wasPowered, int oldPlugType, boolean dockedOnWirelessCharger) {
        if (this.mWakeUpWhenPluggedOrUnpluggedConfig) {
            if (wasPowered && !this.mIsPowered && oldPlugType == 4) {
                return false;
            }
            if (wasPowered || !this.mIsPowered || this.mPlugType != 4 || dockedOnWirelessCharger) {
                if (this.mIsPowered && getGlobalWakefulnessLocked() == 2) {
                    return false;
                }
                if (!this.mTheaterModeEnabled || this.mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig) {
                    return (this.mAlwaysOnEnabled && getGlobalWakefulnessLocked() == 3) ? false : true;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    private void updateStayOnLocked(int dirty) {
        if ((dirty & 288) != 0) {
            boolean wasStayOn = this.mStayOn;
            if (this.mStayOnWhilePluggedInSetting != 0 && !isMaximumScreenOffTimeoutFromDeviceAdminEnforcedLocked()) {
                this.mStayOn = this.mBatteryManagerInternal.isPowered(this.mStayOnWhilePluggedInSetting);
            } else {
                this.mStayOn = false;
            }
            if (this.mStayOn != wasStayOn) {
                this.mDirty |= 128;
            }
        }
    }

    private void updateWakeLockSummaryLocked(int dirty) {
        if ((65539 & dirty) != 0) {
            this.mWakeLockSummary = 0;
            int numProfiles = this.mProfilePowerState.size();
            for (int i = 0; i < numProfiles; i++) {
                this.mProfilePowerState.valueAt(i).mWakeLockSummary = 0;
            }
            for (int idx = 0; idx < this.mPowerGroups.size(); idx++) {
                this.mPowerGroups.valueAt(idx).setWakeLockSummaryLocked(0);
            }
            int invalidGroupWakeLockSummary = 0;
            int numWakeLocks = this.mWakeLocks.size();
            for (int i2 = 0; i2 < numWakeLocks; i2++) {
                WakeLock wakeLock = this.mWakeLocks.get(i2);
                Integer groupId = wakeLock.getPowerGroupId();
                if (groupId != null && (groupId.intValue() == -1 || this.mPowerGroups.contains(groupId.intValue()))) {
                    PowerGroup powerGroup = this.mPowerGroups.get(groupId.intValue());
                    int wakeLockFlags = getWakeLockSummaryFlags(wakeLock);
                    this.mWakeLockSummary |= wakeLockFlags;
                    if (groupId.intValue() != -1) {
                        int wakeLockSummary = powerGroup.getWakeLockSummaryLocked();
                        powerGroup.setWakeLockSummaryLocked(wakeLockSummary | wakeLockFlags);
                    } else {
                        invalidGroupWakeLockSummary |= wakeLockFlags;
                    }
                    for (int j = 0; j < numProfiles; j++) {
                        ProfilePowerState profile = this.mProfilePowerState.valueAt(j);
                        if (wakeLockAffectsUser(wakeLock, profile.mUserId)) {
                            profile.mWakeLockSummary |= wakeLockFlags;
                        }
                    }
                }
            }
            for (int idx2 = 0; idx2 < this.mPowerGroups.size(); idx2++) {
                PowerGroup powerGroup2 = this.mPowerGroups.valueAt(idx2);
                int wakeLockSummary2 = adjustWakeLockSummary(powerGroup2.getWakefulnessLocked(), powerGroup2.getWakeLockSummaryLocked() | invalidGroupWakeLockSummary);
                powerGroup2.setWakeLockSummaryLocked(wakeLockSummary2);
            }
            int idx3 = getGlobalWakefulnessLocked();
            this.mWakeLockSummary = adjustWakeLockSummary(idx3, this.mWakeLockSummary);
            for (int i3 = 0; i3 < numProfiles; i3++) {
                ProfilePowerState profile2 = this.mProfilePowerState.valueAt(i3);
                profile2.mWakeLockSummary = adjustWakeLockSummary(getGlobalWakefulnessLocked(), profile2.mWakeLockSummary);
            }
            if (DEBUG_SPEW) {
                Slog.d(TAG, "updateWakeLockSummaryLocked: mWakefulness=" + PowerManagerInternal.wakefulnessToString(getGlobalWakefulnessLocked()) + ", mWakeLockSummary=0x" + Integer.toHexString(this.mWakeLockSummary));
            }
            if (Build.IS_DEBUG_ENABLE && this.mWakeLockSummary > 1 && this.mWakeLocks.size() > 0 && this.mClock.uptimeMillis() - this.mLastWakeLockLogTime >= 1000) {
                for (int i4 = 0; i4 < this.mWakeLocks.size(); i4++) {
                    WakeLock wakeLock2 = this.mWakeLocks.get(i4);
                    if ((wakeLock2.mFlags & 30) != 0) {
                        Slog.i(TAG, "updateWakeLockSummaryLocked: i=" + i4 + " pkg=" + wakeLock2.mPackageName + " flag=" + wakeLock2.mFlags + " uid=" + wakeLock2.mOwnerUid + " pid=" + wakeLock2.mOwnerPid);
                    }
                }
                this.mLastWakeLockLogTime = (int) this.mClock.uptimeMillis();
            }
        }
    }

    private static int adjustWakeLockSummary(int wakefulness, int wakeLockSummary) {
        if (wakefulness != 3) {
            wakeLockSummary &= -193;
        }
        if (wakefulness == 0 || (wakeLockSummary & 64) != 0) {
            wakeLockSummary &= -15;
            if (wakefulness == 0 || wakefulness == 3) {
                wakeLockSummary &= -17;
            }
        }
        if ((wakeLockSummary & 6) != 0) {
            if (wakefulness == 1) {
                wakeLockSummary |= 33;
            } else if (wakefulness == 2) {
                wakeLockSummary |= 1;
            }
        }
        if ((wakeLockSummary & 128) != 0) {
            return wakeLockSummary | 1;
        }
        return wakeLockSummary;
    }

    private int getWakeLockSummaryFlags(WakeLock wakeLock) {
        switch (wakeLock.mFlags & GnssNative.GNSS_AIDING_TYPE_ALL) {
            case 1:
                if (!wakeLock.mDisabled) {
                    return 1;
                }
                return 0;
            case 6:
                return 4;
            case 10:
                return 2;
            case 26:
                return 10;
            case 32:
                return 16;
            case 64:
                return 64;
            case 128:
                return 128;
            default:
                return 0;
        }
    }

    private boolean wakeLockAffectsUser(WakeLock wakeLock, int userId) {
        if (wakeLock.mWorkSource != null) {
            for (int k = 0; k < wakeLock.mWorkSource.size(); k++) {
                int uid = wakeLock.mWorkSource.getUid(k);
                if (userId == UserHandle.getUserId(uid)) {
                    return true;
                }
            }
            List<WorkSource.WorkChain> workChains = wakeLock.mWorkSource.getWorkChains();
            if (workChains != null) {
                for (int k2 = 0; k2 < workChains.size(); k2++) {
                    int uid2 = workChains.get(k2).getAttributionUid();
                    if (userId == UserHandle.getUserId(uid2)) {
                        return true;
                    }
                }
            }
        }
        return userId == UserHandle.getUserId(wakeLock.mOwnerUid);
    }

    void checkForLongWakeLocks() {
        synchronized (this.mLock) {
            long now = this.mClock.uptimeMillis();
            this.mNotifyLongDispatched = now;
            long when = now - 60000;
            long nextCheckTime = JobStatus.NO_LATEST_RUNTIME;
            int numWakeLocks = this.mWakeLocks.size();
            for (int i = 0; i < numWakeLocks; i++) {
                WakeLock wakeLock = this.mWakeLocks.get(i);
                if ((wakeLock.mFlags & GnssNative.GNSS_AIDING_TYPE_ALL) == 1 && wakeLock.mNotifiedAcquired && !wakeLock.mNotifiedLong) {
                    if (wakeLock.mAcquireTime >= when) {
                        long checkTime = wakeLock.mAcquireTime + 60000;
                        if (checkTime < nextCheckTime) {
                            nextCheckTime = checkTime;
                        }
                    } else {
                        notifyWakeLockLongStartedLocked(wakeLock);
                    }
                }
            }
            this.mNotifyLongScheduled = 0L;
            this.mHandler.removeMessages(4);
            if (nextCheckTime != JobStatus.NO_LATEST_RUNTIME) {
                this.mNotifyLongNextCheck = nextCheckTime;
                enqueueNotifyLongMsgLocked(nextCheckTime);
            } else {
                this.mNotifyLongNextCheck = 0L;
            }
        }
    }

    private void updateUserActivitySummaryLocked(long now, int dirty) {
        long nextTimeout;
        boolean userInactiveOverride;
        int idx;
        long sleepTimeout;
        int groupUserActivitySummary;
        long screenOffTimeout;
        long lastUserActivityTimeNoChangeLights;
        long nextTimeout2;
        long j = now;
        if ((dirty & 81959) == 0) {
            return;
        }
        this.mHandler.removeMessages(1);
        long attentiveTimeout = getAttentiveTimeoutLocked();
        long sleepTimeout2 = getSleepTimeoutLocked(attentiveTimeout);
        long screenOffTimeout2 = getScreenOffTimeoutLocked(sleepTimeout2, attentiveTimeout);
        long screenDimDuration = getScreenDimDurationLocked(screenOffTimeout2);
        long screenOffTimeout3 = getScreenOffTimeoutWithFaceDownLocked(screenOffTimeout2, screenDimDuration);
        boolean userInactiveOverride2 = this.mUserInactiveOverrideFromWindowManager;
        long lastUserActivityTime = -1;
        boolean hasUserActivitySummary = false;
        int idx2 = 0;
        while (true) {
            long attentiveTimeout2 = attentiveTimeout;
            if (idx2 >= this.mPowerGroups.size()) {
                break;
            }
            long groupNextTimeout = 0;
            PowerGroup powerGroup = this.mPowerGroups.valueAt(idx2);
            int wakefulness = powerGroup.getWakefulnessLocked();
            if (wakefulness != 0) {
                long nextTimeout3 = lastUserActivityTime;
                long lastUserActivityTime2 = powerGroup.getLastUserActivityTimeLocked();
                sleepTimeout = sleepTimeout2;
                long lastUserActivityTimeNoChangeLights2 = powerGroup.getLastUserActivityTimeNoChangeLightsLocked();
                if (lastUserActivityTime2 < powerGroup.getLastWakeTimeLocked()) {
                    idx = idx2;
                    groupUserActivitySummary = 0;
                } else {
                    if (ITranPowerManagerService.Instance().getIsConnectSource() && idx2 == 0) {
                        groupNextTimeout = ITranPowerManagerService.Instance().getConnectUserActivityTime();
                    } else if (this.mIsNotifyScreenOn && idx2 == 0) {
                        groupNextTimeout = lastUserActivityTime2;
                    } else {
                        groupNextTimeout = (lastUserActivityTime2 + screenOffTimeout3) - screenDimDuration;
                    }
                    if (j < groupNextTimeout) {
                        if ((this.mIsNotifyScreenOn && idx2 == 0) || (ITranPowerManagerService.Instance().getIsConnectSource() && idx2 == 0)) {
                            groupUserActivitySummary = 2;
                            idx = idx2;
                        } else {
                            groupUserActivitySummary = 1;
                            idx = idx2;
                        }
                    } else {
                        if (ITranPowerManagerService.Instance().getIsConnectSource() && idx2 == 0) {
                            groupNextTimeout = ITranPowerManagerService.Instance().getConnectUserActivityTime() + ITranPowerManagerService.Instance().getConnectDimDuration();
                            idx = idx2;
                        } else if (!this.mIsNotifyScreenOn || idx2 != 0) {
                            idx = idx2;
                            groupNextTimeout = lastUserActivityTime2 + screenOffTimeout3;
                        } else {
                            idx = idx2;
                            groupNextTimeout = this.mNotifyDimDuration + lastUserActivityTime2;
                        }
                        if (j >= groupNextTimeout) {
                            groupUserActivitySummary = 0;
                        } else {
                            groupUserActivitySummary = 2;
                        }
                    }
                }
                if (groupUserActivitySummary == 0 && lastUserActivityTimeNoChangeLights2 >= powerGroup.getLastWakeTimeLocked() && !ITranPowerManagerService.Instance().getIsConnectSource()) {
                    groupNextTimeout = lastUserActivityTimeNoChangeLights2 + screenOffTimeout3;
                    if (j < groupNextTimeout) {
                        if (powerGroup.isPolicyBrightLocked() || powerGroup.isPolicyVrLocked()) {
                            groupUserActivitySummary = 1;
                        } else if (powerGroup.isPolicyDimLocked()) {
                            groupUserActivitySummary = 2;
                        }
                    }
                }
                if (groupUserActivitySummary != 0) {
                    lastUserActivityTimeNoChangeLights = groupNextTimeout;
                } else if (sleepTimeout >= 0) {
                    long anyUserActivity = Math.max(lastUserActivityTime2, lastUserActivityTimeNoChangeLights2);
                    if (anyUserActivity >= powerGroup.getLastWakeTimeLocked()) {
                        groupNextTimeout = anyUserActivity + sleepTimeout;
                        if (j < groupNextTimeout) {
                            groupUserActivitySummary = 4;
                        }
                    }
                    lastUserActivityTimeNoChangeLights = groupNextTimeout;
                } else {
                    groupUserActivitySummary = 4;
                    lastUserActivityTimeNoChangeLights = -1;
                }
                if (groupUserActivitySummary == 4 || !userInactiveOverride2) {
                    userInactiveOverride = userInactiveOverride2;
                } else {
                    if ((groupUserActivitySummary & 3) == 0) {
                        userInactiveOverride = userInactiveOverride2;
                    } else {
                        userInactiveOverride = userInactiveOverride2;
                        if (this.mOverriddenTimeout == -1) {
                            this.mOverriddenTimeout = lastUserActivityTimeNoChangeLights;
                        }
                    }
                    groupUserActivitySummary = 4;
                    lastUserActivityTimeNoChangeLights = -1;
                }
                if ((groupUserActivitySummary & 1) != 0 && (powerGroup.getWakeLockSummaryLocked() & 32) == 0) {
                    lastUserActivityTimeNoChangeLights = this.mAttentionDetector.updateUserActivity(lastUserActivityTimeNoChangeLights, screenDimDuration);
                }
                if (isAttentiveTimeoutExpired(powerGroup, j)) {
                    lastUserActivityTimeNoChangeLights = -1;
                    groupUserActivitySummary = 0;
                }
                boolean hasUserActivitySummary2 = (groupUserActivitySummary != 0) | hasUserActivitySummary;
                if (nextTimeout3 == -1) {
                    nextTimeout2 = lastUserActivityTimeNoChangeLights;
                } else if (lastUserActivityTimeNoChangeLights == -1) {
                    nextTimeout2 = nextTimeout3;
                } else {
                    nextTimeout2 = Math.min(nextTimeout3, lastUserActivityTimeNoChangeLights);
                }
                if (idx == 0 && (groupUserActivitySummary & 4) != 0 && lastUserActivityTimeNoChangeLights < 0) {
                    this.mIsNotifyScreenOn = false;
                }
                lastUserActivityTime = nextTimeout2;
                hasUserActivitySummary = hasUserActivitySummary2;
                groupNextTimeout = lastUserActivityTimeNoChangeLights;
            } else {
                userInactiveOverride = userInactiveOverride2;
                idx = idx2;
                sleepTimeout = sleepTimeout2;
                long nextTimeout4 = lastUserActivityTime;
                if (idx == 0) {
                    this.mIsNotifyScreenOn = false;
                }
                lastUserActivityTime = nextTimeout4;
                groupUserActivitySummary = 0;
            }
            powerGroup.setUserActivitySummaryLocked(groupUserActivitySummary);
            if (DEBUG_SPEW) {
                Slog.d(TAG, "updateUserActivitySummaryLocked: groupId=" + powerGroup.getGroupId() + ", mWakefulness=" + PowerManagerInternal.wakefulnessToString(wakefulness) + ", mUserActivitySummary=0x" + Integer.toHexString(groupUserActivitySummary) + ", nextTimeout=" + TimeUtils.formatUptime(groupNextTimeout));
            }
            if (Build.IS_DEBUG_ENABLE && lastUserActivityTime == -1) {
                screenOffTimeout = screenOffTimeout3;
                if (this.mClock.uptimeMillis() - this.mLastUserActivityLogTime >= 1000) {
                    Slog.d(TAG, "updateUserActivitySummaryLocked: mWakefulness=" + PowerManagerInternal.wakefulnessToString(wakefulness) + ", mWakeLockSummary=0x" + Integer.toHexString(this.mWakeLockSummary) + ", groupUserActivitySummary=0x" + Integer.toHexString(groupUserActivitySummary) + ", nextTimeout=" + TimeUtils.formatUptime(lastUserActivityTime) + ", mStayOn=" + this.mStayOn + ", mScreenBrightnessBoostInProgress=" + this.mScreenBrightnessBoostInProgress + ", mProximityPositive=" + this.mProximityPositive + ", groupId=" + powerGroup.getGroupId());
                    this.mLastUserActivityLogTime = (int) this.mClock.uptimeMillis();
                }
            } else {
                screenOffTimeout = screenOffTimeout3;
            }
            idx2 = idx + 1;
            j = now;
            attentiveTimeout = attentiveTimeout2;
            screenOffTimeout3 = screenOffTimeout;
            sleepTimeout2 = sleepTimeout;
            userInactiveOverride2 = userInactiveOverride;
        }
        long nextTimeout5 = lastUserActivityTime;
        long nextProfileTimeout = getNextProfileTimeoutLocked(now);
        if (nextProfileTimeout <= 0) {
            nextTimeout = nextTimeout5;
        } else {
            nextTimeout = Math.min(nextTimeout5, nextProfileTimeout);
        }
        if (hasUserActivitySummary && nextTimeout >= 0) {
            scheduleUserInactivityTimeout(nextTimeout);
        }
    }

    private void scheduleUserInactivityTimeout(long timeMs) {
        Message msg = this.mHandler.obtainMessage(1);
        msg.setAsynchronous(true);
        this.mHandler.sendMessageAtTime(msg, timeMs);
    }

    private void scheduleAttentiveTimeout(long timeMs) {
        Message msg = this.mHandler.obtainMessage(5);
        msg.setAsynchronous(true);
        this.mHandler.sendMessageAtTime(msg, timeMs);
    }

    private long getNextProfileTimeoutLocked(long now) {
        long nextTimeout = -1;
        int numProfiles = this.mProfilePowerState.size();
        for (int i = 0; i < numProfiles; i++) {
            ProfilePowerState profile = this.mProfilePowerState.valueAt(i);
            long timeout = profile.mLastUserActivityTime + profile.mScreenOffTimeout;
            if (timeout > now && (nextTimeout == -1 || timeout < nextTimeout)) {
                nextTimeout = timeout;
            }
        }
        return nextTimeout;
    }

    private void updateAttentiveStateLocked(long now, int dirty) {
        long nextTimeout;
        long attentiveTimeout = getAttentiveTimeoutLocked();
        long goToSleepTime = this.mPowerGroups.get(0).getLastUserActivityTimeLocked() + attentiveTimeout;
        long showWarningTime = goToSleepTime - this.mAttentiveWarningDurationConfig;
        boolean warningDismissed = maybeHideInattentiveSleepWarningLocked(now, showWarningTime);
        if (attentiveTimeout >= 0) {
            if (!warningDismissed && (dirty & 19122) == 0) {
                return;
            }
            if (DEBUG_SPEW) {
                Slog.d(TAG, "Updating attentive state");
            }
            this.mHandler.removeMessages(5);
            if (isBeingKeptFromInattentiveSleepLocked()) {
                return;
            }
            if (now < showWarningTime) {
                nextTimeout = showWarningTime;
            } else if (now >= goToSleepTime) {
                nextTimeout = -1;
            } else {
                if (DEBUG) {
                    long timeToSleep = goToSleepTime - now;
                    Slog.d(TAG, "Going to sleep in " + timeToSleep + "ms if there is no user activity");
                }
                this.mInattentiveSleepWarningOverlayController.show();
                nextTimeout = goToSleepTime;
            }
            if (nextTimeout >= 0) {
                scheduleAttentiveTimeout(nextTimeout);
            }
        }
    }

    private boolean maybeHideInattentiveSleepWarningLocked(long now, long showWarningTime) {
        long attentiveTimeout = getAttentiveTimeoutLocked();
        if (this.mInattentiveSleepWarningOverlayController.isShown()) {
            if (getGlobalWakefulnessLocked() != 1) {
                this.mInattentiveSleepWarningOverlayController.dismiss(false);
                return true;
            } else if (attentiveTimeout < 0 || isBeingKeptFromInattentiveSleepLocked() || now < showWarningTime) {
                this.mInattentiveSleepWarningOverlayController.dismiss(true);
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    private boolean isAttentiveTimeoutExpired(PowerGroup powerGroup, long now) {
        long attentiveTimeout = getAttentiveTimeoutLocked();
        return powerGroup.getGroupId() == 0 && attentiveTimeout >= 0 && now >= powerGroup.getLastUserActivityTimeLocked() + attentiveTimeout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleUserActivityTimeout() {
        synchronized (this.mLock) {
            if (DEBUG_SPEW) {
                Slog.d(TAG, "handleUserActivityTimeout");
            }
            this.mDirty |= 4;
            updatePowerStateLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAttentiveTimeout() {
        synchronized (this.mLock) {
            if (DEBUG_SPEW) {
                Slog.d(TAG, "handleAttentiveTimeout");
            }
            this.mDirty |= 16384;
            updatePowerStateLocked();
        }
    }

    private long getAttentiveTimeoutLocked() {
        long timeout = this.mAttentiveTimeoutSetting;
        if (timeout <= 0) {
            return -1L;
        }
        return Math.max(timeout, this.mMinimumScreenOffTimeoutConfig);
    }

    private long getSleepTimeoutLocked(long attentiveTimeout) {
        long timeout = this.mSleepTimeoutSetting;
        if (timeout <= 0) {
            return -1L;
        }
        if (attentiveTimeout >= 0) {
            timeout = Math.min(timeout, attentiveTimeout);
        }
        return Math.max(timeout, this.mMinimumScreenOffTimeoutConfig);
    }

    private long getScreenOffTimeoutLocked(long sleepTimeout, long attentiveTimeout) {
        long timeout = this.mScreenOffTimeoutSetting;
        if (isMaximumScreenOffTimeoutFromDeviceAdminEnforcedLocked()) {
            timeout = Math.min(timeout, this.mMaximumScreenOffTimeoutFromDeviceAdmin);
        }
        long j = this.mUserActivityTimeoutOverrideFromWindowManager;
        if (j >= 0) {
            timeout = Math.min(timeout, j);
        }
        if (sleepTimeout >= 0) {
            timeout = Math.min(timeout, sleepTimeout);
        }
        if (attentiveTimeout >= 0) {
            timeout = Math.min(timeout, attentiveTimeout);
        }
        return Math.max(timeout, this.mMinimumScreenOffTimeoutConfig);
    }

    private long getScreenDimDurationLocked(long screenOffTimeout) {
        return Math.min(this.mMaximumScreenDimDurationConfig, ((float) screenOffTimeout) * this.mMaximumScreenDimRatioConfig);
    }

    private long getScreenOffTimeoutWithFaceDownLocked(long screenOffTimeout, long screenDimDuration) {
        if (this.mIsFaceDown) {
            return Math.min(screenDimDuration, screenOffTimeout);
        }
        return screenOffTimeout;
    }

    private boolean updateWakefulnessLocked(int dirty) {
        PowerGroup powerGroup;
        if ((dirty & 20151) == 0) {
            return false;
        }
        long time = this.mClock.uptimeMillis();
        boolean changed = false;
        for (int idx = 0; idx < this.mPowerGroups.size(); idx++) {
            PowerGroup powerGroup2 = this.mPowerGroups.valueAt(idx);
            if (powerGroup2.getWakefulnessLocked() == 1 && isItBedTimeYetLocked(powerGroup2)) {
                if (DEBUG_SPEW) {
                    Slog.d(TAG, "updateWakefulnessLocked: Bed time for group " + powerGroup2.getGroupId());
                }
                Slog.i(TAG, "updateWakefulnessLocked idx: " + idx + " getIsConnectSource: " + ITranPowerManagerService.Instance().getIsConnectSource() + " getIsConnectingState: " + ITranPowerManagerService.Instance().getIsConnectingState());
                if (idx != 0) {
                    powerGroup = powerGroup2;
                } else if (ITranPowerManagerService.Instance().getIsConnectSource() || ITranPowerManagerService.Instance().getIsConnectingState()) {
                    if (powerGroup2.getWakefulnessLocked() != 4) {
                        setWakefulnessLocked(idx, 4, time, 1000, 0, 0, null, null);
                        this.mPolicy.setConnectScreenActive(true, true);
                    }
                    return changed;
                } else {
                    powerGroup = powerGroup2;
                }
                if (isAttentiveTimeoutExpired(powerGroup, time)) {
                    if (DEBUG) {
                        Slog.i(TAG, "Going to sleep now due to long user inactivity");
                    }
                    changed = sleepPowerGroupLocked(powerGroup, time, 9, 1000);
                } else {
                    boolean changed2 = shouldNapAtBedTimeLocked();
                    if (changed2) {
                        changed = dreamPowerGroupLocked(powerGroup, time, 1000);
                    } else {
                        changed = dozePowerGroupLocked(powerGroup, time, 2, 1000);
                    }
                }
            }
        }
        return changed;
    }

    private boolean shouldNapAtBedTimeLocked() {
        return this.mDreamsActivateOnSleepSetting || (this.mDreamsActivateOnDockSetting && this.mDockState != 0);
    }

    private boolean isItBedTimeYetLocked(PowerGroup powerGroup) {
        if (!this.mBootCompleted) {
            return false;
        }
        long now = this.mClock.uptimeMillis();
        if (isAttentiveTimeoutExpired(powerGroup, now)) {
            return !isBeingKeptFromInattentiveSleepLocked();
        }
        return !isBeingKeptAwakeLocked(powerGroup);
    }

    private boolean isBeingKeptAwakeLocked(PowerGroup powerGroup) {
        return ITranPowerManagerService.Instance().getIsConnectSource() ? (powerGroup.getUserActivitySummaryLocked() & 3) != 0 : this.mStayOn || this.mProximityPositive || (powerGroup.getWakeLockSummaryLocked() & 32) != 0 || (powerGroup.getUserActivitySummaryLocked() & 3) != 0 || this.mScreenBrightnessBoostInProgress;
    }

    private boolean isBeingKeptFromInattentiveSleepLocked() {
        return this.mStayOn || this.mScreenBrightnessBoostInProgress || this.mProximityPositive || !this.mBootCompleted;
    }

    private void updateDreamLocked(int dirty, boolean powerGroupBecameReady) {
        if (((dirty & 17407) != 0 || powerGroupBecameReady) && areAllPowerGroupsReadyLocked()) {
            scheduleSandmanLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleSandmanLocked() {
        if (!this.mSandmanScheduled) {
            this.mSandmanScheduled = true;
            for (int idx = 0; idx < this.mPowerGroups.size(); idx++) {
                PowerGroup powerGroup = this.mPowerGroups.valueAt(idx);
                if (powerGroup.supportsSandmanLocked()) {
                    Message msg = this.mHandler.obtainMessage(2);
                    msg.arg1 = powerGroup.getGroupId();
                    msg.setAsynchronous(true);
                    this.mHandler.sendMessageAtTime(msg, this.mClock.uptimeMillis());
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSandman(int groupId) {
        boolean startDreaming;
        boolean startDreaming2;
        boolean isDreaming;
        synchronized (this.mLock) {
            this.mSandmanScheduled = false;
            if (this.mPowerGroups.contains(groupId)) {
                PowerGroup powerGroup = this.mPowerGroups.get(groupId);
                int wakefulness = powerGroup.getWakefulnessLocked();
                boolean z = true;
                if ((wakefulness == 2 || wakefulness == 3) && powerGroup.isSandmanSummonedLocked() && powerGroup.isReadyLocked()) {
                    if (!canDreamLocked(powerGroup) && !canDozeLocked(powerGroup)) {
                        startDreaming = false;
                        powerGroup.setSandmanSummonedLocked(false);
                        startDreaming2 = startDreaming;
                    }
                    startDreaming = true;
                    powerGroup.setSandmanSummonedLocked(false);
                    startDreaming2 = startDreaming;
                } else {
                    startDreaming2 = false;
                }
                DreamManagerInternal dreamManagerInternal = this.mDreamManager;
                if (dreamManagerInternal != null) {
                    if (startDreaming2) {
                        dreamManagerInternal.stopDream(false);
                        ITranPowerManagerService.Instance().hookScreenStateFromOnToDoze();
                        DreamManagerInternal dreamManagerInternal2 = this.mDreamManager;
                        if (wakefulness != 3) {
                            z = false;
                        }
                        dreamManagerInternal2.startDream(z);
                    }
                    isDreaming = this.mDreamManager.isDreaming();
                } else {
                    isDreaming = false;
                }
                this.mDozeStartInProgress = false;
                synchronized (this.mLock) {
                    if (this.mPowerGroups.contains(groupId)) {
                        if (startDreaming2 && isDreaming) {
                            this.mBatteryLevelWhenDreamStarted = this.mBatteryLevel;
                            if (wakefulness == 3) {
                                Slog.i(TAG, "Dozing...");
                            } else {
                                Slog.i(TAG, "Dreaming...");
                            }
                        }
                        PowerGroup powerGroup2 = this.mPowerGroups.get(groupId);
                        if (!powerGroup2.isSandmanSummonedLocked() && powerGroup2.getWakefulnessLocked() == wakefulness) {
                            long now = this.mClock.uptimeMillis();
                            if (wakefulness == 2) {
                                if (isDreaming && canDreamLocked(powerGroup2)) {
                                    int i = this.mDreamsBatteryLevelDrainCutoffConfig;
                                    if (i < 0 || this.mBatteryLevel >= this.mBatteryLevelWhenDreamStarted - i || isBeingKeptAwakeLocked(powerGroup2)) {
                                        return;
                                    }
                                    Slog.i(TAG, "Stopping dream because the battery appears to be draining faster than it is charging.  Battery level when dream started: " + this.mBatteryLevelWhenDreamStarted + "%.  Battery level now: " + this.mBatteryLevel + "%.");
                                }
                                if (isItBedTimeYetLocked(powerGroup2)) {
                                    if (isAttentiveTimeoutExpired(powerGroup2, now)) {
                                        sleepPowerGroupLocked(powerGroup2, now, 2, 1000);
                                    } else {
                                        dozePowerGroupLocked(powerGroup2, now, 2, 1000);
                                    }
                                } else {
                                    wakePowerGroupLocked(powerGroup2, now, 13, "android.server.power:DREAM_FINISHED", 1000, this.mContext.getOpPackageName(), 1000);
                                }
                            } else if (wakefulness == 3) {
                                if (isDreaming) {
                                    return;
                                }
                                sleepPowerGroupLocked(powerGroup2, now, 2, 1000);
                            }
                            if (isDreaming) {
                                this.mDreamManager.stopDream(false);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean canDreamLocked(PowerGroup powerGroup) {
        int i;
        int i2;
        if (this.mBootCompleted && getGlobalWakefulnessLocked() == 2 && this.mDreamsSupportedConfig && this.mDreamsEnabledSetting && powerGroup.isBrightOrDimLocked() && !powerGroup.isPolicyVrLocked() && (powerGroup.getUserActivitySummaryLocked() & 7) != 0) {
            if (isBeingKeptAwakeLocked(powerGroup)) {
                return true;
            }
            boolean z = this.mIsPowered;
            if (z || this.mDreamsEnabledOnBatteryConfig) {
                if (z || (i2 = this.mDreamsBatteryLevelMinimumWhenNotPoweredConfig) < 0 || this.mBatteryLevel >= i2) {
                    return !z || (i = this.mDreamsBatteryLevelMinimumWhenPoweredConfig) < 0 || this.mBatteryLevel >= i;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    private boolean canDozeLocked(PowerGroup powerGroup) {
        return powerGroup.supportsSandmanLocked() && powerGroup.getWakefulnessLocked() == 3;
    }

    private boolean updatePowerGroupsLocked(int dirty) {
        boolean oldPowerGroupsReady;
        boolean z;
        boolean z2;
        boolean autoBrightness;
        float screenBrightnessOverride;
        int groupId;
        boolean z3;
        PowerManagerService powerManagerService = this;
        boolean oldPowerGroupsReady2 = areAllPowerGroupsReadyLocked();
        int i = true;
        boolean z4 = false;
        if ((79935 & dirty) == 0) {
            oldPowerGroupsReady = oldPowerGroupsReady2;
            z = true;
            z2 = false;
        } else {
            if ((dirty & 4096) != 0) {
                if (areAllPowerGroupsReadyLocked()) {
                    sQuiescent = false;
                } else {
                    powerManagerService.mDirty |= 4096;
                }
            }
            int idx = 0;
            while (idx < powerManagerService.mPowerGroups.size()) {
                PowerGroup powerGroup = powerManagerService.mPowerGroups.valueAt(idx);
                int groupId2 = powerGroup.getGroupId();
                if (!powerManagerService.mBootCompleted) {
                    DisplayManagerInternal displayManagerInternal = powerManagerService.mDisplayManagerInternal;
                    if (displayManagerInternal != null) {
                        float screenBrightnessTemp = displayManagerInternal.getDefaultDisplayBrightness();
                        autoBrightness = false;
                        screenBrightnessOverride = MathUtils.constrain(screenBrightnessTemp == -1.0f ? powerManagerService.mScreenBrightnessDefault : screenBrightnessTemp, 0.0f, 1.0f);
                    } else {
                        float screenBrightnessOverride2 = powerManagerService.mScreenBrightnessDefault;
                        autoBrightness = false;
                        screenBrightnessOverride = screenBrightnessOverride2;
                    }
                } else if (isValidBrightness(powerManagerService.mScreenBrightnessOverrideFromWindowManager)) {
                    autoBrightness = false;
                    screenBrightnessOverride = powerManagerService.mScreenBrightnessOverrideFromWindowManager;
                } else {
                    int autoBrightness2 = powerManagerService.mScreenBrightnessModeSetting == i ? i : z4;
                    autoBrightness = autoBrightness2;
                    screenBrightnessOverride = Float.NaN;
                }
                boolean oldPowerGroupsReady3 = oldPowerGroupsReady2;
                int idx2 = idx;
                float screenBrightnessOverride3 = screenBrightnessOverride;
                boolean autoBrightness3 = autoBrightness;
                boolean ready = powerGroup.updateLocked(screenBrightnessOverride, autoBrightness, shouldUseProximitySensorLocked(), shouldBoostScreenBrightness(), powerManagerService.mDozeScreenStateOverrideFromDreamManager, powerManagerService.mDozeScreenBrightnessOverrideFromDreamManagerFloat, powerManagerService.mDrawWakeLockOverrideFromSidekick, powerManagerService.mBatterySaverPolicy.getBatterySaverPolicy(7), sQuiescent, powerManagerService.mDozeAfterScreenOff, powerManagerService.mIsVrModeEnabled, powerManagerService.mBootCompleted, powerManagerService.mScreenBrightnessBoostInProgress, powerManagerService.mRequestWaitForNegativeProximity);
                int wakefulness = powerGroup.getWakefulnessLocked();
                if (!DEBUG_SPEW) {
                    groupId = groupId2;
                    powerManagerService = this;
                } else {
                    groupId = groupId2;
                    powerManagerService = this;
                    Slog.d(TAG, "updateDisplayPowerStateLocked: displayReady=" + ready + ", groupId=" + groupId2 + ", policy=" + DisplayManagerInternal.DisplayPowerRequest.policyToString(powerGroup.getPolicyLocked()) + ", mWakefulness=" + PowerManagerInternal.wakefulnessToString(wakefulness) + ", mWakeLockSummary=0x" + Integer.toHexString(powerGroup.getWakeLockSummaryLocked()) + ", mUserActivitySummary=0x" + Integer.toHexString(powerGroup.getUserActivitySummaryLocked()) + ", mBootCompleted=" + powerManagerService.mBootCompleted + ", screenBrightnessOverride=" + screenBrightnessOverride3 + ", useAutoBrightness=" + autoBrightness3 + ", mScreenBrightnessBoostInProgress=" + powerManagerService.mScreenBrightnessBoostInProgress + ", mIsVrModeEnabled= " + powerManagerService.mIsVrModeEnabled + ", sQuiescent=" + sQuiescent);
                }
                boolean displayReadyStateChanged = powerGroup.setReadyLocked(ready);
                boolean poweringOn = powerGroup.isPoweringOnLocked();
                if (ready && displayReadyStateChanged && poweringOn) {
                    z3 = true;
                    if (wakefulness == 1) {
                        powerGroup.setIsPoweringOnLocked(false);
                        LatencyTracker.getInstance(powerManagerService.mContext).onActionEnd(5);
                        Trace.asyncTraceEnd(131072L, TRACE_SCREEN_ON, groupId);
                        int latencyMs = (int) (powerManagerService.mClock.uptimeMillis() - powerGroup.getLastPowerOnTimeLocked());
                        if (latencyMs >= 200) {
                            Slog.w(TAG, "Screen on took " + latencyMs + " ms");
                        }
                    }
                } else {
                    z3 = true;
                }
                idx = idx2 + 1;
                i = z3;
                oldPowerGroupsReady2 = oldPowerGroupsReady3;
                z4 = false;
            }
            oldPowerGroupsReady = oldPowerGroupsReady2;
            z = i;
            z2 = false;
            powerManagerService.mRequestWaitForNegativeProximity = false;
        }
        return (!areAllPowerGroupsReadyLocked() || oldPowerGroupsReady) ? z2 : z;
    }

    private void updateScreenBrightnessBoostLocked(int dirty) {
        if ((dirty & 2048) != 0 && this.mScreenBrightnessBoostInProgress) {
            long now = this.mClock.uptimeMillis();
            this.mHandler.removeMessages(3);
            long j = this.mLastScreenBrightnessBoostTime;
            if (j > this.mLastGlobalSleepTime) {
                long boostTimeout = j + 5000;
                if (boostTimeout > now) {
                    Message msg = this.mHandler.obtainMessage(3);
                    msg.setAsynchronous(true);
                    this.mHandler.sendMessageAtTime(msg, boostTimeout);
                    return;
                }
            }
            this.mScreenBrightnessBoostInProgress = false;
            userActivityNoUpdateLocked(now, 0, 0, 1000);
        }
    }

    private boolean shouldBoostScreenBrightness() {
        return !this.mIsVrModeEnabled && this.mScreenBrightnessBoostInProgress;
    }

    private static boolean isValidBrightness(float value) {
        return value >= 0.0f && value <= 1.0f;
    }

    int getDesiredScreenPolicyLocked(int groupId) {
        return this.mPowerGroups.get(groupId).getDesiredScreenPolicyLocked(sQuiescent, this.mDozeAfterScreenOff, this.mIsVrModeEnabled, this.mBootCompleted, this.mScreenBrightnessBoostInProgress);
    }

    private boolean shouldUseProximitySensorLocked() {
        return (this.mIsVrModeEnabled || (this.mPowerGroups.get(0).getWakeLockSummaryLocked() & 16) == 0) ? false : true;
    }

    private void updateSuspendBlockerLocked() {
        boolean needWakeLockSuspendBlocker = (this.mWakeLockSummary & 1) != 0;
        boolean needDisplaySuspendBlocker = needSuspendBlockerLocked();
        boolean autoSuspend = !needDisplaySuspendBlocker;
        boolean interactive = false;
        for (int idx = 0; idx < this.mPowerGroups.size() && !interactive; idx++) {
            interactive = this.mPowerGroups.valueAt(idx).isBrightOrDimLocked();
        }
        if (!autoSuspend && this.mDecoupleHalAutoSuspendModeFromDisplayConfig) {
            setHalAutoSuspendModeLocked(false);
        }
        if (!this.mBootCompleted && !this.mHoldingBootingSuspendBlocker) {
            this.mBootingSuspendBlocker.acquire();
            this.mHoldingBootingSuspendBlocker = true;
        }
        if (needWakeLockSuspendBlocker && !this.mHoldingWakeLockSuspendBlocker) {
            this.mWakeLockSuspendBlocker.acquire();
            this.mHoldingWakeLockSuspendBlocker = true;
        }
        if (needDisplaySuspendBlocker && !this.mHoldingDisplaySuspendBlocker) {
            this.mDisplaySuspendBlocker.acquire(HOLDING_DISPLAY_SUSPEND_BLOCKER);
            this.mHoldingDisplaySuspendBlocker = true;
        }
        if (this.mDecoupleHalInteractiveModeFromDisplayConfig && (interactive || areAllPowerGroupsReadyLocked())) {
            setHalInteractiveModeLocked(interactive);
        }
        if (this.mBootCompleted && this.mHoldingBootingSuspendBlocker) {
            this.mBootingSuspendBlocker.release();
            this.mHoldingBootingSuspendBlocker = false;
        }
        if (!needWakeLockSuspendBlocker && this.mHoldingWakeLockSuspendBlocker) {
            this.mWakeLockSuspendBlocker.release();
            this.mHoldingWakeLockSuspendBlocker = false;
        }
        if (!needDisplaySuspendBlocker && this.mHoldingDisplaySuspendBlocker) {
            this.mDisplaySuspendBlocker.release(HOLDING_DISPLAY_SUSPEND_BLOCKER);
            this.mHoldingDisplaySuspendBlocker = false;
        }
        if (autoSuspend && this.mDecoupleHalAutoSuspendModeFromDisplayConfig) {
            setHalAutoSuspendModeLocked(true);
        }
    }

    private boolean needSuspendBlockerLocked() {
        if (areAllPowerGroupsReadyLocked() && !this.mScreenBrightnessBoostInProgress) {
            if (getGlobalWakefulnessLocked() == 3 && this.mDozeStartInProgress) {
                return true;
            }
            for (int idx = 0; idx < this.mPowerGroups.size(); idx++) {
                PowerGroup powerGroup = this.mPowerGroups.valueAt(idx);
                if (powerGroup.needSuspendBlockerLocked(this.mProximityPositive, this.mSuspendWhenScreenOffDueToProximityConfig)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setHalAutoSuspendModeLocked(boolean enable) {
        if (enable != this.mHalAutoSuspendModeEnabled) {
            if (DEBUG) {
                Slog.d(TAG, "Setting HAL auto-suspend mode to " + enable);
            }
            this.mHalAutoSuspendModeEnabled = enable;
            Trace.traceBegin(131072L, "setHalAutoSuspend(" + enable + ")");
            try {
                this.mNativeWrapper.nativeSetAutoSuspend(enable);
            } finally {
                Trace.traceEnd(131072L);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setHalInteractiveModeLocked(boolean enable) {
        if (enable != this.mHalInteractiveModeEnabled) {
            if (DEBUG) {
                Slog.d(TAG, "Setting HAL interactive mode to " + enable);
            }
            this.mHalInteractiveModeEnabled = enable;
            Trace.traceBegin(131072L, "setHalInteractive(" + enable + ")");
            try {
                this.mNativeWrapper.nativeSetPowerMode(7, enable);
            } finally {
                Trace.traceEnd(131072L);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isInteractiveInternal() {
        boolean isInteractive;
        synchronized (this.mLock) {
            isInteractive = PowerManagerInternal.isInteractive(getGlobalWakefulnessLocked());
        }
        return isInteractive;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setLowPowerModeInternal(boolean enabled) {
        synchronized (this.mLock) {
            if (DEBUG) {
                Slog.d(TAG, "setLowPowerModeInternal " + enabled + " mIsPowered=" + this.mIsPowered);
            }
            if (this.mIsPowered) {
                return false;
            }
            this.mBatterySaverStateMachine.setBatterySaverEnabledManually(enabled);
            return true;
        }
    }

    boolean isDeviceIdleModeInternal() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mDeviceIdleMode;
        }
        return z;
    }

    boolean isLightDeviceIdleModeInternal() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mLightDeviceIdleMode;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleBatteryStateChangedLocked() {
        this.mDirty |= 256;
        updatePowerStateLocked();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void shutdownOrRebootInternal(final int haltMode, final boolean confirm, final String reason, boolean wait) {
        if ("userspace".equals(reason)) {
            if (!PowerManager.isRebootingUserspaceSupportedImpl()) {
                throw new UnsupportedOperationException("Attempted userspace reboot on a device that doesn't support it");
            }
            UserspaceRebootLogger.noteUserspaceRebootWasRequested();
        }
        if (this.mHandler == null || !this.mSystemReady) {
            if (RescueParty.isAttemptingFactoryReset()) {
                lowLevelReboot(reason);
            } else {
                throw new IllegalStateException("Too early to call shutdown() or reboot()");
            }
        }
        Runnable runnable = new Runnable() { // from class: com.android.server.power.PowerManagerService.4
            @Override // java.lang.Runnable
            public void run() {
                synchronized (this) {
                    int i = haltMode;
                    if (i == 2) {
                        ShutdownThread.rebootSafeMode(PowerManagerService.this.getUiContext(), confirm);
                    } else if (i == 1) {
                        ShutdownThread.reboot(PowerManagerService.this.getUiContext(), reason, confirm);
                    } else {
                        ShutdownThread.shutdown(PowerManagerService.this.getUiContext(), reason, confirm);
                    }
                }
            }
        };
        Message msg = Message.obtain(UiThread.getHandler(), runnable);
        msg.setAsynchronous(true);
        UiThread.getHandler().sendMessage(msg);
        if (wait) {
            synchronized (runnable) {
                long WAIT_TIME_MS = SystemClock.uptimeMillis() + 55000;
                while (SystemClock.uptimeMillis() - WAIT_TIME_MS <= 0) {
                    try {
                        runnable.wait(55000L);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void crashInternal(final String message) {
        Thread t = new Thread("PowerManagerService.crash()") { // from class: com.android.server.power.PowerManagerService.5
            @Override // java.lang.Thread, java.lang.Runnable
            public void run() {
                throw new RuntimeException(message);
            }
        };
        try {
            t.start();
            t.join();
        } catch (InterruptedException e) {
            Slog.wtf(TAG, e);
        }
    }

    void setStayOnSettingInternal(int val) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "stay_on_while_plugged_in", val);
    }

    void setMaximumScreenOffTimeoutFromDeviceAdminInternal(int userId, long timeMs) {
        if (userId < 0) {
            Slog.wtf(TAG, "Attempt to set screen off timeout for invalid user: " + userId);
            return;
        }
        synchronized (this.mLock) {
            try {
                if (userId == 0) {
                    this.mMaximumScreenOffTimeoutFromDeviceAdmin = timeMs;
                } else {
                    if (timeMs != JobStatus.NO_LATEST_RUNTIME && timeMs != 0) {
                        ProfilePowerState profile = this.mProfilePowerState.get(userId);
                        if (profile != null) {
                            profile.mScreenOffTimeout = timeMs;
                        } else {
                            this.mProfilePowerState.put(userId, new ProfilePowerState(userId, timeMs, this.mClock.uptimeMillis()));
                            this.mDirty |= 1;
                        }
                    }
                    this.mProfilePowerState.delete(userId);
                }
                this.mDirty |= 32;
                updatePowerStateLocked();
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    boolean setDeviceIdleModeInternal(boolean enabled) {
        synchronized (this.mLock) {
            boolean z = false;
            if (this.mDeviceIdleMode == enabled) {
                return false;
            }
            this.mDeviceIdleMode = enabled;
            updateWakeLockDisabledStatesLocked();
            if (this.mDeviceIdleMode || this.mLightDeviceIdleMode) {
                z = true;
            }
            setPowerModeInternal(8, z);
            if (enabled) {
                EventLogTags.writeDeviceIdleOnPhase("power");
            } else {
                EventLogTags.writeDeviceIdleOffPhase("power");
            }
            return true;
        }
    }

    boolean setLightDeviceIdleModeInternal(boolean enabled) {
        synchronized (this.mLock) {
            boolean z = false;
            if (this.mLightDeviceIdleMode != enabled) {
                this.mLightDeviceIdleMode = enabled;
                if (this.mDeviceIdleMode || enabled) {
                    z = true;
                }
                setPowerModeInternal(8, z);
                return true;
            }
            return false;
        }
    }

    void setDeviceIdleWhitelistInternal(int[] appids) {
        synchronized (this.mLock) {
            this.mDeviceIdleWhitelist = appids;
            if (this.mDeviceIdleMode) {
                updateWakeLockDisabledStatesLocked();
            }
        }
    }

    void setDeviceIdleTempWhitelistInternal(int[] appids) {
        synchronized (this.mLock) {
            this.mDeviceIdleTempWhitelist = appids;
            if (this.mDeviceIdleMode) {
                updateWakeLockDisabledStatesLocked();
            }
        }
    }

    void setLowPowerStandbyAllowlistInternal(int[] appids) {
        synchronized (this.mLock) {
            this.mLowPowerStandbyAllowlist = appids;
            if (this.mLowPowerStandbyActive) {
                updateWakeLockDisabledStatesLocked();
            }
        }
    }

    void setLowPowerStandbyActiveInternal(boolean active) {
        synchronized (this.mLock) {
            if (this.mLowPowerStandbyActive != active) {
                this.mLowPowerStandbyActive = active;
                updateWakeLockDisabledStatesLocked();
            }
        }
    }

    void startUidChangesInternal() {
        synchronized (this.mLock) {
            this.mUidsChanging = true;
        }
    }

    void finishUidChangesInternal() {
        synchronized (this.mLock) {
            this.mUidsChanging = false;
            if (this.mUidsChanged) {
                updateWakeLockDisabledStatesLocked();
                this.mUidsChanged = false;
            }
        }
    }

    private void handleUidStateChangeLocked() {
        if (this.mUidsChanging) {
            this.mUidsChanged = true;
        } else {
            updateWakeLockDisabledStatesLocked();
        }
    }

    void updateUidProcStateInternal(int uid, int procState) {
        synchronized (this.mLock) {
            UidState state = this.mUidState.get(uid);
            if (state == null) {
                state = new UidState(uid);
                this.mUidState.put(uid, state);
            }
            boolean z = true;
            boolean oldShouldAllow = state.mProcState <= 11;
            state.mProcState = procState;
            if (state.mNumWakeLocks > 0) {
                if (!this.mDeviceIdleMode && !this.mLowPowerStandbyActive) {
                    if (!state.mActive) {
                        if (procState > 11) {
                            z = false;
                        }
                        if (oldShouldAllow != z) {
                            handleUidStateChangeLocked();
                        }
                    }
                }
                handleUidStateChangeLocked();
            }
        }
    }

    void uidGoneInternal(int uid) {
        synchronized (this.mLock) {
            int index = this.mUidState.indexOfKey(uid);
            if (index >= 0) {
                UidState state = this.mUidState.valueAt(index);
                state.mProcState = 20;
                state.mActive = false;
                this.mUidState.removeAt(index);
                if ((this.mDeviceIdleMode || this.mLowPowerStandbyActive) && state.mNumWakeLocks > 0) {
                    handleUidStateChangeLocked();
                }
            }
        }
    }

    void uidActiveInternal(int uid) {
        synchronized (this.mLock) {
            UidState state = this.mUidState.get(uid);
            if (state == null) {
                state = new UidState(uid);
                state.mProcState = 19;
                this.mUidState.put(uid, state);
            }
            state.mActive = true;
            if (state.mNumWakeLocks > 0) {
                handleUidStateChangeLocked();
            }
        }
    }

    void uidIdleInternal(int uid) {
        synchronized (this.mLock) {
            UidState state = this.mUidState.get(uid);
            if (state != null) {
                state.mActive = false;
                if (state.mNumWakeLocks > 0) {
                    handleUidStateChangeLocked();
                }
            }
        }
    }

    private void updateWakeLockDisabledStatesLocked() {
        boolean changed = false;
        int numWakeLocks = this.mWakeLocks.size();
        for (int i = 0; i < numWakeLocks; i++) {
            WakeLock wakeLock = this.mWakeLocks.get(i);
            if ((wakeLock.mFlags & GnssNative.GNSS_AIDING_TYPE_ALL) == 1 && setWakeLockDisabledStateLocked(wakeLock)) {
                changed = true;
                if (wakeLock.mDisabled) {
                    notifyWakeLockReleasedLocked(wakeLock);
                } else {
                    notifyWakeLockAcquiredLocked(wakeLock);
                }
            }
        }
        if (changed) {
            this.mDirty |= 1;
            updatePowerStateLocked();
        }
    }

    private boolean setWakeLockDisabledStateLocked(WakeLock wakeLock) {
        boolean z = false;
        if ((wakeLock.mFlags & GnssNative.GNSS_AIDING_TYPE_ALL) == 1) {
            boolean disabled = false;
            int appid = UserHandle.getAppId(wakeLock.mOwnerUid);
            if (appid >= 10000) {
                if (this.mConstants.NO_CACHED_WAKE_LOCKS) {
                    if (this.mForceSuspendActive || (!wakeLock.mUidState.mActive && wakeLock.mUidState.mProcState != 20 && wakeLock.mUidState.mProcState > 11)) {
                        z = true;
                    }
                    disabled = z;
                }
                if (sTranSlmSupport) {
                    if (this.mWakefulnessRaw == 0) {
                        UidState state = wakeLock.mUidState;
                        if (Arrays.binarySearch(this.mDeviceIdleWhitelist, appid) < 0 && Arrays.binarySearch(this.mDeviceIdleTempWhitelist, appid) < 0 && state.mProcState != 20 && state.mProcState > 5) {
                            disabled = true;
                        }
                    }
                } else if (this.mDeviceIdleMode) {
                    UidState state2 = wakeLock.mUidState;
                    if (Arrays.binarySearch(this.mDeviceIdleWhitelist, appid) < 0 && Arrays.binarySearch(this.mDeviceIdleTempWhitelist, appid) < 0 && state2.mProcState != 20 && state2.mProcState > 5) {
                        disabled = true;
                    }
                }
                if (this.mLowPowerStandbyActive) {
                    UidState state3 = wakeLock.mUidState;
                    if (Arrays.binarySearch(this.mLowPowerStandbyAllowlist, appid) < 0 && state3.mProcState != 20 && state3.mProcState > 3) {
                        disabled = true;
                    }
                }
            }
            return wakeLock.setDisabled(disabled);
        }
        return false;
    }

    private boolean isMaximumScreenOffTimeoutFromDeviceAdminEnforcedLocked() {
        long j = this.mMaximumScreenOffTimeoutFromDeviceAdmin;
        return j >= 0 && j < JobStatus.NO_LATEST_RUNTIME;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setAttentionLightInternal(boolean on, int color) {
        synchronized (this.mLock) {
            if (this.mSystemReady) {
                LogicalLight light = this.mAttentionLight;
                if (light != null) {
                    light.setFlashing(color, 2, on ? 3 : 0, 0);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDozeAfterScreenOffInternal(boolean on) {
        synchronized (this.mLock) {
            this.mDozeAfterScreenOff = on;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void boostScreenBrightnessInternal(long eventTime, int uid) {
        synchronized (this.mLock) {
            if (this.mSystemReady && getGlobalWakefulnessLocked() != 0 && eventTime >= this.mLastScreenBrightnessBoostTime) {
                Slog.i(TAG, "Brightness boost activated (uid " + uid + ")...");
                this.mLastScreenBrightnessBoostTime = eventTime;
                this.mScreenBrightnessBoostInProgress = true;
                this.mDirty |= 2048;
                userActivityNoUpdateLocked(this.mPowerGroups.get(0), eventTime, 0, 0, uid);
                updatePowerStateLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isScreenBrightnessBoostedInternal() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mScreenBrightnessBoostInProgress;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleScreenBrightnessBoostTimeout() {
        synchronized (this.mLock) {
            if (DEBUG_SPEW) {
                Slog.d(TAG, "handleScreenBrightnessBoostTimeout");
            }
            this.mDirty |= 2048;
            updatePowerStateLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setScreenBrightnessOverrideFromWindowManagerInternal(float brightness) {
        synchronized (this.mLock) {
            if (!BrightnessSynchronizer.floatEquals(this.mScreenBrightnessOverrideFromWindowManager, brightness)) {
                this.mScreenBrightnessOverrideFromWindowManager = brightness;
                this.mDirty |= 32;
                updatePowerStateLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setUserInactiveOverrideFromWindowManagerInternal() {
        synchronized (this.mLock) {
            this.mUserInactiveOverrideFromWindowManager = true;
            this.mDirty |= 4;
            updatePowerStateLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setUserActivityTimeoutOverrideFromWindowManagerInternal(long timeoutMillis) {
        synchronized (this.mLock) {
            if (this.mUserActivityTimeoutOverrideFromWindowManager != timeoutMillis) {
                this.mUserActivityTimeoutOverrideFromWindowManager = timeoutMillis;
                EventLogTags.writeUserActivityTimeoutOverride(timeoutMillis);
                this.mDirty |= 32;
                updatePowerStateLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDozeOverrideFromDreamManagerInternal(int screenState, int screenBrightness) {
        synchronized (this.mLock) {
            if (this.mDozeScreenStateOverrideFromDreamManager != screenState || this.mDozeScreenBrightnessOverrideFromDreamManager != screenBrightness) {
                this.mDozeScreenStateOverrideFromDreamManager = screenState;
                this.mDozeScreenBrightnessOverrideFromDreamManager = screenBrightness;
                this.mDozeScreenBrightnessOverrideFromDreamManagerFloat = BrightnessSynchronizer.brightnessIntToFloat(screenBrightness);
                this.mDirty |= 32;
                updatePowerStateLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDrawWakeLockOverrideFromSidekickInternal(boolean keepState) {
        synchronized (this.mLock) {
            if (this.mDrawWakeLockOverrideFromSidekick != keepState) {
                this.mDrawWakeLockOverrideFromSidekick = keepState;
                this.mDirty |= 32;
                updatePowerStateLocked();
            }
        }
    }

    void setVrModeEnabled(boolean enabled) {
        this.mIsVrModeEnabled = enabled;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setPowerBoostInternal(int boost, int durationMs) {
        this.mNativeWrapper.nativeSetPowerBoost(boost, durationMs);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setPowerModeInternal(int mode, boolean enabled) {
        if (mode == 5 && enabled && this.mBatterySaverController.isLaunchBoostDisabled()) {
            return false;
        }
        return this.mNativeWrapper.nativeSetPowerMode(mode, enabled);
    }

    boolean wasDeviceIdleForInternal(long ms) {
        boolean z;
        synchronized (this.mLock) {
            z = this.mPowerGroups.get(0).getLastUserActivityTimeLocked() + ms < this.mClock.uptimeMillis();
        }
        return z;
    }

    void onUserActivity() {
        synchronized (this.mLock) {
            this.mPowerGroups.get(0).setLastUserActivityTimeLocked(this.mClock.uptimeMillis());
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4902=4] */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    /* JADX INFO: Access modifiers changed from: private */
    public boolean forceSuspendInternal(int uid) {
        try {
            synchronized (this.mLock) {
                this.mForceSuspendActive = true;
                for (int idx = 0; idx < this.mPowerGroups.size(); idx++) {
                    sleepPowerGroupLocked(this.mPowerGroups.valueAt(idx), this.mClock.uptimeMillis(), 8, uid);
                }
                updateWakeLockDisabledStatesLocked();
            }
            Slog.i(TAG, "Force-Suspending (uid " + uid + ")...");
            boolean success = this.mNativeWrapper.nativeForceSuspend();
            if (!success) {
                Slog.i(TAG, "Force-Suspending failed in native.");
            }
            synchronized (this.mLock) {
                this.mForceSuspendActive = false;
                updateWakeLockDisabledStatesLocked();
            }
            return success;
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mForceSuspendActive = false;
                updateWakeLockDisabledStatesLocked();
                throw th;
            }
        }
    }

    public static void lowLevelShutdown(String reason) {
        if (reason == null) {
            reason = "";
        }
        SystemProperties.set("sys.powerctl", "shutdown," + reason);
    }

    public static void lowLevelReboot(String reason) {
        if (reason == null) {
            reason = "";
        }
        if (reason.equals("quiescent")) {
            sQuiescent = true;
            reason = "";
        } else if (reason.endsWith(",quiescent")) {
            sQuiescent = true;
            reason = reason.substring(0, (reason.length() - "quiescent".length()) - 1);
        }
        reason = (reason.equals("recovery") || reason.equals("recovery-update")) ? "recovery" : "recovery";
        if (sQuiescent) {
            if (!"".equals(reason)) {
                reason = reason + ",";
            }
            reason = reason + "quiescent";
        }
        SystemProperties.set("sys.powerctl", "reboot," + reason);
        try {
            Thread.sleep(20000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Slog.wtf(TAG, "Unexpected return from lowLevelReboot!");
    }

    @Override // com.android.server.Watchdog.Monitor
    public void monitor() {
        synchronized (this.mLock) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    @NeverCompile
    public void dumpInternal(PrintWriter pw) {
        WirelessChargerDetector wcd;
        pw.println("POWER MANAGER (dumpsys power)\n");
        synchronized (this.mLock) {
            pw.println("Power Manager State:");
            this.mConstants.dump(pw);
            pw.println("  mDirty=0x" + Integer.toHexString(this.mDirty));
            pw.println("  mWakefulness=" + PowerManagerInternal.wakefulnessToString(getGlobalWakefulnessLocked()));
            pw.println("  mWakefulnessChanging=" + this.mWakefulnessChanging);
            pw.println("  mIsPowered=" + this.mIsPowered);
            pw.println("  mPlugType=" + this.mPlugType);
            pw.println("  mBatteryLevel=" + this.mBatteryLevel);
            pw.println("  mBatteryLevelWhenDreamStarted=" + this.mBatteryLevelWhenDreamStarted);
            pw.println("  mDockState=" + this.mDockState);
            pw.println("  mStayOn=" + this.mStayOn);
            pw.println("  mProximityPositive=" + this.mProximityPositive);
            pw.println("  mBootCompleted=" + this.mBootCompleted);
            pw.println("  mSystemReady=" + this.mSystemReady);
            synchronized (this.mEnhancedDischargeTimeLock) {
                pw.println("  mEnhancedDischargeTimeElapsed=" + this.mEnhancedDischargeTimeElapsed);
                pw.println("  mLastEnhancedDischargeTimeUpdatedElapsed=" + this.mLastEnhancedDischargeTimeUpdatedElapsed);
                pw.println("  mEnhancedDischargePredictionIsPersonalized=" + this.mEnhancedDischargePredictionIsPersonalized);
            }
            pw.println("  mHalAutoSuspendModeEnabled=" + this.mHalAutoSuspendModeEnabled);
            pw.println("  mHalInteractiveModeEnabled=" + this.mHalInteractiveModeEnabled);
            pw.println("  mWakeLockSummary=0x" + Integer.toHexString(this.mWakeLockSummary));
            pw.print("  mNotifyLongScheduled=");
            long j = this.mNotifyLongScheduled;
            if (j == 0) {
                pw.print("(none)");
            } else {
                TimeUtils.formatDuration(j, this.mClock.uptimeMillis(), pw);
            }
            pw.println();
            pw.print("  mNotifyLongDispatched=");
            long j2 = this.mNotifyLongDispatched;
            if (j2 == 0) {
                pw.print("(none)");
            } else {
                TimeUtils.formatDuration(j2, this.mClock.uptimeMillis(), pw);
            }
            pw.println();
            pw.print("  mNotifyLongNextCheck=");
            long j3 = this.mNotifyLongNextCheck;
            if (j3 == 0) {
                pw.print("(none)");
            } else {
                TimeUtils.formatDuration(j3, this.mClock.uptimeMillis(), pw);
            }
            pw.println();
            pw.println("  mRequestWaitForNegativeProximity=" + this.mRequestWaitForNegativeProximity);
            pw.println("  mInterceptedPowerKeyForProximity=" + this.mInterceptedPowerKeyForProximity);
            pw.println("  mSandmanScheduled=" + this.mSandmanScheduled);
            pw.println("  mBatteryLevelLow=" + this.mBatteryLevelLow);
            pw.println("  mLightDeviceIdleMode=" + this.mLightDeviceIdleMode);
            pw.println("  mDeviceIdleMode=" + this.mDeviceIdleMode);
            pw.println("  mDeviceIdleWhitelist=" + Arrays.toString(this.mDeviceIdleWhitelist));
            pw.println("  mDeviceIdleTempWhitelist=" + Arrays.toString(this.mDeviceIdleTempWhitelist));
            pw.println("  mLowPowerStandbyActive=" + this.mLowPowerStandbyActive);
            pw.println("  mLastWakeTime=" + TimeUtils.formatUptime(this.mLastGlobalWakeTime));
            pw.println("  mLastSleepTime=" + TimeUtils.formatUptime(this.mLastGlobalSleepTime));
            pw.println("  mLastSleepReason=" + PowerManager.sleepReasonToString(this.mLastGlobalSleepReason));
            pw.println("  mLastInteractivePowerHintTime=" + TimeUtils.formatUptime(this.mLastInteractivePowerHintTime));
            pw.println("  mLastScreenBrightnessBoostTime=" + TimeUtils.formatUptime(this.mLastScreenBrightnessBoostTime));
            pw.println("  mScreenBrightnessBoostInProgress=" + this.mScreenBrightnessBoostInProgress);
            pw.println("  mHoldingWakeLockSuspendBlocker=" + this.mHoldingWakeLockSuspendBlocker);
            pw.println("  mHoldingDisplaySuspendBlocker=" + this.mHoldingDisplaySuspendBlocker);
            pw.println("  mLastFlipTime=" + this.mLastFlipTime);
            pw.println("  mIsFaceDown=" + this.mIsFaceDown);
            pw.println();
            pw.println("Settings and Configuration:");
            pw.println("  mDecoupleHalAutoSuspendModeFromDisplayConfig=" + this.mDecoupleHalAutoSuspendModeFromDisplayConfig);
            pw.println("  mDecoupleHalInteractiveModeFromDisplayConfig=" + this.mDecoupleHalInteractiveModeFromDisplayConfig);
            pw.println("  mWakeUpWhenPluggedOrUnpluggedConfig=" + this.mWakeUpWhenPluggedOrUnpluggedConfig);
            pw.println("  mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig=" + this.mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig);
            pw.println("  mTheaterModeEnabled=" + this.mTheaterModeEnabled);
            pw.println("  mSuspendWhenScreenOffDueToProximityConfig=" + this.mSuspendWhenScreenOffDueToProximityConfig);
            pw.println("  mDreamsSupportedConfig=" + this.mDreamsSupportedConfig);
            pw.println("  mDreamsEnabledByDefaultConfig=" + this.mDreamsEnabledByDefaultConfig);
            pw.println("  mDreamsActivatedOnSleepByDefaultConfig=" + this.mDreamsActivatedOnSleepByDefaultConfig);
            pw.println("  mDreamsActivatedOnDockByDefaultConfig=" + this.mDreamsActivatedOnDockByDefaultConfig);
            pw.println("  mDreamsEnabledOnBatteryConfig=" + this.mDreamsEnabledOnBatteryConfig);
            pw.println("  mDreamsBatteryLevelMinimumWhenPoweredConfig=" + this.mDreamsBatteryLevelMinimumWhenPoweredConfig);
            pw.println("  mDreamsBatteryLevelMinimumWhenNotPoweredConfig=" + this.mDreamsBatteryLevelMinimumWhenNotPoweredConfig);
            pw.println("  mDreamsBatteryLevelDrainCutoffConfig=" + this.mDreamsBatteryLevelDrainCutoffConfig);
            pw.println("  mDreamsEnabledSetting=" + this.mDreamsEnabledSetting);
            pw.println("  mDreamsActivateOnSleepSetting=" + this.mDreamsActivateOnSleepSetting);
            pw.println("  mDreamsActivateOnDockSetting=" + this.mDreamsActivateOnDockSetting);
            pw.println("  mDozeAfterScreenOff=" + this.mDozeAfterScreenOff);
            pw.println("  mMinimumScreenOffTimeoutConfig=" + this.mMinimumScreenOffTimeoutConfig);
            pw.println("  mMaximumScreenDimDurationConfig=" + this.mMaximumScreenDimDurationConfig);
            pw.println("  mMaximumScreenDimRatioConfig=" + this.mMaximumScreenDimRatioConfig);
            pw.println("  mAttentiveTimeoutConfig=" + this.mAttentiveTimeoutConfig);
            pw.println("  mAttentiveTimeoutSetting=" + this.mAttentiveTimeoutSetting);
            pw.println("  mAttentiveWarningDurationConfig=" + this.mAttentiveWarningDurationConfig);
            pw.println("  mScreenOffTimeoutSetting=" + this.mScreenOffTimeoutSetting);
            pw.println("  mSleepTimeoutSetting=" + this.mSleepTimeoutSetting);
            pw.println("  mMaximumScreenOffTimeoutFromDeviceAdmin=" + this.mMaximumScreenOffTimeoutFromDeviceAdmin + " (enforced=" + isMaximumScreenOffTimeoutFromDeviceAdminEnforcedLocked() + ")");
            pw.println("  mStayOnWhilePluggedInSetting=" + this.mStayOnWhilePluggedInSetting);
            pw.println("  mScreenBrightnessModeSetting=" + this.mScreenBrightnessModeSetting);
            pw.println("  mScreenBrightnessOverrideFromWindowManager=" + this.mScreenBrightnessOverrideFromWindowManager);
            pw.println("  mUserActivityTimeoutOverrideFromWindowManager=" + this.mUserActivityTimeoutOverrideFromWindowManager);
            pw.println("  mUserInactiveOverrideFromWindowManager=" + this.mUserInactiveOverrideFromWindowManager);
            pw.println("  mDozeScreenStateOverrideFromDreamManager=" + this.mDozeScreenStateOverrideFromDreamManager);
            pw.println("  mDrawWakeLockOverrideFromSidekick=" + this.mDrawWakeLockOverrideFromSidekick);
            pw.println("  mDozeScreenBrightnessOverrideFromDreamManager=" + this.mDozeScreenBrightnessOverrideFromDreamManager);
            pw.println("  mScreenBrightnessMinimum=" + this.mScreenBrightnessMinimum);
            pw.println("  mScreenBrightnessMaximum=" + this.mScreenBrightnessMaximum);
            pw.println("  mScreenBrightnessDefault=" + this.mScreenBrightnessDefault);
            pw.println("  mDoubleTapWakeEnabled=" + this.mDoubleTapWakeEnabled);
            pw.println("  mIsVrModeEnabled=" + this.mIsVrModeEnabled);
            pw.println("  mForegroundProfile=" + this.mForegroundProfile);
            pw.println("  mUserId=" + this.mUserId);
            long attentiveTimeout = getAttentiveTimeoutLocked();
            long sleepTimeout = getSleepTimeoutLocked(attentiveTimeout);
            long screenOffTimeout = getScreenOffTimeoutLocked(sleepTimeout, attentiveTimeout);
            long screenDimDuration = getScreenDimDurationLocked(screenOffTimeout);
            pw.println();
            pw.println("Attentive timeout: " + attentiveTimeout + " ms");
            pw.println("Sleep timeout: " + sleepTimeout + " ms");
            pw.println("Screen off timeout: " + screenOffTimeout + " ms");
            pw.println("Screen dim duration: " + screenDimDuration + " ms");
            pw.println();
            pw.print("UID states (changing=");
            pw.print(this.mUidsChanging);
            pw.print(" changed=");
            pw.print(this.mUidsChanged);
            pw.println("):");
            for (int i = 0; i < this.mUidState.size(); i++) {
                UidState state = this.mUidState.valueAt(i);
                pw.print("  UID ");
                UserHandle.formatUid(pw, this.mUidState.keyAt(i));
                pw.print(": ");
                if (state.mActive) {
                    pw.print("  ACTIVE ");
                } else {
                    pw.print("INACTIVE ");
                }
                pw.print(" count=");
                pw.print(state.mNumWakeLocks);
                pw.print(" state=");
                pw.println(state.mProcState);
            }
            pw.println();
            pw.println("Looper state:");
            this.mHandler.getLooper().dump(new PrintWriterPrinter(pw), "  ");
            pw.println();
            pw.println("Wake Locks: size=" + this.mWakeLocks.size());
            Iterator<WakeLock> it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                WakeLock wl = it.next();
                pw.println("  " + wl);
            }
            pw.println();
            pw.println("Suspend Blockers: size=" + this.mSuspendBlockers.size());
            Iterator<SuspendBlocker> it2 = this.mSuspendBlockers.iterator();
            while (it2.hasNext()) {
                SuspendBlocker sb = it2.next();
                pw.println("  " + sb);
            }
            pw.println();
            pw.println("Display Power: " + this.mDisplayPowerCallbacks);
            this.mBatterySaverPolicy.dump(pw);
            this.mBatterySaverStateMachine.dump(pw);
            this.mAttentionDetector.dump(pw);
            pw.println();
            int numProfiles = this.mProfilePowerState.size();
            pw.println("Profile power states: size=" + numProfiles);
            int i2 = 0;
            while (i2 < numProfiles) {
                ProfilePowerState profile = this.mProfilePowerState.valueAt(i2);
                pw.print("  mUserId=");
                pw.print(profile.mUserId);
                pw.print(" mScreenOffTimeout=");
                pw.print(profile.mScreenOffTimeout);
                pw.print(" mWakeLockSummary=");
                pw.print(profile.mWakeLockSummary);
                pw.print(" mLastUserActivityTime=");
                pw.print(profile.mLastUserActivityTime);
                pw.print(" mLockingNotified=");
                pw.println(profile.mLockingNotified);
                i2++;
                attentiveTimeout = attentiveTimeout;
            }
            pw.println("Display Group User Activity:");
            for (int idx = 0; idx < this.mPowerGroups.size(); idx++) {
                PowerGroup powerGroup = this.mPowerGroups.valueAt(idx);
                pw.println("  displayGroupId=" + powerGroup.getGroupId());
                pw.println("  userActivitySummary=0x" + Integer.toHexString(powerGroup.getUserActivitySummaryLocked()));
                pw.println("  lastUserActivityTime=" + TimeUtils.formatUptime(powerGroup.getLastUserActivityTimeLocked()));
                pw.println("  lastUserActivityTimeNoChangeLights=" + TimeUtils.formatUptime(powerGroup.getLastUserActivityTimeNoChangeLightsLocked()));
                pw.println("  mWakeLockSummary=0x" + Integer.toHexString(powerGroup.getWakeLockSummaryLocked()));
            }
            wcd = this.mWirelessChargerDetector;
        }
        if (wcd != null) {
            wcd.dump(pw);
        }
        Notifier notifier = this.mNotifier;
        if (notifier != null) {
            notifier.dump(pw);
        }
        this.mFaceDownDetector.dump(pw);
        this.mAmbientDisplaySuppressionController.dump(pw);
        this.mLowPowerStandbyController.dump(pw);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpProto(FileDescriptor fd) {
        int[] iArr;
        int[] iArr2;
        WirelessChargerDetector wcd;
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        synchronized (this.mLock) {
            this.mConstants.dumpProto(proto);
            proto.write(1120986464258L, this.mDirty);
            proto.write(1159641169923L, getGlobalWakefulnessLocked());
            proto.write(1133871366148L, this.mWakefulnessChanging);
            proto.write(1133871366149L, this.mIsPowered);
            proto.write(1159641169926L, this.mPlugType);
            proto.write(1120986464263L, this.mBatteryLevel);
            proto.write(1120986464264L, this.mBatteryLevelWhenDreamStarted);
            proto.write(1159641169929L, this.mDockState);
            proto.write(1133871366154L, this.mStayOn);
            proto.write(1133871366155L, this.mProximityPositive);
            proto.write(1133871366156L, this.mBootCompleted);
            proto.write(1133871366157L, this.mSystemReady);
            synchronized (this.mEnhancedDischargeTimeLock) {
                proto.write(1112396529716L, this.mEnhancedDischargeTimeElapsed);
                proto.write(1112396529717L, this.mLastEnhancedDischargeTimeUpdatedElapsed);
                proto.write(1133871366198L, this.mEnhancedDischargePredictionIsPersonalized);
            }
            proto.write(1133871366158L, this.mHalAutoSuspendModeEnabled);
            proto.write(1133871366159L, this.mHalInteractiveModeEnabled);
            long activeWakeLocksToken = proto.start(1146756268048L);
            boolean z = true;
            long j = 1133871366145L;
            proto.write(1133871366145L, (this.mWakeLockSummary & 1) != 0);
            proto.write(1133871366146L, (this.mWakeLockSummary & 2) != 0);
            proto.write(1133871366147L, (this.mWakeLockSummary & 4) != 0);
            proto.write(1133871366148L, (this.mWakeLockSummary & 8) != 0);
            proto.write(1133871366149L, (this.mWakeLockSummary & 16) != 0);
            proto.write(1133871366150L, (this.mWakeLockSummary & 32) != 0);
            proto.write(1133871366151L, (this.mWakeLockSummary & 64) != 0);
            proto.write(1133871366152L, (this.mWakeLockSummary & 128) != 0);
            proto.end(activeWakeLocksToken);
            proto.write(1112396529681L, this.mNotifyLongScheduled);
            proto.write(1112396529682L, this.mNotifyLongDispatched);
            proto.write(1112396529683L, this.mNotifyLongNextCheck);
            int idx = 0;
            while (idx < this.mPowerGroups.size()) {
                PowerGroup powerGroup = this.mPowerGroups.valueAt(idx);
                long userActivityToken = proto.start(2246267895828L);
                proto.write(1120986464262L, powerGroup.getGroupId());
                long userActivitySummary = powerGroup.getUserActivitySummaryLocked();
                proto.write(j, (userActivitySummary & 1) != 0);
                proto.write(1133871366146L, (userActivitySummary & 2) != 0);
                proto.write(1133871366147L, (4 & userActivitySummary) != 0);
                proto.write(1112396529668L, powerGroup.getLastUserActivityTimeLocked());
                proto.write(1112396529669L, powerGroup.getLastUserActivityTimeNoChangeLightsLocked());
                proto.end(userActivityToken);
                idx++;
                j = 1133871366145L;
            }
            proto.write(1133871366165L, this.mRequestWaitForNegativeProximity);
            proto.write(1133871366166L, this.mSandmanScheduled);
            proto.write(1133871366168L, this.mBatteryLevelLow);
            proto.write(1133871366169L, this.mLightDeviceIdleMode);
            proto.write(1133871366170L, this.mDeviceIdleMode);
            for (int id : this.mDeviceIdleWhitelist) {
                proto.write(2220498092059L, id);
            }
            for (int id2 : this.mDeviceIdleTempWhitelist) {
                proto.write(2220498092060L, id2);
            }
            proto.write(1133871366199L, this.mLowPowerStandbyActive);
            proto.write(1112396529693L, this.mLastGlobalWakeTime);
            proto.write(1112396529694L, this.mLastGlobalSleepTime);
            proto.write(1112396529697L, this.mLastInteractivePowerHintTime);
            proto.write(1112396529698L, this.mLastScreenBrightnessBoostTime);
            proto.write(1133871366179L, this.mScreenBrightnessBoostInProgress);
            proto.write(1133871366181L, this.mHoldingWakeLockSuspendBlocker);
            proto.write(1133871366182L, this.mHoldingDisplaySuspendBlocker);
            long settingsAndConfigurationToken = proto.start(1146756268071L);
            proto.write(1133871366145L, this.mDecoupleHalAutoSuspendModeFromDisplayConfig);
            proto.write(1133871366146L, this.mDecoupleHalInteractiveModeFromDisplayConfig);
            proto.write(1133871366147L, this.mWakeUpWhenPluggedOrUnpluggedConfig);
            proto.write(1133871366148L, this.mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig);
            proto.write(1133871366149L, this.mTheaterModeEnabled);
            proto.write(1133871366150L, this.mSuspendWhenScreenOffDueToProximityConfig);
            proto.write(1133871366151L, this.mDreamsSupportedConfig);
            proto.write(1133871366152L, this.mDreamsEnabledByDefaultConfig);
            proto.write(1133871366153L, this.mDreamsActivatedOnSleepByDefaultConfig);
            proto.write(1133871366154L, this.mDreamsActivatedOnDockByDefaultConfig);
            proto.write(1133871366155L, this.mDreamsEnabledOnBatteryConfig);
            proto.write(1172526071820L, this.mDreamsBatteryLevelMinimumWhenPoweredConfig);
            proto.write(1172526071821L, this.mDreamsBatteryLevelMinimumWhenNotPoweredConfig);
            proto.write(1172526071822L, this.mDreamsBatteryLevelDrainCutoffConfig);
            proto.write(1133871366159L, this.mDreamsEnabledSetting);
            proto.write(1133871366160L, this.mDreamsActivateOnSleepSetting);
            proto.write(1133871366161L, this.mDreamsActivateOnDockSetting);
            proto.write(1133871366162L, this.mDozeAfterScreenOff);
            proto.write(1120986464275L, this.mMinimumScreenOffTimeoutConfig);
            proto.write(1120986464276L, this.mMaximumScreenDimDurationConfig);
            proto.write(1108101562389L, this.mMaximumScreenDimRatioConfig);
            proto.write(1120986464278L, this.mScreenOffTimeoutSetting);
            proto.write(1172526071831L, this.mSleepTimeoutSetting);
            proto.write(1172526071845L, this.mAttentiveTimeoutSetting);
            proto.write(1172526071846L, this.mAttentiveTimeoutConfig);
            proto.write(1172526071847L, this.mAttentiveWarningDurationConfig);
            proto.write(1120986464280L, Math.min(this.mMaximumScreenOffTimeoutFromDeviceAdmin, 2147483647L));
            proto.write(1133871366169L, isMaximumScreenOffTimeoutFromDeviceAdminEnforcedLocked());
            long stayOnWhilePluggedInToken = proto.start(1146756268058L);
            proto.write(1133871366145L, (this.mStayOnWhilePluggedInSetting & 1) != 0);
            proto.write(1133871366146L, (this.mStayOnWhilePluggedInSetting & 2) != 0);
            if ((this.mStayOnWhilePluggedInSetting & 4) == 0) {
                z = false;
            }
            proto.write(1133871366147L, z);
            proto.end(stayOnWhilePluggedInToken);
            proto.write(1159641169947L, this.mScreenBrightnessModeSetting);
            proto.write(1172526071836L, this.mScreenBrightnessOverrideFromWindowManager);
            proto.write(1176821039133L, this.mUserActivityTimeoutOverrideFromWindowManager);
            proto.write(1133871366174L, this.mUserInactiveOverrideFromWindowManager);
            proto.write(1159641169951L, this.mDozeScreenStateOverrideFromDreamManager);
            proto.write(1133871366180L, this.mDrawWakeLockOverrideFromSidekick);
            proto.write(1108101562400L, this.mDozeScreenBrightnessOverrideFromDreamManager);
            long screenBrightnessSettingLimitsToken = proto.start(1146756268065L);
            proto.write(1108101562372L, this.mScreenBrightnessMinimum);
            proto.write(1108101562373L, this.mScreenBrightnessMaximum);
            proto.write(1108101562374L, this.mScreenBrightnessDefault);
            proto.end(screenBrightnessSettingLimitsToken);
            proto.write(1133871366178L, this.mDoubleTapWakeEnabled);
            proto.write(1133871366179L, this.mIsVrModeEnabled);
            proto.end(settingsAndConfigurationToken);
            long attentiveTimeout = getAttentiveTimeoutLocked();
            long sleepTimeout = getSleepTimeoutLocked(attentiveTimeout);
            long screenOffTimeout = getScreenOffTimeoutLocked(sleepTimeout, attentiveTimeout);
            long screenDimDuration = getScreenDimDurationLocked(screenOffTimeout);
            proto.write(1172526071859L, attentiveTimeout);
            proto.write(1172526071848L, sleepTimeout);
            proto.write(1120986464297L, screenOffTimeout);
            long screenDimDuration2 = screenDimDuration;
            proto.write(1120986464298L, screenDimDuration2);
            proto.write(1133871366187L, this.mUidsChanging);
            proto.write(1133871366188L, this.mUidsChanged);
            int i = 0;
            while (i < this.mUidState.size()) {
                UidState state = this.mUidState.valueAt(i);
                long screenDimDuration3 = screenDimDuration2;
                long uIDToken = proto.start(2246267895853L);
                int uid = this.mUidState.keyAt(i);
                proto.write(CompanionMessage.MESSAGE_ID, uid);
                proto.write(1138166333442L, UserHandle.formatUid(uid));
                proto.write(1133871366147L, state.mActive);
                proto.write(1120986464260L, state.mNumWakeLocks);
                proto.write(1159641169925L, ActivityManager.processStateAmToProto(state.mProcState));
                proto.end(uIDToken);
                i++;
                screenDimDuration2 = screenDimDuration3;
                screenOffTimeout = screenOffTimeout;
                attentiveTimeout = attentiveTimeout;
            }
            this.mBatterySaverStateMachine.dumpProto(proto, 1146756268082L);
            this.mHandler.getLooper().dumpDebug(proto, 1146756268078L);
            Iterator<WakeLock> it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                WakeLock wl = it.next();
                wl.dumpDebug(proto, 2246267895855L);
            }
            Iterator<SuspendBlocker> it2 = this.mSuspendBlockers.iterator();
            while (it2.hasNext()) {
                SuspendBlocker sb = it2.next();
                sb.dumpDebug(proto, 2246267895856L);
            }
            wcd = this.mWirelessChargerDetector;
        }
        if (wcd != null) {
            wcd.dumpDebug(proto, 1146756268081L);
        }
        this.mLowPowerStandbyController.dumpProto(proto, 1146756268088L);
        proto.flush();
    }

    private void incrementBootCount() {
        int count;
        synchronized (this.mLock) {
            try {
                count = Settings.Global.getInt(getContext().getContentResolver(), "boot_count");
            } catch (Settings.SettingNotFoundException e) {
                count = 0;
            }
            Settings.Global.putInt(getContext().getContentResolver(), "boot_count", count + 1);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static WorkSource copyWorkSource(WorkSource workSource) {
        if (workSource != null) {
            return new WorkSource(workSource);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class BatteryReceiver extends BroadcastReceiver {
        BatteryReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            synchronized (PowerManagerService.this.mLock) {
                PowerManagerService.this.handleBatteryStateChangedLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class DreamReceiver extends BroadcastReceiver {
        private DreamReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            synchronized (PowerManagerService.this.mLock) {
                PowerManagerService.this.scheduleSandmanLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class UserSwitchedReceiver extends BroadcastReceiver {
        UserSwitchedReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            synchronized (PowerManagerService.this.mLock) {
                PowerManagerService.this.handleSettingsChangedLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class DockReceiver extends BroadcastReceiver {
        private DockReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            synchronized (PowerManagerService.this.mLock) {
                int dockState = intent.getIntExtra("android.intent.extra.DOCK_STATE", 0);
                if (PowerManagerService.this.mDockState != dockState) {
                    PowerManagerService.this.mDockState = dockState;
                    PowerManagerService.this.mDirty |= 1024;
                    PowerManagerService.this.updatePowerStateLocked();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            synchronized (PowerManagerService.this.mLock) {
                PowerManagerService.this.handleSettingsChangedLocked();
            }
        }
    }

    /* loaded from: classes2.dex */
    private final class PowerManagerHandlerCallback implements Handler.Callback {
        private PowerManagerHandlerCallback() {
        }

        @Override // android.os.Handler.Callback
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    PowerManagerService.this.handleUserActivityTimeout();
                    return true;
                case 2:
                    PowerManagerService.this.handleSandman(msg.arg1);
                    return true;
                case 3:
                    PowerManagerService.this.handleScreenBrightnessBoostTimeout();
                    return true;
                case 4:
                    PowerManagerService.this.checkForLongWakeLocks();
                    return true;
                case 5:
                    PowerManagerService.this.handleAttentiveTimeout();
                    return true;
                case 101:
                    ITranPowerManagerService.Instance().handleScreenOnBoostMessage();
                    return true;
                default:
                    return true;
            }
        }
    }

    private void startCheckWakelock() {
        boolean has = this.mHandlerWatchWakelock.hasMessages(1);
        if (!has) {
            if (sWakelockDebug) {
                Slog.d(TAG, "startCheckWakelock ...");
            }
            sendWakeLockCheckMessage(1L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleCheckWakelock() {
        synchronized (this.mLock) {
            try {
                if (sWakelockDebug) {
                    Slog.d(TAG, "numWakeLocks =" + this.mWakeLocks.size());
                }
                long minActiveDura = -1;
                for (int i = this.mWakeLocks.size() - 1; i >= 0; i--) {
                    WakeLock wakeLock = this.mWakeLocks.get(i);
                    if (1 != wakeLock.mFlags) {
                        if (sWakelockDebug) {
                            Slog.d(TAG, "this wakelock is not partial wakelock, packageName=" + wakeLock.mPackageName + ", tag=" + wakeLock.mTag);
                        }
                    } else {
                        long activeDura = SystemClock.uptimeMillis() - wakeLock.mActiveSince;
                        if (sWakelockDebug) {
                            Slog.d(TAG, "handleCheckWakelock, packageName=" + wakeLock.mPackageName + ", tag=" + wakeLock.mTag + ", activeDura= " + activeDura);
                        }
                        if (wakeLock.mTag == null) {
                            Slog.d(TAG, "handleCheckWakelock. wakelock tag is null");
                        } else if (!isMatchFilterCondition(wakeLock)) {
                            if (activeDura >= JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY) {
                                try {
                                    wakeLock.mLock.unlinkToDeath(wakeLock, 0);
                                    removeWakeLockLocked(wakeLock, i);
                                    Slog.i(TAG, "Force remove wakelock. package=" + wakeLock.mPackageName + ", tag=" + wakeLock.mTag + ", activeDuration=" + activeDura);
                                } catch (NoSuchElementException e) {
                                    Slog.e(TAG, "WakeLock already released, Death link does not exist. package=" + wakeLock.mPackageName + ", tag=" + wakeLock.mTag);
                                }
                            } else if (minActiveDura < 0 || activeDura < minActiveDura) {
                                minActiveDura = activeDura;
                            }
                        }
                    }
                }
                int i2 = (minActiveDura > 0L ? 1 : (minActiveDura == 0L ? 0 : -1));
                if (i2 >= 0) {
                    if (sWakelockDebug) {
                        Slog.d(TAG, "sendMessageDelayed =" + (JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY - minActiveDura));
                    }
                    sendWakeLockCheckMessage(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY - minActiveDura);
                }
            } catch (Exception e2) {
                Slog.e(TAG, "handleCheckWakelock was failed, exception=" + e2);
            }
        }
    }

    private boolean isMatchFilterCondition(WakeLock wakeLock) {
        if (wakeLock.mTag.contains("Audio")) {
            Slog.i(TAG, "isMatchFilterCondition. wakelock is audio. package=" + wakeLock.mPackageName + ", tag=" + wakeLock.mTag);
            return true;
        } else if (wakeLock.mTag.contains("FmService")) {
            Slog.i(TAG, "isMatchFilterCondition. wakelock is fm. package=" + wakeLock.mPackageName + ", tag=" + wakeLock.mTag);
            return true;
        } else if (wakeLock.mTag.contains("BtOppObexServer")) {
            Slog.i(TAG, "isMatchFilterCondition. wakelock is bt. package=" + wakeLock.mPackageName + ", tag=" + wakeLock.mTag);
            return true;
        } else {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class WatchWakelockHandler extends Handler {
        public WatchWakelockHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    PowerManagerService.this.handleCheckWakelock();
                    return;
                case 2:
                    PowerManagerService.this.handleCheckBlackWakelock();
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class WakeLock implements IBinder.DeathRecipient {
        public long mAcquireTime;
        public IWakeLockCallback mCallback;
        public boolean mDisabled;
        public final int mDisplayId;
        public int mFlags;
        public String mHistoryTag;
        public final IBinder mLock;
        public boolean mNotifiedAcquired;
        public boolean mNotifiedLong;
        public final int mOwnerPid;
        public final int mOwnerUid;
        public final String mPackageName;
        public String mTag;
        public final UidState mUidState;
        public WorkSource mWorkSource;
        public long mActiveSince = 0;
        public long mTotalTime = 0;

        public WakeLock(IBinder lock, int displayId, int flags, String tag, String packageName, WorkSource workSource, String historyTag, int ownerUid, int ownerPid, UidState uidState, IWakeLockCallback callback) {
            this.mLock = lock;
            this.mDisplayId = displayId;
            this.mFlags = flags;
            this.mTag = tag;
            this.mPackageName = packageName;
            this.mWorkSource = PowerManagerService.copyWorkSource(workSource);
            this.mHistoryTag = historyTag;
            this.mOwnerUid = ownerUid;
            this.mOwnerPid = ownerPid;
            this.mUidState = uidState;
            this.mCallback = callback;
            linkToDeath();
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            unlinkToDeath();
            PowerManagerService.this.handleWakeLockDeath(this);
        }

        private void linkToDeath() {
            try {
                this.mLock.linkToDeath(this, 0);
            } catch (RemoteException e) {
                throw new IllegalArgumentException("Wakelock.mLock is already dead.");
            }
        }

        void unlinkToDeath() {
            try {
                this.mLock.unlinkToDeath(this, 0);
            } catch (NoSuchElementException e) {
                Slog.wtf(PowerManagerService.TAG, "Failed to unlink Wakelock.mLock", e);
            }
        }

        public boolean setDisabled(boolean disabled) {
            if (this.mDisabled != disabled) {
                this.mDisabled = disabled;
                return true;
            }
            return false;
        }

        public boolean hasSameProperties(int flags, String tag, WorkSource workSource, int ownerUid, int ownerPid, IWakeLockCallback callback) {
            return this.mFlags == flags && this.mTag.equals(tag) && hasSameWorkSource(workSource) && this.mOwnerUid == ownerUid && this.mOwnerPid == ownerPid;
        }

        public void updateProperties(int flags, String tag, String packageName, WorkSource workSource, String historyTag, int ownerUid, int ownerPid, IWakeLockCallback callback) {
            if (!this.mPackageName.equals(packageName)) {
                throw new IllegalStateException("Existing wake lock package name changed: " + this.mPackageName + " to " + packageName);
            }
            if (this.mOwnerUid != ownerUid) {
                throw new IllegalStateException("Existing wake lock uid changed: " + this.mOwnerUid + " to " + ownerUid);
            }
            if (this.mOwnerPid != ownerPid) {
                throw new IllegalStateException("Existing wake lock pid changed: " + this.mOwnerPid + " to " + ownerPid);
            }
            this.mFlags = flags;
            this.mTag = tag;
            updateWorkSource(workSource);
            this.mHistoryTag = historyTag;
            this.mCallback = callback;
        }

        public boolean hasSameWorkSource(WorkSource workSource) {
            return Objects.equals(this.mWorkSource, workSource);
        }

        public void updateWorkSource(WorkSource workSource) {
            this.mWorkSource = PowerManagerService.copyWorkSource(workSource);
        }

        public Integer getPowerGroupId() {
            if (!PowerManagerService.this.mSystemReady || this.mDisplayId == -1) {
                return -1;
            }
            DisplayInfo displayInfo = PowerManagerService.this.mDisplayManagerInternal.getDisplayInfo(this.mDisplayId);
            if (displayInfo != null) {
                return Integer.valueOf(displayInfo.displayGroupId);
            }
            return null;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getLockLevelString());
            sb.append(" '");
            sb.append(this.mTag);
            sb.append("'");
            sb.append(getLockFlagsString());
            if (this.mDisabled) {
                sb.append(" DISABLED");
            }
            if (this.mNotifiedAcquired) {
                sb.append(" ACQ=");
                TimeUtils.formatDuration(this.mAcquireTime - PowerManagerService.this.mClock.uptimeMillis(), sb);
            }
            if (this.mNotifiedLong) {
                sb.append(" LONG");
            }
            sb.append(" (uid=");
            sb.append(this.mOwnerUid);
            if (this.mOwnerPid != 0) {
                sb.append(" pid=");
                sb.append(this.mOwnerPid);
            }
            if (this.mWorkSource != null) {
                sb.append(" ws=");
                sb.append(this.mWorkSource);
            }
            sb.append(")");
            return sb.toString();
        }

        public void dumpDebug(ProtoOutputStream proto, long fieldId) {
            long wakeLockToken = proto.start(fieldId);
            proto.write(1159641169921L, this.mFlags & GnssNative.GNSS_AIDING_TYPE_ALL);
            proto.write(1138166333442L, this.mTag);
            long wakeLockFlagsToken = proto.start(1146756268035L);
            proto.write(1133871366145L, (this.mFlags & 268435456) != 0);
            proto.write(1133871366146L, (this.mFlags & 536870912) != 0);
            proto.write(1133871366147L, (this.mFlags & Integer.MIN_VALUE) != 0);
            proto.end(wakeLockFlagsToken);
            proto.write(1133871366148L, this.mDisabled);
            if (this.mNotifiedAcquired) {
                proto.write(1112396529669L, this.mAcquireTime);
            }
            proto.write(1133871366150L, this.mNotifiedLong);
            proto.write(1120986464263L, this.mOwnerUid);
            proto.write(1120986464264L, this.mOwnerPid);
            WorkSource workSource = this.mWorkSource;
            if (workSource != null) {
                workSource.dumpDebug(proto, 1146756268041L);
            }
            proto.end(wakeLockToken);
        }

        private String getLockLevelString() {
            switch (this.mFlags & GnssNative.GNSS_AIDING_TYPE_ALL) {
                case 1:
                    return "PARTIAL_WAKE_LOCK             ";
                case 6:
                    return "SCREEN_DIM_WAKE_LOCK          ";
                case 10:
                    return "SCREEN_BRIGHT_WAKE_LOCK       ";
                case 26:
                    return "FULL_WAKE_LOCK                ";
                case 32:
                    return "PROXIMITY_SCREEN_OFF_WAKE_LOCK";
                case 64:
                    return "DOZE_WAKE_LOCK                ";
                case 128:
                    return "DRAW_WAKE_LOCK                ";
                default:
                    return "???                           ";
            }
        }

        private String getLockFlagsString() {
            String result = (this.mFlags & 268435456) != 0 ? " ACQUIRE_CAUSES_WAKEUP" : "";
            if ((this.mFlags & 536870912) != 0) {
                result = result + " ON_AFTER_RELEASE";
            }
            if ((this.mFlags & Integer.MIN_VALUE) != 0) {
                return result + " SYSTEM_WAKELOCK";
            }
            return result;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class SuspendBlockerImpl implements SuspendBlocker {
        private static final String UNKNOWN_ID = "unknown";
        private final String mName;
        private final ArrayMap<String, LongArray> mOpenReferenceTimes = new ArrayMap<>();
        private int mReferenceCount;
        private final String mTraceName;

        public SuspendBlockerImpl(String name) {
            this.mName = name;
            this.mTraceName = "SuspendBlocker (" + name + ")";
        }

        protected void finalize() throws Throwable {
            try {
                if (this.mReferenceCount != 0) {
                    Slog.wtf(PowerManagerService.TAG, "Suspend blocker \"" + this.mName + "\" was finalized without being released!");
                    this.mReferenceCount = 0;
                    PowerManagerService.this.mNativeWrapper.nativeReleaseSuspendBlocker(this.mName);
                    Trace.asyncTraceEnd(131072L, this.mTraceName, 0);
                }
            } finally {
                super.finalize();
            }
        }

        @Override // com.android.server.power.SuspendBlocker
        public void acquire() {
            acquire("unknown");
        }

        @Override // com.android.server.power.SuspendBlocker
        public void acquire(String id) {
            synchronized (this) {
                recordReferenceLocked(id);
                int i = this.mReferenceCount + 1;
                this.mReferenceCount = i;
                if (i == 1) {
                    if (PowerManagerService.DEBUG_SPEW) {
                        Slog.d(PowerManagerService.TAG, "Acquiring suspend blocker \"" + this.mName + "\".");
                    }
                    Trace.asyncTraceBegin(131072L, this.mTraceName, 0);
                    PowerManagerService.this.mNativeWrapper.nativeAcquireSuspendBlocker(this.mName);
                }
            }
        }

        @Override // com.android.server.power.SuspendBlocker
        public void release() {
            release("unknown");
        }

        @Override // com.android.server.power.SuspendBlocker
        public void release(String id) {
            synchronized (this) {
                removeReferenceLocked(id);
                int i = this.mReferenceCount - 1;
                this.mReferenceCount = i;
                if (i == 0) {
                    if (PowerManagerService.DEBUG_SPEW) {
                        Slog.d(PowerManagerService.TAG, "Releasing suspend blocker \"" + this.mName + "\".");
                    }
                    PowerManagerService.this.mNativeWrapper.nativeReleaseSuspendBlocker(this.mName);
                    Trace.asyncTraceEnd(131072L, this.mTraceName, 0);
                } else if (i < 0) {
                    Slog.wtf(PowerManagerService.TAG, "Suspend blocker \"" + this.mName + "\" was released without being acquired!", new Throwable());
                    this.mReferenceCount = 0;
                }
            }
        }

        public String toString() {
            String sb;
            synchronized (this) {
                StringBuilder builder = new StringBuilder();
                builder.append(this.mName);
                builder.append(": ref count=").append(this.mReferenceCount);
                builder.append(" [");
                int size = this.mOpenReferenceTimes.size();
                for (int i = 0; i < size; i++) {
                    String id = this.mOpenReferenceTimes.keyAt(i);
                    LongArray times = this.mOpenReferenceTimes.valueAt(i);
                    if (times != null && times.size() != 0) {
                        if (i > 0) {
                            builder.append(", ");
                        }
                        builder.append(id).append(": (");
                        for (int j = 0; j < times.size(); j++) {
                            if (j > 0) {
                                builder.append(", ");
                            }
                            builder.append(PowerManagerService.DATE_FORMAT.format(new Date(times.get(j))));
                        }
                        builder.append(")");
                    }
                }
                builder.append("]");
                sb = builder.toString();
            }
            return sb;
        }

        @Override // com.android.server.power.SuspendBlocker
        public void dumpDebug(ProtoOutputStream proto, long fieldId) {
            long sbToken = proto.start(fieldId);
            synchronized (this) {
                proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, this.mName);
                proto.write(1120986464258L, this.mReferenceCount);
            }
            proto.end(sbToken);
        }

        private void recordReferenceLocked(String id) {
            LongArray times = this.mOpenReferenceTimes.get(id);
            if (times == null) {
                times = new LongArray();
                this.mOpenReferenceTimes.put(id, times);
            }
            times.add(System.currentTimeMillis());
        }

        private void removeReferenceLocked(String id) {
            LongArray times = this.mOpenReferenceTimes.get(id);
            if (times != null && times.size() > 0) {
                times.remove(times.size() - 1);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class UidState {
        boolean mActive;
        int mNumWakeLocks;
        int mProcState;
        final int mUid;

        UidState(int uid) {
            this.mUid = uid;
        }
    }

    /* loaded from: classes2.dex */
    final class BinderService extends IPowerManager.Stub {
        BinderService() {
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.power.PowerManagerService$BinderService */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new PowerManagerShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
        }

        private void powerBoostForLucid(int boost, int durationMs) {
            if (boost == 255) {
                PowerManagerService.setLucidFactor(durationMs);
                if (PowerManagerService.TRAN_LUCID_OPTIMIZATION_SUPPORT) {
                    PowerManagerService.this.updatePowerStateInternalForLucid();
                }
            }
        }

        public void acquireWakeLockWithUid(IBinder lock, int flags, String tag, String packageName, int uid, int displayId, IWakeLockCallback callback) {
            int uid2;
            if (uid >= 0) {
                uid2 = uid;
            } else {
                uid2 = Binder.getCallingUid();
            }
            acquireWakeLock(lock, flags, tag, packageName, new WorkSource(uid2), null, displayId, callback);
        }

        public void setPowerBoost(int boost, int durationMs) {
            if (!PowerManagerService.this.mSystemReady) {
                return;
            }
            if (!PowerManagerService.mIsLucidDisabled) {
                powerBoostForLucid(boost, durationMs);
            }
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            PowerManagerService.this.setPowerBoostInternal(boost, durationMs);
        }

        public void setPowerMode(int mode, boolean enabled) {
            if (!PowerManagerService.this.mSystemReady) {
                return;
            }
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            PowerManagerService.this.setPowerModeInternal(mode, enabled);
        }

        public boolean setPowerModeChecked(int mode, boolean enabled) {
            if (!PowerManagerService.this.mSystemReady) {
                return false;
            }
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            return PowerManagerService.this.setPowerModeInternal(mode, enabled);
        }

        public void acquireWakeLock(IBinder lock, int flags, String tag, String packageName, WorkSource ws, String historyTag, int displayId, IWakeLockCallback callback) {
            WorkSource ws2;
            WorkSource ws3;
            int uid;
            int pid;
            if (lock == null) {
                throw new IllegalArgumentException("lock must not be null");
            }
            if (packageName == null) {
                throw new IllegalArgumentException("packageName must not be null");
            }
            if (PowerManagerService.sTranSlmSupport && !ITranPowerManagerService.Instance().isAllowAcquireWakeLock(isInteractive(), packageName, flags)) {
                return;
            }
            PowerManager.validateWakeLockParameters(flags, tag);
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", null);
            if ((flags & 64) != 0) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            }
            if (ws != null && !ws.isEmpty()) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", null);
                ws2 = ws;
            } else {
                ws2 = null;
            }
            int uid2 = Binder.getCallingUid();
            int pid2 = Binder.getCallingPid();
            if ((Integer.MIN_VALUE & flags) == 0) {
                ws3 = ws2;
                uid = uid2;
                pid = pid2;
            } else {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                WorkSource workSource = new WorkSource(Binder.getCallingUid(), packageName);
                if (ws2 != null && !ws2.isEmpty()) {
                    workSource.add(ws2);
                }
                int uid3 = Process.myUid();
                int pid3 = Process.myPid();
                ws3 = workSource;
                uid = uid3;
                pid = pid3;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.acquireWakeLockInternal(lock, displayId, flags, tag, packageName, ws3, historyTag, uid, pid, callback);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void acquireWakeLockAsync(IBinder lock, int flags, String tag, String packageName, WorkSource ws, String historyTag) {
            acquireWakeLock(lock, flags, tag, packageName, ws, historyTag, -1, null);
        }

        public void releaseWakeLock(IBinder lock, int flags) {
            if (lock == null) {
                throw new IllegalArgumentException("lock must not be null");
            }
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", null);
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.releaseWakeLockInternal(lock, flags);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void releaseWakeLockAsync(IBinder lock, int flags) {
            releaseWakeLock(lock, flags);
        }

        public void updateWakeLockUids(IBinder lock, int[] uids) {
            WorkSource ws = null;
            if (uids != null) {
                ws = new WorkSource();
                for (int uid : uids) {
                    ws.add(uid);
                }
            }
            updateWakeLockWorkSource(lock, ws, null);
        }

        public void updateWakeLockUidsAsync(IBinder lock, int[] uids) {
            updateWakeLockUids(lock, uids);
        }

        public void updateWakeLockWorkSource(IBinder lock, WorkSource ws, String historyTag) {
            if (lock == null) {
                throw new IllegalArgumentException("lock must not be null");
            }
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", null);
            if (ws != null && !ws.isEmpty()) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", null);
            } else {
                ws = null;
            }
            int callingUid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.updateWakeLockWorkSourceInternal(lock, ws, historyTag, callingUid);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void updateWakeLockCallback(IBinder lock, IWakeLockCallback callback) {
            if (lock == null) {
                throw new IllegalArgumentException("lock must not be null");
            }
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", null);
            int callingUid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.updateWakeLockCallbackInternal(lock, callback, callingUid);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isWakeLockLevelSupported(int level) {
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.isWakeLockLevelSupportedInternal(level);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void userActivity(int displayId, long eventTime, int event, int flags) {
            long now = PowerManagerService.this.mClock.uptimeMillis();
            if (PowerManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.DEVICE_POWER") != 0 && PowerManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.USER_ACTIVITY") != 0) {
                synchronized (PowerManagerService.this.mLock) {
                    if (now >= PowerManagerService.this.mLastWarningAboutUserActivityPermission + BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS) {
                        PowerManagerService.this.mLastWarningAboutUserActivityPermission = now;
                        Slog.w(PowerManagerService.TAG, "Ignoring call to PowerManager.userActivity() because the caller does not have DEVICE_POWER or USER_ACTIVITY permission.  Please fix your app!   pid=" + Binder.getCallingPid() + " uid=" + Binder.getCallingUid());
                    }
                }
            } else if (eventTime > now) {
                throw new IllegalArgumentException("event time must not be in the future");
            } else {
                int uid = Binder.getCallingUid();
                long ident = Binder.clearCallingIdentity();
                try {
                    PowerManagerService.this.userActivityInternal(displayId, eventTime, event, flags, uid);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }

        public void wakeUp(long eventTime, int reason, String details, String opPackageName) {
            if (eventTime > PowerManagerService.this.mClock.uptimeMillis()) {
                throw new IllegalArgumentException("event time must not be in the future");
            }
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (PowerManagerService.this.mLock) {
                    if (!PowerManagerService.this.mBootCompleted && PowerManagerService.sQuiescent) {
                        PowerManagerService.this.mDirty |= 4096;
                        PowerManagerService.this.updatePowerStateLocked();
                        return;
                    }
                    PowerManagerService powerManagerService = PowerManagerService.this;
                    powerManagerService.wakePowerGroupLocked((PowerGroup) powerManagerService.mPowerGroups.get(0), eventTime, reason, details, uid, opPackageName, uid);
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void goToSleep(long eventTime, int reason, int flags) {
            if (eventTime > PowerManagerService.this.mClock.uptimeMillis()) {
                throw new IllegalArgumentException("event time must not be in the future");
            }
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (PowerManagerService.this.mLock) {
                    PowerGroup defaultPowerGroup = (PowerGroup) PowerManagerService.this.mPowerGroups.get(0);
                    if ((flags & 1) != 0) {
                        PowerManagerService.this.sleepPowerGroupLocked(defaultPowerGroup, eventTime, reason, uid);
                    } else if (PowerManagerService.TRAN_AOD_SUPPORT && PowerManagerService.this.mDozeAfterScreenOff && (defaultPowerGroup.getWakeLockSummaryLocked() & 64) != 0) {
                        Slog.i(PowerManagerService.TAG, "DOZE_WAKE_LOCK is not relesed,do not go to sleep...");
                    } else {
                        PowerManagerService.this.dozePowerGroupLocked(defaultPowerGroup, eventTime, reason, uid);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void nap(long eventTime) {
            if (eventTime > PowerManagerService.this.mClock.uptimeMillis()) {
                throw new IllegalArgumentException("event time must not be in the future");
            }
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (PowerManagerService.this.mLock) {
                    PowerManagerService powerManagerService = PowerManagerService.this;
                    powerManagerService.dreamPowerGroupLocked((PowerGroup) powerManagerService.mPowerGroups.get(0), eventTime, uid);
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public float getBrightnessConstraint(int constraint) {
            switch (constraint) {
                case 0:
                    return PowerManagerService.this.mScreenBrightnessMinimum;
                case 1:
                    return PowerManagerService.this.mScreenBrightnessMaximum;
                case 2:
                    return PowerManagerService.this.mScreenBrightnessDefault;
                case 3:
                    return PowerManagerService.this.mScreenBrightnessDim;
                case 4:
                    return PowerManagerService.this.mScreenBrightnessDoze;
                case 5:
                    return PowerManagerService.this.mScreenBrightnessMinimumVr;
                case 6:
                    return PowerManagerService.this.mScreenBrightnessMaximumVr;
                case 7:
                    return PowerManagerService.this.mScreenBrightnessDefaultVr;
                default:
                    return Float.NaN;
            }
        }

        public boolean isInteractive() {
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.isInteractiveInternal();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isPowerSaveMode() {
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.mBatterySaverController.isEnabled();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public PowerSaveState getPowerSaveState(int serviceType) {
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.mBatterySaverPolicy.getBatterySaverPolicy(serviceType);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean setPowerSaveModeEnabled(boolean enabled) {
            if (PowerManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.POWER_SAVER") != 0) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            }
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.setLowPowerModeInternal(enabled);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public BatterySaverPolicyConfig getFullPowerSavePolicy() {
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.mBatterySaverStateMachine.getFullBatterySaverPolicy();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean setFullPowerSavePolicy(BatterySaverPolicyConfig config) {
            if (PowerManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.POWER_SAVER") != 0) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", "setFullPowerSavePolicy");
            }
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.mBatterySaverStateMachine.setFullBatterySaverPolicy(config);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean setDynamicPowerSaveHint(boolean powerSaveHint, int disableThreshold) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.POWER_SAVER", "updateDynamicPowerSavings");
            long ident = Binder.clearCallingIdentity();
            try {
                ContentResolver resolver = PowerManagerService.this.mContext.getContentResolver();
                boolean success = Settings.Global.putInt(resolver, "dynamic_power_savings_disable_threshold", disableThreshold);
                if (success) {
                    success &= Settings.Global.putInt(resolver, "dynamic_power_savings_enabled", powerSaveHint ? 1 : 0);
                }
                return success;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean setAdaptivePowerSavePolicy(BatterySaverPolicyConfig config) {
            if (PowerManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.POWER_SAVER") != 0) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", "setAdaptivePowerSavePolicy");
            }
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.mBatterySaverStateMachine.setAdaptiveBatterySaverPolicy(config);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean setAdaptivePowerSaveEnabled(boolean enabled) {
            if (PowerManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.POWER_SAVER") != 0) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", "setAdaptivePowerSaveEnabled");
            }
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.mBatterySaverStateMachine.setAdaptiveBatterySaverEnabled(enabled);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public int getPowerSaveModeTrigger() {
            long ident = Binder.clearCallingIdentity();
            try {
                return Settings.Global.getInt(PowerManagerService.this.mContext.getContentResolver(), "automatic_power_save_mode", 0);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void setBatteryDischargePrediction(ParcelDuration timeRemaining, boolean isPersonalized) {
            long nowElapsed = SystemClock.elapsedRealtime();
            if (PowerManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.BATTERY_PREDICTION") != 0) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", "setBatteryDischargePrediction");
            }
            long timeRemainingMs = timeRemaining.getDuration().toMillis();
            Preconditions.checkArgumentPositive((float) timeRemainingMs, "Given time remaining is not positive: " + timeRemainingMs);
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (PowerManagerService.this.mLock) {
                    if (PowerManagerService.this.mIsPowered) {
                        throw new IllegalStateException("Discharge prediction can't be set while the device is charging");
                    }
                }
                synchronized (PowerManagerService.this.mEnhancedDischargeTimeLock) {
                    if (PowerManagerService.this.mLastEnhancedDischargeTimeUpdatedElapsed > nowElapsed) {
                        return;
                    }
                    long broadcastDelayMs = Math.max(0L, 60000 - (nowElapsed - PowerManagerService.this.mLastEnhancedDischargeTimeUpdatedElapsed));
                    PowerManagerService.this.mEnhancedDischargeTimeElapsed = nowElapsed + timeRemainingMs;
                    PowerManagerService.this.mEnhancedDischargePredictionIsPersonalized = isPersonalized;
                    PowerManagerService.this.mLastEnhancedDischargeTimeUpdatedElapsed = nowElapsed;
                    PowerManagerService.this.mNotifier.postEnhancedDischargePredictionBroadcast(broadcastDelayMs);
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        private boolean isEnhancedDischargePredictionValidLocked(long nowElapsed) {
            return PowerManagerService.this.mLastEnhancedDischargeTimeUpdatedElapsed > 0 && nowElapsed < PowerManagerService.this.mEnhancedDischargeTimeElapsed && nowElapsed - PowerManagerService.this.mLastEnhancedDischargeTimeUpdatedElapsed < 1800000;
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [6757=5] */
        public ParcelDuration getBatteryDischargePrediction() {
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (PowerManagerService.this.mLock) {
                    if (PowerManagerService.this.mIsPowered) {
                        return null;
                    }
                    synchronized (PowerManagerService.this.mEnhancedDischargeTimeLock) {
                        long nowElapsed = SystemClock.elapsedRealtime();
                        if (isEnhancedDischargePredictionValidLocked(nowElapsed)) {
                            return new ParcelDuration(PowerManagerService.this.mEnhancedDischargeTimeElapsed - nowElapsed);
                        }
                        return new ParcelDuration(PowerManagerService.this.mBatteryStats.computeBatteryTimeRemaining());
                    }
                }
            } catch (RemoteException e) {
                return null;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isBatteryDischargePredictionPersonalized() {
            boolean z;
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (PowerManagerService.this.mEnhancedDischargeTimeLock) {
                    z = isEnhancedDischargePredictionValidLocked(SystemClock.elapsedRealtime()) && PowerManagerService.this.mEnhancedDischargePredictionIsPersonalized;
                }
                return z;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isDeviceIdleMode() {
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.isDeviceIdleModeInternal();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isLightDeviceIdleMode() {
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.isLightDeviceIdleModeInternal();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isLowPowerStandbySupported() {
            if (PowerManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.DEVICE_POWER") != 0) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_LOW_POWER_STANDBY", "isLowPowerStandbySupported");
            }
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.mLowPowerStandbyController.isSupported();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isLowPowerStandbyEnabled() {
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.mLowPowerStandbyController.isEnabled();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void setLowPowerStandbyEnabled(boolean enabled) {
            if (PowerManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.DEVICE_POWER") != 0) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_LOW_POWER_STANDBY", "setLowPowerStandbyEnabled");
            }
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.mLowPowerStandbyController.setEnabled(enabled);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void setLowPowerStandbyActiveDuringMaintenance(boolean activeDuringMaintenance) {
            if (PowerManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.DEVICE_POWER") != 0) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_LOW_POWER_STANDBY", "setLowPowerStandbyActiveDuringMaintenance");
            }
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.mLowPowerStandbyController.setActiveDuringMaintenance(activeDuringMaintenance);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void forceLowPowerStandbyActive(boolean active) {
            if (PowerManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.DEVICE_POWER") != 0) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_LOW_POWER_STANDBY", "forceLowPowerStandbyActive");
            }
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.mLowPowerStandbyController.forceActive(active);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public int getLastShutdownReason() {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.getLastShutdownReasonInternal();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public int getLastSleepReason() {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.getLastSleepReasonInternal();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void reboot(boolean confirm, String reason, boolean wait) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.REBOOT", null);
            if ("recovery".equals(reason) || "recovery-update".equals(reason)) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.RECOVERY", null);
            }
            ShutdownCheckPoints.recordCheckPoint(Binder.getCallingPid(), reason);
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.shutdownOrRebootInternal(1, confirm, reason, wait);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void rebootSafeMode(boolean confirm, boolean wait) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.REBOOT", null);
            ShutdownCheckPoints.recordCheckPoint(Binder.getCallingPid(), "safemode");
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.shutdownOrRebootInternal(2, confirm, "safemode", wait);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void shutdown(boolean confirm, String reason, boolean wait) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.REBOOT", null);
            ShutdownCheckPoints.recordCheckPoint(Binder.getCallingPid(), reason);
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.shutdownOrRebootInternal(0, confirm, reason, wait);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void crash(String message) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.REBOOT", null);
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.crashInternal(message);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void setStayOnSetting(int val) {
            int uid = Binder.getCallingUid();
            if (uid != 0 && !Settings.checkAndNoteWriteSettingsOperation(PowerManagerService.this.mContext, uid, Settings.getPackageNameForUid(PowerManagerService.this.mContext, uid), null, true)) {
                return;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.setStayOnSettingInternal(val);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void setAttentionLight(boolean on, int color) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.setAttentionLightInternal(on, color);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void setDozeAfterScreenOff(boolean on) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                if (!PowerManagerService.TRAN_AOD_SUPPORT) {
                    PowerManagerService.this.setDozeAfterScreenOffInternal(on);
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isAmbientDisplayAvailable() {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.READ_DREAM_STATE", null);
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.mAmbientDisplayConfiguration.ambientDisplayAvailable();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void suppressAmbientDisplay(String token, boolean suppress) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_DREAM_STATE", null);
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.mAmbientDisplaySuppressionController.suppress(token, uid, suppress);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isAmbientDisplaySuppressedForToken(String token) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.READ_DREAM_STATE", null);
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.mAmbientDisplaySuppressionController.isSuppressed(token, uid);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isAmbientDisplaySuppressedForTokenByApp(String token, int appUid) {
            boolean z;
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.READ_DREAM_STATE", null);
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.READ_DREAM_SUPPRESSION", null);
            long ident = Binder.clearCallingIdentity();
            try {
                if (isAmbientDisplayAvailable()) {
                    if (PowerManagerService.this.mAmbientDisplaySuppressionController.isSuppressed(token, appUid)) {
                        z = true;
                        return z;
                    }
                }
                z = false;
                return z;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isAmbientDisplaySuppressed() {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.READ_DREAM_STATE", null);
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.mAmbientDisplaySuppressionController.isSuppressed();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void boostScreenBrightness(long eventTime) {
            if (eventTime > PowerManagerService.this.mClock.uptimeMillis()) {
                throw new IllegalArgumentException("event time must not be in the future");
            }
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.boostScreenBrightnessInternal(eventTime, uid);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isScreenBrightnessBoosted() {
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.isScreenBrightnessBoostedInternal();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean forceSuspend() {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.forceSuspendInternal(uid);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public List<String> getWakeLockPkgs() {
            ArrayList<String> list;
            synchronized (PowerManagerService.this.mLock) {
                list = new ArrayList<>(PowerManagerService.this.mWakeLockPkgs.size());
                for (String val : PowerManagerService.this.mWakeLockPkgs) {
                    list.add(val);
                }
            }
            return list;
        }

        public void setWakeLockAppMap(String packageName, boolean on) {
            if (Binder.getCallingUid() == 1000) {
                ITranPowerManagerService.Instance().setWakeLockApp(packageName, on);
                return;
            }
            throw new SecurityException("setWakeLockAppMap called from non-system proc");
        }

        public Map getWakeLockAppMap() {
            if (Binder.getCallingUid() != 1000 && Binder.getCallingUid() != 1001) {
                throw new SecurityException("getWakeLockAppMap called from non-system proc");
            }
            return ITranPowerManagerService.Instance().getWakeLockAppMap();
        }

        public synchronized List<String> getAcquireableWakeLockApp() {
            if (Binder.getCallingUid() != 1000 && Binder.getCallingUid() != 1001) {
                throw new SecurityException("getAcquireableWakeLockApp called from non-system proc");
            }
            return ITranPowerManagerService.Instance().getAcquirableWakeLockAppList();
        }

        public synchronized List<String> getUnacquireableWakeLockApp() {
            if (Binder.getCallingUid() != 1000 && Binder.getCallingUid() != 1001) {
                throw new SecurityException("getUnacquireableWakeLockApp called from non-system proc");
            }
            return ITranPowerManagerService.Instance().getUnacquirableWakeLockAppList();
        }

        public void setScreenOnManagerEnable(boolean enable) {
            if (Binder.getCallingUid() == 1000) {
                ITranPowerManagerService.Instance().setScreenOnManagementEnable(enable);
                return;
            }
            throw new SecurityException("setScreenOnManagerEnable called from non-system proc");
        }

        public boolean getScreenOnManagerEnable() {
            if (Binder.getCallingUid() != 1000 && Binder.getCallingUid() != 1001) {
                throw new SecurityException("getScreenOnManagerEnable called from non-system proc");
            }
            return ITranPowerManagerService.Instance().isScreenOnManagementEnabled();
        }

        public void setTorch(boolean on) {
            ITranPowerManagerService.Instance().enableTorch(on);
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(PowerManagerService.this.mContext, PowerManagerService.TAG, pw)) {
                long ident = Binder.clearCallingIdentity();
                boolean isDumpProto = false;
                for (String arg : args) {
                    if (arg.equals("--proto")) {
                        isDumpProto = true;
                        break;
                    }
                }
                try {
                    if (isDumpProto) {
                        PowerManagerService.this.dumpProto(fd);
                    } else if (ITranPowerManagerService.Instance().doDump(fd, pw, args)) {
                        String opt = null;
                        if (0 < args.length) {
                            opt = args[0];
                        }
                        if (opt != null && "powerdebugon".equals(opt)) {
                            int i = 0 + 1;
                            PowerManagerService.DEBUG = true;
                            PowerManagerService.sWakelockDebug = true;
                            PowerManagerService.DEBUG_SPEW = true;
                        } else if (opt != null && "powerdebugoff".equals(opt)) {
                            int i2 = 0 + 1;
                            PowerManagerService.DEBUG = false;
                            PowerManagerService.sWakelockDebug = false;
                            PowerManagerService.DEBUG_SPEW = false;
                        }
                        pw.println(" DEBUG = " + PowerManagerService.DEBUG + ", sWakelockDebug=" + PowerManagerService.sWakelockDebug + ", DEBUG_SPEW=" + PowerManagerService.DEBUG_SPEW);
                    } else {
                        PowerManagerService.this.dumpInternal(pw);
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }

        public List<String> getAmbientDisplaySuppressionTokens() {
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                return PowerManagerService.this.mAmbientDisplaySuppressionController.getSuppressionTokens(uid);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void isNotifyScreenOn(boolean notify, long dimDuration) {
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.isNotifyScreenOnInternal(notify, dimDuration);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void notifyChangeConnectState(boolean connect) {
            long ident = Binder.clearCallingIdentity();
            try {
                ITranPowerManagerService.Instance().notifyChangeConnectState(connect);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    public void updatePowerStateInternalForLucid() {
        synchronized (this.mLock) {
            this.mDisplayManagerInternal.updatePowerStateForLucid();
        }
    }

    BinderService getBinderServiceInstance() {
        return this.mBinderService;
    }

    LocalService getLocalServiceInstance() {
        return this.mLocalService;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    int getLastShutdownReasonInternal() {
        char c;
        String line = this.mSystemProperties.get(SYSTEM_PROPERTY_REBOOT_REASON, null);
        switch (line.hashCode()) {
            case -2117951935:
                if (line.equals(REASON_THERMAL_SHUTDOWN)) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -1099647817:
                if (line.equals(REASON_LOW_BATTERY)) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case -934938715:
                if (line.equals(REASON_REBOOT)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -852189395:
                if (line.equals(REASON_USERREQUESTED)) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -169343402:
                if (line.equals(REASON_SHUTDOWN)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 1218064802:
                if (line.equals(REASON_BATTERY_THERMAL_STATE)) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            case 4:
                return 5;
            case 5:
                return 6;
            default:
                return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getLastSleepReasonInternal() {
        int i;
        synchronized (this.mLock) {
            i = this.mLastGlobalSleepReason;
        }
        return i;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public PowerManager.WakeData getLastWakeupInternal() {
        PowerManager.WakeData wakeData;
        synchronized (this.mLock) {
            long j = this.mLastGlobalWakeTime;
            wakeData = new PowerManager.WakeData(j, this.mLastGlobalWakeReason, j - this.mLastGlobalSleepTime);
        }
        return wakeData;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public PowerManager.SleepData getLastGoToSleepInternal() {
        PowerManager.SleepData sleepData;
        synchronized (this.mLock) {
            sleepData = new PowerManager.SleepData(this.mLastGlobalSleepTime, this.mLastGlobalSleepReason);
        }
        return sleepData;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean interceptPowerKeyDownInternal(KeyEvent event) {
        synchronized (this.mLock) {
            if (this.mProximityPositive && !this.mInterceptedPowerKeyForProximity) {
                this.mDisplayManagerInternal.ignoreProximitySensorUntilChanged();
                this.mInterceptedPowerKeyForProximity = true;
                return true;
            }
            return false;
        }
    }

    /* loaded from: classes2.dex */
    final class LocalService extends PowerManagerInternal {
        LocalService() {
        }

        public void setScreenBrightnessOverrideFromWindowManager(float screenBrightness) {
            screenBrightness = (screenBrightness < 0.0f || screenBrightness > 1.0f) ? Float.NaN : Float.NaN;
            PowerManagerService.this.setScreenBrightnessOverrideFromWindowManagerInternal(screenBrightness);
        }

        public void setDozeOverrideFromDreamManager(int screenState, int screenBrightness) {
            switch (screenState) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                    break;
                default:
                    screenState = 0;
                    break;
            }
            screenBrightness = (screenBrightness < -1 || screenBrightness > 255) ? -1 : -1;
            PowerManagerService.this.setDozeOverrideFromDreamManagerInternal(screenState, screenBrightness);
        }

        public void setUserInactiveOverrideFromWindowManager() {
            PowerManagerService.this.setUserInactiveOverrideFromWindowManagerInternal();
        }

        public void setUserActivityTimeoutOverrideFromWindowManager(long timeoutMillis) {
            PowerManagerService.this.setUserActivityTimeoutOverrideFromWindowManagerInternal(timeoutMillis);
        }

        public void setDrawWakeLockOverrideFromSidekick(boolean keepState) {
            PowerManagerService.this.setDrawWakeLockOverrideFromSidekickInternal(keepState);
        }

        public void setMaximumScreenOffTimeoutFromDeviceAdmin(int userId, long timeMs) {
            PowerManagerService.this.setMaximumScreenOffTimeoutFromDeviceAdminInternal(userId, timeMs);
        }

        public PowerSaveState getLowPowerState(int serviceType) {
            return PowerManagerService.this.mBatterySaverPolicy.getBatterySaverPolicy(serviceType);
        }

        public void registerLowPowerModeObserver(PowerManagerInternal.LowPowerModeListener listener) {
            PowerManagerService.this.mBatterySaverController.addListener(listener);
        }

        public boolean setDeviceIdleMode(boolean enabled) {
            return PowerManagerService.this.setDeviceIdleModeInternal(enabled);
        }

        public boolean setLightDeviceIdleMode(boolean enabled) {
            return PowerManagerService.this.setLightDeviceIdleModeInternal(enabled);
        }

        public void setDeviceIdleWhitelist(int[] appids) {
            PowerManagerService.this.setDeviceIdleWhitelistInternal(appids);
        }

        public void setDeviceIdleTempWhitelist(int[] appids) {
            PowerManagerService.this.setDeviceIdleTempWhitelistInternal(appids);
        }

        public void setLowPowerStandbyAllowlist(int[] appids) {
            PowerManagerService.this.setLowPowerStandbyAllowlistInternal(appids);
        }

        public void setLowPowerStandbyActive(boolean enabled) {
            PowerManagerService.this.setLowPowerStandbyActiveInternal(enabled);
        }

        public void startUidChanges() {
            PowerManagerService.this.startUidChangesInternal();
        }

        public void finishUidChanges() {
            PowerManagerService.this.finishUidChangesInternal();
        }

        public void updateUidProcState(int uid, int procState) {
            PowerManagerService.this.updateUidProcStateInternal(uid, procState);
        }

        public void uidGone(int uid) {
            PowerManagerService.this.uidGoneInternal(uid);
        }

        public void uidActive(int uid) {
            PowerManagerService.this.uidActiveInternal(uid);
        }

        public void uidIdle(int uid) {
            PowerManagerService.this.uidIdleInternal(uid);
        }

        public void setPowerBoost(int boost, int durationMs) {
            PowerManagerService.this.setPowerBoostInternal(boost, durationMs);
        }

        public void setPowerMode(int mode, boolean enabled) {
            PowerManagerService.this.setPowerModeInternal(mode, enabled);
        }

        public boolean wasDeviceIdleFor(long ms) {
            return PowerManagerService.this.wasDeviceIdleForInternal(ms);
        }

        public PowerManager.WakeData getLastWakeup() {
            return PowerManagerService.this.getLastWakeupInternal();
        }

        public PowerManager.SleepData getLastGoToSleep() {
            return PowerManagerService.this.getLastGoToSleepInternal();
        }

        public boolean interceptPowerKeyDown(KeyEvent event) {
            return PowerManagerService.this.interceptPowerKeyDownInternal(event);
        }
    }

    /* loaded from: classes2.dex */
    class DeviceStateListener implements DeviceStateManager.DeviceStateCallback {
        private int mDeviceState = -1;

        DeviceStateListener() {
        }

        public void onStateChanged(int deviceState) {
            if (this.mDeviceState != deviceState) {
                this.mDeviceState = deviceState;
                PowerManagerService powerManagerService = PowerManagerService.this;
                powerManagerService.userActivityInternal(0, powerManagerService.mClock.uptimeMillis(), 6, 0, 1000);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isSameCallback(IWakeLockCallback callback1, IWakeLockCallback callback2) {
        if (callback1 == callback2) {
            return true;
        }
        if (callback1 != null && callback2 != null && callback1.asBinder() == callback2.asBinder()) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleCheckBlackWakelock() {
        PackageManager pmMgr = this.mContext.getPackageManager();
        ActivityManager am = (ActivityManager) this.mContext.getSystemService(HostingRecord.HOSTING_TYPE_ACTIVITY);
        synchronized (this.mLock) {
            int i = this.mWakefulnessRaw;
            if (i != 0 && i != 2 && i != 3) {
                Slog.i(TAG, "Ignore to check black wakelock in screen on mode.");
                return;
            }
            if (sWakelockDebug) {
                Slog.d(TAG, "handleCheckBlackWakelock...");
            }
            for (Map.Entry<String, Integer> entry : ITranPowerManagerService.Instance().getBlackPackages().entrySet()) {
                try {
                    ApplicationInfo ai = pmMgr.getApplicationInfo(entry.getKey(), 0);
                    entry.setValue(Integer.valueOf(ai.uid));
                    removeBlackWakeLockLocked(entry.getKey(), ai.uid, am);
                } catch (Exception e) {
                    Slog.e(TAG, "Package [" + entry.getKey() + "] was not found. e=" + e);
                }
            }
            startCheckBlackWakelock();
        }
    }

    private void removeBlackWakeLockLocked(String packageName, int uid, ActivityManager am) {
        WakeLock wakeLock;
        boolean forgroundTopForceStop;
        boolean forgroundForceStop;
        Slog.i(TAG, "Remove black wakelock, packageName=" + packageName + ", uid=" + uid);
        WakeLock wakeLock2 = null;
        boolean isFounded = false;
        int i = this.mWakeLocks.size() - 1;
        while (true) {
            if (i < 0) {
                wakeLock = wakeLock2;
                break;
            }
            WakeLock wakeLock3 = this.mWakeLocks.get(i);
            WakeLock wakeLock4 = wakeLock3;
            if (wakeLock4.mWorkSource != null) {
                if (wakeLock4.mTag.equals(TRAN_AUDIOMIX_WAKELOCK)) {
                    int j = 0;
                    while (true) {
                        if (j >= wakeLock4.mWorkSource.size()) {
                            break;
                        } else if (wakeLock4.mWorkSource.getUid(j) != uid) {
                            j++;
                        } else {
                            isFounded = true;
                            break;
                        }
                    }
                } else if (wakeLock4.mTag.startsWith("*job*")) {
                    int j2 = 0;
                    while (true) {
                        if (j2 >= wakeLock4.mWorkSource.size()) {
                            break;
                        } else if (wakeLock4.mWorkSource.getUid(j2) != uid) {
                            j2++;
                        } else {
                            boolean isAudioPlaying = ITranGriffinFeature.Instance().getStateProvider().isAudioPlayingByPackageName(packageName);
                            if (!isAudioPlaying) {
                                isFounded = true;
                            } else {
                                Slog.w(TAG, "Ignore to remove black wakelock caused by [" + packageName + "] was is Audio Playing");
                            }
                        }
                    }
                }
            } else if (uid == wakeLock4.mOwnerUid) {
                isFounded = true;
            }
            if (isFounded) {
                wakeLock = wakeLock4;
                break;
            } else {
                wakeLock2 = null;
                i--;
            }
        }
        Slog.i(TAG, "Remove black wakelock, wakeLock=" + wakeLock);
        if (wakeLock != null) {
            long activeDura = SystemClock.uptimeMillis() - wakeLock.mActiveSince;
            if (activeDura >= BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS) {
                boolean isFocusPkg = this.mFocusPackageName.equals(packageName);
                boolean backgroundForceStop = ITranPowerManagerService.Instance().getForceStopPackages().contains(packageName);
                boolean forgroundForceStop2 = ITranPowerManagerService.Instance().isSupportForgroundForeStop(packageName);
                int forgroundTopForceStopInterval = ITranPowerManagerService.Instance().getForegroundTopForceStopInterval();
                long activeDuraAfterScreenOff = SystemClock.uptimeMillis() - this.mScreenOffTime;
                if (activeDuraAfterScreenOff < forgroundTopForceStopInterval * 60 * 1000) {
                    forgroundTopForceStop = false;
                } else {
                    boolean forgroundTopForceStop2 = ITranPowerManagerService.Instance().isSupportForceStopForegroundTopPackage(packageName);
                    forgroundTopForceStop = forgroundTopForceStop2;
                }
                boolean forgroundRemoveWl = ITranPowerManagerService.Instance().isSupportForceRemoveForgroundWl(packageName);
                if (!sWakelockDebug) {
                    forgroundForceStop = forgroundForceStop2;
                } else {
                    forgroundForceStop = forgroundForceStop2;
                    Slog.i(TAG, "Force remove focus=" + isFocusPkg + " backgroundForceStop=" + backgroundForceStop + " forgroundForceStop=" + forgroundForceStop + " forgroundTopForceStop=" + forgroundTopForceStop + " forgroundTopForceStopInterval=" + forgroundTopForceStopInterval);
                }
                if (isFocusPkg && !forgroundForceStop && !forgroundTopForceStop && !forgroundRemoveWl) {
                    Slog.i(TAG, "Ignore to remove black wakelock caused by [" + packageName + "] was on the top.");
                } else if ((!isFocusPkg && backgroundForceStop) || ((isFocusPkg && forgroundForceStop) || (isFocusPkg && forgroundTopForceStop))) {
                    stopPackage(am, packageName, true);
                    ITranPowerManagerService.Instance().hookLongWLForceStop(packageName);
                } else if (!isFocusPkg || (isFocusPkg && forgroundRemoveWl)) {
                    removeWakeLockByUid(wakeLock, uid, activeDura, i);
                }
            }
        }
    }

    private void removeWakeLockByUid(WakeLock wakeLock, int uid, long activeDura, int index) {
        if (wakeLock.mWorkSource != null && wakeLock.mWorkSource.size() > 1) {
            Slog.i(TAG, "Force remove the work source of black wakelock. package=" + wakeLock.mPackageName + ", tag=" + wakeLock.mTag + ", workSource=" + wakeLock.mWorkSource + ", uid=" + uid + ", pid=" + wakeLock.mOwnerPid + ", activeDuration=" + activeDura);
            removeBlackWakeSource(wakeLock.mWorkSource, uid);
            return;
        }
        Slog.i(TAG, "Force remove the black wakelock. package=" + wakeLock.mPackageName + ", tag=" + wakeLock.mTag + ", uid=" + uid + ", pid=" + wakeLock.mOwnerPid + ", activeDuration=" + activeDura);
        try {
            wakeLock.mLock.unlinkToDeath(wakeLock, 0);
            removeWakeLockLocked(wakeLock, index);
        } catch (NoSuchElementException e) {
            Slog.e(TAG, "Black wakeLock already released, Death link does not exist. package=" + wakeLock.mPackageName + ", tag=" + wakeLock.mTag);
        }
    }

    private void stopPackage(ActivityManager am, String packageName, boolean forceStop) {
        if (forceStop) {
            Slog.i(TAG, "forceStop packageName=" + packageName);
            Method[] methods = ActivityManager.class.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                if ("forceStopPackage".equals(methods[i].getName())) {
                    try {
                        methods[i].invoke(am, packageName);
                        return;
                    } catch (Exception e) {
                        Slog.e(TAG, "stopPackage fail, packageName: " + packageName + ", exception: " + e.toString());
                        return;
                    }
                }
            }
            return;
        }
        Slog.i(TAG, "killProcess packageName=" + packageName);
        am.killBackgroundProcesses(packageName);
    }

    private void removeBlackWakeSource(WorkSource workSource, int uid) {
        WorkSource blackWorkSource;
        for (int i = 0; i < workSource.size(); i++) {
            int uidValue = workSource.getUid(i);
            if (uidValue == uid) {
                String packageName = workSource.getPackageName(i);
                if (packageName != null) {
                    blackWorkSource = new WorkSource(uidValue, packageName);
                } else {
                    blackWorkSource = new WorkSource(uidValue);
                }
                Slog.i(TAG, "removeBlackWakeSource: workSource=" + blackWorkSource);
                workSource.remove(blackWorkSource);
                return;
            }
        }
    }

    private void startCheckBlackWakelock() {
        if (!ITranPowerManagerService.Instance().isEnabledAbnormalHandler()) {
            Slog.i(TAG, "Handling black wakelock has been disabled.");
            return;
        }
        boolean has = this.mHandlerWatchWakelock.hasMessages(2);
        if (!has) {
            if (sWakelockDebug) {
                Slog.d(TAG, "startCheckBlackWakelock ...");
            }
            Message msg = this.mHandlerWatchWakelock.obtainMessage(2);
            this.mHandlerWatchWakelock.sendMessageDelayed(msg, 315000L);
        }
    }

    void isNotifyScreenOnInternal(boolean notify, long dimDuration) {
        synchronized (this.mLock) {
            int i = this.mWakefulnessRaw;
            if (i != 1 && i != 2) {
                this.mIsNotifyScreenOn = notify;
                this.mNotifyDimDuration = dimDuration;
                wakePowerGroupLocked(this.mPowerGroups.get(0), SystemClock.uptimeMillis(), 0, "android.server.power:NotificationScreenOn", 1000, this.mContext.getOpPackageName(), 1000);
                updatePowerStateLocked();
            }
        }
    }

    private void updateSourceConnectDirty() {
        this.mDirty |= 4;
    }

    private long getTimeMillis() {
        return this.mClock.uptimeMillis();
    }
}
