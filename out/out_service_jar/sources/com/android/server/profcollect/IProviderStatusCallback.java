package com.android.server.profcollect;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes2.dex */
public interface IProviderStatusCallback extends IInterface {
    public static final String DESCRIPTOR = "com.android.server.profcollect.IProviderStatusCallback";

    void onProviderReady() throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IProviderStatusCallback {
        @Override // com.android.server.profcollect.IProviderStatusCallback
        public void onProviderReady() throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IProviderStatusCallback {
        static final int TRANSACTION_onProviderReady = 1;

        public Stub() {
            attachInterface(this, IProviderStatusCallback.DESCRIPTOR);
        }

        public static IProviderStatusCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IProviderStatusCallback.DESCRIPTOR);
            if (iin != null && (iin instanceof IProviderStatusCallback)) {
                return (IProviderStatusCallback) iin;
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
                data.enforceInterface(IProviderStatusCallback.DESCRIPTOR);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(IProviderStatusCallback.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            onProviderReady();
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IProviderStatusCallback {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IProviderStatusCallback.DESCRIPTOR;
            }

            @Override // com.android.server.profcollect.IProviderStatusCallback
            public void onProviderReady() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IProviderStatusCallback.DESCRIPTOR);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
