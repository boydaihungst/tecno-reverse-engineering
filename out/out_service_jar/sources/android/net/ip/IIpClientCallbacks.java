package android.net.ip;

import android.net.DhcpResultsParcelable;
import android.net.Layer2PacketParcelable;
import android.net.LinkProperties;
import android.net.ip.IIpClient;
import android.net.networkstack.aidl.ip.ReachabilityLossInfoParcelable;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;
/* loaded from: classes.dex */
public interface IIpClientCallbacks extends IInterface {
    public static final String DESCRIPTOR = "android$net$ip$IIpClientCallbacks".replace('$', '.');
    public static final String HASH = "c7a085b65072b36dc02239895cac021b6daee530";
    public static final int VERSION = 15;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void installPacketFilter(byte[] bArr) throws RemoteException;

    void onIpClientCreated(IIpClient iIpClient) throws RemoteException;

    void onLinkPropertiesChange(LinkProperties linkProperties) throws RemoteException;

    void onNewDhcpResults(DhcpResultsParcelable dhcpResultsParcelable) throws RemoteException;

    void onPostDhcpAction() throws RemoteException;

    void onPreDhcpAction() throws RemoteException;

    void onPreconnectionStart(List<Layer2PacketParcelable> list) throws RemoteException;

    void onProvisioningFailure(LinkProperties linkProperties) throws RemoteException;

    void onProvisioningSuccess(LinkProperties linkProperties) throws RemoteException;

    void onQuit() throws RemoteException;

    void onReachabilityFailure(ReachabilityLossInfoParcelable reachabilityLossInfoParcelable) throws RemoteException;

    void onReachabilityLost(String str) throws RemoteException;

    void setFallbackMulticastFilter(boolean z) throws RemoteException;

    void setNeighborDiscoveryOffload(boolean z) throws RemoteException;

    void startReadPacketFilter() throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IIpClientCallbacks {
        @Override // android.net.ip.IIpClientCallbacks
        public void onIpClientCreated(IIpClient ipClient) throws RemoteException {
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void onPreDhcpAction() throws RemoteException {
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void onPostDhcpAction() throws RemoteException {
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void onNewDhcpResults(DhcpResultsParcelable dhcpResults) throws RemoteException {
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void onProvisioningSuccess(LinkProperties newLp) throws RemoteException {
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void onProvisioningFailure(LinkProperties newLp) throws RemoteException {
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void onLinkPropertiesChange(LinkProperties newLp) throws RemoteException {
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void onReachabilityLost(String logMsg) throws RemoteException {
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void onQuit() throws RemoteException {
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void installPacketFilter(byte[] filter) throws RemoteException {
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void startReadPacketFilter() throws RemoteException {
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void setFallbackMulticastFilter(boolean enabled) throws RemoteException {
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void setNeighborDiscoveryOffload(boolean enable) throws RemoteException {
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void onPreconnectionStart(List<Layer2PacketParcelable> packets) throws RemoteException {
        }

        @Override // android.net.ip.IIpClientCallbacks
        public void onReachabilityFailure(ReachabilityLossInfoParcelable lossInfo) throws RemoteException {
        }

        @Override // android.net.ip.IIpClientCallbacks
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.net.ip.IIpClientCallbacks
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IIpClientCallbacks {
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_installPacketFilter = 10;
        static final int TRANSACTION_onIpClientCreated = 1;
        static final int TRANSACTION_onLinkPropertiesChange = 7;
        static final int TRANSACTION_onNewDhcpResults = 4;
        static final int TRANSACTION_onPostDhcpAction = 3;
        static final int TRANSACTION_onPreDhcpAction = 2;
        static final int TRANSACTION_onPreconnectionStart = 14;
        static final int TRANSACTION_onProvisioningFailure = 6;
        static final int TRANSACTION_onProvisioningSuccess = 5;
        static final int TRANSACTION_onQuit = 9;
        static final int TRANSACTION_onReachabilityFailure = 15;
        static final int TRANSACTION_onReachabilityLost = 8;
        static final int TRANSACTION_setFallbackMulticastFilter = 12;
        static final int TRANSACTION_setNeighborDiscoveryOffload = 13;
        static final int TRANSACTION_startReadPacketFilter = 11;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IIpClientCallbacks asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IIpClientCallbacks)) {
                return (IIpClientCallbacks) iin;
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
                            IIpClient _arg0 = IIpClient.Stub.asInterface(data.readStrongBinder());
                            onIpClientCreated(_arg0);
                            break;
                        case 2:
                            onPreDhcpAction();
                            break;
                        case 3:
                            onPostDhcpAction();
                            break;
                        case 4:
                            DhcpResultsParcelable _arg02 = (DhcpResultsParcelable) data.readTypedObject(DhcpResultsParcelable.CREATOR);
                            onNewDhcpResults(_arg02);
                            break;
                        case 5:
                            LinkProperties _arg03 = (LinkProperties) data.readTypedObject(LinkProperties.CREATOR);
                            onProvisioningSuccess(_arg03);
                            break;
                        case 6:
                            LinkProperties _arg04 = (LinkProperties) data.readTypedObject(LinkProperties.CREATOR);
                            onProvisioningFailure(_arg04);
                            break;
                        case 7:
                            LinkProperties _arg05 = (LinkProperties) data.readTypedObject(LinkProperties.CREATOR);
                            onLinkPropertiesChange(_arg05);
                            break;
                        case 8:
                            String _arg06 = data.readString();
                            onReachabilityLost(_arg06);
                            break;
                        case 9:
                            onQuit();
                            break;
                        case 10:
                            byte[] _arg07 = data.createByteArray();
                            installPacketFilter(_arg07);
                            break;
                        case 11:
                            startReadPacketFilter();
                            break;
                        case 12:
                            boolean _arg08 = data.readBoolean();
                            setFallbackMulticastFilter(_arg08);
                            break;
                        case 13:
                            boolean _arg09 = data.readBoolean();
                            setNeighborDiscoveryOffload(_arg09);
                            break;
                        case 14:
                            List<Layer2PacketParcelable> _arg010 = data.createTypedArrayList(Layer2PacketParcelable.CREATOR);
                            onPreconnectionStart(_arg010);
                            break;
                        case 15:
                            ReachabilityLossInfoParcelable _arg011 = (ReachabilityLossInfoParcelable) data.readTypedObject(ReachabilityLossInfoParcelable.CREATOR);
                            onReachabilityFailure(_arg011);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IIpClientCallbacks {
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

            @Override // android.net.ip.IIpClientCallbacks
            public void onIpClientCreated(IIpClient ipClient) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongInterface(ipClient);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onIpClientCreated is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClientCallbacks
            public void onPreDhcpAction() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onPreDhcpAction is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClientCallbacks
            public void onPostDhcpAction() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(3, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onPostDhcpAction is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClientCallbacks
            public void onNewDhcpResults(DhcpResultsParcelable dhcpResults) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(dhcpResults, 0);
                    boolean _status = this.mRemote.transact(4, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onNewDhcpResults is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClientCallbacks
            public void onProvisioningSuccess(LinkProperties newLp) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(newLp, 0);
                    boolean _status = this.mRemote.transact(5, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onProvisioningSuccess is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClientCallbacks
            public void onProvisioningFailure(LinkProperties newLp) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(newLp, 0);
                    boolean _status = this.mRemote.transact(6, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onProvisioningFailure is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClientCallbacks
            public void onLinkPropertiesChange(LinkProperties newLp) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(newLp, 0);
                    boolean _status = this.mRemote.transact(7, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onLinkPropertiesChange is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClientCallbacks
            public void onReachabilityLost(String logMsg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(logMsg);
                    boolean _status = this.mRemote.transact(8, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onReachabilityLost is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClientCallbacks
            public void onQuit() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(9, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onQuit is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClientCallbacks
            public void installPacketFilter(byte[] filter) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeByteArray(filter);
                    boolean _status = this.mRemote.transact(10, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method installPacketFilter is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClientCallbacks
            public void startReadPacketFilter() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(11, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method startReadPacketFilter is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClientCallbacks
            public void setFallbackMulticastFilter(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    boolean _status = this.mRemote.transact(12, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method setFallbackMulticastFilter is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClientCallbacks
            public void setNeighborDiscoveryOffload(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(enable);
                    boolean _status = this.mRemote.transact(13, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method setNeighborDiscoveryOffload is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClientCallbacks
            public void onPreconnectionStart(List<Layer2PacketParcelable> packets) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedList(packets);
                    boolean _status = this.mRemote.transact(14, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onPreconnectionStart is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClientCallbacks
            public void onReachabilityFailure(ReachabilityLossInfoParcelable lossInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(lossInfo, 0);
                    boolean _status = this.mRemote.transact(15, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onReachabilityFailure is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.ip.IIpClientCallbacks
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

            @Override // android.net.ip.IIpClientCallbacks
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
