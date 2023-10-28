package com.android.server;

import android.os.ConditionVariable;
import android.os.SystemClock;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes4.dex */
public abstract class ResettableTimeout {
    private ConditionVariable mLock = new ConditionVariable();
    private volatile long mOffAt;
    private volatile boolean mOffCalled;
    private Thread mThread;

    public abstract void off();

    public abstract void on(boolean z);

    ResettableTimeout() {
    }

    public void go(long milliseconds) {
        boolean alreadyOn;
        synchronized (this) {
            this.mOffAt = SystemClock.uptimeMillis() + milliseconds;
            Thread thread = this.mThread;
            if (thread == null) {
                alreadyOn = false;
                this.mLock.close();
                T t = new T();
                this.mThread = t;
                t.start();
                this.mLock.block();
                this.mOffCalled = false;
            } else {
                thread.interrupt();
                alreadyOn = true;
            }
            on(alreadyOn);
        }
    }

    public void cancel() {
        synchronized (this) {
            this.mOffAt = 0L;
            Thread thread = this.mThread;
            if (thread != null) {
                thread.interrupt();
                this.mThread = null;
            }
            if (!this.mOffCalled) {
                this.mOffCalled = true;
                off();
            }
        }
    }

    /* loaded from: classes4.dex */
    private class T extends Thread {
        private T() {
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            long diff;
            ResettableTimeout.this.mLock.open();
            while (true) {
                synchronized (this) {
                    diff = ResettableTimeout.this.mOffAt - SystemClock.uptimeMillis();
                    if (diff <= 0) {
                        ResettableTimeout.this.mOffCalled = true;
                        ResettableTimeout.this.off();
                        ResettableTimeout.this.mThread = null;
                        return;
                    }
                }
                try {
                    sleep(diff);
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
