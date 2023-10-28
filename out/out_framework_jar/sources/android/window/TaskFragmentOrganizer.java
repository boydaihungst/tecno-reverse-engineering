package android.window;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.RemoteAnimationDefinition;
import android.window.ITaskFragmentOrganizer;
import android.window.TaskFragmentOrganizer;
import java.util.concurrent.Executor;
/* loaded from: classes4.dex */
public class TaskFragmentOrganizer extends WindowOrganizer {
    private static final String KEY_ERROR_CALLBACK_EXCEPTION = "fragment_exception";
    private final Executor mExecutor;
    private final ITaskFragmentOrganizer mInterface;
    private final TaskFragmentOrganizerToken mToken;

    public static Bundle putExceptionInBundle(Throwable exception) {
        Bundle exceptionBundle = new Bundle();
        exceptionBundle.putSerializable(KEY_ERROR_CALLBACK_EXCEPTION, exception);
        return exceptionBundle;
    }

    public TaskFragmentOrganizer(Executor executor) {
        AnonymousClass1 anonymousClass1 = new AnonymousClass1();
        this.mInterface = anonymousClass1;
        this.mToken = new TaskFragmentOrganizerToken(anonymousClass1);
        this.mExecutor = executor;
    }

    public Executor getExecutor() {
        return this.mExecutor;
    }

    public void registerOrganizer() {
        try {
            getController().registerOrganizer(this.mInterface);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unregisterOrganizer() {
        try {
            getController().unregisterOrganizer(this.mInterface);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void registerRemoteAnimations(int taskId, RemoteAnimationDefinition definition) {
        try {
            getController().registerRemoteAnimations(this.mInterface, taskId, definition);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unregisterRemoteAnimations(int taskId) {
        try {
            getController().unregisterRemoteAnimations(this.mInterface, taskId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void onTaskFragmentAppeared(TaskFragmentInfo taskFragmentInfo) {
    }

    public void onTaskFragmentInfoChanged(TaskFragmentInfo taskFragmentInfo) {
    }

    public void onTaskFragmentVanished(TaskFragmentInfo taskFragmentInfo) {
    }

    public void onTaskFragmentParentInfoChanged(IBinder fragmentToken, Configuration parentConfig) {
    }

    public void onTaskFragmentError(IBinder errorCallbackToken, Throwable exception) {
    }

    public void onActivityReparentToTask(int taskId, Intent activityIntent, IBinder activityToken) {
    }

    @Override // android.window.WindowOrganizer
    public void applyTransaction(WindowContainerTransaction t) {
        t.setTaskFragmentOrganizer(this.mInterface);
        super.applyTransaction(t);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: android.window.TaskFragmentOrganizer$1  reason: invalid class name */
    /* loaded from: classes4.dex */
    public class AnonymousClass1 extends ITaskFragmentOrganizer.Stub {
        AnonymousClass1() {
        }

        @Override // android.window.ITaskFragmentOrganizer
        public void onTaskFragmentAppeared(final TaskFragmentInfo taskFragmentInfo) {
            TaskFragmentOrganizer.this.mExecutor.execute(new Runnable() { // from class: android.window.TaskFragmentOrganizer$1$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    TaskFragmentOrganizer.AnonymousClass1.this.m6266x3bc22d9b(taskFragmentInfo);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onTaskFragmentAppeared$0$android-window-TaskFragmentOrganizer$1  reason: not valid java name */
        public /* synthetic */ void m6266x3bc22d9b(TaskFragmentInfo taskFragmentInfo) {
            TaskFragmentOrganizer.this.onTaskFragmentAppeared(taskFragmentInfo);
        }

        @Override // android.window.ITaskFragmentOrganizer
        public void onTaskFragmentInfoChanged(final TaskFragmentInfo taskFragmentInfo) {
            TaskFragmentOrganizer.this.mExecutor.execute(new Runnable() { // from class: android.window.TaskFragmentOrganizer$1$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    TaskFragmentOrganizer.AnonymousClass1.this.m6268x7fa9cd0a(taskFragmentInfo);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onTaskFragmentInfoChanged$1$android-window-TaskFragmentOrganizer$1  reason: not valid java name */
        public /* synthetic */ void m6268x7fa9cd0a(TaskFragmentInfo taskFragmentInfo) {
            TaskFragmentOrganizer.this.onTaskFragmentInfoChanged(taskFragmentInfo);
        }

        @Override // android.window.ITaskFragmentOrganizer
        public void onTaskFragmentVanished(final TaskFragmentInfo taskFragmentInfo) {
            TaskFragmentOrganizer.this.mExecutor.execute(new Runnable() { // from class: android.window.TaskFragmentOrganizer$1$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    TaskFragmentOrganizer.AnonymousClass1.this.m6270xba63aeb3(taskFragmentInfo);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onTaskFragmentVanished$2$android-window-TaskFragmentOrganizer$1  reason: not valid java name */
        public /* synthetic */ void m6270xba63aeb3(TaskFragmentInfo taskFragmentInfo) {
            TaskFragmentOrganizer.this.onTaskFragmentVanished(taskFragmentInfo);
        }

        @Override // android.window.ITaskFragmentOrganizer
        public void onTaskFragmentParentInfoChanged(final IBinder fragmentToken, final Configuration parentConfig) {
            TaskFragmentOrganizer.this.mExecutor.execute(new Runnable() { // from class: android.window.TaskFragmentOrganizer$1$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    TaskFragmentOrganizer.AnonymousClass1.this.m6269xe8aa59d2(fragmentToken, parentConfig);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onTaskFragmentParentInfoChanged$3$android-window-TaskFragmentOrganizer$1  reason: not valid java name */
        public /* synthetic */ void m6269xe8aa59d2(IBinder fragmentToken, Configuration parentConfig) {
            TaskFragmentOrganizer.this.onTaskFragmentParentInfoChanged(fragmentToken, parentConfig);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onTaskFragmentError$4$android-window-TaskFragmentOrganizer$1  reason: not valid java name */
        public /* synthetic */ void m6267x1aa92ac5(IBinder errorCallbackToken, Bundle exceptionBundle) {
            TaskFragmentOrganizer.this.onTaskFragmentError(errorCallbackToken, (Throwable) exceptionBundle.getSerializable(TaskFragmentOrganizer.KEY_ERROR_CALLBACK_EXCEPTION));
        }

        @Override // android.window.ITaskFragmentOrganizer
        public void onTaskFragmentError(final IBinder errorCallbackToken, final Bundle exceptionBundle) {
            TaskFragmentOrganizer.this.mExecutor.execute(new Runnable() { // from class: android.window.TaskFragmentOrganizer$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    TaskFragmentOrganizer.AnonymousClass1.this.m6267x1aa92ac5(errorCallbackToken, exceptionBundle);
                }
            });
        }

        @Override // android.window.ITaskFragmentOrganizer
        public void onActivityReparentToTask(final int taskId, final Intent activityIntent, final IBinder activityToken) {
            TaskFragmentOrganizer.this.mExecutor.execute(new Runnable() { // from class: android.window.TaskFragmentOrganizer$1$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    TaskFragmentOrganizer.AnonymousClass1.this.m6265xa1dd4cd3(taskId, activityIntent, activityToken);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onActivityReparentToTask$5$android-window-TaskFragmentOrganizer$1  reason: not valid java name */
        public /* synthetic */ void m6265xa1dd4cd3(int taskId, Intent activityIntent, IBinder activityToken) {
            TaskFragmentOrganizer.this.onActivityReparentToTask(taskId, activityIntent, activityToken);
        }
    }

    public TaskFragmentOrganizerToken getOrganizerToken() {
        return this.mToken;
    }

    private ITaskFragmentOrganizerController getController() {
        try {
            return getWindowOrganizerController().getTaskFragmentOrganizerController();
        } catch (RemoteException e) {
            return null;
        }
    }

    public boolean isActivityEmbedded(IBinder activityToken) {
        try {
            return getController().isActivityEmbedded(activityToken);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
