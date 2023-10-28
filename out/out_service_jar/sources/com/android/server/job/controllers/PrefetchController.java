package com.android.server.job.controllers;

import android.app.usage.UsageStatsManagerInternal;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArrayMap;
import android.util.TimeUtils;
import com.android.internal.os.SomeArgs;
import com.android.server.JobSchedulerBackgroundThread;
import com.android.server.LocalServices;
import com.android.server.job.JobSchedulerService;
import com.android.server.usage.AppStandbyController;
import com.android.server.utils.AlarmQueue;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public class PrefetchController extends StateController {
    private static final boolean DEBUG;
    private static final int MSG_PROCESS_TOP_STATE_CHANGE = 2;
    private static final int MSG_PROCESS_UPDATED_ESTIMATED_LAUNCH_TIME = 1;
    private static final int MSG_RETRIEVE_ESTIMATED_LAUNCH_TIME = 0;
    private static final String TAG = "JobScheduler.Prefetch";
    private AppWidgetManager mAppWidgetManager;
    private final UsageStatsManagerInternal.EstimatedLaunchTimeChangedListener mEstimatedLaunchTimeChangedListener;
    private final SparseArrayMap<String, Long> mEstimatedLaunchTimes;
    private final PcHandler mHandler;
    private long mLaunchTimeAllowanceMs;
    private long mLaunchTimeThresholdMs;
    private final PcConstants mPcConstants;
    private final ThresholdAlarmListener mThresholdAlarmListener;
    private final SparseArrayMap<String, ArraySet<JobStatus>> mTrackedJobs;
    private final UsageStatsManagerInternal mUsageStatsManagerInternal;

    static {
        DEBUG = JobSchedulerService.DEBUG || Log.isLoggable(TAG, 3);
    }

    public PrefetchController(JobSchedulerService service) {
        super(service);
        this.mTrackedJobs = new SparseArrayMap<>();
        this.mEstimatedLaunchTimes = new SparseArrayMap<>();
        this.mLaunchTimeThresholdMs = 25200000L;
        this.mLaunchTimeAllowanceMs = 1200000L;
        UsageStatsManagerInternal.EstimatedLaunchTimeChangedListener estimatedLaunchTimeChangedListener = new UsageStatsManagerInternal.EstimatedLaunchTimeChangedListener() { // from class: com.android.server.job.controllers.PrefetchController.1
            @Override // android.app.usage.UsageStatsManagerInternal.EstimatedLaunchTimeChangedListener
            public void onEstimatedLaunchTimeChanged(int userId, String packageName, long newEstimatedLaunchTime) {
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = packageName;
                args.argi1 = userId;
                args.argl1 = newEstimatedLaunchTime;
                PrefetchController.this.mHandler.obtainMessage(1, args).sendToTarget();
            }
        };
        this.mEstimatedLaunchTimeChangedListener = estimatedLaunchTimeChangedListener;
        this.mPcConstants = new PcConstants();
        this.mHandler = new PcHandler(this.mContext.getMainLooper());
        this.mThresholdAlarmListener = new ThresholdAlarmListener(this.mContext, JobSchedulerBackgroundThread.get().getLooper());
        UsageStatsManagerInternal usageStatsManagerInternal = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        this.mUsageStatsManagerInternal = usageStatsManagerInternal;
        usageStatsManagerInternal.registerLaunchTimeChangedListener(estimatedLaunchTimeChangedListener);
    }

    @Override // com.android.server.job.controllers.StateController
    public void onSystemServicesReady() {
        this.mAppWidgetManager = (AppWidgetManager) this.mContext.getSystemService(AppWidgetManager.class);
    }

    @Override // com.android.server.job.controllers.StateController
    public void maybeStartTrackingJobLocked(JobStatus jobStatus, JobStatus lastJob) {
        ArraySet<JobStatus> jobs;
        if (jobStatus.getJob().isPrefetch()) {
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
            long now = JobSchedulerService.sSystemClock.millis();
            long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
            if (jobs.add(jobStatus) && jobs.size() == 1 && !willBeLaunchedSoonLocked(userId, pkgName, now)) {
                updateThresholdAlarmLocked(userId, pkgName, now, nowElapsed);
            }
            updateConstraintLocked(jobStatus, now, nowElapsed);
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void maybeStopTrackingJobLocked(JobStatus jobStatus, JobStatus incomingJob, boolean forUpdate) {
        int userId = jobStatus.getSourceUserId();
        String pkgName = jobStatus.getSourcePackageName();
        ArraySet<JobStatus> jobs = (ArraySet) this.mTrackedJobs.get(userId, pkgName);
        if (jobs != null && jobs.remove(jobStatus) && jobs.size() == 0) {
            this.mThresholdAlarmListener.removeAlarmForKey(new Package(userId, pkgName));
        }
    }

    @Override // com.android.server.job.controllers.StateController
    public void onAppRemovedLocked(String packageName, int uid) {
        if (packageName == null) {
            Slog.wtf(TAG, "Told app removed but given null package name.");
            return;
        }
        int userId = UserHandle.getUserId(uid);
        this.mTrackedJobs.delete(userId, packageName);
        this.mEstimatedLaunchTimes.delete(userId, packageName);
        this.mThresholdAlarmListener.removeAlarmForKey(new Package(userId, packageName));
    }

    @Override // com.android.server.job.controllers.StateController
    public void onUserRemovedLocked(int userId) {
        this.mTrackedJobs.delete(userId);
        this.mEstimatedLaunchTimes.delete(userId);
        this.mThresholdAlarmListener.removeAlarmsForUserId(userId);
    }

    @Override // com.android.server.job.controllers.StateController
    public void onUidBiasChangedLocked(int uid, int prevBias, int newBias) {
        boolean isNowTop = newBias == 40;
        boolean wasTop = prevBias == 40;
        if (isNowTop != wasTop) {
            this.mHandler.obtainMessage(2, uid, 0).sendToTarget();
        }
    }

    public long getNextEstimatedLaunchTimeLocked(JobStatus jobStatus) {
        int userId = jobStatus.getSourceUserId();
        String pkgName = jobStatus.getSourcePackageName();
        return getNextEstimatedLaunchTimeLocked(userId, pkgName, JobSchedulerService.sSystemClock.millis());
    }

    private long getNextEstimatedLaunchTimeLocked(int userId, String pkgName, long now) {
        Long nextEstimatedLaunchTime = (Long) this.mEstimatedLaunchTimes.get(userId, pkgName);
        if (nextEstimatedLaunchTime == null || nextEstimatedLaunchTime.longValue() < now - this.mLaunchTimeAllowanceMs) {
            this.mHandler.obtainMessage(0, userId, 0, pkgName).sendToTarget();
            this.mEstimatedLaunchTimes.add(userId, pkgName, Long.valueOf((long) JobStatus.NO_LATEST_RUNTIME));
            return JobStatus.NO_LATEST_RUNTIME;
        }
        return nextEstimatedLaunchTime.longValue();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean maybeUpdateConstraintForPkgLocked(long now, long nowElapsed, int userId, String pkgName) {
        ArraySet<JobStatus> jobs = (ArraySet) this.mTrackedJobs.get(userId, pkgName);
        if (jobs == null) {
            return false;
        }
        boolean changed = false;
        for (int i = 0; i < jobs.size(); i++) {
            JobStatus js = jobs.valueAt(i);
            changed |= updateConstraintLocked(js, now, nowElapsed);
        }
        return changed;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeUpdateConstraintForUid(int uid) {
        synchronized (this.mLock) {
            try {
                try {
                    ArraySet<String> pkgs = this.mService.getPackagesForUidLocked(uid);
                    if (pkgs == null) {
                        return;
                    }
                    int userId = UserHandle.getUserId(uid);
                    ArraySet<JobStatus> changedJobs = new ArraySet<>();
                    long now = JobSchedulerService.sSystemClock.millis();
                    long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
                    for (int p = pkgs.size() - 1; p >= 0; p--) {
                        String pkgName = pkgs.valueAt(p);
                        ArraySet<JobStatus> jobs = (ArraySet) this.mTrackedJobs.get(userId, pkgName);
                        if (jobs != null) {
                            for (int i = 0; i < jobs.size(); i++) {
                                JobStatus js = jobs.valueAt(i);
                                if (updateConstraintLocked(js, now, nowElapsed)) {
                                    changedJobs.add(js);
                                }
                            }
                        }
                    }
                    if (changedJobs.size() > 0) {
                        this.mStateChangedListener.onControllerStateChanged(changedJobs);
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processUpdatedEstimatedLaunchTime(int userId, String pkgName, long newEstimatedLaunchTime) {
        Object obj;
        Object obj2;
        boolean z = DEBUG;
        if (z) {
            obj = TAG;
            Slog.d(TAG, "Estimated launch time for " + Package.packageToString(userId, pkgName) + " changed to " + newEstimatedLaunchTime + " (" + TimeUtils.formatDuration(newEstimatedLaunchTime - JobSchedulerService.sSystemClock.millis()) + " from now)");
        }
        Object obj3 = this.mLock;
        synchronized (obj3) {
            try {
                try {
                    ArraySet<JobStatus> jobs = (ArraySet) this.mTrackedJobs.get(userId, pkgName);
                    if (jobs == null) {
                        if (!z) {
                            obj2 = obj3;
                        } else {
                            Slog.i(TAG, "Not caching launch time since we haven't seen any prefetch jobs for " + Package.packageToString(userId, pkgName));
                            obj2 = obj3;
                        }
                    } else {
                        this.mEstimatedLaunchTimes.add(userId, pkgName, Long.valueOf(newEstimatedLaunchTime));
                        if (jobs.isEmpty()) {
                            obj2 = obj3;
                        } else {
                            long now = JobSchedulerService.sSystemClock.millis();
                            long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
                            updateThresholdAlarmLocked(userId, pkgName, now, nowElapsed);
                            obj2 = obj3;
                            if (maybeUpdateConstraintForPkgLocked(now, nowElapsed, userId, pkgName)) {
                                this.mStateChangedListener.onControllerStateChanged(jobs);
                            }
                        }
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                obj = obj3;
                throw th;
            }
        }
    }

    private boolean updateConstraintLocked(JobStatus jobStatus, long now, long nowElapsed) {
        AppWidgetManager appWidgetManager;
        boolean satisfied = true;
        boolean appIsOpen = this.mService.getUidBias(jobStatus.getSourceUid()) == 40;
        if (!appIsOpen) {
            int userId = jobStatus.getSourceUserId();
            String pkgName = jobStatus.getSourcePackageName();
            if (!willBeLaunchedSoonLocked(userId, pkgName, now) && ((appWidgetManager = this.mAppWidgetManager) == null || !appWidgetManager.isBoundWidgetPackage(pkgName, userId))) {
                satisfied = false;
            }
        } else {
            satisfied = this.mService.isCurrentlyRunningLocked(jobStatus);
        }
        return jobStatus.setPrefetchConstraintSatisfied(nowElapsed, satisfied);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateThresholdAlarmLocked(int userId, String pkgName, long now, long nowElapsed) {
        ArraySet<JobStatus> jobs = (ArraySet) this.mTrackedJobs.get(userId, pkgName);
        if (jobs == null || jobs.size() == 0) {
            this.mThresholdAlarmListener.removeAlarmForKey(new Package(userId, pkgName));
            return;
        }
        long nextEstimatedLaunchTime = getNextEstimatedLaunchTimeLocked(userId, pkgName, now);
        if (nextEstimatedLaunchTime != JobStatus.NO_LATEST_RUNTIME) {
            long j = this.mLaunchTimeThresholdMs;
            if (nextEstimatedLaunchTime - now > j) {
                long timeToCrossThresholdMs = nextEstimatedLaunchTime - (j + now);
                this.mThresholdAlarmListener.addAlarm(new Package(userId, pkgName), nowElapsed + timeToCrossThresholdMs);
                return;
            }
        }
        this.mThresholdAlarmListener.removeAlarmForKey(new Package(userId, pkgName));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean willBeLaunchedSoonLocked(int userId, String pkgName, long now) {
        return getNextEstimatedLaunchTimeLocked(userId, pkgName, now) <= (this.mLaunchTimeThresholdMs + now) - this.mLaunchTimeAllowanceMs;
    }

    @Override // com.android.server.job.controllers.StateController
    public void prepareForUpdatedConstantsLocked() {
        this.mPcConstants.mShouldReevaluateConstraints = false;
    }

    @Override // com.android.server.job.controllers.StateController
    public void processConstantLocked(DeviceConfig.Properties properties, String key) {
        this.mPcConstants.processConstantLocked(properties, key);
    }

    @Override // com.android.server.job.controllers.StateController
    public void onConstantsUpdatedLocked() {
        if (this.mPcConstants.mShouldReevaluateConstraints) {
            JobSchedulerBackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.job.controllers.PrefetchController$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    PrefetchController.this.m4155xc146c373();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onConstantsUpdatedLocked$0$com-android-server-job-controllers-PrefetchController  reason: not valid java name */
    public /* synthetic */ void m4155xc146c373() {
        int userId;
        int p;
        long now;
        ArraySet<JobStatus> changedJobs = new ArraySet<>();
        synchronized (this.mLock) {
            long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
            long now2 = JobSchedulerService.sSystemClock.millis();
            for (int u = 0; u < this.mTrackedJobs.numMaps(); u++) {
                int userId2 = this.mTrackedJobs.keyAt(u);
                int p2 = 0;
                while (p2 < this.mTrackedJobs.numElementsForKey(userId2)) {
                    String packageName = (String) this.mTrackedJobs.keyAt(u, p2);
                    if (maybeUpdateConstraintForPkgLocked(now2, nowElapsed, userId2, packageName)) {
                        changedJobs.addAll((ArraySet) this.mTrackedJobs.valueAt(u, p2));
                    }
                    if (willBeLaunchedSoonLocked(userId2, packageName, now2)) {
                        userId = userId2;
                        p = p2;
                        now = now2;
                    } else {
                        userId = userId2;
                        p = p2;
                        now = now2;
                        updateThresholdAlarmLocked(userId2, packageName, now2, nowElapsed);
                    }
                    p2 = p + 1;
                    userId2 = userId;
                    now2 = now;
                }
            }
        }
        if (changedJobs.size() > 0) {
            this.mStateChangedListener.onControllerStateChanged(changedJobs);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ThresholdAlarmListener extends AlarmQueue<Package> {
        private ThresholdAlarmListener(Context context, Looper looper) {
            super(context, looper, "*job.prefetch*", "Prefetch threshold", false, 2520000L);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.utils.AlarmQueue
        public boolean isForUser(Package key, int userId) {
            return key.userId == userId;
        }

        @Override // com.android.server.utils.AlarmQueue
        protected void processExpiredAlarms(ArraySet<Package> expired) {
            ArraySet<JobStatus> changedJobs = new ArraySet<>();
            synchronized (PrefetchController.this.mLock) {
                long now = JobSchedulerService.sSystemClock.millis();
                long nowElapsed = JobSchedulerService.sElapsedRealtimeClock.millis();
                for (int i = 0; i < expired.size(); i++) {
                    Package p = expired.valueAt(i);
                    if (!PrefetchController.this.willBeLaunchedSoonLocked(p.userId, p.packageName, now)) {
                        Slog.e(PrefetchController.TAG, "Alarm expired for " + Package.packageToString(p.userId, p.packageName) + " at the wrong time");
                        PrefetchController.this.updateThresholdAlarmLocked(p.userId, p.packageName, now, nowElapsed);
                    } else if (PrefetchController.this.maybeUpdateConstraintForPkgLocked(now, nowElapsed, p.userId, p.packageName)) {
                        changedJobs.addAll((ArraySet) PrefetchController.this.mTrackedJobs.get(p.userId, p.packageName));
                    }
                }
            }
            if (changedJobs.size() > 0) {
                PrefetchController.this.mStateChangedListener.onControllerStateChanged(changedJobs);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class PcHandler extends Handler {
        PcHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    int userId = msg.arg1;
                    String pkgName = (String) msg.obj;
                    long nextEstimatedLaunchTime = PrefetchController.this.mUsageStatsManagerInternal.getEstimatedPackageLaunchTime(pkgName, userId);
                    if (PrefetchController.DEBUG) {
                        Slog.d(PrefetchController.TAG, "Retrieved launch time for " + Package.packageToString(userId, pkgName) + " of " + nextEstimatedLaunchTime + " (" + TimeUtils.formatDuration(nextEstimatedLaunchTime - JobSchedulerService.sSystemClock.millis()) + " from now)");
                    }
                    synchronized (PrefetchController.this.mLock) {
                        Long curEstimatedLaunchTime = (Long) PrefetchController.this.mEstimatedLaunchTimes.get(userId, pkgName);
                        if (curEstimatedLaunchTime == null || nextEstimatedLaunchTime != curEstimatedLaunchTime.longValue()) {
                            PrefetchController.this.processUpdatedEstimatedLaunchTime(userId, pkgName, nextEstimatedLaunchTime);
                        }
                    }
                    return;
                case 1:
                    SomeArgs args = (SomeArgs) msg.obj;
                    PrefetchController.this.processUpdatedEstimatedLaunchTime(args.argi1, (String) args.arg1, args.argl1);
                    args.recycle();
                    return;
                case 2:
                    int uid = msg.arg1;
                    PrefetchController.this.maybeUpdateConstraintForUid(uid);
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    class PcConstants {
        private static final long DEFAULT_LAUNCH_TIME_ALLOWANCE_MS = 1200000;
        private static final long DEFAULT_LAUNCH_TIME_THRESHOLD_MS = 25200000;
        static final String KEY_LAUNCH_TIME_ALLOWANCE_MS = "pc_launch_time_allowance_ms";
        static final String KEY_LAUNCH_TIME_THRESHOLD_MS = "pc_launch_time_threshold_ms";
        private static final String PC_CONSTANT_PREFIX = "pc_";
        private boolean mShouldReevaluateConstraints = false;
        public long LAUNCH_TIME_THRESHOLD_MS = DEFAULT_LAUNCH_TIME_THRESHOLD_MS;
        public long LAUNCH_TIME_ALLOWANCE_MS = DEFAULT_LAUNCH_TIME_ALLOWANCE_MS;

        PcConstants() {
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        public void processConstantLocked(DeviceConfig.Properties properties, String key) {
            char c;
            switch (key.hashCode()) {
                case 1521894047:
                    if (key.equals(KEY_LAUNCH_TIME_ALLOWANCE_MS)) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 1566126444:
                    if (key.equals(KEY_LAUNCH_TIME_THRESHOLD_MS)) {
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
                    long j = properties.getLong(key, (long) DEFAULT_LAUNCH_TIME_ALLOWANCE_MS);
                    this.LAUNCH_TIME_ALLOWANCE_MS = j;
                    long newLaunchTimeAllowanceMs = Math.min((long) AppStandbyController.ConstantsObserver.DEFAULT_SYSTEM_UPDATE_TIMEOUT, Math.max(0L, j));
                    if (PrefetchController.this.mLaunchTimeAllowanceMs != newLaunchTimeAllowanceMs) {
                        PrefetchController.this.mLaunchTimeAllowanceMs = newLaunchTimeAllowanceMs;
                        this.mShouldReevaluateConstraints = true;
                        return;
                    }
                    return;
                case 1:
                    long j2 = properties.getLong(key, (long) DEFAULT_LAUNCH_TIME_THRESHOLD_MS);
                    this.LAUNCH_TIME_THRESHOLD_MS = j2;
                    long newLaunchTimeThresholdMs = Math.min(86400000L, Math.max(3600000L, j2));
                    if (PrefetchController.this.mLaunchTimeThresholdMs != newLaunchTimeThresholdMs) {
                        PrefetchController.this.mLaunchTimeThresholdMs = newLaunchTimeThresholdMs;
                        this.mShouldReevaluateConstraints = true;
                        PrefetchController.this.mThresholdAlarmListener.setMinTimeBetweenAlarmsMs(PrefetchController.this.mLaunchTimeThresholdMs / 10);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void dump(IndentingPrintWriter pw) {
            pw.println();
            pw.print(PrefetchController.class.getSimpleName());
            pw.println(":");
            pw.increaseIndent();
            pw.print(KEY_LAUNCH_TIME_THRESHOLD_MS, Long.valueOf(this.LAUNCH_TIME_THRESHOLD_MS)).println();
            pw.print(KEY_LAUNCH_TIME_ALLOWANCE_MS, Long.valueOf(this.LAUNCH_TIME_ALLOWANCE_MS)).println();
            pw.decreaseIndent();
        }
    }

    long getLaunchTimeAllowanceMs() {
        return this.mLaunchTimeAllowanceMs;
    }

    long getLaunchTimeThresholdMs() {
        return this.mLaunchTimeThresholdMs;
    }

    PcConstants getPcConstants() {
        return this.mPcConstants;
    }

    @Override // com.android.server.job.controllers.StateController
    public void dumpControllerStateLocked(final IndentingPrintWriter pw, final Predicate<JobStatus> predicate) {
        long now = JobSchedulerService.sSystemClock.millis();
        pw.println("Cached launch times:");
        pw.increaseIndent();
        for (int u = 0; u < this.mEstimatedLaunchTimes.numMaps(); u++) {
            int userId = this.mEstimatedLaunchTimes.keyAt(u);
            for (int p = 0; p < this.mEstimatedLaunchTimes.numElementsForKey(userId); p++) {
                String pkgName = (String) this.mEstimatedLaunchTimes.keyAt(u, p);
                long estimatedLaunchTime = ((Long) this.mEstimatedLaunchTimes.valueAt(u, p)).longValue();
                pw.print(Package.packageToString(userId, pkgName));
                pw.print(": ");
                pw.print(estimatedLaunchTime);
                pw.print(" (");
                TimeUtils.formatDuration(estimatedLaunchTime - now, pw, 19);
                pw.println(" from now)");
            }
        }
        pw.decreaseIndent();
        pw.println();
        this.mTrackedJobs.forEach(new Consumer() { // from class: com.android.server.job.controllers.PrefetchController$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                PrefetchController.lambda$dumpControllerStateLocked$1(predicate, pw, (ArraySet) obj);
            }
        });
        pw.println();
        this.mThresholdAlarmListener.dump(pw);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dumpControllerStateLocked$1(Predicate predicate, IndentingPrintWriter pw, ArraySet jobs) {
        for (int j = 0; j < jobs.size(); j++) {
            JobStatus js = (JobStatus) jobs.valueAt(j);
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
    public void dumpConstants(IndentingPrintWriter pw) {
        this.mPcConstants.dump(pw);
    }
}
