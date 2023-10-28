package com.android.server.pm;

import android.app.ResourcesManager;
import android.content.IIntentReceiver;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackagePartitions;
import android.content.pm.UserInfo;
import android.content.pm.VersionedPackage;
import android.content.pm.parsing.ApkLiteParseUtils;
import android.os.Environment;
import android.os.FileUtils;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.StorageManagerInternal;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.policy.AttributeCache;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.pm.Settings;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
/* loaded from: classes2.dex */
public final class StorageEventHelper extends StorageEventListener {
    private final BroadcastHelper mBroadcastHelper;
    private final DeletePackageHelper mDeletePackageHelper;
    final ArraySet<String> mLoadedVolumes = new ArraySet<>();
    private final PackageManagerService mPm;
    private final RemovePackageHelper mRemovePackageHelper;

    public StorageEventHelper(PackageManagerService pm, DeletePackageHelper deletePackageHelper, RemovePackageHelper removePackageHelper) {
        this.mPm = pm;
        this.mBroadcastHelper = new BroadcastHelper(pm.mInjector);
        this.mDeletePackageHelper = deletePackageHelper;
        this.mRemovePackageHelper = removePackageHelper;
    }

    public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
        if (vol.type == 1) {
            if (vol.state == 2) {
                String volumeUuid = vol.getFsUuid();
                this.mPm.mUserManager.reconcileUsers(volumeUuid);
                reconcileApps(this.mPm.snapshotComputer(), volumeUuid);
                this.mPm.mInstallerService.onPrivateVolumeMounted(volumeUuid);
                loadPrivatePackages(vol);
            } else if (vol.state == 5) {
                unloadPrivatePackages(vol);
            }
        }
    }

    public void onVolumeForgotten(String fsUuid) {
        if (TextUtils.isEmpty(fsUuid)) {
            Slog.e("PackageManager", "Forgetting internal storage is probably a mistake; ignoring");
            return;
        }
        synchronized (this.mPm.mLock) {
            List<? extends PackageStateInternal> packages = this.mPm.mSettings.getVolumePackagesLPr(fsUuid);
            for (PackageStateInternal ps : packages) {
                Slog.d("PackageManager", "Destroying " + ps.getPackageName() + " because volume was forgotten");
                this.mPm.deletePackageVersioned(new VersionedPackage(ps.getPackageName(), -1), new PackageManager.LegacyPackageDeleteObserver((IPackageDeleteObserver) null).getBinder(), 0, 2);
                AttributeCache.instance().removePackage(ps.getPackageName());
            }
            this.mPm.mSettings.onVolumeForgotten(fsUuid);
            this.mPm.writeSettingsLPrTEMP();
        }
    }

    private void loadPrivatePackages(final VolumeInfo vol) {
        this.mPm.mHandler.post(new Runnable() { // from class: com.android.server.pm.StorageEventHelper$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                StorageEventHelper.this.m5696xe7f0548(vol);
            }
        });
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Can't wrap try/catch for region: R(14:34|(2:75|(2:80|81)(4:77|78|79|55))(2:38|39)|40|41|42|43|44|134|51|52|53|54|55|32) */
    /* JADX WARN: Code restructure failed: missing block: B:56:0x014a, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:57:0x014b, code lost:
        r17 = r2;
     */
    /* JADX WARN: Code restructure failed: missing block: B:58:0x014e, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:59:0x014f, code lost:
        r17 = r2;
        r16 = r7;
     */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:99:? -> B:48:0x0141). Please submit an issue!!! */
    /* renamed from: loadPrivatePackagesInner */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void m5696xe7f0548(VolumeInfo vol) {
        Settings.VersionInfo ver;
        List<? extends PackageStateInternal> packages;
        int flags;
        StorageManager sm;
        AppDataHelper appDataHelper;
        String volumeUuid = vol.fsUuid;
        if (TextUtils.isEmpty(volumeUuid)) {
            Slog.e("PackageManager", "Loading internal storage is probably a mistake; ignoring");
            return;
        }
        AppDataHelper appDataHelper2 = new AppDataHelper(this.mPm);
        ArrayList<PackageFreezer> freezers = new ArrayList<>();
        ArrayList<AndroidPackage> loaded = new ArrayList<>();
        int parseFlags = this.mPm.getDefParseFlags() | 8;
        InstallPackageHelper installPackageHelper = new InstallPackageHelper(this.mPm);
        synchronized (this.mPm.mLock) {
            try {
                ver = this.mPm.mSettings.findOrCreateVersion(volumeUuid);
                packages = this.mPm.mSettings.getVolumePackagesLPr(volumeUuid);
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
        for (PackageStateInternal ps : packages) {
            freezers.add(this.mPm.freezePackage(ps.getPackageName(), "loadPrivatePackagesInner"));
            synchronized (this.mPm.mInstallLock) {
                try {
                    AndroidPackage pkg = installPackageHelper.scanSystemPackageTracedLI(ps.getPath(), parseFlags, 512, null);
                    loaded.add(pkg);
                } catch (PackageManagerException e) {
                    Slog.w("PackageManager", "Failed to scan " + ps.getPath() + ": " + e.getMessage());
                }
                if (!PackagePartitions.FINGERPRINT.equals(ver.fingerprint)) {
                    appDataHelper2.clearAppDataLIF(ps.getPkg(), -1, 131111);
                }
            }
        }
        StorageManager sm2 = (StorageManager) this.mPm.mInjector.getSystemService(StorageManager.class);
        UserManagerInternal umInternal = this.mPm.mInjector.getUserManagerInternal();
        StorageManagerInternal smInternal = (StorageManagerInternal) this.mPm.mInjector.getLocalService(StorageManagerInternal.class);
        for (UserInfo user : this.mPm.mUserManager.getUsers(false)) {
            if (StorageManager.isUserKeyUnlocked(user.id) && smInternal.isCeStoragePrepared(user.id)) {
                flags = 3;
            } else {
                int flags2 = user.id;
                if (umInternal.isUserRunning(flags2)) {
                    flags = 1;
                }
            }
            int parseFlags2 = parseFlags;
            sm2.prepareUserStorage(volumeUuid, user.id, user.serialNumber, flags);
            synchronized (this.mPm.mInstallLock) {
                try {
                    sm = sm2;
                    try {
                        appDataHelper2.reconcileAppsDataLI(volumeUuid, user.id, flags, true);
                    } catch (Throwable th3) {
                        th = th3;
                        try {
                            throw th;
                            break;
                        } catch (IllegalStateException e2) {
                            e = e2;
                            appDataHelper = appDataHelper2;
                            Slog.w("PackageManager", "Failed to prepare storage: " + e);
                            parseFlags = parseFlags2;
                            sm2 = sm;
                            appDataHelper2 = appDataHelper;
                        }
                    }
                } catch (Throwable th4) {
                    th = th4;
                    sm = sm2;
                    throw th;
                    break;
                    break;
                }
            }
            appDataHelper = appDataHelper2;
            parseFlags = parseFlags2;
            sm2 = sm;
            appDataHelper2 = appDataHelper;
        }
        synchronized (this.mPm.mLock) {
            boolean isUpgrade = !PackagePartitions.FINGERPRINT.equals(ver.fingerprint);
            if (isUpgrade) {
                PackageManagerServiceUtils.logCriticalInfo(4, "Build fingerprint changed from " + ver.fingerprint + " to " + PackagePartitions.FINGERPRINT + "; regranting permissions for " + volumeUuid);
            }
            this.mPm.mPermissionManager.onStorageVolumeMounted(volumeUuid, isUpgrade);
            ver.forceCurrent();
            this.mPm.writeSettingsLPrTEMP();
        }
        Iterator<PackageFreezer> it = freezers.iterator();
        while (it.hasNext()) {
            PackageFreezer freezer = it.next();
            freezer.close();
        }
        if (PackageManagerService.DEBUG_INSTALL) {
            Slog.d("PackageManager", "Loaded packages " + loaded);
        }
        sendResourcesChangedBroadcast(true, false, loaded, null);
        synchronized (this.mLoadedVolumes) {
            this.mLoadedVolumes.add(vol.getId());
        }
    }

    private void unloadPrivatePackages(final VolumeInfo vol) {
        this.mPm.mHandler.post(new Runnable() { // from class: com.android.server.pm.StorageEventHelper$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                StorageEventHelper.this.m5697xba4da800(vol);
            }
        });
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: unloadPrivatePackagesInner */
    public void m5697xba4da800(VolumeInfo vol) {
        Throwable th;
        String volumeUuid = vol.fsUuid;
        if (TextUtils.isEmpty(volumeUuid)) {
            Slog.e("PackageManager", "Unloading internal storage is probably a mistake; ignoring");
            return;
        }
        int[] userIds = this.mPm.mUserManager.getUserIds();
        ArrayList<AndroidPackage> unloaded = new ArrayList<>();
        synchronized (this.mPm.mInstallLock) {
            synchronized (this.mPm.mLock) {
                List<? extends PackageStateInternal> packages = this.mPm.mSettings.getVolumePackagesLPr(volumeUuid);
                for (PackageStateInternal ps : packages) {
                    if (ps.getPkg() != null) {
                        AndroidPackage pkg = ps.getPkg();
                        PackageRemovedInfo outInfo = new PackageRemovedInfo(this.mPm);
                        PackageFreezer freezer = this.mPm.freezePackageForDelete(ps.getPackageName(), 1, "unloadPrivatePackagesInner");
                        try {
                            try {
                                if (this.mDeletePackageHelper.deletePackageLIF(ps.getPackageName(), null, false, userIds, 1, outInfo, false)) {
                                    unloaded.add(pkg);
                                } else {
                                    Slog.w("PackageManager", "Failed to unload " + ps.getPath());
                                }
                                if (freezer != null) {
                                    freezer.close();
                                }
                                AttributeCache.instance().removePackage(ps.getPackageName());
                            } catch (Throwable th2) {
                                th = th2;
                                if (freezer != null) {
                                    freezer.close();
                                }
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    }
                }
                this.mPm.writeSettingsLPrTEMP();
            }
        }
        if (PackageManagerService.DEBUG_INSTALL) {
            Slog.d("PackageManager", "Unloaded packages " + unloaded);
        }
        sendResourcesChangedBroadcast(false, false, unloaded, null);
        synchronized (this.mLoadedVolumes) {
            this.mLoadedVolumes.remove(vol.getId());
        }
        ResourcesManager.getInstance().invalidatePath(vol.getPath().getAbsolutePath());
        for (int i = 0; i < 3; i++) {
            System.gc();
            System.runFinalization();
        }
    }

    private void sendResourcesChangedBroadcast(boolean mediaStatus, boolean replacing, ArrayList<AndroidPackage> packages, IIntentReceiver finishedReceiver) {
        int size = packages.size();
        String[] packageNames = new String[size];
        int[] packageUids = new int[size];
        for (int i = 0; i < size; i++) {
            AndroidPackage pkg = packages.get(i);
            packageNames[i] = pkg.getPackageName();
            packageUids[i] = pkg.getUid();
        }
        this.mBroadcastHelper.sendResourcesChangedBroadcast(mediaStatus, replacing, packageNames, packageUids, finishedReceiver);
    }

    public void reconcileApps(Computer snapshot, String volumeUuid) {
        List<String> absoluteCodePaths = collectAbsoluteCodePaths(snapshot);
        File[] files = FileUtils.listFilesOrEmpty(Environment.getDataAppDirectory(volumeUuid));
        List<File> filesToDelete = null;
        for (File file : files) {
            boolean isPackage = (ApkLiteParseUtils.isApkFile(file) || file.isDirectory()) && !PackageInstallerService.isStageName(file.getName());
            if (isPackage) {
                String absolutePath = file.getAbsolutePath();
                boolean pathValid = false;
                int absoluteCodePathCount = absoluteCodePaths.size();
                int i = 0;
                while (true) {
                    if (i >= absoluteCodePathCount) {
                        break;
                    }
                    String absoluteCodePath = absoluteCodePaths.get(i);
                    if (!absoluteCodePath.startsWith(absolutePath)) {
                        i++;
                    } else {
                        pathValid = true;
                        break;
                    }
                }
                if (!pathValid) {
                    if (filesToDelete == null) {
                        filesToDelete = new ArrayList<>();
                    }
                    filesToDelete.add(file);
                }
            }
        }
        if (filesToDelete != null) {
            int fileToDeleteCount = filesToDelete.size();
            for (int i2 = 0; i2 < fileToDeleteCount; i2++) {
                File fileToDelete = filesToDelete.get(i2);
                PackageManagerServiceUtils.logCriticalInfo(5, "Destroying orphaned at " + fileToDelete);
                synchronized (this.mPm.mInstallLock) {
                    this.mRemovePackageHelper.removeCodePathLI(fileToDelete);
                }
            }
        }
    }

    private List<String> collectAbsoluteCodePaths(Computer snapshot) {
        List<String> codePaths = new ArrayList<>();
        ArrayMap<String, ? extends PackageStateInternal> packageStates = snapshot.getPackageStates();
        int packageCount = packageStates.size();
        for (int i = 0; i < packageCount; i++) {
            PackageStateInternal ps = packageStates.valueAt(i);
            codePaths.add(ps.getPath().getAbsolutePath());
        }
        return codePaths;
    }

    public void dumpLoadedVolumes(PrintWriter pw, DumpState dumpState) {
        if (dumpState.onTitlePrinted()) {
            pw.println();
        }
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ", 120);
        ipw.println();
        ipw.println("Loaded volumes:");
        ipw.increaseIndent();
        synchronized (this.mLoadedVolumes) {
            if (this.mLoadedVolumes.size() == 0) {
                ipw.println("(none)");
            } else {
                for (int i = 0; i < this.mLoadedVolumes.size(); i++) {
                    ipw.println(this.mLoadedVolumes.valueAt(i));
                }
            }
        }
        ipw.decreaseIndent();
    }
}
