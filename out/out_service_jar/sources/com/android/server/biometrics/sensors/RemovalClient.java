package com.android.server.biometrics.sensors;

import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.BiometricAuthenticator.Identifier;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import java.util.Map;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public abstract class RemovalClient<S extends BiometricAuthenticator.Identifier, T> extends HalClientMonitor<T> implements RemovalConsumer, EnrollmentModifier {
    private static final String TAG = "Biometrics/RemovalClient";
    private final Map<Integer, Long> mAuthenticatorIds;
    private final BiometricUtils<S> mBiometricUtils;
    private final boolean mHasEnrollmentsBeforeStarting;

    public RemovalClient(Context context, Supplier<T> lazyDaemon, IBinder token, ClientMonitorCallbackConverter listener, int userId, String owner, BiometricUtils<S> utils, int sensorId, BiometricLogger logger, BiometricContext biometricContext, Map<Integer, Long> authenticatorIds) {
        super(context, lazyDaemon, token, listener, userId, owner, 0, sensorId, logger, biometricContext);
        this.mBiometricUtils = utils;
        this.mAuthenticatorIds = authenticatorIds;
        this.mHasEnrollmentsBeforeStarting = !utils.getBiometricsForUser(context, userId).isEmpty();
    }

    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void unableToStart() {
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public void start(ClientMonitorCallback callback) {
        super.start(callback);
        startHalOperation();
    }

    @Override // com.android.server.biometrics.sensors.RemovalConsumer
    public void onRemoved(BiometricAuthenticator.Identifier identifier, int remaining) {
        if (identifier == null) {
            Slog.e(TAG, "identifier was null, skipping onRemove()");
            try {
                if (getListener() == null) {
                    Slog.e(TAG, "Error, listener was null, not sending onError callback");
                } else {
                    getListener().onError(getSensorId(), getCookie(), 6, 0);
                }
            } catch (RemoteException e) {
                Slog.w(TAG, "Failed to send error to client for onRemoved", e);
            }
            this.mCallback.onClientFinished(this, false);
            return;
        }
        Slog.d(TAG, "onRemoved: " + identifier.getBiometricId() + " remaining: " + remaining);
        this.mBiometricUtils.removeBiometricForUser(getContext(), getTargetUserId(), identifier.getBiometricId());
        try {
            if (getListener() != null) {
                getListener().onRemoved(identifier, remaining);
            }
        } catch (RemoteException e2) {
            Slog.w(TAG, "Failed to notify Removed:", e2);
        }
        if (remaining == 0) {
            if (this.mBiometricUtils.getBiometricsForUser(getContext(), getTargetUserId()).isEmpty()) {
                Slog.d(TAG, "Last biometric removed for user: " + getTargetUserId());
                this.mAuthenticatorIds.put(Integer.valueOf(getTargetUserId()), 0L);
            }
            this.mCallback.onClientFinished(this, true);
        }
    }

    @Override // com.android.server.biometrics.sensors.EnrollmentModifier
    public boolean hasEnrollmentStateChanged() {
        boolean hasEnrollmentsNow = !this.mBiometricUtils.getBiometricsForUser(getContext(), getTargetUserId()).isEmpty();
        return hasEnrollmentsNow != this.mHasEnrollmentsBeforeStarting;
    }

    @Override // com.android.server.biometrics.sensors.EnrollmentModifier
    public boolean hasEnrollments() {
        return !this.mBiometricUtils.getBiometricsForUser(getContext(), getTargetUserId()).isEmpty();
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public int getProtoEnum() {
        return 4;
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public boolean interruptsPrecedingClients() {
        return true;
    }
}
