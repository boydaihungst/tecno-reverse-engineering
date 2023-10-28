package com.android.server.pm;

import android.app.ApplicationPackageManager;
import android.content.Intent;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.PackageChangeEvent;
import android.content.pm.SharedLibraryInfo;
import android.content.pm.VersionedPackage;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.PackageUserState;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.mediatek.server.pm.hbtpackage.HBTPackage;
import com.transsion.hubcore.server.pm.ITranPackageManagerService;
import dalvik.system.VMRuntime;
import java.util.Collections;
import java.util.List;
/* loaded from: classes2.dex */
public final class DeletePackageHelper {
    private static final boolean DEBUG_CLEAN_APKS = false;
    private static final boolean DEBUG_SD_INSTALL = false;
    private final AppDataHelper mAppDataHelper;
    private final PermissionManagerServiceInternal mPermissionManager;
    private final PackageManagerService mPm;
    private final RemovePackageHelper mRemovePackageHelper;
    private final UserManagerInternal mUserManagerInternal;

    /* JADX INFO: Access modifiers changed from: package-private */
    public DeletePackageHelper(PackageManagerService pm, RemovePackageHelper removePackageHelper, AppDataHelper appDataHelper) {
        this.mPm = pm;
        this.mUserManagerInternal = pm.mInjector.getUserManagerInternal();
        this.mPermissionManager = pm.mInjector.getPermissionManagerServiceInternal();
        this.mRemovePackageHelper = removePackageHelper;
        this.mAppDataHelper = appDataHelper;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DeletePackageHelper(PackageManagerService pm) {
        this.mPm = pm;
        AppDataHelper appDataHelper = new AppDataHelper(pm);
        this.mAppDataHelper = appDataHelper;
        this.mUserManagerInternal = pm.mInjector.getUserManagerInternal();
        this.mPermissionManager = pm.mInjector.getPermissionManagerServiceInternal();
        this.mRemovePackageHelper = new RemovePackageHelper(pm, appDataHelper);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [278=8, 235=5] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:314:0x0361 */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:318:0x024e A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Type inference failed for: r28v0, types: [com.android.server.pm.DeletePackageHelper] */
    /* JADX WARN: Type inference failed for: r5v1, types: [int[]] */
    /* JADX WARN: Type inference failed for: r5v21 */
    /* JADX WARN: Type inference failed for: r5v23, types: [com.android.server.pm.PackageRemovedInfo] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public int deletePackageX(String packageName, long versionCode, int userId, int deleteFlags, boolean removedBySystem) {
        int[] allUsers;
        AndroidPackage pkg;
        PackageSetting uninstalledPs;
        PackageSetting uninstalledPs2;
        PackageRemovedInfo info;
        int freezeUser;
        SparseArray<TempUserState> priorUserStates;
        Object obj;
        Object obj2;
        Throwable th;
        AndroidPackage pkg2;
        PackageRemovedInfo info2;
        int i;
        PackageSetting stubPs;
        boolean packageInstalledForSomeUsers;
        SharedLibraryInfo libraryInfo;
        int[] allUsers2;
        int i2;
        AndroidPackage pkg3;
        int i3;
        PackageSetting uninstalledPs3;
        PackageRemovedInfo info3 = new PackageRemovedInfo(this.mPm);
        int i4 = -1;
        int removeUser = (deleteFlags & 2) != 0 ? -1 : userId;
        if (this.mPm.isPackageDeviceAdmin(packageName, removeUser)) {
            Slog.w("PackageManager", "Not removing package " + packageName + ": has active device admin");
            return -2;
        }
        synchronized (this.mPm.mLock) {
            try {
                Computer computer = this.mPm.snapshotComputer();
                PackageSetting uninstalledPs4 = this.mPm.mSettings.getPackageLPr(packageName);
                try {
                    if (uninstalledPs4 == null) {
                        Slog.w("PackageManager", "Not removing non-existent package " + packageName);
                        return -1;
                    } else if (versionCode != -1 && uninstalledPs4.getVersionCode() != versionCode) {
                        Slog.w("PackageManager", "Not removing package " + packageName + " with versionCode " + uninstalledPs4.getVersionCode() + " != " + versionCode);
                        return -1;
                    } else {
                        PackageSetting disabledSystemPs = this.mPm.mSettings.getDisabledSystemPkgLPr(packageName);
                        AndroidPackage pkg4 = this.mPm.mPackages.get(packageName);
                        int[] allUsers3 = this.mUserManagerInternal.getUserIds();
                        boolean pkgEnabled = false;
                        if (pkg4 != null) {
                            if (pkg4.getStaticSharedLibName() != null) {
                                SharedLibraryInfo libraryInfo2 = computer.getSharedLibraryInfo(pkg4.getStaticSharedLibName(), pkg4.getStaticSharedLibVersion());
                                libraryInfo = libraryInfo2;
                            } else if (pkg4.getSdkLibName() != null) {
                                SharedLibraryInfo libraryInfo3 = computer.getSharedLibraryInfo(pkg4.getSdkLibName(), pkg4.getSdkLibVersionMajor());
                                libraryInfo = libraryInfo3;
                            } else {
                                libraryInfo = null;
                            }
                            if (libraryInfo != null) {
                                int length = allUsers3.length;
                                int i5 = 0;
                                while (i5 < length) {
                                    int currUserId = allUsers3[i5];
                                    if (removeUser == i4 || removeUser == currUserId) {
                                        allUsers2 = allUsers3;
                                        i2 = i5;
                                        pkg3 = pkg4;
                                        i3 = length;
                                        uninstalledPs3 = uninstalledPs4;
                                        List<VersionedPackage> libClientPackages = computer.getPackagesUsingSharedLibrary(libraryInfo, 4202496L, 1000, currUserId);
                                        if (!ArrayUtils.isEmpty(libClientPackages)) {
                                            Slog.w("PackageManager", "Not removing package " + pkg3.getManifestPackageName() + " hosting lib " + libraryInfo.getName() + " version " + libraryInfo.getLongVersion() + " used by " + libClientPackages + " for user " + currUserId);
                                            return -6;
                                        }
                                    } else {
                                        allUsers2 = allUsers3;
                                        i2 = i5;
                                        pkg3 = pkg4;
                                        i3 = length;
                                        uninstalledPs3 = uninstalledPs4;
                                    }
                                    i5 = i2 + 1;
                                    uninstalledPs4 = uninstalledPs3;
                                    allUsers3 = allUsers2;
                                    pkg4 = pkg3;
                                    length = i3;
                                    i4 = -1;
                                }
                                allUsers = allUsers3;
                                pkg = pkg4;
                                uninstalledPs = uninstalledPs4;
                            } else {
                                allUsers = allUsers3;
                                pkg = pkg4;
                                uninstalledPs = uninstalledPs4;
                            }
                        } else {
                            allUsers = allUsers3;
                            pkg = pkg4;
                            uninstalledPs = uninstalledPs4;
                        }
                        info3.mOrigUsers = uninstalledPs.queryInstalledUsers(allUsers, true);
                        int deleteFlags2 = (PackageManagerService.sPmsExt.isRemovableSysApp(packageName) && PackageManagerServiceUtils.isSystemApp(uninstalledPs)) ? deleteFlags | 4 : deleteFlags;
                        try {
                            try {
                                if (PackageManagerServiceUtils.isUpdatedSystemApp(uninstalledPs) && (deleteFlags2 & 4) == 0) {
                                    try {
                                        if (PackageManagerService.sPmsExt.isRemovableSysApp(packageName)) {
                                            uninstalledPs2 = uninstalledPs;
                                            info = info3;
                                        } else {
                                            int freezeUser2 = -1;
                                            SparseArray<TempUserState> priorUserStates2 = new SparseArray<>();
                                            int i6 = 0;
                                            while (i6 < allUsers.length) {
                                                PackageUserState userState = uninstalledPs.readUserState(allUsers[i6]);
                                                Computer computer2 = computer;
                                                int freezeUser3 = freezeUser2;
                                                PackageSetting uninstalledPs5 = uninstalledPs;
                                                PackageRemovedInfo info4 = info3;
                                                try {
                                                    priorUserStates2.put(allUsers[i6], new TempUserState(userState.getEnabledState(), userState.getLastDisableAppCaller(), userState.isInstalled()));
                                                    i6++;
                                                    computer = computer2;
                                                    freezeUser2 = freezeUser3;
                                                    uninstalledPs = uninstalledPs5;
                                                    info3 = info4;
                                                } catch (Throwable th2) {
                                                    th = th2;
                                                    while (true) {
                                                        try {
                                                            break;
                                                        } catch (Throwable th3) {
                                                            th = th3;
                                                        }
                                                    }
                                                    throw th;
                                                }
                                            }
                                            uninstalledPs2 = uninstalledPs;
                                            info = info3;
                                            priorUserStates = priorUserStates2;
                                            freezeUser = freezeUser2;
                                            boolean isInstallerPackage = this.mPm.mSettings.isInstallerPackage(packageName);
                                            obj = this.mPm.mInstallLock;
                                            synchronized (obj) {
                                                try {
                                                    try {
                                                        boolean isDisablePackage = this.mPm.mSettings.isDisabledSystemPackageLPr(packageName);
                                                        if (PackageManagerService.DEBUG_REMOVE | Build.IS_DEBUG_ENABLE) {
                                                            try {
                                                                Slog.d("PackageManager", "deletePackageX: pkg=" + packageName + " user=" + userId + " isDisablePackage=" + isDisablePackage);
                                                            } catch (Throwable th4) {
                                                                th = th4;
                                                                obj2 = obj;
                                                                throw th;
                                                            }
                                                        }
                                                        PackageFreezer freezer = this.mPm.freezePackageForDelete(packageName, freezeUser, deleteFlags2, "deletePackageX");
                                                        try {
                                                            ?? r5 = allUsers;
                                                            obj2 = obj;
                                                            int deleteFlags3 = deleteFlags2;
                                                            try {
                                                                boolean res = deletePackageLIF(packageName, UserHandle.of(removeUser), true, r5, Integer.MIN_VALUE | deleteFlags2, info, true);
                                                                if (freezer != null) {
                                                                    try {
                                                                        freezer.close();
                                                                    } catch (Throwable th5) {
                                                                        th = th5;
                                                                        throw th;
                                                                    }
                                                                }
                                                                PackageSetting uninstalledPs6 = uninstalledPs2;
                                                                try {
                                                                    if (uninstalledPs6.pkg != null && res) {
                                                                        try {
                                                                            if (ITranPackageManagerService.Instance().isPreloadApp(packageName, uninstalledPs6.pkg.getBaseApkPath())) {
                                                                                if (uninstalledPs6.isSystem() && !PackageManagerService.sPmsExt.isRemovableSysApp(packageName)) {
                                                                                    Slog.d("PackageManager", "System App and not Removable, ignore it! packageName =  " + packageName);
                                                                                } else if (UserHandle.myUserId() == userId) {
                                                                                    ITranPackageManagerService.Instance().storeRemovedPackage(packageName, deleteFlags3);
                                                                                }
                                                                            } else if (isDisablePackage && uninstalledPs6.pkg.getBaseApkPath() != null && uninstalledPs6.pkg.getBaseApkPath().startsWith("/data") && PackageManagerService.sPmsExt.isRemovableSysApp(packageName) && UserHandle.myUserId() == userId) {
                                                                                ITranPackageManagerService.Instance().storeRemovedPackage(packageName, deleteFlags3);
                                                                            }
                                                                        } catch (Throwable th6) {
                                                                            th = th6;
                                                                            throw th;
                                                                        }
                                                                    }
                                                                    if (res) {
                                                                        pkg2 = pkg;
                                                                        if (pkg2 != null) {
                                                                            try {
                                                                                synchronized (this.mPm.mLock) {
                                                                                    try {
                                                                                        packageInstalledForSomeUsers = this.mPm.mPackages.get(pkg2.getPackageName()) != null;
                                                                                    } catch (Throwable th7) {
                                                                                        th = th7;
                                                                                        throw th;
                                                                                    }
                                                                                }
                                                                                r5 = info;
                                                                                this.mPm.mInstantAppRegistry.onPackageUninstalled(pkg2, uninstalledPs6, r5.mRemovedUsers, packageInstalledForSomeUsers);
                                                                                info2 = r5;
                                                                            } catch (Throwable th8) {
                                                                                th = th8;
                                                                            }
                                                                        } else {
                                                                            info2 = info;
                                                                        }
                                                                    } else {
                                                                        pkg2 = pkg;
                                                                        info2 = info;
                                                                    }
                                                                    try {
                                                                        synchronized (this.mPm.mLock) {
                                                                            if (res) {
                                                                                try {
                                                                                    this.mPm.updateSequenceNumberLP(uninstalledPs6, info2.mRemovedUsers);
                                                                                    this.mPm.updateInstantAppInstallerLocked(packageName);
                                                                                } catch (Throwable th9) {
                                                                                    th = th9;
                                                                                    while (true) {
                                                                                        try {
                                                                                            break;
                                                                                        } catch (Throwable th10) {
                                                                                            th = th10;
                                                                                        }
                                                                                    }
                                                                                    throw th;
                                                                                }
                                                                            }
                                                                            try {
                                                                                ApplicationPackageManager.invalidateGetPackagesForUidCache();
                                                                                if (res) {
                                                                                    boolean killApp = (deleteFlags3 & 8) == 0;
                                                                                    info2.sendPackageRemovedBroadcasts(killApp, removedBySystem);
                                                                                    info2.sendSystemPackageUpdatedBroadcasts();
                                                                                }
                                                                                VMRuntime.getRuntime().requestConcurrentGC();
                                                                                synchronized (this.mPm.mInstallLock) {
                                                                                    try {
                                                                                        try {
                                                                                            if (info2.mArgs != null) {
                                                                                                try {
                                                                                                    info2.mArgs.doPostDeleteLI(true);
                                                                                                } catch (Throwable th11) {
                                                                                                    th = th11;
                                                                                                    throw th;
                                                                                                }
                                                                                            }
                                                                                            boolean reEnableStub = false;
                                                                                            if (priorUserStates != null) {
                                                                                                try {
                                                                                                    synchronized (this.mPm.mLock) {
                                                                                                        try {
                                                                                                            PackageSetting pkgSetting = this.mPm.getPackageSettingForMutation(packageName);
                                                                                                            if (pkgSetting != null) {
                                                                                                                AndroidPackage aPkg = pkgSetting.getPkg();
                                                                                                                if (aPkg != null) {
                                                                                                                    try {
                                                                                                                        if (aPkg.isEnabled()) {
                                                                                                                            pkgEnabled = true;
                                                                                                                        }
                                                                                                                    } catch (Throwable th12) {
                                                                                                                        th = th12;
                                                                                                                        throw th;
                                                                                                                    }
                                                                                                                }
                                                                                                                int i7 = 0;
                                                                                                                PackageRemovedInfo info5 = info2;
                                                                                                                while (true) {
                                                                                                                    PackageRemovedInfo info6 = info5;
                                                                                                                    try {
                                                                                                                        if (i7 >= allUsers.length) {
                                                                                                                            break;
                                                                                                                        }
                                                                                                                        TempUserState priorUserState = priorUserStates.get(allUsers[i7]);
                                                                                                                        int enabledState = priorUserState.enabledState;
                                                                                                                        SparseArray<TempUserState> priorUserStates3 = priorUserStates;
                                                                                                                        pkgSetting.setEnabled(enabledState, allUsers[i7], priorUserState.lastDisableAppCaller);
                                                                                                                        if (!reEnableStub && priorUserState.installed) {
                                                                                                                            if (enabledState == 0 && pkgEnabled) {
                                                                                                                                reEnableStub = true;
                                                                                                                            }
                                                                                                                            if (enabledState != 1) {
                                                                                                                            }
                                                                                                                            reEnableStub = true;
                                                                                                                        }
                                                                                                                        i7++;
                                                                                                                        info5 = info6;
                                                                                                                        priorUserStates = priorUserStates3;
                                                                                                                    } catch (Throwable th13) {
                                                                                                                        th = th13;
                                                                                                                        throw th;
                                                                                                                    }
                                                                                                                }
                                                                                                                i = 1;
                                                                                                            } else {
                                                                                                                i = 1;
                                                                                                                Slog.w("PackageManager", "Missing PackageSetting after uninstalling the update for system app: " + packageName + ". This should not happen.");
                                                                                                            }
                                                                                                            this.mPm.mSettings.writeAllUsersPackageRestrictionsLPr();
                                                                                                        } catch (Throwable th14) {
                                                                                                            th = th14;
                                                                                                        }
                                                                                                    }
                                                                                                } catch (Throwable th15) {
                                                                                                    th = th15;
                                                                                                }
                                                                                            } else {
                                                                                                i = 1;
                                                                                            }
                                                                                            AndroidPackage stubPkg = disabledSystemPs == null ? null : disabledSystemPs.getPkg();
                                                                                            if (stubPkg != null && stubPkg.isStub()) {
                                                                                                synchronized (this.mPm.mLock) {
                                                                                                    stubPs = this.mPm.mSettings.getPackageLPr(stubPkg.getPackageName());
                                                                                                }
                                                                                                if (stubPs != null) {
                                                                                                    if (reEnableStub) {
                                                                                                        if (PackageManagerService.DEBUG_COMPRESSION) {
                                                                                                            Slog.i("PackageManager", "Enabling system stub after removal; pkg: " + stubPkg.getPackageName());
                                                                                                        }
                                                                                                        new InstallPackageHelper(this.mPm).enableCompressedPackage(stubPkg, stubPs);
                                                                                                    } else if (PackageManagerService.DEBUG_COMPRESSION) {
                                                                                                        Slog.i("PackageManager", "System stub disabled for all users, leaving uncompressed after removal; pkg: " + stubPkg.getPackageName());
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                            if (res && isInstallerPackage) {
                                                                                                PackageInstallerService packageInstallerService = this.mPm.mInjector.getPackageInstallerService();
                                                                                                packageInstallerService.onInstallerPackageDeleted(uninstalledPs6.getAppId(), removeUser);
                                                                                            }
                                                                                            if (res) {
                                                                                                return i;
                                                                                            }
                                                                                            return -1;
                                                                                        } catch (Throwable th16) {
                                                                                            th = th16;
                                                                                        }
                                                                                    } catch (Throwable th17) {
                                                                                        th = th17;
                                                                                    }
                                                                                }
                                                                            } catch (Throwable th18) {
                                                                                th = th18;
                                                                                while (true) {
                                                                                    break;
                                                                                    break;
                                                                                }
                                                                                throw th;
                                                                            }
                                                                        }
                                                                    } catch (Throwable th19) {
                                                                        th = th19;
                                                                        throw th;
                                                                    }
                                                                } catch (Throwable th20) {
                                                                    th = th20;
                                                                }
                                                            } catch (Throwable th21) {
                                                                th = th21;
                                                                if (freezer != null) {
                                                                    freezer.close();
                                                                }
                                                                throw th;
                                                            }
                                                        } catch (Throwable th22) {
                                                            th = th22;
                                                        }
                                                    } catch (Throwable th23) {
                                                        th = th23;
                                                        obj2 = obj;
                                                    }
                                                } catch (Throwable th24) {
                                                    th = th24;
                                                }
                                            }
                                        }
                                    } catch (Throwable th25) {
                                        th = th25;
                                    }
                                } else {
                                    uninstalledPs2 = uninstalledPs;
                                    info = info3;
                                }
                                boolean isInstallerPackage2 = this.mPm.mSettings.isInstallerPackage(packageName);
                                obj = this.mPm.mInstallLock;
                                synchronized (obj) {
                                }
                            } catch (Throwable th26) {
                                th = th26;
                                while (true) {
                                    break;
                                    break;
                                }
                                throw th;
                            }
                            freezeUser = removeUser;
                            priorUserStates = null;
                        } catch (Throwable th27) {
                            th = th27;
                        }
                    }
                } catch (Throwable th28) {
                    th = th28;
                }
            } catch (Throwable th29) {
                th = th29;
            }
        }
    }

    public boolean deletePackageLIF(String packageName, UserHandle user, boolean deleteCodeAndResources, int[] allUserHandles, int flags, PackageRemovedInfo outInfo, boolean writeSettings) {
        synchronized (this.mPm.mLock) {
            try {
                try {
                    PackageSetting ps = this.mPm.mSettings.getPackageLPr(packageName);
                    PackageSetting disabledPs = this.mPm.mSettings.getDisabledSystemPkgLPr(ps);
                    HBTPackage.HBTcheckUninstall(packageName, InstructionSets.getAppDexInstructionSets(ps.getPrimaryCpuAbi(), ps.getSecondaryCpuAbi()));
                    DeletePackageAction action = mayDeletePackageLocked(outInfo, ps, disabledPs, flags, user);
                    if (PackageManagerService.DEBUG_REMOVE) {
                        Slog.d("PackageManager", "deletePackageLI: " + packageName + " user " + user);
                    }
                    if (action == null) {
                        if (PackageManagerService.DEBUG_REMOVE) {
                            Slog.d("PackageManager", "deletePackageLI: action was null");
                        }
                        return false;
                    }
                    try {
                        executeDeletePackageLIF(action, packageName, deleteCodeAndResources, allUserHandles, writeSettings);
                        return true;
                    } catch (SystemDeleteException e) {
                        if (PackageManagerService.DEBUG_REMOVE) {
                            Slog.d("PackageManager", "deletePackageLI: system deletion failure", e);
                        }
                        return false;
                    }
                } catch (Throwable th) {
                    e = th;
                    throw e;
                }
            } catch (Throwable th2) {
                e = th2;
            }
        }
    }

    public static DeletePackageAction mayDeletePackageLocked(PackageRemovedInfo outInfo, PackageSetting ps, PackageSetting disabledPs, int flags, UserHandle user) {
        if (ps == null) {
            return null;
        }
        if (PackageManagerServiceUtils.isSystemApp(ps)) {
            boolean deleteAllUsers = true;
            boolean deleteSystem = (flags & 4) != 0;
            if (user != null && user.getIdentifier() != -1) {
                deleteAllUsers = false;
            }
            if (!PackageManagerService.sPmsExt.isRemovableSysApp(ps.getPackageName()) && ((!deleteSystem || deleteAllUsers) && disabledPs == null)) {
                Slog.w("PackageManager", "Attempt to delete removable system package " + ps.pkg.getPackageName());
                return null;
            }
        }
        return new DeletePackageAction(ps, disabledPs, outInfo, flags, user);
    }

    public void executeDeletePackageLIF(DeletePackageAction action, String packageName, boolean deleteCodeAndResources, int[] allUserHandles, boolean writeSettings) throws SystemDeleteException {
        boolean clearPackageStateAndReturn;
        int userId;
        boolean z;
        SparseBooleanArray hadSuspendAppsPermission;
        PackageSetting ps = action.mDeletingPs;
        PackageRemovedInfo outInfo = action.mRemovedInfo;
        UserHandle user = action.mUser;
        int flags = action.mFlags;
        boolean systemApp = PackageManagerServiceUtils.isSystemApp(ps);
        SparseBooleanArray hadSuspendAppsPermission2 = new SparseBooleanArray();
        int length = allUserHandles.length;
        int i = 0;
        while (true) {
            boolean z2 = true;
            if (i >= length) {
                break;
            }
            int userId2 = allUserHandles[i];
            if (this.mPm.checkPermission("android.permission.SUSPEND_APPS", packageName, userId2) != 0) {
                z2 = false;
            }
            hadSuspendAppsPermission2.put(userId2, z2);
            i++;
        }
        int userId3 = user == null ? -1 : user.getIdentifier();
        if ((!systemApp || (flags & 4) != 0) && userId3 != -1) {
            synchronized (this.mPm.mLock) {
                markPackageUninstalledForUserLPw(ps, user);
                if (systemApp && !PackageManagerService.sPmsExt.isRemovableSysApp(packageName)) {
                    if (PackageManagerService.DEBUG_REMOVE) {
                        Slog.d("PackageManager", "Deleting system app");
                    }
                    clearPackageStateAndReturn = true;
                }
                boolean keepUninstalledPackage = this.mPm.shouldKeepUninstalledPackageLPr(packageName);
                if (!ps.isAnyInstalled(this.mUserManagerInternal.getUserIds()) && !keepUninstalledPackage) {
                    if (PackageManagerService.DEBUG_REMOVE) {
                        Slog.d("PackageManager", "Not installed by other users, full delete");
                    }
                    ps.setInstalled(true, userId3);
                    this.mPm.mSettings.writeKernelMappingLPr(ps);
                    clearPackageStateAndReturn = false;
                }
                boolean clearPackageStateAndReturn2 = PackageManagerService.DEBUG_REMOVE;
                if (clearPackageStateAndReturn2) {
                    Slog.d("PackageManager", "Still installed by other users");
                }
                clearPackageStateAndReturn = true;
            }
            if (clearPackageStateAndReturn) {
                clearPackageStateForUserLIF(ps, userId3, outInfo, flags);
                this.mPm.scheduleWritePackageRestrictions(user);
                return;
            }
        }
        if (systemApp && !PackageManagerService.sPmsExt.isRemovableSysApp(packageName)) {
            if (PackageManagerService.DEBUG_REMOVE) {
                Slog.d("PackageManager", "Removing system package: " + ps.getPackageName());
            }
            deleteInstalledSystemPackage(action, allUserHandles, writeSettings);
            new InstallPackageHelper(this.mPm).restoreDisabledSystemPackageLIF(action, allUserHandles, writeSettings);
            userId = userId3;
            z = true;
            hadSuspendAppsPermission = hadSuspendAppsPermission2;
        } else {
            if (PackageManagerService.DEBUG_REMOVE) {
                Slog.d("PackageManager", "Removing non-system package: " + ps.getPackageName());
            }
            userId = userId3;
            z = true;
            hadSuspendAppsPermission = hadSuspendAppsPermission2;
            deleteInstalledPackageLIF(ps, deleteCodeAndResources, flags, allUserHandles, outInfo, writeSettings);
        }
        int[] affectedUserIds = outInfo != null ? outInfo.mRemovedUsers : null;
        if (affectedUserIds == null) {
            affectedUserIds = this.mPm.resolveUserIds(userId);
        }
        Computer snapshot = this.mPm.snapshotComputer();
        for (int affectedUserId : affectedUserIds) {
            if (hadSuspendAppsPermission.get(affectedUserId)) {
                this.mPm.unsuspendForSuspendingPackage(snapshot, packageName, affectedUserId);
                this.mPm.removeAllDistractingPackageRestrictions(snapshot, affectedUserId);
            }
        }
        if (outInfo != null) {
            if (this.mPm.mPackages.get(ps.getPackageName()) != null) {
                z = false;
            }
            outInfo.mRemovedForAllUsers = z;
        }
    }

    private void clearPackageStateForUserLIF(PackageSetting ps, int userId, PackageRemovedInfo outInfo, int flags) {
        synchronized (this.mPm.mLock) {
            try {
                try {
                    AndroidPackage pkg = this.mPm.mPackages.get(ps.getPackageName());
                    SharedUserSetting sus = this.mPm.mSettings.getSharedUserSettingLPr(ps);
                    this.mAppDataHelper.destroyAppProfilesLIF(pkg);
                    List<AndroidPackage> sharedUserPkgs = sus != null ? sus.getPackages() : Collections.emptyList();
                    PreferredActivityHelper preferredActivityHelper = new PreferredActivityHelper(this.mPm);
                    int[] userIds = userId == -1 ? this.mUserManagerInternal.getUserIds() : new int[]{userId};
                    for (int nextUserId : userIds) {
                        if (PackageManagerService.DEBUG_REMOVE) {
                            Slog.d("PackageManager", "Updating package:" + ps.getPackageName() + " install state for user:" + nextUserId);
                        }
                        if ((flags & 1) == 0) {
                            this.mAppDataHelper.destroyAppDataLIF(pkg, nextUserId, 7);
                        }
                        this.mAppDataHelper.clearKeystoreData(nextUserId, ps.getAppId());
                        preferredActivityHelper.clearPackagePreferredActivities(ps.getPackageName(), nextUserId);
                        this.mPm.mDomainVerificationManager.clearPackageForUser(ps.getPackageName(), nextUserId);
                    }
                    this.mPermissionManager.onPackageUninstalled(ps.getPackageName(), ps.getAppId(), pkg, sharedUserPkgs, userId);
                    if (outInfo != null) {
                        if ((flags & 1) == 0) {
                            outInfo.mDataRemoved = true;
                        }
                        outInfo.mRemovedPackage = ps.getPackageName();
                        outInfo.mInstallerPackageName = ps.getInstallSource().installerPackageName;
                        outInfo.mIsStaticSharedLib = (pkg == null || pkg.getStaticSharedLibName() == null) ? false : true;
                        outInfo.mRemovedAppId = ps.getAppId();
                        outInfo.mRemovedUsers = userIds;
                        outInfo.mBroadcastUsers = userIds;
                        outInfo.mIsExternal = ps.isExternalStorage();
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    private void deleteInstalledPackageLIF(PackageSetting ps, boolean deleteCodeAndResources, int flags, int[] allUserHandles, PackageRemovedInfo outInfo, boolean writeSettings) {
        synchronized (this.mPm.mLock) {
            if (outInfo != null) {
                outInfo.mUid = ps.getAppId();
                outInfo.mBroadcastAllowList = this.mPm.mAppsFilter.getVisibilityAllowList(this.mPm.snapshotComputer(), ps, allUserHandles, this.mPm.mSettings.getPackagesLocked());
            }
        }
        this.mRemovePackageHelper.removePackageDataLIF(ps, allUserHandles, outInfo, flags, writeSettings);
        if (deleteCodeAndResources && outInfo != null) {
            outInfo.mArgs = new FileInstallArgs(ps.getPathString(), InstructionSets.getAppDexInstructionSets(ps.getPrimaryCpuAbi(), ps.getSecondaryCpuAbi()), this.mPm);
        }
    }

    private void markPackageUninstalledForUserLPw(PackageSetting ps, UserHandle user) {
        int[] userIds = (user == null || user.getIdentifier() == -1) ? this.mUserManagerInternal.getUserIds() : new int[]{user.getIdentifier()};
        for (int nextUserId : userIds) {
            if (PackageManagerService.DEBUG_REMOVE) {
                Slog.d("PackageManager", "Marking package:" + ps.getPackageName() + " uninstalled for user:" + nextUserId);
            }
            ps.setUserState(nextUserId, 0L, 0, false, true, true, false, 0, null, false, false, null, null, null, 0, 0, null, null, 0L);
        }
        this.mPm.mSettings.writeKernelMappingLPr(ps);
    }

    private void deleteInstalledSystemPackage(DeletePackageAction action, int[] allUserHandles, boolean writeSettings) {
        int flags;
        int flags2 = action.mFlags;
        PackageSetting deletedPs = action.mDeletingPs;
        PackageRemovedInfo outInfo = action.mRemovedInfo;
        boolean applyUserRestrictions = (outInfo == null || outInfo.mOrigUsers == null) ? false : true;
        AndroidPackage deletedPkg = deletedPs.getPkg();
        PackageSetting disabledPs = action.mDisabledPs;
        if (PackageManagerService.DEBUG_REMOVE) {
            Slog.d("PackageManager", "deleteSystemPackageLI: newPs=" + deletedPkg.getPackageName() + " disabledPs=" + disabledPs);
        }
        Slog.d("PackageManager", "Deleting system pkg from data partition");
        if (PackageManagerService.DEBUG_REMOVE && applyUserRestrictions) {
            Slog.d("PackageManager", "Remembering install states:");
            for (int userId : allUserHandles) {
                boolean finstalled = ArrayUtils.contains(outInfo.mOrigUsers, userId);
                Slog.d("PackageManager", "   u=" + userId + " inst=" + finstalled);
            }
        }
        if (outInfo != null) {
            outInfo.mIsRemovedPackageSystemUpdate = true;
        }
        if (disabledPs.getVersionCode() < deletedPs.getVersionCode() || disabledPs.getAppId() != deletedPs.getAppId()) {
            flags = flags2 & (-2);
        } else {
            flags = flags2 | 1;
        }
        deleteInstalledPackageLIF(deletedPs, true, flags, allUserHandles, outInfo, writeSettings);
    }

    public void deletePackageVersionedInternal(VersionedPackage versionedPackage, final IPackageDeleteObserver2 observer, final int userId, int deleteFlagsTmp, boolean allowSilentUninstall) {
        final int callingUid = Binder.getCallingUid();
        this.mPm.mContext.enforceCallingOrSelfPermission("android.permission.DELETE_PACKAGES", null);
        Computer snapshot = this.mPm.snapshotComputer();
        final boolean canViewInstantApps = snapshot.canViewInstantApps(callingUid, userId);
        Preconditions.checkNotNull(versionedPackage);
        Preconditions.checkNotNull(observer);
        Preconditions.checkArgumentInRange(versionedPackage.getLongVersionCode(), -1L, (long) JobStatus.NO_LATEST_RUNTIME, "versionCode must be >= -1");
        final int deleteFlags = ITranPackageManagerService.Instance().deletePackageVersionedInternal(versionedPackage, observer, userId, deleteFlagsTmp, allowSilentUninstall);
        final String packageName = versionedPackage.getPackageName();
        final long versionCode = versionedPackage.getLongVersionCode();
        if (this.mPm.mProtectedPackages.isPackageDataProtected(userId, packageName)) {
            this.mPm.mHandler.post(new Runnable() { // from class: com.android.server.pm.DeletePackageHelper$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    DeletePackageHelper.lambda$deletePackageVersionedInternal$0(packageName, observer);
                }
            });
            return;
        }
        try {
            if (((ActivityTaskManagerInternal) this.mPm.mInjector.getLocalService(ActivityTaskManagerInternal.class)).isBaseOfLockedTask(packageName)) {
                observer.onPackageDeleted(packageName, -7, (String) null);
                EventLog.writeEvent(1397638484, "127605586", -1, "");
                return;
            }
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
        final String internalPackageName = snapshot.resolveInternalPackageName(packageName, versionCode);
        int uid = Binder.getCallingUid();
        if (!isOrphaned(snapshot, internalPackageName) && !allowSilentUninstall && !isCallerAllowedToSilentlyUninstall(snapshot, uid, internalPackageName)) {
            this.mPm.mHandler.post(new Runnable() { // from class: com.android.server.pm.DeletePackageHelper$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    DeletePackageHelper.lambda$deletePackageVersionedInternal$1(packageName, observer);
                }
            });
            return;
        }
        final boolean deleteAllUsers = (deleteFlags & 2) != 0;
        final int[] users = deleteAllUsers ? this.mUserManagerInternal.getUserIds() : new int[]{userId};
        if (UserHandle.getUserId(uid) != userId || (deleteAllUsers && users.length > 1)) {
            this.mPm.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "deletePackage for user " + userId);
        }
        if (this.mPm.isUserRestricted(userId, "no_uninstall_apps")) {
            this.mPm.mHandler.post(new Runnable() { // from class: com.android.server.pm.DeletePackageHelper$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    observer.onPackageDeleted(packageName, -3, (String) null);
                }
            });
        } else if (!deleteAllUsers && snapshot.getBlockUninstallForUser(internalPackageName, userId)) {
            this.mPm.mHandler.post(new Runnable() { // from class: com.android.server.pm.DeletePackageHelper$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    observer.onPackageDeleted(packageName, -4, (String) null);
                }
            });
        } else {
            if (PackageManagerService.DEBUG_REMOVE | Build.IS_DEBUG_ENABLE) {
                Slog.d("PackageManager", "deletePackageAsUser: pkg=" + internalPackageName + " user=" + userId + " deleteAllUsers: " + deleteAllUsers + " version=" + (versionCode == -1 ? "VERSION_CODE_HIGHEST" : Long.valueOf(versionCode)));
            }
            this.mPm.mHandler.post(new Runnable() { // from class: com.android.server.pm.DeletePackageHelper$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    DeletePackageHelper.this.m5407xcb89b83b(internalPackageName, callingUid, canViewInstantApps, deleteAllUsers, versionCode, userId, deleteFlags, users, packageName, observer);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$deletePackageVersionedInternal$0(String packageName, IPackageDeleteObserver2 observer) {
        try {
            Slog.w("PackageManager", "Attempted to delete protected package: " + packageName);
            observer.onPackageDeleted(packageName, -1, (String) null);
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$deletePackageVersionedInternal$1(String packageName, IPackageDeleteObserver2 observer) {
        try {
            Intent intent = new Intent("android.intent.action.UNINSTALL_PACKAGE");
            intent.setData(Uri.fromParts("package", packageName, null));
            intent.putExtra("android.content.pm.extra.CALLBACK", observer.asBinder());
            observer.onUserActionRequired(intent);
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$deletePackageVersionedInternal$4$com-android-server-pm-DeletePackageHelper  reason: not valid java name */
    public /* synthetic */ void m5407xcb89b83b(String internalPackageName, int callingUid, boolean canViewInstantApps, boolean deleteAllUsers, long versionCode, int userId, int deleteFlags, int[] users, String packageName, IPackageDeleteObserver2 observer) {
        boolean doDeletePackage;
        int returnCode;
        int i;
        int i2;
        int returnCode2;
        if (Build.IS_DEBUG_ENABLE) {
            Slog.d("PackageManager", "the method of post has been involved!");
        }
        Computer innerSnapshot = this.mPm.snapshotComputer();
        PackageStateInternal packageState = innerSnapshot.getPackageStateInternal(internalPackageName);
        if (packageState == null) {
            doDeletePackage = true;
        } else {
            boolean targetIsInstantApp = packageState.getUserStateOrDefault(UserHandle.getUserId(callingUid)).isInstantApp();
            boolean doDeletePackage2 = !targetIsInstantApp || canViewInstantApps;
            doDeletePackage = doDeletePackage2;
        }
        if (doDeletePackage) {
            if (!deleteAllUsers) {
                int returnCode3 = deletePackageX(internalPackageName, versionCode, userId, deleteFlags, false);
                if (returnCode3 != 1 || userId != 0) {
                    returnCode2 = returnCode3;
                } else if (!this.mUserManagerInternal.isDualProfile(999)) {
                    returnCode2 = returnCode3;
                } else if (!this.mUserManagerInternal.isUserRunning(999)) {
                    returnCode2 = returnCode3;
                } else {
                    returnCode2 = returnCode3;
                    deletePackageX(internalPackageName, versionCode, 999, deleteFlags, false);
                }
                returnCode = returnCode2;
            } else {
                int[] blockUninstallUserIds = getBlockUninstallForUsers(innerSnapshot, internalPackageName, users);
                if (ArrayUtils.isEmpty(blockUninstallUserIds)) {
                    returnCode = deletePackageX(internalPackageName, versionCode, userId, deleteFlags, false);
                } else {
                    int userFlags = deleteFlags & (-3);
                    int length = users.length;
                    int i3 = 0;
                    while (i3 < length) {
                        int userId1 = users[i3];
                        if (ArrayUtils.contains(blockUninstallUserIds, userId1)) {
                            i = i3;
                            i2 = length;
                        } else {
                            i = i3;
                            i2 = length;
                            int returnCode4 = deletePackageX(internalPackageName, versionCode, userId1, userFlags, false);
                            if (returnCode4 != 1) {
                                Slog.w("PackageManager", "Package delete failed for user " + userId1 + ", returnCode " + returnCode4);
                            }
                        }
                        i3 = i + 1;
                        length = i2;
                    }
                    returnCode = -4;
                }
            }
        } else {
            returnCode = -1;
        }
        if (returnCode == 1) {
            int[] exusers = users;
            if (userId == 0 && this.mUserManagerInternal.isDualProfile(999) && this.mUserManagerInternal.isUserRunning(999)) {
                exusers = new int[users.length + 1];
                exusers[users.length] = 999;
            }
            ITranPackageManagerService.Instance().onPackageDeleted(packageName, exusers);
        }
        try {
            observer.onPackageDeleted(packageName, returnCode, (String) null);
        } catch (RemoteException e) {
            Log.i("PackageManager", "Observer no longer exists.");
        }
        notifyPackageChangeObserversOnDelete(packageName, versionCode);
        this.mPm.schedulePruneUnusedStaticSharedLibraries(true);
    }

    private boolean isOrphaned(Computer snapshot, String packageName) {
        PackageStateInternal packageState = snapshot.getPackageStateInternal(packageName);
        return packageState != null && packageState.getInstallSource().isOrphaned;
    }

    private boolean isCallerAllowedToSilentlyUninstall(Computer snapshot, int callingUid, String pkgName) {
        if (callingUid == 2000 || callingUid == 0 || UserHandle.getAppId(callingUid) == 1000) {
            return true;
        }
        int callingUserId = UserHandle.getUserId(callingUid);
        if (callingUid == snapshot.getPackageUid(snapshot.getInstallerPackageName(pkgName), 0L, callingUserId)) {
            return true;
        }
        if (this.mPm.mRequiredVerifierPackage != null && callingUid == snapshot.getPackageUid(this.mPm.mRequiredVerifierPackage, 0L, callingUserId)) {
            return true;
        }
        if (this.mPm.mRequiredUninstallerPackage == null || callingUid != snapshot.getPackageUid(this.mPm.mRequiredUninstallerPackage, 0L, callingUserId)) {
            return (this.mPm.mStorageManagerPackage != null && callingUid == snapshot.getPackageUid(this.mPm.mStorageManagerPackage, 0L, callingUserId)) || snapshot.checkUidPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS", callingUid) == 0;
        }
        return true;
    }

    private int[] getBlockUninstallForUsers(Computer snapshot, String packageName, int[] userIds) {
        int[] result = PackageManagerService.EMPTY_INT_ARRAY;
        for (int userId : userIds) {
            if (snapshot.getBlockUninstallForUser(packageName, userId)) {
                result = ArrayUtils.appendInt(result, userId);
            }
        }
        return result;
    }

    private void notifyPackageChangeObserversOnDelete(String packageName, long version) {
        PackageChangeEvent pkgChangeEvent = new PackageChangeEvent();
        pkgChangeEvent.packageName = packageName;
        pkgChangeEvent.version = version;
        pkgChangeEvent.lastUpdateTimeMillis = 0L;
        pkgChangeEvent.newInstalled = false;
        pkgChangeEvent.dataRemoved = false;
        pkgChangeEvent.isDeleted = true;
        this.mPm.notifyPackageChangeObservers(pkgChangeEvent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class TempUserState {
        public final int enabledState;
        public final boolean installed;
        public final String lastDisableAppCaller;

        private TempUserState(int enabledState, String lastDisableAppCaller, boolean installed) {
            this.enabledState = enabledState;
            this.lastDisableAppCaller = lastDisableAppCaller;
            this.installed = installed;
        }
    }

    public void removeUnusedPackagesLPw(UserManagerService userManager, final int userId) {
        int[] users = userManager.getUserIds();
        int numPackages = this.mPm.mSettings.getPackagesLocked().size();
        for (int index = 0; index < numPackages; index++) {
            PackageSetting ps = this.mPm.mSettings.getPackagesLocked().valueAt(index);
            if (ps.getPkg() != null) {
                final String packageName = ps.getPkg().getPackageName();
                if ((ps.getFlags() & 1) == 0 && TextUtils.isEmpty(ps.getPkg().getStaticSharedLibName()) && TextUtils.isEmpty(ps.getPkg().getSdkLibName())) {
                    boolean keep = this.mPm.shouldKeepUninstalledPackageLPr(packageName);
                    if (!keep) {
                        int i = 0;
                        while (true) {
                            if (i >= users.length) {
                                break;
                            } else if (users[i] == userId || !ps.getInstalled(users[i])) {
                                i++;
                            } else {
                                keep = true;
                                break;
                            }
                        }
                    }
                    if (!keep) {
                        this.mPm.mHandler.post(new Runnable() { // from class: com.android.server.pm.DeletePackageHelper$$ExternalSyntheticLambda5
                            @Override // java.lang.Runnable
                            public final void run() {
                                DeletePackageHelper.this.m5408xc91ffe35(packageName, userId);
                            }
                        });
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$removeUnusedPackagesLPw$5$com-android-server-pm-DeletePackageHelper  reason: not valid java name */
    public /* synthetic */ void m5408xc91ffe35(String packageName, int userId) {
        deletePackageX(packageName, -1L, userId, 0, true);
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:19:0x0058 -> B:20:0x0059). Please submit an issue!!! */
    public void deleteExistingPackageAsUser(VersionedPackage versionedPackage, IPackageDeleteObserver2 observer, int userId) {
        int installedForUsersCount;
        this.mPm.mContext.enforceCallingOrSelfPermission("android.permission.DELETE_PACKAGES", null);
        Preconditions.checkNotNull(versionedPackage);
        Preconditions.checkNotNull(observer);
        String packageName = versionedPackage.getPackageName();
        long versionCode = versionedPackage.getLongVersionCode();
        synchronized (this.mPm.mLock) {
            try {
                String internalPkgName = this.mPm.snapshotComputer().resolveInternalPackageName(packageName, versionCode);
                PackageSetting ps = this.mPm.mSettings.getPackageLPr(internalPkgName);
                if (ps != null) {
                    int[] installedUsers = ps.queryInstalledUsers(this.mUserManagerInternal.getUserIds(), true);
                    installedForUsersCount = installedUsers.length;
                } else {
                    installedForUsersCount = 0;
                }
                try {
                    if (installedForUsersCount > 1) {
                        deletePackageVersionedInternal(versionedPackage, observer, userId, 0, true);
                        return;
                    }
                    try {
                        observer.onPackageDeleted(packageName, -1, (String) null);
                    } catch (RemoteException e) {
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }
}
