package android.app;

import android.app.IOnProjectionStateChangedListener;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;
/* loaded from: classes.dex */
public interface IUiModeManager extends IInterface {
    void addOnProjectionStateChangedListener(IOnProjectionStateChangedListener iOnProjectionStateChangedListener, int i) throws RemoteException;

    void disableCarMode(int i) throws RemoteException;

    void disableCarModeByCallingPackage(int i, String str) throws RemoteException;

    void enableCarMode(int i, int i2, String str) throws RemoteException;

    int getActiveProjectionTypes() throws RemoteException;

    int getCurrentModeType() throws RemoteException;

    long getCustomNightModeEnd() throws RemoteException;

    long getCustomNightModeStart() throws RemoteException;

    int getNightMode() throws RemoteException;

    int getNightModeCustomType() throws RemoteException;

    List<String> getProjectingPackages(int i) throws RemoteException;

    boolean isNightModeLocked() throws RemoteException;

    boolean isUiModeLocked() throws RemoteException;

    boolean releaseProjection(int i, String str) throws RemoteException;

    void removeOnProjectionStateChangedListener(IOnProjectionStateChangedListener iOnProjectionStateChangedListener) throws RemoteException;

    boolean requestProjection(IBinder iBinder, int i, String str) throws RemoteException;

    void setApplicationNightMode(int i) throws RemoteException;

    void setCustomNightModeEnd(long j) throws RemoteException;

    void setCustomNightModeStart(long j) throws RemoteException;

    void setNightMode(int i) throws RemoteException;

    boolean setNightModeActivated(boolean z) throws RemoteException;

    boolean setNightModeActivatedForCustomMode(int i, boolean z) throws RemoteException;

    void setNightModeCustomType(int i) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IUiModeManager {
        @Override // android.app.IUiModeManager
        public void enableCarMode(int flags, int priority, String callingPackage) throws RemoteException {
        }

        @Override // android.app.IUiModeManager
        public void disableCarMode(int flags) throws RemoteException {
        }

        @Override // android.app.IUiModeManager
        public void disableCarModeByCallingPackage(int flags, String callingPackage) throws RemoteException {
        }

        @Override // android.app.IUiModeManager
        public int getCurrentModeType() throws RemoteException {
            return 0;
        }

        @Override // android.app.IUiModeManager
        public void setNightMode(int mode) throws RemoteException {
        }

        @Override // android.app.IUiModeManager
        public int getNightMode() throws RemoteException {
            return 0;
        }

        @Override // android.app.IUiModeManager
        public void setNightModeCustomType(int nightModeCustomType) throws RemoteException {
        }

        @Override // android.app.IUiModeManager
        public int getNightModeCustomType() throws RemoteException {
            return 0;
        }

        @Override // android.app.IUiModeManager
        public void setApplicationNightMode(int mode) throws RemoteException {
        }

        @Override // android.app.IUiModeManager
        public boolean isUiModeLocked() throws RemoteException {
            return false;
        }

        @Override // android.app.IUiModeManager
        public boolean isNightModeLocked() throws RemoteException {
            return false;
        }

        @Override // android.app.IUiModeManager
        public boolean setNightModeActivatedForCustomMode(int nightModeCustom, boolean active) throws RemoteException {
            return false;
        }

        @Override // android.app.IUiModeManager
        public boolean setNightModeActivated(boolean active) throws RemoteException {
            return false;
        }

        @Override // android.app.IUiModeManager
        public long getCustomNightModeStart() throws RemoteException {
            return 0L;
        }

        @Override // android.app.IUiModeManager
        public void setCustomNightModeStart(long time) throws RemoteException {
        }

        @Override // android.app.IUiModeManager
        public long getCustomNightModeEnd() throws RemoteException {
            return 0L;
        }

        @Override // android.app.IUiModeManager
        public void setCustomNightModeEnd(long time) throws RemoteException {
        }

        @Override // android.app.IUiModeManager
        public boolean requestProjection(IBinder binder, int projectionType, String callingPackage) throws RemoteException {
            return false;
        }

        @Override // android.app.IUiModeManager
        public boolean releaseProjection(int projectionType, String callingPackage) throws RemoteException {
            return false;
        }

        @Override // android.app.IUiModeManager
        public void addOnProjectionStateChangedListener(IOnProjectionStateChangedListener listener, int projectionType) throws RemoteException {
        }

        @Override // android.app.IUiModeManager
        public void removeOnProjectionStateChangedListener(IOnProjectionStateChangedListener listener) throws RemoteException {
        }

        @Override // android.app.IUiModeManager
        public List<String> getProjectingPackages(int projectionType) throws RemoteException {
            return null;
        }

        @Override // android.app.IUiModeManager
        public int getActiveProjectionTypes() throws RemoteException {
            return 0;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IUiModeManager {
        public static final String DESCRIPTOR = "android.app.IUiModeManager";
        static final int TRANSACTION_addOnProjectionStateChangedListener = 20;
        static final int TRANSACTION_disableCarMode = 2;
        static final int TRANSACTION_disableCarModeByCallingPackage = 3;
        static final int TRANSACTION_enableCarMode = 1;
        static final int TRANSACTION_getActiveProjectionTypes = 23;
        static final int TRANSACTION_getCurrentModeType = 4;
        static final int TRANSACTION_getCustomNightModeEnd = 16;
        static final int TRANSACTION_getCustomNightModeStart = 14;
        static final int TRANSACTION_getNightMode = 6;
        static final int TRANSACTION_getNightModeCustomType = 8;
        static final int TRANSACTION_getProjectingPackages = 22;
        static final int TRANSACTION_isNightModeLocked = 11;
        static final int TRANSACTION_isUiModeLocked = 10;
        static final int TRANSACTION_releaseProjection = 19;
        static final int TRANSACTION_removeOnProjectionStateChangedListener = 21;
        static final int TRANSACTION_requestProjection = 18;
        static final int TRANSACTION_setApplicationNightMode = 9;
        static final int TRANSACTION_setCustomNightModeEnd = 17;
        static final int TRANSACTION_setCustomNightModeStart = 15;
        static final int TRANSACTION_setNightMode = 5;
        static final int TRANSACTION_setNightModeActivated = 13;
        static final int TRANSACTION_setNightModeActivatedForCustomMode = 12;
        static final int TRANSACTION_setNightModeCustomType = 7;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IUiModeManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IUiModeManager)) {
                return (IUiModeManager) iin;
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
                    return "enableCarMode";
                case 2:
                    return "disableCarMode";
                case 3:
                    return "disableCarModeByCallingPackage";
                case 4:
                    return "getCurrentModeType";
                case 5:
                    return "setNightMode";
                case 6:
                    return "getNightMode";
                case 7:
                    return "setNightModeCustomType";
                case 8:
                    return "getNightModeCustomType";
                case 9:
                    return "setApplicationNightMode";
                case 10:
                    return "isUiModeLocked";
                case 11:
                    return "isNightModeLocked";
                case 12:
                    return "setNightModeActivatedForCustomMode";
                case 13:
                    return "setNightModeActivated";
                case 14:
                    return "getCustomNightModeStart";
                case 15:
                    return "setCustomNightModeStart";
                case 16:
                    return "getCustomNightModeEnd";
                case 17:
                    return "setCustomNightModeEnd";
                case 18:
                    return "requestProjection";
                case 19:
                    return "releaseProjection";
                case 20:
                    return "addOnProjectionStateChangedListener";
                case 21:
                    return "removeOnProjectionStateChangedListener";
                case 22:
                    return "getProjectingPackages";
                case 23:
                    return "getActiveProjectionTypes";
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
                            int _arg0 = data.readInt();
                            int _arg1 = data.readInt();
                            String _arg2 = data.readString();
                            data.enforceNoDataAvail();
                            enableCarMode(_arg0, _arg1, _arg2);
                            reply.writeNoException();
                            break;
                        case 2:
                            int _arg02 = data.readInt();
                            data.enforceNoDataAvail();
                            disableCarMode(_arg02);
                            reply.writeNoException();
                            break;
                        case 3:
                            int _arg03 = data.readInt();
                            String _arg12 = data.readString();
                            data.enforceNoDataAvail();
                            disableCarModeByCallingPackage(_arg03, _arg12);
                            reply.writeNoException();
                            break;
                        case 4:
                            int _result = getCurrentModeType();
                            reply.writeNoException();
                            reply.writeInt(_result);
                            break;
                        case 5:
                            int _arg04 = data.readInt();
                            data.enforceNoDataAvail();
                            setNightMode(_arg04);
                            reply.writeNoException();
                            break;
                        case 6:
                            int _result2 = getNightMode();
                            reply.writeNoException();
                            reply.writeInt(_result2);
                            break;
                        case 7:
                            int _arg05 = data.readInt();
                            data.enforceNoDataAvail();
                            setNightModeCustomType(_arg05);
                            reply.writeNoException();
                            break;
                        case 8:
                            int _result3 = getNightModeCustomType();
                            reply.writeNoException();
                            reply.writeInt(_result3);
                            break;
                        case 9:
                            int _arg06 = data.readInt();
                            data.enforceNoDataAvail();
                            setApplicationNightMode(_arg06);
                            reply.writeNoException();
                            break;
                        case 10:
                            boolean _result4 = isUiModeLocked();
                            reply.writeNoException();
                            reply.writeBoolean(_result4);
                            break;
                        case 11:
                            boolean _result5 = isNightModeLocked();
                            reply.writeNoException();
                            reply.writeBoolean(_result5);
                            break;
                        case 12:
                            int _arg07 = data.readInt();
                            boolean _arg13 = data.readBoolean();
                            data.enforceNoDataAvail();
                            boolean _result6 = setNightModeActivatedForCustomMode(_arg07, _arg13);
                            reply.writeNoException();
                            reply.writeBoolean(_result6);
                            break;
                        case 13:
                            boolean _arg08 = data.readBoolean();
                            data.enforceNoDataAvail();
                            boolean _result7 = setNightModeActivated(_arg08);
                            reply.writeNoException();
                            reply.writeBoolean(_result7);
                            break;
                        case 14:
                            long _result8 = getCustomNightModeStart();
                            reply.writeNoException();
                            reply.writeLong(_result8);
                            break;
                        case 15:
                            long _arg09 = data.readLong();
                            data.enforceNoDataAvail();
                            setCustomNightModeStart(_arg09);
                            reply.writeNoException();
                            break;
                        case 16:
                            long _result9 = getCustomNightModeEnd();
                            reply.writeNoException();
                            reply.writeLong(_result9);
                            break;
                        case 17:
                            long _arg010 = data.readLong();
                            data.enforceNoDataAvail();
                            setCustomNightModeEnd(_arg010);
                            reply.writeNoException();
                            break;
                        case 18:
                            IBinder _arg011 = data.readStrongBinder();
                            int _arg14 = data.readInt();
                            String _arg22 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result10 = requestProjection(_arg011, _arg14, _arg22);
                            reply.writeNoException();
                            reply.writeBoolean(_result10);
                            break;
                        case 19:
                            int _arg012 = data.readInt();
                            String _arg15 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result11 = releaseProjection(_arg012, _arg15);
                            reply.writeNoException();
                            reply.writeBoolean(_result11);
                            break;
                        case 20:
                            IOnProjectionStateChangedListener _arg013 = IOnProjectionStateChangedListener.Stub.asInterface(data.readStrongBinder());
                            int _arg16 = data.readInt();
                            data.enforceNoDataAvail();
                            addOnProjectionStateChangedListener(_arg013, _arg16);
                            reply.writeNoException();
                            break;
                        case 21:
                            IOnProjectionStateChangedListener _arg014 = IOnProjectionStateChangedListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            removeOnProjectionStateChangedListener(_arg014);
                            reply.writeNoException();
                            break;
                        case 22:
                            int _arg015 = data.readInt();
                            data.enforceNoDataAvail();
                            List<String> _result12 = getProjectingPackages(_arg015);
                            reply.writeNoException();
                            reply.writeStringList(_result12);
                            break;
                        case 23:
                            int _result13 = getActiveProjectionTypes();
                            reply.writeNoException();
                            reply.writeInt(_result13);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IUiModeManager {
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

            @Override // android.app.IUiModeManager
            public void enableCarMode(int flags, int priority, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(flags);
                    _data.writeInt(priority);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IUiModeManager
            public void disableCarMode(int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(flags);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IUiModeManager
            public void disableCarModeByCallingPackage(int flags, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(flags);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IUiModeManager
            public int getCurrentModeType() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IUiModeManager
            public void setNightMode(int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(mode);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IUiModeManager
            public int getNightMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IUiModeManager
            public void setNightModeCustomType(int nightModeCustomType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(nightModeCustomType);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IUiModeManager
            public int getNightModeCustomType() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IUiModeManager
            public void setApplicationNightMode(int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(mode);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IUiModeManager
            public boolean isUiModeLocked() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IUiModeManager
            public boolean isNightModeLocked() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IUiModeManager
            public boolean setNightModeActivatedForCustomMode(int nightModeCustom, boolean active) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(nightModeCustom);
                    _data.writeBoolean(active);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IUiModeManager
            public boolean setNightModeActivated(boolean active) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(active);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IUiModeManager
            public long getCustomNightModeStart() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IUiModeManager
            public void setCustomNightModeStart(long time) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(time);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IUiModeManager
            public long getCustomNightModeEnd() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IUiModeManager
            public void setCustomNightModeEnd(long time) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(time);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IUiModeManager
            public boolean requestProjection(IBinder binder, int projectionType, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(binder);
                    _data.writeInt(projectionType);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IUiModeManager
            public boolean releaseProjection(int projectionType, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(projectionType);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IUiModeManager
            public void addOnProjectionStateChangedListener(IOnProjectionStateChangedListener listener, int projectionType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    _data.writeInt(projectionType);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IUiModeManager
            public void removeOnProjectionStateChangedListener(IOnProjectionStateChangedListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IUiModeManager
            public List<String> getProjectingPackages(int projectionType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(projectionType);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IUiModeManager
            public int getActiveProjectionTypes() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 22;
        }
    }
}
