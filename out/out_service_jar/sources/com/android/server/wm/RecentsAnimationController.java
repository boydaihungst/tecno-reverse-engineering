package com.android.server.wm;

import android.app.WindowConfiguration;
import android.graphics.GraphicBuffer;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.hardware.input.InputManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IntArray;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.proto.ProtoOutputStream;
import android.view.IRecentsAnimationController;
import android.view.IRecentsAnimationRunner;
import android.view.InputWindowHandle;
import android.view.KeyEvent;
import android.view.RemoteAnimationTarget;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import android.view.WindowInsets;
import android.window.BackEvent;
import android.window.IOnBackInvokedCallback;
import android.window.OnBackInvokedCallbackInfo;
import android.window.PictureInPictureSurfaceTransaction;
import android.window.TaskSnapshot;
import com.android.internal.os.BackgroundThread;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.function.pooled.PooledConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.LocalServices;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wm.RecentsAnimationController;
import com.android.server.wm.SurfaceAnimator;
import com.android.server.wm.WindowManagerInternal;
import com.android.server.wm.utils.InsetUtils;
import com.google.android.collect.Sets;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
/* loaded from: classes2.dex */
public class RecentsAnimationController implements IBinder.DeathRecipient {
    private static final long FAILSAFE_DELAY = 1000;
    private static final long LATENCY_TRACKER_LOG_DELAY_MS = 300;
    private static final int MODE_UNKNOWN = -1;
    public static final int REORDER_KEEP_IN_PLACE = 0;
    public static final int REORDER_MOVE_TO_ORIGINAL_POSITION = 2;
    public static final int REORDER_MOVE_TO_TOP = 1;
    private static final String TAG = RecentsAnimationController.class.getSimpleName();
    private final RecentsAnimationCallbacks mCallbacks;
    private boolean mCancelDeferredWithScreenshot;
    private boolean mCancelOnNextTransitionStart;
    private volatile boolean mCanceled;
    private DisplayContent mDisplayContent;
    private final int mDisplayId;
    private boolean mInputConsumerEnabled;
    boolean mIsAddingTaskToTargets;
    volatile boolean mIsTranslucentActivityAboveLauncher;
    boolean mIsTranslucentActivityIsClosing;
    private boolean mLinkedToDeathOfRunner;
    private ActivityRecord mNavBarAttachedApp;
    private boolean mNavigationBarAttachedToApp;
    private boolean mRequestDeferCancelUntilNextTransition;
    private IRecentsAnimationRunner mRunner;
    private final WindowManagerService mService;
    boolean mShouldAttachNavBarToAppDuringTransition;
    private ActivityRecord mTargetActivityRecord;
    private int mTargetActivityType;
    private final ArrayList<TaskAnimationAdapter> mPendingAnimations = new ArrayList<>();
    private final IntArray mPendingNewTaskTargets = new IntArray(0);
    private final ArrayList<WallpaperAnimationAdapter> mPendingWallpaperAnimations = new ArrayList<>();
    private boolean mWillFinishToHome = false;
    private final Runnable mFailsafeRunnable = new Runnable() { // from class: com.android.server.wm.RecentsAnimationController$$ExternalSyntheticLambda5
        @Override // java.lang.Runnable
        public final void run() {
            RecentsAnimationController.this.onFailsafe();
        }
    };
    private boolean mPendingStart = true;
    private final Rect mTmpRect = new Rect();
    private int mPendingCancelWithScreenshotReorderMode = 2;
    private final ArrayList<RemoteAnimationTarget> mPendingTaskAppears = new ArrayList<>();
    final WindowManagerInternal.AppTransitionListener mAppTransitionListener = new WindowManagerInternal.AppTransitionListener() { // from class: com.android.server.wm.RecentsAnimationController.1
        @Override // com.android.server.wm.WindowManagerInternal.AppTransitionListener
        public int onAppTransitionStartingLocked(boolean keyguardGoingAway, boolean keyguardOccluding, long duration, long statusBarAnimationStartTime, long statusBarAnimationDuration) {
            continueDeferredCancel();
            return 0;
        }

        @Override // com.android.server.wm.WindowManagerInternal.AppTransitionListener
        public void onAppTransitionCancelledLocked(boolean keyguardGoingAway) {
            continueDeferredCancel();
        }

        private void continueDeferredCancel() {
            RecentsAnimationController.this.mDisplayContent.mAppTransition.unregisterListener(this);
            if (!RecentsAnimationController.this.mCanceled && RecentsAnimationController.this.mCancelOnNextTransitionStart) {
                RecentsAnimationController.this.mCancelOnNextTransitionStart = false;
                RecentsAnimationController recentsAnimationController = RecentsAnimationController.this;
                recentsAnimationController.cancelAnimationWithScreenshot(recentsAnimationController.mCancelDeferredWithScreenshot);
            }
        }
    };
    final IOnBackInvokedCallback mBackCallback = new IOnBackInvokedCallback.Stub() { // from class: com.android.server.wm.RecentsAnimationController.2
        public void onBackStarted() {
        }

        public void onBackProgressed(BackEvent backEvent) {
        }

        public void onBackCancelled() {
        }

        public void onBackInvoked() {
            sendBackEvent(0);
            sendBackEvent(1);
        }

        private void sendBackEvent(int action) {
            if (RecentsAnimationController.this.mTargetActivityRecord == null) {
                return;
            }
            long when = SystemClock.uptimeMillis();
            KeyEvent ev = new KeyEvent(when, when, action, 4, 0, 0, -1, 0, 72, 257);
            ev.setDisplayId(RecentsAnimationController.this.mTargetActivityRecord.getDisplayId());
            InputManager.getInstance().injectInputEvent(ev, 0);
        }
    };
    private final IRecentsAnimationController mController = new IRecentsAnimationController.Stub() { // from class: com.android.server.wm.RecentsAnimationController.3
        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [280=4] */
        public TaskSnapshot screenshotTask(int taskId) {
            if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
                long protoLogParam0 = taskId;
                boolean protoLogParam1 = RecentsAnimationController.this.mCanceled;
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, 184362060, 13, (String) null, new Object[]{Long.valueOf(protoLogParam0), Boolean.valueOf(protoLogParam1)});
            }
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (RecentsAnimationController.this.mService.getWindowManagerLock()) {
                    if (RecentsAnimationController.this.mCanceled) {
                        return null;
                    }
                    for (int i = RecentsAnimationController.this.mPendingAnimations.size() - 1; i >= 0; i--) {
                        TaskAnimationAdapter adapter = (TaskAnimationAdapter) RecentsAnimationController.this.mPendingAnimations.get(i);
                        Task task = adapter.mTask;
                        if (task.mTaskId == taskId) {
                            TaskSnapshotController snapshotController = RecentsAnimationController.this.mService.mTaskSnapshotController;
                            ArraySet<Task> tasks = Sets.newArraySet(new Task[]{task});
                            snapshotController.snapshotTasks(tasks);
                            snapshotController.addSkipClosingAppSnapshotTasks(tasks);
                            return snapshotController.getSnapshot(taskId, task.mUserId, false, false);
                        }
                    }
                    return null;
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setFinishTaskTransaction(int taskId, PictureInPictureSurfaceTransaction finishTransaction, SurfaceControl overlay) {
            if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
                long protoLogParam0 = taskId;
                String protoLogParam1 = String.valueOf(finishTransaction);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, -163974242, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0), protoLogParam1});
            }
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (RecentsAnimationController.this.mService.getWindowManagerLock()) {
                    int i = RecentsAnimationController.this.mPendingAnimations.size() - 1;
                    while (true) {
                        if (i < 0) {
                            break;
                        }
                        TaskAnimationAdapter taskAdapter = (TaskAnimationAdapter) RecentsAnimationController.this.mPendingAnimations.get(i);
                        if (taskAdapter.mTask.mTaskId != taskId) {
                            i--;
                        } else {
                            taskAdapter.mFinishTransaction = finishTransaction;
                            taskAdapter.mFinishOverlay = overlay;
                            break;
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void finish(boolean moveHomeToTop, boolean sendUserLeaveHint) {
            int i = 1;
            if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
                boolean protoLogParam1 = RecentsAnimationController.this.mCanceled;
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, -445944810, 15, (String) null, new Object[]{Boolean.valueOf(moveHomeToTop), Boolean.valueOf(protoLogParam1)});
            }
            long token = Binder.clearCallingIdentity();
            try {
                RecentsAnimationCallbacks recentsAnimationCallbacks = RecentsAnimationController.this.mCallbacks;
                if (!moveHomeToTop) {
                    i = 2;
                }
                recentsAnimationCallbacks.onAnimationFinished(i, sendUserLeaveHint);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setAnimationTargetsBehindSystemBars(boolean behindSystemBars) throws RemoteException {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (RecentsAnimationController.this.mService.getWindowManagerLock()) {
                    for (int i = RecentsAnimationController.this.mPendingAnimations.size() - 1; i >= 0; i--) {
                        Task task = ((TaskAnimationAdapter) RecentsAnimationController.this.mPendingAnimations.get(i)).mTask;
                        if (task.getActivityType() != RecentsAnimationController.this.mTargetActivityType) {
                            task.setCanAffectSystemUiFlags(behindSystemBars);
                        }
                    }
                    InputMethodManagerInternal.get().maybeFinishStylusHandwriting();
                    if (!behindSystemBars) {
                        if (!RecentsAnimationController.this.mDisplayContent.isImeAttachedToApp()) {
                            InputMethodManagerInternal inputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
                            if (inputMethodManagerInternal != null) {
                                inputMethodManagerInternal.hideCurrentInputMethod(18);
                            }
                        } else {
                            InputMethodManagerInternal.get().updateImeWindowStatus(true);
                        }
                    }
                    RecentsAnimationController.this.mService.mWindowPlacerLocked.requestTraversal();
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setInputConsumerEnabled(boolean enabled) {
            if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
                String protoLogParam0 = String.valueOf(enabled);
                boolean protoLogParam1 = RecentsAnimationController.this.mCanceled;
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, -1219773477, 12, (String) null, new Object[]{protoLogParam0, Boolean.valueOf(protoLogParam1)});
            }
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (RecentsAnimationController.this.mService.getWindowManagerLock()) {
                    if (RecentsAnimationController.this.mCanceled) {
                        return;
                    }
                    RecentsAnimationController.this.mInputConsumerEnabled = enabled;
                    InputMonitor inputMonitor = RecentsAnimationController.this.mDisplayContent.getInputMonitor();
                    inputMonitor.updateInputWindowsLw(true);
                    RecentsAnimationController.this.mService.scheduleAnimationLocked();
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void hideCurrentInputMethod() {
        }

        public void setDeferCancelUntilNextTransition(boolean defer, boolean screenshot) {
            synchronized (RecentsAnimationController.this.mService.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    RecentsAnimationController.this.setDeferredCancel(defer, screenshot);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        public void cleanupScreenshot() {
            long token = Binder.clearCallingIdentity();
            try {
                RecentsAnimationController.this.continueDeferredCancelAnimation();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setWillFinishToHome(boolean willFinishToHome) {
            synchronized (RecentsAnimationController.this.mService.getWindowManagerLock()) {
                RecentsAnimationController.this.setWillFinishToHome(willFinishToHome);
            }
        }

        public boolean removeTask(int taskId) {
            boolean removeTaskInternal;
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (RecentsAnimationController.this.mService.getWindowManagerLock()) {
                    removeTaskInternal = RecentsAnimationController.this.removeTaskInternal(taskId);
                }
                return removeTaskInternal;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void detachNavigationBarFromApp(boolean moveHomeToTop) {
            boolean z;
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (RecentsAnimationController.this.mService.getWindowManagerLock()) {
                    RecentsAnimationController recentsAnimationController = RecentsAnimationController.this;
                    if (!moveHomeToTop && !recentsAnimationController.mIsAddingTaskToTargets) {
                        z = false;
                        recentsAnimationController.restoreNavigationBarFromApp(z);
                        RecentsAnimationController.this.mService.mWindowPlacerLocked.requestTraversal();
                    }
                    z = true;
                    recentsAnimationController.restoreNavigationBarFromApp(z);
                    RecentsAnimationController.this.mService.mWindowPlacerLocked.requestTraversal();
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void animateNavigationBarToApp(long duration) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (RecentsAnimationController.this.mService.getWindowManagerLock()) {
                    RecentsAnimationController.this.animateNavigationBarForAppLaunch(duration);
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    };
    final StatusBarManagerInternal mStatusBar = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);

    /* loaded from: classes2.dex */
    public interface RecentsAnimationCallbacks {
        void onAnimationFinished(int i, boolean z);
    }

    /* loaded from: classes2.dex */
    public @interface ReorderMode {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RecentsAnimationController(WindowManagerService service, IRecentsAnimationRunner remoteAnimationRunner, RecentsAnimationCallbacks callbacks, int displayId) {
        this.mService = service;
        this.mRunner = remoteAnimationRunner;
        this.mCallbacks = callbacks;
        this.mDisplayId = displayId;
        DisplayContent displayContent = service.mRoot.getDisplayContent(displayId);
        this.mDisplayContent = displayContent;
        this.mShouldAttachNavBarToAppDuringTransition = displayContent.getDisplayPolicy().shouldAttachNavBarToAppDuringTransition();
    }

    public void initialize(int targetActivityType, SparseBooleanArray recentTaskIds, ActivityRecord targetActivity) {
        boolean z;
        this.mTargetActivityType = targetActivityType;
        this.mDisplayContent.mAppTransition.registerListenerLocked(this.mAppTransitionListener);
        ArrayList<Task> visibleTasks = this.mDisplayContent.getDefaultTaskDisplayArea().getVisibleTasks();
        Task targetRootTask = this.mDisplayContent.getDefaultTaskDisplayArea().getRootTask(0, targetActivityType);
        if (targetRootTask != null) {
            PooledConsumer c = PooledLambda.obtainConsumer(new BiConsumer() { // from class: com.android.server.wm.RecentsAnimationController$$ExternalSyntheticLambda6
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    RecentsAnimationController.lambda$initialize$0((Task) obj, (ArrayList) obj2);
                }
            }, PooledLambda.__(Task.class), visibleTasks);
            targetRootTask.forAllLeafTasks(c, true);
            c.recycle();
        }
        int taskCount = visibleTasks.size();
        if (taskCount > 2) {
            z = false;
        } else {
            z = true;
        }
        this.mIsTranslucentActivityAboveLauncher = z;
        for (int i = taskCount - 1; i >= 0; i--) {
            final Task task = visibleTasks.get(i);
            if (!skipAnimation(task)) {
                addAnimation(task, !recentTaskIds.get(task.mTaskId), false, new SurfaceAnimator.OnAnimationFinishedCallback() { // from class: com.android.server.wm.RecentsAnimationController$$ExternalSyntheticLambda7
                    @Override // com.android.server.wm.SurfaceAnimator.OnAnimationFinishedCallback
                    public final void onAnimationFinished(int i2, AnimationAdapter animationAdapter) {
                        Task.this.forAllWindows(new Consumer() { // from class: com.android.server.wm.RecentsAnimationController$$ExternalSyntheticLambda3
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                ((WindowState) obj).onAnimationFinished(i2, animationAdapter);
                            }
                        }, true);
                    }
                });
            }
        }
        if (!this.mIsTranslucentActivityAboveLauncher && this.mPendingAnimations.size() == 2) {
            this.mIsTranslucentActivityAboveLauncher = true;
        }
        if (this.mPendingAnimations.isEmpty()) {
            cancelAnimation(2, "initialize-noVisibleTasks");
            return;
        }
        try {
            linkToDeathOfRunner();
            attachNavigationBarToApp();
            if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
                String protoLogParam0 = String.valueOf(targetActivity.getName());
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, 1519757176, 0, (String) null, new Object[]{protoLogParam0});
            }
            this.mTargetActivityRecord = targetActivity;
            if (targetActivity.windowsCanBeWallpaperTarget()) {
                this.mDisplayContent.pendingLayoutChanges |= 4;
                this.mDisplayContent.setLayoutNeeded();
            }
            this.mService.mWindowPlacerLocked.performSurfacePlacement();
            this.mDisplayContent.mFixedRotationTransitionListener.onStartRecentsAnimation(targetActivity);
            StatusBarManagerInternal statusBarManagerInternal = this.mStatusBar;
            if (statusBarManagerInternal != null) {
                statusBarManagerInternal.onRecentsAnimationStateChanged(true);
            }
        } catch (RemoteException e) {
            cancelAnimation(2, "initialize-failedToLinkToDeath");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$initialize$0(Task t, ArrayList outList) {
        if (outList.contains(t)) {
            return;
        }
        outList.add(t);
    }

    private boolean skipAnimation(Task task) {
        WindowConfiguration config = task.getWindowConfiguration();
        return task.isAlwaysOnTop() || config.tasksAreFloating();
    }

    TaskAnimationAdapter addAnimation(Task task, boolean isRecentTaskInvisible) {
        return addAnimation(task, isRecentTaskInvisible, false, null);
    }

    TaskAnimationAdapter addAnimation(Task task, boolean isRecentTaskInvisible, boolean hidden, SurfaceAnimator.OnAnimationFinishedCallback finishedCallback) {
        if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
            String protoLogParam0 = String.valueOf(task.getName());
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, 302992539, 0, (String) null, new Object[]{protoLogParam0});
        }
        TaskAnimationAdapter taskAdapter = new TaskAnimationAdapter(task, isRecentTaskInvisible);
        task.startAnimation(task.getPendingTransaction(), taskAdapter, hidden, 8, finishedCallback);
        task.commitPendingTransaction();
        this.mPendingAnimations.add(taskAdapter);
        return taskAdapter;
    }

    void removeAnimation(TaskAnimationAdapter taskAdapter) {
        if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
            long protoLogParam0 = taskAdapter.mTask.mTaskId;
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, 83950285, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
        }
        taskAdapter.onRemove();
        this.mPendingAnimations.remove(taskAdapter);
    }

    void removeWallpaperAnimation(WallpaperAnimationAdapter wallpaperAdapter) {
        if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, -1768557332, 0, (String) null, (Object[]) null);
        }
        wallpaperAdapter.getLeashFinishedCallback().onAnimationFinished(wallpaperAdapter.getLastAnimationType(), wallpaperAdapter);
        this.mPendingWallpaperAnimations.remove(wallpaperAdapter);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startAnimation() {
        RemoteAnimationTarget[] appTargets;
        Rect contentInsets;
        if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
            boolean protoLogParam0 = this.mPendingStart;
            boolean protoLogParam1 = this.mCanceled;
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, 17696244, 15, (String) null, new Object[]{Boolean.valueOf(protoLogParam0), Boolean.valueOf(protoLogParam1)});
        }
        boolean protoLogParam02 = this.mPendingStart;
        if (!protoLogParam02 || this.mCanceled) {
            return;
        }
        try {
            appTargets = createAppAnimations();
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to start recents animation", e);
        }
        if (appTargets.length == 0) {
            cancelAnimation(2, "startAnimation-noAppWindows");
            return;
        }
        RemoteAnimationTarget[] wallpaperTargets = createWallpaperAnimations();
        this.mPendingStart = false;
        WindowState targetAppMainWindow = getTargetAppMainWindow();
        if (targetAppMainWindow != null) {
            contentInsets = targetAppMainWindow.getInsetsStateWithVisibilityOverride().calculateInsets(this.mTargetActivityRecord.getBounds(), WindowInsets.Type.systemBars(), false).toRect();
        } else {
            this.mService.getStableInsets(this.mDisplayId, this.mTmpRect);
            contentInsets = this.mTmpRect;
        }
        this.mRunner.onAnimationStart(this.mController, appTargets, wallpaperTargets, contentInsets, (Rect) null);
        if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
            String protoLogParam03 = String.valueOf(this.mPendingAnimations.stream().map(new Function() { // from class: com.android.server.wm.RecentsAnimationController$$ExternalSyntheticLambda2
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    Integer valueOf;
                    valueOf = Integer.valueOf(((RecentsAnimationController.TaskAnimationAdapter) obj).mTask.mTaskId);
                    return valueOf;
                }
            }).collect(Collectors.toList()));
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, -1207757583, 0, (String) null, new Object[]{protoLogParam03});
        }
        if (this.mTargetActivityRecord != null) {
            ArrayMap<WindowContainer, Integer> reasons = new ArrayMap<>(1);
            reasons.put(this.mTargetActivityRecord, 5);
            this.mService.mAtmService.mTaskSupervisor.getActivityMetricsLogger().notifyTransitionStarting(reasons);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isNavigationBarAttachedToApp() {
        return this.mNavigationBarAttachedToApp;
    }

    WindowState getNavigationBarWindow() {
        return this.mDisplayContent.getDisplayPolicy().getNavigationBar();
    }

    private void attachNavigationBarToApp() {
        if (!this.mShouldAttachNavBarToAppDuringTransition || this.mDisplayContent.getAsyncRotationController() != null) {
            return;
        }
        int i = this.mPendingAnimations.size() - 1;
        while (true) {
            if (i < 0) {
                break;
            }
            TaskAnimationAdapter adapter = this.mPendingAnimations.get(i);
            Task task = adapter.mTask;
            if (task.isActivityTypeHomeOrRecents()) {
                i--;
            } else {
                this.mNavBarAttachedApp = task.getTopVisibleActivity();
                break;
            }
        }
        WindowState navWindow = getNavigationBarWindow();
        if (this.mNavBarAttachedApp == null || navWindow == null || navWindow.mToken == null) {
            return;
        }
        this.mNavigationBarAttachedToApp = true;
        navWindow.mToken.cancelAnimation();
        SurfaceControl.Transaction t = navWindow.mToken.getPendingTransaction();
        SurfaceControl navSurfaceControl = navWindow.mToken.getSurfaceControl();
        navWindow.setSurfaceTranslationY(-this.mNavBarAttachedApp.getBounds().top);
        t.reparent(navSurfaceControl, this.mNavBarAttachedApp.getSurfaceControl());
        t.show(navSurfaceControl);
        WindowContainer imeContainer = this.mDisplayContent.getImeContainer();
        if (imeContainer.isVisible()) {
            t.setRelativeLayer(navSurfaceControl, imeContainer.getSurfaceControl(), 1);
        } else {
            t.setLayer(navSurfaceControl, Integer.MAX_VALUE);
        }
        StatusBarManagerInternal statusBarManagerInternal = this.mStatusBar;
        if (statusBarManagerInternal != null) {
            statusBarManagerInternal.setNavigationBarLumaSamplingEnabled(this.mDisplayId, false);
        }
    }

    void restoreNavigationBarFromApp(boolean animate) {
        if (!this.mNavigationBarAttachedToApp) {
            return;
        }
        this.mNavigationBarAttachedToApp = false;
        StatusBarManagerInternal statusBarManagerInternal = this.mStatusBar;
        if (statusBarManagerInternal != null) {
            statusBarManagerInternal.setNavigationBarLumaSamplingEnabled(this.mDisplayId, true);
        }
        WindowState navWindow = getNavigationBarWindow();
        if (navWindow == null) {
            return;
        }
        navWindow.setSurfaceTranslationY(0);
        WindowToken navToken = navWindow.mToken;
        if (navToken == null) {
            return;
        }
        SurfaceControl.Transaction t = this.mDisplayContent.getPendingTransaction();
        WindowContainer parent = navToken.getParent();
        t.setLayer(navToken.getSurfaceControl(), navToken.getLastLayer());
        if (animate) {
            NavBarFadeAnimationController controller = new NavBarFadeAnimationController(this.mDisplayContent);
            controller.fadeWindowToken(true);
            return;
        }
        t.reparent(navToken.getSurfaceControl(), parent.getSurfaceControl());
    }

    void animateNavigationBarForAppLaunch(long duration) {
        if (!this.mShouldAttachNavBarToAppDuringTransition || this.mDisplayContent.getAsyncRotationController() != null || this.mNavigationBarAttachedToApp || this.mNavBarAttachedApp == null) {
            return;
        }
        NavBarFadeAnimationController controller = new NavBarFadeAnimationController(this.mDisplayContent);
        controller.fadeOutAndInSequentially(duration, null, this.mNavBarAttachedApp.getSurfaceControl());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addTaskToTargets(Task task, SurfaceAnimator.OnAnimationFinishedCallback finishedCallback) {
        if (this.mRunner != null) {
            this.mIsAddingTaskToTargets = task != null;
            this.mNavBarAttachedApp = task == null ? null : task.getTopVisibleActivity();
            if (isAnimatingTask(task) || skipAnimation(task)) {
                return;
            }
            collectTaskRemoteAnimations(task, 0, finishedCallback);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendTasksAppeared() {
        if (this.mRunner == null) {
            return;
        }
        if (!this.mPendingTaskAppears.isEmpty() || this.mIsTranslucentActivityIsClosing) {
            if (this.mIsTranslucentActivityIsClosing) {
                String str = TAG;
                Slog.d(str, "sendTasksAppeared mIsTranslucentActivityIsClosing = " + this.mIsTranslucentActivityIsClosing);
                this.mIsTranslucentActivityIsClosing = false;
                if (this.mIsTranslucentActivityAboveLauncher) {
                    this.mIsTranslucentActivityAboveLauncher = false;
                    if (this.mPendingTaskAppears.isEmpty()) {
                        Slog.d(str, "skip sendTasksAppeared, cause translucent activity is above launcher");
                        return;
                    }
                }
            }
            try {
                RemoteAnimationTarget[] targets = (RemoteAnimationTarget[]) this.mPendingTaskAppears.toArray(new RemoteAnimationTarget[0]);
                this.mRunner.onTasksAppeared(targets);
                this.mPendingTaskAppears.clear();
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to report task appeared", e);
            }
        }
    }

    private void collectTaskRemoteAnimations(Task task, final int mode, final SurfaceAnimator.OnAnimationFinishedCallback finishedCallback) {
        final SparseBooleanArray recentTaskIds = this.mService.mAtmService.getRecentTasks().getRecentTaskIds();
        task.forAllLeafTasks(new Consumer() { // from class: com.android.server.wm.RecentsAnimationController$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RecentsAnimationController.this.m8159x8c910ecc(recentTaskIds, finishedCallback, mode, (Task) obj);
            }
        }, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$collectTaskRemoteAnimations$4$com-android-server-wm-RecentsAnimationController  reason: not valid java name */
    public /* synthetic */ void m8159x8c910ecc(SparseBooleanArray recentTaskIds, SurfaceAnimator.OnAnimationFinishedCallback finishedCallback, int mode, Task leafTask) {
        if (!leafTask.shouldBeVisible(null)) {
            return;
        }
        int taskId = leafTask.mTaskId;
        TaskAnimationAdapter adapter = addAnimation(leafTask, !recentTaskIds.get(taskId), true, finishedCallback);
        this.mPendingNewTaskTargets.add(taskId);
        RemoteAnimationTarget target = adapter.createRemoteAnimationTarget(taskId, mode);
        if (target != null) {
            this.mPendingTaskAppears.add(target);
            if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
                String protoLogParam0 = String.valueOf(target);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, 1151072840, 0, (String) null, new Object[]{protoLogParam0});
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void logRecentsAnimationStartTime(final int durationMs) {
        BackgroundThread.getHandler().postDelayed(new Runnable() { // from class: com.android.server.wm.RecentsAnimationController$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                RecentsAnimationController.this.m8161x50feae16(durationMs);
            }
        }, LATENCY_TRACKER_LOG_DELAY_MS);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$logRecentsAnimationStartTime$5$com-android-server-wm-RecentsAnimationController  reason: not valid java name */
    public /* synthetic */ void m8161x50feae16(int durationMs) {
        if (!this.mCanceled) {
            this.mService.mLatencyTracker.logAction(8, durationMs);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean removeTaskInternal(int taskId) {
        for (int i = this.mPendingAnimations.size() - 1; i >= 0; i--) {
            TaskAnimationAdapter target = this.mPendingAnimations.get(i);
            if (target.mTask.mTaskId == taskId && target.mTask.isOnTop()) {
                removeAnimation(target);
                int taskIndex = this.mPendingNewTaskTargets.indexOf(taskId);
                if (taskIndex != -1) {
                    this.mPendingNewTaskTargets.remove(taskIndex);
                }
                return true;
            }
        }
        return false;
    }

    private RemoteAnimationTarget[] createAppAnimations() {
        ArrayList<RemoteAnimationTarget> targets = new ArrayList<>();
        for (int i = this.mPendingAnimations.size() - 1; i >= 0; i--) {
            TaskAnimationAdapter taskAdapter = this.mPendingAnimations.get(i);
            RemoteAnimationTarget target = taskAdapter.createRemoteAnimationTarget(-1, -1);
            if (target != null) {
                targets.add(target);
            } else {
                removeAnimation(taskAdapter);
            }
        }
        int i2 = targets.size();
        return (RemoteAnimationTarget[]) targets.toArray(new RemoteAnimationTarget[i2]);
    }

    private RemoteAnimationTarget[] createWallpaperAnimations() {
        if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, 1495525537, 0, (String) null, (Object[]) null);
        }
        return WallpaperAnimationAdapter.startWallpaperAnimations(this.mDisplayContent, 0L, 0L, new Consumer() { // from class: com.android.server.wm.RecentsAnimationController$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RecentsAnimationController.this.m8160xac01c679((WallpaperAnimationAdapter) obj);
            }
        }, this.mPendingWallpaperAnimations);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$createWallpaperAnimations$6$com-android-server-wm-RecentsAnimationController  reason: not valid java name */
    public /* synthetic */ void m8160xac01c679(WallpaperAnimationAdapter adapter) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mPendingWallpaperAnimations.remove(adapter);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forceCancelAnimation(int reorderMode, String reason) {
        if (!this.mCanceled) {
            cancelAnimation(reorderMode, reason);
        } else {
            continueDeferredCancelAnimation();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancelAnimation(int reorderMode, String reason) {
        cancelAnimation(reorderMode, false, reason);
    }

    void cancelAnimationWithScreenshot(boolean screenshot) {
        cancelAnimation(0, screenshot, "rootTaskOrderChanged");
    }

    public void cancelAnimationForHomeStart() {
        int reorderMode;
        if (this.mTargetActivityType == 2 && this.mWillFinishToHome) {
            reorderMode = 1;
        } else {
            reorderMode = 0;
        }
        cancelAnimation(reorderMode, true, "cancelAnimationForHomeStart");
    }

    public void cancelAnimationForDisplayChange() {
        cancelAnimation(this.mWillFinishToHome ? 1 : 2, true, "cancelAnimationForDisplayChange");
    }

    private void cancelAnimation(int reorderMode, boolean screenshot, String reason) {
        if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
            String protoLogParam0 = String.valueOf(reason);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, 1525976603, 0, (String) null, new Object[]{protoLogParam0});
        }
        synchronized (this.mService.getWindowManagerLock()) {
            if (this.mCanceled) {
                return;
            }
            this.mService.mH.removeCallbacks(this.mFailsafeRunnable);
            this.mCanceled = true;
            if (screenshot && !this.mPendingAnimations.isEmpty()) {
                ArrayMap<Task, TaskSnapshot> snapshotMap = screenshotRecentTasks();
                this.mPendingCancelWithScreenshotReorderMode = reorderMode;
                if (!snapshotMap.isEmpty()) {
                    try {
                        int[] taskIds = new int[snapshotMap.size()];
                        TaskSnapshot[] snapshots = new TaskSnapshot[snapshotMap.size()];
                        for (int i = snapshotMap.size() - 1; i >= 0; i--) {
                            taskIds[i] = snapshotMap.keyAt(i).mTaskId;
                            snapshots[i] = snapshotMap.valueAt(i);
                        }
                        this.mRunner.onAnimationCanceled(taskIds, snapshots);
                    } catch (RemoteException e) {
                        Slog.e(TAG, "Failed to cancel recents animation", e);
                    }
                    scheduleFailsafe();
                    return;
                }
            }
            try {
                this.mRunner.onAnimationCanceled((int[]) null, (TaskSnapshot[]) null);
            } catch (RemoteException e2) {
                Slog.e(TAG, "Failed to cancel recents animation", e2);
            }
            this.mCallbacks.onAnimationFinished(reorderMode, false);
        }
    }

    void continueDeferredCancelAnimation() {
        this.mCallbacks.onAnimationFinished(this.mPendingCancelWithScreenshotReorderMode, false);
    }

    void setWillFinishToHome(boolean willFinishToHome) {
        this.mWillFinishToHome = willFinishToHome;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCancelOnNextTransitionStart() {
        this.mCancelOnNextTransitionStart = true;
    }

    void setDeferredCancel(boolean defer, boolean screenshot) {
        this.mRequestDeferCancelUntilNextTransition = defer;
        this.mCancelDeferredWithScreenshot = screenshot;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldDeferCancelUntilNextTransition() {
        return this.mRequestDeferCancelUntilNextTransition;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldDeferCancelWithScreenshot() {
        return this.mRequestDeferCancelUntilNextTransition && this.mCancelDeferredWithScreenshot;
    }

    private ArrayMap<Task, TaskSnapshot> screenshotRecentTasks() {
        TaskSnapshotController snapshotController = this.mService.mTaskSnapshotController;
        ArrayMap<Task, TaskSnapshot> snapshotMap = new ArrayMap<>();
        for (int i = this.mPendingAnimations.size() - 1; i >= 0; i--) {
            TaskAnimationAdapter adapter = this.mPendingAnimations.get(i);
            Task task = adapter.mTask;
            snapshotController.recordTaskSnapshot(task, false);
            TaskSnapshot snapshot = snapshotController.getSnapshot(task.mTaskId, task.mUserId, false, false);
            if (snapshot != null) {
                snapshotMap.put(task, snapshot);
                adapter.setSnapshotOverlay(snapshot);
            }
        }
        snapshotController.addSkipClosingAppSnapshotTasks(snapshotMap.keySet());
        return snapshotMap;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cleanupAnimation(int reorderMode) {
        if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
            long protoLogParam0 = this.mPendingAnimations.size();
            long protoLogParam1 = reorderMode;
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, -1434147454, 5, (String) null, new Object[]{Long.valueOf(protoLogParam0), Long.valueOf(protoLogParam1)});
        }
        if (reorderMode != 2 && this.mTargetActivityRecord != this.mDisplayContent.topRunningActivity()) {
            this.mDisplayContent.mFixedRotationTransitionListener.notifyRecentsWillBeTop();
        }
        for (int i = this.mPendingAnimations.size() - 1; i >= 0; i--) {
            TaskAnimationAdapter taskAdapter = this.mPendingAnimations.get(i);
            if (reorderMode == 1 || reorderMode == 0) {
                taskAdapter.mTask.dontAnimateDimExit();
            }
            removeAnimation(taskAdapter);
            taskAdapter.onCleanup();
        }
        this.mPendingNewTaskTargets.clear();
        this.mPendingTaskAppears.clear();
        for (int i2 = this.mPendingWallpaperAnimations.size() - 1; i2 >= 0; i2--) {
            WallpaperAnimationAdapter wallpaperAdapter = this.mPendingWallpaperAnimations.get(i2);
            removeWallpaperAnimation(wallpaperAdapter);
        }
        restoreNavigationBarFromApp(reorderMode == 1 || this.mIsAddingTaskToTargets);
        this.mService.mH.removeCallbacks(this.mFailsafeRunnable);
        this.mDisplayContent.mAppTransition.unregisterListener(this.mAppTransitionListener);
        unlinkToDeathOfRunner();
        this.mRunner = null;
        this.mCanceled = true;
        if (reorderMode == 2 && !this.mIsAddingTaskToTargets) {
            InputMethodManagerInternal.get().updateImeWindowStatus(false);
        }
        InputMonitor inputMonitor = this.mDisplayContent.getInputMonitor();
        inputMonitor.updateInputWindowsLw(true);
        if (this.mTargetActivityRecord != null && (reorderMode == 1 || reorderMode == 0)) {
            this.mDisplayContent.mAppTransition.notifyAppTransitionFinishedLocked(this.mTargetActivityRecord.token);
        }
        this.mDisplayContent.mFixedRotationTransitionListener.onFinishRecentsAnimation();
        StatusBarManagerInternal statusBarManagerInternal = this.mStatusBar;
        if (statusBarManagerInternal != null) {
            statusBarManagerInternal.onRecentsAnimationStateChanged(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleFailsafe() {
        this.mService.mH.postDelayed(this.mFailsafeRunnable, 1000L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onFailsafe() {
        forceCancelAnimation(this.mWillFinishToHome ? 1 : 2, "onFailsafe");
    }

    private void linkToDeathOfRunner() throws RemoteException {
        if (!this.mLinkedToDeathOfRunner) {
            this.mRunner.asBinder().linkToDeath(this, 0);
            this.mLinkedToDeathOfRunner = true;
        }
    }

    private void unlinkToDeathOfRunner() {
        if (this.mLinkedToDeathOfRunner) {
            this.mRunner.asBinder().unlinkToDeath(this, 0);
            this.mLinkedToDeathOfRunner = false;
        }
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        forceCancelAnimation(2, "binderDied");
        synchronized (this.mService.getWindowManagerLock()) {
            InputMonitor inputMonitor = this.mDisplayContent.getInputMonitor();
            inputMonitor.destroyInputConsumer("recents_animation_input_consumer");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void checkAnimationReady(WallpaperController wallpaperController) {
        if (this.mPendingStart) {
            boolean wallpaperReady = !isTargetOverWallpaper() || (wallpaperController.getWallpaperTarget() != null && wallpaperController.wallpaperTransitionReady());
            if (wallpaperReady) {
                this.mService.getRecentsAnimationController().startAnimation();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isWallpaperVisible(WindowState w) {
        return w != null && w.mAttrs.type == 1 && ((w.mActivityRecord != null && this.mTargetActivityRecord == w.mActivityRecord) || isAnimatingTask(w.getTask())) && isTargetOverWallpaper() && w.isOnScreen();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldApplyInputConsumer(ActivityRecord activity) {
        return this.mInputConsumerEnabled && activity != null && !isTargetApp(activity) && isAnimatingApp(activity);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateInputConsumerForApp(InputWindowHandle inputWindowHandle) {
        WindowState targetAppMainWindow = getTargetAppMainWindow();
        if (targetAppMainWindow != null) {
            targetAppMainWindow.getBounds(this.mTmpRect);
            inputWindowHandle.touchableRegion.set(this.mTmpRect);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTargetApp(ActivityRecord activity) {
        ActivityRecord activityRecord = this.mTargetActivityRecord;
        return activityRecord != null && activity == activityRecord;
    }

    private boolean isTargetOverWallpaper() {
        ActivityRecord activityRecord = this.mTargetActivityRecord;
        if (activityRecord == null) {
            return false;
        }
        return activityRecord.windowsCanBeWallpaperTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState getTargetAppMainWindow() {
        ActivityRecord activityRecord = this.mTargetActivityRecord;
        if (activityRecord == null) {
            return null;
        }
        return activityRecord.findMainWindow();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public OnBackInvokedCallbackInfo getBackInvokedInfo() {
        return new OnBackInvokedCallbackInfo(this.mBackCallback, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayArea getTargetAppDisplayArea() {
        ActivityRecord activityRecord = this.mTargetActivityRecord;
        if (activityRecord == null) {
            return null;
        }
        return activityRecord.getDisplayArea();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAnimatingTask(Task task) {
        for (int i = this.mPendingAnimations.size() - 1; i >= 0; i--) {
            if (task == this.mPendingAnimations.get(i).mTask) {
                return true;
            }
        }
        return false;
    }

    boolean isAnimatingWallpaper(WallpaperWindowToken token) {
        for (int i = this.mPendingWallpaperAnimations.size() - 1; i >= 0; i--) {
            if (token == this.mPendingWallpaperAnimations.get(i).getToken()) {
                return true;
            }
        }
        return false;
    }

    private boolean isAnimatingApp(ActivityRecord activity) {
        for (int i = this.mPendingAnimations.size() - 1; i >= 0; i--) {
            if (activity.isDescendantOf(this.mPendingAnimations.get(i).mTask)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldIgnoreForAccessibility(WindowState windowState) {
        Task task = windowState.getTask();
        return (task == null || !isAnimatingTask(task) || isTargetApp(windowState.mActivityRecord)) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void linkFixedRotationTransformIfNeeded(WindowToken wallpaper) {
        ActivityRecord activityRecord = this.mTargetActivityRecord;
        if (activityRecord == null) {
            return;
        }
        wallpaper.linkFixedRotationTransform(activityRecord);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class TaskAnimationAdapter implements AnimationAdapter {
        private final Rect mBounds;
        private SurfaceAnimator.OnAnimationFinishedCallback mCapturedFinishCallback;
        private SurfaceControl mCapturedLeash;
        private SurfaceControl mFinishOverlay;
        private PictureInPictureSurfaceTransaction mFinishTransaction;
        private final boolean mIsRecentTaskInvisible;
        private int mLastAnimationType;
        private final Rect mLocalBounds;
        private SurfaceControl mSnapshotOverlay;
        private RemoteAnimationTarget mTarget;
        private final Task mTask;

        TaskAnimationAdapter(Task task, boolean isRecentTaskInvisible) {
            Rect rect = new Rect();
            this.mBounds = rect;
            Rect rect2 = new Rect();
            this.mLocalBounds = rect2;
            this.mTask = task;
            this.mIsRecentTaskInvisible = isRecentTaskInvisible;
            rect.set(task.getBounds());
            rect2.set(rect);
            Point tmpPos = new Point();
            task.getRelativePosition(tmpPos);
            rect2.offsetTo(tmpPos.x, tmpPos.y);
        }

        RemoteAnimationTarget createRemoteAnimationTarget(int overrideTaskId, int overrideMode) {
            WindowState mainWindow;
            int mode;
            int overrideTaskId2;
            TaskFragment topAppTaskFragment;
            TaskFragment adjacent;
            ActivityRecord topApp = this.mTask.getTopRealVisibleActivity();
            if (topApp == null) {
                topApp = this.mTask.getTopVisibleActivity();
            }
            if (topApp != null && topApp.findMainWindow() == null && (topAppTaskFragment = topApp.getTaskFragment()) != null && (adjacent = topAppTaskFragment.getAdjacentTaskFragment()) != null) {
                ActivityRecord adjacentTopApp = adjacent.getActivity(new Predicate() { // from class: com.android.server.wm.RecentsAnimationController$TaskAnimationAdapter$$ExternalSyntheticLambda0
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return RecentsAnimationController.TaskAnimationAdapter.lambda$createRemoteAnimationTarget$0((ActivityRecord) obj);
                    }
                });
                if (adjacentTopApp == null) {
                    adjacentTopApp = adjacent.getActivity(new Predicate() { // from class: com.android.server.wm.RecentsAnimationController$TaskAnimationAdapter$$ExternalSyntheticLambda1
                        @Override // java.util.function.Predicate
                        public final boolean test(Object obj) {
                            return RecentsAnimationController.TaskAnimationAdapter.lambda$createRemoteAnimationTarget$1((ActivityRecord) obj);
                        }
                    });
                }
                if (adjacentTopApp != null) {
                    Slog.d(RecentsAnimationController.TAG, "top app has no main window use adjcent top app. topApp = " + topApp + " adjacent top app = " + adjacent);
                    topApp = adjacentTopApp;
                }
            }
            if (topApp != null) {
                mainWindow = topApp.findMainWindow();
            } else {
                mainWindow = null;
            }
            if (mainWindow == null) {
                return null;
            }
            Rect insets = mainWindow.getInsetsStateWithVisibilityOverride().calculateInsets(this.mBounds, WindowInsets.Type.systemBars(), false).toRect();
            InsetUtils.addInsets(insets, mainWindow.mActivityRecord.getLetterboxInsets());
            if (overrideMode == -1) {
                if (topApp.getActivityType() == RecentsAnimationController.this.mTargetActivityType) {
                    mode = 0;
                } else {
                    mode = 1;
                }
            } else {
                mode = overrideMode;
            }
            if (overrideTaskId >= 0) {
                overrideTaskId2 = overrideTaskId;
            } else {
                overrideTaskId2 = this.mTask.mTaskId;
            }
            RemoteAnimationTarget remoteAnimationTarget = new RemoteAnimationTarget(overrideTaskId2, mode, this.mCapturedLeash, !topApp.fillsParent(), new Rect(), insets, this.mTask.getPrefixOrderIndex(), new Point(this.mBounds.left, this.mBounds.top), this.mLocalBounds, this.mBounds, this.mTask.getWindowConfiguration(), this.mIsRecentTaskInvisible, (SurfaceControl) null, (Rect) null, this.mTask.getTaskInfo(), topApp.checkEnterPictureInPictureAppOpsState());
            this.mTarget = remoteAnimationTarget;
            return remoteAnimationTarget;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ boolean lambda$createRemoteAnimationTarget$0(ActivityRecord r) {
            return !r.mIsExiting && r.isClientVisible() && r.isVisible();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ boolean lambda$createRemoteAnimationTarget$1(ActivityRecord r) {
            return !r.mIsExiting && r.isClientVisible() && r.mVisibleRequested;
        }

        void setSnapshotOverlay(TaskSnapshot snapshot) {
            HardwareBuffer buffer = snapshot.getHardwareBuffer();
            if (buffer == null) {
                return;
            }
            SurfaceSession session = new SurfaceSession();
            this.mSnapshotOverlay = RecentsAnimationController.this.mService.mSurfaceControlFactory.apply(session).setName("RecentTaskScreenshotSurface").setCallsite("TaskAnimationAdapter.setSnapshotOverlay").setFormat(buffer.getFormat()).setParent(this.mCapturedLeash).setBLASTLayer().build();
            float scale = (this.mTask.getBounds().width() * 1.0f) / buffer.getWidth();
            this.mTask.getPendingTransaction().setBuffer(this.mSnapshotOverlay, GraphicBuffer.createFromHardwareBuffer(buffer)).setColorSpace(this.mSnapshotOverlay, snapshot.getColorSpace()).setLayer(this.mSnapshotOverlay, Integer.MAX_VALUE).setMatrix(this.mSnapshotOverlay, scale, 0.0f, 0.0f, scale).show(this.mSnapshotOverlay).apply();
        }

        void onRemove() {
            if (this.mSnapshotOverlay != null) {
                this.mTask.getPendingTransaction().remove(this.mSnapshotOverlay).apply();
                this.mSnapshotOverlay = null;
            }
            this.mTask.setCanAffectSystemUiFlags(true);
            this.mCapturedFinishCallback.onAnimationFinished(this.mLastAnimationType, this);
        }

        void onCleanup() {
            SurfaceControl.Transaction pendingTransaction = this.mTask.getPendingTransaction();
            if (this.mFinishTransaction != null) {
                SurfaceControl surfaceControl = this.mFinishOverlay;
                if (surfaceControl != null) {
                    pendingTransaction.reparent(surfaceControl, this.mTask.mSurfaceControl);
                }
                PictureInPictureSurfaceTransaction.apply(this.mFinishTransaction, this.mTask.mSurfaceControl, pendingTransaction);
                this.mTask.setLastRecentsAnimationTransaction(this.mFinishTransaction, this.mFinishOverlay);
                if (RecentsAnimationController.this.mDisplayContent.isFixedRotationLaunchingApp(RecentsAnimationController.this.mTargetActivityRecord)) {
                    RecentsAnimationController.this.mDisplayContent.mPinnedTaskController.setEnterPipTransaction(this.mFinishTransaction);
                }
                this.mFinishTransaction = null;
                this.mFinishOverlay = null;
                pendingTransaction.apply();
                if (this.mTask.getActivityType() != RecentsAnimationController.this.mTargetActivityType) {
                    this.mTask.setCanAffectSystemUiFlags(false);
                }
            } else if (!this.mTask.isAttached()) {
                pendingTransaction.apply();
            }
        }

        public SurfaceControl getSnapshotOverlay() {
            return this.mSnapshotOverlay;
        }

        @Override // com.android.server.wm.AnimationAdapter
        public boolean getShowWallpaper() {
            return false;
        }

        @Override // com.android.server.wm.AnimationAdapter
        public void startAnimation(SurfaceControl animationLeash, SurfaceControl.Transaction t, int type, SurfaceAnimator.OnAnimationFinishedCallback finishCallback) {
            t.setPosition(animationLeash, this.mLocalBounds.left, this.mLocalBounds.top);
            RecentsAnimationController.this.mTmpRect.set(this.mLocalBounds);
            RecentsAnimationController.this.mTmpRect.offsetTo(0, 0);
            t.setWindowCrop(animationLeash, RecentsAnimationController.this.mTmpRect);
            this.mCapturedLeash = animationLeash;
            this.mCapturedFinishCallback = finishCallback;
            this.mLastAnimationType = type;
        }

        @Override // com.android.server.wm.AnimationAdapter
        public void onAnimationCancelled(SurfaceControl animationLeash) {
            RecentsAnimationController.this.cancelAnimation(2, "taskAnimationAdapterCanceled");
        }

        @Override // com.android.server.wm.AnimationAdapter
        public long getDurationHint() {
            return 0L;
        }

        @Override // com.android.server.wm.AnimationAdapter
        public long getStatusBarTransitionsStartTime() {
            return SystemClock.uptimeMillis();
        }

        @Override // com.android.server.wm.AnimationAdapter
        public void dump(PrintWriter pw, String prefix) {
            pw.print(prefix);
            pw.println("task=" + this.mTask);
            if (this.mTarget != null) {
                pw.print(prefix);
                pw.println("Target:");
                this.mTarget.dump(pw, prefix + "  ");
            } else {
                pw.print(prefix);
                pw.println("Target: null");
            }
            pw.println("mIsRecentTaskInvisible=" + this.mIsRecentTaskInvisible);
            pw.println("mLocalBounds=" + this.mLocalBounds);
            pw.println("mFinishTransaction=" + this.mFinishTransaction);
            pw.println("mBounds=" + this.mBounds);
            pw.println("mIsRecentTaskInvisible=" + this.mIsRecentTaskInvisible);
        }

        @Override // com.android.server.wm.AnimationAdapter
        public void dumpDebug(ProtoOutputStream proto) {
            long token = proto.start(1146756268034L);
            RemoteAnimationTarget remoteAnimationTarget = this.mTarget;
            if (remoteAnimationTarget != null) {
                remoteAnimationTarget.dumpDebug(proto, 1146756268033L);
            }
            proto.end(token);
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        String innerPrefix = prefix + "  ";
        pw.print(prefix);
        pw.println(RecentsAnimationController.class.getSimpleName() + ":");
        pw.print(innerPrefix);
        pw.println("mPendingStart=" + this.mPendingStart);
        pw.print(innerPrefix);
        pw.println("mPendingAnimations=" + this.mPendingAnimations.size());
        pw.print(innerPrefix);
        pw.println("mCanceled=" + this.mCanceled);
        pw.print(innerPrefix);
        pw.println("mInputConsumerEnabled=" + this.mInputConsumerEnabled);
        pw.print(innerPrefix);
        pw.println("mTargetActivityRecord=" + this.mTargetActivityRecord);
        pw.print(innerPrefix);
        pw.println("isTargetOverWallpaper=" + isTargetOverWallpaper());
        pw.print(innerPrefix);
        pw.println("mRequestDeferCancelUntilNextTransition=" + this.mRequestDeferCancelUntilNextTransition);
        pw.print(innerPrefix);
        pw.println("mCancelOnNextTransitionStart=" + this.mCancelOnNextTransitionStart);
        pw.print(innerPrefix);
        pw.println("mCancelDeferredWithScreenshot=" + this.mCancelDeferredWithScreenshot);
        pw.print(innerPrefix);
        pw.println("mPendingCancelWithScreenshotReorderMode=" + this.mPendingCancelWithScreenshotReorderMode);
    }
}
