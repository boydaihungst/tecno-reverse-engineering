package com.android.server.job.controllers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.server.job.JobSchedulerService;
import com.android.server.storage.DeviceStorageMonitorService;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public final class StorageController extends StateController {
    private static final boolean DEBUG;
    private static final String TAG = "JobScheduler.Storage";
    private final StorageTracker mStorageTracker;
    private final ArraySet<JobStatus> mTrackedTasks;

    static {
        DEBUG = JobSchedulerService.DEBUG || Log.isLoggable(TAG, 3);
    }

    public StorageTracker getTracker() {
        return this.mStorageTracker;
    }

    public StorageController(JobSchedulerService service) {
        super(service);
        this.mTrackedTasks = new ArraySet<>();
        StorageTracker storageTracker = new StorageTracker();
        this.mStorageTracker = storageTracker;
        storageTracker.startTracking();
    }

    @Override // com.android.server.job.controllers.StateController
    public void maybeStartTrackingJobLocked(JobStatus taskStatus, JobStatus lastJob) {
        if (taskStatus.hasStorageNotLowConstraint()) {
            long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
            this.mTrackedTasks.add(taskStatus);
            taskStatus.setTrackingController(16);
            taskStatus.setStorageNotLowConstraintSatisfied(nowElapsed, this.mStorageTracker.isStorageNotLow());
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void maybeStopTrackingJobLocked(JobStatus taskStatus, JobStatus incomingJob, boolean forUpdate) {
        if (taskStatus.clearTrackingController(16)) {
            this.mTrackedTasks.remove(taskStatus);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeReportNewStorageState() {
        long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        boolean storageNotLow = this.mStorageTracker.isStorageNotLow();
        boolean reportChange = false;
        synchronized (this.mLock) {
            for (int i = this.mTrackedTasks.size() - 1; i >= 0; i--) {
                JobStatus ts = this.mTrackedTasks.valueAt(i);
                reportChange |= ts.setStorageNotLowConstraintSatisfied(nowElapsed, storageNotLow);
            }
        }
        if (storageNotLow) {
            this.mStateChangedListener.onRunJobNow(null);
        } else if (reportChange) {
            this.mStateChangedListener.onControllerStateChanged(this.mTrackedTasks);
        }
    }

    /* loaded from: classes.dex */
    public final class StorageTracker extends BroadcastReceiver {
        private int mLastStorageSeq = -1;
        private boolean mStorageLow;

        public StorageTracker() {
        }

        public void startTracking() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.DEVICE_STORAGE_LOW");
            filter.addAction("android.intent.action.DEVICE_STORAGE_OK");
            StorageController.this.mContext.registerReceiver(this, filter);
        }

        public boolean isStorageNotLow() {
            return !this.mStorageLow;
        }

        public int getSeq() {
            return this.mLastStorageSeq;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            onReceiveInternal(intent);
        }

        public void onReceiveInternal(Intent intent) {
            String action = intent.getAction();
            this.mLastStorageSeq = intent.getIntExtra(DeviceStorageMonitorService.EXTRA_SEQUENCE, this.mLastStorageSeq);
            if ("android.intent.action.DEVICE_STORAGE_LOW".equals(action)) {
                if (StorageController.DEBUG) {
                    Slog.d(StorageController.TAG, "Available storage too low to do work. @ " + JobSchedulerService.sElapsedRealtimeClock.millis());
                }
                this.mStorageLow = true;
                StorageController.this.maybeReportNewStorageState();
            } else if ("android.intent.action.DEVICE_STORAGE_OK".equals(action)) {
                if (StorageController.DEBUG) {
                    Slog.d(StorageController.TAG, "Available storage high enough to do work. @ " + JobSchedulerService.sElapsedRealtimeClock.millis());
                }
                this.mStorageLow = false;
                StorageController.this.maybeReportNewStorageState();
            }
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void dumpControllerStateLocked(IndentingPrintWriter pw, Predicate<JobStatus> predicate) {
        pw.println("Not low: " + this.mStorageTracker.isStorageNotLow());
        pw.println("Sequence: " + this.mStorageTracker.getSeq());
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
        long mToken = proto.start(1146756268039L);
        proto.write(1133871366145L, this.mStorageTracker.isStorageNotLow());
        proto.write(1120986464258L, this.mStorageTracker.getSeq());
        for (int i = 0; i < this.mTrackedTasks.size(); i++) {
            JobStatus js = this.mTrackedTasks.valueAt(i);
            if (predicate.test(js)) {
                long jsToken = proto.start(2246267895811L);
                js.writeToShortProto(proto, 1146756268033L);
                proto.write(1120986464258L, js.getSourceUid());
                proto.end(jsToken);
            }
        }
        proto.end(mToken);
        proto.end(token);
    }
}
