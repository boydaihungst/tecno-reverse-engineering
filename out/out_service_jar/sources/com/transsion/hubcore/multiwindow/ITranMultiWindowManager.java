package com.transsion.hubcore.multiwindow;

import android.app.ITaskAnimation;
import android.app.ITaskSplitManager;
import android.app.ITaskSplitManagerListener;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MotionEvent;
import android.view.MultiTaskRemoteAnimationAdapter;
import android.view.SurfaceControl;
import android.window.IWindowContainerTransactionCallback;
import android.window.IWindowContainerTransactionCallbackSync;
import android.window.WindowContainerToken;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranMultiWindowManager {
    public static final TranClassInfo<ITranMultiWindowManager> classInfo = new TranClassInfo<>("com.transsion.hubcore.multiwindow.TranMultiWindowManagerImpl", ITranMultiWindowManager.class, new Supplier() { // from class: com.transsion.hubcore.multiwindow.ITranMultiWindowManager$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranMultiWindowManager.lambda$static$0();
        }
    });

    static /* synthetic */ ITranMultiWindowManager lambda$static$0() {
        return new ITranMultiWindowManager() { // from class: com.transsion.hubcore.multiwindow.ITranMultiWindowManager.1
        };
    }

    static ITranMultiWindowManager Instance() {
        return (ITranMultiWindowManager) classInfo.getImpl();
    }

    default void init(Context context) {
    }

    default void onSystemUiStarted() {
    }

    default void changeMultiWindowLocation(int x, int y) {
    }

    default void notifyToUse(String pkgName) {
    }

    default void hookFinishBoot() {
    }

    default void showSceneUnSupportMultiToast() {
    }

    default void showMultiFailedToast() {
    }

    default void showUnSupportMultiToast() {
    }

    default void showUnSupportOtherToast() {
    }

    default void showStabilityMultiToast(String pkgName) {
    }

    default void hookInputMethodShown(boolean inputShown) {
    }

    default void hookShowMultiDisplayWindowV3(WindowContainerToken taskToken, SurfaceControl surfaceControl, boolean canLayoutInShotCutOut, boolean initiative) {
    }

    default void hookShowMultiDisplayWindowV4(WindowContainerToken taskToken, int taskId, SurfaceControl surfaceControl, boolean usShotEdgeCutout, int triggerMode, String packageName) {
    }

    default void hookRequestedOrientation(int displayAreaId, int orientation) {
    }

    default void hookExitSplitScreenToMultiWindow(int multiWindowMode) {
    }

    default Rect getMultiWindowDefaultRect() {
        return null;
    }

    default Rect hookGetMultiWindowDefaultRect(int orientationType) {
        return null;
    }

    default Rect hookGetMultiWindowDefaultRectByTask(int taskId) {
        return null;
    }

    default void hookSetMultiWindowDefaultRectResult(Rect rect) {
    }

    default void minimizeMultiWinToEdge(int multiWindowMode, int multiWindowId, boolean toEdge) {
    }

    default Rect getMultiWindowContentRegion(int multiWindowMode, int multiWindowId) {
        return null;
    }

    default void hookStartMultiWindowFromSplitScreen(int fullTaskId, WindowContainerToken multiTaskToken, Rect multiWindowRegion, IWindowContainerTransactionCallbackSync syncCallback, int type) {
    }

    default void hookStartMultiWindow(WindowContainerToken taskToken, String packageName, Rect multiWindowRegion, IWindowContainerTransactionCallback callback) {
    }

    default void hookRequestedOrientationV3(int displayAreaId, int orientation) {
    }

    default void hookRequestedOrientationV4(int multiWindowMode, int multiWindowId, int orientation) {
    }

    default void hookMultiWindowToMax(int displayAreaId, WindowContainerToken displayToken, WindowContainerToken taskToken, boolean canLayoutInShotEdgeCutout) {
    }

    default void hookReparentToDefaultDisplay(int multiWindowMode, int multiWindowId, WindowContainerToken displayToken, WindowContainerToken taskToken, boolean canLayoutInShotEdgeCutout) {
    }

    default void hookMultiWindowToMin(int displayAreaId) {
    }

    default void hookMultiWindowToCloseV3(int displayAreaId) {
    }

    default void hookMultiWindowToCloseV4(int multiWindowMode, int multiWindowId) {
    }

    default void hookMultiWindowToSmallV3(int displayAreaId) {
    }

    default void hookMultiWindowToSmallV4(int multiWindowMode, int multiWindowId) {
    }

    default void hookMultiWindowToLarge(int displayAreaId) {
    }

    default void hookMultiWindowToSplit(int multiWindowMode, int multiWindowId) {
    }

    default void hookMultiWindowLocation(int displayAreaId, int x, int y, int touchX, int touchY) {
    }

    default void hookFinishMovingLocation(int displayAreaId) {
    }

    default void hookMultiWindowFling(int displayAreaId, MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
    }

    default void hookFocusOnFloatWindow(boolean focusOnFloatWindow) {
    }

    default void hookDefaultDisplayRotation(int rotation) {
    }

    default void hookStartActivityResult(int result, Rect location) {
    }

    default void hookNotifyDefaultDisplayRotationResult() {
    }

    default void hookDisplayAreaChildCountV3(int displayAreaId, int taskCount) {
    }

    default void hookDisplayAreaChildCountV4(int multiWindowMode, int multiWindowId, int taskCount) {
    }

    default void hookMultiWindowVisible() {
    }

    default void hookMultiWindowInvisible() {
    }

    default int hookStartActivity(Intent intent, int direction, boolean initiative) {
        return -1;
    }

    default void hookFixedRotationLaunch(int appRotation, int displayRotation, boolean isEnterMultiWin) {
    }

    default void hookActiveMultiWindowMoveStartV3() {
    }

    default void hookActiveMultiWindowMoveStartV4(int multiWindowMode, int multiWindowId) {
    }

    default void hookOnFirstWindowDrawnV3() {
    }

    default void hookOnFirstWindowDrawnV4(int multiWindowMode, int multiWindowId) {
    }

    default void hookMultiWindowHideShadow() {
    }

    default void hookMultiWindowStartAethen(int way, String pkgName) {
    }

    default void hookMultiWindowMuteAethen() {
    }

    default void hookMultiWindowMuteAethenV4(int type) {
    }

    default void hookMultiWindowCloseAethen() {
    }

    default void hookDefaultDisplayFixRotation() {
    }

    default void hookNotifyTaskAnimationResult(int taskId, ITaskAnimation taskAnimation) {
    }

    default ITaskAnimation hookCreateTaskAnimation(IBinder token, int taskId, MultiTaskRemoteAnimationAdapter callback) {
        return null;
    }

    default void hookSetTaskSplitManagerProxy(ITaskSplitManager taskSplitManager) {
    }

    default void hookRegisterTaskSplitListener(ITaskSplitManagerListener taskSplitListener) {
    }

    default void hookUnregisterTaskSplitListener(ITaskSplitManagerListener taskSplitListener) {
    }

    default void hookActiveMultiWindowStartToMove(IBinder token, int multiWindowMode, int multiWindowId, MotionEvent event, Point downPoint) {
    }

    default void hookActiveMultiWindowMove(int multiWindowMode, int multiWindowId, MotionEvent event) {
    }

    default void hookActiveMultiWindowEndMove(int multiWindowMode, int multiWindowId, MotionEvent event) {
    }

    default void hookFocusMultiWindow(int multiWindowMode, int multiWindowId) {
    }

    default void hookAddAnimationIconLayer(SurfaceControl sc) {
    }

    default void hookRemoveAnimationIconLayer(SurfaceControl sc) {
    }

    default int hookGetOrCreateMultiWindow(String pkgName, int direction, int triggerMode, int preferId, boolean needCreate) {
        return -1;
    }

    default void hookUpdateRotationFinished() {
    }

    default void setMultiWindowParams(Bundle params) {
    }

    default Bundle getMultiWindowParams(int multiWindowId) {
        return null;
    }

    default void hookMultiWindowToLargeV4(int multiWindowMode, int multiWindowId) {
    }

    default void hookNotifyKeyguardStateV4(int displayId, boolean keyguardShowing, boolean aodShowing) {
    }

    default void hookReserveMultiWindowNumber(int reserveNum, long showDelayTime) {
    }

    default void hookShowBlurLayer(SurfaceControl appSurface, String packageName) {
    }

    default void hookCancelBlurLayer() {
    }

    default void hookKeyguardShowing(boolean keyguardShowing) {
    }
}
