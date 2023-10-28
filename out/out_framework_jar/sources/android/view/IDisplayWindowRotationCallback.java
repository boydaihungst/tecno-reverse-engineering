package android.view;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.window.WindowContainerTransaction;
/* loaded from: classes3.dex */
public interface IDisplayWindowRotationCallback extends IInterface {
    public static final String DESCRIPTOR = "android.view.IDisplayWindowRotationCallback";

    void continueRotateDisplay(int i, WindowContainerTransaction windowContainerTransaction) throws RemoteException;

    /* loaded from: classes3.dex */
    public static class Default implements IDisplayWindowRotationCallback {
        @Override // android.view.IDisplayWindowRotationCallback
        public void continueRotateDisplay(int targetRotation, WindowContainerTransaction t) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes3.dex */
    public static abstract class Stub extends Binder implements IDisplayWindowRotationCallback {
        static final int TRANSACTION_continueRotateDisplay = 1;

        public Stub() {
            attachInterface(this, IDisplayWindowRotationCallback.DESCRIPTOR);
        }

        public static IDisplayWindowRotationCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IDisplayWindowRotationCallback.DESCRIPTOR);
            if (iin != null && (iin instanceof IDisplayWindowRotationCallback)) {
                return (IDisplayWindowRotationCallback) iin;
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
                    return "continueRotateDisplay";
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
                data.enforceInterface(IDisplayWindowRotationCallback.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IDisplayWindowRotationCallback.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            WindowContainerTransaction _arg1 = (WindowContainerTransaction) data.readTypedObject(WindowContainerTransaction.CREATOR);
                            data.enforceNoDataAvail();
                            continueRotateDisplay(_arg0, _arg1);
                            reply.writeNoException();
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes3.dex */
        private static class Proxy implements IDisplayWindowRotationCallback {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IDisplayWindowRotationCallback.DESCRIPTOR;
            }

            @Override // android.view.IDisplayWindowRotationCallback
            public void continueRotateDisplay(int targetRotation, WindowContainerTransaction t) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDisplayWindowRotationCallback.DESCRIPTOR);
                    _data.writeInt(targetRotation);
                    _data.writeTypedObject(t, 0);
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
