package com.android.server.biometrics.sensors.face.aidl;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.ErrorConsumer;
import com.android.server.biometrics.sensors.HalClientMonitor;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class FaceGetFeatureClient extends HalClientMonitor<AidlSession> implements ErrorConsumer {
    private static final String TAG = "FaceGetFeatureClient";
    private final int mUserId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FaceGetFeatureClient(Context context, Supplier<AidlSession> lazyDaemon, IBinder token, ClientMonitorCallbackConverter listener, int userId, String owner, int sensorId, BiometricLogger logger, BiometricContext biometricContext) {
        super(context, lazyDaemon, token, listener, userId, owner, 0, sensorId, logger, biometricContext);
        this.mUserId = userId;
    }

    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void unableToStart() {
        this.mCallback.onClientFinished(this, false);
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
            getFreshDaemon().getSession().getFeatures();
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to getFeature", e);
            this.mCallback.onClientFinished(this, false);
        }
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public int getProtoEnum() {
        return 9;
    }

    public void onFeatureGet(boolean success, byte[] features) {
        try {
            HashMap<Integer, Boolean> featureMap = getFeatureMap();
            int[] featuresToSend = new int[featureMap.size()];
            boolean[] featureState = new boolean[featureMap.size()];
            for (byte b : features) {
                featureMap.put(Integer.valueOf(AidlConversionUtils.convertAidlToFrameworkFeature(b)), true);
            }
            int i = 0;
            for (Map.Entry<Integer, Boolean> entry : featureMap.entrySet()) {
                featuresToSend[i] = entry.getKey().intValue();
                featureState[i] = entry.getValue().booleanValue();
                i++;
            }
            boolean attentionEnabled = featureMap.get(1).booleanValue();
            Slog.d(TAG, "Updating attention value for user: " + this.mUserId + " to value: " + attentionEnabled);
            Settings.Secure.putIntForUser(getContext().getContentResolver(), "face_unlock_attention_required", attentionEnabled ? 1 : 0, this.mUserId);
            getListener().onFeatureGet(success, featuresToSend, featureState);
            this.mCallback.onClientFinished(this, true);
        } catch (RemoteException | IllegalArgumentException e) {
            Slog.e(TAG, "exception", e);
            this.mCallback.onClientFinished(this, false);
        }
    }

    private HashMap<Integer, Boolean> getFeatureMap() {
        HashMap<Integer, Boolean> featureMap = new HashMap<>();
        featureMap.put(1, false);
        return featureMap;
    }

    @Override // com.android.server.biometrics.sensors.ErrorConsumer
    public void onError(int errorCode, int vendorCode) {
        try {
            getListener().onFeatureGet(false, new int[0], new boolean[0]);
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception", e);
        }
        this.mCallback.onClientFinished(this, false);
    }
}
