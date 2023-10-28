package android.hardware;

import android.Manifest;
import android.app.ActivityThread;
import android.content.AttributionSource;
import android.content.Context;
import android.content.PermissionChecker;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface ISerialManager extends IInterface {
    String[] getSerialPorts() throws RemoteException;

    ParcelFileDescriptor openSerialPort(String str) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements ISerialManager {
        @Override // android.hardware.ISerialManager
        public String[] getSerialPorts() throws RemoteException {
            return null;
        }

        @Override // android.hardware.ISerialManager
        public ParcelFileDescriptor openSerialPort(String name) throws RemoteException {
            return null;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements ISerialManager {
        public static final String DESCRIPTOR = "android.hardware.ISerialManager";
        static final int TRANSACTION_getSerialPorts = 1;
        static final int TRANSACTION_openSerialPort = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISerialManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ISerialManager)) {
                return (ISerialManager) iin;
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
                    return "getSerialPorts";
                case 2:
                    return "openSerialPort";
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
                            if (!permissionCheckerWrapper(Manifest.permission.SERIAL_PORT, getCallingPid(), new AttributionSource(getCallingUid(), null, null))) {
                                throw new SecurityException("Access denied, requires: android.Manifest.permission.SERIAL_PORT");
                            }
                            String[] _result = getSerialPorts();
                            reply.writeNoException();
                            reply.writeStringArray(_result);
                            break;
                        case 2:
                            String _arg0 = data.readString();
                            data.enforceNoDataAvail();
                            if (!permissionCheckerWrapper(Manifest.permission.SERIAL_PORT, getCallingPid(), new AttributionSource(getCallingUid(), null, null))) {
                                throw new SecurityException("Access denied, requires: android.Manifest.permission.SERIAL_PORT");
                            }
                            ParcelFileDescriptor _result2 = openSerialPort(_arg0);
                            reply.writeNoException();
                            reply.writeTypedObject(_result2, 1);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements ISerialManager {
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

            @Override // android.hardware.ISerialManager
            public String[] getSerialPorts() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.ISerialManager
            public ParcelFileDescriptor openSerialPort(String name) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    ParcelFileDescriptor _result = (ParcelFileDescriptor) _reply.readTypedObject(ParcelFileDescriptor.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        private boolean permissionCheckerWrapper(String permission, int pid, AttributionSource attributionSource) {
            Context ctx = ActivityThread.currentActivityThread().getSystemContext();
            return PermissionChecker.checkPermissionForDataDelivery(ctx, permission, pid, attributionSource, "") == 0;
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 1;
        }
    }
}
