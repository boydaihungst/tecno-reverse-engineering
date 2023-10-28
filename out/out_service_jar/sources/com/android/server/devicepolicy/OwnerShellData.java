package com.android.server.devicepolicy;

import android.content.ComponentName;
import com.android.internal.util.Preconditions;
import java.util.Objects;
/* loaded from: classes.dex */
final class OwnerShellData {
    public final ComponentName admin;
    public boolean isAffiliated;
    public final boolean isDeviceOwner;
    public final boolean isManagedProfileOwner;
    public final boolean isProfileOwner;
    public final int parentUserId;
    public final int userId;

    private OwnerShellData(int userId, int parentUserId, ComponentName admin, boolean isDeviceOwner, boolean isProfileOwner, boolean isManagedProfileOwner) {
        Preconditions.checkArgument(userId != -10000, "userId cannot be USER_NULL");
        this.userId = userId;
        this.parentUserId = parentUserId;
        this.admin = (ComponentName) Objects.requireNonNull(admin, "admin must not be null");
        this.isDeviceOwner = isDeviceOwner;
        this.isProfileOwner = isProfileOwner;
        this.isManagedProfileOwner = isManagedProfileOwner;
        if (isManagedProfileOwner) {
            Preconditions.checkArgument(parentUserId != -10000, "parentUserId cannot be USER_NULL for managed profile owner");
            Preconditions.checkArgument(parentUserId != userId, "cannot be parent of itself (%d)", new Object[]{Integer.valueOf(userId)});
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("[userId=").append(this.userId).append(",admin=").append(this.admin.flattenToShortString());
        if (this.isDeviceOwner) {
            sb.append(",deviceOwner");
        }
        if (this.isProfileOwner) {
            sb.append(",isProfileOwner");
        }
        if (this.isManagedProfileOwner) {
            sb.append(",isManagedProfileOwner");
        }
        if (this.parentUserId != -10000) {
            sb.append(",parentUserId=").append(this.parentUserId);
        }
        if (this.isAffiliated) {
            sb.append(",isAffiliated");
        }
        return sb.append(']').toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static OwnerShellData forDeviceOwner(int userId, ComponentName admin) {
        return new OwnerShellData(userId, -10000, admin, true, false, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static OwnerShellData forUserProfileOwner(int userId, ComponentName admin) {
        return new OwnerShellData(userId, -10000, admin, false, true, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static OwnerShellData forManagedProfileOwner(int userId, int parentUserId, ComponentName admin) {
        return new OwnerShellData(userId, parentUserId, admin, false, false, true);
    }
}
