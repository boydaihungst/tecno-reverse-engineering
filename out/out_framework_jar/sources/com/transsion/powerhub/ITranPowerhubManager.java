package com.transsion.powerhub;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes4.dex */
public interface ITranPowerhubManager extends IInterface {
    public static final String DESCRIPTOR = "com.transsion.powerhub.ITranPowerhubManager";

    Bundle getSkipInfo(Bundle bundle) throws RemoteException;

    void updateCollectData(Bundle bundle) throws RemoteException;

    /* loaded from: classes4.dex */
    public static class Default implements ITranPowerhubManager {
        @Override // com.transsion.powerhub.ITranPowerhubManager
        public Bundle getSkipInfo(Bundle viewInfo) throws RemoteException {
            return null;
        }

        @Override // com.transsion.powerhub.ITranPowerhubManager
        public void updateCollectData(Bundle dataInfo) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class Stub extends Binder implements ITranPowerhubManager {
        static final int TRANSACTION_getSkipInfo = 1;
        static final int TRANSACTION_updateCollectData = 2;

        public Stub() {
            attachInterface(this, ITranPowerhubManager.DESCRIPTOR);
        }

        public static ITranPowerhubManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ITranPowerhubManager.DESCRIPTOR);
            if (iin != null && (iin instanceof ITranPowerhubManager)) {
                return (ITranPowerhubManager) iin;
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
                    return "getSkipInfo";
                case 2:
                    return "updateCollectData";
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
                data.enforceInterface(ITranPowerhubManager.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ITranPowerhubManager.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            Bundle _arg0 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            Bundle _result = getSkipInfo(_arg0);
                            reply.writeNoException();
                            reply.writeTypedObject(_result, 1);
                            break;
                        case 2:
                            Bundle _arg02 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            updateCollectData(_arg02);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes4.dex */
        private static class Proxy implements ITranPowerhubManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ITranPowerhubManager.DESCRIPTOR;
            }

            @Override // com.transsion.powerhub.ITranPowerhubManager
            public Bundle getSkipInfo(Bundle viewInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITranPowerhubManager.DESCRIPTOR);
                    _data.writeTypedObject(viewInfo, 0);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    Bundle _result = (Bundle) _reply.readTypedObject(Bundle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.transsion.powerhub.ITranPowerhubManager
            public void updateCollectData(Bundle dataInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITranPowerhubManager.DESCRIPTOR);
                    _data.writeTypedObject(dataInfo, 0);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
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
