package android.net.metrics;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface INetdEventListener extends IInterface {
    public static final String DESCRIPTOR = "android$net$metrics$INetdEventListener".replace('$', '.');
    public static final int DNS_REPORTED_IP_ADDRESSES_LIMIT = 10;
    public static final int EVENT_GETADDRINFO = 1;
    public static final int EVENT_GETHOSTBYADDR = 3;
    public static final int EVENT_GETHOSTBYNAME = 2;
    public static final int EVENT_RES_NSEND = 4;
    public static final String HASH = "8e27594d285ca7c567d87e8cf74766c27647e02b";
    public static final int REPORTING_LEVEL_FULL = 2;
    public static final int REPORTING_LEVEL_METRICS = 1;
    public static final int REPORTING_LEVEL_NONE = 0;
    public static final int VERSION = 1;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void onConnectEvent(int i, int i2, int i3, String str, int i4, int i5) throws RemoteException;

    void onDnsEvent(int i, int i2, int i3, int i4, String str, String[] strArr, int i5, int i6) throws RemoteException;

    void onNat64PrefixEvent(int i, boolean z, String str, int i2) throws RemoteException;

    void onPrivateDnsValidationEvent(int i, String str, String str2, boolean z) throws RemoteException;

    void onTcpSocketStatsEvent(int[] iArr, int[] iArr2, int[] iArr3, int[] iArr4, int[] iArr5) throws RemoteException;

    void onWakeupEvent(String str, int i, int i2, int i3, byte[] bArr, String str2, String str3, int i4, int i5, long j) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements INetdEventListener {
        @Override // android.net.metrics.INetdEventListener
        public void onDnsEvent(int netId, int eventType, int returnCode, int latencyMs, String hostname, String[] ipAddresses, int ipAddressesCount, int uid) throws RemoteException {
        }

        @Override // android.net.metrics.INetdEventListener
        public void onPrivateDnsValidationEvent(int netId, String ipAddress, String hostname, boolean validated) throws RemoteException {
        }

        @Override // android.net.metrics.INetdEventListener
        public void onConnectEvent(int netId, int error, int latencyMs, String ipAddr, int port, int uid) throws RemoteException {
        }

        @Override // android.net.metrics.INetdEventListener
        public void onWakeupEvent(String prefix, int uid, int ethertype, int ipNextHeader, byte[] dstHw, String srcIp, String dstIp, int srcPort, int dstPort, long timestampNs) throws RemoteException {
        }

        @Override // android.net.metrics.INetdEventListener
        public void onTcpSocketStatsEvent(int[] networkIds, int[] sentPackets, int[] lostPackets, int[] rttUs, int[] sentAckDiffMs) throws RemoteException {
        }

        @Override // android.net.metrics.INetdEventListener
        public void onNat64PrefixEvent(int netId, boolean added, String prefixString, int prefixLength) throws RemoteException {
        }

        @Override // android.net.metrics.INetdEventListener
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.net.metrics.INetdEventListener
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements INetdEventListener {
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_onConnectEvent = 3;
        static final int TRANSACTION_onDnsEvent = 1;
        static final int TRANSACTION_onNat64PrefixEvent = 6;
        static final int TRANSACTION_onPrivateDnsValidationEvent = 2;
        static final int TRANSACTION_onTcpSocketStatsEvent = 5;
        static final int TRANSACTION_onWakeupEvent = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INetdEventListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof INetdEventListener)) {
                return (INetdEventListener) iin;
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
                            int _arg0 = data.readInt();
                            int _arg1 = data.readInt();
                            int _arg2 = data.readInt();
                            int _arg3 = data.readInt();
                            String _arg4 = data.readString();
                            String[] _arg5 = data.createStringArray();
                            int _arg6 = data.readInt();
                            int _arg7 = data.readInt();
                            onDnsEvent(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7);
                            return true;
                        case 2:
                            int _arg02 = data.readInt();
                            String _arg12 = data.readString();
                            String _arg22 = data.readString();
                            boolean _arg32 = data.readBoolean();
                            onPrivateDnsValidationEvent(_arg02, _arg12, _arg22, _arg32);
                            return true;
                        case 3:
                            int _arg03 = data.readInt();
                            int _arg13 = data.readInt();
                            int _arg23 = data.readInt();
                            String _arg33 = data.readString();
                            int _arg42 = data.readInt();
                            int _arg52 = data.readInt();
                            onConnectEvent(_arg03, _arg13, _arg23, _arg33, _arg42, _arg52);
                            return true;
                        case 4:
                            String _arg04 = data.readString();
                            int _arg14 = data.readInt();
                            int _arg24 = data.readInt();
                            int _arg34 = data.readInt();
                            byte[] _arg43 = data.createByteArray();
                            String _arg53 = data.readString();
                            String _arg62 = data.readString();
                            int _arg72 = data.readInt();
                            int _arg8 = data.readInt();
                            long _arg9 = data.readLong();
                            onWakeupEvent(_arg04, _arg14, _arg24, _arg34, _arg43, _arg53, _arg62, _arg72, _arg8, _arg9);
                            return true;
                        case 5:
                            int[] _arg05 = data.createIntArray();
                            int[] _arg15 = data.createIntArray();
                            int[] _arg25 = data.createIntArray();
                            int[] _arg35 = data.createIntArray();
                            int[] _arg44 = data.createIntArray();
                            onTcpSocketStatsEvent(_arg05, _arg15, _arg25, _arg35, _arg44);
                            return true;
                        case 6:
                            int _arg06 = data.readInt();
                            boolean _arg16 = data.readBoolean();
                            String _arg26 = data.readString();
                            int _arg36 = data.readInt();
                            onNat64PrefixEvent(_arg06, _arg16, _arg26, _arg36);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements INetdEventListener {
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

            @Override // android.net.metrics.INetdEventListener
            public void onDnsEvent(int netId, int eventType, int returnCode, int latencyMs, String hostname, String[] ipAddresses, int ipAddressesCount, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeInt(eventType);
                    _data.writeInt(returnCode);
                    _data.writeInt(latencyMs);
                    _data.writeString(hostname);
                    _data.writeStringArray(ipAddresses);
                    _data.writeInt(ipAddressesCount);
                    _data.writeInt(uid);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onDnsEvent is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.metrics.INetdEventListener
            public void onPrivateDnsValidationEvent(int netId, String ipAddress, String hostname, boolean validated) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeString(ipAddress);
                    _data.writeString(hostname);
                    _data.writeBoolean(validated);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onPrivateDnsValidationEvent is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.metrics.INetdEventListener
            public void onConnectEvent(int netId, int error, int latencyMs, String ipAddr, int port, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeInt(error);
                    _data.writeInt(latencyMs);
                    _data.writeString(ipAddr);
                    _data.writeInt(port);
                    _data.writeInt(uid);
                    boolean _status = this.mRemote.transact(3, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onConnectEvent is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.metrics.INetdEventListener
            public void onWakeupEvent(String prefix, int uid, int ethertype, int ipNextHeader, byte[] dstHw, String srcIp, String dstIp, int srcPort, int dstPort, long timestampNs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(prefix);
                    try {
                        _data.writeInt(uid);
                        try {
                            _data.writeInt(ethertype);
                        } catch (Throwable th) {
                            th = th;
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                }
                try {
                    _data.writeInt(ipNextHeader);
                    try {
                        _data.writeByteArray(dstHw);
                        try {
                            _data.writeString(srcIp);
                            try {
                                _data.writeString(dstIp);
                            } catch (Throwable th4) {
                                th = th4;
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(srcPort);
                        try {
                            _data.writeInt(dstPort);
                            try {
                                _data.writeLong(timestampNs);
                                try {
                                    boolean _status = this.mRemote.transact(4, _data, null, 1);
                                    if (!_status) {
                                        throw new RemoteException("Method onWakeupEvent is unimplemented.");
                                    }
                                    _data.recycle();
                                } catch (Throwable th7) {
                                    th = th7;
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th8) {
                                th = th8;
                            }
                        } catch (Throwable th9) {
                            th = th9;
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th10) {
                        th = th10;
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th11) {
                    th = th11;
                    _data.recycle();
                    throw th;
                }
            }

            @Override // android.net.metrics.INetdEventListener
            public void onTcpSocketStatsEvent(int[] networkIds, int[] sentPackets, int[] lostPackets, int[] rttUs, int[] sentAckDiffMs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeIntArray(networkIds);
                    _data.writeIntArray(sentPackets);
                    _data.writeIntArray(lostPackets);
                    _data.writeIntArray(rttUs);
                    _data.writeIntArray(sentAckDiffMs);
                    boolean _status = this.mRemote.transact(5, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onTcpSocketStatsEvent is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.metrics.INetdEventListener
            public void onNat64PrefixEvent(int netId, boolean added, String prefixString, int prefixLength) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(netId);
                    _data.writeBoolean(added);
                    _data.writeString(prefixString);
                    _data.writeInt(prefixLength);
                    boolean _status = this.mRemote.transact(6, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onNat64PrefixEvent is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.metrics.INetdEventListener
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

            @Override // android.net.metrics.INetdEventListener
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
