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
import com.android.server.biometrics.sensors.InternalEnumerateClient;
import java.util.List;
import java.util.function.Supplier;
/* loaded from: classes.dex */
class FingerprintInternalEnumerateClient extends InternalEnumerateClient<IBiometricsFingerprint> {
    private static final String TAG = "FingerprintInternalEnumerateClient";

    /* JADX INFO: Access modifiers changed from: package-private */
    public FingerprintInternalEnumerateClient(Context context, Supplier<IBiometricsFingerprint> lazyDaemon, IBinder token, int userId, String owner, List<Fingerprint> enrolledList, BiometricUtils<Fingerprint> utils, int sensorId, BiometricLogger logger, BiometricContext biometricContext) {
        super(context, lazyDaemon, token, userId, owner, enrolledList, utils, sensorId, logger, biometricContext);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        try {
            getFreshDaemon().enumerate();
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception when requesting enumerate", e);
            this.mCallback.onClientFinished(this, false);
        }
    }
}
