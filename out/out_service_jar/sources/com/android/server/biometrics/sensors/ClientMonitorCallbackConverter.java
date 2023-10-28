package com.android.server.biometrics.sensors;

import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.IBiometricSensorReceiver;
import android.hardware.face.Face;
import android.hardware.face.FaceAuthenticationFrame;
import android.hardware.face.FaceEnrollFrame;
import android.hardware.face.IFaceServiceReceiver;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.RemoteException;
/* loaded from: classes.dex */
public class ClientMonitorCallbackConverter {
    private IFaceServiceReceiver mFaceServiceReceiver;
    private IFingerprintServiceReceiver mFingerprintServiceReceiver;
    private IBiometricSensorReceiver mSensorReceiver;

    public ClientMonitorCallbackConverter(IBiometricSensorReceiver sensorReceiver) {
        this.mSensorReceiver = sensorReceiver;
    }

    public ClientMonitorCallbackConverter(IFaceServiceReceiver faceServiceReceiver) {
        this.mFaceServiceReceiver = faceServiceReceiver;
    }

    public ClientMonitorCallbackConverter(IFingerprintServiceReceiver fingerprintServiceReceiver) {
        this.mFingerprintServiceReceiver = fingerprintServiceReceiver;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAcquired(int sensorId, int acquiredInfo, int vendorCode) throws RemoteException {
        IBiometricSensorReceiver iBiometricSensorReceiver = this.mSensorReceiver;
        if (iBiometricSensorReceiver != null) {
            iBiometricSensorReceiver.onAcquired(sensorId, acquiredInfo, vendorCode);
            return;
        }
        IFaceServiceReceiver iFaceServiceReceiver = this.mFaceServiceReceiver;
        if (iFaceServiceReceiver != null) {
            iFaceServiceReceiver.onAcquired(acquiredInfo, vendorCode);
            return;
        }
        IFingerprintServiceReceiver iFingerprintServiceReceiver = this.mFingerprintServiceReceiver;
        if (iFingerprintServiceReceiver != null) {
            iFingerprintServiceReceiver.onAcquired(acquiredInfo, vendorCode);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAuthenticationSucceeded(int sensorId, BiometricAuthenticator.Identifier identifier, byte[] token, int userId, boolean isStrongBiometric) throws RemoteException {
        IBiometricSensorReceiver iBiometricSensorReceiver = this.mSensorReceiver;
        if (iBiometricSensorReceiver != null) {
            iBiometricSensorReceiver.onAuthenticationSucceeded(sensorId, token);
            return;
        }
        IFaceServiceReceiver iFaceServiceReceiver = this.mFaceServiceReceiver;
        if (iFaceServiceReceiver != null) {
            iFaceServiceReceiver.onAuthenticationSucceeded((Face) identifier, userId, isStrongBiometric);
            return;
        }
        IFingerprintServiceReceiver iFingerprintServiceReceiver = this.mFingerprintServiceReceiver;
        if (iFingerprintServiceReceiver != null) {
            iFingerprintServiceReceiver.onAuthenticationSucceeded((Fingerprint) identifier, userId, isStrongBiometric);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAuthenticationFailed(int sensorId) throws RemoteException {
        IBiometricSensorReceiver iBiometricSensorReceiver = this.mSensorReceiver;
        if (iBiometricSensorReceiver != null) {
            iBiometricSensorReceiver.onAuthenticationFailed(sensorId);
            return;
        }
        IFaceServiceReceiver iFaceServiceReceiver = this.mFaceServiceReceiver;
        if (iFaceServiceReceiver != null) {
            iFaceServiceReceiver.onAuthenticationFailed();
            return;
        }
        IFingerprintServiceReceiver iFingerprintServiceReceiver = this.mFingerprintServiceReceiver;
        if (iFingerprintServiceReceiver != null) {
            iFingerprintServiceReceiver.onAuthenticationFailed();
        }
    }

    public void onError(int sensorId, int cookie, int error, int vendorCode) throws RemoteException {
        IBiometricSensorReceiver iBiometricSensorReceiver = this.mSensorReceiver;
        if (iBiometricSensorReceiver != null) {
            iBiometricSensorReceiver.onError(sensorId, cookie, error, vendorCode);
            return;
        }
        IFaceServiceReceiver iFaceServiceReceiver = this.mFaceServiceReceiver;
        if (iFaceServiceReceiver != null) {
            iFaceServiceReceiver.onError(error, vendorCode);
            return;
        }
        IFingerprintServiceReceiver iFingerprintServiceReceiver = this.mFingerprintServiceReceiver;
        if (iFingerprintServiceReceiver != null) {
            iFingerprintServiceReceiver.onError(error, vendorCode);
        }
    }

    public void onDetected(int sensorId, int userId, boolean isStrongBiometric) throws RemoteException {
        IFaceServiceReceiver iFaceServiceReceiver = this.mFaceServiceReceiver;
        if (iFaceServiceReceiver != null) {
            iFaceServiceReceiver.onFaceDetected(sensorId, userId, isStrongBiometric);
            return;
        }
        IFingerprintServiceReceiver iFingerprintServiceReceiver = this.mFingerprintServiceReceiver;
        if (iFingerprintServiceReceiver != null) {
            iFingerprintServiceReceiver.onFingerprintDetected(sensorId, userId, isStrongBiometric);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onEnrollResult(BiometricAuthenticator.Identifier identifier, int remaining) throws RemoteException {
        IFaceServiceReceiver iFaceServiceReceiver = this.mFaceServiceReceiver;
        if (iFaceServiceReceiver != null) {
            iFaceServiceReceiver.onEnrollResult((Face) identifier, remaining);
            return;
        }
        IFingerprintServiceReceiver iFingerprintServiceReceiver = this.mFingerprintServiceReceiver;
        if (iFingerprintServiceReceiver != null) {
            iFingerprintServiceReceiver.onEnrollResult((Fingerprint) identifier, remaining);
        }
    }

    public void onRemoved(BiometricAuthenticator.Identifier identifier, int remaining) throws RemoteException {
        IFaceServiceReceiver iFaceServiceReceiver = this.mFaceServiceReceiver;
        if (iFaceServiceReceiver != null) {
            iFaceServiceReceiver.onRemoved((Face) identifier, remaining);
            return;
        }
        IFingerprintServiceReceiver iFingerprintServiceReceiver = this.mFingerprintServiceReceiver;
        if (iFingerprintServiceReceiver != null) {
            iFingerprintServiceReceiver.onRemoved((Fingerprint) identifier, remaining);
        }
    }

    public void onChallengeGenerated(int sensorId, int userId, long challenge) throws RemoteException {
        IFaceServiceReceiver iFaceServiceReceiver = this.mFaceServiceReceiver;
        if (iFaceServiceReceiver != null) {
            iFaceServiceReceiver.onChallengeGenerated(sensorId, userId, challenge);
            return;
        }
        IFingerprintServiceReceiver iFingerprintServiceReceiver = this.mFingerprintServiceReceiver;
        if (iFingerprintServiceReceiver != null) {
            iFingerprintServiceReceiver.onChallengeGenerated(sensorId, userId, challenge);
        }
    }

    public void onFeatureSet(boolean success, int feature) throws RemoteException {
        IFaceServiceReceiver iFaceServiceReceiver = this.mFaceServiceReceiver;
        if (iFaceServiceReceiver != null) {
            iFaceServiceReceiver.onFeatureSet(success, feature);
        }
    }

    public void onFeatureGet(boolean success, int[] features, boolean[] featureState) throws RemoteException {
        IFaceServiceReceiver iFaceServiceReceiver = this.mFaceServiceReceiver;
        if (iFaceServiceReceiver != null) {
            iFaceServiceReceiver.onFeatureGet(success, features, featureState);
        }
    }

    public void onUdfpsPointerDown(int sensorId) throws RemoteException {
        IFingerprintServiceReceiver iFingerprintServiceReceiver = this.mFingerprintServiceReceiver;
        if (iFingerprintServiceReceiver != null) {
            iFingerprintServiceReceiver.onUdfpsPointerDown(sensorId);
        }
    }

    public void onUdfpsPointerUp(int sensorId) throws RemoteException {
        IFingerprintServiceReceiver iFingerprintServiceReceiver = this.mFingerprintServiceReceiver;
        if (iFingerprintServiceReceiver != null) {
            iFingerprintServiceReceiver.onUdfpsPointerUp(sensorId);
        }
    }

    public void onAuthenticationFrame(FaceAuthenticationFrame frame) throws RemoteException {
        IFaceServiceReceiver iFaceServiceReceiver = this.mFaceServiceReceiver;
        if (iFaceServiceReceiver != null) {
            iFaceServiceReceiver.onAuthenticationFrame(frame);
        }
    }

    public void onEnrollmentFrame(FaceEnrollFrame frame) throws RemoteException {
        IFaceServiceReceiver iFaceServiceReceiver = this.mFaceServiceReceiver;
        if (iFaceServiceReceiver != null) {
            iFaceServiceReceiver.onEnrollmentFrame(frame);
        }
    }
}
