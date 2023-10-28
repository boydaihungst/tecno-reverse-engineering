package android.net.ipmemorystore;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IOnNetworkAttributesRetrievedListener extends IInterface {
    public static final String DESCRIPTOR = "android$net$ipmemorystore$IOnNetworkAttributesRetrievedListener".replace('$', '.');
    public static final String HASH = "d5ea5eb3ddbdaa9a986ce6ba70b0804ca3e39b0c";
    public static final int VERSION = 10;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void onNetworkAttributesRetrieved(StatusParcelable statusParcelable, String str, NetworkAttributesParcelable networkAttributesParcelable) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IOnNetworkAttributesRetrievedListener {
        @Override // android.net.ipmemorystore.IOnNetworkAttributesRetrievedListener
        public void onNetworkAttributesRetrieved(StatusParcelable status, String l2Key, NetworkAttributesParcelable attributes) throws RemoteException {
        }

        @Override // android.net.ipmemorystore.IOnNetworkAttributesRetrievedListener
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.net.ipmemorystore.IOnNetworkAttributesRetrievedListener
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IOnNetworkAttributesRetrievedListener {
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_onNetworkAttributesRetrieved = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IOnNetworkAttributesRetrievedListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IOnNetworkAttributesRetrievedListener)) {
                return (IOnNetworkAttributesRetrievedListener) iin;
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
                            StatusParcelable _arg0 = (StatusParcelable) data.readTypedObject(StatusParcelable.CREATOR);
                            String _arg1 = data.readString();
                            NetworkAttributesParcelable _arg2 = (NetworkAttributesParcelable) data.readTypedObject(NetworkAttributesParcelable.CREATOR);
                            onNetworkAttributesRetrieved(_arg0, _arg1, _arg2);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IOnNetworkAttributesRetrievedListener {
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

            @Override // android.net.ipmemorystore.IOnNetworkAttributesRetrievedListener
            public void onNetworkAttributesRetrieved(StatusParcelable status, String l2Key, NetworkAttributesParcelable attributes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(status, 0);
                    _data.writeString(l2Key);
                    _data.writeTypedObject(attributes, 0);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onNetworkAttributesRetrieved is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ipmemorystore.IOnNetworkAttributesRetrievedListener
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

            @Override // android.net.ipmemorystore.IOnNetworkAttributesRetrievedListener
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
