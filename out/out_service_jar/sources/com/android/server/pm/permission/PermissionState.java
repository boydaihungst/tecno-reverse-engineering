package com.android.server.pm.permission;
/* loaded from: classes2.dex */
public final class PermissionState {
    private int mFlags;
    private boolean mGranted;
    private final Object mLock;
    private final Permission mPermission;

    public PermissionState(Permission permission) {
        this.mLock = new Object();
        this.mPermission = permission;
    }

    public PermissionState(PermissionState other) {
        this(other.mPermission);
        this.mGranted = other.mGranted;
        this.mFlags = other.mFlags;
    }

    public Permission getPermission() {
        return this.mPermission;
    }

    public String getName() {
        return this.mPermission.getName();
    }

    public int[] computeGids(int userId) {
        return this.mPermission.computeGids(userId);
    }

    public boolean isGranted() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mGranted;
        }
        return z;
    }

    public boolean grant() {
        synchronized (this.mLock) {
            if (this.mGranted) {
                return false;
            }
            this.mGranted = true;
            UidPermissionState.invalidateCache();
            return true;
        }
    }

    public boolean revoke() {
        synchronized (this.mLock) {
            if (this.mGranted) {
                this.mGranted = false;
                UidPermissionState.invalidateCache();
                return true;
            }
            return false;
        }
    }

    public int getFlags() {
        int i;
        synchronized (this.mLock) {
            i = this.mFlags;
        }
        return i;
    }

    public boolean updateFlags(int flagMask, int flagValues) {
        boolean z;
        synchronized (this.mLock) {
            int newFlags = flagValues & flagMask;
            UidPermissionState.invalidateCache();
            int oldFlags = this.mFlags;
            int i = (oldFlags & (~flagMask)) | newFlags;
            this.mFlags = i;
            z = i != oldFlags;
        }
        return z;
    }

    public boolean isDefault() {
        boolean z;
        synchronized (this.mLock) {
            z = !this.mGranted && this.mFlags == 0;
        }
        return z;
    }
}
