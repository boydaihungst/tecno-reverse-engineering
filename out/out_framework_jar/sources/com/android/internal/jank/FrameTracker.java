package com.android.internal.jank;

import android.graphics.HardwareRendererObserver;
import android.os.Handler;
import android.os.Trace;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.view.Choreographer;
import android.view.FrameMetrics;
import android.view.SurfaceControl;
import android.view.ThreadedRenderer;
import android.view.ViewRootImpl;
import com.android.internal.jank.FrameTracker;
import com.android.internal.jank.InteractionJankMonitor;
import com.android.internal.util.FrameworkStatsLog;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.TimeUnit;
/* loaded from: classes4.dex */
public class FrameTracker extends SurfaceControl.OnJankDataListener implements HardwareRendererObserver.OnFrameMetricsAvailableListener {
    private static final boolean DEBUG = false;
    private static final long INVALID_ID = -1;
    public static final int NANOS_IN_MILLISECOND = 1000000;
    static final int REASON_CANCEL_NORMAL = 16;
    static final int REASON_CANCEL_NOT_BEGUN = 17;
    static final int REASON_CANCEL_SAME_VSYNC = 18;
    static final int REASON_CANCEL_TIMEOUT = 19;
    static final int REASON_END_NORMAL = 0;
    static final int REASON_END_SURFACE_DESTROYED = 1;
    static final int REASON_END_UNKNOWN = -1;
    private static final String TAG = "FrameTracker";
    private final ChoreographerWrapper mChoreographer;
    private final boolean mDeferMonitoring;
    private final Handler mHandler;
    private FrameTrackerListener mListener;
    private boolean mMetricsFinalized;
    private final FrameMetricsWrapper mMetricsWrapper;
    private final HardwareRendererObserver mObserver;
    private final ThreadedRendererWrapper mRendererWrapper;
    private final InteractionJankMonitor.Session mSession;
    private final StatsLogWrapper mStatsLog;
    private final ViewRootImpl.SurfaceChangedCallback mSurfaceChangedCallback;
    private SurfaceControl mSurfaceControl;
    private final SurfaceControlWrapper mSurfaceControlWrapper;
    public final boolean mSurfaceOnly;
    private final int mTraceThresholdFrameTimeMillis;
    private final int mTraceThresholdMissedFrames;
    private final ViewRootWrapper mViewRoot;
    private Runnable mWaitForFinishTimedOut;
    private final SparseArray<JankInfo> mJankInfos = new SparseArray<>();
    private final Object mLock = InteractionJankMonitor.getInstance().getLock();
    private long mBeginVsyncId = -1;
    private long mEndVsyncId = -1;
    private boolean mCancelled = false;
    private boolean mTracingStarted = false;

    /* loaded from: classes4.dex */
    public interface FrameTrackerListener {
        void onCujEvents(InteractionJankMonitor.Session session, String str);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes4.dex */
    public @interface Reasons {
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public static class JankInfo {
        long frameVsyncId;
        boolean hwuiCallbackFired;
        boolean isFirstFrame;
        int jankType;
        boolean surfaceControlCallbackFired;
        long totalDurationNanos;

        static JankInfo createFromHwuiCallback(long frameVsyncId, long totalDurationNanos, boolean isFirstFrame) {
            return new JankInfo(frameVsyncId, true, false, 0, totalDurationNanos, isFirstFrame);
        }

        static JankInfo createFromSurfaceControlCallback(long frameVsyncId, int jankType) {
            return new JankInfo(frameVsyncId, false, true, jankType, 0L, false);
        }

        private JankInfo(long frameVsyncId, boolean hwuiCallbackFired, boolean surfaceControlCallbackFired, int jankType, long totalDurationNanos, boolean isFirstFrame) {
            this.frameVsyncId = frameVsyncId;
            this.hwuiCallbackFired = hwuiCallbackFired;
            this.surfaceControlCallbackFired = surfaceControlCallbackFired;
            this.totalDurationNanos = totalDurationNanos;
            this.jankType = jankType;
            this.isFirstFrame = isFirstFrame;
        }
    }

    public FrameTracker(InteractionJankMonitor.Session session, Handler handler, ThreadedRendererWrapper renderer, ViewRootWrapper viewRootWrapper, SurfaceControlWrapper surfaceControlWrapper, ChoreographerWrapper choreographer, FrameMetricsWrapper metrics, StatsLogWrapper statsLog, int traceThresholdMissedFrames, int traceThresholdFrameTimeMillis, FrameTrackerListener listener, InteractionJankMonitor.Configuration config) {
        HardwareRendererObserver hardwareRendererObserver;
        boolean isSurfaceOnly = config.isSurfaceOnly();
        this.mSurfaceOnly = isSurfaceOnly;
        this.mSession = session;
        this.mHandler = handler;
        this.mChoreographer = choreographer;
        this.mSurfaceControlWrapper = surfaceControlWrapper;
        this.mStatsLog = statsLog;
        this.mDeferMonitoring = config.shouldDeferMonitor();
        this.mRendererWrapper = isSurfaceOnly ? null : renderer;
        FrameMetricsWrapper frameMetricsWrapper = isSurfaceOnly ? null : metrics;
        this.mMetricsWrapper = frameMetricsWrapper;
        ViewRootWrapper viewRootWrapper2 = isSurfaceOnly ? null : viewRootWrapper;
        this.mViewRoot = viewRootWrapper2;
        if (isSurfaceOnly) {
            hardwareRendererObserver = null;
        } else {
            hardwareRendererObserver = new HardwareRendererObserver(this, frameMetricsWrapper.getTiming(), handler, false);
        }
        this.mObserver = hardwareRendererObserver;
        this.mTraceThresholdMissedFrames = traceThresholdMissedFrames;
        this.mTraceThresholdFrameTimeMillis = traceThresholdFrameTimeMillis;
        this.mListener = listener;
        if (isSurfaceOnly) {
            this.mSurfaceControl = config.getSurfaceControl();
            this.mSurfaceChangedCallback = null;
            return;
        }
        if (viewRootWrapper2.getSurfaceControl().isValid()) {
            this.mSurfaceControl = viewRootWrapper2.getSurfaceControl();
        }
        AnonymousClass1 anonymousClass1 = new AnonymousClass1();
        this.mSurfaceChangedCallback = anonymousClass1;
        viewRootWrapper2.addSurfaceChangedCallback(anonymousClass1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.internal.jank.FrameTracker$1  reason: invalid class name */
    /* loaded from: classes4.dex */
    public class AnonymousClass1 implements ViewRootImpl.SurfaceChangedCallback {
        AnonymousClass1() {
        }

        @Override // android.view.ViewRootImpl.SurfaceChangedCallback
        public void surfaceCreated(SurfaceControl.Transaction t) {
            synchronized (FrameTracker.this.mLock) {
                if (FrameTracker.this.mSurfaceControl == null) {
                    FrameTracker frameTracker = FrameTracker.this;
                    frameTracker.mSurfaceControl = frameTracker.mViewRoot.getSurfaceControl();
                    if (FrameTracker.this.mBeginVsyncId != -1) {
                        SurfaceControlWrapper surfaceControlWrapper = FrameTracker.this.mSurfaceControlWrapper;
                        FrameTracker frameTracker2 = FrameTracker.this;
                        surfaceControlWrapper.addJankStatsListener(frameTracker2, frameTracker2.mSurfaceControl);
                        FrameTracker.this.markEvent("FT#deferMonitoring");
                        FrameTracker.this.postTraceStartMarker();
                    }
                }
            }
        }

        @Override // android.view.ViewRootImpl.SurfaceChangedCallback
        public void surfaceReplaced(SurfaceControl.Transaction t) {
        }

        @Override // android.view.ViewRootImpl.SurfaceChangedCallback
        public void surfaceDestroyed() {
            if (!FrameTracker.this.mMetricsFinalized) {
                FrameTracker.this.mSurfaceControlWrapper.removeJankStatsListener(FrameTracker.this);
            }
            FrameTracker.this.mHandler.postDelayed(new Runnable() { // from class: com.android.internal.jank.FrameTracker$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    FrameTracker.AnonymousClass1.this.m6680x96b3cb5a();
                }
            }, 50L);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$surfaceDestroyed$0$com-android-internal-jank-FrameTracker$1  reason: not valid java name */
        public /* synthetic */ void m6680x96b3cb5a() {
            synchronized (FrameTracker.this.mLock) {
                if (!FrameTracker.this.mMetricsFinalized) {
                    FrameTracker.this.end(1);
                    FrameTracker.this.finish();
                }
            }
        }
    }

    public void begin() {
        synchronized (this.mLock) {
            long currentVsync = this.mChoreographer.getVsyncId();
            boolean z = this.mDeferMonitoring;
            this.mBeginVsyncId = z ? 1 + currentVsync : currentVsync;
            if (this.mSurfaceControl != null) {
                if (z) {
                    markEvent("FT#deferMonitoring");
                    postTraceStartMarker();
                } else {
                    beginInternal();
                }
                this.mSurfaceControlWrapper.addJankStatsListener(this, this.mSurfaceControl);
            }
            if (!this.mSurfaceOnly) {
                this.mRendererWrapper.addObserver(this.mObserver);
            }
        }
    }

    public void postTraceStartMarker() {
        this.mChoreographer.mChoreographer.postCallback(0, new Runnable() { // from class: com.android.internal.jank.FrameTracker$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                FrameTracker.this.beginInternal();
            }
        }, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void beginInternal() {
        synchronized (this.mLock) {
            if (!this.mCancelled && this.mEndVsyncId == -1) {
                this.mTracingStarted = true;
                markEvent("FT#begin");
                Trace.beginAsyncSection(this.mSession.getName(), (int) this.mBeginVsyncId);
            }
        }
    }

    public boolean end(int reason) {
        synchronized (this.mLock) {
            if (!this.mCancelled && this.mEndVsyncId == -1) {
                long vsyncId = this.mChoreographer.getVsyncId();
                this.mEndVsyncId = vsyncId;
                long j = this.mBeginVsyncId;
                if (j == -1) {
                    return cancel(17);
                } else if (vsyncId <= j) {
                    return cancel(18);
                } else {
                    markEvent("FT#end#" + reason);
                    Trace.endAsyncSection(this.mSession.getName(), (int) this.mBeginVsyncId);
                    this.mSession.setReason(reason);
                    Runnable runnable = new Runnable() { // from class: com.android.internal.jank.FrameTracker$$ExternalSyntheticLambda1
                        @Override // java.lang.Runnable
                        public final void run() {
                            FrameTracker.this.m6679lambda$end$0$comandroidinternaljankFrameTracker();
                        }
                    };
                    this.mWaitForFinishTimedOut = runnable;
                    this.mHandler.postDelayed(runnable, TimeUnit.SECONDS.toMillis(10L));
                    notifyCujEvent(InteractionJankMonitor.ACTION_SESSION_END);
                    return true;
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$end$0$com-android-internal-jank-FrameTracker  reason: not valid java name */
    public /* synthetic */ void m6679lambda$end$0$comandroidinternaljankFrameTracker() {
        Log.e(TAG, "force finish cuj because of time out:" + this.mSession.getName());
        finish();
    }

    public boolean cancel(int reason) {
        synchronized (this.mLock) {
            boolean cancelFromEnd = reason == 17 || reason == 18;
            if (!this.mCancelled && (this.mEndVsyncId == -1 || cancelFromEnd)) {
                this.mCancelled = true;
                markEvent("FT#cancel#" + reason);
                if (this.mTracingStarted) {
                    Trace.endAsyncSection(this.mSession.getName(), (int) this.mBeginVsyncId);
                }
                removeObservers();
                this.mSession.setReason(reason);
                notifyCujEvent(InteractionJankMonitor.ACTION_SESSION_CANCEL);
                return true;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void markEvent(String desc) {
        Trace.beginSection(TextUtils.formatSimple("%s#%s", this.mSession.getName(), desc));
        Trace.endSection();
    }

    private void notifyCujEvent(String action) {
        FrameTrackerListener frameTrackerListener = this.mListener;
        if (frameTrackerListener == null) {
            return;
        }
        frameTrackerListener.onCujEvents(this.mSession, action);
    }

    @Override // android.view.SurfaceControl.OnJankDataListener
    public void onJankDataAvailable(SurfaceControl.JankData[] jankData) {
        synchronized (this.mLock) {
            if (this.mCancelled) {
                return;
            }
            for (SurfaceControl.JankData jankStat : jankData) {
                if (isInRange(jankStat.frameVsyncId)) {
                    JankInfo info = findJankInfo(jankStat.frameVsyncId);
                    if (info != null) {
                        info.surfaceControlCallbackFired = true;
                        info.jankType = jankStat.jankType;
                    } else {
                        this.mJankInfos.put((int) jankStat.frameVsyncId, JankInfo.createFromSurfaceControlCallback(jankStat.frameVsyncId, jankStat.jankType));
                    }
                }
            }
            processJankInfos();
        }
    }

    private JankInfo findJankInfo(long frameVsyncId) {
        return this.mJankInfos.get((int) frameVsyncId);
    }

    private boolean isInRange(long vsyncId) {
        return vsyncId >= this.mBeginVsyncId;
    }

    @Override // android.graphics.HardwareRendererObserver.OnFrameMetricsAvailableListener
    public void onFrameMetricsAvailable(int dropCountSinceLastInvocation) {
        synchronized (this.mLock) {
            if (this.mCancelled) {
                return;
            }
            long totalDurationNanos = this.mMetricsWrapper.getMetric(8);
            boolean isFirstFrame = this.mMetricsWrapper.getMetric(9) == 1;
            long frameVsyncId = this.mMetricsWrapper.getTiming()[1];
            if (isInRange(frameVsyncId)) {
                JankInfo info = findJankInfo(frameVsyncId);
                if (info != null) {
                    info.hwuiCallbackFired = true;
                    info.totalDurationNanos = totalDurationNanos;
                    info.isFirstFrame = isFirstFrame;
                } else {
                    this.mJankInfos.put((int) frameVsyncId, JankInfo.createFromHwuiCallback(frameVsyncId, totalDurationNanos, isFirstFrame));
                }
                processJankInfos();
            }
        }
    }

    private boolean hasReceivedCallbacksAfterEnd() {
        JankInfo last;
        if (this.mEndVsyncId == -1) {
            return false;
        }
        if (this.mJankInfos.size() == 0) {
            last = null;
        } else {
            SparseArray<JankInfo> sparseArray = this.mJankInfos;
            last = sparseArray.valueAt(sparseArray.size() - 1);
        }
        if (last != null && last.frameVsyncId >= this.mEndVsyncId) {
            for (int i = this.mJankInfos.size() - 1; i >= 0; i--) {
                JankInfo info = this.mJankInfos.valueAt(i);
                if (info.frameVsyncId >= this.mEndVsyncId && callbacksReceived(info)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private void processJankInfos() {
        if (this.mMetricsFinalized || !hasReceivedCallbacksAfterEnd()) {
            return;
        }
        finish();
    }

    private boolean callbacksReceived(JankInfo info) {
        if (this.mSurfaceOnly) {
            return info.surfaceControlCallbackFired;
        }
        return info.hwuiCallbackFired && info.surfaceControlCallbackFired;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void finish() {
        int totalFramesCount;
        int totalFramesCount2;
        this.mHandler.removeCallbacks(this.mWaitForFinishTimedOut);
        this.mWaitForFinishTimedOut = null;
        boolean z = true;
        this.mMetricsFinalized = true;
        removeObservers();
        int totalFramesCount3 = 0;
        int maxSuccessiveMissedFramesCount = 0;
        long maxFrameTimeNanos = 0;
        int missedFramesCount = 0;
        int missedAppFramesCount = 0;
        int successiveMissedFramesCount = 0;
        int i = 0;
        int missedSfFramesCount = 0;
        while (true) {
            if (i >= this.mJankInfos.size()) {
                totalFramesCount = totalFramesCount3;
                break;
            }
            JankInfo info = this.mJankInfos.valueAt(i);
            boolean isFirstDrawn = (this.mSurfaceOnly || !info.isFirstFrame) ? false : z;
            if (!isFirstDrawn) {
                totalFramesCount = totalFramesCount3;
                if (info.frameVsyncId > this.mEndVsyncId) {
                    break;
                }
                if (info.surfaceControlCallbackFired) {
                    totalFramesCount2 = totalFramesCount + 1;
                    boolean missedFrame = false;
                    if ((info.jankType & 8) != 0) {
                        Log.w(TAG, "Missed App frame:" + info.jankType);
                        missedAppFramesCount++;
                        missedFrame = true;
                    }
                    if ((info.jankType & 1) != 0 || (info.jankType & 2) != 0 || (info.jankType & 4) != 0 || (info.jankType & 32) != 0 || (info.jankType & 16) != 0) {
                        Log.w(TAG, "Missed SF frame:" + info.jankType);
                        missedSfFramesCount++;
                        missedFrame = true;
                    }
                    if (missedFrame) {
                        missedFramesCount++;
                        successiveMissedFramesCount++;
                    } else {
                        maxSuccessiveMissedFramesCount = Math.max(maxSuccessiveMissedFramesCount, successiveMissedFramesCount);
                        successiveMissedFramesCount = 0;
                    }
                    if (!this.mSurfaceOnly && !info.hwuiCallbackFired) {
                        Log.w(TAG, "Missing HWUI jank callback for vsyncId: " + info.frameVsyncId);
                    }
                } else {
                    totalFramesCount2 = totalFramesCount;
                }
                if (this.mSurfaceOnly || !info.hwuiCallbackFired) {
                    totalFramesCount3 = totalFramesCount2;
                } else {
                    long maxFrameTimeNanos2 = Math.max(info.totalDurationNanos, maxFrameTimeNanos);
                    if (!info.surfaceControlCallbackFired) {
                        Log.w(TAG, "Missing SF jank callback for vsyncId: " + info.frameVsyncId);
                    }
                    totalFramesCount3 = totalFramesCount2;
                    maxFrameTimeNanos = maxFrameTimeNanos2;
                }
            }
            i++;
            z = true;
        }
        int maxSuccessiveMissedFramesCount2 = Math.max(maxSuccessiveMissedFramesCount, successiveMissedFramesCount);
        Trace.traceCounter(4096L, this.mSession.getName() + "#missedFrames", missedFramesCount);
        Trace.traceCounter(4096L, this.mSession.getName() + "#missedAppFrames", missedAppFramesCount);
        Trace.traceCounter(4096L, this.mSession.getName() + "#missedSfFrames", missedSfFramesCount);
        int totalFramesCount4 = totalFramesCount;
        Trace.traceCounter(4096L, this.mSession.getName() + "#totalFrames", totalFramesCount4);
        Trace.traceCounter(4096L, this.mSession.getName() + "#maxFrameTimeMillis", (int) (maxFrameTimeNanos / TimeUtils.NANOS_PER_MS));
        Trace.traceCounter(4096L, this.mSession.getName() + "#maxSuccessiveMissedFrames", maxSuccessiveMissedFramesCount2);
        if (shouldTriggerPerfetto(missedFramesCount, (int) maxFrameTimeNanos)) {
            triggerPerfetto();
        }
        if (this.mSession.logToStatsd()) {
            this.mStatsLog.write(305, this.mSession.getStatsdInteractionType(), totalFramesCount4, missedFramesCount, maxFrameTimeNanos, missedSfFramesCount, missedAppFramesCount, maxSuccessiveMissedFramesCount2);
        }
    }

    private boolean shouldTriggerPerfetto(int missedFramesCount, int maxFrameTimeNanos) {
        int i;
        int i2 = this.mTraceThresholdMissedFrames;
        boolean overMissedFramesThreshold = i2 != -1 && missedFramesCount >= i2;
        boolean overFrameTimeThreshold = (this.mSurfaceOnly || (i = this.mTraceThresholdFrameTimeMillis) == -1 || maxFrameTimeNanos < i * 1000000) ? false : true;
        return overMissedFramesThreshold || overFrameTimeThreshold;
    }

    public void removeObservers() {
        this.mSurfaceControlWrapper.removeJankStatsListener(this);
        if (!this.mSurfaceOnly) {
            this.mRendererWrapper.removeObserver(this.mObserver);
            ViewRootImpl.SurfaceChangedCallback surfaceChangedCallback = this.mSurfaceChangedCallback;
            if (surfaceChangedCallback != null) {
                this.mViewRoot.removeSurfaceChangedCallback(surfaceChangedCallback);
            }
        }
    }

    public void triggerPerfetto() {
        InteractionJankMonitor.getInstance().trigger(this.mSession);
    }

    /* loaded from: classes4.dex */
    public static class FrameMetricsWrapper {
        private final FrameMetrics mFrameMetrics = new FrameMetrics();

        public long[] getTiming() {
            return this.mFrameMetrics.mTimingData;
        }

        public long getMetric(int index) {
            return this.mFrameMetrics.getMetric(index);
        }
    }

    /* loaded from: classes4.dex */
    public static class ThreadedRendererWrapper {
        private final ThreadedRenderer mRenderer;

        public ThreadedRendererWrapper(ThreadedRenderer renderer) {
            this.mRenderer = renderer;
        }

        public void addObserver(HardwareRendererObserver observer) {
            this.mRenderer.addObserver(observer);
        }

        public void removeObserver(HardwareRendererObserver observer) {
            this.mRenderer.removeObserver(observer);
        }
    }

    /* loaded from: classes4.dex */
    public static class ViewRootWrapper {
        private final ViewRootImpl mViewRoot;

        public ViewRootWrapper(ViewRootImpl viewRoot) {
            this.mViewRoot = viewRoot;
        }

        public void addSurfaceChangedCallback(ViewRootImpl.SurfaceChangedCallback callback) {
            this.mViewRoot.addSurfaceChangedCallback(callback);
        }

        public void removeSurfaceChangedCallback(ViewRootImpl.SurfaceChangedCallback callback) {
            this.mViewRoot.removeSurfaceChangedCallback(callback);
        }

        public SurfaceControl getSurfaceControl() {
            return this.mViewRoot.getSurfaceControl();
        }
    }

    /* loaded from: classes4.dex */
    public static class SurfaceControlWrapper {
        public void addJankStatsListener(SurfaceControl.OnJankDataListener listener, SurfaceControl surfaceControl) {
            SurfaceControl.addJankDataListener(listener, surfaceControl);
        }

        public void removeJankStatsListener(SurfaceControl.OnJankDataListener listener) {
            SurfaceControl.removeJankDataListener(listener);
        }
    }

    /* loaded from: classes4.dex */
    public static class ChoreographerWrapper {
        private final Choreographer mChoreographer;

        public ChoreographerWrapper(Choreographer choreographer) {
            this.mChoreographer = choreographer;
        }

        public long getVsyncId() {
            return this.mChoreographer.getVsyncId();
        }
    }

    /* loaded from: classes4.dex */
    public static class StatsLogWrapper {
        public void write(int code, int arg1, long arg2, long arg3, long arg4, long arg5, long arg6, long arg7) {
            FrameworkStatsLog.write(code, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
        }
    }
}
