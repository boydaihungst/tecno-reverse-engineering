package com.android.server.backup.internal;

import android.app.backup.RestoreSet;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteException;
import android.util.EventLog;
import android.util.Pair;
import android.util.Slog;
import com.android.server.EventLogTags;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.DataChangedJournal;
import com.android.server.backup.OperationStorage;
import com.android.server.backup.TransportManager;
import com.android.server.backup.UserBackupManagerService;
import com.android.server.backup.fullbackup.PerformAdbBackupTask;
import com.android.server.backup.keyvalue.BackupRequest;
import com.android.server.backup.keyvalue.KeyValueBackupTask;
import com.android.server.backup.params.AdbBackupParams;
import com.android.server.backup.params.AdbParams;
import com.android.server.backup.params.AdbRestoreParams;
import com.android.server.backup.params.BackupParams;
import com.android.server.backup.params.ClearParams;
import com.android.server.backup.params.ClearRetryParams;
import com.android.server.backup.params.RestoreGetSetsParams;
import com.android.server.backup.params.RestoreParams;
import com.android.server.backup.restore.ActiveRestoreSession;
import com.android.server.backup.restore.PerformAdbRestoreTask;
import com.android.server.backup.restore.PerformUnifiedRestoreTask;
import com.android.server.backup.transport.BackupTransportClient;
import com.android.server.backup.transport.TransportConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
/* loaded from: classes.dex */
public class BackupHandler extends Handler {
    public static final int MSG_BACKUP_OPERATION_TIMEOUT = 17;
    public static final int MSG_BACKUP_RESTORE_STEP = 20;
    public static final int MSG_FULL_CONFIRMATION_TIMEOUT = 9;
    public static final int MSG_OP_COMPLETE = 21;
    public static final int MSG_REQUEST_BACKUP = 15;
    public static final int MSG_RESTORE_OPERATION_TIMEOUT = 18;
    public static final int MSG_RESTORE_SESSION_TIMEOUT = 8;
    public static final int MSG_RETRY_CLEAR = 12;
    public static final int MSG_RUN_ADB_BACKUP = 2;
    public static final int MSG_RUN_ADB_RESTORE = 10;
    public static final int MSG_RUN_BACKUP = 1;
    public static final int MSG_RUN_CLEAR = 4;
    public static final int MSG_RUN_GET_RESTORE_SETS = 6;
    public static final int MSG_RUN_RESTORE = 3;
    public static final int MSG_SCHEDULE_BACKUP_PACKAGE = 16;
    public static final int MSG_STOP = 22;
    private final UserBackupManagerService backupManagerService;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    private final HandlerThread mBackupThread;
    volatile boolean mIsStopping;
    private final OperationStorage mOperationStorage;

    public BackupHandler(UserBackupManagerService backupManagerService, OperationStorage operationStorage, HandlerThread backupThread) {
        super(backupThread.getLooper());
        this.mIsStopping = false;
        this.mBackupThread = backupThread;
        this.backupManagerService = backupManagerService;
        this.mOperationStorage = operationStorage;
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Objects.requireNonNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
    }

    public void stop() {
        this.mIsStopping = true;
        sendMessage(obtainMessage(22));
    }

    @Override // android.os.Handler
    public void dispatchMessage(Message message) {
        try {
            dispatchMessageInternal(message);
        } catch (Exception e) {
            if (!this.mIsStopping) {
                throw e;
            }
        }
    }

    void dispatchMessageInternal(Message message) {
        super.dispatchMessage(message);
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE, IGET, INVOKE, INVOKE, IGET, INVOKE, INVOKE, IGET, INVOKE, INVOKE, IGET, INVOKE, INVOKE, IGET, INVOKE, INVOKE, IGET, INVOKE, CONST_STR, CONST_STR, INVOKE, MOVE_EXCEPTION] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [398=4] */
    /* JADX WARN: Removed duplicated region for block: B:146:0x048c  */
    /* JADX WARN: Removed duplicated region for block: B:208:? A[RETURN, SYNTHETIC] */
    @Override // android.os.Handler
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void handleMessage(Message msg) {
        String str;
        StringBuilder sb;
        if (msg.what == 22) {
            Slog.v(BackupManagerService.TAG, "Stopping backup handler");
            this.backupManagerService.getWakelock().quit();
            this.mBackupThread.quitSafely();
        }
        if (this.mIsStopping) {
            return;
        }
        final TransportManager transportManager = this.backupManagerService.getTransportManager();
        switch (msg.what) {
            case 1:
                this.backupManagerService.setLastBackupPass(System.currentTimeMillis());
                final TransportConnection transportConnection = transportManager.getCurrentTransportClient("BH/MSG_RUN_BACKUP");
                BackupTransportClient transport = transportConnection != null ? transportConnection.connect("BH/MSG_RUN_BACKUP") : null;
                if (transport == null) {
                    if (transportConnection != null) {
                        transportManager.disposeOfTransportClient(transportConnection, "BH/MSG_RUN_BACKUP");
                    }
                    Slog.v(BackupManagerService.TAG, "Backup requested but no transport available");
                    return;
                }
                List<String> queue = new ArrayList<>();
                DataChangedJournal oldJournal = this.backupManagerService.getJournal();
                synchronized (this.backupManagerService.getQueueLock()) {
                    try {
                        try {
                            if (this.backupManagerService.isBackupRunning()) {
                                Slog.i(BackupManagerService.TAG, "Backup time but one already running");
                                return;
                            }
                            Slog.v(BackupManagerService.TAG, "Running a backup pass");
                            this.backupManagerService.setBackupRunning(true);
                            this.backupManagerService.getWakelock().acquire();
                            if (this.backupManagerService.getPendingBackups().size() > 0) {
                                for (BackupRequest b : this.backupManagerService.getPendingBackups().values()) {
                                    queue.add(b.packageName);
                                }
                                Slog.v(BackupManagerService.TAG, "clearing pending backups");
                                this.backupManagerService.getPendingBackups().clear();
                                this.backupManagerService.setJournal(null);
                            }
                            boolean staged = true;
                            if (queue.size() > 0) {
                                try {
                                    OnTaskFinishedListener listener = new OnTaskFinishedListener() { // from class: com.android.server.backup.internal.BackupHandler$$ExternalSyntheticLambda0
                                        @Override // com.android.server.backup.internal.OnTaskFinishedListener
                                        public final void onFinished(String str2) {
                                            TransportManager.this.disposeOfTransportClient(transportConnection, str2);
                                        }
                                    };
                                    try {
                                        KeyValueBackupTask.start(this.backupManagerService, this.mOperationStorage, transportConnection, transport.transportDirName(), queue, oldJournal, null, null, listener, Collections.emptyList(), false, false, this.backupManagerService.getEligibilityRulesForOperation(0));
                                    } catch (Exception e) {
                                        e = e;
                                        Slog.e(BackupManagerService.TAG, "Transport became unavailable attempting backup or error initializing backup task", e);
                                        staged = false;
                                        if (staged) {
                                        }
                                    }
                                } catch (Exception e2) {
                                    e = e2;
                                }
                            } else {
                                Slog.v(BackupManagerService.TAG, "Backup requested but nothing pending");
                                staged = false;
                            }
                            if (staged) {
                                transportManager.disposeOfTransportClient(transportConnection, "BH/MSG_RUN_BACKUP");
                                synchronized (this.backupManagerService.getQueueLock()) {
                                    this.backupManagerService.setBackupRunning(false);
                                }
                                this.backupManagerService.getWakelock().release();
                                return;
                            }
                            return;
                        } catch (Throwable th) {
                            th = th;
                            while (true) {
                                try {
                                    throw th;
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            }
                        }
                    } catch (Throwable th3) {
                        th = th3;
                    }
                }
            case 2:
                AdbBackupParams params = (AdbBackupParams) msg.obj;
                PerformAdbBackupTask task = new PerformAdbBackupTask(this.backupManagerService, this.mOperationStorage, params.fd, params.observer, params.includeApks, params.includeObbs, params.includeShared, params.doWidgets, params.curPassword, params.encryptPassword, params.allApps, params.includeSystem, params.doCompress, params.includeKeyValue, params.packages, params.latch, params.backupEligibilityRules);
                new Thread(task, "adb-backup").start();
                return;
            case 3:
                RestoreParams params2 = (RestoreParams) msg.obj;
                Slog.d(BackupManagerService.TAG, "MSG_RUN_RESTORE observer=" + params2.observer);
                PerformUnifiedRestoreTask task2 = new PerformUnifiedRestoreTask(this.backupManagerService, this.mOperationStorage, params2.mTransportConnection, params2.observer, params2.monitor, params2.token, params2.packageInfo, params2.pmToken, params2.isSystemRestore, params2.filterSet, params2.listener, params2.backupEligibilityRules);
                synchronized (this.backupManagerService.getPendingRestores()) {
                    if (this.backupManagerService.isRestoreInProgress()) {
                        Slog.d(BackupManagerService.TAG, "Restore in progress, queueing.");
                        this.backupManagerService.getPendingRestores().add(task2);
                    } else {
                        Slog.d(BackupManagerService.TAG, "Starting restore.");
                        this.backupManagerService.setRestoreInProgress(true);
                        Message restoreMsg = obtainMessage(20, task2);
                        sendMessage(restoreMsg);
                    }
                }
                return;
            case 4:
                ClearParams params3 = (ClearParams) msg.obj;
                Runnable task3 = new PerformClearTask(this.backupManagerService, params3.mTransportConnection, params3.packageInfo, params3.listener);
                task3.run();
                return;
            case 5:
            case 7:
            case 11:
            case 13:
            case 14:
            case 19:
            default:
                return;
            case 6:
                RestoreGetSetsParams params4 = (RestoreGetSetsParams) msg.obj;
                try {
                    try {
                        RestoreSet[] sets = params4.mTransportConnection.connectOrThrow("BH/MSG_RUN_GET_RESTORE_SETS").getAvailableRestoreSets();
                        synchronized (params4.session) {
                            params4.session.setRestoreSets(sets);
                        }
                        if (sets == null) {
                            EventLog.writeEvent((int) EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                        }
                        if (params4.observer != null) {
                            try {
                                params4.observer.restoreSetsAvailable(sets);
                            } catch (RemoteException e3) {
                                Slog.e(BackupManagerService.TAG, "Unable to report listing to observer");
                            } catch (Exception e4) {
                                e = e4;
                                str = BackupManagerService.TAG;
                                sb = new StringBuilder();
                                Slog.e(str, sb.append("Restore observer threw: ").append(e.getMessage()).toString());
                            }
                        }
                    } catch (Throwable th4) {
                        if (params4.observer != null) {
                            try {
                                params4.observer.restoreSetsAvailable((RestoreSet[]) null);
                            } catch (RemoteException e5) {
                                Slog.e(BackupManagerService.TAG, "Unable to report listing to observer");
                            } catch (Exception e6) {
                                Slog.e(BackupManagerService.TAG, "Restore observer threw: " + e6.getMessage());
                            }
                        }
                        removeMessages(8);
                        sendEmptyMessageDelayed(8, this.mAgentTimeoutParameters.getRestoreSessionTimeoutMillis());
                        params4.listener.onFinished("BH/MSG_RUN_GET_RESTORE_SETS");
                        throw th4;
                    }
                } catch (Exception e7) {
                    Slog.e(BackupManagerService.TAG, "Error from transport getting set list: " + e7.getMessage());
                    if (params4.observer != null) {
                        try {
                            params4.observer.restoreSetsAvailable((RestoreSet[]) null);
                        } catch (RemoteException e8) {
                            Slog.e(BackupManagerService.TAG, "Unable to report listing to observer");
                        } catch (Exception e9) {
                            e = e9;
                            str = BackupManagerService.TAG;
                            sb = new StringBuilder();
                            Slog.e(str, sb.append("Restore observer threw: ").append(e.getMessage()).toString());
                        }
                    }
                }
                removeMessages(8);
                sendEmptyMessageDelayed(8, this.mAgentTimeoutParameters.getRestoreSessionTimeoutMillis());
                params4.listener.onFinished("BH/MSG_RUN_GET_RESTORE_SETS");
                return;
            case 8:
                synchronized (this.backupManagerService) {
                    if (this.backupManagerService.getActiveRestoreSession() != null) {
                        Slog.w(BackupManagerService.TAG, "Restore session timed out; aborting");
                        this.backupManagerService.getActiveRestoreSession().markTimedOut();
                        ActiveRestoreSession activeRestoreSession = this.backupManagerService.getActiveRestoreSession();
                        Objects.requireNonNull(activeRestoreSession);
                        UserBackupManagerService userBackupManagerService = this.backupManagerService;
                        post(new ActiveRestoreSession.EndRestoreRunnable(userBackupManagerService, userBackupManagerService.getActiveRestoreSession()));
                    }
                }
                return;
            case 9:
                synchronized (this.backupManagerService.getAdbBackupRestoreConfirmations()) {
                    AdbParams params5 = this.backupManagerService.getAdbBackupRestoreConfirmations().get(msg.arg1);
                    if (params5 != null) {
                        Slog.i(BackupManagerService.TAG, "Full backup/restore timed out waiting for user confirmation");
                        this.backupManagerService.signalAdbBackupRestoreCompletion(params5);
                        this.backupManagerService.getAdbBackupRestoreConfirmations().delete(msg.arg1);
                        if (params5.observer != null) {
                            try {
                                params5.observer.onTimeout();
                            } catch (RemoteException e10) {
                            }
                        }
                    } else {
                        Slog.d(BackupManagerService.TAG, "couldn't find params for token " + msg.arg1);
                    }
                }
                return;
            case 10:
                AdbRestoreParams params6 = (AdbRestoreParams) msg.obj;
                PerformAdbRestoreTask task4 = new PerformAdbRestoreTask(this.backupManagerService, this.mOperationStorage, params6.fd, params6.curPassword, params6.encryptPassword, params6.observer, params6.latch);
                new Thread(task4, "adb-restore").start();
                return;
            case 12:
                ClearRetryParams params7 = (ClearRetryParams) msg.obj;
                this.backupManagerService.clearBackupData(params7.transportName, params7.packageName);
                return;
            case 15:
                BackupParams params8 = (BackupParams) msg.obj;
                this.backupManagerService.setBackupRunning(true);
                this.backupManagerService.getWakelock().acquire();
                KeyValueBackupTask.start(this.backupManagerService, this.mOperationStorage, params8.mTransportConnection, params8.dirName, params8.kvPackages, null, params8.observer, params8.monitor, params8.listener, params8.fullPackages, true, params8.nonIncrementalBackup, params8.mBackupEligibilityRules);
                return;
            case 16:
                String pkgName = (String) msg.obj;
                this.backupManagerService.dataChangedImpl(pkgName);
                return;
            case 17:
            case 18:
                Slog.d(BackupManagerService.TAG, "Timeout message received for token=" + Integer.toHexString(msg.arg1));
                this.backupManagerService.handleCancel(msg.arg1, false);
                return;
            case 20:
                try {
                    BackupRestoreTask task5 = (BackupRestoreTask) msg.obj;
                    task5.execute();
                    return;
                } catch (ClassCastException e11) {
                    Slog.e(BackupManagerService.TAG, "Invalid backup/restore task in flight, obj=" + msg.obj);
                    return;
                }
            case 21:
                try {
                    Pair<BackupRestoreTask, Long> taskWithResult = (Pair) msg.obj;
                    ((BackupRestoreTask) taskWithResult.first).operationComplete(((Long) taskWithResult.second).longValue());
                    return;
                } catch (ClassCastException e12) {
                    Slog.e(BackupManagerService.TAG, "Invalid completion in flight, obj=" + msg.obj);
                    return;
                }
        }
    }
}
