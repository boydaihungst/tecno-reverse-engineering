package com.android.server.biometrics.sensors.face.aidl;

import android.content.Context;
import android.hardware.biometrics.ITestSession;
import android.hardware.biometrics.ITestSessionCallback;
import android.hardware.biometrics.face.AuthenticationFrame;
import android.hardware.biometrics.face.BaseFrame;
import android.hardware.face.Face;
import android.hardware.face.FaceAuthenticationFrame;
import android.hardware.face.FaceEnrollFrame;
import android.hardware.face.IFaceServiceReceiver;
import android.os.Binder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.HardwareAuthTokenUtils;
import com.android.server.biometrics.Utils;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.face.FaceUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
/* loaded from: classes.dex */
public class BiometricTestSessionImpl extends ITestSession.Stub {
    private static final String TAG = "face/aidl/BiometricTestSessionImpl";
    private final ITestSessionCallback mCallback;
    private final Context mContext;
    private final FaceProvider mProvider;
    private final Sensor mSensor;
    private final int mSensorId;
    private final IFaceServiceReceiver mReceiver = new IFaceServiceReceiver.Stub() { // from class: com.android.server.biometrics.sensors.face.aidl.BiometricTestSessionImpl.1
        public void onEnrollResult(Face face, int remaining) {
        }

        public void onAcquired(int acquireInfo, int vendorCode) {
        }

        public void onAuthenticationSucceeded(Face face, int userId, boolean isStrongBiometric) {
        }

        public void onFaceDetected(int sensorId, int userId, boolean isStrongBiometric) {
        }

        public void onAuthenticationFailed() {
        }

        public void onError(int error, int vendorCode) {
        }

        public void onRemoved(Face face, int remaining) {
        }

        public void onFeatureSet(boolean success, int feature) {
        }

        public void onFeatureGet(boolean success, int[] features, boolean[] featureState) {
        }

        public void onChallengeGenerated(int sensorId, int userId, long challenge) {
        }

        public void onAuthenticationFrame(FaceAuthenticationFrame frame) {
        }

        public void onEnrollmentFrame(FaceEnrollFrame frame) {
        }
    };
    private final Set<Integer> mEnrollmentIds = new HashSet();
    private final Random mRandom = new Random();

    /* JADX INFO: Access modifiers changed from: package-private */
    public BiometricTestSessionImpl(Context context, int sensorId, ITestSessionCallback callback, FaceProvider provider, Sensor sensor) {
        this.mContext = context;
        this.mSensorId = sensorId;
        this.mCallback = callback;
        this.mProvider = provider;
        this.mSensor = sensor;
    }

    public void setTestHalEnabled(boolean enabled) {
        Utils.checkPermission(this.mContext, "android.permission.TEST_BIOMETRIC");
        this.mProvider.setTestHalEnabled(enabled);
        this.mSensor.setTestHalEnabled(enabled);
    }

    public void startEnroll(int userId) {
        Utils.checkPermission(this.mContext, "android.permission.TEST_BIOMETRIC");
        this.mProvider.scheduleEnroll(this.mSensorId, new Binder(), new byte[69], userId, this.mReceiver, this.mContext.getOpPackageName(), new int[0], null, false);
    }

    public void finishEnroll(int userId) {
        Utils.checkPermission(this.mContext, "android.permission.TEST_BIOMETRIC");
        int nextRandomId = this.mRandom.nextInt();
        while (this.mEnrollmentIds.contains(Integer.valueOf(nextRandomId))) {
            nextRandomId = this.mRandom.nextInt();
        }
        this.mEnrollmentIds.add(Integer.valueOf(nextRandomId));
        this.mSensor.getSessionForUser(userId).getHalSessionCallback().onEnrollmentProgress(nextRandomId, 0);
    }

    public void acceptAuthentication(int userId) {
        Utils.checkPermission(this.mContext, "android.permission.TEST_BIOMETRIC");
        List<Face> faces = FaceUtils.getInstance(this.mSensorId).getBiometricsForUser(this.mContext, userId);
        if (faces.isEmpty()) {
            Slog.w(TAG, "No faces, returning");
            return;
        }
        int fid = faces.get(0).getBiometricId();
        this.mSensor.getSessionForUser(userId).getHalSessionCallback().onAuthenticationSucceeded(fid, HardwareAuthTokenUtils.toHardwareAuthToken(new byte[69]));
    }

    public void rejectAuthentication(int userId) {
        Utils.checkPermission(this.mContext, "android.permission.TEST_BIOMETRIC");
        this.mSensor.getSessionForUser(userId).getHalSessionCallback().onAuthenticationFailed();
    }

    public void notifyAcquired(int userId, int acquireInfo) {
        Utils.checkPermission(this.mContext, "android.permission.TEST_BIOMETRIC");
        BaseFrame data = new BaseFrame();
        data.acquiredInfo = (byte) acquireInfo;
        AuthenticationFrame authenticationFrame = new AuthenticationFrame();
        authenticationFrame.data = data;
        this.mSensor.getSessionForUser(userId).getHalSessionCallback().onAuthenticationFrame(authenticationFrame);
    }

    public void notifyError(int userId, int errorCode) {
        Utils.checkPermission(this.mContext, "android.permission.TEST_BIOMETRIC");
        this.mSensor.getSessionForUser(userId).getHalSessionCallback().onError((byte) errorCode, 0);
    }

    public void cleanupInternalState(int userId) {
        Utils.checkPermission(this.mContext, "android.permission.TEST_BIOMETRIC");
        Slog.d(TAG, "cleanupInternalState: " + userId);
        this.mProvider.scheduleInternalCleanup(this.mSensorId, userId, new ClientMonitorCallback() { // from class: com.android.server.biometrics.sensors.face.aidl.BiometricTestSessionImpl.2
            @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
            public void onClientStarted(BaseClientMonitor clientMonitor) {
                try {
                    Slog.d(BiometricTestSessionImpl.TAG, "onClientStarted: " + clientMonitor);
                    BiometricTestSessionImpl.this.mCallback.onCleanupStarted(clientMonitor.getTargetUserId());
                } catch (RemoteException e) {
                    Slog.e(BiometricTestSessionImpl.TAG, "Remote exception", e);
                }
            }

            @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
            public void onClientFinished(BaseClientMonitor clientMonitor, boolean success) {
                try {
                    Slog.d(BiometricTestSessionImpl.TAG, "onClientFinished: " + clientMonitor);
                    BiometricTestSessionImpl.this.mCallback.onCleanupFinished(clientMonitor.getTargetUserId());
                } catch (RemoteException e) {
                    Slog.e(BiometricTestSessionImpl.TAG, "Remote exception", e);
                }
            }
        });
    }
}
