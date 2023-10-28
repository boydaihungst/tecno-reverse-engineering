package android.net.dhcp;

import android.net.INetworkStackStatusCallback;
import android.net.dhcp.IDhcpEventCallbacks;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IDhcpServer extends IInterface {
    public static final String DESCRIPTOR = "android$net$dhcp$IDhcpServer".replace('$', '.');
    public static final String HASH = "c7a085b65072b36dc02239895cac021b6daee530";
    public static final int STATUS_INVALID_ARGUMENT = 2;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_UNKNOWN = 0;
    public static final int STATUS_UNKNOWN_ERROR = 3;
    public static final int VERSION = 15;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void start(INetworkStackStatusCallback iNetworkStackStatusCallback) throws RemoteException;

    void startWithCallbacks(INetworkStackStatusCallback iNetworkStackStatusCallback, IDhcpEventCallbacks iDhcpEventCallbacks) throws RemoteException;

    void stop(INetworkStackStatusCallback iNetworkStackStatusCallback) throws RemoteException;

    void updateParams(DhcpServingParamsParcel dhcpServingParamsParcel, INetworkStackStatusCallback iNetworkStackStatusCallback) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IDhcpServer {
        @Override // android.net.dhcp.IDhcpServer
        public void start(INetworkStackStatusCallback cb) throws RemoteException {
        }

        @Override // android.net.dhcp.IDhcpServer
        public void startWithCallbacks(INetworkStackStatusCallback statusCb, IDhcpEventCallbacks eventCb) throws RemoteException {
        }

        @Override // android.net.dhcp.IDhcpServer
        public void updateParams(DhcpServingParamsParcel params, INetworkStackStatusCallback cb) throws RemoteException {
        }

        @Override // android.net.dhcp.IDhcpServer
        public void stop(INetworkStackStatusCallback cb) throws RemoteException {
        }

        @Override // android.net.dhcp.IDhcpServer
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.net.dhcp.IDhcpServer
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IDhcpServer {
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_start = 1;
        static final int TRANSACTION_startWithCallbacks = 4;
        static final int TRANSACTION_stop = 3;
        static final int TRANSACTION_updateParams = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IDhcpServer asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IDhcpServer)) {
                return (IDhcpServer) iin;
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
                            INetworkStackStatusCallback _arg0 = INetworkStackStatusCallback.Stub.asInterface(data.readStrongBinder());
                            start(_arg0);
                            break;
                        case 2:
                            DhcpServingParamsParcel _arg02 = (DhcpServingParamsParcel) data.readTypedObject(DhcpServingParamsParcel.CREATOR);
                            INetworkStackStatusCallback _arg1 = INetworkStackStatusCallback.Stub.asInterface(data.readStrongBinder());
                            updateParams(_arg02, _arg1);
                            break;
                        case 3:
                            INetworkStackStatusCallback _arg03 = INetworkStackStatusCallback.Stub.asInterface(data.readStrongBinder());
                            stop(_arg03);
                            break;
                        case 4:
                            INetworkStackStatusCallback _arg04 = INetworkStackStatusCallback.Stub.asInterface(data.readStrongBinder());
                            IDhcpEventCallbacks _arg12 = IDhcpEventCallbacks.Stub.asInterface(data.readStrongBinder());
                            startWithCallbacks(_arg04, _arg12);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IDhcpServer {
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

            @Override // android.net.dhcp.IDhcpServer
            public void start(INetworkStackStatusCallback cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongInterface(cb);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method start is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.dhcp.IDhcpServer
            public void startWithCallbacks(INetworkStackStatusCallback statusCb, IDhcpEventCallbacks eventCb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongInterface(statusCb);
                    _data.writeStrongInterface(eventCb);
                    boolean _status = this.mRemote.transact(4, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method startWithCallbacks is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.dhcp.IDhcpServer
            public void updateParams(DhcpServingParamsParcel params, INetworkStackStatusCallback cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(params, 0);
                    _data.writeStrongInterface(cb);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method updateParams is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.dhcp.IDhcpServer
            public void stop(INetworkStackStatusCallback cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongInterface(cb);
                    boolean _status = this.mRemote.transact(3, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method stop is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.dhcp.IDhcpServer
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

            @Override // android.net.dhcp.IDhcpServer
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
