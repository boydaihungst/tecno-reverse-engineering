package android.window;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.IRemoteAnimationFinishedCallback;
import android.view.SurfaceControl;
/* loaded from: classes4.dex */
public interface IWindowContainerTransactionCallbackWithFinishCB extends IInterface {
    public static final String DESCRIPTOR = "android.window.IWindowContainerTransactionCallbackWithFinishCB";

    void onSendFinishCallback(IRemoteAnimationFinishedCallback iRemoteAnimationFinishedCallback) throws RemoteException;

    void onTransactionReady(int i, SurfaceControl.Transaction transaction) throws RemoteException;

    /* loaded from: classes4.dex */
    public static class Default implements IWindowContainerTransactionCallbackWithFinishCB {
        @Override // android.window.IWindowContainerTransactionCallbackWithFinishCB
        public void onTransactionReady(int id, SurfaceControl.Transaction t) throws RemoteException {
        }

        @Override // android.window.IWindowContainerTransactionCallbackWithFinishCB
        public void onSendFinishCallback(IRemoteAnimationFinishedCallback finishCallback) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class Stub extends Binder implements IWindowContainerTransactionCallbackWithFinishCB {
        static final int TRANSACTION_onSendFinishCallback = 2;
        static final int TRANSACTION_onTransactionReady = 1;

        public Stub() {
            attachInterface(this, IWindowContainerTransactionCallbackWithFinishCB.DESCRIPTOR);
        }

        public static IWindowContainerTransactionCallbackWithFinishCB asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IWindowContainerTransactionCallbackWithFinishCB.DESCRIPTOR);
            if (iin != null && (iin instanceof IWindowContainerTransactionCallbackWithFinishCB)) {
                return (IWindowContainerTransactionCallbackWithFinishCB) iin;
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
                    return "onTransactionReady";
                case 2:
                    return "onSendFinishCallback";
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
                data.enforceInterface(IWindowContainerTransactionCallbackWithFinishCB.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IWindowContainerTransactionCallbackWithFinishCB.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            SurfaceControl.Transaction _arg1 = (SurfaceControl.Transaction) data.readTypedObject(SurfaceControl.Transaction.CREATOR);
                            data.enforceNoDataAvail();
                            onTransactionReady(_arg0, _arg1);
                            reply.writeNoException();
                            break;
                        case 2:
                            IRemoteAnimationFinishedCallback _arg02 = IRemoteAnimationFinishedCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            onSendFinishCallback(_arg02);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes4.dex */
        private static class Proxy implements IWindowContainerTransactionCallbackWithFinishCB {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IWindowContainerTransactionCallbackWithFinishCB.DESCRIPTOR;
            }

            @Override // android.window.IWindowContainerTransactionCallbackWithFinishCB
            public void onTransactionReady(int id, SurfaceControl.Transaction t) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IWindowContainerTransactionCallbackWithFinishCB.DESCRIPTOR);
                    _data.writeInt(id);
                    _data.writeTypedObject(t, 0);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.window.IWindowContainerTransactionCallbackWithFinishCB
            public void onSendFinishCallback(IRemoteAnimationFinishedCallback finishCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IWindowContainerTransactionCallbackWithFinishCB.DESCRIPTOR);
                    _data.writeStrongInterface(finishCallback);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
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
