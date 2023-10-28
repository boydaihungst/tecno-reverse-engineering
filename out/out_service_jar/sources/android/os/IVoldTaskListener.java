package android.os;
/* loaded from: classes.dex */
public interface IVoldTaskListener extends IInterface {
    void onFinished(int i, PersistableBundle persistableBundle) throws RemoteException;

    void onStatus(int i, PersistableBundle persistableBundle) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IVoldTaskListener {
        @Override // android.os.IVoldTaskListener
        public void onStatus(int status, PersistableBundle extras) throws RemoteException {
        }

        @Override // android.os.IVoldTaskListener
        public void onFinished(int status, PersistableBundle extras) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IVoldTaskListener {
        public static final String DESCRIPTOR = "android.os.IVoldTaskListener";
        static final int TRANSACTION_onFinished = 2;
        static final int TRANSACTION_onStatus = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IVoldTaskListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IVoldTaskListener)) {
                return (IVoldTaskListener) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(DESCRIPTOR);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            PersistableBundle _arg1 = (PersistableBundle) data.readTypedObject(PersistableBundle.CREATOR);
                            data.enforceNoDataAvail();
                            onStatus(_arg0, _arg1);
                            break;
                        case 2:
                            int _arg02 = data.readInt();
                            PersistableBundle _arg12 = (PersistableBundle) data.readTypedObject(PersistableBundle.CREATOR);
                            data.enforceNoDataAvail();
                            onFinished(_arg02, _arg12);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IVoldTaskListener {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override // android.os.IVoldTaskListener
            public void onStatus(int status, PersistableBundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(status);
                    _data.writeTypedObject(extras, 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.os.IVoldTaskListener
            public void onFinished(int status, PersistableBundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(status);
                    _data.writeTypedObject(extras, 0);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
