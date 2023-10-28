package com.android.internal.os;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.Map;
/* loaded from: classes4.dex */
public interface IBinaryTransparencyService extends IInterface {
    public static final String DESCRIPTOR = "com.android.internal.os.IBinaryTransparencyService";

    Map getApexInfo() throws RemoteException;

    String getSignedImageInfo() throws RemoteException;

    /* loaded from: classes4.dex */
    public static class Default implements IBinaryTransparencyService {
        @Override // com.android.internal.os.IBinaryTransparencyService
        public String getSignedImageInfo() throws RemoteException {
            return null;
        }

        @Override // com.android.internal.os.IBinaryTransparencyService
        public Map getApexInfo() throws RemoteException {
            return null;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class Stub extends Binder implements IBinaryTransparencyService {
        static final int TRANSACTION_getApexInfo = 2;
        static final int TRANSACTION_getSignedImageInfo = 1;

        public Stub() {
            attachInterface(this, IBinaryTransparencyService.DESCRIPTOR);
        }

        public static IBinaryTransparencyService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IBinaryTransparencyService.DESCRIPTOR);
            if (iin != null && (iin instanceof IBinaryTransparencyService)) {
                return (IBinaryTransparencyService) iin;
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
                    return "getSignedImageInfo";
                case 2:
                    return "getApexInfo";
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
                data.enforceInterface(IBinaryTransparencyService.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IBinaryTransparencyService.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            String _result = getSignedImageInfo();
                            reply.writeNoException();
                            reply.writeString(_result);
                            break;
                        case 2:
                            Map _result2 = getApexInfo();
                            reply.writeNoException();
                            reply.writeMap(_result2);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes4.dex */
        private static class Proxy implements IBinaryTransparencyService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IBinaryTransparencyService.DESCRIPTOR;
            }

            @Override // com.android.internal.os.IBinaryTransparencyService
            public String getSignedImageInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IBinaryTransparencyService.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.os.IBinaryTransparencyService
            public Map getApexInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IBinaryTransparencyService.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    ClassLoader cl = getClass().getClassLoader();
                    Map _result = _reply.readHashMap(cl);
                    return _result;
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
