package android.service.games;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes3.dex */
public interface IGameServiceController extends IInterface {
    public static final String DESCRIPTOR = "android.service.games.IGameServiceController";

    void createGameSession(int i) throws RemoteException;

    /* loaded from: classes3.dex */
    public static class Default implements IGameServiceController {
        @Override // android.service.games.IGameServiceController
        public void createGameSession(int taskId) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes3.dex */
    public static abstract class Stub extends Binder implements IGameServiceController {
        static final int TRANSACTION_createGameSession = 1;

        public Stub() {
            attachInterface(this, IGameServiceController.DESCRIPTOR);
        }

        public static IGameServiceController asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IGameServiceController.DESCRIPTOR);
            if (iin != null && (iin instanceof IGameServiceController)) {
                return (IGameServiceController) iin;
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
                    return "createGameSession";
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
                data.enforceInterface(IGameServiceController.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IGameServiceController.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            data.enforceNoDataAvail();
                            createGameSession(_arg0);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes3.dex */
        private static class Proxy implements IGameServiceController {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IGameServiceController.DESCRIPTOR;
            }

            @Override // android.service.games.IGameServiceController
            public void createGameSession(int taskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IGameServiceController.DESCRIPTOR);
                    _data.writeInt(taskId);
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
