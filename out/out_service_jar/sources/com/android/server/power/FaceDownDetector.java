package com.android.server.power;

import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.DeviceConfig;
import android.util.Slog;
import com.android.internal.util.FrameworkStatsLog;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public class FaceDownDetector implements SensorEventListener {
    private static final boolean DEBUG = false;
    static final float DEFAULT_ACCELERATION_THRESHOLD = 0.2f;
    private static final boolean DEFAULT_FEATURE_ENABLED = true;
    private static final long DEFAULT_INTERACTION_BACKOFF = 60000;
    static final long DEFAULT_TIME_THRESHOLD_MILLIS = 1000;
    static final float DEFAULT_Z_ACCELERATION_THRESHOLD = -9.5f;
    static final String KEY_ACCELERATION_THRESHOLD = "acceleration_threshold";
    static final String KEY_FEATURE_ENABLED = "enable_flip_to_screen_off";
    private static final String KEY_INTERACTION_BACKOFF = "face_down_interaction_backoff_millis";
    static final String KEY_TIME_THRESHOLD_MILLIS = "time_threshold_millis";
    static final String KEY_Z_ACCELERATION_THRESHOLD = "z_acceleration_threshold";
    private static final float MOVING_AVERAGE_WEIGHT = 0.5f;
    private static final int SCREEN_OFF_RESULT = 4;
    private static final String TAG = "FaceDownDetector";
    private static final int UNFLIP = 2;
    private static final int UNKNOWN = 1;
    private static final int USER_INTERACTION = 3;
    private float mAccelerationThreshold;
    private Sensor mAccelerometer;
    private Context mContext;
    private boolean mIsEnabled;
    private final Consumer<Boolean> mOnFlip;
    private SensorManager mSensorManager;
    private int mSensorMaxLatencyMicros;
    private Duration mTimeThreshold;
    private long mUserInteractionBackoffMillis;
    private float mZAccelerationThreshold;
    private float mZAccelerationThresholdLenient;
    private long mLastFlipTime = 0;
    public int mPreviousResultType = 1;
    public long mPreviousResultTime = 0;
    private long mMillisSaved = 0;
    private final ExponentialMovingAverage mCurrentXYAcceleration = new ExponentialMovingAverage(this, 0.5f);
    private final ExponentialMovingAverage mCurrentZAcceleration = new ExponentialMovingAverage(this, 0.5f);
    private boolean mFaceDown = false;
    private boolean mInteractive = false;
    private boolean mActive = false;
    private float mPrevAcceleration = 0.0f;
    private long mPrevAccelerationTime = 0;
    private boolean mZAccelerationIsFaceDown = false;
    private long mZAccelerationFaceDownTime = 0;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    final BroadcastReceiver mScreenReceiver = new ScreenStateReceiver();
    private final Runnable mUserActivityRunnable = new Runnable() { // from class: com.android.server.power.FaceDownDetector$$ExternalSyntheticLambda0
        @Override // java.lang.Runnable
        public final void run() {
            FaceDownDetector.this.m6034lambda$new$0$comandroidserverpowerFaceDownDetector();
        }
    };

    public FaceDownDetector(Consumer<Boolean> onFlip) {
        this.mOnFlip = (Consumer) Objects.requireNonNull(onFlip);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-power-FaceDownDetector  reason: not valid java name */
    public /* synthetic */ void m6034lambda$new$0$comandroidserverpowerFaceDownDetector() {
        if (this.mFaceDown) {
            exitFaceDown(3, SystemClock.uptimeMillis() - this.mLastFlipTime);
            updateActiveState();
        }
    }

    public void systemReady(Context context) {
        this.mContext = context;
        SensorManager sensorManager = (SensorManager) context.getSystemService(SensorManager.class);
        this.mSensorManager = sensorManager;
        this.mAccelerometer = sensorManager.getDefaultSensor(1);
        readValuesFromDeviceConfig();
        DeviceConfig.addOnPropertiesChangedListener("attention_manager_service", ActivityThread.currentApplication().getMainExecutor(), new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.power.FaceDownDetector$$ExternalSyntheticLambda1
            public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                FaceDownDetector.this.m6035lambda$systemReady$1$comandroidserverpowerFaceDownDetector(properties);
            }
        });
        updateActiveState();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$systemReady$1$com-android-server-power-FaceDownDetector  reason: not valid java name */
    public /* synthetic */ void m6035lambda$systemReady$1$comandroidserverpowerFaceDownDetector(DeviceConfig.Properties properties) {
        onDeviceConfigChange(properties.getKeyset());
    }

    private void registerScreenReceiver(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.setPriority(1000);
        context.registerReceiver(this.mScreenReceiver, intentFilter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateActiveState() {
        long currentTime = SystemClock.uptimeMillis();
        boolean shouldBeActive = true;
        boolean sawRecentInteraction = this.mPreviousResultType == 3 && currentTime - this.mPreviousResultTime < this.mUserInteractionBackoffMillis;
        boolean z = this.mInteractive;
        if (!z || !this.mIsEnabled || sawRecentInteraction) {
            shouldBeActive = false;
        }
        if (this.mActive != shouldBeActive) {
            if (shouldBeActive) {
                this.mSensorManager.registerListener(this, this.mAccelerometer, 3, this.mSensorMaxLatencyMicros);
                if (this.mPreviousResultType == 4) {
                    logScreenOff();
                }
            } else {
                if (this.mFaceDown && !z) {
                    this.mPreviousResultType = 4;
                    this.mPreviousResultTime = currentTime;
                }
                this.mSensorManager.unregisterListener(this);
                this.mFaceDown = false;
                this.mOnFlip.accept(false);
            }
            this.mActive = shouldBeActive;
        }
    }

    public void dump(PrintWriter pw) {
        pw.println("FaceDownDetector:");
        pw.println("  mFaceDown=" + this.mFaceDown);
        pw.println("  mActive=" + this.mActive);
        pw.println("  mLastFlipTime=" + this.mLastFlipTime);
        pw.println("  mSensorMaxLatencyMicros=" + this.mSensorMaxLatencyMicros);
        pw.println("  mUserInteractionBackoffMillis=" + this.mUserInteractionBackoffMillis);
        pw.println("  mPreviousResultTime=" + this.mPreviousResultTime);
        pw.println("  mPreviousResultType=" + this.mPreviousResultType);
        pw.println("  mMillisSaved=" + this.mMillisSaved);
        pw.println("  mZAccelerationThreshold=" + this.mZAccelerationThreshold);
        pw.println("  mAccelerationThreshold=" + this.mAccelerationThreshold);
        pw.println("  mTimeThreshold=" + this.mTimeThreshold);
    }

    @Override // android.hardware.SensorEventListener
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != 1 || !this.mActive || !this.mIsEnabled) {
            return;
        }
        float x = event.values[0];
        float y = event.values[1];
        this.mCurrentXYAcceleration.updateMovingAverage((x * x) + (y * y));
        this.mCurrentZAcceleration.updateMovingAverage(event.values[2]);
        long curTime = event.timestamp;
        if (Math.abs(this.mCurrentXYAcceleration.mMovingAverage - this.mPrevAcceleration) > this.mAccelerationThreshold) {
            this.mPrevAcceleration = this.mCurrentXYAcceleration.mMovingAverage;
            this.mPrevAccelerationTime = curTime;
        }
        boolean moving = curTime - this.mPrevAccelerationTime <= this.mTimeThreshold.toNanos();
        float zAccelerationThreshold = this.mFaceDown ? this.mZAccelerationThresholdLenient : this.mZAccelerationThreshold;
        boolean isCurrentlyFaceDown = this.mCurrentZAcceleration.mMovingAverage < zAccelerationThreshold;
        boolean isFaceDownForPeriod = isCurrentlyFaceDown && this.mZAccelerationIsFaceDown && curTime - this.mZAccelerationFaceDownTime > this.mTimeThreshold.toNanos();
        if (isCurrentlyFaceDown && !this.mZAccelerationIsFaceDown) {
            this.mZAccelerationFaceDownTime = curTime;
            this.mZAccelerationIsFaceDown = true;
        } else if (!isCurrentlyFaceDown) {
            this.mZAccelerationIsFaceDown = false;
        }
        if (!moving && isFaceDownForPeriod && !this.mFaceDown) {
            faceDownDetected();
        } else if (!isFaceDownForPeriod && this.mFaceDown) {
            unFlipDetected();
        }
    }

    @Override // android.hardware.SensorEventListener
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void faceDownDetected() {
        this.mLastFlipTime = SystemClock.uptimeMillis();
        this.mFaceDown = true;
        this.mOnFlip.accept(true);
    }

    private void unFlipDetected() {
        exitFaceDown(2, SystemClock.uptimeMillis() - this.mLastFlipTime);
    }

    public void userActivity(int event) {
        if (event != 5) {
            this.mHandler.post(this.mUserActivityRunnable);
        }
    }

    private void exitFaceDown(int resultType, long millisSinceFlip) {
        FrameworkStatsLog.write((int) FrameworkStatsLog.FACE_DOWN_REPORTED, resultType, millisSinceFlip, 0L, 0L);
        this.mFaceDown = false;
        this.mLastFlipTime = 0L;
        this.mPreviousResultType = resultType;
        this.mPreviousResultTime = SystemClock.uptimeMillis();
        this.mOnFlip.accept(false);
    }

    private void logScreenOff() {
        long currentTime = SystemClock.uptimeMillis();
        long j = this.mPreviousResultTime;
        FrameworkStatsLog.write((int) FrameworkStatsLog.FACE_DOWN_REPORTED, 4, j - this.mLastFlipTime, this.mMillisSaved, currentTime - j);
        this.mPreviousResultType = 1;
    }

    private boolean isEnabled() {
        return DeviceConfig.getBoolean("attention_manager_service", KEY_FEATURE_ENABLED, true) && this.mContext.getResources().getBoolean(17891665);
    }

    private float getAccelerationThreshold() {
        return getFloatFlagValue(KEY_ACCELERATION_THRESHOLD, DEFAULT_ACCELERATION_THRESHOLD, -2.0f, 2.0f);
    }

    private float getZAccelerationThreshold() {
        return getFloatFlagValue(KEY_Z_ACCELERATION_THRESHOLD, DEFAULT_Z_ACCELERATION_THRESHOLD, -15.0f, 0.0f);
    }

    private long getUserInteractionBackoffMillis() {
        return getLongFlagValue(KEY_INTERACTION_BACKOFF, 60000L, 0L, 3600000L);
    }

    private int getSensorMaxLatencyMicros() {
        return this.mContext.getResources().getInteger(17694838);
    }

    private float getFloatFlagValue(String key, float defaultValue, float min, float max) {
        float value = DeviceConfig.getFloat("attention_manager_service", key, defaultValue);
        if (value < min || value > max) {
            Slog.w(TAG, "Bad flag value supplied for: " + key);
            return defaultValue;
        }
        return value;
    }

    private long getLongFlagValue(String key, long defaultValue, long min, long max) {
        long value = DeviceConfig.getLong("attention_manager_service", key, defaultValue);
        if (value < min || value > max) {
            Slog.w(TAG, "Bad flag value supplied for: " + key);
            return defaultValue;
        }
        return value;
    }

    private Duration getTimeThreshold() {
        long millis = DeviceConfig.getLong("attention_manager_service", KEY_TIME_THRESHOLD_MILLIS, 1000L);
        if (millis < 0 || millis > 15000) {
            Slog.w(TAG, "Bad flag value supplied for: time_threshold_millis");
            return Duration.ofMillis(1000L);
        }
        return Duration.ofMillis(millis);
    }

    private void onDeviceConfigChange(Set<String> keys) {
        for (String key : keys) {
            char c = 65535;
            switch (key.hashCode()) {
                case -1974380596:
                    if (key.equals(KEY_TIME_THRESHOLD_MILLIS)) {
                        c = 2;
                        break;
                    }
                    break;
                case -1762356372:
                    if (key.equals(KEY_ACCELERATION_THRESHOLD)) {
                        c = 0;
                        break;
                    }
                    break;
                case -1566292150:
                    if (key.equals(KEY_FEATURE_ENABLED)) {
                        c = 3;
                        break;
                    }
                    break;
                case 941263057:
                    if (key.equals(KEY_Z_ACCELERATION_THRESHOLD)) {
                        c = 1;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                case 1:
                case 2:
                case 3:
                    readValuesFromDeviceConfig();
                    updateActiveState();
                    return;
                default:
                    Slog.i(TAG, "Ignoring change on " + key);
            }
        }
    }

    private void readValuesFromDeviceConfig() {
        this.mAccelerationThreshold = getAccelerationThreshold();
        float zAccelerationThreshold = getZAccelerationThreshold();
        this.mZAccelerationThreshold = zAccelerationThreshold;
        this.mZAccelerationThresholdLenient = zAccelerationThreshold + 1.0f;
        this.mTimeThreshold = getTimeThreshold();
        this.mSensorMaxLatencyMicros = getSensorMaxLatencyMicros();
        this.mUserInteractionBackoffMillis = getUserInteractionBackoffMillis();
        boolean oldEnabled = this.mIsEnabled;
        boolean isEnabled = isEnabled();
        this.mIsEnabled = isEnabled;
        if (oldEnabled != isEnabled) {
            if (!isEnabled) {
                this.mContext.unregisterReceiver(this.mScreenReceiver);
                this.mInteractive = false;
            } else {
                registerScreenReceiver(this.mContext);
                this.mInteractive = ((PowerManager) this.mContext.getSystemService(PowerManager.class)).isInteractive();
            }
        }
        Slog.i(TAG, "readValuesFromDeviceConfig():\nmAccelerationThreshold=" + this.mAccelerationThreshold + "\nmZAccelerationThreshold=" + this.mZAccelerationThreshold + "\nmTimeThreshold=" + this.mTimeThreshold + "\nmIsEnabled=" + this.mIsEnabled);
    }

    public void setMillisSaved(long millisSaved) {
        this.mMillisSaved = millisSaved;
    }

    /* loaded from: classes2.dex */
    private final class ScreenStateReceiver extends BroadcastReceiver {
        private ScreenStateReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                FaceDownDetector.this.mInteractive = false;
                FaceDownDetector.this.updateActiveState();
            } else if ("android.intent.action.SCREEN_ON".equals(intent.getAction())) {
                FaceDownDetector.this.mInteractive = true;
                FaceDownDetector.this.updateActiveState();
            }
        }
    }

    /* loaded from: classes2.dex */
    private final class ExponentialMovingAverage {
        private final float mAlpha;
        private final float mInitialAverage;
        private float mMovingAverage;

        ExponentialMovingAverage(FaceDownDetector faceDownDetector, float alpha) {
            this(alpha, 0.0f);
        }

        ExponentialMovingAverage(float alpha, float initialAverage) {
            this.mAlpha = alpha;
            this.mInitialAverage = initialAverage;
            this.mMovingAverage = initialAverage;
        }

        void updateMovingAverage(float newValue) {
            this.mMovingAverage = (this.mAlpha * (this.mMovingAverage - newValue)) + newValue;
        }

        void reset() {
            this.mMovingAverage = this.mInitialAverage;
        }
    }
}
