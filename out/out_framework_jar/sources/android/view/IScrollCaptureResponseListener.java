package android.view;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes3.dex */
public interface IScrollCaptureResponseListener extends IInterface {
    public static final String DESCRIPTOR = "android.view.IScrollCaptureResponseListener";

    void onScrollCaptureResponse(ScrollCaptureResponse scrollCaptureResponse) throws RemoteException;

    /* loaded from: classes3.dex */
    public static class Default implements IScrollCaptureResponseListener {
        @Override // android.view.IScrollCaptureResponseListener
        public void onScrollCaptureResponse(ScrollCaptureResponse response) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes3.dex */
    public static abstract class Stub extends Binder implements IScrollCaptureResponseListener {
        static final int TRANSACTION_onScrollCaptureResponse = 1;

        public Stub() {
            attachInterface(this, IScrollCaptureResponseListener.DESCRIPTOR);
        }

        public static IScrollCaptureResponseListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IScrollCaptureResponseListener.DESCRIPTOR);
            if (iin != null && (iin instanceof IScrollCaptureResponseListener)) {
                return (IScrollCaptureResponseListener) iin;
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
                    return "onScrollCaptureResponse";
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
                data.enforceInterface(IScrollCaptureResponseListener.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IScrollCaptureResponseListener.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            ScrollCaptureResponse _arg0 = (ScrollCaptureResponse) data.readTypedObject(ScrollCaptureResponse.CREATOR);
                            data.enforceNoDataAvail();
                            onScrollCaptureResponse(_arg0);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes3.dex */
        public static class Proxy implements IScrollCaptureResponseListener {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IScrollCaptureResponseListener.DESCRIPTOR;
            }

            @Override // android.view.IScrollCaptureResponseListener
            public void onScrollCaptureResponse(ScrollCaptureResponse response) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IScrollCaptureResponseListener.DESCRIPTOR);
                    _data.writeTypedObject(response, 0);
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
