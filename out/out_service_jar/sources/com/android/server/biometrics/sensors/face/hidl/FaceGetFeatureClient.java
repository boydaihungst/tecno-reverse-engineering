package com.android.server.biometrics.sensors.face.hidl;

import android.content.Context;
import android.hardware.biometrics.face.V1_0.IBiometricsFace;
import android.hardware.biometrics.face.V1_0.OptionalBool;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.HalClientMonitor;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class FaceGetFeatureClient extends HalClientMonitor<IBiometricsFace> {
    private static final String TAG = "FaceGetFeatureClient";
    private final int mFaceId;
    private final int mFeature;
    private boolean mValue;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FaceGetFeatureClient(Context context, Supplier<IBiometricsFace> lazyDaemon, IBinder token, ClientMonitorCallbackConverter listener, int userId, String owner, int sensorId, BiometricLogger logger, BiometricContext biometricContext, int feature, int faceId) {
        super(context, lazyDaemon, token, listener, userId, owner, 0, sensorId, logger, biometricContext);
        this.mFeature = feature;
        this.mFaceId = faceId;
    }

    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void unableToStart() {
        try {
            if (getListener() != null) {
                getListener().onFeatureGet(false, new int[0], new boolean[0]);
            }
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
        boolean z;
        try {
            OptionalBool result = getFreshDaemon().getFeature(this.mFeature, this.mFaceId);
            int[] features = {this.mFeature};
            boolean[] featureState = {result.value};
            this.mValue = result.value;
            if (getListener() != null) {
                ClientMonitorCallbackConverter listener = getListener();
                if (result.status != 0) {
                    z = false;
                } else {
                    z = true;
                }
                listener.onFeatureGet(z, features, featureState);
            }
            this.mCallback.onClientFinished(this, true);
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to getFeature", e);
            this.mCallback.onClientFinished(this, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getValue() {
        return this.mValue;
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public int getProtoEnum() {
        return 9;
    }
}
