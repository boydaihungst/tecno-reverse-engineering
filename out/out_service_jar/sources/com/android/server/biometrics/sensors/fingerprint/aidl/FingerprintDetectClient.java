package com.android.server.biometrics.sensors.fingerprint.aidl;

import android.content.Context;
import android.hardware.biometrics.common.ICancellationSignal;
import android.hardware.fingerprint.IUdfpsOverlayController;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.AcquisitionClient;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.DetectionConsumer;
import com.android.server.biometrics.sensors.SensorOverlays;
import java.util.function.Supplier;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class FingerprintDetectClient extends AcquisitionClient<AidlSession> implements DetectionConsumer {
    private static final String TAG = "FingerprintDetectClient";
    private ICancellationSignal mCancellationSignal;
    private final boolean mIsStrongBiometric;
    private final SensorOverlays mSensorOverlays;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FingerprintDetectClient(Context context, Supplier<AidlSession> lazyDaemon, IBinder token, long requestId, ClientMonitorCallbackConverter listener, int userId, String owner, int sensorId, BiometricLogger biometricLogger, BiometricContext biometricContext, IUdfpsOverlayController udfpsOverlayController, boolean isStrongBiometric) {
        super(context, lazyDaemon, token, listener, userId, owner, 0, sensorId, true, biometricLogger, biometricContext);
        setRequestId(requestId);
        this.mIsStrongBiometric = isStrongBiometric;
        this.mSensorOverlays = new SensorOverlays(udfpsOverlayController, null);
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public void start(ClientMonitorCallback callback) {
        super.start(callback);
        startHalOperation();
    }

    @Override // com.android.server.biometrics.sensors.AcquisitionClient
    protected void stopHalOperation() {
        this.mSensorOverlays.hide(getSensorId());
        try {
            this.mCancellationSignal.cancel();
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception", e);
            this.mCallback.onClientFinished(this, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        this.mSensorOverlays.show(getSensorId(), 4, this);
        try {
            this.mCancellationSignal = doDetectInteraction();
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception when requesting finger detect", e);
            this.mSensorOverlays.hide(getSensorId());
            this.mCallback.onClientFinished(this, false);
        }
    }

    private ICancellationSignal doDetectInteraction() throws RemoteException {
        AidlSession session = getFreshDaemon();
        if (session.hasContextMethods()) {
            return session.getSession().detectInteractionWithContext(getOperationContext());
        }
        return session.getSession().detectInteraction();
    }

    @Override // com.android.server.biometrics.sensors.DetectionConsumer
    public void onInteractionDetected() {
        vibrateSuccess();
        try {
            getListener().onDetected(getSensorId(), getTargetUserId(), this.mIsStrongBiometric);
            this.mCallback.onClientFinished(this, true);
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception when sending onDetected", e);
            this.mCallback.onClientFinished(this, false);
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
