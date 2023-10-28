package android.hardware.contexthub;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IContextHubCallback extends IInterface {
    public static final String DESCRIPTOR = "android$hardware$contexthub$IContextHubCallback".replace('$', '.');
    public static final String HASH = "10abe2e5202d9b80ccebf5f6376d711a9a212b27";
    public static final int VERSION = 1;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void handleContextHubAsyncEvent(int i) throws RemoteException;

    void handleContextHubMessage(ContextHubMessage contextHubMessage, String[] strArr) throws RemoteException;

    void handleNanoappInfo(NanoappInfo[] nanoappInfoArr) throws RemoteException;

    void handleTransactionResult(int i, boolean z) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IContextHubCallback {
        @Override // android.hardware.contexthub.IContextHubCallback
        public void handleNanoappInfo(NanoappInfo[] appInfo) throws RemoteException {
        }

        @Override // android.hardware.contexthub.IContextHubCallback
        public void handleContextHubMessage(ContextHubMessage msg, String[] msgContentPerms) throws RemoteException {
        }

        @Override // android.hardware.contexthub.IContextHubCallback
        public void handleContextHubAsyncEvent(int evt) throws RemoteException {
        }

        @Override // android.hardware.contexthub.IContextHubCallback
        public void handleTransactionResult(int transactionId, boolean success) throws RemoteException {
        }

        @Override // android.hardware.contexthub.IContextHubCallback
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.hardware.contexthub.IContextHubCallback
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IContextHubCallback {
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_handleContextHubAsyncEvent = 3;
        static final int TRANSACTION_handleContextHubMessage = 2;
        static final int TRANSACTION_handleNanoappInfo = 1;
        static final int TRANSACTION_handleTransactionResult = 4;

        public Stub() {
            markVintfStability();
            attachInterface(this, DESCRIPTOR);
        }

        public static IContextHubCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IContextHubCallback)) {
                return (IContextHubCallback) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(descriptor);
            }
            switch (code) {
                case 16777214:
                    reply.writeNoException();
                    reply.writeString(getInterfaceHash());
                    return true;
                case 16777215:
                    reply.writeNoException();
                    reply.writeInt(getInterfaceVersion());
                    return true;
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(descriptor);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            NanoappInfo[] _arg0 = (NanoappInfo[]) data.createTypedArray(NanoappInfo.CREATOR);
                            data.enforceNoDataAvail();
                            handleNanoappInfo(_arg0);
                            reply.writeNoException();
                            break;
                        case 2:
                            ContextHubMessage _arg02 = (ContextHubMessage) data.readTypedObject(ContextHubMessage.CREATOR);
                            String[] _arg1 = data.createStringArray();
                            data.enforceNoDataAvail();
                            handleContextHubMessage(_arg02, _arg1);
                            reply.writeNoException();
                            break;
                        case 3:
                            int _arg03 = data.readInt();
                            data.enforceNoDataAvail();
                            handleContextHubAsyncEvent(_arg03);
                            reply.writeNoException();
                            break;
                        case 4:
                            int _arg04 = data.readInt();
                            boolean _arg12 = data.readBoolean();
                            data.enforceNoDataAvail();
                            handleTransactionResult(_arg04, _arg12);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IContextHubCallback {
            private IBinder mRemote;
            private int mCachedVersion = -1;
            private String mCachedHash = "-1";

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            @Override // android.hardware.contexthub.IContextHubCallback
            public void handleNanoappInfo(NanoappInfo[] appInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedArray(appInfo, 0);
                    boolean _status = this.mRemote.transact(1, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method handleNanoappInfo is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.contexthub.IContextHubCallback
            public void handleContextHubMessage(ContextHubMessage msg, String[] msgContentPerms) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(msg, 0);
                    _data.writeStringArray(msgContentPerms);
                    boolean _status = this.mRemote.transact(2, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method handleContextHubMessage is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.contexthub.IContextHubCallback
            public void handleContextHubAsyncEvent(int evt) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(evt);
                    boolean _status = this.mRemote.transact(3, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method handleContextHubAsyncEvent is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.contexthub.IContextHubCallback
            public void handleTransactionResult(int transactionId, boolean success) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(transactionId);
                    _data.writeBoolean(success);
                    boolean _status = this.mRemote.transact(4, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method handleTransactionResult is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.contexthub.IContextHubCallback
            public int getInterfaceVersion() throws RemoteException {
                if (this.mCachedVersion == -1) {
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    try {
                        data.writeInterfaceToken(DESCRIPTOR);
                        this.mRemote.transact(16777215, data, reply, 0);
                        reply.readException();
                        this.mCachedVersion = reply.readInt();
                    } finally {
                        reply.recycle();
                        data.recycle();
                    }
                }
                return this.mCachedVersion;
            }

            @Override // android.hardware.contexthub.IContextHubCallback
            public synchronized String getInterfaceHash() throws RemoteException {
                if ("-1".equals(this.mCachedHash)) {
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    data.writeInterfaceToken(DESCRIPTOR);
                    this.mRemote.transact(16777214, data, reply, 0);
                    reply.readException();
                    this.mCachedHash = reply.readString();
                    reply.recycle();
                    data.recycle();
                }
                return this.mCachedHash;
            }
        }
    }
}
