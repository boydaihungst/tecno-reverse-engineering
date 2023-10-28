package com.transsion.mol;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes2.dex */
public interface IMolService extends IInterface {
    public static final String DESCRIPTOR = "com.transsion.mol.IMolService";

    void dispatchMessage(Intent intent) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IMolService {
        @Override // com.transsion.mol.IMolService
        public void dispatchMessage(Intent intent) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IMolService {
        static final int TRANSACTION_dispatchMessage = 1;

        public Stub() {
            attachInterface(this, IMolService.DESCRIPTOR);
        }

        public static IMolService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IMolService.DESCRIPTOR);
            if (iin != null && (iin instanceof IMolService)) {
                return (IMolService) iin;
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
                data.enforceInterface(IMolService.DESCRIPTOR);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(IMolService.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            Intent _arg0 = (Intent) data.readTypedObject(Intent.CREATOR);
                            data.enforceNoDataAvail();
                            dispatchMessage(_arg0);
                            reply.writeNoException();
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IMolService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IMolService.DESCRIPTOR;
            }

            @Override // com.transsion.mol.IMolService
            public void dispatchMessage(Intent intent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMolService.DESCRIPTOR);
                    _data.writeTypedObject(intent, 0);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
