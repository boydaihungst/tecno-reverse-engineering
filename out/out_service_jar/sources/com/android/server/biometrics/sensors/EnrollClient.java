package com.android.server.biometrics.sensors;

import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.transsion.hubcore.server.biometrics.fingerprint.ITranFingerprintUtils;
import java.util.Arrays;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public abstract class EnrollClient<T> extends AcquisitionClient<T> implements EnrollmentModifier {
    private static final String TAG = "Biometrics/EnrollClient";
    protected final BiometricUtils mBiometricUtils;
    private long mEnrollmentStartTimeMs;
    protected final byte[] mHardwareAuthToken;
    private final boolean mHasEnrollmentsBeforeStarting;
    protected final int mTimeoutSec;

    protected abstract boolean hasReachedEnrollmentLimit();

    public EnrollClient(Context context, Supplier<T> lazyDaemon, IBinder token, ClientMonitorCallbackConverter listener, int userId, byte[] hardwareAuthToken, String owner, BiometricUtils utils, int timeoutSec, int sensorId, boolean shouldVibrate, BiometricLogger logger, BiometricContext biometricContext) {
        super(context, lazyDaemon, token, listener, userId, owner, 0, sensorId, shouldVibrate, logger, biometricContext);
        this.mBiometricUtils = utils;
        this.mHardwareAuthToken = Arrays.copyOf(hardwareAuthToken, hardwareAuthToken.length);
        this.mTimeoutSec = timeoutSec;
        this.mHasEnrollmentsBeforeStarting = hasEnrollments();
    }

    @Override // com.android.server.biometrics.sensors.EnrollmentModifier
    public boolean hasEnrollmentStateChanged() {
        boolean hasEnrollmentsNow = hasEnrollments();
        return hasEnrollmentsNow != this.mHasEnrollmentsBeforeStarting;
    }

    @Override // com.android.server.biometrics.sensors.EnrollmentModifier
    public boolean hasEnrollments() {
        return !this.mBiometricUtils.getBiometricsForUser(getContext(), getTargetUserId()).isEmpty();
    }

    public void onEnrollResult(BiometricAuthenticator.Identifier identifier, int remaining) {
        if (this.mShouldVibrate) {
            ITranFingerprintUtils.Instance().vibrateFingerprintSuccess(getContext());
        }
        ClientMonitorCallbackConverter listener = getListener();
        if (listener != null) {
            try {
                listener.onEnrollResult(identifier, remaining);
            } catch (RemoteException e) {
                Slog.e(TAG, "Remote exception", e);
            }
        }
        if (remaining == 0) {
            this.mBiometricUtils.addBiometricForUser(getContext(), getTargetUserId(), identifier);
            getLogger().logOnEnrolled(getTargetUserId(), System.currentTimeMillis() - this.mEnrollmentStartTimeMs, true);
            this.mCallback.onClientFinished(this, true);
        }
        notifyUserActivity();
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public void start(ClientMonitorCallback callback) {
        super.start(callback);
        if (hasReachedEnrollmentLimit()) {
            Slog.e(TAG, "Reached enrollment limit");
            callback.onClientFinished(this, false);
            return;
        }
        this.mEnrollmentStartTimeMs = System.currentTimeMillis();
        startHalOperation();
    }

    @Override // com.android.server.biometrics.sensors.AcquisitionClient, com.android.server.biometrics.sensors.ErrorConsumer
    public void onError(int error, int vendorCode) {
        getLogger().logOnEnrolled(getTargetUserId(), System.currentTimeMillis() - this.mEnrollmentStartTimeMs, false);
        super.onError(error, vendorCode);
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public int getProtoEnum() {
        return 2;
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public boolean interruptsPrecedingClients() {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public int getOverlayReasonFromEnrollReason(int reason) {
        switch (reason) {
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                return 0;
        }
    }
}
