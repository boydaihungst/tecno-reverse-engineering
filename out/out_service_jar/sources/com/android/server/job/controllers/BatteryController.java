package com.android.server.job.controllers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManagerInternal;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.server.JobSchedulerBackgroundThread;
import com.android.server.LocalServices;
import com.android.server.job.JobSchedulerService;
import com.android.server.slice.SliceClientPermissions;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public final class BatteryController extends RestrictingController {
    private static final boolean DEBUG;
    private static final String TAG = "JobScheduler.Battery";
    private final ArraySet<JobStatus> mChangedJobs;
    private final PowerTracker mPowerTracker;
    private final ArraySet<JobStatus> mTopStartedJobs;
    private final ArraySet<JobStatus> mTrackedTasks;

    static {
        DEBUG = JobSchedulerService.DEBUG || Log.isLoggable(TAG, 3);
    }

    public BatteryController(JobSchedulerService service) {
        super(service);
        this.mTrackedTasks = new ArraySet<>();
        this.mTopStartedJobs = new ArraySet<>();
        this.mChangedJobs = new ArraySet<>();
        PowerTracker powerTracker = new PowerTracker();
        this.mPowerTracker = powerTracker;
        powerTracker.startTracking();
    }

    @Override // com.android.server.job.controllers.StateController
    public void maybeStartTrackingJobLocked(JobStatus taskStatus, JobStatus lastJob) {
        if (taskStatus.hasPowerConstraint()) {
            long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
            this.mTrackedTasks.add(taskStatus);
            boolean z = true;
            taskStatus.setTrackingController(1);
            if (taskStatus.hasChargingConstraint()) {
                if (hasTopExemptionLocked(taskStatus)) {
                    taskStatus.setChargingConstraintSatisfied(nowElapsed, this.mPowerTracker.isPowerConnected());
                } else {
                    taskStatus.setChargingConstraintSatisfied(nowElapsed, (this.mService.isBatteryCharging() && this.mService.isBatteryNotLow()) ? false : false);
                }
            }
            taskStatus.setBatteryNotLowConstraintSatisfied(nowElapsed, this.mService.isBatteryNotLow());
        }
    }

    @Override // com.android.server.job.controllers.RestrictingController
    public void startTrackingRestrictedJobLocked(JobStatus jobStatus) {
        maybeStartTrackingJobLocked(jobStatus, null);
    }

    @Override // com.android.server.job.controllers.StateController
    public void prepareForExecutionLocked(JobStatus jobStatus) {
        boolean z = DEBUG;
        if (z) {
            Slog.d(TAG, "Prepping for " + jobStatus.toShortString());
        }
        int uid = jobStatus.getSourceUid();
        if (this.mService.getUidBias(uid) == 40) {
            if (z) {
                Slog.d(TAG, jobStatus.toShortString() + " is top started job");
            }
            this.mTopStartedJobs.add(jobStatus);
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void unprepareFromExecutionLocked(JobStatus jobStatus) {
        this.mTopStartedJobs.remove(jobStatus);
    }

    @Override // com.android.server.job.controllers.StateController
    public void maybeStopTrackingJobLocked(JobStatus taskStatus, JobStatus incomingJob, boolean forUpdate) {
        if (taskStatus.clearTrackingController(1)) {
            this.mTrackedTasks.remove(taskStatus);
            this.mTopStartedJobs.remove(taskStatus);
        }
    }

    @Override // com.android.server.job.controllers.RestrictingController
    public void stopTrackingRestrictedJobLocked(JobStatus jobStatus) {
        if (!jobStatus.hasPowerConstraint()) {
            maybeStopTrackingJobLocked(jobStatus, null, false);
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void onBatteryStateChangedLocked() {
        JobSchedulerBackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.job.controllers.BatteryController$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                BatteryController.this.m4104x8e4fc8dd();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onBatteryStateChangedLocked$0$com-android-server-job-controllers-BatteryController  reason: not valid java name */
    public /* synthetic */ void m4104x8e4fc8dd() {
        synchronized (this.mLock) {
            maybeReportNewChargingStateLocked();
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void onUidBiasChangedLocked(int uid, int prevBias, int newBias) {
        if (prevBias == 40 || newBias == 40) {
            maybeReportNewChargingStateLocked();
        }
    }

    private boolean hasTopExemptionLocked(JobStatus taskStatus) {
        return this.mService.getUidBias(taskStatus.getSourceUid()) == 40 || this.mTopStartedJobs.contains(taskStatus);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeReportNewChargingStateLocked() {
        boolean powerConnected = this.mPowerTracker.isPowerConnected();
        boolean stablePower = this.mService.isBatteryCharging() && this.mService.isBatteryNotLow();
        boolean batteryNotLow = this.mService.isBatteryNotLow();
        if (DEBUG) {
            Slog.d(TAG, "maybeReportNewChargingStateLocked: " + powerConnected + SliceClientPermissions.SliceAuthority.DELIMITER + stablePower + SliceClientPermissions.SliceAuthority.DELIMITER + batteryNotLow);
        }
        long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        for (int i = this.mTrackedTasks.size() - 1; i >= 0; i--) {
            JobStatus ts = this.mTrackedTasks.valueAt(i);
            if (ts.hasChargingConstraint()) {
                if (hasTopExemptionLocked(ts) && ts.getEffectivePriority() >= 300) {
                    if (ts.setChargingConstraintSatisfied(nowElapsed, powerConnected)) {
                        this.mChangedJobs.add(ts);
                    }
                } else if (ts.setChargingConstraintSatisfied(nowElapsed, stablePower)) {
                    this.mChangedJobs.add(ts);
                }
            }
            if (ts.hasBatteryNotLowConstraint() && ts.setBatteryNotLowConstraintSatisfied(nowElapsed, batteryNotLow)) {
                this.mChangedJobs.add(ts);
            }
        }
        if (stablePower || batteryNotLow) {
            this.mStateChangedListener.onRunJobNow(null);
        } else if (this.mChangedJobs.size() > 0) {
            this.mStateChangedListener.onControllerStateChanged(this.mChangedJobs);
        }
        this.mChangedJobs.clear();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class PowerTracker extends BroadcastReceiver {
        private boolean mPowerConnected;

        PowerTracker() {
        }

        void startTracking() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
            filter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
            BatteryController.this.mContext.registerReceiver(this, filter);
            BatteryManagerInternal batteryManagerInternal = (BatteryManagerInternal) LocalServices.getService(BatteryManagerInternal.class);
            this.mPowerConnected = batteryManagerInternal.isPowered(15);
        }

        boolean isPowerConnected() {
            return this.mPowerConnected;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            synchronized (BatteryController.this.mLock) {
                String action = intent.getAction();
                if ("android.intent.action.ACTION_POWER_CONNECTED".equals(action)) {
                    if (BatteryController.DEBUG) {
                        Slog.d(BatteryController.TAG, "Power connected @ " + JobSchedulerService.sElapsedRealtimeClock.millis());
                    }
                    if (this.mPowerConnected) {
                        return;
                    }
                    this.mPowerConnected = true;
                } else if ("android.intent.action.ACTION_POWER_DISCONNECTED".equals(action)) {
                    if (BatteryController.DEBUG) {
                        Slog.d(BatteryController.TAG, "Power disconnected @ " + JobSchedulerService.sElapsedRealtimeClock.millis());
                    }
                    if (!this.mPowerConnected) {
                        return;
                    }
                    this.mPowerConnected = false;
                }
                BatteryController.this.maybeReportNewChargingStateLocked();
            }
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void dumpControllerStateLocked(IndentingPrintWriter pw, Predicate<JobStatus> predicate) {
        pw.println("Power connected: " + this.mPowerTracker.isPowerConnected());
        pw.println("Stable power: " + (this.mService.isBatteryCharging() && this.mService.isBatteryNotLow()));
        pw.println("Not low: " + this.mService.isBatteryNotLow());
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
        long mToken = proto.start(1146756268034L);
        proto.write(1133871366145L, this.mService.isBatteryCharging() && this.mService.isBatteryNotLow());
        proto.write(1133871366146L, this.mService.isBatteryNotLow());
        for (int i = 0; i < this.mTrackedTasks.size(); i++) {
            JobStatus js = this.mTrackedTasks.valueAt(i);
            if (predicate.test(js)) {
                long jsToken = proto.start(2246267895813L);
                js.writeToShortProto(proto, 1146756268033L);
                proto.write(1120986464258L, js.getSourceUid());
                proto.end(jsToken);
            }
        }
        proto.end(mToken);
        proto.end(token);
    }
}
