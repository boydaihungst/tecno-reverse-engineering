package android.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface INetdUnsolicitedEventListener extends IInterface {
    public static final String DESCRIPTOR = "android$net$INetdUnsolicitedEventListener".replace('$', '.');
    public static final String HASH = "3943383e838f39851675e3640fcdf27b42f8c9fc";
    public static final int VERSION = 10;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void onInterfaceAdded(String str) throws RemoteException;

    void onInterfaceAddressRemoved(String str, String str2, int i, int i2) throws RemoteException;

    void onInterfaceAddressUpdated(String str, String str2, int i, int i2) throws RemoteException;

    void onInterfaceChanged(String str, boolean z) throws RemoteException;

    void onInterfaceClassActivityChanged(boolean z, int i, long j, int i2) throws RemoteException;

    void onInterfaceDnsServerInfo(String str, long j, String[] strArr) throws RemoteException;

    void onInterfaceLinkStateChanged(String str, boolean z) throws RemoteException;

    void onInterfaceRemoved(String str) throws RemoteException;

    void onQuotaLimitReached(String str, String str2) throws RemoteException;

    void onRouteChanged(boolean z, String str, String str2, String str3) throws RemoteException;

    void onStrictCleartextDetected(int i, String str) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements INetdUnsolicitedEventListener {
        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceClassActivityChanged(boolean isActive, int timerLabel, long timestampNs, int uid) throws RemoteException {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onQuotaLimitReached(String alertName, String ifName) throws RemoteException {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceDnsServerInfo(String ifName, long lifetimeS, String[] servers) throws RemoteException {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceAddressUpdated(String addr, String ifName, int flags, int scope) throws RemoteException {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceAddressRemoved(String addr, String ifName, int flags, int scope) throws RemoteException {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceAdded(String ifName) throws RemoteException {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceRemoved(String ifName) throws RemoteException {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceChanged(String ifName, boolean up) throws RemoteException {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceLinkStateChanged(String ifName, boolean up) throws RemoteException {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onRouteChanged(boolean updated, String route, String gateway, String ifName) throws RemoteException {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onStrictCleartextDetected(int uid, String hex) throws RemoteException {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements INetdUnsolicitedEventListener {
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_onInterfaceAdded = 6;
        static final int TRANSACTION_onInterfaceAddressRemoved = 5;
        static final int TRANSACTION_onInterfaceAddressUpdated = 4;
        static final int TRANSACTION_onInterfaceChanged = 8;
        static final int TRANSACTION_onInterfaceClassActivityChanged = 1;
        static final int TRANSACTION_onInterfaceDnsServerInfo = 3;
        static final int TRANSACTION_onInterfaceLinkStateChanged = 9;
        static final int TRANSACTION_onInterfaceRemoved = 7;
        static final int TRANSACTION_onQuotaLimitReached = 2;
        static final int TRANSACTION_onRouteChanged = 10;
        static final int TRANSACTION_onStrictCleartextDetected = 11;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INetdUnsolicitedEventListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof INetdUnsolicitedEventListener)) {
                return (INetdUnsolicitedEventListener) iin;
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
                            boolean _arg0 = data.readBoolean();
                            int _arg1 = data.readInt();
                            long _arg2 = data.readLong();
                            int _arg3 = data.readInt();
                            onInterfaceClassActivityChanged(_arg0, _arg1, _arg2, _arg3);
                            break;
                        case 2:
                            String _arg02 = data.readString();
                            String _arg12 = data.readString();
                            onQuotaLimitReached(_arg02, _arg12);
                            break;
                        case 3:
                            String _arg03 = data.readString();
                            long _arg13 = data.readLong();
                            String[] _arg22 = data.createStringArray();
                            onInterfaceDnsServerInfo(_arg03, _arg13, _arg22);
                            break;
                        case 4:
                            String _arg04 = data.readString();
                            String _arg14 = data.readString();
                            int _arg23 = data.readInt();
                            int _arg32 = data.readInt();
                            onInterfaceAddressUpdated(_arg04, _arg14, _arg23, _arg32);
                            break;
                        case 5:
                            String _arg05 = data.readString();
                            String _arg15 = data.readString();
                            int _arg24 = data.readInt();
                            int _arg33 = data.readInt();
                            onInterfaceAddressRemoved(_arg05, _arg15, _arg24, _arg33);
                            break;
                        case 6:
                            String _arg06 = data.readString();
                            onInterfaceAdded(_arg06);
                            break;
                        case 7:
                            String _arg07 = data.readString();
                            onInterfaceRemoved(_arg07);
                            break;
                        case 8:
                            String _arg08 = data.readString();
                            boolean _arg16 = data.readBoolean();
                            onInterfaceChanged(_arg08, _arg16);
                            break;
                        case 9:
                            String _arg09 = data.readString();
                            boolean _arg17 = data.readBoolean();
                            onInterfaceLinkStateChanged(_arg09, _arg17);
                            break;
                        case 10:
                            boolean _arg010 = data.readBoolean();
                            String _arg18 = data.readString();
                            String _arg25 = data.readString();
                            String _arg34 = data.readString();
                            onRouteChanged(_arg010, _arg18, _arg25, _arg34);
                            break;
                        case 11:
                            int _arg011 = data.readInt();
                            String _arg19 = data.readString();
                            onStrictCleartextDetected(_arg011, _arg19);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements INetdUnsolicitedEventListener {
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

            @Override // android.net.INetdUnsolicitedEventListener
            public void onInterfaceClassActivityChanged(boolean isActive, int timerLabel, long timestampNs, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(isActive);
                    _data.writeInt(timerLabel);
                    _data.writeLong(timestampNs);
                    _data.writeInt(uid);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onInterfaceClassActivityChanged is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetdUnsolicitedEventListener
            public void onQuotaLimitReached(String alertName, String ifName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(alertName);
                    _data.writeString(ifName);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onQuotaLimitReached is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetdUnsolicitedEventListener
            public void onInterfaceDnsServerInfo(String ifName, long lifetimeS, String[] servers) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeLong(lifetimeS);
                    _data.writeStringArray(servers);
                    boolean _status = this.mRemote.transact(3, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onInterfaceDnsServerInfo is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetdUnsolicitedEventListener
            public void onInterfaceAddressUpdated(String addr, String ifName, int flags, int scope) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(addr);
                    _data.writeString(ifName);
                    _data.writeInt(flags);
                    _data.writeInt(scope);
                    boolean _status = this.mRemote.transact(4, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onInterfaceAddressUpdated is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetdUnsolicitedEventListener
            public void onInterfaceAddressRemoved(String addr, String ifName, int flags, int scope) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(addr);
                    _data.writeString(ifName);
                    _data.writeInt(flags);
                    _data.writeInt(scope);
                    boolean _status = this.mRemote.transact(5, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onInterfaceAddressRemoved is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetdUnsolicitedEventListener
            public void onInterfaceAdded(String ifName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(ifName);
                    boolean _status = this.mRemote.transact(6, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onInterfaceAdded is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetdUnsolicitedEventListener
            public void onInterfaceRemoved(String ifName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(ifName);
                    boolean _status = this.mRemote.transact(7, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onInterfaceRemoved is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetdUnsolicitedEventListener
            public void onInterfaceChanged(String ifName, boolean up) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeBoolean(up);
                    boolean _status = this.mRemote.transact(8, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onInterfaceChanged is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetdUnsolicitedEventListener
            public void onInterfaceLinkStateChanged(String ifName, boolean up) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeBoolean(up);
                    boolean _status = this.mRemote.transact(9, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onInterfaceLinkStateChanged is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetdUnsolicitedEventListener
            public void onRouteChanged(boolean updated, String route, String gateway, String ifName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(updated);
                    _data.writeString(route);
                    _data.writeString(gateway);
                    _data.writeString(ifName);
                    boolean _status = this.mRemote.transact(10, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onRouteChanged is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetdUnsolicitedEventListener
            public void onStrictCleartextDetected(int uid, String hex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeString(hex);
                    boolean _status = this.mRemote.transact(11, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method onStrictCleartextDetected is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.INetdUnsolicitedEventListener
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

            @Override // android.net.INetdUnsolicitedEventListener
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
