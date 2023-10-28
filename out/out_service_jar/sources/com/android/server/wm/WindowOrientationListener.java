package com.android.server.wm;

import android.app.ActivityThread;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.DeviceConfig;
import android.rotationresolver.RotationResolverInternal;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.Surface;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.LocalServices;
import com.android.server.wm.WindowOrientationListener;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
/* loaded from: classes2.dex */
public abstract class WindowOrientationListener {
    private static final int DEFAULT_BATCH_LATENCY = 100000;
    private static final long DEFAULT_ROTATION_MEMORIZATION_TIMEOUT_MILLIS = 3000;
    private static final long DEFAULT_ROTATION_RESOLVER_TIMEOUT_MILLIS = 700;
    private static final String KEY_ROTATION_MEMORIZATION_TIMEOUT = "rotation_memorization_timeout_millis";
    private static final String KEY_ROTATION_RESOLVER_TIMEOUT = "rotation_resolver_timeout_millis";
    private static final boolean LOG = SystemProperties.getBoolean("debug.orientation.log", false);
    private static final String TAG = "WindowOrientationListener";
    private static final boolean USE_GRAVITY_SENSOR = false;
    private final Context mContext;
    private int mCurrentRotation;
    private boolean mEnabled;
    private Handler mHandler;
    private final Object mLock;
    OrientationJudge mOrientationJudge;
    private int mRate;
    RotationResolverInternal mRotationResolverService;
    private Sensor mSensor;
    private SensorManager mSensorManager;
    private String mSensorType;

    public abstract boolean isKeyguardLocked();

    abstract boolean isRotationResolverEnabled();

    public abstract void onProposedRotationChanged(int i);

    public WindowOrientationListener(Context context, Handler handler) {
        this(context, handler, 2);
    }

    private WindowOrientationListener(Context context, Handler handler, int rate) {
        this.mCurrentRotation = -1;
        this.mLock = new Object();
        this.mContext = context;
        this.mHandler = handler;
        SensorManager sensorManager = (SensorManager) context.getSystemService("sensor");
        this.mSensorManager = sensorManager;
        this.mRate = rate;
        List<Sensor> l = sensorManager.getSensorList(27);
        Sensor wakeUpDeviceOrientationSensor = null;
        Sensor nonWakeUpDeviceOrientationSensor = null;
        for (Sensor s : l) {
            if (s.isWakeUpSensor()) {
                wakeUpDeviceOrientationSensor = s;
            } else {
                nonWakeUpDeviceOrientationSensor = s;
            }
        }
        if (wakeUpDeviceOrientationSensor != null) {
            this.mSensor = wakeUpDeviceOrientationSensor;
        } else {
            this.mSensor = nonWakeUpDeviceOrientationSensor;
        }
        if (this.mSensor != null) {
            this.mOrientationJudge = new OrientationSensorJudge();
        }
        if (this.mOrientationJudge == null) {
            Sensor defaultSensor = this.mSensorManager.getDefaultSensor(1);
            this.mSensor = defaultSensor;
            if (defaultSensor != null) {
                this.mOrientationJudge = new AccelSensorJudge(context);
            }
        }
    }

    public void enable() {
        enable(true);
    }

    public void enable(boolean clearCurrentRotation) {
        synchronized (this.mLock) {
            if (this.mSensor == null) {
                Slog.w(TAG, "Cannot detect sensors. Not enabled");
            } else if (this.mEnabled) {
            } else {
                if (LOG) {
                    Slog.d(TAG, "WindowOrientationListener enabled clearCurrentRotation=" + clearCurrentRotation);
                }
                this.mOrientationJudge.resetLocked(clearCurrentRotation);
                if (this.mSensor.getType() == 1) {
                    this.mSensorManager.registerListener(this.mOrientationJudge, this.mSensor, this.mRate, DEFAULT_BATCH_LATENCY, this.mHandler);
                } else {
                    this.mSensorManager.registerListener(this.mOrientationJudge, this.mSensor, this.mRate, this.mHandler);
                }
                this.mEnabled = true;
            }
        }
    }

    public void disable() {
        synchronized (this.mLock) {
            if (this.mSensor == null) {
                Slog.w(TAG, "Cannot detect sensors. Invalid disable");
                return;
            }
            if (this.mEnabled) {
                if (LOG) {
                    Slog.d(TAG, "WindowOrientationListener disabled");
                }
                this.mSensorManager.unregisterListener(this.mOrientationJudge);
                this.mEnabled = false;
            }
        }
    }

    public void onTouchStart() {
        synchronized (this.mLock) {
            OrientationJudge orientationJudge = this.mOrientationJudge;
            if (orientationJudge != null) {
                orientationJudge.onTouchStartLocked();
            }
        }
    }

    public void onTouchEnd() {
        long whenElapsedNanos = SystemClock.elapsedRealtimeNanos();
        synchronized (this.mLock) {
            OrientationJudge orientationJudge = this.mOrientationJudge;
            if (orientationJudge != null) {
                orientationJudge.onTouchEndLocked(whenElapsedNanos);
            }
        }
    }

    public Handler getHandler() {
        return this.mHandler;
    }

    public void setCurrentRotation(int rotation) {
        synchronized (this.mLock) {
            this.mCurrentRotation = rotation;
        }
    }

    public int getProposedRotation() {
        synchronized (this.mLock) {
            if (this.mEnabled) {
                return this.mOrientationJudge.getProposedRotationLocked();
            }
            return -1;
        }
    }

    public boolean canDetectOrientation() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mSensor != null;
        }
        return z;
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        synchronized (this.mLock) {
            proto.write(1133871366145L, this.mEnabled);
            proto.write(CompanionMessage.TYPE, this.mCurrentRotation);
        }
        proto.end(token);
    }

    public void dump(PrintWriter pw, String prefix) {
        synchronized (this.mLock) {
            pw.println(prefix + TAG);
            String prefix2 = prefix + "  ";
            pw.println(prefix2 + "mEnabled=" + this.mEnabled);
            pw.println(prefix2 + "mCurrentRotation=" + Surface.rotationToString(this.mCurrentRotation));
            pw.println(prefix2 + "mSensorType=" + this.mSensorType);
            pw.println(prefix2 + "mSensor=" + this.mSensor);
            pw.println(prefix2 + "mRate=" + this.mRate);
            OrientationJudge orientationJudge = this.mOrientationJudge;
            if (orientationJudge != null) {
                orientationJudge.dumpLocked(pw, prefix2);
            }
        }
    }

    public boolean shouldStayEnabledWhileDreaming() {
        if (this.mContext.getResources().getBoolean(17891667)) {
            return true;
        }
        return this.mSensor.getType() == 27 && this.mSensor.isWakeUpSensor();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public abstract class OrientationJudge implements SensorEventListener {
        protected static final float MILLIS_PER_NANO = 1.0E-6f;
        protected static final long NANOS_PER_MS = 1000000;
        protected static final long PROPOSAL_MIN_TIME_SINCE_TOUCH_END_NANOS = 500000000;

        public abstract void dumpLocked(PrintWriter printWriter, String str);

        public abstract int getProposedRotationLocked();

        @Override // android.hardware.SensorEventListener
        public abstract void onAccuracyChanged(Sensor sensor, int i);

        @Override // android.hardware.SensorEventListener
        public abstract void onSensorChanged(SensorEvent sensorEvent);

        public abstract void onTouchEndLocked(long j);

        public abstract void onTouchStartLocked();

        public abstract void resetLocked(boolean z);

        OrientationJudge() {
        }
    }

    /* loaded from: classes2.dex */
    final class AccelSensorJudge extends OrientationJudge {
        private static final float ACCELERATION_TOLERANCE = 4.0f;
        private static final int ACCELEROMETER_DATA_X = 0;
        private static final int ACCELEROMETER_DATA_Y = 1;
        private static final int ACCELEROMETER_DATA_Z = 2;
        private static final int ADJACENT_ORIENTATION_ANGLE_GAP = 45;
        private static final float FILTER_TIME_CONSTANT_MS = 200.0f;
        private static final float FLAT_ANGLE = 80.0f;
        private static final long FLAT_TIME_NANOS = 1000000000;
        private static final float MAX_ACCELERATION_MAGNITUDE = 13.80665f;
        private static final long MAX_FILTER_DELTA_TIME_NANOS = 1000000000;
        private static final int MAX_TILT = 80;
        private static final float MIN_ACCELERATION_MAGNITUDE = 5.80665f;
        private static final float NEAR_ZERO_MAGNITUDE = 1.0f;
        private static final long PROPOSAL_MIN_TIME_SINCE_ACCELERATION_ENDED_NANOS = 500000000;
        private static final long PROPOSAL_MIN_TIME_SINCE_FLAT_ENDED_NANOS = 500000000;
        private static final long PROPOSAL_MIN_TIME_SINCE_SWING_ENDED_NANOS = 300000000;
        private static final long PROPOSAL_SETTLE_TIME_NANOS = 40000000;
        private static final float RADIANS_TO_DEGREES = 57.29578f;
        private static final float SWING_AWAY_ANGLE_DELTA = 20.0f;
        private static final long SWING_TIME_NANOS = 300000000;
        private static final int TILT_HISTORY_SIZE = 200;
        private static final int TILT_OVERHEAD_ENTER = -40;
        private static final int TILT_OVERHEAD_EXIT = -15;
        private boolean mAccelerating;
        private long mAccelerationTimestampNanos;
        private boolean mFlat;
        private long mFlatTimestampNanos;
        private long mLastFilteredTimestampNanos;
        private float mLastFilteredX;
        private float mLastFilteredY;
        private float mLastFilteredZ;
        private boolean mOverhead;
        private int mPredictedRotation;
        private long mPredictedRotationTimestampNanos;
        private int mProposedRotation;
        private long mSwingTimestampNanos;
        private boolean mSwinging;
        private float[] mTiltHistory;
        private int mTiltHistoryIndex;
        private long[] mTiltHistoryTimestampNanos;
        private final int[][] mTiltToleranceConfig;
        private long mTouchEndedTimestampNanos;
        private boolean mTouched;

        public AccelSensorJudge(Context context) {
            super();
            this.mTiltToleranceConfig = new int[][]{new int[]{-25, 70}, new int[]{-25, 65}, new int[]{-25, 60}, new int[]{-25, 65}};
            this.mTouchEndedTimestampNanos = Long.MIN_VALUE;
            this.mTiltHistory = new float[200];
            this.mTiltHistoryTimestampNanos = new long[200];
            int[] tiltTolerance = context.getResources().getIntArray(17235994);
            if (tiltTolerance.length == 8) {
                for (int i = 0; i < 4; i++) {
                    int min = tiltTolerance[i * 2];
                    int max = tiltTolerance[(i * 2) + 1];
                    if (min >= -90 && min <= max && max <= 90) {
                        int[] iArr = this.mTiltToleranceConfig[i];
                        iArr[0] = min;
                        iArr[1] = max;
                    } else {
                        Slog.wtf(WindowOrientationListener.TAG, "config_autoRotationTiltTolerance contains invalid range: min=" + min + ", max=" + max);
                    }
                }
                return;
            }
            Slog.wtf(WindowOrientationListener.TAG, "config_autoRotationTiltTolerance should have exactly 8 elements");
        }

        @Override // com.android.server.wm.WindowOrientationListener.OrientationJudge
        public int getProposedRotationLocked() {
            return this.mProposedRotation;
        }

        @Override // com.android.server.wm.WindowOrientationListener.OrientationJudge
        public void dumpLocked(PrintWriter pw, String prefix) {
            pw.println(prefix + "AccelSensorJudge");
            String prefix2 = prefix + "  ";
            pw.println(prefix2 + "mProposedRotation=" + this.mProposedRotation);
            pw.println(prefix2 + "mPredictedRotation=" + this.mPredictedRotation);
            pw.println(prefix2 + "mLastFilteredX=" + this.mLastFilteredX);
            pw.println(prefix2 + "mLastFilteredY=" + this.mLastFilteredY);
            pw.println(prefix2 + "mLastFilteredZ=" + this.mLastFilteredZ);
            long delta = SystemClock.elapsedRealtimeNanos() - this.mLastFilteredTimestampNanos;
            pw.println(prefix2 + "mLastFilteredTimestampNanos=" + this.mLastFilteredTimestampNanos + " (" + (((float) delta) * 1.0E-6f) + "ms ago)");
            pw.println(prefix2 + "mTiltHistory={last: " + getLastTiltLocked() + "}");
            pw.println(prefix2 + "mFlat=" + this.mFlat);
            pw.println(prefix2 + "mSwinging=" + this.mSwinging);
            pw.println(prefix2 + "mAccelerating=" + this.mAccelerating);
            pw.println(prefix2 + "mOverhead=" + this.mOverhead);
            pw.println(prefix2 + "mTouched=" + this.mTouched);
            pw.print(prefix2 + "mTiltToleranceConfig=[");
            for (int i = 0; i < 4; i++) {
                if (i != 0) {
                    pw.print(", ");
                }
                pw.print("[");
                pw.print(this.mTiltToleranceConfig[i][0]);
                pw.print(", ");
                pw.print(this.mTiltToleranceConfig[i][1]);
                pw.print("]");
            }
            pw.println("]");
        }

        @Override // com.android.server.wm.WindowOrientationListener.OrientationJudge, android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        /* JADX WARN: Removed duplicated region for block: B:29:0x0118 A[Catch: all -> 0x03e4, TryCatch #0 {, blocks: (B:4:0x000b, B:6:0x0020, B:7:0x0063, B:9:0x0072, B:18:0x0088, B:20:0x00ab, B:27:0x010b, B:29:0x0118, B:31:0x0130, B:33:0x0136, B:34:0x013d, B:84:0x02bc, B:86:0x02c8, B:89:0x02d2, B:91:0x02da, B:92:0x03b1, B:88:0x02ce, B:35:0x0149, B:37:0x014f, B:38:0x0152, B:40:0x0172, B:41:0x0175, B:43:0x017c, B:46:0x0183, B:50:0x018e, B:52:0x0192, B:54:0x0198, B:55:0x01b0, B:56:0x01be, B:58:0x01c6, B:60:0x01cc, B:61:0x01e4, B:62:0x01f2, B:64:0x0207, B:65:0x0209, B:68:0x0211, B:70:0x0217, B:72:0x021d, B:74:0x0226, B:78:0x0282, B:80:0x0288, B:81:0x02aa, B:49:0x018b, B:23:0x00f9, B:25:0x00ff, B:26:0x0106), top: B:103:0x000b }] */
        /* JADX WARN: Removed duplicated region for block: B:83:0x02b3  */
        /* JADX WARN: Removed duplicated region for block: B:91:0x02da A[Catch: all -> 0x03e4, TryCatch #0 {, blocks: (B:4:0x000b, B:6:0x0020, B:7:0x0063, B:9:0x0072, B:18:0x0088, B:20:0x00ab, B:27:0x010b, B:29:0x0118, B:31:0x0130, B:33:0x0136, B:34:0x013d, B:84:0x02bc, B:86:0x02c8, B:89:0x02d2, B:91:0x02da, B:92:0x03b1, B:88:0x02ce, B:35:0x0149, B:37:0x014f, B:38:0x0152, B:40:0x0172, B:41:0x0175, B:43:0x017c, B:46:0x0183, B:50:0x018e, B:52:0x0192, B:54:0x0198, B:55:0x01b0, B:56:0x01be, B:58:0x01c6, B:60:0x01cc, B:61:0x01e4, B:62:0x01f2, B:64:0x0207, B:65:0x0209, B:68:0x0211, B:70:0x0217, B:72:0x021d, B:74:0x0226, B:78:0x0282, B:80:0x0288, B:81:0x02aa, B:49:0x018b, B:23:0x00f9, B:25:0x00ff, B:26:0x0106), top: B:103:0x000b }] */
        @Override // com.android.server.wm.WindowOrientationListener.OrientationJudge, android.hardware.SensorEventListener
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public void onSensorChanged(SensorEvent event) {
            boolean skipSample;
            int oldProposedRotation;
            int proposedRotation;
            boolean isFlat;
            boolean isSwinging;
            float z;
            synchronized (WindowOrientationListener.this.mLock) {
                float x = event.values[0];
                float y = event.values[1];
                float z2 = event.values[2];
                if (WindowOrientationListener.LOG) {
                    Slog.v(WindowOrientationListener.TAG, "Raw acceleration vector: x=" + x + ", y=" + y + ", z=" + z2 + ", magnitude=" + Math.sqrt((x * x) + (y * y) + (z2 * z2)));
                }
                long now = event.timestamp;
                long then = this.mLastFilteredTimestampNanos;
                float timeDeltaMS = ((float) (now - then)) * 1.0E-6f;
                if (now >= then && now <= 1000000000 + then && (x != 0.0f || y != 0.0f || z2 != 0.0f)) {
                    float alpha = timeDeltaMS / (FILTER_TIME_CONSTANT_MS + timeDeltaMS);
                    float f = this.mLastFilteredX;
                    x = ((x - f) * alpha) + f;
                    float f2 = this.mLastFilteredY;
                    y = ((y - f2) * alpha) + f2;
                    float f3 = this.mLastFilteredZ;
                    float z3 = ((z2 - f3) * alpha) + f3;
                    if (!WindowOrientationListener.LOG) {
                        z = z3;
                    } else {
                        z = z3;
                        Slog.v(WindowOrientationListener.TAG, "Filtered acceleration vector: x=" + x + ", y=" + y + ", z=" + z3 + ", magnitude=" + Math.sqrt((x * x) + (y * y) + (z3 * z3)));
                    }
                    skipSample = false;
                    z2 = z;
                    this.mLastFilteredTimestampNanos = now;
                    this.mLastFilteredX = x;
                    this.mLastFilteredY = y;
                    this.mLastFilteredZ = z2;
                    boolean isAccelerating = false;
                    boolean isFlat2 = false;
                    boolean isSwinging2 = false;
                    if (skipSample) {
                        float magnitude = (float) Math.sqrt((x * x) + (y * y) + (z2 * z2));
                        if (magnitude < 1.0f) {
                            if (WindowOrientationListener.LOG) {
                                Slog.v(WindowOrientationListener.TAG, "Ignoring sensor data, magnitude too close to zero.");
                            }
                            clearPredictedRotationLocked();
                        } else {
                            if (isAcceleratingLocked(magnitude)) {
                                isAccelerating = true;
                                this.mAccelerationTimestampNanos = now;
                            }
                            boolean isAccelerating2 = isAccelerating;
                            int tiltAngle = (int) Math.round(Math.asin(z2 / magnitude) * 57.295780181884766d);
                            addTiltHistoryEntryLocked(now, tiltAngle);
                            if (isFlatLocked(now)) {
                                isFlat2 = true;
                                this.mFlatTimestampNanos = now;
                            }
                            if (isSwingingLocked(now, tiltAngle)) {
                                isSwinging2 = true;
                                this.mSwingTimestampNanos = now;
                            }
                            if (tiltAngle <= TILT_OVERHEAD_ENTER) {
                                this.mOverhead = true;
                            } else if (tiltAngle >= TILT_OVERHEAD_EXIT) {
                                this.mOverhead = false;
                            }
                            if (this.mOverhead) {
                                if (WindowOrientationListener.LOG) {
                                    Slog.v(WindowOrientationListener.TAG, "Ignoring sensor data, device is overhead: tiltAngle=" + tiltAngle);
                                }
                                clearPredictedRotationLocked();
                                isFlat = isFlat2;
                                isSwinging = isSwinging2;
                            } else if (Math.abs(tiltAngle) > 80) {
                                if (WindowOrientationListener.LOG) {
                                    Slog.v(WindowOrientationListener.TAG, "Ignoring sensor data, tilt angle too high: tiltAngle=" + tiltAngle);
                                }
                                clearPredictedRotationLocked();
                                isFlat = isFlat2;
                                isSwinging = isSwinging2;
                            } else {
                                isFlat = isFlat2;
                                isSwinging = isSwinging2;
                                int orientationAngle = (int) Math.round((-Math.atan2(-x, y)) * 57.295780181884766d);
                                if (orientationAngle < 0) {
                                    orientationAngle += 360;
                                }
                                int nearestRotation = (orientationAngle + 45) / 90;
                                if (nearestRotation == 4) {
                                    nearestRotation = 0;
                                }
                                if (isTiltAngleAcceptableLocked(nearestRotation, tiltAngle) && isOrientationAngleAcceptableLocked(nearestRotation, orientationAngle)) {
                                    updatePredictedRotationLocked(now, nearestRotation);
                                    if (WindowOrientationListener.LOG) {
                                        Slog.v(WindowOrientationListener.TAG, "Predicted: tiltAngle=" + tiltAngle + ", orientationAngle=" + orientationAngle + ", predictedRotation=" + this.mPredictedRotation + ", predictedRotationAgeMS=" + (((float) (now - this.mPredictedRotationTimestampNanos)) * 1.0E-6f));
                                    }
                                }
                                if (WindowOrientationListener.LOG) {
                                    Slog.v(WindowOrientationListener.TAG, "Ignoring sensor data, no predicted rotation: tiltAngle=" + tiltAngle + ", orientationAngle=" + orientationAngle);
                                }
                                clearPredictedRotationLocked();
                            }
                            isFlat2 = isFlat;
                            isSwinging2 = isSwinging;
                            isAccelerating = isAccelerating2;
                        }
                    }
                    this.mFlat = isFlat2;
                    this.mSwinging = isSwinging2;
                    this.mAccelerating = isAccelerating;
                    oldProposedRotation = this.mProposedRotation;
                    if (this.mPredictedRotation >= 0 || isPredictedRotationAcceptableLocked(now)) {
                        this.mProposedRotation = this.mPredictedRotation;
                    }
                    proposedRotation = this.mProposedRotation;
                    if (WindowOrientationListener.LOG) {
                        Slog.v(WindowOrientationListener.TAG, "Result: currentRotation=" + WindowOrientationListener.this.mCurrentRotation + ", proposedRotation=" + proposedRotation + ", predictedRotation=" + this.mPredictedRotation + ", timeDeltaMS=" + timeDeltaMS + ", isAccelerating=" + isAccelerating + ", isFlat=" + isFlat2 + ", isSwinging=" + isSwinging2 + ", isOverhead=" + this.mOverhead + ", isTouched=" + this.mTouched + ", timeUntilSettledMS=" + remainingMS(now, this.mPredictedRotationTimestampNanos + PROPOSAL_SETTLE_TIME_NANOS) + ", timeUntilAccelerationDelayExpiredMS=" + remainingMS(now, this.mAccelerationTimestampNanos + 500000000) + ", timeUntilFlatDelayExpiredMS=" + remainingMS(now, this.mFlatTimestampNanos + 500000000) + ", timeUntilSwingDelayExpiredMS=" + remainingMS(now, this.mSwingTimestampNanos + 300000000) + ", timeUntilTouchDelayExpiredMS=" + remainingMS(now, this.mTouchEndedTimestampNanos + 500000000));
                    }
                }
                boolean skipSample2 = WindowOrientationListener.LOG;
                if (skipSample2) {
                    Slog.v(WindowOrientationListener.TAG, "Resetting orientation listener.");
                }
                resetLocked(true);
                skipSample = true;
                this.mLastFilteredTimestampNanos = now;
                this.mLastFilteredX = x;
                this.mLastFilteredY = y;
                this.mLastFilteredZ = z2;
                boolean isAccelerating3 = false;
                boolean isFlat22 = false;
                boolean isSwinging22 = false;
                if (skipSample) {
                }
                this.mFlat = isFlat22;
                this.mSwinging = isSwinging22;
                this.mAccelerating = isAccelerating3;
                oldProposedRotation = this.mProposedRotation;
                if (this.mPredictedRotation >= 0) {
                }
                this.mProposedRotation = this.mPredictedRotation;
                proposedRotation = this.mProposedRotation;
                if (WindowOrientationListener.LOG) {
                }
            }
            if (proposedRotation != oldProposedRotation && proposedRotation >= 0) {
                if (WindowOrientationListener.LOG) {
                    Slog.v(WindowOrientationListener.TAG, "Proposed rotation changed!  proposedRotation=" + proposedRotation + ", oldProposedRotation=" + oldProposedRotation);
                }
                WindowOrientationListener.this.onProposedRotationChanged(proposedRotation);
            }
        }

        @Override // com.android.server.wm.WindowOrientationListener.OrientationJudge
        public void onTouchStartLocked() {
            this.mTouched = true;
        }

        @Override // com.android.server.wm.WindowOrientationListener.OrientationJudge
        public void onTouchEndLocked(long whenElapsedNanos) {
            this.mTouched = false;
            this.mTouchEndedTimestampNanos = whenElapsedNanos;
        }

        @Override // com.android.server.wm.WindowOrientationListener.OrientationJudge
        public void resetLocked(boolean clearCurrentRotation) {
            this.mLastFilteredTimestampNanos = Long.MIN_VALUE;
            if (clearCurrentRotation) {
                this.mProposedRotation = -1;
            }
            this.mFlatTimestampNanos = Long.MIN_VALUE;
            this.mFlat = false;
            this.mSwingTimestampNanos = Long.MIN_VALUE;
            this.mSwinging = false;
            this.mAccelerationTimestampNanos = Long.MIN_VALUE;
            this.mAccelerating = false;
            this.mOverhead = false;
            clearPredictedRotationLocked();
            clearTiltHistoryLocked();
        }

        private boolean isTiltAngleAcceptableLocked(int rotation, int tiltAngle) {
            int[] iArr = this.mTiltToleranceConfig[rotation];
            return tiltAngle >= iArr[0] && tiltAngle <= iArr[1];
        }

        private boolean isOrientationAngleAcceptableLocked(int rotation, int orientationAngle) {
            int currentRotation = WindowOrientationListener.this.mCurrentRotation;
            if (currentRotation >= 0) {
                if (rotation == currentRotation || rotation == (currentRotation + 1) % 4) {
                    int lowerBound = ((rotation * 90) - 45) + 22;
                    if (rotation == 0) {
                        if (orientationAngle >= 315 && orientationAngle < lowerBound + 360) {
                            return false;
                        }
                    } else if (orientationAngle < lowerBound) {
                        return false;
                    }
                }
                if (rotation == currentRotation || rotation == (currentRotation + 3) % 4) {
                    int upperBound = ((rotation * 90) + 45) - 22;
                    return rotation == 0 ? orientationAngle > 45 || orientationAngle <= upperBound : orientationAngle <= upperBound;
                }
                return true;
            }
            return true;
        }

        private boolean isPredictedRotationAcceptableLocked(long now) {
            return now >= this.mPredictedRotationTimestampNanos + PROPOSAL_SETTLE_TIME_NANOS && now >= this.mFlatTimestampNanos + 500000000 && now >= this.mSwingTimestampNanos + 300000000 && now >= this.mAccelerationTimestampNanos + 500000000 && !this.mTouched && now >= this.mTouchEndedTimestampNanos + 500000000;
        }

        private void clearPredictedRotationLocked() {
            this.mPredictedRotation = -1;
            this.mPredictedRotationTimestampNanos = Long.MIN_VALUE;
        }

        private void updatePredictedRotationLocked(long now, int rotation) {
            if (this.mPredictedRotation != rotation) {
                this.mPredictedRotation = rotation;
                this.mPredictedRotationTimestampNanos = now;
            }
        }

        private boolean isAcceleratingLocked(float magnitude) {
            return magnitude < MIN_ACCELERATION_MAGNITUDE || magnitude > MAX_ACCELERATION_MAGNITUDE;
        }

        private void clearTiltHistoryLocked() {
            this.mTiltHistoryTimestampNanos[0] = Long.MIN_VALUE;
            this.mTiltHistoryIndex = 1;
        }

        private void addTiltHistoryEntryLocked(long now, float tilt) {
            float[] fArr = this.mTiltHistory;
            int i = this.mTiltHistoryIndex;
            fArr[i] = tilt;
            long[] jArr = this.mTiltHistoryTimestampNanos;
            jArr[i] = now;
            int i2 = (i + 1) % 200;
            this.mTiltHistoryIndex = i2;
            jArr[i2] = Long.MIN_VALUE;
        }

        private boolean isFlatLocked(long now) {
            int i = this.mTiltHistoryIndex;
            do {
                int nextTiltHistoryIndexLocked = nextTiltHistoryIndexLocked(i);
                i = nextTiltHistoryIndexLocked;
                if (nextTiltHistoryIndexLocked < 0 || this.mTiltHistory[i] < FLAT_ANGLE) {
                    return false;
                }
            } while (this.mTiltHistoryTimestampNanos[i] + 1000000000 > now);
            return true;
        }

        private boolean isSwingingLocked(long now, float tilt) {
            int i = this.mTiltHistoryIndex;
            do {
                int nextTiltHistoryIndexLocked = nextTiltHistoryIndexLocked(i);
                i = nextTiltHistoryIndexLocked;
                if (nextTiltHistoryIndexLocked < 0 || this.mTiltHistoryTimestampNanos[i] + 300000000 < now) {
                    return false;
                }
            } while (this.mTiltHistory[i] + SWING_AWAY_ANGLE_DELTA > tilt);
            return true;
        }

        private int nextTiltHistoryIndexLocked(int index) {
            int index2 = (index == 0 ? 200 : index) - 1;
            if (this.mTiltHistoryTimestampNanos[index2] != Long.MIN_VALUE) {
                return index2;
            }
            return -1;
        }

        private float getLastTiltLocked() {
            int index = nextTiltHistoryIndexLocked(this.mTiltHistoryIndex);
            if (index >= 0) {
                return this.mTiltHistory[index];
            }
            return Float.NaN;
        }

        private float remainingMS(long now, long until) {
            if (now >= until) {
                return 0.0f;
            }
            return ((float) (until - now)) * 1.0E-6f;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class OrientationSensorJudge extends OrientationJudge {
        private static final int ROTATION_UNSET = -1;
        private final ActivityTaskManagerInternal mActivityTaskManagerInternal;
        private Runnable mCancelRotationResolverRequest;
        private int mCurrentCallbackId;
        private int mDesiredRotation;
        private int mLastRotationResolution;
        private long mLastRotationResolutionTimeStamp;
        private int mProposedRotation;
        private boolean mRotationEvaluationScheduled;
        private Runnable mRotationEvaluator;
        private long mRotationMemorizationTimeoutMillis;
        private long mRotationResolverTimeoutMillis;
        private long mTouchEndedTimestampNanos;
        private boolean mTouching;

        OrientationSensorJudge() {
            super();
            this.mTouchEndedTimestampNanos = Long.MIN_VALUE;
            this.mProposedRotation = -1;
            this.mDesiredRotation = -1;
            this.mLastRotationResolution = -1;
            this.mCurrentCallbackId = 0;
            this.mRotationEvaluator = new Runnable() { // from class: com.android.server.wm.WindowOrientationListener.OrientationSensorJudge.2
                @Override // java.lang.Runnable
                public void run() {
                    int newRotation;
                    synchronized (WindowOrientationListener.this.mLock) {
                        OrientationSensorJudge.this.mRotationEvaluationScheduled = false;
                        newRotation = OrientationSensorJudge.this.evaluateRotationChangeLocked();
                    }
                    if (newRotation >= 0) {
                        WindowOrientationListener.this.onProposedRotationChanged(newRotation);
                    }
                }
            };
            setupRotationResolverParameters();
            this.mActivityTaskManagerInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        }

        private void setupRotationResolverParameters() {
            DeviceConfig.addOnPropertiesChangedListener("window_manager", ActivityThread.currentApplication().getMainExecutor(), new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.wm.WindowOrientationListener$OrientationSensorJudge$$ExternalSyntheticLambda0
                public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                    WindowOrientationListener.OrientationSensorJudge.this.m8529xf36900fe(properties);
                }
            });
            readRotationResolverParameters();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$setupRotationResolverParameters$0$com-android-server-wm-WindowOrientationListener$OrientationSensorJudge  reason: not valid java name */
        public /* synthetic */ void m8529xf36900fe(DeviceConfig.Properties properties) {
            Set<String> keys = properties.getKeyset();
            if (keys.contains(WindowOrientationListener.KEY_ROTATION_RESOLVER_TIMEOUT) || keys.contains(WindowOrientationListener.KEY_ROTATION_MEMORIZATION_TIMEOUT)) {
                readRotationResolverParameters();
            }
        }

        private void readRotationResolverParameters() {
            this.mRotationResolverTimeoutMillis = DeviceConfig.getLong("window_manager", WindowOrientationListener.KEY_ROTATION_RESOLVER_TIMEOUT, (long) WindowOrientationListener.DEFAULT_ROTATION_RESOLVER_TIMEOUT_MILLIS);
            this.mRotationMemorizationTimeoutMillis = DeviceConfig.getLong("window_manager", WindowOrientationListener.KEY_ROTATION_MEMORIZATION_TIMEOUT, 3000L);
        }

        @Override // com.android.server.wm.WindowOrientationListener.OrientationJudge
        public int getProposedRotationLocked() {
            return this.mProposedRotation;
        }

        @Override // com.android.server.wm.WindowOrientationListener.OrientationJudge
        public void onTouchStartLocked() {
            this.mTouching = true;
        }

        @Override // com.android.server.wm.WindowOrientationListener.OrientationJudge
        public void onTouchEndLocked(long whenElapsedNanos) {
            this.mTouching = false;
            this.mTouchEndedTimestampNanos = whenElapsedNanos;
            if (this.mDesiredRotation != this.mProposedRotation) {
                long now = SystemClock.elapsedRealtimeNanos();
                scheduleRotationEvaluationIfNecessaryLocked(now);
            }
        }

        @Override // com.android.server.wm.WindowOrientationListener.OrientationJudge, android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            WindowProcessController controller;
            final int reportedRotation = (int) event.values[0];
            if (reportedRotation < 0 || reportedRotation > 3) {
                return;
            }
            FrameworkStatsLog.write((int) FrameworkStatsLog.DEVICE_ROTATED, event.timestamp, rotationToLogEnum(reportedRotation), 2);
            if (WindowOrientationListener.this.isRotationResolverEnabled()) {
                if (WindowOrientationListener.this.isKeyguardLocked()) {
                    if (this.mLastRotationResolution != -1 && SystemClock.uptimeMillis() - this.mLastRotationResolutionTimeStamp < this.mRotationMemorizationTimeoutMillis) {
                        Slog.d(WindowOrientationListener.TAG, "Reusing the last rotation resolution: " + this.mLastRotationResolution);
                        finalizeRotation(this.mLastRotationResolution);
                        return;
                    }
                    finalizeRotation(0);
                    return;
                }
                if (WindowOrientationListener.this.mRotationResolverService == null) {
                    WindowOrientationListener.this.mRotationResolverService = (RotationResolverInternal) LocalServices.getService(RotationResolverInternal.class);
                    if (WindowOrientationListener.this.mRotationResolverService == null) {
                        finalizeRotation(reportedRotation);
                        return;
                    }
                }
                String packageName = null;
                ActivityTaskManagerInternal activityTaskManagerInternal = this.mActivityTaskManagerInternal;
                if (activityTaskManagerInternal != null && (controller = activityTaskManagerInternal.getTopApp()) != null && controller.mInfo != null && controller.mInfo.packageName != null) {
                    packageName = controller.mInfo.packageName;
                }
                this.mCurrentCallbackId++;
                if (this.mCancelRotationResolverRequest != null) {
                    WindowOrientationListener.this.getHandler().removeCallbacks(this.mCancelRotationResolverRequest);
                }
                final CancellationSignal cancellationSignal = new CancellationSignal();
                Objects.requireNonNull(cancellationSignal);
                this.mCancelRotationResolverRequest = new Runnable() { // from class: com.android.server.wm.WindowOrientationListener$OrientationSensorJudge$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        cancellationSignal.cancel();
                    }
                };
                WindowOrientationListener.this.getHandler().postDelayed(this.mCancelRotationResolverRequest, this.mRotationResolverTimeoutMillis);
                WindowOrientationListener.this.mRotationResolverService.resolveRotation(new RotationResolverInternal.RotationResolverCallbackInternal() { // from class: com.android.server.wm.WindowOrientationListener.OrientationSensorJudge.1
                    private final int mCallbackId;

                    {
                        this.mCallbackId = OrientationSensorJudge.this.mCurrentCallbackId;
                    }

                    public void onSuccess(int result) {
                        finalizeRotationIfFresh(result);
                    }

                    public void onFailure(int error) {
                        finalizeRotationIfFresh(reportedRotation);
                    }

                    private void finalizeRotationIfFresh(int rotation) {
                        if (this.mCallbackId == OrientationSensorJudge.this.mCurrentCallbackId) {
                            WindowOrientationListener.this.getHandler().removeCallbacks(OrientationSensorJudge.this.mCancelRotationResolverRequest);
                            OrientationSensorJudge.this.finalizeRotation(rotation);
                            return;
                        }
                        Slog.d(WindowOrientationListener.TAG, String.format("An outdated callback received [%s vs. %s]. Ignoring it.", Integer.valueOf(this.mCallbackId), Integer.valueOf(OrientationSensorJudge.this.mCurrentCallbackId)));
                    }
                }, packageName, reportedRotation, WindowOrientationListener.this.mCurrentRotation, this.mRotationResolverTimeoutMillis, cancellationSignal);
                return;
            }
            finalizeRotation(reportedRotation);
        }

        @Override // com.android.server.wm.WindowOrientationListener.OrientationJudge, android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override // com.android.server.wm.WindowOrientationListener.OrientationJudge
        public void dumpLocked(PrintWriter pw, String prefix) {
            pw.println(prefix + "OrientationSensorJudge");
            String prefix2 = prefix + "  ";
            pw.println(prefix2 + "mDesiredRotation=" + Surface.rotationToString(this.mDesiredRotation));
            pw.println(prefix2 + "mProposedRotation=" + Surface.rotationToString(this.mProposedRotation));
            pw.println(prefix2 + "mTouching=" + this.mTouching);
            pw.println(prefix2 + "mTouchEndedTimestampNanos=" + this.mTouchEndedTimestampNanos);
            pw.println(prefix2 + "mLastRotationResolution=" + this.mLastRotationResolution);
        }

        @Override // com.android.server.wm.WindowOrientationListener.OrientationJudge
        public void resetLocked(boolean clearCurrentRotation) {
            if (clearCurrentRotation) {
                this.mProposedRotation = -1;
                this.mDesiredRotation = -1;
            }
            this.mTouching = false;
            this.mTouchEndedTimestampNanos = Long.MIN_VALUE;
            unscheduleRotationEvaluationLocked();
        }

        public int evaluateRotationChangeLocked() {
            unscheduleRotationEvaluationLocked();
            if (this.mDesiredRotation == this.mProposedRotation) {
                return -1;
            }
            long now = SystemClock.elapsedRealtimeNanos();
            if (isDesiredRotationAcceptableLocked(now)) {
                int i = this.mDesiredRotation;
                this.mProposedRotation = i;
                return i;
            }
            scheduleRotationEvaluationIfNecessaryLocked(now);
            return -1;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void finalizeRotation(int reportedRotation) {
            int newRotation;
            synchronized (WindowOrientationListener.this.mLock) {
                this.mDesiredRotation = reportedRotation;
                newRotation = evaluateRotationChangeLocked();
            }
            if (newRotation >= 0) {
                this.mLastRotationResolution = newRotation;
                this.mLastRotationResolutionTimeStamp = SystemClock.uptimeMillis();
                WindowOrientationListener.this.onProposedRotationChanged(newRotation);
            }
        }

        private boolean isDesiredRotationAcceptableLocked(long now) {
            return !this.mTouching && now >= this.mTouchEndedTimestampNanos + 500000000;
        }

        private void scheduleRotationEvaluationIfNecessaryLocked(long now) {
            if (this.mRotationEvaluationScheduled || this.mDesiredRotation == this.mProposedRotation) {
                if (WindowOrientationListener.LOG) {
                    Slog.d(WindowOrientationListener.TAG, "scheduleRotationEvaluationLocked: ignoring, an evaluation is already scheduled or is unnecessary.");
                }
            } else if (this.mTouching) {
                if (WindowOrientationListener.LOG) {
                    Slog.d(WindowOrientationListener.TAG, "scheduleRotationEvaluationLocked: ignoring, user is still touching the screen.");
                }
            } else {
                long timeOfNextPossibleRotationNanos = this.mTouchEndedTimestampNanos + 500000000;
                if (now >= timeOfNextPossibleRotationNanos) {
                    if (WindowOrientationListener.LOG) {
                        Slog.d(WindowOrientationListener.TAG, "scheduleRotationEvaluationLocked: ignoring, already past the next possible time of rotation.");
                        return;
                    }
                    return;
                }
                long delayMs = (long) Math.ceil(((float) (timeOfNextPossibleRotationNanos - now)) * 1.0E-6f);
                WindowOrientationListener.this.mHandler.postDelayed(this.mRotationEvaluator, delayMs);
                this.mRotationEvaluationScheduled = true;
            }
        }

        private void unscheduleRotationEvaluationLocked() {
            if (!this.mRotationEvaluationScheduled) {
                return;
            }
            WindowOrientationListener.this.mHandler.removeCallbacks(this.mRotationEvaluator);
            this.mRotationEvaluationScheduled = false;
        }

        private int rotationToLogEnum(int rotation) {
            switch (rotation) {
                case 0:
                    return 1;
                case 1:
                    return 2;
                case 2:
                    return 3;
                case 3:
                    return 4;
                default:
                    return 0;
            }
        }
    }
}
