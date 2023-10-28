package android.app.timezone;

import android.app.timezone.ICallback;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IRulesManager extends IInterface {
    public static final String DESCRIPTOR = "android.app.timezone.IRulesManager";

    RulesState getRulesState() throws RemoteException;

    int requestInstall(ParcelFileDescriptor parcelFileDescriptor, byte[] bArr, ICallback iCallback) throws RemoteException;

    void requestNothing(byte[] bArr, boolean z) throws RemoteException;

    int requestUninstall(byte[] bArr, ICallback iCallback) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IRulesManager {
        @Override // android.app.timezone.IRulesManager
        public RulesState getRulesState() throws RemoteException {
            return null;
        }

        @Override // android.app.timezone.IRulesManager
        public int requestInstall(ParcelFileDescriptor distroFileDescriptor, byte[] checkToken, ICallback callback) throws RemoteException {
            return 0;
        }

        @Override // android.app.timezone.IRulesManager
        public int requestUninstall(byte[] checkToken, ICallback callback) throws RemoteException {
            return 0;
        }

        @Override // android.app.timezone.IRulesManager
        public void requestNothing(byte[] token, boolean success) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IRulesManager {
        static final int TRANSACTION_getRulesState = 1;
        static final int TRANSACTION_requestInstall = 2;
        static final int TRANSACTION_requestNothing = 4;
        static final int TRANSACTION_requestUninstall = 3;

        public Stub() {
            attachInterface(this, IRulesManager.DESCRIPTOR);
        }

        public static IRulesManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IRulesManager.DESCRIPTOR);
            if (iin != null && (iin instanceof IRulesManager)) {
                return (IRulesManager) iin;
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
                    return "getRulesState";
                case 2:
                    return "requestInstall";
                case 3:
                    return "requestUninstall";
                case 4:
                    return "requestNothing";
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
                data.enforceInterface(IRulesManager.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IRulesManager.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            RulesState _result = getRulesState();
                            reply.writeNoException();
                            reply.writeTypedObject(_result, 1);
                            break;
                        case 2:
                            ParcelFileDescriptor _arg0 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
                            byte[] _arg1 = data.createByteArray();
                            ICallback _arg2 = ICallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            int _result2 = requestInstall(_arg0, _arg1, _arg2);
                            reply.writeNoException();
                            reply.writeInt(_result2);
                            break;
                        case 3:
                            byte[] _arg02 = data.createByteArray();
                            ICallback _arg12 = ICallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            int _result3 = requestUninstall(_arg02, _arg12);
                            reply.writeNoException();
                            reply.writeInt(_result3);
                            break;
                        case 4:
                            byte[] _arg03 = data.createByteArray();
                            boolean _arg13 = data.readBoolean();
                            data.enforceNoDataAvail();
                            requestNothing(_arg03, _arg13);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IRulesManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IRulesManager.DESCRIPTOR;
            }

            @Override // android.app.timezone.IRulesManager
            public RulesState getRulesState() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IRulesManager.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    RulesState _result = (RulesState) _reply.readTypedObject(RulesState.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.timezone.IRulesManager
            public int requestInstall(ParcelFileDescriptor distroFileDescriptor, byte[] checkToken, ICallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IRulesManager.DESCRIPTOR);
                    _data.writeTypedObject(distroFileDescriptor, 0);
                    _data.writeByteArray(checkToken);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.timezone.IRulesManager
            public int requestUninstall(byte[] checkToken, ICallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IRulesManager.DESCRIPTOR);
                    _data.writeByteArray(checkToken);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.timezone.IRulesManager
            public void requestNothing(byte[] token, boolean success) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IRulesManager.DESCRIPTOR);
                    _data.writeByteArray(token);
                    _data.writeBoolean(success);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 3;
        }
    }
}
