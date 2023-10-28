package com.transsion.multiwindow;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.window.WindowContainerToken;
/* loaded from: classes2.dex */
public interface IMultiWindowService extends IInterface {
    public static final String DESCRIPTOR = "com.transsion.multiwindow.IMultiWindowService";

    void changeMultiWindowLocation(int i, int i2) throws RemoteException;

    void hookActiveMultiWindowMoveStart() throws RemoteException;

    void hookDefaultDisplayFixRotation() throws RemoteException;

    void hookDefaultDisplayRotation(int i) throws RemoteException;

    void hookDisplayAreaChildCount(int i, int i2) throws RemoteException;

    void hookFinishBoot() throws RemoteException;

    void hookFinishMovingLocation(int i) throws RemoteException;

    void hookFixedRotationLaunch(int i, int i2, boolean z) throws RemoteException;

    void hookFocusOnFloatWindow(boolean z) throws RemoteException;

    void hookInputMethodShown(boolean z) throws RemoteException;

    void hookMultiWindowCloseAethen() throws RemoteException;

    void hookMultiWindowFling(int i, MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) throws RemoteException;

    void hookMultiWindowHideShadow() throws RemoteException;

    void hookMultiWindowInvisible() throws RemoteException;

    void hookMultiWindowLocation(int i, int i2, int i3, int i4, int i5) throws RemoteException;

    void hookMultiWindowMuteAethen() throws RemoteException;

    void hookMultiWindowStartAethen(int i, String str) throws RemoteException;

    void hookMultiWindowToClose(int i) throws RemoteException;

    void hookMultiWindowToLarge(int i) throws RemoteException;

    void hookMultiWindowToMax(int i, WindowContainerToken windowContainerToken, WindowContainerToken windowContainerToken2, boolean z) throws RemoteException;

    void hookMultiWindowToMin(int i) throws RemoteException;

    void hookMultiWindowToSmall(int i) throws RemoteException;

    void hookMultiWindowVisible() throws RemoteException;

    void hookOnFirstWindowDrawn() throws RemoteException;

    void hookRequestedOrientation(int i, int i2) throws RemoteException;

    void hookShowMultiDisplayWindow(WindowContainerToken windowContainerToken, SurfaceControl surfaceControl, boolean z, boolean z2) throws RemoteException;

    void hookStartActivity(Intent intent, int i, boolean z) throws RemoteException;

    void notifyToUse(String str) throws RemoteException;

    void showMultiFailedToast() throws RemoteException;

    void showSceneUnSupportMultiToast() throws RemoteException;

    void showStabilityMultiToast(String str) throws RemoteException;

    void showUnSupportMultiToast() throws RemoteException;

    void showUnSupportOtherToast() throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IMultiWindowService {
        @Override // com.transsion.multiwindow.IMultiWindowService
        public void changeMultiWindowLocation(int x, int y) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void notifyToUse(String pkgName) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookFinishBoot() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void showSceneUnSupportMultiToast() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void showMultiFailedToast() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void showUnSupportMultiToast() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void showUnSupportOtherToast() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void showStabilityMultiToast(String pkgName) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookInputMethodShown(boolean inputShown) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookShowMultiDisplayWindow(WindowContainerToken taskToken, SurfaceControl surfaceControl, boolean canLayoutInShotCutOut, boolean initiative) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookRequestedOrientation(int displayAreaId, int orientation) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookMultiWindowToMax(int displayAreaId, WindowContainerToken displayToken, WindowContainerToken taskToken, boolean canLayoutInShotCutOut) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookMultiWindowToMin(int displayAreaId) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookMultiWindowToClose(int displayAreaId) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookMultiWindowToSmall(int displayAreaId) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookMultiWindowToLarge(int displayAreaId) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookMultiWindowLocation(int displayAreaId, int x, int y, int touchX, int touchY) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookFinishMovingLocation(int displayAreaId) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookMultiWindowFling(int displayAreaId, MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookFocusOnFloatWindow(boolean focusOnFloatWindow) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookDefaultDisplayRotation(int rotation) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookDisplayAreaChildCount(int displayAreaId, int taskCount) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookMultiWindowVisible() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookMultiWindowInvisible() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookStartActivity(Intent intent, int direction, boolean initiative) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookFixedRotationLaunch(int appRotation, int displayRotation, boolean isEnterMultiWindow) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookActiveMultiWindowMoveStart() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookOnFirstWindowDrawn() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookMultiWindowHideShadow() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookMultiWindowStartAethen(int way, String pkgName) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookMultiWindowMuteAethen() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookMultiWindowCloseAethen() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowService
        public void hookDefaultDisplayFixRotation() throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IMultiWindowService {
        static final int TRANSACTION_changeMultiWindowLocation = 1;
        static final int TRANSACTION_hookActiveMultiWindowMoveStart = 27;
        static final int TRANSACTION_hookDefaultDisplayFixRotation = 33;
        static final int TRANSACTION_hookDefaultDisplayRotation = 21;
        static final int TRANSACTION_hookDisplayAreaChildCount = 22;
        static final int TRANSACTION_hookFinishBoot = 3;
        static final int TRANSACTION_hookFinishMovingLocation = 18;
        static final int TRANSACTION_hookFixedRotationLaunch = 26;
        static final int TRANSACTION_hookFocusOnFloatWindow = 20;
        static final int TRANSACTION_hookInputMethodShown = 9;
        static final int TRANSACTION_hookMultiWindowCloseAethen = 32;
        static final int TRANSACTION_hookMultiWindowFling = 19;
        static final int TRANSACTION_hookMultiWindowHideShadow = 29;
        static final int TRANSACTION_hookMultiWindowInvisible = 24;
        static final int TRANSACTION_hookMultiWindowLocation = 17;
        static final int TRANSACTION_hookMultiWindowMuteAethen = 31;
        static final int TRANSACTION_hookMultiWindowStartAethen = 30;
        static final int TRANSACTION_hookMultiWindowToClose = 14;
        static final int TRANSACTION_hookMultiWindowToLarge = 16;
        static final int TRANSACTION_hookMultiWindowToMax = 12;
        static final int TRANSACTION_hookMultiWindowToMin = 13;
        static final int TRANSACTION_hookMultiWindowToSmall = 15;
        static final int TRANSACTION_hookMultiWindowVisible = 23;
        static final int TRANSACTION_hookOnFirstWindowDrawn = 28;
        static final int TRANSACTION_hookRequestedOrientation = 11;
        static final int TRANSACTION_hookShowMultiDisplayWindow = 10;
        static final int TRANSACTION_hookStartActivity = 25;
        static final int TRANSACTION_notifyToUse = 2;
        static final int TRANSACTION_showMultiFailedToast = 5;
        static final int TRANSACTION_showSceneUnSupportMultiToast = 4;
        static final int TRANSACTION_showStabilityMultiToast = 8;
        static final int TRANSACTION_showUnSupportMultiToast = 6;
        static final int TRANSACTION_showUnSupportOtherToast = 7;

        public Stub() {
            attachInterface(this, IMultiWindowService.DESCRIPTOR);
        }

        public static IMultiWindowService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IMultiWindowService.DESCRIPTOR);
            if (iin != null && (iin instanceof IMultiWindowService)) {
                return (IMultiWindowService) iin;
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
                data.enforceInterface(IMultiWindowService.DESCRIPTOR);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(IMultiWindowService.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            int _arg1 = data.readInt();
                            data.enforceNoDataAvail();
                            changeMultiWindowLocation(_arg0, _arg1);
                            break;
                        case 2:
                            String _arg02 = data.readString();
                            data.enforceNoDataAvail();
                            notifyToUse(_arg02);
                            break;
                        case 3:
                            hookFinishBoot();
                            break;
                        case 4:
                            showSceneUnSupportMultiToast();
                            break;
                        case 5:
                            showMultiFailedToast();
                            break;
                        case 6:
                            showUnSupportMultiToast();
                            break;
                        case 7:
                            showUnSupportOtherToast();
                            break;
                        case 8:
                            String _arg03 = data.readString();
                            data.enforceNoDataAvail();
                            showStabilityMultiToast(_arg03);
                            break;
                        case 9:
                            boolean _arg04 = data.readBoolean();
                            data.enforceNoDataAvail();
                            hookInputMethodShown(_arg04);
                            break;
                        case 10:
                            WindowContainerToken _arg05 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            SurfaceControl _arg12 = (SurfaceControl) data.readTypedObject(SurfaceControl.CREATOR);
                            boolean _arg2 = data.readBoolean();
                            boolean _arg3 = data.readBoolean();
                            data.enforceNoDataAvail();
                            hookShowMultiDisplayWindow(_arg05, _arg12, _arg2, _arg3);
                            break;
                        case 11:
                            int _arg06 = data.readInt();
                            int _arg13 = data.readInt();
                            data.enforceNoDataAvail();
                            hookRequestedOrientation(_arg06, _arg13);
                            break;
                        case 12:
                            int _arg07 = data.readInt();
                            WindowContainerToken _arg14 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            WindowContainerToken _arg22 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            boolean _arg32 = data.readBoolean();
                            data.enforceNoDataAvail();
                            hookMultiWindowToMax(_arg07, _arg14, _arg22, _arg32);
                            break;
                        case 13:
                            int _arg08 = data.readInt();
                            data.enforceNoDataAvail();
                            hookMultiWindowToMin(_arg08);
                            break;
                        case 14:
                            int _arg09 = data.readInt();
                            data.enforceNoDataAvail();
                            hookMultiWindowToClose(_arg09);
                            break;
                        case 15:
                            int _arg010 = data.readInt();
                            data.enforceNoDataAvail();
                            hookMultiWindowToSmall(_arg010);
                            break;
                        case 16:
                            int _arg011 = data.readInt();
                            data.enforceNoDataAvail();
                            hookMultiWindowToLarge(_arg011);
                            break;
                        case 17:
                            int _arg012 = data.readInt();
                            int _arg15 = data.readInt();
                            int _arg23 = data.readInt();
                            int _arg33 = data.readInt();
                            int _arg4 = data.readInt();
                            data.enforceNoDataAvail();
                            hookMultiWindowLocation(_arg012, _arg15, _arg23, _arg33, _arg4);
                            break;
                        case 18:
                            int _arg013 = data.readInt();
                            data.enforceNoDataAvail();
                            hookFinishMovingLocation(_arg013);
                            break;
                        case 19:
                            int _arg014 = data.readInt();
                            MotionEvent _arg16 = (MotionEvent) data.readTypedObject(MotionEvent.CREATOR);
                            MotionEvent _arg24 = (MotionEvent) data.readTypedObject(MotionEvent.CREATOR);
                            float _arg34 = data.readFloat();
                            float _arg42 = data.readFloat();
                            data.enforceNoDataAvail();
                            hookMultiWindowFling(_arg014, _arg16, _arg24, _arg34, _arg42);
                            break;
                        case 20:
                            boolean _arg015 = data.readBoolean();
                            data.enforceNoDataAvail();
                            hookFocusOnFloatWindow(_arg015);
                            break;
                        case 21:
                            int _arg016 = data.readInt();
                            data.enforceNoDataAvail();
                            hookDefaultDisplayRotation(_arg016);
                            break;
                        case 22:
                            int _arg017 = data.readInt();
                            int _arg17 = data.readInt();
                            data.enforceNoDataAvail();
                            hookDisplayAreaChildCount(_arg017, _arg17);
                            break;
                        case 23:
                            hookMultiWindowVisible();
                            break;
                        case 24:
                            hookMultiWindowInvisible();
                            break;
                        case 25:
                            Intent _arg018 = (Intent) data.readTypedObject(Intent.CREATOR);
                            int _arg18 = data.readInt();
                            boolean _arg25 = data.readBoolean();
                            data.enforceNoDataAvail();
                            hookStartActivity(_arg018, _arg18, _arg25);
                            break;
                        case 26:
                            int _arg019 = data.readInt();
                            int _arg19 = data.readInt();
                            boolean _arg26 = data.readBoolean();
                            data.enforceNoDataAvail();
                            hookFixedRotationLaunch(_arg019, _arg19, _arg26);
                            break;
                        case 27:
                            hookActiveMultiWindowMoveStart();
                            break;
                        case 28:
                            hookOnFirstWindowDrawn();
                            break;
                        case 29:
                            hookMultiWindowHideShadow();
                            break;
                        case 30:
                            int _arg020 = data.readInt();
                            String _arg110 = data.readString();
                            data.enforceNoDataAvail();
                            hookMultiWindowStartAethen(_arg020, _arg110);
                            break;
                        case 31:
                            hookMultiWindowMuteAethen();
                            break;
                        case 32:
                            hookMultiWindowCloseAethen();
                            break;
                        case 33:
                            hookDefaultDisplayFixRotation();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IMultiWindowService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IMultiWindowService.DESCRIPTOR;
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void changeMultiWindowLocation(int x, int y) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    _data.writeInt(x);
                    _data.writeInt(y);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void notifyToUse(String pkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    _data.writeString(pkgName);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookFinishBoot() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void showSceneUnSupportMultiToast() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void showMultiFailedToast() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void showUnSupportMultiToast() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void showUnSupportOtherToast() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void showStabilityMultiToast(String pkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    _data.writeString(pkgName);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookInputMethodShown(boolean inputShown) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    _data.writeBoolean(inputShown);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookShowMultiDisplayWindow(WindowContainerToken taskToken, SurfaceControl surfaceControl, boolean canLayoutInShotCutOut, boolean initiative) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    _data.writeTypedObject(taskToken, 0);
                    _data.writeTypedObject(surfaceControl, 0);
                    _data.writeBoolean(canLayoutInShotCutOut);
                    _data.writeBoolean(initiative);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookRequestedOrientation(int displayAreaId, int orientation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    _data.writeInt(displayAreaId);
                    _data.writeInt(orientation);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookMultiWindowToMax(int displayAreaId, WindowContainerToken displayToken, WindowContainerToken taskToken, boolean canLayoutInShotCutOut) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    _data.writeInt(displayAreaId);
                    _data.writeTypedObject(displayToken, 0);
                    _data.writeTypedObject(taskToken, 0);
                    _data.writeBoolean(canLayoutInShotCutOut);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookMultiWindowToMin(int displayAreaId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    _data.writeInt(displayAreaId);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookMultiWindowToClose(int displayAreaId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    _data.writeInt(displayAreaId);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookMultiWindowToSmall(int displayAreaId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    _data.writeInt(displayAreaId);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookMultiWindowToLarge(int displayAreaId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    _data.writeInt(displayAreaId);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookMultiWindowLocation(int displayAreaId, int x, int y, int touchX, int touchY) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    _data.writeInt(displayAreaId);
                    _data.writeInt(x);
                    _data.writeInt(y);
                    _data.writeInt(touchX);
                    _data.writeInt(touchY);
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookFinishMovingLocation(int displayAreaId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    _data.writeInt(displayAreaId);
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookMultiWindowFling(int displayAreaId, MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    _data.writeInt(displayAreaId);
                    _data.writeTypedObject(e1, 0);
                    _data.writeTypedObject(e2, 0);
                    _data.writeFloat(velocityX);
                    _data.writeFloat(velocityY);
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookFocusOnFloatWindow(boolean focusOnFloatWindow) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    _data.writeBoolean(focusOnFloatWindow);
                    this.mRemote.transact(20, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookDefaultDisplayRotation(int rotation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    _data.writeInt(rotation);
                    this.mRemote.transact(21, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookDisplayAreaChildCount(int displayAreaId, int taskCount) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    _data.writeInt(displayAreaId);
                    _data.writeInt(taskCount);
                    this.mRemote.transact(22, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookMultiWindowVisible() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    this.mRemote.transact(23, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookMultiWindowInvisible() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    this.mRemote.transact(24, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookStartActivity(Intent intent, int direction, boolean initiative) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    _data.writeTypedObject(intent, 0);
                    _data.writeInt(direction);
                    _data.writeBoolean(initiative);
                    this.mRemote.transact(25, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookFixedRotationLaunch(int appRotation, int displayRotation, boolean isEnterMultiWindow) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    _data.writeInt(appRotation);
                    _data.writeInt(displayRotation);
                    _data.writeBoolean(isEnterMultiWindow);
                    this.mRemote.transact(26, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookActiveMultiWindowMoveStart() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    this.mRemote.transact(27, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookOnFirstWindowDrawn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    this.mRemote.transact(28, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookMultiWindowHideShadow() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    this.mRemote.transact(29, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookMultiWindowStartAethen(int way, String pkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    _data.writeInt(way);
                    _data.writeString(pkgName);
                    this.mRemote.transact(30, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookMultiWindowMuteAethen() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    this.mRemote.transact(31, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookMultiWindowCloseAethen() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    this.mRemote.transact(32, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowService
            public void hookDefaultDisplayFixRotation() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowService.DESCRIPTOR);
                    this.mRemote.transact(33, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
