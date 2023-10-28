package android.net;

import android.net.ipmemorystore.Blob;
import android.net.ipmemorystore.IOnBlobRetrievedListener;
import android.net.ipmemorystore.IOnL2KeyResponseListener;
import android.net.ipmemorystore.IOnNetworkAttributesRetrievedListener;
import android.net.ipmemorystore.IOnSameL3NetworkResponseListener;
import android.net.ipmemorystore.IOnStatusAndCountListener;
import android.net.ipmemorystore.IOnStatusListener;
import android.net.ipmemorystore.NetworkAttributesParcelable;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IIpMemoryStore extends IInterface {
    public static final String DESCRIPTOR = "android$net$IIpMemoryStore".replace('$', '.');
    public static final String HASH = "d5ea5eb3ddbdaa9a986ce6ba70b0804ca3e39b0c";
    public static final int VERSION = 10;

    void delete(String str, boolean z, IOnStatusAndCountListener iOnStatusAndCountListener) throws RemoteException;

    void deleteCluster(String str, boolean z, IOnStatusAndCountListener iOnStatusAndCountListener) throws RemoteException;

    void factoryReset() throws RemoteException;

    void findL2Key(NetworkAttributesParcelable networkAttributesParcelable, IOnL2KeyResponseListener iOnL2KeyResponseListener) throws RemoteException;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void isSameNetwork(String str, String str2, IOnSameL3NetworkResponseListener iOnSameL3NetworkResponseListener) throws RemoteException;

    void retrieveBlob(String str, String str2, String str3, IOnBlobRetrievedListener iOnBlobRetrievedListener) throws RemoteException;

    void retrieveNetworkAttributes(String str, IOnNetworkAttributesRetrievedListener iOnNetworkAttributesRetrievedListener) throws RemoteException;

    void storeBlob(String str, String str2, String str3, Blob blob, IOnStatusListener iOnStatusListener) throws RemoteException;

    void storeNetworkAttributes(String str, NetworkAttributesParcelable networkAttributesParcelable, IOnStatusListener iOnStatusListener) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IIpMemoryStore {
        @Override // android.net.IIpMemoryStore
        public void storeNetworkAttributes(String l2Key, NetworkAttributesParcelable attributes, IOnStatusListener listener) throws RemoteException {
        }

        @Override // android.net.IIpMemoryStore
        public void storeBlob(String l2Key, String clientId, String name, Blob data, IOnStatusListener listener) throws RemoteException {
        }

        @Override // android.net.IIpMemoryStore
        public void findL2Key(NetworkAttributesParcelable attributes, IOnL2KeyResponseListener listener) throws RemoteException {
        }

        @Override // android.net.IIpMemoryStore
        public void isSameNetwork(String l2Key1, String l2Key2, IOnSameL3NetworkResponseListener listener) throws RemoteException {
        }

        @Override // android.net.IIpMemoryStore
        public void retrieveNetworkAttributes(String l2Key, IOnNetworkAttributesRetrievedListener listener) throws RemoteException {
        }

        @Override // android.net.IIpMemoryStore
        public void retrieveBlob(String l2Key, String clientId, String name, IOnBlobRetrievedListener listener) throws RemoteException {
        }

        @Override // android.net.IIpMemoryStore
        public void factoryReset() throws RemoteException {
        }

        @Override // android.net.IIpMemoryStore
        public void delete(String l2Key, boolean needWipe, IOnStatusAndCountListener listener) throws RemoteException {
        }

        @Override // android.net.IIpMemoryStore
        public void deleteCluster(String cluster, boolean needWipe, IOnStatusAndCountListener listener) throws RemoteException {
        }

        @Override // android.net.IIpMemoryStore
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.net.IIpMemoryStore
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IIpMemoryStore {
        static final int TRANSACTION_delete = 8;
        static final int TRANSACTION_deleteCluster = 9;
        static final int TRANSACTION_factoryReset = 7;
        static final int TRANSACTION_findL2Key = 3;
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_isSameNetwork = 4;
        static final int TRANSACTION_retrieveBlob = 6;
        static final int TRANSACTION_retrieveNetworkAttributes = 5;
        static final int TRANSACTION_storeBlob = 2;
        static final int TRANSACTION_storeNetworkAttributes = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IIpMemoryStore asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IIpMemoryStore)) {
                return (IIpMemoryStore) iin;
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
                case TRANSACTION_getInterfaceHash /* 16777214 */:
                    reply.writeNoException();
                    reply.writeString(getInterfaceHash());
                    return true;
                case 16777215:
                    reply.writeNoException();
                    reply.writeInt(getInterfaceVersion());
                    return true;
                case 1598968902:
                    reply.writeString(descriptor);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            String _arg0 = data.readString();
                            NetworkAttributesParcelable _arg1 = (NetworkAttributesParcelable) data.readTypedObject(NetworkAttributesParcelable.CREATOR);
                            IOnStatusListener _arg2 = IOnStatusListener.Stub.asInterface(data.readStrongBinder());
                            storeNetworkAttributes(_arg0, _arg1, _arg2);
                            break;
                        case 2:
                            String _arg02 = data.readString();
                            String _arg12 = data.readString();
                            String _arg22 = data.readString();
                            Blob _arg3 = (Blob) data.readTypedObject(Blob.CREATOR);
                            IOnStatusListener _arg4 = IOnStatusListener.Stub.asInterface(data.readStrongBinder());
                            storeBlob(_arg02, _arg12, _arg22, _arg3, _arg4);
                            break;
                        case 3:
                            NetworkAttributesParcelable _arg03 = (NetworkAttributesParcelable) data.readTypedObject(NetworkAttributesParcelable.CREATOR);
                            IOnL2KeyResponseListener _arg13 = IOnL2KeyResponseListener.Stub.asInterface(data.readStrongBinder());
                            findL2Key(_arg03, _arg13);
                            break;
                        case 4:
                            String _arg04 = data.readString();
                            String _arg14 = data.readString();
                            IOnSameL3NetworkResponseListener _arg23 = IOnSameL3NetworkResponseListener.Stub.asInterface(data.readStrongBinder());
                            isSameNetwork(_arg04, _arg14, _arg23);
                            break;
                        case 5:
                            String _arg05 = data.readString();
                            IOnNetworkAttributesRetrievedListener _arg15 = IOnNetworkAttributesRetrievedListener.Stub.asInterface(data.readStrongBinder());
                            retrieveNetworkAttributes(_arg05, _arg15);
                            break;
                        case 6:
                            String _arg06 = data.readString();
                            String _arg16 = data.readString();
                            String _arg24 = data.readString();
                            IOnBlobRetrievedListener _arg32 = IOnBlobRetrievedListener.Stub.asInterface(data.readStrongBinder());
                            retrieveBlob(_arg06, _arg16, _arg24, _arg32);
                            break;
                        case 7:
                            factoryReset();
                            break;
                        case 8:
                            String _arg07 = data.readString();
                            boolean _arg17 = data.readBoolean();
                            IOnStatusAndCountListener _arg25 = IOnStatusAndCountListener.Stub.asInterface(data.readStrongBinder());
                            delete(_arg07, _arg17, _arg25);
                            break;
                        case 9:
                            String _arg08 = data.readString();
                            boolean _arg18 = data.readBoolean();
                            IOnStatusAndCountListener _arg26 = IOnStatusAndCountListener.Stub.asInterface(data.readStrongBinder());
                            deleteCluster(_arg08, _arg18, _arg26);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IIpMemoryStore {
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

            @Override // android.net.IIpMemoryStore
            public void storeNetworkAttributes(String l2Key, NetworkAttributesParcelable attributes, IOnStatusListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(l2Key);
                    _data.writeTypedObject(attributes, 0);
                    _data.writeStrongInterface(listener);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method storeNetworkAttributes is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.IIpMemoryStore
            public void storeBlob(String l2Key, String clientId, String name, Blob data, IOnStatusListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(l2Key);
                    _data.writeString(clientId);
                    _data.writeString(name);
                    _data.writeTypedObject(data, 0);
                    _data.writeStrongInterface(listener);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method storeBlob is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.IIpMemoryStore
            public void findL2Key(NetworkAttributesParcelable attributes, IOnL2KeyResponseListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(attributes, 0);
                    _data.writeStrongInterface(listener);
                    boolean _status = this.mRemote.transact(3, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method findL2Key is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.IIpMemoryStore
            public void isSameNetwork(String l2Key1, String l2Key2, IOnSameL3NetworkResponseListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(l2Key1);
                    _data.writeString(l2Key2);
                    _data.writeStrongInterface(listener);
                    boolean _status = this.mRemote.transact(4, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method isSameNetwork is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.IIpMemoryStore
            public void retrieveNetworkAttributes(String l2Key, IOnNetworkAttributesRetrievedListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(l2Key);
                    _data.writeStrongInterface(listener);
                    boolean _status = this.mRemote.transact(5, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method retrieveNetworkAttributes is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.IIpMemoryStore
            public void retrieveBlob(String l2Key, String clientId, String name, IOnBlobRetrievedListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(l2Key);
                    _data.writeString(clientId);
                    _data.writeString(name);
                    _data.writeStrongInterface(listener);
                    boolean _status = this.mRemote.transact(6, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method retrieveBlob is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.IIpMemoryStore
            public void factoryReset() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(7, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method factoryReset is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.IIpMemoryStore
            public void delete(String l2Key, boolean needWipe, IOnStatusAndCountListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(l2Key);
                    _data.writeBoolean(needWipe);
                    _data.writeStrongInterface(listener);
                    boolean _status = this.mRemote.transact(8, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method delete is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.IIpMemoryStore
            public void deleteCluster(String cluster, boolean needWipe, IOnStatusAndCountListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(cluster);
                    _data.writeBoolean(needWipe);
                    _data.writeStrongInterface(listener);
                    boolean _status = this.mRemote.transact(9, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method deleteCluster is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.IIpMemoryStore
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

            @Override // android.net.IIpMemoryStore
            public synchronized String getInterfaceHash() throws RemoteException {
                if ("-1".equals(this.mCachedHash)) {
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    data.writeInterfaceToken(DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getInterfaceHash, data, reply, 0);
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
