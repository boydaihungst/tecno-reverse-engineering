package android.view;

import android.app.ActivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.IRemoteAnimationFinishedCallback;
/* loaded from: classes3.dex */
public interface IMultiTaskRemoteAnimationRunner extends IInterface {
    public static final String DESCRIPTOR = "android.view.IMultiTaskRemoteAnimationRunner";

    void onAnimationCancelled() throws RemoteException;

    void onAnimationStart(ActivityManager.RunningTaskInfo runningTaskInfo, SurfaceControl surfaceControl, IRemoteAnimationFinishedCallback iRemoteAnimationFinishedCallback) throws RemoteException;

    /* loaded from: classes3.dex */
    public static class Default implements IMultiTaskRemoteAnimationRunner {
        @Override // android.view.IMultiTaskRemoteAnimationRunner
        public void onAnimationStart(ActivityManager.RunningTaskInfo taskInfo, SurfaceControl taskLeash, IRemoteAnimationFinishedCallback finishedCallback) throws RemoteException {
        }

        @Override // android.view.IMultiTaskRemoteAnimationRunner
        public void onAnimationCancelled() throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes3.dex */
    public static abstract class Stub extends Binder implements IMultiTaskRemoteAnimationRunner {
        static final int TRANSACTION_onAnimationCancelled = 2;
        static final int TRANSACTION_onAnimationStart = 1;

        public Stub() {
            attachInterface(this, IMultiTaskRemoteAnimationRunner.DESCRIPTOR);
        }

        public static IMultiTaskRemoteAnimationRunner asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IMultiTaskRemoteAnimationRunner.DESCRIPTOR);
            if (iin != null && (iin instanceof IMultiTaskRemoteAnimationRunner)) {
                return (IMultiTaskRemoteAnimationRunner) iin;
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
                    return "onAnimationStart";
                case 2:
                    return "onAnimationCancelled";
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
                data.enforceInterface(IMultiTaskRemoteAnimationRunner.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IMultiTaskRemoteAnimationRunner.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            ActivityManager.RunningTaskInfo _arg0 = (ActivityManager.RunningTaskInfo) data.readTypedObject(ActivityManager.RunningTaskInfo.CREATOR);
                            SurfaceControl _arg1 = (SurfaceControl) data.readTypedObject(SurfaceControl.CREATOR);
                            IRemoteAnimationFinishedCallback _arg2 = IRemoteAnimationFinishedCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            onAnimationStart(_arg0, _arg1, _arg2);
                            break;
                        case 2:
                            onAnimationCancelled();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes3.dex */
        public static class Proxy implements IMultiTaskRemoteAnimationRunner {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IMultiTaskRemoteAnimationRunner.DESCRIPTOR;
            }

            @Override // android.view.IMultiTaskRemoteAnimationRunner
            public void onAnimationStart(ActivityManager.RunningTaskInfo taskInfo, SurfaceControl taskLeash, IRemoteAnimationFinishedCallback finishedCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiTaskRemoteAnimationRunner.DESCRIPTOR);
                    _data.writeTypedObject(taskInfo, 0);
                    _data.writeTypedObject(taskLeash, 0);
                    _data.writeStrongInterface(finishedCallback);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IMultiTaskRemoteAnimationRunner
            public void onAnimationCancelled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiTaskRemoteAnimationRunner.DESCRIPTOR);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 1;
        }
    }
}
