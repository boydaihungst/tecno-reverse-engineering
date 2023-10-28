package com.android.server.biometrics.sensors.face.aidl;

import android.content.Context;
import android.hardware.SensorPrivacyManager;
import android.hardware.biometrics.common.ICancellationSignal;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.AcquisitionClient;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.DetectionConsumer;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class FaceDetectClient extends AcquisitionClient<AidlSession> implements DetectionConsumer {
    private static final String TAG = "FaceDetectClient";
    private ICancellationSignal mCancellationSignal;
    private final boolean mIsStrongBiometric;
    private SensorPrivacyManager mSensorPrivacyManager;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FaceDetectClient(Context context, Supplier<AidlSession> lazyDaemon, IBinder token, long requestId, ClientMonitorCallbackConverter listener, int userId, String owner, int sensorId, BiometricLogger logger, BiometricContext biometricContext, boolean isStrongBiometric) {
        this(context, lazyDaemon, token, requestId, listener, userId, owner, sensorId, logger, biometricContext, isStrongBiometric, (SensorPrivacyManager) context.getSystemService(SensorPrivacyManager.class));
    }

    FaceDetectClient(Context context, Supplier<AidlSession> lazyDaemon, IBinder token, long requestId, ClientMonitorCallbackConverter listener, int userId, String owner, int sensorId, BiometricLogger logger, BiometricContext biometricContext, boolean isStrongBiometric, SensorPrivacyManager sensorPrivacyManager) {
        super(context, lazyDaemon, token, listener, userId, owner, 0, sensorId, true, logger, biometricContext);
        setRequestId(requestId);
        this.mIsStrongBiometric = isStrongBiometric;
        this.mSensorPrivacyManager = sensorPrivacyManager;
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public void start(ClientMonitorCallback callback) {
        super.start(callback);
        startHalOperation();
    }

    @Override // com.android.server.biometrics.sensors.AcquisitionClient
    protected void stopHalOperation() {
        ICancellationSignal iCancellationSignal = this.mCancellationSignal;
        if (iCancellationSignal != null) {
            try {
                iCancellationSignal.cancel();
            } catch (RemoteException e) {
                Slog.e(TAG, "Remote exception", e);
                this.mCallback.onClientFinished(this, false);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        SensorPrivacyManager sensorPrivacyManager = this.mSensorPrivacyManager;
        if (sensorPrivacyManager != null && sensorPrivacyManager.isSensorPrivacyEnabled(1, 2)) {
            onError(1, 0);
            this.mCallback.onClientFinished(this, false);
            return;
        }
        try {
            this.mCancellationSignal = doDetectInteraction();
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception when requesting face detect", e);
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
