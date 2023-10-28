package android.hardware.power.stats;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IPowerStats extends IInterface {
    public static final String DESCRIPTOR = "android$hardware$power$stats$IPowerStats".replace('$', '.');
    public static final String HASH = "93253458fae451cf1187db6120a59fab428f7d02";
    public static final int VERSION = 1;

    EnergyConsumerResult[] getEnergyConsumed(int[] iArr) throws RemoteException;

    EnergyConsumer[] getEnergyConsumerInfo() throws RemoteException;

    Channel[] getEnergyMeterInfo() throws RemoteException;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    PowerEntity[] getPowerEntityInfo() throws RemoteException;

    StateResidencyResult[] getStateResidency(int[] iArr) throws RemoteException;

    EnergyMeasurement[] readEnergyMeter(int[] iArr) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IPowerStats {
        @Override // android.hardware.power.stats.IPowerStats
        public PowerEntity[] getPowerEntityInfo() throws RemoteException {
            return null;
        }

        @Override // android.hardware.power.stats.IPowerStats
        public StateResidencyResult[] getStateResidency(int[] powerEntityIds) throws RemoteException {
            return null;
        }

        @Override // android.hardware.power.stats.IPowerStats
        public EnergyConsumer[] getEnergyConsumerInfo() throws RemoteException {
            return null;
        }

        @Override // android.hardware.power.stats.IPowerStats
        public EnergyConsumerResult[] getEnergyConsumed(int[] energyConsumerIds) throws RemoteException {
            return null;
        }

        @Override // android.hardware.power.stats.IPowerStats
        public Channel[] getEnergyMeterInfo() throws RemoteException {
            return null;
        }

        @Override // android.hardware.power.stats.IPowerStats
        public EnergyMeasurement[] readEnergyMeter(int[] channelIds) throws RemoteException {
            return null;
        }

        @Override // android.hardware.power.stats.IPowerStats
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.hardware.power.stats.IPowerStats
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IPowerStats {
        static final int TRANSACTION_getEnergyConsumed = 4;
        static final int TRANSACTION_getEnergyConsumerInfo = 3;
        static final int TRANSACTION_getEnergyMeterInfo = 5;
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_getPowerEntityInfo = 1;
        static final int TRANSACTION_getStateResidency = 2;
        static final int TRANSACTION_readEnergyMeter = 6;

        public Stub() {
            markVintfStability();
            attachInterface(this, DESCRIPTOR);
        }

        public static IPowerStats asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IPowerStats)) {
                return (IPowerStats) iin;
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
                            PowerEntity[] _result = getPowerEntityInfo();
                            reply.writeNoException();
                            reply.writeTypedArray(_result, 1);
                            break;
                        case 2:
                            int[] _arg0 = data.createIntArray();
                            data.enforceNoDataAvail();
                            StateResidencyResult[] _result2 = getStateResidency(_arg0);
                            reply.writeNoException();
                            reply.writeTypedArray(_result2, 1);
                            break;
                        case 3:
                            EnergyConsumer[] _result3 = getEnergyConsumerInfo();
                            reply.writeNoException();
                            reply.writeTypedArray(_result3, 1);
                            break;
                        case 4:
                            int[] _arg02 = data.createIntArray();
                            data.enforceNoDataAvail();
                            EnergyConsumerResult[] _result4 = getEnergyConsumed(_arg02);
                            reply.writeNoException();
                            reply.writeTypedArray(_result4, 1);
                            break;
                        case 5:
                            Channel[] _result5 = getEnergyMeterInfo();
                            reply.writeNoException();
                            reply.writeTypedArray(_result5, 1);
                            break;
                        case 6:
                            int[] _arg03 = data.createIntArray();
                            data.enforceNoDataAvail();
                            EnergyMeasurement[] _result6 = readEnergyMeter(_arg03);
                            reply.writeNoException();
                            reply.writeTypedArray(_result6, 1);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IPowerStats {
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

            @Override // android.hardware.power.stats.IPowerStats
            public PowerEntity[] getPowerEntityInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(1, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method getPowerEntityInfo is unimplemented.");
                    }
                    _reply.readException();
                    PowerEntity[] _result = (PowerEntity[]) _reply.createTypedArray(PowerEntity.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.power.stats.IPowerStats
            public StateResidencyResult[] getStateResidency(int[] powerEntityIds) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeIntArray(powerEntityIds);
                    boolean _status = this.mRemote.transact(2, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method getStateResidency is unimplemented.");
                    }
                    _reply.readException();
                    StateResidencyResult[] _result = (StateResidencyResult[]) _reply.createTypedArray(StateResidencyResult.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.power.stats.IPowerStats
            public EnergyConsumer[] getEnergyConsumerInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(3, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method getEnergyConsumerInfo is unimplemented.");
                    }
                    _reply.readException();
                    EnergyConsumer[] _result = (EnergyConsumer[]) _reply.createTypedArray(EnergyConsumer.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.power.stats.IPowerStats
            public EnergyConsumerResult[] getEnergyConsumed(int[] energyConsumerIds) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeIntArray(energyConsumerIds);
                    boolean _status = this.mRemote.transact(4, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method getEnergyConsumed is unimplemented.");
                    }
                    _reply.readException();
                    EnergyConsumerResult[] _result = (EnergyConsumerResult[]) _reply.createTypedArray(EnergyConsumerResult.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.power.stats.IPowerStats
            public Channel[] getEnergyMeterInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(5, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method getEnergyMeterInfo is unimplemented.");
                    }
                    _reply.readException();
                    Channel[] _result = (Channel[]) _reply.createTypedArray(Channel.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.power.stats.IPowerStats
            public EnergyMeasurement[] readEnergyMeter(int[] channelIds) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeIntArray(channelIds);
                    boolean _status = this.mRemote.transact(6, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method readEnergyMeter is unimplemented.");
                    }
                    _reply.readException();
                    EnergyMeasurement[] _result = (EnergyMeasurement[]) _reply.createTypedArray(EnergyMeasurement.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.power.stats.IPowerStats
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

            @Override // android.hardware.power.stats.IPowerStats
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
