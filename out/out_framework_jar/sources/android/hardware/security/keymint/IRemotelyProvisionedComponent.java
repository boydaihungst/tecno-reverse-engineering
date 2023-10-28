package android.hardware.security.keymint;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes2.dex */
public interface IRemotelyProvisionedComponent extends IInterface {
    public static final String DESCRIPTOR = "android$hardware$security$keymint$IRemotelyProvisionedComponent".replace('$', '.');
    public static final String HASH = "207c9f218b9b9e4e74ff5232eb16511eca9d7d2e";
    public static final int STATUS_FAILED = 1;
    public static final int STATUS_INVALID_EEK = 5;
    public static final int STATUS_INVALID_MAC = 2;
    public static final int STATUS_PRODUCTION_KEY_IN_TEST_REQUEST = 3;
    public static final int STATUS_TEST_KEY_IN_PRODUCTION_REQUEST = 4;
    public static final int VERSION = 2;

    byte[] generateCertificateRequest(boolean z, MacedPublicKey[] macedPublicKeyArr, byte[] bArr, byte[] bArr2, DeviceInfo deviceInfo, ProtectedData protectedData) throws RemoteException;

    byte[] generateEcdsaP256KeyPair(boolean z, MacedPublicKey macedPublicKey) throws RemoteException;

    RpcHardwareInfo getHardwareInfo() throws RemoteException;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IRemotelyProvisionedComponent {
        @Override // android.hardware.security.keymint.IRemotelyProvisionedComponent
        public RpcHardwareInfo getHardwareInfo() throws RemoteException {
            return null;
        }

        @Override // android.hardware.security.keymint.IRemotelyProvisionedComponent
        public byte[] generateEcdsaP256KeyPair(boolean testMode, MacedPublicKey macedPublicKey) throws RemoteException {
            return null;
        }

        @Override // android.hardware.security.keymint.IRemotelyProvisionedComponent
        public byte[] generateCertificateRequest(boolean testMode, MacedPublicKey[] keysToSign, byte[] endpointEncryptionCertChain, byte[] challenge, DeviceInfo deviceInfo, ProtectedData protectedData) throws RemoteException {
            return null;
        }

        @Override // android.hardware.security.keymint.IRemotelyProvisionedComponent
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.hardware.security.keymint.IRemotelyProvisionedComponent
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IRemotelyProvisionedComponent {
        static final int TRANSACTION_generateCertificateRequest = 3;
        static final int TRANSACTION_generateEcdsaP256KeyPair = 2;
        static final int TRANSACTION_getHardwareInfo = 1;
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;

        public Stub() {
            markVintfStability();
            attachInterface(this, DESCRIPTOR);
        }

        public static IRemotelyProvisionedComponent asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IRemotelyProvisionedComponent)) {
                return (IRemotelyProvisionedComponent) iin;
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
                            RpcHardwareInfo _result = getHardwareInfo();
                            reply.writeNoException();
                            reply.writeTypedObject(_result, 1);
                            break;
                        case 2:
                            boolean _arg0 = data.readBoolean();
                            MacedPublicKey _arg1 = new MacedPublicKey();
                            data.enforceNoDataAvail();
                            byte[] _result2 = generateEcdsaP256KeyPair(_arg0, _arg1);
                            reply.writeNoException();
                            reply.writeByteArray(_result2);
                            reply.writeTypedObject(_arg1, 1);
                            break;
                        case 3:
                            boolean _arg02 = data.readBoolean();
                            MacedPublicKey[] _arg12 = (MacedPublicKey[]) data.createTypedArray(MacedPublicKey.CREATOR);
                            byte[] _arg2 = data.createByteArray();
                            byte[] _arg3 = data.createByteArray();
                            DeviceInfo _arg4 = new DeviceInfo();
                            ProtectedData _arg5 = new ProtectedData();
                            data.enforceNoDataAvail();
                            byte[] _result3 = generateCertificateRequest(_arg02, _arg12, _arg2, _arg3, _arg4, _arg5);
                            reply.writeNoException();
                            reply.writeByteArray(_result3);
                            reply.writeTypedObject(_arg4, 1);
                            reply.writeTypedObject(_arg5, 1);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IRemotelyProvisionedComponent {
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

            @Override // android.hardware.security.keymint.IRemotelyProvisionedComponent
            public RpcHardwareInfo getHardwareInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(1, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method getHardwareInfo is unimplemented.");
                    }
                    _reply.readException();
                    RpcHardwareInfo _result = (RpcHardwareInfo) _reply.readTypedObject(RpcHardwareInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.security.keymint.IRemotelyProvisionedComponent
            public byte[] generateEcdsaP256KeyPair(boolean testMode, MacedPublicKey macedPublicKey) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(testMode);
                    boolean _status = this.mRemote.transact(2, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method generateEcdsaP256KeyPair is unimplemented.");
                    }
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    if (_reply.readInt() != 0) {
                        macedPublicKey.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.security.keymint.IRemotelyProvisionedComponent
            public byte[] generateCertificateRequest(boolean testMode, MacedPublicKey[] keysToSign, byte[] endpointEncryptionCertChain, byte[] challenge, DeviceInfo deviceInfo, ProtectedData protectedData) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(testMode);
                    _data.writeTypedArray(keysToSign, 0);
                    _data.writeByteArray(endpointEncryptionCertChain);
                    _data.writeByteArray(challenge);
                    boolean _status = this.mRemote.transact(3, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method generateCertificateRequest is unimplemented.");
                    }
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    if (_reply.readInt() != 0) {
                        deviceInfo.readFromParcel(_reply);
                    }
                    if (_reply.readInt() != 0) {
                        protectedData.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.security.keymint.IRemotelyProvisionedComponent
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

            @Override // android.hardware.security.keymint.IRemotelyProvisionedComponent
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
