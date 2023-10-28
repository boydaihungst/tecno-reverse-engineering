package com.mediatek.gnssdebugreport;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.mediatek.gnssdebugreport.IDebugReportCallback;
/* loaded from: classes.dex */
public interface IGnssDebugReportService extends IInterface {
    public static final String DESCRIPTOR = "com.mediatek.gnssdebugreport.IGnssDebugReportService";

    void addListener(IDebugReportCallback iDebugReportCallback) throws RemoteException;

    void removeListener(IDebugReportCallback iDebugReportCallback) throws RemoteException;

    boolean startDebug(IDebugReportCallback iDebugReportCallback) throws RemoteException;

    boolean stopDebug(IDebugReportCallback iDebugReportCallback) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IGnssDebugReportService {
        @Override // com.mediatek.gnssdebugreport.IGnssDebugReportService
        public boolean startDebug(IDebugReportCallback callback) throws RemoteException {
            return false;
        }

        @Override // com.mediatek.gnssdebugreport.IGnssDebugReportService
        public boolean stopDebug(IDebugReportCallback callback) throws RemoteException {
            return false;
        }

        @Override // com.mediatek.gnssdebugreport.IGnssDebugReportService
        public void addListener(IDebugReportCallback callback) throws RemoteException {
        }

        @Override // com.mediatek.gnssdebugreport.IGnssDebugReportService
        public void removeListener(IDebugReportCallback callback) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IGnssDebugReportService {
        static final int TRANSACTION_addListener = 3;
        static final int TRANSACTION_removeListener = 4;
        static final int TRANSACTION_startDebug = 1;
        static final int TRANSACTION_stopDebug = 2;

        public Stub() {
            attachInterface(this, IGnssDebugReportService.DESCRIPTOR);
        }

        public static IGnssDebugReportService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IGnssDebugReportService.DESCRIPTOR);
            if (iin != null && (iin instanceof IGnssDebugReportService)) {
                return (IGnssDebugReportService) iin;
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
                data.enforceInterface(IGnssDebugReportService.DESCRIPTOR);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(IGnssDebugReportService.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            IDebugReportCallback _arg0 = IDebugReportCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            boolean _result = startDebug(_arg0);
                            reply.writeNoException();
                            reply.writeBoolean(_result);
                            break;
                        case 2:
                            IDebugReportCallback _arg02 = IDebugReportCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            boolean _result2 = stopDebug(_arg02);
                            reply.writeNoException();
                            reply.writeBoolean(_result2);
                            break;
                        case 3:
                            IDebugReportCallback _arg03 = IDebugReportCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            addListener(_arg03);
                            reply.writeNoException();
                            break;
                        case 4:
                            IDebugReportCallback _arg04 = IDebugReportCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            removeListener(_arg04);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IGnssDebugReportService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IGnssDebugReportService.DESCRIPTOR;
            }

            @Override // com.mediatek.gnssdebugreport.IGnssDebugReportService
            public boolean startDebug(IDebugReportCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IGnssDebugReportService.DESCRIPTOR);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.gnssdebugreport.IGnssDebugReportService
            public boolean stopDebug(IDebugReportCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IGnssDebugReportService.DESCRIPTOR);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.gnssdebugreport.IGnssDebugReportService
            public void addListener(IDebugReportCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IGnssDebugReportService.DESCRIPTOR);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.mediatek.gnssdebugreport.IGnssDebugReportService
            public void removeListener(IDebugReportCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IGnssDebugReportService.DESCRIPTOR);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
