package android.accounts;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IAccountManagerResponse extends IInterface {
    void onError(int i, String str) throws RemoteException;

    void onResult(Bundle bundle) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IAccountManagerResponse {
        @Override // android.accounts.IAccountManagerResponse
        public void onResult(Bundle value) throws RemoteException {
        }

        @Override // android.accounts.IAccountManagerResponse
        public void onError(int errorCode, String errorMessage) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IAccountManagerResponse {
        public static final String DESCRIPTOR = "android.accounts.IAccountManagerResponse";
        static final int TRANSACTION_onError = 2;
        static final int TRANSACTION_onResult = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAccountManagerResponse asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IAccountManagerResponse)) {
                return (IAccountManagerResponse) iin;
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
                    return "onResult";
                case 2:
                    return "onError";
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
                data.enforceInterface(DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            Bundle _arg0 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            onResult(_arg0);
                            break;
                        case 2:
                            int _arg02 = data.readInt();
                            String _arg1 = data.readString();
                            data.enforceNoDataAvail();
                            onError(_arg02, _arg1);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements IAccountManagerResponse {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override // android.accounts.IAccountManagerResponse
            public void onResult(Bundle value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(value, 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.accounts.IAccountManagerResponse
            public void onError(int errorCode, String errorMessage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(errorCode);
                    _data.writeString(errorMessage);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 1;
        }
    }
}
