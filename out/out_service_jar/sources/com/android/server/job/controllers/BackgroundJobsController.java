package com.android.server.job.controllers;

import android.app.ActivityManagerInternal;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.server.AppStateTracker;
import com.android.server.AppStateTrackerImpl;
import com.android.server.LocalServices;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.JobStore;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public final class BackgroundJobsController extends StateController {
    private static final boolean DEBUG;
    static final int KNOWN_ACTIVE = 1;
    static final int KNOWN_INACTIVE = 2;
    private static final String TAG = "JobScheduler.Background";
    static final int UNKNOWN = 0;
    private final ActivityManagerInternal mActivityManagerInternal;
    private final AppStateTrackerImpl mAppStateTracker;
    private final AppStateTrackerImpl.Listener mForceAppStandbyListener;
    private final UpdateJobFunctor mUpdateJobFunctor;

    static {
        DEBUG = JobSchedulerService.DEBUG || Log.isLoggable(TAG, 3);
    }

    public BackgroundJobsController(JobSchedulerService service) {
        super(service);
        this.mUpdateJobFunctor = new UpdateJobFunctor();
        AppStateTrackerImpl.Listener listener = new AppStateTrackerImpl.Listener() { // from class: com.android.server.job.controllers.BackgroundJobsController.1
            @Override // com.android.server.AppStateTrackerImpl.Listener
            public void updateAllJobs() {
                synchronized (BackgroundJobsController.this.mLock) {
                    BackgroundJobsController.this.updateAllJobRestrictionsLocked();
                }
            }

            @Override // com.android.server.AppStateTrackerImpl.Listener
            public void updateJobsForUid(int uid, boolean isActive) {
                synchronized (BackgroundJobsController.this.mLock) {
                    BackgroundJobsController.this.updateJobRestrictionsForUidLocked(uid, isActive);
                }
            }

            @Override // com.android.server.AppStateTrackerImpl.Listener
            public void updateJobsForUidPackage(int uid, String packageName, boolean isActive) {
                synchronized (BackgroundJobsController.this.mLock) {
                    BackgroundJobsController.this.updateJobRestrictionsForUidLocked(uid, isActive);
                }
            }
        };
        this.mForceAppStandbyListener = listener;
        this.mActivityManagerInternal = (ActivityManagerInternal) Objects.requireNonNull((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class));
        AppStateTrackerImpl appStateTrackerImpl = (AppStateTrackerImpl) Objects.requireNonNull((AppStateTracker) LocalServices.getService(AppStateTracker.class));
        this.mAppStateTracker = appStateTrackerImpl;
        appStateTrackerImpl.addListener(listener);
    }

    @Override // com.android.server.job.controllers.StateController
    public void maybeStartTrackingJobLocked(JobStatus jobStatus, JobStatus lastJob) {
        updateSingleJobRestrictionLocked(jobStatus, JobSchedulerService.sElapsedRealtimeClock.millis(), 0);
    }

    @Override // com.android.server.job.controllers.StateController
    public void maybeStopTrackingJobLocked(JobStatus jobStatus, JobStatus incomingJob, boolean forUpdate) {
    }

    @Override // com.android.server.job.controllers.StateController
    public void evaluateStateLocked(JobStatus jobStatus) {
        if (jobStatus.isRequestedExpeditedJob()) {
            updateSingleJobRestrictionLocked(jobStatus, JobSchedulerService.sElapsedRealtimeClock.millis(), 0);
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void dumpControllerStateLocked(final IndentingPrintWriter pw, Predicate<JobStatus> predicate) {
        this.mAppStateTracker.dump(pw);
        pw.println();
        this.mService.getJobStore().forEachJob(predicate, new Consumer() { // from class: com.android.server.job.controllers.BackgroundJobsController$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                BackgroundJobsController.this.m4100x16642dc(pw, (JobStatus) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dumpControllerStateLocked$0$com-android-server-job-controllers-BackgroundJobsController  reason: not valid java name */
    public /* synthetic */ void m4100x16642dc(IndentingPrintWriter pw, JobStatus jobStatus) {
        int uid = jobStatus.getSourceUid();
        String sourcePkg = jobStatus.getSourcePackageName();
        pw.print("#");
        jobStatus.printUniqueId(pw);
        pw.print(" from ");
        UserHandle.formatUid(pw, uid);
        pw.print(this.mAppStateTracker.isUidActive(uid) ? " active" : " idle");
        if (this.mAppStateTracker.isUidPowerSaveExempt(uid) || this.mAppStateTracker.isUidTempPowerSaveExempt(uid)) {
            pw.print(", exempted");
        }
        pw.print(": ");
        pw.print(sourcePkg);
        pw.print(" [RUN_ANY_IN_BACKGROUND ");
        pw.print(this.mAppStateTracker.isRunAnyInBackgroundAppOpsAllowed(uid, sourcePkg) ? "allowed]" : "disallowed]");
        if ((jobStatus.satisfiedConstraints & 4194304) != 0) {
            pw.println(" RUNNABLE");
        } else {
            pw.println(" WAITING");
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void dumpControllerStateLocked(final ProtoOutputStream proto, long fieldId, Predicate<JobStatus> predicate) {
        long token = proto.start(fieldId);
        long mToken = proto.start(1146756268033L);
        this.mAppStateTracker.dumpProto(proto, 1146756268033L);
        this.mService.getJobStore().forEachJob(predicate, new Consumer() { // from class: com.android.server.job.controllers.BackgroundJobsController$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                BackgroundJobsController.this.m4101xbbdbe35d(proto, (JobStatus) obj);
            }
        });
        proto.end(mToken);
        proto.end(token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dumpControllerStateLocked$1$com-android-server-job-controllers-BackgroundJobsController  reason: not valid java name */
    public /* synthetic */ void m4101xbbdbe35d(ProtoOutputStream proto, JobStatus jobStatus) {
        long jsToken = proto.start(2246267895810L);
        jobStatus.writeToShortProto(proto, 1146756268033L);
        int sourceUid = jobStatus.getSourceUid();
        proto.write(1120986464258L, sourceUid);
        String sourcePkg = jobStatus.getSourcePackageName();
        proto.write(1138166333443L, sourcePkg);
        proto.write(1133871366148L, this.mAppStateTracker.isUidActive(sourceUid));
        proto.write(1133871366149L, this.mAppStateTracker.isUidPowerSaveExempt(sourceUid) || this.mAppStateTracker.isUidTempPowerSaveExempt(sourceUid));
        proto.write(1133871366150L, this.mAppStateTracker.isRunAnyInBackgroundAppOpsAllowed(sourceUid, sourcePkg));
        proto.write(1133871366151L, (jobStatus.satisfiedConstraints & 4194304) != 0);
        proto.end(jsToken);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAllJobRestrictionsLocked() {
        updateJobRestrictionsLocked(-1, 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateJobRestrictionsForUidLocked(int uid, boolean isActive) {
        updateJobRestrictionsLocked(uid, isActive ? 1 : 2);
    }

    private void updateJobRestrictionsLocked(int filterUid, int newActiveState) {
        this.mUpdateJobFunctor.prepare(newActiveState);
        boolean z = DEBUG;
        long start = z ? SystemClock.elapsedRealtimeNanos() : 0L;
        JobStore store = this.mService.getJobStore();
        if (filterUid > 0) {
            store.forEachJobForSourceUid(filterUid, this.mUpdateJobFunctor);
        } else {
            store.forEachJob(this.mUpdateJobFunctor);
        }
        long time = z ? SystemClock.elapsedRealtimeNanos() - start : 0L;
        if (z) {
            Slog.d(TAG, String.format("Job status updated: %d/%d checked/total jobs, %d us", Integer.valueOf(this.mUpdateJobFunctor.mCheckedCount), Integer.valueOf(this.mUpdateJobFunctor.mTotalCount), Long.valueOf(time / 1000)));
        }
        if (this.mUpdateJobFunctor.mChangedJobs.size() > 0) {
            this.mStateChangedListener.onControllerStateChanged(this.mUpdateJobFunctor.mChangedJobs);
        }
    }

    boolean updateSingleJobRestrictionLocked(JobStatus jobStatus, long nowElapsed, int activeState) {
        boolean isActive;
        int uid = jobStatus.getSourceUid();
        String packageName = jobStatus.getSourcePackageName();
        boolean z = true;
        boolean canRun = !this.mAppStateTracker.areJobsRestricted(uid, packageName, jobStatus.canRunInBatterySaver());
        if (activeState == 0) {
            isActive = this.mAppStateTracker.isUidActive(uid);
        } else {
            isActive = activeState == 1;
        }
        if (isActive && jobStatus.getStandbyBucket() == 4) {
            jobStatus.maybeLogBucketMismatch();
        }
        if (this.mActivityManagerInternal.isBgAutoRestrictedBucketFeatureFlagEnabled() || this.mAppStateTracker.isRunAnyInBackgroundAppOpsAllowed(uid, packageName)) {
            z = false;
        }
        boolean didChange = jobStatus.setBackgroundNotRestrictedConstraintSatisfied(nowElapsed, canRun, z);
        return didChange | jobStatus.setUidActive(isActive);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class UpdateJobFunctor implements Consumer<JobStatus> {
        int mActiveState;
        final ArraySet<JobStatus> mChangedJobs;
        int mCheckedCount;
        int mTotalCount;
        long mUpdateTimeElapsed;

        private UpdateJobFunctor() {
            this.mChangedJobs = new ArraySet<>();
            this.mTotalCount = 0;
            this.mCheckedCount = 0;
            this.mUpdateTimeElapsed = 0L;
        }

        void prepare(int newActiveState) {
            this.mActiveState = newActiveState;
            this.mUpdateTimeElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
            this.mChangedJobs.clear();
            this.mTotalCount = 0;
            this.mCheckedCount = 0;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Consumer
        public void accept(JobStatus jobStatus) {
            this.mTotalCount++;
            this.mCheckedCount++;
            if (BackgroundJobsController.this.updateSingleJobRestrictionLocked(jobStatus, this.mUpdateTimeElapsed, this.mActiveState)) {
                this.mChangedJobs.add(jobStatus);
            }
        }
    }
}
