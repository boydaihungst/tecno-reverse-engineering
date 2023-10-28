package com.android.server.biometrics.sensors;

import android.content.Context;
import android.os.IBinder;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public abstract class StopUserClient<T> extends HalClientMonitor<T> {
    private final UserStoppedCallback mUserStoppedCallback;

    /* loaded from: classes.dex */
    public interface UserStoppedCallback {
        void onUserStopped();
    }

    public void onUserStopped() {
        this.mUserStoppedCallback.onUserStopped();
        getCallback().onClientFinished(this, true);
    }

    public StopUserClient(Context context, Supplier<T> lazyDaemon, IBinder token, int userId, int sensorId, BiometricLogger logger, BiometricContext biometricContext, UserStoppedCallback callback) {
        super(context, lazyDaemon, token, null, userId, context.getOpPackageName(), 0, sensorId, logger, biometricContext);
        this.mUserStoppedCallback = callback;
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public int getProtoEnum() {
        return 16;
    }
}
