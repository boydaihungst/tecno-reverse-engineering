package com.android.server.job.controllers;

import android.app.job.JobInfo;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArrayMap;
import com.android.server.JobSchedulerBackgroundThread;
import com.android.server.LocalServices;
import com.android.server.job.JobSchedulerService;
import com.android.server.tare.EconomyManagerInternal;
import com.android.server.tare.JobSchedulerEconomicPolicy;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public class TareController extends StateController {
    private static final EconomyManagerInternal.ActionBill BILL_JOB_RUNNING_DEFAULT;
    private static final EconomyManagerInternal.ActionBill BILL_JOB_RUNNING_HIGH;
    private static final EconomyManagerInternal.ActionBill BILL_JOB_RUNNING_HIGH_EXPEDITED;
    private static final EconomyManagerInternal.ActionBill BILL_JOB_RUNNING_LOW;
    private static final EconomyManagerInternal.ActionBill BILL_JOB_RUNNING_MAX;
    private static final EconomyManagerInternal.ActionBill BILL_JOB_RUNNING_MAX_EXPEDITED;
    private static final EconomyManagerInternal.ActionBill BILL_JOB_RUNNING_MIN;
    private static final EconomyManagerInternal.ActionBill BILL_JOB_START_DEFAULT;
    private static final EconomyManagerInternal.ActionBill BILL_JOB_START_HIGH;
    private static final EconomyManagerInternal.ActionBill BILL_JOB_START_HIGH_EXPEDITED;
    private static final EconomyManagerInternal.ActionBill BILL_JOB_START_LOW;
    private static final EconomyManagerInternal.ActionBill BILL_JOB_START_MAX;
    private static final EconomyManagerInternal.ActionBill BILL_JOB_START_MAX_EXPEDITED;
    private static final EconomyManagerInternal.ActionBill BILL_JOB_START_MIN;
    private static final boolean DEBUG;
    private static final String TAG = "JobScheduler.TARE";
    private final SparseArrayMap<String, ArrayMap<EconomyManagerInternal.ActionBill, Boolean>> mAffordabilityCache;
    private final EconomyManagerInternal.AffordabilityChangeListener mAffordabilityChangeListener;
    private final BackgroundJobsController mBackgroundJobsController;
    private final ConnectivityController mConnectivityController;
    private final EconomyManagerInternal mEconomyManagerInternal;
    private boolean mIsEnabled;
    private final SparseArrayMap<String, ArrayMap<EconomyManagerInternal.ActionBill, ArraySet<JobStatus>>> mRegisteredBillsAndJobs;
    private final ArraySet<JobStatus> mTopStartedJobs;

    static {
        DEBUG = JobSchedulerService.DEBUG || Log.isLoggable(TAG, 3);
        BILL_JOB_START_MIN = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(JobSchedulerEconomicPolicy.ACTION_JOB_DEFAULT_START, 1, 0L), new EconomyManagerInternal.AnticipatedAction(JobSchedulerEconomicPolicy.ACTION_JOB_DEFAULT_RUNNING, 0, 120000L)));
        BILL_JOB_RUNNING_MIN = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(JobSchedulerEconomicPolicy.ACTION_JOB_DEFAULT_RUNNING, 0, 60000L)));
        BILL_JOB_START_LOW = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(JobSchedulerEconomicPolicy.ACTION_JOB_DEFAULT_START, 1, 0L), new EconomyManagerInternal.AnticipatedAction(JobSchedulerEconomicPolicy.ACTION_JOB_DEFAULT_RUNNING, 0, 60000L)));
        BILL_JOB_RUNNING_LOW = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(JobSchedulerEconomicPolicy.ACTION_JOB_DEFAULT_RUNNING, 0, 30000L)));
        BILL_JOB_START_DEFAULT = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(JobSchedulerEconomicPolicy.ACTION_JOB_DEFAULT_START, 1, 0L), new EconomyManagerInternal.AnticipatedAction(JobSchedulerEconomicPolicy.ACTION_JOB_DEFAULT_RUNNING, 0, 30000L)));
        BILL_JOB_RUNNING_DEFAULT = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(JobSchedulerEconomicPolicy.ACTION_JOB_DEFAULT_RUNNING, 0, 1000L)));
        BILL_JOB_START_HIGH = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(JobSchedulerEconomicPolicy.ACTION_JOB_HIGH_START, 1, 0L), new EconomyManagerInternal.AnticipatedAction(JobSchedulerEconomicPolicy.ACTION_JOB_HIGH_RUNNING, 0, 30000L)));
        BILL_JOB_RUNNING_HIGH = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(JobSchedulerEconomicPolicy.ACTION_JOB_HIGH_RUNNING, 0, 1000L)));
        BILL_JOB_START_MAX = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(JobSchedulerEconomicPolicy.ACTION_JOB_MAX_START, 1, 0L), new EconomyManagerInternal.AnticipatedAction(JobSchedulerEconomicPolicy.ACTION_JOB_MAX_RUNNING, 0, 30000L)));
        BILL_JOB_RUNNING_MAX = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(JobSchedulerEconomicPolicy.ACTION_JOB_MAX_RUNNING, 0, 1000L)));
        BILL_JOB_START_MAX_EXPEDITED = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(JobSchedulerEconomicPolicy.ACTION_JOB_MAX_START, 1, 0L), new EconomyManagerInternal.AnticipatedAction(JobSchedulerEconomicPolicy.ACTION_JOB_MAX_RUNNING, 0, 30000L)));
        BILL_JOB_RUNNING_MAX_EXPEDITED = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(JobSchedulerEconomicPolicy.ACTION_JOB_MAX_RUNNING, 0, 1000L)));
        BILL_JOB_START_HIGH_EXPEDITED = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(JobSchedulerEconomicPolicy.ACTION_JOB_HIGH_START, 1, 0L), new EconomyManagerInternal.AnticipatedAction(JobSchedulerEconomicPolicy.ACTION_JOB_HIGH_RUNNING, 0, 30000L)));
        BILL_JOB_RUNNING_HIGH_EXPEDITED = new EconomyManagerInternal.ActionBill(List.of(new EconomyManagerInternal.AnticipatedAction(JobSchedulerEconomicPolicy.ACTION_JOB_HIGH_RUNNING, 0, 1000L)));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:26:0x0093 A[Catch: all -> 0x00b9, TryCatch #0 {, blocks: (B:7:0x0041, B:9:0x004b, B:10:0x0056, B:12:0x0067, B:14:0x006f, B:15:0x0075, B:17:0x007b, B:19:0x0083, B:24:0x008d, B:26:0x0093, B:27:0x0096, B:29:0x009c, B:31:0x00a6, B:32:0x00a9, B:33:0x00ac, B:35:0x00b2, B:36:0x00b7), top: B:41:0x0041 }] */
    /* renamed from: lambda$new$0$com-android-server-job-controllers-TareController  reason: not valid java name */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public /* synthetic */ void m4249lambda$new$0$comandroidserverjobcontrollersTareController(int userId, String pkgName, EconomyManagerInternal.ActionBill bill, boolean canAfford) {
        ArraySet<JobStatus> jobs;
        boolean z;
        long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        if (DEBUG) {
            Slog.d(TAG, userId + ":" + pkgName + " affordability for " + getBillName(bill) + " changed to " + canAfford);
        }
        synchronized (this.mLock) {
            ArrayMap<EconomyManagerInternal.ActionBill, Boolean> actionAffordability = (ArrayMap) this.mAffordabilityCache.get(userId, pkgName);
            if (actionAffordability == null) {
                actionAffordability = new ArrayMap<>();
                this.mAffordabilityCache.add(userId, pkgName, actionAffordability);
            }
            actionAffordability.put(bill, Boolean.valueOf(canAfford));
            ArrayMap<EconomyManagerInternal.ActionBill, ArraySet<JobStatus>> billToJobMap = (ArrayMap) this.mRegisteredBillsAndJobs.get(userId, pkgName);
            if (billToJobMap != null && (jobs = billToJobMap.get(bill)) != null) {
                ArraySet<JobStatus> changedJobs = new ArraySet<>();
                for (int i = 0; i < jobs.size(); i++) {
                    JobStatus job = jobs.valueAt(i);
                    if (!canAfford && !hasEnoughWealthLocked(job)) {
                        z = false;
                        if (job.setTareWealthConstraintSatisfied(nowElapsed, z)) {
                            changedJobs.add(job);
                        }
                        if (job.isRequestedExpeditedJob() && setExpeditedTareApproved(job, nowElapsed, canAffordExpeditedBillLocked(job))) {
                            changedJobs.add(job);
                        }
                    }
                    z = true;
                    if (job.setTareWealthConstraintSatisfied(nowElapsed, z)) {
                    }
                    if (job.isRequestedExpeditedJob()) {
                        changedJobs.add(job);
                    }
                }
                int i2 = changedJobs.size();
                if (i2 > 0) {
                    this.mStateChangedListener.onControllerStateChanged(changedJobs);
                }
            }
        }
    }

    public TareController(JobSchedulerService service, BackgroundJobsController backgroundJobsController, ConnectivityController connectivityController) {
        super(service);
        this.mAffordabilityCache = new SparseArrayMap<>();
        this.mRegisteredBillsAndJobs = new SparseArrayMap<>();
        this.mAffordabilityChangeListener = new EconomyManagerInternal.AffordabilityChangeListener() { // from class: com.android.server.job.controllers.TareController$$ExternalSyntheticLambda1
            @Override // com.android.server.tare.EconomyManagerInternal.AffordabilityChangeListener
            public final void onAffordabilityChanged(int i, String str, EconomyManagerInternal.ActionBill actionBill, boolean z) {
                TareController.this.m4249lambda$new$0$comandroidserverjobcontrollersTareController(i, str, actionBill, z);
            }
        };
        this.mTopStartedJobs = new ArraySet<>();
        this.mBackgroundJobsController = backgroundJobsController;
        this.mConnectivityController = connectivityController;
        this.mEconomyManagerInternal = (EconomyManagerInternal) LocalServices.getService(EconomyManagerInternal.class);
        this.mIsEnabled = this.mConstants.USE_TARE_POLICY;
    }

    @Override // com.android.server.job.controllers.StateController
    public void maybeStartTrackingJobLocked(JobStatus jobStatus, JobStatus lastJob) {
        long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        jobStatus.setTareWealthConstraintSatisfied(nowElapsed, hasEnoughWealthLocked(jobStatus));
        setExpeditedTareApproved(jobStatus, nowElapsed, jobStatus.isRequestedExpeditedJob() && canAffordExpeditedBillLocked(jobStatus));
        ArraySet<EconomyManagerInternal.ActionBill> bills = getPossibleStartBills(jobStatus);
        for (int i = 0; i < bills.size(); i++) {
            addJobToBillList(jobStatus, bills.valueAt(i));
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void prepareForExecutionLocked(JobStatus jobStatus) {
        int userId = jobStatus.getSourceUserId();
        String pkgName = jobStatus.getSourcePackageName();
        ArrayMap<EconomyManagerInternal.ActionBill, ArraySet<JobStatus>> billToJobMap = (ArrayMap) this.mRegisteredBillsAndJobs.get(userId, pkgName);
        if (billToJobMap == null) {
            Slog.e(TAG, "Job is being prepared but doesn't have a pre-existing billToJobMap");
        } else {
            for (int i = 0; i < billToJobMap.size(); i++) {
                removeJobFromBillList(jobStatus, billToJobMap.keyAt(i));
            }
        }
        addJobToBillList(jobStatus, getRunningBill(jobStatus));
        int uid = jobStatus.getSourceUid();
        if (this.mService.getUidBias(uid) == 40) {
            if (DEBUG) {
                Slog.d(TAG, jobStatus.toShortString() + " is top started job");
            }
            this.mTopStartedJobs.add(jobStatus);
            return;
        }
        this.mEconomyManagerInternal.noteOngoingEventStarted(userId, pkgName, getRunningActionId(jobStatus), String.valueOf(jobStatus.getJobId()));
    }

    @Override // com.android.server.job.controllers.StateController
    public void unprepareFromExecutionLocked(JobStatus jobStatus) {
        int userId = jobStatus.getSourceUserId();
        String pkgName = jobStatus.getSourcePackageName();
        this.mEconomyManagerInternal.noteOngoingEventStopped(userId, pkgName, getRunningActionId(jobStatus), String.valueOf(jobStatus.getJobId()));
        this.mTopStartedJobs.remove(jobStatus);
        ArraySet<EconomyManagerInternal.ActionBill> bills = getPossibleStartBills(jobStatus);
        ArrayMap<EconomyManagerInternal.ActionBill, ArraySet<JobStatus>> billToJobMap = (ArrayMap) this.mRegisteredBillsAndJobs.get(userId, pkgName);
        if (billToJobMap == null) {
            Slog.e(TAG, "Job was just unprepared but didn't have a pre-existing billToJobMap");
        } else {
            for (int i = 0; i < billToJobMap.size(); i++) {
                removeJobFromBillList(jobStatus, billToJobMap.keyAt(i));
            }
        }
        for (int i2 = 0; i2 < bills.size(); i2++) {
            addJobToBillList(jobStatus, bills.valueAt(i2));
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void maybeStopTrackingJobLocked(JobStatus jobStatus, JobStatus incomingJob, boolean forUpdate) {
        int userId = jobStatus.getSourceUserId();
        String pkgName = jobStatus.getSourcePackageName();
        this.mEconomyManagerInternal.noteOngoingEventStopped(userId, pkgName, getRunningActionId(jobStatus), String.valueOf(jobStatus.getJobId()));
        this.mTopStartedJobs.remove(jobStatus);
        ArrayMap<EconomyManagerInternal.ActionBill, ArraySet<JobStatus>> billToJobMap = (ArrayMap) this.mRegisteredBillsAndJobs.get(userId, pkgName);
        if (billToJobMap != null) {
            for (int i = 0; i < billToJobMap.size(); i++) {
                removeJobFromBillList(jobStatus, billToJobMap.keyAt(i));
            }
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void onConstantsUpdatedLocked() {
        if (this.mIsEnabled != this.mConstants.USE_TARE_POLICY) {
            this.mIsEnabled = this.mConstants.USE_TARE_POLICY;
            JobSchedulerBackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.job.controllers.TareController$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    TareController.this.m4251xa3f5831e();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onConstantsUpdatedLocked$2$com-android-server-job-controllers-TareController  reason: not valid java name */
    public /* synthetic */ void m4251xa3f5831e() {
        synchronized (this.mLock) {
            final long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
            this.mService.getJobStore().forEachJob(new Consumer() { // from class: com.android.server.job.controllers.TareController$$ExternalSyntheticLambda2
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    TareController.this.m4250x606a655d(nowElapsed, (JobStatus) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onConstantsUpdatedLocked$1$com-android-server-job-controllers-TareController  reason: not valid java name */
    public /* synthetic */ void m4250x606a655d(long nowElapsed, JobStatus jobStatus) {
        boolean z = true;
        if (!this.mIsEnabled) {
            jobStatus.setTareWealthConstraintSatisfied(nowElapsed, true);
            setExpeditedTareApproved(jobStatus, nowElapsed, true);
            return;
        }
        jobStatus.setTareWealthConstraintSatisfied(nowElapsed, hasEnoughWealthLocked(jobStatus));
        setExpeditedTareApproved(jobStatus, nowElapsed, (jobStatus.isRequestedExpeditedJob() && canAffordExpeditedBillLocked(jobStatus)) ? false : false);
    }

    public boolean canScheduleEJ(JobStatus jobStatus) {
        if (!this.mIsEnabled) {
            return true;
        }
        if (jobStatus.getEffectivePriority() == 500) {
            return canAffordBillLocked(jobStatus, BILL_JOB_START_MAX_EXPEDITED);
        }
        return canAffordBillLocked(jobStatus, BILL_JOB_START_HIGH_EXPEDITED);
    }

    private boolean isTopStartedJobLocked(JobStatus jobStatus) {
        return this.mTopStartedJobs.contains(jobStatus);
    }

    public long getMaxJobExecutionTimeMsLocked(JobStatus jobStatus) {
        if (!this.mIsEnabled) {
            return this.mConstants.RUNTIME_FREE_QUOTA_MAX_LIMIT_MS;
        }
        return this.mEconomyManagerInternal.getMaxDurationMs(jobStatus.getSourceUserId(), jobStatus.getSourcePackageName(), getRunningBill(jobStatus));
    }

    private void addJobToBillList(JobStatus jobStatus, EconomyManagerInternal.ActionBill bill) {
        int userId = jobStatus.getSourceUserId();
        String pkgName = jobStatus.getSourcePackageName();
        ArrayMap<EconomyManagerInternal.ActionBill, ArraySet<JobStatus>> billToJobMap = (ArrayMap) this.mRegisteredBillsAndJobs.get(userId, pkgName);
        if (billToJobMap == null) {
            billToJobMap = new ArrayMap<>();
            this.mRegisteredBillsAndJobs.add(userId, pkgName, billToJobMap);
        }
        ArraySet<JobStatus> jobs = billToJobMap.get(bill);
        if (jobs == null) {
            jobs = new ArraySet<>();
            billToJobMap.put(bill, jobs);
        }
        if (jobs.add(jobStatus)) {
            this.mEconomyManagerInternal.registerAffordabilityChangeListener(userId, pkgName, this.mAffordabilityChangeListener, bill);
        }
    }

    private void removeJobFromBillList(JobStatus jobStatus, EconomyManagerInternal.ActionBill bill) {
        int userId = jobStatus.getSourceUserId();
        String pkgName = jobStatus.getSourcePackageName();
        ArrayMap<EconomyManagerInternal.ActionBill, ArraySet<JobStatus>> billToJobMap = (ArrayMap) this.mRegisteredBillsAndJobs.get(userId, pkgName);
        if (billToJobMap != null) {
            ArraySet<JobStatus> jobs = billToJobMap.get(bill);
            if (jobs == null || (jobs.remove(jobStatus) && jobs.size() == 0)) {
                this.mEconomyManagerInternal.unregisterAffordabilityChangeListener(userId, pkgName, this.mAffordabilityChangeListener, bill);
                ArrayMap<EconomyManagerInternal.ActionBill, Boolean> actionAffordability = (ArrayMap) this.mAffordabilityCache.get(userId, pkgName);
                if (actionAffordability != null) {
                    actionAffordability.remove(bill);
                }
            }
        }
    }

    private ArraySet<EconomyManagerInternal.ActionBill> getPossibleStartBills(JobStatus jobStatus) {
        ArraySet<EconomyManagerInternal.ActionBill> bills = new ArraySet<>();
        if (jobStatus.isRequestedExpeditedJob()) {
            if (jobStatus.getEffectivePriority() == 500) {
                bills.add(BILL_JOB_START_MAX_EXPEDITED);
            } else {
                bills.add(BILL_JOB_START_HIGH_EXPEDITED);
            }
        }
        switch (jobStatus.getEffectivePriority()) {
            case 100:
                bills.add(BILL_JOB_START_MIN);
                break;
            case 200:
                bills.add(BILL_JOB_START_LOW);
                break;
            case 300:
                bills.add(BILL_JOB_START_DEFAULT);
                break;
            case 400:
                bills.add(BILL_JOB_START_HIGH);
                break;
            case 500:
                bills.add(BILL_JOB_START_MAX);
                break;
            default:
                Slog.wtf(TAG, "Unexpected priority: " + JobInfo.getPriorityString(jobStatus.getEffectivePriority()));
                break;
        }
        return bills;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private EconomyManagerInternal.ActionBill getRunningBill(JobStatus jobStatus) {
        if (jobStatus.shouldTreatAsExpeditedJob() || jobStatus.startedAsExpeditedJob) {
            if (jobStatus.getEffectivePriority() == 500) {
                return BILL_JOB_RUNNING_MAX_EXPEDITED;
            }
            return BILL_JOB_RUNNING_HIGH_EXPEDITED;
        }
        switch (jobStatus.getEffectivePriority()) {
            case 100:
                return BILL_JOB_RUNNING_MIN;
            case 200:
                return BILL_JOB_RUNNING_LOW;
            case 300:
                break;
            case 400:
                return BILL_JOB_RUNNING_HIGH;
            case 500:
                return BILL_JOB_RUNNING_MAX;
            default:
                Slog.wtf(TAG, "Got unexpected priority: " + jobStatus.getEffectivePriority());
                break;
        }
        return BILL_JOB_RUNNING_DEFAULT;
    }

    private static int getRunningActionId(JobStatus job) {
        switch (job.getEffectivePriority()) {
            case 100:
                return JobSchedulerEconomicPolicy.ACTION_JOB_MIN_RUNNING;
            case 200:
                return JobSchedulerEconomicPolicy.ACTION_JOB_LOW_RUNNING;
            case 300:
                return JobSchedulerEconomicPolicy.ACTION_JOB_DEFAULT_RUNNING;
            case 400:
                return JobSchedulerEconomicPolicy.ACTION_JOB_HIGH_RUNNING;
            case 500:
                return JobSchedulerEconomicPolicy.ACTION_JOB_MAX_RUNNING;
            default:
                Slog.wtf(TAG, "Unknown priority: " + JobInfo.getPriorityString(job.getEffectivePriority()));
                return JobSchedulerEconomicPolicy.ACTION_JOB_DEFAULT_RUNNING;
        }
    }

    private boolean canAffordBillLocked(JobStatus jobStatus, EconomyManagerInternal.ActionBill bill) {
        if (!this.mIsEnabled || this.mService.getUidBias(jobStatus.getSourceUid()) == 40 || isTopStartedJobLocked(jobStatus)) {
            return true;
        }
        int userId = jobStatus.getSourceUserId();
        String pkgName = jobStatus.getSourcePackageName();
        ArrayMap<EconomyManagerInternal.ActionBill, Boolean> actionAffordability = (ArrayMap) this.mAffordabilityCache.get(userId, pkgName);
        if (actionAffordability == null) {
            actionAffordability = new ArrayMap<>();
            this.mAffordabilityCache.add(userId, pkgName, actionAffordability);
        }
        if (actionAffordability.containsKey(bill)) {
            return actionAffordability.get(bill).booleanValue();
        }
        boolean canAfford = this.mEconomyManagerInternal.canPayFor(userId, pkgName, bill);
        actionAffordability.put(bill, Boolean.valueOf(canAfford));
        return canAfford;
    }

    private boolean canAffordExpeditedBillLocked(JobStatus jobStatus) {
        if (this.mIsEnabled) {
            if (!jobStatus.isRequestedExpeditedJob()) {
                return false;
            }
            if (this.mService.getUidBias(jobStatus.getSourceUid()) == 40 || isTopStartedJobLocked(jobStatus)) {
                return true;
            }
            if (this.mService.isCurrentlyRunningLocked(jobStatus)) {
                return canAffordBillLocked(jobStatus, getRunningBill(jobStatus));
            }
            if (jobStatus.getEffectivePriority() == 500) {
                return canAffordBillLocked(jobStatus, BILL_JOB_START_MAX_EXPEDITED);
            }
            return canAffordBillLocked(jobStatus, BILL_JOB_START_HIGH_EXPEDITED);
        }
        return true;
    }

    private boolean hasEnoughWealthLocked(JobStatus jobStatus) {
        if (!this.mIsEnabled || this.mService.getUidBias(jobStatus.getSourceUid()) == 40 || isTopStartedJobLocked(jobStatus)) {
            return true;
        }
        if (this.mService.isCurrentlyRunningLocked(jobStatus)) {
            return canAffordBillLocked(jobStatus, getRunningBill(jobStatus));
        }
        ArraySet<EconomyManagerInternal.ActionBill> bills = getPossibleStartBills(jobStatus);
        for (int i = 0; i < bills.size(); i++) {
            EconomyManagerInternal.ActionBill bill = bills.valueAt(i);
            if (canAffordBillLocked(jobStatus, bill)) {
                return true;
            }
        }
        return false;
    }

    private boolean setExpeditedTareApproved(JobStatus jobStatus, long nowElapsed, boolean isApproved) {
        if (jobStatus.setExpeditedJobTareApproved(nowElapsed, isApproved)) {
            this.mBackgroundJobsController.evaluateStateLocked(jobStatus);
            this.mConnectivityController.evaluateStateLocked(jobStatus);
            if (isApproved && jobStatus.isReady()) {
                this.mStateChangedListener.onRunJobNow(jobStatus);
                return true;
            }
            return true;
        }
        return false;
    }

    private String getBillName(EconomyManagerInternal.ActionBill bill) {
        if (bill.equals(BILL_JOB_START_MAX_EXPEDITED)) {
            return "EJ_MAX_START_BILL";
        }
        if (bill.equals(BILL_JOB_RUNNING_MAX_EXPEDITED)) {
            return "EJ_MAX_RUNNING_BILL";
        }
        if (bill.equals(BILL_JOB_START_HIGH_EXPEDITED)) {
            return "EJ_HIGH_START_BILL";
        }
        if (bill.equals(BILL_JOB_RUNNING_HIGH_EXPEDITED)) {
            return "EJ_HIGH_RUNNING_BILL";
        }
        if (bill.equals(BILL_JOB_START_HIGH)) {
            return "HIGH_START_BILL";
        }
        if (bill.equals(BILL_JOB_RUNNING_HIGH)) {
            return "HIGH_RUNNING_BILL";
        }
        if (bill.equals(BILL_JOB_START_DEFAULT)) {
            return "DEFAULT_START_BILL";
        }
        if (bill.equals(BILL_JOB_RUNNING_DEFAULT)) {
            return "DEFAULT_RUNNING_BILL";
        }
        if (bill.equals(BILL_JOB_START_LOW)) {
            return "LOW_START_BILL";
        }
        if (bill.equals(BILL_JOB_RUNNING_LOW)) {
            return "LOW_RUNNING_BILL";
        }
        if (bill.equals(BILL_JOB_START_MIN)) {
            return "MIN_START_BILL";
        }
        if (bill.equals(BILL_JOB_RUNNING_MIN)) {
            return "MIN_RUNNING_BILL";
        }
        return "UNKNOWN_BILL (" + bill + ")";
    }

    @Override // com.android.server.job.controllers.StateController
    public void dumpControllerStateLocked(final IndentingPrintWriter pw, Predicate<JobStatus> predicate) {
        pw.print("Is enabled: ");
        pw.println(this.mIsEnabled);
        pw.println("Affordability cache:");
        pw.increaseIndent();
        this.mAffordabilityCache.forEach(new SparseArrayMap.TriConsumer() { // from class: com.android.server.job.controllers.TareController$$ExternalSyntheticLambda3
            public final void accept(int i, Object obj, Object obj2) {
                TareController.this.m4248x7e64a2db(pw, i, (String) obj, (ArrayMap) obj2);
            }
        });
        pw.decreaseIndent();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dumpControllerStateLocked$3$com-android-server-job-controllers-TareController  reason: not valid java name */
    public /* synthetic */ void m4248x7e64a2db(IndentingPrintWriter pw, int userId, String pkgName, ArrayMap billMap) {
        int numBills = billMap.size();
        if (numBills > 0) {
            pw.print(userId);
            pw.print(":");
            pw.print(pkgName);
            pw.println(":");
            pw.increaseIndent();
            for (int i = 0; i < numBills; i++) {
                pw.print(getBillName((EconomyManagerInternal.ActionBill) billMap.keyAt(i)));
                pw.print(": ");
                pw.println(billMap.valueAt(i));
            }
            pw.decreaseIndent();
        }
    }
}
