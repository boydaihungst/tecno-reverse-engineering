package com.android.server.biometrics.sensors.face;

import com.android.server.biometrics.sensors.LockoutTracker;
/* loaded from: classes.dex */
public class LockoutHalImpl implements LockoutTracker {
    private int mCurrentUserLockoutMode;

    @Override // com.android.server.biometrics.sensors.LockoutTracker
    public int getLockoutModeForUser(int userId) {
        return this.mCurrentUserLockoutMode;
    }

    public void setCurrentUserLockoutMode(int lockoutMode) {
        this.mCurrentUserLockoutMode = lockoutMode;
    }
}
