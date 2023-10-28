package android.hardware.biometrics.face;

import android.hardware.keymaster.HardwareAuthToken;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface ISessionCallback extends IInterface {
    public static final String DESCRIPTOR = "android$hardware$biometrics$face$ISessionCallback".replace('$', '.');
    public static final String HASH = "74b0b7cb149ee205b12cd2254d216725c6e5429c";
    public static final int VERSION = 2;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void onAuthenticationFailed() throws RemoteException;

    void onAuthenticationFrame(AuthenticationFrame authenticationFrame) throws RemoteException;

    void onAuthenticationSucceeded(int i, HardwareAuthToken hardwareAuthToken) throws RemoteException;

    void onAuthenticatorIdInvalidated(long j) throws RemoteException;

    void onAuthenticatorIdRetrieved(long j) throws RemoteException;

    void onChallengeGenerated(long j) throws RemoteException;

    void onChallengeRevoked(long j) throws RemoteException;

    void onEnrollmentFrame(EnrollmentFrame enrollmentFrame) throws RemoteException;

    void onEnrollmentProgress(int i, int i2) throws RemoteException;

    void onEnrollmentsEnumerated(int[] iArr) throws RemoteException;

    void onEnrollmentsRemoved(int[] iArr) throws RemoteException;

    void onError(byte b, int i) throws RemoteException;

    void onFeatureSet(byte b) throws RemoteException;

    void onFeaturesRetrieved(byte[] bArr) throws RemoteException;

    void onInteractionDetected() throws RemoteException;

    void onLockoutCleared() throws RemoteException;

    void onLockoutPermanent() throws RemoteException;

    void onLockoutTimed(long j) throws RemoteException;

    void onSessionClosed() throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements ISessionCallback {
        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onChallengeGenerated(long challenge) throws RemoteException {
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onChallengeRevoked(long challenge) throws RemoteException {
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onAuthenticationFrame(AuthenticationFrame frame) throws RemoteException {
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onEnrollmentFrame(EnrollmentFrame frame) throws RemoteException {
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onError(byte error, int vendorCode) throws RemoteException {
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onEnrollmentProgress(int enrollmentId, int remaining) throws RemoteException {
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onAuthenticationSucceeded(int enrollmentId, HardwareAuthToken hat) throws RemoteException {
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onAuthenticationFailed() throws RemoteException {
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onLockoutTimed(long durationMillis) throws RemoteException {
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onLockoutPermanent() throws RemoteException {
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onLockoutCleared() throws RemoteException {
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onInteractionDetected() throws RemoteException {
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onEnrollmentsEnumerated(int[] enrollmentIds) throws RemoteException {
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onFeaturesRetrieved(byte[] features) throws RemoteException {
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onFeatureSet(byte feature) throws RemoteException {
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onEnrollmentsRemoved(int[] enrollmentIds) throws RemoteException {
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onAuthenticatorIdRetrieved(long authenticatorId) throws RemoteException {
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onAuthenticatorIdInvalidated(long newAuthenticatorId) throws RemoteException {
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onSessionClosed() throws RemoteException {
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements ISessionCallback {
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_onAuthenticationFailed = 8;
        static final int TRANSACTION_onAuthenticationFrame = 3;
        static final int TRANSACTION_onAuthenticationSucceeded = 7;
        static final int TRANSACTION_onAuthenticatorIdInvalidated = 18;
        static final int TRANSACTION_onAuthenticatorIdRetrieved = 17;
        static final int TRANSACTION_onChallengeGenerated = 1;
        static final int TRANSACTION_onChallengeRevoked = 2;
        static final int TRANSACTION_onEnrollmentFrame = 4;
        static final int TRANSACTION_onEnrollmentProgress = 6;
        static final int TRANSACTION_onEnrollmentsEnumerated = 13;
        static final int TRANSACTION_onEnrollmentsRemoved = 16;
        static final int TRANSACTION_onError = 5;
        static final int TRANSACTION_onFeatureSet = 15;
        static final int TRANSACTION_onFeaturesRetrieved = 14;
        static final int TRANSACTION_onInteractionDetected = 12;
        static final int TRANSACTION_onLockoutCleared = 11;
        static final int TRANSACTION_onLockoutPermanent = 10;
        static final int TRANSACTION_onLockoutTimed = 9;
        static final int TRANSACTION_onSessionClosed = 19;

        public Stub() {
            markVintfStability();
            attachInterface(this, DESCRIPTOR);
        }

        public static ISessionCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ISessionCallback)) {
                return (ISessionCallback) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(descriptor);
            }
            switch (code) {
                case TRANSACTION_getInterfaceHash /* 16777214 */:
                    reply.writeNoException();
                    reply.writeString(getInterfaceHash());
                    return true;
                case 16777215:
                    reply.writeNoException();
                    reply.writeInt(getInterfaceVersion());
                    return true;
                case 1598968902:
                    reply.writeString(descriptor);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            long _arg0 = data.readLong();
                            data.enforceNoDataAvail();
                            onChallengeGenerated(_arg0);
                            reply.writeNoException();
                            break;
                        case 2:
                            long _arg02 = data.readLong();
                            data.enforceNoDataAvail();
                            onChallengeRevoked(_arg02);
                            reply.writeNoException();
                            break;
                        case 3:
                            AuthenticationFrame _arg03 = (AuthenticationFrame) data.readTypedObject(AuthenticationFrame.CREATOR);
                            data.enforceNoDataAvail();
                            onAuthenticationFrame(_arg03);
                            reply.writeNoException();
                            break;
                        case 4:
                            EnrollmentFrame _arg04 = (EnrollmentFrame) data.readTypedObject(EnrollmentFrame.CREATOR);
                            data.enforceNoDataAvail();
                            onEnrollmentFrame(_arg04);
                            reply.writeNoException();
                            break;
                        case 5:
                            byte _arg05 = data.readByte();
                            int _arg1 = data.readInt();
                            data.enforceNoDataAvail();
                            onError(_arg05, _arg1);
                            reply.writeNoException();
                            break;
                        case 6:
                            int _arg06 = data.readInt();
                            int _arg12 = data.readInt();
                            data.enforceNoDataAvail();
                            onEnrollmentProgress(_arg06, _arg12);
                            reply.writeNoException();
                            break;
                        case 7:
                            int _arg07 = data.readInt();
                            HardwareAuthToken _arg13 = (HardwareAuthToken) data.readTypedObject(HardwareAuthToken.CREATOR);
                            data.enforceNoDataAvail();
                            onAuthenticationSucceeded(_arg07, _arg13);
                            reply.writeNoException();
                            break;
                        case 8:
                            onAuthenticationFailed();
                            reply.writeNoException();
                            break;
                        case 9:
                            long _arg08 = data.readLong();
                            data.enforceNoDataAvail();
                            onLockoutTimed(_arg08);
                            reply.writeNoException();
                            break;
                        case 10:
                            onLockoutPermanent();
                            reply.writeNoException();
                            break;
                        case 11:
                            onLockoutCleared();
                            reply.writeNoException();
                            break;
                        case 12:
                            onInteractionDetected();
                            reply.writeNoException();
                            break;
                        case 13:
                            int[] _arg09 = data.createIntArray();
                            data.enforceNoDataAvail();
                            onEnrollmentsEnumerated(_arg09);
                            reply.writeNoException();
                            break;
                        case 14:
                            byte[] _arg010 = data.createByteArray();
                            data.enforceNoDataAvail();
                            onFeaturesRetrieved(_arg010);
                            reply.writeNoException();
                            break;
                        case 15:
                            byte _arg011 = data.readByte();
                            data.enforceNoDataAvail();
                            onFeatureSet(_arg011);
                            reply.writeNoException();
                            break;
                        case 16:
                            int[] _arg012 = data.createIntArray();
                            data.enforceNoDataAvail();
                            onEnrollmentsRemoved(_arg012);
                            reply.writeNoException();
                            break;
                        case 17:
                            long _arg013 = data.readLong();
                            data.enforceNoDataAvail();
                            onAuthenticatorIdRetrieved(_arg013);
                            reply.writeNoException();
                            break;
                        case 18:
                            long _arg014 = data.readLong();
                            data.enforceNoDataAvail();
                            onAuthenticatorIdInvalidated(_arg014);
                            reply.writeNoException();
                            break;
                        case 19:
                            onSessionClosed();
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements ISessionCallback {
            private IBinder mRemote;
            private int mCachedVersion = -1;
            private String mCachedHash = "-1";

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            @Override // android.hardware.biometrics.face.ISessionCallback
            public void onChallengeGenerated(long challenge) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeLong(challenge);
                    boolean _status = this.mRemote.transact(1, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method onChallengeGenerated is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.face.ISessionCallback
            public void onChallengeRevoked(long challenge) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeLong(challenge);
                    boolean _status = this.mRemote.transact(2, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method onChallengeRevoked is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.face.ISessionCallback
            public void onAuthenticationFrame(AuthenticationFrame frame) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(frame, 0);
                    boolean _status = this.mRemote.transact(3, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method onAuthenticationFrame is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.face.ISessionCallback
            public void onEnrollmentFrame(EnrollmentFrame frame) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(frame, 0);
                    boolean _status = this.mRemote.transact(4, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method onEnrollmentFrame is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.face.ISessionCallback
            public void onError(byte error, int vendorCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeByte(error);
                    _data.writeInt(vendorCode);
                    boolean _status = this.mRemote.transact(5, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method onError is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.face.ISessionCallback
            public void onEnrollmentProgress(int enrollmentId, int remaining) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(enrollmentId);
                    _data.writeInt(remaining);
                    boolean _status = this.mRemote.transact(6, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method onEnrollmentProgress is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.face.ISessionCallback
            public void onAuthenticationSucceeded(int enrollmentId, HardwareAuthToken hat) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(enrollmentId);
                    _data.writeTypedObject(hat, 0);
                    boolean _status = this.mRemote.transact(7, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method onAuthenticationSucceeded is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.face.ISessionCallback
            public void onAuthenticationFailed() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(8, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method onAuthenticationFailed is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.face.ISessionCallback
            public void onLockoutTimed(long durationMillis) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeLong(durationMillis);
                    boolean _status = this.mRemote.transact(9, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method onLockoutTimed is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.face.ISessionCallback
            public void onLockoutPermanent() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(10, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method onLockoutPermanent is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.face.ISessionCallback
            public void onLockoutCleared() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(11, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method onLockoutCleared is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.face.ISessionCallback
            public void onInteractionDetected() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(12, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method onInteractionDetected is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.face.ISessionCallback
            public void onEnrollmentsEnumerated(int[] enrollmentIds) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeIntArray(enrollmentIds);
                    boolean _status = this.mRemote.transact(13, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method onEnrollmentsEnumerated is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.face.ISessionCallback
            public void onFeaturesRetrieved(byte[] features) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeByteArray(features);
                    boolean _status = this.mRemote.transact(14, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method onFeaturesRetrieved is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.face.ISessionCallback
            public void onFeatureSet(byte feature) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeByte(feature);
                    boolean _status = this.mRemote.transact(15, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method onFeatureSet is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.face.ISessionCallback
            public void onEnrollmentsRemoved(int[] enrollmentIds) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeIntArray(enrollmentIds);
                    boolean _status = this.mRemote.transact(16, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method onEnrollmentsRemoved is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.face.ISessionCallback
            public void onAuthenticatorIdRetrieved(long authenticatorId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeLong(authenticatorId);
                    boolean _status = this.mRemote.transact(17, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method onAuthenticatorIdRetrieved is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.face.ISessionCallback
            public void onAuthenticatorIdInvalidated(long newAuthenticatorId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeLong(newAuthenticatorId);
                    boolean _status = this.mRemote.transact(18, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method onAuthenticatorIdInvalidated is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.face.ISessionCallback
            public void onSessionClosed() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(19, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method onSessionClosed is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.face.ISessionCallback
            public int getInterfaceVersion() throws RemoteException {
                if (this.mCachedVersion == -1) {
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    try {
                        data.writeInterfaceToken(DESCRIPTOR);
                        this.mRemote.transact(16777215, data, reply, 0);
                        reply.readException();
                        this.mCachedVersion = reply.readInt();
                    } finally {
                        reply.recycle();
                        data.recycle();
                    }
                }
                return this.mCachedVersion;
            }

            @Override // android.hardware.biometrics.face.ISessionCallback
            public synchronized String getInterfaceHash() throws RemoteException {
                if ("-1".equals(this.mCachedHash)) {
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    data.writeInterfaceToken(DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getInterfaceHash, data, reply, 0);
                    reply.readException();
                    this.mCachedHash = reply.readString();
                    reply.recycle();
                    data.recycle();
                }
                return this.mCachedHash;
            }
        }
    }
}
