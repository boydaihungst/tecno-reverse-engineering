package android.companion.virtual;

import android.app.PendingIntent;
import android.companion.virtual.audio.IAudioConfigChangedCallback;
import android.companion.virtual.audio.IAudioRoutingCallback;
import android.graphics.Point;
import android.graphics.PointF;
import android.hardware.input.VirtualKeyEvent;
import android.hardware.input.VirtualMouseButtonEvent;
import android.hardware.input.VirtualMouseRelativeEvent;
import android.hardware.input.VirtualMouseScrollEvent;
import android.hardware.input.VirtualTouchEvent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
/* loaded from: classes.dex */
public interface IVirtualDevice extends IInterface {
    public static final String DESCRIPTOR = "android.companion.virtual.IVirtualDevice";

    void close() throws RemoteException;

    void createVirtualKeyboard(int i, String str, int i2, int i3, IBinder iBinder) throws RemoteException;

    void createVirtualMouse(int i, String str, int i2, int i3, IBinder iBinder) throws RemoteException;

    void createVirtualTouchscreen(int i, String str, int i2, int i3, IBinder iBinder, Point point) throws RemoteException;

    int getAssociationId() throws RemoteException;

    PointF getCursorPosition(IBinder iBinder) throws RemoteException;

    void launchPendingIntent(int i, PendingIntent pendingIntent, ResultReceiver resultReceiver) throws RemoteException;

    void onAudioSessionEnded() throws RemoteException;

    void onAudioSessionStarting(int i, IAudioRoutingCallback iAudioRoutingCallback, IAudioConfigChangedCallback iAudioConfigChangedCallback) throws RemoteException;

    boolean sendButtonEvent(IBinder iBinder, VirtualMouseButtonEvent virtualMouseButtonEvent) throws RemoteException;

    boolean sendKeyEvent(IBinder iBinder, VirtualKeyEvent virtualKeyEvent) throws RemoteException;

    boolean sendRelativeEvent(IBinder iBinder, VirtualMouseRelativeEvent virtualMouseRelativeEvent) throws RemoteException;

    boolean sendScrollEvent(IBinder iBinder, VirtualMouseScrollEvent virtualMouseScrollEvent) throws RemoteException;

    boolean sendTouchEvent(IBinder iBinder, VirtualTouchEvent virtualTouchEvent) throws RemoteException;

    void setShowPointerIcon(boolean z) throws RemoteException;

    void unregisterInputDevice(IBinder iBinder) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IVirtualDevice {
        @Override // android.companion.virtual.IVirtualDevice
        public int getAssociationId() throws RemoteException {
            return 0;
        }

        @Override // android.companion.virtual.IVirtualDevice
        public void close() throws RemoteException {
        }

        @Override // android.companion.virtual.IVirtualDevice
        public void onAudioSessionStarting(int displayId, IAudioRoutingCallback routingCallback, IAudioConfigChangedCallback configChangedCallback) throws RemoteException {
        }

        @Override // android.companion.virtual.IVirtualDevice
        public void onAudioSessionEnded() throws RemoteException {
        }

        @Override // android.companion.virtual.IVirtualDevice
        public void createVirtualKeyboard(int displayId, String inputDeviceName, int vendorId, int productId, IBinder token) throws RemoteException {
        }

        @Override // android.companion.virtual.IVirtualDevice
        public void createVirtualMouse(int displayId, String inputDeviceName, int vendorId, int productId, IBinder token) throws RemoteException {
        }

        @Override // android.companion.virtual.IVirtualDevice
        public void createVirtualTouchscreen(int displayId, String inputDeviceName, int vendorId, int productId, IBinder token, Point screenSize) throws RemoteException {
        }

        @Override // android.companion.virtual.IVirtualDevice
        public void unregisterInputDevice(IBinder token) throws RemoteException {
        }

        @Override // android.companion.virtual.IVirtualDevice
        public boolean sendKeyEvent(IBinder token, VirtualKeyEvent event) throws RemoteException {
            return false;
        }

        @Override // android.companion.virtual.IVirtualDevice
        public boolean sendButtonEvent(IBinder token, VirtualMouseButtonEvent event) throws RemoteException {
            return false;
        }

        @Override // android.companion.virtual.IVirtualDevice
        public boolean sendRelativeEvent(IBinder token, VirtualMouseRelativeEvent event) throws RemoteException {
            return false;
        }

        @Override // android.companion.virtual.IVirtualDevice
        public boolean sendScrollEvent(IBinder token, VirtualMouseScrollEvent event) throws RemoteException {
            return false;
        }

        @Override // android.companion.virtual.IVirtualDevice
        public boolean sendTouchEvent(IBinder token, VirtualTouchEvent event) throws RemoteException {
            return false;
        }

        @Override // android.companion.virtual.IVirtualDevice
        public void launchPendingIntent(int displayId, PendingIntent pendingIntent, ResultReceiver resultReceiver) throws RemoteException {
        }

        @Override // android.companion.virtual.IVirtualDevice
        public PointF getCursorPosition(IBinder token) throws RemoteException {
            return null;
        }

        @Override // android.companion.virtual.IVirtualDevice
        public void setShowPointerIcon(boolean showPointerIcon) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IVirtualDevice {
        static final int TRANSACTION_close = 2;
        static final int TRANSACTION_createVirtualKeyboard = 5;
        static final int TRANSACTION_createVirtualMouse = 6;
        static final int TRANSACTION_createVirtualTouchscreen = 7;
        static final int TRANSACTION_getAssociationId = 1;
        static final int TRANSACTION_getCursorPosition = 15;
        static final int TRANSACTION_launchPendingIntent = 14;
        static final int TRANSACTION_onAudioSessionEnded = 4;
        static final int TRANSACTION_onAudioSessionStarting = 3;
        static final int TRANSACTION_sendButtonEvent = 10;
        static final int TRANSACTION_sendKeyEvent = 9;
        static final int TRANSACTION_sendRelativeEvent = 11;
        static final int TRANSACTION_sendScrollEvent = 12;
        static final int TRANSACTION_sendTouchEvent = 13;
        static final int TRANSACTION_setShowPointerIcon = 16;
        static final int TRANSACTION_unregisterInputDevice = 8;

        public Stub() {
            attachInterface(this, IVirtualDevice.DESCRIPTOR);
        }

        public static IVirtualDevice asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IVirtualDevice.DESCRIPTOR);
            if (iin != null && (iin instanceof IVirtualDevice)) {
                return (IVirtualDevice) iin;
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
                    return "getAssociationId";
                case 2:
                    return "close";
                case 3:
                    return "onAudioSessionStarting";
                case 4:
                    return "onAudioSessionEnded";
                case 5:
                    return "createVirtualKeyboard";
                case 6:
                    return "createVirtualMouse";
                case 7:
                    return "createVirtualTouchscreen";
                case 8:
                    return "unregisterInputDevice";
                case 9:
                    return "sendKeyEvent";
                case 10:
                    return "sendButtonEvent";
                case 11:
                    return "sendRelativeEvent";
                case 12:
                    return "sendScrollEvent";
                case 13:
                    return "sendTouchEvent";
                case 14:
                    return "launchPendingIntent";
                case 15:
                    return "getCursorPosition";
                case 16:
                    return "setShowPointerIcon";
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
                data.enforceInterface(IVirtualDevice.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IVirtualDevice.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _result = getAssociationId();
                            reply.writeNoException();
                            reply.writeInt(_result);
                            break;
                        case 2:
                            close();
                            reply.writeNoException();
                            break;
                        case 3:
                            int _arg0 = data.readInt();
                            IAudioRoutingCallback _arg1 = IAudioRoutingCallback.Stub.asInterface(data.readStrongBinder());
                            IAudioConfigChangedCallback _arg2 = IAudioConfigChangedCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            onAudioSessionStarting(_arg0, _arg1, _arg2);
                            reply.writeNoException();
                            break;
                        case 4:
                            onAudioSessionEnded();
                            reply.writeNoException();
                            break;
                        case 5:
                            int _arg02 = data.readInt();
                            String _arg12 = data.readString();
                            int _arg22 = data.readInt();
                            int _arg3 = data.readInt();
                            IBinder _arg4 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            createVirtualKeyboard(_arg02, _arg12, _arg22, _arg3, _arg4);
                            reply.writeNoException();
                            break;
                        case 6:
                            int _arg03 = data.readInt();
                            String _arg13 = data.readString();
                            int _arg23 = data.readInt();
                            int _arg32 = data.readInt();
                            IBinder _arg42 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            createVirtualMouse(_arg03, _arg13, _arg23, _arg32, _arg42);
                            reply.writeNoException();
                            break;
                        case 7:
                            int _arg04 = data.readInt();
                            String _arg14 = data.readString();
                            int _arg24 = data.readInt();
                            int _arg33 = data.readInt();
                            IBinder _arg43 = data.readStrongBinder();
                            Point _arg5 = (Point) data.readTypedObject(Point.CREATOR);
                            data.enforceNoDataAvail();
                            createVirtualTouchscreen(_arg04, _arg14, _arg24, _arg33, _arg43, _arg5);
                            reply.writeNoException();
                            break;
                        case 8:
                            IBinder _arg05 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            unregisterInputDevice(_arg05);
                            reply.writeNoException();
                            break;
                        case 9:
                            IBinder _arg06 = data.readStrongBinder();
                            VirtualKeyEvent _arg15 = (VirtualKeyEvent) data.readTypedObject(VirtualKeyEvent.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result2 = sendKeyEvent(_arg06, _arg15);
                            reply.writeNoException();
                            reply.writeBoolean(_result2);
                            break;
                        case 10:
                            IBinder _arg07 = data.readStrongBinder();
                            VirtualMouseButtonEvent _arg16 = (VirtualMouseButtonEvent) data.readTypedObject(VirtualMouseButtonEvent.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result3 = sendButtonEvent(_arg07, _arg16);
                            reply.writeNoException();
                            reply.writeBoolean(_result3);
                            break;
                        case 11:
                            IBinder _arg08 = data.readStrongBinder();
                            VirtualMouseRelativeEvent _arg17 = (VirtualMouseRelativeEvent) data.readTypedObject(VirtualMouseRelativeEvent.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result4 = sendRelativeEvent(_arg08, _arg17);
                            reply.writeNoException();
                            reply.writeBoolean(_result4);
                            break;
                        case 12:
                            IBinder _arg09 = data.readStrongBinder();
                            VirtualMouseScrollEvent _arg18 = (VirtualMouseScrollEvent) data.readTypedObject(VirtualMouseScrollEvent.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result5 = sendScrollEvent(_arg09, _arg18);
                            reply.writeNoException();
                            reply.writeBoolean(_result5);
                            break;
                        case 13:
                            IBinder _arg010 = data.readStrongBinder();
                            VirtualTouchEvent _arg19 = (VirtualTouchEvent) data.readTypedObject(VirtualTouchEvent.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result6 = sendTouchEvent(_arg010, _arg19);
                            reply.writeNoException();
                            reply.writeBoolean(_result6);
                            break;
                        case 14:
                            int _arg011 = data.readInt();
                            PendingIntent _arg110 = (PendingIntent) data.readTypedObject(PendingIntent.CREATOR);
                            ResultReceiver _arg25 = (ResultReceiver) data.readTypedObject(ResultReceiver.CREATOR);
                            data.enforceNoDataAvail();
                            launchPendingIntent(_arg011, _arg110, _arg25);
                            reply.writeNoException();
                            break;
                        case 15:
                            IBinder _arg012 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            PointF _result7 = getCursorPosition(_arg012);
                            reply.writeNoException();
                            reply.writeTypedObject(_result7, 1);
                            break;
                        case 16:
                            boolean _arg013 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setShowPointerIcon(_arg013);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IVirtualDevice {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IVirtualDevice.DESCRIPTOR;
            }

            @Override // android.companion.virtual.IVirtualDevice
            public int getAssociationId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IVirtualDevice.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.virtual.IVirtualDevice
            public void close() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IVirtualDevice.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.virtual.IVirtualDevice
            public void onAudioSessionStarting(int displayId, IAudioRoutingCallback routingCallback, IAudioConfigChangedCallback configChangedCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IVirtualDevice.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeStrongInterface(routingCallback);
                    _data.writeStrongInterface(configChangedCallback);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.virtual.IVirtualDevice
            public void onAudioSessionEnded() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IVirtualDevice.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.virtual.IVirtualDevice
            public void createVirtualKeyboard(int displayId, String inputDeviceName, int vendorId, int productId, IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IVirtualDevice.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeString(inputDeviceName);
                    _data.writeInt(vendorId);
                    _data.writeInt(productId);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.virtual.IVirtualDevice
            public void createVirtualMouse(int displayId, String inputDeviceName, int vendorId, int productId, IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IVirtualDevice.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeString(inputDeviceName);
                    _data.writeInt(vendorId);
                    _data.writeInt(productId);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.virtual.IVirtualDevice
            public void createVirtualTouchscreen(int displayId, String inputDeviceName, int vendorId, int productId, IBinder token, Point screenSize) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IVirtualDevice.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeString(inputDeviceName);
                    _data.writeInt(vendorId);
                    _data.writeInt(productId);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(screenSize, 0);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.virtual.IVirtualDevice
            public void unregisterInputDevice(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IVirtualDevice.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.virtual.IVirtualDevice
            public boolean sendKeyEvent(IBinder token, VirtualKeyEvent event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IVirtualDevice.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(event, 0);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.virtual.IVirtualDevice
            public boolean sendButtonEvent(IBinder token, VirtualMouseButtonEvent event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IVirtualDevice.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(event, 0);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.virtual.IVirtualDevice
            public boolean sendRelativeEvent(IBinder token, VirtualMouseRelativeEvent event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IVirtualDevice.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(event, 0);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.virtual.IVirtualDevice
            public boolean sendScrollEvent(IBinder token, VirtualMouseScrollEvent event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IVirtualDevice.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(event, 0);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.virtual.IVirtualDevice
            public boolean sendTouchEvent(IBinder token, VirtualTouchEvent event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IVirtualDevice.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(event, 0);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.virtual.IVirtualDevice
            public void launchPendingIntent(int displayId, PendingIntent pendingIntent, ResultReceiver resultReceiver) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IVirtualDevice.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeTypedObject(pendingIntent, 0);
                    _data.writeTypedObject(resultReceiver, 0);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.virtual.IVirtualDevice
            public PointF getCursorPosition(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IVirtualDevice.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    PointF _result = (PointF) _reply.readTypedObject(PointF.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.virtual.IVirtualDevice
            public void setShowPointerIcon(boolean showPointerIcon) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IVirtualDevice.DESCRIPTOR);
                    _data.writeBoolean(showPointerIcon);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 15;
        }
    }
}
