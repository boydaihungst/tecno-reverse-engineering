package android.net;

import android.net.IIpMemoryStoreCallbacks;
import android.net.INetworkMonitorCallbacks;
import android.net.INetworkStackStatusCallback;
import android.net.dhcp.DhcpServingParamsParcel;
import android.net.dhcp.IDhcpServerCallbacks;
import android.net.ip.IIpClientCallbacks;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface INetworkStackConnector extends IInterface {
    public static final String DESCRIPTOR = "android$net$INetworkStackConnector".replace('$', '.');
    public static final String HASH = "c7a085b65072b36dc02239895cac021b6daee530";
    public static final int VERSION = 15;

    void allowTestUid(int i, INetworkStackStatusCallback iNetworkStackStatusCallback) throws RemoteException;

    void fetchIpMemoryStore(IIpMemoryStoreCallbacks iIpMemoryStoreCallbacks) throws RemoteException;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void makeDhcpServer(String str, DhcpServingParamsParcel dhcpServingParamsParcel, IDhcpServerCallbacks iDhcpServerCallbacks) throws RemoteException;

    void makeIpClient(String str, IIpClientCallbacks iIpClientCallbacks) throws RemoteException;

    void makeNetworkMonitor(Network network, String str, INetworkMonitorCallbacks iNetworkMonitorCallbacks) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements INetworkStackConnector {
        @Override // android.net.INetworkStackConnector
        public void makeDhcpServer(String ifName, DhcpServingParamsParcel params, IDhcpServerCallbacks cb) throws RemoteException {
        }

        @Override // android.net.INetworkStackConnector
        public void makeNetworkMonitor(Network network, String name, INetworkMonitorCallbacks cb) throws RemoteException {
        }

        @Override // android.net.INetworkStackConnector
        public void makeIpClient(String ifName, IIpClientCallbacks callbacks) throws RemoteException {
        }

        @Override // android.net.INetworkStackConnector
        public void fetchIpMemoryStore(IIpMemoryStoreCallbacks cb) throws RemoteException {
        }

        @Override // android.net.INetworkStackConnector
        public void allowTestUid(int uid, INetworkStackStatusCallback cb) throws RemoteException {
        }

        @Override // android.net.INetworkStackConnector
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.net.INetworkStackConnector
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements INetworkStackConnector {
        static final int TRANSACTION_allowTestUid = 5;
        static final int TRANSACTION_fetchIpMemoryStore = 4;
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_makeDhcpServer = 1;
        static final int TRANSACTION_makeIpClient = 3;
        static final int TRANSACTION_makeNetworkMonitor = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INetworkStackConnector asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof INetworkStackConnector)) {
                return (INetworkStackConnector) iin;
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
                            DhcpServingParamsParcel _arg1 = (DhcpServingParamsParcel) data.readTypedObject(DhcpServingParamsParcel.CREATOR);
                            IDhcpServerCallbacks _arg2 = IDhcpServerCallbacks.Stub.asInterface(data.readStrongBinder());
                            makeDhcpServer(_arg0, _arg1, _arg2);
                            break;
                        case 2:
                            Network _arg02 = (Network) data.readTypedObject(Network.CREATOR);
                            String _arg12 = data.readString();
                            INetworkMonitorCallbacks _arg22 = INetworkMonitorCallbacks.Stub.asInterface(data.readStrongBinder());
                            makeNetworkMonitor(_arg02, _arg12, _arg22);
                            break;
                        case 3:
                            String _arg03 = data.readString();
                            IIpClientCallbacks _arg13 = IIpClientCallbacks.Stub.asInterface(data.readStrongBinder());
                            makeIpClient(_arg03, _arg13);
                            break;
                        case 4:
                            IIpMemoryStoreCallbacks _arg04 = IIpMemoryStoreCallbacks.Stub.asInterface(data.readStrongBinder());
                            fetchIpMemoryStore(_arg04);
                            break;
                        case 5:
                            int _arg05 = data.readInt();
                            INetworkStackStatusCallback _arg14 = INetworkStackStatusCallback.Stub.asInterface(data.readStrongBinder());
                            allowTestUid(_arg05, _arg14);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements INetworkStackConnector {
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

            @Override // android.net.INetworkStackConnector
            public void makeDhcpServer(String ifName, DhcpServingParamsParcel params, IDhcpServerCallbacks cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeTypedObject(params, 0);
                    _data.writeStrongInterface(cb);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method makeDhcpServer is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkStackConnector
            public void makeNetworkMonitor(Network network, String name, INetworkMonitorCallbacks cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(network, 0);
                    _data.writeString(name);
                    _data.writeStrongInterface(cb);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method makeNetworkMonitor is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkStackConnector
            public void makeIpClient(String ifName, IIpClientCallbacks callbacks) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeStrongInterface(callbacks);
                    boolean _status = this.mRemote.transact(3, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method makeIpClient is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkStackConnector
            public void fetchIpMemoryStore(IIpMemoryStoreCallbacks cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongInterface(cb);
                    boolean _status = this.mRemote.transact(4, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method fetchIpMemoryStore is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkStackConnector
            public void allowTestUid(int uid, INetworkStackStatusCallback cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeStrongInterface(cb);
                    boolean _status = this.mRemote.transact(5, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method allowTestUid is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkStackConnector
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

            @Override // android.net.INetworkStackConnector
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
