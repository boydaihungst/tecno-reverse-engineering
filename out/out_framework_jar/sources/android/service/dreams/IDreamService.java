package android.service.dreams;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.IRemoteCallback;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes3.dex */
public interface IDreamService extends IInterface {
    void attach(IBinder iBinder, boolean z, IRemoteCallback iRemoteCallback) throws RemoteException;

    void detach() throws RemoteException;

    void notifyAodAction(int i) throws RemoteException;

    void wakeUp() throws RemoteException;

    /* loaded from: classes3.dex */
    public static class Default implements IDreamService {
        @Override // android.service.dreams.IDreamService
        public void attach(IBinder windowToken, boolean canDoze, IRemoteCallback started) throws RemoteException {
        }

        @Override // android.service.dreams.IDreamService
        public void detach() throws RemoteException {
        }

        @Override // android.service.dreams.IDreamService
        public void wakeUp() throws RemoteException {
        }

        @Override // android.service.dreams.IDreamService
        public void notifyAodAction(int state) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes3.dex */
    public static abstract class Stub extends Binder implements IDreamService {
        public static final String DESCRIPTOR = "android.service.dreams.IDreamService";
        static final int TRANSACTION_attach = 1;
        static final int TRANSACTION_detach = 2;
        static final int TRANSACTION_notifyAodAction = 4;
        static final int TRANSACTION_wakeUp = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IDreamService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IDreamService)) {
                return (IDreamService) iin;
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
                    return "attach";
                case 2:
                    return "detach";
                case 3:
                    return "wakeUp";
                case 4:
                    return "notifyAodAction";
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
                data.enforceInterface(DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            IBinder _arg0 = data.readStrongBinder();
                            boolean _arg1 = data.readBoolean();
                            IRemoteCallback _arg2 = IRemoteCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            attach(_arg0, _arg1, _arg2);
                            break;
                        case 2:
                            detach();
                            break;
                        case 3:
                            wakeUp();
                            break;
                        case 4:
                            int _arg02 = data.readInt();
                            data.enforceNoDataAvail();
                            notifyAodAction(_arg02);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes3.dex */
        private static class Proxy implements IDreamService {
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

            @Override // android.service.dreams.IDreamService
            public void attach(IBinder windowToken, boolean canDoze, IRemoteCallback started) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(windowToken);
                    _data.writeBoolean(canDoze);
                    _data.writeStrongInterface(started);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.service.dreams.IDreamService
            public void detach() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.service.dreams.IDreamService
            public void wakeUp() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.service.dreams.IDreamService
            public void notifyAodAction(int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(state);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 3;
        }
    }
}
