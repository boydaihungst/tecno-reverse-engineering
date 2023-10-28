package android.telephony.ims.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes3.dex */
public interface IPublishResponseCallback extends IInterface {
    public static final String DESCRIPTOR = "android.telephony.ims.aidl.IPublishResponseCallback";

    void onCommandError(int i) throws RemoteException;

    void onNetworkRespHeader(int i, String str, int i2, String str2) throws RemoteException;

    void onNetworkResponse(int i, String str) throws RemoteException;

    /* loaded from: classes3.dex */
    public static class Default implements IPublishResponseCallback {
        @Override // android.telephony.ims.aidl.IPublishResponseCallback
        public void onCommandError(int code) throws RemoteException {
        }

        @Override // android.telephony.ims.aidl.IPublishResponseCallback
        public void onNetworkResponse(int code, String reason) throws RemoteException {
        }

        @Override // android.telephony.ims.aidl.IPublishResponseCallback
        public void onNetworkRespHeader(int code, String reasonPhrase, int reasonHeaderCause, String reasonHeaderText) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes3.dex */
    public static abstract class Stub extends Binder implements IPublishResponseCallback {
        static final int TRANSACTION_onCommandError = 1;
        static final int TRANSACTION_onNetworkRespHeader = 3;
        static final int TRANSACTION_onNetworkResponse = 2;

        public Stub() {
            attachInterface(this, IPublishResponseCallback.DESCRIPTOR);
        }

        public static IPublishResponseCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IPublishResponseCallback.DESCRIPTOR);
            if (iin != null && (iin instanceof IPublishResponseCallback)) {
                return (IPublishResponseCallback) iin;
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
                    return "onCommandError";
                case 2:
                    return "onNetworkResponse";
                case 3:
                    return "onNetworkRespHeader";
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
                data.enforceInterface(IPublishResponseCallback.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IPublishResponseCallback.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            data.enforceNoDataAvail();
                            onCommandError(_arg0);
                            break;
                        case 2:
                            int _arg02 = data.readInt();
                            String _arg1 = data.readString();
                            data.enforceNoDataAvail();
                            onNetworkResponse(_arg02, _arg1);
                            break;
                        case 3:
                            int _arg03 = data.readInt();
                            String _arg12 = data.readString();
                            int _arg2 = data.readInt();
                            String _arg3 = data.readString();
                            data.enforceNoDataAvail();
                            onNetworkRespHeader(_arg03, _arg12, _arg2, _arg3);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes3.dex */
        private static class Proxy implements IPublishResponseCallback {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IPublishResponseCallback.DESCRIPTOR;
            }

            @Override // android.telephony.ims.aidl.IPublishResponseCallback
            public void onCommandError(int code) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPublishResponseCallback.DESCRIPTOR);
                    _data.writeInt(code);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IPublishResponseCallback
            public void onNetworkResponse(int code, String reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPublishResponseCallback.DESCRIPTOR);
                    _data.writeInt(code);
                    _data.writeString(reason);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IPublishResponseCallback
            public void onNetworkRespHeader(int code, String reasonPhrase, int reasonHeaderCause, String reasonHeaderText) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IPublishResponseCallback.DESCRIPTOR);
                    _data.writeInt(code);
                    _data.writeString(reasonPhrase);
                    _data.writeInt(reasonHeaderCause);
                    _data.writeString(reasonHeaderText);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 2;
        }
    }
}
