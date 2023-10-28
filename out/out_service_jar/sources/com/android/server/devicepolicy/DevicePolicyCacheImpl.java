package com.android.server.devicepolicy;

import android.app.admin.DevicePolicyCache;
import android.util.IndentingPrintWriter;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
/* loaded from: classes.dex */
public class DevicePolicyCacheImpl extends DevicePolicyCache {
    private final Object mLock = new Object();
    private int mScreenCaptureDisallowedUser = -10000;
    private final SparseIntArray mPasswordQuality = new SparseIntArray();
    private final SparseIntArray mPermissionPolicy = new SparseIntArray();
    private final SparseBooleanArray mCanGrantSensorsPermissions = new SparseBooleanArray();

    public void onUserRemoved(int userHandle) {
        synchronized (this.mLock) {
            this.mPasswordQuality.delete(userHandle);
            this.mPermissionPolicy.delete(userHandle);
            this.mCanGrantSensorsPermissions.delete(userHandle);
        }
    }

    public boolean isScreenCaptureAllowed(int userHandle) {
        boolean z;
        synchronized (this.mLock) {
            int i = this.mScreenCaptureDisallowedUser;
            z = (i == -1 || i == userHandle) ? false : true;
        }
        return z;
    }

    public int getScreenCaptureDisallowedUser() {
        int i;
        synchronized (this.mLock) {
            i = this.mScreenCaptureDisallowedUser;
        }
        return i;
    }

    public void setScreenCaptureDisallowedUser(int userHandle) {
        synchronized (this.mLock) {
            this.mScreenCaptureDisallowedUser = userHandle;
        }
    }

    public int getPasswordQuality(int userHandle) {
        int i;
        synchronized (this.mLock) {
            i = this.mPasswordQuality.get(userHandle, 0);
        }
        return i;
    }

    public void setPasswordQuality(int userHandle, int quality) {
        synchronized (this.mLock) {
            this.mPasswordQuality.put(userHandle, quality);
        }
    }

    public int getPermissionPolicy(int userHandle) {
        int i;
        synchronized (this.mLock) {
            i = this.mPermissionPolicy.get(userHandle, 0);
        }
        return i;
    }

    public void setPermissionPolicy(int userHandle, int policy) {
        synchronized (this.mLock) {
            this.mPermissionPolicy.put(userHandle, policy);
        }
    }

    public boolean canAdminGrantSensorsPermissionsForUser(int userId) {
        boolean z;
        synchronized (this.mLock) {
            z = this.mCanGrantSensorsPermissions.get(userId, false);
        }
        return z;
    }

    public void setAdminCanGrantSensorsPermissions(int userId, boolean canGrant) {
        synchronized (this.mLock) {
            this.mCanGrantSensorsPermissions.put(userId, canGrant);
        }
    }

    public void dump(IndentingPrintWriter pw) {
        pw.println("Device policy cache:");
        pw.increaseIndent();
        pw.println("Screen capture disallowed user: " + this.mScreenCaptureDisallowedUser);
        pw.println("Password quality: " + this.mPasswordQuality.toString());
        pw.println("Permission policy: " + this.mPermissionPolicy.toString());
        pw.println("Admin can grant sensors permission: " + this.mCanGrantSensorsPermissions.toString());
        pw.decreaseIndent();
    }
}
