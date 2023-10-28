package com.android.server.rollback;

import android.content.rollback.PackageRollbackInfo;
import android.os.storage.StorageManager;
import android.util.Slog;
import com.android.server.pm.ApexManager;
import com.android.server.pm.Installer;
import java.util.List;
/* loaded from: classes2.dex */
public class AppDataRollbackHelper {
    private static final String TAG = "RollbackManager";
    private final ApexManager mApexManager;
    private final Installer mInstaller;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppDataRollbackHelper(Installer installer) {
        this.mInstaller = installer;
        this.mApexManager = ApexManager.getInstance();
    }

    AppDataRollbackHelper(Installer installer, ApexManager apexManager) {
        this.mInstaller = installer;
        this.mApexManager = apexManager;
    }

    public void snapshotAppData(int rollbackId, PackageRollbackInfo packageRollbackInfo, int[] userIds) {
        int storageFlags;
        for (int user : userIds) {
            if (isUserCredentialLocked(user)) {
                Slog.v(TAG, "User: " + user + " isn't unlocked, skipping CE userdata backup.");
                storageFlags = 1;
                packageRollbackInfo.addPendingBackup(user);
            } else {
                storageFlags = 3;
            }
            doSnapshot(packageRollbackInfo, user, rollbackId, storageFlags);
        }
    }

    public boolean restoreAppData(int rollbackId, PackageRollbackInfo packageRollbackInfo, int userId, int appId, String seInfo) {
        int storageFlags;
        boolean changedRollback;
        List<Integer> pendingBackups = packageRollbackInfo.getPendingBackups();
        List<PackageRollbackInfo.RestoreInfo> pendingRestores = packageRollbackInfo.getPendingRestores();
        if (pendingBackups == null || pendingBackups.indexOf(Integer.valueOf(userId)) == -1) {
            if (isUserCredentialLocked(userId)) {
                pendingRestores.add(new PackageRollbackInfo.RestoreInfo(userId, appId, seInfo));
                storageFlags = 1;
                changedRollback = true;
            } else {
                int storageFlags2 = 1 | 2;
                storageFlags = storageFlags2;
                changedRollback = false;
            }
        } else {
            pendingBackups.remove(pendingBackups.indexOf(Integer.valueOf(userId)));
            storageFlags = 1;
            changedRollback = true;
        }
        doRestoreOrWipe(packageRollbackInfo, userId, rollbackId, appId, seInfo, storageFlags);
        return changedRollback;
    }

    private boolean doSnapshot(PackageRollbackInfo packageRollbackInfo, int userId, int rollbackId, int flags) {
        if (packageRollbackInfo.isApex()) {
            if ((flags & 2) != 0) {
                return this.mApexManager.snapshotCeData(userId, rollbackId, packageRollbackInfo.getPackageName());
            }
            return true;
        }
        try {
            return this.mInstaller.snapshotAppData(packageRollbackInfo.getPackageName(), userId, rollbackId, flags);
        } catch (Installer.InstallerException ie) {
            Slog.e(TAG, "Unable to create app data snapshot for: " + packageRollbackInfo.getPackageName() + ", userId: " + userId, ie);
            return false;
        }
    }

    private boolean doRestoreOrWipe(PackageRollbackInfo packageRollbackInfo, int userId, int rollbackId, int appId, String seInfo, int flags) {
        if (packageRollbackInfo.isApex()) {
            switch (packageRollbackInfo.getRollbackDataPolicy()) {
                case 0:
                    if ((flags & 2) != 0) {
                        this.mApexManager.restoreCeData(userId, rollbackId, packageRollbackInfo.getPackageName());
                        return true;
                    }
                    return true;
                case 1:
                default:
                    return true;
            }
        }
        try {
            switch (packageRollbackInfo.getRollbackDataPolicy()) {
                case 0:
                    this.mInstaller.restoreAppDataSnapshot(packageRollbackInfo.getPackageName(), appId, seInfo, userId, rollbackId, flags);
                    break;
                case 1:
                    this.mInstaller.clearAppData(null, packageRollbackInfo.getPackageName(), userId, flags, 0L);
                    break;
            }
            return true;
        } catch (Installer.InstallerException ie) {
            Slog.e(TAG, "Unable to restore/wipe app data: " + packageRollbackInfo.getPackageName() + " policy=" + packageRollbackInfo.getRollbackDataPolicy(), ie);
            return false;
        }
    }

    public void destroyAppDataSnapshot(int rollbackId, PackageRollbackInfo packageRollbackInfo, int user) {
        try {
            this.mInstaller.destroyAppDataSnapshot(packageRollbackInfo.getPackageName(), user, rollbackId, 3);
        } catch (Installer.InstallerException ie) {
            Slog.e(TAG, "Unable to delete app data snapshot for " + packageRollbackInfo.getPackageName(), ie);
        }
    }

    public void destroyApexDeSnapshots(int rollbackId) {
        this.mApexManager.destroyDeSnapshots(rollbackId);
    }

    public void destroyApexCeSnapshots(int userId, int rollbackId) {
        if (!isUserCredentialLocked(userId)) {
            this.mApexManager.destroyCeSnapshots(userId, rollbackId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean commitPendingBackupAndRestoreForUser(int userId, Rollback rollback) {
        boolean hasPendingBackup;
        boolean foundBackupOrRestore;
        boolean hasPendingRestore;
        boolean foundBackupOrRestore2 = false;
        for (PackageRollbackInfo info : rollback.info.getPackages()) {
            List<Integer> pendingBackupUsers = info.getPendingBackups();
            if (pendingBackupUsers != null && pendingBackupUsers.indexOf(Integer.valueOf(userId)) != -1) {
                foundBackupOrRestore2 = true;
                hasPendingBackup = true;
            } else {
                hasPendingBackup = false;
            }
            PackageRollbackInfo.RestoreInfo ri = info.getRestoreInfo(userId);
            if (ri == null) {
                foundBackupOrRestore = foundBackupOrRestore2;
                hasPendingRestore = false;
            } else {
                foundBackupOrRestore = true;
                hasPendingRestore = true;
            }
            if (hasPendingBackup && hasPendingRestore) {
                info.removePendingBackup(userId);
                info.removePendingRestoreInfo(userId);
            } else {
                if (hasPendingBackup) {
                    int idx = pendingBackupUsers.indexOf(Integer.valueOf(userId));
                    if (doSnapshot(info, userId, rollback.info.getRollbackId(), 2)) {
                        pendingBackupUsers.remove(idx);
                    }
                }
                if (hasPendingRestore && doRestoreOrWipe(info, userId, rollback.info.getRollbackId(), ri.appId, ri.seInfo, 2)) {
                    info.removeRestoreInfo(ri);
                }
            }
            foundBackupOrRestore2 = foundBackupOrRestore;
        }
        return foundBackupOrRestore2;
    }

    public boolean isUserCredentialLocked(int userId) {
        return StorageManager.isFileEncryptedNativeOrEmulated() && !StorageManager.isUserKeyUnlocked(userId);
    }
}
