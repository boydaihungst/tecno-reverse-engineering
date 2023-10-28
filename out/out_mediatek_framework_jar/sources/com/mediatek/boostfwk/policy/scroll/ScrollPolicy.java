package com.mediatek.boostfwk.policy.scroll;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.view.Window;
import com.mediatek.boostfwk.identify.ime.IMEIdentify;
import com.mediatek.boostfwk.info.ActivityInfo;
import com.mediatek.boostfwk.info.ScrollState;
import com.mediatek.boostfwk.utils.Config;
import com.mediatek.boostfwk.utils.LogUtil;
import com.mediatek.powerhalmgr.PowerHalMgr;
import com.mediatek.powerhalmgr.PowerHalMgrFactory;
import com.mediatek.util.MtkPatterns;
/* loaded from: classes.dex */
public class ScrollPolicy {
    private static final boolean ENABLE_SCROLL_COMMON_POLICY;
    private static int MTKPOWER_HINT_UX_MOVE_SCROLLING = 0;
    private static int MTKPOWER_HINT_UX_SCROLLING = 0;
    private static int MTKPOWER_HINT_UX_SCROLLING_COMMON = 0;
    private static int NON_RENDER_THREAD_TID = 0;
    private static final int PERF_RES_FPS_FPSGO_CTL = 33555200;
    private static final int PERF_RES_FPS_FPSGO_NOCTL = 33555456;
    private static final int PERF_RES_FPS_FPSGO_UBOOST = 33851136;
    private static final int PERF_RES_FPS_FSTB_TARGET_FPS_PID = 33554944;
    private static final int PERF_RES_POWERHAL_TOUCH_BOOST_ENABLE = 54560000;
    private static final String TAG = "ScrollPolicy";
    private static boolean isCorrectFPS = false;
    private static final boolean isSupportAospMove;
    private static int mBoostHandle = 0;
    private static long mCheckFPSTime = 0;
    private static int mFlingPolicyExeCount = 0;
    private static int mPolicyExeCount = 0;
    private static int mPowerHandle = 0;
    private static int mReleaseFPSDuration = 0;
    private static int mRenderThreadTid = 0;
    private static int mSpecialAppDesign = 0;
    public static final int sFINGER_MOVE = 0;
    public static final int sFINGER_UP = 1;
    public static final int sFLING_HORIZONTAL = 3;
    public static final int sFLING_VERTICAL = 2;
    private static final String sTHREAD_NAME = "ScrollPolicy";
    public static boolean useFPSGo;
    private static ScrollPolicy sInstance = null;
    private static Object lock = new Object();
    private HandlerThread mWorkerThread = null;
    private WorkerHandler mWorkerHandler = null;
    private PowerHalMgr mPowerHalService = PowerHalMgrFactory.getInstance().makePowerHalMgr();
    private boolean waitingForReleaseFpsgo = false;
    private int waitingForReleaseFpsgoStep = -1;
    private boolean fpsgoUnderCtrlWhenFling = false;
    private boolean uboostEnable = false;
    private boolean mDisableScrollPolicy = false;
    private boolean mCommonPolicyEnabled = false;
    private int scrollingFingStep = -1;
    private long mLastScrollTimeMS = -1;
    private boolean mIsRealAOSPPage = false;
    private IMEIdentify.IMEStateListener mIMEStateListener = new IMEIdentify.IMEStateListener() { // from class: com.mediatek.boostfwk.policy.scroll.ScrollPolicy.1
        @Override // com.mediatek.boostfwk.identify.ime.IMEIdentify.IMEStateListener
        public void onInit(Window window) {
        }

        @Override // com.mediatek.boostfwk.identify.ime.IMEIdentify.IMEStateListener
        public void onVisibilityChange(boolean show) {
            ScrollPolicy.this.mDisableScrollPolicy = show;
            if (show && ScrollState.isScrolling()) {
                ScrollState.setScrolling(false, "ime show");
                if (ScrollPolicy.useFPSGo) {
                    ScrollPolicy.useFPSGo = false;
                }
                if (ScrollPolicy.mPolicyExeCount > 0) {
                    ScrollPolicy.this.mWorkerHandler.sendEmptyMessage(6);
                }
                if (ScrollPolicy.mFlingPolicyExeCount > 0) {
                    ScrollPolicy.this.mWorkerHandler.sendEmptyMessage(9);
                }
            }
        }
    };

    static {
        boolean z = true;
        if (Config.getBoostFwkVersion() <= 1 || !Config.isEnableScrollCommonPolicy()) {
            z = false;
        }
        ENABLE_SCROLL_COMMON_POLICY = z;
        mPowerHandle = 0;
        mBoostHandle = 0;
        mReleaseFPSDuration = Config.getScrollDuration();
        NON_RENDER_THREAD_TID = -1;
        mRenderThreadTid = -1;
        MTKPOWER_HINT_UX_SCROLLING = 43;
        MTKPOWER_HINT_UX_MOVE_SCROLLING = 45;
        MTKPOWER_HINT_UX_SCROLLING_COMMON = 49;
        mCheckFPSTime = 100L;
        isCorrectFPS = false;
        mPolicyExeCount = 0;
        mFlingPolicyExeCount = 0;
        mSpecialAppDesign = -1;
        useFPSGo = false;
        isSupportAospMove = Config.USER_CONFIG_DEFAULT_TYPE.equals(SystemProperties.get("vendor.boostfwk.aosp_move", "0"));
    }

    public static ScrollPolicy getInstance() {
        if (sInstance == null) {
            synchronized (lock) {
                if (sInstance == null) {
                    sInstance = new ScrollPolicy();
                }
            }
        }
        return sInstance;
    }

    public ScrollPolicy() {
        initThread();
        IMEIdentify.getInstance().registerIMEStateListener(this.mIMEStateListener);
    }

    private void initThread() {
        HandlerThread handlerThread = this.mWorkerThread;
        if (handlerThread != null && handlerThread.isAlive() && this.mWorkerHandler != null) {
            if (Config.isBoostFwkLogEnable()) {
                LogUtil.mLogi("ScrollPolicy", "re-init");
                return;
            }
            return;
        }
        HandlerThread handlerThread2 = new HandlerThread("ScrollPolicy");
        this.mWorkerThread = handlerThread2;
        handlerThread2.start();
        Looper looper = this.mWorkerThread.getLooper();
        if (looper == null) {
            LogUtil.mLogd("ScrollPolicy", "Thread looper is null");
        } else {
            this.mWorkerHandler = new WorkerHandler(looper);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class WorkerHandler extends Handler {
        public static final int MSG_RELEASE_BEGIN = 1;
        public static final int MSG_RELEASE_END = 2;
        public static final int MSG_RELEASE_FPS_CHECK = 3;
        public static final int MSG_RELEASE_FPS_TIMEOUT = 4;
        public static final int MSG_SBE_DELAY_RELEASE_FPSGO = 11;
        public static final int MSG_SBE_DISABLE_FPSGO_COUNT_DOWN = 12;
        public static final int MSG_SBE_FLING_POLICY_BEGIN = 8;
        public static final int MSG_SBE_FLING_POLICY_END = 9;
        public static final int MSG_SBE_FLING_POLICY_FLAG_END = 10;
        public static final int MSG_SBE_POLICY_BEGIN = 5;
        public static final int MSG_SBE_POLICY_END = 6;
        public static final int MSG_SBE_POLICY_FLAG_END = 7;
        public static final int MSG_SBE_SCROLL_COMMON_POLICY_COUNT_DOWN = 13;

        WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ScrollPolicy.this.releaseTargetFPSInternel(true);
                    return;
                case 2:
                    ScrollPolicy.this.releaseTargetFPSInternel(false);
                    return;
                case 3:
                    ScrollPolicy.isCorrectFPS = true;
                    return;
                case 4:
                    ScrollPolicy.this.releaseTargetFPSInternel(false);
                    return;
                case 5:
                    ScrollPolicy.this.mtkScrollingPolicy(true);
                    return;
                case 6:
                    ScrollPolicy.this.mtkScrollingPolicy(false);
                    return;
                case 7:
                    if (ScrollPolicy.mPolicyExeCount > 0) {
                        ScrollPolicy.mPolicyExeCount = 0;
                        ScrollPolicy.this.enableFPSGo(false, 0);
                        return;
                    }
                    return;
                case 8:
                    ScrollPolicy.this.mtkScrollingFlingPolicy(true);
                    return;
                case 9:
                    ScrollPolicy.this.mtkScrollingFlingPolicy(false);
                    return;
                case 10:
                    if (ScrollPolicy.mFlingPolicyExeCount > 0) {
                        ScrollPolicy.mFlingPolicyExeCount = 0;
                        ScrollPolicy scrollPolicy = ScrollPolicy.this;
                        scrollPolicy.enableFPSGo(false, scrollPolicy.scrollingFingStep);
                        return;
                    }
                    return;
                case 11:
                    ScrollPolicy.this.waitingForReleaseFpsgo = false;
                    ScrollPolicy scrollPolicy2 = ScrollPolicy.this;
                    scrollPolicy2.enableFPSGo(false, scrollPolicy2.waitingForReleaseFpsgoStep);
                    ScrollPolicy.this.waitingForReleaseFpsgoStep = -1;
                    return;
                case 12:
                    ScrollPolicy.useFPSGo = false;
                    ScrollPolicy scrollPolicy3 = ScrollPolicy.this;
                    scrollPolicy3.enableFPSGo(false, scrollPolicy3.scrollingFingStep);
                    return;
                case 13:
                    ScrollPolicy.this.scrollCommonPolicyCheck();
                    return;
                default:
                    return;
            }
        }
    }

    public void scrollHint(int step, int specialAppDesign) {
        if (this.mDisableScrollPolicy) {
            LogUtil.traceAndLog("ScrollPolicy", "scroll policy has been disable");
            return;
        }
        LogUtil.traceBeginAndLog("ScrollPolicy", "scrollHint step=" + step + " pageType" + specialAppDesign);
        switch (step) {
            case 0:
                if (useFPSGo) {
                    useFPSGo = false;
                }
                if (mSpecialAppDesign == -1) {
                    mSpecialAppDesign = specialAppDesign;
                }
                if (mPolicyExeCount == 0) {
                    WorkerHandler workerHandler = this.mWorkerHandler;
                    workerHandler.sendMessage(workerHandler.obtainMessage(5, null));
                    break;
                }
                break;
            case 1:
                this.mWorkerHandler.removeMessages(5, null);
                WorkerHandler workerHandler2 = this.mWorkerHandler;
                workerHandler2.sendMessage(workerHandler2.obtainMessage(6, null));
                break;
            case 2:
            case 3:
                this.scrollingFingStep = step;
                if (mFlingPolicyExeCount == 0) {
                    WorkerHandler workerHandler3 = this.mWorkerHandler;
                    workerHandler3.sendMessage(workerHandler3.obtainMessage(8, null));
                    break;
                }
                break;
        }
        LogUtil.traceEnd();
    }

    public void switchToFPSGo(boolean enableFPSGo) {
        if (this.mDisableScrollPolicy) {
            LogUtil.traceAndLog("ScrollPolicy", "switchToFPSGo scroll policy has been disable");
            return;
        }
        useFPSGo = enableFPSGo;
        LogUtil.traceBeginAndLog("ScrollPolicy", "switchToFPSGo " + (enableFPSGo ? MtkPatterns.KEY_URLDATA_START : "stop"));
        if (!this.mIsRealAOSPPage && mSpecialAppDesign == 0) {
            this.mIsRealAOSPPage = true;
        }
        if (enableFPSGo) {
            disableMTKScrollingPolicy(false);
        }
        LogUtil.traceEnd();
    }

    public void disableMTKScrollingPolicy(boolean needCheckBoostNow) {
        if (needCheckBoostNow && mPolicyExeCount == 0) {
            return;
        }
        WorkerHandler workerHandler = this.mWorkerHandler;
        workerHandler.sendMessage(workerHandler.obtainMessage(9, null));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void mtkScrollingPolicy(boolean enable) {
        if (this.mPowerHalService == null) {
            LogUtil.mLogw("ScrollPolicy", "mPowerHalService is null");
            return;
        }
        LogUtil.traceBeginAndLog("ScrollPolicy", "mtkScrollingPolicy " + (enable ? MtkPatterns.KEY_URLDATA_START : "stop"));
        if (enable) {
            if (mPolicyExeCount == 0) {
                disableTouchBoost();
                if (mSpecialAppDesign == 2) {
                    mPolicyExeCount++;
                    ScrollState.setScrolling(false, "PAGE_TYPE_SPECIAL_DESIGN_WEB_ON_60");
                    LogUtil.traceEnd();
                    return;
                }
                if (mFlingPolicyExeCount != 0) {
                    this.mWorkerHandler.removeMessages(10, null);
                    this.mPowerHalService.mtkPowerHint(MTKPOWER_HINT_UX_SCROLLING, 0);
                    mFlingPolicyExeCount = 0;
                }
                if (isSupportAospMove && mSpecialAppDesign != -1) {
                    this.mPowerHalService.mtkPowerHint(MTKPOWER_HINT_UX_MOVE_SCROLLING, mReleaseFPSDuration);
                } else {
                    int i = mSpecialAppDesign;
                    if (i != -1 && i != 0) {
                        this.mPowerHalService.mtkPowerHint(MTKPOWER_HINT_UX_MOVE_SCROLLING, mReleaseFPSDuration);
                    }
                }
                enableFPSGo(true, 0);
                mPolicyExeCount++;
                WorkerHandler workerHandler = this.mWorkerHandler;
                workerHandler.sendMessageDelayed(workerHandler.obtainMessage(7, null), mReleaseFPSDuration - mCheckFPSTime);
            }
        } else {
            mPolicyExeCount = 0;
            this.mWorkerHandler.removeMessages(7, null);
            this.mPowerHalService.mtkPowerHint(MTKPOWER_HINT_UX_MOVE_SCROLLING, 0);
            if (!useFPSGo) {
                delayControlFpsgo(0, true);
            }
        }
        LogUtil.traceEnd();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void mtkScrollingFlingPolicy(boolean enable) {
        if (this.mPowerHalService == null) {
            LogUtil.mLogw("ScrollPolicy", "mPowerHalService is null");
            return;
        }
        LogUtil.traceBeginAndLog("ScrollPolicy", "mtkScrollingFlingPolicy " + (enable ? MtkPatterns.KEY_URLDATA_START : "stop"));
        if (enable) {
            if (mFlingPolicyExeCount == 0 && !useFPSGo) {
                this.mIsRealAOSPPage = false;
                int duration = this.scrollingFingStep == 3 ? Config.SCROLLING_FING_HORIZONTAL_HINT_DURATION : mReleaseFPSDuration;
                this.mPowerHalService.mtkPowerHint(MTKPOWER_HINT_UX_SCROLLING, duration);
                enableFPSGo(true, this.scrollingFingStep);
                mFlingPolicyExeCount++;
                this.mWorkerHandler.removeMessages(10, null);
                WorkerHandler workerHandler = this.mWorkerHandler;
                workerHandler.sendMessageDelayed(workerHandler.obtainMessage(10, null), duration - mCheckFPSTime);
            }
        } else {
            mFlingPolicyExeCount = 0;
            this.mWorkerHandler.removeMessages(10, null);
            this.mPowerHalService.mtkPowerHint(MTKPOWER_HINT_UX_SCROLLING, 0);
            if (useFPSGo) {
                enableFPSGo(true, this.scrollingFingStep);
            } else {
                delayControlFpsgo(this.scrollingFingStep, true);
            }
        }
        LogUtil.traceEnd();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enableFPSGo(boolean enable, int step) {
        LogUtil.traceBeginAndLog("ScrollPolicy", "enableFPSGo " + (enable ? MtkPatterns.KEY_URLDATA_START : "stop"));
        this.mWorkerHandler.removeMessages(12);
        if (enable) {
            releaseFPSGOControl(true, step);
            if (useFPSGo) {
                WorkerHandler workerHandler = this.mWorkerHandler;
                workerHandler.sendMessageDelayed(workerHandler.obtainMessage(12, null), Config.getScrollDuration() - mCheckFPSTime);
            }
        } else {
            releaseFPSGOControl(false, step);
        }
        LogUtil.traceEnd();
    }

    private void delayControlFpsgo(int step, boolean enable) {
        if (!enable) {
            this.mWorkerHandler.removeMessages(11, null);
            this.waitingForReleaseFpsgoStep = -1;
        } else if (!this.waitingForReleaseFpsgo) {
            WorkerHandler workerHandler = this.mWorkerHandler;
            workerHandler.sendMessageDelayed(workerHandler.obtainMessage(11, null), 30L);
            this.waitingForReleaseFpsgoStep = step;
        }
        this.waitingForReleaseFpsgo = enable;
    }

    private void releaseFPSGOControl(boolean isBegin, int step) {
        releaseFPSGOControl(isBegin, step, null);
    }

    private void releaseFPSGOControl(boolean isBegin, int step, int[] commands) {
        int renderThreadTid = getRenderThreadTid();
        int[] perf_lock_rsc = (commands == null || commands.length <= 2) ? new int[4] : commands;
        if (isBegin) {
            delayControlFpsgo(2, false);
            perf_lock_rsc[0] = PERF_RES_FPS_FPSGO_CTL;
            perf_lock_rsc[1] = renderThreadTid;
            if (step == 2 || step == 3) {
                if (!this.fpsgoUnderCtrlWhenFling) {
                    perf_lock_rsc[2] = PERF_RES_FPS_FPSGO_NOCTL;
                    perf_lock_rsc[3] = -renderThreadTid;
                } else {
                    perf_lock_rsc[1] = 0;
                    perf_lock_rsc[0] = 0;
                }
                this.fpsgoUnderCtrlWhenFling = true;
                uBoostAcquire();
            }
            this.mLastScrollTimeMS = System.currentTimeMillis();
            if (!this.mCommonPolicyEnabled) {
                enableScrollingCommonCMD(true);
            }
            controlFpsgoInternal(perf_lock_rsc, "start " + step + " " + this.mLastScrollTimeMS);
            ScrollState.setScrolling(true, "start ctrl fpsgo");
            return;
        }
        perf_lock_rsc[0] = PERF_RES_FPS_FPSGO_CTL;
        perf_lock_rsc[1] = -renderThreadTid;
        if (step == 2 || step == 3) {
            perf_lock_rsc[2] = PERF_RES_FPS_FPSGO_NOCTL;
            perf_lock_rsc[3] = renderThreadTid;
            this.fpsgoUnderCtrlWhenFling = false;
            uBoostRelease();
        }
        enableScrollingCommonCMD(false);
        controlFpsgoInternal(perf_lock_rsc, "end " + step);
        ScrollState.setScrolling(false, "end ctrl fpsgo");
        mSpecialAppDesign = -1;
    }

    private void controlFpsgoInternal(int[] perf_lock_rsc, String logStr) {
        LogUtil.traceBeginAndLog("ScrollPolicy", logStr + " control Fpsgo " + commands2String(perf_lock_rsc));
        perfLockAcquire(perf_lock_rsc);
        LogUtil.traceEnd();
    }

    private String commands2String(int[] commands) {
        if (commands == null || commands.length == 0) {
            return "";
        }
        String cStr = "";
        int l = commands.length;
        for (int i = 0; i < l; i++) {
            switch (commands[i]) {
                case PERF_RES_FPS_FSTB_TARGET_FPS_PID /* 33554944 */:
                    cStr = cStr + " PERF_RES_FPS_FSTB_TARGET_FPS_PID ";
                    break;
                case PERF_RES_FPS_FPSGO_CTL /* 33555200 */:
                    cStr = cStr + " PERF_RES_FPS_FPSGO_CTL ";
                    break;
                case PERF_RES_FPS_FPSGO_NOCTL /* 33555456 */:
                    cStr = cStr + " PERF_RES_FPS_FPSGO_NOCTL ";
                    break;
                default:
                    cStr = cStr + String.valueOf(commands[i]);
                    break;
            }
        }
        return cStr;
    }

    public void releaseTargetFPS(boolean release) {
        if (this.mDisableScrollPolicy) {
            LogUtil.traceAndLog("ScrollPolicy", "releaseTargetFPS scroll policy has been disable");
            return;
        }
        int renderThreadTid = getRenderThreadTid();
        if (renderThreadTid == NON_RENDER_THREAD_TID) {
            LogUtil.mLogw("ScrollPolicy", "cannot found render thread");
        } else if (release) {
            isCorrectFPS = false;
            WorkerHandler workerHandler = this.mWorkerHandler;
            workerHandler.sendMessage(workerHandler.obtainMessage(1, null));
            this.mWorkerHandler.removeMessages(4, null);
            WorkerHandler workerHandler2 = this.mWorkerHandler;
            workerHandler2.sendMessageDelayed(workerHandler2.obtainMessage(3, null), mCheckFPSTime);
        } else if (isCorrectFPS) {
            WorkerHandler workerHandler3 = this.mWorkerHandler;
            workerHandler3.sendMessage(workerHandler3.obtainMessage(2, null));
        } else {
            WorkerHandler workerHandler4 = this.mWorkerHandler;
            workerHandler4.sendMessageDelayed(workerHandler4.obtainMessage(4, null), mReleaseFPSDuration);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void releaseTargetFPSInternel(boolean isBegin) {
        int renderThreadTid = getRenderThreadTid();
        int[] perf_lock_rsc = new int[6];
        perf_lock_rsc[4] = PERF_RES_FPS_FSTB_TARGET_FPS_PID;
        perf_lock_rsc[5] = isBegin ? renderThreadTid : -renderThreadTid;
        LogUtil.traceBeginAndLog("ScrollPolicy", "release Target FPS" + (isBegin ? MtkPatterns.KEY_URLDATA_START : "stop"));
        releaseFPSGOControl(isBegin, 2, perf_lock_rsc);
        LogUtil.traceEnd();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scrollCommonPolicyCheck() {
        if (this.mLastScrollTimeMS > 0 && mReleaseFPSDuration > System.currentTimeMillis() - this.mLastScrollTimeMS) {
            enableScrollingCommonCMD(true);
        } else if (this.mCommonPolicyEnabled) {
            enableScrollingCommonCMD(false);
        }
    }

    private int getRenderThreadTid() {
        if (mRenderThreadTid == NON_RENDER_THREAD_TID) {
            mRenderThreadTid = ActivityInfo.getInstance().getRenderThreadTid();
        }
        return mRenderThreadTid;
    }

    private void perfLockAcquire(int[] resList) {
        PowerHalMgr powerHalMgr = this.mPowerHalService;
        if (powerHalMgr != null) {
            int perfLockAcquire = powerHalMgr.perfLockAcquire(mPowerHandle, mReleaseFPSDuration, resList);
            mPowerHandle = perfLockAcquire;
            this.mPowerHalService.perfLockRelease(perfLockAcquire);
        }
    }

    private void uBoostAcquire() {
        PowerHalMgr powerHalMgr;
        if (Config.getBoostFwkVersion() == 1 && (powerHalMgr = this.mPowerHalService) != null && !this.uboostEnable) {
            this.uboostEnable = true;
            int[] perf_lock_rsc = {PERF_RES_FPS_FPSGO_UBOOST, 1};
            mBoostHandle = powerHalMgr.perfLockAcquire(mBoostHandle, mReleaseFPSDuration, perf_lock_rsc);
        }
    }

    private void uBoostRelease() {
        PowerHalMgr powerHalMgr;
        if (Config.getBoostFwkVersion() == 1 && (powerHalMgr = this.mPowerHalService) != null && this.uboostEnable) {
            this.uboostEnable = false;
            powerHalMgr.perfLockRelease(mBoostHandle);
        }
    }

    private void disableTouchBoost() {
        int[] perf_lock_rsc = {PERF_RES_POWERHAL_TOUCH_BOOST_ENABLE, 0};
        perfLockAcquire(perf_lock_rsc);
    }

    private void enableScrollingCommonCMD(boolean enable) {
        if (!ENABLE_SCROLL_COMMON_POLICY) {
            return;
        }
        this.mCommonPolicyEnabled = enable;
        if (enable) {
            LogUtil.traceAndMLogd("ScrollPolicy", "Enable MTKPOWER_HINT_UX_SCROLLING_COMMON");
            this.mPowerHalService.mtkPowerHint(MTKPOWER_HINT_UX_SCROLLING_COMMON, mReleaseFPSDuration);
            if (mSpecialAppDesign == 1 || !this.mIsRealAOSPPage) {
                this.mWorkerHandler.removeMessages(13);
                this.mWorkerHandler.sendEmptyMessageDelayed(13, mReleaseFPSDuration + mCheckFPSTime);
                return;
            }
            return;
        }
        LogUtil.traceAndMLogd("ScrollPolicy", "Disable MTKPOWER_HINT_UX_SCROLLING_COMMON");
        this.mPowerHalService.mtkPowerHint(MTKPOWER_HINT_UX_SCROLLING_COMMON, 0);
    }
}
