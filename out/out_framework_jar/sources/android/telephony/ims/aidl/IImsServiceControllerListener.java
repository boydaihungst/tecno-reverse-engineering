package android.telephony.ims.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.ims.stub.ImsFeatureConfiguration;
/* loaded from: classes3.dex */
public interface IImsServiceControllerListener extends IInterface {
    public static final String DESCRIPTOR = "android.telephony.ims.aidl.IImsServiceControllerListener";

    void onUpdateSupportedImsFeatures(ImsFeatureConfiguration imsFeatureConfiguration) throws RemoteException;

    /* loaded from: classes3.dex */
    public static class Default implements IImsServiceControllerListener {
        @Override // android.telephony.ims.aidl.IImsServiceControllerListener
        public void onUpdateSupportedImsFeatures(ImsFeatureConfiguration c) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes3.dex */
    public static abstract class Stub extends Binder implements IImsServiceControllerListener {
        static final int TRANSACTION_onUpdateSupportedImsFeatures = 1;

        public Stub() {
            attachInterface(this, IImsServiceControllerListener.DESCRIPTOR);
        }

        public static IImsServiceControllerListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IImsServiceControllerListener.DESCRIPTOR);
            if (iin != null && (iin instanceof IImsServiceControllerListener)) {
                return (IImsServiceControllerListener) iin;
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
                    return "onUpdateSupportedImsFeatures";
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
                data.enforceInterface(IImsServiceControllerListener.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IImsServiceControllerListener.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            ImsFeatureConfiguration _arg0 = (ImsFeatureConfiguration) data.readTypedObject(ImsFeatureConfiguration.CREATOR);
                            data.enforceNoDataAvail();
                            onUpdateSupportedImsFeatures(_arg0);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes3.dex */
        private static class Proxy implements IImsServiceControllerListener {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IImsServiceControllerListener.DESCRIPTOR;
            }

            @Override // android.telephony.ims.aidl.IImsServiceControllerListener
            public void onUpdateSupportedImsFeatures(ImsFeatureConfiguration c) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsServiceControllerListener.DESCRIPTOR);
                    _data.writeTypedObject(c, 0);
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
