package com.transsion.hubcore.server.wm;

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
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.WindowManagerService;
import com.transsion.hubcore.server.wm.ITranActivityTaskManagerService;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.List;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranActivityTaskManagerService {
    public static final TranClassInfo<ITranActivityTaskManagerService> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranActivityTaskManagerServiceImpl", ITranActivityTaskManagerService.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranActivityTaskManagerService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranActivityTaskManagerService.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranActivityTaskManagerService {
    }

    static ITranActivityTaskManagerService Instance() {
        return (ITranActivityTaskManagerService) classInfo.getImpl();
    }

    default void onConstruct(Context context, ActivityTaskManagerService services, WindowManagerService mWindowManager, String tag) {
    }

    default boolean inMultiWindow(boolean inMultiWindow) {
        return false;
    }

    default void hookStartActivityResult(Object mStartActivityLock, int result, Rect location) {
    }

    default boolean getMuteState(int multiWindowId) {
        return false;
    }

    default void setMuteState(boolean state, int multiWindowId) {
    }

    default void hookMultiWindowMuteAethenV4(int type) {
    }

    default void removeFromMuteState(int multiWindowId) {
    }

    default void hookMultiWindowLocation(int tempDisplayAreaId, int x, int y, int touchX, int touchY) {
    }

    default void setStartInMultiWindow(boolean isSpecialScenario, String pkgName, int type, int direction, int startType) {
    }

    default boolean isInBlackList(String pkgName) {
        return false;
    }

    default String getMultiWindowVersion() {
        return null;
    }

    default boolean inMultiWindow(ActivityRecord r) {
        return false;
    }

    default void showUnSupportMultiToast() {
    }

    default void showSceneUnSupportMultiToast() {
    }

    default void hookMultiWindowVisible() {
    }

    default void hookOnFirstWindowDrawnV3() {
    }

    default void hookOnFirstWindowDrawnV4(int multiWindowMode, int multiWindowId) {
    }

    default void hookMultiWindowInvisible() {
    }

    default void hookMultiWindowStartAethen(int startType, String pkgName) {
    }

    default void hookActiveMultiWindowMoveStartV3() {
    }

    default void hookActiveMultiWindowMoveStartV4(int multiWindowMode, int multiWindowId) {
    }

    default void hookNotifyDefaultDisplayRotationResult() {
    }

    default void hookSetTaskSplitManagerProxy(ITaskSplitManager taskSplitManager) {
    }

    default void hookRegisterTaskSplitListener(ITaskSplitManagerListener taskSplitListener) {
    }

    default void hookUnregisterTaskSplitListener(ITaskSplitManagerListener taskSplitListener) {
    }

    default void hookNotifyTaskAnimationResult(int taskId, ITaskAnimation taskAnimation) {
    }

    default ITaskAnimation hookCreateTaskAnimation(IBinder token, int taskId, MultiTaskRemoteAnimationAdapter callback) {
        return null;
    }

    default void hookActiveMultiWindowStartToMove(IBinder token, int multiWindowMode, int multiWindowId, MotionEvent event, Point downPoint) {
    }

    default void hookActiveMultiWindowMove(int multiWindowMode, int multiWindowId, MotionEvent event) {
    }

    default void hookActiveMultiWindowEndMove(int multiWindowMode, int multiWindowId, MotionEvent event) {
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

    default void hookExitSplitScreenToMultiWindow(int multiWindowMode) {
    }

    default void hookStartMultiWindowFromSplitScreen(int fullTaskId, WindowContainerToken multiTaskToken, Rect multiWindowRegion, IWindowContainerTransactionCallbackSync syncCallback, int type) {
    }

    default void hookStartMultiWindow(WindowContainerToken taskToken, String packageName, Rect multiWindowRegion, IWindowContainerTransactionCallback callback) {
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

    default void setMultiWindowParams(Bundle params) {
    }

    default boolean hookStartActivity(String callerPkg, Intent intent) {
        return false;
    }

    default void hookOnCleanUpApplicationRecord() {
    }

    default void startActivityHook(String callingPackage, Intent intent) {
    }

    default void boostSceneStart(int scene) {
    }

    default void boostSceneStartDuration(int scene, long durationMs) {
    }

    default void boostSceneEnd(int scene) {
    }

    default void boostSceneEndDelay(int scene, long delayMs) {
    }

    default void setConnectBlackListToSystem(List<String> list) {
    }

    default List<String> getConnectBlackList() {
        return null;
    }

    default void hookReserveMultiWindowNumber(int reserveNum, long showDelayTime) {
    }

    default void hookStartLaunchPowerMode(int reason) {
    }

    default void boostStartForLauncher() {
    }

    default void boostEndForLauncher() {
    }

    default void hookBoostTransitAnimation(long duration, boolean enter, String callerPkg, boolean isActivityTransitOld, boolean isKeyguardGoingAwayTransitOld, boolean isClosingTransitOld) {
    }

    default void boostStartInLauncher(int type) {
    }

    default void boostEndInLauncher(int type) {
    }

    default void hookBoostAnimation(long duration, boolean enter, ActivityRecord activityRecord, boolean isActivityTransitOld, boolean isKeyguardGoingAwayTransitOld, boolean isClosingTransitOld) {
    }

    default boolean isNightDisplayActivated() {
        return false;
    }

    default boolean hasMultiWindow() {
        return false;
    }

    default void handleDrawThreadsBoost(boolean handleAllThreads, boolean bindBigCore, List<String> handleActName) {
    }

    default void resetBoostThreads() {
    }
}
