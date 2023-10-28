package com.android.server.wm;

import android.os.Process;
import com.android.server.AnimationThread;
import com.android.server.ThreadPriorityBooster;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class WindowManagerThreadPriorityBooster extends ThreadPriorityBooster {
    private final int mAnimationThreadId;
    private boolean mAppTransitionRunning;
    private boolean mBoundsAnimationRunning;
    private final Object mLock;
    private final int mSurfaceAnimationThreadId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowManagerThreadPriorityBooster() {
        super(-4, 5);
        this.mLock = new Object();
        this.mAnimationThreadId = AnimationThread.get().getThreadId();
        this.mSurfaceAnimationThreadId = SurfaceAnimationThread.get().getThreadId();
    }

    @Override // com.android.server.ThreadPriorityBooster
    public void boost() {
        int myTid = Process.myTid();
        if (myTid == this.mAnimationThreadId || myTid == this.mSurfaceAnimationThreadId) {
            return;
        }
        super.boost();
    }

    @Override // com.android.server.ThreadPriorityBooster
    public void reset() {
        int myTid = Process.myTid();
        if (myTid == this.mAnimationThreadId || myTid == this.mSurfaceAnimationThreadId) {
            return;
        }
        super.reset();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAppTransitionRunning(boolean running) {
        synchronized (this.mLock) {
            if (this.mAppTransitionRunning != running) {
                this.mAppTransitionRunning = running;
                updatePriorityLocked();
            }
        }
    }

    void setBoundsAnimationRunning(boolean running) {
        synchronized (this.mLock) {
            if (this.mBoundsAnimationRunning != running) {
                this.mBoundsAnimationRunning = running;
                updatePriorityLocked();
            }
        }
    }

    private void updatePriorityLocked() {
        int priority = (this.mAppTransitionRunning || this.mBoundsAnimationRunning) ? -10 : -4;
        setBoostToPriority(priority);
        Process.setThreadPriority(this.mAnimationThreadId, priority);
        Process.setThreadPriority(this.mSurfaceAnimationThreadId, priority);
    }
}
