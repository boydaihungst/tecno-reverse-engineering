package com.android.server.wm;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Slog;
import android.view.Display;
import android.view.IWindow;
import android.view.InputWindowHandle;
import android.view.SurfaceControl;
import com.mediatek.server.wm.WmsExt;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class TaskPositioningController {
    private SurfaceControl mInputSurface;
    private DisplayContent mPositioningDisplay;
    private final WindowManagerService mService;
    private TaskPositioner mTaskPositioner;
    private final Rect mTmpClipRect = new Rect();
    final SurfaceControl.Transaction mTransaction;

    boolean isPositioningLocked() {
        return this.mTaskPositioner != null;
    }

    InputWindowHandle getDragWindowHandleLocked() {
        TaskPositioner taskPositioner = this.mTaskPositioner;
        if (taskPositioner != null) {
            return taskPositioner.mDragWindowHandle;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskPositioningController(WindowManagerService service) {
        this.mService = service;
        this.mTransaction = service.mTransactionFactory.get();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void hideInputSurface(int displayId) {
        SurfaceControl surfaceControl;
        DisplayContent displayContent = this.mPositioningDisplay;
        if (displayContent != null && displayContent.getDisplayId() == displayId && (surfaceControl = this.mInputSurface) != null) {
            this.mTransaction.hide(surfaceControl);
            this.mTransaction.syncInputWindows().apply();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void showInputSurface(int displayId) {
        DisplayContent displayContent = this.mPositioningDisplay;
        if (displayContent == null || displayContent.getDisplayId() != displayId) {
            return;
        }
        DisplayContent dc = this.mService.mRoot.getDisplayContent(displayId);
        if (this.mInputSurface == null) {
            this.mInputSurface = this.mService.makeSurfaceBuilder(dc.getSession()).setContainerLayer().setName("Drag and Drop Input Consumer").setCallsite("TaskPositioningController.showInputSurface").setParent(dc.getOverlayLayer()).build();
        }
        InputWindowHandle h = getDragWindowHandleLocked();
        if (h == null) {
            Slog.w(WmsExt.TAG, "Drag is in progress but there is no drag window handle.");
            return;
        }
        Display display = dc.getDisplay();
        Point p = new Point();
        display.getRealSize(p);
        this.mTmpClipRect.set(0, 0, p.x, p.y);
        this.mTransaction.show(this.mInputSurface).setInputWindowInfo(this.mInputSurface, h).setLayer(this.mInputSurface, Integer.MAX_VALUE).setPosition(this.mInputSurface, 0.0f, 0.0f).setCrop(this.mInputSurface, this.mTmpClipRect).syncInputWindows().apply();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean startMovingTask(IWindow window, float startX, float startY) {
        synchronized (this.mService.mGlobalLock) {
            try {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState win = this.mService.windowForClientLocked((Session) null, window, false);
                    if (!startPositioningLocked(win, false, false, startX, startY)) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return false;
                    }
                    this.mService.mAtmService.setFocusedTask(win.getTask().mTaskId);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return true;
                } catch (Throwable th) {
                    th = th;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleTapOutsideTask(final DisplayContent displayContent, final int x, final int y) {
        this.mService.mH.post(new Runnable() { // from class: com.android.server.wm.TaskPositioningController$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                TaskPositioningController.this.m8383x999b046d(displayContent, x, y);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleTapOutsideTask$0$com-android-server-wm-TaskPositioningController  reason: not valid java name */
    public /* synthetic */ void m8383x999b046d(DisplayContent displayContent, int x, int y) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Task task = displayContent.findTaskForResizePoint(x, y);
                if (task != null) {
                    if (!task.isResizeable()) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    } else if (!startPositioningLocked(task.getTopVisibleAppMainWindow(), true, task.preserveOrientationOnResize(), x, y)) {
                        ITranWindowManagerService.Instance().onAfterStartTaskPositioningLocked(this, false);
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    } else {
                        ITranWindowManagerService.Instance().onAfterStartTaskPositioningLocked(this, true);
                        this.mService.mAtmService.setFocusedTask(task.mTaskId);
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private boolean startPositioningLocked(WindowState win, boolean resize, boolean preserveOrientation, float startX, float startY) {
        if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
            Slog.d(WmsExt.TAG, "startPositioningLocked: win=" + win + ", resize=" + resize + ", preserveOrientation=" + preserveOrientation + ", {" + startX + ", " + startY + "}");
        }
        if (win == null || win.mActivityRecord == null) {
            Slog.w(WmsExt.TAG, "startPositioningLocked: Bad window " + win);
            return false;
        } else if (win.mInputChannel == null) {
            Slog.wtf(WmsExt.TAG, "startPositioningLocked: " + win + " has no input channel,  probably being removed");
            return false;
        } else {
            DisplayContent displayContent = win.getDisplayContent();
            if (displayContent == null) {
                Slog.w(WmsExt.TAG, "startPositioningLocked: Invalid display content " + win);
                return false;
            }
            this.mPositioningDisplay = displayContent;
            TaskPositioner create = TaskPositioner.create(this.mService);
            this.mTaskPositioner = create;
            create.register(displayContent, win);
            WindowState transferFocusFromWin = win;
            if (displayContent.mCurrentFocus != null && displayContent.mCurrentFocus != win && displayContent.mCurrentFocus.mActivityRecord == win.mActivityRecord) {
                transferFocusFromWin = displayContent.mCurrentFocus;
            }
            if (!this.mService.mInputManager.transferTouchFocus(transferFocusFromWin.mInputChannel, this.mTaskPositioner.mClientChannel, false)) {
                Slog.e(WmsExt.TAG, "startPositioningLocked: Unable to transfer touch focus");
                cleanUpTaskPositioner();
                return false;
            }
            this.mTaskPositioner.startDrag(resize, preserveOrientation, startX, startY);
            return true;
        }
    }

    public void finishTaskPositioning(IWindow window) {
        TaskPositioner taskPositioner = this.mTaskPositioner;
        if (taskPositioner != null && taskPositioner.mClientCallback == window.asBinder()) {
            finishTaskPositioning();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishTaskPositioning() {
        this.mService.mAnimationHandler.post(new Runnable() { // from class: com.android.server.wm.TaskPositioningController$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                TaskPositioningController.this.m8382xe485e366();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$finishTaskPositioning$1$com-android-server-wm-TaskPositioningController  reason: not valid java name */
    public /* synthetic */ void m8382xe485e366() {
        if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
            Slog.d(WmsExt.TAG, "finishPositioning");
        }
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                cleanUpTaskPositioner();
                this.mPositioningDisplay = null;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    private void cleanUpTaskPositioner() {
        TaskPositioner positioner = this.mTaskPositioner;
        if (positioner == null) {
            return;
        }
        this.mTaskPositioner = null;
        positioner.unregister();
    }
}
