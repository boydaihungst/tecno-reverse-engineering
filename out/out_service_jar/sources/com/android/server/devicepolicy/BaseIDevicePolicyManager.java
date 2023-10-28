package com.android.server.devicepolicy;

import android.accounts.Account;
import android.app.admin.DevicePolicyDrawableResource;
import android.app.admin.DevicePolicySafetyChecker;
import android.app.admin.DevicePolicyStringResource;
import android.app.admin.FullyManagedDeviceProvisioningParams;
import android.app.admin.IDevicePolicyManager;
import android.app.admin.ManagedProfileProvisioningParams;
import android.app.admin.ParcelableResource;
import android.content.ComponentName;
import android.os.UserHandle;
import android.util.Slog;
import java.util.Collections;
import java.util.List;
/* loaded from: classes.dex */
abstract class BaseIDevicePolicyManager extends IDevicePolicyManager.Stub {
    private static final String TAG = BaseIDevicePolicyManager.class.getSimpleName();

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void handleOnUserUnlocked(int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void handleStartUser(int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void handleStopUser(int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void handleUnlockUser(int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void systemReady(int i);

    public void setDevicePolicySafetyChecker(DevicePolicySafetyChecker safetyChecker) {
        Slog.w(TAG, "setDevicePolicySafetyChecker() not implemented by " + getClass());
    }

    public void clearSystemUpdatePolicyFreezePeriodRecord() {
    }

    public boolean setKeyGrantForApp(ComponentName admin, String callerPackage, String alias, String packageName, boolean hasGrant) {
        return false;
    }

    public void setLocationEnabled(ComponentName who, boolean locationEnabled) {
    }

    public boolean isOrganizationOwnedDeviceWithManagedProfile() {
        return false;
    }

    public int getPersonalAppsSuspendedReasons(ComponentName admin) {
        return 0;
    }

    public void setPersonalAppsSuspended(ComponentName admin, boolean suspended) {
    }

    public void setManagedProfileMaximumTimeOff(ComponentName admin, long timeoutMs) {
    }

    public long getManagedProfileMaximumTimeOff(ComponentName admin) {
        return 0L;
    }

    public void acknowledgeDeviceCompliant() {
    }

    public boolean isComplianceAcknowledgementRequired() {
        return false;
    }

    public boolean canProfileOwnerResetPasswordWhenLocked(int userId) {
        return false;
    }

    public String getEnrollmentSpecificId(String callerPackage) {
        return "";
    }

    public void setOrganizationIdForUser(String callerPackage, String enterpriseId, int userId) {
    }

    public UserHandle createAndProvisionManagedProfile(ManagedProfileProvisioningParams provisioningParams, String callerPackage) {
        return null;
    }

    public void finalizeWorkProfileProvisioning(UserHandle managedProfileUser, Account migratedAccount) {
    }

    public void provisionFullyManagedDevice(FullyManagedDeviceProvisioningParams provisioningParams, String callerPackage) {
    }

    public void setDeviceOwnerType(ComponentName admin, int deviceOwnerType) {
    }

    public int getDeviceOwnerType(ComponentName admin) {
        return 0;
    }

    public void resetDefaultCrossProfileIntentFilters(int userId) {
    }

    public boolean canAdminGrantSensorsPermissionsForUser(int userId) {
        return false;
    }

    public boolean setKeyGrantToWifiAuth(String callerPackage, String alias, boolean hasGrant) {
        return false;
    }

    public boolean isKeyPairGrantedToWifiAuth(String callerPackage, String alias) {
        return false;
    }

    public void setDrawables(List<DevicePolicyDrawableResource> drawables) {
    }

    public void resetDrawables(List<String> drawableIds) {
    }

    public ParcelableResource getDrawable(String drawableId, String drawableStyle, String drawableSource) {
        return null;
    }

    public void setStrings(List<DevicePolicyStringResource> strings) {
    }

    public void resetStrings(List<String> stringIds) {
    }

    public ParcelableResource getString(String stringId) {
        return null;
    }

    public boolean shouldAllowBypassingDevicePolicyManagementRoleQualification() {
        return false;
    }

    public List<UserHandle> getPolicyManagedProfiles(UserHandle userHandle) {
        return Collections.emptyList();
    }
}
