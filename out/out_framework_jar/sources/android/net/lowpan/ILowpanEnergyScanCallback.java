package android.net.lowpan;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes2.dex */
public interface ILowpanEnergyScanCallback extends IInterface {
    public static final String DESCRIPTOR = "android.net.lowpan.ILowpanEnergyScanCallback";

    void onEnergyScanFinished() throws RemoteException;

    void onEnergyScanResult(int i, int i2) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements ILowpanEnergyScanCallback {
        @Override // android.net.lowpan.ILowpanEnergyScanCallback
        public void onEnergyScanResult(int channel, int rssi) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanEnergyScanCallback
        public void onEnergyScanFinished() throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements ILowpanEnergyScanCallback {
        static final int TRANSACTION_onEnergyScanFinished = 2;
        static final int TRANSACTION_onEnergyScanResult = 1;

        public Stub() {
            attachInterface(this, ILowpanEnergyScanCallback.DESCRIPTOR);
        }

        public static ILowpanEnergyScanCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ILowpanEnergyScanCallback.DESCRIPTOR);
            if (iin != null && (iin instanceof ILowpanEnergyScanCallback)) {
                return (ILowpanEnergyScanCallback) iin;
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
                    return "onEnergyScanResult";
                case 2:
                    return "onEnergyScanFinished";
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
                data.enforceInterface(ILowpanEnergyScanCallback.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ILowpanEnergyScanCallback.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            int _arg1 = data.readInt();
                            data.enforceNoDataAvail();
                            onEnergyScanResult(_arg0, _arg1);
                            break;
                        case 2:
                            onEnergyScanFinished();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements ILowpanEnergyScanCallback {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ILowpanEnergyScanCallback.DESCRIPTOR;
            }

            @Override // android.net.lowpan.ILowpanEnergyScanCallback
            public void onEnergyScanResult(int channel, int rssi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanEnergyScanCallback.DESCRIPTOR);
                    _data.writeInt(channel);
                    _data.writeInt(rssi);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanEnergyScanCallback
            public void onEnergyScanFinished() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanEnergyScanCallback.DESCRIPTOR);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 1;
        }
    }
}
