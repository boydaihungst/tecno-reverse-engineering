package android.app.admin;

import android.accounts.Account;
import android.app.IApplicationThread;
import android.app.IServiceConnection;
import android.app.admin.StartInstallingUpdateCallback;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageDataObserver;
import android.content.pm.ParceledListSlice;
import android.content.pm.StringParceledListSlice;
import android.graphics.Bitmap;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.UserHandle;
import android.security.keymaster.KeymasterCertificateChain;
import android.security.keystore.ParcelableKeyGenParameterSpec;
import android.telephony.data.ApnSetting;
import android.text.TextUtils;
import com.android.internal.infra.AndroidFuture;
import java.util.List;
/* loaded from: classes.dex */
public interface IDevicePolicyManager extends IInterface {
    void acknowledgeDeviceCompliant() throws RemoteException;

    void acknowledgeNewUserDisclaimer(int i) throws RemoteException;

    void addCrossProfileIntentFilter(ComponentName componentName, IntentFilter intentFilter, int i) throws RemoteException;

    boolean addCrossProfileWidgetProvider(ComponentName componentName, String str) throws RemoteException;

    int addOverrideApn(ComponentName componentName, ApnSetting apnSetting) throws RemoteException;

    void addPersistentPreferredActivity(ComponentName componentName, IntentFilter intentFilter, ComponentName componentName2) throws RemoteException;

    boolean approveCaCert(String str, int i, boolean z) throws RemoteException;

    boolean bindDeviceAdminServiceAsUser(ComponentName componentName, IApplicationThread iApplicationThread, IBinder iBinder, Intent intent, IServiceConnection iServiceConnection, int i, int i2) throws RemoteException;

    boolean canAdminGrantSensorsPermissionsForUser(int i) throws RemoteException;

    boolean canProfileOwnerResetPasswordWhenLocked(int i) throws RemoteException;

    boolean canUsbDataSignalingBeDisabled() throws RemoteException;

    boolean checkDeviceIdentifierAccess(String str, int i, int i2) throws RemoteException;

    int checkProvisioningPrecondition(String str, String str2) throws RemoteException;

    void choosePrivateKeyAlias(int i, Uri uri, String str, IBinder iBinder) throws RemoteException;

    void clearApplicationUserData(ComponentName componentName, String str, IPackageDataObserver iPackageDataObserver) throws RemoteException;

    void clearCrossProfileIntentFilters(ComponentName componentName) throws RemoteException;

    void clearDeviceOwner(String str) throws RemoteException;

    void clearOrganizationIdForUser(int i) throws RemoteException;

    void clearPackagePersistentPreferredActivities(ComponentName componentName, String str) throws RemoteException;

    void clearProfileOwner(ComponentName componentName) throws RemoteException;

    boolean clearResetPasswordToken(ComponentName componentName) throws RemoteException;

    void clearSystemUpdatePolicyFreezePeriodRecord() throws RemoteException;

    Intent createAdminSupportIntent(String str) throws RemoteException;

    UserHandle createAndManageUser(ComponentName componentName, String str, ComponentName componentName2, PersistableBundle persistableBundle, int i) throws RemoteException;

    UserHandle createAndProvisionManagedProfile(ManagedProfileProvisioningParams managedProfileProvisioningParams, String str) throws RemoteException;

    void enableSystemApp(ComponentName componentName, String str, String str2) throws RemoteException;

    int enableSystemAppWithIntent(ComponentName componentName, String str, Intent intent) throws RemoteException;

    void enforceCanManageCaCerts(ComponentName componentName, String str) throws RemoteException;

    void finalizeWorkProfileProvisioning(UserHandle userHandle, Account account) throws RemoteException;

    long forceNetworkLogs() throws RemoteException;

    void forceRemoveActiveAdmin(ComponentName componentName, int i) throws RemoteException;

    long forceSecurityLogs() throws RemoteException;

    void forceUpdateUserSetupComplete(int i) throws RemoteException;

    boolean generateKeyPair(ComponentName componentName, String str, String str2, ParcelableKeyGenParameterSpec parcelableKeyGenParameterSpec, int i, KeymasterCertificateChain keymasterCertificateChain) throws RemoteException;

    String[] getAccountTypesWithManagementDisabled() throws RemoteException;

    String[] getAccountTypesWithManagementDisabledAsUser(int i, boolean z) throws RemoteException;

    List<ComponentName> getActiveAdmins(int i) throws RemoteException;

    List<String> getAffiliationIds(ComponentName componentName) throws RemoteException;

    int getAggregatedPasswordComplexityForUser(int i, boolean z) throws RemoteException;

    List<String> getAllCrossProfilePackages() throws RemoteException;

    List<String> getAlwaysOnVpnLockdownAllowlist(ComponentName componentName) throws RemoteException;

    String getAlwaysOnVpnPackage(ComponentName componentName) throws RemoteException;

    String getAlwaysOnVpnPackageForUser(int i) throws RemoteException;

    Bundle getApplicationRestrictions(ComponentName componentName, String str, String str2) throws RemoteException;

    String getApplicationRestrictionsManagingPackage(ComponentName componentName) throws RemoteException;

    boolean getAutoTimeEnabled(ComponentName componentName) throws RemoteException;

    boolean getAutoTimeRequired() throws RemoteException;

    boolean getAutoTimeZoneEnabled(ComponentName componentName) throws RemoteException;

    List<UserHandle> getBindDeviceAdminTargetUsers(ComponentName componentName) throws RemoteException;

    boolean getBluetoothContactSharingDisabled(ComponentName componentName) throws RemoteException;

    boolean getBluetoothContactSharingDisabledForUser(int i) throws RemoteException;

    boolean getCameraDisabled(ComponentName componentName, int i, boolean z) throws RemoteException;

    String getCertInstallerPackage(ComponentName componentName) throws RemoteException;

    List<String> getCrossProfileCalendarPackages(ComponentName componentName) throws RemoteException;

    List<String> getCrossProfileCalendarPackagesForUser(int i) throws RemoteException;

    boolean getCrossProfileCallerIdDisabled(ComponentName componentName) throws RemoteException;

    boolean getCrossProfileCallerIdDisabledForUser(int i) throws RemoteException;

    boolean getCrossProfileContactsSearchDisabled(ComponentName componentName) throws RemoteException;

    boolean getCrossProfileContactsSearchDisabledForUser(int i) throws RemoteException;

    List<String> getCrossProfilePackages(ComponentName componentName) throws RemoteException;

    List<String> getCrossProfileWidgetProviders(ComponentName componentName) throws RemoteException;

    int getCurrentFailedPasswordAttempts(int i, boolean z) throws RemoteException;

    List<String> getDefaultCrossProfilePackages() throws RemoteException;

    List<String> getDelegatePackages(ComponentName componentName, String str) throws RemoteException;

    List<String> getDelegatedScopes(ComponentName componentName, String str) throws RemoteException;

    ComponentName getDeviceOwnerComponent(boolean z) throws RemoteException;

    CharSequence getDeviceOwnerLockScreenInfo() throws RemoteException;

    String getDeviceOwnerName() throws RemoteException;

    CharSequence getDeviceOwnerOrganizationName() throws RemoteException;

    int getDeviceOwnerType(ComponentName componentName) throws RemoteException;

    int getDeviceOwnerUserId() throws RemoteException;

    List<String> getDisallowedSystemApps(ComponentName componentName, int i, String str) throws RemoteException;

    boolean getDoNotAskCredentialsOnBoot() throws RemoteException;

    ParcelableResource getDrawable(String str, String str2, String str3) throws RemoteException;

    CharSequence getEndUserSessionMessage(ComponentName componentName) throws RemoteException;

    Bundle getEnforcingAdminAndUserDetails(int i, String str) throws RemoteException;

    String getEnrollmentSpecificId(String str) throws RemoteException;

    FactoryResetProtectionPolicy getFactoryResetProtectionPolicy(ComponentName componentName) throws RemoteException;

    boolean getForceEphemeralUsers(ComponentName componentName) throws RemoteException;

    String getGlobalPrivateDnsHost(ComponentName componentName) throws RemoteException;

    int getGlobalPrivateDnsMode(ComponentName componentName) throws RemoteException;

    ComponentName getGlobalProxyAdmin(int i) throws RemoteException;

    List<String> getKeepUninstalledPackages(ComponentName componentName, String str) throws RemoteException;

    ParcelableGranteeMap getKeyPairGrants(String str, String str2) throws RemoteException;

    int getKeyguardDisabledFeatures(ComponentName componentName, int i, boolean z) throws RemoteException;

    long getLastBugReportRequestTime() throws RemoteException;

    long getLastNetworkLogRetrievalTime() throws RemoteException;

    long getLastSecurityLogRetrievalTime() throws RemoteException;

    int getLockTaskFeatures(ComponentName componentName) throws RemoteException;

    String[] getLockTaskPackages(ComponentName componentName) throws RemoteException;

    int getLogoutUserId() throws RemoteException;

    CharSequence getLongSupportMessage(ComponentName componentName) throws RemoteException;

    CharSequence getLongSupportMessageForUser(ComponentName componentName, int i) throws RemoteException;

    long getManagedProfileMaximumTimeOff(ComponentName componentName) throws RemoteException;

    int getMaximumFailedPasswordsForWipe(ComponentName componentName, int i, boolean z) throws RemoteException;

    long getMaximumTimeToLock(ComponentName componentName, int i, boolean z) throws RemoteException;

    List<String> getMeteredDataDisabledPackages(ComponentName componentName) throws RemoteException;

    int getMinimumRequiredWifiSecurityLevel() throws RemoteException;

    int getNearbyAppStreamingPolicy(int i) throws RemoteException;

    int getNearbyNotificationStreamingPolicy(int i) throws RemoteException;

    int getOrganizationColor(ComponentName componentName) throws RemoteException;

    int getOrganizationColorForUser(int i) throws RemoteException;

    CharSequence getOrganizationName(ComponentName componentName) throws RemoteException;

    CharSequence getOrganizationNameForUser(int i) throws RemoteException;

    List<ApnSetting> getOverrideApns(ComponentName componentName) throws RemoteException;

    StringParceledListSlice getOwnerInstalledCaCerts(UserHandle userHandle) throws RemoteException;

    int getPasswordComplexity(boolean z) throws RemoteException;

    long getPasswordExpiration(ComponentName componentName, int i, boolean z) throws RemoteException;

    long getPasswordExpirationTimeout(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordHistoryLength(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordMinimumLength(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordMinimumLetters(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordMinimumLowerCase(ComponentName componentName, int i, boolean z) throws RemoteException;

    PasswordMetrics getPasswordMinimumMetrics(int i, boolean z) throws RemoteException;

    int getPasswordMinimumNonLetter(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordMinimumNumeric(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordMinimumSymbols(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordMinimumUpperCase(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordQuality(ComponentName componentName, int i, boolean z) throws RemoteException;

    SystemUpdateInfo getPendingSystemUpdate(ComponentName componentName) throws RemoteException;

    int getPermissionGrantState(ComponentName componentName, String str, String str2, String str3) throws RemoteException;

    int getPermissionPolicy(ComponentName componentName) throws RemoteException;

    List<String> getPermittedAccessibilityServices(ComponentName componentName) throws RemoteException;

    List<String> getPermittedAccessibilityServicesForUser(int i) throws RemoteException;

    List<String> getPermittedCrossProfileNotificationListeners(ComponentName componentName) throws RemoteException;

    List<String> getPermittedInputMethods(ComponentName componentName, boolean z) throws RemoteException;

    List<String> getPermittedInputMethodsAsUser(int i) throws RemoteException;

    int getPersonalAppsSuspendedReasons(ComponentName componentName) throws RemoteException;

    List<UserHandle> getPolicyManagedProfiles(UserHandle userHandle) throws RemoteException;

    List<PreferentialNetworkServiceConfig> getPreferentialNetworkServiceConfigs() throws RemoteException;

    ComponentName getProfileOwnerAsUser(int i) throws RemoteException;

    String getProfileOwnerName(int i) throws RemoteException;

    ComponentName getProfileOwnerOrDeviceOwnerSupervisionComponent(UserHandle userHandle) throws RemoteException;

    int getProfileWithMinimumFailedPasswordsForWipe(int i, boolean z) throws RemoteException;

    void getRemoveWarning(ComponentName componentName, RemoteCallback remoteCallback, int i) throws RemoteException;

    int getRequiredPasswordComplexity(boolean z) throws RemoteException;

    long getRequiredStrongAuthTimeout(ComponentName componentName, int i, boolean z) throws RemoteException;

    ComponentName getRestrictionsProvider(int i) throws RemoteException;

    boolean getScreenCaptureDisabled(ComponentName componentName, int i, boolean z) throws RemoteException;

    List<UserHandle> getSecondaryUsers(ComponentName componentName) throws RemoteException;

    CharSequence getShortSupportMessage(ComponentName componentName) throws RemoteException;

    CharSequence getShortSupportMessageForUser(ComponentName componentName, int i) throws RemoteException;

    CharSequence getStartUserSessionMessage(ComponentName componentName) throws RemoteException;

    boolean getStorageEncryption(ComponentName componentName, int i) throws RemoteException;

    int getStorageEncryptionStatus(String str, int i) throws RemoteException;

    ParcelableResource getString(String str) throws RemoteException;

    SystemUpdatePolicy getSystemUpdatePolicy() throws RemoteException;

    PersistableBundle getTransferOwnershipBundle() throws RemoteException;

    List<PersistableBundle> getTrustAgentConfiguration(ComponentName componentName, ComponentName componentName2, int i, boolean z) throws RemoteException;

    List<String> getUserControlDisabledPackages(ComponentName componentName) throws RemoteException;

    int getUserProvisioningState() throws RemoteException;

    Bundle getUserRestrictions(ComponentName componentName, boolean z) throws RemoteException;

    String getWifiMacAddress(ComponentName componentName) throws RemoteException;

    WifiSsidPolicy getWifiSsidPolicy() throws RemoteException;

    boolean hasDeviceOwner() throws RemoteException;

    boolean hasGrantedPolicy(ComponentName componentName, int i, int i2) throws RemoteException;

    boolean hasKeyPair(String str, String str2) throws RemoteException;

    boolean hasLockdownAdminConfiguredNetworks(ComponentName componentName) throws RemoteException;

    boolean hasUserSetupCompleted() throws RemoteException;

    boolean installCaCert(ComponentName componentName, String str, byte[] bArr) throws RemoteException;

    boolean installExistingPackage(ComponentName componentName, String str, String str2) throws RemoteException;

    boolean installKeyPair(ComponentName componentName, String str, byte[] bArr, byte[] bArr2, byte[] bArr3, String str2, boolean z, boolean z2) throws RemoteException;

    void installUpdateFromFile(ComponentName componentName, ParcelFileDescriptor parcelFileDescriptor, StartInstallingUpdateCallback startInstallingUpdateCallback) throws RemoteException;

    boolean isAccessibilityServicePermittedByAdmin(ComponentName componentName, String str, int i) throws RemoteException;

    boolean isActivePasswordSufficient(int i, boolean z) throws RemoteException;

    boolean isActivePasswordSufficientForDeviceRequirement() throws RemoteException;

    boolean isAdminActive(ComponentName componentName, int i) throws RemoteException;

    boolean isAffiliatedUser(int i) throws RemoteException;

    boolean isAlwaysOnVpnLockdownEnabled(ComponentName componentName) throws RemoteException;

    boolean isAlwaysOnVpnLockdownEnabledForUser(int i) throws RemoteException;

    boolean isApplicationHidden(ComponentName componentName, String str, String str2, boolean z) throws RemoteException;

    boolean isBackupServiceEnabled(ComponentName componentName) throws RemoteException;

    boolean isCaCertApproved(String str, int i) throws RemoteException;

    boolean isCallerApplicationRestrictionsManagingPackage(String str) throws RemoteException;

    boolean isCallingUserAffiliated() throws RemoteException;

    boolean isCommonCriteriaModeEnabled(ComponentName componentName) throws RemoteException;

    boolean isComplianceAcknowledgementRequired() throws RemoteException;

    boolean isCurrentInputMethodSetByOwner() throws RemoteException;

    boolean isDeviceProvisioned() throws RemoteException;

    boolean isDeviceProvisioningConfigApplied() throws RemoteException;

    boolean isDpcDownloaded() throws RemoteException;

    boolean isEphemeralUser(ComponentName componentName) throws RemoteException;

    boolean isFactoryResetProtectionPolicySupported() throws RemoteException;

    boolean isInputMethodPermittedByAdmin(ComponentName componentName, String str, int i, boolean z) throws RemoteException;

    boolean isKeyPairGrantedToWifiAuth(String str, String str2) throws RemoteException;

    boolean isLockTaskPermitted(String str) throws RemoteException;

    boolean isLogoutEnabled() throws RemoteException;

    boolean isManagedKiosk() throws RemoteException;

    boolean isManagedProfile(ComponentName componentName) throws RemoteException;

    boolean isMasterVolumeMuted(ComponentName componentName) throws RemoteException;

    boolean isMeteredDataDisabledPackageForUser(ComponentName componentName, String str, int i) throws RemoteException;

    boolean isNetworkLoggingEnabled(ComponentName componentName, String str) throws RemoteException;

    boolean isNewUserDisclaimerAcknowledged(int i) throws RemoteException;

    boolean isNotificationListenerServicePermitted(String str, int i) throws RemoteException;

    boolean isOrganizationOwnedDeviceWithManagedProfile() throws RemoteException;

    boolean isOverrideApnEnabled(ComponentName componentName) throws RemoteException;

    boolean isPackageAllowedToAccessCalendarForUser(String str, int i) throws RemoteException;

    boolean isPackageSuspended(ComponentName componentName, String str, String str2) throws RemoteException;

    boolean isPasswordSufficientAfterProfileUnification(int i, int i2) throws RemoteException;

    boolean isProvisioningAllowed(String str, String str2) throws RemoteException;

    boolean isRemovingAdmin(ComponentName componentName, int i) throws RemoteException;

    boolean isResetPasswordTokenActive(ComponentName componentName) throws RemoteException;

    boolean isSafeOperation(int i) throws RemoteException;

    boolean isSecondaryLockscreenEnabled(UserHandle userHandle) throws RemoteException;

    boolean isSecurityLoggingEnabled(ComponentName componentName, String str) throws RemoteException;

    boolean isSupervisionComponent(ComponentName componentName) throws RemoteException;

    boolean isUnattendedManagedKiosk() throws RemoteException;

    boolean isUninstallBlocked(ComponentName componentName, String str) throws RemoteException;

    boolean isUninstallInQueue(String str) throws RemoteException;

    boolean isUsbDataSignalingEnabled(String str) throws RemoteException;

    boolean isUsbDataSignalingEnabledForUser(int i) throws RemoteException;

    boolean isUsingUnifiedPassword(ComponentName componentName) throws RemoteException;

    List<UserHandle> listForegroundAffiliatedUsers() throws RemoteException;

    List<String> listPolicyExemptApps() throws RemoteException;

    void lockNow(int i, boolean z) throws RemoteException;

    int logoutUser(ComponentName componentName) throws RemoteException;

    int logoutUserInternal() throws RemoteException;

    void notifyLockTaskModeChanged(boolean z, String str, int i) throws RemoteException;

    void notifyPendingSystemUpdate(SystemUpdateInfo systemUpdateInfo) throws RemoteException;

    boolean packageHasActiveAdmins(String str, int i) throws RemoteException;

    void provisionFullyManagedDevice(FullyManagedDeviceProvisioningParams fullyManagedDeviceProvisioningParams, String str) throws RemoteException;

    void reboot(ComponentName componentName) throws RemoteException;

    void removeActiveAdmin(ComponentName componentName, int i) throws RemoteException;

    boolean removeCrossProfileWidgetProvider(ComponentName componentName, String str) throws RemoteException;

    boolean removeKeyPair(ComponentName componentName, String str, String str2) throws RemoteException;

    boolean removeOverrideApn(ComponentName componentName, int i) throws RemoteException;

    boolean removeUser(ComponentName componentName, UserHandle userHandle) throws RemoteException;

    void reportFailedBiometricAttempt(int i) throws RemoteException;

    void reportFailedPasswordAttempt(int i) throws RemoteException;

    void reportKeyguardDismissed(int i) throws RemoteException;

    void reportKeyguardSecured(int i) throws RemoteException;

    void reportPasswordChanged(PasswordMetrics passwordMetrics, int i) throws RemoteException;

    void reportSuccessfulBiometricAttempt(int i) throws RemoteException;

    void reportSuccessfulPasswordAttempt(int i) throws RemoteException;

    boolean requestBugreport(ComponentName componentName) throws RemoteException;

    void resetDefaultCrossProfileIntentFilters(int i) throws RemoteException;

    void resetDrawables(List<String> list) throws RemoteException;

    boolean resetPassword(String str, int i) throws RemoteException;

    boolean resetPasswordWithToken(ComponentName componentName, String str, byte[] bArr, int i) throws RemoteException;

    void resetStrings(List<String> list) throws RemoteException;

    List<NetworkEvent> retrieveNetworkLogs(ComponentName componentName, String str, long j) throws RemoteException;

    ParceledListSlice retrievePreRebootSecurityLogs(ComponentName componentName, String str) throws RemoteException;

    ParceledListSlice retrieveSecurityLogs(ComponentName componentName, String str) throws RemoteException;

    void sendLostModeLocationUpdate(AndroidFuture<Boolean> androidFuture) throws RemoteException;

    void setAccountManagementDisabled(ComponentName componentName, String str, boolean z, boolean z2) throws RemoteException;

    void setActiveAdmin(ComponentName componentName, boolean z, int i) throws RemoteException;

    void setAffiliationIds(ComponentName componentName, List<String> list) throws RemoteException;

    boolean setAlwaysOnVpnPackage(ComponentName componentName, String str, boolean z, List<String> list) throws RemoteException;

    boolean setApplicationHidden(ComponentName componentName, String str, String str2, boolean z, boolean z2) throws RemoteException;

    void setApplicationRestrictions(ComponentName componentName, String str, String str2, Bundle bundle) throws RemoteException;

    boolean setApplicationRestrictionsManagingPackage(ComponentName componentName, String str) throws RemoteException;

    void setAutoTimeEnabled(ComponentName componentName, boolean z) throws RemoteException;

    void setAutoTimeRequired(ComponentName componentName, boolean z) throws RemoteException;

    void setAutoTimeZoneEnabled(ComponentName componentName, boolean z) throws RemoteException;

    void setBackupServiceEnabled(ComponentName componentName, boolean z) throws RemoteException;

    void setBluetoothContactSharingDisabled(ComponentName componentName, boolean z) throws RemoteException;

    void setCameraDisabled(ComponentName componentName, boolean z, boolean z2) throws RemoteException;

    void setCertInstallerPackage(ComponentName componentName, String str) throws RemoteException;

    void setCommonCriteriaModeEnabled(ComponentName componentName, boolean z) throws RemoteException;

    void setConfiguredNetworksLockdownState(ComponentName componentName, boolean z) throws RemoteException;

    void setCrossProfileCalendarPackages(ComponentName componentName, List<String> list) throws RemoteException;

    void setCrossProfileCallerIdDisabled(ComponentName componentName, boolean z) throws RemoteException;

    void setCrossProfileContactsSearchDisabled(ComponentName componentName, boolean z) throws RemoteException;

    void setCrossProfilePackages(ComponentName componentName, List<String> list) throws RemoteException;

    void setDefaultSmsApplication(ComponentName componentName, String str, boolean z) throws RemoteException;

    void setDelegatedScopes(ComponentName componentName, String str, List<String> list) throws RemoteException;

    boolean setDeviceOwner(ComponentName componentName, String str, int i, boolean z) throws RemoteException;

    void setDeviceOwnerLockScreenInfo(ComponentName componentName, CharSequence charSequence) throws RemoteException;

    void setDeviceOwnerType(ComponentName componentName, int i) throws RemoteException;

    void setDeviceProvisioningConfigApplied() throws RemoteException;

    void setDpcDownloaded(boolean z) throws RemoteException;

    void setDrawables(List<DevicePolicyDrawableResource> list) throws RemoteException;

    void setDualProfileEnabled(ComponentName componentName, int i) throws RemoteException;

    void setEndUserSessionMessage(ComponentName componentName, CharSequence charSequence) throws RemoteException;

    void setFactoryResetProtectionPolicy(ComponentName componentName, FactoryResetProtectionPolicy factoryResetProtectionPolicy) throws RemoteException;

    void setForceEphemeralUsers(ComponentName componentName, boolean z) throws RemoteException;

    int setGlobalPrivateDns(ComponentName componentName, int i, String str) throws RemoteException;

    ComponentName setGlobalProxy(ComponentName componentName, String str, String str2) throws RemoteException;

    void setGlobalSetting(ComponentName componentName, String str, String str2) throws RemoteException;

    void setKeepUninstalledPackages(ComponentName componentName, String str, List<String> list) throws RemoteException;

    boolean setKeyGrantForApp(ComponentName componentName, String str, String str2, String str3, boolean z) throws RemoteException;

    boolean setKeyGrantToWifiAuth(String str, String str2, boolean z) throws RemoteException;

    boolean setKeyPairCertificate(ComponentName componentName, String str, String str2, byte[] bArr, byte[] bArr2, boolean z) throws RemoteException;

    boolean setKeyguardDisabled(ComponentName componentName, boolean z) throws RemoteException;

    void setKeyguardDisabledFeatures(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setLocationEnabled(ComponentName componentName, boolean z) throws RemoteException;

    void setLockTaskFeatures(ComponentName componentName, int i) throws RemoteException;

    void setLockTaskPackages(ComponentName componentName, String[] strArr) throws RemoteException;

    void setLogoutEnabled(ComponentName componentName, boolean z) throws RemoteException;

    void setLongSupportMessage(ComponentName componentName, CharSequence charSequence) throws RemoteException;

    void setManagedProfileMaximumTimeOff(ComponentName componentName, long j) throws RemoteException;

    void setMasterVolumeMuted(ComponentName componentName, boolean z) throws RemoteException;

    void setMaximumFailedPasswordsForWipe(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setMaximumTimeToLock(ComponentName componentName, long j, boolean z) throws RemoteException;

    List<String> setMeteredDataDisabledPackages(ComponentName componentName, List<String> list) throws RemoteException;

    void setMinimumRequiredWifiSecurityLevel(int i) throws RemoteException;

    void setNearbyAppStreamingPolicy(int i) throws RemoteException;

    void setNearbyNotificationStreamingPolicy(int i) throws RemoteException;

    void setNetworkLoggingEnabled(ComponentName componentName, String str, boolean z) throws RemoteException;

    void setNextOperationSafety(int i, int i2) throws RemoteException;

    void setOrganizationColor(ComponentName componentName, int i) throws RemoteException;

    void setOrganizationColorForUser(int i, int i2) throws RemoteException;

    void setOrganizationIdForUser(String str, String str2, int i) throws RemoteException;

    void setOrganizationName(ComponentName componentName, CharSequence charSequence) throws RemoteException;

    void setOverrideApnsEnabled(ComponentName componentName, boolean z) throws RemoteException;

    String[] setPackagesSuspended(ComponentName componentName, String str, String[] strArr, boolean z) throws RemoteException;

    void setPasswordExpirationTimeout(ComponentName componentName, long j, boolean z) throws RemoteException;

    void setPasswordHistoryLength(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordMinimumLength(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordMinimumLetters(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordMinimumLowerCase(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordMinimumNonLetter(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordMinimumNumeric(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordMinimumSymbols(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordMinimumUpperCase(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordQuality(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPermissionGrantState(ComponentName componentName, String str, String str2, String str3, int i, RemoteCallback remoteCallback) throws RemoteException;

    void setPermissionPolicy(ComponentName componentName, String str, int i) throws RemoteException;

    boolean setPermittedAccessibilityServices(ComponentName componentName, List<String> list) throws RemoteException;

    boolean setPermittedCrossProfileNotificationListeners(ComponentName componentName, List<String> list) throws RemoteException;

    boolean setPermittedInputMethods(ComponentName componentName, List<String> list, boolean z) throws RemoteException;

    void setPersonalAppsSuspended(ComponentName componentName, boolean z) throws RemoteException;

    void setPreferentialNetworkServiceConfigs(List<PreferentialNetworkServiceConfig> list) throws RemoteException;

    void setProfileEnabled(ComponentName componentName) throws RemoteException;

    void setProfileName(ComponentName componentName, String str) throws RemoteException;

    boolean setProfileOwner(ComponentName componentName, String str, int i) throws RemoteException;

    void setProfileOwnerOnOrganizationOwnedDevice(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setRecommendedGlobalProxy(ComponentName componentName, ProxyInfo proxyInfo) throws RemoteException;

    void setRequiredPasswordComplexity(int i, boolean z) throws RemoteException;

    void setRequiredStrongAuthTimeout(ComponentName componentName, long j, boolean z) throws RemoteException;

    boolean setResetPasswordToken(ComponentName componentName, byte[] bArr) throws RemoteException;

    void setRestrictionsProvider(ComponentName componentName, ComponentName componentName2) throws RemoteException;

    void setScreenCaptureDisabled(ComponentName componentName, boolean z, boolean z2) throws RemoteException;

    void setSecondaryLockscreenEnabled(ComponentName componentName, boolean z) throws RemoteException;

    void setSecureSetting(ComponentName componentName, String str, String str2) throws RemoteException;

    void setSecurityLoggingEnabled(ComponentName componentName, String str, boolean z) throws RemoteException;

    void setShortSupportMessage(ComponentName componentName, CharSequence charSequence) throws RemoteException;

    void setStartUserSessionMessage(ComponentName componentName, CharSequence charSequence) throws RemoteException;

    boolean setStatusBarDisabled(ComponentName componentName, boolean z) throws RemoteException;

    int setStorageEncryption(ComponentName componentName, boolean z) throws RemoteException;

    void setStrings(List<DevicePolicyStringResource> list) throws RemoteException;

    void setSystemSetting(ComponentName componentName, String str, String str2) throws RemoteException;

    void setSystemUpdatePolicy(ComponentName componentName, SystemUpdatePolicy systemUpdatePolicy) throws RemoteException;

    boolean setTime(ComponentName componentName, long j) throws RemoteException;

    boolean setTimeZone(ComponentName componentName, String str) throws RemoteException;

    void setTrustAgentConfiguration(ComponentName componentName, ComponentName componentName2, PersistableBundle persistableBundle, boolean z) throws RemoteException;

    void setUninstallBlocked(ComponentName componentName, String str, String str2, boolean z) throws RemoteException;

    void setUsbDataSignalingEnabled(String str, boolean z) throws RemoteException;

    void setUserControlDisabledPackages(ComponentName componentName, List<String> list) throws RemoteException;

    void setUserIcon(ComponentName componentName, Bitmap bitmap) throws RemoteException;

    void setUserProvisioningState(int i, int i2) throws RemoteException;

    void setUserRestriction(ComponentName componentName, String str, boolean z, boolean z2) throws RemoteException;

    void setWifiSsidPolicy(WifiSsidPolicy wifiSsidPolicy) throws RemoteException;

    boolean shouldAllowBypassingDevicePolicyManagementRoleQualification() throws RemoteException;

    void startManagedQuickContact(String str, long j, boolean z, long j2, Intent intent) throws RemoteException;

    int startUserInBackground(ComponentName componentName, UserHandle userHandle) throws RemoteException;

    boolean startViewCalendarEventInManagedProfile(String str, long j, long j2, long j3, boolean z, int i) throws RemoteException;

    int stopUser(ComponentName componentName, UserHandle userHandle) throws RemoteException;

    boolean switchUser(ComponentName componentName, UserHandle userHandle) throws RemoteException;

    void transferOwnership(ComponentName componentName, ComponentName componentName2, PersistableBundle persistableBundle) throws RemoteException;

    void uninstallCaCerts(ComponentName componentName, String str, String[] strArr) throws RemoteException;

    void uninstallPackageWithActiveAdmins(String str) throws RemoteException;

    boolean updateOverrideApn(ComponentName componentName, int i, ApnSetting apnSetting) throws RemoteException;

    void wipeDataWithReason(int i, String str, boolean z) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IDevicePolicyManager {
        @Override // android.app.admin.IDevicePolicyManager
        public void setPasswordQuality(ComponentName who, int quality, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getPasswordQuality(ComponentName who, int userHandle, boolean parent) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setPasswordMinimumLength(ComponentName who, int length, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getPasswordMinimumLength(ComponentName who, int userHandle, boolean parent) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setPasswordMinimumUpperCase(ComponentName who, int length, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getPasswordMinimumUpperCase(ComponentName who, int userHandle, boolean parent) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setPasswordMinimumLowerCase(ComponentName who, int length, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getPasswordMinimumLowerCase(ComponentName who, int userHandle, boolean parent) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setPasswordMinimumLetters(ComponentName who, int length, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getPasswordMinimumLetters(ComponentName who, int userHandle, boolean parent) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setPasswordMinimumNumeric(ComponentName who, int length, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getPasswordMinimumNumeric(ComponentName who, int userHandle, boolean parent) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setPasswordMinimumSymbols(ComponentName who, int length, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getPasswordMinimumSymbols(ComponentName who, int userHandle, boolean parent) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setPasswordMinimumNonLetter(ComponentName who, int length, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getPasswordMinimumNonLetter(ComponentName who, int userHandle, boolean parent) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public PasswordMetrics getPasswordMinimumMetrics(int userHandle, boolean deviceWideOnly) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setPasswordHistoryLength(ComponentName who, int length, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getPasswordHistoryLength(ComponentName who, int userHandle, boolean parent) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setPasswordExpirationTimeout(ComponentName who, long expiration, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public long getPasswordExpirationTimeout(ComponentName who, int userHandle, boolean parent) throws RemoteException {
            return 0L;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public long getPasswordExpiration(ComponentName who, int userHandle, boolean parent) throws RemoteException {
            return 0L;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isActivePasswordSufficient(int userHandle, boolean parent) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isActivePasswordSufficientForDeviceRequirement() throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isPasswordSufficientAfterProfileUnification(int userHandle, int profileUser) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getPasswordComplexity(boolean parent) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setRequiredPasswordComplexity(int passwordComplexity, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getRequiredPasswordComplexity(boolean parent) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getAggregatedPasswordComplexityForUser(int userId, boolean deviceWideOnly) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isUsingUnifiedPassword(ComponentName admin) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getCurrentFailedPasswordAttempts(int userHandle, boolean parent) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getProfileWithMinimumFailedPasswordsForWipe(int userHandle, boolean parent) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setMaximumFailedPasswordsForWipe(ComponentName admin, int num, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getMaximumFailedPasswordsForWipe(ComponentName admin, int userHandle, boolean parent) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean resetPassword(String password, int flags) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setMaximumTimeToLock(ComponentName who, long timeMs, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public long getMaximumTimeToLock(ComponentName who, int userHandle, boolean parent) throws RemoteException {
            return 0L;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setRequiredStrongAuthTimeout(ComponentName who, long timeMs, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public long getRequiredStrongAuthTimeout(ComponentName who, int userId, boolean parent) throws RemoteException {
            return 0L;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void lockNow(int flags, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void wipeDataWithReason(int flags, String wipeReasonForUser, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setFactoryResetProtectionPolicy(ComponentName who, FactoryResetProtectionPolicy policy) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public FactoryResetProtectionPolicy getFactoryResetProtectionPolicy(ComponentName who) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isFactoryResetProtectionPolicySupported() throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void sendLostModeLocationUpdate(AndroidFuture<Boolean> future) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public ComponentName setGlobalProxy(ComponentName admin, String proxySpec, String exclusionList) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public ComponentName getGlobalProxyAdmin(int userHandle) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setRecommendedGlobalProxy(ComponentName admin, ProxyInfo proxyInfo) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int setStorageEncryption(ComponentName who, boolean encrypt) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean getStorageEncryption(ComponentName who, int userHandle) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getStorageEncryptionStatus(String callerPackage, int userHandle) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean requestBugreport(ComponentName who) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setCameraDisabled(ComponentName who, boolean disabled, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean getCameraDisabled(ComponentName who, int userHandle, boolean parent) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setScreenCaptureDisabled(ComponentName who, boolean disabled, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean getScreenCaptureDisabled(ComponentName who, int userHandle, boolean parent) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setNearbyNotificationStreamingPolicy(int policy) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getNearbyNotificationStreamingPolicy(int userId) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setNearbyAppStreamingPolicy(int policy) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getNearbyAppStreamingPolicy(int userId) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setKeyguardDisabledFeatures(ComponentName who, int which, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getKeyguardDisabledFeatures(ComponentName who, int userHandle, boolean parent) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setActiveAdmin(ComponentName policyReceiver, boolean refreshing, int userHandle) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isAdminActive(ComponentName policyReceiver, int userHandle) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<ComponentName> getActiveAdmins(int userHandle) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean packageHasActiveAdmins(String packageName, int userHandle) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void getRemoveWarning(ComponentName policyReceiver, RemoteCallback result, int userHandle) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void removeActiveAdmin(ComponentName policyReceiver, int userHandle) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void forceRemoveActiveAdmin(ComponentName policyReceiver, int userHandle) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean hasGrantedPolicy(ComponentName policyReceiver, int usesPolicy, int userHandle) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void reportPasswordChanged(PasswordMetrics metrics, int userId) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void reportFailedPasswordAttempt(int userHandle) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void reportSuccessfulPasswordAttempt(int userHandle) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void reportFailedBiometricAttempt(int userHandle) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void reportSuccessfulBiometricAttempt(int userHandle) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void reportKeyguardDismissed(int userHandle) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void reportKeyguardSecured(int userHandle) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean setDeviceOwner(ComponentName who, String ownerName, int userId, boolean setProfileOwnerOnCurrentUserIfNecessary) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public ComponentName getDeviceOwnerComponent(boolean callingUserOnly) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean hasDeviceOwner() throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public String getDeviceOwnerName() throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void clearDeviceOwner(String packageName) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getDeviceOwnerUserId() throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean setProfileOwner(ComponentName who, String ownerName, int userHandle) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public ComponentName getProfileOwnerAsUser(int userHandle) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public ComponentName getProfileOwnerOrDeviceOwnerSupervisionComponent(UserHandle userHandle) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isSupervisionComponent(ComponentName who) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public String getProfileOwnerName(int userHandle) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setProfileEnabled(ComponentName who) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setProfileName(ComponentName who, String profileName) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void clearProfileOwner(ComponentName who) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean hasUserSetupCompleted() throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isOrganizationOwnedDeviceWithManagedProfile() throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean checkDeviceIdentifierAccess(String packageName, int pid, int uid) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setDeviceOwnerLockScreenInfo(ComponentName who, CharSequence deviceOwnerInfo) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public CharSequence getDeviceOwnerLockScreenInfo() throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public String[] setPackagesSuspended(ComponentName admin, String callerPackage, String[] packageNames, boolean suspended) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isPackageSuspended(ComponentName admin, String callerPackage, String packageName) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<String> listPolicyExemptApps() throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean installCaCert(ComponentName admin, String callerPackage, byte[] certBuffer) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void uninstallCaCerts(ComponentName admin, String callerPackage, String[] aliases) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void enforceCanManageCaCerts(ComponentName admin, String callerPackage) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean approveCaCert(String alias, int userHandle, boolean approval) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isCaCertApproved(String alias, int userHandle) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean installKeyPair(ComponentName who, String callerPackage, byte[] privKeyBuffer, byte[] certBuffer, byte[] certChainBuffer, String alias, boolean requestAccess, boolean isUserSelectable) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean removeKeyPair(ComponentName who, String callerPackage, String alias) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean hasKeyPair(String callerPackage, String alias) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean generateKeyPair(ComponentName who, String callerPackage, String algorithm, ParcelableKeyGenParameterSpec keySpec, int idAttestationFlags, KeymasterCertificateChain attestationChain) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean setKeyPairCertificate(ComponentName who, String callerPackage, String alias, byte[] certBuffer, byte[] certChainBuffer, boolean isUserSelectable) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void choosePrivateKeyAlias(int uid, Uri uri, String alias, IBinder aliasCallback) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setDelegatedScopes(ComponentName who, String delegatePackage, List<String> scopes) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<String> getDelegatedScopes(ComponentName who, String delegatePackage) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<String> getDelegatePackages(ComponentName who, String scope) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setCertInstallerPackage(ComponentName who, String installerPackage) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public String getCertInstallerPackage(ComponentName who) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean setAlwaysOnVpnPackage(ComponentName who, String vpnPackage, boolean lockdown, List<String> lockdownAllowlist) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public String getAlwaysOnVpnPackage(ComponentName who) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public String getAlwaysOnVpnPackageForUser(int userHandle) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isAlwaysOnVpnLockdownEnabled(ComponentName who) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isAlwaysOnVpnLockdownEnabledForUser(int userHandle) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<String> getAlwaysOnVpnLockdownAllowlist(ComponentName who) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void addPersistentPreferredActivity(ComponentName admin, IntentFilter filter, ComponentName activity) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void clearPackagePersistentPreferredActivities(ComponentName admin, String packageName) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setDefaultSmsApplication(ComponentName admin, String packageName, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setApplicationRestrictions(ComponentName who, String callerPackage, String packageName, Bundle settings) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public Bundle getApplicationRestrictions(ComponentName who, String callerPackage, String packageName) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean setApplicationRestrictionsManagingPackage(ComponentName admin, String packageName) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public String getApplicationRestrictionsManagingPackage(ComponentName admin) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isCallerApplicationRestrictionsManagingPackage(String callerPackage) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setRestrictionsProvider(ComponentName who, ComponentName provider) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public ComponentName getRestrictionsProvider(int userHandle) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setUserRestriction(ComponentName who, String key, boolean enable, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public Bundle getUserRestrictions(ComponentName who, boolean parent) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void addCrossProfileIntentFilter(ComponentName admin, IntentFilter filter, int flags) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void clearCrossProfileIntentFilters(ComponentName admin) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean setPermittedAccessibilityServices(ComponentName admin, List<String> packageList) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<String> getPermittedAccessibilityServices(ComponentName admin) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<String> getPermittedAccessibilityServicesForUser(int userId) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isAccessibilityServicePermittedByAdmin(ComponentName admin, String packageName, int userId) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean setPermittedInputMethods(ComponentName admin, List<String> packageList, boolean parent) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<String> getPermittedInputMethods(ComponentName admin, boolean parent) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<String> getPermittedInputMethodsAsUser(int userId) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isInputMethodPermittedByAdmin(ComponentName admin, String packageName, int userId, boolean parent) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean setPermittedCrossProfileNotificationListeners(ComponentName admin, List<String> packageList) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<String> getPermittedCrossProfileNotificationListeners(ComponentName admin) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isNotificationListenerServicePermitted(String packageName, int userId) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public Intent createAdminSupportIntent(String restriction) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public Bundle getEnforcingAdminAndUserDetails(int userId, String restriction) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean setApplicationHidden(ComponentName admin, String callerPackage, String packageName, boolean hidden, boolean parent) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isApplicationHidden(ComponentName admin, String callerPackage, String packageName, boolean parent) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public UserHandle createAndManageUser(ComponentName who, String name, ComponentName profileOwner, PersistableBundle adminExtras, int flags) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean removeUser(ComponentName who, UserHandle userHandle) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean switchUser(ComponentName who, UserHandle userHandle) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int startUserInBackground(ComponentName who, UserHandle userHandle) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int stopUser(ComponentName who, UserHandle userHandle) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int logoutUser(ComponentName who) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int logoutUserInternal() throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getLogoutUserId() throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<UserHandle> getSecondaryUsers(ComponentName who) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void acknowledgeNewUserDisclaimer(int userId) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isNewUserDisclaimerAcknowledged(int userId) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void enableSystemApp(ComponentName admin, String callerPackage, String packageName) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int enableSystemAppWithIntent(ComponentName admin, String callerPackage, Intent intent) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean installExistingPackage(ComponentName admin, String callerPackage, String packageName) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setAccountManagementDisabled(ComponentName who, String accountType, boolean disabled, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public String[] getAccountTypesWithManagementDisabled() throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public String[] getAccountTypesWithManagementDisabledAsUser(int userId, boolean parent) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setSecondaryLockscreenEnabled(ComponentName who, boolean enabled) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isSecondaryLockscreenEnabled(UserHandle userHandle) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setPreferentialNetworkServiceConfigs(List<PreferentialNetworkServiceConfig> preferentialNetworkServiceConfigs) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<PreferentialNetworkServiceConfig> getPreferentialNetworkServiceConfigs() throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setLockTaskPackages(ComponentName who, String[] packages) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public String[] getLockTaskPackages(ComponentName who) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isLockTaskPermitted(String pkg) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setLockTaskFeatures(ComponentName who, int flags) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getLockTaskFeatures(ComponentName who) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setGlobalSetting(ComponentName who, String setting, String value) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setSystemSetting(ComponentName who, String setting, String value) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setSecureSetting(ComponentName who, String setting, String value) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setConfiguredNetworksLockdownState(ComponentName who, boolean lockdown) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean hasLockdownAdminConfiguredNetworks(ComponentName who) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setLocationEnabled(ComponentName who, boolean locationEnabled) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean setTime(ComponentName who, long millis) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean setTimeZone(ComponentName who, String timeZone) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setMasterVolumeMuted(ComponentName admin, boolean on) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isMasterVolumeMuted(ComponentName admin) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void notifyLockTaskModeChanged(boolean isEnabled, String pkg, int userId) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setUninstallBlocked(ComponentName admin, String callerPackage, String packageName, boolean uninstallBlocked) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isUninstallBlocked(ComponentName admin, String packageName) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setCrossProfileCallerIdDisabled(ComponentName who, boolean disabled) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean getCrossProfileCallerIdDisabled(ComponentName who) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean getCrossProfileCallerIdDisabledForUser(int userId) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setCrossProfileContactsSearchDisabled(ComponentName who, boolean disabled) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean getCrossProfileContactsSearchDisabled(ComponentName who) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean getCrossProfileContactsSearchDisabledForUser(int userId) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void startManagedQuickContact(String lookupKey, long contactId, boolean isContactIdIgnored, long directoryId, Intent originalIntent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setBluetoothContactSharingDisabled(ComponentName who, boolean disabled) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean getBluetoothContactSharingDisabled(ComponentName who) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean getBluetoothContactSharingDisabledForUser(int userId) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setTrustAgentConfiguration(ComponentName admin, ComponentName agent, PersistableBundle args, boolean parent) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<PersistableBundle> getTrustAgentConfiguration(ComponentName admin, ComponentName agent, int userId, boolean parent) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean addCrossProfileWidgetProvider(ComponentName admin, String packageName) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean removeCrossProfileWidgetProvider(ComponentName admin, String packageName) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<String> getCrossProfileWidgetProviders(ComponentName admin) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setAutoTimeRequired(ComponentName who, boolean required) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean getAutoTimeRequired() throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setAutoTimeEnabled(ComponentName who, boolean enabled) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean getAutoTimeEnabled(ComponentName who) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setAutoTimeZoneEnabled(ComponentName who, boolean enabled) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean getAutoTimeZoneEnabled(ComponentName who) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setForceEphemeralUsers(ComponentName who, boolean forceEpehemeralUsers) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean getForceEphemeralUsers(ComponentName who) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isRemovingAdmin(ComponentName adminReceiver, int userHandle) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setUserIcon(ComponentName admin, Bitmap icon) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setSystemUpdatePolicy(ComponentName who, SystemUpdatePolicy policy) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public SystemUpdatePolicy getSystemUpdatePolicy() throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void clearSystemUpdatePolicyFreezePeriodRecord() throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean setKeyguardDisabled(ComponentName admin, boolean disabled) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean setStatusBarDisabled(ComponentName who, boolean disabled) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean getDoNotAskCredentialsOnBoot() throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void notifyPendingSystemUpdate(SystemUpdateInfo info) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public SystemUpdateInfo getPendingSystemUpdate(ComponentName admin) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setPermissionPolicy(ComponentName admin, String callerPackage, int policy) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getPermissionPolicy(ComponentName admin) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setPermissionGrantState(ComponentName admin, String callerPackage, String packageName, String permission, int grantState, RemoteCallback resultReceiver) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getPermissionGrantState(ComponentName admin, String callerPackage, String packageName, String permission) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isProvisioningAllowed(String action, String packageName) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int checkProvisioningPrecondition(String action, String packageName) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setKeepUninstalledPackages(ComponentName admin, String callerPackage, List<String> packageList) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<String> getKeepUninstalledPackages(ComponentName admin, String callerPackage) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isManagedProfile(ComponentName admin) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public String getWifiMacAddress(ComponentName admin) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void reboot(ComponentName admin) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setShortSupportMessage(ComponentName admin, CharSequence message) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public CharSequence getShortSupportMessage(ComponentName admin) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setLongSupportMessage(ComponentName admin, CharSequence message) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public CharSequence getLongSupportMessage(ComponentName admin) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public CharSequence getShortSupportMessageForUser(ComponentName admin, int userHandle) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public CharSequence getLongSupportMessageForUser(ComponentName admin, int userHandle) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setOrganizationColor(ComponentName admin, int color) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setOrganizationColorForUser(int color, int userId) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void clearOrganizationIdForUser(int userHandle) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getOrganizationColor(ComponentName admin) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getOrganizationColorForUser(int userHandle) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setOrganizationName(ComponentName admin, CharSequence title) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public CharSequence getOrganizationName(ComponentName admin) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public CharSequence getDeviceOwnerOrganizationName() throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public CharSequence getOrganizationNameForUser(int userHandle) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getUserProvisioningState() throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setUserProvisioningState(int state, int userHandle) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setAffiliationIds(ComponentName admin, List<String> ids) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<String> getAffiliationIds(ComponentName admin) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isCallingUserAffiliated() throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isAffiliatedUser(int userId) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setSecurityLoggingEnabled(ComponentName admin, String packageName, boolean enabled) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isSecurityLoggingEnabled(ComponentName admin, String packageName) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public ParceledListSlice retrieveSecurityLogs(ComponentName admin, String packageName) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public ParceledListSlice retrievePreRebootSecurityLogs(ComponentName admin, String packageName) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public long forceNetworkLogs() throws RemoteException {
            return 0L;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public long forceSecurityLogs() throws RemoteException {
            return 0L;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isUninstallInQueue(String packageName) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void uninstallPackageWithActiveAdmins(String packageName) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isDeviceProvisioned() throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isDeviceProvisioningConfigApplied() throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setDeviceProvisioningConfigApplied() throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void forceUpdateUserSetupComplete(int userId) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setBackupServiceEnabled(ComponentName admin, boolean enabled) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isBackupServiceEnabled(ComponentName admin) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setNetworkLoggingEnabled(ComponentName admin, String packageName, boolean enabled) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isNetworkLoggingEnabled(ComponentName admin, String packageName) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<NetworkEvent> retrieveNetworkLogs(ComponentName admin, String packageName, long batchToken) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean bindDeviceAdminServiceAsUser(ComponentName admin, IApplicationThread caller, IBinder token, Intent service, IServiceConnection connection, int flags, int targetUserId) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<UserHandle> getBindDeviceAdminTargetUsers(ComponentName admin) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isEphemeralUser(ComponentName admin) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public long getLastSecurityLogRetrievalTime() throws RemoteException {
            return 0L;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public long getLastBugReportRequestTime() throws RemoteException {
            return 0L;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public long getLastNetworkLogRetrievalTime() throws RemoteException {
            return 0L;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean setResetPasswordToken(ComponentName admin, byte[] token) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean clearResetPasswordToken(ComponentName admin) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isResetPasswordTokenActive(ComponentName admin) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean resetPasswordWithToken(ComponentName admin, String password, byte[] token, int flags) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isCurrentInputMethodSetByOwner() throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public StringParceledListSlice getOwnerInstalledCaCerts(UserHandle user) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void clearApplicationUserData(ComponentName admin, String packageName, IPackageDataObserver callback) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setLogoutEnabled(ComponentName admin, boolean enabled) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isLogoutEnabled() throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<String> getDisallowedSystemApps(ComponentName admin, int userId, String provisioningAction) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void transferOwnership(ComponentName admin, ComponentName target, PersistableBundle bundle) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public PersistableBundle getTransferOwnershipBundle() throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setStartUserSessionMessage(ComponentName admin, CharSequence startUserSessionMessage) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setEndUserSessionMessage(ComponentName admin, CharSequence endUserSessionMessage) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public CharSequence getStartUserSessionMessage(ComponentName admin) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public CharSequence getEndUserSessionMessage(ComponentName admin) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<String> setMeteredDataDisabledPackages(ComponentName admin, List<String> packageNames) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<String> getMeteredDataDisabledPackages(ComponentName admin) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int addOverrideApn(ComponentName admin, ApnSetting apnSetting) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean updateOverrideApn(ComponentName admin, int apnId, ApnSetting apnSetting) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean removeOverrideApn(ComponentName admin, int apnId) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<ApnSetting> getOverrideApns(ComponentName admin) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setOverrideApnsEnabled(ComponentName admin, boolean enabled) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isOverrideApnEnabled(ComponentName admin) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isMeteredDataDisabledPackageForUser(ComponentName admin, String packageName, int userId) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int setGlobalPrivateDns(ComponentName admin, int mode, String privateDnsHost) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getGlobalPrivateDnsMode(ComponentName admin) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public String getGlobalPrivateDnsHost(ComponentName admin) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setProfileOwnerOnOrganizationOwnedDevice(ComponentName who, int userId, boolean isProfileOwnerOnOrganizationOwnedDevice) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void installUpdateFromFile(ComponentName admin, ParcelFileDescriptor updateFileDescriptor, StartInstallingUpdateCallback listener) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setCrossProfileCalendarPackages(ComponentName admin, List<String> packageNames) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<String> getCrossProfileCalendarPackages(ComponentName admin) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isPackageAllowedToAccessCalendarForUser(String packageName, int userHandle) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<String> getCrossProfileCalendarPackagesForUser(int userHandle) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setCrossProfilePackages(ComponentName admin, List<String> packageNames) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<String> getCrossProfilePackages(ComponentName admin) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<String> getAllCrossProfilePackages() throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<String> getDefaultCrossProfilePackages() throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isManagedKiosk() throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isUnattendedManagedKiosk() throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean startViewCalendarEventInManagedProfile(String packageName, long eventId, long start, long end, boolean allDay, int flags) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean setKeyGrantForApp(ComponentName admin, String callerPackage, String alias, String packageName, boolean hasGrant) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public ParcelableGranteeMap getKeyPairGrants(String callerPackage, String alias) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean setKeyGrantToWifiAuth(String callerPackage, String alias, boolean hasGrant) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isKeyPairGrantedToWifiAuth(String callerPackage, String alias) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setUserControlDisabledPackages(ComponentName admin, List<String> packages) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<String> getUserControlDisabledPackages(ComponentName admin) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setCommonCriteriaModeEnabled(ComponentName admin, boolean enabled) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isCommonCriteriaModeEnabled(ComponentName admin) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getPersonalAppsSuspendedReasons(ComponentName admin) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setPersonalAppsSuspended(ComponentName admin, boolean suspended) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public long getManagedProfileMaximumTimeOff(ComponentName admin) throws RemoteException {
            return 0L;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setManagedProfileMaximumTimeOff(ComponentName admin, long timeoutMs) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void acknowledgeDeviceCompliant() throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isComplianceAcknowledgementRequired() throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean canProfileOwnerResetPasswordWhenLocked(int userId) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setNextOperationSafety(int operation, int reason) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isSafeOperation(int reason) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public String getEnrollmentSpecificId(String callerPackage) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setOrganizationIdForUser(String callerPackage, String enterpriseId, int userId) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public UserHandle createAndProvisionManagedProfile(ManagedProfileProvisioningParams provisioningParams, String callerPackage) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void provisionFullyManagedDevice(FullyManagedDeviceProvisioningParams provisioningParams, String callerPackage) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void finalizeWorkProfileProvisioning(UserHandle managedProfileUser, Account migratedAccount) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setDeviceOwnerType(ComponentName admin, int deviceOwnerType) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getDeviceOwnerType(ComponentName admin) throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void resetDefaultCrossProfileIntentFilters(int userId) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean canAdminGrantSensorsPermissionsForUser(int userId) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setUsbDataSignalingEnabled(String callerPackage, boolean enabled) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isUsbDataSignalingEnabled(String callerPackage) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isUsbDataSignalingEnabledForUser(int userId) throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean canUsbDataSignalingBeDisabled() throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setMinimumRequiredWifiSecurityLevel(int level) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public int getMinimumRequiredWifiSecurityLevel() throws RemoteException {
            return 0;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setWifiSsidPolicy(WifiSsidPolicy policy) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public WifiSsidPolicy getWifiSsidPolicy() throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<UserHandle> listForegroundAffiliatedUsers() throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setDrawables(List<DevicePolicyDrawableResource> drawables) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void resetDrawables(List<String> drawableIds) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public ParcelableResource getDrawable(String drawableId, String drawableStyle, String drawableSource) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean isDpcDownloaded() throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setDpcDownloaded(boolean downloaded) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setStrings(List<DevicePolicyStringResource> strings) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void resetStrings(List<String> stringIds) throws RemoteException {
        }

        @Override // android.app.admin.IDevicePolicyManager
        public ParcelableResource getString(String stringId) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public boolean shouldAllowBypassingDevicePolicyManagementRoleQualification() throws RemoteException {
            return false;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public List<UserHandle> getPolicyManagedProfiles(UserHandle userHandle) throws RemoteException {
            return null;
        }

        @Override // android.app.admin.IDevicePolicyManager
        public void setDualProfileEnabled(ComponentName who, int userId) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IDevicePolicyManager {
        public static final String DESCRIPTOR = "android.app.admin.IDevicePolicyManager";
        static final int TRANSACTION_acknowledgeDeviceCompliant = 331;
        static final int TRANSACTION_acknowledgeNewUserDisclaimer = 160;
        static final int TRANSACTION_addCrossProfileIntentFilter = 134;
        static final int TRANSACTION_addCrossProfileWidgetProvider = 202;
        static final int TRANSACTION_addOverrideApn = 296;
        static final int TRANSACTION_addPersistentPreferredActivity = 122;
        static final int TRANSACTION_approveCaCert = 103;
        static final int TRANSACTION_bindDeviceAdminServiceAsUser = 272;
        static final int TRANSACTION_canAdminGrantSensorsPermissionsForUser = 344;
        static final int TRANSACTION_canProfileOwnerResetPasswordWhenLocked = 333;
        static final int TRANSACTION_canUsbDataSignalingBeDisabled = 348;
        static final int TRANSACTION_checkDeviceIdentifierAccess = 94;
        static final int TRANSACTION_checkProvisioningPrecondition = 228;
        static final int TRANSACTION_choosePrivateKeyAlias = 110;
        static final int TRANSACTION_clearApplicationUserData = 284;
        static final int TRANSACTION_clearCrossProfileIntentFilters = 135;
        static final int TRANSACTION_clearDeviceOwner = 82;
        static final int TRANSACTION_clearOrganizationIdForUser = 242;
        static final int TRANSACTION_clearPackagePersistentPreferredActivities = 123;
        static final int TRANSACTION_clearProfileOwner = 91;
        static final int TRANSACTION_clearResetPasswordToken = 279;
        static final int TRANSACTION_clearSystemUpdatePolicyFreezePeriodRecord = 217;
        static final int TRANSACTION_createAdminSupportIntent = 147;
        static final int TRANSACTION_createAndManageUser = 151;
        static final int TRANSACTION_createAndProvisionManagedProfile = 338;
        static final int TRANSACTION_enableSystemApp = 162;
        static final int TRANSACTION_enableSystemAppWithIntent = 163;
        static final int TRANSACTION_enforceCanManageCaCerts = 102;
        static final int TRANSACTION_finalizeWorkProfileProvisioning = 340;
        static final int TRANSACTION_forceNetworkLogs = 259;
        static final int TRANSACTION_forceRemoveActiveAdmin = 69;
        static final int TRANSACTION_forceSecurityLogs = 260;
        static final int TRANSACTION_forceUpdateUserSetupComplete = 266;
        static final int TRANSACTION_generateKeyPair = 108;
        static final int TRANSACTION_getAccountTypesWithManagementDisabled = 166;
        static final int TRANSACTION_getAccountTypesWithManagementDisabledAsUser = 167;
        static final int TRANSACTION_getActiveAdmins = 65;
        static final int TRANSACTION_getAffiliationIds = 252;
        static final int TRANSACTION_getAggregatedPasswordComplexityForUser = 29;
        static final int TRANSACTION_getAllCrossProfilePackages = 314;
        static final int TRANSACTION_getAlwaysOnVpnLockdownAllowlist = 121;
        static final int TRANSACTION_getAlwaysOnVpnPackage = 117;
        static final int TRANSACTION_getAlwaysOnVpnPackageForUser = 118;
        static final int TRANSACTION_getApplicationRestrictions = 126;
        static final int TRANSACTION_getApplicationRestrictionsManagingPackage = 128;
        static final int TRANSACTION_getAutoTimeEnabled = 208;
        static final int TRANSACTION_getAutoTimeRequired = 206;
        static final int TRANSACTION_getAutoTimeZoneEnabled = 210;
        static final int TRANSACTION_getBindDeviceAdminTargetUsers = 273;
        static final int TRANSACTION_getBluetoothContactSharingDisabled = 198;
        static final int TRANSACTION_getBluetoothContactSharingDisabledForUser = 199;
        static final int TRANSACTION_getCameraDisabled = 54;
        static final int TRANSACTION_getCertInstallerPackage = 115;
        static final int TRANSACTION_getCrossProfileCalendarPackages = 309;
        static final int TRANSACTION_getCrossProfileCalendarPackagesForUser = 311;
        static final int TRANSACTION_getCrossProfileCallerIdDisabled = 191;
        static final int TRANSACTION_getCrossProfileCallerIdDisabledForUser = 192;
        static final int TRANSACTION_getCrossProfileContactsSearchDisabled = 194;
        static final int TRANSACTION_getCrossProfileContactsSearchDisabledForUser = 195;
        static final int TRANSACTION_getCrossProfilePackages = 313;
        static final int TRANSACTION_getCrossProfileWidgetProviders = 204;
        static final int TRANSACTION_getCurrentFailedPasswordAttempts = 31;
        static final int TRANSACTION_getDefaultCrossProfilePackages = 315;
        static final int TRANSACTION_getDelegatePackages = 113;
        static final int TRANSACTION_getDelegatedScopes = 112;
        static final int TRANSACTION_getDeviceOwnerComponent = 79;
        static final int TRANSACTION_getDeviceOwnerLockScreenInfo = 96;
        static final int TRANSACTION_getDeviceOwnerName = 81;
        static final int TRANSACTION_getDeviceOwnerOrganizationName = 247;
        static final int TRANSACTION_getDeviceOwnerType = 342;
        static final int TRANSACTION_getDeviceOwnerUserId = 83;
        static final int TRANSACTION_getDisallowedSystemApps = 287;
        static final int TRANSACTION_getDoNotAskCredentialsOnBoot = 220;
        static final int TRANSACTION_getDrawable = 356;
        static final int TRANSACTION_getEndUserSessionMessage = 293;
        static final int TRANSACTION_getEnforcingAdminAndUserDetails = 148;
        static final int TRANSACTION_getEnrollmentSpecificId = 336;
        static final int TRANSACTION_getFactoryResetProtectionPolicy = 43;
        static final int TRANSACTION_getForceEphemeralUsers = 212;
        static final int TRANSACTION_getGlobalPrivateDnsHost = 305;
        static final int TRANSACTION_getGlobalPrivateDnsMode = 304;
        static final int TRANSACTION_getGlobalProxyAdmin = 47;
        static final int TRANSACTION_getKeepUninstalledPackages = 230;
        static final int TRANSACTION_getKeyPairGrants = 320;
        static final int TRANSACTION_getKeyguardDisabledFeatures = 62;
        static final int TRANSACTION_getLastBugReportRequestTime = 276;
        static final int TRANSACTION_getLastNetworkLogRetrievalTime = 277;
        static final int TRANSACTION_getLastSecurityLogRetrievalTime = 275;
        static final int TRANSACTION_getLockTaskFeatures = 176;
        static final int TRANSACTION_getLockTaskPackages = 173;
        static final int TRANSACTION_getLogoutUserId = 158;
        static final int TRANSACTION_getLongSupportMessage = 237;
        static final int TRANSACTION_getLongSupportMessageForUser = 239;
        static final int TRANSACTION_getManagedProfileMaximumTimeOff = 329;
        static final int TRANSACTION_getMaximumFailedPasswordsForWipe = 34;
        static final int TRANSACTION_getMaximumTimeToLock = 37;
        static final int TRANSACTION_getMeteredDataDisabledPackages = 295;
        static final int TRANSACTION_getMinimumRequiredWifiSecurityLevel = 350;
        static final int TRANSACTION_getNearbyAppStreamingPolicy = 60;
        static final int TRANSACTION_getNearbyNotificationStreamingPolicy = 58;
        static final int TRANSACTION_getOrganizationColor = 243;
        static final int TRANSACTION_getOrganizationColorForUser = 244;
        static final int TRANSACTION_getOrganizationName = 246;
        static final int TRANSACTION_getOrganizationNameForUser = 248;
        static final int TRANSACTION_getOverrideApns = 299;
        static final int TRANSACTION_getOwnerInstalledCaCerts = 283;
        static final int TRANSACTION_getPasswordComplexity = 26;
        static final int TRANSACTION_getPasswordExpiration = 22;
        static final int TRANSACTION_getPasswordExpirationTimeout = 21;
        static final int TRANSACTION_getPasswordHistoryLength = 19;
        static final int TRANSACTION_getPasswordMinimumLength = 4;
        static final int TRANSACTION_getPasswordMinimumLetters = 10;
        static final int TRANSACTION_getPasswordMinimumLowerCase = 8;
        static final int TRANSACTION_getPasswordMinimumMetrics = 17;
        static final int TRANSACTION_getPasswordMinimumNonLetter = 16;
        static final int TRANSACTION_getPasswordMinimumNumeric = 12;
        static final int TRANSACTION_getPasswordMinimumSymbols = 14;
        static final int TRANSACTION_getPasswordMinimumUpperCase = 6;
        static final int TRANSACTION_getPasswordQuality = 2;
        static final int TRANSACTION_getPendingSystemUpdate = 222;
        static final int TRANSACTION_getPermissionGrantState = 226;
        static final int TRANSACTION_getPermissionPolicy = 224;
        static final int TRANSACTION_getPermittedAccessibilityServices = 137;
        static final int TRANSACTION_getPermittedAccessibilityServicesForUser = 138;
        static final int TRANSACTION_getPermittedCrossProfileNotificationListeners = 145;
        static final int TRANSACTION_getPermittedInputMethods = 141;
        static final int TRANSACTION_getPermittedInputMethodsAsUser = 142;
        static final int TRANSACTION_getPersonalAppsSuspendedReasons = 327;
        static final int TRANSACTION_getPolicyManagedProfiles = 363;
        static final int TRANSACTION_getPreferentialNetworkServiceConfigs = 171;
        static final int TRANSACTION_getProfileOwnerAsUser = 85;
        static final int TRANSACTION_getProfileOwnerName = 88;
        static final int TRANSACTION_getProfileOwnerOrDeviceOwnerSupervisionComponent = 86;
        static final int TRANSACTION_getProfileWithMinimumFailedPasswordsForWipe = 32;
        static final int TRANSACTION_getRemoveWarning = 67;
        static final int TRANSACTION_getRequiredPasswordComplexity = 28;
        static final int TRANSACTION_getRequiredStrongAuthTimeout = 39;
        static final int TRANSACTION_getRestrictionsProvider = 131;
        static final int TRANSACTION_getScreenCaptureDisabled = 56;
        static final int TRANSACTION_getSecondaryUsers = 159;
        static final int TRANSACTION_getShortSupportMessage = 235;
        static final int TRANSACTION_getShortSupportMessageForUser = 238;
        static final int TRANSACTION_getStartUserSessionMessage = 292;
        static final int TRANSACTION_getStorageEncryption = 50;
        static final int TRANSACTION_getStorageEncryptionStatus = 51;
        static final int TRANSACTION_getString = 361;
        static final int TRANSACTION_getSystemUpdatePolicy = 216;
        static final int TRANSACTION_getTransferOwnershipBundle = 289;
        static final int TRANSACTION_getTrustAgentConfiguration = 201;
        static final int TRANSACTION_getUserControlDisabledPackages = 324;
        static final int TRANSACTION_getUserProvisioningState = 249;
        static final int TRANSACTION_getUserRestrictions = 133;
        static final int TRANSACTION_getWifiMacAddress = 232;
        static final int TRANSACTION_getWifiSsidPolicy = 352;
        static final int TRANSACTION_hasDeviceOwner = 80;
        static final int TRANSACTION_hasGrantedPolicy = 70;
        static final int TRANSACTION_hasKeyPair = 107;
        static final int TRANSACTION_hasLockdownAdminConfiguredNetworks = 181;
        static final int TRANSACTION_hasUserSetupCompleted = 92;
        static final int TRANSACTION_installCaCert = 100;
        static final int TRANSACTION_installExistingPackage = 164;
        static final int TRANSACTION_installKeyPair = 105;
        static final int TRANSACTION_installUpdateFromFile = 307;
        static final int TRANSACTION_isAccessibilityServicePermittedByAdmin = 139;
        static final int TRANSACTION_isActivePasswordSufficient = 23;
        static final int TRANSACTION_isActivePasswordSufficientForDeviceRequirement = 24;
        static final int TRANSACTION_isAdminActive = 64;
        static final int TRANSACTION_isAffiliatedUser = 254;
        static final int TRANSACTION_isAlwaysOnVpnLockdownEnabled = 119;
        static final int TRANSACTION_isAlwaysOnVpnLockdownEnabledForUser = 120;
        static final int TRANSACTION_isApplicationHidden = 150;
        static final int TRANSACTION_isBackupServiceEnabled = 268;
        static final int TRANSACTION_isCaCertApproved = 104;
        static final int TRANSACTION_isCallerApplicationRestrictionsManagingPackage = 129;
        static final int TRANSACTION_isCallingUserAffiliated = 253;
        static final int TRANSACTION_isCommonCriteriaModeEnabled = 326;
        static final int TRANSACTION_isComplianceAcknowledgementRequired = 332;
        static final int TRANSACTION_isCurrentInputMethodSetByOwner = 282;
        static final int TRANSACTION_isDeviceProvisioned = 263;
        static final int TRANSACTION_isDeviceProvisioningConfigApplied = 264;
        static final int TRANSACTION_isDpcDownloaded = 357;
        static final int TRANSACTION_isEphemeralUser = 274;
        static final int TRANSACTION_isFactoryResetProtectionPolicySupported = 44;
        static final int TRANSACTION_isInputMethodPermittedByAdmin = 143;
        static final int TRANSACTION_isKeyPairGrantedToWifiAuth = 322;
        static final int TRANSACTION_isLockTaskPermitted = 174;
        static final int TRANSACTION_isLogoutEnabled = 286;
        static final int TRANSACTION_isManagedKiosk = 316;
        static final int TRANSACTION_isManagedProfile = 231;
        static final int TRANSACTION_isMasterVolumeMuted = 186;
        static final int TRANSACTION_isMeteredDataDisabledPackageForUser = 302;
        static final int TRANSACTION_isNetworkLoggingEnabled = 270;
        static final int TRANSACTION_isNewUserDisclaimerAcknowledged = 161;
        static final int TRANSACTION_isNotificationListenerServicePermitted = 146;
        static final int TRANSACTION_isOrganizationOwnedDeviceWithManagedProfile = 93;
        static final int TRANSACTION_isOverrideApnEnabled = 301;
        static final int TRANSACTION_isPackageAllowedToAccessCalendarForUser = 310;
        static final int TRANSACTION_isPackageSuspended = 98;
        static final int TRANSACTION_isPasswordSufficientAfterProfileUnification = 25;
        static final int TRANSACTION_isProvisioningAllowed = 227;
        static final int TRANSACTION_isRemovingAdmin = 213;
        static final int TRANSACTION_isResetPasswordTokenActive = 280;
        static final int TRANSACTION_isSafeOperation = 335;
        static final int TRANSACTION_isSecondaryLockscreenEnabled = 169;
        static final int TRANSACTION_isSecurityLoggingEnabled = 256;
        static final int TRANSACTION_isSupervisionComponent = 87;
        static final int TRANSACTION_isUnattendedManagedKiosk = 317;
        static final int TRANSACTION_isUninstallBlocked = 189;
        static final int TRANSACTION_isUninstallInQueue = 261;
        static final int TRANSACTION_isUsbDataSignalingEnabled = 346;
        static final int TRANSACTION_isUsbDataSignalingEnabledForUser = 347;
        static final int TRANSACTION_isUsingUnifiedPassword = 30;
        static final int TRANSACTION_listForegroundAffiliatedUsers = 353;
        static final int TRANSACTION_listPolicyExemptApps = 99;
        static final int TRANSACTION_lockNow = 40;
        static final int TRANSACTION_logoutUser = 156;
        static final int TRANSACTION_logoutUserInternal = 157;
        static final int TRANSACTION_notifyLockTaskModeChanged = 187;
        static final int TRANSACTION_notifyPendingSystemUpdate = 221;
        static final int TRANSACTION_packageHasActiveAdmins = 66;
        static final int TRANSACTION_provisionFullyManagedDevice = 339;
        static final int TRANSACTION_reboot = 233;
        static final int TRANSACTION_removeActiveAdmin = 68;
        static final int TRANSACTION_removeCrossProfileWidgetProvider = 203;
        static final int TRANSACTION_removeKeyPair = 106;
        static final int TRANSACTION_removeOverrideApn = 298;
        static final int TRANSACTION_removeUser = 152;
        static final int TRANSACTION_reportFailedBiometricAttempt = 74;
        static final int TRANSACTION_reportFailedPasswordAttempt = 72;
        static final int TRANSACTION_reportKeyguardDismissed = 76;
        static final int TRANSACTION_reportKeyguardSecured = 77;
        static final int TRANSACTION_reportPasswordChanged = 71;
        static final int TRANSACTION_reportSuccessfulBiometricAttempt = 75;
        static final int TRANSACTION_reportSuccessfulPasswordAttempt = 73;
        static final int TRANSACTION_requestBugreport = 52;
        static final int TRANSACTION_resetDefaultCrossProfileIntentFilters = 343;
        static final int TRANSACTION_resetDrawables = 355;
        static final int TRANSACTION_resetPassword = 35;
        static final int TRANSACTION_resetPasswordWithToken = 281;
        static final int TRANSACTION_resetStrings = 360;
        static final int TRANSACTION_retrieveNetworkLogs = 271;
        static final int TRANSACTION_retrievePreRebootSecurityLogs = 258;
        static final int TRANSACTION_retrieveSecurityLogs = 257;
        static final int TRANSACTION_sendLostModeLocationUpdate = 45;
        static final int TRANSACTION_setAccountManagementDisabled = 165;
        static final int TRANSACTION_setActiveAdmin = 63;
        static final int TRANSACTION_setAffiliationIds = 251;
        static final int TRANSACTION_setAlwaysOnVpnPackage = 116;
        static final int TRANSACTION_setApplicationHidden = 149;
        static final int TRANSACTION_setApplicationRestrictions = 125;
        static final int TRANSACTION_setApplicationRestrictionsManagingPackage = 127;
        static final int TRANSACTION_setAutoTimeEnabled = 207;
        static final int TRANSACTION_setAutoTimeRequired = 205;
        static final int TRANSACTION_setAutoTimeZoneEnabled = 209;
        static final int TRANSACTION_setBackupServiceEnabled = 267;
        static final int TRANSACTION_setBluetoothContactSharingDisabled = 197;
        static final int TRANSACTION_setCameraDisabled = 53;
        static final int TRANSACTION_setCertInstallerPackage = 114;
        static final int TRANSACTION_setCommonCriteriaModeEnabled = 325;
        static final int TRANSACTION_setConfiguredNetworksLockdownState = 180;
        static final int TRANSACTION_setCrossProfileCalendarPackages = 308;
        static final int TRANSACTION_setCrossProfileCallerIdDisabled = 190;
        static final int TRANSACTION_setCrossProfileContactsSearchDisabled = 193;
        static final int TRANSACTION_setCrossProfilePackages = 312;
        static final int TRANSACTION_setDefaultSmsApplication = 124;
        static final int TRANSACTION_setDelegatedScopes = 111;
        static final int TRANSACTION_setDeviceOwner = 78;
        static final int TRANSACTION_setDeviceOwnerLockScreenInfo = 95;
        static final int TRANSACTION_setDeviceOwnerType = 341;
        static final int TRANSACTION_setDeviceProvisioningConfigApplied = 265;
        static final int TRANSACTION_setDpcDownloaded = 358;
        static final int TRANSACTION_setDrawables = 354;
        static final int TRANSACTION_setDualProfileEnabled = 364;
        static final int TRANSACTION_setEndUserSessionMessage = 291;
        static final int TRANSACTION_setFactoryResetProtectionPolicy = 42;
        static final int TRANSACTION_setForceEphemeralUsers = 211;
        static final int TRANSACTION_setGlobalPrivateDns = 303;
        static final int TRANSACTION_setGlobalProxy = 46;
        static final int TRANSACTION_setGlobalSetting = 177;
        static final int TRANSACTION_setKeepUninstalledPackages = 229;
        static final int TRANSACTION_setKeyGrantForApp = 319;
        static final int TRANSACTION_setKeyGrantToWifiAuth = 321;
        static final int TRANSACTION_setKeyPairCertificate = 109;
        static final int TRANSACTION_setKeyguardDisabled = 218;
        static final int TRANSACTION_setKeyguardDisabledFeatures = 61;
        static final int TRANSACTION_setLocationEnabled = 182;
        static final int TRANSACTION_setLockTaskFeatures = 175;
        static final int TRANSACTION_setLockTaskPackages = 172;
        static final int TRANSACTION_setLogoutEnabled = 285;
        static final int TRANSACTION_setLongSupportMessage = 236;
        static final int TRANSACTION_setManagedProfileMaximumTimeOff = 330;
        static final int TRANSACTION_setMasterVolumeMuted = 185;
        static final int TRANSACTION_setMaximumFailedPasswordsForWipe = 33;
        static final int TRANSACTION_setMaximumTimeToLock = 36;
        static final int TRANSACTION_setMeteredDataDisabledPackages = 294;
        static final int TRANSACTION_setMinimumRequiredWifiSecurityLevel = 349;
        static final int TRANSACTION_setNearbyAppStreamingPolicy = 59;
        static final int TRANSACTION_setNearbyNotificationStreamingPolicy = 57;
        static final int TRANSACTION_setNetworkLoggingEnabled = 269;
        static final int TRANSACTION_setNextOperationSafety = 334;
        static final int TRANSACTION_setOrganizationColor = 240;
        static final int TRANSACTION_setOrganizationColorForUser = 241;
        static final int TRANSACTION_setOrganizationIdForUser = 337;
        static final int TRANSACTION_setOrganizationName = 245;
        static final int TRANSACTION_setOverrideApnsEnabled = 300;
        static final int TRANSACTION_setPackagesSuspended = 97;
        static final int TRANSACTION_setPasswordExpirationTimeout = 20;
        static final int TRANSACTION_setPasswordHistoryLength = 18;
        static final int TRANSACTION_setPasswordMinimumLength = 3;
        static final int TRANSACTION_setPasswordMinimumLetters = 9;
        static final int TRANSACTION_setPasswordMinimumLowerCase = 7;
        static final int TRANSACTION_setPasswordMinimumNonLetter = 15;
        static final int TRANSACTION_setPasswordMinimumNumeric = 11;
        static final int TRANSACTION_setPasswordMinimumSymbols = 13;
        static final int TRANSACTION_setPasswordMinimumUpperCase = 5;
        static final int TRANSACTION_setPasswordQuality = 1;
        static final int TRANSACTION_setPermissionGrantState = 225;
        static final int TRANSACTION_setPermissionPolicy = 223;
        static final int TRANSACTION_setPermittedAccessibilityServices = 136;
        static final int TRANSACTION_setPermittedCrossProfileNotificationListeners = 144;
        static final int TRANSACTION_setPermittedInputMethods = 140;
        static final int TRANSACTION_setPersonalAppsSuspended = 328;
        static final int TRANSACTION_setPreferentialNetworkServiceConfigs = 170;
        static final int TRANSACTION_setProfileEnabled = 89;
        static final int TRANSACTION_setProfileName = 90;
        static final int TRANSACTION_setProfileOwner = 84;
        static final int TRANSACTION_setProfileOwnerOnOrganizationOwnedDevice = 306;
        static final int TRANSACTION_setRecommendedGlobalProxy = 48;
        static final int TRANSACTION_setRequiredPasswordComplexity = 27;
        static final int TRANSACTION_setRequiredStrongAuthTimeout = 38;
        static final int TRANSACTION_setResetPasswordToken = 278;
        static final int TRANSACTION_setRestrictionsProvider = 130;
        static final int TRANSACTION_setScreenCaptureDisabled = 55;
        static final int TRANSACTION_setSecondaryLockscreenEnabled = 168;
        static final int TRANSACTION_setSecureSetting = 179;
        static final int TRANSACTION_setSecurityLoggingEnabled = 255;
        static final int TRANSACTION_setShortSupportMessage = 234;
        static final int TRANSACTION_setStartUserSessionMessage = 290;
        static final int TRANSACTION_setStatusBarDisabled = 219;
        static final int TRANSACTION_setStorageEncryption = 49;
        static final int TRANSACTION_setStrings = 359;
        static final int TRANSACTION_setSystemSetting = 178;
        static final int TRANSACTION_setSystemUpdatePolicy = 215;
        static final int TRANSACTION_setTime = 183;
        static final int TRANSACTION_setTimeZone = 184;
        static final int TRANSACTION_setTrustAgentConfiguration = 200;
        static final int TRANSACTION_setUninstallBlocked = 188;
        static final int TRANSACTION_setUsbDataSignalingEnabled = 345;
        static final int TRANSACTION_setUserControlDisabledPackages = 323;
        static final int TRANSACTION_setUserIcon = 214;
        static final int TRANSACTION_setUserProvisioningState = 250;
        static final int TRANSACTION_setUserRestriction = 132;
        static final int TRANSACTION_setWifiSsidPolicy = 351;
        static final int TRANSACTION_shouldAllowBypassingDevicePolicyManagementRoleQualification = 362;
        static final int TRANSACTION_startManagedQuickContact = 196;
        static final int TRANSACTION_startUserInBackground = 154;
        static final int TRANSACTION_startViewCalendarEventInManagedProfile = 318;
        static final int TRANSACTION_stopUser = 155;
        static final int TRANSACTION_switchUser = 153;
        static final int TRANSACTION_transferOwnership = 288;
        static final int TRANSACTION_uninstallCaCerts = 101;
        static final int TRANSACTION_uninstallPackageWithActiveAdmins = 262;
        static final int TRANSACTION_updateOverrideApn = 297;
        static final int TRANSACTION_wipeDataWithReason = 41;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IDevicePolicyManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IDevicePolicyManager)) {
                return (IDevicePolicyManager) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public static String getDefaultTransactionName(int transactionCode) {
            switch (transactionCode) {
                case 1:
                    return "setPasswordQuality";
                case 2:
                    return "getPasswordQuality";
                case 3:
                    return "setPasswordMinimumLength";
                case 4:
                    return "getPasswordMinimumLength";
                case 5:
                    return "setPasswordMinimumUpperCase";
                case 6:
                    return "getPasswordMinimumUpperCase";
                case 7:
                    return "setPasswordMinimumLowerCase";
                case 8:
                    return "getPasswordMinimumLowerCase";
                case 9:
                    return "setPasswordMinimumLetters";
                case 10:
                    return "getPasswordMinimumLetters";
                case 11:
                    return "setPasswordMinimumNumeric";
                case 12:
                    return "getPasswordMinimumNumeric";
                case 13:
                    return "setPasswordMinimumSymbols";
                case 14:
                    return "getPasswordMinimumSymbols";
                case 15:
                    return "setPasswordMinimumNonLetter";
                case 16:
                    return "getPasswordMinimumNonLetter";
                case 17:
                    return "getPasswordMinimumMetrics";
                case 18:
                    return "setPasswordHistoryLength";
                case 19:
                    return "getPasswordHistoryLength";
                case 20:
                    return "setPasswordExpirationTimeout";
                case 21:
                    return "getPasswordExpirationTimeout";
                case 22:
                    return "getPasswordExpiration";
                case 23:
                    return "isActivePasswordSufficient";
                case 24:
                    return "isActivePasswordSufficientForDeviceRequirement";
                case 25:
                    return "isPasswordSufficientAfterProfileUnification";
                case 26:
                    return "getPasswordComplexity";
                case 27:
                    return "setRequiredPasswordComplexity";
                case 28:
                    return "getRequiredPasswordComplexity";
                case 29:
                    return "getAggregatedPasswordComplexityForUser";
                case 30:
                    return "isUsingUnifiedPassword";
                case 31:
                    return "getCurrentFailedPasswordAttempts";
                case 32:
                    return "getProfileWithMinimumFailedPasswordsForWipe";
                case 33:
                    return "setMaximumFailedPasswordsForWipe";
                case 34:
                    return "getMaximumFailedPasswordsForWipe";
                case 35:
                    return "resetPassword";
                case 36:
                    return "setMaximumTimeToLock";
                case 37:
                    return "getMaximumTimeToLock";
                case 38:
                    return "setRequiredStrongAuthTimeout";
                case 39:
                    return "getRequiredStrongAuthTimeout";
                case 40:
                    return "lockNow";
                case 41:
                    return "wipeDataWithReason";
                case 42:
                    return "setFactoryResetProtectionPolicy";
                case 43:
                    return "getFactoryResetProtectionPolicy";
                case 44:
                    return "isFactoryResetProtectionPolicySupported";
                case 45:
                    return "sendLostModeLocationUpdate";
                case 46:
                    return "setGlobalProxy";
                case 47:
                    return "getGlobalProxyAdmin";
                case 48:
                    return "setRecommendedGlobalProxy";
                case 49:
                    return "setStorageEncryption";
                case 50:
                    return "getStorageEncryption";
                case 51:
                    return "getStorageEncryptionStatus";
                case 52:
                    return "requestBugreport";
                case 53:
                    return "setCameraDisabled";
                case 54:
                    return "getCameraDisabled";
                case 55:
                    return "setScreenCaptureDisabled";
                case 56:
                    return "getScreenCaptureDisabled";
                case 57:
                    return "setNearbyNotificationStreamingPolicy";
                case 58:
                    return "getNearbyNotificationStreamingPolicy";
                case 59:
                    return "setNearbyAppStreamingPolicy";
                case 60:
                    return "getNearbyAppStreamingPolicy";
                case 61:
                    return "setKeyguardDisabledFeatures";
                case 62:
                    return "getKeyguardDisabledFeatures";
                case 63:
                    return "setActiveAdmin";
                case 64:
                    return "isAdminActive";
                case 65:
                    return "getActiveAdmins";
                case 66:
                    return "packageHasActiveAdmins";
                case 67:
                    return "getRemoveWarning";
                case 68:
                    return "removeActiveAdmin";
                case 69:
                    return "forceRemoveActiveAdmin";
                case 70:
                    return "hasGrantedPolicy";
                case 71:
                    return "reportPasswordChanged";
                case 72:
                    return "reportFailedPasswordAttempt";
                case 73:
                    return "reportSuccessfulPasswordAttempt";
                case 74:
                    return "reportFailedBiometricAttempt";
                case 75:
                    return "reportSuccessfulBiometricAttempt";
                case 76:
                    return "reportKeyguardDismissed";
                case 77:
                    return "reportKeyguardSecured";
                case 78:
                    return "setDeviceOwner";
                case 79:
                    return "getDeviceOwnerComponent";
                case 80:
                    return "hasDeviceOwner";
                case 81:
                    return "getDeviceOwnerName";
                case 82:
                    return "clearDeviceOwner";
                case 83:
                    return "getDeviceOwnerUserId";
                case 84:
                    return "setProfileOwner";
                case 85:
                    return "getProfileOwnerAsUser";
                case 86:
                    return "getProfileOwnerOrDeviceOwnerSupervisionComponent";
                case 87:
                    return "isSupervisionComponent";
                case 88:
                    return "getProfileOwnerName";
                case 89:
                    return "setProfileEnabled";
                case 90:
                    return "setProfileName";
                case 91:
                    return "clearProfileOwner";
                case 92:
                    return "hasUserSetupCompleted";
                case 93:
                    return "isOrganizationOwnedDeviceWithManagedProfile";
                case 94:
                    return "checkDeviceIdentifierAccess";
                case 95:
                    return "setDeviceOwnerLockScreenInfo";
                case 96:
                    return "getDeviceOwnerLockScreenInfo";
                case 97:
                    return "setPackagesSuspended";
                case 98:
                    return "isPackageSuspended";
                case 99:
                    return "listPolicyExemptApps";
                case 100:
                    return "installCaCert";
                case 101:
                    return "uninstallCaCerts";
                case 102:
                    return "enforceCanManageCaCerts";
                case 103:
                    return "approveCaCert";
                case 104:
                    return "isCaCertApproved";
                case 105:
                    return "installKeyPair";
                case 106:
                    return "removeKeyPair";
                case 107:
                    return "hasKeyPair";
                case 108:
                    return "generateKeyPair";
                case 109:
                    return "setKeyPairCertificate";
                case 110:
                    return "choosePrivateKeyAlias";
                case 111:
                    return "setDelegatedScopes";
                case 112:
                    return "getDelegatedScopes";
                case 113:
                    return "getDelegatePackages";
                case 114:
                    return "setCertInstallerPackage";
                case 115:
                    return "getCertInstallerPackage";
                case 116:
                    return "setAlwaysOnVpnPackage";
                case 117:
                    return "getAlwaysOnVpnPackage";
                case 118:
                    return "getAlwaysOnVpnPackageForUser";
                case 119:
                    return "isAlwaysOnVpnLockdownEnabled";
                case 120:
                    return "isAlwaysOnVpnLockdownEnabledForUser";
                case 121:
                    return "getAlwaysOnVpnLockdownAllowlist";
                case 122:
                    return "addPersistentPreferredActivity";
                case 123:
                    return "clearPackagePersistentPreferredActivities";
                case 124:
                    return "setDefaultSmsApplication";
                case 125:
                    return "setApplicationRestrictions";
                case 126:
                    return "getApplicationRestrictions";
                case 127:
                    return "setApplicationRestrictionsManagingPackage";
                case 128:
                    return "getApplicationRestrictionsManagingPackage";
                case 129:
                    return "isCallerApplicationRestrictionsManagingPackage";
                case 130:
                    return "setRestrictionsProvider";
                case 131:
                    return "getRestrictionsProvider";
                case 132:
                    return "setUserRestriction";
                case 133:
                    return "getUserRestrictions";
                case 134:
                    return "addCrossProfileIntentFilter";
                case 135:
                    return "clearCrossProfileIntentFilters";
                case 136:
                    return "setPermittedAccessibilityServices";
                case 137:
                    return "getPermittedAccessibilityServices";
                case 138:
                    return "getPermittedAccessibilityServicesForUser";
                case 139:
                    return "isAccessibilityServicePermittedByAdmin";
                case 140:
                    return "setPermittedInputMethods";
                case 141:
                    return "getPermittedInputMethods";
                case 142:
                    return "getPermittedInputMethodsAsUser";
                case 143:
                    return "isInputMethodPermittedByAdmin";
                case 144:
                    return "setPermittedCrossProfileNotificationListeners";
                case 145:
                    return "getPermittedCrossProfileNotificationListeners";
                case 146:
                    return "isNotificationListenerServicePermitted";
                case 147:
                    return "createAdminSupportIntent";
                case 148:
                    return "getEnforcingAdminAndUserDetails";
                case 149:
                    return "setApplicationHidden";
                case 150:
                    return "isApplicationHidden";
                case 151:
                    return "createAndManageUser";
                case 152:
                    return "removeUser";
                case 153:
                    return "switchUser";
                case 154:
                    return "startUserInBackground";
                case 155:
                    return "stopUser";
                case 156:
                    return "logoutUser";
                case 157:
                    return "logoutUserInternal";
                case 158:
                    return "getLogoutUserId";
                case 159:
                    return "getSecondaryUsers";
                case 160:
                    return "acknowledgeNewUserDisclaimer";
                case 161:
                    return "isNewUserDisclaimerAcknowledged";
                case 162:
                    return "enableSystemApp";
                case 163:
                    return "enableSystemAppWithIntent";
                case 164:
                    return "installExistingPackage";
                case 165:
                    return "setAccountManagementDisabled";
                case 166:
                    return "getAccountTypesWithManagementDisabled";
                case 167:
                    return "getAccountTypesWithManagementDisabledAsUser";
                case 168:
                    return "setSecondaryLockscreenEnabled";
                case 169:
                    return "isSecondaryLockscreenEnabled";
                case 170:
                    return "setPreferentialNetworkServiceConfigs";
                case 171:
                    return "getPreferentialNetworkServiceConfigs";
                case 172:
                    return "setLockTaskPackages";
                case 173:
                    return "getLockTaskPackages";
                case 174:
                    return "isLockTaskPermitted";
                case 175:
                    return "setLockTaskFeatures";
                case 176:
                    return "getLockTaskFeatures";
                case 177:
                    return "setGlobalSetting";
                case 178:
                    return "setSystemSetting";
                case 179:
                    return "setSecureSetting";
                case 180:
                    return "setConfiguredNetworksLockdownState";
                case 181:
                    return "hasLockdownAdminConfiguredNetworks";
                case 182:
                    return "setLocationEnabled";
                case 183:
                    return "setTime";
                case 184:
                    return "setTimeZone";
                case 185:
                    return "setMasterVolumeMuted";
                case 186:
                    return "isMasterVolumeMuted";
                case 187:
                    return "notifyLockTaskModeChanged";
                case 188:
                    return "setUninstallBlocked";
                case 189:
                    return "isUninstallBlocked";
                case 190:
                    return "setCrossProfileCallerIdDisabled";
                case 191:
                    return "getCrossProfileCallerIdDisabled";
                case 192:
                    return "getCrossProfileCallerIdDisabledForUser";
                case 193:
                    return "setCrossProfileContactsSearchDisabled";
                case 194:
                    return "getCrossProfileContactsSearchDisabled";
                case 195:
                    return "getCrossProfileContactsSearchDisabledForUser";
                case 196:
                    return "startManagedQuickContact";
                case 197:
                    return "setBluetoothContactSharingDisabled";
                case 198:
                    return "getBluetoothContactSharingDisabled";
                case 199:
                    return "getBluetoothContactSharingDisabledForUser";
                case 200:
                    return "setTrustAgentConfiguration";
                case 201:
                    return "getTrustAgentConfiguration";
                case 202:
                    return "addCrossProfileWidgetProvider";
                case 203:
                    return "removeCrossProfileWidgetProvider";
                case 204:
                    return "getCrossProfileWidgetProviders";
                case 205:
                    return "setAutoTimeRequired";
                case 206:
                    return "getAutoTimeRequired";
                case 207:
                    return "setAutoTimeEnabled";
                case 208:
                    return "getAutoTimeEnabled";
                case 209:
                    return "setAutoTimeZoneEnabled";
                case 210:
                    return "getAutoTimeZoneEnabled";
                case 211:
                    return "setForceEphemeralUsers";
                case 212:
                    return "getForceEphemeralUsers";
                case 213:
                    return "isRemovingAdmin";
                case 214:
                    return "setUserIcon";
                case 215:
                    return "setSystemUpdatePolicy";
                case 216:
                    return "getSystemUpdatePolicy";
                case 217:
                    return "clearSystemUpdatePolicyFreezePeriodRecord";
                case 218:
                    return "setKeyguardDisabled";
                case 219:
                    return "setStatusBarDisabled";
                case 220:
                    return "getDoNotAskCredentialsOnBoot";
                case 221:
                    return "notifyPendingSystemUpdate";
                case 222:
                    return "getPendingSystemUpdate";
                case 223:
                    return "setPermissionPolicy";
                case 224:
                    return "getPermissionPolicy";
                case 225:
                    return "setPermissionGrantState";
                case 226:
                    return "getPermissionGrantState";
                case 227:
                    return "isProvisioningAllowed";
                case 228:
                    return "checkProvisioningPrecondition";
                case 229:
                    return "setKeepUninstalledPackages";
                case 230:
                    return "getKeepUninstalledPackages";
                case 231:
                    return "isManagedProfile";
                case 232:
                    return "getWifiMacAddress";
                case 233:
                    return "reboot";
                case 234:
                    return "setShortSupportMessage";
                case 235:
                    return "getShortSupportMessage";
                case 236:
                    return "setLongSupportMessage";
                case 237:
                    return "getLongSupportMessage";
                case 238:
                    return "getShortSupportMessageForUser";
                case 239:
                    return "getLongSupportMessageForUser";
                case 240:
                    return "setOrganizationColor";
                case 241:
                    return "setOrganizationColorForUser";
                case 242:
                    return "clearOrganizationIdForUser";
                case 243:
                    return "getOrganizationColor";
                case 244:
                    return "getOrganizationColorForUser";
                case 245:
                    return "setOrganizationName";
                case 246:
                    return "getOrganizationName";
                case 247:
                    return "getDeviceOwnerOrganizationName";
                case 248:
                    return "getOrganizationNameForUser";
                case 249:
                    return "getUserProvisioningState";
                case 250:
                    return "setUserProvisioningState";
                case 251:
                    return "setAffiliationIds";
                case 252:
                    return "getAffiliationIds";
                case 253:
                    return "isCallingUserAffiliated";
                case 254:
                    return "isAffiliatedUser";
                case 255:
                    return "setSecurityLoggingEnabled";
                case 256:
                    return "isSecurityLoggingEnabled";
                case 257:
                    return "retrieveSecurityLogs";
                case 258:
                    return "retrievePreRebootSecurityLogs";
                case 259:
                    return "forceNetworkLogs";
                case 260:
                    return "forceSecurityLogs";
                case 261:
                    return "isUninstallInQueue";
                case 262:
                    return "uninstallPackageWithActiveAdmins";
                case 263:
                    return "isDeviceProvisioned";
                case 264:
                    return "isDeviceProvisioningConfigApplied";
                case 265:
                    return "setDeviceProvisioningConfigApplied";
                case 266:
                    return "forceUpdateUserSetupComplete";
                case 267:
                    return "setBackupServiceEnabled";
                case 268:
                    return "isBackupServiceEnabled";
                case 269:
                    return "setNetworkLoggingEnabled";
                case 270:
                    return "isNetworkLoggingEnabled";
                case 271:
                    return "retrieveNetworkLogs";
                case 272:
                    return "bindDeviceAdminServiceAsUser";
                case 273:
                    return "getBindDeviceAdminTargetUsers";
                case 274:
                    return "isEphemeralUser";
                case 275:
                    return "getLastSecurityLogRetrievalTime";
                case 276:
                    return "getLastBugReportRequestTime";
                case 277:
                    return "getLastNetworkLogRetrievalTime";
                case 278:
                    return "setResetPasswordToken";
                case 279:
                    return "clearResetPasswordToken";
                case 280:
                    return "isResetPasswordTokenActive";
                case 281:
                    return "resetPasswordWithToken";
                case 282:
                    return "isCurrentInputMethodSetByOwner";
                case 283:
                    return "getOwnerInstalledCaCerts";
                case 284:
                    return "clearApplicationUserData";
                case 285:
                    return "setLogoutEnabled";
                case 286:
                    return "isLogoutEnabled";
                case 287:
                    return "getDisallowedSystemApps";
                case 288:
                    return "transferOwnership";
                case 289:
                    return "getTransferOwnershipBundle";
                case 290:
                    return "setStartUserSessionMessage";
                case 291:
                    return "setEndUserSessionMessage";
                case 292:
                    return "getStartUserSessionMessage";
                case 293:
                    return "getEndUserSessionMessage";
                case 294:
                    return "setMeteredDataDisabledPackages";
                case 295:
                    return "getMeteredDataDisabledPackages";
                case 296:
                    return "addOverrideApn";
                case 297:
                    return "updateOverrideApn";
                case 298:
                    return "removeOverrideApn";
                case 299:
                    return "getOverrideApns";
                case 300:
                    return "setOverrideApnsEnabled";
                case 301:
                    return "isOverrideApnEnabled";
                case 302:
                    return "isMeteredDataDisabledPackageForUser";
                case 303:
                    return "setGlobalPrivateDns";
                case 304:
                    return "getGlobalPrivateDnsMode";
                case 305:
                    return "getGlobalPrivateDnsHost";
                case 306:
                    return "setProfileOwnerOnOrganizationOwnedDevice";
                case 307:
                    return "installUpdateFromFile";
                case 308:
                    return "setCrossProfileCalendarPackages";
                case 309:
                    return "getCrossProfileCalendarPackages";
                case 310:
                    return "isPackageAllowedToAccessCalendarForUser";
                case 311:
                    return "getCrossProfileCalendarPackagesForUser";
                case 312:
                    return "setCrossProfilePackages";
                case 313:
                    return "getCrossProfilePackages";
                case 314:
                    return "getAllCrossProfilePackages";
                case 315:
                    return "getDefaultCrossProfilePackages";
                case 316:
                    return "isManagedKiosk";
                case 317:
                    return "isUnattendedManagedKiosk";
                case 318:
                    return "startViewCalendarEventInManagedProfile";
                case 319:
                    return "setKeyGrantForApp";
                case 320:
                    return "getKeyPairGrants";
                case 321:
                    return "setKeyGrantToWifiAuth";
                case 322:
                    return "isKeyPairGrantedToWifiAuth";
                case 323:
                    return "setUserControlDisabledPackages";
                case 324:
                    return "getUserControlDisabledPackages";
                case 325:
                    return "setCommonCriteriaModeEnabled";
                case 326:
                    return "isCommonCriteriaModeEnabled";
                case 327:
                    return "getPersonalAppsSuspendedReasons";
                case 328:
                    return "setPersonalAppsSuspended";
                case 329:
                    return "getManagedProfileMaximumTimeOff";
                case 330:
                    return "setManagedProfileMaximumTimeOff";
                case 331:
                    return "acknowledgeDeviceCompliant";
                case 332:
                    return "isComplianceAcknowledgementRequired";
                case 333:
                    return "canProfileOwnerResetPasswordWhenLocked";
                case 334:
                    return "setNextOperationSafety";
                case 335:
                    return "isSafeOperation";
                case 336:
                    return "getEnrollmentSpecificId";
                case 337:
                    return "setOrganizationIdForUser";
                case 338:
                    return "createAndProvisionManagedProfile";
                case 339:
                    return "provisionFullyManagedDevice";
                case 340:
                    return "finalizeWorkProfileProvisioning";
                case 341:
                    return "setDeviceOwnerType";
                case 342:
                    return "getDeviceOwnerType";
                case 343:
                    return "resetDefaultCrossProfileIntentFilters";
                case 344:
                    return "canAdminGrantSensorsPermissionsForUser";
                case 345:
                    return "setUsbDataSignalingEnabled";
                case 346:
                    return "isUsbDataSignalingEnabled";
                case 347:
                    return "isUsbDataSignalingEnabledForUser";
                case 348:
                    return "canUsbDataSignalingBeDisabled";
                case 349:
                    return "setMinimumRequiredWifiSecurityLevel";
                case 350:
                    return "getMinimumRequiredWifiSecurityLevel";
                case 351:
                    return "setWifiSsidPolicy";
                case 352:
                    return "getWifiSsidPolicy";
                case 353:
                    return "listForegroundAffiliatedUsers";
                case 354:
                    return "setDrawables";
                case 355:
                    return "resetDrawables";
                case 356:
                    return "getDrawable";
                case 357:
                    return "isDpcDownloaded";
                case 358:
                    return "setDpcDownloaded";
                case 359:
                    return "setStrings";
                case 360:
                    return "resetStrings";
                case 361:
                    return "getString";
                case 362:
                    return "shouldAllowBypassingDevicePolicyManagementRoleQualification";
                case 363:
                    return "getPolicyManagedProfiles";
                case 364:
                    return "setDualProfileEnabled";
                default:
                    return null;
            }
        }

        @Override // android.os.Binder
        public String getTransactionName(int transactionCode) {
            return getDefaultTransactionName(transactionCode);
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg1 = data.readInt();
                            boolean _arg2 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setPasswordQuality(_arg0, _arg1, _arg2);
                            reply.writeNoException();
                            break;
                        case 2:
                            ComponentName _arg02 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg12 = data.readInt();
                            boolean _arg22 = data.readBoolean();
                            data.enforceNoDataAvail();
                            int _result = getPasswordQuality(_arg02, _arg12, _arg22);
                            reply.writeNoException();
                            reply.writeInt(_result);
                            break;
                        case 3:
                            ComponentName _arg03 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg13 = data.readInt();
                            boolean _arg23 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setPasswordMinimumLength(_arg03, _arg13, _arg23);
                            reply.writeNoException();
                            break;
                        case 4:
                            ComponentName _arg04 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg14 = data.readInt();
                            boolean _arg24 = data.readBoolean();
                            data.enforceNoDataAvail();
                            int _result2 = getPasswordMinimumLength(_arg04, _arg14, _arg24);
                            reply.writeNoException();
                            reply.writeInt(_result2);
                            break;
                        case 5:
                            ComponentName _arg05 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg15 = data.readInt();
                            boolean _arg25 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setPasswordMinimumUpperCase(_arg05, _arg15, _arg25);
                            reply.writeNoException();
                            break;
                        case 6:
                            ComponentName _arg06 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg16 = data.readInt();
                            boolean _arg26 = data.readBoolean();
                            data.enforceNoDataAvail();
                            int _result3 = getPasswordMinimumUpperCase(_arg06, _arg16, _arg26);
                            reply.writeNoException();
                            reply.writeInt(_result3);
                            break;
                        case 7:
                            ComponentName _arg07 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg17 = data.readInt();
                            boolean _arg27 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setPasswordMinimumLowerCase(_arg07, _arg17, _arg27);
                            reply.writeNoException();
                            break;
                        case 8:
                            ComponentName _arg08 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg18 = data.readInt();
                            boolean _arg28 = data.readBoolean();
                            data.enforceNoDataAvail();
                            int _result4 = getPasswordMinimumLowerCase(_arg08, _arg18, _arg28);
                            reply.writeNoException();
                            reply.writeInt(_result4);
                            break;
                        case 9:
                            ComponentName _arg09 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg19 = data.readInt();
                            boolean _arg29 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setPasswordMinimumLetters(_arg09, _arg19, _arg29);
                            reply.writeNoException();
                            break;
                        case 10:
                            ComponentName _arg010 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg110 = data.readInt();
                            boolean _arg210 = data.readBoolean();
                            data.enforceNoDataAvail();
                            int _result5 = getPasswordMinimumLetters(_arg010, _arg110, _arg210);
                            reply.writeNoException();
                            reply.writeInt(_result5);
                            break;
                        case 11:
                            return onTransact$setPasswordMinimumNumeric$(data, reply);
                        case 12:
                            return onTransact$getPasswordMinimumNumeric$(data, reply);
                        case 13:
                            return onTransact$setPasswordMinimumSymbols$(data, reply);
                        case 14:
                            return onTransact$getPasswordMinimumSymbols$(data, reply);
                        case 15:
                            return onTransact$setPasswordMinimumNonLetter$(data, reply);
                        case 16:
                            return onTransact$getPasswordMinimumNonLetter$(data, reply);
                        case 17:
                            int _arg011 = data.readInt();
                            boolean _arg111 = data.readBoolean();
                            data.enforceNoDataAvail();
                            PasswordMetrics _result6 = getPasswordMinimumMetrics(_arg011, _arg111);
                            reply.writeNoException();
                            reply.writeTypedObject(_result6, 1);
                            break;
                        case 18:
                            return onTransact$setPasswordHistoryLength$(data, reply);
                        case 19:
                            return onTransact$getPasswordHistoryLength$(data, reply);
                        case 20:
                            return onTransact$setPasswordExpirationTimeout$(data, reply);
                        case 21:
                            return onTransact$getPasswordExpirationTimeout$(data, reply);
                        case 22:
                            return onTransact$getPasswordExpiration$(data, reply);
                        case 23:
                            int _arg012 = data.readInt();
                            boolean _arg112 = data.readBoolean();
                            data.enforceNoDataAvail();
                            boolean _result7 = isActivePasswordSufficient(_arg012, _arg112);
                            reply.writeNoException();
                            reply.writeBoolean(_result7);
                            break;
                        case 24:
                            boolean _result8 = isActivePasswordSufficientForDeviceRequirement();
                            reply.writeNoException();
                            reply.writeBoolean(_result8);
                            break;
                        case 25:
                            int _arg013 = data.readInt();
                            int _arg113 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result9 = isPasswordSufficientAfterProfileUnification(_arg013, _arg113);
                            reply.writeNoException();
                            reply.writeBoolean(_result9);
                            break;
                        case 26:
                            boolean _arg014 = data.readBoolean();
                            data.enforceNoDataAvail();
                            int _result10 = getPasswordComplexity(_arg014);
                            reply.writeNoException();
                            reply.writeInt(_result10);
                            break;
                        case 27:
                            int _arg015 = data.readInt();
                            boolean _arg114 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setRequiredPasswordComplexity(_arg015, _arg114);
                            reply.writeNoException();
                            break;
                        case 28:
                            boolean _arg016 = data.readBoolean();
                            data.enforceNoDataAvail();
                            int _result11 = getRequiredPasswordComplexity(_arg016);
                            reply.writeNoException();
                            reply.writeInt(_result11);
                            break;
                        case 29:
                            int _arg017 = data.readInt();
                            boolean _arg115 = data.readBoolean();
                            data.enforceNoDataAvail();
                            int _result12 = getAggregatedPasswordComplexityForUser(_arg017, _arg115);
                            reply.writeNoException();
                            reply.writeInt(_result12);
                            break;
                        case 30:
                            ComponentName _arg018 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result13 = isUsingUnifiedPassword(_arg018);
                            reply.writeNoException();
                            reply.writeBoolean(_result13);
                            break;
                        case 31:
                            int _arg019 = data.readInt();
                            boolean _arg116 = data.readBoolean();
                            data.enforceNoDataAvail();
                            int _result14 = getCurrentFailedPasswordAttempts(_arg019, _arg116);
                            reply.writeNoException();
                            reply.writeInt(_result14);
                            break;
                        case 32:
                            int _arg020 = data.readInt();
                            boolean _arg117 = data.readBoolean();
                            data.enforceNoDataAvail();
                            int _result15 = getProfileWithMinimumFailedPasswordsForWipe(_arg020, _arg117);
                            reply.writeNoException();
                            reply.writeInt(_result15);
                            break;
                        case 33:
                            return onTransact$setMaximumFailedPasswordsForWipe$(data, reply);
                        case 34:
                            return onTransact$getMaximumFailedPasswordsForWipe$(data, reply);
                        case 35:
                            String _arg021 = data.readString();
                            int _arg118 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result16 = resetPassword(_arg021, _arg118);
                            reply.writeNoException();
                            reply.writeBoolean(_result16);
                            break;
                        case 36:
                            return onTransact$setMaximumTimeToLock$(data, reply);
                        case 37:
                            return onTransact$getMaximumTimeToLock$(data, reply);
                        case 38:
                            return onTransact$setRequiredStrongAuthTimeout$(data, reply);
                        case 39:
                            return onTransact$getRequiredStrongAuthTimeout$(data, reply);
                        case 40:
                            int _arg022 = data.readInt();
                            boolean _arg119 = data.readBoolean();
                            data.enforceNoDataAvail();
                            lockNow(_arg022, _arg119);
                            reply.writeNoException();
                            break;
                        case 41:
                            return onTransact$wipeDataWithReason$(data, reply);
                        case 42:
                            ComponentName _arg023 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            FactoryResetProtectionPolicy _arg120 = (FactoryResetProtectionPolicy) data.readTypedObject(FactoryResetProtectionPolicy.CREATOR);
                            data.enforceNoDataAvail();
                            setFactoryResetProtectionPolicy(_arg023, _arg120);
                            reply.writeNoException();
                            break;
                        case 43:
                            ComponentName _arg024 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            FactoryResetProtectionPolicy _result17 = getFactoryResetProtectionPolicy(_arg024);
                            reply.writeNoException();
                            reply.writeTypedObject(_result17, 1);
                            break;
                        case 44:
                            boolean _result18 = isFactoryResetProtectionPolicySupported();
                            reply.writeNoException();
                            reply.writeBoolean(_result18);
                            break;
                        case 45:
                            AndroidFuture<Boolean> _arg025 = (AndroidFuture) data.readTypedObject(AndroidFuture.CREATOR);
                            data.enforceNoDataAvail();
                            sendLostModeLocationUpdate(_arg025);
                            reply.writeNoException();
                            break;
                        case 46:
                            return onTransact$setGlobalProxy$(data, reply);
                        case 47:
                            int _arg026 = data.readInt();
                            data.enforceNoDataAvail();
                            ComponentName _result19 = getGlobalProxyAdmin(_arg026);
                            reply.writeNoException();
                            reply.writeTypedObject(_result19, 1);
                            break;
                        case 48:
                            ComponentName _arg027 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            ProxyInfo _arg121 = (ProxyInfo) data.readTypedObject(ProxyInfo.CREATOR);
                            data.enforceNoDataAvail();
                            setRecommendedGlobalProxy(_arg027, _arg121);
                            reply.writeNoException();
                            break;
                        case 49:
                            ComponentName _arg028 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            boolean _arg122 = data.readBoolean();
                            data.enforceNoDataAvail();
                            int _result20 = setStorageEncryption(_arg028, _arg122);
                            reply.writeNoException();
                            reply.writeInt(_result20);
                            break;
                        case 50:
                            ComponentName _arg029 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg123 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result21 = getStorageEncryption(_arg029, _arg123);
                            reply.writeNoException();
                            reply.writeBoolean(_result21);
                            break;
                        case 51:
                            String _arg030 = data.readString();
                            int _arg124 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result22 = getStorageEncryptionStatus(_arg030, _arg124);
                            reply.writeNoException();
                            reply.writeInt(_result22);
                            break;
                        case 52:
                            ComponentName _arg031 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result23 = requestBugreport(_arg031);
                            reply.writeNoException();
                            reply.writeBoolean(_result23);
                            break;
                        case 53:
                            return onTransact$setCameraDisabled$(data, reply);
                        case 54:
                            return onTransact$getCameraDisabled$(data, reply);
                        case 55:
                            return onTransact$setScreenCaptureDisabled$(data, reply);
                        case 56:
                            return onTransact$getScreenCaptureDisabled$(data, reply);
                        case 57:
                            int _arg032 = data.readInt();
                            data.enforceNoDataAvail();
                            setNearbyNotificationStreamingPolicy(_arg032);
                            reply.writeNoException();
                            break;
                        case 58:
                            int _arg033 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result24 = getNearbyNotificationStreamingPolicy(_arg033);
                            reply.writeNoException();
                            reply.writeInt(_result24);
                            break;
                        case 59:
                            int _arg034 = data.readInt();
                            data.enforceNoDataAvail();
                            setNearbyAppStreamingPolicy(_arg034);
                            reply.writeNoException();
                            break;
                        case 60:
                            int _arg035 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result25 = getNearbyAppStreamingPolicy(_arg035);
                            reply.writeNoException();
                            reply.writeInt(_result25);
                            break;
                        case 61:
                            return onTransact$setKeyguardDisabledFeatures$(data, reply);
                        case 62:
                            return onTransact$getKeyguardDisabledFeatures$(data, reply);
                        case 63:
                            return onTransact$setActiveAdmin$(data, reply);
                        case 64:
                            ComponentName _arg036 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg125 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result26 = isAdminActive(_arg036, _arg125);
                            reply.writeNoException();
                            reply.writeBoolean(_result26);
                            break;
                        case 65:
                            int _arg037 = data.readInt();
                            data.enforceNoDataAvail();
                            List<ComponentName> _result27 = getActiveAdmins(_arg037);
                            reply.writeNoException();
                            reply.writeTypedList(_result27);
                            break;
                        case 66:
                            String _arg038 = data.readString();
                            int _arg126 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result28 = packageHasActiveAdmins(_arg038, _arg126);
                            reply.writeNoException();
                            reply.writeBoolean(_result28);
                            break;
                        case 67:
                            return onTransact$getRemoveWarning$(data, reply);
                        case 68:
                            ComponentName _arg039 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg127 = data.readInt();
                            data.enforceNoDataAvail();
                            removeActiveAdmin(_arg039, _arg127);
                            reply.writeNoException();
                            break;
                        case 69:
                            ComponentName _arg040 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg128 = data.readInt();
                            data.enforceNoDataAvail();
                            forceRemoveActiveAdmin(_arg040, _arg128);
                            reply.writeNoException();
                            break;
                        case 70:
                            return onTransact$hasGrantedPolicy$(data, reply);
                        case 71:
                            PasswordMetrics _arg041 = (PasswordMetrics) data.readTypedObject(PasswordMetrics.CREATOR);
                            int _arg129 = data.readInt();
                            data.enforceNoDataAvail();
                            reportPasswordChanged(_arg041, _arg129);
                            reply.writeNoException();
                            break;
                        case 72:
                            int _arg042 = data.readInt();
                            data.enforceNoDataAvail();
                            reportFailedPasswordAttempt(_arg042);
                            reply.writeNoException();
                            break;
                        case 73:
                            int _arg043 = data.readInt();
                            data.enforceNoDataAvail();
                            reportSuccessfulPasswordAttempt(_arg043);
                            reply.writeNoException();
                            break;
                        case 74:
                            int _arg044 = data.readInt();
                            data.enforceNoDataAvail();
                            reportFailedBiometricAttempt(_arg044);
                            reply.writeNoException();
                            break;
                        case 75:
                            int _arg045 = data.readInt();
                            data.enforceNoDataAvail();
                            reportSuccessfulBiometricAttempt(_arg045);
                            reply.writeNoException();
                            break;
                        case 76:
                            int _arg046 = data.readInt();
                            data.enforceNoDataAvail();
                            reportKeyguardDismissed(_arg046);
                            reply.writeNoException();
                            break;
                        case 77:
                            int _arg047 = data.readInt();
                            data.enforceNoDataAvail();
                            reportKeyguardSecured(_arg047);
                            reply.writeNoException();
                            break;
                        case 78:
                            return onTransact$setDeviceOwner$(data, reply);
                        case 79:
                            boolean _arg048 = data.readBoolean();
                            data.enforceNoDataAvail();
                            ComponentName _result29 = getDeviceOwnerComponent(_arg048);
                            reply.writeNoException();
                            reply.writeTypedObject(_result29, 1);
                            break;
                        case 80:
                            boolean _result30 = hasDeviceOwner();
                            reply.writeNoException();
                            reply.writeBoolean(_result30);
                            break;
                        case 81:
                            String _result31 = getDeviceOwnerName();
                            reply.writeNoException();
                            reply.writeString(_result31);
                            break;
                        case 82:
                            String _arg049 = data.readString();
                            data.enforceNoDataAvail();
                            clearDeviceOwner(_arg049);
                            reply.writeNoException();
                            break;
                        case 83:
                            int _result32 = getDeviceOwnerUserId();
                            reply.writeNoException();
                            reply.writeInt(_result32);
                            break;
                        case 84:
                            return onTransact$setProfileOwner$(data, reply);
                        case 85:
                            int _arg050 = data.readInt();
                            data.enforceNoDataAvail();
                            ComponentName _result33 = getProfileOwnerAsUser(_arg050);
                            reply.writeNoException();
                            reply.writeTypedObject(_result33, 1);
                            break;
                        case 86:
                            UserHandle _arg051 = (UserHandle) data.readTypedObject(UserHandle.CREATOR);
                            data.enforceNoDataAvail();
                            ComponentName _result34 = getProfileOwnerOrDeviceOwnerSupervisionComponent(_arg051);
                            reply.writeNoException();
                            reply.writeTypedObject(_result34, 1);
                            break;
                        case 87:
                            ComponentName _arg052 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result35 = isSupervisionComponent(_arg052);
                            reply.writeNoException();
                            reply.writeBoolean(_result35);
                            break;
                        case 88:
                            int _arg053 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result36 = getProfileOwnerName(_arg053);
                            reply.writeNoException();
                            reply.writeString(_result36);
                            break;
                        case 89:
                            ComponentName _arg054 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            setProfileEnabled(_arg054);
                            reply.writeNoException();
                            break;
                        case 90:
                            ComponentName _arg055 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            String _arg130 = data.readString();
                            data.enforceNoDataAvail();
                            setProfileName(_arg055, _arg130);
                            reply.writeNoException();
                            break;
                        case 91:
                            ComponentName _arg056 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            clearProfileOwner(_arg056);
                            reply.writeNoException();
                            break;
                        case 92:
                            boolean _result37 = hasUserSetupCompleted();
                            reply.writeNoException();
                            reply.writeBoolean(_result37);
                            break;
                        case 93:
                            boolean _result38 = isOrganizationOwnedDeviceWithManagedProfile();
                            reply.writeNoException();
                            reply.writeBoolean(_result38);
                            break;
                        case 94:
                            return onTransact$checkDeviceIdentifierAccess$(data, reply);
                        case 95:
                            ComponentName _arg057 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            CharSequence _arg131 = (CharSequence) data.readTypedObject(TextUtils.CHAR_SEQUENCE_CREATOR);
                            data.enforceNoDataAvail();
                            setDeviceOwnerLockScreenInfo(_arg057, _arg131);
                            reply.writeNoException();
                            break;
                        case 96:
                            CharSequence _result39 = getDeviceOwnerLockScreenInfo();
                            reply.writeNoException();
                            if (_result39 != null) {
                                reply.writeInt(1);
                                TextUtils.writeToParcel(_result39, reply, 1);
                                break;
                            } else {
                                reply.writeInt(0);
                                break;
                            }
                        case 97:
                            return onTransact$setPackagesSuspended$(data, reply);
                        case 98:
                            return onTransact$isPackageSuspended$(data, reply);
                        case 99:
                            List<String> _result40 = listPolicyExemptApps();
                            reply.writeNoException();
                            reply.writeStringList(_result40);
                            break;
                        case 100:
                            return onTransact$installCaCert$(data, reply);
                        case 101:
                            return onTransact$uninstallCaCerts$(data, reply);
                        case 102:
                            ComponentName _arg058 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            String _arg132 = data.readString();
                            data.enforceNoDataAvail();
                            enforceCanManageCaCerts(_arg058, _arg132);
                            reply.writeNoException();
                            break;
                        case 103:
                            return onTransact$approveCaCert$(data, reply);
                        case 104:
                            String _arg059 = data.readString();
                            int _arg133 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result41 = isCaCertApproved(_arg059, _arg133);
                            reply.writeNoException();
                            reply.writeBoolean(_result41);
                            break;
                        case 105:
                            return onTransact$installKeyPair$(data, reply);
                        case 106:
                            return onTransact$removeKeyPair$(data, reply);
                        case 107:
                            String _arg060 = data.readString();
                            String _arg134 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result42 = hasKeyPair(_arg060, _arg134);
                            reply.writeNoException();
                            reply.writeBoolean(_result42);
                            break;
                        case 108:
                            return onTransact$generateKeyPair$(data, reply);
                        case 109:
                            return onTransact$setKeyPairCertificate$(data, reply);
                        case 110:
                            return onTransact$choosePrivateKeyAlias$(data, reply);
                        case 111:
                            return onTransact$setDelegatedScopes$(data, reply);
                        case 112:
                            ComponentName _arg061 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            String _arg135 = data.readString();
                            data.enforceNoDataAvail();
                            List<String> _result43 = getDelegatedScopes(_arg061, _arg135);
                            reply.writeNoException();
                            reply.writeStringList(_result43);
                            break;
                        case 113:
                            ComponentName _arg062 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            String _arg136 = data.readString();
                            data.enforceNoDataAvail();
                            List<String> _result44 = getDelegatePackages(_arg062, _arg136);
                            reply.writeNoException();
                            reply.writeStringList(_result44);
                            break;
                        case 114:
                            ComponentName _arg063 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            String _arg137 = data.readString();
                            data.enforceNoDataAvail();
                            setCertInstallerPackage(_arg063, _arg137);
                            reply.writeNoException();
                            break;
                        case 115:
                            ComponentName _arg064 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            String _result45 = getCertInstallerPackage(_arg064);
                            reply.writeNoException();
                            reply.writeString(_result45);
                            break;
                        case 116:
                            return onTransact$setAlwaysOnVpnPackage$(data, reply);
                        case 117:
                            ComponentName _arg065 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            String _result46 = getAlwaysOnVpnPackage(_arg065);
                            reply.writeNoException();
                            reply.writeString(_result46);
                            break;
                        case 118:
                            int _arg066 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result47 = getAlwaysOnVpnPackageForUser(_arg066);
                            reply.writeNoException();
                            reply.writeString(_result47);
                            break;
                        case 119:
                            ComponentName _arg067 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result48 = isAlwaysOnVpnLockdownEnabled(_arg067);
                            reply.writeNoException();
                            reply.writeBoolean(_result48);
                            break;
                        case 120:
                            int _arg068 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result49 = isAlwaysOnVpnLockdownEnabledForUser(_arg068);
                            reply.writeNoException();
                            reply.writeBoolean(_result49);
                            break;
                        case 121:
                            ComponentName _arg069 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            List<String> _result50 = getAlwaysOnVpnLockdownAllowlist(_arg069);
                            reply.writeNoException();
                            reply.writeStringList(_result50);
                            break;
                        case 122:
                            return onTransact$addPersistentPreferredActivity$(data, reply);
                        case 123:
                            ComponentName _arg070 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            String _arg138 = data.readString();
                            data.enforceNoDataAvail();
                            clearPackagePersistentPreferredActivities(_arg070, _arg138);
                            reply.writeNoException();
                            break;
                        case 124:
                            return onTransact$setDefaultSmsApplication$(data, reply);
                        case 125:
                            return onTransact$setApplicationRestrictions$(data, reply);
                        case 126:
                            return onTransact$getApplicationRestrictions$(data, reply);
                        case 127:
                            ComponentName _arg071 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            String _arg139 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result51 = setApplicationRestrictionsManagingPackage(_arg071, _arg139);
                            reply.writeNoException();
                            reply.writeBoolean(_result51);
                            break;
                        case 128:
                            ComponentName _arg072 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            String _result52 = getApplicationRestrictionsManagingPackage(_arg072);
                            reply.writeNoException();
                            reply.writeString(_result52);
                            break;
                        case 129:
                            String _arg073 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result53 = isCallerApplicationRestrictionsManagingPackage(_arg073);
                            reply.writeNoException();
                            reply.writeBoolean(_result53);
                            break;
                        case 130:
                            ComponentName _arg074 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            ComponentName _arg140 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            setRestrictionsProvider(_arg074, _arg140);
                            reply.writeNoException();
                            break;
                        case 131:
                            int _arg075 = data.readInt();
                            data.enforceNoDataAvail();
                            ComponentName _result54 = getRestrictionsProvider(_arg075);
                            reply.writeNoException();
                            reply.writeTypedObject(_result54, 1);
                            break;
                        case 132:
                            return onTransact$setUserRestriction$(data, reply);
                        case 133:
                            ComponentName _arg076 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            boolean _arg141 = data.readBoolean();
                            data.enforceNoDataAvail();
                            Bundle _result55 = getUserRestrictions(_arg076, _arg141);
                            reply.writeNoException();
                            reply.writeTypedObject(_result55, 1);
                            break;
                        case 134:
                            return onTransact$addCrossProfileIntentFilter$(data, reply);
                        case 135:
                            ComponentName _arg077 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            clearCrossProfileIntentFilters(_arg077);
                            reply.writeNoException();
                            break;
                        case 136:
                            ComponentName _arg078 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            List<String> _arg142 = data.createStringArrayList();
                            data.enforceNoDataAvail();
                            boolean _result56 = setPermittedAccessibilityServices(_arg078, _arg142);
                            reply.writeNoException();
                            reply.writeBoolean(_result56);
                            break;
                        case 137:
                            ComponentName _arg079 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            List<String> _result57 = getPermittedAccessibilityServices(_arg079);
                            reply.writeNoException();
                            reply.writeStringList(_result57);
                            break;
                        case 138:
                            int _arg080 = data.readInt();
                            data.enforceNoDataAvail();
                            List<String> _result58 = getPermittedAccessibilityServicesForUser(_arg080);
                            reply.writeNoException();
                            reply.writeStringList(_result58);
                            break;
                        case 139:
                            return onTransact$isAccessibilityServicePermittedByAdmin$(data, reply);
                        case 140:
                            return onTransact$setPermittedInputMethods$(data, reply);
                        case 141:
                            ComponentName _arg081 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            boolean _arg143 = data.readBoolean();
                            data.enforceNoDataAvail();
                            List<String> _result59 = getPermittedInputMethods(_arg081, _arg143);
                            reply.writeNoException();
                            reply.writeStringList(_result59);
                            break;
                        case 142:
                            int _arg082 = data.readInt();
                            data.enforceNoDataAvail();
                            List<String> _result60 = getPermittedInputMethodsAsUser(_arg082);
                            reply.writeNoException();
                            reply.writeStringList(_result60);
                            break;
                        case 143:
                            return onTransact$isInputMethodPermittedByAdmin$(data, reply);
                        case 144:
                            ComponentName _arg083 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            List<String> _arg144 = data.createStringArrayList();
                            data.enforceNoDataAvail();
                            boolean _result61 = setPermittedCrossProfileNotificationListeners(_arg083, _arg144);
                            reply.writeNoException();
                            reply.writeBoolean(_result61);
                            break;
                        case 145:
                            ComponentName _arg084 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            List<String> _result62 = getPermittedCrossProfileNotificationListeners(_arg084);
                            reply.writeNoException();
                            reply.writeStringList(_result62);
                            break;
                        case 146:
                            String _arg085 = data.readString();
                            int _arg145 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result63 = isNotificationListenerServicePermitted(_arg085, _arg145);
                            reply.writeNoException();
                            reply.writeBoolean(_result63);
                            break;
                        case 147:
                            String _arg086 = data.readString();
                            data.enforceNoDataAvail();
                            Intent _result64 = createAdminSupportIntent(_arg086);
                            reply.writeNoException();
                            reply.writeTypedObject(_result64, 1);
                            break;
                        case 148:
                            int _arg087 = data.readInt();
                            String _arg146 = data.readString();
                            data.enforceNoDataAvail();
                            Bundle _result65 = getEnforcingAdminAndUserDetails(_arg087, _arg146);
                            reply.writeNoException();
                            reply.writeTypedObject(_result65, 1);
                            break;
                        case 149:
                            return onTransact$setApplicationHidden$(data, reply);
                        case 150:
                            return onTransact$isApplicationHidden$(data, reply);
                        case 151:
                            return onTransact$createAndManageUser$(data, reply);
                        case 152:
                            ComponentName _arg088 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            UserHandle _arg147 = (UserHandle) data.readTypedObject(UserHandle.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result66 = removeUser(_arg088, _arg147);
                            reply.writeNoException();
                            reply.writeBoolean(_result66);
                            break;
                        case 153:
                            ComponentName _arg089 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            UserHandle _arg148 = (UserHandle) data.readTypedObject(UserHandle.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result67 = switchUser(_arg089, _arg148);
                            reply.writeNoException();
                            reply.writeBoolean(_result67);
                            break;
                        case 154:
                            ComponentName _arg090 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            UserHandle _arg149 = (UserHandle) data.readTypedObject(UserHandle.CREATOR);
                            data.enforceNoDataAvail();
                            int _result68 = startUserInBackground(_arg090, _arg149);
                            reply.writeNoException();
                            reply.writeInt(_result68);
                            break;
                        case 155:
                            ComponentName _arg091 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            UserHandle _arg150 = (UserHandle) data.readTypedObject(UserHandle.CREATOR);
                            data.enforceNoDataAvail();
                            int _result69 = stopUser(_arg091, _arg150);
                            reply.writeNoException();
                            reply.writeInt(_result69);
                            break;
                        case 156:
                            ComponentName _arg092 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            int _result70 = logoutUser(_arg092);
                            reply.writeNoException();
                            reply.writeInt(_result70);
                            break;
                        case 157:
                            int _result71 = logoutUserInternal();
                            reply.writeNoException();
                            reply.writeInt(_result71);
                            break;
                        case 158:
                            int _result72 = getLogoutUserId();
                            reply.writeNoException();
                            reply.writeInt(_result72);
                            break;
                        case 159:
                            ComponentName _arg093 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            List<UserHandle> _result73 = getSecondaryUsers(_arg093);
                            reply.writeNoException();
                            reply.writeTypedList(_result73);
                            break;
                        case 160:
                            int _arg094 = data.readInt();
                            data.enforceNoDataAvail();
                            acknowledgeNewUserDisclaimer(_arg094);
                            reply.writeNoException();
                            break;
                        case 161:
                            int _arg095 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result74 = isNewUserDisclaimerAcknowledged(_arg095);
                            reply.writeNoException();
                            reply.writeBoolean(_result74);
                            break;
                        case 162:
                            return onTransact$enableSystemApp$(data, reply);
                        case 163:
                            return onTransact$enableSystemAppWithIntent$(data, reply);
                        case 164:
                            return onTransact$installExistingPackage$(data, reply);
                        case 165:
                            return onTransact$setAccountManagementDisabled$(data, reply);
                        case 166:
                            String[] _result75 = getAccountTypesWithManagementDisabled();
                            reply.writeNoException();
                            reply.writeStringArray(_result75);
                            break;
                        case 167:
                            int _arg096 = data.readInt();
                            boolean _arg151 = data.readBoolean();
                            data.enforceNoDataAvail();
                            String[] _result76 = getAccountTypesWithManagementDisabledAsUser(_arg096, _arg151);
                            reply.writeNoException();
                            reply.writeStringArray(_result76);
                            break;
                        case 168:
                            ComponentName _arg097 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            boolean _arg152 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setSecondaryLockscreenEnabled(_arg097, _arg152);
                            reply.writeNoException();
                            break;
                        case 169:
                            UserHandle _arg098 = (UserHandle) data.readTypedObject(UserHandle.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result77 = isSecondaryLockscreenEnabled(_arg098);
                            reply.writeNoException();
                            reply.writeBoolean(_result77);
                            break;
                        case 170:
                            List<PreferentialNetworkServiceConfig> _arg099 = data.createTypedArrayList(PreferentialNetworkServiceConfig.CREATOR);
                            data.enforceNoDataAvail();
                            setPreferentialNetworkServiceConfigs(_arg099);
                            reply.writeNoException();
                            break;
                        case 171:
                            List<PreferentialNetworkServiceConfig> _result78 = getPreferentialNetworkServiceConfigs();
                            reply.writeNoException();
                            reply.writeTypedList(_result78);
                            break;
                        case 172:
                            ComponentName _arg0100 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            String[] _arg153 = data.createStringArray();
                            data.enforceNoDataAvail();
                            setLockTaskPackages(_arg0100, _arg153);
                            reply.writeNoException();
                            break;
                        case 173:
                            ComponentName _arg0101 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            String[] _result79 = getLockTaskPackages(_arg0101);
                            reply.writeNoException();
                            reply.writeStringArray(_result79);
                            break;
                        case 174:
                            String _arg0102 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result80 = isLockTaskPermitted(_arg0102);
                            reply.writeNoException();
                            reply.writeBoolean(_result80);
                            break;
                        case 175:
                            ComponentName _arg0103 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg154 = data.readInt();
                            data.enforceNoDataAvail();
                            setLockTaskFeatures(_arg0103, _arg154);
                            reply.writeNoException();
                            break;
                        case 176:
                            ComponentName _arg0104 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            int _result81 = getLockTaskFeatures(_arg0104);
                            reply.writeNoException();
                            reply.writeInt(_result81);
                            break;
                        case 177:
                            return onTransact$setGlobalSetting$(data, reply);
                        case 178:
                            return onTransact$setSystemSetting$(data, reply);
                        case 179:
                            return onTransact$setSecureSetting$(data, reply);
                        case 180:
                            ComponentName _arg0105 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            boolean _arg155 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setConfiguredNetworksLockdownState(_arg0105, _arg155);
                            reply.writeNoException();
                            break;
                        case 181:
                            ComponentName _arg0106 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result82 = hasLockdownAdminConfiguredNetworks(_arg0106);
                            reply.writeNoException();
                            reply.writeBoolean(_result82);
                            break;
                        case 182:
                            ComponentName _arg0107 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            boolean _arg156 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setLocationEnabled(_arg0107, _arg156);
                            reply.writeNoException();
                            break;
                        case 183:
                            ComponentName _arg0108 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            long _arg157 = data.readLong();
                            data.enforceNoDataAvail();
                            boolean _result83 = setTime(_arg0108, _arg157);
                            reply.writeNoException();
                            reply.writeBoolean(_result83);
                            break;
                        case 184:
                            ComponentName _arg0109 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            String _arg158 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result84 = setTimeZone(_arg0109, _arg158);
                            reply.writeNoException();
                            reply.writeBoolean(_result84);
                            break;
                        case 185:
                            ComponentName _arg0110 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            boolean _arg159 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setMasterVolumeMuted(_arg0110, _arg159);
                            reply.writeNoException();
                            break;
                        case 186:
                            ComponentName _arg0111 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result85 = isMasterVolumeMuted(_arg0111);
                            reply.writeNoException();
                            reply.writeBoolean(_result85);
                            break;
                        case 187:
                            return onTransact$notifyLockTaskModeChanged$(data, reply);
                        case 188:
                            return onTransact$setUninstallBlocked$(data, reply);
                        case 189:
                            ComponentName _arg0112 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            String _arg160 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result86 = isUninstallBlocked(_arg0112, _arg160);
                            reply.writeNoException();
                            reply.writeBoolean(_result86);
                            break;
                        case 190:
                            ComponentName _arg0113 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            boolean _arg161 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setCrossProfileCallerIdDisabled(_arg0113, _arg161);
                            reply.writeNoException();
                            break;
                        case 191:
                            ComponentName _arg0114 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result87 = getCrossProfileCallerIdDisabled(_arg0114);
                            reply.writeNoException();
                            reply.writeBoolean(_result87);
                            break;
                        case 192:
                            int _arg0115 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result88 = getCrossProfileCallerIdDisabledForUser(_arg0115);
                            reply.writeNoException();
                            reply.writeBoolean(_result88);
                            break;
                        case 193:
                            ComponentName _arg0116 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            boolean _arg162 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setCrossProfileContactsSearchDisabled(_arg0116, _arg162);
                            reply.writeNoException();
                            break;
                        case 194:
                            ComponentName _arg0117 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result89 = getCrossProfileContactsSearchDisabled(_arg0117);
                            reply.writeNoException();
                            reply.writeBoolean(_result89);
                            break;
                        case 195:
                            int _arg0118 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result90 = getCrossProfileContactsSearchDisabledForUser(_arg0118);
                            reply.writeNoException();
                            reply.writeBoolean(_result90);
                            break;
                        case 196:
                            return onTransact$startManagedQuickContact$(data, reply);
                        case 197:
                            ComponentName _arg0119 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            boolean _arg163 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setBluetoothContactSharingDisabled(_arg0119, _arg163);
                            reply.writeNoException();
                            break;
                        case 198:
                            ComponentName _arg0120 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result91 = getBluetoothContactSharingDisabled(_arg0120);
                            reply.writeNoException();
                            reply.writeBoolean(_result91);
                            break;
                        case 199:
                            int _arg0121 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result92 = getBluetoothContactSharingDisabledForUser(_arg0121);
                            reply.writeNoException();
                            reply.writeBoolean(_result92);
                            break;
                        case 200:
                            return onTransact$setTrustAgentConfiguration$(data, reply);
                        case 201:
                            return onTransact$getTrustAgentConfiguration$(data, reply);
                        case 202:
                            ComponentName _arg0122 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            String _arg164 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result93 = addCrossProfileWidgetProvider(_arg0122, _arg164);
                            reply.writeNoException();
                            reply.writeBoolean(_result93);
                            break;
                        case 203:
                            ComponentName _arg0123 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            String _arg165 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result94 = removeCrossProfileWidgetProvider(_arg0123, _arg165);
                            reply.writeNoException();
                            reply.writeBoolean(_result94);
                            break;
                        case 204:
                            ComponentName _arg0124 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            List<String> _result95 = getCrossProfileWidgetProviders(_arg0124);
                            reply.writeNoException();
                            reply.writeStringList(_result95);
                            break;
                        case 205:
                            ComponentName _arg0125 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            boolean _arg166 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setAutoTimeRequired(_arg0125, _arg166);
                            reply.writeNoException();
                            break;
                        case 206:
                            boolean _result96 = getAutoTimeRequired();
                            reply.writeNoException();
                            reply.writeBoolean(_result96);
                            break;
                        case 207:
                            ComponentName _arg0126 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            boolean _arg167 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setAutoTimeEnabled(_arg0126, _arg167);
                            reply.writeNoException();
                            break;
                        case 208:
                            ComponentName _arg0127 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result97 = getAutoTimeEnabled(_arg0127);
                            reply.writeNoException();
                            reply.writeBoolean(_result97);
                            break;
                        case 209:
                            ComponentName _arg0128 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            boolean _arg168 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setAutoTimeZoneEnabled(_arg0128, _arg168);
                            reply.writeNoException();
                            break;
                        case 210:
                            ComponentName _arg0129 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result98 = getAutoTimeZoneEnabled(_arg0129);
                            reply.writeNoException();
                            reply.writeBoolean(_result98);
                            break;
                        case 211:
                            ComponentName _arg0130 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            boolean _arg169 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setForceEphemeralUsers(_arg0130, _arg169);
                            reply.writeNoException();
                            break;
                        case 212:
                            ComponentName _arg0131 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result99 = getForceEphemeralUsers(_arg0131);
                            reply.writeNoException();
                            reply.writeBoolean(_result99);
                            break;
                        case 213:
                            ComponentName _arg0132 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg170 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result100 = isRemovingAdmin(_arg0132, _arg170);
                            reply.writeNoException();
                            reply.writeBoolean(_result100);
                            break;
                        case 214:
                            ComponentName _arg0133 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            Bitmap _arg171 = (Bitmap) data.readTypedObject(Bitmap.CREATOR);
                            data.enforceNoDataAvail();
                            setUserIcon(_arg0133, _arg171);
                            reply.writeNoException();
                            break;
                        case 215:
                            ComponentName _arg0134 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            SystemUpdatePolicy _arg172 = (SystemUpdatePolicy) data.readTypedObject(SystemUpdatePolicy.CREATOR);
                            data.enforceNoDataAvail();
                            setSystemUpdatePolicy(_arg0134, _arg172);
                            reply.writeNoException();
                            break;
                        case 216:
                            SystemUpdatePolicy _result101 = getSystemUpdatePolicy();
                            reply.writeNoException();
                            reply.writeTypedObject(_result101, 1);
                            break;
                        case 217:
                            clearSystemUpdatePolicyFreezePeriodRecord();
                            reply.writeNoException();
                            break;
                        case 218:
                            ComponentName _arg0135 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            boolean _arg173 = data.readBoolean();
                            data.enforceNoDataAvail();
                            boolean _result102 = setKeyguardDisabled(_arg0135, _arg173);
                            reply.writeNoException();
                            reply.writeBoolean(_result102);
                            break;
                        case 219:
                            ComponentName _arg0136 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            boolean _arg174 = data.readBoolean();
                            data.enforceNoDataAvail();
                            boolean _result103 = setStatusBarDisabled(_arg0136, _arg174);
                            reply.writeNoException();
                            reply.writeBoolean(_result103);
                            break;
                        case 220:
                            boolean _result104 = getDoNotAskCredentialsOnBoot();
                            reply.writeNoException();
                            reply.writeBoolean(_result104);
                            break;
                        case 221:
                            SystemUpdateInfo _arg0137 = (SystemUpdateInfo) data.readTypedObject(SystemUpdateInfo.CREATOR);
                            data.enforceNoDataAvail();
                            notifyPendingSystemUpdate(_arg0137);
                            reply.writeNoException();
                            break;
                        case 222:
                            ComponentName _arg0138 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            SystemUpdateInfo _result105 = getPendingSystemUpdate(_arg0138);
                            reply.writeNoException();
                            reply.writeTypedObject(_result105, 1);
                            break;
                        case 223:
                            return onTransact$setPermissionPolicy$(data, reply);
                        case 224:
                            ComponentName _arg0139 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            int _result106 = getPermissionPolicy(_arg0139);
                            reply.writeNoException();
                            reply.writeInt(_result106);
                            break;
                        case 225:
                            return onTransact$setPermissionGrantState$(data, reply);
                        case 226:
                            return onTransact$getPermissionGrantState$(data, reply);
                        case 227:
                            String _arg0140 = data.readString();
                            String _arg175 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result107 = isProvisioningAllowed(_arg0140, _arg175);
                            reply.writeNoException();
                            reply.writeBoolean(_result107);
                            break;
                        case 228:
                            String _arg0141 = data.readString();
                            String _arg176 = data.readString();
                            data.enforceNoDataAvail();
                            int _result108 = checkProvisioningPrecondition(_arg0141, _arg176);
                            reply.writeNoException();
                            reply.writeInt(_result108);
                            break;
                        case 229:
                            return onTransact$setKeepUninstalledPackages$(data, reply);
                        case 230:
                            ComponentName _arg0142 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            String _arg177 = data.readString();
                            data.enforceNoDataAvail();
                            List<String> _result109 = getKeepUninstalledPackages(_arg0142, _arg177);
                            reply.writeNoException();
                            reply.writeStringList(_result109);
                            break;
                        case 231:
                            ComponentName _arg0143 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result110 = isManagedProfile(_arg0143);
                            reply.writeNoException();
                            reply.writeBoolean(_result110);
                            break;
                        case 232:
                            ComponentName _arg0144 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            String _result111 = getWifiMacAddress(_arg0144);
                            reply.writeNoException();
                            reply.writeString(_result111);
                            break;
                        case 233:
                            ComponentName _arg0145 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            reboot(_arg0145);
                            reply.writeNoException();
                            break;
                        case 234:
                            ComponentName _arg0146 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            CharSequence _arg178 = (CharSequence) data.readTypedObject(TextUtils.CHAR_SEQUENCE_CREATOR);
                            data.enforceNoDataAvail();
                            setShortSupportMessage(_arg0146, _arg178);
                            reply.writeNoException();
                            break;
                        case 235:
                            ComponentName _arg0147 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            CharSequence _result112 = getShortSupportMessage(_arg0147);
                            reply.writeNoException();
                            if (_result112 != null) {
                                reply.writeInt(1);
                                TextUtils.writeToParcel(_result112, reply, 1);
                                break;
                            } else {
                                reply.writeInt(0);
                                break;
                            }
                        case 236:
                            ComponentName _arg0148 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            CharSequence _arg179 = (CharSequence) data.readTypedObject(TextUtils.CHAR_SEQUENCE_CREATOR);
                            data.enforceNoDataAvail();
                            setLongSupportMessage(_arg0148, _arg179);
                            reply.writeNoException();
                            break;
                        case 237:
                            ComponentName _arg0149 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            CharSequence _result113 = getLongSupportMessage(_arg0149);
                            reply.writeNoException();
                            if (_result113 != null) {
                                reply.writeInt(1);
                                TextUtils.writeToParcel(_result113, reply, 1);
                                break;
                            } else {
                                reply.writeInt(0);
                                break;
                            }
                        case 238:
                            ComponentName _arg0150 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg180 = data.readInt();
                            data.enforceNoDataAvail();
                            CharSequence _result114 = getShortSupportMessageForUser(_arg0150, _arg180);
                            reply.writeNoException();
                            if (_result114 != null) {
                                reply.writeInt(1);
                                TextUtils.writeToParcel(_result114, reply, 1);
                                break;
                            } else {
                                reply.writeInt(0);
                                break;
                            }
                        case 239:
                            ComponentName _arg0151 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg181 = data.readInt();
                            data.enforceNoDataAvail();
                            CharSequence _result115 = getLongSupportMessageForUser(_arg0151, _arg181);
                            reply.writeNoException();
                            if (_result115 != null) {
                                reply.writeInt(1);
                                TextUtils.writeToParcel(_result115, reply, 1);
                                break;
                            } else {
                                reply.writeInt(0);
                                break;
                            }
                        case 240:
                            ComponentName _arg0152 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg182 = data.readInt();
                            data.enforceNoDataAvail();
                            setOrganizationColor(_arg0152, _arg182);
                            reply.writeNoException();
                            break;
                        case 241:
                            int _arg0153 = data.readInt();
                            int _arg183 = data.readInt();
                            data.enforceNoDataAvail();
                            setOrganizationColorForUser(_arg0153, _arg183);
                            reply.writeNoException();
                            break;
                        case 242:
                            int _arg0154 = data.readInt();
                            data.enforceNoDataAvail();
                            clearOrganizationIdForUser(_arg0154);
                            reply.writeNoException();
                            break;
                        case 243:
                            ComponentName _arg0155 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            int _result116 = getOrganizationColor(_arg0155);
                            reply.writeNoException();
                            reply.writeInt(_result116);
                            break;
                        case 244:
                            int _arg0156 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result117 = getOrganizationColorForUser(_arg0156);
                            reply.writeNoException();
                            reply.writeInt(_result117);
                            break;
                        case 245:
                            ComponentName _arg0157 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            CharSequence _arg184 = (CharSequence) data.readTypedObject(TextUtils.CHAR_SEQUENCE_CREATOR);
                            data.enforceNoDataAvail();
                            setOrganizationName(_arg0157, _arg184);
                            reply.writeNoException();
                            break;
                        case 246:
                            ComponentName _arg0158 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            CharSequence _result118 = getOrganizationName(_arg0158);
                            reply.writeNoException();
                            if (_result118 != null) {
                                reply.writeInt(1);
                                TextUtils.writeToParcel(_result118, reply, 1);
                                break;
                            } else {
                                reply.writeInt(0);
                                break;
                            }
                        case 247:
                            CharSequence _result119 = getDeviceOwnerOrganizationName();
                            reply.writeNoException();
                            if (_result119 != null) {
                                reply.writeInt(1);
                                TextUtils.writeToParcel(_result119, reply, 1);
                                break;
                            } else {
                                reply.writeInt(0);
                                break;
                            }
                        case 248:
                            int _arg0159 = data.readInt();
                            data.enforceNoDataAvail();
                            CharSequence _result120 = getOrganizationNameForUser(_arg0159);
                            reply.writeNoException();
                            if (_result120 != null) {
                                reply.writeInt(1);
                                TextUtils.writeToParcel(_result120, reply, 1);
                                break;
                            } else {
                                reply.writeInt(0);
                                break;
                            }
                        case 249:
                            int _result121 = getUserProvisioningState();
                            reply.writeNoException();
                            reply.writeInt(_result121);
                            break;
                        case 250:
                            int _arg0160 = data.readInt();
                            int _arg185 = data.readInt();
                            data.enforceNoDataAvail();
                            setUserProvisioningState(_arg0160, _arg185);
                            reply.writeNoException();
                            break;
                        case 251:
                            ComponentName _arg0161 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            List<String> _arg186 = data.createStringArrayList();
                            data.enforceNoDataAvail();
                            setAffiliationIds(_arg0161, _arg186);
                            reply.writeNoException();
                            break;
                        case 252:
                            ComponentName _arg0162 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            List<String> _result122 = getAffiliationIds(_arg0162);
                            reply.writeNoException();
                            reply.writeStringList(_result122);
                            break;
                        case 253:
                            boolean _result123 = isCallingUserAffiliated();
                            reply.writeNoException();
                            reply.writeBoolean(_result123);
                            break;
                        case 254:
                            int _arg0163 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result124 = isAffiliatedUser(_arg0163);
                            reply.writeNoException();
                            reply.writeBoolean(_result124);
                            break;
                        case 255:
                            return onTransact$setSecurityLoggingEnabled$(data, reply);
                        case 256:
                            ComponentName _arg0164 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            String _arg187 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result125 = isSecurityLoggingEnabled(_arg0164, _arg187);
                            reply.writeNoException();
                            reply.writeBoolean(_result125);
                            break;
                        case 257:
                            ComponentName _arg0165 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            String _arg188 = data.readString();
                            data.enforceNoDataAvail();
                            ParceledListSlice _result126 = retrieveSecurityLogs(_arg0165, _arg188);
                            reply.writeNoException();
                            reply.writeTypedObject(_result126, 1);
                            break;
                        case 258:
                            ComponentName _arg0166 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            String _arg189 = data.readString();
                            data.enforceNoDataAvail();
                            ParceledListSlice _result127 = retrievePreRebootSecurityLogs(_arg0166, _arg189);
                            reply.writeNoException();
                            reply.writeTypedObject(_result127, 1);
                            break;
                        case 259:
                            long _result128 = forceNetworkLogs();
                            reply.writeNoException();
                            reply.writeLong(_result128);
                            break;
                        case 260:
                            long _result129 = forceSecurityLogs();
                            reply.writeNoException();
                            reply.writeLong(_result129);
                            break;
                        case 261:
                            String _arg0167 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result130 = isUninstallInQueue(_arg0167);
                            reply.writeNoException();
                            reply.writeBoolean(_result130);
                            break;
                        case 262:
                            String _arg0168 = data.readString();
                            data.enforceNoDataAvail();
                            uninstallPackageWithActiveAdmins(_arg0168);
                            reply.writeNoException();
                            break;
                        case 263:
                            boolean _result131 = isDeviceProvisioned();
                            reply.writeNoException();
                            reply.writeBoolean(_result131);
                            break;
                        case 264:
                            boolean _result132 = isDeviceProvisioningConfigApplied();
                            reply.writeNoException();
                            reply.writeBoolean(_result132);
                            break;
                        case 265:
                            setDeviceProvisioningConfigApplied();
                            reply.writeNoException();
                            break;
                        case 266:
                            int _arg0169 = data.readInt();
                            data.enforceNoDataAvail();
                            forceUpdateUserSetupComplete(_arg0169);
                            reply.writeNoException();
                            break;
                        case 267:
                            ComponentName _arg0170 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            boolean _arg190 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setBackupServiceEnabled(_arg0170, _arg190);
                            reply.writeNoException();
                            break;
                        case 268:
                            ComponentName _arg0171 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result133 = isBackupServiceEnabled(_arg0171);
                            reply.writeNoException();
                            reply.writeBoolean(_result133);
                            break;
                        case 269:
                            return onTransact$setNetworkLoggingEnabled$(data, reply);
                        case 270:
                            ComponentName _arg0172 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            String _arg191 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result134 = isNetworkLoggingEnabled(_arg0172, _arg191);
                            reply.writeNoException();
                            reply.writeBoolean(_result134);
                            break;
                        case 271:
                            return onTransact$retrieveNetworkLogs$(data, reply);
                        case 272:
                            return onTransact$bindDeviceAdminServiceAsUser$(data, reply);
                        case 273:
                            ComponentName _arg0173 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            List<UserHandle> _result135 = getBindDeviceAdminTargetUsers(_arg0173);
                            reply.writeNoException();
                            reply.writeTypedList(_result135);
                            break;
                        case 274:
                            ComponentName _arg0174 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result136 = isEphemeralUser(_arg0174);
                            reply.writeNoException();
                            reply.writeBoolean(_result136);
                            break;
                        case 275:
                            long _result137 = getLastSecurityLogRetrievalTime();
                            reply.writeNoException();
                            reply.writeLong(_result137);
                            break;
                        case 276:
                            long _result138 = getLastBugReportRequestTime();
                            reply.writeNoException();
                            reply.writeLong(_result138);
                            break;
                        case 277:
                            long _result139 = getLastNetworkLogRetrievalTime();
                            reply.writeNoException();
                            reply.writeLong(_result139);
                            break;
                        case 278:
                            ComponentName _arg0175 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            byte[] _arg192 = data.createByteArray();
                            data.enforceNoDataAvail();
                            boolean _result140 = setResetPasswordToken(_arg0175, _arg192);
                            reply.writeNoException();
                            reply.writeBoolean(_result140);
                            break;
                        case 279:
                            ComponentName _arg0176 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result141 = clearResetPasswordToken(_arg0176);
                            reply.writeNoException();
                            reply.writeBoolean(_result141);
                            break;
                        case 280:
                            ComponentName _arg0177 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result142 = isResetPasswordTokenActive(_arg0177);
                            reply.writeNoException();
                            reply.writeBoolean(_result142);
                            break;
                        case 281:
                            return onTransact$resetPasswordWithToken$(data, reply);
                        case 282:
                            boolean _result143 = isCurrentInputMethodSetByOwner();
                            reply.writeNoException();
                            reply.writeBoolean(_result143);
                            break;
                        case 283:
                            UserHandle _arg0178 = (UserHandle) data.readTypedObject(UserHandle.CREATOR);
                            data.enforceNoDataAvail();
                            StringParceledListSlice _result144 = getOwnerInstalledCaCerts(_arg0178);
                            reply.writeNoException();
                            reply.writeTypedObject(_result144, 1);
                            break;
                        case 284:
                            return onTransact$clearApplicationUserData$(data, reply);
                        case 285:
                            ComponentName _arg0179 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            boolean _arg193 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setLogoutEnabled(_arg0179, _arg193);
                            reply.writeNoException();
                            break;
                        case 286:
                            boolean _result145 = isLogoutEnabled();
                            reply.writeNoException();
                            reply.writeBoolean(_result145);
                            break;
                        case 287:
                            return onTransact$getDisallowedSystemApps$(data, reply);
                        case 288:
                            return onTransact$transferOwnership$(data, reply);
                        case 289:
                            PersistableBundle _result146 = getTransferOwnershipBundle();
                            reply.writeNoException();
                            reply.writeTypedObject(_result146, 1);
                            break;
                        case 290:
                            ComponentName _arg0180 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            CharSequence _arg194 = (CharSequence) data.readTypedObject(TextUtils.CHAR_SEQUENCE_CREATOR);
                            data.enforceNoDataAvail();
                            setStartUserSessionMessage(_arg0180, _arg194);
                            reply.writeNoException();
                            break;
                        case 291:
                            ComponentName _arg0181 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            CharSequence _arg195 = (CharSequence) data.readTypedObject(TextUtils.CHAR_SEQUENCE_CREATOR);
                            data.enforceNoDataAvail();
                            setEndUserSessionMessage(_arg0181, _arg195);
                            reply.writeNoException();
                            break;
                        case 292:
                            ComponentName _arg0182 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            CharSequence _result147 = getStartUserSessionMessage(_arg0182);
                            reply.writeNoException();
                            if (_result147 != null) {
                                reply.writeInt(1);
                                TextUtils.writeToParcel(_result147, reply, 1);
                                break;
                            } else {
                                reply.writeInt(0);
                                break;
                            }
                        case 293:
                            ComponentName _arg0183 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            CharSequence _result148 = getEndUserSessionMessage(_arg0183);
                            reply.writeNoException();
                            if (_result148 != null) {
                                reply.writeInt(1);
                                TextUtils.writeToParcel(_result148, reply, 1);
                                break;
                            } else {
                                reply.writeInt(0);
                                break;
                            }
                        case 294:
                            ComponentName _arg0184 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            List<String> _arg196 = data.createStringArrayList();
                            data.enforceNoDataAvail();
                            List<String> _result149 = setMeteredDataDisabledPackages(_arg0184, _arg196);
                            reply.writeNoException();
                            reply.writeStringList(_result149);
                            break;
                        case 295:
                            ComponentName _arg0185 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            List<String> _result150 = getMeteredDataDisabledPackages(_arg0185);
                            reply.writeNoException();
                            reply.writeStringList(_result150);
                            break;
                        case 296:
                            ComponentName _arg0186 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            ApnSetting _arg197 = (ApnSetting) data.readTypedObject(ApnSetting.CREATOR);
                            data.enforceNoDataAvail();
                            int _result151 = addOverrideApn(_arg0186, _arg197);
                            reply.writeNoException();
                            reply.writeInt(_result151);
                            break;
                        case 297:
                            return onTransact$updateOverrideApn$(data, reply);
                        case 298:
                            ComponentName _arg0187 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg198 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result152 = removeOverrideApn(_arg0187, _arg198);
                            reply.writeNoException();
                            reply.writeBoolean(_result152);
                            break;
                        case 299:
                            ComponentName _arg0188 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            List<ApnSetting> _result153 = getOverrideApns(_arg0188);
                            reply.writeNoException();
                            reply.writeTypedList(_result153);
                            break;
                        case 300:
                            ComponentName _arg0189 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            boolean _arg199 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setOverrideApnsEnabled(_arg0189, _arg199);
                            reply.writeNoException();
                            break;
                        case 301:
                            ComponentName _arg0190 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result154 = isOverrideApnEnabled(_arg0190);
                            reply.writeNoException();
                            reply.writeBoolean(_result154);
                            break;
                        case 302:
                            return onTransact$isMeteredDataDisabledPackageForUser$(data, reply);
                        case 303:
                            return onTransact$setGlobalPrivateDns$(data, reply);
                        case 304:
                            ComponentName _arg0191 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            int _result155 = getGlobalPrivateDnsMode(_arg0191);
                            reply.writeNoException();
                            reply.writeInt(_result155);
                            break;
                        case 305:
                            ComponentName _arg0192 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            String _result156 = getGlobalPrivateDnsHost(_arg0192);
                            reply.writeNoException();
                            reply.writeString(_result156);
                            break;
                        case 306:
                            return onTransact$setProfileOwnerOnOrganizationOwnedDevice$(data, reply);
                        case 307:
                            return onTransact$installUpdateFromFile$(data, reply);
                        case 308:
                            ComponentName _arg0193 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            List<String> _arg1100 = data.createStringArrayList();
                            data.enforceNoDataAvail();
                            setCrossProfileCalendarPackages(_arg0193, _arg1100);
                            reply.writeNoException();
                            break;
                        case 309:
                            ComponentName _arg0194 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            List<String> _result157 = getCrossProfileCalendarPackages(_arg0194);
                            reply.writeNoException();
                            reply.writeStringList(_result157);
                            break;
                        case 310:
                            String _arg0195 = data.readString();
                            int _arg1101 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result158 = isPackageAllowedToAccessCalendarForUser(_arg0195, _arg1101);
                            reply.writeNoException();
                            reply.writeBoolean(_result158);
                            break;
                        case 311:
                            int _arg0196 = data.readInt();
                            data.enforceNoDataAvail();
                            List<String> _result159 = getCrossProfileCalendarPackagesForUser(_arg0196);
                            reply.writeNoException();
                            reply.writeStringList(_result159);
                            break;
                        case 312:
                            ComponentName _arg0197 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            List<String> _arg1102 = data.createStringArrayList();
                            data.enforceNoDataAvail();
                            setCrossProfilePackages(_arg0197, _arg1102);
                            reply.writeNoException();
                            break;
                        case 313:
                            ComponentName _arg0198 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            List<String> _result160 = getCrossProfilePackages(_arg0198);
                            reply.writeNoException();
                            reply.writeStringList(_result160);
                            break;
                        case 314:
                            List<String> _result161 = getAllCrossProfilePackages();
                            reply.writeNoException();
                            reply.writeStringList(_result161);
                            break;
                        case 315:
                            List<String> _result162 = getDefaultCrossProfilePackages();
                            reply.writeNoException();
                            reply.writeStringList(_result162);
                            break;
                        case 316:
                            boolean _result163 = isManagedKiosk();
                            reply.writeNoException();
                            reply.writeBoolean(_result163);
                            break;
                        case 317:
                            boolean _result164 = isUnattendedManagedKiosk();
                            reply.writeNoException();
                            reply.writeBoolean(_result164);
                            break;
                        case 318:
                            return onTransact$startViewCalendarEventInManagedProfile$(data, reply);
                        case 319:
                            return onTransact$setKeyGrantForApp$(data, reply);
                        case 320:
                            String _arg0199 = data.readString();
                            String _arg1103 = data.readString();
                            data.enforceNoDataAvail();
                            ParcelableGranteeMap _result165 = getKeyPairGrants(_arg0199, _arg1103);
                            reply.writeNoException();
                            reply.writeTypedObject(_result165, 1);
                            break;
                        case 321:
                            return onTransact$setKeyGrantToWifiAuth$(data, reply);
                        case 322:
                            String _arg0200 = data.readString();
                            String _arg1104 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result166 = isKeyPairGrantedToWifiAuth(_arg0200, _arg1104);
                            reply.writeNoException();
                            reply.writeBoolean(_result166);
                            break;
                        case 323:
                            ComponentName _arg0201 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            List<String> _arg1105 = data.createStringArrayList();
                            data.enforceNoDataAvail();
                            setUserControlDisabledPackages(_arg0201, _arg1105);
                            reply.writeNoException();
                            break;
                        case 324:
                            ComponentName _arg0202 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            List<String> _result167 = getUserControlDisabledPackages(_arg0202);
                            reply.writeNoException();
                            reply.writeStringList(_result167);
                            break;
                        case 325:
                            ComponentName _arg0203 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            boolean _arg1106 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setCommonCriteriaModeEnabled(_arg0203, _arg1106);
                            reply.writeNoException();
                            break;
                        case 326:
                            ComponentName _arg0204 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result168 = isCommonCriteriaModeEnabled(_arg0204);
                            reply.writeNoException();
                            reply.writeBoolean(_result168);
                            break;
                        case 327:
                            ComponentName _arg0205 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            int _result169 = getPersonalAppsSuspendedReasons(_arg0205);
                            reply.writeNoException();
                            reply.writeInt(_result169);
                            break;
                        case 328:
                            ComponentName _arg0206 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            boolean _arg1107 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setPersonalAppsSuspended(_arg0206, _arg1107);
                            reply.writeNoException();
                            break;
                        case 329:
                            ComponentName _arg0207 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            long _result170 = getManagedProfileMaximumTimeOff(_arg0207);
                            reply.writeNoException();
                            reply.writeLong(_result170);
                            break;
                        case 330:
                            ComponentName _arg0208 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            long _arg1108 = data.readLong();
                            data.enforceNoDataAvail();
                            setManagedProfileMaximumTimeOff(_arg0208, _arg1108);
                            reply.writeNoException();
                            break;
                        case 331:
                            acknowledgeDeviceCompliant();
                            reply.writeNoException();
                            break;
                        case 332:
                            boolean _result171 = isComplianceAcknowledgementRequired();
                            reply.writeNoException();
                            reply.writeBoolean(_result171);
                            break;
                        case 333:
                            int _arg0209 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result172 = canProfileOwnerResetPasswordWhenLocked(_arg0209);
                            reply.writeNoException();
                            reply.writeBoolean(_result172);
                            break;
                        case 334:
                            int _arg0210 = data.readInt();
                            int _arg1109 = data.readInt();
                            data.enforceNoDataAvail();
                            setNextOperationSafety(_arg0210, _arg1109);
                            reply.writeNoException();
                            break;
                        case 335:
                            int _arg0211 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result173 = isSafeOperation(_arg0211);
                            reply.writeNoException();
                            reply.writeBoolean(_result173);
                            break;
                        case 336:
                            String _arg0212 = data.readString();
                            data.enforceNoDataAvail();
                            String _result174 = getEnrollmentSpecificId(_arg0212);
                            reply.writeNoException();
                            reply.writeString(_result174);
                            break;
                        case 337:
                            return onTransact$setOrganizationIdForUser$(data, reply);
                        case 338:
                            ManagedProfileProvisioningParams _arg0213 = (ManagedProfileProvisioningParams) data.readTypedObject(ManagedProfileProvisioningParams.CREATOR);
                            String _arg1110 = data.readString();
                            data.enforceNoDataAvail();
                            UserHandle _result175 = createAndProvisionManagedProfile(_arg0213, _arg1110);
                            reply.writeNoException();
                            reply.writeTypedObject(_result175, 1);
                            break;
                        case 339:
                            FullyManagedDeviceProvisioningParams _arg0214 = (FullyManagedDeviceProvisioningParams) data.readTypedObject(FullyManagedDeviceProvisioningParams.CREATOR);
                            String _arg1111 = data.readString();
                            data.enforceNoDataAvail();
                            provisionFullyManagedDevice(_arg0214, _arg1111);
                            reply.writeNoException();
                            break;
                        case 340:
                            UserHandle _arg0215 = (UserHandle) data.readTypedObject(UserHandle.CREATOR);
                            Account _arg1112 = (Account) data.readTypedObject(Account.CREATOR);
                            data.enforceNoDataAvail();
                            finalizeWorkProfileProvisioning(_arg0215, _arg1112);
                            reply.writeNoException();
                            break;
                        case 341:
                            ComponentName _arg0216 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg1113 = data.readInt();
                            data.enforceNoDataAvail();
                            setDeviceOwnerType(_arg0216, _arg1113);
                            reply.writeNoException();
                            break;
                        case 342:
                            ComponentName _arg0217 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            int _result176 = getDeviceOwnerType(_arg0217);
                            reply.writeNoException();
                            reply.writeInt(_result176);
                            break;
                        case 343:
                            int _arg0218 = data.readInt();
                            data.enforceNoDataAvail();
                            resetDefaultCrossProfileIntentFilters(_arg0218);
                            reply.writeNoException();
                            break;
                        case 344:
                            int _arg0219 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result177 = canAdminGrantSensorsPermissionsForUser(_arg0219);
                            reply.writeNoException();
                            reply.writeBoolean(_result177);
                            break;
                        case 345:
                            String _arg0220 = data.readString();
                            boolean _arg1114 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setUsbDataSignalingEnabled(_arg0220, _arg1114);
                            reply.writeNoException();
                            break;
                        case 346:
                            String _arg0221 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result178 = isUsbDataSignalingEnabled(_arg0221);
                            reply.writeNoException();
                            reply.writeBoolean(_result178);
                            break;
                        case 347:
                            int _arg0222 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result179 = isUsbDataSignalingEnabledForUser(_arg0222);
                            reply.writeNoException();
                            reply.writeBoolean(_result179);
                            break;
                        case 348:
                            boolean _result180 = canUsbDataSignalingBeDisabled();
                            reply.writeNoException();
                            reply.writeBoolean(_result180);
                            break;
                        case 349:
                            int _arg0223 = data.readInt();
                            data.enforceNoDataAvail();
                            setMinimumRequiredWifiSecurityLevel(_arg0223);
                            reply.writeNoException();
                            break;
                        case 350:
                            int _result181 = getMinimumRequiredWifiSecurityLevel();
                            reply.writeNoException();
                            reply.writeInt(_result181);
                            break;
                        case 351:
                            WifiSsidPolicy _arg0224 = (WifiSsidPolicy) data.readTypedObject(WifiSsidPolicy.CREATOR);
                            data.enforceNoDataAvail();
                            setWifiSsidPolicy(_arg0224);
                            reply.writeNoException();
                            break;
                        case 352:
                            WifiSsidPolicy _result182 = getWifiSsidPolicy();
                            reply.writeNoException();
                            reply.writeTypedObject(_result182, 1);
                            break;
                        case 353:
                            List<UserHandle> _result183 = listForegroundAffiliatedUsers();
                            reply.writeNoException();
                            reply.writeTypedList(_result183);
                            break;
                        case 354:
                            List<DevicePolicyDrawableResource> _arg0225 = data.createTypedArrayList(DevicePolicyDrawableResource.CREATOR);
                            data.enforceNoDataAvail();
                            setDrawables(_arg0225);
                            reply.writeNoException();
                            break;
                        case 355:
                            List<String> _arg0226 = data.createStringArrayList();
                            data.enforceNoDataAvail();
                            resetDrawables(_arg0226);
                            reply.writeNoException();
                            break;
                        case 356:
                            return onTransact$getDrawable$(data, reply);
                        case 357:
                            boolean _result184 = isDpcDownloaded();
                            reply.writeNoException();
                            reply.writeBoolean(_result184);
                            break;
                        case 358:
                            boolean _arg0227 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setDpcDownloaded(_arg0227);
                            reply.writeNoException();
                            break;
                        case 359:
                            List<DevicePolicyStringResource> _arg0228 = data.createTypedArrayList(DevicePolicyStringResource.CREATOR);
                            data.enforceNoDataAvail();
                            setStrings(_arg0228);
                            reply.writeNoException();
                            break;
                        case 360:
                            List<String> _arg0229 = data.createStringArrayList();
                            data.enforceNoDataAvail();
                            resetStrings(_arg0229);
                            reply.writeNoException();
                            break;
                        case 361:
                            String _arg0230 = data.readString();
                            data.enforceNoDataAvail();
                            ParcelableResource _result185 = getString(_arg0230);
                            reply.writeNoException();
                            reply.writeTypedObject(_result185, 1);
                            break;
                        case 362:
                            boolean _result186 = shouldAllowBypassingDevicePolicyManagementRoleQualification();
                            reply.writeNoException();
                            reply.writeBoolean(_result186);
                            break;
                        case 363:
                            UserHandle _arg0231 = (UserHandle) data.readTypedObject(UserHandle.CREATOR);
                            data.enforceNoDataAvail();
                            List<UserHandle> _result187 = getPolicyManagedProfiles(_arg0231);
                            reply.writeNoException();
                            reply.writeTypedList(_result187);
                            break;
                        case 364:
                            ComponentName _arg0232 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg1115 = data.readInt();
                            data.enforceNoDataAvail();
                            setDualProfileEnabled(_arg0232, _arg1115);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements IDevicePolicyManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setPasswordQuality(ComponentName who, int quality, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(quality);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getPasswordQuality(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(userHandle);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setPasswordMinimumLength(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(length);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getPasswordMinimumLength(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(userHandle);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setPasswordMinimumUpperCase(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(length);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getPasswordMinimumUpperCase(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(userHandle);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setPasswordMinimumLowerCase(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(length);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getPasswordMinimumLowerCase(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(userHandle);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setPasswordMinimumLetters(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(length);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getPasswordMinimumLetters(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(userHandle);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setPasswordMinimumNumeric(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(length);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getPasswordMinimumNumeric(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(userHandle);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setPasswordMinimumSymbols(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(length);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getPasswordMinimumSymbols(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(userHandle);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setPasswordMinimumNonLetter(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(length);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getPasswordMinimumNonLetter(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(userHandle);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public PasswordMetrics getPasswordMinimumMetrics(int userHandle, boolean deviceWideOnly) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    _data.writeBoolean(deviceWideOnly);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    PasswordMetrics _result = (PasswordMetrics) _reply.readTypedObject(PasswordMetrics.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setPasswordHistoryLength(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(length);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getPasswordHistoryLength(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(userHandle);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setPasswordExpirationTimeout(ComponentName who, long expiration, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeLong(expiration);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public long getPasswordExpirationTimeout(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(userHandle);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public long getPasswordExpiration(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(userHandle);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isActivePasswordSufficient(int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isActivePasswordSufficientForDeviceRequirement() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isPasswordSufficientAfterProfileUnification(int userHandle, int profileUser) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    _data.writeInt(profileUser);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getPasswordComplexity(boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setRequiredPasswordComplexity(int passwordComplexity, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(passwordComplexity);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getRequiredPasswordComplexity(boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getAggregatedPasswordComplexityForUser(int userId, boolean deviceWideOnly) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeBoolean(deviceWideOnly);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isUsingUnifiedPassword(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getCurrentFailedPasswordAttempts(int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getProfileWithMinimumFailedPasswordsForWipe(int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setMaximumFailedPasswordsForWipe(ComponentName admin, int num, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeInt(num);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getMaximumFailedPasswordsForWipe(ComponentName admin, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeInt(userHandle);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean resetPassword(String password, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(password);
                    _data.writeInt(flags);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setMaximumTimeToLock(ComponentName who, long timeMs, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeLong(timeMs);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public long getMaximumTimeToLock(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(userHandle);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setRequiredStrongAuthTimeout(ComponentName who, long timeMs, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeLong(timeMs);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public long getRequiredStrongAuthTimeout(ComponentName who, int userId, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(userId);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void lockNow(int flags, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(flags);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void wipeDataWithReason(int flags, String wipeReasonForUser, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(flags);
                    _data.writeString(wipeReasonForUser);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setFactoryResetProtectionPolicy(ComponentName who, FactoryResetProtectionPolicy policy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeTypedObject(policy, 0);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public FactoryResetProtectionPolicy getFactoryResetProtectionPolicy(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                    FactoryResetProtectionPolicy _result = (FactoryResetProtectionPolicy) _reply.readTypedObject(FactoryResetProtectionPolicy.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isFactoryResetProtectionPolicySupported() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(44, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void sendLostModeLocationUpdate(AndroidFuture<Boolean> future) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(future, 0);
                    this.mRemote.transact(45, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public ComponentName setGlobalProxy(ComponentName admin, String proxySpec, String exclusionList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(proxySpec);
                    _data.writeString(exclusionList);
                    this.mRemote.transact(46, _data, _reply, 0);
                    _reply.readException();
                    ComponentName _result = (ComponentName) _reply.readTypedObject(ComponentName.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public ComponentName getGlobalProxyAdmin(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(47, _data, _reply, 0);
                    _reply.readException();
                    ComponentName _result = (ComponentName) _reply.readTypedObject(ComponentName.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setRecommendedGlobalProxy(ComponentName admin, ProxyInfo proxyInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeTypedObject(proxyInfo, 0);
                    this.mRemote.transact(48, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int setStorageEncryption(ComponentName who, boolean encrypt) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeBoolean(encrypt);
                    this.mRemote.transact(49, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean getStorageEncryption(ComponentName who, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(50, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getStorageEncryptionStatus(String callerPackage, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callerPackage);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(51, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean requestBugreport(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    this.mRemote.transact(52, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setCameraDisabled(ComponentName who, boolean disabled, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeBoolean(disabled);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean getCameraDisabled(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(userHandle);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(54, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setScreenCaptureDisabled(ComponentName who, boolean disabled, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeBoolean(disabled);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(55, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean getScreenCaptureDisabled(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(userHandle);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(56, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setNearbyNotificationStreamingPolicy(int policy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(policy);
                    this.mRemote.transact(57, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getNearbyNotificationStreamingPolicy(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(58, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setNearbyAppStreamingPolicy(int policy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(policy);
                    this.mRemote.transact(59, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getNearbyAppStreamingPolicy(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(60, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setKeyguardDisabledFeatures(ComponentName who, int which, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(which);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(61, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getKeyguardDisabledFeatures(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(userHandle);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(62, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setActiveAdmin(ComponentName policyReceiver, boolean refreshing, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(policyReceiver, 0);
                    _data.writeBoolean(refreshing);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(63, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isAdminActive(ComponentName policyReceiver, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(policyReceiver, 0);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(64, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<ComponentName> getActiveAdmins(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(65, _data, _reply, 0);
                    _reply.readException();
                    List<ComponentName> _result = _reply.createTypedArrayList(ComponentName.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean packageHasActiveAdmins(String packageName, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(66, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void getRemoveWarning(ComponentName policyReceiver, RemoteCallback result, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(policyReceiver, 0);
                    _data.writeTypedObject(result, 0);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(67, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void removeActiveAdmin(ComponentName policyReceiver, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(policyReceiver, 0);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(68, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void forceRemoveActiveAdmin(ComponentName policyReceiver, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(policyReceiver, 0);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(69, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean hasGrantedPolicy(ComponentName policyReceiver, int usesPolicy, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(policyReceiver, 0);
                    _data.writeInt(usesPolicy);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(70, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void reportPasswordChanged(PasswordMetrics metrics, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(metrics, 0);
                    _data.writeInt(userId);
                    this.mRemote.transact(71, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void reportFailedPasswordAttempt(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(72, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void reportSuccessfulPasswordAttempt(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(73, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void reportFailedBiometricAttempt(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(74, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void reportSuccessfulBiometricAttempt(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(75, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void reportKeyguardDismissed(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(76, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void reportKeyguardSecured(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(77, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean setDeviceOwner(ComponentName who, String ownerName, int userId, boolean setProfileOwnerOnCurrentUserIfNecessary) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeString(ownerName);
                    _data.writeInt(userId);
                    _data.writeBoolean(setProfileOwnerOnCurrentUserIfNecessary);
                    this.mRemote.transact(78, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public ComponentName getDeviceOwnerComponent(boolean callingUserOnly) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(callingUserOnly);
                    this.mRemote.transact(79, _data, _reply, 0);
                    _reply.readException();
                    ComponentName _result = (ComponentName) _reply.readTypedObject(ComponentName.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean hasDeviceOwner() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(80, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public String getDeviceOwnerName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(81, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void clearDeviceOwner(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(82, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getDeviceOwnerUserId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(83, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean setProfileOwner(ComponentName who, String ownerName, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeString(ownerName);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(84, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public ComponentName getProfileOwnerAsUser(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(85, _data, _reply, 0);
                    _reply.readException();
                    ComponentName _result = (ComponentName) _reply.readTypedObject(ComponentName.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public ComponentName getProfileOwnerOrDeviceOwnerSupervisionComponent(UserHandle userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(userHandle, 0);
                    this.mRemote.transact(86, _data, _reply, 0);
                    _reply.readException();
                    ComponentName _result = (ComponentName) _reply.readTypedObject(ComponentName.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isSupervisionComponent(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    this.mRemote.transact(87, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public String getProfileOwnerName(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(88, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setProfileEnabled(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    this.mRemote.transact(89, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setProfileName(ComponentName who, String profileName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeString(profileName);
                    this.mRemote.transact(90, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void clearProfileOwner(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    this.mRemote.transact(91, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean hasUserSetupCompleted() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(92, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isOrganizationOwnedDeviceWithManagedProfile() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(93, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean checkDeviceIdentifierAccess(String packageName, int pid, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    this.mRemote.transact(94, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setDeviceOwnerLockScreenInfo(ComponentName who, CharSequence deviceOwnerInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    if (deviceOwnerInfo != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(deviceOwnerInfo, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(95, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public CharSequence getDeviceOwnerLockScreenInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(96, _data, _reply, 0);
                    _reply.readException();
                    CharSequence _result = (CharSequence) _reply.readTypedObject(TextUtils.CHAR_SEQUENCE_CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public String[] setPackagesSuspended(ComponentName admin, String callerPackage, String[] packageNames, boolean suspended) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(callerPackage);
                    _data.writeStringArray(packageNames);
                    _data.writeBoolean(suspended);
                    this.mRemote.transact(97, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isPackageSuspended(ComponentName admin, String callerPackage, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(callerPackage);
                    _data.writeString(packageName);
                    this.mRemote.transact(98, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<String> listPolicyExemptApps() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(99, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean installCaCert(ComponentName admin, String callerPackage, byte[] certBuffer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(callerPackage);
                    _data.writeByteArray(certBuffer);
                    this.mRemote.transact(100, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void uninstallCaCerts(ComponentName admin, String callerPackage, String[] aliases) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(callerPackage);
                    _data.writeStringArray(aliases);
                    this.mRemote.transact(101, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void enforceCanManageCaCerts(ComponentName admin, String callerPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(callerPackage);
                    this.mRemote.transact(102, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean approveCaCert(String alias, int userHandle, boolean approval) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(alias);
                    _data.writeInt(userHandle);
                    _data.writeBoolean(approval);
                    this.mRemote.transact(103, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isCaCertApproved(String alias, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(alias);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(104, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean installKeyPair(ComponentName who, String callerPackage, byte[] privKeyBuffer, byte[] certBuffer, byte[] certChainBuffer, String alias, boolean requestAccess, boolean isUserSelectable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeString(callerPackage);
                    _data.writeByteArray(privKeyBuffer);
                    _data.writeByteArray(certBuffer);
                    _data.writeByteArray(certChainBuffer);
                    _data.writeString(alias);
                    _data.writeBoolean(requestAccess);
                    _data.writeBoolean(isUserSelectable);
                    this.mRemote.transact(105, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean removeKeyPair(ComponentName who, String callerPackage, String alias) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeString(callerPackage);
                    _data.writeString(alias);
                    this.mRemote.transact(106, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean hasKeyPair(String callerPackage, String alias) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callerPackage);
                    _data.writeString(alias);
                    this.mRemote.transact(107, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean generateKeyPair(ComponentName who, String callerPackage, String algorithm, ParcelableKeyGenParameterSpec keySpec, int idAttestationFlags, KeymasterCertificateChain attestationChain) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeString(callerPackage);
                    _data.writeString(algorithm);
                    _data.writeTypedObject(keySpec, 0);
                    _data.writeInt(idAttestationFlags);
                    this.mRemote.transact(108, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    if (_reply.readInt() != 0) {
                        attestationChain.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean setKeyPairCertificate(ComponentName who, String callerPackage, String alias, byte[] certBuffer, byte[] certChainBuffer, boolean isUserSelectable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeString(callerPackage);
                    _data.writeString(alias);
                    _data.writeByteArray(certBuffer);
                    _data.writeByteArray(certChainBuffer);
                    _data.writeBoolean(isUserSelectable);
                    this.mRemote.transact(109, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void choosePrivateKeyAlias(int uid, Uri uri, String alias, IBinder aliasCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeTypedObject(uri, 0);
                    _data.writeString(alias);
                    _data.writeStrongBinder(aliasCallback);
                    this.mRemote.transact(110, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setDelegatedScopes(ComponentName who, String delegatePackage, List<String> scopes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeString(delegatePackage);
                    _data.writeStringList(scopes);
                    this.mRemote.transact(111, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<String> getDelegatedScopes(ComponentName who, String delegatePackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeString(delegatePackage);
                    this.mRemote.transact(112, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<String> getDelegatePackages(ComponentName who, String scope) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeString(scope);
                    this.mRemote.transact(113, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setCertInstallerPackage(ComponentName who, String installerPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeString(installerPackage);
                    this.mRemote.transact(114, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public String getCertInstallerPackage(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    this.mRemote.transact(115, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean setAlwaysOnVpnPackage(ComponentName who, String vpnPackage, boolean lockdown, List<String> lockdownAllowlist) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeString(vpnPackage);
                    _data.writeBoolean(lockdown);
                    _data.writeStringList(lockdownAllowlist);
                    this.mRemote.transact(116, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public String getAlwaysOnVpnPackage(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    this.mRemote.transact(117, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public String getAlwaysOnVpnPackageForUser(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(118, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isAlwaysOnVpnLockdownEnabled(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    this.mRemote.transact(119, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isAlwaysOnVpnLockdownEnabledForUser(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(120, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<String> getAlwaysOnVpnLockdownAllowlist(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    this.mRemote.transact(121, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void addPersistentPreferredActivity(ComponentName admin, IntentFilter filter, ComponentName activity) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeTypedObject(filter, 0);
                    _data.writeTypedObject(activity, 0);
                    this.mRemote.transact(122, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void clearPackagePersistentPreferredActivities(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(packageName);
                    this.mRemote.transact(123, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setDefaultSmsApplication(ComponentName admin, String packageName, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(packageName);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(124, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setApplicationRestrictions(ComponentName who, String callerPackage, String packageName, Bundle settings) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeString(callerPackage);
                    _data.writeString(packageName);
                    _data.writeTypedObject(settings, 0);
                    this.mRemote.transact(125, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public Bundle getApplicationRestrictions(ComponentName who, String callerPackage, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeString(callerPackage);
                    _data.writeString(packageName);
                    this.mRemote.transact(126, _data, _reply, 0);
                    _reply.readException();
                    Bundle _result = (Bundle) _reply.readTypedObject(Bundle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean setApplicationRestrictionsManagingPackage(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(packageName);
                    this.mRemote.transact(127, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public String getApplicationRestrictionsManagingPackage(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(128, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isCallerApplicationRestrictionsManagingPackage(String callerPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callerPackage);
                    this.mRemote.transact(129, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setRestrictionsProvider(ComponentName who, ComponentName provider) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeTypedObject(provider, 0);
                    this.mRemote.transact(130, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public ComponentName getRestrictionsProvider(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(131, _data, _reply, 0);
                    _reply.readException();
                    ComponentName _result = (ComponentName) _reply.readTypedObject(ComponentName.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setUserRestriction(ComponentName who, String key, boolean enable, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeString(key);
                    _data.writeBoolean(enable);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(132, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public Bundle getUserRestrictions(ComponentName who, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(133, _data, _reply, 0);
                    _reply.readException();
                    Bundle _result = (Bundle) _reply.readTypedObject(Bundle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void addCrossProfileIntentFilter(ComponentName admin, IntentFilter filter, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeTypedObject(filter, 0);
                    _data.writeInt(flags);
                    this.mRemote.transact(134, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void clearCrossProfileIntentFilters(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(135, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean setPermittedAccessibilityServices(ComponentName admin, List<String> packageList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeStringList(packageList);
                    this.mRemote.transact(136, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<String> getPermittedAccessibilityServices(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(137, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<String> getPermittedAccessibilityServicesForUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(138, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isAccessibilityServicePermittedByAdmin(ComponentName admin, String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(139, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean setPermittedInputMethods(ComponentName admin, List<String> packageList, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeStringList(packageList);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(140, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<String> getPermittedInputMethods(ComponentName admin, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(141, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<String> getPermittedInputMethodsAsUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(142, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isInputMethodPermittedByAdmin(ComponentName admin, String packageName, int userId, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(143, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean setPermittedCrossProfileNotificationListeners(ComponentName admin, List<String> packageList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeStringList(packageList);
                    this.mRemote.transact(144, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<String> getPermittedCrossProfileNotificationListeners(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(145, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isNotificationListenerServicePermitted(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(146, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public Intent createAdminSupportIntent(String restriction) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(restriction);
                    this.mRemote.transact(147, _data, _reply, 0);
                    _reply.readException();
                    Intent _result = (Intent) _reply.readTypedObject(Intent.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public Bundle getEnforcingAdminAndUserDetails(int userId, String restriction) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeString(restriction);
                    this.mRemote.transact(148, _data, _reply, 0);
                    _reply.readException();
                    Bundle _result = (Bundle) _reply.readTypedObject(Bundle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean setApplicationHidden(ComponentName admin, String callerPackage, String packageName, boolean hidden, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(callerPackage);
                    _data.writeString(packageName);
                    _data.writeBoolean(hidden);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(149, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isApplicationHidden(ComponentName admin, String callerPackage, String packageName, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(callerPackage);
                    _data.writeString(packageName);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(150, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public UserHandle createAndManageUser(ComponentName who, String name, ComponentName profileOwner, PersistableBundle adminExtras, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeString(name);
                    _data.writeTypedObject(profileOwner, 0);
                    _data.writeTypedObject(adminExtras, 0);
                    _data.writeInt(flags);
                    this.mRemote.transact(151, _data, _reply, 0);
                    _reply.readException();
                    UserHandle _result = (UserHandle) _reply.readTypedObject(UserHandle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean removeUser(ComponentName who, UserHandle userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeTypedObject(userHandle, 0);
                    this.mRemote.transact(152, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean switchUser(ComponentName who, UserHandle userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeTypedObject(userHandle, 0);
                    this.mRemote.transact(153, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int startUserInBackground(ComponentName who, UserHandle userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeTypedObject(userHandle, 0);
                    this.mRemote.transact(154, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int stopUser(ComponentName who, UserHandle userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeTypedObject(userHandle, 0);
                    this.mRemote.transact(155, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int logoutUser(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    this.mRemote.transact(156, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int logoutUserInternal() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(157, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getLogoutUserId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(158, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<UserHandle> getSecondaryUsers(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    this.mRemote.transact(159, _data, _reply, 0);
                    _reply.readException();
                    List<UserHandle> _result = _reply.createTypedArrayList(UserHandle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void acknowledgeNewUserDisclaimer(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(160, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isNewUserDisclaimerAcknowledged(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(161, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void enableSystemApp(ComponentName admin, String callerPackage, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(callerPackage);
                    _data.writeString(packageName);
                    this.mRemote.transact(162, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int enableSystemAppWithIntent(ComponentName admin, String callerPackage, Intent intent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(callerPackage);
                    _data.writeTypedObject(intent, 0);
                    this.mRemote.transact(163, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean installExistingPackage(ComponentName admin, String callerPackage, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(callerPackage);
                    _data.writeString(packageName);
                    this.mRemote.transact(164, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setAccountManagementDisabled(ComponentName who, String accountType, boolean disabled, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeString(accountType);
                    _data.writeBoolean(disabled);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(165, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public String[] getAccountTypesWithManagementDisabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(166, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public String[] getAccountTypesWithManagementDisabledAsUser(int userId, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(167, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setSecondaryLockscreenEnabled(ComponentName who, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(168, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isSecondaryLockscreenEnabled(UserHandle userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(userHandle, 0);
                    this.mRemote.transact(169, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setPreferentialNetworkServiceConfigs(List<PreferentialNetworkServiceConfig> preferentialNetworkServiceConfigs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(preferentialNetworkServiceConfigs);
                    this.mRemote.transact(170, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<PreferentialNetworkServiceConfig> getPreferentialNetworkServiceConfigs() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(171, _data, _reply, 0);
                    _reply.readException();
                    List<PreferentialNetworkServiceConfig> _result = _reply.createTypedArrayList(PreferentialNetworkServiceConfig.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setLockTaskPackages(ComponentName who, String[] packages) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeStringArray(packages);
                    this.mRemote.transact(172, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public String[] getLockTaskPackages(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    this.mRemote.transact(173, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isLockTaskPermitted(String pkg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    this.mRemote.transact(174, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setLockTaskFeatures(ComponentName who, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(flags);
                    this.mRemote.transact(175, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getLockTaskFeatures(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    this.mRemote.transact(176, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setGlobalSetting(ComponentName who, String setting, String value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeString(setting);
                    _data.writeString(value);
                    this.mRemote.transact(177, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setSystemSetting(ComponentName who, String setting, String value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeString(setting);
                    _data.writeString(value);
                    this.mRemote.transact(178, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setSecureSetting(ComponentName who, String setting, String value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeString(setting);
                    _data.writeString(value);
                    this.mRemote.transact(179, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setConfiguredNetworksLockdownState(ComponentName who, boolean lockdown) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeBoolean(lockdown);
                    this.mRemote.transact(180, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean hasLockdownAdminConfiguredNetworks(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    this.mRemote.transact(181, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setLocationEnabled(ComponentName who, boolean locationEnabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeBoolean(locationEnabled);
                    this.mRemote.transact(182, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean setTime(ComponentName who, long millis) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeLong(millis);
                    this.mRemote.transact(183, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean setTimeZone(ComponentName who, String timeZone) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeString(timeZone);
                    this.mRemote.transact(184, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setMasterVolumeMuted(ComponentName admin, boolean on) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeBoolean(on);
                    this.mRemote.transact(185, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isMasterVolumeMuted(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(186, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void notifyLockTaskModeChanged(boolean isEnabled, String pkg, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(isEnabled);
                    _data.writeString(pkg);
                    _data.writeInt(userId);
                    this.mRemote.transact(187, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setUninstallBlocked(ComponentName admin, String callerPackage, String packageName, boolean uninstallBlocked) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(callerPackage);
                    _data.writeString(packageName);
                    _data.writeBoolean(uninstallBlocked);
                    this.mRemote.transact(188, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isUninstallBlocked(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(packageName);
                    this.mRemote.transact(189, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setCrossProfileCallerIdDisabled(ComponentName who, boolean disabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeBoolean(disabled);
                    this.mRemote.transact(190, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean getCrossProfileCallerIdDisabled(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    this.mRemote.transact(191, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean getCrossProfileCallerIdDisabledForUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(192, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setCrossProfileContactsSearchDisabled(ComponentName who, boolean disabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeBoolean(disabled);
                    this.mRemote.transact(193, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean getCrossProfileContactsSearchDisabled(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    this.mRemote.transact(194, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean getCrossProfileContactsSearchDisabledForUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(195, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void startManagedQuickContact(String lookupKey, long contactId, boolean isContactIdIgnored, long directoryId, Intent originalIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(lookupKey);
                    _data.writeLong(contactId);
                    _data.writeBoolean(isContactIdIgnored);
                    _data.writeLong(directoryId);
                    _data.writeTypedObject(originalIntent, 0);
                    this.mRemote.transact(196, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setBluetoothContactSharingDisabled(ComponentName who, boolean disabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeBoolean(disabled);
                    this.mRemote.transact(197, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean getBluetoothContactSharingDisabled(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    this.mRemote.transact(198, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean getBluetoothContactSharingDisabledForUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(199, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setTrustAgentConfiguration(ComponentName admin, ComponentName agent, PersistableBundle args, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeTypedObject(agent, 0);
                    _data.writeTypedObject(args, 0);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(200, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<PersistableBundle> getTrustAgentConfiguration(ComponentName admin, ComponentName agent, int userId, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeTypedObject(agent, 0);
                    _data.writeInt(userId);
                    _data.writeBoolean(parent);
                    this.mRemote.transact(201, _data, _reply, 0);
                    _reply.readException();
                    List<PersistableBundle> _result = _reply.createTypedArrayList(PersistableBundle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean addCrossProfileWidgetProvider(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(packageName);
                    this.mRemote.transact(202, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean removeCrossProfileWidgetProvider(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(packageName);
                    this.mRemote.transact(203, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<String> getCrossProfileWidgetProviders(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(204, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setAutoTimeRequired(ComponentName who, boolean required) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeBoolean(required);
                    this.mRemote.transact(205, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean getAutoTimeRequired() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(206, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setAutoTimeEnabled(ComponentName who, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(207, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean getAutoTimeEnabled(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    this.mRemote.transact(208, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setAutoTimeZoneEnabled(ComponentName who, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(209, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean getAutoTimeZoneEnabled(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    this.mRemote.transact(210, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setForceEphemeralUsers(ComponentName who, boolean forceEpehemeralUsers) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeBoolean(forceEpehemeralUsers);
                    this.mRemote.transact(211, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean getForceEphemeralUsers(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    this.mRemote.transact(212, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isRemovingAdmin(ComponentName adminReceiver, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(adminReceiver, 0);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(213, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setUserIcon(ComponentName admin, Bitmap icon) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeTypedObject(icon, 0);
                    this.mRemote.transact(214, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setSystemUpdatePolicy(ComponentName who, SystemUpdatePolicy policy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeTypedObject(policy, 0);
                    this.mRemote.transact(215, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public SystemUpdatePolicy getSystemUpdatePolicy() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(216, _data, _reply, 0);
                    _reply.readException();
                    SystemUpdatePolicy _result = (SystemUpdatePolicy) _reply.readTypedObject(SystemUpdatePolicy.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void clearSystemUpdatePolicyFreezePeriodRecord() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(217, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean setKeyguardDisabled(ComponentName admin, boolean disabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeBoolean(disabled);
                    this.mRemote.transact(218, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean setStatusBarDisabled(ComponentName who, boolean disabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeBoolean(disabled);
                    this.mRemote.transact(219, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean getDoNotAskCredentialsOnBoot() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(220, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void notifyPendingSystemUpdate(SystemUpdateInfo info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(info, 0);
                    this.mRemote.transact(221, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public SystemUpdateInfo getPendingSystemUpdate(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(222, _data, _reply, 0);
                    _reply.readException();
                    SystemUpdateInfo _result = (SystemUpdateInfo) _reply.readTypedObject(SystemUpdateInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setPermissionPolicy(ComponentName admin, String callerPackage, int policy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(callerPackage);
                    _data.writeInt(policy);
                    this.mRemote.transact(223, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getPermissionPolicy(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(224, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setPermissionGrantState(ComponentName admin, String callerPackage, String packageName, String permission, int grantState, RemoteCallback resultReceiver) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(callerPackage);
                    _data.writeString(packageName);
                    _data.writeString(permission);
                    _data.writeInt(grantState);
                    _data.writeTypedObject(resultReceiver, 0);
                    this.mRemote.transact(225, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getPermissionGrantState(ComponentName admin, String callerPackage, String packageName, String permission) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(callerPackage);
                    _data.writeString(packageName);
                    _data.writeString(permission);
                    this.mRemote.transact(226, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isProvisioningAllowed(String action, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(action);
                    _data.writeString(packageName);
                    this.mRemote.transact(227, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int checkProvisioningPrecondition(String action, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(action);
                    _data.writeString(packageName);
                    this.mRemote.transact(228, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setKeepUninstalledPackages(ComponentName admin, String callerPackage, List<String> packageList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(callerPackage);
                    _data.writeStringList(packageList);
                    this.mRemote.transact(229, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<String> getKeepUninstalledPackages(ComponentName admin, String callerPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(callerPackage);
                    this.mRemote.transact(230, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isManagedProfile(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(231, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public String getWifiMacAddress(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(232, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void reboot(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(233, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setShortSupportMessage(ComponentName admin, CharSequence message) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    if (message != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(message, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(234, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public CharSequence getShortSupportMessage(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(235, _data, _reply, 0);
                    _reply.readException();
                    CharSequence _result = (CharSequence) _reply.readTypedObject(TextUtils.CHAR_SEQUENCE_CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setLongSupportMessage(ComponentName admin, CharSequence message) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    if (message != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(message, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(236, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public CharSequence getLongSupportMessage(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(237, _data, _reply, 0);
                    _reply.readException();
                    CharSequence _result = (CharSequence) _reply.readTypedObject(TextUtils.CHAR_SEQUENCE_CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public CharSequence getShortSupportMessageForUser(ComponentName admin, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(238, _data, _reply, 0);
                    _reply.readException();
                    CharSequence _result = (CharSequence) _reply.readTypedObject(TextUtils.CHAR_SEQUENCE_CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public CharSequence getLongSupportMessageForUser(ComponentName admin, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(239, _data, _reply, 0);
                    _reply.readException();
                    CharSequence _result = (CharSequence) _reply.readTypedObject(TextUtils.CHAR_SEQUENCE_CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setOrganizationColor(ComponentName admin, int color) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeInt(color);
                    this.mRemote.transact(240, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setOrganizationColorForUser(int color, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(color);
                    _data.writeInt(userId);
                    this.mRemote.transact(241, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void clearOrganizationIdForUser(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(242, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getOrganizationColor(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(243, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getOrganizationColorForUser(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(244, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setOrganizationName(ComponentName admin, CharSequence title) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    if (title != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(title, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(245, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public CharSequence getOrganizationName(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(246, _data, _reply, 0);
                    _reply.readException();
                    CharSequence _result = (CharSequence) _reply.readTypedObject(TextUtils.CHAR_SEQUENCE_CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public CharSequence getDeviceOwnerOrganizationName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(247, _data, _reply, 0);
                    _reply.readException();
                    CharSequence _result = (CharSequence) _reply.readTypedObject(TextUtils.CHAR_SEQUENCE_CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public CharSequence getOrganizationNameForUser(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(248, _data, _reply, 0);
                    _reply.readException();
                    CharSequence _result = (CharSequence) _reply.readTypedObject(TextUtils.CHAR_SEQUENCE_CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getUserProvisioningState() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(249, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setUserProvisioningState(int state, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(state);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(250, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setAffiliationIds(ComponentName admin, List<String> ids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeStringList(ids);
                    this.mRemote.transact(251, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<String> getAffiliationIds(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(252, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isCallingUserAffiliated() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(253, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isAffiliatedUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(254, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setSecurityLoggingEnabled(ComponentName admin, String packageName, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(packageName);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(255, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isSecurityLoggingEnabled(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(packageName);
                    this.mRemote.transact(256, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public ParceledListSlice retrieveSecurityLogs(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(packageName);
                    this.mRemote.transact(257, _data, _reply, 0);
                    _reply.readException();
                    ParceledListSlice _result = (ParceledListSlice) _reply.readTypedObject(ParceledListSlice.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public ParceledListSlice retrievePreRebootSecurityLogs(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(packageName);
                    this.mRemote.transact(258, _data, _reply, 0);
                    _reply.readException();
                    ParceledListSlice _result = (ParceledListSlice) _reply.readTypedObject(ParceledListSlice.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public long forceNetworkLogs() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(259, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public long forceSecurityLogs() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(260, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isUninstallInQueue(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(261, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void uninstallPackageWithActiveAdmins(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(262, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isDeviceProvisioned() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(263, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isDeviceProvisioningConfigApplied() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(264, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setDeviceProvisioningConfigApplied() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(265, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void forceUpdateUserSetupComplete(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(266, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setBackupServiceEnabled(ComponentName admin, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(267, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isBackupServiceEnabled(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(268, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setNetworkLoggingEnabled(ComponentName admin, String packageName, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(packageName);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(269, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isNetworkLoggingEnabled(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(packageName);
                    this.mRemote.transact(270, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<NetworkEvent> retrieveNetworkLogs(ComponentName admin, String packageName, long batchToken) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(packageName);
                    _data.writeLong(batchToken);
                    this.mRemote.transact(271, _data, _reply, 0);
                    _reply.readException();
                    List<NetworkEvent> _result = _reply.createTypedArrayList(NetworkEvent.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean bindDeviceAdminServiceAsUser(ComponentName admin, IApplicationThread caller, IBinder token, Intent service, IServiceConnection connection, int flags, int targetUserId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeStrongInterface(caller);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(service, 0);
                    _data.writeStrongInterface(connection);
                    _data.writeInt(flags);
                    _data.writeInt(targetUserId);
                    this.mRemote.transact(272, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<UserHandle> getBindDeviceAdminTargetUsers(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(273, _data, _reply, 0);
                    _reply.readException();
                    List<UserHandle> _result = _reply.createTypedArrayList(UserHandle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isEphemeralUser(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(274, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public long getLastSecurityLogRetrievalTime() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(275, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public long getLastBugReportRequestTime() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(276, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public long getLastNetworkLogRetrievalTime() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(277, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean setResetPasswordToken(ComponentName admin, byte[] token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeByteArray(token);
                    this.mRemote.transact(278, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean clearResetPasswordToken(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(279, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isResetPasswordTokenActive(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(280, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean resetPasswordWithToken(ComponentName admin, String password, byte[] token, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(password);
                    _data.writeByteArray(token);
                    _data.writeInt(flags);
                    this.mRemote.transact(281, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isCurrentInputMethodSetByOwner() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(282, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public StringParceledListSlice getOwnerInstalledCaCerts(UserHandle user) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(user, 0);
                    this.mRemote.transact(283, _data, _reply, 0);
                    _reply.readException();
                    StringParceledListSlice _result = (StringParceledListSlice) _reply.readTypedObject(StringParceledListSlice.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void clearApplicationUserData(ComponentName admin, String packageName, IPackageDataObserver callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(packageName);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(284, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setLogoutEnabled(ComponentName admin, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(285, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isLogoutEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(286, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<String> getDisallowedSystemApps(ComponentName admin, int userId, String provisioningAction) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeInt(userId);
                    _data.writeString(provisioningAction);
                    this.mRemote.transact(287, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void transferOwnership(ComponentName admin, ComponentName target, PersistableBundle bundle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeTypedObject(target, 0);
                    _data.writeTypedObject(bundle, 0);
                    this.mRemote.transact(288, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public PersistableBundle getTransferOwnershipBundle() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(289, _data, _reply, 0);
                    _reply.readException();
                    PersistableBundle _result = (PersistableBundle) _reply.readTypedObject(PersistableBundle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setStartUserSessionMessage(ComponentName admin, CharSequence startUserSessionMessage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    if (startUserSessionMessage != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(startUserSessionMessage, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(290, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setEndUserSessionMessage(ComponentName admin, CharSequence endUserSessionMessage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    if (endUserSessionMessage != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(endUserSessionMessage, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(291, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public CharSequence getStartUserSessionMessage(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(292, _data, _reply, 0);
                    _reply.readException();
                    CharSequence _result = (CharSequence) _reply.readTypedObject(TextUtils.CHAR_SEQUENCE_CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public CharSequence getEndUserSessionMessage(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(293, _data, _reply, 0);
                    _reply.readException();
                    CharSequence _result = (CharSequence) _reply.readTypedObject(TextUtils.CHAR_SEQUENCE_CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<String> setMeteredDataDisabledPackages(ComponentName admin, List<String> packageNames) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeStringList(packageNames);
                    this.mRemote.transact(294, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<String> getMeteredDataDisabledPackages(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(295, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int addOverrideApn(ComponentName admin, ApnSetting apnSetting) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeTypedObject(apnSetting, 0);
                    this.mRemote.transact(296, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean updateOverrideApn(ComponentName admin, int apnId, ApnSetting apnSetting) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeInt(apnId);
                    _data.writeTypedObject(apnSetting, 0);
                    this.mRemote.transact(297, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean removeOverrideApn(ComponentName admin, int apnId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeInt(apnId);
                    this.mRemote.transact(298, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<ApnSetting> getOverrideApns(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(299, _data, _reply, 0);
                    _reply.readException();
                    List<ApnSetting> _result = _reply.createTypedArrayList(ApnSetting.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setOverrideApnsEnabled(ComponentName admin, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(300, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isOverrideApnEnabled(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(301, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isMeteredDataDisabledPackageForUser(ComponentName admin, String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(302, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int setGlobalPrivateDns(ComponentName admin, int mode, String privateDnsHost) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeInt(mode);
                    _data.writeString(privateDnsHost);
                    this.mRemote.transact(303, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getGlobalPrivateDnsMode(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(304, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public String getGlobalPrivateDnsHost(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(305, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setProfileOwnerOnOrganizationOwnedDevice(ComponentName who, int userId, boolean isProfileOwnerOnOrganizationOwnedDevice) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(userId);
                    _data.writeBoolean(isProfileOwnerOnOrganizationOwnedDevice);
                    this.mRemote.transact(306, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void installUpdateFromFile(ComponentName admin, ParcelFileDescriptor updateFileDescriptor, StartInstallingUpdateCallback listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeTypedObject(updateFileDescriptor, 0);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(307, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setCrossProfileCalendarPackages(ComponentName admin, List<String> packageNames) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeStringList(packageNames);
                    this.mRemote.transact(308, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<String> getCrossProfileCalendarPackages(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(309, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isPackageAllowedToAccessCalendarForUser(String packageName, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(310, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<String> getCrossProfileCalendarPackagesForUser(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(311, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setCrossProfilePackages(ComponentName admin, List<String> packageNames) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeStringList(packageNames);
                    this.mRemote.transact(312, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<String> getCrossProfilePackages(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(313, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<String> getAllCrossProfilePackages() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(314, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<String> getDefaultCrossProfilePackages() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(315, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isManagedKiosk() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(316, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isUnattendedManagedKiosk() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(317, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean startViewCalendarEventInManagedProfile(String packageName, long eventId, long start, long end, boolean allDay, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeLong(eventId);
                    _data.writeLong(start);
                    _data.writeLong(end);
                    _data.writeBoolean(allDay);
                    _data.writeInt(flags);
                    this.mRemote.transact(318, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean setKeyGrantForApp(ComponentName admin, String callerPackage, String alias, String packageName, boolean hasGrant) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeString(callerPackage);
                    _data.writeString(alias);
                    _data.writeString(packageName);
                    _data.writeBoolean(hasGrant);
                    this.mRemote.transact(319, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public ParcelableGranteeMap getKeyPairGrants(String callerPackage, String alias) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callerPackage);
                    _data.writeString(alias);
                    this.mRemote.transact(320, _data, _reply, 0);
                    _reply.readException();
                    ParcelableGranteeMap _result = (ParcelableGranteeMap) _reply.readTypedObject(ParcelableGranteeMap.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean setKeyGrantToWifiAuth(String callerPackage, String alias, boolean hasGrant) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callerPackage);
                    _data.writeString(alias);
                    _data.writeBoolean(hasGrant);
                    this.mRemote.transact(321, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isKeyPairGrantedToWifiAuth(String callerPackage, String alias) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callerPackage);
                    _data.writeString(alias);
                    this.mRemote.transact(322, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setUserControlDisabledPackages(ComponentName admin, List<String> packages) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeStringList(packages);
                    this.mRemote.transact(323, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<String> getUserControlDisabledPackages(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(324, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setCommonCriteriaModeEnabled(ComponentName admin, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(325, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isCommonCriteriaModeEnabled(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(326, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getPersonalAppsSuspendedReasons(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(327, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setPersonalAppsSuspended(ComponentName admin, boolean suspended) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeBoolean(suspended);
                    this.mRemote.transact(328, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public long getManagedProfileMaximumTimeOff(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(329, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setManagedProfileMaximumTimeOff(ComponentName admin, long timeoutMs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeLong(timeoutMs);
                    this.mRemote.transact(330, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void acknowledgeDeviceCompliant() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(331, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isComplianceAcknowledgementRequired() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(332, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean canProfileOwnerResetPasswordWhenLocked(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(333, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setNextOperationSafety(int operation, int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(operation);
                    _data.writeInt(reason);
                    this.mRemote.transact(334, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isSafeOperation(int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(reason);
                    this.mRemote.transact(335, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public String getEnrollmentSpecificId(String callerPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callerPackage);
                    this.mRemote.transact(336, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setOrganizationIdForUser(String callerPackage, String enterpriseId, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callerPackage);
                    _data.writeString(enterpriseId);
                    _data.writeInt(userId);
                    this.mRemote.transact(337, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public UserHandle createAndProvisionManagedProfile(ManagedProfileProvisioningParams provisioningParams, String callerPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(provisioningParams, 0);
                    _data.writeString(callerPackage);
                    this.mRemote.transact(338, _data, _reply, 0);
                    _reply.readException();
                    UserHandle _result = (UserHandle) _reply.readTypedObject(UserHandle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void provisionFullyManagedDevice(FullyManagedDeviceProvisioningParams provisioningParams, String callerPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(provisioningParams, 0);
                    _data.writeString(callerPackage);
                    this.mRemote.transact(339, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void finalizeWorkProfileProvisioning(UserHandle managedProfileUser, Account migratedAccount) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(managedProfileUser, 0);
                    _data.writeTypedObject(migratedAccount, 0);
                    this.mRemote.transact(340, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setDeviceOwnerType(ComponentName admin, int deviceOwnerType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    _data.writeInt(deviceOwnerType);
                    this.mRemote.transact(341, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getDeviceOwnerType(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(admin, 0);
                    this.mRemote.transact(342, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void resetDefaultCrossProfileIntentFilters(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(343, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean canAdminGrantSensorsPermissionsForUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(344, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setUsbDataSignalingEnabled(String callerPackage, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callerPackage);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(345, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isUsbDataSignalingEnabled(String callerPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callerPackage);
                    this.mRemote.transact(346, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isUsbDataSignalingEnabledForUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(347, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean canUsbDataSignalingBeDisabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(348, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setMinimumRequiredWifiSecurityLevel(int level) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(level);
                    this.mRemote.transact(349, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public int getMinimumRequiredWifiSecurityLevel() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(350, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setWifiSsidPolicy(WifiSsidPolicy policy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(policy, 0);
                    this.mRemote.transact(351, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public WifiSsidPolicy getWifiSsidPolicy() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(352, _data, _reply, 0);
                    _reply.readException();
                    WifiSsidPolicy _result = (WifiSsidPolicy) _reply.readTypedObject(WifiSsidPolicy.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<UserHandle> listForegroundAffiliatedUsers() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(353, _data, _reply, 0);
                    _reply.readException();
                    List<UserHandle> _result = _reply.createTypedArrayList(UserHandle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setDrawables(List<DevicePolicyDrawableResource> drawables) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(drawables);
                    this.mRemote.transact(354, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void resetDrawables(List<String> drawableIds) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringList(drawableIds);
                    this.mRemote.transact(355, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public ParcelableResource getDrawable(String drawableId, String drawableStyle, String drawableSource) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(drawableId);
                    _data.writeString(drawableStyle);
                    _data.writeString(drawableSource);
                    this.mRemote.transact(356, _data, _reply, 0);
                    _reply.readException();
                    ParcelableResource _result = (ParcelableResource) _reply.readTypedObject(ParcelableResource.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean isDpcDownloaded() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(357, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setDpcDownloaded(boolean downloaded) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(downloaded);
                    this.mRemote.transact(358, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setStrings(List<DevicePolicyStringResource> strings) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(strings);
                    this.mRemote.transact(359, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void resetStrings(List<String> stringIds) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringList(stringIds);
                    this.mRemote.transact(360, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public ParcelableResource getString(String stringId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(stringId);
                    this.mRemote.transact(361, _data, _reply, 0);
                    _reply.readException();
                    ParcelableResource _result = (ParcelableResource) _reply.readTypedObject(ParcelableResource.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public boolean shouldAllowBypassingDevicePolicyManagementRoleQualification() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(362, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public List<UserHandle> getPolicyManagedProfiles(UserHandle userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(userHandle, 0);
                    this.mRemote.transact(363, _data, _reply, 0);
                    _reply.readException();
                    List<UserHandle> _result = _reply.createTypedArrayList(UserHandle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.admin.IDevicePolicyManager
            public void setDualProfileEnabled(ComponentName who, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(who, 0);
                    _data.writeInt(userId);
                    this.mRemote.transact(364, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        private boolean onTransact$setPasswordMinimumNumeric$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            setPasswordMinimumNumeric(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getPasswordMinimumNumeric$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            int _result = getPasswordMinimumNumeric(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeInt(_result);
            return true;
        }

        private boolean onTransact$setPasswordMinimumSymbols$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            setPasswordMinimumSymbols(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getPasswordMinimumSymbols$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            int _result = getPasswordMinimumSymbols(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeInt(_result);
            return true;
        }

        private boolean onTransact$setPasswordMinimumNonLetter$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            setPasswordMinimumNonLetter(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getPasswordMinimumNonLetter$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            int _result = getPasswordMinimumNonLetter(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeInt(_result);
            return true;
        }

        private boolean onTransact$setPasswordHistoryLength$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            setPasswordHistoryLength(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getPasswordHistoryLength$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            int _result = getPasswordHistoryLength(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeInt(_result);
            return true;
        }

        private boolean onTransact$setPasswordExpirationTimeout$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            long _arg1 = data.readLong();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            setPasswordExpirationTimeout(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getPasswordExpirationTimeout$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            long _result = getPasswordExpirationTimeout(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeLong(_result);
            return true;
        }

        private boolean onTransact$getPasswordExpiration$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            long _result = getPasswordExpiration(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeLong(_result);
            return true;
        }

        private boolean onTransact$setMaximumFailedPasswordsForWipe$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            setMaximumFailedPasswordsForWipe(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getMaximumFailedPasswordsForWipe$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            int _result = getMaximumFailedPasswordsForWipe(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeInt(_result);
            return true;
        }

        private boolean onTransact$setMaximumTimeToLock$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            long _arg1 = data.readLong();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            setMaximumTimeToLock(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getMaximumTimeToLock$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            long _result = getMaximumTimeToLock(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeLong(_result);
            return true;
        }

        private boolean onTransact$setRequiredStrongAuthTimeout$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            long _arg1 = data.readLong();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            setRequiredStrongAuthTimeout(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getRequiredStrongAuthTimeout$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            long _result = getRequiredStrongAuthTimeout(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeLong(_result);
            return true;
        }

        private boolean onTransact$wipeDataWithReason$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            String _arg1 = data.readString();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            wipeDataWithReason(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$setGlobalProxy$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            ComponentName _result = setGlobalProxy(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeTypedObject(_result, 1);
            return true;
        }

        private boolean onTransact$setCameraDisabled$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            boolean _arg1 = data.readBoolean();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            setCameraDisabled(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getCameraDisabled$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            boolean _result = getCameraDisabled(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$setScreenCaptureDisabled$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            boolean _arg1 = data.readBoolean();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            setScreenCaptureDisabled(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getScreenCaptureDisabled$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            boolean _result = getScreenCaptureDisabled(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$setKeyguardDisabledFeatures$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            setKeyguardDisabledFeatures(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getKeyguardDisabledFeatures$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            int _result = getKeyguardDisabledFeatures(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeInt(_result);
            return true;
        }

        private boolean onTransact$setActiveAdmin$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            boolean _arg1 = data.readBoolean();
            int _arg2 = data.readInt();
            data.enforceNoDataAvail();
            setActiveAdmin(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getRemoveWarning$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            RemoteCallback _arg1 = (RemoteCallback) data.readTypedObject(RemoteCallback.CREATOR);
            int _arg2 = data.readInt();
            data.enforceNoDataAvail();
            getRemoveWarning(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$hasGrantedPolicy$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            int _arg2 = data.readInt();
            data.enforceNoDataAvail();
            boolean _result = hasGrantedPolicy(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$setDeviceOwner$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            int _arg2 = data.readInt();
            boolean _arg3 = data.readBoolean();
            data.enforceNoDataAvail();
            boolean _result = setDeviceOwner(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$setProfileOwner$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            int _arg2 = data.readInt();
            data.enforceNoDataAvail();
            boolean _result = setProfileOwner(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$checkDeviceIdentifierAccess$(Parcel data, Parcel reply) throws RemoteException {
            String _arg0 = data.readString();
            int _arg1 = data.readInt();
            int _arg2 = data.readInt();
            data.enforceNoDataAvail();
            boolean _result = checkDeviceIdentifierAccess(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$setPackagesSuspended$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            String[] _arg2 = data.createStringArray();
            boolean _arg3 = data.readBoolean();
            data.enforceNoDataAvail();
            String[] _result = setPackagesSuspended(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            reply.writeStringArray(_result);
            return true;
        }

        private boolean onTransact$isPackageSuspended$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            boolean _result = isPackageSuspended(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$installCaCert$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            byte[] _arg2 = data.createByteArray();
            data.enforceNoDataAvail();
            boolean _result = installCaCert(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$uninstallCaCerts$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            String[] _arg2 = data.createStringArray();
            data.enforceNoDataAvail();
            uninstallCaCerts(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$approveCaCert$(Parcel data, Parcel reply) throws RemoteException {
            String _arg0 = data.readString();
            int _arg1 = data.readInt();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            boolean _result = approveCaCert(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$installKeyPair$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            byte[] _arg2 = data.createByteArray();
            byte[] _arg3 = data.createByteArray();
            byte[] _arg4 = data.createByteArray();
            String _arg5 = data.readString();
            boolean _arg6 = data.readBoolean();
            boolean _arg7 = data.readBoolean();
            data.enforceNoDataAvail();
            boolean _result = installKeyPair(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$removeKeyPair$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            boolean _result = removeKeyPair(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$generateKeyPair$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            ParcelableKeyGenParameterSpec _arg3 = (ParcelableKeyGenParameterSpec) data.readTypedObject(ParcelableKeyGenParameterSpec.CREATOR);
            int _arg4 = data.readInt();
            KeymasterCertificateChain _arg5 = new KeymasterCertificateChain();
            data.enforceNoDataAvail();
            boolean _result = generateKeyPair(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5);
            reply.writeNoException();
            reply.writeBoolean(_result);
            reply.writeTypedObject(_arg5, 1);
            return true;
        }

        private boolean onTransact$setKeyPairCertificate$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            byte[] _arg3 = data.createByteArray();
            byte[] _arg4 = data.createByteArray();
            boolean _arg5 = data.readBoolean();
            data.enforceNoDataAvail();
            boolean _result = setKeyPairCertificate(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$choosePrivateKeyAlias$(Parcel data, Parcel reply) throws RemoteException {
            int _arg0 = data.readInt();
            Uri _arg1 = (Uri) data.readTypedObject(Uri.CREATOR);
            String _arg2 = data.readString();
            IBinder _arg3 = data.readStrongBinder();
            data.enforceNoDataAvail();
            choosePrivateKeyAlias(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$setDelegatedScopes$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            List<String> _arg2 = data.createStringArrayList();
            data.enforceNoDataAvail();
            setDelegatedScopes(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$setAlwaysOnVpnPackage$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            boolean _arg2 = data.readBoolean();
            List<String> _arg3 = data.createStringArrayList();
            data.enforceNoDataAvail();
            boolean _result = setAlwaysOnVpnPackage(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$addPersistentPreferredActivity$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            IntentFilter _arg1 = (IntentFilter) data.readTypedObject(IntentFilter.CREATOR);
            ComponentName _arg2 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            data.enforceNoDataAvail();
            addPersistentPreferredActivity(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$setDefaultSmsApplication$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            setDefaultSmsApplication(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$setApplicationRestrictions$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            Bundle _arg3 = (Bundle) data.readTypedObject(Bundle.CREATOR);
            data.enforceNoDataAvail();
            setApplicationRestrictions(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getApplicationRestrictions$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            Bundle _result = getApplicationRestrictions(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeTypedObject(_result, 1);
            return true;
        }

        private boolean onTransact$setUserRestriction$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            boolean _arg2 = data.readBoolean();
            boolean _arg3 = data.readBoolean();
            data.enforceNoDataAvail();
            setUserRestriction(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$addCrossProfileIntentFilter$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            IntentFilter _arg1 = (IntentFilter) data.readTypedObject(IntentFilter.CREATOR);
            int _arg2 = data.readInt();
            data.enforceNoDataAvail();
            addCrossProfileIntentFilter(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$isAccessibilityServicePermittedByAdmin$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            int _arg2 = data.readInt();
            data.enforceNoDataAvail();
            boolean _result = isAccessibilityServicePermittedByAdmin(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$setPermittedInputMethods$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            List<String> _arg1 = data.createStringArrayList();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            boolean _result = setPermittedInputMethods(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$isInputMethodPermittedByAdmin$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            int _arg2 = data.readInt();
            boolean _arg3 = data.readBoolean();
            data.enforceNoDataAvail();
            boolean _result = isInputMethodPermittedByAdmin(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$setApplicationHidden$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            boolean _arg3 = data.readBoolean();
            boolean _arg4 = data.readBoolean();
            data.enforceNoDataAvail();
            boolean _result = setApplicationHidden(_arg0, _arg1, _arg2, _arg3, _arg4);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$isApplicationHidden$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            boolean _arg3 = data.readBoolean();
            data.enforceNoDataAvail();
            boolean _result = isApplicationHidden(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$createAndManageUser$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            ComponentName _arg2 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            PersistableBundle _arg3 = (PersistableBundle) data.readTypedObject(PersistableBundle.CREATOR);
            int _arg4 = data.readInt();
            data.enforceNoDataAvail();
            UserHandle _result = createAndManageUser(_arg0, _arg1, _arg2, _arg3, _arg4);
            reply.writeNoException();
            reply.writeTypedObject(_result, 1);
            return true;
        }

        private boolean onTransact$enableSystemApp$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            enableSystemApp(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$enableSystemAppWithIntent$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            Intent _arg2 = (Intent) data.readTypedObject(Intent.CREATOR);
            data.enforceNoDataAvail();
            int _result = enableSystemAppWithIntent(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeInt(_result);
            return true;
        }

        private boolean onTransact$installExistingPackage$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            boolean _result = installExistingPackage(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$setAccountManagementDisabled$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            boolean _arg2 = data.readBoolean();
            boolean _arg3 = data.readBoolean();
            data.enforceNoDataAvail();
            setAccountManagementDisabled(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$setGlobalSetting$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            setGlobalSetting(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$setSystemSetting$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            setSystemSetting(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$setSecureSetting$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            setSecureSetting(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$notifyLockTaskModeChanged$(Parcel data, Parcel reply) throws RemoteException {
            boolean _arg0 = data.readBoolean();
            String _arg1 = data.readString();
            int _arg2 = data.readInt();
            data.enforceNoDataAvail();
            notifyLockTaskModeChanged(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$setUninstallBlocked$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            boolean _arg3 = data.readBoolean();
            data.enforceNoDataAvail();
            setUninstallBlocked(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$startManagedQuickContact$(Parcel data, Parcel reply) throws RemoteException {
            String _arg0 = data.readString();
            long _arg1 = data.readLong();
            boolean _arg2 = data.readBoolean();
            long _arg3 = data.readLong();
            Intent _arg4 = (Intent) data.readTypedObject(Intent.CREATOR);
            data.enforceNoDataAvail();
            startManagedQuickContact(_arg0, _arg1, _arg2, _arg3, _arg4);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$setTrustAgentConfiguration$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            ComponentName _arg1 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            PersistableBundle _arg2 = (PersistableBundle) data.readTypedObject(PersistableBundle.CREATOR);
            boolean _arg3 = data.readBoolean();
            data.enforceNoDataAvail();
            setTrustAgentConfiguration(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getTrustAgentConfiguration$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            ComponentName _arg1 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg2 = data.readInt();
            boolean _arg3 = data.readBoolean();
            data.enforceNoDataAvail();
            List<PersistableBundle> _result = getTrustAgentConfiguration(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            reply.writeTypedList(_result);
            return true;
        }

        private boolean onTransact$setPermissionPolicy$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            int _arg2 = data.readInt();
            data.enforceNoDataAvail();
            setPermissionPolicy(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$setPermissionGrantState$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            String _arg3 = data.readString();
            int _arg4 = data.readInt();
            RemoteCallback _arg5 = (RemoteCallback) data.readTypedObject(RemoteCallback.CREATOR);
            data.enforceNoDataAvail();
            setPermissionGrantState(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getPermissionGrantState$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            String _arg3 = data.readString();
            data.enforceNoDataAvail();
            int _result = getPermissionGrantState(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            reply.writeInt(_result);
            return true;
        }

        private boolean onTransact$setKeepUninstalledPackages$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            List<String> _arg2 = data.createStringArrayList();
            data.enforceNoDataAvail();
            setKeepUninstalledPackages(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$setSecurityLoggingEnabled$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            setSecurityLoggingEnabled(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$setNetworkLoggingEnabled$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            setNetworkLoggingEnabled(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$retrieveNetworkLogs$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            long _arg2 = data.readLong();
            data.enforceNoDataAvail();
            List<NetworkEvent> _result = retrieveNetworkLogs(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeTypedList(_result);
            return true;
        }

        private boolean onTransact$bindDeviceAdminServiceAsUser$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            IApplicationThread _arg1 = IApplicationThread.Stub.asInterface(data.readStrongBinder());
            IBinder _arg2 = data.readStrongBinder();
            Intent _arg3 = (Intent) data.readTypedObject(Intent.CREATOR);
            IServiceConnection _arg4 = IServiceConnection.Stub.asInterface(data.readStrongBinder());
            int _arg5 = data.readInt();
            int _arg6 = data.readInt();
            data.enforceNoDataAvail();
            boolean _result = bindDeviceAdminServiceAsUser(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$resetPasswordWithToken$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            byte[] _arg2 = data.createByteArray();
            int _arg3 = data.readInt();
            data.enforceNoDataAvail();
            boolean _result = resetPasswordWithToken(_arg0, _arg1, _arg2, _arg3);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$clearApplicationUserData$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            IPackageDataObserver _arg2 = IPackageDataObserver.Stub.asInterface(data.readStrongBinder());
            data.enforceNoDataAvail();
            clearApplicationUserData(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getDisallowedSystemApps$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            List<String> _result = getDisallowedSystemApps(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeStringList(_result);
            return true;
        }

        private boolean onTransact$transferOwnership$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            ComponentName _arg1 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            PersistableBundle _arg2 = (PersistableBundle) data.readTypedObject(PersistableBundle.CREATOR);
            data.enforceNoDataAvail();
            transferOwnership(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$updateOverrideApn$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            ApnSetting _arg2 = (ApnSetting) data.readTypedObject(ApnSetting.CREATOR);
            data.enforceNoDataAvail();
            boolean _result = updateOverrideApn(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$isMeteredDataDisabledPackageForUser$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            int _arg2 = data.readInt();
            data.enforceNoDataAvail();
            boolean _result = isMeteredDataDisabledPackageForUser(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$setGlobalPrivateDns$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            int _result = setGlobalPrivateDns(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeInt(_result);
            return true;
        }

        private boolean onTransact$setProfileOwnerOnOrganizationOwnedDevice$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            int _arg1 = data.readInt();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            setProfileOwnerOnOrganizationOwnedDevice(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$installUpdateFromFile$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            ParcelFileDescriptor _arg1 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
            StartInstallingUpdateCallback _arg2 = StartInstallingUpdateCallback.Stub.asInterface(data.readStrongBinder());
            data.enforceNoDataAvail();
            installUpdateFromFile(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$startViewCalendarEventInManagedProfile$(Parcel data, Parcel reply) throws RemoteException {
            String _arg0 = data.readString();
            long _arg1 = data.readLong();
            long _arg2 = data.readLong();
            long _arg3 = data.readLong();
            boolean _arg4 = data.readBoolean();
            int _arg5 = data.readInt();
            data.enforceNoDataAvail();
            boolean _result = startViewCalendarEventInManagedProfile(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$setKeyGrantForApp$(Parcel data, Parcel reply) throws RemoteException {
            ComponentName _arg0 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            String _arg3 = data.readString();
            boolean _arg4 = data.readBoolean();
            data.enforceNoDataAvail();
            boolean _result = setKeyGrantForApp(_arg0, _arg1, _arg2, _arg3, _arg4);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$setKeyGrantToWifiAuth$(Parcel data, Parcel reply) throws RemoteException {
            String _arg0 = data.readString();
            String _arg1 = data.readString();
            boolean _arg2 = data.readBoolean();
            data.enforceNoDataAvail();
            boolean _result = setKeyGrantToWifiAuth(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeBoolean(_result);
            return true;
        }

        private boolean onTransact$setOrganizationIdForUser$(Parcel data, Parcel reply) throws RemoteException {
            String _arg0 = data.readString();
            String _arg1 = data.readString();
            int _arg2 = data.readInt();
            data.enforceNoDataAvail();
            setOrganizationIdForUser(_arg0, _arg1, _arg2);
            reply.writeNoException();
            return true;
        }

        private boolean onTransact$getDrawable$(Parcel data, Parcel reply) throws RemoteException {
            String _arg0 = data.readString();
            String _arg1 = data.readString();
            String _arg2 = data.readString();
            data.enforceNoDataAvail();
            ParcelableResource _result = getDrawable(_arg0, _arg1, _arg2);
            reply.writeNoException();
            reply.writeTypedObject(_result, 1);
            return true;
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 363;
        }
    }
}
