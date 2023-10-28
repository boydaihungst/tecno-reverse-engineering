package com.android.server.wm;

import com.android.internal.os.BackgroundThread;
import com.android.server.wm.WindowManagerInternal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.Executor;
/* loaded from: classes2.dex */
final class TaskSystemBarsListenerController {
    private final HashSet<WindowManagerInternal.TaskSystemBarsListener> mListeners = new HashSet<>();
    private final Executor mBackgroundExecutor = BackgroundThread.getExecutor();

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerListener(WindowManagerInternal.TaskSystemBarsListener listener) {
        this.mListeners.add(listener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterListener(WindowManagerInternal.TaskSystemBarsListener listener) {
        this.mListeners.remove(listener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchTransientSystemBarVisibilityChanged(final int taskId, final boolean visible, final boolean wereRevealedFromSwipeOnSystemBar) {
        final HashSet<WindowManagerInternal.TaskSystemBarsListener> localListeners = new HashSet<>(this.mListeners);
        this.mBackgroundExecutor.execute(new Runnable() { // from class: com.android.server.wm.TaskSystemBarsListenerController$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                TaskSystemBarsListenerController.lambda$dispatchTransientSystemBarVisibilityChanged$0(localListeners, taskId, visible, wereRevealedFromSwipeOnSystemBar);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dispatchTransientSystemBarVisibilityChanged$0(HashSet localListeners, int taskId, boolean visible, boolean wereRevealedFromSwipeOnSystemBar) {
        Iterator it = localListeners.iterator();
        while (it.hasNext()) {
            WindowManagerInternal.TaskSystemBarsListener listener = (WindowManagerInternal.TaskSystemBarsListener) it.next();
            listener.onTransientSystemBarsVisibilityChanged(taskId, visible, wereRevealedFromSwipeOnSystemBar);
        }
    }
}
