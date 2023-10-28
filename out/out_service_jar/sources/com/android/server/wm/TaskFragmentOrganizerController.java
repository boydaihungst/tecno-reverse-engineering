package com.android.server.wm;

import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import android.view.RemoteAnimationDefinition;
import android.window.ITaskFragmentOrganizer;
import android.window.ITaskFragmentOrganizerController;
import android.window.TaskFragmentInfo;
import android.window.TaskFragmentOrganizer;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.server.wm.TaskFragmentOrganizerController;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;
/* loaded from: classes2.dex */
public class TaskFragmentOrganizerController extends ITaskFragmentOrganizerController.Stub {
    private static final String TAG = "TaskFragmentOrganizerController";
    private static final long TEMPORARY_ACTIVITY_TOKEN_TIMEOUT_MS = 5000;
    private final ActivityTaskManagerService mAtmService;
    private final WindowManagerGlobalLock mGlobalLock;
    private final ArrayMap<IBinder, TaskFragmentOrganizerState> mTaskFragmentOrganizerState = new ArrayMap<>();
    private final ArrayList<PendingTaskFragmentEvent> mPendingTaskFragmentEvents = new ArrayList<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskFragmentOrganizerController(ActivityTaskManagerService atm) {
        this.mAtmService = atm;
        this.mGlobalLock = atm.mGlobalLock;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class TaskFragmentOrganizerState implements IBinder.DeathRecipient {
        private final ITaskFragmentOrganizer mOrganizer;
        private final int mOrganizerPid;
        private final int mOrganizerUid;
        private final ArrayList<TaskFragment> mOrganizedTaskFragments = new ArrayList<>();
        private final Map<TaskFragment, TaskFragmentInfo> mLastSentTaskFragmentInfos = new WeakHashMap();
        private final Map<TaskFragment, Configuration> mLastSentTaskFragmentParentConfigs = new WeakHashMap();
        private final Map<IBinder, ActivityRecord> mTemporaryActivityTokens = new WeakHashMap();
        private final SparseArray<RemoteAnimationDefinition> mRemoteAnimationDefinitions = new SparseArray<>();

        TaskFragmentOrganizerState(ITaskFragmentOrganizer organizer, int pid, int uid) {
            this.mOrganizer = organizer;
            this.mOrganizerPid = pid;
            this.mOrganizerUid = uid;
            try {
                organizer.asBinder().linkToDeath(this, 0);
            } catch (RemoteException e) {
                Slog.e(TaskFragmentOrganizerController.TAG, "TaskFragmentOrganizer failed to register death recipient");
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (TaskFragmentOrganizerController.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    TaskFragmentOrganizerController.this.removeOrganizer(this.mOrganizer);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        boolean addTaskFragment(TaskFragment taskFragment) {
            if (taskFragment.mTaskFragmentAppearedSent || this.mOrganizedTaskFragments.contains(taskFragment)) {
                return false;
            }
            this.mOrganizedTaskFragments.add(taskFragment);
            return true;
        }

        void removeTaskFragment(TaskFragment taskFragment) {
            this.mOrganizedTaskFragments.remove(taskFragment);
        }

        void dispose() {
            while (!this.mOrganizedTaskFragments.isEmpty()) {
                TaskFragment taskFragment = this.mOrganizedTaskFragments.get(0);
                taskFragment.removeImmediately();
                this.mOrganizedTaskFragments.remove(taskFragment);
            }
            this.mOrganizer.asBinder().unlinkToDeath(this, 0);
        }

        void onTaskFragmentAppeared(TaskFragment tf) {
            if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                String protoLogParam0 = String.valueOf(tf.getName());
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, 1284122013, 0, (String) null, new Object[]{protoLogParam0});
            }
            TaskFragmentInfo info = tf.getTaskFragmentInfo();
            try {
                this.mOrganizer.onTaskFragmentAppeared(info);
                this.mLastSentTaskFragmentInfos.put(tf, info);
                tf.mTaskFragmentAppearedSent = true;
            } catch (RemoteException e) {
                Slog.d(TaskFragmentOrganizerController.TAG, "Exception sending onTaskFragmentAppeared callback", e);
            }
            onTaskFragmentParentInfoChanged(tf);
        }

        void onTaskFragmentVanished(TaskFragment tf) {
            if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                String protoLogParam0 = String.valueOf(tf.getName());
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, -542756093, 0, (String) null, new Object[]{protoLogParam0});
            }
            try {
                this.mOrganizer.onTaskFragmentVanished(tf.getTaskFragmentInfo());
            } catch (RemoteException e) {
                Slog.d(TaskFragmentOrganizerController.TAG, "Exception sending onTaskFragmentVanished callback", e);
            }
            tf.mTaskFragmentAppearedSent = false;
            this.mLastSentTaskFragmentInfos.remove(tf);
            this.mLastSentTaskFragmentParentConfigs.remove(tf);
        }

        void onTaskFragmentInfoChanged(TaskFragment tf) {
            onTaskFragmentParentInfoChanged(tf);
            TaskFragmentInfo info = tf.getTaskFragmentInfo();
            TaskFragmentInfo lastInfo = this.mLastSentTaskFragmentInfos.get(tf);
            if (info.equalsForTaskFragmentOrganizer(lastInfo) && WindowOrganizerController.configurationsAreEqualForOrganizer(info.getConfiguration(), lastInfo.getConfiguration())) {
                return;
            }
            if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                String protoLogParam0 = String.valueOf(tf.getName());
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, 1022095595, 0, (String) null, new Object[]{protoLogParam0});
            }
            try {
                this.mOrganizer.onTaskFragmentInfoChanged(info);
                this.mLastSentTaskFragmentInfos.put(tf, info);
            } catch (RemoteException e) {
                Slog.d(TaskFragmentOrganizerController.TAG, "Exception sending onTaskFragmentInfoChanged callback", e);
            }
        }

        void onTaskFragmentParentInfoChanged(TaskFragment tf) {
            if (tf.getParent() == null || tf.getParent().asTask() == null) {
                this.mLastSentTaskFragmentParentConfigs.remove(tf);
                return;
            }
            Task parent = tf.getParent().asTask();
            Configuration parentConfig = parent.getConfiguration();
            Configuration lastParentConfig = this.mLastSentTaskFragmentParentConfigs.get(tf);
            if (WindowOrganizerController.configurationsAreEqualForOrganizer(parentConfig, lastParentConfig) && parentConfig.windowConfiguration.getWindowingMode() == lastParentConfig.windowConfiguration.getWindowingMode()) {
                return;
            }
            if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                String protoLogParam0 = String.valueOf(tf.getName());
                long protoLogParam1 = parent.mTaskId;
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, -706481945, 4, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1)});
            }
            try {
                this.mOrganizer.onTaskFragmentParentInfoChanged(tf.getFragmentToken(), parentConfig);
                this.mLastSentTaskFragmentParentConfigs.put(tf, new Configuration(parentConfig));
            } catch (RemoteException e) {
                Slog.d(TaskFragmentOrganizerController.TAG, "Exception sending onTaskFragmentParentInfoChanged callback", e);
            }
        }

        void onTaskFragmentError(IBinder errorCallbackToken, Throwable exception) {
            if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                String protoLogParam0 = String.valueOf(exception.toString());
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, 743418423, 0, (String) null, new Object[]{protoLogParam0});
            }
            Bundle exceptionBundle = TaskFragmentOrganizer.putExceptionInBundle(exception);
            try {
                this.mOrganizer.onTaskFragmentError(errorCallbackToken, exceptionBundle);
            } catch (RemoteException e) {
                Slog.d(TaskFragmentOrganizerController.TAG, "Exception sending onTaskFragmentError callback", e);
            }
        }

        void onActivityReparentToTask(ActivityRecord activity) {
            final IBinder activityToken;
            if (activity.finishing) {
                Slog.d(TaskFragmentOrganizerController.TAG, "Reparent activity=" + activity.token + " is finishing");
                return;
            }
            Task task = activity.getTask();
            if (task != null) {
                int i = task.effectiveUid;
                int i2 = this.mOrganizerUid;
                if (i == i2) {
                    if (task.isAllowedToEmbedActivity(activity, i2) != 0) {
                        Slog.d(TaskFragmentOrganizerController.TAG, "Reparent activity=" + activity.token + " is not allowed to be embedded.");
                        return;
                    }
                    if (activity.getPid() == this.mOrganizerPid) {
                        activityToken = activity.token;
                    } else {
                        activityToken = new Binder("TemporaryActivityToken");
                        this.mTemporaryActivityTokens.put(activityToken, activity);
                        Runnable timeout = new Runnable() { // from class: com.android.server.wm.TaskFragmentOrganizerController$TaskFragmentOrganizerState$$ExternalSyntheticLambda0
                            @Override // java.lang.Runnable
                            public final void run() {
                                TaskFragmentOrganizerController.TaskFragmentOrganizerState.this.m8358x53698bec(activityToken);
                            }
                        };
                        TaskFragmentOrganizerController.this.mAtmService.mWindowManager.mH.postDelayed(timeout, TaskFragmentOrganizerController.TEMPORARY_ACTIVITY_TOKEN_TIMEOUT_MS);
                    }
                    if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                        String protoLogParam0 = String.valueOf(activity.token);
                        long protoLogParam1 = task.mTaskId;
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, 873160948, 4, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1)});
                    }
                    try {
                        this.mOrganizer.onActivityReparentToTask(task.mTaskId, activity.intent, activityToken);
                        return;
                    } catch (RemoteException e) {
                        Slog.d(TaskFragmentOrganizerController.TAG, "Exception sending onActivityReparentToTask callback", e);
                        return;
                    }
                }
            }
            Slog.d(TaskFragmentOrganizerController.TAG, "Reparent activity=" + activity.token + " is not in a task belong to the organizer app.");
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onActivityReparentToTask$0$com-android-server-wm-TaskFragmentOrganizerController$TaskFragmentOrganizerState  reason: not valid java name */
        public /* synthetic */ void m8358x53698bec(IBinder activityToken) {
            synchronized (TaskFragmentOrganizerController.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    this.mTemporaryActivityTokens.remove(activityToken);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getReparentActivityFromTemporaryToken(ITaskFragmentOrganizer organizer, IBinder activityToken) {
        TaskFragmentOrganizerState state;
        if (organizer == null || activityToken == null || (state = this.mTaskFragmentOrganizerState.get(organizer.asBinder())) == null) {
            return null;
        }
        return (ActivityRecord) state.mTemporaryActivityTokens.remove(activityToken);
    }

    public void registerOrganizer(ITaskFragmentOrganizer organizer) {
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        synchronized (this.mGlobalLock) {
            try {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                        String protoLogParam0 = String.valueOf(organizer.asBinder());
                        long protoLogParam1 = uid;
                        long protoLogParam2 = pid;
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, 1653025361, 20, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1), Long.valueOf(protoLogParam2)});
                    }
                    if (this.mTaskFragmentOrganizerState.containsKey(organizer.asBinder())) {
                        throw new IllegalStateException("Replacing existing organizer currently unsupported");
                    }
                    this.mTaskFragmentOrganizerState.put(organizer.asBinder(), new TaskFragmentOrganizerState(organizer, pid, uid));
                    WindowManagerService.resetPriorityAfterLockedSection();
                } catch (Throwable th) {
                    th = th;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void unregisterOrganizer(ITaskFragmentOrganizer organizer) {
        validateAndGetState(organizer);
        int pid = Binder.getCallingPid();
        long uid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                    String protoLogParam0 = String.valueOf(organizer.asBinder());
                    long protoLogParam2 = pid;
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, -1311436264, 20, (String) null, new Object[]{protoLogParam0, Long.valueOf(uid), Long.valueOf(protoLogParam2)});
                }
                removeOrganizer(organizer);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void registerRemoteAnimations(ITaskFragmentOrganizer organizer, int taskId, RemoteAnimationDefinition definition) {
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                    String protoLogParam0 = String.valueOf(organizer.asBinder());
                    long protoLogParam1 = uid;
                    long protoLogParam2 = pid;
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, 1210037962, 20, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1), Long.valueOf(protoLogParam2)});
                }
                TaskFragmentOrganizerState organizerState = this.mTaskFragmentOrganizerState.get(organizer.asBinder());
                if (organizerState == null) {
                    throw new IllegalStateException("The organizer hasn't been registered.");
                }
                if (organizerState.mRemoteAnimationDefinitions.contains(taskId)) {
                    throw new IllegalStateException("The organizer has already registered remote animations=" + organizerState.mRemoteAnimationDefinitions.get(taskId) + " for TaskId=" + taskId);
                }
                definition.setCallingPidUid(pid, uid);
                organizerState.mRemoteAnimationDefinitions.put(taskId, definition);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void unregisterRemoteAnimations(ITaskFragmentOrganizer organizer, int taskId) {
        int pid = Binder.getCallingPid();
        long uid = Binder.getCallingUid();
        synchronized (this.mGlobalLock) {
            try {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (ProtoLogCache.WM_DEBUG_WINDOW_ORGANIZER_enabled) {
                        String protoLogParam0 = String.valueOf(organizer.asBinder());
                        long protoLogParam2 = pid;
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_ORGANIZER, -70719599, 20, (String) null, new Object[]{protoLogParam0, Long.valueOf(uid), Long.valueOf(protoLogParam2)});
                    }
                    TaskFragmentOrganizerState organizerState = this.mTaskFragmentOrganizerState.get(organizer.asBinder());
                    if (organizerState == null) {
                        Slog.e(TAG, "The organizer hasn't been registered.");
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    organizerState.mRemoteAnimationDefinitions.remove(taskId);
                    WindowManagerService.resetPriorityAfterLockedSection();
                } catch (Throwable th) {
                    th = th;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public RemoteAnimationDefinition getRemoteAnimationDefinition(ITaskFragmentOrganizer organizer, int taskId) {
        RemoteAnimationDefinition remoteAnimationDefinition;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                TaskFragmentOrganizerState organizerState = this.mTaskFragmentOrganizerState.get(organizer.asBinder());
                if (organizerState != null) {
                    remoteAnimationDefinition = (RemoteAnimationDefinition) organizerState.mRemoteAnimationDefinitions.get(taskId);
                } else {
                    remoteAnimationDefinition = null;
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return remoteAnimationDefinition;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getTaskFragmentOrganizerUid(ITaskFragmentOrganizer organizer) {
        TaskFragmentOrganizerState state = validateAndGetState(organizer);
        return state.mOrganizerUid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTaskFragmentAppeared(ITaskFragmentOrganizer organizer, TaskFragment taskFragment) {
        TaskFragmentOrganizerState state = validateAndGetState(organizer);
        if (!state.addTaskFragment(taskFragment)) {
            return;
        }
        ITaskFragmentOrganizerControllerLice.instance().onTaskFragmentAppeared(taskFragment);
        PendingTaskFragmentEvent pendingEvent = getPendingTaskFragmentEvent(taskFragment, 0);
        if (pendingEvent == null) {
            PendingTaskFragmentEvent pendingEvent2 = new PendingTaskFragmentEvent.Builder(0, organizer).setTaskFragment(taskFragment).build();
            this.mPendingTaskFragmentEvents.add(pendingEvent2);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTaskFragmentInfoChanged(ITaskFragmentOrganizer organizer, TaskFragment taskFragment) {
        handleTaskFragmentInfoChanged(organizer, taskFragment, 2);
    }

    void onTaskFragmentParentInfoChanged(ITaskFragmentOrganizer organizer, TaskFragment taskFragment) {
        handleTaskFragmentInfoChanged(organizer, taskFragment, 3);
    }

    private void handleTaskFragmentInfoChanged(ITaskFragmentOrganizer organizer, TaskFragment taskFragment, int eventType) {
        validateAndGetState(organizer);
        if (!taskFragment.mTaskFragmentAppearedSent) {
            return;
        }
        PendingTaskFragmentEvent pendingEvent = getLastPendingLifecycleEvent(taskFragment);
        if (pendingEvent == null) {
            pendingEvent = new PendingTaskFragmentEvent.Builder(eventType, organizer).setTaskFragment(taskFragment).build();
        } else if (pendingEvent.mEventType == 1) {
            return;
        } else {
            this.mPendingTaskFragmentEvents.remove(pendingEvent);
            pendingEvent.mDeferTime = 0L;
        }
        this.mPendingTaskFragmentEvents.add(pendingEvent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTaskFragmentVanished(ITaskFragmentOrganizer organizer, TaskFragment taskFragment) {
        TaskFragmentOrganizerState state = validateAndGetState(organizer);
        for (int i = this.mPendingTaskFragmentEvents.size() - 1; i >= 0; i--) {
            PendingTaskFragmentEvent entry = this.mPendingTaskFragmentEvents.get(i);
            if (taskFragment == entry.mTaskFragment) {
                this.mPendingTaskFragmentEvents.remove(i);
                if (entry.mEventType == 0) {
                    return;
                }
            }
        }
        if (!taskFragment.mTaskFragmentAppearedSent) {
            return;
        }
        PendingTaskFragmentEvent pendingEvent = new PendingTaskFragmentEvent.Builder(1, organizer).setTaskFragment(taskFragment).build();
        this.mPendingTaskFragmentEvents.add(pendingEvent);
        state.removeTaskFragment(taskFragment);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTaskFragmentError(ITaskFragmentOrganizer organizer, IBinder errorCallbackToken, Throwable exception) {
        validateAndGetState(organizer);
        Slog.w(TAG, "onTaskFragmentError ", exception);
        PendingTaskFragmentEvent pendingEvent = new PendingTaskFragmentEvent.Builder(4, organizer).setErrorCallbackToken(errorCallbackToken).setException(exception).build();
        this.mPendingTaskFragmentEvents.add(pendingEvent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onActivityReparentToTask(ActivityRecord activity) {
        ITaskFragmentOrganizer organizer;
        if (activity.mLastTaskFragmentOrganizerBeforePip != null) {
            organizer = activity.mLastTaskFragmentOrganizerBeforePip;
        } else {
            Task task = activity.getTask();
            final TaskFragment[] organizedTf = new TaskFragment[1];
            task.forAllLeafTaskFragments(new Predicate() { // from class: com.android.server.wm.TaskFragmentOrganizerController$$ExternalSyntheticLambda0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return TaskFragmentOrganizerController.lambda$onActivityReparentToTask$0(organizedTf, (TaskFragment) obj);
                }
            });
            if (organizedTf[0] == null) {
                return;
            }
            organizer = organizedTf[0].getTaskFragmentOrganizer();
        }
        if (!this.mTaskFragmentOrganizerState.containsKey(organizer.asBinder())) {
            Slog.w(TAG, "The last TaskFragmentOrganizer no longer exists");
            return;
        }
        PendingTaskFragmentEvent pendingEvent = new PendingTaskFragmentEvent.Builder(5, organizer).setActivity(activity).build();
        this.mPendingTaskFragmentEvents.add(pendingEvent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$onActivityReparentToTask$0(TaskFragment[] organizedTf, TaskFragment tf) {
        if (tf.isOrganizedTaskFragment()) {
            organizedTf[0] = tf;
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeOrganizer(ITaskFragmentOrganizer organizer) {
        TaskFragmentOrganizerState state = validateAndGetState(organizer);
        state.dispose();
        this.mTaskFragmentOrganizerState.remove(organizer.asBinder());
    }

    private TaskFragmentOrganizerState validateAndGetState(ITaskFragmentOrganizer organizer) {
        TaskFragmentOrganizerState state = this.mTaskFragmentOrganizerState.get(organizer.asBinder());
        if (state == null) {
            throw new IllegalArgumentException("TaskFragmentOrganizer has not been registered. Organizer=" + organizer);
        }
        return state;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class PendingTaskFragmentEvent {
        static final int EVENT_ACTIVITY_REPARENT_TO_TASK = 5;
        static final int EVENT_APPEARED = 0;
        static final int EVENT_ERROR = 4;
        static final int EVENT_INFO_CHANGED = 2;
        static final int EVENT_PARENT_INFO_CHANGED = 3;
        static final int EVENT_VANISHED = 1;
        private final ActivityRecord mActivity;
        private long mDeferTime;
        private final IBinder mErrorCallbackToken;
        private final int mEventType;
        private final Throwable mException;
        private final TaskFragment mTaskFragment;
        private final ITaskFragmentOrganizer mTaskFragmentOrg;

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes2.dex */
        public @interface EventType {
        }

        private PendingTaskFragmentEvent(int eventType, ITaskFragmentOrganizer taskFragmentOrg, TaskFragment taskFragment, IBinder errorCallbackToken, Throwable exception, ActivityRecord activity) {
            this.mEventType = eventType;
            this.mTaskFragmentOrg = taskFragmentOrg;
            this.mTaskFragment = taskFragment;
            this.mErrorCallbackToken = errorCallbackToken;
            this.mException = exception;
            this.mActivity = activity;
        }

        boolean isLifecycleEvent() {
            switch (this.mEventType) {
                case 0:
                case 1:
                case 2:
                case 3:
                    return true;
                default:
                    return false;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public static class Builder {
            private ActivityRecord mActivity;
            private IBinder mErrorCallbackToken;
            private final int mEventType;
            private Throwable mException;
            private TaskFragment mTaskFragment;
            private final ITaskFragmentOrganizer mTaskFragmentOrg;

            Builder(int eventType, ITaskFragmentOrganizer taskFragmentOrg) {
                this.mEventType = eventType;
                this.mTaskFragmentOrg = taskFragmentOrg;
            }

            Builder setTaskFragment(TaskFragment taskFragment) {
                this.mTaskFragment = taskFragment;
                return this;
            }

            Builder setErrorCallbackToken(IBinder errorCallbackToken) {
                this.mErrorCallbackToken = errorCallbackToken;
                return this;
            }

            Builder setException(Throwable exception) {
                this.mException = exception;
                return this;
            }

            Builder setActivity(ActivityRecord activity) {
                this.mActivity = activity;
                return this;
            }

            PendingTaskFragmentEvent build() {
                return new PendingTaskFragmentEvent(this.mEventType, this.mTaskFragmentOrg, this.mTaskFragment, this.mErrorCallbackToken, this.mException, this.mActivity);
            }
        }
    }

    private PendingTaskFragmentEvent getLastPendingLifecycleEvent(TaskFragment tf) {
        for (int i = this.mPendingTaskFragmentEvents.size() - 1; i >= 0; i--) {
            PendingTaskFragmentEvent entry = this.mPendingTaskFragmentEvents.get(i);
            if (tf == entry.mTaskFragment && entry.isLifecycleEvent()) {
                return entry;
            }
        }
        return null;
    }

    private PendingTaskFragmentEvent getPendingTaskFragmentEvent(TaskFragment taskFragment, int type) {
        for (int i = this.mPendingTaskFragmentEvents.size() - 1; i >= 0; i--) {
            PendingTaskFragmentEvent entry = this.mPendingTaskFragmentEvents.get(i);
            if (taskFragment == entry.mTaskFragment && type == entry.mEventType) {
                return entry;
            }
        }
        return null;
    }

    private boolean shouldSendEventWhenTaskInvisible(PendingTaskFragmentEvent event) {
        TaskFragmentOrganizerState state = this.mTaskFragmentOrganizerState.get(event.mTaskFragmentOrg.asBinder());
        TaskFragmentInfo lastInfo = (TaskFragmentInfo) state.mLastSentTaskFragmentInfos.get(event.mTaskFragment);
        TaskFragmentInfo info = event.mTaskFragment.getTaskFragmentInfo();
        return event.mEventType == 2 && lastInfo != null && lastInfo.hasRunningActivity() && info.isEmpty();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchPendingEvents() {
        if (this.mAtmService.mWindowManager.mWindowPlacerLocked.isLayoutDeferred() || this.mPendingTaskFragmentEvents.isEmpty()) {
            return;
        }
        ArrayList<Task> visibleTasks = new ArrayList<>();
        ArrayList<Task> invisibleTasks = new ArrayList<>();
        ArrayList<PendingTaskFragmentEvent> candidateEvents = new ArrayList<>();
        int n = this.mPendingTaskFragmentEvents.size();
        for (int i = 0; i < n; i++) {
            PendingTaskFragmentEvent event = this.mPendingTaskFragmentEvents.get(i);
            Task task = event.mTaskFragment != null ? event.mTaskFragment.getTask() : null;
            if (task != null && (task.lastActiveTime <= event.mDeferTime || (!isTaskVisible(task, visibleTasks, invisibleTasks) && !shouldSendEventWhenTaskInvisible(event)))) {
                event.mDeferTime = task.lastActiveTime;
            } else {
                candidateEvents.add(event);
            }
        }
        int numEvents = candidateEvents.size();
        for (int i2 = 0; i2 < numEvents; i2++) {
            dispatchEvent(candidateEvents.get(i2));
        }
        if (numEvents > 0) {
            this.mPendingTaskFragmentEvents.removeAll(candidateEvents);
        }
    }

    private static boolean isTaskVisible(Task task, ArrayList<Task> knownVisibleTasks, ArrayList<Task> knownInvisibleTasks) {
        if (knownVisibleTasks.contains(task)) {
            return true;
        }
        if (knownInvisibleTasks.contains(task)) {
            return false;
        }
        if (task.shouldBeVisible(null)) {
            knownVisibleTasks.add(task);
            return true;
        }
        knownInvisibleTasks.add(task);
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchPendingInfoChangedEvent(TaskFragment taskFragment) {
        PendingTaskFragmentEvent event = getPendingTaskFragmentEvent(taskFragment, 2);
        if (event == null) {
            return;
        }
        dispatchEvent(event);
        this.mPendingTaskFragmentEvents.remove(event);
    }

    private void dispatchEvent(PendingTaskFragmentEvent event) {
        ITaskFragmentOrganizer taskFragmentOrg = event.mTaskFragmentOrg;
        TaskFragment taskFragment = event.mTaskFragment;
        TaskFragmentOrganizerState state = this.mTaskFragmentOrganizerState.get(taskFragmentOrg.asBinder());
        if (state == null) {
            return;
        }
        switch (event.mEventType) {
            case 0:
                state.onTaskFragmentAppeared(taskFragment);
                return;
            case 1:
                state.onTaskFragmentVanished(taskFragment);
                return;
            case 2:
                state.onTaskFragmentInfoChanged(taskFragment);
                return;
            case 3:
                state.onTaskFragmentParentInfoChanged(taskFragment);
                return;
            case 4:
                state.onTaskFragmentError(event.mErrorCallbackToken, event.mException);
                return;
            case 5:
                state.onActivityReparentToTask(event.mActivity);
                return;
            default:
                return;
        }
    }

    public boolean isActivityEmbedded(IBinder activityToken) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord activity = ActivityRecord.forTokenLocked(activityToken);
                boolean z = false;
                if (activity == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                TaskFragment taskFragment = activity.getOrganizedTaskFragment();
                if (taskFragment == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                Task parentTask = taskFragment.getTask();
                if (parentTask == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                Rect taskBounds = parentTask.getBounds();
                Rect taskFragBounds = taskFragment.getBounds();
                if (!taskBounds.equals(taskFragBounds) && taskBounds.contains(taskFragBounds)) {
                    z = true;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return z;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }
}
