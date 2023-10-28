package android.companion.virtual;

import android.companion.virtual.IVirtualDevice;
import android.companion.virtual.IVirtualDeviceActivityListener;
import android.hardware.display.IVirtualDisplayCallback;
import android.hardware.display.VirtualDisplayConfig;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IVirtualDeviceManager extends IInterface {
    public static final String DESCRIPTOR = "android.companion.virtual.IVirtualDeviceManager";

    IVirtualDevice createVirtualDevice(IBinder iBinder, String str, int i, VirtualDeviceParams virtualDeviceParams, IVirtualDeviceActivityListener iVirtualDeviceActivityListener) throws RemoteException;

    int createVirtualDisplay(VirtualDisplayConfig virtualDisplayConfig, IVirtualDisplayCallback iVirtualDisplayCallback, IVirtualDevice iVirtualDevice, String str) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IVirtualDeviceManager {
        @Override // android.companion.virtual.IVirtualDeviceManager
        public IVirtualDevice createVirtualDevice(IBinder token, String packageName, int associationId, VirtualDeviceParams params, IVirtualDeviceActivityListener activityListener) throws RemoteException {
            return null;
        }

        @Override // android.companion.virtual.IVirtualDeviceManager
        public int createVirtualDisplay(VirtualDisplayConfig virtualDisplayConfig, IVirtualDisplayCallback callback, IVirtualDevice virtualDevice, String packageName) throws RemoteException {
            return 0;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IVirtualDeviceManager {
        static final int TRANSACTION_createVirtualDevice = 1;
        static final int TRANSACTION_createVirtualDisplay = 2;

        public Stub() {
            attachInterface(this, IVirtualDeviceManager.DESCRIPTOR);
        }

        public static IVirtualDeviceManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IVirtualDeviceManager.DESCRIPTOR);
            if (iin != null && (iin instanceof IVirtualDeviceManager)) {
                return (IVirtualDeviceManager) iin;
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
                    return "createVirtualDevice";
                case 2:
                    return "createVirtualDisplay";
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
                data.enforceInterface(IVirtualDeviceManager.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IVirtualDeviceManager.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            IBinder _arg0 = data.readStrongBinder();
                            String _arg1 = data.readString();
                            int _arg2 = data.readInt();
                            VirtualDeviceParams _arg3 = (VirtualDeviceParams) data.readTypedObject(VirtualDeviceParams.CREATOR);
                            IVirtualDeviceActivityListener _arg4 = IVirtualDeviceActivityListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            IVirtualDevice _result = createVirtualDevice(_arg0, _arg1, _arg2, _arg3, _arg4);
                            reply.writeNoException();
                            reply.writeStrongInterface(_result);
                            break;
                        case 2:
                            VirtualDisplayConfig _arg02 = (VirtualDisplayConfig) data.readTypedObject(VirtualDisplayConfig.CREATOR);
                            IVirtualDisplayCallback _arg12 = IVirtualDisplayCallback.Stub.asInterface(data.readStrongBinder());
                            IVirtualDevice _arg22 = IVirtualDevice.Stub.asInterface(data.readStrongBinder());
                            String _arg32 = data.readString();
                            data.enforceNoDataAvail();
                            int _result2 = createVirtualDisplay(_arg02, _arg12, _arg22, _arg32);
                            reply.writeNoException();
                            reply.writeInt(_result2);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements IVirtualDeviceManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IVirtualDeviceManager.DESCRIPTOR;
            }

            @Override // android.companion.virtual.IVirtualDeviceManager
            public IVirtualDevice createVirtualDevice(IBinder token, String packageName, int associationId, VirtualDeviceParams params, IVirtualDeviceActivityListener activityListener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IVirtualDeviceManager.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeString(packageName);
                    _data.writeInt(associationId);
                    _data.writeTypedObject(params, 0);
                    _data.writeStrongInterface(activityListener);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    IVirtualDevice _result = IVirtualDevice.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.virtual.IVirtualDeviceManager
            public int createVirtualDisplay(VirtualDisplayConfig virtualDisplayConfig, IVirtualDisplayCallback callback, IVirtualDevice virtualDevice, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IVirtualDeviceManager.DESCRIPTOR);
                    _data.writeTypedObject(virtualDisplayConfig, 0);
                    _data.writeStrongInterface(callback);
                    _data.writeStrongInterface(virtualDevice);
                    _data.writeString(packageName);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
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
