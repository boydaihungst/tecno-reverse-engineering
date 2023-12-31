package com.android.server;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Slog;
/* loaded from: classes.dex */
public class AnyMotionDetector {
    private static final long ACCELEROMETER_DATA_TIMEOUT_MILLIS = 3000;
    private static final boolean DEBUG = false;
    private static final long ORIENTATION_MEASUREMENT_DURATION_MILLIS = 2500;
    private static final long ORIENTATION_MEASUREMENT_INTERVAL_MILLIS = 5000;
    public static final int RESULT_MOVED = 1;
    public static final int RESULT_STATIONARY = 0;
    public static final int RESULT_UNKNOWN = -1;
    private static final int SAMPLING_INTERVAL_MILLIS = 40;
    private static final int STALE_MEASUREMENT_TIMEOUT_MILLIS = 120000;
    private static final int STATE_ACTIVE = 1;
    private static final int STATE_INACTIVE = 0;
    private static final String TAG = "AnyMotionDetector";
    private static final long WAKELOCK_TIMEOUT_MILLIS = 30000;
    private final float THRESHOLD_ENERGY = 5.0f;
    private final Sensor mAccelSensor;
    private final DeviceIdleCallback mCallback;
    private Vector3 mCurrentGravityVector;
    private final Handler mHandler;
    private final SensorEventListener mListener;
    private final Object mLock;
    private boolean mMeasurementInProgress;
    private final Runnable mMeasurementTimeout;
    private boolean mMeasurementTimeoutIsActive;
    private int mNumSufficientSamples;
    private Vector3 mPreviousGravityVector;
    private final RunningSignalStats mRunningStats;
    private final SensorManager mSensorManager;
    private final Runnable mSensorRestart;
    private boolean mSensorRestartIsActive;
    private int mState;
    private final float mThresholdAngle;
    private final PowerManager.WakeLock mWakeLock;
    private final Runnable mWakelockTimeout;
    private volatile boolean mWakelockTimeoutIsActive;

    /* loaded from: classes.dex */
    interface DeviceIdleCallback {
        void onAnyMotionResult(int i);
    }

    public AnyMotionDetector(PowerManager pm, Handler handler, SensorManager sm, DeviceIdleCallback callback, float thresholdAngle) {
        Object obj = new Object();
        this.mLock = obj;
        this.mCurrentGravityVector = null;
        this.mPreviousGravityVector = null;
        this.mListener = new SensorEventListener() { // from class: com.android.server.AnyMotionDetector.1
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                int status = -1;
                synchronized (AnyMotionDetector.this.mLock) {
                    Vector3 accelDatum = new Vector3(SystemClock.elapsedRealtime(), event.values[0], event.values[1], event.values[2]);
                    AnyMotionDetector.this.mRunningStats.accumulate(accelDatum);
                    if (AnyMotionDetector.this.mRunningStats.getSampleCount() >= AnyMotionDetector.this.mNumSufficientSamples) {
                        status = AnyMotionDetector.this.stopOrientationMeasurementLocked();
                    }
                }
                if (status != -1) {
                    AnyMotionDetector.this.mHandler.removeCallbacks(AnyMotionDetector.this.mWakelockTimeout);
                    AnyMotionDetector.this.mWakelockTimeoutIsActive = false;
                    AnyMotionDetector.this.mCallback.onAnyMotionResult(status);
                }
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mSensorRestart = new Runnable() { // from class: com.android.server.AnyMotionDetector.2
            @Override // java.lang.Runnable
            public void run() {
                synchronized (AnyMotionDetector.this.mLock) {
                    if (AnyMotionDetector.this.mSensorRestartIsActive) {
                        AnyMotionDetector.this.mSensorRestartIsActive = false;
                        AnyMotionDetector.this.startOrientationMeasurementLocked();
                    }
                }
            }
        };
        this.mMeasurementTimeout = new Runnable() { // from class: com.android.server.AnyMotionDetector.3
            @Override // java.lang.Runnable
            public void run() {
                int status = -1;
                synchronized (AnyMotionDetector.this.mLock) {
                    if (AnyMotionDetector.this.mMeasurementTimeoutIsActive) {
                        AnyMotionDetector.this.mMeasurementTimeoutIsActive = false;
                        status = AnyMotionDetector.this.stopOrientationMeasurementLocked();
                    }
                }
                if (status != -1) {
                    AnyMotionDetector.this.mHandler.removeCallbacks(AnyMotionDetector.this.mWakelockTimeout);
                    AnyMotionDetector.this.mWakelockTimeoutIsActive = false;
                    AnyMotionDetector.this.mCallback.onAnyMotionResult(status);
                }
            }
        };
        this.mWakelockTimeout = new Runnable() { // from class: com.android.server.AnyMotionDetector.4
            @Override // java.lang.Runnable
            public void run() {
                synchronized (AnyMotionDetector.this.mLock) {
                    if (AnyMotionDetector.this.mWakelockTimeoutIsActive) {
                        AnyMotionDetector.this.mWakelockTimeoutIsActive = false;
                        AnyMotionDetector.this.stop();
                    }
                }
            }
        };
        synchronized (obj) {
            PowerManager.WakeLock newWakeLock = pm.newWakeLock(1, TAG);
            this.mWakeLock = newWakeLock;
            newWakeLock.setReferenceCounted(false);
            this.mHandler = handler;
            this.mSensorManager = sm;
            this.mAccelSensor = sm.getDefaultSensor(1);
            this.mMeasurementInProgress = false;
            this.mMeasurementTimeoutIsActive = false;
            this.mWakelockTimeoutIsActive = false;
            this.mSensorRestartIsActive = false;
            this.mState = 0;
            this.mCallback = callback;
            this.mThresholdAngle = thresholdAngle;
            this.mRunningStats = new RunningSignalStats();
            this.mNumSufficientSamples = (int) Math.ceil(62.5d);
        }
    }

    public boolean hasSensor() {
        return this.mAccelSensor != null;
    }

    public void checkForAnyMotion() {
        synchronized (this.mLock) {
            if (this.mState != 1) {
                this.mState = 1;
                this.mCurrentGravityVector = null;
                this.mPreviousGravityVector = null;
                this.mWakeLock.acquire();
                Message wakelockTimeoutMsg = Message.obtain(this.mHandler, this.mWakelockTimeout);
                this.mHandler.sendMessageDelayed(wakelockTimeoutMsg, 30000L);
                this.mWakelockTimeoutIsActive = true;
                startOrientationMeasurementLocked();
            }
        }
    }

    public void stop() {
        synchronized (this.mLock) {
            if (this.mState == 1) {
                this.mState = 0;
            }
            this.mHandler.removeCallbacks(this.mMeasurementTimeout);
            this.mHandler.removeCallbacks(this.mSensorRestart);
            this.mMeasurementTimeoutIsActive = false;
            this.mSensorRestartIsActive = false;
            if (this.mMeasurementInProgress) {
                this.mMeasurementInProgress = false;
                this.mSensorManager.unregisterListener(this.mListener);
            }
            this.mCurrentGravityVector = null;
            this.mPreviousGravityVector = null;
            if (this.mWakeLock.isHeld()) {
                this.mHandler.removeCallbacks(this.mWakelockTimeout);
                this.mWakelockTimeoutIsActive = false;
                this.mWakeLock.release();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startOrientationMeasurementLocked() {
        Sensor sensor;
        if (!this.mMeasurementInProgress && (sensor = this.mAccelSensor) != null) {
            if (this.mSensorManager.registerListener(this.mListener, sensor, EventLogTags.VOLUME_CHANGED)) {
                this.mMeasurementInProgress = true;
                this.mRunningStats.reset();
            }
            Message measurementTimeoutMsg = Message.obtain(this.mHandler, this.mMeasurementTimeout);
            this.mHandler.sendMessageDelayed(measurementTimeoutMsg, 3000L);
            this.mMeasurementTimeoutIsActive = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int stopOrientationMeasurementLocked() {
        int status = -1;
        if (this.mMeasurementInProgress) {
            this.mHandler.removeCallbacks(this.mMeasurementTimeout);
            this.mMeasurementTimeoutIsActive = false;
            this.mSensorManager.unregisterListener(this.mListener);
            this.mMeasurementInProgress = false;
            this.mPreviousGravityVector = this.mCurrentGravityVector;
            this.mCurrentGravityVector = this.mRunningStats.getRunningAverage();
            if (this.mRunningStats.getSampleCount() == 0) {
                Slog.w(TAG, "No accelerometer data acquired for orientation measurement.");
            }
            status = getStationaryStatusLocked();
            this.mRunningStats.reset();
            if (status != -1) {
                if (this.mWakeLock.isHeld()) {
                    this.mHandler.removeCallbacks(this.mWakelockTimeout);
                    this.mWakelockTimeoutIsActive = false;
                    this.mWakeLock.release();
                }
                this.mState = 0;
            } else {
                Message msg = Message.obtain(this.mHandler, this.mSensorRestart);
                this.mHandler.sendMessageDelayed(msg, ORIENTATION_MEASUREMENT_INTERVAL_MILLIS);
                this.mSensorRestartIsActive = true;
            }
        }
        return status;
    }

    private int getStationaryStatusLocked() {
        Vector3 vector3 = this.mPreviousGravityVector;
        if (vector3 == null || this.mCurrentGravityVector == null) {
            return -1;
        }
        Vector3 previousGravityVectorNormalized = vector3.normalized();
        Vector3 currentGravityVectorNormalized = this.mCurrentGravityVector.normalized();
        float angle = previousGravityVectorNormalized.angleBetween(currentGravityVectorNormalized);
        if (angle < this.mThresholdAngle && this.mRunningStats.getEnergy() < 5.0f) {
            return 0;
        }
        if (Float.isNaN(angle)) {
            return 1;
        }
        long diffTime = this.mCurrentGravityVector.timeMillisSinceBoot - this.mPreviousGravityVector.timeMillisSinceBoot;
        return diffTime > 120000 ? -1 : 1;
    }

    /* loaded from: classes.dex */
    public static final class Vector3 {
        public long timeMillisSinceBoot;
        public float x;
        public float y;
        public float z;

        public Vector3(long timeMillisSinceBoot, float x, float y, float z) {
            this.timeMillisSinceBoot = timeMillisSinceBoot;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public float norm() {
            return (float) Math.sqrt(dotProduct(this));
        }

        public Vector3 normalized() {
            float mag = norm();
            return new Vector3(this.timeMillisSinceBoot, this.x / mag, this.y / mag, this.z / mag);
        }

        public float angleBetween(Vector3 other) {
            Vector3 crossVector = cross(other);
            float degrees = Math.abs((float) Math.toDegrees(Math.atan2(crossVector.norm(), dotProduct(other))));
            Slog.d(AnyMotionDetector.TAG, "angleBetween: this = " + toString() + ", other = " + other.toString() + ", degrees = " + degrees);
            return degrees;
        }

        public Vector3 cross(Vector3 v) {
            long j = v.timeMillisSinceBoot;
            float f = this.y;
            float f2 = v.z;
            float f3 = this.z;
            float f4 = v.y;
            float f5 = (f * f2) - (f3 * f4);
            float f6 = v.x;
            float f7 = this.x;
            return new Vector3(j, f5, (f3 * f6) - (f2 * f7), (f7 * f4) - (f * f6));
        }

        public String toString() {
            String msg = "timeMillisSinceBoot=" + this.timeMillisSinceBoot;
            return ((msg + " | x=" + this.x) + ", y=" + this.y) + ", z=" + this.z;
        }

        public float dotProduct(Vector3 v) {
            return (this.x * v.x) + (this.y * v.y) + (this.z * v.z);
        }

        public Vector3 times(float val) {
            return new Vector3(this.timeMillisSinceBoot, this.x * val, this.y * val, this.z * val);
        }

        public Vector3 plus(Vector3 v) {
            return new Vector3(v.timeMillisSinceBoot, v.x + this.x, v.y + this.y, v.z + this.z);
        }

        public Vector3 minus(Vector3 v) {
            return new Vector3(v.timeMillisSinceBoot, this.x - v.x, this.y - v.y, this.z - v.z);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class RunningSignalStats {
        Vector3 currentVector;
        float energy;
        Vector3 previousVector;
        Vector3 runningSum;
        int sampleCount;

        public RunningSignalStats() {
            reset();
        }

        public void reset() {
            this.previousVector = null;
            this.currentVector = null;
            this.runningSum = new Vector3(0L, 0.0f, 0.0f, 0.0f);
            this.energy = 0.0f;
            this.sampleCount = 0;
        }

        public void accumulate(Vector3 v) {
            if (v == null) {
                return;
            }
            this.sampleCount++;
            this.runningSum = this.runningSum.plus(v);
            Vector3 vector3 = this.currentVector;
            this.previousVector = vector3;
            this.currentVector = v;
            if (vector3 != null) {
                Vector3 dv = v.minus(vector3);
                float incrementalEnergy = (dv.x * dv.x) + (dv.y * dv.y) + (dv.z * dv.z);
                this.energy += incrementalEnergy;
            }
        }

        public Vector3 getRunningAverage() {
            int i = this.sampleCount;
            if (i > 0) {
                return this.runningSum.times(1.0f / i);
            }
            return null;
        }

        public float getEnergy() {
            return this.energy;
        }

        public int getSampleCount() {
            return this.sampleCount;
        }

        public String toString() {
            Vector3 vector3 = this.currentVector;
            String currentVectorString = vector3 == null ? "null" : vector3.toString();
            Vector3 vector32 = this.previousVector;
            String previousVectorString = vector32 != null ? vector32.toString() : "null";
            String msg = "previousVector = " + previousVectorString;
            return ((msg + ", currentVector = " + currentVectorString) + ", sampleCount = " + this.sampleCount) + ", energy = " + this.energy;
        }
    }
}
