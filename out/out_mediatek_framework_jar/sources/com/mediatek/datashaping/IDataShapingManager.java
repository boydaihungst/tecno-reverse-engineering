package com.mediatek.datashaping;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IDataShapingManager extends IInterface {
    public static final String DESCRIPTOR = "com.mediatek.datashaping.IDataShapingManager";

    void disableDataShaping() throws RemoteException;

    void enableDataShaping() throws RemoteException;

    boolean isDataShapingWhitelistApp(String str) throws RemoteException;

    boolean openLteDataUpLinkGate(boolean z) throws RemoteException;

    void setDeviceIdleMode(boolean z) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IDataShapingManager {
        @Override // com.mediatek.datashaping.IDataShapingManager
        public void enableDataShaping() throws RemoteException {
        }

        @Override // com.mediatek.datashaping.IDataShapingManager
        public void disableDataShaping() throws RemoteException {
        }

        @Override // com.mediatek.datashaping.IDataShapingManager
        public boolean openLteDataUpLinkGate(boolean isForce) throws RemoteException {
            return false;
        }

        @Override // com.mediatek.datashaping.IDataShapingManager
        public void setDeviceIdleMode(boolean enabled) throws RemoteException {
        }

        @Override // com.mediatek.datashaping.IDataShapingManager
        public boolean isDataShapingWhitelistApp(String packageName) throws RemoteException {
            return false;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IDataShapingManager {
        static final int TRANSACTION_disableDataShaping = 2;
        static final int TRANSACTION_enableDataShaping = 1;
        static final int TRANSACTION_isDataShapingWhitelistApp = 5;
        static final int TRANSACTION_openLteDataUpLinkGate = 3;
        static final int TRANSACTION_setDeviceIdleMode = 4;

        public Stub() {
            attachInterface(this, IDataShapingManager.DESCRIPTOR);
        }

        public static IDataShapingManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IDataShapingManager.DESCRIPTOR);
            if (iin != null && (iin instanceof IDataShapingManager)) {
                return (IDataShapingManager) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(IDataShapingManager.DESCRIPTOR);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(IDataShapingManager.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            enableDataShaping();
                            reply.writeNoException();
                            break;
                        case 2:
                            disableDataShaping();
                            reply.writeNoException();
                            break;
                        case 3:
                            boolean _arg0 = data.readBoolean();
                            data.enforceNoDataAvail();
                            boolean _result = openLteDataUpLinkGate(_arg0);
                            reply.writeNoException();
                            reply.writeBoolean(_result);
                            break;
                        case 4:
                            boolean _arg02 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setDeviceIdleMode(_arg02);
                            reply.writeNoException();
                            break;
                        case 5:
                            String _arg03 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result2 = isDataShapingWhitelistApp(_arg03);
                            reply.writeNoException();
                            reply.writeBoolean(_result2);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IDataShapingManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IDataShapingManager.DESCRIPTOR;
            }

            @Override // com.mediatek.datashaping.IDataShapingManager
            public void enableDataShaping() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDataShapingManager.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.datashaping.IDataShapingManager
            public void disableDataShaping() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDataShapingManager.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.datashaping.IDataShapingManager
            public boolean openLteDataUpLinkGate(boolean isForce) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDataShapingManager.DESCRIPTOR);
                    _data.writeBoolean(isForce);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.datashaping.IDataShapingManager
            public void setDeviceIdleMode(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDataShapingManager.DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.datashaping.IDataShapingManager
            public boolean isDataShapingWhitelistApp(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDataShapingManager.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
