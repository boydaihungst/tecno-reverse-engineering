package com.android.server.biometrics.sensors.fingerprint.aidl;

import android.content.Context;
import android.hardware.keymaster.HardwareAuthToken;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.HardwareAuthTokenUtils;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ErrorConsumer;
import com.android.server.biometrics.sensors.HalClientMonitor;
import com.android.server.biometrics.sensors.LockoutCache;
import com.android.server.biometrics.sensors.LockoutResetDispatcher;
import java.util.function.Supplier;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class FingerprintResetLockoutClient extends HalClientMonitor<AidlSession> implements ErrorConsumer {
    private static final String TAG = "FingerprintResetLockoutClient";
    private final HardwareAuthToken mHardwareAuthToken;
    private final LockoutCache mLockoutCache;
    private final LockoutResetDispatcher mLockoutResetDispatcher;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FingerprintResetLockoutClient(Context context, Supplier<AidlSession> lazyDaemon, int userId, String owner, int sensorId, BiometricLogger biometricLogger, BiometricContext biometricContext, byte[] hardwareAuthToken, LockoutCache lockoutTracker, LockoutResetDispatcher lockoutResetDispatcher) {
        super(context, lazyDaemon, null, null, userId, owner, 0, sensorId, biometricLogger, biometricContext);
        this.mHardwareAuthToken = HardwareAuthTokenUtils.toHardwareAuthToken(hardwareAuthToken);
        this.mLockoutCache = lockoutTracker;
        this.mLockoutResetDispatcher = lockoutResetDispatcher;
    }

    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void unableToStart() {
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public void start(ClientMonitorCallback callback) {
        super.start(callback);
        startHalOperation();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        try {
            getFreshDaemon().getSession().resetLockout(this.mHardwareAuthToken);
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to reset lockout", e);
            this.mCallback.onClientFinished(this, false);
        }
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public boolean interruptsPrecedingClients() {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onLockoutCleared() {
        resetLocalLockoutStateToNone(getSensorId(), getTargetUserId(), this.mLockoutCache, this.mLockoutResetDispatcher);
        this.mCallback.onClientFinished(this, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void resetLocalLockoutStateToNone(int sensorId, int userId, LockoutCache lockoutTracker, LockoutResetDispatcher lockoutResetDispatcher) {
        lockoutTracker.setLockoutModeForUser(userId, 0);
        lockoutResetDispatcher.notifyLockoutResetCallbacks(sensorId);
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public int getProtoEnum() {
        return 12;
    }

    @Override // com.android.server.biometrics.sensors.ErrorConsumer
    public void onError(int errorCode, int vendorCode) {
        Slog.e(TAG, "Error during resetLockout: " + errorCode);
        this.mCallback.onClientFinished(this, false);
    }
}
