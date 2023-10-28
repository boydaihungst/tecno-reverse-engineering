package com.android.server.health;

import android.hardware.health.Translate;
import android.hardware.health.V2_0.HealthInfo;
import android.hardware.health.V2_0.IHealth;
import android.hardware.health.V2_0.Result;
import android.hardware.health.V2_1.IHealthInfoCallback;
import android.os.RemoteException;
import android.os.Trace;
import android.util.Slog;
import com.android.server.health.HealthServiceWrapperHidl;
/* loaded from: classes.dex */
class HealthHalCallbackHidl extends IHealthInfoCallback.Stub implements HealthServiceWrapperHidl.Callback {
    private static final String TAG = HealthHalCallbackHidl.class.getSimpleName();
    private HealthInfoCallback mCallback;

    private static void traceBegin(String name) {
        Trace.traceBegin(524288L, name);
    }

    private static void traceEnd() {
        Trace.traceEnd(524288L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HealthHalCallbackHidl(HealthInfoCallback callback) {
        this.mCallback = callback;
    }

    @Override // android.hardware.health.V2_0.IHealthInfoCallback
    public void healthInfoChanged(HealthInfo props) {
        android.hardware.health.V2_1.HealthInfo propsLatest = new android.hardware.health.V2_1.HealthInfo();
        propsLatest.legacy = props;
        propsLatest.batteryCapacityLevel = -1;
        propsLatest.batteryChargeTimeToFullNowSeconds = -1L;
        this.mCallback.update(Translate.h2aTranslate(propsLatest));
    }

    @Override // android.hardware.health.V2_1.IHealthInfoCallback
    public void healthInfoChanged_2_1(android.hardware.health.V2_1.HealthInfo props) {
        props.batteryChargeTimeToFullNowSeconds = -1L;
        this.mCallback.update(Translate.h2aTranslate(props));
    }

    @Override // com.android.server.health.HealthServiceWrapperHidl.Callback
    public void onRegistration(IHealth oldService, IHealth newService, String instance) {
        int r;
        if (newService == null) {
            return;
        }
        traceBegin("HealthUnregisterCallback");
        if (oldService != null) {
            try {
                try {
                    int r2 = oldService.unregisterCallback(this);
                    if (r2 != 0) {
                        Slog.w(TAG, "health: cannot unregister previous callback: " + Result.toString(r2));
                    }
                } catch (RemoteException ex) {
                    Slog.w(TAG, "health: cannot unregister previous callback (transaction error): " + ex.getMessage());
                }
            } finally {
            }
        }
        traceEnd();
        traceBegin("HealthRegisterCallback");
        try {
            try {
                r = newService.registerCallback(this);
            } catch (RemoteException ex2) {
                Slog.e(TAG, "health: cannot register callback (transaction error): " + ex2.getMessage());
            }
            if (r != 0) {
                Slog.w(TAG, "health: cannot register callback: " + Result.toString(r));
            } else {
                newService.update();
            }
        } finally {
        }
    }
}
