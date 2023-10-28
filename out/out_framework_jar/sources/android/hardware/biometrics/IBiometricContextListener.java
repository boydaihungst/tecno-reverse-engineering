package android.hardware.biometrics;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IBiometricContextListener extends IInterface {
    public static final String DESCRIPTOR = "android.hardware.biometrics.IBiometricContextListener";

    void onDozeChanged(boolean z) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IBiometricContextListener {
        @Override // android.hardware.biometrics.IBiometricContextListener
        public void onDozeChanged(boolean isDozing) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IBiometricContextListener {
        static final int TRANSACTION_onDozeChanged = 1;

        public Stub() {
            attachInterface(this, IBiometricContextListener.DESCRIPTOR);
        }

        public static IBiometricContextListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IBiometricContextListener.DESCRIPTOR);
            if (iin != null && (iin instanceof IBiometricContextListener)) {
                return (IBiometricContextListener) iin;
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
                    return "onDozeChanged";
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
                data.enforceInterface(IBiometricContextListener.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IBiometricContextListener.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            boolean _arg0 = data.readBoolean();
                            data.enforceNoDataAvail();
                            onDozeChanged(_arg0);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IBiometricContextListener {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IBiometricContextListener.DESCRIPTOR;
            }

            @Override // android.hardware.biometrics.IBiometricContextListener
            public void onDozeChanged(boolean isDozing) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IBiometricContextListener.DESCRIPTOR);
                    _data.writeBoolean(isDozing);
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
