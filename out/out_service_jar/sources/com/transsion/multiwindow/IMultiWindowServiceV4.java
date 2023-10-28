package com.transsion.multiwindow;

import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.MotionEvent;
import android.view.MultiTaskRemoteAnimationAdapter;
import android.view.SurfaceControl;
import android.window.IWindowContainerTransactionCallback;
import android.window.IWindowContainerTransactionCallbackSync;
import android.window.WindowContainerToken;
/* loaded from: classes2.dex */
public interface IMultiWindowServiceV4 extends IInterface {
    public static final String DESCRIPTOR = "com.transsion.multiwindow.IMultiWindowServiceV4";

    void changeMultiWindowLocation(int i, int i2) throws RemoteException;

    Rect getMultiWindowContentRegion(int i, int i2) throws RemoteException;

    void getMultiWindowParams(int i) throws RemoteException;

    void hookActiveMultiWindowEndMove(int i, int i2, MotionEvent motionEvent) throws RemoteException;

    void hookActiveMultiWindowMove(int i, int i2, MotionEvent motionEvent) throws RemoteException;

    void hookActiveMultiWindowMoveStart(int i, int i2) throws RemoteException;

    void hookActiveMultiWindowStartToMove(IBinder iBinder, int i, int i2, MotionEvent motionEvent, Point point) throws RemoteException;

    void hookCancelBlurLayer() throws RemoteException;

    void hookCreateTaskAnimation(IBinder iBinder, int i, MultiTaskRemoteAnimationAdapter multiTaskRemoteAnimationAdapter) throws RemoteException;

    void hookDefaultDisplayFixRotation() throws RemoteException;

    void hookDefaultDisplayRotation(int i) throws RemoteException;

    void hookDisplayAreaChildCount(int i, int i2, int i3) throws RemoteException;

    void hookExitSplitScreenToMultiWindow(int i) throws RemoteException;

    void hookFinishBoot() throws RemoteException;

    void hookFixedRotationLaunch(int i, int i2, boolean z) throws RemoteException;

    void hookFocusMultiWindow(int i, int i2) throws RemoteException;

    void hookFocusOnFloatWindow(boolean z) throws RemoteException;

    Rect hookGetMultiWindowDefaultRect(int i) throws RemoteException;

    void hookGetMultiWindowDefaultRectByTask(int i) throws RemoteException;

    void hookGetOrCreateMultiWindow(String str, int i, int i2, int i3, boolean z) throws RemoteException;

    void hookInputMethodShown(boolean z) throws RemoteException;

    void hookKeyguardShowing(boolean z) throws RemoteException;

    void hookMultiWindowCloseAethen() throws RemoteException;

    void hookMultiWindowHideShadow() throws RemoteException;

    void hookMultiWindowInvisible() throws RemoteException;

    void hookMultiWindowLocation(int i, int i2, int i3, int i4, int i5) throws RemoteException;

    void hookMultiWindowMuteAethen(int i) throws RemoteException;

    void hookMultiWindowStartAethen(int i, String str) throws RemoteException;

    void hookMultiWindowToClose(int i, int i2) throws RemoteException;

    void hookMultiWindowToLarge(int i, int i2) throws RemoteException;

    void hookMultiWindowToMax(int i, WindowContainerToken windowContainerToken, WindowContainerToken windowContainerToken2, boolean z) throws RemoteException;

    void hookMultiWindowToSmall(int i, int i2) throws RemoteException;

    void hookMultiWindowToSplit(int i, int i2) throws RemoteException;

    void hookMultiWindowVisible() throws RemoteException;

    void hookNotifyKeyguardState(int i, boolean z, boolean z2) throws RemoteException;

    void hookOnFirstWindowDrawn(int i, int i2) throws RemoteException;

    void hookReparentToDefaultDisplay(int i, int i2, WindowContainerToken windowContainerToken, WindowContainerToken windowContainerToken2, boolean z) throws RemoteException;

    void hookRequestedOrientation(int i, int i2, int i3) throws RemoteException;

    void hookReserveMultiWindowNumber(int i, long j) throws RemoteException;

    void hookShowBlurLayer(SurfaceControl surfaceControl, String str) throws RemoteException;

    void hookShowMultiDisplayWindow(WindowContainerToken windowContainerToken, int i, SurfaceControl surfaceControl, boolean z, int i2, String str) throws RemoteException;

    void hookStartActivity(Intent intent, int i, boolean z) throws RemoteException;

    void hookStartMultiWindow(WindowContainerToken windowContainerToken, String str, Rect rect, IWindowContainerTransactionCallback iWindowContainerTransactionCallback) throws RemoteException;

    void hookStartMultiWindowFromSplitScreen(int i, WindowContainerToken windowContainerToken, Rect rect, IWindowContainerTransactionCallbackSync iWindowContainerTransactionCallbackSync, int i2) throws RemoteException;

    void hookUpdateRotationFinished() throws RemoteException;

    void minimizeMultiWinToEdge(int i, int i2, boolean z) throws RemoteException;

    void notifyToUse(String str) throws RemoteException;

    void showMultiFailedToast() throws RemoteException;

    void showSceneUnSupportMultiToast() throws RemoteException;

    void showStabilityMultiToast(String str) throws RemoteException;

    void showUnSupportMultiToast() throws RemoteException;

    void showUnSupportOtherToast() throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IMultiWindowServiceV4 {
        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void changeMultiWindowLocation(int x, int y) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void notifyToUse(String pkgName) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookFinishBoot() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void showSceneUnSupportMultiToast() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void showMultiFailedToast() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void showUnSupportMultiToast() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void showUnSupportOtherToast() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void showStabilityMultiToast(String pkgName) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookInputMethodShown(boolean inputShown) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookShowMultiDisplayWindow(WindowContainerToken taskToken, int taskId, SurfaceControl surfaceControl, boolean canLayoutInShotCutOut, int triggerMode, String packageName) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookExitSplitScreenToMultiWindow(int multiWindowMode) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookRequestedOrientation(int multiWindowMode, int multiWindowId, int orientation) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookMultiWindowToMax(int displayAreaId, WindowContainerToken displayToken, WindowContainerToken taskToken, boolean canLayoutInShotCutOut) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookReparentToDefaultDisplay(int multiWindowMode, int multiWindowId, WindowContainerToken displayToken, WindowContainerToken taskToken, boolean canLayoutInShotCutOut) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookMultiWindowToClose(int multiWindowMode, int multiWindowId) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookMultiWindowToSmall(int multiWindowMode, int multiWindowId) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookMultiWindowToSplit(int multiWindowMode, int multiWindowId) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookMultiWindowLocation(int displayAreaId, int x, int y, int touchX, int touchY) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookFocusOnFloatWindow(boolean focusOnFloatWindow) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookDefaultDisplayRotation(int rotation) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookDisplayAreaChildCount(int multiWindowMode, int multiWindowId, int taskCount) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookMultiWindowVisible() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookMultiWindowInvisible() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookStartActivity(Intent intent, int direction, boolean initiative) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookFixedRotationLaunch(int appRotation, int displayRotation, boolean isEnterMultiWindow) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookActiveMultiWindowMoveStart(int multiWindowMode, int multiWindowId) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookActiveMultiWindowStartToMove(IBinder token, int multiWindowMode, int multiWindowId, MotionEvent e, Point downPoint) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookActiveMultiWindowMove(int multiWindowMode, int multiWindowId, MotionEvent e) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookActiveMultiWindowEndMove(int multiWindowMode, int multiWindowId, MotionEvent e) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookOnFirstWindowDrawn(int multiWindowMode, int multiWindowId) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookMultiWindowHideShadow() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookMultiWindowStartAethen(int way, String pkgName) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookMultiWindowMuteAethen(int type) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookMultiWindowCloseAethen() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookDefaultDisplayFixRotation() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookCreateTaskAnimation(IBinder token, int taskId, MultiTaskRemoteAnimationAdapter callback) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookFocusMultiWindow(int multiWindowMode, int multiWindowId) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookGetOrCreateMultiWindow(String pkgName, int direction, int triggerMode, int preferId, boolean needCreate) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public Rect hookGetMultiWindowDefaultRect(int orientationType) throws RemoteException {
            return null;
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookGetMultiWindowDefaultRectByTask(int taskId) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookStartMultiWindowFromSplitScreen(int fullTaskId, WindowContainerToken multiTaskToken, Rect multiWindowRegion, IWindowContainerTransactionCallbackSync syncCallback, int type) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookStartMultiWindow(WindowContainerToken taskToken, String packageName, Rect multiWindowRegion, IWindowContainerTransactionCallback callback) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookUpdateRotationFinished() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void minimizeMultiWinToEdge(int multiWindowMode, int multiWindowId, boolean toEdge) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public Rect getMultiWindowContentRegion(int multiWindowMode, int multiWindowId) throws RemoteException {
            return null;
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void getMultiWindowParams(int multiWindowId) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookMultiWindowToLarge(int multiWindowMode, int multiWindowId) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookNotifyKeyguardState(int displayId, boolean keyguardShowing, boolean aodShowing) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookReserveMultiWindowNumber(int reserveNum, long showDelayTime) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookShowBlurLayer(SurfaceControl appSurface, String packageName) throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookCancelBlurLayer() throws RemoteException {
        }

        @Override // com.transsion.multiwindow.IMultiWindowServiceV4
        public void hookKeyguardShowing(boolean keyguardShowing) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IMultiWindowServiceV4 {
        static final int TRANSACTION_changeMultiWindowLocation = 1;
        static final int TRANSACTION_getMultiWindowContentRegion = 45;
        static final int TRANSACTION_getMultiWindowParams = 46;
        static final int TRANSACTION_hookActiveMultiWindowEndMove = 29;
        static final int TRANSACTION_hookActiveMultiWindowMove = 28;
        static final int TRANSACTION_hookActiveMultiWindowMoveStart = 26;
        static final int TRANSACTION_hookActiveMultiWindowStartToMove = 27;
        static final int TRANSACTION_hookCancelBlurLayer = 51;
        static final int TRANSACTION_hookCreateTaskAnimation = 36;
        static final int TRANSACTION_hookDefaultDisplayFixRotation = 35;
        static final int TRANSACTION_hookDefaultDisplayRotation = 20;
        static final int TRANSACTION_hookDisplayAreaChildCount = 21;
        static final int TRANSACTION_hookExitSplitScreenToMultiWindow = 11;
        static final int TRANSACTION_hookFinishBoot = 3;
        static final int TRANSACTION_hookFixedRotationLaunch = 25;
        static final int TRANSACTION_hookFocusMultiWindow = 37;
        static final int TRANSACTION_hookFocusOnFloatWindow = 19;
        static final int TRANSACTION_hookGetMultiWindowDefaultRect = 39;
        static final int TRANSACTION_hookGetMultiWindowDefaultRectByTask = 40;
        static final int TRANSACTION_hookGetOrCreateMultiWindow = 38;
        static final int TRANSACTION_hookInputMethodShown = 9;
        static final int TRANSACTION_hookKeyguardShowing = 52;
        static final int TRANSACTION_hookMultiWindowCloseAethen = 34;
        static final int TRANSACTION_hookMultiWindowHideShadow = 31;
        static final int TRANSACTION_hookMultiWindowInvisible = 23;
        static final int TRANSACTION_hookMultiWindowLocation = 18;
        static final int TRANSACTION_hookMultiWindowMuteAethen = 33;
        static final int TRANSACTION_hookMultiWindowStartAethen = 32;
        static final int TRANSACTION_hookMultiWindowToClose = 15;
        static final int TRANSACTION_hookMultiWindowToLarge = 47;
        static final int TRANSACTION_hookMultiWindowToMax = 13;
        static final int TRANSACTION_hookMultiWindowToSmall = 16;
        static final int TRANSACTION_hookMultiWindowToSplit = 17;
        static final int TRANSACTION_hookMultiWindowVisible = 22;
        static final int TRANSACTION_hookNotifyKeyguardState = 48;
        static final int TRANSACTION_hookOnFirstWindowDrawn = 30;
        static final int TRANSACTION_hookReparentToDefaultDisplay = 14;
        static final int TRANSACTION_hookRequestedOrientation = 12;
        static final int TRANSACTION_hookReserveMultiWindowNumber = 49;
        static final int TRANSACTION_hookShowBlurLayer = 50;
        static final int TRANSACTION_hookShowMultiDisplayWindow = 10;
        static final int TRANSACTION_hookStartActivity = 24;
        static final int TRANSACTION_hookStartMultiWindow = 42;
        static final int TRANSACTION_hookStartMultiWindowFromSplitScreen = 41;
        static final int TRANSACTION_hookUpdateRotationFinished = 43;
        static final int TRANSACTION_minimizeMultiWinToEdge = 44;
        static final int TRANSACTION_notifyToUse = 2;
        static final int TRANSACTION_showMultiFailedToast = 5;
        static final int TRANSACTION_showSceneUnSupportMultiToast = 4;
        static final int TRANSACTION_showStabilityMultiToast = 8;
        static final int TRANSACTION_showUnSupportMultiToast = 6;
        static final int TRANSACTION_showUnSupportOtherToast = 7;

        public Stub() {
            attachInterface(this, IMultiWindowServiceV4.DESCRIPTOR);
        }

        public static IMultiWindowServiceV4 asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IMultiWindowServiceV4.DESCRIPTOR);
            if (iin != null && (iin instanceof IMultiWindowServiceV4)) {
                return (IMultiWindowServiceV4) iin;
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
                data.enforceInterface(IMultiWindowServiceV4.DESCRIPTOR);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(IMultiWindowServiceV4.DESCRIPTOR);
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
                            int _arg12 = data.readInt();
                            SurfaceControl _arg2 = (SurfaceControl) data.readTypedObject(SurfaceControl.CREATOR);
                            boolean _arg3 = data.readBoolean();
                            int _arg4 = data.readInt();
                            String _arg5 = data.readString();
                            data.enforceNoDataAvail();
                            hookShowMultiDisplayWindow(_arg05, _arg12, _arg2, _arg3, _arg4, _arg5);
                            break;
                        case 11:
                            int _arg06 = data.readInt();
                            data.enforceNoDataAvail();
                            hookExitSplitScreenToMultiWindow(_arg06);
                            break;
                        case 12:
                            int _arg07 = data.readInt();
                            int _arg13 = data.readInt();
                            int _arg22 = data.readInt();
                            data.enforceNoDataAvail();
                            hookRequestedOrientation(_arg07, _arg13, _arg22);
                            break;
                        case 13:
                            int _arg08 = data.readInt();
                            WindowContainerToken _arg14 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            WindowContainerToken _arg23 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            boolean _arg32 = data.readBoolean();
                            data.enforceNoDataAvail();
                            hookMultiWindowToMax(_arg08, _arg14, _arg23, _arg32);
                            break;
                        case 14:
                            int _arg09 = data.readInt();
                            int _arg15 = data.readInt();
                            WindowContainerToken _arg24 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            WindowContainerToken _arg33 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            boolean _arg42 = data.readBoolean();
                            data.enforceNoDataAvail();
                            hookReparentToDefaultDisplay(_arg09, _arg15, _arg24, _arg33, _arg42);
                            break;
                        case 15:
                            int _arg010 = data.readInt();
                            int _arg16 = data.readInt();
                            data.enforceNoDataAvail();
                            hookMultiWindowToClose(_arg010, _arg16);
                            break;
                        case 16:
                            int _arg011 = data.readInt();
                            int _arg17 = data.readInt();
                            data.enforceNoDataAvail();
                            hookMultiWindowToSmall(_arg011, _arg17);
                            break;
                        case 17:
                            int _arg012 = data.readInt();
                            int _arg18 = data.readInt();
                            data.enforceNoDataAvail();
                            hookMultiWindowToSplit(_arg012, _arg18);
                            break;
                        case 18:
                            int _arg013 = data.readInt();
                            int _arg19 = data.readInt();
                            int _arg25 = data.readInt();
                            int _arg34 = data.readInt();
                            int _arg43 = data.readInt();
                            data.enforceNoDataAvail();
                            hookMultiWindowLocation(_arg013, _arg19, _arg25, _arg34, _arg43);
                            break;
                        case 19:
                            boolean _arg014 = data.readBoolean();
                            data.enforceNoDataAvail();
                            hookFocusOnFloatWindow(_arg014);
                            break;
                        case 20:
                            int _arg015 = data.readInt();
                            data.enforceNoDataAvail();
                            hookDefaultDisplayRotation(_arg015);
                            break;
                        case 21:
                            int _arg016 = data.readInt();
                            int _arg110 = data.readInt();
                            int _arg26 = data.readInt();
                            data.enforceNoDataAvail();
                            hookDisplayAreaChildCount(_arg016, _arg110, _arg26);
                            break;
                        case 22:
                            hookMultiWindowVisible();
                            break;
                        case 23:
                            hookMultiWindowInvisible();
                            break;
                        case 24:
                            Intent _arg017 = (Intent) data.readTypedObject(Intent.CREATOR);
                            int _arg111 = data.readInt();
                            boolean _arg27 = data.readBoolean();
                            data.enforceNoDataAvail();
                            hookStartActivity(_arg017, _arg111, _arg27);
                            break;
                        case 25:
                            int _arg018 = data.readInt();
                            int _arg112 = data.readInt();
                            boolean _arg28 = data.readBoolean();
                            data.enforceNoDataAvail();
                            hookFixedRotationLaunch(_arg018, _arg112, _arg28);
                            break;
                        case 26:
                            int _arg019 = data.readInt();
                            int _arg113 = data.readInt();
                            data.enforceNoDataAvail();
                            hookActiveMultiWindowMoveStart(_arg019, _arg113);
                            break;
                        case 27:
                            IBinder _arg020 = data.readStrongBinder();
                            int _arg114 = data.readInt();
                            int _arg29 = data.readInt();
                            MotionEvent _arg35 = (MotionEvent) data.readTypedObject(MotionEvent.CREATOR);
                            Point _arg44 = (Point) data.readTypedObject(Point.CREATOR);
                            data.enforceNoDataAvail();
                            hookActiveMultiWindowStartToMove(_arg020, _arg114, _arg29, _arg35, _arg44);
                            break;
                        case 28:
                            int _arg021 = data.readInt();
                            int _arg115 = data.readInt();
                            MotionEvent _arg210 = (MotionEvent) data.readTypedObject(MotionEvent.CREATOR);
                            data.enforceNoDataAvail();
                            hookActiveMultiWindowMove(_arg021, _arg115, _arg210);
                            break;
                        case 29:
                            int _arg022 = data.readInt();
                            int _arg116 = data.readInt();
                            MotionEvent _arg211 = (MotionEvent) data.readTypedObject(MotionEvent.CREATOR);
                            data.enforceNoDataAvail();
                            hookActiveMultiWindowEndMove(_arg022, _arg116, _arg211);
                            break;
                        case 30:
                            int _arg023 = data.readInt();
                            int _arg117 = data.readInt();
                            data.enforceNoDataAvail();
                            hookOnFirstWindowDrawn(_arg023, _arg117);
                            break;
                        case 31:
                            hookMultiWindowHideShadow();
                            break;
                        case 32:
                            int _arg024 = data.readInt();
                            String _arg118 = data.readString();
                            data.enforceNoDataAvail();
                            hookMultiWindowStartAethen(_arg024, _arg118);
                            break;
                        case 33:
                            int _arg025 = data.readInt();
                            data.enforceNoDataAvail();
                            hookMultiWindowMuteAethen(_arg025);
                            break;
                        case 34:
                            hookMultiWindowCloseAethen();
                            break;
                        case 35:
                            hookDefaultDisplayFixRotation();
                            break;
                        case 36:
                            IBinder _arg026 = data.readStrongBinder();
                            int _arg119 = data.readInt();
                            MultiTaskRemoteAnimationAdapter _arg212 = (MultiTaskRemoteAnimationAdapter) data.readTypedObject(MultiTaskRemoteAnimationAdapter.CREATOR);
                            data.enforceNoDataAvail();
                            hookCreateTaskAnimation(_arg026, _arg119, _arg212);
                            break;
                        case 37:
                            int _arg027 = data.readInt();
                            int _arg120 = data.readInt();
                            data.enforceNoDataAvail();
                            hookFocusMultiWindow(_arg027, _arg120);
                            break;
                        case 38:
                            String _arg028 = data.readString();
                            int _arg121 = data.readInt();
                            int _arg213 = data.readInt();
                            int _arg36 = data.readInt();
                            boolean _arg45 = data.readBoolean();
                            data.enforceNoDataAvail();
                            hookGetOrCreateMultiWindow(_arg028, _arg121, _arg213, _arg36, _arg45);
                            break;
                        case 39:
                            int _arg029 = data.readInt();
                            data.enforceNoDataAvail();
                            Rect _result = hookGetMultiWindowDefaultRect(_arg029);
                            reply.writeNoException();
                            reply.writeTypedObject(_result, 1);
                            break;
                        case 40:
                            int _arg030 = data.readInt();
                            data.enforceNoDataAvail();
                            hookGetMultiWindowDefaultRectByTask(_arg030);
                            break;
                        case 41:
                            int _arg031 = data.readInt();
                            WindowContainerToken _arg122 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            Rect _arg214 = (Rect) data.readTypedObject(Rect.CREATOR);
                            IWindowContainerTransactionCallbackSync _arg37 = IWindowContainerTransactionCallbackSync.Stub.asInterface(data.readStrongBinder());
                            int _arg46 = data.readInt();
                            data.enforceNoDataAvail();
                            hookStartMultiWindowFromSplitScreen(_arg031, _arg122, _arg214, _arg37, _arg46);
                            break;
                        case 42:
                            WindowContainerToken _arg032 = (WindowContainerToken) data.readTypedObject(WindowContainerToken.CREATOR);
                            String _arg123 = data.readString();
                            Rect _arg215 = (Rect) data.readTypedObject(Rect.CREATOR);
                            IWindowContainerTransactionCallback _arg38 = IWindowContainerTransactionCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            hookStartMultiWindow(_arg032, _arg123, _arg215, _arg38);
                            break;
                        case 43:
                            hookUpdateRotationFinished();
                            break;
                        case 44:
                            int _arg033 = data.readInt();
                            int _arg124 = data.readInt();
                            boolean _arg216 = data.readBoolean();
                            data.enforceNoDataAvail();
                            minimizeMultiWinToEdge(_arg033, _arg124, _arg216);
                            break;
                        case 45:
                            int _arg034 = data.readInt();
                            int _arg125 = data.readInt();
                            data.enforceNoDataAvail();
                            Rect _result2 = getMultiWindowContentRegion(_arg034, _arg125);
                            reply.writeNoException();
                            reply.writeTypedObject(_result2, 1);
                            break;
                        case 46:
                            int _arg035 = data.readInt();
                            data.enforceNoDataAvail();
                            getMultiWindowParams(_arg035);
                            break;
                        case 47:
                            int _arg036 = data.readInt();
                            int _arg126 = data.readInt();
                            data.enforceNoDataAvail();
                            hookMultiWindowToLarge(_arg036, _arg126);
                            break;
                        case 48:
                            int _arg037 = data.readInt();
                            boolean _arg127 = data.readBoolean();
                            boolean _arg217 = data.readBoolean();
                            data.enforceNoDataAvail();
                            hookNotifyKeyguardState(_arg037, _arg127, _arg217);
                            break;
                        case 49:
                            int _arg038 = data.readInt();
                            long _arg128 = data.readLong();
                            data.enforceNoDataAvail();
                            hookReserveMultiWindowNumber(_arg038, _arg128);
                            break;
                        case 50:
                            SurfaceControl _arg039 = (SurfaceControl) data.readTypedObject(SurfaceControl.CREATOR);
                            String _arg129 = data.readString();
                            data.enforceNoDataAvail();
                            hookShowBlurLayer(_arg039, _arg129);
                            break;
                        case 51:
                            hookCancelBlurLayer();
                            break;
                        case 52:
                            boolean _arg040 = data.readBoolean();
                            data.enforceNoDataAvail();
                            hookKeyguardShowing(_arg040);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IMultiWindowServiceV4 {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IMultiWindowServiceV4.DESCRIPTOR;
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void changeMultiWindowLocation(int x, int y) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(x);
                    _data.writeInt(y);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void notifyToUse(String pkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeString(pkgName);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookFinishBoot() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void showSceneUnSupportMultiToast() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void showMultiFailedToast() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void showUnSupportMultiToast() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void showUnSupportOtherToast() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void showStabilityMultiToast(String pkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeString(pkgName);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookInputMethodShown(boolean inputShown) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeBoolean(inputShown);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookShowMultiDisplayWindow(WindowContainerToken taskToken, int taskId, SurfaceControl surfaceControl, boolean canLayoutInShotCutOut, int triggerMode, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeTypedObject(taskToken, 0);
                    _data.writeInt(taskId);
                    _data.writeTypedObject(surfaceControl, 0);
                    _data.writeBoolean(canLayoutInShotCutOut);
                    _data.writeInt(triggerMode);
                    _data.writeString(packageName);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookExitSplitScreenToMultiWindow(int multiWindowMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookRequestedOrientation(int multiWindowMode, int multiWindowId, int orientation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    _data.writeInt(orientation);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookMultiWindowToMax(int displayAreaId, WindowContainerToken displayToken, WindowContainerToken taskToken, boolean canLayoutInShotCutOut) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(displayAreaId);
                    _data.writeTypedObject(displayToken, 0);
                    _data.writeTypedObject(taskToken, 0);
                    _data.writeBoolean(canLayoutInShotCutOut);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookReparentToDefaultDisplay(int multiWindowMode, int multiWindowId, WindowContainerToken displayToken, WindowContainerToken taskToken, boolean canLayoutInShotCutOut) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    _data.writeTypedObject(displayToken, 0);
                    _data.writeTypedObject(taskToken, 0);
                    _data.writeBoolean(canLayoutInShotCutOut);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookMultiWindowToClose(int multiWindowMode, int multiWindowId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookMultiWindowToSmall(int multiWindowMode, int multiWindowId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookMultiWindowToSplit(int multiWindowMode, int multiWindowId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookMultiWindowLocation(int displayAreaId, int x, int y, int touchX, int touchY) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(displayAreaId);
                    _data.writeInt(x);
                    _data.writeInt(y);
                    _data.writeInt(touchX);
                    _data.writeInt(touchY);
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookFocusOnFloatWindow(boolean focusOnFloatWindow) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeBoolean(focusOnFloatWindow);
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookDefaultDisplayRotation(int rotation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(rotation);
                    this.mRemote.transact(20, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookDisplayAreaChildCount(int multiWindowMode, int multiWindowId, int taskCount) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    _data.writeInt(taskCount);
                    this.mRemote.transact(21, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookMultiWindowVisible() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    this.mRemote.transact(22, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookMultiWindowInvisible() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    this.mRemote.transact(23, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookStartActivity(Intent intent, int direction, boolean initiative) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeTypedObject(intent, 0);
                    _data.writeInt(direction);
                    _data.writeBoolean(initiative);
                    this.mRemote.transact(24, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookFixedRotationLaunch(int appRotation, int displayRotation, boolean isEnterMultiWindow) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(appRotation);
                    _data.writeInt(displayRotation);
                    _data.writeBoolean(isEnterMultiWindow);
                    this.mRemote.transact(25, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookActiveMultiWindowMoveStart(int multiWindowMode, int multiWindowId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    this.mRemote.transact(26, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookActiveMultiWindowStartToMove(IBinder token, int multiWindowMode, int multiWindowId, MotionEvent e, Point downPoint) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    _data.writeTypedObject(e, 0);
                    _data.writeTypedObject(downPoint, 0);
                    this.mRemote.transact(27, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookActiveMultiWindowMove(int multiWindowMode, int multiWindowId, MotionEvent e) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    _data.writeTypedObject(e, 0);
                    this.mRemote.transact(28, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookActiveMultiWindowEndMove(int multiWindowMode, int multiWindowId, MotionEvent e) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    _data.writeTypedObject(e, 0);
                    this.mRemote.transact(29, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookOnFirstWindowDrawn(int multiWindowMode, int multiWindowId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    this.mRemote.transact(30, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookMultiWindowHideShadow() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    this.mRemote.transact(31, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookMultiWindowStartAethen(int way, String pkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(way);
                    _data.writeString(pkgName);
                    this.mRemote.transact(32, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookMultiWindowMuteAethen(int type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(type);
                    this.mRemote.transact(33, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookMultiWindowCloseAethen() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    this.mRemote.transact(34, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookDefaultDisplayFixRotation() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    this.mRemote.transact(35, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookCreateTaskAnimation(IBinder token, int taskId, MultiTaskRemoteAnimationAdapter callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(taskId);
                    _data.writeTypedObject(callback, 0);
                    this.mRemote.transact(36, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookFocusMultiWindow(int multiWindowMode, int multiWindowId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    this.mRemote.transact(37, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookGetOrCreateMultiWindow(String pkgName, int direction, int triggerMode, int preferId, boolean needCreate) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeInt(direction);
                    _data.writeInt(triggerMode);
                    _data.writeInt(preferId);
                    _data.writeBoolean(needCreate);
                    this.mRemote.transact(38, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public Rect hookGetMultiWindowDefaultRect(int orientationType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(orientationType);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                    Rect _result = (Rect) _reply.readTypedObject(Rect.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookGetMultiWindowDefaultRectByTask(int taskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(taskId);
                    this.mRemote.transact(40, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookStartMultiWindowFromSplitScreen(int fullTaskId, WindowContainerToken multiTaskToken, Rect multiWindowRegion, IWindowContainerTransactionCallbackSync syncCallback, int type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(fullTaskId);
                    _data.writeTypedObject(multiTaskToken, 0);
                    _data.writeTypedObject(multiWindowRegion, 0);
                    _data.writeStrongInterface(syncCallback);
                    _data.writeInt(type);
                    this.mRemote.transact(41, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookStartMultiWindow(WindowContainerToken taskToken, String packageName, Rect multiWindowRegion, IWindowContainerTransactionCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeTypedObject(taskToken, 0);
                    _data.writeString(packageName);
                    _data.writeTypedObject(multiWindowRegion, 0);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(42, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookUpdateRotationFinished() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    this.mRemote.transact(43, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void minimizeMultiWinToEdge(int multiWindowMode, int multiWindowId, boolean toEdge) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    _data.writeBoolean(toEdge);
                    this.mRemote.transact(44, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public Rect getMultiWindowContentRegion(int multiWindowMode, int multiWindowId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    this.mRemote.transact(45, _data, _reply, 0);
                    _reply.readException();
                    Rect _result = (Rect) _reply.readTypedObject(Rect.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void getMultiWindowParams(int multiWindowId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(multiWindowId);
                    this.mRemote.transact(46, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookMultiWindowToLarge(int multiWindowMode, int multiWindowId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(multiWindowMode);
                    _data.writeInt(multiWindowId);
                    this.mRemote.transact(47, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookNotifyKeyguardState(int displayId, boolean keyguardShowing, boolean aodShowing) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeBoolean(keyguardShowing);
                    _data.writeBoolean(aodShowing);
                    this.mRemote.transact(48, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookReserveMultiWindowNumber(int reserveNum, long showDelayTime) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeInt(reserveNum);
                    _data.writeLong(showDelayTime);
                    this.mRemote.transact(49, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookShowBlurLayer(SurfaceControl appSurface, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeTypedObject(appSurface, 0);
                    _data.writeString(packageName);
                    this.mRemote.transact(50, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookCancelBlurLayer() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    this.mRemote.transact(51, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.transsion.multiwindow.IMultiWindowServiceV4
            public void hookKeyguardShowing(boolean keyguardShowing) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMultiWindowServiceV4.DESCRIPTOR);
                    _data.writeBoolean(keyguardShowing);
                    this.mRemote.transact(52, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
