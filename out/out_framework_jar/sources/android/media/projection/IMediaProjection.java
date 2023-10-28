package android.media.projection;

import android.media.projection.IMediaProjectionCallback;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.window.WindowContainerToken;
/* loaded from: classes2.dex */
public interface IMediaProjection extends IInterface {
    int applyVirtualDisplayFlags(int i) throws RemoteException;

    boolean canProjectAudio() throws RemoteException;

    boolean canProjectSecureVideo() throws RemoteException;

    boolean canProjectVideo() throws RemoteException;

    WindowContainerToken getTaskRecordingWindowContainerToken() throws RemoteException;

    void registerCallback(IMediaProjectionCallback iMediaProjectionCallback) throws RemoteException;

    void setTaskRecordingWindowContainerToken(WindowContainerToken windowContainerToken) throws RemoteException;

    void start(IMediaProjectionCallback iMediaProjectionCallback) throws RemoteException;

    void stop() throws RemoteException;

    void unregisterCallback(IMediaProjectionCallback iMediaProjectionCallback) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IMediaProjection {
        @Override // android.media.projection.IMediaProjection
        public void start(IMediaProjectionCallback callback) throws RemoteException {
        }

        @Override // android.media.projection.IMediaProjection
        public void stop() throws RemoteException {
        }

        @Override // android.media.projection.IMediaProjection
        public boolean canProjectAudio() throws RemoteException {
            return false;
        }

        @Override // android.media.projection.IMediaProjection
        public boolean canProjectVideo() throws RemoteException {
            return false;
        }

        @Override // android.media.projection.IMediaProjection
        public boolean canProjectSecureVideo() throws RemoteException {
            return false;
        }

        @Override // android.media.projection.IMediaProjection
        public int applyVirtualDisplayFlags(int flags) throws RemoteException {
            return 0;
        }

        @Override // android.media.projection.IMediaProjection
        public void registerCallback(IMediaProjectionCallback callback) throws RemoteException {
        }

        @Override // android.media.projection.IMediaProjection
        public void unregisterCallback(IMediaProjectionCallback callback) throws RemoteException {
        }

        @Override // android.media.projection.IMediaProjection
        public WindowContainerToken getTaskRecordingWindowContainerToken() throws RemoteException {
            return null;
        }

        @Override // android.media.projection.IMediaProjection
        public void setTaskRecordingWindowContainerToken(WindowContainerToken token) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IMediaProjection {
        public static final String DESCRIPTOR = "android.media.projection.IMediaProjection";
        static final int TRANSACTION_applyVirtualDisplayFlags = 6;
        static final int TRANSACTION_canProjectAudio = 3;
        static final int TRANSACTION_canProjectSecureVideo = 5;
        static final int TRANSACTION_canProjectVideo = 4;
        static final int TRANSACTION_getTaskRecordingWindowContainerToken = 9;
        static final int TRANSACTION_registerCallback = 7;
        static final int TRANSACTION_setTaskRecordingWindowContainerToken = 10;
        static final int TRANSACTION_start = 1;
        static final int TRANSACTION_stop = 2;
        static final int TRANSACTION_unregisterCallback = 8;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMediaProjection asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IMediaProjection)) {
                return (IMediaProjection) iin;
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
                    return "start";
                case 2:
                    return "stop";
                case 3:
                    return "canProjectAudio";
                case 4:
                    return "canProjectVideo";
                case 5:
                    return "canProjectSecureVideo";
                case 6:
                    return "applyVirtualDisplayFlags";
                case 7:
                    return "registerCallback";
                case 8:
                    return "unregisterCallback";
                case 9:
                    return "getTaskRecordingWindowContainerToken";
                case 10:
                    return "setTaskRecordingWindowContainerToken";
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
                            IMediaProjectionCallback _arg0 = IMediaProjectionCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            start(_arg0);
                            reply.writeNoException();
                            break;
                        case 2:
                            stop();
                            reply.writeNoException();
                            break;
                        case 3:
                            boolean _result = canProjectAudio();
                            reply.writeNoException();
                            reply.writeBoolean(_result);
                            break;
                        case 4:
                            boolean _result2 = canProjectVideo();
                            reply.writeNoException();
                            reply.writeBoolean(_result2);
                            break;
                        case 5:
                            boolean _result3 = canProjectSecureVideo();
                            reply.writeNoException();
                            reply.writeBoolean(_result3);
                            break;
                        case 6:
                            int _arg02 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result4 = applyVirtualDisplayFlags(_arg02);
                            reply.writeNoException();
                            reply.writeInt(_result4);
                            break;
                        case 7:
                            IMediaProjectionCallback _arg03 = IMediaProjectionCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            registerCallback(_arg03);
                            reply.writeNoException();
                            break;
                        case 8:
                            IMediaProjectionCallback _arg04 = IMediaProjectionCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            unregisterCallback(_arg04);
                            reply.writeNoException();
                            break;
                        case 9:
                            WindowContainerToken _result5 = getTaskRecordingWindowContainerToken();
                            reply.writeNoException();
                            reply.writeTypedObject(_result5, 1);
                            break;
                        case 10:
                            WindowContainerToken _arg05 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            data.enforceNoDataAvail();
                            setTaskRecordingWindowContainerToken(_arg05);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public static class Proxy implements IMediaProjection {
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

            @Override // android.media.projection.IMediaProjection
            public void start(IMediaProjectionCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.projection.IMediaProjection
            public void stop() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.projection.IMediaProjection
            public boolean canProjectAudio() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.projection.IMediaProjection
            public boolean canProjectVideo() throws RemoteException {
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

            @Override // android.media.projection.IMediaProjection
            public boolean canProjectSecureVideo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.projection.IMediaProjection
            public int applyVirtualDisplayFlags(int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(flags);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.projection.IMediaProjection
            public void registerCallback(IMediaProjectionCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.projection.IMediaProjection
            public void unregisterCallback(IMediaProjectionCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.projection.IMediaProjection
            public WindowContainerToken getTaskRecordingWindowContainerToken() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    WindowContainerToken _result = (WindowContainerToken) _reply.readTypedObject(WindowContainerToken.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.projection.IMediaProjection
            public void setTaskRecordingWindowContainerToken(WindowContainerToken token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(token, 0);
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
