package android.window;

import android.app.ActivityManager;
import android.app.PendingIntent$$ExternalSyntheticLambda1;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.SurfaceControl;
import android.window.ITaskOrganizer;
import android.window.TaskOrganizer;
import java.util.List;
import java.util.concurrent.Executor;
/* loaded from: classes4.dex */
public class TaskOrganizer extends WindowOrganizer {
    private final Executor mExecutor;
    private final ITaskOrganizer mInterface;
    private final ITaskOrganizerController mTaskOrganizerController;

    public TaskOrganizer() {
        this(null, null);
    }

    public TaskOrganizer(ITaskOrganizerController taskOrganizerController, Executor executor) {
        this.mInterface = new AnonymousClass1();
        this.mExecutor = executor != null ? executor : new PendingIntent$$ExternalSyntheticLambda1();
        this.mTaskOrganizerController = taskOrganizerController != null ? taskOrganizerController : getController();
    }

    public List<TaskAppearedInfo> registerOrganizer() {
        try {
            return this.mTaskOrganizerController.registerTaskOrganizer(this.mInterface).getList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unregisterOrganizer() {
        try {
            this.mTaskOrganizerController.unregisterTaskOrganizer(this.mInterface);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void addStartingWindow(StartingWindowInfo info, IBinder appToken) {
    }

    public void removeStartingWindow(StartingWindowRemovalInfo removalInfo) {
    }

    public void copySplashScreenView(int taskId) {
    }

    public void onAppSplashScreenViewRemoved(int taskId) {
    }

    public void onTaskAppeared(ActivityManager.RunningTaskInfo taskInfo, SurfaceControl leash) {
    }

    public void onTaskVanished(ActivityManager.RunningTaskInfo taskInfo) {
    }

    public void onTaskInfoChanged(ActivityManager.RunningTaskInfo taskInfo) {
    }

    public void onBackPressedOnTaskRoot(ActivityManager.RunningTaskInfo taskInfo) {
    }

    public void onImeDrawnOnTask(int taskId) {
    }

    public SurfaceControl createRootTaskAnimationLeash(int taskId) {
        try {
            return this.mTaskOrganizerController.createRootTaskAnimationLeash(taskId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void destroyRootTaskAnimationLeash(int taskId) {
        try {
            this.mTaskOrganizerController.destroyRootTaskAnimationLeash(taskId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void createRootTask(int displayId, int windowingMode, IBinder launchCookie) {
        try {
            this.mTaskOrganizerController.createRootTask(displayId, windowingMode, launchCookie);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean deleteRootTask(WindowContainerToken task) {
        try {
            return this.mTaskOrganizerController.deleteRootTask(task);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<ActivityManager.RunningTaskInfo> getChildTasks(WindowContainerToken parent, int[] activityTypes) {
        try {
            return this.mTaskOrganizerController.getChildTasks(parent, activityTypes);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<ActivityManager.RunningTaskInfo> getRootTasks(int displayId, int[] activityTypes) {
        try {
            return this.mTaskOrganizerController.getRootTasks(displayId, activityTypes);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public WindowContainerToken getImeTarget(int display) {
        try {
            return this.mTaskOrganizerController.getImeTarget(display);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setInterceptBackPressedOnTaskRoot(WindowContainerToken task, boolean interceptBackPressed) {
        try {
            this.mTaskOrganizerController.setInterceptBackPressedOnTaskRoot(task, interceptBackPressed);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void restartTaskTopActivityProcessIfVisible(WindowContainerToken task) {
        try {
            this.mTaskOrganizerController.restartTaskTopActivityProcessIfVisible(task);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setCompatibleModeInTask(WindowContainerToken task, int compatibleMode) {
        try {
            this.mTaskOrganizerController.setCompatibleModeInTask(task, compatibleMode);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void updateCameraCompatControlState(WindowContainerToken task, int state) {
        try {
            this.mTaskOrganizerController.updateCameraCompatControlState(task, state);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setIsIgnoreOrientationRequestDisabled(boolean isDisabled) {
        try {
            this.mTaskOrganizerController.setIsIgnoreOrientationRequestDisabled(isDisabled);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ActivityManager.RunningTaskInfo getFocusedRootTask(int displayId) {
        try {
            return this.mTaskOrganizerController.getFocusedRootTask(displayId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isLeafFocusedRootTask(int displayId) {
        try {
            return this.mTaskOrganizerController.isLeafFocusedRootTask(displayId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<ActivityManager.RunningTaskInfo> getVisibleTasks(int displayId) {
        try {
            return this.mTaskOrganizerController.getVisibleTasks(displayId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void onTaskMoveToFront(WindowContainerToken token) {
        try {
            this.mTaskOrganizerController.onTaskMoveToFront(token);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Executor getExecutor() {
        return this.mExecutor;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: android.window.TaskOrganizer$1  reason: invalid class name */
    /* loaded from: classes4.dex */
    public class AnonymousClass1 extends ITaskOrganizer.Stub {
        AnonymousClass1() {
        }

        @Override // android.window.ITaskOrganizer
        public void addStartingWindow(final StartingWindowInfo windowInfo, final IBinder appToken) {
            TaskOrganizer.this.mExecutor.execute(new Runnable() { // from class: android.window.TaskOrganizer$1$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    TaskOrganizer.AnonymousClass1.this.m6272lambda$addStartingWindow$0$androidwindowTaskOrganizer$1(windowInfo, appToken);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$addStartingWindow$0$android-window-TaskOrganizer$1  reason: not valid java name */
        public /* synthetic */ void m6272lambda$addStartingWindow$0$androidwindowTaskOrganizer$1(StartingWindowInfo windowInfo, IBinder appToken) {
            TaskOrganizer.this.addStartingWindow(windowInfo, appToken);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$removeStartingWindow$1$android-window-TaskOrganizer$1  reason: not valid java name */
        public /* synthetic */ void m6280lambda$removeStartingWindow$1$androidwindowTaskOrganizer$1(StartingWindowRemovalInfo removalInfo) {
            TaskOrganizer.this.removeStartingWindow(removalInfo);
        }

        @Override // android.window.ITaskOrganizer
        public void removeStartingWindow(final StartingWindowRemovalInfo removalInfo) {
            TaskOrganizer.this.mExecutor.execute(new Runnable() { // from class: android.window.TaskOrganizer$1$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    TaskOrganizer.AnonymousClass1.this.m6280lambda$removeStartingWindow$1$androidwindowTaskOrganizer$1(removalInfo);
                }
            });
        }

        @Override // android.window.ITaskOrganizer
        public void copySplashScreenView(final int taskId) {
            TaskOrganizer.this.mExecutor.execute(new Runnable() { // from class: android.window.TaskOrganizer$1$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    TaskOrganizer.AnonymousClass1.this.m6273lambda$copySplashScreenView$2$androidwindowTaskOrganizer$1(taskId);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$copySplashScreenView$2$android-window-TaskOrganizer$1  reason: not valid java name */
        public /* synthetic */ void m6273lambda$copySplashScreenView$2$androidwindowTaskOrganizer$1(int taskId) {
            TaskOrganizer.this.copySplashScreenView(taskId);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAppSplashScreenViewRemoved$3$android-window-TaskOrganizer$1  reason: not valid java name */
        public /* synthetic */ void m6274xdbf2e18a(int taskId) {
            TaskOrganizer.this.onAppSplashScreenViewRemoved(taskId);
        }

        @Override // android.window.ITaskOrganizer
        public void onAppSplashScreenViewRemoved(final int taskId) {
            TaskOrganizer.this.mExecutor.execute(new Runnable() { // from class: android.window.TaskOrganizer$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    TaskOrganizer.AnonymousClass1.this.m6274xdbf2e18a(taskId);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onTaskAppeared$4$android-window-TaskOrganizer$1  reason: not valid java name */
        public /* synthetic */ void m6277lambda$onTaskAppeared$4$androidwindowTaskOrganizer$1(ActivityManager.RunningTaskInfo taskInfo, SurfaceControl leash) {
            TaskOrganizer.this.onTaskAppeared(taskInfo, leash);
        }

        @Override // android.window.ITaskOrganizer
        public void onTaskAppeared(final ActivityManager.RunningTaskInfo taskInfo, final SurfaceControl leash) {
            TaskOrganizer.this.mExecutor.execute(new Runnable() { // from class: android.window.TaskOrganizer$1$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    TaskOrganizer.AnonymousClass1.this.m6277lambda$onTaskAppeared$4$androidwindowTaskOrganizer$1(taskInfo, leash);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onTaskVanished$5$android-window-TaskOrganizer$1  reason: not valid java name */
        public /* synthetic */ void m6279lambda$onTaskVanished$5$androidwindowTaskOrganizer$1(ActivityManager.RunningTaskInfo taskInfo) {
            TaskOrganizer.this.onTaskVanished(taskInfo);
        }

        @Override // android.window.ITaskOrganizer
        public void onTaskVanished(final ActivityManager.RunningTaskInfo taskInfo) {
            TaskOrganizer.this.mExecutor.execute(new Runnable() { // from class: android.window.TaskOrganizer$1$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    TaskOrganizer.AnonymousClass1.this.m6279lambda$onTaskVanished$5$androidwindowTaskOrganizer$1(taskInfo);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onTaskInfoChanged$6$android-window-TaskOrganizer$1  reason: not valid java name */
        public /* synthetic */ void m6278lambda$onTaskInfoChanged$6$androidwindowTaskOrganizer$1(ActivityManager.RunningTaskInfo info) {
            TaskOrganizer.this.onTaskInfoChanged(info);
        }

        @Override // android.window.ITaskOrganizer
        public void onTaskInfoChanged(final ActivityManager.RunningTaskInfo info) {
            TaskOrganizer.this.mExecutor.execute(new Runnable() { // from class: android.window.TaskOrganizer$1$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    TaskOrganizer.AnonymousClass1.this.m6278lambda$onTaskInfoChanged$6$androidwindowTaskOrganizer$1(info);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onBackPressedOnTaskRoot$7$android-window-TaskOrganizer$1  reason: not valid java name */
        public /* synthetic */ void m6275lambda$onBackPressedOnTaskRoot$7$androidwindowTaskOrganizer$1(ActivityManager.RunningTaskInfo info) {
            TaskOrganizer.this.onBackPressedOnTaskRoot(info);
        }

        @Override // android.window.ITaskOrganizer
        public void onBackPressedOnTaskRoot(final ActivityManager.RunningTaskInfo info) {
            TaskOrganizer.this.mExecutor.execute(new Runnable() { // from class: android.window.TaskOrganizer$1$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    TaskOrganizer.AnonymousClass1.this.m6275lambda$onBackPressedOnTaskRoot$7$androidwindowTaskOrganizer$1(info);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onImeDrawnOnTask$8$android-window-TaskOrganizer$1  reason: not valid java name */
        public /* synthetic */ void m6276lambda$onImeDrawnOnTask$8$androidwindowTaskOrganizer$1(int taskId) {
            TaskOrganizer.this.onImeDrawnOnTask(taskId);
        }

        @Override // android.window.ITaskOrganizer
        public void onImeDrawnOnTask(final int taskId) {
            TaskOrganizer.this.mExecutor.execute(new Runnable() { // from class: android.window.TaskOrganizer$1$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    TaskOrganizer.AnonymousClass1.this.m6276lambda$onImeDrawnOnTask$8$androidwindowTaskOrganizer$1(taskId);
                }
            });
        }
    }

    private ITaskOrganizerController getController() {
        try {
            return getWindowOrganizerController().getTaskOrganizerController();
        } catch (RemoteException e) {
            return null;
        }
    }
}
