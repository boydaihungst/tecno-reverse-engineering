package android.view;

import android.sysprop.InputProperties;
import android.util.ArrayMap;
import android.util.Pools;
import com.transsion.hubcore.view.ITranView;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;
/* loaded from: classes3.dex */
public final class VelocityTracker {
    private static final int ACTIVE_POINTER_ID = -1;
    private static final Map<String, Integer> STRATEGIES;
    public static final int VELOCITY_TRACKER_STRATEGY_DEFAULT = -1;
    public static final int VELOCITY_TRACKER_STRATEGY_IMPULSE = 0;
    public static final int VELOCITY_TRACKER_STRATEGY_INT1 = 7;
    public static final int VELOCITY_TRACKER_STRATEGY_INT2 = 8;
    public static final int VELOCITY_TRACKER_STRATEGY_LEGACY = 9;
    public static final int VELOCITY_TRACKER_STRATEGY_LSQ1 = 1;
    public static final int VELOCITY_TRACKER_STRATEGY_LSQ2 = 2;
    public static final int VELOCITY_TRACKER_STRATEGY_LSQ3 = 3;
    public static final int VELOCITY_TRACKER_STRATEGY_WLSQ2_CENTRAL = 5;
    public static final int VELOCITY_TRACKER_STRATEGY_WLSQ2_DELTA = 4;
    public static final int VELOCITY_TRACKER_STRATEGY_WLSQ2_RECENT = 6;
    private static final Pools.SynchronizedPool<VelocityTracker> sPool = new Pools.SynchronizedPool<>(2);
    private long mPtr;
    private final int mStrategy;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    public @interface VelocityTrackerStrategy {
    }

    private static native void nativeAddMovement(long j, MotionEvent motionEvent);

    private static native void nativeClear(long j);

    private static native void nativeComputeCurrentVelocity(long j, int i, float f);

    private static native void nativeDispose(long j);

    private static native boolean nativeGetEstimator(long j, int i, Estimator estimator);

    private static native float nativeGetXVelocity(long j, int i);

    private static native float nativeGetYVelocity(long j, int i);

    private static native long nativeInitialize(int i);

    static {
        ArrayMap arrayMap = new ArrayMap();
        STRATEGIES = arrayMap;
        arrayMap.put("impulse", 0);
        arrayMap.put("lsq1", 1);
        arrayMap.put("lsq2", 2);
        arrayMap.put("lsq3", 3);
        arrayMap.put("wlsq2-delta", 4);
        arrayMap.put("wlsq2-central", 5);
        arrayMap.put("wlsq2-recent", 6);
        arrayMap.put("int1", 7);
        arrayMap.put("int2", 8);
        arrayMap.put("legacy", 9);
    }

    private static int toStrategyId(String strStrategy) {
        Map<String, Integer> map = STRATEGIES;
        if (map.containsKey(strStrategy)) {
            return map.get(strStrategy).intValue();
        }
        return -1;
    }

    public static VelocityTracker obtain() {
        VelocityTracker instance = sPool.acquire();
        return instance != null ? instance : new VelocityTracker(-1);
    }

    @Deprecated
    public static VelocityTracker obtain(String strategy) {
        if (strategy == null) {
            return obtain();
        }
        return new VelocityTracker(toStrategyId(strategy));
    }

    public static VelocityTracker obtain(int strategy) {
        return new VelocityTracker(strategy);
    }

    public void recycle() {
        if (this.mStrategy == -1) {
            clear();
            sPool.release(this);
        }
    }

    public int getStrategyId() {
        return this.mStrategy;
    }

    private VelocityTracker(int strategy) {
        if (strategy == -1) {
            String strategyProperty = InputProperties.velocitytracker_strategy().orElse(null);
            if (strategyProperty == null || strategyProperty.isEmpty()) {
                this.mStrategy = strategy;
            } else {
                this.mStrategy = toStrategyId(strategyProperty);
            }
        } else {
            this.mStrategy = strategy;
        }
        this.mPtr = nativeInitialize(this.mStrategy);
    }

    protected void finalize() throws Throwable {
        try {
            long j = this.mPtr;
            if (j != 0) {
                nativeDispose(j);
                this.mPtr = 0L;
            }
        } finally {
            super.finalize();
        }
    }

    public void clear() {
        nativeClear(this.mPtr);
    }

    public void addMovement(MotionEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        nativeAddMovement(this.mPtr, event);
    }

    public void computeCurrentVelocity(int units) {
        nativeComputeCurrentVelocity(this.mPtr, units, Float.MAX_VALUE);
    }

    public void computeCurrentVelocity(int units, float maxVelocity) {
        nativeComputeCurrentVelocity(this.mPtr, units, maxVelocity);
    }

    public float getXVelocity() {
        return nativeGetXVelocity(this.mPtr, -1);
    }

    public float getYVelocity() {
        return nativeGetYVelocity(this.mPtr, -1);
    }

    public float getXVelocity(int id) {
        if (ITranView.Instance().isAppturboSupport()) {
            return ITranView.Instance().getVelocity(true, nativeGetXVelocity(this.mPtr, id));
        }
        return nativeGetXVelocity(this.mPtr, id);
    }

    public float getYVelocity(int id) {
        if (ITranView.Instance().isAppturboSupport()) {
            return ITranView.Instance().getVelocity(false, nativeGetYVelocity(this.mPtr, id));
        }
        return nativeGetYVelocity(this.mPtr, id);
    }

    public boolean getEstimator(int id, Estimator outEstimator) {
        if (outEstimator == null) {
            throw new IllegalArgumentException("outEstimator must not be null");
        }
        return nativeGetEstimator(this.mPtr, id, outEstimator);
    }

    /* loaded from: classes3.dex */
    public static final class Estimator {
        private static final int MAX_DEGREE = 4;
        public float confidence;
        public int degree;
        public final float[] xCoeff = new float[5];
        public final float[] yCoeff = new float[5];

        public float estimateX(float time) {
            return estimate(time, this.xCoeff);
        }

        public float estimateY(float time) {
            return estimate(time, this.yCoeff);
        }

        public float getXCoeff(int index) {
            if (index <= this.degree) {
                return this.xCoeff[index];
            }
            return 0.0f;
        }

        public float getYCoeff(int index) {
            if (index <= this.degree) {
                return this.yCoeff[index];
            }
            return 0.0f;
        }

        private float estimate(float time, float[] c) {
            float a = 0.0f;
            float scale = 1.0f;
            for (int i = 0; i <= this.degree; i++) {
                a += c[i] * scale;
                scale *= time;
            }
            return a;
        }
    }
}
