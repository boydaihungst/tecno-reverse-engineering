package com.android.server.biometrics.sensors.fingerprint.hidl;

import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.ISidefpsController;
import android.hardware.fingerprint.IUdfpsOverlayController;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.BiometricNotificationUtils;
import com.android.server.biometrics.sensors.BiometricUtils;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.ClientMonitorCompositeCallback;
import com.android.server.biometrics.sensors.EnrollClient;
import com.android.server.biometrics.sensors.SensorOverlays;
import com.android.server.biometrics.sensors.fingerprint.Udfps;
import com.android.server.biometrics.sensors.fingerprint.UdfpsHelper;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class FingerprintEnrollClient extends EnrollClient<IBiometricsFingerprint> implements Udfps {
    private static final String TAG = "FingerprintEnrollClient";
    private final int mEnrollReason;
    private boolean mIsPointerDown;
    private final SensorOverlays mSensorOverlays;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FingerprintEnrollClient(Context context, Supplier<IBiometricsFingerprint> lazyDaemon, IBinder token, long requestId, ClientMonitorCallbackConverter listener, int userId, byte[] hardwareAuthToken, String owner, BiometricUtils<Fingerprint> utils, int timeoutSec, int sensorId, BiometricLogger biometricLogger, BiometricContext biometricContext, IUdfpsOverlayController udfpsOverlayController, ISidefpsController sidefpsController, int enrollReason) {
        super(context, lazyDaemon, token, listener, userId, hardwareAuthToken, owner, utils, timeoutSec, sensorId, true, biometricLogger, biometricContext);
        setRequestId(requestId);
        this.mSensorOverlays = new SensorOverlays(udfpsOverlayController, sidefpsController);
        this.mEnrollReason = enrollReason;
        if (enrollReason == 1) {
            getLogger().disableMetrics();
        }
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    protected ClientMonitorCallback wrapCallbackForStart(ClientMonitorCallback callback) {
        return new ClientMonitorCompositeCallback(getLogger().createALSCallback(true), callback);
    }

    @Override // com.android.server.biometrics.sensors.EnrollClient
    protected boolean hasReachedEnrollmentLimit() {
        int limit = getContext().getResources().getInteger(17694832);
        int enrolled = this.mBiometricUtils.getBiometricsForUser(getContext(), getTargetUserId()).size();
        if (enrolled >= limit) {
            Slog.w(TAG, "Too many fingerprints registered, user: " + getTargetUserId());
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        this.mSensorOverlays.show(getSensorId(), getOverlayReasonFromEnrollReason(this.mEnrollReason), this);
        BiometricNotificationUtils.cancelBadCalibrationNotification(getContext());
        try {
            getFreshDaemon().enroll(this.mHardwareAuthToken, getTargetUserId(), this.mTimeoutSec);
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception when requesting enroll", e);
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

    @Override // com.android.server.biometrics.sensors.EnrollClient
    public void onEnrollResult(BiometricAuthenticator.Identifier identifier, final int remaining) {
        super.onEnrollResult(identifier, remaining);
        this.mSensorOverlays.ifUdfps(new SensorOverlays.OverlayControllerConsumer() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.FingerprintEnrollClient$$ExternalSyntheticLambda1
            @Override // com.android.server.biometrics.sensors.SensorOverlays.OverlayControllerConsumer
            public final void accept(Object obj) {
                FingerprintEnrollClient.this.m2548x621379d9(remaining, (IUdfpsOverlayController) obj);
            }
        });
        if (remaining == 0) {
            this.mSensorOverlays.hide(getSensorId());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onEnrollResult$0$com-android-server-biometrics-sensors-fingerprint-hidl-FingerprintEnrollClient  reason: not valid java name */
    public /* synthetic */ void m2548x621379d9(int remaining, IUdfpsOverlayController controller) throws RemoteException {
        controller.onEnrollmentProgress(getSensorId(), remaining);
    }

    @Override // com.android.server.biometrics.sensors.AcquisitionClient
    public void onAcquired(final int acquiredInfo, final int vendorCode) {
        super.onAcquired(acquiredInfo, vendorCode);
        this.mSensorOverlays.ifUdfps(new SensorOverlays.OverlayControllerConsumer() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.FingerprintEnrollClient$$ExternalSyntheticLambda0
            @Override // com.android.server.biometrics.sensors.SensorOverlays.OverlayControllerConsumer
            public final void accept(Object obj) {
                FingerprintEnrollClient.this.m2547x289b432d(acquiredInfo, vendorCode, (IUdfpsOverlayController) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onAcquired$1$com-android-server-biometrics-sensors-fingerprint-hidl-FingerprintEnrollClient  reason: not valid java name */
    public /* synthetic */ void m2547x289b432d(int acquiredInfo, int vendorCode, IUdfpsOverlayController controller) throws RemoteException {
        if (UdfpsHelper.isValidAcquisitionMessage(getContext(), acquiredInfo, vendorCode)) {
            controller.onEnrollmentHelp(getSensorId());
        }
    }

    @Override // com.android.server.biometrics.sensors.EnrollClient, com.android.server.biometrics.sensors.AcquisitionClient, com.android.server.biometrics.sensors.ErrorConsumer
    public void onError(int errorCode, int vendorCode) {
        super.onError(errorCode, vendorCode);
        this.mSensorOverlays.hide(getSensorId());
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
}
