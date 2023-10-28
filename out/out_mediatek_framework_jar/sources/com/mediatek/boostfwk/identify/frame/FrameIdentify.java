package com.mediatek.boostfwk.identify.frame;

import com.mediatek.boostfwk.identify.BaseIdentify;
import com.mediatek.boostfwk.info.ActivityInfo;
import com.mediatek.boostfwk.info.ScrollState;
import com.mediatek.boostfwk.policy.frame.BaseFramePolicy;
import com.mediatek.boostfwk.policy.frame.FramePolicy;
import com.mediatek.boostfwk.policy.frame.FramePolicyV2;
import com.mediatek.boostfwk.policy.frame.ScrollingFramePrefetcher;
import com.mediatek.boostfwk.policy.refreshrate.RefreshRateInfo;
import com.mediatek.boostfwk.scenario.BasicScenario;
import com.mediatek.boostfwk.scenario.frame.FrameScenario;
import com.mediatek.boostfwk.utils.Config;
import com.mediatek.boostfwk.utils.LogUtil;
/* loaded from: classes.dex */
public class FrameIdentify extends BaseIdentify {
    private static final String TAG = "FrameIdentify";
    private BaseFramePolicy mFramePolicy;
    private ScrollingFramePrefetcher mScrollingFramePrefetcher;
    private static FrameIdentify sInstance = null;
    private static Object slock = new Object();
    private static boolean mIsOnVsyncCheck = false;
    private static boolean mIsDoFrameBegin = false;
    private static float mRefreshRate = RefreshRateInfo.DECELERATION_RATE;
    private static long mFrameIntervalMs = 0;
    private static long mLimitVsyncTime = 0;
    private static final int BOOST_FWK_VERSION = Config.getBoostFwkVersion();

    public static FrameIdentify getInstance() {
        if (sInstance == null) {
            synchronized (slock) {
                if (sInstance == null) {
                    sInstance = new FrameIdentify();
                }
            }
        }
        return sInstance;
    }

    private FrameIdentify() {
        this.mFramePolicy = null;
        this.mScrollingFramePrefetcher = null;
        this.mFramePolicy = BOOST_FWK_VERSION > 1 ? FramePolicyV2.getInstance() : FramePolicy.getInstance();
        if (this.mScrollingFramePrefetcher == null) {
            this.mScrollingFramePrefetcher = ScrollingFramePrefetcher.getInstance();
        }
    }

    @Override // com.mediatek.boostfwk.identify.BaseIdentify
    public boolean dispatchScenario(BasicScenario basicScenario) {
        if (Config.disableFrameIdentify() || basicScenario == null) {
            return false;
        }
        FrameScenario scenario = (FrameScenario) basicScenario;
        int action = scenario.getScenarioAction();
        int status = scenario.getBoostStatus();
        int frameStep = scenario.getFrameStep();
        if (Config.isBoostFwkLogEnable()) {
            LogUtil.mLogd(TAG, "Frame action dispatcher to = " + action + " status = " + status + ", frame step = " + frameStep);
        }
        switch (action) {
            case 0:
                doFrameCheck(status, scenario.getFrameId(), scenario);
                return true;
            case 1:
                doFrameStepCheck(status, frameStep);
                return true;
            case 2:
                doFrameRequestNextVsync();
                return true;
            case 3:
                updateRenderInfo(scenario);
                return true;
            case 4:
                doScrollingFramePrefetcher(status, scenario);
                return true;
            case 5:
                doScrollingFramePrefetcherPreAnim(scenario);
                return true;
            default:
                LogUtil.mLogw(TAG, "Not found dispatcher frame action.");
                return true;
        }
    }

    private void updateRenderInfo(FrameScenario scenario) {
        int renderThreadTid = scenario.getRenderThreadTid();
        if (renderThreadTid > 0) {
            ActivityInfo.getInstance().setRenderThreadTid(renderThreadTid);
        }
        LogUtil.traceAndLog(TAG, "init renderThreadTid=" + renderThreadTid);
        this.mFramePolicy.setThreadedRenderer(scenario.getThreadedRendererAndClear());
    }

    private void doFrameCheck(int status, long frameId, FrameScenario scenario) {
        boolean init = doFrameInit();
        if (BOOST_FWK_VERSION > 1) {
            scenario.setIsListenFrameHint(init);
            scenario.setFling(ScrollState.getFling());
            scenario.setSFPEnable(ScrollingFramePrefetcher.FEATURE_ENABLE).setPreAnimEnable(ScrollingFramePrefetcher.PRE_ANIM_ENABLE);
        }
        if (!init) {
            return;
        }
        boolean boostBeginEndCheck = boostBeginEndCheck(status);
        mIsOnVsyncCheck = boostBeginEndCheck;
        this.mFramePolicy.doFrameHint(boostBeginEndCheck, frameId);
    }

    private void doFrameStepCheck(int status, int step) {
        if (!isBeginFrameAction()) {
            return;
        }
        this.mFramePolicy.doFrameStepHint(boostBeginEndCheck(status), step);
    }

    private void doFrameRequestNextVsync() {
        this.mFramePolicy.onRequestNextVsync();
        this.mScrollingFramePrefetcher.onRequestNextVsync();
    }

    private boolean doFrameInit() {
        float refreshRate = ScrollState.getRefreshRate();
        mRefreshRate = refreshRate;
        long j = 1000.0f / refreshRate;
        mFrameIntervalMs = j;
        return this.mFramePolicy.initLimitTime((float) j);
    }

    private boolean isBeginFrameAction() {
        return mIsOnVsyncCheck;
    }

    private boolean boostBeginEndCheck(int status) {
        return status == 0;
    }

    private void doScrollingFramePrefetcher(int status, FrameScenario scenario) {
        boolean isBegin = boostBeginEndCheck(status);
        if (isBegin) {
            mIsDoFrameBegin = true;
            if (!mIsOnVsyncCheck && scenario.getFrameId() == -1) {
                scenario.setIsListenFrameHint(doFrameInit());
                mIsOnVsyncCheck = true;
                this.mFramePolicy.doFrameHint(true, scenario.getFrameId());
            }
            this.mScrollingFramePrefetcher.doScrollingFramePrefetcher(scenario);
        } else if (mIsDoFrameBegin) {
            mIsDoFrameBegin = false;
            this.mScrollingFramePrefetcher.onFrameEnd(isBegin, scenario);
        }
    }

    private void doScrollingFramePrefetcherPreAnim(FrameScenario scenario) {
        scenario.setPreAnim(this.mScrollingFramePrefetcher.isPreAnimation());
    }
}
