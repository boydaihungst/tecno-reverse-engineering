package com.android.server.pm.permission;

import android.util.SparseArray;
/* loaded from: classes2.dex */
public final class DevicePermissionState {
    private final SparseArray<UserPermissionState> mUserStates = new SparseArray<>();

    public UserPermissionState getUserState(int userId) {
        return this.mUserStates.get(userId);
    }

    public UserPermissionState getOrCreateUserState(int userId) {
        UserPermissionState userState = this.mUserStates.get(userId);
        if (userState == null) {
            UserPermissionState userState2 = new UserPermissionState();
            this.mUserStates.put(userId, userState2);
            return userState2;
        }
        return userState;
    }

    public void removeUserState(int userId) {
        this.mUserStates.delete(userId);
    }

    public int[] getUserIds() {
        int userStatesSize = this.mUserStates.size();
        int[] userIds = new int[userStatesSize];
        for (int i = 0; i < userStatesSize; i++) {
            int userId = this.mUserStates.keyAt(i);
            userIds[i] = userId;
        }
        return userIds;
    }
}
