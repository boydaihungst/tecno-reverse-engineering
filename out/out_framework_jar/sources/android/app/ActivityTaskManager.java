package android.app;

import android.app.ActivityManager;
import android.app.IActivityTaskManager;
import android.app.ITaskSplitManagerListener;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.DisplayMetrics;
import android.util.Singleton;
import android.view.IWindow;
import android.view.MotionEvent;
import android.view.MultiTaskRemoteAnimationAdapter;
import android.view.RemoteAnimationDefinition;
import android.view.SurfaceControl;
import android.window.IWindowContainerTransactionCallback;
import android.window.IWindowContainerTransactionCallbackSync;
import android.window.SplashScreenView;
import android.window.WindowContainerToken;
import com.android.internal.R;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class ActivityTaskManager {
    public static final int BOOST_SCENE_2 = 2;
    public static final int BOOST_SCENE_3 = 3;
    public static final int BOOST_SCENE_4 = 4;
    public static final int BOOST_SCENE_5 = 5;
    public static final int BOOST_SCENE_6 = 6;
    public static final int BOOST_SCENE_END = 0;
    public static final int DEFAULT_MINIMAL_SPLIT_SCREEN_DISPLAY_SIZE_DP = 440;
    public static final String EXTRA_IGNORE_TARGET_SECURITY = "android.app.extra.EXTRA_IGNORE_TARGET_SECURITY";
    public static final String EXTRA_OPTIONS = "android.app.extra.OPTIONS";
    public static final int INVALID_STACK_ID = -1;
    public static final int INVALID_TASK_ID = -1;
    public static final int RESIZE_MODE_FORCED = 2;
    public static final int RESIZE_MODE_PRESERVE_WINDOW = 1;
    public static final int RESIZE_MODE_SYSTEM = 0;
    public static final int RESIZE_MODE_SYSTEM_SCREEN_ROTATION = 1;
    public static final int RESIZE_MODE_USER = 1;
    public static final int RESIZE_MODE_USER_FORCED = 3;
    private static int sMaxRecentTasks = -1;
    private static final Singleton<ActivityTaskManager> sInstance = new Singleton<ActivityTaskManager>() { // from class: android.app.ActivityTaskManager.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.util.Singleton
        public ActivityTaskManager create() {
            return new ActivityTaskManager();
        }
    };
    private static final Singleton<IActivityTaskManager> IActivityTaskManagerSingleton = new Singleton<IActivityTaskManager>() { // from class: android.app.ActivityTaskManager.2
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.util.Singleton
        public IActivityTaskManager create() {
            IBinder b = ServiceManager.getService(Context.ACTIVITY_TASK_SERVICE);
            return IActivityTaskManager.Stub.asInterface(b);
        }
    };
    private static List<TaskSplitListener> mTaskSplitListeners = new ArrayList();
    private static ITaskSplitManagerListener mITaskSplitManagerListener = null;
    private static ITaskSplitManager mTaskSplitManager = null;

    private ActivityTaskManager() {
    }

    public static ActivityTaskManager getInstance() {
        return sInstance.get();
    }

    public static IActivityTaskManager getService() {
        return IActivityTaskManagerSingleton.get();
    }

    public void removeRootTasksInWindowingModes(int[] windowingModes) {
        try {
            getService().removeRootTasksInWindowingModes(windowingModes);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeRootTasksWithActivityTypes(int[] activityTypes) {
        try {
            getService().removeRootTasksWithActivityTypes(activityTypes);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeAllVisibleRecentTasks() {
        try {
            getService().removeAllVisibleRecentTasks();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static int getMaxRecentTasksStatic() {
        int i = sMaxRecentTasks;
        if (i < 0) {
            int i2 = ActivityManager.isLowRamDeviceStatic() ? 36 : 48;
            sMaxRecentTasks = i2;
            return i2;
        }
        return i;
    }

    public void onSplashScreenViewCopyFinished(int taskId, SplashScreenView.SplashScreenViewParcelable parcelable) {
        try {
            getService().onSplashScreenViewCopyFinished(taskId, parcelable);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static int getDefaultAppRecentsLimitStatic() {
        return getMaxRecentTasksStatic() / 6;
    }

    public static int getMaxAppRecentsLimitStatic() {
        return getMaxRecentTasksStatic() / 2;
    }

    public static boolean supportsMultiWindow(Context context) {
        boolean isWatch = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH);
        return (!ActivityManager.isLowRamDeviceStatic() || isWatch) && Resources.getSystem().getBoolean(R.bool.config_supportsMultiWindow);
    }

    public static boolean supportsSplitScreenMultiWindow(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        context.getDisplay().getRealMetrics(dm);
        int widthDp = (int) (dm.widthPixels / dm.density);
        int heightDp = (int) (dm.heightPixels / dm.density);
        return Math.max(widthDp, heightDp) >= 440 && supportsMultiWindow(context) && Resources.getSystem().getBoolean(R.bool.config_supportsSplitScreenMultiWindow);
    }

    public void startSystemLockTaskMode(int taskId) {
        try {
            getService().startSystemLockTaskMode(taskId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void stopSystemLockTaskMode() {
        try {
            getService().stopSystemLockTaskMode();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void moveTaskToRootTask(int taskId, int rootTaskId, boolean toTop) {
        try {
            getService().moveTaskToRootTask(taskId, rootTaskId, toTop);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void resizeTask(int taskId, Rect bounds) {
        try {
            getService().resizeTask(taskId, bounds, 0);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void clearLaunchParamsForPackages(List<String> packageNames) {
        try {
            getService().clearLaunchParamsForPackages(packageNames);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    public static boolean currentUiModeSupportsErrorDialogs(Configuration config) {
        int modeType = config.uiMode & 15;
        return (modeType == 3 || (modeType == 6 && Build.IS_USER) || modeType == 4 || modeType == 7) ? false : true;
    }

    public static boolean currentUiModeSupportsErrorDialogs(Context context) {
        Configuration config = context.getResources().getConfiguration();
        return currentUiModeSupportsErrorDialogs(config);
    }

    public static int getMaxNumPictureInPictureActions(Context context) {
        return context.getResources().getInteger(R.integer.config_pictureInPictureMaxNumberOfActions);
    }

    public List<ActivityManager.RunningTaskInfo> getTasks(int maxNum) {
        return getTasks(maxNum, false);
    }

    public List<ActivityManager.RunningTaskInfo> getTasks(int maxNum, boolean filterOnlyVisibleRecents) {
        return getTasks(maxNum, filterOnlyVisibleRecents, false);
    }

    public List<ActivityManager.RunningTaskInfo> getTasks(int maxNum, boolean filterOnlyVisibleRecents, boolean keepIntentExtra) {
        try {
            return getService().getTasks(maxNum, filterOnlyVisibleRecents, keepIntentExtra);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<ActivityManager.RecentTaskInfo> getRecentTasks(int maxNum, int flags, int userId) {
        try {
            return getService().getRecentTasks(maxNum, flags, userId).getList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void registerTaskStackListener(TaskStackListener listener) {
        try {
            getService().registerTaskStackListener(listener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unregisterTaskStackListener(TaskStackListener listener) {
        try {
            getService().unregisterTaskStackListener(listener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Rect getTaskBounds(int taskId) {
        try {
            return getService().getTaskBounds(taskId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void registerRemoteAnimationsForDisplay(int displayId, RemoteAnimationDefinition definition) {
        try {
            getService().registerRemoteAnimationsForDisplay(displayId, definition);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isInLockTaskMode() {
        try {
            return getService().isInLockTaskMode();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean removeTask(int taskId) {
        try {
            return getService().removeTask(taskId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void moveToBottomForMultiWindowV3(String reason) {
        try {
            getService().moveToBottomForMultiWindowV3(reason);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setMultiWindowAcquireFocus(int multiWindowId, boolean acquireFocus) {
        try {
            getService().setMultiWindowAcquireFocus(multiWindowId, acquireFocus);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setMultiWindowAcquireFocuse() {
        try {
            getService().setMultiWindowAcquireFocus(0, true);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setTaskSplitManagerProxy(ITaskSplitManager taskSplitManager) {
        try {
            getService().setTaskSplitManagerProxy(taskSplitManager);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateListener(TaskSplitListener listener, boolean connected) {
        if (connected) {
            if (listener == null) {
                for (int i = 0; i < mTaskSplitListeners.size(); i++) {
                    TaskSplitListener l = mTaskSplitListeners.get(i);
                    l.onTaskSplitManagerConnected(mTaskSplitManager);
                }
                return;
            }
            listener.onTaskSplitManagerConnected(mTaskSplitManager);
        } else if (listener == null) {
            for (int i2 = 0; i2 < mTaskSplitListeners.size(); i2++) {
                TaskSplitListener l2 = mTaskSplitListeners.get(i2);
                l2.onTaskSplitManagerDisconnected();
            }
        } else {
            listener.onTaskSplitManagerDisconnected();
        }
    }

    public boolean registerTaskSplitListenr(TaskSplitListener listener) {
        synchronized (mTaskSplitListeners) {
            if (mTaskSplitListeners.contains(listener)) {
                return false;
            }
            mTaskSplitListeners.add(listener);
            if (mTaskSplitManager != null) {
                updateListener(listener, true);
            }
            if (mTaskSplitListeners.size() == 1 && mITaskSplitManagerListener == null) {
                mITaskSplitManagerListener = new ITaskSplitManagerListener.Stub() { // from class: android.app.ActivityTaskManager.3
                    @Override // android.app.ITaskSplitManagerListener
                    public void onTaskSplitManagerConnected(ITaskSplitManager taskSplitManager) {
                        synchronized (ActivityTaskManager.mTaskSplitListeners) {
                            if (ActivityTaskManager.mTaskSplitManager != taskSplitManager) {
                                ActivityTaskManager.mTaskSplitManager = taskSplitManager;
                                ActivityTaskManager.this.updateListener(null, true);
                            }
                        }
                    }

                    @Override // android.app.ITaskSplitManagerListener
                    public void onTaskSplitManagerDisconnected() {
                        synchronized (ActivityTaskManager.mTaskSplitListeners) {
                            ActivityTaskManager.mTaskSplitManager = null;
                            ActivityTaskManager.this.updateListener(null, false);
                        }
                    }
                };
                try {
                    getService().registerTaskSplitListener(mITaskSplitManagerListener);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            return true;
        }
    }

    public void unregisterTaskSplitListenr(TaskSplitListener listener) {
        synchronized (mTaskSplitListeners) {
            if (mTaskSplitListeners.contains(listener)) {
                mTaskSplitListeners.remove(listener);
                if (mTaskSplitListeners.size() == 0 && mITaskSplitManagerListener != null) {
                    try {
                        getService().unregisterTaskSplitListener(mITaskSplitManagerListener);
                        mITaskSplitManagerListener = null;
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            }
        }
    }

    public void notifyTaskAnimationResult(int taskId, ITaskAnimation taskAnimation) {
        try {
            getService().notifyTaskAnimationResult(taskId, taskAnimation);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ITaskAnimation createTaskAnimation(IBinder token, int taskId, MultiTaskRemoteAnimationAdapter callback) {
        try {
            return getService().createTaskAnimation(token, taskId, callback);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookReparentToDefaultDisplay(int multiWindowMode, int multiWindowId) {
        try {
            getService().hookReparentToDefaultDisplay(multiWindowMode, multiWindowId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookMultiWindowToCloseV3(IWindow window) {
        try {
            getService().hookMultiWindowToCloseV3(window);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookMultiWindowToCloseV4(int multiWindowMode, int multiWindowId) {
        try {
            getService().hookMultiWindowToCloseV4(multiWindowMode, multiWindowId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookMultiWindowToMaxV3(IWindow window) {
        try {
            getService().hookMultiWindowToMaxV3(window);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookMultiWindowToSmallV4(int multiWindowMode, int multiWindowId) {
        try {
            getService().hookMultiWindowToSmallV4(multiWindowMode, multiWindowId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookMultiWindowToSmallV3(IWindow window) {
        try {
            getService().hookMultiWindowToSmallV3(window);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookMultiWindowToSplit(int multiWindowMode, int multiWindowId) {
        try {
            getService().hookMultiWindowToSplit(multiWindowMode, multiWindowId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookMultiWindowMute(IWindow window) {
        try {
            getService().hookMultiWindowMute(window);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookMultiWindowVisible() {
        try {
            getService().hookMultiWindowVisible();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookMultiWindowInvisible() {
        try {
            getService().hookMultiWindowInvisible();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookStartActivityResult(int result, Rect location) {
        try {
            getService().hookStartActivityResult(result, location);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookMultiWindowLocation(IWindow window, int x, int y, int touchX, int touchY) {
        try {
            getService().hookMultiWindowLocation(window, x, y, touchX, touchY);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setTranMultiWindowMode(int mode) {
        try {
            getService().setTranMultiWindowModeV3(mode);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Rect getMultiWindowDefaultRect() {
        try {
            return getService().getMultiWindowDefaultRect();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setStartInMultiWindow(String pkgName, int type, int direction, int startType) {
        try {
            getService().setStartInMultiWindow(pkgName, type, direction, startType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setMultiWindowBlackListToSystem(List<String> list) {
        try {
            getService().setMultiWindowBlackListToSystem(list);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setMultiWindowWhiteListToSystem(List<String> list) {
        try {
            getService().setMultiWindowWhiteListToSystem(list);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setMultiWindowConfigToSystem(String key, List<String> list) {
        try {
            getService().setMultiWindowConfigToSystem(key, list);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getMultiWindowVersion() {
        try {
            return getService().getMultiWindowVersion();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<String> getMultiWindowBlackList() {
        try {
            return getService().getMultiWindowBlackList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean hasMultiWindow() {
        try {
            return getService().hasMultiWindow();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isPinnedMode() {
        try {
            return getService().isPinnedMode();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean activityInMultiWindow(String pkgName) {
        try {
            return getService().activityInMultiWindow(pkgName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getMulitWindowTopPackage() {
        try {
            return getService().getMulitWindowTopPackage();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<String> getMultiWindowTopPackages() {
        try {
            return getService().getMultiWindowTopPackages();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void startCurrentAppInMultiWindow(boolean anim, int startType) {
        try {
            getService().startCurrentAppInMultiWindow(anim, startType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isSupportMultiWindow() {
        try {
            return getService().isSupportMultiWindow();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isSplitScreenSupportMultiWindowV4(int type, ActivityManager.RunningTaskInfo info) {
        try {
            return getService().isSplitScreenSupportMultiWindowV4(type, info);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setFinishFixedRotationEnterMultiWindowTransaction(SurfaceControl leash, int x, int y, int rotation, float scale) {
        try {
            getService().setFinishFixedRotationEnterMultiWindowTransactionV3(leash, x, y, rotation, scale);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setFinishFixedRotationWithTransaction(SurfaceControl leash, float[] transFloat9, float[] cropFloat4, int rotation) {
        try {
            getService().setFinishFixedRotationWithTransaction(leash, transFloat9, cropFloat4, rotation);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void clearFinishFixedRotationWithTransaction() {
        try {
            getService().clearFinishFixedRotationWithTransaction();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public SurfaceControl getWeltWindowLeash(int width, int height, int x, int y, boolean hidden) {
        try {
            return getService().getWeltWindowLeash(width, height, x, y, hidden);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeWeltWindowLeash(SurfaceControl leash) {
        try {
            getService().removeWeltWindowLeash(leash);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public SurfaceControl getDragAndZoomBgLeash(int width, int height, int x, int y, boolean hidden) {
        try {
            return getService().getDragAndZoomBgLeash(width, height, x, y, hidden);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookActiveMultiWindowMoveStartV3() {
        try {
            getService().hookActiveMultiWindowMoveStartV3();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookActiveMultiWindowMoveStartV4(int multiWindowMode, int multiWindowId) {
        try {
            getService().hookActiveMultiWindowMoveStartV4(multiWindowMode, multiWindowId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookActiveMultiWindowStartToMove(IBinder token, int multiWindowMode, int multiWindowId, MotionEvent event, Point downPoint) {
        try {
            getService().hookActiveMultiWindowStartToMove(token, multiWindowMode, multiWindowId, event, downPoint);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookActiveMultiWindowMove(int multiWindowMode, int multiWindowId, MotionEvent event) {
        try {
            getService().hookActiveMultiWindowMove(multiWindowMode, multiWindowId, event);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookActiveMultiWindowEndMove(int multiWindowMode, int multiWindowId, MotionEvent event) {
        try {
            getService().hookActiveMultiWindowEndMove(multiWindowMode, multiWindowId, event);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookExitSplitScreenToMultiWindow(int multiWindowMode) {
        try {
            getService().hookExitSplitScreenToMultiWindow(multiWindowMode);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Rect hookGetMultiWindowDefaultRect(int orientationType) {
        try {
            return getService().hookGetMultiWindowDefaultRect(orientationType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Rect hookGetMultiWindowDefaultRectByTask(int taskId) {
        try {
            return getService().hookGetMultiWindowDefaultRectByTask(taskId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookSetMultiWindowDefaultRectResult(Rect rect) {
        try {
            getService().hookSetMultiWindowDefaultRectResult(rect);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookStartMultiWindowFromSplitScreen(int fullTaskId, WindowContainerToken multiTaskToken, Rect multiWindowRegion, IWindowContainerTransactionCallbackSync syncCallback) {
        try {
            getService().hookStartMultiWindowFromSplitScreen(fullTaskId, multiTaskToken, multiWindowRegion, syncCallback);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookStartMultiWindowFromSplitScreenV4(int fullTaskId, WindowContainerToken multiTaskToken, Rect multiWindowRegion, IWindowContainerTransactionCallbackSync syncCallback, int type) {
        try {
            getService().hookStartMultiWindowFromSplitScreenV4(fullTaskId, multiTaskToken, multiWindowRegion, syncCallback, type);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookStartMultiWindow(int taskId, Rect multiWindowRegion, IWindowContainerTransactionCallback callback) {
        try {
            getService().hookStartMultiWindow(taskId, multiWindowRegion, callback);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean checkMultiWindowFeatureOn() {
        try {
            return getService().checkMultiWindowFeatureOn();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean getMuteStateV4(int multiWindowId) {
        try {
            return getService().getMuteStateV4(multiWindowId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean getMuteState() {
        try {
            return getService().getMuteState();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setMuteStateV4(boolean state, int multiWindowId) {
        try {
            getService().setMuteStateV4(state, multiWindowId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookMultiWindowMuteAethenV4(int type) {
        try {
            getService().hookMultiWindowMuteAethenV4(type);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setMuteState(boolean state) {
        try {
            getService().setMuteState(state);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getMultiDisplayAreaTopPackage() {
        try {
            return getService().getMultiDisplayAreaTopPackage();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getMultiDisplayAreaTopPackageV4(int multiWindowMode, int multiWindowId) {
        try {
            return getService().getMultiDisplayAreaTopPackageV4(multiWindowMode, multiWindowId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ActivityManager.RunningTaskInfo getTopTask(int displayId) {
        try {
            return getService().getTopTask(displayId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ActivityManager.RunningTaskInfo getMultiWinTopTask(int winMode, int winId) {
        try {
            return getService().getMultiWinTopTask(winMode, winId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getTaskOrientation(int taskId) {
        try {
            return getService().getTaskOrientation(taskId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void minimizeMultiWinToEdge(int taskId, boolean toEdge) {
        try {
            getService().minimizeMultiWinToEdge(taskId, toEdge);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Rect getMultiWindowContentRegion(int taskId) {
        try {
            return getService().getMultiWindowContentRegion(taskId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Bundle getMultiWindowParams(String pkgName) {
        try {
            return getService().getMultiWindowParams(pkgName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setMultiWindowParams(Bundle params) {
        try {
            getService().setMultiWindowParams(params);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void updateZBoostTaskIdWhenToSplit(int taskId) {
        try {
            getService().updateZBoostTaskIdWhenToSplit(taskId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean taskInMultiWindowById(int taskId) {
        try {
            return getService().taskInMultiWindowById(taskId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void addAnimationIconLayer(SurfaceControl sc) {
        try {
            getService().addAnimationIconLayer(sc);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeAnimationIconLayer(SurfaceControl sc) {
        try {
            getService().removeAnimationIconLayer(sc);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public SurfaceControl getDefaultRootLeash() {
        try {
            return getService().getDefaultRootLeash();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookReserveMultiWindowNumber(int reserveNum, long showDelayTime) {
        try {
            getService().hookReserveMultiWindowNumber(reserveNum, showDelayTime);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void hookShowBlurLayerFinish() {
        try {
            getService().hookShowBlurLayerFinish();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void reparentActivity(int fromDisplayId, int destDisplayId, boolean onTop) {
        try {
            getService().reparentActivity(fromDisplayId, destDisplayId, onTop);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isSplitScreen() {
        try {
            return getService().isSplitScreen();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setConnectBlackListToSystem(List<String> list) {
        try {
            getService().setConnectBlackListToSystem(list);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void detachNavigationBarFromApp(IBinder transition) {
        try {
            getService().detachNavigationBarFromApp(transition);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void boostSceneStart(int scene) {
        try {
            getService().boostSceneStart(scene);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void boostSceneStartDuration(int scene, long durationMs) {
        try {
            getService().boostSceneStartDuration(scene, durationMs);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void boostSceneEnd(int scene) {
        try {
            getService().boostSceneEnd(scene);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void boostSceneEndDelay(int scene, long delayMs) {
        try {
            getService().boostSceneEndDelay(scene, delayMs);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void boostStartForLauncher() {
        try {
            getService().boostStartForLauncher();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void boostEndForLauncher() {
        try {
            getService().boostEndForLauncher();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void boostStartInLauncher(int type) {
        try {
            getService().boostStartInLauncher(type);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void boostEndInLauncher(int type) {
        try {
            getService().boostEndInLauncher(type);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void handleDrawThreadsBoost(boolean handleAllThreads, boolean bindBigCore, List<String> handleActName) {
        try {
            getService().handleDrawThreadsBoost(handleAllThreads, bindBigCore, handleActName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void resetBoostThreads() {
        try {
            getService().resetBoostThreads();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* loaded from: classes.dex */
    public static class RootTaskInfo extends TaskInfo implements Parcelable {
        public static final Parcelable.Creator<RootTaskInfo> CREATOR = new Parcelable.Creator<RootTaskInfo>() { // from class: android.app.ActivityTaskManager.RootTaskInfo.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public RootTaskInfo createFromParcel(Parcel source) {
                return new RootTaskInfo(source);
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public RootTaskInfo[] newArray(int size) {
                return new RootTaskInfo[size];
            }
        };
        public Rect bounds;
        public Rect[] childTaskBounds;
        public int[] childTaskIds;
        public String[] childTaskNames;
        public int[] childTaskUserIds;
        public int position;
        public boolean visible;

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.app.TaskInfo, android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeTypedObject(this.bounds, flags);
            dest.writeIntArray(this.childTaskIds);
            dest.writeStringArray(this.childTaskNames);
            dest.writeTypedArray(this.childTaskBounds, flags);
            dest.writeIntArray(this.childTaskUserIds);
            dest.writeInt(this.visible ? 1 : 0);
            dest.writeInt(this.position);
            super.writeToParcel(dest, flags);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // android.app.TaskInfo
        public void readFromParcel(Parcel source) {
            this.bounds = (Rect) source.readTypedObject(Rect.CREATOR);
            this.childTaskIds = source.createIntArray();
            this.childTaskNames = source.createStringArray();
            this.childTaskBounds = (Rect[]) source.createTypedArray(Rect.CREATOR);
            this.childTaskUserIds = source.createIntArray();
            this.visible = source.readInt() > 0;
            this.position = source.readInt();
            super.readFromParcel(source);
        }

        public RootTaskInfo() {
            this.bounds = new Rect();
        }

        private RootTaskInfo(Parcel source) {
            this.bounds = new Rect();
            readFromParcel(source);
        }

        @Override // android.app.TaskInfo
        public String toString() {
            StringBuilder sb = new StringBuilder(256);
            sb.append("RootTask id=");
            sb.append(this.taskId);
            sb.append(" bounds=");
            sb.append(this.bounds.toShortString());
            sb.append(" displayId=");
            sb.append(this.displayId);
            sb.append(" userId=");
            sb.append(this.userId);
            sb.append("\n");
            sb.append(" configuration=");
            sb.append(this.configuration);
            sb.append("\n");
            for (int i = 0; i < this.childTaskIds.length; i++) {
                sb.append("  taskId=");
                sb.append(this.childTaskIds[i]);
                sb.append(": ");
                sb.append(this.childTaskNames[i]);
                if (this.childTaskBounds != null) {
                    sb.append(" bounds=");
                    sb.append(this.childTaskBounds[i].toShortString());
                }
                sb.append(" userId=").append(this.childTaskUserIds[i]);
                sb.append(" visible=").append(this.visible);
                if (this.topActivity != null) {
                    sb.append(" topActivity=").append(this.topActivity);
                }
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    public void setSruWhiteListToSystem(List<String> list) {
        try {
            getService().setSruWhiteListToSystem(list);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
