package com.android.server.backup.internal;

import android.content.pm.IPackageDataObserver;
import com.android.server.backup.UserBackupManagerService;
/* loaded from: classes.dex */
public class ClearDataObserver extends IPackageDataObserver.Stub {
    private UserBackupManagerService backupManagerService;

    public ClearDataObserver(UserBackupManagerService backupManagerService) {
        this.backupManagerService = backupManagerService;
    }

    public void onRemoveCompleted(String packageName, boolean succeeded) {
        synchronized (this.backupManagerService.getClearDataLock()) {
            this.backupManagerService.setClearingData(false);
            this.backupManagerService.getClearDataLock().notifyAll();
        }
    }
}
