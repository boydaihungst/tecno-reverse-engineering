package android.window;

import android.app.ActivityManager;
import android.content.pm.ParceledListSlice;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.SurfaceControl;
import android.window.ITaskOrganizer;
import java.util.List;
/* loaded from: classes4.dex */
public interface ITaskOrganizerController extends IInterface {
    public static final String DESCRIPTOR = "android.window.ITaskOrganizerController";

    void createRootTask(int i, int i2, IBinder iBinder) throws RemoteException;

    SurfaceControl createRootTaskAnimationLeash(int i) throws RemoteException;

    boolean deleteRootTask(WindowContainerToken windowContainerToken) throws RemoteException;

    void destroyRootTaskAnimationLeash(int i) throws RemoteException;

    List<ActivityManager.RunningTaskInfo> getChildTasks(WindowContainerToken windowContainerToken, int[] iArr) throws RemoteException;

    ActivityManager.RunningTaskInfo getFocusedRootTask(int i) throws RemoteException;

    WindowContainerToken getImeTarget(int i) throws RemoteException;

    List<ActivityManager.RunningTaskInfo> getRootTasks(int i, int[] iArr) throws RemoteException;

    List<ActivityManager.RunningTaskInfo> getVisibleTasks(int i) throws RemoteException;

    boolean isLeafFocusedRootTask(int i) throws RemoteException;

    void onTaskMoveToFront(WindowContainerToken windowContainerToken) throws RemoteException;

    ParceledListSlice<TaskAppearedInfo> registerTaskOrganizer(ITaskOrganizer iTaskOrganizer) throws RemoteException;

    void restartTaskTopActivityProcessIfVisible(WindowContainerToken windowContainerToken) throws RemoteException;

    void setCompatibleModeInTask(WindowContainerToken windowContainerToken, int i) throws RemoteException;

    void setInterceptBackPressedOnTaskRoot(WindowContainerToken windowContainerToken, boolean z) throws RemoteException;

    void setIsIgnoreOrientationRequestDisabled(boolean z) throws RemoteException;

    void unregisterTaskOrganizer(ITaskOrganizer iTaskOrganizer) throws RemoteException;

    void updateCameraCompatControlState(WindowContainerToken windowContainerToken, int i) throws RemoteException;

    /* loaded from: classes4.dex */
    public static class Default implements ITaskOrganizerController {
        @Override // android.window.ITaskOrganizerController
        public ParceledListSlice<TaskAppearedInfo> registerTaskOrganizer(ITaskOrganizer organizer) throws RemoteException {
            return null;
        }

        @Override // android.window.ITaskOrganizerController
        public void unregisterTaskOrganizer(ITaskOrganizer organizer) throws RemoteException {
        }

        @Override // android.window.ITaskOrganizerController
        public SurfaceControl createRootTaskAnimationLeash(int taskId) throws RemoteException {
            return null;
        }

        @Override // android.window.ITaskOrganizerController
        public void destroyRootTaskAnimationLeash(int taskId) throws RemoteException {
        }

        @Override // android.window.ITaskOrganizerController
        public void createRootTask(int displayId, int windowingMode, IBinder launchCookie) throws RemoteException {
        }

        @Override // android.window.ITaskOrganizerController
        public boolean deleteRootTask(WindowContainerToken task) throws RemoteException {
            return false;
        }

        @Override // android.window.ITaskOrganizerController
        public List<ActivityManager.RunningTaskInfo> getChildTasks(WindowContainerToken parent, int[] activityTypes) throws RemoteException {
            return null;
        }

        @Override // android.window.ITaskOrganizerController
        public List<ActivityManager.RunningTaskInfo> getRootTasks(int displayId, int[] activityTypes) throws RemoteException {
            return null;
        }

        @Override // android.window.ITaskOrganizerController
        public WindowContainerToken getImeTarget(int display) throws RemoteException {
            return null;
        }

        @Override // android.window.ITaskOrganizerController
        public void setInterceptBackPressedOnTaskRoot(WindowContainerToken task, boolean interceptBackPressed) throws RemoteException {
        }

        @Override // android.window.ITaskOrganizerController
        public void restartTaskTopActivityProcessIfVisible(WindowContainerToken task) throws RemoteException {
        }

        @Override // android.window.ITaskOrganizerController
        public void updateCameraCompatControlState(WindowContainerToken task, int state) throws RemoteException {
        }

        @Override // android.window.ITaskOrganizerController
        public void setIsIgnoreOrientationRequestDisabled(boolean isDisabled) throws RemoteException {
        }

        @Override // android.window.ITaskOrganizerController
        public ActivityManager.RunningTaskInfo getFocusedRootTask(int displayId) throws RemoteException {
            return null;
        }

        @Override // android.window.ITaskOrganizerController
        public boolean isLeafFocusedRootTask(int displayId) throws RemoteException {
            return false;
        }

        @Override // android.window.ITaskOrganizerController
        public List<ActivityManager.RunningTaskInfo> getVisibleTasks(int displayId) throws RemoteException {
            return null;
        }

        @Override // android.window.ITaskOrganizerController
        public void onTaskMoveToFront(WindowContainerToken token) throws RemoteException {
        }

        @Override // android.window.ITaskOrganizerController
        public void setCompatibleModeInTask(WindowContainerToken task, int compatibleMode) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class Stub extends Binder implements ITaskOrganizerController {
        static final int TRANSACTION_createRootTask = 5;
        static final int TRANSACTION_createRootTaskAnimationLeash = 3;
        static final int TRANSACTION_deleteRootTask = 6;
        static final int TRANSACTION_destroyRootTaskAnimationLeash = 4;
        static final int TRANSACTION_getChildTasks = 7;
        static final int TRANSACTION_getFocusedRootTask = 14;
        static final int TRANSACTION_getImeTarget = 9;
        static final int TRANSACTION_getRootTasks = 8;
        static final int TRANSACTION_getVisibleTasks = 16;
        static final int TRANSACTION_isLeafFocusedRootTask = 15;
        static final int TRANSACTION_onTaskMoveToFront = 17;
        static final int TRANSACTION_registerTaskOrganizer = 1;
        static final int TRANSACTION_restartTaskTopActivityProcessIfVisible = 11;
        static final int TRANSACTION_setCompatibleModeInTask = 18;
        static final int TRANSACTION_setInterceptBackPressedOnTaskRoot = 10;
        static final int TRANSACTION_setIsIgnoreOrientationRequestDisabled = 13;
        static final int TRANSACTION_unregisterTaskOrganizer = 2;
        static final int TRANSACTION_updateCameraCompatControlState = 12;

        public Stub() {
            attachInterface(this, ITaskOrganizerController.DESCRIPTOR);
        }

        public static ITaskOrganizerController asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ITaskOrganizerController.DESCRIPTOR);
            if (iin != null && (iin instanceof ITaskOrganizerController)) {
                return (ITaskOrganizerController) iin;
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
                    return "registerTaskOrganizer";
                case 2:
                    return "unregisterTaskOrganizer";
                case 3:
                    return "createRootTaskAnimationLeash";
                case 4:
                    return "destroyRootTaskAnimationLeash";
                case 5:
                    return "createRootTask";
                case 6:
                    return "deleteRootTask";
                case 7:
                    return "getChildTasks";
                case 8:
                    return "getRootTasks";
                case 9:
                    return "getImeTarget";
                case 10:
                    return "setInterceptBackPressedOnTaskRoot";
                case 11:
                    return "restartTaskTopActivityProcessIfVisible";
                case 12:
                    return "updateCameraCompatControlState";
                case 13:
                    return "setIsIgnoreOrientationRequestDisabled";
                case 14:
                    return "getFocusedRootTask";
                case 15:
                    return "isLeafFocusedRootTask";
                case 16:
                    return "getVisibleTasks";
                case 17:
                    return "onTaskMoveToFront";
                case 18:
                    return "setCompatibleModeInTask";
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
                data.enforceInterface(ITaskOrganizerController.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ITaskOrganizerController.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            ITaskOrganizer _arg0 = ITaskOrganizer.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            ParceledListSlice<TaskAppearedInfo> _result = registerTaskOrganizer(_arg0);
                            reply.writeNoException();
                            reply.writeTypedObject(_result, 1);
                            break;
                        case 2:
                            ITaskOrganizer _arg02 = ITaskOrganizer.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            unregisterTaskOrganizer(_arg02);
                            reply.writeNoException();
                            break;
                        case 3:
                            int _arg03 = data.readInt();
                            data.enforceNoDataAvail();
                            SurfaceControl _result2 = createRootTaskAnimationLeash(_arg03);
                            reply.writeNoException();
                            reply.writeTypedObject(_result2, 1);
                            break;
                        case 4:
                            int _arg04 = data.readInt();
                            data.enforceNoDataAvail();
                            destroyRootTaskAnimationLeash(_arg04);
                            reply.writeNoException();
                            break;
                        case 5:
                            int _arg05 = data.readInt();
                            int _arg1 = data.readInt();
                            IBinder _arg2 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            createRootTask(_arg05, _arg1, _arg2);
                            reply.writeNoException();
                            break;
                        case 6:
                            WindowContainerToken _arg06 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result3 = deleteRootTask(_arg06);
                            reply.writeNoException();
                            reply.writeBoolean(_result3);
                            break;
                        case 7:
                            WindowContainerToken _arg07 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            int[] _arg12 = data.createIntArray();
                            data.enforceNoDataAvail();
                            List<ActivityManager.RunningTaskInfo> _result4 = getChildTasks(_arg07, _arg12);
                            reply.writeNoException();
                            reply.writeTypedList(_result4);
                            break;
                        case 8:
                            int _arg08 = data.readInt();
                            int[] _arg13 = data.createIntArray();
                            data.enforceNoDataAvail();
                            List<ActivityManager.RunningTaskInfo> _result5 = getRootTasks(_arg08, _arg13);
                            reply.writeNoException();
                            reply.writeTypedList(_result5);
                            break;
                        case 9:
                            int _arg09 = data.readInt();
                            data.enforceNoDataAvail();
                            WindowContainerToken _result6 = getImeTarget(_arg09);
                            reply.writeNoException();
                            reply.writeTypedObject(_result6, 1);
                            break;
                        case 10:
                            WindowContainerToken _arg010 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            boolean _arg14 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setInterceptBackPressedOnTaskRoot(_arg010, _arg14);
                            reply.writeNoException();
                            break;
                        case 11:
                            WindowContainerToken _arg011 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            data.enforceNoDataAvail();
                            restartTaskTopActivityProcessIfVisible(_arg011);
                            reply.writeNoException();
                            break;
                        case 12:
                            WindowContainerToken _arg012 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            int _arg15 = data.readInt();
                            data.enforceNoDataAvail();
                            updateCameraCompatControlState(_arg012, _arg15);
                            reply.writeNoException();
                            break;
                        case 13:
                            boolean _arg013 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setIsIgnoreOrientationRequestDisabled(_arg013);
                            reply.writeNoException();
                            break;
                        case 14:
                            int _arg014 = data.readInt();
                            data.enforceNoDataAvail();
                            ActivityManager.RunningTaskInfo _result7 = getFocusedRootTask(_arg014);
                            reply.writeNoException();
                            reply.writeTypedObject(_result7, 1);
                            break;
                        case 15:
                            int _arg015 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result8 = isLeafFocusedRootTask(_arg015);
                            reply.writeNoException();
                            reply.writeBoolean(_result8);
                            break;
                        case 16:
                            int _arg016 = data.readInt();
                            data.enforceNoDataAvail();
                            List<ActivityManager.RunningTaskInfo> _result9 = getVisibleTasks(_arg016);
                            reply.writeNoException();
                            reply.writeTypedList(_result9);
                            break;
                        case 17:
                            WindowContainerToken _arg017 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            data.enforceNoDataAvail();
                            onTaskMoveToFront(_arg017);
                            reply.writeNoException();
                            break;
                        case 18:
                            WindowContainerToken _arg018 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            int _arg16 = data.readInt();
                            data.enforceNoDataAvail();
                            setCompatibleModeInTask(_arg018, _arg16);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes4.dex */
        public static class Proxy implements ITaskOrganizerController {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ITaskOrganizerController.DESCRIPTOR;
            }

            @Override // android.window.ITaskOrganizerController
            public ParceledListSlice<TaskAppearedInfo> registerTaskOrganizer(ITaskOrganizer organizer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskOrganizerController.DESCRIPTOR);
                    _data.writeStrongInterface(organizer);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    ParceledListSlice<TaskAppearedInfo> _result = (ParceledListSlice) _reply.readTypedObject(ParceledListSlice.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.window.ITaskOrganizerController
            public void unregisterTaskOrganizer(ITaskOrganizer organizer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskOrganizerController.DESCRIPTOR);
                    _data.writeStrongInterface(organizer);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.window.ITaskOrganizerController
            public SurfaceControl createRootTaskAnimationLeash(int taskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskOrganizerController.DESCRIPTOR);
                    _data.writeInt(taskId);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    SurfaceControl _result = (SurfaceControl) _reply.readTypedObject(SurfaceControl.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.window.ITaskOrganizerController
            public void destroyRootTaskAnimationLeash(int taskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskOrganizerController.DESCRIPTOR);
                    _data.writeInt(taskId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.window.ITaskOrganizerController
            public void createRootTask(int displayId, int windowingMode, IBinder launchCookie) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskOrganizerController.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeInt(windowingMode);
                    _data.writeStrongBinder(launchCookie);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.window.ITaskOrganizerController
            public boolean deleteRootTask(WindowContainerToken task) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskOrganizerController.DESCRIPTOR);
                    _data.writeTypedObject(task, 0);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.window.ITaskOrganizerController
            public List<ActivityManager.RunningTaskInfo> getChildTasks(WindowContainerToken parent, int[] activityTypes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskOrganizerController.DESCRIPTOR);
                    _data.writeTypedObject(parent, 0);
                    _data.writeIntArray(activityTypes);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    List<ActivityManager.RunningTaskInfo> _result = _reply.createTypedArrayList(ActivityManager.RunningTaskInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.window.ITaskOrganizerController
            public List<ActivityManager.RunningTaskInfo> getRootTasks(int displayId, int[] activityTypes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskOrganizerController.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeIntArray(activityTypes);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    List<ActivityManager.RunningTaskInfo> _result = _reply.createTypedArrayList(ActivityManager.RunningTaskInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.window.ITaskOrganizerController
            public WindowContainerToken getImeTarget(int display) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskOrganizerController.DESCRIPTOR);
                    _data.writeInt(display);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    WindowContainerToken _result = (WindowContainerToken) _reply.readTypedObject(WindowContainerToken.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.window.ITaskOrganizerController
            public void setInterceptBackPressedOnTaskRoot(WindowContainerToken task, boolean interceptBackPressed) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskOrganizerController.DESCRIPTOR);
                    _data.writeTypedObject(task, 0);
                    _data.writeBoolean(interceptBackPressed);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.window.ITaskOrganizerController
            public void restartTaskTopActivityProcessIfVisible(WindowContainerToken task) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskOrganizerController.DESCRIPTOR);
                    _data.writeTypedObject(task, 0);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.window.ITaskOrganizerController
            public void updateCameraCompatControlState(WindowContainerToken task, int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskOrganizerController.DESCRIPTOR);
                    _data.writeTypedObject(task, 0);
                    _data.writeInt(state);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.window.ITaskOrganizerController
            public void setIsIgnoreOrientationRequestDisabled(boolean isDisabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskOrganizerController.DESCRIPTOR);
                    _data.writeBoolean(isDisabled);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.window.ITaskOrganizerController
            public ActivityManager.RunningTaskInfo getFocusedRootTask(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskOrganizerController.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    ActivityManager.RunningTaskInfo _result = (ActivityManager.RunningTaskInfo) _reply.readTypedObject(ActivityManager.RunningTaskInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.window.ITaskOrganizerController
            public boolean isLeafFocusedRootTask(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskOrganizerController.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.window.ITaskOrganizerController
            public List<ActivityManager.RunningTaskInfo> getVisibleTasks(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskOrganizerController.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    List<ActivityManager.RunningTaskInfo> _result = _reply.createTypedArrayList(ActivityManager.RunningTaskInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.window.ITaskOrganizerController
            public void onTaskMoveToFront(WindowContainerToken token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskOrganizerController.DESCRIPTOR);
                    _data.writeTypedObject(token, 0);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.window.ITaskOrganizerController
            public void setCompatibleModeInTask(WindowContainerToken task, int compatibleMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITaskOrganizerController.DESCRIPTOR);
                    _data.writeTypedObject(task, 0);
                    _data.writeInt(compatibleMode);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 17;
        }
    }
}
