package com.android.server.wm;

import android.app.ActivityOptions;
import android.app.ThunderbackConfig;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.util.IntArray;
import android.util.Slog;
import android.view.RemoteAnimationTarget;
import android.view.SurfaceControl;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.internal.util.function.pooled.PooledPredicate;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.DisplayArea;
import com.android.server.wm.LaunchParamsController;
import com.android.server.wm.RemoteAnimationController;
import com.android.server.wm.Task;
import com.android.server.wm.WindowContainer;
import com.mediatek.server.wm.WmsExt;
import com.transsion.hubcore.media.ITranAudioSystemCallJni;
import com.transsion.hubcore.multiwindow.ITranMultiWindow;
import com.transsion.hubcore.server.wm.ITranTaskDisplayArea;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class TaskDisplayArea extends DisplayArea<WindowContainer> {
    boolean mAppTransitionReady;
    private ActivityTaskManagerService mAtmService;
    private int mBackgroundColor;
    private final boolean mCanHostHomeTask;
    private int mColorLayerCounter;
    final boolean mCreatedByOrganizer;
    private boolean mDisplayAreaHasEmpty;
    DisplayContent mDisplayContent;
    Task mLastFocusedRootTask;
    private int mLastLeafTaskToFrontId;
    Task mLaunchAdjacentFlagRootTask;
    private final ArrayList<LaunchRootTaskDef> mLaunchRootTasks;
    boolean mPendingToShow;
    Task mPreferredTopFocusableRootTask;
    private boolean mRemoved;
    private Task mRootHomeTask;
    private Task mRootPinnedTask;
    private ArrayList<OnRootTaskOrderChangedListener> mRootTaskOrderChangedCallbacks;
    private RootWindowContainer mRootWindowContainer;
    private SurfaceControl mSplitScreenDividerAnchor;
    private final ArrayList<WindowContainer> mTmpAlwaysOnTopChildren;
    private final ArrayList<WindowContainer> mTmpHomeChildren;
    private final IntArray mTmpNeedsZBoostIndexes;
    private final ArrayList<WindowContainer> mTmpNormalChildren;
    private ArrayList<Task> mTmpTasks;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface OnRootTaskOrderChangedListener {
        void onRootTaskOrderChanged(Task task);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class LaunchRootTaskDef {
        int[] activityTypes;
        Task task;
        int[] windowingModes;

        private LaunchRootTaskDef() {
        }

        boolean contains(int windowingMode, int activityType) {
            return ArrayUtils.contains(this.windowingModes, windowingMode) && ArrayUtils.contains(this.activityTypes, activityType);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskDisplayArea(DisplayContent displayContent, WindowManagerService service, String name, int displayAreaFeature) {
        this(displayContent, service, name, displayAreaFeature, false, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskDisplayArea(DisplayContent displayContent, WindowManagerService service, String name, int displayAreaFeature, boolean createdByOrganizer) {
        this(displayContent, service, name, displayAreaFeature, createdByOrganizer, true);
    }

    TaskDisplayArea(DisplayContent displayContent, WindowManagerService service, String name, int displayAreaFeature, boolean createdByOrganizer, boolean canHostHomeTask) {
        super(service, DisplayArea.Type.ANY, name, displayAreaFeature);
        this.mBackgroundColor = 0;
        this.mColorLayerCounter = 0;
        this.mTmpAlwaysOnTopChildren = new ArrayList<>();
        this.mTmpNormalChildren = new ArrayList<>();
        this.mTmpHomeChildren = new ArrayList<>();
        this.mTmpNeedsZBoostIndexes = new IntArray();
        this.mTmpTasks = new ArrayList<>();
        this.mDisplayAreaHasEmpty = true;
        this.mLaunchRootTasks = new ArrayList<>();
        this.mRootTaskOrderChangedCallbacks = new ArrayList<>();
        this.mDisplayContent = displayContent;
        this.mRootWindowContainer = service.mRoot;
        this.mAtmService = service.mAtmService;
        this.mCreatedByOrganizer = createdByOrganizer;
        this.mCanHostHomeTask = canHostHomeTask;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getRootTask(final int windowingMode, final int activityType) {
        if (activityType == 2) {
            return this.mRootHomeTask;
        }
        if (windowingMode == 2) {
            return this.mRootPinnedTask;
        }
        return getRootTask(new Predicate() { // from class: com.android.server.wm.TaskDisplayArea$$ExternalSyntheticLambda15
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return TaskDisplayArea.lambda$getRootTask$0(activityType, windowingMode, (Task) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getRootTask$0(int activityType, int windowingMode, Task rootTask) {
        if (activityType == 0 && windowingMode == rootTask.getWindowingMode()) {
            return true;
        }
        return rootTask.isCompatible(windowingMode, activityType);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getTopRootTask$1(Task t) {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getTopRootTask() {
        return getRootTask(new Predicate() { // from class: com.android.server.wm.TaskDisplayArea$$ExternalSyntheticLambda14
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return TaskDisplayArea.lambda$getTopRootTask$1((Task) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getRootHomeTask() {
        return this.mRootHomeTask;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getRootPinnedTask() {
        return this.mRootPinnedTask;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayList<Task> getVisibleTasks() {
        final ArrayList<Task> visibleTasks = new ArrayList<>();
        forAllTasks(new Consumer() { // from class: com.android.server.wm.TaskDisplayArea$$ExternalSyntheticLambda11
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                TaskDisplayArea.lambda$getVisibleTasks$2(visibleTasks, (Task) obj);
            }
        });
        return visibleTasks;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getVisibleTasks$2(ArrayList visibleTasks, Task task) {
        if (task.isLeafTask() && task.isVisible()) {
            visibleTasks.add(task);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onRootTaskWindowingModeChanged(Task rootTask) {
        removeRootTaskReferenceIfNeeded(rootTask);
        addRootTaskReferenceIfNeeded(rootTask);
        if (rootTask == this.mRootPinnedTask && getTopRootTask() != rootTask) {
            positionChildAt(Integer.MAX_VALUE, rootTask, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addRootTaskReferenceIfNeeded(Task rootTask) {
        if (rootTask.isActivityTypeHome()) {
            Task task = this.mRootHomeTask;
            if (task != null) {
                if (!rootTask.isDescendantOf(task)) {
                    throw new IllegalArgumentException("addRootTaskReferenceIfNeeded: root home task=" + this.mRootHomeTask + " already exist on display=" + this + " rootTask=" + rootTask);
                }
            } else {
                this.mRootHomeTask = rootTask;
            }
        }
        if (!rootTask.isRootTask()) {
            return;
        }
        int windowingMode = rootTask.getWindowingMode();
        if (windowingMode == 2) {
            if (this.mRootPinnedTask != null) {
                throw new IllegalArgumentException("addRootTaskReferenceIfNeeded: root pinned task=" + this.mRootPinnedTask + " already exist on display=" + this + " rootTask=" + rootTask);
            }
            this.mRootPinnedTask = rootTask;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeRootTaskReferenceIfNeeded(Task rootTask) {
        if (rootTask == this.mRootHomeTask) {
            this.mRootHomeTask = null;
        } else if (rootTask == this.mRootPinnedTask) {
            this.mRootPinnedTask = null;
        }
    }

    @Override // com.android.server.wm.WindowContainer
    void setInitialSurfaceControlProperties(SurfaceControl.Builder b) {
        b.setEffectLayer();
        super.setInitialSurfaceControlProperties(b);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void addChild(WindowContainer child, int position) {
        if (child.asTaskDisplayArea() != null) {
            if (WindowManagerDebugConfig.DEBUG_ROOT_TASK) {
                Slog.d(WmsExt.TAG, "Set TaskDisplayArea=" + child + " on taskDisplayArea=" + this);
            }
            super.addChild((TaskDisplayArea) child, position);
        } else if (child.asTask() != null) {
            addChildTask(child.asTask(), position);
        } else {
            throw new IllegalArgumentException("TaskDisplayArea can only add Task and TaskDisplayArea, but found " + child);
        }
    }

    private void addChildTask(Task task, int position) {
        if (WindowManagerDebugConfig.DEBUG_ROOT_TASK) {
            Slog.d(WmsExt.TAG, "Set task=" + task + " on taskDisplayArea=" + this);
        }
        addRootTaskReferenceIfNeeded(task);
        int position2 = findPositionForRootTask(position, task, true);
        ITranTaskDisplayArea.Instance().hookChildSizeChange(this.mDisplayContent.isSourceConnectDisplay(), this.mChildren.size(), getDisplayId(), true);
        super.addChild((TaskDisplayArea) task, position2);
        hookDisplayAreaChildCountInAddChildTask();
        if (this.mPreferredTopFocusableRootTask != null && task.isFocusable() && this.mPreferredTopFocusableRootTask.compareTo((WindowContainer) task) < 0) {
            this.mPreferredTopFocusableRootTask = null;
        }
        this.mAtmService.updateSleepIfNeededLocked();
        onRootTaskOrderChanged(task);
    }

    @Override // com.android.server.wm.WindowContainer
    protected void removeChild(WindowContainer child) {
        if (child.asTaskDisplayArea() != null) {
            super.removeChild(child);
        } else if (child.asTask() != null) {
            removeChildTask(child.asTask());
        } else {
            throw new IllegalArgumentException("TaskDisplayArea can only remove Task and TaskDisplayArea, but found " + child);
        }
    }

    private void removeChildTask(Task task) {
        super.removeChild(task);
        hookDisplayAreaChildCountInremoveChildTask();
        ITranTaskDisplayArea.Instance().hookChildSizeChange(this.mDisplayContent.isSourceConnectDisplay(), this.mChildren.size(), getDisplayId(), true);
        onRootTaskRemoved(task);
        this.mAtmService.updateSleepIfNeededLocked();
        removeRootTaskReferenceIfNeeded(task);
    }

    @Override // com.android.server.wm.WindowContainer
    boolean isOnTop() {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer
    public void positionChildAt(int position, WindowContainer child, boolean includingParents) {
        if (child.asTaskDisplayArea() != null) {
            super.positionChildAt(position, child, includingParents);
        } else if (child.asTask() != null) {
            positionChildTaskAt(position, child.asTask(), includingParents);
        } else {
            throw new IllegalArgumentException("TaskDisplayArea can only position Task and TaskDisplayArea, but found " + child);
        }
    }

    private void positionChildTaskAt(int position, Task child, boolean includingParents) {
        boolean moveToTop = position >= getChildCount() - 1;
        boolean moveToBottom = position <= 0;
        int oldPosition = this.mChildren.indexOf(child);
        ITranTaskDisplayArea.Instance().hookChildSizeChange(this.mDisplayContent.isSourceConnectDisplay(), this.mChildren.size(), getDisplayId(), true);
        if (child.isAlwaysOnTop() && !moveToTop) {
            Slog.w(WmsExt.TAG, "Ignoring move of always-on-top root task=" + this + " to bottom");
            super.positionChildAt(oldPosition, child, false);
            hookDisplayAreaChildCountInPositionChildTaskAt(moveToBottom, oldPosition);
            return;
        }
        if ((!this.mDisplayContent.isTrusted() || this.mDisplayContent.mDontMoveToTop) && !getParent().isOnTop()) {
            includingParents = false;
        }
        int targetPosition = findPositionForRootTask(position, child, false);
        super.positionChildAt(targetPosition, child, false);
        hookDisplayAreaChildCountInPositionChildTaskAt(moveToBottom, oldPosition);
        if (includingParents && getParent() != null && (moveToTop || moveToBottom)) {
            getParent().positionChildAt(moveToTop ? Integer.MAX_VALUE : Integer.MIN_VALUE, this, true);
        }
        child.updateTaskMovement(moveToTop, targetPosition);
        this.mDisplayContent.layoutAndAssignWindowLayersIfNeeded();
        if (moveToTop && child.isFocusableAndVisible()) {
            this.mPreferredTopFocusableRootTask = child;
        } else if (this.mPreferredTopFocusableRootTask == child) {
            this.mPreferredTopFocusableRootTask = null;
        }
        this.mAtmService.mTaskSupervisor.updateTopResumedActivityIfNeeded();
        ActivityRecord r = child.getTopResumedActivity();
        if (r != null && r == this.mRootWindowContainer.getTopResumedActivity()) {
            this.mAtmService.setResumedActivityUncheckLocked(r, "positionChildAt");
        }
        if (this.mChildren.indexOf(child) != oldPosition) {
            onRootTaskOrderChanged(child);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onLeafTaskRemoved(int taskId) {
        if (this.mLastLeafTaskToFrontId == taskId) {
            this.mLastLeafTaskToFrontId = -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onLeafTaskMoved(Task t, boolean toTop) {
        if (!toTop) {
            if (t.mTaskId == this.mLastLeafTaskToFrontId) {
                this.mLastLeafTaskToFrontId = -1;
                ActivityRecord topMost = getTopMostActivity();
                if (topMost != null) {
                    this.mAtmService.getTaskChangeNotificationController().notifyTaskMovedToFront(topMost.getTask().getTaskInfo());
                }
            }
        } else if (t.mTaskId == this.mLastLeafTaskToFrontId || t.topRunningActivityLocked() == null) {
        } else {
            this.mLastLeafTaskToFrontId = t.mTaskId;
            EventLogTags.writeWmTaskToFront(t.mUserId, t.mTaskId);
            this.mAtmService.getTaskChangeNotificationController().notifyTaskMovedToFront(t.getTaskInfo());
        }
    }

    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer
    void onChildPositionChanged(WindowContainer child) {
        super.onChildPositionChanged(child);
        this.mRootWindowContainer.invalidateTaskLayers();
    }

    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer
    boolean forAllTaskDisplayAreas(Predicate<TaskDisplayArea> callback, boolean traverseTopToBottom) {
        return traverseTopToBottom ? super.forAllTaskDisplayAreas(callback, traverseTopToBottom) || callback.test(this) : callback.test(this) || super.forAllTaskDisplayAreas(callback, traverseTopToBottom);
    }

    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer
    void forAllTaskDisplayAreas(Consumer<TaskDisplayArea> callback, boolean traverseTopToBottom) {
        if (traverseTopToBottom) {
            super.forAllTaskDisplayAreas(callback, traverseTopToBottom);
            callback.accept(this);
            return;
        }
        callback.accept(this);
        super.forAllTaskDisplayAreas(callback, traverseTopToBottom);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: java.util.function.BiFunction<com.android.server.wm.TaskDisplayArea, R, R> */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer
    <R> R reduceOnAllTaskDisplayAreas(BiFunction<TaskDisplayArea, R, R> accumulator, R initValue, boolean traverseTopToBottom) {
        if (traverseTopToBottom) {
            return (R) accumulator.apply(this, super.reduceOnAllTaskDisplayAreas(accumulator, initValue, traverseTopToBottom));
        }
        R result = accumulator.apply(this, initValue);
        return (R) super.reduceOnAllTaskDisplayAreas(accumulator, result, traverseTopToBottom);
    }

    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer
    <R> R getItemFromTaskDisplayAreas(Function<TaskDisplayArea, R> callback, boolean traverseTopToBottom) {
        if (traverseTopToBottom) {
            R item = (R) super.getItemFromTaskDisplayAreas(callback, traverseTopToBottom);
            return item != null ? item : callback.apply(this);
        }
        R item2 = callback.apply(this);
        if (item2 != null) {
            return item2;
        }
        return (R) super.getItemFromTaskDisplayAreas(callback, traverseTopToBottom);
    }

    /* JADX WARN: Type inference failed for: r1v1, types: [com.android.server.wm.WindowContainer] */
    private int getPriority(WindowContainer child) {
        TaskDisplayArea tda = child.asTaskDisplayArea();
        if (tda != null) {
            return tda.getPriority(tda.getTopChild());
        }
        Task rootTask = child.asTask();
        if (this.mWmService.mAssistantOnTopOfDream && rootTask.isActivityTypeAssistant()) {
            return 4;
        }
        if (rootTask.isActivityTypeDream()) {
            return 3;
        }
        if (rootTask.inPinnedWindowingMode()) {
            return 2;
        }
        return rootTask.isAlwaysOnTop() ? 1 : 0;
    }

    private int findMinPositionForRootTask(Task rootTask) {
        int currentIndex;
        int minPosition = Integer.MIN_VALUE;
        for (int i = 0; i < this.mChildren.size() && getPriority((WindowContainer) this.mChildren.get(i)) < getPriority(rootTask); i++) {
            minPosition = i;
        }
        if (rootTask.isAlwaysOnTop() && (currentIndex = this.mChildren.indexOf(rootTask)) > minPosition) {
            return currentIndex;
        }
        return minPosition;
    }

    private int findMaxPositionForRootTask(Task rootTask) {
        int i = this.mChildren.size() - 1;
        while (true) {
            if (i < 0) {
                return 0;
            }
            WindowContainer curr = (WindowContainer) this.mChildren.get(i);
            boolean sameRootTask = curr == rootTask;
            if (getPriority(curr) > getPriority(rootTask) || sameRootTask) {
                i--;
            } else {
                return i;
            }
        }
    }

    private int findPositionForRootTask(int requestedPosition, Task rootTask, boolean adding) {
        int maxPosition = findMaxPositionForRootTask(rootTask);
        int minPosition = findMinPositionForRootTask(rootTask);
        if (requestedPosition == Integer.MAX_VALUE) {
            requestedPosition = this.mChildren.size();
        } else if (requestedPosition == Integer.MIN_VALUE) {
            requestedPosition = 0;
        }
        int targetPosition = Math.max(Math.min(requestedPosition, maxPosition), minPosition);
        int prevPosition = this.mChildren.indexOf(rootTask);
        if (targetPosition != requestedPosition) {
            if (adding || targetPosition < prevPosition) {
                return targetPosition + 1;
            }
            return targetPosition;
        }
        return targetPosition;
    }

    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer
    int getOrientation(final int candidate) {
        Task task;
        this.mLastOrientationSource = null;
        if (getIgnoreOrientationRequest()) {
            return -2;
        }
        if (!canSpecifyOrientation()) {
            return ((Integer) reduceOnAllTaskDisplayAreas(new BiFunction() { // from class: com.android.server.wm.TaskDisplayArea$$ExternalSyntheticLambda8
                @Override // java.util.function.BiFunction
                public final Object apply(Object obj, Object obj2) {
                    return TaskDisplayArea.this.m8331lambda$getOrientation$3$comandroidserverwmTaskDisplayArea(candidate, (TaskDisplayArea) obj, (Integer) obj2);
                }
            }, -2)).intValue();
        }
        Task nonFloatingTopTask = getTask(new Predicate() { // from class: com.android.server.wm.TaskDisplayArea$$ExternalSyntheticLambda9
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return TaskDisplayArea.lambda$getOrientation$4((Task) obj);
            }
        });
        if (nonFloatingTopTask != null && (task = nonFloatingTopTask.getCreatedByOrganizerTask()) != null && task.getAdjacentTaskFragment() != null && task.isVisible()) {
            return -1;
        }
        int orientation = super.getOrientation(candidate);
        if (orientation != -2 && orientation != 3) {
            if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                long protoLogParam0 = orientation;
                long protoLogParam1 = this.mDisplayContent.mDisplayId;
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, 1381227466, 5, (String) null, new Object[]{Long.valueOf(protoLogParam0), Long.valueOf(protoLogParam1)});
            }
            return orientation;
        }
        if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
            long protoLogParam02 = this.mDisplayContent.getLastOrientation();
            long protoLogParam12 = this.mDisplayContent.mDisplayId;
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, 1640436199, 5, (String) null, new Object[]{Long.valueOf(protoLogParam02), Long.valueOf(protoLogParam12)});
        }
        return this.mDisplayContent.getLastOrientation();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getOrientation$3$com-android-server-wm-TaskDisplayArea  reason: not valid java name */
    public /* synthetic */ Integer m8331lambda$getOrientation$3$comandroidserverwmTaskDisplayArea(int candidate, TaskDisplayArea taskDisplayArea, Integer orientation) {
        if (taskDisplayArea == this || orientation.intValue() != -2) {
            return orientation;
        }
        return Integer.valueOf(taskDisplayArea.getOrientation(candidate));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getOrientation$4(Task t) {
        return !t.getWindowConfiguration().tasksAreFloating();
    }

    @Override // com.android.server.wm.WindowContainer
    void assignChildLayers(SurfaceControl.Transaction t) {
        assignRootTaskOrdering(t);
        for (int i = 0; i < this.mChildren.size(); i++) {
            ((WindowContainer) this.mChildren.get(i)).assignChildLayers(t);
        }
    }

    void assignRootTaskOrdering(SurfaceControl.Transaction t) {
        if (getParent() == null) {
            return;
        }
        this.mTmpAlwaysOnTopChildren.clear();
        this.mTmpHomeChildren.clear();
        this.mTmpNormalChildren.clear();
        for (int i = 0; i < this.mChildren.size(); i++) {
            WindowContainer child = (WindowContainer) this.mChildren.get(i);
            TaskDisplayArea childTda = child.asTaskDisplayArea();
            if (childTda != null) {
                Task childTdaTopRootTask = childTda.getTopRootTask();
                if (childTdaTopRootTask == null) {
                    this.mTmpNormalChildren.add(childTda);
                } else if (childTdaTopRootTask.isAlwaysOnTop()) {
                    this.mTmpAlwaysOnTopChildren.add(childTda);
                } else if (childTdaTopRootTask.isActivityTypeHome()) {
                    this.mTmpHomeChildren.add(childTda);
                } else {
                    this.mTmpNormalChildren.add(childTda);
                }
            } else {
                Task childTask = child.asTask();
                if (childTask.isAlwaysOnTop()) {
                    this.mTmpAlwaysOnTopChildren.add(childTask);
                } else if (childTask.isActivityTypeHome()) {
                    this.mTmpHomeChildren.add(childTask);
                } else {
                    this.mTmpNormalChildren.add(childTask);
                }
            }
        }
        int layer = adjustRootTaskLayer(t, this.mTmpHomeChildren, 0);
        adjustRootTaskLayer(t, this.mTmpAlwaysOnTopChildren, Math.max(adjustRootTaskLayer(t, this.mTmpNormalChildren, layer), (int) EventLogTags.WM_FINISH_ACTIVITY));
        t.setLayer(this.mSplitScreenDividerAnchor, 30000);
    }

    private int adjustRootTaskLayer(SurfaceControl.Transaction t, ArrayList<WindowContainer> children, int startLayer) {
        boolean childNeedsZBoost;
        this.mTmpNeedsZBoostIndexes.clear();
        int childCount = children.size();
        boolean hasAdjacentTask = false;
        for (int i = 0; i < childCount; i++) {
            WindowContainer child = children.get(i);
            TaskDisplayArea childTda = child.asTaskDisplayArea();
            if (childTda != null) {
                childNeedsZBoost = childTda.childrenNeedZBoost();
            } else {
                childNeedsZBoost = child.needsZBoost();
            }
            if (childNeedsZBoost) {
                this.mTmpNeedsZBoostIndexes.add(i);
            } else {
                Task childTask = child.asTask();
                boolean inAdjacentTask = (childTask == null || !child.inMultiWindowMode() || childTask.getRootTask().getAdjacentTaskFragment() == null) ? false : true;
                if (inAdjacentTask) {
                    hasAdjacentTask = true;
                } else if (hasAdjacentTask && startLayer < 30000) {
                    startLayer = EventLogTags.WM_FINISH_ACTIVITY;
                }
                child.assignLayer(t, startLayer);
                startLayer++;
            }
        }
        int zBoostSize = this.mTmpNeedsZBoostIndexes.size();
        int i2 = 0;
        while (i2 < zBoostSize) {
            children.get(this.mTmpNeedsZBoostIndexes.get(i2)).assignLayer(t, startLayer);
            i2++;
            startLayer++;
        }
        return startLayer;
    }

    private boolean childrenNeedZBoost() {
        final boolean[] needsZBoost = new boolean[1];
        forAllRootTasks(new Consumer() { // from class: com.android.server.wm.TaskDisplayArea$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                TaskDisplayArea.lambda$childrenNeedZBoost$5(needsZBoost, (Task) obj);
            }
        });
        return needsZBoost[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$childrenNeedZBoost$5(boolean[] needsZBoost, Task task) {
        needsZBoost[0] = needsZBoost[0] | task.needsZBoost();
    }

    @Override // com.android.server.wm.WindowContainer
    RemoteAnimationTarget createRemoteAnimationTarget(RemoteAnimationController.RemoteAnimationRecord record) {
        ActivityRecord activity = getTopMostActivity();
        if (activity != null) {
            return activity.createRemoteAnimationTarget(record);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceControl getSplitScreenDividerAnchor() {
        return this.mSplitScreenDividerAnchor;
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    void onParentChanged(ConfigurationContainer newParent, ConfigurationContainer oldParent) {
        if (getParent() != null) {
            super.onParentChanged(newParent, oldParent, new WindowContainer.PreAssignChildLayersCallback() { // from class: com.android.server.wm.TaskDisplayArea$$ExternalSyntheticLambda4
                @Override // com.android.server.wm.WindowContainer.PreAssignChildLayersCallback
                public final void onPreAssignChildLayers() {
                    TaskDisplayArea.this.m8333lambda$onParentChanged$6$comandroidserverwmTaskDisplayArea();
                }
            });
            return;
        }
        super.onParentChanged(newParent, oldParent);
        this.mWmService.mTransactionFactory.get().remove(this.mSplitScreenDividerAnchor).apply();
        this.mSplitScreenDividerAnchor = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onParentChanged$6$com-android-server-wm-TaskDisplayArea  reason: not valid java name */
    public /* synthetic */ void m8333lambda$onParentChanged$6$comandroidserverwmTaskDisplayArea() {
        this.mSplitScreenDividerAnchor = makeChildSurface(null).setName("splitScreenDividerAnchor").setCallsite("TaskDisplayArea.onParentChanged").build();
        getSyncTransaction().show(this.mSplitScreenDividerAnchor);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setBackgroundColor(int colorInt) {
        setBackgroundColor(colorInt, false);
    }

    void setBackgroundColor(int colorInt, boolean restore) {
        this.mBackgroundColor = colorInt;
        Color color = Color.valueOf(colorInt);
        if (!restore) {
            this.mColorLayerCounter++;
        }
        if (this.mSurfaceControl != null) {
            getPendingTransaction().setColor(this.mSurfaceControl, new float[]{color.red(), color.green(), color.blue()});
            scheduleAnimation();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearBackgroundColor() {
        int i = this.mColorLayerCounter - 1;
        this.mColorLayerCounter = i;
        if (i == 0 && this.mSurfaceControl != null) {
            getPendingTransaction().unsetColor(this.mSurfaceControl);
            scheduleAnimation();
        }
    }

    @Override // com.android.server.wm.WindowContainer
    void migrateToNewSurfaceControl(SurfaceControl.Transaction t) {
        super.migrateToNewSurfaceControl(t);
        if (this.mColorLayerCounter > 0) {
            setBackgroundColor(this.mBackgroundColor, true);
        }
        SurfaceControl surfaceControl = this.mSplitScreenDividerAnchor;
        if (surfaceControl == null) {
            return;
        }
        t.reparent(surfaceControl, this.mSurfaceControl);
        reassignLayer(t);
        scheduleAnimation();
    }

    void onRootTaskRemoved(Task rootTask) {
        if (ActivityTaskManagerDebugConfig.DEBUG_ROOT_TASK) {
            Slog.v(ActivityTaskManagerService.TAG_ROOT_TASK, "onRootTaskRemoved: detaching " + rootTask + " from displayId=" + this.mDisplayContent.mDisplayId);
        }
        if (this.mPreferredTopFocusableRootTask == rootTask) {
            this.mPreferredTopFocusableRootTask = null;
        }
        if (this.mLaunchAdjacentFlagRootTask == rootTask) {
            this.mLaunchAdjacentFlagRootTask = null;
        }
        this.mDisplayContent.releaseSelfIfNeeded();
        onRootTaskOrderChanged(rootTask);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void positionTaskBehindHome(Task task) {
        Task home = getOrCreateRootHomeTask();
        WindowContainer homeParent = home.getParent();
        Task homeParentTask = homeParent != null ? homeParent.asTask() : null;
        if (homeParentTask == null) {
            if (task.getParent() == this) {
                positionChildAt(Integer.MIN_VALUE, task, false);
            } else {
                task.reparent(this, false);
            }
        } else if (homeParentTask == task.getParent()) {
            homeParentTask.positionChildAtBottom(task);
        } else {
            task.reparent(homeParentTask, false, 2, false, false, "positionTaskBehindHome");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getOrCreateRootTask(int windowingMode, int activityType, boolean onTop) {
        return getOrCreateRootTask(windowingMode, activityType, onTop, null, null, null, 0);
    }

    Task getOrCreateRootTask(int windowingMode, int activityType, boolean onTop, Task candidateTask, Task sourceTask, ActivityOptions options, int launchFlags) {
        int resolvedWindowingMode = windowingMode == 0 ? getWindowingMode() : windowingMode;
        if (!DisplayContent.alwaysCreateRootTask(resolvedWindowingMode, activityType)) {
            Task rootTask = getRootTask(resolvedWindowingMode, activityType);
            if (rootTask != null) {
                return rootTask;
            }
        } else if (candidateTask != null) {
            int position = onTop ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            Task launchRootTask = getLaunchRootTask(resolvedWindowingMode, activityType, options, sourceTask, launchFlags, candidateTask);
            if (launchRootTask == null) {
                if (candidateTask.getDisplayArea() != this) {
                    if (candidateTask.getParent() == null) {
                        addChild(candidateTask, position);
                    } else {
                        candidateTask.reparent(this, onTop);
                    }
                }
            } else if (candidateTask.getParent() == null) {
                launchRootTask.addChild(candidateTask, position);
            } else if (candidateTask.getParent() != launchRootTask) {
                if (candidateTask == getRootPinnedTask() || candidateTask == getRootHomeTask()) {
                    Slog.d("Task", "candidateTask is pinned or home root task, no need reparent: " + candidateTask + ", launchRootTask is " + launchRootTask);
                    return candidateTask;
                }
                candidateTask.reparent(launchRootTask, position);
            }
            if (windowingMode != 0 && candidateTask.isRootTask() && candidateTask.getWindowingMode() != windowingMode) {
                candidateTask.setWindowingMode(windowingMode);
            }
            return candidateTask.getRootTask();
        }
        return new Task.Builder(this.mAtmService).setWindowingMode(windowingMode).setActivityType(activityType).setOnTop(onTop).setParent(this).setSourceTask(sourceTask).setActivityOptions(options).setLaunchFlags(launchFlags).build();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getOrCreateRootTask(ActivityRecord r, ActivityOptions options, Task candidateTask, Task sourceTask, LaunchParamsController.LaunchParams launchParams, int launchFlags, int activityType, boolean onTop) {
        int windowingMode = 0;
        if (launchParams != null) {
            windowingMode = launchParams.mWindowingMode;
        } else if (options != null) {
            windowingMode = options.getLaunchWindowingMode();
        }
        return getOrCreateRootTask(validateWindowingMode(windowingMode, r, candidateTask), activityType, onTop, candidateTask, sourceTask, options, launchFlags);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getNextRootTaskId() {
        return this.mAtmService.mTaskSupervisor.getNextTaskIdForUser();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task createRootTask(int windowingMode, int activityType, boolean onTop) {
        return createRootTask(windowingMode, activityType, onTop, null);
    }

    Task createRootTask(int windowingMode, int activityType, boolean onTop, ActivityOptions opts) {
        return new Task.Builder(this.mAtmService).setWindowingMode(windowingMode).setActivityType(activityType).setParent(this).setOnTop(onTop).setActivityOptions(opts).build();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLaunchRootTask(Task rootTask, int[] windowingModes, int[] activityTypes) {
        if (!rootTask.mCreatedByOrganizer) {
            throw new IllegalArgumentException("Can't set not mCreatedByOrganizer as launch root tr=" + rootTask);
        }
        LaunchRootTaskDef def = getLaunchRootTaskDef(rootTask);
        if (def != null) {
            this.mLaunchRootTasks.remove(def);
        } else {
            def = new LaunchRootTaskDef();
            def.task = rootTask;
        }
        def.activityTypes = activityTypes;
        def.windowingModes = windowingModes;
        if (!ArrayUtils.isEmpty(windowingModes) || !ArrayUtils.isEmpty(activityTypes)) {
            this.mLaunchRootTasks.add(def);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeLaunchRootTask(Task rootTask) {
        LaunchRootTaskDef def = getLaunchRootTaskDef(rootTask);
        if (def != null) {
            this.mLaunchRootTasks.remove(def);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLaunchAdjacentFlagRootTask(Task adjacentFlagRootTask) {
        if (adjacentFlagRootTask != null) {
            if (!adjacentFlagRootTask.mCreatedByOrganizer) {
                throw new IllegalArgumentException("Can't set not mCreatedByOrganizer as launch adjacent flag root tr=" + adjacentFlagRootTask);
            }
            if (adjacentFlagRootTask.getAdjacentTaskFragment() == null) {
                throw new UnsupportedOperationException("Can't set non-adjacent root as launch adjacent flag root tr=" + adjacentFlagRootTask);
            }
        }
        this.mLaunchAdjacentFlagRootTask = adjacentFlagRootTask;
    }

    private LaunchRootTaskDef getLaunchRootTaskDef(Task rootTask) {
        for (int i = this.mLaunchRootTasks.size() - 1; i >= 0; i--) {
            if (this.mLaunchRootTasks.get(i).task.mTaskId == rootTask.mTaskId) {
                LaunchRootTaskDef def = this.mLaunchRootTasks.get(i);
                return def;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getLaunchRootTask(int windowingMode, int activityType, ActivityOptions options, Task sourceTask, int launchFlags) {
        return getLaunchRootTask(windowingMode, activityType, options, sourceTask, launchFlags, null);
    }

    Task getLaunchRootTask(int windowingMode, int activityType, ActivityOptions options, Task sourceTask, int launchFlags, Task candidateTask) {
        Task launchTarget;
        Task candidateRoot;
        Task task;
        Task task2;
        Task launchRootTask;
        if (options != null && (launchRootTask = Task.fromWindowContainerToken(options.getLaunchRootTask())) != null && launchRootTask.mCreatedByOrganizer) {
            return launchRootTask;
        }
        if ((launchFlags & 4096) != 0 && (task = this.mLaunchAdjacentFlagRootTask) != null) {
            if (sourceTask != 0 && task.getAdjacentTaskFragment() != null && (sourceTask == (task2 = this.mLaunchAdjacentFlagRootTask) || sourceTask.isDescendantOf(task2))) {
                return this.mLaunchAdjacentFlagRootTask.getAdjacentTaskFragment().asTask();
            }
            return this.mLaunchAdjacentFlagRootTask;
        }
        int i = this.mLaunchRootTasks.size();
        while (true) {
            i--;
            if (i >= 0) {
                if (this.mLaunchRootTasks.get(i).contains(windowingMode, activityType)) {
                    Task launchRootTask2 = this.mLaunchRootTasks.get(i).task;
                    TaskFragment adjacentTaskFragment = launchRootTask2 != null ? launchRootTask2.getAdjacentTaskFragment() : null;
                    Task adjacentRootTask = adjacentTaskFragment != null ? adjacentTaskFragment.asTask() : null;
                    if (sourceTask != null && adjacentRootTask != null && (sourceTask == adjacentRootTask || sourceTask.isDescendantOf(adjacentRootTask))) {
                        return adjacentRootTask;
                    }
                    return launchRootTask2;
                }
            } else if (sourceTask == null || (launchTarget = sourceTask.getCreatedByOrganizerTask()) == null || launchTarget.getAdjacentTaskFragment() == null) {
                return null;
            } else {
                if (candidateTask != null && (candidateRoot = candidateTask.getCreatedByOrganizerTask()) != null && candidateRoot != launchTarget && launchTarget == candidateRoot.getAdjacentTaskFragment()) {
                    return candidateRoot;
                }
                return launchTarget;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getFocusedRootTask() {
        Task task = this.mPreferredTopFocusableRootTask;
        if (task != null) {
            return task;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowContainer child = (WindowContainer) this.mChildren.get(i);
            if (child.asTaskDisplayArea() != null) {
                Task rootTask = child.asTaskDisplayArea().getFocusedRootTask();
                if (rootTask != null) {
                    return rootTask;
                }
            } else {
                Task rootTask2 = ((WindowContainer) this.mChildren.get(i)).asTask();
                if (rootTask2.isFocusableAndVisible()) {
                    return rootTask2;
                }
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getNextFocusableRootTask(Task currentFocus, boolean ignoreCurrent) {
        if (currentFocus != null) {
            currentFocus.getWindowingMode();
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowContainer child = (WindowContainer) this.mChildren.get(i);
            if (child.asTaskDisplayArea() != null) {
                Task rootTask = child.asTaskDisplayArea().getNextFocusableRootTask(currentFocus, ignoreCurrent);
                if (rootTask != null) {
                    return rootTask;
                }
            } else {
                Task rootTask2 = ((WindowContainer) this.mChildren.get(i)).asTask();
                if ((!ignoreCurrent || rootTask2 != currentFocus) && rootTask2.isFocusableAndVisible()) {
                    return rootTask2;
                }
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getFocusedActivity() {
        Task focusedRootTask = getFocusedRootTask();
        if (focusedRootTask == null) {
            return null;
        }
        ActivityRecord resumedActivity = focusedRootTask.getTopResumedActivity();
        if (resumedActivity == null || resumedActivity.app == null) {
            ActivityRecord resumedActivity2 = focusedRootTask.getTopPausingActivity();
            if (resumedActivity2 == null || resumedActivity2.app == null) {
                return focusedRootTask.topRunningActivity(true);
            }
            return resumedActivity2;
        }
        return resumedActivity;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getLastFocusedRootTask() {
        return this.mLastFocusedRootTask;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateLastFocusedRootTask(Task prevFocusedTask, String updateLastFocusedTaskReason) {
        Task currentFocusedTask;
        if (updateLastFocusedTaskReason == null || (currentFocusedTask = getFocusedRootTask()) == prevFocusedTask) {
            return;
        }
        if (this.mDisplayContent.isSleeping()) {
            currentFocusedTask.clearLastPausedActivity();
        }
        this.mLastFocusedRootTask = prevFocusedTask;
        int i = this.mRootWindowContainer.mCurrentUser;
        int i2 = this.mDisplayContent.mDisplayId;
        int rootTaskId = currentFocusedTask == null ? -1 : currentFocusedTask.getRootTaskId();
        Task task = this.mLastFocusedRootTask;
        EventLogTags.writeWmFocusedRootTask(i, i2, rootTaskId, task != null ? task.getRootTaskId() : -1, updateLastFocusedTaskReason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean allResumedActivitiesComplete() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowContainer child = (WindowContainer) this.mChildren.get(i);
            if (child.asTaskDisplayArea() != null) {
                if (!child.asTaskDisplayArea().allResumedActivitiesComplete()) {
                    return false;
                }
            } else {
                ActivityRecord r = ((WindowContainer) this.mChildren.get(i)).asTask().getTopResumedActivity();
                if (r != null && !r.isState(ActivityRecord.State.RESUMED)) {
                    return false;
                }
            }
        }
        Task currentFocusedRootTask = getFocusedRootTask();
        if (ActivityTaskManagerDebugConfig.DEBUG_ROOT_TASK) {
            Slog.d(ActivityTaskManagerService.TAG_ROOT_TASK, "allResumedActivitiesComplete: currentFocusedRootTask changing from=" + this.mLastFocusedRootTask + " to=" + currentFocusedRootTask);
        }
        this.mLastFocusedRootTask = currentFocusedRootTask;
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean pauseBackTasks(final ActivityRecord resuming) {
        final int[] someActivityPaused = {0};
        forAllLeafTasks(new Consumer() { // from class: com.android.server.wm.TaskDisplayArea$$ExternalSyntheticLambda5
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                TaskDisplayArea.this.m8334lambda$pauseBackTasks$8$comandroidserverwmTaskDisplayArea(resuming, someActivityPaused, (Task) obj);
            }
        }, true);
        return someActivityPaused[0] > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$pauseBackTasks$8$com-android-server-wm-TaskDisplayArea  reason: not valid java name */
    public /* synthetic */ void m8334lambda$pauseBackTasks$8$comandroidserverwmTaskDisplayArea(final ActivityRecord resuming, final int[] someActivityPaused, Task leafTask) {
        if (!leafTask.isLeafTaskFragment()) {
            ActivityRecord top = topRunningActivity();
            ActivityRecord resumedActivity = leafTask.getResumedActivity();
            if (resumedActivity != null && top.getTaskFragment() != leafTask && leafTask.startPausing(false, resuming, "pauseBackTasks")) {
                someActivityPaused[0] = someActivityPaused[0] + 1;
            }
        }
        leafTask.forAllLeafTaskFragments(new Consumer() { // from class: com.android.server.wm.TaskDisplayArea$$ExternalSyntheticLambda7
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                TaskDisplayArea.lambda$pauseBackTasks$7(ActivityRecord.this, someActivityPaused, (TaskFragment) obj);
            }
        }, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$pauseBackTasks$7(ActivityRecord resuming, int[] someActivityPaused, TaskFragment taskFrag) {
        ActivityRecord resumedActivity = taskFrag.getResumedActivity();
        if (resumedActivity != null && !taskFrag.canBeResumed(resuming) && taskFrag.startPausing(false, resuming, "pauseBackTasks")) {
            someActivityPaused[0] = someActivityPaused[0] + 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isWindowingModeSupported(int windowingMode, boolean supportsMultiWindow, boolean supportsFreeform, boolean supportsPip) {
        if (windowingMode == 0 || windowingMode == 1) {
            return true;
        }
        if (!supportsMultiWindow) {
            return false;
        }
        if (windowingMode == 6) {
            return true;
        }
        if (!supportsFreeform && windowingMode == 5) {
            return false;
        }
        if (supportsPip || windowingMode != 2) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int resolveWindowingMode(ActivityRecord r, ActivityOptions options, Task task) {
        int windowingMode = options != null ? options.getLaunchWindowingMode() : 0;
        if (windowingMode == 0) {
            if (task != null) {
                windowingMode = task.getWindowingMode();
            }
            if (windowingMode == 0 && r != null) {
                windowingMode = r.getWindowingMode();
            }
            if (windowingMode == 0) {
                windowingMode = getWindowingMode();
            }
        }
        int windowingMode2 = validateWindowingMode(windowingMode, r, task);
        if (windowingMode2 != 0) {
            return windowingMode2;
        }
        return 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isValidWindowingMode(int windowingMode, ActivityRecord r, Task task) {
        boolean supportsMultiWindow = this.mAtmService.mSupportsMultiWindow;
        boolean supportsFreeform = this.mAtmService.mSupportsFreeformWindowManagement;
        boolean supportsPip = this.mAtmService.mSupportsPictureInPicture;
        if (supportsMultiWindow) {
            if (task != null) {
                supportsFreeform = task.supportsFreeformInDisplayArea(this);
                supportsMultiWindow = task.supportsMultiWindowInDisplayArea(this) || (windowingMode == 2 && supportsPip);
            } else if (r != null) {
                supportsFreeform = r.supportsFreeformInDisplayArea(this);
                supportsPip = r.supportsPictureInPicture();
                supportsMultiWindow = r.supportsMultiWindowInDisplayArea(this);
            }
        }
        return windowingMode != 0 && isWindowingModeSupported(windowingMode, supportsMultiWindow, supportsFreeform, supportsPip);
    }

    int validateWindowingMode(int windowingMode, ActivityRecord r, Task task) {
        if (!isValidWindowingMode(windowingMode, r, task)) {
            return 0;
        }
        return windowingMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean supportsNonResizableMultiWindow() {
        int configSupportsNonResizableMultiWindow = this.mAtmService.mSupportsNonResizableMultiWindow;
        if (this.mAtmService.mDevEnableNonResizableMultiWindow || configSupportsNonResizableMultiWindow == 1) {
            return true;
        }
        if (configSupportsNonResizableMultiWindow == -1) {
            return false;
        }
        return isLargeEnoughForMultiWindow();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean supportsActivityMinWidthHeightMultiWindow(int minWidth, int minHeight, ActivityInfo activityInfo) {
        int configRespectsActivityMinWidthHeightMultiWindow;
        if (activityInfo != null && !activityInfo.shouldCheckMinWidthHeightForMultiWindow()) {
            return true;
        }
        if ((minWidth <= 0 && minHeight <= 0) || (configRespectsActivityMinWidthHeightMultiWindow = this.mAtmService.mRespectsActivityMinWidthHeightMultiWindow) == -1) {
            return true;
        }
        if (configRespectsActivityMinWidthHeightMultiWindow == 0 && isLargeEnoughForMultiWindow()) {
            return true;
        }
        Configuration config = getConfiguration();
        int orientation = config.orientation;
        if (orientation == 2) {
            int maxSupportMinWidth = (int) (this.mAtmService.mMinPercentageMultiWindowSupportWidth * config.screenWidthDp * this.mDisplayContent.getDisplayMetrics().density);
            return minWidth <= maxSupportMinWidth;
        }
        int maxSupportMinHeight = (int) (this.mAtmService.mMinPercentageMultiWindowSupportHeight * config.screenHeightDp * this.mDisplayContent.getDisplayMetrics().density);
        return minHeight <= maxSupportMinHeight;
    }

    private boolean isLargeEnoughForMultiWindow() {
        return getConfiguration().smallestScreenWidthDp >= this.mAtmService.mLargeScreenSmallestScreenWidthDp;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTopRootTask(Task rootTask) {
        return rootTask == getTopRootTask();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord topRunningActivity() {
        return topRunningActivity(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord topRunningActivity(boolean considerKeyguardState) {
        ActivityRecord topRunning = null;
        Task focusedRootTask = getFocusedRootTask();
        if (focusedRootTask != null) {
            topRunning = focusedRootTask.topRunningActivity();
        }
        if (topRunning == null) {
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                WindowContainer child = (WindowContainer) this.mChildren.get(i);
                if (child.asTaskDisplayArea() != null) {
                    topRunning = child.asTaskDisplayArea().topRunningActivity(considerKeyguardState);
                    if (topRunning != null) {
                        break;
                    }
                } else {
                    Task rootTask = ((WindowContainer) this.mChildren.get(i)).asTask();
                    if (rootTask != focusedRootTask && rootTask.isTopActivityFocusable() && (topRunning = rootTask.topRunningActivity()) != null) {
                        break;
                    }
                }
            }
        }
        if (topRunning != null && considerKeyguardState && this.mRootWindowContainer.mTaskSupervisor.getKeyguardController().isKeyguardLocked(topRunning.getDisplayId()) && !topRunning.canShowWhenLocked()) {
            return null;
        }
        return topRunning;
    }

    protected int getRootTaskCount() {
        final int[] count = new int[1];
        forAllRootTasks(new Consumer() { // from class: com.android.server.wm.TaskDisplayArea$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                TaskDisplayArea.lambda$getRootTaskCount$9(count, (Task) obj);
            }
        });
        return count[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getRootTaskCount$9(int[] count, Task task) {
        count[0] = count[0] + 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getOrCreateRootHomeTask() {
        return getOrCreateRootHomeTask(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getOrCreateRootHomeTask(boolean onTop) {
        Task homeTask = getRootHomeTask();
        if (homeTask == null && canHostHomeTask()) {
            return createRootTask(0, 2, onTop);
        }
        return homeTask;
    }

    Task getTopRootTaskInWindowingMode(int windowingMode) {
        return getRootTask(windowingMode, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void moveHomeRootTaskToFront(String reason) {
        Task homeRootTask = getOrCreateRootHomeTask();
        if (homeRootTask != null) {
            homeRootTask.moveToFront(reason);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void moveHomeActivityToTop(String reason) {
        ActivityRecord top = getHomeActivity();
        if (top == null) {
            moveHomeRootTaskToFront(reason);
        } else {
            top.moveFocusableActivityToTop(reason);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getHomeActivity() {
        return getHomeActivityForUser(this.mRootWindowContainer.mCurrentUser);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getHomeActivityForUser(int userId) {
        Task rootHomeTask = getRootHomeTask();
        if (rootHomeTask == null) {
            return null;
        }
        PooledPredicate p = PooledLambda.obtainPredicate(new BiPredicate() { // from class: com.android.server.wm.TaskDisplayArea$$ExternalSyntheticLambda6
            @Override // java.util.function.BiPredicate
            public final boolean test(Object obj, Object obj2) {
                boolean isHomeActivityForUser;
                isHomeActivityForUser = TaskDisplayArea.isHomeActivityForUser((ActivityRecord) obj, ((Integer) obj2).intValue());
                return isHomeActivityForUser;
            }
        }, PooledLambda.__(ActivityRecord.class), Integer.valueOf(userId));
        ActivityRecord r = rootHomeTask.getActivity(p);
        p.recycle();
        return r;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isHomeActivityForUser(ActivityRecord r, int userId) {
        return r.isActivityTypeHome() && (userId == -1 || r.mUserId == userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void moveRootTaskBehindBottomMostVisibleRootTask(Task rootTask) {
        Task s;
        if (rootTask.shouldBeVisible(null)) {
            return;
        }
        rootTask.getParent().positionChildAt(Integer.MIN_VALUE, rootTask, false);
        boolean isRootTask = rootTask.isRootTask();
        int numRootTasks = isRootTask ? this.mChildren.size() : rootTask.getParent().getChildCount();
        for (int rootTaskNdx = 0; rootTaskNdx < numRootTasks; rootTaskNdx++) {
            if (isRootTask) {
                WindowContainer child = (WindowContainer) this.mChildren.get(rootTaskNdx);
                if (child.asTaskDisplayArea() != null) {
                    s = child.asTaskDisplayArea().getBottomMostVisibleRootTask(rootTask);
                } else {
                    s = child.asTask();
                }
            } else {
                s = rootTask.getParent().getChildAt(rootTaskNdx).asTask();
            }
            if (s != rootTask && s != null) {
                int winMode = s.getWindowingMode();
                boolean isValidWindowingMode = winMode == 1;
                if (s.shouldBeVisible(null) && isValidWindowingMode) {
                    int position = Math.max(0, rootTaskNdx - 1);
                    rootTask.getParent().positionChildAt(position, rootTask, false);
                    return;
                }
            }
        }
    }

    private Task getBottomMostVisibleRootTask(Task excludeRootTask) {
        return getRootTask(new Predicate() { // from class: com.android.server.wm.TaskDisplayArea$$ExternalSyntheticLambda1
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return TaskDisplayArea.lambda$getBottomMostVisibleRootTask$10((Task) obj);
            }
        }, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getBottomMostVisibleRootTask$10(Task task) {
        int winMode = task.getWindowingMode();
        boolean isValidWindowingMode = winMode == 1;
        return task.shouldBeVisible(null) && isValidWindowingMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void moveRootTaskBehindRootTask(Task rootTask, Task behindRootTask) {
        WindowContainer parent;
        if (behindRootTask == null || behindRootTask == rootTask || (parent = rootTask.getParent()) == null || parent != behindRootTask.getParent()) {
            return;
        }
        int rootTaskIndex = parent.mChildren.indexOf(rootTask);
        int behindRootTaskIndex = parent.mChildren.indexOf(behindRootTask);
        int insertIndex = rootTaskIndex <= behindRootTaskIndex ? behindRootTaskIndex - 1 : behindRootTaskIndex;
        int position = Math.max(0, insertIndex);
        parent.positionChildAt(position, rootTask, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasPinnedTask() {
        return getRootPinnedTask() != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Task getRootTaskAbove(Task rootTask) {
        WindowContainer wc = rootTask.getParent();
        int index = wc.mChildren.indexOf(rootTask) + 1;
        if (index < wc.mChildren.size()) {
            return (Task) wc.mChildren.get(index);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRootTaskVisible(int windowingMode) {
        Task rootTask = getTopRootTaskInWindowingMode(windowingMode);
        return rootTask != null && rootTask.isVisible();
    }

    void removeRootTask(Task rootTask) {
        removeChild(rootTask);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getDisplayId() {
        return this.mDisplayContent.getDisplayId();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRemoved() {
        return this.mRemoved;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerRootTaskOrderChangedListener(OnRootTaskOrderChangedListener listener) {
        if (!this.mRootTaskOrderChangedCallbacks.contains(listener)) {
            this.mRootTaskOrderChangedCallbacks.add(listener);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterRootTaskOrderChangedListener(OnRootTaskOrderChangedListener listener) {
        this.mRootTaskOrderChangedCallbacks.remove(listener);
    }

    void onRootTaskOrderChanged(Task rootTask) {
        for (int i = this.mRootTaskOrderChangedCallbacks.size() - 1; i >= 0; i--) {
            this.mRootTaskOrderChangedCallbacks.get(i).onRootTaskOrderChanged(rootTask);
        }
    }

    @Override // com.android.server.wm.WindowContainer
    boolean canCreateRemoteAnimationTarget() {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canHostHomeTask() {
        return this.mDisplayContent.supportsSystemDecorations() && this.mCanHostHomeTask;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void ensureActivitiesVisible(final ActivityRecord starting, final int configChanges, final boolean preserveWindows, final boolean notifyClients) {
        this.mAtmService.mTaskSupervisor.beginActivityVisibilityUpdate();
        try {
            forAllRootTasks(new Consumer() { // from class: com.android.server.wm.TaskDisplayArea$$ExternalSyntheticLambda2
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((Task) obj).ensureActivitiesVisible(ActivityRecord.this, configChanges, preserveWindows, notifyClients);
                }
            });
        } finally {
            this.mAtmService.mTaskSupervisor.endActivityVisibilityUpdate();
        }
    }

    @Override // com.android.server.wm.WindowContainer
    boolean showSurfaceOnCreation() {
        if (this.mCreatedByOrganizer && getName() != null && getName().contains("multi_display")) {
            return false;
        }
        return super.showSurfaceOnCreation();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Type inference failed for: r1v11 */
    /* JADX WARN: Type inference failed for: r1v12, types: [int, boolean] */
    /* JADX WARN: Type inference failed for: r1v13 */
    public Task remove() {
        boolean z;
        ?? r1;
        WindowContainer launchRoot;
        this.mPreferredTopFocusableRootTask = null;
        boolean destroyContentOnRemoval = this.mDisplayContent.shouldDestroyContentOnRemove();
        TaskDisplayArea toDisplayArea = this.mRootWindowContainer.getDefaultTaskDisplayArea();
        int numRootTasks = this.mChildren.size();
        hookMakeChildrenInvisible();
        Task lastReparentedRootTask = null;
        int numRootTasks2 = numRootTasks;
        int i = 0;
        while (i < numRootTasks2) {
            WindowContainer child = (WindowContainer) this.mChildren.get(i);
            if (child.asTaskDisplayArea() != null) {
                Task lastReparentedRootTask2 = child.asTaskDisplayArea().remove();
                lastReparentedRootTask = lastReparentedRootTask2;
            } else {
                Task task = ((WindowContainer) this.mChildren.get(i)).asTask();
                if (destroyContentOnRemoval) {
                    z = false;
                } else if (!task.isActivityTypeStandardOrUndefined()) {
                    z = false;
                } else if (task.mCreatedByOrganizer) {
                    z = false;
                } else {
                    if (task.supportsSplitScreenWindowingModeInDisplayArea(toDisplayArea)) {
                        r1 = 0;
                        launchRoot = toDisplayArea.getLaunchRootTask(task.getWindowingMode(), task.getActivityType(), null, null, 0);
                    } else {
                        r1 = 0;
                        launchRoot = null;
                    }
                    if (isMultiWindow()) {
                        task.mReparentFromMultiWindow = true;
                    }
                    hookTaskReparent(task, toDisplayArea, launchRoot);
                    task.setWindowingMode(r1);
                    task.mReparentFromMultiWindow = r1;
                    lastReparentedRootTask = task;
                    i -= numRootTasks2 - this.mChildren.size();
                    numRootTasks2 = this.mChildren.size();
                }
                task.remove(z, "removeTaskDisplayArea");
                i -= numRootTasks2 - this.mChildren.size();
                numRootTasks2 = this.mChildren.size();
            }
            i++;
        }
        if (lastReparentedRootTask != null && !lastReparentedRootTask.isRootTask()) {
            lastReparentedRootTask.getRootTask().moveToFront("display-removed");
        }
        this.mRemoved = true;
        clearAllChildrenResizeable();
        hookMuteAndClearMuteProcessList();
        return lastReparentedRootTask;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canSpecifyOrientation() {
        return !getIgnoreOrientationRequest() && this.mDisplayContent.getOrientationRequestingTaskDisplayArea() == this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearPreferredTopFocusableRootTask() {
        this.mPreferredTopFocusableRootTask = null;
    }

    @Override // com.android.server.wm.WindowContainer
    TaskDisplayArea getTaskDisplayArea() {
        return this;
    }

    @Override // com.android.server.wm.DisplayArea
    boolean isTaskDisplayArea() {
        return true;
    }

    @Override // com.android.server.wm.WindowContainer
    TaskDisplayArea asTaskDisplayArea() {
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer
    public void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        pw.println(prefix + "TaskDisplayArea " + getName());
        String doublePrefix = prefix + "  ";
        super.dump(pw, doublePrefix, dumpAll);
        if (this.mPreferredTopFocusableRootTask != null) {
            pw.println(doublePrefix + "mPreferredTopFocusableRootTask=" + this.mPreferredTopFocusableRootTask);
        }
        if (this.mLastFocusedRootTask != null) {
            pw.println(doublePrefix + "mLastFocusedRootTask=" + this.mLastFocusedRootTask);
        }
        String triplePrefix = doublePrefix + "  ";
        if (this.mLaunchRootTasks.size() > 0) {
            pw.println(doublePrefix + "mLaunchRootTasks:");
            for (int i = this.mLaunchRootTasks.size() - 1; i >= 0; i--) {
                LaunchRootTaskDef def = this.mLaunchRootTasks.get(i);
                pw.println(triplePrefix + Arrays.toString(def.activityTypes) + " " + Arrays.toString(def.windowingModes) + "  task=" + def.task);
            }
        }
        pw.println(doublePrefix + "Application tokens in top down Z order:");
        for (int index = getChildCount() - 1; index >= 0; index--) {
            WindowContainer child = getChildAt(index);
            if (child.asTaskDisplayArea() != null) {
                child.dump(pw, doublePrefix, dumpAll);
            } else {
                Task rootTask = child.asTask();
                pw.println(doublePrefix + "* " + rootTask);
                rootTask.dump(pw, triplePrefix, dumpAll);
            }
        }
    }

    private void hookDisplayAreaChildCountInAddChildTask() {
        Task topTask;
        if (isMultiWindow() && this.mChildren.size() != 0 && (topTask = getTopRootTask()) != null) {
            if (topTask.getTopNonFinishingActivity() != null) {
                String str = topTask.getTopNonFinishingActivity().packageName;
            }
            if (topTask.getActivityType() == 1) {
                if (this.mDisplayAreaHasEmpty) {
                    if (ThunderbackConfig.isVersion4()) {
                        ITranTaskDisplayArea.Instance().hookDisplayAreaChildCountV4(getMultiWindowingMode(), getMultiWindowingId(), this.mChildren.size());
                    } else {
                        ITranTaskDisplayArea.Instance().hookDisplayAreaChildCountV3(this.mDisplayAreaAppearedInfo.getTranMultiDisplayAreaId(), this.mChildren.size());
                    }
                    this.mDisplayAreaHasEmpty = false;
                    return;
                }
                return;
            }
            if (ThunderbackConfig.isVersion4()) {
                ITranTaskDisplayArea.Instance().hookDisplayAreaChildCountV4(getMultiWindowingMode(), getMultiWindowingId(), 0);
            } else {
                ITranTaskDisplayArea.Instance().hookDisplayAreaChildCountV3(this.mDisplayAreaAppearedInfo.getTranMultiDisplayAreaId(), 0);
            }
            this.mDisplayAreaHasEmpty = true;
        }
    }

    private void hookDisplayAreaChildCountInremoveChildTask() {
        if (isMultiWindow()) {
            if (this.mChildren.size() == 0) {
                if (ThunderbackConfig.isVersion4()) {
                    ITranTaskDisplayArea.Instance().hookDisplayAreaChildCountV4(getMultiWindowingMode(), getMultiWindowingId(), this.mChildren.size());
                } else {
                    ITranTaskDisplayArea.Instance().hookDisplayAreaChildCountV3(this.mDisplayAreaAppearedInfo.getTranMultiDisplayAreaId(), this.mChildren.size());
                }
                this.mDisplayAreaHasEmpty = true;
                return;
            }
            Task topTask = getTopRootTask();
            if (topTask.getActivityType() != 1) {
                if (ThunderbackConfig.isVersion4()) {
                    ITranTaskDisplayArea.Instance().hookDisplayAreaChildCountV4(getMultiWindowingMode(), getMultiWindowingId(), 0);
                } else {
                    ITranTaskDisplayArea.Instance().hookDisplayAreaChildCountV3(this.mDisplayAreaAppearedInfo.getTranMultiDisplayAreaId(), 0);
                }
                Slog.i(WmsExt.TAG, "taskDisplayArea toptask not standard task , so close");
                this.mDisplayAreaHasEmpty = true;
            }
        }
    }

    private void hookDisplayAreaChildCountInPositionChildTaskAt(boolean moveToBottom, int oldPosition) {
        if (isMultiWindow() && this.mChildren.size() != 0) {
            if (moveToBottom && oldPosition == 0 && this.mChildren.size() == 1) {
                Slog.i(WmsExt.TAG, "hookDisplayAreaChildCountInPositionChildTaskAt childSize == 0");
                if (ThunderbackConfig.isVersion4()) {
                    ITranTaskDisplayArea.Instance().hookDisplayAreaChildCountV4(getMultiWindowingMode(), getMultiWindowingId(), 0);
                    this.mDisplayAreaHasEmpty = true;
                    return;
                }
            }
            Task topTask = getTopRootTask();
            if (topTask.getTopNonFinishingActivity() != null) {
                String str = topTask.getTopNonFinishingActivity().packageName;
            }
            if (topTask.getActivityType() == 1) {
                if (this.mDisplayAreaHasEmpty) {
                    if (ThunderbackConfig.isVersion4()) {
                        ITranTaskDisplayArea.Instance().hookDisplayAreaChildCountV4(getMultiWindowingMode(), getMultiWindowingId(), this.mChildren.size());
                    } else {
                        ITranTaskDisplayArea.Instance().hookDisplayAreaChildCountV3(this.mDisplayAreaAppearedInfo.getTranMultiDisplayAreaId(), this.mChildren.size());
                    }
                    this.mDisplayAreaHasEmpty = false;
                    return;
                }
                return;
            }
            if (ThunderbackConfig.isVersion4()) {
                ITranTaskDisplayArea.Instance().hookDisplayAreaChildCountV4(getMultiWindowingMode(), getMultiWindowingId(), 0);
            } else {
                ITranTaskDisplayArea.Instance().hookDisplayAreaChildCountV3(this.mDisplayAreaAppearedInfo.getTranMultiDisplayAreaId(), 0);
            }
            this.mDisplayAreaHasEmpty = true;
        }
    }

    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer
    boolean onDescendantOrientationChanged(WindowContainer requestingContainer) {
        if (isMultiWindow()) {
            boolean isTopActivityAndThunderbackWindow = topRunningActivity() == requestingContainer;
            if (isTopActivityAndThunderbackWindow) {
                if (ThunderbackConfig.isVersion4()) {
                    ITranTaskDisplayArea.Instance().hookRequestedOrientationV4(getMultiWindowingMode(), getMultiWindowingId(), requestingContainer.getOrientation());
                } else {
                    ITranTaskDisplayArea.Instance().hookRequestedOrientationV3(this.mDisplayAreaAppearedInfo.getTranMultiDisplayAreaId(), requestingContainer.getOrientation());
                }
            }
        }
        boolean isTopActivityAndThunderbackWindow2 = super.onDescendantOrientationChanged(requestingContainer);
        return isTopActivityAndThunderbackWindow2;
    }

    private void makeChildrenInvisible() {
        int numRootTasks = this.mChildren.size();
        for (int i = 0; i < numRootTasks; i++) {
            WindowContainer child = (WindowContainer) this.mChildren.get(i);
            if (child.asTaskDisplayArea() == null) {
                Task task = ((WindowContainer) this.mChildren.get(i)).asTask();
                task.startPausing(false, null, "displayArea-removed");
                task.forAllActivities(new Consumer() { // from class: com.android.server.wm.TaskDisplayArea$$ExternalSyntheticLambda12
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((ActivityRecord) obj).makeInvisible();
                    }
                });
            }
        }
    }

    private void clearAllChildrenResizeable() {
        TaskDisplayArea defaultTaskDisplayArea = this.mRootWindowContainer.getDefaultTaskDisplayArea();
        int numRootTasks = defaultTaskDisplayArea.mChildren.size();
        for (int i = 0; i < numRootTasks; i++) {
            WindowContainer child = (WindowContainer) defaultTaskDisplayArea.mChildren.get(i);
            if (child.asTaskDisplayArea() == null) {
                Task task = ((WindowContainer) defaultTaskDisplayArea.mChildren.get(i)).asTask();
                task.forAllActivities(new Consumer() { // from class: com.android.server.wm.TaskDisplayArea$$ExternalSyntheticLambda10
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((ActivityRecord) obj).setForceResizeable(false);
                    }
                });
            }
        }
    }

    private void hookMakeChildrenInvisible() {
        if (isMultiWindow()) {
            makeChildrenInvisible();
        }
    }

    private void hookTaskReparent(Task task, TaskDisplayArea toDisplayArea, WindowContainer launchRoot) {
        if (isMultiWindow()) {
            task.reparent(launchRoot == null ? toDisplayArea : launchRoot, Integer.MIN_VALUE);
        } else {
            task.reparent(launchRoot == null ? toDisplayArea : launchRoot, Integer.MAX_VALUE);
        }
    }

    private void hookMuteAndClearMuteProcessList() {
        if (isMultiWindow()) {
            int multWinId = -999;
            if (ThunderbackConfig.isVersion4()) {
                multWinId = getConfiguration().windowConfiguration.getMultiWindowingId();
            }
            final int multiWindowId = multWinId;
            ITranMultiWindow.Instance().postExecute(new Runnable() { // from class: com.android.server.wm.TaskDisplayArea$$ExternalSyntheticLambda16
                @Override // java.lang.Runnable
                public final void run() {
                    TaskDisplayArea.this.m8332x73ef3051(multiWindowId);
                }
            }, 1500L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$hookMuteAndClearMuteProcessList$14$com-android-server-wm-TaskDisplayArea  reason: not valid java name */
    public /* synthetic */ void m8332x73ef3051(int multiWindowId) {
        ITranAudioSystemCallJni.Instance().clearCurrentAudioTrackMute(multiWindowId);
        ITranAudioSystemCallJni.Instance().setMuteState(false, multiWindowId);
        this.mAtmService.setMuteStateV4(false, multiWindowId);
        this.mAtmService.removeFromMuteState(multiWindowId);
        this.mAtmService.mAmInternal.clearMuteProcessList(multiWindowId);
    }

    @Override // com.android.server.wm.DisplayArea
    public boolean isMultiWindow() {
        return getConfiguration().windowConfiguration.isThunderbackWindow();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getTopVisibleRootTask() {
        return getRootTask(new Predicate() { // from class: com.android.server.wm.TaskDisplayArea$$ExternalSyntheticLambda13
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return TaskDisplayArea.lambda$getTopVisibleRootTask$15((Task) obj);
            }
        }, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getTopVisibleRootTask$15(Task task) {
        return (task == null || !task.isVisible() || 2 == task.getWindowingMode()) ? false : true;
    }
}
