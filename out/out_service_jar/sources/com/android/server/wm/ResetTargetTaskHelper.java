package com.android.server.wm;

import android.app.ActivityOptions;
import android.os.Debug;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class ResetTargetTaskHelper implements Consumer<Task>, Predicate<ActivityRecord> {
    private int mActivityReparentPosition;
    private boolean mCanMoveOptions;
    private boolean mForceReset;
    private boolean mIsTargetTask;
    private ActivityRecord mRoot;
    private Task mTargetRootTask;
    private Task mTargetTask;
    private boolean mTargetTaskFound;
    private Task mTask;
    private ActivityOptions mTopOptions;
    private ArrayList<ActivityRecord> mResultActivities = new ArrayList<>();
    private ArrayList<ActivityRecord> mAllActivities = new ArrayList<>();
    private ArrayList<ActivityRecord> mPendingReparentActivities = new ArrayList<>();

    private void reset(Task task) {
        this.mTask = task;
        this.mRoot = null;
        this.mCanMoveOptions = true;
        this.mTopOptions = null;
        this.mResultActivities.clear();
        this.mAllActivities.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityOptions process(Task targetTask, boolean forceReset) {
        this.mForceReset = forceReset;
        this.mTargetTask = targetTask;
        this.mTargetTaskFound = false;
        this.mTargetRootTask = targetTask.getRootTask();
        this.mActivityReparentPosition = -1;
        targetTask.mWmService.mRoot.forAllLeafTasks(this, true);
        processPendingReparentActivities();
        reset(null);
        return this.mTopOptions;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // java.util.function.Consumer
    public void accept(Task task) {
        reset(task);
        ActivityRecord rootActivity = task.getRootActivity(true);
        this.mRoot = rootActivity;
        if (rootActivity == null) {
            return;
        }
        boolean z = task == this.mTargetTask;
        this.mIsTargetTask = z;
        if (z) {
            this.mTargetTaskFound = true;
        }
        task.forAllActivities((Predicate<ActivityRecord>) this);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // java.util.function.Predicate
    public boolean test(ActivityRecord r) {
        ActivityRecord p;
        if (r == this.mRoot) {
            return true;
        }
        this.mAllActivities.add(r);
        int flags = r.info.flags;
        boolean finishOnTaskLaunch = (flags & 2) != 0;
        boolean allowTaskReparenting = (flags & 64) != 0;
        boolean clearWhenTaskReset = (r.intent.getFlags() & 524288) != 0;
        if (this.mIsTargetTask) {
            if (!finishOnTaskLaunch && !clearWhenTaskReset) {
                if (r.resultTo != null) {
                    this.mResultActivities.add(r);
                    return false;
                } else if (allowTaskReparenting && r.taskAffinity != null && !r.taskAffinity.equals(this.mTask.affinity)) {
                    this.mPendingReparentActivities.add(r);
                    return false;
                }
            }
            if (this.mForceReset || finishOnTaskLaunch || clearWhenTaskReset) {
                if (clearWhenTaskReset) {
                    finishActivities(this.mAllActivities, "clearWhenTaskReset");
                } else {
                    this.mResultActivities.add(r);
                    finishActivities(this.mResultActivities, "reset-task");
                }
                this.mResultActivities.clear();
                return false;
            }
            this.mResultActivities.clear();
            return false;
        } else if (r.resultTo != null) {
            this.mResultActivities.add(r);
            return false;
        } else {
            if (this.mTargetTaskFound && allowTaskReparenting && this.mTargetTask.affinity != null && this.mTargetTask.affinity.equals(r.taskAffinity)) {
                this.mResultActivities.add(r);
                if (this.mForceReset || finishOnTaskLaunch) {
                    finishActivities(this.mResultActivities, "move-affinity");
                    return false;
                }
                if (this.mActivityReparentPosition == -1) {
                    this.mActivityReparentPosition = this.mTargetTask.getChildCount();
                }
                processResultActivities(r, this.mTargetTask, this.mActivityReparentPosition, false, false);
                if (r.info.launchMode == 1 && (p = this.mTargetTask.getActivityBelow(r)) != null && p.intent.getComponent().equals(r.intent.getComponent())) {
                    p.finishIfPossible("replace", false);
                }
            }
            return false;
        }
    }

    private void finishActivities(ArrayList<ActivityRecord> activities, String reason) {
        boolean noOptions = this.mCanMoveOptions;
        while (!activities.isEmpty()) {
            ActivityRecord p = activities.remove(0);
            if (!p.finishing) {
                noOptions = takeOption(p, noOptions);
                if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                    String protoLogParam0 = String.valueOf(p);
                    ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_TASKS, -1704402370, 0, (String) null, new Object[]{protoLogParam0});
                }
                p.finishIfPossible(reason, false);
            }
        }
    }

    private void processResultActivities(ActivityRecord target, Task targetTask, int position, boolean ignoreFinishing, boolean takeOptions) {
        boolean noOptions = this.mCanMoveOptions;
        while (!this.mResultActivities.isEmpty()) {
            ActivityRecord p = this.mResultActivities.remove(0);
            if (!ignoreFinishing || !p.finishing) {
                if (takeOptions) {
                    noOptions = takeOption(p, noOptions);
                }
                if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
                    String protoLogParam0 = String.valueOf(p);
                    String protoLogParam1 = String.valueOf(this.mTask);
                    String protoLogParam2 = String.valueOf(targetTask);
                    String protoLogParam3 = String.valueOf(Debug.getCallers(4));
                    ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, -1638958146, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1, protoLogParam2, protoLogParam3});
                }
                if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                    String protoLogParam02 = String.valueOf(p);
                    String protoLogParam12 = String.valueOf(target);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_TASKS, -1198579104, 0, (String) null, new Object[]{protoLogParam02, protoLogParam12});
                }
                p.reparent(targetTask, position, "resetTargetTaskIfNeeded");
            }
        }
    }

    private void processPendingReparentActivities() {
        Task task;
        if (this.mPendingReparentActivities.isEmpty()) {
            return;
        }
        ActivityTaskManagerService atmService = this.mTargetRootTask.mAtmService;
        TaskDisplayArea taskDisplayArea = this.mTargetRootTask.getDisplayArea();
        int windowingMode = this.mTargetRootTask.getWindowingMode();
        int activityType = this.mTargetRootTask.getActivityType();
        while (!this.mPendingReparentActivities.isEmpty()) {
            ActivityRecord r = this.mPendingReparentActivities.remove(0);
            boolean alwaysCreateTask = DisplayContent.alwaysCreateRootTask(windowingMode, activityType);
            if (!alwaysCreateTask) {
                task = this.mTargetRootTask.getBottomMostTask();
            } else {
                task = taskDisplayArea.getBottomMostTask();
            }
            Task targetTask = null;
            if (task != null && r.taskAffinity.equals(task.affinity)) {
                targetTask = task;
                if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                    String protoLogParam0 = String.valueOf(r);
                    String protoLogParam1 = String.valueOf(targetTask);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_TASKS, -877494781, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
                }
            }
            if (targetTask == null) {
                if (!alwaysCreateTask) {
                    targetTask = this.mTargetRootTask.reuseOrCreateTask(r.info, null, false);
                } else {
                    targetTask = taskDisplayArea.getOrCreateRootTask(windowingMode, activityType, false);
                }
                targetTask.affinityIntent = r.intent;
            }
            r.reparent(targetTask, 0, "resetTargetTaskIfNeeded");
            atmService.mTaskSupervisor.mRecentTasks.add(targetTask);
        }
    }

    private boolean takeOption(ActivityRecord p, boolean noOptions) {
        this.mCanMoveOptions = false;
        if (noOptions && this.mTopOptions == null) {
            ActivityOptions options = p.getOptions();
            this.mTopOptions = options;
            if (options != null) {
                p.clearOptionsAnimation();
                return false;
            }
            return noOptions;
        }
        return noOptions;
    }
}
