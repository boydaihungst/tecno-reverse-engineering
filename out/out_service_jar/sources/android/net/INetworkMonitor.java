package android.net;

import android.net.networkstack.aidl.NetworkMonitorParameters;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface INetworkMonitor extends IInterface {
    public static final String DESCRIPTOR = "android$net$INetworkMonitor".replace('$', '.');
    public static final String HASH = "c7a085b65072b36dc02239895cac021b6daee530";
    public static final int NETWORK_TEST_RESULT_INVALID = 1;
    public static final int NETWORK_TEST_RESULT_PARTIAL_CONNECTIVITY = 2;
    public static final int NETWORK_TEST_RESULT_VALID = 0;
    public static final int NETWORK_VALIDATION_PROBE_DNS = 4;
    public static final int NETWORK_VALIDATION_PROBE_FALLBACK = 32;
    public static final int NETWORK_VALIDATION_PROBE_HTTP = 8;
    public static final int NETWORK_VALIDATION_PROBE_HTTPS = 16;
    public static final int NETWORK_VALIDATION_PROBE_PRIVDNS = 64;
    public static final int NETWORK_VALIDATION_RESULT_PARTIAL = 2;
    public static final int NETWORK_VALIDATION_RESULT_SKIPPED = 4;
    public static final int NETWORK_VALIDATION_RESULT_VALID = 1;
    public static final int VERSION = 15;

    void forceReevaluation(int i) throws RemoteException;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void launchCaptivePortalApp() throws RemoteException;

    void notifyCaptivePortalAppFinished(int i) throws RemoteException;

    void notifyDnsResponse(int i) throws RemoteException;

    void notifyLinkPropertiesChanged(LinkProperties linkProperties) throws RemoteException;

    void notifyNetworkCapabilitiesChanged(NetworkCapabilities networkCapabilities) throws RemoteException;

    void notifyNetworkConnected(LinkProperties linkProperties, NetworkCapabilities networkCapabilities) throws RemoteException;

    void notifyNetworkConnectedParcel(NetworkMonitorParameters networkMonitorParameters) throws RemoteException;

    void notifyNetworkDisconnected() throws RemoteException;

    void notifyPrivateDnsChanged(PrivateDnsConfigParcel privateDnsConfigParcel) throws RemoteException;

    void setAcceptPartialConnectivity() throws RemoteException;

    void start() throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements INetworkMonitor {
        @Override // android.net.INetworkMonitor
        public void start() throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public void launchCaptivePortalApp() throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public void notifyCaptivePortalAppFinished(int response) throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public void setAcceptPartialConnectivity() throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public void forceReevaluation(int uid) throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public void notifyPrivateDnsChanged(PrivateDnsConfigParcel config) throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public void notifyDnsResponse(int returnCode) throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public void notifyNetworkConnected(LinkProperties lp, NetworkCapabilities nc) throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public void notifyNetworkDisconnected() throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public void notifyLinkPropertiesChanged(LinkProperties lp) throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public void notifyNetworkCapabilitiesChanged(NetworkCapabilities nc) throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public void notifyNetworkConnectedParcel(NetworkMonitorParameters params) throws RemoteException {
        }

        @Override // android.net.INetworkMonitor
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.net.INetworkMonitor
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements INetworkMonitor {
        static final int TRANSACTION_forceReevaluation = 5;
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_launchCaptivePortalApp = 2;
        static final int TRANSACTION_notifyCaptivePortalAppFinished = 3;
        static final int TRANSACTION_notifyDnsResponse = 7;
        static final int TRANSACTION_notifyLinkPropertiesChanged = 10;
        static final int TRANSACTION_notifyNetworkCapabilitiesChanged = 11;
        static final int TRANSACTION_notifyNetworkConnected = 8;
        static final int TRANSACTION_notifyNetworkConnectedParcel = 12;
        static final int TRANSACTION_notifyNetworkDisconnected = 9;
        static final int TRANSACTION_notifyPrivateDnsChanged = 6;
        static final int TRANSACTION_setAcceptPartialConnectivity = 4;
        static final int TRANSACTION_start = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INetworkMonitor asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof INetworkMonitor)) {
                return (INetworkMonitor) iin;
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
                            start();
                            break;
                        case 2:
                            launchCaptivePortalApp();
                            break;
                        case 3:
                            int _arg0 = data.readInt();
                            notifyCaptivePortalAppFinished(_arg0);
                            break;
                        case 4:
                            setAcceptPartialConnectivity();
                            break;
                        case 5:
                            int _arg02 = data.readInt();
                            forceReevaluation(_arg02);
                            break;
                        case 6:
                            PrivateDnsConfigParcel _arg03 = (PrivateDnsConfigParcel) data.readTypedObject(PrivateDnsConfigParcel.CREATOR);
                            notifyPrivateDnsChanged(_arg03);
                            break;
                        case 7:
                            int _arg04 = data.readInt();
                            notifyDnsResponse(_arg04);
                            break;
                        case 8:
                            LinkProperties _arg05 = (LinkProperties) data.readTypedObject(LinkProperties.CREATOR);
                            NetworkCapabilities _arg1 = (NetworkCapabilities) data.readTypedObject(NetworkCapabilities.CREATOR);
                            notifyNetworkConnected(_arg05, _arg1);
                            break;
                        case 9:
                            notifyNetworkDisconnected();
                            break;
                        case 10:
                            LinkProperties _arg06 = (LinkProperties) data.readTypedObject(LinkProperties.CREATOR);
                            notifyLinkPropertiesChanged(_arg06);
                            break;
                        case 11:
                            NetworkCapabilities _arg07 = (NetworkCapabilities) data.readTypedObject(NetworkCapabilities.CREATOR);
                            notifyNetworkCapabilitiesChanged(_arg07);
                            break;
                        case 12:
                            NetworkMonitorParameters _arg08 = (NetworkMonitorParameters) data.readTypedObject(NetworkMonitorParameters.CREATOR);
                            notifyNetworkConnectedParcel(_arg08);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements INetworkMonitor {
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

            @Override // android.net.INetworkMonitor
            public void start() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method start is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
            public void launchCaptivePortalApp() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method launchCaptivePortalApp is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
            public void notifyCaptivePortalAppFinished(int response) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(response);
                    boolean _status = this.mRemote.transact(3, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyCaptivePortalAppFinished is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
            public void setAcceptPartialConnectivity() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(4, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method setAcceptPartialConnectivity is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
            public void forceReevaluation(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    boolean _status = this.mRemote.transact(5, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method forceReevaluation is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
            public void notifyPrivateDnsChanged(PrivateDnsConfigParcel config) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(config, 0);
                    boolean _status = this.mRemote.transact(6, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyPrivateDnsChanged is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
            public void notifyDnsResponse(int returnCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(returnCode);
                    boolean _status = this.mRemote.transact(7, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyDnsResponse is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
            public void notifyNetworkConnected(LinkProperties lp, NetworkCapabilities nc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(lp, 0);
                    _data.writeTypedObject(nc, 0);
                    boolean _status = this.mRemote.transact(8, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyNetworkConnected is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
            public void notifyNetworkDisconnected() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(9, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyNetworkDisconnected is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
            public void notifyLinkPropertiesChanged(LinkProperties lp) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(lp, 0);
                    boolean _status = this.mRemote.transact(10, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyLinkPropertiesChanged is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
            public void notifyNetworkCapabilitiesChanged(NetworkCapabilities nc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(nc, 0);
                    boolean _status = this.mRemote.transact(11, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyNetworkCapabilitiesChanged is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
            public void notifyNetworkConnectedParcel(NetworkMonitorParameters params) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(params, 0);
                    boolean _status = this.mRemote.transact(12, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyNetworkConnectedParcel is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitor
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

            @Override // android.net.INetworkMonitor
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
