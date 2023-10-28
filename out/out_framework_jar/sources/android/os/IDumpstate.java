package android.os;

import android.os.IDumpstateListener;
import java.io.FileDescriptor;
/* loaded from: classes2.dex */
public interface IDumpstate extends IInterface {
    public static final int BUGREPORT_MODE_DEFAULT = 6;
    public static final int BUGREPORT_MODE_FULL = 0;
    public static final int BUGREPORT_MODE_INTERACTIVE = 1;
    public static final int BUGREPORT_MODE_REMOTE = 2;
    public static final int BUGREPORT_MODE_TELEPHONY = 4;
    public static final int BUGREPORT_MODE_WEAR = 3;
    public static final int BUGREPORT_MODE_WIFI = 5;
    public static final String DESCRIPTOR = "android.os.IDumpstate";

    void cancelBugreport(int i, String str) throws RemoteException;

    void startBugreport(int i, String str, FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, int i2, IDumpstateListener iDumpstateListener, boolean z) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IDumpstate {
        @Override // android.os.IDumpstate
        public void startBugreport(int callingUid, String callingPackage, FileDescriptor bugreportFd, FileDescriptor screenshotFd, int bugreportMode, IDumpstateListener listener, boolean isScreenshotRequested) throws RemoteException {
        }

        @Override // android.os.IDumpstate
        public void cancelBugreport(int callingUid, String callingPackage) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IDumpstate {
        static final int TRANSACTION_cancelBugreport = 2;
        static final int TRANSACTION_startBugreport = 1;

        public Stub() {
            attachInterface(this, IDumpstate.DESCRIPTOR);
        }

        public static IDumpstate asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IDumpstate.DESCRIPTOR);
            if (iin != null && (iin instanceof IDumpstate)) {
                return (IDumpstate) iin;
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
                    return "startBugreport";
                case 2:
                    return "cancelBugreport";
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
                data.enforceInterface(IDumpstate.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IDumpstate.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            String _arg1 = data.readString();
                            FileDescriptor _arg2 = data.readRawFileDescriptor();
                            FileDescriptor _arg3 = data.readRawFileDescriptor();
                            int _arg4 = data.readInt();
                            IDumpstateListener _arg5 = IDumpstateListener.Stub.asInterface(data.readStrongBinder());
                            boolean _arg6 = data.readBoolean();
                            data.enforceNoDataAvail();
                            startBugreport(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6);
                            reply.writeNoException();
                            break;
                        case 2:
                            int _arg02 = data.readInt();
                            String _arg12 = data.readString();
                            data.enforceNoDataAvail();
                            cancelBugreport(_arg02, _arg12);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IDumpstate {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IDumpstate.DESCRIPTOR;
            }

            @Override // android.os.IDumpstate
            public void startBugreport(int callingUid, String callingPackage, FileDescriptor bugreportFd, FileDescriptor screenshotFd, int bugreportMode, IDumpstateListener listener, boolean isScreenshotRequested) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDumpstate.DESCRIPTOR);
                    _data.writeInt(callingUid);
                    _data.writeString(callingPackage);
                    _data.writeRawFileDescriptor(bugreportFd);
                    _data.writeRawFileDescriptor(screenshotFd);
                    _data.writeInt(bugreportMode);
                    _data.writeStrongInterface(listener);
                    _data.writeBoolean(isScreenshotRequested);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.IDumpstate
            public void cancelBugreport(int callingUid, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDumpstate.DESCRIPTOR);
                    _data.writeInt(callingUid);
                    _data.writeString(callingPackage);
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
