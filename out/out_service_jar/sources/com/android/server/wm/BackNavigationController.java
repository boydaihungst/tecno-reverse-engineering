package com.android.server.wm;

import android.app.WindowConfiguration;
import android.content.ComponentName;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Slog;
import android.view.RemoteAnimationTarget;
import android.view.SurfaceControl;
import android.window.BackNavigationInfo;
import android.window.OnBackInvokedCallbackInfo;
import android.window.TaskSnapshot;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.server.LocalServices;
import com.android.server.wm.EmbeddedWindowController;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class BackNavigationController {
    private static final String TAG = "BackNavigationController";
    private TaskSnapshotController mTaskSnapshotController;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isEnabled() {
        return SystemProperties.getInt("persist.wm.debug.predictive_back", 1) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isScreenshotEnabled() {
        return SystemProperties.getInt("persist.wm.debug.predictive_back_screenshot", 0) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BackNavigationInfo startBackNavigation(WindowManagerService wmService, boolean requestAnimation) {
        return startBackNavigation(wmService, null, requestAnimation);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [314=18] */
    /* JADX WARN: Code restructure failed: missing block: B:52:0x00ff, code lost:
        if (com.android.server.wm.ProtoLogCache.WM_DEBUG_BACK_PREVIEW_enabled == false) goto L279;
     */
    /* JADX WARN: Code restructure failed: missing block: B:53:0x0101, code lost:
        r16 = null;
     */
    /* JADX WARN: Code restructure failed: missing block: B:54:0x010d, code lost:
        r19 = null;
     */
    /* JADX WARN: Code restructure failed: missing block: B:55:0x0110, code lost:
        com.android.internal.protolog.ProtoLogImpl.w(com.android.internal.protolog.ProtoLogGroup.WM_DEBUG_BACK_PREVIEW, 1264179654, 0, "No focused window, defaulting to top current task's window", (java.lang.Object[]) null);
     */
    /* JADX WARN: Code restructure failed: missing block: B:56:0x0114, code lost:
        r19 = null;
     */
    /* JADX WARN: Code restructure failed: missing block: B:58:0x011c, code lost:
        r3 = r35.mAtmService.getTopDisplayFocusedRootTask();
     */
    /* JADX WARN: Code restructure failed: missing block: B:59:0x011d, code lost:
        r15 = r3.getWindow(new com.android.server.wm.BackNavigationController$$ExternalSyntheticLambda0());
     */
    /* JADX WARN: Code restructure failed: missing block: B:60:0x0128, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:62:0x0135, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:88:0x0190, code lost:
        r0 = th;
     */
    /* JADX WARN: Not initialized variable reg: 20, insn: 0x00b0: MOVE  (r4 I:??[OBJECT, ARRAY]) = (r20 I:??[OBJECT, ARRAY] A[D('prevTask' com.android.server.wm.Task)]), block:B:35:0x00ae */
    /* JADX WARN: Not initialized variable reg: 21, insn: 0x00b2: MOVE  (r2 I:??[OBJECT, ARRAY]) = (r21 I:??[OBJECT, ARRAY] A[D('currentActivity' com.android.server.wm.ActivityRecord)]), block:B:35:0x00ae */
    /* JADX WARN: Not initialized variable reg: 22, insn: 0x00b4: MOVE  (r3 I:??[OBJECT, ARRAY]) = (r22 I:??[OBJECT, ARRAY] A[D('currentTask' com.android.server.wm.Task)]), block:B:35:0x00ae */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    BackNavigationInfo startBackNavigation(WindowManagerService wmService, SurfaceControl.Transaction tx, final boolean requestAnimation) {
        ActivityRecord currentActivity;
        Task currentTask;
        Task prevTask;
        int backType;
        Task currentTask2;
        ActivityRecord currentActivity2;
        SurfaceControl animationLeashParent;
        HardwareBuffer screenshotBuffer;
        Task currentTask3;
        ActivityRecord currentActivity3;
        RemoteAnimationTarget topAppTarget;
        int backType2;
        WindowContainer<?> removedWindowContainer;
        Task prevTask2;
        int prevTaskId;
        RemoteAnimationTarget topAppTarget2;
        RemoteAnimationTarget topAppTarget3;
        SurfaceControl animationLeashParent2;
        Task currentTask4;
        ActivityRecord currentActivity4;
        boolean z;
        RecentsAnimationController recentsAnimationController;
        OnBackInvokedCallbackInfo overrideCallbackInfo;
        SurfaceControl.Transaction tx2 = tx == null ? new SurfaceControl.Transaction() : tx;
        BackNavigationInfo.Builder infoBuilder = new BackNavigationInfo.Builder();
        synchronized (wmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                WindowManagerInternal windowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
                IBinder focusedWindowToken = windowManagerInternal.getFocusedWindowToken();
                WindowState window = wmService.getFocusedWindowLocked();
                try {
                    if (window == null) {
                        try {
                            EmbeddedWindowController.EmbeddedWindow embeddedWindow = wmService.mEmbeddedWindowController.getByFocusToken(focusedWindowToken);
                            if (embeddedWindow != null) {
                                if (ProtoLogCache.WM_DEBUG_BACK_PREVIEW_enabled) {
                                    try {
                                        Object[] objArr = null;
                                        ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_BACK_PREVIEW, -1717147904, 0, "Current focused window is embeddedWindow. Dispatch KEYCODE_BACK.", (Object[]) null);
                                    } catch (Throwable th) {
                                        th = th;
                                        while (true) {
                                            try {
                                                break;
                                            } catch (Throwable th2) {
                                                th = th2;
                                            }
                                        }
                                        WindowManagerService.resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                }
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return null;
                            }
                            currentActivity = null;
                            currentTask = null;
                            prevTask = null;
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    } else {
                        currentActivity = null;
                        currentTask = null;
                        prevTask = null;
                    }
                    if (window != null && ProtoLogCache.WM_DEBUG_BACK_PREVIEW_enabled) {
                        Object[] objArr2 = null;
                        ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_BACK_PREVIEW, -997565097, 0, "Focused window found using getFocusedWindowToken", (Object[]) null);
                    }
                    OnBackInvokedCallbackInfo overrideCallbackInfo2 = null;
                    if (window != null && (recentsAnimationController = wmService.getRecentsAnimationController()) != null && recentsAnimationController.shouldApplyInputConsumer(window.getActivityRecord())) {
                        window = recentsAnimationController.getTargetAppMainWindow();
                        OnBackInvokedCallbackInfo overrideCallbackInfo3 = recentsAnimationController.getBackInvokedInfo();
                        if (ProtoLogCache.WM_DEBUG_BACK_PREVIEW_enabled) {
                            Object[] objArr3 = null;
                            overrideCallbackInfo = overrideCallbackInfo3;
                            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_BACK_PREVIEW, -451552570, 0, "Current focused window being animated by recents. Overriding back callback to recents controller callback.", (Object[]) null);
                        } else {
                            overrideCallbackInfo = overrideCallbackInfo3;
                        }
                        overrideCallbackInfo2 = overrideCallbackInfo;
                    }
                    WindowContainer<?> windowContainer = null;
                    Task currentTask5 = currentTask;
                    OnBackInvokedCallbackInfo callbackInfo = null;
                    if (window != null) {
                        ActivityRecord currentActivity5 = window.mActivityRecord;
                        try {
                            Task currentTask6 = window.getTask();
                            callbackInfo = overrideCallbackInfo2 != null ? overrideCallbackInfo2 : window.getOnBackInvokedCallbackInfo();
                            if (callbackInfo == null) {
                                Slog.e(TAG, "No callback registered, returning null.");
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return null;
                            }
                            backType = !callbackInfo.isSystemCallback() ? 4 : -1;
                            try {
                                infoBuilder.setOnBackInvokedCallback(callbackInfo.getCallback());
                                currentActivity2 = currentActivity5;
                                currentTask2 = currentTask6;
                            } catch (Throwable th4) {
                                th = th4;
                                while (true) {
                                    break;
                                    break;
                                }
                                WindowManagerService.resetPriorityAfterLockedSection();
                                throw th;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                        }
                    } else {
                        backType = -1;
                        currentTask2 = currentTask5;
                        currentActivity2 = currentActivity;
                    }
                    try {
                        if (ProtoLogCache.WM_DEBUG_BACK_PREVIEW_enabled) {
                            try {
                                String protoLogParam0 = String.valueOf(currentTask2);
                                String protoLogParam1 = String.valueOf(currentActivity2);
                                String protoLogParam2 = String.valueOf(callbackInfo);
                                String protoLogParam3 = String.valueOf(window);
                                animationLeashParent = null;
                                screenshotBuffer = null;
                                try {
                                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_BACK_PREVIEW, -1277068810, 0, "startBackNavigation currentTask=%s, topRunningActivity=%s, callbackInfo=%s, currentFocus=%s", new Object[]{protoLogParam0, protoLogParam1, protoLogParam2, protoLogParam3});
                                } catch (Throwable th6) {
                                    th = th6;
                                    while (true) {
                                        break;
                                        break;
                                    }
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    throw th;
                                }
                            } catch (Throwable th7) {
                                th = th7;
                            }
                        } else {
                            animationLeashParent = null;
                            screenshotBuffer = null;
                        }
                        if (window == null) {
                            Slog.e(TAG, "Window is null, returning null.");
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return null;
                        }
                        if (backType == 4 || currentActivity2 == null || currentTask2 == null) {
                            currentTask3 = currentTask2;
                            currentActivity3 = currentActivity2;
                            topAppTarget = null;
                        } else {
                            try {
                                if (currentActivity2.isActivityTypeHome()) {
                                    currentTask3 = currentTask2;
                                    currentActivity3 = currentActivity2;
                                    topAppTarget = null;
                                } else {
                                    final Task finalTask = currentTask2;
                                    ActivityRecord prevActivity = currentTask2.getActivity(new Predicate() { // from class: com.android.server.wm.BackNavigationController$$ExternalSyntheticLambda1
                                        @Override // java.util.function.Predicate
                                        public final boolean test(Object obj) {
                                            return BackNavigationController.lambda$startBackNavigation$0(Task.this, (ActivityRecord) obj);
                                        }
                                    });
                                    if (window.getParent().getChildCount() > 1 && window.getParent().getChildAt(0) != window) {
                                        backType2 = 0;
                                        removedWindowContainer = window;
                                        prevTask2 = prevTask;
                                    } else if (prevActivity != null) {
                                        backType2 = 2;
                                        removedWindowContainer = currentActivity2;
                                        prevTask2 = prevTask;
                                    } else if (currentTask2.returnsToHomeRootTask()) {
                                        backType2 = 1;
                                        removedWindowContainer = currentTask2;
                                        prevTask2 = prevTask;
                                    } else if (currentActivity2.isRootOfTask()) {
                                        prevTask2 = currentTask2.mRootWindowContainer.getTaskBelow(currentTask2);
                                        removedWindowContainer = currentTask2;
                                        try {
                                            prevActivity = prevTask2.getTopNonFinishingActivity();
                                            backType2 = prevTask2.isActivityTypeHome() ? 1 : 3;
                                        } catch (Throwable th8) {
                                            th = th8;
                                            while (true) {
                                                break;
                                                break;
                                            }
                                            WindowManagerService.resetPriorityAfterLockedSection();
                                            throw th;
                                        }
                                    } else {
                                        backType2 = backType;
                                        removedWindowContainer = windowContainer;
                                        prevTask2 = prevTask;
                                    }
                                    try {
                                        infoBuilder.setType(backType2);
                                        if (prevTask2 != null) {
                                            try {
                                                prevTaskId = prevTask2.mTaskId;
                                            } catch (Throwable th9) {
                                                th = th9;
                                                while (true) {
                                                    break;
                                                    break;
                                                }
                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                throw th;
                                            }
                                        } else {
                                            prevTaskId = 0;
                                        }
                                        int prevUserId = prevTask2 != null ? prevTask2.mUserId : 0;
                                        if (ProtoLogCache.WM_DEBUG_BACK_PREVIEW_enabled) {
                                            try {
                                                String protoLogParam02 = String.valueOf(prevActivity != null ? prevActivity.mActivityComponent : null);
                                                String protoLogParam12 = String.valueOf(prevTask2 != null ? prevTask2.getName() : null);
                                                String protoLogParam22 = String.valueOf(removedWindowContainer);
                                                String protoLogParam32 = String.valueOf(BackNavigationInfo.typeToString(backType2));
                                                topAppTarget2 = null;
                                                try {
                                                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_BACK_PREVIEW, 531891870, 0, "Previous Destination is Activity:%s Task:%s removedContainer:%s, backType=%s", new Object[]{protoLogParam02, protoLogParam12, protoLogParam22, protoLogParam32});
                                                } catch (Throwable th10) {
                                                    th = th10;
                                                    while (true) {
                                                        break;
                                                        break;
                                                    }
                                                    WindowManagerService.resetPriorityAfterLockedSection();
                                                    throw th;
                                                }
                                            } catch (Throwable th11) {
                                                th = th11;
                                                while (true) {
                                                    break;
                                                    break;
                                                }
                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                throw th;
                                            }
                                        } else {
                                            topAppTarget2 = null;
                                        }
                                        boolean prepareAnimation = backType2 == 1 && !removedWindowContainer.hasCommittedReparentToAnimationLeash();
                                        if (prepareAnimation) {
                                            WindowConfiguration taskWindowConfiguration = currentTask2.getTaskInfo().configuration.windowConfiguration;
                                            infoBuilder.setTaskWindowConfiguration(taskWindowConfiguration);
                                            SurfaceControl animLeash = removedWindowContainer.makeAnimationLeash().setName("BackPreview Leash for " + removedWindowContainer).setHidden(false).setBLASTLayer().build();
                                            removedWindowContainer.reparentSurfaceControl(tx2, animLeash);
                                            animationLeashParent2 = removedWindowContainer.getAnimationLeashParent();
                                            try {
                                                RemoteAnimationTarget topAppTarget4 = createRemoteAnimationTargetLocked(removedWindowContainer, currentActivity2, currentTask2, animLeash);
                                                try {
                                                    infoBuilder.setDepartingAnimationTarget(topAppTarget4);
                                                    topAppTarget3 = topAppTarget4;
                                                } catch (Throwable th12) {
                                                    th = th12;
                                                    while (true) {
                                                        break;
                                                        break;
                                                    }
                                                    WindowManagerService.resetPriorityAfterLockedSection();
                                                    throw th;
                                                }
                                            } catch (Throwable th13) {
                                                th = th13;
                                            }
                                        } else {
                                            topAppTarget3 = topAppTarget2;
                                            animationLeashParent2 = animationLeashParent;
                                        }
                                        try {
                                            if (needsScreenshot(backType2) && prevActivity != null) {
                                                try {
                                                    if (prevActivity.mActivityComponent != null) {
                                                        screenshotBuffer = getActivitySnapshot(currentTask2, prevActivity.mActivityComponent);
                                                    }
                                                } catch (Throwable th14) {
                                                    th = th14;
                                                    while (true) {
                                                        break;
                                                        break;
                                                    }
                                                    WindowManagerService.resetPriorityAfterLockedSection();
                                                    throw th;
                                                }
                                            }
                                            if (backType2 == 1 && requestAnimation && prevTask2 != null) {
                                                try {
                                                    currentTask2.mBackGestureStarted = true;
                                                    prevActivity = prevTask2.getTopNonFinishingActivity();
                                                    if (prevActivity != null) {
                                                        if (prevActivity.mVisibleRequested) {
                                                            z = true;
                                                        } else {
                                                            z = true;
                                                            prevActivity.setVisibility(true);
                                                        }
                                                        prevActivity.mLaunchTaskBehind = z;
                                                        if (ProtoLogCache.WM_DEBUG_BACK_PREVIEW_enabled) {
                                                            String protoLogParam03 = String.valueOf(prevActivity);
                                                            currentTask4 = currentTask2;
                                                            currentActivity4 = currentActivity2;
                                                            try {
                                                                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_BACK_PREVIEW, 948208142, 0, "Setting Activity.mLauncherTaskBehind to true. Activity=%s", new Object[]{protoLogParam03});
                                                            } catch (Throwable th15) {
                                                                th = th15;
                                                                while (true) {
                                                                    break;
                                                                    break;
                                                                }
                                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                                throw th;
                                                            }
                                                        } else {
                                                            currentTask4 = currentTask2;
                                                            currentActivity4 = currentActivity2;
                                                        }
                                                        prevActivity.mRootWindowContainer.ensureActivitiesVisible(null, 0, false);
                                                    } else {
                                                        currentTask4 = currentTask2;
                                                        currentActivity4 = currentActivity2;
                                                    }
                                                } catch (Throwable th16) {
                                                    th = th16;
                                                }
                                            } else {
                                                currentTask4 = currentTask2;
                                                currentActivity4 = currentActivity2;
                                            }
                                            final ActivityRecord prevActivity2 = prevActivity;
                                            try {
                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                if (topAppTarget3 != null && needsScreenshot(backType2) && prevTask2 != null && screenshotBuffer == null) {
                                                    SurfaceControl.Builder builder = new SurfaceControl.Builder().setName("BackPreview Screenshot for " + prevActivity2).setParent(animationLeashParent2).setHidden(false).setBLASTLayer();
                                                    infoBuilder.setScreenshotSurface(builder.build());
                                                    HardwareBuffer screenshotBuffer2 = getTaskSnapshot(prevTaskId, prevUserId);
                                                    infoBuilder.setScreenshotBuffer(screenshotBuffer2);
                                                    tx2.setLayer(topAppTarget3.leash, 1);
                                                }
                                                final WindowContainer<?> finalRemovedWindowContainer = removedWindowContainer;
                                                if (finalRemovedWindowContainer != null) {
                                                    try {
                                                        currentActivity4.token.linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.wm.BackNavigationController$$ExternalSyntheticLambda2
                                                            @Override // android.os.IBinder.DeathRecipient
                                                            public final void binderDied() {
                                                                BackNavigationController.this.m7877x9892e5e5(finalRemovedWindowContainer);
                                                            }
                                                        }, 0);
                                                        final int prevTaskId2 = backType2;
                                                        final Task prevTask3 = currentTask4;
                                                        RemoteCallback onBackNavigationDone = new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.wm.BackNavigationController$$ExternalSyntheticLambda3
                                                            public final void onResult(Bundle bundle) {
                                                                BackNavigationController.this.m7878xd25d87c4(finalRemovedWindowContainer, prevTaskId2, prevTask3, prevActivity2, requestAnimation, bundle);
                                                            }
                                                        });
                                                        infoBuilder.setOnBackNavigationDone(onBackNavigationDone);
                                                    } catch (RemoteException e) {
                                                        Slog.e(TAG, "Failed to link to death", e);
                                                        m7877x9892e5e5(removedWindowContainer);
                                                        return null;
                                                    }
                                                }
                                                tx2.apply();
                                                return infoBuilder.build();
                                            } catch (Throwable th17) {
                                                th = th17;
                                                while (true) {
                                                    break;
                                                    break;
                                                }
                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                throw th;
                                            }
                                        } catch (Throwable th18) {
                                            th = th18;
                                        }
                                    } catch (Throwable th19) {
                                        th = th19;
                                    }
                                }
                            } catch (Throwable th20) {
                                th = th20;
                            }
                        }
                        try {
                            BackNavigationInfo build = infoBuilder.setType(backType).build();
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return build;
                        } catch (Throwable th21) {
                            th = th21;
                            while (true) {
                                break;
                                break;
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    } catch (Throwable th22) {
                        th = th22;
                    }
                } catch (Throwable th23) {
                    th = th23;
                }
            } catch (Throwable th24) {
                th = th24;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$startBackNavigation$0(Task finalTask, ActivityRecord r) {
        return (r.finishing || r.getTask() != finalTask || r.isTopRunningActivity()) ? false : true;
    }

    private static RemoteAnimationTarget createRemoteAnimationTargetLocked(WindowContainer<?> removedWindowContainer, ActivityRecord activityRecord, Task task, SurfaceControl animLeash) {
        return new RemoteAnimationTarget(task.mTaskId, 1, animLeash, false, new Rect(), new Rect(), activityRecord.getPrefixOrderIndex(), new Point(0, 0), new Rect(), new Rect(), removedWindowContainer.getWindowConfiguration(), true, (SurfaceControl) null, (Rect) null, task.getTaskInfo(), false, activityRecord.windowType);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: onBackNavigationDone */
    public void m7878xd25d87c4(Bundle result, WindowContainer<?> windowContainer, int backType, Task task, ActivityRecord prevActivity, boolean requestAnimation) {
        SurfaceControl surfaceControl = windowContainer.getSurfaceControl();
        boolean triggerBack = result != null && result.getBoolean("TriggerBack");
        if (ProtoLogCache.WM_DEBUG_BACK_PREVIEW_enabled) {
            String protoLogParam0 = String.valueOf(backType);
            String protoLogParam1 = String.valueOf(task);
            String protoLogParam2 = String.valueOf(prevActivity);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_BACK_PREVIEW, 1778919449, 0, "onBackNavigationDone backType=%s, task=%s, prevActivity=%s", new Object[]{protoLogParam0, protoLogParam1, protoLogParam2});
        }
        if (backType == 1 && requestAnimation) {
            if (triggerBack && surfaceControl != null && surfaceControl.isValid()) {
                SurfaceControl.Transaction t = windowContainer.getSyncTransaction();
                t.hide(surfaceControl);
                t.apply();
            }
            if (prevActivity != null && !triggerBack) {
                task.mTaskSupervisor.scheduleLaunchTaskBehindComplete(prevActivity.token);
                prevActivity.mLaunchTaskBehind = false;
                if (ProtoLogCache.WM_DEBUG_BACK_PREVIEW_enabled) {
                    String protoLogParam02 = String.valueOf(prevActivity);
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_BACK_PREVIEW, -711194343, 0, "Setting Activity.mLauncherTaskBehind to false. Activity=%s", new Object[]{protoLogParam02});
                }
            }
        } else {
            task.mBackGestureStarted = false;
        }
        m7877x9892e5e5(windowContainer);
    }

    private HardwareBuffer getActivitySnapshot(Task task, ComponentName activityComponent) {
        SurfaceControl.ScreenshotHardwareBuffer backBuffer = task.mBackScreenshots.get(activityComponent.flattenToString());
        if (backBuffer != null) {
            return backBuffer.getHardwareBuffer();
        }
        return null;
    }

    private HardwareBuffer getTaskSnapshot(int taskId, int userId) {
        TaskSnapshot snapshot;
        TaskSnapshotController taskSnapshotController = this.mTaskSnapshotController;
        if (taskSnapshotController == null || (snapshot = taskSnapshotController.getSnapshot(taskId, userId, true, false)) == null) {
            return null;
        }
        return snapshot.getHardwareBuffer();
    }

    private boolean needsScreenshot(int backType) {
        if (isScreenshotEnabled()) {
            switch (backType) {
                case 0:
                case 1:
                    return false;
                default:
                    return true;
            }
        }
        return false;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: resetSurfaces */
    public void m7877x9892e5e5(WindowContainer<?> windowContainer) {
        synchronized (windowContainer.mWmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (ProtoLogCache.WM_DEBUG_BACK_PREVIEW_enabled) {
                    Object[] objArr = null;
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_BACK_PREVIEW, -23020844, 0, "Back: Reset surfaces", (Object[]) null);
                }
                SurfaceControl.Transaction tx = windowContainer.getSyncTransaction();
                SurfaceControl surfaceControl = windowContainer.getSurfaceControl();
                if (surfaceControl != null) {
                    tx.reparent(surfaceControl, windowContainer.getParent().getSurfaceControl());
                    tx.apply();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTaskSnapshotController(TaskSnapshotController taskSnapshotController) {
        this.mTaskSnapshotController = taskSnapshotController;
    }
}
