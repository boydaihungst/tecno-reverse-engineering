package com.android.server.wm;

import android.content.ComponentName;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.ArraySet;
import android.util.Slog;
import android.view.InsetsSource;
import android.view.InsetsState;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import com.android.server.inputmethod.InputMethodManagerInternal;
/* loaded from: classes2.dex */
public class SplitScreenHelper {
    private static final long SPLIT_APP_CLOSE_DURATION = 250;
    private static final int TRANSIT_SPLIT_APP_CLOSE = 13;
    private static final int TRANSIT_SPLIT_APP_OPEN = 12;
    private static final String TAG = SplitScreenHelper.class.getSimpleName();
    static final boolean DISABLE_CUSTOM = "1".equals(SystemProperties.get("persist.sys.tran_split_screen.debug_disable", "0"));
    static final long MIN_FREEZE_TIME_FOR_SPLIT_SCREEN = SystemProperties.getLong("persist.sys.tran_split_screen.min_freeze_time", 300);
    private static final ComponentName MINI_LAUNCHER = new ComponentName("com.transsion.ossettingsext", "com.transsion.minilauncher.MainActivity");
    private static final Interpolator SPLIT_APP_INTERPOLATOR = new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f);

    public static boolean isInSplitScreenTaskStack(Task task, Task tr, int last) {
        return task.isRootTask() && getParentTaskInSplitScreenTaskStack(tr, task) != null;
    }

    public static boolean isSplitScreenRootTask(Task rootTask) {
        return getSplitScreenAdjacentTaskFragmentsForTask(rootTask, rootTask) != null;
    }

    public static boolean isSplitScreenTask(Task task) {
        Task[] tasks = getSplitScreenAdjacentTaskFragmentsForTask(task, null);
        if (tasks == null || tasks.length != 2) {
            return false;
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_ROOT_TASK) {
            Slog.d(TAG, task + " is split screen task");
            return true;
        }
        return true;
    }

    public static Task getParentTaskInSplitScreenTaskStack(Task task, Task rootTask) {
        Task[] tasks = getSplitScreenAdjacentTaskFragmentsForTask(task, rootTask);
        if (tasks == null || tasks.length != 2) {
            return null;
        }
        if (tasks[0].getTopChild() == task) {
            return tasks[0];
        }
        if (tasks[1].getTopChild() != task) {
            return null;
        }
        return tasks[1];
    }

    public static Task[] getSplitScreenAdjacentTaskFragmentsForTask(Task task, Task rootTask) {
        if (DISABLE_CUSTOM) {
            return null;
        }
        if (rootTask == null) {
            rootTask = task.getRootTask();
        }
        if (rootTask != null && rootTask.getChildCount() == 2) {
            Task first = rootTask.getChildAt(0).asTask();
            Task second = rootTask.getChildAt(1).asTask();
            if (first != null && second != null && first.getAdjacentTaskFragment() == second && second.getAdjacentTaskFragment() == first) {
                if (ActivityTaskManagerDebugConfig.DEBUG_ROOT_TASK) {
                    Slog.d(TAG, "current split screen adjacent pair is " + first + " and " + second);
                }
                return new Task[]{first, second};
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Task getParentTaskInSplitScreenTaskStack(Task task) {
        return getParentTaskInSplitScreenTaskStack(task, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean removeReuseTaskIfNeed(ActivityRecord sourceRecord, Task reusedTask) {
        if (reusedTask == null) {
            return false;
        }
        Task sourceTask = sourceRecord == null ? null : sourceRecord.getTask();
        if (sourceTask != null && isSplitScreenTask(sourceTask)) {
            int activityType = reusedTask.getActivityType();
            if (activityType == 2 || activityType == 3) {
                Slog.w(TAG, "warning: launch launcher in to split screen by " + sourceTask + ", target is " + reusedTask);
                return false;
            } else if (reusedTask.supportsSplitScreenWindowingMode() && reusedTask.getWindowingMode() == 1) {
                Slog.d(TAG, "fullscreen to split screen, remove " + reusedTask);
                sourceTask.mTaskSupervisor.removeTask(reusedTask, false, true, "remove from full screen");
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean shouldBlockStopFreezingForSplitScreen(RootWindowContainer rootWindowContainer, WindowManagerService wms) {
        long freezeTime = SystemClock.elapsedRealtime() - wms.mDisplayFreezeTime;
        if (freezeTime < MIN_FREEZE_TIME_FOR_SPLIT_SCREEN) {
            Task task = rootWindowContainer.getTopDisplayFocusedRootTask();
            Slog.d(TAG, "shouldBlockStopFreezingForSplitScreen " + task);
            if (task != null && isSplitScreenTask(task)) {
                return true;
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isIgnoreCloseTransitionForSplitScreen(Task task, boolean endTask) {
        Task parentTaskInSplitScreen = getParentTaskInSplitScreenTaskStack(task);
        if ((task.getChildCount() <= 1 || endTask) && parentTaskInSplitScreen != null && parentTaskInSplitScreen.getChildCount() == 1) {
            Slog.d(TAG, "well, this will cause split screen mode to exit " + task);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getTransitForSplitScreen(DisplayContent displayContent, int transit, boolean enter) {
        if (displayContent == null) {
            return -1;
        }
        if (transit == 8 && isTargetActivitySetContainsMiniLauncher(displayContent.mClosingApps)) {
            return 12;
        }
        if (transit != 9 || !isTargetActivitySetContainsMiniLauncher(displayContent.mOpeningApps)) {
            return -1;
        }
        if (!enter) {
            return 13;
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void modifyAnimationForSplitScreen(Animation animation, int transit, boolean enter) {
        if (transit == 13 && !enter) {
            animation.setDuration(SPLIT_APP_CLOSE_DURATION);
            animation.setInterpolator(SPLIT_APP_INTERPOLATOR);
        }
    }

    static boolean isTargetActivitySetContainsMiniLauncher(ArraySet<ActivityRecord> activities) {
        int size = activities.size();
        for (int i = size - 1; i >= 0; i--) {
            if (MINI_LAUNCHER.equals(activities.valueAt(i).mActivityComponent)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void onUpdateRotation(DisplayContent displayContent) {
        Task focusedTask = displayContent.mFocusedApp == null ? null : displayContent.mFocusedApp.getTask();
        if (focusedTask != null && getParentTaskInSplitScreenTaskStack(focusedTask) != null) {
            DisplayFrames displayFrames = displayContent.mDisplayFrames;
            InsetsState state = displayContent.getInsetsStateController().getRawInsetsState();
            InsetsSource imeSource = state.peekSource(19);
            if (imeSource != null && imeSource.isVisible()) {
                float ratio = displayFrames.mDisplayHeight / displayFrames.mDisplayWidth;
                if (ratio > 1.7d || ratio < 0.6d) {
                    Slog.d(TAG, "hide ime before screen rote for split screen, ratio = " + ratio);
                    InputMethodManagerInternal.get().hideCurrentInputMethod(17);
                }
            }
        }
    }
}
