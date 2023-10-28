package com.android.server.wm;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Pair;
import android.view.ContentRecordingSession;
import android.view.Display;
import android.view.IInputFilter;
import android.view.IRemoteAnimationFinishedCallback;
import android.view.IWindow;
import android.view.InputChannel;
import android.view.MagnificationSpec;
import android.view.RemoteAnimationTarget;
import android.view.SurfaceControl;
import android.view.SurfaceControlViewHost;
import android.view.WindowInfo;
import com.android.internal.policy.KeyInterceptionInfo;
import com.android.server.input.InputManagerService;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Set;
/* loaded from: classes2.dex */
public abstract class WindowManagerInternal {

    /* loaded from: classes2.dex */
    public interface AccessibilityControllerInternal {

        /* loaded from: classes2.dex */
        public interface UiChangesForAccessibilityCallbacks {
            void onRectangleOnScreenRequested(int i, int i2, int i3, int i4, int i5);
        }

        boolean isAccessibilityTracingEnabled();

        void logTrace(String str, long j, String str2, byte[] bArr, int i, StackTraceElement[] stackTraceElementArr, long j2, int i2, long j3, Set<String> set);

        void logTrace(String str, long j, String str2, byte[] bArr, int i, StackTraceElement[] stackTraceElementArr, Set<String> set);

        void setUiChangesForAccessibilityCallbacks(UiChangesForAccessibilityCallbacks uiChangesForAccessibilityCallbacks);

        void startTrace(long j);

        void stopTrace();
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface ImeClientFocusResult {
        public static final int DISPLAY_ID_MISMATCH = -2;
        public static final int HAS_IME_FOCUS = 0;
        public static final int INVALID_DISPLAY_ID = -3;
        public static final int NOT_IME_TARGET_WINDOW = -1;
    }

    /* loaded from: classes2.dex */
    public interface KeyguardExitAnimationStartListener {
        void onAnimationStart(RemoteAnimationTarget[] remoteAnimationTargetArr, RemoteAnimationTarget[] remoteAnimationTargetArr2, IRemoteAnimationFinishedCallback iRemoteAnimationFinishedCallback);
    }

    /* loaded from: classes2.dex */
    public interface MagnificationCallbacks {
        void onDisplaySizeChanged();

        void onImeWindowVisibilityChanged(boolean z);

        void onMagnificationRegionChanged(Region region);

        void onRectangleOnScreenRequested(int i, int i2, int i3, int i4);

        void onUserContextChanged();
    }

    /* loaded from: classes2.dex */
    public interface OnHardKeyboardStatusChangeListener {
        void onHardKeyboardStatusChange(boolean z);
    }

    /* loaded from: classes2.dex */
    public interface TaskSystemBarsListener {
        void onTransientSystemBarsVisibilityChanged(int i, boolean z, boolean z2);
    }

    /* loaded from: classes2.dex */
    public interface WindowsForAccessibilityCallback {
        void onWindowsForAccessibilityChanged(boolean z, int i, IBinder iBinder, List<WindowInfo> list);
    }

    public abstract void addRefreshRateRangeForPackage(String str, float f, float f2);

    public abstract void addTrustedTaskOverlay(int i, SurfaceControlViewHost.SurfacePackage surfacePackage);

    public abstract void addWindowToken(IBinder iBinder, int i, int i2, Bundle bundle);

    public abstract void clearForcedDisplaySize(int i);

    public abstract void clearSnapshotCache();

    public abstract void computeWindowsForAccessibility(int i);

    public abstract AccessibilityControllerInternal getAccessibilityController();

    public abstract int getDisplayIdForWindow(IBinder iBinder);

    public abstract int getDisplayImePolicy(int i);

    public abstract IBinder getFocusedWindowToken();

    public abstract IBinder getFocusedWindowTokenFromWindowStates();

    public abstract SurfaceControl getHandwritingSurfaceForDisplay(int i);

    public abstract int getInputMethodWindowVisibleHeight(int i);

    public abstract KeyInterceptionInfo getKeyInterceptionInfoFromToken(IBinder iBinder);

    public abstract void getMagnificationRegion(int i, Region region);

    public abstract int getTopFocusedDisplayId();

    public abstract Context getTopFocusedDisplayUiContext();

    public abstract void getWindowFrame(IBinder iBinder, Rect rect);

    public abstract String getWindowName(IBinder iBinder);

    public abstract int getWindowOwnerUserId(IBinder iBinder);

    public abstract Pair<Matrix, MagnificationSpec> getWindowTransformationMatrixAndMagnificationSpec(IBinder iBinder);

    public abstract int hasInputMethodClientFocus(IBinder iBinder, int i, int i2, int i3);

    public abstract void hideIme(IBinder iBinder, int i);

    public abstract boolean isHardKeyboardAvailable();

    public abstract boolean isKeyguardLocked();

    public abstract boolean isKeyguardShowingAndNotOccluded();

    public abstract boolean isPointInsideWindow(IBinder iBinder, int i, float f, float f2);

    public abstract boolean isTouchOrFaketouchDevice();

    public abstract boolean isUidAllowedOnDisplay(int i, int i2);

    public abstract boolean isUidFocused(int i);

    public abstract void lockNow();

    public abstract void moveWindowTokenToDisplay(IBinder iBinder, int i);

    public abstract ImeTargetInfo onToggleImeRequested(boolean z, IBinder iBinder, IBinder iBinder2, int i);

    public abstract void registerAppTransitionListener(AppTransitionListener appTransitionListener);

    public abstract void registerDragDropControllerCallback(IDragDropCallback iDragDropCallback);

    public abstract void registerKeyguardExitAnimationStartListener(KeyguardExitAnimationStartListener keyguardExitAnimationStartListener);

    public abstract void registerTaskSystemBarsListener(TaskSystemBarsListener taskSystemBarsListener);

    public abstract void relayoutWindowForDreamAnimation();

    public abstract void removeRefreshRateRangeForPackage(String str);

    public abstract void removeTrustedTaskOverlay(int i, SurfaceControlViewHost.SurfacePackage surfacePackage);

    public abstract void removeWindowToken(IBinder iBinder, boolean z, boolean z2, int i);

    public abstract void reportPasswordChanged(int i);

    public abstract void requestTraversalFromDisplayManager();

    public abstract void setAccessibilityIdToSurfaceMetadata(IBinder iBinder, int i);

    public abstract void setContentRecordingSession(ContentRecordingSession contentRecordingSession);

    public abstract void setDismissImeOnBackKeyPressed(boolean z);

    public abstract void setForceShowMagnifiableBounds(int i, boolean z);

    public abstract void setForcedDisplaySize(int i, int i2, int i3);

    public abstract void setInputFilter(IInputFilter iInputFilter);

    public abstract boolean setMagnificationCallbacks(int i, MagnificationCallbacks magnificationCallbacks);

    public abstract void setMagnificationSpec(int i, MagnificationSpec magnificationSpec);

    public abstract void setOnHardKeyboardStatusChangeListener(OnHardKeyboardStatusChangeListener onHardKeyboardStatusChangeListener);

    public abstract void setVr2dDisplayId(int i);

    public abstract void setWindowsForAccessibilityCallback(int i, WindowsForAccessibilityCallback windowsForAccessibilityCallback);

    public abstract boolean shouldRestoreImeVisibility(IBinder iBinder);

    public abstract boolean shouldShowSystemDecorOnDisplay(int i);

    public abstract void showGlobalActions();

    public abstract void showImePostLayout(IBinder iBinder);

    public abstract void unregisterTaskSystemBarsListener(TaskSystemBarsListener taskSystemBarsListener);

    public abstract void updateInputMethodTargetWindow(IBinder iBinder, IBinder iBinder2);

    public abstract void updateRefreshRateForScene(Bundle bundle);

    public abstract void updateRefreshRateForVideoScene(int i, int i2, int i3);

    public abstract void waitForAllWindowsDrawn(Runnable runnable, long j, int i);

    /* loaded from: classes2.dex */
    public static abstract class AppTransitionListener {
        public void onAppTransitionPendingLocked() {
        }

        public void onAppTransitionCancelledLocked(boolean keyguardGoingAway) {
        }

        public void onAppTransitionTimeoutLocked() {
        }

        public int onAppTransitionStartingLocked(boolean keyguardGoingAway, boolean keyguardOccluding, long duration, long statusBarAnimationStartTime, long statusBarAnimationDuration) {
            return 0;
        }

        public void onAppTransitionFinishedLocked(IBinder token) {
        }
    }

    /* loaded from: classes2.dex */
    public interface IDragDropCallback {
        default boolean registerInputChannel(DragState state, Display display, InputManagerService service, InputChannel source) {
            state.register(display);
            return service.transferTouchFocus(source, state.getInputChannel(), true);
        }

        default boolean prePerformDrag(IWindow window, IBinder dragToken, int touchSource, float touchX, float touchY, float thumbCenterX, float thumbCenterY, ClipData data) {
            return true;
        }

        default void postPerformDrag() {
        }

        default void preReportDropResult(IWindow window, boolean consumed) {
        }

        default void postReportDropResult() {
        }

        default void preCancelDragAndDrop(IBinder dragToken) {
        }

        default void postCancelDragAndDrop() {
        }

        default void dragRecipientEntered(IWindow window) {
        }

        default void dragRecipientExited(IWindow window) {
        }

        default boolean getDropConsumedState(boolean defaultValue) {
            return defaultValue;
        }

        default void handleMotionEvent(float x, float y) {
        }

        default void onDump(PrintWriter pw) {
        }
    }

    public final void removeWindowToken(IBinder token, boolean removeWindows, int displayId) {
        removeWindowToken(token, removeWindows, true, displayId);
    }

    /* loaded from: classes2.dex */
    public static class ImeTargetInfo {
        public final String focusedWindowName;
        public final String imeControlTargetName;
        public final String imeLayerTargetName;
        public final String requestWindowName;

        public ImeTargetInfo(String focusedWindowName, String requestWindowName, String imeControlTargetName, String imeLayerTargetName) {
            this.focusedWindowName = focusedWindowName;
            this.requestWindowName = requestWindowName;
            this.imeControlTargetName = imeControlTargetName;
            this.imeLayerTargetName = imeLayerTargetName;
        }
    }
}
