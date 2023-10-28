package com.android.server.biometrics.sensors.face.hidl;

import android.content.Context;
import android.hardware.biometrics.face.V1_0.IBiometricsFace;
import android.hardware.face.IFaceServiceReceiver;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.util.Preconditions;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.GenerateChallengeClient;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class FaceGenerateChallengeClient extends GenerateChallengeClient<IBiometricsFace> {
    static final int CHALLENGE_TIMEOUT_SEC = 600;
    private static final ClientMonitorCallback EMPTY_CALLBACK = new ClientMonitorCallback() { // from class: com.android.server.biometrics.sensors.face.hidl.FaceGenerateChallengeClient.1
    };
    private static final String TAG = "FaceGenerateChallengeClient";
    private Long mChallengeResult;
    private final long mCreatedAt;
    private List<IFaceServiceReceiver> mWaiting;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FaceGenerateChallengeClient(Context context, Supplier<IBiometricsFace> lazyDaemon, IBinder token, ClientMonitorCallbackConverter listener, int userId, String owner, int sensorId, BiometricLogger logger, BiometricContext biometricContext, long now) {
        super(context, lazyDaemon, token, listener, userId, owner, sensorId, logger, biometricContext);
        this.mCreatedAt = now;
        this.mWaiting = new ArrayList();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        this.mChallengeResult = null;
        try {
            try {
                this.mChallengeResult = Long.valueOf(getFreshDaemon().generateChallenge(600).value);
                sendChallengeResult(getListener(), this.mCallback);
                for (IFaceServiceReceiver receiver : this.mWaiting) {
                    sendChallengeResult(new ClientMonitorCallbackConverter(receiver), EMPTY_CALLBACK);
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "generateChallenge failed", e);
                this.mCallback.onClientFinished(this, false);
            }
        } finally {
            this.mWaiting = null;
        }
    }

    public long getCreatedAt() {
        return this.mCreatedAt;
    }

    public void reuseResult(IFaceServiceReceiver receiver) {
        List<IFaceServiceReceiver> list = this.mWaiting;
        if (list != null) {
            list.add(receiver);
        } else {
            sendChallengeResult(new ClientMonitorCallbackConverter(receiver), EMPTY_CALLBACK);
        }
    }

    private void sendChallengeResult(ClientMonitorCallbackConverter receiver, ClientMonitorCallback ownerCallback) {
        Preconditions.checkState(this.mChallengeResult != null, "result not available");
        try {
            receiver.onChallengeGenerated(getSensorId(), getTargetUserId(), this.mChallengeResult.longValue());
            ownerCallback.onClientFinished(this, true);
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception", e);
            ownerCallback.onClientFinished(this, false);
        }
    }
}
