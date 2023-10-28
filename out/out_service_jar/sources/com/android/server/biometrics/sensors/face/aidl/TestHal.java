package com.android.server.biometrics.sensors.face.aidl;

import android.hardware.biometrics.common.ICancellationSignal;
import android.hardware.biometrics.common.OperationContext;
import android.hardware.biometrics.face.EnrollmentStageConfig;
import android.hardware.biometrics.face.IFace;
import android.hardware.biometrics.face.ISession;
import android.hardware.biometrics.face.ISessionCallback;
import android.hardware.biometrics.face.SensorProps;
import android.hardware.common.NativeHandle;
import android.hardware.keymaster.HardwareAuthToken;
import android.os.RemoteException;
import android.util.Slog;
/* loaded from: classes.dex */
public class TestHal extends IFace.Stub {
    private static final String TAG = "face.aidl.TestHal";

    @Override // android.hardware.biometrics.face.IFace
    public int getInterfaceVersion() {
        return 2;
    }

    @Override // android.hardware.biometrics.face.IFace
    public String getInterfaceHash() {
        return "74b0b7cb149ee205b12cd2254d216725c6e5429c";
    }

    @Override // android.hardware.biometrics.face.IFace
    public SensorProps[] getSensorProps() {
        Slog.w(TAG, "getSensorProps");
        return new SensorProps[0];
    }

    @Override // android.hardware.biometrics.face.IFace
    public ISession createSession(int sensorId, int userId, final ISessionCallback cb) {
        Slog.w(TAG, "createSession, sensorId: " + sensorId + " userId: " + userId);
        return new ISession.Stub() { // from class: com.android.server.biometrics.sensors.face.aidl.TestHal.1
            @Override // android.hardware.biometrics.face.ISession
            public int getInterfaceVersion() {
                return 2;
            }

            @Override // android.hardware.biometrics.face.ISession
            public String getInterfaceHash() {
                return "74b0b7cb149ee205b12cd2254d216725c6e5429c";
            }

            @Override // android.hardware.biometrics.face.ISession
            public void generateChallenge() throws RemoteException {
                Slog.w(TestHal.TAG, "generateChallenge");
                cb.onChallengeGenerated(0L);
            }

            @Override // android.hardware.biometrics.face.ISession
            public void revokeChallenge(long challenge) throws RemoteException {
                Slog.w(TestHal.TAG, "revokeChallenge: " + challenge);
                cb.onChallengeRevoked(challenge);
            }

            @Override // android.hardware.biometrics.face.ISession
            public EnrollmentStageConfig[] getEnrollmentConfig(byte enrollmentType) {
                return new EnrollmentStageConfig[0];
            }

            @Override // android.hardware.biometrics.face.ISession
            public ICancellationSignal enroll(HardwareAuthToken hat, byte enrollmentType, byte[] features, NativeHandle previewSurface) {
                Slog.w(TestHal.TAG, "enroll");
                return new ICancellationSignal.Stub() { // from class: com.android.server.biometrics.sensors.face.aidl.TestHal.1.1
                    @Override // android.hardware.biometrics.common.ICancellationSignal
                    public void cancel() throws RemoteException {
                        cb.onError((byte) 5, 0);
                    }

                    @Override // android.hardware.biometrics.common.ICancellationSignal
                    public int getInterfaceVersion() {
                        return 2;
                    }

                    @Override // android.hardware.biometrics.common.ICancellationSignal
                    public String getInterfaceHash() {
                        return ICancellationSignal.HASH;
                    }
                };
            }

            @Override // android.hardware.biometrics.face.ISession
            public ICancellationSignal authenticate(long operationId) {
                Slog.w(TestHal.TAG, "authenticate");
                return new ICancellationSignal.Stub() { // from class: com.android.server.biometrics.sensors.face.aidl.TestHal.1.2
                    @Override // android.hardware.biometrics.common.ICancellationSignal
                    public void cancel() throws RemoteException {
                        cb.onError((byte) 5, 0);
                    }

                    @Override // android.hardware.biometrics.common.ICancellationSignal
                    public int getInterfaceVersion() {
                        return 2;
                    }

                    @Override // android.hardware.biometrics.common.ICancellationSignal
                    public String getInterfaceHash() {
                        return ICancellationSignal.HASH;
                    }
                };
            }

            @Override // android.hardware.biometrics.face.ISession
            public ICancellationSignal detectInteraction() {
                Slog.w(TestHal.TAG, "detectInteraction");
                return new ICancellationSignal.Stub() { // from class: com.android.server.biometrics.sensors.face.aidl.TestHal.1.3
                    @Override // android.hardware.biometrics.common.ICancellationSignal
                    public void cancel() throws RemoteException {
                        cb.onError((byte) 5, 0);
                    }

                    @Override // android.hardware.biometrics.common.ICancellationSignal
                    public int getInterfaceVersion() {
                        return 2;
                    }

                    @Override // android.hardware.biometrics.common.ICancellationSignal
                    public String getInterfaceHash() {
                        return ICancellationSignal.HASH;
                    }
                };
            }

            @Override // android.hardware.biometrics.face.ISession
            public void enumerateEnrollments() throws RemoteException {
                Slog.w(TestHal.TAG, "enumerateEnrollments");
                cb.onEnrollmentsEnumerated(new int[0]);
            }

            @Override // android.hardware.biometrics.face.ISession
            public void removeEnrollments(int[] enrollmentIds) throws RemoteException {
                Slog.w(TestHal.TAG, "removeEnrollments");
                cb.onEnrollmentsRemoved(enrollmentIds);
            }

            @Override // android.hardware.biometrics.face.ISession
            public void getFeatures() throws RemoteException {
                Slog.w(TestHal.TAG, "getFeatures");
                cb.onFeaturesRetrieved(new byte[0]);
            }

            @Override // android.hardware.biometrics.face.ISession
            public void setFeature(HardwareAuthToken hat, byte feature, boolean enabled) throws RemoteException {
                Slog.w(TestHal.TAG, "setFeature");
                cb.onFeatureSet(feature);
            }

            @Override // android.hardware.biometrics.face.ISession
            public void getAuthenticatorId() throws RemoteException {
                Slog.w(TestHal.TAG, "getAuthenticatorId");
                cb.onAuthenticatorIdRetrieved(0L);
            }

            @Override // android.hardware.biometrics.face.ISession
            public void invalidateAuthenticatorId() throws RemoteException {
                Slog.w(TestHal.TAG, "invalidateAuthenticatorId");
                cb.onAuthenticatorIdInvalidated(0L);
            }

            @Override // android.hardware.biometrics.face.ISession
            public void resetLockout(HardwareAuthToken hat) throws RemoteException {
                Slog.w(TestHal.TAG, "resetLockout");
                cb.onLockoutCleared();
            }

            @Override // android.hardware.biometrics.face.ISession
            public void close() throws RemoteException {
                Slog.w(TestHal.TAG, "close");
                cb.onSessionClosed();
            }

            @Override // android.hardware.biometrics.face.ISession
            public ICancellationSignal authenticateWithContext(long operationId, OperationContext context) {
                return authenticate(operationId);
            }

            @Override // android.hardware.biometrics.face.ISession
            public ICancellationSignal enrollWithContext(HardwareAuthToken hat, byte enrollmentType, byte[] features, NativeHandle previewSurface, OperationContext context) {
                return enroll(hat, enrollmentType, features, previewSurface);
            }

            @Override // android.hardware.biometrics.face.ISession
            public ICancellationSignal detectInteractionWithContext(OperationContext context) {
                return detectInteraction();
            }

            @Override // android.hardware.biometrics.face.ISession
            public void onContextChanged(OperationContext context) {
                Slog.w(TestHal.TAG, "onContextChanged");
            }
        };
    }
}
