package com.android.server.biometrics.sensors.face.aidl;

import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.SensorPrivacyManager;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.common.ICancellationSignal;
import android.hardware.face.FaceAuthenticationFrame;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.Utils;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.AuthenticationClient;
import com.android.server.biometrics.sensors.BiometricNotificationUtils;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.ClientMonitorCompositeCallback;
import com.android.server.biometrics.sensors.LockoutCache;
import com.android.server.biometrics.sensors.LockoutConsumer;
import com.android.server.biometrics.sensors.face.UsageStats;
import java.util.ArrayList;
import java.util.function.Supplier;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class FaceAuthenticationClient extends AuthenticationClient<AidlSession> implements LockoutConsumer {
    private static final String TAG = "FaceAuthenticationClient";
    private final int[] mBiometricPromptIgnoreList;
    private final int[] mBiometricPromptIgnoreListVendor;
    private ICancellationSignal mCancellationSignal;
    private final int[] mKeyguardIgnoreList;
    private final int[] mKeyguardIgnoreListVendor;
    private int mLastAcquire;
    private final LockoutCache mLockoutCache;
    private final NotificationManager mNotificationManager;
    private SensorPrivacyManager mSensorPrivacyManager;
    private final UsageStats mUsageStats;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FaceAuthenticationClient(Context context, Supplier<AidlSession> lazyDaemon, IBinder token, long requestId, ClientMonitorCallbackConverter listener, int targetUserId, long operationId, boolean restricted, String owner, int cookie, boolean requireConfirmation, int sensorId, BiometricLogger logger, BiometricContext biometricContext, boolean isStrongBiometric, UsageStats usageStats, LockoutCache lockoutCache, boolean allowBackgroundAuthentication, boolean isKeyguardBypassEnabled) {
        this(context, lazyDaemon, token, requestId, listener, targetUserId, operationId, restricted, owner, cookie, requireConfirmation, sensorId, logger, biometricContext, isStrongBiometric, usageStats, lockoutCache, allowBackgroundAuthentication, isKeyguardBypassEnabled, (SensorPrivacyManager) context.getSystemService(SensorPrivacyManager.class));
    }

    FaceAuthenticationClient(Context context, Supplier<AidlSession> lazyDaemon, IBinder token, long requestId, ClientMonitorCallbackConverter listener, int targetUserId, long operationId, boolean restricted, String owner, int cookie, boolean requireConfirmation, int sensorId, BiometricLogger logger, BiometricContext biometricContext, boolean isStrongBiometric, UsageStats usageStats, LockoutCache lockoutCache, boolean allowBackgroundAuthentication, boolean isKeyguardBypassEnabled, SensorPrivacyManager sensorPrivacyManager) {
        super(context, lazyDaemon, token, listener, targetUserId, operationId, restricted, owner, cookie, requireConfirmation, sensorId, logger, biometricContext, isStrongBiometric, null, lockoutCache, allowBackgroundAuthentication, true, isKeyguardBypassEnabled);
        this.mLastAcquire = 23;
        setRequestId(requestId);
        this.mUsageStats = usageStats;
        this.mLockoutCache = lockoutCache;
        this.mNotificationManager = (NotificationManager) context.getSystemService(NotificationManager.class);
        this.mSensorPrivacyManager = sensorPrivacyManager;
        Resources resources = getContext().getResources();
        this.mBiometricPromptIgnoreList = resources.getIntArray(17236062);
        this.mBiometricPromptIgnoreListVendor = resources.getIntArray(17236065);
        this.mKeyguardIgnoreList = resources.getIntArray(17236064);
        this.mKeyguardIgnoreListVendor = resources.getIntArray(17236067);
    }

    @Override // com.android.server.biometrics.sensors.AuthenticationClient, com.android.server.biometrics.sensors.BaseClientMonitor
    public void start(ClientMonitorCallback callback) {
        super.start(callback);
        this.mState = 1;
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    protected ClientMonitorCallback wrapCallbackForStart(ClientMonitorCallback callback) {
        return new ClientMonitorCompositeCallback(getLogger().createALSCallback(true), callback);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        try {
            SensorPrivacyManager sensorPrivacyManager = this.mSensorPrivacyManager;
            if (sensorPrivacyManager != null && sensorPrivacyManager.isSensorPrivacyEnabled(1, 2)) {
                onError(1, 0);
                this.mCallback.onClientFinished(this, false);
            } else {
                this.mCancellationSignal = doAuthenticate();
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception when requesting auth", e);
            onError(1, 0);
            this.mCallback.onClientFinished(this, false);
        }
    }

    private ICancellationSignal doAuthenticate() throws RemoteException {
        AidlSession session = getFreshDaemon();
        if (session.hasContextMethods()) {
            return session.getSession().authenticateWithContext(this.mOperationId, getOperationContext());
        }
        return session.getSession().authenticate(this.mOperationId);
    }

    @Override // com.android.server.biometrics.sensors.AcquisitionClient
    protected void stopHalOperation() {
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

    @Override // com.android.server.biometrics.sensors.AuthenticationClient
    public boolean wasUserDetected() {
        int i = this.mLastAcquire;
        return (i == 11 || i == 21 || i == 23) ? false : true;
    }

    @Override // com.android.server.biometrics.sensors.AuthenticationClient
    protected void handleLifecycleAfterAuth(boolean authenticated) {
        this.mCallback.onClientFinished(this, true);
    }

    @Override // com.android.server.biometrics.sensors.AuthenticationClient, com.android.server.biometrics.sensors.AuthenticationConsumer
    public void onAuthenticated(BiometricAuthenticator.Identifier identifier, boolean authenticated, ArrayList<Byte> token) {
        super.onAuthenticated(identifier, authenticated, token);
        this.mState = 4;
        this.mUsageStats.addEvent(new UsageStats.AuthenticationEvent(getStartTimeMs(), System.currentTimeMillis() - getStartTimeMs(), authenticated, 0, 0, getTargetUserId()));
    }

    @Override // com.android.server.biometrics.sensors.AuthenticationClient, com.android.server.biometrics.sensors.AcquisitionClient, com.android.server.biometrics.sensors.ErrorConsumer
    public void onError(int error, int vendorCode) {
        this.mUsageStats.addEvent(new UsageStats.AuthenticationEvent(getStartTimeMs(), System.currentTimeMillis() - getStartTimeMs(), false, error, vendorCode, getTargetUserId()));
        if (error == 16) {
            BiometricNotificationUtils.showReEnrollmentNotification(getContext());
        }
        super.onError(error, vendorCode);
    }

    private int[] getAcquireIgnorelist() {
        return isBiometricPrompt() ? this.mBiometricPromptIgnoreList : this.mKeyguardIgnoreList;
    }

    private int[] getAcquireVendorIgnorelist() {
        return isBiometricPrompt() ? this.mBiometricPromptIgnoreListVendor : this.mKeyguardIgnoreListVendor;
    }

    private boolean shouldSendAcquiredMessage(int acquireInfo, int vendorCode) {
        return acquireInfo == 22 ? !Utils.listContains(getAcquireVendorIgnorelist(), vendorCode) : !Utils.listContains(getAcquireIgnorelist(), acquireInfo);
    }

    @Override // com.android.server.biometrics.sensors.AuthenticationClient, com.android.server.biometrics.sensors.AcquisitionClient
    public void onAcquired(int acquireInfo, int vendorCode) {
        this.mLastAcquire = acquireInfo;
        boolean shouldSend = shouldSendAcquiredMessage(acquireInfo, vendorCode);
        onAcquiredInternal(acquireInfo, vendorCode, shouldSend);
    }

    public void onAuthenticationFrame(FaceAuthenticationFrame frame) {
        int acquireInfo = frame.getData().getAcquiredInfo();
        int vendorCode = frame.getData().getVendorCode();
        this.mLastAcquire = acquireInfo;
        onAcquiredInternal(acquireInfo, vendorCode, false);
        boolean shouldSend = shouldSendAcquiredMessage(acquireInfo, vendorCode);
        if (shouldSend && getListener() != null) {
            try {
                getListener().onAuthenticationFrame(frame);
            } catch (RemoteException e) {
                Slog.w(TAG, "Failed to send authentication frame", e);
                this.mCallback.onClientFinished(this, false);
            }
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
    }
}
