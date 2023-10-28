package com.android.server.biometrics.sensors.fingerprint.aidl;

import android.content.Context;
import android.hardware.fingerprint.Fingerprint;
import android.os.IBinder;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.BiometricUtils;
import com.android.server.biometrics.sensors.InternalCleanupClient;
import com.android.server.biometrics.sensors.InternalEnumerateClient;
import com.android.server.biometrics.sensors.RemovalClient;
import com.android.server.biometrics.sensors.fingerprint.FingerprintUtils;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
/* loaded from: classes.dex */
class FingerprintInternalCleanupClient extends InternalCleanupClient<Fingerprint, AidlSession> {
    /* JADX INFO: Access modifiers changed from: package-private */
    public FingerprintInternalCleanupClient(Context context, Supplier<AidlSession> lazyDaemon, int userId, String owner, int sensorId, BiometricLogger logger, BiometricContext biometricContext, List<Fingerprint> enrolledList, FingerprintUtils utils, Map<Integer, Long> authenticatorIds) {
        super(context, lazyDaemon, userId, owner, sensorId, logger, biometricContext, enrolledList, utils, authenticatorIds);
    }

    @Override // com.android.server.biometrics.sensors.InternalCleanupClient
    protected InternalEnumerateClient<AidlSession> getEnumerateClient(Context context, Supplier<AidlSession> lazyDaemon, IBinder token, int userId, String owner, List<Fingerprint> enrolledList, BiometricUtils<Fingerprint> utils, int sensorId, BiometricLogger logger, BiometricContext biometricContext) {
        return new FingerprintInternalEnumerateClient(context, lazyDaemon, token, userId, owner, enrolledList, utils, sensorId, logger.swapAction(context, 3), biometricContext);
    }

    @Override // com.android.server.biometrics.sensors.InternalCleanupClient
    protected RemovalClient<Fingerprint, AidlSession> getRemovalClient(Context context, Supplier<AidlSession> lazyDaemon, IBinder token, int biometricId, int userId, String owner, BiometricUtils<Fingerprint> utils, int sensorId, BiometricLogger logger, BiometricContext biometricContext, Map<Integer, Long> authenticatorIds) {
        return new FingerprintRemovalClient(context, lazyDaemon, token, null, new int[]{biometricId}, userId, owner, utils, sensorId, logger.swapAction(context, 4), biometricContext, authenticatorIds);
    }
}
