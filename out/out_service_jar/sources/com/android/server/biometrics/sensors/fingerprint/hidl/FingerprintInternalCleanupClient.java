package com.android.server.biometrics.sensors.fingerprint.hidl;

import android.content.Context;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.hardware.fingerprint.Fingerprint;
import android.os.IBinder;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.BiometricUtils;
import com.android.server.biometrics.sensors.InternalCleanupClient;
import com.android.server.biometrics.sensors.InternalEnumerateClient;
import com.android.server.biometrics.sensors.RemovalClient;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
/* loaded from: classes.dex */
class FingerprintInternalCleanupClient extends InternalCleanupClient<Fingerprint, IBiometricsFingerprint> {
    /* JADX INFO: Access modifiers changed from: package-private */
    public FingerprintInternalCleanupClient(Context context, Supplier<IBiometricsFingerprint> lazyDaemon, int userId, String owner, int sensorId, BiometricLogger logger, BiometricContext biometricContext, List<Fingerprint> enrolledList, BiometricUtils<Fingerprint> utils, Map<Integer, Long> authenticatorIds) {
        super(context, lazyDaemon, userId, owner, sensorId, logger, biometricContext, enrolledList, utils, authenticatorIds);
    }

    @Override // com.android.server.biometrics.sensors.InternalCleanupClient
    protected InternalEnumerateClient<IBiometricsFingerprint> getEnumerateClient(Context context, Supplier<IBiometricsFingerprint> lazyDaemon, IBinder token, int userId, String owner, List<Fingerprint> enrolledList, BiometricUtils<Fingerprint> utils, int sensorId, BiometricLogger logger, BiometricContext biometricContext) {
        return new FingerprintInternalEnumerateClient(context, lazyDaemon, token, userId, owner, enrolledList, utils, sensorId, logger, biometricContext);
    }

    @Override // com.android.server.biometrics.sensors.InternalCleanupClient
    protected RemovalClient<Fingerprint, IBiometricsFingerprint> getRemovalClient(Context context, Supplier<IBiometricsFingerprint> lazyDaemon, IBinder token, int biometricId, int userId, String owner, BiometricUtils<Fingerprint> utils, int sensorId, BiometricLogger logger, BiometricContext biometricContext, Map<Integer, Long> authenticatorIds) {
        return new FingerprintRemovalClient(context, lazyDaemon, token, null, biometricId, userId, owner, utils, sensorId, logger, biometricContext, authenticatorIds);
    }
}
