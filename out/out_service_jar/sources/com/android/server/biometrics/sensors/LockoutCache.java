package com.android.server.biometrics.sensors;

import android.util.Slog;
import android.util.SparseIntArray;
/* loaded from: classes.dex */
public class LockoutCache implements LockoutTracker {
    private static final String TAG = "LockoutCache";
    private final SparseIntArray mUserLockoutStates = new SparseIntArray();

    public void setLockoutModeForUser(int userId, int mode) {
        Slog.d(TAG, "Lockout for user: " + userId + " is " + mode);
        synchronized (this) {
            this.mUserLockoutStates.put(userId, mode);
        }
    }

    @Override // com.android.server.biometrics.sensors.LockoutTracker
    public int getLockoutModeForUser(int userId) {
        int i;
        synchronized (this) {
            i = this.mUserLockoutStates.get(userId, 0);
        }
        return i;
    }
}
