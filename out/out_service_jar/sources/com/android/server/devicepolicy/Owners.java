package com.android.server.devicepolicy;

import android.app.ActivityManagerInternal;
import android.app.AppOpsManagerInternal;
import android.app.admin.SystemUpdateInfo;
import android.app.admin.SystemUpdatePolicy;
import android.content.ComponentName;
import android.content.pm.PackageManagerInternal;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.UserManager;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.server.LocalServices;
import com.android.server.devicepolicy.OwnersData;
import com.android.server.pm.UserManagerInternal;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToIntFunction;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class Owners {
    private static final boolean DEBUG = false;
    private static final String TAG = "DevicePolicyManagerService";
    private final ActivityManagerInternal mActivityManagerInternal;
    private final ActivityTaskManagerInternal mActivityTaskManagerInternal;
    private final OwnersData mData;
    private final PackageManagerInternal mPackageManagerInternal;
    private boolean mSystemReady;
    private final UserManager mUserManager;
    private final UserManagerInternal mUserManagerInternal;

    /* JADX INFO: Access modifiers changed from: package-private */
    public Owners(UserManager userManager, UserManagerInternal userManagerInternal, PackageManagerInternal packageManagerInternal, ActivityTaskManagerInternal activityTaskManagerInternal, ActivityManagerInternal activityManagerInternal, PolicyPathProvider pathProvider) {
        this.mUserManager = userManager;
        this.mUserManagerInternal = userManagerInternal;
        this.mPackageManagerInternal = packageManagerInternal;
        this.mActivityTaskManagerInternal = activityTaskManagerInternal;
        this.mActivityManagerInternal = activityManagerInternal;
        this.mData = new OwnersData(pathProvider);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void load() {
        synchronized (this.mData) {
            int[] usersIds = this.mUserManager.getAliveUsers().stream().mapToInt(new ToIntFunction() { // from class: com.android.server.devicepolicy.Owners$$ExternalSyntheticLambda0
                @Override // java.util.function.ToIntFunction
                public final int applyAsInt(Object obj) {
                    int i;
                    i = ((UserInfo) obj).id;
                    return i;
                }
            }).toArray();
            this.mData.load(usersIds);
            this.mUserManagerInternal.setDeviceManaged(hasDeviceOwner());
            for (int userId : usersIds) {
                this.mUserManagerInternal.setUserManaged(userId, hasProfileOwner(userId));
            }
            notifyChangeLocked();
            pushToActivityTaskManagerLocked();
        }
    }

    private void notifyChangeLocked() {
        pushToDevicePolicyManager();
        pushToPackageManagerLocked();
        pushToActivityManagerLocked();
        pushToAppOpsLocked();
    }

    private void pushToDevicePolicyManager() {
        DevicePolicyManagerService.invalidateBinderCaches();
    }

    private void pushToPackageManagerLocked() {
        SparseArray<String> po = new SparseArray<>();
        for (int i = this.mData.mProfileOwners.size() - 1; i >= 0; i--) {
            po.put(this.mData.mProfileOwners.keyAt(i).intValue(), this.mData.mProfileOwners.valueAt(i).packageName);
        }
        String doPackage = this.mData.mDeviceOwner != null ? this.mData.mDeviceOwner.packageName : null;
        this.mPackageManagerInternal.setDeviceAndProfileOwnerPackages(this.mData.mDeviceOwnerUserId, doPackage, po);
    }

    private void pushToActivityTaskManagerLocked() {
        this.mActivityTaskManagerInternal.setDeviceOwnerUid(getDeviceOwnerUidLocked());
    }

    private void pushToActivityManagerLocked() {
        this.mActivityManagerInternal.setDeviceOwnerUid(getDeviceOwnerUidLocked());
        ArraySet<Integer> profileOwners = new ArraySet<>();
        for (int poi = this.mData.mProfileOwners.size() - 1; poi >= 0; poi--) {
            int userId = this.mData.mProfileOwners.keyAt(poi).intValue();
            int profileOwnerUid = this.mPackageManagerInternal.getPackageUid(this.mData.mProfileOwners.valueAt(poi).packageName, 4333568L, userId);
            if (profileOwnerUid >= 0) {
                profileOwners.add(Integer.valueOf(profileOwnerUid));
            }
        }
        this.mActivityManagerInternal.setProfileOwnerUid(profileOwners);
    }

    int getDeviceOwnerUidLocked() {
        if (this.mData.mDeviceOwner != null) {
            return this.mPackageManagerInternal.getPackageUid(this.mData.mDeviceOwner.packageName, 4333568L, this.mData.mDeviceOwnerUserId);
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getDeviceOwnerPackageName() {
        String str;
        synchronized (this.mData) {
            str = this.mData.mDeviceOwner != null ? this.mData.mDeviceOwner.packageName : null;
        }
        return str;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getDeviceOwnerUserId() {
        int i;
        synchronized (this.mData) {
            i = this.mData.mDeviceOwnerUserId;
        }
        return i;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Pair<Integer, ComponentName> getDeviceOwnerUserIdAndComponent() {
        synchronized (this.mData) {
            if (this.mData.mDeviceOwner == null) {
                return null;
            }
            return Pair.create(Integer.valueOf(this.mData.mDeviceOwnerUserId), this.mData.mDeviceOwner.admin);
        }
    }

    String getDeviceOwnerName() {
        String str;
        synchronized (this.mData) {
            str = this.mData.mDeviceOwner != null ? this.mData.mDeviceOwner.name : null;
        }
        return str;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComponentName getDeviceOwnerComponent() {
        ComponentName componentName;
        synchronized (this.mData) {
            componentName = this.mData.mDeviceOwner != null ? this.mData.mDeviceOwner.admin : null;
        }
        return componentName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getDeviceOwnerRemoteBugreportUri() {
        String str;
        synchronized (this.mData) {
            str = this.mData.mDeviceOwner != null ? this.mData.mDeviceOwner.remoteBugreportUri : null;
        }
        return str;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getDeviceOwnerRemoteBugreportHash() {
        String str;
        synchronized (this.mData) {
            str = this.mData.mDeviceOwner != null ? this.mData.mDeviceOwner.remoteBugreportHash : null;
        }
        return str;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDeviceOwner(ComponentName admin, String ownerName, int userId) {
        if (userId < 0) {
            Slog.e(TAG, "Invalid user id for device owner user: " + userId);
            return;
        }
        synchronized (this.mData) {
            this.mData.mDeviceOwner = new OwnersData.OwnerInfo(ownerName, admin, null, null, true);
            this.mData.mDeviceOwnerUserId = userId;
            this.mUserManagerInternal.setDeviceManaged(true);
            notifyChangeLocked();
            pushToActivityTaskManagerLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearDeviceOwner() {
        synchronized (this.mData) {
            this.mData.mDeviceOwnerTypes.remove(this.mData.mDeviceOwner.packageName);
            this.mData.mDeviceOwner = null;
            this.mData.mDeviceOwnerUserId = -10000;
            this.mUserManagerInternal.setDeviceManaged(false);
            notifyChangeLocked();
            pushToActivityTaskManagerLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setProfileOwner(ComponentName admin, String ownerName, int userId) {
        synchronized (this.mData) {
            this.mData.mProfileOwners.put(Integer.valueOf(userId), new OwnersData.OwnerInfo(ownerName, admin, null, null, false));
            this.mUserManagerInternal.setUserManaged(userId, true);
            notifyChangeLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeProfileOwner(int userId) {
        synchronized (this.mData) {
            this.mData.mProfileOwners.remove(Integer.valueOf(userId));
            this.mUserManagerInternal.setUserManaged(userId, false);
            notifyChangeLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void transferProfileOwner(ComponentName target, int userId) {
        synchronized (this.mData) {
            OwnersData.OwnerInfo ownerInfo = this.mData.mProfileOwners.get(Integer.valueOf(userId));
            OwnersData.OwnerInfo newOwnerInfo = new OwnersData.OwnerInfo(target.getPackageName(), target, ownerInfo.remoteBugreportUri, ownerInfo.remoteBugreportHash, ownerInfo.isOrganizationOwnedDevice);
            this.mData.mProfileOwners.put(Integer.valueOf(userId), newOwnerInfo);
            notifyChangeLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void transferDeviceOwnership(ComponentName target) {
        synchronized (this.mData) {
            Integer previousDeviceOwnerType = this.mData.mDeviceOwnerTypes.remove(this.mData.mDeviceOwner.packageName);
            OwnersData ownersData = this.mData;
            ownersData.mDeviceOwner = new OwnersData.OwnerInfo(null, target, ownersData.mDeviceOwner.remoteBugreportUri, this.mData.mDeviceOwner.remoteBugreportHash, this.mData.mDeviceOwner.isOrganizationOwnedDevice);
            if (previousDeviceOwnerType != null) {
                this.mData.mDeviceOwnerTypes.put(this.mData.mDeviceOwner.packageName, previousDeviceOwnerType);
            }
            notifyChangeLocked();
            pushToActivityTaskManagerLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComponentName getProfileOwnerComponent(int userId) {
        ComponentName componentName;
        synchronized (this.mData) {
            OwnersData.OwnerInfo profileOwner = this.mData.mProfileOwners.get(Integer.valueOf(userId));
            componentName = profileOwner != null ? profileOwner.admin : null;
        }
        return componentName;
    }

    String getProfileOwnerName(int userId) {
        String str;
        synchronized (this.mData) {
            OwnersData.OwnerInfo profileOwner = this.mData.mProfileOwners.get(Integer.valueOf(userId));
            str = profileOwner != null ? profileOwner.name : null;
        }
        return str;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getProfileOwnerPackage(int userId) {
        String str;
        synchronized (this.mData) {
            OwnersData.OwnerInfo profileOwner = this.mData.mProfileOwners.get(Integer.valueOf(userId));
            str = profileOwner != null ? profileOwner.packageName : null;
        }
        return str;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isProfileOwnerOfOrganizationOwnedDevice(int userId) {
        boolean z;
        synchronized (this.mData) {
            OwnersData.OwnerInfo profileOwner = this.mData.mProfileOwners.get(Integer.valueOf(userId));
            z = profileOwner != null ? profileOwner.isOrganizationOwnedDevice : false;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Set<Integer> getProfileOwnerKeys() {
        Set<Integer> keySet;
        synchronized (this.mData) {
            keySet = this.mData.mProfileOwners.keySet();
        }
        return keySet;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<OwnerShellData> listAllOwners() {
        List<OwnerShellData> owners = new ArrayList<>();
        synchronized (this.mData) {
            if (this.mData.mDeviceOwner != null) {
                owners.add(OwnerShellData.forDeviceOwner(this.mData.mDeviceOwnerUserId, this.mData.mDeviceOwner.admin));
            }
            for (int i = 0; i < this.mData.mProfileOwners.size(); i++) {
                int userId = this.mData.mProfileOwners.keyAt(i).intValue();
                OwnersData.OwnerInfo info = this.mData.mProfileOwners.valueAt(i);
                owners.add(OwnerShellData.forUserProfileOwner(userId, info.admin));
            }
        }
        return owners;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SystemUpdatePolicy getSystemUpdatePolicy() {
        SystemUpdatePolicy systemUpdatePolicy;
        synchronized (this.mData) {
            systemUpdatePolicy = this.mData.mSystemUpdatePolicy;
        }
        return systemUpdatePolicy;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSystemUpdatePolicy(SystemUpdatePolicy systemUpdatePolicy) {
        synchronized (this.mData) {
            this.mData.mSystemUpdatePolicy = systemUpdatePolicy;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearSystemUpdatePolicy() {
        synchronized (this.mData) {
            this.mData.mSystemUpdatePolicy = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Pair<LocalDate, LocalDate> getSystemUpdateFreezePeriodRecord() {
        Pair<LocalDate, LocalDate> pair;
        synchronized (this.mData) {
            pair = new Pair<>(this.mData.mSystemUpdateFreezeStart, this.mData.mSystemUpdateFreezeEnd);
        }
        return pair;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getSystemUpdateFreezePeriodRecordAsString() {
        String systemUpdateFreezePeriodRecordAsString;
        synchronized (this.mData) {
            systemUpdateFreezePeriodRecordAsString = this.mData.getSystemUpdateFreezePeriodRecordAsString();
        }
        return systemUpdateFreezePeriodRecordAsString;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setSystemUpdateFreezePeriodRecord(LocalDate start, LocalDate end) {
        boolean changed = false;
        synchronized (this.mData) {
            if (!Objects.equals(this.mData.mSystemUpdateFreezeStart, start)) {
                this.mData.mSystemUpdateFreezeStart = start;
                changed = true;
            }
            if (!Objects.equals(this.mData.mSystemUpdateFreezeEnd, end)) {
                this.mData.mSystemUpdateFreezeEnd = end;
                changed = true;
            }
        }
        return changed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasDeviceOwner() {
        boolean z;
        synchronized (this.mData) {
            z = this.mData.mDeviceOwner != null;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDeviceOwnerUserId(int userId) {
        boolean z;
        synchronized (this.mData) {
            z = this.mData.mDeviceOwner != null && this.mData.mDeviceOwnerUserId == userId;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasProfileOwner(int userId) {
        boolean z;
        synchronized (this.mData) {
            z = getProfileOwnerComponent(userId) != null;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDeviceOwnerRemoteBugreportUriAndHash(String remoteBugreportUri, String remoteBugreportHash) {
        synchronized (this.mData) {
            if (this.mData.mDeviceOwner != null) {
                this.mData.mDeviceOwner.remoteBugreportUri = remoteBugreportUri;
                this.mData.mDeviceOwner.remoteBugreportHash = remoteBugreportHash;
            }
            writeDeviceOwner();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setProfileOwnerOfOrganizationOwnedDevice(int userId, boolean isOrganizationOwnedDevice) {
        synchronized (this.mData) {
            OwnersData.OwnerInfo profileOwner = this.mData.mProfileOwners.get(Integer.valueOf(userId));
            if (profileOwner != null) {
                profileOwner.isOrganizationOwnedDevice = isOrganizationOwnedDevice;
            } else {
                Slog.e(TAG, String.format("No profile owner for user %d to set org-owned flag.", Integer.valueOf(userId)));
            }
            writeProfileOwner(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDeviceOwnerType(String packageName, int deviceOwnerType, boolean isAdminTestOnly) {
        synchronized (this.mData) {
            if (!hasDeviceOwner()) {
                Slog.e(TAG, "Attempting to set a device owner type when there is no device owner");
            } else if (!isAdminTestOnly && isDeviceOwnerTypeSetForDeviceOwner(packageName)) {
                Slog.e(TAG, "Setting the device owner type more than once is only allowed for test only admins");
            } else {
                this.mData.mDeviceOwnerTypes.put(packageName, Integer.valueOf(deviceOwnerType));
                writeDeviceOwner();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getDeviceOwnerType(String packageName) {
        synchronized (this.mData) {
            if (isDeviceOwnerTypeSetForDeviceOwner(packageName)) {
                return this.mData.mDeviceOwnerTypes.get(packageName).intValue();
            }
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDeviceOwnerTypeSetForDeviceOwner(String packageName) {
        boolean z;
        synchronized (this.mData) {
            z = !this.mData.mDeviceOwnerTypes.isEmpty() && this.mData.mDeviceOwnerTypes.containsKey(packageName);
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeDeviceOwner() {
        synchronized (this.mData) {
            pushToDevicePolicyManager();
            this.mData.writeDeviceOwner();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeProfileOwner(int userId) {
        synchronized (this.mData) {
            pushToDevicePolicyManager();
            this.mData.writeProfileOwner(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean saveSystemUpdateInfo(SystemUpdateInfo newInfo) {
        synchronized (this.mData) {
            if (Objects.equals(newInfo, this.mData.mSystemUpdateInfo)) {
                return false;
            }
            this.mData.mSystemUpdateInfo = newInfo;
            this.mData.writeDeviceOwner();
            return true;
        }
    }

    public SystemUpdateInfo getSystemUpdateInfo() {
        SystemUpdateInfo systemUpdateInfo;
        synchronized (this.mData) {
            systemUpdateInfo = this.mData.mSystemUpdateInfo;
        }
        return systemUpdateInfo;
    }

    void pushToAppOpsLocked() {
        int uid;
        if (!this.mSystemReady) {
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            SparseIntArray owners = new SparseIntArray();
            if (this.mData.mDeviceOwner != null && (uid = getDeviceOwnerUidLocked()) >= 0) {
                owners.put(this.mData.mDeviceOwnerUserId, uid);
            }
            if (this.mData.mProfileOwners != null) {
                for (int poi = this.mData.mProfileOwners.size() - 1; poi >= 0; poi--) {
                    int uid2 = this.mPackageManagerInternal.getPackageUid(this.mData.mProfileOwners.valueAt(poi).packageName, 4333568L, this.mData.mProfileOwners.keyAt(poi).intValue());
                    if (uid2 >= 0) {
                        owners.put(this.mData.mProfileOwners.keyAt(poi).intValue(), uid2);
                    }
                }
            }
            AppOpsManagerInternal appops = (AppOpsManagerInternal) LocalServices.getService(AppOpsManagerInternal.class);
            if (appops != null) {
                appops.setDeviceAndProfileOwners(owners.size() > 0 ? owners : null);
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void systemReady() {
        synchronized (this.mData) {
            this.mSystemReady = true;
            pushToActivityManagerLocked();
            pushToAppOpsLocked();
        }
    }

    public void dump(IndentingPrintWriter pw) {
        synchronized (this.mData) {
            this.mData.dump(pw);
        }
    }

    File getDeviceOwnerFile() {
        return this.mData.getDeviceOwnerFile();
    }

    File getProfileOwnerFile(int userId) {
        return this.mData.getProfileOwnerFile(userId);
    }
}
