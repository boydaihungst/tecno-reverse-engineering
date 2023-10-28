package android.telephony.ims.aidl;

import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsRegistrationAttributes;
/* loaded from: classes3.dex */
public interface IImsRegistrationCallback extends IInterface {
    public static final String DESCRIPTOR = "android.telephony.ims.aidl.IImsRegistrationCallback";

    void onDeregistered(ImsReasonInfo imsReasonInfo) throws RemoteException;

    void onRegistered(ImsRegistrationAttributes imsRegistrationAttributes) throws RemoteException;

    void onRegistering(ImsRegistrationAttributes imsRegistrationAttributes) throws RemoteException;

    void onSubscriberAssociatedUriChanged(Uri[] uriArr) throws RemoteException;

    void onTechnologyChangeFailed(int i, ImsReasonInfo imsReasonInfo) throws RemoteException;

    /* loaded from: classes3.dex */
    public static class Default implements IImsRegistrationCallback {
        @Override // android.telephony.ims.aidl.IImsRegistrationCallback
        public void onRegistered(ImsRegistrationAttributes attr) throws RemoteException {
        }

        @Override // android.telephony.ims.aidl.IImsRegistrationCallback
        public void onRegistering(ImsRegistrationAttributes attr) throws RemoteException {
        }

        @Override // android.telephony.ims.aidl.IImsRegistrationCallback
        public void onDeregistered(ImsReasonInfo info) throws RemoteException {
        }

        @Override // android.telephony.ims.aidl.IImsRegistrationCallback
        public void onTechnologyChangeFailed(int imsRadioTech, ImsReasonInfo info) throws RemoteException {
        }

        @Override // android.telephony.ims.aidl.IImsRegistrationCallback
        public void onSubscriberAssociatedUriChanged(Uri[] uris) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes3.dex */
    public static abstract class Stub extends Binder implements IImsRegistrationCallback {
        static final int TRANSACTION_onDeregistered = 3;
        static final int TRANSACTION_onRegistered = 1;
        static final int TRANSACTION_onRegistering = 2;
        static final int TRANSACTION_onSubscriberAssociatedUriChanged = 5;
        static final int TRANSACTION_onTechnologyChangeFailed = 4;

        public Stub() {
            attachInterface(this, IImsRegistrationCallback.DESCRIPTOR);
        }

        public static IImsRegistrationCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IImsRegistrationCallback.DESCRIPTOR);
            if (iin != null && (iin instanceof IImsRegistrationCallback)) {
                return (IImsRegistrationCallback) iin;
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
                    return "onRegistered";
                case 2:
                    return "onRegistering";
                case 3:
                    return "onDeregistered";
                case 4:
                    return "onTechnologyChangeFailed";
                case 5:
                    return "onSubscriberAssociatedUriChanged";
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
                data.enforceInterface(IImsRegistrationCallback.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IImsRegistrationCallback.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            ImsRegistrationAttributes _arg0 = (ImsRegistrationAttributes) data.readTypedObject(ImsRegistrationAttributes.CREATOR);
                            data.enforceNoDataAvail();
                            onRegistered(_arg0);
                            break;
                        case 2:
                            ImsRegistrationAttributes _arg02 = (ImsRegistrationAttributes) data.readTypedObject(ImsRegistrationAttributes.CREATOR);
                            data.enforceNoDataAvail();
                            onRegistering(_arg02);
                            break;
                        case 3:
                            ImsReasonInfo _arg03 = (ImsReasonInfo) data.readTypedObject(ImsReasonInfo.CREATOR);
                            data.enforceNoDataAvail();
                            onDeregistered(_arg03);
                            break;
                        case 4:
                            int _arg04 = data.readInt();
                            ImsReasonInfo _arg1 = (ImsReasonInfo) data.readTypedObject(ImsReasonInfo.CREATOR);
                            data.enforceNoDataAvail();
                            onTechnologyChangeFailed(_arg04, _arg1);
                            break;
                        case 5:
                            Uri[] _arg05 = (Uri[]) data.createTypedArray(Uri.CREATOR);
                            data.enforceNoDataAvail();
                            onSubscriberAssociatedUriChanged(_arg05);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes3.dex */
        public static class Proxy implements IImsRegistrationCallback {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IImsRegistrationCallback.DESCRIPTOR;
            }

            @Override // android.telephony.ims.aidl.IImsRegistrationCallback
            public void onRegistered(ImsRegistrationAttributes attr) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsRegistrationCallback.DESCRIPTOR);
                    _data.writeTypedObject(attr, 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsRegistrationCallback
            public void onRegistering(ImsRegistrationAttributes attr) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsRegistrationCallback.DESCRIPTOR);
                    _data.writeTypedObject(attr, 0);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsRegistrationCallback
            public void onDeregistered(ImsReasonInfo info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsRegistrationCallback.DESCRIPTOR);
                    _data.writeTypedObject(info, 0);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsRegistrationCallback
            public void onTechnologyChangeFailed(int imsRadioTech, ImsReasonInfo info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsRegistrationCallback.DESCRIPTOR);
                    _data.writeInt(imsRadioTech);
                    _data.writeTypedObject(info, 0);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsRegistrationCallback
            public void onSubscriberAssociatedUriChanged(Uri[] uris) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsRegistrationCallback.DESCRIPTOR);
                    _data.writeTypedArray(uris, 0);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 4;
        }
    }
}
