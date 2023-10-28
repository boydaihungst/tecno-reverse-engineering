package android.hardware.biometrics;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface ITestSession extends IInterface {
    public static final String DESCRIPTOR = "android.hardware.biometrics.ITestSession";

    void acceptAuthentication(int i) throws RemoteException;

    void cleanupInternalState(int i) throws RemoteException;

    void finishEnroll(int i) throws RemoteException;

    void notifyAcquired(int i, int i2) throws RemoteException;

    void notifyError(int i, int i2) throws RemoteException;

    void rejectAuthentication(int i) throws RemoteException;

    void setTestHalEnabled(boolean z) throws RemoteException;

    void startEnroll(int i) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements ITestSession {
        @Override // android.hardware.biometrics.ITestSession
        public void setTestHalEnabled(boolean enableTestHal) throws RemoteException {
        }

        @Override // android.hardware.biometrics.ITestSession
        public void startEnroll(int userId) throws RemoteException {
        }

        @Override // android.hardware.biometrics.ITestSession
        public void finishEnroll(int userId) throws RemoteException {
        }

        @Override // android.hardware.biometrics.ITestSession
        public void acceptAuthentication(int userId) throws RemoteException {
        }

        @Override // android.hardware.biometrics.ITestSession
        public void rejectAuthentication(int userId) throws RemoteException {
        }

        @Override // android.hardware.biometrics.ITestSession
        public void notifyAcquired(int userId, int acquireInfo) throws RemoteException {
        }

        @Override // android.hardware.biometrics.ITestSession
        public void notifyError(int userId, int errorCode) throws RemoteException {
        }

        @Override // android.hardware.biometrics.ITestSession
        public void cleanupInternalState(int userId) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements ITestSession {
        static final int TRANSACTION_acceptAuthentication = 4;
        static final int TRANSACTION_cleanupInternalState = 8;
        static final int TRANSACTION_finishEnroll = 3;
        static final int TRANSACTION_notifyAcquired = 6;
        static final int TRANSACTION_notifyError = 7;
        static final int TRANSACTION_rejectAuthentication = 5;
        static final int TRANSACTION_setTestHalEnabled = 1;
        static final int TRANSACTION_startEnroll = 2;

        public Stub() {
            attachInterface(this, ITestSession.DESCRIPTOR);
        }

        public static ITestSession asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ITestSession.DESCRIPTOR);
            if (iin != null && (iin instanceof ITestSession)) {
                return (ITestSession) iin;
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
                    return "setTestHalEnabled";
                case 2:
                    return "startEnroll";
                case 3:
                    return "finishEnroll";
                case 4:
                    return "acceptAuthentication";
                case 5:
                    return "rejectAuthentication";
                case 6:
                    return "notifyAcquired";
                case 7:
                    return "notifyError";
                case 8:
                    return "cleanupInternalState";
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
                data.enforceInterface(ITestSession.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ITestSession.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            boolean _arg0 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setTestHalEnabled(_arg0);
                            reply.writeNoException();
                            break;
                        case 2:
                            int _arg02 = data.readInt();
                            data.enforceNoDataAvail();
                            startEnroll(_arg02);
                            reply.writeNoException();
                            break;
                        case 3:
                            int _arg03 = data.readInt();
                            data.enforceNoDataAvail();
                            finishEnroll(_arg03);
                            reply.writeNoException();
                            break;
                        case 4:
                            int _arg04 = data.readInt();
                            data.enforceNoDataAvail();
                            acceptAuthentication(_arg04);
                            reply.writeNoException();
                            break;
                        case 5:
                            int _arg05 = data.readInt();
                            data.enforceNoDataAvail();
                            rejectAuthentication(_arg05);
                            reply.writeNoException();
                            break;
                        case 6:
                            int _arg06 = data.readInt();
                            int _arg1 = data.readInt();
                            data.enforceNoDataAvail();
                            notifyAcquired(_arg06, _arg1);
                            reply.writeNoException();
                            break;
                        case 7:
                            int _arg07 = data.readInt();
                            int _arg12 = data.readInt();
                            data.enforceNoDataAvail();
                            notifyError(_arg07, _arg12);
                            reply.writeNoException();
                            break;
                        case 8:
                            int _arg08 = data.readInt();
                            data.enforceNoDataAvail();
                            cleanupInternalState(_arg08);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements ITestSession {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ITestSession.DESCRIPTOR;
            }

            @Override // android.hardware.biometrics.ITestSession
            public void setTestHalEnabled(boolean enableTestHal) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITestSession.DESCRIPTOR);
                    _data.writeBoolean(enableTestHal);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.ITestSession
            public void startEnroll(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITestSession.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.ITestSession
            public void finishEnroll(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITestSession.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.ITestSession
            public void acceptAuthentication(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITestSession.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.ITestSession
            public void rejectAuthentication(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITestSession.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.ITestSession
            public void notifyAcquired(int userId, int acquireInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITestSession.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeInt(acquireInfo);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.ITestSession
            public void notifyError(int userId, int errorCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITestSession.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeInt(errorCode);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.biometrics.ITestSession
            public void cleanupInternalState(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITestSession.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 7;
        }
    }
}
