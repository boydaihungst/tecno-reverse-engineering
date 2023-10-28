package android.hardware;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface ICameraServiceProxy extends IInterface {
    int getRotateAndCropOverride(String str, int i, int i2) throws RemoteException;

    boolean isCameraDisabled() throws RemoteException;

    void notifyCameraState(CameraSessionStats cameraSessionStats) throws RemoteException;

    void pingForUserUpdate() throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements ICameraServiceProxy {
        @Override // android.hardware.ICameraServiceProxy
        public void pingForUserUpdate() throws RemoteException {
        }

        @Override // android.hardware.ICameraServiceProxy
        public void notifyCameraState(CameraSessionStats cameraSessionStats) throws RemoteException {
        }

        @Override // android.hardware.ICameraServiceProxy
        public int getRotateAndCropOverride(String packageName, int lensFacing, int userId) throws RemoteException {
            return 0;
        }

        @Override // android.hardware.ICameraServiceProxy
        public boolean isCameraDisabled() throws RemoteException {
            return false;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements ICameraServiceProxy {
        public static final String DESCRIPTOR = "android.hardware.ICameraServiceProxy";
        static final int TRANSACTION_getRotateAndCropOverride = 3;
        static final int TRANSACTION_isCameraDisabled = 4;
        static final int TRANSACTION_notifyCameraState = 2;
        static final int TRANSACTION_pingForUserUpdate = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ICameraServiceProxy asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ICameraServiceProxy)) {
                return (ICameraServiceProxy) iin;
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
                    return "pingForUserUpdate";
                case 2:
                    return "notifyCameraState";
                case 3:
                    return "getRotateAndCropOverride";
                case 4:
                    return "isCameraDisabled";
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
                            pingForUserUpdate();
                            break;
                        case 2:
                            CameraSessionStats _arg0 = (CameraSessionStats) data.readTypedObject(CameraSessionStats.CREATOR);
                            data.enforceNoDataAvail();
                            notifyCameraState(_arg0);
                            break;
                        case 3:
                            String _arg02 = data.readString();
                            int _arg1 = data.readInt();
                            int _arg2 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result = getRotateAndCropOverride(_arg02, _arg1, _arg2);
                            reply.writeNoException();
                            reply.writeInt(_result);
                            break;
                        case 4:
                            boolean _result2 = isCameraDisabled();
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
        private static class Proxy implements ICameraServiceProxy {
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

            @Override // android.hardware.ICameraServiceProxy
            public void pingForUserUpdate() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.ICameraServiceProxy
            public void notifyCameraState(CameraSessionStats cameraSessionStats) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(cameraSessionStats, 0);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.ICameraServiceProxy
            public int getRotateAndCropOverride(String packageName, int lensFacing, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(lensFacing);
                    _data.writeInt(userId);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.ICameraServiceProxy
            public boolean isCameraDisabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
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
