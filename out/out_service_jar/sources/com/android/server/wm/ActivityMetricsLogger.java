package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IncrementalStatesInfo;
import android.content.pm.dex.ArtManagerInternal;
import android.content.pm.dex.PackageOptimizationInfo;
import android.metrics.LogMaker;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Trace;
import android.os.incremental.IncrementalManager;
import android.util.ArrayMap;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.am.MemoryStatUtil;
import com.android.server.apphibernation.AppHibernationManagerInternal;
import com.android.server.apphibernation.AppHibernationService;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.android.server.wm.ActivityRecord;
import com.mediatek.server.MtkSystemServer;
import com.transsion.hubcore.server.am.ITranActivityManagerService;
import com.transsion.hubcore.server.wm.ITranActivityMetricsLogger;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class ActivityMetricsLogger {
    private static final int IGNORE_CALLER = -1;
    private static final String TAG = "ActivityTaskManager";
    private static final String[] TRON_WINDOW_STATE_VARZ_STRINGS = {"window_time_0", "window_time_1", "window_time_2", "window_time_3", "window_time_4"};
    private static final long UNKNOWN_VISIBILITY_CHECK_DELAY_MS = 3000;
    private static final int WINDOW_STATE_ASSISTANT = 3;
    private static final int WINDOW_STATE_FREEFORM = 2;
    private static final int WINDOW_STATE_INVALID = -1;
    private static final int WINDOW_STATE_MULTI_WINDOW = 4;
    private static final int WINDOW_STATE_SIDE_BY_SIDE = 1;
    private static final int WINDOW_STATE_STANDARD = 0;
    private AppHibernationManagerInternal mAppHibernationManagerInternal;
    private ArtManagerInternal mArtManagerInternal;
    private final LaunchObserverRegistryImpl mLaunchObserver;
    private final ActivityTaskSupervisor mSupervisor;
    private int mWindowState = 0;
    private final MetricsLogger mMetricsLogger = new MetricsLogger();
    private final Handler mLoggerHandler = FgThread.getHandler();
    private final ArrayList<TransitionInfo> mTransitionInfoList = new ArrayList<>();
    private final ArrayMap<ActivityRecord, TransitionInfo> mLastTransitionInfo = new ArrayMap<>();
    private final SparseArray<PackageCompatStateInfo> mPackageUidToCompatStateInfo = new SparseArray<>(0);
    private final StringBuilder mStringBuilder = new StringBuilder();
    private final ArrayMap<String, Boolean> mLastHibernationStates = new ArrayMap<>();
    private long mLastLogTimeSecs = SystemClock.elapsedRealtime() / 1000;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class LaunchingState {
        private static int sTraceSeqId;
        int launchType;
        private TransitionInfo mAssociatedTransitionInfo;
        private long mCurrentTransitionStartTimeNs;
        private final long mCurrentUpTimeMs = SystemClock.uptimeMillis();
        String mTraceName;
        int procType;

        LaunchingState() {
            if (!Trace.isTagEnabled(64L)) {
                return;
            }
            sTraceSeqId++;
            String str = "launchingActivity#" + sTraceSeqId;
            this.mTraceName = str;
            Trace.asyncTraceBegin(64L, str, 0);
        }

        void stopTrace(boolean abort, TransitionInfo endInfo) {
            String str;
            String launchResult;
            String str2 = this.mTraceName;
            if (str2 == null) {
                return;
            }
            if (!abort && endInfo != this.mAssociatedTransitionInfo) {
                return;
            }
            Trace.asyncTraceEnd(64L, str2, 0);
            if (this.mAssociatedTransitionInfo == null) {
                launchResult = ":failed";
            } else {
                StringBuilder sb = new StringBuilder();
                if (abort) {
                    str = ":canceled:";
                } else {
                    str = this.mAssociatedTransitionInfo.mProcessSwitch ? ":completed:" : ":completed-same-process:";
                }
                launchResult = sb.append(str).append(this.mAssociatedTransitionInfo.mLastLaunchedActivity.packageName).toString();
            }
            Trace.instant(64L, this.mTraceName + launchResult);
            this.mTraceName = null;
        }

        boolean allDrawn() {
            TransitionInfo transitionInfo = this.mAssociatedTransitionInfo;
            return transitionInfo != null && transitionInfo.mIsDrawn;
        }

        boolean hasActiveTransitionInfo() {
            return this.mAssociatedTransitionInfo != null;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean contains(ActivityRecord r) {
            TransitionInfo transitionInfo = this.mAssociatedTransitionInfo;
            return transitionInfo != null && transitionInfo.contains(r);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class TransitionInfo {
        boolean isFromAgares;
        int mCurrentTransitionDelayMs;
        boolean mIsDrawn;
        ActivityRecord mLastLaunchedActivity;
        String mLaunchPackageName;
        String mLaunchTraceName;
        final LaunchingState mLaunchingState;
        boolean mLoggedStartingWindowDrawn;
        boolean mLoggedTransitionStarting;
        Runnable mPendingFullyDrawn;
        final boolean mProcessRunning;
        final boolean mProcessSwitch;
        boolean mRelaunched;
        int mSourceEventDelayMs;
        int mSourceType;
        final long mTransitionDeviceUptimeMs;
        final long mTransitionStartTimeNs;
        final int mTransitionType;
        int mWindowsDrawnDelayMs;
        final ArrayList<ActivityRecord> mPendingDrawActivities = new ArrayList<>(2);
        int mStartingWindowDelayMs = -1;
        int mBindApplicationDelayMs = -1;
        int mReason = 3;

        public boolean isFromAgares() {
            return this.isFromAgares;
        }

        public void setFromAgares(boolean fromAgares) {
            this.isFromAgares = fromAgares;
        }

        static TransitionInfo create(ActivityRecord r, LaunchingState launchingState, ActivityOptions options, boolean processRunning, boolean processSwitch, boolean newActivityCreated, int startResult) {
            int transitionType;
            if (startResult != 0 && startResult != 2) {
                return null;
            }
            if (processRunning) {
                if (!newActivityCreated && r.attachedToProcess()) {
                    transitionType = 9;
                } else {
                    transitionType = 8;
                }
            } else {
                transitionType = 7;
            }
            if (ITranActivityManagerService.Instance().isAppLaunchEnable() && launchingState != null) {
                launchingState.launchType = transitionType;
            }
            return new TransitionInfo(r, launchingState, options, transitionType, processRunning, processSwitch);
        }

        private TransitionInfo(ActivityRecord r, LaunchingState launchingState, ActivityOptions options, int transitionType, boolean processRunning, boolean processSwitch) {
            ActivityOptions.SourceInfo sourceInfo;
            this.mSourceEventDelayMs = -1;
            this.mLaunchingState = launchingState;
            this.mTransitionStartTimeNs = launchingState.mCurrentTransitionStartTimeNs;
            this.mTransitionType = transitionType;
            this.mProcessRunning = processRunning;
            this.mProcessSwitch = processSwitch;
            this.mTransitionDeviceUptimeMs = launchingState.mCurrentUpTimeMs;
            setLatestLaunchedActivity(r);
            if (launchingState.mAssociatedTransitionInfo == null) {
                launchingState.mAssociatedTransitionInfo = this;
            }
            if (options != null && (sourceInfo = options.getSourceInfo()) != null) {
                this.mSourceType = sourceInfo.type;
                this.mSourceEventDelayMs = (int) (launchingState.mCurrentUpTimeMs - sourceInfo.eventTimeMs);
            }
        }

        void setLatestLaunchedActivity(ActivityRecord r) {
            ActivityRecord activityRecord = this.mLastLaunchedActivity;
            if (activityRecord == r) {
                return;
            }
            if (activityRecord != null) {
                r.mLaunchCookie = activityRecord.mLaunchCookie;
                this.mLastLaunchedActivity.mLaunchCookie = null;
                r.mLaunchRootTask = this.mLastLaunchedActivity.mLaunchRootTask;
                this.mLastLaunchedActivity.mLaunchRootTask = null;
            }
            this.mLastLaunchedActivity = r;
            this.mIsDrawn = r.isReportedDrawn();
            if (!r.noDisplay) {
                this.mPendingDrawActivities.add(r);
            }
        }

        boolean canCoalesce(ActivityRecord r) {
            return this.mLastLaunchedActivity.mDisplayContent == r.mDisplayContent && this.mLastLaunchedActivity.getWindowingMode() == r.getWindowingMode();
        }

        boolean contains(ActivityRecord r) {
            return r != null && (r == this.mLastLaunchedActivity || this.mPendingDrawActivities.contains(r));
        }

        boolean isInterestingToLoggerAndObserver() {
            return this.mProcessSwitch;
        }

        int calculateCurrentDelay() {
            return calculateDelay(SystemClock.elapsedRealtimeNanos());
        }

        int calculateDelay(long timestampNs) {
            return (int) TimeUnit.NANOSECONDS.toMillis(timestampNs - this.mTransitionStartTimeNs);
        }

        public String toString() {
            return "TransitionInfo{" + Integer.toHexString(System.identityHashCode(this)) + " a=" + this.mLastLaunchedActivity + " d=" + this.mIsDrawn + "}";
        }

        void removePendingDrawActivity(ActivityRecord r) {
            if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
                Slog.i(ActivityMetricsLogger.TAG, "Remove pending draw " + r, new Throwable());
            }
            this.mPendingDrawActivities.remove(r);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class TransitionInfoSnapshot {
        final int activityRecordIdHashCode;
        private final ApplicationInfo applicationInfo;
        private final int bindApplicationDelayMs;
        private final String launchedActivityAppRecordRequiredAbi;
        private final String launchedActivityLaunchToken;
        private final String launchedActivityLaunchedFromPackage;
        final String launchedActivityName;
        final String launchedActivityShortComponentName;
        final String packageName;
        private final String processName;
        private final WindowProcessController processRecord;
        private final int reason;
        final boolean relaunched;
        final int sourceEventDelayMs;
        final int sourceType;
        private final int startingWindowDelayMs;
        final int type;
        final int userId;
        final int windowsDrawnDelayMs;
        final int windowsFullyDrawnDelayMs;

        private TransitionInfoSnapshot(TransitionInfo info) {
            this(info, info.mLastLaunchedActivity, -1);
        }

        private TransitionInfoSnapshot(TransitionInfo info, ActivityRecord launchedActivity, int windowsFullyDrawnDelayMs) {
            String requiredAbi;
            this.applicationInfo = launchedActivity.info.applicationInfo;
            this.packageName = launchedActivity.packageName;
            this.launchedActivityName = launchedActivity.info.name;
            this.launchedActivityLaunchedFromPackage = launchedActivity.launchedFromPackage;
            this.launchedActivityLaunchToken = launchedActivity.info.launchToken;
            if (launchedActivity.app == null) {
                requiredAbi = null;
            } else {
                requiredAbi = launchedActivity.app.getRequiredAbi();
            }
            this.launchedActivityAppRecordRequiredAbi = requiredAbi;
            this.reason = info.mReason;
            this.sourceEventDelayMs = info.mSourceEventDelayMs;
            this.startingWindowDelayMs = info.mStartingWindowDelayMs;
            this.bindApplicationDelayMs = info.mBindApplicationDelayMs;
            this.windowsDrawnDelayMs = info.mWindowsDrawnDelayMs;
            this.type = info.mTransitionType;
            this.processRecord = launchedActivity.app;
            this.processName = launchedActivity.processName;
            this.sourceType = info.mSourceType;
            this.userId = launchedActivity.mUserId;
            this.launchedActivityShortComponentName = launchedActivity.shortComponentName;
            this.activityRecordIdHashCode = System.identityHashCode(launchedActivity);
            this.windowsFullyDrawnDelayMs = windowsFullyDrawnDelayMs;
            this.relaunched = info.mRelaunched;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public int getLaunchState() {
            switch (this.type) {
                case 7:
                    return 1;
                case 8:
                    return 2;
                case 9:
                    return this.relaunched ? 4 : 3;
                default:
                    return -1;
            }
        }

        PackageOptimizationInfo getPackageOptimizationInfo(ArtManagerInternal artManagerInternal) {
            String str;
            if (artManagerInternal == null || (str = this.launchedActivityAppRecordRequiredAbi) == null) {
                return PackageOptimizationInfo.createWithNoInfo();
            }
            return artManagerInternal.getPackageOptimizationInfo(this.applicationInfo, str, this.launchedActivityName);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class PackageCompatStateInfo {
        ActivityRecord mLastLoggedActivity;
        int mLastLoggedState;
        final ArrayList<ActivityRecord> mVisibleActivities;

        private PackageCompatStateInfo() {
            this.mVisibleActivities = new ArrayList<>();
            this.mLastLoggedState = 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityMetricsLogger(ActivityTaskSupervisor supervisor, Looper looper) {
        this.mSupervisor = supervisor;
        this.mLaunchObserver = new LaunchObserverRegistryImpl(looper);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logWindowState(String state, int durationSecs) {
        this.mMetricsLogger.count(state, durationSecs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void logWindowState() {
        long now = SystemClock.elapsedRealtime() / 1000;
        if (this.mWindowState != -1) {
            this.mLoggerHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.wm.ActivityMetricsLogger$$ExternalSyntheticLambda1
                public final void accept(Object obj, Object obj2, Object obj3) {
                    ((ActivityMetricsLogger) obj).logWindowState((String) obj2, ((Integer) obj3).intValue());
                }
            }, this, TRON_WINDOW_STATE_VARZ_STRINGS[this.mWindowState], Integer.valueOf((int) (now - this.mLastLogTimeSecs))));
        }
        this.mLastLogTimeSecs = now;
        this.mWindowState = -1;
        Task focusedTask = this.mSupervisor.mRootWindowContainer.getTopDisplayFocusedRootTask();
        if (focusedTask == null) {
            return;
        }
        if (focusedTask.isActivityTypeAssistant()) {
            this.mWindowState = 3;
            return;
        }
        int windowingMode = focusedTask.getWindowingMode();
        switch (windowingMode) {
            case 1:
                this.mWindowState = 0;
                return;
            case 5:
                this.mWindowState = 2;
                return;
            case 6:
                this.mWindowState = 4;
                return;
            default:
                if (windowingMode != 0) {
                    Slog.wtf(TAG, "Unknown windowing mode for task=" + focusedTask + " windowingMode=" + windowingMode);
                    return;
                }
                return;
        }
    }

    private TransitionInfo getActiveTransitionInfo(ActivityRecord r) {
        for (int i = this.mTransitionInfoList.size() - 1; i >= 0; i--) {
            TransitionInfo info = this.mTransitionInfoList.get(i);
            if (info.contains(r)) {
                return info;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public LaunchingState notifyActivityLaunching(Intent intent) {
        return notifyActivityLaunching(intent, null, -1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public LaunchingState notifyActivityLaunching(Intent intent, ActivityRecord caller, int callingUid) {
        long transitionStartTimeNs = SystemClock.elapsedRealtimeNanos();
        TransitionInfo existingInfo = null;
        if (callingUid != -1) {
            int i = this.mTransitionInfoList.size() - 1;
            while (true) {
                if (i < 0) {
                    break;
                }
                TransitionInfo info = this.mTransitionInfoList.get(i);
                if (caller != null && info.contains(caller)) {
                    existingInfo = info;
                    break;
                }
                if (existingInfo == null && callingUid == info.mLastLaunchedActivity.getUid()) {
                    existingInfo = info;
                }
                i--;
            }
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
            Slog.i(TAG, "notifyActivityLaunching intent=" + intent + " existingInfo=" + existingInfo);
        }
        if (existingInfo == null) {
            LaunchingState launchingState = new LaunchingState();
            launchingState.mCurrentTransitionStartTimeNs = transitionStartTimeNs;
            launchObserverNotifyIntentStarted(intent, transitionStartTimeNs);
            return launchingState;
        }
        existingInfo.mLaunchingState.mCurrentTransitionStartTimeNs = transitionStartTimeNs;
        return existingInfo.mLaunchingState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyActivityLaunched(LaunchingState launchingState, int resultCode, boolean newActivityCreated, ActivityRecord launchedActivity, ActivityOptions options) {
        WindowProcessController processController;
        if (launchedActivity == null) {
            abort(launchingState, "nothing launched");
            return;
        }
        if (launchedActivity.app != null) {
            processController = launchedActivity.app;
        } else {
            processController = this.mSupervisor.mService.getProcessController(launchedActivity.processName, launchedActivity.info.applicationInfo.uid);
        }
        WindowProcessController processRecord = processController;
        notifyActivityLaunchedByApplaunch(launchingState, processRecord);
        boolean z = false;
        boolean processRunning = processRecord != null;
        boolean processSwitch = (processRunning && processRecord.hasStartedActivity(launchedActivity)) ? true : true;
        TransitionInfo info = launchingState.mAssociatedTransitionInfo;
        if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
            Slog.i(TAG, "notifyActivityLaunched resultCode=" + resultCode + " launchedActivity=" + launchedActivity + " processRunning=" + processRunning + " processSwitch=" + processSwitch + " newActivityCreated=" + newActivityCreated + " info=" + info);
        }
        if (launchedActivity.isReportedDrawn() && launchedActivity.isVisible()) {
            abort(launchingState, "launched activity already visible");
        } else if (info != null && info.canCoalesce(launchedActivity)) {
            if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
                Slog.i(TAG, "notifyActivityLaunched consecutive launch");
            }
            boolean crossPackage = !info.mLastLaunchedActivity.packageName.equals(launchedActivity.packageName);
            if (crossPackage) {
                if (ITranActivityMetricsLogger.Instance().isAgaresEnable()) {
                    info.setFromAgares(ITranActivityMetricsLogger.Instance().isAgaresProcess(processRecord));
                }
                stopLaunchTrace(info);
            }
            this.mLastTransitionInfo.remove(info.mLastLaunchedActivity);
            info.setLatestLaunchedActivity(launchedActivity);
            this.mLastTransitionInfo.put(launchedActivity, info);
            if (crossPackage) {
                startLaunchTrace(info);
            }
            scheduleCheckActivityToBeDrawnIfSleeping(launchedActivity);
        } else {
            TransitionInfo newInfo = TransitionInfo.create(launchedActivity, launchingState, options, processRunning, processSwitch, newActivityCreated, resultCode);
            if (newInfo == null) {
                abort(launchingState, "unrecognized launch");
                return;
            }
            if (ITranActivityMetricsLogger.Instance().isAgaresEnable()) {
                newInfo.setFromAgares(ITranActivityMetricsLogger.Instance().isAgaresProcess(processRecord));
            }
            if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
                Slog.i(TAG, "notifyActivityLaunched successful");
            }
            this.mTransitionInfoList.add(newInfo);
            this.mLastTransitionInfo.put(launchedActivity, newInfo);
            startLaunchTrace(newInfo);
            if (!newInfo.isInterestingToLoggerAndObserver()) {
                launchObserverNotifyIntentFailed(newInfo.mTransitionStartTimeNs);
            } else {
                launchObserverNotifyActivityLaunched(newInfo);
            }
            scheduleCheckActivityToBeDrawnIfSleeping(launchedActivity);
            for (int i = this.mTransitionInfoList.size() - 2; i >= 0; i--) {
                TransitionInfo prevInfo = this.mTransitionInfoList.get(i);
                if (prevInfo.mIsDrawn || !prevInfo.mLastLaunchedActivity.mVisibleRequested) {
                    scheduleCheckActivityToBeDrawn(prevInfo.mLastLaunchedActivity, 0L);
                }
            }
        }
    }

    private void scheduleCheckActivityToBeDrawnIfSleeping(ActivityRecord r) {
        if (r.mDisplayContent.isSleeping()) {
            scheduleCheckActivityToBeDrawn(r, 3000L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TransitionInfoSnapshot notifyWindowsDrawn(ActivityRecord r, long timestampNs) {
        if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
            Slog.i(TAG, "notifyWindowsDrawn " + r);
        }
        TransitionInfo info = getActiveTransitionInfo(r);
        if (info == null || info.mIsDrawn) {
            if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
                Slog.i(TAG, "notifyWindowsDrawn not pending drawn " + info);
            }
            return null;
        }
        info.mWindowsDrawnDelayMs = info.calculateDelay(timestampNs);
        info.mIsDrawn = true;
        info.removePendingDrawActivity(r);
        TransitionInfoSnapshot infoSnapshot = new TransitionInfoSnapshot(info);
        if (info.mLoggedTransitionStarting) {
            done(false, info, "notifyWindowsDrawn", timestampNs);
        }
        if (r.mWmService.isRecentsAnimationTarget(r)) {
            r.mWmService.getRecentsAnimationController().logRecentsAnimationStartTime(info.mSourceEventDelayMs + info.mWindowsDrawnDelayMs);
        }
        return infoSnapshot;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyStartingWindowDrawn(ActivityRecord r) {
        TransitionInfo info = getActiveTransitionInfo(r);
        if (info == null || info.mLoggedStartingWindowDrawn) {
            return;
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
            Slog.i(TAG, "notifyStartingWindowDrawn " + r);
        }
        info.mLoggedStartingWindowDrawn = true;
        info.mStartingWindowDelayMs = info.calculateDelay(SystemClock.elapsedRealtimeNanos());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyTransitionStarting(ArrayMap<WindowContainer, Integer> activityToReason) {
        if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
            Slog.i(TAG, "notifyTransitionStarting " + activityToReason);
        }
        long timestampNs = SystemClock.elapsedRealtimeNanos();
        for (int index = activityToReason.size() - 1; index >= 0; index--) {
            WindowContainer<?> wc = activityToReason.keyAt(index);
            ActivityRecord activity = wc.asActivityRecord();
            ActivityRecord r = activity != null ? activity : wc.getTopActivity(false, true);
            TransitionInfo info = getActiveTransitionInfo(r);
            if (info != null && !info.mLoggedTransitionStarting) {
                if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
                    Slog.i(TAG, "notifyTransitionStarting activity=" + wc + " info=" + info);
                }
                info.mCurrentTransitionDelayMs = info.calculateDelay(timestampNs);
                info.mReason = activityToReason.valueAt(index).intValue();
                info.mLoggedTransitionStarting = true;
                if (info.mIsDrawn) {
                    done(false, info, "notifyTransitionStarting drawn", timestampNs);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyActivityRelaunched(ActivityRecord r) {
        TransitionInfo info = getActiveTransitionInfo(r);
        if (info != null) {
            info.mRelaunched = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyActivityRemoved(ActivityRecord r) {
        this.mLastTransitionInfo.remove(r);
        TransitionInfo info = getActiveTransitionInfo(r);
        if (info != null) {
            abort(info, "removed");
        }
        int packageUid = r.info.applicationInfo.uid;
        PackageCompatStateInfo compatStateInfo = this.mPackageUidToCompatStateInfo.get(packageUid);
        if (compatStateInfo == null) {
            return;
        }
        compatStateInfo.mVisibleActivities.remove(r);
        if (compatStateInfo.mLastLoggedActivity == r) {
            compatStateInfo.mLastLoggedActivity = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyVisibilityChanged(ActivityRecord r) {
        TransitionInfo info = getActiveTransitionInfo(r);
        if (info == null) {
            return;
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
            Slog.i(TAG, "notifyVisibilityChanged " + r + " visible=" + r.mVisibleRequested + " state=" + r.getState() + " finishing=" + r.finishing);
        }
        if (r.isState(ActivityRecord.State.RESUMED) && r.mDisplayContent.isSleeping()) {
            return;
        }
        if (!r.mVisibleRequested || r.finishing) {
            info.removePendingDrawActivity(r);
            scheduleCheckActivityToBeDrawn(r, 0L);
        }
    }

    private void scheduleCheckActivityToBeDrawn(ActivityRecord r, long delay) {
        r.mAtmService.mH.sendMessageDelayed(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.wm.ActivityMetricsLogger$$ExternalSyntheticLambda6
            public final void accept(Object obj, Object obj2, Object obj3) {
                ((ActivityMetricsLogger) obj).checkActivityToBeDrawn((Task) obj2, (ActivityRecord) obj3);
            }
        }, this, r.getTask(), r), delay);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkActivityToBeDrawn(Task t, ActivityRecord r) {
        synchronized (this.mSupervisor.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                TransitionInfo info = getActiveTransitionInfo(r);
                if (info == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                } else if (t != null && t.forAllActivities(new Predicate() { // from class: com.android.server.wm.ActivityMetricsLogger$$ExternalSyntheticLambda0
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return ActivityMetricsLogger.lambda$checkActivityToBeDrawn$0((ActivityRecord) obj);
                    }
                })) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                } else {
                    if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
                        Slog.i(TAG, "checkActivityToBeDrawn cancels activity=" + r);
                    }
                    logAppTransitionCancel(info);
                    abort(info, "checkActivityToBeDrawn (invisible or drawn already)");
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$checkActivityToBeDrawn$0(ActivityRecord a) {
        return (!a.mVisibleRequested || a.isReportedDrawn() || a.finishing) ? false : true;
    }

    private AppHibernationManagerInternal getAppHibernationManagerInternal() {
        if (AppHibernationService.isAppHibernationEnabled()) {
            if (this.mAppHibernationManagerInternal == null) {
                this.mAppHibernationManagerInternal = (AppHibernationManagerInternal) LocalServices.getService(AppHibernationManagerInternal.class);
            }
            return this.mAppHibernationManagerInternal;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyBeforePackageUnstopped(String packageName) {
        AppHibernationManagerInternal ahmInternal = getAppHibernationManagerInternal();
        if (ahmInternal != null) {
            this.mLastHibernationStates.put(packageName, Boolean.valueOf(ahmInternal.isHibernatingGlobally(packageName)));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyBindApplication(ApplicationInfo appInfo) {
        for (int i = this.mTransitionInfoList.size() - 1; i >= 0; i--) {
            TransitionInfo info = this.mTransitionInfoList.get(i);
            if (info.mLastLaunchedActivity.info.applicationInfo == appInfo) {
                info.mBindApplicationDelayMs = info.calculateCurrentDelay();
            }
        }
    }

    private void abort(LaunchingState state, String cause) {
        if (state.mAssociatedTransitionInfo != null) {
            abort(state.mAssociatedTransitionInfo, cause);
            return;
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
            Slog.i(TAG, "abort launch cause=" + cause);
        }
        state.stopTrace(true, null);
        launchObserverNotifyIntentFailed(state.mCurrentTransitionStartTimeNs);
    }

    private void abort(TransitionInfo info, String cause) {
        done(true, info, cause, 0L);
    }

    private void done(boolean abort, TransitionInfo info, String cause, long timestampNs) {
        if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
            Slog.i(TAG, "done abort=" + abort + " cause=" + cause + " timestamp=" + timestampNs + " info=" + info);
        }
        info.mLaunchingState.stopTrace(abort, info);
        stopLaunchTrace(info);
        Boolean isHibernating = this.mLastHibernationStates.remove(info.mLastLaunchedActivity.packageName);
        if (abort) {
            this.mLastTransitionInfo.remove(info.mLastLaunchedActivity);
            this.mSupervisor.stopWaitingForActivityVisible(info.mLastLaunchedActivity);
            launchObserverNotifyActivityLaunchCancelled(info);
        } else {
            if (info.isInterestingToLoggerAndObserver()) {
                launchObserverNotifyActivityLaunchFinished(info, timestampNs);
            }
            logAppTransitionFinished(info, isHibernating != null ? isHibernating.booleanValue() : false);
        }
        info.mPendingDrawActivities.clear();
        this.mTransitionInfoList.remove(info);
    }

    private void logAppTransitionCancel(TransitionInfo info) {
        int type = info.mTransitionType;
        ActivityRecord activity = info.mLastLaunchedActivity;
        LogMaker builder = new LogMaker(1144);
        builder.setPackageName(activity.packageName);
        builder.setType(type);
        builder.addTaggedData(871, activity.info.name);
        this.mMetricsLogger.write(builder);
        FrameworkStatsLog.write(49, activity.info.applicationInfo.uid, activity.packageName, getAppStartTransitionType(type, info.mRelaunched), activity.info.name);
        if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
            Slog.i(TAG, String.format("APP_START_CANCELED(%s, %s, %s, %s)", Integer.valueOf(activity.info.applicationInfo.uid), activity.packageName, Integer.valueOf(getAppStartTransitionType(type, info.mRelaunched)), activity.info.name));
        }
    }

    private void logAppTransitionFinished(TransitionInfo info, final boolean isHibernating) {
        if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
            Slog.i(TAG, "logging finished transition " + info);
        }
        final TransitionInfoSnapshot infoSnapshot = new TransitionInfoSnapshot(info);
        if (info.isInterestingToLoggerAndObserver()) {
            final long timestamp = info.mTransitionStartTimeNs;
            final long uptime = info.mTransitionDeviceUptimeMs;
            final int transitionDelay = info.mCurrentTransitionDelayMs;
            this.mLoggerHandler.post(new Runnable() { // from class: com.android.server.wm.ActivityMetricsLogger$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    ActivityMetricsLogger.this.m7755xc7467417(timestamp, uptime, transitionDelay, infoSnapshot, isHibernating);
                }
            });
        }
        this.mLoggerHandler.post(new Runnable() { // from class: com.android.server.wm.ActivityMetricsLogger$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                ActivityMetricsLogger.this.m7756x61e73698(infoSnapshot);
            }
        });
        if (info.mPendingFullyDrawn != null) {
            info.mPendingFullyDrawn.run();
        }
        info.mLastLaunchedActivity.info.launchToken = null;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: logAppTransition */
    public void m7755xc7467417(long transitionStartTimeNs, long transitionDeviceUptimeMs, int currentTransitionDelayMs, TransitionInfoSnapshot info, boolean isHibernating) {
        boolean isIncremental;
        boolean isLoading;
        LogMaker builder = new LogMaker(761);
        builder.setPackageName(info.packageName);
        builder.setType(info.type);
        builder.addTaggedData(871, info.launchedActivityName);
        boolean isInstantApp = info.applicationInfo.isInstantApp();
        if (info.launchedActivityLaunchedFromPackage != null) {
            builder.addTaggedData(904, info.launchedActivityLaunchedFromPackage);
        }
        String launchToken = info.launchedActivityLaunchToken;
        if (launchToken != null) {
            builder.addTaggedData(903, launchToken);
        }
        builder.addTaggedData(905, Integer.valueOf(isInstantApp ? 1 : 0));
        builder.addTaggedData((int) FrameworkStatsLog.APP_PROCESS_DIED__IMPORTANCE__IMPORTANCE_TOP_SLEEPING, Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(transitionDeviceUptimeMs)));
        builder.addTaggedData(319, Integer.valueOf(currentTransitionDelayMs));
        builder.setSubtype(info.reason);
        if (info.startingWindowDelayMs != -1) {
            builder.addTaggedData(321, Integer.valueOf(info.startingWindowDelayMs));
        }
        if (info.bindApplicationDelayMs != -1) {
            builder.addTaggedData(945, Integer.valueOf(info.bindApplicationDelayMs));
        }
        builder.addTaggedData(322, Integer.valueOf(info.windowsDrawnDelayMs));
        PackageOptimizationInfo packageOptimizationInfo = info.getPackageOptimizationInfo(getArtManagerInternal());
        builder.addTaggedData(1321, Integer.valueOf(packageOptimizationInfo.getCompilationReason()));
        builder.addTaggedData(1320, Integer.valueOf(packageOptimizationInfo.getCompilationFilter()));
        this.mMetricsLogger.write(builder);
        String codePath = info.applicationInfo.getCodePath();
        if (codePath != null && IncrementalManager.isIncrementalPath(codePath)) {
            boolean isLoading2 = isIncrementalLoading(info.packageName, info.userId);
            isIncremental = true;
            isLoading = isLoading2;
        } else {
            isIncremental = false;
            isLoading = false;
        }
        FrameworkStatsLog.write(48, info.applicationInfo.uid, info.packageName, getAppStartTransitionType(info.type, info.relaunched), info.launchedActivityName, info.launchedActivityLaunchedFromPackage, isInstantApp, 0L, info.reason, currentTransitionDelayMs, info.startingWindowDelayMs, info.bindApplicationDelayMs, info.windowsDrawnDelayMs, launchToken, packageOptimizationInfo.getCompilationReason(), packageOptimizationInfo.getCompilationFilter(), info.sourceType, info.sourceEventDelayMs, isHibernating, isIncremental, isLoading, info.launchedActivityName.hashCode(), TimeUnit.NANOSECONDS.toMillis(transitionStartTimeNs));
        if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
            Slog.i(TAG, String.format("APP_START_OCCURRED(%s, %s, %s, %s, %s)", Integer.valueOf(info.applicationInfo.uid), info.packageName, Integer.valueOf(getAppStartTransitionType(info.type, info.relaunched)), info.launchedActivityName, info.launchedActivityLaunchedFromPackage));
        }
        logAppStartMemoryStateCapture(info);
    }

    private boolean isIncrementalLoading(String packageName, int userId) {
        IncrementalStatesInfo info = this.mSupervisor.mService.getPackageManagerInternalLocked().getIncrementalStatesInfo(packageName, 0, userId);
        return info != null && info.isLoading();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: logAppDisplayed */
    public void m7756x61e73698(TransitionInfoSnapshot info) {
        if (info.type != 8 && info.type != 7) {
            return;
        }
        EventLog.writeEvent((int) EventLogTags.WM_ACTIVITY_LAUNCH_TIME, Integer.valueOf(info.userId), Integer.valueOf(info.activityRecordIdHashCode), info.launchedActivityShortComponentName, Integer.valueOf(info.windowsDrawnDelayMs));
        StringBuilder sb = this.mStringBuilder;
        sb.setLength(0);
        sb.append("Displayed ");
        sb.append(info.launchedActivityShortComponentName);
        sb.append(": ");
        TimeUtils.formatDuration(info.windowsDrawnDelayMs, sb);
        Log.i(TAG, sb.toString());
        MtkSystemServer.getInstance().addBootEvent("AP_Launch: " + info.launchedActivityShortComponentName + " " + info.windowsDrawnDelayMs + "ms");
    }

    private static int getAppStartTransitionType(int tronType, boolean relaunched) {
        if (tronType == 7) {
            return 3;
        }
        if (tronType == 8) {
            return 1;
        }
        if (tronType == 9) {
            if (relaunched) {
                return 4;
            }
            return 2;
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TransitionInfoSnapshot logAppTransitionReportedDrawn(final ActivityRecord r, final boolean restoredFromBundle) {
        long startupTimeMs;
        int i;
        boolean isIncremental;
        boolean isLoading;
        int i2;
        final TransitionInfo info = this.mLastTransitionInfo.get(r);
        if (info == null) {
            return null;
        }
        if (!info.mIsDrawn && info.mPendingFullyDrawn == null) {
            info.mPendingFullyDrawn = new Runnable() { // from class: com.android.server.wm.ActivityMetricsLogger$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    ActivityMetricsLogger.this.m7757x62e1c4c0(r, restoredFromBundle, info);
                }
            };
            return null;
        }
        long currentTimestampNs = SystemClock.elapsedRealtimeNanos();
        if (info.mPendingFullyDrawn != null) {
            startupTimeMs = info.mWindowsDrawnDelayMs;
        } else {
            startupTimeMs = TimeUnit.NANOSECONDS.toMillis(currentTimestampNs - info.mTransitionStartTimeNs);
        }
        final TransitionInfoSnapshot infoSnapshot = new TransitionInfoSnapshot(info, r, (int) startupTimeMs);
        this.mLoggerHandler.post(new Runnable() { // from class: com.android.server.wm.ActivityMetricsLogger$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                ActivityMetricsLogger.this.m7758xfd828741(infoSnapshot);
            }
        });
        this.mLastTransitionInfo.remove(r);
        if (info.isInterestingToLoggerAndObserver()) {
            Trace.traceBegin(64L, "ActivityManager:ReportingFullyDrawn " + info.mLastLaunchedActivity.packageName);
            LogMaker builder = new LogMaker(1090);
            builder.setPackageName(r.packageName);
            builder.addTaggedData(871, r.info.name);
            builder.addTaggedData(1091, Long.valueOf(startupTimeMs));
            if (restoredFromBundle) {
                i = 13;
            } else {
                i = 12;
            }
            builder.setType(i);
            builder.addTaggedData((int) FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_ACTIVE_DEVICE_ADMIN, Integer.valueOf(info.mProcessRunning ? 1 : 0));
            this.mMetricsLogger.write(builder);
            PackageOptimizationInfo packageOptimizationInfo = infoSnapshot.getPackageOptimizationInfo(getArtManagerInternal());
            String codePath = info.mLastLaunchedActivity.info.applicationInfo.getCodePath();
            if (codePath != null && IncrementalManager.isIncrementalPath(codePath)) {
                boolean isLoading2 = isIncrementalLoading(info.mLastLaunchedActivity.packageName, info.mLastLaunchedActivity.mUserId);
                isIncremental = true;
                isLoading = isLoading2;
            } else {
                isIncremental = false;
                isLoading = false;
            }
            int i3 = info.mLastLaunchedActivity.info.applicationInfo.uid;
            String str = info.mLastLaunchedActivity.packageName;
            if (restoredFromBundle) {
                i2 = 1;
            } else {
                i2 = 2;
            }
            FrameworkStatsLog.write(50, i3, str, i2, info.mLastLaunchedActivity.info.name, info.mProcessRunning, startupTimeMs, packageOptimizationInfo.getCompilationReason(), packageOptimizationInfo.getCompilationFilter(), info.mSourceType, info.mSourceEventDelayMs, isIncremental, isLoading, info.mLastLaunchedActivity.info.name.hashCode());
            Trace.traceEnd(64L);
            launchObserverNotifyReportFullyDrawn(info, currentTimestampNs);
            return infoSnapshot;
        }
        return infoSnapshot;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$logAppTransitionReportedDrawn$3$com-android-server-wm-ActivityMetricsLogger  reason: not valid java name */
    public /* synthetic */ void m7757x62e1c4c0(ActivityRecord r, boolean restoredFromBundle, TransitionInfo info) {
        logAppTransitionReportedDrawn(r, restoredFromBundle);
        info.mPendingFullyDrawn = null;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: logAppFullyDrawn */
    public void m7758xfd828741(TransitionInfoSnapshot info) {
        if (info.type != 8 && info.type != 7) {
            return;
        }
        StringBuilder sb = this.mStringBuilder;
        sb.setLength(0);
        sb.append("Fully drawn ");
        sb.append(info.launchedActivityShortComponentName);
        sb.append(": ");
        TimeUtils.formatDuration(info.windowsFullyDrawnDelayMs, sb);
        Log.i(TAG, sb.toString());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void logAbortedBgActivityStart(Intent intent, WindowProcessController callerApp, int callingUid, String callingPackage, int callingUidProcState, boolean callingUidHasAnyVisibleWindow, int realCallingUid, int realCallingUidProcState, boolean realCallingUidHasAnyVisibleWindow, boolean comingFromPendingIntent) {
        long nowElapsed = SystemClock.elapsedRealtime();
        long nowUptime = SystemClock.uptimeMillis();
        LogMaker builder = new LogMaker(1513);
        builder.setTimestamp(System.currentTimeMillis());
        builder.addTaggedData(1514, Integer.valueOf(callingUid));
        builder.addTaggedData(1515, callingPackage);
        builder.addTaggedData(1516, Integer.valueOf(ActivityManager.processStateAmToProto(callingUidProcState)));
        builder.addTaggedData(1517, Integer.valueOf(callingUidHasAnyVisibleWindow ? 1 : 0));
        builder.addTaggedData(1518, Integer.valueOf(realCallingUid));
        builder.addTaggedData(1519, Integer.valueOf(ActivityManager.processStateAmToProto(realCallingUidProcState)));
        builder.addTaggedData(1520, Integer.valueOf(realCallingUidHasAnyVisibleWindow ? 1 : 0));
        builder.addTaggedData(1527, Integer.valueOf(comingFromPendingIntent ? 1 : 0));
        if (intent != null) {
            builder.addTaggedData(1528, intent.getAction());
            ComponentName component = intent.getComponent();
            if (component != null) {
                builder.addTaggedData(1526, component.flattenToShortString());
            }
        }
        if (callerApp != null) {
            builder.addTaggedData(1529, callerApp.mName);
            builder.addTaggedData(1530, Integer.valueOf(ActivityManager.processStateAmToProto(callerApp.getCurrentProcState())));
            builder.addTaggedData(1531, Integer.valueOf(callerApp.hasClientActivities() ? 1 : 0));
            builder.addTaggedData(1532, Integer.valueOf(callerApp.hasForegroundServices() ? 1 : 0));
            builder.addTaggedData(1533, Integer.valueOf(callerApp.hasForegroundActivities() ? 1 : 0));
            builder.addTaggedData(1534, Integer.valueOf(callerApp.hasTopUi() ? 1 : 0));
            builder.addTaggedData(1535, Integer.valueOf(callerApp.hasOverlayUi() ? 1 : 0));
            builder.addTaggedData(1536, Integer.valueOf(callerApp.hasPendingUiClean() ? 1 : 0));
            if (callerApp.getInteractionEventTime() != 0) {
                builder.addTaggedData((int) UsbTerminalTypes.TERMINAL_EXTERN_ANALOG, Long.valueOf(nowElapsed - callerApp.getInteractionEventTime()));
            }
            if (callerApp.getFgInteractionTime() != 0) {
                builder.addTaggedData((int) UsbTerminalTypes.TERMINAL_EXTERN_DIGITAL, Long.valueOf(nowElapsed - callerApp.getFgInteractionTime()));
            }
            if (callerApp.getWhenUnimportant() != 0) {
                builder.addTaggedData(1539, Long.valueOf(nowUptime - callerApp.getWhenUnimportant()));
            }
        }
        this.mMetricsLogger.write(builder);
    }

    private void logAppStartMemoryStateCapture(TransitionInfoSnapshot info) {
        if (info.processRecord == null) {
            if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
                Slog.i(TAG, "logAppStartMemoryStateCapture processRecord null");
                return;
            }
            return;
        }
        int pid = info.processRecord.getPid();
        int uid = info.applicationInfo.uid;
        MemoryStatUtil.MemoryStat memoryStat = MemoryStatUtil.readMemoryStatFromFilesystem(uid, pid);
        if (memoryStat == null) {
            if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
                Slog.i(TAG, "logAppStartMemoryStateCapture memoryStat null");
                return;
            }
            return;
        }
        FrameworkStatsLog.write(55, uid, info.processName, info.launchedActivityName, memoryStat.pgfault, memoryStat.pgmajfault, memoryStat.rssInBytes, memoryStat.cacheInBytes, memoryStat.swapInBytes);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void logAppCompatState(ActivityRecord activity) {
        int packageUid = activity.info.applicationInfo.uid;
        int state = activity.getAppCompatState();
        if (!this.mPackageUidToCompatStateInfo.contains(packageUid)) {
            this.mPackageUidToCompatStateInfo.put(packageUid, new PackageCompatStateInfo());
        }
        PackageCompatStateInfo compatStateInfo = this.mPackageUidToCompatStateInfo.get(packageUid);
        int lastLoggedState = compatStateInfo.mLastLoggedState;
        ActivityRecord lastLoggedActivity = compatStateInfo.mLastLoggedActivity;
        boolean isVisible = state != 1;
        ArrayList<ActivityRecord> visibleActivities = compatStateInfo.mVisibleActivities;
        if (isVisible && !visibleActivities.contains(activity)) {
            visibleActivities.add(activity);
        } else if (!isVisible) {
            visibleActivities.remove(activity);
            if (visibleActivities.isEmpty()) {
                this.mPackageUidToCompatStateInfo.remove(packageUid);
            }
        }
        if (state == lastLoggedState) {
            return;
        }
        if (!isVisible && !visibleActivities.isEmpty()) {
            if (lastLoggedActivity == null || activity == lastLoggedActivity) {
                findAppCompatStateToLog(compatStateInfo, packageUid);
            }
        } else if (lastLoggedActivity != null && activity != lastLoggedActivity && lastLoggedState != 1 && lastLoggedState != 2) {
        } else {
            logAppCompatStateInternal(activity, state, packageUid, compatStateInfo);
        }
    }

    private void findAppCompatStateToLog(PackageCompatStateInfo compatStateInfo, int packageUid) {
        ArrayList<ActivityRecord> visibleActivities = compatStateInfo.mVisibleActivities;
        int lastLoggedState = compatStateInfo.mLastLoggedState;
        ActivityRecord activityToLog = null;
        int stateToLog = 1;
        for (int i = 0; i < visibleActivities.size(); i++) {
            ActivityRecord activity = visibleActivities.get(i);
            int state = activity.getAppCompatState();
            if (state == lastLoggedState) {
                compatStateInfo.mLastLoggedActivity = activity;
                return;
            }
            if (state == 1) {
                Slog.w(TAG, "Visible activity with NOT_VISIBLE App Compat state for package UID: " + packageUid);
            } else if (stateToLog == 1 || (stateToLog == 2 && state != 2)) {
                activityToLog = activity;
                stateToLog = state;
            }
        }
        if (activityToLog != null && stateToLog != 1) {
            logAppCompatStateInternal(activityToLog, stateToLog, packageUid, compatStateInfo);
        }
    }

    private void logAppCompatStateInternal(ActivityRecord activity, int state, int packageUid, PackageCompatStateInfo compatStateInfo) {
        compatStateInfo.mLastLoggedState = state;
        compatStateInfo.mLastLoggedActivity = activity;
        FrameworkStatsLog.write((int) FrameworkStatsLog.APP_COMPAT_STATE_CHANGED, packageUid, state);
        if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
            Slog.i(TAG, String.format("APP_COMPAT_STATE_CHANGED(%s, %s)", Integer.valueOf(packageUid), Integer.valueOf(state)));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void logCameraCompatControlAppearedEventReported(int state, int packageUid) {
        switch (state) {
            case 0:
                return;
            case 1:
                logCameraCompatControlEventReported(1, packageUid);
                return;
            case 2:
                logCameraCompatControlEventReported(2, packageUid);
                return;
            default:
                Slog.w(TAG, "Unexpected state in logCameraCompatControlAppearedEventReported: " + state);
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void logCameraCompatControlClickedEventReported(int state, int packageUid) {
        switch (state) {
            case 1:
                logCameraCompatControlEventReported(4, packageUid);
                return;
            case 2:
                logCameraCompatControlEventReported(3, packageUid);
                return;
            case 3:
                logCameraCompatControlEventReported(5, packageUid);
                return;
            default:
                Slog.w(TAG, "Unexpected state in logCameraCompatControlAppearedEventReported: " + state);
                return;
        }
    }

    private void logCameraCompatControlEventReported(int event, int packageUid) {
        FrameworkStatsLog.write((int) FrameworkStatsLog.CAMERA_COMPAT_CONTROL_EVENT_REPORTED, packageUid, event);
        if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
            Slog.i(TAG, String.format("CAMERA_COMPAT_CONTROL_EVENT_REPORTED(%s, %s)", Integer.valueOf(packageUid), Integer.valueOf(event)));
        }
    }

    private ArtManagerInternal getArtManagerInternal() {
        if (this.mArtManagerInternal == null) {
            this.mArtManagerInternal = (ArtManagerInternal) LocalServices.getService(ArtManagerInternal.class);
        }
        return this.mArtManagerInternal;
    }

    private void startLaunchTrace(TransitionInfo info) {
        if (Build.TRANCARE_SUPPORT && info != null && info.mLastLaunchedActivity != null) {
            if (ITranActivityMetricsLogger.Instance().isAgaresEnable()) {
                ITranActivityMetricsLogger.Instance().onAgaresActivityStart(info.mLastLaunchedActivity.packageName, info.mLastLaunchedActivity.mActivityComponent.getShortClassName(), info.mTransitionType, info.isFromAgares());
            }
            if (ITranActivityMetricsLogger.Instance().isGameBoosterEnable()) {
                ITranActivityMetricsLogger.Instance().onActivityStart(info.mLastLaunchedActivity.packageName, info.mLastLaunchedActivity.mActivityComponent.getShortClassName(), info.mTransitionType, info.isFromAgares());
            }
        }
        if (Build.TRANCARE_SUPPORT && info != null && info.mLastLaunchedActivity != null) {
            ITranActivityMetricsLogger.Instance().startLaunchTrace(info.mLastLaunchedActivity.packageName, info.mLastLaunchedActivity.mActivityComponent.getShortClassName(), info.mTransitionType, info.mTransitionStartTimeNs, false);
            info.mLaunchPackageName = info.mLastLaunchedActivity.packageName;
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
            Slog.i(TAG, "startLaunchTrace " + info);
        }
        if (info.mLaunchingState.mTraceName == null) {
            return;
        }
        info.mLaunchTraceName = "launching: " + info.mLastLaunchedActivity.packageName;
        Trace.asyncTraceBegin(64L, info.mLaunchTraceName, (int) info.mTransitionStartTimeNs);
    }

    private void stopLaunchTrace(TransitionInfo info) {
        if (Build.TRANCARE_SUPPORT && info != null) {
            ITranActivityMetricsLogger.Instance().stopLaunchTrace(info.mLaunchPackageName, info.mTransitionStartTimeNs, info.mWindowsDrawnDelayMs);
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_METRICS) {
            Slog.i(TAG, "stopLaunchTrace " + info);
        }
        if (info.mLaunchTraceName == null) {
            return;
        }
        Trace.asyncTraceEnd(64L, info.mLaunchTraceName, (int) info.mTransitionStartTimeNs);
        info.mLaunchTraceName = null;
    }

    public ActivityMetricsLaunchObserverRegistry getLaunchObserverRegistry() {
        return this.mLaunchObserver;
    }

    private void launchObserverNotifyIntentStarted(Intent intent, long timestampNs) {
        Trace.traceBegin(64L, "MetricsLogger:launchObserverNotifyIntentStarted");
        this.mLaunchObserver.onIntentStarted(intent, timestampNs);
        Trace.traceEnd(64L);
    }

    private void launchObserverNotifyIntentFailed(long id) {
        Trace.traceBegin(64L, "MetricsLogger:launchObserverNotifyIntentFailed");
        this.mLaunchObserver.onIntentFailed(id);
        Trace.traceEnd(64L);
    }

    private void launchObserverNotifyActivityLaunched(TransitionInfo info) {
        Trace.traceBegin(64L, "MetricsLogger:launchObserverNotifyActivityLaunched");
        int temperature = convertTransitionTypeToLaunchObserverTemperature(info.mTransitionType);
        this.mLaunchObserver.onActivityLaunched(info.mTransitionStartTimeNs, info.mLastLaunchedActivity.mActivityComponent, temperature);
        Trace.traceEnd(64L);
    }

    private void launchObserverNotifyReportFullyDrawn(TransitionInfo info, long timestampNs) {
        Trace.traceBegin(64L, "MetricsLogger:launchObserverNotifyReportFullyDrawn");
        this.mLaunchObserver.onReportFullyDrawn(info.mTransitionStartTimeNs, timestampNs);
        Trace.traceEnd(64L);
    }

    private void launchObserverNotifyActivityLaunchCancelled(TransitionInfo info) {
        Trace.traceBegin(64L, "MetricsLogger:launchObserverNotifyActivityLaunchCancelled");
        this.mLaunchObserver.onActivityLaunchCancelled(info.mTransitionStartTimeNs);
        Trace.traceEnd(64L);
    }

    private void launchObserverNotifyActivityLaunchFinished(TransitionInfo info, long timestampNs) {
        Trace.traceBegin(64L, "MetricsLogger:launchObserverNotifyActivityLaunchFinished");
        this.mLaunchObserver.onActivityLaunchFinished(info.mTransitionStartTimeNs, info.mLastLaunchedActivity.mActivityComponent, timestampNs);
        Trace.traceEnd(64L);
    }

    private static int convertTransitionTypeToLaunchObserverTemperature(int transitionType) {
        switch (transitionType) {
            case 7:
                return 1;
            case 8:
                return 2;
            case 9:
                return 3;
            default:
                return -1;
        }
    }

    private void notifyActivityLaunchedByApplaunch(LaunchingState launchingState, WindowProcessController processRecord) {
        if (ITranActivityManagerService.Instance().isAppLaunchEnable()) {
            if (ITranActivityManagerService.Instance().isAppLaunchDebugEnable()) {
                Slog.d(TAG, "AppLaunchTracker notifyActivityLaunched processRecord is null?" + (processRecord == null));
            }
            if (processRecord != null && launchingState != null) {
                launchingState.procType = processRecord.getProcType();
                if (ITranActivityManagerService.Instance().isAppLaunchDebugEnable()) {
                    Slog.d(TAG, "AppLaunchTracker notifyActivityLaunched procType=" + launchingState.procType);
                }
                processRecord.setNormalProcType();
            }
        }
    }
}
