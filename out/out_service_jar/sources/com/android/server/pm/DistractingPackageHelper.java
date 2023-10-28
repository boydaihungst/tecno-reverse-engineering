package com.android.server.pm;

import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.IntArray;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.mutate.PackageStateMutator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public final class DistractingPackageHelper {
    private final BroadcastHelper mBroadcastHelper;
    private final PackageManagerServiceInjector mInjector;
    private final PackageManagerService mPm;
    private final SuspendPackageHelper mSuspendPackageHelper;

    /* JADX INFO: Access modifiers changed from: package-private */
    public DistractingPackageHelper(PackageManagerService pm, PackageManagerServiceInjector injector, BroadcastHelper broadcastHelper, SuspendPackageHelper suspendPackageHelper) {
        this.mPm = pm;
        this.mInjector = injector;
        this.mBroadcastHelper = broadcastHelper;
        this.mSuspendPackageHelper = suspendPackageHelper;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String[] setDistractingPackageRestrictionsAsUser(Computer snapshot, String[] packageNames, final int restrictionFlags, final int userId, int callingUid) {
        boolean[] canRestrict;
        Computer computer = snapshot;
        if (ArrayUtils.isEmpty(packageNames)) {
            return packageNames;
        }
        if (restrictionFlags != 0 && !this.mSuspendPackageHelper.isSuspendAllowedForUser(computer, userId, callingUid)) {
            Slog.w("PackageManager", "Cannot restrict packages due to restrictions on user " + userId);
            return packageNames;
        }
        List<String> changedPackagesList = new ArrayList<>(packageNames.length);
        IntArray changedUids = new IntArray(packageNames.length);
        List<String> unactionedPackages = new ArrayList<>(packageNames.length);
        final ArraySet<String> changesToCommit = new ArraySet<>();
        if (restrictionFlags != 0) {
            canRestrict = this.mSuspendPackageHelper.canSuspendPackageForUser(computer, packageNames, userId, callingUid);
        } else {
            canRestrict = null;
        }
        int i = 0;
        while (i < packageNames.length) {
            String packageName = packageNames[i];
            PackageStateInternal packageState = computer.getPackageStateInternal(packageName);
            if (packageState == null || computer.shouldFilterApplication(packageState, callingUid, userId)) {
                Slog.w("PackageManager", "Could not find package setting for package: " + packageName + ". Skipping...");
                unactionedPackages.add(packageName);
            } else if (canRestrict != null && !canRestrict[i]) {
                unactionedPackages.add(packageName);
            } else {
                int oldDistractionFlags = packageState.getUserStateOrDefault(userId).getDistractionFlags();
                if (restrictionFlags != oldDistractionFlags) {
                    changedPackagesList.add(packageName);
                    changedUids.add(UserHandle.getUid(userId, packageState.getAppId()));
                    changesToCommit.add(packageName);
                }
            }
            i++;
            computer = snapshot;
        }
        this.mPm.commitPackageStateMutation(null, new Consumer() { // from class: com.android.server.pm.DistractingPackageHelper$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DistractingPackageHelper.lambda$setDistractingPackageRestrictionsAsUser$0(changesToCommit, userId, restrictionFlags, (PackageStateMutator) obj);
            }
        });
        if (!changedPackagesList.isEmpty()) {
            String[] changedPackages = (String[]) changedPackagesList.toArray(new String[changedPackagesList.size()]);
            sendDistractingPackagesChanged(changedPackages, changedUids.toArray(), userId, restrictionFlags);
            this.mPm.scheduleWritePackageRestrictions(userId);
        }
        return (String[]) unactionedPackages.toArray(new String[0]);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$setDistractingPackageRestrictionsAsUser$0(ArraySet changesToCommit, int userId, int restrictionFlags, PackageStateMutator mutator) {
        int size = changesToCommit.size();
        for (int index = 0; index < size; index++) {
            mutator.forPackage((String) changesToCommit.valueAt(index)).userState(userId).setDistractionFlags(restrictionFlags);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeDistractingPackageRestrictions(Computer snapshot, String[] packagesToChange, final int userId) {
        if (ArrayUtils.isEmpty(packagesToChange)) {
            return;
        }
        final List<String> changedPackages = new ArrayList<>(packagesToChange.length);
        IntArray changedUids = new IntArray(packagesToChange.length);
        for (String packageName : packagesToChange) {
            PackageStateInternal ps = snapshot.getPackageStateInternal(packageName);
            if (ps != null && ps.getUserStateOrDefault(userId).getDistractionFlags() != 0) {
                changedPackages.add(ps.getPackageName());
                changedUids.add(UserHandle.getUid(userId, ps.getAppId()));
            }
        }
        this.mPm.commitPackageStateMutation(null, new Consumer() { // from class: com.android.server.pm.DistractingPackageHelper$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DistractingPackageHelper.lambda$removeDistractingPackageRestrictions$1(changedPackages, userId, (PackageStateMutator) obj);
            }
        });
        if (!changedPackages.isEmpty()) {
            String[] packageArray = (String[]) changedPackages.toArray(new String[changedPackages.size()]);
            sendDistractingPackagesChanged(packageArray, changedUids.toArray(), userId, 0);
            this.mPm.scheduleWritePackageRestrictions(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$removeDistractingPackageRestrictions$1(List changedPackages, int userId, PackageStateMutator mutator) {
        for (int index = 0; index < changedPackages.size(); index++) {
            mutator.forPackage((String) changedPackages.get(index)).userState(userId).setDistractionFlags(0);
        }
    }

    void sendDistractingPackagesChanged(String[] pkgList, int[] uidList, final int userId, int distractionFlags) {
        final Bundle extras = new Bundle(3);
        extras.putStringArray("android.intent.extra.changed_package_list", pkgList);
        extras.putIntArray("android.intent.extra.changed_uid_list", uidList);
        extras.putInt("android.intent.extra.distraction_restrictions", distractionFlags);
        Handler handler = this.mInjector.getHandler();
        handler.post(new Runnable() { // from class: com.android.server.pm.DistractingPackageHelper$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                DistractingPackageHelper.this.m5410xb861188e(extras, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendDistractingPackagesChanged$2$com-android-server-pm-DistractingPackageHelper  reason: not valid java name */
    public /* synthetic */ void m5410xb861188e(Bundle extras, int userId) {
        this.mBroadcastHelper.sendPackageBroadcast("android.intent.action.DISTRACTING_PACKAGES_CHANGED", null, extras, 1073741824, null, null, new int[]{userId}, null, null, null);
    }
}
