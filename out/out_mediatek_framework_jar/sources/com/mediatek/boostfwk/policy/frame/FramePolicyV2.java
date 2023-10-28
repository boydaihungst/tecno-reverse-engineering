package com.mediatek.boostfwk.policy.frame;

import android.content.Context;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.view.DisplayEventReceiver;
import android.view.ThreadedRenderer;
import com.mediatek.boostfwk.info.ActivityInfo;
import com.mediatek.boostfwk.info.ScrollState;
import com.mediatek.boostfwk.policy.refreshrate.RefreshRateInfo;
import com.mediatek.boostfwk.policy.scroll.ScrollPolicy;
import com.mediatek.boostfwk.utils.Config;
import com.mediatek.boostfwk.utils.LogUtil;
import com.mediatek.boostfwk.utils.Util;
/* loaded from: classes.dex */
public class FramePolicyV2 extends BaseFramePolicy implements ScrollState.RefreshRateChangedListener, ActivityInfo.ActivityChangeListener {
    private static final double CHECK_POINT = 0.5d;
    private static final long CHECK_TIME_OFFSET_THRESHOLD_NS = 500000;
    private static final long DEFAULT_FRAME_ID = -2147483648L;
    private static final long DEFAULT_FRAME_TIME = -1;
    private static final int FRAME_STEP_BASIC = -1;
    private static final int FRAME_STEP_DEFAULT = -1;
    private static final int FRAME_STEP_DO_FRAME_ANIMATION = 3;
    private static final int FRAME_STEP_DO_FRAME_INPUT = 2;
    private static final int FRAME_STEP_DO_FRAME_TRAVERSAL = 4;
    private static final int FRAME_STEP_VSYNC_FOR_APP_TO_INPUT = 1;
    private static final int FRAME_STEP_VSYNC_FOR_SBE_TO_APP_VSYNC = 0;
    private static final int MAX_WAITING_FRAME_COUNT = 5;
    public static final int MSG_DELAY_STOP_HWUI_HINT = 9;
    public static final int MSG_FRAME_BEGIN = 1;
    public static final int MSG_FRAME_END = 2;
    public static final int MSG_NO_DRAW = 4;
    public static final int MSG_REQUEST_VSYNC = 8;
    public static final int MSG_RESCUE_HALF_VSYNC_CHECK = 3;
    public static final int MSG_RESCUE_TIME_OUT = 6;
    public static final int MSG_RESUCE_ONE_VSYNC_CHECK = 5;
    public static final int MSG_UPDATE_RESCUE_FRAME_ID = 7;
    private static final long NANOS_PER_MS = 1000000;
    private static final int NON_RENDER_THREAD_TID = -1;
    private static final double NO_DRAW_FRAME_VSYNC_RATIO = 0.1d;
    private static final int PERF_RES_FPS_FBT_RESCUE_SBE_RESCUE = 33802752;
    private static final int PERF_RES_FPS_FPSGO_STOP_BOOST = 33571328;
    private static final int PERF_RES_UX_SBE_RESCUE_ENHANS = 50348800;
    private static final long PREFETCHER_FRAME_ID = -1;
    private static final int RESCUE_COUNT_DOWN_THRESHOLD = 1;
    private static final boolean RESCUE_TO_NEXT_FRAME = true;
    private static final boolean ENBALE_FRAME_RESCUE = Config.isEnableFrameRescue();
    private static final Object LOCK = new Object();
    private static FramePolicyV2 mInstance = null;
    private static final int mReleaseFPSDuration = Config.sSCROLLING_HINT_DURATION;
    private static float mFrameIntervalTime = RefreshRateInfo.DECELERATION_RATE;
    private static float mLimitVsyncTime = RefreshRateInfo.DECELERATION_RATE;
    private static long mDelayStopHWUIHintTime = 0;
    private static int mFrameStep = -1;
    private static boolean mIsAnimationStepEnd = false;
    private static boolean mIsTraversalStepEnd = false;
    private static boolean mIsScrolling = false;
    private final int SBE_RESUCE_MODE_END = 0;
    private final int SBE_RESUCE_MODE_START = 1;
    private final int SBE_RESUCE_MODE_TO_QUEUE_END = 2;
    private int mCurFrameRescueMode = 1;
    private int mRescueStrength = 50;
    private boolean mUpdateStrength = false;
    private int mUnlockPowerHandle = 0;
    private int mRenderThreadTid = -1;
    private DisplayEventReceiver mDisplayEventReceiver = null;
    private ScrollingFramePrefetcher mScrollingFramePrefetcher = null;
    private boolean mRescueDoFrame = false;
    private boolean mRescueNextFrame = false;
    private boolean mRescueWhenWaitNextVsync = false;
    private boolean mHasNextFrame = false;
    private long mCurFrameStartTimeNanos = -1;
    private long mCurFrameId = DEFAULT_FRAME_ID;
    private long mLastFrameEndFrameId = DEFAULT_FRAME_ID;
    private long mRescuingFrameId = DEFAULT_FRAME_ID;
    private long mVsyncFrameId = DEFAULT_FRAME_ID;
    private long mVsyncTimeNanos = -1;

    @Override // com.mediatek.boostfwk.policy.frame.BaseFramePolicy
    protected void handleMessageInternal(Message msg) {
        switch (msg.what) {
            case 1:
            case 4:
            default:
                return;
            case 2:
                onDoFrameEndInternal(this.mLastFrameEndFrameId);
                return;
            case 3:
                halfVsyncRescueCheck();
                return;
            case 5:
                oneVsyncRescueCheck();
                return;
            case 6:
                shutdownSBERescue("time out");
                return;
            case 7:
                updateRescueFrameId();
                return;
            case 8:
                requestVsyncInternal();
                return;
            case 9:
                dleayStopHwuiHint();
                return;
        }
    }

    public static FramePolicyV2 getInstance() {
        if (mInstance == null) {
            synchronized (LOCK) {
                if (mInstance == null) {
                    mInstance = new FramePolicyV2();
                }
            }
        }
        return mInstance;
    }

    private FramePolicyV2() {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.mediatek.boostfwk.policy.frame.BaseFramePolicy
    public void initCoreServiceInternal() {
        this.mDisplayEventReceiver = new DisplayEventReceiverImpl(this.mWorkerHandler.getLooper(), 0);
        super.initCoreServiceInternal();
    }

    @Override // com.mediatek.boostfwk.policy.frame.BaseFramePolicy
    public boolean initLimitTime(float frameIntervalTime) {
        if (frameIntervalTime > RefreshRateInfo.DECELERATION_RATE && frameIntervalTime != mFrameIntervalTime) {
            mFrameIntervalTime = frameIntervalTime;
            mLimitVsyncTime = (float) ((frameIntervalTime * CHECK_POINT) - CHECK_POINT);
            mDelayStopHWUIHintTime = 5.0f * frameIntervalTime;
            initialRescueStrengthOnce(ScrollState.getRefreshRate());
        }
        if (ENBALE_FRAME_RESCUE && mListenFrameHint && mFrameIntervalTime != RefreshRateInfo.DECELERATION_RATE) {
            return RESCUE_TO_NEXT_FRAME;
        }
        return false;
    }

    @Override // com.mediatek.boostfwk.policy.frame.BaseFramePolicy
    public void doFrameHint(boolean isBegin, long frameId) {
        if (!this.mCoreServiceReady) {
            return;
        }
        LogUtil.mLogd("FramePolicy", "vsync is begin = " + isBegin + " frame=" + frameId);
        if (isBegin) {
            onDoFrameBegin(frameId);
        } else {
            onDoFrameEnd();
        }
    }

    private void onDoFrameBegin(long frameId) {
        LogUtil.traceBegin("onDoFrameBegin=" + frameId);
        updateBasicFrameInfo(frameId, SystemClock.uptimeNanos());
        setFrameStep(1);
        this.mRescuingFrameId = DEFAULT_FRAME_ID;
        this.mRescueDoFrame = false;
        if (this.mRescueWhenWaitNextVsync || (this.mRescueNextFrame && !mDisableFrameRescue)) {
            this.mWorkerHandler.sendMessageAtFrontOfQueue(this.mWorkerHandler.obtainMessage(7));
        }
        LogUtil.traceEnd();
    }

    private void onDoFrameEnd() {
        LogUtil.traceBegin("onDoFrameEnd=" + this.mCurFrameId);
        mFrameStep = -1;
        this.mLastFrameEndFrameId = this.mCurFrameId;
        updateBasicFrameInfo(DEFAULT_FRAME_ID, -1L);
        this.mWorkerHandler.sendMessageAtFrontOfQueue(this.mWorkerHandler.obtainMessage(2));
        if (this.mHasNextFrame && !mDisableFrameRescue) {
            requestVsync();
            mListenFrameHint = RESCUE_TO_NEXT_FRAME;
        }
        this.mHasNextFrame = false;
        if (mDisableFrameRescue) {
            mListenFrameHint = false;
            this.mWorkerHandler.sendEmptyMessageDelayed(9, mDelayStopHWUIHintTime);
        }
        LogUtil.traceEnd();
    }

    private void dleayStopHwuiHint() {
        ThreadedRenderer render = getThreadedRenderer();
        if (render != null) {
            ThreadedRenderer.needFrameCompleteHint(false);
        }
    }

    private void updateBasicFrameInfo(long curFrameId, long frameTimeNanos) {
        this.mCurFrameId = curFrameId;
        this.mCurFrameStartTimeNanos = frameTimeNanos;
    }

    @Override // com.mediatek.boostfwk.policy.frame.BaseFramePolicy
    public void doFrameStepHint(boolean isBegin, int step) {
        if (!this.mCoreServiceReady) {
            return;
        }
        if (isBegin) {
            setFrameStep(mappingStepForDoFrame(step));
        } else if (step == 1) {
            mIsAnimationStepEnd = RESCUE_TO_NEXT_FRAME;
        } else if (step == 3) {
            mIsTraversalStepEnd = RESCUE_TO_NEXT_FRAME;
        }
    }

    private int mappingStepForDoFrame(int step) {
        switch (step) {
            case 0:
                return 2;
            case 1:
                return 3;
            case 2:
            default:
                return -1;
            case 3:
                return 4;
        }
    }

    private void setFrameStep(int step) {
        if (step > mFrameStep) {
            mFrameStep = step;
            LogUtil.trace("new step=" + step + " frameId=" + this.mCurFrameId);
        }
    }

    @Override // com.mediatek.boostfwk.policy.frame.BaseFramePolicy
    public void onRequestNextVsync() {
        if (!mListenFrameHint || !this.mCoreServiceReady) {
            return;
        }
        if (this.mScrollingFramePrefetcher == null) {
            this.mScrollingFramePrefetcher = ScrollingFramePrefetcher.getInstance();
        }
        if (mFrameStep < 1 && !this.mScrollingFramePrefetcher.isPreAnimationRunning()) {
            requestVsync();
        } else {
            this.mHasNextFrame = RESCUE_TO_NEXT_FRAME;
        }
    }

    public void requestVsync() {
        if (!this.mCoreServiceReady) {
            return;
        }
        this.mWorkerHandler.removeMessages(8);
        this.mWorkerHandler.sendEmptyMessage(8);
    }

    @Override // com.mediatek.boostfwk.policy.frame.BaseFramePolicy
    public void onScrollStateChange(boolean scrolling) {
        super.onScrollStateChange(scrolling);
        ThreadedRenderer render = mInstance.getThreadedRenderer();
        LogUtil.mLogd("FramePolicy", "onScrollStateChange scroll=" + scrolling + " " + render);
        if (mListenFrameHint) {
            this.mWorkerHandler.removeMessages(9);
            if (render != null) {
                ThreadedRenderer.needFrameCompleteHint((boolean) RESCUE_TO_NEXT_FRAME);
            }
        }
    }

    private void onDoFrameEndInternal(long frameId) {
        LogUtil.traceBegin("onDoFrameEndInternal frameId= " + frameId);
        this.mWorkerHandler.removeMessages(3);
        this.mWorkerHandler.removeMessages(5);
        mIsAnimationStepEnd = false;
        if (!mIsTraversalStepEnd && (this.mRescueDoFrame || this.mRescueWhenWaitNextVsync)) {
            shutdownSBERescue("frame end-no draw");
        }
        this.mRescueWhenWaitNextVsync = false;
        mIsTraversalStepEnd = false;
        LogUtil.traceEnd();
    }

    private void requestVsyncInternal() {
        LogUtil.traceBegin("requestVsyncInternal frameId= " + this.mCurFrameId);
        DisplayEventReceiver displayEventReceiver = this.mDisplayEventReceiver;
        if (displayEventReceiver != null) {
            displayEventReceiver.scheduleVsync();
        }
        LogUtil.traceEnd();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onVsyncInternal(long frameId) {
        if (frameId < this.mCurFrameId || this.mLastFrameEndFrameId >= frameId) {
            return;
        }
        this.mVsyncFrameId = frameId;
        this.mVsyncTimeNanos = SystemClock.uptimeNanos();
        if (mFrameStep != 0) {
            setFrameStep(0);
            rescueCheck();
        }
    }

    private void rescueCheck() {
        LogUtil.traceBegin("rescueCheck frameId= " + this.mVsyncFrameId + " vsyncTime=" + this.mVsyncTimeNanos + " frameStartTime=" + this.mCurFrameStartTimeNanos);
        long vsyncTimeNanos = this.mVsyncTimeNanos;
        long curFrameStartTimeNanos = this.mCurFrameStartTimeNanos;
        float halfCheckTime = mLimitVsyncTime;
        float oneVsyncCheckTime = mFrameIntervalTime;
        if (this.mCurFrameStartTimeNanos > 0) {
            long frameOffset = vsyncTimeNanos - curFrameStartTimeNanos;
            if (frameOffset > CHECK_TIME_OFFSET_THRESHOLD_NS) {
                halfCheckTime = adjustCheckTime(halfCheckTime, (float) frameOffset);
                oneVsyncCheckTime = adjustCheckTime(oneVsyncCheckTime, (float) frameOffset);
            }
        }
        this.mWorkerHandler.sendMessageDelayed(this.mWorkerHandler.obtainMessage(3), halfCheckTime);
        this.mWorkerHandler.sendMessageDelayed(this.mWorkerHandler.obtainMessage(5), oneVsyncCheckTime);
        this.mWorkerHandler.removeMessages(6);
        LogUtil.traceEnd();
    }

    private float adjustCheckTime(float defaultCheckTime, float offset) {
        float checkTime = ((defaultCheckTime * 1000000.0f) - offset) / 1000000.0f;
        return checkTime > RefreshRateInfo.DECELERATION_RATE ? checkTime : RefreshRateInfo.DECELERATION_RATE;
    }

    private void halfVsyncRescueCheck() {
        LogUtil.traceBeginAndLog("FramePolicy", "halfVsyncRescueCheck=" + mFrameStep + " frame=" + this.mCurFrameId);
        switch (mFrameStep) {
            case 0:
                this.mRescueWhenWaitNextVsync = RESCUE_TO_NEXT_FRAME;
                doSBERescue("waiting vsync");
                break;
            case 1:
            case 2:
            case 3:
                if (!mIsAnimationStepEnd) {
                    this.mRescueDoFrame = RESCUE_TO_NEXT_FRAME;
                    doSBERescue("animation end");
                    break;
                }
                break;
        }
        LogUtil.traceEnd();
    }

    private void oneVsyncRescueCheck() {
        int i;
        boolean z = this.mRescueDoFrame;
        if (!z && mFrameStep == 4 && !mIsTraversalStepEnd) {
            this.mRescueDoFrame = RESCUE_TO_NEXT_FRAME;
            doSBERescue("traversal over vsync");
            this.mRescueNextFrame = RESCUE_TO_NEXT_FRAME;
        } else if (z && (i = mFrameStep) <= 4 && i > -1) {
            this.mRescueNextFrame = RESCUE_TO_NEXT_FRAME;
        }
        LogUtil.mLogd("FramePolicy", "oneVsyncRescueCheck mFrameStep=" + mFrameStep + " mRescueNextFrame=" + this.mRescueNextFrame + " mRescueDoFrame=" + this.mRescueDoFrame + " frameId=" + this.mCurFrameId);
    }

    private void updateRescueFrameId() {
        if (this.mRescueWhenWaitNextVsync) {
            this.mRescueWhenWaitNextVsync = false;
            doSBERescue("update frame id");
            this.mRescueDoFrame = RESCUE_TO_NEXT_FRAME;
        } else if (this.mRescueNextFrame) {
            this.mRescueNextFrame = false;
            doSBERescue("update frame id");
            this.mRescueDoFrame = RESCUE_TO_NEXT_FRAME;
        }
    }

    private void frameDraw(boolean isDraw) {
        if (isDraw) {
            this.mWorkerHandler.removeMessages(4, null);
            this.mWorkerHandler.sendEmptyMessageDelayed(4, (long) drawFrameDelayTime());
            return;
        }
        powerHintForRender(PERF_RES_FPS_FPSGO_STOP_BOOST, "STOP: No draw");
    }

    private void doSBERescue(String tagMsg) {
        long frameId = this.mCurFrameId;
        if (frameId == DEFAULT_FRAME_ID && mFrameStep != 0) {
            LogUtil.traceAndLog("FramePolicy", "do not rescue beacause frameID=-1");
            return;
        }
        this.mRescuingFrameId = frameId;
        if (this.mUpdateStrength) {
            this.mUpdateStrength = false;
            powerHintForRender(PERF_RES_UX_SBE_RESCUE_ENHANS, "change to " + this.mRescueStrength);
        }
        this.mCurFrameRescueMode = 1;
        powerHintForRender(PERF_RES_FPS_FBT_RESCUE_SBE_RESCUE, tagMsg + " curStep=" + mFrameStep + " mode=" + this.mCurFrameRescueMode);
        this.mWorkerHandler.sendEmptyMessageDelayed(6, (long) drawFrameDelayTime());
    }

    private void shutdownSBERescue(String tagMsg) {
        this.mCurFrameRescueMode = 0;
        powerHintForRender(PERF_RES_FPS_FBT_RESCUE_SBE_RESCUE, "shutdown " + tagMsg + " curStep=" + mFrameStep + " mode=" + this.mCurFrameRescueMode);
        clearRescueInfo();
    }

    private void clearRescueInfo() {
        this.mRescuingFrameId = DEFAULT_FRAME_ID;
        this.mRescueWhenWaitNextVsync = false;
        this.mRescueDoFrame = false;
        this.mRescueNextFrame = false;
        this.mWorkerHandler.removeMessages(6);
    }

    private void initialRescueStrengthOnce(float refreshRate) {
        int old = this.mRescueStrength;
        int result = this.mRescueStrength;
        if (refreshRate <= 65.0f) {
            result = 50;
        } else if (refreshRate < 95.0f) {
            result = 50;
        }
        if (result != old) {
            this.mRescueStrength = result;
            this.mUpdateStrength = RESCUE_TO_NEXT_FRAME;
        }
    }

    @Override // com.mediatek.boostfwk.info.ScrollState.RefreshRateChangedListener
    public void onDisplayRefreshRateChanged(int displayId, float refreshRate, float frameIntervalNanos) {
        initialRescueStrengthOnce(refreshRate);
    }

    private void powerHintForRender(int cmd, String tagMsg) {
        int renderThreadTid = getRenderThreadTid();
        if (renderThreadTid != Integer.MIN_VALUE) {
            LogUtil.traceBeginAndMLogd("FramePolicy", "hint for [" + tagMsg + "] renderId=" + renderThreadTid + " frameId=" + this.mRescuingFrameId);
            switch (cmd) {
                case PERF_RES_FPS_FPSGO_STOP_BOOST /* 33571328 */:
                    int[] perf_lock_rsc = {cmd, renderThreadTid};
                    perfLockAcquire(perf_lock_rsc);
                    ScrollPolicy.getInstance().disableMTKScrollingPolicy(RESCUE_TO_NEXT_FRAME);
                    break;
                case PERF_RES_FPS_FBT_RESCUE_SBE_RESCUE /* 33802752 */:
                    long rescueFrameId = this.mRescuingFrameId;
                    if (rescueFrameId != DEFAULT_FRAME_ID && rescueFrameId == -1 && this.mCurFrameId != -1 && rescueFrameId <= this.mLastFrameEndFrameId && this.mCurFrameRescueMode == 1) {
                        clearRescueInfo();
                        LogUtil.trace("do not rescue frameEndFrameId=" + this.mLastFrameEndFrameId);
                        break;
                    } else {
                        this.mPowerHalWrap.mtkNotifySbeRescue(renderThreadTid, this.mCurFrameRescueMode, 50, rescueFrameId);
                        break;
                    }
                    break;
                case PERF_RES_UX_SBE_RESCUE_ENHANS /* 50348800 */:
                    int[] enhance = {PERF_RES_UX_SBE_RESCUE_ENHANS, this.mRescueStrength};
                    perfLockAcquireUnlock(enhance);
                    break;
                default:
                    LogUtil.mLogd("FramePolicy", "not surpport for cmd = " + cmd);
                    break;
            }
            LogUtil.traceEnd();
            return;
        }
        clearRescueInfo();
        LogUtil.traceAndLog("FramePolicy", this.mCurFrameId + "cancel rescue: " + renderThreadTid);
    }

    private double drawFrameDelayTime() {
        if (mFrameIntervalTime == RefreshRateInfo.DECELERATION_RATE) {
            return -1.0d;
        }
        float refreshRate = Util.getRefreshRate();
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

    private void perfLockAcquireUnlock(int[] resList) {
        if (this.mPowerHalService != null) {
            this.mUnlockPowerHandle = this.mPowerHalService.perfLockAcquire(this.mUnlockPowerHandle, 0, resList);
        }
    }

    /* loaded from: classes.dex */
    private static class DisplayEventReceiverImpl extends DisplayEventReceiver {
        public DisplayEventReceiverImpl(Looper looper, int vsyncSource) {
            super(looper, vsyncSource, 0);
        }

        public void onVsync(long timestampNanos, long physicalDisplayId, int frame, DisplayEventReceiver.VsyncEventData vsyncEventData) {
            if (FramePolicyV2.mInstance != null) {
                FramePolicyV2.mInstance.onVsyncInternal(vsyncEventData.preferredFrameTimeline().vsyncId);
            }
        }
    }

    @Override // com.mediatek.boostfwk.info.ActivityInfo.ActivityChangeListener
    public void onChange(Context c) {
    }

    @Override // com.mediatek.boostfwk.info.ActivityInfo.ActivityChangeListener
    public void onAllActivityPaused(Context c) {
        if (this.mPowerHalService != null) {
            this.mPowerHalService.perfLockRelease(this.mUnlockPowerHandle);
        }
    }
}
