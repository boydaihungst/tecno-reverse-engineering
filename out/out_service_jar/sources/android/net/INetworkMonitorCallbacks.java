package android.net;

import android.net.INetworkMonitor;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface INetworkMonitorCallbacks extends IInterface {
    public static final String DESCRIPTOR = "android$net$INetworkMonitorCallbacks".replace('$', '.');
    public static final String HASH = "c7a085b65072b36dc02239895cac021b6daee530";
    public static final int VERSION = 15;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void hideProvisioningNotification() throws RemoteException;

    void notifyCaptivePortalDataChanged(CaptivePortalData captivePortalData) throws RemoteException;

    void notifyDataStallSuspected(DataStallReportParcelable dataStallReportParcelable) throws RemoteException;

    void notifyNetworkTested(int i, String str) throws RemoteException;

    void notifyNetworkTestedWithExtras(NetworkTestResultParcelable networkTestResultParcelable) throws RemoteException;

    void notifyPrivateDnsConfigResolved(PrivateDnsConfigParcel privateDnsConfigParcel) throws RemoteException;

    void notifyProbeStatusChanged(int i, int i2) throws RemoteException;

    void onNetworkMonitorCreated(INetworkMonitor iNetworkMonitor) throws RemoteException;

    void showProvisioningNotification(String str, String str2) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements INetworkMonitorCallbacks {
        @Override // android.net.INetworkMonitorCallbacks
        public void onNetworkMonitorCreated(INetworkMonitor networkMonitor) throws RemoteException {
        }

        @Override // android.net.INetworkMonitorCallbacks
        public void notifyNetworkTested(int testResult, String redirectUrl) throws RemoteException {
        }

        @Override // android.net.INetworkMonitorCallbacks
        public void notifyPrivateDnsConfigResolved(PrivateDnsConfigParcel config) throws RemoteException {
        }

        @Override // android.net.INetworkMonitorCallbacks
        public void showProvisioningNotification(String action, String packageName) throws RemoteException {
        }

        @Override // android.net.INetworkMonitorCallbacks
        public void hideProvisioningNotification() throws RemoteException {
        }

        @Override // android.net.INetworkMonitorCallbacks
        public void notifyProbeStatusChanged(int probesCompleted, int probesSucceeded) throws RemoteException {
        }

        @Override // android.net.INetworkMonitorCallbacks
        public void notifyNetworkTestedWithExtras(NetworkTestResultParcelable result) throws RemoteException {
        }

        @Override // android.net.INetworkMonitorCallbacks
        public void notifyDataStallSuspected(DataStallReportParcelable report) throws RemoteException {
        }

        @Override // android.net.INetworkMonitorCallbacks
        public void notifyCaptivePortalDataChanged(CaptivePortalData data) throws RemoteException {
        }

        @Override // android.net.INetworkMonitorCallbacks
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.net.INetworkMonitorCallbacks
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements INetworkMonitorCallbacks {
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_hideProvisioningNotification = 5;
        static final int TRANSACTION_notifyCaptivePortalDataChanged = 9;
        static final int TRANSACTION_notifyDataStallSuspected = 8;
        static final int TRANSACTION_notifyNetworkTested = 2;
        static final int TRANSACTION_notifyNetworkTestedWithExtras = 7;
        static final int TRANSACTION_notifyPrivateDnsConfigResolved = 3;
        static final int TRANSACTION_notifyProbeStatusChanged = 6;
        static final int TRANSACTION_onNetworkMonitorCreated = 1;
        static final int TRANSACTION_showProvisioningNotification = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INetworkMonitorCallbacks asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof INetworkMonitorCallbacks)) {
                return (INetworkMonitorCallbacks) iin;
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
                            INetworkMonitor _arg0 = INetworkMonitor.Stub.asInterface(data.readStrongBinder());
                            onNetworkMonitorCreated(_arg0);
                            break;
                        case 2:
                            int _arg02 = data.readInt();
                            String _arg1 = data.readString();
                            notifyNetworkTested(_arg02, _arg1);
                            break;
                        case 3:
                            PrivateDnsConfigParcel _arg03 = (PrivateDnsConfigParcel) data.readTypedObject(PrivateDnsConfigParcel.CREATOR);
                            notifyPrivateDnsConfigResolved(_arg03);
                            break;
                        case 4:
                            String _arg04 = data.readString();
                            String _arg12 = data.readString();
                            showProvisioningNotification(_arg04, _arg12);
                            break;
                        case 5:
                            hideProvisioningNotification();
                            break;
                        case 6:
                            int _arg05 = data.readInt();
                            int _arg13 = data.readInt();
                            notifyProbeStatusChanged(_arg05, _arg13);
                            break;
                        case 7:
                            NetworkTestResultParcelable _arg06 = (NetworkTestResultParcelable) data.readTypedObject(NetworkTestResultParcelable.CREATOR);
                            notifyNetworkTestedWithExtras(_arg06);
                            break;
                        case 8:
                            DataStallReportParcelable _arg07 = (DataStallReportParcelable) data.readTypedObject(DataStallReportParcelable.CREATOR);
                            notifyDataStallSuspected(_arg07);
                            break;
                        case 9:
                            CaptivePortalData _arg08 = (CaptivePortalData) data.readTypedObject(CaptivePortalData.CREATOR);
                            notifyCaptivePortalDataChanged(_arg08);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements INetworkMonitorCallbacks {
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

            @Override // android.net.INetworkMonitorCallbacks
            public void onNetworkMonitorCreated(INetworkMonitor networkMonitor) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongInterface(networkMonitor);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onNetworkMonitorCreated is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitorCallbacks
            public void notifyNetworkTested(int testResult, String redirectUrl) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(testResult);
                    _data.writeString(redirectUrl);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyNetworkTested is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitorCallbacks
            public void notifyPrivateDnsConfigResolved(PrivateDnsConfigParcel config) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(config, 0);
                    boolean _status = this.mRemote.transact(3, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyPrivateDnsConfigResolved is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitorCallbacks
            public void showProvisioningNotification(String action, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(action);
                    _data.writeString(packageName);
                    boolean _status = this.mRemote.transact(4, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method showProvisioningNotification is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitorCallbacks
            public void hideProvisioningNotification() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(5, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method hideProvisioningNotification is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitorCallbacks
            public void notifyProbeStatusChanged(int probesCompleted, int probesSucceeded) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(probesCompleted);
                    _data.writeInt(probesSucceeded);
                    boolean _status = this.mRemote.transact(6, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyProbeStatusChanged is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitorCallbacks
            public void notifyNetworkTestedWithExtras(NetworkTestResultParcelable result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(result, 0);
                    boolean _status = this.mRemote.transact(7, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyNetworkTestedWithExtras is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitorCallbacks
            public void notifyDataStallSuspected(DataStallReportParcelable report) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(report, 0);
                    boolean _status = this.mRemote.transact(8, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyDataStallSuspected is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitorCallbacks
            public void notifyCaptivePortalDataChanged(CaptivePortalData data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(data, 0);
                    boolean _status = this.mRemote.transact(9, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyCaptivePortalDataChanged is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetworkMonitorCallbacks
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

            @Override // android.net.INetworkMonitorCallbacks
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
