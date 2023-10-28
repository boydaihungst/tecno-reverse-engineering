package android.app.cloudsearch;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface ICloudSearchManagerCallback extends IInterface {
    public static final String DESCRIPTOR = "android.app.cloudsearch.ICloudSearchManagerCallback";

    void onSearchFailed(SearchResponse searchResponse) throws RemoteException;

    void onSearchSucceeded(SearchResponse searchResponse) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements ICloudSearchManagerCallback {
        @Override // android.app.cloudsearch.ICloudSearchManagerCallback
        public void onSearchSucceeded(SearchResponse response) throws RemoteException {
        }

        @Override // android.app.cloudsearch.ICloudSearchManagerCallback
        public void onSearchFailed(SearchResponse response) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements ICloudSearchManagerCallback {
        static final int TRANSACTION_onSearchFailed = 2;
        static final int TRANSACTION_onSearchSucceeded = 1;

        public Stub() {
            attachInterface(this, ICloudSearchManagerCallback.DESCRIPTOR);
        }

        public static ICloudSearchManagerCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ICloudSearchManagerCallback.DESCRIPTOR);
            if (iin != null && (iin instanceof ICloudSearchManagerCallback)) {
                return (ICloudSearchManagerCallback) iin;
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
                    return "onSearchSucceeded";
                case 2:
                    return "onSearchFailed";
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
                data.enforceInterface(ICloudSearchManagerCallback.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ICloudSearchManagerCallback.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            SearchResponse _arg0 = (SearchResponse) data.readTypedObject(SearchResponse.CREATOR);
                            data.enforceNoDataAvail();
                            onSearchSucceeded(_arg0);
                            break;
                        case 2:
                            SearchResponse _arg02 = (SearchResponse) data.readTypedObject(SearchResponse.CREATOR);
                            data.enforceNoDataAvail();
                            onSearchFailed(_arg02);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements ICloudSearchManagerCallback {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ICloudSearchManagerCallback.DESCRIPTOR;
            }

            @Override // android.app.cloudsearch.ICloudSearchManagerCallback
            public void onSearchSucceeded(SearchResponse response) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ICloudSearchManagerCallback.DESCRIPTOR);
                    _data.writeTypedObject(response, 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.cloudsearch.ICloudSearchManagerCallback
            public void onSearchFailed(SearchResponse response) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ICloudSearchManagerCallback.DESCRIPTOR);
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
