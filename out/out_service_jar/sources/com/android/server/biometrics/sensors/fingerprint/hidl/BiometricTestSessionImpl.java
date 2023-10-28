package com.android.server.biometrics.sensors.fingerprint.hidl;

import android.content.Context;
import android.hardware.biometrics.ITestSession;
import android.hardware.biometrics.ITestSessionCallback;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.Binder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.Utils;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.android.server.biometrics.sensors.BiometricStateCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.fingerprint.FingerprintUtils;
import com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
/* loaded from: classes.dex */
public class BiometricTestSessionImpl extends ITestSession.Stub {
    private static final String TAG = "BiometricTestSessionImpl";
    private final BiometricStateCallback mBiometricStateCallback;
    private final ITestSessionCallback mCallback;
    private final Context mContext;
    private final Fingerprint21 mFingerprint21;
    private final Fingerprint21.HalResultController mHalResultController;
    private final int mSensorId;
    private final IFingerprintServiceReceiver mReceiver = new IFingerprintServiceReceiver.Stub() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.BiometricTestSessionImpl.1
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
    public BiometricTestSessionImpl(Context context, int sensorId, ITestSessionCallback callback, BiometricStateCallback biometricStateCallback, Fingerprint21 fingerprint21, Fingerprint21.HalResultController halResultController) {
        this.mContext = context;
        this.mSensorId = sensorId;
        this.mCallback = callback;
        this.mFingerprint21 = fingerprint21;
        this.mBiometricStateCallback = biometricStateCallback;
        this.mHalResultController = halResultController;
    }

    public void setTestHalEnabled(boolean enabled) {
        Utils.checkPermission(this.mContext, "android.permission.TEST_BIOMETRIC");
        this.mFingerprint21.setTestHalEnabled(enabled);
    }

    public void startEnroll(int userId) {
        Utils.checkPermission(this.mContext, "android.permission.TEST_BIOMETRIC");
        this.mFingerprint21.scheduleEnroll(this.mSensorId, new Binder(), new byte[69], userId, this.mReceiver, this.mContext.getOpPackageName(), 2);
    }

    public void finishEnroll(int userId) {
        Utils.checkPermission(this.mContext, "android.permission.TEST_BIOMETRIC");
        int nextRandomId = this.mRandom.nextInt();
        while (this.mEnrollmentIds.contains(Integer.valueOf(nextRandomId))) {
            nextRandomId = this.mRandom.nextInt();
        }
        this.mEnrollmentIds.add(Integer.valueOf(nextRandomId));
        this.mHalResultController.onEnrollResult(0L, nextRandomId, userId, 0);
    }

    public void acceptAuthentication(int userId) {
        Utils.checkPermission(this.mContext, "android.permission.TEST_BIOMETRIC");
        List<Fingerprint> fingerprints = FingerprintUtils.getLegacyInstance(this.mSensorId).getBiometricsForUser(this.mContext, userId);
        if (fingerprints.isEmpty()) {
            Slog.w(TAG, "No fingerprints, returning");
            return;
        }
        int fid = fingerprints.get(0).getBiometricId();
        ArrayList<Byte> hat = new ArrayList<>(Collections.nCopies(69, (byte) 0));
        this.mHalResultController.onAuthenticated(0L, fid, userId, hat);
    }

    public void rejectAuthentication(int userId) {
        Utils.checkPermission(this.mContext, "android.permission.TEST_BIOMETRIC");
        this.mHalResultController.onAuthenticated(0L, 0, userId, null);
    }

    public void notifyAcquired(int userId, int acquireInfo) {
        Utils.checkPermission(this.mContext, "android.permission.TEST_BIOMETRIC");
        this.mHalResultController.onAcquired(0L, acquireInfo, 0);
    }

    public void notifyError(int userId, int errorCode) {
        Utils.checkPermission(this.mContext, "android.permission.TEST_BIOMETRIC");
        this.mHalResultController.onError(0L, errorCode, 0);
    }

    public void cleanupInternalState(int userId) {
        Utils.checkPermission(this.mContext, "android.permission.TEST_BIOMETRIC");
        this.mFingerprint21.scheduleInternalCleanup(this.mSensorId, userId, new ClientMonitorCallback() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.BiometricTestSessionImpl.2
            @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
            public void onClientStarted(BaseClientMonitor clientMonitor) {
                try {
                    BiometricTestSessionImpl.this.mCallback.onCleanupStarted(clientMonitor.getTargetUserId());
                } catch (RemoteException e) {
                    Slog.e(BiometricTestSessionImpl.TAG, "Remote exception", e);
                }
            }

            @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
            public void onClientFinished(BaseClientMonitor clientMonitor, boolean success) {
                try {
                    BiometricTestSessionImpl.this.mCallback.onCleanupFinished(clientMonitor.getTargetUserId());
                } catch (RemoteException e) {
                    Slog.e(BiometricTestSessionImpl.TAG, "Remote exception", e);
                }
            }
        });
    }
}
