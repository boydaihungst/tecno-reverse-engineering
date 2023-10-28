package com.android.server.wm;

import android.util.EventLog;
/* loaded from: classes2.dex */
public class EventLogTags {
    public static final int WM_ACTIVITY_LAUNCH_TIME = 30009;
    public static final int WM_ADD_TO_STOPPING = 30066;
    public static final int WM_BOOT_ANIMATION_DONE = 31007;
    public static final int WM_CREATE_ACTIVITY = 30005;
    public static final int WM_CREATE_TASK = 30004;
    public static final int WM_DESTROY_ACTIVITY = 30018;
    public static final int WM_FAILED_TO_PAUSE = 30012;
    public static final int WM_FINISH_ACTIVITY = 30001;
    public static final int WM_FOCUSED_ROOT_TASK = 30044;
    public static final int WM_NEW_INTENT = 30003;
    public static final int WM_NO_SURFACE_MEMORY = 31000;
    public static final int WM_PAUSE_ACTIVITY = 30013;
    public static final int WM_RELAUNCH_ACTIVITY = 30020;
    public static final int WM_RELAUNCH_RESUME_ACTIVITY = 30019;
    public static final int WM_REMOVE_TASK = 30061;
    public static final int WM_RESTART_ACTIVITY = 30006;
    public static final int WM_RESUME_ACTIVITY = 30007;
    public static final int WM_SET_KEYGUARD_SHOWN = 30067;
    public static final int WM_SET_RESUMED_ACTIVITY = 30043;
    public static final int WM_STOP_ACTIVITY = 30048;
    public static final int WM_TASK_CREATED = 31001;
    public static final int WM_TASK_MOVED = 31002;
    public static final int WM_TASK_REMOVED = 31003;
    public static final int WM_TASK_TO_FRONT = 30002;
    public static final int WM_WALLPAPER_SURFACE = 33001;

    private EventLogTags() {
    }

    public static void writeWmFinishActivity(int user, int token, int taskId, String componentName, String reason) {
        EventLog.writeEvent((int) WM_FINISH_ACTIVITY, Integer.valueOf(user), Integer.valueOf(token), Integer.valueOf(taskId), componentName, reason);
    }

    public static void writeWmTaskToFront(int user, int task) {
        EventLog.writeEvent((int) WM_TASK_TO_FRONT, Integer.valueOf(user), Integer.valueOf(task));
    }

    public static void writeWmNewIntent(int user, int token, int taskId, String componentName, String action, String mimeType, String uri, int flags) {
        EventLog.writeEvent((int) WM_NEW_INTENT, Integer.valueOf(user), Integer.valueOf(token), Integer.valueOf(taskId), componentName, action, mimeType, uri, Integer.valueOf(flags));
    }

    public static void writeWmCreateTask(int user, int taskId) {
        EventLog.writeEvent((int) WM_CREATE_TASK, Integer.valueOf(user), Integer.valueOf(taskId));
    }

    public static void writeWmCreateActivity(int user, int token, int taskId, String componentName, String action, String mimeType, String uri, int flags) {
        EventLog.writeEvent((int) WM_CREATE_ACTIVITY, Integer.valueOf(user), Integer.valueOf(token), Integer.valueOf(taskId), componentName, action, mimeType, uri, Integer.valueOf(flags));
    }

    public static void writeWmRestartActivity(int user, int token, int taskId, String componentName) {
        EventLog.writeEvent((int) WM_RESTART_ACTIVITY, Integer.valueOf(user), Integer.valueOf(token), Integer.valueOf(taskId), componentName);
    }

    public static void writeWmResumeActivity(int user, int token, int taskId, String componentName) {
        EventLog.writeEvent((int) WM_RESUME_ACTIVITY, Integer.valueOf(user), Integer.valueOf(token), Integer.valueOf(taskId), componentName);
    }

    public static void writeWmActivityLaunchTime(int user, int token, String componentName, long time) {
        EventLog.writeEvent((int) WM_ACTIVITY_LAUNCH_TIME, Integer.valueOf(user), Integer.valueOf(token), componentName, Long.valueOf(time));
    }

    public static void writeWmFailedToPause(int user, int token, String wantingToPause, String currentlyPausing) {
        EventLog.writeEvent((int) WM_FAILED_TO_PAUSE, Integer.valueOf(user), Integer.valueOf(token), wantingToPause, currentlyPausing);
    }

    public static void writeWmPauseActivity(int user, int token, String componentName, String userLeaving, String reason) {
        EventLog.writeEvent((int) WM_PAUSE_ACTIVITY, Integer.valueOf(user), Integer.valueOf(token), componentName, userLeaving, reason);
    }

    public static void writeWmDestroyActivity(int user, int token, int taskId, String componentName, String reason) {
        EventLog.writeEvent((int) WM_DESTROY_ACTIVITY, Integer.valueOf(user), Integer.valueOf(token), Integer.valueOf(taskId), componentName, reason);
    }

    public static void writeWmRelaunchResumeActivity(int user, int token, int taskId, String componentName) {
        EventLog.writeEvent((int) WM_RELAUNCH_RESUME_ACTIVITY, Integer.valueOf(user), Integer.valueOf(token), Integer.valueOf(taskId), componentName);
    }

    public static void writeWmRelaunchActivity(int user, int token, int taskId, String componentName) {
        EventLog.writeEvent((int) WM_RELAUNCH_ACTIVITY, Integer.valueOf(user), Integer.valueOf(token), Integer.valueOf(taskId), componentName);
    }

    public static void writeWmSetResumedActivity(int user, String componentName, String reason) {
        EventLog.writeEvent((int) WM_SET_RESUMED_ACTIVITY, Integer.valueOf(user), componentName, reason);
    }

    public static void writeWmFocusedRootTask(int user, int displayId, int focusedRootTaskId, int lastFocusedRootTaskId, String reason) {
        EventLog.writeEvent((int) WM_FOCUSED_ROOT_TASK, Integer.valueOf(user), Integer.valueOf(displayId), Integer.valueOf(focusedRootTaskId), Integer.valueOf(lastFocusedRootTaskId), reason);
    }

    public static void writeWmStopActivity(int user, int token, String componentName) {
        EventLog.writeEvent((int) WM_STOP_ACTIVITY, Integer.valueOf(user), Integer.valueOf(token), componentName);
    }

    public static void writeWmRemoveTask(int taskId, int rootTaskId) {
        EventLog.writeEvent((int) WM_REMOVE_TASK, Integer.valueOf(taskId), Integer.valueOf(rootTaskId));
    }

    public static void writeWmAddToStopping(int user, int token, String componentName, String reason) {
        EventLog.writeEvent((int) WM_ADD_TO_STOPPING, Integer.valueOf(user), Integer.valueOf(token), componentName, reason);
    }

    public static void writeWmSetKeyguardShown(int displayId, int keyguardshowing, int aodshowing, int keyguardgoingaway, String reason) {
        EventLog.writeEvent((int) WM_SET_KEYGUARD_SHOWN, Integer.valueOf(displayId), Integer.valueOf(keyguardshowing), Integer.valueOf(aodshowing), Integer.valueOf(keyguardgoingaway), reason);
    }

    public static void writeWmNoSurfaceMemory(String window, int pid, String operation) {
        EventLog.writeEvent((int) WM_NO_SURFACE_MEMORY, window, Integer.valueOf(pid), operation);
    }

    public static void writeWmTaskCreated(int taskid, int roottaskid) {
        EventLog.writeEvent((int) WM_TASK_CREATED, Integer.valueOf(taskid), Integer.valueOf(roottaskid));
    }

    public static void writeWmTaskMoved(int taskid, int totop, int index) {
        EventLog.writeEvent((int) WM_TASK_MOVED, Integer.valueOf(taskid), Integer.valueOf(totop), Integer.valueOf(index));
    }

    public static void writeWmTaskRemoved(int taskid, String reason) {
        EventLog.writeEvent((int) WM_TASK_REMOVED, Integer.valueOf(taskid), reason);
    }

    public static void writeWmBootAnimationDone(long time) {
        EventLog.writeEvent((int) WM_BOOT_ANIMATION_DONE, time);
    }

    public static void writeWmWallpaperSurface(int displayId, int visible) {
        EventLog.writeEvent((int) WM_WALLPAPER_SURFACE, Integer.valueOf(displayId), Integer.valueOf(visible));
    }
}
