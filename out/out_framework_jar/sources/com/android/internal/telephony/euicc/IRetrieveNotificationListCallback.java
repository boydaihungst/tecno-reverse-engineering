package com.android.internal.telephony.euicc;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.euicc.EuiccNotification;
/* loaded from: classes4.dex */
public interface IRetrieveNotificationListCallback extends IInterface {
    public static final String DESCRIPTOR = "com.android.internal.telephony.euicc.IRetrieveNotificationListCallback";

    void onComplete(int i, EuiccNotification[] euiccNotificationArr) throws RemoteException;

    /* loaded from: classes4.dex */
    public static class Default implements IRetrieveNotificationListCallback {
        @Override // com.android.internal.telephony.euicc.IRetrieveNotificationListCallback
        public void onComplete(int resultCode, EuiccNotification[] notifications) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class Stub extends Binder implements IRetrieveNotificationListCallback {
        static final int TRANSACTION_onComplete = 1;

        public Stub() {
            attachInterface(this, IRetrieveNotificationListCallback.DESCRIPTOR);
        }

        public static IRetrieveNotificationListCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IRetrieveNotificationListCallback.DESCRIPTOR);
            if (iin != null && (iin instanceof IRetrieveNotificationListCallback)) {
                return (IRetrieveNotificationListCallback) iin;
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
                    return "onComplete";
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
                data.enforceInterface(IRetrieveNotificationListCallback.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IRetrieveNotificationListCallback.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            EuiccNotification[] _arg1 = (EuiccNotification[]) data.createTypedArray(EuiccNotification.CREATOR);
                            data.enforceNoDataAvail();
                            onComplete(_arg0, _arg1);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes4.dex */
        private static class Proxy implements IRetrieveNotificationListCallback {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IRetrieveNotificationListCallback.DESCRIPTOR;
            }

            @Override // com.android.internal.telephony.euicc.IRetrieveNotificationListCallback
            public void onComplete(int resultCode, EuiccNotification[] notifications) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IRetrieveNotificationListCallback.DESCRIPTOR);
                    _data.writeInt(resultCode);
                    _data.writeTypedArray(notifications, 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 0;
        }
    }
}
