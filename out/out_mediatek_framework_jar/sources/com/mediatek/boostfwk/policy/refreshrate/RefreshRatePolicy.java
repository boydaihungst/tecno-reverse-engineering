package com.mediatek.boostfwk.policy.refreshrate;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.VelocityTracker;
import android.view.ViewRootImpl;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import com.mediatek.boostfwk.info.ActivityInfo;
import com.mediatek.boostfwk.scenario.refreshrate.EventScenario;
import com.mediatek.boostfwk.scenario.refreshrate.RefreshRateScenario;
import com.mediatek.boostfwk.utils.Config;
import com.mediatek.boostfwk.utils.LogUtil;
import com.mediatek.boostfwk.utils.Util;
import dalvik.system.PathClassLoader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
/* loaded from: classes.dex */
public class RefreshRatePolicy {
    private static final int DEFAULT_MIN_REFRESHRATE_CHANGE_VALUE = 1000;
    private static final float FLING_STOP_PROGRESS_THRESHOLED = 0.99f;
    private static final int IDLE_DELAY_TIME = 2000;
    private static final int LIST_FINISH_THRESHOLED = 150;
    private static final int LIST_STATE_FLING_FINISH = 2;
    private static final int LIST_STATE_FLING_START = 0;
    private static final int LIST_STATE_FLING_UPDATE = 1;
    private static final int LIST_STATE_SCROLLER_INIT = 3;
    private static final int MOTION_EVENT_ACTION_DEFAULT = -1;
    private static final int MOTION_EVENT_ACTION_DOWN = 0;
    private static final int MOTION_EVENT_ACTION_MOVE = 1;
    private static final int MOTION_EVENT_ACTION_UP = 2;
    private static final int MSG_ENTER_IDLE = 0;
    private static final int REFRESH_RATE_IDLE = 1000;
    private static final int REFRESH_RATE_IME = 1003;
    private static final int REFRESH_RATE_INVALID = 9999;
    private static final int REFRESH_RATE_TOUCH_SCROLL_DOWN = 1001;
    private static final int REFRESH_RATE_TOUCH_SCROLL_UP = 1000;
    private static final int SCENARIO_ACTION_DEFAULT = 0;
    private static final int SCENARIO_ACTION_HIDE = 2;
    private static final int SCENARIO_ACTION_SHOW = 1;
    private static final int SCENARIO_TYPE_DEFAULT = 0;
    private static final int SCENARIO_TYPE_IME = 1;
    private static final int SCENARIO_TYPE_VOICE = 2;
    private static final String TAG = "MSYNC3-VariableRefreshRate";
    private static final int TOUCH_SCROLL_REFRESH_CHANGE_THRESHOLED = 200;
    private static final int TOUCH_SCROLL_REFRESH_DURATION_THRESHOLED = 300;
    private static final int TOUCH_SCROLL_REFRESH_VELOCITY_THRESHOLED = 100;
    private static final int TOUCH_SCROLL_STATE_HIGH_SPEED = 0;
    private static final int TOUCH_SCROLL_STATE_INVALID_SPEED = -1;
    private static final int TOUCH_SCROLL_STATE_LOW_SPEED = 1;
    private static final int TOUCH_SCROLL_VELOCITY_SAMPLE_COUNT = 5;
    private PathClassLoader mClassLoader;
    private ArrayList<Float> mFlingRefreshRateChangeGap;
    private ArrayList<Float> mFlingRefreshRateChangeTimeOffset;
    private ArrayList<Float> mFlingRefreshRateVsyncTime;
    private int mFlingSupportRefreshRateCount;
    private ArrayList<Integer> mFlingSupportedRefreshRate;
    private WeakReference<Window> mImeWindow;
    private float mLastTouchMovePointerY;
    private RefreshRateInfo mRefreshRateInfo;
    private int mSplinePositionCount;
    private float mTouchDownPointerY;
    private WorkerHandler mWorkerHandler;
    private RefreshRateScenario mActiveRefreshScenario = null;
    private boolean mIsDataInited = false;
    private boolean mMSyncSupportedByProcess = true;
    private boolean mActivityHasVideoView = false;
    private IRefreshRateEx mRefreshRatePolicyExt = null;
    private int mCurrentMaxRefreshRate = -1;
    private int mTouchScrollSpeed = 0;
    private long mLastRefreshChangeTime = 0;
    private int mCurrentTouchState = -1;
    private VelocityTracker mVelocityTracker = null;
    private long mTouchDownTime = 0;
    private long mTouchDuration = 0;
    private ArrayList<Float> mTouchScrollVelocityList = new ArrayList<>(5);

    public RefreshRatePolicy() {
        this.mRefreshRateInfo = null;
        this.mWorkerHandler = null;
        this.mRefreshRateInfo = new RefreshRateInfo();
        registerActivityListener();
        this.mWorkerHandler = new WorkerHandler(Looper.getMainLooper());
    }

    public void registerActivityListener() {
        ActivityInfo.getInstance().registerActivityListener(new ActivityInfo.ActivityChangeListener() { // from class: com.mediatek.boostfwk.policy.refreshrate.RefreshRatePolicy.1
            @Override // com.mediatek.boostfwk.info.ActivityInfo.ActivityChangeListener
            public void onChange(Context c) {
                LogUtil.mLogd(RefreshRatePolicy.TAG, "Activity Changed");
                RefreshRatePolicy.this.mActiveRefreshScenario = null;
                RefreshRatePolicy.this.mActivityHasVideoView = false;
                if (!RefreshRatePolicy.this.mMSyncSupportedByProcess || c == null || !(c instanceof Activity)) {
                    return;
                }
                RefreshRatePolicy.this.mRefreshRateInfo.updateCurrentActivityName(c);
                if (RefreshRatePolicy.this.mIsDataInited) {
                    return;
                }
                RefreshRatePolicy.this.mRefreshRateInfo.initHardwareSupportRefreshRate(c);
                RefreshRatePolicy.this.mRefreshRateInfo.initMSync3SupportRefreshRate();
                RefreshRatePolicy.this.mRefreshRateInfo.initPackageInfo(c);
                RefreshRatePolicy refreshRatePolicy = RefreshRatePolicy.this;
                refreshRatePolicy.mFlingSupportedRefreshRate = refreshRatePolicy.mRefreshRateInfo.getFlingSupportedRefreshRate();
                RefreshRatePolicy refreshRatePolicy2 = RefreshRatePolicy.this;
                refreshRatePolicy2.mFlingRefreshRateChangeGap = refreshRatePolicy2.mRefreshRateInfo.getFlingRefreshRateChangeGap();
                RefreshRatePolicy refreshRatePolicy3 = RefreshRatePolicy.this;
                refreshRatePolicy3.mFlingRefreshRateChangeTimeOffset = refreshRatePolicy3.mRefreshRateInfo.getFlingRefreshRateChangeTimeOffset();
                RefreshRatePolicy refreshRatePolicy4 = RefreshRatePolicy.this;
                refreshRatePolicy4.mFlingSupportRefreshRateCount = refreshRatePolicy4.mRefreshRateInfo.getFlingSupportedRefreshRateCount();
                RefreshRatePolicy refreshRatePolicy5 = RefreshRatePolicy.this;
                refreshRatePolicy5.mFlingRefreshRateVsyncTime = refreshRatePolicy5.mRefreshRateInfo.getFlingRefreshRateVSyncTime();
                try {
                    RefreshRatePolicy.this.mClassLoader = new PathClassLoader("/system/framework/msync-lib.jar", c.getClassLoader());
                    Class<?> clazz = Class.forName("com.mediatek.msync.RefreshRatePolicyExt", false, RefreshRatePolicy.this.mClassLoader);
                    RefreshRatePolicy.this.mRefreshRatePolicyExt = (IRefreshRateEx) clazz.getConstructor(new Class[0]).newInstance(new Object[0]);
                } catch (Exception e) {
                    LogUtil.mLoge(RefreshRatePolicy.TAG, "msync-lib.jar not exits");
                }
                RefreshRatePolicy.this.mIsDataInited = true;
            }
        });
    }

    public void setVeriableRefreshRateSupported(boolean isSupport) {
        this.mMSyncSupportedByProcess = isSupport;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class WorkerHandler extends Handler {
        WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    if (RefreshRatePolicy.this.mActiveRefreshScenario != null) {
                        RefreshRatePolicy refreshRatePolicy = RefreshRatePolicy.this;
                        refreshRatePolicy.updateRefreshRateWhenFling(Config.TOUCH_HINT_DURATION_DEFAULT, refreshRatePolicy.mActiveRefreshScenario);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    public void dispatchScenario(RefreshRateScenario refreshRateScenario) {
        int action = refreshRateScenario.getScenarioAction();
        LogUtil.mLogd(TAG, "dispatchScenario action = " + action + " mCurrentTouchState = " + this.mCurrentTouchState);
        LogUtil.traceBegin("Dispatch Scenario Action=" + action);
        switch (action) {
            case 0:
                if (this.mIsDataInited) {
                    onListFlingStart(refreshRateScenario);
                    break;
                } else {
                    return;
                }
            case 1:
                if (this.mIsDataInited) {
                    onListFlingUpdate(refreshRateScenario);
                    break;
                } else {
                    return;
                }
            case 2:
                if (this.mIsDataInited) {
                    onListFlingFinished(refreshRateScenario);
                    break;
                } else {
                    return;
                }
            case 3:
                onListScrollerInit(refreshRateScenario);
                break;
            default:
                LogUtil.mLoge(TAG, "RefreshRatePolicy dispatchScenario error");
                break;
        }
        LogUtil.traceEnd();
    }

    public void dispatchEvent(EventScenario eventScenario) {
        int action = eventScenario.getScenarioAction();
        LogUtil.mLogd(TAG, "dispatchEvent action = " + action);
        LogUtil.traceBegin("Dispatch Event Action=" + action);
        switch (action) {
            case 0:
            case 1:
                onViewEventUpdate(eventScenario);
                eventScenario.setVariableRefreshRateEnabled(true);
                break;
            default:
                LogUtil.mLoge(TAG, "RefreshRatePolicy dispatchEvent error");
                break;
        }
        LogUtil.traceEnd();
    }

    private void onListScrollerInit(RefreshRateScenario refreshRateScenario) {
        refreshRateScenario.setCurrentFlingState(3).setVariableRefreshRateEnabled(Config.isVariableRefreshRateSupported()).setSmoothFlingEnabled(Config.isMSync3SmoothFlingEnabled()).setListScrollStateListening(true);
        if (Config.isMSync3SmoothFlingEnabled()) {
            refreshRateScenario.setRealSplinePosition(this.mRefreshRateInfo.getSmoothFlingSplinePosition());
            refreshRateScenario.setRealSplineTime(this.mRefreshRateInfo.getSmoothFlingSplineTime());
            this.mSplinePositionCount = this.mRefreshRateInfo.getSmoothFlingSplinePositionCount();
        } else {
            refreshRateScenario.setRealSplinePosition(refreshRateScenario.getOriginalSplinePosition());
            refreshRateScenario.setRealSplineTime(refreshRateScenario.getOriginalSplineTime());
            this.mSplinePositionCount = refreshRateScenario.getOriginalSplinePosition().length - 1;
        }
        LogUtil.mLogd(TAG, "onListScrollerInit action = SCROLL_INIT");
    }

    private void onListFlingStart(RefreshRateScenario refreshRateScenario) {
        LogUtil.mLogi(TAG, "List fling start");
        this.mActiveRefreshScenario = refreshRateScenario;
        float velocity = refreshRateScenario.getCurrentVelocity();
        Util.getRefreshRate();
        refreshRateScenario.setTouchScrollEnabled(true);
        refreshRateScenario.setCurrentFlingState(0);
        int touchScrollState = getTouchScrollState();
        if (touchScrollState == -1 || Math.abs(velocity) < 1000.0f) {
            LogUtil.mLogd(TAG, "Current refresh rate not match  Current device refresh rate = " + Util.getRefreshRate() + " Max refresh rate = " + this.mRefreshRateInfo.getMaxFlingSupportedRefreshRate());
            refreshRateScenario.setRefreshRateChangeEnabledWhenFling(false);
        } else {
            refreshRateScenario.setFlingRefreshRateChangeIndex(touchScrollState).setCurrentRefreshrate(this.mFlingSupportedRefreshRate.get(touchScrollState).intValue()).setRefreshRateChangeEnabledWhenFling(true);
            this.mCurrentMaxRefreshRate = this.mRefreshRateInfo.getMaxFlingSupportedRefreshRate();
            boostToMaxRefreshRate(refreshRateScenario, this.mFlingSupportedRefreshRate.get(touchScrollState).intValue());
        }
        if (velocity != RefreshRateInfo.DECELERATION_RATE) {
            refreshRateScenario.setCurrentVelocity(adjustVelocity(velocity));
            int splineDuration = getSplineFlingDuration(refreshRateScenario);
            Double splineFlingDistance = Double.valueOf(getSplineFlingDistance(refreshRateScenario));
            int splineDistance = (int) (splineFlingDistance.doubleValue() * Math.signum(velocity));
            refreshRateScenario.setSplineDuration(splineDuration).setSplineFlingDistance(splineFlingDistance.doubleValue()).setSplineDistance(splineDistance).setDistanceCoef((float) RefreshRateInfo.DECELERATION_RATE);
        }
        LogUtil.mLogd(TAG, "onListFlingStart action = FLING_START velocity = " + velocity + " mSplineDuration = " + refreshRateScenario.getSplineDuration() + " mSplineFlingDistance = " + refreshRateScenario.getSplineFlingDistance() + " mSplineDistance = " + refreshRateScenario.getSplineDistance());
    }

    private float adjustVelocity(float velocity) {
        long touchScrollTime = SystemClock.uptimeMillis() - this.mTouchDownTime;
        float deltaY = this.mTouchDownPointerY - this.mLastTouchMovePointerY;
        float deltaV = velocity / ((deltaY / ((float) touchScrollTime)) * 1000.0f);
        LogUtil.mLogd(TAG, "adjustVelocity  velocity = " + velocity + " touchScrollTime = " + touchScrollTime + " deltaY = " + deltaY + " result = " + deltaV);
        if (touchScrollTime >= 200 || Math.abs(deltaY) >= 400.0f || Math.abs(deltaV) <= 2.5d) {
            return velocity;
        }
        float newVelocity = ((float) ((deltaY / ((float) touchScrollTime)) * 1000.0f * 2.5d)) * Math.signum(velocity);
        return newVelocity;
    }

    private void onListFlingUpdate(RefreshRateScenario refreshRateScenario) {
        float d_sup;
        float velocityCoef;
        refreshRateScenario.setCurrentFlingState(1);
        long currentTime = refreshRateScenario.getCurrentTime();
        int splineDuration = refreshRateScenario.getSplineDuration();
        int splineDistance = refreshRateScenario.getSplineDistance();
        float[] realSplinePosition = refreshRateScenario.getRealSplinePosition();
        int flingRefreshRateChangeIndex = refreshRateScenario.getFlingRefreshRateChangeIndex();
        int currentRefreshRate = refreshRateScenario.getCurrentRefreshrate();
        float t = ((float) currentTime) / splineDuration;
        int i = this.mSplinePositionCount;
        int index = (int) (i * t);
        if (index >= i) {
            d_sup = 1.0f;
            velocityCoef = 0.0f;
        } else {
            float t_inf = index / i;
            float t_sup = (index + 1) / i;
            float d_inf = realSplinePosition[index];
            float d_sup2 = realSplinePosition[index + 1];
            float velocityCoef2 = (d_sup2 - d_inf) / (t_sup - t_inf);
            float distanceCoef = d_inf + ((t - t_inf) * velocityCoef2);
            d_sup = distanceCoef;
            velocityCoef = velocityCoef2;
        }
        double currentDistance = splineDistance * d_sup;
        float currentVelocity = 1000.0f * ((splineDistance * velocityCoef) / splineDuration);
        refreshRateScenario.setCurrentDistance(currentDistance).setCurrentVelocity(currentVelocity).setDistanceCoef(d_sup);
        LogUtil.mLogd(TAG, "onListFlingUpdate  flingRefreshRateChangeIndex = " + flingRefreshRateChangeIndex + " currentRefreshRate = " + currentRefreshRate + " distanceCoef = " + d_sup + " currentDistance = " + currentDistance + " index = " + index);
        if (refreshRateScenario.getRefreshRateChangeEnabledWhenFling() && flingRefreshRateChangeIndex < this.mFlingSupportRefreshRateCount - 1) {
            if ((refreshRateScenario.hasVideo() || this.mActivityHasVideoView) && this.mFlingSupportedRefreshRate.get(flingRefreshRateChangeIndex + 1).intValue() < this.mRefreshRateInfo.getVideoFloorRefreshRate()) {
                LogUtil.mLogd(TAG, "List with video, min support refresh rate is " + this.mRefreshRateInfo.getVideoFloorRefreshRate());
                return;
            }
            IRefreshRateEx iRefreshRateEx = this.mRefreshRatePolicyExt;
            if (iRefreshRateEx != null) {
                int flingRefreshRateChangeIndex2 = refreshRateScenario.getCurrentRefreshrate();
                float currentVelocity2 = Util.getRefreshRate();
                int currentRefreshRate2 = this.mFlingSupportedRefreshRate.get(flingRefreshRateChangeIndex).intValue();
                double stepGap = iRefreshRateEx.calculateGap(currentTime, flingRefreshRateChangeIndex2, (int) currentVelocity2, currentRefreshRate2, this.mFlingRefreshRateVsyncTime.get(flingRefreshRateChangeIndex).doubleValue(), this.mFlingRefreshRateChangeTimeOffset.get(flingRefreshRateChangeIndex).doubleValue(), splineDuration, splineDistance, this.mSplinePositionCount, realSplinePosition);
                LogUtil.mLogd(TAG, "onListFlingUpdate stepGap = " + stepGap);
                if (flingRefreshRateChangeIndex < this.mFlingSupportRefreshRateCount) {
                    if (stepGap <= this.mFlingRefreshRateChangeGap.get(flingRefreshRateChangeIndex).floatValue() && stepGap != -1.0d) {
                        updateRefreshRateWhenFling(this.mFlingSupportedRefreshRate.get(flingRefreshRateChangeIndex + 1).intValue(), refreshRateScenario);
                    }
                }
            }
        }
    }

    private void onListFlingFinished(RefreshRateScenario refreshRateScenario) {
        if (refreshRateScenario.getCurrentFlingState() != 1) {
            refreshRateScenario.setCurrentFlingState(2);
            return;
        }
        refreshRateScenario.setLastFlingFinishTime(SystemClock.uptimeMillis());
        if (refreshRateScenario.getDistanceCoef() > FLING_STOP_PROGRESS_THRESHOLED) {
            updateRefreshRateWhenFling(Config.TOUCH_HINT_DURATION_DEFAULT, refreshRateScenario);
            refreshRateScenario.setTouchScrollEnabled(false);
            LogUtil.mLogd(TAG, "onListFlingFinished List stop by self");
        } else if (SystemClock.uptimeMillis() - refreshRateScenario.getLastTouchDownTime() < 150) {
            refreshRateScenario.setTouchScrollEnabled(true);
            LogUtil.mLogd(TAG, "onListFlingFinished List stop by touch event");
        } else {
            this.mWorkerHandler.removeMessages(0);
            refreshRateScenario.setTouchScrollEnabled(false);
            this.mWorkerHandler.sendEmptyMessageDelayed(0, 150L);
            LogUtil.mLogd(TAG, "onListFlingFinished List stop by other reason");
        }
        refreshRateScenario.setCurrentFlingState(2).setFlingRefreshRateChangeIndex(0).setRefreshRateChangeEnabledWhenFling(false).setDistanceCoef((float) RefreshRateInfo.DECELERATION_RATE);
    }

    private void onViewEventUpdate(EventScenario eventScenario) {
        RefreshRateScenario refreshRateScenario = this.mActiveRefreshScenario;
        if (refreshRateScenario != null && refreshRateScenario.getCurrentFlingState() == 1) {
            this.mActiveRefreshScenario.setHasVideo(true);
            eventScenario.setIsMarked(true);
            this.mActivityHasVideoView = true;
        }
    }

    private double getSplineDeceleration(RefreshRateScenario refreshRateScenario) {
        return Math.log((this.mRefreshRateInfo.getInflextion() * Math.abs(refreshRateScenario.getCurrentVelocity())) / (refreshRateScenario.getFlingFriction() * refreshRateScenario.getPhysicalCoeff()));
    }

    private double getSplineFlingDistance(RefreshRateScenario refreshRateScenario) {
        double l = getSplineDeceleration(refreshRateScenario);
        double decelMinusOne = RefreshRateInfo.DECELERATION_RATE - 1.0d;
        float rate = Math.abs(refreshRateScenario.getCurrentVelocity()) / this.mRefreshRateInfo.getMaximumVelocity();
        int i = this.mSplinePositionCount;
        int index = (int) (i * rate);
        if (index > i) {
            index = this.mSplinePositionCount;
        }
        float value = 1.0f - refreshRateScenario.getRealSplinePosition()[index];
        double tuningValue = (3.5f * value) + 1.65f;
        if (!Config.isMSync3SmoothFlingEnabled()) {
            tuningValue = 1.0d;
        }
        LogUtil.mLogd(TAG, "getSplineFlingDistance tuningValue = " + tuningValue + " rate = " + rate);
        return refreshRateScenario.getFlingFriction() * tuningValue * refreshRateScenario.getPhysicalCoeff() * Math.exp((RefreshRateInfo.DECELERATION_RATE / decelMinusOne) * l);
    }

    private int getSplineFlingDuration(RefreshRateScenario refreshRateScenario) {
        double l = getSplineDeceleration(refreshRateScenario);
        double decelMinusOne = RefreshRateInfo.DECELERATION_RATE - 1.0d;
        float rate = Math.abs(refreshRateScenario.getCurrentVelocity()) / this.mRefreshRateInfo.getMaximumVelocity();
        int i = this.mSplinePositionCount;
        int index = (int) (i * rate);
        if (index > i) {
            index = this.mSplinePositionCount;
        }
        float value = 1.0f - refreshRateScenario.getRealSplinePosition()[index];
        double tuningValue = (9.0f * value) + 1.65f;
        if (!Config.isMSync3SmoothFlingEnabled()) {
            tuningValue = 1.0d;
        }
        LogUtil.mLogd(TAG, "getSplineFlingDuration tuningValue = " + tuningValue + " rate = " + rate);
        return (int) (1000.0d * tuningValue * Math.exp(l / decelMinusOne));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateRefreshRateWhenFling(int refreshRate, RefreshRateScenario refreshRateScenario) {
        LogUtil.mLogd(TAG, "updateRefreshRateWhenFling change oldRefreshRate = " + refreshRateScenario.getCurrentRefreshrate() + " New RefreshRate = " + refreshRate);
        if (refreshRateScenario.getCurrentRefreshrate() != refreshRate && updateRefreshInternal(refreshRateScenario, refreshRate, 0, 0)) {
            refreshRateScenario.setCurrentRefreshrate(refreshRate);
            refreshRateScenario.increaseFlingRefreshRateChangeIndex();
        }
    }

    private void boostToMaxRefreshRate(RefreshRateScenario refreshRateScenario, int refreshRate) {
        LogUtil.mLogd(TAG, "boostToMaxRefreshRate change oldRefreshRate = " + refreshRateScenario.getCurrentRefreshrate() + " New RefreshRate = " + refreshRate);
        updateRefreshInternal(refreshRateScenario, refreshRate, 0, 0);
    }

    public void onTouchEvent(MotionEvent event) {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        switch (event.getActionMasked()) {
            case 0:
                this.mVelocityTracker.clear();
                this.mVelocityTracker.addMovement(event);
                this.mTouchDownTime = SystemClock.uptimeMillis();
                this.mTouchDownPointerY = event.getY();
                this.mLastTouchMovePointerY = RefreshRateInfo.DECELERATION_RATE;
                onScrollStateChange(0, this.mVelocityTracker.getYVelocity());
                this.mCurrentTouchState = 0;
                this.mTouchScrollSpeed = 0;
                return;
            case 1:
            case 3:
                this.mVelocityTracker.addMovement(event);
                this.mVelocityTracker.computeCurrentVelocity(100);
                onScrollStateChange(1, this.mVelocityTracker.getYVelocity());
                this.mCurrentTouchState = 2;
                this.mTouchScrollSpeed = -1;
                this.mLastTouchMovePointerY = event.getY();
                return;
            case 2:
                this.mVelocityTracker.addMovement(event);
                this.mVelocityTracker.computeCurrentVelocity(100);
                this.mTouchDuration = SystemClock.uptimeMillis() - this.mTouchDownTime;
                onVelocityChange(this.mVelocityTracker.getYVelocity(), this.mTouchDuration);
                this.mCurrentTouchState = 1;
                this.mLastTouchMovePointerY = event.getY();
                return;
            default:
                return;
        }
    }

    public void onVelocityChange(float velocity, long duration) {
        if (this.mActiveRefreshScenario == null) {
            return;
        }
        int velocitySize = this.mTouchScrollVelocityList.size();
        if (velocitySize >= 5) {
            this.mTouchScrollVelocityList.remove(0);
            this.mTouchScrollVelocityList.add(Float.valueOf(Math.abs(velocity)));
            if (duration > 300 && duration - this.mLastRefreshChangeTime > 200) {
                float averageVelocity = calculateAverage(this.mTouchScrollVelocityList);
                if (averageVelocity <= this.mRefreshRateInfo.getTouchScrollVelocityThreshold() && this.mTouchScrollSpeed == 0 && this.mActiveRefreshScenario.getTouchScrollEnabled()) {
                    updateRefreshRateWhenScroll(this.mActiveRefreshScenario, this.mRefreshRateInfo.getSlowScrollRefreshRate());
                    this.mTouchScrollSpeed = 1;
                    this.mLastRefreshChangeTime = duration;
                    this.mActiveRefreshScenario.setCurrentRefreshrate(this.mRefreshRateInfo.getSlowScrollRefreshRate());
                    LogUtil.mLogi(TAG, "Touch scroll speed change : High -> Low");
                    return;
                } else if (averageVelocity > this.mRefreshRateInfo.getTouchScrollVelocityThreshold()) {
                    updateRefreshRateWhenScroll(this.mActiveRefreshScenario, this.mRefreshRateInfo.getMaxFlingSupportedRefreshRate());
                    this.mTouchScrollSpeed = 0;
                    this.mActiveRefreshScenario.setCurrentRefreshrate(this.mRefreshRateInfo.getMaxFlingSupportedRefreshRate());
                    this.mLastRefreshChangeTime = duration;
                    LogUtil.mLogi(TAG, "Touch scroll speed change : Low -> High");
                    return;
                } else {
                    return;
                }
            }
            return;
        }
        this.mTouchScrollVelocityList.add(Float.valueOf(Math.abs(velocity)));
    }

    private float calculateAverage(ArrayList<Float> velocityList) {
        float total = RefreshRateInfo.DECELERATION_RATE;
        Iterator<Float> it = velocityList.iterator();
        while (it.hasNext()) {
            float velocity = it.next().floatValue();
            total += velocity;
        }
        return total / 5.0f;
    }

    private void updateRefreshRateWhenScroll(RefreshRateScenario refreshRateScenario, int refreshRate) {
        LogUtil.mLogd(TAG, "updateRefreshRateWhenScroll oldRefreshRate = " + refreshRateScenario.getCurrentRefreshrate() + " New RefreshRate = " + refreshRate);
        if (updateRefreshInternal(refreshRateScenario, refreshRate, 0, 0)) {
            refreshRateScenario.setCurrentRefreshrate(refreshRate);
        }
    }

    public void onScrollStateChange(int scrollState, float velocity) {
        if (this.mActiveRefreshScenario == null) {
            return;
        }
        LogUtil.mLogd(TAG, "onScrollStateChange velocity = " + velocity + " scrollState = " + scrollState);
        switch (scrollState) {
            case 0:
                this.mActiveRefreshScenario.setLastTouchDownTime(SystemClock.uptimeMillis());
                if (SystemClock.uptimeMillis() - this.mActiveRefreshScenario.getLastFlingFinishTime() >= 150) {
                    this.mActiveRefreshScenario.setTouchScrollEnabled(false);
                    LogUtil.mLogd(TAG, "onScrollStateChange List stop by other reason");
                } else {
                    this.mActiveRefreshScenario.setTouchScrollEnabled(true);
                    LogUtil.mLogd(TAG, "onScrollStateChange List stop by touch event");
                }
                this.mWorkerHandler.removeMessages(0);
                LogUtil.mLogd(TAG, "onScrollStateChange List touch scroll state = " + this.mActiveRefreshScenario.getTouchScrollEnabled());
                this.mLastRefreshChangeTime = 0L;
                this.mTouchScrollSpeed = 0;
                updateScrollAction(this.mActiveRefreshScenario, REFRESH_RATE_TOUCH_SCROLL_DOWN);
                return;
            case 1:
            case 3:
                if (Math.abs(velocity) <= 100.0f) {
                    updateScrollAction(this.mActiveRefreshScenario, Config.TOUCH_HINT_DURATION_DEFAULT);
                    this.mActiveRefreshScenario.setTouchScrollEnabled(false);
                    this.mActiveRefreshScenario = null;
                    return;
                }
                return;
            case 2:
            default:
                return;
        }
    }

    private void updateScrollAction(RefreshRateScenario refreshRateScenario, int refreshRate) {
        updateRefreshInternal(refreshRateScenario, refreshRate, 0, 0);
    }

    private boolean updateRefreshInternal(RefreshRateScenario refreshRateScenario, int refreshRate, int scenarioAction, int scenarioType) {
        Context activityContext;
        LogUtil.mLogd(TAG, "updateRefreshInternal RefreshRate = " + refreshRate);
        Context activityContext2 = refreshRateScenario.getScenarioContext();
        if (activityContext2 != null) {
            activityContext = activityContext2;
        } else {
            Context activityContext3 = ActivityInfo.getInstance().getContext();
            if (activityContext3 == null) {
                return false;
            }
            activityContext = activityContext3;
        }
        if (activityContext instanceof Activity) {
            try {
                Window window = ((Activity) activityContext).getWindow();
                ViewRootImpl viewRootImpl = window.getDecorView().getViewRootImpl();
                if (viewRootImpl != null) {
                    SurfaceControl surfaceControl = viewRootImpl.getSurfaceControl();
                    if (surfaceControl.isValid()) {
                        LogUtil.mLogd(TAG, "Update RefreshRate = " + refreshRate + " scenarioAction = " + scenarioAction + " scenarioType = " + scenarioType);
                        LogUtil.traceBegin("Refreshrate Change =" + refreshRate + " action = " + scenarioAction + " type = " + scenarioType);
                        WindowManagerGlobal.getWindowSession().setRefreshRate(surfaceControl, refreshRate, scenarioAction, scenarioType, this.mRefreshRateInfo.getCurrentActivityName(), this.mRefreshRateInfo.getPackageName());
                        LogUtil.traceEnd();
                        return true;
                    }
                    LogUtil.mLogd(TAG, "Refreshrate change failed : SurfaceControl is invalid");
                } else {
                    LogUtil.mLogd(TAG, "Refreshrate change failed : ViewRootImpl == null ");
                }
            } catch (RemoteException e) {
                LogUtil.mLogd(TAG, "Refreshrate change failed, e = " + e.toString());
            }
        } else {
            LogUtil.mLogd(TAG, "Refreshrate change failed : ActivityContext == null");
        }
        return false;
    }

    private int getTouchScrollState() {
        int currentRefreshRate = (int) Util.getRefreshRate();
        if (currentRefreshRate == this.mRefreshRateInfo.getMaxFlingSupportedRefreshRate()) {
            return 0;
        }
        if (currentRefreshRate == this.mRefreshRateInfo.getSlowScrollRefreshRate()) {
            return 1;
        }
        return -1;
    }

    public void onVoiceDialogEvent(boolean isVoiceShow) {
        LogUtil.mLogi(TAG, "Voice Dialog Show = " + isVoiceShow);
        if (isVoiceShow) {
            updateVoiceDialogRefreshRate(1);
        } else {
            updateVoiceDialogRefreshRate(2);
        }
    }

    private void updateVoiceDialogRefreshRate(int scenarioAction) {
        Window window;
        LogUtil.mLogd(TAG, "updateVoiceDialogRefreshRate action = " + scenarioAction);
        LogUtil.traceBegin("Voice Action=" + scenarioAction);
        Context activityContext = ActivityInfo.getInstance().getContext();
        if (activityContext != null && (activityContext instanceof Activity) && (window = ((Activity) activityContext).getWindow()) != null) {
            WindowManager.LayoutParams windowLayoutParams = window.getAttributes();
            windowLayoutParams.mMSyncScenarioType = 2;
            windowLayoutParams.mMSyncScenarioAction = scenarioAction;
            window.setAttributes(windowLayoutParams);
        }
        LogUtil.traceEnd();
    }

    public void onIMEInit(Window window) {
        if (window == null) {
            return;
        }
        WeakReference<Window> weakReference = this.mImeWindow;
        if (weakReference != null) {
            weakReference.clear();
        }
        this.mImeWindow = new WeakReference<>(window);
        LogUtil.mLogd(TAG, "ime init");
    }

    private Window getIMEWindow() {
        WeakReference<Window> weakReference = this.mImeWindow;
        if (weakReference == null) {
            return null;
        }
        return weakReference.get();
    }

    public void onIMEVisibilityChange(boolean isIMEShow) {
        Window imeWindow = getIMEWindow();
        if (imeWindow != null) {
            LogUtil.mLogi(TAG, "IME show = " + isIMEShow);
            if (isIMEShow) {
                updateIMERefreshRate(1, imeWindow);
            } else {
                updateIMERefreshRate(2, imeWindow);
            }
        }
    }

    private void updateIMERefreshRate(int action, Window window) {
        LogUtil.mLogd(TAG, "updateIMERefreshRate action = " + action);
        LogUtil.traceBegin("Refreshrate Change : IME");
        WindowManager.LayoutParams windowLayoutParams = window.getAttributes();
        windowLayoutParams.mMSyncScenarioType = 1;
        windowLayoutParams.mMSyncScenarioAction = action;
        window.setAttributes(windowLayoutParams);
        LogUtil.traceEnd();
    }
}
