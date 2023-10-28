package com.android.server.biometrics.sensors.fingerprint.aidl;

import android.content.Context;
import android.hardware.biometrics.ITestSession;
import android.hardware.biometrics.ITestSessionCallback;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.Binder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.HardwareAuthTokenUtils;
import com.android.server.biometrics.Utils;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.android.server.biometrics.sensors.BiometricStateCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.fingerprint.FingerprintUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class BiometricTestSessionImpl extends ITestSession.Stub {
    private static final String TAG = "fp/aidl/BiometricTestSessionImpl";
    private final BiometricStateCallback mBiometricStateCallback;
    private final ITestSessionCallback mCallback;
    private final Context mContext;
    private final FingerprintProvider mProvider;
    private final Sensor mSensor;
    private final int mSensorId;
    private final IFingerprintServiceReceiver mReceiver = new IFingerprintServiceReceiver.Stub() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.BiometricTestSessionImpl.1
        public void onEnrollResult(Fingerprint fp, int remaining) {
        }

        public void onAcquired(int acquiredInfo, int vendorCode) {
        }

        public void onAuthenticationSucceeded(Fingerprint fp, int userId, boolean isStrongBiometric) {
        }

        public void onFingerprintDetected(int sensorId, int userId, boolean isStrongBiometric) {
        }

        public void onAuthenticationFailed() {
        }

        public void onError(int error, int vendorCode) {
        }

        public void onRemoved(Fingerprint fp, int remaining) {
        }

        public void onChallengeGenerated(int sensorId, int userId, long challenge) {
        }

        public void onUdfpsPointerDown(int sensorId) {
        }

        public void onUdfpsPointerUp(int sensorId) {
        }
    };
    private final Set<Integer> mEnrollmentIds = new HashSet();
    private final Random mRandom = new Random();

    /* JADX INFO: Access modifiers changed from: package-private */
    public BiometricTestSessionImpl(Context context, int sensorId, ITestSessionCallback callback, BiometricStateCallback biometricStateCallback, FingerprintProvider provider, Sensor sensor) {
        this.mContext = context;
        this.mSensorId = sensorId;
        this.mCallback = callback;
        this.mBiometricStateCallback = biometricStateCallback;
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
        this.mProvider.scheduleEnroll(this.mSensorId, new Binder(), new byte[69], userId, this.mReceiver, this.mContext.getOpPackageName(), 2);
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
        List<Fingerprint> fingerprints = FingerprintUtils.getInstance(this.mSensorId).getBiometricsForUser(this.mContext, userId);
        if (fingerprints.isEmpty()) {
            Slog.w(TAG, "No fingerprints, returning");
            return;
        }
        int fid = fingerprints.get(0).getBiometricId();
        this.mSensor.getSessionForUser(userId).getHalSessionCallback().onAuthenticationSucceeded(fid, HardwareAuthTokenUtils.toHardwareAuthToken(new byte[69]));
    }

    public void rejectAuthentication(int userId) {
        Utils.checkPermission(this.mContext, "android.permission.TEST_BIOMETRIC");
        this.mSensor.getSessionForUser(userId).getHalSessionCallback().onAuthenticationFailed();
    }

    public void notifyAcquired(int userId, int acquireInfo) {
        Utils.checkPermission(this.mContext, "android.permission.TEST_BIOMETRIC");
        this.mSensor.getSessionForUser(userId).getHalSessionCallback().onAcquired((byte) acquireInfo, 0);
    }

    public void notifyError(int userId, int errorCode) {
        Utils.checkPermission(this.mContext, "android.permission.TEST_BIOMETRIC");
        this.mSensor.getSessionForUser(userId).getHalSessionCallback().onError((byte) errorCode, 0);
    }

    public void cleanupInternalState(int userId) {
        Utils.checkPermission(this.mContext, "android.permission.TEST_BIOMETRIC");
        Slog.d(TAG, "cleanupInternalState: " + userId);
        this.mProvider.scheduleInternalCleanup(this.mSensorId, userId, new ClientMonitorCallback() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.BiometricTestSessionImpl.2
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
