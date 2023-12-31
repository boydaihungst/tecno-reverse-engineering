package android.hardware.devicestate;

import android.hardware.devicestate.IDeviceStateManagerCallback;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IDeviceStateManager extends IInterface {
    public static final String DESCRIPTOR = "android.hardware.devicestate.IDeviceStateManager";

    void cancelStateRequest() throws RemoteException;

    DeviceStateInfo getDeviceStateInfo() throws RemoteException;

    void registerCallback(IDeviceStateManagerCallback iDeviceStateManagerCallback) throws RemoteException;

    void requestState(IBinder iBinder, int i, int i2) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IDeviceStateManager {
        @Override // android.hardware.devicestate.IDeviceStateManager
        public DeviceStateInfo getDeviceStateInfo() throws RemoteException {
            return null;
        }

        @Override // android.hardware.devicestate.IDeviceStateManager
        public void registerCallback(IDeviceStateManagerCallback callback) throws RemoteException {
        }

        @Override // android.hardware.devicestate.IDeviceStateManager
        public void requestState(IBinder token, int state, int flags) throws RemoteException {
        }

        @Override // android.hardware.devicestate.IDeviceStateManager
        public void cancelStateRequest() throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IDeviceStateManager {
        static final int TRANSACTION_cancelStateRequest = 4;
        static final int TRANSACTION_getDeviceStateInfo = 1;
        static final int TRANSACTION_registerCallback = 2;
        static final int TRANSACTION_requestState = 3;

        public Stub() {
            attachInterface(this, IDeviceStateManager.DESCRIPTOR);
        }

        public static IDeviceStateManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IDeviceStateManager.DESCRIPTOR);
            if (iin != null && (iin instanceof IDeviceStateManager)) {
                return (IDeviceStateManager) iin;
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
                    return "getDeviceStateInfo";
                case 2:
                    return "registerCallback";
                case 3:
                    return "requestState";
                case 4:
                    return "cancelStateRequest";
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
                data.enforceInterface(IDeviceStateManager.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IDeviceStateManager.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            DeviceStateInfo _result = getDeviceStateInfo();
                            reply.writeNoException();
                            reply.writeTypedObject(_result, 1);
                            break;
                        case 2:
                            IBinder _arg0 = data.readStrongBinder();
                            IDeviceStateManagerCallback _arg02 = IDeviceStateManagerCallback.Stub.asInterface(_arg0);
                            data.enforceNoDataAvail();
                            registerCallback(_arg02);
                            reply.writeNoException();
                            break;
                        case 3:
                            IBinder _arg03 = data.readStrongBinder();
                            int _arg1 = data.readInt();
                            int _arg2 = data.readInt();
                            data.enforceNoDataAvail();
                            requestState(_arg03, _arg1, _arg2);
                            reply.writeNoException();
                            break;
                        case 4:
                            cancelStateRequest();
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IDeviceStateManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IDeviceStateManager.DESCRIPTOR;
            }

            @Override // android.hardware.devicestate.IDeviceStateManager
            public DeviceStateInfo getDeviceStateInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDeviceStateManager.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    DeviceStateInfo _result = (DeviceStateInfo) _reply.readTypedObject(DeviceStateInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.devicestate.IDeviceStateManager
            public void registerCallback(IDeviceStateManagerCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDeviceStateManager.DESCRIPTOR);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.devicestate.IDeviceStateManager
            public void requestState(IBinder token, int state, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDeviceStateManager.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(state);
                    _data.writeInt(flags);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.devicestate.IDeviceStateManager
            public void cancelStateRequest() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDeviceStateManager.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 3;
        }
    }
}
