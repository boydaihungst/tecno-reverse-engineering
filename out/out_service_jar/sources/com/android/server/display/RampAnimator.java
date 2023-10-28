package com.android.server.display;

import android.animation.ValueAnimator;
import android.os.SystemProperties;
import android.util.FloatProperty;
import android.util.Slog;
import android.view.Choreographer;
import com.android.server.display.LocalDisplayAdapter;
import com.transsion.hubcore.server.display.ITranDisplayPowerController;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class RampAnimator<T> {
    protected static final int ANIMATOR_STATE_END = 2;
    protected static final int ANIMATOR_STATE_START = 1;
    private static final String TAG = "RampAnimator";
    private float mAnimatedValue;
    private boolean mAnimating;
    private float mAnimationDecreaseMaxTimeSecs;
    private float mAnimationIncreaseMaxTimeSecs;
    private float mCurrentValue;
    private int mDisplayId;
    private long mLastFrameTimeNanos;
    private Listener mListener;
    private final T mObject;
    private final FloatProperty<T> mProperty;
    private float mRate;
    private float mRateHigh;
    private float mRateLow;
    private float mTargetValue;
    private boolean mUpdateRate;
    private static final boolean DEBUG = SystemProperties.getBoolean("dbg.dms.dpc", false);
    private static final boolean TRAN_BACKLIGHT_OPTIMIZATION_SUPPORT = "1".equals(SystemProperties.get("ro.transsion.backlight.optimization", "0"));
    private static final boolean mIsLucidDisabled = SystemProperties.get("persist.product.lucid.disabled").equals("1");
    private static final boolean TRAN_LUCID_OPTIMIZATION_SUPPORT = "1".equals(SystemProperties.get("persist.transsion.lucid.optimization", "0"));
    private boolean mFirstTime = true;
    private final Runnable mAnimationCallback = new Runnable() { // from class: com.android.server.display.RampAnimator.1
        @Override // java.lang.Runnable
        public void run() {
            float amount;
            long frameTimeNanos = RampAnimator.this.mChoreographer.getFrameTimeNanos();
            float timeDelta = ((float) (frameTimeNanos - RampAnimator.this.mLastFrameTimeNanos)) * 1.0E-9f;
            RampAnimator.this.mLastFrameTimeNanos = frameTimeNanos;
            float scale = ValueAnimator.getDurationScale();
            if (scale == 0.0f) {
                RampAnimator rampAnimator = RampAnimator.this;
                rampAnimator.mAnimatedValue = rampAnimator.mTargetValue;
            } else {
                if (RampAnimator.TRAN_BACKLIGHT_OPTIMIZATION_SUPPORT) {
                    if (RampAnimator.this.mCurrentValue < 0.04884005d) {
                        amount = (RampAnimator.this.mRateLow * timeDelta) / scale;
                    } else {
                        amount = (RampAnimator.this.mRateHigh * timeDelta) / scale;
                    }
                } else {
                    amount = (RampAnimator.this.mRate * timeDelta) / scale;
                }
                if (RampAnimator.this.mTargetValue > RampAnimator.this.mCurrentValue) {
                    RampAnimator rampAnimator2 = RampAnimator.this;
                    rampAnimator2.mAnimatedValue = Math.min(rampAnimator2.mAnimatedValue + amount, RampAnimator.this.mTargetValue);
                } else {
                    RampAnimator rampAnimator3 = RampAnimator.this;
                    rampAnimator3.mAnimatedValue = Math.max(rampAnimator3.mAnimatedValue - amount, RampAnimator.this.mTargetValue);
                }
            }
            float oldCurrentValue = RampAnimator.this.mCurrentValue;
            RampAnimator rampAnimator4 = RampAnimator.this;
            rampAnimator4.mCurrentValue = rampAnimator4.mAnimatedValue;
            if (oldCurrentValue != RampAnimator.this.mCurrentValue) {
                RampAnimator rampAnimator5 = RampAnimator.this;
                rampAnimator5.setPropertyValue(rampAnimator5.mCurrentValue);
            }
            if (RampAnimator.this.mTargetValue != RampAnimator.this.mCurrentValue) {
                RampAnimator.this.postAnimationCallback();
                return;
            }
            RampAnimator.this.mAnimating = false;
            ITranDisplayPowerController.Instance().hookDisplayBrightnessAnimatorState(2);
            if (RampAnimator.this.mListener != null) {
                RampAnimator.this.mListener.onAnimationEnd();
            }
        }
    };
    private final Choreographer mChoreographer = Choreographer.getInstance();

    /* loaded from: classes.dex */
    public interface Listener {
        void onAnimationEnd();
    }

    RampAnimator(T object, FloatProperty<T> property) {
        this.mObject = object;
        this.mProperty = property;
    }

    public void setAnimationTimeLimits(long animationRampIncreaseMaxTimeMillis, long animationRampDecreaseMaxTimeMillis) {
        this.mAnimationIncreaseMaxTimeSecs = animationRampIncreaseMaxTimeMillis > 0 ? ((float) animationRampIncreaseMaxTimeMillis) / 1000.0f : 0.0f;
        this.mAnimationDecreaseMaxTimeSecs = animationRampDecreaseMaxTimeMillis > 0 ? ((float) animationRampDecreaseMaxTimeMillis) / 1000.0f : 0.0f;
    }

    /* JADX WARN: Removed duplicated region for block: B:44:0x0075  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean animateTo(float targetLinear, float rate) {
        float target;
        boolean z;
        if (this.mDisplayId == 0) {
            target = LocalDisplayAdapter.BacklightAdapter.useSurfaceControlBrightness() ? BrightnessUtils.convertLinearToGamma(targetLinear) : targetLinear;
        } else {
            target = BrightnessUtils.convertLinearToGamma(targetLinear);
        }
        boolean z2 = this.mFirstTime;
        if (z2 || rate <= 0.0f) {
            if (z2 || target != this.mCurrentValue) {
                this.mFirstTime = false;
                this.mRate = 0.0f;
                this.mTargetValue = target;
                this.mCurrentValue = target;
                setPropertyValue(target);
                if (this.mAnimating) {
                    this.mAnimating = false;
                    ITranDisplayPowerController.Instance().hookDisplayBrightnessAnimatorState(2);
                    cancelAnimationCallback();
                }
                Listener listener = this.mListener;
                if (listener != null) {
                    listener.onAnimationEnd();
                }
                return true;
            }
            return false;
        }
        float f = this.mCurrentValue;
        if (target > f) {
            float f2 = this.mAnimationIncreaseMaxTimeSecs;
            if (f2 > 0.0f && (target - f) / rate > f2) {
                rate = (target - f) / f2;
                z = this.mAnimating;
                if (z || rate > this.mRate || ((target <= f && f <= this.mTargetValue) || (this.mTargetValue <= f && f <= target))) {
                    this.mRate = rate;
                }
                boolean changed = this.mTargetValue != target;
                this.mTargetValue = target;
                if (!z && target != f) {
                    this.mAnimating = true;
                    ITranDisplayPowerController.Instance().hookDisplayBrightnessAnimatorState(1);
                    this.mAnimatedValue = this.mCurrentValue;
                    this.mLastFrameTimeNanos = System.nanoTime();
                    postAnimationCallback();
                }
                return changed;
            }
        }
        if (target < f) {
            float f3 = this.mAnimationDecreaseMaxTimeSecs;
            if (f3 > 0.0f && (f - target) / rate > f3) {
                rate = (f - target) / f3;
            }
        }
        z = this.mAnimating;
        if (z) {
        }
        this.mRate = rate;
        if (this.mTargetValue != target) {
        }
        this.mTargetValue = target;
        if (!z) {
            this.mAnimating = true;
            ITranDisplayPowerController.Instance().hookDisplayBrightnessAnimatorState(1);
            this.mAnimatedValue = this.mCurrentValue;
            this.mLastFrameTimeNanos = System.nanoTime();
            postAnimationCallback();
        }
        return changed;
    }

    public boolean isAnimating() {
        return this.mAnimating;
    }

    public void setDisplayId(int displayId) {
        this.mDisplayId = displayId;
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setPropertyValue(float val) {
        this.mProperty.setValue(this.mObject, val);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void postAnimationCallback() {
        this.mChoreographer.postCallback(1, this.mAnimationCallback, null);
    }

    private void cancelAnimationCallback() {
        this.mChoreographer.removeCallbacks(1, this.mAnimationCallback, null);
    }

    public float currentBrightness() {
        return this.mCurrentValue;
    }

    public boolean isRateUpdate() {
        return this.mUpdateRate;
    }

    /* JADX WARN: Code restructure failed: missing block: B:16:0x004e, code lost:
        if (r3 <= r7.mTargetValue) goto L29;
     */
    /* JADX WARN: Code restructure failed: missing block: B:20:0x0058, code lost:
        if (r3 <= r8) goto L29;
     */
    /* JADX WARN: Removed duplicated region for block: B:26:0x006d  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean animateTo(float target, float rateHigh, float rateLow, boolean updateImmediately) {
        boolean z = DEBUG;
        if (z) {
            Slog.i(TAG, "animateTo target=" + target + "   rateHigh=" + rateHigh + "    rateLow=" + rateLow);
        }
        this.mUpdateRate = false;
        boolean z2 = this.mFirstTime;
        if (z2 || rateHigh <= 0.0f) {
            if (!z2 && target == this.mCurrentValue) {
                return false;
            }
            this.mFirstTime = false;
            this.mRateHigh = 0.0f;
            this.mRateLow = 0.0f;
            this.mTargetValue = target;
            this.mCurrentValue = target;
            this.mProperty.setValue(this.mObject, target);
            if (this.mAnimating) {
                this.mAnimating = false;
                ITranDisplayPowerController.Instance().hookDisplayBrightnessAnimatorState(2);
                cancelAnimationCallback();
            }
            Listener listener = this.mListener;
            if (listener != null) {
                listener.onAnimationEnd();
            }
            return true;
        }
        if (this.mAnimating && !updateImmediately) {
            float f = this.mCurrentValue;
            if (target <= f) {
            }
            if (this.mTargetValue <= f) {
            }
            boolean changed = this.mTargetValue != target;
            this.mTargetValue = target;
            if (!this.mAnimating && target != this.mCurrentValue) {
                this.mAnimating = true;
                ITranDisplayPowerController.Instance().hookDisplayBrightnessAnimatorState(1);
                this.mAnimatedValue = this.mCurrentValue;
                this.mLastFrameTimeNanos = System.nanoTime();
                postAnimationCallback();
            }
            return changed;
        }
        this.mRateHigh = rateHigh;
        this.mRateLow = rateLow;
        this.mUpdateRate = true;
        if (z) {
            Slog.i(TAG, "  update dimming rate.");
        }
        boolean changed2 = this.mTargetValue != target;
        this.mTargetValue = target;
        if (!this.mAnimating) {
            this.mAnimating = true;
            ITranDisplayPowerController.Instance().hookDisplayBrightnessAnimatorState(1);
            this.mAnimatedValue = this.mCurrentValue;
            this.mLastFrameTimeNanos = System.nanoTime();
            postAnimationCallback();
        }
        return changed2;
    }

    /* loaded from: classes.dex */
    static class DualRampAnimator<T> {
        private final RampAnimator<T> mFirst;
        private final Listener mInternalListener;
        private Listener mListener;
        private final RampAnimator<T> mSecond;

        /* JADX INFO: Access modifiers changed from: package-private */
        public DualRampAnimator(T object, FloatProperty<T> firstProperty, FloatProperty<T> secondProperty) {
            Listener listener = new Listener() { // from class: com.android.server.display.RampAnimator.DualRampAnimator.1
                @Override // com.android.server.display.RampAnimator.Listener
                public void onAnimationEnd() {
                    if (DualRampAnimator.this.mListener != null && !DualRampAnimator.this.isAnimating()) {
                        DualRampAnimator.this.mListener.onAnimationEnd();
                    }
                }
            };
            this.mInternalListener = listener;
            RampAnimator<T> rampAnimator = new RampAnimator<>(object, firstProperty);
            this.mFirst = rampAnimator;
            rampAnimator.setListener(listener);
            RampAnimator<T> rampAnimator2 = new RampAnimator<>(object, secondProperty);
            this.mSecond = rampAnimator2;
            rampAnimator2.setListener(listener);
        }

        public void setAnimationTimeLimits(long animationRampIncreaseMaxTimeMillis, long animationRampDecreaseMaxTimeMillis) {
            this.mFirst.setAnimationTimeLimits(animationRampIncreaseMaxTimeMillis, animationRampDecreaseMaxTimeMillis);
            this.mSecond.setAnimationTimeLimits(animationRampIncreaseMaxTimeMillis, animationRampDecreaseMaxTimeMillis);
        }

        public boolean animateTo(float linearFirstTarget, float linearSecondTarget, float rate) {
            boolean firstRetval = this.mFirst.animateTo(linearFirstTarget, rate);
            boolean secondRetval = this.mSecond.animateTo(linearSecondTarget, rate);
            return firstRetval && secondRetval;
        }

        public void setListener(Listener listener) {
            this.mListener = listener;
        }

        public boolean isAnimating() {
            return this.mFirst.isAnimating() && this.mSecond.isAnimating();
        }

        public void setDisplayId(int displayId) {
            this.mFirst.setDisplayId(displayId);
        }

        public boolean animateTo(float target, float rateHigh, float rateLow, boolean updateImmediately) {
            return this.mFirst.animateTo(target, rateHigh, rateLow, updateImmediately);
        }

        public boolean isRateUpdate() {
            return this.mFirst.isRateUpdate();
        }

        public float currentBrightness() {
            return this.mFirst.currentBrightness();
        }
    }
}
