package android.hardware.ir;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IConsumerIr extends IInterface {
    public static final String DESCRIPTOR = "android$hardware$ir$IConsumerIr".replace('$', '.');
    public static final String HASH = "3e04aed366e96850c6164287eaf78a8e4ab071b0";
    public static final int VERSION = 1;

    ConsumerIrFreqRange[] getCarrierFreqs() throws RemoteException;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void transmit(int i, int[] iArr) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IConsumerIr {
        @Override // android.hardware.ir.IConsumerIr
        public ConsumerIrFreqRange[] getCarrierFreqs() throws RemoteException {
            return null;
        }

        @Override // android.hardware.ir.IConsumerIr
        public void transmit(int carrierFreqHz, int[] pattern) throws RemoteException {
        }

        @Override // android.hardware.ir.IConsumerIr
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.hardware.ir.IConsumerIr
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IConsumerIr {
        static final int TRANSACTION_getCarrierFreqs = 1;
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_transmit = 2;

        public Stub() {
            markVintfStability();
            attachInterface(this, DESCRIPTOR);
        }

        public static IConsumerIr asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IConsumerIr)) {
                return (IConsumerIr) iin;
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
                            ConsumerIrFreqRange[] _result = getCarrierFreqs();
                            reply.writeNoException();
                            reply.writeTypedArray(_result, 1);
                            break;
                        case 2:
                            int _arg0 = data.readInt();
                            int[] _arg1 = data.createIntArray();
                            data.enforceNoDataAvail();
                            transmit(_arg0, _arg1);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IConsumerIr {
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

            @Override // android.hardware.ir.IConsumerIr
            public ConsumerIrFreqRange[] getCarrierFreqs() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(1, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method getCarrierFreqs is unimplemented.");
                    }
                    _reply.readException();
                    ConsumerIrFreqRange[] _result = (ConsumerIrFreqRange[]) _reply.createTypedArray(ConsumerIrFreqRange.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.ir.IConsumerIr
            public void transmit(int carrierFreqHz, int[] pattern) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(carrierFreqHz);
                    _data.writeIntArray(pattern);
                    boolean _status = this.mRemote.transact(2, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method transmit is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.ir.IConsumerIr
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

            @Override // android.hardware.ir.IConsumerIr
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
