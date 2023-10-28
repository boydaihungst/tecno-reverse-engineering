package com.android.server.biometrics.sensors.face.aidl;

import android.content.Context;
import android.hardware.keymaster.HardwareAuthToken;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.HardwareAuthTokenUtils;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.ErrorConsumer;
import com.android.server.biometrics.sensors.HalClientMonitor;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class FaceSetFeatureClient extends HalClientMonitor<AidlSession> implements ErrorConsumer {
    private static final String TAG = "FaceSetFeatureClient";
    private final boolean mEnabled;
    private final int mFeature;
    private final HardwareAuthToken mHardwareAuthToken;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FaceSetFeatureClient(Context context, Supplier<AidlSession> lazyDaemon, IBinder token, ClientMonitorCallbackConverter listener, int userId, String owner, int sensorId, BiometricLogger logger, BiometricContext biometricContext, int feature, boolean enabled, byte[] hardwareAuthToken) {
        super(context, lazyDaemon, token, listener, userId, owner, 0, sensorId, logger, biometricContext);
        this.mFeature = feature;
        this.mEnabled = enabled;
        this.mHardwareAuthToken = HardwareAuthTokenUtils.toHardwareAuthToken(hardwareAuthToken);
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
            getFreshDaemon().getSession().setFeature(this.mHardwareAuthToken, AidlConversionUtils.convertFrameworkToAidlFeature(this.mFeature), this.mEnabled);
        } catch (RemoteException | IllegalArgumentException e) {
            Slog.e(TAG, "Unable to set feature: " + this.mFeature + " to enabled: " + this.mEnabled, e);
            this.mCallback.onClientFinished(this, false);
        }
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public int getProtoEnum() {
        return 8;
    }

    public void onFeatureSet(boolean success) {
        try {
            getListener().onFeatureSet(success, this.mFeature);
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception", e);
        }
        this.mCallback.onClientFinished(this, true);
    }

    @Override // com.android.server.biometrics.sensors.ErrorConsumer
    public void onError(int errorCode, int vendorCode) {
        try {
            getListener().onFeatureSet(false, this.mFeature);
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception", e);
        }
        this.mCallback.onClientFinished(this, false);
    }
}
