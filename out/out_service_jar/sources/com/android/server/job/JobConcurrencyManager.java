package com.android.server.job;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.UserSwitchObserver;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Pair;
import android.util.Pools;
import android.util.Slog;
import android.util.SparseArrayMap;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.app.IBatteryStats;
import com.android.internal.util.jobs.StatLogger;
import com.android.server.JobSchedulerBackgroundThread;
import com.android.server.LocalServices;
import com.android.server.job.JobConcurrencyManager;
import com.android.server.job.controllers.JobStatus;
import com.android.server.job.controllers.StateController;
import com.android.server.job.restrictions.JobRestriction;
import com.android.server.pm.UserManagerInternal;
import com.android.server.slice.SliceClientPermissions;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class JobConcurrencyManager {
    private static final int ALL_WORK_TYPES = 63;
    static final String CONFIG_KEY_PREFIX_CONCURRENCY = "concurrency_";
    private static final int DEFAULT_PKG_CONCURRENCY_LIMIT_EJ = 3;
    private static final int DEFAULT_PKG_CONCURRENCY_LIMIT_REGULAR = 8;
    private static final long DEFAULT_SCREEN_OFF_ADJUSTMENT_DELAY_MS = 30000;
    static final String KEY_PKG_CONCURRENCY_LIMIT_EJ = "concurrency_pkg_concurrency_limit_ej";
    static final String KEY_PKG_CONCURRENCY_LIMIT_REGULAR = "concurrency_pkg_concurrency_limit_regular";
    private static final String KEY_SCREEN_OFF_ADJUSTMENT_DELAY_MS = "concurrency_screen_off_adjustment_delay_ms";
    private static final int MAX_RETAINED_OBJECTS = 24;
    static final int NUM_WORK_TYPES = 6;
    public static final int STANDARD_CONCURRENCY_LIMIT = 16;
    private static final int SYSTEM_STATE_REFRESH_MIN_INTERVAL = 1000;
    private static final String TAG = "JobScheduler.Concurrency";
    static final int WORK_TYPE_BG = 8;
    static final int WORK_TYPE_BGUSER = 32;
    static final int WORK_TYPE_BGUSER_IMPORTANT = 16;
    static final int WORK_TYPE_EJ = 4;
    static final int WORK_TYPE_FGS = 2;
    static final int WORK_TYPE_NONE = 0;
    static final int WORK_TYPE_TOP = 1;
    private final Context mContext;
    private boolean mCurrentInteractiveState;
    private boolean mEffectiveInteractiveState;
    GracePeriodObserver mGracePeriodObserver;
    private final Handler mHandler;
    private int mLastMemoryTrimLevel;
    private long mLastScreenOffRealtime;
    private long mLastScreenOnRealtime;
    private final Object mLock;
    private long mNextSystemStateRefreshTime;
    private PowerManager mPowerManager;
    private final JobSchedulerService mService;
    boolean mShouldRestrictBgUser;
    private static final boolean DEBUG = JobSchedulerService.DEBUG;
    private static final WorkConfigLimitsPerMemoryTrimLevel CONFIG_LIMITS_SCREEN_ON = new WorkConfigLimitsPerMemoryTrimLevel(new WorkTypeConfig("screen_on_normal", 11, List.of(Pair.create(1, 4), Pair.create(2, 1), Pair.create(4, 3), Pair.create(8, 2), Pair.create(16, 1)), List.of(Pair.create(8, 6), Pair.create(16, 2), Pair.create(32, 3))), new WorkTypeConfig("screen_on_moderate", 9, List.of(Pair.create(1, 4), Pair.create(2, 1), Pair.create(4, 2), Pair.create(8, 1), Pair.create(16, 1)), List.of(Pair.create(8, 4), Pair.create(16, 1), Pair.create(32, 1))), new WorkTypeConfig("screen_on_low", 6, List.of(Pair.create(1, 4), Pair.create(2, 1), Pair.create(4, 1)), List.of(Pair.create(8, 2), Pair.create(16, 1), Pair.create(32, 1))), new WorkTypeConfig("screen_on_critical", 6, List.of(Pair.create(1, 4), Pair.create(2, 1), Pair.create(4, 1)), List.of(Pair.create(8, 1), Pair.create(16, 1), Pair.create(32, 1))));
    private static final WorkConfigLimitsPerMemoryTrimLevel CONFIG_LIMITS_SCREEN_OFF = new WorkConfigLimitsPerMemoryTrimLevel(new WorkTypeConfig("screen_off_normal", 16, List.of(Pair.create(1, 4), Pair.create(2, 2), Pair.create(4, 3), Pair.create(8, 2), Pair.create(16, 1)), List.of(Pair.create(8, 10), Pair.create(16, 2), Pair.create(32, 3))), new WorkTypeConfig("screen_off_moderate", 14, List.of(Pair.create(1, 4), Pair.create(2, 2), Pair.create(4, 3), Pair.create(8, 2), Pair.create(16, 1)), List.of(Pair.create(8, 7), Pair.create(16, 1), Pair.create(32, 1))), new WorkTypeConfig("screen_off_low", 9, List.of(Pair.create(1, 4), Pair.create(2, 1), Pair.create(4, 2), Pair.create(8, 1)), List.of(Pair.create(8, 3), Pair.create(16, 1), Pair.create(32, 1))), new WorkTypeConfig("screen_off_critical", 6, List.of(Pair.create(1, 4), Pair.create(2, 1), Pair.create(4, 1)), List.of(Pair.create(8, 1), Pair.create(16, 1), Pair.create(32, 1))));
    private static final Comparator<ContextAssignment> sDeterminationComparator = new Comparator() { // from class: com.android.server.job.JobConcurrencyManager$$ExternalSyntheticLambda0
        @Override // java.util.Comparator
        public final int compare(Object obj, Object obj2) {
            return JobConcurrencyManager.lambda$static$0((JobConcurrencyManager.ContextAssignment) obj, (JobConcurrencyManager.ContextAssignment) obj2);
        }
    };
    private final ArraySet<ContextAssignment> mRecycledChanged = new ArraySet<>();
    private final ArraySet<ContextAssignment> mRecycledIdle = new ArraySet<>();
    private final ArrayList<ContextAssignment> mRecycledPreferredUidOnly = new ArrayList<>();
    private final ArrayList<ContextAssignment> mRecycledStoppable = new ArrayList<>();
    private final Pools.Pool<ContextAssignment> mContextAssignmentPool = new Pools.SimplePool(24);
    final List<JobServiceContext> mActiveServices = new ArrayList();
    private final ArraySet<JobServiceContext> mIdleContexts = new ArraySet<>();
    private int mNumDroppedContexts = 0;
    private final ArraySet<JobStatus> mRunningJobs = new ArraySet<>();
    private final WorkCountTracker mWorkCountTracker = new WorkCountTracker();
    private final Pools.Pool<PackageStats> mPkgStatsPool = new Pools.SimplePool(24);
    private final SparseArrayMap<String, PackageStats> mActivePkgStats = new SparseArrayMap<>();
    private WorkTypeConfig mWorkTypeConfig = CONFIG_LIMITS_SCREEN_OFF.normal;
    private long mScreenOffAdjustmentDelayMs = 30000;
    private int mPkgConcurrencyLimitEj = 3;
    private int mPkgConcurrencyLimitRegular = 8;
    private final Consumer<PackageStats> mPackageStatsStagingCountClearer = new Consumer() { // from class: com.android.server.job.JobConcurrencyManager$$ExternalSyntheticLambda1
        @Override // java.util.function.Consumer
        public final void accept(Object obj) {
            ((JobConcurrencyManager.PackageStats) obj).resetStagedCount();
        }
    };
    private final StatLogger mStatLogger = new StatLogger(new String[]{"assignJobsToContexts", "refreshSystemState"});
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.server.job.JobConcurrencyManager.1
        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            char c;
            String action = intent.getAction();
            switch (action.hashCode()) {
                case -2128145023:
                    if (action.equals("android.intent.action.SCREEN_OFF")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case -1454123155:
                    if (action.equals("android.intent.action.SCREEN_ON")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 870701415:
                    if (action.equals("android.os.action.DEVICE_IDLE_MODE_CHANGED")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 1779291251:
                    if (action.equals("android.os.action.POWER_SAVE_MODE_CHANGED")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    JobConcurrencyManager.this.onInteractiveStateChanged(true);
                    return;
                case 1:
                    JobConcurrencyManager.this.onInteractiveStateChanged(false);
                    return;
                case 2:
                    if (JobConcurrencyManager.this.mPowerManager != null && JobConcurrencyManager.this.mPowerManager.isDeviceIdleMode()) {
                        synchronized (JobConcurrencyManager.this.mLock) {
                            JobConcurrencyManager.this.stopUnexemptedJobsForDoze();
                            JobConcurrencyManager.this.stopLongRunningJobsLocked("deep doze");
                        }
                        return;
                    }
                    return;
                case 3:
                    if (JobConcurrencyManager.this.mPowerManager != null && JobConcurrencyManager.this.mPowerManager.isPowerSaveMode()) {
                        synchronized (JobConcurrencyManager.this.mLock) {
                            JobConcurrencyManager.this.stopLongRunningJobsLocked("battery saver");
                        }
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    };
    private final Runnable mRampUpForScreenOff = new Runnable() { // from class: com.android.server.job.JobConcurrencyManager$$ExternalSyntheticLambda2
        @Override // java.lang.Runnable
        public final void run() {
            JobConcurrencyManager.this.rampUpForScreenOff();
        }
    };

    /* loaded from: classes.dex */
    interface Stats {
        public static final int ASSIGN_JOBS_TO_CONTEXTS = 0;
        public static final int COUNT = 2;
        public static final int REFRESH_SYSTEM_STATE = 1;
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface WorkType {
    }

    static String workTypeToString(int workType) {
        switch (workType) {
            case 0:
                return "NONE";
            case 1:
                return "TOP";
            case 2:
                return "FGS";
            case 4:
                return "EJ";
            case 8:
                return "BG";
            case 16:
                return "BGUSER_IMPORTANT";
            case 32:
                return "BGUSER";
            default:
                return "WORK(" + workType + ")";
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$static$0(ContextAssignment ca1, ContextAssignment ca2) {
        if (ca1 == ca2) {
            return 0;
        }
        JobStatus js1 = ca1.context.getRunningJobLocked();
        JobStatus js2 = ca2.context.getRunningJobLocked();
        if (js1 == null) {
            if (js2 == null) {
                return 0;
            }
            return 1;
        } else if (js2 == null) {
            return -1;
        } else {
            if (js1.lastEvaluatedBias == 40) {
                if (js2.lastEvaluatedBias != 40) {
                    return -1;
                }
            } else if (js2.lastEvaluatedBias == 40) {
                return 1;
            }
            return Long.compare(ca2.context.getExecutionStartTimeElapsed(), ca1.context.getExecutionStartTimeElapsed());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public JobConcurrencyManager(JobSchedulerService service) {
        this.mService = service;
        this.mLock = service.mLock;
        Context testableContext = service.getTestableContext();
        this.mContext = testableContext;
        this.mHandler = JobSchedulerBackgroundThread.getHandler();
        this.mGracePeriodObserver = new GracePeriodObserver(testableContext);
        this.mShouldRestrictBgUser = testableContext.getResources().getBoolean(17891687);
    }

    public void onSystemReady() {
        this.mPowerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
        IntentFilter filter = new IntentFilter("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
        filter.addAction("android.os.action.POWER_SAVE_MODE_CHANGED");
        this.mContext.registerReceiver(this.mReceiver, filter);
        try {
            ActivityManager.getService().registerUserSwitchObserver(this.mGracePeriodObserver, TAG);
        } catch (RemoteException e) {
        }
        onInteractiveStateChanged(this.mPowerManager.isInteractive());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onThirdPartyAppsCanStart() {
        IBatteryStats batteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
        for (int i = 0; i < 16; i++) {
            ArraySet<JobServiceContext> arraySet = this.mIdleContexts;
            JobSchedulerService jobSchedulerService = this.mService;
            arraySet.add(new JobServiceContext(jobSchedulerService, this, batteryStats, jobSchedulerService.mJobPackageTracker, this.mContext.getMainLooper()));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAppRemovedLocked(String pkgName, int uid) {
        PackageStats packageStats = (PackageStats) this.mActivePkgStats.get(UserHandle.getUserId(uid), pkgName);
        if (packageStats != null) {
            if (packageStats.numRunningEj > 0 || packageStats.numRunningRegular > 0) {
                Slog.w(TAG, pkgName + "(" + uid + ") marked as removed before jobs stopped running");
            } else {
                this.mActivePkgStats.delete(UserHandle.getUserId(uid), pkgName);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUserRemoved(int userId) {
        this.mGracePeriodObserver.onUserRemoved(userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onInteractiveStateChanged(boolean interactive) {
        synchronized (this.mLock) {
            if (this.mCurrentInteractiveState == interactive) {
                return;
            }
            this.mCurrentInteractiveState = interactive;
            if (DEBUG) {
                Slog.d(TAG, "Interactive: " + interactive);
            }
            long nowRealtime = JobSchedulerService.sElapsedRealtimeClock.millis();
            if (interactive) {
                this.mLastScreenOnRealtime = nowRealtime;
                this.mEffectiveInteractiveState = true;
                this.mHandler.removeCallbacks(this.mRampUpForScreenOff);
            } else {
                this.mLastScreenOffRealtime = nowRealtime;
                this.mHandler.postDelayed(this.mRampUpForScreenOff, this.mScreenOffAdjustmentDelayMs);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void rampUpForScreenOff() {
        synchronized (this.mLock) {
            if (this.mEffectiveInteractiveState) {
                if (this.mLastScreenOnRealtime > this.mLastScreenOffRealtime) {
                    return;
                }
                long now = JobSchedulerService.sElapsedRealtimeClock.millis();
                if (this.mLastScreenOffRealtime + this.mScreenOffAdjustmentDelayMs > now) {
                    return;
                }
                this.mEffectiveInteractiveState = false;
                if (DEBUG) {
                    Slog.d(TAG, "Ramping up concurrency");
                }
                this.mService.maybeRunPendingJobsLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArraySet<JobStatus> getRunningJobsLocked() {
        return this.mRunningJobs;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isJobRunningLocked(JobStatus job) {
        return this.mRunningJobs.contains(job);
    }

    private boolean isSimilarJobRunningLocked(JobStatus job) {
        for (int i = this.mRunningJobs.size() - 1; i >= 0; i--) {
            JobStatus js = this.mRunningJobs.valueAt(i);
            if (job.getUid() == js.getUid() && job.getJobId() == js.getJobId()) {
                return true;
            }
        }
        return false;
    }

    private boolean refreshSystemStateLocked() {
        long nowUptime = JobSchedulerService.sUptimeMillisClock.millis();
        if (nowUptime < this.mNextSystemStateRefreshTime) {
            return false;
        }
        long start = this.mStatLogger.getTime();
        this.mNextSystemStateRefreshTime = 1000 + nowUptime;
        this.mLastMemoryTrimLevel = 0;
        try {
            this.mLastMemoryTrimLevel = ActivityManager.getService().getMemoryTrimLevel();
        } catch (RemoteException e) {
        }
        this.mStatLogger.logDurationStat(1, start);
        return true;
    }

    private void updateCounterConfigLocked() {
        if (!refreshSystemStateLocked()) {
            return;
        }
        WorkConfigLimitsPerMemoryTrimLevel workConfigs = this.mEffectiveInteractiveState ? CONFIG_LIMITS_SCREEN_ON : CONFIG_LIMITS_SCREEN_OFF;
        switch (this.mLastMemoryTrimLevel) {
            case 1:
                this.mWorkTypeConfig = workConfigs.moderate;
                break;
            case 2:
                this.mWorkTypeConfig = workConfigs.low;
                break;
            case 3:
                this.mWorkTypeConfig = workConfigs.critical;
                break;
            default:
                this.mWorkTypeConfig = workConfigs.normal;
                break;
        }
        this.mWorkCountTracker.setConfig(this.mWorkTypeConfig);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void assignJobsToContextsLocked() {
        long start = this.mStatLogger.getTime();
        assignJobsToContextsInternalLocked();
        this.mStatLogger.logDurationStat(0, start);
    }

    private void assignJobsToContextsInternalLocked() {
        int i;
        String str;
        PendingJobQueue pendingJobQueue;
        List<JobServiceContext> activeServices;
        String str2;
        int numRunningJobs;
        ContextAssignment selectedContext;
        ContextAssignment selectedContext2;
        Object obj;
        JobServiceContext createNewJobServiceContext;
        int jobBias;
        int topEjCount;
        ContextAssignment selectedContext3;
        int replaceWorkType;
        boolean topEjCount2;
        JobServiceContext jsc;
        boolean z = DEBUG;
        String str3 = TAG;
        if (z) {
            Slog.d(TAG, printPendingQueueLocked());
        }
        if (this.mService.getPendingJobQueue().size() == 0) {
            return;
        }
        PendingJobQueue pendingJobQueue2 = this.mService.getPendingJobQueue();
        List<JobServiceContext> activeServices2 = this.mActiveServices;
        ArraySet<ContextAssignment> changed = this.mRecycledChanged;
        ArraySet<ContextAssignment> idle = this.mRecycledIdle;
        ArrayList<ContextAssignment> preferredUidOnly = this.mRecycledPreferredUidOnly;
        ArrayList<ContextAssignment> stoppable = this.mRecycledStoppable;
        updateCounterConfigLocked();
        this.mWorkCountTracker.resetCounts();
        boolean z2 = true;
        updateNonRunningPrioritiesLocked(pendingJobQueue2, true);
        int numRunningJobs2 = activeServices2.size();
        for (int i2 = 0; i2 < numRunningJobs2; i2++) {
            JobServiceContext jsc2 = activeServices2.get(i2);
            JobStatus js = jsc2.getRunningJobLocked();
            ContextAssignment assignment = (ContextAssignment) this.mContextAssignmentPool.acquire();
            if (assignment == null) {
                assignment = new ContextAssignment();
            }
            assignment.context = jsc2;
            if (js != null) {
                this.mWorkCountTracker.incrementRunningJobCount(jsc2.getRunningJobWorkType());
                assignment.workType = jsc2.getRunningJobWorkType();
            }
            assignment.preferredUid = jsc2.getPreferredUid();
            String shouldStopRunningJobLocked = shouldStopRunningJobLocked(jsc2);
            assignment.shouldStopJobReason = shouldStopRunningJobLocked;
            if (shouldStopRunningJobLocked != null) {
                stoppable.add(assignment);
            } else {
                preferredUidOnly.add(assignment);
            }
        }
        Comparator<ContextAssignment> comparator = sDeterminationComparator;
        preferredUidOnly.sort(comparator);
        stoppable.sort(comparator);
        int i3 = numRunningJobs2;
        while (true) {
            i = 16;
            if (i3 >= 16) {
                break;
            }
            int numIdleContexts = this.mIdleContexts.size();
            if (numIdleContexts > 0) {
                jsc = this.mIdleContexts.removeAt(numIdleContexts - 1);
            } else {
                Slog.wtf(TAG, "Had fewer than 16 in existence");
                jsc = createNewJobServiceContext();
            }
            ContextAssignment assignment2 = (ContextAssignment) this.mContextAssignmentPool.acquire();
            if (assignment2 == null) {
                assignment2 = new ContextAssignment();
            }
            assignment2.context = jsc;
            idle.add(assignment2);
            i3++;
        }
        if (DEBUG) {
            Slog.d(TAG, printAssignments("running jobs initial", stoppable, preferredUidOnly));
        }
        this.mWorkCountTracker.onCountDone();
        pendingJobQueue2.resetIterator();
        int projectedRunningCount = numRunningJobs2;
        while (true) {
            JobStatus nextPending = pendingJobQueue2.next();
            if (nextPending == null) {
                break;
            } else if (this.mRunningJobs.contains(nextPending)) {
                Slog.wtf(str3, "Pending queue contained a running job");
                if (DEBUG) {
                    Slog.e(str3, "Pending+running job: " + nextPending);
                }
                pendingJobQueue2.remove(nextPending);
            } else {
                boolean isTopEj = (nextPending.shouldTreatAsExpeditedJob() && nextPending.lastEvaluatedBias == 40) ? z2 : false;
                if (DEBUG && isSimilarJobRunningLocked(nextPending)) {
                    Slog.w(str3, "Already running similar " + (isTopEj ? "TOP-EJ" : "job") + " to: " + nextPending);
                }
                ContextAssignment selectedContext4 = null;
                int allWorkTypes = getJobWorkTypes(nextPending);
                boolean pkgConcurrencyOkay = !isPkgConcurrencyLimitedLocked(nextPending);
                boolean isInOverage = projectedRunningCount > i ? z2 : false;
                boolean startingJob = false;
                if (idle.size() <= 0) {
                    pendingJobQueue = pendingJobQueue2;
                    activeServices = activeServices2;
                } else {
                    int idx = idle.size() - 1;
                    ContextAssignment assignment3 = idle.valueAt(idx);
                    pendingJobQueue = pendingJobQueue2;
                    activeServices = activeServices2;
                    boolean preferredUidOkay = assignment3.preferredUid == nextPending.getUid() || assignment3.preferredUid == -1;
                    int workType = this.mWorkCountTracker.canJobStart(allWorkTypes);
                    if (preferredUidOkay && pkgConcurrencyOkay && workType != 0) {
                        selectedContext4 = assignment3;
                        startingJob = true;
                        idle.removeAt(idx);
                        assignment3.newJob = nextPending;
                        assignment3.newWorkType = workType;
                    }
                }
                if (selectedContext4 != null || stoppable.size() <= 0) {
                    str2 = str3;
                    numRunningJobs = numRunningJobs2;
                    selectedContext = selectedContext4;
                } else {
                    int topEjCount3 = 0;
                    for (int r = this.mRunningJobs.size() - 1; r >= 0; r--) {
                        JobStatus js2 = this.mRunningJobs.valueAt(r);
                        if (js2.startedAsExpeditedJob && js2.lastEvaluatedBias == 40) {
                            topEjCount3++;
                        }
                    }
                    int r2 = stoppable.size();
                    int s = r2 - 1;
                    while (s >= 0) {
                        ContextAssignment assignment4 = stoppable.get(s);
                        JobStatus runningJob = assignment4.context.getRunningJobLocked();
                        boolean canReplace = isTopEj;
                        if (canReplace || isInOverage) {
                            topEjCount = topEjCount3;
                            str2 = str3;
                            numRunningJobs = numRunningJobs2;
                            selectedContext3 = selectedContext4;
                        } else {
                            numRunningJobs = numRunningJobs2;
                            int currentJobBias = this.mService.evaluateJobBiasLocked(runningJob);
                            selectedContext3 = selectedContext4;
                            if (runningJob.lastEvaluatedBias < 40 || currentJobBias < 40) {
                                topEjCount = topEjCount3;
                                str2 = str3;
                            } else {
                                topEjCount = topEjCount3;
                                str2 = str3;
                                if (topEjCount3 <= this.mWorkTypeConfig.getMaxTotal() * 0.5d) {
                                    topEjCount2 = false;
                                    canReplace = topEjCount2;
                                }
                            }
                            topEjCount2 = true;
                            canReplace = topEjCount2;
                        }
                        if (canReplace && (replaceWorkType = this.mWorkCountTracker.canJobStart(allWorkTypes, assignment4.context.getRunningJobWorkType())) != 0) {
                            assignment4.preemptReason = assignment4.shouldStopJobReason;
                            assignment4.preemptReasonCode = 4;
                            selectedContext2 = assignment4;
                            stoppable.remove(s);
                            assignment4.newJob = nextPending;
                            assignment4.newWorkType = replaceWorkType;
                            break;
                        }
                        s--;
                        numRunningJobs2 = numRunningJobs;
                        selectedContext4 = selectedContext3;
                        topEjCount3 = topEjCount;
                        str3 = str2;
                    }
                    str2 = str3;
                    numRunningJobs = numRunningJobs2;
                    selectedContext = selectedContext4;
                }
                selectedContext2 = selectedContext;
                if (selectedContext2 == null && (!isInOverage || isTopEj)) {
                    int lowestBiasSeen = Integer.MAX_VALUE;
                    for (int p = preferredUidOnly.size() - 1; p >= 0; p--) {
                        ContextAssignment assignment5 = preferredUidOnly.get(p);
                        JobStatus runningJob2 = assignment5.context.getRunningJobLocked();
                        if (runningJob2.getUid() == nextPending.getUid() && (jobBias = this.mService.evaluateJobBiasLocked(runningJob2)) < nextPending.lastEvaluatedBias && (selectedContext2 == null || lowestBiasSeen > jobBias)) {
                            lowestBiasSeen = jobBias;
                            selectedContext2 = assignment5;
                            assignment5.preemptReason = "higher bias job found";
                            assignment5.preemptReasonCode = 2;
                        }
                    }
                    if (selectedContext2 != null) {
                        selectedContext2.newJob = nextPending;
                        preferredUidOnly.remove(selectedContext2);
                    }
                }
                if (!isTopEj) {
                    obj = null;
                } else {
                    if (selectedContext2 != null && selectedContext2.context.getRunningJobLocked() != null) {
                        changed.add(selectedContext2);
                        projectedRunningCount--;
                        selectedContext2.newJob = null;
                        selectedContext2.newWorkType = 0;
                        selectedContext2 = null;
                    }
                    if (selectedContext2 != null) {
                        obj = null;
                    } else {
                        ContextAssignment selectedContext5 = (ContextAssignment) this.mContextAssignmentPool.acquire();
                        if (selectedContext5 != null) {
                            obj = null;
                            selectedContext2 = selectedContext5;
                        } else {
                            obj = null;
                            selectedContext2 = new ContextAssignment();
                        }
                        if (this.mIdleContexts.size() > 0) {
                            ArraySet<JobServiceContext> arraySet = this.mIdleContexts;
                            createNewJobServiceContext = arraySet.removeAt(arraySet.size() - 1);
                        } else {
                            createNewJobServiceContext = createNewJobServiceContext();
                        }
                        selectedContext2.context = createNewJobServiceContext;
                        selectedContext2.newJob = nextPending;
                        int workType2 = this.mWorkCountTracker.canJobStart(allWorkTypes);
                        selectedContext2.newWorkType = workType2 != 0 ? workType2 : 1;
                    }
                }
                PackageStats packageStats = getPkgStatsLocked(nextPending.getSourceUserId(), nextPending.getSourcePackageName());
                if (selectedContext2 != null) {
                    changed.add(selectedContext2);
                    if (selectedContext2.context.getRunningJobLocked() != null) {
                        projectedRunningCount--;
                    }
                    if (selectedContext2.newJob != null) {
                        projectedRunningCount++;
                    }
                    packageStats.adjustStagedCount(true, nextPending.shouldTreatAsExpeditedJob());
                }
                if (startingJob) {
                    this.mWorkCountTracker.stageJob(selectedContext2.newWorkType, allWorkTypes);
                    this.mActivePkgStats.add(nextPending.getSourceUserId(), nextPending.getSourcePackageName(), packageStats);
                }
                pendingJobQueue2 = pendingJobQueue;
                activeServices2 = activeServices;
                numRunningJobs2 = numRunningJobs;
                str3 = str2;
                z2 = true;
                i = 16;
            }
        }
        String str4 = str3;
        if (DEBUG) {
            str = str4;
            Slog.d(str, printAssignments("running jobs final", stoppable, preferredUidOnly, changed));
            Slog.d(str, "assignJobsToContexts: " + this.mWorkCountTracker.toString());
        } else {
            str = str4;
        }
        for (int c = changed.size() - 1; c >= 0; c--) {
            ContextAssignment assignment6 = changed.valueAt(c);
            JobStatus js3 = assignment6.context.getRunningJobLocked();
            if (js3 != null) {
                if (DEBUG) {
                    Slog.d(str, "preempting job: " + js3);
                }
                assignment6.context.cancelExecutingJobLocked(assignment6.preemptReasonCode, 2, assignment6.preemptReason);
            } else {
                JobStatus pendingJob = assignment6.newJob;
                if (DEBUG) {
                    Slog.d(str, "About to run job on context " + assignment6.context.getId() + ", job: " + pendingJob);
                }
                startJobLocked(assignment6.context, pendingJob, assignment6.newWorkType);
            }
            assignment6.clear();
            this.mContextAssignmentPool.release(assignment6);
        }
        int c2 = stoppable.size();
        for (int s2 = c2 - 1; s2 >= 0; s2--) {
            ContextAssignment assignment7 = stoppable.get(s2);
            assignment7.clear();
            this.mContextAssignmentPool.release(assignment7);
        }
        int s3 = preferredUidOnly.size();
        for (int p2 = s3 - 1; p2 >= 0; p2--) {
            ContextAssignment assignment8 = preferredUidOnly.get(p2);
            assignment8.clear();
            this.mContextAssignmentPool.release(assignment8);
        }
        int p3 = idle.size();
        for (int i4 = p3 - 1; i4 >= 0; i4--) {
            ContextAssignment assignment9 = idle.valueAt(i4);
            this.mIdleContexts.add(assignment9.context);
            assignment9.clear();
            this.mContextAssignmentPool.release(assignment9);
        }
        changed.clear();
        idle.clear();
        stoppable.clear();
        preferredUidOnly.clear();
        this.mWorkCountTracker.resetStagingCount();
        this.mActivePkgStats.forEach(this.mPackageStatsStagingCountClearer);
        noteConcurrency();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUidBiasChangedLocked(int prevBias, int newBias) {
        if ((prevBias != 40 && newBias != 40) || this.mService.getPendingJobQueue().size() == 0) {
            return;
        }
        assignJobsToContextsLocked();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean stopJobOnServiceContextLocked(JobStatus job, int reason, int internalReasonCode, String debugReason) {
        if (this.mRunningJobs.contains(job)) {
            for (int i = 0; i < this.mActiveServices.size(); i++) {
                JobServiceContext jsc = this.mActiveServices.get(i);
                JobStatus executing = jsc.getRunningJobLocked();
                if (executing == job) {
                    jsc.cancelExecutingJobLocked(reason, internalReasonCode, debugReason);
                    return true;
                }
            }
            Slog.wtf(TAG, "Couldn't find running job on a context");
            this.mRunningJobs.remove(job);
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopUnexemptedJobsForDoze() {
        for (int i = 0; i < this.mActiveServices.size(); i++) {
            JobServiceContext jsc = this.mActiveServices.get(i);
            JobStatus executing = jsc.getRunningJobLocked();
            if (executing != null && !executing.canRunInDoze()) {
                jsc.cancelExecutingJobLocked(4, 4, "cancelled due to doze");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopLongRunningJobsLocked(String debugReason) {
        for (int i = 0; i < this.mActiveServices.size(); i++) {
            JobServiceContext jsc = this.mActiveServices.get(i);
            JobStatus jobStatus = jsc.getRunningJobLocked();
            if (jobStatus != null && !jsc.isWithinExecutionGuaranteeTime()) {
                jsc.cancelExecutingJobLocked(4, 3, debugReason);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopNonReadyActiveJobsLocked() {
        for (int i = 0; i < this.mActiveServices.size(); i++) {
            JobServiceContext serviceContext = this.mActiveServices.get(i);
            JobStatus running = serviceContext.getRunningJobLocked();
            if (running != null) {
                if (!running.isReady()) {
                    if (running.getEffectiveStandbyBucket() == 5 && running.getStopReason() == 12) {
                        serviceContext.cancelExecutingJobLocked(running.getStopReason(), 6, "cancelled due to restricted bucket");
                    } else {
                        serviceContext.cancelExecutingJobLocked(running.getStopReason(), 1, "cancelled due to unsatisfied constraints");
                    }
                } else {
                    JobRestriction restriction = this.mService.checkIfRestricted(running);
                    if (restriction != null) {
                        int internalReasonCode = restriction.getInternalReason();
                        serviceContext.cancelExecutingJobLocked(restriction.getReason(), internalReasonCode, "restricted due to " + JobParameters.getInternalReasonCodeDescription(internalReasonCode));
                    }
                }
            }
        }
    }

    private void noteConcurrency() {
        this.mService.mJobPackageTracker.noteConcurrency(this.mRunningJobs.size(), this.mWorkCountTracker.getRunningJobCount(1));
    }

    private void updateNonRunningPrioritiesLocked(PendingJobQueue jobQueue, boolean updateCounter) {
        jobQueue.resetIterator();
        while (true) {
            JobStatus pending = jobQueue.next();
            if (pending != null) {
                if (!this.mRunningJobs.contains(pending)) {
                    pending.lastEvaluatedBias = this.mService.evaluateJobBiasLocked(pending);
                    if (updateCounter) {
                        this.mWorkCountTracker.incrementPendingJobCount(getJobWorkTypes(pending));
                    }
                }
            } else {
                return;
            }
        }
    }

    private PackageStats getPkgStatsLocked(int userId, String packageName) {
        PackageStats packageStats = (PackageStats) this.mActivePkgStats.get(userId, packageName);
        if (packageStats == null) {
            packageStats = (PackageStats) this.mPkgStatsPool.acquire();
            if (packageStats == null) {
                packageStats = new PackageStats();
            }
            packageStats.setPackage(userId, packageName);
        }
        return packageStats;
    }

    boolean isPkgConcurrencyLimitedLocked(JobStatus jobStatus) {
        PackageStats packageStats;
        if (jobStatus.lastEvaluatedBias < 40 && this.mService.getPendingJobQueue().size() + this.mRunningJobs.size() >= this.mWorkTypeConfig.getMaxTotal() && (packageStats = (PackageStats) this.mActivePkgStats.get(jobStatus.getSourceUserId(), jobStatus.getSourcePackageName())) != null) {
            return jobStatus.shouldTreatAsExpeditedJob() ? packageStats.numRunningEj + packageStats.numStagedEj >= this.mPkgConcurrencyLimitEj : packageStats.numRunningRegular + packageStats.numStagedRegular >= this.mPkgConcurrencyLimitRegular;
        }
        return false;
    }

    private void startJobLocked(JobServiceContext worker, JobStatus jobStatus, int workType) {
        List<StateController> controllers = this.mService.mControllers;
        int numControllers = controllers.size();
        PowerManager.WakeLock wl = this.mPowerManager.newWakeLock(1, jobStatus.getTag());
        wl.setWorkSource(this.mService.deriveWorkSource(jobStatus.getSourceUid(), jobStatus.getSourcePackageName()));
        wl.setReferenceCounted(false);
        wl.acquire();
        for (int ic = 0; ic < numControllers; ic++) {
            try {
                controllers.get(ic).prepareForExecutionLocked(jobStatus);
            } finally {
                wl.release();
            }
        }
        PackageStats packageStats = getPkgStatsLocked(jobStatus.getSourceUserId(), jobStatus.getSourcePackageName());
        packageStats.adjustStagedCount(false, jobStatus.shouldTreatAsExpeditedJob());
        if (!worker.executeRunnableJob(jobStatus, workType)) {
            Slog.e(TAG, "Error executing " + jobStatus);
            this.mWorkCountTracker.onStagedJobFailed(workType);
            for (int ic2 = 0; ic2 < numControllers; ic2++) {
                controllers.get(ic2).unprepareFromExecutionLocked(jobStatus);
            }
        } else {
            this.mRunningJobs.add(jobStatus);
            this.mActiveServices.add(worker);
            this.mIdleContexts.remove(worker);
            this.mWorkCountTracker.onJobStarted(workType);
            packageStats.adjustRunningCount(true, jobStatus.shouldTreatAsExpeditedJob());
            this.mActivePkgStats.add(jobStatus.getSourceUserId(), jobStatus.getSourcePackageName(), packageStats);
        }
        if (this.mService.getPendingJobQueue().remove(jobStatus)) {
            this.mService.mJobPackageTracker.noteNonpending(jobStatus);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onJobCompletedLocked(JobServiceContext worker, JobStatus jobStatus, int workType) {
        String str;
        int allWorkTypes;
        int workAsType;
        String str2;
        this.mWorkCountTracker.onJobFinished(workType);
        this.mRunningJobs.remove(jobStatus);
        this.mActiveServices.remove(worker);
        if (this.mIdleContexts.size() >= 24) {
            this.mNumDroppedContexts++;
        } else {
            this.mIdleContexts.add(worker);
        }
        PackageStats packageStats = (PackageStats) this.mActivePkgStats.get(jobStatus.getSourceUserId(), jobStatus.getSourcePackageName());
        if (packageStats != null) {
            packageStats.adjustRunningCount(false, jobStatus.startedAsExpeditedJob);
            if (packageStats.numRunningEj <= 0 && packageStats.numRunningRegular <= 0) {
                this.mActivePkgStats.delete(packageStats.userId, packageStats.packageName);
                this.mPkgStatsPool.release(packageStats);
            }
        } else {
            Slog.wtf(TAG, "Running job didn't have an active PackageStats object");
        }
        PendingJobQueue pendingJobQueue = this.mService.getPendingJobQueue();
        if (this.mActiveServices.size() < 16 && pendingJobQueue.size() != 0) {
            String str3 = "Already running similar job to: ";
            if (worker.getPreferredUid() != -1) {
                updateCounterConfigLocked();
                updateNonRunningPrioritiesLocked(pendingJobQueue, false);
                JobStatus highestBiasJob = null;
                int highBiasWorkType = workType;
                int highBiasAllWorkTypes = workType;
                JobStatus backupJob = null;
                int backupWorkType = 0;
                int backupAllWorkTypes = 0;
                pendingJobQueue.resetIterator();
                while (true) {
                    JobStatus nextPending = pendingJobQueue.next();
                    if (nextPending == null) {
                        break;
                    }
                    PackageStats packageStats2 = packageStats;
                    if (this.mRunningJobs.contains(nextPending)) {
                        Slog.wtf(TAG, "Pending queue contained a running job");
                        if (DEBUG) {
                            Slog.e(TAG, "Pending+running job: " + nextPending);
                        }
                        pendingJobQueue.remove(nextPending);
                        str2 = str3;
                    } else {
                        if (DEBUG && isSimilarJobRunningLocked(nextPending)) {
                            Slog.w(TAG, str3 + nextPending);
                        }
                        str2 = str3;
                        if (worker.getPreferredUid() != nextPending.getUid()) {
                            if (backupJob == null && !isPkgConcurrencyLimitedLocked(nextPending)) {
                                int allWorkTypes2 = getJobWorkTypes(nextPending);
                                int workAsType2 = this.mWorkCountTracker.canJobStart(allWorkTypes2);
                                if (workAsType2 != 0) {
                                    backupJob = nextPending;
                                    backupWorkType = workAsType2;
                                    backupAllWorkTypes = allWorkTypes2;
                                }
                                packageStats = packageStats2;
                                str3 = str2;
                            }
                        } else if ((nextPending.lastEvaluatedBias > jobStatus.lastEvaluatedBias || !isPkgConcurrencyLimitedLocked(nextPending)) && (highestBiasJob == null || highestBiasJob.lastEvaluatedBias < nextPending.lastEvaluatedBias)) {
                            highestBiasJob = nextPending;
                            highBiasAllWorkTypes = getJobWorkTypes(nextPending);
                            int workAsType3 = this.mWorkCountTracker.canJobStart(highBiasAllWorkTypes);
                            if (workAsType3 == 0) {
                                highBiasWorkType = workType;
                            } else {
                                highBiasWorkType = workAsType3;
                            }
                            packageStats = packageStats2;
                            str3 = str2;
                        }
                    }
                    packageStats = packageStats2;
                    str3 = str2;
                }
                if (highestBiasJob != null) {
                    if (DEBUG) {
                        Slog.d(TAG, "Running job " + highestBiasJob + " as preemption");
                    }
                    this.mWorkCountTracker.stageJob(highBiasWorkType, highBiasAllWorkTypes);
                    startJobLocked(worker, highestBiasJob, highBiasWorkType);
                } else {
                    boolean z = DEBUG;
                    if (z) {
                        Slog.d(TAG, "Couldn't find preemption job for uid " + worker.getPreferredUid());
                    }
                    worker.clearPreferredUid();
                    if (backupJob != null) {
                        if (z) {
                            Slog.d(TAG, "Running job " + backupJob + " instead");
                        }
                        this.mWorkCountTracker.stageJob(backupWorkType, backupAllWorkTypes);
                        startJobLocked(worker, backupJob, backupWorkType);
                    }
                }
            } else {
                String str4 = "Already running similar job to: ";
                if (pendingJobQueue.size() > 0) {
                    updateCounterConfigLocked();
                    updateNonRunningPrioritiesLocked(pendingJobQueue, false);
                    JobStatus highestBiasJob2 = null;
                    int highBiasWorkType2 = workType;
                    int highBiasAllWorkTypes2 = workType;
                    pendingJobQueue.resetIterator();
                    while (true) {
                        JobStatus nextPending2 = pendingJobQueue.next();
                        if (nextPending2 == null) {
                            break;
                        }
                        if (this.mRunningJobs.contains(nextPending2)) {
                            Slog.wtf(TAG, "Pending queue contained a running job");
                            if (DEBUG) {
                                Slog.e(TAG, "Pending+running job: " + nextPending2);
                            }
                            pendingJobQueue.remove(nextPending2);
                            str = str4;
                        } else {
                            if (!DEBUG || !isSimilarJobRunningLocked(nextPending2)) {
                                str = str4;
                            } else {
                                str = str4;
                                Slog.w(TAG, str + nextPending2);
                            }
                            if (!isPkgConcurrencyLimitedLocked(nextPending2) && (workAsType = this.mWorkCountTracker.canJobStart((allWorkTypes = getJobWorkTypes(nextPending2)))) != 0) {
                                if (highestBiasJob2 == null || highestBiasJob2.lastEvaluatedBias < nextPending2.lastEvaluatedBias) {
                                    highestBiasJob2 = nextPending2;
                                    highBiasWorkType2 = workAsType;
                                    highBiasAllWorkTypes2 = allWorkTypes;
                                }
                                str4 = str;
                            }
                        }
                        str4 = str;
                    }
                    if (highestBiasJob2 != null) {
                        if (DEBUG) {
                            Slog.d(TAG, "About to run job: " + highestBiasJob2);
                        }
                        this.mWorkCountTracker.stageJob(highBiasWorkType2, highBiasAllWorkTypes2);
                        startJobLocked(worker, highestBiasJob2, highBiasWorkType2);
                    }
                }
            }
            noteConcurrency();
            return;
        }
        worker.clearPreferredUid();
        noteConcurrency();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String shouldStopRunningJobLocked(JobServiceContext context) {
        JobStatus js = context.getRunningJobLocked();
        if (js == null || context.isWithinExecutionGuaranteeTime()) {
            return null;
        }
        if (this.mPowerManager.isPowerSaveMode()) {
            return "battery saver";
        }
        if (this.mPowerManager.isDeviceIdleMode()) {
            return "deep doze";
        }
        updateCounterConfigLocked();
        int workType = context.getRunningJobWorkType();
        if (this.mRunningJobs.size() > this.mWorkTypeConfig.getMaxTotal() || this.mWorkCountTracker.isOverTypeLimit(workType)) {
            return "too many jobs running";
        }
        PendingJobQueue pendingJobQueue = this.mService.getPendingJobQueue();
        int numPending = pendingJobQueue.size();
        if (numPending == 0) {
            return null;
        }
        if (js.shouldTreatAsExpeditedJob() || js.startedAsExpeditedJob) {
            if (workType == 16 || workType == 32) {
                if (this.mWorkCountTracker.getPendingJobCount(16) > 0) {
                    return "blocking " + workTypeToString(16) + " queue";
                }
                if (this.mWorkCountTracker.getPendingJobCount(4) > 0 && this.mWorkCountTracker.canJobStart(4, workType) != 0) {
                    return "blocking " + workTypeToString(4) + " queue";
                }
            } else if (this.mWorkCountTracker.getPendingJobCount(4) > 0) {
                return "blocking " + workTypeToString(4) + " queue";
            } else {
                if (js.startedAsExpeditedJob && js.lastEvaluatedBias == 40) {
                    int topEjCount = 0;
                    for (int r = this.mRunningJobs.size() - 1; r >= 0; r--) {
                        JobStatus j = this.mRunningJobs.valueAt(r);
                        if (j.startedAsExpeditedJob && j.lastEvaluatedBias == 40) {
                            topEjCount++;
                        }
                    }
                    if (topEjCount > this.mWorkTypeConfig.getMaxTotal() * 0.5d) {
                        return "prevent top EJ dominance";
                    }
                }
            }
            return null;
        } else if (this.mWorkCountTracker.getPendingJobCount(workType) > 0) {
            return "blocking " + workTypeToString(workType) + " queue";
        } else {
            int remainingWorkTypes = 63;
            pendingJobQueue.resetIterator();
            do {
                JobStatus pending = pendingJobQueue.next();
                if (pending == null) {
                    break;
                }
                int workTypes = getJobWorkTypes(pending);
                if ((workTypes & remainingWorkTypes) > 0 && this.mWorkCountTracker.canJobStart(workTypes, workType) != 0) {
                    return "blocking other pending jobs";
                }
                remainingWorkTypes &= ~workTypes;
            } while (remainingWorkTypes != 0);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean executeTimeoutCommandLocked(PrintWriter pw, String pkgName, int userId, boolean hasJobId, int jobId) {
        boolean foundSome = false;
        for (int i = 0; i < this.mActiveServices.size(); i++) {
            JobServiceContext jc = this.mActiveServices.get(i);
            JobStatus js = jc.getRunningJobLocked();
            if (jc.timeoutIfExecutingLocked(pkgName, userId, hasJobId, jobId, "shell")) {
                foundSome = true;
                pw.print("Timing out: ");
                js.printUniqueId(pw);
                pw.print(" ");
                pw.println(js.getServiceComponent().flattenToShortString());
            }
        }
        return foundSome;
    }

    private JobServiceContext createNewJobServiceContext() {
        return new JobServiceContext(this.mService, this, IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats")), this.mService.mJobPackageTracker, this.mContext.getMainLooper());
    }

    private String printPendingQueueLocked() {
        StringBuilder s = new StringBuilder("Pending queue: ");
        PendingJobQueue pendingJobQueue = this.mService.getPendingJobQueue();
        pendingJobQueue.resetIterator();
        while (true) {
            JobStatus js = pendingJobQueue.next();
            if (js != null) {
                s.append("(").append(js.getJob().getId()).append(", ").append(js.getUid()).append(") ");
            } else {
                return s.toString();
            }
        }
    }

    private static String printAssignments(String header, Collection<ContextAssignment>... list) {
        StringBuilder s = new StringBuilder(header + ": ");
        for (int l = 0; l < list.length; l++) {
            Collection<ContextAssignment> assignments = list[l];
            int c = 0;
            for (ContextAssignment assignment : assignments) {
                JobStatus job = assignment.newJob == null ? assignment.context.getRunningJobLocked() : assignment.newJob;
                if (l > 0 || c > 0) {
                    s.append(" ");
                }
                s.append("(").append(assignment.context.getId()).append("=");
                if (job == null) {
                    s.append("nothing");
                } else {
                    s.append(job.getJobId()).append(SliceClientPermissions.SliceAuthority.DELIMITER).append(job.getUid());
                }
                s.append(")");
                c++;
            }
        }
        return s.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateConfigLocked() {
        DeviceConfig.Properties properties = DeviceConfig.getProperties("jobscheduler", new String[0]);
        this.mScreenOffAdjustmentDelayMs = properties.getLong(KEY_SCREEN_OFF_ADJUSTMENT_DELAY_MS, 30000L);
        WorkConfigLimitsPerMemoryTrimLevel workConfigLimitsPerMemoryTrimLevel = CONFIG_LIMITS_SCREEN_ON;
        workConfigLimitsPerMemoryTrimLevel.normal.update(properties);
        workConfigLimitsPerMemoryTrimLevel.moderate.update(properties);
        workConfigLimitsPerMemoryTrimLevel.low.update(properties);
        workConfigLimitsPerMemoryTrimLevel.critical.update(properties);
        WorkConfigLimitsPerMemoryTrimLevel workConfigLimitsPerMemoryTrimLevel2 = CONFIG_LIMITS_SCREEN_OFF;
        workConfigLimitsPerMemoryTrimLevel2.normal.update(properties);
        workConfigLimitsPerMemoryTrimLevel2.moderate.update(properties);
        workConfigLimitsPerMemoryTrimLevel2.low.update(properties);
        workConfigLimitsPerMemoryTrimLevel2.critical.update(properties);
        this.mPkgConcurrencyLimitEj = Math.max(1, Math.min(16, properties.getInt(KEY_PKG_CONCURRENCY_LIMIT_EJ, 3)));
        this.mPkgConcurrencyLimitRegular = Math.max(1, Math.min(16, properties.getInt(KEY_PKG_CONCURRENCY_LIMIT_REGULAR, 8)));
    }

    public void dumpLocked(final IndentingPrintWriter pw, long now, long nowRealtime) {
        pw.println("Concurrency:");
        pw.increaseIndent();
        try {
            pw.println("Configuration:");
            pw.increaseIndent();
            pw.print(KEY_SCREEN_OFF_ADJUSTMENT_DELAY_MS, Long.valueOf(this.mScreenOffAdjustmentDelayMs)).println();
            pw.print(KEY_PKG_CONCURRENCY_LIMIT_EJ, Integer.valueOf(this.mPkgConcurrencyLimitEj)).println();
            pw.print(KEY_PKG_CONCURRENCY_LIMIT_REGULAR, Integer.valueOf(this.mPkgConcurrencyLimitRegular)).println();
            pw.println();
            WorkConfigLimitsPerMemoryTrimLevel workConfigLimitsPerMemoryTrimLevel = CONFIG_LIMITS_SCREEN_ON;
            workConfigLimitsPerMemoryTrimLevel.normal.dump(pw);
            pw.println();
            workConfigLimitsPerMemoryTrimLevel.moderate.dump(pw);
            pw.println();
            workConfigLimitsPerMemoryTrimLevel.low.dump(pw);
            pw.println();
            workConfigLimitsPerMemoryTrimLevel.critical.dump(pw);
            pw.println();
            WorkConfigLimitsPerMemoryTrimLevel workConfigLimitsPerMemoryTrimLevel2 = CONFIG_LIMITS_SCREEN_OFF;
            workConfigLimitsPerMemoryTrimLevel2.normal.dump(pw);
            pw.println();
            workConfigLimitsPerMemoryTrimLevel2.moderate.dump(pw);
            pw.println();
            workConfigLimitsPerMemoryTrimLevel2.low.dump(pw);
            pw.println();
            workConfigLimitsPerMemoryTrimLevel2.critical.dump(pw);
            pw.println();
            pw.decreaseIndent();
            pw.print("Screen state: current ");
            String str = "ON";
            pw.print(this.mCurrentInteractiveState ? "ON" : "OFF");
            pw.print("  effective ");
            if (!this.mEffectiveInteractiveState) {
                str = "OFF";
            }
            pw.print(str);
            pw.println();
            pw.print("Last screen ON: ");
            TimeUtils.dumpTimeWithDelta(pw, (now - nowRealtime) + this.mLastScreenOnRealtime, now);
            pw.println();
            pw.print("Last screen OFF: ");
            TimeUtils.dumpTimeWithDelta(pw, (now - nowRealtime) + this.mLastScreenOffRealtime, now);
            pw.println();
            pw.println();
            pw.print("Current work counts: ");
            pw.println(this.mWorkCountTracker);
            pw.println();
            pw.print("mLastMemoryTrimLevel: ");
            pw.println(this.mLastMemoryTrimLevel);
            pw.println();
            pw.println("Active Package stats:");
            pw.increaseIndent();
            this.mActivePkgStats.forEach(new Consumer() { // from class: com.android.server.job.JobConcurrencyManager$$ExternalSyntheticLambda3
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((JobConcurrencyManager.PackageStats) obj).dumpLocked(pw);
                }
            });
            pw.decreaseIndent();
            pw.println();
            pw.print("User Grace Period: ");
            pw.println(this.mGracePeriodObserver.mGracePeriodExpiration);
            pw.println();
            this.mStatLogger.dump(pw);
        } finally {
            pw.decreaseIndent();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpContextInfoLocked(IndentingPrintWriter pw, Predicate<JobStatus> predicate, long nowElapsed, long nowUptime) {
        pw.println("Active jobs:");
        pw.increaseIndent();
        if (this.mActiveServices.size() == 0) {
            pw.println("N/A");
        }
        for (int i = 0; i < this.mActiveServices.size(); i++) {
            JobServiceContext jsc = this.mActiveServices.get(i);
            JobStatus job = jsc.getRunningJobLocked();
            if (job == null || predicate.test(job)) {
                pw.print("Slot #");
                pw.print(i);
                pw.print("(ID=");
                pw.print(jsc.getId());
                pw.print("): ");
                jsc.dumpLocked(pw, nowElapsed);
                if (job != null) {
                    pw.increaseIndent();
                    pw.increaseIndent();
                    job.dump(pw, false, nowElapsed);
                    pw.decreaseIndent();
                    pw.print("Evaluated bias: ");
                    pw.println(JobInfo.getBiasString(job.lastEvaluatedBias));
                    pw.print("Active at ");
                    TimeUtils.formatDuration(job.madeActive - nowUptime, pw);
                    pw.print(", pending for ");
                    TimeUtils.formatDuration(job.madeActive - job.madePending, pw);
                    pw.decreaseIndent();
                    pw.println();
                }
            }
        }
        pw.decreaseIndent();
        pw.println();
        pw.print("Idle contexts (");
        pw.print(this.mIdleContexts.size());
        pw.println("):");
        pw.increaseIndent();
        for (int i2 = 0; i2 < this.mIdleContexts.size(); i2++) {
            JobServiceContext jsc2 = this.mIdleContexts.valueAt(i2);
            pw.print("ID=");
            pw.print(jsc2.getId());
            pw.print(": ");
            jsc2.dumpLocked(pw, nowElapsed);
        }
        pw.decreaseIndent();
        if (this.mNumDroppedContexts > 0) {
            pw.println();
            pw.print("Dropped ");
            pw.print(this.mNumDroppedContexts);
            pw.println(" contexts");
        }
    }

    public void dumpProtoLocked(ProtoOutputStream proto, long tag, long now, long nowRealtime) {
        long token = proto.start(tag);
        proto.write(1133871366145L, this.mCurrentInteractiveState);
        proto.write(1133871366146L, this.mEffectiveInteractiveState);
        proto.write(1112396529667L, nowRealtime - this.mLastScreenOnRealtime);
        proto.write(1112396529668L, nowRealtime - this.mLastScreenOffRealtime);
        proto.write(1120986464262L, this.mLastMemoryTrimLevel);
        this.mStatLogger.dumpProto(proto, 1146756268039L);
        proto.end(token);
    }

    boolean shouldRunAsFgUserJob(JobStatus job) {
        if (this.mShouldRestrictBgUser) {
            int userId = job.getSourceUserId();
            UserManagerInternal um = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
            UserInfo userInfo = um.getUserInfo(userId);
            if (userInfo.profileGroupId != -10000 && userInfo.profileGroupId != userId) {
                userId = userInfo.profileGroupId;
                userInfo = um.getUserInfo(userId);
            }
            int currentUser = ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).getCurrentUserId();
            return currentUser == userId || userInfo.isPrimary() || this.mGracePeriodObserver.isWithinGracePeriodForUser(userId);
        }
        return true;
    }

    int getJobWorkTypes(JobStatus js) {
        int classification;
        int classification2 = 0;
        if (shouldRunAsFgUserJob(js)) {
            if (js.lastEvaluatedBias >= 40) {
                classification = 0 | 1;
            } else if (js.lastEvaluatedBias >= 35) {
                classification = 0 | 2;
            } else {
                classification = 0 | 8;
            }
            if (js.shouldTreatAsExpeditedJob()) {
                return classification | 4;
            }
            return classification;
        }
        if (js.lastEvaluatedBias >= 35 || js.shouldTreatAsExpeditedJob()) {
            classification2 = 0 | 16;
        }
        return classification2 | 32;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class WorkTypeConfig {
        private static final String KEY_PREFIX_MAX_BG = "concurrency_max_bg_";
        private static final String KEY_PREFIX_MAX_BGUSER = "concurrency_max_bguser_";
        private static final String KEY_PREFIX_MAX_BGUSER_IMPORTANT = "concurrency_max_bguser_important_";
        private static final String KEY_PREFIX_MAX_EJ = "concurrency_max_ej_";
        private static final String KEY_PREFIX_MAX_FGS = "concurrency_max_fgs_";
        private static final String KEY_PREFIX_MAX_TOP = "concurrency_max_top_";
        static final String KEY_PREFIX_MAX_TOTAL = "concurrency_max_total_";
        private static final String KEY_PREFIX_MIN_BG = "concurrency_min_bg_";
        private static final String KEY_PREFIX_MIN_BGUSER = "concurrency_min_bguser_";
        private static final String KEY_PREFIX_MIN_BGUSER_IMPORTANT = "concurrency_min_bguser_important_";
        private static final String KEY_PREFIX_MIN_EJ = "concurrency_min_ej_";
        private static final String KEY_PREFIX_MIN_FGS = "concurrency_min_fgs_";
        private static final String KEY_PREFIX_MIN_TOP = "concurrency_min_top_";
        private final String mConfigIdentifier;
        private final int mDefaultMaxTotal;
        private int mMaxTotal;
        private final SparseIntArray mMinReservedSlots = new SparseIntArray(6);
        private final SparseIntArray mMaxAllowedSlots = new SparseIntArray(6);
        private final SparseIntArray mDefaultMinReservedSlots = new SparseIntArray(6);
        private final SparseIntArray mDefaultMaxAllowedSlots = new SparseIntArray(6);

        WorkTypeConfig(String configIdentifier, int defaultMaxTotal, List<Pair<Integer, Integer>> defaultMin, List<Pair<Integer, Integer>> defaultMax) {
            this.mConfigIdentifier = configIdentifier;
            int min = Math.min(defaultMaxTotal, 16);
            this.mMaxTotal = min;
            this.mDefaultMaxTotal = min;
            int numReserved = 0;
            for (int i = defaultMin.size() - 1; i >= 0; i--) {
                this.mDefaultMinReservedSlots.put(((Integer) defaultMin.get(i).first).intValue(), ((Integer) defaultMin.get(i).second).intValue());
                numReserved += ((Integer) defaultMin.get(i).second).intValue();
            }
            int i2 = this.mDefaultMaxTotal;
            if (i2 < 0 || numReserved > i2) {
                throw new IllegalArgumentException("Invalid default config: t=" + defaultMaxTotal + " min=" + defaultMin + " max=" + defaultMax);
            }
            for (int i3 = defaultMax.size() - 1; i3 >= 0; i3--) {
                this.mDefaultMaxAllowedSlots.put(((Integer) defaultMax.get(i3).first).intValue(), ((Integer) defaultMax.get(i3).second).intValue());
            }
            update(new DeviceConfig.Properties.Builder("jobscheduler").build());
        }

        void update(DeviceConfig.Properties properties) {
            this.mMaxTotal = Math.max(1, Math.min(16, properties.getInt(KEY_PREFIX_MAX_TOTAL + this.mConfigIdentifier, this.mDefaultMaxTotal)));
            this.mMaxAllowedSlots.clear();
            int maxTop = Math.max(1, Math.min(this.mMaxTotal, properties.getInt(KEY_PREFIX_MAX_TOP + this.mConfigIdentifier, this.mDefaultMaxAllowedSlots.get(1, this.mMaxTotal))));
            this.mMaxAllowedSlots.put(1, maxTop);
            int maxFgs = Math.max(1, Math.min(this.mMaxTotal, properties.getInt(KEY_PREFIX_MAX_FGS + this.mConfigIdentifier, this.mDefaultMaxAllowedSlots.get(2, this.mMaxTotal))));
            this.mMaxAllowedSlots.put(2, maxFgs);
            int maxEj = Math.max(1, Math.min(this.mMaxTotal, properties.getInt(KEY_PREFIX_MAX_EJ + this.mConfigIdentifier, this.mDefaultMaxAllowedSlots.get(4, this.mMaxTotal))));
            this.mMaxAllowedSlots.put(4, maxEj);
            int maxBg = Math.max(1, Math.min(this.mMaxTotal, properties.getInt(KEY_PREFIX_MAX_BG + this.mConfigIdentifier, this.mDefaultMaxAllowedSlots.get(8, this.mMaxTotal))));
            this.mMaxAllowedSlots.put(8, maxBg);
            int maxBgUserImp = Math.max(1, Math.min(this.mMaxTotal, properties.getInt(KEY_PREFIX_MAX_BGUSER_IMPORTANT + this.mConfigIdentifier, this.mDefaultMaxAllowedSlots.get(16, this.mMaxTotal))));
            this.mMaxAllowedSlots.put(16, maxBgUserImp);
            int maxBgUser = Math.max(1, Math.min(this.mMaxTotal, properties.getInt(KEY_PREFIX_MAX_BGUSER + this.mConfigIdentifier, this.mDefaultMaxAllowedSlots.get(32, this.mMaxTotal))));
            this.mMaxAllowedSlots.put(32, maxBgUser);
            int remaining = this.mMaxTotal;
            this.mMinReservedSlots.clear();
            int minTop = Math.max(1, Math.min(Math.min(maxTop, this.mMaxTotal), properties.getInt(KEY_PREFIX_MIN_TOP + this.mConfigIdentifier, this.mDefaultMinReservedSlots.get(1))));
            this.mMinReservedSlots.put(1, minTop);
            int remaining2 = remaining - minTop;
            int minFgs = Math.max(0, Math.min(Math.min(maxFgs, remaining2), properties.getInt(KEY_PREFIX_MIN_FGS + this.mConfigIdentifier, this.mDefaultMinReservedSlots.get(2))));
            this.mMinReservedSlots.put(2, minFgs);
            int remaining3 = remaining2 - minFgs;
            int minEj = Math.max(0, Math.min(Math.min(maxEj, remaining3), properties.getInt(KEY_PREFIX_MIN_EJ + this.mConfigIdentifier, this.mDefaultMinReservedSlots.get(4))));
            this.mMinReservedSlots.put(4, minEj);
            int remaining4 = remaining3 - minEj;
            int minBg = Math.max(0, Math.min(Math.min(maxBg, remaining4), properties.getInt(KEY_PREFIX_MIN_BG + this.mConfigIdentifier, this.mDefaultMinReservedSlots.get(8))));
            this.mMinReservedSlots.put(8, minBg);
            int remaining5 = remaining4 - minBg;
            int minBgUserImp = Math.max(0, Math.min(Math.min(maxBgUserImp, remaining5), properties.getInt(KEY_PREFIX_MIN_BGUSER_IMPORTANT + this.mConfigIdentifier, this.mDefaultMinReservedSlots.get(16, 0))));
            this.mMinReservedSlots.put(16, minBgUserImp);
            int minBgUser = Math.max(0, Math.min(Math.min(maxBgUser, remaining5), properties.getInt(KEY_PREFIX_MIN_BGUSER + this.mConfigIdentifier, this.mDefaultMinReservedSlots.get(32, 0))));
            this.mMinReservedSlots.put(32, minBgUser);
        }

        int getMaxTotal() {
            return this.mMaxTotal;
        }

        int getMax(int workType) {
            return this.mMaxAllowedSlots.get(workType, this.mMaxTotal);
        }

        int getMinReserved(int workType) {
            return this.mMinReservedSlots.get(workType);
        }

        void dump(IndentingPrintWriter pw) {
            pw.print(KEY_PREFIX_MAX_TOTAL + this.mConfigIdentifier, Integer.valueOf(this.mMaxTotal)).println();
            pw.print(KEY_PREFIX_MIN_TOP + this.mConfigIdentifier, Integer.valueOf(this.mMinReservedSlots.get(1))).println();
            pw.print(KEY_PREFIX_MAX_TOP + this.mConfigIdentifier, Integer.valueOf(this.mMaxAllowedSlots.get(1))).println();
            pw.print(KEY_PREFIX_MIN_FGS + this.mConfigIdentifier, Integer.valueOf(this.mMinReservedSlots.get(2))).println();
            pw.print(KEY_PREFIX_MAX_FGS + this.mConfigIdentifier, Integer.valueOf(this.mMaxAllowedSlots.get(2))).println();
            pw.print(KEY_PREFIX_MIN_EJ + this.mConfigIdentifier, Integer.valueOf(this.mMinReservedSlots.get(4))).println();
            pw.print(KEY_PREFIX_MAX_EJ + this.mConfigIdentifier, Integer.valueOf(this.mMaxAllowedSlots.get(4))).println();
            pw.print(KEY_PREFIX_MIN_BG + this.mConfigIdentifier, Integer.valueOf(this.mMinReservedSlots.get(8))).println();
            pw.print(KEY_PREFIX_MAX_BG + this.mConfigIdentifier, Integer.valueOf(this.mMaxAllowedSlots.get(8))).println();
            pw.print(KEY_PREFIX_MIN_BGUSER + this.mConfigIdentifier, Integer.valueOf(this.mMinReservedSlots.get(16))).println();
            pw.print(KEY_PREFIX_MAX_BGUSER + this.mConfigIdentifier, Integer.valueOf(this.mMaxAllowedSlots.get(16))).println();
            pw.print(KEY_PREFIX_MIN_BGUSER + this.mConfigIdentifier, Integer.valueOf(this.mMinReservedSlots.get(32))).println();
            pw.print(KEY_PREFIX_MAX_BGUSER + this.mConfigIdentifier, Integer.valueOf(this.mMaxAllowedSlots.get(32))).println();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class WorkConfigLimitsPerMemoryTrimLevel {
        public final WorkTypeConfig critical;
        public final WorkTypeConfig low;
        public final WorkTypeConfig moderate;
        public final WorkTypeConfig normal;

        WorkConfigLimitsPerMemoryTrimLevel(WorkTypeConfig normal, WorkTypeConfig moderate, WorkTypeConfig low, WorkTypeConfig critical) {
            this.normal = normal;
            this.moderate = moderate;
            this.low = low;
            this.critical = critical;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class GracePeriodObserver extends UserSwitchObserver {
        int mGracePeriod;
        final SparseLongArray mGracePeriodExpiration = new SparseLongArray();
        final Object mLock = new Object();
        private int mCurrentUserId = ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).getCurrentUserId();
        private final UserManagerInternal mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);

        GracePeriodObserver(Context context) {
            this.mGracePeriod = Math.max(0, context.getResources().getInteger(17694844));
        }

        public void onUserSwitchComplete(int newUserId) {
            long expiration = JobSchedulerService.sElapsedRealtimeClock.millis() + this.mGracePeriod;
            synchronized (this.mLock) {
                int i = this.mCurrentUserId;
                if (i != -10000 && this.mUserManagerInternal.exists(i)) {
                    this.mGracePeriodExpiration.append(this.mCurrentUserId, expiration);
                }
                this.mGracePeriodExpiration.delete(newUserId);
                this.mCurrentUserId = newUserId;
            }
        }

        void onUserRemoved(int userId) {
            synchronized (this.mLock) {
                this.mGracePeriodExpiration.delete(userId);
            }
        }

        public boolean isWithinGracePeriodForUser(int userId) {
            boolean z;
            synchronized (this.mLock) {
                z = userId == this.mCurrentUserId || JobSchedulerService.sElapsedRealtimeClock.millis() < this.mGracePeriodExpiration.get(userId, JobStatus.NO_LATEST_RUNTIME);
            }
            return z;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class WorkCountTracker {
        private int mConfigMaxTotal;
        private final SparseIntArray mConfigNumReservedSlots = new SparseIntArray(6);
        private final SparseIntArray mConfigAbsoluteMaxSlots = new SparseIntArray(6);
        private final SparseIntArray mRecycledReserved = new SparseIntArray(6);
        private final SparseIntArray mNumActuallyReservedSlots = new SparseIntArray(6);
        private final SparseIntArray mNumPendingJobs = new SparseIntArray(6);
        private final SparseIntArray mNumRunningJobs = new SparseIntArray(6);
        private final SparseIntArray mNumStartingJobs = new SparseIntArray(6);
        private int mNumUnspecializedRemaining = 0;

        WorkCountTracker() {
        }

        void setConfig(WorkTypeConfig workTypeConfig) {
            this.mConfigMaxTotal = workTypeConfig.getMaxTotal();
            for (int workType = 1; workType < 63; workType <<= 1) {
                this.mConfigNumReservedSlots.put(workType, workTypeConfig.getMinReserved(workType));
                this.mConfigAbsoluteMaxSlots.put(workType, workTypeConfig.getMax(workType));
            }
            int workType2 = this.mConfigMaxTotal;
            this.mNumUnspecializedRemaining = workType2;
            for (int i = this.mNumRunningJobs.size() - 1; i >= 0; i--) {
                this.mNumUnspecializedRemaining -= Math.max(this.mNumRunningJobs.valueAt(i), this.mConfigNumReservedSlots.get(this.mNumRunningJobs.keyAt(i)));
            }
        }

        void resetCounts() {
            this.mNumActuallyReservedSlots.clear();
            this.mNumPendingJobs.clear();
            this.mNumRunningJobs.clear();
            resetStagingCount();
        }

        void resetStagingCount() {
            this.mNumStartingJobs.clear();
        }

        void incrementRunningJobCount(int workType) {
            SparseIntArray sparseIntArray = this.mNumRunningJobs;
            sparseIntArray.put(workType, sparseIntArray.get(workType) + 1);
        }

        void incrementPendingJobCount(int workTypes) {
            adjustPendingJobCount(workTypes, true);
        }

        void decrementPendingJobCount(int workTypes) {
            if (adjustPendingJobCount(workTypes, false) > 1) {
                for (int workType = 1; workType <= workTypes; workType <<= 1) {
                    if ((workType & workTypes) == workType) {
                        maybeAdjustReservations(workType);
                    }
                }
            }
        }

        private int adjustPendingJobCount(int workTypes, boolean add) {
            int adj = add ? 1 : -1;
            int numAdj = 0;
            for (int workType = 1; workType <= workTypes; workType <<= 1) {
                if ((workTypes & workType) == workType) {
                    SparseIntArray sparseIntArray = this.mNumPendingJobs;
                    sparseIntArray.put(workType, sparseIntArray.get(workType) + adj);
                    numAdj++;
                }
            }
            return numAdj;
        }

        void stageJob(int workType, int allWorkTypes) {
            int newNumStartingJobs = this.mNumStartingJobs.get(workType) + 1;
            this.mNumStartingJobs.put(workType, newNumStartingJobs);
            decrementPendingJobCount(allWorkTypes);
            if (this.mNumRunningJobs.get(workType) + newNumStartingJobs > this.mNumActuallyReservedSlots.get(workType)) {
                this.mNumUnspecializedRemaining--;
            }
        }

        void onStagedJobFailed(int workType) {
            int oldNumStartingJobs = this.mNumStartingJobs.get(workType);
            if (oldNumStartingJobs == 0) {
                Slog.e(JobConcurrencyManager.TAG, "# staged jobs for " + workType + " went negative.");
                return;
            }
            this.mNumStartingJobs.put(workType, oldNumStartingJobs - 1);
            maybeAdjustReservations(workType);
        }

        private void maybeAdjustReservations(int workType) {
            int numRemainingForType = Math.max(this.mConfigNumReservedSlots.get(workType), this.mNumRunningJobs.get(workType) + this.mNumStartingJobs.get(workType) + this.mNumPendingJobs.get(workType));
            if (numRemainingForType < this.mNumActuallyReservedSlots.get(workType)) {
                this.mNumActuallyReservedSlots.put(workType, numRemainingForType);
                int assignWorkType = 0;
                for (int i = 0; i < this.mNumActuallyReservedSlots.size(); i++) {
                    int wt = this.mNumActuallyReservedSlots.keyAt(i);
                    if (assignWorkType == 0 || wt < assignWorkType) {
                        int total = this.mNumRunningJobs.get(wt) + this.mNumStartingJobs.get(wt) + this.mNumPendingJobs.get(wt);
                        if (this.mNumActuallyReservedSlots.valueAt(i) < this.mConfigAbsoluteMaxSlots.get(wt) && total > this.mNumActuallyReservedSlots.valueAt(i)) {
                            assignWorkType = wt;
                        }
                    }
                }
                if (assignWorkType != 0) {
                    SparseIntArray sparseIntArray = this.mNumActuallyReservedSlots;
                    sparseIntArray.put(assignWorkType, sparseIntArray.get(assignWorkType) + 1);
                    return;
                }
                this.mNumUnspecializedRemaining++;
            }
        }

        void onJobStarted(int workType) {
            SparseIntArray sparseIntArray = this.mNumRunningJobs;
            sparseIntArray.put(workType, sparseIntArray.get(workType) + 1);
            int oldNumStartingJobs = this.mNumStartingJobs.get(workType);
            if (oldNumStartingJobs == 0) {
                Slog.e(JobConcurrencyManager.TAG, "# stated jobs for " + workType + " went negative.");
            } else {
                this.mNumStartingJobs.put(workType, oldNumStartingJobs - 1);
            }
        }

        void onJobFinished(int workType) {
            int newNumRunningJobs = this.mNumRunningJobs.get(workType) - 1;
            if (newNumRunningJobs < 0) {
                Slog.e(JobConcurrencyManager.TAG, "# running jobs for " + workType + " went negative.");
                return;
            }
            this.mNumRunningJobs.put(workType, newNumRunningJobs);
            maybeAdjustReservations(workType);
        }

        void onCountDone() {
            this.mNumUnspecializedRemaining = this.mConfigMaxTotal;
            for (int workType = 1; workType < 63; workType <<= 1) {
                int run = this.mNumRunningJobs.get(workType);
                this.mRecycledReserved.put(workType, run);
                this.mNumUnspecializedRemaining -= run;
            }
            for (int workType2 = 1; workType2 < 63; workType2 <<= 1) {
                int num = this.mNumRunningJobs.get(workType2) + this.mNumPendingJobs.get(workType2);
                int res = this.mRecycledReserved.get(workType2);
                int fillUp = Math.max(0, Math.min(this.mNumUnspecializedRemaining, Math.min(num, this.mConfigNumReservedSlots.get(workType2) - res)));
                this.mRecycledReserved.put(workType2, res + fillUp);
                this.mNumUnspecializedRemaining -= fillUp;
            }
            for (int workType3 = 1; workType3 < 63; workType3 <<= 1) {
                int num2 = this.mNumRunningJobs.get(workType3) + this.mNumPendingJobs.get(workType3);
                int res2 = this.mRecycledReserved.get(workType3);
                int unspecializedAssigned = Math.max(0, Math.min(this.mNumUnspecializedRemaining, Math.min(this.mConfigAbsoluteMaxSlots.get(workType3), num2) - res2));
                this.mNumActuallyReservedSlots.put(workType3, res2 + unspecializedAssigned);
                this.mNumUnspecializedRemaining -= unspecializedAssigned;
            }
        }

        int canJobStart(int workTypes) {
            for (int workType = 1; workType <= workTypes; workType <<= 1) {
                if ((workTypes & workType) == workType) {
                    int maxAllowed = Math.min(this.mConfigAbsoluteMaxSlots.get(workType), this.mNumActuallyReservedSlots.get(workType) + this.mNumUnspecializedRemaining);
                    if (this.mNumRunningJobs.get(workType) + this.mNumStartingJobs.get(workType) < maxAllowed) {
                        return workType;
                    }
                }
            }
            return 0;
        }

        int canJobStart(int workTypes, int replacingWorkType) {
            boolean changedNums;
            int oldNumRunning = this.mNumRunningJobs.get(replacingWorkType);
            if (replacingWorkType != 0 && oldNumRunning > 0) {
                this.mNumRunningJobs.put(replacingWorkType, oldNumRunning - 1);
                this.mNumUnspecializedRemaining++;
                changedNums = true;
            } else {
                changedNums = false;
            }
            int ret = canJobStart(workTypes);
            if (changedNums) {
                this.mNumRunningJobs.put(replacingWorkType, oldNumRunning);
                this.mNumUnspecializedRemaining--;
            }
            return ret;
        }

        int getPendingJobCount(int workType) {
            return this.mNumPendingJobs.get(workType, 0);
        }

        int getRunningJobCount(int workType) {
            return this.mNumRunningJobs.get(workType, 0);
        }

        boolean isOverTypeLimit(int workType) {
            return getRunningJobCount(workType) > this.mConfigAbsoluteMaxSlots.get(workType);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Config={");
            sb.append("tot=").append(this.mConfigMaxTotal);
            sb.append(" mins=");
            sb.append(this.mConfigNumReservedSlots);
            sb.append(" maxs=");
            sb.append(this.mConfigAbsoluteMaxSlots);
            sb.append("}");
            sb.append(", act res=").append(this.mNumActuallyReservedSlots);
            sb.append(", Pending=").append(this.mNumPendingJobs);
            sb.append(", Running=").append(this.mNumRunningJobs);
            sb.append(", Staged=").append(this.mNumStartingJobs);
            sb.append(", # unspecialized remaining=").append(this.mNumUnspecializedRemaining);
            return sb.toString();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class PackageStats {
        public int numRunningEj;
        public int numRunningRegular;
        public int numStagedEj;
        public int numStagedRegular;
        public String packageName;
        public int userId;

        PackageStats() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setPackage(int userId, String packageName) {
            this.userId = userId;
            this.packageName = packageName;
            this.numRunningRegular = 0;
            this.numRunningEj = 0;
            resetStagedCount();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void resetStagedCount() {
            this.numStagedRegular = 0;
            this.numStagedEj = 0;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void adjustRunningCount(boolean add, boolean forEj) {
            if (forEj) {
                this.numRunningEj = Math.max(0, this.numRunningEj + (add ? 1 : -1));
            } else {
                this.numRunningRegular = Math.max(0, this.numRunningRegular + (add ? 1 : -1));
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void adjustStagedCount(boolean add, boolean forEj) {
            if (forEj) {
                this.numStagedEj = Math.max(0, this.numStagedEj + (add ? 1 : -1));
            } else {
                this.numStagedRegular = Math.max(0, this.numStagedRegular + (add ? 1 : -1));
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dumpLocked(IndentingPrintWriter pw) {
            pw.print("PackageStats{");
            pw.print(this.userId);
            pw.print("-");
            pw.print(this.packageName);
            pw.print("#runEJ", Integer.valueOf(this.numRunningEj));
            pw.print("#runReg", Integer.valueOf(this.numRunningRegular));
            pw.print("#stagedEJ", Integer.valueOf(this.numStagedEj));
            pw.print("#stagedReg", Integer.valueOf(this.numStagedRegular));
            pw.println("}");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class ContextAssignment {
        public JobServiceContext context;
        public JobStatus newJob;
        public int newWorkType;
        public String preemptReason;
        public int preemptReasonCode;
        public int preferredUid;
        public String shouldStopJobReason;
        public int workType;

        private ContextAssignment() {
            this.preferredUid = -1;
            this.workType = 0;
            this.preemptReasonCode = 0;
            this.newWorkType = 0;
        }

        void clear() {
            this.context = null;
            this.preferredUid = -1;
            this.workType = 0;
            this.preemptReason = null;
            this.preemptReasonCode = 0;
            this.shouldStopJobReason = null;
            this.newJob = null;
            this.newWorkType = 0;
        }
    }

    void addRunningJobForTesting(JobStatus job) {
        this.mRunningJobs.add(job);
        PackageStats packageStats = getPackageStatsForTesting(job.getSourceUserId(), job.getSourcePackageName());
        packageStats.adjustRunningCount(true, job.shouldTreatAsExpeditedJob());
    }

    int getPackageConcurrencyLimitEj() {
        return this.mPkgConcurrencyLimitEj;
    }

    int getPackageConcurrencyLimitRegular() {
        return this.mPkgConcurrencyLimitRegular;
    }

    PackageStats getPackageStatsForTesting(int userId, String packageName) {
        PackageStats packageStats = getPkgStatsLocked(userId, packageName);
        this.mActivePkgStats.add(userId, packageName, packageStats);
        return packageStats;
    }
}
