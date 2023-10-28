package android.app;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.LocaleList;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface ILocaleManager extends IInterface {
    public static final String DESCRIPTOR = "android.app.ILocaleManager";

    LocaleList getApplicationLocales(String str, int i) throws RemoteException;

    LocaleList getSystemLocales() throws RemoteException;

    void setApplicationLocales(String str, int i, LocaleList localeList) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements ILocaleManager {
        @Override // android.app.ILocaleManager
        public void setApplicationLocales(String packageName, int userId, LocaleList locales) throws RemoteException {
        }

        @Override // android.app.ILocaleManager
        public LocaleList getApplicationLocales(String packageName, int userId) throws RemoteException {
            return null;
        }

        @Override // android.app.ILocaleManager
        public LocaleList getSystemLocales() throws RemoteException {
            return null;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements ILocaleManager {
        static final int TRANSACTION_getApplicationLocales = 2;
        static final int TRANSACTION_getSystemLocales = 3;
        static final int TRANSACTION_setApplicationLocales = 1;

        public Stub() {
            attachInterface(this, ILocaleManager.DESCRIPTOR);
        }

        public static ILocaleManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ILocaleManager.DESCRIPTOR);
            if (iin != null && (iin instanceof ILocaleManager)) {
                return (ILocaleManager) iin;
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
                    return "setApplicationLocales";
                case 2:
                    return "getApplicationLocales";
                case 3:
                    return "getSystemLocales";
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
                data.enforceInterface(ILocaleManager.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ILocaleManager.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            String _arg0 = data.readString();
                            int _arg1 = data.readInt();
                            LocaleList _arg2 = (LocaleList) data.readTypedObject(LocaleList.CREATOR);
                            data.enforceNoDataAvail();
                            setApplicationLocales(_arg0, _arg1, _arg2);
                            reply.writeNoException();
                            break;
                        case 2:
                            String _arg02 = data.readString();
                            int _arg12 = data.readInt();
                            data.enforceNoDataAvail();
                            LocaleList _result = getApplicationLocales(_arg02, _arg12);
                            reply.writeNoException();
                            reply.writeTypedObject(_result, 1);
                            break;
                        case 3:
                            LocaleList _result2 = getSystemLocales();
                            reply.writeNoException();
                            reply.writeTypedObject(_result2, 1);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements ILocaleManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ILocaleManager.DESCRIPTOR;
            }

            @Override // android.app.ILocaleManager
            public void setApplicationLocales(String packageName, int userId, LocaleList locales) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILocaleManager.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    _data.writeTypedObject(locales, 0);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ILocaleManager
            public LocaleList getApplicationLocales(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILocaleManager.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    LocaleList _result = (LocaleList) _reply.readTypedObject(LocaleList.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ILocaleManager
            public LocaleList getSystemLocales() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILocaleManager.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    LocaleList _result = (LocaleList) _reply.readTypedObject(LocaleList.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
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
