package android.service.cloudsearch;

import android.app.cloudsearch.SearchRequest;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes3.dex */
public interface ICloudSearchService extends IInterface {
    public static final String DESCRIPTOR = "android.service.cloudsearch.ICloudSearchService";

    void onSearch(SearchRequest searchRequest) throws RemoteException;

    /* loaded from: classes3.dex */
    public static class Default implements ICloudSearchService {
        @Override // android.service.cloudsearch.ICloudSearchService
        public void onSearch(SearchRequest request) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes3.dex */
    public static abstract class Stub extends Binder implements ICloudSearchService {
        static final int TRANSACTION_onSearch = 1;

        public Stub() {
            attachInterface(this, ICloudSearchService.DESCRIPTOR);
        }

        public static ICloudSearchService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ICloudSearchService.DESCRIPTOR);
            if (iin != null && (iin instanceof ICloudSearchService)) {
                return (ICloudSearchService) iin;
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
                    return "onSearch";
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
                data.enforceInterface(ICloudSearchService.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ICloudSearchService.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            SearchRequest _arg0 = (SearchRequest) data.readTypedObject(SearchRequest.CREATOR);
                            data.enforceNoDataAvail();
                            onSearch(_arg0);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes3.dex */
        private static class Proxy implements ICloudSearchService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ICloudSearchService.DESCRIPTOR;
            }

            @Override // android.service.cloudsearch.ICloudSearchService
            public void onSearch(SearchRequest request) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ICloudSearchService.DESCRIPTOR);
                    _data.writeTypedObject(request, 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 0;
        }
    }
}
