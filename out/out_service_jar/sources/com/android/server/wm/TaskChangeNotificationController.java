package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ITaskStackListener;
import android.app.TaskInfo;
import android.app.TaskStackListener;
import android.content.ComponentName;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.window.TaskSnapshot;
import com.android.internal.os.SomeArgs;
import java.util.ArrayList;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class TaskChangeNotificationController {
    private static final int NOTIFY_ACTIVITY_DISMISSING_DOCKED_ROOT_TASK_MSG = 7;
    private static final int NOTIFY_ACTIVITY_LAUNCH_ON_SECONDARY_DISPLAY_FAILED_MSG = 18;
    private static final int NOTIFY_ACTIVITY_LAUNCH_ON_SECONDARY_DISPLAY_REROUTED_MSG = 19;
    private static final int NOTIFY_ACTIVITY_PINNED_LISTENERS_MSG = 3;
    private static final int NOTIFY_ACTIVITY_REQUESTED_ORIENTATION_CHANGED_LISTENERS = 12;
    private static final int NOTIFY_ACTIVITY_RESTART_ATTEMPT_LISTENERS_MSG = 4;
    private static final int NOTIFY_ACTIVITY_ROTATED_MSG = 26;
    private static final int NOTIFY_ACTIVITY_UNPINNED_LISTENERS_MSG = 17;
    private static final int NOTIFY_BACK_PRESSED_ON_TASK_ROOT = 20;
    private static final int NOTIFY_FORCED_RESIZABLE_MSG = 6;
    private static final int NOTIFY_LOCK_TASK_MODE_CHANGED_MSG = 28;
    private static final int NOTIFY_TASK_ADDED_LISTENERS_MSG = 8;
    private static final int NOTIFY_TASK_DESCRIPTION_CHANGED_LISTENERS_MSG = 11;
    private static final int NOTIFY_TASK_DISPLAY_CHANGED_LISTENERS_MSG = 21;
    private static final int NOTIFY_TASK_FOCUS_CHANGED_MSG = 24;
    private static final int NOTIFY_TASK_LIST_FROZEN_UNFROZEN_MSG = 23;
    private static final int NOTIFY_TASK_LIST_UPDATED_LISTENERS_MSG = 22;
    private static final int NOTIFY_TASK_MOVED_TO_BACK_LISTENERS_MSG = 27;
    private static final int NOTIFY_TASK_MOVED_TO_FRONT_LISTENERS_MSG = 10;
    private static final int NOTIFY_TASK_PROFILE_LOCKED_LISTENERS_MSG = 14;
    private static final int NOTIFY_TASK_REMOVAL_STARTED_LISTENERS = 13;
    private static final int NOTIFY_TASK_REMOVED_LISTENERS_MSG = 9;
    private static final int NOTIFY_TASK_REQUESTED_ORIENTATION_CHANGED_MSG = 25;
    private static final int NOTIFY_TASK_SNAPSHOT_CHANGED_LISTENERS_MSG = 15;
    private static final int NOTIFY_TASK_STACK_CHANGE_LISTENERS_DELAY = 100;
    private static final int NOTIFY_TASK_STACK_CHANGE_LISTENERS_MSG = 2;
    private final Handler mHandler;
    private final ActivityTaskSupervisor mTaskSupervisor;
    private final RemoteCallbackList<ITaskStackListener> mRemoteTaskStackListeners = new RemoteCallbackList<>();
    private final ArrayList<ITaskStackListener> mLocalTaskStackListeners = new ArrayList<>();
    private final TaskStackConsumer mNotifyTaskStackChanged = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda0
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onTaskStackChanged();
        }
    };
    private final TaskStackConsumer mNotifyTaskCreated = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda11
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onTaskCreated(message.arg1, (ComponentName) message.obj);
        }
    };
    private final TaskStackConsumer mNotifyTaskRemoved = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda17
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onTaskRemoved(message.arg1);
        }
    };
    private final TaskStackConsumer mNotifyTaskMovedToFront = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda18
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onTaskMovedToFront((ActivityManager.RunningTaskInfo) message.obj);
        }
    };
    private final TaskStackConsumer mNotifyTaskDescriptionChanged = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda19
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onTaskDescriptionChanged((ActivityManager.RunningTaskInfo) message.obj);
        }
    };
    private final TaskStackConsumer mNotifyBackPressedOnTaskRoot = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda20
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onBackPressedOnTaskRoot((ActivityManager.RunningTaskInfo) message.obj);
        }
    };
    private final TaskStackConsumer mNotifyActivityRequestedOrientationChanged = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda21
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onActivityRequestedOrientationChanged(message.arg1, message.arg2);
        }
    };
    private final TaskStackConsumer mNotifyTaskRemovalStarted = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda22
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onTaskRemovalStarted((ActivityManager.RunningTaskInfo) message.obj);
        }
    };
    private final TaskStackConsumer mNotifyActivityPinned = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda23
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onActivityPinned((String) message.obj, message.sendingUid, message.arg1, message.arg2);
        }
    };
    private final TaskStackConsumer mNotifyActivityUnpinned = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda24
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onActivityUnpinned();
        }
    };
    private final TaskStackConsumer mNotifyActivityRestartAttempt = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda1
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            TaskChangeNotificationController.lambda$new$10(iTaskStackListener, message);
        }
    };
    private final TaskStackConsumer mNotifyActivityForcedResizable = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda2
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onActivityForcedResizable((String) message.obj, message.arg1, message.arg2);
        }
    };
    private final TaskStackConsumer mNotifyActivityDismissingDockedTask = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda3
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onActivityDismissingDockedTask();
        }
    };
    private final TaskStackConsumer mNotifyActivityLaunchOnSecondaryDisplayFailed = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda4
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onActivityLaunchOnSecondaryDisplayFailed((ActivityManager.RunningTaskInfo) message.obj, message.arg1);
        }
    };
    private final TaskStackConsumer mNotifyActivityLaunchOnSecondaryDisplayRerouted = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda5
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onActivityLaunchOnSecondaryDisplayRerouted((ActivityManager.RunningTaskInfo) message.obj, message.arg1);
        }
    };
    private final TaskStackConsumer mNotifyTaskProfileLocked = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda6
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onTaskProfileLocked((ActivityManager.RunningTaskInfo) message.obj);
        }
    };
    private final TaskStackConsumer mNotifyTaskSnapshotChanged = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda7
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onTaskSnapshotChanged(message.arg1, (TaskSnapshot) message.obj);
        }
    };
    private final TaskStackConsumer mNotifyTaskDisplayChanged = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda8
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onTaskDisplayChanged(message.arg1, message.arg2);
        }
    };
    private final TaskStackConsumer mNotifyTaskListUpdated = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda9
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onRecentTaskListUpdated();
        }
    };
    private final TaskStackConsumer mNotifyTaskListFrozen = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda10
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onRecentTaskListFrozenChanged(m.arg1 != 0);
        }
    };
    private final TaskStackConsumer mNotifyTaskFocusChanged = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda12
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onTaskFocusChanged(message.arg1, m.arg2 != 0);
        }
    };
    private final TaskStackConsumer mNotifyTaskRequestedOrientationChanged = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda13
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onTaskRequestedOrientationChanged(message.arg1, message.arg2);
        }
    };
    private final TaskStackConsumer mNotifyOnActivityRotation = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda14
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onActivityRotation(message.arg1);
        }
    };
    private final TaskStackConsumer mNotifyTaskMovedToBack = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda15
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onTaskMovedToBack((ActivityManager.RunningTaskInfo) message.obj);
        }
    };
    private final TaskStackConsumer mNotifyLockTaskModeChanged = new TaskStackConsumer() { // from class: com.android.server.wm.TaskChangeNotificationController$$ExternalSyntheticLambda16
        @Override // com.android.server.wm.TaskChangeNotificationController.TaskStackConsumer
        public final void accept(ITaskStackListener iTaskStackListener, Message message) {
            iTaskStackListener.onLockTaskModeChanged(message.arg1);
        }
    };

    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface TaskStackConsumer {
        void accept(ITaskStackListener iTaskStackListener, Message message) throws RemoteException;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$new$10(ITaskStackListener l, Message m) throws RemoteException {
        SomeArgs args = (SomeArgs) m.obj;
        l.onActivityRestartAttempt((ActivityManager.RunningTaskInfo) args.arg1, args.argi1 != 0, args.argi2 != 0, args.argi3 != 0);
    }

    /* loaded from: classes2.dex */
    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 2:
                    TaskChangeNotificationController taskChangeNotificationController = TaskChangeNotificationController.this;
                    taskChangeNotificationController.forAllRemoteListeners(taskChangeNotificationController.mNotifyTaskStackChanged, msg);
                    break;
                case 3:
                    TaskChangeNotificationController taskChangeNotificationController2 = TaskChangeNotificationController.this;
                    taskChangeNotificationController2.forAllRemoteListeners(taskChangeNotificationController2.mNotifyActivityPinned, msg);
                    break;
                case 4:
                    TaskChangeNotificationController taskChangeNotificationController3 = TaskChangeNotificationController.this;
                    taskChangeNotificationController3.forAllRemoteListeners(taskChangeNotificationController3.mNotifyActivityRestartAttempt, msg);
                    break;
                case 6:
                    TaskChangeNotificationController taskChangeNotificationController4 = TaskChangeNotificationController.this;
                    taskChangeNotificationController4.forAllRemoteListeners(taskChangeNotificationController4.mNotifyActivityForcedResizable, msg);
                    break;
                case 7:
                    TaskChangeNotificationController taskChangeNotificationController5 = TaskChangeNotificationController.this;
                    taskChangeNotificationController5.forAllRemoteListeners(taskChangeNotificationController5.mNotifyActivityDismissingDockedTask, msg);
                    break;
                case 8:
                    TaskChangeNotificationController taskChangeNotificationController6 = TaskChangeNotificationController.this;
                    taskChangeNotificationController6.forAllRemoteListeners(taskChangeNotificationController6.mNotifyTaskCreated, msg);
                    break;
                case 9:
                    TaskChangeNotificationController taskChangeNotificationController7 = TaskChangeNotificationController.this;
                    taskChangeNotificationController7.forAllRemoteListeners(taskChangeNotificationController7.mNotifyTaskRemoved, msg);
                    break;
                case 10:
                    TaskChangeNotificationController taskChangeNotificationController8 = TaskChangeNotificationController.this;
                    taskChangeNotificationController8.forAllRemoteListeners(taskChangeNotificationController8.mNotifyTaskMovedToFront, msg);
                    break;
                case 11:
                    TaskChangeNotificationController taskChangeNotificationController9 = TaskChangeNotificationController.this;
                    taskChangeNotificationController9.forAllRemoteListeners(taskChangeNotificationController9.mNotifyTaskDescriptionChanged, msg);
                    break;
                case 12:
                    TaskChangeNotificationController taskChangeNotificationController10 = TaskChangeNotificationController.this;
                    taskChangeNotificationController10.forAllRemoteListeners(taskChangeNotificationController10.mNotifyActivityRequestedOrientationChanged, msg);
                    break;
                case 13:
                    TaskChangeNotificationController taskChangeNotificationController11 = TaskChangeNotificationController.this;
                    taskChangeNotificationController11.forAllRemoteListeners(taskChangeNotificationController11.mNotifyTaskRemovalStarted, msg);
                    break;
                case 14:
                    TaskChangeNotificationController taskChangeNotificationController12 = TaskChangeNotificationController.this;
                    taskChangeNotificationController12.forAllRemoteListeners(taskChangeNotificationController12.mNotifyTaskProfileLocked, msg);
                    break;
                case 15:
                    TaskChangeNotificationController taskChangeNotificationController13 = TaskChangeNotificationController.this;
                    taskChangeNotificationController13.forAllRemoteListeners(taskChangeNotificationController13.mNotifyTaskSnapshotChanged, msg);
                    break;
                case 17:
                    TaskChangeNotificationController taskChangeNotificationController14 = TaskChangeNotificationController.this;
                    taskChangeNotificationController14.forAllRemoteListeners(taskChangeNotificationController14.mNotifyActivityUnpinned, msg);
                    break;
                case 18:
                    TaskChangeNotificationController taskChangeNotificationController15 = TaskChangeNotificationController.this;
                    taskChangeNotificationController15.forAllRemoteListeners(taskChangeNotificationController15.mNotifyActivityLaunchOnSecondaryDisplayFailed, msg);
                    break;
                case 19:
                    TaskChangeNotificationController taskChangeNotificationController16 = TaskChangeNotificationController.this;
                    taskChangeNotificationController16.forAllRemoteListeners(taskChangeNotificationController16.mNotifyActivityLaunchOnSecondaryDisplayRerouted, msg);
                    break;
                case 20:
                    TaskChangeNotificationController taskChangeNotificationController17 = TaskChangeNotificationController.this;
                    taskChangeNotificationController17.forAllRemoteListeners(taskChangeNotificationController17.mNotifyBackPressedOnTaskRoot, msg);
                    break;
                case 21:
                    TaskChangeNotificationController taskChangeNotificationController18 = TaskChangeNotificationController.this;
                    taskChangeNotificationController18.forAllRemoteListeners(taskChangeNotificationController18.mNotifyTaskDisplayChanged, msg);
                    break;
                case 22:
                    TaskChangeNotificationController taskChangeNotificationController19 = TaskChangeNotificationController.this;
                    taskChangeNotificationController19.forAllRemoteListeners(taskChangeNotificationController19.mNotifyTaskListUpdated, msg);
                    break;
                case 23:
                    TaskChangeNotificationController taskChangeNotificationController20 = TaskChangeNotificationController.this;
                    taskChangeNotificationController20.forAllRemoteListeners(taskChangeNotificationController20.mNotifyTaskListFrozen, msg);
                    break;
                case 24:
                    TaskChangeNotificationController taskChangeNotificationController21 = TaskChangeNotificationController.this;
                    taskChangeNotificationController21.forAllRemoteListeners(taskChangeNotificationController21.mNotifyTaskFocusChanged, msg);
                    break;
                case 25:
                    TaskChangeNotificationController taskChangeNotificationController22 = TaskChangeNotificationController.this;
                    taskChangeNotificationController22.forAllRemoteListeners(taskChangeNotificationController22.mNotifyTaskRequestedOrientationChanged, msg);
                    break;
                case 26:
                    TaskChangeNotificationController taskChangeNotificationController23 = TaskChangeNotificationController.this;
                    taskChangeNotificationController23.forAllRemoteListeners(taskChangeNotificationController23.mNotifyOnActivityRotation, msg);
                    break;
                case 27:
                    TaskChangeNotificationController taskChangeNotificationController24 = TaskChangeNotificationController.this;
                    taskChangeNotificationController24.forAllRemoteListeners(taskChangeNotificationController24.mNotifyTaskMovedToBack, msg);
                    break;
                case 28:
                    TaskChangeNotificationController taskChangeNotificationController25 = TaskChangeNotificationController.this;
                    taskChangeNotificationController25.forAllRemoteListeners(taskChangeNotificationController25.mNotifyLockTaskModeChanged, msg);
                    break;
            }
            if (msg.obj instanceof SomeArgs) {
                ((SomeArgs) msg.obj).recycle();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskChangeNotificationController(ActivityTaskSupervisor taskSupervisor, Handler handler) {
        this.mTaskSupervisor = taskSupervisor;
        this.mHandler = new MainHandler(handler.getLooper());
    }

    public void registerTaskStackListener(ITaskStackListener listener) {
        if (listener instanceof Binder) {
            synchronized (this.mLocalTaskStackListeners) {
                if (!this.mLocalTaskStackListeners.contains(listener)) {
                    if (listener instanceof TaskStackListener) {
                        ((TaskStackListener) listener).setIsLocal();
                    }
                    this.mLocalTaskStackListeners.add(listener);
                }
            }
        } else if (listener != null) {
            synchronized (this.mRemoteTaskStackListeners) {
                this.mRemoteTaskStackListeners.register(listener);
            }
        }
    }

    public void unregisterTaskStackListener(ITaskStackListener listener) {
        if (listener instanceof Binder) {
            synchronized (this.mLocalTaskStackListeners) {
                this.mLocalTaskStackListeners.remove(listener);
            }
        } else if (listener != null) {
            synchronized (this.mRemoteTaskStackListeners) {
                this.mRemoteTaskStackListeners.unregister(listener);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void forAllRemoteListeners(TaskStackConsumer callback, Message message) {
        synchronized (this.mRemoteTaskStackListeners) {
            for (int i = this.mRemoteTaskStackListeners.beginBroadcast() - 1; i >= 0; i--) {
                try {
                    callback.accept(this.mRemoteTaskStackListeners.getBroadcastItem(i), message);
                } catch (RemoteException e) {
                }
            }
            this.mRemoteTaskStackListeners.finishBroadcast();
        }
    }

    private void forAllLocalListeners(TaskStackConsumer callback, Message message) {
        synchronized (this.mLocalTaskStackListeners) {
            for (int i = this.mLocalTaskStackListeners.size() - 1; i >= 0; i--) {
                try {
                    callback.accept(this.mLocalTaskStackListeners.get(i), message);
                } catch (RemoteException e) {
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyTaskStackChanged() {
        this.mTaskSupervisor.getActivityMetricsLogger().logWindowState();
        this.mHandler.removeMessages(2);
        Message msg = this.mHandler.obtainMessage(2);
        forAllLocalListeners(this.mNotifyTaskStackChanged, msg);
        this.mHandler.sendMessageDelayed(msg, 100L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyActivityPinned(ActivityRecord r) {
        this.mHandler.removeMessages(3);
        Message msg = this.mHandler.obtainMessage(3, r.getTask().mTaskId, r.getRootTaskId(), r.packageName);
        msg.sendingUid = r.mUserId;
        forAllLocalListeners(this.mNotifyActivityPinned, msg);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyActivityUnpinned() {
        this.mHandler.removeMessages(17);
        Message msg = this.mHandler.obtainMessage(17);
        forAllLocalListeners(this.mNotifyActivityUnpinned, msg);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyActivityRestartAttempt(ActivityManager.RunningTaskInfo task, boolean homeTaskVisible, boolean clearedTask, boolean wasVisible) {
        this.mHandler.removeMessages(4);
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = task;
        args.argi1 = homeTaskVisible ? 1 : 0;
        args.argi2 = clearedTask ? 1 : 0;
        args.argi3 = wasVisible ? 1 : 0;
        Message msg = this.mHandler.obtainMessage(4, args);
        forAllLocalListeners(this.mNotifyActivityRestartAttempt, msg);
        msg.sendToTarget();
    }

    void notifyActivityDismissingDockedRootTask() {
        this.mHandler.removeMessages(7);
        Message msg = this.mHandler.obtainMessage(7);
        forAllLocalListeners(this.mNotifyActivityDismissingDockedTask, msg);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyActivityForcedResizable(int taskId, int reason, String packageName) {
        this.mHandler.removeMessages(6);
        Message msg = this.mHandler.obtainMessage(6, taskId, reason, packageName);
        forAllLocalListeners(this.mNotifyActivityForcedResizable, msg);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyActivityLaunchOnSecondaryDisplayFailed(TaskInfo ti, int requestedDisplayId) {
        this.mHandler.removeMessages(18);
        Message msg = this.mHandler.obtainMessage(18, requestedDisplayId, 0, ti);
        forAllLocalListeners(this.mNotifyActivityLaunchOnSecondaryDisplayFailed, msg);
        msg.sendToTarget();
    }

    void notifyActivityLaunchOnSecondaryDisplayRerouted(TaskInfo ti, int requestedDisplayId) {
        this.mHandler.removeMessages(19);
        Message msg = this.mHandler.obtainMessage(19, requestedDisplayId, 0, ti);
        forAllLocalListeners(this.mNotifyActivityLaunchOnSecondaryDisplayRerouted, msg);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyTaskCreated(int taskId, ComponentName componentName) {
        Message msg = this.mHandler.obtainMessage(8, taskId, 0, componentName);
        forAllLocalListeners(this.mNotifyTaskCreated, msg);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyTaskRemoved(int taskId) {
        Message msg = this.mHandler.obtainMessage(9, taskId, 0);
        forAllLocalListeners(this.mNotifyTaskRemoved, msg);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyTaskMovedToFront(TaskInfo ti) {
        Message msg = this.mHandler.obtainMessage(10, ti);
        forAllLocalListeners(this.mNotifyTaskMovedToFront, msg);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyTaskDescriptionChanged(TaskInfo taskInfo) {
        Message msg = this.mHandler.obtainMessage(11, taskInfo);
        forAllLocalListeners(this.mNotifyTaskDescriptionChanged, msg);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyActivityRequestedOrientationChanged(int taskId, int orientation) {
        Message msg = this.mHandler.obtainMessage(12, taskId, orientation);
        forAllLocalListeners(this.mNotifyActivityRequestedOrientationChanged, msg);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyTaskRemovalStarted(ActivityManager.RunningTaskInfo taskInfo) {
        Message msg = this.mHandler.obtainMessage(13, taskInfo);
        forAllLocalListeners(this.mNotifyTaskRemovalStarted, msg);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyTaskProfileLocked(ActivityManager.RunningTaskInfo taskInfo) {
        Message msg = this.mHandler.obtainMessage(14, taskInfo);
        forAllLocalListeners(this.mNotifyTaskProfileLocked, msg);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyTaskSnapshotChanged(int taskId, TaskSnapshot snapshot) {
        Message msg = this.mHandler.obtainMessage(15, taskId, 0, snapshot);
        forAllLocalListeners(this.mNotifyTaskSnapshotChanged, msg);
        msg.sendToTarget();
    }

    void notifyBackPressedOnTaskRoot(TaskInfo taskInfo) {
        Message msg = this.mHandler.obtainMessage(20, taskInfo);
        forAllLocalListeners(this.mNotifyBackPressedOnTaskRoot, msg);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyTaskDisplayChanged(int taskId, int newDisplayId) {
        Message msg = this.mHandler.obtainMessage(21, taskId, newDisplayId);
        forAllLocalListeners(this.mNotifyTaskDisplayChanged, msg);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyTaskListUpdated() {
        Message msg = this.mHandler.obtainMessage(22);
        forAllLocalListeners(this.mNotifyTaskListUpdated, msg);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyTaskListFrozen(boolean frozen) {
        Message msg = this.mHandler.obtainMessage(23, frozen ? 1 : 0, 0);
        forAllLocalListeners(this.mNotifyTaskListFrozen, msg);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyTaskFocusChanged(int taskId, boolean focused) {
        Message msg = this.mHandler.obtainMessage(24, taskId, focused ? 1 : 0);
        forAllLocalListeners(this.mNotifyTaskFocusChanged, msg);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyTaskRequestedOrientationChanged(int taskId, int requestedOrientation) {
        Message msg = this.mHandler.obtainMessage(25, taskId, requestedOrientation);
        forAllLocalListeners(this.mNotifyTaskRequestedOrientationChanged, msg);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyOnActivityRotation(int displayId) {
        Message msg = this.mHandler.obtainMessage(26, displayId, 0);
        forAllLocalListeners(this.mNotifyOnActivityRotation, msg);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyTaskMovedToBack(TaskInfo ti) {
        Message msg = this.mHandler.obtainMessage(27, ti);
        forAllLocalListeners(this.mNotifyTaskMovedToBack, msg);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyLockTaskModeChanged(int lockTaskModeState) {
        Message msg = this.mHandler.obtainMessage(28, lockTaskModeState, 0);
        forAllLocalListeners(this.mNotifyLockTaskModeChanged, msg);
        msg.sendToTarget();
    }
}
