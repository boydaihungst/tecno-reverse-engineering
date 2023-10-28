package android.net.lowpan;

import android.net.IpPrefix;
import android.net.lowpan.ILowpanEnergyScanCallback;
import android.net.lowpan.ILowpanInterfaceListener;
import android.net.lowpan.ILowpanNetScanCallback;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.Map;
/* loaded from: classes2.dex */
public interface ILowpanInterface extends IInterface {
    public static final String DESCRIPTOR = "android.net.lowpan.ILowpanInterface";
    public static final int ERROR_ALREADY = 9;
    public static final int ERROR_BUSY = 8;
    public static final int ERROR_CANCELED = 10;
    public static final int ERROR_DISABLED = 3;
    public static final int ERROR_FEATURE_NOT_SUPPORTED = 11;
    public static final int ERROR_FORM_FAILED_AT_SCAN = 15;
    public static final int ERROR_INVALID_ARGUMENT = 2;
    public static final int ERROR_IO_FAILURE = 6;
    public static final int ERROR_JOIN_FAILED_AT_AUTH = 14;
    public static final int ERROR_JOIN_FAILED_AT_SCAN = 13;
    public static final int ERROR_JOIN_FAILED_UNKNOWN = 12;
    public static final int ERROR_NCP_PROBLEM = 7;
    public static final int ERROR_TIMEOUT = 5;
    public static final int ERROR_UNSPECIFIED = 1;
    public static final int ERROR_WRONG_STATE = 4;
    public static final String KEY_CHANNEL_MASK = "android.net.lowpan.property.CHANNEL_MASK";
    public static final String KEY_MAX_TX_POWER = "android.net.lowpan.property.MAX_TX_POWER";
    public static final String NETWORK_TYPE_THREAD_V1 = "org.threadgroup.thread.v1";
    public static final String NETWORK_TYPE_UNKNOWN = "unknown";
    public static final String PERM_ACCESS_LOWPAN_STATE = "android.permission.ACCESS_LOWPAN_STATE";
    public static final String PERM_CHANGE_LOWPAN_STATE = "android.permission.CHANGE_LOWPAN_STATE";
    public static final String PERM_READ_LOWPAN_CREDENTIAL = "android.permission.READ_LOWPAN_CREDENTIAL";
    public static final String ROLE_COORDINATOR = "coordinator";
    public static final String ROLE_DETACHED = "detached";
    public static final String ROLE_END_DEVICE = "end-device";
    public static final String ROLE_LEADER = "leader";
    public static final String ROLE_ROUTER = "router";
    public static final String ROLE_SLEEPY_END_DEVICE = "sleepy-end-device";
    public static final String ROLE_SLEEPY_ROUTER = "sleepy-router";
    public static final String STATE_ATTACHED = "attached";
    public static final String STATE_ATTACHING = "attaching";
    public static final String STATE_COMMISSIONING = "commissioning";
    public static final String STATE_FAULT = "fault";
    public static final String STATE_OFFLINE = "offline";

    void addExternalRoute(IpPrefix ipPrefix, int i) throws RemoteException;

    void addListener(ILowpanInterfaceListener iLowpanInterfaceListener) throws RemoteException;

    void addOnMeshPrefix(IpPrefix ipPrefix, int i) throws RemoteException;

    void attach(LowpanProvision lowpanProvision) throws RemoteException;

    void beginLowPower() throws RemoteException;

    void closeCommissioningSession() throws RemoteException;

    void form(LowpanProvision lowpanProvision) throws RemoteException;

    String getDriverVersion() throws RemoteException;

    byte[] getExtendedAddress() throws RemoteException;

    String[] getLinkAddresses() throws RemoteException;

    IpPrefix[] getLinkNetworks() throws RemoteException;

    LowpanCredential getLowpanCredential() throws RemoteException;

    LowpanIdentity getLowpanIdentity() throws RemoteException;

    byte[] getMacAddress() throws RemoteException;

    String getName() throws RemoteException;

    String getNcpVersion() throws RemoteException;

    String getPartitionId() throws RemoteException;

    String getRole() throws RemoteException;

    String getState() throws RemoteException;

    LowpanChannelInfo[] getSupportedChannels() throws RemoteException;

    String[] getSupportedNetworkTypes() throws RemoteException;

    boolean isCommissioned() throws RemoteException;

    boolean isConnected() throws RemoteException;

    boolean isEnabled() throws RemoteException;

    boolean isUp() throws RemoteException;

    void join(LowpanProvision lowpanProvision) throws RemoteException;

    void leave() throws RemoteException;

    void onHostWake() throws RemoteException;

    void pollForData() throws RemoteException;

    void removeExternalRoute(IpPrefix ipPrefix) throws RemoteException;

    void removeListener(ILowpanInterfaceListener iLowpanInterfaceListener) throws RemoteException;

    void removeOnMeshPrefix(IpPrefix ipPrefix) throws RemoteException;

    void reset() throws RemoteException;

    void sendToCommissioner(byte[] bArr) throws RemoteException;

    void setEnabled(boolean z) throws RemoteException;

    void startCommissioningSession(LowpanBeaconInfo lowpanBeaconInfo) throws RemoteException;

    void startEnergyScan(Map map, ILowpanEnergyScanCallback iLowpanEnergyScanCallback) throws RemoteException;

    void startNetScan(Map map, ILowpanNetScanCallback iLowpanNetScanCallback) throws RemoteException;

    void stopEnergyScan() throws RemoteException;

    void stopNetScan() throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements ILowpanInterface {
        @Override // android.net.lowpan.ILowpanInterface
        public String getName() throws RemoteException {
            return null;
        }

        @Override // android.net.lowpan.ILowpanInterface
        public String getNcpVersion() throws RemoteException {
            return null;
        }

        @Override // android.net.lowpan.ILowpanInterface
        public String getDriverVersion() throws RemoteException {
            return null;
        }

        @Override // android.net.lowpan.ILowpanInterface
        public LowpanChannelInfo[] getSupportedChannels() throws RemoteException {
            return null;
        }

        @Override // android.net.lowpan.ILowpanInterface
        public String[] getSupportedNetworkTypes() throws RemoteException {
            return null;
        }

        @Override // android.net.lowpan.ILowpanInterface
        public byte[] getMacAddress() throws RemoteException {
            return null;
        }

        @Override // android.net.lowpan.ILowpanInterface
        public boolean isEnabled() throws RemoteException {
            return false;
        }

        @Override // android.net.lowpan.ILowpanInterface
        public void setEnabled(boolean enabled) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterface
        public boolean isUp() throws RemoteException {
            return false;
        }

        @Override // android.net.lowpan.ILowpanInterface
        public boolean isCommissioned() throws RemoteException {
            return false;
        }

        @Override // android.net.lowpan.ILowpanInterface
        public boolean isConnected() throws RemoteException {
            return false;
        }

        @Override // android.net.lowpan.ILowpanInterface
        public String getState() throws RemoteException {
            return null;
        }

        @Override // android.net.lowpan.ILowpanInterface
        public String getRole() throws RemoteException {
            return null;
        }

        @Override // android.net.lowpan.ILowpanInterface
        public String getPartitionId() throws RemoteException {
            return null;
        }

        @Override // android.net.lowpan.ILowpanInterface
        public byte[] getExtendedAddress() throws RemoteException {
            return null;
        }

        @Override // android.net.lowpan.ILowpanInterface
        public LowpanIdentity getLowpanIdentity() throws RemoteException {
            return null;
        }

        @Override // android.net.lowpan.ILowpanInterface
        public LowpanCredential getLowpanCredential() throws RemoteException {
            return null;
        }

        @Override // android.net.lowpan.ILowpanInterface
        public String[] getLinkAddresses() throws RemoteException {
            return null;
        }

        @Override // android.net.lowpan.ILowpanInterface
        public IpPrefix[] getLinkNetworks() throws RemoteException {
            return null;
        }

        @Override // android.net.lowpan.ILowpanInterface
        public void join(LowpanProvision provision) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterface
        public void form(LowpanProvision provision) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterface
        public void attach(LowpanProvision provision) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterface
        public void leave() throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterface
        public void reset() throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterface
        public void startCommissioningSession(LowpanBeaconInfo beaconInfo) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterface
        public void closeCommissioningSession() throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterface
        public void sendToCommissioner(byte[] packet) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterface
        public void beginLowPower() throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterface
        public void pollForData() throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterface
        public void onHostWake() throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterface
        public void addListener(ILowpanInterfaceListener listener) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterface
        public void removeListener(ILowpanInterfaceListener listener) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterface
        public void startNetScan(Map properties, ILowpanNetScanCallback listener) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterface
        public void stopNetScan() throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterface
        public void startEnergyScan(Map properties, ILowpanEnergyScanCallback listener) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterface
        public void stopEnergyScan() throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterface
        public void addOnMeshPrefix(IpPrefix prefix, int flags) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterface
        public void removeOnMeshPrefix(IpPrefix prefix) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterface
        public void addExternalRoute(IpPrefix prefix, int flags) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterface
        public void removeExternalRoute(IpPrefix prefix) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements ILowpanInterface {
        static final int TRANSACTION_addExternalRoute = 39;
        static final int TRANSACTION_addListener = 31;
        static final int TRANSACTION_addOnMeshPrefix = 37;
        static final int TRANSACTION_attach = 22;
        static final int TRANSACTION_beginLowPower = 28;
        static final int TRANSACTION_closeCommissioningSession = 26;
        static final int TRANSACTION_form = 21;
        static final int TRANSACTION_getDriverVersion = 3;
        static final int TRANSACTION_getExtendedAddress = 15;
        static final int TRANSACTION_getLinkAddresses = 18;
        static final int TRANSACTION_getLinkNetworks = 19;
        static final int TRANSACTION_getLowpanCredential = 17;
        static final int TRANSACTION_getLowpanIdentity = 16;
        static final int TRANSACTION_getMacAddress = 6;
        static final int TRANSACTION_getName = 1;
        static final int TRANSACTION_getNcpVersion = 2;
        static final int TRANSACTION_getPartitionId = 14;
        static final int TRANSACTION_getRole = 13;
        static final int TRANSACTION_getState = 12;
        static final int TRANSACTION_getSupportedChannels = 4;
        static final int TRANSACTION_getSupportedNetworkTypes = 5;
        static final int TRANSACTION_isCommissioned = 10;
        static final int TRANSACTION_isConnected = 11;
        static final int TRANSACTION_isEnabled = 7;
        static final int TRANSACTION_isUp = 9;
        static final int TRANSACTION_join = 20;
        static final int TRANSACTION_leave = 23;
        static final int TRANSACTION_onHostWake = 30;
        static final int TRANSACTION_pollForData = 29;
        static final int TRANSACTION_removeExternalRoute = 40;
        static final int TRANSACTION_removeListener = 32;
        static final int TRANSACTION_removeOnMeshPrefix = 38;
        static final int TRANSACTION_reset = 24;
        static final int TRANSACTION_sendToCommissioner = 27;
        static final int TRANSACTION_setEnabled = 8;
        static final int TRANSACTION_startCommissioningSession = 25;
        static final int TRANSACTION_startEnergyScan = 35;
        static final int TRANSACTION_startNetScan = 33;
        static final int TRANSACTION_stopEnergyScan = 36;
        static final int TRANSACTION_stopNetScan = 34;

        public Stub() {
            attachInterface(this, ILowpanInterface.DESCRIPTOR);
        }

        public static ILowpanInterface asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ILowpanInterface.DESCRIPTOR);
            if (iin != null && (iin instanceof ILowpanInterface)) {
                return (ILowpanInterface) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public static String getDefaultTransactionName(int transactionCode) {
            switch (transactionCode) {
                case 1:
                    return "getName";
                case 2:
                    return "getNcpVersion";
                case 3:
                    return "getDriverVersion";
                case 4:
                    return "getSupportedChannels";
                case 5:
                    return "getSupportedNetworkTypes";
                case 6:
                    return "getMacAddress";
                case 7:
                    return "isEnabled";
                case 8:
                    return "setEnabled";
                case 9:
                    return "isUp";
                case 10:
                    return "isCommissioned";
                case 11:
                    return "isConnected";
                case 12:
                    return "getState";
                case 13:
                    return "getRole";
                case 14:
                    return "getPartitionId";
                case 15:
                    return "getExtendedAddress";
                case 16:
                    return "getLowpanIdentity";
                case 17:
                    return "getLowpanCredential";
                case 18:
                    return "getLinkAddresses";
                case 19:
                    return "getLinkNetworks";
                case 20:
                    return "join";
                case 21:
                    return "form";
                case 22:
                    return "attach";
                case 23:
                    return "leave";
                case 24:
                    return "reset";
                case 25:
                    return "startCommissioningSession";
                case 26:
                    return "closeCommissioningSession";
                case 27:
                    return "sendToCommissioner";
                case 28:
                    return "beginLowPower";
                case 29:
                    return "pollForData";
                case 30:
                    return "onHostWake";
                case 31:
                    return "addListener";
                case 32:
                    return "removeListener";
                case 33:
                    return "startNetScan";
                case 34:
                    return "stopNetScan";
                case 35:
                    return "startEnergyScan";
                case 36:
                    return "stopEnergyScan";
                case 37:
                    return "addOnMeshPrefix";
                case 38:
                    return "removeOnMeshPrefix";
                case 39:
                    return "addExternalRoute";
                case 40:
                    return "removeExternalRoute";
                default:
                    return null;
            }
        }

        @Override // android.os.Binder
        public String getTransactionName(int transactionCode) {
            return getDefaultTransactionName(transactionCode);
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(ILowpanInterface.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ILowpanInterface.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            String _result = getName();
                            reply.writeNoException();
                            reply.writeString(_result);
                            break;
                        case 2:
                            String _result2 = getNcpVersion();
                            reply.writeNoException();
                            reply.writeString(_result2);
                            break;
                        case 3:
                            String _result3 = getDriverVersion();
                            reply.writeNoException();
                            reply.writeString(_result3);
                            break;
                        case 4:
                            LowpanChannelInfo[] _result4 = getSupportedChannels();
                            reply.writeNoException();
                            reply.writeTypedArray(_result4, 1);
                            break;
                        case 5:
                            String[] _result5 = getSupportedNetworkTypes();
                            reply.writeNoException();
                            reply.writeStringArray(_result5);
                            break;
                        case 6:
                            byte[] _result6 = getMacAddress();
                            reply.writeNoException();
                            reply.writeByteArray(_result6);
                            break;
                        case 7:
                            boolean _result7 = isEnabled();
                            reply.writeNoException();
                            reply.writeBoolean(_result7);
                            break;
                        case 8:
                            boolean _arg0 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setEnabled(_arg0);
                            reply.writeNoException();
                            break;
                        case 9:
                            boolean _result8 = isUp();
                            reply.writeNoException();
                            reply.writeBoolean(_result8);
                            break;
                        case 10:
                            boolean _result9 = isCommissioned();
                            reply.writeNoException();
                            reply.writeBoolean(_result9);
                            break;
                        case 11:
                            boolean _result10 = isConnected();
                            reply.writeNoException();
                            reply.writeBoolean(_result10);
                            break;
                        case 12:
                            String _result11 = getState();
                            reply.writeNoException();
                            reply.writeString(_result11);
                            break;
                        case 13:
                            String _result12 = getRole();
                            reply.writeNoException();
                            reply.writeString(_result12);
                            break;
                        case 14:
                            String _result13 = getPartitionId();
                            reply.writeNoException();
                            reply.writeString(_result13);
                            break;
                        case 15:
                            byte[] _result14 = getExtendedAddress();
                            reply.writeNoException();
                            reply.writeByteArray(_result14);
                            break;
                        case 16:
                            LowpanIdentity _result15 = getLowpanIdentity();
                            reply.writeNoException();
                            reply.writeTypedObject(_result15, 1);
                            break;
                        case 17:
                            LowpanCredential _result16 = getLowpanCredential();
                            reply.writeNoException();
                            reply.writeTypedObject(_result16, 1);
                            break;
                        case 18:
                            String[] _result17 = getLinkAddresses();
                            reply.writeNoException();
                            reply.writeStringArray(_result17);
                            break;
                        case 19:
                            IpPrefix[] _result18 = getLinkNetworks();
                            reply.writeNoException();
                            reply.writeTypedArray(_result18, 1);
                            break;
                        case 20:
                            LowpanProvision _arg02 = (LowpanProvision) data.readTypedObject(LowpanProvision.CREATOR);
                            data.enforceNoDataAvail();
                            join(_arg02);
                            reply.writeNoException();
                            break;
                        case 21:
                            LowpanProvision _arg03 = (LowpanProvision) data.readTypedObject(LowpanProvision.CREATOR);
                            data.enforceNoDataAvail();
                            form(_arg03);
                            reply.writeNoException();
                            break;
                        case 22:
                            LowpanProvision _arg04 = (LowpanProvision) data.readTypedObject(LowpanProvision.CREATOR);
                            data.enforceNoDataAvail();
                            attach(_arg04);
                            reply.writeNoException();
                            break;
                        case 23:
                            leave();
                            reply.writeNoException();
                            break;
                        case 24:
                            reset();
                            reply.writeNoException();
                            break;
                        case 25:
                            LowpanBeaconInfo _arg05 = (LowpanBeaconInfo) data.readTypedObject(LowpanBeaconInfo.CREATOR);
                            data.enforceNoDataAvail();
                            startCommissioningSession(_arg05);
                            reply.writeNoException();
                            break;
                        case 26:
                            closeCommissioningSession();
                            reply.writeNoException();
                            break;
                        case 27:
                            byte[] _arg06 = data.createByteArray();
                            data.enforceNoDataAvail();
                            sendToCommissioner(_arg06);
                            break;
                        case 28:
                            beginLowPower();
                            reply.writeNoException();
                            break;
                        case 29:
                            pollForData();
                            break;
                        case 30:
                            onHostWake();
                            break;
                        case 31:
                            ILowpanInterfaceListener _arg07 = ILowpanInterfaceListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            addListener(_arg07);
                            reply.writeNoException();
                            break;
                        case 32:
                            ILowpanInterfaceListener _arg08 = ILowpanInterfaceListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            removeListener(_arg08);
                            break;
                        case 33:
                            ClassLoader cl = getClass().getClassLoader();
                            Map _arg09 = data.readHashMap(cl);
                            ILowpanNetScanCallback _arg1 = ILowpanNetScanCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            startNetScan(_arg09, _arg1);
                            reply.writeNoException();
                            break;
                        case 34:
                            stopNetScan();
                            break;
                        case 35:
                            ClassLoader cl2 = getClass().getClassLoader();
                            Map _arg010 = data.readHashMap(cl2);
                            ILowpanEnergyScanCallback _arg12 = ILowpanEnergyScanCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            startEnergyScan(_arg010, _arg12);
                            reply.writeNoException();
                            break;
                        case 36:
                            stopEnergyScan();
                            break;
                        case 37:
                            IpPrefix _arg011 = (IpPrefix) data.readTypedObject(IpPrefix.CREATOR);
                            int _arg13 = data.readInt();
                            data.enforceNoDataAvail();
                            addOnMeshPrefix(_arg011, _arg13);
                            reply.writeNoException();
                            break;
                        case 38:
                            IpPrefix _arg012 = (IpPrefix) data.readTypedObject(IpPrefix.CREATOR);
                            data.enforceNoDataAvail();
                            removeOnMeshPrefix(_arg012);
                            break;
                        case 39:
                            IpPrefix _arg013 = (IpPrefix) data.readTypedObject(IpPrefix.CREATOR);
                            int _arg14 = data.readInt();
                            data.enforceNoDataAvail();
                            addExternalRoute(_arg013, _arg14);
                            reply.writeNoException();
                            break;
                        case 40:
                            IpPrefix _arg014 = (IpPrefix) data.readTypedObject(IpPrefix.CREATOR);
                            data.enforceNoDataAvail();
                            removeExternalRoute(_arg014);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements ILowpanInterface {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ILowpanInterface.DESCRIPTOR;
            }

            @Override // android.net.lowpan.ILowpanInterface
            public String getName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public String getNcpVersion() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public String getDriverVersion() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public LowpanChannelInfo[] getSupportedChannels() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    LowpanChannelInfo[] _result = (LowpanChannelInfo[]) _reply.createTypedArray(LowpanChannelInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public String[] getSupportedNetworkTypes() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public byte[] getMacAddress() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public boolean isEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public void setEnabled(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public boolean isUp() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public boolean isCommissioned() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public boolean isConnected() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public String getState() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public String getRole() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public String getPartitionId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public byte[] getExtendedAddress() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public LowpanIdentity getLowpanIdentity() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    LowpanIdentity _result = (LowpanIdentity) _reply.readTypedObject(LowpanIdentity.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public LowpanCredential getLowpanCredential() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    LowpanCredential _result = (LowpanCredential) _reply.readTypedObject(LowpanCredential.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public String[] getLinkAddresses() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public IpPrefix[] getLinkNetworks() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    IpPrefix[] _result = (IpPrefix[]) _reply.createTypedArray(IpPrefix.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public void join(LowpanProvision provision) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    _data.writeTypedObject(provision, 0);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public void form(LowpanProvision provision) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    _data.writeTypedObject(provision, 0);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public void attach(LowpanProvision provision) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    _data.writeTypedObject(provision, 0);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public void leave() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public void reset() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public void startCommissioningSession(LowpanBeaconInfo beaconInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    _data.writeTypedObject(beaconInfo, 0);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public void closeCommissioningSession() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public void sendToCommissioner(byte[] packet) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    _data.writeByteArray(packet);
                    this.mRemote.transact(27, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public void beginLowPower() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public void pollForData() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(29, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public void onHostWake() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(30, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public void addListener(ILowpanInterfaceListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public void removeListener(ILowpanInterfaceListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(32, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public void startNetScan(Map properties, ILowpanNetScanCallback listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    _data.writeMap(properties);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public void stopNetScan() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(34, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public void startEnergyScan(Map properties, ILowpanEnergyScanCallback listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    _data.writeMap(properties);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public void stopEnergyScan() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    this.mRemote.transact(36, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public void addOnMeshPrefix(IpPrefix prefix, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    _data.writeTypedObject(prefix, 0);
                    _data.writeInt(flags);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public void removeOnMeshPrefix(IpPrefix prefix) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    _data.writeTypedObject(prefix, 0);
                    this.mRemote.transact(38, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public void addExternalRoute(IpPrefix prefix, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    _data.writeTypedObject(prefix, 0);
                    _data.writeInt(flags);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterface
            public void removeExternalRoute(IpPrefix prefix) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterface.DESCRIPTOR);
                    _data.writeTypedObject(prefix, 0);
                    this.mRemote.transact(40, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 39;
        }
    }
}
