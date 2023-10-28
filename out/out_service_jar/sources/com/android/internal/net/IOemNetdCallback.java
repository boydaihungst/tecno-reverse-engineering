package com.android.internal.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IOemNetdCallback extends IInterface {
    public static final String DESCRIPTOR = "com$android$internal$net$IOemNetdCallback".replace('$', '.');

    void onResult(String str, int i, int i2) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IOemNetdCallback {
        @Override // com.android.internal.net.IOemNetdCallback
        public void onResult(String adapter, int markId, int result) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IOemNetdCallback {
        static final int TRANSACTION_onResult = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IOemNetdCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IOemNetdCallback)) {
                return (IOemNetdCallback) iin;
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
                case 1598968902:
                    reply.writeString(descriptor);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            String _arg0 = data.readString();
                            int _arg1 = data.readInt();
                            int _arg2 = data.readInt();
                            data.enforceNoDataAvail();
                            onResult(_arg0, _arg1, _arg2);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IOemNetdCallback {
            private IBinder mRemote;

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

            @Override // com.android.internal.net.IOemNetdCallback
            public void onResult(String adapter, int markId, int result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(adapter);
                    _data.writeInt(markId);
                    _data.writeInt(result);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
