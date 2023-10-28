package com.android.server.pm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.AuxiliaryResolveInfo;
import android.content.pm.IOnChecksumsReadyListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ProcessInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.SuspendDialogInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseArray;
import com.android.server.pm.dex.DexManager;
import com.android.server.pm.dex.DynamicCodeLogger;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import com.android.server.pm.pkg.AndroidPackageApi;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.PackageStateUtils;
import com.android.server.pm.pkg.SharedUserApi;
import com.android.server.pm.pkg.component.ParsedMainComponent;
import com.android.server.pm.pkg.mutate.PackageStateMutator;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public abstract class PackageManagerInternalBase extends PackageManagerInternal {
    private final PackageManagerService mService;

    protected abstract ApexManager getApexManager();

    protected abstract AppDataHelper getAppDataHelper();

    protected abstract Context getContext();

    protected abstract DexManager getDexManager();

    protected abstract DistractingPackageHelper getDistractingPackageHelper();

    protected abstract InstantAppRegistry getInstantAppRegistry();

    protected abstract PackageObserverHelper getPackageObserverHelper();

    protected abstract PermissionManagerServiceInternal getPermissionManager();

    protected abstract ProtectedPackages getProtectedPackages();

    protected abstract ResolveIntentHelper getResolveIntentHelper();

    protected abstract SuspendPackageHelper getSuspendPackageHelper();

    protected abstract UserNeedsBadgingCache getUserNeedsBadging();

    public PackageManagerInternalBase(PackageManagerService service) {
        this.mService = service;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // android.content.pm.PackageManagerInternal
    public final Computer snapshot() {
        return this.mService.snapshotComputer();
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final List<ApplicationInfo> getInstalledApplications(long flags, int userId, int callingUid) {
        return snapshot().getInstalledApplications(flags, userId, callingUid);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean isInstantApp(String packageName, int userId) {
        return snapshot().isInstantApp(packageName, userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final String getInstantAppPackageName(int uid) {
        return snapshot().getInstantAppPackageName(uid);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean filterAppAccess(AndroidPackage pkg, int callingUid, int userId) {
        return snapshot().filterAppAccess(pkg, callingUid, userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean filterAppAccess(String packageName, int callingUid, int userId) {
        return snapshot().filterAppAccess(packageName, callingUid, userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean filterAppAccess(int uid, int callingUid) {
        return snapshot().filterAppAccess(uid, callingUid);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final int[] getVisibilityAllowList(String packageName, int userId) {
        return snapshot().getVisibilityAllowList(packageName, userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean canQueryPackage(int callingUid, String packageName) {
        return snapshot().canQueryPackage(callingUid, packageName);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final AndroidPackage getPackage(String packageName) {
        return snapshot().getPackage(packageName);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final AndroidPackageApi getAndroidPackage(String packageName) {
        return snapshot().getPackage(packageName);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final AndroidPackage getPackage(int uid) {
        return snapshot().getPackage(uid);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final List<AndroidPackage> getPackagesForAppId(int appId) {
        return snapshot().getPackagesForAppId(appId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final PackageStateInternal getPackageStateInternal(String packageName) {
        return snapshot().getPackageStateInternal(packageName);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final ArrayMap<String, ? extends PackageStateInternal> getPackageStates() {
        return snapshot().getPackageStates();
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final void removePackageListObserver(PackageManagerInternal.PackageListObserver observer) {
        getPackageObserverHelper().removeObserver(observer);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final PackageStateInternal getDisabledSystemPackage(String packageName) {
        return snapshot().getDisabledSystemPackage(packageName);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final String[] getKnownPackageNames(int knownPackage, int userId) {
        return this.mService.getKnownPackageNamesInternal(snapshot(), knownPackage, userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final void setKeepUninstalledPackages(List<String> packageList) {
        this.mService.setKeepUninstalledPackagesInternal(snapshot(), packageList);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean isPermissionsReviewRequired(String packageName, int userId) {
        return getPermissionManager().isPermissionsReviewRequired(packageName, userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final PackageInfo getPackageInfo(String packageName, long flags, int filterCallingUid, int userId) {
        return snapshot().getPackageInfoInternal(packageName, -1L, flags, filterCallingUid, userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final Bundle getSuspendedPackageLauncherExtras(String packageName, int userId) {
        return getSuspendPackageHelper().getSuspendedPackageLauncherExtras(snapshot(), packageName, userId, Binder.getCallingUid());
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean isPackageSuspended(String packageName, int userId) {
        return getSuspendPackageHelper().isPackageSuspended(snapshot(), packageName, userId, Binder.getCallingUid());
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final void removeNonSystemPackageSuspensions(String packageName, int userId) {
        getSuspendPackageHelper().removeSuspensionsBySuspendingPackage(snapshot(), new String[]{packageName}, new Predicate() { // from class: com.android.server.pm.PackageManagerInternalBase$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return PackageManagerInternalBase.lambda$removeNonSystemPackageSuspensions$0((String) obj);
            }
        }, userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$removeNonSystemPackageSuspensions$0(String suspendingPackage) {
        return !PackageManagerService.PLATFORM_PACKAGE_NAME.equals(suspendingPackage);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final void removeDistractingPackageRestrictions(String packageName, int userId) {
        getDistractingPackageHelper().removeDistractingPackageRestrictions(snapshot(), new String[]{packageName}, userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final void removeAllDistractingPackageRestrictions(int userId) {
        this.mService.removeAllDistractingPackageRestrictions(snapshot(), userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final String getSuspendingPackage(String suspendedPackage, int userId) {
        return getSuspendPackageHelper().getSuspendingPackage(snapshot(), suspendedPackage, userId, Binder.getCallingUid());
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final SuspendDialogInfo getSuspendedDialogInfo(String suspendedPackage, String suspendingPackage, int userId) {
        return getSuspendPackageHelper().getSuspendedDialogInfo(snapshot(), suspendedPackage, suspendingPackage, userId, Binder.getCallingUid());
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final int getDistractingPackageRestrictions(String packageName, int userId) {
        PackageStateInternal packageState = getPackageStateInternal(packageName);
        if (packageState == null) {
            return 0;
        }
        return packageState.getUserStateOrDefault(userId).getDistractionFlags();
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final int getPackageUid(String packageName, long flags, int userId) {
        return snapshot().getPackageUidInternal(packageName, flags, userId, 1000);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final ApplicationInfo getApplicationInfo(String packageName, long flags, int filterCallingUid, int userId) {
        return snapshot().getApplicationInfoInternal(packageName, flags, filterCallingUid, userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final ActivityInfo getActivityInfo(ComponentName component, long flags, int filterCallingUid, int userId) {
        return snapshot().getActivityInfoInternal(component, flags, filterCallingUid, userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final List<ResolveInfo> queryIntentActivities(Intent intent, String resolvedType, long flags, int filterCallingUid, int userId) {
        return snapshot().queryIntentActivitiesInternal(intent, resolvedType, flags, filterCallingUid, userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final List<ResolveInfo> queryIntentReceivers(Intent intent, String resolvedType, long flags, int filterCallingUid, int userId, boolean forSend) {
        return getResolveIntentHelper().queryIntentReceiversInternal(snapshot(), intent, resolvedType, flags, userId, filterCallingUid, forSend);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final List<ResolveInfo> queryIntentServices(Intent intent, long flags, int callingUid, int userId) {
        String resolvedType = intent.resolveTypeIfNeeded(getContext().getContentResolver());
        return snapshot().queryIntentServicesInternal(intent, resolvedType, flags, userId, callingUid, false);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final ComponentName getHomeActivitiesAsUser(List<ResolveInfo> allHomeCandidates, int userId) {
        return snapshot().getHomeActivitiesAsUser(allHomeCandidates, userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final ComponentName getDefaultHomeActivity(int userId) {
        return snapshot().getDefaultHomeActivity(userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final ComponentName getSystemUiServiceComponent() {
        return ComponentName.unflattenFromString(getContext().getResources().getString(17040045));
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final void setOwnerProtectedPackages(int userId, List<String> packageNames) {
        getProtectedPackages().setOwnerProtectedPackages(userId, packageNames);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean isPackageDataProtected(int userId, String packageName) {
        return getProtectedPackages().isPackageDataProtected(userId, packageName);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean isPackageStateProtected(String packageName, int userId) {
        return getProtectedPackages().isPackageStateProtected(userId, packageName);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean isPackageEphemeral(int userId, String packageName) {
        PackageStateInternal packageState = getPackageStateInternal(packageName);
        return packageState != null && packageState.getUserStateOrDefault(userId).isInstantApp();
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean wasPackageEverLaunched(String packageName, int userId) {
        PackageStateInternal packageState = getPackageStateInternal(packageName);
        if (packageState == null) {
            throw new IllegalArgumentException("Unknown package: " + packageName);
        }
        return !packageState.getUserStateOrDefault(userId).isNotLaunched();
    }

    @Override // android.content.pm.PackageManagerInternal
    public boolean wasPackageLatestStopped(String packageName, int userId) {
        PackageStateInternal packageState = getPackageStateInternal(packageName);
        if (packageState == null) {
            throw new IllegalArgumentException("Unknown package: " + packageName);
        }
        return packageState.getUserStateOrDefault(userId).isStopped();
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean isEnabledAndMatches(ParsedMainComponent component, long flags, int userId) {
        return PackageStateUtils.isEnabledAndMatches(getPackageStateInternal(component.getPackageName()), component, flags, userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean userNeedsBadging(int userId) {
        return getUserNeedsBadging().get(userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final String getNameForUid(int uid) {
        return snapshot().getNameForUid(uid);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final void requestInstantAppResolutionPhaseTwo(AuxiliaryResolveInfo responseObj, Intent origIntent, String resolvedType, String callingPackage, String callingFeatureId, boolean isRequesterInstantApp, Bundle verificationBundle, int userId) {
        this.mService.requestInstantAppResolutionPhaseTwo(responseObj, origIntent, resolvedType, callingPackage, callingFeatureId, isRequesterInstantApp, verificationBundle, userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final void grantImplicitAccess(int userId, Intent intent, int recipientAppId, int visibleUid, boolean direct) {
        grantImplicitAccess(userId, intent, recipientAppId, visibleUid, direct, false);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final void grantImplicitAccess(int userId, Intent intent, int recipientAppId, int visibleUid, boolean direct, boolean retainOnUpdate) {
        this.mService.grantImplicitAccess(snapshot(), userId, intent, recipientAppId, visibleUid, direct, retainOnUpdate);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean isInstantAppInstallerComponent(ComponentName component) {
        ActivityInfo instantAppInstallerActivity = this.mService.mInstantAppInstallerActivity;
        return instantAppInstallerActivity != null && instantAppInstallerActivity.getComponentName().equals(component);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final void pruneInstantApps() {
        getInstantAppRegistry().pruneInstantApps(snapshot());
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final String getSetupWizardPackageName() {
        return this.mService.mSetupWizardPackage;
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final ResolveInfo resolveIntent(Intent intent, String resolvedType, long flags, long privateResolveFlags, int userId, boolean resolveForStart, int filterCallingUid) {
        return getResolveIntentHelper().resolveIntentInternal(snapshot(), intent, resolvedType, flags, privateResolveFlags, userId, resolveForStart, filterCallingUid);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final ResolveInfo resolveService(Intent intent, String resolvedType, long flags, int userId, int callingUid) {
        return getResolveIntentHelper().resolveServiceInternal(snapshot(), intent, resolvedType, flags, userId, callingUid);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final ProviderInfo resolveContentProvider(String name, long flags, int userId, int callingUid) {
        return snapshot().resolveContentProvider(name, flags, userId, callingUid);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final int getUidTargetSdkVersion(int uid) {
        return snapshot().getUidTargetSdkVersion(uid);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final int getPackageTargetSdkVersion(String packageName) {
        PackageStateInternal packageState = getPackageStateInternal(packageName);
        if (packageState != null && packageState.getPkg() != null) {
            return packageState.getPkg().getTargetSdkVersion();
        }
        return 10000;
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean canAccessInstantApps(int callingUid, int userId) {
        return snapshot().canViewInstantApps(callingUid, userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean canAccessComponent(int callingUid, ComponentName component, int userId) {
        return snapshot().canAccessComponent(callingUid, component, userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean hasInstantApplicationMetadata(String packageName, int userId) {
        return getInstantAppRegistry().hasInstantApplicationMetadata(packageName, userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final SparseArray<String> getAppsWithSharedUserIds() {
        return snapshot().getAppsWithSharedUserIds();
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final String[] getSharedUserPackagesForPackage(String packageName, int userId) {
        return snapshot().getSharedUserPackagesForPackage(packageName, userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final ArrayMap<String, ProcessInfo> getProcessesForUid(int uid) {
        return snapshot().getProcessesForUid(uid);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final int[] getPermissionGids(String permissionName, int userId) {
        return getPermissionManager().getPermissionGids(permissionName, userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean isOnlyCoreApps() {
        return this.mService.isOnlyCoreApps();
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final void freeStorage(String volumeUuid, long bytes, int flags) throws IOException {
        this.mService.freeStorage(volumeUuid, bytes, flags);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final void freeAllAppCacheAboveQuota(String volumeUuid) throws IOException {
        this.mService.freeAllAppCacheAboveQuota(volumeUuid);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final void forEachPackageSetting(Consumer<PackageSetting> actionLocked) {
        this.mService.forEachPackageSetting(actionLocked);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final void forEachPackageState(Consumer<PackageStateInternal> action) {
        this.mService.forEachPackageState(snapshot(), action);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final void forEachPackage(Consumer<AndroidPackage> action) {
        this.mService.forEachPackage(snapshot(), action);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final void forEachInstalledPackage(Consumer<AndroidPackage> action, int userId) {
        this.mService.forEachInstalledPackage(snapshot(), action, userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final ArraySet<String> getEnabledComponents(String packageName, int userId) {
        PackageStateInternal packageState = getPackageStateInternal(packageName);
        if (packageState == null) {
            return new ArraySet<>();
        }
        return packageState.getUserStateOrDefault(userId).getEnabledComponents();
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final ArraySet<String> getDisabledComponents(String packageName, int userId) {
        PackageStateInternal packageState = getPackageStateInternal(packageName);
        if (packageState == null) {
            return new ArraySet<>();
        }
        return packageState.getUserStateOrDefault(userId).getDisabledComponents();
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final int getApplicationEnabledState(String packageName, int userId) {
        PackageStateInternal packageState = getPackageStateInternal(packageName);
        if (packageState == null) {
            return 0;
        }
        return packageState.getUserStateOrDefault(userId).getEnabledState();
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final int getComponentEnabledSetting(ComponentName componentName, int callingUid, int userId) {
        return snapshot().getComponentEnabledSettingInternal(componentName, callingUid, userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final void setEnableRollbackCode(int token, int enableRollbackCode) {
        this.mService.setEnableRollbackCode(token, enableRollbackCode);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final void finishPackageInstall(int token, boolean didLaunch) {
        this.mService.finishPackageInstall(token, didLaunch);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean isApexPackage(String packageName) {
        return getApexManager().isApexPackage(packageName);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final List<String> getApksInApex(String apexPackageName) {
        return getApexManager().getApksInApex(apexPackageName);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean isCallerInstallerOfRecord(AndroidPackage pkg, int callingUid) {
        return snapshot().isCallerInstallerOfRecord(pkg, callingUid);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final List<String> getMimeGroup(String packageName, String mimeGroup) {
        return this.mService.getMimeGroupInternal(snapshot(), packageName, mimeGroup);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean isSystemPackage(String packageName) {
        return packageName.equals(this.mService.ensureSystemPackageName(snapshot(), packageName));
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final void unsuspendForSuspendingPackage(String packageName, int affectedUser) {
        this.mService.unsuspendForSuspendingPackage(snapshot(), packageName, affectedUser);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean isSuspendingAnyPackages(String suspendingPackage, int userId) {
        return snapshot().isSuspendingAnyPackages(suspendingPackage, userId);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final void requestChecksums(String packageName, boolean includeSplits, int optional, int required, List trustedInstallers, IOnChecksumsReadyListener onChecksumsReadyListener, int userId, Executor executor, Handler handler) {
        this.mService.requestChecksumsInternal(snapshot(), packageName, includeSplits, optional, required, trustedInstallers, onChecksumsReadyListener, userId, executor, handler);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final boolean isPackageFrozen(String packageName, int callingUid, int userId) {
        return snapshot().getPackageStartability(this.mService.getSafeMode(), packageName, callingUid, userId) == 3;
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final long deleteOatArtifactsOfPackage(String packageName) {
        return this.mService.deleteOatArtifactsOfPackage(snapshot(), packageName);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final void reconcileAppsData(int userId, int flags, boolean migrateAppsData) {
        getAppDataHelper().reconcileAppsData(userId, flags, migrateAppsData);
    }

    @Override // android.content.pm.PackageManagerInternal
    public ArraySet<PackageStateInternal> getSharedUserPackages(int sharedUserAppId) {
        return snapshot().getSharedUserPackages(sharedUserAppId);
    }

    @Override // android.content.pm.PackageManagerInternal
    public SharedUserApi getSharedUserApi(int sharedUserAppId) {
        return snapshot().getSharedUser(sharedUserAppId);
    }

    @Override // android.content.pm.PackageManagerInternal
    public boolean isUidPrivileged(int uid) {
        return snapshot().isUidPrivileged(uid);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final PackageStateMutator.InitialState recordInitialState() {
        return this.mService.recordInitialState();
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final PackageStateMutator.Result commitPackageStateMutation(PackageStateMutator.InitialState state, Consumer<PackageStateMutator> consumer) {
        return this.mService.commitPackageStateMutation(state, consumer);
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final void shutdown() {
        this.mService.shutdown();
    }

    @Override // android.content.pm.PackageManagerInternal
    @Deprecated
    public final DynamicCodeLogger getDynamicCodeLogger() {
        return getDexManager().getDynamicCodeLogger();
    }
}
