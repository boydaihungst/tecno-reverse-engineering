package com.android.server.wm.utils;
/* loaded from: classes2.dex */
public class TranFpUnlockStateController {
    private static final int KEYGUARD_INIT = 0;
    private static final int KEYGUARD_LOCKED = 2;
    private static final int KEYGUARD_LOCKED_HIDE = 4;
    private static final int KEYGUARD_UNLOCKED = 1;
    private static final int KEYGUARD_UNLOCKED_HIDE = 3;
    private static final String TAG = "TranFpUnlockStateController";
    private static TranFpUnlockStateController mFpUnlockStateController;
    private boolean mIsAuthenticateSucceed = false;
    private boolean mIsFingerprintHideState = false;
    private boolean mIsKeyguardShown = false;
    private int mKeygaurdState = 0;

    private TranFpUnlockStateController() {
    }

    public static TranFpUnlockStateController getInstance() {
        synchronized (TranFpUnlockStateController.class) {
            if (mFpUnlockStateController == null) {
                mFpUnlockStateController = new TranFpUnlockStateController();
            }
        }
        return mFpUnlockStateController;
    }

    public synchronized void setAuthenticateSucceed(boolean success) {
        this.mIsAuthenticateSucceed = success;
    }

    public boolean isAuthenticateSucceed() {
        return this.mIsAuthenticateSucceed;
    }

    public synchronized void setFingerprintHideState(boolean hide) {
        updateKeyguardHideState(hide);
        this.mIsFingerprintHideState = hide;
    }

    public synchronized void setKeyguardGoingAway(boolean goingAway) {
        if (goingAway) {
            updateKeyguardState(false);
        }
    }

    public synchronized void setKeyguardShown(boolean keyguardShown) {
        updateKeyguardState(keyguardShown);
        this.mIsKeyguardShown = keyguardShown;
    }

    private void updateKeyguardHideState(boolean hide) {
        switch (this.mKeygaurdState) {
            case 0:
            default:
                return;
            case 1:
                if (hide) {
                    this.mKeygaurdState = 3;
                    return;
                }
                return;
            case 2:
                if (hide) {
                    this.mKeygaurdState = 4;
                    return;
                }
                return;
            case 3:
                if (!hide) {
                    this.mKeygaurdState = 1;
                    return;
                }
                return;
            case 4:
                if (!hide) {
                    this.mKeygaurdState = 2;
                    return;
                }
                return;
        }
    }

    private void updateKeyguardState(boolean keyguardShown) {
        switch (this.mKeygaurdState) {
            case 0:
                if (keyguardShown) {
                    this.mKeygaurdState = 2;
                    return;
                } else {
                    this.mKeygaurdState = 1;
                    return;
                }
            case 1:
                if (keyguardShown) {
                    this.mKeygaurdState = 2;
                    return;
                }
                return;
            case 2:
                if (!keyguardShown) {
                    this.mKeygaurdState = 1;
                    return;
                }
                return;
            case 3:
                if (keyguardShown) {
                    this.mKeygaurdState = 4;
                    return;
                }
                return;
            case 4:
                if (!keyguardShown) {
                    this.mKeygaurdState = 3;
                    return;
                } else {
                    this.mKeygaurdState = 1;
                    return;
                }
            default:
                return;
        }
    }

    public synchronized boolean canHideByFingerprint() {
        int i = this.mKeygaurdState;
        if (i == 4 || i == 3) {
            return true;
        }
        return false;
    }
}
