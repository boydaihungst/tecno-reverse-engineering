package com.android.server.biometrics.sensors;

import android.content.Context;
import android.os.IBinder;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public abstract class RevokeChallengeClient<T> extends HalClientMonitor<T> {
    public RevokeChallengeClient(Context context, Supplier<T> lazyDaemon, IBinder token, int userId, String owner, int sensorId, BiometricLogger biometricLogger, BiometricContext biometricContext) {
        super(context, lazyDaemon, token, null, userId, owner, 0, sensorId, biometricLogger, biometricContext);
    }

    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void unableToStart() {
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public void start(ClientMonitorCallback callback) {
        super.start(callback);
        startHalOperation();
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public int getProtoEnum() {
        return 11;
    }
}
