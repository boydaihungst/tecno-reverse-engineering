package com.android.server.wm;

import android.graphics.PointF;
import android.os.Debug;
import android.os.IBinder;
import android.util.Slog;
import android.view.InputApplicationHandle;
import android.view.KeyEvent;
import android.view.SurfaceControl;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.input.InputManagerService;
import com.android.server.wm.WindowManagerService;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class InputManagerCallback implements InputManagerService.WindowManagerCallbacks {
    private static final String TAG = "WindowManager";
    private boolean mInputDevicesReady;
    private boolean mInputDispatchEnabled;
    private boolean mInputDispatchFrozen;
    private final WindowManagerService mService;
    private final Object mInputDevicesReadyMonitor = new Object();
    private String mInputFreezeReason = null;

    public InputManagerCallback(WindowManagerService service) {
        this.mService = service;
    }

    @Override // com.android.server.input.InputManagerService.WindowManagerCallbacks
    public void notifyInputChannelBroken(IBinder token) {
        if (token == null) {
            return;
        }
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                WindowState windowState = this.mService.mInputToWindowMap.get(token);
                if (windowState != null) {
                    Slog.i("WindowManager", "WINDOW DIED " + windowState);
                    windowState.removeIfPossible();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    @Override // com.android.server.input.InputManagerService.WindowManagerCallbacks
    public void notifyNoFocusedWindowAnr(InputApplicationHandle applicationHandle) {
        this.mService.mAnrController.notifyAppUnresponsive(applicationHandle, "Application does not have a focused window");
    }

    @Override // com.android.server.input.InputManagerService.WindowManagerCallbacks
    public void notifyWindowUnresponsive(IBinder token, OptionalInt pid, String reason) {
        this.mService.mAnrController.notifyWindowUnresponsive(token, pid, reason);
    }

    @Override // com.android.server.input.InputManagerService.WindowManagerCallbacks
    public void notifyWindowResponsive(IBinder token, OptionalInt pid) {
        this.mService.mAnrController.notifyWindowResponsive(token, pid);
    }

    @Override // com.android.server.input.InputManagerService.WindowManagerCallbacks
    public void notifyConfigurationChanged() {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mService.mRoot.forAllDisplays(new Consumer() { // from class: com.android.server.wm.InputManagerCallback$$ExternalSyntheticLambda2
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((DisplayContent) obj).sendNewConfiguration();
                    }
                });
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        synchronized (this.mInputDevicesReadyMonitor) {
            if (!this.mInputDevicesReady) {
                this.mInputDevicesReady = true;
                this.mInputDevicesReadyMonitor.notifyAll();
            }
        }
    }

    public void notifyLidSwitchChanged(long whenNanos, boolean lidOpen) {
        this.mService.mPolicy.notifyLidSwitchChanged(whenNanos, lidOpen);
    }

    @Override // com.android.server.input.InputManagerService.WindowManagerCallbacks
    public void notifyCameraLensCoverSwitchChanged(long whenNanos, boolean lensCovered) {
        this.mService.mPolicy.notifyCameraLensCoverSwitchChanged(whenNanos, lensCovered);
    }

    @Override // com.android.server.input.InputManagerService.WindowManagerCallbacks
    public int interceptKeyBeforeQueueing(KeyEvent event, int policyFlags) {
        return this.mService.mPolicy.interceptKeyBeforeQueueing(event, policyFlags);
    }

    @Override // com.android.server.input.InputManagerService.WindowManagerCallbacks
    public int interceptMotionBeforeQueueingNonInteractive(int displayId, long whenNanos, int policyFlags) {
        return this.mService.mPolicy.interceptMotionBeforeQueueingNonInteractive(displayId, whenNanos, policyFlags);
    }

    @Override // com.android.server.input.InputManagerService.WindowManagerCallbacks
    public long interceptKeyBeforeDispatching(IBinder focusedToken, KeyEvent event, int policyFlags) {
        return this.mService.mPolicy.interceptKeyBeforeDispatching(focusedToken, event, policyFlags);
    }

    @Override // com.android.server.input.InputManagerService.WindowManagerCallbacks
    public KeyEvent dispatchUnhandledKey(IBinder focusedToken, KeyEvent event, int policyFlags) {
        return this.mService.mPolicy.dispatchUnhandledKey(focusedToken, event, policyFlags);
    }

    @Override // com.android.server.input.InputManagerService.WindowManagerCallbacks
    public int getPointerLayer() {
        return (this.mService.mPolicy.getWindowLayerFromTypeLw(2018) * 10000) + 1000;
    }

    @Override // com.android.server.input.InputManagerService.WindowManagerCallbacks
    public int getPointerDisplayId() {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (!this.mService.mForceDesktopModeOnExternalDisplays) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return 0;
                }
                int firstExternalDisplayId = 0;
                for (int i = this.mService.mRoot.mChildren.size() - 1; i >= 0; i--) {
                    DisplayContent displayContent = (DisplayContent) this.mService.mRoot.mChildren.get(i);
                    if (displayContent.getDisplayInfo().state != 1) {
                        if (displayContent.getWindowingMode() == 5) {
                            int displayId = displayContent.getDisplayId();
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return displayId;
                        } else if (firstExternalDisplayId == 0 && displayContent.getDisplayId() != 0) {
                            firstExternalDisplayId = displayContent.getDisplayId();
                        }
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return firstExternalDisplayId;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    @Override // com.android.server.input.InputManagerService.WindowManagerCallbacks
    public PointF getCursorPosition() {
        return this.mService.getLatestMousePosition();
    }

    @Override // com.android.server.input.InputManagerService.WindowManagerCallbacks
    public void onPointerDownOutsideFocus(IBinder touchedToken) {
        this.mService.mH.obtainMessage(62, touchedToken).sendToTarget();
    }

    @Override // com.android.server.input.InputManagerService.WindowManagerCallbacks
    public void notifyFocusChanged(IBinder oldToken, IBinder newToken) {
        WindowManagerService.H h = this.mService.mH;
        final WindowManagerService windowManagerService = this.mService;
        Objects.requireNonNull(windowManagerService);
        h.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.wm.InputManagerCallback$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                WindowManagerService.this.reportFocusChanged((IBinder) obj, (IBinder) obj2);
            }
        }, oldToken, newToken));
    }

    @Override // com.android.server.input.InputManagerService.WindowManagerCallbacks
    public void notifyDropWindow(IBinder token, float x, float y) {
        WindowManagerService.H h = this.mService.mH;
        final DragDropController dragDropController = this.mService.mDragDropController;
        Objects.requireNonNull(dragDropController);
        h.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.wm.InputManagerCallback$$ExternalSyntheticLambda1
            public final void accept(Object obj, Object obj2, Object obj3) {
                DragDropController.this.reportDropWindow((IBinder) obj, ((Float) obj2).floatValue(), ((Float) obj3).floatValue());
            }
        }, token, Float.valueOf(x), Float.valueOf(y)));
    }

    @Override // com.android.server.input.InputManagerService.WindowManagerCallbacks
    public SurfaceControl getParentSurfaceForPointers(int displayId) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent dc = this.mService.mRoot.getDisplayContent(displayId);
                if (dc == null) {
                    Slog.e("WindowManager", "Failed to get parent surface for pointers on display " + displayId + " - DisplayContent not found.");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return null;
                }
                SurfaceControl overlayLayer = dc.getOverlayLayer();
                WindowManagerService.resetPriorityAfterLockedSection();
                return overlayLayer;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    @Override // com.android.server.input.InputManagerService.WindowManagerCallbacks
    public SurfaceControl createSurfaceForGestureMonitor(String name, int displayId) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent dc = this.mService.mRoot.getDisplayContent(displayId);
                if (dc == null) {
                    Slog.e("WindowManager", "Failed to create a gesture monitor on display: " + displayId + " - DisplayContent not found.");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return null;
                }
                SurfaceControl build = this.mService.makeSurfaceBuilder(dc.getSession()).setContainerLayer().setName(name).setCallsite("createSurfaceForGestureMonitor").setParent(dc.getOverlayLayer()).build();
                WindowManagerService.resetPriorityAfterLockedSection();
                return build;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    @Override // com.android.server.input.InputManagerService.WindowManagerCallbacks
    public void notifyPointerDisplayIdChanged(int displayId, float x, float y) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mService.setMousePointerDisplayId(displayId);
                if (displayId == -1) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                DisplayContent dc = this.mService.mRoot.getDisplayContent(displayId);
                if (dc == null) {
                    Slog.wtf("WindowManager", "The mouse pointer was moved to display " + displayId + " that does not have a valid DisplayContent.");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                this.mService.restorePointerIconLocked(dc, x, y);
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public boolean waitForInputDevicesReady(long timeoutMillis) {
        boolean z;
        synchronized (this.mInputDevicesReadyMonitor) {
            if (!this.mInputDevicesReady) {
                try {
                    this.mInputDevicesReadyMonitor.wait(timeoutMillis);
                } catch (InterruptedException e) {
                }
            }
            z = this.mInputDevicesReady;
        }
        return z;
    }

    public void freezeInputDispatchingLw() {
        if (!this.mInputDispatchFrozen) {
            if (WindowManagerDebugConfig.DEBUG_INPUT) {
                Slog.v("WindowManager", "Freezing input dispatching");
            }
            this.mInputDispatchFrozen = true;
            if (WindowManagerDebugConfig.DEBUG_INPUT) {
                this.mInputFreezeReason = Debug.getCallers(6);
            }
            updateInputDispatchModeLw();
        }
    }

    public void thawInputDispatchingLw() {
        if (this.mInputDispatchFrozen) {
            if (WindowManagerDebugConfig.DEBUG_INPUT) {
                Slog.v("WindowManager", "Thawing input dispatching");
            }
            this.mInputDispatchFrozen = false;
            this.mInputFreezeReason = null;
            updateInputDispatchModeLw();
        }
    }

    public void setEventDispatchingLw(boolean enabled) {
        if (this.mInputDispatchEnabled != enabled) {
            if (WindowManagerDebugConfig.DEBUG_INPUT) {
                Slog.v("WindowManager", "Setting event dispatching to " + enabled);
            }
            this.mInputDispatchEnabled = enabled;
            updateInputDispatchModeLw();
        }
    }

    private void updateInputDispatchModeLw() {
        this.mService.mInputManager.setInputDispatchMode(this.mInputDispatchEnabled, this.mInputDispatchFrozen);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        if (this.mInputFreezeReason != null) {
            pw.println(prefix + "mInputFreezeReason=" + this.mInputFreezeReason);
        }
    }
}
