package com.android.server.biometrics.sensors.face.hidl;

import android.content.Context;
import android.hardware.biometrics.face.V1_0.IBiometricsFace;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.RevokeChallengeClient;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class FaceRevokeChallengeClient extends RevokeChallengeClient<IBiometricsFace> {
    private static final String TAG = "FaceRevokeChallengeClient";

    /* JADX INFO: Access modifiers changed from: package-private */
    public FaceRevokeChallengeClient(Context context, Supplier<IBiometricsFace> lazyDaemon, IBinder token, int userId, String owner, int sensorId, BiometricLogger logger, BiometricContext biometricContext) {
        super(context, lazyDaemon, token, userId, owner, sensorId, logger, biometricContext);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        try {
            getFreshDaemon().revokeChallenge();
            this.mCallback.onClientFinished(this, true);
        } catch (RemoteException e) {
            Slog.e(TAG, "revokeChallenge failed", e);
            this.mCallback.onClientFinished(this, false);
        }
    }
}
