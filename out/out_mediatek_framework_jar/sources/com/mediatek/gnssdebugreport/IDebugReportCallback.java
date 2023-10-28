package com.mediatek.gnssdebugreport;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IDebugReportCallback extends IInterface {
    public static final String DESCRIPTOR = "com.mediatek.gnssdebugreport.IDebugReportCallback";

    void onDebugReportAvailable(Bundle bundle) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IDebugReportCallback {
        @Override // com.mediatek.gnssdebugreport.IDebugReportCallback
        public void onDebugReportAvailable(Bundle debugReport) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IDebugReportCallback {
        static final int TRANSACTION_onDebugReportAvailable = 1;

        public Stub() {
            attachInterface(this, IDebugReportCallback.DESCRIPTOR);
        }

        public static IDebugReportCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IDebugReportCallback.DESCRIPTOR);
            if (iin != null && (iin instanceof IDebugReportCallback)) {
                return (IDebugReportCallback) iin;
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
                data.enforceInterface(IDebugReportCallback.DESCRIPTOR);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(IDebugReportCallback.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            Bundle _arg0 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            onDebugReportAvailable(_arg0);
                            reply.writeNoException();
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IDebugReportCallback {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IDebugReportCallback.DESCRIPTOR;
            }

            @Override // com.mediatek.gnssdebugreport.IDebugReportCallback
            public void onDebugReportAvailable(Bundle debugReport) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDebugReportCallback.DESCRIPTOR);
                    _data.writeTypedObject(debugReport, 0);
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
