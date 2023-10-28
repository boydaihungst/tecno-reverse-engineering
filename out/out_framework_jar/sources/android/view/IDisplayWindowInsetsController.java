package android.view;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes3.dex */
public interface IDisplayWindowInsetsController extends IInterface {
    public static final String DESCRIPTOR = "android.view.IDisplayWindowInsetsController";

    void hideInsets(int i, boolean z) throws RemoteException;

    void insetsChanged(InsetsState insetsState) throws RemoteException;

    void insetsControlChanged(InsetsState insetsState, InsetsSourceControl[] insetsSourceControlArr) throws RemoteException;

    void showInsets(int i, boolean z) throws RemoteException;

    void topFocusedWindowChanged(String str, InsetsVisibilities insetsVisibilities) throws RemoteException;

    /* loaded from: classes3.dex */
    public static class Default implements IDisplayWindowInsetsController {
        @Override // android.view.IDisplayWindowInsetsController
        public void topFocusedWindowChanged(String packageName, InsetsVisibilities insetsVisibilities) throws RemoteException {
        }

        @Override // android.view.IDisplayWindowInsetsController
        public void insetsChanged(InsetsState insetsState) throws RemoteException {
        }

        @Override // android.view.IDisplayWindowInsetsController
        public void insetsControlChanged(InsetsState insetsState, InsetsSourceControl[] activeControls) throws RemoteException {
        }

        @Override // android.view.IDisplayWindowInsetsController
        public void showInsets(int types, boolean fromIme) throws RemoteException {
        }

        @Override // android.view.IDisplayWindowInsetsController
        public void hideInsets(int types, boolean fromIme) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes3.dex */
    public static abstract class Stub extends Binder implements IDisplayWindowInsetsController {
        static final int TRANSACTION_hideInsets = 5;
        static final int TRANSACTION_insetsChanged = 2;
        static final int TRANSACTION_insetsControlChanged = 3;
        static final int TRANSACTION_showInsets = 4;
        static final int TRANSACTION_topFocusedWindowChanged = 1;

        public Stub() {
            attachInterface(this, IDisplayWindowInsetsController.DESCRIPTOR);
        }

        public static IDisplayWindowInsetsController asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IDisplayWindowInsetsController.DESCRIPTOR);
            if (iin != null && (iin instanceof IDisplayWindowInsetsController)) {
                return (IDisplayWindowInsetsController) iin;
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
                    return "topFocusedWindowChanged";
                case 2:
                    return "insetsChanged";
                case 3:
                    return "insetsControlChanged";
                case 4:
                    return "showInsets";
                case 5:
                    return "hideInsets";
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
                data.enforceInterface(IDisplayWindowInsetsController.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IDisplayWindowInsetsController.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            String _arg0 = data.readString();
                            InsetsVisibilities _arg1 = (InsetsVisibilities) data.readTypedObject(InsetsVisibilities.CREATOR);
                            data.enforceNoDataAvail();
                            topFocusedWindowChanged(_arg0, _arg1);
                            break;
                        case 2:
                            InsetsState _arg02 = (InsetsState) data.readTypedObject(InsetsState.CREATOR);
                            data.enforceNoDataAvail();
                            insetsChanged(_arg02);
                            break;
                        case 3:
                            InsetsState _arg03 = (InsetsState) data.readTypedObject(InsetsState.CREATOR);
                            InsetsSourceControl[] _arg12 = (InsetsSourceControl[]) data.createTypedArray(InsetsSourceControl.CREATOR);
                            data.enforceNoDataAvail();
                            insetsControlChanged(_arg03, _arg12);
                            break;
                        case 4:
                            int _arg04 = data.readInt();
                            boolean _arg13 = data.readBoolean();
                            data.enforceNoDataAvail();
                            showInsets(_arg04, _arg13);
                            break;
                        case 5:
                            int _arg05 = data.readInt();
                            boolean _arg14 = data.readBoolean();
                            data.enforceNoDataAvail();
                            hideInsets(_arg05, _arg14);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes3.dex */
        public static class Proxy implements IDisplayWindowInsetsController {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IDisplayWindowInsetsController.DESCRIPTOR;
            }

            @Override // android.view.IDisplayWindowInsetsController
            public void topFocusedWindowChanged(String packageName, InsetsVisibilities insetsVisibilities) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDisplayWindowInsetsController.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeTypedObject(insetsVisibilities, 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IDisplayWindowInsetsController
            public void insetsChanged(InsetsState insetsState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDisplayWindowInsetsController.DESCRIPTOR);
                    _data.writeTypedObject(insetsState, 0);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IDisplayWindowInsetsController
            public void insetsControlChanged(InsetsState insetsState, InsetsSourceControl[] activeControls) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDisplayWindowInsetsController.DESCRIPTOR);
                    _data.writeTypedObject(insetsState, 0);
                    _data.writeTypedArray(activeControls, 0);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IDisplayWindowInsetsController
            public void showInsets(int types, boolean fromIme) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDisplayWindowInsetsController.DESCRIPTOR);
                    _data.writeInt(types);
                    _data.writeBoolean(fromIme);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IDisplayWindowInsetsController
            public void hideInsets(int types, boolean fromIme) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDisplayWindowInsetsController.DESCRIPTOR);
                    _data.writeInt(types);
                    _data.writeBoolean(fromIme);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
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
