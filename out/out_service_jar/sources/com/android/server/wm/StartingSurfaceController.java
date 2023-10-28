package com.android.server.wm;

import android.app.ActivityOptions;
import android.app.compat.CompatChanges;
import android.content.pm.ApplicationInfo;
import android.os.UserHandle;
import android.util.Slog;
import android.window.TaskSnapshot;
import com.mediatek.server.wm.WmsExt;
import java.util.ArrayList;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public class StartingSurfaceController {
    private static final long ALLOW_COPY_SOLID_COLOR_VIEW = 205907456;
    private static final String TAG = WmsExt.TAG;
    private final ArrayList<DeferringStartingWindowRecord> mDeferringAddStartActivities = new ArrayList<>();
    private boolean mDeferringAddStartingWindow;
    boolean mInitNewTask;
    boolean mInitProcessRunning;
    boolean mInitTaskSwitch;
    private final WindowManagerService mService;
    private final SplashScreenExceptionList mSplashScreenExceptionsList;

    public StartingSurfaceController(WindowManagerService wm) {
        this.mService = wm;
        this.mSplashScreenExceptionsList = new SplashScreenExceptionList(wm.mContext.getMainExecutor());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public StartingSurface createSplashScreenStartingSurface(ActivityRecord activity, int theme) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Task task = activity.getTask();
                if (task == null || !this.mService.mAtmService.mTaskOrganizerController.addStartingWindow(task, activity, theme, null)) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return null;
                }
                StartingSurface startingSurface = new StartingSurface(task);
                WindowManagerService.resetPriorityAfterLockedSection();
                return startingSurface;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isExceptionApp(String packageName, int targetSdk, Supplier<ApplicationInfo> infoProvider) {
        return this.mSplashScreenExceptionsList.isException(packageName, targetSdk, infoProvider);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int makeStartingWindowTypeParameter(boolean newTask, boolean taskSwitch, boolean processRunning, boolean allowTaskSnapshot, boolean activityCreated, boolean isSolidColor, boolean useLegacy, boolean activityDrawn, int startingWindowType, String packageName, int userId) {
        int parameter = 0;
        if (newTask) {
            parameter = 0 | 1;
        }
        if (taskSwitch) {
            parameter |= 2;
        }
        if (processRunning) {
            parameter |= 4;
        }
        if (allowTaskSnapshot) {
            parameter |= 8;
        }
        if (activityCreated || startingWindowType == 1) {
            parameter |= 16;
        }
        if (isSolidColor) {
            parameter |= 32;
        }
        if (useLegacy) {
            parameter |= Integer.MIN_VALUE;
        }
        if (activityDrawn) {
            parameter |= 64;
        }
        if (startingWindowType == 2 && CompatChanges.isChangeEnabled((long) ALLOW_COPY_SOLID_COLOR_VIEW, packageName, UserHandle.of(userId))) {
            return parameter | 128;
        }
        return parameter;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public StartingSurface createTaskSnapshotSurface(ActivityRecord activity, TaskSnapshot taskSnapshot) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Task task = activity.getTask();
                if (task == null) {
                    Slog.w(TAG, "TaskSnapshotSurface.create: Failed to find task for activity=" + activity);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return null;
                }
                ActivityRecord topFullscreenActivity = activity.getTask().getTopFullscreenActivity();
                if (topFullscreenActivity == null) {
                    Slog.w(TAG, "TaskSnapshotSurface.create: Failed to find top fullscreen for task=" + task);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return null;
                }
                WindowState topFullscreenOpaqueWindow = topFullscreenActivity.getTopFullscreenOpaqueWindow();
                if (topFullscreenOpaqueWindow == null) {
                    Slog.w(TAG, "TaskSnapshotSurface.create: no opaque window in " + topFullscreenActivity);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return null;
                }
                if (activity.mDisplayContent.getRotation() != taskSnapshot.getRotation()) {
                    activity.mDisplayContent.handleTopActivityLaunchingInDifferentOrientation(activity, false);
                }
                this.mService.mAtmService.mTaskOrganizerController.addStartingWindow(task, activity, 0, taskSnapshot);
                StartingSurface startingSurface = new StartingSurface(task);
                WindowManagerService.resetPriorityAfterLockedSection();
                return startingSurface;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class DeferringStartingWindowRecord {
        final ActivityRecord mDeferring;
        final ActivityRecord mPrev;
        final ActivityRecord mSource;

        DeferringStartingWindowRecord(ActivityRecord deferring, ActivityRecord prev, ActivityRecord source) {
            this.mDeferring = deferring;
            this.mPrev = prev;
            this.mSource = source;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void showStartingWindow(ActivityRecord target, ActivityRecord prev, boolean newTask, boolean isTaskSwitch, ActivityRecord source) {
        if (this.mDeferringAddStartingWindow) {
            addDeferringRecord(target, prev, newTask, isTaskSwitch, source);
        } else {
            target.showStartingWindow(prev, newTask, isTaskSwitch, true, source);
        }
    }

    private void addDeferringRecord(ActivityRecord deferring, ActivityRecord prev, boolean newTask, boolean isTaskSwitch, ActivityRecord source) {
        if (this.mDeferringAddStartActivities.isEmpty()) {
            this.mInitProcessRunning = deferring.isProcessRunning();
            this.mInitNewTask = newTask;
            this.mInitTaskSwitch = isTaskSwitch;
        }
        this.mDeferringAddStartActivities.add(new DeferringStartingWindowRecord(deferring, prev, source));
    }

    private void showStartingWindowFromDeferringActivities(ActivityOptions topOptions) {
        for (int i = this.mDeferringAddStartActivities.size() - 1; i >= 0; i--) {
            DeferringStartingWindowRecord next = this.mDeferringAddStartActivities.get(i);
            next.mDeferring.showStartingWindow(next.mPrev, this.mInitNewTask, this.mInitTaskSwitch, this.mInitProcessRunning, true, next.mSource, topOptions);
            if (next.mDeferring.mStartingData != null) {
                break;
            }
        }
        this.mDeferringAddStartActivities.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void beginDeferAddStartingWindow() {
        this.mDeferringAddStartingWindow = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void endDeferAddStartingWindow(ActivityOptions topOptions) {
        this.mDeferringAddStartingWindow = false;
        showStartingWindowFromDeferringActivities(topOptions);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class StartingSurface {
        private final Task mTask;

        StartingSurface(Task task) {
            this.mTask = task;
        }

        public void remove(boolean animate) {
            synchronized (StartingSurfaceController.this.mService.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    StartingSurfaceController.this.mService.mAtmService.mTaskOrganizerController.removeStartingWindow(this.mTask, animate);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }
    }
}
