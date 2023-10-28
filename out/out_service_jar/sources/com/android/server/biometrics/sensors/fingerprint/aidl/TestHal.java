package com.android.server.biometrics.sensors.fingerprint.aidl;

import android.hardware.biometrics.common.ICancellationSignal;
import android.hardware.biometrics.common.OperationContext;
import android.hardware.biometrics.fingerprint.IFingerprint;
import android.hardware.biometrics.fingerprint.ISession;
import android.hardware.biometrics.fingerprint.ISessionCallback;
import android.hardware.biometrics.fingerprint.PointerContext;
import android.hardware.biometrics.fingerprint.SensorProps;
import android.hardware.keymaster.HardwareAuthToken;
import android.os.RemoteException;
import android.util.Slog;
/* loaded from: classes.dex */
public class TestHal extends IFingerprint.Stub {
    private static final String TAG = "fingerprint.aidl.TestHal";

    @Override // android.hardware.biometrics.fingerprint.IFingerprint
    public int getInterfaceVersion() {
        return 2;
    }

    @Override // android.hardware.biometrics.fingerprint.IFingerprint
    public String getInterfaceHash() {
        return "c2f3b863b6dff925bc4451473ee6caa1aa304b8f";
    }

    @Override // android.hardware.biometrics.fingerprint.IFingerprint
    public SensorProps[] getSensorProps() {
        Slog.w(TAG, "getSensorProps");
        return new SensorProps[0];
    }

    @Override // android.hardware.biometrics.fingerprint.IFingerprint
    public ISession createSession(int sensorId, int userId, final ISessionCallback cb) {
        Slog.w(TAG, "createSession, sensorId: " + sensorId + " userId: " + userId);
        return new ISession.Stub() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.TestHal.1
            @Override // android.hardware.biometrics.fingerprint.ISession
            public int getInterfaceVersion() {
                return 2;
            }

            @Override // android.hardware.biometrics.fingerprint.ISession
            public String getInterfaceHash() {
                return "c2f3b863b6dff925bc4451473ee6caa1aa304b8f";
            }

            @Override // android.hardware.biometrics.fingerprint.ISession
            public void generateChallenge() throws RemoteException {
                Slog.w(TestHal.TAG, "generateChallenge");
                cb.onChallengeGenerated(0L);
            }

            @Override // android.hardware.biometrics.fingerprint.ISession
            public void revokeChallenge(long challenge) throws RemoteException {
                Slog.w(TestHal.TAG, "revokeChallenge: " + challenge);
                cb.onChallengeRevoked(challenge);
            }

            @Override // android.hardware.biometrics.fingerprint.ISession
            public ICancellationSignal enroll(HardwareAuthToken hat) {
                Slog.w(TestHal.TAG, "enroll");
                return new ICancellationSignal.Stub() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.TestHal.1.1
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

            @Override // android.hardware.biometrics.fingerprint.ISession
            public ICancellationSignal authenticate(long operationId) {
                Slog.w(TestHal.TAG, "authenticate");
                return new ICancellationSignal.Stub() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.TestHal.1.2
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

            @Override // android.hardware.biometrics.fingerprint.ISession
            public ICancellationSignal detectInteraction() {
                Slog.w(TestHal.TAG, "detectInteraction");
                return new ICancellationSignal.Stub() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.TestHal.1.3
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

            @Override // android.hardware.biometrics.fingerprint.ISession
            public void enumerateEnrollments() throws RemoteException {
                Slog.w(TestHal.TAG, "enumerateEnrollments");
                cb.onEnrollmentsEnumerated(new int[0]);
            }

            @Override // android.hardware.biometrics.fingerprint.ISession
            public void removeEnrollments(int[] enrollmentIds) throws RemoteException {
                Slog.w(TestHal.TAG, "removeEnrollments");
                cb.onEnrollmentsRemoved(enrollmentIds);
            }

            @Override // android.hardware.biometrics.fingerprint.ISession
            public void getAuthenticatorId() throws RemoteException {
                Slog.w(TestHal.TAG, "getAuthenticatorId");
                cb.onAuthenticatorIdRetrieved(0L);
            }

            @Override // android.hardware.biometrics.fingerprint.ISession
            public void invalidateAuthenticatorId() throws RemoteException {
                Slog.w(TestHal.TAG, "invalidateAuthenticatorId");
                cb.onAuthenticatorIdInvalidated(0L);
            }

            @Override // android.hardware.biometrics.fingerprint.ISession
            public void resetLockout(HardwareAuthToken hat) throws RemoteException {
                Slog.w(TestHal.TAG, "resetLockout");
                cb.onLockoutCleared();
            }

            @Override // android.hardware.biometrics.fingerprint.ISession
            public void close() throws RemoteException {
                Slog.w(TestHal.TAG, "close");
                cb.onSessionClosed();
            }

            @Override // android.hardware.biometrics.fingerprint.ISession
            public void onPointerDown(int pointerId, int x, int y, float minor, float major) {
                Slog.w(TestHal.TAG, "onPointerDown");
            }

            @Override // android.hardware.biometrics.fingerprint.ISession
            public void onPointerUp(int pointerId) {
                Slog.w(TestHal.TAG, "onPointerUp");
            }

            @Override // android.hardware.biometrics.fingerprint.ISession
            public void onUiReady() {
                Slog.w(TestHal.TAG, "onUiReady");
            }

            @Override // android.hardware.biometrics.fingerprint.ISession
            public ICancellationSignal authenticateWithContext(long operationId, OperationContext context) {
                return authenticate(operationId);
            }

            @Override // android.hardware.biometrics.fingerprint.ISession
            public ICancellationSignal enrollWithContext(HardwareAuthToken hat, OperationContext context) {
                return enroll(hat);
            }

            @Override // android.hardware.biometrics.fingerprint.ISession
            public ICancellationSignal detectInteractionWithContext(OperationContext context) {
                return detectInteraction();
            }

            @Override // android.hardware.biometrics.fingerprint.ISession
            public void onPointerDownWithContext(PointerContext context) {
                onPointerDown(context.pointerId, (int) context.x, (int) context.y, context.minor, context.major);
            }

            @Override // android.hardware.biometrics.fingerprint.ISession
            public void onPointerUpWithContext(PointerContext context) {
                onPointerUp(context.pointerId);
            }

            @Override // android.hardware.biometrics.fingerprint.ISession
            public void onContextChanged(OperationContext context) {
                Slog.w(TestHal.TAG, "onContextChanged");
            }
        };
    }
}
