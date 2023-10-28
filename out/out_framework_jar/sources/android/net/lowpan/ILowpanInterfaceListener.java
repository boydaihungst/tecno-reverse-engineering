package android.net.lowpan;

import android.net.IpPrefix;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes2.dex */
public interface ILowpanInterfaceListener extends IInterface {
    public static final String DESCRIPTOR = "android.net.lowpan.ILowpanInterfaceListener";

    void onConnectedChanged(boolean z) throws RemoteException;

    void onEnabledChanged(boolean z) throws RemoteException;

    void onLinkAddressAdded(String str) throws RemoteException;

    void onLinkAddressRemoved(String str) throws RemoteException;

    void onLinkNetworkAdded(IpPrefix ipPrefix) throws RemoteException;

    void onLinkNetworkRemoved(IpPrefix ipPrefix) throws RemoteException;

    void onLowpanIdentityChanged(LowpanIdentity lowpanIdentity) throws RemoteException;

    void onReceiveFromCommissioner(byte[] bArr) throws RemoteException;

    void onRoleChanged(String str) throws RemoteException;

    void onStateChanged(String str) throws RemoteException;

    void onUpChanged(boolean z) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements ILowpanInterfaceListener {
        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onEnabledChanged(boolean value) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onConnectedChanged(boolean value) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onUpChanged(boolean value) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onRoleChanged(String value) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onStateChanged(String value) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onLowpanIdentityChanged(LowpanIdentity value) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onLinkNetworkAdded(IpPrefix value) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onLinkNetworkRemoved(IpPrefix value) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onLinkAddressAdded(String value) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onLinkAddressRemoved(String value) throws RemoteException {
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onReceiveFromCommissioner(byte[] packet) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements ILowpanInterfaceListener {
        static final int TRANSACTION_onConnectedChanged = 2;
        static final int TRANSACTION_onEnabledChanged = 1;
        static final int TRANSACTION_onLinkAddressAdded = 9;
        static final int TRANSACTION_onLinkAddressRemoved = 10;
        static final int TRANSACTION_onLinkNetworkAdded = 7;
        static final int TRANSACTION_onLinkNetworkRemoved = 8;
        static final int TRANSACTION_onLowpanIdentityChanged = 6;
        static final int TRANSACTION_onReceiveFromCommissioner = 11;
        static final int TRANSACTION_onRoleChanged = 4;
        static final int TRANSACTION_onStateChanged = 5;
        static final int TRANSACTION_onUpChanged = 3;

        public Stub() {
            attachInterface(this, ILowpanInterfaceListener.DESCRIPTOR);
        }

        public static ILowpanInterfaceListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ILowpanInterfaceListener.DESCRIPTOR);
            if (iin != null && (iin instanceof ILowpanInterfaceListener)) {
                return (ILowpanInterfaceListener) iin;
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
                    return "onEnabledChanged";
                case 2:
                    return "onConnectedChanged";
                case 3:
                    return "onUpChanged";
                case 4:
                    return "onRoleChanged";
                case 5:
                    return "onStateChanged";
                case 6:
                    return "onLowpanIdentityChanged";
                case 7:
                    return "onLinkNetworkAdded";
                case 8:
                    return "onLinkNetworkRemoved";
                case 9:
                    return "onLinkAddressAdded";
                case 10:
                    return "onLinkAddressRemoved";
                case 11:
                    return "onReceiveFromCommissioner";
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
                data.enforceInterface(ILowpanInterfaceListener.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ILowpanInterfaceListener.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            boolean _arg0 = data.readBoolean();
                            data.enforceNoDataAvail();
                            onEnabledChanged(_arg0);
                            break;
                        case 2:
                            boolean _arg02 = data.readBoolean();
                            data.enforceNoDataAvail();
                            onConnectedChanged(_arg02);
                            break;
                        case 3:
                            boolean _arg03 = data.readBoolean();
                            data.enforceNoDataAvail();
                            onUpChanged(_arg03);
                            break;
                        case 4:
                            String _arg04 = data.readString();
                            data.enforceNoDataAvail();
                            onRoleChanged(_arg04);
                            break;
                        case 5:
                            String _arg05 = data.readString();
                            data.enforceNoDataAvail();
                            onStateChanged(_arg05);
                            break;
                        case 6:
                            LowpanIdentity _arg06 = (LowpanIdentity) data.readTypedObject(LowpanIdentity.CREATOR);
                            data.enforceNoDataAvail();
                            onLowpanIdentityChanged(_arg06);
                            break;
                        case 7:
                            IpPrefix _arg07 = (IpPrefix) data.readTypedObject(IpPrefix.CREATOR);
                            data.enforceNoDataAvail();
                            onLinkNetworkAdded(_arg07);
                            break;
                        case 8:
                            IpPrefix _arg08 = (IpPrefix) data.readTypedObject(IpPrefix.CREATOR);
                            data.enforceNoDataAvail();
                            onLinkNetworkRemoved(_arg08);
                            break;
                        case 9:
                            String _arg09 = data.readString();
                            data.enforceNoDataAvail();
                            onLinkAddressAdded(_arg09);
                            break;
                        case 10:
                            String _arg010 = data.readString();
                            data.enforceNoDataAvail();
                            onLinkAddressRemoved(_arg010);
                            break;
                        case 11:
                            byte[] _arg011 = data.createByteArray();
                            data.enforceNoDataAvail();
                            onReceiveFromCommissioner(_arg011);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements ILowpanInterfaceListener {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ILowpanInterfaceListener.DESCRIPTOR;
            }

            @Override // android.net.lowpan.ILowpanInterfaceListener
            public void onEnabledChanged(boolean value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterfaceListener.DESCRIPTOR);
                    _data.writeBoolean(value);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterfaceListener
            public void onConnectedChanged(boolean value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterfaceListener.DESCRIPTOR);
                    _data.writeBoolean(value);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterfaceListener
            public void onUpChanged(boolean value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterfaceListener.DESCRIPTOR);
                    _data.writeBoolean(value);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterfaceListener
            public void onRoleChanged(String value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterfaceListener.DESCRIPTOR);
                    _data.writeString(value);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterfaceListener
            public void onStateChanged(String value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterfaceListener.DESCRIPTOR);
                    _data.writeString(value);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterfaceListener
            public void onLowpanIdentityChanged(LowpanIdentity value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterfaceListener.DESCRIPTOR);
                    _data.writeTypedObject(value, 0);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterfaceListener
            public void onLinkNetworkAdded(IpPrefix value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterfaceListener.DESCRIPTOR);
                    _data.writeTypedObject(value, 0);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterfaceListener
            public void onLinkNetworkRemoved(IpPrefix value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterfaceListener.DESCRIPTOR);
                    _data.writeTypedObject(value, 0);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterfaceListener
            public void onLinkAddressAdded(String value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterfaceListener.DESCRIPTOR);
                    _data.writeString(value);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterfaceListener
            public void onLinkAddressRemoved(String value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterfaceListener.DESCRIPTOR);
                    _data.writeString(value);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.lowpan.ILowpanInterfaceListener
            public void onReceiveFromCommissioner(byte[] packet) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ILowpanInterfaceListener.DESCRIPTOR);
                    _data.writeByteArray(packet);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 10;
        }
    }
}
