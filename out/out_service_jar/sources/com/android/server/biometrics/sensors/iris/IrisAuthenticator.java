package com.android.server.biometrics.sensors.iris;

import android.hardware.biometrics.IBiometricAuthenticator;
import android.hardware.biometrics.IBiometricSensorReceiver;
import android.hardware.biometrics.IInvalidationCallback;
import android.hardware.biometrics.ITestSession;
import android.hardware.biometrics.ITestSessionCallback;
import android.hardware.biometrics.SensorPropertiesInternal;
import android.hardware.iris.IIrisService;
import android.os.IBinder;
import android.os.RemoteException;
/* loaded from: classes.dex */
public final class IrisAuthenticator extends IBiometricAuthenticator.Stub {
    private final IIrisService mIrisService;

    public IrisAuthenticator(IIrisService irisService, int sensorId) {
        this.mIrisService = irisService;
    }

    public ITestSession createTestSession(ITestSessionCallback callback, String opPackageName) throws RemoteException {
        return null;
    }

    public SensorPropertiesInternal getSensorProperties(String opPackageName) throws RemoteException {
        return null;
    }

    public byte[] dumpSensorServiceStateProto(boolean clearSchedulerBuffer) throws RemoteException {
        return null;
    }

    public void prepareForAuthentication(boolean requireConfirmation, IBinder token, long sessionId, int userId, IBiometricSensorReceiver sensorReceiver, String opPackageName, long requestId, int cookie, boolean allowBackgroundAuthentication) throws RemoteException {
    }

    public void startPreparedClient(int cookie) throws RemoteException {
    }

    public void cancelAuthenticationFromService(IBinder token, String opPackageName, long requestId) throws RemoteException {
    }

    public boolean isHardwareDetected(String opPackageName) throws RemoteException {
        return false;
    }

    public boolean hasEnrolledTemplates(int userId, String opPackageName) throws RemoteException {
        return false;
    }

    public int getLockoutModeForUser(int userId) throws RemoteException {
        return 0;
    }

    public void invalidateAuthenticatorId(int userId, IInvalidationCallback callback) {
    }

    public long getAuthenticatorId(int callingUserId) throws RemoteException {
        return 0L;
    }

    public void resetLockout(IBinder token, String opPackageName, int userId, byte[] hardwareAuthToken) throws RemoteException {
    }
}
