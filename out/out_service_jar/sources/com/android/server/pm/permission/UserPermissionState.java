package com.android.server.pm.permission;

import android.os.UserHandle;
import android.util.ArraySet;
import android.util.SparseArray;
/* loaded from: classes2.dex */
public final class UserPermissionState {
    private final ArraySet<String> mInstallPermissionsFixed = new ArraySet<>();
    private final SparseArray<UidPermissionState> mUidStates = new SparseArray<>();

    public boolean areInstallPermissionsFixed(String packageName) {
        return this.mInstallPermissionsFixed.contains(packageName);
    }

    public void setInstallPermissionsFixed(String packageName, boolean fixed) {
        if (fixed) {
            this.mInstallPermissionsFixed.add(packageName);
        } else {
            this.mInstallPermissionsFixed.remove(packageName);
        }
    }

    public UidPermissionState getUidState(int appId) {
        checkAppId(appId);
        return this.mUidStates.get(appId);
    }

    public UidPermissionState getOrCreateUidState(int appId) {
        checkAppId(appId);
        UidPermissionState uidState = this.mUidStates.get(appId);
        if (uidState == null) {
            UidPermissionState uidState2 = new UidPermissionState();
            this.mUidStates.put(appId, uidState2);
            return uidState2;
        }
        return uidState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UidPermissionState createUidStateWithExisting(int appId, UidPermissionState other) {
        checkAppId(appId);
        UidPermissionState uidState = new UidPermissionState(other);
        this.mUidStates.put(appId, uidState);
        return uidState;
    }

    public void removeUidState(int appId) {
        checkAppId(appId);
        this.mUidStates.delete(appId);
    }

    private void checkAppId(int appId) {
        if (UserHandle.getUserId(appId) != 0) {
            throw new IllegalArgumentException("Invalid app ID " + appId);
        }
    }
}
