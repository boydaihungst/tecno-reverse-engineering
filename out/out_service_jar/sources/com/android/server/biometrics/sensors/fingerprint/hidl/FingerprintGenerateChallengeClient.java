package com.android.server.biometrics.sensors.fingerprint.hidl;

import android.content.Context;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.GenerateChallengeClient;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class FingerprintGenerateChallengeClient extends GenerateChallengeClient<IBiometricsFingerprint> {
    private static final String TAG = "FingerprintGenerateChallengeClient";

    /* JADX INFO: Access modifiers changed from: package-private */
    public FingerprintGenerateChallengeClient(Context context, Supplier<IBiometricsFingerprint> lazyDaemon, IBinder token, ClientMonitorCallbackConverter listener, int userId, String owner, int sensorId, BiometricLogger logger, BiometricContext biometricContext) {
        super(context, lazyDaemon, token, listener, userId, owner, sensorId, logger, biometricContext);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        try {
            long challenge = getFreshDaemon().preEnroll();
            try {
                getListener().onChallengeGenerated(getSensorId(), getTargetUserId(), challenge);
                this.mCallback.onClientFinished(this, true);
            } catch (RemoteException e) {
                Slog.e(TAG, "Remote exception", e);
                this.mCallback.onClientFinished(this, false);
            }
        } catch (RemoteException e2) {
            Slog.e(TAG, "preEnroll failed", e2);
            this.mCallback.onClientFinished(this, false);
        }
    }
}
