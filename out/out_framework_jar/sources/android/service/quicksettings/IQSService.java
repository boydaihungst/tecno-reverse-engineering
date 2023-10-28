package android.service.quicksettings;

import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes3.dex */
public interface IQSService extends IInterface {
    Tile getTile(IBinder iBinder) throws RemoteException;

    boolean isLocked() throws RemoteException;

    boolean isSecure() throws RemoteException;

    void onDialogHidden(IBinder iBinder) throws RemoteException;

    void onShowDialog(IBinder iBinder) throws RemoteException;

    void onStartActivity(IBinder iBinder) throws RemoteException;

    void onStartSuccessful(IBinder iBinder) throws RemoteException;

    void startUnlockAndRun(IBinder iBinder) throws RemoteException;

    void updateQsTile(Tile tile, IBinder iBinder) throws RemoteException;

    void updateStatusIcon(IBinder iBinder, Icon icon, String str) throws RemoteException;

    /* loaded from: classes3.dex */
    public static class Default implements IQSService {
        @Override // android.service.quicksettings.IQSService
        public Tile getTile(IBinder tile) throws RemoteException {
            return null;
        }

        @Override // android.service.quicksettings.IQSService
        public void updateQsTile(Tile tile, IBinder service) throws RemoteException {
        }

        @Override // android.service.quicksettings.IQSService
        public void updateStatusIcon(IBinder tile, Icon icon, String contentDescription) throws RemoteException {
        }

        @Override // android.service.quicksettings.IQSService
        public void onShowDialog(IBinder tile) throws RemoteException {
        }

        @Override // android.service.quicksettings.IQSService
        public void onStartActivity(IBinder tile) throws RemoteException {
        }

        @Override // android.service.quicksettings.IQSService
        public boolean isLocked() throws RemoteException {
            return false;
        }

        @Override // android.service.quicksettings.IQSService
        public boolean isSecure() throws RemoteException {
            return false;
        }

        @Override // android.service.quicksettings.IQSService
        public void startUnlockAndRun(IBinder tile) throws RemoteException {
        }

        @Override // android.service.quicksettings.IQSService
        public void onDialogHidden(IBinder tile) throws RemoteException {
        }

        @Override // android.service.quicksettings.IQSService
        public void onStartSuccessful(IBinder tile) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes3.dex */
    public static abstract class Stub extends Binder implements IQSService {
        public static final String DESCRIPTOR = "android.service.quicksettings.IQSService";
        static final int TRANSACTION_getTile = 1;
        static final int TRANSACTION_isLocked = 6;
        static final int TRANSACTION_isSecure = 7;
        static final int TRANSACTION_onDialogHidden = 9;
        static final int TRANSACTION_onShowDialog = 4;
        static final int TRANSACTION_onStartActivity = 5;
        static final int TRANSACTION_onStartSuccessful = 10;
        static final int TRANSACTION_startUnlockAndRun = 8;
        static final int TRANSACTION_updateQsTile = 2;
        static final int TRANSACTION_updateStatusIcon = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IQSService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IQSService)) {
                return (IQSService) iin;
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
                    return "getTile";
                case 2:
                    return "updateQsTile";
                case 3:
                    return "updateStatusIcon";
                case 4:
                    return "onShowDialog";
                case 5:
                    return "onStartActivity";
                case 6:
                    return "isLocked";
                case 7:
                    return "isSecure";
                case 8:
                    return "startUnlockAndRun";
                case 9:
                    return "onDialogHidden";
                case 10:
                    return "onStartSuccessful";
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
                data.enforceInterface(DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            IBinder _arg0 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            Tile _result = getTile(_arg0);
                            reply.writeNoException();
                            reply.writeTypedObject(_result, 1);
                            break;
                        case 2:
                            Tile _arg02 = (Tile) data.readTypedObject(Tile.CREATOR);
                            IBinder _arg1 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            updateQsTile(_arg02, _arg1);
                            reply.writeNoException();
                            break;
                        case 3:
                            IBinder _arg03 = data.readStrongBinder();
                            Icon _arg12 = (Icon) data.readTypedObject(Icon.CREATOR);
                            String _arg2 = data.readString();
                            data.enforceNoDataAvail();
                            updateStatusIcon(_arg03, _arg12, _arg2);
                            reply.writeNoException();
                            break;
                        case 4:
                            IBinder _arg04 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            onShowDialog(_arg04);
                            reply.writeNoException();
                            break;
                        case 5:
                            IBinder _arg05 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            onStartActivity(_arg05);
                            reply.writeNoException();
                            break;
                        case 6:
                            boolean _result2 = isLocked();
                            reply.writeNoException();
                            reply.writeBoolean(_result2);
                            break;
                        case 7:
                            boolean _result3 = isSecure();
                            reply.writeNoException();
                            reply.writeBoolean(_result3);
                            break;
                        case 8:
                            IBinder _arg06 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            startUnlockAndRun(_arg06);
                            reply.writeNoException();
                            break;
                        case 9:
                            IBinder _arg07 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            onDialogHidden(_arg07);
                            reply.writeNoException();
                            break;
                        case 10:
                            IBinder _arg08 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            onStartSuccessful(_arg08);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes3.dex */
        private static class Proxy implements IQSService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override // android.service.quicksettings.IQSService
            public Tile getTile(IBinder tile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(tile);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    Tile _result = (Tile) _reply.readTypedObject(Tile.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.service.quicksettings.IQSService
            public void updateQsTile(Tile tile, IBinder service) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(tile, 0);
                    _data.writeStrongBinder(service);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.service.quicksettings.IQSService
            public void updateStatusIcon(IBinder tile, Icon icon, String contentDescription) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(tile);
                    _data.writeTypedObject(icon, 0);
                    _data.writeString(contentDescription);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.service.quicksettings.IQSService
            public void onShowDialog(IBinder tile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(tile);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.service.quicksettings.IQSService
            public void onStartActivity(IBinder tile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(tile);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.service.quicksettings.IQSService
            public boolean isLocked() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.service.quicksettings.IQSService
            public boolean isSecure() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.service.quicksettings.IQSService
            public void startUnlockAndRun(IBinder tile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(tile);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.service.quicksettings.IQSService
            public void onDialogHidden(IBinder tile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(tile);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.service.quicksettings.IQSService
            public void onStartSuccessful(IBinder tile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(tile);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 9;
        }
    }
}
