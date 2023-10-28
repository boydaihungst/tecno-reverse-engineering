package android.app;

import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.SurfaceControl;
import android.window.WindowContainerToken;
/* loaded from: classes.dex */
public interface ITaskAnimation extends IInterface {
    public static final String DESCRIPTOR = "android.app.ITaskAnimation";

    void finishAnimation(boolean z, Rect rect) throws RemoteException;

    void finishAnimationForSplitTask(boolean z, Rect rect, WindowContainerToken windowContainerToken, int i) throws RemoteException;

    void initAnimation(int i, Bundle bundle) throws RemoteException;

    void updateTransform(Bundle bundle) throws RemoteException;

    void updateTransformWithTransaction(SurfaceControl.Transaction transaction, Bundle bundle) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements ITaskAnimation {
        @Override // android.app.ITaskAnimation
        public void initAnimation(int type, Bundle params) throws RemoteException {
        }

        @Override // android.app.ITaskAnimation
        public void updateTransform(Bundle transformParams) throws RemoteException {
        }

        @Override // android.app.ITaskAnimation
        public void updateTransformWithTransaction(SurfaceControl.Transaction t, Bundle transformParams) throws RemoteException {
        }

        @Override // android.app.ITaskAnimation
        public void finishAnimation(boolean toMultiWindow, Rect multiWindowRegion) throws RemoteException {
        }

        @Override // android.app.ITaskAnimation
        public void finishAnimationForSplitTask(boolean toMultiWindow, Rect multiWindowRegion, WindowContainerToken toMultiTask, int splitToTopTaskId) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements ITaskAnimation {
        static final int TRANSACTION_finishAnimation = 4;
        static final int TRANSACTION_finishAnimationForSplitTask = 5;
        static final int TRANSACTION_initAnimation = 1;
        static final int TRANSACTION_updateTransform = 2;
        static final int TRANSACTION_updateTransformWithTransaction = 3;

        public Stub() {
            attachInterface(this, ITaskAnimation.DESCRIPTOR);
        }

        public static ITaskAnimation asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ITaskAnimation.DESCRIPTOR);
            if (iin != null && (iin instanceof ITaskAnimation)) {
                return (ITaskAnimation) iin;
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
                    return "initAnimation";
                case 2:
                    return "updateTransform";
                case 3:
                    return "updateTransformWithTransaction";
                case 4:
                    return "finishAnimation";
                case 5:
                    return "finishAnimationForSplitTask";
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
                data.enforceInterface(ITaskAnimation.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ITaskAnimation.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            Bundle _arg1 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            initAnimation(_arg0, _arg1);
                            reply.writeNoException();
                            break;
                        case 2:
                            Bundle _arg02 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            updateTransform(_arg02);
                            reply.writeNoException();
                            break;
                        case 3:
                            SurfaceControl.Transaction _arg03 = (SurfaceControl.Transaction) data.readTypedObject(SurfaceControl.Transaction.CREATOR);
                            Bundle _arg12 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            updateTransformWithTransaction(_arg03, _arg12);
                            reply.writeNoException();
                            break;
                        case 4:
                            boolean _arg04 = data.readBoolean();
                            Rect _arg13 = (Rect) data.readTypedObject(Rect.CREATOR);
                            data.enforceNoDataAvail();
                            finishAnimation(_arg04, _arg13);
                            reply.writeNoException();
                            break;
                        case 5:
                            boolean _arg05 = data.readBoolean();
                            Rect _arg14 = (Rect) data.readTypedObject(Rect.CREATOR);
                            WindowContainerToken _arg2 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            int _arg3 = data.readInt();
                            data.enforceNoDataAvail();
                            finishAnimationForSplitTask(_arg05, _arg14, _arg2, _arg3);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements ITaskAnimation {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ITaskAnimation.DESCRIPTOR;
            }

            @Override // android.app.ITaskAnimation
            public void initAnimation(int type, Bundle params) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskAnimation.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeTypedObject(params, 0);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ITaskAnimation
            public void updateTransform(Bundle transformParams) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskAnimation.DESCRIPTOR);
                    _data.writeTypedObject(transformParams, 0);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ITaskAnimation
            public void updateTransformWithTransaction(SurfaceControl.Transaction t, Bundle transformParams) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskAnimation.DESCRIPTOR);
                    _data.writeTypedObject(t, 0);
                    _data.writeTypedObject(transformParams, 0);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ITaskAnimation
            public void finishAnimation(boolean toMultiWindow, Rect multiWindowRegion) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskAnimation.DESCRIPTOR);
                    _data.writeBoolean(toMultiWindow);
                    _data.writeTypedObject(multiWindowRegion, 0);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ITaskAnimation
            public void finishAnimationForSplitTask(boolean toMultiWindow, Rect multiWindowRegion, WindowContainerToken toMultiTask, int splitToTopTaskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskAnimation.DESCRIPTOR);
                    _data.writeBoolean(toMultiWindow);
                    _data.writeTypedObject(multiWindowRegion, 0);
                    _data.writeTypedObject(toMultiTask, 0);
                    _data.writeInt(splitToTopTaskId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 4;
        }
    }
}
