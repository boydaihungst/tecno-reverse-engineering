package android.hardware.security.keymint;

import android.hardware.security.secureclock.TimeStampToken;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes2.dex */
public interface IKeyMintOperation extends IInterface {
    public static final String DESCRIPTOR = "android$hardware$security$keymint$IKeyMintOperation".replace('$', '.');
    public static final String HASH = "207c9f218b9b9e4e74ff5232eb16511eca9d7d2e";
    public static final int VERSION = 2;

    void abort() throws RemoteException;

    byte[] finish(byte[] bArr, byte[] bArr2, HardwareAuthToken hardwareAuthToken, TimeStampToken timeStampToken, byte[] bArr3) throws RemoteException;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    byte[] update(byte[] bArr, HardwareAuthToken hardwareAuthToken, TimeStampToken timeStampToken) throws RemoteException;

    void updateAad(byte[] bArr, HardwareAuthToken hardwareAuthToken, TimeStampToken timeStampToken) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IKeyMintOperation {
        @Override // android.hardware.security.keymint.IKeyMintOperation
        public void updateAad(byte[] input, HardwareAuthToken authToken, TimeStampToken timeStampToken) throws RemoteException {
        }

        @Override // android.hardware.security.keymint.IKeyMintOperation
        public byte[] update(byte[] input, HardwareAuthToken authToken, TimeStampToken timeStampToken) throws RemoteException {
            return null;
        }

        @Override // android.hardware.security.keymint.IKeyMintOperation
        public byte[] finish(byte[] input, byte[] signature, HardwareAuthToken authToken, TimeStampToken timestampToken, byte[] confirmationToken) throws RemoteException {
            return null;
        }

        @Override // android.hardware.security.keymint.IKeyMintOperation
        public void abort() throws RemoteException {
        }

        @Override // android.hardware.security.keymint.IKeyMintOperation
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.hardware.security.keymint.IKeyMintOperation
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IKeyMintOperation {
        static final int TRANSACTION_abort = 4;
        static final int TRANSACTION_finish = 3;
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_update = 2;
        static final int TRANSACTION_updateAad = 1;

        public Stub() {
            markVintfStability();
            attachInterface(this, DESCRIPTOR);
        }

        public static IKeyMintOperation asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IKeyMintOperation)) {
                return (IKeyMintOperation) iin;
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
                case 16777214:
                    reply.writeNoException();
                    reply.writeString(getInterfaceHash());
                    return true;
                case 16777215:
                    reply.writeNoException();
                    reply.writeInt(getInterfaceVersion());
                    return true;
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(descriptor);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            byte[] _arg0 = data.createByteArray();
                            HardwareAuthToken _arg1 = (HardwareAuthToken) data.readTypedObject(HardwareAuthToken.CREATOR);
                            TimeStampToken _arg2 = (TimeStampToken) data.readTypedObject(TimeStampToken.CREATOR);
                            data.enforceNoDataAvail();
                            updateAad(_arg0, _arg1, _arg2);
                            reply.writeNoException();
                            break;
                        case 2:
                            byte[] _arg02 = data.createByteArray();
                            HardwareAuthToken _arg12 = (HardwareAuthToken) data.readTypedObject(HardwareAuthToken.CREATOR);
                            TimeStampToken _arg22 = (TimeStampToken) data.readTypedObject(TimeStampToken.CREATOR);
                            data.enforceNoDataAvail();
                            byte[] _result = update(_arg02, _arg12, _arg22);
                            reply.writeNoException();
                            reply.writeByteArray(_result);
                            break;
                        case 3:
                            byte[] _arg03 = data.createByteArray();
                            byte[] _arg13 = data.createByteArray();
                            HardwareAuthToken _arg23 = (HardwareAuthToken) data.readTypedObject(HardwareAuthToken.CREATOR);
                            TimeStampToken _arg3 = (TimeStampToken) data.readTypedObject(TimeStampToken.CREATOR);
                            byte[] _arg4 = data.createByteArray();
                            data.enforceNoDataAvail();
                            byte[] _result2 = finish(_arg03, _arg13, _arg23, _arg3, _arg4);
                            reply.writeNoException();
                            reply.writeByteArray(_result2);
                            break;
                        case 4:
                            abort();
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IKeyMintOperation {
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

            @Override // android.hardware.security.keymint.IKeyMintOperation
            public void updateAad(byte[] input, HardwareAuthToken authToken, TimeStampToken timeStampToken) throws RemoteException {
                Parcel _data = Parcel.obtain();
                _data.markSensitive();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeByteArray(input);
                    _data.writeTypedObject(authToken, 0);
                    _data.writeTypedObject(timeStampToken, 0);
                    boolean _status = this.mRemote.transact(1, _data, _reply, 32);
                    if (!_status) {
                        throw new RemoteException("Method updateAad is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.security.keymint.IKeyMintOperation
            public byte[] update(byte[] input, HardwareAuthToken authToken, TimeStampToken timeStampToken) throws RemoteException {
                Parcel _data = Parcel.obtain();
                _data.markSensitive();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeByteArray(input);
                    _data.writeTypedObject(authToken, 0);
                    _data.writeTypedObject(timeStampToken, 0);
                    boolean _status = this.mRemote.transact(2, _data, _reply, 32);
                    if (!_status) {
                        throw new RemoteException("Method update is unimplemented.");
                    }
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.security.keymint.IKeyMintOperation
            public byte[] finish(byte[] input, byte[] signature, HardwareAuthToken authToken, TimeStampToken timestampToken, byte[] confirmationToken) throws RemoteException {
                Parcel _data = Parcel.obtain();
                _data.markSensitive();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeByteArray(input);
                    _data.writeByteArray(signature);
                    _data.writeTypedObject(authToken, 0);
                    _data.writeTypedObject(timestampToken, 0);
                    _data.writeByteArray(confirmationToken);
                    boolean _status = this.mRemote.transact(3, _data, _reply, 32);
                    if (!_status) {
                        throw new RemoteException("Method finish is unimplemented.");
                    }
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.security.keymint.IKeyMintOperation
            public void abort() throws RemoteException {
                Parcel _data = Parcel.obtain();
                _data.markSensitive();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(4, _data, _reply, 32);
                    if (!_status) {
                        throw new RemoteException("Method abort is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.security.keymint.IKeyMintOperation
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

            @Override // android.hardware.security.keymint.IKeyMintOperation
            public synchronized String getInterfaceHash() throws RemoteException {
                if ("-1".equals(this.mCachedHash)) {
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    data.writeInterfaceToken(DESCRIPTOR);
                    this.mRemote.transact(16777214, data, reply, 0);
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
