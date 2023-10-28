package com.android.server.wm;

import android.os.Debug;
import android.os.SystemProperties;
import android.util.Slog;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class WindowSurfacePlacer {
    static final int SET_UPDATE_ROTATION = 1;
    static final int SET_WALLPAPER_ACTION_PENDING = 2;
    private static final String TAG = "WindowManager";
    private static final boolean TRAN_USER_ROOT_SUPPORT = "1".equals(SystemProperties.get("persist.sys.adb.support", "0"));
    private int mDeferredRequests;
    private int mLayoutRepeatCount;
    private final WindowManagerService mService;
    private boolean mTraversalScheduled;
    RuntimeException re;
    private boolean mInLayout = false;
    private int mDeferDepth = 0;
    private final Traverser mPerformSurfacePlacement = new Traverser();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class Traverser implements Runnable {
        private Traverser() {
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (WindowSurfacePlacer.this.mService.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowSurfacePlacer.this.performSurfacePlacement();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowSurfacePlacer(WindowManagerService service) {
        this.mService = service;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void deferLayout() {
        this.mDeferDepth++;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void continueLayout(boolean hasChanges) {
        int i = this.mDeferDepth - 1;
        this.mDeferDepth = i;
        if (i > 0) {
            return;
        }
        if (hasChanges || this.mDeferredRequests > 0) {
            if (WindowManagerDebugConfig.DEBUG) {
                Slog.i("WindowManager", "continueLayout hasChanges=" + hasChanges + " deferredRequests=" + this.mDeferredRequests + " " + Debug.getCallers(2, 3));
            }
            performSurfacePlacement();
            this.mDeferredRequests = 0;
        } else if (WindowManagerDebugConfig.DEBUG) {
            Slog.i("WindowManager", "Cancel continueLayout " + Debug.getCallers(2, 3));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLayoutDeferred() {
        return this.mDeferDepth > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void performSurfacePlacementIfScheduled() {
        if (this.mTraversalScheduled) {
            performSurfacePlacement();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void performSurfacePlacement() {
        performSurfacePlacement(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void performSurfacePlacement(boolean force) {
        if (this.mDeferDepth > 0 && !force) {
            this.mDeferredRequests++;
            return;
        }
        int loopCount = 6;
        do {
            this.mTraversalScheduled = false;
            performSurfacePlacementLoop();
            this.mService.mAnimationHandler.removeCallbacks(this.mPerformSurfacePlacement);
            loopCount--;
            if (!this.mTraversalScheduled) {
                break;
            }
        } while (loopCount > 0);
        this.mService.mRoot.mWallpaperActionPending = false;
    }

    private void performSurfacePlacementLoop() {
        if (this.mInLayout) {
            if (TRAN_USER_ROOT_SUPPORT) {
                Thread.currentThread();
                boolean isHoldLockInLayout = Thread.holdsLock(this.mService.mGlobalLock);
                if (isHoldLockInLayout) {
                    Slog.w("WindowManager", "performSurface is called by ", this.re);
                } else {
                    Slog.w("WindowManager", "performSurface this way is " + Debug.getCaller());
                }
            }
            boolean isHoldLockInLayout2 = WindowManagerDebugConfig.DEBUG;
            if (isHoldLockInLayout2) {
                throw new RuntimeException("Recursive call!");
            }
            Slog.w("WindowManager", "performLayoutAndPlaceSurfacesLocked called while in layout. Callers=" + Debug.getCallers(3));
            return;
        }
        DisplayContent defaultDisplay = this.mService.getDefaultDisplayContentLocked();
        if (defaultDisplay.mWaitingForConfig || !this.mService.mDisplayReady) {
            return;
        }
        this.mInLayout = true;
        if (TRAN_USER_ROOT_SUPPORT) {
            Thread.currentThread();
            boolean isHoldLock = Thread.holdsLock(this.mService.mGlobalLock);
            if (!isHoldLock) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                RuntimeException runtimeException = new RuntimeException("First: " + format.format(new Date()));
                this.re = runtimeException;
                runtimeException.fillInStackTrace();
            }
        }
        if (!this.mService.mForceRemoves.isEmpty()) {
            while (!this.mService.mForceRemoves.isEmpty()) {
                WindowState ws = this.mService.mForceRemoves.remove(0);
                Slog.i("WindowManager", "Force removing: " + ws);
                ws.removeImmediately();
            }
            Slog.w("WindowManager", "Due to memory failure, waiting a bit for next layout");
            Object tmp = new Object();
            synchronized (tmp) {
                try {
                    tmp.wait(250L);
                } catch (InterruptedException e) {
                }
            }
        }
        try {
            this.mService.mRoot.performSurfacePlacement();
            this.mInLayout = false;
            if (this.mService.mRoot.isLayoutNeeded()) {
                int i = this.mLayoutRepeatCount + 1;
                this.mLayoutRepeatCount = i;
                if (i < 6) {
                    requestTraversal();
                } else {
                    Slog.e("WindowManager", "Performed 6 layouts in a row. Skipping");
                    this.mLayoutRepeatCount = 0;
                }
            } else {
                this.mLayoutRepeatCount = 0;
            }
            if (this.mService.mWindowsChanged && !this.mService.mWindowChangeListeners.isEmpty()) {
                this.mService.mH.removeMessages(19);
                this.mService.mH.sendEmptyMessage(19);
            }
        } catch (RuntimeException e2) {
            this.mInLayout = false;
            Slog.wtf("WindowManager", "Unhandled exception while laying out windows", e2);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void debugLayoutRepeats(String msg, int pendingLayoutChanges) {
        if (this.mLayoutRepeatCount >= 4) {
            Slog.v("WindowManager", "Layouts looping: " + msg + ", mPendingLayoutChanges = 0x" + Integer.toHexString(pendingLayoutChanges));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInLayout() {
        return this.mInLayout;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void requestTraversal() {
        if (this.mTraversalScheduled) {
            return;
        }
        this.mTraversalScheduled = true;
        if (this.mDeferDepth > 0) {
            this.mDeferredRequests++;
            if (WindowManagerDebugConfig.DEBUG) {
                Slog.i("WindowManager", "Defer requestTraversal " + Debug.getCallers(3));
                return;
            }
            return;
        }
        this.mService.mAnimationHandler.post(this.mPerformSurfacePlacement);
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.println(prefix + "mTraversalScheduled=" + this.mTraversalScheduled);
        pw.println(prefix + "mHoldScreenWindow=" + this.mService.mRoot.mHoldScreenWindow);
        pw.println(prefix + "mObscuringWindow=" + this.mService.mRoot.mObscuringWindow);
    }
}
