package com.android.server.backup.internal;

import android.content.pm.PackageInfo;
import android.util.Slog;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.TransportManager;
import com.android.server.backup.UserBackupManagerService;
import com.android.server.backup.transport.BackupTransportClient;
import com.android.server.backup.transport.TransportConnection;
import java.io.File;
/* loaded from: classes.dex */
public class PerformClearTask implements Runnable {
    private final UserBackupManagerService mBackupManagerService;
    private final OnTaskFinishedListener mListener;
    private final PackageInfo mPackage;
    private final TransportConnection mTransportConnection;
    private final TransportManager mTransportManager;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PerformClearTask(UserBackupManagerService backupManagerService, TransportConnection transportConnection, PackageInfo packageInfo, OnTaskFinishedListener listener) {
        this.mBackupManagerService = backupManagerService;
        this.mTransportManager = backupManagerService.getTransportManager();
        this.mTransportConnection = transportConnection;
        this.mPackage = packageInfo;
        this.mListener = listener;
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, IGET, INVOKE, IGET, INVOKE, INVOKE, IGET, INVOKE, IGET, INVOKE, INVOKE] complete} */
    @Override // java.lang.Runnable
    public void run() {
        StringBuilder sb;
        BackupTransportClient transport = null;
        try {
            try {
                String transportDirName = this.mTransportManager.getTransportDirName(this.mTransportConnection.getTransportComponent());
                File stateDir = new File(this.mBackupManagerService.getBaseStateDir(), transportDirName);
                File stateFile = new File(stateDir, this.mPackage.packageName);
                stateFile.delete();
                transport = this.mTransportConnection.connectOrThrow("PerformClearTask.run()");
                transport.clearBackupData(this.mPackage);
                if (transport != null) {
                    try {
                        transport.finishBackup();
                    } catch (Exception e) {
                        e = e;
                        sb = new StringBuilder();
                        Slog.e(BackupManagerService.TAG, sb.append("Unable to mark clear operation finished: ").append(e.getMessage()).toString());
                        this.mListener.onFinished("PerformClearTask.run()");
                        this.mBackupManagerService.getWakelock().release();
                    }
                }
            } catch (Exception e2) {
                Slog.e(BackupManagerService.TAG, "Transport threw clearing data for " + this.mPackage + ": " + e2.getMessage());
                if (transport != null) {
                    try {
                        transport.finishBackup();
                    } catch (Exception e3) {
                        e = e3;
                        sb = new StringBuilder();
                        Slog.e(BackupManagerService.TAG, sb.append("Unable to mark clear operation finished: ").append(e.getMessage()).toString());
                        this.mListener.onFinished("PerformClearTask.run()");
                        this.mBackupManagerService.getWakelock().release();
                    }
                }
            }
            this.mListener.onFinished("PerformClearTask.run()");
            this.mBackupManagerService.getWakelock().release();
        } catch (Throwable e4) {
            if (transport != null) {
                try {
                    transport.finishBackup();
                } catch (Exception e5) {
                    Slog.e(BackupManagerService.TAG, "Unable to mark clear operation finished: " + e5.getMessage());
                }
            }
            this.mListener.onFinished("PerformClearTask.run()");
            this.mBackupManagerService.getWakelock().release();
            throw e4;
        }
    }
}
