package com.transsion.connectx.mirror.source;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.WindowManager;
/* loaded from: classes2.dex */
public interface ICastStatusInterface extends IInterface {
    public static final String DESCRIPTOR = "com.transsion.connectx.mirror.source.ICastStatusInterface";

    void hookDisplayChildCount(int i, int i2) throws RemoteException;

    void hookDisplayResumedActivityChanged(int i, String str) throws RemoteException;

    void hookDisplayRotation(int i, int i2) throws RemoteException;

    void hookInputMethodShown(boolean z) throws RemoteException;

    void hookSecureWindowVisible(int i, WindowManager.LayoutParams layoutParams) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements ICastStatusInterface {
        @Override // com.transsion.connectx.mirror.source.ICastStatusInterface
        public void hookDisplayRotation(int displayId, int rotation) throws RemoteException {
        }

        @Override // com.transsion.connectx.mirror.source.ICastStatusInterface
        public void hookDisplayResumedActivityChanged(int displayId, String packageName) throws RemoteException {
        }

        @Override // com.transsion.connectx.mirror.source.ICastStatusInterface
        public void hookSecureWindowVisible(int displayId, WindowManager.LayoutParams attrs) throws RemoteException {
        }

        @Override // com.transsion.connectx.mirror.source.ICastStatusInterface
        public void hookInputMethodShown(boolean inputShown) throws RemoteException {
        }

        @Override // com.transsion.connectx.mirror.source.ICastStatusInterface
        public void hookDisplayChildCount(int displayId, int count) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements ICastStatusInterface {
        static final int TRANSACTION_hookDisplayChildCount = 5;
        static final int TRANSACTION_hookDisplayResumedActivityChanged = 2;
        static final int TRANSACTION_hookDisplayRotation = 1;
        static final int TRANSACTION_hookInputMethodShown = 4;
        static final int TRANSACTION_hookSecureWindowVisible = 3;

        public Stub() {
            attachInterface(this, ICastStatusInterface.DESCRIPTOR);
        }

        public static ICastStatusInterface asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ICastStatusInterface.DESCRIPTOR);
            if (iin != null && (iin instanceof ICastStatusInterface)) {
                return (ICastStatusInterface) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(ICastStatusInterface.DESCRIPTOR);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(ICastStatusInterface.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            int _arg1 = data.readInt();
                            data.enforceNoDataAvail();
                            hookDisplayRotation(_arg0, _arg1);
                            break;
                        case 2:
                            int _arg02 = data.readInt();
                            String _arg12 = data.readString();
                            data.enforceNoDataAvail();
                            hookDisplayResumedActivityChanged(_arg02, _arg12);
                            break;
                        case 3:
                            int _arg03 = data.readInt();
                            WindowManager.LayoutParams _arg13 = (WindowManager.LayoutParams) data.readTypedObject(WindowManager.LayoutParams.CREATOR);
                            data.enforceNoDataAvail();
                            hookSecureWindowVisible(_arg03, _arg13);
                            break;
                        case 4:
                            boolean _arg04 = data.readBoolean();
                            data.enforceNoDataAvail();
                            hookInputMethodShown(_arg04);
                            break;
                        case 5:
                            int _arg05 = data.readInt();
                            int _arg14 = data.readInt();
                            data.enforceNoDataAvail();
                            hookDisplayChildCount(_arg05, _arg14);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements ICastStatusInterface {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ICastStatusInterface.DESCRIPTOR;
            }

            @Override // com.transsion.connectx.mirror.source.ICastStatusInterface
            public void hookDisplayRotation(int displayId, int rotation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ICastStatusInterface.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeInt(rotation);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.connectx.mirror.source.ICastStatusInterface
            public void hookDisplayResumedActivityChanged(int displayId, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ICastStatusInterface.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeString(packageName);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.connectx.mirror.source.ICastStatusInterface
            public void hookSecureWindowVisible(int displayId, WindowManager.LayoutParams attrs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ICastStatusInterface.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeTypedObject(attrs, 0);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.connectx.mirror.source.ICastStatusInterface
            public void hookInputMethodShown(boolean inputShown) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ICastStatusInterface.DESCRIPTOR);
                    _data.writeBoolean(inputShown);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.connectx.mirror.source.ICastStatusInterface
            public void hookDisplayChildCount(int displayId, int count) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ICastStatusInterface.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeInt(count);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
