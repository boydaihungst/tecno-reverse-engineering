package com.android.server.job.controllers;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.IUidObserver;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManagerInternal;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseArrayMap;
import android.util.SparseBooleanArray;
import android.util.SparseLongArray;
import android.util.SparseSetArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.jobs.ArrayUtils;
import com.android.server.JobSchedulerBackgroundThread;
import com.android.server.LocalServices;
import com.android.server.PowerAllowlistInternal;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.controllers.QuotaController;
import com.android.server.usage.AppStandbyController;
import com.android.server.usage.AppStandbyInternal;
import com.android.server.usage.UnixCalendar;
import com.android.server.utils.AlarmQueue;
import dalvik.annotation.optimization.NeverCompile;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public final class QuotaController extends StateController {
    private static final String ALARM_TAG_CLEANUP = "*job.cleanup*";
    private static final String ALARM_TAG_QUOTA_CHECK = "*job.quota_check*";
    private static final boolean DEBUG;
    private static final long MAX_PERIOD_MS = 86400000;
    private static final int MSG_CHECK_PACKAGE = 2;
    private static final int MSG_CLEAN_UP_SESSIONS = 1;
    static final int MSG_END_GRACE_PERIOD = 6;
    private static final int MSG_PROCESS_USAGE_EVENT = 5;
    static final int MSG_REACHED_EJ_QUOTA = 4;
    static final int MSG_REACHED_QUOTA = 0;
    private static final int MSG_UID_PROCESS_STATE_CHANGED = 3;
    private static final int SYSTEM_APP_CHECK_FLAGS = 4993024;
    private static final String TAG = "JobScheduler.Quota";
    private final AlarmManager mAlarmManager;
    private final long[] mAllowedTimePerPeriodMs;
    private final BackgroundJobsController mBackgroundJobsController;
    private final long[] mBucketPeriodsMs;
    private final ConnectivityController mConnectivityController;
    private final Consumer<List<TimedEvent>> mDeleteOldEventsFunctor;
    private long mEJGracePeriodTempAllowlistMs;
    private long mEJGracePeriodTopAppMs;
    private long mEJLimitWindowSizeMs;
    private final long[] mEJLimitsMs;
    private final SparseArrayMap<String, Timer> mEJPkgTimers;
    private long mEJRewardInteractionMs;
    private long mEJRewardNotificationSeenMs;
    private long mEJRewardTopAppMs;
    private final SparseArrayMap<String, ShrinkableDebits> mEJStats;
    private final SparseArrayMap<String, List<TimedEvent>> mEJTimingSessions;
    private long mEJTopAppTimeChunkSizeMs;
    private final EarliestEndTimeFunctor mEarliestEndTimeFunctor;
    private long mEjLimitAdditionInstallerMs;
    private long mEjLimitAdditionSpecialMs;
    private final SparseArrayMap<String, ExecutionStats[]> mExecutionStatsCache;
    private final SparseBooleanArray mForegroundUids;
    private final QcHandler mHandler;
    private final InQuotaAlarmQueue mInQuotaAlarmQueue;
    private boolean mIsEnabled;
    private final int[] mMaxBucketJobCounts;
    private final int[] mMaxBucketSessionCounts;
    private long mMaxExecutionTimeIntoQuotaMs;
    private long mMaxExecutionTimeMs;
    private int mMaxJobCountPerRateLimitingWindow;
    private int mMaxSessionCountPerRateLimitingWindow;
    private long mNextCleanupTimeElapsed;
    private final SparseArrayMap<String, Timer> mPkgTimers;
    private final QcConstants mQcConstants;
    private long mQuotaBufferMs;
    private long mQuotaBumpAdditionalDurationMs;
    private int mQuotaBumpAdditionalJobCount;
    private int mQuotaBumpAdditionalSessionCount;
    private int mQuotaBumpLimit;
    private long mQuotaBumpWindowSizeMs;
    private long mRateLimitingWindowMs;
    private final AlarmManager.OnAlarmListener mSessionCleanupAlarmListener;
    private final SparseSetArray<String> mSystemInstallers;
    private final SparseBooleanArray mTempAllowlistCache;
    private final SparseLongArray mTempAllowlistGraceCache;
    private final TimedEventTooOldPredicate mTimedEventTooOld;
    private final TimerChargingUpdateFunctor mTimerChargingUpdateFunctor;
    private final SparseArrayMap<String, List<TimedEvent>> mTimingEvents;
    private long mTimingSessionCoalescingDurationMs;
    private final SparseBooleanArray mTopAppCache;
    private final SparseLongArray mTopAppGraceCache;
    private final SparseArrayMap<String, TopAppTimer> mTopAppTrackers;
    private final ArraySet<JobStatus> mTopStartedJobs;
    private final SparseArrayMap<String, ArraySet<JobStatus>> mTrackedJobs;
    private final UidConstraintUpdater mUpdateUidConstraints;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface TimedEvent {
        void dump(IndentingPrintWriter indentingPrintWriter);

        long getEndTimeElapsed();
    }

    static {
        DEBUG = JobSchedulerService.DEBUG || Log.isLoggable(TAG, 3);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int hashLong(long val) {
        return (int) ((val >>> 32) ^ val);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class ExecutionStats {
        public long allowedTimePerPeriodMs;
        public int bgJobCountInMaxPeriod;
        public int bgJobCountInWindow;
        public long executionTimeInMaxPeriodMs;
        public long executionTimeInWindowMs;
        public long expirationTimeElapsed;
        public long inQuotaTimeElapsed;
        public int jobCountInRateLimitingWindow;
        public int jobCountLimit;
        public long jobRateLimitExpirationTimeElapsed;
        public int sessionCountInRateLimitingWindow;
        public int sessionCountInWindow;
        public int sessionCountLimit;
        public long sessionRateLimitExpirationTimeElapsed;
        public long windowSizeMs;

        ExecutionStats() {
        }

        public String toString() {
            return "expirationTime=" + this.expirationTimeElapsed + ", allowedTimePerPeriodMs=" + this.allowedTimePerPeriodMs + ", windowSizeMs=" + this.windowSizeMs + ", jobCountLimit=" + this.jobCountLimit + ", sessionCountLimit=" + this.sessionCountLimit + ", executionTimeInWindow=" + this.executionTimeInWindowMs + ", bgJobCountInWindow=" + this.bgJobCountInWindow + ", executionTimeInMaxPeriod=" + this.executionTimeInMaxPeriodMs + ", bgJobCountInMaxPeriod=" + this.bgJobCountInMaxPeriod + ", sessionCountInWindow=" + this.sessionCountInWindow + ", inQuotaTime=" + this.inQuotaTimeElapsed + ", rateLimitJobCountExpirationTime=" + this.jobRateLimitExpirationTimeElapsed + ", rateLimitJobCountWindow=" + this.jobCountInRateLimitingWindow + ", rateLimitSessionCountExpirationTime=" + this.sessionRateLimitExpirationTimeElapsed + ", rateLimitSessionCountWindow=" + this.sessionCountInRateLimitingWindow;
        }

        public boolean equals(Object obj) {
            if (obj instanceof ExecutionStats) {
                ExecutionStats other = (ExecutionStats) obj;
                return this.expirationTimeElapsed == other.expirationTimeElapsed && this.allowedTimePerPeriodMs == other.allowedTimePerPeriodMs && this.windowSizeMs == other.windowSizeMs && this.jobCountLimit == other.jobCountLimit && this.sessionCountLimit == other.sessionCountLimit && this.executionTimeInWindowMs == other.executionTimeInWindowMs && this.bgJobCountInWindow == other.bgJobCountInWindow && this.executionTimeInMaxPeriodMs == other.executionTimeInMaxPeriodMs && this.sessionCountInWindow == other.sessionCountInWindow && this.bgJobCountInMaxPeriod == other.bgJobCountInMaxPeriod && this.inQuotaTimeElapsed == other.inQuotaTimeElapsed && this.jobRateLimitExpirationTimeElapsed == other.jobRateLimitExpirationTimeElapsed && this.jobCountInRateLimitingWindow == other.jobCountInRateLimitingWindow && this.sessionRateLimitExpirationTimeElapsed == other.sessionRateLimitExpirationTimeElapsed && this.sessionCountInRateLimitingWindow == other.sessionCountInRateLimitingWindow;
            }
            return false;
        }

        public int hashCode() {
            int result = (0 * 31) + QuotaController.hashLong(this.expirationTimeElapsed);
            return (((((((((((((((((((((((((((result * 31) + QuotaController.hashLong(this.allowedTimePerPeriodMs)) * 31) + QuotaController.hashLong(this.windowSizeMs)) * 31) + QuotaController.hashLong(this.jobCountLimit)) * 31) + QuotaController.hashLong(this.sessionCountLimit)) * 31) + QuotaController.hashLong(this.executionTimeInWindowMs)) * 31) + this.bgJobCountInWindow) * 31) + QuotaController.hashLong(this.executionTimeInMaxPeriodMs)) * 31) + this.bgJobCountInMaxPeriod) * 31) + this.sessionCountInWindow) * 31) + QuotaController.hashLong(this.inQuotaTimeElapsed)) * 31) + QuotaController.hashLong(this.jobRateLimitExpirationTimeElapsed)) * 31) + this.jobCountInRateLimitingWindow) * 31) + QuotaController.hashLong(this.sessionRateLimitExpirationTimeElapsed)) * 31) + this.sessionCountInRateLimitingWindow;
        }
    }

    /* loaded from: classes.dex */
    private class QcUidObserver extends IUidObserver.Stub {
        private QcUidObserver() {
        }

        public void onUidStateChanged(int uid, int procState, long procStateSeq, int capability) {
            QuotaController.this.mHandler.obtainMessage(3, uid, procState).sendToTarget();
        }

        public void onUidGone(int uid, boolean disabled) {
        }

        public void onUidActive(int uid) {
        }

        public void onUidIdle(int uid, boolean disabled) {
        }

        public void onUidCachedChanged(int uid, boolean cached) {
        }

        public void onUidProcAdjChanged(int uid) {
        }
    }

    public QuotaController(JobSchedulerService service, BackgroundJobsController backgroundJobsController, ConnectivityController connectivityController) {
        super(service);
        this.mTrackedJobs = new SparseArrayMap<>();
        this.mPkgTimers = new SparseArrayMap<>();
        this.mEJPkgTimers = new SparseArrayMap<>();
        this.mTimingEvents = new SparseArrayMap<>();
        this.mEJTimingSessions = new SparseArrayMap<>();
        this.mExecutionStatsCache = new SparseArrayMap<>();
        this.mEJStats = new SparseArrayMap<>();
        this.mTopAppTrackers = new SparseArrayMap<>();
        this.mForegroundUids = new SparseBooleanArray();
        this.mTopStartedJobs = new ArraySet<>();
        this.mTempAllowlistCache = new SparseBooleanArray();
        this.mTempAllowlistGraceCache = new SparseLongArray();
        this.mTopAppCache = new SparseBooleanArray();
        this.mTopAppGraceCache = new SparseLongArray();
        this.mAllowedTimePerPeriodMs = new long[]{600000, 600000, 600000, 600000, 0, 600000, 600000};
        this.mMaxExecutionTimeMs = 14400000L;
        this.mQuotaBufferMs = 30000L;
        this.mMaxExecutionTimeIntoQuotaMs = 14400000 - 30000;
        this.mRateLimitingWindowMs = 60000L;
        this.mMaxJobCountPerRateLimitingWindow = 20;
        this.mMaxSessionCountPerRateLimitingWindow = 20;
        this.mNextCleanupTimeElapsed = 0L;
        this.mSessionCleanupAlarmListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.job.controllers.QuotaController.1
            @Override // android.app.AlarmManager.OnAlarmListener
            public void onAlarm() {
                QuotaController.this.mHandler.obtainMessage(1).sendToTarget();
            }
        };
        this.mBucketPeriodsMs = new long[]{600000, AppStandbyController.ConstantsObserver.DEFAULT_SYSTEM_UPDATE_TIMEOUT, 28800000, 86400000, 0, 86400000, 600000};
        this.mMaxBucketJobCounts = new int[]{75, 120, 200, 48, 0, 10, 75};
        this.mMaxBucketSessionCounts = new int[]{75, 10, 8, 3, 0, 1, 75};
        this.mTimingSessionCoalescingDurationMs = 5000L;
        this.mEJLimitsMs = new long[]{1800000, 1800000, 600000, 600000, 0, BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS, 2700000};
        this.mEjLimitAdditionInstallerMs = 1800000L;
        this.mEjLimitAdditionSpecialMs = 900000L;
        this.mEJLimitWindowSizeMs = 86400000L;
        this.mEJTopAppTimeChunkSizeMs = 30000L;
        this.mEJRewardTopAppMs = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        this.mEJRewardInteractionMs = 15000L;
        this.mEJRewardNotificationSeenMs = 0L;
        this.mEJGracePeriodTempAllowlistMs = 180000L;
        this.mEJGracePeriodTopAppMs = 60000L;
        this.mQuotaBumpAdditionalDurationMs = 60000L;
        this.mQuotaBumpAdditionalJobCount = 2;
        this.mQuotaBumpAdditionalSessionCount = 1;
        this.mQuotaBumpWindowSizeMs = 28800000L;
        this.mQuotaBumpLimit = 8;
        this.mSystemInstallers = new SparseSetArray<>();
        this.mEarliestEndTimeFunctor = new EarliestEndTimeFunctor();
        this.mTimerChargingUpdateFunctor = new TimerChargingUpdateFunctor();
        this.mUpdateUidConstraints = new UidConstraintUpdater();
        this.mTimedEventTooOld = new TimedEventTooOldPredicate();
        this.mDeleteOldEventsFunctor = new Consumer() { // from class: com.android.server.job.controllers.QuotaController$$ExternalSyntheticLambda4
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                QuotaController.this.m4232lambda$new$2$comandroidserverjobcontrollersQuotaController((List) obj);
            }
        };
        this.mHandler = new QcHandler(this.mContext.getMainLooper());
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService(AlarmManager.class);
        this.mQcConstants = new QcConstants();
        this.mBackgroundJobsController = backgroundJobsController;
        this.mConnectivityController = connectivityController;
        this.mIsEnabled = !this.mConstants.USE_TARE_POLICY;
        this.mInQuotaAlarmQueue = new InQuotaAlarmQueue(this.mContext, this.mContext.getMainLooper());
        AppStandbyInternal appStandby = (AppStandbyInternal) LocalServices.getService(AppStandbyInternal.class);
        appStandby.addListener(new StandbyTracker());
        UsageStatsManagerInternal usmi = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        usmi.registerListener(new UsageEventTracker());
        PowerAllowlistInternal pai = (PowerAllowlistInternal) LocalServices.getService(PowerAllowlistInternal.class);
        pai.registerTempAllowlistChangeListener(new TempAllowlistTracker());
        try {
            ActivityManager.getService().registerUidObserver(new QcUidObserver(), 1, 4, (String) null);
            ActivityManager.getService().registerUidObserver(new QcUidObserver(), 1, 2, (String) null);
        } catch (RemoteException e) {
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void onSystemServicesReady() {
        synchronized (this.mLock) {
            cacheInstallerPackagesLocked(0);
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void maybeStartTrackingJobLocked(JobStatus jobStatus, JobStatus lastJob) {
        ArraySet<JobStatus> jobs;
        boolean outOfEJQuota;
        long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        int userId = jobStatus.getSourceUserId();
        String pkgName = jobStatus.getSourcePackageName();
        ArraySet<JobStatus> jobs2 = (ArraySet) this.mTrackedJobs.get(userId, pkgName);
        if (jobs2 != null) {
            jobs = jobs2;
        } else {
            ArraySet<JobStatus> jobs3 = new ArraySet<>();
            this.mTrackedJobs.add(userId, pkgName, jobs3);
            jobs = jobs3;
        }
        jobs.add(jobStatus);
        jobStatus.setTrackingController(64);
        boolean isWithinQuota = isWithinQuotaLocked(jobStatus);
        boolean isWithinEJQuota = jobStatus.isRequestedExpeditedJob() && isWithinEJQuotaLocked(jobStatus);
        setConstraintSatisfied(jobStatus, nowElapsed, isWithinQuota, isWithinEJQuota);
        if (jobStatus.isRequestedExpeditedJob()) {
            setExpeditedQuotaApproved(jobStatus, nowElapsed, isWithinEJQuota);
            outOfEJQuota = isWithinEJQuota ? false : true;
        } else {
            outOfEJQuota = false;
        }
        if (!isWithinQuota || outOfEJQuota) {
            maybeScheduleStartAlarmLocked(userId, pkgName, jobStatus.getEffectiveStandbyBucket());
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void prepareForExecutionLocked(JobStatus jobStatus) {
        boolean z = DEBUG;
        if (z) {
            Slog.d(TAG, "Prepping for " + jobStatus.toShortString());
        }
        int uid = jobStatus.getSourceUid();
        if (this.mTopAppCache.get(uid)) {
            if (z) {
                Slog.d(TAG, jobStatus.toShortString() + " is top started job");
            }
            this.mTopStartedJobs.add(jobStatus);
            return;
        }
        int userId = jobStatus.getSourceUserId();
        String packageName = jobStatus.getSourcePackageName();
        SparseArrayMap<String, Timer> timerMap = jobStatus.shouldTreatAsExpeditedJob() ? this.mEJPkgTimers : this.mPkgTimers;
        Timer timer = (Timer) timerMap.get(userId, packageName);
        if (timer == null) {
            timer = new Timer(uid, userId, packageName, !jobStatus.shouldTreatAsExpeditedJob());
            timerMap.add(userId, packageName, timer);
        }
        timer.startTrackingJobLocked(jobStatus);
    }

    @Override // com.android.server.job.controllers.StateController
    public void unprepareFromExecutionLocked(JobStatus jobStatus) {
        Timer timer;
        Timer timer2 = (Timer) this.mPkgTimers.get(jobStatus.getSourceUserId(), jobStatus.getSourcePackageName());
        if (timer2 != null) {
            timer2.stopTrackingJob(jobStatus);
        }
        if (jobStatus.isRequestedExpeditedJob() && (timer = (Timer) this.mEJPkgTimers.get(jobStatus.getSourceUserId(), jobStatus.getSourcePackageName())) != null) {
            timer.stopTrackingJob(jobStatus);
        }
        this.mTopStartedJobs.remove(jobStatus);
    }

    @Override // com.android.server.job.controllers.StateController
    public void maybeStopTrackingJobLocked(JobStatus jobStatus, JobStatus incomingJob, boolean forUpdate) {
        if (jobStatus.clearTrackingController(64)) {
            unprepareFromExecutionLocked(jobStatus);
            int userId = jobStatus.getSourceUserId();
            String pkgName = jobStatus.getSourcePackageName();
            ArraySet<JobStatus> jobs = (ArraySet) this.mTrackedJobs.get(userId, pkgName);
            if (jobs != null && jobs.remove(jobStatus) && jobs.size() == 0) {
                this.mInQuotaAlarmQueue.removeAlarmForKey(new Package(userId, pkgName));
            }
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void onAppRemovedLocked(String packageName, int uid) {
        if (packageName == null) {
            Slog.wtf(TAG, "Told app removed but given null package name.");
            return;
        }
        clearAppStatsLocked(UserHandle.getUserId(uid), packageName);
        if (this.mService.getPackagesForUidLocked(uid) == null) {
            this.mForegroundUids.delete(uid);
            this.mTempAllowlistCache.delete(uid);
            this.mTempAllowlistGraceCache.delete(uid);
            this.mTopAppCache.delete(uid);
            this.mTopAppGraceCache.delete(uid);
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void onUserAddedLocked(int userId) {
        cacheInstallerPackagesLocked(userId);
    }

    @Override // com.android.server.job.controllers.StateController
    public void onUserRemovedLocked(int userId) {
        this.mTrackedJobs.delete(userId);
        this.mPkgTimers.delete(userId);
        this.mEJPkgTimers.delete(userId);
        this.mTimingEvents.delete(userId);
        this.mEJTimingSessions.delete(userId);
        this.mInQuotaAlarmQueue.removeAlarmsForUserId(userId);
        this.mExecutionStatsCache.delete(userId);
        this.mEJStats.delete(userId);
        this.mSystemInstallers.remove(userId);
        this.mTopAppTrackers.delete(userId);
    }

    @Override // com.android.server.job.controllers.StateController
    public void onBatteryStateChangedLocked() {
        handleNewChargingStateLocked();
    }

    public void clearAppStatsLocked(int userId, String packageName) {
        this.mTrackedJobs.delete(userId, packageName);
        Timer timer = (Timer) this.mPkgTimers.delete(userId, packageName);
        if (timer != null && timer.isActive()) {
            Slog.e(TAG, "clearAppStats called before Timer turned off.");
            timer.dropEverythingLocked();
        }
        Timer timer2 = (Timer) this.mEJPkgTimers.delete(userId, packageName);
        if (timer2 != null && timer2.isActive()) {
            Slog.e(TAG, "clearAppStats called before EJ Timer turned off.");
            timer2.dropEverythingLocked();
        }
        this.mTimingEvents.delete(userId, packageName);
        this.mEJTimingSessions.delete(userId, packageName);
        this.mInQuotaAlarmQueue.removeAlarmForKey(new Package(userId, packageName));
        this.mExecutionStatsCache.delete(userId, packageName);
        this.mEJStats.delete(userId, packageName);
        this.mTopAppTrackers.delete(userId, packageName);
    }

    private void cacheInstallerPackagesLocked(int userId) {
        List<PackageInfo> packages = this.mContext.getPackageManager().getInstalledPackagesAsUser(SYSTEM_APP_CHECK_FLAGS, userId);
        for (int i = packages.size() - 1; i >= 0; i--) {
            PackageInfo pi = packages.get(i);
            ApplicationInfo ai = pi.applicationInfo;
            int idx = ArrayUtils.indexOf(pi.requestedPermissions, "android.permission.INSTALL_PACKAGES");
            if (idx >= 0 && ai != null && this.mContext.checkPermission("android.permission.INSTALL_PACKAGES", -1, ai.uid) == 0) {
                this.mSystemInstallers.add(UserHandle.getUserId(ai.uid), pi.packageName);
            }
        }
    }

    private boolean isUidInForeground(int uid) {
        boolean z;
        if (UserHandle.isCore(uid)) {
            return true;
        }
        synchronized (this.mLock) {
            z = this.mForegroundUids.get(uid);
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isTopStartedJobLocked(JobStatus jobStatus) {
        return this.mTopStartedJobs.contains(jobStatus);
    }

    public long getMaxJobExecutionTimeMsLocked(JobStatus jobStatus) {
        if (!jobStatus.shouldTreatAsExpeditedJob()) {
            if (this.mService.isBatteryCharging() || this.mTopAppCache.get(jobStatus.getSourceUid()) || isTopStartedJobLocked(jobStatus) || isUidInForeground(jobStatus.getSourceUid())) {
                return this.mConstants.RUNTIME_FREE_QUOTA_MAX_LIMIT_MS;
            }
            return getTimeUntilQuotaConsumedLocked(jobStatus.getSourceUserId(), jobStatus.getSourcePackageName());
        } else if (this.mService.isBatteryCharging()) {
            return this.mConstants.RUNTIME_FREE_QUOTA_MAX_LIMIT_MS;
        } else {
            if (jobStatus.getEffectiveStandbyBucket() == 6) {
                return Math.max(this.mEJLimitsMs[6] / 2, getTimeUntilEJQuotaConsumedLocked(jobStatus.getSourceUserId(), jobStatus.getSourcePackageName()));
            }
            if (this.mTopAppCache.get(jobStatus.getSourceUid()) || isTopStartedJobLocked(jobStatus)) {
                return Math.max(this.mEJLimitsMs[0] / 2, getTimeUntilEJQuotaConsumedLocked(jobStatus.getSourceUserId(), jobStatus.getSourcePackageName()));
            }
            if (isUidInForeground(jobStatus.getSourceUid())) {
                return Math.max(this.mEJLimitsMs[1] / 2, getTimeUntilEJQuotaConsumedLocked(jobStatus.getSourceUserId(), jobStatus.getSourcePackageName()));
            }
            return getTimeUntilEJQuotaConsumedLocked(jobStatus.getSourceUserId(), jobStatus.getSourcePackageName());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean hasTempAllowlistExemptionLocked(int sourceUid, int standbyBucket, long nowElapsed) {
        if (standbyBucket == 5 || standbyBucket == 4) {
            return false;
        }
        long tempAllowlistGracePeriodEndElapsed = this.mTempAllowlistGraceCache.get(sourceUid);
        return this.mTempAllowlistCache.get(sourceUid) || nowElapsed < tempAllowlistGracePeriodEndElapsed;
    }

    public boolean isWithinEJQuotaLocked(JobStatus jobStatus) {
        if (!this.mIsEnabled || isQuotaFreeLocked(jobStatus.getEffectiveStandbyBucket()) || isTopStartedJobLocked(jobStatus) || isUidInForeground(jobStatus.getSourceUid())) {
            return true;
        }
        long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        if (hasTempAllowlistExemptionLocked(jobStatus.getSourceUid(), jobStatus.getEffectiveStandbyBucket(), nowElapsed)) {
            return true;
        }
        long topAppGracePeriodEndElapsed = this.mTopAppGraceCache.get(jobStatus.getSourceUid());
        boolean hasTopAppExemption = this.mTopAppCache.get(jobStatus.getSourceUid()) || nowElapsed < topAppGracePeriodEndElapsed;
        return hasTopAppExemption || 0 < getRemainingEJExecutionTimeLocked(jobStatus.getSourceUserId(), jobStatus.getSourcePackageName());
    }

    ShrinkableDebits getEJDebitsLocked(int userId, String packageName) {
        ShrinkableDebits debits = (ShrinkableDebits) this.mEJStats.get(userId, packageName);
        if (debits == null) {
            ShrinkableDebits debits2 = new ShrinkableDebits(JobSchedulerService.standbyBucketForPackage(packageName, userId, JobSchedulerService.sElapsedRealtimeClock.millis()));
            this.mEJStats.add(userId, packageName, debits2);
            return debits2;
        }
        return debits;
    }

    boolean isWithinQuotaLocked(JobStatus jobStatus) {
        if (this.mIsEnabled) {
            int standbyBucket = jobStatus.getEffectiveStandbyBucket();
            return isTopStartedJobLocked(jobStatus) || isUidInForeground(jobStatus.getSourceUid()) || isWithinQuotaLocked(jobStatus.getSourceUserId(), jobStatus.getSourcePackageName(), standbyBucket);
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isQuotaFreeLocked(int standbyBucket) {
        return this.mService.isBatteryCharging() && standbyBucket != 5;
    }

    boolean isWithinQuotaLocked(int userId, String packageName, int standbyBucket) {
        if (this.mIsEnabled) {
            if (standbyBucket == 4) {
                return false;
            }
            if (isQuotaFreeLocked(standbyBucket)) {
                return true;
            }
            ExecutionStats stats = getExecutionStatsLocked(userId, packageName, standbyBucket);
            return getRemainingExecutionTimeLocked(stats) > 0 && isUnderJobCountQuotaLocked(stats, standbyBucket) && isUnderSessionCountQuotaLocked(stats, standbyBucket);
        }
        return true;
    }

    private boolean isUnderJobCountQuotaLocked(ExecutionStats stats, int standbyBucket) {
        long now = JobSchedulerService.sElapsedRealtimeClock.millis();
        boolean isUnderAllowedTimeQuota = stats.jobRateLimitExpirationTimeElapsed <= now || stats.jobCountInRateLimitingWindow < this.mMaxJobCountPerRateLimitingWindow;
        return isUnderAllowedTimeQuota && stats.bgJobCountInWindow < stats.jobCountLimit;
    }

    private boolean isUnderSessionCountQuotaLocked(ExecutionStats stats, int standbyBucket) {
        long now = JobSchedulerService.sElapsedRealtimeClock.millis();
        boolean isUnderAllowedTimeQuota = stats.sessionRateLimitExpirationTimeElapsed <= now || stats.sessionCountInRateLimitingWindow < this.mMaxSessionCountPerRateLimitingWindow;
        return isUnderAllowedTimeQuota && stats.sessionCountInWindow < stats.sessionCountLimit;
    }

    long getRemainingExecutionTimeLocked(JobStatus jobStatus) {
        return getRemainingExecutionTimeLocked(jobStatus.getSourceUserId(), jobStatus.getSourcePackageName(), jobStatus.getEffectiveStandbyBucket());
    }

    long getRemainingExecutionTimeLocked(int userId, String packageName) {
        int standbyBucket = JobSchedulerService.standbyBucketForPackage(packageName, userId, JobSchedulerService.sElapsedRealtimeClock.millis());
        return getRemainingExecutionTimeLocked(userId, packageName, standbyBucket);
    }

    private long getRemainingExecutionTimeLocked(int userId, String packageName, int standbyBucket) {
        if (standbyBucket == 4) {
            return 0L;
        }
        return getRemainingExecutionTimeLocked(getExecutionStatsLocked(userId, packageName, standbyBucket));
    }

    private long getRemainingExecutionTimeLocked(ExecutionStats stats) {
        return Math.min(stats.allowedTimePerPeriodMs - stats.executionTimeInWindowMs, this.mMaxExecutionTimeMs - stats.executionTimeInMaxPeriodMs);
    }

    long getRemainingEJExecutionTimeLocked(int userId, String packageName) {
        long nowElapsed;
        ShrinkableDebits quota = getEJDebitsLocked(userId, packageName);
        if (quota.getStandbyBucketLocked() != 4) {
            long limitMs = getEJLimitMsLocked(userId, packageName, quota.getStandbyBucketLocked());
            long remainingMs = limitMs - quota.getTallyLocked();
            List<TimedEvent> timingSessions = (List) this.mEJTimingSessions.get(userId, packageName);
            long nowElapsed2 = JobSchedulerService.sElapsedRealtimeClock.millis();
            long windowStartTimeElapsed = nowElapsed2 - this.mEJLimitWindowSizeMs;
            if (timingSessions != null) {
                while (true) {
                    if (timingSessions.size() <= 0) {
                        nowElapsed = nowElapsed2;
                        break;
                    }
                    TimingSession ts = (TimingSession) timingSessions.get(0);
                    long limitMs2 = limitMs;
                    if (ts.endTimeElapsed < windowStartTimeElapsed) {
                        long duration = ts.endTimeElapsed - ts.startTimeElapsed;
                        remainingMs += duration;
                        quota.transactLocked(-duration);
                        timingSessions.remove(0);
                        limitMs = limitMs2;
                        nowElapsed2 = nowElapsed2;
                    } else {
                        nowElapsed = nowElapsed2;
                        if (ts.startTimeElapsed < windowStartTimeElapsed) {
                            remainingMs += windowStartTimeElapsed - ts.startTimeElapsed;
                        }
                    }
                }
            } else {
                nowElapsed = nowElapsed2;
            }
            TopAppTimer topAppTimer = (TopAppTimer) this.mTopAppTrackers.get(userId, packageName);
            if (topAppTimer != null && topAppTimer.isActive()) {
                remainingMs += topAppTimer.getPendingReward(nowElapsed);
            }
            Timer timer = (Timer) this.mEJPkgTimers.get(userId, packageName);
            if (timer == null) {
                return remainingMs;
            }
            return remainingMs - timer.getCurrentDuration(JobSchedulerService.sElapsedRealtimeClock.millis());
        }
        return 0L;
    }

    private long getEJLimitMsLocked(int userId, String packageName, int standbyBucket) {
        long baseLimitMs = this.mEJLimitsMs[standbyBucket];
        if (this.mSystemInstallers.contains(userId, packageName)) {
            return this.mEjLimitAdditionInstallerMs + baseLimitMs;
        }
        return baseLimitMs;
    }

    long getTimeUntilQuotaConsumedLocked(int userId, String packageName) {
        long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        int standbyBucket = JobSchedulerService.standbyBucketForPackage(packageName, userId, nowElapsed);
        if (standbyBucket == 4) {
            return 0L;
        }
        List<TimedEvent> events = (List) this.mTimingEvents.get(userId, packageName);
        ExecutionStats stats = getExecutionStatsLocked(userId, packageName, standbyBucket);
        if (events == null || events.size() == 0) {
            long j = stats.windowSizeMs;
            long j2 = this.mAllowedTimePerPeriodMs[standbyBucket];
            if (j == j2) {
                return this.mMaxExecutionTimeMs;
            }
            return j2;
        }
        long startWindowElapsed = nowElapsed - stats.windowSizeMs;
        long startMaxElapsed = nowElapsed - 86400000;
        long allowedTimePerPeriodMs = this.mAllowedTimePerPeriodMs[standbyBucket];
        long allowedTimeRemainingMs = allowedTimePerPeriodMs - stats.executionTimeInWindowMs;
        long maxExecutionTimeRemainingMs = this.mMaxExecutionTimeMs - stats.executionTimeInMaxPeriodMs;
        if (stats.windowSizeMs == this.mAllowedTimePerPeriodMs[standbyBucket]) {
            return calculateTimeUntilQuotaConsumedLocked(events, startMaxElapsed, maxExecutionTimeRemainingMs, false);
        }
        return Math.min(calculateTimeUntilQuotaConsumedLocked(events, startMaxElapsed, maxExecutionTimeRemainingMs, false), calculateTimeUntilQuotaConsumedLocked(events, startWindowElapsed, allowedTimeRemainingMs, true));
    }

    private long calculateTimeUntilQuotaConsumedLocked(List<TimedEvent> sessions, long windowStartElapsed, long deadSpaceMs, boolean allowQuotaBumps) {
        long timeUntilQuotaConsumedMs;
        long deadSpaceMs2;
        int numQuotaBumps;
        long timeUntilQuotaConsumedMs2;
        long timeUntilQuotaConsumedMs3 = 0;
        long start = windowStartElapsed;
        int numQuotaBumps2 = 0;
        long quotaBumpWindowStartElapsed = JobSchedulerService.sElapsedRealtimeClock.millis() - this.mQuotaBumpWindowSizeMs;
        int numSessions = sessions.size();
        if (!allowQuotaBumps) {
            timeUntilQuotaConsumedMs = 0;
            deadSpaceMs2 = deadSpaceMs;
        } else {
            int i = numSessions - 1;
            deadSpaceMs2 = deadSpaceMs;
            while (true) {
                if (i < 0) {
                    timeUntilQuotaConsumedMs = timeUntilQuotaConsumedMs3;
                    break;
                }
                TimedEvent event = sessions.get(i);
                if (!(event instanceof QuotaBump)) {
                    timeUntilQuotaConsumedMs2 = timeUntilQuotaConsumedMs3;
                } else if (event.getEndTimeElapsed() < quotaBumpWindowStartElapsed) {
                    timeUntilQuotaConsumedMs = timeUntilQuotaConsumedMs3;
                    break;
                } else {
                    int numQuotaBumps3 = numQuotaBumps2 + 1;
                    if (numQuotaBumps2 >= this.mQuotaBumpLimit) {
                        timeUntilQuotaConsumedMs = timeUntilQuotaConsumedMs3;
                        numQuotaBumps2 = numQuotaBumps3;
                        break;
                    }
                    timeUntilQuotaConsumedMs2 = timeUntilQuotaConsumedMs3;
                    long timeUntilQuotaConsumedMs4 = this.mQuotaBumpAdditionalDurationMs;
                    deadSpaceMs2 += timeUntilQuotaConsumedMs4;
                    numQuotaBumps2 = numQuotaBumps3;
                }
                i--;
                timeUntilQuotaConsumedMs3 = timeUntilQuotaConsumedMs2;
            }
        }
        int i2 = 0;
        while (i2 < numSessions) {
            TimedEvent event2 = sessions.get(i2);
            if (!(event2 instanceof QuotaBump)) {
                TimingSession session = (TimingSession) event2;
                if (session.endTimeElapsed >= windowStartElapsed) {
                    if (session.startTimeElapsed <= windowStartElapsed) {
                        timeUntilQuotaConsumedMs += session.endTimeElapsed - windowStartElapsed;
                        start = session.endTimeElapsed;
                        numQuotaBumps = numQuotaBumps2;
                    } else {
                        long diff = session.startTimeElapsed - start;
                        if (diff > deadSpaceMs2) {
                            break;
                        }
                        numQuotaBumps = numQuotaBumps2;
                        timeUntilQuotaConsumedMs += (session.endTimeElapsed - session.startTimeElapsed) + diff;
                        deadSpaceMs2 -= diff;
                        start = session.endTimeElapsed;
                    }
                    i2++;
                    numQuotaBumps2 = numQuotaBumps;
                }
            }
            numQuotaBumps = numQuotaBumps2;
            i2++;
            numQuotaBumps2 = numQuotaBumps;
        }
        long timeUntilQuotaConsumedMs5 = timeUntilQuotaConsumedMs + deadSpaceMs2;
        if (timeUntilQuotaConsumedMs5 > this.mMaxExecutionTimeMs) {
            Slog.wtf(TAG, "Calculated quota consumed time too high: " + timeUntilQuotaConsumedMs5);
        }
        return timeUntilQuotaConsumedMs5;
    }

    long getTimeUntilEJQuotaConsumedLocked(int userId, String packageName) {
        long nowElapsed;
        long startWindowElapsed;
        long remainingExecutionTimeMs = getRemainingEJExecutionTimeLocked(userId, packageName);
        List<TimedEvent> sessions = (List) this.mEJTimingSessions.get(userId, packageName);
        if (sessions == null || sessions.size() == 0) {
            return remainingExecutionTimeMs;
        }
        long nowElapsed2 = JobSchedulerService.sElapsedRealtimeClock.millis();
        ShrinkableDebits quota = getEJDebitsLocked(userId, packageName);
        long limitMs = getEJLimitMsLocked(userId, packageName, quota.getStandbyBucketLocked());
        long startWindowElapsed2 = Math.max(0L, nowElapsed2 - this.mEJLimitWindowSizeMs);
        long deadSpaceMs = 0;
        long phasedOutSessionTimeMs = 0;
        long remainingDeadSpaceMs = remainingExecutionTimeMs;
        int i = 0;
        while (i < sessions.size()) {
            TimingSession session = (TimingSession) sessions.get(i);
            if (session.endTimeElapsed < startWindowElapsed2) {
                nowElapsed = nowElapsed2;
                remainingDeadSpaceMs += session.endTimeElapsed - session.startTimeElapsed;
                sessions.remove(i);
                i--;
                startWindowElapsed = startWindowElapsed2;
            } else {
                nowElapsed = nowElapsed2;
                if (session.startTimeElapsed < startWindowElapsed2) {
                    phasedOutSessionTimeMs = session.endTimeElapsed - startWindowElapsed2;
                    startWindowElapsed = startWindowElapsed2;
                } else {
                    long phasedOutSessionTimeMs2 = session.startTimeElapsed;
                    long timeBetweenSessions = phasedOutSessionTimeMs2 - (i == 0 ? startWindowElapsed2 : sessions.get(i - 1).getEndTimeElapsed());
                    long usedDeadSpaceMs = Math.min(remainingDeadSpaceMs, timeBetweenSessions);
                    deadSpaceMs += usedDeadSpaceMs;
                    if (usedDeadSpaceMs == timeBetweenSessions) {
                        long timeBetweenSessions2 = session.endTimeElapsed;
                        startWindowElapsed = startWindowElapsed2;
                        long startWindowElapsed3 = session.startTimeElapsed;
                        phasedOutSessionTimeMs += timeBetweenSessions2 - startWindowElapsed3;
                    } else {
                        startWindowElapsed = startWindowElapsed2;
                    }
                    remainingDeadSpaceMs -= usedDeadSpaceMs;
                    if (remainingDeadSpaceMs <= 0) {
                        break;
                    }
                }
            }
            i++;
            nowElapsed2 = nowElapsed;
            startWindowElapsed2 = startWindowElapsed;
        }
        return Math.min(limitMs, deadSpaceMs + phasedOutSessionTimeMs + remainingDeadSpaceMs);
    }

    ExecutionStats getExecutionStatsLocked(int userId, String packageName, int standbyBucket) {
        return getExecutionStatsLocked(userId, packageName, standbyBucket, true);
    }

    private ExecutionStats getExecutionStatsLocked(int userId, String packageName, int standbyBucket, boolean refreshStatsIfOld) {
        if (standbyBucket == 4) {
            Slog.wtf(TAG, "getExecutionStatsLocked called for a NEVER app.");
            return new ExecutionStats();
        }
        ExecutionStats[] appStats = (ExecutionStats[]) this.mExecutionStatsCache.get(userId, packageName);
        if (appStats == null) {
            appStats = new ExecutionStats[this.mBucketPeriodsMs.length];
            this.mExecutionStatsCache.add(userId, packageName, appStats);
        }
        ExecutionStats stats = appStats[standbyBucket];
        if (stats == null) {
            stats = new ExecutionStats();
            appStats[standbyBucket] = stats;
        }
        if (refreshStatsIfOld) {
            long bucketAllowedTimeMs = this.mAllowedTimePerPeriodMs[standbyBucket];
            long bucketWindowSizeMs = this.mBucketPeriodsMs[standbyBucket];
            int jobCountLimit = this.mMaxBucketJobCounts[standbyBucket];
            int sessionCountLimit = this.mMaxBucketSessionCounts[standbyBucket];
            Timer timer = (Timer) this.mPkgTimers.get(userId, packageName);
            if ((timer != null && timer.isActive()) || stats.expirationTimeElapsed <= JobSchedulerService.sElapsedRealtimeClock.millis() || stats.allowedTimePerPeriodMs != bucketAllowedTimeMs || stats.windowSizeMs != bucketWindowSizeMs || stats.jobCountLimit != jobCountLimit || stats.sessionCountLimit != sessionCountLimit) {
                stats.allowedTimePerPeriodMs = bucketAllowedTimeMs;
                stats.windowSizeMs = bucketWindowSizeMs;
                stats.jobCountLimit = jobCountLimit;
                stats.sessionCountLimit = sessionCountLimit;
                updateExecutionStatsLocked(userId, packageName, stats);
            }
        }
        return stats;
    }

    void updateExecutionStatsLocked(int userId, String packageName, ExecutionStats stats) {
        long allowedTimeIntoQuotaMs;
        int sessionCountInWindow;
        long startMaxElapsed;
        int i;
        List<TimedEvent> events;
        long quotaBumpWindowStartElapsed;
        long emptyTimeMs;
        long start;
        long emptyTimeMs2;
        long allowedTimeIntoQuotaMs2;
        stats.executionTimeInWindowMs = 0L;
        stats.bgJobCountInWindow = 0;
        stats.executionTimeInMaxPeriodMs = 0L;
        stats.bgJobCountInMaxPeriod = 0;
        stats.sessionCountInWindow = 0;
        if (stats.jobCountLimit == 0 || stats.sessionCountLimit == 0) {
            stats.inQuotaTimeElapsed = JobStatus.NO_LATEST_RUNTIME;
        } else {
            stats.inQuotaTimeElapsed = 0L;
        }
        long allowedTimeIntoQuotaMs3 = stats.allowedTimePerPeriodMs - this.mQuotaBufferMs;
        Timer timer = (Timer) this.mPkgTimers.get(userId, packageName);
        long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        stats.expirationTimeElapsed = nowElapsed + 86400000;
        if (timer != null && timer.isActive()) {
            long currentDuration = timer.getCurrentDuration(nowElapsed);
            stats.executionTimeInMaxPeriodMs = currentDuration;
            stats.executionTimeInWindowMs = currentDuration;
            int bgJobCount = timer.getBgJobCount();
            stats.bgJobCountInMaxPeriod = bgJobCount;
            stats.bgJobCountInWindow = bgJobCount;
            stats.expirationTimeElapsed = nowElapsed;
            if (stats.executionTimeInWindowMs >= allowedTimeIntoQuotaMs3) {
                stats.inQuotaTimeElapsed = Math.max(stats.inQuotaTimeElapsed, (nowElapsed - allowedTimeIntoQuotaMs3) + stats.windowSizeMs);
            }
            long j = stats.executionTimeInMaxPeriodMs;
            long j2 = this.mMaxExecutionTimeIntoQuotaMs;
            if (j >= j2) {
                long inQuotaTime = (nowElapsed - j2) + 86400000;
                stats.inQuotaTimeElapsed = Math.max(stats.inQuotaTimeElapsed, inQuotaTime);
            }
            if (stats.bgJobCountInWindow >= stats.jobCountLimit) {
                long inQuotaTime2 = stats.windowSizeMs + nowElapsed;
                stats.inQuotaTimeElapsed = Math.max(stats.inQuotaTimeElapsed, inQuotaTime2);
            }
        }
        List<TimedEvent> events2 = (List) this.mTimingEvents.get(userId, packageName);
        if (events2 != null && events2.size() != 0) {
            long startWindowElapsed = nowElapsed - stats.windowSizeMs;
            long startMaxElapsed2 = nowElapsed - 86400000;
            long quotaBumpWindowStartElapsed2 = nowElapsed - this.mQuotaBumpWindowSizeMs;
            int loopStart = events2.size() - 1;
            int numQuotaBumps = 0;
            int i2 = loopStart;
            int sessionCountInWindow2 = 0;
            long emptyTimeMs3 = Long.MAX_VALUE;
            while (true) {
                if (i2 < 0) {
                    allowedTimeIntoQuotaMs = allowedTimeIntoQuotaMs3;
                    sessionCountInWindow = sessionCountInWindow2;
                    startMaxElapsed = startMaxElapsed2;
                    break;
                }
                sessionCountInWindow = sessionCountInWindow2;
                TimedEvent event = events2.get(i2);
                if (event.getEndTimeElapsed() >= quotaBumpWindowStartElapsed2) {
                    startMaxElapsed = startMaxElapsed2;
                    if (numQuotaBumps >= this.mQuotaBumpLimit) {
                        allowedTimeIntoQuotaMs = allowedTimeIntoQuotaMs3;
                        break;
                    }
                    if (event instanceof QuotaBump) {
                        allowedTimeIntoQuotaMs2 = allowedTimeIntoQuotaMs3;
                        stats.allowedTimePerPeriodMs += this.mQuotaBumpAdditionalDurationMs;
                        stats.jobCountLimit += this.mQuotaBumpAdditionalJobCount;
                        stats.sessionCountLimit += this.mQuotaBumpAdditionalSessionCount;
                        numQuotaBumps++;
                        emptyTimeMs3 = Math.min(emptyTimeMs3, event.getEndTimeElapsed() - quotaBumpWindowStartElapsed2);
                    } else {
                        allowedTimeIntoQuotaMs2 = allowedTimeIntoQuotaMs3;
                    }
                    i2--;
                    sessionCountInWindow2 = sessionCountInWindow;
                    startMaxElapsed2 = startMaxElapsed;
                    allowedTimeIntoQuotaMs3 = allowedTimeIntoQuotaMs2;
                } else {
                    allowedTimeIntoQuotaMs = allowedTimeIntoQuotaMs3;
                    startMaxElapsed = startMaxElapsed2;
                    break;
                }
            }
            TimingSession lastSeenTimingSession = null;
            int i3 = loopStart;
            while (true) {
                if (i3 < 0) {
                    i = sessionCountInWindow;
                    break;
                }
                TimedEvent event2 = events2.get(i3);
                if (event2 instanceof QuotaBump) {
                    events = events2;
                    quotaBumpWindowStartElapsed = quotaBumpWindowStartElapsed2;
                } else {
                    TimingSession session = (TimingSession) event2;
                    if (startWindowElapsed < session.endTimeElapsed) {
                        if (startWindowElapsed < session.startTimeElapsed) {
                            start = session.startTimeElapsed;
                            events = events2;
                            emptyTimeMs2 = Math.min(emptyTimeMs3, session.startTimeElapsed - startWindowElapsed);
                        } else {
                            events = events2;
                            start = startWindowElapsed;
                            emptyTimeMs2 = 0;
                        }
                        long emptyTimeMs4 = stats.executionTimeInWindowMs;
                        long emptyTimeMs5 = emptyTimeMs2;
                        long emptyTimeMs6 = session.endTimeElapsed;
                        stats.executionTimeInWindowMs = emptyTimeMs4 + (emptyTimeMs6 - start);
                        stats.bgJobCountInWindow += session.bgJobCount;
                        if (stats.executionTimeInWindowMs < allowedTimeIntoQuotaMs) {
                            quotaBumpWindowStartElapsed = quotaBumpWindowStartElapsed2;
                        } else {
                            quotaBumpWindowStartElapsed = quotaBumpWindowStartElapsed2;
                            stats.inQuotaTimeElapsed = Math.max(stats.inQuotaTimeElapsed, ((stats.executionTimeInWindowMs + start) - allowedTimeIntoQuotaMs) + stats.windowSizeMs);
                        }
                        if (stats.bgJobCountInWindow >= stats.jobCountLimit) {
                            long inQuotaTime3 = session.endTimeElapsed + stats.windowSizeMs;
                            stats.inQuotaTimeElapsed = Math.max(stats.inQuotaTimeElapsed, inQuotaTime3);
                        }
                        boolean shouldCoalesce = lastSeenTimingSession != null && lastSeenTimingSession.startTimeElapsed - session.endTimeElapsed <= this.mTimingSessionCoalescingDurationMs;
                        if (shouldCoalesce) {
                            emptyTimeMs3 = emptyTimeMs5;
                        } else {
                            int sessionCountInWindow3 = sessionCountInWindow + 1;
                            if (sessionCountInWindow3 >= stats.sessionCountLimit) {
                                long inQuotaTime4 = session.endTimeElapsed + stats.windowSizeMs;
                                stats.inQuotaTimeElapsed = Math.max(stats.inQuotaTimeElapsed, inQuotaTime4);
                            }
                            sessionCountInWindow = sessionCountInWindow3;
                            emptyTimeMs3 = emptyTimeMs5;
                        }
                    } else {
                        events = events2;
                        quotaBumpWindowStartElapsed = quotaBumpWindowStartElapsed2;
                    }
                    if (startMaxElapsed < session.startTimeElapsed) {
                        stats.executionTimeInMaxPeriodMs += session.endTimeElapsed - session.startTimeElapsed;
                        stats.bgJobCountInMaxPeriod += session.bgJobCount;
                        emptyTimeMs = Math.min(emptyTimeMs3, session.startTimeElapsed - startMaxElapsed);
                        long emptyTimeMs7 = stats.executionTimeInMaxPeriodMs;
                        if (emptyTimeMs7 >= this.mMaxExecutionTimeIntoQuotaMs) {
                            stats.inQuotaTimeElapsed = Math.max(stats.inQuotaTimeElapsed, ((session.startTimeElapsed + stats.executionTimeInMaxPeriodMs) - this.mMaxExecutionTimeIntoQuotaMs) + 86400000);
                        }
                    } else {
                        long emptyTimeMs8 = session.endTimeElapsed;
                        if (startMaxElapsed >= emptyTimeMs8) {
                            i = sessionCountInWindow;
                            break;
                        }
                        stats.executionTimeInMaxPeriodMs += session.endTimeElapsed - startMaxElapsed;
                        stats.bgJobCountInMaxPeriod += session.bgJobCount;
                        emptyTimeMs = 0;
                        long emptyTimeMs9 = stats.executionTimeInMaxPeriodMs;
                        if (emptyTimeMs9 >= this.mMaxExecutionTimeIntoQuotaMs) {
                            stats.inQuotaTimeElapsed = Math.max(stats.inQuotaTimeElapsed, ((startMaxElapsed + stats.executionTimeInMaxPeriodMs) - this.mMaxExecutionTimeIntoQuotaMs) + 86400000);
                        }
                    }
                    lastSeenTimingSession = session;
                    emptyTimeMs3 = emptyTimeMs;
                }
                i3--;
                events2 = events;
                quotaBumpWindowStartElapsed2 = quotaBumpWindowStartElapsed;
            }
            stats.expirationTimeElapsed = nowElapsed + emptyTimeMs3;
            stats.sessionCountInWindow = i;
        }
    }

    void invalidateAllExecutionStatsLocked() {
        final long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        this.mExecutionStatsCache.forEach(new Consumer() { // from class: com.android.server.job.controllers.QuotaController$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                QuotaController.lambda$invalidateAllExecutionStatsLocked$0(nowElapsed, (QuotaController.ExecutionStats[]) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$invalidateAllExecutionStatsLocked$0(long nowElapsed, ExecutionStats[] appStats) {
        if (appStats != null) {
            for (ExecutionStats stats : appStats) {
                if (stats != null) {
                    stats.expirationTimeElapsed = nowElapsed;
                }
            }
        }
    }

    void invalidateAllExecutionStatsLocked(int userId, String packageName) {
        ExecutionStats[] appStats = (ExecutionStats[]) this.mExecutionStatsCache.get(userId, packageName);
        if (appStats != null) {
            long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
            for (ExecutionStats stats : appStats) {
                if (stats != null) {
                    stats.expirationTimeElapsed = nowElapsed;
                }
            }
        }
    }

    void incrementJobCountLocked(int userId, String packageName, int count) {
        long now = JobSchedulerService.sElapsedRealtimeClock.millis();
        ExecutionStats[] appStats = (ExecutionStats[]) this.mExecutionStatsCache.get(userId, packageName);
        if (appStats == null) {
            appStats = new ExecutionStats[this.mBucketPeriodsMs.length];
            this.mExecutionStatsCache.add(userId, packageName, appStats);
        }
        for (int i = 0; i < appStats.length; i++) {
            ExecutionStats stats = appStats[i];
            if (stats == null) {
                stats = new ExecutionStats();
                appStats[i] = stats;
            }
            if (stats.jobRateLimitExpirationTimeElapsed <= now) {
                stats.jobRateLimitExpirationTimeElapsed = this.mRateLimitingWindowMs + now;
                stats.jobCountInRateLimitingWindow = 0;
            }
            stats.jobCountInRateLimitingWindow += count;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void incrementTimingSessionCountLocked(int userId, String packageName) {
        long now = JobSchedulerService.sElapsedRealtimeClock.millis();
        ExecutionStats[] appStats = (ExecutionStats[]) this.mExecutionStatsCache.get(userId, packageName);
        if (appStats == null) {
            appStats = new ExecutionStats[this.mBucketPeriodsMs.length];
            this.mExecutionStatsCache.add(userId, packageName, appStats);
        }
        for (int i = 0; i < appStats.length; i++) {
            ExecutionStats stats = appStats[i];
            if (stats == null) {
                stats = new ExecutionStats();
                appStats[i] = stats;
            }
            if (stats.sessionRateLimitExpirationTimeElapsed <= now) {
                stats.sessionRateLimitExpirationTimeElapsed = this.mRateLimitingWindowMs + now;
                stats.sessionCountInRateLimitingWindow = 0;
            }
            stats.sessionCountInRateLimitingWindow++;
        }
    }

    void saveTimingSession(int userId, String packageName, TimingSession session, boolean isExpedited) {
        saveTimingSession(userId, packageName, session, isExpedited, 0L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void saveTimingSession(int userId, String packageName, TimingSession session, boolean isExpedited, long debitAdjustment) {
        synchronized (this.mLock) {
            SparseArrayMap<String, List<TimedEvent>> sessionMap = isExpedited ? this.mEJTimingSessions : this.mTimingEvents;
            List<TimedEvent> sessions = (List) sessionMap.get(userId, packageName);
            if (sessions == null) {
                sessions = new ArrayList<>();
                sessionMap.add(userId, packageName, sessions);
            }
            sessions.add(session);
            if (isExpedited) {
                ShrinkableDebits quota = getEJDebitsLocked(userId, packageName);
                quota.transactLocked((session.endTimeElapsed - session.startTimeElapsed) + debitAdjustment);
            } else {
                invalidateAllExecutionStatsLocked(userId, packageName);
                maybeScheduleCleanupAlarmLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void grantRewardForInstantEvent(int userId, String packageName, long credit) {
        if (credit == 0) {
            return;
        }
        synchronized (this.mLock) {
            long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
            ShrinkableDebits quota = getEJDebitsLocked(userId, packageName);
            if (transactQuotaLocked(userId, packageName, nowElapsed, quota, credit)) {
                this.mStateChangedListener.onControllerStateChanged(maybeUpdateConstraintForPkgLocked(nowElapsed, userId, packageName));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean transactQuotaLocked(int userId, String packageName, long nowElapsed, ShrinkableDebits debits, long credit) {
        Timer ejTimer;
        long oldTally = debits.getTallyLocked();
        long leftover = debits.transactLocked(-credit);
        if (DEBUG) {
            Slog.d(TAG, "debits overflowed by " + leftover);
        }
        boolean changed = oldTally != debits.getTallyLocked();
        if (leftover != 0 && (ejTimer = (Timer) this.mEJPkgTimers.get(userId, packageName)) != null && ejTimer.isActive()) {
            ejTimer.updateDebitAdjustment(nowElapsed, leftover);
            return true;
        }
        return changed;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class EarliestEndTimeFunctor implements Consumer<List<TimedEvent>> {
        public long earliestEndElapsed;

        private EarliestEndTimeFunctor() {
            this.earliestEndElapsed = JobStatus.NO_LATEST_RUNTIME;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Consumer
        public void accept(List<TimedEvent> events) {
            if (events != null && events.size() > 0) {
                this.earliestEndElapsed = Math.min(this.earliestEndElapsed, events.get(0).getEndTimeElapsed());
            }
        }

        void reset() {
            this.earliestEndElapsed = JobStatus.NO_LATEST_RUNTIME;
        }
    }

    void maybeScheduleCleanupAlarmLocked() {
        long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        if (this.mNextCleanupTimeElapsed > nowElapsed) {
            if (DEBUG) {
                Slog.v(TAG, "Not scheduling cleanup since there's already one at " + this.mNextCleanupTimeElapsed + " (in " + (this.mNextCleanupTimeElapsed - nowElapsed) + "ms)");
                return;
            }
            return;
        }
        this.mEarliestEndTimeFunctor.reset();
        this.mTimingEvents.forEach(this.mEarliestEndTimeFunctor);
        this.mEJTimingSessions.forEach(this.mEarliestEndTimeFunctor);
        long earliestEndElapsed = this.mEarliestEndTimeFunctor.earliestEndElapsed;
        if (earliestEndElapsed == JobStatus.NO_LATEST_RUNTIME) {
            if (DEBUG) {
                Slog.d(TAG, "Didn't find a time to schedule cleanup");
                return;
            }
            return;
        }
        long nextCleanupElapsed = 86400000 + earliestEndElapsed;
        long j = this.mNextCleanupTimeElapsed;
        if (nextCleanupElapsed - j <= 600000) {
            nextCleanupElapsed = j + 600000;
        }
        this.mNextCleanupTimeElapsed = nextCleanupElapsed;
        this.mAlarmManager.set(3, nextCleanupElapsed, ALARM_TAG_CLEANUP, this.mSessionCleanupAlarmListener, this.mHandler);
        if (DEBUG) {
            Slog.d(TAG, "Scheduled next cleanup for " + this.mNextCleanupTimeElapsed);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class TimerChargingUpdateFunctor implements Consumer<Timer> {
        private boolean mIsCharging;
        private long mNowElapsed;

        private TimerChargingUpdateFunctor() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setStatus(long nowElapsed, boolean isCharging) {
            this.mNowElapsed = nowElapsed;
            this.mIsCharging = isCharging;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Consumer
        public void accept(Timer timer) {
            if (JobSchedulerService.standbyBucketForPackage(timer.mPkg.packageName, timer.mPkg.userId, this.mNowElapsed) != 5) {
                timer.onStateChangedLocked(this.mNowElapsed, this.mIsCharging);
            }
        }
    }

    private void handleNewChargingStateLocked() {
        this.mTimerChargingUpdateFunctor.setStatus(JobSchedulerService.sElapsedRealtimeClock.millis(), this.mService.isBatteryCharging());
        if (DEBUG) {
            Slog.d(TAG, "handleNewChargingStateLocked: " + this.mService.isBatteryCharging());
        }
        this.mEJPkgTimers.forEach(this.mTimerChargingUpdateFunctor);
        this.mPkgTimers.forEach(this.mTimerChargingUpdateFunctor);
        JobSchedulerBackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.job.controllers.QuotaController$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                QuotaController.this.m4231x63169bb6();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleNewChargingStateLocked$1$com-android-server-job-controllers-QuotaController  reason: not valid java name */
    public /* synthetic */ void m4231x63169bb6() {
        synchronized (this.mLock) {
            maybeUpdateAllConstraintsLocked();
        }
    }

    private void maybeUpdateAllConstraintsLocked() {
        ArraySet<JobStatus> changedJobs = new ArraySet<>();
        long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        for (int u = 0; u < this.mTrackedJobs.numMaps(); u++) {
            int userId = this.mTrackedJobs.keyAt(u);
            for (int p = 0; p < this.mTrackedJobs.numElementsForKey(userId); p++) {
                String packageName = (String) this.mTrackedJobs.keyAt(u, p);
                changedJobs.addAll((ArraySet<? extends JobStatus>) maybeUpdateConstraintForPkgLocked(nowElapsed, userId, packageName));
            }
        }
        int u2 = changedJobs.size();
        if (u2 > 0) {
            this.mStateChangedListener.onControllerStateChanged(changedJobs);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ArraySet<JobStatus> maybeUpdateConstraintForPkgLocked(long nowElapsed, int userId, String packageName) {
        JobStatus js;
        boolean isWithinEJQuota;
        int i;
        boolean z;
        ArraySet<JobStatus> jobs = (ArraySet) this.mTrackedJobs.get(userId, packageName);
        ArraySet<JobStatus> changedJobs = new ArraySet<>();
        if (jobs == null || jobs.size() == 0) {
            return changedJobs;
        }
        boolean z2 = false;
        int realStandbyBucket = jobs.valueAt(0).getStandbyBucket();
        boolean realInQuota = isWithinQuotaLocked(userId, packageName, realStandbyBucket);
        boolean z3 = true;
        boolean outOfEJQuota = false;
        int i2 = jobs.size() - 1;
        while (i2 >= 0) {
            JobStatus js2 = jobs.valueAt(i2);
            boolean isWithinEJQuota2 = (js2.isRequestedExpeditedJob() && isWithinEJQuotaLocked(js2)) ? z3 : z2;
            if (isTopStartedJobLocked(js2)) {
                if (!js2.setQuotaConstraintSatisfied(nowElapsed, z3)) {
                    js = js2;
                    isWithinEJQuota = isWithinEJQuota2;
                    i = i2;
                    z = z3;
                } else {
                    changedJobs.add(js2);
                    js = js2;
                    isWithinEJQuota = isWithinEJQuota2;
                    i = i2;
                    z = z3;
                }
            } else {
                if (realStandbyBucket == 6 || realStandbyBucket == 0) {
                    js = js2;
                    isWithinEJQuota = isWithinEJQuota2;
                    i = i2;
                    z = z3;
                } else if (realStandbyBucket != js2.getEffectiveStandbyBucket()) {
                    js = js2;
                    isWithinEJQuota = isWithinEJQuota2;
                    i = i2;
                    z = z3;
                } else {
                    js = js2;
                    isWithinEJQuota = isWithinEJQuota2;
                    i = i2;
                    z = z3;
                    if (setConstraintSatisfied(js2, nowElapsed, realInQuota, isWithinEJQuota)) {
                        changedJobs.add(js);
                    }
                }
                if (setConstraintSatisfied(js, nowElapsed, isWithinQuotaLocked(js), isWithinEJQuota)) {
                    changedJobs.add(js);
                }
            }
            if (js.isRequestedExpeditedJob()) {
                boolean isWithinEJQuota3 = isWithinEJQuota;
                if (setExpeditedQuotaApproved(js, nowElapsed, isWithinEJQuota3)) {
                    changedJobs.add(js);
                }
                outOfEJQuota |= !isWithinEJQuota3 ? z : false;
            }
            i2 = i - 1;
            z3 = z;
            z2 = false;
        }
        if (!realInQuota || outOfEJQuota) {
            maybeScheduleStartAlarmLocked(userId, packageName, realStandbyBucket);
        } else {
            this.mInQuotaAlarmQueue.removeAlarmForKey(new Package(userId, packageName));
        }
        return changedJobs;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class UidConstraintUpdater implements Consumer<JobStatus> {
        public final ArraySet<JobStatus> changedJobs;
        private final SparseArrayMap<String, Integer> mToScheduleStartAlarms;
        long mUpdateTimeElapsed;

        private UidConstraintUpdater() {
            this.mToScheduleStartAlarms = new SparseArrayMap<>();
            this.changedJobs = new ArraySet<>();
            this.mUpdateTimeElapsed = 0L;
        }

        void prepare() {
            this.mUpdateTimeElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
            this.changedJobs.clear();
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Consumer
        public void accept(JobStatus jobStatus) {
            boolean isWithinEJQuota;
            if (jobStatus.isRequestedExpeditedJob()) {
                isWithinEJQuota = QuotaController.this.isWithinEJQuotaLocked(jobStatus);
            } else {
                isWithinEJQuota = false;
            }
            QuotaController quotaController = QuotaController.this;
            if (quotaController.setConstraintSatisfied(jobStatus, this.mUpdateTimeElapsed, quotaController.isWithinQuotaLocked(jobStatus), isWithinEJQuota)) {
                this.changedJobs.add(jobStatus);
            }
            if (QuotaController.this.setExpeditedQuotaApproved(jobStatus, this.mUpdateTimeElapsed, isWithinEJQuota)) {
                this.changedJobs.add(jobStatus);
            }
            int userId = jobStatus.getSourceUserId();
            String packageName = jobStatus.getSourcePackageName();
            int realStandbyBucket = jobStatus.getStandbyBucket();
            if (isWithinEJQuota && QuotaController.this.isWithinQuotaLocked(userId, packageName, realStandbyBucket)) {
                QuotaController.this.mInQuotaAlarmQueue.removeAlarmForKey(new Package(userId, packageName));
            } else {
                this.mToScheduleStartAlarms.add(userId, packageName, Integer.valueOf(realStandbyBucket));
            }
        }

        void postProcess() {
            for (int u = 0; u < this.mToScheduleStartAlarms.numMaps(); u++) {
                int userId = this.mToScheduleStartAlarms.keyAt(u);
                for (int p = 0; p < this.mToScheduleStartAlarms.numElementsForKey(userId); p++) {
                    String packageName = (String) this.mToScheduleStartAlarms.keyAt(u, p);
                    int standbyBucket = ((Integer) this.mToScheduleStartAlarms.get(userId, packageName)).intValue();
                    QuotaController.this.maybeScheduleStartAlarmLocked(userId, packageName, standbyBucket);
                }
            }
        }

        void reset() {
            this.mToScheduleStartAlarms.clear();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ArraySet<JobStatus> maybeUpdateConstraintForUidLocked(int uid) {
        this.mUpdateUidConstraints.prepare();
        this.mService.getJobStore().forEachJobForSourceUid(uid, this.mUpdateUidConstraints);
        this.mUpdateUidConstraints.postProcess();
        this.mUpdateUidConstraints.reset();
        return this.mUpdateUidConstraints.changedJobs;
    }

    /* JADX WARN: Removed duplicated region for block: B:30:0x00af  */
    /* JADX WARN: Removed duplicated region for block: B:42:0x00d5  */
    /* JADX WARN: Removed duplicated region for block: B:69:0x0180  */
    /* JADX WARN: Removed duplicated region for block: B:72:0x019a  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    void maybeScheduleStartAlarmLocked(int userId, String packageName, int standbyBucket) {
        int i;
        String str;
        boolean inRegularQuota;
        long inRegularQuotaTimeElapsed;
        long inEJQuotaTimeElapsed;
        long inQuotaTimeElapsed;
        if (standbyBucket == 4) {
            return;
        }
        ArraySet<JobStatus> jobs = (ArraySet) this.mTrackedJobs.get(userId, packageName);
        if (jobs == null) {
            i = userId;
            str = packageName;
        } else if (jobs.size() != 0) {
            ExecutionStats stats = getExecutionStatsLocked(userId, packageName, standbyBucket);
            boolean isUnderJobCountQuota = isUnderJobCountQuotaLocked(stats, standbyBucket);
            boolean isUnderTimingSessionCountQuota = isUnderSessionCountQuotaLocked(stats, standbyBucket);
            long remainingEJQuota = getRemainingEJExecutionTimeLocked(userId, packageName);
            if (stats.executionTimeInWindowMs < this.mAllowedTimePerPeriodMs[standbyBucket] && stats.executionTimeInMaxPeriodMs < this.mMaxExecutionTimeMs && isUnderJobCountQuota && isUnderTimingSessionCountQuota) {
                inRegularQuota = true;
                if (!inRegularQuota && remainingEJQuota > 0) {
                    if (DEBUG) {
                        Slog.e(TAG, "maybeScheduleStartAlarmLocked called for " + Package.packageToString(userId, packageName) + " even though it already has " + getRemainingExecutionTimeLocked(userId, packageName, standbyBucket) + "ms in its quota.");
                    }
                    this.mInQuotaAlarmQueue.removeAlarmForKey(new Package(userId, packageName));
                    this.mHandler.obtainMessage(2, userId, 0, packageName).sendToTarget();
                    return;
                }
                long inRegularQuotaTimeElapsed2 = JobStatus.NO_LATEST_RUNTIME;
                long inEJQuotaTimeElapsed2 = JobStatus.NO_LATEST_RUNTIME;
                if (!inRegularQuota) {
                    long inQuotaTimeElapsed2 = stats.inQuotaTimeElapsed;
                    if (!isUnderJobCountQuota && stats.bgJobCountInWindow < stats.jobCountLimit) {
                        inQuotaTimeElapsed2 = Math.max(inQuotaTimeElapsed2, stats.jobRateLimitExpirationTimeElapsed);
                    }
                    if (!isUnderTimingSessionCountQuota && stats.sessionCountInWindow < stats.sessionCountLimit) {
                        inQuotaTimeElapsed2 = Math.max(inQuotaTimeElapsed2, stats.sessionRateLimitExpirationTimeElapsed);
                    }
                    inRegularQuotaTimeElapsed2 = inQuotaTimeElapsed2;
                }
                if (remainingEJQuota > 0) {
                    long limitMs = getEJLimitMsLocked(userId, packageName, standbyBucket) - this.mQuotaBufferMs;
                    long sumMs = 0;
                    Timer ejTimer = (Timer) this.mEJPkgTimers.get(userId, packageName);
                    if (ejTimer != null && ejTimer.isActive()) {
                        long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
                        sumMs = 0 + ejTimer.getCurrentDuration(nowElapsed);
                        if (sumMs >= limitMs) {
                            inEJQuotaTimeElapsed2 = (nowElapsed - limitMs) + this.mEJLimitWindowSizeMs;
                        }
                    }
                    List<TimedEvent> timingSessions = (List) this.mEJTimingSessions.get(userId, packageName);
                    if (timingSessions != null) {
                        int i2 = timingSessions.size() - 1;
                        while (true) {
                            if (i2 < 0) {
                                inRegularQuotaTimeElapsed = inRegularQuotaTimeElapsed2;
                                break;
                            }
                            TimingSession ts = (TimingSession) timingSessions.get(i2);
                            long j = ts.endTimeElapsed;
                            inRegularQuotaTimeElapsed = inRegularQuotaTimeElapsed2;
                            long inRegularQuotaTimeElapsed3 = ts.startTimeElapsed;
                            long durationMs = j - inRegularQuotaTimeElapsed3;
                            sumMs += durationMs;
                            if (sumMs >= limitMs) {
                                long durationMs2 = this.mEJLimitWindowSizeMs;
                                inEJQuotaTimeElapsed2 = ts.startTimeElapsed + (sumMs - limitMs) + durationMs2;
                                break;
                            }
                            i2--;
                            inRegularQuotaTimeElapsed2 = inRegularQuotaTimeElapsed;
                        }
                        inEJQuotaTimeElapsed = inEJQuotaTimeElapsed2;
                    } else {
                        inRegularQuotaTimeElapsed = inRegularQuotaTimeElapsed2;
                        if ((ejTimer == null || !ejTimer.isActive()) && inRegularQuota) {
                            Slog.wtf(TAG, Package.packageToString(userId, packageName) + " has 0 EJ quota without running anything");
                            return;
                        }
                        inEJQuotaTimeElapsed = inEJQuotaTimeElapsed2;
                    }
                } else {
                    inRegularQuotaTimeElapsed = inRegularQuotaTimeElapsed2;
                    inEJQuotaTimeElapsed = Long.MAX_VALUE;
                }
                inQuotaTimeElapsed = Math.min(inRegularQuotaTimeElapsed, inEJQuotaTimeElapsed);
                if (inQuotaTimeElapsed <= JobSchedulerService.sElapsedRealtimeClock.millis()) {
                    long nowElapsed2 = JobSchedulerService.sElapsedRealtimeClock.millis();
                    Slog.wtf(TAG, "In quota time is " + (nowElapsed2 - inQuotaTimeElapsed) + "ms old. Now=" + nowElapsed2 + ", inQuotaTime=" + inQuotaTimeElapsed + ": " + stats);
                    inQuotaTimeElapsed = nowElapsed2 + BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS;
                }
                this.mInQuotaAlarmQueue.addAlarm(new Package(userId, packageName), inQuotaTimeElapsed);
                return;
            }
            inRegularQuota = false;
            if (!inRegularQuota) {
            }
            long inRegularQuotaTimeElapsed22 = JobStatus.NO_LATEST_RUNTIME;
            long inEJQuotaTimeElapsed22 = JobStatus.NO_LATEST_RUNTIME;
            if (!inRegularQuota) {
            }
            if (remainingEJQuota > 0) {
            }
            inQuotaTimeElapsed = Math.min(inRegularQuotaTimeElapsed, inEJQuotaTimeElapsed);
            if (inQuotaTimeElapsed <= JobSchedulerService.sElapsedRealtimeClock.millis()) {
            }
            this.mInQuotaAlarmQueue.addAlarm(new Package(userId, packageName), inQuotaTimeElapsed);
            return;
        } else {
            i = userId;
            str = packageName;
        }
        Slog.e(TAG, "maybeScheduleStartAlarmLocked called for " + Package.packageToString(userId, packageName) + " that has no jobs");
        this.mInQuotaAlarmQueue.removeAlarmForKey(new Package(i, str));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setConstraintSatisfied(JobStatus jobStatus, long nowElapsed, boolean isWithinQuota, boolean isWithinEjQuota) {
        boolean isSatisfied;
        if (jobStatus.startedAsExpeditedJob) {
            isSatisfied = isWithinEjQuota;
        } else if (this.mService.isCurrentlyRunningLocked(jobStatus)) {
            isSatisfied = isWithinQuota;
        } else {
            isSatisfied = isWithinEjQuota || isWithinQuota;
        }
        if (!isSatisfied && jobStatus.getWhenStandbyDeferred() == 0) {
            jobStatus.setWhenStandbyDeferred(nowElapsed);
        }
        return jobStatus.setQuotaConstraintSatisfied(nowElapsed, isSatisfied);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setExpeditedQuotaApproved(JobStatus jobStatus, long nowElapsed, boolean isWithinQuota) {
        if (jobStatus.setExpeditedJobQuotaApproved(nowElapsed, isWithinQuota)) {
            this.mBackgroundJobsController.evaluateStateLocked(jobStatus);
            this.mConnectivityController.evaluateStateLocked(jobStatus);
            if (isWithinQuota && jobStatus.isReady()) {
                this.mStateChangedListener.onRunJobNow(jobStatus);
                return true;
            }
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class TimingSession implements TimedEvent {
        public final int bgJobCount;
        public final long endTimeElapsed;
        private final int mHashCode;
        public final long startTimeElapsed;

        TimingSession(long startElapsed, long endElapsed, int bgJobCount) {
            this.startTimeElapsed = startElapsed;
            this.endTimeElapsed = endElapsed;
            this.bgJobCount = bgJobCount;
            int hashCode = (0 * 31) + QuotaController.hashLong(startElapsed);
            this.mHashCode = (((hashCode * 31) + QuotaController.hashLong(endElapsed)) * 31) + bgJobCount;
        }

        @Override // com.android.server.job.controllers.QuotaController.TimedEvent
        public long getEndTimeElapsed() {
            return this.endTimeElapsed;
        }

        public String toString() {
            return "TimingSession{" + this.startTimeElapsed + "->" + this.endTimeElapsed + ", " + this.bgJobCount + "}";
        }

        public boolean equals(Object obj) {
            if (obj instanceof TimingSession) {
                TimingSession other = (TimingSession) obj;
                return this.startTimeElapsed == other.startTimeElapsed && this.endTimeElapsed == other.endTimeElapsed && this.bgJobCount == other.bgJobCount;
            }
            return false;
        }

        public int hashCode() {
            return this.mHashCode;
        }

        @Override // com.android.server.job.controllers.QuotaController.TimedEvent
        public void dump(IndentingPrintWriter pw) {
            pw.print(this.startTimeElapsed);
            pw.print(" -> ");
            pw.print(this.endTimeElapsed);
            pw.print(" (");
            pw.print(this.endTimeElapsed - this.startTimeElapsed);
            pw.print("), ");
            pw.print(this.bgJobCount);
            pw.print(" bg jobs.");
            pw.println();
        }

        public void dump(ProtoOutputStream proto, long fieldId) {
            long token = proto.start(fieldId);
            proto.write(1112396529665L, this.startTimeElapsed);
            proto.write(1112396529666L, this.endTimeElapsed);
            proto.write(1120986464259L, this.bgJobCount);
            proto.end(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class QuotaBump implements TimedEvent {
        public final long eventTimeElapsed;

        QuotaBump(long eventElapsed) {
            this.eventTimeElapsed = eventElapsed;
        }

        @Override // com.android.server.job.controllers.QuotaController.TimedEvent
        public long getEndTimeElapsed() {
            return this.eventTimeElapsed;
        }

        @Override // com.android.server.job.controllers.QuotaController.TimedEvent
        public void dump(IndentingPrintWriter pw) {
            pw.print("Quota bump @ ");
            pw.print(this.eventTimeElapsed);
            pw.println();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class ShrinkableDebits {
        private long mDebitTally = 0;
        private int mStandbyBucket;

        ShrinkableDebits(int standbyBucket) {
            this.mStandbyBucket = standbyBucket;
        }

        long getTallyLocked() {
            return this.mDebitTally;
        }

        long transactLocked(long amount) {
            long j;
            if (amount < 0) {
                long abs = Math.abs(amount);
                long j2 = this.mDebitTally;
                if (abs > j2) {
                    j = j2 + amount;
                    long leftover = j;
                    this.mDebitTally = Math.max(0L, this.mDebitTally + amount);
                    return leftover;
                }
            }
            j = 0;
            long leftover2 = j;
            this.mDebitTally = Math.max(0L, this.mDebitTally + amount);
            return leftover2;
        }

        void setStandbyBucketLocked(int standbyBucket) {
            this.mStandbyBucket = standbyBucket;
        }

        int getStandbyBucketLocked() {
            return this.mStandbyBucket;
        }

        public String toString() {
            return "ShrinkableDebits { debit tally: " + this.mDebitTally + ", bucket: " + this.mStandbyBucket + " }";
        }

        void dumpLocked(IndentingPrintWriter pw) {
            pw.println(toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class Timer {
        private int mBgJobCount;
        private long mDebitAdjustment;
        private final Package mPkg;
        private final boolean mRegularJobTimer;
        private final ArraySet<JobStatus> mRunningBgJobs = new ArraySet<>();
        private long mStartTimeElapsed;
        private final int mUid;

        Timer(int uid, int userId, String packageName, boolean regularJobTimer) {
            this.mPkg = new Package(userId, packageName);
            this.mUid = uid;
            this.mRegularJobTimer = regularJobTimer;
        }

        void startTrackingJobLocked(JobStatus jobStatus) {
            if (QuotaController.this.isTopStartedJobLocked(jobStatus)) {
                if (QuotaController.DEBUG) {
                    Slog.v(QuotaController.TAG, "Timer ignoring " + jobStatus.toShortString() + " because isTop");
                    return;
                }
                return;
            }
            if (QuotaController.DEBUG) {
                Slog.v(QuotaController.TAG, "Starting to track " + jobStatus.toShortString());
            }
            if (this.mRunningBgJobs.add(jobStatus) && shouldTrackLocked()) {
                this.mBgJobCount++;
                if (this.mRegularJobTimer) {
                    QuotaController.this.incrementJobCountLocked(this.mPkg.userId, this.mPkg.packageName, 1);
                }
                if (this.mRunningBgJobs.size() == 1) {
                    this.mStartTimeElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
                    this.mDebitAdjustment = 0L;
                    if (this.mRegularJobTimer) {
                        QuotaController.this.invalidateAllExecutionStatsLocked(this.mPkg.userId, this.mPkg.packageName);
                    }
                    scheduleCutoff();
                }
            }
        }

        void stopTrackingJob(JobStatus jobStatus) {
            if (QuotaController.DEBUG) {
                Slog.v(QuotaController.TAG, "Stopping tracking of " + jobStatus.toShortString());
            }
            synchronized (QuotaController.this.mLock) {
                if (this.mRunningBgJobs.size() == 0) {
                    if (QuotaController.DEBUG) {
                        Slog.d(QuotaController.TAG, "Timer isn't tracking any jobs but still told to stop");
                    }
                    return;
                }
                long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
                int standbyBucket = JobSchedulerService.standbyBucketForPackage(this.mPkg.packageName, this.mPkg.userId, nowElapsed);
                if (this.mRunningBgJobs.remove(jobStatus) && this.mRunningBgJobs.size() == 0 && !QuotaController.this.isQuotaFreeLocked(standbyBucket)) {
                    emitSessionLocked(nowElapsed);
                    cancelCutoff();
                }
            }
        }

        void updateDebitAdjustment(long nowElapsed, long debit) {
            this.mDebitAdjustment = Math.max(this.mDebitAdjustment + debit, this.mStartTimeElapsed - nowElapsed);
        }

        void dropEverythingLocked() {
            this.mRunningBgJobs.clear();
            cancelCutoff();
        }

        private void emitSessionLocked(long nowElapsed) {
            int i = this.mBgJobCount;
            if (i <= 0) {
                return;
            }
            TimingSession ts = new TimingSession(this.mStartTimeElapsed, nowElapsed, i);
            QuotaController.this.saveTimingSession(this.mPkg.userId, this.mPkg.packageName, ts, !this.mRegularJobTimer, this.mDebitAdjustment);
            this.mBgJobCount = 0;
            cancelCutoff();
            if (this.mRegularJobTimer) {
                QuotaController.this.incrementTimingSessionCountLocked(this.mPkg.userId, this.mPkg.packageName);
            }
        }

        public boolean isActive() {
            boolean z;
            synchronized (QuotaController.this.mLock) {
                z = this.mBgJobCount > 0;
            }
            return z;
        }

        boolean isRunning(JobStatus jobStatus) {
            return this.mRunningBgJobs.contains(jobStatus);
        }

        long getCurrentDuration(long nowElapsed) {
            long j;
            synchronized (QuotaController.this.mLock) {
                j = !isActive() ? 0L : (nowElapsed - this.mStartTimeElapsed) + this.mDebitAdjustment;
            }
            return j;
        }

        int getBgJobCount() {
            int i;
            synchronized (QuotaController.this.mLock) {
                i = this.mBgJobCount;
            }
            return i;
        }

        private boolean shouldTrackLocked() {
            long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
            int standbyBucket = JobSchedulerService.standbyBucketForPackage(this.mPkg.packageName, this.mPkg.userId, nowElapsed);
            boolean hasTempAllowlistExemption = !this.mRegularJobTimer && QuotaController.this.hasTempAllowlistExemptionLocked(this.mUid, standbyBucket, nowElapsed);
            long topAppGracePeriodEndElapsed = QuotaController.this.mTopAppGraceCache.get(this.mUid);
            boolean hasTopAppExemption = !this.mRegularJobTimer && (QuotaController.this.mTopAppCache.get(this.mUid) || nowElapsed < topAppGracePeriodEndElapsed);
            if (QuotaController.DEBUG) {
                Slog.d(QuotaController.TAG, "quotaFree=" + QuotaController.this.isQuotaFreeLocked(standbyBucket) + " isFG=" + QuotaController.this.mForegroundUids.get(this.mUid) + " tempEx=" + hasTempAllowlistExemption + " topEx=" + hasTopAppExemption);
            }
            return (QuotaController.this.isQuotaFreeLocked(standbyBucket) || QuotaController.this.mForegroundUids.get(this.mUid) || hasTempAllowlistExemption || hasTopAppExemption) ? false : true;
        }

        void onStateChangedLocked(long nowElapsed, boolean isQuotaFree) {
            if (isQuotaFree) {
                emitSessionLocked(nowElapsed);
            } else if (!isActive() && shouldTrackLocked() && this.mRunningBgJobs.size() > 0) {
                this.mStartTimeElapsed = nowElapsed;
                this.mDebitAdjustment = 0L;
                this.mBgJobCount = this.mRunningBgJobs.size();
                if (this.mRegularJobTimer) {
                    QuotaController.this.incrementJobCountLocked(this.mPkg.userId, this.mPkg.packageName, this.mBgJobCount);
                    QuotaController.this.invalidateAllExecutionStatsLocked(this.mPkg.userId, this.mPkg.packageName);
                }
                scheduleCutoff();
            }
        }

        void rescheduleCutoff() {
            cancelCutoff();
            scheduleCutoff();
        }

        private void scheduleCutoff() {
            long timeRemainingMs;
            synchronized (QuotaController.this.mLock) {
                if (isActive()) {
                    Message msg = QuotaController.this.mHandler.obtainMessage(this.mRegularJobTimer ? 0 : 4, this.mPkg);
                    if (this.mRegularJobTimer) {
                        timeRemainingMs = QuotaController.this.getTimeUntilQuotaConsumedLocked(this.mPkg.userId, this.mPkg.packageName);
                    } else {
                        timeRemainingMs = QuotaController.this.getTimeUntilEJQuotaConsumedLocked(this.mPkg.userId, this.mPkg.packageName);
                    }
                    if (QuotaController.DEBUG) {
                        Slog.i(QuotaController.TAG, (this.mRegularJobTimer ? "Regular job" : "EJ") + " for " + this.mPkg + " has " + timeRemainingMs + "ms left.");
                    }
                    QuotaController.this.mHandler.sendMessageDelayed(msg, timeRemainingMs);
                }
            }
        }

        private void cancelCutoff() {
            QuotaController.this.mHandler.removeMessages(this.mRegularJobTimer ? 0 : 4, this.mPkg);
        }

        public void dump(IndentingPrintWriter pw, Predicate<JobStatus> predicate) {
            pw.print("Timer<");
            pw.print(this.mRegularJobTimer ? "REG" : "EJ");
            pw.print(">{");
            pw.print(this.mPkg);
            pw.print("} ");
            if (isActive()) {
                pw.print("started at ");
                pw.print(this.mStartTimeElapsed);
                pw.print(" (");
                pw.print(JobSchedulerService.sElapsedRealtimeClock.millis() - this.mStartTimeElapsed);
                pw.print("ms ago)");
            } else {
                pw.print("NOT active");
            }
            pw.print(", ");
            pw.print(this.mBgJobCount);
            pw.print(" running bg jobs");
            if (!this.mRegularJobTimer) {
                pw.print(" (debit adj=");
                pw.print(this.mDebitAdjustment);
                pw.print(")");
            }
            pw.println();
            pw.increaseIndent();
            for (int i = 0; i < this.mRunningBgJobs.size(); i++) {
                JobStatus js = this.mRunningBgJobs.valueAt(i);
                if (predicate.test(js)) {
                    pw.println(js.toShortString());
                }
            }
            pw.decreaseIndent();
        }

        public void dump(ProtoOutputStream proto, long fieldId, Predicate<JobStatus> predicate) {
            long token = proto.start(fieldId);
            proto.write(1133871366146L, isActive());
            proto.write(1112396529667L, this.mStartTimeElapsed);
            proto.write(1120986464260L, this.mBgJobCount);
            for (int i = 0; i < this.mRunningBgJobs.size(); i++) {
                JobStatus js = this.mRunningBgJobs.valueAt(i);
                if (predicate.test(js)) {
                    js.writeToShortProto(proto, 2246267895813L);
                }
            }
            proto.end(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class TopAppTimer {
        private final SparseArray<UsageEvents.Event> mActivities = new SparseArray<>();
        private final Package mPkg;
        private long mStartTimeElapsed;

        TopAppTimer(int userId, String packageName) {
            this.mPkg = new Package(userId, packageName);
        }

        private int calculateTimeChunks(long nowElapsed) {
            long totalTopTimeMs = nowElapsed - this.mStartTimeElapsed;
            int numTimeChunks = (int) (totalTopTimeMs / QuotaController.this.mEJTopAppTimeChunkSizeMs);
            long remainderMs = totalTopTimeMs % QuotaController.this.mEJTopAppTimeChunkSizeMs;
            if (remainderMs >= 1000) {
                return numTimeChunks + 1;
            }
            return numTimeChunks;
        }

        long getPendingReward(long nowElapsed) {
            return QuotaController.this.mEJRewardTopAppMs * calculateTimeChunks(nowElapsed);
        }

        void processEventLocked(UsageEvents.Event event) {
            long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
            switch (event.getEventType()) {
                case 1:
                    if (this.mActivities.size() == 0) {
                        this.mStartTimeElapsed = nowElapsed;
                    }
                    this.mActivities.put(event.mInstanceId, event);
                    return;
                case 2:
                case 23:
                case 24:
                    UsageEvents.Event existingEvent = (UsageEvents.Event) this.mActivities.removeReturnOld(event.mInstanceId);
                    if (existingEvent != null && this.mActivities.size() == 0) {
                        long pendingReward = getPendingReward(nowElapsed);
                        if (QuotaController.DEBUG) {
                            Slog.d(QuotaController.TAG, "Crediting " + this.mPkg + " " + pendingReward + "ms for " + calculateTimeChunks(nowElapsed) + " time chunks");
                        }
                        ShrinkableDebits debits = QuotaController.this.getEJDebitsLocked(this.mPkg.userId, this.mPkg.packageName);
                        if (QuotaController.this.transactQuotaLocked(this.mPkg.userId, this.mPkg.packageName, nowElapsed, debits, pendingReward)) {
                            QuotaController.this.mStateChangedListener.onControllerStateChanged(QuotaController.this.maybeUpdateConstraintForPkgLocked(nowElapsed, this.mPkg.userId, this.mPkg.packageName));
                            return;
                        }
                        return;
                    }
                    return;
                default:
                    return;
            }
        }

        boolean isActive() {
            boolean z;
            synchronized (QuotaController.this.mLock) {
                z = this.mActivities.size() > 0;
            }
            return z;
        }

        public void dump(IndentingPrintWriter pw) {
            pw.print("TopAppTimer{");
            pw.print(this.mPkg);
            pw.print("} ");
            if (isActive()) {
                pw.print("started at ");
                pw.print(this.mStartTimeElapsed);
                pw.print(" (");
                pw.print(JobSchedulerService.sElapsedRealtimeClock.millis() - this.mStartTimeElapsed);
                pw.print("ms ago)");
            } else {
                pw.print("NOT active");
            }
            pw.println();
            pw.increaseIndent();
            for (int i = 0; i < this.mActivities.size(); i++) {
                UsageEvents.Event event = this.mActivities.valueAt(i);
                pw.println(event.getClassName());
            }
            pw.decreaseIndent();
        }

        public void dump(ProtoOutputStream proto, long fieldId) {
            long token = proto.start(fieldId);
            proto.write(1133871366146L, isActive());
            proto.write(1112396529667L, this.mStartTimeElapsed);
            proto.write(1120986464260L, this.mActivities.size());
            proto.end(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class StandbyTracker extends AppStandbyInternal.AppIdleStateChangeListener {
        StandbyTracker() {
        }

        public void onAppIdleStateChanged(final String packageName, final int userId, boolean idle, final int bucket, int reason) {
            JobSchedulerBackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.job.controllers.QuotaController$StandbyTracker$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    QuotaController.StandbyTracker.this.m4242xf14103e5(bucket, userId, packageName);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAppIdleStateChanged$0$com-android-server-job-controllers-QuotaController$StandbyTracker  reason: not valid java name */
        public /* synthetic */ void m4242xf14103e5(int bucket, int userId, String packageName) {
            int bucketIndex = JobSchedulerService.standbyBucketToBucketIndex(bucket);
            QuotaController.this.updateStandbyBucket(userId, packageName, bucketIndex);
        }

        public void triggerTemporaryQuotaBump(String packageName, int userId) {
            synchronized (QuotaController.this.mLock) {
                List<TimedEvent> events = (List) QuotaController.this.mTimingEvents.get(userId, packageName);
                if (events != null && events.size() != 0) {
                    events.add(new QuotaBump(JobSchedulerService.sElapsedRealtimeClock.millis()));
                    QuotaController.this.invalidateAllExecutionStatsLocked(userId, packageName);
                    QuotaController.this.mHandler.obtainMessage(2, userId, 0, packageName).sendToTarget();
                }
            }
        }
    }

    void updateStandbyBucket(int userId, String packageName, int bucketIndex) {
        if (DEBUG) {
            Slog.i(TAG, "Moving pkg " + Package.packageToString(userId, packageName) + " to bucketIndex " + bucketIndex);
        }
        List<JobStatus> restrictedChanges = new ArrayList<>();
        synchronized (this.mLock) {
            ShrinkableDebits debits = (ShrinkableDebits) this.mEJStats.get(userId, packageName);
            if (debits != null) {
                debits.setStandbyBucketLocked(bucketIndex);
            }
            ArraySet<JobStatus> jobs = (ArraySet) this.mTrackedJobs.get(userId, packageName);
            if (jobs != null && jobs.size() != 0) {
                for (int i = jobs.size() - 1; i >= 0; i--) {
                    JobStatus js = jobs.valueAt(i);
                    if ((bucketIndex == 5 || js.getStandbyBucket() == 5) && bucketIndex != js.getStandbyBucket()) {
                        restrictedChanges.add(js);
                    }
                    js.setStandbyBucket(bucketIndex);
                }
                Timer timer = (Timer) this.mPkgTimers.get(userId, packageName);
                if (timer != null && timer.isActive()) {
                    timer.rescheduleCutoff();
                }
                Timer timer2 = (Timer) this.mEJPkgTimers.get(userId, packageName);
                if (timer2 != null && timer2.isActive()) {
                    timer2.rescheduleCutoff();
                }
                this.mStateChangedListener.onControllerStateChanged(maybeUpdateConstraintForPkgLocked(JobSchedulerService.sElapsedRealtimeClock.millis(), userId, packageName));
                if (restrictedChanges.size() > 0) {
                    this.mStateChangedListener.onRestrictedBucketChanged(restrictedChanges);
                }
            }
        }
    }

    /* loaded from: classes.dex */
    final class UsageEventTracker implements UsageStatsManagerInternal.UsageEventListener {
        UsageEventTracker() {
        }

        @Override // android.app.usage.UsageStatsManagerInternal.UsageEventListener
        public void onUsageEvent(int userId, UsageEvents.Event event) {
            QuotaController.this.mHandler.obtainMessage(5, userId, 0, event).sendToTarget();
        }
    }

    /* loaded from: classes.dex */
    final class TempAllowlistTracker implements PowerAllowlistInternal.TempAllowlistChangeListener {
        TempAllowlistTracker() {
        }

        public void onAppAdded(int uid) {
            synchronized (QuotaController.this.mLock) {
                long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
                QuotaController.this.mTempAllowlistCache.put(uid, true);
                ArraySet<String> packages = QuotaController.this.mService.getPackagesForUidLocked(uid);
                if (packages != null) {
                    int userId = UserHandle.getUserId(uid);
                    for (int i = packages.size() - 1; i >= 0; i--) {
                        Timer t = (Timer) QuotaController.this.mEJPkgTimers.get(userId, packages.valueAt(i));
                        if (t != null) {
                            t.onStateChangedLocked(nowElapsed, true);
                        }
                    }
                    ArraySet<JobStatus> changedJobs = QuotaController.this.maybeUpdateConstraintForUidLocked(uid);
                    if (changedJobs.size() > 0) {
                        QuotaController.this.mStateChangedListener.onControllerStateChanged(changedJobs);
                    }
                }
            }
        }

        public void onAppRemoved(int uid) {
            synchronized (QuotaController.this.mLock) {
                long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
                long endElapsed = QuotaController.this.mEJGracePeriodTempAllowlistMs + nowElapsed;
                QuotaController.this.mTempAllowlistCache.delete(uid);
                QuotaController.this.mTempAllowlistGraceCache.put(uid, endElapsed);
                Message msg = QuotaController.this.mHandler.obtainMessage(6, uid, 0);
                QuotaController.this.mHandler.sendMessageDelayed(msg, QuotaController.this.mEJGracePeriodTempAllowlistMs);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class TimedEventTooOldPredicate implements Predicate<TimedEvent> {
        private long mNowElapsed;

        private TimedEventTooOldPredicate() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateNow() {
            this.mNowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Predicate
        public boolean test(TimedEvent ts) {
            return ts.getEndTimeElapsed() <= this.mNowElapsed - 86400000;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$2$com-android-server-job-controllers-QuotaController  reason: not valid java name */
    public /* synthetic */ void m4232lambda$new$2$comandroidserverjobcontrollersQuotaController(List events) {
        if (events != null) {
            events.removeIf(this.mTimedEventTooOld);
        }
    }

    void deleteObsoleteSessionsLocked() {
        this.mTimedEventTooOld.updateNow();
        this.mTimingEvents.forEach(this.mDeleteOldEventsFunctor);
        for (int uIdx = 0; uIdx < this.mEJTimingSessions.numMaps(); uIdx++) {
            int userId = this.mEJTimingSessions.keyAt(uIdx);
            for (int pIdx = 0; pIdx < this.mEJTimingSessions.numElementsForKey(userId); pIdx++) {
                String packageName = (String) this.mEJTimingSessions.keyAt(uIdx, pIdx);
                ShrinkableDebits debits = getEJDebitsLocked(userId, packageName);
                List<TimedEvent> sessions = (List) this.mEJTimingSessions.get(userId, packageName);
                if (sessions != null) {
                    while (sessions.size() > 0) {
                        TimingSession ts = (TimingSession) sessions.get(0);
                        if (this.mTimedEventTooOld.test((TimedEvent) ts)) {
                            long duration = ts.endTimeElapsed - ts.startTimeElapsed;
                            debits.transactLocked(-duration);
                            sessions.remove(0);
                        }
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class QcHandler extends Handler {
        QcHandler(Looper looper) {
            super(looper);
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2798=5] */
        /* JADX WARN: Removed duplicated region for block: B:124:0x0332 A[Catch: all -> 0x038a, TryCatch #6 {all -> 0x038a, blocks: (B:133:0x036c, B:135:0x0378, B:136:0x037f, B:122:0x032c, B:124:0x0332, B:126:0x033a, B:128:0x034e, B:129:0x0351, B:131:0x0366, B:132:0x0369, B:142:0x0388), top: B:179:0x025e }] */
        /* JADX WARN: Removed duplicated region for block: B:135:0x0378 A[Catch: all -> 0x038a, TryCatch #6 {all -> 0x038a, blocks: (B:133:0x036c, B:135:0x0378, B:136:0x037f, B:122:0x032c, B:124:0x0332, B:126:0x033a, B:128:0x034e, B:129:0x0351, B:131:0x0366, B:132:0x0369, B:142:0x0388), top: B:179:0x025e }] */
        @Override // android.os.Handler
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public void handleMessage(Message msg) {
            boolean reprocess;
            boolean isQuotaFree;
            int uid;
            int uid2;
            ArraySet<String> packages;
            ArraySet<JobStatus> changedJobs;
            synchronized (QuotaController.this.mLock) {
                switch (msg.what) {
                    case 0:
                        Package pkg = (Package) msg.obj;
                        if (QuotaController.DEBUG) {
                            Slog.d(QuotaController.TAG, "Checking if " + pkg + " has reached its quota.");
                        }
                        if (QuotaController.this.getRemainingExecutionTimeLocked(pkg.userId, pkg.packageName) <= 50) {
                            if (QuotaController.DEBUG) {
                                Slog.d(QuotaController.TAG, pkg + " has reached its quota.");
                            }
                            QuotaController.this.mStateChangedListener.onControllerStateChanged(QuotaController.this.maybeUpdateConstraintForPkgLocked(JobSchedulerService.sElapsedRealtimeClock.millis(), pkg.userId, pkg.packageName));
                            break;
                        } else {
                            Message rescheduleMsg = obtainMessage(0, pkg);
                            long timeRemainingMs = QuotaController.this.getTimeUntilQuotaConsumedLocked(pkg.userId, pkg.packageName);
                            if (QuotaController.DEBUG) {
                                Slog.d(QuotaController.TAG, pkg + " has " + timeRemainingMs + "ms left.");
                            }
                            sendMessageDelayed(rescheduleMsg, timeRemainingMs);
                            break;
                        }
                    case 1:
                        if (QuotaController.DEBUG) {
                            Slog.d(QuotaController.TAG, "Cleaning up timing sessions.");
                        }
                        QuotaController.this.deleteObsoleteSessionsLocked();
                        QuotaController.this.maybeScheduleCleanupAlarmLocked();
                        break;
                    case 2:
                        String packageName = (String) msg.obj;
                        int userId = msg.arg1;
                        if (QuotaController.DEBUG) {
                            Slog.d(QuotaController.TAG, "Checking pkg " + Package.packageToString(userId, packageName));
                        }
                        QuotaController.this.mStateChangedListener.onControllerStateChanged(QuotaController.this.maybeUpdateConstraintForPkgLocked(JobSchedulerService.sElapsedRealtimeClock.millis(), userId, packageName));
                        break;
                    case 3:
                        int uid3 = msg.arg1;
                        int procState = msg.arg2;
                        int userId2 = UserHandle.getUserId(uid3);
                        long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
                        synchronized (QuotaController.this.mLock) {
                            try {
                                try {
                                    if (procState <= 2) {
                                        QuotaController.this.mTopAppCache.put(uid3, true);
                                        QuotaController.this.mTopAppGraceCache.delete(uid3);
                                        if (QuotaController.this.mForegroundUids.get(uid3)) {
                                            break;
                                        } else {
                                            QuotaController.this.mForegroundUids.put(uid3, true);
                                            isQuotaFree = true;
                                            uid = uid3;
                                            try {
                                                if (QuotaController.this.mPkgTimers.indexOfKey(userId2) < 0 || QuotaController.this.mEJPkgTimers.indexOfKey(userId2) >= 0) {
                                                    uid2 = uid;
                                                    packages = QuotaController.this.mService.getPackagesForUidLocked(uid2);
                                                    if (packages != null) {
                                                        for (int i = packages.size() - 1; i >= 0; i--) {
                                                            Timer t = (Timer) QuotaController.this.mEJPkgTimers.get(userId2, packages.valueAt(i));
                                                            if (t != null) {
                                                                t.onStateChangedLocked(nowElapsed, isQuotaFree);
                                                            }
                                                            Timer t2 = (Timer) QuotaController.this.mPkgTimers.get(userId2, packages.valueAt(i));
                                                            if (t2 != null) {
                                                                t2.onStateChangedLocked(nowElapsed, isQuotaFree);
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    uid2 = uid;
                                                }
                                                changedJobs = QuotaController.this.maybeUpdateConstraintForUidLocked(uid2);
                                                if (changedJobs.size() > 0) {
                                                    QuotaController.this.mStateChangedListener.onControllerStateChanged(changedJobs);
                                                }
                                                break;
                                            } catch (Throwable th) {
                                                th = th;
                                                throw th;
                                            }
                                        }
                                    } else {
                                        if (procState <= 4) {
                                            boolean reprocess2 = !QuotaController.this.mForegroundUids.get(uid3);
                                            QuotaController.this.mForegroundUids.put(uid3, true);
                                            reprocess = reprocess2;
                                            isQuotaFree = true;
                                        } else {
                                            try {
                                                QuotaController.this.mForegroundUids.delete(uid3);
                                                reprocess = true;
                                                isQuotaFree = false;
                                            } catch (Throwable th2) {
                                                th = th2;
                                                throw th;
                                            }
                                        }
                                        if (QuotaController.this.mTopAppCache.get(uid3)) {
                                            try {
                                                long endElapsed = QuotaController.this.mEJGracePeriodTopAppMs + nowElapsed;
                                                QuotaController.this.mTopAppCache.delete(uid3);
                                                QuotaController.this.mTopAppGraceCache.put(uid3, endElapsed);
                                                uid = uid3;
                                                try {
                                                    sendMessageDelayed(obtainMessage(6, uid3, 0), QuotaController.this.mEJGracePeriodTopAppMs);
                                                } catch (Throwable th3) {
                                                    th = th3;
                                                    throw th;
                                                }
                                            } catch (Throwable th4) {
                                                th = th4;
                                            }
                                        } else {
                                            uid = uid3;
                                        }
                                        if (!reprocess) {
                                            break;
                                        }
                                        if (QuotaController.this.mPkgTimers.indexOfKey(userId2) < 0) {
                                        }
                                        uid2 = uid;
                                        packages = QuotaController.this.mService.getPackagesForUidLocked(uid2);
                                        if (packages != null) {
                                        }
                                        changedJobs = QuotaController.this.maybeUpdateConstraintForUidLocked(uid2);
                                        if (changedJobs.size() > 0) {
                                        }
                                    }
                                } catch (Throwable th5) {
                                    th = th5;
                                }
                            } catch (Throwable th6) {
                                th = th6;
                            }
                        }
                    case 4:
                        Package pkg2 = (Package) msg.obj;
                        if (QuotaController.DEBUG) {
                            Slog.d(QuotaController.TAG, "Checking if " + pkg2 + " has reached its EJ quota.");
                        }
                        if (QuotaController.this.getRemainingEJExecutionTimeLocked(pkg2.userId, pkg2.packageName) <= 0) {
                            if (QuotaController.DEBUG) {
                                Slog.d(QuotaController.TAG, pkg2 + " has reached its EJ quota.");
                            }
                            QuotaController.this.mStateChangedListener.onControllerStateChanged(QuotaController.this.maybeUpdateConstraintForPkgLocked(JobSchedulerService.sElapsedRealtimeClock.millis(), pkg2.userId, pkg2.packageName));
                            break;
                        } else {
                            Message rescheduleMsg2 = obtainMessage(4, pkg2);
                            long timeRemainingMs2 = QuotaController.this.getTimeUntilEJQuotaConsumedLocked(pkg2.userId, pkg2.packageName);
                            if (QuotaController.DEBUG) {
                                Slog.d(QuotaController.TAG, pkg2 + " has " + timeRemainingMs2 + "ms left for EJ");
                            }
                            sendMessageDelayed(rescheduleMsg2, timeRemainingMs2);
                            break;
                        }
                    case 5:
                        int userId3 = msg.arg1;
                        UsageEvents.Event event = (UsageEvents.Event) msg.obj;
                        String pkgName = event.getPackageName();
                        if (QuotaController.DEBUG) {
                            Slog.d(QuotaController.TAG, "Processing event " + event.getEventType() + " for " + Package.packageToString(userId3, pkgName));
                        }
                        switch (event.getEventType()) {
                            case 1:
                            case 2:
                            case 23:
                            case 24:
                                synchronized (QuotaController.this.mLock) {
                                    TopAppTimer timer = (TopAppTimer) QuotaController.this.mTopAppTrackers.get(userId3, pkgName);
                                    if (timer == null) {
                                        timer = new TopAppTimer(userId3, pkgName);
                                        QuotaController.this.mTopAppTrackers.add(userId3, pkgName, timer);
                                    }
                                    timer.processEventLocked(event);
                                }
                                break;
                            case 7:
                            case 9:
                            case 12:
                                QuotaController quotaController = QuotaController.this;
                                quotaController.grantRewardForInstantEvent(userId3, pkgName, quotaController.mEJRewardInteractionMs);
                                break;
                            case 10:
                                QuotaController quotaController2 = QuotaController.this;
                                quotaController2.grantRewardForInstantEvent(userId3, pkgName, quotaController2.mEJRewardNotificationSeenMs);
                                break;
                        }
                        break;
                    case 6:
                        int uid4 = msg.arg1;
                        synchronized (QuotaController.this.mLock) {
                            if (!QuotaController.this.mTempAllowlistCache.get(uid4) && !QuotaController.this.mTopAppCache.get(uid4)) {
                                long nowElapsed2 = JobSchedulerService.sElapsedRealtimeClock.millis();
                                if (nowElapsed2 >= QuotaController.this.mTempAllowlistGraceCache.get(uid4) && nowElapsed2 >= QuotaController.this.mTopAppGraceCache.get(uid4)) {
                                    if (QuotaController.DEBUG) {
                                        Slog.d(QuotaController.TAG, uid4 + " is now out of grace period");
                                    }
                                    QuotaController.this.mTempAllowlistGraceCache.delete(uid4);
                                    QuotaController.this.mTopAppGraceCache.delete(uid4);
                                    ArraySet<String> packages2 = QuotaController.this.mService.getPackagesForUidLocked(uid4);
                                    if (packages2 != null) {
                                        int userId4 = UserHandle.getUserId(uid4);
                                        for (int i2 = packages2.size() - 1; i2 >= 0; i2--) {
                                            Timer t3 = (Timer) QuotaController.this.mEJPkgTimers.get(userId4, packages2.valueAt(i2));
                                            if (t3 != null) {
                                                t3.onStateChangedLocked(nowElapsed2, false);
                                            }
                                        }
                                        ArraySet<JobStatus> changedJobs2 = QuotaController.this.maybeUpdateConstraintForUidLocked(uid4);
                                        if (changedJobs2.size() > 0) {
                                            QuotaController.this.mStateChangedListener.onControllerStateChanged(changedJobs2);
                                        }
                                    }
                                    break;
                                }
                                if (QuotaController.DEBUG) {
                                    Slog.d(QuotaController.TAG, uid4 + " is still in grace period");
                                }
                                break;
                            }
                            if (QuotaController.DEBUG) {
                                Slog.d(QuotaController.TAG, uid4 + " is still allowed");
                            }
                            break;
                        }
                        break;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class InQuotaAlarmQueue extends AlarmQueue<Package> {
        private InQuotaAlarmQueue(Context context, Looper looper) {
            super(context, looper, QuotaController.ALARM_TAG_QUOTA_CHECK, "In quota", false, 60000L);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.utils.AlarmQueue
        public boolean isForUser(Package key, int userId) {
            return key.userId == userId;
        }

        @Override // com.android.server.utils.AlarmQueue
        protected void processExpiredAlarms(ArraySet<Package> expired) {
            for (int i = 0; i < expired.size(); i++) {
                Package p = expired.valueAt(i);
                QuotaController.this.mHandler.obtainMessage(2, p.userId, 0, p.packageName).sendToTarget();
            }
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void prepareForUpdatedConstantsLocked() {
        this.mQcConstants.mShouldReevaluateConstraints = false;
        this.mQcConstants.mRateLimitingConstantsUpdated = false;
        this.mQcConstants.mExecutionPeriodConstantsUpdated = false;
        this.mQcConstants.mEJLimitConstantsUpdated = false;
        this.mQcConstants.mQuotaBumpConstantsUpdated = false;
    }

    @Override // com.android.server.job.controllers.StateController
    public void processConstantLocked(DeviceConfig.Properties properties, String key) {
        this.mQcConstants.processConstantLocked(properties, key);
    }

    @Override // com.android.server.job.controllers.StateController
    public void onConstantsUpdatedLocked() {
        if (this.mQcConstants.mShouldReevaluateConstraints || this.mIsEnabled == this.mConstants.USE_TARE_POLICY) {
            this.mIsEnabled = !this.mConstants.USE_TARE_POLICY;
            JobSchedulerBackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.job.controllers.QuotaController$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    QuotaController.this.m4233x5274dcd1();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onConstantsUpdatedLocked$3$com-android-server-job-controllers-QuotaController  reason: not valid java name */
    public /* synthetic */ void m4233x5274dcd1() {
        synchronized (this.mLock) {
            invalidateAllExecutionStatsLocked();
            maybeUpdateAllConstraintsLocked();
        }
    }

    /* loaded from: classes.dex */
    class QcConstants {
        private static final long DEFAULT_ALLOWED_TIME_PER_PERIOD_ACTIVE_MS = 600000;
        private static final long DEFAULT_ALLOWED_TIME_PER_PERIOD_EXEMPTED_MS = 600000;
        private static final long DEFAULT_ALLOWED_TIME_PER_PERIOD_FREQUENT_MS = 600000;
        private static final long DEFAULT_ALLOWED_TIME_PER_PERIOD_RARE_MS = 600000;
        private static final long DEFAULT_ALLOWED_TIME_PER_PERIOD_RESTRICTED_MS = 600000;
        private static final long DEFAULT_ALLOWED_TIME_PER_PERIOD_WORKING_MS = 600000;
        private static final long DEFAULT_EJ_GRACE_PERIOD_TEMP_ALLOWLIST_MS = 180000;
        private static final long DEFAULT_EJ_GRACE_PERIOD_TOP_APP_MS = 60000;
        private static final long DEFAULT_EJ_LIMIT_ACTIVE_MS = 1800000;
        private static final long DEFAULT_EJ_LIMIT_ADDITION_INSTALLER_MS = 1800000;
        private static final long DEFAULT_EJ_LIMIT_ADDITION_SPECIAL_MS = 900000;
        private static final long DEFAULT_EJ_LIMIT_EXEMPTED_MS = 2700000;
        private static final long DEFAULT_EJ_LIMIT_FREQUENT_MS = 600000;
        private static final long DEFAULT_EJ_LIMIT_RARE_MS = 600000;
        private static final long DEFAULT_EJ_LIMIT_RESTRICTED_MS = 300000;
        private static final long DEFAULT_EJ_LIMIT_WORKING_MS = 1800000;
        private static final long DEFAULT_EJ_REWARD_INTERACTION_MS = 15000;
        private static final long DEFAULT_EJ_REWARD_NOTIFICATION_SEEN_MS = 0;
        private static final long DEFAULT_EJ_REWARD_TOP_APP_MS = 10000;
        private static final long DEFAULT_EJ_TOP_APP_TIME_CHUNK_SIZE_MS = 30000;
        private static final long DEFAULT_EJ_WINDOW_SIZE_MS = 86400000;
        private static final long DEFAULT_IN_QUOTA_BUFFER_MS = 30000;
        private static final long DEFAULT_MAX_EXECUTION_TIME_MS = 14400000;
        private static final int DEFAULT_MAX_JOB_COUNT_ACTIVE = 75;
        private static final int DEFAULT_MAX_JOB_COUNT_EXEMPTED = 75;
        private static final int DEFAULT_MAX_JOB_COUNT_FREQUENT = 200;
        private static final int DEFAULT_MAX_JOB_COUNT_PER_RATE_LIMITING_WINDOW = 20;
        private static final int DEFAULT_MAX_JOB_COUNT_RARE = 48;
        private static final int DEFAULT_MAX_JOB_COUNT_RESTRICTED = 10;
        private static final int DEFAULT_MAX_JOB_COUNT_WORKING = 120;
        private static final int DEFAULT_MAX_SESSION_COUNT_ACTIVE = 75;
        private static final int DEFAULT_MAX_SESSION_COUNT_EXEMPTED = 75;
        private static final int DEFAULT_MAX_SESSION_COUNT_FREQUENT = 8;
        private static final int DEFAULT_MAX_SESSION_COUNT_PER_RATE_LIMITING_WINDOW = 20;
        private static final int DEFAULT_MAX_SESSION_COUNT_RARE = 3;
        private static final int DEFAULT_MAX_SESSION_COUNT_RESTRICTED = 1;
        private static final int DEFAULT_MAX_SESSION_COUNT_WORKING = 10;
        private static final long DEFAULT_MIN_QUOTA_CHECK_DELAY_MS = 60000;
        private static final long DEFAULT_QUOTA_BUMP_ADDITIONAL_DURATION_MS = 60000;
        private static final int DEFAULT_QUOTA_BUMP_ADDITIONAL_JOB_COUNT = 2;
        private static final int DEFAULT_QUOTA_BUMP_ADDITIONAL_SESSION_COUNT = 1;
        private static final int DEFAULT_QUOTA_BUMP_LIMIT = 8;
        private static final long DEFAULT_QUOTA_BUMP_WINDOW_SIZE_MS = 28800000;
        private static final long DEFAULT_RATE_LIMITING_WINDOW_MS = 60000;
        private static final long DEFAULT_TIMING_SESSION_COALESCING_DURATION_MS = 5000;
        private static final long DEFAULT_WINDOW_SIZE_ACTIVE_MS = 600000;
        private static final long DEFAULT_WINDOW_SIZE_EXEMPTED_MS = 600000;
        private static final long DEFAULT_WINDOW_SIZE_FREQUENT_MS = 28800000;
        private static final long DEFAULT_WINDOW_SIZE_RARE_MS = 86400000;
        private static final long DEFAULT_WINDOW_SIZE_RESTRICTED_MS = 86400000;
        private static final long DEFAULT_WINDOW_SIZE_WORKING_MS = 7200000;
        static final String KEY_ALLOWED_TIME_PER_PERIOD_ACTIVE_MS = "qc_allowed_time_per_period_active_ms";
        static final String KEY_ALLOWED_TIME_PER_PERIOD_EXEMPTED_MS = "qc_allowed_time_per_period_exempted_ms";
        static final String KEY_ALLOWED_TIME_PER_PERIOD_FREQUENT_MS = "qc_allowed_time_per_period_frequent_ms";
        static final String KEY_ALLOWED_TIME_PER_PERIOD_RARE_MS = "qc_allowed_time_per_period_rare_ms";
        static final String KEY_ALLOWED_TIME_PER_PERIOD_RESTRICTED_MS = "qc_allowed_time_per_period_restricted_ms";
        static final String KEY_ALLOWED_TIME_PER_PERIOD_WORKING_MS = "qc_allowed_time_per_period_working_ms";
        static final String KEY_EJ_GRACE_PERIOD_TEMP_ALLOWLIST_MS = "qc_ej_grace_period_temp_allowlist_ms";
        static final String KEY_EJ_GRACE_PERIOD_TOP_APP_MS = "qc_ej_grace_period_top_app_ms";
        static final String KEY_EJ_LIMIT_ACTIVE_MS = "qc_ej_limit_active_ms";
        static final String KEY_EJ_LIMIT_ADDITION_INSTALLER_MS = "qc_ej_limit_addition_installer_ms";
        static final String KEY_EJ_LIMIT_ADDITION_SPECIAL_MS = "qc_ej_limit_addition_special_ms";
        static final String KEY_EJ_LIMIT_EXEMPTED_MS = "qc_ej_limit_exempted_ms";
        static final String KEY_EJ_LIMIT_FREQUENT_MS = "qc_ej_limit_frequent_ms";
        static final String KEY_EJ_LIMIT_RARE_MS = "qc_ej_limit_rare_ms";
        static final String KEY_EJ_LIMIT_RESTRICTED_MS = "qc_ej_limit_restricted_ms";
        static final String KEY_EJ_LIMIT_WORKING_MS = "qc_ej_limit_working_ms";
        static final String KEY_EJ_REWARD_INTERACTION_MS = "qc_ej_reward_interaction_ms";
        static final String KEY_EJ_REWARD_NOTIFICATION_SEEN_MS = "qc_ej_reward_notification_seen_ms";
        static final String KEY_EJ_REWARD_TOP_APP_MS = "qc_ej_reward_top_app_ms";
        static final String KEY_EJ_TOP_APP_TIME_CHUNK_SIZE_MS = "qc_ej_top_app_time_chunk_size_ms";
        static final String KEY_EJ_WINDOW_SIZE_MS = "qc_ej_window_size_ms";
        static final String KEY_IN_QUOTA_BUFFER_MS = "qc_in_quota_buffer_ms";
        static final String KEY_MAX_EXECUTION_TIME_MS = "qc_max_execution_time_ms";
        static final String KEY_MAX_JOB_COUNT_ACTIVE = "qc_max_job_count_active";
        static final String KEY_MAX_JOB_COUNT_EXEMPTED = "qc_max_job_count_exempted";
        static final String KEY_MAX_JOB_COUNT_FREQUENT = "qc_max_job_count_frequent";
        static final String KEY_MAX_JOB_COUNT_PER_RATE_LIMITING_WINDOW = "qc_max_job_count_per_rate_limiting_window";
        static final String KEY_MAX_JOB_COUNT_RARE = "qc_max_job_count_rare";
        static final String KEY_MAX_JOB_COUNT_RESTRICTED = "qc_max_job_count_restricted";
        static final String KEY_MAX_JOB_COUNT_WORKING = "qc_max_job_count_working";
        static final String KEY_MAX_SESSION_COUNT_ACTIVE = "qc_max_session_count_active";
        static final String KEY_MAX_SESSION_COUNT_EXEMPTED = "qc_max_session_count_exempted";
        static final String KEY_MAX_SESSION_COUNT_FREQUENT = "qc_max_session_count_frequent";
        static final String KEY_MAX_SESSION_COUNT_PER_RATE_LIMITING_WINDOW = "qc_max_session_count_per_rate_limiting_window";
        static final String KEY_MAX_SESSION_COUNT_RARE = "qc_max_session_count_rare";
        static final String KEY_MAX_SESSION_COUNT_RESTRICTED = "qc_max_session_count_restricted";
        static final String KEY_MAX_SESSION_COUNT_WORKING = "qc_max_session_count_working";
        static final String KEY_MIN_QUOTA_CHECK_DELAY_MS = "qc_min_quota_check_delay_ms";
        static final String KEY_QUOTA_BUMP_ADDITIONAL_DURATION_MS = "qc_quota_bump_additional_duration_ms";
        static final String KEY_QUOTA_BUMP_ADDITIONAL_JOB_COUNT = "qc_quota_bump_additional_job_count";
        static final String KEY_QUOTA_BUMP_ADDITIONAL_SESSION_COUNT = "qc_quota_bump_additional_session_count";
        static final String KEY_QUOTA_BUMP_LIMIT = "qc_quota_bump_limit";
        static final String KEY_QUOTA_BUMP_WINDOW_SIZE_MS = "qc_quota_bump_window_size_ms";
        static final String KEY_RATE_LIMITING_WINDOW_MS = "qc_rate_limiting_window_ms";
        static final String KEY_TIMING_SESSION_COALESCING_DURATION_MS = "qc_timing_session_coalescing_duration_ms";
        static final String KEY_WINDOW_SIZE_ACTIVE_MS = "qc_window_size_active_ms";
        static final String KEY_WINDOW_SIZE_EXEMPTED_MS = "qc_window_size_exempted_ms";
        static final String KEY_WINDOW_SIZE_FREQUENT_MS = "qc_window_size_frequent_ms";
        static final String KEY_WINDOW_SIZE_RARE_MS = "qc_window_size_rare_ms";
        static final String KEY_WINDOW_SIZE_RESTRICTED_MS = "qc_window_size_restricted_ms";
        static final String KEY_WINDOW_SIZE_WORKING_MS = "qc_window_size_working_ms";
        private static final int MIN_BUCKET_JOB_COUNT = 10;
        private static final int MIN_BUCKET_SESSION_COUNT = 1;
        private static final long MIN_MAX_EXECUTION_TIME_MS = 3600000;
        private static final int MIN_MAX_JOB_COUNT_PER_RATE_LIMITING_WINDOW = 10;
        private static final int MIN_MAX_SESSION_COUNT_PER_RATE_LIMITING_WINDOW = 10;
        private static final long MIN_RATE_LIMITING_WINDOW_MS = 30000;
        private static final String QC_CONSTANT_PREFIX = "qc_";
        private boolean mShouldReevaluateConstraints = false;
        private boolean mRateLimitingConstantsUpdated = false;
        private boolean mExecutionPeriodConstantsUpdated = false;
        private boolean mEJLimitConstantsUpdated = false;
        private boolean mQuotaBumpConstantsUpdated = false;
        public long ALLOWED_TIME_PER_PERIOD_EXEMPTED_MS = 600000;
        public long ALLOWED_TIME_PER_PERIOD_ACTIVE_MS = 600000;
        public long ALLOWED_TIME_PER_PERIOD_WORKING_MS = 600000;
        public long ALLOWED_TIME_PER_PERIOD_FREQUENT_MS = 600000;
        public long ALLOWED_TIME_PER_PERIOD_RARE_MS = 600000;
        public long ALLOWED_TIME_PER_PERIOD_RESTRICTED_MS = 600000;
        public long IN_QUOTA_BUFFER_MS = 30000;
        public long WINDOW_SIZE_EXEMPTED_MS = 600000;
        public long WINDOW_SIZE_ACTIVE_MS = 600000;
        public long WINDOW_SIZE_WORKING_MS = 7200000;
        public long WINDOW_SIZE_FREQUENT_MS = 28800000;
        public long WINDOW_SIZE_RARE_MS = 86400000;
        public long WINDOW_SIZE_RESTRICTED_MS = 86400000;
        public long MAX_EXECUTION_TIME_MS = 14400000;
        public int MAX_JOB_COUNT_EXEMPTED = 75;
        public int MAX_JOB_COUNT_ACTIVE = 75;
        public int MAX_JOB_COUNT_WORKING = 120;
        public int MAX_JOB_COUNT_FREQUENT = 200;
        public int MAX_JOB_COUNT_RARE = 48;
        public int MAX_JOB_COUNT_RESTRICTED = 10;
        public long RATE_LIMITING_WINDOW_MS = 60000;
        public int MAX_JOB_COUNT_PER_RATE_LIMITING_WINDOW = 20;
        public int MAX_SESSION_COUNT_EXEMPTED = 75;
        public int MAX_SESSION_COUNT_ACTIVE = 75;
        public int MAX_SESSION_COUNT_WORKING = 10;
        public int MAX_SESSION_COUNT_FREQUENT = 8;
        public int MAX_SESSION_COUNT_RARE = 3;
        public int MAX_SESSION_COUNT_RESTRICTED = 1;
        public int MAX_SESSION_COUNT_PER_RATE_LIMITING_WINDOW = 20;
        public long TIMING_SESSION_COALESCING_DURATION_MS = DEFAULT_TIMING_SESSION_COALESCING_DURATION_MS;
        public long MIN_QUOTA_CHECK_DELAY_MS = 60000;
        public long EJ_LIMIT_EXEMPTED_MS = DEFAULT_EJ_LIMIT_EXEMPTED_MS;
        public long EJ_LIMIT_ACTIVE_MS = 1800000;
        public long EJ_LIMIT_WORKING_MS = 1800000;
        public long EJ_LIMIT_FREQUENT_MS = 600000;
        public long EJ_LIMIT_RARE_MS = 600000;
        public long EJ_LIMIT_RESTRICTED_MS = 300000;
        public long EJ_LIMIT_ADDITION_SPECIAL_MS = DEFAULT_EJ_LIMIT_ADDITION_SPECIAL_MS;
        public long EJ_LIMIT_ADDITION_INSTALLER_MS = 1800000;
        public long EJ_WINDOW_SIZE_MS = 86400000;
        public long EJ_TOP_APP_TIME_CHUNK_SIZE_MS = 30000;
        public long EJ_REWARD_TOP_APP_MS = 10000;
        public long EJ_REWARD_INTERACTION_MS = DEFAULT_EJ_REWARD_INTERACTION_MS;
        public long EJ_REWARD_NOTIFICATION_SEEN_MS = 0;
        public long EJ_GRACE_PERIOD_TEMP_ALLOWLIST_MS = 180000;
        public long EJ_GRACE_PERIOD_TOP_APP_MS = 60000;
        public long QUOTA_BUMP_ADDITIONAL_DURATION_MS = 60000;
        public int QUOTA_BUMP_ADDITIONAL_JOB_COUNT = 2;
        public int QUOTA_BUMP_ADDITIONAL_SESSION_COUNT = 1;
        public long QUOTA_BUMP_WINDOW_SIZE_MS = 28800000;
        public int QUOTA_BUMP_LIMIT = 8;

        QcConstants() {
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        public void processConstantLocked(DeviceConfig.Properties properties, String key) {
            char c;
            switch (key.hashCode()) {
                case -1952749138:
                    if (key.equals(KEY_EJ_LIMIT_ACTIVE_MS)) {
                        c = 16;
                        break;
                    }
                    c = 65535;
                    break;
                case -1719823663:
                    if (key.equals(KEY_ALLOWED_TIME_PER_PERIOD_ACTIVE_MS)) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case -1683576133:
                    if (key.equals(KEY_WINDOW_SIZE_FREQUENT_MS)) {
                        c = '\n';
                        break;
                    }
                    c = 65535;
                    break;
                case -1525098678:
                    if (key.equals(KEY_QUOTA_BUMP_WINDOW_SIZE_MS)) {
                        c = 27;
                        break;
                    }
                    c = 65535;
                    break;
                case -1515776932:
                    if (key.equals(KEY_ALLOWED_TIME_PER_PERIOD_RESTRICTED_MS)) {
                        c = 5;
                        break;
                    }
                    c = 65535;
                    break;
                case -1507602138:
                    if (key.equals(KEY_EJ_LIMIT_FREQUENT_MS)) {
                        c = 18;
                        break;
                    }
                    c = 65535;
                    break;
                case -1495638658:
                    if (key.equals(KEY_EJ_LIMIT_ADDITION_SPECIAL_MS)) {
                        c = 21;
                        break;
                    }
                    c = 65535;
                    break;
                case -1436524327:
                    if (key.equals(KEY_EJ_REWARD_NOTIFICATION_SEEN_MS)) {
                        c = '.';
                        break;
                    }
                    c = 65535;
                    break;
                case -1412574464:
                    if (key.equals(KEY_MAX_JOB_COUNT_ACTIVE)) {
                        c = 30;
                        break;
                    }
                    c = 65535;
                    break;
                case -1409079211:
                    if (key.equals(KEY_EJ_TOP_APP_TIME_CHUNK_SIZE_MS)) {
                        c = '+';
                        break;
                    }
                    c = 65535;
                    break;
                case -1301522660:
                    if (key.equals(KEY_MAX_JOB_COUNT_RARE)) {
                        c = '!';
                        break;
                    }
                    c = 65535;
                    break;
                case -1253638898:
                    if (key.equals(KEY_WINDOW_SIZE_RESTRICTED_MS)) {
                        c = '\f';
                        break;
                    }
                    c = 65535;
                    break;
                case -1004520055:
                    if (key.equals(KEY_ALLOWED_TIME_PER_PERIOD_FREQUENT_MS)) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case -947372170:
                    if (key.equals(KEY_EJ_REWARD_INTERACTION_MS)) {
                        c = '-';
                        break;
                    }
                    c = 65535;
                    break;
                case -947005713:
                    if (key.equals(KEY_RATE_LIMITING_WINDOW_MS)) {
                        c = '\r';
                        break;
                    }
                    c = 65535;
                    break;
                case -911626004:
                    if (key.equals(KEY_MAX_SESSION_COUNT_PER_RATE_LIMITING_WINDOW)) {
                        c = 15;
                        break;
                    }
                    c = 65535;
                    break;
                case -861283784:
                    if (key.equals(KEY_MAX_JOB_COUNT_EXEMPTED)) {
                        c = 29;
                        break;
                    }
                    c = 65535;
                    break;
                case -743649451:
                    if (key.equals(KEY_MAX_JOB_COUNT_RESTRICTED)) {
                        c = '\"';
                        break;
                    }
                    c = 65535;
                    break;
                case -615999962:
                    if (key.equals(KEY_QUOTA_BUMP_LIMIT)) {
                        c = 28;
                        break;
                    }
                    c = 65535;
                    break;
                case -473591193:
                    if (key.equals(KEY_WINDOW_SIZE_RARE_MS)) {
                        c = 11;
                        break;
                    }
                    c = 65535;
                    break;
                case -144699320:
                    if (key.equals(KEY_MAX_JOB_COUNT_FREQUENT)) {
                        c = ' ';
                        break;
                    }
                    c = 65535;
                    break;
                case 202838626:
                    if (key.equals(KEY_ALLOWED_TIME_PER_PERIOD_WORKING_MS)) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 224532750:
                    if (key.equals(KEY_QUOTA_BUMP_ADDITIONAL_DURATION_MS)) {
                        c = 24;
                        break;
                    }
                    c = 65535;
                    break;
                case 319829733:
                    if (key.equals(KEY_MAX_JOB_COUNT_PER_RATE_LIMITING_WINDOW)) {
                        c = 14;
                        break;
                    }
                    c = 65535;
                    break;
                case 353645753:
                    if (key.equals(KEY_EJ_LIMIT_RESTRICTED_MS)) {
                        c = 20;
                        break;
                    }
                    c = 65535;
                    break;
                case 353674834:
                    if (key.equals(KEY_EJ_LIMIT_RARE_MS)) {
                        c = 19;
                        break;
                    }
                    c = 65535;
                    break;
                case 515924943:
                    if (key.equals(KEY_EJ_LIMIT_ADDITION_INSTALLER_MS)) {
                        c = 22;
                        break;
                    }
                    c = 65535;
                    break;
                case 542719401:
                    if (key.equals(KEY_MAX_EXECUTION_TIME_MS)) {
                        c = 7;
                        break;
                    }
                    c = 65535;
                    break;
                case 659682264:
                    if (key.equals(KEY_EJ_GRACE_PERIOD_TOP_APP_MS)) {
                        c = '0';
                        break;
                    }
                    c = 65535;
                    break;
                case 1012217584:
                    if (key.equals(KEY_WINDOW_SIZE_WORKING_MS)) {
                        c = '\t';
                        break;
                    }
                    c = 65535;
                    break;
                case 1029123626:
                    if (key.equals(KEY_QUOTA_BUMP_ADDITIONAL_JOB_COUNT)) {
                        c = 25;
                        break;
                    }
                    c = 65535;
                    break;
                case 1070239943:
                    if (key.equals(KEY_MAX_SESSION_COUNT_ACTIVE)) {
                        c = '$';
                        break;
                    }
                    c = 65535;
                    break;
                case 1072854979:
                    if (key.equals(KEY_QUOTA_BUMP_ADDITIONAL_SESSION_COUNT)) {
                        c = 26;
                        break;
                    }
                    c = 65535;
                    break;
                case 1185201205:
                    if (key.equals(KEY_ALLOWED_TIME_PER_PERIOD_RARE_MS)) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case 1211719583:
                    if (key.equals(KEY_EJ_GRACE_PERIOD_TEMP_ALLOWLIST_MS)) {
                        c = '/';
                        break;
                    }
                    c = 65535;
                    break;
                case 1232643386:
                    if (key.equals(KEY_MIN_QUOTA_CHECK_DELAY_MS)) {
                        c = '*';
                        break;
                    }
                    c = 65535;
                    break;
                case 1415707953:
                    if (key.equals(KEY_IN_QUOTA_BUFFER_MS)) {
                        c = 6;
                        break;
                    }
                    c = 65535;
                    break;
                case 1416512063:
                    if (key.equals(KEY_MAX_SESSION_COUNT_EXEMPTED)) {
                        c = '#';
                        break;
                    }
                    c = 65535;
                    break;
                case 1504661904:
                    if (key.equals(KEY_MAX_SESSION_COUNT_WORKING)) {
                        c = '%';
                        break;
                    }
                    c = 65535;
                    break;
                case 1510141337:
                    if (key.equals(KEY_ALLOWED_TIME_PER_PERIOD_EXEMPTED_MS)) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 1572083493:
                    if (key.equals(KEY_EJ_LIMIT_WORKING_MS)) {
                        c = 17;
                        break;
                    }
                    c = 65535;
                    break;
                case 1737007281:
                    if (key.equals(KEY_EJ_REWARD_TOP_APP_MS)) {
                        c = ',';
                        break;
                    }
                    c = 65535;
                    break;
                case 1846826615:
                    if (key.equals(KEY_MAX_JOB_COUNT_WORKING)) {
                        c = 31;
                        break;
                    }
                    c = 65535;
                    break;
                case 1908515971:
                    if (key.equals(KEY_WINDOW_SIZE_ACTIVE_MS)) {
                        c = '\b';
                        break;
                    }
                    c = 65535;
                    break;
                case 1921715463:
                    if (key.equals(KEY_TIMING_SESSION_COALESCING_DURATION_MS)) {
                        c = ')';
                        break;
                    }
                    c = 65535;
                    break;
                case 1988481858:
                    if (key.equals(KEY_EJ_WINDOW_SIZE_MS)) {
                        c = 23;
                        break;
                    }
                    c = 65535;
                    break;
                case 2079805852:
                    if (key.equals(KEY_MAX_SESSION_COUNT_RESTRICTED)) {
                        c = '(';
                        break;
                    }
                    c = 65535;
                    break;
                case 2084297379:
                    if (key.equals(KEY_MAX_SESSION_COUNT_RARE)) {
                        c = '\'';
                        break;
                    }
                    c = 65535;
                    break;
                case 2133096527:
                    if (key.equals(KEY_MAX_SESSION_COUNT_FREQUENT)) {
                        c = '&';
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
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case '\b':
                case '\t':
                case '\n':
                case 11:
                case '\f':
                    updateExecutionPeriodConstantsLocked();
                    return;
                case '\r':
                case 14:
                case 15:
                    updateRateLimitingConstantsLocked();
                    return;
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                    updateEJLimitConstantsLocked();
                    return;
                case 24:
                case 25:
                case 26:
                case 27:
                case 28:
                    updateQuotaBumpConstantsLocked();
                    return;
                case 29:
                    int i = properties.getInt(key, 75);
                    this.MAX_JOB_COUNT_EXEMPTED = i;
                    int newExemptedMaxJobCount = Math.max(10, i);
                    if (QuotaController.this.mMaxBucketJobCounts[6] != newExemptedMaxJobCount) {
                        QuotaController.this.mMaxBucketJobCounts[6] = newExemptedMaxJobCount;
                        this.mShouldReevaluateConstraints = true;
                        return;
                    }
                    return;
                case 30:
                    int i2 = properties.getInt(key, 75);
                    this.MAX_JOB_COUNT_ACTIVE = i2;
                    int newActiveMaxJobCount = Math.max(10, i2);
                    if (QuotaController.this.mMaxBucketJobCounts[0] != newActiveMaxJobCount) {
                        QuotaController.this.mMaxBucketJobCounts[0] = newActiveMaxJobCount;
                        this.mShouldReevaluateConstraints = true;
                        return;
                    }
                    return;
                case 31:
                    int i3 = properties.getInt(key, 120);
                    this.MAX_JOB_COUNT_WORKING = i3;
                    int newWorkingMaxJobCount = Math.max(10, i3);
                    if (QuotaController.this.mMaxBucketJobCounts[1] != newWorkingMaxJobCount) {
                        QuotaController.this.mMaxBucketJobCounts[1] = newWorkingMaxJobCount;
                        this.mShouldReevaluateConstraints = true;
                        return;
                    }
                    return;
                case ' ':
                    int i4 = properties.getInt(key, 200);
                    this.MAX_JOB_COUNT_FREQUENT = i4;
                    int newFrequentMaxJobCount = Math.max(10, i4);
                    if (QuotaController.this.mMaxBucketJobCounts[2] != newFrequentMaxJobCount) {
                        QuotaController.this.mMaxBucketJobCounts[2] = newFrequentMaxJobCount;
                        this.mShouldReevaluateConstraints = true;
                        return;
                    }
                    return;
                case '!':
                    int i5 = properties.getInt(key, 48);
                    this.MAX_JOB_COUNT_RARE = i5;
                    int newRareMaxJobCount = Math.max(10, i5);
                    if (QuotaController.this.mMaxBucketJobCounts[3] != newRareMaxJobCount) {
                        QuotaController.this.mMaxBucketJobCounts[3] = newRareMaxJobCount;
                        this.mShouldReevaluateConstraints = true;
                        return;
                    }
                    return;
                case '\"':
                    int i6 = properties.getInt(key, 10);
                    this.MAX_JOB_COUNT_RESTRICTED = i6;
                    int newRestrictedMaxJobCount = Math.max(10, i6);
                    if (QuotaController.this.mMaxBucketJobCounts[5] != newRestrictedMaxJobCount) {
                        QuotaController.this.mMaxBucketJobCounts[5] = newRestrictedMaxJobCount;
                        this.mShouldReevaluateConstraints = true;
                        return;
                    }
                    return;
                case '#':
                    int i7 = properties.getInt(key, 75);
                    this.MAX_SESSION_COUNT_EXEMPTED = i7;
                    int newExemptedMaxSessionCount = Math.max(1, i7);
                    if (QuotaController.this.mMaxBucketSessionCounts[6] != newExemptedMaxSessionCount) {
                        QuotaController.this.mMaxBucketSessionCounts[6] = newExemptedMaxSessionCount;
                        this.mShouldReevaluateConstraints = true;
                        return;
                    }
                    return;
                case '$':
                    int i8 = properties.getInt(key, 75);
                    this.MAX_SESSION_COUNT_ACTIVE = i8;
                    int newActiveMaxSessionCount = Math.max(1, i8);
                    if (QuotaController.this.mMaxBucketSessionCounts[0] != newActiveMaxSessionCount) {
                        QuotaController.this.mMaxBucketSessionCounts[0] = newActiveMaxSessionCount;
                        this.mShouldReevaluateConstraints = true;
                        return;
                    }
                    return;
                case '%':
                    int i9 = properties.getInt(key, 10);
                    this.MAX_SESSION_COUNT_WORKING = i9;
                    int newWorkingMaxSessionCount = Math.max(1, i9);
                    if (QuotaController.this.mMaxBucketSessionCounts[1] != newWorkingMaxSessionCount) {
                        QuotaController.this.mMaxBucketSessionCounts[1] = newWorkingMaxSessionCount;
                        this.mShouldReevaluateConstraints = true;
                        return;
                    }
                    return;
                case '&':
                    int i10 = properties.getInt(key, 8);
                    this.MAX_SESSION_COUNT_FREQUENT = i10;
                    int newFrequentMaxSessionCount = Math.max(1, i10);
                    if (QuotaController.this.mMaxBucketSessionCounts[2] != newFrequentMaxSessionCount) {
                        QuotaController.this.mMaxBucketSessionCounts[2] = newFrequentMaxSessionCount;
                        this.mShouldReevaluateConstraints = true;
                        return;
                    }
                    return;
                case '\'':
                    int i11 = properties.getInt(key, 3);
                    this.MAX_SESSION_COUNT_RARE = i11;
                    int newRareMaxSessionCount = Math.max(1, i11);
                    if (QuotaController.this.mMaxBucketSessionCounts[3] != newRareMaxSessionCount) {
                        QuotaController.this.mMaxBucketSessionCounts[3] = newRareMaxSessionCount;
                        this.mShouldReevaluateConstraints = true;
                        return;
                    }
                    return;
                case '(':
                    int i12 = properties.getInt(key, 1);
                    this.MAX_SESSION_COUNT_RESTRICTED = i12;
                    int newRestrictedMaxSessionCount = Math.max(0, i12);
                    if (QuotaController.this.mMaxBucketSessionCounts[5] != newRestrictedMaxSessionCount) {
                        QuotaController.this.mMaxBucketSessionCounts[5] = newRestrictedMaxSessionCount;
                        this.mShouldReevaluateConstraints = true;
                        return;
                    }
                    return;
                case ')':
                    long j = properties.getLong(key, (long) DEFAULT_TIMING_SESSION_COALESCING_DURATION_MS);
                    this.TIMING_SESSION_COALESCING_DURATION_MS = j;
                    long newSessionCoalescingDurationMs = Math.min((long) DEFAULT_EJ_LIMIT_ADDITION_SPECIAL_MS, Math.max(0L, j));
                    if (QuotaController.this.mTimingSessionCoalescingDurationMs != newSessionCoalescingDurationMs) {
                        QuotaController.this.mTimingSessionCoalescingDurationMs = newSessionCoalescingDurationMs;
                        this.mShouldReevaluateConstraints = true;
                        return;
                    }
                    return;
                case '*':
                    this.MIN_QUOTA_CHECK_DELAY_MS = properties.getLong(key, 60000L);
                    QuotaController.this.mInQuotaAlarmQueue.setMinTimeBetweenAlarmsMs(Math.min((long) DEFAULT_EJ_LIMIT_ADDITION_SPECIAL_MS, Math.max(0L, this.MIN_QUOTA_CHECK_DELAY_MS)));
                    return;
                case '+':
                    long j2 = properties.getLong(key, 30000L);
                    this.EJ_TOP_APP_TIME_CHUNK_SIZE_MS = j2;
                    long newChunkSizeMs = Math.min((long) DEFAULT_EJ_LIMIT_ADDITION_SPECIAL_MS, Math.max(1L, j2));
                    if (QuotaController.this.mEJTopAppTimeChunkSizeMs != newChunkSizeMs) {
                        QuotaController.this.mEJTopAppTimeChunkSizeMs = newChunkSizeMs;
                        if (QuotaController.this.mEJTopAppTimeChunkSizeMs < QuotaController.this.mEJRewardTopAppMs) {
                            Slog.w(QuotaController.TAG, "EJ top app time chunk less than reward: " + QuotaController.this.mEJTopAppTimeChunkSizeMs + " vs " + QuotaController.this.mEJRewardTopAppMs);
                            return;
                        }
                        return;
                    }
                    return;
                case ',':
                    long j3 = properties.getLong(key, 10000L);
                    this.EJ_REWARD_TOP_APP_MS = j3;
                    long newTopReward = Math.min((long) DEFAULT_EJ_LIMIT_ADDITION_SPECIAL_MS, Math.max(10000L, j3));
                    if (QuotaController.this.mEJRewardTopAppMs != newTopReward) {
                        QuotaController.this.mEJRewardTopAppMs = newTopReward;
                        if (QuotaController.this.mEJTopAppTimeChunkSizeMs < QuotaController.this.mEJRewardTopAppMs) {
                            Slog.w(QuotaController.TAG, "EJ top app time chunk less than reward: " + QuotaController.this.mEJTopAppTimeChunkSizeMs + " vs " + QuotaController.this.mEJRewardTopAppMs);
                            return;
                        }
                        return;
                    }
                    return;
                case '-':
                    long j4 = properties.getLong(key, (long) DEFAULT_EJ_REWARD_INTERACTION_MS);
                    this.EJ_REWARD_INTERACTION_MS = j4;
                    QuotaController.this.mEJRewardInteractionMs = Math.min((long) DEFAULT_EJ_LIMIT_ADDITION_SPECIAL_MS, Math.max((long) DEFAULT_TIMING_SESSION_COALESCING_DURATION_MS, j4));
                    return;
                case '.':
                    long j5 = properties.getLong(key, 0L);
                    this.EJ_REWARD_NOTIFICATION_SEEN_MS = j5;
                    QuotaController.this.mEJRewardNotificationSeenMs = Math.min(300000L, Math.max(0L, j5));
                    return;
                case '/':
                    long j6 = properties.getLong(key, 180000L);
                    this.EJ_GRACE_PERIOD_TEMP_ALLOWLIST_MS = j6;
                    QuotaController.this.mEJGracePeriodTempAllowlistMs = Math.min(3600000L, Math.max(0L, j6));
                    return;
                case '0':
                    long j7 = properties.getLong(key, 60000L);
                    this.EJ_GRACE_PERIOD_TOP_APP_MS = j7;
                    QuotaController.this.mEJGracePeriodTopAppMs = Math.min(3600000L, Math.max(0L, j7));
                    return;
                default:
                    return;
            }
        }

        /* JADX WARN: Type inference failed for: r5v31 */
        /* JADX WARN: Type inference failed for: r5v32, types: [boolean] */
        /* JADX WARN: Type inference failed for: r5v55 */
        private void updateExecutionPeriodConstantsLocked() {
            ?? r5;
            if (!this.mExecutionPeriodConstantsUpdated) {
                this.mExecutionPeriodConstantsUpdated = true;
                DeviceConfig.Properties properties = DeviceConfig.getProperties("jobscheduler", new String[]{KEY_ALLOWED_TIME_PER_PERIOD_EXEMPTED_MS, KEY_ALLOWED_TIME_PER_PERIOD_ACTIVE_MS, KEY_ALLOWED_TIME_PER_PERIOD_WORKING_MS, KEY_ALLOWED_TIME_PER_PERIOD_FREQUENT_MS, KEY_ALLOWED_TIME_PER_PERIOD_RARE_MS, KEY_ALLOWED_TIME_PER_PERIOD_RESTRICTED_MS, KEY_IN_QUOTA_BUFFER_MS, KEY_MAX_EXECUTION_TIME_MS, KEY_WINDOW_SIZE_EXEMPTED_MS, KEY_WINDOW_SIZE_ACTIVE_MS, KEY_WINDOW_SIZE_WORKING_MS, KEY_WINDOW_SIZE_FREQUENT_MS, KEY_WINDOW_SIZE_RARE_MS, KEY_WINDOW_SIZE_RESTRICTED_MS});
                this.ALLOWED_TIME_PER_PERIOD_EXEMPTED_MS = properties.getLong(KEY_ALLOWED_TIME_PER_PERIOD_EXEMPTED_MS, 600000L);
                this.ALLOWED_TIME_PER_PERIOD_ACTIVE_MS = properties.getLong(KEY_ALLOWED_TIME_PER_PERIOD_ACTIVE_MS, 600000L);
                this.ALLOWED_TIME_PER_PERIOD_WORKING_MS = properties.getLong(KEY_ALLOWED_TIME_PER_PERIOD_WORKING_MS, 600000L);
                this.ALLOWED_TIME_PER_PERIOD_FREQUENT_MS = properties.getLong(KEY_ALLOWED_TIME_PER_PERIOD_FREQUENT_MS, 600000L);
                this.ALLOWED_TIME_PER_PERIOD_RARE_MS = properties.getLong(KEY_ALLOWED_TIME_PER_PERIOD_RARE_MS, 600000L);
                this.ALLOWED_TIME_PER_PERIOD_RESTRICTED_MS = properties.getLong(KEY_ALLOWED_TIME_PER_PERIOD_RESTRICTED_MS, 600000L);
                this.IN_QUOTA_BUFFER_MS = properties.getLong(KEY_IN_QUOTA_BUFFER_MS, 30000L);
                this.MAX_EXECUTION_TIME_MS = properties.getLong(KEY_MAX_EXECUTION_TIME_MS, 14400000L);
                this.WINDOW_SIZE_EXEMPTED_MS = properties.getLong(KEY_WINDOW_SIZE_EXEMPTED_MS, 600000L);
                this.WINDOW_SIZE_ACTIVE_MS = properties.getLong(KEY_WINDOW_SIZE_ACTIVE_MS, 600000L);
                this.WINDOW_SIZE_WORKING_MS = properties.getLong(KEY_WINDOW_SIZE_WORKING_MS, 7200000L);
                this.WINDOW_SIZE_FREQUENT_MS = properties.getLong(KEY_WINDOW_SIZE_FREQUENT_MS, 28800000L);
                this.WINDOW_SIZE_RARE_MS = properties.getLong(KEY_WINDOW_SIZE_RARE_MS, 86400000L);
                this.WINDOW_SIZE_RESTRICTED_MS = properties.getLong(KEY_WINDOW_SIZE_RESTRICTED_MS, 86400000L);
                long newMaxExecutionTimeMs = Math.max(3600000L, Math.min(86400000L, this.MAX_EXECUTION_TIME_MS));
                if (QuotaController.this.mMaxExecutionTimeMs != newMaxExecutionTimeMs) {
                    QuotaController.this.mMaxExecutionTimeMs = newMaxExecutionTimeMs;
                    QuotaController quotaController = QuotaController.this;
                    quotaController.mMaxExecutionTimeIntoQuotaMs = quotaController.mMaxExecutionTimeMs - QuotaController.this.mQuotaBufferMs;
                    this.mShouldReevaluateConstraints = true;
                }
                long newAllowedTimeExemptedMs = Math.min(QuotaController.this.mMaxExecutionTimeMs, Math.max(60000L, this.ALLOWED_TIME_PER_PERIOD_EXEMPTED_MS));
                long minAllowedTimeMs = Math.min((long) JobStatus.NO_LATEST_RUNTIME, newAllowedTimeExemptedMs);
                if (QuotaController.this.mAllowedTimePerPeriodMs[6] != newAllowedTimeExemptedMs) {
                    QuotaController.this.mAllowedTimePerPeriodMs[6] = newAllowedTimeExemptedMs;
                    this.mShouldReevaluateConstraints = true;
                }
                long newAllowedTimeActiveMs = Math.min(QuotaController.this.mMaxExecutionTimeMs, Math.max(60000L, this.ALLOWED_TIME_PER_PERIOD_ACTIVE_MS));
                long minAllowedTimeMs2 = Math.min(minAllowedTimeMs, newAllowedTimeActiveMs);
                if (QuotaController.this.mAllowedTimePerPeriodMs[0] != newAllowedTimeActiveMs) {
                    QuotaController.this.mAllowedTimePerPeriodMs[0] = newAllowedTimeActiveMs;
                    this.mShouldReevaluateConstraints = true;
                }
                long newAllowedTimeWorkingMs = Math.min(QuotaController.this.mMaxExecutionTimeMs, Math.max(60000L, this.ALLOWED_TIME_PER_PERIOD_WORKING_MS));
                long minAllowedTimeMs3 = Math.min(minAllowedTimeMs2, newAllowedTimeWorkingMs);
                if (QuotaController.this.mAllowedTimePerPeriodMs[1] != newAllowedTimeWorkingMs) {
                    QuotaController.this.mAllowedTimePerPeriodMs[1] = newAllowedTimeWorkingMs;
                    this.mShouldReevaluateConstraints = true;
                }
                long newAllowedTimeFrequentMs = Math.min(QuotaController.this.mMaxExecutionTimeMs, Math.max(60000L, this.ALLOWED_TIME_PER_PERIOD_FREQUENT_MS));
                long minAllowedTimeMs4 = Math.min(minAllowedTimeMs3, newAllowedTimeFrequentMs);
                if (QuotaController.this.mAllowedTimePerPeriodMs[2] != newAllowedTimeFrequentMs) {
                    QuotaController.this.mAllowedTimePerPeriodMs[2] = newAllowedTimeFrequentMs;
                    this.mShouldReevaluateConstraints = true;
                }
                long newAllowedTimeRareMs = Math.min(QuotaController.this.mMaxExecutionTimeMs, Math.max(60000L, this.ALLOWED_TIME_PER_PERIOD_RARE_MS));
                long minAllowedTimeMs5 = Math.min(minAllowedTimeMs4, newAllowedTimeRareMs);
                if (QuotaController.this.mAllowedTimePerPeriodMs[3] != newAllowedTimeRareMs) {
                    QuotaController.this.mAllowedTimePerPeriodMs[3] = newAllowedTimeRareMs;
                    this.mShouldReevaluateConstraints = true;
                }
                long newAllowedTimeRestrictedMs = Math.min(QuotaController.this.mMaxExecutionTimeMs, Math.max(60000L, this.ALLOWED_TIME_PER_PERIOD_RESTRICTED_MS));
                long minAllowedTimeMs6 = Math.min(minAllowedTimeMs5, newAllowedTimeRestrictedMs);
                if (QuotaController.this.mAllowedTimePerPeriodMs[5] != newAllowedTimeRestrictedMs) {
                    QuotaController.this.mAllowedTimePerPeriodMs[5] = newAllowedTimeRestrictedMs;
                    this.mShouldReevaluateConstraints = true;
                }
                long newQuotaBufferMs = Math.max(0L, Math.min(minAllowedTimeMs6, Math.min(300000L, this.IN_QUOTA_BUFFER_MS)));
                if (QuotaController.this.mQuotaBufferMs != newQuotaBufferMs) {
                    QuotaController.this.mQuotaBufferMs = newQuotaBufferMs;
                    QuotaController quotaController2 = QuotaController.this;
                    quotaController2.mMaxExecutionTimeIntoQuotaMs = quotaController2.mMaxExecutionTimeMs - QuotaController.this.mQuotaBufferMs;
                    this.mShouldReevaluateConstraints = true;
                }
                long newExemptedPeriodMs = Math.max(QuotaController.this.mAllowedTimePerPeriodMs[6], Math.min(86400000L, this.WINDOW_SIZE_EXEMPTED_MS));
                if (QuotaController.this.mBucketPeriodsMs[6] != newExemptedPeriodMs) {
                    QuotaController.this.mBucketPeriodsMs[6] = newExemptedPeriodMs;
                    this.mShouldReevaluateConstraints = true;
                }
                long newActivePeriodMs = Math.max(QuotaController.this.mAllowedTimePerPeriodMs[0], Math.min(86400000L, this.WINDOW_SIZE_ACTIVE_MS));
                if (QuotaController.this.mBucketPeriodsMs[0] == newActivePeriodMs) {
                    r5 = 1;
                } else {
                    QuotaController.this.mBucketPeriodsMs[0] = newActivePeriodMs;
                    r5 = 1;
                    this.mShouldReevaluateConstraints = true;
                }
                long newWorkingPeriodMs = Math.max(QuotaController.this.mAllowedTimePerPeriodMs[r5], Math.min(86400000L, this.WINDOW_SIZE_WORKING_MS));
                if (QuotaController.this.mBucketPeriodsMs[r5] != newWorkingPeriodMs) {
                    QuotaController.this.mBucketPeriodsMs[r5] = newWorkingPeriodMs;
                    this.mShouldReevaluateConstraints = r5;
                }
                long newFrequentPeriodMs = Math.max(QuotaController.this.mAllowedTimePerPeriodMs[2], Math.min(86400000L, this.WINDOW_SIZE_FREQUENT_MS));
                if (QuotaController.this.mBucketPeriodsMs[2] != newFrequentPeriodMs) {
                    QuotaController.this.mBucketPeriodsMs[2] = newFrequentPeriodMs;
                    this.mShouldReevaluateConstraints = true;
                }
                long newRarePeriodMs = Math.max(QuotaController.this.mAllowedTimePerPeriodMs[3], Math.min(86400000L, this.WINDOW_SIZE_RARE_MS));
                if (QuotaController.this.mBucketPeriodsMs[3] != newRarePeriodMs) {
                    QuotaController.this.mBucketPeriodsMs[3] = newRarePeriodMs;
                    this.mShouldReevaluateConstraints = true;
                }
                long newRestrictedPeriodMs = Math.max(QuotaController.this.mAllowedTimePerPeriodMs[5], Math.min((long) UnixCalendar.WEEK_IN_MILLIS, this.WINDOW_SIZE_RESTRICTED_MS));
                if (QuotaController.this.mBucketPeriodsMs[5] != newRestrictedPeriodMs) {
                    QuotaController.this.mBucketPeriodsMs[5] = newRestrictedPeriodMs;
                    this.mShouldReevaluateConstraints = true;
                }
            }
        }

        private void updateRateLimitingConstantsLocked() {
            if (this.mRateLimitingConstantsUpdated) {
                return;
            }
            this.mRateLimitingConstantsUpdated = true;
            DeviceConfig.Properties properties = DeviceConfig.getProperties("jobscheduler", new String[]{KEY_RATE_LIMITING_WINDOW_MS, KEY_MAX_JOB_COUNT_PER_RATE_LIMITING_WINDOW, KEY_MAX_SESSION_COUNT_PER_RATE_LIMITING_WINDOW});
            this.RATE_LIMITING_WINDOW_MS = properties.getLong(KEY_RATE_LIMITING_WINDOW_MS, 60000L);
            this.MAX_JOB_COUNT_PER_RATE_LIMITING_WINDOW = properties.getInt(KEY_MAX_JOB_COUNT_PER_RATE_LIMITING_WINDOW, 20);
            this.MAX_SESSION_COUNT_PER_RATE_LIMITING_WINDOW = properties.getInt(KEY_MAX_SESSION_COUNT_PER_RATE_LIMITING_WINDOW, 20);
            long newRateLimitingWindowMs = Math.min(86400000L, Math.max(30000L, this.RATE_LIMITING_WINDOW_MS));
            if (QuotaController.this.mRateLimitingWindowMs != newRateLimitingWindowMs) {
                QuotaController.this.mRateLimitingWindowMs = newRateLimitingWindowMs;
                this.mShouldReevaluateConstraints = true;
            }
            int newMaxJobCountPerRateLimitingWindow = Math.max(10, this.MAX_JOB_COUNT_PER_RATE_LIMITING_WINDOW);
            if (QuotaController.this.mMaxJobCountPerRateLimitingWindow != newMaxJobCountPerRateLimitingWindow) {
                QuotaController.this.mMaxJobCountPerRateLimitingWindow = newMaxJobCountPerRateLimitingWindow;
                this.mShouldReevaluateConstraints = true;
            }
            int newMaxSessionCountPerRateLimitPeriod = Math.max(10, this.MAX_SESSION_COUNT_PER_RATE_LIMITING_WINDOW);
            if (QuotaController.this.mMaxSessionCountPerRateLimitingWindow != newMaxSessionCountPerRateLimitPeriod) {
                QuotaController.this.mMaxSessionCountPerRateLimitingWindow = newMaxSessionCountPerRateLimitPeriod;
                this.mShouldReevaluateConstraints = true;
            }
        }

        private void updateEJLimitConstantsLocked() {
            if (!this.mEJLimitConstantsUpdated) {
                this.mEJLimitConstantsUpdated = true;
                DeviceConfig.Properties properties = DeviceConfig.getProperties("jobscheduler", new String[]{KEY_EJ_LIMIT_EXEMPTED_MS, KEY_EJ_LIMIT_ACTIVE_MS, KEY_EJ_LIMIT_WORKING_MS, KEY_EJ_LIMIT_FREQUENT_MS, KEY_EJ_LIMIT_RARE_MS, KEY_EJ_LIMIT_RESTRICTED_MS, KEY_EJ_LIMIT_ADDITION_SPECIAL_MS, KEY_EJ_LIMIT_ADDITION_INSTALLER_MS, KEY_EJ_WINDOW_SIZE_MS});
                this.EJ_LIMIT_EXEMPTED_MS = properties.getLong(KEY_EJ_LIMIT_EXEMPTED_MS, (long) DEFAULT_EJ_LIMIT_EXEMPTED_MS);
                this.EJ_LIMIT_ACTIVE_MS = properties.getLong(KEY_EJ_LIMIT_ACTIVE_MS, 1800000L);
                this.EJ_LIMIT_WORKING_MS = properties.getLong(KEY_EJ_LIMIT_WORKING_MS, 1800000L);
                this.EJ_LIMIT_FREQUENT_MS = properties.getLong(KEY_EJ_LIMIT_FREQUENT_MS, 600000L);
                this.EJ_LIMIT_RARE_MS = properties.getLong(KEY_EJ_LIMIT_RARE_MS, 600000L);
                this.EJ_LIMIT_RESTRICTED_MS = properties.getLong(KEY_EJ_LIMIT_RESTRICTED_MS, 300000L);
                this.EJ_LIMIT_ADDITION_INSTALLER_MS = properties.getLong(KEY_EJ_LIMIT_ADDITION_INSTALLER_MS, 1800000L);
                this.EJ_LIMIT_ADDITION_SPECIAL_MS = properties.getLong(KEY_EJ_LIMIT_ADDITION_SPECIAL_MS, (long) DEFAULT_EJ_LIMIT_ADDITION_SPECIAL_MS);
                long j = properties.getLong(KEY_EJ_WINDOW_SIZE_MS, 86400000L);
                this.EJ_WINDOW_SIZE_MS = j;
                long newWindowSizeMs = Math.max(3600000L, Math.min(86400000L, j));
                if (QuotaController.this.mEJLimitWindowSizeMs != newWindowSizeMs) {
                    QuotaController.this.mEJLimitWindowSizeMs = newWindowSizeMs;
                    this.mShouldReevaluateConstraints = true;
                }
                long newExemptLimitMs = Math.max((long) DEFAULT_EJ_LIMIT_ADDITION_SPECIAL_MS, Math.min(newWindowSizeMs, this.EJ_LIMIT_EXEMPTED_MS));
                if (QuotaController.this.mEJLimitsMs[6] != newExemptLimitMs) {
                    QuotaController.this.mEJLimitsMs[6] = newExemptLimitMs;
                    this.mShouldReevaluateConstraints = true;
                }
                long newActiveLimitMs = Math.max((long) DEFAULT_EJ_LIMIT_ADDITION_SPECIAL_MS, Math.min(newExemptLimitMs, this.EJ_LIMIT_ACTIVE_MS));
                if (QuotaController.this.mEJLimitsMs[0] != newActiveLimitMs) {
                    QuotaController.this.mEJLimitsMs[0] = newActiveLimitMs;
                    this.mShouldReevaluateConstraints = true;
                }
                long newWorkingLimitMs = Math.max((long) DEFAULT_EJ_LIMIT_ADDITION_SPECIAL_MS, Math.min(newActiveLimitMs, this.EJ_LIMIT_WORKING_MS));
                if (QuotaController.this.mEJLimitsMs[1] != newWorkingLimitMs) {
                    QuotaController.this.mEJLimitsMs[1] = newWorkingLimitMs;
                    this.mShouldReevaluateConstraints = true;
                }
                long newFrequentLimitMs = Math.max(600000L, Math.min(newWorkingLimitMs, this.EJ_LIMIT_FREQUENT_MS));
                if (QuotaController.this.mEJLimitsMs[2] != newFrequentLimitMs) {
                    QuotaController.this.mEJLimitsMs[2] = newFrequentLimitMs;
                    this.mShouldReevaluateConstraints = true;
                }
                long newRareLimitMs = Math.max(600000L, Math.min(newFrequentLimitMs, this.EJ_LIMIT_RARE_MS));
                if (QuotaController.this.mEJLimitsMs[3] != newRareLimitMs) {
                    QuotaController.this.mEJLimitsMs[3] = newRareLimitMs;
                    this.mShouldReevaluateConstraints = true;
                }
                long newRestrictedLimitMs = Math.max(300000L, Math.min(newRareLimitMs, this.EJ_LIMIT_RESTRICTED_MS));
                if (QuotaController.this.mEJLimitsMs[5] != newRestrictedLimitMs) {
                    QuotaController.this.mEJLimitsMs[5] = newRestrictedLimitMs;
                    this.mShouldReevaluateConstraints = true;
                }
                long newAdditionInstallerMs = Math.max(0L, Math.min(newWindowSizeMs - newActiveLimitMs, this.EJ_LIMIT_ADDITION_INSTALLER_MS));
                if (QuotaController.this.mEjLimitAdditionInstallerMs != newAdditionInstallerMs) {
                    QuotaController.this.mEjLimitAdditionInstallerMs = newAdditionInstallerMs;
                    this.mShouldReevaluateConstraints = true;
                }
                long newAdditionSpecialMs = Math.max(0L, Math.min(newWindowSizeMs - newActiveLimitMs, this.EJ_LIMIT_ADDITION_SPECIAL_MS));
                if (QuotaController.this.mEjLimitAdditionSpecialMs != newAdditionSpecialMs) {
                    QuotaController.this.mEjLimitAdditionSpecialMs = newAdditionSpecialMs;
                    this.mShouldReevaluateConstraints = true;
                }
            }
        }

        private void updateQuotaBumpConstantsLocked() {
            if (this.mQuotaBumpConstantsUpdated) {
                return;
            }
            this.mQuotaBumpConstantsUpdated = true;
            DeviceConfig.Properties properties = DeviceConfig.getProperties("jobscheduler", new String[]{KEY_QUOTA_BUMP_ADDITIONAL_DURATION_MS, KEY_QUOTA_BUMP_ADDITIONAL_JOB_COUNT, KEY_QUOTA_BUMP_ADDITIONAL_SESSION_COUNT, KEY_QUOTA_BUMP_WINDOW_SIZE_MS, KEY_QUOTA_BUMP_LIMIT});
            this.QUOTA_BUMP_ADDITIONAL_DURATION_MS = properties.getLong(KEY_QUOTA_BUMP_ADDITIONAL_DURATION_MS, 60000L);
            this.QUOTA_BUMP_ADDITIONAL_JOB_COUNT = properties.getInt(KEY_QUOTA_BUMP_ADDITIONAL_JOB_COUNT, 2);
            this.QUOTA_BUMP_ADDITIONAL_SESSION_COUNT = properties.getInt(KEY_QUOTA_BUMP_ADDITIONAL_SESSION_COUNT, 1);
            this.QUOTA_BUMP_WINDOW_SIZE_MS = properties.getLong(KEY_QUOTA_BUMP_WINDOW_SIZE_MS, 28800000L);
            this.QUOTA_BUMP_LIMIT = properties.getInt(KEY_QUOTA_BUMP_LIMIT, 8);
            long newWindowSizeMs = Math.max(3600000L, Math.min(86400000L, this.QUOTA_BUMP_WINDOW_SIZE_MS));
            if (QuotaController.this.mQuotaBumpWindowSizeMs != newWindowSizeMs) {
                QuotaController.this.mQuotaBumpWindowSizeMs = newWindowSizeMs;
                this.mShouldReevaluateConstraints = true;
            }
            int newLimit = Math.max(0, this.QUOTA_BUMP_LIMIT);
            if (QuotaController.this.mQuotaBumpLimit != newLimit) {
                QuotaController.this.mQuotaBumpLimit = newLimit;
                this.mShouldReevaluateConstraints = true;
            }
            int newJobAddition = Math.max(0, this.QUOTA_BUMP_ADDITIONAL_JOB_COUNT);
            if (QuotaController.this.mQuotaBumpAdditionalJobCount != newJobAddition) {
                QuotaController.this.mQuotaBumpAdditionalJobCount = newJobAddition;
                this.mShouldReevaluateConstraints = true;
            }
            int newSessionAddition = Math.max(0, this.QUOTA_BUMP_ADDITIONAL_SESSION_COUNT);
            if (QuotaController.this.mQuotaBumpAdditionalSessionCount != newSessionAddition) {
                QuotaController.this.mQuotaBumpAdditionalSessionCount = newSessionAddition;
                this.mShouldReevaluateConstraints = true;
            }
            long newAdditionalDuration = Math.max(0L, Math.min(600000L, this.QUOTA_BUMP_ADDITIONAL_DURATION_MS));
            if (QuotaController.this.mQuotaBumpAdditionalDurationMs != newAdditionalDuration) {
                QuotaController.this.mQuotaBumpAdditionalDurationMs = newAdditionalDuration;
                this.mShouldReevaluateConstraints = true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dump(IndentingPrintWriter pw) {
            pw.println();
            pw.println("QuotaController:");
            pw.increaseIndent();
            pw.print(KEY_ALLOWED_TIME_PER_PERIOD_EXEMPTED_MS, Long.valueOf(this.ALLOWED_TIME_PER_PERIOD_EXEMPTED_MS)).println();
            pw.print(KEY_ALLOWED_TIME_PER_PERIOD_ACTIVE_MS, Long.valueOf(this.ALLOWED_TIME_PER_PERIOD_ACTIVE_MS)).println();
            pw.print(KEY_ALLOWED_TIME_PER_PERIOD_WORKING_MS, Long.valueOf(this.ALLOWED_TIME_PER_PERIOD_WORKING_MS)).println();
            pw.print(KEY_ALLOWED_TIME_PER_PERIOD_FREQUENT_MS, Long.valueOf(this.ALLOWED_TIME_PER_PERIOD_FREQUENT_MS)).println();
            pw.print(KEY_ALLOWED_TIME_PER_PERIOD_RARE_MS, Long.valueOf(this.ALLOWED_TIME_PER_PERIOD_RARE_MS)).println();
            pw.print(KEY_ALLOWED_TIME_PER_PERIOD_RESTRICTED_MS, Long.valueOf(this.ALLOWED_TIME_PER_PERIOD_RESTRICTED_MS)).println();
            pw.print(KEY_IN_QUOTA_BUFFER_MS, Long.valueOf(this.IN_QUOTA_BUFFER_MS)).println();
            pw.print(KEY_WINDOW_SIZE_EXEMPTED_MS, Long.valueOf(this.WINDOW_SIZE_EXEMPTED_MS)).println();
            pw.print(KEY_WINDOW_SIZE_ACTIVE_MS, Long.valueOf(this.WINDOW_SIZE_ACTIVE_MS)).println();
            pw.print(KEY_WINDOW_SIZE_WORKING_MS, Long.valueOf(this.WINDOW_SIZE_WORKING_MS)).println();
            pw.print(KEY_WINDOW_SIZE_FREQUENT_MS, Long.valueOf(this.WINDOW_SIZE_FREQUENT_MS)).println();
            pw.print(KEY_WINDOW_SIZE_RARE_MS, Long.valueOf(this.WINDOW_SIZE_RARE_MS)).println();
            pw.print(KEY_WINDOW_SIZE_RESTRICTED_MS, Long.valueOf(this.WINDOW_SIZE_RESTRICTED_MS)).println();
            pw.print(KEY_MAX_EXECUTION_TIME_MS, Long.valueOf(this.MAX_EXECUTION_TIME_MS)).println();
            pw.print(KEY_MAX_JOB_COUNT_EXEMPTED, Integer.valueOf(this.MAX_JOB_COUNT_EXEMPTED)).println();
            pw.print(KEY_MAX_JOB_COUNT_ACTIVE, Integer.valueOf(this.MAX_JOB_COUNT_ACTIVE)).println();
            pw.print(KEY_MAX_JOB_COUNT_WORKING, Integer.valueOf(this.MAX_JOB_COUNT_WORKING)).println();
            pw.print(KEY_MAX_JOB_COUNT_FREQUENT, Integer.valueOf(this.MAX_JOB_COUNT_FREQUENT)).println();
            pw.print(KEY_MAX_JOB_COUNT_RARE, Integer.valueOf(this.MAX_JOB_COUNT_RARE)).println();
            pw.print(KEY_MAX_JOB_COUNT_RESTRICTED, Integer.valueOf(this.MAX_JOB_COUNT_RESTRICTED)).println();
            pw.print(KEY_RATE_LIMITING_WINDOW_MS, Long.valueOf(this.RATE_LIMITING_WINDOW_MS)).println();
            pw.print(KEY_MAX_JOB_COUNT_PER_RATE_LIMITING_WINDOW, Integer.valueOf(this.MAX_JOB_COUNT_PER_RATE_LIMITING_WINDOW)).println();
            pw.print(KEY_MAX_SESSION_COUNT_EXEMPTED, Integer.valueOf(this.MAX_SESSION_COUNT_EXEMPTED)).println();
            pw.print(KEY_MAX_SESSION_COUNT_ACTIVE, Integer.valueOf(this.MAX_SESSION_COUNT_ACTIVE)).println();
            pw.print(KEY_MAX_SESSION_COUNT_WORKING, Integer.valueOf(this.MAX_SESSION_COUNT_WORKING)).println();
            pw.print(KEY_MAX_SESSION_COUNT_FREQUENT, Integer.valueOf(this.MAX_SESSION_COUNT_FREQUENT)).println();
            pw.print(KEY_MAX_SESSION_COUNT_RARE, Integer.valueOf(this.MAX_SESSION_COUNT_RARE)).println();
            pw.print(KEY_MAX_SESSION_COUNT_RESTRICTED, Integer.valueOf(this.MAX_SESSION_COUNT_RESTRICTED)).println();
            pw.print(KEY_MAX_SESSION_COUNT_PER_RATE_LIMITING_WINDOW, Integer.valueOf(this.MAX_SESSION_COUNT_PER_RATE_LIMITING_WINDOW)).println();
            pw.print(KEY_TIMING_SESSION_COALESCING_DURATION_MS, Long.valueOf(this.TIMING_SESSION_COALESCING_DURATION_MS)).println();
            pw.print(KEY_MIN_QUOTA_CHECK_DELAY_MS, Long.valueOf(this.MIN_QUOTA_CHECK_DELAY_MS)).println();
            pw.print(KEY_EJ_LIMIT_EXEMPTED_MS, Long.valueOf(this.EJ_LIMIT_EXEMPTED_MS)).println();
            pw.print(KEY_EJ_LIMIT_ACTIVE_MS, Long.valueOf(this.EJ_LIMIT_ACTIVE_MS)).println();
            pw.print(KEY_EJ_LIMIT_WORKING_MS, Long.valueOf(this.EJ_LIMIT_WORKING_MS)).println();
            pw.print(KEY_EJ_LIMIT_FREQUENT_MS, Long.valueOf(this.EJ_LIMIT_FREQUENT_MS)).println();
            pw.print(KEY_EJ_LIMIT_RARE_MS, Long.valueOf(this.EJ_LIMIT_RARE_MS)).println();
            pw.print(KEY_EJ_LIMIT_RESTRICTED_MS, Long.valueOf(this.EJ_LIMIT_RESTRICTED_MS)).println();
            pw.print(KEY_EJ_LIMIT_ADDITION_INSTALLER_MS, Long.valueOf(this.EJ_LIMIT_ADDITION_INSTALLER_MS)).println();
            pw.print(KEY_EJ_LIMIT_ADDITION_SPECIAL_MS, Long.valueOf(this.EJ_LIMIT_ADDITION_SPECIAL_MS)).println();
            pw.print(KEY_EJ_WINDOW_SIZE_MS, Long.valueOf(this.EJ_WINDOW_SIZE_MS)).println();
            pw.print(KEY_EJ_TOP_APP_TIME_CHUNK_SIZE_MS, Long.valueOf(this.EJ_TOP_APP_TIME_CHUNK_SIZE_MS)).println();
            pw.print(KEY_EJ_REWARD_TOP_APP_MS, Long.valueOf(this.EJ_REWARD_TOP_APP_MS)).println();
            pw.print(KEY_EJ_REWARD_INTERACTION_MS, Long.valueOf(this.EJ_REWARD_INTERACTION_MS)).println();
            pw.print(KEY_EJ_REWARD_NOTIFICATION_SEEN_MS, Long.valueOf(this.EJ_REWARD_NOTIFICATION_SEEN_MS)).println();
            pw.print(KEY_EJ_GRACE_PERIOD_TEMP_ALLOWLIST_MS, Long.valueOf(this.EJ_GRACE_PERIOD_TEMP_ALLOWLIST_MS)).println();
            pw.print(KEY_EJ_GRACE_PERIOD_TOP_APP_MS, Long.valueOf(this.EJ_GRACE_PERIOD_TOP_APP_MS)).println();
            pw.print(KEY_QUOTA_BUMP_ADDITIONAL_DURATION_MS, Long.valueOf(this.QUOTA_BUMP_ADDITIONAL_DURATION_MS)).println();
            pw.print(KEY_QUOTA_BUMP_ADDITIONAL_JOB_COUNT, Integer.valueOf(this.QUOTA_BUMP_ADDITIONAL_JOB_COUNT)).println();
            pw.print(KEY_QUOTA_BUMP_ADDITIONAL_SESSION_COUNT, Integer.valueOf(this.QUOTA_BUMP_ADDITIONAL_SESSION_COUNT)).println();
            pw.print(KEY_QUOTA_BUMP_WINDOW_SIZE_MS, Long.valueOf(this.QUOTA_BUMP_WINDOW_SIZE_MS)).println();
            pw.print(KEY_QUOTA_BUMP_LIMIT, Integer.valueOf(this.QUOTA_BUMP_LIMIT)).println();
            pw.decreaseIndent();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dump(ProtoOutputStream proto) {
            long qcToken = proto.start(1146756268056L);
            proto.write(1112396529666L, this.IN_QUOTA_BUFFER_MS);
            proto.write(1112396529667L, this.WINDOW_SIZE_ACTIVE_MS);
            proto.write(1112396529668L, this.WINDOW_SIZE_WORKING_MS);
            proto.write(1112396529669L, this.WINDOW_SIZE_FREQUENT_MS);
            proto.write(1112396529670L, this.WINDOW_SIZE_RARE_MS);
            proto.write(1112396529684L, this.WINDOW_SIZE_RESTRICTED_MS);
            proto.write(1112396529671L, this.MAX_EXECUTION_TIME_MS);
            proto.write(1120986464264L, this.MAX_JOB_COUNT_ACTIVE);
            proto.write(1120986464265L, this.MAX_JOB_COUNT_WORKING);
            proto.write(1120986464266L, this.MAX_JOB_COUNT_FREQUENT);
            proto.write(1120986464267L, this.MAX_JOB_COUNT_RARE);
            proto.write(1120986464277L, this.MAX_JOB_COUNT_RESTRICTED);
            proto.write(1120986464275L, this.RATE_LIMITING_WINDOW_MS);
            proto.write(1120986464268L, this.MAX_JOB_COUNT_PER_RATE_LIMITING_WINDOW);
            proto.write(1120986464269L, this.MAX_SESSION_COUNT_ACTIVE);
            proto.write(1120986464270L, this.MAX_SESSION_COUNT_WORKING);
            proto.write(1120986464271L, this.MAX_SESSION_COUNT_FREQUENT);
            proto.write(1120986464272L, this.MAX_SESSION_COUNT_RARE);
            proto.write(1120986464278L, this.MAX_SESSION_COUNT_RESTRICTED);
            proto.write(1120986464273L, this.MAX_SESSION_COUNT_PER_RATE_LIMITING_WINDOW);
            proto.write(1112396529682L, this.TIMING_SESSION_COALESCING_DURATION_MS);
            proto.write(1112396529687L, this.MIN_QUOTA_CHECK_DELAY_MS);
            proto.write(1112396529688L, this.EJ_LIMIT_ACTIVE_MS);
            proto.write(1112396529689L, this.EJ_LIMIT_WORKING_MS);
            proto.write(1112396529690L, this.EJ_LIMIT_FREQUENT_MS);
            proto.write(1112396529691L, this.EJ_LIMIT_RARE_MS);
            proto.write(1112396529692L, this.EJ_LIMIT_RESTRICTED_MS);
            proto.write(1112396529693L, this.EJ_WINDOW_SIZE_MS);
            proto.write(1112396529694L, this.EJ_TOP_APP_TIME_CHUNK_SIZE_MS);
            proto.write(1112396529695L, this.EJ_REWARD_TOP_APP_MS);
            proto.write(1112396529696L, this.EJ_REWARD_INTERACTION_MS);
            proto.write(1112396529697L, this.EJ_REWARD_NOTIFICATION_SEEN_MS);
            proto.end(qcToken);
        }
    }

    long[] getAllowedTimePerPeriodMs() {
        return this.mAllowedTimePerPeriodMs;
    }

    int[] getBucketMaxJobCounts() {
        return this.mMaxBucketJobCounts;
    }

    int[] getBucketMaxSessionCounts() {
        return this.mMaxBucketSessionCounts;
    }

    long[] getBucketWindowSizes() {
        return this.mBucketPeriodsMs;
    }

    SparseBooleanArray getForegroundUids() {
        return this.mForegroundUids;
    }

    Handler getHandler() {
        return this.mHandler;
    }

    long getEJGracePeriodTempAllowlistMs() {
        return this.mEJGracePeriodTempAllowlistMs;
    }

    long getEJGracePeriodTopAppMs() {
        return this.mEJGracePeriodTopAppMs;
    }

    long[] getEJLimitsMs() {
        return this.mEJLimitsMs;
    }

    long getEjLimitAdditionInstallerMs() {
        return this.mEjLimitAdditionInstallerMs;
    }

    long getEjLimitAdditionSpecialMs() {
        return this.mEjLimitAdditionSpecialMs;
    }

    long getEJLimitWindowSizeMs() {
        return this.mEJLimitWindowSizeMs;
    }

    long getEJRewardInteractionMs() {
        return this.mEJRewardInteractionMs;
    }

    long getEJRewardNotificationSeenMs() {
        return this.mEJRewardNotificationSeenMs;
    }

    long getEJRewardTopAppMs() {
        return this.mEJRewardTopAppMs;
    }

    List<TimedEvent> getEJTimingSessions(int userId, String packageName) {
        return (List) this.mEJTimingSessions.get(userId, packageName);
    }

    long getEJTopAppTimeChunkSizeMs() {
        return this.mEJTopAppTimeChunkSizeMs;
    }

    long getInQuotaBufferMs() {
        return this.mQuotaBufferMs;
    }

    long getMaxExecutionTimeMs() {
        return this.mMaxExecutionTimeMs;
    }

    int getMaxJobCountPerRateLimitingWindow() {
        return this.mMaxJobCountPerRateLimitingWindow;
    }

    int getMaxSessionCountPerRateLimitingWindow() {
        return this.mMaxSessionCountPerRateLimitingWindow;
    }

    long getMinQuotaCheckDelayMs() {
        return this.mInQuotaAlarmQueue.getMinTimeBetweenAlarmsMs();
    }

    long getRateLimitingWindowMs() {
        return this.mRateLimitingWindowMs;
    }

    long getTimingSessionCoalescingDurationMs() {
        return this.mTimingSessionCoalescingDurationMs;
    }

    List<TimedEvent> getTimingSessions(int userId, String packageName) {
        return (List) this.mTimingEvents.get(userId, packageName);
    }

    QcConstants getQcConstants() {
        return this.mQcConstants;
    }

    long getQuotaBumpAdditionDurationMs() {
        return this.mQuotaBumpAdditionalDurationMs;
    }

    int getQuotaBumpAdditionJobCount() {
        return this.mQuotaBumpAdditionalJobCount;
    }

    int getQuotaBumpAdditionSessionCount() {
        return this.mQuotaBumpAdditionalSessionCount;
    }

    int getQuotaBumpLimit() {
        return this.mQuotaBumpLimit;
    }

    long getQuotaBumpWindowSizeMs() {
        return this.mQuotaBumpWindowSizeMs;
    }

    @Override // com.android.server.job.controllers.StateController
    @NeverCompile
    public void dumpControllerStateLocked(final IndentingPrintWriter pw, final Predicate<JobStatus> predicate) {
        pw.println("Is enabled: " + this.mIsEnabled);
        pw.println("Current elapsed time: " + JobSchedulerService.sElapsedRealtimeClock.millis());
        pw.println();
        pw.print("Foreground UIDs: ");
        pw.println(this.mForegroundUids.toString());
        pw.println();
        pw.print("Cached top apps: ");
        pw.println(this.mTopAppCache.toString());
        pw.print("Cached top app grace period: ");
        pw.println(this.mTopAppGraceCache.toString());
        pw.print("Cached temp allowlist: ");
        pw.println(this.mTempAllowlistCache.toString());
        pw.print("Cached temp allowlist grace period: ");
        pw.println(this.mTempAllowlistGraceCache.toString());
        pw.println();
        pw.println("Special apps:");
        pw.increaseIndent();
        pw.print("System installers={");
        for (int si = 0; si < this.mSystemInstallers.size(); si++) {
            if (si > 0) {
                pw.print(", ");
            }
            pw.print(this.mSystemInstallers.keyAt(si));
            pw.print("->");
            pw.print(this.mSystemInstallers.get(si));
        }
        pw.println("}");
        pw.decreaseIndent();
        pw.println();
        this.mTrackedJobs.forEach(new Consumer() { // from class: com.android.server.job.controllers.QuotaController$$ExternalSyntheticLambda2
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                QuotaController.this.m4229xc5e9b4b4(predicate, pw, (ArraySet) obj);
            }
        });
        pw.println();
        for (int u = 0; u < this.mPkgTimers.numMaps(); u++) {
            int userId = this.mPkgTimers.keyAt(u);
            for (int p = 0; p < this.mPkgTimers.numElementsForKey(userId); p++) {
                String pkgName = (String) this.mPkgTimers.keyAt(u, p);
                ((Timer) this.mPkgTimers.valueAt(u, p)).dump(pw, predicate);
                pw.println();
                List<TimedEvent> events = (List) this.mTimingEvents.get(userId, pkgName);
                if (events != null) {
                    pw.increaseIndent();
                    pw.println("Saved events:");
                    pw.increaseIndent();
                    for (int j = events.size() - 1; j >= 0; j--) {
                        TimedEvent event = events.get(j);
                        event.dump(pw);
                    }
                    pw.decreaseIndent();
                    pw.decreaseIndent();
                    pw.println();
                }
            }
        }
        pw.println();
        for (int u2 = 0; u2 < this.mEJPkgTimers.numMaps(); u2++) {
            int userId2 = this.mEJPkgTimers.keyAt(u2);
            for (int p2 = 0; p2 < this.mEJPkgTimers.numElementsForKey(userId2); p2++) {
                String pkgName2 = (String) this.mEJPkgTimers.keyAt(u2, p2);
                ((Timer) this.mEJPkgTimers.valueAt(u2, p2)).dump(pw, predicate);
                pw.println();
                List<TimedEvent> sessions = (List) this.mEJTimingSessions.get(userId2, pkgName2);
                if (sessions != null) {
                    pw.increaseIndent();
                    pw.println("Saved sessions:");
                    pw.increaseIndent();
                    for (int j2 = sessions.size() - 1; j2 >= 0; j2--) {
                        TimedEvent session = sessions.get(j2);
                        session.dump(pw);
                    }
                    pw.decreaseIndent();
                    pw.decreaseIndent();
                    pw.println();
                }
            }
        }
        pw.println();
        this.mTopAppTrackers.forEach(new Consumer() { // from class: com.android.server.job.controllers.QuotaController$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((QuotaController.TopAppTimer) obj).dump(pw);
            }
        });
        pw.println();
        pw.println("Cached execution stats:");
        pw.increaseIndent();
        for (int u3 = 0; u3 < this.mExecutionStatsCache.numMaps(); u3++) {
            int userId3 = this.mExecutionStatsCache.keyAt(u3);
            for (int p3 = 0; p3 < this.mExecutionStatsCache.numElementsForKey(userId3); p3++) {
                String pkgName3 = (String) this.mExecutionStatsCache.keyAt(u3, p3);
                ExecutionStats[] stats = (ExecutionStats[]) this.mExecutionStatsCache.valueAt(u3, p3);
                pw.println(Package.packageToString(userId3, pkgName3));
                pw.increaseIndent();
                for (int i = 0; i < stats.length; i++) {
                    ExecutionStats executionStats = stats[i];
                    if (executionStats != null) {
                        pw.print(JobStatus.bucketName(i));
                        pw.print(": ");
                        pw.println(executionStats);
                    }
                }
                pw.decreaseIndent();
            }
        }
        pw.decreaseIndent();
        pw.println();
        pw.println("EJ debits:");
        pw.increaseIndent();
        for (int u4 = 0; u4 < this.mEJStats.numMaps(); u4++) {
            int userId4 = this.mEJStats.keyAt(u4);
            for (int p4 = 0; p4 < this.mEJStats.numElementsForKey(userId4); p4++) {
                String pkgName4 = (String) this.mEJStats.keyAt(u4, p4);
                ShrinkableDebits debits = (ShrinkableDebits) this.mEJStats.valueAt(u4, p4);
                pw.print(Package.packageToString(userId4, pkgName4));
                pw.print(": ");
                debits.dumpLocked(pw);
            }
        }
        pw.decreaseIndent();
        pw.println();
        this.mInQuotaAlarmQueue.dump(pw);
        pw.decreaseIndent();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dumpControllerStateLocked$4$com-android-server-job-controllers-QuotaController  reason: not valid java name */
    public /* synthetic */ void m4229xc5e9b4b4(Predicate predicate, IndentingPrintWriter pw, ArraySet jobs) {
        for (int j = 0; j < jobs.size(); j++) {
            JobStatus js = (JobStatus) jobs.valueAt(j);
            if (predicate.test(js)) {
                pw.print("#");
                js.printUniqueId(pw);
                pw.print(" from ");
                UserHandle.formatUid(pw, js.getSourceUid());
                if (this.mTopStartedJobs.contains(js)) {
                    pw.print(" (TOP)");
                }
                pw.println();
                pw.increaseIndent();
                pw.print(JobStatus.bucketName(js.getEffectiveStandbyBucket()));
                pw.print(", ");
                if (js.shouldTreatAsExpeditedJob()) {
                    pw.print("within EJ quota");
                } else if (js.startedAsExpeditedJob) {
                    pw.print("out of EJ quota");
                } else if (js.isConstraintSatisfied(16777216)) {
                    pw.print("within regular quota");
                } else {
                    pw.print("not within quota");
                }
                pw.print(", ");
                if (js.shouldTreatAsExpeditedJob()) {
                    pw.print(getRemainingEJExecutionTimeLocked(js.getSourceUserId(), js.getSourcePackageName()));
                    pw.print("ms remaining in EJ quota");
                } else if (js.startedAsExpeditedJob) {
                    pw.print("should be stopped after min execution time");
                } else {
                    pw.print(getRemainingExecutionTimeLocked(js));
                    pw.print("ms remaining in quota");
                }
                pw.println();
                pw.decreaseIndent();
            }
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void dumpControllerStateLocked(final ProtoOutputStream proto, long fieldId, Predicate<JobStatus> predicate) {
        long token;
        Timer ejTimer;
        long mToken;
        int userId;
        int p;
        List<TimedEvent> events;
        final Predicate<JobStatus> predicate2 = predicate;
        long token2 = proto.start(fieldId);
        long mToken2 = proto.start(1146756268041L);
        proto.write(1133871366145L, this.mService.isBatteryCharging());
        proto.write(1112396529670L, JobSchedulerService.sElapsedRealtimeClock.millis());
        for (int i = 0; i < this.mForegroundUids.size(); i++) {
            proto.write(2220498092035L, this.mForegroundUids.keyAt(i));
        }
        this.mTrackedJobs.forEach(new Consumer() { // from class: com.android.server.job.controllers.QuotaController$$ExternalSyntheticLambda6
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                QuotaController.this.m4230x219ae972(predicate2, proto, (ArraySet) obj);
            }
        });
        int u = 0;
        while (u < this.mPkgTimers.numMaps()) {
            int userId2 = this.mPkgTimers.keyAt(u);
            int p2 = 0;
            while (p2 < this.mPkgTimers.numElementsForKey(userId2)) {
                String pkgName = (String) this.mPkgTimers.keyAt(u, p2);
                long psToken = proto.start(2246267895813L);
                ((Timer) this.mPkgTimers.valueAt(u, p2)).dump(proto, 1146756268034L, predicate2);
                Timer ejTimer2 = (Timer) this.mEJPkgTimers.get(userId2, pkgName);
                if (ejTimer2 == null) {
                    token = token2;
                } else {
                    token = token2;
                    ejTimer2.dump(proto, 1146756268038L, predicate2);
                }
                List<TimedEvent> events2 = (List) this.mTimingEvents.get(userId2, pkgName);
                if (events2 != null) {
                    int j = events2.size() - 1;
                    while (j >= 0) {
                        TimedEvent event = events2.get(j);
                        if (!(event instanceof TimingSession)) {
                            events = events2;
                        } else {
                            TimingSession session = (TimingSession) event;
                            events = events2;
                            session.dump(proto, 2246267895811L);
                        }
                        j--;
                        events2 = events;
                    }
                }
                ExecutionStats[] stats = (ExecutionStats[]) this.mExecutionStatsCache.get(userId2, pkgName);
                if (stats != null) {
                    int bucketIndex = 0;
                    while (bucketIndex < stats.length) {
                        ExecutionStats es = stats[bucketIndex];
                        if (es == null) {
                            mToken = mToken2;
                            userId = userId2;
                            ejTimer = ejTimer2;
                            p = p2;
                        } else {
                            int userId3 = userId2;
                            ejTimer = ejTimer2;
                            long esToken = proto.start(2246267895812L);
                            mToken = mToken2;
                            proto.write(1159641169921L, bucketIndex);
                            userId = userId3;
                            p = p2;
                            proto.write(1112396529666L, es.expirationTimeElapsed);
                            proto.write(1112396529667L, es.windowSizeMs);
                            proto.write(1120986464270L, es.jobCountLimit);
                            proto.write(1120986464271L, es.sessionCountLimit);
                            proto.write(1112396529668L, es.executionTimeInWindowMs);
                            proto.write(1120986464261L, es.bgJobCountInWindow);
                            proto.write(1112396529670L, es.executionTimeInMaxPeriodMs);
                            proto.write(1120986464263L, es.bgJobCountInMaxPeriod);
                            proto.write(1120986464267L, es.sessionCountInWindow);
                            proto.write(1112396529672L, es.inQuotaTimeElapsed);
                            proto.write(1112396529673L, es.jobRateLimitExpirationTimeElapsed);
                            proto.write(1120986464266L, es.jobCountInRateLimitingWindow);
                            proto.write(1112396529676L, es.sessionRateLimitExpirationTimeElapsed);
                            proto.write(1120986464269L, es.sessionCountInRateLimitingWindow);
                            proto.end(esToken);
                        }
                        bucketIndex++;
                        ejTimer2 = ejTimer;
                        mToken2 = mToken;
                        p2 = p;
                        userId2 = userId;
                    }
                }
                proto.end(psToken);
                p2++;
                predicate2 = predicate;
                token2 = token;
                mToken2 = mToken2;
                userId2 = userId2;
            }
            u++;
            predicate2 = predicate;
        }
        proto.end(mToken2);
        proto.end(token2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dumpControllerStateLocked$6$com-android-server-job-controllers-QuotaController  reason: not valid java name */
    public /* synthetic */ void m4230x219ae972(Predicate predicate, ProtoOutputStream proto, ArraySet jobs) {
        for (int j = 0; j < jobs.size(); j++) {
            JobStatus js = (JobStatus) jobs.valueAt(j);
            if (predicate.test(js)) {
                long jsToken = proto.start(2246267895812L);
                js.writeToShortProto(proto, 1146756268033L);
                proto.write(1120986464258L, js.getSourceUid());
                proto.write(1159641169923L, js.getEffectiveStandbyBucket());
                proto.write(1133871366148L, this.mTopStartedJobs.contains(js));
                proto.write(1133871366149L, js.isConstraintSatisfied(16777216));
                proto.write(1112396529670L, getRemainingExecutionTimeLocked(js));
                proto.write(1133871366151L, js.isRequestedExpeditedJob());
                proto.write(1133871366152L, js.isExpeditedQuotaApproved());
                proto.end(jsToken);
            }
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void dumpConstants(IndentingPrintWriter pw) {
        this.mQcConstants.dump(pw);
    }

    @Override // com.android.server.job.controllers.StateController
    public void dumpConstants(ProtoOutputStream proto) {
        this.mQcConstants.dump(proto);
    }
}
