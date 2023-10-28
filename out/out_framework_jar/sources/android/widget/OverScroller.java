package android.widget;

import android.animation.dynamicanimation.DynamicAnimation;
import android.animation.dynamicanimation.FlingAnimation;
import android.animation.dynamicanimation.FloatPropertyCompat;
import android.animation.dynamicanimation.SpringAnimation;
import android.animation.dynamicanimation.SpringForce;
import android.app.ActivityThread;
import android.content.Context;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.util.Log;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.OverScroller;
import android.widget.Scroller;
import com.mediatek.boostfwk.BoostFwkFactory;
import com.mediatek.boostfwk.BoostFwkManager;
import com.mediatek.boostfwk.scenario.BasicScenario;
import com.mediatek.boostfwk.scenario.refreshrate.RefreshRateScenario;
import com.mediatek.boostfwk.scenario.scroll.ScrollScenario;
import com.mediatek.powerhalmgr.PowerHalMgr;
import com.mediatek.powerhalmgr.PowerHalMgrFactory;
import com.transsion.hubcore.widget.ITranOverScroller;
/* loaded from: classes4.dex */
public class OverScroller {
    private static final int DEFAULT_DURATION = 250;
    private static final float DEFAULT_FRICTION = 0.53f;
    private static final float DEFAULT_MINIMUM_VISIBLE_CHANGE = 0.5f;
    private static final int FAIL_TIMEOUT = 1000;
    private static final int FLING_MODE = 1;
    private static final int MTKPOWER_HINT_UX_FLINGING = 46;
    private static final int SCROLL_MODE = 0;
    private static final float SPRING_DAMPING_RATIO = 1.0f;
    private static final float SPRING_STIFFNESS = 200.0f;
    private static final String TAG = "OverScroller";
    private static PowerHalMgr mPowerHalService;
    private final boolean mFlywheel;
    private Interpolator mInterpolator;
    private boolean mIsInTranFlingBlacklist;
    private int mMode;
    private final SplineOverScroller mScrollerX;
    private final SplineOverScroller mScrollerY;
    private boolean mTranFlingSupport;

    public OverScroller(Context context) {
        this(context, null);
    }

    public OverScroller(Context context, Interpolator interpolator) {
        this(context, interpolator, true);
    }

    public OverScroller(Context context, Interpolator interpolator, boolean flywheel) {
        this.mTranFlingSupport = false;
        this.mIsInTranFlingBlacklist = false;
        if (interpolator == null) {
            this.mInterpolator = new Scroller.ViscousFluidInterpolator();
        } else {
            this.mInterpolator = interpolator;
        }
        this.mFlywheel = flywheel;
        boolean z = 1 == SystemProperties.getInt("ro.tran.fling.support", 0) && SystemProperties.getBoolean("persist.sys.traneffect.enable", true);
        this.mTranFlingSupport = z;
        if (z) {
            String processName = ActivityThread.currentProcessName();
            this.mIsInTranFlingBlacklist = processName == null || processName.contains("system_server") || processName.contains("com.shopee") || processName.contains("com.instagram.android") || processName.contains("trendyol.com");
            if (Looper.myLooper() == null) {
                this.mTranFlingSupport = false;
            }
        }
        SplineOverScroller splineOverScroller = new SplineOverScroller(context);
        this.mScrollerX = splineOverScroller;
        SplineOverScroller splineOverScroller2 = new SplineOverScroller(context);
        this.mScrollerY = splineOverScroller2;
        splineOverScroller.setVariableRefreshRateEnable(false);
        splineOverScroller2.setVariableRefreshRateEnable(true);
        splineOverScroller.mScrollerType = 0;
        splineOverScroller2.mScrollerType = 1;
    }

    @Deprecated
    public OverScroller(Context context, Interpolator interpolator, float bounceCoefficientX, float bounceCoefficientY) {
        this(context, interpolator, true);
    }

    @Deprecated
    public OverScroller(Context context, Interpolator interpolator, float bounceCoefficientX, float bounceCoefficientY, boolean flywheel) {
        this(context, interpolator, flywheel);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setInterpolator(Interpolator interpolator) {
        if (interpolator == null) {
            this.mInterpolator = new Scroller.ViscousFluidInterpolator();
        } else {
            this.mInterpolator = interpolator;
        }
    }

    public final void setFriction(float friction) {
        this.mScrollerX.setFriction(friction);
        this.mScrollerY.setFriction(friction);
    }

    public final boolean isFinished() {
        return this.mScrollerX.mFinished && this.mScrollerY.mFinished;
    }

    public final void forceFinished(boolean finished) {
        SplineOverScroller splineOverScroller = this.mScrollerX;
        this.mScrollerY.mFinished = finished;
        splineOverScroller.mFinished = finished;
        if (isTranFlinging() && finished) {
            this.mScrollerX.cancelTranFlingAnimationIfNeeded();
            this.mScrollerY.cancelTranFlingAnimationIfNeeded();
        }
    }

    public final int getCurrX() {
        this.mScrollerX.mIsFinalPointRead = true;
        return this.mScrollerX.mCurrentPosition;
    }

    public final int getCurrY() {
        this.mScrollerY.mIsFinalPointRead = true;
        return this.mScrollerY.mCurrentPosition;
    }

    public float getCurrVelocity() {
        return (float) Math.hypot(this.mScrollerX.mCurrVelocity, this.mScrollerY.mCurrVelocity);
    }

    public final int getStartX() {
        return this.mScrollerX.mStart;
    }

    public final int getStartY() {
        return this.mScrollerY.mStart;
    }

    public final int getFinalX() {
        return this.mScrollerX.mFinal;
    }

    public final int getFinalY() {
        return this.mScrollerY.mFinal;
    }

    public final int getDuration() {
        return Math.max(this.mScrollerX.mDuration, this.mScrollerY.mDuration);
    }

    public void extendDuration(int extend) {
        this.mScrollerX.extendDuration(extend);
        this.mScrollerY.extendDuration(extend);
    }

    public void setFinalX(int newX) {
        this.mScrollerX.setFinalPosition(newX);
    }

    public void setFinalY(int newY) {
        this.mScrollerY.setFinalPosition(newY);
    }

    public boolean computeScrollOffset() {
        if (isFinished()) {
            return this.mTranFlingSupport && !(this.mScrollerX.mIsFinalPointRead && this.mScrollerY.mIsFinalPointRead);
        }
        switch (this.mMode) {
            case 0:
                long time = AnimationUtils.currentAnimationTimeMillis();
                long elapsedTime = time - this.mScrollerX.mStartTime;
                int duration = this.mScrollerX.mDuration;
                if (elapsedTime < duration) {
                    float q = this.mInterpolator.getInterpolation(((float) elapsedTime) / duration);
                    this.mScrollerX.updateScroll(q);
                    this.mScrollerY.updateScroll(q);
                    break;
                } else {
                    abortAnimation();
                    break;
                }
            case 1:
                if (this.mTranFlingSupport) {
                    if (!this.mScrollerX.mFinished) {
                        BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mScrollerX.mScrollScenario.setBoostStatus(0));
                    }
                    if (!this.mScrollerY.mFinished) {
                        BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mScrollerY.mScrollScenario.setBoostStatus(0));
                    }
                }
                if (this.mScrollerX.mState == 3 || this.mScrollerY.mState == 3) {
                    ITranOverScroller.Instance().computeScrollOffsetOnFlingMode(this.mScrollerY.mState, 1, this.mScrollerY.mCurrVelocity);
                    return !isFinished();
                }
                if (!this.mScrollerX.mFinished && !this.mScrollerX.update() && !this.mScrollerX.continueWhenFinished()) {
                    this.mScrollerX.finish();
                }
                if (!this.mScrollerY.mFinished && !this.mScrollerY.update() && !this.mScrollerY.continueWhenFinished()) {
                    this.mScrollerY.finish();
                    break;
                }
                break;
        }
        return true;
    }

    public void startScroll(int startX, int startY, int dx, int dy) {
        startScroll(startX, startY, dx, dy, 250);
    }

    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        this.mMode = 0;
        this.mScrollerX.startScroll(startX, dx, duration);
        this.mScrollerY.startScroll(startY, dy, duration);
    }

    public boolean springBack(int startX, int startY, int minX, int maxX, int minY, int maxY) {
        this.mMode = 1;
        boolean spingbackX = this.mScrollerX.springback(startX, minX, maxX);
        boolean spingbackY = this.mScrollerY.springback(startY, minY, maxY);
        return spingbackX || spingbackY;
    }

    public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY) {
        fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, 0, 0);
    }

    /* JADX WARN: Removed duplicated region for block: B:14:0x0053  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY, int overX, int overY) {
        int velocityX2;
        int velocityY2;
        if (this.mFlywheel && !isFinished()) {
            float oldVelocityX = this.mScrollerX.mCurrVelocity;
            float oldVelocityY = this.mScrollerY.mCurrVelocity;
            if (Math.signum(velocityX) == Math.signum(oldVelocityX) && Math.signum(velocityY) == Math.signum(oldVelocityY)) {
                velocityX2 = (int) (velocityX + oldVelocityX);
                velocityY2 = (int) (velocityY + oldVelocityY);
                this.mMode = 1;
                if (this.mTranFlingSupport) {
                    if (overX == 0 && overY == 0 && minX <= 0 && minY <= 0 && maxX == Integer.MAX_VALUE && maxY == Integer.MAX_VALUE && startX >= minX && startX <= maxX && startY >= minY && startY <= maxY && !this.mIsInTranFlingBlacklist) {
                        this.mScrollerX.mState = 3;
                        this.mScrollerY.mState = 3;
                    } else {
                        this.mScrollerX.cancelTranFlingAnimationIfNeeded();
                        this.mScrollerY.cancelTranFlingAnimationIfNeeded();
                        this.mScrollerX.mState = 0;
                        this.mScrollerY.mState = 0;
                    }
                }
                this.mScrollerX.fling(startX, velocityX2, minX, maxX, overX);
                this.mScrollerY.fling(startY, velocityY2, minY, maxY, overY);
            }
        }
        velocityX2 = velocityX;
        velocityY2 = velocityY;
        this.mMode = 1;
        if (this.mTranFlingSupport) {
        }
        this.mScrollerX.fling(startX, velocityX2, minX, maxX, overX);
        this.mScrollerY.fling(startY, velocityY2, minY, maxY, overY);
    }

    public void notifyHorizontalEdgeReached(int startX, int finalX, int overX) {
        this.mScrollerX.notifyEdgeReached(startX, finalX, overX);
    }

    public void notifyVerticalEdgeReached(int startY, int finalY, int overY) {
        this.mScrollerY.notifyEdgeReached(startY, finalY, overY);
    }

    public boolean isOverScrolled() {
        return (this.mScrollerX.mState == 3 || this.mScrollerY.mState == 3) ? this.mScrollerX.isOverScroll() || this.mScrollerY.isOverScroll() : ((this.mScrollerX.mFinished || this.mScrollerX.mState == 0) && (this.mScrollerY.mFinished || this.mScrollerY.mState == 0)) ? false : true;
    }

    public void abortAnimation() {
        this.mScrollerX.finish();
        this.mScrollerY.finish();
    }

    public int timePassed() {
        long time = AnimationUtils.currentAnimationTimeMillis();
        long startTime = Math.min(this.mScrollerX.mStartTime, this.mScrollerY.mStartTime);
        return (int) (time - startTime);
    }

    public void disableTranFling() {
        this.mTranFlingSupport = false;
    }

    private boolean isTranFlinging() {
        return (this.mScrollerX.mState == 3 || this.mScrollerY.mState == 3) && !isFinished();
    }

    public boolean isScrollingInDirection(float xvel, float yvel) {
        int dx = this.mScrollerX.mFinal - this.mScrollerX.mStart;
        int dy = this.mScrollerY.mFinal - this.mScrollerY.mStart;
        return !isFinished() && Math.signum(xvel) == Math.signum((float) dx) && Math.signum(yvel) == Math.signum((float) dy);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes4.dex */
    public static class SplineOverScroller {
        private static final int BALLISTIC = 2;
        private static final int CUBIC = 1;
        private static final float END_TENSION = 1.0f;
        private static final float GRAVITY = 2000.0f;
        private static final float INFLEXION = 0.35f;
        private static final int NB_SAMPLES = 100;
        private static final float P1 = 0.175f;
        private static final float P2 = 0.35000002f;
        private static final int SPLINE = 0;
        private static final float START_TENSION = 0.5f;
        private static final int TRAN_FLING = 3;
        private static final int XSCROLLER = 0;
        private static final int YSCROLLER = 1;
        private Context mContext;
        private float mCurrVelocity;
        private int mCurrentPosition;
        private float mDeceleration;
        private int mDuration;
        private int mFinal;
        private FlingAnimation mFlingAnimation;
        private Handler mHandler;
        private int mMaxValue;
        private int mMinValue;
        private int mOver;
        private float mPhysicalCoeff;
        private RefreshRateScenario mRefreshRateScenario;
        private ScrollScenario mScrollScenario;
        private int mScrollerType;
        private int mSplineDistance;
        private int mSplineDuration;
        private SpringAnimation mSpringAnimation;
        private int mStart;
        private long mStartTime;
        private int mVelocity;
        private static float DECELERATION_RATE = (float) (Math.log(0.78d) / Math.log(0.9d));
        private static final float[] SPLINE_POSITION = new float[101];
        private static final float[] SPLINE_TIME = new float[101];
        private static final FloatPropertyCompat<SplineOverScroller> SPRING_PROPERTY = new FloatPropertyCompat<SplineOverScroller>("splineOverScrollerSpring") { // from class: android.widget.OverScroller.SplineOverScroller.1
            /* JADX DEBUG: Method merged with bridge method */
            @Override // android.animation.dynamicanimation.FloatPropertyCompat
            public float getValue(SplineOverScroller scroller) {
                return scroller.mCurrentPosition - scroller.mStart;
            }

            /* JADX DEBUG: Method merged with bridge method */
            @Override // android.animation.dynamicanimation.FloatPropertyCompat
            public void setValue(SplineOverScroller scroller, float value) {
                scroller.mIsFinalPointRead = false;
                scroller.mCurrentPosition = Math.round(value) + scroller.mStart;
            }
        };
        private float mFlingFriction = ViewConfiguration.getScrollFriction();
        private int mState = 0;
        private boolean mIsFinalPointRead = true;
        private int mPowerHandle = 0;
        private boolean mIsVariableRefreshRateEnabled = false;
        private boolean mIsSmoothFlingEnabled = false;
        private boolean mFinished = true;

        static {
            float f;
            float x;
            float f2;
            float coef;
            float y;
            float coef2;
            float x_min = 0.0f;
            float y_min = 0.0f;
            for (int i = 0; i < 100; i++) {
                float alpha = i / 100.0f;
                float x_max = 1.0f;
                while (true) {
                    f = 2.0f;
                    x = ((x_max - x_min) / 2.0f) + x_min;
                    f2 = 3.0f;
                    coef = x * 3.0f * (1.0f - x);
                    float tx = ((((1.0f - x) * P1) + (x * P2)) * coef) + (x * x * x);
                    if (Math.abs(tx - alpha) < 1.0E-5d) {
                        break;
                    } else if (tx > alpha) {
                        x_max = x;
                    } else {
                        x_min = x;
                    }
                }
                SPLINE_POSITION[i] = ((((1.0f - x) * 0.5f) + x) * coef) + (x * x * x);
                float y_max = 1.0f;
                while (true) {
                    y = ((y_max - y_min) / f) + y_min;
                    coef2 = y * f2 * (1.0f - y);
                    float dy = ((((1.0f - y) * 0.5f) + y) * coef2) + (y * y * y);
                    if (Math.abs(dy - alpha) < 1.0E-5d) {
                        break;
                    } else if (dy > alpha) {
                        y_max = y;
                        f = 2.0f;
                        f2 = 3.0f;
                    } else {
                        y_min = y;
                        f = 2.0f;
                        f2 = 3.0f;
                    }
                }
                SPLINE_TIME[i] = (coef2 * (((1.0f - y) * P1) + (P2 * y))) + (y * y * y);
            }
            float[] fArr = SPLINE_POSITION;
            SPLINE_TIME[100] = 1.0f;
            fArr[100] = 1.0f;
        }

        void setFriction(float friction) {
            this.mFlingFriction = friction;
            if (this.mIsVariableRefreshRateEnabled) {
                this.mRefreshRateScenario.setFlingFriction(friction);
            }
        }

        SplineOverScroller(Context context) {
            this.mContext = context;
            float ppi = context.getResources().getDisplayMetrics().density * 160.0f;
            this.mPhysicalCoeff = 386.0878f * ppi * 0.84f;
            this.mScrollScenario = new ScrollScenario(1, 2, this.mContext, this);
        }

        void updateScroll(float q) {
            int i = this.mStart;
            this.mCurrentPosition = i + Math.round((this.mFinal - i) * q);
        }

        private static float getDeceleration(int velocity) {
            if (velocity > 0) {
                return -2000.0f;
            }
            return GRAVITY;
        }

        private void adjustDuration(int start, int oldFinal, int newFinal) {
            int oldDistance = oldFinal - start;
            int newDistance = newFinal - start;
            float x = Math.abs(newDistance / oldDistance);
            int index = (int) (x * 100.0f);
            if (index < 100) {
                float x_inf = index / 100.0f;
                float x_sup = (index + 1) / 100.0f;
                float[] fArr = SPLINE_TIME;
                float t_inf = fArr[index];
                float t_sup = fArr[index + 1];
                float timeCoef = (((x - x_inf) / (x_sup - x_inf)) * (t_sup - t_inf)) + t_inf;
                this.mDuration = (int) (this.mDuration * timeCoef);
            }
        }

        void cancelTranFlingAnimationIfNeeded() {
            if (this.mState == 3) {
                FlingAnimation flingAnimation = this.mFlingAnimation;
                if (flingAnimation != null && flingAnimation.isRunning()) {
                    cancelAnimation(this.mFlingAnimation);
                }
                SpringAnimation springAnimation = this.mSpringAnimation;
                if (springAnimation != null && springAnimation.isRunning()) {
                    cancelAnimation(this.mSpringAnimation);
                }
            }
        }

        void cancelAnimation(final DynamicAnimation animation) {
            Handler handler = this.mHandler;
            if (handler != null) {
                handler.runWithScissors(new Runnable() { // from class: android.widget.OverScroller$SplineOverScroller$$ExternalSyntheticLambda4
                    @Override // java.lang.Runnable
                    public final void run() {
                        DynamicAnimation.this.cancel();
                    }
                }, 1000L);
            }
        }

        private void startAfterEdgeTran(int start, int min, int max, int velocity) {
            boolean positive;
            boolean keepIncreasing = true;
            if (start > min && start < max) {
                Log.e(OverScroller.TAG, "startAfterEdge called from a valid position");
                this.mFinished = true;
                return;
            }
            if (start > max) {
                positive = true;
            } else {
                positive = false;
            }
            int edge = positive ? max : min;
            int overDistance = start - edge;
            if (overDistance * velocity < 0) {
                keepIncreasing = false;
            }
            if (keepIncreasing) {
                this.mStart = start;
                this.mCurrentPosition = start;
                startSpring(start, 0.0f, edge, overDistance);
            }
        }

        private void startSpring(int start, float velocity, int edge, int over) {
            this.mStart = start;
            this.mCurrentPosition = start;
            SpringAnimation springAnimation = this.mSpringAnimation;
            if (springAnimation != null) {
                springAnimation.getSpring().setFinalPosition(edge - this.mStart);
                this.mSpringAnimation.setStartVelocity(velocity);
                this.mSpringAnimation.start();
            }
        }

        boolean isOverScroll() {
            int i;
            SpringAnimation springAnimation = this.mSpringAnimation;
            return (springAnimation != null && springAnimation.isRunning()) || (i = this.mCurrentPosition) < this.mMinValue || i > this.mMaxValue;
        }

        void startScroll(int start, int distance, int duration) {
            cancelTranFlingAnimationIfNeeded();
            if (this.mState == 3) {
                this.mState = 0;
            }
            this.mFinished = false;
            this.mStart = start;
            this.mCurrentPosition = start;
            this.mFinal = start + distance;
            this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
            this.mDuration = duration;
            this.mDeceleration = 0.0f;
            this.mVelocity = 0;
        }

        void finish() {
            BoostFwkManager makeBoostFwkManager = BoostFwkFactory.getInstance().makeBoostFwkManager();
            BasicScenario[] basicScenarioArr = new BasicScenario[2];
            basicScenarioArr[0] = this.mScrollScenario.setBoostStatus(1);
            basicScenarioArr[1] = !this.mIsVariableRefreshRateEnabled ? null : this.mRefreshRateScenario.setScenarioAction(2);
            makeBoostFwkManager.perfHint(basicScenarioArr);
            ITranOverScroller.Instance().hookFinish(this.mScrollerType);
            cancelTranFlingAnimationIfNeeded();
            if (this.mState == 3) {
                this.mFinished = true;
                releaseFlingBoost();
                return;
            }
            this.mCurrentPosition = this.mFinal;
            this.mFinished = true;
        }

        void setFinalPosition(int position) {
            this.mFinal = position;
            this.mSplineDistance = position - this.mStart;
            this.mFinished = false;
        }

        void extendDuration(int extend) {
            long time = AnimationUtils.currentAnimationTimeMillis();
            int elapsedTime = (int) (time - this.mStartTime);
            int i = elapsedTime + extend;
            this.mSplineDuration = i;
            this.mDuration = i;
            this.mFinished = false;
        }

        boolean springback(int start, int min, int max) {
            this.mFinished = true;
            this.mFinal = start;
            this.mStart = start;
            this.mCurrentPosition = start;
            this.mVelocity = 0;
            this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
            this.mDuration = 0;
            if (start < min) {
                startSpringback(start, min, 0);
            } else if (start > max) {
                startSpringback(start, max, 0);
            }
            return true ^ this.mFinished;
        }

        private void startSpringback(int start, int end, int velocity) {
            if (this.mState == 3) {
                cancelTranFlingAnimationIfNeeded();
                this.mFinished = false;
                startSpring(start, 0.0f, end, 0);
                return;
            }
            this.mFinished = false;
            this.mState = 1;
            this.mStart = start;
            this.mCurrentPosition = start;
            this.mFinal = end;
            int delta = start - end;
            this.mDeceleration = getDeceleration(delta);
            this.mVelocity = -delta;
            this.mOver = Math.abs(delta);
            this.mDuration = (int) (Math.sqrt((delta * (-2.0d)) / this.mDeceleration) * 1000.0d);
        }

        void fling(int start, int velocity, int min, int max, int over) {
            BoostFwkManager makeBoostFwkManager = BoostFwkFactory.getInstance().makeBoostFwkManager();
            BasicScenario[] basicScenarioArr = new BasicScenario[2];
            basicScenarioArr[0] = this.mScrollScenario.setBoostStatus(1);
            basicScenarioArr[1] = !this.mIsVariableRefreshRateEnabled ? null : this.mRefreshRateScenario.setScenarioAction(0).setCurrentVelocity(velocity);
            makeBoostFwkManager.perfHint(basicScenarioArr);
            this.mOver = over;
            this.mFinished = false;
            this.mVelocity = velocity;
            this.mCurrVelocity = velocity;
            this.mSplineDuration = 0;
            this.mDuration = 0;
            this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
            this.mStart = start;
            this.mCurrentPosition = start;
            if (this.mState == 3) {
                FlingAnimation flingAnimation = this.mFlingAnimation;
                if (flingAnimation != null && flingAnimation.isRunning()) {
                    cancelAnimation(this.mFlingAnimation);
                }
                SpringAnimation springAnimation = this.mSpringAnimation;
                if (springAnimation != null && springAnimation.isRunning()) {
                    cancelAnimation(this.mSpringAnimation);
                }
                this.mStart = start;
                this.mCurrentPosition = start;
                this.mVelocity = velocity;
                this.mCurrVelocity = velocity;
                if (Looper.myLooper() == null) {
                    this.mState = 0;
                    this.mHandler = null;
                    this.mOver = over;
                    this.mFinished = false;
                    this.mSplineDuration = 0;
                    this.mDuration = 0;
                    this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
                    Log.d(OverScroller.TAG, "use spline");
                } else {
                    this.mHandler = new Handler(Looper.myLooper());
                    if (velocity == 0) {
                        this.mState = 0;
                        this.mFinished = true;
                        return;
                    }
                    this.mMinValue = min;
                    this.mMaxValue = max;
                    if (this.mFlingAnimation == null) {
                        this.mFlingAnimation = new FlingAnimation(this, SPRING_PROPERTY);
                        if (SystemProperties.get("vendor.transsion.scroll_adjust.support").equals("1")) {
                            float OS_FRICTION = Float.valueOf(SystemProperties.get("vendor.transsion.os_friction")).floatValue();
                            this.mFlingAnimation.setFriction(OS_FRICTION);
                        } else {
                            this.mFlingAnimation.setFriction(OverScroller.DEFAULT_FRICTION);
                        }
                        this.mFlingAnimation.setMinimumVisibleChange(0.5f);
                        this.mFlingAnimation.addEndListener(new DynamicAnimation.OnAnimationEndListener() { // from class: android.widget.OverScroller$SplineOverScroller$$ExternalSyntheticLambda0
                            @Override // android.animation.dynamicanimation.DynamicAnimation.OnAnimationEndListener
                            public final void onAnimationEnd(DynamicAnimation dynamicAnimation, boolean z, float f, float f2) {
                                OverScroller.SplineOverScroller.this.m5863lambda$fling$1$androidwidgetOverScroller$SplineOverScroller(dynamicAnimation, z, f, f2);
                            }
                        });
                        this.mFlingAnimation.addUpdateListener(new DynamicAnimation.OnAnimationUpdateListener() { // from class: android.widget.OverScroller$SplineOverScroller$$ExternalSyntheticLambda1
                            @Override // android.animation.dynamicanimation.DynamicAnimation.OnAnimationUpdateListener
                            public final void onAnimationUpdate(DynamicAnimation dynamicAnimation, float f, float f2) {
                                OverScroller.SplineOverScroller.this.m5864lambda$fling$2$androidwidgetOverScroller$SplineOverScroller(dynamicAnimation, f, f2);
                            }
                        });
                    }
                    if (this.mSpringAnimation == null) {
                        SpringAnimation springAnimation2 = new SpringAnimation(this, SPRING_PROPERTY);
                        this.mSpringAnimation = springAnimation2;
                        springAnimation2.setSpring(new SpringForce(this.mFinal).setStiffness(200.0f).setDampingRatio(1.0f));
                        this.mSpringAnimation.addEndListener(new DynamicAnimation.OnAnimationEndListener() { // from class: android.widget.OverScroller$SplineOverScroller$$ExternalSyntheticLambda2
                            @Override // android.animation.dynamicanimation.DynamicAnimation.OnAnimationEndListener
                            public final void onAnimationEnd(DynamicAnimation dynamicAnimation, boolean z, float f, float f2) {
                                OverScroller.SplineOverScroller.this.m5865lambda$fling$3$androidwidgetOverScroller$SplineOverScroller(dynamicAnimation, z, f, f2);
                            }
                        });
                    }
                    this.mFlingAnimation.setStartVelocity(velocity);
                    this.mFinished = false;
                    Handler handler = this.mHandler;
                    if (handler != null) {
                        handler.runWithScissors(new Runnable() { // from class: android.widget.OverScroller$SplineOverScroller$$ExternalSyntheticLambda3
                            @Override // java.lang.Runnable
                            public final void run() {
                                OverScroller.SplineOverScroller.this.m5866lambda$fling$4$androidwidgetOverScroller$SplineOverScroller();
                            }
                        }, 1000L);
                    }
                    this.mFinal = this.mStart + Math.round(this.mFlingAnimation.getDestination());
                    this.mDuration = Math.round(this.mFlingAnimation.getDuration());
                    if (this.mFinal < min) {
                        this.mFinal = min;
                    }
                    if (this.mFinal > max) {
                        this.mFinal = max;
                    }
                    startFlingBoost();
                    return;
                }
            }
            if (start > max || start < min) {
                startAfterEdge(start, min, max, velocity);
                return;
            }
            this.mState = 0;
            double totalDistance = 0.0d;
            if (velocity != 0) {
                int splineFlingDuration = getSplineFlingDuration(velocity);
                this.mSplineDuration = splineFlingDuration;
                this.mDuration = splineFlingDuration;
                totalDistance = getSplineFlingDistance(velocity);
            }
            int signum = (int) (Math.signum(velocity) * totalDistance);
            this.mSplineDistance = signum;
            int i = signum + start;
            this.mFinal = i;
            if (i < min) {
                adjustDuration(this.mStart, i, min);
                this.mFinal = min;
            }
            int i2 = this.mFinal;
            if (i2 > max) {
                adjustDuration(this.mStart, i2, max);
                this.mFinal = max;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$fling$1$android-widget-OverScroller$SplineOverScroller  reason: not valid java name */
        public /* synthetic */ void m5863lambda$fling$1$androidwidgetOverScroller$SplineOverScroller(DynamicAnimation animation, boolean canceled, float value, float velocity1) {
            this.mCurrentPosition = Math.round(value) + this.mStart;
            finish();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$fling$2$android-widget-OverScroller$SplineOverScroller  reason: not valid java name */
        public /* synthetic */ void m5864lambda$fling$2$androidwidgetOverScroller$SplineOverScroller(DynamicAnimation animation, float value, float velocity2) {
            this.mCurrVelocity = velocity2;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$fling$3$android-widget-OverScroller$SplineOverScroller  reason: not valid java name */
        public /* synthetic */ void m5865lambda$fling$3$androidwidgetOverScroller$SplineOverScroller(DynamicAnimation animation, boolean canceled, float value, float velocity1) {
            this.mCurrentPosition = Math.round(value) + this.mStart;
            this.mFinished = true;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$fling$4$android-widget-OverScroller$SplineOverScroller  reason: not valid java name */
        public /* synthetic */ void m5866lambda$fling$4$androidwidgetOverScroller$SplineOverScroller() {
            this.mFlingAnimation.start();
        }

        private void startFlingBoost() {
            if (OverScroller.mPowerHalService == null) {
                OverScroller.mPowerHalService = PowerHalMgrFactory.getInstance().makePowerHalMgr();
            }
            if (OverScroller.mPowerHalService != null) {
                OverScroller.mPowerHalService.perfLockRelease(this.mPowerHandle);
                Log.d(OverScroller.TAG, "SplineOverScroller fling startFlingBoost mPowerHandle = [" + this.mPowerHandle + NavigationBarInflaterView.SIZE_MOD_END);
                this.mPowerHandle = OverScroller.mPowerHalService.perfCusLockHint(46, this.mDuration);
            }
        }

        private void releaseFlingBoost() {
            if (OverScroller.mPowerHalService == null) {
                OverScroller.mPowerHalService = PowerHalMgrFactory.getInstance().makePowerHalMgr();
            }
            if (OverScroller.mPowerHalService != null) {
                Log.d(OverScroller.TAG, "SplineOverScroller fling releaseFlingBoost mPowerHandle = [" + this.mPowerHandle + NavigationBarInflaterView.SIZE_MOD_END);
                OverScroller.mPowerHalService.perfLockRelease(this.mPowerHandle);
            }
        }

        private double getSplineDeceleration(int velocity) {
            return Math.log((Math.abs(velocity) * INFLEXION) / (this.mFlingFriction * this.mPhysicalCoeff));
        }

        private double getSplineFlingDistance(int velocity) {
            double l = getSplineDeceleration(velocity);
            float f = DECELERATION_RATE;
            double decelMinusOne = f - 1.0d;
            if (this.mIsVariableRefreshRateEnabled && this.mIsSmoothFlingEnabled) {
                return this.mRefreshRateScenario.getSplineFlingDistance();
            }
            return this.mFlingFriction * this.mPhysicalCoeff * Math.exp((f / decelMinusOne) * l);
        }

        private int getSplineFlingDuration(int velocity) {
            double l = getSplineDeceleration(velocity);
            double decelMinusOne = DECELERATION_RATE - 1.0d;
            if (this.mIsVariableRefreshRateEnabled && this.mIsSmoothFlingEnabled) {
                return this.mRefreshRateScenario.getSplineDuration();
            }
            return (int) (Math.exp(l / decelMinusOne) * 1000.0d);
        }

        private void fitOnBounceCurve(int start, int end, int velocity) {
            float f = this.mDeceleration;
            float durationToApex = (-velocity) / f;
            float velocitySquared = velocity * velocity;
            float distanceToApex = (velocitySquared / 2.0f) / Math.abs(f);
            float distanceToEdge = Math.abs(end - start);
            float totalDuration = (float) Math.sqrt(((distanceToApex + distanceToEdge) * 2.0d) / Math.abs(this.mDeceleration));
            this.mStartTime -= (int) ((totalDuration - durationToApex) * 1000.0f);
            this.mStart = end;
            this.mCurrentPosition = end;
            this.mVelocity = (int) ((-this.mDeceleration) * totalDuration);
        }

        private void startBounceAfterEdge(int start, int end, int velocity) {
            this.mDeceleration = getDeceleration(velocity == 0 ? start - end : velocity);
            fitOnBounceCurve(start, end, velocity);
            onEdgeReached();
        }

        private void startAfterEdge(int start, int min, int max, int velocity) {
            if (start > min && start < max) {
                Log.e(OverScroller.TAG, "startAfterEdge called from a valid position");
                this.mFinished = true;
                return;
            }
            boolean positive = start > max;
            int edge = positive ? max : min;
            int overDistance = start - edge;
            boolean keepIncreasing = overDistance * velocity >= 0;
            if (keepIncreasing) {
                startBounceAfterEdge(start, edge, velocity);
                return;
            }
            double totalDistance = getSplineFlingDistance(velocity);
            if (totalDistance > Math.abs(overDistance)) {
                fling(start, velocity, positive ? min : start, positive ? start : max, this.mOver);
            } else {
                startSpringback(start, edge, velocity);
            }
        }

        void notifyEdgeReached(int start, int end, int over) {
            int i = this.mState;
            if (i == 3) {
                cancelTranFlingAnimationIfNeeded();
                this.mOver = over;
                this.mFinished = false;
                this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
                startAfterEdgeTran(start, end, end, (int) this.mCurrVelocity);
            } else if (i == 0) {
                this.mOver = over;
                this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
                startAfterEdge(start, end, end, (int) this.mCurrVelocity);
            }
        }

        private void onEdgeReached() {
            int i = this.mVelocity;
            float velocitySquared = i * i;
            float distance = velocitySquared / (Math.abs(this.mDeceleration) * 2.0f);
            float sign = Math.signum(this.mVelocity);
            int i2 = this.mOver;
            if (distance > i2) {
                this.mDeceleration = ((-sign) * velocitySquared) / (i2 * 2.0f);
                distance = i2;
            }
            this.mOver = (int) distance;
            this.mState = 2;
            int i3 = this.mStart;
            int i4 = this.mVelocity;
            this.mFinal = i3 + ((int) (i4 > 0 ? distance : -distance));
            this.mDuration = -((int) ((i4 * 1000.0f) / this.mDeceleration));
        }

        boolean continueWhenFinished() {
            switch (this.mState) {
                case 0:
                    if (this.mDuration < this.mSplineDuration) {
                        int i = this.mFinal;
                        this.mStart = i;
                        this.mCurrentPosition = i;
                        int i2 = (int) this.mCurrVelocity;
                        this.mVelocity = i2;
                        this.mDeceleration = getDeceleration(i2);
                        this.mStartTime += this.mDuration;
                        onEdgeReached();
                        break;
                    } else {
                        return false;
                    }
                case 1:
                    return false;
                case 2:
                    this.mStartTime += this.mDuration;
                    startSpringback(this.mFinal, this.mStart, 0);
                    break;
            }
            update();
            return true;
        }

        boolean update() {
            if (this.mState == 3) {
                return this.mFinished;
            }
            long time = AnimationUtils.currentAnimationTimeMillis();
            long currentTime = time - this.mStartTime;
            if (currentTime == 0) {
                return this.mDuration > 0;
            } else if (this.mScrollScenario.isSFPEnable() && currentTime < 0) {
                return this.mDuration > 0;
            } else if (currentTime > this.mDuration) {
                return false;
            } else {
                BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mScrollScenario.setBoostStatus(0));
                double distance = 0.0d;
                switch (this.mState) {
                    case 0:
                        if (!this.mIsVariableRefreshRateEnabled) {
                            int i = this.mSplineDuration;
                            float t = ((float) currentTime) / i;
                            int index = (int) (t * 100.0f);
                            float distanceCoef = 1.0f;
                            float velocityCoef = 0.0f;
                            if (index < 100) {
                                float t_inf = index / 100.0f;
                                float t_sup = (index + 1) / 100.0f;
                                float[] fArr = SPLINE_POSITION;
                                float d_inf = fArr[index];
                                float d_sup = fArr[index + 1];
                                velocityCoef = (d_sup - d_inf) / (t_sup - t_inf);
                                distanceCoef = d_inf + ((t - t_inf) * velocityCoef);
                            }
                            int i2 = this.mSplineDistance;
                            distance = i2 * distanceCoef;
                            this.mCurrVelocity = ((i2 * velocityCoef) / i) * 1000.0f;
                            ITranOverScroller.Instance().update(this.mScrollerType, this.mCurrVelocity);
                            break;
                        } else {
                            BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mRefreshRateScenario.setCurrentTime(currentTime).setScenarioAction(1));
                            distance = this.mRefreshRateScenario.getCurrentDistance();
                            this.mCurrVelocity = this.mRefreshRateScenario.getCurrentVelocity();
                            break;
                        }
                    case 1:
                        float t2 = ((float) currentTime) / this.mDuration;
                        float t22 = t2 * t2;
                        float sign = Math.signum(this.mVelocity);
                        int i3 = this.mOver;
                        distance = i3 * sign * ((3.0f * t22) - ((2.0f * t2) * t22));
                        this.mCurrVelocity = i3 * sign * 6.0f * ((-t2) + t22);
                        break;
                    case 2:
                        float t3 = ((float) currentTime) / 1000.0f;
                        int i4 = this.mVelocity;
                        float f = this.mDeceleration;
                        this.mCurrVelocity = i4 + (f * t3);
                        distance = (i4 * t3) + (((f * t3) * t3) / 2.0f);
                        break;
                }
                this.mCurrentPosition = this.mStart + ((int) Math.round(distance));
                return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setVariableRefreshRateEnable(boolean isVariableRefreshRateEnabled) {
            if (isVariableRefreshRateEnabled) {
                this.mRefreshRateScenario = new RefreshRateScenario(this.mContext);
                BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mRefreshRateScenario.setScenarioAction(3).setOriginalSplinePosition(SPLINE_POSITION).setOriginalSplineTime(SPLINE_TIME).setFlingFriction(this.mFlingFriction).setPhysicalCoeff(this.mPhysicalCoeff));
                this.mIsSmoothFlingEnabled = this.mRefreshRateScenario.getSmoothFlingEnabled();
                this.mIsVariableRefreshRateEnabled = this.mRefreshRateScenario.getVariableRefreshRateEnabled();
                return;
            }
            this.mIsVariableRefreshRateEnabled = false;
        }
    }

    /* loaded from: classes4.dex */
    public static class TranSplineOverScroller {
        public static int getTranFling() {
            return 3;
        }

        private TranSplineOverScroller() {
        }
    }
}
