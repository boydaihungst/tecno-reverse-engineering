package com.android.server;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.MutableBoolean;
import android.util.Slog;
import android.view.KeyEvent;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.UiEventLogger;
import com.android.internal.logging.UiEventLoggerImpl;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wm.WindowManagerInternal;
/* loaded from: classes.dex */
public class GestureLauncherService extends SystemService {
    static final long CAMERA_POWER_DOUBLE_TAP_MAX_TIME_MS = 300;
    private static final int CAMERA_POWER_TAP_COUNT_THRESHOLD = 2;
    static final long CAMERA_VOLUME_DOUBLE_TAP_MAX_TIME_MS = 300;
    private static final boolean DBG = false;
    private static final boolean DBG_CAMERA_LIFT = false;
    private static final int EMERGENCY_GESTURE_POWER_BUTTON_COOLDOWN_PERIOD_MS_DEFAULT = 3000;
    static final int EMERGENCY_GESTURE_POWER_BUTTON_COOLDOWN_PERIOD_MS_MAX = 5000;
    private static final int EMERGENCY_GESTURE_POWER_TAP_COUNT_THRESHOLD = 5;
    static final int EMERGENCY_GESTURE_TAP_DETECTION_MIN_TIME_MS = 160;
    private static final String EXTRA_LAUNCH_EMERGENCY_VIA_GESTURE = "launch_emergency_via_gesture";
    static final long POWER_SHORT_TAP_SEQUENCE_MAX_INTERVAL_MS = 500;
    private static final String TAG = "GestureLauncherService";
    private static final boolean TRAN_CAM_QUICKUP_SUPPORT = "1".equals(SystemProperties.get("ro.tran_cam_quickup_support", "0"));
    private static final String WEAR_LAUNCH_EMERGENCY_ACTION = "com.android.systemui.action.LAUNCH_EMERGENCY";
    private static final String WEAR_LAUNCH_EMERGENCY_RETAIL_ACTION = "com.android.systemui.action.LAUNCH_EMERGENCY_RETAIL";
    private boolean mCameraDoubleTapPowerEnabled;
    private long mCameraGestureLastEventTime;
    private long mCameraGestureOnTimeMs;
    private long mCameraGestureSensor1LastOnTimeMs;
    private long mCameraGestureSensor2LastOnTimeMs;
    private int mCameraLaunchLastEventExtra;
    private boolean mCameraLaunchRegistered;
    private Sensor mCameraLaunchSensor;
    private boolean mCameraLiftRegistered;
    private final CameraLiftTriggerEventListener mCameraLiftTriggerListener;
    private Sensor mCameraLiftTriggerSensor;
    private Context mContext;
    private boolean mEmergencyGestureEnabled;
    private int mEmergencyGesturePowerButtonCooldownPeriodMs;
    private long mFirstPowerDown;
    private final GestureEventListener mGestureListener;
    private boolean mHasFeatureWatch;
    private long mLastEmergencyGestureTriggered;
    private long mLastPowerDown;
    private long mLastVolumeDown;
    private final MetricsLogger mMetricsLogger;
    private int mPowerButtonConsecutiveTaps;
    private int mPowerButtonSlowConsecutiveTaps;
    private PowerManager mPowerManager;
    private final ContentObserver mSettingObserver;
    private final UiEventLogger mUiEventLogger;
    private int mUserId;
    private final BroadcastReceiver mUserReceiver;
    private long mVibrateMilliSecondsForPanicGesture;
    private PowerManager.WakeLock mWakeLock;
    private WindowManagerInternal mWindowManagerInternal;

    /* loaded from: classes.dex */
    public enum GestureLauncherEvent implements UiEventLogger.UiEventEnum {
        GESTURE_CAMERA_LIFT(658),
        GESTURE_CAMERA_WIGGLE(659),
        GESTURE_CAMERA_DOUBLE_TAP_POWER(660),
        GESTURE_EMERGENCY_TAP_POWER(661);
        
        private final int mId;

        GestureLauncherEvent(int id) {
            this.mId = id;
        }

        public int getId() {
            return this.mId;
        }
    }

    public GestureLauncherService(Context context) {
        this(context, new MetricsLogger(), new UiEventLoggerImpl());
    }

    GestureLauncherService(Context context, MetricsLogger metricsLogger, UiEventLogger uiEventLogger) {
        super(context);
        this.mGestureListener = new GestureEventListener();
        this.mCameraLiftTriggerListener = new CameraLiftTriggerEventListener();
        this.mCameraGestureOnTimeMs = 0L;
        this.mCameraGestureLastEventTime = 0L;
        this.mCameraGestureSensor1LastOnTimeMs = 0L;
        this.mCameraGestureSensor2LastOnTimeMs = 0L;
        this.mCameraLaunchLastEventExtra = 0;
        this.mUserReceiver = new BroadcastReceiver() { // from class: com.android.server.GestureLauncherService.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                    GestureLauncherService.this.mUserId = intent.getIntExtra("android.intent.extra.user_handle", 0);
                    GestureLauncherService.this.mContext.getContentResolver().unregisterContentObserver(GestureLauncherService.this.mSettingObserver);
                    GestureLauncherService.this.registerContentObservers();
                    GestureLauncherService.this.updateCameraRegistered();
                    GestureLauncherService.this.updateCameraDoubleTapPowerEnabled();
                    GestureLauncherService.this.updateEmergencyGestureEnabled();
                    GestureLauncherService.this.updateEmergencyGesturePowerButtonCooldownPeriodMs();
                }
            }
        };
        this.mSettingObserver = new ContentObserver(new Handler()) { // from class: com.android.server.GestureLauncherService.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri, int userId) {
                if (userId == GestureLauncherService.this.mUserId) {
                    GestureLauncherService.this.updateCameraRegistered();
                    GestureLauncherService.this.updateCameraDoubleTapPowerEnabled();
                    GestureLauncherService.this.updateEmergencyGestureEnabled();
                    GestureLauncherService.this.updateEmergencyGesturePowerButtonCooldownPeriodMs();
                }
            }
        };
        this.mContext = context;
        this.mMetricsLogger = metricsLogger;
        this.mUiEventLogger = uiEventLogger;
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        LocalServices.addService(GestureLauncherService.class, this);
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 600) {
            Resources resources = this.mContext.getResources();
            if (!isGestureLauncherEnabled(resources)) {
                return;
            }
            this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
            PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
            this.mPowerManager = powerManager;
            this.mWakeLock = powerManager.newWakeLock(1, TAG);
            updateCameraRegistered();
            updateCameraDoubleTapPowerEnabled();
            updateEmergencyGestureEnabled();
            updateEmergencyGesturePowerButtonCooldownPeriodMs();
            this.mUserId = ActivityManager.getCurrentUser();
            this.mContext.registerReceiver(this.mUserReceiver, new IntentFilter("android.intent.action.USER_SWITCHED"));
            registerContentObservers();
            this.mHasFeatureWatch = this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.watch");
            this.mVibrateMilliSecondsForPanicGesture = resources.getInteger(17694867);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerContentObservers() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("camera_gesture_disabled"), false, this.mSettingObserver, this.mUserId);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("camera_double_tap_power_gesture_disabled"), false, this.mSettingObserver, this.mUserId);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("camera_lift_trigger_enabled"), false, this.mSettingObserver, this.mUserId);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("emergency_gesture_enabled"), false, this.mSettingObserver, this.mUserId);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("emergency_gesture_power_button_cooldown_period_ms"), false, this.mSettingObserver, this.mUserId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCameraRegistered() {
        Resources resources = this.mContext.getResources();
        if (isCameraLaunchSettingEnabled(this.mContext, this.mUserId)) {
            registerCameraLaunchGesture(resources);
        } else {
            unregisterCameraLaunchGesture();
        }
        if (isCameraLiftTriggerSettingEnabled(this.mContext, this.mUserId)) {
            registerCameraLiftTrigger(resources);
        } else {
            unregisterCameraLiftTrigger();
        }
    }

    void updateCameraDoubleTapPowerEnabled() {
        boolean enabled = isCameraDoubleTapPowerSettingEnabled(this.mContext, this.mUserId);
        synchronized (this) {
            this.mCameraDoubleTapPowerEnabled = enabled;
        }
    }

    void updateEmergencyGestureEnabled() {
        boolean enabled = isEmergencyGestureSettingEnabled(this.mContext, this.mUserId);
        synchronized (this) {
            this.mEmergencyGestureEnabled = enabled;
        }
    }

    void updateEmergencyGesturePowerButtonCooldownPeriodMs() {
        int cooldownPeriodMs = getEmergencyGesturePowerButtonCooldownPeriodMs(this.mContext, this.mUserId);
        synchronized (this) {
            this.mEmergencyGesturePowerButtonCooldownPeriodMs = cooldownPeriodMs;
        }
    }

    private void unregisterCameraLaunchGesture() {
        if (this.mCameraLaunchRegistered) {
            this.mCameraLaunchRegistered = false;
            this.mCameraGestureOnTimeMs = 0L;
            this.mCameraGestureLastEventTime = 0L;
            this.mCameraGestureSensor1LastOnTimeMs = 0L;
            this.mCameraGestureSensor2LastOnTimeMs = 0L;
            this.mCameraLaunchLastEventExtra = 0;
            SensorManager sensorManager = (SensorManager) this.mContext.getSystemService("sensor");
            sensorManager.unregisterListener(this.mGestureListener);
        }
    }

    private void registerCameraLaunchGesture(Resources resources) {
        if (this.mCameraLaunchRegistered) {
            return;
        }
        long elapsedRealtime = SystemClock.elapsedRealtime();
        this.mCameraGestureOnTimeMs = elapsedRealtime;
        this.mCameraGestureLastEventTime = elapsedRealtime;
        SensorManager sensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        int cameraLaunchGestureId = resources.getInteger(17694764);
        if (cameraLaunchGestureId != -1) {
            this.mCameraLaunchRegistered = false;
            String sensorName = resources.getString(17039896);
            Sensor defaultSensor = sensorManager.getDefaultSensor(cameraLaunchGestureId, true);
            this.mCameraLaunchSensor = defaultSensor;
            if (defaultSensor != null) {
                if (sensorName.equals(defaultSensor.getStringType())) {
                    this.mCameraLaunchRegistered = sensorManager.registerListener(this.mGestureListener, this.mCameraLaunchSensor, 0);
                } else {
                    String message = String.format("Wrong configuration. Sensor type and sensor string type don't match: %s in resources, %s in the sensor.", sensorName, this.mCameraLaunchSensor.getStringType());
                    throw new RuntimeException(message);
                }
            }
        }
    }

    private void unregisterCameraLiftTrigger() {
        if (this.mCameraLiftRegistered) {
            this.mCameraLiftRegistered = false;
            SensorManager sensorManager = (SensorManager) this.mContext.getSystemService("sensor");
            sensorManager.cancelTriggerSensor(this.mCameraLiftTriggerListener, this.mCameraLiftTriggerSensor);
        }
    }

    private void registerCameraLiftTrigger(Resources resources) {
        if (this.mCameraLiftRegistered) {
            return;
        }
        SensorManager sensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        int cameraLiftTriggerId = resources.getInteger(17694765);
        if (cameraLiftTriggerId != -1) {
            this.mCameraLiftRegistered = false;
            String sensorName = resources.getString(17039897);
            Sensor defaultSensor = sensorManager.getDefaultSensor(cameraLiftTriggerId, true);
            this.mCameraLiftTriggerSensor = defaultSensor;
            if (defaultSensor != null) {
                if (sensorName.equals(defaultSensor.getStringType())) {
                    this.mCameraLiftRegistered = sensorManager.requestTriggerSensor(this.mCameraLiftTriggerListener, this.mCameraLiftTriggerSensor);
                } else {
                    String message = String.format("Wrong configuration. Sensor type and sensor string type don't match: %s in resources, %s in the sensor.", sensorName, this.mCameraLiftTriggerSensor.getStringType());
                    throw new RuntimeException(message);
                }
            }
        }
    }

    public static boolean isCameraLaunchSettingEnabled(Context context, int userId) {
        return isCameraLaunchEnabled(context.getResources()) && Settings.Secure.getIntForUser(context.getContentResolver(), "camera_gesture_disabled", 0, userId) == 0;
    }

    public static boolean isCameraDoubleTapPowerSettingEnabled(Context context, int userId) {
        return isCameraDoubleTapPowerEnabled(context.getResources()) && Settings.Secure.getIntForUser(context.getContentResolver(), "camera_double_tap_power_gesture_disabled", 0, userId) == 0;
    }

    public static boolean isCameraLiftTriggerSettingEnabled(Context context, int userId) {
        return isCameraLiftTriggerEnabled(context.getResources()) && Settings.Secure.getIntForUser(context.getContentResolver(), "camera_lift_trigger_enabled", 1, userId) != 0;
    }

    public static boolean isEmergencyGestureSettingEnabled(Context context, int userId) {
        return isEmergencyGestureEnabled(context.getResources()) && Settings.Secure.getIntForUser(context.getContentResolver(), "emergency_gesture_enabled", 1, userId) != 0;
    }

    static int getEmergencyGesturePowerButtonCooldownPeriodMs(Context context, int userId) {
        int cooldown = Settings.Global.getInt(context.getContentResolver(), "emergency_gesture_power_button_cooldown_period_ms", EMERGENCY_GESTURE_POWER_BUTTON_COOLDOWN_PERIOD_MS_DEFAULT);
        return Math.min(cooldown, 5000);
    }

    private static boolean isCameraLaunchEnabled(Resources resources) {
        boolean configSet = resources.getInteger(17694764) != -1;
        return configSet && !SystemProperties.getBoolean("gesture.disable_camera_launch", false);
    }

    static boolean isCameraDoubleTapPowerEnabled(Resources resources) {
        return resources.getBoolean(17891398);
    }

    private static boolean isCameraLiftTriggerEnabled(Resources resources) {
        return resources.getInteger(17694765) != -1;
    }

    private static boolean isEmergencyGestureEnabled(Resources resources) {
        return resources.getBoolean(17891623);
    }

    public static boolean isGestureLauncherEnabled(Resources resources) {
        return isCameraLaunchEnabled(resources) || isCameraDoubleTapPowerEnabled(resources) || isCameraLiftTriggerEnabled(resources) || isEmergencyGestureEnabled(resources);
    }

    public boolean interceptVolumeKeyDown(KeyEvent event, boolean interactive, MutableBoolean outLaunched) {
        long doubleTapInterval;
        boolean launched = false;
        boolean intercept = false;
        synchronized (this) {
            doubleTapInterval = event.getEventTime() - this.mLastVolumeDown;
            if (doubleTapInterval < 300) {
                launched = true;
                intercept = interactive;
            }
            this.mLastVolumeDown = event.getEventTime();
        }
        if (launched && TRAN_CAM_QUICKUP_SUPPORT) {
            Slog.i(TAG, "Volume button double tap gesture detected, launching camera. Interval=" + doubleTapInterval + "ms");
            launched = handleCameraGesture(false, 3);
        }
        outLaunched.value = launched;
        return intercept && launched && TRAN_CAM_QUICKUP_SUPPORT;
    }

    public boolean interceptPowerKeyDown(KeyEvent event, boolean interactive, MutableBoolean outLaunched) {
        long powerTapInterval;
        boolean z;
        if (this.mEmergencyGestureEnabled && this.mEmergencyGesturePowerButtonCooldownPeriodMs >= 0) {
            int i = this.mEmergencyGesturePowerButtonCooldownPeriodMs;
            if (event.getEventTime() - this.mLastEmergencyGestureTriggered < i) {
                Slog.i(TAG, String.format("Suppressing power button: within %dms cooldown period after Emergency Gesture. Begin=%dms, end=%dms.", Integer.valueOf(i), Long.valueOf(this.mLastEmergencyGestureTriggered), Long.valueOf(this.mLastEmergencyGestureTriggered + this.mEmergencyGesturePowerButtonCooldownPeriodMs)));
                outLaunched.value = false;
                return true;
            }
        }
        if (event.isLongPress()) {
            outLaunched.value = false;
            return false;
        }
        boolean launchCamera = false;
        boolean launchEmergencyGesture = false;
        boolean intercept = false;
        synchronized (this) {
            powerTapInterval = event.getEventTime() - this.mLastPowerDown;
            this.mLastPowerDown = event.getEventTime();
            if (powerTapInterval >= 500) {
                this.mFirstPowerDown = event.getEventTime();
                this.mPowerButtonConsecutiveTaps = 1;
                this.mPowerButtonSlowConsecutiveTaps = 1;
            } else if (powerTapInterval >= 300) {
                this.mFirstPowerDown = event.getEventTime();
                this.mPowerButtonConsecutiveTaps = 1;
                this.mPowerButtonSlowConsecutiveTaps++;
            } else {
                this.mPowerButtonConsecutiveTaps++;
                this.mPowerButtonSlowConsecutiveTaps++;
            }
            if (this.mEmergencyGestureEnabled) {
                int i2 = this.mPowerButtonConsecutiveTaps;
                if (i2 > (this.mHasFeatureWatch ? 5 : 1)) {
                    intercept = interactive;
                }
                if (i2 == 5) {
                    long emergencyGestureSpentTime = event.getEventTime() - this.mFirstPowerDown;
                    long emergencyGestureTapDetectionMinTimeMs = Settings.Global.getInt(this.mContext.getContentResolver(), "emergency_gesture_tap_detection_min_time_ms", 160);
                    if (emergencyGestureSpentTime <= emergencyGestureTapDetectionMinTimeMs) {
                        Slog.i(TAG, "Emergency gesture detected but it's too fast. Gesture time: " + emergencyGestureSpentTime + " ms");
                        this.mFirstPowerDown = event.getEventTime();
                        this.mPowerButtonConsecutiveTaps = 1;
                        this.mPowerButtonSlowConsecutiveTaps = 1;
                    } else {
                        Slog.i(TAG, "Emergency gesture detected. Gesture time: " + emergencyGestureSpentTime + " ms");
                        launchEmergencyGesture = true;
                        this.mMetricsLogger.histogram("emergency_gesture_spent_time", (int) emergencyGestureSpentTime);
                    }
                }
            }
            if (this.mCameraDoubleTapPowerEnabled && powerTapInterval < 300 && this.mPowerButtonConsecutiveTaps == 2) {
                launchCamera = true;
                intercept = interactive;
            }
        }
        if (this.mPowerButtonConsecutiveTaps > 1 || this.mPowerButtonSlowConsecutiveTaps > 1) {
            Slog.i(TAG, Long.valueOf(this.mPowerButtonConsecutiveTaps) + " consecutive power button taps detected, " + Long.valueOf(this.mPowerButtonSlowConsecutiveTaps) + " consecutive slow power button taps detected");
        }
        if (launchCamera) {
            Slog.i(TAG, "Power button double tap gesture detected, launching camera. Interval=" + powerTapInterval + "ms");
            z = false;
            launchCamera = handleCameraGesture(false, 1);
            if (launchCamera) {
                this.mMetricsLogger.action(255, (int) powerTapInterval);
                this.mUiEventLogger.log(GestureLauncherEvent.GESTURE_CAMERA_DOUBLE_TAP_POWER);
            }
        } else {
            z = false;
            if (launchEmergencyGesture) {
                Slog.i(TAG, "Emergency gesture detected, launching.");
                launchEmergencyGesture = handleEmergencyGesture();
                this.mUiEventLogger.log(GestureLauncherEvent.GESTURE_EMERGENCY_TAP_POWER);
                if (launchEmergencyGesture) {
                    synchronized (this) {
                        this.mLastEmergencyGestureTriggered = event.getEventTime();
                    }
                }
            }
        }
        this.mMetricsLogger.histogram("power_consecutive_short_tap_count", this.mPowerButtonSlowConsecutiveTaps);
        this.mMetricsLogger.histogram("power_double_tap_interval", (int) powerTapInterval);
        outLaunched.value = (launchCamera || launchEmergencyGesture) ? true : z;
        if (intercept && isUserSetupComplete()) {
            return true;
        }
        return z;
    }

    boolean handleCameraGesture(boolean useWakelock, int source) {
        Trace.traceBegin(64L, "GestureLauncher:handleCameraGesture");
        try {
            boolean userSetupComplete = isUserSetupComplete();
            if (userSetupComplete) {
                if (useWakelock) {
                    this.mWakeLock.acquire(500L);
                }
                StatusBarManagerInternal service = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
                service.onCameraLaunchGestureDetected(source);
                return true;
            }
            return false;
        } finally {
            Trace.traceEnd(64L);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [756=4] */
    boolean handleEmergencyGesture() {
        Trace.traceBegin(64L, "GestureLauncher:handleEmergencyGesture");
        try {
            boolean userSetupComplete = isUserSetupComplete();
            if (!userSetupComplete) {
                return false;
            } else if (this.mHasFeatureWatch) {
                onEmergencyGestureDetectedOnWatch();
                return true;
            } else {
                StatusBarManagerInternal service = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
                service.onEmergencyActionLaunchGestureDetected();
                return true;
            }
        } finally {
            Trace.traceEnd(64L);
        }
    }

    private void onEmergencyGestureDetectedOnWatch() {
        String str;
        if (isInRetailMode()) {
            str = WEAR_LAUNCH_EMERGENCY_RETAIL_ACTION;
        } else {
            str = WEAR_LAUNCH_EMERGENCY_ACTION;
        }
        Intent emergencyIntent = new Intent(str);
        PackageManager pm = this.mContext.getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(emergencyIntent, 0);
        if (resolveInfo == null) {
            Slog.w(TAG, "Couldn't find an app to process the emergency intent " + emergencyIntent.getAction());
            return;
        }
        Vibrator vibrator = (Vibrator) this.mContext.getSystemService(Vibrator.class);
        vibrator.vibrate(VibrationEffect.createOneShot(this.mVibrateMilliSecondsForPanicGesture, -1));
        emergencyIntent.setComponent(new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
        emergencyIntent.setFlags(268435456);
        emergencyIntent.putExtra(EXTRA_LAUNCH_EMERGENCY_VIA_GESTURE, true);
        this.mContext.startActivityAsUser(emergencyIntent, new UserHandle(this.mUserId));
    }

    private boolean isInRetailMode() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "device_demo_mode", 0) == 1;
    }

    private boolean isUserSetupComplete() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class GestureEventListener implements SensorEventListener {
        private GestureEventListener() {
        }

        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            if (GestureLauncherService.this.mCameraLaunchRegistered && event.sensor == GestureLauncherService.this.mCameraLaunchSensor && GestureLauncherService.this.handleCameraGesture(true, 0)) {
                GestureLauncherService.this.mMetricsLogger.action(256);
                GestureLauncherService.this.mUiEventLogger.log(GestureLauncherEvent.GESTURE_CAMERA_WIGGLE);
                trackCameraLaunchEvent(event);
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        private void trackCameraLaunchEvent(SensorEvent event) {
            long now = SystemClock.elapsedRealtime();
            long totalDuration = now - GestureLauncherService.this.mCameraGestureOnTimeMs;
            float[] values = event.values;
            long sensor1OnTime = (long) (totalDuration * values[0]);
            long sensor2OnTime = (long) (totalDuration * values[1]);
            int extra = (int) values[2];
            long gestureOnTimeDiff = now - GestureLauncherService.this.mCameraGestureLastEventTime;
            long sensor1OnTimeDiff = sensor1OnTime - GestureLauncherService.this.mCameraGestureSensor1LastOnTimeMs;
            long sensor2OnTimeDiff = sensor2OnTime - GestureLauncherService.this.mCameraGestureSensor2LastOnTimeMs;
            int extraDiff = extra - GestureLauncherService.this.mCameraLaunchLastEventExtra;
            if (gestureOnTimeDiff < 0 || sensor1OnTimeDiff < 0 || sensor2OnTimeDiff < 0) {
                return;
            }
            EventLogTags.writeCameraGestureTriggered(gestureOnTimeDiff, sensor1OnTimeDiff, sensor2OnTimeDiff, extraDiff);
            GestureLauncherService.this.mCameraGestureLastEventTime = now;
            GestureLauncherService.this.mCameraGestureSensor1LastOnTimeMs = sensor1OnTime;
            GestureLauncherService.this.mCameraGestureSensor2LastOnTimeMs = sensor2OnTime;
            GestureLauncherService.this.mCameraLaunchLastEventExtra = extra;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class CameraLiftTriggerEventListener extends TriggerEventListener {
        private CameraLiftTriggerEventListener() {
        }

        @Override // android.hardware.TriggerEventListener
        public void onTrigger(TriggerEvent event) {
            if (GestureLauncherService.this.mCameraLiftRegistered && event.sensor == GestureLauncherService.this.mCameraLiftTriggerSensor) {
                GestureLauncherService.this.mContext.getResources();
                SensorManager sensorManager = (SensorManager) GestureLauncherService.this.mContext.getSystemService("sensor");
                boolean keyguardShowingAndNotOccluded = GestureLauncherService.this.mWindowManagerInternal.isKeyguardShowingAndNotOccluded();
                boolean interactive = GestureLauncherService.this.mPowerManager.isInteractive();
                if ((keyguardShowingAndNotOccluded || !interactive) && GestureLauncherService.this.handleCameraGesture(true, 2)) {
                    MetricsLogger.action(GestureLauncherService.this.mContext, 989);
                    GestureLauncherService.this.mUiEventLogger.log(GestureLauncherEvent.GESTURE_CAMERA_LIFT);
                }
                GestureLauncherService gestureLauncherService = GestureLauncherService.this;
                gestureLauncherService.mCameraLiftRegistered = sensorManager.requestTriggerSensor(gestureLauncherService.mCameraLiftTriggerListener, GestureLauncherService.this.mCameraLiftTriggerSensor);
            }
        }
    }
}
