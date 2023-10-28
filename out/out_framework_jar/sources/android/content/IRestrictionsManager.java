package android.content;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IRestrictionsManager extends IInterface {
    Intent createLocalApprovalIntent() throws RemoteException;

    Bundle getApplicationRestrictions(String str) throws RemoteException;

    boolean hasRestrictionsProvider() throws RemoteException;

    void notifyPermissionResponse(String str, PersistableBundle persistableBundle) throws RemoteException;

    void requestPermission(String str, String str2, String str3, PersistableBundle persistableBundle) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IRestrictionsManager {
        @Override // android.content.IRestrictionsManager
        public Bundle getApplicationRestrictions(String packageName) throws RemoteException {
            return null;
        }

        @Override // android.content.IRestrictionsManager
        public boolean hasRestrictionsProvider() throws RemoteException {
            return false;
        }

        @Override // android.content.IRestrictionsManager
        public void requestPermission(String packageName, String requestType, String requestId, PersistableBundle requestData) throws RemoteException {
        }

        @Override // android.content.IRestrictionsManager
        public void notifyPermissionResponse(String packageName, PersistableBundle response) throws RemoteException {
        }

        @Override // android.content.IRestrictionsManager
        public Intent createLocalApprovalIntent() throws RemoteException {
            return null;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IRestrictionsManager {
        public static final String DESCRIPTOR = "android.content.IRestrictionsManager";
        static final int TRANSACTION_createLocalApprovalIntent = 5;
        static final int TRANSACTION_getApplicationRestrictions = 1;
        static final int TRANSACTION_hasRestrictionsProvider = 2;
        static final int TRANSACTION_notifyPermissionResponse = 4;
        static final int TRANSACTION_requestPermission = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IRestrictionsManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IRestrictionsManager)) {
                return (IRestrictionsManager) iin;
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
                    return "getApplicationRestrictions";
                case 2:
                    return "hasRestrictionsProvider";
                case 3:
                    return "requestPermission";
                case 4:
                    return "notifyPermissionResponse";
                case 5:
                    return "createLocalApprovalIntent";
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
                            String _arg0 = data.readString();
                            data.enforceNoDataAvail();
                            Bundle _result = getApplicationRestrictions(_arg0);
                            reply.writeNoException();
                            reply.writeTypedObject(_result, 1);
                            break;
                        case 2:
                            boolean _result2 = hasRestrictionsProvider();
                            reply.writeNoException();
                            reply.writeBoolean(_result2);
                            break;
                        case 3:
                            String _arg02 = data.readString();
                            String _arg1 = data.readString();
                            String _arg2 = data.readString();
                            PersistableBundle _arg3 = (PersistableBundle) data.readTypedObject(PersistableBundle.CREATOR);
                            data.enforceNoDataAvail();
                            requestPermission(_arg02, _arg1, _arg2, _arg3);
                            reply.writeNoException();
                            break;
                        case 4:
                            String _arg03 = data.readString();
                            PersistableBundle _arg12 = (PersistableBundle) data.readTypedObject(PersistableBundle.CREATOR);
                            data.enforceNoDataAvail();
                            notifyPermissionResponse(_arg03, _arg12);
                            reply.writeNoException();
                            break;
                        case 5:
                            Intent _result3 = createLocalApprovalIntent();
                            reply.writeNoException();
                            reply.writeTypedObject(_result3, 1);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements IRestrictionsManager {
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

            @Override // android.content.IRestrictionsManager
            public Bundle getApplicationRestrictions(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    Bundle _result = (Bundle) _reply.readTypedObject(Bundle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.IRestrictionsManager
            public boolean hasRestrictionsProvider() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.IRestrictionsManager
            public void requestPermission(String packageName, String requestType, String requestId, PersistableBundle requestData) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(requestType);
                    _data.writeString(requestId);
                    _data.writeTypedObject(requestData, 0);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.IRestrictionsManager
            public void notifyPermissionResponse(String packageName, PersistableBundle response) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeTypedObject(response, 0);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.IRestrictionsManager
            public Intent createLocalApprovalIntent() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    Intent _result = (Intent) _reply.readTypedObject(Intent.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 4;
        }
    }
}
