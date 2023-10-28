package android.net.ipmemorystore;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IOnBlobRetrievedListener extends IInterface {
    public static final String DESCRIPTOR = "android$net$ipmemorystore$IOnBlobRetrievedListener".replace('$', '.');
    public static final String HASH = "d5ea5eb3ddbdaa9a986ce6ba70b0804ca3e39b0c";
    public static final int VERSION = 10;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void onBlobRetrieved(StatusParcelable statusParcelable, String str, String str2, Blob blob) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IOnBlobRetrievedListener {
        @Override // android.net.ipmemorystore.IOnBlobRetrievedListener
        public void onBlobRetrieved(StatusParcelable status, String l2Key, String name, Blob data) throws RemoteException {
        }

        @Override // android.net.ipmemorystore.IOnBlobRetrievedListener
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.net.ipmemorystore.IOnBlobRetrievedListener
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IOnBlobRetrievedListener {
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_onBlobRetrieved = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IOnBlobRetrievedListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IOnBlobRetrievedListener)) {
                return (IOnBlobRetrievedListener) iin;
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
                            String _arg2 = data.readString();
                            Blob _arg3 = (Blob) data.readTypedObject(Blob.CREATOR);
                            onBlobRetrieved(_arg0, _arg1, _arg2, _arg3);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IOnBlobRetrievedListener {
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

            @Override // android.net.ipmemorystore.IOnBlobRetrievedListener
            public void onBlobRetrieved(StatusParcelable status, String l2Key, String name, Blob data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(status, 0);
                    _data.writeString(l2Key);
                    _data.writeString(name);
                    _data.writeTypedObject(data, 0);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onBlobRetrieved is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ipmemorystore.IOnBlobRetrievedListener
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

            @Override // android.net.ipmemorystore.IOnBlobRetrievedListener
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
