package com.android.server.biometrics.sensors;

import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.BiometricAuthenticator.Identifier;
import android.hardware.biometrics.IInvalidationCallback;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import java.util.Map;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public abstract class InvalidationClient<S extends BiometricAuthenticator.Identifier, T> extends HalClientMonitor<T> {
    private static final String TAG = "InvalidationClient";
    private final Map<Integer, Long> mAuthenticatorIds;
    private final IInvalidationCallback mInvalidationCallback;

    public InvalidationClient(Context context, Supplier<T> lazyDaemon, int userId, int sensorId, BiometricLogger logger, BiometricContext biometricContext, Map<Integer, Long> authenticatorIds, IInvalidationCallback callback) {
        super(context, lazyDaemon, null, null, userId, context.getOpPackageName(), 0, sensorId, logger, biometricContext);
        this.mAuthenticatorIds = authenticatorIds;
        this.mInvalidationCallback = callback;
    }

    public void onAuthenticatorIdInvalidated(long newAuthenticatorId) {
        this.mAuthenticatorIds.put(Integer.valueOf(getTargetUserId()), Long.valueOf(newAuthenticatorId));
        try {
            this.mInvalidationCallback.onCompleted();
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception", e);
        }
        this.mCallback.onClientFinished(this, true);
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public void start(ClientMonitorCallback callback) {
        super.start(callback);
        startHalOperation();
    }

    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void unableToStart() {
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public int getProtoEnum() {
        return 15;
    }
}
