package com.android.server.biometrics.sensors.face.aidl;

import android.content.Context;
import android.hardware.biometrics.IInvalidationCallback;
import android.hardware.face.Face;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.InvalidationClient;
import java.util.Map;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class FaceInvalidationClient extends InvalidationClient<Face, AidlSession> {
    private static final String TAG = "FaceInvalidationClient";

    public FaceInvalidationClient(Context context, Supplier<AidlSession> lazyDaemon, int userId, int sensorId, BiometricLogger logger, BiometricContext biometricContext, Map<Integer, Long> authenticatorIds, IInvalidationCallback callback) {
        super(context, lazyDaemon, userId, sensorId, logger, biometricContext, authenticatorIds, callback);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        try {
            getFreshDaemon().getSession().invalidateAuthenticatorId();
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception", e);
            this.mCallback.onClientFinished(this, false);
        }
    }
}
