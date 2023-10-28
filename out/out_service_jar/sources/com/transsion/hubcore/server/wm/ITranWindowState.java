package com.transsion.hubcore.server.wm;

import android.content.res.Configuration;
import android.view.IWindow;
import android.view.InsetsState;
import android.view.SurfaceControl;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.WindowState;
import com.transsion.hubcore.server.wm.ITranWindowState;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranWindowState {
    public static final TranClassInfo<ITranWindowState> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranWindowStateImpl", ITranWindowState.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranWindowState$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranWindowState.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranWindowState {
    }

    static ITranWindowState Instance() {
        return (ITranWindowState) classInfo.getImpl();
    }

    default int getDreamAnimation() {
        return 0;
    }

    default void hookAodWindowRemove() {
    }

    default void hookAodWindowFinishDraw() {
    }

    default boolean isAodWallpaperFeatureEnabled() {
        return false;
    }

    default int getAnimationTypeNormal() {
        return 0;
    }

    default int getAnimationTypeDreamKeyguard() {
        return 0;
    }

    default int getAnimationTypeDreamAod() {
        return 0;
    }

    default int getAnimationFromAodToKeyguard() {
        return 0;
    }

    default int getAnimationFromKeyguardToAod() {
        return 0;
    }

    default int selectAnimation(int animationType, int transit) {
        return 0;
    }

    default void onConstruct(String tag) {
    }

    default boolean isThunderbackWindow(Configuration currentConfiguration) {
        return false;
    }

    default InsetsState getInsetsState(WindowState windowState, InsetsState state) {
        return state;
    }

    default boolean isUpdateImeForcely(Configuration newParentConfig, Configuration currentParentConfig) {
        return false;
    }

    default SurfaceControl createWeltAnimationLeash(WindowState windowState, boolean sfHasLeash, boolean saHasLeash, SurfaceControl.Transaction t, int width, int height, int x, int y, boolean hidden) {
        return null;
    }

    default void removeWeltLeash(WindowState windowState, SurfaceControl.Transaction t) {
    }

    default void updateCaptionViewStatusIfNeeded(WindowState windowState, IWindow mClient, ActivityRecord mActivityRecord) {
    }

    default SurfaceControl createDragAndZoomBgLeash(WindowState windowState, boolean sfHasLeash, boolean saHasLeash, SurfaceControl.Transaction t, int width, int height, int x, int y, boolean hidden) {
        return null;
    }

    default boolean isInMultiWindow(Configuration currentConfiguration) {
        return false;
    }

    default String getLastTitle(WindowState windowState) {
        return "";
    }

    default boolean isVisibleOnePixelWindow(WindowState windowState) {
        return false;
    }

    default boolean needHookWindowState() {
        return false;
    }

    default void updateFrameRateSelectionPriorityIfNeeded(WindowState windowState) {
    }
}
