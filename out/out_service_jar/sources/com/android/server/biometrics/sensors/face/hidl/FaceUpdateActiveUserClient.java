package com.android.server.biometrics.sensors.face.hidl;

import android.content.Context;
import android.hardware.biometrics.face.V1_0.IBiometricsFace;
import android.os.Environment;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.HalClientMonitor;
import java.io.File;
import java.util.Map;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class FaceUpdateActiveUserClient extends HalClientMonitor<IBiometricsFace> {
    private static final String FACE_DATA_DIR = "facedata";
    private static final String TAG = "FaceUpdateActiveUserClient";
    private final Map<Integer, Long> mAuthenticatorIds;
    private final boolean mHasEnrolledBiometrics;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FaceUpdateActiveUserClient(Context context, Supplier<IBiometricsFace> lazyDaemon, int userId, String owner, int sensorId, BiometricLogger logger, BiometricContext biometricContext, boolean hasEnrolledBiometrics, Map<Integer, Long> authenticatorIds) {
        super(context, lazyDaemon, null, null, userId, owner, 0, sensorId, logger, biometricContext);
        this.mHasEnrolledBiometrics = hasEnrolledBiometrics;
        this.mAuthenticatorIds = authenticatorIds;
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public void start(ClientMonitorCallback callback) {
        super.start(callback);
        startHalOperation();
    }

    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void unableToStart() {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        File storePath = new File(Environment.getDataVendorDeDirectory(getTargetUserId()), FACE_DATA_DIR);
        if (!storePath.exists()) {
            Slog.e(TAG, "vold has not created the directory?");
            this.mCallback.onClientFinished(this, false);
            return;
        }
        try {
            IBiometricsFace daemon = getFreshDaemon();
            daemon.setActiveUser(getTargetUserId(), storePath.getAbsolutePath());
            this.mAuthenticatorIds.put(Integer.valueOf(getTargetUserId()), Long.valueOf(this.mHasEnrolledBiometrics ? daemon.getAuthenticatorId().value : 0L));
            this.mCallback.onClientFinished(this, true);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to setActiveUser: " + e);
            this.mCallback.onClientFinished(this, false);
        }
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public int getProtoEnum() {
        return 1;
    }
}
