package com.mediatek.search;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.mediatek.common.search.SearchEngine;
import java.util.List;
/* loaded from: classes.dex */
public interface ISearchEngineManagerService extends IInterface {
    public static final String DESCRIPTOR = "com.mediatek.search.ISearchEngineManagerService";

    List<SearchEngine> getAvailables() throws RemoteException;

    SearchEngine getBestMatch(String str, String str2) throws RemoteException;

    SearchEngine getDefault() throws RemoteException;

    SearchEngine getSearchEngine(int i, String str) throws RemoteException;

    boolean setDefault(SearchEngine searchEngine) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements ISearchEngineManagerService {
        @Override // com.mediatek.search.ISearchEngineManagerService
        public List<SearchEngine> getAvailables() throws RemoteException {
            return null;
        }

        @Override // com.mediatek.search.ISearchEngineManagerService
        public SearchEngine getDefault() throws RemoteException {
            return null;
        }

        @Override // com.mediatek.search.ISearchEngineManagerService
        public SearchEngine getBestMatch(String name, String favicon) throws RemoteException {
            return null;
        }

        @Override // com.mediatek.search.ISearchEngineManagerService
        public SearchEngine getSearchEngine(int field, String value) throws RemoteException {
            return null;
        }

        @Override // com.mediatek.search.ISearchEngineManagerService
        public boolean setDefault(SearchEngine engine) throws RemoteException {
            return false;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements ISearchEngineManagerService {
        static final int TRANSACTION_getAvailables = 1;
        static final int TRANSACTION_getBestMatch = 3;
        static final int TRANSACTION_getDefault = 2;
        static final int TRANSACTION_getSearchEngine = 4;
        static final int TRANSACTION_setDefault = 5;

        public Stub() {
            attachInterface(this, ISearchEngineManagerService.DESCRIPTOR);
        }

        public static ISearchEngineManagerService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ISearchEngineManagerService.DESCRIPTOR);
            if (iin != null && (iin instanceof ISearchEngineManagerService)) {
                return (ISearchEngineManagerService) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(ISearchEngineManagerService.DESCRIPTOR);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(ISearchEngineManagerService.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            List<SearchEngine> _result = getAvailables();
                            reply.writeNoException();
                            reply.writeTypedList(_result);
                            break;
                        case 2:
                            SearchEngine _result2 = getDefault();
                            reply.writeNoException();
                            reply.writeTypedObject(_result2, 1);
                            break;
                        case 3:
                            String _arg0 = data.readString();
                            String _arg1 = data.readString();
                            data.enforceNoDataAvail();
                            SearchEngine _result3 = getBestMatch(_arg0, _arg1);
                            reply.writeNoException();
                            reply.writeTypedObject(_result3, 1);
                            break;
                        case 4:
                            int _arg02 = data.readInt();
                            String _arg12 = data.readString();
                            data.enforceNoDataAvail();
                            SearchEngine _result4 = getSearchEngine(_arg02, _arg12);
                            reply.writeNoException();
                            reply.writeTypedObject(_result4, 1);
                            break;
                        case 5:
                            SearchEngine _arg03 = (SearchEngine) data.readTypedObject(SearchEngine.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result5 = setDefault(_arg03);
                            reply.writeNoException();
                            reply.writeBoolean(_result5);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements ISearchEngineManagerService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ISearchEngineManagerService.DESCRIPTOR;
            }

            @Override // com.mediatek.search.ISearchEngineManagerService
            public List<SearchEngine> getAvailables() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ISearchEngineManagerService.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    List<SearchEngine> _result = _reply.createTypedArrayList(SearchEngine.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.search.ISearchEngineManagerService
            public SearchEngine getDefault() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ISearchEngineManagerService.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    SearchEngine _result = (SearchEngine) _reply.readTypedObject(SearchEngine.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.search.ISearchEngineManagerService
            public SearchEngine getBestMatch(String name, String favicon) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ISearchEngineManagerService.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeString(favicon);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    SearchEngine _result = (SearchEngine) _reply.readTypedObject(SearchEngine.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.search.ISearchEngineManagerService
            public SearchEngine getSearchEngine(int field, String value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ISearchEngineManagerService.DESCRIPTOR);
                    _data.writeInt(field);
                    _data.writeString(value);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    SearchEngine _result = (SearchEngine) _reply.readTypedObject(SearchEngine.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.search.ISearchEngineManagerService
            public boolean setDefault(SearchEngine engine) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ISearchEngineManagerService.DESCRIPTOR);
                    _data.writeTypedObject(engine, 0);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
