package com.android.server.biometrics.sensors.fingerprint.aidl;

import android.app.TaskStackListener;
import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.common.ICancellationSignal;
import android.hardware.biometrics.common.OperationContext;
import android.hardware.biometrics.fingerprint.PointerContext;
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
import com.android.server.biometrics.sensors.LockoutCache;
import com.android.server.biometrics.sensors.LockoutConsumer;
import com.android.server.biometrics.sensors.SensorOverlays;
import com.android.server.biometrics.sensors.fingerprint.Udfps;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class FingerprintAuthenticationClient extends AuthenticationClient<AidlSession> implements Udfps, LockoutConsumer {
    private static final String TAG = "FingerprintAuthenticationClient";
    private final CallbackWithProbe<Probe> mALSProbeCallback;
    private ICancellationSignal mCancellationSignal;
    private boolean mIsPointerDown;
    private final LockoutCache mLockoutCache;
    private final SensorOverlays mSensorOverlays;
    private final FingerprintSensorPropertiesInternal mSensorProps;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FingerprintAuthenticationClient(Context context, Supplier<AidlSession> lazyDaemon, IBinder token, long requestId, ClientMonitorCallbackConverter listener, int targetUserId, long operationId, boolean restricted, String owner, int cookie, boolean requireConfirmation, int sensorId, BiometricLogger biometricLogger, BiometricContext biometricContext, boolean isStrongBiometric, TaskStackListener taskStackListener, LockoutCache lockoutCache, IUdfpsOverlayController udfpsOverlayController, ISidefpsController sidefpsController, boolean allowBackgroundAuthentication, FingerprintSensorPropertiesInternal sensorProps) {
        super(context, lazyDaemon, token, listener, targetUserId, operationId, restricted, owner, cookie, requireConfirmation, sensorId, biometricLogger, biometricContext, isStrongBiometric, taskStackListener, lockoutCache, allowBackgroundAuthentication, true, false);
        setRequestId(requestId);
        this.mLockoutCache = lockoutCache;
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
        return new ClientMonitorCompositeCallback(this.mALSProbeCallback, getBiometricContextUnsubscriber(), callback);
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

    @Override // com.android.server.biometrics.sensors.AuthenticationClient, com.android.server.biometrics.sensors.AuthenticationConsumer
    public void onAuthenticated(BiometricAuthenticator.Identifier identifier, boolean authenticated, ArrayList<Byte> token) {
        super.onAuthenticated(identifier, authenticated, token);
        if (authenticated) {
            this.mState = 4;
            this.mSensorOverlays.hide(getSensorId());
            return;
        }
        this.mState = 3;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onAcquired$0$com-android-server-biometrics-sensors-fingerprint-aidl-FingerprintAuthenticationClient  reason: not valid java name */
    public /* synthetic */ void m2441x2bf17207(int acquiredInfo, IUdfpsOverlayController controller) throws RemoteException {
        controller.onAcquired(getSensorId(), acquiredInfo);
    }

    @Override // com.android.server.biometrics.sensors.AuthenticationClient, com.android.server.biometrics.sensors.AcquisitionClient
    public void onAcquired(final int acquiredInfo, int vendorCode) {
        this.mSensorOverlays.ifUdfps(new SensorOverlays.OverlayControllerConsumer() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintAuthenticationClient$$ExternalSyntheticLambda0
            @Override // com.android.server.biometrics.sensors.SensorOverlays.OverlayControllerConsumer
            public final void accept(Object obj) {
                FingerprintAuthenticationClient.this.m2441x2bf17207(acquiredInfo, (IUdfpsOverlayController) obj);
            }
        });
        super.onAcquired(acquiredInfo, vendorCode);
    }

    @Override // com.android.server.biometrics.sensors.AuthenticationClient, com.android.server.biometrics.sensors.AcquisitionClient, com.android.server.biometrics.sensors.ErrorConsumer
    public void onError(int errorCode, int vendorCode) {
        super.onError(errorCode, vendorCode);
        if (errorCode == 18) {
            BiometricNotificationUtils.showBadCalibrationNotification(getContext());
        }
        this.mSensorOverlays.hide(getSensorId());
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        this.mSensorOverlays.show(getSensorId(), getShowOverlayReason(), this);
        try {
            this.mCancellationSignal = doAuthenticate();
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception", e);
            onError(1, 0);
            this.mSensorOverlays.hide(getSensorId());
            this.mCallback.onClientFinished(this, false);
        }
    }

    private ICancellationSignal doAuthenticate() throws RemoteException {
        final AidlSession session = getFreshDaemon();
        if (session.hasContextMethods()) {
            OperationContext opContext = getOperationContext();
            ICancellationSignal cancel = session.getSession().authenticateWithContext(this.mOperationId, opContext);
            getBiometricContext().subscribe(opContext, new Consumer() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.FingerprintAuthenticationClient$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    FingerprintAuthenticationClient.lambda$doAuthenticate$1(AidlSession.this, (OperationContext) obj);
                }
            });
            return cancel;
        }
        return session.getSession().authenticate(this.mOperationId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$doAuthenticate$1(AidlSession session, OperationContext ctx) {
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
                return;
            } catch (RemoteException e) {
                Slog.e(TAG, "Remote exception", e);
                onError(1, 0);
                this.mCallback.onClientFinished(this, false);
                return;
            }
        }
        Slog.e(TAG, "cancellation signal was null");
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.Udfps
    public void onPointerDown(int x, int y, float minor, float major) {
        try {
            this.mIsPointerDown = true;
            this.mState = 1;
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
            if (getListener() != null) {
                getListener().onUdfpsPointerDown(getSensorId());
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception", e);
        }
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.Udfps
    public void onPointerUp() {
        try {
            this.mIsPointerDown = false;
            this.mState = 3;
            this.mALSProbeCallback.getProbe().disable();
            AidlSession session = getFreshDaemon();
            if (!session.hasContextMethods()) {
                session.getSession().onPointerUp(0);
            } else {
                PointerContext context = new PointerContext();
                context.pointerId = 0;
                session.getSession().onPointerUpWithContext(context);
            }
            if (getListener() != null) {
                getListener().onUdfpsPointerUp(getSensorId());
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception", e);
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
            Slog.e(TAG, "Remote exception", e);
        }
    }

    @Override // com.android.server.biometrics.sensors.AuthenticationClient, com.android.server.biometrics.sensors.LockoutConsumer
    public void onLockoutTimed(long durationMillis) {
        super.onLockoutTimed(durationMillis);
        this.mLockoutCache.setLockoutModeForUser(getTargetUserId(), 1);
        getLogger().logOnError(getContext(), getOperationContext(), 7, 0, getTargetUserId());
        try {
            getListener().onError(getSensorId(), getCookie(), 7, 0);
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception", e);
        }
        this.mSensorOverlays.hide(getSensorId());
        this.mCallback.onClientFinished(this, false);
    }

    @Override // com.android.server.biometrics.sensors.AuthenticationClient, com.android.server.biometrics.sensors.LockoutConsumer
    public void onLockoutPermanent() {
        super.onLockoutPermanent();
        this.mLockoutCache.setLockoutModeForUser(getTargetUserId(), 2);
        getLogger().logOnError(getContext(), getOperationContext(), 9, 0, getTargetUserId());
        try {
            getListener().onError(getSensorId(), getCookie(), 9, 0);
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception", e);
        }
        this.mSensorOverlays.hide(getSensorId());
        this.mCallback.onClientFinished(this, false);
    }
}
