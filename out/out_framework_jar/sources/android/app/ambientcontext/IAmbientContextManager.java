package android.app.ambientcontext;

import android.app.PendingIntent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteCallback;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IAmbientContextManager extends IInterface {
    public static final String DESCRIPTOR = "android.app.ambientcontext.IAmbientContextManager";

    void queryServiceStatus(int[] iArr, String str, RemoteCallback remoteCallback) throws RemoteException;

    void registerObserver(AmbientContextEventRequest ambientContextEventRequest, PendingIntent pendingIntent, RemoteCallback remoteCallback) throws RemoteException;

    void startConsentActivity(int[] iArr, String str) throws RemoteException;

    void unregisterObserver(String str) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IAmbientContextManager {
        @Override // android.app.ambientcontext.IAmbientContextManager
        public void registerObserver(AmbientContextEventRequest request, PendingIntent resultPendingIntent, RemoteCallback statusCallback) throws RemoteException {
        }

        @Override // android.app.ambientcontext.IAmbientContextManager
        public void unregisterObserver(String callingPackage) throws RemoteException {
        }

        @Override // android.app.ambientcontext.IAmbientContextManager
        public void queryServiceStatus(int[] eventTypes, String callingPackage, RemoteCallback statusCallback) throws RemoteException {
        }

        @Override // android.app.ambientcontext.IAmbientContextManager
        public void startConsentActivity(int[] eventTypes, String callingPackage) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IAmbientContextManager {
        static final int TRANSACTION_queryServiceStatus = 3;
        static final int TRANSACTION_registerObserver = 1;
        static final int TRANSACTION_startConsentActivity = 4;
        static final int TRANSACTION_unregisterObserver = 2;

        public Stub() {
            attachInterface(this, IAmbientContextManager.DESCRIPTOR);
        }

        public static IAmbientContextManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IAmbientContextManager.DESCRIPTOR);
            if (iin != null && (iin instanceof IAmbientContextManager)) {
                return (IAmbientContextManager) iin;
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
                    return "registerObserver";
                case 2:
                    return "unregisterObserver";
                case 3:
                    return "queryServiceStatus";
                case 4:
                    return "startConsentActivity";
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
                data.enforceInterface(IAmbientContextManager.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IAmbientContextManager.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            AmbientContextEventRequest _arg0 = (AmbientContextEventRequest) data.readTypedObject(AmbientContextEventRequest.CREATOR);
                            PendingIntent _arg1 = (PendingIntent) data.readTypedObject(PendingIntent.CREATOR);
                            RemoteCallback _arg2 = (RemoteCallback) data.readTypedObject(RemoteCallback.CREATOR);
                            data.enforceNoDataAvail();
                            registerObserver(_arg0, _arg1, _arg2);
                            reply.writeNoException();
                            break;
                        case 2:
                            String _arg02 = data.readString();
                            data.enforceNoDataAvail();
                            unregisterObserver(_arg02);
                            reply.writeNoException();
                            break;
                        case 3:
                            int[] _arg03 = data.createIntArray();
                            String _arg12 = data.readString();
                            RemoteCallback _arg22 = (RemoteCallback) data.readTypedObject(RemoteCallback.CREATOR);
                            data.enforceNoDataAvail();
                            queryServiceStatus(_arg03, _arg12, _arg22);
                            reply.writeNoException();
                            break;
                        case 4:
                            int[] _arg04 = data.createIntArray();
                            String _arg13 = data.readString();
                            data.enforceNoDataAvail();
                            startConsentActivity(_arg04, _arg13);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IAmbientContextManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IAmbientContextManager.DESCRIPTOR;
            }

            @Override // android.app.ambientcontext.IAmbientContextManager
            public void registerObserver(AmbientContextEventRequest request, PendingIntent resultPendingIntent, RemoteCallback statusCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAmbientContextManager.DESCRIPTOR);
                    _data.writeTypedObject(request, 0);
                    _data.writeTypedObject(resultPendingIntent, 0);
                    _data.writeTypedObject(statusCallback, 0);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ambientcontext.IAmbientContextManager
            public void unregisterObserver(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAmbientContextManager.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ambientcontext.IAmbientContextManager
            public void queryServiceStatus(int[] eventTypes, String callingPackage, RemoteCallback statusCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAmbientContextManager.DESCRIPTOR);
                    _data.writeIntArray(eventTypes);
                    _data.writeString(callingPackage);
                    _data.writeTypedObject(statusCallback, 0);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ambientcontext.IAmbientContextManager
            public void startConsentActivity(int[] eventTypes, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAmbientContextManager.DESCRIPTOR);
                    _data.writeIntArray(eventTypes);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 3;
        }
    }
}
