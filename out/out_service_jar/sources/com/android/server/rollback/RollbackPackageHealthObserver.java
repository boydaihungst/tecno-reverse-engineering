package com.android.server.rollback;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.VersionedPackage;
import android.content.rollback.PackageRollbackInfo;
import android.content.rollback.RollbackInfo;
import android.content.rollback.RollbackManager;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.Preconditions;
import com.android.server.PackageWatchdog;
import com.android.server.pm.ApexManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class RollbackPackageHealthObserver implements PackageWatchdog.PackageHealthObserver {
    private static final String NAME = "rollback-observer";
    private static final String TAG = "RollbackPackageHealthObserver";
    private final ApexManager mApexManager;
    private final Context mContext;
    private final Handler mHandler;
    private final File mLastStagedRollbackIdsFile;
    private final Set<Integer> mPendingStagedRollbackIds = new ArraySet();
    private boolean mTwoPhaseRollbackEnabled;
    private final File mTwoPhaseRollbackEnabledFile;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RollbackPackageHealthObserver(Context context) {
        this.mContext = context;
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        this.mHandler = new Handler(handlerThread.getLooper());
        File dataDir = new File(Environment.getDataDirectory(), NAME);
        dataDir.mkdirs();
        this.mLastStagedRollbackIdsFile = new File(dataDir, "last-staged-rollback-ids");
        File file = new File(dataDir, "two-phase-rollback-enabled");
        this.mTwoPhaseRollbackEnabledFile = file;
        PackageWatchdog.getInstance(context).registerHealthObserver(this);
        this.mApexManager = ApexManager.getInstance();
        if (SystemProperties.getBoolean("sys.boot_completed", false)) {
            this.mTwoPhaseRollbackEnabled = readBoolean(file);
            return;
        }
        this.mTwoPhaseRollbackEnabled = false;
        writeBoolean(file, false);
    }

    @Override // com.android.server.PackageWatchdog.PackageHealthObserver
    public int onHealthCheckFailed(VersionedPackage failedPackage, int failureReason, int mitigationCount) {
        if ((failureReason == 1 && !((RollbackManager) this.mContext.getSystemService(RollbackManager.class)).getAvailableRollbacks().isEmpty()) || getAvailableRollback(failedPackage) != null) {
            return 3;
        }
        return 0;
    }

    @Override // com.android.server.PackageWatchdog.PackageHealthObserver
    public boolean execute(final VersionedPackage failedPackage, final int rollbackReason, int mitigationCount) {
        if (rollbackReason == 1) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.rollback.RollbackPackageHealthObserver$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    RollbackPackageHealthObserver.this.m6380x8630ee8b();
                }
            });
            return true;
        }
        final RollbackInfo rollback = getAvailableRollback(failedPackage);
        if (rollback == null) {
            Slog.w(TAG, "Expected rollback but no valid rollback found for " + failedPackage);
            return false;
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.rollback.RollbackPackageHealthObserver$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                RollbackPackageHealthObserver.this.m6381x96e6bb4c(rollback, failedPackage, rollbackReason);
            }
        });
        return true;
    }

    @Override // com.android.server.PackageWatchdog.PackageHealthObserver
    public String getName() {
        return NAME;
    }

    private void assertInWorkerThread() {
        Preconditions.checkState(this.mHandler.getLooper().isCurrentThread());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startObservingHealth(List<String> packages, long durationMs) {
        PackageWatchdog.getInstance(this.mContext).startObservingHealth(this, packages, durationMs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyRollbackAvailable(final RollbackInfo rollback) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.rollback.RollbackPackageHealthObserver$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                RollbackPackageHealthObserver.this.m6382xeea1be14(rollback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyRollbackAvailable$2$com-android-server-rollback-RollbackPackageHealthObserver  reason: not valid java name */
    public /* synthetic */ void m6382xeea1be14(RollbackInfo rollback) {
        if (isRebootlessApex(rollback)) {
            this.mTwoPhaseRollbackEnabled = true;
            writeBoolean(this.mTwoPhaseRollbackEnabledFile, true);
        }
    }

    private static boolean isRebootlessApex(RollbackInfo rollback) {
        if (!rollback.isStaged()) {
            for (PackageRollbackInfo info : rollback.getPackages()) {
                if (info.isApex()) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onBootCompletedAsync() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.rollback.RollbackPackageHealthObserver$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                RollbackPackageHealthObserver.this.m6383x8554e435();
            }
        });
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: onBootCompleted */
    public void m6383x8554e435() {
        assertInWorkerThread();
        RollbackManager rollbackManager = (RollbackManager) this.mContext.getSystemService(RollbackManager.class);
        if (!rollbackManager.getAvailableRollbacks().isEmpty()) {
            PackageWatchdog.getInstance(this.mContext).scheduleCheckAndMitigateNativeCrashes();
        }
        SparseArray<String> rollbackIds = popLastStagedRollbackIds();
        for (int i = 0; i < rollbackIds.size(); i++) {
            WatchdogRollbackLogger.logRollbackStatusOnBoot(this.mContext, rollbackIds.keyAt(i), rollbackIds.valueAt(i), rollbackManager.getRecentlyCommittedRollbacks());
        }
    }

    private RollbackInfo getAvailableRollback(VersionedPackage failedPackage) {
        RollbackManager rollbackManager = (RollbackManager) this.mContext.getSystemService(RollbackManager.class);
        for (RollbackInfo rollback : rollbackManager.getAvailableRollbacks()) {
            for (PackageRollbackInfo packageRollback : rollback.getPackages()) {
                if (packageRollback.getVersionRolledBackFrom().equals(failedPackage)) {
                    return rollback;
                }
                if (packageRollback.isApkInApex() && packageRollback.getVersionRolledBackFrom().getPackageName().equals(failedPackage.getPackageName())) {
                    return rollback;
                }
            }
        }
        return null;
    }

    private boolean markStagedSessionHandled(int rollbackId) {
        assertInWorkerThread();
        return this.mPendingStagedRollbackIds.remove(Integer.valueOf(rollbackId));
    }

    private boolean isPendingStagedSessionsEmpty() {
        assertInWorkerThread();
        return this.mPendingStagedRollbackIds.isEmpty();
    }

    private static boolean readBoolean(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            boolean z = fis.read() == 1;
            fis.close();
            return z;
        } catch (IOException e) {
            return false;
        }
    }

    private static void writeBoolean(File file, boolean value) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(value ? 1 : 0);
            fos.flush();
            FileUtils.sync(fos);
            fos.close();
        } catch (IOException e) {
        }
    }

    private void saveStagedRollbackId(int stagedRollbackId, VersionedPackage logPackage) {
        assertInWorkerThread();
        writeStagedRollbackId(this.mLastStagedRollbackIdsFile, stagedRollbackId, logPackage);
    }

    static void writeStagedRollbackId(File file, int stagedRollbackId, VersionedPackage logPackage) {
        try {
            FileOutputStream fos = new FileOutputStream(file, true);
            PrintWriter pw = new PrintWriter(fos);
            String logPackageName = logPackage != null ? logPackage.getPackageName() : "";
            pw.append((CharSequence) String.valueOf(stagedRollbackId)).append((CharSequence) ",").append((CharSequence) logPackageName);
            pw.println();
            pw.flush();
            FileUtils.sync(fos);
            pw.close();
        } catch (IOException e) {
            Slog.e(TAG, "Failed to save last staged rollback id", e);
            file.delete();
        }
    }

    private SparseArray<String> popLastStagedRollbackIds() {
        assertInWorkerThread();
        try {
            return readStagedRollbackIds(this.mLastStagedRollbackIdsFile);
        } finally {
            this.mLastStagedRollbackIdsFile.delete();
        }
    }

    static SparseArray<String> readStagedRollbackIds(File file) {
        SparseArray<String> result = new SparseArray<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            while (true) {
                String line = reader.readLine();
                if (line != null) {
                    String[] values = line.trim().split(",");
                    String rollbackId = values[0];
                    String logPackageName = "";
                    if (values.length > 1) {
                        logPackageName = values[1];
                    }
                    result.put(Integer.parseInt(rollbackId), logPackageName);
                } else {
                    return result;
                }
            }
        } catch (Exception e) {
            return new SparseArray<>();
        }
    }

    private boolean isModule(String packageName) {
        String apexPackageName = this.mApexManager.getActiveApexPackageNameContainingPackage(packageName);
        if (apexPackageName != null) {
            packageName = apexPackageName;
        }
        PackageManager pm = this.mContext.getPackageManager();
        try {
            return pm.getModuleInfo(packageName, 0) != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: rollbackPackage */
    public void m6381x96e6bb4c(final RollbackInfo rollback, VersionedPackage failedPackage, int rollbackReason) {
        String failedPackageToLog;
        VersionedPackage logPackageTemp;
        assertInWorkerThread();
        RollbackManager rollbackManager = (RollbackManager) this.mContext.getSystemService(RollbackManager.class);
        final int reasonToLog = WatchdogRollbackLogger.mapFailureReasonToMetric(rollbackReason);
        if (rollbackReason == 1) {
            failedPackageToLog = SystemProperties.get("sys.init.updatable_crashing_process_name", "");
        } else {
            String failedPackageToLog2 = failedPackage.getPackageName();
            failedPackageToLog = failedPackageToLog2;
        }
        if (!isModule(failedPackage.getPackageName())) {
            logPackageTemp = null;
        } else {
            VersionedPackage logPackageTemp2 = WatchdogRollbackLogger.getLogPackage(this.mContext, failedPackage);
            logPackageTemp = logPackageTemp2;
        }
        final VersionedPackage logPackage = logPackageTemp;
        WatchdogRollbackLogger.logEvent(logPackage, 1, reasonToLog, failedPackageToLog);
        final String str = failedPackageToLog;
        final Consumer<Intent> onResult = new Consumer() { // from class: com.android.server.rollback.RollbackPackageHealthObserver$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RollbackPackageHealthObserver.this.m6384x56c7a25c(rollback, logPackage, reasonToLog, str, (Intent) obj);
            }
        };
        LocalIntentReceiver rollbackReceiver = new LocalIntentReceiver(new Consumer() { // from class: com.android.server.rollback.RollbackPackageHealthObserver$$ExternalSyntheticLambda2
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RollbackPackageHealthObserver.this.m6385x78333bde(onResult, (Intent) obj);
            }
        });
        rollbackManager.commitRollback(rollback.getRollbackId(), Collections.singletonList(failedPackage), rollbackReceiver.getIntentSender());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$rollbackPackage$4$com-android-server-rollback-RollbackPackageHealthObserver  reason: not valid java name */
    public /* synthetic */ void m6384x56c7a25c(RollbackInfo rollback, VersionedPackage logPackage, int reasonToLog, String failedPackageToLog, Intent result) {
        assertInWorkerThread();
        int status = result.getIntExtra("android.content.rollback.extra.STATUS", 1);
        if (status == 0) {
            if (rollback.isStaged()) {
                int rollbackId = rollback.getRollbackId();
                saveStagedRollbackId(rollbackId, logPackage);
                WatchdogRollbackLogger.logEvent(logPackage, 4, reasonToLog, failedPackageToLog);
            } else {
                WatchdogRollbackLogger.logEvent(logPackage, 2, reasonToLog, failedPackageToLog);
            }
        } else {
            WatchdogRollbackLogger.logEvent(logPackage, 3, reasonToLog, failedPackageToLog);
        }
        if (rollback.isStaged()) {
            markStagedSessionHandled(rollback.getRollbackId());
            if (isPendingStagedSessionsEmpty()) {
                ((PowerManager) this.mContext.getSystemService(PowerManager.class)).reboot("Rollback staged install");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$rollbackPackage$6$com-android-server-rollback-RollbackPackageHealthObserver  reason: not valid java name */
    public /* synthetic */ void m6385x78333bde(final Consumer onResult, final Intent result) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.rollback.RollbackPackageHealthObserver$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                onResult.accept(result);
            }
        });
    }

    private boolean useTwoPhaseRollback(List<RollbackInfo> rollbacks) {
        assertInWorkerThread();
        if (this.mTwoPhaseRollbackEnabled) {
            Slog.i(TAG, "Rolling back all rebootless APEX rollbacks");
            boolean found = false;
            for (RollbackInfo rollback : rollbacks) {
                if (isRebootlessApex(rollback)) {
                    VersionedPackage sample = ((PackageRollbackInfo) rollback.getPackages().get(0)).getVersionRolledBackFrom();
                    m6381x96e6bb4c(rollback, sample, 1);
                    found = true;
                }
            }
            return found;
        }
        return false;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: rollbackAll */
    public void m6380x8630ee8b() {
        assertInWorkerThread();
        RollbackManager rollbackManager = (RollbackManager) this.mContext.getSystemService(RollbackManager.class);
        List<RollbackInfo> rollbacks = rollbackManager.getAvailableRollbacks();
        if (useTwoPhaseRollback(rollbacks)) {
            return;
        }
        Slog.i(TAG, "Rolling back all available rollbacks");
        for (RollbackInfo rollback : rollbacks) {
            if (rollback.isStaged()) {
                this.mPendingStagedRollbackIds.add(Integer.valueOf(rollback.getRollbackId()));
            }
        }
        for (RollbackInfo rollback2 : rollbacks) {
            VersionedPackage sample = ((PackageRollbackInfo) rollback2.getPackages().get(0)).getVersionRolledBackFrom();
            m6381x96e6bb4c(rollback2, sample, 1);
        }
    }
}
