package com.android.server.health;

import android.hardware.health.HealthInfo;
import android.hardware.health.IHealth;
import android.hardware.health.IHealthInfoCallback;
import android.os.RemoteException;
import android.os.Trace;
import android.util.Slog;
/* loaded from: classes.dex */
public class HealthRegCallbackAidl {
    private static final String TAG = "HealthRegCallbackAidl";
    private final IHealthInfoCallback mHalInfoCallback = new HalInfoCallback();
    private final HealthInfoCallback mServiceInfoCallback;

    /* JADX INFO: Access modifiers changed from: package-private */
    public HealthRegCallbackAidl(HealthInfoCallback healthInfoCallback) {
        this.mServiceInfoCallback = healthInfoCallback;
    }

    public void onRegistration(IHealth oldService, IHealth newService) {
        if (this.mServiceInfoCallback == null) {
            return;
        }
        Trace.traceBegin(524288L, "HealthUnregisterCallbackAidl");
        try {
            unregisterCallback(oldService, this.mHalInfoCallback);
            Trace.traceEnd(524288L);
            Trace.traceBegin(524288L, "HealthRegisterCallbackAidl");
            try {
                registerCallback(newService, this.mHalInfoCallback);
            } finally {
            }
        } finally {
        }
    }

    private static void unregisterCallback(IHealth oldService, IHealthInfoCallback cb) {
        if (oldService == null) {
            return;
        }
        try {
            oldService.unregisterCallback(cb);
        } catch (RemoteException e) {
            Slog.w(TAG, "health: cannot unregister previous callback (transaction error): " + e.getMessage());
        }
    }

    private static void registerCallback(IHealth newService, IHealthInfoCallback cb) {
        try {
            newService.registerCallback(cb);
            try {
                newService.update();
            } catch (RemoteException e) {
                Slog.e(TAG, "health: cannot update after registering health info callback", e);
            }
        } catch (RemoteException e2) {
            Slog.e(TAG, "health: cannot register callback, framework may cease to receive updates on health / battery info!", e2);
        }
    }

    /* loaded from: classes.dex */
    private class HalInfoCallback extends IHealthInfoCallback.Stub {
        private HalInfoCallback() {
        }

        @Override // android.hardware.health.IHealthInfoCallback
        public void healthInfoChanged(HealthInfo healthInfo) throws RemoteException {
            HealthRegCallbackAidl.this.mServiceInfoCallback.update(healthInfo);
        }

        @Override // android.hardware.health.IHealthInfoCallback
        public String getInterfaceHash() {
            return "94e77215594f8ad98ab33a769263d48fdabed92e";
        }

        @Override // android.hardware.health.IHealthInfoCallback
        public int getInterfaceVersion() {
            return 1;
        }
    }
}
