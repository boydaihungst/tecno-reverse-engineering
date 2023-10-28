package android.window;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes4.dex */
public interface ITaskFragmentOrganizer extends IInterface {
    public static final String DESCRIPTOR = "android.window.ITaskFragmentOrganizer";

    void onActivityReparentToTask(int i, Intent intent, IBinder iBinder) throws RemoteException;

    void onTaskFragmentAppeared(TaskFragmentInfo taskFragmentInfo) throws RemoteException;

    void onTaskFragmentError(IBinder iBinder, Bundle bundle) throws RemoteException;

    void onTaskFragmentInfoChanged(TaskFragmentInfo taskFragmentInfo) throws RemoteException;

    void onTaskFragmentParentInfoChanged(IBinder iBinder, Configuration configuration) throws RemoteException;

    void onTaskFragmentVanished(TaskFragmentInfo taskFragmentInfo) throws RemoteException;

    /* loaded from: classes4.dex */
    public static class Default implements ITaskFragmentOrganizer {
        @Override // android.window.ITaskFragmentOrganizer
        public void onTaskFragmentAppeared(TaskFragmentInfo taskFragmentInfo) throws RemoteException {
        }

        @Override // android.window.ITaskFragmentOrganizer
        public void onTaskFragmentInfoChanged(TaskFragmentInfo taskFragmentInfo) throws RemoteException {
        }

        @Override // android.window.ITaskFragmentOrganizer
        public void onTaskFragmentVanished(TaskFragmentInfo taskFragmentInfo) throws RemoteException {
        }

        @Override // android.window.ITaskFragmentOrganizer
        public void onTaskFragmentParentInfoChanged(IBinder fragmentToken, Configuration parentConfig) throws RemoteException {
        }

        @Override // android.window.ITaskFragmentOrganizer
        public void onTaskFragmentError(IBinder errorCallbackToken, Bundle exceptionBundle) throws RemoteException {
        }

        @Override // android.window.ITaskFragmentOrganizer
        public void onActivityReparentToTask(int taskId, Intent activityIntent, IBinder activityToken) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class Stub extends Binder implements ITaskFragmentOrganizer {
        static final int TRANSACTION_onActivityReparentToTask = 6;
        static final int TRANSACTION_onTaskFragmentAppeared = 1;
        static final int TRANSACTION_onTaskFragmentError = 5;
        static final int TRANSACTION_onTaskFragmentInfoChanged = 2;
        static final int TRANSACTION_onTaskFragmentParentInfoChanged = 4;
        static final int TRANSACTION_onTaskFragmentVanished = 3;

        public Stub() {
            attachInterface(this, ITaskFragmentOrganizer.DESCRIPTOR);
        }

        public static ITaskFragmentOrganizer asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ITaskFragmentOrganizer.DESCRIPTOR);
            if (iin != null && (iin instanceof ITaskFragmentOrganizer)) {
                return (ITaskFragmentOrganizer) iin;
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
                    return "onTaskFragmentAppeared";
                case 2:
                    return "onTaskFragmentInfoChanged";
                case 3:
                    return "onTaskFragmentVanished";
                case 4:
                    return "onTaskFragmentParentInfoChanged";
                case 5:
                    return "onTaskFragmentError";
                case 6:
                    return "onActivityReparentToTask";
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
                data.enforceInterface(ITaskFragmentOrganizer.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ITaskFragmentOrganizer.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            TaskFragmentInfo _arg0 = (TaskFragmentInfo) data.readTypedObject(TaskFragmentInfo.CREATOR);
                            data.enforceNoDataAvail();
                            onTaskFragmentAppeared(_arg0);
                            break;
                        case 2:
                            TaskFragmentInfo _arg02 = (TaskFragmentInfo) data.readTypedObject(TaskFragmentInfo.CREATOR);
                            data.enforceNoDataAvail();
                            onTaskFragmentInfoChanged(_arg02);
                            break;
                        case 3:
                            TaskFragmentInfo _arg03 = (TaskFragmentInfo) data.readTypedObject(TaskFragmentInfo.CREATOR);
                            data.enforceNoDataAvail();
                            onTaskFragmentVanished(_arg03);
                            break;
                        case 4:
                            IBinder _arg04 = data.readStrongBinder();
                            Configuration _arg1 = (Configuration) data.readTypedObject(Configuration.CREATOR);
                            data.enforceNoDataAvail();
                            onTaskFragmentParentInfoChanged(_arg04, _arg1);
                            break;
                        case 5:
                            IBinder _arg05 = data.readStrongBinder();
                            Bundle _arg12 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            onTaskFragmentError(_arg05, _arg12);
                            break;
                        case 6:
                            int _arg06 = data.readInt();
                            Intent _arg13 = (Intent) data.readTypedObject(Intent.CREATOR);
                            IBinder _arg2 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            onActivityReparentToTask(_arg06, _arg13, _arg2);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes4.dex */
        public static class Proxy implements ITaskFragmentOrganizer {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ITaskFragmentOrganizer.DESCRIPTOR;
            }

            @Override // android.window.ITaskFragmentOrganizer
            public void onTaskFragmentAppeared(TaskFragmentInfo taskFragmentInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskFragmentOrganizer.DESCRIPTOR);
                    _data.writeTypedObject(taskFragmentInfo, 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.window.ITaskFragmentOrganizer
            public void onTaskFragmentInfoChanged(TaskFragmentInfo taskFragmentInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskFragmentOrganizer.DESCRIPTOR);
                    _data.writeTypedObject(taskFragmentInfo, 0);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.window.ITaskFragmentOrganizer
            public void onTaskFragmentVanished(TaskFragmentInfo taskFragmentInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskFragmentOrganizer.DESCRIPTOR);
                    _data.writeTypedObject(taskFragmentInfo, 0);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.window.ITaskFragmentOrganizer
            public void onTaskFragmentParentInfoChanged(IBinder fragmentToken, Configuration parentConfig) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskFragmentOrganizer.DESCRIPTOR);
                    _data.writeStrongBinder(fragmentToken);
                    _data.writeTypedObject(parentConfig, 0);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.window.ITaskFragmentOrganizer
            public void onTaskFragmentError(IBinder errorCallbackToken, Bundle exceptionBundle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskFragmentOrganizer.DESCRIPTOR);
                    _data.writeStrongBinder(errorCallbackToken);
                    _data.writeTypedObject(exceptionBundle, 0);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.window.ITaskFragmentOrganizer
            public void onActivityReparentToTask(int taskId, Intent activityIntent, IBinder activityToken) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskFragmentOrganizer.DESCRIPTOR);
                    _data.writeInt(taskId);
                    _data.writeTypedObject(activityIntent, 0);
                    _data.writeStrongBinder(activityToken);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 5;
        }
    }
}
