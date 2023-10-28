package com.android.server.biometrics.sensors.fingerprint.aidl;

import android.content.Context;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.HalClientMonitor;
import java.util.Map;
import java.util.function.Supplier;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class FingerprintGetAuthenticatorIdClient extends HalClientMonitor<AidlSession> {
    private static final String TAG = "FingerprintGetAuthenticatorIdClient";
    private final Map<Integer, Long> mAuthenticatorIds;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FingerprintGetAuthenticatorIdClient(Context context, Supplier<AidlSession> lazyDaemon, int userId, String owner, int sensorId, BiometricLogger biometricLogger, BiometricContext biometricContext, Map<Integer, Long> authenticatorIds) {
        super(context, lazyDaemon, null, null, userId, owner, 0, sensorId, biometricLogger, biometricContext);
        this.mAuthenticatorIds = authenticatorIds;
    }

    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void unableToStart() {
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
            getFreshDaemon().getSession().getAuthenticatorId();
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAuthenticatorIdRetrieved(long authenticatorId) {
        this.mAuthenticatorIds.put(Integer.valueOf(getTargetUserId()), Long.valueOf(authenticatorId));
        this.mCallback.onClientFinished(this, true);
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public int getProtoEnum() {
        return 5;
    }
}
