package android.os;
/* loaded from: classes2.dex */
public interface IAdaptiveSmartNetworkService extends IInterface {
    public static final String DESCRIPTOR = "android.os.IAdaptiveSmartNetworkService";

    void asnServiceStart() throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IAdaptiveSmartNetworkService {
        @Override // android.os.IAdaptiveSmartNetworkService
        public void asnServiceStart() throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IAdaptiveSmartNetworkService {
        static final int TRANSACTION_asnServiceStart = 1;

        public Stub() {
            attachInterface(this, IAdaptiveSmartNetworkService.DESCRIPTOR);
        }

        public static IAdaptiveSmartNetworkService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IAdaptiveSmartNetworkService.DESCRIPTOR);
            if (iin != null && (iin instanceof IAdaptiveSmartNetworkService)) {
                return (IAdaptiveSmartNetworkService) iin;
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
                    return "asnServiceStart";
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
                data.enforceInterface(IAdaptiveSmartNetworkService.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IAdaptiveSmartNetworkService.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            asnServiceStart();
                            reply.writeNoException();
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IAdaptiveSmartNetworkService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IAdaptiveSmartNetworkService.DESCRIPTOR;
            }

            @Override // android.os.IAdaptiveSmartNetworkService
            public void asnServiceStart() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAdaptiveSmartNetworkService.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 0;
        }
    }
}
