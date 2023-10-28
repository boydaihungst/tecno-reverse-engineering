package com.android.server.biometrics.sensors.fingerprint.aidl;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.StopUserClient;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class FingerprintStopUserClient extends StopUserClient<AidlSession> {
    private static final String TAG = "FingerprintStopUserClient";

    public FingerprintStopUserClient(Context context, Supplier<AidlSession> lazyDaemon, IBinder token, int userId, int sensorId, BiometricLogger logger, BiometricContext biometricContext, StopUserClient.UserStoppedCallback callback) {
        super(context, lazyDaemon, token, userId, sensorId, logger, biometricContext, callback);
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public void start(ClientMonitorCallback callback) {
        super.start(callback);
        startHalOperation();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        try {
            getFreshDaemon().getSession().close();
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception", e);
            getCallback().onClientFinished(this, false);
        }
    }

    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void unableToStart() {
    }
}
