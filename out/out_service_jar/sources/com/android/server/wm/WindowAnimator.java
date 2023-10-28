package com.android.server.wm;

import android.content.Context;
import android.os.Trace;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.view.Choreographer;
import android.view.SurfaceControl;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.server.policy.WindowManagerPolicy;
import com.mediatek.powerhalmgr.PowerHalMgr;
import com.mediatek.powerhalmgr.PowerHalMgrFactory;
import java.io.PrintWriter;
import java.util.ArrayList;
/* loaded from: classes2.dex */
public class WindowAnimator {
    private static final int MTKPOWER_HINT_ANIMATING_BOOST = 50;
    private static final String TAG = "WindowManager";
    private static PowerHalMgr mPowerHalService;
    final Choreographer.FrameCallback mAnimationFrameCallback;
    private boolean mAnimationFrameCallbackScheduled;
    private Choreographer mChoreographer;
    final Context mContext;
    long mCurrentTime;
    private boolean mInExecuteAfterPrepareSurfacesRunnables;
    private boolean mLastRootAnimating;
    Object mLastWindowFreezeSource;
    final WindowManagerPolicy mPolicy;
    private boolean mRunningExpensiveAnimations;
    final WindowManagerService mService;
    private final SurfaceControl.Transaction mTransaction;
    int mBulkUpdateParams = 0;
    SparseArray<DisplayContentsAnimator> mDisplayContentsAnimators = new SparseArray<>(2);
    private boolean mInitialized = false;
    private boolean mRemoveReplacedWindows = false;
    boolean mNotifyWhenNoAnimation = false;
    private final ArrayList<Runnable> mAfterPrepareSurfacesRunnables = new ArrayList<>();
    private int mPowerHandle = 0;

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowAnimator(WindowManagerService service) {
        this.mService = service;
        this.mContext = service.mContext;
        this.mPolicy = service.mPolicy;
        this.mTransaction = service.mTransactionFactory.get();
        service.mAnimationHandler.runWithScissors(new Runnable() { // from class: com.android.server.wm.WindowAnimator$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                WindowAnimator.this.m8460lambda$new$0$comandroidserverwmWindowAnimator();
            }
        }, 0L);
        this.mAnimationFrameCallback = new Choreographer.FrameCallback() { // from class: com.android.server.wm.WindowAnimator$$ExternalSyntheticLambda1
            @Override // android.view.Choreographer.FrameCallback
            public final void doFrame(long j) {
                WindowAnimator.this.m8461lambda$new$1$comandroidserverwmWindowAnimator(j);
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-wm-WindowAnimator  reason: not valid java name */
    public /* synthetic */ void m8460lambda$new$0$comandroidserverwmWindowAnimator() {
        this.mChoreographer = Choreographer.getSfInstance();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$1$com-android-server-wm-WindowAnimator  reason: not valid java name */
    public /* synthetic */ void m8461lambda$new$1$comandroidserverwmWindowAnimator(long frameTimeNs) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mAnimationFrameCallbackScheduled = false;
                long vsyncId = this.mChoreographer.getVsyncId();
                Trace.traceBegin(32L, "wmAnimate");
                animate(frameTimeNs, vsyncId);
                Trace.traceEnd(32L);
                if (this.mNotifyWhenNoAnimation && !this.mLastRootAnimating) {
                    this.mService.mGlobalLock.notifyAll();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addDisplayLocked(int displayId) {
        getDisplayContentsAnimatorLocked(displayId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeDisplayLocked(int displayId) {
        this.mDisplayContentsAnimators.delete(displayId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void ready() {
        this.mInitialized = true;
    }

    private void animate(long frameTimeNs, long vsyncId) {
        boolean doRequest;
        if (!this.mInitialized) {
            return;
        }
        scheduleAnimation();
        RootWindowContainer root = this.mService.mRoot;
        this.mCurrentTime = frameTimeNs / 1000000;
        this.mBulkUpdateParams = 0;
        root.mOrientationChangeComplete = true;
        if (WindowManagerDebugConfig.DEBUG_WINDOW_TRACE) {
            Slog.i("WindowManager", "!!! animate: entry time=" + this.mCurrentTime);
        }
        if (ProtoLogCache.WM_SHOW_TRANSACTIONS_enabled) {
            ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_TRANSACTIONS, 1984782949, 0, (String) null, (Object[]) null);
        }
        this.mService.openSurfaceTransaction();
        try {
            root.handleCompleteDeferredRemoval();
            AccessibilityController accessibilityController = this.mService.mAccessibilityController;
            int numDisplays = this.mDisplayContentsAnimators.size();
            for (int i = 0; i < numDisplays; i++) {
                DisplayContent dc = root.getDisplayContent(this.mDisplayContentsAnimators.keyAt(i));
                dc.updateWindowsForAnimator();
                dc.prepareSurfaces();
            }
            for (int i2 = 0; i2 < numDisplays; i2++) {
                int displayId = this.mDisplayContentsAnimators.keyAt(i2);
                root.getDisplayContent(displayId).checkAppWindowsReadyToShow();
                if (accessibilityController.hasCallbacks()) {
                    accessibilityController.drawMagnifiedRegionBorderIfNeeded(displayId, this.mTransaction);
                }
            }
            cancelAnimation();
            if (this.mService.mWatermark != null) {
                this.mService.mWatermark.drawIfNeeded();
            }
        } catch (RuntimeException e) {
            Slog.wtf("WindowManager", "Unhandled exception in Window Manager", e);
        }
        boolean hasPendingLayoutChanges = root.hasPendingLayoutChanges(this);
        if ((this.mBulkUpdateParams == 0 && !root.mOrientationChangeComplete) || !root.copyAnimToLayoutParams()) {
            doRequest = false;
        } else {
            doRequest = true;
        }
        if (hasPendingLayoutChanges || doRequest) {
            this.mService.mWindowPlacerLocked.requestTraversal();
        }
        boolean rootAnimating = root.isAnimating(5, -1);
        if (rootAnimating && !this.mLastRootAnimating) {
            Trace.asyncTraceBegin(32L, "animating", 0);
            startAnimatingBoost();
        }
        if (!rootAnimating && this.mLastRootAnimating) {
            this.mService.mWindowPlacerLocked.requestTraversal();
            Trace.asyncTraceEnd(32L, "animating", 0);
            releaseAnimatingBoost();
        }
        this.mLastRootAnimating = rootAnimating;
        boolean runningExpensiveAnimations = root.isAnimating(5, 11);
        if (runningExpensiveAnimations && !this.mRunningExpensiveAnimations) {
            this.mService.mTaskSnapshotController.setPersisterPaused(true);
            this.mTransaction.setEarlyWakeupStart();
        } else if (!runningExpensiveAnimations && this.mRunningExpensiveAnimations) {
            this.mService.mTaskSnapshotController.setPersisterPaused(false);
            this.mTransaction.setEarlyWakeupEnd();
        }
        this.mRunningExpensiveAnimations = runningExpensiveAnimations;
        SurfaceControl.mergeToGlobalTransaction(this.mTransaction);
        this.mService.closeSurfaceTransaction("WindowAnimator");
        if (ProtoLogCache.WM_SHOW_TRANSACTIONS_enabled) {
            ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_TRANSACTIONS, -545190927, 0, (String) null, (Object[]) null);
        }
        if (this.mRemoveReplacedWindows) {
            root.removeReplacedWindows();
            this.mRemoveReplacedWindows = false;
        }
        this.mService.mAtmService.mTaskOrganizerController.dispatchPendingEvents();
        executeAfterPrepareSurfacesRunnables();
        if (WindowManagerDebugConfig.DEBUG_WINDOW_TRACE) {
            Slog.i("WindowManager", "!!! animate: exit mBulkUpdateParams=" + Integer.toHexString(this.mBulkUpdateParams) + " hasPendingLayoutChanges=" + hasPendingLayoutChanges);
        }
    }

    private static String bulkUpdateParamsToString(int bulkUpdateParams) {
        StringBuilder builder = new StringBuilder(128);
        if ((bulkUpdateParams & 1) != 0) {
            builder.append(" UPDATE_ROTATION");
        }
        if ((bulkUpdateParams & 2) != 0) {
            builder.append(" SET_WALLPAPER_ACTION_PENDING");
        }
        return builder.toString();
    }

    public void dumpLocked(PrintWriter pw, String prefix, boolean dumpAll) {
        String subPrefix = "  " + prefix;
        for (int i = 0; i < this.mDisplayContentsAnimators.size(); i++) {
            pw.print(prefix);
            pw.print("DisplayContentsAnimator #");
            pw.print(this.mDisplayContentsAnimators.keyAt(i));
            pw.println(":");
            DisplayContent dc = this.mService.mRoot.getDisplayContent(this.mDisplayContentsAnimators.keyAt(i));
            dc.dumpWindowAnimators(pw, subPrefix);
            pw.println();
        }
        pw.println();
        if (dumpAll) {
            pw.print(prefix);
            pw.print("mCurrentTime=");
            pw.println(TimeUtils.formatUptime(this.mCurrentTime));
        }
        if (this.mBulkUpdateParams != 0) {
            pw.print(prefix);
            pw.print("mBulkUpdateParams=0x");
            pw.print(Integer.toHexString(this.mBulkUpdateParams));
            pw.println(bulkUpdateParamsToString(this.mBulkUpdateParams));
        }
    }

    private DisplayContentsAnimator getDisplayContentsAnimatorLocked(int displayId) {
        if (displayId < 0) {
            return null;
        }
        DisplayContentsAnimator displayAnimator = this.mDisplayContentsAnimators.get(displayId);
        if (displayAnimator == null && this.mService.mRoot.getDisplayContent(displayId) != null) {
            DisplayContentsAnimator displayAnimator2 = new DisplayContentsAnimator();
            this.mDisplayContentsAnimators.put(displayId, displayAnimator2);
            return displayAnimator2;
        }
        return displayAnimator;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void requestRemovalOfReplacedWindows(WindowState win) {
        this.mRemoveReplacedWindows = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleAnimation() {
        if (!this.mAnimationFrameCallbackScheduled) {
            this.mAnimationFrameCallbackScheduled = true;
            this.mChoreographer.postFrameCallback(this.mAnimationFrameCallback);
        }
    }

    private void cancelAnimation() {
        if (this.mAnimationFrameCallbackScheduled) {
            this.mAnimationFrameCallbackScheduled = false;
            this.mChoreographer.removeFrameCallback(this.mAnimationFrameCallback);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class DisplayContentsAnimator {
        private DisplayContentsAnimator() {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAnimationScheduled() {
        return this.mAnimationFrameCallbackScheduled;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Choreographer getChoreographer() {
        return this.mChoreographer;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addAfterPrepareSurfacesRunnable(Runnable r) {
        if (this.mInExecuteAfterPrepareSurfacesRunnables) {
            r.run();
            return;
        }
        this.mAfterPrepareSurfacesRunnables.add(r);
        scheduleAnimation();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void executeAfterPrepareSurfacesRunnables() {
        if (this.mInExecuteAfterPrepareSurfacesRunnables) {
            return;
        }
        this.mInExecuteAfterPrepareSurfacesRunnables = true;
        int size = this.mAfterPrepareSurfacesRunnables.size();
        for (int i = 0; i < size; i++) {
            this.mAfterPrepareSurfacesRunnables.get(i).run();
        }
        this.mAfterPrepareSurfacesRunnables.clear();
        this.mInExecuteAfterPrepareSurfacesRunnables = false;
    }

    void startAnimatingBoost() {
        if (mPowerHalService == null) {
            mPowerHalService = PowerHalMgrFactory.getInstance().makePowerHalMgr();
        }
        PowerHalMgr powerHalMgr = mPowerHalService;
        if (powerHalMgr != null) {
            powerHalMgr.perfLockRelease(this.mPowerHandle);
            this.mPowerHandle = mPowerHalService.perfCusLockHint(50, 1000);
        }
    }

    void releaseAnimatingBoost() {
        if (mPowerHalService == null) {
            mPowerHalService = PowerHalMgrFactory.getInstance().makePowerHalMgr();
        }
        PowerHalMgr powerHalMgr = mPowerHalService;
        if (powerHalMgr != null) {
            powerHalMgr.perfLockRelease(this.mPowerHandle);
        }
    }
}
