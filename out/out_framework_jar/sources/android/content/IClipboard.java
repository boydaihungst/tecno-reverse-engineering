package android.content;

import android.content.IOnPrimaryClipChangedListener;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IClipboard extends IInterface {
    void addPrimaryClipChangedListener(IOnPrimaryClipChangedListener iOnPrimaryClipChangedListener, String str, int i) throws RemoteException;

    void clearPrimaryClip(String str, int i) throws RemoteException;

    ClipData getPrimaryClip(String str, int i) throws RemoteException;

    ClipDescription getPrimaryClipDescription(String str, int i) throws RemoteException;

    String getPrimaryClipSource(String str, int i) throws RemoteException;

    boolean hasClipboardText(String str, int i) throws RemoteException;

    boolean hasPrimaryClip(String str, int i) throws RemoteException;

    void removePrimaryClipChangedListener(IOnPrimaryClipChangedListener iOnPrimaryClipChangedListener, String str, int i) throws RemoteException;

    void setPrimaryClip(ClipData clipData, String str, int i) throws RemoteException;

    void setPrimaryClipAsPackage(ClipData clipData, String str, int i, String str2) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IClipboard {
        @Override // android.content.IClipboard
        public void setPrimaryClip(ClipData clip, String callingPackage, int userId) throws RemoteException {
        }

        @Override // android.content.IClipboard
        public void setPrimaryClipAsPackage(ClipData clip, String callingPackage, int userId, String sourcePackage) throws RemoteException {
        }

        @Override // android.content.IClipboard
        public void clearPrimaryClip(String callingPackage, int userId) throws RemoteException {
        }

        @Override // android.content.IClipboard
        public ClipData getPrimaryClip(String pkg, int userId) throws RemoteException {
            return null;
        }

        @Override // android.content.IClipboard
        public ClipDescription getPrimaryClipDescription(String callingPackage, int userId) throws RemoteException {
            return null;
        }

        @Override // android.content.IClipboard
        public boolean hasPrimaryClip(String callingPackage, int userId) throws RemoteException {
            return false;
        }

        @Override // android.content.IClipboard
        public void addPrimaryClipChangedListener(IOnPrimaryClipChangedListener listener, String callingPackage, int userId) throws RemoteException {
        }

        @Override // android.content.IClipboard
        public void removePrimaryClipChangedListener(IOnPrimaryClipChangedListener listener, String callingPackage, int userId) throws RemoteException {
        }

        @Override // android.content.IClipboard
        public boolean hasClipboardText(String callingPackage, int userId) throws RemoteException {
            return false;
        }

        @Override // android.content.IClipboard
        public String getPrimaryClipSource(String callingPackage, int userId) throws RemoteException {
            return null;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IClipboard {
        public static final String DESCRIPTOR = "android.content.IClipboard";
        static final int TRANSACTION_addPrimaryClipChangedListener = 7;
        static final int TRANSACTION_clearPrimaryClip = 3;
        static final int TRANSACTION_getPrimaryClip = 4;
        static final int TRANSACTION_getPrimaryClipDescription = 5;
        static final int TRANSACTION_getPrimaryClipSource = 10;
        static final int TRANSACTION_hasClipboardText = 9;
        static final int TRANSACTION_hasPrimaryClip = 6;
        static final int TRANSACTION_removePrimaryClipChangedListener = 8;
        static final int TRANSACTION_setPrimaryClip = 1;
        static final int TRANSACTION_setPrimaryClipAsPackage = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IClipboard asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IClipboard)) {
                return (IClipboard) iin;
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
                    return "setPrimaryClip";
                case 2:
                    return "setPrimaryClipAsPackage";
                case 3:
                    return "clearPrimaryClip";
                case 4:
                    return "getPrimaryClip";
                case 5:
                    return "getPrimaryClipDescription";
                case 6:
                    return "hasPrimaryClip";
                case 7:
                    return "addPrimaryClipChangedListener";
                case 8:
                    return "removePrimaryClipChangedListener";
                case 9:
                    return "hasClipboardText";
                case 10:
                    return "getPrimaryClipSource";
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
                data.enforceInterface(DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            ClipData _arg0 = (ClipData) data.readTypedObject(ClipData.CREATOR);
                            String _arg1 = data.readString();
                            int _arg2 = data.readInt();
                            data.enforceNoDataAvail();
                            setPrimaryClip(_arg0, _arg1, _arg2);
                            reply.writeNoException();
                            break;
                        case 2:
                            ClipData _arg02 = (ClipData) data.readTypedObject(ClipData.CREATOR);
                            String _arg12 = data.readString();
                            int _arg22 = data.readInt();
                            String _arg3 = data.readString();
                            data.enforceNoDataAvail();
                            setPrimaryClipAsPackage(_arg02, _arg12, _arg22, _arg3);
                            reply.writeNoException();
                            break;
                        case 3:
                            String _arg03 = data.readString();
                            int _arg13 = data.readInt();
                            data.enforceNoDataAvail();
                            clearPrimaryClip(_arg03, _arg13);
                            reply.writeNoException();
                            break;
                        case 4:
                            String _arg04 = data.readString();
                            int _arg14 = data.readInt();
                            data.enforceNoDataAvail();
                            ClipData _result = getPrimaryClip(_arg04, _arg14);
                            reply.writeNoException();
                            reply.writeTypedObject(_result, 1);
                            break;
                        case 5:
                            String _arg05 = data.readString();
                            int _arg15 = data.readInt();
                            data.enforceNoDataAvail();
                            ClipDescription _result2 = getPrimaryClipDescription(_arg05, _arg15);
                            reply.writeNoException();
                            reply.writeTypedObject(_result2, 1);
                            break;
                        case 6:
                            String _arg06 = data.readString();
                            int _arg16 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result3 = hasPrimaryClip(_arg06, _arg16);
                            reply.writeNoException();
                            reply.writeBoolean(_result3);
                            break;
                        case 7:
                            IOnPrimaryClipChangedListener _arg07 = IOnPrimaryClipChangedListener.Stub.asInterface(data.readStrongBinder());
                            String _arg17 = data.readString();
                            int _arg23 = data.readInt();
                            data.enforceNoDataAvail();
                            addPrimaryClipChangedListener(_arg07, _arg17, _arg23);
                            reply.writeNoException();
                            break;
                        case 8:
                            IOnPrimaryClipChangedListener _arg08 = IOnPrimaryClipChangedListener.Stub.asInterface(data.readStrongBinder());
                            String _arg18 = data.readString();
                            int _arg24 = data.readInt();
                            data.enforceNoDataAvail();
                            removePrimaryClipChangedListener(_arg08, _arg18, _arg24);
                            reply.writeNoException();
                            break;
                        case 9:
                            String _arg09 = data.readString();
                            int _arg19 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result4 = hasClipboardText(_arg09, _arg19);
                            reply.writeNoException();
                            reply.writeBoolean(_result4);
                            break;
                        case 10:
                            String _arg010 = data.readString();
                            int _arg110 = data.readInt();
                            data.enforceNoDataAvail();
                            String _result5 = getPrimaryClipSource(_arg010, _arg110);
                            reply.writeNoException();
                            reply.writeString(_result5);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IClipboard {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override // android.content.IClipboard
            public void setPrimaryClip(ClipData clip, String callingPackage, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(clip, 0);
                    _data.writeString(callingPackage);
                    _data.writeInt(userId);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.IClipboard
            public void setPrimaryClipAsPackage(ClipData clip, String callingPackage, int userId, String sourcePackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(clip, 0);
                    _data.writeString(callingPackage);
                    _data.writeInt(userId);
                    _data.writeString(sourcePackage);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.IClipboard
            public void clearPrimaryClip(String callingPackage, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(userId);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.IClipboard
            public ClipData getPrimaryClip(String pkg, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeInt(userId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    ClipData _result = (ClipData) _reply.readTypedObject(ClipData.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.IClipboard
            public ClipDescription getPrimaryClipDescription(String callingPackage, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(userId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    ClipDescription _result = (ClipDescription) _reply.readTypedObject(ClipDescription.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.IClipboard
            public boolean hasPrimaryClip(String callingPackage, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(userId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.IClipboard
            public void addPrimaryClipChangedListener(IOnPrimaryClipChangedListener listener, String callingPackage, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    _data.writeString(callingPackage);
                    _data.writeInt(userId);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.IClipboard
            public void removePrimaryClipChangedListener(IOnPrimaryClipChangedListener listener, String callingPackage, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    _data.writeString(callingPackage);
                    _data.writeInt(userId);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.IClipboard
            public boolean hasClipboardText(String callingPackage, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(userId);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.IClipboard
            public String getPrimaryClipSource(String callingPackage, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(userId);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 9;
        }
    }
}
