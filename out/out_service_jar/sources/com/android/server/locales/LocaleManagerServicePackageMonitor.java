package com.android.server.locales;

import com.android.internal.content.PackageMonitor;
/* loaded from: classes.dex */
final class LocaleManagerServicePackageMonitor extends PackageMonitor {
    private LocaleManagerBackupHelper mBackupHelper;
    private SystemAppUpdateTracker mSystemAppUpdateTracker;

    /* JADX INFO: Access modifiers changed from: package-private */
    public LocaleManagerServicePackageMonitor(LocaleManagerBackupHelper localeManagerBackupHelper, SystemAppUpdateTracker systemAppUpdateTracker) {
        this.mBackupHelper = localeManagerBackupHelper;
        this.mSystemAppUpdateTracker = systemAppUpdateTracker;
    }

    public void onPackageAdded(String packageName, int uid) {
        this.mBackupHelper.onPackageAdded(packageName, uid);
    }

    public void onPackageDataCleared(String packageName, int uid) {
        this.mBackupHelper.onPackageDataCleared();
    }

    public void onPackageRemoved(String packageName, int uid) {
        this.mBackupHelper.onPackageRemoved();
    }

    public void onPackageUpdateFinished(String packageName, int uid) {
        this.mSystemAppUpdateTracker.onPackageUpdateFinished(packageName, uid);
    }
}
