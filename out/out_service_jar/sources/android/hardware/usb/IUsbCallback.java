package android.hardware.usb;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IUsbCallback extends IInterface {
    public static final String DESCRIPTOR = "android$hardware$usb$IUsbCallback".replace('$', '.');
    public static final String HASH = "9762531142d72e03bb4228209846c135f276d40e";
    public static final int VERSION = 1;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void notifyContaminantEnabledStatus(String str, boolean z, int i, long j) throws RemoteException;

    void notifyEnableUsbDataStatus(String str, boolean z, int i, long j) throws RemoteException;

    void notifyEnableUsbDataWhileDockedStatus(String str, int i, long j) throws RemoteException;

    void notifyLimitPowerTransferStatus(String str, boolean z, int i, long j) throws RemoteException;

    void notifyPortStatusChange(PortStatus[] portStatusArr, int i) throws RemoteException;

    void notifyQueryPortStatus(String str, int i, long j) throws RemoteException;

    void notifyResetUsbPortStatus(String str, int i, long j) throws RemoteException;

    void notifyRoleSwitchStatus(String str, PortRole portRole, int i, long j) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IUsbCallback {
        @Override // android.hardware.usb.IUsbCallback
        public void notifyPortStatusChange(PortStatus[] currentPortStatus, int retval) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbCallback
        public void notifyRoleSwitchStatus(String portName, PortRole newRole, int retval, long transactionId) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbCallback
        public void notifyEnableUsbDataStatus(String portName, boolean enable, int retval, long transactionId) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbCallback
        public void notifyEnableUsbDataWhileDockedStatus(String portName, int retval, long transactionId) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbCallback
        public void notifyContaminantEnabledStatus(String portName, boolean enable, int retval, long transactionId) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbCallback
        public void notifyQueryPortStatus(String portName, int retval, long transactionId) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbCallback
        public void notifyLimitPowerTransferStatus(String portName, boolean limit, int retval, long transactionId) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbCallback
        public void notifyResetUsbPortStatus(String portName, int retval, long transactionId) throws RemoteException {
        }

        @Override // android.hardware.usb.IUsbCallback
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.hardware.usb.IUsbCallback
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IUsbCallback {
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_notifyContaminantEnabledStatus = 5;
        static final int TRANSACTION_notifyEnableUsbDataStatus = 3;
        static final int TRANSACTION_notifyEnableUsbDataWhileDockedStatus = 4;
        static final int TRANSACTION_notifyLimitPowerTransferStatus = 7;
        static final int TRANSACTION_notifyPortStatusChange = 1;
        static final int TRANSACTION_notifyQueryPortStatus = 6;
        static final int TRANSACTION_notifyResetUsbPortStatus = 8;
        static final int TRANSACTION_notifyRoleSwitchStatus = 2;

        public Stub() {
            markVintfStability();
            attachInterface(this, DESCRIPTOR);
        }

        public static IUsbCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IUsbCallback)) {
                return (IUsbCallback) iin;
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
                            PortStatus[] _arg0 = (PortStatus[]) data.createTypedArray(PortStatus.CREATOR);
                            int _arg1 = data.readInt();
                            data.enforceNoDataAvail();
                            notifyPortStatusChange(_arg0, _arg1);
                            break;
                        case 2:
                            String _arg02 = data.readString();
                            PortRole _arg12 = (PortRole) data.readTypedObject(PortRole.CREATOR);
                            int _arg2 = data.readInt();
                            long _arg3 = data.readLong();
                            data.enforceNoDataAvail();
                            notifyRoleSwitchStatus(_arg02, _arg12, _arg2, _arg3);
                            break;
                        case 3:
                            String _arg03 = data.readString();
                            boolean _arg13 = data.readBoolean();
                            int _arg22 = data.readInt();
                            long _arg32 = data.readLong();
                            data.enforceNoDataAvail();
                            notifyEnableUsbDataStatus(_arg03, _arg13, _arg22, _arg32);
                            break;
                        case 4:
                            String _arg04 = data.readString();
                            int _arg14 = data.readInt();
                            long _arg23 = data.readLong();
                            data.enforceNoDataAvail();
                            notifyEnableUsbDataWhileDockedStatus(_arg04, _arg14, _arg23);
                            break;
                        case 5:
                            String _arg05 = data.readString();
                            boolean _arg15 = data.readBoolean();
                            int _arg24 = data.readInt();
                            long _arg33 = data.readLong();
                            data.enforceNoDataAvail();
                            notifyContaminantEnabledStatus(_arg05, _arg15, _arg24, _arg33);
                            break;
                        case 6:
                            String _arg06 = data.readString();
                            int _arg16 = data.readInt();
                            long _arg25 = data.readLong();
                            data.enforceNoDataAvail();
                            notifyQueryPortStatus(_arg06, _arg16, _arg25);
                            break;
                        case 7:
                            String _arg07 = data.readString();
                            boolean _arg17 = data.readBoolean();
                            int _arg26 = data.readInt();
                            long _arg34 = data.readLong();
                            data.enforceNoDataAvail();
                            notifyLimitPowerTransferStatus(_arg07, _arg17, _arg26, _arg34);
                            break;
                        case 8:
                            String _arg08 = data.readString();
                            int _arg18 = data.readInt();
                            long _arg27 = data.readLong();
                            data.enforceNoDataAvail();
                            notifyResetUsbPortStatus(_arg08, _arg18, _arg27);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IUsbCallback {
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

            @Override // android.hardware.usb.IUsbCallback
            public void notifyPortStatusChange(PortStatus[] currentPortStatus, int retval) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedArray(currentPortStatus, 0);
                    _data.writeInt(retval);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyPortStatusChange is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbCallback
            public void notifyRoleSwitchStatus(String portName, PortRole newRole, int retval, long transactionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(portName);
                    _data.writeTypedObject(newRole, 0);
                    _data.writeInt(retval);
                    _data.writeLong(transactionId);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyRoleSwitchStatus is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbCallback
            public void notifyEnableUsbDataStatus(String portName, boolean enable, int retval, long transactionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(portName);
                    _data.writeBoolean(enable);
                    _data.writeInt(retval);
                    _data.writeLong(transactionId);
                    boolean _status = this.mRemote.transact(3, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyEnableUsbDataStatus is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbCallback
            public void notifyEnableUsbDataWhileDockedStatus(String portName, int retval, long transactionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(portName);
                    _data.writeInt(retval);
                    _data.writeLong(transactionId);
                    boolean _status = this.mRemote.transact(4, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyEnableUsbDataWhileDockedStatus is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbCallback
            public void notifyContaminantEnabledStatus(String portName, boolean enable, int retval, long transactionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(portName);
                    _data.writeBoolean(enable);
                    _data.writeInt(retval);
                    _data.writeLong(transactionId);
                    boolean _status = this.mRemote.transact(5, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyContaminantEnabledStatus is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbCallback
            public void notifyQueryPortStatus(String portName, int retval, long transactionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(portName);
                    _data.writeInt(retval);
                    _data.writeLong(transactionId);
                    boolean _status = this.mRemote.transact(6, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyQueryPortStatus is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbCallback
            public void notifyLimitPowerTransferStatus(String portName, boolean limit, int retval, long transactionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(portName);
                    _data.writeBoolean(limit);
                    _data.writeInt(retval);
                    _data.writeLong(transactionId);
                    boolean _status = this.mRemote.transact(7, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyLimitPowerTransferStatus is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbCallback
            public void notifyResetUsbPortStatus(String portName, int retval, long transactionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(portName);
                    _data.writeInt(retval);
                    _data.writeLong(transactionId);
                    boolean _status = this.mRemote.transact(8, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method notifyResetUsbPortStatus is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.usb.IUsbCallback
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

            @Override // android.hardware.usb.IUsbCallback
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
