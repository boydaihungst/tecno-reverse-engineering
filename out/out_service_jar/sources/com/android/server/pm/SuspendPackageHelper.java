package com.android.server.pm;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.pm.SuspendDialogInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IntArray;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.PackageUserStateInternal;
import com.android.server.pm.pkg.SuspendParams;
import com.android.server.pm.pkg.mutate.PackageStateMutator;
import com.android.server.pm.pkg.mutate.PackageUserStateWrite;
import com.android.server.utils.WatchedArrayMap;
import com.transsion.hubcore.server.pm.ITranPackageManagerService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* loaded from: classes2.dex */
public final class SuspendPackageHelper {
    private final BroadcastHelper mBroadcastHelper;
    private final PackageManagerServiceInjector mInjector;
    private final PackageManagerService mPm;
    private final ProtectedPackages mProtectedPackages;

    /* JADX INFO: Access modifiers changed from: package-private */
    public SuspendPackageHelper(PackageManagerService pm, PackageManagerServiceInjector injector, BroadcastHelper broadcastHelper, ProtectedPackages protectedPackages) {
        this.mPm = pm;
        this.mInjector = injector;
        this.mBroadcastHelper = broadcastHelper;
        this.mProtectedPackages = protectedPackages;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String[] setPackagesSuspended(Computer snapshot, String[] packageNames, final boolean suspended, PersistableBundle appExtras, PersistableBundle launcherExtras, SuspendDialogInfo dialogInfo, final String callingPackage, final int userId, int callingUid) {
        final ArraySet<String> modifiedPackages;
        ArraySet<String> modifiedPackages2;
        IntArray modifiedUids;
        ArraySet<String> modifiedPackages3;
        SuspendParams suspendParams;
        String[] strArr = packageNames;
        String str = callingPackage;
        int i = userId;
        int i2 = callingUid;
        if (!ArrayUtils.isEmpty(packageNames)) {
            if (suspended && !isSuspendAllowedForUser(snapshot, i, i2)) {
                Slog.w("PackageManager", "Cannot suspend due to restrictions on user " + i);
                return strArr;
            }
            final SuspendParams newSuspendParams = new SuspendParams(dialogInfo, appExtras, launcherExtras);
            List<String> changedPackagesList = new ArrayList<>(strArr.length);
            IntArray changedUids = new IntArray(strArr.length);
            IntArray modifiedUids2 = new IntArray(strArr.length);
            List<String> unmodifiablePackages = new ArrayList<>(strArr.length);
            ArraySet<String> modifiedPackages4 = new ArraySet<>();
            IntArray modifiedUids3 = modifiedUids2;
            boolean[] canSuspend = suspended ? canSuspendPackageForUser(snapshot, strArr, i, i2) : null;
            int i3 = 0;
            while (true) {
                modifiedPackages = modifiedPackages4;
                if (i3 >= strArr.length) {
                    break;
                }
                String packageName = strArr[i3];
                if (str.equals(packageName)) {
                    Slog.w("PackageManager", "Calling package: " + str + " trying to " + (suspended ? "" : "un") + "suspend itself. Ignoring");
                    unmodifiablePackages.add(packageName);
                    modifiedUids = modifiedUids3;
                    modifiedPackages3 = modifiedPackages;
                } else {
                    PackageStateInternal packageState = snapshot.getPackageStateInternal(packageName);
                    if (packageState == null) {
                        modifiedUids = modifiedUids3;
                        modifiedPackages3 = modifiedPackages;
                    } else if (snapshot.shouldFilterApplication(packageState, i2, i)) {
                        modifiedUids = modifiedUids3;
                        modifiedPackages3 = modifiedPackages;
                    } else if (canSuspend != null && !canSuspend[i3]) {
                        unmodifiablePackages.add(packageName);
                        modifiedUids = modifiedUids3;
                        modifiedPackages3 = modifiedPackages;
                    } else {
                        WatchedArrayMap<String, SuspendParams> suspendParamsMap = packageState.getUserStateOrDefault(i).getSuspendParams();
                        if (suspended && suspendParamsMap != null && suspendParamsMap.containsKey(packageName) && (suspendParams = suspendParamsMap.get(packageName)) != null && Objects.equals(suspendParams.getDialogInfo(), dialogInfo) && Objects.equals(suspendParams.getAppExtras(), appExtras) && Objects.equals(suspendParams.getLauncherExtras(), launcherExtras)) {
                            changedPackagesList.add(packageName);
                            changedUids.add(UserHandle.getUid(i, packageState.getAppId()));
                            modifiedUids = modifiedUids3;
                            modifiedPackages3 = modifiedPackages;
                        } else {
                            boolean packageUnsuspended = !suspended && CollectionUtils.size(suspendParamsMap) <= 1;
                            if (suspended || packageUnsuspended) {
                                changedPackagesList.add(packageName);
                                changedUids.add(UserHandle.getUid(i, packageState.getAppId()));
                            }
                            modifiedPackages3 = modifiedPackages;
                            modifiedPackages3.add(packageName);
                            int uid = UserHandle.getUid(i, packageState.getAppId());
                            modifiedUids = modifiedUids3;
                            modifiedUids.add(uid);
                        }
                    }
                    Slog.w("PackageManager", "Could not find package setting for package: " + packageName + ". Skipping suspending/un-suspending.");
                    unmodifiablePackages.add(packageName);
                }
                i3++;
                strArr = packageNames;
                i2 = callingUid;
                modifiedUids3 = modifiedUids;
                modifiedPackages4 = modifiedPackages3;
                str = callingPackage;
                i = userId;
            }
            this.mPm.commitPackageStateMutation(null, new Consumer() { // from class: com.android.server.pm.SuspendPackageHelper$$ExternalSyntheticLambda3
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    SuspendPackageHelper.lambda$setPackagesSuspended$0(modifiedPackages, userId, suspended, callingPackage, newSuspendParams, (PackageStateMutator) obj);
                }
            });
            Computer newSnapshot = this.mPm.snapshotComputer();
            if (changedPackagesList.isEmpty()) {
                modifiedPackages2 = modifiedPackages;
            } else {
                String[] changedPackages = (String[]) changedPackagesList.toArray(new String[0]);
                modifiedPackages2 = modifiedPackages;
                sendPackagesSuspendedForUser(newSnapshot, suspended ? "android.intent.action.PACKAGES_SUSPENDED" : "android.intent.action.PACKAGES_UNSUSPENDED", changedPackages, changedUids.toArray(), userId);
                sendMyPackageSuspendedOrUnsuspended(changedPackages, suspended, userId);
                this.mPm.scheduleWritePackageRestrictions(userId);
            }
            if (!modifiedPackages2.isEmpty()) {
                sendPackagesSuspendedForUser(newSnapshot, "android.intent.action.PACKAGES_SUSPENSION_CHANGED", (String[]) modifiedPackages2.toArray(new String[0]), modifiedUids3.toArray(), userId);
            }
            return (String[]) unmodifiablePackages.toArray(new String[0]);
        }
        return strArr;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$setPackagesSuspended$0(ArraySet modifiedPackages, int userId, boolean suspended, String callingPackage, SuspendParams newSuspendParams, PackageStateMutator mutator) {
        int size = modifiedPackages.size();
        for (int index = 0; index < size; index++) {
            String packageName = (String) modifiedPackages.valueAt(index);
            PackageUserStateWrite userState = mutator.forPackage(packageName).userState(userId);
            if (suspended) {
                userState.putSuspendParams(callingPackage, newSuspendParams);
            } else {
                userState.removeSuspension(callingPackage);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String[] getUnsuspendablePackagesForUser(Computer snapshot, String[] packageNames, int userId, int callingUid) {
        if (!isSuspendAllowedForUser(snapshot, userId, callingUid)) {
            Slog.w("PackageManager", "Cannot suspend due to restrictions on user " + userId);
            return packageNames;
        }
        ArraySet<String> unactionablePackages = new ArraySet<>();
        boolean[] canSuspend = canSuspendPackageForUser(snapshot, packageNames, userId, callingUid);
        for (int i = 0; i < packageNames.length; i++) {
            if (!canSuspend[i]) {
                unactionablePackages.add(packageNames[i]);
            } else {
                PackageStateInternal packageState = snapshot.getPackageStateFiltered(packageNames[i], callingUid, userId);
                if (packageState == null) {
                    Slog.w("PackageManager", "Could not find package setting for package: " + packageNames[i]);
                    unactionablePackages.add(packageNames[i]);
                }
            }
        }
        return (String[]) unactionablePackages.toArray(new String[unactionablePackages.size()]);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Bundle getSuspendedPackageAppExtras(Computer snapshot, String packageName, int userId, int callingUid) {
        PackageStateInternal ps = snapshot.getPackageStateInternal(packageName, callingUid);
        if (ps == null) {
            return null;
        }
        PackageUserStateInternal pus = ps.getUserStateOrDefault(userId);
        Bundle allExtras = new Bundle();
        if (pus.isSuspended()) {
            for (int i = 0; i < pus.getSuspendParams().size(); i++) {
                SuspendParams params = pus.getSuspendParams().valueAt(i);
                if (params != null && params.getAppExtras() != null) {
                    allExtras.putAll(params.getAppExtras());
                }
            }
        }
        int i2 = allExtras.size();
        if (i2 > 0) {
            return allExtras;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeSuspensionsBySuspendingPackage(Computer computer, String[] packagesToChange, Predicate<String> suspendingPackagePredicate, final int userId) {
        int i;
        int i2;
        ArraySet<String> suspendingPkgsToCommit;
        List<String> unsuspendedPackages = new ArrayList<>();
        IntArray unsuspendedUids = new IntArray();
        final ArrayMap<String, ArraySet<String>> pkgToSuspendingPkgsToCommit = new ArrayMap<>();
        int length = packagesToChange.length;
        int i3 = 0;
        while (true) {
            if (i3 >= length) {
                break;
            }
            String packageName = packagesToChange[i3];
            PackageStateInternal packageState = computer.getPackageStateInternal(packageName);
            PackageUserStateInternal packageUserState = packageState != null ? packageState.getUserStateOrDefault(userId) : null;
            if (packageUserState == null) {
                i = length;
            } else if (packageUserState.isSuspended()) {
                WatchedArrayMap<String, SuspendParams> suspendParamsMap = packageUserState.getSuspendParams();
                int countRemoved = 0;
                int index = 0;
                while (index < suspendParamsMap.size()) {
                    String suspendingPackage = suspendParamsMap.keyAt(index);
                    PackageUserStateInternal packageUserState2 = packageUserState;
                    if (!suspendingPackagePredicate.test(suspendingPackage)) {
                        i2 = length;
                    } else {
                        ArraySet<String> suspendingPkgsToCommit2 = pkgToSuspendingPkgsToCommit.get(packageName);
                        if (suspendingPkgsToCommit2 != null) {
                            i2 = length;
                            suspendingPkgsToCommit = suspendingPkgsToCommit2;
                        } else {
                            i2 = length;
                            suspendingPkgsToCommit = new ArraySet<>();
                            pkgToSuspendingPkgsToCommit.put(packageName, suspendingPkgsToCommit);
                        }
                        suspendingPkgsToCommit.add(suspendingPackage);
                        countRemoved++;
                    }
                    index++;
                    packageUserState = packageUserState2;
                    length = i2;
                }
                i = length;
                if (countRemoved == suspendParamsMap.size()) {
                    unsuspendedPackages.add(packageState.getPackageName());
                    unsuspendedUids.add(UserHandle.getUid(userId, packageState.getAppId()));
                }
            } else {
                i = length;
            }
            i3++;
            length = i;
        }
        this.mPm.commitPackageStateMutation(null, new Consumer() { // from class: com.android.server.pm.SuspendPackageHelper$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                SuspendPackageHelper.lambda$removeSuspensionsBySuspendingPackage$1(pkgToSuspendingPkgsToCommit, userId, (PackageStateMutator) obj);
            }
        });
        Computer newSnapshot = this.mPm.snapshotComputer();
        this.mPm.scheduleWritePackageRestrictions(userId);
        if (!unsuspendedPackages.isEmpty()) {
            String[] packageArray = (String[]) unsuspendedPackages.toArray(new String[unsuspendedPackages.size()]);
            sendMyPackageSuspendedOrUnsuspended(packageArray, false, userId);
            sendPackagesSuspendedForUser(newSnapshot, "android.intent.action.PACKAGES_UNSUSPENDED", packageArray, unsuspendedUids.toArray(), userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$removeSuspensionsBySuspendingPackage$1(ArrayMap pkgToSuspendingPkgsToCommit, int userId, PackageStateMutator mutator) {
        for (int mapIndex = 0; mapIndex < pkgToSuspendingPkgsToCommit.size(); mapIndex++) {
            String packageName = (String) pkgToSuspendingPkgsToCommit.keyAt(mapIndex);
            ArraySet<String> packagesToRemove = (ArraySet) pkgToSuspendingPkgsToCommit.valueAt(mapIndex);
            PackageUserStateWrite userState = mutator.forPackage(packageName).userState(userId);
            for (int setIndex = 0; setIndex < packagesToRemove.size(); setIndex++) {
                userState.removeSuspension(packagesToRemove.valueAt(setIndex));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Bundle getSuspendedPackageLauncherExtras(Computer snapshot, String packageName, int userId, int callingUid) {
        PackageStateInternal packageState = snapshot.getPackageStateInternal(packageName, callingUid);
        if (packageState == null) {
            return null;
        }
        Bundle allExtras = new Bundle();
        PackageUserStateInternal userState = packageState.getUserStateOrDefault(userId);
        if (userState.isSuspended()) {
            for (int i = 0; i < userState.getSuspendParams().size(); i++) {
                SuspendParams params = userState.getSuspendParams().valueAt(i);
                if (params != null && params.getLauncherExtras() != null) {
                    allExtras.putAll(params.getLauncherExtras());
                }
            }
        }
        int i2 = allExtras.size();
        if (i2 > 0) {
            return allExtras;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPackageSuspended(Computer snapshot, String packageName, int userId, int callingUid) {
        PackageStateInternal packageState = snapshot.getPackageStateInternal(packageName, callingUid);
        return packageState != null && packageState.getUserStateOrDefault(userId).isSuspended();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getSuspendingPackage(Computer snapshot, String suspendedPackage, int userId, int callingUid) {
        PackageStateInternal packageState = snapshot.getPackageStateInternal(suspendedPackage, callingUid);
        if (packageState == null) {
            return null;
        }
        PackageUserStateInternal userState = packageState.getUserStateOrDefault(userId);
        if (!userState.isSuspended()) {
            return null;
        }
        String suspendingPackage = null;
        for (int i = 0; i < userState.getSuspendParams().size(); i++) {
            String suspendingPackage2 = userState.getSuspendParams().keyAt(i);
            suspendingPackage = suspendingPackage2;
            if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(suspendingPackage)) {
                return suspendingPackage;
            }
        }
        return suspendingPackage;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SuspendDialogInfo getSuspendedDialogInfo(Computer snapshot, String suspendedPackage, String suspendingPackage, int userId, int callingUid) {
        WatchedArrayMap<String, SuspendParams> suspendParamsMap;
        SuspendParams suspendParams;
        PackageStateInternal packageState = snapshot.getPackageStateInternal(suspendedPackage, callingUid);
        if (packageState == null) {
            return null;
        }
        PackageUserStateInternal userState = packageState.getUserStateOrDefault(userId);
        if (!userState.isSuspended() || (suspendParamsMap = userState.getSuspendParams()) == null || (suspendParams = suspendParamsMap.get(suspendingPackage)) == null) {
            return null;
        }
        return suspendParams.getDialogInfo();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSuspendAllowedForUser(Computer snapshot, int userId, int callingUid) {
        UserManagerService userManager = this.mInjector.getUserManagerService();
        return isCallerDeviceOrProfileOwner(snapshot, userId, callingUid) || !(userManager.hasUserRestriction("no_control_apps", userId) || userManager.hasUserRestriction("no_uninstall_apps", userId));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean[] canSuspendPackageForUser(Computer snapshot, String[] packageNames, int userId, int callingUid) {
        long token;
        String packageName;
        SuspendPackageHelper suspendPackageHelper = this;
        Computer computer = snapshot;
        String[] strArr = packageNames;
        boolean[] canSuspend = new boolean[strArr.length];
        boolean isCallerOwner = suspendPackageHelper.isCallerDeviceOrProfileOwner(computer, userId, callingUid);
        long token2 = Binder.clearCallingIdentity();
        try {
            DefaultAppProvider defaultAppProvider = suspendPackageHelper.mInjector.getDefaultAppProvider();
            String activeLauncherPackageName = defaultAppProvider.getDefaultHome(userId);
            String dialerPackageName = defaultAppProvider.getDefaultDialer(userId);
            String requiredInstallerPackage = suspendPackageHelper.getKnownPackageName(computer, 2, userId);
            String requiredUninstallerPackage = suspendPackageHelper.getKnownPackageName(computer, 3, userId);
            String requiredVerifierPackage = suspendPackageHelper.getKnownPackageName(computer, 4, userId);
            String requiredPermissionControllerPackage = suspendPackageHelper.getKnownPackageName(computer, 7, userId);
            int i = 0;
            while (i < strArr.length) {
                try {
                    canSuspend[i] = false;
                    packageName = strArr[i];
                    token = token2;
                } catch (Throwable th) {
                    th = th;
                    token = token2;
                    Binder.restoreCallingIdentity(token);
                    throw th;
                }
                try {
                    if (suspendPackageHelper.mPm.isPackageDeviceAdmin(packageName, userId)) {
                        Slog.w("PackageManager", "Cannot suspend package \"" + packageName + "\": has an active device admin");
                    } else if (packageName.equals(activeLauncherPackageName)) {
                        Slog.w("PackageManager", "Cannot suspend package \"" + packageName + "\": contains the active launcher");
                    } else if (packageName.equals(requiredInstallerPackage)) {
                        Slog.w("PackageManager", "Cannot suspend package \"" + packageName + "\": required for package installation");
                    } else if (packageName.equals(requiredUninstallerPackage)) {
                        Slog.w("PackageManager", "Cannot suspend package \"" + packageName + "\": required for package uninstallation");
                    } else if (packageName.equals(requiredVerifierPackage)) {
                        Slog.w("PackageManager", "Cannot suspend package \"" + packageName + "\": required for package verification");
                    } else if (packageName.equals(dialerPackageName)) {
                        Slog.w("PackageManager", "Cannot suspend package \"" + packageName + "\": is the default dialer");
                    } else if (packageName.equals(requiredPermissionControllerPackage)) {
                        Slog.w("PackageManager", "Cannot suspend package \"" + packageName + "\": required for permissions management");
                    } else if (suspendPackageHelper.mProtectedPackages.isPackageStateProtected(userId, packageName)) {
                        Slog.w("PackageManager", "Cannot suspend package \"" + packageName + "\": protected package");
                    } else if (!isCallerOwner && computer.getBlockUninstall(userId, packageName)) {
                        Slog.w("PackageManager", "Cannot suspend package \"" + packageName + "\": blocked by admin");
                    } else {
                        PackageStateInternal packageState = computer.getPackageStateInternal(packageName);
                        AndroidPackage pkg = packageState == null ? null : packageState.getPkg();
                        if (pkg != null) {
                            if (pkg.isSdkLibrary()) {
                                Slog.w("PackageManager", "Cannot suspend package: " + packageName + " providing SDK library: " + pkg.getSdkLibName());
                            } else if (pkg.isStaticSharedLibrary()) {
                                Slog.w("PackageManager", "Cannot suspend package: " + packageName + " providing static shared library: " + pkg.getStaticSharedLibName());
                            }
                        }
                        if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(packageName)) {
                            Slog.w("PackageManager", "Cannot suspend the platform package: " + packageName);
                        } else {
                            canSuspend[i] = true;
                        }
                    }
                    i++;
                    suspendPackageHelper = this;
                    computer = snapshot;
                    strArr = packageNames;
                    token2 = token;
                } catch (Throwable th2) {
                    th = th2;
                    Binder.restoreCallingIdentity(token);
                    throw th;
                }
            }
            token = token2;
        } catch (Throwable th3) {
            th = th3;
            token = token2;
        }
        try {
            ITranPackageManagerService.Instance().canSuspendPackageForUserInternal(packageNames, userId, canSuspend);
            Binder.restoreCallingIdentity(token);
            return canSuspend;
        } catch (Throwable th4) {
            th = th4;
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    void sendPackagesSuspendedForUser(Computer snapshot, final String intent, String[] pkgList, int[] uidList, int userId) {
        boolean z;
        int i;
        List<List<String>> pkgsToSend = new ArrayList<>(pkgList.length);
        List<IntArray> uidsToSend = new ArrayList<>(pkgList.length);
        List<SparseArray<int[]>> allowListsToSend = new ArrayList<>(pkgList.length);
        int i2 = 0;
        final int[] userIds = {userId};
        int i3 = 0;
        while (i3 < pkgList.length) {
            String pkgName = pkgList[i3];
            int uid = uidList[i3];
            SparseArray<int[]> allowList = this.mInjector.getAppsFilter().getVisibilityAllowList(snapshot, snapshot.getPackageStateInternal(pkgName, 1000), userIds, snapshot.getPackageStates());
            if (allowList == null) {
                allowList = new SparseArray<>(i2);
            }
            boolean merged = false;
            int j = 0;
            while (true) {
                if (j < allowListsToSend.size()) {
                    if (!Arrays.equals(allowListsToSend.get(j).get(userId), allowList.get(userId))) {
                        j++;
                    } else {
                        pkgsToSend.get(j).add(pkgName);
                        uidsToSend.get(j).add(uid);
                        merged = true;
                        break;
                    }
                } else {
                    break;
                }
            }
            if (merged) {
                z = true;
                i = 0;
            } else {
                z = true;
                i = 0;
                pkgsToSend.add(new ArrayList<>(Arrays.asList(pkgName)));
                uidsToSend.add(IntArray.wrap(new int[]{uid}));
                allowListsToSend.add(allowList);
            }
            i3++;
            i2 = i;
        }
        Handler handler = this.mInjector.getHandler();
        for (int i4 = 0; i4 < pkgsToSend.size(); i4++) {
            final Bundle extras = new Bundle(3);
            extras.putStringArray("android.intent.extra.changed_package_list", (String[]) pkgsToSend.get(i4).toArray(new String[pkgsToSend.get(i4).size()]));
            extras.putIntArray("android.intent.extra.changed_uid_list", uidsToSend.get(i4).toArray());
            final SparseArray<int[]> allowList2 = allowListsToSend.get(i4).size() == 0 ? null : allowListsToSend.get(i4);
            handler.post(new Runnable() { // from class: com.android.server.pm.SuspendPackageHelper$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    SuspendPackageHelper.this.m5699x90cc6a73(intent, extras, userIds, allowList2);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendPackagesSuspendedForUser$2$com-android-server-pm-SuspendPackageHelper  reason: not valid java name */
    public /* synthetic */ void m5699x90cc6a73(String intent, Bundle extras, int[] userIds, SparseArray allowList) {
        this.mBroadcastHelper.sendPackageBroadcast(intent, null, extras, 1073741824, null, null, userIds, null, allowList, null);
    }

    private String getKnownPackageName(Computer snapshot, int knownPackage, int userId) {
        String[] knownPackages = this.mPm.getKnownPackageNamesInternal(snapshot, knownPackage, userId);
        if (knownPackages.length > 0) {
            return knownPackages[0];
        }
        return null;
    }

    private boolean isCallerDeviceOrProfileOwner(Computer snapshot, int userId, int callingUid) {
        if (callingUid == 1000) {
            return true;
        }
        String ownerPackage = this.mProtectedPackages.getDeviceOwnerOrProfileOwnerPackage(userId);
        if (ownerPackage != null && callingUid == snapshot.getPackageUidInternal(ownerPackage, 0L, userId, callingUid)) {
            return true;
        }
        return false;
    }

    private void sendMyPackageSuspendedOrUnsuspended(final String[] affectedPackages, final boolean suspended, final int userId) {
        final String action;
        Handler handler = this.mInjector.getHandler();
        if (suspended) {
            action = "android.intent.action.MY_PACKAGE_SUSPENDED";
        } else {
            action = "android.intent.action.MY_PACKAGE_UNSUSPENDED";
        }
        handler.post(new Runnable() { // from class: com.android.server.pm.SuspendPackageHelper$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                SuspendPackageHelper.this.m5698x63abc4e4(suspended, userId, affectedPackages, action);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendMyPackageSuspendedOrUnsuspended$3$com-android-server-pm-SuspendPackageHelper  reason: not valid java name */
    public /* synthetic */ void m5698x63abc4e4(boolean suspended, int userId, String[] affectedPackages, String action) {
        Bundle bundle;
        Bundle intentExtras;
        IActivityManager am = ActivityManager.getService();
        if (am == null) {
            Slog.wtf("PackageManager", "IActivityManager null. Cannot send MY_PACKAGE_ " + (suspended ? "" : "UN") + "SUSPENDED broadcasts");
            return;
        }
        int[] targetUserIds = {userId};
        Computer snapshot = this.mPm.snapshotComputer();
        int length = affectedPackages.length;
        int i = 0;
        while (i < length) {
            String packageName = affectedPackages[i];
            if (suspended) {
                bundle = getSuspendedPackageAppExtras(snapshot, packageName, userId, 1000);
            } else {
                bundle = null;
            }
            Bundle appExtras = bundle;
            if (appExtras != null) {
                intentExtras = new Bundle(1);
                intentExtras.putBundle("android.intent.extra.SUSPENDED_PACKAGE_EXTRAS", appExtras);
            } else {
                intentExtras = null;
            }
            this.mBroadcastHelper.doSendBroadcast(action, null, intentExtras, 16777216, packageName, null, targetUserIds, false, null, null);
            i++;
            length = length;
            snapshot = snapshot;
        }
    }
}
