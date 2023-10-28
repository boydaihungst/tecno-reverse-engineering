package com.android.server.biometrics.sensors.fingerprint.hidl;

import android.content.Context;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.RevokeChallengeClient;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class FingerprintRevokeChallengeClient extends RevokeChallengeClient<IBiometricsFingerprint> {
    private static final String TAG = "FingerprintRevokeChallengeClient";

    /* JADX INFO: Access modifiers changed from: package-private */
    public FingerprintRevokeChallengeClient(Context context, Supplier<IBiometricsFingerprint> lazyDaemon, IBinder token, int userId, String owner, int sensorId, BiometricLogger logger, BiometricContext biometricContext) {
        super(context, lazyDaemon, token, userId, owner, sensorId, logger, biometricContext);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        try {
            getFreshDaemon().postEnroll();
            this.mCallback.onClientFinished(this, true);
        } catch (RemoteException e) {
            Slog.e(TAG, "revokeChallenge/postEnroll failed", e);
            this.mCallback.onClientFinished(this, false);
        }
    }
}
