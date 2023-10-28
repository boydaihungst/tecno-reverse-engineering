package com.mediatek.boostfwk.policy.touch;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.view.MotionEvent;
import com.mediatek.boostfwk.identify.scroll.ScrollIdentify;
import com.mediatek.boostfwk.info.ActivityInfo;
import com.mediatek.boostfwk.info.ScrollState;
import com.mediatek.boostfwk.utils.Config;
import com.mediatek.boostfwk.utils.LogUtil;
import com.mediatek.boostfwk.utils.Util;
import com.mediatek.powerhalmgr.PowerHalMgr;
import com.mediatek.powerhalmgr.PowerHalMgrFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
/* loaded from: classes.dex */
public class TouchPolicy implements ScrollIdentify.TouchEventListener, ScrollState.ScrollStateListener, ActivityInfo.ActivityChangeListener {
    private static final boolean ENABLE_TOUCH_POLICY_FOR_ALL = false;
    private static final int PERF_RES_SCHED_SBB_ACTIVE_RATIO = 21251584;
    private static final int PERF_RES_SCHED_SBB_GROUP_SET = 21250560;
    private static final int PERF_RES_SCHED_SBB_GROUP_UNSET = 21250816;
    private static final int PERF_RES_SCHED_UCLAMP_MIN_TA = 21005056;
    private static final String TAG = "TouchPolicy";
    private static final String THREAD_NAME = "TouchPolicy";
    private static final int sDEFAULT_ACTIVE_RATIO = 70;
    private static final int sDEFAULT_UCLAMP_TA = 25;
    public static final Map<String, Long> LONG_TIME_PAGES = new HashMap<String, Long>() { // from class: com.mediatek.boostfwk.policy.touch.TouchPolicy.1
        {
            put("NebulaActivity", 999999L);
        }
    };
    private static TouchPolicy mInstance = null;
    private static final Object LOCK = new Object();
    private HandlerThread mWorkerThread = null;
    private WorkerHandler mWorkerHandler = null;
    private ActivityInfo mActivityInfo = null;
    private boolean mIsSBBTrigger = false;
    private PowerHalMgr mPowerHalService = PowerHalMgrFactory.getInstance().makePowerHalMgr();
    private int mPowerHandle = 0;
    private String mActivityStr = "";
    private long mLastTriggerTime = -1;
    private long mLastTouchDownTime = -1;
    private int mReleaseDuration = -1;
    private final long mResetBufferTimeMS = 100;
    private int mPid = Integer.MIN_VALUE;
    private int mPageType = -1;
    private boolean mIsLongTimePages = false;

    public static TouchPolicy getInstance() {
        if (mInstance == null) {
            synchronized (LOCK) {
                if (mInstance == null) {
                    mInstance = new TouchPolicy();
                }
            }
        }
        return mInstance;
    }

    private TouchPolicy() {
        initThread();
        ScrollIdentify.getInstance().registerTouchEventListener(this);
        ScrollState.registerScrollStateListener(this);
    }

    private void initThread() {
        HandlerThread handlerThread = this.mWorkerThread;
        if (handlerThread != null && handlerThread.isAlive() && this.mWorkerHandler != null) {
            if (Config.isBoostFwkLogEnable()) {
                LogUtil.mLogi("TouchPolicy", "re-init");
                return;
            }
            return;
        }
        HandlerThread handlerThread2 = new HandlerThread("TouchPolicy");
        this.mWorkerThread = handlerThread2;
        handlerThread2.start();
        Looper looper = this.mWorkerThread.getLooper();
        if (looper == null) {
            LogUtil.mLogd("TouchPolicy", "Thread looper is null");
        } else {
            this.mWorkerHandler = new WorkerHandler(looper);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class WorkerHandler extends Handler {
        public static final int MSG_ENABLE_SBB = 1;
        public static final int MSG_RESET_SBB = 2;

        WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    TouchPolicy.this.enableSBBInternal(-1);
                    return;
                case 2:
                    TouchPolicy.this.resetSBBInternal(msg.obj != null);
                    return;
                default:
                    return;
            }
        }
    }

    @Override // com.mediatek.boostfwk.identify.scroll.ScrollIdentify.TouchEventListener
    public void onTouchEvent(MotionEvent event) {
        if (!Config.isEnableTouchPolicy() || Config.getBoostFwkVersion() <= 1 || ScrollState.getRefreshRate() <= 61.0f || event.getAction() != 0 || !Util.isMainThread()) {
            return;
        }
        if (this.mActivityInfo == null) {
            ActivityInfo activityInfo = ActivityInfo.getInstance();
            this.mActivityInfo = activityInfo;
            activityInfo.registerActivityListener(this);
        }
        if (this.mPageType == -1) {
            this.mPageType = ScrollIdentify.getInstance().getPageDesign();
        }
        if (this.mPageType == -1) {
            ScrollIdentify.getInstance().updatePageType();
            this.mPageType = ScrollIdentify.getInstance().getPageDesign();
        }
        if (this.mPageType == 1 && !this.mIsLongTimePages) {
            long diff = System.currentTimeMillis() - this.mLastTriggerTime;
            this.mLastTouchDownTime = System.currentTimeMillis();
            if (diff < this.mReleaseDuration) {
                LogUtil.traceAndMLogd("TouchPolicy", "onTouchEvent for return" + diff);
            } else {
                enableSBB();
            }
        }
    }

    private void enableSBB() {
        if (this.mPid == Integer.MIN_VALUE) {
            this.mPid = Process.myPid();
        }
        this.mLastTriggerTime = System.currentTimeMillis();
        this.mWorkerHandler.removeMessages(1, null);
        this.mWorkerHandler.sendEmptyMessage(1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enableSBBInternal(int duration) {
        if (this.mReleaseDuration < 0) {
            this.mReleaseDuration = (int) generateNewDuration();
        }
        int enableDuration = duration <= 0 ? this.mReleaseDuration : duration;
        this.mIsSBBTrigger = true;
        LogUtil.traceAndMLogd("TouchPolicy", "enableSBB for " + this.mActivityStr + " with duration=" + this.mReleaseDuration + " pid=" + this.mPid);
        int[] iArr = new int[6];
        int[] perf_lock_rsc = this.mIsLongTimePages ? new int[]{PERF_RES_SCHED_SBB_GROUP_SET, this.mPid, PERF_RES_SCHED_SBB_ACTIVE_RATIO, sDEFAULT_ACTIVE_RATIO, PERF_RES_SCHED_UCLAMP_MIN_TA, sDEFAULT_UCLAMP_TA} : new int[]{PERF_RES_SCHED_SBB_GROUP_SET, this.mPid, PERF_RES_SCHED_SBB_ACTIVE_RATIO, sDEFAULT_ACTIVE_RATIO};
        acquirePowerhal(perf_lock_rsc, enableDuration);
        this.mWorkerHandler.removeMessages(2, null);
        WorkerHandler workerHandler = this.mWorkerHandler;
        workerHandler.sendMessageDelayed(workerHandler.obtainMessage(2, true), enableDuration);
    }

    private long generateNewDuration() {
        long duration = Config.getTouchDuration();
        Set<String> list = LONG_TIME_PAGES.keySet();
        for (String activity : list) {
            if (this.mActivityStr.contains(activity)) {
                long duration2 = LONG_TIME_PAGES.get(activity).longValue();
                this.mIsLongTimePages = true;
                return duration2;
            }
        }
        return duration;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:9:0x0022, code lost:
        if (r5 < (r11.mReleaseDuration - 100)) goto L9;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void resetSBBInternal(boolean continueSbb) {
        long diff = 0;
        if (continueSbb) {
            if (!this.mIsLongTimePages) {
                if (this.mLastTouchDownTime > 0) {
                    long currentTimeMillis = System.currentTimeMillis() - this.mLastTouchDownTime;
                    diff = currentTimeMillis;
                }
            }
            LogUtil.traceAndMLogd("TouchPolicy", "continueSBB");
            this.mLastTriggerTime = System.currentTimeMillis();
            enableSBBInternal((int) (this.mReleaseDuration - diff));
            return;
        }
        LogUtil.traceAndMLogd("TouchPolicy", "resetSBB for " + this.mActivityStr);
        releasePowerHandle();
        this.mLastTriggerTime = 0L;
        this.mIsSBBTrigger = false;
        this.mIsLongTimePages = false;
    }

    @Override // com.mediatek.boostfwk.info.ScrollState.ScrollStateListener
    public void onScroll(boolean scrolling) {
        if (scrolling && !this.mIsLongTimePages) {
            LogUtil.traceAndMLogd("TouchPolicy", "onScroll for " + this.mActivityStr + " with scrolling=" + scrolling);
            resetSBB();
        }
    }

    private void resetSBB() {
        if (this.mIsSBBTrigger) {
            this.mWorkerHandler.removeMessages(2, null);
            WorkerHandler workerHandler = this.mWorkerHandler;
            workerHandler.sendMessageAtFrontOfQueue(workerHandler.obtainMessage(2));
        }
    }

    @Override // com.mediatek.boostfwk.info.ActivityInfo.ActivityChangeListener
    public void onChange(Context c) {
        LogUtil.traceAndMLogd("TouchPolicy", "onChange for " + c + " " + this.mActivityStr);
        if (c != null && !c.toString().equals(this.mActivityStr)) {
            this.mActivityStr = c.toString();
            resetSBB();
            this.mReleaseDuration = -1;
            generateNewDuration();
            if (this.mIsLongTimePages) {
                enableSBB();
            }
        }
    }

    @Override // com.mediatek.boostfwk.info.ActivityInfo.ActivityChangeListener
    public void onAllActivityPaused(Context c) {
        LogUtil.traceAndMLogd("TouchPolicy", "onAllActivityPause for " + c + " " + this.mActivityStr);
        resetSBB();
        this.mActivityStr = "";
    }

    private void acquirePowerhal(int[] perf_lock_rsc, int duration) {
        PowerHalMgr powerHalMgr = this.mPowerHalService;
        if (powerHalMgr != null) {
            this.mPowerHandle = powerHalMgr.perfLockAcquire(this.mPowerHandle, duration, perf_lock_rsc);
        }
    }

    private void releasePowerHandle() {
        int i;
        if (this.mPowerHalService != null && (i = this.mPid) != Integer.MIN_VALUE) {
            int[] perf_lock_rsc = {PERF_RES_SCHED_SBB_GROUP_UNSET, i};
            acquirePowerhal(perf_lock_rsc, 1);
            this.mPowerHalService.perfLockRelease(this.mPowerHandle);
        }
    }
}
