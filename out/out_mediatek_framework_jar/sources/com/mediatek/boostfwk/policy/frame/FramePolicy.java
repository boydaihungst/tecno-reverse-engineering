package com.mediatek.boostfwk.policy.frame;

import android.os.Message;
import android.os.Trace;
import com.mediatek.boostfwk.info.ActivityInfo;
import com.mediatek.boostfwk.info.ScrollState;
import com.mediatek.boostfwk.policy.frame.BaseFramePolicy;
import com.mediatek.boostfwk.policy.refreshrate.RefreshRateInfo;
import com.mediatek.boostfwk.policy.scroll.ScrollPolicy;
import com.mediatek.boostfwk.utils.Config;
import com.mediatek.boostfwk.utils.LogUtil;
/* loaded from: classes.dex */
public class FramePolicy extends BaseFramePolicy {
    private static final double CHECK_POINT = 0.5d;
    private static final int FRAME_STEP_BASIC = -1000;
    public static final int MSG_FRAME_BEGIN = 1;
    public static final int MSG_FRAME_END = 2;
    public static final int MSG_NO_DRAW = 4;
    public static final int MSG_STEP_CHECK = 3;
    public static final int MSG_TRAVERSAL_RESUCE_CHECK = 5;
    private static final int NON_FRAME_STEP = -1000;
    private static final int NON_RENDER_THREAD_TID = -1;
    private static final double NO_DRAW_FRAME_VSYNC_RATIO = 0.1d;
    private static final int PERF_RES_FPS_FBT_RESCUE_SBE_RESCUE = 33802752;
    private static final int PERF_RES_FPS_FPSGO_STOP_BOOST = 33571328;
    private static final int RECEIVE_VSYNC_TO_INPUT = -999;
    private static final long RECUME_FRAME_ID = -10000;
    private static final String sTHREAD_NAME = "FramePolicy";
    private static FramePolicy sInstance = null;
    private static Object sLock = new Object();
    private static final int mReleaseFPSDuration = Config.sSCROLLING_HINT_DURATION;
    private static float mFrameIntervalTime = RefreshRateInfo.DECELERATION_RATE;
    private static float mLimitVsyncTime = RefreshRateInfo.DECELERATION_RATE;
    private static int mFrameStep = BaseFramePolicy.WorkerHandler.MSG_INIT_CORE_SERVICE;
    private static boolean isNoDraw = true;
    private static boolean mIsDoframeCheck = false;
    private static boolean mAnimAcquiredLock = false;
    private static boolean mTranversalAcquiredLock = false;
    private static boolean isTranversalDraw = false;
    private static boolean isAnimationStepEnd = false;
    private static boolean isTraversalStepEnd = false;
    private final int SBE_RESUCE_MODE_END = 0;
    private final int SBE_RESUCE_MODE_START = 1;
    private final int SBE_RESUCE_MODE_TO_QUEUE_END = 2;
    private int curFrameRescueMode = 2;
    private long mFrameId = -1;
    private int mRenderThreadTid = -1;
    private long frameStartTime = -1;
    private boolean underRescue = false;

    @Override // com.mediatek.boostfwk.policy.frame.BaseFramePolicy
    protected void handleMessageInternal(Message msg) {
        switch (msg.what) {
            case 1:
                doFrameHintInternel(true, ((Long) msg.obj).longValue());
                return;
            case 2:
                doFrameHintInternel(false, ((Long) msg.obj).longValue());
                return;
            case 3:
                doFrameStepHintInternel(mFrameStep);
                return;
            case 4:
                frameDraw(false);
                return;
            case 5:
                traversalRescueChecker();
                return;
            default:
                return;
        }
    }

    public static FramePolicy getInstance() {
        if (sInstance == null) {
            synchronized (sLock) {
                if (sInstance == null) {
                    sInstance = new FramePolicy();
                }
            }
        }
        return sInstance;
    }

    @Override // com.mediatek.boostfwk.policy.frame.BaseFramePolicy
    public boolean initLimitTime(float frameIntervalTime) {
        if (frameIntervalTime > RefreshRateInfo.DECELERATION_RATE && frameIntervalTime != mFrameIntervalTime) {
            mFrameIntervalTime = frameIntervalTime;
            mLimitVsyncTime = (float) ((frameIntervalTime * CHECK_POINT) - CHECK_POINT);
        }
        return mListenFrameHint && mFrameIntervalTime != RefreshRateInfo.DECELERATION_RATE;
    }

    @Override // com.mediatek.boostfwk.policy.frame.BaseFramePolicy
    public void doFrameHint(boolean isBegin, long frameId) {
        if (!this.mCoreServiceReady) {
            return;
        }
        if (Config.isBoostFwkLogEnable()) {
            LogUtil.mLogd(sTHREAD_NAME, "vsync is begin = " + isBegin);
        }
        mIsDoframeCheck = isBegin;
        if (isBegin) {
            setFrameStep(RECEIVE_VSYNC_TO_INPUT);
            this.mWorkerHandler.sendMessage(this.mWorkerHandler.obtainMessage(1, Long.valueOf(frameId)));
            return;
        }
        if (!isNoDraw && isTranversalDraw) {
            this.mWorkerHandler.sendMessageDelayed(this.mWorkerHandler.obtainMessage(4, null), (long) drawFrameDelayTime());
        }
        this.mWorkerHandler.sendMessage(this.mWorkerHandler.obtainMessage(2, Long.valueOf(frameId)));
        if (mDisableFrameRescue) {
            mListenFrameHint = false;
        }
    }

    @Override // com.mediatek.boostfwk.policy.frame.BaseFramePolicy
    public void doFrameStepHint(boolean isBegin, int step) {
        if (this.mCoreServiceReady) {
            if (isBegin) {
                setFrameStep(step);
                if (step == 3) {
                    mTranversalAcquiredLock = true;
                    this.mWorkerHandler.sendMessage(this.mWorkerHandler.obtainMessage(3, null));
                    return;
                }
                return;
            }
            if (step == 1) {
                isAnimationStepEnd = true;
            }
            if (step == 3) {
                isTraversalStepEnd = true;
            }
        }
    }

    private void setFrameStep(int step) {
        if (step > mFrameStep) {
            mFrameStep = step;
        }
    }

    private void doFrameHintInternel(boolean isBegin, long frameId) {
        if (isBegin) {
            this.mFrameId = frameId;
            if (mLimitVsyncTime != RefreshRateInfo.DECELERATION_RATE) {
                if (Config.isBoostFwkLogEnable()) {
                    LogUtil.mLogd(sTHREAD_NAME, "scrolling!! try check animation and draw state.");
                }
                this.mWorkerHandler.sendMessageDelayed(this.mWorkerHandler.obtainMessage(3), mLimitVsyncTime);
                this.mWorkerHandler.sendMessageDelayed(this.mWorkerHandler.obtainMessage(5), mFrameIntervalTime);
                return;
            }
            return;
        }
        this.mWorkerHandler.removeMessages(3, null);
        this.mWorkerHandler.removeMessages(5, null);
        mAnimAcquiredLock = false;
        mTranversalAcquiredLock = false;
        isTranversalDraw = false;
        isAnimationStepEnd = false;
        mFrameStep = BaseFramePolicy.WorkerHandler.MSG_INIT_CORE_SERVICE;
        this.mFrameId = -1L;
        this.underRescue = false;
        isTraversalStepEnd = false;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private void doFrameStepHintInternel(int step) {
        if (!mIsDoframeCheck && step == -1000) {
            return;
        }
        switch (step) {
            case RECEIVE_VSYNC_TO_INPUT /* -999 */:
            case 0:
            case 1:
                if (!isAnimationStepEnd) {
                    powerHintForRender(PERF_RES_FPS_FBT_RESCUE_SBE_RESCUE, "animation end, curStep=" + step);
                }
                if (!mAnimAcquiredLock && step != RECEIVE_VSYNC_TO_INPUT) {
                    if (Config.isBoostFwkLogEnable()) {
                        LogUtil.mLogd(sTHREAD_NAME, "input/anim hint drop, enable rescue!");
                    }
                    frameDraw(true);
                    mAnimAcquiredLock = true;
                    return;
                }
                break;
            case 3:
                break;
            default:
                return;
        }
        if (mTranversalAcquiredLock && !mAnimAcquiredLock) {
            if (Config.isBoostFwkLogEnable()) {
                LogUtil.mLogd(sTHREAD_NAME, "traversal step, enable rescue!");
            }
            frameDraw(true);
            mTranversalAcquiredLock = false;
            mAnimAcquiredLock = true;
        }
    }

    private void traversalRescueChecker() {
        if (!this.underRescue && mFrameStep == 3 && !isTraversalStepEnd) {
            powerHintForRender(PERF_RES_FPS_FBT_RESCUE_SBE_RESCUE, "traversal over vsync, curStep=" + mFrameStep);
        }
    }

    private void frameDraw(boolean isDraw) {
        if (isDraw && this.mFrameId == -1) {
            if (Config.isBoostFwkLogEnable()) {
                LogUtil.mLogd(sTHREAD_NAME, "frame clear when rescue. mFrameId = " + this.mFrameId);
            }
        } else if (isDraw) {
            if (isNoDraw) {
                if (Config.isBoostFwkLogEnable()) {
                    Trace.traceBegin(8L, "Draw, notify FPSGO draw" + this.mFrameId);
                }
                if (Config.isBoostFwkLogEnable()) {
                    Trace.traceEnd(8L);
                }
            }
            this.mWorkerHandler.removeMessages(4, null);
            isNoDraw = false;
            isTranversalDraw = true;
        } else {
            powerHintForRender(PERF_RES_FPS_FPSGO_STOP_BOOST, "STOP: No draw");
            isNoDraw = true;
        }
    }

    private void powerHintForRender(int cmd, String tagMsg) {
        int renderThreadTid = getRenderThreadTid();
        if (Config.isBoostFwkLogEnable()) {
            Trace.traceBegin(8L, "hint for [" + tagMsg + "] render id = " + renderThreadTid);
        }
        switch (cmd) {
            case PERF_RES_FPS_FPSGO_STOP_BOOST /* 33571328 */:
                int[] perf_lock_rsc = {cmd, renderThreadTid};
                perfLockAcquire(perf_lock_rsc);
                ScrollPolicy.getInstance().disableMTKScrollingPolicy(true);
                break;
            case PERF_RES_FPS_FBT_RESCUE_SBE_RESCUE /* 33802752 */:
                this.underRescue = true;
                this.mPowerHalWrap.mtkNotifySbeRescue(renderThreadTid, this.curFrameRescueMode, 50, RECUME_FRAME_ID);
                break;
            default:
                if (Config.isBoostFwkLogEnable()) {
                    LogUtil.mLogd(sTHREAD_NAME, "not surpport for cmd = " + cmd);
                    break;
                }
                break;
        }
        if (Config.isBoostFwkLogEnable()) {
            Trace.traceEnd(8L);
        }
    }

    private double drawFrameDelayTime() {
        if (mFrameIntervalTime == RefreshRateInfo.DECELERATION_RATE) {
            return -1.0d;
        }
        float refreshRate = ScrollState.getRefreshRate();
        double delayCheckTime = mFrameIntervalTime * refreshRate * NO_DRAW_FRAME_VSYNC_RATIO;
        return delayCheckTime;
    }

    private int getRenderThreadTid() {
        if (this.mRenderThreadTid == -1) {
            this.mRenderThreadTid = ActivityInfo.getInstance().getRenderThreadTid();
        }
        return this.mRenderThreadTid;
    }

    private void perfLockAcquire(int[] resList) {
        if (this.mPowerHalService != null) {
            this.mPowerHandle = this.mPowerHalService.perfLockAcquire(this.mPowerHandle, 0, resList);
            this.mPowerHalService.perfLockRelease(this.mPowerHandle);
        }
    }
}
