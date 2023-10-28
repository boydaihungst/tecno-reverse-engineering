package com.mediatek.boostfwk.policy.frame;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.SparseArray;
import android.view.ThreadedRenderer;
import com.mediatek.boostfwk.info.ScrollState;
import com.mediatek.boostfwk.utils.LogUtil;
import com.mediatek.powerhalmgr.PowerHalMgr;
import com.mediatek.powerhalmgr.PowerHalMgrFactory;
import com.mediatek.powerhalwrapper.PowerHalWrapper;
import java.lang.ref.WeakReference;
/* loaded from: classes.dex */
public class BaseFramePolicy {
    static final String TAG = "FramePolicy";
    static final String THREAD_NAME = "FramePolicy";
    ScrollState.ScrollStateListener mScrollStateListener;
    static boolean mListenFrameHint = false;
    static boolean mDisableFrameRescue = true;
    PowerHalMgr mPowerHalService = null;
    PowerHalWrapper mPowerHalWrap = null;
    int mPowerHandle = 0;
    HandlerThread mWorkerThread = null;
    WorkerHandler mWorkerHandler = null;
    boolean mCoreServiceReady = false;
    SparseArray<WeakReference<ThreadedRenderer>> mWeakThreadedRenderArray = null;
    WeakReference<ThreadedRenderer> mWeakThreadedRender = null;

    /* loaded from: classes.dex */
    private class ScrollListener implements ScrollState.ScrollStateListener {
        private ScrollListener() {
        }

        @Override // com.mediatek.boostfwk.info.ScrollState.ScrollStateListener
        public void onScroll(boolean scrolling) {
            BaseFramePolicy.this.onScrollStateChange(scrolling);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BaseFramePolicy() {
        this.mScrollStateListener = null;
        initThread();
        ScrollListener scrollListener = new ScrollListener();
        this.mScrollStateListener = scrollListener;
        ScrollState.registerScrollStateListener(scrollListener);
    }

    private void initThread() {
        HandlerThread handlerThread = this.mWorkerThread;
        if (handlerThread != null && handlerThread.isAlive() && this.mWorkerHandler != null) {
            LogUtil.mLogd("FramePolicy", "re-init");
            return;
        }
        HandlerThread handlerThread2 = new HandlerThread("FramePolicy");
        this.mWorkerThread = handlerThread2;
        handlerThread2.start();
        Looper looper = this.mWorkerThread.getLooper();
        if (looper == null) {
            LogUtil.mLogd("FramePolicy", "Thread looper is null");
            return;
        }
        WorkerHandler workerHandler = new WorkerHandler(looper);
        this.mWorkerHandler = workerHandler;
        workerHandler.sendEmptyMessage(WorkerHandler.MSG_INIT_CORE_SERVICE);
    }

    public boolean initLimitTime(float frameIntervalTime) {
        return false;
    }

    public void onRequestNextVsync() {
    }

    public void doFrameStepHint(boolean isBegin, int step) {
    }

    public void doFrameHint(boolean isBegin, long frameId) {
    }

    public void setThreadedRenderer(ThreadedRenderer threadedRenderer) {
        if (threadedRenderer == null) {
            return;
        }
        if (this.mWeakThreadedRenderArray == null) {
            this.mWeakThreadedRenderArray = new SparseArray<>();
        }
        int index = threadedRenderer.hashCode();
        if (!this.mWeakThreadedRenderArray.contains(index)) {
            WeakReference<ThreadedRenderer> weakThreadedRender = new WeakReference<>(threadedRenderer);
            this.mWeakThreadedRenderArray.put(index, weakThreadedRender);
            LogUtil.mLogd("FramePolicy", "add new render = " + threadedRenderer);
        }
    }

    public ThreadedRenderer getThreadedRenderer() {
        ThreadedRenderer render;
        ThreadedRenderer render2;
        if (this.mWeakThreadedRenderArray == null) {
            return null;
        }
        WeakReference<ThreadedRenderer> weakReference = this.mWeakThreadedRender;
        if (weakReference == null || (render2 = weakReference.get()) == null) {
            int size = this.mWeakThreadedRenderArray.size();
            if (size == 0) {
                return null;
            }
            for (int i = 0; i < size; i++) {
                int key = this.mWeakThreadedRenderArray.keyAt(i);
                WeakReference<ThreadedRenderer> weakThreadedRender = this.mWeakThreadedRenderArray.get(key);
                if (weakThreadedRender != null && (render = weakThreadedRender.get()) != null) {
                    this.mWeakThreadedRender = weakThreadedRender;
                    return render;
                }
            }
            this.mWeakThreadedRenderArray.clear();
            return null;
        }
        return render2;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onScrollStateChange(boolean scrolling) {
        boolean z = !scrolling;
        mDisableFrameRescue = z;
        if (!z) {
            mListenFrameHint = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void initCoreServiceInternal() {
        this.mPowerHalService = PowerHalMgrFactory.getInstance().makePowerHalMgr();
        this.mPowerHalWrap = PowerHalWrapper.getInstance();
        this.mCoreServiceReady = true;
    }

    protected void handleMessageInternal(Message msg) {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public class WorkerHandler extends Handler {
        public static final int MSG_INIT_CORE_SERVICE = -1000;

        WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == -1000) {
                BaseFramePolicy.this.initCoreServiceInternal();
            } else {
                BaseFramePolicy.this.handleMessageInternal(msg);
            }
        }
    }
}
