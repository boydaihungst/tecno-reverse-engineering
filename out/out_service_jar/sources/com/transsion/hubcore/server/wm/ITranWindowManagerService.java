package com.transsion.hubcore.server.wm;

import android.app.ActivityOptions;
import android.app.IActivityTaskManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.display.DisplayManagerInternal;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Pair;
import android.view.IWindow;
import android.view.InputChannel;
import android.view.InsetsSourceControl;
import android.view.InsetsState;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.animation.Animation;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.server.SystemService;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.ActivityTaskSupervisor;
import com.android.server.wm.DisplayPolicy;
import com.android.server.wm.IWindowManagerServiceLice;
import com.android.server.wm.WindowManagerGlobalLock;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowProcessController;
import com.android.server.wm.WindowState;
import com.transsion.hubcore.utils.TranClassInfo;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranWindowManagerService {
    public static final TranClassInfo<ITranWindowManagerService> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranWindowManagerServiceImpl", ITranWindowManagerService.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranWindowManagerService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranWindowManagerService.lambda$static$0();
        }
    });

    static /* synthetic */ ITranWindowManagerService lambda$static$0() {
        return new ITranWindowManagerService() { // from class: com.transsion.hubcore.server.wm.ITranWindowManagerService.1
        };
    }

    static ITranWindowManagerService Instance() {
        return (ITranWindowManagerService) classInfo.getImpl();
    }

    default Integer onStartActivityAsUser(Context context, Intent intent, Bundle bOptions, int userId) {
        return null;
    }

    default boolean fixUpFreeformWindowManagement(boolean freeformWindowManagement, boolean supportsMultiWindow, boolean forceResizable) {
        return freeformWindowManagement;
    }

    default void createStackUnchecked(Object stack, int windowingMode, int activityType, int stackId, boolean onTop) {
    }

    default void onSetTargetStackToFreeformTaskRecord(Object intentActivity, Object intentTask, Object launchStack, boolean toTop, int moveStackMode, boolean animate, boolean deferResume, String reason, Object launchParams) {
    }

    default boolean checkFreeformStackVisibilityForOrientation() {
        return true;
    }

    default Pair<Boolean, Animation> loadAnimation(Context context, int userId, WindowManager.LayoutParams lp, int transit, boolean enter, int uiMode, int orientation, Rect frame, Rect displayFrame, Rect insets, Rect surfaceInsets, Rect stableInsets, boolean isVoiceInteraction, boolean freeform) {
        return new Pair<>(false, null);
    }

    default boolean checkFreeformStackVisibilityForSystemBar() {
        return false;
    }

    default boolean useTopFullscreenWindowAsCandidate(int windowingMode, String owningPackage, int type) {
        return false;
    }

    default void fixPersistLaunchParams(Object task, ActivityInfo.WindowLayout layout, Object activity, Object source, ActivityOptions options, Object result, int toStackWindowingMode) {
    }

    default boolean skipSaveLaunchingState(String packageName) {
        return false;
    }

    default boolean validLaunchStackByWindowingMode(int candidateStackWindowingMode, Object r, ActivityOptions options, Object launchParams) {
        return true;
    }

    default Object getFocusedTaskOnUpdateTouchExcludeRegion(Object freeformStack, Object candidateTask) {
        return candidateTask;
    }

    default boolean ondumpD(PrintWriter pw, String[] args, int opti) {
        return false;
    }

    default boolean onOSServerMessage(int messageId, Bundle data, int callingUid, int callingPid) {
        return false;
    }

    default Boolean onTransactIActivityTaskManager(IActivityTaskManager stub, int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        return null;
    }

    default void onProcessMapped(int pid, WindowProcessController proc) {
    }

    default void onProcessUnMapped(int pid) {
    }

    default TranActivityStarterInterceptInfo onActivityStarterIntercept(ActivityInfo aInfo, String callingPackage, String featureId, int callingUid, int userId, Intent intent, String resolvedType, Object resultRecord, int realCallingUid, ActivityTaskSupervisor supervisor, int filterCallingUid, int startFlags, ActivityOptions checkedOptions, int displayId) {
        return null;
    }

    /* loaded from: classes2.dex */
    public static class TranActivityStarterInterceptInfo extends IWindowManagerServiceLice.ActivityStarterInterceptInfo {
        public TranActivityStarterInterceptInfo(IWindowManagerServiceLice.ActivityStarterInterceptInfo info) {
            super(info.intent, info.rInfo, info.aInfo);
        }
    }

    default void onActivityResume(Object next) {
    }

    default boolean onActivityResume(Object next, int displayId) {
        return false;
    }

    default void onMoveTaskToFront(Object next, int displayId) {
    }

    default void restoreOptions(Object request, ActivityOptions originalOptions) {
    }

    default void onMoveTaskToFront(Object next) {
    }

    default void onSetActivityRecordState(Object r, int oldState, int newState) {
    }

    default Integer onUpdateGlobalConfigurationLocked(Context context, Configuration oldConfig, Configuration newConfig) {
        return null;
    }

    default void onRealStartActivityLocked(Object activityRecord, boolean isKeyguardShowing) {
    }

    default void onCommitVisibility(Object appWindowToken, boolean visible) {
    }

    default void onStopActivityLocked(Object activityRecord) {
    }

    default void onStartActivityUnchecked(Object activityRecord) {
    }

    default Boolean onContainsDismissKeyguardWindow(Object appWindowToken) {
        return null;
    }

    default boolean onDismissKeyguard(IKeyguardDismissCallback callback, CharSequence message, Object activityRecord) {
        return false;
    }

    default boolean onDisableKeyguard(IBinder token, String tag, int callingUid, int userId, Runnable r) {
        return false;
    }

    default boolean onStartActivity(ComponentName component, int userId) {
        return false;
    }

    default void exitOneHandMode() {
    }

    default void exitOneHandMode(String reason) {
    }

    default boolean getShowWallpaper(boolean showWallpaper) {
        return false;
    }

    default Pair<Boolean, Integer> updateRotation(int rotation) {
        return null;
    }

    default Pair<Boolean, Boolean> performTraversalInternal(boolean pendingTraversal) {
        return null;
    }

    default int[] configureDisplayLocked(boolean defaultDisplay, int displayRectLeft, int displayRectTop, int displayRectWidth, int displayRectHeight) {
        return null;
    }

    default int getOneHandCurrentState() {
        return 0;
    }

    default void onDrawStateModified(Object windowState, int oldState, int newState) {
    }

    default void onActivityRecordWindowsDrawn(Object activityRecord, int windowsDrawnDelayMs, int launchState) {
    }

    default void onUpdateActivityUsageStats(int event, Object activityRecord) {
    }

    default void onStartActivity(Object activityRecord, int callingPid, int realCallingPid, String callingPackage, String lastStartReason) {
    }

    default void onStartActivityUnchecked(Object reusedActivity, Object activityRecord) {
    }

    default void onStartActivityFromRecents(Object targetActivity, int callingPid) {
    }

    default void onAdjustWindowParamsLw(DisplayPolicy policy, Object windowState, WindowManager.LayoutParams attrs) {
    }

    default boolean onShouldAbortBackgroundActivityStart(Object activityStarter, WindowProcessController callerApp, int callingUid, int callingPid, int callingUidProcState, WindowProcessController realCallerApp, int realCallingUid, int realCallingPid, int realCallingUidProcState) {
        return false;
    }

    default boolean onAdjustBoundsToAvoidConflictInDisplay(Object taskLaunchParamsModifier, Object activityDisplay, Rect inOutBounds, Object taskRecord) {
        return false;
    }

    default void onTaskTapPointerEvent(Object taskTapPointerEventListener, MotionEvent motionEvent) {
    }

    default void onAfterStartTaskPositioningLocked(Object taskPositioningController, boolean started) {
    }

    default void onTaskPositionerReceiveInputEvent(Object taskPosotioner, Object event) {
    }

    default boolean checkAppSwitchAllowedLocked(int sourcePid, int sourceUid, int callingPid, int callingUid, String name) {
        return false;
    }

    default void handleStartActivityResult(int result, Object stack, Object activityRecord, ActivityOptions options) {
    }

    default boolean isCurrentActivityKeepAwake(String className, boolean ignoreActivity) {
        return false;
    }

    default boolean shouldHideWinForKeepAwake(Object windowStats) {
        return false;
    }

    default boolean shouldResetWinForKeepAwake(Object windowStats) {
        return false;
    }

    default boolean getHasHiddedKeepAwake(Object holdScreenWindow, Object oldHoldScreen, Object newHoldScreen) {
        return false;
    }

    default void finishCurrentActivityKeepAwake(boolean keyguardlock) {
    }

    default boolean isVisibilityOnKeyguard(Object activityRecord, boolean shouldBeVisible, boolean isTop, boolean keyguardOrAodShowing, boolean keyguardLocked, boolean showWhenLocked, boolean dismissKeyguard) {
        return true;
    }

    default void onConfigurationChanged(int[] navigationBarHeightForRotationDefault) {
    }

    default void onSetAdjustedForIme(boolean imeVisible, int imeHeight) {
    }

    default void onSwipeFired(int swipe) {
    }

    default boolean requestTransientBars(Object targetWindowState, boolean isNavBar, int navBarPosition, Object insetsControlTarget, Object insetsPolicy) {
        return false;
    }

    default boolean interceptUnknowSource(Context context, Object request) {
        return false;
    }

    default void restoreInstallIntent(Context context, Object request) {
    }

    default void onUpdateFocusedApp(String oldPackageName, ComponentName oldComponent, String newPackageName, ComponentName newComponent) {
    }

    default void onTaskSurfaceShown(Context context, Object task, SurfaceControl.Transaction t) {
    }

    default void onTaskWindowingModeChange(Context context, Object task, int oldWindowingMode, int newWindowingMode) {
    }

    default void onBackgroundActivityPrevented(int uid, String pkg) {
    }

    default int checkAddWindowResultLocked(int res, IWindow client, WindowManager.LayoutParams attrs, int viewVisibility, int displayId, int requestUserId, InsetsState requestedVisibility, InputChannel outInputChannel, InsetsState outInsetsState, InsetsSourceControl[] outActiveControls) {
        return res;
    }

    default void onInitReady() {
    }

    default void onConstruct(WindowManagerService self, ConstructParameters constructParameters) {
    }

    /* loaded from: classes2.dex */
    public interface ConstructParameters {
        default ActivityTaskManagerService getActivityTaskManagerService() {
            return null;
        }

        default WindowManagerGlobalLock getGlobalLock() {
            return null;
        }

        default Context getContext() {
            return null;
        }

        default Looper getDisplayThreadLooper() {
            return null;
        }

        default DisplayManagerInternal getDisplayManagerInternal() {
            return null;
        }

        default WindowManagerPolicy getWindowManagerPolicy() {
            return null;
        }
    }

    default void wmsModeContrl(TranWindowManagerPnPInterface pst) {
    }

    default void setAffinity(int pid, int mask) {
    }

    default void onWindowAdded(WindowState windowState) {
    }

    default void onWindowRemoved(WindowState windowState) {
    }

    default void onLayoutWindowLocked(WindowState windowState, int requestedWidth, int requestedHeight) {
    }

    default boolean onDoDumpData(FileDescriptor fd, PrintWriter pw, String[] args, int opti) {
        return false;
    }

    default void onSetKeyguardShown(int displayId, boolean keyguardShowing, boolean aodShowing) {
    }

    default Pair<Boolean, Float> getCompatScale(String packageName, int uid) {
        return null;
    }

    default void onApplySurfaceChangesTransaction(int dw, int dh) {
    }

    default boolean isInPreWakeupInProgress(WindowState windowState, List<WindowState> outWaitingForDrawn) {
        return false;
    }

    default boolean isAppLocker(int pid, int uid) {
        return false;
    }

    default Pair<Boolean, Boolean> modifyForceSeamlesslyRotate(WindowManager.LayoutParams attr) {
        return null;
    }

    default boolean isNeedSetForceHideNonSystemOverlay(WindowState windowState) {
        return false;
    }

    default void setTranPictureMode(int mode, String packageName) {
    }

    default int[] getTranPictureSupportMode() {
        return null;
    }

    default List<String> getTranPictureList(int mode) {
        return null;
    }

    default void reloadPQEConfig() {
    }

    default boolean modifyConfigFile(String mode, String packageName, int sharpness, int saturation, int contrast, int brightness) {
        return false;
    }

    default String getFocusedWinPkgName() {
        return null;
    }

    default boolean isSecureWindow() {
        return false;
    }

    default boolean inMultiWindowMode() {
        return false;
    }

    default boolean isIMEShowing() {
        return false;
    }

    default void onInit(Context context) {
    }

    default void onSystemUiStarted() {
    }

    default void hookIMEVisibleChanged(boolean show) {
    }

    default void onUserSwitching(SystemService.TargetUser from, SystemService.TargetUser to) {
    }

    default void setPQSupport(int enable) {
    }

    default void setCloudPQ(int enable) {
    }

    default boolean isAodWallpaperFeatureEnabled() {
        return false;
    }

    default void initMultWindowManager(Context context) {
    }

    default boolean onComponentNameJudge(ComponentName cn) {
        return true;
    }

    default void wmsSetProcessGroup(int uid, int pid, int processgroup, String pkg) {
    }

    default void setActivityResumedHook(ActivityRecord record) {
    }
}
