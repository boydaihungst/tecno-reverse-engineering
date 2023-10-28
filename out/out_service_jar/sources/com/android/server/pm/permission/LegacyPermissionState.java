package com.android.server.pm.permission;

import android.util.ArrayMap;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
/* loaded from: classes2.dex */
public final class LegacyPermissionState {
    private final SparseArray<UserState> mUserStates = new SparseArray<>();
    private final SparseBooleanArray mMissing = new SparseBooleanArray();

    public void copyFrom(LegacyPermissionState other) {
        if (other == this) {
            return;
        }
        this.mUserStates.clear();
        int userStatesSize = other.mUserStates.size();
        for (int i = 0; i < userStatesSize; i++) {
            this.mUserStates.put(other.mUserStates.keyAt(i), new UserState(other.mUserStates.valueAt(i)));
        }
        this.mMissing.clear();
        int missingSize = other.mMissing.size();
        for (int i2 = 0; i2 < missingSize; i2++) {
            this.mMissing.put(other.mMissing.keyAt(i2), other.mMissing.valueAt(i2));
        }
    }

    public void reset() {
        this.mUserStates.clear();
        this.mMissing.clear();
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        LegacyPermissionState other = (LegacyPermissionState) object;
        int userStatesSize = this.mUserStates.size();
        if (userStatesSize != other.mUserStates.size()) {
            return false;
        }
        for (int i = 0; i < userStatesSize; i++) {
            int userId = this.mUserStates.keyAt(i);
            if (!Objects.equals(this.mUserStates.get(userId), other.mUserStates.get(userId))) {
                return false;
            }
        }
        return Objects.equals(this.mMissing, other.mMissing);
    }

    public PermissionState getPermissionState(String permissionName, int userId) {
        checkUserId(userId);
        UserState userState = this.mUserStates.get(userId);
        if (userState == null) {
            return null;
        }
        return userState.getPermissionState(permissionName);
    }

    public void putPermissionState(PermissionState permissionState, int userId) {
        checkUserId(userId);
        UserState userState = this.mUserStates.get(userId);
        if (userState == null) {
            userState = new UserState();
            this.mUserStates.put(userId, userState);
        }
        userState.putPermissionState(permissionState);
    }

    public boolean hasPermissionState(Collection<String> permissionNames) {
        int userStatesSize = this.mUserStates.size();
        for (int i = 0; i < userStatesSize; i++) {
            UserState userState = this.mUserStates.valueAt(i);
            for (String permissionName : permissionNames) {
                if (userState.getPermissionState(permissionName) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public Collection<PermissionState> getPermissionStates(int userId) {
        checkUserId(userId);
        UserState userState = this.mUserStates.get(userId);
        if (userState == null) {
            return Collections.emptyList();
        }
        return userState.getPermissionStates();
    }

    public boolean isMissing(int userId) {
        checkUserId(userId);
        return this.mMissing.get(userId);
    }

    public void setMissing(boolean missing, int userId) {
        checkUserId(userId);
        if (missing) {
            this.mMissing.put(userId, true);
        } else {
            this.mMissing.delete(userId);
        }
    }

    private static void checkUserId(int userId) {
        if (userId < 0) {
            throw new IllegalArgumentException("Invalid user ID " + userId);
        }
    }

    /* loaded from: classes2.dex */
    private static final class UserState {
        private final ArrayMap<String, PermissionState> mPermissionStates = new ArrayMap<>();

        public UserState() {
        }

        public UserState(UserState other) {
            int permissionStatesSize = other.mPermissionStates.size();
            for (int i = 0; i < permissionStatesSize; i++) {
                this.mPermissionStates.put(other.mPermissionStates.keyAt(i), new PermissionState(other.mPermissionStates.valueAt(i)));
            }
        }

        public PermissionState getPermissionState(String permissionName) {
            return this.mPermissionStates.get(permissionName);
        }

        public void putPermissionState(PermissionState permissionState) {
            this.mPermissionStates.put(permissionState.getName(), permissionState);
        }

        public Collection<PermissionState> getPermissionStates() {
            return Collections.unmodifiableCollection(this.mPermissionStates.values());
        }
    }

    /* loaded from: classes2.dex */
    public static final class PermissionState {
        private final int mFlags;
        private final boolean mGranted;
        private final String mName;
        private final boolean mRuntime;

        public PermissionState(String name, boolean runtime, boolean granted, int flags) {
            this.mName = name;
            this.mRuntime = runtime;
            this.mGranted = granted;
            this.mFlags = flags;
        }

        private PermissionState(PermissionState other) {
            this.mName = other.mName;
            this.mRuntime = other.mRuntime;
            this.mGranted = other.mGranted;
            this.mFlags = other.mFlags;
        }

        public String getName() {
            return this.mName;
        }

        public boolean isRuntime() {
            return this.mRuntime;
        }

        public boolean isGranted() {
            return this.mGranted;
        }

        public int getFlags() {
            return this.mFlags;
        }

        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            PermissionState that = (PermissionState) object;
            if (this.mRuntime == that.mRuntime && this.mGranted == that.mGranted && this.mFlags == that.mFlags && Objects.equals(this.mName, that.mName)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(this.mName, Boolean.valueOf(this.mRuntime), Boolean.valueOf(this.mGranted), Integer.valueOf(this.mFlags));
        }
    }
}
