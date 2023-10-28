package com.android.server.wm;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.window.ITaskFpsCallback;
import java.util.HashMap;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class TaskFpsCallbackController {
    private final Context mContext;
    private final HashMap<ITaskFpsCallback, Long> mTaskFpsCallbacks = new HashMap<>();
    private final HashMap<ITaskFpsCallback, IBinder.DeathRecipient> mDeathRecipients = new HashMap<>();

    private static native long nativeRegister(ITaskFpsCallback iTaskFpsCallback, int i);

    private static native void nativeUnregister(long j);

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskFpsCallbackController(Context context) {
        this.mContext = context;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerListener(int taskId, final ITaskFpsCallback callback) {
        if (this.mTaskFpsCallbacks.containsKey(callback)) {
            return;
        }
        long nativeListener = nativeRegister(callback, taskId);
        this.mTaskFpsCallbacks.put(callback, Long.valueOf(nativeListener));
        IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() { // from class: com.android.server.wm.TaskFpsCallbackController$$ExternalSyntheticLambda0
            @Override // android.os.IBinder.DeathRecipient
            public final void binderDied() {
                TaskFpsCallbackController.this.m8335x6c8167ea(callback);
            }
        };
        try {
            callback.asBinder().linkToDeath(deathRecipient, 0);
            this.mDeathRecipients.put(callback, deathRecipient);
        } catch (RemoteException e) {
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: unregisterListener */
    public void m8335x6c8167ea(ITaskFpsCallback callback) {
        if (!this.mTaskFpsCallbacks.containsKey(callback)) {
            return;
        }
        callback.asBinder().unlinkToDeath(this.mDeathRecipients.get(callback), 0);
        this.mDeathRecipients.remove(callback);
        nativeUnregister(this.mTaskFpsCallbacks.get(callback).longValue());
        this.mTaskFpsCallbacks.remove(callback);
    }
}
