package android.app;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.IActivityClientController;
import android.app.IActivityController;
import android.app.IApplicationThread;
import android.app.IAssistDataReceiver;
import android.app.ITaskAnimation;
import android.app.ITaskSplitManager;
import android.app.ITaskSplitManagerListener;
import android.app.ITaskStackListener;
import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.content.ComponentName;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.content.pm.ParceledListSlice;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.service.voice.IVoiceInteractionSession;
import android.view.IRecentsAnimationRunner;
import android.view.IWindow;
import android.view.MotionEvent;
import android.view.MultiTaskRemoteAnimationAdapter;
import android.view.RemoteAnimationAdapter;
import android.view.RemoteAnimationDefinition;
import android.view.SurfaceControl;
import android.window.BackNavigationInfo;
import android.window.IWindowContainerTransactionCallback;
import android.window.IWindowContainerTransactionCallbackSync;
import android.window.IWindowOrganizerController;
import android.window.SplashScreenView;
import android.window.TaskSnapshot;
import android.window.WindowContainerToken;
import com.android.internal.app.IVoiceInteractor;
import java.util.List;
/* loaded from: classes.dex */
public interface IActivityTaskManager extends IInterface {
    public static final String DESCRIPTOR = "android.app.IActivityTaskManager";

    boolean activityInMultiWindow(String str) throws RemoteException;

    void addAnimationIconLayer(SurfaceControl surfaceControl) throws RemoteException;

    int addAppTask(IBinder iBinder, Intent intent, ActivityManager.TaskDescription taskDescription, Bitmap bitmap) throws RemoteException;

    void alwaysShowUnsupportedCompileSdkWarning(ComponentName componentName) throws RemoteException;

    void boostEndForLauncher() throws RemoteException;

    void boostEndInLauncher(int i) throws RemoteException;

    void boostSceneEnd(int i) throws RemoteException;

    void boostSceneEndDelay(int i, long j) throws RemoteException;

    void boostSceneStart(int i) throws RemoteException;

    void boostSceneStartDuration(int i, long j) throws RemoteException;

    void boostStartForLauncher() throws RemoteException;

    void boostStartInLauncher(int i) throws RemoteException;

    void cancelRecentsAnimation(boolean z) throws RemoteException;

    void cancelTaskWindowTransition(int i) throws RemoteException;

    boolean checkMultiWindowFeatureOn() throws RemoteException;

    void clearFinishFixedRotationWithTransaction() throws RemoteException;

    void clearLaunchParamsForPackages(List<String> list) throws RemoteException;

    ITaskAnimation createTaskAnimation(IBinder iBinder, int i, MultiTaskRemoteAnimationAdapter multiTaskRemoteAnimationAdapter) throws RemoteException;

    void detachNavigationBarFromApp(IBinder iBinder) throws RemoteException;

    void finishVoiceTask(IVoiceInteractionSession iVoiceInteractionSession) throws RemoteException;

    IActivityClientController getActivityClientController() throws RemoteException;

    List<ActivityTaskManager.RootTaskInfo> getAllRootTaskInfos() throws RemoteException;

    List<ActivityTaskManager.RootTaskInfo> getAllRootTaskInfosOnDisplay(int i) throws RemoteException;

    IBinder getAppTaskByTaskId(int i) throws RemoteException;

    Point getAppTaskThumbnailSize() throws RemoteException;

    List<IBinder> getAppTasks(String str) throws RemoteException;

    Bundle getAssistContextExtras(int i) throws RemoteException;

    SurfaceControl getDefaultRootLeash() throws RemoteException;

    ConfigurationInfo getDeviceConfigurationInfo() throws RemoteException;

    SurfaceControl getDragAndZoomBgLeash(int i, int i2, int i3, int i4, boolean z) throws RemoteException;

    ActivityTaskManager.RootTaskInfo getFocusedRootTaskInfo() throws RemoteException;

    String getFocusedWinPkgName() throws RemoteException;

    int getFrontActivityScreenCompatMode() throws RemoteException;

    int getLastResumedActivityUserId() throws RemoteException;

    int getLockTaskModeState() throws RemoteException;

    String getMulitWindowTopPackage() throws RemoteException;

    String getMultiDisplayAreaTopPackage() throws RemoteException;

    String getMultiDisplayAreaTopPackageV4(int i, int i2) throws RemoteException;

    ActivityManager.RunningTaskInfo getMultiWinTopTask(int i, int i2) throws RemoteException;

    List<String> getMultiWindowBlackList() throws RemoteException;

    Rect getMultiWindowContentRegion(int i) throws RemoteException;

    Rect getMultiWindowDefaultRect() throws RemoteException;

    Bundle getMultiWindowParams(String str) throws RemoteException;

    List<String> getMultiWindowTopPackages() throws RemoteException;

    String getMultiWindowVersion() throws RemoteException;

    boolean getMuteState() throws RemoteException;

    boolean getMuteStateV4(int i) throws RemoteException;

    boolean getPackageAskScreenCompat(String str) throws RemoteException;

    int getPackageScreenCompatMode(String str) throws RemoteException;

    ParceledListSlice<ActivityManager.RecentTaskInfo> getRecentTasks(int i, int i2, int i3) throws RemoteException;

    ActivityTaskManager.RootTaskInfo getRootTaskInfo(int i, int i2) throws RemoteException;

    ActivityTaskManager.RootTaskInfo getRootTaskInfoOnDisplay(int i, int i2, int i3) throws RemoteException;

    Rect getTaskBounds(int i) throws RemoteException;

    ActivityManager.TaskDescription getTaskDescription(int i) throws RemoteException;

    Bitmap getTaskDescriptionIcon(String str, int i) throws RemoteException;

    int getTaskOrientation(int i) throws RemoteException;

    TaskSnapshot getTaskSnapshot(int i, boolean z) throws RemoteException;

    List<ActivityManager.RunningTaskInfo> getTasks(int i, boolean z, boolean z2) throws RemoteException;

    ComponentName getTopActivityComponent() throws RemoteException;

    ActivityManager.RunningTaskInfo getTopTask(int i) throws RemoteException;

    String getVoiceInteractorPackageName(IBinder iBinder) throws RemoteException;

    SurfaceControl getWeltWindowLeash(int i, int i2, int i3, int i4, boolean z) throws RemoteException;

    IWindowOrganizerController getWindowOrganizerController() throws RemoteException;

    void handleDrawThreadsBoost(boolean z, boolean z2, List<String> list) throws RemoteException;

    boolean hasMultiWindow() throws RemoteException;

    void hookActiveMultiWindowEndMove(int i, int i2, MotionEvent motionEvent) throws RemoteException;

    void hookActiveMultiWindowMove(int i, int i2, MotionEvent motionEvent) throws RemoteException;

    void hookActiveMultiWindowMoveStartV3() throws RemoteException;

    void hookActiveMultiWindowMoveStartV4(int i, int i2) throws RemoteException;

    void hookActiveMultiWindowStartToMove(IBinder iBinder, int i, int i2, MotionEvent motionEvent, Point point) throws RemoteException;

    void hookExitSplitScreenToMultiWindow(int i) throws RemoteException;

    void hookFinishMovingLocationV3(IWindow iWindow) throws RemoteException;

    Rect hookGetMultiWindowDefaultRect(int i) throws RemoteException;

    Rect hookGetMultiWindowDefaultRectByTask(int i) throws RemoteException;

    void hookMultiWindowFlingV3(IWindow iWindow, MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) throws RemoteException;

    void hookMultiWindowInvisible() throws RemoteException;

    void hookMultiWindowLocation(IWindow iWindow, int i, int i2, int i3, int i4) throws RemoteException;

    void hookMultiWindowMute(IWindow iWindow) throws RemoteException;

    void hookMultiWindowMuteAethenV4(int i) throws RemoteException;

    void hookMultiWindowToCloseV3(IWindow iWindow) throws RemoteException;

    void hookMultiWindowToCloseV4(int i, int i2) throws RemoteException;

    void hookMultiWindowToLargeV3(IWindow iWindow) throws RemoteException;

    void hookMultiWindowToMaxV3(IWindow iWindow) throws RemoteException;

    void hookMultiWindowToMinV3(IWindow iWindow) throws RemoteException;

    void hookMultiWindowToSmallV3(IWindow iWindow) throws RemoteException;

    void hookMultiWindowToSmallV4(int i, int i2) throws RemoteException;

    void hookMultiWindowToSplit(int i, int i2) throws RemoteException;

    void hookMultiWindowVisible() throws RemoteException;

    void hookReparentToDefaultDisplay(int i, int i2) throws RemoteException;

    void hookReserveMultiWindowNumber(int i, long j) throws RemoteException;

    void hookSetMultiWindowDefaultRectResult(Rect rect) throws RemoteException;

    void hookShowBlurLayerFinish() throws RemoteException;

    void hookStartActivityResult(int i, Rect rect) throws RemoteException;

    void hookStartMultiWindow(int i, Rect rect, IWindowContainerTransactionCallback iWindowContainerTransactionCallback) throws RemoteException;

    void hookStartMultiWindowFromSplitScreen(int i, WindowContainerToken windowContainerToken, Rect rect, IWindowContainerTransactionCallbackSync iWindowContainerTransactionCallbackSync) throws RemoteException;

    void hookStartMultiWindowFromSplitScreenV4(int i, WindowContainerToken windowContainerToken, Rect rect, IWindowContainerTransactionCallbackSync iWindowContainerTransactionCallbackSync, int i2) throws RemoteException;

    boolean inMultiWindowMode() throws RemoteException;

    boolean isActivityStartAllowedOnDisplay(int i, Intent intent, String str, int i2) throws RemoteException;

    boolean isAssistDataAllowedOnCurrentActivity() throws RemoteException;

    boolean isIMEShowing() throws RemoteException;

    boolean isInLockTaskMode() throws RemoteException;

    boolean isKeyguardLocking() throws RemoteException;

    boolean isPinnedMode() throws RemoteException;

    boolean isSecureWindow() throws RemoteException;

    boolean isSplitScreen() throws RemoteException;

    boolean isSplitScreenSupportMultiWindowV4(int i, ActivityManager.RunningTaskInfo runningTaskInfo) throws RemoteException;

    boolean isSupportMultiWindow() throws RemoteException;

    boolean isTopActivityImmersive() throws RemoteException;

    void keyguardGoingAway(int i) throws RemoteException;

    void minimizeMultiWinToEdge(int i, boolean z) throws RemoteException;

    void moveRootTaskToDisplay(int i, int i2) throws RemoteException;

    void moveTaskToFront(IApplicationThread iApplicationThread, String str, int i, int i2, Bundle bundle) throws RemoteException;

    void moveTaskToRootTask(int i, int i2, boolean z) throws RemoteException;

    void moveToBottomForMultiWindowV3(String str) throws RemoteException;

    void notAllowKeyguardGoingAwayQuickly(boolean z) throws RemoteException;

    void notifyAuthenticateSucceed(boolean z) throws RemoteException;

    void notifyKeyguardGoingAwayQuickly(boolean z) throws RemoteException;

    void notifyTaskAnimationResult(int i, ITaskAnimation iTaskAnimation) throws RemoteException;

    void onPictureInPictureStateChanged(PictureInPictureUiState pictureInPictureUiState) throws RemoteException;

    void onSplashScreenViewCopyFinished(int i, SplashScreenView.SplashScreenViewParcelable splashScreenViewParcelable) throws RemoteException;

    void registerRemoteAnimationForNextActivityStart(String str, RemoteAnimationAdapter remoteAnimationAdapter, IBinder iBinder) throws RemoteException;

    void registerRemoteAnimationsForDisplay(int i, RemoteAnimationDefinition remoteAnimationDefinition) throws RemoteException;

    void registerTaskSplitListener(ITaskSplitManagerListener iTaskSplitManagerListener) throws RemoteException;

    void registerTaskStackListener(ITaskStackListener iTaskStackListener) throws RemoteException;

    void releaseSomeActivities(IApplicationThread iApplicationThread) throws RemoteException;

    void removeAllVisibleRecentTasks() throws RemoteException;

    void removeAnimationIconLayer(SurfaceControl surfaceControl) throws RemoteException;

    void removeRootTasksInWindowingModes(int[] iArr) throws RemoteException;

    void removeRootTasksWithActivityTypes(int[] iArr) throws RemoteException;

    boolean removeTask(int i) throws RemoteException;

    void removeWeltWindowLeash(SurfaceControl surfaceControl) throws RemoteException;

    void reparentActivity(int i, int i2, boolean z) throws RemoteException;

    void reportAssistContextExtras(IBinder iBinder, Bundle bundle, AssistStructure assistStructure, AssistContent assistContent, Uri uri) throws RemoteException;

    boolean requestAssistContextExtras(int i, IAssistDataReceiver iAssistDataReceiver, Bundle bundle, IBinder iBinder, boolean z, boolean z2) throws RemoteException;

    boolean requestAssistDataForTask(IAssistDataReceiver iAssistDataReceiver, int i, String str) throws RemoteException;

    boolean requestAutofillData(IAssistDataReceiver iAssistDataReceiver, Bundle bundle, IBinder iBinder, int i) throws RemoteException;

    void resetBoostThreads() throws RemoteException;

    boolean resizeTask(int i, Rect rect, int i2) throws RemoteException;

    void resumeAppSwitches() throws RemoteException;

    void setActivityController(IActivityController iActivityController, boolean z) throws RemoteException;

    void setConnectBlackListToSystem(List<String> list) throws RemoteException;

    void setFinishFixedRotationEnterMultiWindowTransactionV3(SurfaceControl surfaceControl, int i, int i2, int i3, float f) throws RemoteException;

    void setFinishFixedRotationWithTransaction(SurfaceControl surfaceControl, float[] fArr, float[] fArr2, int i) throws RemoteException;

    void setFocusedRootTask(int i) throws RemoteException;

    void setFocusedTask(int i) throws RemoteException;

    void setFrontActivityScreenCompatMode(int i) throws RemoteException;

    void setLockScreenShown(boolean z, boolean z2) throws RemoteException;

    void setMultiWindowAcquireFocus(int i, boolean z) throws RemoteException;

    void setMultiWindowBlackListToSystem(List<String> list) throws RemoteException;

    void setMultiWindowConfigToSystem(String str, List<String> list) throws RemoteException;

    void setMultiWindowParams(Bundle bundle) throws RemoteException;

    void setMultiWindowWhiteListToSystem(List<String> list) throws RemoteException;

    void setMuteState(boolean z) throws RemoteException;

    void setMuteStateV4(boolean z, int i) throws RemoteException;

    void setPackageAskScreenCompat(String str, boolean z) throws RemoteException;

    void setPackageScreenCompatMode(String str, int i) throws RemoteException;

    void setPersistentVrThread(int i) throws RemoteException;

    void setRunningRemoteTransitionDelegate(IApplicationThread iApplicationThread) throws RemoteException;

    void setSplitScreenResizing(boolean z) throws RemoteException;

    void setSruWhiteListToSystem(List<String> list) throws RemoteException;

    void setStartInMultiWindow(String str, int i, int i2, int i3) throws RemoteException;

    void setTaskResizeable(int i, int i2) throws RemoteException;

    void setTaskSplitManagerProxy(ITaskSplitManager iTaskSplitManager) throws RemoteException;

    void setThreadScheduler(int i, int i2, int i3) throws RemoteException;

    void setTranMultiWindowModeV3(int i) throws RemoteException;

    void setVoiceKeepAwake(IVoiceInteractionSession iVoiceInteractionSession, boolean z) throws RemoteException;

    void setVrThread(int i) throws RemoteException;

    int startActivities(IApplicationThread iApplicationThread, String str, String str2, Intent[] intentArr, String[] strArr, IBinder iBinder, Bundle bundle, int i) throws RemoteException;

    int startActivity(IApplicationThread iApplicationThread, String str, String str2, Intent intent, String str3, IBinder iBinder, String str4, int i, int i2, ProfilerInfo profilerInfo, Bundle bundle) throws RemoteException;

    WaitResult startActivityAndWait(IApplicationThread iApplicationThread, String str, String str2, Intent intent, String str3, IBinder iBinder, String str4, int i, int i2, ProfilerInfo profilerInfo, Bundle bundle, int i3) throws RemoteException;

    int startActivityAsCaller(IApplicationThread iApplicationThread, String str, Intent intent, String str2, IBinder iBinder, String str3, int i, int i2, ProfilerInfo profilerInfo, Bundle bundle, boolean z, int i3) throws RemoteException;

    int startActivityAsUser(IApplicationThread iApplicationThread, String str, String str2, Intent intent, String str3, IBinder iBinder, String str4, int i, int i2, ProfilerInfo profilerInfo, Bundle bundle, int i3) throws RemoteException;

    int startActivityFromGameSession(IApplicationThread iApplicationThread, String str, String str2, int i, int i2, Intent intent, int i3, int i4) throws RemoteException;

    int startActivityFromRecents(int i, Bundle bundle) throws RemoteException;

    int startActivityIntentSender(IApplicationThread iApplicationThread, IIntentSender iIntentSender, IBinder iBinder, Intent intent, String str, IBinder iBinder2, String str2, int i, int i2, int i3, Bundle bundle) throws RemoteException;

    int startActivityWithConfig(IApplicationThread iApplicationThread, String str, String str2, Intent intent, String str3, IBinder iBinder, String str4, int i, int i2, Configuration configuration, Bundle bundle, int i3) throws RemoteException;

    int startAssistantActivity(String str, String str2, int i, int i2, Intent intent, String str3, Bundle bundle, int i3) throws RemoteException;

    BackNavigationInfo startBackNavigation(boolean z) throws RemoteException;

    void startCurrentAppInMultiWindow(boolean z, int i) throws RemoteException;

    boolean startDreamActivity(Intent intent) throws RemoteException;

    boolean startNextMatchingActivity(IBinder iBinder, Intent intent, Bundle bundle) throws RemoteException;

    void startRecentsActivity(Intent intent, long j, IRecentsAnimationRunner iRecentsAnimationRunner) throws RemoteException;

    void startSystemLockTaskMode(int i) throws RemoteException;

    int startVoiceActivity(String str, String str2, int i, int i2, Intent intent, String str3, IVoiceInteractionSession iVoiceInteractionSession, IVoiceInteractor iVoiceInteractor, int i3, ProfilerInfo profilerInfo, Bundle bundle, int i4) throws RemoteException;

    void stopAppSwitches() throws RemoteException;

    void stopSystemLockTaskMode() throws RemoteException;

    boolean supportsLocalVoiceInteraction() throws RemoteException;

    void suppressResizeConfigChanges(boolean z) throws RemoteException;

    TaskSnapshot takeTaskSnapshot(int i) throws RemoteException;

    boolean taskInMultiWindowById(int i) throws RemoteException;

    void unhandledBack() throws RemoteException;

    void unregisterTaskSplitListener(ITaskSplitManagerListener iTaskSplitManagerListener) throws RemoteException;

    void unregisterTaskStackListener(ITaskStackListener iTaskStackListener) throws RemoteException;

    boolean updateConfiguration(Configuration configuration) throws RemoteException;

    void updateLockTaskFeatures(int i, int i2) throws RemoteException;

    void updateLockTaskPackages(int i, String[] strArr) throws RemoteException;

    void updateZBoostTaskIdWhenToSplit(int i) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IActivityTaskManager {
        @Override // android.app.IActivityTaskManager
        public int startActivity(IApplicationThread caller, String callingPackage, String callingFeatureId, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int flags, ProfilerInfo profilerInfo, Bundle options) throws RemoteException {
            return 0;
        }

        @Override // android.app.IActivityTaskManager
        public int startActivities(IApplicationThread caller, String callingPackage, String callingFeatureId, Intent[] intents, String[] resolvedTypes, IBinder resultTo, Bundle options, int userId) throws RemoteException {
            return 0;
        }

        @Override // android.app.IActivityTaskManager
        public int startActivityAsUser(IApplicationThread caller, String callingPackage, String callingFeatureId, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int flags, ProfilerInfo profilerInfo, Bundle options, int userId) throws RemoteException {
            return 0;
        }

        @Override // android.app.IActivityTaskManager
        public boolean startNextMatchingActivity(IBinder callingActivity, Intent intent, Bundle options) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public boolean startDreamActivity(Intent intent) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public int startActivityIntentSender(IApplicationThread caller, IIntentSender target, IBinder whitelistToken, Intent fillInIntent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int flagsMask, int flagsValues, Bundle options) throws RemoteException {
            return 0;
        }

        @Override // android.app.IActivityTaskManager
        public WaitResult startActivityAndWait(IApplicationThread caller, String callingPackage, String callingFeatureId, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int flags, ProfilerInfo profilerInfo, Bundle options, int userId) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public int startActivityWithConfig(IApplicationThread caller, String callingPackage, String callingFeatureId, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int startFlags, Configuration newConfig, Bundle options, int userId) throws RemoteException {
            return 0;
        }

        @Override // android.app.IActivityTaskManager
        public int startVoiceActivity(String callingPackage, String callingFeatureId, int callingPid, int callingUid, Intent intent, String resolvedType, IVoiceInteractionSession session, IVoiceInteractor interactor, int flags, ProfilerInfo profilerInfo, Bundle options, int userId) throws RemoteException {
            return 0;
        }

        @Override // android.app.IActivityTaskManager
        public String getVoiceInteractorPackageName(IBinder callingVoiceInteractor) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public int startAssistantActivity(String callingPackage, String callingFeatureId, int callingPid, int callingUid, Intent intent, String resolvedType, Bundle options, int userId) throws RemoteException {
            return 0;
        }

        @Override // android.app.IActivityTaskManager
        public int startActivityFromGameSession(IApplicationThread caller, String callingPackage, String callingFeatureId, int callingPid, int callingUid, Intent intent, int taskId, int userId) throws RemoteException {
            return 0;
        }

        @Override // android.app.IActivityTaskManager
        public void startRecentsActivity(Intent intent, long eventTime, IRecentsAnimationRunner recentsAnimationRunner) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public int startActivityFromRecents(int taskId, Bundle options) throws RemoteException {
            return 0;
        }

        @Override // android.app.IActivityTaskManager
        public int startActivityAsCaller(IApplicationThread caller, String callingPackage, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int flags, ProfilerInfo profilerInfo, Bundle options, boolean ignoreTargetSecurity, int userId) throws RemoteException {
            return 0;
        }

        @Override // android.app.IActivityTaskManager
        public boolean isActivityStartAllowedOnDisplay(int displayId, Intent intent, String resolvedType, int userId) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public void unhandledBack() throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public IActivityClientController getActivityClientController() throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public int getFrontActivityScreenCompatMode() throws RemoteException {
            return 0;
        }

        @Override // android.app.IActivityTaskManager
        public void setFrontActivityScreenCompatMode(int mode) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void setFocusedTask(int taskId) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public boolean removeTask(int taskId) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public void removeAllVisibleRecentTasks() throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public List<ActivityManager.RunningTaskInfo> getTasks(int maxNum, boolean filterOnlyVisibleRecents, boolean keepIntentExtra) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public void moveTaskToFront(IApplicationThread app, String callingPackage, int task, int flags, Bundle options) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public ParceledListSlice<ActivityManager.RecentTaskInfo> getRecentTasks(int maxNum, int flags, int userId) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public boolean isTopActivityImmersive() throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public ActivityManager.TaskDescription getTaskDescription(int taskId) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public void reportAssistContextExtras(IBinder assistToken, Bundle extras, AssistStructure structure, AssistContent content, Uri referrer) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void setFocusedRootTask(int taskId) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public ActivityTaskManager.RootTaskInfo getFocusedRootTaskInfo() throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public Rect getTaskBounds(int taskId) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public void cancelRecentsAnimation(boolean restoreHomeRootTaskPosition) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void updateLockTaskPackages(int userId, String[] packages) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public boolean isInLockTaskMode() throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public int getLockTaskModeState() throws RemoteException {
            return 0;
        }

        @Override // android.app.IActivityTaskManager
        public List<IBinder> getAppTasks(String callingPackage) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public void startSystemLockTaskMode(int taskId) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void stopSystemLockTaskMode() throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void finishVoiceTask(IVoiceInteractionSession session) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public int addAppTask(IBinder activityToken, Intent intent, ActivityManager.TaskDescription description, Bitmap thumbnail) throws RemoteException {
            return 0;
        }

        @Override // android.app.IActivityTaskManager
        public Point getAppTaskThumbnailSize() throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public void releaseSomeActivities(IApplicationThread app) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public Bitmap getTaskDescriptionIcon(String filename, int userId) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public void registerTaskStackListener(ITaskStackListener listener) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void unregisterTaskStackListener(ITaskStackListener listener) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void setTaskResizeable(int taskId, int resizeableMode) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public boolean resizeTask(int taskId, Rect bounds, int resizeMode) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public void moveRootTaskToDisplay(int taskId, int displayId) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void moveTaskToRootTask(int taskId, int rootTaskId, boolean toTop) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void removeRootTasksInWindowingModes(int[] windowingModes) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void removeRootTasksWithActivityTypes(int[] activityTypes) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public List<ActivityTaskManager.RootTaskInfo> getAllRootTaskInfos() throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public ActivityTaskManager.RootTaskInfo getRootTaskInfo(int windowingMode, int activityType) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public List<ActivityTaskManager.RootTaskInfo> getAllRootTaskInfosOnDisplay(int displayId) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public ActivityTaskManager.RootTaskInfo getRootTaskInfoOnDisplay(int windowingMode, int activityType, int displayId) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public void setLockScreenShown(boolean showingKeyguard, boolean showingAod) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public Bundle getAssistContextExtras(int requestType) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public boolean requestAssistContextExtras(int requestType, IAssistDataReceiver receiver, Bundle receiverExtras, IBinder activityToken, boolean focused, boolean newSessionId) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public boolean requestAutofillData(IAssistDataReceiver receiver, Bundle receiverExtras, IBinder activityToken, int flags) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public boolean isAssistDataAllowedOnCurrentActivity() throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public boolean requestAssistDataForTask(IAssistDataReceiver receiver, int taskId, String callingPackageName) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public void keyguardGoingAway(int flags) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void suppressResizeConfigChanges(boolean suppress) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public IWindowOrganizerController getWindowOrganizerController() throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public void setSplitScreenResizing(boolean resizing) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public boolean supportsLocalVoiceInteraction() throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public ConfigurationInfo getDeviceConfigurationInfo() throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public void cancelTaskWindowTransition(int taskId) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public TaskSnapshot getTaskSnapshot(int taskId, boolean isLowResolution) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public TaskSnapshot takeTaskSnapshot(int taskId) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public int getLastResumedActivityUserId() throws RemoteException {
            return 0;
        }

        @Override // android.app.IActivityTaskManager
        public boolean updateConfiguration(Configuration values) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public void updateLockTaskFeatures(int userId, int flags) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void registerRemoteAnimationForNextActivityStart(String packageName, RemoteAnimationAdapter adapter, IBinder launchCookie) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void registerRemoteAnimationsForDisplay(int displayId, RemoteAnimationDefinition definition) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void alwaysShowUnsupportedCompileSdkWarning(ComponentName activity) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void setVrThread(int tid) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void setPersistentVrThread(int tid) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void stopAppSwitches() throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void resumeAppSwitches() throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void setActivityController(IActivityController watcher, boolean imAMonkey) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void setVoiceKeepAwake(IVoiceInteractionSession session, boolean keepAwake) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public int getPackageScreenCompatMode(String packageName) throws RemoteException {
            return 0;
        }

        @Override // android.app.IActivityTaskManager
        public void setPackageScreenCompatMode(String packageName, int mode) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public boolean getPackageAskScreenCompat(String packageName) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public void setPackageAskScreenCompat(String packageName, boolean ask) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void clearLaunchParamsForPackages(List<String> packageNames) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void onSplashScreenViewCopyFinished(int taskId, SplashScreenView.SplashScreenViewParcelable material) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void onPictureInPictureStateChanged(PictureInPictureUiState pipState) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public String getFocusedWinPkgName() throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public boolean isSecureWindow() throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public boolean isIMEShowing() throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public boolean inMultiWindowMode() throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public void notifyKeyguardGoingAwayQuickly(boolean goingAwayQuickly) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public boolean isKeyguardLocking() throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public void notifyAuthenticateSucceed(boolean isSucceed) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void notAllowKeyguardGoingAwayQuickly(boolean notAllow) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void detachNavigationBarFromApp(IBinder transition) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void setRunningRemoteTransitionDelegate(IApplicationThread caller) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public BackNavigationInfo startBackNavigation(boolean requestAnimation) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public ComponentName getTopActivityComponent() throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public void setThreadScheduler(int tid, int policy, int priority) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void setTaskSplitManagerProxy(ITaskSplitManager taskSplitManager) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void registerTaskSplitListener(ITaskSplitManagerListener taskSplitListener) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void unregisterTaskSplitListener(ITaskSplitManagerListener taskSplitListener) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void notifyTaskAnimationResult(int taskId, ITaskAnimation taskAnimation) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public ITaskAnimation createTaskAnimation(IBinder token, int taskId, MultiTaskRemoteAnimationAdapter callback) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public void hookReparentToDefaultDisplay(int multiWindowMode, int multiWindowId) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookMultiWindowToMaxV3(IWindow window) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookMultiWindowToMinV3(IWindow window) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookMultiWindowToCloseV3(IWindow window) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookMultiWindowToSmallV3(IWindow window) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookMultiWindowToLargeV3(IWindow window) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookMultiWindowToCloseV4(int multiWindowMode, int multiWindowId) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookMultiWindowToSmallV4(int multiWindowMode, int multiWindowId) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookMultiWindowToSplit(int multiWindowMode, int multiWindowId) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookMultiWindowMute(IWindow window) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookMultiWindowLocation(IWindow window, int x, int y, int touchX, int touchY) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookFinishMovingLocationV3(IWindow window) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookMultiWindowFlingV3(IWindow window, MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookStartActivityResult(int result, Rect location) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookMultiWindowVisible() throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookMultiWindowInvisible() throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void setTranMultiWindowModeV3(int mode) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void setStartInMultiWindow(String pkgName, int type, int direction, int startType) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public String getMultiWindowVersion() throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public boolean hasMultiWindow() throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public boolean activityInMultiWindow(String pkgName) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public String getMulitWindowTopPackage() throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public void startCurrentAppInMultiWindow(boolean anim, int startType) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public boolean isSupportMultiWindow() throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public boolean isSplitScreenSupportMultiWindowV4(int type, ActivityManager.RunningTaskInfo info) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public List<String> getMultiWindowBlackList() throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public void setMultiWindowBlackListToSystem(List<String> list) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void setMultiWindowWhiteListToSystem(List<String> list) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void setMultiWindowConfigToSystem(String key, List<String> list) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public Rect getMultiWindowDefaultRect() throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public void setFinishFixedRotationEnterMultiWindowTransactionV3(SurfaceControl leash, int x, int y, int rotation, float scale) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public SurfaceControl getWeltWindowLeash(int width, int height, int x, int y, boolean hidden) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public void removeWeltWindowLeash(SurfaceControl leash) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public SurfaceControl getDragAndZoomBgLeash(int width, int height, int x, int y, boolean hidden) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public void hookActiveMultiWindowMoveStartV3() throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookActiveMultiWindowMoveStartV4(int multiWindowMode, int multiWindowId) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookActiveMultiWindowStartToMove(IBinder token, int multiWindowMode, int multiWindowId, MotionEvent event, Point downPoint) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookActiveMultiWindowMove(int multiWindowMode, int multiWindowId, MotionEvent event) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookActiveMultiWindowEndMove(int multiWindowMode, int multiWindowId, MotionEvent event) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public boolean checkMultiWindowFeatureOn() throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public List<String> getMultiWindowTopPackages() throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public boolean getMuteState() throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public boolean getMuteStateV4(int multiWindowId) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public void setMuteState(boolean state) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void setMuteStateV4(boolean state, int multiWindowId) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookMultiWindowMuteAethenV4(int type) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public String getMultiDisplayAreaTopPackage() throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public String getMultiDisplayAreaTopPackageV4(int multiWindowMode, int multiWindowId) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public void moveToBottomForMultiWindowV3(String reason) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void setMultiWindowAcquireFocus(int multiWindowId, boolean acquireFocus) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public ActivityManager.RunningTaskInfo getTopTask(int displayId) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public ActivityManager.RunningTaskInfo getMultiWinTopTask(int winMode, int winId) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public int getTaskOrientation(int taskId) throws RemoteException {
            return 0;
        }

        @Override // android.app.IActivityTaskManager
        public void minimizeMultiWinToEdge(int taskId, boolean toEdge) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public Rect getMultiWindowContentRegion(int taskId) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public void updateZBoostTaskIdWhenToSplit(int taskId) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookExitSplitScreenToMultiWindow(int multiWindowMode) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public Rect hookGetMultiWindowDefaultRect(int orientationType) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public Rect hookGetMultiWindowDefaultRectByTask(int taskId) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public void hookSetMultiWindowDefaultRectResult(Rect rect) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookStartMultiWindowFromSplitScreen(int fullTaskId, WindowContainerToken multiTaskToken, Rect multiWindowRegion, IWindowContainerTransactionCallbackSync syncCallback) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookStartMultiWindowFromSplitScreenV4(int fullTaskId, WindowContainerToken multiTaskToken, Rect multiWindowRegion, IWindowContainerTransactionCallbackSync syncCallback, int type) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void setFinishFixedRotationWithTransaction(SurfaceControl leash, float[] transFloat9, float[] cropFloat4, int rotation) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void clearFinishFixedRotationWithTransaction() throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookStartMultiWindow(int taskId, Rect multiWindowRegion, IWindowContainerTransactionCallback callback) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public boolean taskInMultiWindowById(int taskId) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public void addAnimationIconLayer(SurfaceControl sc) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void removeAnimationIconLayer(SurfaceControl sc) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public IBinder getAppTaskByTaskId(int taskId) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public void boostSceneStart(int scene) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void boostSceneStartDuration(int scene, long durationMs) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void boostSceneEnd(int scene) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void boostSceneEndDelay(int scene, long delayMs) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void boostStartForLauncher() throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void boostEndForLauncher() throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void boostStartInLauncher(int type) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void boostEndInLauncher(int type) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void handleDrawThreadsBoost(boolean handleAllThreads, boolean bindBigCore, List<String> handleActName) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void resetBoostThreads() throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public Bundle getMultiWindowParams(String pkgName) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public void setMultiWindowParams(Bundle params) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public boolean isPinnedMode() throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public SurfaceControl getDefaultRootLeash() throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityTaskManager
        public void reparentActivity(int fromDisplayId, int destDisplayId, boolean onTop) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public boolean isSplitScreen() throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityTaskManager
        public void setConnectBlackListToSystem(List<String> list) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookReserveMultiWindowNumber(int reserveNum, long showDelayTime) throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void hookShowBlurLayerFinish() throws RemoteException {
        }

        @Override // android.app.IActivityTaskManager
        public void setSruWhiteListToSystem(List<String> list) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IActivityTaskManager {
        static final int TRANSACTION_activityInMultiWindow = 129;
        static final int TRANSACTION_addAnimationIconLayer = 175;
        static final int TRANSACTION_addAppTask = 41;
        static final int TRANSACTION_alwaysShowUnsupportedCompileSdkWarning = 77;
        static final int TRANSACTION_boostEndForLauncher = 183;
        static final int TRANSACTION_boostEndInLauncher = 185;
        static final int TRANSACTION_boostSceneEnd = 180;
        static final int TRANSACTION_boostSceneEndDelay = 181;
        static final int TRANSACTION_boostSceneStart = 178;
        static final int TRANSACTION_boostSceneStartDuration = 179;
        static final int TRANSACTION_boostStartForLauncher = 182;
        static final int TRANSACTION_boostStartInLauncher = 184;
        static final int TRANSACTION_cancelRecentsAnimation = 33;
        static final int TRANSACTION_cancelTaskWindowTransition = 69;
        static final int TRANSACTION_checkMultiWindowFeatureOn = 148;
        static final int TRANSACTION_clearFinishFixedRotationWithTransaction = 172;
        static final int TRANSACTION_clearLaunchParamsForPackages = 88;
        static final int TRANSACTION_createTaskAnimation = 108;
        static final int TRANSACTION_detachNavigationBarFromApp = 99;
        static final int TRANSACTION_finishVoiceTask = 40;
        static final int TRANSACTION_getActivityClientController = 18;
        static final int TRANSACTION_getAllRootTaskInfos = 53;
        static final int TRANSACTION_getAllRootTaskInfosOnDisplay = 55;
        static final int TRANSACTION_getAppTaskByTaskId = 177;
        static final int TRANSACTION_getAppTaskThumbnailSize = 42;
        static final int TRANSACTION_getAppTasks = 37;
        static final int TRANSACTION_getAssistContextExtras = 58;
        static final int TRANSACTION_getDefaultRootLeash = 191;
        static final int TRANSACTION_getDeviceConfigurationInfo = 68;
        static final int TRANSACTION_getDragAndZoomBgLeash = 142;
        static final int TRANSACTION_getFocusedRootTaskInfo = 31;
        static final int TRANSACTION_getFocusedWinPkgName = 91;
        static final int TRANSACTION_getFrontActivityScreenCompatMode = 19;
        static final int TRANSACTION_getLastResumedActivityUserId = 72;
        static final int TRANSACTION_getLockTaskModeState = 36;
        static final int TRANSACTION_getMulitWindowTopPackage = 130;
        static final int TRANSACTION_getMultiDisplayAreaTopPackage = 155;
        static final int TRANSACTION_getMultiDisplayAreaTopPackageV4 = 156;
        static final int TRANSACTION_getMultiWinTopTask = 160;
        static final int TRANSACTION_getMultiWindowBlackList = 134;
        static final int TRANSACTION_getMultiWindowContentRegion = 163;
        static final int TRANSACTION_getMultiWindowDefaultRect = 138;
        static final int TRANSACTION_getMultiWindowParams = 188;
        static final int TRANSACTION_getMultiWindowTopPackages = 149;
        static final int TRANSACTION_getMultiWindowVersion = 127;
        static final int TRANSACTION_getMuteState = 150;
        static final int TRANSACTION_getMuteStateV4 = 151;
        static final int TRANSACTION_getPackageAskScreenCompat = 86;
        static final int TRANSACTION_getPackageScreenCompatMode = 84;
        static final int TRANSACTION_getRecentTasks = 26;
        static final int TRANSACTION_getRootTaskInfo = 54;
        static final int TRANSACTION_getRootTaskInfoOnDisplay = 56;
        static final int TRANSACTION_getTaskBounds = 32;
        static final int TRANSACTION_getTaskDescription = 28;
        static final int TRANSACTION_getTaskDescriptionIcon = 44;
        static final int TRANSACTION_getTaskOrientation = 161;
        static final int TRANSACTION_getTaskSnapshot = 70;
        static final int TRANSACTION_getTasks = 24;
        static final int TRANSACTION_getTopActivityComponent = 102;
        static final int TRANSACTION_getTopTask = 159;
        static final int TRANSACTION_getVoiceInteractorPackageName = 10;
        static final int TRANSACTION_getWeltWindowLeash = 140;
        static final int TRANSACTION_getWindowOrganizerController = 65;
        static final int TRANSACTION_handleDrawThreadsBoost = 186;
        static final int TRANSACTION_hasMultiWindow = 128;
        static final int TRANSACTION_hookActiveMultiWindowEndMove = 147;
        static final int TRANSACTION_hookActiveMultiWindowMove = 146;
        static final int TRANSACTION_hookActiveMultiWindowMoveStartV3 = 143;
        static final int TRANSACTION_hookActiveMultiWindowMoveStartV4 = 144;
        static final int TRANSACTION_hookActiveMultiWindowStartToMove = 145;
        static final int TRANSACTION_hookExitSplitScreenToMultiWindow = 165;
        static final int TRANSACTION_hookFinishMovingLocationV3 = 120;
        static final int TRANSACTION_hookGetMultiWindowDefaultRect = 166;
        static final int TRANSACTION_hookGetMultiWindowDefaultRectByTask = 167;
        static final int TRANSACTION_hookMultiWindowFlingV3 = 121;
        static final int TRANSACTION_hookMultiWindowInvisible = 124;
        static final int TRANSACTION_hookMultiWindowLocation = 119;
        static final int TRANSACTION_hookMultiWindowMute = 118;
        static final int TRANSACTION_hookMultiWindowMuteAethenV4 = 154;
        static final int TRANSACTION_hookMultiWindowToCloseV3 = 112;
        static final int TRANSACTION_hookMultiWindowToCloseV4 = 115;
        static final int TRANSACTION_hookMultiWindowToLargeV3 = 114;
        static final int TRANSACTION_hookMultiWindowToMaxV3 = 110;
        static final int TRANSACTION_hookMultiWindowToMinV3 = 111;
        static final int TRANSACTION_hookMultiWindowToSmallV3 = 113;
        static final int TRANSACTION_hookMultiWindowToSmallV4 = 116;
        static final int TRANSACTION_hookMultiWindowToSplit = 117;
        static final int TRANSACTION_hookMultiWindowVisible = 123;
        static final int TRANSACTION_hookReparentToDefaultDisplay = 109;
        static final int TRANSACTION_hookReserveMultiWindowNumber = 195;
        static final int TRANSACTION_hookSetMultiWindowDefaultRectResult = 168;
        static final int TRANSACTION_hookShowBlurLayerFinish = 196;
        static final int TRANSACTION_hookStartActivityResult = 122;
        static final int TRANSACTION_hookStartMultiWindow = 173;
        static final int TRANSACTION_hookStartMultiWindowFromSplitScreen = 169;
        static final int TRANSACTION_hookStartMultiWindowFromSplitScreenV4 = 170;
        static final int TRANSACTION_inMultiWindowMode = 94;
        static final int TRANSACTION_isActivityStartAllowedOnDisplay = 16;
        static final int TRANSACTION_isAssistDataAllowedOnCurrentActivity = 61;
        static final int TRANSACTION_isIMEShowing = 93;
        static final int TRANSACTION_isInLockTaskMode = 35;
        static final int TRANSACTION_isKeyguardLocking = 96;
        static final int TRANSACTION_isPinnedMode = 190;
        static final int TRANSACTION_isSecureWindow = 92;
        static final int TRANSACTION_isSplitScreen = 193;
        static final int TRANSACTION_isSplitScreenSupportMultiWindowV4 = 133;
        static final int TRANSACTION_isSupportMultiWindow = 132;
        static final int TRANSACTION_isTopActivityImmersive = 27;
        static final int TRANSACTION_keyguardGoingAway = 63;
        static final int TRANSACTION_minimizeMultiWinToEdge = 162;
        static final int TRANSACTION_moveRootTaskToDisplay = 49;
        static final int TRANSACTION_moveTaskToFront = 25;
        static final int TRANSACTION_moveTaskToRootTask = 50;
        static final int TRANSACTION_moveToBottomForMultiWindowV3 = 157;
        static final int TRANSACTION_notAllowKeyguardGoingAwayQuickly = 98;
        static final int TRANSACTION_notifyAuthenticateSucceed = 97;
        static final int TRANSACTION_notifyKeyguardGoingAwayQuickly = 95;
        static final int TRANSACTION_notifyTaskAnimationResult = 107;
        static final int TRANSACTION_onPictureInPictureStateChanged = 90;
        static final int TRANSACTION_onSplashScreenViewCopyFinished = 89;
        static final int TRANSACTION_registerRemoteAnimationForNextActivityStart = 75;
        static final int TRANSACTION_registerRemoteAnimationsForDisplay = 76;
        static final int TRANSACTION_registerTaskSplitListener = 105;
        static final int TRANSACTION_registerTaskStackListener = 45;
        static final int TRANSACTION_releaseSomeActivities = 43;
        static final int TRANSACTION_removeAllVisibleRecentTasks = 23;
        static final int TRANSACTION_removeAnimationIconLayer = 176;
        static final int TRANSACTION_removeRootTasksInWindowingModes = 51;
        static final int TRANSACTION_removeRootTasksWithActivityTypes = 52;
        static final int TRANSACTION_removeTask = 22;
        static final int TRANSACTION_removeWeltWindowLeash = 141;
        static final int TRANSACTION_reparentActivity = 192;
        static final int TRANSACTION_reportAssistContextExtras = 29;
        static final int TRANSACTION_requestAssistContextExtras = 59;
        static final int TRANSACTION_requestAssistDataForTask = 62;
        static final int TRANSACTION_requestAutofillData = 60;
        static final int TRANSACTION_resetBoostThreads = 187;
        static final int TRANSACTION_resizeTask = 48;
        static final int TRANSACTION_resumeAppSwitches = 81;
        static final int TRANSACTION_setActivityController = 82;
        static final int TRANSACTION_setConnectBlackListToSystem = 194;
        static final int TRANSACTION_setFinishFixedRotationEnterMultiWindowTransactionV3 = 139;
        static final int TRANSACTION_setFinishFixedRotationWithTransaction = 171;
        static final int TRANSACTION_setFocusedRootTask = 30;
        static final int TRANSACTION_setFocusedTask = 21;
        static final int TRANSACTION_setFrontActivityScreenCompatMode = 20;
        static final int TRANSACTION_setLockScreenShown = 57;
        static final int TRANSACTION_setMultiWindowAcquireFocus = 158;
        static final int TRANSACTION_setMultiWindowBlackListToSystem = 135;
        static final int TRANSACTION_setMultiWindowConfigToSystem = 137;
        static final int TRANSACTION_setMultiWindowParams = 189;
        static final int TRANSACTION_setMultiWindowWhiteListToSystem = 136;
        static final int TRANSACTION_setMuteState = 152;
        static final int TRANSACTION_setMuteStateV4 = 153;
        static final int TRANSACTION_setPackageAskScreenCompat = 87;
        static final int TRANSACTION_setPackageScreenCompatMode = 85;
        static final int TRANSACTION_setPersistentVrThread = 79;
        static final int TRANSACTION_setRunningRemoteTransitionDelegate = 100;
        static final int TRANSACTION_setSplitScreenResizing = 66;
        static final int TRANSACTION_setSruWhiteListToSystem = 197;
        static final int TRANSACTION_setStartInMultiWindow = 126;
        static final int TRANSACTION_setTaskResizeable = 47;
        static final int TRANSACTION_setTaskSplitManagerProxy = 104;
        static final int TRANSACTION_setThreadScheduler = 103;
        static final int TRANSACTION_setTranMultiWindowModeV3 = 125;
        static final int TRANSACTION_setVoiceKeepAwake = 83;
        static final int TRANSACTION_setVrThread = 78;
        static final int TRANSACTION_startActivities = 2;
        static final int TRANSACTION_startActivity = 1;
        static final int TRANSACTION_startActivityAndWait = 7;
        static final int TRANSACTION_startActivityAsCaller = 15;
        static final int TRANSACTION_startActivityAsUser = 3;
        static final int TRANSACTION_startActivityFromGameSession = 12;
        static final int TRANSACTION_startActivityFromRecents = 14;
        static final int TRANSACTION_startActivityIntentSender = 6;
        static final int TRANSACTION_startActivityWithConfig = 8;
        static final int TRANSACTION_startAssistantActivity = 11;
        static final int TRANSACTION_startBackNavigation = 101;
        static final int TRANSACTION_startCurrentAppInMultiWindow = 131;
        static final int TRANSACTION_startDreamActivity = 5;
        static final int TRANSACTION_startNextMatchingActivity = 4;
        static final int TRANSACTION_startRecentsActivity = 13;
        static final int TRANSACTION_startSystemLockTaskMode = 38;
        static final int TRANSACTION_startVoiceActivity = 9;
        static final int TRANSACTION_stopAppSwitches = 80;
        static final int TRANSACTION_stopSystemLockTaskMode = 39;
        static final int TRANSACTION_supportsLocalVoiceInteraction = 67;
        static final int TRANSACTION_suppressResizeConfigChanges = 64;
        static final int TRANSACTION_takeTaskSnapshot = 71;
        static final int TRANSACTION_taskInMultiWindowById = 174;
        static final int TRANSACTION_unhandledBack = 17;
        static final int TRANSACTION_unregisterTaskSplitListener = 106;
        static final int TRANSACTION_unregisterTaskStackListener = 46;
        static final int TRANSACTION_updateConfiguration = 73;
        static final int TRANSACTION_updateLockTaskFeatures = 74;
        static final int TRANSACTION_updateLockTaskPackages = 34;
        static final int TRANSACTION_updateZBoostTaskIdWhenToSplit = 164;

        public Stub() {
            attachInterface(this, IActivityTaskManager.DESCRIPTOR);
        }

        public static IActivityTaskManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IActivityTaskManager.DESCRIPTOR);
            if (iin != null && (iin instanceof IActivityTaskManager)) {
                return (IActivityTaskManager) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public static String getDefaultTransactionName(int transactionCode) {
            switch (transactionCode) {
                case 1:
                    return "startActivity";
                case 2:
                    return "startActivities";
                case 3:
                    return "startActivityAsUser";
                case 4:
                    return "startNextMatchingActivity";
                case 5:
                    return "startDreamActivity";
                case 6:
                    return "startActivityIntentSender";
                case 7:
                    return "startActivityAndWait";
                case 8:
                    return "startActivityWithConfig";
                case 9:
                    return "startVoiceActivity";
                case 10:
                    return "getVoiceInteractorPackageName";
                case 11:
                    return "startAssistantActivity";
                case 12:
                    return "startActivityFromGameSession";
                case 13:
                    return "startRecentsActivity";
                case 14:
                    return "startActivityFromRecents";
                case 15:
                    return "startActivityAsCaller";
                case 16:
                    return "isActivityStartAllowedOnDisplay";
                case 17:
                    return "unhandledBack";
                case 18:
                    return "getActivityClientController";
                case 19:
                    return "getFrontActivityScreenCompatMode";
                case 20:
                    return "setFrontActivityScreenCompatMode";
                case 21:
                    return "setFocusedTask";
                case 22:
                    return "removeTask";
                case 23:
                    return "removeAllVisibleRecentTasks";
                case 24:
                    return "getTasks";
                case 25:
                    return "moveTaskToFront";
                case 26:
                    return "getRecentTasks";
                case 27:
                    return "isTopActivityImmersive";
                case 28:
                    return "getTaskDescription";
                case 29:
                    return "reportAssistContextExtras";
                case 30:
                    return "setFocusedRootTask";
                case 31:
                    return "getFocusedRootTaskInfo";
                case 32:
                    return "getTaskBounds";
                case 33:
                    return "cancelRecentsAnimation";
                case 34:
                    return "updateLockTaskPackages";
                case 35:
                    return "isInLockTaskMode";
                case 36:
                    return "getLockTaskModeState";
                case 37:
                    return "getAppTasks";
                case 38:
                    return "startSystemLockTaskMode";
                case 39:
                    return "stopSystemLockTaskMode";
                case 40:
                    return "finishVoiceTask";
                case 41:
                    return "addAppTask";
                case 42:
                    return "getAppTaskThumbnailSize";
                case 43:
                    return "releaseSomeActivities";
                case 44:
                    return "getTaskDescriptionIcon";
                case 45:
                    return "registerTaskStackListener";
                case 46:
                    return "unregisterTaskStackListener";
                case 47:
                    return "setTaskResizeable";
                case 48:
                    return "resizeTask";
                case 49:
                    return "moveRootTaskToDisplay";
                case 50:
                    return "moveTaskToRootTask";
                case 51:
                    return "removeRootTasksInWindowingModes";
                case 52:
                    return "removeRootTasksWithActivityTypes";
                case 53:
                    return "getAllRootTaskInfos";
                case 54:
                    return "getRootTaskInfo";
                case 55:
                    return "getAllRootTaskInfosOnDisplay";
                case 56:
                    return "getRootTaskInfoOnDisplay";
                case 57:
                    return "setLockScreenShown";
                case 58:
                    return "getAssistContextExtras";
                case 59:
                    return "requestAssistContextExtras";
                case 60:
                    return "requestAutofillData";
                case 61:
                    return "isAssistDataAllowedOnCurrentActivity";
                case 62:
                    return "requestAssistDataForTask";
                case 63:
                    return "keyguardGoingAway";
                case 64:
                    return "suppressResizeConfigChanges";
                case 65:
                    return "getWindowOrganizerController";
                case 66:
                    return "setSplitScreenResizing";
                case 67:
                    return "supportsLocalVoiceInteraction";
                case 68:
                    return "getDeviceConfigurationInfo";
                case 69:
                    return "cancelTaskWindowTransition";
                case 70:
                    return "getTaskSnapshot";
                case 71:
                    return "takeTaskSnapshot";
                case 72:
                    return "getLastResumedActivityUserId";
                case 73:
                    return "updateConfiguration";
                case 74:
                    return "updateLockTaskFeatures";
                case 75:
                    return "registerRemoteAnimationForNextActivityStart";
                case 76:
                    return "registerRemoteAnimationsForDisplay";
                case 77:
                    return "alwaysShowUnsupportedCompileSdkWarning";
                case 78:
                    return "setVrThread";
                case 79:
                    return "setPersistentVrThread";
                case 80:
                    return "stopAppSwitches";
                case 81:
                    return "resumeAppSwitches";
                case 82:
                    return "setActivityController";
                case 83:
                    return "setVoiceKeepAwake";
                case 84:
                    return "getPackageScreenCompatMode";
                case 85:
                    return "setPackageScreenCompatMode";
                case 86:
                    return "getPackageAskScreenCompat";
                case 87:
                    return "setPackageAskScreenCompat";
                case 88:
                    return "clearLaunchParamsForPackages";
                case 89:
                    return "onSplashScreenViewCopyFinished";
                case 90:
                    return "onPictureInPictureStateChanged";
                case 91:
                    return "getFocusedWinPkgName";
                case 92:
                    return "isSecureWindow";
                case 93:
                    return "isIMEShowing";
                case 94:
                    return "inMultiWindowMode";
                case 95:
                    return "notifyKeyguardGoingAwayQuickly";
                case 96:
                    return "isKeyguardLocking";
                case 97:
                    return "notifyAuthenticateSucceed";
                case 98:
                    return "notAllowKeyguardGoingAwayQuickly";
                case 99:
                    return "detachNavigationBarFromApp";
                case 100:
                    return "setRunningRemoteTransitionDelegate";
                case 101:
                    return "startBackNavigation";
                case 102:
                    return "getTopActivityComponent";
                case 103:
                    return "setThreadScheduler";
                case 104:
                    return "setTaskSplitManagerProxy";
                case 105:
                    return "registerTaskSplitListener";
                case 106:
                    return "unregisterTaskSplitListener";
                case 107:
                    return "notifyTaskAnimationResult";
                case 108:
                    return "createTaskAnimation";
                case 109:
                    return "hookReparentToDefaultDisplay";
                case 110:
                    return "hookMultiWindowToMaxV3";
                case 111:
                    return "hookMultiWindowToMinV3";
                case 112:
                    return "hookMultiWindowToCloseV3";
                case 113:
                    return "hookMultiWindowToSmallV3";
                case 114:
                    return "hookMultiWindowToLargeV3";
                case 115:
                    return "hookMultiWindowToCloseV4";
                case 116:
                    return "hookMultiWindowToSmallV4";
                case 117:
                    return "hookMultiWindowToSplit";
                case 118:
                    return "hookMultiWindowMute";
                case 119:
                    return "hookMultiWindowLocation";
                case 120:
                    return "hookFinishMovingLocationV3";
                case 121:
                    return "hookMultiWindowFlingV3";
                case 122:
                    return "hookStartActivityResult";
                case 123:
                    return "hookMultiWindowVisible";
                case 124:
                    return "hookMultiWindowInvisible";
                case 125:
                    return "setTranMultiWindowModeV3";
                case 126:
                    return "setStartInMultiWindow";
                case 127:
                    return "getMultiWindowVersion";
                case 128:
                    return "hasMultiWindow";
                case 129:
                    return "activityInMultiWindow";
                case 130:
                    return "getMulitWindowTopPackage";
                case 131:
                    return "startCurrentAppInMultiWindow";
                case 132:
                    return "isSupportMultiWindow";
                case 133:
                    return "isSplitScreenSupportMultiWindowV4";
                case 134:
                    return "getMultiWindowBlackList";
                case 135:
                    return "setMultiWindowBlackListToSystem";
                case 136:
                    return "setMultiWindowWhiteListToSystem";
                case 137:
                    return "setMultiWindowConfigToSystem";
                case 138:
                    return "getMultiWindowDefaultRect";
                case 139:
                    return "setFinishFixedRotationEnterMultiWindowTransactionV3";
                case 140:
                    return "getWeltWindowLeash";
                case 141:
                    return "removeWeltWindowLeash";
                case 142:
                    return "getDragAndZoomBgLeash";
                case 143:
                    return "hookActiveMultiWindowMoveStartV3";
                case 144:
                    return "hookActiveMultiWindowMoveStartV4";
                case 145:
                    return "hookActiveMultiWindowStartToMove";
                case 146:
                    return "hookActiveMultiWindowMove";
                case 147:
                    return "hookActiveMultiWindowEndMove";
                case 148:
                    return "checkMultiWindowFeatureOn";
                case 149:
                    return "getMultiWindowTopPackages";
                case 150:
                    return "getMuteState";
                case 151:
                    return "getMuteStateV4";
                case 152:
                    return "setMuteState";
                case 153:
                    return "setMuteStateV4";
                case 154:
                    return "hookMultiWindowMuteAethenV4";
                case 155:
                    return "getMultiDisplayAreaTopPackage";
                case 156:
                    return "getMultiDisplayAreaTopPackageV4";
                case 157:
                    return "moveToBottomForMultiWindowV3";
                case 158:
                    return "setMultiWindowAcquireFocus";
                case 159:
                    return "getTopTask";
                case 160:
                    return "getMultiWinTopTask";
                case 161:
                    return "getTaskOrientation";
                case 162:
                    return "minimizeMultiWinToEdge";
                case 163:
                    return "getMultiWindowContentRegion";
                case 164:
                    return "updateZBoostTaskIdWhenToSplit";
                case 165:
                    return "hookExitSplitScreenToMultiWindow";
                case 166:
                    return "hookGetMultiWindowDefaultRect";
                case 167:
                    return "hookGetMultiWindowDefaultRectByTask";
                case 168:
                    return "hookSetMultiWindowDefaultRectResult";
                case 169:
                    return "hookStartMultiWindowFromSplitScreen";
                case 170:
                    return "hookStartMultiWindowFromSplitScreenV4";
                case 171:
                    return "setFinishFixedRotationWithTransaction";
                case 172:
                    return "clearFinishFixedRotationWithTransaction";
                case 173:
                    return "hookStartMultiWindow";
                case 174:
                    return "taskInMultiWindowById";
                case 175:
                    return "addAnimationIconLayer";
                case 176:
                    return "removeAnimationIconLayer";
                case 177:
                    return "getAppTaskByTaskId";
                case 178:
                    return "boostSceneStart";
                case 179:
                    return "boostSceneStartDuration";
                case 180:
                    return "boostSceneEnd";
                case 181:
                    return "boostSceneEndDelay";
                case 182:
                    return "boostStartForLauncher";
                case 183:
                    return "boostEndForLauncher";
                case 184:
                    return "boostStartInLauncher";
                case 185:
                    return "boostEndInLauncher";
                case 186:
                    return "handleDrawThreadsBoost";
                case 187:
                    return "resetBoostThreads";
                case 188:
                    return "getMultiWindowParams";
                case 189:
                    return "setMultiWindowParams";
                case 190:
                    return "isPinnedMode";
                case 191:
                    return "getDefaultRootLeash";
                case 192:
                    return "reparentActivity";
                case 193:
                    return "isSplitScreen";
                case 194:
                    return "setConnectBlackListToSystem";
                case 195:
                    return "hookReserveMultiWindowNumber";
                case 196:
                    return "hookShowBlurLayerFinish";
                case 197:
                    return "setSruWhiteListToSystem";
                default:
                    return null;
            }
        }

        @Override // android.os.Binder
        public String getTransactionName(int transactionCode) {
            return getDefaultTransactionName(transactionCode);
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(IActivityTaskManager.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IActivityTaskManager.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            IApplicationThread _arg0 = IApplicationThread.Stub.asInterface(data.readStrongBinder());
                            String _arg1 = data.readString();
                            String _arg2 = data.readString();
                            Intent _arg3 = (Intent) data.readTypedObject(Intent.CREATOR);
                            String _arg4 = data.readString();
                            IBinder _arg5 = data.readStrongBinder();
                            String _arg6 = data.readString();
                            int _arg7 = data.readInt();
                            int _arg8 = data.readInt();
                            ProfilerInfo _arg9 = (ProfilerInfo) data.readTypedObject(ProfilerInfo.CREATOR);
                            Bundle _arg10 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            int _result = startActivity(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7, _arg8, _arg9, _arg10);
                            reply.writeNoException();
                            reply.writeInt(_result);
                            return true;
                        case 2:
                            IApplicationThread _arg02 = IApplicationThread.Stub.asInterface(data.readStrongBinder());
                            String _arg12 = data.readString();
                            String _arg22 = data.readString();
                            Intent[] _arg32 = (Intent[]) data.createTypedArray(Intent.CREATOR);
                            String[] _arg42 = data.createStringArray();
                            IBinder _arg52 = data.readStrongBinder();
                            Bundle _arg62 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            int _arg72 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result2 = startActivities(_arg02, _arg12, _arg22, _arg32, _arg42, _arg52, _arg62, _arg72);
                            reply.writeNoException();
                            reply.writeInt(_result2);
                            return true;
                        case 3:
                            IApplicationThread _arg03 = IApplicationThread.Stub.asInterface(data.readStrongBinder());
                            String _arg13 = data.readString();
                            String _arg23 = data.readString();
                            Intent _arg33 = (Intent) data.readTypedObject(Intent.CREATOR);
                            String _arg43 = data.readString();
                            IBinder _arg53 = data.readStrongBinder();
                            String _arg63 = data.readString();
                            int _arg73 = data.readInt();
                            int _arg82 = data.readInt();
                            ProfilerInfo _arg92 = (ProfilerInfo) data.readTypedObject(ProfilerInfo.CREATOR);
                            Bundle _arg102 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            int _arg11 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result3 = startActivityAsUser(_arg03, _arg13, _arg23, _arg33, _arg43, _arg53, _arg63, _arg73, _arg82, _arg92, _arg102, _arg11);
                            reply.writeNoException();
                            reply.writeInt(_result3);
                            return true;
                        case 4:
                            IBinder _arg04 = data.readStrongBinder();
                            Intent _arg14 = (Intent) data.readTypedObject(Intent.CREATOR);
                            Bundle _arg24 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result4 = startNextMatchingActivity(_arg04, _arg14, _arg24);
                            reply.writeNoException();
                            reply.writeBoolean(_result4);
                            return true;
                        case 5:
                            Intent _arg05 = (Intent) data.readTypedObject(Intent.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result5 = startDreamActivity(_arg05);
                            reply.writeNoException();
                            reply.writeBoolean(_result5);
                            return true;
                        case 6:
                            IApplicationThread _arg06 = IApplicationThread.Stub.asInterface(data.readStrongBinder());
                            IIntentSender _arg15 = IIntentSender.Stub.asInterface(data.readStrongBinder());
                            IBinder _arg25 = data.readStrongBinder();
                            Intent _arg34 = (Intent) data.readTypedObject(Intent.CREATOR);
                            String _arg44 = data.readString();
                            IBinder _arg54 = data.readStrongBinder();
                            String _arg64 = data.readString();
                            int _arg74 = data.readInt();
                            int _arg83 = data.readInt();
                            int _arg93 = data.readInt();
                            Bundle _arg103 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            int _result6 = startActivityIntentSender(_arg06, _arg15, _arg25, _arg34, _arg44, _arg54, _arg64, _arg74, _arg83, _arg93, _arg103);
                            reply.writeNoException();
                            reply.writeInt(_result6);
                            return true;
                        case 7:
                            IApplicationThread _arg07 = IApplicationThread.Stub.asInterface(data.readStrongBinder());
                            String _arg16 = data.readString();
                            String _arg26 = data.readString();
                            Intent _arg35 = (Intent) data.readTypedObject(Intent.CREATOR);
                            String _arg45 = data.readString();
                            IBinder _arg55 = data.readStrongBinder();
                            String _arg65 = data.readString();
                            int _arg75 = data.readInt();
                            int _arg84 = data.readInt();
                            ProfilerInfo _arg94 = (ProfilerInfo) data.readTypedObject(ProfilerInfo.CREATOR);
                            Bundle _arg104 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            int _arg112 = data.readInt();
                            data.enforceNoDataAvail();
                            WaitResult _result7 = startActivityAndWait(_arg07, _arg16, _arg26, _arg35, _arg45, _arg55, _arg65, _arg75, _arg84, _arg94, _arg104, _arg112);
                            reply.writeNoException();
                            reply.writeTypedObject(_result7, 1);
                            return true;
                        case 8:
                            IApplicationThread _arg08 = IApplicationThread.Stub.asInterface(data.readStrongBinder());
                            String _arg17 = data.readString();
                            String _arg27 = data.readString();
                            Intent _arg36 = (Intent) data.readTypedObject(Intent.CREATOR);
                            String _arg46 = data.readString();
                            IBinder _arg56 = data.readStrongBinder();
                            String _arg66 = data.readString();
                            int _arg76 = data.readInt();
                            int _arg85 = data.readInt();
                            Configuration _arg95 = (Configuration) data.readTypedObject(Configuration.CREATOR);
                            Bundle _arg105 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            int _arg113 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result8 = startActivityWithConfig(_arg08, _arg17, _arg27, _arg36, _arg46, _arg56, _arg66, _arg76, _arg85, _arg95, _arg105, _arg113);
                            reply.writeNoException();
                            reply.writeInt(_result8);
                            return true;
                        case 9:
                            String _arg09 = data.readString();
                            String _arg18 = data.readString();
                            int _arg28 = data.readInt();
                            int _arg37 = data.readInt();
                            Intent _arg47 = (Intent) data.readTypedObject(Intent.CREATOR);
                            String _arg57 = data.readString();
                            IVoiceInteractionSession _arg67 = IVoiceInteractionSession.Stub.asInterface(data.readStrongBinder());
                            IVoiceInteractor _arg77 = IVoiceInteractor.Stub.asInterface(data.readStrongBinder());
                            int _arg86 = data.readInt();
                            ProfilerInfo _arg96 = (ProfilerInfo) data.readTypedObject(ProfilerInfo.CREATOR);
                            Bundle _arg106 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            int _arg114 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result9 = startVoiceActivity(_arg09, _arg18, _arg28, _arg37, _arg47, _arg57, _arg67, _arg77, _arg86, _arg96, _arg106, _arg114);
                            reply.writeNoException();
                            reply.writeInt(_result9);
                            return true;
                        case 10:
                            IBinder _arg010 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            String _result10 = getVoiceInteractorPackageName(_arg010);
                            reply.writeNoException();
                            reply.writeString(_result10);
                            return true;
                        case 11:
                            String _arg011 = data.readString();
                            String _arg19 = data.readString();
                            int _arg29 = data.readInt();
                            int _arg38 = data.readInt();
                            Intent _arg48 = (Intent) data.readTypedObject(Intent.CREATOR);
                            String _arg58 = data.readString();
                            Bundle _arg68 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            int _arg78 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result11 = startAssistantActivity(_arg011, _arg19, _arg29, _arg38, _arg48, _arg58, _arg68, _arg78);
                            reply.writeNoException();
                            reply.writeInt(_result11);
                            return true;
                        case 12:
                            IApplicationThread _arg012 = IApplicationThread.Stub.asInterface(data.readStrongBinder());
                            String _arg110 = data.readString();
                            String _arg210 = data.readString();
                            int _arg39 = data.readInt();
                            int _arg49 = data.readInt();
                            Intent _arg59 = (Intent) data.readTypedObject(Intent.CREATOR);
                            int _arg69 = data.readInt();
                            int _arg79 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result12 = startActivityFromGameSession(_arg012, _arg110, _arg210, _arg39, _arg49, _arg59, _arg69, _arg79);
                            reply.writeNoException();
                            reply.writeInt(_result12);
                            return true;
                        case 13:
                            Intent _arg013 = (Intent) data.readTypedObject(Intent.CREATOR);
                            long _arg111 = data.readLong();
                            IRecentsAnimationRunner _arg211 = IRecentsAnimationRunner.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            startRecentsActivity(_arg013, _arg111, _arg211);
                            reply.writeNoException();
                            return true;
                        case 14:
                            int _arg014 = data.readInt();
                            Bundle _arg115 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            int _result13 = startActivityFromRecents(_arg014, _arg115);
                            reply.writeNoException();
                            reply.writeInt(_result13);
                            return true;
                        case 15:
                            IApplicationThread _arg015 = IApplicationThread.Stub.asInterface(data.readStrongBinder());
                            String _arg116 = data.readString();
                            Intent _arg212 = (Intent) data.readTypedObject(Intent.CREATOR);
                            String _arg310 = data.readString();
                            IBinder _arg410 = data.readStrongBinder();
                            String _arg510 = data.readString();
                            int _arg610 = data.readInt();
                            int _arg710 = data.readInt();
                            ProfilerInfo _arg87 = (ProfilerInfo) data.readTypedObject(ProfilerInfo.CREATOR);
                            Bundle _arg97 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            boolean _arg107 = data.readBoolean();
                            int _arg117 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result14 = startActivityAsCaller(_arg015, _arg116, _arg212, _arg310, _arg410, _arg510, _arg610, _arg710, _arg87, _arg97, _arg107, _arg117);
                            reply.writeNoException();
                            reply.writeInt(_result14);
                            return true;
                        case 16:
                            int _arg016 = data.readInt();
                            Intent _arg118 = (Intent) data.readTypedObject(Intent.CREATOR);
                            String _arg213 = data.readString();
                            int _arg311 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result15 = isActivityStartAllowedOnDisplay(_arg016, _arg118, _arg213, _arg311);
                            reply.writeNoException();
                            reply.writeBoolean(_result15);
                            return true;
                        case 17:
                            unhandledBack();
                            reply.writeNoException();
                            return true;
                        case 18:
                            IActivityClientController _result16 = getActivityClientController();
                            reply.writeNoException();
                            reply.writeStrongInterface(_result16);
                            return true;
                        case 19:
                            int _result17 = getFrontActivityScreenCompatMode();
                            reply.writeNoException();
                            reply.writeInt(_result17);
                            return true;
                        case 20:
                            int _arg017 = data.readInt();
                            data.enforceNoDataAvail();
                            setFrontActivityScreenCompatMode(_arg017);
                            reply.writeNoException();
                            return true;
                        case 21:
                            int _arg018 = data.readInt();
                            data.enforceNoDataAvail();
                            setFocusedTask(_arg018);
                            reply.writeNoException();
                            return true;
                        case 22:
                            int _arg019 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result18 = removeTask(_arg019);
                            reply.writeNoException();
                            reply.writeBoolean(_result18);
                            return true;
                        case 23:
                            removeAllVisibleRecentTasks();
                            reply.writeNoException();
                            return true;
                        case 24:
                            int _arg020 = data.readInt();
                            boolean _arg119 = data.readBoolean();
                            boolean _arg214 = data.readBoolean();
                            data.enforceNoDataAvail();
                            List<ActivityManager.RunningTaskInfo> _result19 = getTasks(_arg020, _arg119, _arg214);
                            reply.writeNoException();
                            reply.writeTypedList(_result19);
                            return true;
                        case 25:
                            IApplicationThread _arg021 = IApplicationThread.Stub.asInterface(data.readStrongBinder());
                            String _arg120 = data.readString();
                            int _arg215 = data.readInt();
                            int _arg312 = data.readInt();
                            Bundle _arg411 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            moveTaskToFront(_arg021, _arg120, _arg215, _arg312, _arg411);
                            reply.writeNoException();
                            return true;
                        case 26:
                            int _arg022 = data.readInt();
                            int _arg121 = data.readInt();
                            int _arg216 = data.readInt();
                            data.enforceNoDataAvail();
                            ParceledListSlice<ActivityManager.RecentTaskInfo> _result20 = getRecentTasks(_arg022, _arg121, _arg216);
                            reply.writeNoException();
                            reply.writeTypedObject(_result20, 1);
                            return true;
                        case 27:
                            boolean _result21 = isTopActivityImmersive();
                            reply.writeNoException();
                            reply.writeBoolean(_result21);
                            return true;
                        case 28:
                            int _arg023 = data.readInt();
                            data.enforceNoDataAvail();
                            ActivityManager.TaskDescription _result22 = getTaskDescription(_arg023);
                            reply.writeNoException();
                            reply.writeTypedObject(_result22, 1);
                            return true;
                        case 29:
                            IBinder _arg024 = data.readStrongBinder();
                            Bundle _arg122 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            AssistStructure _arg217 = (AssistStructure) data.readTypedObject(AssistStructure.CREATOR);
                            AssistContent _arg313 = (AssistContent) data.readTypedObject(AssistContent.CREATOR);
                            Uri _arg412 = (Uri) data.readTypedObject(Uri.CREATOR);
                            data.enforceNoDataAvail();
                            reportAssistContextExtras(_arg024, _arg122, _arg217, _arg313, _arg412);
                            reply.writeNoException();
                            return true;
                        case 30:
                            int _arg025 = data.readInt();
                            data.enforceNoDataAvail();
                            setFocusedRootTask(_arg025);
                            reply.writeNoException();
                            return true;
                        case 31:
                            ActivityTaskManager.RootTaskInfo _result23 = getFocusedRootTaskInfo();
                            reply.writeNoException();
                            reply.writeTypedObject(_result23, 1);
                            return true;
                        case 32:
                            int _arg026 = data.readInt();
                            data.enforceNoDataAvail();
                            Rect _result24 = getTaskBounds(_arg026);
                            reply.writeNoException();
                            reply.writeTypedObject(_result24, 1);
                            return true;
                        case 33:
                            boolean _arg027 = data.readBoolean();
                            data.enforceNoDataAvail();
                            cancelRecentsAnimation(_arg027);
                            reply.writeNoException();
                            return true;
                        case 34:
                            int _arg028 = data.readInt();
                            String[] _arg123 = data.createStringArray();
                            data.enforceNoDataAvail();
                            updateLockTaskPackages(_arg028, _arg123);
                            reply.writeNoException();
                            return true;
                        case 35:
                            boolean _result25 = isInLockTaskMode();
                            reply.writeNoException();
                            reply.writeBoolean(_result25);
                            return true;
                        case 36:
                            int _result26 = getLockTaskModeState();
                            reply.writeNoException();
                            reply.writeInt(_result26);
                            return true;
                        case 37:
                            String _arg029 = data.readString();
                            data.enforceNoDataAvail();
                            List<IBinder> _result27 = getAppTasks(_arg029);
                            reply.writeNoException();
                            reply.writeBinderList(_result27);
                            return true;
                        case 38:
                            int _arg030 = data.readInt();
                            data.enforceNoDataAvail();
                            startSystemLockTaskMode(_arg030);
                            reply.writeNoException();
                            return true;
                        case 39:
                            stopSystemLockTaskMode();
                            reply.writeNoException();
                            return true;
                        case 40:
                            IBinder _arg031 = data.readStrongBinder();
                            IVoiceInteractionSession _arg032 = IVoiceInteractionSession.Stub.asInterface(_arg031);
                            data.enforceNoDataAvail();
                            finishVoiceTask(_arg032);
                            reply.writeNoException();
                            return true;
                        case 41:
                            IBinder _arg033 = data.readStrongBinder();
                            Intent _arg124 = (Intent) data.readTypedObject(Intent.CREATOR);
                            ActivityManager.TaskDescription _arg218 = (ActivityManager.TaskDescription) data.readTypedObject(ActivityManager.TaskDescription.CREATOR);
                            Bitmap _arg314 = (Bitmap) data.readTypedObject(Bitmap.CREATOR);
                            data.enforceNoDataAvail();
                            int _result28 = addAppTask(_arg033, _arg124, _arg218, _arg314);
                            reply.writeNoException();
                            reply.writeInt(_result28);
                            return true;
                        case 42:
                            Point _result29 = getAppTaskThumbnailSize();
                            reply.writeNoException();
                            reply.writeTypedObject(_result29, 1);
                            return true;
                        case 43:
                            IApplicationThread _arg034 = IApplicationThread.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            releaseSomeActivities(_arg034);
                            return true;
                        case 44:
                            String _arg035 = data.readString();
                            int _arg125 = data.readInt();
                            data.enforceNoDataAvail();
                            Bitmap _result30 = getTaskDescriptionIcon(_arg035, _arg125);
                            reply.writeNoException();
                            reply.writeTypedObject(_result30, 1);
                            return true;
                        case 45:
                            ITaskStackListener _arg036 = ITaskStackListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            registerTaskStackListener(_arg036);
                            reply.writeNoException();
                            return true;
                        case 46:
                            ITaskStackListener _arg037 = ITaskStackListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            unregisterTaskStackListener(_arg037);
                            reply.writeNoException();
                            return true;
                        case 47:
                            int _arg038 = data.readInt();
                            int _arg126 = data.readInt();
                            data.enforceNoDataAvail();
                            setTaskResizeable(_arg038, _arg126);
                            reply.writeNoException();
                            return true;
                        case 48:
                            int _arg039 = data.readInt();
                            Rect _arg127 = (Rect) data.readTypedObject(Rect.CREATOR);
                            int _arg219 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result31 = resizeTask(_arg039, _arg127, _arg219);
                            reply.writeNoException();
                            reply.writeBoolean(_result31);
                            return true;
                        case 49:
                            int _arg040 = data.readInt();
                            int _arg128 = data.readInt();
                            data.enforceNoDataAvail();
                            moveRootTaskToDisplay(_arg040, _arg128);
                            reply.writeNoException();
                            return true;
                        case 50:
                            int _arg041 = data.readInt();
                            int _arg129 = data.readInt();
                            boolean _arg220 = data.readBoolean();
                            data.enforceNoDataAvail();
                            moveTaskToRootTask(_arg041, _arg129, _arg220);
                            reply.writeNoException();
                            return true;
                        case 51:
                            int[] _arg042 = data.createIntArray();
                            data.enforceNoDataAvail();
                            removeRootTasksInWindowingModes(_arg042);
                            reply.writeNoException();
                            return true;
                        case 52:
                            int[] _arg043 = data.createIntArray();
                            data.enforceNoDataAvail();
                            removeRootTasksWithActivityTypes(_arg043);
                            reply.writeNoException();
                            return true;
                        case 53:
                            List<ActivityTaskManager.RootTaskInfo> _result32 = getAllRootTaskInfos();
                            reply.writeNoException();
                            reply.writeTypedList(_result32);
                            return true;
                        case 54:
                            int _arg044 = data.readInt();
                            int _arg130 = data.readInt();
                            data.enforceNoDataAvail();
                            ActivityTaskManager.RootTaskInfo _result33 = getRootTaskInfo(_arg044, _arg130);
                            reply.writeNoException();
                            reply.writeTypedObject(_result33, 1);
                            return true;
                        case 55:
                            int _arg045 = data.readInt();
                            data.enforceNoDataAvail();
                            List<ActivityTaskManager.RootTaskInfo> _result34 = getAllRootTaskInfosOnDisplay(_arg045);
                            reply.writeNoException();
                            reply.writeTypedList(_result34);
                            return true;
                        case 56:
                            int _arg046 = data.readInt();
                            int _arg131 = data.readInt();
                            int _arg221 = data.readInt();
                            data.enforceNoDataAvail();
                            ActivityTaskManager.RootTaskInfo _result35 = getRootTaskInfoOnDisplay(_arg046, _arg131, _arg221);
                            reply.writeNoException();
                            reply.writeTypedObject(_result35, 1);
                            return true;
                        case 57:
                            boolean _arg047 = data.readBoolean();
                            boolean _arg132 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setLockScreenShown(_arg047, _arg132);
                            reply.writeNoException();
                            return true;
                        case 58:
                            int _arg048 = data.readInt();
                            data.enforceNoDataAvail();
                            Bundle _result36 = getAssistContextExtras(_arg048);
                            reply.writeNoException();
                            reply.writeTypedObject(_result36, 1);
                            return true;
                        case 59:
                            int _arg049 = data.readInt();
                            IAssistDataReceiver _arg133 = IAssistDataReceiver.Stub.asInterface(data.readStrongBinder());
                            Bundle _arg222 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            IBinder _arg315 = data.readStrongBinder();
                            boolean _arg413 = data.readBoolean();
                            boolean _arg511 = data.readBoolean();
                            data.enforceNoDataAvail();
                            boolean _result37 = requestAssistContextExtras(_arg049, _arg133, _arg222, _arg315, _arg413, _arg511);
                            reply.writeNoException();
                            reply.writeBoolean(_result37);
                            return true;
                        case 60:
                            IAssistDataReceiver _arg050 = IAssistDataReceiver.Stub.asInterface(data.readStrongBinder());
                            Bundle _arg134 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            IBinder _arg223 = data.readStrongBinder();
                            int _arg316 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result38 = requestAutofillData(_arg050, _arg134, _arg223, _arg316);
                            reply.writeNoException();
                            reply.writeBoolean(_result38);
                            return true;
                        case 61:
                            boolean _result39 = isAssistDataAllowedOnCurrentActivity();
                            reply.writeNoException();
                            reply.writeBoolean(_result39);
                            return true;
                        case 62:
                            IAssistDataReceiver _arg051 = IAssistDataReceiver.Stub.asInterface(data.readStrongBinder());
                            int _arg135 = data.readInt();
                            String _arg224 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result40 = requestAssistDataForTask(_arg051, _arg135, _arg224);
                            reply.writeNoException();
                            reply.writeBoolean(_result40);
                            return true;
                        case 63:
                            int _arg052 = data.readInt();
                            data.enforceNoDataAvail();
                            keyguardGoingAway(_arg052);
                            reply.writeNoException();
                            return true;
                        case 64:
                            boolean _arg053 = data.readBoolean();
                            data.enforceNoDataAvail();
                            suppressResizeConfigChanges(_arg053);
                            reply.writeNoException();
                            return true;
                        case 65:
                            IWindowOrganizerController _result41 = getWindowOrganizerController();
                            reply.writeNoException();
                            reply.writeStrongInterface(_result41);
                            return true;
                        case 66:
                            boolean _arg054 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setSplitScreenResizing(_arg054);
                            reply.writeNoException();
                            return true;
                        case 67:
                            boolean _result42 = supportsLocalVoiceInteraction();
                            reply.writeNoException();
                            reply.writeBoolean(_result42);
                            return true;
                        case 68:
                            ConfigurationInfo _result43 = getDeviceConfigurationInfo();
                            reply.writeNoException();
                            reply.writeTypedObject(_result43, 1);
                            return true;
                        case 69:
                            int _arg055 = data.readInt();
                            data.enforceNoDataAvail();
                            cancelTaskWindowTransition(_arg055);
                            reply.writeNoException();
                            return true;
                        case 70:
                            int _arg056 = data.readInt();
                            boolean _arg136 = data.readBoolean();
                            data.enforceNoDataAvail();
                            TaskSnapshot _result44 = getTaskSnapshot(_arg056, _arg136);
                            reply.writeNoException();
                            reply.writeTypedObject(_result44, 1);
                            return true;
                        case 71:
                            int _arg057 = data.readInt();
                            data.enforceNoDataAvail();
                            TaskSnapshot _result45 = takeTaskSnapshot(_arg057);
                            reply.writeNoException();
                            reply.writeTypedObject(_result45, 1);
                            return true;
                        case 72:
                            int _result46 = getLastResumedActivityUserId();
                            reply.writeNoException();
                            reply.writeInt(_result46);
                            return true;
                        case 73:
                            Configuration _arg058 = (Configuration) data.readTypedObject(Configuration.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result47 = updateConfiguration(_arg058);
                            reply.writeNoException();
                            reply.writeBoolean(_result47);
                            return true;
                        case 74:
                            int _arg059 = data.readInt();
                            int _arg137 = data.readInt();
                            data.enforceNoDataAvail();
                            updateLockTaskFeatures(_arg059, _arg137);
                            reply.writeNoException();
                            return true;
                        case 75:
                            String _arg060 = data.readString();
                            RemoteAnimationAdapter _arg138 = (RemoteAnimationAdapter) data.readTypedObject(RemoteAnimationAdapter.CREATOR);
                            IBinder _arg225 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            registerRemoteAnimationForNextActivityStart(_arg060, _arg138, _arg225);
                            reply.writeNoException();
                            return true;
                        case 76:
                            int _arg061 = data.readInt();
                            RemoteAnimationDefinition _arg139 = (RemoteAnimationDefinition) data.readTypedObject(RemoteAnimationDefinition.CREATOR);
                            data.enforceNoDataAvail();
                            registerRemoteAnimationsForDisplay(_arg061, _arg139);
                            reply.writeNoException();
                            return true;
                        case 77:
                            ComponentName _arg062 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            alwaysShowUnsupportedCompileSdkWarning(_arg062);
                            reply.writeNoException();
                            return true;
                        case 78:
                            int _arg063 = data.readInt();
                            data.enforceNoDataAvail();
                            setVrThread(_arg063);
                            reply.writeNoException();
                            return true;
                        case 79:
                            int _arg064 = data.readInt();
                            data.enforceNoDataAvail();
                            setPersistentVrThread(_arg064);
                            reply.writeNoException();
                            return true;
                        case 80:
                            stopAppSwitches();
                            reply.writeNoException();
                            return true;
                        case 81:
                            resumeAppSwitches();
                            reply.writeNoException();
                            return true;
                        case 82:
                            IActivityController _arg065 = IActivityController.Stub.asInterface(data.readStrongBinder());
                            boolean _arg140 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setActivityController(_arg065, _arg140);
                            reply.writeNoException();
                            return true;
                        case 83:
                            IVoiceInteractionSession _arg066 = IVoiceInteractionSession.Stub.asInterface(data.readStrongBinder());
                            boolean _arg141 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setVoiceKeepAwake(_arg066, _arg141);
                            reply.writeNoException();
                            return true;
                        case 84:
                            String _arg067 = data.readString();
                            data.enforceNoDataAvail();
                            int _result48 = getPackageScreenCompatMode(_arg067);
                            reply.writeNoException();
                            reply.writeInt(_result48);
                            return true;
                        case 85:
                            String _arg068 = data.readString();
                            int _arg142 = data.readInt();
                            data.enforceNoDataAvail();
                            setPackageScreenCompatMode(_arg068, _arg142);
                            reply.writeNoException();
                            return true;
                        case 86:
                            String _arg069 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result49 = getPackageAskScreenCompat(_arg069);
                            reply.writeNoException();
                            reply.writeBoolean(_result49);
                            return true;
                        case 87:
                            String _arg070 = data.readString();
                            boolean _arg143 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setPackageAskScreenCompat(_arg070, _arg143);
                            reply.writeNoException();
                            return true;
                        case 88:
                            List<String> _arg071 = data.createStringArrayList();
                            data.enforceNoDataAvail();
                            clearLaunchParamsForPackages(_arg071);
                            reply.writeNoException();
                            return true;
                        case 89:
                            int _arg072 = data.readInt();
                            SplashScreenView.SplashScreenViewParcelable _arg144 = (SplashScreenView.SplashScreenViewParcelable) data.readTypedObject(SplashScreenView.SplashScreenViewParcelable.CREATOR);
                            data.enforceNoDataAvail();
                            onSplashScreenViewCopyFinished(_arg072, _arg144);
                            reply.writeNoException();
                            return true;
                        case 90:
                            PictureInPictureUiState _arg073 = (PictureInPictureUiState) data.readTypedObject(PictureInPictureUiState.CREATOR);
                            data.enforceNoDataAvail();
                            onPictureInPictureStateChanged(_arg073);
                            reply.writeNoException();
                            return true;
                        case 91:
                            String _result50 = getFocusedWinPkgName();
                            reply.writeNoException();
                            reply.writeString(_result50);
                            return true;
                        case 92:
                            boolean _result51 = isSecureWindow();
                            reply.writeNoException();
                            reply.writeBoolean(_result51);
                            return true;
                        case 93:
                            boolean _result52 = isIMEShowing();
                            reply.writeNoException();
                            reply.writeBoolean(_result52);
                            return true;
                        case 94:
                            boolean _result53 = inMultiWindowMode();
                            reply.writeNoException();
                            reply.writeBoolean(_result53);
                            return true;
                        case 95:
                            boolean _arg074 = data.readBoolean();
                            data.enforceNoDataAvail();
                            notifyKeyguardGoingAwayQuickly(_arg074);
                            reply.writeNoException();
                            return true;
                        case 96:
                            boolean _result54 = isKeyguardLocking();
                            reply.writeNoException();
                            reply.writeBoolean(_result54);
                            return true;
                        case 97:
                            boolean _arg075 = data.readBoolean();
                            data.enforceNoDataAvail();
                            notifyAuthenticateSucceed(_arg075);
                            reply.writeNoException();
                            return true;
                        case 98:
                            boolean _arg076 = data.readBoolean();
                            data.enforceNoDataAvail();
                            notAllowKeyguardGoingAwayQuickly(_arg076);
                            reply.writeNoException();
                            return true;
                        case 99:
                            IBinder _arg077 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            detachNavigationBarFromApp(_arg077);
                            reply.writeNoException();
                            return true;
                        case 100:
                            IApplicationThread _arg078 = IApplicationThread.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            setRunningRemoteTransitionDelegate(_arg078);
                            reply.writeNoException();
                            return true;
                        case 101:
                            boolean _arg079 = data.readBoolean();
                            data.enforceNoDataAvail();
                            BackNavigationInfo _result55 = startBackNavigation(_arg079);
                            reply.writeNoException();
                            reply.writeTypedObject(_result55, 1);
                            return true;
                        case 102:
                            ComponentName _result56 = getTopActivityComponent();
                            reply.writeNoException();
                            reply.writeTypedObject(_result56, 1);
                            return true;
                        case 103:
                            int _arg080 = data.readInt();
                            int _arg145 = data.readInt();
                            int _arg226 = data.readInt();
                            data.enforceNoDataAvail();
                            setThreadScheduler(_arg080, _arg145, _arg226);
                            reply.writeNoException();
                            return true;
                        case 104:
                            ITaskSplitManager _arg081 = ITaskSplitManager.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            setTaskSplitManagerProxy(_arg081);
                            reply.writeNoException();
                            return true;
                        case 105:
                            ITaskSplitManagerListener _arg082 = ITaskSplitManagerListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            registerTaskSplitListener(_arg082);
                            reply.writeNoException();
                            return true;
                        case 106:
                            ITaskSplitManagerListener _arg083 = ITaskSplitManagerListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            unregisterTaskSplitListener(_arg083);
                            reply.writeNoException();
                            return true;
                        case 107:
                            int _arg084 = data.readInt();
                            ITaskAnimation _arg146 = ITaskAnimation.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            notifyTaskAnimationResult(_arg084, _arg146);
                            reply.writeNoException();
                            return true;
                        case 108:
                            IBinder _arg085 = data.readStrongBinder();
                            int _arg147 = data.readInt();
                            MultiTaskRemoteAnimationAdapter _arg227 = (MultiTaskRemoteAnimationAdapter) data.readTypedObject(MultiTaskRemoteAnimationAdapter.CREATOR);
                            data.enforceNoDataAvail();
                            ITaskAnimation _result57 = createTaskAnimation(_arg085, _arg147, _arg227);
                            reply.writeNoException();
                            reply.writeStrongInterface(_result57);
                            return true;
                        case 109:
                            int _arg086 = data.readInt();
                            int _arg148 = data.readInt();
                            data.enforceNoDataAvail();
                            hookReparentToDefaultDisplay(_arg086, _arg148);
                            reply.writeNoException();
                            return true;
                        case 110:
                            IWindow _arg087 = IWindow.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            hookMultiWindowToMaxV3(_arg087);
                            reply.writeNoException();
                            return true;
                        case 111:
                            IWindow _arg088 = IWindow.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            hookMultiWindowToMinV3(_arg088);
                            reply.writeNoException();
                            return true;
                        case 112:
                            IWindow _arg089 = IWindow.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            hookMultiWindowToCloseV3(_arg089);
                            reply.writeNoException();
                            return true;
                        case 113:
                            IWindow _arg090 = IWindow.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            hookMultiWindowToSmallV3(_arg090);
                            reply.writeNoException();
                            return true;
                        case 114:
                            IWindow _arg091 = IWindow.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            hookMultiWindowToLargeV3(_arg091);
                            reply.writeNoException();
                            return true;
                        case 115:
                            int _arg092 = data.readInt();
                            int _arg149 = data.readInt();
                            data.enforceNoDataAvail();
                            hookMultiWindowToCloseV4(_arg092, _arg149);
                            reply.writeNoException();
                            return true;
                        case 116:
                            int _arg093 = data.readInt();
                            int _arg150 = data.readInt();
                            data.enforceNoDataAvail();
                            hookMultiWindowToSmallV4(_arg093, _arg150);
                            reply.writeNoException();
                            return true;
                        case 117:
                            int _arg094 = data.readInt();
                            int _arg151 = data.readInt();
                            data.enforceNoDataAvail();
                            hookMultiWindowToSplit(_arg094, _arg151);
                            reply.writeNoException();
                            return true;
                        case 118:
                            IWindow _arg095 = IWindow.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            hookMultiWindowMute(_arg095);
                            reply.writeNoException();
                            return true;
                        case 119:
                            IWindow _arg096 = IWindow.Stub.asInterface(data.readStrongBinder());
                            int _arg152 = data.readInt();
                            int _arg228 = data.readInt();
                            int _arg317 = data.readInt();
                            int _arg414 = data.readInt();
                            data.enforceNoDataAvail();
                            hookMultiWindowLocation(_arg096, _arg152, _arg228, _arg317, _arg414);
                            reply.writeNoException();
                            return true;
                        case 120:
                            IWindow _arg097 = IWindow.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            hookFinishMovingLocationV3(_arg097);
                            reply.writeNoException();
                            return true;
                        case 121:
                            IWindow _arg098 = IWindow.Stub.asInterface(data.readStrongBinder());
                            MotionEvent _arg153 = (MotionEvent) data.readTypedObject(MotionEvent.CREATOR);
                            MotionEvent _arg229 = (MotionEvent) data.readTypedObject(MotionEvent.CREATOR);
                            float _arg318 = data.readFloat();
                            float _arg415 = data.readFloat();
                            data.enforceNoDataAvail();
                            hookMultiWindowFlingV3(_arg098, _arg153, _arg229, _arg318, _arg415);
                            reply.writeNoException();
                            return true;
                        case 122:
                            int _arg099 = data.readInt();
                            Rect _arg154 = (Rect) data.readTypedObject(Rect.CREATOR);
                            data.enforceNoDataAvail();
                            hookStartActivityResult(_arg099, _arg154);
                            reply.writeNoException();
                            return true;
                        case 123:
                            hookMultiWindowVisible();
                            reply.writeNoException();
                            return true;
                        case 124:
                            hookMultiWindowInvisible();
                            reply.writeNoException();
                            return true;
                        case 125:
                            int _arg0100 = data.readInt();
                            data.enforceNoDataAvail();
                            setTranMultiWindowModeV3(_arg0100);
                            reply.writeNoException();
                            return true;
                        case 126:
                            String _arg0101 = data.readString();
                            int _arg155 = data.readInt();
                            int _arg230 = data.readInt();
                            int _arg319 = data.readInt();
                            data.enforceNoDataAvail();
                            setStartInMultiWindow(_arg0101, _arg155, _arg230, _arg319);
                            reply.writeNoException();
                            return true;
                        case 127:
                            String _result58 = getMultiWindowVersion();
                            reply.writeNoException();
                            reply.writeString(_result58);
                            return true;
                        case 128:
                            boolean _result59 = hasMultiWindow();
                            reply.writeNoException();
                            reply.writeBoolean(_result59);
                            return true;
                        case 129:
                            String _arg0102 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result60 = activityInMultiWindow(_arg0102);
                            reply.writeNoException();
                            reply.writeBoolean(_result60);
                            return true;
                        case 130:
                            String _result61 = getMulitWindowTopPackage();
                            reply.writeNoException();
                            reply.writeString(_result61);
                            return true;
                        case 131:
                            boolean _arg0103 = data.readBoolean();
                            int _arg156 = data.readInt();
                            data.enforceNoDataAvail();
                            startCurrentAppInMultiWindow(_arg0103, _arg156);
                            reply.writeNoException();
                            return true;
                        case 132:
                            boolean _result62 = isSupportMultiWindow();
                            reply.writeNoException();
                            reply.writeBoolean(_result62);
                            return true;
                        case 133:
                            int _arg0104 = data.readInt();
                            ActivityManager.RunningTaskInfo _arg157 = (ActivityManager.RunningTaskInfo) data.readTypedObject(ActivityManager.RunningTaskInfo.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result63 = isSplitScreenSupportMultiWindowV4(_arg0104, _arg157);
                            reply.writeNoException();
                            reply.writeBoolean(_result63);
                            return true;
                        case 134:
                            List<String> _result64 = getMultiWindowBlackList();
                            reply.writeNoException();
                            reply.writeStringList(_result64);
                            return true;
                        case 135:
                            List<String> _arg0105 = data.createStringArrayList();
                            data.enforceNoDataAvail();
                            setMultiWindowBlackListToSystem(_arg0105);
                            reply.writeNoException();
                            return true;
                        case 136:
                            List<String> _arg0106 = data.createStringArrayList();
                            data.enforceNoDataAvail();
                            setMultiWindowWhiteListToSystem(_arg0106);
                            reply.writeNoException();
                            return true;
                        case 137:
                            String _arg0107 = data.readString();
                            List<String> _arg158 = data.createStringArrayList();
                            data.enforceNoDataAvail();
                            setMultiWindowConfigToSystem(_arg0107, _arg158);
                            reply.writeNoException();
                            return true;
                        case 138:
                            Rect _result65 = getMultiWindowDefaultRect();
                            reply.writeNoException();
                            reply.writeTypedObject(_result65, 1);
                            return true;
                        case 139:
                            SurfaceControl _arg0108 = (SurfaceControl) data.readTypedObject(SurfaceControl.CREATOR);
                            int _arg159 = data.readInt();
                            int _arg231 = data.readInt();
                            int _arg320 = data.readInt();
                            float _arg416 = data.readFloat();
                            data.enforceNoDataAvail();
                            setFinishFixedRotationEnterMultiWindowTransactionV3(_arg0108, _arg159, _arg231, _arg320, _arg416);
                            reply.writeNoException();
                            return true;
                        case 140:
                            int _arg0109 = data.readInt();
                            int _arg160 = data.readInt();
                            int _arg232 = data.readInt();
                            int _arg321 = data.readInt();
                            boolean _arg417 = data.readBoolean();
                            data.enforceNoDataAvail();
                            SurfaceControl _result66 = getWeltWindowLeash(_arg0109, _arg160, _arg232, _arg321, _arg417);
                            reply.writeNoException();
                            reply.writeTypedObject(_result66, 1);
                            return true;
                        case 141:
                            SurfaceControl _arg0110 = (SurfaceControl) data.readTypedObject(SurfaceControl.CREATOR);
                            data.enforceNoDataAvail();
                            removeWeltWindowLeash(_arg0110);
                            reply.writeNoException();
                            return true;
                        case 142:
                            int _arg0111 = data.readInt();
                            int _arg161 = data.readInt();
                            int _arg233 = data.readInt();
                            int _arg322 = data.readInt();
                            boolean _arg418 = data.readBoolean();
                            data.enforceNoDataAvail();
                            SurfaceControl _result67 = getDragAndZoomBgLeash(_arg0111, _arg161, _arg233, _arg322, _arg418);
                            reply.writeNoException();
                            reply.writeTypedObject(_result67, 1);
                            return true;
                        case 143:
                            hookActiveMultiWindowMoveStartV3();
                            reply.writeNoException();
                            return true;
                        case 144:
                            int _arg0112 = data.readInt();
                            int _arg162 = data.readInt();
                            data.enforceNoDataAvail();
                            hookActiveMultiWindowMoveStartV4(_arg0112, _arg162);
                            reply.writeNoException();
                            return true;
                        case 145:
                            IBinder _arg0113 = data.readStrongBinder();
                            int _arg163 = data.readInt();
                            int _arg234 = data.readInt();
                            MotionEvent _arg323 = (MotionEvent) data.readTypedObject(MotionEvent.CREATOR);
                            Point _arg419 = (Point) data.readTypedObject(Point.CREATOR);
                            data.enforceNoDataAvail();
                            hookActiveMultiWindowStartToMove(_arg0113, _arg163, _arg234, _arg323, _arg419);
                            reply.writeNoException();
                            return true;
                        case 146:
                            int _arg0114 = data.readInt();
                            int _arg164 = data.readInt();
                            MotionEvent _arg235 = (MotionEvent) data.readTypedObject(MotionEvent.CREATOR);
                            data.enforceNoDataAvail();
                            hookActiveMultiWindowMove(_arg0114, _arg164, _arg235);
                            reply.writeNoException();
                            return true;
                        case 147:
                            int _arg0115 = data.readInt();
                            int _arg165 = data.readInt();
                            MotionEvent _arg236 = (MotionEvent) data.readTypedObject(MotionEvent.CREATOR);
                            data.enforceNoDataAvail();
                            hookActiveMultiWindowEndMove(_arg0115, _arg165, _arg236);
                            reply.writeNoException();
                            return true;
                        case 148:
                            boolean _result68 = checkMultiWindowFeatureOn();
                            reply.writeNoException();
                            reply.writeBoolean(_result68);
                            return true;
                        case 149:
                            List<String> _result69 = getMultiWindowTopPackages();
                            reply.writeNoException();
                            reply.writeStringList(_result69);
                            return true;
                        case 150:
                            boolean _result70 = getMuteState();
                            reply.writeNoException();
                            reply.writeBoolean(_result70);
                            return true;
                        case 151:
                            int _arg0116 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result71 = getMuteStateV4(_arg0116);
                            reply.writeNoException();
                            reply.writeBoolean(_result71);
                            return true;
                        case 152:
                            boolean _arg0117 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setMuteState(_arg0117);
                            reply.writeNoException();
                            return true;
                        case 153:
                            boolean _arg0118 = data.readBoolean();
                            int _arg166 = data.readInt();
                            data.enforceNoDataAvail();
                            setMuteStateV4(_arg0118, _arg166);
                            reply.writeNoException();
                            return true;
                        case 154:
                            int _arg0119 = data.readInt();
                            data.enforceNoDataAvail();
                            hookMultiWindowMuteAethenV4(_arg0119);
                            reply.writeNoException();
                            return true;
                        case 155:
                            String _result72 = getMultiDisplayAreaTopPackage();
                            reply.writeNoException();
                            reply.writeString(_result72);
                            return true;
                        case 156:
                            int _arg0120 = data.readInt();
                            int _arg167 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result73 = getMultiDisplayAreaTopPackageV4(_arg0120, _arg167);
                            reply.writeNoException();
                            reply.writeString(_result73);
                            return true;
                        case 157:
                            String _arg0121 = data.readString();
                            data.enforceNoDataAvail();
                            moveToBottomForMultiWindowV3(_arg0121);
                            reply.writeNoException();
                            return true;
                        case 158:
                            int _arg0122 = data.readInt();
                            boolean _arg168 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setMultiWindowAcquireFocus(_arg0122, _arg168);
                            reply.writeNoException();
                            return true;
                        case 159:
                            int _arg0123 = data.readInt();
                            data.enforceNoDataAvail();
                            ActivityManager.RunningTaskInfo _result74 = getTopTask(_arg0123);
                            reply.writeNoException();
                            reply.writeTypedObject(_result74, 1);
                            return true;
                        case 160:
                            int _arg0124 = data.readInt();
                            int _arg169 = data.readInt();
                            data.enforceNoDataAvail();
                            ActivityManager.RunningTaskInfo _result75 = getMultiWinTopTask(_arg0124, _arg169);
                            reply.writeNoException();
                            reply.writeTypedObject(_result75, 1);
                            return true;
                        case 161:
                            int _arg0125 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result76 = getTaskOrientation(_arg0125);
                            reply.writeNoException();
                            reply.writeInt(_result76);
                            return true;
                        case 162:
                            int _arg0126 = data.readInt();
                            boolean _arg170 = data.readBoolean();
                            data.enforceNoDataAvail();
                            minimizeMultiWinToEdge(_arg0126, _arg170);
                            reply.writeNoException();
                            return true;
                        case 163:
                            int _arg0127 = data.readInt();
                            data.enforceNoDataAvail();
                            Rect _result77 = getMultiWindowContentRegion(_arg0127);
                            reply.writeNoException();
                            reply.writeTypedObject(_result77, 1);
                            return true;
                        case 164:
                            int _arg0128 = data.readInt();
                            data.enforceNoDataAvail();
                            updateZBoostTaskIdWhenToSplit(_arg0128);
                            reply.writeNoException();
                            return true;
                        case 165:
                            int _arg0129 = data.readInt();
                            data.enforceNoDataAvail();
                            hookExitSplitScreenToMultiWindow(_arg0129);
                            reply.writeNoException();
                            return true;
                        case 166:
                            int _arg0130 = data.readInt();
                            data.enforceNoDataAvail();
                            Rect _result78 = hookGetMultiWindowDefaultRect(_arg0130);
                            reply.writeNoException();
                            reply.writeTypedObject(_result78, 1);
                            return true;
                        case 167:
                            int _arg0131 = data.readInt();
                            data.enforceNoDataAvail();
                            Rect _result79 = hookGetMultiWindowDefaultRectByTask(_arg0131);
                            reply.writeNoException();
                            reply.writeTypedObject(_result79, 1);
                            return true;
                        case 168:
                            Rect _arg0132 = (Rect) data.readTypedObject(Rect.CREATOR);
                            data.enforceNoDataAvail();
                            hookSetMultiWindowDefaultRectResult(_arg0132);
                            reply.writeNoException();
                            return true;
                        case 169:
                            int _arg0133 = data.readInt();
                            WindowContainerToken _arg171 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            Rect _arg237 = (Rect) data.readTypedObject(Rect.CREATOR);
                            IWindowContainerTransactionCallbackSync _arg324 = IWindowContainerTransactionCallbackSync.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            hookStartMultiWindowFromSplitScreen(_arg0133, _arg171, _arg237, _arg324);
                            reply.writeNoException();
                            return true;
                        case 170:
                            int _arg0134 = data.readInt();
                            WindowContainerToken _arg172 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            Rect _arg238 = (Rect) data.readTypedObject(Rect.CREATOR);
                            IWindowContainerTransactionCallbackSync _arg325 = IWindowContainerTransactionCallbackSync.Stub.asInterface(data.readStrongBinder());
                            int _arg420 = data.readInt();
                            data.enforceNoDataAvail();
                            hookStartMultiWindowFromSplitScreenV4(_arg0134, _arg172, _arg238, _arg325, _arg420);
                            reply.writeNoException();
                            return true;
                        case 171:
                            SurfaceControl _arg0135 = (SurfaceControl) data.readTypedObject(SurfaceControl.CREATOR);
                            float[] _arg173 = data.createFloatArray();
                            float[] _arg239 = data.createFloatArray();
                            int _arg326 = data.readInt();
                            data.enforceNoDataAvail();
                            setFinishFixedRotationWithTransaction(_arg0135, _arg173, _arg239, _arg326);
                            reply.writeNoException();
                            return true;
                        case 172:
                            clearFinishFixedRotationWithTransaction();
                            reply.writeNoException();
                            return true;
                        case 173:
                            int _arg0136 = data.readInt();
                            Rect _arg174 = (Rect) data.readTypedObject(Rect.CREATOR);
                            IWindowContainerTransactionCallback _arg240 = IWindowContainerTransactionCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            hookStartMultiWindow(_arg0136, _arg174, _arg240);
                            reply.writeNoException();
                            return true;
                        case 174:
                            int _arg0137 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result80 = taskInMultiWindowById(_arg0137);
                            reply.writeNoException();
                            reply.writeBoolean(_result80);
                            return true;
                        case 175:
                            SurfaceControl _arg0138 = (SurfaceControl) data.readTypedObject(SurfaceControl.CREATOR);
                            data.enforceNoDataAvail();
                            addAnimationIconLayer(_arg0138);
                            reply.writeNoException();
                            return true;
                        case 176:
                            SurfaceControl _arg0139 = (SurfaceControl) data.readTypedObject(SurfaceControl.CREATOR);
                            data.enforceNoDataAvail();
                            removeAnimationIconLayer(_arg0139);
                            reply.writeNoException();
                            return true;
                        case 177:
                            int _arg0140 = data.readInt();
                            data.enforceNoDataAvail();
                            IBinder _result81 = getAppTaskByTaskId(_arg0140);
                            reply.writeNoException();
                            reply.writeStrongBinder(_result81);
                            return true;
                        case 178:
                            int _arg0141 = data.readInt();
                            data.enforceNoDataAvail();
                            boostSceneStart(_arg0141);
                            reply.writeNoException();
                            return true;
                        case 179:
                            int _arg0142 = data.readInt();
                            long _arg175 = data.readLong();
                            data.enforceNoDataAvail();
                            boostSceneStartDuration(_arg0142, _arg175);
                            reply.writeNoException();
                            return true;
                        case 180:
                            int _arg0143 = data.readInt();
                            data.enforceNoDataAvail();
                            boostSceneEnd(_arg0143);
                            reply.writeNoException();
                            return true;
                        case 181:
                            int _arg0144 = data.readInt();
                            long _arg176 = data.readLong();
                            data.enforceNoDataAvail();
                            boostSceneEndDelay(_arg0144, _arg176);
                            reply.writeNoException();
                            return true;
                        case 182:
                            boostStartForLauncher();
                            reply.writeNoException();
                            return true;
                        case 183:
                            boostEndForLauncher();
                            reply.writeNoException();
                            return true;
                        case 184:
                            int _arg0145 = data.readInt();
                            data.enforceNoDataAvail();
                            boostStartInLauncher(_arg0145);
                            reply.writeNoException();
                            return true;
                        case 185:
                            int _arg0146 = data.readInt();
                            data.enforceNoDataAvail();
                            boostEndInLauncher(_arg0146);
                            reply.writeNoException();
                            return true;
                        case 186:
                            boolean _arg0147 = data.readBoolean();
                            boolean _arg177 = data.readBoolean();
                            List<String> _arg241 = data.createStringArrayList();
                            data.enforceNoDataAvail();
                            handleDrawThreadsBoost(_arg0147, _arg177, _arg241);
                            reply.writeNoException();
                            return true;
                        case 187:
                            resetBoostThreads();
                            reply.writeNoException();
                            return true;
                        case 188:
                            String _arg0148 = data.readString();
                            data.enforceNoDataAvail();
                            Bundle _result82 = getMultiWindowParams(_arg0148);
                            reply.writeNoException();
                            reply.writeTypedObject(_result82, 1);
                            return true;
                        case 189:
                            Bundle _arg0149 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            setMultiWindowParams(_arg0149);
                            reply.writeNoException();
                            return true;
                        case 190:
                            boolean _result83 = isPinnedMode();
                            reply.writeNoException();
                            reply.writeBoolean(_result83);
                            return true;
                        case 191:
                            SurfaceControl _result84 = getDefaultRootLeash();
                            reply.writeNoException();
                            reply.writeTypedObject(_result84, 1);
                            return true;
                        case 192:
                            int _arg0150 = data.readInt();
                            int _arg178 = data.readInt();
                            boolean _arg242 = data.readBoolean();
                            data.enforceNoDataAvail();
                            reparentActivity(_arg0150, _arg178, _arg242);
                            reply.writeNoException();
                            return true;
                        case 193:
                            boolean _result85 = isSplitScreen();
                            reply.writeNoException();
                            reply.writeBoolean(_result85);
                            return true;
                        case 194:
                            List<String> _arg0151 = data.createStringArrayList();
                            data.enforceNoDataAvail();
                            setConnectBlackListToSystem(_arg0151);
                            reply.writeNoException();
                            return true;
                        case 195:
                            int _arg0152 = data.readInt();
                            long _arg179 = data.readLong();
                            data.enforceNoDataAvail();
                            hookReserveMultiWindowNumber(_arg0152, _arg179);
                            reply.writeNoException();
                            return true;
                        case 196:
                            hookShowBlurLayerFinish();
                            reply.writeNoException();
                            return true;
                        case 197:
                            List<String> _arg0153 = data.createStringArrayList();
                            data.enforceNoDataAvail();
                            setSruWhiteListToSystem(_arg0153);
                            reply.writeNoException();
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements IActivityTaskManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IActivityTaskManager.DESCRIPTOR;
            }

            @Override // android.app.IActivityTaskManager
            public int startActivity(IApplicationThread caller, String callingPackage, String callingFeatureId, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int flags, ProfilerInfo profilerInfo, Bundle options) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(caller);
                    try {
                        _data.writeString(callingPackage);
                        try {
                            _data.writeString(callingFeatureId);
                        } catch (Throwable th) {
                            th = th;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                }
                try {
                    _data.writeTypedObject(intent, 0);
                    try {
                        _data.writeString(resolvedType);
                        try {
                            _data.writeStrongBinder(resultTo);
                            try {
                                _data.writeString(resultWho);
                            } catch (Throwable th4) {
                                th = th4;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(requestCode);
                        try {
                            _data.writeInt(flags);
                            try {
                                _data.writeTypedObject(profilerInfo, 0);
                                try {
                                    _data.writeTypedObject(options, 0);
                                } catch (Throwable th7) {
                                    th = th7;
                                }
                            } catch (Throwable th8) {
                                th = th8;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th9) {
                            th = th9;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th10) {
                        th = th10;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th11) {
                    th = th11;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
                try {
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th12) {
                    th = th12;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            @Override // android.app.IActivityTaskManager
            public int startActivities(IApplicationThread caller, String callingPackage, String callingFeatureId, Intent[] intents, String[] resolvedTypes, IBinder resultTo, Bundle options, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(caller);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    _data.writeTypedArray(intents, 0);
                    _data.writeStringArray(resolvedTypes);
                    _data.writeStrongBinder(resultTo);
                    _data.writeTypedObject(options, 0);
                    _data.writeInt(userId);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public int startActivityAsUser(IApplicationThread caller, String callingPackage, String callingFeatureId, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int flags, ProfilerInfo profilerInfo, Bundle options, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(caller);
                    _data.writeString(callingPackage);
                    try {
                        _data.writeString(callingFeatureId);
                        try {
                            _data.writeTypedObject(intent, 0);
                            try {
                                _data.writeString(resolvedType);
                            } catch (Throwable th) {
                                th = th;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeStrongBinder(resultTo);
                        try {
                            _data.writeString(resultWho);
                            try {
                                _data.writeInt(requestCode);
                                try {
                                    _data.writeInt(flags);
                                } catch (Throwable th4) {
                                    th = th4;
                                    _reply.recycle();
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th5) {
                                th = th5;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th6) {
                            th = th6;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                        try {
                            _data.writeTypedObject(profilerInfo, 0);
                            try {
                                _data.writeTypedObject(options, 0);
                                try {
                                    _data.writeInt(userId);
                                    try {
                                        this.mRemote.transact(3, _data, _reply, 0);
                                        _reply.readException();
                                        int _result = _reply.readInt();
                                        _reply.recycle();
                                        _data.recycle();
                                        return _result;
                                    } catch (Throwable th7) {
                                        th = th7;
                                        _reply.recycle();
                                        _data.recycle();
                                        throw th;
                                    }
                                } catch (Throwable th8) {
                                    th = th8;
                                }
                            } catch (Throwable th9) {
                                th = th9;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th10) {
                            th = th10;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th11) {
                        th = th11;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th12) {
                    th = th12;
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean startNextMatchingActivity(IBinder callingActivity, Intent intent, Bundle options) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongBinder(callingActivity);
                    _data.writeTypedObject(intent, 0);
                    _data.writeTypedObject(options, 0);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean startDreamActivity(Intent intent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeTypedObject(intent, 0);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public int startActivityIntentSender(IApplicationThread caller, IIntentSender target, IBinder whitelistToken, Intent fillInIntent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int flagsMask, int flagsValues, Bundle options) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(caller);
                    try {
                        _data.writeStrongInterface(target);
                        try {
                            _data.writeStrongBinder(whitelistToken);
                        } catch (Throwable th) {
                            th = th;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                }
                try {
                    _data.writeTypedObject(fillInIntent, 0);
                    try {
                        _data.writeString(resolvedType);
                        try {
                            _data.writeStrongBinder(resultTo);
                            try {
                                _data.writeString(resultWho);
                            } catch (Throwable th4) {
                                th = th4;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(requestCode);
                        try {
                            _data.writeInt(flagsMask);
                            try {
                                _data.writeInt(flagsValues);
                                try {
                                    _data.writeTypedObject(options, 0);
                                } catch (Throwable th7) {
                                    th = th7;
                                }
                            } catch (Throwable th8) {
                                th = th8;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th9) {
                            th = th9;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th10) {
                        th = th10;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th11) {
                    th = th11;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
                try {
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th12) {
                    th = th12;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            @Override // android.app.IActivityTaskManager
            public WaitResult startActivityAndWait(IApplicationThread caller, String callingPackage, String callingFeatureId, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int flags, ProfilerInfo profilerInfo, Bundle options, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(caller);
                    _data.writeString(callingPackage);
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    _data.writeString(callingFeatureId);
                    try {
                        _data.writeTypedObject(intent, 0);
                        try {
                            _data.writeString(resolvedType);
                            try {
                                _data.writeStrongBinder(resultTo);
                            } catch (Throwable th2) {
                                th = th2;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeString(resultWho);
                        try {
                            _data.writeInt(requestCode);
                            try {
                                _data.writeInt(flags);
                                try {
                                    _data.writeTypedObject(profilerInfo, 0);
                                } catch (Throwable th5) {
                                    th = th5;
                                    _reply.recycle();
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th6) {
                                th = th6;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th7) {
                            th = th7;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                        try {
                            _data.writeTypedObject(options, 0);
                            try {
                                _data.writeInt(userId);
                                try {
                                    this.mRemote.transact(7, _data, _reply, 0);
                                    _reply.readException();
                                    WaitResult _result = (WaitResult) _reply.readTypedObject(WaitResult.CREATOR);
                                    _reply.recycle();
                                    _data.recycle();
                                    return _result;
                                } catch (Throwable th8) {
                                    th = th8;
                                    _reply.recycle();
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th9) {
                                th = th9;
                            }
                        } catch (Throwable th10) {
                            th = th10;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th11) {
                        th = th11;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th12) {
                    th = th12;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            @Override // android.app.IActivityTaskManager
            public int startActivityWithConfig(IApplicationThread caller, String callingPackage, String callingFeatureId, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int startFlags, Configuration newConfig, Bundle options, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(caller);
                    _data.writeString(callingPackage);
                    try {
                        _data.writeString(callingFeatureId);
                        try {
                            _data.writeTypedObject(intent, 0);
                            try {
                                _data.writeString(resolvedType);
                            } catch (Throwable th) {
                                th = th;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeStrongBinder(resultTo);
                        try {
                            _data.writeString(resultWho);
                            try {
                                _data.writeInt(requestCode);
                                try {
                                    _data.writeInt(startFlags);
                                } catch (Throwable th4) {
                                    th = th4;
                                    _reply.recycle();
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th5) {
                                th = th5;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th6) {
                            th = th6;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                        try {
                            _data.writeTypedObject(newConfig, 0);
                            try {
                                _data.writeTypedObject(options, 0);
                                try {
                                    _data.writeInt(userId);
                                    try {
                                        this.mRemote.transact(8, _data, _reply, 0);
                                        _reply.readException();
                                        int _result = _reply.readInt();
                                        _reply.recycle();
                                        _data.recycle();
                                        return _result;
                                    } catch (Throwable th7) {
                                        th = th7;
                                        _reply.recycle();
                                        _data.recycle();
                                        throw th;
                                    }
                                } catch (Throwable th8) {
                                    th = th8;
                                }
                            } catch (Throwable th9) {
                                th = th9;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th10) {
                            th = th10;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th11) {
                        th = th11;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th12) {
                    th = th12;
                }
            }

            @Override // android.app.IActivityTaskManager
            public int startVoiceActivity(String callingPackage, String callingFeatureId, int callingPid, int callingUid, Intent intent, String resolvedType, IVoiceInteractionSession session, IVoiceInteractor interactor, int flags, ProfilerInfo profilerInfo, Bundle options, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    try {
                        _data.writeInt(callingPid);
                        try {
                            _data.writeInt(callingUid);
                            try {
                                _data.writeTypedObject(intent, 0);
                            } catch (Throwable th) {
                                th = th;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeString(resolvedType);
                        try {
                            _data.writeStrongInterface(session);
                            try {
                                _data.writeStrongInterface(interactor);
                                try {
                                    _data.writeInt(flags);
                                } catch (Throwable th4) {
                                    th = th4;
                                    _reply.recycle();
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th5) {
                                th = th5;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th6) {
                            th = th6;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                        try {
                            _data.writeTypedObject(profilerInfo, 0);
                            try {
                                _data.writeTypedObject(options, 0);
                                try {
                                    _data.writeInt(userId);
                                    try {
                                        this.mRemote.transact(9, _data, _reply, 0);
                                        _reply.readException();
                                        int _result = _reply.readInt();
                                        _reply.recycle();
                                        _data.recycle();
                                        return _result;
                                    } catch (Throwable th7) {
                                        th = th7;
                                        _reply.recycle();
                                        _data.recycle();
                                        throw th;
                                    }
                                } catch (Throwable th8) {
                                    th = th8;
                                }
                            } catch (Throwable th9) {
                                th = th9;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th10) {
                            th = th10;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th11) {
                        th = th11;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th12) {
                    th = th12;
                }
            }

            @Override // android.app.IActivityTaskManager
            public String getVoiceInteractorPackageName(IBinder callingVoiceInteractor) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongBinder(callingVoiceInteractor);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public int startAssistantActivity(String callingPackage, String callingFeatureId, int callingPid, int callingUid, Intent intent, String resolvedType, Bundle options, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    _data.writeInt(callingPid);
                    _data.writeInt(callingUid);
                    _data.writeTypedObject(intent, 0);
                    _data.writeString(resolvedType);
                    _data.writeTypedObject(options, 0);
                    _data.writeInt(userId);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public int startActivityFromGameSession(IApplicationThread caller, String callingPackage, String callingFeatureId, int callingPid, int callingUid, Intent intent, int taskId, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(caller);
                    _data.writeString(callingPackage);
                    _data.writeString(callingFeatureId);
                    _data.writeInt(callingPid);
                    _data.writeInt(callingUid);
                    _data.writeTypedObject(intent, 0);
                    _data.writeInt(taskId);
                    _data.writeInt(userId);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void startRecentsActivity(Intent intent, long eventTime, IRecentsAnimationRunner recentsAnimationRunner) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeTypedObject(intent, 0);
                    _data.writeLong(eventTime);
                    _data.writeStrongInterface(recentsAnimationRunner);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public int startActivityFromRecents(int taskId, Bundle options) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    _data.writeTypedObject(options, 0);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public int startActivityAsCaller(IApplicationThread caller, String callingPackage, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int flags, ProfilerInfo profilerInfo, Bundle options, boolean ignoreTargetSecurity, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(caller);
                    _data.writeString(callingPackage);
                    try {
                        _data.writeTypedObject(intent, 0);
                        try {
                            _data.writeString(resolvedType);
                            try {
                                _data.writeStrongBinder(resultTo);
                            } catch (Throwable th) {
                                th = th;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeString(resultWho);
                        try {
                            _data.writeInt(requestCode);
                            try {
                                _data.writeInt(flags);
                                try {
                                    _data.writeTypedObject(profilerInfo, 0);
                                } catch (Throwable th4) {
                                    th = th4;
                                    _reply.recycle();
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th5) {
                                th = th5;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th6) {
                            th = th6;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                        try {
                            _data.writeTypedObject(options, 0);
                            try {
                                _data.writeBoolean(ignoreTargetSecurity);
                                try {
                                    _data.writeInt(userId);
                                    try {
                                        this.mRemote.transact(15, _data, _reply, 0);
                                        _reply.readException();
                                        int _result = _reply.readInt();
                                        _reply.recycle();
                                        _data.recycle();
                                        return _result;
                                    } catch (Throwable th7) {
                                        th = th7;
                                        _reply.recycle();
                                        _data.recycle();
                                        throw th;
                                    }
                                } catch (Throwable th8) {
                                    th = th8;
                                }
                            } catch (Throwable th9) {
                                th = th9;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th10) {
                            th = th10;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th11) {
                        th = th11;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th12) {
                    th = th12;
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean isActivityStartAllowedOnDisplay(int displayId, Intent intent, String resolvedType, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeTypedObject(intent, 0);
                    _data.writeString(resolvedType);
                    _data.writeInt(userId);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void unhandledBack() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public IActivityClientController getActivityClientController() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    IActivityClientController _result = IActivityClientController.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public int getFrontActivityScreenCompatMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setFrontActivityScreenCompatMode(int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(mode);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setFocusedTask(int taskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean removeTask(int taskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void removeAllVisibleRecentTasks() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public List<ActivityManager.RunningTaskInfo> getTasks(int maxNum, boolean filterOnlyVisibleRecents, boolean keepIntentExtra) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(maxNum);
                    _data.writeBoolean(filterOnlyVisibleRecents);
                    _data.writeBoolean(keepIntentExtra);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    List<ActivityManager.RunningTaskInfo> _result = _reply.createTypedArrayList(ActivityManager.RunningTaskInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void moveTaskToFront(IApplicationThread app, String callingPackage, int task, int flags, Bundle options) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(app);
                    _data.writeString(callingPackage);
                    _data.writeInt(task);
                    _data.writeInt(flags);
                    _data.writeTypedObject(options, 0);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public ParceledListSlice<ActivityManager.RecentTaskInfo> getRecentTasks(int maxNum, int flags, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(maxNum);
                    _data.writeInt(flags);
                    _data.writeInt(userId);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    ParceledListSlice<ActivityManager.RecentTaskInfo> _result = (ParceledListSlice) _reply.readTypedObject(ParceledListSlice.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean isTopActivityImmersive() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public ActivityManager.TaskDescription getTaskDescription(int taskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    ActivityManager.TaskDescription _result = (ActivityManager.TaskDescription) _reply.readTypedObject(ActivityManager.TaskDescription.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void reportAssistContextExtras(IBinder assistToken, Bundle extras, AssistStructure structure, AssistContent content, Uri referrer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongBinder(assistToken);
                    _data.writeTypedObject(extras, 0);
                    _data.writeTypedObject(structure, 0);
                    _data.writeTypedObject(content, 0);
                    _data.writeTypedObject(referrer, 0);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setFocusedRootTask(int taskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public ActivityTaskManager.RootTaskInfo getFocusedRootTaskInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                    ActivityTaskManager.RootTaskInfo _result = (ActivityTaskManager.RootTaskInfo) _reply.readTypedObject(ActivityTaskManager.RootTaskInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public Rect getTaskBounds(int taskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    Rect _result = (Rect) _reply.readTypedObject(Rect.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void cancelRecentsAnimation(boolean restoreHomeRootTaskPosition) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeBoolean(restoreHomeRootTaskPosition);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void updateLockTaskPackages(int userId, String[] packages) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeStringArray(packages);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean isInLockTaskMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public int getLockTaskModeState() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public List<IBinder> getAppTasks(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                    List<IBinder> _result = _reply.createBinderArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void startSystemLockTaskMode(int taskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void stopSystemLockTaskMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void finishVoiceTask(IVoiceInteractionSession session) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(session);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public int addAppTask(IBinder activityToken, Intent intent, ActivityManager.TaskDescription description, Bitmap thumbnail) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongBinder(activityToken);
                    _data.writeTypedObject(intent, 0);
                    _data.writeTypedObject(description, 0);
                    _data.writeTypedObject(thumbnail, 0);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public Point getAppTaskThumbnailSize() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                    Point _result = (Point) _reply.readTypedObject(Point.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void releaseSomeActivities(IApplicationThread app) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(app);
                    this.mRemote.transact(43, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public Bitmap getTaskDescriptionIcon(String filename, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeString(filename);
                    _data.writeInt(userId);
                    this.mRemote.transact(44, _data, _reply, 0);
                    _reply.readException();
                    Bitmap _result = (Bitmap) _reply.readTypedObject(Bitmap.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void registerTaskStackListener(ITaskStackListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(45, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void unregisterTaskStackListener(ITaskStackListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(46, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setTaskResizeable(int taskId, int resizeableMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    _data.writeInt(resizeableMode);
                    this.mRemote.transact(47, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean resizeTask(int taskId, Rect bounds, int resizeMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    _data.writeTypedObject(bounds, 0);
                    _data.writeInt(resizeMode);
                    this.mRemote.transact(48, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void moveRootTaskToDisplay(int taskId, int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    _data.writeInt(displayId);
                    this.mRemote.transact(49, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void moveTaskToRootTask(int taskId, int rootTaskId, boolean toTop) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    _data.writeInt(rootTaskId);
                    _data.writeBoolean(toTop);
                    this.mRemote.transact(50, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void removeRootTasksInWindowingModes(int[] windowingModes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeIntArray(windowingModes);
                    this.mRemote.transact(51, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void removeRootTasksWithActivityTypes(int[] activityTypes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeIntArray(activityTypes);
                    this.mRemote.transact(52, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public List<ActivityTaskManager.RootTaskInfo> getAllRootTaskInfos() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                    List<ActivityTaskManager.RootTaskInfo> _result = _reply.createTypedArrayList(ActivityTaskManager.RootTaskInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public ActivityTaskManager.RootTaskInfo getRootTaskInfo(int windowingMode, int activityType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(windowingMode);
                    _data.writeInt(activityType);
                    this.mRemote.transact(54, _data, _reply, 0);
                    _reply.readException();
                    ActivityTaskManager.RootTaskInfo _result = (ActivityTaskManager.RootTaskInfo) _reply.readTypedObject(ActivityTaskManager.RootTaskInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public List<ActivityTaskManager.RootTaskInfo> getAllRootTaskInfosOnDisplay(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(55, _data, _reply, 0);
                    _reply.readException();
                    List<ActivityTaskManager.RootTaskInfo> _result = _reply.createTypedArrayList(ActivityTaskManager.RootTaskInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public ActivityTaskManager.RootTaskInfo getRootTaskInfoOnDisplay(int windowingMode, int activityType, int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(windowingMode);
                    _data.writeInt(activityType);
                    _data.writeInt(displayId);
                    this.mRemote.transact(56, _data, _reply, 0);
                    _reply.readException();
                    ActivityTaskManager.RootTaskInfo _result = (ActivityTaskManager.RootTaskInfo) _reply.readTypedObject(ActivityTaskManager.RootTaskInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setLockScreenShown(boolean showingKeyguard, boolean showingAod) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeBoolean(showingKeyguard);
                    _data.writeBoolean(showingAod);
                    this.mRemote.transact(57, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public Bundle getAssistContextExtras(int requestType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(requestType);
                    this.mRemote.transact(58, _data, _reply, 0);
                    _reply.readException();
                    Bundle _result = (Bundle) _reply.readTypedObject(Bundle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean requestAssistContextExtras(int requestType, IAssistDataReceiver receiver, Bundle receiverExtras, IBinder activityToken, boolean focused, boolean newSessionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(requestType);
                    _data.writeStrongInterface(receiver);
                    _data.writeTypedObject(receiverExtras, 0);
                    _data.writeStrongBinder(activityToken);
                    _data.writeBoolean(focused);
                    _data.writeBoolean(newSessionId);
                    this.mRemote.transact(59, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean requestAutofillData(IAssistDataReceiver receiver, Bundle receiverExtras, IBinder activityToken, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(receiver);
                    _data.writeTypedObject(receiverExtras, 0);
                    _data.writeStrongBinder(activityToken);
                    _data.writeInt(flags);
                    this.mRemote.transact(60, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean isAssistDataAllowedOnCurrentActivity() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(61, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean requestAssistDataForTask(IAssistDataReceiver receiver, int taskId, String callingPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(receiver);
                    _data.writeInt(taskId);
                    _data.writeString(callingPackageName);
                    this.mRemote.transact(62, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void keyguardGoingAway(int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(flags);
                    this.mRemote.transact(63, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void suppressResizeConfigChanges(boolean suppress) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeBoolean(suppress);
                    this.mRemote.transact(64, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public IWindowOrganizerController getWindowOrganizerController() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(65, _data, _reply, 0);
                    _reply.readException();
                    IWindowOrganizerController _result = IWindowOrganizerController.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setSplitScreenResizing(boolean resizing) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeBoolean(resizing);
                    this.mRemote.transact(66, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean supportsLocalVoiceInteraction() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(67, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public ConfigurationInfo getDeviceConfigurationInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(68, _data, _reply, 0);
                    _reply.readException();
                    ConfigurationInfo _result = (ConfigurationInfo) _reply.readTypedObject(ConfigurationInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void cancelTaskWindowTransition(int taskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    this.mRemote.transact(69, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public TaskSnapshot getTaskSnapshot(int taskId, boolean isLowResolution) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    _data.writeBoolean(isLowResolution);
                    this.mRemote.transact(70, _data, _reply, 0);
                    _reply.readException();
                    TaskSnapshot _result = (TaskSnapshot) _reply.readTypedObject(TaskSnapshot.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public TaskSnapshot takeTaskSnapshot(int taskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    this.mRemote.transact(71, _data, _reply, 0);
                    _reply.readException();
                    TaskSnapshot _result = (TaskSnapshot) _reply.readTypedObject(TaskSnapshot.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public int getLastResumedActivityUserId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(72, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean updateConfiguration(Configuration values) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeTypedObject(values, 0);
                    this.mRemote.transact(73, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void updateLockTaskFeatures(int userId, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeInt(flags);
                    this.mRemote.transact(74, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void registerRemoteAnimationForNextActivityStart(String packageName, RemoteAnimationAdapter adapter, IBinder launchCookie) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeTypedObject(adapter, 0);
                    _data.writeStrongBinder(launchCookie);
                    this.mRemote.transact(75, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void registerRemoteAnimationsForDisplay(int displayId, RemoteAnimationDefinition definition) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeTypedObject(definition, 0);
                    this.mRemote.transact(76, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void alwaysShowUnsupportedCompileSdkWarning(ComponentName activity) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeTypedObject(activity, 0);
                    this.mRemote.transact(77, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setVrThread(int tid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(tid);
                    this.mRemote.transact(78, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setPersistentVrThread(int tid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(tid);
                    this.mRemote.transact(79, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void stopAppSwitches() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(80, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void resumeAppSwitches() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(81, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setActivityController(IActivityController watcher, boolean imAMonkey) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(watcher);
                    _data.writeBoolean(imAMonkey);
                    this.mRemote.transact(82, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setVoiceKeepAwake(IVoiceInteractionSession session, boolean keepAwake) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(session);
                    _data.writeBoolean(keepAwake);
                    this.mRemote.transact(83, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public int getPackageScreenCompatMode(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(84, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setPackageScreenCompatMode(String packageName, int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(mode);
                    this.mRemote.transact(85, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean getPackageAskScreenCompat(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(86, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setPackageAskScreenCompat(String packageName, boolean ask) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeBoolean(ask);
                    this.mRemote.transact(87, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void clearLaunchParamsForPackages(List<String> packageNames) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStringList(packageNames);
                    this.mRemote.transact(88, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void onSplashScreenViewCopyFinished(int taskId, SplashScreenView.SplashScreenViewParcelable material) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    _data.writeTypedObject(material, 0);
                    this.mRemote.transact(89, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void onPictureInPictureStateChanged(PictureInPictureUiState pipState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeTypedObject(pipState, 0);
                    this.mRemote.transact(90, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public String getFocusedWinPkgName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(91, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean isSecureWindow() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(92, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean isIMEShowing() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(93, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean inMultiWindowMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(94, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void notifyKeyguardGoingAwayQuickly(boolean goingAwayQuickly) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeBoolean(goingAwayQuickly);
                    this.mRemote.transact(95, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean isKeyguardLocking() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(96, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void notifyAuthenticateSucceed(boolean isSucceed) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeBoolean(isSucceed);
                    this.mRemote.transact(97, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void notAllowKeyguardGoingAwayQuickly(boolean notAllow) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeBoolean(notAllow);
                    this.mRemote.transact(98, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void detachNavigationBarFromApp(IBinder transition) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongBinder(transition);
                    this.mRemote.transact(99, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setRunningRemoteTransitionDelegate(IApplicationThread caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(caller);
                    this.mRemote.transact(100, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public BackNavigationInfo startBackNavigation(boolean requestAnimation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeBoolean(requestAnimation);
                    this.mRemote.transact(101, _data, _reply, 0);
                    _reply.readException();
                    BackNavigationInfo _result = (BackNavigationInfo) _reply.readTypedObject(BackNavigationInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public ComponentName getTopActivityComponent() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(102, _data, _reply, 0);
                    _reply.readException();
                    ComponentName _result = (ComponentName) _reply.readTypedObject(ComponentName.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setThreadScheduler(int tid, int policy, int priority) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(tid);
                    _data.writeInt(policy);
                    _data.writeInt(priority);
                    this.mRemote.transact(103, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setTaskSplitManagerProxy(ITaskSplitManager taskSplitManager) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(taskSplitManager);
                    this.mRemote.transact(104, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void registerTaskSplitListener(ITaskSplitManagerListener taskSplitListener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(taskSplitListener);
                    this.mRemote.transact(105, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void unregisterTaskSplitListener(ITaskSplitManagerListener taskSplitListener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(taskSplitListener);
                    this.mRemote.transact(106, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void notifyTaskAnimationResult(int taskId, ITaskAnimation taskAnimation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    _data.writeStrongInterface(taskAnimation);
                    this.mRemote.transact(107, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public ITaskAnimation createTaskAnimation(IBinder token, int taskId, MultiTaskRemoteAnimationAdapter callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(taskId);
                    _data.writeTypedObject(callback, 0);
                    this.mRemote.transact(108, _data, _reply, 0);
                    _reply.readException();
                    ITaskAnimation _result = ITaskAnimation.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookReparentToDefaultDisplay(int multiWindowMode, int multiWindowId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    this.mRemote.transact(109, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookMultiWindowToMaxV3(IWindow window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    this.mRemote.transact(110, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookMultiWindowToMinV3(IWindow window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    this.mRemote.transact(111, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookMultiWindowToCloseV3(IWindow window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    this.mRemote.transact(112, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookMultiWindowToSmallV3(IWindow window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    this.mRemote.transact(113, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookMultiWindowToLargeV3(IWindow window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    this.mRemote.transact(114, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookMultiWindowToCloseV4(int multiWindowMode, int multiWindowId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    this.mRemote.transact(115, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookMultiWindowToSmallV4(int multiWindowMode, int multiWindowId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    this.mRemote.transact(116, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookMultiWindowToSplit(int multiWindowMode, int multiWindowId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    this.mRemote.transact(117, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookMultiWindowMute(IWindow window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    this.mRemote.transact(118, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookMultiWindowLocation(IWindow window, int x, int y, int touchX, int touchY) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    _data.writeInt(x);
                    _data.writeInt(y);
                    _data.writeInt(touchX);
                    _data.writeInt(touchY);
                    this.mRemote.transact(119, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookFinishMovingLocationV3(IWindow window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    this.mRemote.transact(120, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookMultiWindowFlingV3(IWindow window, MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    _data.writeTypedObject(e1, 0);
                    _data.writeTypedObject(e2, 0);
                    _data.writeFloat(velocityX);
                    _data.writeFloat(velocityY);
                    this.mRemote.transact(121, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookStartActivityResult(int result, Rect location) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(result);
                    _data.writeTypedObject(location, 0);
                    this.mRemote.transact(122, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookMultiWindowVisible() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(123, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookMultiWindowInvisible() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(124, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setTranMultiWindowModeV3(int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(mode);
                    this.mRemote.transact(125, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setStartInMultiWindow(String pkgName, int type, int direction, int startType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeInt(type);
                    _data.writeInt(direction);
                    _data.writeInt(startType);
                    this.mRemote.transact(126, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public String getMultiWindowVersion() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(127, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean hasMultiWindow() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(128, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean activityInMultiWindow(String pkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeString(pkgName);
                    this.mRemote.transact(129, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public String getMulitWindowTopPackage() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(130, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void startCurrentAppInMultiWindow(boolean anim, int startType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeBoolean(anim);
                    _data.writeInt(startType);
                    this.mRemote.transact(131, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean isSupportMultiWindow() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(132, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean isSplitScreenSupportMultiWindowV4(int type, ActivityManager.RunningTaskInfo info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeTypedObject(info, 0);
                    this.mRemote.transact(133, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public List<String> getMultiWindowBlackList() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(134, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setMultiWindowBlackListToSystem(List<String> list) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStringList(list);
                    this.mRemote.transact(135, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setMultiWindowWhiteListToSystem(List<String> list) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStringList(list);
                    this.mRemote.transact(136, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setMultiWindowConfigToSystem(String key, List<String> list) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeString(key);
                    _data.writeStringList(list);
                    this.mRemote.transact(137, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public Rect getMultiWindowDefaultRect() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(138, _data, _reply, 0);
                    _reply.readException();
                    Rect _result = (Rect) _reply.readTypedObject(Rect.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setFinishFixedRotationEnterMultiWindowTransactionV3(SurfaceControl leash, int x, int y, int rotation, float scale) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeTypedObject(leash, 0);
                    _data.writeInt(x);
                    _data.writeInt(y);
                    _data.writeInt(rotation);
                    _data.writeFloat(scale);
                    this.mRemote.transact(139, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public SurfaceControl getWeltWindowLeash(int width, int height, int x, int y, boolean hidden) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(width);
                    _data.writeInt(height);
                    _data.writeInt(x);
                    _data.writeInt(y);
                    _data.writeBoolean(hidden);
                    this.mRemote.transact(140, _data, _reply, 0);
                    _reply.readException();
                    SurfaceControl _result = (SurfaceControl) _reply.readTypedObject(SurfaceControl.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void removeWeltWindowLeash(SurfaceControl leash) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeTypedObject(leash, 0);
                    this.mRemote.transact(141, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public SurfaceControl getDragAndZoomBgLeash(int width, int height, int x, int y, boolean hidden) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(width);
                    _data.writeInt(height);
                    _data.writeInt(x);
                    _data.writeInt(y);
                    _data.writeBoolean(hidden);
                    this.mRemote.transact(142, _data, _reply, 0);
                    _reply.readException();
                    SurfaceControl _result = (SurfaceControl) _reply.readTypedObject(SurfaceControl.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookActiveMultiWindowMoveStartV3() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(143, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookActiveMultiWindowMoveStartV4(int multiWindowMode, int multiWindowId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    this.mRemote.transact(144, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookActiveMultiWindowStartToMove(IBinder token, int multiWindowMode, int multiWindowId, MotionEvent event, Point downPoint) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    _data.writeTypedObject(event, 0);
                    _data.writeTypedObject(downPoint, 0);
                    this.mRemote.transact(145, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookActiveMultiWindowMove(int multiWindowMode, int multiWindowId, MotionEvent event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    _data.writeTypedObject(event, 0);
                    this.mRemote.transact(146, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookActiveMultiWindowEndMove(int multiWindowMode, int multiWindowId, MotionEvent event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    _data.writeTypedObject(event, 0);
                    this.mRemote.transact(147, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean checkMultiWindowFeatureOn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(148, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public List<String> getMultiWindowTopPackages() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(149, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean getMuteState() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(150, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean getMuteStateV4(int multiWindowId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(multiWindowId);
                    this.mRemote.transact(151, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setMuteState(boolean state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeBoolean(state);
                    this.mRemote.transact(152, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setMuteStateV4(boolean state, int multiWindowId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeBoolean(state);
                    _data.writeInt(multiWindowId);
                    this.mRemote.transact(153, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookMultiWindowMuteAethenV4(int type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(type);
                    this.mRemote.transact(154, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public String getMultiDisplayAreaTopPackage() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(155, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public String getMultiDisplayAreaTopPackageV4(int multiWindowMode, int multiWindowId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    this.mRemote.transact(156, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void moveToBottomForMultiWindowV3(String reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeString(reason);
                    this.mRemote.transact(157, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setMultiWindowAcquireFocus(int multiWindowId, boolean acquireFocus) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(multiWindowId);
                    _data.writeBoolean(acquireFocus);
                    this.mRemote.transact(158, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public ActivityManager.RunningTaskInfo getTopTask(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(159, _data, _reply, 0);
                    _reply.readException();
                    ActivityManager.RunningTaskInfo _result = (ActivityManager.RunningTaskInfo) _reply.readTypedObject(ActivityManager.RunningTaskInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public ActivityManager.RunningTaskInfo getMultiWinTopTask(int winMode, int winId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(winMode);
                    _data.writeInt(winId);
                    this.mRemote.transact(160, _data, _reply, 0);
                    _reply.readException();
                    ActivityManager.RunningTaskInfo _result = (ActivityManager.RunningTaskInfo) _reply.readTypedObject(ActivityManager.RunningTaskInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public int getTaskOrientation(int taskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    this.mRemote.transact(161, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void minimizeMultiWinToEdge(int taskId, boolean toEdge) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    _data.writeBoolean(toEdge);
                    this.mRemote.transact(162, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public Rect getMultiWindowContentRegion(int taskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    this.mRemote.transact(163, _data, _reply, 0);
                    _reply.readException();
                    Rect _result = (Rect) _reply.readTypedObject(Rect.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void updateZBoostTaskIdWhenToSplit(int taskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    this.mRemote.transact(164, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookExitSplitScreenToMultiWindow(int multiWindowMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    this.mRemote.transact(165, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public Rect hookGetMultiWindowDefaultRect(int orientationType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(orientationType);
                    this.mRemote.transact(166, _data, _reply, 0);
                    _reply.readException();
                    Rect _result = (Rect) _reply.readTypedObject(Rect.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public Rect hookGetMultiWindowDefaultRectByTask(int taskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    this.mRemote.transact(167, _data, _reply, 0);
                    _reply.readException();
                    Rect _result = (Rect) _reply.readTypedObject(Rect.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookSetMultiWindowDefaultRectResult(Rect rect) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeTypedObject(rect, 0);
                    this.mRemote.transact(168, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookStartMultiWindowFromSplitScreen(int fullTaskId, WindowContainerToken multiTaskToken, Rect multiWindowRegion, IWindowContainerTransactionCallbackSync syncCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(fullTaskId);
                    _data.writeTypedObject(multiTaskToken, 0);
                    _data.writeTypedObject(multiWindowRegion, 0);
                    _data.writeStrongInterface(syncCallback);
                    this.mRemote.transact(169, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookStartMultiWindowFromSplitScreenV4(int fullTaskId, WindowContainerToken multiTaskToken, Rect multiWindowRegion, IWindowContainerTransactionCallbackSync syncCallback, int type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(fullTaskId);
                    _data.writeTypedObject(multiTaskToken, 0);
                    _data.writeTypedObject(multiWindowRegion, 0);
                    _data.writeStrongInterface(syncCallback);
                    _data.writeInt(type);
                    this.mRemote.transact(170, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setFinishFixedRotationWithTransaction(SurfaceControl leash, float[] transFloat9, float[] cropFloat4, int rotation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeTypedObject(leash, 0);
                    _data.writeFloatArray(transFloat9);
                    _data.writeFloatArray(cropFloat4);
                    _data.writeInt(rotation);
                    this.mRemote.transact(171, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void clearFinishFixedRotationWithTransaction() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(172, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookStartMultiWindow(int taskId, Rect multiWindowRegion, IWindowContainerTransactionCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    _data.writeTypedObject(multiWindowRegion, 0);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(173, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean taskInMultiWindowById(int taskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    this.mRemote.transact(174, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void addAnimationIconLayer(SurfaceControl sc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeTypedObject(sc, 0);
                    this.mRemote.transact(175, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void removeAnimationIconLayer(SurfaceControl sc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeTypedObject(sc, 0);
                    this.mRemote.transact(176, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public IBinder getAppTaskByTaskId(int taskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    this.mRemote.transact(177, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void boostSceneStart(int scene) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(scene);
                    this.mRemote.transact(178, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void boostSceneStartDuration(int scene, long durationMs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(scene);
                    _data.writeLong(durationMs);
                    this.mRemote.transact(179, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void boostSceneEnd(int scene) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(scene);
                    this.mRemote.transact(180, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void boostSceneEndDelay(int scene, long delayMs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(scene);
                    _data.writeLong(delayMs);
                    this.mRemote.transact(181, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void boostStartForLauncher() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(182, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void boostEndForLauncher() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(183, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void boostStartInLauncher(int type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(type);
                    this.mRemote.transact(184, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void boostEndInLauncher(int type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(type);
                    this.mRemote.transact(185, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void handleDrawThreadsBoost(boolean handleAllThreads, boolean bindBigCore, List<String> handleActName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeBoolean(handleAllThreads);
                    _data.writeBoolean(bindBigCore);
                    _data.writeStringList(handleActName);
                    this.mRemote.transact(186, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void resetBoostThreads() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(187, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public Bundle getMultiWindowParams(String pkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeString(pkgName);
                    this.mRemote.transact(188, _data, _reply, 0);
                    _reply.readException();
                    Bundle _result = (Bundle) _reply.readTypedObject(Bundle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setMultiWindowParams(Bundle params) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeTypedObject(params, 0);
                    this.mRemote.transact(189, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean isPinnedMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(190, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public SurfaceControl getDefaultRootLeash() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(191, _data, _reply, 0);
                    _reply.readException();
                    SurfaceControl _result = (SurfaceControl) _reply.readTypedObject(SurfaceControl.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void reparentActivity(int fromDisplayId, int destDisplayId, boolean onTop) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(fromDisplayId);
                    _data.writeInt(destDisplayId);
                    _data.writeBoolean(onTop);
                    this.mRemote.transact(192, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public boolean isSplitScreen() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(193, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setConnectBlackListToSystem(List<String> list) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStringList(list);
                    this.mRemote.transact(194, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookReserveMultiWindowNumber(int reserveNum, long showDelayTime) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeInt(reserveNum);
                    _data.writeLong(showDelayTime);
                    this.mRemote.transact(195, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void hookShowBlurLayerFinish() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    this.mRemote.transact(196, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityTaskManager
            public void setSruWhiteListToSystem(List<String> list) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityTaskManager.DESCRIPTOR);
                    _data.writeStringList(list);
                    this.mRemote.transact(197, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 196;
        }
    }
}
