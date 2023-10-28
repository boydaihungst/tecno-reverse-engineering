package com.android.server.job.controllers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.jobs.ArrayUtils;
import com.android.server.DeviceIdleInternal;
import com.android.server.LocalServices;
import com.android.server.job.JobSchedulerService;
import com.android.server.pm.verify.domain.DomainVerificationPersistence;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public final class DeviceIdleJobsController extends StateController {
    private static final long BACKGROUND_JOBS_DELAY = 3000;
    private static final boolean DEBUG;
    static final int PROCESS_BACKGROUND_JOBS = 1;
    private static final String TAG = "JobScheduler.DeviceIdle";
    private final ArraySet<JobStatus> mAllowInIdleJobs;
    private final BroadcastReceiver mBroadcastReceiver;
    private boolean mDeviceIdleMode;
    private final DeviceIdleUpdateFunctor mDeviceIdleUpdateFunctor;
    private int[] mDeviceIdleWhitelistAppIds;
    private final SparseBooleanArray mForegroundUids;
    private final DeviceIdleJobsDelayHandler mHandler;
    private final DeviceIdleInternal mLocalDeviceIdleController;
    private final PowerManager mPowerManager;
    private int[] mPowerSaveTempWhitelistAppIds;
    private final Predicate<JobStatus> mShouldRushEvaluation;

    static {
        DEBUG = JobSchedulerService.DEBUG || Log.isLoggable(TAG, 3);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-job-controllers-DeviceIdleJobsController  reason: not valid java name */
    public /* synthetic */ boolean m4139xc4c26ed(JobStatus jobStatus) {
        return jobStatus.isRequestedExpeditedJob() || this.mForegroundUids.get(jobStatus.getSourceUid());
    }

    public DeviceIdleJobsController(JobSchedulerService service) {
        super(service);
        this.mForegroundUids = new SparseBooleanArray();
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.job.controllers.DeviceIdleJobsController.1
            /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                char c;
                String action = intent.getAction();
                boolean z = false;
                switch (action.hashCode()) {
                    case -712152692:
                        if (action.equals("android.os.action.POWER_SAVE_TEMP_WHITELIST_CHANGED")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    case -65633567:
                        if (action.equals("android.os.action.POWER_SAVE_WHITELIST_CHANGED")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 498807504:
                        if (action.equals("android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED")) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case 870701415:
                        if (action.equals("android.os.action.DEVICE_IDLE_MODE_CHANGED")) {
                            c = 1;
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
                        DeviceIdleJobsController deviceIdleJobsController = DeviceIdleJobsController.this;
                        if (deviceIdleJobsController.mPowerManager != null && (DeviceIdleJobsController.this.mPowerManager.isDeviceIdleMode() || DeviceIdleJobsController.this.mPowerManager.isLightDeviceIdleMode())) {
                            z = true;
                        }
                        deviceIdleJobsController.updateIdleMode(z);
                        return;
                    case 2:
                        synchronized (DeviceIdleJobsController.this.mLock) {
                            DeviceIdleJobsController deviceIdleJobsController2 = DeviceIdleJobsController.this;
                            deviceIdleJobsController2.mDeviceIdleWhitelistAppIds = deviceIdleJobsController2.mLocalDeviceIdleController.getPowerSaveWhitelistUserAppIds();
                            if (DeviceIdleJobsController.DEBUG) {
                                Slog.d(DeviceIdleJobsController.TAG, "Got whitelist " + Arrays.toString(DeviceIdleJobsController.this.mDeviceIdleWhitelistAppIds));
                            }
                        }
                        return;
                    case 3:
                        synchronized (DeviceIdleJobsController.this.mLock) {
                            DeviceIdleJobsController deviceIdleJobsController3 = DeviceIdleJobsController.this;
                            deviceIdleJobsController3.mPowerSaveTempWhitelistAppIds = deviceIdleJobsController3.mLocalDeviceIdleController.getPowerSaveTempWhitelistAppIds();
                            if (DeviceIdleJobsController.DEBUG) {
                                Slog.d(DeviceIdleJobsController.TAG, "Got temp whitelist " + Arrays.toString(DeviceIdleJobsController.this.mPowerSaveTempWhitelistAppIds));
                            }
                            ArraySet<JobStatus> changedJobs = new ArraySet<>();
                            long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
                            for (int i = 0; i < DeviceIdleJobsController.this.mAllowInIdleJobs.size(); i++) {
                                DeviceIdleJobsController deviceIdleJobsController4 = DeviceIdleJobsController.this;
                                if (deviceIdleJobsController4.updateTaskStateLocked((JobStatus) deviceIdleJobsController4.mAllowInIdleJobs.valueAt(i), nowElapsed)) {
                                    changedJobs.add((JobStatus) DeviceIdleJobsController.this.mAllowInIdleJobs.valueAt(i));
                                }
                            }
                            int i2 = changedJobs.size();
                            if (i2 > 0) {
                                DeviceIdleJobsController.this.mStateChangedListener.onControllerStateChanged(changedJobs);
                            }
                        }
                        return;
                    default:
                        return;
                }
            }
        };
        this.mBroadcastReceiver = broadcastReceiver;
        this.mShouldRushEvaluation = new Predicate() { // from class: com.android.server.job.controllers.DeviceIdleJobsController$$ExternalSyntheticLambda2
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return DeviceIdleJobsController.this.m4139xc4c26ed((JobStatus) obj);
            }
        };
        this.mHandler = new DeviceIdleJobsDelayHandler(this.mContext.getMainLooper());
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        DeviceIdleInternal deviceIdleInternal = (DeviceIdleInternal) LocalServices.getService(DeviceIdleInternal.class);
        this.mLocalDeviceIdleController = deviceIdleInternal;
        this.mDeviceIdleWhitelistAppIds = deviceIdleInternal.getPowerSaveWhitelistUserAppIds();
        this.mPowerSaveTempWhitelistAppIds = deviceIdleInternal.getPowerSaveTempWhitelistAppIds();
        this.mDeviceIdleUpdateFunctor = new DeviceIdleUpdateFunctor();
        this.mAllowInIdleJobs = new ArraySet<>();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
        filter.addAction("android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED");
        filter.addAction("android.os.action.POWER_SAVE_WHITELIST_CHANGED");
        filter.addAction("android.os.action.POWER_SAVE_TEMP_WHITELIST_CHANGED");
        this.mContext.registerReceiverAsUser(broadcastReceiver, UserHandle.ALL, filter, null, null);
    }

    void updateIdleMode(boolean enabled) {
        boolean changed = false;
        synchronized (this.mLock) {
            if (this.mDeviceIdleMode != enabled) {
                changed = true;
            }
            this.mDeviceIdleMode = enabled;
            if (DEBUG) {
                Slog.d(TAG, "mDeviceIdleMode=" + this.mDeviceIdleMode);
            }
            this.mDeviceIdleUpdateFunctor.prepare();
            if (enabled) {
                this.mHandler.removeMessages(1);
                this.mService.getJobStore().forEachJob(this.mDeviceIdleUpdateFunctor);
            } else {
                this.mService.getJobStore().forEachJob(this.mShouldRushEvaluation, this.mDeviceIdleUpdateFunctor);
                this.mHandler.sendEmptyMessageDelayed(1, 3000L);
            }
        }
        if (changed) {
            this.mStateChangedListener.onDeviceIdleStateChanged(enabled);
        }
    }

    public void setUidActiveLocked(int uid, boolean active) {
        boolean changed = active != this.mForegroundUids.get(uid);
        if (!changed) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "uid " + uid + " going " + (active ? DomainVerificationPersistence.TAG_ACTIVE : "inactive"));
        }
        this.mForegroundUids.put(uid, active);
        this.mDeviceIdleUpdateFunctor.prepare();
        this.mService.getJobStore().forEachJobForSourceUid(uid, this.mDeviceIdleUpdateFunctor);
        if (this.mDeviceIdleUpdateFunctor.mChangedJobs.size() > 0) {
            this.mStateChangedListener.onControllerStateChanged(this.mDeviceIdleUpdateFunctor.mChangedJobs);
        }
    }

    boolean isWhitelistedLocked(JobStatus job) {
        return Arrays.binarySearch(this.mDeviceIdleWhitelistAppIds, UserHandle.getAppId(job.getSourceUid())) >= 0;
    }

    boolean isTempWhitelistedLocked(JobStatus job) {
        return ArrayUtils.contains(this.mPowerSaveTempWhitelistAppIds, UserHandle.getAppId(job.getSourceUid()));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean updateTaskStateLocked(JobStatus task, long nowElapsed) {
        boolean enableTask = true;
        boolean allowInIdle = (task.getFlags() & 2) != 0 && (this.mForegroundUids.get(task.getSourceUid()) || isTempWhitelistedLocked(task));
        boolean whitelisted = isWhitelistedLocked(task);
        if (this.mDeviceIdleMode && !whitelisted && !allowInIdle) {
            enableTask = false;
        }
        return task.setDeviceNotDozingConstraintSatisfied(nowElapsed, enableTask, whitelisted);
    }

    @Override // com.android.server.job.controllers.StateController
    public void maybeStartTrackingJobLocked(JobStatus jobStatus, JobStatus lastJob) {
        if ((jobStatus.getFlags() & 2) != 0) {
            this.mAllowInIdleJobs.add(jobStatus);
        }
        updateTaskStateLocked(jobStatus, JobSchedulerService.sElapsedRealtimeClock.millis());
    }

    @Override // com.android.server.job.controllers.StateController
    public void maybeStopTrackingJobLocked(JobStatus jobStatus, JobStatus incomingJob, boolean forUpdate) {
        if ((jobStatus.getFlags() & 2) != 0) {
            this.mAllowInIdleJobs.remove(jobStatus);
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void dumpControllerStateLocked(final IndentingPrintWriter pw, Predicate<JobStatus> predicate) {
        pw.println("Idle mode: " + this.mDeviceIdleMode);
        pw.println();
        this.mService.getJobStore().forEachJob(predicate, new Consumer() { // from class: com.android.server.job.controllers.DeviceIdleJobsController$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DeviceIdleJobsController.this.m4137xbcfa6479(pw, (JobStatus) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dumpControllerStateLocked$1$com-android-server-job-controllers-DeviceIdleJobsController  reason: not valid java name */
    public /* synthetic */ void m4137xbcfa6479(IndentingPrintWriter pw, JobStatus jobStatus) {
        pw.print("#");
        jobStatus.printUniqueId(pw);
        pw.print(" from ");
        UserHandle.formatUid(pw, jobStatus.getSourceUid());
        pw.print(": ");
        pw.print(jobStatus.getSourcePackageName());
        pw.print((jobStatus.satisfiedConstraints & 33554432) != 0 ? " RUNNABLE" : " WAITING");
        if (jobStatus.appHasDozeExemption) {
            pw.print(" WHITELISTED");
        }
        if (this.mAllowInIdleJobs.contains(jobStatus)) {
            pw.print(" ALLOWED_IN_DOZE");
        }
        pw.println();
    }

    @Override // com.android.server.job.controllers.StateController
    public void dumpControllerStateLocked(final ProtoOutputStream proto, long fieldId, Predicate<JobStatus> predicate) {
        long token = proto.start(fieldId);
        long mToken = proto.start(1146756268037L);
        proto.write(1133871366145L, this.mDeviceIdleMode);
        this.mService.getJobStore().forEachJob(predicate, new Consumer() { // from class: com.android.server.job.controllers.DeviceIdleJobsController$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DeviceIdleJobsController.this.m4138x777004fa(proto, (JobStatus) obj);
            }
        });
        proto.end(mToken);
        proto.end(token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dumpControllerStateLocked$2$com-android-server-job-controllers-DeviceIdleJobsController  reason: not valid java name */
    public /* synthetic */ void m4138x777004fa(ProtoOutputStream proto, JobStatus jobStatus) {
        long jsToken = proto.start(2246267895810L);
        jobStatus.writeToShortProto(proto, 1146756268033L);
        proto.write(1120986464258L, jobStatus.getSourceUid());
        proto.write(1138166333443L, jobStatus.getSourcePackageName());
        proto.write(1133871366148L, (jobStatus.satisfiedConstraints & 33554432) != 0);
        proto.write(1133871366149L, jobStatus.appHasDozeExemption);
        proto.write(1133871366150L, this.mAllowInIdleJobs.contains(jobStatus));
        proto.end(jsToken);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class DeviceIdleUpdateFunctor implements Consumer<JobStatus> {
        final ArraySet<JobStatus> mChangedJobs = new ArraySet<>();
        long mUpdateTimeElapsed = 0;

        DeviceIdleUpdateFunctor() {
        }

        void prepare() {
            this.mChangedJobs.clear();
            this.mUpdateTimeElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Consumer
        public void accept(JobStatus jobStatus) {
            if (DeviceIdleJobsController.this.updateTaskStateLocked(jobStatus, this.mUpdateTimeElapsed)) {
                this.mChangedJobs.add(jobStatus);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class DeviceIdleJobsDelayHandler extends Handler {
        public DeviceIdleJobsDelayHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    synchronized (DeviceIdleJobsController.this.mLock) {
                        DeviceIdleJobsController.this.mDeviceIdleUpdateFunctor.prepare();
                        DeviceIdleJobsController.this.mService.getJobStore().forEachJob(DeviceIdleJobsController.this.mDeviceIdleUpdateFunctor);
                        if (DeviceIdleJobsController.this.mDeviceIdleUpdateFunctor.mChangedJobs.size() > 0) {
                            DeviceIdleJobsController.this.mStateChangedListener.onControllerStateChanged(DeviceIdleJobsController.this.mDeviceIdleUpdateFunctor.mChangedJobs);
                        }
                    }
                    return;
                default:
                    return;
            }
        }
    }
}
