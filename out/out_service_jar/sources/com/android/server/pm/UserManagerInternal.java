package com.android.server.pm;

import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.UserManager;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
/* loaded from: classes2.dex */
public abstract class UserManagerInternal {
    public static final int OWNER_TYPE_DEVICE_OWNER = 0;
    public static final int OWNER_TYPE_NO_OWNER = 3;
    public static final int OWNER_TYPE_PROFILE_OWNER = 1;
    public static final int OWNER_TYPE_PROFILE_OWNER_OF_ORGANIZATION_OWNED_DEVICE = 2;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface OwnerType {
    }

    /* loaded from: classes2.dex */
    public interface UserRestrictionsListener {
        void onUserRestrictionsChanged(int i, Bundle bundle, Bundle bundle2);
    }

    public abstract void addUserLifecycleListener(UserLifecycleListener userLifecycleListener);

    public abstract void addUserRestrictionsListener(UserRestrictionsListener userRestrictionsListener);

    public abstract UserInfo createUserEvenWhenDisallowed(String str, String str2, int i, String[] strArr, Object obj) throws UserManager.CheckedUserOperationException;

    public abstract boolean exists(int i);

    public abstract int getProfileParentId(int i);

    public abstract int[] getUserIds();

    public abstract UserInfo getUserInfo(int i);

    public abstract UserInfo[] getUserInfos();

    public abstract boolean getUserRestriction(int i, String str);

    public abstract List<UserInfo> getUsers(boolean z);

    public abstract List<UserInfo> getUsers(boolean z, boolean z2, boolean z3);

    public abstract boolean hasUserRestriction(String str, int i);

    public abstract boolean isDeviceManaged();

    public abstract boolean isDualProfile(int i);

    public abstract boolean isProfileAccessible(int i, int i2, String str, boolean z);

    public abstract boolean isSettingRestrictedForUser(String str, int i, String str2, int i2);

    public abstract boolean isUserInitialized(int i);

    public abstract boolean isUserManaged(int i);

    public abstract boolean isUserRunning(int i);

    public abstract boolean isUserUnlocked(int i);

    public abstract boolean isUserUnlockingOrUnlocked(int i);

    public abstract void onEphemeralUserStop(int i);

    public abstract void removeAllUsers();

    public abstract boolean removeUserEvenWhenDisallowed(int i);

    public abstract void removeUserLifecycleListener(UserLifecycleListener userLifecycleListener);

    public abstract void removeUserRestrictionsListener(UserRestrictionsListener userRestrictionsListener);

    public abstract void removeUserState(int i);

    public abstract void setDefaultCrossProfileIntentFilters(int i, int i2);

    public abstract void setDeviceManaged(boolean z);

    public abstract void setDevicePolicyUserRestrictions(int i, Bundle bundle, RestrictionsSet restrictionsSet, boolean z);

    public abstract void setForceEphemeralUsers(boolean z);

    public abstract void setUserIcon(int i, Bitmap bitmap);

    public abstract void setUserManaged(int i, boolean z);

    public abstract void setUserState(int i, int i2);

    public abstract boolean shouldIgnorePrepareStorageErrors(int i);

    /* loaded from: classes2.dex */
    public interface UserLifecycleListener {
        default void onUserCreated(UserInfo user, Object token) {
        }

        default void onUserRemoved(UserInfo user) {
        }
    }
}
