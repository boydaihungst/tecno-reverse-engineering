package android.media.musicrecognition;

import android.media.MediaMetadata;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes2.dex */
public interface IMusicRecognitionServiceCallback extends IInterface {
    public static final String DESCRIPTOR = "android.media.musicrecognition.IMusicRecognitionServiceCallback";

    void onRecognitionFailed(int i) throws RemoteException;

    void onRecognitionSucceeded(MediaMetadata mediaMetadata, Bundle bundle) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IMusicRecognitionServiceCallback {
        @Override // android.media.musicrecognition.IMusicRecognitionServiceCallback
        public void onRecognitionSucceeded(MediaMetadata result, Bundle extras) throws RemoteException {
        }

        @Override // android.media.musicrecognition.IMusicRecognitionServiceCallback
        public void onRecognitionFailed(int failureCode) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IMusicRecognitionServiceCallback {
        static final int TRANSACTION_onRecognitionFailed = 2;
        static final int TRANSACTION_onRecognitionSucceeded = 1;

        public Stub() {
            attachInterface(this, IMusicRecognitionServiceCallback.DESCRIPTOR);
        }

        public static IMusicRecognitionServiceCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IMusicRecognitionServiceCallback.DESCRIPTOR);
            if (iin != null && (iin instanceof IMusicRecognitionServiceCallback)) {
                return (IMusicRecognitionServiceCallback) iin;
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
                    return "onRecognitionSucceeded";
                case 2:
                    return "onRecognitionFailed";
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
                data.enforceInterface(IMusicRecognitionServiceCallback.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IMusicRecognitionServiceCallback.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            MediaMetadata _arg0 = (MediaMetadata) data.readTypedObject(MediaMetadata.CREATOR);
                            Bundle _arg1 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            onRecognitionSucceeded(_arg0, _arg1);
                            break;
                        case 2:
                            int _arg02 = data.readInt();
                            data.enforceNoDataAvail();
                            onRecognitionFailed(_arg02);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IMusicRecognitionServiceCallback {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IMusicRecognitionServiceCallback.DESCRIPTOR;
            }

            @Override // android.media.musicrecognition.IMusicRecognitionServiceCallback
            public void onRecognitionSucceeded(MediaMetadata result, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMusicRecognitionServiceCallback.DESCRIPTOR);
                    _data.writeTypedObject(result, 0);
                    _data.writeTypedObject(extras, 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.musicrecognition.IMusicRecognitionServiceCallback
            public void onRecognitionFailed(int failureCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMusicRecognitionServiceCallback.DESCRIPTOR);
                    _data.writeInt(failureCode);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 1;
        }
    }
}