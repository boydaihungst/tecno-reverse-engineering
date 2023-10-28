package android.hardware.usb;

import android.hardware.usb.IUsbCallback;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IUsb extends IInterface {
    public static final String DESCRIPTOR = "android$hardware$usb$IUsb".replace('$', '.');
    public static final String HASH = "9762531142d72e03bb4228209846c135f276d40e";
    public static final int VERSION = 1;

    void enableContaminantPresenceDetection(String str, boolean z, long j) throws RemoteException;

    void enableUsbData(String str, boolean z, long j) throws RemoteException;

    void enableUsbDataWhileDocked(String str, long j) throws RemoteException;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void limitPowerTransfer(String str, boolean z, long j) throws RemoteException;

    void queryPortStatus(long j) throws RemoteException;

    void resetUsbPort(String str, long j) throws RemoteException;

    void setCallback(IUsbCallback iUsbCallback) throws RemoteException;

    void switchRole(String str, PortRole portRole, long j) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IUsb {
        @Override // android.hardware.usb.IUsb
        public void enableContaminantPresenceDetection(String portName, boolean enable, long transactionId) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsb
        public void enableUsbData(String portName, boolean enable, long transactionId) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsb
        public void enableUsbDataWhileDocked(String portName, long transactionId) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsb
        public void queryPortStatus(long transactionId) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsb
        public void setCallback(IUsbCallback callback) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsb
        public void switchRole(String portName, PortRole role, long transactionId) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsb
        public void limitPowerTransfer(String portName, boolean limit, long transactionId) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsb
        public void resetUsbPort(String portName, long transactionId) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsb
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.hardware.usb.IUsb
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IUsb {
        static final int TRANSACTION_enableContaminantPresenceDetection = 1;
        static final int TRANSACTION_enableUsbData = 2;
        static final int TRANSACTION_enableUsbDataWhileDocked = 3;
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_limitPowerTransfer = 7;
        static final int TRANSACTION_queryPortStatus = 4;
        static final int TRANSACTION_resetUsbPort = 8;
        static final int TRANSACTION_setCallback = 5;
        static final int TRANSACTION_switchRole = 6;

        public Stub() {
            markVintfStability();
            attachInterface(this, DESCRIPTOR);
        }

        public static IUsb asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IUsb)) {
                return (IUsb) iin;
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
                case TRANSACTION_getInterfaceHash /* 16777214 */:
                    reply.writeNoException();
                    reply.writeString(getInterfaceHash());
                    return true;
                case 16777215:
                    reply.writeNoException();
                    reply.writeInt(getInterfaceVersion());
                    return true;
                case 1598968902:
                    reply.writeString(descriptor);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            String _arg0 = data.readString();
                            boolean _arg1 = data.readBoolean();
                            long _arg2 = data.readLong();
                            data.enforceNoDataAvail();
                            enableContaminantPresenceDetection(_arg0, _arg1, _arg2);
                            break;
                        case 2:
                            String _arg02 = data.readString();
                            boolean _arg12 = data.readBoolean();
                            long _arg22 = data.readLong();
                            data.enforceNoDataAvail();
                            enableUsbData(_arg02, _arg12, _arg22);
                            break;
                        case 3:
                            String _arg03 = data.readString();
                            long _arg13 = data.readLong();
                            data.enforceNoDataAvail();
                            enableUsbDataWhileDocked(_arg03, _arg13);
                            break;
                        case 4:
                            long _arg04 = data.readLong();
                            data.enforceNoDataAvail();
                            queryPortStatus(_arg04);
                            break;
                        case 5:
                            IUsbCallback _arg05 = IUsbCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            setCallback(_arg05);
                            break;
                        case 6:
                            String _arg06 = data.readString();
                            PortRole _arg14 = (PortRole) data.readTypedObject(PortRole.CREATOR);
                            long _arg23 = data.readLong();
                            data.enforceNoDataAvail();
                            switchRole(_arg06, _arg14, _arg23);
                            break;
                        case 7:
                            String _arg07 = data.readString();
                            boolean _arg15 = data.readBoolean();
                            long _arg24 = data.readLong();
                            data.enforceNoDataAvail();
                            limitPowerTransfer(_arg07, _arg15, _arg24);
                            break;
                        case 8:
                            String _arg08 = data.readString();
                            long _arg16 = data.readLong();
                            data.enforceNoDataAvail();
                            resetUsbPort(_arg08, _arg16);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IUsb {
            private IBinder mRemote;
            private int mCachedVersion = -1;
            private String mCachedHash = "-1";

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

            @Override // android.hardware.usb.IUsb
            public void enableContaminantPresenceDetection(String portName, boolean enable, long transactionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(portName);
                    _data.writeBoolean(enable);
                    _data.writeLong(transactionId);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method enableContaminantPresenceDetection is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsb
            public void enableUsbData(String portName, boolean enable, long transactionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(portName);
                    _data.writeBoolean(enable);
                    _data.writeLong(transactionId);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method enableUsbData is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsb
            public void enableUsbDataWhileDocked(String portName, long transactionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(portName);
                    _data.writeLong(transactionId);
                    boolean _status = this.mRemote.transact(3, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method enableUsbDataWhileDocked is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsb
            public void queryPortStatus(long transactionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeLong(transactionId);
                    boolean _status = this.mRemote.transact(4, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method queryPortStatus is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsb
            public void setCallback(IUsbCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongInterface(callback);
                    boolean _status = this.mRemote.transact(5, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method setCallback is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsb
            public void switchRole(String portName, PortRole role, long transactionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(portName);
                    _data.writeTypedObject(role, 0);
                    _data.writeLong(transactionId);
                    boolean _status = this.mRemote.transact(6, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method switchRole is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsb
            public void limitPowerTransfer(String portName, boolean limit, long transactionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(portName);
                    _data.writeBoolean(limit);
                    _data.writeLong(transactionId);
                    boolean _status = this.mRemote.transact(7, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method limitPowerTransfer is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsb
            public void resetUsbPort(String portName, long transactionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(portName);
                    _data.writeLong(transactionId);
                    boolean _status = this.mRemote.transact(8, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method resetUsbPort is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsb
            public int getInterfaceVersion() throws RemoteException {
                if (this.mCachedVersion == -1) {
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    try {
                        data.writeInterfaceToken(DESCRIPTOR);
                        this.mRemote.transact(16777215, data, reply, 0);
                        reply.readException();
                        this.mCachedVersion = reply.readInt();
                    } finally {
                        reply.recycle();
                        data.recycle();
                    }
                }
                return this.mCachedVersion;
            }

            @Override // android.hardware.usb.IUsb
            public synchronized String getInterfaceHash() throws RemoteException {
                if ("-1".equals(this.mCachedHash)) {
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    data.writeInterfaceToken(DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getInterfaceHash, data, reply, 0);
                    reply.readException();
                    this.mCachedHash = reply.readString();
                    reply.recycle();
                    data.recycle();
                }
                return this.mCachedHash;
            }
        }
    }
}
