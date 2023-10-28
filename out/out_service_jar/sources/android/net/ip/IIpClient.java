package android.net.ip;

import android.net.Layer2InformationParcelable;
import android.net.NattKeepalivePacketDataParcelable;
import android.net.ProvisioningConfigurationParcelable;
import android.net.ProxyInfo;
import android.net.TcpKeepalivePacketDataParcelable;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IIpClient extends IInterface {
    public static final String DESCRIPTOR = "android$net$ip$IIpClient".replace('$', '.');
    public static final String HASH = "c7a085b65072b36dc02239895cac021b6daee530";
    public static final int PROV_IPV4_DHCP = 2;
    public static final int PROV_IPV4_DISABLED = 0;
    public static final int PROV_IPV4_STATIC = 1;
    public static final int PROV_IPV6_DISABLED = 0;
    public static final int PROV_IPV6_LINKLOCAL = 2;
    public static final int PROV_IPV6_SLAAC = 1;
    public static final int VERSION = 15;

    void addKeepalivePacketFilter(int i, TcpKeepalivePacketDataParcelable tcpKeepalivePacketDataParcelable) throws RemoteException;

    void addNattKeepalivePacketFilter(int i, NattKeepalivePacketDataParcelable nattKeepalivePacketDataParcelable) throws RemoteException;

    void completedPreDhcpAction() throws RemoteException;

    void confirmConfiguration() throws RemoteException;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void notifyPreconnectionComplete(boolean z) throws RemoteException;

    void readPacketFilterComplete(byte[] bArr) throws RemoteException;

    void removeKeepalivePacketFilter(int i) throws RemoteException;

    void setHttpProxy(ProxyInfo proxyInfo) throws RemoteException;

    void setL2KeyAndGroupHint(String str, String str2) throws RemoteException;

    void setMulticastFilter(boolean z) throws RemoteException;

    void setTcpBufferSizes(String str) throws RemoteException;

    void shutdown() throws RemoteException;

    void startProvisioning(ProvisioningConfigurationParcelable provisioningConfigurationParcelable) throws RemoteException;

    void stop() throws RemoteException;

    void updateLayer2Information(Layer2InformationParcelable layer2InformationParcelable) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IIpClient {
        @Override // android.net.ip.IIpClient
        public void completedPreDhcpAction() throws RemoteException {
        }

        @Override // android.net.ip.IIpClient
        public void confirmConfiguration() throws RemoteException {
        }

        @Override // android.net.ip.IIpClient
        public void readPacketFilterComplete(byte[] data) throws RemoteException {
        }

        @Override // android.net.ip.IIpClient
        public void shutdown() throws RemoteException {
        }

        @Override // android.net.ip.IIpClient
        public void startProvisioning(ProvisioningConfigurationParcelable req) throws RemoteException {
        }

        @Override // android.net.ip.IIpClient
        public void stop() throws RemoteException {
        }

        @Override // android.net.ip.IIpClient
        public void setTcpBufferSizes(String tcpBufferSizes) throws RemoteException {
        }

        @Override // android.net.ip.IIpClient
        public void setHttpProxy(ProxyInfo proxyInfo) throws RemoteException {
        }

        @Override // android.net.ip.IIpClient
        public void setMulticastFilter(boolean enabled) throws RemoteException {
        }

        @Override // android.net.ip.IIpClient
        public void addKeepalivePacketFilter(int slot, TcpKeepalivePacketDataParcelable pkt) throws RemoteException {
        }

        @Override // android.net.ip.IIpClient
        public void removeKeepalivePacketFilter(int slot) throws RemoteException {
        }

        @Override // android.net.ip.IIpClient
        public void setL2KeyAndGroupHint(String l2Key, String cluster) throws RemoteException {
        }

        @Override // android.net.ip.IIpClient
        public void addNattKeepalivePacketFilter(int slot, NattKeepalivePacketDataParcelable pkt) throws RemoteException {
        }

        @Override // android.net.ip.IIpClient
        public void notifyPreconnectionComplete(boolean success) throws RemoteException {
        }

        @Override // android.net.ip.IIpClient
        public void updateLayer2Information(Layer2InformationParcelable info) throws RemoteException {
        }

        @Override // android.net.ip.IIpClient
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.net.ip.IIpClient
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IIpClient {
        static final int TRANSACTION_addKeepalivePacketFilter = 10;
        static final int TRANSACTION_addNattKeepalivePacketFilter = 13;
        static final int TRANSACTION_completedPreDhcpAction = 1;
        static final int TRANSACTION_confirmConfiguration = 2;
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_notifyPreconnectionComplete = 14;
        static final int TRANSACTION_readPacketFilterComplete = 3;
        static final int TRANSACTION_removeKeepalivePacketFilter = 11;
        static final int TRANSACTION_setHttpProxy = 8;
        static final int TRANSACTION_setL2KeyAndGroupHint = 12;
        static final int TRANSACTION_setMulticastFilter = 9;
        static final int TRANSACTION_setTcpBufferSizes = 7;
        static final int TRANSACTION_shutdown = 4;
        static final int TRANSACTION_startProvisioning = 5;
        static final int TRANSACTION_stop = 6;
        static final int TRANSACTION_updateLayer2Information = 15;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IIpClient asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IIpClient)) {
                return (IIpClient) iin;
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
                            completedPreDhcpAction();
                            break;
                        case 2:
                            confirmConfiguration();
                            break;
                        case 3:
                            byte[] _arg0 = data.createByteArray();
                            readPacketFilterComplete(_arg0);
                            break;
                        case 4:
                            shutdown();
                            break;
                        case 5:
                            ProvisioningConfigurationParcelable _arg02 = (ProvisioningConfigurationParcelable) data.readTypedObject(ProvisioningConfigurationParcelable.CREATOR);
                            startProvisioning(_arg02);
                            break;
                        case 6:
                            stop();
                            break;
                        case 7:
                            String _arg03 = data.readString();
                            setTcpBufferSizes(_arg03);
                            break;
                        case 8:
                            ProxyInfo _arg04 = (ProxyInfo) data.readTypedObject(ProxyInfo.CREATOR);
                            setHttpProxy(_arg04);
                            break;
                        case 9:
                            boolean _arg05 = data.readBoolean();
                            setMulticastFilter(_arg05);
                            break;
                        case 10:
                            int _arg06 = data.readInt();
                            TcpKeepalivePacketDataParcelable _arg1 = (TcpKeepalivePacketDataParcelable) data.readTypedObject(TcpKeepalivePacketDataParcelable.CREATOR);
                            addKeepalivePacketFilter(_arg06, _arg1);
                            break;
                        case 11:
                            int _arg07 = data.readInt();
                            removeKeepalivePacketFilter(_arg07);
                            break;
                        case 12:
                            String _arg08 = data.readString();
                            String _arg12 = data.readString();
                            setL2KeyAndGroupHint(_arg08, _arg12);
                            break;
                        case 13:
                            int _arg09 = data.readInt();
                            NattKeepalivePacketDataParcelable _arg13 = (NattKeepalivePacketDataParcelable) data.readTypedObject(NattKeepalivePacketDataParcelable.CREATOR);
                            addNattKeepalivePacketFilter(_arg09, _arg13);
                            break;
                        case 14:
                            boolean _arg010 = data.readBoolean();
                            notifyPreconnectionComplete(_arg010);
                            break;
                        case 15:
                            Layer2InformationParcelable _arg011 = (Layer2InformationParcelable) data.readTypedObject(Layer2InformationParcelable.CREATOR);
                            updateLayer2Information(_arg011);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IIpClient {
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

            @Override // android.net.ip.IIpClient
            public void completedPreDhcpAction() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method completedPreDhcpAction is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClient
            public void confirmConfiguration() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method confirmConfiguration is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClient
            public void readPacketFilterComplete(byte[] data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeByteArray(data);
                    boolean _status = this.mRemote.transact(3, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method readPacketFilterComplete is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClient
            public void shutdown() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(4, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method shutdown is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClient
            public void startProvisioning(ProvisioningConfigurationParcelable req) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(req, 0);
                    boolean _status = this.mRemote.transact(5, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method startProvisioning is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClient
            public void stop() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(6, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method stop is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClient
            public void setTcpBufferSizes(String tcpBufferSizes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(tcpBufferSizes);
                    boolean _status = this.mRemote.transact(7, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method setTcpBufferSizes is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClient
            public void setHttpProxy(ProxyInfo proxyInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(proxyInfo, 0);
                    boolean _status = this.mRemote.transact(8, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method setHttpProxy is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClient
            public void setMulticastFilter(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    boolean _status = this.mRemote.transact(9, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method setMulticastFilter is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClient
            public void addKeepalivePacketFilter(int slot, TcpKeepalivePacketDataParcelable pkt) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(slot);
                    _data.writeTypedObject(pkt, 0);
                    boolean _status = this.mRemote.transact(10, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method addKeepalivePacketFilter is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClient
            public void removeKeepalivePacketFilter(int slot) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(slot);
                    boolean _status = this.mRemote.transact(11, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method removeKeepalivePacketFilter is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClient
            public void setL2KeyAndGroupHint(String l2Key, String cluster) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(l2Key);
                    _data.writeString(cluster);
                    boolean _status = this.mRemote.transact(12, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method setL2KeyAndGroupHint is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClient
            public void addNattKeepalivePacketFilter(int slot, NattKeepalivePacketDataParcelable pkt) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(slot);
                    _data.writeTypedObject(pkt, 0);
                    boolean _status = this.mRemote.transact(13, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method addNattKeepalivePacketFilter is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClient
            public void notifyPreconnectionComplete(boolean success) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(success);
                    boolean _status = this.mRemote.transact(14, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyPreconnectionComplete is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClient
            public void updateLayer2Information(Layer2InformationParcelable info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(info, 0);
                    boolean _status = this.mRemote.transact(15, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method updateLayer2Information is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClient
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

            @Override // android.net.ip.IIpClient
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
