package android.service.games;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.internal.infra.AndroidFuture;
/* loaded from: classes3.dex */
public interface IGameSessionController extends IInterface {
    public static final String DESCRIPTOR = "android.service.games.IGameSessionController";

    void restartGame(int i) throws RemoteException;

    void takeScreenshot(int i, AndroidFuture androidFuture) throws RemoteException;

    /* loaded from: classes3.dex */
    public static class Default implements IGameSessionController {
        @Override // android.service.games.IGameSessionController
        public void takeScreenshot(int taskId, AndroidFuture gameScreenshotResultFuture) throws RemoteException {
        }

        @Override // android.service.games.IGameSessionController
        public void restartGame(int taskId) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes3.dex */
    public static abstract class Stub extends Binder implements IGameSessionController {
        static final int TRANSACTION_restartGame = 2;
        static final int TRANSACTION_takeScreenshot = 1;

        public Stub() {
            attachInterface(this, IGameSessionController.DESCRIPTOR);
        }

        public static IGameSessionController asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IGameSessionController.DESCRIPTOR);
            if (iin != null && (iin instanceof IGameSessionController)) {
                return (IGameSessionController) iin;
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
                    return "takeScreenshot";
                case 2:
                    return "restartGame";
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
                data.enforceInterface(IGameSessionController.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IGameSessionController.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            AndroidFuture _arg1 = (AndroidFuture) data.readTypedObject(AndroidFuture.CREATOR);
                            data.enforceNoDataAvail();
                            takeScreenshot(_arg0, _arg1);
                            break;
                        case 2:
                            int _arg02 = data.readInt();
                            data.enforceNoDataAvail();
                            restartGame(_arg02);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes3.dex */
        private static class Proxy implements IGameSessionController {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IGameSessionController.DESCRIPTOR;
            }

            @Override // android.service.games.IGameSessionController
            public void takeScreenshot(int taskId, AndroidFuture gameScreenshotResultFuture) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IGameSessionController.DESCRIPTOR);
                    _data.writeInt(taskId);
                    _data.writeTypedObject(gameScreenshotResultFuture, 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.service.games.IGameSessionController
            public void restartGame(int taskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IGameSessionController.DESCRIPTOR);
                    _data.writeInt(taskId);
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
