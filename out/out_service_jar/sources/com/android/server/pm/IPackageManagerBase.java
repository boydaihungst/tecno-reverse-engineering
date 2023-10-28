package com.android.server.pm;

import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.IPackageInstaller;
import android.content.pm.IPackageManager;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.InstallSourceInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.KeySet;
import android.content.pm.ModuleInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.SharedLibraryInfo;
import android.content.pm.VersionedPackage;
import android.content.pm.dex.IArtManager;
import android.os.Binder;
import android.os.RemoteException;
import android.os.Trace;
import android.os.UserHandle;
import android.permission.PermissionManager;
import android.provider.Settings;
import com.android.internal.util.CollectionUtils;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.verify.domain.DomainVerificationManagerInternal;
import com.android.server.pm.verify.domain.proxy.DomainVerificationProxyV1;
import java.util.List;
import java.util.Objects;
/* loaded from: classes2.dex */
public abstract class IPackageManagerBase extends IPackageManager.Stub {
    private final Context mContext;
    private final DexOptHelper mDexOptHelper;
    private final DomainVerificationConnection mDomainVerificationConnection;
    private final DomainVerificationManagerInternal mDomainVerificationManager;
    private final PackageInstallerService mInstallerService;
    private final ComponentName mInstantAppResolverSettingsComponent;
    private final ModuleInfoProvider mModuleInfoProvider;
    private final PackageProperty mPackageProperty;
    private final PreferredActivityHelper mPreferredActivityHelper;
    private final String mRequiredSupplementalProcessPackage;
    private final ComponentName mResolveComponentName;
    private final ResolveIntentHelper mResolveIntentHelper;
    private final PackageManagerService mService;
    private final String mServicesExtensionPackageName;
    private final String mSharedSystemSharedLibraryPackageName;

    public IPackageManagerBase(PackageManagerService service, Context context, DexOptHelper dexOptHelper, ModuleInfoProvider moduleInfoProvider, PreferredActivityHelper preferredActivityHelper, ResolveIntentHelper resolveIntentHelper, DomainVerificationManagerInternal domainVerificationManager, DomainVerificationConnection domainVerificationConnection, PackageInstallerService installerService, PackageProperty packageProperty, ComponentName resolveComponentName, ComponentName instantAppResolverSettingsComponent, String requiredSupplementalProcessPackage, String servicesExtensionPackageName, String sharedSystemSharedLibraryPackageName) {
        this.mService = service;
        this.mContext = context;
        this.mDexOptHelper = dexOptHelper;
        this.mModuleInfoProvider = moduleInfoProvider;
        this.mPreferredActivityHelper = preferredActivityHelper;
        this.mResolveIntentHelper = resolveIntentHelper;
        this.mDomainVerificationManager = domainVerificationManager;
        this.mDomainVerificationConnection = domainVerificationConnection;
        this.mInstallerService = installerService;
        this.mPackageProperty = packageProperty;
        this.mResolveComponentName = resolveComponentName;
        this.mInstantAppResolverSettingsComponent = instantAppResolverSettingsComponent;
        this.mRequiredSupplementalProcessPackage = requiredSupplementalProcessPackage;
        this.mServicesExtensionPackageName = servicesExtensionPackageName;
        this.mSharedSystemSharedLibraryPackageName = sharedSystemSharedLibraryPackageName;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public Computer snapshot() {
        return this.mService.snapshotComputer();
    }

    @Deprecated
    public final boolean activitySupportsIntent(ComponentName component, Intent intent, String resolvedType) {
        return snapshot().activitySupportsIntent(this.mResolveComponentName, component, intent, resolvedType);
    }

    @Deprecated
    public final void addCrossProfileIntentFilter(IntentFilter intentFilter, String ownerPackage, int sourceUserId, int targetUserId, int flags) {
        this.mService.addCrossProfileIntentFilter(snapshot(), new WatchedIntentFilter(intentFilter), ownerPackage, sourceUserId, targetUserId, flags);
    }

    @Deprecated
    public final boolean addPermission(PermissionInfo info) {
        return ((PermissionManager) this.mContext.getSystemService(PermissionManager.class)).addPermission(info, false);
    }

    @Deprecated
    public final boolean addPermissionAsync(PermissionInfo info) {
        return ((PermissionManager) this.mContext.getSystemService(PermissionManager.class)).addPermission(info, true);
    }

    @Deprecated
    public final void addPersistentPreferredActivity(IntentFilter filter, ComponentName activity, int userId) {
        this.mPreferredActivityHelper.addPersistentPreferredActivity(new WatchedIntentFilter(filter), activity, userId);
    }

    @Deprecated
    public final void addPreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity, int userId, boolean removeExisting) {
        this.mPreferredActivityHelper.addPreferredActivity(snapshot(), new WatchedIntentFilter(filter), match, set, activity, true, userId, "Adding preferred", removeExisting);
    }

    @Deprecated
    public final boolean canForwardTo(Intent intent, String resolvedType, int sourceUserId, int targetUserId) {
        return snapshot().canForwardTo(intent, resolvedType, sourceUserId, targetUserId);
    }

    @Deprecated
    public final boolean canRequestPackageInstalls(String packageName, int userId) {
        return snapshot().canRequestPackageInstalls(packageName, Binder.getCallingUid(), userId, true);
    }

    @Deprecated
    public final String[] canonicalToCurrentPackageNames(String[] names) {
        return snapshot().canonicalToCurrentPackageNames(names);
    }

    @Deprecated
    public final int checkPermission(String permName, String pkgName, int userId) {
        return this.mService.checkPermission(permName, pkgName, userId);
    }

    @Deprecated
    public final int checkSignatures(String pkg1, String pkg2) {
        return snapshot().checkSignatures(pkg1, pkg2);
    }

    @Deprecated
    public final int checkUidPermission(String permName, int uid) {
        return snapshot().checkUidPermission(permName, uid);
    }

    @Deprecated
    public final int checkUidSignatures(int uid1, int uid2) {
        return snapshot().checkUidSignatures(uid1, uid2);
    }

    @Deprecated
    public final void clearPackagePersistentPreferredActivities(String packageName, int userId) {
        this.mPreferredActivityHelper.clearPackagePersistentPreferredActivities(packageName, userId);
    }

    @Deprecated
    public final void clearPackagePreferredActivities(String packageName) {
        this.mPreferredActivityHelper.clearPackagePreferredActivities(snapshot(), packageName);
    }

    @Deprecated
    public final String[] currentToCanonicalPackageNames(String[] names) {
        return snapshot().currentToCanonicalPackageNames(names);
    }

    @Deprecated
    public final void deleteExistingPackageAsUser(VersionedPackage versionedPackage, IPackageDeleteObserver2 observer, int userId) {
        this.mService.deleteExistingPackageAsUser(versionedPackage, observer, userId);
    }

    @Deprecated
    public final void deletePackageAsUser(String packageName, int versionCode, IPackageDeleteObserver observer, int userId, int flags) {
        deletePackageVersioned(new VersionedPackage(packageName, versionCode), new PackageManager.LegacyPackageDeleteObserver(observer).getBinder(), userId, flags);
    }

    @Deprecated
    public final void deletePackageAsOOBE(String packageName, int versionCode, IPackageDeleteObserver observer, int userId, int flags) {
        this.mService.deletePackageAsOOBE(new VersionedPackage(packageName, versionCode), new PackageManager.LegacyPackageDeleteObserver(observer).getBinder(), userId, flags);
    }

    @Deprecated
    public final void deletePackageVersioned(VersionedPackage versionedPackage, IPackageDeleteObserver2 observer, int userId, int deleteFlags) {
        this.mService.deletePackageVersioned(versionedPackage, observer, userId, deleteFlags);
    }

    @Deprecated
    public final ResolveInfo findPersistentPreferredActivity(Intent intent, int userId) {
        return this.mPreferredActivityHelper.findPersistentPreferredActivity(snapshot(), intent, userId);
    }

    @Deprecated
    public final void forceDexOpt(String packageName) {
        this.mDexOptHelper.forceDexOpt(snapshot(), packageName);
    }

    @Deprecated
    public final ActivityInfo getActivityInfo(ComponentName component, long flags, int userId) {
        return snapshot().getActivityInfo(component, flags, userId);
    }

    @Deprecated
    public final ParceledListSlice<IntentFilter> getAllIntentFilters(String packageName) {
        return snapshot().getAllIntentFilters(packageName);
    }

    @Deprecated
    public final List<String> getAllPackages() {
        return snapshot().getAllPackages();
    }

    @Deprecated
    public final String[] getAppOpPermissionPackages(String permissionName) {
        return snapshot().getAppOpPermissionPackages(permissionName);
    }

    @Deprecated
    public final String getAppPredictionServicePackageName() {
        return this.mService.mAppPredictionServicePackage;
    }

    @Deprecated
    public final int getApplicationEnabledSetting(String packageName, int userId) {
        return snapshot().getApplicationEnabledSetting(packageName, userId);
    }

    @Deprecated
    public final boolean getApplicationHiddenSettingAsUser(String packageName, int userId) {
        return snapshot().getApplicationHiddenSettingAsUser(packageName, userId);
    }

    @Deprecated
    public final ApplicationInfo getApplicationInfo(String packageName, long flags, int userId) {
        return snapshot().getApplicationInfo(packageName, flags, userId);
    }

    @Deprecated
    public final IArtManager getArtManager() {
        return this.mService.mArtManagerService;
    }

    @Deprecated
    public final String getAttentionServicePackageName() {
        return this.mService.ensureSystemPackageName(snapshot(), this.mService.getPackageFromComponentString(17039920));
    }

    @Deprecated
    public final boolean getBlockUninstallForUser(String packageName, int userId) {
        return snapshot().getBlockUninstallForUser(packageName, userId);
    }

    @Deprecated
    public final int getComponentEnabledSetting(ComponentName component, int userId) {
        return snapshot().getComponentEnabledSetting(component, Binder.getCallingUid(), userId);
    }

    @Deprecated
    public final String getContentCaptureServicePackageName() {
        return this.mService.ensureSystemPackageName(snapshot(), this.mService.getPackageFromComponentString(17039925));
    }

    @Deprecated
    public final ParceledListSlice<SharedLibraryInfo> getDeclaredSharedLibraries(String packageName, long flags, int userId) {
        return snapshot().getDeclaredSharedLibraries(packageName, flags, userId);
    }

    @Deprecated
    public final byte[] getDefaultAppsBackup(int userId) {
        return this.mPreferredActivityHelper.getDefaultAppsBackup(userId);
    }

    @Deprecated
    public final String getDefaultTextClassifierPackageName() {
        return this.mService.mDefaultTextClassifierPackage;
    }

    @Deprecated
    public final int getFlagsForUid(int uid) {
        return snapshot().getFlagsForUid(uid);
    }

    @Deprecated
    public final CharSequence getHarmfulAppWarning(String packageName, int userId) {
        return snapshot().getHarmfulAppWarning(packageName, userId);
    }

    @Deprecated
    public final ComponentName getHomeActivities(List<ResolveInfo> allHomeCandidates) {
        Computer snapshot = snapshot();
        if (snapshot.getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        return snapshot.getHomeActivitiesAsUser(allHomeCandidates, UserHandle.getCallingUserId());
    }

    @Deprecated
    public final String getIncidentReportApproverPackageName() {
        return this.mService.mIncidentReportApproverPackage;
    }

    @Deprecated
    public final int getInstallLocation() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "default_install_location", 0);
    }

    @Deprecated
    public final int getInstallReason(String packageName, int userId) {
        return snapshot().getInstallReason(packageName, userId);
    }

    @Deprecated
    public final InstallSourceInfo getInstallSourceInfo(String packageName) {
        return snapshot().getInstallSourceInfo(packageName);
    }

    @Deprecated
    public final ParceledListSlice<ApplicationInfo> getInstalledApplications(long flags, int userId) {
        int callingUid = Binder.getCallingUid();
        return new ParceledListSlice<>(snapshot().getInstalledApplications(flags, userId, callingUid));
    }

    @Deprecated
    public final List<ModuleInfo> getInstalledModules(int flags) {
        return this.mModuleInfoProvider.getInstalledModules(flags);
    }

    @Deprecated
    public final ParceledListSlice<PackageInfo> getInstalledPackages(long flags, int userId) {
        return snapshot().getInstalledPackages(flags, userId);
    }

    @Deprecated
    public final String getInstallerPackageName(String packageName) {
        return snapshot().getInstallerPackageName(packageName);
    }

    @Deprecated
    public final ComponentName getInstantAppInstallerComponent() {
        Computer snapshot = snapshot();
        if (snapshot.getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        return snapshot.getInstantAppInstallerComponent();
    }

    @Deprecated
    public final ComponentName getInstantAppResolverComponent() {
        Computer snapshot = snapshot();
        if (snapshot.getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        return this.mService.getInstantAppResolver(snapshot);
    }

    @Deprecated
    public final ComponentName getInstantAppResolverSettingsComponent() {
        return this.mInstantAppResolverSettingsComponent;
    }

    @Deprecated
    public final InstrumentationInfo getInstrumentationInfo(ComponentName component, int flags) {
        return snapshot().getInstrumentationInfo(component, flags);
    }

    @Deprecated
    public final ParceledListSlice<IntentFilterVerificationInfo> getIntentFilterVerifications(String packageName) {
        return ParceledListSlice.emptyList();
    }

    @Deprecated
    public final int getIntentVerificationStatus(String packageName, int userId) {
        return this.mDomainVerificationManager.getLegacyState(packageName, userId);
    }

    @Deprecated
    public final KeySet getKeySetByAlias(String packageName, String alias) {
        return snapshot().getKeySetByAlias(packageName, alias);
    }

    @Deprecated
    public final ModuleInfo getModuleInfo(String packageName, int flags) {
        return this.mModuleInfoProvider.getModuleInfo(packageName, flags);
    }

    @Deprecated
    public final String getNameForUid(int uid) {
        return snapshot().getNameForUid(uid);
    }

    @Deprecated
    public final String[] getNamesForUids(int[] uids) {
        return snapshot().getNamesForUids(uids);
    }

    @Deprecated
    public final int[] getPackageGids(String packageName, long flags, int userId) {
        return snapshot().getPackageGids(packageName, flags, userId);
    }

    @Deprecated
    public final PackageInfo getPackageInfo(String packageName, long flags, int userId) {
        return snapshot().getPackageInfo(packageName, flags, userId);
    }

    @Deprecated
    public final PackageInfo getPackageInfoVersioned(VersionedPackage versionedPackage, long flags, int userId) {
        return snapshot().getPackageInfoInternal(versionedPackage.getPackageName(), versionedPackage.getLongVersionCode(), flags, Binder.getCallingUid(), userId);
    }

    @Deprecated
    public final IPackageInstaller getPackageInstaller() {
        if (PackageManagerServiceUtils.isSystemOrRoot()) {
            return this.mInstallerService;
        }
        Computer snapshot = snapshot();
        if (snapshot.getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        return this.mInstallerService;
    }

    @Deprecated
    public final void getPackageSizeInfo(String packageName, int userId, IPackageStatsObserver observer) {
        throw new UnsupportedOperationException("Shame on you for calling the hidden API getPackageSizeInfo(). Shame!");
    }

    @Deprecated
    public final int getPackageUid(String packageName, long flags, int userId) {
        return snapshot().getPackageUid(packageName, flags, userId);
    }

    @Deprecated
    public final String[] getPackagesForUid(int uid) {
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(uid);
        snapshot().enforceCrossUserOrProfilePermission(callingUid, userId, false, false, "getPackagesForUid");
        return snapshot().getPackagesForUid(uid);
    }

    @Deprecated
    public final ParceledListSlice<PackageInfo> getPackagesHoldingPermissions(String[] permissions, long flags, int userId) {
        return snapshot().getPackagesHoldingPermissions(permissions, flags, userId);
    }

    @Deprecated
    public final PermissionGroupInfo getPermissionGroupInfo(String groupName, int flags) {
        return this.mService.getPermissionGroupInfo(groupName, flags);
    }

    @Deprecated
    public final ParceledListSlice<ApplicationInfo> getPersistentApplications(int flags) {
        Computer snapshot = snapshot();
        if (snapshot.getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return ParceledListSlice.emptyList();
        }
        return new ParceledListSlice<>(snapshot.getPersistentApplications(isSafeMode(), flags));
    }

    @Deprecated
    public final int getPreferredActivities(List<IntentFilter> outFilters, List<ComponentName> outActivities, String packageName) {
        return this.mPreferredActivityHelper.getPreferredActivities(snapshot(), outFilters, outActivities, packageName);
    }

    @Deprecated
    public final byte[] getPreferredActivityBackup(int userId) {
        return this.mPreferredActivityHelper.getPreferredActivityBackup(userId);
    }

    @Deprecated
    public final int getPrivateFlagsForUid(int uid) {
        return snapshot().getPrivateFlagsForUid(uid);
    }

    @Deprecated
    public final PackageManager.Property getProperty(String propertyName, String packageName, String className) {
        Objects.requireNonNull(propertyName);
        Objects.requireNonNull(packageName);
        PackageStateInternal packageState = snapshot().getPackageStateFiltered(packageName, Binder.getCallingUid(), UserHandle.getCallingUserId());
        if (packageState == null) {
            return null;
        }
        return this.mPackageProperty.getProperty(propertyName, packageName, className);
    }

    @Deprecated
    public final ProviderInfo getProviderInfo(ComponentName component, long flags, int userId) {
        return snapshot().getProviderInfo(component, flags, userId);
    }

    @Deprecated
    public final ActivityInfo getReceiverInfo(ComponentName component, long flags, int userId) {
        return snapshot().getReceiverInfo(component, flags, userId);
    }

    @Deprecated
    public final String getRotationResolverPackageName() {
        return this.mService.ensureSystemPackageName(snapshot(), this.mService.getPackageFromComponentString(17039940));
    }

    @Deprecated
    public final ServiceInfo getServiceInfo(ComponentName component, long flags, int userId) {
        return snapshot().getServiceInfo(component, flags, userId);
    }

    @Deprecated
    public final String getServicesSystemSharedLibraryPackageName() {
        return this.mServicesExtensionPackageName;
    }

    @Deprecated
    public final String getSetupWizardPackageName() {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Non-system caller");
        }
        return this.mService.mSetupWizardPackage;
    }

    @Deprecated
    public final ParceledListSlice<SharedLibraryInfo> getSharedLibraries(String packageName, long flags, int userId) {
        return snapshot().getSharedLibraries(packageName, flags, userId);
    }

    @Deprecated
    public final String getSharedSystemSharedLibraryPackageName() {
        return this.mSharedSystemSharedLibraryPackageName;
    }

    @Deprecated
    public final KeySet getSigningKeySet(String packageName) {
        return snapshot().getSigningKeySet(packageName);
    }

    @Deprecated
    public final String getSdkSandboxPackageName() {
        return this.mService.getSdkSandboxPackageName();
    }

    @Deprecated
    public final String getSystemCaptionsServicePackageName() {
        return this.mService.ensureSystemPackageName(snapshot(), this.mService.getPackageFromComponentString(17039945));
    }

    @Deprecated
    public final String[] getSystemSharedLibraryNames() {
        return snapshot().getSystemSharedLibraryNames();
    }

    @Deprecated
    public final String getSystemTextClassifierPackageName() {
        return this.mService.mSystemTextClassifierPackageName;
    }

    @Deprecated
    public final int getTargetSdkVersion(String packageName) {
        return snapshot().getTargetSdkVersion(packageName);
    }

    @Deprecated
    public final int getUidForSharedUser(String sharedUserName) {
        return snapshot().getUidForSharedUser(sharedUserName);
    }

    @Deprecated
    public final String getWellbeingPackageName() {
        long identity = Binder.clearCallingIdentity();
        try {
            return (String) CollectionUtils.firstOrNull(((RoleManager) this.mContext.getSystemService(RoleManager.class)).getRoleHolders("android.app.role.SYSTEM_WELLBEING"));
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    @Deprecated
    public final void grantRuntimePermission(String packageName, String permName, int userId) {
        ((PermissionManager) this.mContext.getSystemService(PermissionManager.class)).grantRuntimePermission(packageName, permName, UserHandle.of(userId));
    }

    @Deprecated
    public final boolean hasSigningCertificate(String packageName, byte[] certificate, int type) {
        return snapshot().hasSigningCertificate(packageName, certificate, type);
    }

    @Deprecated
    public final boolean hasSystemFeature(String name, int version) {
        return this.mService.hasSystemFeature(name, version);
    }

    @Deprecated
    public final boolean hasSystemUidErrors() {
        return false;
    }

    @Deprecated
    public final boolean hasUidSigningCertificate(int uid, byte[] certificate, int type) {
        return snapshot().hasUidSigningCertificate(uid, certificate, type);
    }

    @Deprecated
    public final boolean isDeviceUpgrading() {
        return this.mService.isDeviceUpgrading();
    }

    @Deprecated
    public final boolean isFirstBoot() {
        return this.mService.isFirstBoot();
    }

    @Deprecated
    public final boolean isInstantApp(String packageName, int userId) {
        return snapshot().isInstantApp(packageName, userId);
    }

    @Deprecated
    public final boolean isOnlyCoreApps() {
        return this.mService.isOnlyCoreApps();
    }

    @Deprecated
    public final boolean isPackageAvailable(String packageName, int userId) {
        return snapshot().isPackageAvailable(packageName, userId);
    }

    @Deprecated
    public final boolean isPackageDeviceAdminOnAnyUser(String packageName) {
        return this.mService.isPackageDeviceAdminOnAnyUser(snapshot(), packageName);
    }

    @Deprecated
    public final boolean isPackageSignedByKeySet(String packageName, KeySet ks) {
        return snapshot().isPackageSignedByKeySet(packageName, ks);
    }

    @Deprecated
    public final boolean isPackageSignedByKeySetExactly(String packageName, KeySet ks) {
        return snapshot().isPackageSignedByKeySetExactly(packageName, ks);
    }

    @Deprecated
    public final boolean isPackageSuspendedForUser(String packageName, int userId) {
        return snapshot().isPackageSuspendedForUser(packageName, userId);
    }

    @Deprecated
    public final boolean isSafeMode() {
        return this.mService.getSafeMode();
    }

    @Deprecated
    public final boolean isStorageLow() {
        return this.mService.isStorageLow();
    }

    @Deprecated
    public final boolean isUidPrivileged(int uid) {
        return snapshot().isUidPrivileged(uid);
    }

    @Deprecated
    public final boolean performDexOptMode(String packageName, boolean checkProfiles, String targetCompilerFilter, boolean force, boolean bootComplete, String splitName) {
        Computer snapshot = snapshot();
        return this.mDexOptHelper.performDexOptMode(snapshot, packageName, checkProfiles, targetCompilerFilter, force, bootComplete, splitName);
    }

    @Deprecated
    public final boolean performDexOptSecondary(String packageName, String compilerFilter, boolean force) {
        return this.mDexOptHelper.performDexOptSecondary(packageName, compilerFilter, force);
    }

    @Deprecated
    public final ParceledListSlice<ResolveInfo> queryIntentActivities(Intent intent, String resolvedType, long flags, int userId) {
        try {
            Trace.traceBegin(262144L, "queryIntentActivities");
            return new ParceledListSlice<>(snapshot().queryIntentActivitiesInternal(intent, resolvedType, flags, userId));
        } finally {
            Trace.traceEnd(262144L);
        }
    }

    @Deprecated
    public final ParceledListSlice<ProviderInfo> queryContentProviders(String processName, int uid, long flags, String metaDataKey) {
        return snapshot().queryContentProviders(processName, uid, flags, metaDataKey);
    }

    @Deprecated
    public final ParceledListSlice<InstrumentationInfo> queryInstrumentation(String targetPackage, int flags) {
        return snapshot().queryInstrumentation(targetPackage, flags);
    }

    @Deprecated
    public final ParceledListSlice<ResolveInfo> queryIntentActivityOptions(ComponentName caller, Intent[] specifics, String[] specificTypes, Intent intent, String resolvedType, long flags, int userId) {
        return new ParceledListSlice<>(this.mResolveIntentHelper.queryIntentActivityOptionsInternal(snapshot(), caller, specifics, specificTypes, intent, resolvedType, flags, userId));
    }

    @Deprecated
    public final ParceledListSlice<ResolveInfo> queryIntentContentProviders(Intent intent, String resolvedType, long flags, int userId) {
        return new ParceledListSlice<>(this.mResolveIntentHelper.queryIntentContentProvidersInternal(snapshot(), intent, resolvedType, flags, userId));
    }

    @Deprecated
    public final ParceledListSlice<ResolveInfo> queryIntentReceivers(Intent intent, String resolvedType, long flags, int userId) {
        return new ParceledListSlice<>(this.mResolveIntentHelper.queryIntentReceiversInternal(snapshot(), intent, resolvedType, flags, userId, Binder.getCallingUid()));
    }

    @Deprecated
    public final ParceledListSlice<ResolveInfo> queryIntentServices(Intent intent, String resolvedType, long flags, int userId) {
        int callingUid = Binder.getCallingUid();
        return new ParceledListSlice<>(snapshot().queryIntentServicesInternal(intent, resolvedType, flags, userId, callingUid, false));
    }

    @Deprecated
    public final void querySyncProviders(List<String> outNames, List<ProviderInfo> outInfo) {
        snapshot().querySyncProviders(isSafeMode(), outNames, outInfo);
    }

    @Deprecated
    public final void removePermission(String permName) {
        ((PermissionManager) this.mContext.getSystemService(PermissionManager.class)).removePermission(permName);
    }

    @Deprecated
    public final void replacePreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity, int userId) {
        this.mPreferredActivityHelper.replacePreferredActivity(snapshot(), new WatchedIntentFilter(filter), match, set, activity, userId);
    }

    @Deprecated
    public final ProviderInfo resolveContentProvider(String name, long flags, int userId) {
        return snapshot().resolveContentProvider(name, flags, userId, Binder.getCallingUid());
    }

    @Deprecated
    public final void resetApplicationPreferences(int userId) {
        this.mPreferredActivityHelper.resetApplicationPreferences(userId);
    }

    @Deprecated
    public final ResolveInfo resolveIntent(Intent intent, String resolvedType, long flags, int userId) {
        return this.mResolveIntentHelper.resolveIntentInternal(snapshot(), intent, resolvedType, flags, 0L, userId, false, Binder.getCallingUid());
    }

    @Deprecated
    public final ResolveInfo resolveService(Intent intent, String resolvedType, long flags, int userId) {
        int callingUid = Binder.getCallingUid();
        return this.mResolveIntentHelper.resolveServiceInternal(snapshot(), intent, resolvedType, flags, userId, callingUid);
    }

    @Deprecated
    public final void restoreDefaultApps(byte[] backup, int userId) {
        this.mPreferredActivityHelper.restoreDefaultApps(backup, userId);
    }

    @Deprecated
    public final void restorePreferredActivities(byte[] backup, int userId) {
        this.mPreferredActivityHelper.restorePreferredActivities(backup, userId);
    }

    @Deprecated
    public final void setHomeActivity(ComponentName comp, int userId) {
        this.mPreferredActivityHelper.setHomeActivity(snapshot(), comp, userId);
    }

    @Deprecated
    public final void setLastChosenActivity(Intent intent, String resolvedType, int flags, IntentFilter filter, int match, ComponentName activity) {
        this.mPreferredActivityHelper.setLastChosenActivity(snapshot(), intent, resolvedType, flags, new WatchedIntentFilter(filter), match, activity);
    }

    @Deprecated
    public final boolean updateIntentVerificationStatus(String packageName, int status, int userId) {
        return this.mDomainVerificationManager.setLegacyUserState(packageName, userId, status);
    }

    @Deprecated
    public final void verifyIntentFilter(int id, int verificationCode, List<String> failedDomains) {
        DomainVerificationProxyV1.queueLegacyVerifyResult(this.mContext, this.mDomainVerificationConnection, id, verificationCode, failedDomains, Binder.getCallingUid());
    }

    @Deprecated
    public final boolean canPackageQuery(String sourcePackageName, String targetPackageName, int userId) {
        return snapshot().canPackageQuery(sourcePackageName, targetPackageName, userId);
    }

    @Deprecated
    public final void deletePreloadsFileCache() throws RemoteException {
        this.mService.deletePreloadsFileCache();
    }

    @Deprecated
    public final void setSystemAppHiddenUntilInstalled(String packageName, boolean hidden) throws RemoteException {
        this.mService.setSystemAppHiddenUntilInstalled(snapshot(), packageName, hidden);
    }

    @Deprecated
    public final boolean setSystemAppInstallState(String packageName, boolean installed, int userId) throws RemoteException {
        return this.mService.setSystemAppInstallState(snapshot(), packageName, installed, userId);
    }

    @Deprecated
    public final void finishPackageInstall(int token, boolean didLaunch) throws RemoteException {
        this.mService.finishPackageInstall(token, didLaunch);
    }
}
