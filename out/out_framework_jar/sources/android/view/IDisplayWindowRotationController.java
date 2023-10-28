package android.view;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.IDisplayWindowRotationCallback;
/* loaded from: classes3.dex */
public interface IDisplayWindowRotationController extends IInterface {
    public static final String DESCRIPTOR = "android.view.IDisplayWindowRotationController";

    void onRotateDisplay(int i, int i2, int i3, IDisplayWindowRotationCallback iDisplayWindowRotationCallback) throws RemoteException;

    /* loaded from: classes3.dex */
    public static class Default implements IDisplayWindowRotationController {
        @Override // android.view.IDisplayWindowRotationController
        public void onRotateDisplay(int displayId, int fromRotation, int toRotation, IDisplayWindowRotationCallback callback) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes3.dex */
    public static abstract class Stub extends Binder implements IDisplayWindowRotationController {
        static final int TRANSACTION_onRotateDisplay = 1;

        public Stub() {
            attachInterface(this, IDisplayWindowRotationController.DESCRIPTOR);
        }

        public static IDisplayWindowRotationController asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IDisplayWindowRotationController.DESCRIPTOR);
            if (iin != null && (iin instanceof IDisplayWindowRotationController)) {
                return (IDisplayWindowRotationController) iin;
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
                    return "onRotateDisplay";
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
                data.enforceInterface(IDisplayWindowRotationController.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IDisplayWindowRotationController.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            int _arg1 = data.readInt();
                            int _arg2 = data.readInt();
                            IDisplayWindowRotationCallback _arg3 = IDisplayWindowRotationCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            onRotateDisplay(_arg0, _arg1, _arg2, _arg3);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes3.dex */
        public static class Proxy implements IDisplayWindowRotationController {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IDisplayWindowRotationController.DESCRIPTOR;
            }

            @Override // android.view.IDisplayWindowRotationController
            public void onRotateDisplay(int displayId, int fromRotation, int toRotation, IDisplayWindowRotationCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDisplayWindowRotationController.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeInt(fromRotation);
                    _data.writeInt(toRotation);
                    _data.writeStrongInterface(callback);
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
