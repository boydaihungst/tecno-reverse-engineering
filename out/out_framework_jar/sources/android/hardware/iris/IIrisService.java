package android.hardware.iris;

import android.hardware.biometrics.SensorPropertiesInternal;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;
/* loaded from: classes2.dex */
public interface IIrisService extends IInterface {
    public static final String DESCRIPTOR = "android.hardware.iris.IIrisService";

    void registerAuthenticators(List<SensorPropertiesInternal> list) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IIrisService {
        @Override // android.hardware.iris.IIrisService
        public void registerAuthenticators(List<SensorPropertiesInternal> hidlSensors) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IIrisService {
        static final int TRANSACTION_registerAuthenticators = 1;

        public Stub() {
            attachInterface(this, IIrisService.DESCRIPTOR);
        }

        public static IIrisService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IIrisService.DESCRIPTOR);
            if (iin != null && (iin instanceof IIrisService)) {
                return (IIrisService) iin;
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
                    return "registerAuthenticators";
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
                data.enforceInterface(IIrisService.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IIrisService.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            List<SensorPropertiesInternal> _arg0 = data.createTypedArrayList(SensorPropertiesInternal.CREATOR);
                            data.enforceNoDataAvail();
                            registerAuthenticators(_arg0);
                            reply.writeNoException();
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IIrisService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IIrisService.DESCRIPTOR;
            }

            @Override // android.hardware.iris.IIrisService
            public void registerAuthenticators(List<SensorPropertiesInternal> hidlSensors) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IIrisService.DESCRIPTOR);
                    _data.writeTypedList(hidlSensors);
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
