package com.android.server.biometrics.sensors;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public abstract class GenerateChallengeClient<T> extends HalClientMonitor<T> {
    private static final String TAG = "GenerateChallengeClient";

    public GenerateChallengeClient(Context context, Supplier<T> lazyDaemon, IBinder token, ClientMonitorCallbackConverter listener, int userId, String owner, int sensorId, BiometricLogger biometricLogger, BiometricContext biometricContext) {
        super(context, lazyDaemon, token, listener, userId, owner, 0, sensorId, biometricLogger, biometricContext);
    }

    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void unableToStart() {
        try {
            getListener().onChallengeGenerated(getSensorId(), getTargetUserId(), 0L);
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to send error", e);
        }
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public void start(ClientMonitorCallback callback) {
        super.start(callback);
        startHalOperation();
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public int getProtoEnum() {
        return 10;
    }
}
