package com.android.server.job;

import android.app.job.IJobCallback;
import android.app.job.IJobService;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobWorkItem;
import android.app.usage.UsageStatsManagerInternal;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.EventLog;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.internal.app.IBatteryStats;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.job.controllers.JobStatus;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.tare.EconomyManagerInternal;
import com.android.server.tare.JobSchedulerEconomicPolicy;
/* loaded from: classes.dex */
public final class JobServiceContext implements ServiceConnection {
    private static final int MSG_TIMEOUT = 0;
    public static final int NO_PREFERRED_UID = -1;
    private static final String TAG = "JobServiceContext";
    static final int VERB_BINDING = 0;
    static final int VERB_EXECUTING = 2;
    static final int VERB_FINISHED = 4;
    static final int VERB_STARTING = 1;
    static final int VERB_STOPPING = 3;
    private boolean mAvailable;
    private final IBatteryStats mBatteryStats;
    private final Handler mCallbackHandler;
    private boolean mCancelled;
    private final JobCompletedListener mCompletedListener;
    private final Context mContext;
    private final EconomyManagerInternal mEconomyManagerInternal;
    private long mExecutionStartTimeElapsed;
    private final JobConcurrencyManager mJobConcurrencyManager;
    private final JobPackageTracker mJobPackageTracker;
    private long mLastUnsuccessfulFinishElapsed;
    private final Object mLock;
    private long mMaxExecutionTimeMillis;
    private long mMinExecutionGuaranteeMillis;
    private JobParameters mParams;
    private String mPendingDebugStopReason;
    private int mPendingInternalStopReason;
    private int mPendingStopReason = 0;
    private final PowerManager mPowerManager;
    private int mPreferredUid;
    private boolean mPreviousJobHadSuccessfulFinish;
    private JobCallback mRunningCallback;
    private JobStatus mRunningJob;
    private int mRunningJobWorkType;
    private final JobSchedulerService mService;
    public String mStoppedReason;
    public long mStoppedTime;
    private long mTimeoutElapsed;
    int mVerb;
    private PowerManager.WakeLock mWakeLock;
    IJobService service;
    private static final boolean DEBUG = JobSchedulerService.DEBUG;
    private static final boolean DEBUG_STANDBY = JobSchedulerService.DEBUG_STANDBY;
    private static final long OP_BIND_TIMEOUT_MILLIS = Build.HW_TIMEOUT_MULTIPLIER * 18000;
    private static final long OP_TIMEOUT_MILLIS = Build.HW_TIMEOUT_MULTIPLIER * EventLogTags.JOB_DEFERRED_EXECUTION;
    private static final String[] VERB_STRINGS = {"VERB_BINDING", "VERB_STARTING", "VERB_EXECUTING", "VERB_STOPPING", "VERB_FINISHED"};

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class JobCallback extends IJobCallback.Stub {
        public String mStoppedReason;
        public long mStoppedTime;

        JobCallback() {
        }

        public void acknowledgeStartMessage(int jobId, boolean ongoing) {
            JobServiceContext.this.doAcknowledgeStartMessage(this, jobId, ongoing);
        }

        public void acknowledgeStopMessage(int jobId, boolean reschedule) {
            JobServiceContext.this.doAcknowledgeStopMessage(this, jobId, reschedule);
        }

        public JobWorkItem dequeueWork(int jobId) {
            return JobServiceContext.this.doDequeueWork(this, jobId);
        }

        public boolean completeWork(int jobId, int workId) {
            return JobServiceContext.this.doCompleteWork(this, jobId, workId);
        }

        public void jobFinished(int jobId, boolean reschedule) {
            JobServiceContext.this.doJobFinished(this, jobId, reschedule);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public JobServiceContext(JobSchedulerService service, JobConcurrencyManager concurrencyManager, IBatteryStats batteryStats, JobPackageTracker tracker, Looper looper) {
        Context context = service.getContext();
        this.mContext = context;
        this.mLock = service.getLock();
        this.mService = service;
        this.mBatteryStats = batteryStats;
        this.mEconomyManagerInternal = (EconomyManagerInternal) LocalServices.getService(EconomyManagerInternal.class);
        this.mJobPackageTracker = tracker;
        this.mCallbackHandler = new JobServiceHandler(looper);
        this.mJobConcurrencyManager = concurrencyManager;
        this.mCompletedListener = service;
        this.mPowerManager = (PowerManager) context.getSystemService(PowerManager.class);
        this.mAvailable = true;
        this.mVerb = 4;
        this.mPreferredUid = -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean executeRunnableJob(JobStatus job, int workType) {
        Uri[] triggeredUris;
        String[] triggeredAuthorities;
        boolean binding;
        int bindFlags;
        synchronized (this.mLock) {
            try {
                if (this.mAvailable) {
                    this.mPreferredUid = -1;
                    this.mRunningJob = job;
                    try {
                        this.mRunningJobWorkType = workType;
                        this.mRunningCallback = new JobCallback();
                        boolean isDeadlineExpired = job.hasDeadlineConstraint() && job.getLatestRunTimeElapsed() < JobSchedulerService.sElapsedRealtimeClock.millis();
                        if (job.changedUris == null) {
                            triggeredUris = null;
                        } else {
                            Uri[] triggeredUris2 = new Uri[job.changedUris.size()];
                            job.changedUris.toArray(triggeredUris2);
                            triggeredUris = triggeredUris2;
                        }
                        if (job.changedAuthorities == null) {
                            triggeredAuthorities = null;
                        } else {
                            String[] triggeredAuthorities2 = new String[job.changedAuthorities.size()];
                            job.changedAuthorities.toArray(triggeredAuthorities2);
                            triggeredAuthorities = triggeredAuthorities2;
                        }
                        JobInfo ji = job.getJob();
                        this.mParams = new JobParameters(this.mRunningCallback, job.getJobId(), ji.getExtras(), ji.getTransientExtras(), ji.getClipData(), ji.getClipGrantFlags(), isDeadlineExpired, job.shouldTreatAsExpeditedJob(), triggeredUris, triggeredAuthorities, job.network);
                        this.mExecutionStartTimeElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
                        this.mMinExecutionGuaranteeMillis = this.mService.getMinJobExecutionGuaranteeMs(job);
                        this.mMaxExecutionTimeMillis = Math.max(this.mService.getMaxJobExecutionTimeMs(job), this.mMinExecutionGuaranteeMillis);
                        long whenDeferred = job.getWhenStandbyDeferred();
                        if (whenDeferred > 0) {
                            long deferral = this.mExecutionStartTimeElapsed - whenDeferred;
                            EventLog.writeEvent((int) EventLogTags.JOB_DEFERRED_EXECUTION, deferral);
                            if (DEBUG_STANDBY) {
                                StringBuilder sb = new StringBuilder(128);
                                sb.append("Starting job deferred for standby by ");
                                TimeUtils.formatDuration(deferral, sb);
                                sb.append(" ms : ");
                                sb.append(job.toShortString());
                                Slog.v(TAG, sb.toString());
                            }
                        }
                        job.clearPersistedUtcTimes();
                        PowerManager.WakeLock newWakeLock = this.mPowerManager.newWakeLock(1, job.getTag());
                        this.mWakeLock = newWakeLock;
                        newWakeLock.setWorkSource(this.mService.deriveWorkSource(job.getSourceUid(), job.getSourcePackageName()));
                        this.mWakeLock.setReferenceCounted(false);
                        this.mWakeLock.acquire();
                        this.mEconomyManagerInternal.noteInstantaneousEvent(job.getSourceUserId(), job.getSourcePackageName(), getStartActionId(job), String.valueOf(job.getJobId()));
                        this.mVerb = 0;
                        scheduleOpTimeOutLocked();
                        Intent intent = new Intent().setComponent(job.getServiceComponent());
                        try {
                            if (job.shouldTreatAsExpeditedJob()) {
                                bindFlags = 229381;
                            } else {
                                bindFlags = 33029;
                            }
                            binding = this.mContext.bindServiceAsUser(intent, this, bindFlags, UserHandle.of(job.getUserId()));
                        } catch (SecurityException e) {
                            Slog.w(TAG, "Job service " + job.getServiceComponent().getShortClassName() + " cannot be executed: " + e.getMessage());
                            binding = false;
                        }
                        if (!binding) {
                            if (DEBUG) {
                                Slog.d(TAG, job.getServiceComponent().getShortClassName() + " unavailable.");
                            }
                            this.mContext.unbindService(this);
                            this.mRunningJob = null;
                            this.mRunningJobWorkType = 0;
                            this.mRunningCallback = null;
                            this.mParams = null;
                            this.mExecutionStartTimeElapsed = 0L;
                            this.mWakeLock.release();
                            this.mVerb = 4;
                            removeOpTimeOutLocked();
                            return false;
                        }
                        this.mJobPackageTracker.noteActive(job);
                        FrameworkStatsLog.write_non_chained(8, job.getSourceUid(), null, job.getBatteryName(), 1, -1, job.getStandbyBucket(), job.getJobId(), job.hasChargingConstraint(), job.hasBatteryNotLowConstraint(), job.hasStorageNotLowConstraint(), job.hasTimingDelayConstraint(), job.hasDeadlineConstraint(), job.hasIdleConstraint(), job.hasConnectivityConstraint(), job.hasContentTriggerConstraint(), job.isRequestedExpeditedJob(), job.shouldTreatAsExpeditedJob(), 0, job.getJob().isPrefetch(), job.getJob().getPriority(), job.getEffectivePriority(), job.getNumFailures());
                        try {
                            this.mBatteryStats.noteJobStart(job.getBatteryName(), job.getSourceUid());
                        } catch (RemoteException e2) {
                        }
                        String jobPackage = job.getSourcePackageName();
                        int jobUserId = job.getSourceUserId();
                        UsageStatsManagerInternal usageStats = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
                        usageStats.setLastJobRunTime(jobPackage, jobUserId, this.mExecutionStartTimeElapsed);
                        this.mAvailable = false;
                        this.mStoppedReason = null;
                        this.mStoppedTime = 0L;
                        job.startedAsExpeditedJob = job.shouldTreatAsExpeditedJob();
                        return true;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                Slog.e(TAG, "Starting new runnable but context is unavailable > Error.");
                return false;
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    private static int getStartActionId(JobStatus job) {
        switch (job.getEffectivePriority()) {
            case 100:
                return JobSchedulerEconomicPolicy.ACTION_JOB_MIN_START;
            case 200:
                return JobSchedulerEconomicPolicy.ACTION_JOB_LOW_START;
            case 300:
                return JobSchedulerEconomicPolicy.ACTION_JOB_DEFAULT_START;
            case 400:
                return JobSchedulerEconomicPolicy.ACTION_JOB_HIGH_START;
            case 500:
                return JobSchedulerEconomicPolicy.ACTION_JOB_MAX_START;
            default:
                Slog.wtf(TAG, "Unknown priority: " + JobInfo.getPriorityString(job.getEffectivePriority()));
                return JobSchedulerEconomicPolicy.ACTION_JOB_DEFAULT_START;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public JobStatus getRunningJobLocked() {
        return this.mRunningJob;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getRunningJobWorkType() {
        return this.mRunningJobWorkType;
    }

    private String getRunningJobNameLocked() {
        JobStatus jobStatus = this.mRunningJob;
        return jobStatus != null ? jobStatus.toShortString() : "<null>";
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancelExecutingJobLocked(int reason, int internalStopReason, String debugReason) {
        doCancelLocked(reason, internalStopReason, debugReason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getPreferredUid() {
        return this.mPreferredUid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearPreferredUid() {
        this.mPreferredUid = -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getId() {
        return hashCode();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getExecutionStartTimeElapsed() {
        return this.mExecutionStartTimeElapsed;
    }

    long getTimeoutElapsed() {
        return this.mTimeoutElapsed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isWithinExecutionGuaranteeTime() {
        return JobSchedulerService.sElapsedRealtimeClock.millis() < this.mExecutionStartTimeElapsed + this.mMinExecutionGuaranteeMillis;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean timeoutIfExecutingLocked(String pkgName, int userId, boolean matchJobId, int jobId, String reason) {
        JobStatus executing = getRunningJobLocked();
        if (executing != null) {
            if (userId == -1 || userId == executing.getUserId()) {
                if (pkgName == null || pkgName.equals(executing.getSourcePackageName())) {
                    if ((!matchJobId || jobId == executing.getJobId()) && this.mVerb == 2) {
                        this.mParams.setStopReason(3, 3, reason);
                        sendStopMessageLocked("force timeout from shell");
                        return true;
                    }
                    return false;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    void doJobFinished(JobCallback cb, int jobId, boolean reschedule) {
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                if (!verifyCallerLocked(cb)) {
                    return;
                }
                this.mParams.setStopReason(0, 10, "app called jobFinished");
                doCallbackLocked(reschedule, "app called jobFinished");
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    void doAcknowledgeStopMessage(JobCallback cb, int jobId, boolean reschedule) {
        doCallback(cb, reschedule, null);
    }

    void doAcknowledgeStartMessage(JobCallback cb, int jobId, boolean ongoing) {
        doCallback(cb, ongoing, "finished start");
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [522=4] */
    JobWorkItem doDequeueWork(JobCallback cb, int jobId) {
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                if (assertCallerLocked(cb)) {
                    int i = this.mVerb;
                    if (i != 3 && i != 4) {
                        JobWorkItem work = this.mRunningJob.dequeueWorkLocked();
                        if (work == null && !this.mRunningJob.hasExecutingWorkLocked()) {
                            this.mParams.setStopReason(0, 10, "last work dequeued");
                            doCallbackLocked(false, "last work dequeued");
                        }
                        return work;
                    }
                    return null;
                }
                return null;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    boolean doCompleteWork(JobCallback cb, int jobId, int workId) {
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                if (!assertCallerLocked(cb)) {
                    return true;
                }
                return this.mRunningJob.completeWorkLocked(workId);
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    @Override // android.content.ServiceConnection
    public void onServiceConnected(ComponentName name, IBinder service) {
        synchronized (this.mLock) {
            JobStatus runningJob = this.mRunningJob;
            if (runningJob != null && name.equals(runningJob.getServiceComponent())) {
                this.service = IJobService.Stub.asInterface(service);
                doServiceBoundLocked();
                return;
            }
            closeAndCleanupJobLocked(true, "connected for different component");
        }
    }

    @Override // android.content.ServiceConnection
    public void onServiceDisconnected(ComponentName name) {
        synchronized (this.mLock) {
            closeAndCleanupJobLocked(true, "unexpectedly disconnected");
        }
    }

    @Override // android.content.ServiceConnection
    public void onBindingDied(ComponentName name) {
        synchronized (this.mLock) {
            JobStatus jobStatus = this.mRunningJob;
            if (jobStatus == null) {
                Slog.e(TAG, "Binding died for " + name.getPackageName() + " but no running job on this context");
            } else if (jobStatus.getServiceComponent().equals(name)) {
                Slog.e(TAG, "Binding died for " + this.mRunningJob.getSourceUserId() + ":" + name.getPackageName());
            } else {
                Slog.e(TAG, "Binding died for " + name.getPackageName() + " but context is running a different job");
            }
            closeAndCleanupJobLocked(true, "binding died");
        }
    }

    @Override // android.content.ServiceConnection
    public void onNullBinding(ComponentName name) {
        synchronized (this.mLock) {
            JobStatus jobStatus = this.mRunningJob;
            if (jobStatus == null) {
                Slog.wtf(TAG, "Got null binding for " + name.getPackageName() + " but no running job on this context");
            } else if (jobStatus.getServiceComponent().equals(name)) {
                Slog.wtf(TAG, "Got null binding for " + this.mRunningJob.getSourceUserId() + ":" + name.getPackageName());
            } else {
                Slog.wtf(TAG, "Got null binding for " + name.getPackageName() + " but context is running a different job");
            }
            closeAndCleanupJobLocked(false, "null binding");
        }
    }

    private boolean verifyCallerLocked(JobCallback cb) {
        if (this.mRunningCallback != cb) {
            if (DEBUG) {
                Slog.d(TAG, "Stale callback received, ignoring.");
                return false;
            }
            return false;
        }
        return true;
    }

    private boolean assertCallerLocked(JobCallback cb) {
        if (!verifyCallerLocked(cb)) {
            long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
            if (!this.mPreviousJobHadSuccessfulFinish && nowElapsed - this.mLastUnsuccessfulFinishElapsed < 15000) {
                return false;
            }
            StringBuilder sb = new StringBuilder(128);
            sb.append("Caller no longer running");
            if (cb.mStoppedReason != null) {
                sb.append(", last stopped ");
                TimeUtils.formatDuration(nowElapsed - cb.mStoppedTime, sb);
                sb.append(" because: ");
                sb.append(cb.mStoppedReason);
            }
            throw new SecurityException(sb.toString());
        }
        return true;
    }

    /* loaded from: classes.dex */
    private class JobServiceHandler extends Handler {
        JobServiceHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    synchronized (JobServiceContext.this.mLock) {
                        if (message.obj == JobServiceContext.this.mRunningCallback) {
                            JobServiceContext.this.handleOpTimeoutLocked();
                        } else {
                            JobCallback jc = (JobCallback) message.obj;
                            StringBuilder sb = new StringBuilder(128);
                            sb.append("Ignoring timeout of no longer active job");
                            if (jc.mStoppedReason != null) {
                                sb.append(", stopped ");
                                TimeUtils.formatDuration(JobSchedulerService.sElapsedRealtimeClock.millis() - jc.mStoppedTime, sb);
                                sb.append(" because: ");
                                sb.append(jc.mStoppedReason);
                            }
                            Slog.w(JobServiceContext.TAG, sb.toString());
                        }
                    }
                    return;
                default:
                    Slog.e(JobServiceContext.TAG, "Unrecognised message: " + message);
                    return;
            }
        }
    }

    void doServiceBoundLocked() {
        removeOpTimeOutLocked();
        handleServiceBoundLocked();
    }

    void doCallback(JobCallback cb, boolean reschedule, String reason) {
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                if (!verifyCallerLocked(cb)) {
                    return;
                }
                doCallbackLocked(reschedule, reason);
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    void doCallbackLocked(boolean reschedule, String reason) {
        boolean z = DEBUG;
        if (z) {
            Slog.d(TAG, "doCallback of : " + this.mRunningJob + " v:" + VERB_STRINGS[this.mVerb]);
        }
        removeOpTimeOutLocked();
        int i = this.mVerb;
        if (i == 1) {
            handleStartedLocked(reschedule);
        } else if (i == 2 || i == 3) {
            handleFinishedLocked(reschedule, reason);
        } else if (z) {
            Slog.d(TAG, "Unrecognised callback: " + this.mRunningJob);
        }
    }

    private void doCancelLocked(int stopReasonCode, int internalStopReasonCode, String debugReason) {
        if (this.mVerb == 4) {
            if (DEBUG) {
                Slog.d(TAG, "Trying to process cancel for torn-down context, ignoring.");
                return;
            }
            return;
        }
        if (this.mRunningJob.startedAsExpeditedJob && stopReasonCode == 10) {
            long earliestStopTimeElapsed = this.mExecutionStartTimeElapsed + this.mMinExecutionGuaranteeMillis;
            long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
            if (nowElapsed < earliestStopTimeElapsed) {
                this.mPendingStopReason = stopReasonCode;
                this.mPendingInternalStopReason = internalStopReasonCode;
                this.mPendingDebugStopReason = debugReason;
                return;
            }
        }
        this.mParams.setStopReason(stopReasonCode, internalStopReasonCode, debugReason);
        if (stopReasonCode == 2) {
            JobStatus jobStatus = this.mRunningJob;
            this.mPreferredUid = jobStatus != null ? jobStatus.getUid() : -1;
        }
        handleCancelLocked(debugReason);
    }

    private void handleServiceBoundLocked() {
        boolean z = DEBUG;
        if (z) {
            Slog.d(TAG, "handleServiceBound for " + getRunningJobNameLocked());
        }
        if (this.mVerb != 0) {
            Slog.e(TAG, "Sending onStartJob for a job that isn't pending. " + VERB_STRINGS[this.mVerb]);
            closeAndCleanupJobLocked(false, "started job not pending");
        } else if (this.mCancelled) {
            if (z) {
                Slog.d(TAG, "Job cancelled while waiting for bind to complete. " + this.mRunningJob);
            }
            closeAndCleanupJobLocked(true, "cancelled while waiting for bind");
        } else {
            try {
                this.mVerb = 1;
                scheduleOpTimeOutLocked();
                this.service.startJob(this.mParams);
            } catch (Exception e) {
                Slog.e(TAG, "Error sending onStart message to '" + this.mRunningJob.getServiceComponent().getShortClassName() + "' ", e);
            }
        }
    }

    private void handleStartedLocked(boolean workOngoing) {
        switch (this.mVerb) {
            case 1:
                this.mVerb = 2;
                if (!workOngoing) {
                    handleFinishedLocked(false, "onStartJob returned false");
                    return;
                } else if (this.mCancelled) {
                    if (DEBUG) {
                        Slog.d(TAG, "Job cancelled while waiting for onStartJob to complete.");
                    }
                    handleCancelLocked(null);
                    return;
                } else {
                    scheduleOpTimeOutLocked();
                    return;
                }
            default:
                Slog.e(TAG, "Handling started job but job wasn't starting! Was " + VERB_STRINGS[this.mVerb] + ".");
                return;
        }
    }

    private void handleFinishedLocked(boolean reschedule, String reason) {
        switch (this.mVerb) {
            case 2:
            case 3:
                closeAndCleanupJobLocked(reschedule, reason);
                return;
            default:
                Slog.e(TAG, "Got an execution complete message for a job that wasn't beingexecuted. Was " + VERB_STRINGS[this.mVerb] + ".");
                return;
        }
    }

    private void handleCancelLocked(String reason) {
        if (JobSchedulerService.DEBUG) {
            Slog.d(TAG, "Handling cancel for: " + this.mRunningJob.getJobId() + " " + VERB_STRINGS[this.mVerb]);
        }
        switch (this.mVerb) {
            case 0:
            case 1:
                this.mCancelled = true;
                applyStoppedReasonLocked(reason);
                return;
            case 2:
                sendStopMessageLocked(reason);
                return;
            case 3:
                return;
            default:
                Slog.e(TAG, "Cancelling a job without a valid verb: " + this.mVerb);
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOpTimeoutLocked() {
        switch (this.mVerb) {
            case 0:
                Slog.w(TAG, "Time-out while trying to bind " + getRunningJobNameLocked() + ", dropping.");
                closeAndCleanupJobLocked(false, "timed out while binding");
                return;
            case 1:
                Slog.w(TAG, "No response from client for onStartJob " + getRunningJobNameLocked());
                closeAndCleanupJobLocked(false, "timed out while starting");
                return;
            case 2:
                if (this.mPendingStopReason != 0) {
                    if (this.mService.isReadyToBeExecutedLocked(this.mRunningJob, false)) {
                        this.mPendingStopReason = 0;
                        this.mPendingInternalStopReason = 0;
                        this.mPendingDebugStopReason = null;
                    } else {
                        Slog.i(TAG, "JS was waiting to stop this job. Sending onStop: " + getRunningJobNameLocked());
                        this.mParams.setStopReason(this.mPendingStopReason, this.mPendingInternalStopReason, this.mPendingDebugStopReason);
                        sendStopMessageLocked(this.mPendingDebugStopReason);
                        return;
                    }
                }
                long latestStopTimeElapsed = this.mExecutionStartTimeElapsed + this.mMaxExecutionTimeMillis;
                long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
                if (nowElapsed >= latestStopTimeElapsed) {
                    Slog.i(TAG, "Client timed out while executing (no jobFinished received). Sending onStop: " + getRunningJobNameLocked());
                    this.mParams.setStopReason(3, 3, "client timed out");
                    sendStopMessageLocked("timeout while executing");
                    return;
                }
                String reason = this.mJobConcurrencyManager.shouldStopRunningJobLocked(this);
                if (reason != null) {
                    Slog.i(TAG, "Stopping client after min execution time: " + getRunningJobNameLocked() + " because " + reason);
                    this.mParams.setStopReason(4, 3, reason);
                    sendStopMessageLocked(reason);
                    return;
                }
                Slog.i(TAG, "Letting " + getRunningJobNameLocked() + " continue to run past min execution time");
                scheduleOpTimeOutLocked();
                return;
            case 3:
                Slog.w(TAG, "No response from client for onStopJob " + getRunningJobNameLocked());
                closeAndCleanupJobLocked(true, "timed out while stopping");
                return;
            default:
                Slog.e(TAG, "Handling timeout for an invalid job state: " + getRunningJobNameLocked() + ", dropping.");
                closeAndCleanupJobLocked(false, "invalid timeout");
                return;
        }
    }

    private void sendStopMessageLocked(String reason) {
        removeOpTimeOutLocked();
        if (this.mVerb != 2) {
            Slog.e(TAG, "Sending onStopJob for a job that isn't started. " + this.mRunningJob);
            closeAndCleanupJobLocked(false, reason);
            return;
        }
        try {
            applyStoppedReasonLocked(reason);
            this.mVerb = 3;
            scheduleOpTimeOutLocked();
            this.service.stopJob(this.mParams);
        } catch (RemoteException e) {
            Slog.e(TAG, "Error sending onStopJob to client.", e);
            closeAndCleanupJobLocked(true, "host crashed when trying to stop");
        }
    }

    private void closeAndCleanupJobLocked(boolean reschedule, String reason) {
        int internalStopReason;
        if (this.mVerb == 4) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "Cleaning up " + this.mRunningJob.toShortString() + " reschedule=" + reschedule + " reason=" + reason);
        }
        applyStoppedReasonLocked(reason);
        JobStatus completedJob = this.mRunningJob;
        int internalStopReason2 = this.mParams.getInternalStopReasonCode();
        boolean z = internalStopReason2 == 10;
        this.mPreviousJobHadSuccessfulFinish = z;
        if (!z) {
            this.mLastUnsuccessfulFinishElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        }
        this.mJobPackageTracker.noteInactive(completedJob, internalStopReason2, reason);
        FrameworkStatsLog.write_non_chained(8, completedJob.getSourceUid(), null, completedJob.getBatteryName(), 0, internalStopReason2, completedJob.getStandbyBucket(), completedJob.getJobId(), completedJob.hasChargingConstraint(), completedJob.hasBatteryNotLowConstraint(), completedJob.hasStorageNotLowConstraint(), completedJob.hasTimingDelayConstraint(), completedJob.hasDeadlineConstraint(), completedJob.hasIdleConstraint(), completedJob.hasConnectivityConstraint(), completedJob.hasContentTriggerConstraint(), completedJob.isRequestedExpeditedJob(), completedJob.startedAsExpeditedJob, this.mParams.getStopReason(), completedJob.getJob().isPrefetch(), completedJob.getJob().getPriority(), completedJob.getEffectivePriority(), completedJob.getNumFailures());
        try {
            internalStopReason = internalStopReason2;
            try {
                this.mBatteryStats.noteJobFinish(this.mRunningJob.getBatteryName(), this.mRunningJob.getSourceUid(), internalStopReason);
            } catch (RemoteException e) {
            }
        } catch (RemoteException e2) {
            internalStopReason = internalStopReason2;
        }
        if (this.mParams.getStopReason() == 3) {
            this.mEconomyManagerInternal.noteInstantaneousEvent(this.mRunningJob.getSourceUserId(), this.mRunningJob.getSourcePackageName(), JobSchedulerEconomicPolicy.ACTION_JOB_TIMEOUT, String.valueOf(this.mRunningJob.getJobId()));
        }
        PowerManager.WakeLock wakeLock = this.mWakeLock;
        if (wakeLock != null) {
            wakeLock.release();
        }
        int workType = this.mRunningJobWorkType;
        this.mContext.unbindService(this);
        this.mWakeLock = null;
        this.mRunningJob = null;
        this.mRunningJobWorkType = 0;
        this.mRunningCallback = null;
        this.mParams = null;
        this.mVerb = 4;
        this.mCancelled = false;
        this.service = null;
        this.mAvailable = true;
        this.mPendingStopReason = 0;
        this.mPendingInternalStopReason = 0;
        this.mPendingDebugStopReason = null;
        removeOpTimeOutLocked();
        this.mCompletedListener.onJobCompletedLocked(completedJob, internalStopReason, reschedule);
        this.mJobConcurrencyManager.onJobCompletedLocked(this, completedJob, workType);
    }

    private void applyStoppedReasonLocked(String reason) {
        if (reason != null && this.mStoppedReason == null) {
            this.mStoppedReason = reason;
            this.mStoppedTime = JobSchedulerService.sElapsedRealtimeClock.millis();
            JobCallback jobCallback = this.mRunningCallback;
            if (jobCallback != null) {
                jobCallback.mStoppedReason = this.mStoppedReason;
                this.mRunningCallback.mStoppedTime = this.mStoppedTime;
            }
        }
    }

    private void scheduleOpTimeOutLocked() {
        long timeoutMillis;
        removeOpTimeOutLocked();
        switch (this.mVerb) {
            case 0:
                timeoutMillis = OP_BIND_TIMEOUT_MILLIS;
                break;
            case 1:
            default:
                timeoutMillis = OP_TIMEOUT_MILLIS;
                break;
            case 2:
                long j = this.mExecutionStartTimeElapsed;
                long earliestStopTimeElapsed = this.mMinExecutionGuaranteeMillis + j;
                long latestStopTimeElapsed = j + this.mMaxExecutionTimeMillis;
                long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
                if (nowElapsed < earliestStopTimeElapsed) {
                    timeoutMillis = earliestStopTimeElapsed - nowElapsed;
                    break;
                } else {
                    timeoutMillis = latestStopTimeElapsed - nowElapsed;
                    break;
                }
        }
        if (DEBUG) {
            Slog.d(TAG, "Scheduling time out for '" + this.mRunningJob.getServiceComponent().getShortClassName() + "' jId: " + this.mParams.getJobId() + ", in " + (timeoutMillis / 1000) + " s");
        }
        Message m = this.mCallbackHandler.obtainMessage(0, this.mRunningCallback);
        this.mCallbackHandler.sendMessageDelayed(m, timeoutMillis);
        this.mTimeoutElapsed = JobSchedulerService.sElapsedRealtimeClock.millis() + timeoutMillis;
    }

    private void removeOpTimeOutLocked() {
        this.mCallbackHandler.removeMessages(0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpLocked(IndentingPrintWriter pw, long nowElapsed) {
        JobStatus jobStatus = this.mRunningJob;
        if (jobStatus == null) {
            if (this.mStoppedReason != null) {
                pw.print("inactive since ");
                TimeUtils.formatDuration(this.mStoppedTime, nowElapsed, pw);
                pw.print(", stopped because: ");
                pw.println(this.mStoppedReason);
                return;
            }
            pw.println("inactive");
            return;
        }
        pw.println(jobStatus.toShortString());
        pw.increaseIndent();
        pw.print("Running for: ");
        TimeUtils.formatDuration(nowElapsed - this.mExecutionStartTimeElapsed, pw);
        pw.print(", timeout at: ");
        TimeUtils.formatDuration(this.mTimeoutElapsed - nowElapsed, pw);
        pw.println();
        pw.print("Remaining execution limits: [");
        TimeUtils.formatDuration((this.mExecutionStartTimeElapsed + this.mMinExecutionGuaranteeMillis) - nowElapsed, pw);
        pw.print(", ");
        TimeUtils.formatDuration((this.mExecutionStartTimeElapsed + this.mMaxExecutionTimeMillis) - nowElapsed, pw);
        pw.print("]");
        if (this.mPendingStopReason != 0) {
            pw.print(" Pending stop because ");
            pw.print(this.mPendingStopReason);
            pw.print(SliceClientPermissions.SliceAuthority.DELIMITER);
            pw.print(this.mPendingInternalStopReason);
            pw.print(SliceClientPermissions.SliceAuthority.DELIMITER);
            pw.print(this.mPendingDebugStopReason);
        }
        pw.println();
        pw.decreaseIndent();
    }
}
