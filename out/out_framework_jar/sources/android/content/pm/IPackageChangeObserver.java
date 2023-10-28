package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IPackageChangeObserver extends IInterface {
    public static final String DESCRIPTOR = "android$content$pm$IPackageChangeObserver".replace('$', '.');

    void onPackageChanged(PackageChangeEvent packageChangeEvent) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IPackageChangeObserver {
        @Override // android.content.pm.IPackageChangeObserver
        public void onPackageChanged(PackageChangeEvent event) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IPackageChangeObserver {
        static final int TRANSACTION_onPackageChanged = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IPackageChangeObserver asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IPackageChangeObserver)) {
                return (IPackageChangeObserver) iin;
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
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(descriptor);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            PackageChangeEvent _arg0 = (PackageChangeEvent) data.readTypedObject(PackageChangeEvent.CREATOR);
                            data.enforceNoDataAvail();
                            onPackageChanged(_arg0);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IPackageChangeObserver {
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

            @Override // android.content.pm.IPackageChangeObserver
            public void onPackageChanged(PackageChangeEvent event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(event, 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
