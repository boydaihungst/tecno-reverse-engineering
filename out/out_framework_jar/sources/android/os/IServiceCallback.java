package android.os;
/* loaded from: classes2.dex */
public interface IServiceCallback extends IInterface {
    public static final String DESCRIPTOR = "android.os.IServiceCallback";

    void onRegistration(String str, IBinder iBinder) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IServiceCallback {
        @Override // android.os.IServiceCallback
        public void onRegistration(String name, IBinder binder) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IServiceCallback {
        static final int TRANSACTION_onRegistration = 1;

        public Stub() {
            attachInterface(this, IServiceCallback.DESCRIPTOR);
        }

        public static IServiceCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IServiceCallback.DESCRIPTOR);
            if (iin != null && (iin instanceof IServiceCallback)) {
                return (IServiceCallback) iin;
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
                    return "onRegistration";
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
                data.enforceInterface(IServiceCallback.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IServiceCallback.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            String _arg0 = data.readString();
                            IBinder _arg1 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            onRegistration(_arg0, _arg1);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IServiceCallback {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IServiceCallback.DESCRIPTOR;
            }

            @Override // android.os.IServiceCallback
            public void onRegistration(String name, IBinder binder) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IServiceCallback.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeStrongBinder(binder);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
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
