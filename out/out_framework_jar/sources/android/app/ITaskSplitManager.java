package android.app;

import android.app.ActivityManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.IRemoteAnimationFinishedCallback;
import android.view.MultiTaskRemoteAnimationAdapter;
import android.view.RemoteAnimationAdapter;
import android.window.IWindowContainerTransactionCallback;
import android.window.IWindowContainerTransactionCallbackSync;
import android.window.IWindowContainerTransactionCallbackWithFinishCB;
import android.window.WindowContainerToken;
import android.window.WindowContainerTransaction;
/* loaded from: classes.dex */
public interface ITaskSplitManager extends IInterface {
    public static final String DESCRIPTOR = "android.app.ITaskSplitManager";

    void applySyncTransactionAck() throws RemoteException;

    WindowContainerTransaction exitSplitScreenSync(int i) throws RemoteException;

    void exitSplitScreenWithSyncTransaction(int i, WindowContainerTransaction windowContainerTransaction, IWindowContainerTransactionCallbackSync iWindowContainerTransactionCallbackSync) throws RemoteException;

    int getSplitPrimaryTaskId() throws RemoteException;

    ActivityManager.RunningTaskInfo getSplitPrimaryTopTaskInfo() throws RemoteException;

    int getSplitSecondaryTaskId() throws RemoteException;

    ActivityManager.RunningTaskInfo getSplitSecondaryTopTaskInfo() throws RemoteException;

    boolean isSplitActive() throws RemoteException;

    void reverseSplitPosition() throws RemoteException;

    void startAnimationOnSplitSideTask(int i, MultiTaskRemoteAnimationAdapter multiTaskRemoteAnimationAdapter) throws RemoteException;

    void startMultiWinToSplit(boolean z, WindowContainerToken windowContainerToken, WindowContainerToken windowContainerToken2, Bundle bundle, Bundle bundle2, boolean z2, int i, float f, IWindowContainerTransactionCallbackWithFinishCB iWindowContainerTransactionCallbackWithFinishCB) throws RemoteException;

    void startTaskAnimation(int i, MultiTaskRemoteAnimationAdapter multiTaskRemoteAnimationAdapter) throws RemoteException;

    IRemoteAnimationFinishedCallback startTasksToSplit(boolean z, WindowContainerToken windowContainerToken, WindowContainerToken windowContainerToken2, Bundle bundle, Bundle bundle2, boolean z2, int i, float f, IWindowContainerTransactionCallback iWindowContainerTransactionCallback) throws RemoteException;

    void tranLauncherTask(int i, Bundle bundle, int i2, float f) throws RemoteException;

    void tranStartTasks(int i, Bundle bundle, int i2, Bundle bundle2, int i3, float f, RemoteAnimationAdapter remoteAnimationAdapter) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements ITaskSplitManager {
        @Override // android.app.ITaskSplitManager
        public void startTaskAnimation(int taskId, MultiTaskRemoteAnimationAdapter adapter) throws RemoteException {
        }

        @Override // android.app.ITaskSplitManager
        public void startAnimationOnSplitSideTask(int taskId, MultiTaskRemoteAnimationAdapter adapter) throws RemoteException {
        }

        @Override // android.app.ITaskSplitManager
        public boolean isSplitActive() throws RemoteException {
            return false;
        }

        @Override // android.app.ITaskSplitManager
        public int getSplitPrimaryTaskId() throws RemoteException {
            return 0;
        }

        @Override // android.app.ITaskSplitManager
        public int getSplitSecondaryTaskId() throws RemoteException {
            return 0;
        }

        @Override // android.app.ITaskSplitManager
        public ActivityManager.RunningTaskInfo getSplitPrimaryTopTaskInfo() throws RemoteException {
            return null;
        }

        @Override // android.app.ITaskSplitManager
        public ActivityManager.RunningTaskInfo getSplitSecondaryTopTaskInfo() throws RemoteException {
            return null;
        }

        @Override // android.app.ITaskSplitManager
        public WindowContainerTransaction exitSplitScreenSync(int toTopTaskId) throws RemoteException {
            return null;
        }

        @Override // android.app.ITaskSplitManager
        public void exitSplitScreenWithSyncTransaction(int toTopTaskId, WindowContainerTransaction appendWct, IWindowContainerTransactionCallbackSync syncCallback) throws RemoteException {
        }

        @Override // android.app.ITaskSplitManager
        public void applySyncTransactionAck() throws RemoteException {
        }

        @Override // android.app.ITaskSplitManager
        public void tranLauncherTask(int mainTaskId, Bundle mainOptions, int sidePosition, float splitRatio) throws RemoteException {
        }

        @Override // android.app.ITaskSplitManager
        public void tranStartTasks(int mainTaskId, Bundle mainOptions, int sideTaskId, Bundle sideOptions, int sidePosition, float splitRatio, RemoteAnimationAdapter adapter) throws RemoteException {
        }

        @Override // android.app.ITaskSplitManager
        public IRemoteAnimationFinishedCallback startTasksToSplit(boolean hasLauncher, WindowContainerToken mainToken, WindowContainerToken sideToken, Bundle mainOptions, Bundle sideOptions, boolean isMultiWinInMainStage, int sidePosition, float splitRatio, IWindowContainerTransactionCallback callback) throws RemoteException {
            return null;
        }

        @Override // android.app.ITaskSplitManager
        public void startMultiWinToSplit(boolean hasLauncher, WindowContainerToken mainToken, WindowContainerToken sideToken, Bundle mainOptions, Bundle sideOptions, boolean isMultiWinInMainStage, int sidePosition, float splitRatio, IWindowContainerTransactionCallbackWithFinishCB callback) throws RemoteException {
        }

        @Override // android.app.ITaskSplitManager
        public void reverseSplitPosition() throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements ITaskSplitManager {
        static final int TRANSACTION_applySyncTransactionAck = 10;
        static final int TRANSACTION_exitSplitScreenSync = 8;
        static final int TRANSACTION_exitSplitScreenWithSyncTransaction = 9;
        static final int TRANSACTION_getSplitPrimaryTaskId = 4;
        static final int TRANSACTION_getSplitPrimaryTopTaskInfo = 6;
        static final int TRANSACTION_getSplitSecondaryTaskId = 5;
        static final int TRANSACTION_getSplitSecondaryTopTaskInfo = 7;
        static final int TRANSACTION_isSplitActive = 3;
        static final int TRANSACTION_reverseSplitPosition = 15;
        static final int TRANSACTION_startAnimationOnSplitSideTask = 2;
        static final int TRANSACTION_startMultiWinToSplit = 14;
        static final int TRANSACTION_startTaskAnimation = 1;
        static final int TRANSACTION_startTasksToSplit = 13;
        static final int TRANSACTION_tranLauncherTask = 11;
        static final int TRANSACTION_tranStartTasks = 12;

        public Stub() {
            attachInterface(this, ITaskSplitManager.DESCRIPTOR);
        }

        public static ITaskSplitManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ITaskSplitManager.DESCRIPTOR);
            if (iin != null && (iin instanceof ITaskSplitManager)) {
                return (ITaskSplitManager) iin;
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
                    return "startTaskAnimation";
                case 2:
                    return "startAnimationOnSplitSideTask";
                case 3:
                    return "isSplitActive";
                case 4:
                    return "getSplitPrimaryTaskId";
                case 5:
                    return "getSplitSecondaryTaskId";
                case 6:
                    return "getSplitPrimaryTopTaskInfo";
                case 7:
                    return "getSplitSecondaryTopTaskInfo";
                case 8:
                    return "exitSplitScreenSync";
                case 9:
                    return "exitSplitScreenWithSyncTransaction";
                case 10:
                    return "applySyncTransactionAck";
                case 11:
                    return "tranLauncherTask";
                case 12:
                    return "tranStartTasks";
                case 13:
                    return "startTasksToSplit";
                case 14:
                    return "startMultiWinToSplit";
                case 15:
                    return "reverseSplitPosition";
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
                data.enforceInterface(ITaskSplitManager.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ITaskSplitManager.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            MultiTaskRemoteAnimationAdapter _arg1 = (MultiTaskRemoteAnimationAdapter) data.readTypedObject(MultiTaskRemoteAnimationAdapter.CREATOR);
                            data.enforceNoDataAvail();
                            startTaskAnimation(_arg0, _arg1);
                            reply.writeNoException();
                            break;
                        case 2:
                            int _arg02 = data.readInt();
                            MultiTaskRemoteAnimationAdapter _arg12 = (MultiTaskRemoteAnimationAdapter) data.readTypedObject(MultiTaskRemoteAnimationAdapter.CREATOR);
                            data.enforceNoDataAvail();
                            startAnimationOnSplitSideTask(_arg02, _arg12);
                            reply.writeNoException();
                            break;
                        case 3:
                            boolean _result = isSplitActive();
                            reply.writeNoException();
                            reply.writeBoolean(_result);
                            break;
                        case 4:
                            int _result2 = getSplitPrimaryTaskId();
                            reply.writeNoException();
                            reply.writeInt(_result2);
                            break;
                        case 5:
                            int _result3 = getSplitSecondaryTaskId();
                            reply.writeNoException();
                            reply.writeInt(_result3);
                            break;
                        case 6:
                            ActivityManager.RunningTaskInfo _result4 = getSplitPrimaryTopTaskInfo();
                            reply.writeNoException();
                            reply.writeTypedObject(_result4, 1);
                            break;
                        case 7:
                            ActivityManager.RunningTaskInfo _result5 = getSplitSecondaryTopTaskInfo();
                            reply.writeNoException();
                            reply.writeTypedObject(_result5, 1);
                            break;
                        case 8:
                            int _arg03 = data.readInt();
                            data.enforceNoDataAvail();
                            WindowContainerTransaction _result6 = exitSplitScreenSync(_arg03);
                            reply.writeNoException();
                            reply.writeTypedObject(_result6, 1);
                            break;
                        case 9:
                            int _arg04 = data.readInt();
                            WindowContainerTransaction _arg13 = (WindowContainerTransaction) data.readTypedObject(WindowContainerTransaction.CREATOR);
                            IWindowContainerTransactionCallbackSync _arg2 = IWindowContainerTransactionCallbackSync.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            exitSplitScreenWithSyncTransaction(_arg04, _arg13, _arg2);
                            reply.writeNoException();
                            break;
                        case 10:
                            applySyncTransactionAck();
                            reply.writeNoException();
                            break;
                        case 11:
                            int _arg05 = data.readInt();
                            Bundle _arg14 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            int _arg22 = data.readInt();
                            float _arg3 = data.readFloat();
                            data.enforceNoDataAvail();
                            tranLauncherTask(_arg05, _arg14, _arg22, _arg3);
                            reply.writeNoException();
                            break;
                        case 12:
                            int _arg06 = data.readInt();
                            Bundle _arg15 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            int _arg23 = data.readInt();
                            Bundle _arg32 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            int _arg4 = data.readInt();
                            float _arg5 = data.readFloat();
                            RemoteAnimationAdapter _arg6 = (RemoteAnimationAdapter) data.readTypedObject(RemoteAnimationAdapter.CREATOR);
                            data.enforceNoDataAvail();
                            tranStartTasks(_arg06, _arg15, _arg23, _arg32, _arg4, _arg5, _arg6);
                            reply.writeNoException();
                            break;
                        case 13:
                            boolean _arg07 = data.readBoolean();
                            WindowContainerToken _arg16 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            WindowContainerToken _arg24 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            Bundle _arg33 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            Bundle _arg42 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            boolean _arg52 = data.readBoolean();
                            int _arg62 = data.readInt();
                            float _arg7 = data.readFloat();
                            IWindowContainerTransactionCallback _arg8 = IWindowContainerTransactionCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            IRemoteAnimationFinishedCallback _result7 = startTasksToSplit(_arg07, _arg16, _arg24, _arg33, _arg42, _arg52, _arg62, _arg7, _arg8);
                            reply.writeNoException();
                            reply.writeStrongInterface(_result7);
                            break;
                        case 14:
                            boolean _arg08 = data.readBoolean();
                            WindowContainerToken _arg17 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            WindowContainerToken _arg25 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            Bundle _arg34 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            Bundle _arg43 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            boolean _arg53 = data.readBoolean();
                            int _arg63 = data.readInt();
                            float _arg72 = data.readFloat();
                            IWindowContainerTransactionCallbackWithFinishCB _arg82 = IWindowContainerTransactionCallbackWithFinishCB.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            startMultiWinToSplit(_arg08, _arg17, _arg25, _arg34, _arg43, _arg53, _arg63, _arg72, _arg82);
                            reply.writeNoException();
                            break;
                        case 15:
                            reverseSplitPosition();
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements ITaskSplitManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ITaskSplitManager.DESCRIPTOR;
            }

            @Override // android.app.ITaskSplitManager
            public void startTaskAnimation(int taskId, MultiTaskRemoteAnimationAdapter adapter) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskSplitManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    _data.writeTypedObject(adapter, 0);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ITaskSplitManager
            public void startAnimationOnSplitSideTask(int taskId, MultiTaskRemoteAnimationAdapter adapter) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskSplitManager.DESCRIPTOR);
                    _data.writeInt(taskId);
                    _data.writeTypedObject(adapter, 0);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ITaskSplitManager
            public boolean isSplitActive() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskSplitManager.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ITaskSplitManager
            public int getSplitPrimaryTaskId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskSplitManager.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ITaskSplitManager
            public int getSplitSecondaryTaskId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskSplitManager.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ITaskSplitManager
            public ActivityManager.RunningTaskInfo getSplitPrimaryTopTaskInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskSplitManager.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    ActivityManager.RunningTaskInfo _result = (ActivityManager.RunningTaskInfo) _reply.readTypedObject(ActivityManager.RunningTaskInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ITaskSplitManager
            public ActivityManager.RunningTaskInfo getSplitSecondaryTopTaskInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskSplitManager.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    ActivityManager.RunningTaskInfo _result = (ActivityManager.RunningTaskInfo) _reply.readTypedObject(ActivityManager.RunningTaskInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ITaskSplitManager
            public WindowContainerTransaction exitSplitScreenSync(int toTopTaskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskSplitManager.DESCRIPTOR);
                    _data.writeInt(toTopTaskId);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    WindowContainerTransaction _result = (WindowContainerTransaction) _reply.readTypedObject(WindowContainerTransaction.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ITaskSplitManager
            public void exitSplitScreenWithSyncTransaction(int toTopTaskId, WindowContainerTransaction appendWct, IWindowContainerTransactionCallbackSync syncCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskSplitManager.DESCRIPTOR);
                    _data.writeInt(toTopTaskId);
                    _data.writeTypedObject(appendWct, 0);
                    _data.writeStrongInterface(syncCallback);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ITaskSplitManager
            public void applySyncTransactionAck() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskSplitManager.DESCRIPTOR);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ITaskSplitManager
            public void tranLauncherTask(int mainTaskId, Bundle mainOptions, int sidePosition, float splitRatio) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskSplitManager.DESCRIPTOR);
                    _data.writeInt(mainTaskId);
                    _data.writeTypedObject(mainOptions, 0);
                    _data.writeInt(sidePosition);
                    _data.writeFloat(splitRatio);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ITaskSplitManager
            public void tranStartTasks(int mainTaskId, Bundle mainOptions, int sideTaskId, Bundle sideOptions, int sidePosition, float splitRatio, RemoteAnimationAdapter adapter) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskSplitManager.DESCRIPTOR);
                    _data.writeInt(mainTaskId);
                    _data.writeTypedObject(mainOptions, 0);
                    _data.writeInt(sideTaskId);
                    _data.writeTypedObject(sideOptions, 0);
                    _data.writeInt(sidePosition);
                    _data.writeFloat(splitRatio);
                    _data.writeTypedObject(adapter, 0);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ITaskSplitManager
            public IRemoteAnimationFinishedCallback startTasksToSplit(boolean hasLauncher, WindowContainerToken mainToken, WindowContainerToken sideToken, Bundle mainOptions, Bundle sideOptions, boolean isMultiWinInMainStage, int sidePosition, float splitRatio, IWindowContainerTransactionCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskSplitManager.DESCRIPTOR);
                    _data.writeBoolean(hasLauncher);
                    _data.writeTypedObject(mainToken, 0);
                    _data.writeTypedObject(sideToken, 0);
                    _data.writeTypedObject(mainOptions, 0);
                    _data.writeTypedObject(sideOptions, 0);
                    _data.writeBoolean(isMultiWinInMainStage);
                    _data.writeInt(sidePosition);
                    _data.writeFloat(splitRatio);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    IRemoteAnimationFinishedCallback _result = IRemoteAnimationFinishedCallback.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ITaskSplitManager
            public void startMultiWinToSplit(boolean hasLauncher, WindowContainerToken mainToken, WindowContainerToken sideToken, Bundle mainOptions, Bundle sideOptions, boolean isMultiWinInMainStage, int sidePosition, float splitRatio, IWindowContainerTransactionCallbackWithFinishCB callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskSplitManager.DESCRIPTOR);
                    _data.writeBoolean(hasLauncher);
                    _data.writeTypedObject(mainToken, 0);
                    _data.writeTypedObject(sideToken, 0);
                    _data.writeTypedObject(mainOptions, 0);
                    _data.writeTypedObject(sideOptions, 0);
                    _data.writeBoolean(isMultiWinInMainStage);
                    _data.writeInt(sidePosition);
                    _data.writeFloat(splitRatio);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.ITaskSplitManager
            public void reverseSplitPosition() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskSplitManager.DESCRIPTOR);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 14;
        }
    }
}
