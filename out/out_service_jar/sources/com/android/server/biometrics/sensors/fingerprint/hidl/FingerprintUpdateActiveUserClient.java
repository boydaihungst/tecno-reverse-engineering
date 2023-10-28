package com.android.server.biometrics.sensors.fingerprint.hidl;

import android.content.Context;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.os.Build;
import android.os.Environment;
import android.os.RemoteException;
import android.os.SELinux;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.HalClientMonitor;
import java.io.File;
import java.util.Map;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class FingerprintUpdateActiveUserClient extends HalClientMonitor<IBiometricsFingerprint> {
    private static final String FP_DATA_DIR = "fpdata";
    private static final String TAG = "FingerprintUpdateActiveUserClient";
    private final Map<Integer, Long> mAuthenticatorIds;
    private final Supplier<Integer> mCurrentUserId;
    private File mDirectory;
    private final boolean mForceUpdateAuthenticatorId;
    private final boolean mHasEnrolledBiometrics;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FingerprintUpdateActiveUserClient(Context context, Supplier<IBiometricsFingerprint> lazyDaemon, int userId, String owner, int sensorId, BiometricLogger logger, BiometricContext biometricContext, Supplier<Integer> currentUserId, boolean hasEnrolledBiometrics, Map<Integer, Long> authenticatorIds, boolean forceUpdateAuthenticatorId) {
        super(context, lazyDaemon, null, null, userId, owner, 0, sensorId, logger, biometricContext);
        this.mCurrentUserId = currentUserId;
        this.mForceUpdateAuthenticatorId = forceUpdateAuthenticatorId;
        this.mHasEnrolledBiometrics = hasEnrolledBiometrics;
        this.mAuthenticatorIds = authenticatorIds;
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public void start(ClientMonitorCallback callback) {
        File baseDir;
        super.start(callback);
        if (this.mCurrentUserId.get().intValue() == getTargetUserId() && !this.mForceUpdateAuthenticatorId) {
            Slog.d(TAG, "Already user: " + this.mCurrentUserId + ", returning");
            callback.onClientFinished(this, true);
            return;
        }
        int firstSdkInt = Build.VERSION.DEVICE_INITIAL_SDK_INT;
        if (firstSdkInt < 1) {
            Slog.e(TAG, "First SDK version " + firstSdkInt + " is invalid; must be at least VERSION_CODES.BASE");
        }
        if (firstSdkInt <= 27) {
            baseDir = Environment.getUserSystemDirectory(getTargetUserId());
        } else {
            baseDir = Environment.getDataVendorDeDirectory(getTargetUserId());
        }
        File file = new File(baseDir, FP_DATA_DIR);
        this.mDirectory = file;
        if (!file.exists()) {
            if (!this.mDirectory.mkdir()) {
                Slog.e(TAG, "Cannot make directory: " + this.mDirectory.getAbsolutePath());
                callback.onClientFinished(this, false);
                return;
            } else if (!SELinux.restorecon(this.mDirectory)) {
                Slog.e(TAG, "Restorecons failed. Directory will have wrong label.");
                callback.onClientFinished(this, false);
                return;
            }
        }
        startHalOperation();
    }

    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void unableToStart() {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        try {
            int targetId = getTargetUserId();
            Slog.d(TAG, "Setting active user: " + targetId);
            getFreshDaemon().setActiveGroup(targetId, this.mDirectory.getAbsolutePath());
            this.mAuthenticatorIds.put(Integer.valueOf(targetId), Long.valueOf(this.mHasEnrolledBiometrics ? getFreshDaemon().getAuthenticatorId() : 0L));
            this.mCallback.onClientFinished(this, true);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to setActiveGroup: " + e);
            this.mCallback.onClientFinished(this, false);
        }
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public int getProtoEnum() {
        return 1;
    }
}
