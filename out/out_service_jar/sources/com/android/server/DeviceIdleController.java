package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.net.ConnectivityManager;
import android.net.INetworkPolicyManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IDeviceIdleController;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.PowerSaveState;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.MutableLong;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.TimeUtils;
import com.android.internal.app.IBatteryStats;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.jobs.ArrayUtils;
import com.android.internal.util.jobs.DumpUtils;
import com.android.internal.util.jobs.FastXmlSerializer;
import com.android.internal.util.jobs.XmlUtils;
import com.android.server.AnyMotionDetector;
import com.android.server.DeviceIdleInternal;
import com.android.server.PowerAllowlistInternal;
import com.android.server.UiModeManagerService;
import com.android.server.am.BatteryStatsService;
import com.android.server.am.HostingRecord;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.deviceidle.ConstraintController;
import com.android.server.deviceidle.DeviceIdleConstraintTracker;
import com.android.server.deviceidle.IDeviceIdleConstraint;
import com.android.server.deviceidle.TvConstraintController;
import com.android.server.job.controllers.JobStatus;
import com.android.server.net.NetworkPolicyManagerInternal;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
/* loaded from: classes.dex */
public class DeviceIdleController extends SystemService implements AnyMotionDetector.DeviceIdleCallback {
    private static final int ACTIVE_REASON_ALARM = 7;
    private static final int ACTIVE_REASON_CHARGING = 3;
    private static final int ACTIVE_REASON_FORCED = 6;
    private static final int ACTIVE_REASON_FROM_BINDER_CALL = 5;
    private static final int ACTIVE_REASON_MOTION = 1;
    private static final int ACTIVE_REASON_SCREEN = 2;
    private static final int ACTIVE_REASON_UNKNOWN = 0;
    private static final int ACTIVE_REASON_UNLOCKED = 4;
    private static final boolean COMPRESS_TIME = false;
    private static final boolean DEBUG = false;
    private static final int EVENT_BUFFER_SIZE = 100;
    private static final int EVENT_DEEP_IDLE = 4;
    private static final int EVENT_DEEP_MAINTENANCE = 5;
    private static final int EVENT_LIGHT_IDLE = 2;
    private static final int EVENT_LIGHT_MAINTENANCE = 3;
    private static final int EVENT_NORMAL = 1;
    private static final int EVENT_NULL = 0;
    static final int LIGHT_STATE_ACTIVE = 0;
    static final int LIGHT_STATE_IDLE = 4;
    static final int LIGHT_STATE_IDLE_MAINTENANCE = 6;
    static final int LIGHT_STATE_INACTIVE = 1;
    static final int LIGHT_STATE_OVERRIDE = 7;
    static final int LIGHT_STATE_WAITING_FOR_NETWORK = 5;
    static final float MIN_PRE_IDLE_FACTOR_CHANGE = 0.05f;
    static final long MIN_STATE_STEP_ALARM_CHANGE = 60000;
    private static final int MSG_FINISH_IDLE_OP = 8;
    private static final int MSG_REPORT_ACTIVE = 5;
    private static final int MSG_REPORT_IDLE_OFF = 4;
    private static final int MSG_REPORT_IDLE_ON = 2;
    private static final int MSG_REPORT_IDLE_ON_LIGHT = 3;
    static final int MSG_REPORT_STATIONARY_STATUS = 7;
    private static final int MSG_REPORT_TEMP_APP_WHITELIST_ADDED_TO_NPMS = 14;
    private static final int MSG_REPORT_TEMP_APP_WHITELIST_CHANGED = 13;
    private static final int MSG_REPORT_TEMP_APP_WHITELIST_REMOVED_TO_NPMS = 15;
    static final int MSG_RESET_PRE_IDLE_TIMEOUT_FACTOR = 12;
    private static final int MSG_SEND_CONSTRAINT_MONITORING = 10;
    private static final int MSG_TEMP_APP_WHITELIST_TIMEOUT = 6;
    static final int MSG_UPDATE_PRE_IDLE_TIMEOUT_FACTOR = 11;
    private static final int MSG_WRITE_CONFIG = 1;
    static final int SET_IDLE_FACTOR_RESULT_IGNORED = 0;
    static final int SET_IDLE_FACTOR_RESULT_INVALID = 3;
    static final int SET_IDLE_FACTOR_RESULT_NOT_SUPPORT = 2;
    static final int SET_IDLE_FACTOR_RESULT_OK = 1;
    static final int SET_IDLE_FACTOR_RESULT_UNINIT = -1;
    static final int STATE_ACTIVE = 0;
    static final int STATE_IDLE = 5;
    static final int STATE_IDLE_MAINTENANCE = 6;
    static final int STATE_IDLE_PENDING = 2;
    static final int STATE_INACTIVE = 1;
    static final int STATE_LOCATING = 4;
    static final int STATE_QUICK_DOZE_DELAY = 7;
    static final int STATE_SENSING = 3;
    private static final String TAG = "DeviceIdleController";
    private int mActiveIdleOpCount;
    private PowerManager.WakeLock mActiveIdleWakeLock;
    private int mActiveReason;
    private AlarmManager mAlarmManager;
    private boolean mAlarmsActive;
    private ArraySet<String> mAllowInIdleUserAppArray;
    private AnyMotionDetector mAnyMotionDetector;
    private final AppStateTrackerImpl mAppStateTracker;
    private IBatteryStats mBatteryStats;
    BinderService mBinderService;
    private boolean mCharging;
    public final AtomicFile mConfigFile;
    private Constants mConstants;
    private ConstraintController mConstraintController;
    private final ArrayMap<IDeviceIdleConstraint, DeviceIdleConstraintTracker> mConstraints;
    private long mCurLightIdleBudget;
    final AlarmManager.OnAlarmListener mDeepAlarmListener;
    private boolean mDeepEnabled;
    private final int[] mEventCmds;
    private final String[] mEventReasons;
    private final long[] mEventTimes;
    private boolean mForceIdle;
    private final LocationListener mGenericLocationListener;
    private PowerManager.WakeLock mGoingIdleWakeLock;
    private final LocationListener mGpsLocationListener;
    final MyHandler mHandler;
    private boolean mHasGps;
    private boolean mHasNetworkLocation;
    private Intent mIdleIntent;
    private long mIdleStartTime;
    private final BroadcastReceiver mIdleStartedDoneReceiver;
    private long mInactiveTimeout;
    private final Injector mInjector;
    private final BroadcastReceiver mInteractivityReceiver;
    private boolean mJobsActive;
    private Location mLastGenericLocation;
    private Location mLastGpsLocation;
    private long mLastMotionEventElapsed;
    private float mLastPreIdleFactor;
    private final AlarmManager.OnAlarmListener mLightAlarmListener;
    private boolean mLightEnabled;
    private Intent mLightIdleIntent;
    private final AlarmManager.OnAlarmListener mLightMaintenanceAlarmListener;
    private int mLightState;
    private ActivityManagerInternal mLocalActivityManager;
    private ActivityTaskManagerInternal mLocalActivityTaskManager;
    private AlarmManagerInternal mLocalAlarmManager;
    private PowerManagerInternal mLocalPowerManager;
    private DeviceIdleInternal mLocalService;
    private boolean mLocated;
    private boolean mLocating;
    private LocationRequest mLocationRequest;
    private long mMaintenanceStartTime;
    final MotionListener mMotionListener;
    private final AlarmManager.OnAlarmListener mMotionRegistrationAlarmListener;
    private Sensor mMotionSensor;
    private final AlarmManager.OnAlarmListener mMotionTimeoutAlarmListener;
    private boolean mNetworkConnected;
    private INetworkPolicyManager mNetworkPolicyManager;
    private NetworkPolicyManagerInternal mNetworkPolicyManagerInternal;
    private long mNextAlarmTime;
    private long mNextIdleDelay;
    private long mNextIdlePendingDelay;
    private long mNextLightAlarmTime;
    private long mNextLightIdleDelay;
    private long mNextLightMaintenanceAlarmTime;
    private long mNextSensingTimeoutAlarmTime;
    private boolean mNotMoving;
    private int mNumBlockingConstraints;
    private PackageManagerInternal mPackageManagerInternal;
    private PowerManager mPowerManager;
    private int[] mPowerSaveWhitelistAllAppIdArray;
    private final SparseBooleanArray mPowerSaveWhitelistAllAppIds;
    private final ArrayMap<String, Integer> mPowerSaveWhitelistApps;
    private final ArrayMap<String, Integer> mPowerSaveWhitelistAppsExceptIdle;
    private int[] mPowerSaveWhitelistExceptIdleAppIdArray;
    private final SparseBooleanArray mPowerSaveWhitelistExceptIdleAppIds;
    private final SparseBooleanArray mPowerSaveWhitelistSystemAppIds;
    private final SparseBooleanArray mPowerSaveWhitelistSystemAppIdsExceptIdle;
    private int[] mPowerSaveWhitelistUserAppIdArray;
    private final SparseBooleanArray mPowerSaveWhitelistUserAppIds;
    private final ArrayMap<String, Integer> mPowerSaveWhitelistUserApps;
    private final ArraySet<String> mPowerSaveWhitelistUserAppsExceptIdle;
    private float mPreIdleFactor;
    private boolean mQuickDozeActivated;
    private boolean mQuickDozeActivatedWhileIdling;
    private final BroadcastReceiver mReceiver;
    private ArrayMap<String, Integer> mRemovedFromSystemWhitelistApps;
    private boolean mScreenLocked;
    private ActivityTaskManagerInternal.ScreenObserver mScreenObserver;
    private boolean mScreenOn;
    private final AlarmManager.OnAlarmListener mSensingTimeoutAlarmListener;
    private SensorManager mSensorManager;
    private int mState;
    private final ArraySet<DeviceIdleInternal.StationaryListener> mStationaryListeners;
    private final ArraySet<PowerAllowlistInternal.TempAllowlistChangeListener> mTempAllowlistChangeListeners;
    private int[] mTempWhitelistAppIdArray;
    private final SparseArray<Pair<MutableLong, String>> mTempWhitelistAppIdEndTimes;
    private final boolean mUseMotionSensor;

    static String stateToString(int state) {
        switch (state) {
            case 0:
                return "ACTIVE";
            case 1:
                return "INACTIVE";
            case 2:
                return "IDLE_PENDING";
            case 3:
                return "SENSING";
            case 4:
                return "LOCATING";
            case 5:
                return "IDLE";
            case 6:
                return "IDLE_MAINTENANCE";
            case 7:
                return "QUICK_DOZE_DELAY";
            default:
                return Integer.toString(state);
        }
    }

    static String lightStateToString(int state) {
        switch (state) {
            case 0:
                return "ACTIVE";
            case 1:
                return "INACTIVE";
            case 2:
            case 3:
            default:
                return Integer.toString(state);
            case 4:
                return "IDLE";
            case 5:
                return "WAITING_FOR_NETWORK";
            case 6:
                return "IDLE_MAINTENANCE";
            case 7:
                return "OVERRIDE";
        }
    }

    private void addEvent(int cmd, String reason) {
        int[] iArr = this.mEventCmds;
        if (iArr[0] != cmd) {
            System.arraycopy(iArr, 0, iArr, 1, 99);
            long[] jArr = this.mEventTimes;
            System.arraycopy(jArr, 0, jArr, 1, 99);
            String[] strArr = this.mEventReasons;
            System.arraycopy(strArr, 0, strArr, 1, 99);
            this.mEventCmds[0] = cmd;
            this.mEventTimes[0] = SystemClock.elapsedRealtime();
            this.mEventReasons[0] = reason;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-DeviceIdleController  reason: not valid java name */
    public /* synthetic */ void m155lambda$new$0$comandroidserverDeviceIdleController() {
        synchronized (this) {
            stepLightIdleStateLocked("s:alarm");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$1$com-android-server-DeviceIdleController  reason: not valid java name */
    public /* synthetic */ void m156lambda$new$1$comandroidserverDeviceIdleController() {
        synchronized (this) {
            stepLightIdleStateLocked("s:alarm");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$2$com-android-server-DeviceIdleController  reason: not valid java name */
    public /* synthetic */ void m157lambda$new$2$comandroidserverDeviceIdleController() {
        synchronized (this) {
            if (this.mStationaryListeners.size() > 0) {
                startMonitoringMotionLocked();
                scheduleMotionTimeoutAlarmLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$3$com-android-server-DeviceIdleController  reason: not valid java name */
    public /* synthetic */ void m158lambda$new$3$comandroidserverDeviceIdleController() {
        synchronized (this) {
            if (!isStationaryLocked()) {
                Slog.w(TAG, "motion timeout went off and device isn't stationary");
            } else {
                postStationaryStatusUpdated();
            }
        }
    }

    private void postStationaryStatus(DeviceIdleInternal.StationaryListener listener) {
        this.mHandler.obtainMessage(7, listener).sendToTarget();
    }

    private void postStationaryStatusUpdated() {
        this.mHandler.sendEmptyMessage(7);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isStationaryLocked() {
        long now = this.mInjector.getElapsedRealtime();
        return this.mMotionListener.active && now - Math.max(this.mMotionListener.activatedTimeElapsed, this.mLastMotionEventElapsed) >= this.mConstants.MOTION_INACTIVE_TIMEOUT;
    }

    void registerStationaryListener(DeviceIdleInternal.StationaryListener listener) {
        synchronized (this) {
            if (this.mStationaryListeners.add(listener)) {
                postStationaryStatus(listener);
                if (this.mMotionListener.active) {
                    if (!isStationaryLocked() && this.mStationaryListeners.size() == 1) {
                        scheduleMotionTimeoutAlarmLocked();
                    }
                } else {
                    startMonitoringMotionLocked();
                    scheduleMotionTimeoutAlarmLocked();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterStationaryListener(DeviceIdleInternal.StationaryListener listener) {
        int i;
        synchronized (this) {
            if (this.mStationaryListeners.remove(listener) && this.mStationaryListeners.size() == 0 && ((i = this.mState) == 0 || i == 1 || this.mQuickDozeActivated)) {
                maybeStopMonitoringMotionLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerTempAllowlistChangeListener(PowerAllowlistInternal.TempAllowlistChangeListener listener) {
        synchronized (this) {
            this.mTempAllowlistChangeListeners.add(listener);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterTempAllowlistChangeListener(PowerAllowlistInternal.TempAllowlistChangeListener listener) {
        synchronized (this) {
            this.mTempAllowlistChangeListeners.remove(listener);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class MotionListener extends TriggerEventListener implements SensorEventListener {
        long activatedTimeElapsed;
        boolean active = false;

        MotionListener() {
        }

        public boolean isActive() {
            return this.active;
        }

        @Override // android.hardware.TriggerEventListener
        public void onTrigger(TriggerEvent event) {
            synchronized (DeviceIdleController.this) {
                this.active = false;
                DeviceIdleController.this.motionLocked();
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            synchronized (DeviceIdleController.this) {
                DeviceIdleController.this.mSensorManager.unregisterListener(this, DeviceIdleController.this.mMotionSensor);
                this.active = false;
                DeviceIdleController.this.motionLocked();
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public boolean registerLocked() {
            boolean success;
            if (DeviceIdleController.this.mMotionSensor.getReportingMode() == 2) {
                success = DeviceIdleController.this.mSensorManager.requestTriggerSensor(DeviceIdleController.this.mMotionListener, DeviceIdleController.this.mMotionSensor);
            } else {
                success = DeviceIdleController.this.mSensorManager.registerListener(DeviceIdleController.this.mMotionListener, DeviceIdleController.this.mMotionSensor, 3);
            }
            if (success) {
                this.active = true;
                this.activatedTimeElapsed = DeviceIdleController.this.mInjector.getElapsedRealtime();
            } else {
                Slog.e(DeviceIdleController.TAG, "Unable to register for " + DeviceIdleController.this.mMotionSensor);
            }
            return success;
        }

        public void unregisterLocked() {
            if (DeviceIdleController.this.mMotionSensor.getReportingMode() == 2) {
                DeviceIdleController.this.mSensorManager.cancelTriggerSensor(DeviceIdleController.this.mMotionListener, DeviceIdleController.this.mMotionSensor);
            } else {
                DeviceIdleController.this.mSensorManager.unregisterListener(DeviceIdleController.this.mMotionListener);
            }
            this.active = false;
        }
    }

    /* loaded from: classes.dex */
    public final class Constants implements DeviceConfig.OnPropertiesChangedListener {
        private static final long DEFAULT_FLEX_TIME_SHORT = 60000;
        private static final long DEFAULT_IDLE_AFTER_INACTIVE_TIMEOUT = 1800000;
        private static final long DEFAULT_IDLE_AFTER_INACTIVE_TIMEOUT_SMALL_BATTERY = 900000;
        private static final float DEFAULT_IDLE_FACTOR = 2.0f;
        private static final float DEFAULT_IDLE_PENDING_FACTOR = 2.0f;
        private static final long DEFAULT_IDLE_PENDING_TIMEOUT = 300000;
        private static final long DEFAULT_IDLE_TIMEOUT = 3600000;
        private static final long DEFAULT_INACTIVE_TIMEOUT = 1800000;
        private static final long DEFAULT_INACTIVE_TIMEOUT_SMALL_BATTERY = 900000;
        private static final long DEFAULT_LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT = 240000;
        private static final float DEFAULT_LIGHT_IDLE_FACTOR = 2.0f;
        private static final long DEFAULT_LIGHT_IDLE_MAINTENANCE_MAX_BUDGET = 300000;
        private static final long DEFAULT_LIGHT_IDLE_MAINTENANCE_MIN_BUDGET = 60000;
        private static final long DEFAULT_LIGHT_IDLE_TIMEOUT = 300000;
        private static final long DEFAULT_LIGHT_MAX_IDLE_TIMEOUT = 900000;
        private static final long DEFAULT_LOCATING_TIMEOUT = 30000;
        private static final float DEFAULT_LOCATION_ACCURACY = 20.0f;
        private static final long DEFAULT_MAX_IDLE_PENDING_TIMEOUT = 600000;
        private static final long DEFAULT_MAX_IDLE_TIMEOUT = 21600000;
        private static final long DEFAULT_MAX_TEMP_APP_ALLOWLIST_DURATION_MS = 300000;
        private static final long DEFAULT_MIN_DEEP_MAINTENANCE_TIME = 30000;
        private static final long DEFAULT_MIN_LIGHT_MAINTENANCE_TIME = 5000;
        private static final long DEFAULT_MIN_TIME_TO_ALARM = 1800000;
        private static final long DEFAULT_MMS_TEMP_APP_ALLOWLIST_DURATION_MS = 60000;
        private static final long DEFAULT_MOTION_INACTIVE_TIMEOUT = 600000;
        private static final long DEFAULT_MOTION_INACTIVE_TIMEOUT_FLEX = 60000;
        private static final long DEFAULT_NOTIFICATION_ALLOWLIST_DURATION_MS = 30000;
        private static final float DEFAULT_PRE_IDLE_FACTOR_LONG = 1.67f;
        private static final float DEFAULT_PRE_IDLE_FACTOR_SHORT = 0.33f;
        private static final long DEFAULT_QUICK_DOZE_DELAY_TIMEOUT = 60000;
        private static final long DEFAULT_SENSING_TIMEOUT = 240000;
        private static final long DEFAULT_SMS_TEMP_APP_ALLOWLIST_DURATION_MS = 20000;
        private static final boolean DEFAULT_USE_WINDOW_ALARMS = true;
        private static final boolean DEFAULT_WAIT_FOR_UNLOCK = true;
        private static final String KEY_FLEX_TIME_SHORT = "flex_time_short";
        private static final String KEY_IDLE_AFTER_INACTIVE_TIMEOUT = "idle_after_inactive_to";
        private static final String KEY_IDLE_FACTOR = "idle_factor";
        private static final String KEY_IDLE_PENDING_FACTOR = "idle_pending_factor";
        private static final String KEY_IDLE_PENDING_TIMEOUT = "idle_pending_to";
        private static final String KEY_IDLE_TIMEOUT = "idle_to";
        private static final String KEY_INACTIVE_TIMEOUT = "inactive_to";
        private static final String KEY_LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT = "light_after_inactive_to";
        private static final String KEY_LIGHT_IDLE_FACTOR = "light_idle_factor";
        private static final String KEY_LIGHT_IDLE_MAINTENANCE_MAX_BUDGET = "light_idle_maintenance_max_budget";
        private static final String KEY_LIGHT_IDLE_MAINTENANCE_MIN_BUDGET = "light_idle_maintenance_min_budget";
        private static final String KEY_LIGHT_IDLE_TIMEOUT = "light_idle_to";
        private static final String KEY_LIGHT_MAX_IDLE_TIMEOUT = "light_max_idle_to";
        private static final String KEY_LOCATING_TIMEOUT = "locating_to";
        private static final String KEY_LOCATION_ACCURACY = "location_accuracy";
        private static final String KEY_MAX_IDLE_PENDING_TIMEOUT = "max_idle_pending_to";
        private static final String KEY_MAX_IDLE_TIMEOUT = "max_idle_to";
        private static final String KEY_MAX_TEMP_APP_ALLOWLIST_DURATION_MS = "max_temp_app_allowlist_duration_ms";
        private static final String KEY_MIN_DEEP_MAINTENANCE_TIME = "min_deep_maintenance_time";
        private static final String KEY_MIN_LIGHT_MAINTENANCE_TIME = "min_light_maintenance_time";
        private static final String KEY_MIN_TIME_TO_ALARM = "min_time_to_alarm";
        private static final String KEY_MMS_TEMP_APP_ALLOWLIST_DURATION_MS = "mms_temp_app_allowlist_duration_ms";
        private static final String KEY_MOTION_INACTIVE_TIMEOUT = "motion_inactive_to";
        private static final String KEY_MOTION_INACTIVE_TIMEOUT_FLEX = "motion_inactive_to_flex";
        private static final String KEY_NOTIFICATION_ALLOWLIST_DURATION_MS = "notification_allowlist_duration_ms";
        private static final String KEY_PRE_IDLE_FACTOR_LONG = "pre_idle_factor_long";
        private static final String KEY_PRE_IDLE_FACTOR_SHORT = "pre_idle_factor_short";
        private static final String KEY_QUICK_DOZE_DELAY_TIMEOUT = "quick_doze_delay_to";
        private static final String KEY_SENSING_TIMEOUT = "sensing_to";
        private static final String KEY_SMS_TEMP_APP_ALLOWLIST_DURATION_MS = "sms_temp_app_allowlist_duration_ms";
        private static final String KEY_USE_WINDOW_ALARMS = "use_window_alarms";
        private static final String KEY_WAIT_FOR_UNLOCK = "wait_for_unlock";
        public long IDLE_AFTER_INACTIVE_TIMEOUT;
        public long INACTIVE_TIMEOUT;
        private final boolean mSmallBatteryDevice;
        public long FLEX_TIME_SHORT = 60000;
        public long LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT = 240000;
        public long LIGHT_IDLE_TIMEOUT = BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS;
        public float LIGHT_IDLE_FACTOR = 2.0f;
        public long LIGHT_MAX_IDLE_TIMEOUT = 900000;
        public long LIGHT_IDLE_MAINTENANCE_MIN_BUDGET = 60000;
        public long LIGHT_IDLE_MAINTENANCE_MAX_BUDGET = BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS;
        public long MIN_LIGHT_MAINTENANCE_TIME = DEFAULT_MIN_LIGHT_MAINTENANCE_TIME;
        public long MIN_DEEP_MAINTENANCE_TIME = 30000;
        public long SENSING_TIMEOUT = 240000;
        public long LOCATING_TIMEOUT = 30000;
        public float LOCATION_ACCURACY = DEFAULT_LOCATION_ACCURACY;
        public long MOTION_INACTIVE_TIMEOUT = 600000;
        public long MOTION_INACTIVE_TIMEOUT_FLEX = 60000;
        public long IDLE_PENDING_TIMEOUT = BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS;
        public long MAX_IDLE_PENDING_TIMEOUT = 600000;
        public float IDLE_PENDING_FACTOR = 2.0f;
        public long QUICK_DOZE_DELAY_TIMEOUT = 60000;
        public long IDLE_TIMEOUT = 3600000;
        public long MAX_IDLE_TIMEOUT = DEFAULT_MAX_IDLE_TIMEOUT;
        public float IDLE_FACTOR = 2.0f;
        public long MIN_TIME_TO_ALARM = 1800000;
        public long MAX_TEMP_APP_ALLOWLIST_DURATION_MS = BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS;
        public long MMS_TEMP_APP_ALLOWLIST_DURATION_MS = 60000;
        public long SMS_TEMP_APP_ALLOWLIST_DURATION_MS = DEFAULT_SMS_TEMP_APP_ALLOWLIST_DURATION_MS;
        public long NOTIFICATION_ALLOWLIST_DURATION_MS = 30000;
        public float PRE_IDLE_FACTOR_LONG = DEFAULT_PRE_IDLE_FACTOR_LONG;
        public float PRE_IDLE_FACTOR_SHORT = DEFAULT_PRE_IDLE_FACTOR_SHORT;
        public boolean WAIT_FOR_UNLOCK = true;
        public boolean USE_WINDOW_ALARMS = true;

        public Constants() {
            this.INACTIVE_TIMEOUT = 1800000L;
            this.IDLE_AFTER_INACTIVE_TIMEOUT = 1800000L;
            boolean isSmallBatteryDevice = ActivityManager.isSmallBatteryDevice();
            this.mSmallBatteryDevice = isSmallBatteryDevice;
            if (isSmallBatteryDevice) {
                this.INACTIVE_TIMEOUT = 900000L;
                this.IDLE_AFTER_INACTIVE_TIMEOUT = 900000L;
            }
            DeviceConfig.addOnPropertiesChangedListener("device_idle", JobSchedulerBackgroundThread.getExecutor(), this);
            onPropertiesChanged(DeviceConfig.getProperties("device_idle", new String[0]));
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        public void onPropertiesChanged(DeviceConfig.Properties properties) {
            synchronized (DeviceIdleController.this) {
                for (String name : properties.getKeyset()) {
                    if (name != null) {
                        char c = 65535;
                        switch (name.hashCode()) {
                            case -1781086459:
                                if (name.equals(KEY_NOTIFICATION_ALLOWLIST_DURATION_MS)) {
                                    c = 27;
                                    break;
                                }
                                break;
                            case -1102128050:
                                if (name.equals(KEY_LIGHT_IDLE_MAINTENANCE_MAX_BUDGET)) {
                                    c = 6;
                                    break;
                                }
                                break;
                            case -1067343247:
                                if (name.equals(KEY_LIGHT_IDLE_FACTOR)) {
                                    c = 3;
                                    break;
                                }
                                break;
                            case -986742087:
                                if (name.equals(KEY_USE_WINDOW_ALARMS)) {
                                    c = 31;
                                    break;
                                }
                                break;
                            case -919175870:
                                if (name.equals(KEY_LIGHT_MAX_IDLE_TIMEOUT)) {
                                    c = 4;
                                    break;
                                }
                                break;
                            case -564968069:
                                if (name.equals(KEY_PRE_IDLE_FACTOR_SHORT)) {
                                    c = 30;
                                    break;
                                }
                                break;
                            case -547781361:
                                if (name.equals(KEY_MIN_LIGHT_MAINTENANCE_TIME)) {
                                    c = 7;
                                    break;
                                }
                                break;
                            case -492261706:
                                if (name.equals(KEY_SMS_TEMP_APP_ALLOWLIST_DURATION_MS)) {
                                    c = 26;
                                    break;
                                }
                                break;
                            case -318123838:
                                if (name.equals(KEY_IDLE_PENDING_FACTOR)) {
                                    c = 18;
                                    break;
                                }
                                break;
                            case -173192557:
                                if (name.equals(KEY_MAX_IDLE_PENDING_TIMEOUT)) {
                                    c = 17;
                                    break;
                                }
                                break;
                            case -80111214:
                                if (name.equals(KEY_MIN_TIME_TO_ALARM)) {
                                    c = 23;
                                    break;
                                }
                                break;
                            case 197367965:
                                if (name.equals(KEY_LIGHT_IDLE_TIMEOUT)) {
                                    c = 2;
                                    break;
                                }
                                break;
                            case 361511631:
                                if (name.equals(KEY_INACTIVE_TIMEOUT)) {
                                    c = '\t';
                                    break;
                                }
                                break;
                            case 370224338:
                                if (name.equals(KEY_MOTION_INACTIVE_TIMEOUT_FLEX)) {
                                    c = 14;
                                    break;
                                }
                                break;
                            case 415987654:
                                if (name.equals(KEY_MOTION_INACTIVE_TIMEOUT)) {
                                    c = '\r';
                                    break;
                                }
                                break;
                            case 551187755:
                                if (name.equals(KEY_LOCATING_TIMEOUT)) {
                                    c = 11;
                                    break;
                                }
                                break;
                            case 866187779:
                                if (name.equals(KEY_LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT)) {
                                    c = 1;
                                    break;
                                }
                                break;
                            case 891348287:
                                if (name.equals(KEY_MIN_DEEP_MAINTENANCE_TIME)) {
                                    c = '\b';
                                    break;
                                }
                                break;
                            case 918455627:
                                if (name.equals(KEY_MAX_TEMP_APP_ALLOWLIST_DURATION_MS)) {
                                    c = 24;
                                    break;
                                }
                                break;
                            case 1001374852:
                                if (name.equals(KEY_WAIT_FOR_UNLOCK)) {
                                    c = 28;
                                    break;
                                }
                                break;
                            case 1228499357:
                                if (name.equals(KEY_PRE_IDLE_FACTOR_LONG)) {
                                    c = 29;
                                    break;
                                }
                                break;
                            case 1350761616:
                                if (name.equals(KEY_FLEX_TIME_SHORT)) {
                                    c = 0;
                                    break;
                                }
                                break;
                            case 1369871264:
                                if (name.equals(KEY_LIGHT_IDLE_MAINTENANCE_MIN_BUDGET)) {
                                    c = 5;
                                    break;
                                }
                                break;
                            case 1383403841:
                                if (name.equals(KEY_IDLE_AFTER_INACTIVE_TIMEOUT)) {
                                    c = 15;
                                    break;
                                }
                                break;
                            case 1536604751:
                                if (name.equals(KEY_SENSING_TIMEOUT)) {
                                    c = '\n';
                                    break;
                                }
                                break;
                            case 1547108378:
                                if (name.equals(KEY_IDLE_FACTOR)) {
                                    c = 22;
                                    break;
                                }
                                break;
                            case 1563458830:
                                if (name.equals(KEY_QUICK_DOZE_DELAY_TIMEOUT)) {
                                    c = 19;
                                    break;
                                }
                                break;
                            case 1664365254:
                                if (name.equals(KEY_IDLE_TIMEOUT)) {
                                    c = 20;
                                    break;
                                }
                                break;
                            case 1679398766:
                                if (name.equals(KEY_IDLE_PENDING_TIMEOUT)) {
                                    c = 16;
                                    break;
                                }
                                break;
                            case 1695275755:
                                if (name.equals(KEY_MAX_IDLE_TIMEOUT)) {
                                    c = 21;
                                    break;
                                }
                                break;
                            case 1930831427:
                                if (name.equals(KEY_LOCATION_ACCURACY)) {
                                    c = '\f';
                                    break;
                                }
                                break;
                            case 1944720892:
                                if (name.equals(KEY_MMS_TEMP_APP_ALLOWLIST_DURATION_MS)) {
                                    c = 25;
                                    break;
                                }
                                break;
                        }
                        switch (c) {
                            case 0:
                                this.FLEX_TIME_SHORT = properties.getLong(KEY_FLEX_TIME_SHORT, 60000L);
                                break;
                            case 1:
                                this.LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT = properties.getLong(KEY_LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT, 240000L);
                                break;
                            case 2:
                                this.LIGHT_IDLE_TIMEOUT = properties.getLong(KEY_LIGHT_IDLE_TIMEOUT, (long) BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS);
                                break;
                            case 3:
                                this.LIGHT_IDLE_FACTOR = Math.max(1.0f, properties.getFloat(KEY_LIGHT_IDLE_FACTOR, 2.0f));
                                break;
                            case 4:
                                this.LIGHT_MAX_IDLE_TIMEOUT = properties.getLong(KEY_LIGHT_MAX_IDLE_TIMEOUT, 900000L);
                                break;
                            case 5:
                                this.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET = properties.getLong(KEY_LIGHT_IDLE_MAINTENANCE_MIN_BUDGET, 60000L);
                                break;
                            case 6:
                                this.LIGHT_IDLE_MAINTENANCE_MAX_BUDGET = properties.getLong(KEY_LIGHT_IDLE_MAINTENANCE_MAX_BUDGET, (long) BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS);
                                break;
                            case 7:
                                this.MIN_LIGHT_MAINTENANCE_TIME = properties.getLong(KEY_MIN_LIGHT_MAINTENANCE_TIME, (long) DEFAULT_MIN_LIGHT_MAINTENANCE_TIME);
                                break;
                            case '\b':
                                this.MIN_DEEP_MAINTENANCE_TIME = properties.getLong(KEY_MIN_DEEP_MAINTENANCE_TIME, 30000L);
                                break;
                            case '\t':
                                long defaultInactiveTimeout = this.mSmallBatteryDevice ? 900000L : 1800000L;
                                this.INACTIVE_TIMEOUT = properties.getLong(KEY_INACTIVE_TIMEOUT, defaultInactiveTimeout);
                                break;
                            case '\n':
                                this.SENSING_TIMEOUT = properties.getLong(KEY_SENSING_TIMEOUT, 240000L);
                                break;
                            case 11:
                                this.LOCATING_TIMEOUT = properties.getLong(KEY_LOCATING_TIMEOUT, 30000L);
                                break;
                            case '\f':
                                this.LOCATION_ACCURACY = properties.getFloat(KEY_LOCATION_ACCURACY, (float) DEFAULT_LOCATION_ACCURACY);
                                break;
                            case '\r':
                                this.MOTION_INACTIVE_TIMEOUT = properties.getLong(KEY_MOTION_INACTIVE_TIMEOUT, 600000L);
                                break;
                            case 14:
                                this.MOTION_INACTIVE_TIMEOUT_FLEX = properties.getLong(KEY_MOTION_INACTIVE_TIMEOUT_FLEX, 60000L);
                                break;
                            case 15:
                                long defaultIdleAfterInactiveTimeout = this.mSmallBatteryDevice ? 900000L : 1800000L;
                                this.IDLE_AFTER_INACTIVE_TIMEOUT = properties.getLong(KEY_IDLE_AFTER_INACTIVE_TIMEOUT, defaultIdleAfterInactiveTimeout);
                                break;
                            case 16:
                                this.IDLE_PENDING_TIMEOUT = properties.getLong(KEY_IDLE_PENDING_TIMEOUT, (long) BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS);
                                break;
                            case 17:
                                this.MAX_IDLE_PENDING_TIMEOUT = properties.getLong(KEY_MAX_IDLE_PENDING_TIMEOUT, 600000L);
                                break;
                            case 18:
                                this.IDLE_PENDING_FACTOR = properties.getFloat(KEY_IDLE_PENDING_FACTOR, 2.0f);
                                break;
                            case 19:
                                this.QUICK_DOZE_DELAY_TIMEOUT = properties.getLong(KEY_QUICK_DOZE_DELAY_TIMEOUT, 60000L);
                                break;
                            case 20:
                                this.IDLE_TIMEOUT = properties.getLong(KEY_IDLE_TIMEOUT, 3600000L);
                                break;
                            case 21:
                                this.MAX_IDLE_TIMEOUT = properties.getLong(KEY_MAX_IDLE_TIMEOUT, (long) DEFAULT_MAX_IDLE_TIMEOUT);
                                break;
                            case 22:
                                this.IDLE_FACTOR = properties.getFloat(KEY_IDLE_FACTOR, 2.0f);
                                break;
                            case 23:
                                this.MIN_TIME_TO_ALARM = properties.getLong(KEY_MIN_TIME_TO_ALARM, 1800000L);
                                break;
                            case 24:
                                this.MAX_TEMP_APP_ALLOWLIST_DURATION_MS = properties.getLong(KEY_MAX_TEMP_APP_ALLOWLIST_DURATION_MS, (long) BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS);
                                break;
                            case 25:
                                this.MMS_TEMP_APP_ALLOWLIST_DURATION_MS = properties.getLong(KEY_MMS_TEMP_APP_ALLOWLIST_DURATION_MS, 60000L);
                                break;
                            case 26:
                                this.SMS_TEMP_APP_ALLOWLIST_DURATION_MS = properties.getLong(KEY_SMS_TEMP_APP_ALLOWLIST_DURATION_MS, (long) DEFAULT_SMS_TEMP_APP_ALLOWLIST_DURATION_MS);
                                break;
                            case 27:
                                this.NOTIFICATION_ALLOWLIST_DURATION_MS = properties.getLong(KEY_NOTIFICATION_ALLOWLIST_DURATION_MS, 30000L);
                                break;
                            case 28:
                                this.WAIT_FOR_UNLOCK = properties.getBoolean(KEY_WAIT_FOR_UNLOCK, true);
                                break;
                            case 29:
                                this.PRE_IDLE_FACTOR_LONG = properties.getFloat(KEY_PRE_IDLE_FACTOR_LONG, (float) DEFAULT_PRE_IDLE_FACTOR_LONG);
                                break;
                            case 30:
                                this.PRE_IDLE_FACTOR_SHORT = properties.getFloat(KEY_PRE_IDLE_FACTOR_SHORT, (float) DEFAULT_PRE_IDLE_FACTOR_SHORT);
                                break;
                            case 31:
                                this.USE_WINDOW_ALARMS = properties.getBoolean(KEY_USE_WINDOW_ALARMS, true);
                                break;
                            default:
                                Slog.e(DeviceIdleController.TAG, "Unknown configuration key: " + name);
                                break;
                        }
                    }
                }
            }
        }

        void dump(PrintWriter pw) {
            pw.println("  Settings:");
            pw.print("    ");
            pw.print(KEY_FLEX_TIME_SHORT);
            pw.print("=");
            TimeUtils.formatDuration(this.FLEX_TIME_SHORT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LIGHT_IDLE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.LIGHT_IDLE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LIGHT_IDLE_FACTOR);
            pw.print("=");
            pw.print(this.LIGHT_IDLE_FACTOR);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LIGHT_MAX_IDLE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.LIGHT_MAX_IDLE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LIGHT_IDLE_MAINTENANCE_MIN_BUDGET);
            pw.print("=");
            TimeUtils.formatDuration(this.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LIGHT_IDLE_MAINTENANCE_MAX_BUDGET);
            pw.print("=");
            TimeUtils.formatDuration(this.LIGHT_IDLE_MAINTENANCE_MAX_BUDGET, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MIN_LIGHT_MAINTENANCE_TIME);
            pw.print("=");
            TimeUtils.formatDuration(this.MIN_LIGHT_MAINTENANCE_TIME, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MIN_DEEP_MAINTENANCE_TIME);
            pw.print("=");
            TimeUtils.formatDuration(this.MIN_DEEP_MAINTENANCE_TIME, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_INACTIVE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.INACTIVE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_SENSING_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.SENSING_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LOCATING_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.LOCATING_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LOCATION_ACCURACY);
            pw.print("=");
            pw.print(this.LOCATION_ACCURACY);
            pw.print("m");
            pw.println();
            pw.print("    ");
            pw.print(KEY_MOTION_INACTIVE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.MOTION_INACTIVE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MOTION_INACTIVE_TIMEOUT_FLEX);
            pw.print("=");
            TimeUtils.formatDuration(this.MOTION_INACTIVE_TIMEOUT_FLEX, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_IDLE_AFTER_INACTIVE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.IDLE_AFTER_INACTIVE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_IDLE_PENDING_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.IDLE_PENDING_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MAX_IDLE_PENDING_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.MAX_IDLE_PENDING_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_IDLE_PENDING_FACTOR);
            pw.print("=");
            pw.println(this.IDLE_PENDING_FACTOR);
            pw.print("    ");
            pw.print(KEY_QUICK_DOZE_DELAY_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.QUICK_DOZE_DELAY_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_IDLE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.IDLE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MAX_IDLE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.MAX_IDLE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_IDLE_FACTOR);
            pw.print("=");
            pw.println(this.IDLE_FACTOR);
            pw.print("    ");
            pw.print(KEY_MIN_TIME_TO_ALARM);
            pw.print("=");
            TimeUtils.formatDuration(this.MIN_TIME_TO_ALARM, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MAX_TEMP_APP_ALLOWLIST_DURATION_MS);
            pw.print("=");
            TimeUtils.formatDuration(this.MAX_TEMP_APP_ALLOWLIST_DURATION_MS, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MMS_TEMP_APP_ALLOWLIST_DURATION_MS);
            pw.print("=");
            TimeUtils.formatDuration(this.MMS_TEMP_APP_ALLOWLIST_DURATION_MS, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_SMS_TEMP_APP_ALLOWLIST_DURATION_MS);
            pw.print("=");
            TimeUtils.formatDuration(this.SMS_TEMP_APP_ALLOWLIST_DURATION_MS, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_NOTIFICATION_ALLOWLIST_DURATION_MS);
            pw.print("=");
            TimeUtils.formatDuration(this.NOTIFICATION_ALLOWLIST_DURATION_MS, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_WAIT_FOR_UNLOCK);
            pw.print("=");
            pw.println(this.WAIT_FOR_UNLOCK);
            pw.print("    ");
            pw.print(KEY_PRE_IDLE_FACTOR_LONG);
            pw.print("=");
            pw.println(this.PRE_IDLE_FACTOR_LONG);
            pw.print("    ");
            pw.print(KEY_PRE_IDLE_FACTOR_SHORT);
            pw.print("=");
            pw.println(this.PRE_IDLE_FACTOR_SHORT);
            pw.print("    ");
            pw.print(KEY_USE_WINDOW_ALARMS);
            pw.print("=");
            pw.println(this.USE_WINDOW_ALARMS);
        }
    }

    @Override // com.android.server.AnyMotionDetector.DeviceIdleCallback
    public void onAnyMotionResult(int result) {
        synchronized (this) {
            if (result != -1) {
                try {
                    cancelSensingTimeoutAlarmLocked();
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (result != 1 && result != -1) {
                if (result == 0) {
                    int i = this.mState;
                    if (i == 3) {
                        this.mNotMoving = true;
                        stepIdleStateLocked("s:stationary");
                    } else if (i == 4) {
                        this.mNotMoving = true;
                        if (this.mLocated) {
                            stepIdleStateLocked("s:stationary");
                        }
                    }
                }
            }
            handleMotionDetectedLocked(this.mConstants.INACTIVE_TIMEOUT, "non_stationary");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class MyHandler extends Handler {
        MyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            boolean deepChanged;
            boolean lightChanged;
            boolean isStationary;
            DeviceIdleInternal.StationaryListener[] listeners;
            PowerAllowlistInternal.TempAllowlistChangeListener[] listeners2;
            int i = 0;
            switch (msg.what) {
                case 1:
                    DeviceIdleController.this.handleWriteConfigFile();
                    return;
                case 2:
                case 3:
                    EventLogTags.writeDeviceIdleOnStart();
                    if (msg.what == 2) {
                        deepChanged = DeviceIdleController.this.mLocalPowerManager.setDeviceIdleMode(true);
                        lightChanged = DeviceIdleController.this.mLocalPowerManager.setLightDeviceIdleMode(false);
                    } else {
                        deepChanged = DeviceIdleController.this.mLocalPowerManager.setDeviceIdleMode(false);
                        lightChanged = DeviceIdleController.this.mLocalPowerManager.setLightDeviceIdleMode(true);
                    }
                    try {
                        DeviceIdleController.this.mNetworkPolicyManager.setDeviceIdleMode(true);
                        DeviceIdleController.this.mBatteryStats.noteDeviceIdleMode(msg.what == 2 ? 2 : 1, (String) null, Process.myUid());
                    } catch (RemoteException e) {
                    }
                    if (deepChanged) {
                        DeviceIdleController.this.getContext().sendBroadcastAsUser(DeviceIdleController.this.mIdleIntent, UserHandle.ALL);
                    }
                    if (lightChanged) {
                        DeviceIdleController.this.getContext().sendBroadcastAsUser(DeviceIdleController.this.mLightIdleIntent, UserHandle.ALL);
                    }
                    EventLogTags.writeDeviceIdleOnComplete();
                    DeviceIdleController.this.mGoingIdleWakeLock.release();
                    return;
                case 4:
                    EventLogTags.writeDeviceIdleOffStart(UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN);
                    boolean deepChanged2 = DeviceIdleController.this.mLocalPowerManager.setDeviceIdleMode(false);
                    boolean lightChanged2 = DeviceIdleController.this.mLocalPowerManager.setLightDeviceIdleMode(false);
                    try {
                        DeviceIdleController.this.mNetworkPolicyManager.setDeviceIdleMode(false);
                        DeviceIdleController.this.mBatteryStats.noteDeviceIdleMode(0, (String) null, Process.myUid());
                    } catch (RemoteException e2) {
                    }
                    if (deepChanged2) {
                        DeviceIdleController.this.incActiveIdleOps();
                        DeviceIdleController.this.getContext().sendOrderedBroadcastAsUser(DeviceIdleController.this.mIdleIntent, UserHandle.ALL, null, DeviceIdleController.this.mIdleStartedDoneReceiver, null, 0, null, null);
                    }
                    if (lightChanged2) {
                        DeviceIdleController.this.incActiveIdleOps();
                        DeviceIdleController.this.getContext().sendOrderedBroadcastAsUser(DeviceIdleController.this.mLightIdleIntent, UserHandle.ALL, null, DeviceIdleController.this.mIdleStartedDoneReceiver, null, 0, null, null);
                    }
                    DeviceIdleController.this.decActiveIdleOps();
                    EventLogTags.writeDeviceIdleOffComplete();
                    return;
                case 5:
                    String activeReason = (String) msg.obj;
                    int activeUid = msg.arg1;
                    EventLogTags.writeDeviceIdleOffStart(activeReason != null ? activeReason : UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN);
                    boolean deepChanged3 = DeviceIdleController.this.mLocalPowerManager.setDeviceIdleMode(false);
                    boolean lightChanged3 = DeviceIdleController.this.mLocalPowerManager.setLightDeviceIdleMode(false);
                    try {
                        DeviceIdleController.this.mNetworkPolicyManager.setDeviceIdleMode(false);
                        DeviceIdleController.this.mBatteryStats.noteDeviceIdleMode(0, activeReason, activeUid);
                    } catch (RemoteException e3) {
                    }
                    if (deepChanged3) {
                        DeviceIdleController.this.getContext().sendBroadcastAsUser(DeviceIdleController.this.mIdleIntent, UserHandle.ALL);
                    }
                    if (lightChanged3) {
                        DeviceIdleController.this.getContext().sendBroadcastAsUser(DeviceIdleController.this.mLightIdleIntent, UserHandle.ALL);
                    }
                    EventLogTags.writeDeviceIdleOffComplete();
                    return;
                case 6:
                    DeviceIdleController.this.checkTempAppWhitelistTimeout(msg.arg1);
                    return;
                case 7:
                    DeviceIdleInternal.StationaryListener newListener = (DeviceIdleInternal.StationaryListener) msg.obj;
                    synchronized (DeviceIdleController.this) {
                        isStationary = DeviceIdleController.this.isStationaryLocked();
                        if (newListener == null) {
                            listeners = (DeviceIdleInternal.StationaryListener[]) DeviceIdleController.this.mStationaryListeners.toArray(new DeviceIdleInternal.StationaryListener[DeviceIdleController.this.mStationaryListeners.size()]);
                        } else {
                            listeners = null;
                        }
                    }
                    if (listeners != null) {
                        int length = listeners.length;
                        while (i < length) {
                            listeners[i].onDeviceStationaryChanged(isStationary);
                            i++;
                        }
                    }
                    if (newListener != null) {
                        newListener.onDeviceStationaryChanged(isStationary);
                        return;
                    }
                    return;
                case 8:
                    DeviceIdleController.this.decActiveIdleOps();
                    return;
                case 9:
                default:
                    return;
                case 10:
                    IDeviceIdleConstraint constraint = (IDeviceIdleConstraint) msg.obj;
                    if ((msg.arg1 != 1 ? 0 : 1) != 0) {
                        constraint.startMonitoring();
                        return;
                    } else {
                        constraint.stopMonitoring();
                        return;
                    }
                case 11:
                    DeviceIdleController.this.updatePreIdleFactor();
                    return;
                case 12:
                    DeviceIdleController.this.updatePreIdleFactor();
                    DeviceIdleController.this.maybeDoImmediateMaintenance();
                    return;
                case 13:
                    int uid = msg.arg1;
                    int i2 = msg.arg2 != 1 ? 0 : 1;
                    synchronized (DeviceIdleController.this) {
                        listeners2 = (PowerAllowlistInternal.TempAllowlistChangeListener[]) DeviceIdleController.this.mTempAllowlistChangeListeners.toArray(new PowerAllowlistInternal.TempAllowlistChangeListener[DeviceIdleController.this.mTempAllowlistChangeListeners.size()]);
                    }
                    int length2 = listeners2.length;
                    while (i < length2) {
                        PowerAllowlistInternal.TempAllowlistChangeListener listener = listeners2[i];
                        if (i2 != 0) {
                            listener.onAppAdded(uid);
                        } else {
                            listener.onAppRemoved(uid);
                        }
                        i++;
                    }
                    return;
                case 14:
                    int appId = msg.arg1;
                    int reasonCode = msg.arg2;
                    String reason = (String) msg.obj;
                    DeviceIdleController.this.mNetworkPolicyManagerInternal.onTempPowerSaveWhitelistChange(appId, true, reasonCode, reason);
                    return;
                case 15:
                    int appId2 = msg.arg1;
                    DeviceIdleController.this.mNetworkPolicyManagerInternal.onTempPowerSaveWhitelistChange(appId2, false, 0, null);
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class BinderService extends IDeviceIdleController.Stub {
        private BinderService() {
        }

        public void addPowerSaveWhitelistApp(String name) {
            addPowerSaveWhitelistApps(Collections.singletonList(name));
        }

        public int addPowerSaveWhitelistApps(List<String> packageNames) {
            DeviceIdleController.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                return DeviceIdleController.this.addPowerSaveWhitelistAppsInternal(packageNames);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void removePowerSaveWhitelistApp(String name) {
            DeviceIdleController.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                if (!DeviceIdleController.this.removePowerSaveWhitelistAppInternal(name) && DeviceIdleController.this.mPowerSaveWhitelistAppsExceptIdle.containsKey(name)) {
                    throw new UnsupportedOperationException("Cannot remove system whitelisted app");
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void removeSystemPowerWhitelistApp(String name) {
            DeviceIdleController.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                DeviceIdleController.this.removeSystemPowerWhitelistAppInternal(name);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void restoreSystemPowerWhitelistApp(String name) {
            DeviceIdleController.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                DeviceIdleController.this.restoreSystemPowerWhitelistAppInternal(name);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public String[] getRemovedSystemPowerWhitelistApps() {
            return DeviceIdleController.this.getRemovedSystemPowerWhitelistAppsInternal(Binder.getCallingUid(), UserHandle.getCallingUserId());
        }

        public String[] getSystemPowerWhitelistExceptIdle() {
            return DeviceIdleController.this.getSystemPowerWhitelistExceptIdleInternal(Binder.getCallingUid(), UserHandle.getCallingUserId());
        }

        public String[] getSystemPowerWhitelist() {
            return DeviceIdleController.this.getSystemPowerWhitelistInternal(Binder.getCallingUid(), UserHandle.getCallingUserId());
        }

        public String[] getUserPowerWhitelist() {
            return DeviceIdleController.this.getUserPowerWhitelistInternal(Binder.getCallingUid(), UserHandle.getCallingUserId());
        }

        public String[] getFullPowerWhitelistExceptIdle() {
            return DeviceIdleController.this.getFullPowerWhitelistExceptIdleInternal(Binder.getCallingUid(), UserHandle.getCallingUserId());
        }

        public String[] getFullPowerWhitelist() {
            return DeviceIdleController.this.getFullPowerWhitelistInternal(Binder.getCallingUid(), UserHandle.getCallingUserId());
        }

        public int[] getAppIdWhitelistExceptIdle() {
            return DeviceIdleController.this.getAppIdWhitelistExceptIdleInternal();
        }

        public int[] getAppIdWhitelist() {
            return DeviceIdleController.this.getAppIdWhitelistInternal();
        }

        public int[] getAppIdUserWhitelist() {
            return DeviceIdleController.this.getAppIdUserWhitelistInternal();
        }

        public int[] getAppIdTempWhitelist() {
            return DeviceIdleController.this.getAppIdTempWhitelistInternal();
        }

        public boolean isPowerSaveWhitelistExceptIdleApp(String name) {
            if (DeviceIdleController.this.mPackageManagerInternal.filterAppAccess(name, Binder.getCallingUid(), UserHandle.getCallingUserId())) {
                return false;
            }
            return DeviceIdleController.this.isPowerSaveWhitelistExceptIdleAppInternal(name);
        }

        public boolean isPowerSaveWhitelistApp(String name) {
            if (DeviceIdleController.this.mPackageManagerInternal.filterAppAccess(name, Binder.getCallingUid(), UserHandle.getCallingUserId())) {
                return false;
            }
            return DeviceIdleController.this.isPowerSaveWhitelistAppInternal(name);
        }

        public long whitelistAppTemporarily(String packageName, int userId, int reasonCode, String reason) throws RemoteException {
            long durationMs = Math.max((long) JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, DeviceIdleController.this.mConstants.MAX_TEMP_APP_ALLOWLIST_DURATION_MS / 2);
            DeviceIdleController.this.addPowerSaveTempAllowlistAppChecked(packageName, durationMs, userId, reasonCode, reason);
            return durationMs;
        }

        public void addPowerSaveTempWhitelistApp(String packageName, long duration, int userId, int reasonCode, String reason) throws RemoteException {
            DeviceIdleController.this.addPowerSaveTempAllowlistAppChecked(packageName, duration, userId, reasonCode, reason);
        }

        public long addPowerSaveTempWhitelistAppForMms(String packageName, int userId, int reasonCode, String reason) throws RemoteException {
            long durationMs = DeviceIdleController.this.mConstants.MMS_TEMP_APP_ALLOWLIST_DURATION_MS;
            DeviceIdleController.this.addPowerSaveTempAllowlistAppChecked(packageName, durationMs, userId, reasonCode, reason);
            return durationMs;
        }

        public long addPowerSaveTempWhitelistAppForSms(String packageName, int userId, int reasonCode, String reason) throws RemoteException {
            long durationMs = DeviceIdleController.this.mConstants.SMS_TEMP_APP_ALLOWLIST_DURATION_MS;
            DeviceIdleController.this.addPowerSaveTempAllowlistAppChecked(packageName, durationMs, userId, reasonCode, reason);
            return durationMs;
        }

        public void exitIdle(String reason) {
            DeviceIdleController.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                DeviceIdleController.this.exitIdleInternal(reason);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public int setPreIdleTimeoutMode(int mode) {
            DeviceIdleController.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                return DeviceIdleController.this.setPreIdleTimeoutMode(mode);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void resetPreIdleTimeoutMode() {
            DeviceIdleController.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                DeviceIdleController.this.resetPreIdleTimeoutMode();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            DeviceIdleController.this.dump(fd, pw, args);
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.DeviceIdleController$BinderService */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new Shell().exec(this, in, out, err, args, callback, resultReceiver);
        }
    }

    /* loaded from: classes.dex */
    private class LocalService implements DeviceIdleInternal {
        private LocalService() {
        }

        public void onConstraintStateChanged(IDeviceIdleConstraint constraint, boolean active) {
            synchronized (DeviceIdleController.this) {
                DeviceIdleController.this.onConstraintStateChangedLocked(constraint, active);
            }
        }

        public void registerDeviceIdleConstraint(IDeviceIdleConstraint constraint, String name, int minState) {
            DeviceIdleController.this.registerDeviceIdleConstraintInternal(constraint, name, minState);
        }

        public void unregisterDeviceIdleConstraint(IDeviceIdleConstraint constraint) {
            DeviceIdleController.this.unregisterDeviceIdleConstraintInternal(constraint);
        }

        public void exitIdle(String reason) {
            DeviceIdleController.this.exitIdleInternal(reason);
        }

        public void addPowerSaveTempWhitelistApp(int callingUid, String packageName, long durationMs, int userId, boolean sync, int reasonCode, String reason) {
            DeviceIdleController.this.addPowerSaveTempAllowlistAppInternal(callingUid, packageName, durationMs, 0, userId, sync, reasonCode, reason);
        }

        public void addPowerSaveTempWhitelistApp(int callingUid, String packageName, long durationMs, int tempAllowListType, int userId, boolean sync, int reasonCode, String reason) {
            DeviceIdleController.this.addPowerSaveTempAllowlistAppInternal(callingUid, packageName, durationMs, tempAllowListType, userId, sync, reasonCode, reason);
        }

        public void addPowerSaveTempWhitelistAppDirect(int uid, long durationMs, int tempAllowListType, boolean sync, int reasonCode, String reason, int callingUid) {
            DeviceIdleController.this.addPowerSaveTempWhitelistAppDirectInternal(callingUid, uid, durationMs, tempAllowListType, sync, reasonCode, reason);
        }

        public long getNotificationAllowlistDuration() {
            return DeviceIdleController.this.mConstants.NOTIFICATION_ALLOWLIST_DURATION_MS;
        }

        public void setJobsActive(boolean active) {
            DeviceIdleController.this.setJobsActive(active);
        }

        public void setAlarmsActive(boolean active) {
            DeviceIdleController.this.setAlarmsActive(active);
        }

        public boolean isAppOnWhitelist(int appid) {
            return DeviceIdleController.this.isAppOnWhitelistInternal(appid);
        }

        public int[] getPowerSaveWhitelistUserAppIds() {
            return DeviceIdleController.this.getPowerSaveWhitelistUserAppIds();
        }

        public int[] getPowerSaveTempWhitelistAppIds() {
            return DeviceIdleController.this.getAppIdTempWhitelistInternal();
        }

        public void registerStationaryListener(DeviceIdleInternal.StationaryListener listener) {
            DeviceIdleController.this.registerStationaryListener(listener);
        }

        public void unregisterStationaryListener(DeviceIdleInternal.StationaryListener listener) {
            DeviceIdleController.this.unregisterStationaryListener(listener);
        }

        public int getTempAllowListType(int reasonCode, int defaultType) {
            return DeviceIdleController.this.getTempAllowListType(reasonCode, defaultType);
        }
    }

    /* loaded from: classes.dex */
    private class LocalPowerAllowlistService implements PowerAllowlistInternal {
        private LocalPowerAllowlistService() {
        }

        public void registerTempAllowlistChangeListener(PowerAllowlistInternal.TempAllowlistChangeListener listener) {
            DeviceIdleController.this.registerTempAllowlistChangeListener(listener);
        }

        public void unregisterTempAllowlistChangeListener(PowerAllowlistInternal.TempAllowlistChangeListener listener) {
            DeviceIdleController.this.unregisterTempAllowlistChangeListener(listener);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Injector {
        private ConnectivityManager mConnectivityManager;
        private Constants mConstants;
        private final Context mContext;
        private LocationManager mLocationManager;

        Injector(Context ctx) {
            this.mContext = ctx.createAttributionContext(DeviceIdleController.TAG);
        }

        AlarmManager getAlarmManager() {
            return (AlarmManager) this.mContext.getSystemService(AlarmManager.class);
        }

        AnyMotionDetector getAnyMotionDetector(Handler handler, SensorManager sm, AnyMotionDetector.DeviceIdleCallback callback, float angleThreshold) {
            return new AnyMotionDetector(getPowerManager(), handler, sm, callback, angleThreshold);
        }

        AppStateTrackerImpl getAppStateTracker(Context ctx, Looper looper) {
            return new AppStateTrackerImpl(ctx, looper);
        }

        ConnectivityManager getConnectivityManager() {
            if (this.mConnectivityManager == null) {
                this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class);
            }
            return this.mConnectivityManager;
        }

        Constants getConstants(DeviceIdleController controller) {
            if (this.mConstants == null) {
                Objects.requireNonNull(controller);
                this.mConstants = new Constants();
            }
            return this.mConstants;
        }

        long getElapsedRealtime() {
            return SystemClock.elapsedRealtime();
        }

        LocationManager getLocationManager() {
            if (this.mLocationManager == null) {
                this.mLocationManager = (LocationManager) this.mContext.getSystemService(LocationManager.class);
            }
            return this.mLocationManager;
        }

        MyHandler getHandler(DeviceIdleController controller) {
            Objects.requireNonNull(controller);
            return new MyHandler(JobSchedulerBackgroundThread.getHandler().getLooper());
        }

        Sensor getMotionSensor() {
            SensorManager sensorManager = getSensorManager();
            Sensor motionSensor = null;
            int sigMotionSensorId = this.mContext.getResources().getInteger(17694744);
            if (sigMotionSensorId > 0) {
                motionSensor = sensorManager.getDefaultSensor(sigMotionSensorId, true);
            }
            if (motionSensor == null && this.mContext.getResources().getBoolean(17891374)) {
                motionSensor = sensorManager.getDefaultSensor(26, true);
            }
            if (motionSensor == null) {
                Sensor motionSensor2 = sensorManager.getDefaultSensor(17, true);
                return motionSensor2;
            }
            return motionSensor;
        }

        PowerManager getPowerManager() {
            return (PowerManager) this.mContext.getSystemService(PowerManager.class);
        }

        SensorManager getSensorManager() {
            return (SensorManager) this.mContext.getSystemService(SensorManager.class);
        }

        ConstraintController getConstraintController(Handler handler, DeviceIdleInternal localService) {
            if (this.mContext.getPackageManager().hasSystemFeature("android.software.leanback_only")) {
                return new TvConstraintController(this.mContext, handler);
            }
            return null;
        }

        boolean useMotionSensor() {
            return this.mContext.getResources().getBoolean(17891376);
        }
    }

    DeviceIdleController(Context context, Injector injector) {
        super(context);
        this.mNumBlockingConstraints = 0;
        this.mConstraints = new ArrayMap<>();
        this.mPowerSaveWhitelistAppsExceptIdle = new ArrayMap<>();
        this.mPowerSaveWhitelistUserAppsExceptIdle = new ArraySet<>();
        this.mPowerSaveWhitelistApps = new ArrayMap<>();
        this.mPowerSaveWhitelistUserApps = new ArrayMap<>();
        this.mPowerSaveWhitelistSystemAppIdsExceptIdle = new SparseBooleanArray();
        this.mPowerSaveWhitelistSystemAppIds = new SparseBooleanArray();
        this.mPowerSaveWhitelistExceptIdleAppIds = new SparseBooleanArray();
        this.mPowerSaveWhitelistExceptIdleAppIdArray = new int[0];
        this.mPowerSaveWhitelistAllAppIds = new SparseBooleanArray();
        this.mPowerSaveWhitelistAllAppIdArray = new int[0];
        this.mPowerSaveWhitelistUserAppIds = new SparseBooleanArray();
        this.mPowerSaveWhitelistUserAppIdArray = new int[0];
        this.mTempWhitelistAppIdEndTimes = new SparseArray<>();
        this.mTempWhitelistAppIdArray = new int[0];
        this.mRemovedFromSystemWhitelistApps = new ArrayMap<>();
        this.mStationaryListeners = new ArraySet<>();
        this.mAllowInIdleUserAppArray = new ArraySet<>();
        this.mTempAllowlistChangeListeners = new ArraySet<>();
        this.mEventCmds = new int[100];
        this.mEventTimes = new long[100];
        this.mEventReasons = new String[100];
        this.mReceiver = new BroadcastReceiver() { // from class: com.android.server.DeviceIdleController.1
            /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                char c;
                Uri data;
                String ssp;
                String pkgName;
                String action = intent.getAction();
                boolean z = true;
                switch (action.hashCode()) {
                    case -1538406691:
                        if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1172645946:
                        if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case 525384130:
                        if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1544582882:
                        if (action.equals("android.intent.action.PACKAGE_ADDED")) {
                            c = 3;
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
                        DeviceIdleController.this.updateConnectivityState(intent);
                        return;
                    case 1:
                        boolean present = intent.getBooleanExtra("present", true);
                        boolean plugged = intent.getIntExtra("plugged", 0) != 0;
                        synchronized (DeviceIdleController.this) {
                            DeviceIdleController deviceIdleController = DeviceIdleController.this;
                            if (!present || !plugged) {
                                z = false;
                            }
                            deviceIdleController.updateChargingLocked(z);
                        }
                        return;
                    case 2:
                        if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false) && (data = intent.getData()) != null && (ssp = data.getSchemeSpecificPart()) != null) {
                            DeviceIdleController.this.removePowerSaveWhitelistAppInternal(ssp);
                            return;
                        }
                        return;
                    case 3:
                        Uri data2 = intent.getData();
                        if (data2 != null && (pkgName = data2.getSchemeSpecificPart()) != null) {
                            boolean bInPkgList = false;
                            synchronized (DeviceIdleController.this) {
                                if (DeviceIdleController.this.mAllowInIdleUserAppArray.contains(pkgName) && !DeviceIdleController.this.mPowerSaveWhitelistApps.containsKey(pkgName)) {
                                    bInPkgList = true;
                                }
                            }
                            if (bInPkgList) {
                                DeviceIdleController.this.addPowerSaveWhitelistAppsInternal(Collections.singletonList(pkgName));
                                return;
                            }
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
        this.mLightAlarmListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.DeviceIdleController$$ExternalSyntheticLambda2
            @Override // android.app.AlarmManager.OnAlarmListener
            public final void onAlarm() {
                DeviceIdleController.this.m155lambda$new$0$comandroidserverDeviceIdleController();
            }
        };
        this.mLightMaintenanceAlarmListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.DeviceIdleController$$ExternalSyntheticLambda3
            @Override // android.app.AlarmManager.OnAlarmListener
            public final void onAlarm() {
                DeviceIdleController.this.m156lambda$new$1$comandroidserverDeviceIdleController();
            }
        };
        this.mMotionRegistrationAlarmListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.DeviceIdleController$$ExternalSyntheticLambda4
            @Override // android.app.AlarmManager.OnAlarmListener
            public final void onAlarm() {
                DeviceIdleController.this.m157lambda$new$2$comandroidserverDeviceIdleController();
            }
        };
        this.mMotionTimeoutAlarmListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.DeviceIdleController$$ExternalSyntheticLambda5
            @Override // android.app.AlarmManager.OnAlarmListener
            public final void onAlarm() {
                DeviceIdleController.this.m158lambda$new$3$comandroidserverDeviceIdleController();
            }
        };
        this.mSensingTimeoutAlarmListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.DeviceIdleController.2
            @Override // android.app.AlarmManager.OnAlarmListener
            public void onAlarm() {
                synchronized (DeviceIdleController.this) {
                    if (DeviceIdleController.this.mState == 3) {
                        DeviceIdleController.this.becomeInactiveIfAppropriateLocked();
                    }
                }
            }
        };
        this.mDeepAlarmListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.DeviceIdleController.3
            @Override // android.app.AlarmManager.OnAlarmListener
            public void onAlarm() {
                synchronized (DeviceIdleController.this) {
                    DeviceIdleController.this.stepIdleStateLocked("s:alarm");
                }
            }
        };
        this.mIdleStartedDoneReceiver = new BroadcastReceiver() { // from class: com.android.server.DeviceIdleController.4
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if ("android.os.action.DEVICE_IDLE_MODE_CHANGED".equals(intent.getAction())) {
                    DeviceIdleController.this.mHandler.sendEmptyMessageDelayed(8, DeviceIdleController.this.mConstants.MIN_DEEP_MAINTENANCE_TIME);
                } else {
                    DeviceIdleController.this.mHandler.sendEmptyMessageDelayed(8, DeviceIdleController.this.mConstants.MIN_LIGHT_MAINTENANCE_TIME);
                }
            }
        };
        this.mInteractivityReceiver = new BroadcastReceiver() { // from class: com.android.server.DeviceIdleController.5
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                synchronized (DeviceIdleController.this) {
                    DeviceIdleController.this.updateInteractivityLocked();
                }
            }
        };
        this.mMotionListener = new MotionListener();
        this.mGenericLocationListener = new LocationListener() { // from class: com.android.server.DeviceIdleController.6
            @Override // android.location.LocationListener
            public void onLocationChanged(Location location) {
                synchronized (DeviceIdleController.this) {
                    DeviceIdleController.this.receivedGenericLocationLocked(location);
                }
            }

            @Override // android.location.LocationListener
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override // android.location.LocationListener
            public void onProviderEnabled(String provider) {
            }

            @Override // android.location.LocationListener
            public void onProviderDisabled(String provider) {
            }
        };
        this.mGpsLocationListener = new LocationListener() { // from class: com.android.server.DeviceIdleController.7
            @Override // android.location.LocationListener
            public void onLocationChanged(Location location) {
                synchronized (DeviceIdleController.this) {
                    DeviceIdleController.this.receivedGpsLocationLocked(location);
                }
            }

            @Override // android.location.LocationListener
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override // android.location.LocationListener
            public void onProviderEnabled(String provider) {
            }

            @Override // android.location.LocationListener
            public void onProviderDisabled(String provider) {
            }
        };
        this.mScreenObserver = new ActivityTaskManagerInternal.ScreenObserver() { // from class: com.android.server.DeviceIdleController.8
            @Override // com.android.server.wm.ActivityTaskManagerInternal.ScreenObserver
            public void onAwakeStateChanged(boolean isAwake) {
            }

            @Override // com.android.server.wm.ActivityTaskManagerInternal.ScreenObserver
            public void onKeyguardStateChanged(boolean isShowing) {
                synchronized (DeviceIdleController.this) {
                    DeviceIdleController.this.keyguardShowingLocked(isShowing);
                }
            }
        };
        this.mInjector = injector;
        this.mConfigFile = new AtomicFile(new File(getSystemDir(), "deviceidle.xml"));
        this.mHandler = injector.getHandler(this);
        AppStateTrackerImpl appStateTracker = injector.getAppStateTracker(context, JobSchedulerBackgroundThread.get().getLooper());
        this.mAppStateTracker = appStateTracker;
        LocalServices.addService(AppStateTracker.class, appStateTracker);
        this.mUseMotionSensor = injector.useMotionSensor();
    }

    public DeviceIdleController(Context context) {
        this(context, new Injector(context));
    }

    boolean isAppOnWhitelistInternal(int appid) {
        boolean z;
        synchronized (this) {
            z = Arrays.binarySearch(this.mPowerSaveWhitelistAllAppIdArray, appid) >= 0;
        }
        return z;
    }

    int[] getPowerSaveWhitelistUserAppIds() {
        int[] iArr;
        synchronized (this) {
            iArr = this.mPowerSaveWhitelistUserAppIdArray;
        }
        return iArr;
    }

    private static File getSystemDir() {
        return new File(Environment.getDataDirectory(), HostingRecord.HOSTING_TYPE_SYSTEM);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r13v0, resolved type: com.android.server.DeviceIdleController */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r1v5, types: [com.android.server.DeviceIdleController$BinderService, android.os.IBinder] */
    @Override // com.android.server.SystemService
    public void onStart() {
        PackageManager pm = getContext().getPackageManager();
        synchronized (this) {
            boolean z = getContext().getResources().getBoolean(17891626);
            this.mDeepEnabled = z;
            this.mLightEnabled = z;
            SystemConfig sysConfig = SystemConfig.getInstance();
            this.mAllowInIdleUserAppArray = sysConfig.getAllowInIdleUserApp();
            for (int i = 0; i < this.mAllowInIdleUserAppArray.size(); i++) {
                String pkg = this.mAllowInIdleUserAppArray.valueAt(i);
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(pkg, 131072);
                    int appid = UserHandle.getAppId(ai.uid);
                    this.mPowerSaveWhitelistApps.put(ai.packageName, Integer.valueOf(appid));
                    this.mPowerSaveWhitelistSystemAppIds.put(appid, true);
                } catch (Exception e) {
                }
            }
            ArraySet<String> allowPowerExceptIdle = sysConfig.getAllowInPowerSaveExceptIdle();
            for (int i2 = 0; i2 < allowPowerExceptIdle.size(); i2++) {
                String pkg2 = allowPowerExceptIdle.valueAt(i2);
                try {
                    ApplicationInfo ai2 = pm.getApplicationInfo(pkg2, 1048576);
                    int appid2 = UserHandle.getAppId(ai2.uid);
                    this.mPowerSaveWhitelistAppsExceptIdle.put(ai2.packageName, Integer.valueOf(appid2));
                    this.mPowerSaveWhitelistSystemAppIdsExceptIdle.put(appid2, true);
                } catch (PackageManager.NameNotFoundException e2) {
                }
            }
            ArraySet<String> allowPower = sysConfig.getAllowInPowerSave();
            for (int i3 = 0; i3 < allowPower.size(); i3++) {
                String pkg3 = allowPower.valueAt(i3);
                try {
                    ApplicationInfo ai3 = pm.getApplicationInfo(pkg3, 1048576);
                    int appid3 = UserHandle.getAppId(ai3.uid);
                    this.mPowerSaveWhitelistAppsExceptIdle.put(ai3.packageName, Integer.valueOf(appid3));
                    this.mPowerSaveWhitelistSystemAppIdsExceptIdle.put(appid3, true);
                    this.mPowerSaveWhitelistApps.put(ai3.packageName, Integer.valueOf(appid3));
                    this.mPowerSaveWhitelistSystemAppIds.put(appid3, true);
                } catch (PackageManager.NameNotFoundException e3) {
                }
            }
            this.mConstants = this.mInjector.getConstants(this);
            readConfigFileLocked();
            updateWhitelistAppIdsLocked();
            this.mNetworkConnected = true;
            this.mScreenOn = true;
            this.mScreenLocked = false;
            this.mCharging = true;
            this.mActiveReason = 0;
            this.mState = 0;
            this.mLightState = 0;
            this.mInactiveTimeout = this.mConstants.INACTIVE_TIMEOUT;
            this.mPreIdleFactor = 1.0f;
            this.mLastPreIdleFactor = 1.0f;
        }
        ?? binderService = new BinderService();
        this.mBinderService = binderService;
        publishBinderService("deviceidle", binderService);
        LocalService localService = new LocalService();
        this.mLocalService = localService;
        publishLocalService(DeviceIdleInternal.class, localService);
        publishLocalService(PowerAllowlistInternal.class, new LocalPowerAllowlistService());
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 500) {
            synchronized (this) {
                this.mAlarmManager = this.mInjector.getAlarmManager();
                this.mLocalAlarmManager = (AlarmManagerInternal) getLocalService(AlarmManagerInternal.class);
                this.mBatteryStats = BatteryStatsService.getService();
                this.mLocalActivityManager = (ActivityManagerInternal) getLocalService(ActivityManagerInternal.class);
                this.mLocalActivityTaskManager = (ActivityTaskManagerInternal) getLocalService(ActivityTaskManagerInternal.class);
                this.mPackageManagerInternal = (PackageManagerInternal) getLocalService(PackageManagerInternal.class);
                this.mLocalPowerManager = (PowerManagerInternal) getLocalService(PowerManagerInternal.class);
                PowerManager powerManager = this.mInjector.getPowerManager();
                this.mPowerManager = powerManager;
                PowerManager.WakeLock newWakeLock = powerManager.newWakeLock(1, "deviceidle_maint");
                this.mActiveIdleWakeLock = newWakeLock;
                newWakeLock.setReferenceCounted(false);
                PowerManager.WakeLock newWakeLock2 = this.mPowerManager.newWakeLock(1, "deviceidle_going_idle");
                this.mGoingIdleWakeLock = newWakeLock2;
                newWakeLock2.setReferenceCounted(true);
                this.mNetworkPolicyManager = INetworkPolicyManager.Stub.asInterface(ServiceManager.getService("netpolicy"));
                this.mNetworkPolicyManagerInternal = (NetworkPolicyManagerInternal) getLocalService(NetworkPolicyManagerInternal.class);
                this.mSensorManager = this.mInjector.getSensorManager();
                if (this.mUseMotionSensor) {
                    this.mMotionSensor = this.mInjector.getMotionSensor();
                }
                if (getContext().getResources().getBoolean(17891375)) {
                    this.mLocationRequest = new LocationRequest.Builder(0L).setQuality(100).setMaxUpdates(1).build();
                }
                ConstraintController constraintController = this.mInjector.getConstraintController(this.mHandler, (DeviceIdleInternal) getLocalService(LocalService.class));
                this.mConstraintController = constraintController;
                if (constraintController != null) {
                    constraintController.start();
                }
                float angleThreshold = getContext().getResources().getInteger(17694745) / 100.0f;
                this.mAnyMotionDetector = this.mInjector.getAnyMotionDetector(this.mHandler, this.mSensorManager, this, angleThreshold);
                this.mAppStateTracker.onSystemServicesReady();
                Intent intent = new Intent("android.os.action.DEVICE_IDLE_MODE_CHANGED");
                this.mIdleIntent = intent;
                intent.addFlags(1342177280);
                Intent intent2 = new Intent("android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED");
                this.mLightIdleIntent = intent2;
                intent2.addFlags(1342177280);
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.BATTERY_CHANGED");
                getContext().registerReceiver(this.mReceiver, filter);
                IntentFilter filter2 = new IntentFilter();
                filter2.addAction("android.intent.action.PACKAGE_ADDED");
                filter2.addAction("android.intent.action.PACKAGE_REMOVED");
                filter2.addDataScheme("package");
                getContext().registerReceiver(this.mReceiver, filter2);
                IntentFilter filter3 = new IntentFilter();
                filter3.addAction("android.net.conn.CONNECTIVITY_CHANGE");
                getContext().registerReceiver(this.mReceiver, filter3);
                IntentFilter filter4 = new IntentFilter();
                filter4.addAction("android.intent.action.SCREEN_OFF");
                filter4.addAction("android.intent.action.SCREEN_ON");
                getContext().registerReceiver(this.mInteractivityReceiver, filter4);
                this.mLocalActivityManager.setDeviceIdleAllowlist(this.mPowerSaveWhitelistAllAppIdArray, this.mPowerSaveWhitelistExceptIdleAppIdArray);
                this.mLocalPowerManager.setDeviceIdleWhitelist(this.mPowerSaveWhitelistAllAppIdArray);
                this.mLocalPowerManager.registerLowPowerModeObserver(15, new Consumer() { // from class: com.android.server.DeviceIdleController$$ExternalSyntheticLambda16
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        DeviceIdleController.this.m159lambda$onBootPhase$4$comandroidserverDeviceIdleController((PowerSaveState) obj);
                    }
                });
                updateQuickDozeFlagLocked(this.mLocalPowerManager.getLowPowerState(15).batterySaverEnabled);
                this.mLocalActivityTaskManager.registerScreenObserver(this.mScreenObserver);
                passWhiteListsToForceAppStandbyTrackerLocked();
                updateInteractivityLocked();
            }
            updateConnectivityState(null);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onBootPhase$4$com-android-server-DeviceIdleController  reason: not valid java name */
    public /* synthetic */ void m159lambda$onBootPhase$4$comandroidserverDeviceIdleController(PowerSaveState state) {
        synchronized (this) {
            updateQuickDozeFlagLocked(state.batterySaverEnabled);
        }
    }

    boolean hasMotionSensor() {
        return this.mUseMotionSensor && this.mMotionSensor != null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerDeviceIdleConstraintInternal(IDeviceIdleConstraint constraint, String name, int type) {
        int minState;
        switch (type) {
            case 0:
                minState = 0;
                break;
            case 1:
                minState = 3;
                break;
            default:
                Slog.wtf(TAG, "Registering device-idle constraint with invalid type: " + type);
                return;
        }
        synchronized (this) {
            if (this.mConstraints.containsKey(constraint)) {
                Slog.e(TAG, "Re-registering device-idle constraint: " + constraint + ".");
                return;
            }
            DeviceIdleConstraintTracker tracker = new DeviceIdleConstraintTracker(name, minState);
            this.mConstraints.put(constraint, tracker);
            updateActiveConstraintsLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterDeviceIdleConstraintInternal(IDeviceIdleConstraint constraint) {
        synchronized (this) {
            onConstraintStateChangedLocked(constraint, false);
            setConstraintMonitoringLocked(constraint, false);
            this.mConstraints.remove(constraint);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onConstraintStateChangedLocked(IDeviceIdleConstraint constraint, boolean active) {
        DeviceIdleConstraintTracker tracker = this.mConstraints.get(constraint);
        if (tracker == null) {
            Slog.e(TAG, "device-idle constraint " + constraint + " has not been registered.");
        } else if (active != tracker.active && tracker.monitoring) {
            tracker.active = active;
            int i = this.mNumBlockingConstraints + (tracker.active ? 1 : -1);
            this.mNumBlockingConstraints = i;
            if (i == 0) {
                if (this.mState == 0) {
                    becomeInactiveIfAppropriateLocked();
                    return;
                }
                long j = this.mNextAlarmTime;
                if (j == 0 || j < SystemClock.elapsedRealtime()) {
                    stepIdleStateLocked("s:" + tracker.name);
                }
            }
        }
    }

    private void setConstraintMonitoringLocked(IDeviceIdleConstraint constraint, boolean monitor) {
        DeviceIdleConstraintTracker tracker = this.mConstraints.get(constraint);
        if (tracker.monitoring != monitor) {
            tracker.monitoring = monitor;
            updateActiveConstraintsLocked();
            this.mHandler.obtainMessage(10, monitor ? 1 : 0, -1, constraint).sendToTarget();
        }
    }

    private void updateActiveConstraintsLocked() {
        this.mNumBlockingConstraints = 0;
        for (int i = 0; i < this.mConstraints.size(); i++) {
            IDeviceIdleConstraint constraint = this.mConstraints.keyAt(i);
            DeviceIdleConstraintTracker tracker = this.mConstraints.valueAt(i);
            boolean monitoring = tracker.minState == this.mState;
            if (monitoring != tracker.monitoring) {
                setConstraintMonitoringLocked(constraint, monitoring);
                tracker.active = monitoring;
            }
            if (tracker.monitoring && tracker.active) {
                this.mNumBlockingConstraints++;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int addPowerSaveWhitelistAppsInternal(List<String> pkgNames) {
        int numAdded = 0;
        int numErrors = 0;
        synchronized (this) {
            for (int i = pkgNames.size() - 1; i >= 0; i--) {
                String name = pkgNames.get(i);
                if (name == null) {
                    numErrors++;
                } else {
                    try {
                        ApplicationInfo ai = getContext().getPackageManager().getApplicationInfo(name, 4194304);
                        if (this.mPowerSaveWhitelistUserApps.put(name, Integer.valueOf(UserHandle.getAppId(ai.uid))) == null) {
                            numAdded++;
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        Slog.e(TAG, "Tried to add unknown package to power save whitelist: " + name);
                        numErrors++;
                    }
                }
            }
            if (numAdded > 0) {
                reportPowerSaveWhitelistChangedLocked();
                updateWhitelistAppIdsLocked();
                writeConfigFileLocked();
            }
        }
        return pkgNames.size() - numErrors;
    }

    public boolean removePowerSaveWhitelistAppInternal(String name) {
        synchronized (this) {
            if (this.mPowerSaveWhitelistUserApps.remove(name) != null) {
                reportPowerSaveWhitelistChangedLocked();
                updateWhitelistAppIdsLocked();
                writeConfigFileLocked();
                return true;
            }
            return false;
        }
    }

    public boolean getPowerSaveWhitelistAppInternal(String name) {
        boolean containsKey;
        synchronized (this) {
            containsKey = this.mPowerSaveWhitelistUserApps.containsKey(name);
        }
        return containsKey;
    }

    void resetSystemPowerWhitelistInternal() {
        synchronized (this) {
            this.mPowerSaveWhitelistApps.putAll((ArrayMap<? extends String, ? extends Integer>) this.mRemovedFromSystemWhitelistApps);
            this.mRemovedFromSystemWhitelistApps.clear();
            reportPowerSaveWhitelistChangedLocked();
            updateWhitelistAppIdsLocked();
            writeConfigFileLocked();
        }
    }

    public boolean restoreSystemPowerWhitelistAppInternal(String name) {
        synchronized (this) {
            if (!this.mRemovedFromSystemWhitelistApps.containsKey(name)) {
                return false;
            }
            this.mPowerSaveWhitelistApps.put(name, this.mRemovedFromSystemWhitelistApps.remove(name));
            reportPowerSaveWhitelistChangedLocked();
            updateWhitelistAppIdsLocked();
            writeConfigFileLocked();
            return true;
        }
    }

    public boolean removeSystemPowerWhitelistAppInternal(String name) {
        synchronized (this) {
            if (!this.mPowerSaveWhitelistApps.containsKey(name)) {
                return false;
            }
            this.mRemovedFromSystemWhitelistApps.put(name, this.mPowerSaveWhitelistApps.remove(name));
            reportPowerSaveWhitelistChangedLocked();
            updateWhitelistAppIdsLocked();
            writeConfigFileLocked();
            return true;
        }
    }

    public boolean addPowerSaveWhitelistExceptIdleInternal(String name) {
        synchronized (this) {
            try {
                try {
                    ApplicationInfo ai = getContext().getPackageManager().getApplicationInfo(name, 4194304);
                    if (this.mPowerSaveWhitelistAppsExceptIdle.put(name, Integer.valueOf(UserHandle.getAppId(ai.uid))) == null) {
                        this.mPowerSaveWhitelistUserAppsExceptIdle.add(name);
                        reportPowerSaveWhitelistChangedLocked();
                        this.mPowerSaveWhitelistExceptIdleAppIdArray = buildAppIdArray(this.mPowerSaveWhitelistAppsExceptIdle, this.mPowerSaveWhitelistUserApps, this.mPowerSaveWhitelistExceptIdleAppIds);
                        passWhiteListsToForceAppStandbyTrackerLocked();
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return true;
    }

    public void resetPowerSaveWhitelistExceptIdleInternal() {
        synchronized (this) {
            if (this.mPowerSaveWhitelistAppsExceptIdle.removeAll(this.mPowerSaveWhitelistUserAppsExceptIdle)) {
                reportPowerSaveWhitelistChangedLocked();
                this.mPowerSaveWhitelistExceptIdleAppIdArray = buildAppIdArray(this.mPowerSaveWhitelistAppsExceptIdle, this.mPowerSaveWhitelistUserApps, this.mPowerSaveWhitelistExceptIdleAppIds);
                this.mPowerSaveWhitelistUserAppsExceptIdle.clear();
                passWhiteListsToForceAppStandbyTrackerLocked();
            }
        }
    }

    public boolean getPowerSaveWhitelistExceptIdleInternal(String name) {
        boolean containsKey;
        synchronized (this) {
            containsKey = this.mPowerSaveWhitelistAppsExceptIdle.containsKey(name);
        }
        return containsKey;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String[] getSystemPowerWhitelistExceptIdleInternal(final int callingUid, final int callingUserId) {
        String[] apps;
        synchronized (this) {
            int size = this.mPowerSaveWhitelistAppsExceptIdle.size();
            apps = new String[size];
            for (int i = 0; i < size; i++) {
                apps[i] = this.mPowerSaveWhitelistAppsExceptIdle.keyAt(i);
            }
        }
        return (String[]) ArrayUtils.filter(apps, new IntFunction() { // from class: com.android.server.DeviceIdleController$$ExternalSyntheticLambda8
            @Override // java.util.function.IntFunction
            public final Object apply(int i2) {
                return DeviceIdleController.lambda$getSystemPowerWhitelistExceptIdleInternal$5(i2);
            }
        }, new Predicate() { // from class: com.android.server.DeviceIdleController$$ExternalSyntheticLambda9
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return DeviceIdleController.this.m152xa7daeaa8(callingUid, callingUserId, (String) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ String[] lambda$getSystemPowerWhitelistExceptIdleInternal$5(int x$0) {
        return new String[x$0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getSystemPowerWhitelistExceptIdleInternal$6$com-android-server-DeviceIdleController  reason: not valid java name */
    public /* synthetic */ boolean m152xa7daeaa8(int callingUid, int callingUserId, String pkg) {
        return !this.mPackageManagerInternal.filterAppAccess(pkg, callingUid, callingUserId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String[] getSystemPowerWhitelistInternal(final int callingUid, final int callingUserId) {
        String[] apps;
        synchronized (this) {
            int size = this.mPowerSaveWhitelistApps.size();
            apps = new String[size];
            for (int i = 0; i < size; i++) {
                apps[i] = this.mPowerSaveWhitelistApps.keyAt(i);
            }
        }
        return (String[]) ArrayUtils.filter(apps, new IntFunction() { // from class: com.android.server.DeviceIdleController$$ExternalSyntheticLambda0
            @Override // java.util.function.IntFunction
            public final Object apply(int i2) {
                return DeviceIdleController.lambda$getSystemPowerWhitelistInternal$7(i2);
            }
        }, new Predicate() { // from class: com.android.server.DeviceIdleController$$ExternalSyntheticLambda1
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return DeviceIdleController.this.m153x633761bd(callingUid, callingUserId, (String) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ String[] lambda$getSystemPowerWhitelistInternal$7(int x$0) {
        return new String[x$0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getSystemPowerWhitelistInternal$8$com-android-server-DeviceIdleController  reason: not valid java name */
    public /* synthetic */ boolean m153x633761bd(int callingUid, int callingUserId, String pkg) {
        return !this.mPackageManagerInternal.filterAppAccess(pkg, callingUid, callingUserId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String[] getRemovedSystemPowerWhitelistAppsInternal(final int callingUid, final int callingUserId) {
        String[] apps;
        synchronized (this) {
            int size = this.mRemovedFromSystemWhitelistApps.size();
            apps = new String[size];
            for (int i = 0; i < size; i++) {
                apps[i] = this.mRemovedFromSystemWhitelistApps.keyAt(i);
            }
        }
        return (String[]) ArrayUtils.filter(apps, new IntFunction() { // from class: com.android.server.DeviceIdleController$$ExternalSyntheticLambda10
            @Override // java.util.function.IntFunction
            public final Object apply(int i2) {
                return DeviceIdleController.lambda$getRemovedSystemPowerWhitelistAppsInternal$9(i2);
            }
        }, new Predicate() { // from class: com.android.server.DeviceIdleController$$ExternalSyntheticLambda11
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return DeviceIdleController.this.m151x3a97d7e(callingUid, callingUserId, (String) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ String[] lambda$getRemovedSystemPowerWhitelistAppsInternal$9(int x$0) {
        return new String[x$0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getRemovedSystemPowerWhitelistAppsInternal$10$com-android-server-DeviceIdleController  reason: not valid java name */
    public /* synthetic */ boolean m151x3a97d7e(int callingUid, int callingUserId, String pkg) {
        return !this.mPackageManagerInternal.filterAppAccess(pkg, callingUid, callingUserId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String[] getUserPowerWhitelistInternal(final int callingUid, final int callingUserId) {
        String[] apps;
        synchronized (this) {
            int size = this.mPowerSaveWhitelistUserApps.size();
            apps = new String[size];
            for (int i = 0; i < this.mPowerSaveWhitelistUserApps.size(); i++) {
                apps[i] = this.mPowerSaveWhitelistUserApps.keyAt(i);
            }
        }
        return (String[]) ArrayUtils.filter(apps, new IntFunction() { // from class: com.android.server.DeviceIdleController$$ExternalSyntheticLambda12
            @Override // java.util.function.IntFunction
            public final Object apply(int i2) {
                return DeviceIdleController.lambda$getUserPowerWhitelistInternal$11(i2);
            }
        }, new Predicate() { // from class: com.android.server.DeviceIdleController$$ExternalSyntheticLambda13
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return DeviceIdleController.this.m154x752926e4(callingUid, callingUserId, (String) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ String[] lambda$getUserPowerWhitelistInternal$11(int x$0) {
        return new String[x$0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getUserPowerWhitelistInternal$12$com-android-server-DeviceIdleController  reason: not valid java name */
    public /* synthetic */ boolean m154x752926e4(int callingUid, int callingUserId, String pkg) {
        return !this.mPackageManagerInternal.filterAppAccess(pkg, callingUid, callingUserId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String[] getFullPowerWhitelistExceptIdleInternal(final int callingUid, final int callingUserId) {
        String[] apps;
        synchronized (this) {
            int size = this.mPowerSaveWhitelistAppsExceptIdle.size() + this.mPowerSaveWhitelistUserApps.size();
            apps = new String[size];
            int cur = 0;
            for (int i = 0; i < this.mPowerSaveWhitelistAppsExceptIdle.size(); i++) {
                apps[cur] = this.mPowerSaveWhitelistAppsExceptIdle.keyAt(i);
                cur++;
            }
            for (int i2 = 0; i2 < this.mPowerSaveWhitelistUserApps.size(); i2++) {
                apps[cur] = this.mPowerSaveWhitelistUserApps.keyAt(i2);
                cur++;
            }
        }
        return (String[]) ArrayUtils.filter(apps, new IntFunction() { // from class: com.android.server.DeviceIdleController$$ExternalSyntheticLambda14
            @Override // java.util.function.IntFunction
            public final Object apply(int i3) {
                return DeviceIdleController.lambda$getFullPowerWhitelistExceptIdleInternal$13(i3);
            }
        }, new Predicate() { // from class: com.android.server.DeviceIdleController$$ExternalSyntheticLambda15
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return DeviceIdleController.this.m149xe3210555(callingUid, callingUserId, (String) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ String[] lambda$getFullPowerWhitelistExceptIdleInternal$13(int x$0) {
        return new String[x$0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getFullPowerWhitelistExceptIdleInternal$14$com-android-server-DeviceIdleController  reason: not valid java name */
    public /* synthetic */ boolean m149xe3210555(int callingUid, int callingUserId, String pkg) {
        return !this.mPackageManagerInternal.filterAppAccess(pkg, callingUid, callingUserId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String[] getFullPowerWhitelistInternal(final int callingUid, final int callingUserId) {
        String[] apps;
        synchronized (this) {
            int size = this.mPowerSaveWhitelistApps.size() + this.mPowerSaveWhitelistUserApps.size();
            apps = new String[size];
            int cur = 0;
            for (int i = 0; i < this.mPowerSaveWhitelistApps.size(); i++) {
                apps[cur] = this.mPowerSaveWhitelistApps.keyAt(i);
                cur++;
            }
            for (int i2 = 0; i2 < this.mPowerSaveWhitelistUserApps.size(); i2++) {
                apps[cur] = this.mPowerSaveWhitelistUserApps.keyAt(i2);
                cur++;
            }
        }
        return (String[]) ArrayUtils.filter(apps, new IntFunction() { // from class: com.android.server.DeviceIdleController$$ExternalSyntheticLambda6
            @Override // java.util.function.IntFunction
            public final Object apply(int i3) {
                return DeviceIdleController.lambda$getFullPowerWhitelistInternal$15(i3);
            }
        }, new Predicate() { // from class: com.android.server.DeviceIdleController$$ExternalSyntheticLambda7
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return DeviceIdleController.this.m150x41eb94a4(callingUid, callingUserId, (String) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ String[] lambda$getFullPowerWhitelistInternal$15(int x$0) {
        return new String[x$0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getFullPowerWhitelistInternal$16$com-android-server-DeviceIdleController  reason: not valid java name */
    public /* synthetic */ boolean m150x41eb94a4(int callingUid, int callingUserId, String pkg) {
        return !this.mPackageManagerInternal.filterAppAccess(pkg, callingUid, callingUserId);
    }

    public boolean isPowerSaveWhitelistExceptIdleAppInternal(String packageName) {
        boolean z;
        synchronized (this) {
            z = this.mPowerSaveWhitelistAppsExceptIdle.containsKey(packageName) || this.mPowerSaveWhitelistUserApps.containsKey(packageName);
        }
        return z;
    }

    public boolean isPowerSaveWhitelistAppInternal(String packageName) {
        boolean z;
        synchronized (this) {
            z = this.mPowerSaveWhitelistApps.containsKey(packageName) || this.mPowerSaveWhitelistUserApps.containsKey(packageName);
        }
        return z;
    }

    public int[] getAppIdWhitelistExceptIdleInternal() {
        int[] iArr;
        synchronized (this) {
            iArr = this.mPowerSaveWhitelistExceptIdleAppIdArray;
        }
        return iArr;
    }

    public int[] getAppIdWhitelistInternal() {
        int[] iArr;
        synchronized (this) {
            iArr = this.mPowerSaveWhitelistAllAppIdArray;
        }
        return iArr;
    }

    public int[] getAppIdUserWhitelistInternal() {
        int[] iArr;
        synchronized (this) {
            iArr = this.mPowerSaveWhitelistUserAppIdArray;
        }
        return iArr;
    }

    public int[] getAppIdTempWhitelistInternal() {
        int[] iArr;
        synchronized (this) {
            iArr = this.mTempWhitelistAppIdArray;
        }
        return iArr;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getTempAllowListType(int reasonCode, int defaultType) {
        switch (reasonCode) {
            case -1:
                return -1;
            case 102:
                return this.mLocalActivityManager.getPushMessagingOverQuotaBehavior();
            default:
                return defaultType;
        }
    }

    void addPowerSaveTempAllowlistAppChecked(String packageName, long duration, int userId, int reasonCode, String reason) throws RemoteException {
        getContext().enforceCallingOrSelfPermission("android.permission.CHANGE_DEVICE_IDLE_TEMP_WHITELIST", "No permission to change device idle whitelist");
        int callingUid = Binder.getCallingUid();
        int userId2 = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), callingUid, userId, false, false, "addPowerSaveTempWhitelistApp", (String) null);
        long token = Binder.clearCallingIdentity();
        try {
            int type = getTempAllowListType(reasonCode, 0);
            if (type != -1) {
                addPowerSaveTempAllowlistAppInternal(callingUid, packageName, duration, type, userId2, true, reasonCode, reason);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    void removePowerSaveTempAllowlistAppChecked(String packageName, int userId) throws RemoteException {
        getContext().enforceCallingOrSelfPermission("android.permission.CHANGE_DEVICE_IDLE_TEMP_WHITELIST", "No permission to change device idle whitelist");
        int callingUid = Binder.getCallingUid();
        int userId2 = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), callingUid, userId, false, false, "removePowerSaveTempWhitelistApp", (String) null);
        long token = Binder.clearCallingIdentity();
        try {
            removePowerSaveTempAllowlistAppInternal(packageName, userId2);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    void addPowerSaveTempAllowlistAppInternal(int callingUid, String packageName, long durationMs, int tempAllowListType, int userId, boolean sync, int reasonCode, String reason) {
        try {
            try {
                int uid = getContext().getPackageManager().getPackageUidAsUser(packageName, userId);
                addPowerSaveTempWhitelistAppDirectInternal(callingUid, uid, durationMs, tempAllowListType, sync, reasonCode, reason);
            } catch (PackageManager.NameNotFoundException e) {
            }
        } catch (PackageManager.NameNotFoundException e2) {
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2959=4] */
    void addPowerSaveTempWhitelistAppDirectInternal(int callingUid, int uid, long duration, int tempAllowListType, boolean sync, int reasonCode, String reason) {
        Pair<MutableLong, String> entry;
        boolean z;
        int appId;
        String str;
        int i;
        long timeNow = SystemClock.elapsedRealtime();
        boolean informWhitelistChanged = false;
        int appId2 = UserHandle.getAppId(uid);
        synchronized (this) {
            try {
                try {
                    try {
                        long duration2 = Math.min(duration, this.mConstants.MAX_TEMP_APP_ALLOWLIST_DURATION_MS);
                        try {
                            Pair<MutableLong, String> entry2 = this.mTempWhitelistAppIdEndTimes.get(appId2);
                            boolean newEntry = entry2 == null;
                            if (newEntry) {
                                try {
                                    Pair<MutableLong, String> entry3 = new Pair<>(new MutableLong(0L), reason);
                                    this.mTempWhitelistAppIdEndTimes.put(appId2, entry3);
                                    entry = entry3;
                                } catch (Throwable th) {
                                    th = th;
                                    throw th;
                                }
                            } else {
                                entry = entry2;
                            }
                            ((MutableLong) entry.first).value = timeNow + duration2;
                            if (newEntry) {
                                try {
                                    this.mBatteryStats.noteEvent(32785, reason, uid);
                                } catch (RemoteException e) {
                                }
                                try {
                                    postTempActiveTimeoutMessage(uid, duration2);
                                    try {
                                        updateTempWhitelistAppIdsLocked(uid, true, duration2, tempAllowListType, reasonCode, reason, callingUid);
                                        if (sync) {
                                            informWhitelistChanged = true;
                                        } else {
                                            this.mHandler.obtainMessage(14, appId2, reasonCode, reason).sendToTarget();
                                        }
                                        reportTempWhitelistChangedLocked(uid, true);
                                        z = true;
                                        appId = appId2;
                                        str = reason;
                                        i = reasonCode;
                                    } catch (Throwable th2) {
                                        th = th2;
                                        throw th;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                            } else {
                                try {
                                    ActivityManagerInternal activityManagerInternal = this.mLocalActivityManager;
                                    if (activityManagerInternal != null) {
                                        z = true;
                                        appId = appId2;
                                        str = reason;
                                        i = reasonCode;
                                        activityManagerInternal.updateDeviceIdleTempAllowlist((int[]) null, uid, true, duration2, tempAllowListType, reasonCode, reason, callingUid);
                                    } else {
                                        z = true;
                                        appId = appId2;
                                        str = reason;
                                        i = reasonCode;
                                    }
                                } catch (Throwable th4) {
                                    th = th4;
                                    throw th;
                                }
                            }
                            if (informWhitelistChanged) {
                                this.mNetworkPolicyManagerInternal.onTempPowerSaveWhitelistChange(appId, z, i, str);
                            }
                        } catch (Throwable th5) {
                            th = th5;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                    }
                } catch (Throwable th7) {
                    th = th7;
                }
            } catch (Throwable th8) {
                th = th8;
            }
        }
    }

    private void removePowerSaveTempAllowlistAppInternal(String packageName, int userId) {
        try {
            int uid = getContext().getPackageManager().getPackageUidAsUser(packageName, userId);
            removePowerSaveTempWhitelistAppDirectInternal(uid);
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    private void removePowerSaveTempWhitelistAppDirectInternal(int uid) {
        int appId = UserHandle.getAppId(uid);
        synchronized (this) {
            int idx = this.mTempWhitelistAppIdEndTimes.indexOfKey(appId);
            if (idx < 0) {
                return;
            }
            String reason = (String) this.mTempWhitelistAppIdEndTimes.valueAt(idx).second;
            this.mTempWhitelistAppIdEndTimes.removeAt(idx);
            onAppRemovedFromTempWhitelistLocked(uid, reason);
        }
    }

    private void postTempActiveTimeoutMessage(int uid, long delay) {
        MyHandler myHandler = this.mHandler;
        myHandler.sendMessageDelayed(myHandler.obtainMessage(6, uid, 0), delay);
    }

    void checkTempAppWhitelistTimeout(int uid) {
        long timeNow = SystemClock.elapsedRealtime();
        int appId = UserHandle.getAppId(uid);
        synchronized (this) {
            Pair<MutableLong, String> entry = this.mTempWhitelistAppIdEndTimes.get(appId);
            if (entry == null) {
                return;
            }
            if (timeNow >= ((MutableLong) entry.first).value) {
                this.mTempWhitelistAppIdEndTimes.delete(appId);
                onAppRemovedFromTempWhitelistLocked(uid, (String) entry.second);
            } else {
                postTempActiveTimeoutMessage(uid, ((MutableLong) entry.first).value - timeNow);
            }
        }
    }

    private void onAppRemovedFromTempWhitelistLocked(int uid, String reason) {
        int appId = UserHandle.getAppId(uid);
        updateTempWhitelistAppIdsLocked(uid, false, 0L, 0, 0, reason, -1);
        this.mHandler.obtainMessage(15, appId, 0).sendToTarget();
        reportTempWhitelistChangedLocked(uid, false);
        try {
            this.mBatteryStats.noteEvent(16401, reason, appId);
        } catch (RemoteException e) {
        }
    }

    public void exitIdleInternal(String reason) {
        synchronized (this) {
            this.mActiveReason = 5;
            becomeActiveLocked(reason, Binder.getCallingUid());
        }
    }

    boolean isNetworkConnected() {
        boolean z;
        synchronized (this) {
            z = this.mNetworkConnected;
        }
        return z;
    }

    void updateConnectivityState(Intent connIntent) {
        ConnectivityManager cm;
        boolean conn;
        synchronized (this) {
            cm = this.mInjector.getConnectivityManager();
        }
        if (cm == null) {
            return;
        }
        NetworkInfo ni = cm.getActiveNetworkInfo();
        synchronized (this) {
            if (ni == null) {
                conn = false;
            } else if (connIntent == null) {
                conn = ni.isConnected();
            } else {
                int networkType = connIntent.getIntExtra("networkType", -1);
                if (ni.getType() != networkType) {
                    return;
                }
                conn = !connIntent.getBooleanExtra("noConnectivity", false);
            }
            if (conn != this.mNetworkConnected) {
                this.mNetworkConnected = conn;
                if (conn && this.mLightState == 5) {
                    stepLightIdleStateLocked("network", true);
                }
            }
        }
    }

    boolean isScreenOn() {
        boolean z;
        synchronized (this) {
            z = this.mScreenOn;
        }
        return z;
    }

    void updateInteractivityLocked() {
        boolean screenOn = this.mPowerManager.isInteractive();
        if (!screenOn && this.mScreenOn) {
            this.mScreenOn = false;
            if (!this.mForceIdle) {
                becomeInactiveIfAppropriateLocked();
            }
        } else if (screenOn) {
            this.mScreenOn = true;
            if (this.mForceIdle) {
                return;
            }
            if (!this.mScreenLocked || !this.mConstants.WAIT_FOR_UNLOCK) {
                this.mActiveReason = 2;
                becomeActiveLocked("screen", Process.myUid());
            }
        }
    }

    boolean isCharging() {
        boolean z;
        synchronized (this) {
            z = this.mCharging;
        }
        return z;
    }

    void updateChargingLocked(boolean charging) {
        if (!charging && this.mCharging) {
            this.mCharging = false;
            if (!this.mForceIdle) {
                becomeInactiveIfAppropriateLocked();
            }
        } else if (charging) {
            this.mCharging = charging;
            if (!this.mForceIdle) {
                this.mActiveReason = 3;
                becomeActiveLocked("charging", Process.myUid());
            }
        }
    }

    boolean isQuickDozeEnabled() {
        boolean z;
        synchronized (this) {
            z = this.mQuickDozeActivated;
        }
        return z;
    }

    void updateQuickDozeFlagLocked(boolean enabled) {
        int i;
        this.mQuickDozeActivated = enabled;
        this.mQuickDozeActivatedWhileIdling = enabled && ((i = this.mState) == 5 || i == 6);
        if (enabled) {
            becomeInactiveIfAppropriateLocked();
        }
    }

    boolean isKeyguardShowing() {
        boolean z;
        synchronized (this) {
            z = this.mScreenLocked;
        }
        return z;
    }

    void keyguardShowingLocked(boolean showing) {
        if (this.mScreenLocked != showing) {
            this.mScreenLocked = showing;
            if (this.mScreenOn && !this.mForceIdle && !showing) {
                this.mActiveReason = 4;
                becomeActiveLocked("unlocked", Process.myUid());
            }
        }
    }

    void scheduleReportActiveLocked(String activeReason, int activeUid) {
        Message msg = this.mHandler.obtainMessage(5, activeUid, 0, activeReason);
        this.mHandler.sendMessage(msg);
    }

    void becomeActiveLocked(String activeReason, int activeUid) {
        becomeActiveLocked(activeReason, activeUid, this.mConstants.INACTIVE_TIMEOUT, true);
    }

    private void becomeActiveLocked(String activeReason, int activeUid, long newInactiveTimeout, boolean changeLightIdle) {
        if (this.mState != 0 || this.mLightState != 0) {
            EventLogTags.writeDeviceIdle(0, activeReason);
            this.mState = 0;
            this.mInactiveTimeout = newInactiveTimeout;
            resetIdleManagementLocked();
            if (this.mLightState != 6) {
                this.mMaintenanceStartTime = 0L;
            }
            if (changeLightIdle) {
                EventLogTags.writeDeviceIdleLight(0, activeReason);
                this.mLightState = 0;
                resetLightIdleManagementLocked();
                scheduleReportActiveLocked(activeReason, activeUid);
                addEvent(1, activeReason);
            }
        }
    }

    void setDeepEnabledForTest(boolean enabled) {
        synchronized (this) {
            this.mDeepEnabled = enabled;
        }
    }

    void setLightEnabledForTest(boolean enabled) {
        synchronized (this) {
            this.mLightEnabled = enabled;
        }
    }

    private void verifyAlarmStateLocked() {
        if (this.mState == 0 && this.mNextAlarmTime != 0) {
            Slog.wtf(TAG, "mState=ACTIVE but mNextAlarmTime=" + this.mNextAlarmTime);
        }
        if (this.mState != 5 && this.mLocalAlarmManager.isIdling()) {
            Slog.wtf(TAG, "mState=" + stateToString(this.mState) + " but AlarmManager is idling");
        }
        if (this.mState == 5 && !this.mLocalAlarmManager.isIdling()) {
            Slog.wtf(TAG, "mState=IDLE but AlarmManager is not idling");
        }
        if (this.mLightState == 0 && this.mNextLightAlarmTime != 0) {
            Slog.wtf(TAG, "mLightState=ACTIVE but mNextLightAlarmTime is " + TimeUtils.formatDuration(this.mNextLightAlarmTime - SystemClock.elapsedRealtime()) + " from now");
        }
    }

    void becomeInactiveIfAppropriateLocked() {
        verifyAlarmStateLocked();
        boolean isScreenBlockingInactive = this.mScreenOn && !(this.mConstants.WAIT_FOR_UNLOCK && this.mScreenLocked);
        if (!this.mForceIdle && (this.mCharging || isScreenBlockingInactive)) {
            return;
        }
        if (this.mDeepEnabled) {
            if (this.mQuickDozeActivated) {
                int i = this.mState;
                if (i == 7 || i == 5 || i == 6) {
                    return;
                }
                this.mState = 7;
                resetIdleManagementLocked();
                if (isUpcomingAlarmClock()) {
                    scheduleAlarmLocked((this.mAlarmManager.getNextWakeFromIdleTime() - this.mInjector.getElapsedRealtime()) + this.mConstants.QUICK_DOZE_DELAY_TIMEOUT, false);
                } else {
                    scheduleAlarmLocked(this.mConstants.QUICK_DOZE_DELAY_TIMEOUT, false);
                }
                EventLogTags.writeDeviceIdle(this.mState, "no activity");
            } else if (this.mState == 0) {
                this.mState = 1;
                resetIdleManagementLocked();
                long delay = this.mInactiveTimeout;
                if (shouldUseIdleTimeoutFactorLocked()) {
                    delay = this.mPreIdleFactor * ((float) delay);
                }
                if (isUpcomingAlarmClock()) {
                    scheduleAlarmLocked((this.mAlarmManager.getNextWakeFromIdleTime() - this.mInjector.getElapsedRealtime()) + delay, false);
                } else {
                    scheduleAlarmLocked(delay, false);
                }
                EventLogTags.writeDeviceIdle(this.mState, "no activity");
            }
        }
        if (this.mLightState == 0 && this.mLightEnabled) {
            this.mLightState = 1;
            resetLightIdleManagementLocked();
            scheduleLightAlarmLocked(this.mConstants.LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT, this.mConstants.FLEX_TIME_SHORT);
            scheduleLightMaintenanceAlarmLocked(this.mConstants.LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT + this.mConstants.LIGHT_IDLE_TIMEOUT);
            EventLogTags.writeDeviceIdleLight(this.mLightState, "no activity");
        }
    }

    private void resetIdleManagementLocked() {
        this.mNextIdlePendingDelay = 0L;
        this.mNextIdleDelay = 0L;
        this.mIdleStartTime = 0L;
        this.mQuickDozeActivatedWhileIdling = false;
        cancelAlarmLocked();
        cancelSensingTimeoutAlarmLocked();
        cancelLocatingLocked();
        maybeStopMonitoringMotionLocked();
        this.mAnyMotionDetector.stop();
        updateActiveConstraintsLocked();
    }

    private void resetLightIdleManagementLocked() {
        this.mNextLightIdleDelay = this.mConstants.LIGHT_IDLE_TIMEOUT;
        this.mMaintenanceStartTime = 0L;
        this.mCurLightIdleBudget = this.mConstants.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET;
        cancelAllLightAlarmsLocked();
    }

    void exitForceIdleLocked() {
        if (this.mForceIdle) {
            this.mForceIdle = false;
            if (this.mScreenOn || this.mCharging) {
                this.mActiveReason = 6;
                becomeActiveLocked("exit-force", Process.myUid());
            }
        }
    }

    void setLightStateForTest(int lightState) {
        synchronized (this) {
            this.mLightState = lightState;
        }
    }

    int getLightState() {
        int i;
        synchronized (this) {
            i = this.mLightState;
        }
        return i;
    }

    private void stepLightIdleStateLocked(String reason) {
        stepLightIdleStateLocked(reason, false);
    }

    void stepLightIdleStateLocked(String reason, boolean forceProgression) {
        boolean enterMaintenance;
        int i = this.mLightState;
        if (i == 0 || i == 7) {
            return;
        }
        EventLogTags.writeDeviceIdleLightStep();
        long nowElapsed = this.mInjector.getElapsedRealtime();
        long j = this.mNextLightMaintenanceAlarmTime;
        boolean crossedMaintenanceTime = j > 0 && nowElapsed >= j;
        long j2 = this.mNextLightAlarmTime;
        boolean crossedProgressionTime = j2 > 0 && nowElapsed >= j2;
        if (crossedMaintenanceTime) {
            if (crossedProgressionTime) {
                enterMaintenance = j2 <= j;
            } else {
                enterMaintenance = true;
            }
        } else if (crossedProgressionTime) {
            enterMaintenance = false;
        } else if (!forceProgression) {
            Slog.wtfStack(TAG, "stepLightIdleStateLocked called in invalid state: " + this.mLightState);
            return;
        } else {
            int i2 = this.mLightState;
            if (i2 == 4 || i2 == 5) {
                r9 = true;
            }
            enterMaintenance = r9;
        }
        if (enterMaintenance) {
            if (!this.mNetworkConnected && this.mLightState != 5) {
                scheduleLightMaintenanceAlarmLocked(this.mNextLightIdleDelay);
                cancelLightAlarmLocked();
                this.mLightState = 5;
                EventLogTags.writeDeviceIdleLight(5, reason);
                return;
            }
            this.mActiveIdleOpCount = 1;
            this.mActiveIdleWakeLock.acquire();
            this.mMaintenanceStartTime = SystemClock.elapsedRealtime();
            if (this.mCurLightIdleBudget < this.mConstants.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET) {
                this.mCurLightIdleBudget = this.mConstants.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET;
            } else if (this.mCurLightIdleBudget > this.mConstants.LIGHT_IDLE_MAINTENANCE_MAX_BUDGET) {
                this.mCurLightIdleBudget = this.mConstants.LIGHT_IDLE_MAINTENANCE_MAX_BUDGET;
            }
            this.mNextLightIdleDelay = Math.min(this.mConstants.LIGHT_MAX_IDLE_TIMEOUT, ((float) this.mNextLightIdleDelay) * this.mConstants.LIGHT_IDLE_FACTOR);
            scheduleLightAlarmLocked(this.mCurLightIdleBudget, this.mConstants.FLEX_TIME_SHORT);
            scheduleLightMaintenanceAlarmLocked(this.mCurLightIdleBudget + this.mNextLightIdleDelay);
            this.mLightState = 6;
            EventLogTags.writeDeviceIdleLight(6, reason);
            addEvent(3, null);
            this.mHandler.sendEmptyMessage(4);
            return;
        }
        if (this.mMaintenanceStartTime != 0) {
            long duration = Math.min(this.mCurLightIdleBudget, SystemClock.elapsedRealtime() - this.mMaintenanceStartTime);
            if (duration < this.mConstants.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET) {
                this.mCurLightIdleBudget += this.mConstants.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET - duration;
            } else {
                this.mCurLightIdleBudget -= duration - this.mConstants.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET;
            }
        }
        this.mMaintenanceStartTime = 0L;
        scheduleLightMaintenanceAlarmLocked(this.mNextLightIdleDelay);
        cancelLightAlarmLocked();
        this.mLightState = 4;
        EventLogTags.writeDeviceIdleLight(4, reason);
        addEvent(2, null);
        this.mGoingIdleWakeLock.acquire();
        this.mHandler.sendEmptyMessage(3);
    }

    int getState() {
        int i;
        synchronized (this) {
            i = this.mState;
        }
        return i;
    }

    private boolean isUpcomingAlarmClock() {
        return this.mInjector.getElapsedRealtime() + this.mConstants.MIN_TIME_TO_ALARM >= this.mAlarmManager.getNextWakeFromIdleTime();
    }

    /* JADX WARN: Removed duplicated region for block: B:47:0x014b  */
    /* JADX WARN: Removed duplicated region for block: B:50:0x0158  */
    /* JADX WARN: Removed duplicated region for block: B:63:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    void stepIdleStateLocked(String reason) {
        long min;
        LocationManager locationManager;
        EventLogTags.writeDeviceIdleStep();
        if (isUpcomingAlarmClock()) {
            if (this.mState != 0) {
                this.mActiveReason = 7;
                becomeActiveLocked("alarm", Process.myUid());
                becomeInactiveIfAppropriateLocked();
            }
        } else if (this.mNumBlockingConstraints != 0 && !this.mForceIdle) {
        } else {
            switch (this.mState) {
                case 1:
                    startMonitoringMotionLocked();
                    long delay = this.mConstants.IDLE_AFTER_INACTIVE_TIMEOUT;
                    if (shouldUseIdleTimeoutFactorLocked()) {
                        delay = this.mPreIdleFactor * ((float) delay);
                    }
                    scheduleAlarmLocked(delay, false);
                    moveToStateLocked(2, reason);
                    return;
                case 2:
                    moveToStateLocked(3, reason);
                    cancelLocatingLocked();
                    this.mLocated = false;
                    this.mLastGenericLocation = null;
                    this.mLastGpsLocation = null;
                    updateActiveConstraintsLocked();
                    if (this.mUseMotionSensor && this.mAnyMotionDetector.hasSensor()) {
                        scheduleSensingTimeoutAlarmLocked(this.mConstants.SENSING_TIMEOUT);
                        this.mNotMoving = false;
                        this.mAnyMotionDetector.checkForAnyMotion();
                        return;
                    } else if (this.mNumBlockingConstraints != 0) {
                        cancelAlarmLocked();
                        return;
                    } else {
                        this.mNotMoving = true;
                        cancelSensingTimeoutAlarmLocked();
                        moveToStateLocked(4, reason);
                        scheduleAlarmLocked(this.mConstants.LOCATING_TIMEOUT, false);
                        locationManager = this.mInjector.getLocationManager();
                        if (locationManager == null && locationManager.getProvider("network") != null) {
                            locationManager.requestLocationUpdates(this.mLocationRequest, this.mGenericLocationListener, this.mHandler.getLooper());
                            this.mLocating = true;
                        } else {
                            this.mHasNetworkLocation = false;
                        }
                        if (locationManager == null && locationManager.getProvider("gps") != null) {
                            this.mHasGps = true;
                            locationManager.requestLocationUpdates("gps", 1000L, 5.0f, this.mGpsLocationListener, this.mHandler.getLooper());
                            this.mLocating = true;
                        } else {
                            this.mHasGps = false;
                        }
                        if (this.mLocating) {
                            return;
                        }
                        cancelAlarmLocked();
                        cancelLocatingLocked();
                        this.mAnyMotionDetector.stop();
                        this.mNextIdlePendingDelay = this.mConstants.IDLE_PENDING_TIMEOUT;
                        this.mNextIdleDelay = this.mConstants.IDLE_TIMEOUT;
                        scheduleAlarmLocked(this.mNextIdleDelay, true);
                        this.mNextIdleDelay = ((float) this.mNextIdleDelay) * this.mConstants.IDLE_FACTOR;
                        this.mIdleStartTime = SystemClock.elapsedRealtime();
                        min = Math.min(this.mNextIdleDelay, this.mConstants.MAX_IDLE_TIMEOUT);
                        this.mNextIdleDelay = min;
                        if (min < this.mConstants.IDLE_TIMEOUT) {
                            this.mNextIdleDelay = this.mConstants.IDLE_TIMEOUT;
                        }
                        moveToStateLocked(5, reason);
                        if (this.mLightState != 7) {
                            this.mLightState = 7;
                            cancelAllLightAlarmsLocked();
                        }
                        addEvent(4, null);
                        this.mGoingIdleWakeLock.acquire();
                        this.mHandler.sendEmptyMessage(2);
                        return;
                    }
                case 3:
                    cancelSensingTimeoutAlarmLocked();
                    moveToStateLocked(4, reason);
                    scheduleAlarmLocked(this.mConstants.LOCATING_TIMEOUT, false);
                    locationManager = this.mInjector.getLocationManager();
                    if (locationManager == null) {
                        break;
                    }
                    this.mHasNetworkLocation = false;
                    if (locationManager == null) {
                        break;
                    }
                    this.mHasGps = false;
                    if (this.mLocating) {
                    }
                    cancelAlarmLocked();
                    cancelLocatingLocked();
                    this.mAnyMotionDetector.stop();
                    this.mNextIdlePendingDelay = this.mConstants.IDLE_PENDING_TIMEOUT;
                    this.mNextIdleDelay = this.mConstants.IDLE_TIMEOUT;
                    scheduleAlarmLocked(this.mNextIdleDelay, true);
                    this.mNextIdleDelay = ((float) this.mNextIdleDelay) * this.mConstants.IDLE_FACTOR;
                    this.mIdleStartTime = SystemClock.elapsedRealtime();
                    min = Math.min(this.mNextIdleDelay, this.mConstants.MAX_IDLE_TIMEOUT);
                    this.mNextIdleDelay = min;
                    if (min < this.mConstants.IDLE_TIMEOUT) {
                    }
                    moveToStateLocked(5, reason);
                    if (this.mLightState != 7) {
                    }
                    addEvent(4, null);
                    this.mGoingIdleWakeLock.acquire();
                    this.mHandler.sendEmptyMessage(2);
                    return;
                case 4:
                    cancelAlarmLocked();
                    cancelLocatingLocked();
                    this.mAnyMotionDetector.stop();
                    this.mNextIdlePendingDelay = this.mConstants.IDLE_PENDING_TIMEOUT;
                    this.mNextIdleDelay = this.mConstants.IDLE_TIMEOUT;
                    scheduleAlarmLocked(this.mNextIdleDelay, true);
                    this.mNextIdleDelay = ((float) this.mNextIdleDelay) * this.mConstants.IDLE_FACTOR;
                    this.mIdleStartTime = SystemClock.elapsedRealtime();
                    min = Math.min(this.mNextIdleDelay, this.mConstants.MAX_IDLE_TIMEOUT);
                    this.mNextIdleDelay = min;
                    if (min < this.mConstants.IDLE_TIMEOUT) {
                    }
                    moveToStateLocked(5, reason);
                    if (this.mLightState != 7) {
                    }
                    addEvent(4, null);
                    this.mGoingIdleWakeLock.acquire();
                    this.mHandler.sendEmptyMessage(2);
                    return;
                case 5:
                    this.mActiveIdleOpCount = 1;
                    this.mActiveIdleWakeLock.acquire();
                    scheduleAlarmLocked(this.mNextIdlePendingDelay, false);
                    this.mMaintenanceStartTime = SystemClock.elapsedRealtime();
                    long min2 = Math.min(this.mConstants.MAX_IDLE_PENDING_TIMEOUT, ((float) this.mNextIdlePendingDelay) * this.mConstants.IDLE_PENDING_FACTOR);
                    this.mNextIdlePendingDelay = min2;
                    if (min2 < this.mConstants.IDLE_PENDING_TIMEOUT) {
                        this.mNextIdlePendingDelay = this.mConstants.IDLE_PENDING_TIMEOUT;
                    }
                    moveToStateLocked(6, reason);
                    addEvent(5, null);
                    this.mHandler.sendEmptyMessage(4);
                    return;
                case 6:
                    scheduleAlarmLocked(this.mNextIdleDelay, true);
                    this.mNextIdleDelay = ((float) this.mNextIdleDelay) * this.mConstants.IDLE_FACTOR;
                    this.mIdleStartTime = SystemClock.elapsedRealtime();
                    min = Math.min(this.mNextIdleDelay, this.mConstants.MAX_IDLE_TIMEOUT);
                    this.mNextIdleDelay = min;
                    if (min < this.mConstants.IDLE_TIMEOUT) {
                    }
                    moveToStateLocked(5, reason);
                    if (this.mLightState != 7) {
                    }
                    addEvent(4, null);
                    this.mGoingIdleWakeLock.acquire();
                    this.mHandler.sendEmptyMessage(2);
                    return;
                case 7:
                    this.mNextIdlePendingDelay = this.mConstants.IDLE_PENDING_TIMEOUT;
                    this.mNextIdleDelay = this.mConstants.IDLE_TIMEOUT;
                    scheduleAlarmLocked(this.mNextIdleDelay, true);
                    this.mNextIdleDelay = ((float) this.mNextIdleDelay) * this.mConstants.IDLE_FACTOR;
                    this.mIdleStartTime = SystemClock.elapsedRealtime();
                    min = Math.min(this.mNextIdleDelay, this.mConstants.MAX_IDLE_TIMEOUT);
                    this.mNextIdleDelay = min;
                    if (min < this.mConstants.IDLE_TIMEOUT) {
                    }
                    moveToStateLocked(5, reason);
                    if (this.mLightState != 7) {
                    }
                    addEvent(4, null);
                    this.mGoingIdleWakeLock.acquire();
                    this.mHandler.sendEmptyMessage(2);
                    return;
                default:
                    return;
            }
        }
    }

    private void moveToStateLocked(int state, String reason) {
        int i = this.mState;
        this.mState = state;
        EventLogTags.writeDeviceIdle(state, reason);
        updateActiveConstraintsLocked();
    }

    void incActiveIdleOps() {
        synchronized (this) {
            this.mActiveIdleOpCount++;
        }
    }

    void decActiveIdleOps() {
        synchronized (this) {
            int i = this.mActiveIdleOpCount - 1;
            this.mActiveIdleOpCount = i;
            if (i <= 0) {
                exitMaintenanceEarlyIfNeededLocked();
                this.mActiveIdleWakeLock.release();
            }
        }
    }

    void setActiveIdleOpsForTest(int count) {
        synchronized (this) {
            this.mActiveIdleOpCount = count;
        }
    }

    void setJobsActive(boolean active) {
        synchronized (this) {
            this.mJobsActive = active;
            if (!active) {
                exitMaintenanceEarlyIfNeededLocked();
            }
        }
    }

    void setAlarmsActive(boolean active) {
        synchronized (this) {
            this.mAlarmsActive = active;
            if (!active) {
                exitMaintenanceEarlyIfNeededLocked();
            }
        }
    }

    int setPreIdleTimeoutMode(int mode) {
        return setPreIdleTimeoutFactor(getPreIdleTimeoutByMode(mode));
    }

    float getPreIdleTimeoutByMode(int mode) {
        switch (mode) {
            case 0:
                return 1.0f;
            case 1:
                return this.mConstants.PRE_IDLE_FACTOR_LONG;
            case 2:
                return this.mConstants.PRE_IDLE_FACTOR_SHORT;
            default:
                Slog.w(TAG, "Invalid time out factor mode: " + mode);
                return 1.0f;
        }
    }

    float getPreIdleTimeoutFactor() {
        float f;
        synchronized (this) {
            f = this.mPreIdleFactor;
        }
        return f;
    }

    int setPreIdleTimeoutFactor(float ratio) {
        synchronized (this) {
            if (!this.mDeepEnabled) {
                return 2;
            }
            if (ratio > MIN_PRE_IDLE_FACTOR_CHANGE) {
                if (Math.abs(ratio - this.mPreIdleFactor) < MIN_PRE_IDLE_FACTOR_CHANGE) {
                    return 0;
                }
                this.mLastPreIdleFactor = this.mPreIdleFactor;
                this.mPreIdleFactor = ratio;
                postUpdatePreIdleFactor();
                return 1;
            }
            return 3;
        }
    }

    void resetPreIdleTimeoutMode() {
        synchronized (this) {
            this.mLastPreIdleFactor = this.mPreIdleFactor;
            this.mPreIdleFactor = 1.0f;
        }
        postResetPreIdleTimeoutFactor();
    }

    private void postUpdatePreIdleFactor() {
        this.mHandler.sendEmptyMessage(11);
    }

    private void postResetPreIdleTimeoutFactor() {
        this.mHandler.sendEmptyMessage(12);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePreIdleFactor() {
        synchronized (this) {
            if (shouldUseIdleTimeoutFactorLocked()) {
                int i = this.mState;
                if (i == 1 || i == 2) {
                    long j = this.mNextAlarmTime;
                    if (j == 0) {
                        return;
                    }
                    long delay = j - SystemClock.elapsedRealtime();
                    if (delay < 60000) {
                        return;
                    }
                    long newDelay = (((float) delay) / this.mLastPreIdleFactor) * this.mPreIdleFactor;
                    if (Math.abs(delay - newDelay) < 60000) {
                        return;
                    }
                    scheduleAlarmLocked(newDelay, false);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeDoImmediateMaintenance() {
        synchronized (this) {
            if (this.mState == 5) {
                long duration = SystemClock.elapsedRealtime() - this.mIdleStartTime;
                if (duration > this.mConstants.IDLE_TIMEOUT) {
                    scheduleAlarmLocked(0L, false);
                }
            }
        }
    }

    private boolean shouldUseIdleTimeoutFactorLocked() {
        return this.mActiveReason != 1;
    }

    void setIdleStartTimeForTest(long idleStartTime) {
        synchronized (this) {
            this.mIdleStartTime = idleStartTime;
            maybeDoImmediateMaintenance();
        }
    }

    long getNextAlarmTime() {
        long j;
        synchronized (this) {
            j = this.mNextAlarmTime;
        }
        return j;
    }

    boolean isOpsInactiveLocked() {
        return (this.mActiveIdleOpCount > 0 || this.mJobsActive || this.mAlarmsActive) ? false : true;
    }

    void exitMaintenanceEarlyIfNeededLocked() {
        if ((this.mState == 6 || this.mLightState == 6) && isOpsInactiveLocked()) {
            SystemClock.elapsedRealtime();
            if (this.mState == 6) {
                stepIdleStateLocked("s:early");
            } else {
                stepLightIdleStateLocked("s:early", true);
            }
        }
    }

    void motionLocked() {
        this.mLastMotionEventElapsed = this.mInjector.getElapsedRealtime();
        handleMotionDetectedLocked(this.mConstants.MOTION_INACTIVE_TIMEOUT, "motion");
    }

    void handleMotionDetectedLocked(long timeout, String type) {
        if (this.mStationaryListeners.size() > 0) {
            postStationaryStatusUpdated();
            cancelMotionTimeoutAlarmLocked();
            scheduleMotionRegistrationAlarmLocked();
        }
        if (this.mQuickDozeActivated && !this.mQuickDozeActivatedWhileIdling) {
            return;
        }
        maybeStopMonitoringMotionLocked();
        boolean becomeInactive = this.mState != 0 || this.mLightState == 7;
        becomeActiveLocked(type, Process.myUid(), timeout, this.mLightState == 7);
        if (becomeInactive) {
            becomeInactiveIfAppropriateLocked();
        }
    }

    void receivedGenericLocationLocked(Location location) {
        if (this.mState != 4) {
            cancelLocatingLocked();
            return;
        }
        this.mLastGenericLocation = new Location(location);
        if (location.getAccuracy() > this.mConstants.LOCATION_ACCURACY && this.mHasGps) {
            return;
        }
        this.mLocated = true;
        if (this.mNotMoving) {
            stepIdleStateLocked("s:location");
        }
    }

    void receivedGpsLocationLocked(Location location) {
        if (this.mState != 4) {
            cancelLocatingLocked();
            return;
        }
        this.mLastGpsLocation = new Location(location);
        if (location.getAccuracy() > this.mConstants.LOCATION_ACCURACY) {
            return;
        }
        this.mLocated = true;
        if (this.mNotMoving) {
            stepIdleStateLocked("s:gps");
        }
    }

    void startMonitoringMotionLocked() {
        if (this.mMotionSensor != null && !this.mMotionListener.active) {
            this.mMotionListener.registerLocked();
        }
    }

    private void maybeStopMonitoringMotionLocked() {
        if (this.mMotionSensor != null && this.mStationaryListeners.size() == 0) {
            if (this.mMotionListener.active) {
                this.mMotionListener.unregisterLocked();
                cancelMotionTimeoutAlarmLocked();
            }
            cancelMotionRegistrationAlarmLocked();
        }
    }

    void cancelAlarmLocked() {
        if (this.mNextAlarmTime != 0) {
            this.mNextAlarmTime = 0L;
            this.mAlarmManager.cancel(this.mDeepAlarmListener);
        }
    }

    private void cancelAllLightAlarmsLocked() {
        cancelLightAlarmLocked();
        cancelLightMaintenanceAlarmLocked();
    }

    private void cancelLightAlarmLocked() {
        if (this.mNextLightAlarmTime != 0) {
            this.mNextLightAlarmTime = 0L;
            this.mAlarmManager.cancel(this.mLightAlarmListener);
        }
    }

    private void cancelLightMaintenanceAlarmLocked() {
        if (this.mNextLightMaintenanceAlarmTime != 0) {
            this.mNextLightMaintenanceAlarmTime = 0L;
            this.mAlarmManager.cancel(this.mLightMaintenanceAlarmListener);
        }
    }

    void cancelLocatingLocked() {
        if (this.mLocating) {
            LocationManager locationManager = this.mInjector.getLocationManager();
            locationManager.removeUpdates(this.mGenericLocationListener);
            locationManager.removeUpdates(this.mGpsLocationListener);
            this.mLocating = false;
        }
    }

    private void cancelMotionTimeoutAlarmLocked() {
        this.mAlarmManager.cancel(this.mMotionTimeoutAlarmListener);
    }

    private void cancelMotionRegistrationAlarmLocked() {
        this.mAlarmManager.cancel(this.mMotionRegistrationAlarmListener);
    }

    void cancelSensingTimeoutAlarmLocked() {
        if (this.mNextSensingTimeoutAlarmTime != 0) {
            this.mNextSensingTimeoutAlarmTime = 0L;
            this.mAlarmManager.cancel(this.mSensingTimeoutAlarmListener);
        }
    }

    void scheduleAlarmLocked(long delay, boolean idleUntil) {
        int i;
        if (this.mUseMotionSensor && this.mMotionSensor == null && (i = this.mState) != 7 && i != 5 && i != 6) {
            return;
        }
        long elapsedRealtime = SystemClock.elapsedRealtime() + delay;
        this.mNextAlarmTime = elapsedRealtime;
        if (idleUntil) {
            this.mAlarmManager.setIdleUntil(2, elapsedRealtime, "DeviceIdleController.deep", this.mDeepAlarmListener, this.mHandler);
        } else if (this.mState == 4) {
            this.mAlarmManager.setExact(2, elapsedRealtime, "DeviceIdleController.deep", this.mDeepAlarmListener, this.mHandler);
        } else if (this.mConstants.USE_WINDOW_ALARMS) {
            this.mAlarmManager.setWindow(2, this.mNextAlarmTime, this.mConstants.FLEX_TIME_SHORT, "DeviceIdleController.deep", this.mDeepAlarmListener, this.mHandler);
        } else {
            this.mAlarmManager.set(2, this.mNextAlarmTime, "DeviceIdleController.deep", this.mDeepAlarmListener, this.mHandler);
        }
    }

    void scheduleLightAlarmLocked(long delay, long flex) {
        this.mNextLightAlarmTime = this.mInjector.getElapsedRealtime() + delay;
        if (this.mConstants.USE_WINDOW_ALARMS) {
            this.mAlarmManager.setWindow(3, this.mNextLightAlarmTime, flex, "DeviceIdleController.light", this.mLightAlarmListener, this.mHandler);
        } else {
            this.mAlarmManager.set(3, this.mNextLightAlarmTime, "DeviceIdleController.light", this.mLightAlarmListener, this.mHandler);
        }
    }

    void scheduleLightMaintenanceAlarmLocked(long delay) {
        long elapsedRealtime = this.mInjector.getElapsedRealtime() + delay;
        this.mNextLightMaintenanceAlarmTime = elapsedRealtime;
        this.mAlarmManager.setWindow(2, elapsedRealtime, this.mConstants.FLEX_TIME_SHORT, "DeviceIdleController.light", this.mLightMaintenanceAlarmListener, this.mHandler);
    }

    long getNextLightAlarmTimeForTesting() {
        long j;
        synchronized (this) {
            j = this.mNextLightAlarmTime;
        }
        return j;
    }

    long getNextLightMaintenanceAlarmTimeForTesting() {
        long j;
        synchronized (this) {
            j = this.mNextLightMaintenanceAlarmTime;
        }
        return j;
    }

    private void scheduleMotionRegistrationAlarmLocked() {
        long nextMotionRegistrationAlarmTime = this.mInjector.getElapsedRealtime() + (this.mConstants.MOTION_INACTIVE_TIMEOUT / 2);
        if (this.mConstants.USE_WINDOW_ALARMS) {
            this.mAlarmManager.setWindow(2, nextMotionRegistrationAlarmTime, this.mConstants.MOTION_INACTIVE_TIMEOUT_FLEX, "DeviceIdleController.motion_registration", this.mMotionRegistrationAlarmListener, this.mHandler);
        } else {
            this.mAlarmManager.set(2, nextMotionRegistrationAlarmTime, "DeviceIdleController.motion_registration", this.mMotionRegistrationAlarmListener, this.mHandler);
        }
    }

    private void scheduleMotionTimeoutAlarmLocked() {
        long nextMotionTimeoutAlarmTime = this.mInjector.getElapsedRealtime() + this.mConstants.MOTION_INACTIVE_TIMEOUT;
        if (this.mConstants.USE_WINDOW_ALARMS) {
            this.mAlarmManager.setWindow(2, nextMotionTimeoutAlarmTime, this.mConstants.MOTION_INACTIVE_TIMEOUT_FLEX, "DeviceIdleController.motion", this.mMotionTimeoutAlarmListener, this.mHandler);
        } else {
            this.mAlarmManager.set(2, nextMotionTimeoutAlarmTime, "DeviceIdleController.motion", this.mMotionTimeoutAlarmListener, this.mHandler);
        }
    }

    void scheduleSensingTimeoutAlarmLocked(long delay) {
        this.mNextSensingTimeoutAlarmTime = SystemClock.elapsedRealtime() + delay;
        if (this.mConstants.USE_WINDOW_ALARMS) {
            this.mAlarmManager.setWindow(2, this.mNextSensingTimeoutAlarmTime, this.mConstants.FLEX_TIME_SHORT, "DeviceIdleController.sensing", this.mSensingTimeoutAlarmListener, this.mHandler);
        } else {
            this.mAlarmManager.set(2, this.mNextSensingTimeoutAlarmTime, "DeviceIdleController.sensing", this.mSensingTimeoutAlarmListener, this.mHandler);
        }
    }

    private static int[] buildAppIdArray(ArrayMap<String, Integer> systemApps, ArrayMap<String, Integer> userApps, SparseBooleanArray outAppIds) {
        outAppIds.clear();
        if (systemApps != null) {
            for (int i = 0; i < systemApps.size(); i++) {
                outAppIds.put(systemApps.valueAt(i).intValue(), true);
            }
        }
        if (userApps != null) {
            for (int i2 = 0; i2 < userApps.size(); i2++) {
                outAppIds.put(userApps.valueAt(i2).intValue(), true);
            }
        }
        int size = outAppIds.size();
        int[] appids = new int[size];
        for (int i3 = 0; i3 < size; i3++) {
            appids[i3] = outAppIds.keyAt(i3);
        }
        return appids;
    }

    private void updateWhitelistAppIdsLocked() {
        this.mPowerSaveWhitelistExceptIdleAppIdArray = buildAppIdArray(this.mPowerSaveWhitelistAppsExceptIdle, this.mPowerSaveWhitelistUserApps, this.mPowerSaveWhitelistExceptIdleAppIds);
        this.mPowerSaveWhitelistAllAppIdArray = buildAppIdArray(this.mPowerSaveWhitelistApps, this.mPowerSaveWhitelistUserApps, this.mPowerSaveWhitelistAllAppIds);
        this.mPowerSaveWhitelistUserAppIdArray = buildAppIdArray(null, this.mPowerSaveWhitelistUserApps, this.mPowerSaveWhitelistUserAppIds);
        ActivityManagerInternal activityManagerInternal = this.mLocalActivityManager;
        if (activityManagerInternal != null) {
            activityManagerInternal.setDeviceIdleAllowlist(this.mPowerSaveWhitelistAllAppIdArray, this.mPowerSaveWhitelistExceptIdleAppIdArray);
        }
        PowerManagerInternal powerManagerInternal = this.mLocalPowerManager;
        if (powerManagerInternal != null) {
            powerManagerInternal.setDeviceIdleWhitelist(this.mPowerSaveWhitelistAllAppIdArray);
        }
        passWhiteListsToForceAppStandbyTrackerLocked();
    }

    private void updateTempWhitelistAppIdsLocked(int uid, boolean adding, long durationMs, int type, int reasonCode, String reason, int callingUid) {
        int size = this.mTempWhitelistAppIdEndTimes.size();
        if (this.mTempWhitelistAppIdArray.length != size) {
            this.mTempWhitelistAppIdArray = new int[size];
        }
        for (int i = 0; i < size; i++) {
            this.mTempWhitelistAppIdArray[i] = this.mTempWhitelistAppIdEndTimes.keyAt(i);
        }
        ActivityManagerInternal activityManagerInternal = this.mLocalActivityManager;
        if (activityManagerInternal != null) {
            activityManagerInternal.updateDeviceIdleTempAllowlist(this.mTempWhitelistAppIdArray, uid, adding, durationMs, type, reasonCode, reason, callingUid);
        }
        PowerManagerInternal powerManagerInternal = this.mLocalPowerManager;
        if (powerManagerInternal != null) {
            powerManagerInternal.setDeviceIdleTempWhitelist(this.mTempWhitelistAppIdArray);
        }
        passWhiteListsToForceAppStandbyTrackerLocked();
    }

    private void reportPowerSaveWhitelistChangedLocked() {
        Intent intent = new Intent("android.os.action.POWER_SAVE_WHITELIST_CHANGED");
        intent.addFlags(1073741824);
        getContext().sendBroadcastAsUser(intent, UserHandle.SYSTEM);
    }

    private void reportTempWhitelistChangedLocked(int uid, boolean added) {
        this.mHandler.obtainMessage(13, uid, added ? 1 : 0).sendToTarget();
        Intent intent = new Intent("android.os.action.POWER_SAVE_TEMP_WHITELIST_CHANGED");
        intent.addFlags(1073741824);
        getContext().sendBroadcastAsUser(intent, UserHandle.SYSTEM);
    }

    private void passWhiteListsToForceAppStandbyTrackerLocked() {
        this.mAppStateTracker.setPowerSaveExemptionListAppIds(this.mPowerSaveWhitelistExceptIdleAppIdArray, this.mPowerSaveWhitelistUserAppIdArray, this.mTempWhitelistAppIdArray);
    }

    /*  JADX ERROR: JadxRuntimeException in pass: RegionMakerVisitor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find top splitter block for handler:B:15:0x002c
        	at jadx.core.utils.BlockUtils.getTopSplitterForHandler(BlockUtils.java:1234)
        	at jadx.core.dex.visitors.regions.RegionMaker.processTryCatchBlocks(RegionMaker.java:1018)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:55)
        */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:15:0x002c -> B:16:0x002e). Please submit an issue!!! */
    void readConfigFileLocked() {
        /*
            r3 = this;
            android.util.ArrayMap<java.lang.String, java.lang.Integer> r0 = r3.mPowerSaveWhitelistUserApps
            r0.clear()
            android.util.AtomicFile r0 = r3.mConfigFile     // Catch: java.io.FileNotFoundException -> L2f
            java.io.FileInputStream r0 = r0.openRead()     // Catch: java.io.FileNotFoundException -> L2f
            org.xmlpull.v1.XmlPullParser r1 = android.util.Xml.newPullParser()     // Catch: java.lang.Throwable -> L20 org.xmlpull.v1.XmlPullParserException -> L27
            java.nio.charset.Charset r2 = java.nio.charset.StandardCharsets.UTF_8     // Catch: java.lang.Throwable -> L20 org.xmlpull.v1.XmlPullParserException -> L27
            java.lang.String r2 = r2.name()     // Catch: java.lang.Throwable -> L20 org.xmlpull.v1.XmlPullParserException -> L27
            r1.setInput(r0, r2)     // Catch: java.lang.Throwable -> L20 org.xmlpull.v1.XmlPullParserException -> L27
            r3.readConfigFileLocked(r1)     // Catch: java.lang.Throwable -> L20 org.xmlpull.v1.XmlPullParserException -> L27
            r0.close()     // Catch: java.io.IOException -> L2c
            goto L2b
        L20:
            r1 = move-exception
            r0.close()     // Catch: java.io.IOException -> L25
            goto L26
        L25:
            r2 = move-exception
        L26:
            throw r1
        L27:
            r1 = move-exception
            r0.close()     // Catch: java.io.IOException -> L2c
        L2b:
            goto L2e
        L2c:
            r1 = move-exception
        L2e:
            return
        L2f:
            r0 = move-exception
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.DeviceIdleController.readConfigFileLocked():void");
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private void readConfigFileLocked(XmlPullParser parser) {
        int type;
        PackageManager pm = getContext().getPackageManager();
        while (true) {
            try {
                type = parser.next();
                if (type == 2 || type == 1) {
                    break;
                }
            } catch (IOException e) {
                Slog.w(TAG, "Failed parsing config " + e);
                return;
            } catch (IllegalStateException e2) {
                Slog.w(TAG, "Failed parsing config " + e2);
                return;
            } catch (IndexOutOfBoundsException e3) {
                Slog.w(TAG, "Failed parsing config " + e3);
                return;
            } catch (NullPointerException e4) {
                Slog.w(TAG, "Failed parsing config " + e4);
                return;
            } catch (NumberFormatException e5) {
                Slog.w(TAG, "Failed parsing config " + e5);
                return;
            } catch (XmlPullParserException e6) {
                Slog.w(TAG, "Failed parsing config " + e6);
                return;
            }
        }
        if (type != 2) {
            throw new IllegalStateException("no start tag found");
        }
        int outerDepth = parser.getDepth();
        while (true) {
            int type2 = parser.next();
            if (type2 != 1) {
                if (type2 != 3 || parser.getDepth() > outerDepth) {
                    if (type2 != 3 && type2 != 4) {
                        String tagName = parser.getName();
                        char c = 65535;
                        switch (tagName.hashCode()) {
                            case 3797:
                                if (tagName.equals("wl")) {
                                    c = 0;
                                    break;
                                }
                                break;
                            case 111376009:
                                if (tagName.equals("un-wl")) {
                                    c = 1;
                                    break;
                                }
                                break;
                        }
                        switch (c) {
                            case 0:
                                String name = parser.getAttributeValue(null, "n");
                                if (name != null) {
                                    try {
                                        ApplicationInfo ai = pm.getApplicationInfo(name, 4194304);
                                        this.mPowerSaveWhitelistUserApps.put(ai.packageName, Integer.valueOf(UserHandle.getAppId(ai.uid)));
                                        break;
                                    } catch (PackageManager.NameNotFoundException e7) {
                                        break;
                                    }
                                }
                                break;
                            case 1:
                                String packageName = parser.getAttributeValue(null, "n");
                                if (this.mPowerSaveWhitelistApps.containsKey(packageName)) {
                                    this.mRemovedFromSystemWhitelistApps.put(packageName, this.mPowerSaveWhitelistApps.remove(packageName));
                                    break;
                                }
                                break;
                            default:
                                Slog.w(TAG, "Unknown element under <config>: " + parser.getName());
                                XmlUtils.skipCurrentTag(parser);
                                break;
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    void writeConfigFileLocked() {
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessageDelayed(1, 5000L);
    }

    void handleWriteConfigFile() {
        ByteArrayOutputStream memStream = new ByteArrayOutputStream();
        try {
            synchronized (this) {
                XmlSerializer out = new FastXmlSerializer();
                out.setOutput(memStream, StandardCharsets.UTF_8.name());
                writeConfigFileLocked(out);
            }
        } catch (IOException e) {
        }
        synchronized (this.mConfigFile) {
            FileOutputStream stream = null;
            try {
                stream = this.mConfigFile.startWrite();
                memStream.writeTo(stream);
                this.mConfigFile.finishWrite(stream);
            } catch (IOException e2) {
                Slog.w(TAG, "Error writing config file", e2);
                this.mConfigFile.failWrite(stream);
            }
        }
    }

    void writeConfigFileLocked(XmlSerializer out) throws IOException {
        out.startDocument(null, true);
        out.startTag(null, "config");
        for (int i = 0; i < this.mPowerSaveWhitelistUserApps.size(); i++) {
            String name = this.mPowerSaveWhitelistUserApps.keyAt(i);
            out.startTag(null, "wl");
            out.attribute(null, "n", name);
            out.endTag(null, "wl");
        }
        for (int i2 = 0; i2 < this.mRemovedFromSystemWhitelistApps.size(); i2++) {
            out.startTag(null, "un-wl");
            out.attribute(null, "n", this.mRemovedFromSystemWhitelistApps.keyAt(i2));
            out.endTag(null, "un-wl");
        }
        out.endTag(null, "config");
        out.endDocument();
    }

    static void dumpHelp(PrintWriter pw) {
        pw.println("Device idle controller (deviceidle) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  step [light|deep]");
        pw.println("    Immediately step to next state, without waiting for alarm.");
        pw.println("  force-idle [light|deep]");
        pw.println("    Force directly into idle mode, regardless of other device state.");
        pw.println("  force-inactive");
        pw.println("    Force to be inactive, ready to freely step idle states.");
        pw.println("  unforce");
        pw.println("    Resume normal functioning after force-idle or force-inactive.");
        pw.println("  get [light|deep|force|screen|charging|network]");
        pw.println("    Retrieve the current given state.");
        pw.println("  disable [light|deep|all]");
        pw.println("    Completely disable device idle mode.");
        pw.println("  enable [light|deep|all]");
        pw.println("    Re-enable device idle mode after it had previously been disabled.");
        pw.println("  enabled [light|deep|all]");
        pw.println("    Print 1 if device idle mode is currently enabled, else 0.");
        pw.println("  whitelist");
        pw.println("    Print currently whitelisted apps.");
        pw.println("  whitelist [package ...]");
        pw.println("    Add (prefix with +) or remove (prefix with -) packages.");
        pw.println("  sys-whitelist [package ...|reset]");
        pw.println("    Prefix the package with '-' to remove it from the system whitelist or '+' to put it back in the system whitelist.");
        pw.println("    Note that only packages that were earlier removed from the system whitelist can be added back.");
        pw.println("    reset will reset the whitelist to the original state");
        pw.println("    Prints the system whitelist if no arguments are specified");
        pw.println("  except-idle-whitelist [package ...|reset]");
        pw.println("    Prefix the package with '+' to add it to whitelist or '=' to check if it is already whitelisted");
        pw.println("    [reset] will reset the whitelist to it's original state");
        pw.println("    Note that unlike <whitelist> cmd, changes made using this won't be persisted across boots");
        pw.println("  tempwhitelist");
        pw.println("    Print packages that are temporarily whitelisted.");
        pw.println("  tempwhitelist [-u USER] [-d DURATION] [-r] [package]");
        pw.println("    Temporarily place package in whitelist for DURATION milliseconds.");
        pw.println("    If no DURATION is specified, 10 seconds is used");
        pw.println("    If [-r] option is used, then the package is removed from temp whitelist and any [-d] is ignored");
        pw.println("  motion");
        pw.println("    Simulate a motion event to bring the device out of deep doze");
        pw.println("  pre-idle-factor [0|1|2]");
        pw.println("    Set a new factor to idle time before step to idle(inactive_to and idle_after_inactive_to)");
        pw.println("  reset-pre-idle-factor");
        pw.println("    Reset factor to idle time to default");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class Shell extends ShellCommand {
        int userId = 0;

        Shell() {
        }

        public int onCommand(String cmd) {
            return DeviceIdleController.this.onShellCommand(this, cmd);
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            DeviceIdleController.dumpHelp(pw);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4823=5, 4916=4, 4547=5] */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    int onShellCommand(Shell shell, String cmd) {
        long token;
        Exception e;
        PrintWriter pw = shell.getOutPrintWriter();
        char c = 1;
        if ("step".equals(cmd)) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            synchronized (this) {
                token = Binder.clearCallingIdentity();
                String arg = shell.getNextArg();
                if (arg != null && !"deep".equals(arg)) {
                    if ("light".equals(arg)) {
                        stepLightIdleStateLocked("s:shell", true);
                        pw.print("Stepped to light: ");
                        pw.println(lightStateToString(this.mLightState));
                    } else {
                        pw.println("Unknown idle mode: " + arg);
                    }
                }
                stepIdleStateLocked("s:shell");
                pw.print("Stepped to deep: ");
                pw.println(stateToString(this.mState));
            }
        } else if ("force-idle".equals(cmd)) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            synchronized (this) {
                token = Binder.clearCallingIdentity();
                String arg2 = shell.getNextArg();
                if (arg2 != null && !"deep".equals(arg2)) {
                    if ("light".equals(arg2)) {
                        this.mForceIdle = true;
                        becomeInactiveIfAppropriateLocked();
                        int curLightState = this.mLightState;
                        while (curLightState != 4) {
                            stepLightIdleStateLocked("s:shell", true);
                            int i = this.mLightState;
                            if (curLightState == i) {
                                pw.print("Unable to go light idle; stopped at ");
                                pw.println(lightStateToString(this.mLightState));
                                exitForceIdleLocked();
                                return -1;
                            }
                            curLightState = i;
                        }
                        pw.println("Now forced in to light idle mode");
                    } else {
                        pw.println("Unknown idle mode: " + arg2);
                    }
                }
                if (!this.mDeepEnabled) {
                    pw.println("Unable to go deep idle; not enabled");
                    return -1;
                }
                this.mForceIdle = true;
                becomeInactiveIfAppropriateLocked();
                int curState = this.mState;
                while (curState != 5) {
                    stepIdleStateLocked("s:shell");
                    int i2 = this.mState;
                    if (curState == i2) {
                        pw.print("Unable to go deep idle; stopped at ");
                        pw.println(stateToString(this.mState));
                        exitForceIdleLocked();
                        return -1;
                    }
                    curState = i2;
                }
                pw.println("Now forced in to deep idle mode");
            }
        } else if ("force-inactive".equals(cmd)) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            synchronized (this) {
                token = Binder.clearCallingIdentity();
                this.mForceIdle = true;
                becomeInactiveIfAppropriateLocked();
                pw.print("Light state: ");
                pw.print(lightStateToString(this.mLightState));
                pw.print(", deep state: ");
                pw.println(stateToString(this.mState));
            }
        } else if ("unforce".equals(cmd)) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            synchronized (this) {
                token = Binder.clearCallingIdentity();
                exitForceIdleLocked();
                pw.print("Light state: ");
                pw.print(lightStateToString(this.mLightState));
                pw.print(", deep state: ");
                pw.println(stateToString(this.mState));
            }
        } else if ("get".equals(cmd)) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            synchronized (this) {
                String arg3 = shell.getNextArg();
                if (arg3 != null) {
                    long token2 = Binder.clearCallingIdentity();
                    switch (arg3.hashCode()) {
                        case -907689876:
                            if (arg3.equals("screen")) {
                                c = 4;
                                break;
                            }
                            c = 65535;
                            break;
                        case 3079404:
                            if (arg3.equals("deep")) {
                                break;
                            }
                            c = 65535;
                            break;
                        case 97618667:
                            if (arg3.equals("force")) {
                                c = 2;
                                break;
                            }
                            c = 65535;
                            break;
                        case 102970646:
                            if (arg3.equals("light")) {
                                c = 0;
                                break;
                            }
                            c = 65535;
                            break;
                        case 107947501:
                            if (arg3.equals("quick")) {
                                c = 3;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1436115569:
                            if (arg3.equals("charging")) {
                                c = 5;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1843485230:
                            if (arg3.equals("network")) {
                                c = 6;
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
                            pw.println(lightStateToString(this.mLightState));
                            break;
                        case 1:
                            pw.println(stateToString(this.mState));
                            break;
                        case 2:
                            pw.println(this.mForceIdle);
                            break;
                        case 3:
                            pw.println(this.mQuickDozeActivated);
                            break;
                        case 4:
                            pw.println(this.mScreenOn);
                            break;
                        case 5:
                            pw.println(this.mCharging);
                            break;
                        case 6:
                            pw.println(this.mNetworkConnected);
                            break;
                        default:
                            pw.println("Unknown get option: " + arg3);
                            break;
                    }
                    Binder.restoreCallingIdentity(token2);
                } else {
                    pw.println("Argument required");
                }
            }
        } else if ("disable".equals(cmd)) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            synchronized (this) {
                token = Binder.clearCallingIdentity();
                String arg4 = shell.getNextArg();
                boolean becomeActive = false;
                boolean valid = false;
                if (arg4 == null || "deep".equals(arg4) || "all".equals(arg4)) {
                    valid = true;
                    if (this.mDeepEnabled) {
                        this.mDeepEnabled = false;
                        becomeActive = true;
                        pw.println("Deep idle mode disabled");
                    }
                }
                if (arg4 == null || "light".equals(arg4) || "all".equals(arg4)) {
                    valid = true;
                    if (this.mLightEnabled) {
                        this.mLightEnabled = false;
                        becomeActive = true;
                        pw.println("Light idle mode disabled");
                    }
                }
                if (becomeActive) {
                    this.mActiveReason = 6;
                    becomeActiveLocked((arg4 == null ? "all" : arg4) + "-disabled", Process.myUid());
                }
                if (!valid) {
                    pw.println("Unknown idle mode: " + arg4);
                }
            }
        } else if ("enable".equals(cmd)) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            synchronized (this) {
                token = Binder.clearCallingIdentity();
                String arg5 = shell.getNextArg();
                boolean becomeInactive = false;
                boolean valid2 = false;
                if (arg5 == null || "deep".equals(arg5) || "all".equals(arg5)) {
                    valid2 = true;
                    if (!this.mDeepEnabled) {
                        this.mDeepEnabled = true;
                        becomeInactive = true;
                        pw.println("Deep idle mode enabled");
                    }
                }
                if (arg5 == null || "light".equals(arg5) || "all".equals(arg5)) {
                    valid2 = true;
                    if (!this.mLightEnabled) {
                        this.mLightEnabled = true;
                        becomeInactive = true;
                        pw.println("Light idle mode enable");
                    }
                }
                if (becomeInactive) {
                    becomeInactiveIfAppropriateLocked();
                }
                if (!valid2) {
                    pw.println("Unknown idle mode: " + arg5);
                }
            }
        } else if (ServiceConfigAccessor.PROVIDER_MODE_ENABLED.equals(cmd)) {
            synchronized (this) {
                String arg6 = shell.getNextArg();
                if (arg6 != null && !"all".equals(arg6)) {
                    if ("deep".equals(arg6)) {
                        pw.println(this.mDeepEnabled ? "1" : 0);
                    } else if ("light".equals(arg6)) {
                        pw.println(this.mLightEnabled ? "1" : 0);
                    } else {
                        pw.println("Unknown idle mode: " + arg6);
                    }
                }
                if (this.mDeepEnabled && this.mLightEnabled) {
                    r3 = "1";
                }
                pw.println(r3);
            }
        } else {
            char c2 = '=';
            if ("whitelist".equals(cmd)) {
                String arg7 = shell.getNextArg();
                if (arg7 != null) {
                    getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                    token = Binder.clearCallingIdentity();
                    for (char c3 = '+'; arg7.length() >= 1 && (arg7.charAt(0) == '-' || arg7.charAt(0) == c3 || arg7.charAt(0) == c2); c3 = '+') {
                        try {
                            char op = arg7.charAt(0);
                            String pkg = arg7.substring(1);
                            if (op == c3) {
                                if (addPowerSaveWhitelistAppsInternal(Collections.singletonList(pkg)) == 1) {
                                    pw.println("Added: " + pkg);
                                } else {
                                    pw.println("Unknown package: " + pkg);
                                }
                            } else if (op != '-') {
                                pw.println(getPowerSaveWhitelistAppInternal(pkg));
                            } else if (removePowerSaveWhitelistAppInternal(pkg)) {
                                pw.println("Removed: " + pkg);
                            }
                            String nextArg = shell.getNextArg();
                            arg7 = nextArg;
                            if (nextArg != null) {
                                c2 = '=';
                            }
                        } finally {
                        }
                    }
                    pw.println("Package must be prefixed with +, -, or =: " + arg7);
                    return -1;
                }
                synchronized (this) {
                    for (int j = 0; j < this.mPowerSaveWhitelistAppsExceptIdle.size(); j++) {
                        pw.print("system-excidle,");
                        pw.print(this.mPowerSaveWhitelistAppsExceptIdle.keyAt(j));
                        pw.print(",");
                        pw.println(this.mPowerSaveWhitelistAppsExceptIdle.valueAt(j));
                    }
                    for (int j2 = 0; j2 < this.mPowerSaveWhitelistApps.size(); j2++) {
                        pw.print("system,");
                        pw.print(this.mPowerSaveWhitelistApps.keyAt(j2));
                        pw.print(",");
                        pw.println(this.mPowerSaveWhitelistApps.valueAt(j2));
                    }
                    for (int j3 = 0; j3 < this.mPowerSaveWhitelistUserApps.size(); j3++) {
                        pw.print("user,");
                        pw.print(this.mPowerSaveWhitelistUserApps.keyAt(j3));
                        pw.print(",");
                        pw.println(this.mPowerSaveWhitelistUserApps.valueAt(j3));
                    }
                }
            } else if ("tempwhitelist".equals(cmd)) {
                long duration = 10000;
                boolean removePkg = false;
                while (true) {
                    String opt = shell.getNextOption();
                    if (opt == null) {
                        String arg8 = shell.getNextArg();
                        if (arg8 != null) {
                            if (removePkg) {
                                try {
                                    removePowerSaveTempAllowlistAppChecked(arg8, shell.userId);
                                } catch (Exception e2) {
                                    e = e2;
                                    pw.println("Failed: " + e);
                                    return -1;
                                }
                            } else {
                                try {
                                } catch (Exception e3) {
                                    e = e3;
                                }
                                try {
                                    addPowerSaveTempAllowlistAppChecked(arg8, duration, shell.userId, FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_SHELL, "shell");
                                } catch (Exception e4) {
                                    e = e4;
                                    pw.println("Failed: " + e);
                                    return -1;
                                }
                            }
                        } else if (removePkg) {
                            pw.println("[-r] requires a package name");
                            return -1;
                        } else {
                            dumpTempWhitelistSchedule(pw, false);
                        }
                    } else if ("-u".equals(opt)) {
                        String opt2 = shell.getNextArg();
                        if (opt2 == null) {
                            pw.println("-u requires a user number");
                            return -1;
                        }
                        shell.userId = Integer.parseInt(opt2);
                    } else if ("-d".equals(opt)) {
                        String opt3 = shell.getNextArg();
                        if (opt3 == null) {
                            pw.println("-d requires a duration");
                            return -1;
                        }
                        duration = Long.parseLong(opt3);
                    } else if ("-r".equals(opt)) {
                        removePkg = true;
                    }
                }
            } else if ("except-idle-whitelist".equals(cmd)) {
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                token = Binder.clearCallingIdentity();
                try {
                    String arg9 = shell.getNextArg();
                    if (arg9 == null) {
                        pw.println("No arguments given");
                        return -1;
                    } else if (!"reset".equals(arg9)) {
                        while (arg9.length() >= 1 && (arg9.charAt(0) == '-' || arg9.charAt(0) == '+' || arg9.charAt(0) == '=')) {
                            char op2 = arg9.charAt(0);
                            String pkg2 = arg9.substring(1);
                            if (op2 == '+') {
                                if (addPowerSaveWhitelistExceptIdleInternal(pkg2)) {
                                    pw.println("Added: " + pkg2);
                                } else {
                                    pw.println("Unknown package: " + pkg2);
                                }
                            } else if (op2 != '=') {
                                pw.println("Unknown argument: " + arg9);
                                return -1;
                            } else {
                                pw.println(getPowerSaveWhitelistExceptIdleInternal(pkg2));
                            }
                            String nextArg2 = shell.getNextArg();
                            arg9 = nextArg2;
                            if (nextArg2 == null) {
                            }
                        }
                        pw.println("Package must be prefixed with +, -, or =: " + arg9);
                        return -1;
                    } else {
                        resetPowerSaveWhitelistExceptIdleInternal();
                    }
                } finally {
                }
            } else if ("sys-whitelist".equals(cmd)) {
                String arg10 = shell.getNextArg();
                if (arg10 != null) {
                    getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                    token = Binder.clearCallingIdentity();
                    try {
                        if (!"reset".equals(arg10)) {
                            for (char c4 = '-'; arg10.length() >= 1 && (arg10.charAt(0) == c4 || arg10.charAt(0) == '+'); c4 = '-') {
                                char op3 = arg10.charAt(0);
                                String pkg3 = arg10.substring(1);
                                switch (op3) {
                                    case '+':
                                        if (restoreSystemPowerWhitelistAppInternal(pkg3)) {
                                            pw.println("Restored " + pkg3);
                                            break;
                                        }
                                        break;
                                    case '-':
                                        if (removeSystemPowerWhitelistAppInternal(pkg3)) {
                                            pw.println("Removed " + pkg3);
                                            break;
                                        }
                                        break;
                                }
                                String nextArg3 = shell.getNextArg();
                                arg10 = nextArg3;
                                if (nextArg3 != null) {
                                }
                            }
                            pw.println("Package must be prefixed with + or - " + arg10);
                            return -1;
                        }
                        resetSystemPowerWhitelistInternal();
                    } finally {
                    }
                } else {
                    synchronized (this) {
                        for (int j4 = 0; j4 < this.mPowerSaveWhitelistApps.size(); j4++) {
                            pw.print(this.mPowerSaveWhitelistApps.keyAt(j4));
                            pw.print(",");
                            pw.println(this.mPowerSaveWhitelistApps.valueAt(j4));
                        }
                    }
                }
            } else if ("motion".equals(cmd)) {
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                synchronized (this) {
                    token = Binder.clearCallingIdentity();
                    motionLocked();
                    pw.print("Light state: ");
                    pw.print(lightStateToString(this.mLightState));
                    pw.print(", deep state: ");
                    pw.println(stateToString(this.mState));
                }
            } else if ("pre-idle-factor".equals(cmd)) {
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                synchronized (this) {
                    token = Binder.clearCallingIdentity();
                    int ret = -1;
                    try {
                        String arg11 = shell.getNextArg();
                        boolean valid3 = false;
                        if (arg11 != null) {
                            int mode = Integer.parseInt(arg11);
                            ret = setPreIdleTimeoutMode(mode);
                            if (ret == 1) {
                                pw.println("pre-idle-factor: " + mode);
                                valid3 = true;
                            } else if (ret == 2) {
                                valid3 = true;
                                pw.println("Deep idle not supported");
                            } else if (ret == 0) {
                                valid3 = true;
                                pw.println("Idle timeout factor not changed");
                            }
                        }
                        if (!valid3) {
                            pw.println("Unknown idle timeout factor: " + arg11 + ",(error code: " + ret + ")");
                        }
                    } catch (NumberFormatException e5) {
                        pw.println("Unknown idle timeout factor,(error code: " + ret + ")");
                        Binder.restoreCallingIdentity(token);
                    }
                }
            } else if (!"reset-pre-idle-factor".equals(cmd)) {
                return shell.handleDefaultCommands(cmd);
            } else {
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                synchronized (this) {
                    token = Binder.clearCallingIdentity();
                    resetPreIdleTimeoutMode();
                }
            }
        }
        return 0;
    }

    void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        String label;
        if (DumpUtils.checkDumpPermission(getContext(), TAG, pw)) {
            if (args != null) {
                int userId = 0;
                int i = 0;
                while (i < args.length) {
                    String arg = args[i];
                    if ("-h".equals(arg)) {
                        dumpHelp(pw);
                        return;
                    }
                    if ("-u".equals(arg)) {
                        i++;
                        if (i < args.length) {
                            userId = Integer.parseInt(args[i]);
                        }
                    } else if (!"-a".equals(arg)) {
                        if (arg.length() <= 0 || arg.charAt(0) != '-') {
                            Shell shell = new Shell();
                            shell.userId = userId;
                            String[] newArgs = new String[args.length - i];
                            System.arraycopy(args, i, newArgs, 0, args.length - i);
                            shell.exec(this.mBinderService, null, fd, null, newArgs, null, new ResultReceiver(null));
                            return;
                        }
                        pw.println("Unknown option: " + arg);
                        return;
                    }
                    i++;
                }
            }
            synchronized (this) {
                this.mConstants.dump(pw);
                if (this.mEventCmds[0] != 0) {
                    pw.println("  Idling history:");
                    long now = SystemClock.elapsedRealtime();
                    for (int i2 = 99; i2 >= 0; i2--) {
                        int cmd = this.mEventCmds[i2];
                        if (cmd != 0) {
                            switch (cmd) {
                                case 1:
                                    label = "     normal";
                                    break;
                                case 2:
                                    label = " light-idle";
                                    break;
                                case 3:
                                    label = "light-maint";
                                    break;
                                case 4:
                                    label = "  deep-idle";
                                    break;
                                case 5:
                                    label = " deep-maint";
                                    break;
                                default:
                                    label = "         ??";
                                    break;
                            }
                            pw.print("    ");
                            pw.print(label);
                            pw.print(": ");
                            TimeUtils.formatDuration(this.mEventTimes[i2], now, pw);
                            if (this.mEventReasons[i2] != null) {
                                pw.print(" (");
                                pw.print(this.mEventReasons[i2]);
                                pw.print(")");
                            }
                            pw.println();
                        }
                    }
                }
                int size = this.mPowerSaveWhitelistAppsExceptIdle.size();
                if (size > 0) {
                    pw.println("  Whitelist (except idle) system apps:");
                    for (int i3 = 0; i3 < size; i3++) {
                        pw.print("    ");
                        pw.println(this.mPowerSaveWhitelistAppsExceptIdle.keyAt(i3));
                    }
                }
                int size2 = this.mPowerSaveWhitelistApps.size();
                if (size2 > 0) {
                    pw.println("  Whitelist system apps:");
                    for (int i4 = 0; i4 < size2; i4++) {
                        pw.print("    ");
                        pw.println(this.mPowerSaveWhitelistApps.keyAt(i4));
                    }
                }
                int size3 = this.mRemovedFromSystemWhitelistApps.size();
                if (size3 > 0) {
                    pw.println("  Removed from whitelist system apps:");
                    for (int i5 = 0; i5 < size3; i5++) {
                        pw.print("    ");
                        pw.println(this.mRemovedFromSystemWhitelistApps.keyAt(i5));
                    }
                }
                int size4 = this.mPowerSaveWhitelistUserApps.size();
                if (size4 > 0) {
                    pw.println("  Whitelist user apps:");
                    for (int i6 = 0; i6 < size4; i6++) {
                        pw.print("    ");
                        pw.println(this.mPowerSaveWhitelistUserApps.keyAt(i6));
                    }
                }
                int size5 = this.mPowerSaveWhitelistExceptIdleAppIds.size();
                if (size5 > 0) {
                    pw.println("  Whitelist (except idle) all app ids:");
                    for (int i7 = 0; i7 < size5; i7++) {
                        pw.print("    ");
                        pw.print(this.mPowerSaveWhitelistExceptIdleAppIds.keyAt(i7));
                        pw.println();
                    }
                }
                int size6 = this.mPowerSaveWhitelistUserAppIds.size();
                if (size6 > 0) {
                    pw.println("  Whitelist user app ids:");
                    for (int i8 = 0; i8 < size6; i8++) {
                        pw.print("    ");
                        pw.print(this.mPowerSaveWhitelistUserAppIds.keyAt(i8));
                        pw.println();
                    }
                }
                int size7 = this.mPowerSaveWhitelistAllAppIds.size();
                if (size7 > 0) {
                    pw.println("  Whitelist all app ids:");
                    for (int i9 = 0; i9 < size7; i9++) {
                        pw.print("    ");
                        pw.print(this.mPowerSaveWhitelistAllAppIds.keyAt(i9));
                        pw.println();
                    }
                }
                dumpTempWhitelistSchedule(pw, true);
                int[] iArr = this.mTempWhitelistAppIdArray;
                int size8 = iArr != null ? iArr.length : 0;
                if (size8 > 0) {
                    pw.println("  Temp whitelist app ids:");
                    for (int i10 = 0; i10 < size8; i10++) {
                        pw.print("    ");
                        pw.print(this.mTempWhitelistAppIdArray[i10]);
                        pw.println();
                    }
                }
                pw.print("  mLightEnabled=");
                pw.print(this.mLightEnabled);
                pw.print("  mDeepEnabled=");
                pw.println(this.mDeepEnabled);
                pw.print("  mForceIdle=");
                pw.println(this.mForceIdle);
                pw.print("  mUseMotionSensor=");
                pw.print(this.mUseMotionSensor);
                if (this.mUseMotionSensor) {
                    pw.print(" mMotionSensor=");
                    pw.println(this.mMotionSensor);
                } else {
                    pw.println();
                }
                pw.print("  mScreenOn=");
                pw.println(this.mScreenOn);
                pw.print("  mScreenLocked=");
                pw.println(this.mScreenLocked);
                pw.print("  mNetworkConnected=");
                pw.println(this.mNetworkConnected);
                pw.print("  mCharging=");
                pw.println(this.mCharging);
                if (this.mConstraints.size() != 0) {
                    pw.println("  mConstraints={");
                    for (int i11 = 0; i11 < this.mConstraints.size(); i11++) {
                        DeviceIdleConstraintTracker tracker = this.mConstraints.valueAt(i11);
                        pw.print("    \"");
                        pw.print(tracker.name);
                        pw.print("\"=");
                        if (tracker.minState == this.mState) {
                            pw.println(tracker.active);
                        } else {
                            pw.print("ignored <mMinState=");
                            pw.print(stateToString(tracker.minState));
                            pw.println(">");
                        }
                    }
                    pw.println("  }");
                }
                if (this.mUseMotionSensor || this.mStationaryListeners.size() > 0) {
                    pw.print("  mMotionActive=");
                    pw.println(this.mMotionListener.active);
                    pw.print("  mNotMoving=");
                    pw.println(this.mNotMoving);
                    pw.print("  mMotionListener.activatedTimeElapsed=");
                    pw.println(this.mMotionListener.activatedTimeElapsed);
                    pw.print("  mLastMotionEventElapsed=");
                    pw.println(this.mLastMotionEventElapsed);
                    pw.print("  ");
                    pw.print(this.mStationaryListeners.size());
                    pw.println(" stationary listeners registered");
                }
                pw.print("  mLocating=");
                pw.print(this.mLocating);
                pw.print(" mHasGps=");
                pw.print(this.mHasGps);
                pw.print(" mHasNetwork=");
                pw.print(this.mHasNetworkLocation);
                pw.print(" mLocated=");
                pw.println(this.mLocated);
                if (this.mLastGenericLocation != null) {
                    pw.print("  mLastGenericLocation=");
                    pw.println(this.mLastGenericLocation);
                }
                if (this.mLastGpsLocation != null) {
                    pw.print("  mLastGpsLocation=");
                    pw.println(this.mLastGpsLocation);
                }
                pw.print("  mState=");
                pw.print(stateToString(this.mState));
                pw.print(" mLightState=");
                pw.println(lightStateToString(this.mLightState));
                pw.print("  mInactiveTimeout=");
                TimeUtils.formatDuration(this.mInactiveTimeout, pw);
                pw.println();
                if (this.mActiveIdleOpCount != 0) {
                    pw.print("  mActiveIdleOpCount=");
                    pw.println(this.mActiveIdleOpCount);
                }
                if (this.mNextAlarmTime != 0) {
                    pw.print("  mNextAlarmTime=");
                    TimeUtils.formatDuration(this.mNextAlarmTime, SystemClock.elapsedRealtime(), pw);
                    pw.println();
                }
                if (this.mNextIdlePendingDelay != 0) {
                    pw.print("  mNextIdlePendingDelay=");
                    TimeUtils.formatDuration(this.mNextIdlePendingDelay, pw);
                    pw.println();
                }
                if (this.mNextIdleDelay != 0) {
                    pw.print("  mNextIdleDelay=");
                    TimeUtils.formatDuration(this.mNextIdleDelay, pw);
                    pw.println();
                }
                if (this.mNextLightIdleDelay != 0) {
                    pw.print("  mNextLightIdleDelay=");
                    TimeUtils.formatDuration(this.mNextLightIdleDelay, pw);
                    pw.println();
                }
                if (this.mNextLightAlarmTime != 0) {
                    pw.print("  mNextLightAlarmTime=");
                    TimeUtils.formatDuration(this.mNextLightAlarmTime, SystemClock.elapsedRealtime(), pw);
                    pw.println();
                }
                if (this.mNextLightMaintenanceAlarmTime != 0) {
                    pw.print("  mNextLightMaintenanceAlarmTime=");
                    TimeUtils.formatDuration(this.mNextLightMaintenanceAlarmTime, SystemClock.elapsedRealtime(), pw);
                    pw.println();
                }
                if (this.mCurLightIdleBudget != 0) {
                    pw.print("  mCurLightIdleBudget=");
                    TimeUtils.formatDuration(this.mCurLightIdleBudget, pw);
                    pw.println();
                }
                if (this.mMaintenanceStartTime != 0) {
                    pw.print("  mMaintenanceStartTime=");
                    TimeUtils.formatDuration(this.mMaintenanceStartTime, SystemClock.elapsedRealtime(), pw);
                    pw.println();
                }
                if (this.mJobsActive) {
                    pw.print("  mJobsActive=");
                    pw.println(this.mJobsActive);
                }
                if (this.mAlarmsActive) {
                    pw.print("  mAlarmsActive=");
                    pw.println(this.mAlarmsActive);
                }
                if (Math.abs(this.mPreIdleFactor - 1.0f) > MIN_PRE_IDLE_FACTOR_CHANGE) {
                    pw.print("  mPreIdleFactor=");
                    pw.println(this.mPreIdleFactor);
                }
            }
        }
    }

    void dumpTempWhitelistSchedule(PrintWriter pw, boolean printTitle) {
        int size = this.mTempWhitelistAppIdEndTimes.size();
        if (size > 0) {
            String prefix = "";
            if (printTitle) {
                pw.println("  Temp whitelist schedule:");
                prefix = "    ";
            }
            long timeNow = SystemClock.elapsedRealtime();
            for (int i = 0; i < size; i++) {
                pw.print(prefix);
                pw.print("UID=");
                pw.print(this.mTempWhitelistAppIdEndTimes.keyAt(i));
                pw.print(": ");
                Pair<MutableLong, String> entry = this.mTempWhitelistAppIdEndTimes.valueAt(i);
                TimeUtils.formatDuration(((MutableLong) entry.first).value, timeNow, pw);
                pw.print(" - ");
                pw.println((String) entry.second);
            }
        }
    }
}
