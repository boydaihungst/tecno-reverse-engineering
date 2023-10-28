package com.android.server.wm;

import android.content.ClipData;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Slog;
import android.view.Display;
import android.view.IWindow;
import android.view.SurfaceControl;
import android.view.accessibility.AccessibilityManager;
import com.android.server.wm.DragState;
import com.android.server.wm.IDragLice;
import com.android.server.wm.WindowManagerInternal;
import com.mediatek.server.wm.WmsExt;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class DragDropController {
    private static final int A11Y_DRAG_TIMEOUT_DEFAULT_MS = 60000;
    private static final float DRAG_SHADOW_ALPHA_TRANSPARENT = 0.7071f;
    static final long DRAG_TIMEOUT_MS = 5000;
    static final int MSG_ANIMATION_END = 2;
    static final int MSG_DRAG_END_TIMEOUT = 0;
    static final int MSG_REMOVE_DRAG_SURFACE_TIMEOUT = 3;
    static final int MSG_TEAR_DOWN_DRAG_AND_DROP_INPUT = 1;
    private AtomicReference<WindowManagerInternal.IDragDropCallback> mCallback = new AtomicReference<>(new WindowManagerInternal.IDragDropCallback() { // from class: com.android.server.wm.DragDropController.1
    });
    private DragState mDragState;
    private final Handler mHandler;
    private WindowManagerService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public DragDropController(WindowManagerService service, Looper looper) {
        this.mService = service;
        this.mHandler = new DragHandler(service, looper);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean dragDropActiveLocked() {
        DragState dragState = this.mDragState;
        return (dragState == null || dragState.isClosing()) ? false : true;
    }

    boolean dragSurfaceRelinquishedToDropTarget() {
        DragState dragState = this.mDragState;
        return dragState != null && dragState.mRelinquishDragSurfaceToDropTarget;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerCallback(WindowManagerInternal.IDragDropCallback callback) {
        Objects.requireNonNull(callback);
        this.mCallback.set(callback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendDragStartedIfNeededLocked(WindowState window) {
        this.mDragState.sendDragStartedIfNeededLocked(window);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [219=10, 220=7, 222=7, 223=7, 226=6, 229=8] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:160:0x0351 A[Catch: all -> 0x0365, TRY_ENTER, TryCatch #2 {all -> 0x0365, blocks: (B:169:0x0366, B:160:0x0351, B:161:0x0354, B:163:0x0358, B:165:0x035e, B:167:0x0364), top: B:182:0x008b }] */
    /* JADX WARN: Removed duplicated region for block: B:163:0x0358 A[Catch: all -> 0x0365, TryCatch #2 {all -> 0x0365, blocks: (B:169:0x0366, B:160:0x0351, B:161:0x0354, B:163:0x0358, B:165:0x035e, B:167:0x0364), top: B:182:0x008b }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public IBinder performDrag(int callerPid, int callerUid, IWindow window, int flags, SurfaceControl surface, int touchSource, float touchX, float touchY, float thumbCenterX, float thumbCenterY, ClipData data) {
        SurfaceControl surface2;
        DragState dragState;
        IBinder token;
        if (WindowManagerDebugConfig.DEBUG_DRAG) {
            Slog.d(WmsExt.TAG, "perform drag: win=" + window + " surface=" + surface + " flags=" + Integer.toHexString(flags) + " data=" + data);
        }
        if (IDragLice.Instance().isSupportedUpdateClipData(flags)) {
            return IDragLice.Instance().updateClipData(flags, data, new IDragLice.IUpdateClipDataCallback() { // from class: com.android.server.wm.DragDropController.2
                @Override // com.android.server.wm.IDragLice.IUpdateClipDataCallback
                public void updateClipData(ClipData data2) {
                    synchronized (DragDropController.this.mService.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (DragDropController.this.dragDropActiveLocked()) {
                                Slog.v(WmsExt.TAG, "updateClipData data " + (data2 != null ? data2.toString() : null));
                                DragDropController.this.mDragState.mData = data2;
                            }
                        } catch (Throwable th) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            });
        }
        IBinder dragToken = new Binder();
        boolean callbackResult = this.mCallback.get().prePerformDrag(window, dragToken, touchSource, touchX, touchY, thumbCenterX, thumbCenterY, data);
        try {
            try {
                synchronized (this.mService.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        try {
                            try {
                                if (!callbackResult) {
                                    Slog.w(WmsExt.TAG, "IDragDropCallback rejects the performDrag request");
                                    if (surface != null) {
                                        surface.release();
                                    }
                                    DragState dragState2 = this.mDragState;
                                    if (dragState2 != null && !dragState2.isInProgress()) {
                                        this.mDragState.closeLocked();
                                    }
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    this.mCallback.get().postPerformDrag();
                                    return null;
                                } else if (dragDropActiveLocked()) {
                                    Slog.w(WmsExt.TAG, "Drag already in progress");
                                    if (surface != null) {
                                        surface.release();
                                    }
                                    DragState dragState3 = this.mDragState;
                                    if (dragState3 != null && !dragState3.isInProgress()) {
                                        this.mDragState.closeLocked();
                                    }
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    this.mCallback.get().postPerformDrag();
                                    return null;
                                } else {
                                    WindowState callingWin = this.mService.windowForClientLocked((Session) null, window, false);
                                    if (callingWin != null) {
                                        try {
                                            if (callingWin.canReceiveTouchInput()) {
                                                DisplayContent displayContent = callingWin.getDisplayContent();
                                                if (displayContent == null) {
                                                    Slog.w(WmsExt.TAG, "display content is null");
                                                    if (surface != null) {
                                                        surface.release();
                                                    }
                                                    DragState dragState4 = this.mDragState;
                                                    if (dragState4 != null && !dragState4.isInProgress()) {
                                                        this.mDragState.closeLocked();
                                                    }
                                                    WindowManagerService.resetPriorityAfterLockedSection();
                                                    this.mCallback.get().postPerformDrag();
                                                    return null;
                                                }
                                                float alpha = (flags & 512) == 0 ? DRAG_SHADOW_ALPHA_TRANSPARENT : 1.0f;
                                                IBinder winBinder = window.asBinder();
                                                try {
                                                    token = new Binder();
                                                    try {
                                                    } catch (Throwable th) {
                                                        th = th;
                                                    }
                                                } catch (Throwable th2) {
                                                    th = th2;
                                                    surface2 = surface;
                                                }
                                                try {
                                                    DragState dragState5 = new DragState(this.mService, this, token, surface, flags, winBinder);
                                                    this.mDragState = dragState5;
                                                    surface2 = null;
                                                    try {
                                                        dragState5.mPid = callerPid;
                                                        try {
                                                            this.mDragState.mUid = callerUid;
                                                            this.mDragState.mOriginalAlpha = alpha;
                                                            this.mDragState.mToken = dragToken;
                                                            this.mDragState.mDisplayContent = displayContent;
                                                            this.mDragState.mData = data;
                                                            try {
                                                                if ((flags & 1024) == 0) {
                                                                    Display display = displayContent.getDisplay();
                                                                    if (!this.mCallback.get().registerInputChannel(this.mDragState, display, this.mService.mInputManager, callingWin.mInputChannel)) {
                                                                        try {
                                                                            Slog.e(WmsExt.TAG, "Unable to transfer touch focus");
                                                                            if (0 != 0) {
                                                                                try {
                                                                                    surface2.release();
                                                                                } catch (Throwable th3) {
                                                                                    th = th3;
                                                                                    try {
                                                                                        WindowManagerService.resetPriorityAfterLockedSection();
                                                                                        throw th;
                                                                                    } catch (Throwable th4) {
                                                                                        th = th4;
                                                                                        this.mCallback.get().postPerformDrag();
                                                                                        throw th;
                                                                                    }
                                                                                }
                                                                            }
                                                                            DragState dragState6 = this.mDragState;
                                                                            if (dragState6 != null && !dragState6.isInProgress()) {
                                                                                this.mDragState.closeLocked();
                                                                            }
                                                                            WindowManagerService.resetPriorityAfterLockedSection();
                                                                            this.mCallback.get().postPerformDrag();
                                                                            return null;
                                                                        } catch (Throwable th5) {
                                                                            th = th5;
                                                                            if (surface2 != null) {
                                                                            }
                                                                            dragState = this.mDragState;
                                                                            if (dragState != null) {
                                                                            }
                                                                            throw th;
                                                                        }
                                                                    }
                                                                    SurfaceControl surfaceControl = this.mDragState.mSurfaceControl;
                                                                    this.mDragState.broadcastDragStartedLocked(touchX, touchY);
                                                                    this.mDragState.overridePointerIconLocked(touchSource);
                                                                    this.mDragState.mThumbOffsetX = thumbCenterX;
                                                                    this.mDragState.mThumbOffsetY = thumbCenterY;
                                                                    if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                                                                        Slog.i(WmsExt.TAG, ">>> OPEN TRANSACTION performDrag");
                                                                    }
                                                                    SurfaceControl.Transaction transaction = this.mDragState.mTransaction;
                                                                    transaction.setAlpha(surfaceControl, this.mDragState.mOriginalAlpha);
                                                                    transaction.show(surfaceControl);
                                                                    displayContent.reparentToOverlay(transaction, surfaceControl);
                                                                    this.mDragState.updateDragSurfaceLocked(true, touchX, touchY);
                                                                    if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                                                                        Slog.i(WmsExt.TAG, "<<< CLOSE TRANSACTION performDrag");
                                                                    }
                                                                } else {
                                                                    this.mDragState.broadcastDragStartedLocked(touchX, touchY);
                                                                    sendTimeoutMessage(0, callingWin.mClient.asBinder(), getAccessibilityManager().getRecommendedTimeoutMillis(60000, 4));
                                                                }
                                                                if (0 != 0) {
                                                                    try {
                                                                        surface2.release();
                                                                    } catch (Throwable th6) {
                                                                        th = th6;
                                                                        WindowManagerService.resetPriorityAfterLockedSection();
                                                                        throw th;
                                                                    }
                                                                }
                                                                DragState dragState7 = this.mDragState;
                                                                if (dragState7 != null && !dragState7.isInProgress()) {
                                                                    this.mDragState.closeLocked();
                                                                }
                                                                try {
                                                                    WindowManagerService.resetPriorityAfterLockedSection();
                                                                    this.mCallback.get().postPerformDrag();
                                                                    return dragToken;
                                                                } catch (Throwable th7) {
                                                                    th = th7;
                                                                    this.mCallback.get().postPerformDrag();
                                                                    throw th;
                                                                }
                                                            } catch (Throwable th8) {
                                                                th = th8;
                                                                if (surface2 != null) {
                                                                    surface2.release();
                                                                }
                                                                dragState = this.mDragState;
                                                                if (dragState != null && !dragState.isInProgress()) {
                                                                    this.mDragState.closeLocked();
                                                                }
                                                                throw th;
                                                            }
                                                        } catch (Throwable th9) {
                                                            th = th9;
                                                            if (surface2 != null) {
                                                            }
                                                            dragState = this.mDragState;
                                                            if (dragState != null) {
                                                                this.mDragState.closeLocked();
                                                            }
                                                            throw th;
                                                        }
                                                    } catch (Throwable th10) {
                                                        th = th10;
                                                    }
                                                } catch (Throwable th11) {
                                                    th = th11;
                                                    surface2 = surface;
                                                    if (surface2 != null) {
                                                    }
                                                    dragState = this.mDragState;
                                                    if (dragState != null) {
                                                    }
                                                    throw th;
                                                }
                                            }
                                        } catch (Throwable th12) {
                                            th = th12;
                                            surface2 = surface;
                                            if (surface2 != null) {
                                            }
                                            dragState = this.mDragState;
                                            if (dragState != null) {
                                            }
                                            throw th;
                                        }
                                    }
                                    try {
                                        try {
                                            Slog.w(WmsExt.TAG, "Bad requesting window " + window);
                                            if (surface != null) {
                                                try {
                                                    surface.release();
                                                } catch (Throwable th13) {
                                                    th = th13;
                                                    WindowManagerService.resetPriorityAfterLockedSection();
                                                    throw th;
                                                }
                                            }
                                            DragState dragState8 = this.mDragState;
                                            if (dragState8 != null && !dragState8.isInProgress()) {
                                                this.mDragState.closeLocked();
                                            }
                                            WindowManagerService.resetPriorityAfterLockedSection();
                                            this.mCallback.get().postPerformDrag();
                                            return null;
                                        } catch (Throwable th14) {
                                            th = th14;
                                            surface2 = surface;
                                            if (surface2 != null) {
                                            }
                                            dragState = this.mDragState;
                                            if (dragState != null) {
                                            }
                                            throw th;
                                        }
                                    } catch (Throwable th15) {
                                        th = th15;
                                        surface2 = surface;
                                        if (surface2 != null) {
                                        }
                                        dragState = this.mDragState;
                                        if (dragState != null) {
                                        }
                                        throw th;
                                    }
                                }
                            } catch (Throwable th16) {
                                th = th16;
                                surface2 = surface;
                            }
                        } catch (Throwable th17) {
                            th = th17;
                        }
                    } catch (Throwable th18) {
                        th = th18;
                    }
                }
            } catch (Throwable th19) {
                th = th19;
            }
        } catch (Throwable th20) {
            th = th20;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [272=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public void reportDropResult(IWindow window, boolean consumed) {
        AtomicReference<WindowManagerInternal.IDragDropCallback> atomicReference;
        WindowManagerInternal.IDragDropCallback iDragDropCallback;
        WindowManagerInternal.IDragDropCallback iDragDropCallback2;
        IBinder token = window.asBinder();
        if (WindowManagerDebugConfig.DEBUG_DRAG) {
            Slog.d(WmsExt.TAG, "Drop result=" + consumed + " reported by " + token);
        }
        this.mCallback.get().preReportDropResult(window, consumed);
        try {
            synchronized (this.mService.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                DragState dragState = this.mDragState;
                if (dragState == null) {
                    Slog.w(WmsExt.TAG, "Drop result given but no drag in progress");
                    WindowManagerService.resetPriorityAfterLockedSection();
                } else if (dragState.mToken != token) {
                    Slog.w(WmsExt.TAG, "Invalid drop-result claim by " + window);
                    throw new IllegalStateException("reportDropResult() by non-recipient");
                } else {
                    boolean z = false;
                    this.mHandler.removeMessages(0, window.asBinder());
                    WindowState callingWin = this.mService.windowForClientLocked((Session) null, window, false);
                    if (callingWin == null) {
                        Slog.w(WmsExt.TAG, "Bad result-reporting window " + window);
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    this.mDragState.mDragResult = this.mCallback.get().getDropConsumedState(consumed);
                    DragState dragState2 = this.mDragState;
                    if (consumed && dragState2.targetInterceptsGlobalDrag(callingWin)) {
                        z = true;
                    }
                    dragState2.mRelinquishDragSurfaceToDropTarget = z;
                    this.mDragState.endDragLocked();
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        } finally {
            this.mCallback.get().postReportDropResult();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancelDragAndDrop(IBinder dragToken, boolean skipAnimation) {
        if (WindowManagerDebugConfig.DEBUG_DRAG) {
            Slog.d(WmsExt.TAG, "cancelDragAndDrop");
        }
        this.mCallback.get().preCancelDragAndDrop(dragToken);
        try {
            synchronized (this.mService.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                DragState dragState = this.mDragState;
                if (dragState == null) {
                    Slog.w(WmsExt.TAG, "cancelDragAndDrop() without prepareDrag()");
                    throw new IllegalStateException("cancelDragAndDrop() without prepareDrag()");
                } else if (dragState.mToken != dragToken) {
                    Slog.w(WmsExt.TAG, "cancelDragAndDrop() does not match prepareDrag()");
                    throw new IllegalStateException("cancelDragAndDrop() does not match prepareDrag()");
                } else {
                    this.mDragState.mDragResult = false;
                    this.mDragState.cancelDragLocked(skipAnimation);
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            this.mCallback.get().postCancelDragAndDrop();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleMotionEvent(boolean keepHandling, float newX, float newY) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (!dragDropActiveLocked()) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                this.mDragState.updateDragSurfaceLocked(keepHandling, newX, newY);
                WindowManagerService.resetPriorityAfterLockedSection();
                this.mCallback.get().handleMotionEvent(newX, newY);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dragRecipientEntered(IWindow window) {
        if (WindowManagerDebugConfig.DEBUG_DRAG) {
            Slog.d(WmsExt.TAG, "Drag into new candidate view @ " + window.asBinder());
        }
        this.mCallback.get().dragRecipientEntered(window);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dragRecipientExited(IWindow window) {
        if (WindowManagerDebugConfig.DEBUG_DRAG) {
            Slog.d(WmsExt.TAG, "Drag from old candidate view @ " + window.asBinder());
        }
        this.mCallback.get().dragRecipientExited(window);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendHandlerMessage(int what, Object arg) {
        this.mHandler.obtainMessage(what, arg).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendTimeoutMessage(int what, Object arg, long timeoutMs) {
        this.mHandler.removeMessages(what, arg);
        Message msg = this.mHandler.obtainMessage(what, arg);
        this.mHandler.sendMessageDelayed(msg, timeoutMs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDragStateClosedLocked(DragState dragState) {
        if (this.mDragState != dragState) {
            Slog.wtf(WmsExt.TAG, "Unknown drag state is closed");
        } else {
            this.mDragState = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reportDropWindow(IBinder token, float x, float y) {
        if (this.mDragState == null) {
            Slog.w(WmsExt.TAG, "Drag state is closed.");
            return;
        }
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mDragState.reportDropWindowLock(token, x, y);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean dropForAccessibility(IWindow window, float x, float y) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                boolean isA11yEnabled = getAccessibilityManager().isEnabled();
                if (!dragDropActiveLocked()) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } else if (!this.mDragState.isAccessibilityDragDrop() || !isA11yEnabled) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } else {
                    WindowState winState = this.mService.windowForClientLocked((Session) null, window, false);
                    if (!this.mDragState.isWindowNotified(winState)) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return false;
                    }
                    IBinder token = winState.mInputChannelToken;
                    boolean reportDropWindowLock = this.mDragState.reportDropWindowLock(token, x, y);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return reportDropWindowLock;
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    AccessibilityManager getAccessibilityManager() {
        return (AccessibilityManager) this.mService.mContext.getSystemService("accessibility");
    }

    /* loaded from: classes2.dex */
    private class DragHandler extends Handler {
        private final WindowManagerService mService;

        DragHandler(WindowManagerService service, Looper looper) {
            super(looper);
            this.mService = service;
        }

        /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    IBinder win = (IBinder) msg.obj;
                    if (WindowManagerDebugConfig.DEBUG_DRAG) {
                        Slog.w(WmsExt.TAG, "Timeout ending drag to win " + win);
                    }
                    synchronized (this.mService.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (DragDropController.this.mDragState != null) {
                                DragDropController.this.mDragState.mDragResult = false;
                                DragDropController.this.mDragState.endDragLocked();
                            }
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                case 1:
                    if (WindowManagerDebugConfig.DEBUG_DRAG) {
                        Slog.d(WmsExt.TAG, "Drag ending; tearing down input channel");
                    }
                    DragState.InputInterceptor interceptor = (DragState.InputInterceptor) msg.obj;
                    if (interceptor == null) {
                        return;
                    }
                    synchronized (this.mService.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            interceptor.tearDown();
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                case 2:
                    synchronized (this.mService.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (DragDropController.this.mDragState == null) {
                                Slog.wtf(WmsExt.TAG, "mDragState unexpectedly became null while playing animation");
                                return;
                            }
                            DragDropController.this.mDragState.closeLocked();
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                case 3:
                    synchronized (this.mService.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            this.mService.mTransactionFactory.get().reparent((SurfaceControl) msg.obj, null).apply();
                        } finally {
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                default:
                    return;
            }
        }
    }

    public void onDump(PrintWriter pw) {
        this.mCallback.get().onDump(pw);
    }
}
