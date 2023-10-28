package android.os;
/* loaded from: classes2.dex */
public interface IDumpstateListener extends IInterface {
    public static final int BUGREPORT_ERROR_ANOTHER_REPORT_IN_PROGRESS = 5;
    public static final int BUGREPORT_ERROR_INVALID_INPUT = 1;
    public static final int BUGREPORT_ERROR_RUNTIME_ERROR = 2;
    public static final int BUGREPORT_ERROR_USER_CONSENT_TIMED_OUT = 4;
    public static final int BUGREPORT_ERROR_USER_DENIED_CONSENT = 3;
    public static final String DESCRIPTOR = "android.os.IDumpstateListener";

    void onError(int i) throws RemoteException;

    void onFinished() throws RemoteException;

    void onProgress(int i) throws RemoteException;

    void onScreenshotTaken(boolean z) throws RemoteException;

    void onUiIntensiveBugreportDumpsFinished() throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IDumpstateListener {
        @Override // android.os.IDumpstateListener
        public void onProgress(int progress) throws RemoteException {
        }

        @Override // android.os.IDumpstateListener
        public void onError(int errorCode) throws RemoteException {
        }

        @Override // android.os.IDumpstateListener
        public void onFinished() throws RemoteException {
        }

        @Override // android.os.IDumpstateListener
        public void onScreenshotTaken(boolean success) throws RemoteException {
        }

        @Override // android.os.IDumpstateListener
        public void onUiIntensiveBugreportDumpsFinished() throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IDumpstateListener {
        static final int TRANSACTION_onError = 2;
        static final int TRANSACTION_onFinished = 3;
        static final int TRANSACTION_onProgress = 1;
        static final int TRANSACTION_onScreenshotTaken = 4;
        static final int TRANSACTION_onUiIntensiveBugreportDumpsFinished = 5;

        public Stub() {
            attachInterface(this, IDumpstateListener.DESCRIPTOR);
        }

        public static IDumpstateListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IDumpstateListener.DESCRIPTOR);
            if (iin != null && (iin instanceof IDumpstateListener)) {
                return (IDumpstateListener) iin;
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
                    return "onProgress";
                case 2:
                    return "onError";
                case 3:
                    return "onFinished";
                case 4:
                    return "onScreenshotTaken";
                case 5:
                    return "onUiIntensiveBugreportDumpsFinished";
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
                data.enforceInterface(IDumpstateListener.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IDumpstateListener.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            data.enforceNoDataAvail();
                            onProgress(_arg0);
                            break;
                        case 2:
                            int _arg02 = data.readInt();
                            data.enforceNoDataAvail();
                            onError(_arg02);
                            break;
                        case 3:
                            onFinished();
                            break;
                        case 4:
                            boolean _arg03 = data.readBoolean();
                            data.enforceNoDataAvail();
                            onScreenshotTaken(_arg03);
                            break;
                        case 5:
                            onUiIntensiveBugreportDumpsFinished();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IDumpstateListener {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IDumpstateListener.DESCRIPTOR;
            }

            @Override // android.os.IDumpstateListener
            public void onProgress(int progress) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDumpstateListener.DESCRIPTOR);
                    _data.writeInt(progress);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.os.IDumpstateListener
            public void onError(int errorCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDumpstateListener.DESCRIPTOR);
                    _data.writeInt(errorCode);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.os.IDumpstateListener
            public void onFinished() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDumpstateListener.DESCRIPTOR);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.os.IDumpstateListener
            public void onScreenshotTaken(boolean success) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDumpstateListener.DESCRIPTOR);
                    _data.writeBoolean(success);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.os.IDumpstateListener
            public void onUiIntensiveBugreportDumpsFinished() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDumpstateListener.DESCRIPTOR);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 4;
        }
    }
}
