package com.android.server.biometrics.sensors.fingerprint;

import android.hardware.biometrics.IBiometricAuthenticator;
import android.hardware.biometrics.IBiometricSensorReceiver;
import android.hardware.biometrics.IInvalidationCallback;
import android.hardware.biometrics.ITestSession;
import android.hardware.biometrics.ITestSessionCallback;
import android.hardware.biometrics.SensorPropertiesInternal;
import android.hardware.fingerprint.IFingerprintService;
import android.os.IBinder;
import android.os.RemoteException;
/* loaded from: classes.dex */
public final class FingerprintAuthenticator extends IBiometricAuthenticator.Stub {
    private final IFingerprintService mFingerprintService;
    private final int mSensorId;

    public FingerprintAuthenticator(IFingerprintService fingerprintService, int sensorId) {
        this.mFingerprintService = fingerprintService;
        this.mSensorId = sensorId;
    }

    public ITestSession createTestSession(ITestSessionCallback callback, String opPackageName) throws RemoteException {
        return this.mFingerprintService.createTestSession(this.mSensorId, callback, opPackageName);
    }

    public SensorPropertiesInternal getSensorProperties(String opPackageName) throws RemoteException {
        return this.mFingerprintService.getSensorProperties(this.mSensorId, opPackageName);
    }

    public byte[] dumpSensorServiceStateProto(boolean clearSchedulerBuffer) throws RemoteException {
        return this.mFingerprintService.dumpSensorServiceStateProto(this.mSensorId, clearSchedulerBuffer);
    }

    public void prepareForAuthentication(boolean requireConfirmation, IBinder token, long operationId, int userId, IBiometricSensorReceiver sensorReceiver, String opPackageName, long requestId, int cookie, boolean allowBackgroundAuthentication) throws RemoteException {
        this.mFingerprintService.prepareForAuthentication(this.mSensorId, token, operationId, userId, sensorReceiver, opPackageName, requestId, cookie, allowBackgroundAuthentication);
    }

    public void startPreparedClient(int cookie) throws RemoteException {
        this.mFingerprintService.startPreparedClient(this.mSensorId, cookie);
    }

    public void cancelAuthenticationFromService(IBinder token, String opPackageName, long requestId) throws RemoteException {
        this.mFingerprintService.cancelAuthenticationFromService(this.mSensorId, token, opPackageName, requestId);
    }

    public boolean isHardwareDetected(String opPackageName) throws RemoteException {
        return this.mFingerprintService.isHardwareDetected(this.mSensorId, opPackageName);
    }

    public boolean hasEnrolledTemplates(int userId, String opPackageName) throws RemoteException {
        return this.mFingerprintService.hasEnrolledFingerprints(this.mSensorId, userId, opPackageName);
    }

    public int getLockoutModeForUser(int userId) throws RemoteException {
        return this.mFingerprintService.getLockoutModeForUser(this.mSensorId, userId);
    }

    public void invalidateAuthenticatorId(int userId, IInvalidationCallback callback) throws RemoteException {
        this.mFingerprintService.invalidateAuthenticatorId(this.mSensorId, userId, callback);
    }

    public long getAuthenticatorId(int callingUserId) throws RemoteException {
        return this.mFingerprintService.getAuthenticatorId(this.mSensorId, callingUserId);
    }

    public void resetLockout(IBinder token, String opPackageName, int userId, byte[] hardwareAuthToken) throws RemoteException {
        this.mFingerprintService.resetLockout(token, this.mSensorId, userId, hardwareAuthToken, opPackageName);
    }
}
