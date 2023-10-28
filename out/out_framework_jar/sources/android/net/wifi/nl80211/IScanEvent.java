package android.net.wifi.nl80211;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes2.dex */
public interface IScanEvent extends IInterface {
    public static final String DESCRIPTOR = "android.net.wifi.nl80211.IScanEvent";

    void OnScanFailed() throws RemoteException;

    void OnScanResultReady() throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IScanEvent {
        @Override // android.net.wifi.nl80211.IScanEvent
        public void OnScanResultReady() throws RemoteException {
        }

        @Override // android.net.wifi.nl80211.IScanEvent
        public void OnScanFailed() throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IScanEvent {
        static final int TRANSACTION_OnScanFailed = 2;
        static final int TRANSACTION_OnScanResultReady = 1;

        public Stub() {
            attachInterface(this, IScanEvent.DESCRIPTOR);
        }

        public static IScanEvent asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IScanEvent.DESCRIPTOR);
            if (iin != null && (iin instanceof IScanEvent)) {
                return (IScanEvent) iin;
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
                    return "OnScanResultReady";
                case 2:
                    return "OnScanFailed";
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
                data.enforceInterface(IScanEvent.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IScanEvent.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            OnScanResultReady();
                            break;
                        case 2:
                            OnScanFailed();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IScanEvent {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IScanEvent.DESCRIPTOR;
            }

            @Override // android.net.wifi.nl80211.IScanEvent
            public void OnScanResultReady() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IScanEvent.DESCRIPTOR);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.wifi.nl80211.IScanEvent
            public void OnScanFailed() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IScanEvent.DESCRIPTOR);
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
