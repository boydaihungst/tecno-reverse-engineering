package android.telephony.ims.aidl;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsReasonInfo;
import com.android.ims.internal.IImsCallSession;
/* loaded from: classes3.dex */
public interface IImsMmTelListener extends IInterface {
    public static final String DESCRIPTOR = "android.telephony.ims.aidl.IImsMmTelListener";

    void onIncomingCall(IImsCallSession iImsCallSession, Bundle bundle) throws RemoteException;

    void onRejectedCall(ImsCallProfile imsCallProfile, ImsReasonInfo imsReasonInfo) throws RemoteException;

    void onVoiceMessageCountUpdate(int i) throws RemoteException;

    /* loaded from: classes3.dex */
    public static class Default implements IImsMmTelListener {
        @Override // android.telephony.ims.aidl.IImsMmTelListener
        public void onIncomingCall(IImsCallSession c, Bundle extras) throws RemoteException {
        }

        @Override // android.telephony.ims.aidl.IImsMmTelListener
        public void onRejectedCall(ImsCallProfile callProfile, ImsReasonInfo reason) throws RemoteException {
        }

        @Override // android.telephony.ims.aidl.IImsMmTelListener
        public void onVoiceMessageCountUpdate(int count) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes3.dex */
    public static abstract class Stub extends Binder implements IImsMmTelListener {
        static final int TRANSACTION_onIncomingCall = 1;
        static final int TRANSACTION_onRejectedCall = 2;
        static final int TRANSACTION_onVoiceMessageCountUpdate = 3;

        public Stub() {
            attachInterface(this, IImsMmTelListener.DESCRIPTOR);
        }

        public static IImsMmTelListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IImsMmTelListener.DESCRIPTOR);
            if (iin != null && (iin instanceof IImsMmTelListener)) {
                return (IImsMmTelListener) iin;
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
                    return "onIncomingCall";
                case 2:
                    return "onRejectedCall";
                case 3:
                    return "onVoiceMessageCountUpdate";
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
                data.enforceInterface(IImsMmTelListener.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IImsMmTelListener.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            IImsCallSession _arg0 = IImsCallSession.Stub.asInterface(data.readStrongBinder());
                            Bundle _arg1 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            onIncomingCall(_arg0, _arg1);
                            reply.writeNoException();
                            break;
                        case 2:
                            ImsCallProfile _arg02 = (ImsCallProfile) data.readTypedObject(ImsCallProfile.CREATOR);
                            ImsReasonInfo _arg12 = (ImsReasonInfo) data.readTypedObject(ImsReasonInfo.CREATOR);
                            data.enforceNoDataAvail();
                            onRejectedCall(_arg02, _arg12);
                            reply.writeNoException();
                            break;
                        case 3:
                            int _arg03 = data.readInt();
                            data.enforceNoDataAvail();
                            onVoiceMessageCountUpdate(_arg03);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes3.dex */
        private static class Proxy implements IImsMmTelListener {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IImsMmTelListener.DESCRIPTOR;
            }

            @Override // android.telephony.ims.aidl.IImsMmTelListener
            public void onIncomingCall(IImsCallSession c, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelListener.DESCRIPTOR);
                    _data.writeStrongInterface(c);
                    _data.writeTypedObject(extras, 0);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsMmTelListener
            public void onRejectedCall(ImsCallProfile callProfile, ImsReasonInfo reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelListener.DESCRIPTOR);
                    _data.writeTypedObject(callProfile, 0);
                    _data.writeTypedObject(reason, 0);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsMmTelListener
            public void onVoiceMessageCountUpdate(int count) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelListener.DESCRIPTOR);
                    _data.writeInt(count);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 2;
        }
    }
}
