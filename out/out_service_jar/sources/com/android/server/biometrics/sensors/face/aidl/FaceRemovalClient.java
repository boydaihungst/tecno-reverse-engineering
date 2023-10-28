package com.android.server.biometrics.sensors.face.aidl;

import android.content.Context;
import android.hardware.face.Face;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.BiometricUtils;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.RemovalClient;
import java.util.Map;
import java.util.function.Supplier;
/* loaded from: classes.dex */
class FaceRemovalClient extends RemovalClient<Face, AidlSession> {
    private static final String TAG = "FaceRemovalClient";
    final int[] mBiometricIds;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FaceRemovalClient(Context context, Supplier<AidlSession> lazyDaemon, IBinder token, ClientMonitorCallbackConverter listener, int[] biometricIds, int userId, String owner, BiometricUtils<Face> utils, int sensorId, BiometricLogger logger, BiometricContext biometricContext, Map<Integer, Long> authenticatorIds) {
        super(context, lazyDaemon, token, listener, userId, owner, utils, sensorId, logger, biometricContext, authenticatorIds);
        this.mBiometricIds = biometricIds;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        try {
            getFreshDaemon().getSession().removeEnrollments(this.mBiometricIds);
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception when requesting remove", e);
            this.mCallback.onClientFinished(this, false);
        }
    }
}
