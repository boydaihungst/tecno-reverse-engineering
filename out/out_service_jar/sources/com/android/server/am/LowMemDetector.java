package com.android.server.am;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes.dex */
public final class LowMemDetector {
    public static final int ADJ_MEM_FACTOR_NOTHING = -1;
    private static final String TAG = "LowMemDetector";
    private final ActivityManagerService mAm;
    private boolean mAvailable;
    private final LowMemThread mLowMemThread;
    private final Object mPressureStateLock = new Object();
    private int mPressureState = 0;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface MemFactor {
    }

    private native int init();

    /* JADX INFO: Access modifiers changed from: private */
    public native int waitForPressure();

    /* JADX INFO: Access modifiers changed from: package-private */
    public LowMemDetector(ActivityManagerService am) {
        this.mAm = am;
        LowMemThread lowMemThread = new LowMemThread();
        this.mLowMemThread = lowMemThread;
        if (init() != 0) {
            this.mAvailable = false;
            return;
        }
        this.mAvailable = true;
        lowMemThread.start();
    }

    public boolean isAvailable() {
        return this.mAvailable;
    }

    public int getMemFactor() {
        int i;
        synchronized (this.mPressureStateLock) {
            i = this.mPressureState;
        }
        return i;
    }

    /* loaded from: classes.dex */
    private final class LowMemThread extends Thread {
        private LowMemThread() {
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            while (true) {
                int newPressureState = LowMemDetector.this.waitForPressure();
                if (newPressureState == -1) {
                    LowMemDetector.this.mAvailable = false;
                    return;
                }
                synchronized (LowMemDetector.this.mPressureStateLock) {
                    LowMemDetector.this.mPressureState = newPressureState;
                }
            }
        }
    }
}
