package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.TaskInfo;
import android.app.WindowConfiguration;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import android.window.ITaskOrganizer;
import android.window.ITaskOrganizerController;
import android.window.StartingWindowInfo;
import android.window.StartingWindowRemovalInfo;
import android.window.TaskAppearedInfo;
import android.window.TaskSnapshot;
import android.window.WindowContainerToken;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.ArrayUtils;
import com.android.server.wm.SurfaceAnimator;
import com.android.server.wm.Task;
import com.android.server.wm.TaskOrganizerController;
import com.transsion.foldable.TranFoldingScreenManager;
import com.transsion.server.foldable.TranFoldingScreenController;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class TaskOrganizerController extends ITaskOrganizerController.Stub {
    private static final String TAG = "TaskOrganizerController";
    private static final String TRAN_AUTO_PACKAGE_NAME = "com.transsion.auto";
    private Consumer<Runnable> mDeferTaskOrgCallbacksConsumer;
    private final WindowManagerGlobalLock mGlobalLock;
    private final ActivityTaskManagerService mService;
    private ActivityManager.RunningTaskInfo mTmpTaskInfo;
    private TaskOrganizerState mTranAutoTaskOrganizerState;
    private final Object mTmpTaskInfoLock = new Object();
    private final LinkedList<ITaskOrganizer> mTaskOrganizers = new LinkedList<>();
    private final HashMap<IBinder, TaskOrganizerState> mTaskOrganizerStates = new HashMap<>();
    private final WeakHashMap<Task, ActivityManager.RunningTaskInfo> mLastSentTaskInfos = new WeakHashMap<>();
    private final ArrayList<PendingTaskEvent> mPendingTaskEvents = new ArrayList<>();
    private final HashSet<Integer> mInterceptBackPressedOnRootTasks = new HashSet<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class DeathRecipient implements IBinder.DeathRecipient {
        ITaskOrganizer mTaskOrganizer;

        DeathRecipient(ITaskOrganizer organizer) {
            this.mTaskOrganizer = organizer;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (TaskOrganizerController.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    TaskOrganizerState state = (TaskOrganizerState) TaskOrganizerController.this.mTaskOrganizerStates.get(this.mTaskOrganizer.asBinder());
                    if (state != null) {
                        state.dispose();
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class TaskOrganizerCallbacks {
        final Consumer<Runnable> mDeferTaskOrgCallbacksConsumer;
        final ITaskOrganizer mTaskOrganizer;

        TaskOrganizerCallbacks(ITaskOrganizer taskOrg, Consumer<Runnable> deferTaskOrgCallbacksConsumer) {
            this.mDeferTaskOrgCallbacksConsumer = deferTaskOrgCallbacksConsumer;
            this.mTaskOrganizer = taskOrg;
        }

        IBinder getBinder() {
            return this.mTaskOrganizer.asBinder();
        }

        SurfaceControl prepareLeash(Task task, String reason) {
            return new SurfaceControl(task.getSurfaceControl(), reason);
        }

        void onTaskAppeared(Task task) {
            if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                long protoLogParam0 = task.mTaskId;
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, 1918448345, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
            }
            ActivityManager.RunningTaskInfo taskInfo = task.getTaskInfo();
            try {
                this.mTaskOrganizer.onTaskAppeared(taskInfo, prepareLeash(task, "TaskOrganizerController.onTaskAppeared"));
            } catch (RemoteException e) {
                Slog.e(TaskOrganizerController.TAG, "Exception sending onTaskAppeared callback", e);
            }
        }

        void onTaskVanished(Task task) {
            if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                long protoLogParam0 = task.mTaskId;
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, -1364754753, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
            }
            ActivityManager.RunningTaskInfo taskInfo = task.getTaskInfo();
            try {
                this.mTaskOrganizer.onTaskVanished(taskInfo);
            } catch (RemoteException e) {
                Slog.e(TaskOrganizerController.TAG, "Exception sending onTaskVanished callback", e);
            }
        }

        void onTaskInfoChanged(Task task, ActivityManager.RunningTaskInfo taskInfo) {
            if (!task.mTaskAppearedSent) {
                return;
            }
            if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                long protoLogParam0 = task.mTaskId;
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, 302969511, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
            }
            if (!task.isOrganized()) {
                return;
            }
            try {
                this.mTaskOrganizer.onTaskInfoChanged(taskInfo);
            } catch (RemoteException e) {
                Slog.e(TaskOrganizerController.TAG, "Exception sending onTaskInfoChanged callback", e);
            }
        }

        void onBackPressedOnTaskRoot(Task task) {
            if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                long protoLogParam0 = task.mTaskId;
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, -2049725903, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
            }
            if (!task.mTaskAppearedSent || !task.isOrganized()) {
                return;
            }
            try {
                this.mTaskOrganizer.onBackPressedOnTaskRoot(task.getTaskInfo());
            } catch (Exception e) {
                Slog.e(TaskOrganizerController.TAG, "Exception sending onBackPressedOnTaskRoot callback", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class TaskOrganizerState {
        private final DeathRecipient mDeathRecipient;
        private final ArrayList<Task> mOrganizedTasks = new ArrayList<>();
        private final TaskOrganizerCallbacks mOrganizer;
        private final int mUid;

        TaskOrganizerState(ITaskOrganizer organizer, int uid) {
            Consumer consumer;
            if (TaskOrganizerController.this.mDeferTaskOrgCallbacksConsumer != null) {
                consumer = TaskOrganizerController.this.mDeferTaskOrgCallbacksConsumer;
            } else {
                final WindowAnimator windowAnimator = TaskOrganizerController.this.mService.mWindowManager.mAnimator;
                Objects.requireNonNull(windowAnimator);
                consumer = new Consumer() { // from class: com.android.server.wm.TaskOrganizerController$TaskOrganizerState$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        WindowAnimator.this.addAfterPrepareSurfacesRunnable((Runnable) obj);
                    }
                };
            }
            this.mOrganizer = new TaskOrganizerCallbacks(organizer, consumer);
            DeathRecipient deathRecipient = new DeathRecipient(organizer);
            this.mDeathRecipient = deathRecipient;
            try {
                organizer.asBinder().linkToDeath(deathRecipient, 0);
            } catch (RemoteException e) {
                Slog.e(TaskOrganizerController.TAG, "TaskOrganizer failed to register death recipient");
            }
            this.mUid = uid;
        }

        DeathRecipient getDeathRecipient() {
            return this.mDeathRecipient;
        }

        SurfaceControl addTaskWithoutCallback(Task t, String reason) {
            t.mTaskAppearedSent = true;
            if (!this.mOrganizedTasks.contains(t)) {
                this.mOrganizedTasks.add(t);
            }
            return this.mOrganizer.prepareLeash(t, reason);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean addTask(Task t) {
            if (t.mTaskAppearedSent) {
                return false;
            }
            if (!this.mOrganizedTasks.contains(t)) {
                this.mOrganizedTasks.add(t);
            }
            if (t.taskAppearedReady()) {
                t.mTaskAppearedSent = true;
                return true;
            }
            return false;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean removeTask(Task t, boolean removeFromSystem) {
            this.mOrganizedTasks.remove(t);
            TaskOrganizerController.this.mInterceptBackPressedOnRootTasks.remove(Integer.valueOf(t.mTaskId));
            boolean taskAppearedSent = t.mTaskAppearedSent;
            if (taskAppearedSent) {
                if (t.getSurfaceControl() != null) {
                    t.migrateToNewSurfaceControl(t.getSyncTransaction());
                }
                t.mTaskAppearedSent = false;
            }
            if (removeFromSystem) {
                TaskOrganizerController.this.mService.removeTask(t.mTaskId);
            }
            return taskAppearedSent;
        }

        void dispose() {
            TaskOrganizerController.this.mTaskOrganizers.remove(this.mOrganizer.mTaskOrganizer);
            while (!this.mOrganizedTasks.isEmpty()) {
                Task t = this.mOrganizedTasks.get(0);
                if (t.mCreatedByOrganizer) {
                    t.removeImmediately();
                } else {
                    t.updateTaskOrganizerState();
                }
                if (this.mOrganizedTasks.contains(t) && removeTask(t, t.mRemoveWithTaskOrganizer)) {
                    TaskOrganizerController.this.onTaskVanishedInternal(this.mOrganizer.mTaskOrganizer, t);
                }
                if (TaskOrganizerController.this.mService.getTransitionController().isShellTransitionsEnabled() && t.mTaskOrganizer != null && t.getSurfaceControl() != null) {
                    t.getSyncTransaction().show(t.getSurfaceControl());
                }
            }
            TaskOrganizerController.this.mTaskOrganizerStates.remove(this.mOrganizer.getBinder());
            if (this == TaskOrganizerController.this.mTranAutoTaskOrganizerState) {
                Slog.d(TaskOrganizerController.TAG, "remove tran auto task organizer");
                TaskOrganizerController.this.mTranAutoTaskOrganizerState = null;
            }
        }

        void unlinkDeath() {
            this.mOrganizer.getBinder().unlinkToDeath(this.mDeathRecipient, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class PendingTaskEvent {
        static final int EVENT_APPEARED = 0;
        static final int EVENT_INFO_CHANGED = 2;
        static final int EVENT_ROOT_BACK_PRESSED = 3;
        static final int EVENT_VANISHED = 1;
        final int mEventType;
        boolean mForce;
        final Task mTask;
        final ITaskOrganizer mTaskOrg;

        PendingTaskEvent(Task task, int event) {
            this(task, task.mTaskOrganizer, event);
        }

        PendingTaskEvent(Task task, ITaskOrganizer taskOrg, int eventType) {
            this.mTask = task;
            this.mTaskOrg = taskOrg;
            this.mEventType = eventType;
        }

        boolean isLifecycleEvent() {
            int i = this.mEventType;
            return i == 0 || i == 1 || i == 2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskOrganizerController(ActivityTaskManagerService atm) {
        this.mService = atm;
        this.mGlobalLock = atm.mGlobalLock;
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            throw ActivityTaskManagerService.logAndRethrowRuntimeExceptionOnTransact(TAG, e);
        }
    }

    public void setDeferTaskOrgCallbacksConsumer(Consumer<Runnable> consumer) {
        this.mDeferTaskOrgCallbacksConsumer = consumer;
    }

    ArrayList<PendingTaskEvent> getPendingEventList() {
        return this.mPendingTaskEvents;
    }

    public ParceledListSlice<TaskAppearedInfo> registerTaskOrganizer(final ITaskOrganizer organizer) {
        ActivityTaskManagerService.enforceTaskPermission("registerTaskOrganizer()");
        final int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        long origId = Binder.clearCallingIdentity();
        try {
            final boolean isTranAuto = isRegisteredForTranAuto(pid, uid);
            final ArrayList<TaskAppearedInfo> taskInfos = new ArrayList<>();
            Runnable withGlobalLock = new Runnable() { // from class: com.android.server.wm.TaskOrganizerController$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    TaskOrganizerController.this.m8371xd13f2042(organizer, uid, isTranAuto, taskInfos);
                }
            };
            if (this.mService.getTransitionController().isShellTransitionsEnabled()) {
                this.mService.getTransitionController().mRunningLock.runWhenIdle(1000L, withGlobalLock);
            } else {
                synchronized (this.mGlobalLock) {
                    WindowManagerService.boostPriorityForLockedSection();
                    withGlobalLock.run();
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
            return new ParceledListSlice<>(taskInfos);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$registerTaskOrganizer$1$com-android-server-wm-TaskOrganizerController  reason: not valid java name */
    public /* synthetic */ void m8371xd13f2042(ITaskOrganizer organizer, int uid, boolean isTranAuto, final ArrayList taskInfos) {
        if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
            String protoLogParam0 = String.valueOf(organizer.asBinder());
            long protoLogParam1 = uid;
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, -1792633344, 4, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1)});
        }
        if (!this.mTaskOrganizerStates.containsKey(organizer.asBinder())) {
            this.mTaskOrganizers.add(organizer);
            this.mTaskOrganizerStates.put(organizer.asBinder(), new TaskOrganizerState(organizer, uid));
            if (isTranAuto) {
                Slog.d(TAG, "register tran auto task oranizer");
                this.mTranAutoTaskOrganizerState = this.mTaskOrganizerStates.get(organizer.asBinder());
                this.mTaskOrganizers.remove(organizer);
                return;
            }
        }
        final TaskOrganizerState state = this.mTaskOrganizerStates.get(organizer.asBinder());
        this.mService.mRootWindowContainer.forAllTasks(new Consumer() { // from class: com.android.server.wm.TaskOrganizerController$$ExternalSyntheticLambda6
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                TaskOrganizerController.lambda$registerTaskOrganizer$0(TaskOrganizerController.TaskOrganizerState.this, taskInfos, (Task) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$registerTaskOrganizer$0(TaskOrganizerState state, ArrayList taskInfos, Task task) {
        boolean returnTask = !task.mCreatedByOrganizer;
        task.updateTaskOrganizerState(returnTask);
        if (returnTask) {
            SurfaceControl outSurfaceControl = state.addTaskWithoutCallback(task, "TaskOrganizerController.registerTaskOrganizer");
            taskInfos.add(new TaskAppearedInfo(task.getTaskInfo(), outSurfaceControl));
        }
    }

    public void unregisterTaskOrganizer(final ITaskOrganizer organizer) {
        ActivityTaskManagerService.enforceTaskPermission("unregisterTaskOrganizer()");
        final int uid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            Runnable withGlobalLock = new Runnable() { // from class: com.android.server.wm.TaskOrganizerController$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    TaskOrganizerController.this.m8372x7fdf0aca(organizer, uid);
                }
            };
            if (this.mService.getTransitionController().isShellTransitionsEnabled()) {
                this.mService.getTransitionController().mRunningLock.runWhenIdle(1000L, withGlobalLock);
            } else {
                synchronized (this.mGlobalLock) {
                    WindowManagerService.boostPriorityForLockedSection();
                    withGlobalLock.run();
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$unregisterTaskOrganizer$2$com-android-server-wm-TaskOrganizerController  reason: not valid java name */
    public /* synthetic */ void m8372x7fdf0aca(ITaskOrganizer organizer, int uid) {
        TaskOrganizerState state = this.mTaskOrganizerStates.get(organizer.asBinder());
        if (state == null) {
            return;
        }
        if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
            String protoLogParam0 = String.valueOf(organizer.asBinder());
            long protoLogParam1 = uid;
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, -951939129, 4, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1)});
        }
        state.unlinkDeath();
        state.dispose();
    }

    ITaskOrganizer getTaskOrganizer() {
        return this.mTaskOrganizers.peekLast();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class StartingWindowAnimationAdaptor implements AnimationAdapter {
        SurfaceControl mAnimationLeash;

        StartingWindowAnimationAdaptor() {
        }

        @Override // com.android.server.wm.AnimationAdapter
        public boolean getShowWallpaper() {
            return false;
        }

        @Override // com.android.server.wm.AnimationAdapter
        public void startAnimation(SurfaceControl animationLeash, SurfaceControl.Transaction t, int type, SurfaceAnimator.OnAnimationFinishedCallback finishCallback) {
            this.mAnimationLeash = animationLeash;
        }

        @Override // com.android.server.wm.AnimationAdapter
        public void onAnimationCancelled(SurfaceControl animationLeash) {
            if (this.mAnimationLeash == animationLeash) {
                this.mAnimationLeash = null;
            }
        }

        @Override // com.android.server.wm.AnimationAdapter
        public long getDurationHint() {
            return 0L;
        }

        @Override // com.android.server.wm.AnimationAdapter
        public long getStatusBarTransitionsStartTime() {
            return 0L;
        }

        @Override // com.android.server.wm.AnimationAdapter
        public void dump(PrintWriter pw, String prefix) {
            pw.print(prefix + "StartingWindowAnimationAdaptor mCapturedLeash=");
            pw.print(this.mAnimationLeash);
            pw.println();
        }

        @Override // com.android.server.wm.AnimationAdapter
        public void dumpDebug(ProtoOutputStream proto) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static SurfaceControl applyStartingWindowAnimation(WindowState window) {
        SurfaceControl.Transaction t = window.getPendingTransaction();
        Rect mainFrame = window.getRelativeFrame();
        StartingWindowAnimationAdaptor adaptor = new StartingWindowAnimationAdaptor();
        window.startAnimation(t, adaptor, false, 128);
        if (adaptor.mAnimationLeash == null) {
            return null;
        }
        t.setPosition(adaptor.mAnimationLeash, mainFrame.left, mainFrame.top);
        return adaptor.mAnimationLeash;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean addStartingWindow(Task task, ActivityRecord activity, int launchTheme, TaskSnapshot taskSnapshot) {
        ITaskOrganizer lastOrganizer;
        Task rootTask = task.getRootTask();
        if (rootTask == null || activity.mStartingData == null || (lastOrganizer = this.mTaskOrganizers.peekLast()) == null) {
            return false;
        }
        StartingWindowInfo info = task.getStartingWindowInfo(activity);
        if (launchTheme != 0) {
            info.splashScreenThemeResId = launchTheme;
        }
        info.taskSnapshot = taskSnapshot;
        try {
            lastOrganizer.addStartingWindow(info, activity.token);
            return true;
        } catch (RemoteException e) {
            Slog.e(TAG, "Exception sending onTaskStart callback", e);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeStartingWindow(Task task, boolean prepareAnimation) {
        ITaskOrganizer lastOrganizer;
        WindowState mainWindow;
        Task rootTask = task.getRootTask();
        if (rootTask == null || (lastOrganizer = this.mTaskOrganizers.peekLast()) == null) {
            return;
        }
        StartingWindowRemovalInfo removalInfo = new StartingWindowRemovalInfo();
        removalInfo.taskId = task.mTaskId;
        removalInfo.playRevealAnimation = prepareAnimation && task.getDisplayInfo().state == 2;
        boolean playShiftUpAnimation = true ^ task.inMultiWindowMode();
        ActivityRecord topActivity = task.topActivityContainsStartingWindow();
        if (topActivity != null) {
            removalInfo.deferRemoveForIme = topActivity.mDisplayContent.mayImeShowOnLaunchingActivity(topActivity);
            if (removalInfo.playRevealAnimation && playShiftUpAnimation && (mainWindow = topActivity.findMainWindow(false)) != null) {
                removalInfo.windowAnimationLeash = applyStartingWindowAnimation(mainWindow);
                removalInfo.mainFrame = mainWindow.getRelativeFrame();
            }
        }
        try {
            lastOrganizer.removeStartingWindow(removalInfo);
        } catch (RemoteException e) {
            Slog.e(TAG, "Exception sending onStartTaskFinished callback", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean copySplashScreenView(Task task) {
        ITaskOrganizer lastOrganizer;
        Task rootTask = task.getRootTask();
        if (rootTask == null || (lastOrganizer = this.mTaskOrganizers.peekLast()) == null) {
            return false;
        }
        try {
            lastOrganizer.copySplashScreenView(task.mTaskId);
            return true;
        } catch (RemoteException e) {
            Slog.e(TAG, "Exception sending copyStartingWindowView callback", e);
            return false;
        }
    }

    public void onAppSplashScreenViewRemoved(Task task) {
        ITaskOrganizer lastOrganizer;
        Task rootTask = task.getRootTask();
        if (rootTask == null || (lastOrganizer = this.mTaskOrganizers.peekLast()) == null) {
            return;
        }
        try {
            lastOrganizer.onAppSplashScreenViewRemoved(task.mTaskId);
        } catch (RemoteException e) {
            Slog.e(TAG, "Exception sending onAppSplashScreenViewRemoved callback", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTaskAppeared(ITaskOrganizer organizer, Task task) {
        TaskOrganizerState state = this.mTaskOrganizerStates.get(organizer.asBinder());
        if (state != null && state.addTask(task)) {
            PendingTaskEvent pending = getPendingTaskEvent(task, 0);
            if (pending == null) {
                PendingTaskEvent pending2 = new PendingTaskEvent(task, 0);
                this.mPendingTaskEvents.add(pending2);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTaskVanished(ITaskOrganizer organizer, Task task) {
        TaskOrganizerState state = this.mTaskOrganizerStates.get(organizer.asBinder());
        if (state != null && state.removeTask(task, task.mRemoveWithTaskOrganizer)) {
            onTaskVanishedInternal(organizer, task);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onTaskVanishedInternal(ITaskOrganizer organizer, Task task) {
        for (int i = this.mPendingTaskEvents.size() - 1; i >= 0; i--) {
            PendingTaskEvent entry = this.mPendingTaskEvents.get(i);
            if (task.mTaskId == entry.mTask.mTaskId && entry.mTaskOrg == organizer) {
                this.mPendingTaskEvents.remove(i);
                if (entry.mEventType == 0) {
                    return;
                }
            }
        }
        PendingTaskEvent pending = new PendingTaskEvent(task, organizer, 1);
        this.mPendingTaskEvents.add(pending);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [681=4] */
    public void createRootTask(int displayId, int windowingMode, IBinder launchCookie) {
        ActivityTaskManagerService.enforceTaskPermission("createRootTask()");
        long origId = Binder.clearCallingIdentity();
        try {
            try {
                try {
                    synchronized (this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            DisplayContent display = this.mService.mRootWindowContainer.getDisplayContent(displayId);
                            if (display != null) {
                                createRootTask(display, windowingMode, launchCookie);
                                WindowManagerService.resetPriorityAfterLockedSection();
                                Binder.restoreCallingIdentity(origId);
                                return;
                            }
                            if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                                long protoLogParam0 = displayId;
                                ProtoLogImpl.e(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, 1396893178, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            Binder.restoreCallingIdentity(origId);
                        } catch (Throwable th) {
                            th = th;
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    Binder.restoreCallingIdentity(origId);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        } catch (Throwable th4) {
            th = th4;
        }
    }

    Task createRootTask(DisplayContent display, int windowingMode, IBinder launchCookie) {
        if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
            long protoLogParam0 = display.mDisplayId;
            long protoLogParam1 = windowingMode;
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, -1939861963, 5, (String) null, new Object[]{Long.valueOf(protoLogParam0), Long.valueOf(protoLogParam1)});
        }
        Task task = new Task.Builder(this.mService).setWindowingMode(windowingMode).setIntent(new Intent()).setCreatedByOrganizer(true).setDeferTaskAppear(true).setLaunchCookie(launchCookie).setParent(display.getDefaultTaskDisplayArea()).build();
        task.setDeferTaskAppear(false);
        return task;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [725=4] */
    public boolean deleteRootTask(WindowContainerToken token) {
        ActivityTaskManagerService.enforceTaskPermission("deleteRootTask()");
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                WindowContainer wc = WindowContainer.fromBinder(token.asBinder());
                if (wc == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                Task task = wc.asTask();
                if (task == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } else if (task.mCreatedByOrganizer) {
                    if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                        long protoLogParam0 = task.getDisplayId();
                        long protoLogParam1 = task.getWindowingMode();
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, -1895337367, 5, (String) null, new Object[]{Long.valueOf(protoLogParam0), Long.valueOf(protoLogParam1)});
                    }
                    task.remove(true, "deleteRootTask");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return true;
                } else {
                    throw new IllegalArgumentException("Attempt to delete task not created by organizer task=" + task);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchPendingEvents() {
        if (this.mService.mWindowManager.mWindowPlacerLocked.isLayoutDeferred() || this.mPendingTaskEvents.isEmpty()) {
            return;
        }
        int n = this.mPendingTaskEvents.size();
        for (int i = 0; i < n; i++) {
            PendingTaskEvent event = this.mPendingTaskEvents.get(i);
            Task task = event.mTask;
            switch (event.mEventType) {
                case 0:
                    TaskOrganizerState state = this.mTaskOrganizerStates.get(event.mTaskOrg.asBinder());
                    if (task.getWindowingMode() == 1) {
                        Slog.d(TAG, "dispatch pending EVENT_APPEARED for:" + task.mTaskId + " state:" + state + " appearedReady:" + task.taskAppearedReady());
                    }
                    if (state != null && task.taskAppearedReady()) {
                        state.mOrganizer.onTaskAppeared(task);
                        break;
                    }
                    break;
                case 1:
                    try {
                        event.mTaskOrg.onTaskVanished(task.getTaskInfo());
                    } catch (RemoteException ex) {
                        Slog.e(TAG, "Exception sending onTaskVanished callback", ex);
                    }
                    this.mLastSentTaskInfos.remove(task);
                    break;
                case 2:
                    dispatchTaskInfoChanged(event.mTask, event.mForce);
                    break;
                case 3:
                    TaskOrganizerState state2 = this.mTaskOrganizerStates.get(event.mTaskOrg.asBinder());
                    if (state2 != null) {
                        state2.mOrganizer.onBackPressedOnTaskRoot(task);
                        break;
                    } else {
                        break;
                    }
            }
        }
        this.mPendingTaskEvents.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reportImeDrawnOnTask(Task task) {
        TaskOrganizerState state = this.mTaskOrganizerStates.get(task.mTaskOrganizer.asBinder());
        if (state != null) {
            try {
                state.mOrganizer.mTaskOrganizer.onImeDrawnOnTask(task.mTaskId);
            } catch (RemoteException e) {
                Slog.e(TAG, "Exception sending onImeDrawnOnTask callback", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTaskInfoChanged(Task task, boolean force) {
        if (!task.mTaskAppearedSent) {
            return;
        }
        if (force && this.mPendingTaskEvents.isEmpty()) {
            dispatchTaskInfoChanged(task, true);
            return;
        }
        PendingTaskEvent pending = getPendingLifecycleTaskEvent(task);
        if (pending != null) {
            if (pending.mEventType != 2) {
                return;
            }
            this.mPendingTaskEvents.remove(pending);
        } else {
            pending = new PendingTaskEvent(task, 2);
        }
        pending.mForce |= force;
        this.mPendingTaskEvents.add(pending);
    }

    private void dispatchTaskInfoChanged(Task task, boolean force) {
        boolean changed;
        ActivityManager.RunningTaskInfo newInfo;
        TaskOrganizerState state;
        if (Build.IS_DEBUG_ENABLE) {
            Thread.currentThread();
            boolean isHoldLock = Thread.holdsLock(this.mGlobalLock);
            if (!isHoldLock) {
                Slog.d(TAG, "current thread don't hold globallock !!!!");
                throw new IllegalStateException("current thread don't hold globallock !!!!");
            }
        }
        synchronized (this.mTmpTaskInfoLock) {
            ActivityManager.RunningTaskInfo lastInfo = this.mLastSentTaskInfos.get(task);
            if (this.mTmpTaskInfo == null) {
                this.mTmpTaskInfo = new ActivityManager.RunningTaskInfo();
            }
            this.mTmpTaskInfo.configuration.unset();
            task.fillTaskInfo(this.mTmpTaskInfo);
            if (this.mTmpTaskInfo.equalsForTaskOrganizer(lastInfo) && WindowOrganizerController.configurationsAreEqualForOrganizer(this.mTmpTaskInfo.configuration, lastInfo.configuration)) {
                changed = false;
                if (!changed || force) {
                    newInfo = this.mTmpTaskInfo;
                    this.mLastSentTaskInfos.put(task, newInfo);
                    this.mTmpTaskInfo = null;
                    if (task.isOrganized() && (state = this.mTaskOrganizerStates.get(task.mTaskOrganizer.asBinder())) != null) {
                        state.mOrganizer.onTaskInfoChanged(task, newInfo);
                    }
                }
                return;
            }
            changed = true;
            if (changed) {
            }
            newInfo = this.mTmpTaskInfo;
            this.mLastSentTaskInfos.put(task, newInfo);
            this.mTmpTaskInfo = null;
            if (task.isOrganized()) {
                state.mOrganizer.onTaskInfoChanged(task, newInfo);
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [897=5] */
    public WindowContainerToken getImeTarget(int displayId) {
        ActivityTaskManagerService.enforceTaskPermission("getImeTarget()");
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent dc = this.mService.mWindowManager.mRoot.getDisplayContent(displayId);
                if (dc == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return null;
                }
                InsetsControlTarget imeLayeringTarget = dc.getImeTarget(0);
                if (imeLayeringTarget != null && imeLayeringTarget.getWindow() != null) {
                    Task task = imeLayeringTarget.getWindow().getTask();
                    if (task == null) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return null;
                    }
                    WindowContainerToken windowContainerToken = task.mRemoteToken.toWindowContainerToken();
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return windowContainerToken;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return null;
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [939=5] */
    public List<ActivityManager.RunningTaskInfo> getChildTasks(WindowContainerToken parent, int[] activityTypes) {
        ActivityTaskManagerService.enforceTaskPermission("getChildTasks()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                if (parent != null) {
                    WindowContainer container = WindowContainer.fromBinder(parent.asBinder());
                    if (container == null) {
                        Slog.e(TAG, "Can't get children of " + parent + " because it is not valid.");
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return null;
                    }
                    Task task = container.asTask();
                    if (task == null) {
                        Slog.e(TAG, container + " is not a task...");
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return null;
                    } else if (!task.mCreatedByOrganizer) {
                        Slog.w(TAG, "Can only get children of root tasks created via createRootTask");
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return null;
                    } else {
                        ArrayList<ActivityManager.RunningTaskInfo> out = new ArrayList<>();
                        for (int i = task.getChildCount() - 1; i >= 0; i--) {
                            Task child = task.getChildAt(i).asTask();
                            if (child != null && (activityTypes == null || ArrayUtils.contains(activityTypes, child.getActivityType()))) {
                                out.add(child.getTaskInfo());
                            }
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return out;
                    }
                }
                throw new IllegalArgumentException("Can't get children of null parent");
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public List<ActivityManager.RunningTaskInfo> getRootTasks(int displayId, final int[] activityTypes) {
        final ArrayList<ActivityManager.RunningTaskInfo> out;
        ActivityTaskManagerService.enforceTaskPermission("getRootTasks()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent dc = this.mService.mRootWindowContainer.getDisplayContent(displayId);
                if (dc == null) {
                    throw new IllegalArgumentException("Display " + displayId + " doesn't exist");
                }
                out = new ArrayList<>();
                dc.forAllRootTasks(new Consumer() { // from class: com.android.server.wm.TaskOrganizerController$$ExternalSyntheticLambda9
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        TaskOrganizerController.lambda$getRootTasks$3(activityTypes, out, (Task) obj);
                    }
                });
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return out;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getRootTasks$3(int[] activityTypes, ArrayList out, Task task) {
        if (activityTypes != null && !ArrayUtils.contains(activityTypes, task.getActivityType())) {
            return;
        }
        out.add(task.getTaskInfo());
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [995=4] */
    public void setInterceptBackPressedOnTaskRoot(WindowContainerToken token, boolean interceptBackPressed) {
        ActivityTaskManagerService.enforceTaskPermission("setInterceptBackPressedOnTaskRoot()");
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, 232317536, 3, (String) null, new Object[]{Boolean.valueOf(interceptBackPressed)});
                }
                WindowContainer wc = WindowContainer.fromBinder(token.asBinder());
                if (wc == null) {
                    Slog.w(TAG, "Could not resolve window from token");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                Task task = wc.asTask();
                if (task == null) {
                    Slog.w(TAG, "Could not resolve task from token");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (interceptBackPressed) {
                    this.mInterceptBackPressedOnRootTasks.add(Integer.valueOf(task.mTaskId));
                } else {
                    this.mInterceptBackPressedOnRootTasks.remove(Integer.valueOf(task.mTaskId));
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, INVOKE, INVOKE, INVOKE] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1048=6, 1049=6, 1052=6] */
    public void restartTaskTopActivityProcessIfVisible(WindowContainerToken token) {
        ActivityTaskManagerService.enforceTaskPermission("restartTopActivityProcessIfVisible()");
        int userId = UserHandle.getUserId(Binder.getCallingUid());
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                WindowContainer wc = WindowContainer.fromBinder(token.asBinder());
                if (wc == null) {
                    Slog.w(TAG, "Could not resolve window from token");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                Task task = wc.asTask();
                if (task == null) {
                    Slog.w(TAG, "Could not resolve task from token");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    if (0 != 0) {
                        restartPackage(null, userId, -1);
                    }
                    Binder.restoreCallingIdentity(origId);
                    return;
                }
                int taskId = task.mTaskId;
                String compatibleModePackageName = task.realActivity.getPackageName();
                boolean shouldRestartPackage = TranFoldingScreenController.shouldRestartPackage(compatibleModePackageName);
                if (shouldRestartPackage) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    if (shouldRestartPackage) {
                        restartPackage(compatibleModePackageName, userId, taskId);
                    }
                    Binder.restoreCallingIdentity(origId);
                } else if (TranFoldingScreenManager.isFoldableDevice()) {
                    task.forAllActivities(new Consumer() { // from class: com.android.server.wm.TaskOrganizerController$$ExternalSyntheticLambda5
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((ActivityRecord) obj).restartProcessIfVisible();
                        }
                    });
                    WindowManagerService.resetPriorityAfterLockedSection();
                    if (shouldRestartPackage) {
                        restartPackage(compatibleModePackageName, userId, taskId);
                    }
                    Binder.restoreCallingIdentity(origId);
                } else {
                    if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                        long protoLogParam0 = task.mTaskId;
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, -1963363332, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
                    }
                    ActivityRecord activity = task.getTopNonFinishingActivity();
                    if (activity != null) {
                        activity.restartProcessIfVisible();
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    if (shouldRestartPackage) {
                        restartPackage(compatibleModePackageName, userId, taskId);
                    }
                    Binder.restoreCallingIdentity(origId);
                }
            }
        } finally {
            if (0 != 0) {
                restartPackage(null, userId, -1);
            }
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, INVOKE, INVOKE, INVOKE] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1092=4, 1093=4, 1095=4] */
    public void setCompatibleModeInTask(WindowContainerToken token, int compatibleMode) {
        ActivityTaskManagerService.enforceTaskPermission("setCompatibleModeInTask()");
        int userId = UserHandle.getUserId(Binder.getCallingUid());
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                WindowContainer wc = WindowContainer.fromBinder(token.asBinder());
                if (wc == null) {
                    Slog.w(TAG, "Could not resolve window from token");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                Task task = wc.asTask();
                if (task == null) {
                    Slog.w(TAG, "Could not resolve task from token");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    if (0 != 0) {
                        restartPackage(null, userId, -1);
                    }
                    Binder.restoreCallingIdentity(origId);
                    return;
                }
                int taskId = task.mTaskId;
                String compatibleModePackageName = task.realActivity.getPackageName();
                boolean shouldRestartPackage = TranFoldingScreenController.shouldRestartPackage(compatibleModePackageName);
                if (!shouldRestartPackage) {
                    task.updateTaskMinAspectRatio(compatibleMode);
                    task.forAllActivities(new Consumer() { // from class: com.android.server.wm.TaskOrganizerController$$ExternalSyntheticLambda4
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((ActivityRecord) obj).restartProcessIfVisible();
                        }
                    });
                }
                TranFoldingScreenManager.getInstance().setCompatibleModeNoStopPackage(this.mService.mUiContext, compatibleModePackageName, compatibleMode);
                WindowManagerService.resetPriorityAfterLockedSection();
                if (shouldRestartPackage) {
                    restartPackage(compatibleModePackageName, userId, taskId);
                }
                Binder.restoreCallingIdentity(origId);
            }
        } finally {
            if (0 != 0) {
                restartPackage(null, userId, -1);
            }
            Binder.restoreCallingIdentity(origId);
        }
    }

    private void restartPackage(String packageName, int userId, int taskId) {
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setIsCompatibleModeChanged(true);
        this.mService.mAmInternal.stopAppForUser(packageName, userId);
        this.mService.startActivityFromRecents(taskId, options.toBundle());
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1134=5] */
    public void updateCameraCompatControlState(WindowContainerToken token, int state) {
        ActivityTaskManagerService.enforceTaskPermission("updateCameraCompatControlState()");
        long origId = Binder.clearCallingIdentity();
        try {
            try {
                try {
                    synchronized (this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            WindowContainer wc = WindowContainer.fromBinder(token.asBinder());
                            if (wc == null) {
                                Slog.w(TAG, "Could not resolve window from token");
                                WindowManagerService.resetPriorityAfterLockedSection();
                                Binder.restoreCallingIdentity(origId);
                                return;
                            }
                            Task task = wc.asTask();
                            if (task == null) {
                                Slog.w(TAG, "Could not resolve task from token");
                                WindowManagerService.resetPriorityAfterLockedSection();
                                Binder.restoreCallingIdentity(origId);
                                return;
                            }
                            if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                                String protoLogParam0 = String.valueOf(TaskInfo.cameraCompatControlStateToString(state));
                                long protoLogParam1 = task.mTaskId;
                                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, -846931068, 4, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1)});
                            }
                            ActivityRecord activity = task.getTopNonFinishingActivity();
                            if (activity != null) {
                                activity.updateCameraCompatStateFromUser(state);
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            Binder.restoreCallingIdentity(origId);
                        } catch (Throwable th) {
                            th = th;
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    Binder.restoreCallingIdentity(origId);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        } catch (Throwable th4) {
            th = th4;
        }
    }

    public void setIsIgnoreOrientationRequestDisabled(boolean isDisabled) {
        ActivityTaskManagerService.enforceTaskPermission("setIsIgnoreOrientationRequestDisabled()");
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                this.mService.mWindowManager.setIsIgnoreOrientationRequestDisabled(isDisabled);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public boolean handleInterceptBackPressedOnTaskRoot(Task task) {
        if (task == null || !task.isOrganized() || !this.mInterceptBackPressedOnRootTasks.contains(Integer.valueOf(task.mTaskId))) {
            return false;
        }
        PendingTaskEvent pendingVanished = getPendingTaskEvent(task, 1);
        if (pendingVanished != null) {
            return false;
        }
        PendingTaskEvent pending = getPendingTaskEvent(task, 3);
        if (pending == null) {
            pending = new PendingTaskEvent(task, 3);
        } else {
            this.mPendingTaskEvents.remove(pending);
        }
        this.mPendingTaskEvents.add(pending);
        this.mService.mWindowManager.mWindowPlacerLocked.requestTraversal();
        return true;
    }

    private PendingTaskEvent getPendingTaskEvent(Task task, int type) {
        for (int i = this.mPendingTaskEvents.size() - 1; i >= 0; i--) {
            PendingTaskEvent entry = this.mPendingTaskEvents.get(i);
            if (task.mTaskId == entry.mTask.mTaskId && type == entry.mEventType) {
                return entry;
            }
        }
        return null;
    }

    PendingTaskEvent getPendingLifecycleTaskEvent(Task task) {
        for (int i = this.mPendingTaskEvents.size() - 1; i >= 0; i--) {
            PendingTaskEvent entry = this.mPendingTaskEvents.get(i);
            if (task.mTaskId == entry.mTask.mTaskId && entry.isLifecycleEvent()) {
                return entry;
            }
        }
        return null;
    }

    public void dump(PrintWriter pw, String prefix) {
        String innerPrefix = prefix + "  ";
        pw.print(prefix);
        pw.println("TaskOrganizerController:");
        for (TaskOrganizerState state : this.mTaskOrganizerStates.values()) {
            ArrayList<Task> tasks = state.mOrganizedTasks;
            pw.print(innerPrefix + "  ");
            pw.println(state.mOrganizer.mTaskOrganizer + " uid=" + state.mUid + ":");
            for (int k = 0; k < tasks.size(); k++) {
                Task task = tasks.get(k);
                int mode = task.getWindowingMode();
                pw.println(innerPrefix + "    (" + WindowConfiguration.windowingModeToString(mode) + ") " + task);
            }
        }
        pw.println();
    }

    TaskOrganizerState getTaskOrganizerState(IBinder taskOrganizer) {
        return this.mTaskOrganizerStates.get(taskOrganizer);
    }

    public SurfaceControl createRootTaskAnimationLeash(final int taskId) {
        ActivityTaskManagerService.enforceTaskPermission("createRootTaskAnimationLeash()");
        Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        final SurfaceControl[] animationLeash = new SurfaceControl[1];
        try {
            Runnable withGlobalLock = new Runnable() { // from class: com.android.server.wm.TaskOrganizerController$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    TaskOrganizerController.this.m8369xc4cf0617(taskId, animationLeash);
                }
            };
            if (this.mService.getTransitionController().isShellTransitionsEnabled()) {
                this.mService.getTransitionController().mRunningLock.runWhenIdle(1000L, withGlobalLock);
            } else {
                synchronized (this.mGlobalLock) {
                    WindowManagerService.boostPriorityForLockedSection();
                    withGlobalLock.run();
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
            return animationLeash[0];
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$createRootTaskAnimationLeash$7$com-android-server-wm-TaskOrganizerController  reason: not valid java name */
    public /* synthetic */ void m8369xc4cf0617(final int taskId, final SurfaceControl[] animationLeash) {
        this.mService.mRootWindowContainer.forAllTasks(new Consumer() { // from class: com.android.server.wm.TaskOrganizerController$$ExternalSyntheticLambda8
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                TaskOrganizerController.lambda$createRootTaskAnimationLeash$6(taskId, animationLeash, (Task) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$createRootTaskAnimationLeash$6(int taskId, SurfaceControl[] animationLeash, Task task) {
        if (task.isTaskId(taskId)) {
            SurfaceControl.Transaction t = new SurfaceControl.Transaction();
            SurfaceControl surface = task.getSurfaceControl();
            if (surface == null) {
                Slog.w(TAG, "Unable to start animation, surface is null or no children.");
                return;
            }
            SurfaceControl.Builder builder = task.makeAnimationLeash().setParent(task.getAnimationLeashParent()).setName(surface + " - animation-leash of taskAnimation").setHidden(false).setEffectLayer().setCallsite("TaskOrganizerController.createRootTaskAnimationLeash");
            SurfaceControl leash = builder.build();
            t.show(leash);
            t.setAlpha(leash, 1.0f);
            t.setLayer(leash, task.getLastLayer());
            t.reparent(surface, leash);
            t.apply();
            animationLeash[0] = new SurfaceControl(leash, "TaskAnimationLeash");
            task.mTaskAnimationLeash = leash;
            Slog.i(TAG, " Create RootTask Leash on task=" + task + "  mNativeObject=" + leash.mNativeObject + "  leash=" + leash);
        }
    }

    public void destroyRootTaskAnimationLeash(final int taskId) {
        ActivityTaskManagerService.enforceTaskPermission("destroyRootTaskAnimationLeash()");
        Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            Runnable withGlobalLock = new Runnable() { // from class: com.android.server.wm.TaskOrganizerController$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    TaskOrganizerController.this.m8370xfd3c4f3d(taskId);
                }
            };
            if (this.mService.getTransitionController().isShellTransitionsEnabled()) {
                this.mService.getTransitionController().mRunningLock.runWhenIdle(1000L, withGlobalLock);
            } else {
                synchronized (this.mGlobalLock) {
                    WindowManagerService.boostPriorityForLockedSection();
                    withGlobalLock.run();
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$destroyRootTaskAnimationLeash$9$com-android-server-wm-TaskOrganizerController  reason: not valid java name */
    public /* synthetic */ void m8370xfd3c4f3d(final int taskId) {
        this.mService.mRootWindowContainer.forAllTasks(new Consumer() { // from class: com.android.server.wm.TaskOrganizerController$$ExternalSyntheticLambda2
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                TaskOrganizerController.lambda$destroyRootTaskAnimationLeash$8(taskId, (Task) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$destroyRootTaskAnimationLeash$8(int taskId, Task task) {
        if (task.isTaskId(taskId)) {
            SurfaceControl.Transaction t = new SurfaceControl.Transaction();
            SurfaceControl surface = task.getSurfaceControl();
            SurfaceControl parent = task.getAnimationLeashParent();
            SurfaceControl curAnimationLeash = task.mTaskAnimationLeash;
            Slog.i(TAG, " Prepare to remove RootTask Leash on task=" + task + "  curAnimationLeash=" + curAnimationLeash);
            boolean reparent = (surface == null || curAnimationLeash == null || parent == null) ? false : true;
            if (reparent) {
                t.reparent(surface, parent);
                t.remove(curAnimationLeash);
                t.apply();
                task.mTaskAnimationLeash = null;
                Slog.i(TAG, " Remove RootTask Leash.");
            } else if (curAnimationLeash != null) {
                t.remove(curAnimationLeash);
                t.apply();
                task.mTaskAnimationLeash = null;
                Slog.i(TAG, " Only remove RootTask Leash.");
            }
        }
    }

    public ActivityManager.RunningTaskInfo getFocusedRootTask(int displayId) {
        ActivityManager.RunningTaskInfo taskInfo;
        ActivityTaskManagerService.enforceTaskPermission("getFocusedRootTask()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent dc = this.mService.mRootWindowContainer.getDisplayContent(displayId);
                if (dc == null) {
                    throw new IllegalArgumentException("Display " + displayId + " doesn't exist");
                }
                taskInfo = dc.getFocusedRootTask() == null ? null : dc.getFocusedRootTask().getTaskInfo();
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return taskInfo;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean isLeafFocusedRootTask(int displayId) {
        ActivityTaskManagerService.enforceTaskPermission("isLeafFocusedRootTask()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent dc = this.mService.mRootWindowContainer.getDisplayContent(displayId);
                if (dc == null) {
                    throw new IllegalArgumentException("Display " + displayId + " doesn't exist");
                }
                if (dc.getFocusedRootTask() != null) {
                    boolean isLeafTask = dc.getFocusedRootTask().isLeafTask();
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return isLeafTask;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return false;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public List<ActivityManager.RunningTaskInfo> getVisibleTasks(int displayId) {
        ArrayList<ActivityManager.RunningTaskInfo> visibleRunningTask;
        ActivityTaskManagerService.enforceTaskPermission("getVisibleTasks()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent dc = this.mService.mRootWindowContainer.getDisplayContent(displayId);
                if (dc == null) {
                    throw new IllegalArgumentException("Display " + displayId + " doesn't exist");
                }
                ArrayList<Task> listTask = dc.getDefaultTaskDisplayArea().getVisibleTasks();
                visibleRunningTask = new ArrayList<>();
                Iterator<Task> it = listTask.iterator();
                while (it.hasNext()) {
                    Task task = it.next();
                    visibleRunningTask.add(task.getTaskInfo());
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return visibleRunningTask;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1414=4] */
    public void onTaskMoveToFront(WindowContainerToken token) {
        ActivityTaskManagerService.enforceTaskPermission("onTaskMoveToFront()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                if (token == null) {
                    throw new IllegalArgumentException("Can't get children of null parent");
                }
                WindowContainer container = WindowContainer.fromBinder(token.asBinder());
                if (container == null) {
                    Slog.e(TAG, "Can't get children of " + token + " because it is not valid.");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                Task task = container.asTask();
                if (task == null) {
                    Slog.e(TAG, container + " is not a task...");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                task.moveToFront("onTaskMoveToFront");
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private boolean isRegisteredForTranAuto(int callingPid, int callingUid) {
        return TRAN_AUTO_PACKAGE_NAME.equals(getProcessName(callingPid, callingUid));
    }

    private String getProcessName(int pid, int uid) {
        try {
            List<ActivityManager.RunningAppProcessInfo> procs = ActivityManager.getService().getRunningAppProcesses();
            int N = procs.size();
            for (int i = 0; i < N; i++) {
                ActivityManager.RunningAppProcessInfo proc = procs.get(i);
                if (proc.pid == pid && proc.uid == uid) {
                    return proc.processName;
                }
            }
            return null;
        } catch (RemoteException e) {
            Slog.v(TAG, "am.getRunningAppProcesses() failed");
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ITaskOrganizer getTaskOrganizer(Task task) {
        if (this.mTranAutoTaskOrganizerState != null && TRAN_AUTO_PACKAGE_NAME.equals(task.mCallingPackage)) {
            Slog.d(TAG, "decided to use tran auto task organizer for " + task);
            return this.mTranAutoTaskOrganizerState.mOrganizer.mTaskOrganizer;
        }
        return getTaskOrganizer();
    }
}
