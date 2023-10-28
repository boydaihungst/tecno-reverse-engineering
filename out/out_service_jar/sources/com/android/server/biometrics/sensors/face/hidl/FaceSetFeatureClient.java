package com.android.server.biometrics.sensors.face.hidl;

import android.content.Context;
import android.hardware.biometrics.face.V1_0.IBiometricsFace;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.HalClientMonitor;
import java.util.ArrayList;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class FaceSetFeatureClient extends HalClientMonitor<IBiometricsFace> {
    private static final String TAG = "FaceSetFeatureClient";
    private final boolean mEnabled;
    private final int mFaceId;
    private final int mFeature;
    private final ArrayList<Byte> mHardwareAuthToken;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FaceSetFeatureClient(Context context, Supplier<IBiometricsFace> lazyDaemon, IBinder token, ClientMonitorCallbackConverter listener, int userId, String owner, int sensorId, BiometricLogger logger, BiometricContext biometricContext, int feature, boolean enabled, byte[] hardwareAuthToken, int faceId) {
        super(context, lazyDaemon, token, listener, userId, owner, 0, sensorId, logger, biometricContext);
        this.mFeature = feature;
        this.mEnabled = enabled;
        this.mFaceId = faceId;
        this.mHardwareAuthToken = new ArrayList<>();
        for (byte b : hardwareAuthToken) {
            this.mHardwareAuthToken.add(Byte.valueOf(b));
        }
    }

    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void unableToStart() {
        try {
            getListener().onFeatureSet(false, this.mFeature);
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to send error", e);
        }
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
            int result = getFreshDaemon().setFeature(this.mFeature, this.mEnabled, this.mHardwareAuthToken, this.mFaceId);
            getListener().onFeatureSet(result == 0, this.mFeature);
            this.mCallback.onClientFinished(this, true);
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to set feature: " + this.mFeature + " to enabled: " + this.mEnabled, e);
            this.mCallback.onClientFinished(this, false);
        }
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public int getProtoEnum() {
        return 8;
    }
}
