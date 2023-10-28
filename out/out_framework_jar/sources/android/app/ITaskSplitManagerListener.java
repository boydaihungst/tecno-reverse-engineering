package android.app;

import android.app.ITaskSplitManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface ITaskSplitManagerListener extends IInterface {
    public static final String DESCRIPTOR = "android.app.ITaskSplitManagerListener";

    void onTaskSplitManagerConnected(ITaskSplitManager iTaskSplitManager) throws RemoteException;

    void onTaskSplitManagerDisconnected() throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements ITaskSplitManagerListener {
        @Override // android.app.ITaskSplitManagerListener
        public void onTaskSplitManagerConnected(ITaskSplitManager taskSplitManager) throws RemoteException {
        }

        @Override // android.app.ITaskSplitManagerListener
        public void onTaskSplitManagerDisconnected() throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements ITaskSplitManagerListener {
        static final int TRANSACTION_onTaskSplitManagerConnected = 1;
        static final int TRANSACTION_onTaskSplitManagerDisconnected = 2;

        public Stub() {
            attachInterface(this, ITaskSplitManagerListener.DESCRIPTOR);
        }

        public static ITaskSplitManagerListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ITaskSplitManagerListener.DESCRIPTOR);
            if (iin != null && (iin instanceof ITaskSplitManagerListener)) {
                return (ITaskSplitManagerListener) iin;
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
                    return "onTaskSplitManagerConnected";
                case 2:
                    return "onTaskSplitManagerDisconnected";
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
                data.enforceInterface(ITaskSplitManagerListener.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ITaskSplitManagerListener.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            ITaskSplitManager _arg0 = ITaskSplitManager.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            onTaskSplitManagerConnected(_arg0);
                            break;
                        case 2:
                            onTaskSplitManagerDisconnected();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements ITaskSplitManagerListener {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ITaskSplitManagerListener.DESCRIPTOR;
            }

            @Override // android.app.ITaskSplitManagerListener
            public void onTaskSplitManagerConnected(ITaskSplitManager taskSplitManager) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskSplitManagerListener.DESCRIPTOR);
                    _data.writeStrongInterface(taskSplitManager);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.ITaskSplitManagerListener
            public void onTaskSplitManagerDisconnected() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskSplitManagerListener.DESCRIPTOR);
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
