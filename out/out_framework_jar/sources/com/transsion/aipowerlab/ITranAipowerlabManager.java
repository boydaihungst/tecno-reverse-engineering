package com.transsion.aipowerlab;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes4.dex */
public interface ITranAipowerlabManager extends IInterface {
    public static final String DESCRIPTOR = "com.transsion.aipowerlab.ITranAipowerlabManager";

    void enable(boolean z) throws RemoteException;

    /* loaded from: classes4.dex */
    public static class Default implements ITranAipowerlabManager {
        @Override // com.transsion.aipowerlab.ITranAipowerlabManager
        public void enable(boolean enable) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class Stub extends Binder implements ITranAipowerlabManager {
        static final int TRANSACTION_enable = 1;

        public Stub() {
            attachInterface(this, ITranAipowerlabManager.DESCRIPTOR);
        }

        public static ITranAipowerlabManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ITranAipowerlabManager.DESCRIPTOR);
            if (iin != null && (iin instanceof ITranAipowerlabManager)) {
                return (ITranAipowerlabManager) iin;
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
                    return "enable";
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
                data.enforceInterface(ITranAipowerlabManager.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ITranAipowerlabManager.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            boolean _arg0 = data.readBoolean();
                            data.enforceNoDataAvail();
                            enable(_arg0);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes4.dex */
        private static class Proxy implements ITranAipowerlabManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ITranAipowerlabManager.DESCRIPTOR;
            }

            @Override // com.transsion.aipowerlab.ITranAipowerlabManager
            public void enable(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITranAipowerlabManager.DESCRIPTOR);
                    _data.writeBoolean(enable);
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
