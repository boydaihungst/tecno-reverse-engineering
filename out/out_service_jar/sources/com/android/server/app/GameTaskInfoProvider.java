package com.android.server.app;

import android.app.ActivityManager;
import android.app.IActivityTaskManager;
import android.content.ComponentName;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.LruCache;
import android.util.Slog;
import java.util.List;
/* loaded from: classes.dex */
final class GameTaskInfoProvider {
    private static final String TAG = "GameTaskInfoProvider";
    private static final int TASK_INFO_CACHE_MAX_SIZE = 50;
    private final IActivityTaskManager mActivityTaskManager;
    private final GameClassifier mGameClassifier;
    private final UserHandle mUserHandle;
    private final Object mLock = new Object();
    private final LruCache<Integer, GameTaskInfo> mGameTaskInfoCache = new LruCache<>(50);

    /* JADX INFO: Access modifiers changed from: package-private */
    public GameTaskInfoProvider(UserHandle userHandle, IActivityTaskManager activityTaskManager, GameClassifier gameClassifier) {
        this.mUserHandle = userHandle;
        this.mActivityTaskManager = activityTaskManager;
        this.mGameClassifier = gameClassifier;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public GameTaskInfo get(int taskId) {
        synchronized (this.mLock) {
            GameTaskInfo cachedTaskInfo = this.mGameTaskInfoCache.get(Integer.valueOf(taskId));
            if (cachedTaskInfo != null) {
                return cachedTaskInfo;
            }
            ActivityManager.RunningTaskInfo runningTaskInfo = getRunningTaskInfo(taskId);
            if (runningTaskInfo == null || runningTaskInfo.baseActivity == null) {
                return null;
            }
            return generateGameInfo(taskId, runningTaskInfo.baseActivity);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public GameTaskInfo get(int taskId, ComponentName componentName) {
        synchronized (this.mLock) {
            GameTaskInfo cachedTaskInfo = this.mGameTaskInfoCache.get(Integer.valueOf(taskId));
            if (cachedTaskInfo != null) {
                if (!cachedTaskInfo.mComponentName.equals(componentName)) {
                    return cachedTaskInfo;
                }
                Slog.w(TAG, "Found cached task info for taskId " + taskId + " but cached component name " + cachedTaskInfo.mComponentName + " does not match " + componentName);
            }
            return generateGameInfo(taskId, componentName);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityManager.RunningTaskInfo getRunningTaskInfo(int taskId) {
        try {
            List<ActivityManager.RunningTaskInfo> runningTaskInfos = this.mActivityTaskManager.getTasks(Integer.MAX_VALUE, false, false);
            for (ActivityManager.RunningTaskInfo taskInfo : runningTaskInfos) {
                if (taskInfo.taskId == taskId) {
                    return taskInfo;
                }
            }
            return null;
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to fetch running tasks");
            return null;
        }
    }

    private GameTaskInfo generateGameInfo(int taskId, ComponentName componentName) {
        GameTaskInfo gameTaskInfo = new GameTaskInfo(taskId, this.mGameClassifier.isGame(componentName.getPackageName(), this.mUserHandle), componentName);
        synchronized (this.mLock) {
            this.mGameTaskInfoCache.put(Integer.valueOf(taskId), gameTaskInfo);
        }
        return gameTaskInfo;
    }
}
