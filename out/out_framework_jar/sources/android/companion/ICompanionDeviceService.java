package android.companion;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface ICompanionDeviceService extends IInterface {
    public static final String DESCRIPTOR = "android.companion.ICompanionDeviceService";

    void onDeviceAppeared(AssociationInfo associationInfo) throws RemoteException;

    void onDeviceDisappeared(AssociationInfo associationInfo) throws RemoteException;

    void onDispatchMessage(int i, int i2, byte[] bArr) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements ICompanionDeviceService {
        @Override // android.companion.ICompanionDeviceService
        public void onDeviceAppeared(AssociationInfo associationInfo) throws RemoteException {
        }

        @Override // android.companion.ICompanionDeviceService
        public void onDeviceDisappeared(AssociationInfo associationInfo) throws RemoteException {
        }

        @Override // android.companion.ICompanionDeviceService
        public void onDispatchMessage(int messageId, int associationId, byte[] message) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements ICompanionDeviceService {
        static final int TRANSACTION_onDeviceAppeared = 1;
        static final int TRANSACTION_onDeviceDisappeared = 2;
        static final int TRANSACTION_onDispatchMessage = 3;

        public Stub() {
            attachInterface(this, ICompanionDeviceService.DESCRIPTOR);
        }

        public static ICompanionDeviceService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ICompanionDeviceService.DESCRIPTOR);
            if (iin != null && (iin instanceof ICompanionDeviceService)) {
                return (ICompanionDeviceService) iin;
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
                    return "onDeviceAppeared";
                case 2:
                    return "onDeviceDisappeared";
                case 3:
                    return "onDispatchMessage";
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
                data.enforceInterface(ICompanionDeviceService.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ICompanionDeviceService.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            AssociationInfo _arg0 = (AssociationInfo) data.readTypedObject(AssociationInfo.CREATOR);
                            data.enforceNoDataAvail();
                            onDeviceAppeared(_arg0);
                            break;
                        case 2:
                            AssociationInfo _arg02 = (AssociationInfo) data.readTypedObject(AssociationInfo.CREATOR);
                            data.enforceNoDataAvail();
                            onDeviceDisappeared(_arg02);
                            break;
                        case 3:
                            int _arg03 = data.readInt();
                            int _arg1 = data.readInt();
                            byte[] _arg2 = data.createByteArray();
                            data.enforceNoDataAvail();
                            onDispatchMessage(_arg03, _arg1, _arg2);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements ICompanionDeviceService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ICompanionDeviceService.DESCRIPTOR;
            }

            @Override // android.companion.ICompanionDeviceService
            public void onDeviceAppeared(AssociationInfo associationInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ICompanionDeviceService.DESCRIPTOR);
                    _data.writeTypedObject(associationInfo, 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.companion.ICompanionDeviceService
            public void onDeviceDisappeared(AssociationInfo associationInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ICompanionDeviceService.DESCRIPTOR);
                    _data.writeTypedObject(associationInfo, 0);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.companion.ICompanionDeviceService
            public void onDispatchMessage(int messageId, int associationId, byte[] message) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ICompanionDeviceService.DESCRIPTOR);
                    _data.writeInt(messageId);
                    _data.writeInt(associationId);
                    _data.writeByteArray(message);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 2;
        }
    }
}
