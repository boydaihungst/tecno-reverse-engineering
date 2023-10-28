package com.android.server.pm;

import android.os.Trace;
import android.os.incremental.IncrementalManager;
import android.util.Log;
import android.util.Slog;
import android.util.SparseBooleanArray;
import com.android.internal.util.ArrayUtils;
import com.android.server.pm.Installer;
import com.android.server.pm.parsing.PackageCacher;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.PackageImpl;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.component.ParsedInstrumentation;
import java.io.File;
import java.util.Collections;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class RemovePackageHelper {
    private final AppDataHelper mAppDataHelper;
    private final IncrementalManager mIncrementalManager;
    private final Installer mInstaller;
    private final PermissionManagerServiceInternal mPermissionManager;
    private final PackageManagerService mPm;
    private final SharedLibrariesImpl mSharedLibraries;
    private final UserManagerInternal mUserManagerInternal;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RemovePackageHelper(PackageManagerService pm, AppDataHelper appDataHelper) {
        this.mPm = pm;
        this.mIncrementalManager = pm.mInjector.getIncrementalManager();
        this.mInstaller = pm.mInjector.getInstaller();
        this.mUserManagerInternal = pm.mInjector.getUserManagerInternal();
        this.mPermissionManager = pm.mInjector.getPermissionManagerServiceInternal();
        this.mSharedLibraries = pm.mInjector.getSharedLibrariesImpl();
        this.mAppDataHelper = appDataHelper;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RemovePackageHelper(PackageManagerService pm) {
        this(pm, new AppDataHelper(pm));
    }

    public void removeCodePathLI(File codePath) {
        if (codePath.isDirectory()) {
            File codePathParent = codePath.getParentFile();
            boolean needRemoveParent = codePathParent.getName().startsWith("~~");
            try {
                boolean isIncremental = this.mIncrementalManager != null && IncrementalManager.isIncrementalPath(codePath.getAbsolutePath());
                if (isIncremental) {
                    if (needRemoveParent) {
                        this.mIncrementalManager.rmPackageDir(codePathParent);
                    } else {
                        this.mIncrementalManager.rmPackageDir(codePath);
                    }
                }
                String packageName = codePath.getName();
                this.mInstaller.rmPackageDir(packageName, codePath.getAbsolutePath());
                if (needRemoveParent) {
                    this.mInstaller.rmPackageDir(packageName, codePathParent.getAbsolutePath());
                    removeCachedResult(codePathParent);
                    return;
                }
                return;
            } catch (Installer.InstallerException e) {
                Slog.w("PackageManager", "Failed to remove code path", e);
                return;
            }
        }
        codePath.delete();
    }

    private void removeCachedResult(File codePath) {
        if (this.mPm.getCacheDir() == null) {
            return;
        }
        PackageCacher cacher = new PackageCacher(this.mPm.getCacheDir());
        cacher.cleanCachedResult(codePath);
    }

    public void removePackageLI(AndroidPackage pkg, boolean chatty) {
        PackageStateInternal ps = this.mPm.snapshotComputer().getPackageStateInternal(pkg.getPackageName());
        if (ps != null) {
            removePackageLI(ps.getPackageName(), chatty);
        } else if (PackageManagerService.DEBUG_REMOVE && chatty) {
            Log.d("PackageManager", "Not removing package " + pkg.getPackageName() + "; mExtras == null");
        }
    }

    private void removePackageLI(String packageName, boolean chatty) {
        if (PackageManagerService.DEBUG_INSTALL && chatty) {
            Log.d("PackageManager", "Removing package " + packageName);
        }
        synchronized (this.mPm.mLock) {
            AndroidPackage removedPackage = this.mPm.mPackages.remove(packageName);
            if (removedPackage != null) {
                cleanPackageDataStructuresLILPw(removedPackage, chatty);
            }
        }
    }

    private void cleanPackageDataStructuresLILPw(AndroidPackage pkg, boolean chatty) {
        this.mPm.mComponentResolver.removeAllComponents(pkg, chatty);
        this.mPermissionManager.onPackageRemoved(pkg);
        this.mPm.getPackageProperty().removeAllProperties(pkg);
        int instrumentationSize = ArrayUtils.size(pkg.getInstrumentations());
        StringBuilder r = null;
        for (int i = 0; i < instrumentationSize; i++) {
            ParsedInstrumentation a = pkg.getInstrumentations().get(i);
            this.mPm.getInstrumentation().remove(a.getComponentName());
            if (PackageManagerService.DEBUG_REMOVE && chatty) {
                if (r == null) {
                    r = new StringBuilder(256);
                } else {
                    r.append(' ');
                }
                r.append(a.getName());
            }
        }
        if (r != null && PackageManagerService.DEBUG_REMOVE) {
            Log.d("PackageManager", "  Instrumentation: " + ((Object) r));
        }
        StringBuilder r2 = null;
        if (pkg.isSystem()) {
            int libraryNamesSize = pkg.getLibraryNames().size();
            for (int i2 = 0; i2 < libraryNamesSize; i2++) {
                String name = pkg.getLibraryNames().get(i2);
                if (this.mSharedLibraries.removeSharedLibraryLPw(name, 0L) && PackageManagerService.DEBUG_REMOVE && chatty) {
                    if (r2 == null) {
                        r2 = new StringBuilder(256);
                    } else {
                        r2.append(' ');
                    }
                    r2.append(name);
                }
            }
        }
        StringBuilder r3 = null;
        if (pkg.getSdkLibName() != null && this.mSharedLibraries.removeSharedLibraryLPw(pkg.getSdkLibName(), pkg.getSdkLibVersionMajor()) && PackageManagerService.DEBUG_REMOVE && chatty) {
            if (0 == 0) {
                r3 = new StringBuilder(256);
            } else {
                r3.append(' ');
            }
            r3.append(pkg.getSdkLibName());
        }
        if (pkg.getStaticSharedLibName() != null && this.mSharedLibraries.removeSharedLibraryLPw(pkg.getStaticSharedLibName(), pkg.getStaticSharedLibVersion()) && PackageManagerService.DEBUG_REMOVE && chatty) {
            if (r3 == null) {
                r3 = new StringBuilder(256);
            } else {
                r3.append(' ');
            }
            r3.append(pkg.getStaticSharedLibName());
        }
        if (r3 != null && PackageManagerService.DEBUG_REMOVE) {
            Log.d("PackageManager", "  Libraries: " + ((Object) r3));
        }
    }

    public void removePackageDataLIF(PackageSetting deletedPs, int[] allUserHandles, PackageRemovedInfo outInfo, int flags, boolean writeSettings) {
        PackageManagerTracedLock packageManagerTracedLock;
        int removedAppId;
        AndroidPackage resolvedPkg;
        String packageName = deletedPs.getPackageName();
        if (PackageManagerService.DEBUG_REMOVE) {
            Slog.d("PackageManager", "removePackageDataLI: " + deletedPs);
        }
        AndroidPackage deletedPkg = deletedPs.getPkg();
        if (outInfo != null) {
            outInfo.mRemovedPackage = packageName;
            outInfo.mInstallerPackageName = deletedPs.getInstallSource().installerPackageName;
            outInfo.mIsStaticSharedLib = (deletedPkg == null || deletedPkg.getStaticSharedLibName() == null) ? false : true;
            outInfo.populateUsers(deletedPs.queryInstalledUsers(this.mUserManagerInternal.getUserIds(), true), deletedPs);
            outInfo.mIsExternal = deletedPs.isExternalStorage();
        }
        removePackageLI(deletedPs.getPackageName(), (flags & Integer.MIN_VALUE) != 0);
        if ((flags & 1) == 0) {
            if (deletedPkg != null) {
                resolvedPkg = deletedPkg;
            } else {
                resolvedPkg = PackageImpl.buildFakeForDeletion(deletedPs.getPackageName(), deletedPs.getVolumeUuid());
            }
            this.mAppDataHelper.destroyAppDataLIF(resolvedPkg, -1, 7);
            this.mAppDataHelper.destroyAppProfilesLIF(resolvedPkg);
            if (outInfo != null) {
                outInfo.mDataRemoved = true;
            }
        }
        int removedAppId2 = -1;
        boolean installedStateChanged = false;
        if ((flags & 1) == 0) {
            SparseBooleanArray changedUsers = new SparseBooleanArray();
            PackageManagerTracedLock packageManagerTracedLock2 = this.mPm.mLock;
            synchronized (packageManagerTracedLock2) {
                try {
                    this.mPm.mDomainVerificationManager.clearPackage(deletedPs.getPackageName());
                    this.mPm.mSettings.getKeySetManagerService().removeAppKeySetDataLPw(packageName);
                    Computer snapshot = this.mPm.snapshotComputer();
                    this.mPm.mAppsFilter.removePackage(snapshot, snapshot.getPackageStateInternal(packageName), false);
                    int removedAppId3 = this.mPm.mSettings.removePackageLPw(packageName);
                    if (outInfo != null) {
                        try {
                            outInfo.mRemovedAppId = removedAppId3;
                        } catch (Throwable th) {
                            th = th;
                            packageManagerTracedLock = packageManagerTracedLock2;
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
                    try {
                        if (!this.mPm.mSettings.isDisabledSystemPackageLPr(packageName) || PackageManagerService.sPmsExt.isRemovableSysApp(packageName)) {
                            SharedUserSetting sus = this.mPm.mSettings.getSharedUserSettingLPr(deletedPs);
                            List<AndroidPackage> sharedUserPkgs = sus != null ? sus.getPackages() : Collections.emptyList();
                            removedAppId = removedAppId3;
                            packageManagerTracedLock = packageManagerTracedLock2;
                            try {
                                this.mPermissionManager.onPackageUninstalled(packageName, deletedPs.getAppId(), deletedPkg, sharedUserPkgs, -1);
                                if (sus != null) {
                                    this.mPm.mSettings.checkAndConvertSharedUserSettingsLPw(sus);
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                while (true) {
                                    break;
                                    break;
                                }
                                throw th;
                            }
                        } else {
                            removedAppId = removedAppId3;
                            packageManagerTracedLock = packageManagerTracedLock2;
                        }
                        this.mPm.clearPackagePreferredActivitiesLPw(deletedPs.getPackageName(), changedUsers, -1);
                        this.mPm.mSettings.removeRenamedPackageLPw(deletedPs.getRealName());
                        if (changedUsers.size() > 0) {
                            PreferredActivityHelper preferredActivityHelper = new PreferredActivityHelper(this.mPm);
                            preferredActivityHelper.updateDefaultHomeNotLocked(this.mPm.snapshotComputer(), changedUsers);
                            this.mPm.postPreferredActivityChangedBroadcast(-1);
                        }
                        removedAppId2 = removedAppId;
                    } catch (Throwable th4) {
                        th = th4;
                        packageManagerTracedLock = packageManagerTracedLock2;
                    }
                } catch (Throwable th5) {
                    th = th5;
                    packageManagerTracedLock = packageManagerTracedLock2;
                }
            }
        }
        if (outInfo != null && outInfo.mOrigUsers != null) {
            if (PackageManagerService.DEBUG_REMOVE) {
                Slog.d("PackageManager", "Propagating install state across downgrade");
            }
            for (int userId : allUserHandles) {
                boolean installed = ArrayUtils.contains(outInfo.mOrigUsers, userId);
                if (PackageManagerService.DEBUG_REMOVE) {
                    Slog.d("PackageManager", "    user " + userId + " => " + installed);
                }
                if (installed != deletedPs.getInstalled(userId)) {
                    installedStateChanged = true;
                }
                deletedPs.setInstalled(installed, userId);
                if (installed) {
                    deletedPs.setUninstallReason(0, userId);
                }
            }
        }
        synchronized (this.mPm.mLock) {
            if (writeSettings) {
                try {
                    this.mPm.writeSettingsLPrTEMP();
                } catch (Throwable th6) {
                    throw th6;
                }
            }
            if (installedStateChanged) {
                this.mPm.mSettings.writeKernelMappingLPr(deletedPs);
            }
        }
        if (removedAppId2 != -1) {
            final int appIdToRemove = removedAppId2;
            this.mPm.mInjector.getBackgroundHandler().post(new Runnable() { // from class: com.android.server.pm.RemovePackageHelper$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    RemovePackageHelper.this.m5603x77f8e9c2(appIdToRemove);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$removePackageDataLIF$0$com-android-server-pm-RemovePackageHelper  reason: not valid java name */
    public /* synthetic */ void m5603x77f8e9c2(int appIdToRemove) {
        try {
            Trace.traceBegin(262144L, "clearKeystoreData:" + appIdToRemove);
            this.mAppDataHelper.clearKeystoreData(-1, appIdToRemove);
        } finally {
            Trace.traceEnd(262144L);
        }
    }
}
