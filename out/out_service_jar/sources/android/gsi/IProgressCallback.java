package android.gsi;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IProgressCallback extends IInterface {
    public static final String DESCRIPTOR = "android.gsi.IProgressCallback";

    void onProgress(long j, long j2) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IProgressCallback {
        @Override // android.gsi.IProgressCallback
        public void onProgress(long current, long total) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IProgressCallback {
        static final int TRANSACTION_onProgress = 1;

        public Stub() {
            attachInterface(this, IProgressCallback.DESCRIPTOR);
        }

        public static IProgressCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IProgressCallback.DESCRIPTOR);
            if (iin != null && (iin instanceof IProgressCallback)) {
                return (IProgressCallback) iin;
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
                data.enforceInterface(IProgressCallback.DESCRIPTOR);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(IProgressCallback.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            long _arg0 = data.readLong();
                            long _arg1 = data.readLong();
                            data.enforceNoDataAvail();
                            onProgress(_arg0, _arg1);
                            reply.writeNoException();
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IProgressCallback {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IProgressCallback.DESCRIPTOR;
            }

            @Override // android.gsi.IProgressCallback
            public void onProgress(long current, long total) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IProgressCallback.DESCRIPTOR);
                    _data.writeLong(current);
                    _data.writeLong(total);
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
