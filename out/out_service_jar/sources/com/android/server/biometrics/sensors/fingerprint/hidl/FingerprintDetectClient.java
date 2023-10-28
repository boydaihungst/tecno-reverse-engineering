package com.android.server.biometrics.sensors.fingerprint.hidl;

import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.hardware.fingerprint.IUdfpsOverlayController;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.AcquisitionClient;
import com.android.server.biometrics.sensors.AuthenticationConsumer;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.PerformanceTracker;
import com.android.server.biometrics.sensors.SensorOverlays;
import com.android.server.biometrics.sensors.fingerprint.Udfps;
import com.android.server.biometrics.sensors.fingerprint.UdfpsHelper;
import java.util.ArrayList;
import java.util.function.Supplier;
/* loaded from: classes.dex */
class FingerprintDetectClient extends AcquisitionClient<IBiometricsFingerprint> implements AuthenticationConsumer, Udfps {
    private static final String TAG = "FingerprintDetectClient";
    private boolean mIsPointerDown;
    private final boolean mIsStrongBiometric;
    private final SensorOverlays mSensorOverlays;

    public FingerprintDetectClient(Context context, Supplier<IBiometricsFingerprint> lazyDaemon, IBinder token, long requestId, ClientMonitorCallbackConverter listener, int userId, String owner, int sensorId, BiometricLogger biometricLogger, BiometricContext biometricContext, IUdfpsOverlayController udfpsOverlayController, boolean isStrongBiometric) {
        super(context, lazyDaemon, token, listener, userId, owner, 0, sensorId, true, biometricLogger, biometricContext);
        setRequestId(requestId);
        this.mSensorOverlays = new SensorOverlays(udfpsOverlayController, null);
        this.mIsStrongBiometric = isStrongBiometric;
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

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public void start(ClientMonitorCallback callback) {
        super.start(callback);
        startHalOperation();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        this.mSensorOverlays.show(getSensorId(), 4, this);
        try {
            getFreshDaemon().authenticate(0L, getTargetUserId());
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception when requesting auth", e);
            onError(1, 0);
            this.mSensorOverlays.hide(getSensorId());
            this.mCallback.onClientFinished(this, false);
        }
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.Udfps
    public void onPointerDown(int x, int y, float minor, float major) {
        this.mIsPointerDown = true;
        UdfpsHelper.onFingerDown(getFreshDaemon(), x, y, minor, major);
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.Udfps
    public void onPointerUp() {
        this.mIsPointerDown = false;
        UdfpsHelper.onFingerUp(getFreshDaemon());
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.Udfps
    public boolean isPointerDown() {
        return this.mIsPointerDown;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.Udfps
    public void onUiReady() {
    }

    @Override // com.android.server.biometrics.sensors.AuthenticationConsumer
    public void onAuthenticated(BiometricAuthenticator.Identifier identifier, boolean authenticated, ArrayList<Byte> hardwareAuthToken) {
        getLogger().logOnAuthenticated(getContext(), getOperationContext(), authenticated, false, getTargetUserId(), false);
        vibrateSuccess();
        PerformanceTracker pm = PerformanceTracker.getInstanceForSensorId(getSensorId());
        pm.incrementAuthForUser(getTargetUserId(), authenticated);
        if (getListener() != null) {
            try {
                getListener().onDetected(getSensorId(), getTargetUserId(), this.mIsStrongBiometric);
            } catch (RemoteException e) {
                Slog.e(TAG, "Remote exception when sending onDetected", e);
            }
        }
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public int getProtoEnum() {
        return 13;
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public boolean interruptsPrecedingClients() {
        return true;
    }
}
