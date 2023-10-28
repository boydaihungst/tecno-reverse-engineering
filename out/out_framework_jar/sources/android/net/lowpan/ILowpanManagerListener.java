package android.net.lowpan;

import android.net.lowpan.ILowpanInterface;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes2.dex */
public interface ILowpanManagerListener extends IInterface {
    public static final String DESCRIPTOR = "android.net.lowpan.ILowpanManagerListener";

    void onInterfaceAdded(ILowpanInterface iLowpanInterface) throws RemoteException;

    void onInterfaceRemoved(ILowpanInterface iLowpanInterface) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements ILowpanManagerListener {
        @Override // android.net.lowpan.ILowpanManagerListener
        public void onInterfaceAdded(ILowpanInterface lowpanInterface) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanManagerListener
        public void onInterfaceRemoved(ILowpanInterface lowpanInterface) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements ILowpanManagerListener {
        static final int TRANSACTION_onInterfaceAdded = 1;
        static final int TRANSACTION_onInterfaceRemoved = 2;

        public Stub() {
            attachInterface(this, ILowpanManagerListener.DESCRIPTOR);
        }

        public static ILowpanManagerListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ILowpanManagerListener.DESCRIPTOR);
            if (iin != null && (iin instanceof ILowpanManagerListener)) {
                return (ILowpanManagerListener) iin;
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
                    return "onInterfaceAdded";
                case 2:
                    return "onInterfaceRemoved";
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
                data.enforceInterface(ILowpanManagerListener.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ILowpanManagerListener.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            ILowpanInterface _arg0 = ILowpanInterface.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            onInterfaceAdded(_arg0);
                            break;
                        case 2:
                            ILowpanInterface _arg02 = ILowpanInterface.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            onInterfaceRemoved(_arg02);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements ILowpanManagerListener {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ILowpanManagerListener.DESCRIPTOR;
            }

            @Override // android.net.lowpan.ILowpanManagerListener
            public void onInterfaceAdded(ILowpanInterface lowpanInterface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanManagerListener.DESCRIPTOR);
                    _data.writeStrongInterface(lowpanInterface);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanManagerListener
            public void onInterfaceRemoved(ILowpanInterface lowpanInterface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanManagerListener.DESCRIPTOR);
                    _data.writeStrongInterface(lowpanInterface);
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
