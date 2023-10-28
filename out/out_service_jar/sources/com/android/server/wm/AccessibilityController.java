package com.android.server.wm;

import android.accessibilityservice.AccessibilityTrace;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManagerInternal;
import android.graphics.BLASTBufferQueue;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.hardware.usb.gadget.V1_2.GadgetFunction;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import android.view.InsetsSource;
import android.view.MagnificationSpec;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.ViewConfiguration;
import android.view.WindowInfo;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.TraceBuffer;
import com.android.internal.util.function.QuintConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.LocalServices;
import com.android.server.notification.NotificationShellCmd;
import com.android.server.wm.AccessibilityController;
import com.android.server.wm.AccessibilityWindowsPopulator;
import com.android.server.wm.WindowManagerInternal;
import defpackage.CompanionAppsPermissions;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class AccessibilityController {
    private final AccessibilityControllerInternalImpl mAccessibilityTracing;
    private final AccessibilityWindowsPopulator mAccessibilityWindowsPopulator;
    private final WindowManagerService mService;
    private static final String TAG = AccessibilityController.class.getSimpleName();
    private static final Object STATIC_LOCK = new Object();
    private static final Rect EMPTY_RECT = new Rect();
    private static final float[] sTempFloats = new float[9];
    private final SparseArray<DisplayMagnifier> mDisplayMagnifiers = new SparseArray<>();
    private final SparseArray<WindowsForAccessibilityObserver> mWindowsForAccessibilityObserver = new SparseArray<>();
    private SparseArray<IBinder> mFocusedWindow = new SparseArray<>();
    private int mFocusedDisplay = -1;
    private final SparseBooleanArray mIsImeVisibleArray = new SparseBooleanArray();
    private boolean mAllObserversInitialized = true;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static AccessibilityControllerInternalImpl getAccessibilityControllerInternal(WindowManagerService service) {
        return AccessibilityControllerInternalImpl.getInstance(service);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AccessibilityController(WindowManagerService service) {
        this.mService = service;
        this.mAccessibilityTracing = getAccessibilityControllerInternal(service);
        this.mAccessibilityWindowsPopulator = new AccessibilityWindowsPopulator(service, this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setMagnificationCallbacks(int displayId, WindowManagerInternal.MagnificationCallbacks callbacks) {
        Display display;
        if (this.mAccessibilityTracing.isTracingEnabled(2048L)) {
            this.mAccessibilityTracing.logTrace(TAG + ".setMagnificationCallbacks", 2048L, "displayId=" + displayId + "; callbacks={" + callbacks + "}");
        }
        if (callbacks != null) {
            if (this.mDisplayMagnifiers.get(displayId) != null) {
                throw new IllegalStateException("Magnification callbacks already set!");
            }
            DisplayContent dc = this.mService.mRoot.getDisplayContent(displayId);
            if (dc == null || (display = dc.getDisplay()) == null || display.getType() == 4) {
                return false;
            }
            DisplayMagnifier magnifier = new DisplayMagnifier(this.mService, dc, display, callbacks);
            magnifier.notifyImeWindowVisibilityChanged(this.mIsImeVisibleArray.get(displayId, false));
            this.mDisplayMagnifiers.put(displayId, magnifier);
            return true;
        }
        DisplayMagnifier displayMagnifier = this.mDisplayMagnifiers.get(displayId);
        if (displayMagnifier == null) {
            throw new IllegalStateException("Magnification callbacks already cleared!");
        }
        displayMagnifier.destroy();
        this.mDisplayMagnifiers.remove(displayId);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWindowsForAccessibilityCallback(int displayId, WindowManagerInternal.WindowsForAccessibilityCallback callback) {
        if (this.mAccessibilityTracing.isTracingEnabled(GadgetFunction.NCM)) {
            this.mAccessibilityTracing.logTrace(TAG + ".setWindowsForAccessibilityCallback", GadgetFunction.NCM, "displayId=" + displayId + "; callback={" + callback + "}");
        }
        if (callback != null) {
            if (this.mWindowsForAccessibilityObserver.get(displayId) != null) {
                String errorMessage = "Windows for accessibility callback of display " + displayId + " already set!";
                Slog.e(TAG, errorMessage);
                if (Build.IS_DEBUGGABLE) {
                    throw new IllegalStateException(errorMessage);
                }
                this.mWindowsForAccessibilityObserver.remove(displayId);
            }
            this.mAccessibilityWindowsPopulator.setWindowsNotification(true);
            WindowsForAccessibilityObserver observer = new WindowsForAccessibilityObserver(this.mService, displayId, callback, this.mAccessibilityWindowsPopulator);
            this.mWindowsForAccessibilityObserver.put(displayId, observer);
            this.mAllObserversInitialized &= observer.mInitialized;
            return;
        }
        WindowsForAccessibilityObserver windowsForA11yObserver = this.mWindowsForAccessibilityObserver.get(displayId);
        if (windowsForA11yObserver == null) {
            String errorMessage2 = "Windows for accessibility callback of display " + displayId + " already cleared!";
            Slog.e(TAG, errorMessage2);
            if (Build.IS_DEBUGGABLE) {
                throw new IllegalStateException(errorMessage2);
            }
        }
        this.mWindowsForAccessibilityObserver.remove(displayId);
        if (this.mWindowsForAccessibilityObserver.size() <= 0) {
            this.mAccessibilityWindowsPopulator.setWindowsNotification(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void performComputeChangedWindowsNot(int displayId, boolean forceSend) {
        if (this.mAccessibilityTracing.isTracingEnabled(GadgetFunction.NCM)) {
            this.mAccessibilityTracing.logTrace(TAG + ".performComputeChangedWindowsNot", GadgetFunction.NCM, "displayId=" + displayId + "; forceSend=" + forceSend);
        }
        WindowsForAccessibilityObserver observer = null;
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                WindowsForAccessibilityObserver windowsForA11yObserver = this.mWindowsForAccessibilityObserver.get(displayId);
                if (windowsForA11yObserver != null) {
                    observer = windowsForA11yObserver;
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        if (observer != null) {
            observer.performComputeChangedWindows(forceSend);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setMagnificationSpec(int displayId, MagnificationSpec spec) {
        if (this.mAccessibilityTracing.isTracingEnabled(3072L)) {
            this.mAccessibilityTracing.logTrace(TAG + ".setMagnificationSpec", 3072L, "displayId=" + displayId + "; spec={" + spec + "}");
        }
        this.mAccessibilityWindowsPopulator.setMagnificationSpec(displayId, spec);
        DisplayMagnifier displayMagnifier = this.mDisplayMagnifiers.get(displayId);
        if (displayMagnifier != null) {
            displayMagnifier.setMagnificationSpec(spec);
        }
        WindowsForAccessibilityObserver windowsForA11yObserver = this.mWindowsForAccessibilityObserver.get(displayId);
        if (windowsForA11yObserver != null) {
            windowsForA11yObserver.scheduleComputeChangedWindows();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getMagnificationRegion(int displayId, Region outMagnificationRegion) {
        if (this.mAccessibilityTracing.isTracingEnabled(2048L)) {
            this.mAccessibilityTracing.logTrace(TAG + ".getMagnificationRegion", 2048L, "displayId=" + displayId + "; outMagnificationRegion={" + outMagnificationRegion + "}");
        }
        DisplayMagnifier displayMagnifier = this.mDisplayMagnifiers.get(displayId);
        if (displayMagnifier != null) {
            displayMagnifier.getMagnificationRegion(outMagnificationRegion);
        }
    }

    void onWindowLayersChanged(int displayId) {
        if (this.mAccessibilityTracing.isTracingEnabled(3072L)) {
            this.mAccessibilityTracing.logTrace(TAG + ".onWindowLayersChanged", 3072L, "displayId=" + displayId);
        }
        DisplayMagnifier displayMagnifier = this.mDisplayMagnifiers.get(displayId);
        if (displayMagnifier != null) {
            displayMagnifier.onWindowLayersChanged();
        }
        WindowsForAccessibilityObserver windowsForA11yObserver = this.mWindowsForAccessibilityObserver.get(displayId);
        if (windowsForA11yObserver != null) {
            windowsForA11yObserver.scheduleComputeChangedWindows();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDisplaySizeChanged(DisplayContent displayContent) {
        if (this.mAccessibilityTracing.isTracingEnabled(3072L)) {
            this.mAccessibilityTracing.logTrace(TAG + ".onRotationChanged", 3072L, "displayContent={" + displayContent + "}");
        }
        int displayId = displayContent.getDisplayId();
        DisplayMagnifier displayMagnifier = this.mDisplayMagnifiers.get(displayId);
        if (displayMagnifier != null) {
            displayMagnifier.onDisplaySizeChanged(displayContent);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAppWindowTransition(int displayId, int transition) {
        if (this.mAccessibilityTracing.isTracingEnabled(2048L)) {
            this.mAccessibilityTracing.logTrace(TAG + ".onAppWindowTransition", 2048L, "displayId=" + displayId + "; transition=" + transition);
        }
        DisplayMagnifier displayMagnifier = this.mDisplayMagnifiers.get(displayId);
        if (displayMagnifier != null) {
            displayMagnifier.onAppWindowTransition(displayId, transition);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onWindowTransition(WindowState windowState, int transition) {
        if (this.mAccessibilityTracing.isTracingEnabled(3072L)) {
            this.mAccessibilityTracing.logTrace(TAG + ".onWindowTransition", 3072L, "windowState={" + windowState + "}; transition=" + transition);
        }
        int displayId = windowState.getDisplayId();
        DisplayMagnifier displayMagnifier = this.mDisplayMagnifiers.get(displayId);
        if (displayMagnifier != null) {
            displayMagnifier.onWindowTransition(windowState, transition);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onWindowFocusChangedNot(int displayId) {
        if (this.mAccessibilityTracing.isTracingEnabled(GadgetFunction.NCM)) {
            this.mAccessibilityTracing.logTrace(TAG + ".onWindowFocusChangedNot", GadgetFunction.NCM, "displayId=" + displayId);
        }
        WindowsForAccessibilityObserver observer = null;
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                WindowsForAccessibilityObserver windowsForA11yObserver = this.mWindowsForAccessibilityObserver.get(displayId);
                if (windowsForA11yObserver != null) {
                    observer = windowsForA11yObserver;
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        if (observer != null) {
            observer.performComputeChangedWindows(false);
        }
        sendCallbackToUninitializedObserversIfNeeded();
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
    private void sendCallbackToUninitializedObserversIfNeeded() {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mAllObserversInitialized) {
                    return;
                }
                if (this.mService.mRoot.getTopFocusedDisplayContent().mCurrentFocus == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                List<WindowsForAccessibilityObserver> unInitializedObservers = new ArrayList<>();
                for (int i = this.mWindowsForAccessibilityObserver.size() - 1; i >= 0; i--) {
                    WindowsForAccessibilityObserver observer = this.mWindowsForAccessibilityObserver.valueAt(i);
                    if (!observer.mInitialized) {
                        unInitializedObservers.add(observer);
                    }
                }
                this.mAllObserversInitialized = true;
                WindowManagerService.resetPriorityAfterLockedSection();
                boolean areAllObserversInitialized = true;
                for (int i2 = unInitializedObservers.size() - 1; i2 >= 0; i2--) {
                    WindowsForAccessibilityObserver observer2 = unInitializedObservers.get(i2);
                    observer2.performComputeChangedWindows(true);
                    areAllObserversInitialized &= observer2.mInitialized;
                }
                synchronized (this.mService.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        this.mAllObserversInitialized &= areAllObserversInitialized;
                    } finally {
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSomeWindowResizedOrMoved(int... displayIds) {
        onSomeWindowResizedOrMovedWithCallingUid(Binder.getCallingUid(), displayIds);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSomeWindowResizedOrMovedWithCallingUid(int callingUid, int... displayIds) {
        if (this.mAccessibilityTracing.isTracingEnabled(GadgetFunction.NCM)) {
            this.mAccessibilityTracing.logTrace(TAG + ".onSomeWindowResizedOrMoved", GadgetFunction.NCM, "displayIds={" + displayIds.toString() + "}", "".getBytes(), callingUid);
        }
        for (int i : displayIds) {
            WindowsForAccessibilityObserver windowsForA11yObserver = this.mWindowsForAccessibilityObserver.get(i);
            if (windowsForA11yObserver != null) {
                windowsForA11yObserver.scheduleComputeChangedWindows();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void drawMagnifiedRegionBorderIfNeeded(int displayId, SurfaceControl.Transaction t) {
        if (this.mAccessibilityTracing.isTracingEnabled(2048L)) {
            this.mAccessibilityTracing.logTrace(TAG + ".drawMagnifiedRegionBorderIfNeeded", 2048L, "displayId=" + displayId + "; transaction={" + t + "}");
        }
        DisplayMagnifier displayMagnifier = this.mDisplayMagnifiers.get(displayId);
        if (displayMagnifier != null) {
            displayMagnifier.drawMagnifiedRegionBorderIfNeeded(t);
        }
    }

    public Pair<Matrix, MagnificationSpec> getWindowTransformationMatrixAndMagnificationSpec(IBinder token) {
        Pair<Matrix, MagnificationSpec> pair;
        MagnificationSpec otherMagnificationSpec;
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Matrix transformationMatrix = new Matrix();
                MagnificationSpec magnificationSpec = new MagnificationSpec();
                WindowState windowState = this.mService.mWindowMap.get(token);
                if (windowState != null) {
                    windowState.getTransformationMatrix(new float[9], transformationMatrix);
                    if (hasCallbacks() && (otherMagnificationSpec = getMagnificationSpecForWindow(windowState)) != null && !otherMagnificationSpec.isNop()) {
                        magnificationSpec.setTo(otherMagnificationSpec);
                    }
                }
                pair = new Pair<>(transformationMatrix, magnificationSpec);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return pair;
    }

    MagnificationSpec getMagnificationSpecForWindow(WindowState windowState) {
        if (this.mAccessibilityTracing.isTracingEnabled(2048L)) {
            this.mAccessibilityTracing.logTrace(TAG + ".getMagnificationSpecForWindow", 2048L, "windowState={" + windowState + "}");
        }
        int displayId = windowState.getDisplayId();
        DisplayMagnifier displayMagnifier = this.mDisplayMagnifiers.get(displayId);
        if (displayMagnifier != null) {
            return displayMagnifier.getMagnificationSpecForWindow(windowState);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasCallbacks() {
        if (this.mAccessibilityTracing.isTracingEnabled(3072L)) {
            this.mAccessibilityTracing.logTrace(TAG + ".hasCallbacks", 3072L);
        }
        return this.mDisplayMagnifiers.size() > 0 || this.mWindowsForAccessibilityObserver.size() > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setForceShowMagnifiableBounds(int displayId, boolean show) {
        if (this.mAccessibilityTracing.isTracingEnabled(2048L)) {
            this.mAccessibilityTracing.logTrace(TAG + ".setForceShowMagnifiableBounds", 2048L, "displayId=" + displayId + "; show=" + show);
        }
        DisplayMagnifier displayMagnifier = this.mDisplayMagnifiers.get(displayId);
        if (displayMagnifier != null) {
            displayMagnifier.setForceShowMagnifiableBounds(show);
            displayMagnifier.showMagnificationBoundsIfNeeded();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateImeVisibilityIfNeeded(int displayId, boolean shown) {
        if (this.mAccessibilityTracing.isTracingEnabled(2048L)) {
            this.mAccessibilityTracing.logTrace(TAG + ".updateImeVisibilityIfNeeded", 2048L, "displayId=" + displayId + ";shown=" + shown);
        }
        boolean isDisplayImeVisible = this.mIsImeVisibleArray.get(displayId, false);
        if (isDisplayImeVisible == shown) {
            return;
        }
        this.mIsImeVisibleArray.put(displayId, shown);
        DisplayMagnifier displayMagnifier = this.mDisplayMagnifiers.get(displayId);
        if (displayMagnifier != null) {
            displayMagnifier.notifyImeWindowVisibilityChanged(shown);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void populateTransformationMatrix(WindowState windowState, Matrix outMatrix) {
        windowState.getTransformationMatrix(sTempFloats, outMatrix);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        for (int i = 0; i < this.mDisplayMagnifiers.size(); i++) {
            DisplayMagnifier displayMagnifier = this.mDisplayMagnifiers.valueAt(i);
            if (displayMagnifier != null) {
                displayMagnifier.dump(pw, prefix + "Magnification display# " + this.mDisplayMagnifiers.keyAt(i));
            }
        }
        pw.println(prefix + "mWindowsForAccessibilityObserver=" + this.mWindowsForAccessibilityObserver);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onFocusChanged(InputTarget lastTarget, InputTarget newTarget) {
        if (lastTarget != null) {
            this.mFocusedWindow.remove(lastTarget.getDisplayId());
        }
        if (newTarget != null) {
            int displayId = newTarget.getDisplayId();
            IBinder clientBinder = newTarget.getIWindow().asBinder();
            this.mFocusedWindow.put(displayId, clientBinder);
        }
    }

    public void onDisplayRemoved(int displayId) {
        this.mIsImeVisibleArray.delete(displayId);
        this.mFocusedWindow.remove(displayId);
    }

    public void setFocusedDisplay(int focusedDisplayId) {
        this.mFocusedDisplay = focusedDisplayId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IBinder getFocusedWindowToken() {
        return this.mFocusedWindow.get(this.mFocusedDisplay);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class DisplayMagnifier {
        private static final boolean DEBUG_DISPLAY_SIZE = false;
        private static final boolean DEBUG_LAYERS = false;
        private static final boolean DEBUG_RECTANGLE_REQUESTED = false;
        private static final boolean DEBUG_VIEWPORT_WINDOW = false;
        private static final boolean DEBUG_WINDOW_TRANSITIONS = false;
        private static final String LOG_TAG = "WindowManager";
        private final AccessibilityControllerInternalImpl mAccessibilityTracing;
        private final WindowManagerInternal.MagnificationCallbacks mCallbacks;
        private final Display mDisplay;
        private final DisplayContent mDisplayContent;
        private final Context mDisplayContext;
        private final Handler mHandler;
        private final long mLongAnimationDuration;
        private final MagnifiedViewport mMagnifedViewport;
        private final WindowManagerService mService;
        private final Rect mTempRect1 = new Rect();
        private final Rect mTempRect2 = new Rect();
        private final Region mTempRegion1 = new Region();
        private final Region mTempRegion2 = new Region();
        private final Region mTempRegion3 = new Region();
        private final Region mTempRegion4 = new Region();
        private boolean mForceShowMagnifiableBounds = false;

        DisplayMagnifier(WindowManagerService windowManagerService, DisplayContent displayContent, Display display, WindowManagerInternal.MagnificationCallbacks callbacks) {
            Context createDisplayContext = windowManagerService.mContext.createDisplayContext(display);
            this.mDisplayContext = createDisplayContext;
            this.mService = windowManagerService;
            this.mCallbacks = callbacks;
            this.mDisplayContent = displayContent;
            this.mDisplay = display;
            this.mHandler = new MyHandler(windowManagerService.mH.getLooper());
            this.mMagnifedViewport = new MagnifiedViewport();
            AccessibilityControllerInternalImpl accessibilityControllerInternal = AccessibilityController.getAccessibilityControllerInternal(windowManagerService);
            this.mAccessibilityTracing = accessibilityControllerInternal;
            this.mLongAnimationDuration = createDisplayContext.getResources().getInteger(17694722);
            if (accessibilityControllerInternal.isTracingEnabled(2048L)) {
                accessibilityControllerInternal.logTrace("WindowManager.DisplayMagnifier.constructor", 2048L, "windowManagerService={" + windowManagerService + "}; displayContent={" + displayContent + "}; display={" + display + "}; callbacks={" + callbacks + "}");
            }
        }

        void setMagnificationSpec(MagnificationSpec spec) {
            if (this.mAccessibilityTracing.isTracingEnabled(2048L)) {
                this.mAccessibilityTracing.logTrace("WindowManager.setMagnificationSpec", 2048L, "spec={" + spec + "}");
            }
            this.mMagnifedViewport.updateMagnificationSpec(spec);
            this.mMagnifedViewport.recomputeBounds();
            this.mService.applyMagnificationSpecLocked(this.mDisplay.getDisplayId(), spec);
            this.mService.scheduleAnimationLocked();
        }

        void setForceShowMagnifiableBounds(boolean show) {
            if (this.mAccessibilityTracing.isTracingEnabled(2048L)) {
                this.mAccessibilityTracing.logTrace("WindowManager.setForceShowMagnifiableBounds", 2048L, "show=" + show);
            }
            this.mForceShowMagnifiableBounds = show;
            this.mMagnifedViewport.setMagnifiedRegionBorderShown(show, true);
        }

        boolean isForceShowingMagnifiableBounds() {
            if (this.mAccessibilityTracing.isTracingEnabled(2048L)) {
                this.mAccessibilityTracing.logTrace("WindowManager.isForceShowingMagnifiableBounds", 2048L);
            }
            return this.mForceShowMagnifiableBounds;
        }

        void onWindowLayersChanged() {
            if (this.mAccessibilityTracing.isTracingEnabled(2048L)) {
                this.mAccessibilityTracing.logTrace("WindowManager.onWindowLayersChanged", 2048L);
            }
            this.mMagnifedViewport.recomputeBounds();
            this.mService.scheduleAnimationLocked();
        }

        void onDisplaySizeChanged(DisplayContent displayContent) {
            if (this.mAccessibilityTracing.isTracingEnabled(2048L)) {
                this.mAccessibilityTracing.logTrace("WindowManager.onDisplaySizeChanged", 2048L, "displayContent={" + displayContent + "}");
            }
            this.mMagnifedViewport.onDisplaySizeChanged();
            this.mHandler.sendEmptyMessage(4);
        }

        void onAppWindowTransition(int displayId, int transition) {
            if (this.mAccessibilityTracing.isTracingEnabled(2048L)) {
                this.mAccessibilityTracing.logTrace("WindowManager.onAppWindowTransition", 2048L, "displayId=" + displayId + "; transition=" + transition);
            }
            boolean magnifying = this.mMagnifedViewport.isMagnifying();
            if (magnifying) {
                switch (transition) {
                    case 6:
                    case 8:
                    case 10:
                    case 12:
                    case 13:
                    case 14:
                    case 28:
                        this.mHandler.sendEmptyMessage(3);
                        return;
                    default:
                        return;
                }
            }
        }

        void onWindowTransition(WindowState windowState, int transition) {
            if (this.mAccessibilityTracing.isTracingEnabled(2048L)) {
                this.mAccessibilityTracing.logTrace("WindowManager.onWindowTransition", 2048L, "windowState={" + windowState + "}; transition=" + transition);
            }
            boolean magnifying = this.mMagnifedViewport.isMagnifying();
            int type = windowState.mAttrs.type;
            switch (transition) {
                case 1:
                case 3:
                    if (magnifying) {
                        switch (type) {
                            case 2:
                            case 4:
                            case 1000:
                            case 1001:
                            case 1002:
                            case 1003:
                            case 1005:
                            case 2001:
                            case 2002:
                            case 2003:
                            case 2005:
                            case 2006:
                            case 2007:
                            case 2008:
                            case 2009:
                            case 2010:
                            case NotificationShellCmd.NOTIFICATION_ID /* 2020 */:
                            case 2024:
                            case 2035:
                            case 2038:
                                Rect magnifiedRegionBounds = this.mTempRect2;
                                this.mMagnifedViewport.getMagnifiedFrameInContentCoords(magnifiedRegionBounds);
                                Rect touchableRegionBounds = this.mTempRect1;
                                windowState.getTouchableRegion(this.mTempRegion1);
                                this.mTempRegion1.getBounds(touchableRegionBounds);
                                if (!magnifiedRegionBounds.intersect(touchableRegionBounds)) {
                                    this.mCallbacks.onRectangleOnScreenRequested(touchableRegionBounds.left, touchableRegionBounds.top, touchableRegionBounds.right, touchableRegionBounds.bottom);
                                    return;
                                }
                                return;
                            default:
                                return;
                        }
                    }
                    return;
                case 2:
                default:
                    return;
            }
        }

        void notifyImeWindowVisibilityChanged(boolean shown) {
            if (this.mAccessibilityTracing.isTracingEnabled(2048L)) {
                this.mAccessibilityTracing.logTrace("WindowManager.notifyImeWindowVisibilityChanged", 2048L, "shown=" + shown);
            }
            this.mHandler.obtainMessage(6, shown ? 1 : 0, 0).sendToTarget();
        }

        MagnificationSpec getMagnificationSpecForWindow(WindowState windowState) {
            if (this.mAccessibilityTracing.isTracingEnabled(2048L)) {
                this.mAccessibilityTracing.logTrace("WindowManager.getMagnificationSpecForWindow", 2048L, "windowState={" + windowState + "}");
            }
            MagnificationSpec spec = this.mMagnifedViewport.getMagnificationSpec();
            if (spec != null && !spec.isNop() && !windowState.shouldMagnify()) {
                return null;
            }
            return spec;
        }

        void getMagnificationRegion(Region outMagnificationRegion) {
            if (this.mAccessibilityTracing.isTracingEnabled(2048L)) {
                this.mAccessibilityTracing.logTrace("WindowManager.getMagnificationRegion", 2048L, "outMagnificationRegion={" + outMagnificationRegion + "}");
            }
            this.mMagnifedViewport.recomputeBounds();
            this.mMagnifedViewport.getMagnificationRegion(outMagnificationRegion);
        }

        void destroy() {
            if (this.mAccessibilityTracing.isTracingEnabled(2048L)) {
                this.mAccessibilityTracing.logTrace("WindowManager.destroy", 2048L);
            }
            this.mMagnifedViewport.destroyWindow();
        }

        void showMagnificationBoundsIfNeeded() {
            if (this.mAccessibilityTracing.isTracingEnabled(2048L)) {
                this.mAccessibilityTracing.logTrace("WindowManager.showMagnificationBoundsIfNeeded", 2048L);
            }
            this.mHandler.obtainMessage(5).sendToTarget();
        }

        void drawMagnifiedRegionBorderIfNeeded(SurfaceControl.Transaction t) {
            if (this.mAccessibilityTracing.isTracingEnabled(2048L)) {
                this.mAccessibilityTracing.logTrace("WindowManager.drawMagnifiedRegionBorderIfNeeded", 2048L, "transition={" + t + "}");
            }
            this.mMagnifedViewport.drawWindowIfNeeded(t);
        }

        void dump(PrintWriter pw, String prefix) {
            this.mMagnifedViewport.dump(pw, prefix);
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public final class MagnifiedViewport {
            private final float mBorderWidth;
            private final Path mCircularPath;
            private final int mDrawBorderInset;
            private boolean mFullRedrawNeeded;
            private final int mHalfBorderWidth;
            private final Region mMagnificationRegion;
            private final MagnificationSpec mMagnificationSpec;
            private final Region mOldMagnificationRegion;
            private final Point mScreenSize;
            private int mTempLayer;
            private final Matrix mTempMatrix;
            private final ViewportWindow mWindow;
            private final SparseArray<WindowState> mTempWindowStates = new SparseArray<>();
            private final RectF mTempRectF = new RectF();

            MagnifiedViewport() {
                Point point = new Point();
                this.mScreenSize = point;
                this.mTempMatrix = new Matrix();
                this.mMagnificationRegion = new Region();
                this.mOldMagnificationRegion = new Region();
                this.mMagnificationSpec = new MagnificationSpec();
                this.mTempLayer = 0;
                float dimension = DisplayMagnifier.this.mDisplayContext.getResources().getDimension(17104910);
                this.mBorderWidth = dimension;
                this.mHalfBorderWidth = (int) Math.ceil(dimension / 2.0f);
                this.mDrawBorderInset = ((int) dimension) / 2;
                this.mWindow = new ViewportWindow(DisplayMagnifier.this.mDisplayContext);
                if (DisplayMagnifier.this.mDisplayContext.getResources().getConfiguration().isScreenRound()) {
                    Path path = new Path();
                    this.mCircularPath = path;
                    getDisplaySizeLocked(point);
                    int centerXY = point.x / 2;
                    path.addCircle(centerXY, centerXY, centerXY, Path.Direction.CW);
                } else {
                    this.mCircularPath = null;
                }
                recomputeBounds();
            }

            void getMagnificationRegion(Region outMagnificationRegion) {
                outMagnificationRegion.set(this.mMagnificationRegion);
            }

            void updateMagnificationSpec(MagnificationSpec spec) {
                if (spec != null) {
                    this.mMagnificationSpec.initialize(spec.scale, spec.offsetX, spec.offsetY);
                } else {
                    this.mMagnificationSpec.clear();
                }
                if (!DisplayMagnifier.this.mHandler.hasMessages(5)) {
                    setMagnifiedRegionBorderShown(isMagnifying() || DisplayMagnifier.this.isForceShowingMagnifiableBounds(), true);
                }
            }

            void recomputeBounds() {
                getDisplaySizeLocked(this.mScreenSize);
                int screenWidth = this.mScreenSize.x;
                int screenHeight = this.mScreenSize.y;
                this.mMagnificationRegion.set(0, 0, 0, 0);
                Region availableBounds = DisplayMagnifier.this.mTempRegion1;
                availableBounds.set(0, 0, screenWidth, screenHeight);
                Path path = this.mCircularPath;
                if (path != null) {
                    availableBounds.setPath(path, availableBounds);
                }
                Region nonMagnifiedBounds = DisplayMagnifier.this.mTempRegion4;
                nonMagnifiedBounds.set(0, 0, 0, 0);
                SparseArray<WindowState> visibleWindows = this.mTempWindowStates;
                visibleWindows.clear();
                populateWindowsOnScreen(visibleWindows);
                int visibleWindowCount = visibleWindows.size();
                for (int i = visibleWindowCount - 1; i >= 0; i--) {
                    WindowState windowState = visibleWindows.valueAt(i);
                    int windowType = windowState.mAttrs.type;
                    if (!isExcludedWindowType(windowType) && (windowState.mAttrs.privateFlags & 2097152) == 0) {
                        if ((windowState.mAttrs.privateFlags & 1048576) != 0) {
                            continue;
                        } else {
                            Matrix matrix = this.mTempMatrix;
                            AccessibilityController.populateTransformationMatrix(windowState, matrix);
                            Region touchableRegion = DisplayMagnifier.this.mTempRegion3;
                            windowState.getTouchableRegion(touchableRegion);
                            Rect touchableFrame = DisplayMagnifier.this.mTempRect1;
                            touchableRegion.getBounds(touchableFrame);
                            RectF windowFrame = this.mTempRectF;
                            windowFrame.set(touchableFrame);
                            windowFrame.offset(-windowState.getFrame().left, -windowState.getFrame().top);
                            matrix.mapRect(windowFrame);
                            Region windowBounds = DisplayMagnifier.this.mTempRegion2;
                            windowBounds.set((int) windowFrame.left, (int) windowFrame.top, (int) windowFrame.right, (int) windowFrame.bottom);
                            Region portionOfWindowAlreadyAccountedFor = DisplayMagnifier.this.mTempRegion3;
                            portionOfWindowAlreadyAccountedFor.set(this.mMagnificationRegion);
                            portionOfWindowAlreadyAccountedFor.op(nonMagnifiedBounds, Region.Op.UNION);
                            windowBounds.op(portionOfWindowAlreadyAccountedFor, Region.Op.DIFFERENCE);
                            if (windowState.shouldMagnify()) {
                                this.mMagnificationRegion.op(windowBounds, Region.Op.UNION);
                                this.mMagnificationRegion.op(availableBounds, Region.Op.INTERSECT);
                            } else {
                                nonMagnifiedBounds.op(windowBounds, Region.Op.UNION);
                                availableBounds.op(windowBounds, Region.Op.DIFFERENCE);
                            }
                            if (AccessibilityController.isUntouchableNavigationBar(windowState, DisplayMagnifier.this.mTempRegion3)) {
                                Rect navBarInsets = AccessibilityController.getNavBarInsets(DisplayMagnifier.this.mDisplayContent);
                                nonMagnifiedBounds.op(navBarInsets, Region.Op.UNION);
                                availableBounds.op(navBarInsets, Region.Op.DIFFERENCE);
                            }
                            if (windowState.areAppWindowBoundsLetterboxed()) {
                                Region letterboxBounds = AccessibilityController.getLetterboxBounds(windowState);
                                nonMagnifiedBounds.op(letterboxBounds, Region.Op.UNION);
                                availableBounds.op(letterboxBounds, Region.Op.DIFFERENCE);
                            }
                            Region accountedBounds = DisplayMagnifier.this.mTempRegion2;
                            accountedBounds.set(this.mMagnificationRegion);
                            accountedBounds.op(nonMagnifiedBounds, Region.Op.UNION);
                            accountedBounds.op(0, 0, screenWidth, screenHeight, Region.Op.INTERSECT);
                            if (accountedBounds.isRect()) {
                                Rect accountedFrame = DisplayMagnifier.this.mTempRect1;
                                accountedBounds.getBounds(accountedFrame);
                                if (accountedFrame.width() == screenWidth && accountedFrame.height() == screenHeight) {
                                    break;
                                }
                            }
                        }
                    }
                }
                visibleWindows.clear();
                Region region = this.mMagnificationRegion;
                int i2 = this.mDrawBorderInset;
                region.op(i2, i2, screenWidth - i2, screenHeight - i2, Region.Op.INTERSECT);
                boolean magnifiedChanged = !this.mOldMagnificationRegion.equals(this.mMagnificationRegion);
                if (magnifiedChanged) {
                    this.mWindow.setBounds(this.mMagnificationRegion);
                    Rect dirtyRect = DisplayMagnifier.this.mTempRect1;
                    if (this.mFullRedrawNeeded) {
                        this.mFullRedrawNeeded = false;
                        int i3 = this.mDrawBorderInset;
                        dirtyRect.set(i3, i3, screenWidth - i3, screenHeight - i3);
                        this.mWindow.invalidate(dirtyRect);
                    } else {
                        Region dirtyRegion = DisplayMagnifier.this.mTempRegion3;
                        dirtyRegion.set(this.mMagnificationRegion);
                        dirtyRegion.op(this.mOldMagnificationRegion, Region.Op.XOR);
                        dirtyRegion.getBounds(dirtyRect);
                        this.mWindow.invalidate(dirtyRect);
                    }
                    this.mOldMagnificationRegion.set(this.mMagnificationRegion);
                    SomeArgs args = SomeArgs.obtain();
                    args.arg1 = Region.obtain(this.mMagnificationRegion);
                    DisplayMagnifier.this.mHandler.obtainMessage(1, args).sendToTarget();
                }
            }

            private boolean isExcludedWindowType(int windowType) {
                return windowType == 2027 || windowType == 2039;
            }

            void onDisplaySizeChanged() {
                if (isMagnifying() || DisplayMagnifier.this.isForceShowingMagnifiableBounds()) {
                    setMagnifiedRegionBorderShown(false, false);
                    long delay = ((float) DisplayMagnifier.this.mLongAnimationDuration) * DisplayMagnifier.this.mService.getWindowAnimationScaleLocked();
                    Message message = DisplayMagnifier.this.mHandler.obtainMessage(5);
                    DisplayMagnifier.this.mHandler.sendMessageDelayed(message, delay);
                }
                recomputeBounds();
                this.mWindow.updateSize();
            }

            void setMagnifiedRegionBorderShown(boolean shown, boolean animate) {
                if (shown) {
                    this.mFullRedrawNeeded = true;
                    this.mOldMagnificationRegion.set(0, 0, 0, 0);
                }
                this.mWindow.setShown(shown, animate);
            }

            void getMagnifiedFrameInContentCoords(Rect rect) {
                MagnificationSpec spec = this.mMagnificationSpec;
                this.mMagnificationRegion.getBounds(rect);
                rect.offset((int) (-spec.offsetX), (int) (-spec.offsetY));
                rect.scale(1.0f / spec.scale);
            }

            boolean isMagnifying() {
                return this.mMagnificationSpec.scale > 1.0f;
            }

            MagnificationSpec getMagnificationSpec() {
                return this.mMagnificationSpec;
            }

            void drawWindowIfNeeded(SurfaceControl.Transaction t) {
                recomputeBounds();
                this.mWindow.drawIfNeeded(t);
            }

            void destroyWindow() {
                this.mWindow.releaseSurface();
            }

            private void populateWindowsOnScreen(final SparseArray<WindowState> outWindows) {
                this.mTempLayer = 0;
                DisplayMagnifier.this.mDisplayContent.forAllWindows(new Consumer() { // from class: com.android.server.wm.AccessibilityController$DisplayMagnifier$MagnifiedViewport$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        AccessibilityController.DisplayMagnifier.MagnifiedViewport.this.m7743xa32e27b0(outWindows, (WindowState) obj);
                    }
                }, false);
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            /* renamed from: lambda$populateWindowsOnScreen$0$com-android-server-wm-AccessibilityController$DisplayMagnifier$MagnifiedViewport  reason: not valid java name */
            public /* synthetic */ void m7743xa32e27b0(SparseArray outWindows, WindowState w) {
                if (w.isOnScreen() && w.isVisible() && w.mAttrs.alpha != 0.0f) {
                    int i = this.mTempLayer + 1;
                    this.mTempLayer = i;
                    outWindows.put(i, w);
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void getDisplaySizeLocked(Point outSize) {
                Rect bounds = DisplayMagnifier.this.mDisplayContent.getConfiguration().windowConfiguration.getBounds();
                outSize.set(bounds.width(), bounds.height());
            }

            void dump(PrintWriter pw, String prefix) {
                this.mWindow.dump(pw, prefix);
            }

            /* JADX INFO: Access modifiers changed from: private */
            /* loaded from: classes2.dex */
            public final class ViewportWindow {
                private static final String SURFACE_TITLE = "Magnification Overlay";
                private int mAlpha;
                private final AnimationController mAnimationController;
                private final BLASTBufferQueue mBlastBufferQueue;
                private boolean mInvalidated;
                private boolean mShown;
                private final Surface mSurface;
                private final SurfaceControl mSurfaceControl;
                private final Region mBounds = new Region();
                private final Rect mDirtyRect = new Rect();
                private final Paint mPaint = new Paint();

                ViewportWindow(Context context) {
                    SurfaceControl surfaceControl = null;
                    try {
                        surfaceControl = DisplayMagnifier.this.mDisplayContent.makeOverlay().setName(SURFACE_TITLE).setBLASTLayer().setFormat(-3).setCallsite("ViewportWindow").build();
                    } catch (Surface.OutOfResourcesException e) {
                    }
                    this.mSurfaceControl = surfaceControl;
                    DisplayMagnifier.this.mDisplay.getRealSize(MagnifiedViewport.this.mScreenSize);
                    BLASTBufferQueue bLASTBufferQueue = new BLASTBufferQueue(SURFACE_TITLE, surfaceControl, MagnifiedViewport.this.mScreenSize.x, MagnifiedViewport.this.mScreenSize.y, 1);
                    this.mBlastBufferQueue = bLASTBufferQueue;
                    SurfaceControl.Transaction t = DisplayMagnifier.this.mService.mTransactionFactory.get();
                    int layer = DisplayMagnifier.this.mService.mPolicy.getWindowLayerFromTypeLw(2027) * 10000;
                    t.setLayer(surfaceControl, layer).setPosition(surfaceControl, 0.0f, 0.0f);
                    InputMonitor.setTrustedOverlayInputInfo(surfaceControl, t, DisplayMagnifier.this.mDisplayContent.getDisplayId(), SURFACE_TITLE);
                    t.apply();
                    this.mSurface = bLASTBufferQueue.createSurface();
                    this.mAnimationController = new AnimationController(context, DisplayMagnifier.this.mService.mH.getLooper());
                    TypedValue typedValue = new TypedValue();
                    context.getTheme().resolveAttribute(16843664, typedValue, true);
                    int borderColor = context.getColor(typedValue.resourceId);
                    this.mPaint.setStyle(Paint.Style.STROKE);
                    this.mPaint.setStrokeWidth(MagnifiedViewport.this.mBorderWidth);
                    this.mPaint.setColor(borderColor);
                    this.mInvalidated = true;
                }

                void setShown(boolean shown, boolean animate) {
                    synchronized (DisplayMagnifier.this.mService.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (this.mShown == shown) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return;
                            }
                            this.mShown = shown;
                            this.mAnimationController.onFrameShownStateChanged(shown, animate);
                            WindowManagerService.resetPriorityAfterLockedSection();
                        } catch (Throwable th) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                }

                int getAlpha() {
                    int i;
                    synchronized (DisplayMagnifier.this.mService.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            i = this.mAlpha;
                        } catch (Throwable th) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return i;
                }

                void setAlpha(int alpha) {
                    synchronized (DisplayMagnifier.this.mService.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (this.mAlpha == alpha) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return;
                            }
                            this.mAlpha = alpha;
                            invalidate(null);
                            WindowManagerService.resetPriorityAfterLockedSection();
                        } catch (Throwable th) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                }

                void setBounds(Region bounds) {
                    synchronized (DisplayMagnifier.this.mService.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (this.mBounds.equals(bounds)) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return;
                            }
                            this.mBounds.set(bounds);
                            invalidate(this.mDirtyRect);
                            WindowManagerService.resetPriorityAfterLockedSection();
                        } catch (Throwable th) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                }

                void updateSize() {
                    synchronized (DisplayMagnifier.this.mService.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            MagnifiedViewport magnifiedViewport = MagnifiedViewport.this;
                            magnifiedViewport.getDisplaySizeLocked(magnifiedViewport.mScreenSize);
                            this.mBlastBufferQueue.update(this.mSurfaceControl, MagnifiedViewport.this.mScreenSize.x, MagnifiedViewport.this.mScreenSize.y, 1);
                            invalidate(this.mDirtyRect);
                        } catch (Throwable th) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }

                void invalidate(Rect dirtyRect) {
                    if (dirtyRect != null) {
                        this.mDirtyRect.set(dirtyRect);
                    } else {
                        this.mDirtyRect.setEmpty();
                    }
                    this.mInvalidated = true;
                    DisplayMagnifier.this.mService.scheduleAnimationLocked();
                }

                void drawIfNeeded(SurfaceControl.Transaction t) {
                    synchronized (DisplayMagnifier.this.mService.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (!this.mInvalidated) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return;
                            }
                            this.mInvalidated = false;
                            if (this.mAlpha > 0) {
                                Canvas canvas = null;
                                try {
                                    if (this.mDirtyRect.isEmpty()) {
                                        this.mBounds.getBounds(this.mDirtyRect);
                                    }
                                    this.mDirtyRect.inset(-MagnifiedViewport.this.mHalfBorderWidth, -MagnifiedViewport.this.mHalfBorderWidth);
                                    canvas = this.mSurface.lockCanvas(this.mDirtyRect);
                                } catch (Surface.OutOfResourcesException e) {
                                } catch (IllegalArgumentException e2) {
                                }
                                if (canvas == null) {
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    return;
                                }
                                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                                this.mPaint.setAlpha(this.mAlpha);
                                Path path = this.mBounds.getBoundaryPath();
                                canvas.drawPath(path, this.mPaint);
                                this.mSurface.unlockCanvasAndPost(canvas);
                                t.show(this.mSurfaceControl);
                            } else {
                                t.hide(this.mSurfaceControl);
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                        } catch (Throwable th) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                }

                void releaseSurface() {
                    BLASTBufferQueue bLASTBufferQueue = this.mBlastBufferQueue;
                    if (bLASTBufferQueue != null) {
                        bLASTBufferQueue.destroy();
                    }
                    DisplayMagnifier.this.mService.mTransactionFactory.get().remove(this.mSurfaceControl).apply();
                    this.mSurface.release();
                }

                void dump(PrintWriter pw, String prefix) {
                    pw.println(prefix + " mBounds= " + this.mBounds + " mDirtyRect= " + this.mDirtyRect + " mWidth= " + MagnifiedViewport.this.mScreenSize.x + " mHeight= " + MagnifiedViewport.this.mScreenSize.y);
                }

                /* JADX INFO: Access modifiers changed from: private */
                /* loaded from: classes2.dex */
                public final class AnimationController extends Handler {
                    private static final int MAX_ALPHA = 255;
                    private static final int MIN_ALPHA = 0;
                    private static final int MSG_FRAME_SHOWN_STATE_CHANGED = 1;
                    private static final String PROPERTY_NAME_ALPHA = "alpha";
                    private final ValueAnimator mShowHideFrameAnimator;

                    AnimationController(Context context, Looper looper) {
                        super(looper);
                        ObjectAnimator ofInt = ObjectAnimator.ofInt(ViewportWindow.this, PROPERTY_NAME_ALPHA, 0, 255);
                        this.mShowHideFrameAnimator = ofInt;
                        Interpolator interpolator = new DecelerateInterpolator(2.5f);
                        long longAnimationDuration = context.getResources().getInteger(17694722);
                        ofInt.setInterpolator(interpolator);
                        ofInt.setDuration(longAnimationDuration);
                    }

                    void onFrameShownStateChanged(boolean shown, boolean animate) {
                        obtainMessage(1, shown ? 1 : 0, animate ? 1 : 0).sendToTarget();
                    }

                    @Override // android.os.Handler
                    public void handleMessage(Message message) {
                        switch (message.what) {
                            case 1:
                                boolean shown = message.arg1 == 1;
                                boolean animate = message.arg2 == 1;
                                if (animate) {
                                    if (this.mShowHideFrameAnimator.isRunning()) {
                                        this.mShowHideFrameAnimator.reverse();
                                        return;
                                    } else if (shown) {
                                        this.mShowHideFrameAnimator.start();
                                        return;
                                    } else {
                                        this.mShowHideFrameAnimator.reverse();
                                        return;
                                    }
                                }
                                this.mShowHideFrameAnimator.cancel();
                                if (shown) {
                                    ViewportWindow.this.setAlpha(255);
                                    return;
                                } else {
                                    ViewportWindow.this.setAlpha(0);
                                    return;
                                }
                            default:
                                return;
                        }
                    }
                }
            }
        }

        /* loaded from: classes2.dex */
        private class MyHandler extends Handler {
            public static final int MESSAGE_NOTIFY_DISPLAY_SIZE_CHANGED = 4;
            public static final int MESSAGE_NOTIFY_IME_WINDOW_VISIBILITY_CHANGED = 6;
            public static final int MESSAGE_NOTIFY_MAGNIFICATION_REGION_CHANGED = 1;
            public static final int MESSAGE_NOTIFY_USER_CONTEXT_CHANGED = 3;
            public static final int MESSAGE_SHOW_MAGNIFIED_REGION_BOUNDS_IF_NEEDED = 5;

            MyHandler(Looper looper) {
                super(looper);
            }

            @Override // android.os.Handler
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        SomeArgs args = (SomeArgs) message.obj;
                        Region magnifiedBounds = (Region) args.arg1;
                        DisplayMagnifier.this.mCallbacks.onMagnificationRegionChanged(magnifiedBounds);
                        magnifiedBounds.recycle();
                        return;
                    case 2:
                    default:
                        return;
                    case 3:
                        DisplayMagnifier.this.mCallbacks.onUserContextChanged();
                        return;
                    case 4:
                        DisplayMagnifier.this.mCallbacks.onDisplaySizeChanged();
                        return;
                    case 5:
                        synchronized (DisplayMagnifier.this.mService.mGlobalLock) {
                            try {
                                WindowManagerService.boostPriorityForLockedSection();
                                if (DisplayMagnifier.this.mMagnifedViewport.isMagnifying() || DisplayMagnifier.this.isForceShowingMagnifiableBounds()) {
                                    DisplayMagnifier.this.mMagnifedViewport.setMagnifiedRegionBorderShown(true, true);
                                    DisplayMagnifier.this.mService.scheduleAnimationLocked();
                                }
                            } catch (Throwable th) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                throw th;
                            }
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    case 6:
                        boolean shown = message.arg1 == 1;
                        DisplayMagnifier.this.mCallbacks.onImeWindowVisibilityChanged(shown);
                        return;
                }
            }
        }
    }

    static boolean isUntouchableNavigationBar(WindowState windowState, Region touchableRegion) {
        if (windowState.mAttrs.type != 2019) {
            return false;
        }
        windowState.getTouchableRegion(touchableRegion);
        return touchableRegion.isEmpty();
    }

    static Rect getNavBarInsets(DisplayContent displayContent) {
        InsetsSource source = displayContent.getInsetsStateController().getRawInsetsState().peekSource(1);
        return source != null ? source.getFrame() : EMPTY_RECT;
    }

    static Region getLetterboxBounds(WindowState windowState) {
        ActivityRecord appToken = windowState.mActivityRecord;
        if (appToken == null) {
            return new Region();
        }
        Rect letterboxInsets = appToken.getLetterboxInsets();
        Rect nonLetterboxRect = windowState.getBounds();
        nonLetterboxRect.inset(letterboxInsets);
        Region letterboxBounds = new Region();
        letterboxBounds.set(windowState.getBounds());
        letterboxBounds.op(nonLetterboxRect, Region.Op.DIFFERENCE);
        return letterboxBounds;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class WindowsForAccessibilityObserver {
        private static final boolean DEBUG = false;
        private static final String LOG_TAG = "WindowManager";
        private final AccessibilityWindowsPopulator mA11yWindowsPopulator;
        private final AccessibilityControllerInternalImpl mAccessibilityTracing;
        private final WindowManagerInternal.WindowsForAccessibilityCallback mCallback;
        private final int mDisplayId;
        private final Handler mHandler;
        private boolean mInitialized;
        private final WindowManagerService mService;
        private final List<AccessibilityWindowsPopulator.AccessibilityWindow> mTempA11yWindows = new ArrayList();
        private final Set<IBinder> mTempBinderSet = new ArraySet();
        private final Point mTempPoint = new Point();
        private final Region mTempRegion = new Region();
        private final Region mTempRegion1 = new Region();
        private final Region mTempRegion2 = new Region();
        private final long mRecurringAccessibilityEventsIntervalMillis = ViewConfiguration.getSendRecurringAccessibilityEventsInterval();

        WindowsForAccessibilityObserver(WindowManagerService windowManagerService, int displayId, WindowManagerInternal.WindowsForAccessibilityCallback callback, AccessibilityWindowsPopulator accessibilityWindowsPopulator) {
            this.mService = windowManagerService;
            this.mCallback = callback;
            this.mDisplayId = displayId;
            this.mHandler = new MyHandler(windowManagerService.mH.getLooper());
            this.mAccessibilityTracing = AccessibilityController.getAccessibilityControllerInternal(windowManagerService);
            this.mA11yWindowsPopulator = accessibilityWindowsPopulator;
            computeChangedWindows(true);
        }

        void performComputeChangedWindows(boolean forceSend) {
            if (this.mAccessibilityTracing.isTracingEnabled(GadgetFunction.NCM)) {
                this.mAccessibilityTracing.logTrace("WindowManager.performComputeChangedWindows", GadgetFunction.NCM, "forceSend=" + forceSend);
            }
            this.mHandler.removeMessages(1);
            computeChangedWindows(forceSend);
        }

        void scheduleComputeChangedWindows() {
            if (this.mAccessibilityTracing.isTracingEnabled(GadgetFunction.NCM)) {
                this.mAccessibilityTracing.logTrace("WindowManager.scheduleComputeChangedWindows", GadgetFunction.NCM);
            }
            if (!this.mHandler.hasMessages(1)) {
                this.mHandler.sendEmptyMessageDelayed(1, this.mRecurringAccessibilityEventsIntervalMillis);
            }
        }

        void computeChangedWindows(boolean forceSend) {
            WindowState topFocusedWindowState;
            IBinder topFocusedWindowToken;
            int windowCount;
            DisplayContent dc;
            if (this.mAccessibilityTracing.isTracingEnabled(GadgetFunction.NCM)) {
                this.mAccessibilityTracing.logTrace("WindowManager.computeChangedWindows", GadgetFunction.NCM, "forceSend=" + forceSend);
            }
            List<WindowInfo> windows = new ArrayList<>();
            IBinder topFocusedWindowToken2 = null;
            synchronized (this.mService.mGlobalLock) {
                try {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        RecentsAnimationController controller = this.mService.getRecentsAnimationController();
                        if (controller != null) {
                            topFocusedWindowState = controller.getTargetAppMainWindow();
                        } else {
                            topFocusedWindowState = getTopFocusWindow();
                        }
                        if (topFocusedWindowState == null) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                        DisplayContent dc2 = this.mService.mRoot.getDisplayContent(this.mDisplayId);
                        if (dc2 == null) {
                            Slog.w("WindowManager", "display content is null, should be created later");
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                        Display display = dc2.getDisplay();
                        display.getRealSize(this.mTempPoint);
                        int screenWidth = this.mTempPoint.x;
                        int screenHeight = this.mTempPoint.y;
                        Region unaccountedSpace = this.mTempRegion;
                        unaccountedSpace.set(0, 0, screenWidth, screenHeight);
                        List<AccessibilityWindowsPopulator.AccessibilityWindow> visibleWindows = this.mTempA11yWindows;
                        this.mA11yWindowsPopulator.populateVisibleWindowsOnScreenLocked(this.mDisplayId, visibleWindows);
                        Set<IBinder> addedWindows = this.mTempBinderSet;
                        addedWindows.clear();
                        boolean focusedWindowAdded = false;
                        int visibleWindowCount = visibleWindows.size();
                        int i = 0;
                        while (true) {
                            if (i >= visibleWindowCount) {
                                topFocusedWindowToken = topFocusedWindowToken2;
                                break;
                            }
                            AccessibilityWindowsPopulator.AccessibilityWindow a11yWindow = visibleWindows.get(i);
                            Region regionInWindow = new Region();
                            topFocusedWindowToken = topFocusedWindowToken2;
                            Display display2 = display;
                            try {
                                a11yWindow.getTouchableRegionInWindow(regionInWindow);
                                if (windowMattersToAccessibility(a11yWindow, regionInWindow, unaccountedSpace)) {
                                    addPopulatedWindowInfo(a11yWindow, regionInWindow, windows, addedWindows);
                                    if (windowMattersToUnaccountedSpaceComputation(a11yWindow)) {
                                        updateUnaccountedSpace(a11yWindow, unaccountedSpace);
                                    }
                                    focusedWindowAdded |= a11yWindow.isFocused();
                                    dc = dc2;
                                } else if (a11yWindow.isUntouchableNavigationBar()) {
                                    dc = dc2;
                                    unaccountedSpace.op(AccessibilityController.getNavBarInsets(dc2), unaccountedSpace, Region.Op.REVERSE_DIFFERENCE);
                                } else {
                                    dc = dc2;
                                }
                                if (unaccountedSpace.isEmpty() && focusedWindowAdded) {
                                    break;
                                }
                                i++;
                                topFocusedWindowToken2 = topFocusedWindowToken;
                                display = display2;
                                dc2 = dc;
                            } catch (Throwable th) {
                                th = th;
                                WindowManagerService.resetPriorityAfterLockedSection();
                                throw th;
                            }
                        }
                        int windowCount2 = windows.size();
                        int i2 = 0;
                        while (i2 < windowCount2) {
                            WindowInfo window = windows.get(i2);
                            if (!addedWindows.contains(window.parentToken)) {
                                window.parentToken = null;
                            }
                            if (window.childTokens == null) {
                                windowCount = windowCount2;
                            } else {
                                int childTokenCount = window.childTokens.size();
                                windowCount = windowCount2;
                                int windowCount3 = childTokenCount - 1;
                                while (windowCount3 >= 0) {
                                    int childTokenCount2 = childTokenCount;
                                    if (!addedWindows.contains(window.childTokens.get(windowCount3))) {
                                        window.childTokens.remove(windowCount3);
                                    }
                                    windowCount3--;
                                    childTokenCount = childTokenCount2;
                                }
                            }
                            i2++;
                            windowCount2 = windowCount;
                        }
                        visibleWindows.clear();
                        addedWindows.clear();
                        int topFocusedDisplayId = this.mService.mRoot.getTopFocusedDisplayContent().getDisplayId();
                        IBinder topFocusedWindowToken3 = topFocusedWindowState.mClient.asBinder();
                        WindowManagerService.resetPriorityAfterLockedSection();
                        this.mCallback.onWindowsForAccessibilityChanged(forceSend, topFocusedDisplayId, topFocusedWindowToken3, windows);
                        clearAndRecycleWindows(windows);
                        this.mInitialized = true;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } catch (Throwable th3) {
                    th = th3;
                }
            }
        }

        private boolean windowMattersToUnaccountedSpaceComputation(AccessibilityWindowsPopulator.AccessibilityWindow a11yWindow) {
            return (a11yWindow.isTouchable() || a11yWindow.getType() == 2034 || !a11yWindow.isTrustedOverlay()) && a11yWindow.getType() != 2032;
        }

        private boolean windowMattersToAccessibility(AccessibilityWindowsPopulator.AccessibilityWindow a11yWindow, Region regionInScreen, Region unaccountedSpace) {
            if (a11yWindow.ignoreRecentsAnimationForAccessibility()) {
                return false;
            }
            if (a11yWindow.isFocused()) {
                return true;
            }
            return (a11yWindow.isTouchable() || a11yWindow.getType() == 2034 || a11yWindow.isPIPMenu()) && !unaccountedSpace.quickReject(regionInScreen) && isReportedWindowType(a11yWindow.getType());
        }

        private void updateUnaccountedSpace(AccessibilityWindowsPopulator.AccessibilityWindow a11yWindow, Region unaccountedSpace) {
            if (a11yWindow.getType() != 2032) {
                Region touchableRegion = this.mTempRegion2;
                a11yWindow.getTouchableRegionInScreen(touchableRegion);
                unaccountedSpace.op(touchableRegion, unaccountedSpace, Region.Op.REVERSE_DIFFERENCE);
                Region letterboxBounds = this.mTempRegion1;
                if (a11yWindow.setLetterBoxBoundsIfNeeded(letterboxBounds).booleanValue()) {
                    unaccountedSpace.op(letterboxBounds, unaccountedSpace, Region.Op.REVERSE_DIFFERENCE);
                }
            }
        }

        private static void addPopulatedWindowInfo(AccessibilityWindowsPopulator.AccessibilityWindow a11yWindow, Region regionInScreen, List<WindowInfo> out, Set<IBinder> tokenOut) {
            WindowInfo window = a11yWindow.getWindowInfo();
            window.regionInScreen.set(regionInScreen);
            window.layer = tokenOut.size();
            out.add(window);
            tokenOut.add(window.token);
        }

        private static void clearAndRecycleWindows(List<WindowInfo> windows) {
            int windowCount = windows.size();
            for (int i = windowCount - 1; i >= 0; i--) {
                windows.remove(i).recycle();
            }
        }

        private static boolean isReportedWindowType(int windowType) {
            return (windowType == 2013 || windowType == 2021 || windowType == 2026 || windowType == 2016 || windowType == 2022 || windowType == 2018 || windowType == 2027 || windowType == 1004 || windowType == 2015 || windowType == 2030) ? false : true;
        }

        private WindowState getTopFocusWindow() {
            return this.mService.mRoot.getTopFocusedDisplayContent().mCurrentFocus;
        }

        public String toString() {
            return "WindowsForAccessibilityObserver{mDisplayId=" + this.mDisplayId + ", mInitialized=" + this.mInitialized + '}';
        }

        /* loaded from: classes2.dex */
        private class MyHandler extends Handler {
            public static final int MESSAGE_COMPUTE_CHANGED_WINDOWS = 1;

            public MyHandler(Looper looper) {
                super(looper, null, false);
            }

            @Override // android.os.Handler
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        WindowsForAccessibilityObserver.this.computeChangedWindows(false);
                        return;
                    default:
                        return;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class AccessibilityControllerInternalImpl implements WindowManagerInternal.AccessibilityControllerInternal {
        private static AccessibilityControllerInternalImpl sInstance;
        private UiChangesForAccessibilityCallbacksDispatcher mCallbacksDispatcher;
        private volatile long mEnabledTracingFlags = 0;
        private final Looper mLooper;
        private final AccessibilityTracing mTracing;

        static AccessibilityControllerInternalImpl getInstance(WindowManagerService service) {
            AccessibilityControllerInternalImpl accessibilityControllerInternalImpl;
            synchronized (AccessibilityController.STATIC_LOCK) {
                if (sInstance == null) {
                    sInstance = new AccessibilityControllerInternalImpl(service);
                }
                accessibilityControllerInternalImpl = sInstance;
            }
            return accessibilityControllerInternalImpl;
        }

        private AccessibilityControllerInternalImpl(WindowManagerService service) {
            this.mLooper = service.mH.getLooper();
            this.mTracing = AccessibilityTracing.getInstance(service);
        }

        @Override // com.android.server.wm.WindowManagerInternal.AccessibilityControllerInternal
        public void startTrace(long loggingTypes) {
            this.mEnabledTracingFlags = loggingTypes;
            this.mTracing.startTrace();
        }

        @Override // com.android.server.wm.WindowManagerInternal.AccessibilityControllerInternal
        public void stopTrace() {
            this.mTracing.stopTrace();
            this.mEnabledTracingFlags = 0L;
        }

        @Override // com.android.server.wm.WindowManagerInternal.AccessibilityControllerInternal
        public boolean isAccessibilityTracingEnabled() {
            return this.mTracing.isEnabled();
        }

        boolean isTracingEnabled(long flags) {
            return (this.mEnabledTracingFlags & flags) != 0;
        }

        void logTrace(String where, long loggingTypes) {
            logTrace(where, loggingTypes, "");
        }

        void logTrace(String where, long loggingTypes, String callingParams) {
            logTrace(where, loggingTypes, callingParams, "".getBytes(), Binder.getCallingUid());
        }

        void logTrace(String where, long loggingTypes, String callingParams, byte[] a11yDump, int callingUid) {
            this.mTracing.logState(where, loggingTypes, callingParams, a11yDump, callingUid, new HashSet(Arrays.asList("logTrace")));
        }

        @Override // com.android.server.wm.WindowManagerInternal.AccessibilityControllerInternal
        public void logTrace(String where, long loggingTypes, String callingParams, byte[] a11yDump, int callingUid, StackTraceElement[] stackTrace, Set<String> ignoreStackEntries) {
            this.mTracing.logState(where, loggingTypes, callingParams, a11yDump, callingUid, stackTrace, ignoreStackEntries);
        }

        @Override // com.android.server.wm.WindowManagerInternal.AccessibilityControllerInternal
        public void logTrace(String where, long loggingTypes, String callingParams, byte[] a11yDump, int callingUid, StackTraceElement[] callStack, long timeStamp, int processId, long threadId, Set<String> ignoreStackEntries) {
            this.mTracing.logState(where, loggingTypes, callingParams, a11yDump, callingUid, callStack, timeStamp, processId, threadId, ignoreStackEntries);
        }

        @Override // com.android.server.wm.WindowManagerInternal.AccessibilityControllerInternal
        public void setUiChangesForAccessibilityCallbacks(WindowManagerInternal.AccessibilityControllerInternal.UiChangesForAccessibilityCallbacks callbacks) {
            if (isTracingEnabled(2048L)) {
                logTrace(AccessibilityController.TAG + ".setAccessibilityWindowManagerCallbacks", 2048L, "callbacks={" + callbacks + "}");
            }
            if (callbacks != null) {
                if (this.mCallbacksDispatcher != null) {
                    throw new IllegalStateException("Accessibility window manager callback already set!");
                }
                this.mCallbacksDispatcher = new UiChangesForAccessibilityCallbacksDispatcher(this, this.mLooper, callbacks);
            } else if (this.mCallbacksDispatcher == null) {
                throw new IllegalStateException("Accessibility window manager callback already cleared!");
            } else {
                this.mCallbacksDispatcher = null;
            }
        }

        public boolean hasWindowManagerEventDispatcher() {
            if (isTracingEnabled(3072L)) {
                logTrace(AccessibilityController.TAG + ".hasCallbacks", 3072L);
            }
            return this.mCallbacksDispatcher != null;
        }

        public void onRectangleOnScreenRequested(int displayId, Rect rectangle) {
            if (isTracingEnabled(2048L)) {
                logTrace(AccessibilityController.TAG + ".onRectangleOnScreenRequested", 2048L, "rectangle={" + rectangle + "}");
            }
            UiChangesForAccessibilityCallbacksDispatcher uiChangesForAccessibilityCallbacksDispatcher = this.mCallbacksDispatcher;
            if (uiChangesForAccessibilityCallbacksDispatcher != null) {
                uiChangesForAccessibilityCallbacksDispatcher.onRectangleOnScreenRequested(displayId, rectangle);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public static final class UiChangesForAccessibilityCallbacksDispatcher {
            private static final boolean DEBUG_RECTANGLE_REQUESTED = false;
            private static final String LOG_TAG = "WindowManager";
            private final AccessibilityControllerInternalImpl mAccessibilityTracing;
            private final WindowManagerInternal.AccessibilityControllerInternal.UiChangesForAccessibilityCallbacks mCallbacks;
            private final Handler mHandler;

            UiChangesForAccessibilityCallbacksDispatcher(AccessibilityControllerInternalImpl accessibilityControllerInternal, Looper looper, WindowManagerInternal.AccessibilityControllerInternal.UiChangesForAccessibilityCallbacks callbacks) {
                this.mAccessibilityTracing = accessibilityControllerInternal;
                this.mCallbacks = callbacks;
                this.mHandler = new Handler(looper);
            }

            void onRectangleOnScreenRequested(int displayId, Rect rectangle) {
                if (this.mAccessibilityTracing.isTracingEnabled(2048L)) {
                    this.mAccessibilityTracing.logTrace("WindowManager.onRectangleOnScreenRequested", 2048L, "rectangle={" + rectangle + "}");
                }
                final WindowManagerInternal.AccessibilityControllerInternal.UiChangesForAccessibilityCallbacks uiChangesForAccessibilityCallbacks = this.mCallbacks;
                Objects.requireNonNull(uiChangesForAccessibilityCallbacks);
                Message m = PooledLambda.obtainMessage(new QuintConsumer() { // from class: com.android.server.wm.AccessibilityController$AccessibilityControllerInternalImpl$UiChangesForAccessibilityCallbacksDispatcher$$ExternalSyntheticLambda0
                    public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                        WindowManagerInternal.AccessibilityControllerInternal.UiChangesForAccessibilityCallbacks.this.onRectangleOnScreenRequested(((Integer) obj).intValue(), ((Integer) obj2).intValue(), ((Integer) obj3).intValue(), ((Integer) obj4).intValue(), ((Integer) obj5).intValue());
                    }
                }, Integer.valueOf(displayId), Integer.valueOf(rectangle.left), Integer.valueOf(rectangle.top), Integer.valueOf(rectangle.right), Integer.valueOf(rectangle.bottom));
                this.mHandler.sendMessage(m);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class AccessibilityTracing {
        private static final int BUFFER_CAPACITY = 12582912;
        private static final int CPU_STATS_COUNT = 5;
        private static final long MAGIC_NUMBER_VALUE = 4846245196254490945L;
        private static final String TAG = "AccessibilityTracing";
        private static final String TRACE_FILENAME = "/data/misc/a11ytrace/a11y_trace.winscope";
        private static AccessibilityTracing sInstance;
        private volatile boolean mEnabled;
        private final LogHandler mHandler;
        private final WindowManagerService mService;
        private final Object mLock = new Object();
        private final File mTraceFile = new File(TRACE_FILENAME);
        private final TraceBuffer mBuffer = new TraceBuffer((int) BUFFER_CAPACITY);

        static AccessibilityTracing getInstance(WindowManagerService service) {
            AccessibilityTracing accessibilityTracing;
            synchronized (AccessibilityController.STATIC_LOCK) {
                if (sInstance == null) {
                    sInstance = new AccessibilityTracing(service);
                }
                accessibilityTracing = sInstance;
            }
            return accessibilityTracing;
        }

        AccessibilityTracing(WindowManagerService service) {
            this.mService = service;
            HandlerThread workThread = new HandlerThread(TAG);
            workThread.start();
            this.mHandler = new LogHandler(workThread.getLooper());
        }

        void startTrace() {
            if (Build.IS_USER) {
                Slog.e(TAG, "Error: Tracing is not supported on user builds.");
                return;
            }
            synchronized (this.mLock) {
                this.mEnabled = true;
                this.mBuffer.resetBuffer();
            }
        }

        void stopTrace() {
            if (Build.IS_USER) {
                Slog.e(TAG, "Error: Tracing is not supported on user builds.");
                return;
            }
            synchronized (this.mLock) {
                this.mEnabled = false;
                if (this.mEnabled) {
                    Slog.e(TAG, "Error: tracing enabled while waiting for flush.");
                } else {
                    writeTraceToFile();
                }
            }
        }

        boolean isEnabled() {
            return this.mEnabled;
        }

        void logState(String where, long loggingTypes) {
            if (!this.mEnabled) {
                return;
            }
            logState(where, loggingTypes, "");
        }

        void logState(String where, long loggingTypes, String callingParams) {
            if (!this.mEnabled) {
                return;
            }
            logState(where, loggingTypes, callingParams, "".getBytes());
        }

        void logState(String where, long loggingTypes, String callingParams, byte[] a11yDump) {
            if (!this.mEnabled) {
                return;
            }
            logState(where, loggingTypes, callingParams, a11yDump, Binder.getCallingUid(), new HashSet(Arrays.asList("logState")));
        }

        void logState(String where, long loggingTypes, String callingParams, byte[] a11yDump, int callingUid, Set<String> ignoreStackEntries) {
            if (!this.mEnabled) {
                return;
            }
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            ignoreStackEntries.add("logState");
            logState(where, loggingTypes, callingParams, a11yDump, callingUid, stackTraceElements, ignoreStackEntries);
        }

        void logState(String where, long loggingTypes, String callingParams, byte[] a11yDump, int callingUid, StackTraceElement[] stackTrace, Set<String> ignoreStackEntries) {
            if (!this.mEnabled) {
                return;
            }
            log(where, loggingTypes, callingParams, a11yDump, callingUid, stackTrace, SystemClock.elapsedRealtimeNanos(), Process.myPid() + ":" + Application.getProcessName(), Thread.currentThread().getId() + ":" + Thread.currentThread().getName(), ignoreStackEntries);
        }

        void logState(String where, long loggingTypes, String callingParams, byte[] a11yDump, int callingUid, StackTraceElement[] callingStack, long timeStamp, int processId, long threadId, Set<String> ignoreStackEntries) {
            if (!this.mEnabled) {
                return;
            }
            log(where, loggingTypes, callingParams, a11yDump, callingUid, callingStack, timeStamp, String.valueOf(processId), String.valueOf(threadId), ignoreStackEntries);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public String toStackTraceString(StackTraceElement[] stackTraceElements, Set<String> ignoreStackEntries) {
            if (stackTraceElements == null) {
                return "";
            }
            StringBuilder stringBuilder = new StringBuilder();
            int i = 0;
            int firstMatch = -1;
            while (i < stackTraceElements.length) {
                Iterator<String> it = ignoreStackEntries.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    String ele = it.next();
                    if (stackTraceElements[i].toString().contains(ele)) {
                        firstMatch = i;
                        break;
                    }
                }
                if (firstMatch >= 0) {
                    break;
                }
                i++;
            }
            int lastMatch = firstMatch;
            if (i < stackTraceElements.length) {
                while (true) {
                    i++;
                    if (i >= stackTraceElements.length) {
                        break;
                    }
                    Iterator<String> it2 = ignoreStackEntries.iterator();
                    while (true) {
                        if (!it2.hasNext()) {
                            break;
                        }
                        String ele2 = it2.next();
                        if (stackTraceElements[i].toString().contains(ele2)) {
                            lastMatch = i;
                            break;
                        }
                    }
                    if (lastMatch != i) {
                        break;
                    }
                }
            }
            for (int i2 = lastMatch + 1; i2 < stackTraceElements.length; i2++) {
                stringBuilder.append(stackTraceElements[i2].toString()).append("\n");
            }
            return stringBuilder.toString();
        }

        private void log(String where, long loggingTypes, String callingParams, byte[] a11yDump, int callingUid, StackTraceElement[] callingStack, long timeStamp, String processName, String threadName, Set<String> ignoreStackEntries) {
            SomeArgs args = SomeArgs.obtain();
            args.argl1 = timeStamp;
            args.argl2 = loggingTypes;
            args.arg1 = where;
            args.arg2 = processName;
            args.arg3 = threadName;
            args.arg4 = ignoreStackEntries;
            args.arg5 = callingParams;
            args.arg6 = callingStack;
            args.arg7 = a11yDump;
            this.mHandler.obtainMessage(1, callingUid, 0, args).sendToTarget();
        }

        void writeTraceToFile() {
            this.mHandler.sendEmptyMessage(2);
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public class LogHandler extends Handler {
            public static final int MESSAGE_LOG_TRACE_ENTRY = 1;
            public static final int MESSAGE_WRITE_FILE = 2;

            LogHandler(Looper looper) {
                super(looper);
            }

            @Override // android.os.Handler
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        SomeArgs args = (SomeArgs) message.obj;
                        try {
                            ProtoOutputStream os = new ProtoOutputStream();
                            PackageManagerInternal pmInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                            long tokenOuter = os.start(2246267895810L);
                            long reportedTimeStampNanos = args.argl1;
                            long currentElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
                            long timeDiffNanos = currentElapsedRealtimeNanos - reportedTimeStampNanos;
                            long currentTimeMillis = new Date().getTime();
                            long reportedTimeMillis = currentTimeMillis - (timeDiffNanos / 1000000);
                            SimpleDateFormat fm = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
                            os.write(1125281431553L, reportedTimeStampNanos);
                            os.write(1138166333442L, fm.format(Long.valueOf(reportedTimeMillis)).toString());
                            long loggingTypes = args.argl2;
                            List<String> loggingTypeNames = AccessibilityTrace.getNamesOfLoggingTypes(loggingTypes);
                            for (String type : loggingTypeNames) {
                                try {
                                    SimpleDateFormat fm2 = fm;
                                    long loggingTypes2 = loggingTypes;
                                    os.write(CompanionAppsPermissions.AppPermissions.PERMISSION, type);
                                    fm = fm2;
                                    loggingTypes = loggingTypes2;
                                } catch (Exception e) {
                                    e = e;
                                    Slog.e(AccessibilityTracing.TAG, "Exception while tracing state", e);
                                    return;
                                }
                            }
                            os.write(1138166333446L, (String) args.arg1);
                            os.write(1138166333444L, (String) args.arg2);
                            os.write(1138166333445L, (String) args.arg3);
                            os.write(1138166333447L, pmInternal.getNameForUid(message.arg1));
                            os.write(1138166333448L, (String) args.arg5);
                            String callingStack = AccessibilityTracing.this.toStackTraceString((StackTraceElement[]) args.arg6, (Set) args.arg4);
                            os.write(1138166333449L, callingStack);
                            os.write(1146756268042L, (byte[]) args.arg7);
                            long tokenInner = os.start(1146756268043L);
                            try {
                                try {
                                    synchronized (AccessibilityTracing.this.mService.mGlobalLock) {
                                        try {
                                            WindowManagerService.boostPriorityForLockedSection();
                                            AccessibilityTracing.this.mService.dumpDebugLocked(os, 0);
                                            WindowManagerService.resetPriorityAfterLockedSection();
                                            os.end(tokenInner);
                                            os.write(1138166333452L, AccessibilityTracing.this.printCpuStats(reportedTimeStampNanos));
                                            os.end(tokenOuter);
                                            synchronized (AccessibilityTracing.this.mLock) {
                                                AccessibilityTracing.this.mBuffer.add(os);
                                            }
                                            return;
                                        } catch (Throwable th) {
                                            th = th;
                                            WindowManagerService.resetPriorityAfterLockedSection();
                                            throw th;
                                        }
                                    }
                                } catch (Exception e2) {
                                    e = e2;
                                    Slog.e(AccessibilityTracing.TAG, "Exception while tracing state", e);
                                    return;
                                }
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        } catch (Exception e3) {
                            e = e3;
                        }
                    case 2:
                        synchronized (AccessibilityTracing.this.mLock) {
                            AccessibilityTracing.this.writeTraceToFileInternal();
                        }
                        return;
                    default:
                        return;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void writeTraceToFileInternal() {
            try {
                ProtoOutputStream proto = new ProtoOutputStream();
                proto.write(1125281431553L, MAGIC_NUMBER_VALUE);
                this.mBuffer.writeTraceToFile(this.mTraceFile, proto);
            } catch (IOException e) {
                Slog.e(TAG, "Unable to write buffer to file", e);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public String printCpuStats(long timeStampNanos) {
            Pair<String, String> stats = this.mService.mAmInternal.getAppProfileStatsForDebugging(timeStampNanos, 5);
            return ((String) stats.first) + ((String) stats.second);
        }
    }
}
