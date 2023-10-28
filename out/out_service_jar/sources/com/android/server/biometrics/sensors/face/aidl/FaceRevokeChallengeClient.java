package com.android.server.biometrics.sensors.face.aidl;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.RevokeChallengeClient;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class FaceRevokeChallengeClient extends RevokeChallengeClient<AidlSession> {
    private static final String TAG = "FaceRevokeChallengeClient";
    private final long mChallenge;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FaceRevokeChallengeClient(Context context, Supplier<AidlSession> lazyDaemon, IBinder token, int userId, String owner, int sensorId, BiometricLogger logger, BiometricContext biometricContext, long challenge) {
        super(context, lazyDaemon, token, userId, owner, sensorId, logger, biometricContext);
        this.mChallenge = challenge;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        try {
            getFreshDaemon().getSession().revokeChallenge(this.mChallenge);
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to revokeChallenge", e);
            this.mCallback.onClientFinished(this, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onChallengeRevoked(int sensorId, int userId, long challenge) {
        boolean success = challenge == this.mChallenge;
        this.mCallback.onClientFinished(this, success);
    }
}
