package com.android.server.wm;

import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.RemoteException;
import android.os.Trace;
import android.util.Slog;
import android.view.IRecentsAnimationRunner;
import android.window.TaskSnapshot;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.internal.util.function.pooled.PooledPredicate;
import com.android.server.wm.ActivityMetricsLogger;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.RecentsAnimationController;
import com.android.server.wm.TaskDisplayArea;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class RecentsAnimation implements RecentsAnimationController.RecentsAnimationCallbacks, TaskDisplayArea.OnRootTaskOrderChangedListener {
    private static final String TAG = RecentsAnimation.class.getSimpleName();
    private final ActivityStartController mActivityStartController;
    private final WindowProcessController mCaller;
    private final TaskDisplayArea mDefaultTaskDisplayArea;
    private ActivityRecord mLaunchedTargetActivity;
    private final ComponentName mRecentsComponent;
    private final String mRecentsFeatureId;
    private final int mRecentsUid;
    private Task mRestoreTargetBehindRootTask;
    private final ActivityTaskManagerService mService;
    private final int mTargetActivityType;
    private final Intent mTargetIntent;
    private final ActivityTaskSupervisor mTaskSupervisor;
    private final int mUserId;
    private final WindowManagerService mWindowManager;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RecentsAnimation(ActivityTaskManagerService atm, ActivityTaskSupervisor taskSupervisor, ActivityStartController activityStartController, WindowManagerService wm, Intent targetIntent, ComponentName recentsComponent, String recentsFeatureId, int recentsUid, WindowProcessController caller) {
        int i;
        this.mService = atm;
        this.mTaskSupervisor = taskSupervisor;
        this.mDefaultTaskDisplayArea = atm.mRootWindowContainer.getDefaultTaskDisplayArea();
        this.mActivityStartController = activityStartController;
        this.mWindowManager = wm;
        this.mTargetIntent = targetIntent;
        this.mRecentsComponent = recentsComponent;
        this.mRecentsFeatureId = recentsFeatureId;
        this.mRecentsUid = recentsUid;
        this.mCaller = caller;
        this.mUserId = atm.getCurrentUserId();
        if (targetIntent.getComponent() != null && recentsComponent.equals(targetIntent.getComponent())) {
            i = 3;
        } else {
            i = 2;
        }
        this.mTargetActivityType = i;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void preloadRecentsActivity() {
        if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
            String protoLogParam0 = String.valueOf(this.mTargetIntent);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, -106400104, 0, (String) null, new Object[]{protoLogParam0});
        }
        Task targetRootTask = this.mDefaultTaskDisplayArea.getRootTask(0, this.mTargetActivityType);
        ActivityRecord targetActivity = getTargetActivity(targetRootTask);
        if (targetActivity != null) {
            if (targetActivity.mVisibleRequested || targetActivity.isTopRunningActivity()) {
                return;
            }
            if (targetActivity.attachedToProcess()) {
                targetActivity.ensureActivityConfiguration(0, false, true);
                if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
                    String protoLogParam02 = String.valueOf(targetActivity.getConfiguration());
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, -1156118957, 0, (String) null, new Object[]{protoLogParam02});
                }
            }
        } else if (this.mDefaultTaskDisplayArea.getActivity(new Predicate() { // from class: com.android.server.wm.RecentsAnimation$$ExternalSyntheticLambda1
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ((ActivityRecord) obj).occludesParent();
            }
        }, false) == null) {
            return;
        } else {
            startRecentsActivityInBackground("preloadRecents");
            Task targetRootTask2 = this.mDefaultTaskDisplayArea.getRootTask(0, this.mTargetActivityType);
            targetActivity = getTargetActivity(targetRootTask2);
            if (targetActivity == null) {
                Slog.w(TAG, "Cannot start " + this.mTargetIntent);
                return;
            }
        }
        if (!targetActivity.attachedToProcess()) {
            if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, 644675193, 0, (String) null, (Object[]) null);
            }
            this.mTaskSupervisor.startSpecificActivity(targetActivity, false, false);
            if (targetActivity.getDisplayContent() != null) {
                targetActivity.getDisplayContent().mUnknownAppVisibilityController.appRemovedOrHidden(targetActivity);
            }
        }
        if (!targetActivity.isState(ActivityRecord.State.STOPPING, ActivityRecord.State.STOPPED)) {
            targetActivity.addToStopping(true, true, "preloadRecents");
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [286=4, 290=6] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public void startRecentsActivity(IRecentsAnimationRunner recentsAnimationRunner, long eventTime) {
        Task targetRootTask;
        ActivityRecord targetActivity;
        ActivityRecord targetActivity2;
        if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
            String protoLogParam0 = String.valueOf(this.mTargetIntent);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, -1413901262, 0, (String) null, new Object[]{protoLogParam0});
        }
        Trace.traceBegin(32L, "RecentsAnimation#startRecentsActivity");
        if (this.mWindowManager.getRecentsAnimationController() != null) {
            this.mWindowManager.getRecentsAnimationController().forceCancelAnimation(2, "startRecentsActivity");
        }
        Task targetRootTask2 = this.mDefaultTaskDisplayArea.getRootTask(0, this.mTargetActivityType);
        ActivityRecord targetActivity3 = getTargetActivity(targetRootTask2);
        boolean hasExistingActivity = targetActivity3 != null;
        if (hasExistingActivity) {
            Task rootTaskAbove = TaskDisplayArea.getRootTaskAbove(targetRootTask2);
            this.mRestoreTargetBehindRootTask = rootTaskAbove;
            if (rootTaskAbove == null && targetRootTask2.getTopMostTask() == targetActivity3.getTask()) {
                notifyAnimationCancelBeforeStart(recentsAnimationRunner);
                if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
                    String protoLogParam02 = String.valueOf(targetRootTask2);
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, -8483143, 0, (String) null, new Object[]{protoLogParam02});
                    return;
                }
                return;
            }
        }
        if (targetActivity3 == null || !targetActivity3.mVisibleRequested) {
            this.mService.mRootWindowContainer.startPowerModeLaunchIfNeeded(true, targetActivity3);
        }
        ActivityMetricsLogger.LaunchingState launchingState = this.mTaskSupervisor.getActivityMetricsLogger().notifyActivityLaunching(this.mTargetIntent);
        if (this.mCaller != null) {
            try {
                ActivityTaskManager.getService().boostSceneStart(6);
            } catch (Exception e) {
                Slog.e(TAG, "Failed to start boost recents animation", e);
            }
            this.mCaller.setRunningRecentsAnimation(true);
        }
        this.mService.deferWindowLayout();
        try {
            try {
                if (hasExistingActivity) {
                    this.mDefaultTaskDisplayArea.moveRootTaskBehindBottomMostVisibleRootTask(targetRootTask2);
                    if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
                        String protoLogParam03 = String.valueOf(targetRootTask2);
                        String protoLogParam1 = String.valueOf(TaskDisplayArea.getRootTaskAbove(targetRootTask2));
                        ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, 1191587912, 0, (String) null, new Object[]{protoLogParam03, protoLogParam1});
                    }
                    Task task = targetActivity3.getTask();
                    if (targetRootTask2.getTopMostTask() != task) {
                        targetRootTask2.positionChildAtTop(task);
                    }
                    targetRootTask = targetRootTask2;
                    targetActivity = targetActivity3;
                } else {
                    startRecentsActivityInBackground("startRecentsActivity_noTargetActivity");
                    Task targetRootTask3 = this.mDefaultTaskDisplayArea.getRootTask(0, this.mTargetActivityType);
                    ActivityRecord targetActivity4 = getTargetActivity(targetRootTask3);
                    this.mDefaultTaskDisplayArea.moveRootTaskBehindBottomMostVisibleRootTask(targetRootTask3);
                    if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
                        String protoLogParam04 = String.valueOf(targetRootTask3);
                        String protoLogParam12 = String.valueOf(TaskDisplayArea.getRootTaskAbove(targetRootTask3));
                        ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, 1191587912, 0, (String) null, new Object[]{protoLogParam04, protoLogParam12});
                    }
                    this.mWindowManager.prepareAppTransitionNone();
                    this.mWindowManager.executeAppTransition();
                    if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
                        String protoLogParam05 = String.valueOf(this.mTargetIntent);
                        ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, 646155519, 0, (String) null, new Object[]{protoLogParam05});
                    }
                    targetRootTask = targetRootTask3;
                    targetActivity = targetActivity4;
                }
                try {
                    targetActivity.mLaunchTaskBehind = true;
                    this.mLaunchedTargetActivity = targetActivity;
                    targetActivity.intent.replaceExtras(this.mTargetIntent);
                    if (!targetActivity.mLaunchTaskBehind) {
                        try {
                            Slog.i(TAG, "OS ADD: reset mLaunchTaskBehind to true");
                            targetActivity.mLaunchTaskBehind = true;
                        } catch (Exception e2) {
                            e = e2;
                            Slog.e(TAG, "Failed to start recents activity", e);
                            throw e;
                        } catch (Throwable th) {
                            e = th;
                            this.mService.continueWindowLayout();
                            Trace.traceEnd(32L);
                            throw e;
                        }
                    }
                    targetActivity2 = targetActivity;
                } catch (Exception e3) {
                    e = e3;
                } catch (Throwable th2) {
                    e = th2;
                }
            } catch (Exception e4) {
                e = e4;
            }
        } catch (Throwable th3) {
            e = th3;
        }
        try {
            this.mWindowManager.initializeRecentsAnimation(this.mTargetActivityType, recentsAnimationRunner, this, this.mDefaultTaskDisplayArea.getDisplayId(), this.mTaskSupervisor.mRecentTasks.getRecentTaskIds(), targetActivity);
            this.mService.mRootWindowContainer.ensureActivitiesVisible(null, 0, true);
            ActivityOptions options = null;
            if (eventTime > 0) {
                options = ActivityOptions.makeBasic();
                options.setSourceInfo(4, eventTime);
            }
            this.mTaskSupervisor.getActivityMetricsLogger().notifyActivityLaunched(launchingState, 2, !hasExistingActivity, targetActivity2, options);
            this.mDefaultTaskDisplayArea.registerRootTaskOrderChangedListener(this);
            this.mService.continueWindowLayout();
            Trace.traceEnd(32L);
        } catch (Exception e5) {
            e = e5;
            Slog.e(TAG, "Failed to start recents activity", e);
            throw e;
        } catch (Throwable th4) {
            e = th4;
            this.mService.continueWindowLayout();
            Trace.traceEnd(32L);
            throw e;
        }
    }

    private void finishAnimation(final int reorderMode, final boolean sendUserLeaveHint) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
                    String protoLogParam0 = String.valueOf(this.mWindowManager.getRecentsAnimationController());
                    long protoLogParam1 = reorderMode;
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, 765395228, 4, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1)});
                }
                this.mDefaultTaskDisplayArea.unregisterRootTaskOrderChangedListener(this);
                final RecentsAnimationController controller = this.mWindowManager.getRecentsAnimationController();
                if (controller == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (reorderMode != 0) {
                    this.mService.endLaunchPowerMode(1);
                }
                if (reorderMode == 1) {
                    this.mService.stopAppSwitches();
                }
                this.mWindowManager.inSurfaceTransaction(new Runnable() { // from class: com.android.server.wm.RecentsAnimation$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        RecentsAnimation.this.m8144lambda$finishAnimation$0$comandroidserverwmRecentsAnimation(reorderMode, sendUserLeaveHint, controller);
                    }
                });
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [418=5, 419=4, 423=4, 424=4, 426=4, 429=4, 430=4, 431=4, 432=4, 434=4, 436=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$finishAnimation$0$com-android-server-wm-RecentsAnimation  reason: not valid java name */
    public /* synthetic */ void m8144lambda$finishAnimation$0$comandroidserverwmRecentsAnimation(int reorderMode, boolean sendUserLeaveHint, RecentsAnimationController controller) {
        boolean z;
        int i;
        Trace.traceBegin(32L, "RecentsAnimation#onAnimationFinished_inSurfaceTransaction");
        this.mService.deferWindowLayout();
        try {
            try {
                this.mWindowManager.cleanupRecentsAnimation(reorderMode);
                Task targetRootTask = this.mDefaultTaskDisplayArea.getRootTask(0, this.mTargetActivityType);
                ActivityRecord targetActivity = targetRootTask != null ? targetRootTask.isInTask(this.mLaunchedTargetActivity) : null;
                if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
                    String protoLogParam0 = String.valueOf(targetRootTask);
                    String protoLogParam1 = String.valueOf(targetActivity);
                    String protoLogParam2 = String.valueOf(this.mRestoreTargetBehindRootTask);
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, 1781673113, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1, protoLogParam2});
                }
                if (targetActivity == null) {
                    this.mTaskSupervisor.mUserLeaving = false;
                    this.mService.continueWindowLayout();
                    if (this.mWindowManager.mRoot.isLayoutNeeded()) {
                        this.mWindowManager.mRoot.performSurfacePlacement();
                    }
                    if (this.mCaller != null) {
                        try {
                            ActivityTaskManager.getService().boostSceneEnd(6);
                        } catch (Exception e) {
                            Slog.e(TAG, "Failed to stop boost recents animation", e);
                        }
                        this.mCaller.setRunningRecentsAnimation(false);
                    }
                    Trace.traceEnd(32L);
                    return;
                }
                targetActivity.mLaunchTaskBehind = false;
                if (reorderMode == 1) {
                    this.mTaskSupervisor.mNoAnimActivities.add(targetActivity);
                    if (sendUserLeaveHint) {
                        this.mTaskSupervisor.mUserLeaving = true;
                        z = true;
                        i = 2;
                        targetRootTask.moveTaskToFront(targetActivity.getTask(), true, null, targetActivity.appTimeTracker, "RecentsAnimation.onAnimationFinished()");
                    } else {
                        z = true;
                        i = 2;
                        targetRootTask.moveToFront("RecentsAnimation.onAnimationFinished()");
                    }
                    if (ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS.isLogToAny()) {
                        Task topRootTask = getTopNonAlwaysOnTopRootTask();
                        if (topRootTask != targetRootTask && ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
                            String protoLogParam02 = String.valueOf(targetRootTask);
                            String protoLogParam12 = String.valueOf(topRootTask);
                            ProtoLogGroup protoLogGroup = ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS;
                            Object[] objArr = new Object[i];
                            objArr[0] = protoLogParam02;
                            objArr[z ? 1 : 0] = protoLogParam12;
                            ProtoLogImpl.w(protoLogGroup, -302468788, 0, (String) null, objArr);
                        }
                    }
                } else {
                    z = true;
                    if (reorderMode != 2) {
                        if (!controller.shouldDeferCancelWithScreenshot() && !targetRootTask.isFocusedRootTaskOnDisplay()) {
                            targetRootTask.ensureActivitiesVisible(null, 0, false);
                        }
                        this.mTaskSupervisor.mUserLeaving = false;
                        this.mService.continueWindowLayout();
                        if (this.mWindowManager.mRoot.isLayoutNeeded()) {
                            this.mWindowManager.mRoot.performSurfacePlacement();
                        }
                        if (this.mCaller != null) {
                            try {
                                ActivityTaskManager.getService().boostSceneEnd(6);
                            } catch (Exception e2) {
                                Slog.e(TAG, "Failed to stop boost recents animation", e2);
                            }
                            this.mCaller.setRunningRecentsAnimation(false);
                        }
                        Trace.traceEnd(32L);
                        return;
                    }
                    TaskDisplayArea taskDisplayArea = targetActivity.getDisplayArea();
                    taskDisplayArea.moveRootTaskBehindRootTask(targetRootTask, this.mRestoreTargetBehindRootTask);
                    if (ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS.isLogToAny()) {
                        Task aboveTargetRootTask = TaskDisplayArea.getRootTaskAbove(targetRootTask);
                        Task task = this.mRestoreTargetBehindRootTask;
                        if (task != null && aboveTargetRootTask != task && ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
                            String protoLogParam03 = String.valueOf(targetRootTask);
                            String protoLogParam13 = String.valueOf(this.mRestoreTargetBehindRootTask);
                            String protoLogParam22 = String.valueOf(aboveTargetRootTask);
                            ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, 1822314934, 0, (String) null, new Object[]{protoLogParam03, protoLogParam13, protoLogParam22});
                        }
                    }
                }
                this.mWindowManager.prepareAppTransitionNone();
                this.mService.mRootWindowContainer.ensureActivitiesVisible(null, 0, false);
                this.mService.mRootWindowContainer.resumeFocusedTasksTopActivities();
                this.mWindowManager.executeAppTransition();
                Task rootTask = targetRootTask.getRootTask();
                rootTask.dispatchTaskInfoChangedIfNeeded(z);
                this.mTaskSupervisor.mUserLeaving = false;
                this.mService.continueWindowLayout();
                if (this.mWindowManager.mRoot.isLayoutNeeded()) {
                    this.mWindowManager.mRoot.performSurfacePlacement();
                }
                if (this.mCaller != null) {
                    try {
                        ActivityTaskManager.getService().boostSceneEnd(6);
                    } catch (Exception e3) {
                        Slog.e(TAG, "Failed to stop boost recents animation", e3);
                    }
                    this.mCaller.setRunningRecentsAnimation(false);
                }
                Trace.traceEnd(32L);
            } catch (Throwable th) {
                this.mTaskSupervisor.mUserLeaving = false;
                this.mService.continueWindowLayout();
                if (this.mWindowManager.mRoot.isLayoutNeeded()) {
                    this.mWindowManager.mRoot.performSurfacePlacement();
                }
                if (this.mCaller != null) {
                    try {
                        ActivityTaskManager.getService().boostSceneEnd(6);
                    } catch (Exception e4) {
                        Slog.e(TAG, "Failed to stop boost recents animation", e4);
                    }
                    this.mCaller.setRunningRecentsAnimation(false);
                }
                Trace.traceEnd(32L);
                throw th;
            }
        } catch (Exception e5) {
            Slog.e(TAG, "Failed to clean up recents activity", e5);
            throw e5;
        }
    }

    @Override // com.android.server.wm.RecentsAnimationController.RecentsAnimationCallbacks
    public void onAnimationFinished(int reorderMode, boolean sendUserLeaveHint) {
        finishAnimation(reorderMode, sendUserLeaveHint);
    }

    @Override // com.android.server.wm.TaskDisplayArea.OnRootTaskOrderChangedListener
    public void onRootTaskOrderChanged(final Task rootTask) {
        RecentsAnimationController controller;
        if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
            String protoLogParam0 = String.valueOf(rootTask);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, -1069336896, 0, (String) null, new Object[]{protoLogParam0});
        }
        if (this.mDefaultTaskDisplayArea.getRootTask(new Predicate() { // from class: com.android.server.wm.RecentsAnimation$$ExternalSyntheticLambda4
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return RecentsAnimation.lambda$onRootTaskOrderChanged$1(Task.this, (Task) obj);
            }
        }) == null || !rootTask.shouldBeVisible(null) || (controller = this.mWindowManager.getRecentsAnimationController()) == null) {
            return;
        }
        if ((!controller.isAnimatingTask(rootTask.getTopMostTask()) || controller.isTargetApp(rootTask.getTopNonFinishingActivity())) && controller.shouldDeferCancelUntilNextTransition()) {
            this.mWindowManager.prepareAppTransitionNone();
            controller.setCancelOnNextTransitionStart();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$onRootTaskOrderChanged$1(Task rootTask, Task t) {
        return t == rootTask;
    }

    private void startRecentsActivityInBackground(String reason) {
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchActivityType(this.mTargetActivityType);
        options.setAvoidMoveToFront();
        this.mTargetIntent.addFlags(268500992);
        this.mActivityStartController.obtainStarter(this.mTargetIntent, reason).setCallingUid(this.mRecentsUid).setCallingPackage(this.mRecentsComponent.getPackageName()).setCallingFeatureId(this.mRecentsFeatureId).setActivityOptions(new SafeActivityOptions(options)).setUserId(this.mUserId).execute();
    }

    static void notifyAnimationCancelBeforeStart(IRecentsAnimationRunner recentsAnimationRunner) {
        try {
            recentsAnimationRunner.onAnimationCanceled((int[]) null, (TaskSnapshot[]) null);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to cancel recents animation before start", e);
        }
    }

    private Task getTopNonAlwaysOnTopRootTask() {
        return this.mDefaultTaskDisplayArea.getRootTask(new Predicate() { // from class: com.android.server.wm.RecentsAnimation$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return RecentsAnimation.lambda$getTopNonAlwaysOnTopRootTask$2((Task) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getTopNonAlwaysOnTopRootTask$2(Task task) {
        return !task.getWindowConfiguration().isAlwaysOnTop();
    }

    private ActivityRecord getTargetActivity(Task targetRootTask) {
        if (targetRootTask == null) {
            return null;
        }
        PooledPredicate p = PooledLambda.obtainPredicate(new BiPredicate() { // from class: com.android.server.wm.RecentsAnimation$$ExternalSyntheticLambda0
            @Override // java.util.function.BiPredicate
            public final boolean test(Object obj, Object obj2) {
                boolean matchesTarget;
                matchesTarget = ((RecentsAnimation) obj).matchesTarget((Task) obj2);
                return matchesTarget;
            }
        }, this, PooledLambda.__(Task.class));
        Task task = targetRootTask.getTask(p);
        p.recycle();
        if (task != null) {
            return task.getTopNonFinishingActivity();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean matchesTarget(Task task) {
        return task.getNonFinishingActivityCount() > 0 && task.mUserId == this.mUserId && task.getBaseIntent().getComponent().equals(this.mTargetIntent.getComponent());
    }
}
