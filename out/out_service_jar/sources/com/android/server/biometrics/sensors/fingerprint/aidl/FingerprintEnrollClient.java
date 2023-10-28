package com.android.server.biometrics.sensors.fingerprint.aidl;

import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.common.ICancellationSignal;
import android.hardware.biometrics.common.OperationContext;
import android.hardware.biometrics.fingerprint.PointerContext;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.hardware.fingerprint.ISidefpsController;
import android.hardware.fingerprint.IUdfpsOverlayController;
import android.hardware.keymaster.HardwareAuthToken;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import android.view.accessibility.AccessibilityManager;
import com.android.server.biometrics.HardwareAuthTokenUtils;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.log.CallbackWithProbe;
import com.android.server.biometrics.log.Probe;
import com.android.server.biometrics.sensors.BiometricNotificationUtils;
import com.android.server.biometrics.sensors.BiometricUtils;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.ClientMonitorCompositeCallback;
import com.android.server.biometrics.sensors.EnrollClient;
import com.android.server.biometrics.sensors.SensorOverlays;
import com.android.server.biometrics.sensors.fingerprint.FingerprintUtils;
import com.android.server.biometrics.sensors.fingerprint.Udfps;
import com.android.server.biometrics.sensors.fingerprint.UdfpsHelper;
import java.util.function.Consumer;
import java.util.function.Supplier;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class FingerprintEnrollClient extends EnrollClient<AidlSession> implements Udfps {
    private static final String TAG = "FingerprintEnrollClient";
    private final CallbackWithProbe<Probe> mALSProbeCallback;
    private ICancellationSignal mCancellationSignal;
    private final int mEnrollReason;
    private boolean mIsPointerDown;
    private final int mMaxTemplatesPerUser;
    private final SensorOverlays mSensorOverlays;
    private final FingerprintSensorPropertiesInternal mSensorProps;

    private static boolean shouldVibrateFor(Context context, FingerprintSensorPropertiesInternal sensorProps) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(AccessibilityManager.class);
        boolean isAccessbilityEnabled = am.isTouchExplorationEnabled();
        return !sensorProps.isAnyUdfpsType() || isAccessbilityEnabled;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public FingerprintEnrollClient(Context context, Supplier<AidlSession> lazyDaemon, IBinder token, long requestId, ClientMonitorCallbackConverter listener, int userId, byte[] hardwareAuthToken, String owner, BiometricUtils<Fingerprint> utils, int sensorId, BiometricLogger logger, BiometricContext biometricContext, FingerprintSensorPropertiesInternal sensorProps, IUdfpsOverlayController udfpsOverlayController, ISidefpsController sidefpsController, int maxTemplatesPerUser, int enrollReason) {
        super(context, lazyDaemon, token, listener, userId, hardwareAuthToken, owner, utils, 0, sensorId, shouldVibrateFor(context, sensorProps), logger, biometricContext);
        setRequestId(requestId);
        this.mSensorProps = sensorProps;
        this.mSensorOverlays = new SensorOverlays(udfpsOverlayController, sidefpsController);
        this.mMaxTemplatesPerUser = maxTemplatesPerUser;
        this.mALSProbeCallback = getLogger().createALSCallback(false);
        this.mEnrollReason = enrollReason;
        if (enrollReason == 1) {
            getLogger().disableMetrics();
        }
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    protected ClientMonitorCallback wrapCallbackForStart(ClientMonitorCallback callback) {
        return new ClientMonitorCompositeCallback(this.mALSProbeCallback, getBiometricContextUnsubscriber(), callback);
    }

    @Override // com.android.server.biometrics.sensors.EnrollClient
    public void onEnrollResult(BiometricAuthenticator.Identifier identifier, final int remaining) {
        super.onEnrollResult(identifier, remaining);
        this.mSensorOverlays.ifUdfps(new SensorOverlays.OverlayControllerConsumer() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintEnrollClient$$ExternalSyntheticLambda3
            @Override // com.android.server.biometrics.sensors.SensorOverlays.OverlayControllerConsumer
            public final void accept(Object obj) {
                FingerprintEnrollClient.this.m2444xcc5c4640(remaining, (IUdfpsOverlayController) obj);
            }
        });
        if (remaining == 0) {
            this.mSensorOverlays.hide(getSensorId());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onEnrollResult$0$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintEnrollClient  reason: not valid java name */
    public /* synthetic */ void m2444xcc5c4640(int remaining, IUdfpsOverlayController controller) throws RemoteException {
        controller.onEnrollmentProgress(getSensorId(), remaining);
    }

    @Override // com.android.server.biometrics.sensors.AcquisitionClient
    public void onAcquired(final int acquiredInfo, final int vendorCode) {
        boolean acquiredGood = acquiredInfo == 0;
        if (this.mSensorProps.isAnyUdfpsType()) {
            if (acquiredGood && this.mShouldVibrate) {
                vibrateSuccess();
            }
            this.mSensorOverlays.ifUdfps(new SensorOverlays.OverlayControllerConsumer() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintEnrollClient$$ExternalSyntheticLambda0
                @Override // com.android.server.biometrics.sensors.SensorOverlays.OverlayControllerConsumer
                public final void accept(Object obj) {
                    FingerprintEnrollClient.this.m2442x92e40f94(acquiredInfo, (IUdfpsOverlayController) obj);
                }
            });
        }
        this.mSensorOverlays.ifUdfps(new SensorOverlays.OverlayControllerConsumer() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintEnrollClient$$ExternalSyntheticLambda1
            @Override // com.android.server.biometrics.sensors.SensorOverlays.OverlayControllerConsumer
            public final void accept(Object obj) {
                FingerprintEnrollClient.this.m2443x70d77573(acquiredInfo, vendorCode, (IUdfpsOverlayController) obj);
            }
        });
        super.onAcquired(acquiredInfo, vendorCode);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onAcquired$1$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintEnrollClient  reason: not valid java name */
    public /* synthetic */ void m2442x92e40f94(int acquiredInfo, IUdfpsOverlayController controller) throws RemoteException {
        controller.onAcquired(getSensorId(), acquiredInfo);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onAcquired$2$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintEnrollClient  reason: not valid java name */
    public /* synthetic */ void m2443x70d77573(int acquiredInfo, int vendorCode, IUdfpsOverlayController controller) throws RemoteException {
        if (UdfpsHelper.isValidAcquisitionMessage(getContext(), acquiredInfo, vendorCode)) {
            controller.onEnrollmentHelp(getSensorId());
        }
    }

    @Override // com.android.server.biometrics.sensors.EnrollClient, com.android.server.biometrics.sensors.AcquisitionClient, com.android.server.biometrics.sensors.ErrorConsumer
    public void onError(int errorCode, int vendorCode) {
        super.onError(errorCode, vendorCode);
        this.mSensorOverlays.hide(getSensorId());
    }

    @Override // com.android.server.biometrics.sensors.EnrollClient
    protected boolean hasReachedEnrollmentLimit() {
        return FingerprintUtils.getInstance(getSensorId()).getBiometricsForUser(getContext(), getTargetUserId()).size() >= this.mMaxTemplatesPerUser;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        this.mSensorOverlays.show(getSensorId(), getOverlayReasonFromEnrollReason(this.mEnrollReason), this);
        BiometricNotificationUtils.cancelBadCalibrationNotification(getContext());
        try {
            this.mCancellationSignal = doEnroll();
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception when requesting enroll", e);
            onError(2, 0);
            this.mCallback.onClientFinished(this, false);
        }
    }

    private ICancellationSignal doEnroll() throws RemoteException {
        final AidlSession session = getFreshDaemon();
        HardwareAuthToken hat = HardwareAuthTokenUtils.toHardwareAuthToken(this.mHardwareAuthToken);
        if (session.hasContextMethods()) {
            OperationContext opContext = getOperationContext();
            ICancellationSignal cancel = session.getSession().enrollWithContext(hat, opContext);
            getBiometricContext().subscribe(opContext, new Consumer() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintEnrollClient$$ExternalSyntheticLambda2
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    FingerprintEnrollClient.lambda$doEnroll$3(AidlSession.this, (OperationContext) obj);
                }
            });
            return cancel;
        }
        return session.getSession().enroll(hat);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$doEnroll$3(AidlSession session, OperationContext ctx) {
        try {
            session.getSession().onContextChanged(ctx);
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to notify context changed", e);
        }
    }

    @Override // com.android.server.biometrics.sensors.AcquisitionClient
    protected void stopHalOperation() {
        this.mSensorOverlays.hide(getSensorId());
        unsubscribeBiometricContext();
        ICancellationSignal iCancellationSignal = this.mCancellationSignal;
        if (iCancellationSignal != null) {
            try {
                iCancellationSignal.cancel();
            } catch (RemoteException e) {
                Slog.e(TAG, "Remote exception when requesting cancel", e);
                onError(1, 0);
                this.mCallback.onClientFinished(this, false);
            }
        }
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.Udfps
    public void onPointerDown(int x, int y, float minor, float major) {
        try {
            this.mIsPointerDown = true;
            this.mALSProbeCallback.getProbe().enable();
            AidlSession session = getFreshDaemon();
            if (session.hasContextMethods()) {
                PointerContext context = new PointerContext();
                context.pointerId = 0;
                context.x = x;
                context.y = y;
                context.minor = minor;
                context.major = major;
                context.isAod = getBiometricContext().isAod();
                session.getSession().onPointerDownWithContext(context);
            } else {
                session.getSession().onPointerDown(0, x, y, minor, major);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to send pointer down", e);
        }
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.Udfps
    public void onPointerUp() {
        try {
            this.mIsPointerDown = false;
            this.mALSProbeCallback.getProbe().disable();
            AidlSession session = getFreshDaemon();
            if (!session.hasContextMethods()) {
                session.getSession().onPointerUp(0);
            } else {
                PointerContext context = new PointerContext();
                context.pointerId = 0;
                session.getSession().onPointerUpWithContext(context);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to send pointer up", e);
        }
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.Udfps
    public boolean isPointerDown() {
        return this.mIsPointerDown;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.Udfps
    public void onUiReady() {
        try {
            getFreshDaemon().getSession().onUiReady();
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to send UI ready", e);
        }
    }
}
