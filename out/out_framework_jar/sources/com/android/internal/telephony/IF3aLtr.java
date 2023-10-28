package com.android.internal.telephony;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes4.dex */
public interface IF3aLtr extends IInterface {
    public static final String DESCRIPTOR = "com.android.internal.telephony.IF3aLtr";

    void onF3a(Bundle bundle) throws RemoteException;

    /* loaded from: classes4.dex */
    public static class Default implements IF3aLtr {
        @Override // com.android.internal.telephony.IF3aLtr
        public void onF3a(Bundle data) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class Stub extends Binder implements IF3aLtr {
        static final int TRANSACTION_onF3a = 1;

        public Stub() {
            attachInterface(this, IF3aLtr.DESCRIPTOR);
        }

        public static IF3aLtr asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IF3aLtr.DESCRIPTOR);
            if (iin != null && (iin instanceof IF3aLtr)) {
                return (IF3aLtr) iin;
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
                    return "onF3a";
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
                data.enforceInterface(IF3aLtr.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IF3aLtr.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            Bundle _arg0 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            onF3a(_arg0);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes4.dex */
        private static class Proxy implements IF3aLtr {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IF3aLtr.DESCRIPTOR;
            }

            @Override // com.android.internal.telephony.IF3aLtr
            public void onF3a(Bundle data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IF3aLtr.DESCRIPTOR);
                    _data.writeTypedObject(data, 0);
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
