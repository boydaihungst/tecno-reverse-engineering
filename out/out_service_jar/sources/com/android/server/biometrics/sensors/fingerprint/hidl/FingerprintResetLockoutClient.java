package com.android.server.biometrics.sensors.fingerprint.hidl;

import android.content.Context;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
/* loaded from: classes.dex */
public class FingerprintResetLockoutClient extends BaseClientMonitor {
    final LockoutFrameworkImpl mLockoutTracker;

    public FingerprintResetLockoutClient(Context context, int userId, String owner, int sensorId, BiometricLogger logger, BiometricContext biometricContext, LockoutFrameworkImpl lockoutTracker) {
        super(context, null, null, userId, owner, 0, sensorId, logger, biometricContext);
        this.mLockoutTracker = lockoutTracker;
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public void start(ClientMonitorCallback callback) {
        super.start(callback);
        this.mLockoutTracker.resetFailedAttemptsForUser(true, getTargetUserId());
        callback.onClientFinished(this, true);
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public boolean interruptsPrecedingClients() {
        return true;
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public int getProtoEnum() {
        return 12;
    }
}
