package com.android.server.backup.fullbackup;

import android.app.IBackupAgent;
import android.app.backup.BackupProgress;
import android.app.backup.IBackupCallback;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import android.app.backup.IFullBackupRestoreObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import com.android.server.EventLogTags;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.FullBackupJob;
import com.android.server.backup.OperationStorage;
import com.android.server.backup.TransportManager;
import com.android.server.backup.UserBackupManagerService;
import com.android.server.backup.fullbackup.PerformFullTransportBackupTask;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.remote.RemoteCall;
import com.android.server.backup.remote.RemoteCallable;
import com.android.server.backup.transport.BackupTransportClient;
import com.android.server.backup.transport.TransportConnection;
import com.android.server.backup.transport.TransportNotAvailableException;
import com.android.server.backup.utils.BackupEligibilityRules;
import com.android.server.backup.utils.BackupManagerMonitorUtils;
import com.android.server.backup.utils.BackupObserverUtils;
import com.android.server.job.JobSchedulerShellCommand;
import com.google.android.collect.Sets;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
/* loaded from: classes.dex */
public class PerformFullTransportBackupTask extends FullBackupTask implements BackupRestoreTask {
    private static final String TAG = "PFTBT";
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    private final BackupEligibilityRules mBackupEligibilityRules;
    IBackupObserver mBackupObserver;
    SinglePackageBackupRunner mBackupRunner;
    private final int mBackupRunnerOpToken;
    private volatile boolean mCancelAll;
    private final Object mCancelLock;
    private final int mCurrentOpToken;
    PackageInfo mCurrentPackage;
    private volatile boolean mIsDoingBackup;
    FullBackupJob mJob;
    CountDownLatch mLatch;
    private final OnTaskFinishedListener mListener;
    private IBackupManagerMonitor mMonitor;
    OperationStorage mOperationStorage;
    List<PackageInfo> mPackages;
    private final TransportConnection mTransportConnection;
    boolean mUpdateSchedule;
    private UserBackupManagerService mUserBackupManagerService;
    private final int mUserId;
    boolean mUserInitiated;

    public static PerformFullTransportBackupTask newWithCurrentTransport(UserBackupManagerService backupManagerService, OperationStorage operationStorage, IFullBackupRestoreObserver observer, String[] whichPackages, boolean updateSchedule, FullBackupJob runningJob, CountDownLatch latch, IBackupObserver backupObserver, IBackupManagerMonitor monitor, boolean userInitiated, String caller, BackupEligibilityRules backupEligibilityRules) {
        final TransportManager transportManager = backupManagerService.getTransportManager();
        final TransportConnection transportConnection = transportManager.getCurrentTransportClient(caller);
        if (transportConnection == null) {
            throw new IllegalStateException("No TransportConnection available");
        }
        OnTaskFinishedListener listener = new OnTaskFinishedListener() { // from class: com.android.server.backup.fullbackup.PerformFullTransportBackupTask$$ExternalSyntheticLambda0
            @Override // com.android.server.backup.internal.OnTaskFinishedListener
            public final void onFinished(String str) {
                TransportManager.this.disposeOfTransportClient(transportConnection, str);
            }
        };
        return new PerformFullTransportBackupTask(backupManagerService, operationStorage, transportConnection, observer, whichPackages, updateSchedule, runningJob, latch, backupObserver, monitor, listener, userInitiated, backupEligibilityRules);
    }

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public PerformFullTransportBackupTask(UserBackupManagerService backupManagerService, OperationStorage operationStorage, TransportConnection transportConnection, IFullBackupRestoreObserver observer, String[] whichPackages, boolean updateSchedule, FullBackupJob runningJob, CountDownLatch latch, IBackupObserver backupObserver, IBackupManagerMonitor monitor, OnTaskFinishedListener listener, boolean userInitiated, BackupEligibilityRules backupEligibilityRules) {
        super(observer);
        String[] strArr = whichPackages;
        this.mCancelLock = new Object();
        this.mUserBackupManagerService = backupManagerService;
        this.mOperationStorage = operationStorage;
        this.mTransportConnection = transportConnection;
        this.mUpdateSchedule = updateSchedule;
        this.mLatch = latch;
        this.mJob = runningJob;
        this.mPackages = new ArrayList(strArr.length);
        this.mBackupObserver = backupObserver;
        this.mMonitor = monitor;
        this.mListener = listener != null ? listener : OnTaskFinishedListener.NOP;
        this.mUserInitiated = userInitiated;
        this.mCurrentOpToken = backupManagerService.generateRandomIntegerToken();
        this.mBackupRunnerOpToken = backupManagerService.generateRandomIntegerToken();
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Objects.requireNonNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
        this.mUserId = backupManagerService.getUserId();
        this.mBackupEligibilityRules = backupEligibilityRules;
        if (backupManagerService.isBackupOperationInProgress()) {
            Slog.d(TAG, "Skipping full backup. A backup is already in progress.");
            this.mCancelAll = true;
            return;
        }
        int length = strArr.length;
        int i = 0;
        while (i < length) {
            int i2 = length;
            String pkg = strArr[i];
            try {
                PackageManager pm = backupManagerService.getPackageManager();
                PackageInfo info = pm.getPackageInfoAsUser(pkg, 134217728, this.mUserId);
                this.mCurrentPackage = info;
                if (!this.mBackupEligibilityRules.appIsEligibleForBackup(info.applicationInfo)) {
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 9, this.mCurrentPackage, 3, null);
                    BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, pkg, -2001);
                } else if (!this.mBackupEligibilityRules.appGetsFullBackup(info)) {
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 10, this.mCurrentPackage, 3, null);
                    BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, pkg, -2001);
                } else if (this.mBackupEligibilityRules.appIsStopped(info.applicationInfo)) {
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 11, this.mCurrentPackage, 3, null);
                    BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, pkg, -2001);
                } else {
                    this.mPackages.add(info);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Slog.i(TAG, "Requested package " + pkg + " not found; ignoring");
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 12, this.mCurrentPackage, 3, null);
            }
            i++;
            strArr = whichPackages;
            length = i2;
        }
        this.mPackages = backupManagerService.filterUserFacingPackages(this.mPackages);
        Set<String> packageNames = Sets.newHashSet();
        for (PackageInfo pkgInfo : this.mPackages) {
            packageNames.add(pkgInfo.packageName);
        }
        Slog.d(TAG, "backupmanager pftbt token=" + Integer.toHexString(this.mCurrentOpToken));
        this.mOperationStorage.registerOperationForPackages(this.mCurrentOpToken, 0, packageNames, this, 2);
    }

    public void unregisterTask() {
        this.mOperationStorage.removeOperation(this.mCurrentOpToken);
    }

    @Override // com.android.server.backup.BackupRestoreTask
    public void execute() {
    }

    @Override // com.android.server.backup.BackupRestoreTask
    public void handleCancel(boolean cancelAll) {
        synchronized (this.mCancelLock) {
            if (!cancelAll) {
                Slog.wtf(TAG, "Expected cancelAll to be true.");
            }
            if (this.mCancelAll) {
                Slog.d(TAG, "Ignoring duplicate cancel call.");
                return;
            }
            this.mCancelAll = true;
            if (this.mIsDoingBackup) {
                this.mUserBackupManagerService.handleCancel(this.mBackupRunnerOpToken, cancelAll);
                try {
                    BackupTransportClient transport = this.mTransportConnection.getConnectedTransport("PFTBT.handleCancel()");
                    transport.cancelFullBackup();
                } catch (RemoteException | TransportNotAvailableException e) {
                    Slog.w(TAG, "Error calling cancelFullBackup() on transport: " + e);
                }
            }
        }
    }

    @Override // com.android.server.backup.BackupRestoreTask
    public void operationComplete(long result) {
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[MOVE, CONST_STR]}, finally: {[MOVE] complete} */
    /* JADX DEBUG: Another duplicated slice has different insns count: {[MOVE, GOTO]}, finally: {[MOVE] complete} */
    /* JADX DEBUG: Another duplicated slice has different insns count: {[MOVE, IGET]}, finally: {[MOVE] complete} */
    /* JADX DEBUG: Another duplicated slice has different insns count: {[MOVE, INVOKE]}, finally: {[MOVE] complete} */
    /* JADX DEBUG: Another duplicated slice has different insns count: {[MOVE, MOVE]}, finally: {[MOVE] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [635=14, 648=23, 649=6, 653=6, 655=6, 657=6, 658=6, 660=6, 662=6, 663=6, 666=6, 667=6, 668=12, 670=6, 672=6, 676=7, 420=5, 677=6, 680=6, 681=6] */
    /* JADX DEBUG: Incorrect finally slice size: {[MOVE, MOVE] complete}, expected: {[MOVE] complete} */
    /* JADX WARN: Code restructure failed: missing block: B:189:0x04fc, code lost:
        com.android.server.backup.utils.BackupObserverUtils.sendBackupOnPackageResult(r37.mBackupObserver, r2, -1000);
        android.util.Slog.w(com.android.server.backup.fullbackup.PerformFullTransportBackupTask.TAG, "Transport failed; aborting backup: " + r1);
        android.util.EventLog.writeEvent((int) com.android.server.EventLogTags.FULL_BACKUP_TRANSPORT_FAILURE, new java.lang.Object[0]);
     */
    /* JADX WARN: Code restructure failed: missing block: B:190:0x0523, code lost:
        r11 = -1000;
     */
    /* JADX WARN: Code restructure failed: missing block: B:191:0x0525, code lost:
        r37.mUserBackupManagerService.tearDownAgentAndKill(r4.applicationInfo);
     */
    /* JADX WARN: Code restructure failed: missing block: B:193:0x052e, code lost:
        if (r37.mCancelAll == false) goto L245;
     */
    /* JADX WARN: Code restructure failed: missing block: B:194:0x0530, code lost:
        r3 = -2003;
     */
    /* JADX WARN: Code restructure failed: missing block: B:195:0x0534, code lost:
        r3 = -1000;
     */
    /* JADX WARN: Code restructure failed: missing block: B:196:0x0535, code lost:
        android.util.Slog.i(com.android.server.backup.fullbackup.PerformFullTransportBackupTask.TAG, "Full backup completed with status: " + r3);
        com.android.server.backup.utils.BackupObserverUtils.sendBackupFinished(r37.mBackupObserver, r3);
        cleanUpPipes(r5);
        cleanUpPipes(r32);
        unregisterTask();
        r9 = r37.mJob;
     */
    /* JADX WARN: Code restructure failed: missing block: B:197:0x055f, code lost:
        if (r9 == null) goto L230;
     */
    /* JADX WARN: Code restructure failed: missing block: B:198:0x0561, code lost:
        r9.finishBackupPass(r37.mUserId);
     */
    /* JADX WARN: Code restructure failed: missing block: B:199:0x0566, code lost:
        r9 = r37.mUserBackupManagerService.getQueueLock();
     */
    /* JADX WARN: Code restructure failed: missing block: B:200:0x056c, code lost:
        monitor-enter(r9);
     */
    /* JADX WARN: Code restructure failed: missing block: B:201:0x056d, code lost:
        r37.mUserBackupManagerService.setRunningFullBackupTask(null);
     */
    /* JADX WARN: Code restructure failed: missing block: B:202:0x0573, code lost:
        monitor-exit(r9);
     */
    /* JADX WARN: Code restructure failed: missing block: B:203:0x0574, code lost:
        r37.mListener.onFinished("PFTBT.run()");
        r37.mLatch.countDown();
     */
    /* JADX WARN: Code restructure failed: missing block: B:204:0x0582, code lost:
        if (r37.mUpdateSchedule == false) goto L238;
     */
    /* JADX WARN: Code restructure failed: missing block: B:205:0x0584, code lost:
        r37.mUserBackupManagerService.scheduleNextFullBackupJob(r6);
     */
    /* JADX WARN: Code restructure failed: missing block: B:206:0x0589, code lost:
        android.util.Slog.i(com.android.server.backup.fullbackup.PerformFullTransportBackupTask.TAG, "Full data backup pass finished.");
        r37.mUserBackupManagerService.getWakelock().release();
     */
    /* JADX WARN: Code restructure failed: missing block: B:207:0x0599, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:212:0x059e, code lost:
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:213:0x059f, code lost:
        r2 = r5;
        r3 = r6;
        r1 = r32;
        r5 = r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:214:0x05a7, code lost:
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:215:0x05a8, code lost:
        r2 = r5;
        r3 = r6;
        r1 = r32;
        r5 = r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:257:0x06a4, code lost:
        if (r37.mCancelAll == false) goto L386;
     */
    /* JADX WARN: Code restructure failed: missing block: B:258:0x06a6, code lost:
        r11 = -2003;
     */
    /* JADX WARN: Code restructure failed: missing block: B:259:0x06a9, code lost:
        r11 = r28;
     */
    /* JADX WARN: Code restructure failed: missing block: B:260:0x06ab, code lost:
        android.util.Slog.i(com.android.server.backup.fullbackup.PerformFullTransportBackupTask.TAG, "Full backup completed with status: " + r11);
        com.android.server.backup.utils.BackupObserverUtils.sendBackupFinished(r37.mBackupObserver, r11);
        cleanUpPipes(r13);
        cleanUpPipes(r1);
        unregisterTask();
        r2 = r37.mJob;
     */
    /* JADX WARN: Code restructure failed: missing block: B:261:0x06d3, code lost:
        if (r2 == null) goto L365;
     */
    /* JADX WARN: Code restructure failed: missing block: B:262:0x06d5, code lost:
        r2.finishBackupPass(r37.mUserId);
     */
    /* JADX WARN: Code restructure failed: missing block: B:263:0x06da, code lost:
        r5 = r37.mUserBackupManagerService.getQueueLock();
     */
    /* JADX WARN: Code restructure failed: missing block: B:264:0x06e0, code lost:
        monitor-enter(r5);
     */
    /* JADX WARN: Code restructure failed: missing block: B:265:0x06e1, code lost:
        r37.mUserBackupManagerService.setRunningFullBackupTask(null);
     */
    /* JADX WARN: Code restructure failed: missing block: B:266:0x06e7, code lost:
        monitor-exit(r5);
     */
    /* JADX WARN: Code restructure failed: missing block: B:267:0x06e8, code lost:
        r37.mListener.onFinished("PFTBT.run()");
        r37.mLatch.countDown();
     */
    /* JADX WARN: Code restructure failed: missing block: B:268:0x06f6, code lost:
        if (r37.mUpdateSchedule == false) goto L375;
     */
    /* JADX WARN: Code restructure failed: missing block: B:269:0x06f8, code lost:
        r37.mUserBackupManagerService.scheduleNextFullBackupJob(r24);
     */
    /* JADX WARN: Code restructure failed: missing block: B:271:0x0702, code lost:
        android.util.Slog.i(com.android.server.backup.fullbackup.PerformFullTransportBackupTask.TAG, "Full data backup pass finished.");
        r37.mUserBackupManagerService.getWakelock().release();
     */
    /* JADX WARN: Code restructure failed: missing block: B:272:0x0714, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:275:0x0719, code lost:
        throw r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:276:0x071a, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:441:?, code lost:
        return;
     */
    /* JADX WARN: Finally extract failed */
    /* JADX WARN: Removed duplicated region for block: B:337:0x088a  */
    /* JADX WARN: Removed duplicated region for block: B:340:0x08b6  */
    /* JADX WARN: Removed duplicated region for block: B:382:0x08c2 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    @Override // java.lang.Runnable
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void run() {
        Throwable th;
        Exception e;
        FullBackupJob fullBackupJob;
        int backupRunStatus;
        long backoff;
        int backupRunStatus2;
        ParcelFileDescriptor[] transportPipes;
        long backoff2;
        Object obj;
        int backupRunStatus3;
        ParcelFileDescriptor[] transportPipes2;
        Throwable th2;
        long backoff3;
        PackageInfo currentPackage;
        byte[] buffer;
        int N;
        String packageName;
        char c;
        ParcelFileDescriptor[] enginePipes;
        ParcelFileDescriptor[] transportPipes3;
        String packageName2;
        int backupPackageStatus;
        PackageInfo currentPackage2;
        ParcelFileDescriptor[] enginePipes2;
        int nRead;
        Throwable th3;
        FileInputStream in;
        String packageName3;
        int backupPackageStatus2;
        int backupPackageStatus3;
        Throwable th4;
        ParcelFileDescriptor[] enginePipes3 = null;
        ParcelFileDescriptor[] transportPipes4 = null;
        long backoff4 = 0;
        int backupRunStatus4 = 0;
        SinglePackageBackupRunner singlePackageBackupRunner = null;
        try {
        } catch (Exception e2) {
            e = e2;
        } catch (Throwable th5) {
            th = th5;
        }
        if (!this.mUserBackupManagerService.isEnabled()) {
            backupRunStatus = 0;
        } else if (this.mUserBackupManagerService.isSetupComplete()) {
            BackupTransportClient transport = this.mTransportConnection.connect("PFTBT.run()");
            if (transport != null) {
                int N2 = this.mPackages.size();
                byte[] buffer2 = new byte[8192];
                int i = 0;
                long backoff5 = 0;
                while (true) {
                    if (i >= N2) {
                        backoff = backoff5;
                        backupRunStatus2 = backupRunStatus4;
                        transportPipes = transportPipes4;
                        break;
                    }
                    try {
                        this.mBackupRunner = singlePackageBackupRunner;
                        PackageInfo currentPackage3 = this.mPackages.get(i);
                        String packageName4 = currentPackage3.packageName;
                        Slog.i(TAG, "Initiating full-data transport backup of " + packageName4 + " token: " + this.mCurrentOpToken);
                        EventLog.writeEvent((int) EventLogTags.FULL_BACKUP_PACKAGE, packageName4);
                        transportPipes = ParcelFileDescriptor.createPipe();
                        try {
                            int flags = this.mUserInitiated ? 1 : 0;
                            Object obj2 = this.mCancelLock;
                            synchronized (obj2) {
                                try {
                                    if (this.mCancelAll) {
                                        try {
                                            break;
                                        } catch (Throwable th6) {
                                            backoff2 = backoff5;
                                            obj = obj2;
                                            backupRunStatus3 = backupRunStatus4;
                                            transportPipes2 = transportPipes;
                                            th2 = th6;
                                        }
                                    } else {
                                        int i2 = i;
                                        try {
                                            int backupPackageStatus4 = transport.performFullBackup(currentPackage3, transportPipes[0], flags);
                                            if (backupPackageStatus4 == 0) {
                                                try {
                                                    backoff2 = backoff5;
                                                    backoff3 = transport.getBackupQuota(currentPackage3.packageName, true);
                                                    try {
                                                        ParcelFileDescriptor[] enginePipes4 = ParcelFileDescriptor.createPipe();
                                                        try {
                                                            currentPackage = currentPackage3;
                                                            backupRunStatus3 = backupRunStatus4;
                                                            buffer = buffer2;
                                                            N = N2;
                                                            packageName = packageName4;
                                                            c = 1;
                                                            obj = obj2;
                                                            try {
                                                                this.mBackupRunner = new SinglePackageBackupRunner(enginePipes4[1], currentPackage, this.mTransportConnection, backoff3, this.mBackupRunnerOpToken, transport.getTransportFlags());
                                                                enginePipes4[1].close();
                                                                enginePipes4[1] = null;
                                                                this.mIsDoingBackup = true;
                                                                enginePipes3 = enginePipes4;
                                                            } catch (Throwable th7) {
                                                                th2 = th7;
                                                                transportPipes2 = transportPipes;
                                                                enginePipes3 = enginePipes4;
                                                            }
                                                        } catch (Throwable th8) {
                                                            obj = obj2;
                                                            backupRunStatus3 = backupRunStatus4;
                                                            transportPipes2 = transportPipes;
                                                            enginePipes3 = enginePipes4;
                                                            th2 = th8;
                                                        }
                                                    } catch (Throwable th9) {
                                                        obj = obj2;
                                                        backupRunStatus3 = backupRunStatus4;
                                                        transportPipes2 = transportPipes;
                                                        th2 = th9;
                                                    }
                                                } catch (Throwable th10) {
                                                    backoff2 = backoff5;
                                                    obj = obj2;
                                                    backupRunStatus3 = backupRunStatus4;
                                                    transportPipes2 = transportPipes;
                                                    th2 = th10;
                                                }
                                            } else {
                                                currentPackage = currentPackage3;
                                                backoff2 = backoff5;
                                                N = N2;
                                                obj = obj2;
                                                backupRunStatus3 = backupRunStatus4;
                                                packageName = packageName4;
                                                c = 1;
                                                buffer = buffer2;
                                                backoff3 = Long.MAX_VALUE;
                                            }
                                            try {
                                                if (backupPackageStatus4 == 0) {
                                                    try {
                                                        transportPipes[0].close();
                                                        transportPipes[0] = null;
                                                        new Thread(this.mBackupRunner, "package-backup-bridge").start();
                                                        FileInputStream in2 = new FileInputStream(enginePipes3[0].getFileDescriptor());
                                                        FileOutputStream out = new FileOutputStream(transportPipes[c].getFileDescriptor());
                                                        long preflightResult = this.mBackupRunner.getPreflightResultBlocking();
                                                        transportPipes3 = transportPipes;
                                                        if (preflightResult < 0) {
                                                            try {
                                                                enginePipes = enginePipes3;
                                                                try {
                                                                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 16, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_PREFLIGHT_ERROR", preflightResult));
                                                                    nRead = (int) preflightResult;
                                                                    packageName2 = packageName;
                                                                } catch (Exception e3) {
                                                                    transportPipes4 = transportPipes3;
                                                                    backoff4 = backoff2;
                                                                    enginePipes3 = enginePipes;
                                                                    e = e3;
                                                                } catch (Throwable th11) {
                                                                    transportPipes4 = transportPipes3;
                                                                    backoff4 = backoff2;
                                                                    backupRunStatus4 = backupRunStatus3;
                                                                    enginePipes3 = enginePipes;
                                                                    th = th11;
                                                                    if (this.mCancelAll) {
                                                                    }
                                                                    Slog.i(TAG, "Full backup completed with status: " + backupRunStatus4);
                                                                    BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus4);
                                                                    cleanUpPipes(transportPipes4);
                                                                    cleanUpPipes(enginePipes3);
                                                                    unregisterTask();
                                                                    fullBackupJob = this.mJob;
                                                                    if (fullBackupJob != null) {
                                                                    }
                                                                    synchronized (this.mUserBackupManagerService.getQueueLock()) {
                                                                    }
                                                                }
                                                            } catch (Exception e4) {
                                                                transportPipes4 = transportPipes3;
                                                                backoff4 = backoff2;
                                                                e = e4;
                                                            } catch (Throwable th12) {
                                                                transportPipes4 = transportPipes3;
                                                                backoff4 = backoff2;
                                                                backupRunStatus4 = backupRunStatus3;
                                                                th = th12;
                                                            }
                                                        } else {
                                                            enginePipes = enginePipes3;
                                                            long totalRead = 0;
                                                            while (true) {
                                                                int nRead2 = in2.read(buffer);
                                                                if (nRead2 > 0) {
                                                                    out.write(buffer, 0, nRead2);
                                                                    synchronized (this.mCancelLock) {
                                                                        try {
                                                                            if (!this.mCancelAll) {
                                                                                try {
                                                                                    backupPackageStatus4 = transport.sendBackupData(nRead2);
                                                                                } catch (Throwable th13) {
                                                                                    th4 = th13;
                                                                                    while (true) {
                                                                                        try {
                                                                                            break;
                                                                                        } catch (Throwable th14) {
                                                                                            th4 = th14;
                                                                                        }
                                                                                    }
                                                                                    throw th4;
                                                                                }
                                                                            }
                                                                            in = in2;
                                                                            totalRead += nRead2;
                                                                            IBackupObserver iBackupObserver = this.mBackupObserver;
                                                                            if (iBackupObserver == null || preflightResult <= 0) {
                                                                                packageName3 = packageName;
                                                                            } else {
                                                                                packageName3 = packageName;
                                                                                BackupObserverUtils.sendBackupOnUpdate(iBackupObserver, packageName3, new BackupProgress(preflightResult, totalRead));
                                                                            }
                                                                            backupPackageStatus2 = backupPackageStatus4;
                                                                        } catch (Throwable th15) {
                                                                            th4 = th15;
                                                                        }
                                                                    }
                                                                } else {
                                                                    in = in2;
                                                                    packageName3 = packageName;
                                                                    backupPackageStatus2 = backupPackageStatus4;
                                                                }
                                                                if (nRead2 <= 0 || backupPackageStatus2 != 0) {
                                                                    break;
                                                                }
                                                                backupPackageStatus4 = backupPackageStatus2;
                                                                packageName = packageName3;
                                                                in2 = in;
                                                            }
                                                            if (backupPackageStatus2 == -1005) {
                                                                backupPackageStatus3 = backupPackageStatus2;
                                                                Slog.w(TAG, "Package hit quota limit in-flight " + packageName3 + ": " + totalRead + " of " + backoff3);
                                                                packageName2 = packageName3;
                                                                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 18, this.mCurrentPackage, 1, null);
                                                                this.mBackupRunner.sendQuotaExceeded(totalRead, backoff3);
                                                            } else {
                                                                backupPackageStatus3 = backupPackageStatus2;
                                                                packageName2 = packageName3;
                                                            }
                                                            nRead = backupPackageStatus3;
                                                        }
                                                        int backupRunnerResult = this.mBackupRunner.getBackupResultBlocking();
                                                        synchronized (this.mCancelLock) {
                                                            try {
                                                                this.mIsDoingBackup = false;
                                                                if (!this.mCancelAll) {
                                                                    if (backupRunnerResult == 0) {
                                                                        try {
                                                                            int finishResult = transport.finishBackup();
                                                                            if (nRead == 0) {
                                                                                nRead = finishResult;
                                                                            }
                                                                        } catch (Throwable th16) {
                                                                            th = th16;
                                                                            while (true) {
                                                                                try {
                                                                                    break;
                                                                                } catch (Throwable th17) {
                                                                                    th = th17;
                                                                                }
                                                                            }
                                                                            throw th3;
                                                                        }
                                                                    } else {
                                                                        transport.cancelFullBackup();
                                                                    }
                                                                }
                                                                if (nRead == 0 && backupRunnerResult != 0) {
                                                                    nRead = backupRunnerResult;
                                                                }
                                                                if (nRead != 0) {
                                                                    Slog.w(TAG, "Error " + nRead + " backing up " + packageName2);
                                                                }
                                                                backoff4 = transport.requestFullBackupTime();
                                                                try {
                                                                    int backupPackageStatus5 = nRead;
                                                                    Slog.i(TAG, "Transport suggested backoff=" + backoff4);
                                                                    backoff5 = backoff4;
                                                                    backupPackageStatus = backupPackageStatus5;
                                                                } catch (Exception e5) {
                                                                    transportPipes4 = transportPipes3;
                                                                    enginePipes3 = enginePipes;
                                                                    e = e5;
                                                                } catch (Throwable th18) {
                                                                    transportPipes4 = transportPipes3;
                                                                    backupRunStatus4 = backupRunStatus3;
                                                                    enginePipes3 = enginePipes;
                                                                    th = th18;
                                                                    if (this.mCancelAll) {
                                                                    }
                                                                    Slog.i(TAG, "Full backup completed with status: " + backupRunStatus4);
                                                                    BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus4);
                                                                    cleanUpPipes(transportPipes4);
                                                                    cleanUpPipes(enginePipes3);
                                                                    unregisterTask();
                                                                    fullBackupJob = this.mJob;
                                                                    if (fullBackupJob != null) {
                                                                    }
                                                                    synchronized (this.mUserBackupManagerService.getQueueLock()) {
                                                                    }
                                                                }
                                                            } catch (Throwable th19) {
                                                                th = th19;
                                                            }
                                                        }
                                                    } catch (Exception e6) {
                                                        transportPipes4 = transportPipes;
                                                        backoff4 = backoff2;
                                                        e = e6;
                                                    } catch (Throwable th20) {
                                                        transportPipes4 = transportPipes;
                                                        backoff4 = backoff2;
                                                        backupRunStatus4 = backupRunStatus3;
                                                        th = th20;
                                                    }
                                                } else {
                                                    enginePipes = enginePipes3;
                                                    transportPipes3 = transportPipes;
                                                    packageName2 = packageName;
                                                    backupPackageStatus = backupPackageStatus4;
                                                    backoff5 = backoff2;
                                                }
                                                try {
                                                    if (this.mUpdateSchedule) {
                                                        try {
                                                            this.mUserBackupManagerService.enqueueFullBackup(packageName2, System.currentTimeMillis());
                                                        } catch (Exception e7) {
                                                            transportPipes4 = transportPipes3;
                                                            backoff4 = backoff5;
                                                            enginePipes3 = enginePipes;
                                                            e = e7;
                                                        } catch (Throwable th21) {
                                                            transportPipes4 = transportPipes3;
                                                            backoff4 = backoff5;
                                                            backupRunStatus4 = backupRunStatus3;
                                                            enginePipes3 = enginePipes;
                                                            th = th21;
                                                            if (this.mCancelAll) {
                                                            }
                                                            Slog.i(TAG, "Full backup completed with status: " + backupRunStatus4);
                                                            BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus4);
                                                            cleanUpPipes(transportPipes4);
                                                            cleanUpPipes(enginePipes3);
                                                            unregisterTask();
                                                            fullBackupJob = this.mJob;
                                                            if (fullBackupJob != null) {
                                                            }
                                                            synchronized (this.mUserBackupManagerService.getQueueLock()) {
                                                            }
                                                        }
                                                    }
                                                    if (backupPackageStatus == -1002) {
                                                        BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, packageName2, JobSchedulerShellCommand.CMD_ERR_CONSTRAINTS);
                                                        Slog.i(TAG, "Transport rejected backup of " + packageName2 + ", skipping");
                                                        EventLog.writeEvent((int) EventLogTags.FULL_BACKUP_AGENT_FAILURE, packageName2, "transport rejected");
                                                        if (this.mBackupRunner != null) {
                                                            currentPackage2 = currentPackage;
                                                            this.mUserBackupManagerService.tearDownAgentAndKill(currentPackage2.applicationInfo);
                                                            enginePipes2 = enginePipes;
                                                        } else {
                                                            currentPackage2 = currentPackage;
                                                            enginePipes2 = enginePipes;
                                                        }
                                                    } else {
                                                        currentPackage2 = currentPackage;
                                                        if (backupPackageStatus == -1005) {
                                                            BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, packageName2, -1005);
                                                            Slog.i(TAG, "Transport quota exceeded for package: " + packageName2);
                                                            EventLog.writeEvent((int) EventLogTags.FULL_BACKUP_QUOTA_EXCEEDED, packageName2);
                                                            this.mUserBackupManagerService.tearDownAgentAndKill(currentPackage2.applicationInfo);
                                                            enginePipes2 = enginePipes;
                                                        } else if (backupPackageStatus == -1003) {
                                                            BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, packageName2, -1003);
                                                            Slog.w(TAG, "Application failure for package: " + packageName2);
                                                            EventLog.writeEvent((int) EventLogTags.BACKUP_AGENT_FAILURE, packageName2);
                                                            this.mUserBackupManagerService.tearDownAgentAndKill(currentPackage2.applicationInfo);
                                                            enginePipes2 = enginePipes;
                                                        } else if (backupPackageStatus == -2003) {
                                                            BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, packageName2, -2003);
                                                            Slog.w(TAG, "Backup cancelled. package=" + packageName2 + ", cancelAll=" + this.mCancelAll);
                                                            EventLog.writeEvent((int) EventLogTags.FULL_BACKUP_CANCELLED, packageName2);
                                                            this.mUserBackupManagerService.tearDownAgentAndKill(currentPackage2.applicationInfo);
                                                            enginePipes2 = enginePipes;
                                                        } else if (backupPackageStatus != 0) {
                                                            break;
                                                        } else {
                                                            enginePipes2 = enginePipes;
                                                            BackupObserverUtils.sendBackupOnPackageResult(this.mBackupObserver, packageName2, 0);
                                                            EventLog.writeEvent((int) EventLogTags.FULL_BACKUP_SUCCESS, packageName2);
                                                            this.mUserBackupManagerService.logBackupComplete(packageName2);
                                                        }
                                                    }
                                                    cleanUpPipes(transportPipes3);
                                                    cleanUpPipes(enginePipes2);
                                                    if (currentPackage2.applicationInfo != null) {
                                                        try {
                                                            Slog.i(TAG, "Unbinding agent in " + packageName2);
                                                            try {
                                                                this.mUserBackupManagerService.getActivityManager().unbindBackupAgent(currentPackage2.applicationInfo);
                                                            } catch (RemoteException e8) {
                                                            }
                                                        } catch (Exception e9) {
                                                            transportPipes4 = transportPipes3;
                                                            backoff4 = backoff5;
                                                            enginePipes3 = enginePipes2;
                                                            e = e9;
                                                        } catch (Throwable th22) {
                                                            transportPipes4 = transportPipes3;
                                                            backoff4 = backoff5;
                                                            enginePipes3 = enginePipes2;
                                                            backupRunStatus4 = backupRunStatus3;
                                                            th = th22;
                                                            if (this.mCancelAll) {
                                                            }
                                                            Slog.i(TAG, "Full backup completed with status: " + backupRunStatus4);
                                                            BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus4);
                                                            cleanUpPipes(transportPipes4);
                                                            cleanUpPipes(enginePipes3);
                                                            unregisterTask();
                                                            fullBackupJob = this.mJob;
                                                            if (fullBackupJob != null) {
                                                            }
                                                            synchronized (this.mUserBackupManagerService.getQueueLock()) {
                                                            }
                                                        }
                                                    }
                                                    i = i2 + 1;
                                                    transportPipes4 = transportPipes3;
                                                    enginePipes3 = enginePipes2;
                                                    buffer2 = buffer;
                                                    N2 = N;
                                                    backupRunStatus4 = backupRunStatus3;
                                                    singlePackageBackupRunner = null;
                                                } catch (Exception e10) {
                                                    transportPipes4 = transportPipes3;
                                                    backoff4 = backoff5;
                                                    enginePipes3 = enginePipes;
                                                    e = e10;
                                                } catch (Throwable th23) {
                                                    transportPipes4 = transportPipes3;
                                                    backoff4 = backoff5;
                                                    enginePipes3 = enginePipes;
                                                    backupRunStatus4 = backupRunStatus3;
                                                    th = th23;
                                                }
                                            } catch (Throwable th24) {
                                                transportPipes2 = transportPipes;
                                                th2 = th24;
                                            }
                                        } catch (Throwable th25) {
                                            backoff2 = backoff5;
                                            obj = obj2;
                                            backupRunStatus3 = backupRunStatus4;
                                            transportPipes2 = transportPipes;
                                            th2 = th25;
                                        }
                                    }
                                } catch (Throwable th26) {
                                    backoff2 = backoff5;
                                    obj = obj2;
                                    backupRunStatus3 = backupRunStatus4;
                                    transportPipes2 = transportPipes;
                                    th2 = th26;
                                }
                                while (true) {
                                    try {
                                        try {
                                            break;
                                        } catch (Exception e11) {
                                            transportPipes4 = transportPipes2;
                                            backoff4 = backoff2;
                                            e = e11;
                                        } catch (Throwable th27) {
                                            transportPipes4 = transportPipes2;
                                            backoff4 = backoff2;
                                            backupRunStatus4 = backupRunStatus3;
                                            th = th27;
                                            if (this.mCancelAll) {
                                            }
                                            Slog.i(TAG, "Full backup completed with status: " + backupRunStatus4);
                                            BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus4);
                                            cleanUpPipes(transportPipes4);
                                            cleanUpPipes(enginePipes3);
                                            unregisterTask();
                                            fullBackupJob = this.mJob;
                                            if (fullBackupJob != null) {
                                            }
                                            synchronized (this.mUserBackupManagerService.getQueueLock()) {
                                            }
                                        }
                                    } catch (Throwable th28) {
                                        th2 = th28;
                                    }
                                }
                                throw th2;
                            }
                            backoff = backoff5;
                            backupRunStatus2 = backupRunStatus4;
                            break;
                        } catch (Exception e12) {
                            transportPipes4 = transportPipes;
                            backoff4 = backoff5;
                            e = e12;
                        } catch (Throwable th29) {
                            transportPipes4 = transportPipes;
                            backoff4 = backoff5;
                            th = th29;
                        }
                    } catch (Exception e13) {
                        e = e13;
                        backoff4 = backoff5;
                    } catch (Throwable th30) {
                        th = th30;
                        backoff4 = backoff5;
                    }
                }
            } else {
                try {
                    try {
                        Slog.w(TAG, "Transport not present; full data backup not performed");
                        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 15, this.mCurrentPackage, 1, null);
                        int backupRunStatus5 = this.mCancelAll ? -2003 : -1000;
                        Slog.i(TAG, "Full backup completed with status: " + backupRunStatus5);
                        BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus5);
                        cleanUpPipes(null);
                        cleanUpPipes(null);
                        unregisterTask();
                        FullBackupJob fullBackupJob2 = this.mJob;
                        if (fullBackupJob2 != null) {
                            fullBackupJob2.finishBackupPass(this.mUserId);
                        }
                        synchronized (this.mUserBackupManagerService.getQueueLock()) {
                            this.mUserBackupManagerService.setRunningFullBackupTask(null);
                        }
                        this.mListener.onFinished("PFTBT.run()");
                        this.mLatch.countDown();
                        if (this.mUpdateSchedule) {
                            this.mUserBackupManagerService.scheduleNextFullBackupJob(0L);
                        }
                        Slog.i(TAG, "Full data backup pass finished.");
                        this.mUserBackupManagerService.getWakelock().release();
                        return;
                    } catch (Exception e14) {
                        e = e14;
                    }
                } catch (Throwable th31) {
                    th = th31;
                    if (this.mCancelAll) {
                        backupRunStatus4 = -2003;
                    }
                    Slog.i(TAG, "Full backup completed with status: " + backupRunStatus4);
                    BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus4);
                    cleanUpPipes(transportPipes4);
                    cleanUpPipes(enginePipes3);
                    unregisterTask();
                    fullBackupJob = this.mJob;
                    if (fullBackupJob != null) {
                        fullBackupJob.finishBackupPass(this.mUserId);
                    }
                    synchronized (this.mUserBackupManagerService.getQueueLock()) {
                        this.mUserBackupManagerService.setRunningFullBackupTask(null);
                    }
                    this.mListener.onFinished("PFTBT.run()");
                    this.mLatch.countDown();
                    if (this.mUpdateSchedule) {
                        this.mUserBackupManagerService.scheduleNextFullBackupJob(backoff4);
                    }
                    Slog.i(TAG, "Full data backup pass finished.");
                    this.mUserBackupManagerService.getWakelock().release();
                    throw th;
                }
            }
            backupRunStatus4 = -1000;
            Slog.w(TAG, "Exception trying full transport backup", e);
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 19, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_EXCEPTION_FULL_BACKUP", Log.getStackTraceString(e)));
            int backupRunStatus6 = this.mCancelAll ? -2003 : -1000;
            Slog.i(TAG, "Full backup completed with status: " + backupRunStatus6);
            BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus6);
            cleanUpPipes(transportPipes4);
            cleanUpPipes(enginePipes3);
            unregisterTask();
            FullBackupJob fullBackupJob3 = this.mJob;
            if (fullBackupJob3 != null) {
                fullBackupJob3.finishBackupPass(this.mUserId);
            }
            synchronized (this.mUserBackupManagerService.getQueueLock()) {
                this.mUserBackupManagerService.setRunningFullBackupTask(null);
            }
            this.mListener.onFinished("PFTBT.run()");
            this.mLatch.countDown();
            if (this.mUpdateSchedule) {
                this.mUserBackupManagerService.scheduleNextFullBackupJob(backoff4);
            }
            Slog.i(TAG, "Full data backup pass finished.");
            this.mUserBackupManagerService.getWakelock().release();
            return;
        } else {
            backupRunStatus = 0;
        }
        try {
            Slog.i(TAG, "full backup requested but enabled=" + this.mUserBackupManagerService.isEnabled() + " setupComplete=" + this.mUserBackupManagerService.isSetupComplete() + "; ignoring");
            int monitoringEvent = this.mUserBackupManagerService.isSetupComplete() ? 13 : 14;
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, monitoringEvent, null, 3, null);
            this.mUpdateSchedule = false;
            int backupRunStatus7 = this.mCancelAll ? -2003 : -2001;
            Slog.i(TAG, "Full backup completed with status: " + backupRunStatus7);
            BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus7);
            cleanUpPipes(null);
            cleanUpPipes(null);
            unregisterTask();
            FullBackupJob fullBackupJob4 = this.mJob;
            if (fullBackupJob4 != null) {
                fullBackupJob4.finishBackupPass(this.mUserId);
            }
            synchronized (this.mUserBackupManagerService.getQueueLock()) {
                this.mUserBackupManagerService.setRunningFullBackupTask(null);
            }
            this.mListener.onFinished("PFTBT.run()");
            this.mLatch.countDown();
            if (this.mUpdateSchedule) {
                this.mUserBackupManagerService.scheduleNextFullBackupJob(0L);
            }
            Slog.i(TAG, "Full data backup pass finished.");
            this.mUserBackupManagerService.getWakelock().release();
        } catch (Exception e15) {
            e = e15;
        } catch (Throwable th32) {
            th = th32;
            backupRunStatus4 = backupRunStatus;
            if (this.mCancelAll) {
            }
            Slog.i(TAG, "Full backup completed with status: " + backupRunStatus4);
            BackupObserverUtils.sendBackupFinished(this.mBackupObserver, backupRunStatus4);
            cleanUpPipes(transportPipes4);
            cleanUpPipes(enginePipes3);
            unregisterTask();
            fullBackupJob = this.mJob;
            if (fullBackupJob != null) {
            }
            synchronized (this.mUserBackupManagerService.getQueueLock()) {
            }
        }
    }

    void cleanUpPipes(ParcelFileDescriptor[] pipes) {
        if (pipes != null) {
            if (pipes[0] != null) {
                ParcelFileDescriptor fd = pipes[0];
                pipes[0] = null;
                try {
                    fd.close();
                } catch (IOException e) {
                    Slog.w(TAG, "Unable to close pipe!");
                }
            }
            if (pipes[1] != null) {
                ParcelFileDescriptor fd2 = pipes[1];
                pipes[1] = null;
                try {
                    fd2.close();
                } catch (IOException e2) {
                    Slog.w(TAG, "Unable to close pipe!");
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class SinglePackageBackupPreflight implements BackupRestoreTask, FullBackupPreflight {
        private final int mCurrentOpToken;
        final long mQuota;
        final TransportConnection mTransportConnection;
        private final int mTransportFlags;
        final AtomicLong mResult = new AtomicLong(-1003);
        final CountDownLatch mLatch = new CountDownLatch(1);

        SinglePackageBackupPreflight(TransportConnection transportConnection, long quota, int currentOpToken, int transportFlags) {
            this.mTransportConnection = transportConnection;
            this.mQuota = quota;
            this.mCurrentOpToken = currentOpToken;
            this.mTransportFlags = transportFlags;
        }

        @Override // com.android.server.backup.fullbackup.FullBackupPreflight
        public int preflightFullBackup(PackageInfo pkg, final IBackupAgent agent) {
            long fullBackupAgentTimeoutMillis = PerformFullTransportBackupTask.this.mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis();
            try {
                PerformFullTransportBackupTask.this.mUserBackupManagerService.prepareOperationTimeout(this.mCurrentOpToken, fullBackupAgentTimeoutMillis, this, 0);
                agent.doMeasureFullBackup(this.mQuota, this.mCurrentOpToken, PerformFullTransportBackupTask.this.mUserBackupManagerService.getBackupManagerBinder(), this.mTransportFlags);
                this.mLatch.await(fullBackupAgentTimeoutMillis, TimeUnit.MILLISECONDS);
                final long totalSize = this.mResult.get();
                if (totalSize < 0) {
                    return (int) totalSize;
                }
                BackupTransportClient transport = this.mTransportConnection.connectOrThrow("PFTBT$SPBP.preflightFullBackup()");
                int result = transport.checkFullBackupSize(totalSize);
                if (result == -1005) {
                    try {
                        RemoteCall.execute(new RemoteCallable() { // from class: com.android.server.backup.fullbackup.PerformFullTransportBackupTask$SinglePackageBackupPreflight$$ExternalSyntheticLambda0
                            @Override // com.android.server.backup.remote.RemoteCallable
                            public final void call(Object obj) {
                                PerformFullTransportBackupTask.SinglePackageBackupPreflight.this.m2222x10491397(agent, totalSize, (IBackupCallback) obj);
                            }
                        }, PerformFullTransportBackupTask.this.mAgentTimeoutParameters.getQuotaExceededTimeoutMillis());
                    } catch (Exception e) {
                        e = e;
                        Slog.w(PerformFullTransportBackupTask.TAG, "Exception preflighting " + pkg.packageName + ": " + e.getMessage());
                        return -1003;
                    }
                }
                return result;
            } catch (Exception e2) {
                e = e2;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$preflightFullBackup$0$com-android-server-backup-fullbackup-PerformFullTransportBackupTask$SinglePackageBackupPreflight  reason: not valid java name */
        public /* synthetic */ void m2222x10491397(IBackupAgent agent, long totalSize, IBackupCallback callback) throws RemoteException {
            agent.doQuotaExceeded(totalSize, this.mQuota, callback);
        }

        @Override // com.android.server.backup.BackupRestoreTask
        public void execute() {
        }

        @Override // com.android.server.backup.BackupRestoreTask
        public void operationComplete(long result) {
            this.mResult.set(result);
            this.mLatch.countDown();
            PerformFullTransportBackupTask.this.mOperationStorage.removeOperation(this.mCurrentOpToken);
        }

        @Override // com.android.server.backup.BackupRestoreTask
        public void handleCancel(boolean cancelAll) {
            this.mResult.set(-1003L);
            this.mLatch.countDown();
            PerformFullTransportBackupTask.this.mOperationStorage.removeOperation(this.mCurrentOpToken);
        }

        @Override // com.android.server.backup.fullbackup.FullBackupPreflight
        public long getExpectedSizeOrErrorCode() {
            long fullBackupAgentTimeoutMillis = PerformFullTransportBackupTask.this.mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis();
            try {
                this.mLatch.await(fullBackupAgentTimeoutMillis, TimeUnit.MILLISECONDS);
                return this.mResult.get();
            } catch (InterruptedException e) {
                return -1L;
            }
        }
    }

    /* loaded from: classes.dex */
    class SinglePackageBackupRunner implements Runnable, BackupRestoreTask {
        final CountDownLatch mBackupLatch;
        private volatile int mBackupResult;
        private final int mCurrentOpToken;
        private FullBackupEngine mEngine;
        private final int mEphemeralToken;
        private volatile boolean mIsCancelled;
        final ParcelFileDescriptor mOutput;
        final SinglePackageBackupPreflight mPreflight;
        final CountDownLatch mPreflightLatch;
        private volatile int mPreflightResult;
        private final long mQuota;
        final PackageInfo mTarget;
        private final int mTransportFlags;

        SinglePackageBackupRunner(ParcelFileDescriptor output, PackageInfo target, TransportConnection transportConnection, long quota, int currentOpToken, int transportFlags) throws IOException {
            this.mOutput = ParcelFileDescriptor.dup(output.getFileDescriptor());
            this.mTarget = target;
            this.mCurrentOpToken = currentOpToken;
            int generateRandomIntegerToken = PerformFullTransportBackupTask.this.mUserBackupManagerService.generateRandomIntegerToken();
            this.mEphemeralToken = generateRandomIntegerToken;
            this.mPreflight = new SinglePackageBackupPreflight(transportConnection, quota, generateRandomIntegerToken, transportFlags);
            this.mPreflightLatch = new CountDownLatch(1);
            this.mBackupLatch = new CountDownLatch(1);
            this.mPreflightResult = -1003;
            this.mBackupResult = -1003;
            this.mQuota = quota;
            this.mTransportFlags = transportFlags;
            registerTask(target.packageName);
        }

        void registerTask(String packageName) {
            Set<String> packages = Sets.newHashSet(new String[]{packageName});
            PerformFullTransportBackupTask.this.mOperationStorage.registerOperationForPackages(this.mCurrentOpToken, 0, packages, this, 0);
        }

        void unregisterTask() {
            PerformFullTransportBackupTask.this.mOperationStorage.removeOperation(this.mCurrentOpToken);
        }

        @Override // java.lang.Runnable
        public void run() {
            FileOutputStream out = new FileOutputStream(this.mOutput.getFileDescriptor());
            this.mEngine = new FullBackupEngine(PerformFullTransportBackupTask.this.mUserBackupManagerService, out, this.mPreflight, this.mTarget, false, this, this.mQuota, this.mCurrentOpToken, this.mTransportFlags, PerformFullTransportBackupTask.this.mBackupEligibilityRules);
            try {
                try {
                    try {
                        try {
                            if (!this.mIsCancelled) {
                                this.mPreflightResult = this.mEngine.preflightCheck();
                            }
                            this.mPreflightLatch.countDown();
                            if (this.mPreflightResult == 0 && !this.mIsCancelled) {
                                this.mBackupResult = this.mEngine.backupOnePackage();
                            }
                            unregisterTask();
                            this.mBackupLatch.countDown();
                            this.mOutput.close();
                        } catch (Exception e) {
                            Slog.w(PerformFullTransportBackupTask.TAG, "Exception during full package backup of " + this.mTarget.packageName, e);
                            unregisterTask();
                            this.mBackupLatch.countDown();
                            this.mOutput.close();
                        }
                    } catch (Throwable th) {
                        this.mPreflightLatch.countDown();
                        throw th;
                    }
                } catch (Throwable th2) {
                    unregisterTask();
                    this.mBackupLatch.countDown();
                    try {
                        this.mOutput.close();
                    } catch (IOException e2) {
                        Slog.w(PerformFullTransportBackupTask.TAG, "Error closing transport pipe in runner");
                    }
                    throw th2;
                }
            } catch (IOException e3) {
                Slog.w(PerformFullTransportBackupTask.TAG, "Error closing transport pipe in runner");
            }
        }

        public void sendQuotaExceeded(long backupDataBytes, long quotaBytes) {
            this.mEngine.sendQuotaExceeded(backupDataBytes, quotaBytes);
        }

        long getPreflightResultBlocking() {
            long fullBackupAgentTimeoutMillis = PerformFullTransportBackupTask.this.mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis();
            try {
                this.mPreflightLatch.await(fullBackupAgentTimeoutMillis, TimeUnit.MILLISECONDS);
                if (this.mIsCancelled) {
                    return -2003L;
                }
                if (this.mPreflightResult == 0) {
                    return this.mPreflight.getExpectedSizeOrErrorCode();
                }
                return this.mPreflightResult;
            } catch (InterruptedException e) {
                return -1003L;
            }
        }

        int getBackupResultBlocking() {
            long fullBackupAgentTimeoutMillis = PerformFullTransportBackupTask.this.mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis();
            try {
                this.mBackupLatch.await(fullBackupAgentTimeoutMillis, TimeUnit.MILLISECONDS);
                if (this.mIsCancelled) {
                    return -2003;
                }
                return this.mBackupResult;
            } catch (InterruptedException e) {
                return -1003;
            }
        }

        @Override // com.android.server.backup.BackupRestoreTask
        public void execute() {
        }

        @Override // com.android.server.backup.BackupRestoreTask
        public void operationComplete(long result) {
        }

        @Override // com.android.server.backup.BackupRestoreTask
        public void handleCancel(boolean cancelAll) {
            Slog.w(PerformFullTransportBackupTask.TAG, "Full backup cancel of " + this.mTarget.packageName);
            PerformFullTransportBackupTask performFullTransportBackupTask = PerformFullTransportBackupTask.this;
            performFullTransportBackupTask.mMonitor = BackupManagerMonitorUtils.monitorEvent(performFullTransportBackupTask.mMonitor, 4, PerformFullTransportBackupTask.this.mCurrentPackage, 2, null);
            this.mIsCancelled = true;
            PerformFullTransportBackupTask.this.mUserBackupManagerService.handleCancel(this.mEphemeralToken, cancelAll);
            PerformFullTransportBackupTask.this.mUserBackupManagerService.tearDownAgentAndKill(this.mTarget.applicationInfo);
            this.mPreflightLatch.countDown();
            this.mBackupLatch.countDown();
            PerformFullTransportBackupTask.this.mOperationStorage.removeOperation(this.mCurrentOpToken);
        }
    }
}
