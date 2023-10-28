package com.android.server.biometrics.sensors.fingerprint.aidl;

import android.content.Context;
import android.hardware.biometrics.fingerprint.IFingerprint;
import android.hardware.biometrics.fingerprint.ISession;
import android.hardware.biometrics.fingerprint.ISessionCallback;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.StartUserClient;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class FingerprintStartUserClient extends StartUserClient<IFingerprint, ISession> {
    private static final String TAG = "FingerprintStartUserClient";
    private final ISessionCallback mSessionCallback;

    public FingerprintStartUserClient(Context context, Supplier<IFingerprint> lazyDaemon, IBinder token, int userId, int sensorId, BiometricLogger logger, BiometricContext biometricContext, ISessionCallback sessionCallback, StartUserClient.UserStartedCallback<ISession> callback) {
        super(context, lazyDaemon, token, userId, sensorId, logger, biometricContext, callback);
        this.mSessionCallback = sessionCallback;
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
            IFingerprint hal = getFreshDaemon();
            int version = hal.getInterfaceVersion();
            ISession newSession = hal.createSession(getSensorId(), getTargetUserId(), this.mSessionCallback);
            Binder.allowBlocking(newSession.asBinder());
            this.mUserStartedCallback.onUserStarted(getTargetUserId(), newSession, version);
            getCallback().onClientFinished(this, true);
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception", e);
            getCallback().onClientFinished(this, false);
        }
    }

    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void unableToStart() {
    }
}
