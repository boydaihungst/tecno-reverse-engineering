package android.hardware.health;

import android.hardware.health.IHealthInfoCallback;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IHealth extends IInterface {
    public static final String DESCRIPTOR = "android$hardware$health$IHealth".replace('$', '.');
    public static final String HASH = "94e77215594f8ad98ab33a769263d48fdabed92e";
    public static final int STATUS_CALLBACK_DIED = 4;
    public static final int STATUS_UNKNOWN = 2;
    public static final int VERSION = 1;

    int getCapacity() throws RemoteException;

    int getChargeCounterUah() throws RemoteException;

    int getChargeStatus() throws RemoteException;

    int getCurrentAverageMicroamps() throws RemoteException;

    int getCurrentNowMicroamps() throws RemoteException;

    DiskStats[] getDiskStats() throws RemoteException;

    long getEnergyCounterNwh() throws RemoteException;

    HealthInfo getHealthInfo() throws RemoteException;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    StorageInfo[] getStorageInfo() throws RemoteException;

    void registerCallback(IHealthInfoCallback iHealthInfoCallback) throws RemoteException;

    void unregisterCallback(IHealthInfoCallback iHealthInfoCallback) throws RemoteException;

    void update() throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IHealth {
        @Override // android.hardware.health.IHealth
        public void registerCallback(IHealthInfoCallback callback) throws RemoteException {
        }

        @Override // android.hardware.health.IHealth
        public void unregisterCallback(IHealthInfoCallback callback) throws RemoteException {
        }

        @Override // android.hardware.health.IHealth
        public void update() throws RemoteException {
        }

        @Override // android.hardware.health.IHealth
        public int getChargeCounterUah() throws RemoteException {
            return 0;
        }

        @Override // android.hardware.health.IHealth
        public int getCurrentNowMicroamps() throws RemoteException {
            return 0;
        }

        @Override // android.hardware.health.IHealth
        public int getCurrentAverageMicroamps() throws RemoteException {
            return 0;
        }

        @Override // android.hardware.health.IHealth
        public int getCapacity() throws RemoteException {
            return 0;
        }

        @Override // android.hardware.health.IHealth
        public long getEnergyCounterNwh() throws RemoteException {
            return 0L;
        }

        @Override // android.hardware.health.IHealth
        public int getChargeStatus() throws RemoteException {
            return 0;
        }

        @Override // android.hardware.health.IHealth
        public StorageInfo[] getStorageInfo() throws RemoteException {
            return null;
        }

        @Override // android.hardware.health.IHealth
        public DiskStats[] getDiskStats() throws RemoteException {
            return null;
        }

        @Override // android.hardware.health.IHealth
        public HealthInfo getHealthInfo() throws RemoteException {
            return null;
        }

        @Override // android.hardware.health.IHealth
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.hardware.health.IHealth
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IHealth {
        static final int TRANSACTION_getCapacity = 7;
        static final int TRANSACTION_getChargeCounterUah = 4;
        static final int TRANSACTION_getChargeStatus = 9;
        static final int TRANSACTION_getCurrentAverageMicroamps = 6;
        static final int TRANSACTION_getCurrentNowMicroamps = 5;
        static final int TRANSACTION_getDiskStats = 11;
        static final int TRANSACTION_getEnergyCounterNwh = 8;
        static final int TRANSACTION_getHealthInfo = 12;
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_getStorageInfo = 10;
        static final int TRANSACTION_registerCallback = 1;
        static final int TRANSACTION_unregisterCallback = 2;
        static final int TRANSACTION_update = 3;

        public Stub() {
            markVintfStability();
            attachInterface(this, DESCRIPTOR);
        }

        public static IHealth asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IHealth)) {
                return (IHealth) iin;
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
                            IHealthInfoCallback _arg0 = IHealthInfoCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            registerCallback(_arg0);
                            reply.writeNoException();
                            break;
                        case 2:
                            IHealthInfoCallback _arg02 = IHealthInfoCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            unregisterCallback(_arg02);
                            reply.writeNoException();
                            break;
                        case 3:
                            update();
                            reply.writeNoException();
                            break;
                        case 4:
                            int _result = getChargeCounterUah();
                            reply.writeNoException();
                            reply.writeInt(_result);
                            break;
                        case 5:
                            int _result2 = getCurrentNowMicroamps();
                            reply.writeNoException();
                            reply.writeInt(_result2);
                            break;
                        case 6:
                            int _result3 = getCurrentAverageMicroamps();
                            reply.writeNoException();
                            reply.writeInt(_result3);
                            break;
                        case 7:
                            int _result4 = getCapacity();
                            reply.writeNoException();
                            reply.writeInt(_result4);
                            break;
                        case 8:
                            long _result5 = getEnergyCounterNwh();
                            reply.writeNoException();
                            reply.writeLong(_result5);
                            break;
                        case 9:
                            int _result6 = getChargeStatus();
                            reply.writeNoException();
                            reply.writeInt(_result6);
                            break;
                        case 10:
                            StorageInfo[] _result7 = getStorageInfo();
                            reply.writeNoException();
                            reply.writeTypedArray(_result7, 1);
                            break;
                        case 11:
                            DiskStats[] _result8 = getDiskStats();
                            reply.writeNoException();
                            reply.writeTypedArray(_result8, 1);
                            break;
                        case 12:
                            HealthInfo _result9 = getHealthInfo();
                            reply.writeNoException();
                            reply.writeTypedObject(_result9, 1);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IHealth {
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

            @Override // android.hardware.health.IHealth
            public void registerCallback(IHealthInfoCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongInterface(callback);
                    boolean _status = this.mRemote.transact(1, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method registerCallback is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.health.IHealth
            public void unregisterCallback(IHealthInfoCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongInterface(callback);
                    boolean _status = this.mRemote.transact(2, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method unregisterCallback is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.health.IHealth
            public void update() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(3, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method update is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.health.IHealth
            public int getChargeCounterUah() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(4, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method getChargeCounterUah is unimplemented.");
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.health.IHealth
            public int getCurrentNowMicroamps() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(5, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method getCurrentNowMicroamps is unimplemented.");
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.health.IHealth
            public int getCurrentAverageMicroamps() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(6, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method getCurrentAverageMicroamps is unimplemented.");
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.health.IHealth
            public int getCapacity() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(7, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method getCapacity is unimplemented.");
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.health.IHealth
            public long getEnergyCounterNwh() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(8, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method getEnergyCounterNwh is unimplemented.");
                    }
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.health.IHealth
            public int getChargeStatus() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(9, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method getChargeStatus is unimplemented.");
                    }
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.health.IHealth
            public StorageInfo[] getStorageInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(10, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method getStorageInfo is unimplemented.");
                    }
                    _reply.readException();
                    StorageInfo[] _result = (StorageInfo[]) _reply.createTypedArray(StorageInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.health.IHealth
            public DiskStats[] getDiskStats() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(11, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method getDiskStats is unimplemented.");
                    }
                    _reply.readException();
                    DiskStats[] _result = (DiskStats[]) _reply.createTypedArray(DiskStats.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.health.IHealth
            public HealthInfo getHealthInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(12, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method getHealthInfo is unimplemented.");
                    }
                    _reply.readException();
                    HealthInfo _result = (HealthInfo) _reply.readTypedObject(HealthInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.health.IHealth
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

            @Override // android.hardware.health.IHealth
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
