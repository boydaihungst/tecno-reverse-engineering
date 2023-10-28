package com.android.server.biometrics.sensors.fingerprint.hidl;

import android.content.Context;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.hardware.fingerprint.Fingerprint;
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
class FingerprintRemovalClient extends RemovalClient<Fingerprint, IBiometricsFingerprint> {
    private static final String TAG = "FingerprintRemovalClient";
    private final int mBiometricId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FingerprintRemovalClient(Context context, Supplier<IBiometricsFingerprint> lazyDaemon, IBinder token, ClientMonitorCallbackConverter listener, int biometricId, int userId, String owner, BiometricUtils<Fingerprint> utils, int sensorId, BiometricLogger logger, BiometricContext biometricContext, Map<Integer, Long> authenticatorIds) {
        super(context, lazyDaemon, token, listener, userId, owner, utils, sensorId, logger, biometricContext, authenticatorIds);
        this.mBiometricId = biometricId;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        try {
            getFreshDaemon().remove(getTargetUserId(), this.mBiometricId);
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception when requesting remove", e);
            this.mCallback.onClientFinished(this, false);
        }
    }
}
