package com.android.server.job.controllers;

import android.content.Context;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.proto.ProtoOutputStream;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.controllers.idle.CarIdlenessTracker;
import com.android.server.job.controllers.idle.DeviceIdlenessTracker;
import com.android.server.job.controllers.idle.IdlenessListener;
import com.android.server.job.controllers.idle.IdlenessTracker;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public final class IdleController extends RestrictingController implements IdlenessListener {
    private static final String TAG = "JobScheduler.IdleController";
    IdlenessTracker mIdleTracker;
    final ArraySet<JobStatus> mTrackedTasks;

    public IdleController(JobSchedulerService service) {
        super(service);
        this.mTrackedTasks = new ArraySet<>();
        initIdleStateTracking(this.mContext);
    }

    @Override // com.android.server.job.controllers.StateController
    public void maybeStartTrackingJobLocked(JobStatus taskStatus, JobStatus lastJob) {
        if (taskStatus.hasIdleConstraint()) {
            long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
            this.mTrackedTasks.add(taskStatus);
            taskStatus.setTrackingController(8);
            taskStatus.setIdleConstraintSatisfied(nowElapsed, this.mIdleTracker.isIdle());
        }
    }

    @Override // com.android.server.job.controllers.RestrictingController
    public void startTrackingRestrictedJobLocked(JobStatus jobStatus) {
        maybeStartTrackingJobLocked(jobStatus, null);
    }

    @Override // com.android.server.job.controllers.StateController
    public void maybeStopTrackingJobLocked(JobStatus taskStatus, JobStatus incomingJob, boolean forUpdate) {
        if (taskStatus.clearTrackingController(8)) {
            this.mTrackedTasks.remove(taskStatus);
        }
    }

    @Override // com.android.server.job.controllers.RestrictingController
    public void stopTrackingRestrictedJobLocked(JobStatus jobStatus) {
        if (!jobStatus.hasIdleConstraint()) {
            maybeStopTrackingJobLocked(jobStatus, null, false);
        }
    }

    @Override // com.android.server.job.controllers.idle.IdlenessListener
    public void reportNewIdleState(boolean isIdle) {
        synchronized (this.mLock) {
            long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
            for (int i = this.mTrackedTasks.size() - 1; i >= 0; i--) {
                this.mTrackedTasks.valueAt(i).setIdleConstraintSatisfied(nowElapsed, isIdle);
            }
        }
        this.mStateChangedListener.onControllerStateChanged(this.mTrackedTasks);
    }

    private void initIdleStateTracking(Context ctx) {
        boolean isCar = this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive");
        if (isCar) {
            this.mIdleTracker = new CarIdlenessTracker();
        } else {
            this.mIdleTracker = new DeviceIdlenessTracker();
        }
        this.mIdleTracker.startTracking(ctx, this);
    }

    @Override // com.android.server.job.controllers.StateController
    public void dumpControllerStateLocked(IndentingPrintWriter pw, Predicate<JobStatus> predicate) {
        pw.println("Currently idle: " + this.mIdleTracker.isIdle());
        pw.println("Idleness tracker:");
        this.mIdleTracker.dump(pw);
        pw.println();
        for (int i = 0; i < this.mTrackedTasks.size(); i++) {
            JobStatus js = this.mTrackedTasks.valueAt(i);
            if (predicate.test(js)) {
                pw.print("#");
                js.printUniqueId(pw);
                pw.print(" from ");
                UserHandle.formatUid(pw, js.getSourceUid());
                pw.println();
            }
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void dumpControllerStateLocked(ProtoOutputStream proto, long fieldId, Predicate<JobStatus> predicate) {
        long token = proto.start(fieldId);
        long mToken = proto.start(1146756268038L);
        proto.write(1133871366145L, this.mIdleTracker.isIdle());
        this.mIdleTracker.dump(proto, 1146756268035L);
        for (int i = 0; i < this.mTrackedTasks.size(); i++) {
            JobStatus js = this.mTrackedTasks.valueAt(i);
            if (predicate.test(js)) {
                long jsToken = proto.start(2246267895810L);
                js.writeToShortProto(proto, 1146756268033L);
                proto.write(1120986464258L, js.getSourceUid());
                proto.end(jsToken);
            }
        }
        proto.end(mToken);
        proto.end(token);
    }
}
