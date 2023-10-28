package com.transsion.sru;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes2.dex */
public interface ITranSruService extends IInterface {
    public static final String DESCRIPTOR = "com.transsion.sru.ITranSruService";

    void setData() throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements ITranSruService {
        @Override // com.transsion.sru.ITranSruService
        public void setData() throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements ITranSruService {
        static final int TRANSACTION_setData = 1;

        public Stub() {
            attachInterface(this, ITranSruService.DESCRIPTOR);
        }

        public static ITranSruService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ITranSruService.DESCRIPTOR);
            if (iin != null && (iin instanceof ITranSruService)) {
                return (ITranSruService) iin;
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
                data.enforceInterface(ITranSruService.DESCRIPTOR);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(ITranSruService.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            setData();
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements ITranSruService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ITranSruService.DESCRIPTOR;
            }

            @Override // com.transsion.sru.ITranSruService
            public void setData() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITranSruService.DESCRIPTOR);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
