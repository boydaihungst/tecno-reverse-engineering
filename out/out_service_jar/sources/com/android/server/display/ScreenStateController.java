package com.android.server.display;

import android.os.SystemProperties;
import android.util.Slog;
/* loaded from: classes.dex */
final class ScreenStateController {
    private static final String TAG = "ScreenStateController";
    private static final boolean mDualDisplaySupport;
    private final Object mLock;
    private boolean mSecondaryScreenOff;

    static {
        mDualDisplaySupport = SystemProperties.getInt("ro.product.dualdisplay.support", 0) == 1;
    }

    private ScreenStateController() {
        this.mSecondaryScreenOff = true;
        this.mLock = new Object();
    }

    /* loaded from: classes.dex */
    private static class ScreenStateControllerInstance {
        private static final ScreenStateController INSTANCE = new ScreenStateController();

        private ScreenStateControllerInstance() {
        }
    }

    public static ScreenStateController getInstance() {
        return ScreenStateControllerInstance.INSTANCE;
    }

    public void hookSecondaryScreenState(boolean isDualDisplay, int state) {
        synchronized (this.mLock) {
            if (mDualDisplaySupport && isDualDisplay) {
                if (state == 1) {
                    this.mSecondaryScreenOff = true;
                    this.mLock.notifyAll();
                } else {
                    this.mSecondaryScreenOff = false;
                }
            }
        }
    }

    public void hookScreenUpdate(int displayId, int state) {
        synchronized (this.mLock) {
            if (mDualDisplaySupport && displayId == 0 && state == 1 && !this.mSecondaryScreenOff) {
                try {
                    this.mLock.wait(200L);
                    Slog.i(TAG, "SecondaryDisplay has not been set STATE_OFF yet, so primaryDisplay can't be set STATE_OFF");
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
