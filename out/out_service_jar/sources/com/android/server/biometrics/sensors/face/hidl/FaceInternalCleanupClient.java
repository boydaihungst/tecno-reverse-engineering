package com.android.server.biometrics.sensors.face.hidl;

import android.content.Context;
import android.hardware.biometrics.face.V1_0.IBiometricsFace;
import android.hardware.face.Face;
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
class FaceInternalCleanupClient extends InternalCleanupClient<Face, IBiometricsFace> {
    /* JADX INFO: Access modifiers changed from: package-private */
    public FaceInternalCleanupClient(Context context, Supplier<IBiometricsFace> lazyDaemon, int userId, String owner, int sensorId, BiometricLogger logger, BiometricContext biometricContext, List<Face> enrolledList, BiometricUtils<Face> utils, Map<Integer, Long> authenticatorIds) {
        super(context, lazyDaemon, userId, owner, sensorId, logger, biometricContext, enrolledList, utils, authenticatorIds);
    }

    @Override // com.android.server.biometrics.sensors.InternalCleanupClient
    protected InternalEnumerateClient<IBiometricsFace> getEnumerateClient(Context context, Supplier<IBiometricsFace> lazyDaemon, IBinder token, int userId, String owner, List<Face> enrolledList, BiometricUtils<Face> utils, int sensorId, BiometricLogger logger, BiometricContext biometricContext) {
        return new FaceInternalEnumerateClient(context, lazyDaemon, token, userId, owner, enrolledList, utils, sensorId, logger, biometricContext);
    }

    @Override // com.android.server.biometrics.sensors.InternalCleanupClient
    protected RemovalClient<Face, IBiometricsFace> getRemovalClient(Context context, Supplier<IBiometricsFace> lazyDaemon, IBinder token, int biometricId, int userId, String owner, BiometricUtils<Face> utils, int sensorId, BiometricLogger logger, BiometricContext biometricContext, Map<Integer, Long> authenticatorIds) {
        return new FaceRemovalClient(context, lazyDaemon, token, null, biometricId, userId, owner, utils, sensorId, logger, biometricContext, authenticatorIds);
    }
}
