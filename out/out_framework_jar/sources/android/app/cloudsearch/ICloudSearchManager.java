package android.app.cloudsearch;

import android.app.cloudsearch.ICloudSearchManagerCallback;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface ICloudSearchManager extends IInterface {
    public static final String DESCRIPTOR = "android.app.cloudsearch.ICloudSearchManager";

    void returnResults(IBinder iBinder, String str, SearchResponse searchResponse) throws RemoteException;

    void search(SearchRequest searchRequest, ICloudSearchManagerCallback iCloudSearchManagerCallback) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements ICloudSearchManager {
        @Override // android.app.cloudsearch.ICloudSearchManager
        public void search(SearchRequest request, ICloudSearchManagerCallback callBack) throws RemoteException {
        }

        @Override // android.app.cloudsearch.ICloudSearchManager
        public void returnResults(IBinder token, String requestId, SearchResponse response) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements ICloudSearchManager {
        static final int TRANSACTION_returnResults = 2;
        static final int TRANSACTION_search = 1;

        public Stub() {
            attachInterface(this, ICloudSearchManager.DESCRIPTOR);
        }

        public static ICloudSearchManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ICloudSearchManager.DESCRIPTOR);
            if (iin != null && (iin instanceof ICloudSearchManager)) {
                return (ICloudSearchManager) iin;
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
                    return "search";
                case 2:
                    return "returnResults";
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
                data.enforceInterface(ICloudSearchManager.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ICloudSearchManager.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            SearchRequest _arg0 = (SearchRequest) data.readTypedObject(SearchRequest.CREATOR);
                            ICloudSearchManagerCallback _arg1 = ICloudSearchManagerCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            search(_arg0, _arg1);
                            break;
                        case 2:
                            IBinder _arg02 = data.readStrongBinder();
                            String _arg12 = data.readString();
                            SearchResponse _arg2 = (SearchResponse) data.readTypedObject(SearchResponse.CREATOR);
                            data.enforceNoDataAvail();
                            returnResults(_arg02, _arg12, _arg2);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements ICloudSearchManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ICloudSearchManager.DESCRIPTOR;
            }

            @Override // android.app.cloudsearch.ICloudSearchManager
            public void search(SearchRequest request, ICloudSearchManagerCallback callBack) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ICloudSearchManager.DESCRIPTOR);
                    _data.writeTypedObject(request, 0);
                    _data.writeStrongInterface(callBack);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.cloudsearch.ICloudSearchManager
            public void returnResults(IBinder token, String requestId, SearchResponse response) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ICloudSearchManager.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeString(requestId);
                    _data.writeTypedObject(response, 0);
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
