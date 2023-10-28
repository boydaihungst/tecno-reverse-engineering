package com.android.server.biometrics.sensors;

import android.content.Context;
import android.hardware.biometrics.IBiometricServiceLockoutResetCallback;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Slog;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
/* loaded from: classes.dex */
public class LockoutResetDispatcher implements IBinder.DeathRecipient {
    private static final String TAG = "LockoutResetTracker";
    final CopyOnWriteArrayList<ClientCallback> mClientCallbacks = new CopyOnWriteArrayList<>();
    private final Context mContext;

    /* loaded from: classes.dex */
    private static class ClientCallback {
        private static final long WAKELOCK_TIMEOUT_MS = 2000;
        private final IBiometricServiceLockoutResetCallback mCallback;
        private final String mOpPackageName;
        private final PowerManager.WakeLock mWakeLock;

        ClientCallback(Context context, IBiometricServiceLockoutResetCallback callback, String opPackageName) {
            PowerManager pm = (PowerManager) context.getSystemService(PowerManager.class);
            this.mOpPackageName = opPackageName;
            this.mCallback = callback;
            this.mWakeLock = pm.newWakeLock(1, "LockoutResetMonitor:SendLockoutReset");
        }

        void sendLockoutReset(int sensorId) {
            if (this.mCallback != null) {
                try {
                    if ("1".equals(SystemProperties.get("persist.sys.adb.support", "0"))) {
                        Slog.d(LockoutResetDispatcher.TAG, "mWakeLock acquire for " + this.mCallback.asBinder());
                    }
                    this.mWakeLock.acquire(WAKELOCK_TIMEOUT_MS);
                    this.mCallback.onLockoutReset(sensorId, new IRemoteCallback.Stub() { // from class: com.android.server.biometrics.sensors.LockoutResetDispatcher.ClientCallback.1
                        public void sendResult(Bundle data) {
                            ClientCallback.this.releaseWakelock();
                        }
                    });
                } catch (RemoteException e) {
                    Slog.w(LockoutResetDispatcher.TAG, "Failed to invoke onLockoutReset: ", e);
                    releaseWakelock();
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void releaseWakelock() {
            if (this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
        }
    }

    public LockoutResetDispatcher(Context context) {
        this.mContext = context;
    }

    public void addCallback(IBiometricServiceLockoutResetCallback callback, String opPackageName) {
        if (callback == null) {
            Slog.w(TAG, "Callback from : " + opPackageName + " is null");
            return;
        }
        this.mClientCallbacks.add(new ClientCallback(this.mContext, callback, opPackageName));
        try {
            callback.asBinder().linkToDeath(this, 0);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to link to death", e);
        }
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
    }

    public void binderDied(IBinder who) {
        Slog.e(TAG, "Callback binder died: " + who);
        int size = this.mClientCallbacks.size();
        for (int i = 0; i < size; i++) {
            ClientCallback callback = this.mClientCallbacks.get(i);
            if (callback.mCallback.asBinder().equals(who)) {
                Slog.e(TAG, "Removing dead callback for: " + callback.mOpPackageName);
                this.mClientCallbacks.remove(i);
                callback.releaseWakelock();
                Slog.e(TAG, " Removed dead callback for Binder:" + callback.mCallback.asBinder());
                return;
            }
        }
    }

    public void notifyLockoutResetCallbacks(int sensorId) {
        Iterator<ClientCallback> it = this.mClientCallbacks.iterator();
        while (it.hasNext()) {
            ClientCallback callback = it.next();
            callback.sendLockoutReset(sensorId);
        }
    }
}
