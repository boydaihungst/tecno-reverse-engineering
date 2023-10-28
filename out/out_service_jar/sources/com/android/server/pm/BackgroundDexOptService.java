package com.android.server.pm;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.os.BatteryManagerInternal;
import android.os.Binder;
import android.os.Environment;
import android.os.IThermalService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.LocalServices;
import com.android.server.PinnerService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.dex.ArtStatsLogUtils;
import com.android.server.pm.dex.DexManager;
import com.android.server.pm.dex.DexoptOptions;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.android.server.utils.TimingsTraceAndSlog;
import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public final class BackgroundDexOptService {
    private static final long CANCELLATION_WAIT_CHECK_INTERVAL_MS = 200;
    static final int JOB_IDLE_OPTIMIZE = 800;
    static final int JOB_POST_BOOT_UPDATE = 801;
    private static final int LOW_THRESHOLD_MULTIPLIER_FOR_DOWNGRADE = 2;
    public static final int STATUS_ABORT_BATTERY = 4;
    public static final int STATUS_ABORT_BY_CANCELLATION = 1;
    public static final int STATUS_ABORT_NO_SPACE_LEFT = 2;
    public static final int STATUS_ABORT_THERMAL = 3;
    public static final int STATUS_DEX_OPT_FAILED = 5;
    public static final int STATUS_OK = 0;
    private static final int THERMAL_CUTOFF_DEFAULT = 2;
    private Thread mDexOptCancellingThread;
    private final DexOptHelper mDexOptHelper;
    private Thread mDexOptThread;
    private final long mDowngradeUnusedAppsThresholdInMillis;
    private final ArraySet<String> mFailedPackageNamesPrimary;
    private final ArraySet<String> mFailedPackageNamesSecondary;
    private boolean mFinishedPostBootUpdate;
    private final Injector mInjector;
    private final ArraySet<String> mLastCancelledPackages;
    private long mLastExecutionDurationIncludingSleepMs;
    private long mLastExecutionDurationMs;
    private long mLastExecutionStartTimeMs;
    private long mLastExecutionStartUptimeMs;
    private int mLastExecutionStatus;
    private final Object mLock;
    private List<PackagesUpdatedListener> mPackagesUpdatedListeners;
    private final ArtStatsLogUtils.BackgroundDexoptJobStatsLogger mStatsLogger;
    private int mThermalStatusCutoff;
    private static final String TAG = "BackgroundDexOptService";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final long IDLE_OPTIMIZATION_PERIOD = TimeUnit.DAYS.toMillis(1);
    private static ComponentName sDexoptServiceName = new ComponentName(PackageManagerService.PLATFORM_PACKAGE_NAME, BackgroundDexOptJobService.class.getName());

    /* loaded from: classes2.dex */
    public interface PackagesUpdatedListener {
        void onPackagesUpdated(ArraySet<String> arraySet);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface Status {
    }

    public BackgroundDexOptService(Context context, DexManager dexManager, PackageManagerService pm) {
        this(new Injector(context, dexManager, pm));
    }

    public BackgroundDexOptService(Injector injector) {
        this.mStatsLogger = new ArtStatsLogUtils.BackgroundDexoptJobStatsLogger();
        this.mLock = new Object();
        this.mLastExecutionStatus = 0;
        this.mLastCancelledPackages = new ArraySet<>();
        this.mFailedPackageNamesPrimary = new ArraySet<>();
        this.mFailedPackageNamesSecondary = new ArraySet<>();
        this.mPackagesUpdatedListeners = new ArrayList();
        this.mThermalStatusCutoff = 2;
        this.mInjector = injector;
        this.mDexOptHelper = injector.getDexOptHelper();
        LocalServices.addService(BackgroundDexOptService.class, this);
        this.mDowngradeUnusedAppsThresholdInMillis = injector.getDowngradeUnusedAppsThresholdInMillis();
    }

    public void systemReady() {
        if (this.mInjector.isBackgroundDexOptDisabled()) {
            return;
        }
        this.mInjector.getContext().registerReceiver(new BroadcastReceiver() { // from class: com.android.server.pm.BackgroundDexOptService.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                BackgroundDexOptService.this.mInjector.getContext().unregisterReceiver(this);
                BackgroundDexOptService.this.scheduleAJob(BackgroundDexOptService.JOB_POST_BOOT_UPDATE);
                BackgroundDexOptService.this.scheduleAJob(800);
                if (BackgroundDexOptService.DEBUG) {
                    Slog.d(BackgroundDexOptService.TAG, "BootBgDexopt scheduled");
                }
            }
        }, new IntentFilter("android.intent.action.BOOT_COMPLETED"));
    }

    public void dump(IndentingPrintWriter writer) {
        boolean disabled = this.mInjector.isBackgroundDexOptDisabled();
        writer.print("enabled:");
        writer.println(!disabled);
        if (disabled) {
            return;
        }
        synchronized (this.mLock) {
            writer.print("mDexOptThread:");
            writer.println(this.mDexOptThread);
            writer.print("mDexOptCancellingThread:");
            writer.println(this.mDexOptCancellingThread);
            writer.print("mFinishedPostBootUpdate:");
            writer.println(this.mFinishedPostBootUpdate);
            writer.print("mLastExecutionStatus:");
            writer.println(this.mLastExecutionStatus);
            writer.print("mLastExecutionStartTimeMs:");
            writer.println(this.mLastExecutionStartTimeMs);
            writer.print("mLastExecutionDurationIncludingSleepMs:");
            writer.println(this.mLastExecutionDurationIncludingSleepMs);
            writer.print("mLastExecutionStartUptimeMs:");
            writer.println(this.mLastExecutionStartUptimeMs);
            writer.print("mLastExecutionDurationMs:");
            writer.println(this.mLastExecutionDurationMs);
            writer.print("now:");
            writer.println(SystemClock.elapsedRealtime());
            writer.print("mLastCancelledPackages:");
            writer.println(String.join(",", this.mLastCancelledPackages));
            writer.print("mFailedPackageNamesPrimary:");
            writer.println(String.join(",", this.mFailedPackageNamesPrimary));
            writer.print("mFailedPackageNamesSecondary:");
            writer.println(String.join(",", this.mFailedPackageNamesSecondary));
        }
    }

    public static BackgroundDexOptService getService() {
        return (BackgroundDexOptService) LocalServices.getService(BackgroundDexOptService.class);
    }

    public boolean runBackgroundDexoptJob(List<String> packageNames) {
        List<String> packagesToOptimize;
        enforceRootOrShell();
        long identity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                waitForDexOptThreadToFinishLocked();
                resetStatesForNewDexOptRunLocked(Thread.currentThread());
            }
            PackageManagerService pm = this.mInjector.getPackageManagerService();
            if (packageNames == null) {
                packagesToOptimize = this.mDexOptHelper.getOptimizablePackages(pm.snapshotComputer());
            } else {
                packagesToOptimize = packageNames;
            }
            return runIdleOptimization(pm, packagesToOptimize, false);
        } finally {
            Binder.restoreCallingIdentity(identity);
            markDexOptCompleted();
        }
    }

    public void cancelBackgroundDexoptJob() {
        enforceRootOrShell();
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.pm.BackgroundDexOptService$$ExternalSyntheticLambda1
            public final void runOrThrow() {
                BackgroundDexOptService.this.m5378x692144df();
            }
        });
    }

    public void addPackagesUpdatedListener(PackagesUpdatedListener listener) {
        synchronized (this.mLock) {
            this.mPackagesUpdatedListeners.add(listener);
        }
    }

    public void removePackagesUpdatedListener(PackagesUpdatedListener listener) {
        synchronized (this.mLock) {
            this.mPackagesUpdatedListeners.remove(listener);
        }
    }

    public void notifyPackageChanged(String packageName) {
        synchronized (this.mLock) {
            this.mFailedPackageNamesPrimary.remove(packageName);
            this.mFailedPackageNamesSecondary.remove(packageName);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean onStartJob(final BackgroundDexOptJobService job, final JobParameters params) {
        Slog.i(TAG, "onStartJob:" + params.getJobId());
        boolean isPostBootUpdateJob = params.getJobId() == JOB_POST_BOOT_UPDATE;
        final String dex2oat_threads = SystemProperties.get("dalvik.vm.background-dex2oat-threads");
        final String cpu_set = SystemProperties.get("dalvik.vm.background-dex2oat-cpu-set");
        final PackageManagerService pm = this.mInjector.getPackageManagerService();
        if (pm.isStorageLow()) {
            Slog.w(TAG, "Low storage, skipping this run");
            markPostBootUpdateCompleted(params);
            return false;
        }
        final List<String> pkgs = this.mDexOptHelper.getOptimizablePackages(pm.snapshotComputer());
        if (!pkgs.isEmpty()) {
            this.mThermalStatusCutoff = this.mInjector.getDexOptThermalCutoff();
            synchronized (this.mLock) {
                try {
                    try {
                        Thread thread = this.mDexOptThread;
                        if (thread == null || !thread.isAlive()) {
                            if (isPostBootUpdateJob || this.mFinishedPostBootUpdate) {
                                if (!isPostBootUpdateJob) {
                                    if (SystemProperties.getBoolean("ro.tinno.project", false)) {
                                        SystemProperties.set("dalvik.vm.background-dex2oat-threads", "8");
                                        SystemProperties.set("dalvik.vm.background-dex2oat-cpu-set", "0,1,2,3,4,5,6,7");
                                    } else {
                                        SystemProperties.set("dalvik.vm.background-dex2oat-threads", "4");
                                        SystemProperties.set("dalvik.vm.background-dex2oat-cpu-set", "0,1,2,3");
                                    }
                                    Slog.i(TAG, "background-dex2oat-threads = " + SystemProperties.get("dalvik.vm.background-dex2oat-threads") + " background-dex2oat-cpu-set = " + SystemProperties.get("dalvik.vm.background-dex2oat-cpu-set"));
                                }
                                resetStatesForNewDexOptRunLocked(this.mInjector.createAndStartThread("BackgroundDexOptService_" + (isPostBootUpdateJob ? "PostBoot" : "Idle"), new Runnable() { // from class: com.android.server.pm.BackgroundDexOptService$$ExternalSyntheticLambda3
                                    @Override // java.lang.Runnable
                                    public final void run() {
                                        BackgroundDexOptService.this.m5379x7fc0bf3b(pm, pkgs, params, job, dex2oat_threads, cpu_set);
                                    }
                                }));
                                return true;
                            }
                            return false;
                        }
                        return false;
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
        Slog.i(TAG, "No packages to optimize");
        markPostBootUpdateCompleted(params);
        return false;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [393=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onStartJob$1$com-android-server-pm-BackgroundDexOptService  reason: not valid java name */
    public /* synthetic */ void m5379x7fc0bf3b(PackageManagerService pm, List pkgs, JobParameters params, BackgroundDexOptJobService job, String dex2oat_threads, String cpu_set) {
        TimingsTraceAndSlog tr = new TimingsTraceAndSlog(TAG, 262144L);
        tr.traceBegin("jobExecution");
        try {
            try {
                boolean completed = runIdleOptimization(pm, pkgs, params.getJobId() == JOB_POST_BOOT_UPDATE);
                tr.traceEnd();
                Slog.i(TAG, "dexopt finishing. jobid:" + params.getJobId() + " completed:" + completed);
                writeStatsLog(params);
                if (params.getJobId() == JOB_POST_BOOT_UPDATE) {
                    if (completed) {
                        markPostBootUpdateCompleted(params);
                    }
                    job.jobFinished(params, !completed);
                } else {
                    SystemProperties.set("dalvik.vm.background-dex2oat-threads", dex2oat_threads);
                    SystemProperties.set("dalvik.vm.background-dex2oat-cpu-set", cpu_set);
                    job.jobFinished(params, true);
                }
                markDexOptCompleted();
            } catch (Throwable th) {
                th = th;
                tr.traceEnd();
                Slog.i(TAG, "dexopt finishing. jobid:" + params.getJobId() + " completed:false");
                writeStatsLog(params);
                if (params.getJobId() == JOB_POST_BOOT_UPDATE) {
                    if (0 != 0) {
                        markPostBootUpdateCompleted(params);
                    }
                    job.jobFinished(params, !false);
                } else {
                    SystemProperties.set("dalvik.vm.background-dex2oat-threads", dex2oat_threads);
                    SystemProperties.set("dalvik.vm.background-dex2oat-cpu-set", cpu_set);
                    job.jobFinished(params, true);
                }
                markDexOptCompleted();
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean onStopJob(BackgroundDexOptJobService job, JobParameters params) {
        Slog.i(TAG, "onStopJob:" + params.getJobId());
        this.mInjector.createAndStartThread("DexOptCancel", new Runnable() { // from class: com.android.server.pm.BackgroundDexOptService$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                BackgroundDexOptService.this.m5378x692144df();
            }
        });
        return true;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: cancelDexOptAndWaitForCompletion */
    public void m5378x692144df() {
        synchronized (this.mLock) {
            if (this.mDexOptThread == null) {
                return;
            }
            Thread thread = this.mDexOptCancellingThread;
            if (thread != null && thread.isAlive()) {
                waitForDexOptThreadToFinishLocked();
                return;
            }
            this.mDexOptCancellingThread = Thread.currentThread();
            controlDexOptBlockingLocked(true);
            waitForDexOptThreadToFinishLocked();
            this.mDexOptCancellingThread = null;
            this.mDexOptThread = null;
            controlDexOptBlockingLocked(false);
            this.mLock.notifyAll();
        }
    }

    private void waitForDexOptThreadToFinishLocked() {
        TimingsTraceAndSlog tr = new TimingsTraceAndSlog(TAG, 262144L);
        tr.traceBegin("waitForDexOptThreadToFinishLocked");
        while (true) {
            try {
                Thread thread = this.mDexOptThread;
                if (thread == null || !thread.isAlive()) {
                    break;
                }
                this.mLock.wait(CANCELLATION_WAIT_CHECK_INTERVAL_MS);
            } catch (InterruptedException e) {
                Slog.w(TAG, "Interrupted while waiting for dexopt thread");
                Thread.currentThread().interrupt();
            }
        }
        tr.traceEnd();
    }

    private void markDexOptCompleted() {
        synchronized (this.mLock) {
            if (this.mDexOptThread != Thread.currentThread()) {
                throw new IllegalStateException("Only mDexOptThread can mark completion, mDexOptThread:" + this.mDexOptThread + " current:" + Thread.currentThread());
            }
            this.mDexOptThread = null;
            this.mLock.notifyAll();
        }
    }

    private void resetStatesForNewDexOptRunLocked(Thread thread) {
        this.mDexOptThread = thread;
        this.mLastCancelledPackages.clear();
        controlDexOptBlockingLocked(false);
    }

    private void enforceRootOrShell() {
        int uid = Binder.getCallingUid();
        if (uid != 0 && uid != 2000) {
            throw new SecurityException("Should be shell or root user");
        }
    }

    private void controlDexOptBlockingLocked(boolean block) {
        this.mInjector.getPackageManagerService();
        this.mDexOptHelper.controlDexOptBlocking(block);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleAJob(int jobId) {
        JobScheduler js = this.mInjector.getJobScheduler();
        JobInfo.Builder builder = new JobInfo.Builder(jobId, sDexoptServiceName).setRequiresDeviceIdle(true);
        if (jobId == 800) {
            builder.setRequiresCharging(true).setPeriodic(IDLE_OPTIMIZATION_PERIOD);
        }
        js.schedule(builder.build());
    }

    private long getLowStorageThreshold() {
        long lowThreshold = this.mInjector.getDataDirStorageLowBytes();
        if (lowThreshold == 0) {
            Slog.e(TAG, "Invalid low storage threshold");
        }
        return lowThreshold;
    }

    private void logStatus(int status) {
        switch (status) {
            case 0:
                Slog.i(TAG, "Idle optimizations completed.");
                return;
            case 1:
                Slog.w(TAG, "Idle optimizations aborted by cancellation.");
                return;
            case 2:
                Slog.w(TAG, "Idle optimizations aborted because of space constraints.");
                return;
            case 3:
                Slog.w(TAG, "Idle optimizations aborted by thermal throttling.");
                return;
            case 4:
                Slog.w(TAG, "Idle optimizations aborted by low battery.");
                return;
            case 5:
                Slog.w(TAG, "Idle optimizations failed from dexopt.");
                return;
            default:
                Slog.w(TAG, "Idle optimizations ended with unexpected code: " + status);
                return;
        }
    }

    private boolean runIdleOptimization(PackageManagerService pm, List<String> pkgs, boolean isPostBootUpdate) {
        synchronized (this.mLock) {
            this.mLastExecutionStartTimeMs = SystemClock.elapsedRealtime();
            this.mLastExecutionDurationIncludingSleepMs = -1L;
            this.mLastExecutionStartUptimeMs = SystemClock.uptimeMillis();
            this.mLastExecutionDurationMs = -1L;
        }
        long lowStorageThreshold = getLowStorageThreshold();
        int status = idleOptimizePackages(pm, pkgs, lowStorageThreshold, isPostBootUpdate);
        logStatus(status);
        synchronized (this.mLock) {
            this.mLastExecutionStatus = status;
            this.mLastExecutionDurationIncludingSleepMs = SystemClock.elapsedRealtime() - this.mLastExecutionStartTimeMs;
            this.mLastExecutionDurationMs = SystemClock.uptimeMillis() - this.mLastExecutionStartUptimeMs;
        }
        if (status != 0 && status != 5) {
            return false;
        }
        return true;
    }

    private long getDirectorySize(File f) {
        File[] listFiles;
        long size = 0;
        if (f.isDirectory()) {
            for (File file : f.listFiles()) {
                size += getDirectorySize(file);
            }
            return size;
        }
        long size2 = f.length();
        return size2;
    }

    private long getPackageSize(Computer snapshot, String pkg) {
        String[] strArr;
        PackageInfo info = snapshot.getPackageInfo(pkg, 0L, 0);
        if (info == null || info.applicationInfo == null) {
            return 0L;
        }
        File path = Paths.get(info.applicationInfo.sourceDir, new String[0]).toFile();
        if (path.isFile()) {
            path = path.getParentFile();
        }
        long size = 0 + getDirectorySize(path);
        if (!ArrayUtils.isEmpty(info.applicationInfo.splitSourceDirs)) {
            for (String splitSourceDir : info.applicationInfo.splitSourceDirs) {
                File path2 = Paths.get(splitSourceDir, new String[0]).toFile();
                if (path2.isFile()) {
                    path2 = path2.getParentFile();
                }
                size += getDirectorySize(path2);
            }
        }
        return size;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [688=7, 692=6] */
    private int idleOptimizePackages(PackageManagerService pm, List<String> pkgs, long lowStorageThreshold, boolean isPostBootUpdate) {
        List<String> list;
        ArrayList pkgs2;
        int status;
        int result;
        ArraySet<String> updatedPackages = new ArraySet<>();
        try {
            boolean supportSecondaryDex = this.mInjector.supportSecondaryDex();
            if (supportSecondaryDex && (result = reconcileSecondaryDexFiles()) != 0) {
                notifyPinService(updatedPackages);
                notifyPackagesUpdated(updatedPackages);
                return result;
            }
            long lowStorageThresholdForDowngrade = lowStorageThreshold * 2;
            boolean shouldDowngrade = shouldDowngrade(lowStorageThresholdForDowngrade);
            boolean z = DEBUG;
            if (z) {
                Slog.d(TAG, "Should Downgrade " + shouldDowngrade);
            }
            try {
                if (shouldDowngrade) {
                    Computer snapshot = pm.snapshotComputer();
                    Set<String> unusedPackages = snapshot.getUnusedPackages(this.mDowngradeUnusedAppsThresholdInMillis);
                    if (z) {
                        Slog.d(TAG, "Unsused Packages " + String.join(",", unusedPackages));
                    }
                    if (unusedPackages.isEmpty()) {
                        list = pkgs;
                    } else {
                        for (String pkg : unusedPackages) {
                            int abortCode = abortIdleOptimizations(-1L);
                            if (abortCode != 0) {
                                notifyPinService(updatedPackages);
                                notifyPackagesUpdated(updatedPackages);
                                return abortCode;
                            }
                            int downgradeResult = downgradePackage(snapshot, pm, pkg, true, isPostBootUpdate);
                            if (downgradeResult == 1) {
                                updatedPackages.add(pkg);
                            }
                            int status2 = convertPackageDexOptimizerStatusToInternal(downgradeResult);
                            if (status2 != 0) {
                                notifyPinService(updatedPackages);
                                notifyPackagesUpdated(updatedPackages);
                                return status2;
                            } else if (supportSecondaryDex && (status = convertPackageDexOptimizerStatusToInternal(downgradePackage(snapshot, pm, pkg, false, isPostBootUpdate))) != 0) {
                                notifyPinService(updatedPackages);
                                notifyPackagesUpdated(updatedPackages);
                                return status;
                            }
                        }
                        try {
                            ArrayList arrayList = new ArrayList(pkgs);
                            try {
                                arrayList.removeAll(unusedPackages);
                                pkgs2 = arrayList;
                                int optimizePackages = optimizePackages(pkgs2, lowStorageThreshold, updatedPackages, isPostBootUpdate);
                                notifyPinService(updatedPackages);
                                notifyPackagesUpdated(updatedPackages);
                                return optimizePackages;
                            } catch (Throwable th) {
                                th = th;
                                notifyPinService(updatedPackages);
                                notifyPackagesUpdated(updatedPackages);
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                } else {
                    list = pkgs;
                }
                int optimizePackages2 = optimizePackages(pkgs2, lowStorageThreshold, updatedPackages, isPostBootUpdate);
                notifyPinService(updatedPackages);
                notifyPackagesUpdated(updatedPackages);
                return optimizePackages2;
            } catch (Throwable th3) {
                th = th3;
                notifyPinService(updatedPackages);
                notifyPackagesUpdated(updatedPackages);
                throw th;
            }
            pkgs2 = list;
        } catch (Throwable th4) {
            th = th4;
        }
    }

    private int optimizePackages(List<String> pkgs, long lowStorageThreshold, ArraySet<String> updatedPackages, boolean isPostBootUpdate) {
        boolean supportSecondaryDex = this.mInjector.supportSecondaryDex();
        int status = 0;
        for (String pkg : pkgs) {
            int abortCode = abortIdleOptimizations(lowStorageThreshold);
            if (abortCode != 0) {
                return abortCode;
            }
            int primaryResult = optimizePackage(pkg, true, isPostBootUpdate);
            if (primaryResult == 2) {
                return 1;
            }
            if (primaryResult == 1) {
                updatedPackages.add(pkg);
            } else if (primaryResult == -1) {
                status = convertPackageDexOptimizerStatusToInternal(primaryResult);
            }
            if (supportSecondaryDex) {
                int secondaryResult = optimizePackage(pkg, false, isPostBootUpdate);
                if (secondaryResult == 2) {
                    return 1;
                }
                if (secondaryResult == -1) {
                    status = convertPackageDexOptimizerStatusToInternal(secondaryResult);
                }
            }
        }
        return status;
    }

    private int downgradePackage(Computer snapshot, PackageManagerService pm, String pkg, boolean isForPrimaryDex, boolean isPostBootUpdate) {
        int dexoptFlags;
        int result;
        if (DEBUG) {
            Slog.d(TAG, "Downgrading " + pkg);
        }
        if (isCancelling()) {
            return 2;
        }
        if (isPostBootUpdate) {
            dexoptFlags = 36;
        } else {
            int dexoptFlags2 = 36 | 512;
            dexoptFlags = dexoptFlags2;
        }
        long package_size_before = getPackageSize(snapshot, pkg);
        if (isForPrimaryDex || PackageManagerService.PLATFORM_PACKAGE_NAME.equals(pkg)) {
            if (!pm.canHaveOatDir(snapshot, pkg)) {
                pm.deleteOatArtifactsOfPackage(snapshot, pkg);
                result = 0;
            } else {
                int result2 = performDexOptPrimary(pkg, 11, dexoptFlags);
                result = result2;
            }
        } else {
            int result3 = performDexOptSecondary(pkg, 11, dexoptFlags);
            result = result3;
        }
        if (result == 1) {
            Computer newSnapshot = pm.snapshotComputer();
            FrameworkStatsLog.write(128, pkg, package_size_before, getPackageSize(newSnapshot, pkg), false);
        }
        return result;
    }

    private int reconcileSecondaryDexFiles() {
        for (String p : this.mInjector.getDexManager().getAllPackagesWithSecondaryDexFiles()) {
            if (isCancelling()) {
                return 1;
            }
            this.mInjector.getDexManager().reconcileSecondaryDexFiles(p);
        }
        return 0;
    }

    private int optimizePackage(String pkg, boolean isForPrimaryDex, boolean isPostBootUpdate) {
        int reason = isPostBootUpdate ? 2 : 9;
        int dexoptFlags = 4;
        if (!isPostBootUpdate) {
            dexoptFlags = 4 | UsbTerminalTypes.TERMINAL_IN_MIC;
        }
        if (isForPrimaryDex || PackageManagerService.PLATFORM_PACKAGE_NAME.equals(pkg)) {
            return performDexOptPrimary(pkg, reason, dexoptFlags);
        }
        return performDexOptSecondary(pkg, reason, dexoptFlags);
    }

    private int performDexOptPrimary(String pkg, int reason, int dexoptFlags) {
        final DexoptOptions dexoptOptions = new DexoptOptions(pkg, reason, dexoptFlags);
        return trackPerformDexOpt(pkg, true, new Supplier() { // from class: com.android.server.pm.BackgroundDexOptService$$ExternalSyntheticLambda0
            @Override // java.util.function.Supplier
            public final Object get() {
                return BackgroundDexOptService.this.m5380xf005f1a7(dexoptOptions);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$performDexOptPrimary$2$com-android-server-pm-BackgroundDexOptService  reason: not valid java name */
    public /* synthetic */ Integer m5380xf005f1a7(DexoptOptions dexoptOptions) {
        return Integer.valueOf(this.mDexOptHelper.performDexOptWithStatus(dexoptOptions));
    }

    private int performDexOptSecondary(String pkg, int reason, int dexoptFlags) {
        final DexoptOptions dexoptOptions = new DexoptOptions(pkg, reason, dexoptFlags | 8);
        return trackPerformDexOpt(pkg, false, new Supplier() { // from class: com.android.server.pm.BackgroundDexOptService$$ExternalSyntheticLambda4
            @Override // java.util.function.Supplier
            public final Object get() {
                return BackgroundDexOptService.this.m5381x44f1875a(dexoptOptions);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$performDexOptSecondary$3$com-android-server-pm-BackgroundDexOptService  reason: not valid java name */
    public /* synthetic */ Integer m5381x44f1875a(DexoptOptions dexoptOptions) {
        int i;
        if (this.mDexOptHelper.performDexOpt(dexoptOptions)) {
            i = 1;
        } else {
            i = -1;
        }
        return Integer.valueOf(i);
    }

    private int trackPerformDexOpt(String pkg, boolean isForPrimaryDex, Supplier<Integer> performDexOptWrapper) {
        synchronized (this.mLock) {
            ArraySet<String> failedPackageNames = isForPrimaryDex ? this.mFailedPackageNamesPrimary : this.mFailedPackageNamesSecondary;
            if (failedPackageNames.contains(pkg)) {
                return 0;
            }
            int result = performDexOptWrapper.get().intValue();
            if (result == -1) {
                synchronized (this.mLock) {
                    failedPackageNames.add(pkg);
                }
            } else if (result == 2) {
                synchronized (this.mLock) {
                    this.mLastCancelledPackages.add(pkg);
                }
            }
            return result;
        }
    }

    private int convertPackageDexOptimizerStatusToInternal(int pdoStatus) {
        switch (pdoStatus) {
            case -1:
                return 5;
            case 0:
            case 1:
                return 0;
            case 2:
                return 1;
            default:
                Slog.e(TAG, "Unkknown error code from PackageDexOptimizer:" + pdoStatus, new RuntimeException());
                return 5;
        }
    }

    private int abortIdleOptimizations(long lowStorageThreshold) {
        if (isCancelling()) {
            return 1;
        }
        int thermalStatus = this.mInjector.getCurrentThermalStatus();
        if (DEBUG) {
            Log.d(TAG, "Thermal throttling status during bgdexopt: " + thermalStatus);
        }
        if (thermalStatus >= this.mThermalStatusCutoff) {
            return 3;
        }
        if (this.mInjector.isBatteryLevelLow()) {
            return 4;
        }
        long usableSpace = this.mInjector.getDataDirUsableSpace();
        if (usableSpace < lowStorageThreshold) {
            Slog.w(TAG, "Aborting background dex opt job due to low storage: " + usableSpace);
            return 2;
        }
        return 0;
    }

    private boolean shouldDowngrade(long lowStorageThresholdForDowngrade) {
        if (this.mInjector.getDataDirUsableSpace() < lowStorageThresholdForDowngrade) {
            return true;
        }
        return false;
    }

    private boolean isCancelling() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mDexOptCancellingThread != null;
        }
        return z;
    }

    private void markPostBootUpdateCompleted(JobParameters params) {
        if (params.getJobId() != JOB_POST_BOOT_UPDATE) {
            return;
        }
        synchronized (this.mLock) {
            if (!this.mFinishedPostBootUpdate) {
                this.mFinishedPostBootUpdate = true;
            }
        }
        this.mInjector.getJobScheduler().cancel(JOB_POST_BOOT_UPDATE);
    }

    private void notifyPinService(ArraySet<String> updatedPackages) {
        PinnerService pinnerService = this.mInjector.getPinnerService();
        if (pinnerService != null) {
            Slog.i(TAG, "Pinning optimized code " + updatedPackages);
            pinnerService.update(updatedPackages, false);
        }
    }

    private void notifyPackagesUpdated(ArraySet<String> updatedPackages) {
        synchronized (this.mLock) {
            for (PackagesUpdatedListener listener : this.mPackagesUpdatedListeners) {
                listener.onPackagesUpdated(updatedPackages);
            }
        }
    }

    private void writeStatsLog(JobParameters params) {
        int status;
        long durationMs;
        long durationIncludingSleepMs;
        synchronized (this.mLock) {
            status = this.mLastExecutionStatus;
            durationMs = this.mLastExecutionDurationMs;
            durationIncludingSleepMs = this.mLastExecutionDurationIncludingSleepMs;
        }
        this.mStatsLogger.write(status, params.getStopReason(), durationMs, durationIncludingSleepMs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class Injector {
        private final Context mContext;
        private final File mDataDir = Environment.getDataDirectory();
        private final DexManager mDexManager;
        private final PackageManagerService mPackageManagerService;

        Injector(Context context, DexManager dexManager, PackageManagerService pm) {
            this.mContext = context;
            this.mDexManager = dexManager;
            this.mPackageManagerService = pm;
        }

        Context getContext() {
            return this.mContext;
        }

        PackageManagerService getPackageManagerService() {
            return this.mPackageManagerService;
        }

        DexOptHelper getDexOptHelper() {
            return new DexOptHelper(getPackageManagerService());
        }

        JobScheduler getJobScheduler() {
            return (JobScheduler) this.mContext.getSystemService(JobScheduler.class);
        }

        DexManager getDexManager() {
            return this.mDexManager;
        }

        PinnerService getPinnerService() {
            return (PinnerService) LocalServices.getService(PinnerService.class);
        }

        boolean isBackgroundDexOptDisabled() {
            return SystemProperties.getBoolean("pm.dexopt.disable_bg_dexopt", false);
        }

        boolean isBatteryLevelLow() {
            return ((BatteryManagerInternal) LocalServices.getService(BatteryManagerInternal.class)).getBatteryLevelLow();
        }

        long getDowngradeUnusedAppsThresholdInMillis() {
            String sysPropValue = SystemProperties.get("pm.dexopt.downgrade_after_inactive_days");
            if (sysPropValue == null || sysPropValue.isEmpty()) {
                Slog.w(BackgroundDexOptService.TAG, "SysProp pm.dexopt.downgrade_after_inactive_days not set");
                return JobStatus.NO_LATEST_RUNTIME;
            }
            return TimeUnit.DAYS.toMillis(Long.parseLong(sysPropValue));
        }

        boolean supportSecondaryDex() {
            return SystemProperties.getBoolean("dalvik.vm.dexopt.secondary", false);
        }

        long getDataDirUsableSpace() {
            return this.mDataDir.getUsableSpace();
        }

        long getDataDirStorageLowBytes() {
            return ((StorageManager) this.mContext.getSystemService(StorageManager.class)).getStorageLowBytes(this.mDataDir);
        }

        int getCurrentThermalStatus() {
            IThermalService thermalService = IThermalService.Stub.asInterface(ServiceManager.getService("thermalservice"));
            try {
                return thermalService.getCurrentThermalStatus();
            } catch (RemoteException e) {
                return 3;
            }
        }

        int getDexOptThermalCutoff() {
            return SystemProperties.getInt("dalvik.vm.dexopt.thermal-cutoff", 2);
        }

        Thread createAndStartThread(String name, Runnable target) {
            Thread thread = new Thread(target, name);
            Slog.i(BackgroundDexOptService.TAG, "Starting thread:" + name);
            thread.start();
            return thread;
        }
    }
}
