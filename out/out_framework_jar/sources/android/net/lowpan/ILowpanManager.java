package android.net.lowpan;

import android.net.lowpan.ILowpanInterface;
import android.net.lowpan.ILowpanManagerListener;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes2.dex */
public interface ILowpanManager extends IInterface {
    public static final String DESCRIPTOR = "android.net.lowpan.ILowpanManager";
    public static final String LOWPAN_SERVICE_NAME = "lowpan";

    void addInterface(ILowpanInterface iLowpanInterface) throws RemoteException;

    void addListener(ILowpanManagerListener iLowpanManagerListener) throws RemoteException;

    ILowpanInterface getInterface(String str) throws RemoteException;

    String[] getInterfaceList() throws RemoteException;

    void removeInterface(ILowpanInterface iLowpanInterface) throws RemoteException;

    void removeListener(ILowpanManagerListener iLowpanManagerListener) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements ILowpanManager {
        @Override // android.net.lowpan.ILowpanManager
        public ILowpanInterface getInterface(String name) throws RemoteException {
            return null;
        }

        @Override // android.net.lowpan.ILowpanManager
        public String[] getInterfaceList() throws RemoteException {
            return null;
        }

        @Override // android.net.lowpan.ILowpanManager
        public void addListener(ILowpanManagerListener listener) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanManager
        public void removeListener(ILowpanManagerListener listener) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanManager
        public void addInterface(ILowpanInterface lowpan_interface) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanManager
        public void removeInterface(ILowpanInterface lowpan_interface) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements ILowpanManager {
        static final int TRANSACTION_addInterface = 5;
        static final int TRANSACTION_addListener = 3;
        static final int TRANSACTION_getInterface = 1;
        static final int TRANSACTION_getInterfaceList = 2;
        static final int TRANSACTION_removeInterface = 6;
        static final int TRANSACTION_removeListener = 4;

        public Stub() {
            attachInterface(this, ILowpanManager.DESCRIPTOR);
        }

        public static ILowpanManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ILowpanManager.DESCRIPTOR);
            if (iin != null && (iin instanceof ILowpanManager)) {
                return (ILowpanManager) iin;
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
                    return "getInterface";
                case 2:
                    return "getInterfaceList";
                case 3:
                    return "addListener";
                case 4:
                    return "removeListener";
                case 5:
                    return "addInterface";
                case 6:
                    return "removeInterface";
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
                data.enforceInterface(ILowpanManager.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ILowpanManager.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            String _arg0 = data.readString();
                            data.enforceNoDataAvail();
                            ILowpanInterface _result = getInterface(_arg0);
                            reply.writeNoException();
                            reply.writeStrongInterface(_result);
                            break;
                        case 2:
                            String[] _result2 = getInterfaceList();
                            reply.writeNoException();
                            reply.writeStringArray(_result2);
                            break;
                        case 3:
                            ILowpanManagerListener _arg02 = ILowpanManagerListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            addListener(_arg02);
                            reply.writeNoException();
                            break;
                        case 4:
                            ILowpanManagerListener _arg03 = ILowpanManagerListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            removeListener(_arg03);
                            reply.writeNoException();
                            break;
                        case 5:
                            ILowpanInterface _arg04 = ILowpanInterface.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            addInterface(_arg04);
                            reply.writeNoException();
                            break;
                        case 6:
                            ILowpanInterface _arg05 = ILowpanInterface.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            removeInterface(_arg05);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements ILowpanManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ILowpanManager.DESCRIPTOR;
            }

            @Override // android.net.lowpan.ILowpanManager
            public ILowpanInterface getInterface(String name) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanManager.DESCRIPTOR);
                    _data.writeString(name);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    ILowpanInterface _result = ILowpanInterface.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanManager
            public String[] getInterfaceList() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanManager.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanManager
            public void addListener(ILowpanManagerListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanManager.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanManager
            public void removeListener(ILowpanManagerListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanManager.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanManager
            public void addInterface(ILowpanInterface lowpan_interface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanManager.DESCRIPTOR);
                    _data.writeStrongInterface(lowpan_interface);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanManager
            public void removeInterface(ILowpanInterface lowpan_interface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanManager.DESCRIPTOR);
                    _data.writeStrongInterface(lowpan_interface);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 5;
        }
    }
}
