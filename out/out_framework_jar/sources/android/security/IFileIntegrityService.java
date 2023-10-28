package android.security;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes3.dex */
public interface IFileIntegrityService extends IInterface {
    public static final String DESCRIPTOR = "android.security.IFileIntegrityService";

    boolean isApkVeritySupported() throws RemoteException;

    boolean isAppSourceCertificateTrusted(byte[] bArr, String str) throws RemoteException;

    /* loaded from: classes3.dex */
    public static class Default implements IFileIntegrityService {
        @Override // android.security.IFileIntegrityService
        public boolean isApkVeritySupported() throws RemoteException {
            return false;
        }

        @Override // android.security.IFileIntegrityService
        public boolean isAppSourceCertificateTrusted(byte[] certificateBytes, String packageName) throws RemoteException {
            return false;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes3.dex */
    public static abstract class Stub extends Binder implements IFileIntegrityService {
        static final int TRANSACTION_isApkVeritySupported = 1;
        static final int TRANSACTION_isAppSourceCertificateTrusted = 2;

        public Stub() {
            attachInterface(this, IFileIntegrityService.DESCRIPTOR);
        }

        public static IFileIntegrityService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IFileIntegrityService.DESCRIPTOR);
            if (iin != null && (iin instanceof IFileIntegrityService)) {
                return (IFileIntegrityService) iin;
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
                    return "isApkVeritySupported";
                case 2:
                    return "isAppSourceCertificateTrusted";
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
                data.enforceInterface(IFileIntegrityService.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IFileIntegrityService.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            boolean _result = isApkVeritySupported();
                            reply.writeNoException();
                            reply.writeBoolean(_result);
                            break;
                        case 2:
                            byte[] _arg0 = data.createByteArray();
                            String _arg1 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result2 = isAppSourceCertificateTrusted(_arg0, _arg1);
                            reply.writeNoException();
                            reply.writeBoolean(_result2);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes3.dex */
        private static class Proxy implements IFileIntegrityService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IFileIntegrityService.DESCRIPTOR;
            }

            @Override // android.security.IFileIntegrityService
            public boolean isApkVeritySupported() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFileIntegrityService.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.security.IFileIntegrityService
            public boolean isAppSourceCertificateTrusted(byte[] certificateBytes, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IFileIntegrityService.DESCRIPTOR);
                    _data.writeByteArray(certificateBytes);
                    _data.writeString(packageName);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
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
