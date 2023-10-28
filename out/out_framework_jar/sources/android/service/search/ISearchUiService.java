package android.service.search;

import android.app.search.ISearchCallback;
import android.app.search.Query;
import android.app.search.SearchContext;
import android.app.search.SearchSessionId;
import android.app.search.SearchTargetEvent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes3.dex */
public interface ISearchUiService extends IInterface {
    public static final String DESCRIPTOR = "android.service.search.ISearchUiService";

    void onCreateSearchSession(SearchContext searchContext, SearchSessionId searchSessionId) throws RemoteException;

    void onDestroy(SearchSessionId searchSessionId) throws RemoteException;

    void onNotifyEvent(SearchSessionId searchSessionId, Query query, SearchTargetEvent searchTargetEvent) throws RemoteException;

    void onQuery(SearchSessionId searchSessionId, Query query, ISearchCallback iSearchCallback) throws RemoteException;

    /* loaded from: classes3.dex */
    public static class Default implements ISearchUiService {
        @Override // android.service.search.ISearchUiService
        public void onCreateSearchSession(SearchContext context, SearchSessionId sessionId) throws RemoteException {
        }

        @Override // android.service.search.ISearchUiService
        public void onQuery(SearchSessionId sessionId, Query input, ISearchCallback callback) throws RemoteException {
        }

        @Override // android.service.search.ISearchUiService
        public void onNotifyEvent(SearchSessionId sessionId, Query input, SearchTargetEvent event) throws RemoteException {
        }

        @Override // android.service.search.ISearchUiService
        public void onDestroy(SearchSessionId sessionId) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes3.dex */
    public static abstract class Stub extends Binder implements ISearchUiService {
        static final int TRANSACTION_onCreateSearchSession = 1;
        static final int TRANSACTION_onDestroy = 4;
        static final int TRANSACTION_onNotifyEvent = 3;
        static final int TRANSACTION_onQuery = 2;

        public Stub() {
            attachInterface(this, ISearchUiService.DESCRIPTOR);
        }

        public static ISearchUiService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ISearchUiService.DESCRIPTOR);
            if (iin != null && (iin instanceof ISearchUiService)) {
                return (ISearchUiService) iin;
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
                    return "onCreateSearchSession";
                case 2:
                    return "onQuery";
                case 3:
                    return "onNotifyEvent";
                case 4:
                    return "onDestroy";
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
                data.enforceInterface(ISearchUiService.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ISearchUiService.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            SearchContext _arg0 = (SearchContext) data.readTypedObject(SearchContext.CREATOR);
                            SearchSessionId _arg1 = (SearchSessionId) data.readTypedObject(SearchSessionId.CREATOR);
                            data.enforceNoDataAvail();
                            onCreateSearchSession(_arg0, _arg1);
                            break;
                        case 2:
                            SearchSessionId _arg02 = (SearchSessionId) data.readTypedObject(SearchSessionId.CREATOR);
                            Query _arg12 = (Query) data.readTypedObject(Query.CREATOR);
                            ISearchCallback _arg2 = ISearchCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            onQuery(_arg02, _arg12, _arg2);
                            break;
                        case 3:
                            SearchSessionId _arg03 = (SearchSessionId) data.readTypedObject(SearchSessionId.CREATOR);
                            Query _arg13 = (Query) data.readTypedObject(Query.CREATOR);
                            SearchTargetEvent _arg22 = (SearchTargetEvent) data.readTypedObject(SearchTargetEvent.CREATOR);
                            data.enforceNoDataAvail();
                            onNotifyEvent(_arg03, _arg13, _arg22);
                            break;
                        case 4:
                            SearchSessionId _arg04 = (SearchSessionId) data.readTypedObject(SearchSessionId.CREATOR);
                            data.enforceNoDataAvail();
                            onDestroy(_arg04);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes3.dex */
        private static class Proxy implements ISearchUiService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ISearchUiService.DESCRIPTOR;
            }

            @Override // android.service.search.ISearchUiService
            public void onCreateSearchSession(SearchContext context, SearchSessionId sessionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ISearchUiService.DESCRIPTOR);
                    _data.writeTypedObject(context, 0);
                    _data.writeTypedObject(sessionId, 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.service.search.ISearchUiService
            public void onQuery(SearchSessionId sessionId, Query input, ISearchCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ISearchUiService.DESCRIPTOR);
                    _data.writeTypedObject(sessionId, 0);
                    _data.writeTypedObject(input, 0);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.service.search.ISearchUiService
            public void onNotifyEvent(SearchSessionId sessionId, Query input, SearchTargetEvent event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ISearchUiService.DESCRIPTOR);
                    _data.writeTypedObject(sessionId, 0);
                    _data.writeTypedObject(input, 0);
                    _data.writeTypedObject(event, 0);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.service.search.ISearchUiService
            public void onDestroy(SearchSessionId sessionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ISearchUiService.DESCRIPTOR);
                    _data.writeTypedObject(sessionId, 0);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
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
