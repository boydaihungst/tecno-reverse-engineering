package com.android.server.biometrics.sensors.fingerprint.hidl;

import android.app.TaskStackListener;
import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.hardware.fingerprint.ISidefpsController;
import android.hardware.fingerprint.IUdfpsOverlayController;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.log.CallbackWithProbe;
import com.android.server.biometrics.log.Probe;
import com.android.server.biometrics.sensors.AuthenticationClient;
import com.android.server.biometrics.sensors.BiometricNotificationUtils;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.ClientMonitorCompositeCallback;
import com.android.server.biometrics.sensors.SensorOverlays;
import com.android.server.biometrics.sensors.fingerprint.Udfps;
import com.android.server.biometrics.sensors.fingerprint.UdfpsHelper;
import java.util.ArrayList;
import java.util.function.Supplier;
/* loaded from: classes.dex */
class FingerprintAuthenticationClient extends AuthenticationClient<IBiometricsFingerprint> implements Udfps {
    private static final String TAG = "Biometrics/FingerprintAuthClient";
    private final CallbackWithProbe<Probe> mALSProbeCallback;
    private boolean mIsPointerDown;
    private final LockoutFrameworkImpl mLockoutFrameworkImpl;
    private final SensorOverlays mSensorOverlays;
    private final FingerprintSensorPropertiesInternal mSensorProps;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FingerprintAuthenticationClient(Context context, Supplier<IBiometricsFingerprint> lazyDaemon, IBinder token, long requestId, ClientMonitorCallbackConverter listener, int targetUserId, long operationId, boolean restricted, String owner, int cookie, boolean requireConfirmation, int sensorId, BiometricLogger logger, BiometricContext biometricContext, boolean isStrongBiometric, TaskStackListener taskStackListener, LockoutFrameworkImpl lockoutTracker, IUdfpsOverlayController udfpsOverlayController, ISidefpsController sidefpsController, boolean allowBackgroundAuthentication, FingerprintSensorPropertiesInternal sensorProps) {
        super(context, lazyDaemon, token, listener, targetUserId, operationId, restricted, owner, cookie, requireConfirmation, sensorId, logger, biometricContext, isStrongBiometric, taskStackListener, lockoutTracker, allowBackgroundAuthentication, true, false);
        setRequestId(requestId);
        this.mLockoutFrameworkImpl = lockoutTracker;
        this.mSensorOverlays = new SensorOverlays(udfpsOverlayController, sidefpsController);
        this.mSensorProps = sensorProps;
        this.mALSProbeCallback = getLogger().createALSCallback(false);
    }

    @Override // com.android.server.biometrics.sensors.AuthenticationClient, com.android.server.biometrics.sensors.BaseClientMonitor
    public void start(ClientMonitorCallback callback) {
        super.start(callback);
        if (this.mSensorProps.isAnyUdfpsType()) {
            this.mState = 2;
        } else {
            this.mState = 1;
        }
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    protected ClientMonitorCallback wrapCallbackForStart(ClientMonitorCallback callback) {
        return new ClientMonitorCompositeCallback(this.mALSProbeCallback, callback);
    }

    @Override // com.android.server.biometrics.sensors.AuthenticationClient, com.android.server.biometrics.sensors.AuthenticationConsumer
    public void onAuthenticated(BiometricAuthenticator.Identifier identifier, boolean authenticated, ArrayList<Byte> token) {
        int errorCode;
        super.onAuthenticated(identifier, authenticated, token);
        if (authenticated) {
            this.mState = 4;
            resetFailedAttempts(getTargetUserId());
            this.mSensorOverlays.hide(getSensorId());
            return;
        }
        this.mState = 3;
        int lockoutMode = this.mLockoutFrameworkImpl.getLockoutModeForUser(getTargetUserId());
        if (lockoutMode != 0) {
            Slog.w(TAG, "Fingerprint locked out, lockoutMode(" + lockoutMode + ")");
            if (lockoutMode == 1) {
                errorCode = 7;
            } else {
                errorCode = 9;
            }
            this.mSensorOverlays.hide(getSensorId());
            onErrorInternal(errorCode, 0, false);
            cancel();
        }
    }

    @Override // com.android.server.biometrics.sensors.AuthenticationClient, com.android.server.biometrics.sensors.AcquisitionClient, com.android.server.biometrics.sensors.ErrorConsumer
    public void onError(int errorCode, int vendorCode) {
        super.onError(errorCode, vendorCode);
        if (errorCode == 18) {
            BiometricNotificationUtils.showBadCalibrationNotification(getContext());
        }
        this.mSensorOverlays.hide(getSensorId());
    }

    private void resetFailedAttempts(int userId) {
        this.mLockoutFrameworkImpl.resetFailedAttemptsForUser(true, userId);
    }

    @Override // com.android.server.biometrics.sensors.AuthenticationClient
    protected void handleLifecycleAfterAuth(boolean authenticated) {
        if (authenticated) {
            this.mCallback.onClientFinished(this, true);
        }
    }

    @Override // com.android.server.biometrics.sensors.AuthenticationClient
    public boolean wasUserDetected() {
        return false;
    }

    @Override // com.android.server.biometrics.sensors.AuthenticationClient
    public int handleFailedAttempt(int userId) {
        this.mLockoutFrameworkImpl.addFailedAttemptForUser(userId);
        return super.handleFailedAttempt(userId);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        this.mSensorOverlays.show(getSensorId(), getShowOverlayReason(), this);
        try {
            getFreshDaemon().authenticate(this.mOperationId, getTargetUserId());
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception when requesting auth", e);
            onError(1, 0);
            this.mSensorOverlays.hide(getSensorId());
            this.mCallback.onClientFinished(this, false);
        }
    }

    @Override // com.android.server.biometrics.sensors.AcquisitionClient
    protected void stopHalOperation() {
        this.mSensorOverlays.hide(getSensorId());
        try {
            getFreshDaemon().cancel();
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception when requesting cancel", e);
            onError(1, 0);
            this.mCallback.onClientFinished(this, false);
        }
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.Udfps
    public void onPointerDown(int x, int y, float minor, float major) {
        this.mIsPointerDown = true;
        this.mState = 1;
        this.mALSProbeCallback.getProbe().enable();
        UdfpsHelper.onFingerDown(getFreshDaemon(), x, y, minor, major);
        if (getListener() != null) {
            try {
                getListener().onUdfpsPointerDown(getSensorId());
            } catch (RemoteException e) {
                Slog.e(TAG, "Remote exception", e);
            }
        }
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.Udfps
    public void onPointerUp() {
        this.mIsPointerDown = false;
        this.mState = 3;
        this.mALSProbeCallback.getProbe().disable();
        UdfpsHelper.onFingerUp(getFreshDaemon());
        if (getListener() != null) {
            try {
                getListener().onUdfpsPointerUp(getSensorId());
            } catch (RemoteException e) {
                Slog.e(TAG, "Remote exception", e);
            }
        }
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.Udfps
    public boolean isPointerDown() {
        return this.mIsPointerDown;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.Udfps
    public void onUiReady() {
    }
}
