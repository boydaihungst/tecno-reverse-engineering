package com.transsion.hubcore.server.wm;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.window.WindowContainerToken;
import com.android.server.wm.WindowState;
import com.transsion.hubcore.server.wm.ITranRootWindowContainer;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranRootWindowContainer {
    public static final TranClassInfo<ITranRootWindowContainer> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranRootWindowContainerImpl", ITranRootWindowContainer.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranRootWindowContainer$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranRootWindowContainer.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranRootWindowContainer {
    }

    static ITranRootWindowContainer Instance() {
        return (ITranRootWindowContainer) classInfo.getImpl();
    }

    default boolean isThunderbackWindow(boolean isConfig) {
        return false;
    }

    default void hookShowMultiDisplayWindowV3(WindowContainerToken taskToken, SurfaceControl surfaceControl, boolean canLayoutInShotCutOut, boolean initiative) {
    }

    default void hookShowMultiDisplayWindowV4(WindowContainerToken taskToken, int taskId, SurfaceControl surfaceControl, boolean canLayoutInShotCutOut, int triggerMode, String packageName) {
    }

    default void showUnSupportMultiToast() {
    }

    default void showSceneUnSupportMultiToast() {
    }

    default void hookMultiWindowToMax(int displayAreaId, WindowContainerToken displayToken, WindowContainerToken taskToken, boolean canLayoutInShotCutOut) {
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

    default void hookFinishMovingLocation(int displayAreaId) {
    }

    default void hookMultiWindowFling(int displayAreaId, MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
    }

    default void hookMultiWindowToSplit(int multiWindowMode, int multiWindowId) {
    }

    default void hookDisplayAreaChildCountV3(int displayAreaId, int taskCount) {
    }

    default void hookDisplayAreaChildCountV4(int multiWindowMode, int multiWindowId, int taskCount) {
    }

    default void removeWeltLeash(WindowState win, SurfaceControl.Transaction t) {
    }

    default void minimizeMultiWinToEdge(int multiWindowMode, int multiWindowId, boolean toEdge) {
    }

    default Rect getMultiWindowContentRegion(int multiWindowMode, int multiWindowId) {
        return null;
    }

    default Bundle getMultiWindowParams(int multiWindowId) {
        return null;
    }
}
