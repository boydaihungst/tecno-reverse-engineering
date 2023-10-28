package android.net.lowpan;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes2.dex */
public interface ILowpanNetScanCallback extends IInterface {
    public static final String DESCRIPTOR = "android.net.lowpan.ILowpanNetScanCallback";

    void onNetScanBeacon(LowpanBeaconInfo lowpanBeaconInfo) throws RemoteException;

    void onNetScanFinished() throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements ILowpanNetScanCallback {
        @Override // android.net.lowpan.ILowpanNetScanCallback
        public void onNetScanBeacon(LowpanBeaconInfo beacon) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanNetScanCallback
        public void onNetScanFinished() throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements ILowpanNetScanCallback {
        static final int TRANSACTION_onNetScanBeacon = 1;
        static final int TRANSACTION_onNetScanFinished = 2;

        public Stub() {
            attachInterface(this, ILowpanNetScanCallback.DESCRIPTOR);
        }

        public static ILowpanNetScanCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ILowpanNetScanCallback.DESCRIPTOR);
            if (iin != null && (iin instanceof ILowpanNetScanCallback)) {
                return (ILowpanNetScanCallback) iin;
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
                    return "onNetScanBeacon";
                case 2:
                    return "onNetScanFinished";
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
                data.enforceInterface(ILowpanNetScanCallback.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ILowpanNetScanCallback.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            LowpanBeaconInfo _arg0 = (LowpanBeaconInfo) data.readTypedObject(LowpanBeaconInfo.CREATOR);
                            data.enforceNoDataAvail();
                            onNetScanBeacon(_arg0);
                            break;
                        case 2:
                            onNetScanFinished();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements ILowpanNetScanCallback {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ILowpanNetScanCallback.DESCRIPTOR;
            }

            @Override // android.net.lowpan.ILowpanNetScanCallback
            public void onNetScanBeacon(LowpanBeaconInfo beacon) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanNetScanCallback.DESCRIPTOR);
                    _data.writeTypedObject(beacon, 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanNetScanCallback
            public void onNetScanFinished() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanNetScanCallback.DESCRIPTOR);
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
