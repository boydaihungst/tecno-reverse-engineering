package android.net.dhcp;

import android.net.IpPrefix;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;
/* loaded from: classes.dex */
public interface IDhcpEventCallbacks extends IInterface {
    public static final String DESCRIPTOR = "android$net$dhcp$IDhcpEventCallbacks".replace('$', '.');
    public static final String HASH = "c7a085b65072b36dc02239895cac021b6daee530";
    public static final int VERSION = 15;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void onLeasesChanged(List<DhcpLeaseParcelable> list) throws RemoteException;

    void onNewPrefixRequest(IpPrefix ipPrefix) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IDhcpEventCallbacks {
        @Override // android.net.dhcp.IDhcpEventCallbacks
        public void onLeasesChanged(List<DhcpLeaseParcelable> newLeases) throws RemoteException {
        }

        @Override // android.net.dhcp.IDhcpEventCallbacks
        public void onNewPrefixRequest(IpPrefix currentPrefix) throws RemoteException {
        }

        @Override // android.net.dhcp.IDhcpEventCallbacks
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.net.dhcp.IDhcpEventCallbacks
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IDhcpEventCallbacks {
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_onLeasesChanged = 1;
        static final int TRANSACTION_onNewPrefixRequest = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IDhcpEventCallbacks asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IDhcpEventCallbacks)) {
                return (IDhcpEventCallbacks) iin;
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
                            List<DhcpLeaseParcelable> _arg0 = data.createTypedArrayList(DhcpLeaseParcelable.CREATOR);
                            onLeasesChanged(_arg0);
                            break;
                        case 2:
                            IpPrefix _arg02 = (IpPrefix) data.readTypedObject(IpPrefix.CREATOR);
                            onNewPrefixRequest(_arg02);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IDhcpEventCallbacks {
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

            @Override // android.net.dhcp.IDhcpEventCallbacks
            public void onLeasesChanged(List<DhcpLeaseParcelable> newLeases) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedList(newLeases);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onLeasesChanged is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.dhcp.IDhcpEventCallbacks
            public void onNewPrefixRequest(IpPrefix currentPrefix) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(currentPrefix, 0);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onNewPrefixRequest is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.dhcp.IDhcpEventCallbacks
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

            @Override // android.net.dhcp.IDhcpEventCallbacks
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
