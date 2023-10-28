package com.android.server.pm;

import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.CreateAppDataArgs;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Trace;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.StorageManagerInternal;
import android.os.storage.VolumeInfo;
import android.security.AndroidKeyStoreMaintenance;
import android.text.TextUtils;
import android.util.Slog;
import android.util.TimingsTraceLog;
import com.android.internal.util.Preconditions;
import com.android.server.SystemServerInitThreadPool;
import com.android.server.pm.Installer;
import com.android.server.pm.dex.ArtManagerService;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.SELinuxUtil;
import com.transsion.hubcore.server.pm.ITranPackageManagerService;
import dalvik.system.VMRuntime;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class AppDataHelper {
    private static final boolean DEBUG_APP_DATA = false;
    private final ArtManagerService mArtManagerService;
    private final PackageManagerServiceInjector mInjector;
    private final Installer mInstaller;
    private final PackageManagerService mPm;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppDataHelper(PackageManagerService pm) {
        this.mPm = pm;
        PackageManagerServiceInjector packageManagerServiceInjector = pm.mInjector;
        this.mInjector = packageManagerServiceInjector;
        this.mInstaller = packageManagerServiceInjector.getInstaller();
        this.mArtManagerService = packageManagerServiceInjector.getArtManagerService();
    }

    public void prepareAppDataAfterInstallLIF(AndroidPackage pkg) {
        prepareAppDataPostCommitLIF(pkg, 0);
    }

    public void prepareAppDataPostCommitLIF(final AndroidPackage pkg, int previousAppId) {
        PackageSetting ps;
        int flags;
        synchronized (this.mPm.mLock) {
            ps = this.mPm.mSettings.getPackageLPr(pkg.getPackageName());
            this.mPm.mSettings.writeKernelMappingLPr(ps);
        }
        if (!shouldHaveAppStorage(pkg)) {
            Slog.w("PackageManager", "Skipping preparing app data for " + pkg.getPackageName());
            return;
        }
        Installer.Batch batch = new Installer.Batch();
        final UserManagerInternal umInternal = this.mInjector.getUserManagerInternal();
        final StorageManagerInternal smInternal = (StorageManagerInternal) this.mInjector.getLocalService(StorageManagerInternal.class);
        for (final UserInfo user : umInternal.getUsers(false)) {
            if (StorageManager.isUserKeyUnlocked(user.id) && smInternal.isCeStoragePrepared(user.id)) {
                flags = 3;
            } else {
                int flags2 = user.id;
                if (umInternal.isUserRunning(flags2)) {
                    flags = 1;
                }
            }
            if (ps.getInstalled(user.id)) {
                prepareAppData(batch, pkg, previousAppId, user.id, flags).thenRun(new Runnable() { // from class: com.android.server.pm.AppDataHelper$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        AppDataHelper.lambda$prepareAppDataPostCommitLIF$0(UserManagerInternal.this, user, pkg, smInternal);
                    }
                });
            }
        }
        executeBatchLI(batch);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$prepareAppDataPostCommitLIF$0(UserManagerInternal umInternal, UserInfo user, AndroidPackage pkg, StorageManagerInternal smInternal) {
        if (umInternal.isUserUnlockingOrUnlocked(user.id)) {
            int uid = UserHandle.getUid(user.id, UserHandle.getAppId(pkg.getUid()));
            smInternal.prepareAppDataAfterInstall(pkg.getPackageName(), uid);
        }
    }

    private void executeBatchLI(Installer.Batch batch) {
        try {
            batch.execute(this.mInstaller);
        } catch (Installer.InstallerException e) {
            Slog.w("PackageManager", "Failed to execute pending operations", e);
        }
    }

    private CompletableFuture<?> prepareAppData(Installer.Batch batch, AndroidPackage pkg, int previousAppId, int userId, int flags) {
        if (pkg == null) {
            Slog.wtf("PackageManager", "Package was null!", new Throwable());
            return CompletableFuture.completedFuture(null);
        } else if (!shouldHaveAppStorage(pkg)) {
            Slog.w("PackageManager", "Skipping preparing app data for " + pkg.getPackageName());
            return CompletableFuture.completedFuture(null);
        } else {
            return prepareAppDataLeaf(batch, pkg, previousAppId, userId, flags);
        }
    }

    private void prepareAppDataAndMigrate(Installer.Batch batch, final AndroidPackage pkg, final int userId, final int flags, final boolean maybeMigrateAppData) {
        prepareAppData(batch, pkg, -1, userId, flags).thenRun(new Runnable() { // from class: com.android.server.pm.AppDataHelper$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                AppDataHelper.this.m5369x33c03be7(maybeMigrateAppData, pkg, userId, flags);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$prepareAppDataAndMigrate$1$com-android-server-pm-AppDataHelper  reason: not valid java name */
    public /* synthetic */ void m5369x33c03be7(boolean maybeMigrateAppData, AndroidPackage pkg, int userId, int flags) {
        if (maybeMigrateAppData && maybeMigrateAppDataLIF(pkg, userId)) {
            Installer.Batch batchInner = new Installer.Batch();
            prepareAppData(batchInner, pkg, -1, userId, flags);
            executeBatchLI(batchInner);
        }
    }

    private CompletableFuture<?> prepareAppDataLeaf(Installer.Batch batch, final AndroidPackage pkg, int previousAppId, final int userId, final int flags) {
        final PackageSetting ps;
        String seInfoUser;
        synchronized (this.mPm.mLock) {
            try {
                ps = this.mPm.mSettings.getPackageLPr(pkg.getPackageName());
                seInfoUser = SELinuxUtil.getSeinfoUser(ps.readUserState(userId));
            } catch (Throwable th) {
                th = th;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                throw th;
            }
        }
        String volumeUuid = pkg.getVolumeUuid();
        final String packageName = pkg.getPackageName();
        int appId = UserHandle.getAppId(pkg.getUid());
        String pkgSeInfo = AndroidPackageUtils.getSeInfo(pkg, ps);
        Preconditions.checkNotNull(pkgSeInfo);
        String seInfo = pkgSeInfo + seInfoUser;
        int targetSdkVersion = pkg.getTargetSdkVersion();
        boolean usesSdk = !pkg.getUsesSdkLibraries().isEmpty();
        final CreateAppDataArgs args = Installer.buildCreateAppDataArgs(volumeUuid, packageName, userId, flags, appId, seInfo, targetSdkVersion, usesSdk);
        args.previousAppId = previousAppId;
        return batch.createAppData(args).whenComplete(new BiConsumer() { // from class: com.android.server.pm.AppDataHelper$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                AppDataHelper.this.m5370lambda$prepareAppDataLeaf$2$comandroidserverpmAppDataHelper(packageName, pkg, userId, flags, args, ps, (Long) obj, (Throwable) obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$prepareAppDataLeaf$2$com-android-server-pm-AppDataHelper  reason: not valid java name */
    public /* synthetic */ void m5370lambda$prepareAppDataLeaf$2$comandroidserverpmAppDataHelper(String packageName, AndroidPackage pkg, int userId, int flags, CreateAppDataArgs args, PackageSetting ps, Long ceDataInode, Throwable e) {
        if (e != null) {
            PackageManagerServiceUtils.logCriticalInfo(5, "Failed to create app data for " + packageName + ", but trying to recover: " + e);
            destroyAppDataLeafLIF(pkg, userId, flags);
            try {
                ceDataInode = Long.valueOf(this.mInstaller.createAppData(args).ceDataInode);
                PackageManagerServiceUtils.logCriticalInfo(3, "Recovery succeeded!");
            } catch (Installer.InstallerException e2) {
                PackageManagerServiceUtils.logCriticalInfo(3, "Recovery failed!");
            }
        }
        if (this.mPm.isDeviceUpgrading() || this.mPm.isFirstBoot() || userId != 0) {
            this.mArtManagerService.prepareAppProfiles(pkg, userId, false);
        }
        if ((flags & 2) != 0 && ceDataInode.longValue() != -1) {
            synchronized (this.mPm.mLock) {
                ps.setCeDataInode(ceDataInode.longValue(), userId);
            }
        }
        prepareAppDataContentsLeafLIF(pkg, ps, userId, flags);
    }

    public void prepareAppDataContentsLIF(AndroidPackage pkg, PackageStateInternal pkgSetting, int userId, int flags) {
        if (pkg == null) {
            Slog.wtf("PackageManager", "Package was null!", new Throwable());
        } else {
            prepareAppDataContentsLeafLIF(pkg, pkgSetting, userId, flags);
        }
    }

    private void prepareAppDataContentsLeafLIF(AndroidPackage pkg, PackageStateInternal pkgSetting, int userId, int flags) {
        String primaryCpuAbi;
        String volumeUuid = pkg.getVolumeUuid();
        String packageName = pkg.getPackageName();
        if ((flags & 2) != 0 && (primaryCpuAbi = AndroidPackageUtils.getPrimaryCpuAbi(pkg, pkgSetting)) != null && !VMRuntime.is64BitAbi(primaryCpuAbi)) {
            String nativeLibPath = pkg.getNativeLibraryDir();
            if (!new File(nativeLibPath).exists()) {
                return;
            }
            try {
                this.mInstaller.linkNativeLibraryDirectory(volumeUuid, packageName, nativeLibPath, userId);
            } catch (Installer.InstallerException e) {
                Slog.e("PackageManager", "Failed to link native for " + packageName + ": " + e);
            }
        }
    }

    private boolean maybeMigrateAppDataLIF(AndroidPackage pkg, int userId) {
        if (pkg.isSystem() && !StorageManager.isFileEncryptedNativeOrEmulated()) {
            int storageTarget = pkg.isDefaultToDeviceProtectedStorage() ? 1 : 2;
            try {
                this.mInstaller.migrateAppData(pkg.getVolumeUuid(), pkg.getPackageName(), userId, storageTarget);
            } catch (Installer.InstallerException e) {
                PackageManagerServiceUtils.logCriticalInfo(5, "Failed to migrate " + pkg.getPackageName() + ": " + e.getMessage());
            }
            return true;
        }
        return false;
    }

    public void reconcileAppsData(int userId, int flags, boolean migrateAppsData) {
        StorageManager storage = (StorageManager) this.mInjector.getSystemService(StorageManager.class);
        for (VolumeInfo vol : storage.getWritablePrivateVolumes()) {
            String volumeUuid = vol.getFsUuid();
            synchronized (this.mPm.mInstallLock) {
                reconcileAppsDataLI(volumeUuid, userId, flags, migrateAppsData);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reconcileAppsDataLI(String volumeUuid, int userId, int flags, boolean migrateAppData) {
        reconcileAppsDataLI(volumeUuid, userId, flags, migrateAppData, false);
    }

    private List<String> reconcileAppsDataLI(String volumeUuid, int userId, int flags, boolean migrateAppData, boolean onlyCoreApps) {
        List<String> result;
        String str;
        String str2;
        Computer snapshot;
        String str3;
        List<String> result2;
        List<String> result3;
        String str4;
        Computer snapshot2;
        String str5;
        String str6;
        int i;
        File[] files;
        int i2;
        List<String> result4;
        int i3;
        int i4;
        File[] files2;
        String str7;
        String packageName;
        String str8;
        Computer snapshot3;
        int i5 = userId;
        String str9 = "PackageManager";
        Slog.v("PackageManager", "reconcileAppsData for " + volumeUuid + " u" + i5 + " 0x" + Integer.toHexString(flags) + " migrateAppData=" + migrateAppData);
        PackageManagerService.sMtkSystemServerIns.addBootEvent("PMS:reconcileAppsDataLI");
        List<String> result5 = onlyCoreApps ? new ArrayList<>() : null;
        try {
            this.mInstaller.cleanupInvalidPackageDirs(volumeUuid, i5, flags);
        } catch (Installer.InstallerException e) {
            PackageManagerServiceUtils.logCriticalInfo(5, "Failed to cleanup deleted dirs: " + e);
        }
        File ceDir = Environment.getDataUserCeDirectory(volumeUuid, userId);
        File deDir = Environment.getDataUserDeDirectory(volumeUuid, userId);
        Computer snapshot4 = this.mPm.snapshotComputer();
        String str10 = "Failed to destroy: ";
        String str11 = " due to: ";
        String str12 = "Destroying ";
        if ((flags & 2) == 0) {
            result = result5;
            str = "Destroying ";
            str2 = " due to: ";
            snapshot = snapshot4;
            str3 = "Failed to destroy: ";
        } else if (StorageManager.isFileEncryptedNativeOrEmulated() && !StorageManager.isUserKeyUnlocked(userId)) {
            throw new RuntimeException("Yikes, someone asked us to reconcile CE storage while " + i5 + " was still locked; this would have caused massive data loss!");
        } else {
            File[] files3 = FileUtils.listFilesOrEmpty(ceDir);
            int length = files3.length;
            int i6 = 0;
            while (i6 < length) {
                String str13 = str10;
                File file = files3[i6];
                String packageName2 = file.getName();
                try {
                    assertPackageStorageValid(snapshot4, volumeUuid, packageName2, i5);
                    result4 = result5;
                    i3 = i6;
                    i4 = length;
                    files2 = files3;
                    str7 = str12;
                    packageName = str11;
                    snapshot3 = snapshot4;
                    str8 = str13;
                } catch (PackageManagerException e2) {
                    Computer snapshot5 = snapshot4;
                    PackageManagerServiceUtils.logCriticalInfo(5, str12 + file + str11 + e2);
                    try {
                        i3 = i6;
                        i4 = length;
                        files2 = files3;
                        str7 = str12;
                        packageName = str11;
                        result4 = result5;
                        str8 = str13;
                        snapshot3 = snapshot5;
                        try {
                            this.mInstaller.destroyAppData(volumeUuid, packageName2, userId, 2, 0L);
                        } catch (Installer.InstallerException e3) {
                            e2 = e3;
                            PackageManagerServiceUtils.logCriticalInfo(5, str8 + e2);
                            i6 = i3 + 1;
                            str11 = packageName;
                            str10 = str8;
                            snapshot4 = snapshot3;
                            length = i4;
                            files3 = files2;
                            str12 = str7;
                            result5 = result4;
                        }
                    } catch (Installer.InstallerException e4) {
                        e2 = e4;
                        result4 = result5;
                        i3 = i6;
                        i4 = length;
                        files2 = files3;
                        str7 = str12;
                        packageName = str11;
                        str8 = str13;
                        snapshot3 = snapshot5;
                    }
                }
                i6 = i3 + 1;
                str11 = packageName;
                str10 = str8;
                snapshot4 = snapshot3;
                length = i4;
                files3 = files2;
                str12 = str7;
                result5 = result4;
            }
            result = result5;
            str = str12;
            str2 = str11;
            snapshot = snapshot4;
            str3 = str10;
        }
        if ((flags & 1) != 0) {
            File[] files4 = FileUtils.listFilesOrEmpty(deDir);
            int length2 = files4.length;
            int i7 = 0;
            while (i7 < length2) {
                File file2 = files4[i7];
                String packageName3 = file2.getName();
                try {
                    assertPackageStorageValid(snapshot, volumeUuid, packageName3, i5);
                    str5 = str2;
                    files = files4;
                    i2 = length2;
                    str6 = str;
                    i = i7;
                } catch (PackageManagerException e5) {
                    String str14 = str;
                    str5 = str2;
                    PackageManagerServiceUtils.logCriticalInfo(5, str14 + file2 + str2 + e5);
                    try {
                        str6 = str14;
                        i = i7;
                        files = files4;
                        i2 = length2;
                        try {
                            this.mInstaller.destroyAppData(volumeUuid, packageName3, userId, 1, 0L);
                        } catch (Installer.InstallerException e6) {
                            e2 = e6;
                            PackageManagerServiceUtils.logCriticalInfo(5, str3 + e2);
                            i7 = i + 1;
                            files4 = files;
                            str2 = str5;
                            str = str6;
                            length2 = i2;
                        }
                    } catch (Installer.InstallerException e7) {
                        e2 = e7;
                        str6 = str14;
                        i = i7;
                        files = files4;
                        i2 = length2;
                    }
                }
                i7 = i + 1;
                files4 = files;
                str2 = str5;
                str = str6;
                length2 = i2;
            }
        }
        Trace.traceBegin(262144L, "prepareAppDataAndMigrate");
        Installer.Batch batch = new Installer.Batch();
        List<? extends PackageStateInternal> packages = snapshot.getVolumePackages(volumeUuid);
        int preparedCount = 0;
        for (PackageStateInternal ps : packages) {
            String packageName4 = ps.getPackageName();
            if (ps.getPkg() == null) {
                Slog.w(str9, "Odd, missing scanned package " + packageName4);
                ITranPackageManagerService.Instance().onPackageDeleted(packageName4, new int[]{i5});
                result2 = result;
            } else if (!onlyCoreApps || ps.getPkg().isCoreApp()) {
                List<String> result6 = result;
                if (!ps.getUserStateOrDefault(i5).isInstalled()) {
                    result3 = result6;
                    str4 = str9;
                    snapshot2 = snapshot;
                } else {
                    result3 = result6;
                    str4 = str9;
                    snapshot2 = snapshot;
                    prepareAppDataAndMigrate(batch, ps.getPkg(), userId, flags, migrateAppData);
                    preparedCount++;
                }
                i5 = userId;
                snapshot = snapshot2;
                result = result3;
                str9 = str4;
            } else {
                result2 = result;
                result2.add(packageName4);
            }
            result = result2;
        }
        List<String> result7 = result;
        executeBatchLI(batch);
        Trace.traceEnd(262144L);
        Slog.v(str9, "reconcileAppsData finished " + preparedCount + " packages");
        return result7;
    }

    private void assertPackageStorageValid(Computer snapshot, String volumeUuid, String packageName, int userId) throws PackageManagerException {
        PackageStateInternal packageState = snapshot.getPackageStateInternal(packageName);
        if (packageState == null) {
            throw new PackageManagerException("Package " + packageName + " is unknown");
        }
        if (!TextUtils.equals(volumeUuid, packageState.getVolumeUuid())) {
            throw new PackageManagerException("Package " + packageName + " found on unknown volume " + volumeUuid + "; expected volume " + packageState.getVolumeUuid());
        }
        if (!packageState.getUserStateOrDefault(userId).isInstalled()) {
            throw new PackageManagerException("Package " + packageName + " not installed for user " + userId);
        }
        if (packageState.getPkg() != null && !shouldHaveAppStorage(packageState.getPkg())) {
            throw new PackageManagerException("Package " + packageName + " shouldn't have storage");
        }
    }

    public Future<?> fixAppsDataOnBoot() {
        final int storageFlags;
        if (StorageManager.isFileEncryptedNativeOrEmulated()) {
            storageFlags = 1;
        } else {
            storageFlags = 3;
        }
        final List<String> deferPackages = reconcileAppsDataLI(StorageManager.UUID_PRIVATE_INTERNAL, 0, storageFlags, true, true);
        Future<?> prepareAppDataFuture = SystemServerInitThreadPool.submit(new Runnable() { // from class: com.android.server.pm.AppDataHelper$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                AppDataHelper.this.m5368lambda$fixAppsDataOnBoot$3$comandroidserverpmAppDataHelper(deferPackages, storageFlags);
            }
        }, "prepareAppData");
        return prepareAppDataFuture;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:30:0x007a -> B:31:0x007b). Please submit an issue!!! */
    /* renamed from: lambda$fixAppsDataOnBoot$3$com-android-server-pm-AppDataHelper  reason: not valid java name */
    public /* synthetic */ void m5368lambda$fixAppsDataOnBoot$3$comandroidserverpmAppDataHelper(List deferPackages, int storageFlags) {
        AndroidPackage pkg;
        TimingsTraceLog traceLog = new TimingsTraceLog("SystemServerTimingAsync", 262144L);
        traceLog.traceBegin("AppDataFixup");
        try {
            this.mInstaller.fixupAppData(StorageManager.UUID_PRIVATE_INTERNAL, 3);
        } catch (Installer.InstallerException e) {
            Slog.w("PackageManager", "Trouble fixing GIDs", e);
        }
        traceLog.traceEnd();
        traceLog.traceBegin("AppDataPrepare");
        if (deferPackages == null || deferPackages.isEmpty()) {
            return;
        }
        int count = 0;
        Installer.Batch batch = new Installer.Batch();
        Iterator it = deferPackages.iterator();
        while (it.hasNext()) {
            String pkgName = (String) it.next();
            synchronized (this.mPm.mLock) {
                try {
                    PackageSetting ps = this.mPm.mSettings.getPackageLPr(pkgName);
                    if (ps != null && ps.getInstalled(0)) {
                        AndroidPackage pkg2 = ps.getPkg();
                        pkg = pkg2;
                    } else {
                        pkg = null;
                    }
                    try {
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
            if (pkg != null) {
                prepareAppDataAndMigrate(batch, pkg, 0, storageFlags, true);
                count++;
            }
        }
        synchronized (this.mPm.mInstallLock) {
            executeBatchLI(batch);
        }
        traceLog.traceEnd();
        Slog.i("PackageManager", "Deferred reconcileAppsData finished " + count + " packages");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearAppDataLIF(AndroidPackage pkg, int userId, int flags) {
        if (pkg == null) {
            return;
        }
        clearAppDataLeafLIF(pkg, userId, flags);
        if ((131072 & flags) == 0) {
            clearAppProfilesLIF(pkg);
        }
    }

    private void clearAppDataLeafLIF(AndroidPackage pkg, int userId, int flags) {
        PackageSetting ps;
        int[] resolveUserIds;
        synchronized (this.mPm.mLock) {
            ps = this.mPm.mSettings.getPackageLPr(pkg.getPackageName());
        }
        for (int realUserId : this.mPm.resolveUserIds(userId)) {
            long ceDataInode = ps != null ? ps.getCeDataInode(realUserId) : 0L;
            try {
                this.mInstaller.clearAppData(pkg.getVolumeUuid(), pkg.getPackageName(), realUserId, flags, ceDataInode);
            } catch (Installer.InstallerException e) {
                Slog.w("PackageManager", String.valueOf(e));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearAppProfilesLIF(AndroidPackage pkg) {
        if (pkg == null) {
            Slog.wtf("PackageManager", "Package was null!", new Throwable());
        } else {
            this.mArtManagerService.clearAppProfiles(pkg);
        }
    }

    public void destroyAppDataLIF(AndroidPackage pkg, int userId, int flags) {
        if (pkg == null) {
            Slog.wtf("PackageManager", "Package was null!", new Throwable());
        } else {
            destroyAppDataLeafLIF(pkg, userId, flags);
        }
    }

    public void destroyAppDataLeafLIF(AndroidPackage pkg, int userId, int flags) {
        PackageSetting ps;
        int[] resolveUserIds;
        synchronized (this.mPm.mLock) {
            ps = this.mPm.mSettings.getPackageLPr(pkg.getPackageName());
        }
        for (int realUserId : this.mPm.resolveUserIds(userId)) {
            long ceDataInode = ps != null ? ps.getCeDataInode(realUserId) : 0L;
            try {
                this.mInstaller.destroyAppData(pkg.getVolumeUuid(), pkg.getPackageName(), realUserId, flags, ceDataInode);
            } catch (Installer.InstallerException e) {
                Slog.w("PackageManager", String.valueOf(e));
            }
            this.mPm.getDexManager().notifyPackageDataDestroyed(pkg.getPackageName(), userId);
        }
    }

    public void destroyAppProfilesLIF(AndroidPackage pkg) {
        if (pkg == null) {
            Slog.wtf("PackageManager", "Package was null!", new Throwable());
        } else {
            destroyAppProfilesLeafLIF(pkg);
        }
    }

    private void destroyAppProfilesLeafLIF(AndroidPackage pkg) {
        try {
            this.mInstaller.destroyAppProfiles(pkg.getPackageName());
        } catch (Installer.InstallerException e) {
            Slog.w("PackageManager", String.valueOf(e));
        }
    }

    private boolean shouldHaveAppStorage(AndroidPackage pkg) {
        PackageManager.Property noAppDataProp = pkg.getProperties().get("android.internal.PROPERTY_NO_APP_DATA_STORAGE");
        return noAppDataProp == null || !noAppDataProp.getBoolean();
    }

    public void clearKeystoreData(int userId, int appId) {
        int[] resolveUserIds;
        if (appId < 0) {
            return;
        }
        for (int realUserId : this.mPm.resolveUserIds(userId)) {
            AndroidKeyStoreMaintenance.clearNamespace(0, UserHandle.getUid(realUserId, appId));
        }
    }
}
