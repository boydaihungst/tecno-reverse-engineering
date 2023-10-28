package com.android.server.backup.internal;

import android.app.backup.IBackupObserver;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.EventLog;
import android.util.Slog;
import com.android.server.EventLogTags;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.TransportManager;
import com.android.server.backup.UserBackupManagerService;
import com.android.server.backup.transport.BackupTransportClient;
import com.android.server.backup.transport.TransportConnection;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class PerformInitializeTask implements Runnable {
    private final UserBackupManagerService mBackupManagerService;
    private final File mBaseStateDir;
    private final OnTaskFinishedListener mListener;
    private IBackupObserver mObserver;
    private final String[] mQueue;
    private final TransportManager mTransportManager;

    public PerformInitializeTask(UserBackupManagerService backupManagerService, String[] transportNames, IBackupObserver observer, OnTaskFinishedListener listener) {
        this(backupManagerService, backupManagerService.getTransportManager(), transportNames, observer, listener, backupManagerService.getBaseStateDir());
    }

    PerformInitializeTask(UserBackupManagerService backupManagerService, TransportManager transportManager, String[] transportNames, IBackupObserver observer, OnTaskFinishedListener listener, File baseStateDir) {
        this.mBackupManagerService = backupManagerService;
        this.mTransportManager = transportManager;
        this.mQueue = transportNames;
        this.mObserver = observer;
        this.mListener = listener;
        this.mBaseStateDir = baseStateDir;
    }

    private void notifyResult(String target, int status) {
        try {
            IBackupObserver iBackupObserver = this.mObserver;
            if (iBackupObserver != null) {
                iBackupObserver.onResult(target, status);
            }
        } catch (RemoteException e) {
            this.mObserver = null;
        }
    }

    private void notifyFinished(int status) {
        try {
            IBackupObserver iBackupObserver = this.mObserver;
            if (iBackupObserver != null) {
                iBackupObserver.backupFinished(status);
            }
        } catch (RemoteException e) {
            this.mObserver = null;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [174=6] */
    @Override // java.lang.Runnable
    public void run() {
        String[] strArr;
        int i;
        List<TransportConnection> transportClientsToDisposeOf = new ArrayList<>(this.mQueue.length);
        int result = 0;
        try {
            try {
                String[] strArr2 = this.mQueue;
                int i2 = 0;
                for (int length = strArr2.length; i2 < length; length = i) {
                    String transportName = strArr2[i2];
                    TransportConnection transportConnection = this.mTransportManager.getTransportClient(transportName, "PerformInitializeTask.run()");
                    if (transportConnection == null) {
                        Slog.e(BackupManagerService.TAG, "Requested init for " + transportName + " but not found");
                        strArr = strArr2;
                        i = length;
                    } else {
                        transportClientsToDisposeOf.add(transportConnection);
                        Slog.i(BackupManagerService.TAG, "Initializing (wiping) backup transport storage: " + transportName);
                        String transportDirName = this.mTransportManager.getTransportDirName(transportConnection.getTransportComponent());
                        EventLog.writeEvent((int) EventLogTags.BACKUP_START, transportDirName);
                        long startRealtime = SystemClock.elapsedRealtime();
                        BackupTransportClient transport = transportConnection.connectOrThrow("PerformInitializeTask.run()");
                        int status = transport.initializeDevice();
                        if (status != 0) {
                            Slog.e(BackupManagerService.TAG, "Transport error in initializeDevice()");
                        } else {
                            status = transport.finishBackup();
                            if (status != 0) {
                                Slog.e(BackupManagerService.TAG, "Transport error in finishBackup()");
                            }
                        }
                        if (status == 0) {
                            Slog.i(BackupManagerService.TAG, "Device init successful");
                            i = length;
                            int millis = (int) (SystemClock.elapsedRealtime() - startRealtime);
                            strArr = strArr2;
                            EventLog.writeEvent((int) EventLogTags.BACKUP_INITIALIZE, new Object[0]);
                            File stateFileDir = new File(this.mBaseStateDir, transportDirName);
                            this.mBackupManagerService.resetBackupState(stateFileDir);
                            EventLog.writeEvent((int) EventLogTags.BACKUP_SUCCESS, 0, Integer.valueOf(millis));
                            this.mBackupManagerService.recordInitPending(false, transportName, transportDirName);
                            notifyResult(transportName, 0);
                        } else {
                            strArr = strArr2;
                            i = length;
                            EventLog.writeEvent((int) EventLogTags.BACKUP_TRANSPORT_FAILURE, "(initialize)");
                            this.mBackupManagerService.recordInitPending(true, transportName, transportDirName);
                            notifyResult(transportName, status);
                            result = status;
                            try {
                                long delay = transport.requestBackupTime();
                                try {
                                    Slog.w(BackupManagerService.TAG, "Init failed on " + transportName + " resched in " + delay);
                                    this.mBackupManagerService.getAlarmManager().set(0, System.currentTimeMillis() + delay, this.mBackupManagerService.getRunInitIntent());
                                    result = result;
                                    i2++;
                                    strArr2 = strArr;
                                } catch (Exception e) {
                                    e = e;
                                    Slog.e(BackupManagerService.TAG, "Unexpected error performing init", e);
                                    result = -1000;
                                    for (TransportConnection transportConnection2 : transportClientsToDisposeOf) {
                                        this.mTransportManager.disposeOfTransportClient(transportConnection2, "PerformInitializeTask.run()");
                                    }
                                    notifyFinished(result);
                                    this.mListener.onFinished("PerformInitializeTask.run()");
                                } catch (Throwable th) {
                                    th = th;
                                    result = result;
                                    for (TransportConnection transportConnection3 : transportClientsToDisposeOf) {
                                        this.mTransportManager.disposeOfTransportClient(transportConnection3, "PerformInitializeTask.run()");
                                    }
                                    notifyFinished(result);
                                    this.mListener.onFinished("PerformInitializeTask.run()");
                                    throw th;
                                }
                            } catch (Exception e2) {
                                e = e2;
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        }
                    }
                    i2++;
                    strArr2 = strArr;
                }
                for (TransportConnection transportConnection4 : transportClientsToDisposeOf) {
                    this.mTransportManager.disposeOfTransportClient(transportConnection4, "PerformInitializeTask.run()");
                }
            } catch (Throwable th3) {
                th = th3;
            }
        } catch (Exception e3) {
            e = e3;
        }
        notifyFinished(result);
        this.mListener.onFinished("PerformInitializeTask.run()");
    }
}
