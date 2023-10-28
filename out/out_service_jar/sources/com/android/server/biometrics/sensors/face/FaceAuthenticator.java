package com.android.server.biometrics.sensors.face;

import android.hardware.biometrics.IBiometricAuthenticator;
import android.hardware.biometrics.IBiometricSensorReceiver;
import android.hardware.biometrics.IInvalidationCallback;
import android.hardware.biometrics.ITestSession;
import android.hardware.biometrics.ITestSessionCallback;
import android.hardware.biometrics.SensorPropertiesInternal;
import android.hardware.face.IFaceService;
import android.os.IBinder;
import android.os.RemoteException;
/* loaded from: classes.dex */
public final class FaceAuthenticator extends IBiometricAuthenticator.Stub {
    private final IFaceService mFaceService;
    private final int mSensorId;

    public FaceAuthenticator(IFaceService faceService, int sensorId) {
        this.mFaceService = faceService;
        this.mSensorId = sensorId;
    }

    public ITestSession createTestSession(ITestSessionCallback callback, String opPackageName) throws RemoteException {
        return this.mFaceService.createTestSession(this.mSensorId, callback, opPackageName);
    }

    public SensorPropertiesInternal getSensorProperties(String opPackageName) throws RemoteException {
        return this.mFaceService.getSensorProperties(this.mSensorId, opPackageName);
    }

    public byte[] dumpSensorServiceStateProto(boolean clearSchedulerBuffer) throws RemoteException {
        return this.mFaceService.dumpSensorServiceStateProto(this.mSensorId, clearSchedulerBuffer);
    }

    public void prepareForAuthentication(boolean requireConfirmation, IBinder token, long operationId, int userId, IBiometricSensorReceiver sensorReceiver, String opPackageName, long requestId, int cookie, boolean allowBackgroundAuthentication) throws RemoteException {
        this.mFaceService.prepareForAuthentication(this.mSensorId, requireConfirmation, token, operationId, userId, sensorReceiver, opPackageName, requestId, cookie, allowBackgroundAuthentication);
    }

    public void startPreparedClient(int cookie) throws RemoteException {
        this.mFaceService.startPreparedClient(this.mSensorId, cookie);
    }

    public void cancelAuthenticationFromService(IBinder token, String opPackageName, long requestId) throws RemoteException {
        this.mFaceService.cancelAuthenticationFromService(this.mSensorId, token, opPackageName, requestId);
    }

    public boolean isHardwareDetected(String opPackageName) throws RemoteException {
        return this.mFaceService.isHardwareDetected(this.mSensorId, opPackageName);
    }

    public boolean hasEnrolledTemplates(int userId, String opPackageName) throws RemoteException {
        return this.mFaceService.hasEnrolledFaces(this.mSensorId, userId, opPackageName);
    }

    public void invalidateAuthenticatorId(int userId, IInvalidationCallback callback) throws RemoteException {
        this.mFaceService.invalidateAuthenticatorId(this.mSensorId, userId, callback);
    }

    public int getLockoutModeForUser(int userId) throws RemoteException {
        return this.mFaceService.getLockoutModeForUser(this.mSensorId, userId);
    }

    public long getAuthenticatorId(int callingUserId) throws RemoteException {
        return this.mFaceService.getAuthenticatorId(this.mSensorId, callingUserId);
    }

    public void resetLockout(IBinder token, String opPackageName, int userId, byte[] hardwareAuthToken) throws RemoteException {
        this.mFaceService.resetLockout(token, this.mSensorId, userId, hardwareAuthToken, opPackageName);
    }
}
