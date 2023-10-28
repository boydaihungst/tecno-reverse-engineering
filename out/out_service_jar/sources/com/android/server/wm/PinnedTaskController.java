package com.android.server.wm;

import android.app.PictureInPictureParams;
import android.content.ComponentName;
import android.content.res.Resources;
import android.graphics.Insets;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.RotationUtils;
import android.util.Slog;
import android.view.IPinnedTaskListener;
import android.view.SurfaceControl;
import android.window.PictureInPictureSurfaceTransaction;
import java.io.PrintWriter;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class PinnedTaskController {
    private static final int DEFER_ORIENTATION_CHANGE_TIMEOUT_MS = 1000;
    private static final String TAG = "WindowManager";
    private boolean mDeferOrientationChanging;
    private Rect mDestRotatedBounds;
    private final DisplayContent mDisplayContent;
    private boolean mFreezingTaskConfig;
    private int mImeHeight;
    private boolean mIsImeShowing;
    private float mMaxAspectRatio;
    private float mMinAspectRatio;
    private IPinnedTaskListener mPinnedTaskListener;
    private PictureInPictureSurfaceTransaction mPipTransaction;
    private final WindowManagerService mService;
    private final PinnedTaskListenerDeathHandler mPinnedTaskListenerDeathHandler = new PinnedTaskListenerDeathHandler();
    private final Runnable mDeferOrientationTimeoutRunnable = new Runnable() { // from class: com.android.server.wm.PinnedTaskController$$ExternalSyntheticLambda0
        @Override // java.lang.Runnable
        public final void run() {
            PinnedTaskController.this.m8139lambda$new$0$comandroidserverwmPinnedTaskController();
        }
    };

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class PinnedTaskListenerDeathHandler implements IBinder.DeathRecipient {
        private PinnedTaskListenerDeathHandler() {
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (PinnedTaskController.this.mService.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    PinnedTaskController.this.mPinnedTaskListener = null;
                    PinnedTaskController.this.mFreezingTaskConfig = false;
                    PinnedTaskController.this.mDeferOrientationTimeoutRunnable.run();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PinnedTaskController(WindowManagerService service, DisplayContent displayContent) {
        this.mService = service;
        this.mDisplayContent = displayContent;
        reloadResources();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-wm-PinnedTaskController  reason: not valid java name */
    public /* synthetic */ void m8139lambda$new$0$comandroidserverwmPinnedTaskController() {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mDeferOrientationChanging) {
                    continueOrientationChange();
                    this.mService.mWindowPlacerLocked.requestTraversal();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPostDisplayConfigurationChanged() {
        reloadResources();
        this.mFreezingTaskConfig = false;
    }

    private void reloadResources() {
        Resources res = this.mService.mContext.getResources();
        this.mMinAspectRatio = res.getFloat(17105095);
        this.mMaxAspectRatio = res.getFloat(17105094);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerPinnedTaskListener(IPinnedTaskListener listener) {
        try {
            listener.asBinder().linkToDeath(this.mPinnedTaskListenerDeathHandler, 0);
            this.mPinnedTaskListener = listener;
            notifyImeVisibilityChanged(this.mIsImeShowing, this.mImeHeight);
            notifyMovementBoundsChanged(false);
        } catch (RemoteException e) {
            Log.e("WindowManager", "Failed to register pinned task listener", e);
        }
    }

    public boolean isValidPictureInPictureAspectRatio(float aspectRatio) {
        return Float.compare(this.mMinAspectRatio, aspectRatio) <= 0 && Float.compare(aspectRatio, this.mMaxAspectRatio) <= 0;
    }

    public boolean isValidExpandedPictureInPictureAspectRatio(float aspectRatio) {
        return Float.compare(this.mMinAspectRatio, aspectRatio) > 0 || Float.compare(aspectRatio, this.mMaxAspectRatio) > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void deferOrientationChangeForEnteringPipFromFullScreenIfNeeded() {
        int rotation;
        ActivityRecord topFullscreen = this.mDisplayContent.getActivity(new Predicate() { // from class: com.android.server.wm.PinnedTaskController$$ExternalSyntheticLambda1
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return PinnedTaskController.lambda$deferOrientationChangeForEnteringPipFromFullScreenIfNeeded$1((ActivityRecord) obj);
            }
        });
        if (topFullscreen == null || topFullscreen.hasFixedRotationTransform() || (rotation = this.mDisplayContent.rotationForActivityInDifferentOrientation(topFullscreen)) == -1) {
            return;
        }
        this.mDisplayContent.setFixedRotationLaunchingApp(topFullscreen, rotation);
        this.mDeferOrientationChanging = true;
        this.mService.mH.removeCallbacks(this.mDeferOrientationTimeoutRunnable);
        float animatorScale = Math.max(1.0f, this.mService.getCurrentAnimatorScale());
        this.mService.mH.postDelayed(this.mDeferOrientationTimeoutRunnable, (int) (1000.0f * animatorScale));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$deferOrientationChangeForEnteringPipFromFullScreenIfNeeded$1(ActivityRecord a) {
        return a.providesOrientation() && !a.getTask().inMultiWindowMode();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldDeferOrientationChange() {
        return this.mDeferOrientationChanging;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setEnterPipBounds(Rect bounds) {
        if (!this.mDeferOrientationChanging) {
            return;
        }
        this.mFreezingTaskConfig = true;
        this.mDestRotatedBounds = new Rect(bounds);
        if (!this.mDisplayContent.mTransitionController.isShellTransitionsEnabled()) {
            continueOrientationChange();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setEnterPipTransaction(PictureInPictureSurfaceTransaction tx) {
        this.mFreezingTaskConfig = true;
        this.mPipTransaction = tx;
    }

    private void continueOrientationChange() {
        this.mDeferOrientationChanging = false;
        this.mService.mH.removeCallbacks(this.mDeferOrientationTimeoutRunnable);
        WindowContainer<?> orientationSource = this.mDisplayContent.getLastOrientationSource();
        if (orientationSource != null && !orientationSource.isAppTransitioning()) {
            this.mDisplayContent.continueUpdateOrientationForDiffOrienLaunchingApp();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startSeamlessRotationIfNeeded(SurfaceControl.Transaction t, int oldRotation, int newRotation) {
        TaskDisplayArea taskArea;
        Task pinnedTask;
        Rect bounds = this.mDestRotatedBounds;
        PictureInPictureSurfaceTransaction pipTx = this.mPipTransaction;
        boolean emptyPipPositionTx = pipTx == null || pipTx.mPosition == null;
        if ((bounds == null && emptyPipPositionTx) || (pinnedTask = (taskArea = this.mDisplayContent.getDefaultTaskDisplayArea()).getRootPinnedTask()) == null) {
            return;
        }
        Rect sourceHintRect = null;
        this.mDestRotatedBounds = null;
        this.mPipTransaction = null;
        Rect areaBounds = taskArea.getBounds();
        if (!emptyPipPositionTx) {
            float dx = pipTx.mPosition.x;
            float dy = pipTx.mPosition.y;
            Matrix matrix = pipTx.getMatrix();
            if (pipTx.mRotation == 90.0f) {
                dx = pipTx.mPosition.y;
                dy = areaBounds.right - pipTx.mPosition.x;
                matrix.postRotate(-90.0f);
            } else if (pipTx.mRotation == -90.0f) {
                dx = areaBounds.bottom - pipTx.mPosition.y;
                dy = pipTx.mPosition.x;
                matrix.postRotate(90.0f);
            }
            matrix.postTranslate(dx, dy);
            SurfaceControl leash = pinnedTask.getSurfaceControl();
            t.setMatrix(leash, matrix, new float[9]);
            if (pipTx.hasCornerRadiusSet()) {
                t.setCornerRadius(leash, pipTx.mCornerRadius);
            }
            Slog.i("WindowManager", "Seamless rotation PiP tx=" + pipTx + " pos=" + dx + "," + dy);
            return;
        }
        PictureInPictureParams params = pinnedTask.getPictureInPictureParams();
        if (params != null && params.hasSourceBoundsHint()) {
            sourceHintRect = params.getSourceRectHint();
        }
        Slog.i("WindowManager", "Seamless rotation PiP bounds=" + bounds + " hintRect=" + sourceHintRect);
        int rotationDelta = RotationUtils.deltaRotation(oldRotation, newRotation);
        if (sourceHintRect != null && rotationDelta == 3) {
            if (pinnedTask.getDisplayCutoutInsets() != null) {
                int rotationBackDelta = RotationUtils.deltaRotation(newRotation, oldRotation);
                Rect displayCutoutInsets = RotationUtils.rotateInsets(Insets.of(pinnedTask.getDisplayCutoutInsets()), rotationBackDelta).toRect();
                sourceHintRect.offset(displayCutoutInsets.left, displayCutoutInsets.top);
            }
        }
        Rect contentBounds = (sourceHintRect == null || !areaBounds.contains(sourceHintRect)) ? areaBounds : sourceHintRect;
        int w = contentBounds.width();
        int h = contentBounds.height();
        float scale = w <= h ? bounds.width() / w : bounds.height() / h;
        int insetLeft = (int) (((contentBounds.left - areaBounds.left) * scale) + 0.5f);
        int insetTop = (int) (((contentBounds.top - areaBounds.top) * scale) + 0.5f);
        Matrix matrix2 = new Matrix();
        matrix2.setScale(scale, scale);
        int insetLeft2 = bounds.top;
        matrix2.postTranslate(bounds.left - insetLeft, insetLeft2 - insetTop);
        t.setMatrix(pinnedTask.getSurfaceControl(), matrix2, new float[9]);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFreezingTaskConfig(Task task) {
        return this.mFreezingTaskConfig && task == this.mDisplayContent.getDefaultTaskDisplayArea().getRootPinnedTask();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onCancelFixedRotationTransform() {
        this.mFreezingTaskConfig = false;
        this.mDeferOrientationChanging = false;
        this.mDestRotatedBounds = null;
        this.mPipTransaction = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onActivityHidden(ComponentName componentName) {
        IPinnedTaskListener iPinnedTaskListener = this.mPinnedTaskListener;
        if (iPinnedTaskListener == null) {
            return;
        }
        try {
            iPinnedTaskListener.onActivityHidden(componentName);
        } catch (RemoteException e) {
            Slog.e("WindowManager", "Error delivering reset reentry fraction event.", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAdjustedForIme(boolean adjustedForIme, int imeHeight) {
        boolean imeShowing = adjustedForIme && imeHeight > 0;
        int imeHeight2 = imeShowing ? imeHeight : 0;
        if (imeShowing == this.mIsImeShowing && imeHeight2 == this.mImeHeight) {
            return;
        }
        this.mIsImeShowing = imeShowing;
        this.mImeHeight = imeHeight2;
        notifyImeVisibilityChanged(imeShowing, imeHeight2);
        notifyMovementBoundsChanged(true);
    }

    private void notifyImeVisibilityChanged(boolean imeVisible, int imeHeight) {
        IPinnedTaskListener iPinnedTaskListener = this.mPinnedTaskListener;
        if (iPinnedTaskListener != null) {
            try {
                iPinnedTaskListener.onImeVisibilityChanged(imeVisible, imeHeight);
            } catch (RemoteException e) {
                Slog.e("WindowManager", "Error delivering bounds changed event.", e);
            }
        }
    }

    private void notifyMovementBoundsChanged(boolean fromImeAdjustment) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                IPinnedTaskListener iPinnedTaskListener = this.mPinnedTaskListener;
                if (iPinnedTaskListener == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                try {
                    iPinnedTaskListener.onMovementBoundsChanged(fromImeAdjustment);
                } catch (RemoteException e) {
                    Slog.e("WindowManager", "Error delivering actions changed event.", e);
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(String prefix, PrintWriter pw) {
        pw.println(prefix + "PinnedTaskController");
        if (this.mDeferOrientationChanging) {
            pw.println(prefix + "  mDeferOrientationChanging=true");
        }
        if (this.mFreezingTaskConfig) {
            pw.println(prefix + "  mFreezingTaskConfig=true");
        }
        if (this.mDestRotatedBounds != null) {
            pw.println(prefix + "  mPendingBounds=" + this.mDestRotatedBounds);
        }
        if (this.mPipTransaction != null) {
            pw.println(prefix + "  mPipTransaction=" + this.mPipTransaction);
        }
        pw.println(prefix + "  mIsImeShowing=" + this.mIsImeShowing);
        pw.println(prefix + "  mImeHeight=" + this.mImeHeight);
        pw.println(prefix + "  mMinAspectRatio=" + this.mMinAspectRatio);
        pw.println(prefix + "  mMaxAspectRatio=" + this.mMaxAspectRatio);
    }
}
