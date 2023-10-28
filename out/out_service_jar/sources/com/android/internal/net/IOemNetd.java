package com.android.internal.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.internal.net.IOemNetdCallback;
import com.android.internal.net.IOemNetdUnsolicitedEventListener;
/* loaded from: classes.dex */
public interface IOemNetd extends IInterface {
    public static final String DESCRIPTOR = "com$android$internal$net$IOemNetd".replace('$', '.');

    void bindAppUidToNetwork(String str, int i) throws RemoteException;

    boolean isAlive() throws RemoteException;

    void registerOemUnsolicitedEventListener(IOemNetdUnsolicitedEventListener iOemNetdUnsolicitedEventListener) throws RemoteException;

    void sendNetworkOptimizeMsg(String str, int i, String[] strArr, boolean z, IOemNetdCallback iOemNetdCallback) throws RemoteException;

    void setMultiLink(int i, int i2, boolean z) throws RemoteException;

    void unbindAppUidToNetwork(int i) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IOemNetd {
        @Override // com.android.internal.net.IOemNetd
        public boolean isAlive() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public void bindAppUidToNetwork(String ifName, int uid) throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public void unbindAppUidToNetwork(int uid) throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public void registerOemUnsolicitedEventListener(IOemNetdUnsolicitedEventListener listener) throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public void sendNetworkOptimizeMsg(String adapter, int markId, String[] uids, boolean isLimit, IOemNetdCallback resultCallback) throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public void setMultiLink(int uid, int netId, boolean isMultiLink) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IOemNetd {
        static final int TRANSACTION_bindAppUidToNetwork = 2;
        static final int TRANSACTION_isAlive = 1;
        static final int TRANSACTION_registerOemUnsolicitedEventListener = 4;
        static final int TRANSACTION_sendNetworkOptimizeMsg = 5;
        static final int TRANSACTION_setMultiLink = 6;
        static final int TRANSACTION_unbindAppUidToNetwork = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IOemNetd asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IOemNetd)) {
                return (IOemNetd) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(descriptor);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(descriptor);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            boolean _result = isAlive();
                            reply.writeNoException();
                            reply.writeBoolean(_result);
                            break;
                        case 2:
                            String _arg0 = data.readString();
                            int _arg1 = data.readInt();
                            data.enforceNoDataAvail();
                            bindAppUidToNetwork(_arg0, _arg1);
                            reply.writeNoException();
                            break;
                        case 3:
                            int _arg02 = data.readInt();
                            data.enforceNoDataAvail();
                            unbindAppUidToNetwork(_arg02);
                            reply.writeNoException();
                            break;
                        case 4:
                            IOemNetdUnsolicitedEventListener _arg03 = IOemNetdUnsolicitedEventListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            registerOemUnsolicitedEventListener(_arg03);
                            reply.writeNoException();
                            break;
                        case 5:
                            String _arg04 = data.readString();
                            int _arg12 = data.readInt();
                            String[] _arg2 = data.createStringArray();
                            boolean _arg3 = data.readBoolean();
                            IOemNetdCallback _arg4 = IOemNetdCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            sendNetworkOptimizeMsg(_arg04, _arg12, _arg2, _arg3, _arg4);
                            break;
                        case 6:
                            int _arg05 = data.readInt();
                            int _arg13 = data.readInt();
                            boolean _arg22 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setMultiLink(_arg05, _arg13, _arg22);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IOemNetd {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean isAlive() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void bindAppUidToNetwork(String ifName, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(ifName);
                    _data.writeInt(uid);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void unbindAppUidToNetwork(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void registerOemUnsolicitedEventListener(IOemNetdUnsolicitedEventListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void sendNetworkOptimizeMsg(String adapter, int markId, String[] uids, boolean isLimit, IOemNetdCallback resultCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(adapter);
                    _data.writeInt(markId);
                    _data.writeStringArray(uids);
                    _data.writeBoolean(isLimit);
                    _data.writeStrongInterface(resultCallback);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void setMultiLink(int uid, int netId, boolean isMultiLink) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(netId);
                    _data.writeBoolean(isMultiLink);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
