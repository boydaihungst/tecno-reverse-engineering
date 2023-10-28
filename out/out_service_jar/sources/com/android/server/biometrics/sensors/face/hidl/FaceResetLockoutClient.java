package com.android.server.biometrics.sensors.face.hidl;

import android.content.Context;
import android.hardware.biometrics.face.V1_0.IBiometricsFace;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.HalClientMonitor;
import java.util.ArrayList;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class FaceResetLockoutClient extends HalClientMonitor<IBiometricsFace> {
    private static final String TAG = "FaceResetLockoutClient";
    private final ArrayList<Byte> mHardwareAuthToken;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FaceResetLockoutClient(Context context, Supplier<IBiometricsFace> lazyDaemon, int userId, String owner, int sensorId, BiometricLogger logger, BiometricContext biometricContext, byte[] hardwareAuthToken) {
        super(context, lazyDaemon, null, null, userId, owner, 0, sensorId, logger, biometricContext);
        this.mHardwareAuthToken = new ArrayList<>();
        for (byte b : hardwareAuthToken) {
            this.mHardwareAuthToken.add(Byte.valueOf(b));
        }
    }

    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void unableToStart() {
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public void start(ClientMonitorCallback callback) {
        super.start(callback);
        startHalOperation();
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public boolean interruptsPrecedingClients() {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        try {
            getFreshDaemon().resetLockout(this.mHardwareAuthToken);
            this.mCallback.onClientFinished(this, true);
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to reset lockout", e);
            this.mCallback.onClientFinished(this, false);
        }
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public int getProtoEnum() {
        return 12;
    }
}
