package com.android.server.biometrics.sensors.face.hidl;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.SensorPrivacyManager;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.face.V1_0.IBiometricsFace;
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
import com.android.server.biometrics.sensors.LockoutTracker;
import com.android.server.biometrics.sensors.face.UsageStats;
import java.util.ArrayList;
import java.util.function.Supplier;
/* loaded from: classes.dex */
class FaceAuthenticationClient extends AuthenticationClient<IBiometricsFace> {
    private static final String TAG = "FaceAuthenticationClient";
    private final int[] mBiometricPromptIgnoreList;
    private final int[] mBiometricPromptIgnoreListVendor;
    private final int[] mKeyguardIgnoreList;
    private final int[] mKeyguardIgnoreListVendor;
    private int mLastAcquire;
    private SensorPrivacyManager mSensorPrivacyManager;
    private final UsageStats mUsageStats;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FaceAuthenticationClient(Context context, Supplier<IBiometricsFace> lazyDaemon, IBinder token, long requestId, ClientMonitorCallbackConverter listener, int targetUserId, long operationId, boolean restricted, String owner, int cookie, boolean requireConfirmation, int sensorId, BiometricLogger logger, BiometricContext biometricContext, boolean isStrongBiometric, LockoutTracker lockoutTracker, UsageStats usageStats, boolean allowBackgroundAuthentication, boolean isKeyguardBypassEnabled) {
        super(context, lazyDaemon, token, listener, targetUserId, operationId, restricted, owner, cookie, requireConfirmation, sensorId, logger, biometricContext, isStrongBiometric, null, lockoutTracker, allowBackgroundAuthentication, true, isKeyguardBypassEnabled);
        setRequestId(requestId);
        this.mUsageStats = usageStats;
        this.mSensorPrivacyManager = (SensorPrivacyManager) context.getSystemService(SensorPrivacyManager.class);
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
        SensorPrivacyManager sensorPrivacyManager = this.mSensorPrivacyManager;
        if (sensorPrivacyManager != null && sensorPrivacyManager.isSensorPrivacyEnabled(1, 2)) {
            onError(1, 0);
            this.mCallback.onClientFinished(this, false);
            return;
        }
        try {
            getFreshDaemon().authenticate(this.mOperationId);
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception when requesting auth", e);
            onError(1, 0);
            this.mCallback.onClientFinished(this, false);
        }
    }

    @Override // com.android.server.biometrics.sensors.AcquisitionClient
    protected void stopHalOperation() {
        try {
            getFreshDaemon().cancel();
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception when requesting cancel", e);
            onError(1, 0);
            this.mCallback.onClientFinished(this, false);
        }
    }

    @Override // com.android.server.biometrics.sensors.AuthenticationClient
    public boolean wasUserDetected() {
        int i = this.mLastAcquire;
        return (i == 11 || i == 21) ? false : true;
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
        super.onError(error, vendorCode);
    }

    private int[] getAcquireIgnorelist() {
        return isBiometricPrompt() ? this.mBiometricPromptIgnoreList : this.mKeyguardIgnoreList;
    }

    private int[] getAcquireVendorIgnorelist() {
        return isBiometricPrompt() ? this.mBiometricPromptIgnoreListVendor : this.mKeyguardIgnoreListVendor;
    }

    private boolean shouldSend(int acquireInfo, int vendorCode) {
        if (acquireInfo == 22) {
            return !Utils.listContains(getAcquireVendorIgnorelist(), vendorCode);
        }
        return !Utils.listContains(getAcquireIgnorelist(), acquireInfo);
    }

    @Override // com.android.server.biometrics.sensors.AuthenticationClient, com.android.server.biometrics.sensors.AcquisitionClient
    public void onAcquired(int acquireInfo, int vendorCode) {
        this.mLastAcquire = acquireInfo;
        if (acquireInfo == 13) {
            BiometricNotificationUtils.showReEnrollmentNotification(getContext());
        }
        boolean shouldSend = shouldSend(acquireInfo, vendorCode);
        onAcquiredInternal(acquireInfo, vendorCode, shouldSend);
    }
}
