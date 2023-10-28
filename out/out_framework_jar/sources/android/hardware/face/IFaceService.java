package android.hardware.face;

import android.hardware.biometrics.IBiometricSensorReceiver;
import android.hardware.biometrics.IBiometricServiceLockoutResetCallback;
import android.hardware.biometrics.IInvalidationCallback;
import android.hardware.biometrics.ITestSession;
import android.hardware.biometrics.ITestSessionCallback;
import android.hardware.face.IFaceServiceReceiver;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.Surface;
import java.util.List;
/* loaded from: classes.dex */
public interface IFaceService extends IInterface {
    public static final String DESCRIPTOR = "android.hardware.face.IFaceService";

    void addLockoutResetCallback(IBiometricServiceLockoutResetCallback iBiometricServiceLockoutResetCallback, String str) throws RemoteException;

    long authenticate(IBinder iBinder, long j, int i, IFaceServiceReceiver iFaceServiceReceiver, String str, boolean z) throws RemoteException;

    void cancelAuthentication(IBinder iBinder, String str, long j) throws RemoteException;

    void cancelAuthenticationFromService(int i, IBinder iBinder, String str, long j) throws RemoteException;

    void cancelEnrollment(IBinder iBinder, long j) throws RemoteException;

    void cancelFaceDetect(IBinder iBinder, String str, long j) throws RemoteException;

    ITestSession createTestSession(int i, ITestSessionCallback iTestSessionCallback, String str) throws RemoteException;

    long detectFace(IBinder iBinder, int i, IFaceServiceReceiver iFaceServiceReceiver, String str) throws RemoteException;

    byte[] dumpSensorServiceStateProto(int i, boolean z) throws RemoteException;

    long enroll(int i, IBinder iBinder, byte[] bArr, IFaceServiceReceiver iFaceServiceReceiver, String str, int[] iArr, Surface surface, boolean z) throws RemoteException;

    long enrollRemotely(int i, IBinder iBinder, byte[] bArr, IFaceServiceReceiver iFaceServiceReceiver, String str, int[] iArr) throws RemoteException;

    void generateChallenge(IBinder iBinder, int i, int i2, IFaceServiceReceiver iFaceServiceReceiver, String str) throws RemoteException;

    long getAuthenticatorId(int i, int i2) throws RemoteException;

    List<Face> getEnrolledFaces(int i, int i2, String str) throws RemoteException;

    void getFeature(IBinder iBinder, int i, int i2, IFaceServiceReceiver iFaceServiceReceiver, String str) throws RemoteException;

    int getLockoutModeForUser(int i, int i2) throws RemoteException;

    FaceSensorPropertiesInternal getSensorProperties(int i, String str) throws RemoteException;

    List<FaceSensorPropertiesInternal> getSensorPropertiesInternal(String str) throws RemoteException;

    boolean hasEnrolledFaces(int i, int i2, String str) throws RemoteException;

    void invalidateAuthenticatorId(int i, int i2, IInvalidationCallback iInvalidationCallback) throws RemoteException;

    boolean isHardwareDetected(int i, String str) throws RemoteException;

    void prepareForAuthentication(int i, boolean z, IBinder iBinder, long j, int i2, IBiometricSensorReceiver iBiometricSensorReceiver, String str, long j2, int i3, boolean z2) throws RemoteException;

    void registerAuthenticators(List<FaceSensorPropertiesInternal> list) throws RemoteException;

    void remove(IBinder iBinder, int i, int i2, IFaceServiceReceiver iFaceServiceReceiver, String str) throws RemoteException;

    void removeAll(IBinder iBinder, int i, IFaceServiceReceiver iFaceServiceReceiver, String str) throws RemoteException;

    void resetLockout(IBinder iBinder, int i, int i2, byte[] bArr, String str) throws RemoteException;

    void revokeChallenge(IBinder iBinder, int i, int i2, String str, long j) throws RemoteException;

    void setFeature(IBinder iBinder, int i, int i2, boolean z, byte[] bArr, IFaceServiceReceiver iFaceServiceReceiver, String str) throws RemoteException;

    void startPreparedClient(int i, int i2) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IFaceService {
        @Override // android.hardware.face.IFaceService
        public ITestSession createTestSession(int sensorId, ITestSessionCallback callback, String opPackageName) throws RemoteException {
            return null;
        }

        @Override // android.hardware.face.IFaceService
        public byte[] dumpSensorServiceStateProto(int sensorId, boolean clearSchedulerBuffer) throws RemoteException {
            return null;
        }

        @Override // android.hardware.face.IFaceService
        public List<FaceSensorPropertiesInternal> getSensorPropertiesInternal(String opPackageName) throws RemoteException {
            return null;
        }

        @Override // android.hardware.face.IFaceService
        public FaceSensorPropertiesInternal getSensorProperties(int sensorId, String opPackageName) throws RemoteException {
            return null;
        }

        @Override // android.hardware.face.IFaceService
        public long authenticate(IBinder token, long operationId, int userId, IFaceServiceReceiver receiver, String opPackageName, boolean isKeyguardBypassEnabled) throws RemoteException {
            return 0L;
        }

        @Override // android.hardware.face.IFaceService
        public long detectFace(IBinder token, int userId, IFaceServiceReceiver receiver, String opPackageName) throws RemoteException {
            return 0L;
        }

        @Override // android.hardware.face.IFaceService
        public void prepareForAuthentication(int sensorId, boolean requireConfirmation, IBinder token, long operationId, int userId, IBiometricSensorReceiver sensorReceiver, String opPackageName, long requestId, int cookie, boolean allowBackgroundAuthentication) throws RemoteException {
        }

        @Override // android.hardware.face.IFaceService
        public void startPreparedClient(int sensorId, int cookie) throws RemoteException {
        }

        @Override // android.hardware.face.IFaceService
        public void cancelAuthentication(IBinder token, String opPackageName, long requestId) throws RemoteException {
        }

        @Override // android.hardware.face.IFaceService
        public void cancelFaceDetect(IBinder token, String opPackageName, long requestId) throws RemoteException {
        }

        @Override // android.hardware.face.IFaceService
        public void cancelAuthenticationFromService(int sensorId, IBinder token, String opPackageName, long requestId) throws RemoteException {
        }

        @Override // android.hardware.face.IFaceService
        public long enroll(int userId, IBinder token, byte[] hardwareAuthToken, IFaceServiceReceiver receiver, String opPackageName, int[] disabledFeatures, Surface previewSurface, boolean debugConsent) throws RemoteException {
            return 0L;
        }

        @Override // android.hardware.face.IFaceService
        public long enrollRemotely(int userId, IBinder token, byte[] hardwareAuthToken, IFaceServiceReceiver receiver, String opPackageName, int[] disabledFeatures) throws RemoteException {
            return 0L;
        }

        @Override // android.hardware.face.IFaceService
        public void cancelEnrollment(IBinder token, long requestId) throws RemoteException {
        }

        @Override // android.hardware.face.IFaceService
        public void remove(IBinder token, int faceId, int userId, IFaceServiceReceiver receiver, String opPackageName) throws RemoteException {
        }

        @Override // android.hardware.face.IFaceService
        public void removeAll(IBinder token, int userId, IFaceServiceReceiver receiver, String opPackageName) throws RemoteException {
        }

        @Override // android.hardware.face.IFaceService
        public List<Face> getEnrolledFaces(int sensorId, int userId, String opPackageName) throws RemoteException {
            return null;
        }

        @Override // android.hardware.face.IFaceService
        public boolean isHardwareDetected(int sensorId, String opPackageName) throws RemoteException {
            return false;
        }

        @Override // android.hardware.face.IFaceService
        public void generateChallenge(IBinder token, int sensorId, int userId, IFaceServiceReceiver receiver, String opPackageName) throws RemoteException {
        }

        @Override // android.hardware.face.IFaceService
        public void revokeChallenge(IBinder token, int sensorId, int userId, String opPackageName, long challenge) throws RemoteException {
        }

        @Override // android.hardware.face.IFaceService
        public boolean hasEnrolledFaces(int sensorId, int userId, String opPackageName) throws RemoteException {
            return false;
        }

        @Override // android.hardware.face.IFaceService
        public int getLockoutModeForUser(int sensorId, int userId) throws RemoteException {
            return 0;
        }

        @Override // android.hardware.face.IFaceService
        public void invalidateAuthenticatorId(int sensorId, int userId, IInvalidationCallback callback) throws RemoteException {
        }

        @Override // android.hardware.face.IFaceService
        public long getAuthenticatorId(int sensorId, int callingUserId) throws RemoteException {
            return 0L;
        }

        @Override // android.hardware.face.IFaceService
        public void resetLockout(IBinder token, int sensorId, int userId, byte[] hardwareAuthToken, String opPackageName) throws RemoteException {
        }

        @Override // android.hardware.face.IFaceService
        public void addLockoutResetCallback(IBiometricServiceLockoutResetCallback callback, String opPackageName) throws RemoteException {
        }

        @Override // android.hardware.face.IFaceService
        public void setFeature(IBinder token, int userId, int feature, boolean enabled, byte[] hardwareAuthToken, IFaceServiceReceiver receiver, String opPackageName) throws RemoteException {
        }

        @Override // android.hardware.face.IFaceService
        public void getFeature(IBinder token, int userId, int feature, IFaceServiceReceiver receiver, String opPackageName) throws RemoteException {
        }

        @Override // android.hardware.face.IFaceService
        public void registerAuthenticators(List<FaceSensorPropertiesInternal> hidlSensors) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IFaceService {
        static final int TRANSACTION_addLockoutResetCallback = 26;
        static final int TRANSACTION_authenticate = 5;
        static final int TRANSACTION_cancelAuthentication = 9;
        static final int TRANSACTION_cancelAuthenticationFromService = 11;
        static final int TRANSACTION_cancelEnrollment = 14;
        static final int TRANSACTION_cancelFaceDetect = 10;
        static final int TRANSACTION_createTestSession = 1;
        static final int TRANSACTION_detectFace = 6;
        static final int TRANSACTION_dumpSensorServiceStateProto = 2;
        static final int TRANSACTION_enroll = 12;
        static final int TRANSACTION_enrollRemotely = 13;
        static final int TRANSACTION_generateChallenge = 19;
        static final int TRANSACTION_getAuthenticatorId = 24;
        static final int TRANSACTION_getEnrolledFaces = 17;
        static final int TRANSACTION_getFeature = 28;
        static final int TRANSACTION_getLockoutModeForUser = 22;
        static final int TRANSACTION_getSensorProperties = 4;
        static final int TRANSACTION_getSensorPropertiesInternal = 3;
        static final int TRANSACTION_hasEnrolledFaces = 21;
        static final int TRANSACTION_invalidateAuthenticatorId = 23;
        static final int TRANSACTION_isHardwareDetected = 18;
        static final int TRANSACTION_prepareForAuthentication = 7;
        static final int TRANSACTION_registerAuthenticators = 29;
        static final int TRANSACTION_remove = 15;
        static final int TRANSACTION_removeAll = 16;
        static final int TRANSACTION_resetLockout = 25;
        static final int TRANSACTION_revokeChallenge = 20;
        static final int TRANSACTION_setFeature = 27;
        static final int TRANSACTION_startPreparedClient = 8;

        public Stub() {
            attachInterface(this, IFaceService.DESCRIPTOR);
        }

        public static IFaceService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IFaceService.DESCRIPTOR);
            if (iin != null && (iin instanceof IFaceService)) {
                return (IFaceService) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public static String getDefaultTransactionName(int transactionCode) {
            switch (transactionCode) {
                case 1:
                    return "createTestSession";
                case 2:
                    return "dumpSensorServiceStateProto";
                case 3:
                    return "getSensorPropertiesInternal";
                case 4:
                    return "getSensorProperties";
                case 5:
                    return "authenticate";
                case 6:
                    return "detectFace";
                case 7:
                    return "prepareForAuthentication";
                case 8:
                    return "startPreparedClient";
                case 9:
                    return "cancelAuthentication";
                case 10:
                    return "cancelFaceDetect";
                case 11:
                    return "cancelAuthenticationFromService";
                case 12:
                    return "enroll";
                case 13:
                    return "enrollRemotely";
                case 14:
                    return "cancelEnrollment";
                case 15:
                    return "remove";
                case 16:
                    return "removeAll";
                case 17:
                    return "getEnrolledFaces";
                case 18:
                    return "isHardwareDetected";
                case 19:
                    return "generateChallenge";
                case 20:
                    return "revokeChallenge";
                case 21:
                    return "hasEnrolledFaces";
                case 22:
                    return "getLockoutModeForUser";
                case 23:
                    return "invalidateAuthenticatorId";
                case 24:
                    return "getAuthenticatorId";
                case 25:
                    return "resetLockout";
                case 26:
                    return "addLockoutResetCallback";
                case 27:
                    return "setFeature";
                case 28:
                    return "getFeature";
                case 29:
                    return "registerAuthenticators";
                default:
                    return null;
            }
        }

        @Override // android.os.Binder
        public String getTransactionName(int transactionCode) {
            return getDefaultTransactionName(transactionCode);
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(IFaceService.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IFaceService.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            ITestSessionCallback _arg1 = ITestSessionCallback.Stub.asInterface(data.readStrongBinder());
                            String _arg2 = data.readString();
                            data.enforceNoDataAvail();
                            ITestSession _result = createTestSession(_arg0, _arg1, _arg2);
                            reply.writeNoException();
                            reply.writeStrongInterface(_result);
                            return true;
                        case 2:
                            int _arg02 = data.readInt();
                            boolean _arg12 = data.readBoolean();
                            data.enforceNoDataAvail();
                            byte[] _result2 = dumpSensorServiceStateProto(_arg02, _arg12);
                            reply.writeNoException();
                            reply.writeByteArray(_result2);
                            return true;
                        case 3:
                            String _arg03 = data.readString();
                            data.enforceNoDataAvail();
                            List<FaceSensorPropertiesInternal> _result3 = getSensorPropertiesInternal(_arg03);
                            reply.writeNoException();
                            reply.writeTypedList(_result3);
                            return true;
                        case 4:
                            int _arg04 = data.readInt();
                            String _arg13 = data.readString();
                            data.enforceNoDataAvail();
                            FaceSensorPropertiesInternal _result4 = getSensorProperties(_arg04, _arg13);
                            reply.writeNoException();
                            reply.writeTypedObject(_result4, 1);
                            return true;
                        case 5:
                            IBinder _arg05 = data.readStrongBinder();
                            long _arg14 = data.readLong();
                            int _arg22 = data.readInt();
                            IFaceServiceReceiver _arg3 = IFaceServiceReceiver.Stub.asInterface(data.readStrongBinder());
                            String _arg4 = data.readString();
                            boolean _arg5 = data.readBoolean();
                            data.enforceNoDataAvail();
                            long _result5 = authenticate(_arg05, _arg14, _arg22, _arg3, _arg4, _arg5);
                            reply.writeNoException();
                            reply.writeLong(_result5);
                            return true;
                        case 6:
                            IBinder _arg06 = data.readStrongBinder();
                            int _arg15 = data.readInt();
                            IFaceServiceReceiver _arg23 = IFaceServiceReceiver.Stub.asInterface(data.readStrongBinder());
                            String _arg32 = data.readString();
                            data.enforceNoDataAvail();
                            long _result6 = detectFace(_arg06, _arg15, _arg23, _arg32);
                            reply.writeNoException();
                            reply.writeLong(_result6);
                            return true;
                        case 7:
                            int _arg07 = data.readInt();
                            boolean _arg16 = data.readBoolean();
                            IBinder _arg24 = data.readStrongBinder();
                            long _arg33 = data.readLong();
                            int _arg42 = data.readInt();
                            IBiometricSensorReceiver _arg52 = IBiometricSensorReceiver.Stub.asInterface(data.readStrongBinder());
                            String _arg6 = data.readString();
                            long _arg7 = data.readLong();
                            int _arg8 = data.readInt();
                            boolean _arg9 = data.readBoolean();
                            data.enforceNoDataAvail();
                            prepareForAuthentication(_arg07, _arg16, _arg24, _arg33, _arg42, _arg52, _arg6, _arg7, _arg8, _arg9);
                            reply.writeNoException();
                            return true;
                        case 8:
                            int _arg08 = data.readInt();
                            int _arg17 = data.readInt();
                            data.enforceNoDataAvail();
                            startPreparedClient(_arg08, _arg17);
                            reply.writeNoException();
                            return true;
                        case 9:
                            IBinder _arg09 = data.readStrongBinder();
                            String _arg18 = data.readString();
                            long _arg25 = data.readLong();
                            data.enforceNoDataAvail();
                            cancelAuthentication(_arg09, _arg18, _arg25);
                            reply.writeNoException();
                            return true;
                        case 10:
                            IBinder _arg010 = data.readStrongBinder();
                            String _arg19 = data.readString();
                            long _arg26 = data.readLong();
                            data.enforceNoDataAvail();
                            cancelFaceDetect(_arg010, _arg19, _arg26);
                            reply.writeNoException();
                            return true;
                        case 11:
                            int _arg011 = data.readInt();
                            IBinder _arg110 = data.readStrongBinder();
                            String _arg27 = data.readString();
                            long _arg34 = data.readLong();
                            data.enforceNoDataAvail();
                            cancelAuthenticationFromService(_arg011, _arg110, _arg27, _arg34);
                            reply.writeNoException();
                            return true;
                        case 12:
                            int _arg012 = data.readInt();
                            IBinder _arg111 = data.readStrongBinder();
                            byte[] _arg28 = data.createByteArray();
                            IFaceServiceReceiver _arg35 = IFaceServiceReceiver.Stub.asInterface(data.readStrongBinder());
                            String _arg43 = data.readString();
                            int[] _arg53 = data.createIntArray();
                            Surface _arg62 = (Surface) data.readTypedObject(Surface.CREATOR);
                            boolean _arg72 = data.readBoolean();
                            data.enforceNoDataAvail();
                            long _result7 = enroll(_arg012, _arg111, _arg28, _arg35, _arg43, _arg53, _arg62, _arg72);
                            reply.writeNoException();
                            reply.writeLong(_result7);
                            return true;
                        case 13:
                            int _arg013 = data.readInt();
                            IBinder _arg112 = data.readStrongBinder();
                            byte[] _arg29 = data.createByteArray();
                            IFaceServiceReceiver _arg36 = IFaceServiceReceiver.Stub.asInterface(data.readStrongBinder());
                            String _arg44 = data.readString();
                            int[] _arg54 = data.createIntArray();
                            data.enforceNoDataAvail();
                            long _result8 = enrollRemotely(_arg013, _arg112, _arg29, _arg36, _arg44, _arg54);
                            reply.writeNoException();
                            reply.writeLong(_result8);
                            return true;
                        case 14:
                            IBinder _arg014 = data.readStrongBinder();
                            long _arg113 = data.readLong();
                            data.enforceNoDataAvail();
                            cancelEnrollment(_arg014, _arg113);
                            reply.writeNoException();
                            return true;
                        case 15:
                            IBinder _arg015 = data.readStrongBinder();
                            int _arg114 = data.readInt();
                            int _arg210 = data.readInt();
                            IFaceServiceReceiver _arg37 = IFaceServiceReceiver.Stub.asInterface(data.readStrongBinder());
                            String _arg45 = data.readString();
                            data.enforceNoDataAvail();
                            remove(_arg015, _arg114, _arg210, _arg37, _arg45);
                            reply.writeNoException();
                            return true;
                        case 16:
                            IBinder _arg016 = data.readStrongBinder();
                            int _arg115 = data.readInt();
                            IFaceServiceReceiver _arg211 = IFaceServiceReceiver.Stub.asInterface(data.readStrongBinder());
                            String _arg38 = data.readString();
                            data.enforceNoDataAvail();
                            removeAll(_arg016, _arg115, _arg211, _arg38);
                            reply.writeNoException();
                            return true;
                        case 17:
                            int _arg017 = data.readInt();
                            int _arg116 = data.readInt();
                            String _arg212 = data.readString();
                            data.enforceNoDataAvail();
                            List<Face> _result9 = getEnrolledFaces(_arg017, _arg116, _arg212);
                            reply.writeNoException();
                            reply.writeTypedList(_result9);
                            return true;
                        case 18:
                            int _arg018 = data.readInt();
                            String _arg117 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result10 = isHardwareDetected(_arg018, _arg117);
                            reply.writeNoException();
                            reply.writeBoolean(_result10);
                            return true;
                        case 19:
                            IBinder _arg019 = data.readStrongBinder();
                            int _arg118 = data.readInt();
                            int _arg213 = data.readInt();
                            IFaceServiceReceiver _arg39 = IFaceServiceReceiver.Stub.asInterface(data.readStrongBinder());
                            String _arg46 = data.readString();
                            data.enforceNoDataAvail();
                            generateChallenge(_arg019, _arg118, _arg213, _arg39, _arg46);
                            reply.writeNoException();
                            return true;
                        case 20:
                            IBinder _arg020 = data.readStrongBinder();
                            int _arg119 = data.readInt();
                            int _arg214 = data.readInt();
                            String _arg310 = data.readString();
                            long _arg47 = data.readLong();
                            data.enforceNoDataAvail();
                            revokeChallenge(_arg020, _arg119, _arg214, _arg310, _arg47);
                            reply.writeNoException();
                            return true;
                        case 21:
                            int _arg021 = data.readInt();
                            int _arg120 = data.readInt();
                            String _arg215 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result11 = hasEnrolledFaces(_arg021, _arg120, _arg215);
                            reply.writeNoException();
                            reply.writeBoolean(_result11);
                            return true;
                        case 22:
                            int _arg022 = data.readInt();
                            int _arg121 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result12 = getLockoutModeForUser(_arg022, _arg121);
                            reply.writeNoException();
                            reply.writeInt(_result12);
                            return true;
                        case 23:
                            int _arg023 = data.readInt();
                            int _arg122 = data.readInt();
                            IInvalidationCallback _arg216 = IInvalidationCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            invalidateAuthenticatorId(_arg023, _arg122, _arg216);
                            reply.writeNoException();
                            return true;
                        case 24:
                            int _arg024 = data.readInt();
                            int _arg123 = data.readInt();
                            data.enforceNoDataAvail();
                            long _result13 = getAuthenticatorId(_arg024, _arg123);
                            reply.writeNoException();
                            reply.writeLong(_result13);
                            return true;
                        case 25:
                            IBinder _arg025 = data.readStrongBinder();
                            int _arg124 = data.readInt();
                            int _arg217 = data.readInt();
                            byte[] _arg311 = data.createByteArray();
                            String _arg48 = data.readString();
                            data.enforceNoDataAvail();
                            resetLockout(_arg025, _arg124, _arg217, _arg311, _arg48);
                            reply.writeNoException();
                            return true;
                        case 26:
                            IBiometricServiceLockoutResetCallback _arg026 = IBiometricServiceLockoutResetCallback.Stub.asInterface(data.readStrongBinder());
                            String _arg125 = data.readString();
                            data.enforceNoDataAvail();
                            addLockoutResetCallback(_arg026, _arg125);
                            reply.writeNoException();
                            return true;
                        case 27:
                            IBinder _arg027 = data.readStrongBinder();
                            int _arg126 = data.readInt();
                            int _arg218 = data.readInt();
                            boolean _arg312 = data.readBoolean();
                            byte[] _arg49 = data.createByteArray();
                            IFaceServiceReceiver _arg55 = IFaceServiceReceiver.Stub.asInterface(data.readStrongBinder());
                            String _arg63 = data.readString();
                            data.enforceNoDataAvail();
                            setFeature(_arg027, _arg126, _arg218, _arg312, _arg49, _arg55, _arg63);
                            reply.writeNoException();
                            return true;
                        case 28:
                            IBinder _arg028 = data.readStrongBinder();
                            int _arg127 = data.readInt();
                            int _arg219 = data.readInt();
                            IFaceServiceReceiver _arg313 = IFaceServiceReceiver.Stub.asInterface(data.readStrongBinder());
                            String _arg410 = data.readString();
                            data.enforceNoDataAvail();
                            getFeature(_arg028, _arg127, _arg219, _arg313, _arg410);
                            reply.writeNoException();
                            return true;
                        case 29:
                            List<FaceSensorPropertiesInternal> _arg029 = data.createTypedArrayList(FaceSensorPropertiesInternal.CREATOR);
                            data.enforceNoDataAvail();
                            registerAuthenticators(_arg029);
                            reply.writeNoException();
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements IFaceService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IFaceService.DESCRIPTOR;
            }

            @Override // android.hardware.face.IFaceService
            public ITestSession createTestSession(int sensorId, ITestSessionCallback callback, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeInt(sensorId);
                    _data.writeStrongInterface(callback);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    ITestSession _result = ITestSession.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public byte[] dumpSensorServiceStateProto(int sensorId, boolean clearSchedulerBuffer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeInt(sensorId);
                    _data.writeBoolean(clearSchedulerBuffer);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public List<FaceSensorPropertiesInternal> getSensorPropertiesInternal(String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    List<FaceSensorPropertiesInternal> _result = _reply.createTypedArrayList(FaceSensorPropertiesInternal.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public FaceSensorPropertiesInternal getSensorProperties(int sensorId, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeInt(sensorId);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    FaceSensorPropertiesInternal _result = (FaceSensorPropertiesInternal) _reply.readTypedObject(FaceSensorPropertiesInternal.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public long authenticate(IBinder token, long operationId, int userId, IFaceServiceReceiver receiver, String opPackageName, boolean isKeyguardBypassEnabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeLong(operationId);
                    _data.writeInt(userId);
                    _data.writeStrongInterface(receiver);
                    _data.writeString(opPackageName);
                    _data.writeBoolean(isKeyguardBypassEnabled);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public long detectFace(IBinder token, int userId, IFaceServiceReceiver receiver, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(userId);
                    _data.writeStrongInterface(receiver);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public void prepareForAuthentication(int sensorId, boolean requireConfirmation, IBinder token, long operationId, int userId, IBiometricSensorReceiver sensorReceiver, String opPackageName, long requestId, int cookie, boolean allowBackgroundAuthentication) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeInt(sensorId);
                    _data.writeBoolean(requireConfirmation);
                    try {
                        _data.writeStrongBinder(token);
                    } catch (Throwable th) {
                        th = th;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
                try {
                    _data.writeLong(operationId);
                    try {
                        _data.writeInt(userId);
                        try {
                            _data.writeStrongInterface(sensorReceiver);
                            try {
                                _data.writeString(opPackageName);
                            } catch (Throwable th3) {
                                th = th3;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeLong(requestId);
                        try {
                            _data.writeInt(cookie);
                            try {
                                _data.writeBoolean(allowBackgroundAuthentication);
                                try {
                                    this.mRemote.transact(7, _data, _reply, 0);
                                    _reply.readException();
                                    _reply.recycle();
                                    _data.recycle();
                                } catch (Throwable th6) {
                                    th = th6;
                                    _reply.recycle();
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th7) {
                                th = th7;
                            }
                        } catch (Throwable th8) {
                            th = th8;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th9) {
                        th = th9;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th10) {
                    th = th10;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            @Override // android.hardware.face.IFaceService
            public void startPreparedClient(int sensorId, int cookie) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeInt(sensorId);
                    _data.writeInt(cookie);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public void cancelAuthentication(IBinder token, String opPackageName, long requestId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeString(opPackageName);
                    _data.writeLong(requestId);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public void cancelFaceDetect(IBinder token, String opPackageName, long requestId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeString(opPackageName);
                    _data.writeLong(requestId);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public void cancelAuthenticationFromService(int sensorId, IBinder token, String opPackageName, long requestId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeInt(sensorId);
                    _data.writeStrongBinder(token);
                    _data.writeString(opPackageName);
                    _data.writeLong(requestId);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public long enroll(int userId, IBinder token, byte[] hardwareAuthToken, IFaceServiceReceiver receiver, String opPackageName, int[] disabledFeatures, Surface previewSurface, boolean debugConsent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeStrongBinder(token);
                    _data.writeByteArray(hardwareAuthToken);
                    _data.writeStrongInterface(receiver);
                    _data.writeString(opPackageName);
                    _data.writeIntArray(disabledFeatures);
                    _data.writeTypedObject(previewSurface, 0);
                    _data.writeBoolean(debugConsent);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public long enrollRemotely(int userId, IBinder token, byte[] hardwareAuthToken, IFaceServiceReceiver receiver, String opPackageName, int[] disabledFeatures) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeStrongBinder(token);
                    _data.writeByteArray(hardwareAuthToken);
                    _data.writeStrongInterface(receiver);
                    _data.writeString(opPackageName);
                    _data.writeIntArray(disabledFeatures);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public void cancelEnrollment(IBinder token, long requestId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeLong(requestId);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public void remove(IBinder token, int faceId, int userId, IFaceServiceReceiver receiver, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(faceId);
                    _data.writeInt(userId);
                    _data.writeStrongInterface(receiver);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public void removeAll(IBinder token, int userId, IFaceServiceReceiver receiver, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(userId);
                    _data.writeStrongInterface(receiver);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public List<Face> getEnrolledFaces(int sensorId, int userId, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeInt(sensorId);
                    _data.writeInt(userId);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    List<Face> _result = _reply.createTypedArrayList(Face.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public boolean isHardwareDetected(int sensorId, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeInt(sensorId);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public void generateChallenge(IBinder token, int sensorId, int userId, IFaceServiceReceiver receiver, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(sensorId);
                    _data.writeInt(userId);
                    _data.writeStrongInterface(receiver);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public void revokeChallenge(IBinder token, int sensorId, int userId, String opPackageName, long challenge) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(sensorId);
                    _data.writeInt(userId);
                    _data.writeString(opPackageName);
                    _data.writeLong(challenge);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public boolean hasEnrolledFaces(int sensorId, int userId, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeInt(sensorId);
                    _data.writeInt(userId);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public int getLockoutModeForUser(int sensorId, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeInt(sensorId);
                    _data.writeInt(userId);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public void invalidateAuthenticatorId(int sensorId, int userId, IInvalidationCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeInt(sensorId);
                    _data.writeInt(userId);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public long getAuthenticatorId(int sensorId, int callingUserId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeInt(sensorId);
                    _data.writeInt(callingUserId);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public void resetLockout(IBinder token, int sensorId, int userId, byte[] hardwareAuthToken, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(sensorId);
                    _data.writeInt(userId);
                    _data.writeByteArray(hardwareAuthToken);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public void addLockoutResetCallback(IBiometricServiceLockoutResetCallback callback, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeStrongInterface(callback);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public void setFeature(IBinder token, int userId, int feature, boolean enabled, byte[] hardwareAuthToken, IFaceServiceReceiver receiver, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(userId);
                    _data.writeInt(feature);
                    _data.writeBoolean(enabled);
                    _data.writeByteArray(hardwareAuthToken);
                    _data.writeStrongInterface(receiver);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public void getFeature(IBinder token, int userId, int feature, IFaceServiceReceiver receiver, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(userId);
                    _data.writeInt(feature);
                    _data.writeStrongInterface(receiver);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.face.IFaceService
            public void registerAuthenticators(List<FaceSensorPropertiesInternal> hidlSensors) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFaceService.DESCRIPTOR);
                    _data.writeTypedList(hidlSensors);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 28;
        }
    }
}
