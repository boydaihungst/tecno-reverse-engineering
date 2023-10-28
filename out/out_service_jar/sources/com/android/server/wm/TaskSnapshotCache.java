package com.android.server.wm;

import android.util.ArrayMap;
import android.window.TaskSnapshot;
import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class TaskSnapshotCache {
    private final TaskSnapshotLoader mLoader;
    private final WindowManagerService mService;
    private final ArrayMap<ActivityRecord, Integer> mAppTaskMap = new ArrayMap<>();
    private final ArrayMap<Integer, CacheEntry> mRunningCache = new ArrayMap<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskSnapshotCache(WindowManagerService service, TaskSnapshotLoader loader) {
        this.mService = service;
        this.mLoader = loader;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearRunningCache() {
        this.mRunningCache.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void putSnapshot(Task task, TaskSnapshot snapshot) {
        CacheEntry entry = this.mRunningCache.get(Integer.valueOf(task.mTaskId));
        if (entry != null) {
            this.mAppTaskMap.remove(entry.topApp);
        }
        ActivityRecord top = task.getTopMostActivity();
        this.mAppTaskMap.put(top, Integer.valueOf(task.mTaskId));
        this.mRunningCache.put(Integer.valueOf(task.mTaskId), new CacheEntry(snapshot, top));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskSnapshot getSnapshot(int taskId, int userId, boolean restoreFromDisk, boolean isLowResolution) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                CacheEntry entry = this.mRunningCache.get(Integer.valueOf(taskId));
                if (entry != null) {
                    TaskSnapshot taskSnapshot = entry.snapshot;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return taskSnapshot;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                if (!restoreFromDisk) {
                    return null;
                }
                return tryRestoreFromDisk(taskId, userId, isLowResolution);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private TaskSnapshot tryRestoreFromDisk(int taskId, int userId, boolean isLowResolution) {
        TaskSnapshot snapshot = this.mLoader.loadTask(taskId, userId, isLowResolution);
        if (snapshot == null) {
            return null;
        }
        return snapshot;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAppRemoved(ActivityRecord activity) {
        Integer taskId = this.mAppTaskMap.get(activity);
        if (taskId != null) {
            removeRunningEntry(taskId.intValue());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAppDied(ActivityRecord activity) {
        Integer taskId = this.mAppTaskMap.get(activity);
        if (taskId != null) {
            removeRunningEntry(taskId.intValue());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTaskRemoved(int taskId) {
        removeRunningEntry(taskId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeRunningEntry(int taskId) {
        CacheEntry entry = this.mRunningCache.get(Integer.valueOf(taskId));
        if (entry != null) {
            this.mAppTaskMap.remove(entry.topApp);
            this.mRunningCache.remove(Integer.valueOf(taskId));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        String doublePrefix = prefix + "  ";
        String triplePrefix = doublePrefix + "  ";
        pw.println(prefix + "SnapshotCache");
        for (int i = this.mRunningCache.size() - 1; i >= 0; i--) {
            CacheEntry entry = this.mRunningCache.valueAt(i);
            pw.println(doublePrefix + "Entry taskId=" + this.mRunningCache.keyAt(i));
            pw.println(triplePrefix + "topApp=" + entry.topApp);
            pw.println(triplePrefix + "snapshot=" + entry.snapshot);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class CacheEntry {
        final TaskSnapshot snapshot;
        final ActivityRecord topApp;

        CacheEntry(TaskSnapshot snapshot, ActivityRecord topApp) {
            this.snapshot = snapshot;
            this.topApp = topApp;
        }
    }
}
