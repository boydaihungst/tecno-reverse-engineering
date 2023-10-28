package com.android.server.backup.fullbackup;

import android.app.backup.IFullBackupRestoreObserver;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.backup.BackupManagerService;
/* loaded from: classes.dex */
public abstract class FullBackupTask implements Runnable {
    IFullBackupRestoreObserver mObserver;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FullBackupTask(IFullBackupRestoreObserver observer) {
        this.mObserver = observer;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void sendStartBackup() {
        IFullBackupRestoreObserver iFullBackupRestoreObserver = this.mObserver;
        if (iFullBackupRestoreObserver != null) {
            try {
                iFullBackupRestoreObserver.onStartBackup();
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "full backup observer went away: startBackup");
                this.mObserver = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void sendOnBackupPackage(String name) {
        IFullBackupRestoreObserver iFullBackupRestoreObserver = this.mObserver;
        if (iFullBackupRestoreObserver != null) {
            try {
                iFullBackupRestoreObserver.onBackupPackage(name);
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "full backup observer went away: backupPackage");
                this.mObserver = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void sendEndBackup() {
        IFullBackupRestoreObserver iFullBackupRestoreObserver = this.mObserver;
        if (iFullBackupRestoreObserver != null) {
            try {
                iFullBackupRestoreObserver.onEndBackup();
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "full backup observer went away: endBackup");
                this.mObserver = null;
            }
        }
    }
}
