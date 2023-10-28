package com.transsion.foldable;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes4.dex */
public interface ITranFoldingScreen extends IInterface {
    public static final String DESCRIPTOR = "com.transsion.foldable.ITranFoldingScreen";

    int getCompatibleModeDefaultValue(String str) throws RemoteException;

    void removeTaskWhileModeChanged(String str, int i, String str2) throws RemoteException;

    /* loaded from: classes4.dex */
    public static class Default implements ITranFoldingScreen {
        @Override // com.transsion.foldable.ITranFoldingScreen
        public int getCompatibleModeDefaultValue(String packageName) throws RemoteException {
            return 0;
        }

        @Override // com.transsion.foldable.ITranFoldingScreen
        public void removeTaskWhileModeChanged(String packageName, int userId, String reason) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class Stub extends Binder implements ITranFoldingScreen {
        static final int TRANSACTION_getCompatibleModeDefaultValue = 1;
        static final int TRANSACTION_removeTaskWhileModeChanged = 2;

        public Stub() {
            attachInterface(this, ITranFoldingScreen.DESCRIPTOR);
        }

        public static ITranFoldingScreen asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ITranFoldingScreen.DESCRIPTOR);
            if (iin != null && (iin instanceof ITranFoldingScreen)) {
                return (ITranFoldingScreen) iin;
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
                    return "getCompatibleModeDefaultValue";
                case 2:
                    return "removeTaskWhileModeChanged";
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
                data.enforceInterface(ITranFoldingScreen.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ITranFoldingScreen.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            String _arg0 = data.readString();
                            data.enforceNoDataAvail();
                            int _result = getCompatibleModeDefaultValue(_arg0);
                            reply.writeNoException();
                            reply.writeInt(_result);
                            break;
                        case 2:
                            String _arg02 = data.readString();
                            int _arg1 = data.readInt();
                            String _arg2 = data.readString();
                            data.enforceNoDataAvail();
                            removeTaskWhileModeChanged(_arg02, _arg1, _arg2);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes4.dex */
        private static class Proxy implements ITranFoldingScreen {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ITranFoldingScreen.DESCRIPTOR;
            }

            @Override // com.transsion.foldable.ITranFoldingScreen
            public int getCompatibleModeDefaultValue(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITranFoldingScreen.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.transsion.foldable.ITranFoldingScreen
            public void removeTaskWhileModeChanged(String packageName, int userId, String reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITranFoldingScreen.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    _data.writeString(reason);
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
