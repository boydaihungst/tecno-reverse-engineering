package android.view;

import android.content.ClipData;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.util.MergedConfiguration;
import android.view.IWindow;
import android.view.IWindowId;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.window.ClientWindowFrames;
import android.window.OnBackInvokedCallbackInfo;
import java.util.List;
/* loaded from: classes3.dex */
public interface IWindowSession extends IInterface {
    int addToDisplay(IWindow iWindow, WindowManager.LayoutParams layoutParams, int i, int i2, InsetsVisibilities insetsVisibilities, InputChannel inputChannel, InsetsState insetsState, InsetsSourceControl[] insetsSourceControlArr) throws RemoteException;

    int addToDisplayAsUser(IWindow iWindow, WindowManager.LayoutParams layoutParams, int i, int i2, int i3, InsetsVisibilities insetsVisibilities, InputChannel inputChannel, InsetsState insetsState, InsetsSourceControl[] insetsSourceControlArr) throws RemoteException;

    int addToDisplayWithoutInputChannel(IWindow iWindow, WindowManager.LayoutParams layoutParams, int i, int i2, InsetsState insetsState) throws RemoteException;

    void cancelDragAndDrop(IBinder iBinder, boolean z) throws RemoteException;

    void clearTouchableRegion(IWindow iWindow) throws RemoteException;

    void dragRecipientEntered(IWindow iWindow) throws RemoteException;

    void dragRecipientExited(IWindow iWindow) throws RemoteException;

    boolean dropForAccessibility(IWindow iWindow, int i, int i2) throws RemoteException;

    void finishDrawing(IWindow iWindow, SurfaceControl.Transaction transaction, int i) throws RemoteException;

    void finishMovingTask(IWindow iWindow) throws RemoteException;

    void generateDisplayHash(IWindow iWindow, Rect rect, String str, RemoteCallback remoteCallback) throws RemoteException;

    boolean getInTouchMode() throws RemoteException;

    IWindowId getWindowId(IBinder iBinder) throws RemoteException;

    void grantEmbeddedWindowFocus(IWindow iWindow, IBinder iBinder, boolean z) throws RemoteException;

    void grantInputChannel(int i, SurfaceControl surfaceControl, IWindow iWindow, IBinder iBinder, int i2, int i3, int i4, IBinder iBinder2, String str, InputChannel inputChannel) throws RemoteException;

    void onRectangleOnScreenRequested(IBinder iBinder, Rect rect) throws RemoteException;

    boolean outOfMemory(IWindow iWindow) throws RemoteException;

    IBinder performDrag(IWindow iWindow, int i, SurfaceControl surfaceControl, int i2, float f, float f2, float f3, float f4, ClipData clipData) throws RemoteException;

    boolean performHapticFeedback(int i, boolean z) throws RemoteException;

    void pokeDrawLock(IBinder iBinder) throws RemoteException;

    void prepareToReplaceWindows(IBinder iBinder, boolean z) throws RemoteException;

    int relayout(IWindow iWindow, WindowManager.LayoutParams layoutParams, int i, int i2, int i3, int i4, ClientWindowFrames clientWindowFrames, MergedConfiguration mergedConfiguration, SurfaceControl surfaceControl, InsetsState insetsState, InsetsSourceControl[] insetsSourceControlArr, Bundle bundle) throws RemoteException;

    void remove(IWindow iWindow) throws RemoteException;

    void reportDropResult(IWindow iWindow, boolean z) throws RemoteException;

    void reportKeepClearAreasChanged(IWindow iWindow, List<Rect> list, List<Rect> list2) throws RemoteException;

    void reportSystemGestureExclusionChanged(IWindow iWindow, List<Rect> list) throws RemoteException;

    Bundle sendWallpaperCommand(IBinder iBinder, String str, int i, int i2, int i3, Bundle bundle, boolean z) throws RemoteException;

    void setInTouchMode(boolean z) throws RemoteException;

    void setInsets(IWindow iWindow, int i, Rect rect, Rect rect2, Region region) throws RemoteException;

    void setOnBackInvokedCallbackInfo(IWindow iWindow, OnBackInvokedCallbackInfo onBackInvokedCallbackInfo) throws RemoteException;

    void setRefreshRate(SurfaceControl surfaceControl, float f, int i, int i2, String str, String str2) throws RemoteException;

    void setShouldZoomOutWallpaper(IBinder iBinder, boolean z) throws RemoteException;

    void setWallpaperDisplayOffset(IBinder iBinder, int i, int i2) throws RemoteException;

    void setWallpaperPosition(IBinder iBinder, float f, float f2, float f3, float f4) throws RemoteException;

    void setWallpaperZoomOut(IBinder iBinder, float f) throws RemoteException;

    boolean startMovingTask(IWindow iWindow, float f, float f2) throws RemoteException;

    void updateInputChannel(IBinder iBinder, int i, SurfaceControl surfaceControl, int i2, int i3, Region region) throws RemoteException;

    void updateLayout(IWindow iWindow, WindowManager.LayoutParams layoutParams, int i, ClientWindowFrames clientWindowFrames, int i2, int i3) throws RemoteException;

    void updatePointerIcon(IWindow iWindow) throws RemoteException;

    void updateRequestedVisibilities(IWindow iWindow, InsetsVisibilities insetsVisibilities) throws RemoteException;

    void updateTapExcludeRegion(IWindow iWindow, Region region) throws RemoteException;

    int updateVisibility(IWindow iWindow, WindowManager.LayoutParams layoutParams, int i, MergedConfiguration mergedConfiguration, SurfaceControl surfaceControl, InsetsState insetsState, InsetsSourceControl[] insetsSourceControlArr) throws RemoteException;

    void wallpaperCommandComplete(IBinder iBinder, Bundle bundle) throws RemoteException;

    void wallpaperOffsetsComplete(IBinder iBinder) throws RemoteException;

    /* loaded from: classes3.dex */
    public static class Default implements IWindowSession {
        @Override // android.view.IWindowSession
        public int addToDisplay(IWindow window, WindowManager.LayoutParams attrs, int viewVisibility, int layerStackId, InsetsVisibilities requestedVisibilities, InputChannel outInputChannel, InsetsState insetsState, InsetsSourceControl[] activeControls) throws RemoteException {
            return 0;
        }

        @Override // android.view.IWindowSession
        public int addToDisplayAsUser(IWindow window, WindowManager.LayoutParams attrs, int viewVisibility, int layerStackId, int userId, InsetsVisibilities requestedVisibilities, InputChannel outInputChannel, InsetsState insetsState, InsetsSourceControl[] activeControls) throws RemoteException {
            return 0;
        }

        @Override // android.view.IWindowSession
        public int addToDisplayWithoutInputChannel(IWindow window, WindowManager.LayoutParams attrs, int viewVisibility, int layerStackId, InsetsState insetsState) throws RemoteException {
            return 0;
        }

        @Override // android.view.IWindowSession
        public void remove(IWindow window) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public int relayout(IWindow window, WindowManager.LayoutParams attrs, int requestedWidth, int requestedHeight, int viewVisibility, int flags, ClientWindowFrames outFrames, MergedConfiguration outMergedConfiguration, SurfaceControl outSurfaceControl, InsetsState insetsState, InsetsSourceControl[] activeControls, Bundle bundle) throws RemoteException {
            return 0;
        }

        @Override // android.view.IWindowSession
        public int updateVisibility(IWindow window, WindowManager.LayoutParams attrs, int viewVisibility, MergedConfiguration outMergedConfiguration, SurfaceControl outSurfaceControl, InsetsState outInsetsState, InsetsSourceControl[] outActiveControls) throws RemoteException {
            return 0;
        }

        @Override // android.view.IWindowSession
        public void updateLayout(IWindow window, WindowManager.LayoutParams attrs, int flags, ClientWindowFrames clientFrames, int requestedWidth, int requestedHeight) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void prepareToReplaceWindows(IBinder appToken, boolean childrenOnly) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public boolean outOfMemory(IWindow window) throws RemoteException {
            return false;
        }

        @Override // android.view.IWindowSession
        public void setInsets(IWindow window, int touchableInsets, Rect contentInsets, Rect visibleInsets, Region touchableRegion) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void finishDrawing(IWindow window, SurfaceControl.Transaction postDrawTransaction, int seqId) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void setInTouchMode(boolean showFocus) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public boolean getInTouchMode() throws RemoteException {
            return false;
        }

        @Override // android.view.IWindowSession
        public boolean performHapticFeedback(int effectId, boolean always) throws RemoteException {
            return false;
        }

        @Override // android.view.IWindowSession
        public IBinder performDrag(IWindow window, int flags, SurfaceControl surface, int touchSource, float touchX, float touchY, float thumbCenterX, float thumbCenterY, ClipData data) throws RemoteException {
            return null;
        }

        @Override // android.view.IWindowSession
        public boolean dropForAccessibility(IWindow window, int x, int y) throws RemoteException {
            return false;
        }

        @Override // android.view.IWindowSession
        public void reportDropResult(IWindow window, boolean consumed) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void cancelDragAndDrop(IBinder dragToken, boolean skipAnimation) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void dragRecipientEntered(IWindow window) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void dragRecipientExited(IWindow window) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void setWallpaperPosition(IBinder windowToken, float x, float y, float xstep, float ystep) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void setWallpaperZoomOut(IBinder windowToken, float scale) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void setShouldZoomOutWallpaper(IBinder windowToken, boolean shouldZoom) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void wallpaperOffsetsComplete(IBinder window) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void setWallpaperDisplayOffset(IBinder windowToken, int x, int y) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public Bundle sendWallpaperCommand(IBinder window, String action, int x, int y, int z, Bundle extras, boolean sync) throws RemoteException {
            return null;
        }

        @Override // android.view.IWindowSession
        public void wallpaperCommandComplete(IBinder window, Bundle result) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void onRectangleOnScreenRequested(IBinder token, Rect rectangle) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public IWindowId getWindowId(IBinder window) throws RemoteException {
            return null;
        }

        @Override // android.view.IWindowSession
        public void pokeDrawLock(IBinder window) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public boolean startMovingTask(IWindow window, float startX, float startY) throws RemoteException {
            return false;
        }

        @Override // android.view.IWindowSession
        public void finishMovingTask(IWindow window) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void updatePointerIcon(IWindow window) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void updateTapExcludeRegion(IWindow window, Region region) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void updateRequestedVisibilities(IWindow window, InsetsVisibilities visibilities) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void reportSystemGestureExclusionChanged(IWindow window, List<Rect> exclusionRects) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void reportKeepClearAreasChanged(IWindow window, List<Rect> restricted, List<Rect> unrestricted) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void grantInputChannel(int displayId, SurfaceControl surface, IWindow window, IBinder hostInputToken, int flags, int privateFlags, int type, IBinder focusGrantToken, String inputHandleName, InputChannel outInputChannel) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void updateInputChannel(IBinder channelToken, int displayId, SurfaceControl surface, int flags, int privateFlags, Region region) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void grantEmbeddedWindowFocus(IWindow window, IBinder inputToken, boolean grantFocus) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void generateDisplayHash(IWindow window, Rect boundsInWindow, String hashAlgorithm, RemoteCallback callback) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void setOnBackInvokedCallbackInfo(IWindow window, OnBackInvokedCallbackInfo callbackInfo) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void clearTouchableRegion(IWindow window) throws RemoteException {
        }

        @Override // android.view.IWindowSession
        public void setRefreshRate(SurfaceControl sc, float refreshRate, int mMSyncScenarioAction, int mMSyncScenarioType, String activityName, String packgeName) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes3.dex */
    public static abstract class Stub extends Binder implements IWindowSession {
        public static final String DESCRIPTOR = "android.view.IWindowSession";
        static final int TRANSACTION_addToDisplay = 1;
        static final int TRANSACTION_addToDisplayAsUser = 2;
        static final int TRANSACTION_addToDisplayWithoutInputChannel = 3;
        static final int TRANSACTION_cancelDragAndDrop = 18;
        static final int TRANSACTION_clearTouchableRegion = 43;
        static final int TRANSACTION_dragRecipientEntered = 19;
        static final int TRANSACTION_dragRecipientExited = 20;
        static final int TRANSACTION_dropForAccessibility = 16;
        static final int TRANSACTION_finishDrawing = 11;
        static final int TRANSACTION_finishMovingTask = 32;
        static final int TRANSACTION_generateDisplayHash = 41;
        static final int TRANSACTION_getInTouchMode = 13;
        static final int TRANSACTION_getWindowId = 29;
        static final int TRANSACTION_grantEmbeddedWindowFocus = 40;
        static final int TRANSACTION_grantInputChannel = 38;
        static final int TRANSACTION_onRectangleOnScreenRequested = 28;
        static final int TRANSACTION_outOfMemory = 9;
        static final int TRANSACTION_performDrag = 15;
        static final int TRANSACTION_performHapticFeedback = 14;
        static final int TRANSACTION_pokeDrawLock = 30;
        static final int TRANSACTION_prepareToReplaceWindows = 8;
        static final int TRANSACTION_relayout = 5;
        static final int TRANSACTION_remove = 4;
        static final int TRANSACTION_reportDropResult = 17;
        static final int TRANSACTION_reportKeepClearAreasChanged = 37;
        static final int TRANSACTION_reportSystemGestureExclusionChanged = 36;
        static final int TRANSACTION_sendWallpaperCommand = 26;
        static final int TRANSACTION_setInTouchMode = 12;
        static final int TRANSACTION_setInsets = 10;
        static final int TRANSACTION_setOnBackInvokedCallbackInfo = 42;
        static final int TRANSACTION_setRefreshRate = 44;
        static final int TRANSACTION_setShouldZoomOutWallpaper = 23;
        static final int TRANSACTION_setWallpaperDisplayOffset = 25;
        static final int TRANSACTION_setWallpaperPosition = 21;
        static final int TRANSACTION_setWallpaperZoomOut = 22;
        static final int TRANSACTION_startMovingTask = 31;
        static final int TRANSACTION_updateInputChannel = 39;
        static final int TRANSACTION_updateLayout = 7;
        static final int TRANSACTION_updatePointerIcon = 33;
        static final int TRANSACTION_updateRequestedVisibilities = 35;
        static final int TRANSACTION_updateTapExcludeRegion = 34;
        static final int TRANSACTION_updateVisibility = 6;
        static final int TRANSACTION_wallpaperCommandComplete = 27;
        static final int TRANSACTION_wallpaperOffsetsComplete = 24;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IWindowSession asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IWindowSession)) {
                return (IWindowSession) iin;
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
                    return "addToDisplay";
                case 2:
                    return "addToDisplayAsUser";
                case 3:
                    return "addToDisplayWithoutInputChannel";
                case 4:
                    return "remove";
                case 5:
                    return "relayout";
                case 6:
                    return "updateVisibility";
                case 7:
                    return "updateLayout";
                case 8:
                    return "prepareToReplaceWindows";
                case 9:
                    return "outOfMemory";
                case 10:
                    return "setInsets";
                case 11:
                    return "finishDrawing";
                case 12:
                    return "setInTouchMode";
                case 13:
                    return "getInTouchMode";
                case 14:
                    return "performHapticFeedback";
                case 15:
                    return "performDrag";
                case 16:
                    return "dropForAccessibility";
                case 17:
                    return "reportDropResult";
                case 18:
                    return "cancelDragAndDrop";
                case 19:
                    return "dragRecipientEntered";
                case 20:
                    return "dragRecipientExited";
                case 21:
                    return "setWallpaperPosition";
                case 22:
                    return "setWallpaperZoomOut";
                case 23:
                    return "setShouldZoomOutWallpaper";
                case 24:
                    return "wallpaperOffsetsComplete";
                case 25:
                    return "setWallpaperDisplayOffset";
                case 26:
                    return "sendWallpaperCommand";
                case 27:
                    return "wallpaperCommandComplete";
                case 28:
                    return "onRectangleOnScreenRequested";
                case 29:
                    return "getWindowId";
                case 30:
                    return "pokeDrawLock";
                case 31:
                    return "startMovingTask";
                case 32:
                    return "finishMovingTask";
                case 33:
                    return "updatePointerIcon";
                case 34:
                    return "updateTapExcludeRegion";
                case 35:
                    return "updateRequestedVisibilities";
                case 36:
                    return "reportSystemGestureExclusionChanged";
                case 37:
                    return "reportKeepClearAreasChanged";
                case 38:
                    return "grantInputChannel";
                case 39:
                    return "updateInputChannel";
                case 40:
                    return "grantEmbeddedWindowFocus";
                case 41:
                    return "generateDisplayHash";
                case 42:
                    return "setOnBackInvokedCallbackInfo";
                case 43:
                    return "clearTouchableRegion";
                case 44:
                    return "setRefreshRate";
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
            InsetsSourceControl[] _arg7;
            InsetsSourceControl[] _arg8;
            InsetsSourceControl[] _arg10;
            InsetsSourceControl[] _arg6;
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            IWindow _arg0 = IWindow.Stub.asInterface(data.readStrongBinder());
                            WindowManager.LayoutParams _arg1 = (WindowManager.LayoutParams) data.readTypedObject(WindowManager.LayoutParams.CREATOR);
                            int _arg2 = data.readInt();
                            int _arg3 = data.readInt();
                            InsetsVisibilities _arg4 = (InsetsVisibilities) data.readTypedObject(InsetsVisibilities.CREATOR);
                            InputChannel _arg5 = new InputChannel();
                            InsetsState _arg62 = new InsetsState();
                            int _arg7_length = data.readInt();
                            if (_arg7_length < 0) {
                                _arg7 = null;
                            } else {
                                InsetsSourceControl[] _arg72 = new InsetsSourceControl[_arg7_length];
                                _arg7 = _arg72;
                            }
                            data.enforceNoDataAvail();
                            InsetsSourceControl[] _arg73 = _arg7;
                            int _result = addToDisplay(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg62, _arg73);
                            reply.writeNoException();
                            reply.writeInt(_result);
                            reply.writeTypedObject(_arg5, 1);
                            reply.writeTypedObject(_arg62, 1);
                            reply.writeTypedArray(_arg73, 1);
                            return true;
                        case 2:
                            IWindow _arg02 = IWindow.Stub.asInterface(data.readStrongBinder());
                            WindowManager.LayoutParams _arg12 = (WindowManager.LayoutParams) data.readTypedObject(WindowManager.LayoutParams.CREATOR);
                            int _arg22 = data.readInt();
                            int _arg32 = data.readInt();
                            int _arg42 = data.readInt();
                            InsetsVisibilities _arg52 = (InsetsVisibilities) data.readTypedObject(InsetsVisibilities.CREATOR);
                            InputChannel _arg63 = new InputChannel();
                            InsetsState _arg74 = new InsetsState();
                            int _arg8_length = data.readInt();
                            if (_arg8_length < 0) {
                                _arg8 = null;
                            } else {
                                InsetsSourceControl[] _arg82 = new InsetsSourceControl[_arg8_length];
                                _arg8 = _arg82;
                            }
                            data.enforceNoDataAvail();
                            InsetsSourceControl[] _arg83 = _arg8;
                            int _result2 = addToDisplayAsUser(_arg02, _arg12, _arg22, _arg32, _arg42, _arg52, _arg63, _arg74, _arg83);
                            reply.writeNoException();
                            reply.writeInt(_result2);
                            reply.writeTypedObject(_arg63, 1);
                            reply.writeTypedObject(_arg74, 1);
                            reply.writeTypedArray(_arg83, 1);
                            return true;
                        case 3:
                            IWindow _arg03 = IWindow.Stub.asInterface(data.readStrongBinder());
                            WindowManager.LayoutParams _arg13 = (WindowManager.LayoutParams) data.readTypedObject(WindowManager.LayoutParams.CREATOR);
                            int _arg23 = data.readInt();
                            int _arg33 = data.readInt();
                            InsetsState _arg43 = new InsetsState();
                            data.enforceNoDataAvail();
                            int _result3 = addToDisplayWithoutInputChannel(_arg03, _arg13, _arg23, _arg33, _arg43);
                            reply.writeNoException();
                            reply.writeInt(_result3);
                            reply.writeTypedObject(_arg43, 1);
                            return true;
                        case 4:
                            IWindow _arg04 = IWindow.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            remove(_arg04);
                            reply.writeNoException();
                            return true;
                        case 5:
                            IWindow _arg05 = IWindow.Stub.asInterface(data.readStrongBinder());
                            WindowManager.LayoutParams _arg14 = (WindowManager.LayoutParams) data.readTypedObject(WindowManager.LayoutParams.CREATOR);
                            int _arg24 = data.readInt();
                            int _arg34 = data.readInt();
                            int _arg44 = data.readInt();
                            int _arg53 = data.readInt();
                            ClientWindowFrames _arg64 = new ClientWindowFrames();
                            MergedConfiguration _arg75 = new MergedConfiguration();
                            SurfaceControl _arg84 = new SurfaceControl();
                            InsetsState _arg9 = new InsetsState();
                            int _arg10_length = data.readInt();
                            if (_arg10_length < 0) {
                                _arg10 = null;
                            } else {
                                InsetsSourceControl[] _arg102 = new InsetsSourceControl[_arg10_length];
                                _arg10 = _arg102;
                            }
                            Bundle _arg11 = new Bundle();
                            data.enforceNoDataAvail();
                            InsetsSourceControl[] _arg103 = _arg10;
                            int _result4 = relayout(_arg05, _arg14, _arg24, _arg34, _arg44, _arg53, _arg64, _arg75, _arg84, _arg9, _arg103, _arg11);
                            reply.writeNoException();
                            reply.writeInt(_result4);
                            reply.writeTypedObject(_arg64, 1);
                            reply.writeTypedObject(_arg75, 1);
                            reply.writeTypedObject(_arg84, 1);
                            reply.writeTypedObject(_arg9, 1);
                            reply.writeTypedArray(_arg103, 1);
                            reply.writeTypedObject(_arg11, 1);
                            return true;
                        case 6:
                            IWindow _arg06 = IWindow.Stub.asInterface(data.readStrongBinder());
                            WindowManager.LayoutParams _arg15 = (WindowManager.LayoutParams) data.readTypedObject(WindowManager.LayoutParams.CREATOR);
                            int _arg25 = data.readInt();
                            MergedConfiguration _arg35 = new MergedConfiguration();
                            SurfaceControl _arg45 = new SurfaceControl();
                            InsetsState _arg54 = new InsetsState();
                            int _arg6_length = data.readInt();
                            if (_arg6_length < 0) {
                                _arg6 = null;
                            } else {
                                _arg6 = new InsetsSourceControl[_arg6_length];
                            }
                            data.enforceNoDataAvail();
                            InsetsSourceControl[] _arg65 = _arg6;
                            int _result5 = updateVisibility(_arg06, _arg15, _arg25, _arg35, _arg45, _arg54, _arg65);
                            reply.writeNoException();
                            reply.writeInt(_result5);
                            reply.writeTypedObject(_arg35, 1);
                            reply.writeTypedObject(_arg45, 1);
                            reply.writeTypedObject(_arg54, 1);
                            reply.writeTypedArray(_arg65, 1);
                            return true;
                        case 7:
                            IWindow _arg07 = IWindow.Stub.asInterface(data.readStrongBinder());
                            WindowManager.LayoutParams _arg16 = (WindowManager.LayoutParams) data.readTypedObject(WindowManager.LayoutParams.CREATOR);
                            int _arg26 = data.readInt();
                            ClientWindowFrames _arg36 = (ClientWindowFrames) data.readTypedObject(ClientWindowFrames.CREATOR);
                            int _arg46 = data.readInt();
                            int _arg55 = data.readInt();
                            data.enforceNoDataAvail();
                            updateLayout(_arg07, _arg16, _arg26, _arg36, _arg46, _arg55);
                            return true;
                        case 8:
                            IBinder _arg08 = data.readStrongBinder();
                            boolean _arg17 = data.readBoolean();
                            data.enforceNoDataAvail();
                            prepareToReplaceWindows(_arg08, _arg17);
                            return true;
                        case 9:
                            IWindow _arg09 = IWindow.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            boolean _result6 = outOfMemory(_arg09);
                            reply.writeNoException();
                            reply.writeBoolean(_result6);
                            return true;
                        case 10:
                            IWindow _arg010 = IWindow.Stub.asInterface(data.readStrongBinder());
                            int _arg18 = data.readInt();
                            Rect _arg27 = (Rect) data.readTypedObject(Rect.CREATOR);
                            Rect _arg37 = (Rect) data.readTypedObject(Rect.CREATOR);
                            Region _arg47 = (Region) data.readTypedObject(Region.CREATOR);
                            data.enforceNoDataAvail();
                            setInsets(_arg010, _arg18, _arg27, _arg37, _arg47);
                            return true;
                        case 11:
                            IWindow _arg011 = IWindow.Stub.asInterface(data.readStrongBinder());
                            SurfaceControl.Transaction _arg19 = (SurfaceControl.Transaction) data.readTypedObject(SurfaceControl.Transaction.CREATOR);
                            int _arg28 = data.readInt();
                            data.enforceNoDataAvail();
                            finishDrawing(_arg011, _arg19, _arg28);
                            return true;
                        case 12:
                            boolean _arg012 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setInTouchMode(_arg012);
                            return true;
                        case 13:
                            boolean _result7 = getInTouchMode();
                            reply.writeNoException();
                            reply.writeBoolean(_result7);
                            return true;
                        case 14:
                            int _arg013 = data.readInt();
                            boolean _arg110 = data.readBoolean();
                            data.enforceNoDataAvail();
                            boolean _result8 = performHapticFeedback(_arg013, _arg110);
                            reply.writeNoException();
                            reply.writeBoolean(_result8);
                            return true;
                        case 15:
                            IWindow _arg014 = IWindow.Stub.asInterface(data.readStrongBinder());
                            int _arg111 = data.readInt();
                            SurfaceControl _arg29 = (SurfaceControl) data.readTypedObject(SurfaceControl.CREATOR);
                            int _arg38 = data.readInt();
                            float _arg48 = data.readFloat();
                            float _arg56 = data.readFloat();
                            float _arg66 = data.readFloat();
                            float _arg76 = data.readFloat();
                            ClipData _arg85 = (ClipData) data.readTypedObject(ClipData.CREATOR);
                            data.enforceNoDataAvail();
                            IBinder _result9 = performDrag(_arg014, _arg111, _arg29, _arg38, _arg48, _arg56, _arg66, _arg76, _arg85);
                            reply.writeNoException();
                            reply.writeStrongBinder(_result9);
                            return true;
                        case 16:
                            IWindow _arg015 = IWindow.Stub.asInterface(data.readStrongBinder());
                            int _arg112 = data.readInt();
                            int _arg210 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result10 = dropForAccessibility(_arg015, _arg112, _arg210);
                            reply.writeNoException();
                            reply.writeBoolean(_result10);
                            return true;
                        case 17:
                            IWindow _arg016 = IWindow.Stub.asInterface(data.readStrongBinder());
                            boolean _arg113 = data.readBoolean();
                            data.enforceNoDataAvail();
                            reportDropResult(_arg016, _arg113);
                            return true;
                        case 18:
                            IBinder _arg017 = data.readStrongBinder();
                            boolean _arg114 = data.readBoolean();
                            data.enforceNoDataAvail();
                            cancelDragAndDrop(_arg017, _arg114);
                            return true;
                        case 19:
                            IWindow _arg018 = IWindow.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            dragRecipientEntered(_arg018);
                            return true;
                        case 20:
                            IWindow _arg019 = IWindow.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            dragRecipientExited(_arg019);
                            return true;
                        case 21:
                            IBinder _arg020 = data.readStrongBinder();
                            float _arg115 = data.readFloat();
                            float _arg211 = data.readFloat();
                            float _arg39 = data.readFloat();
                            float _arg49 = data.readFloat();
                            data.enforceNoDataAvail();
                            setWallpaperPosition(_arg020, _arg115, _arg211, _arg39, _arg49);
                            return true;
                        case 22:
                            IBinder _arg021 = data.readStrongBinder();
                            float _arg116 = data.readFloat();
                            data.enforceNoDataAvail();
                            setWallpaperZoomOut(_arg021, _arg116);
                            return true;
                        case 23:
                            IBinder _arg022 = data.readStrongBinder();
                            boolean _arg117 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setShouldZoomOutWallpaper(_arg022, _arg117);
                            return true;
                        case 24:
                            IBinder _arg023 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            wallpaperOffsetsComplete(_arg023);
                            return true;
                        case 25:
                            IBinder _arg024 = data.readStrongBinder();
                            int _arg118 = data.readInt();
                            int _arg212 = data.readInt();
                            data.enforceNoDataAvail();
                            setWallpaperDisplayOffset(_arg024, _arg118, _arg212);
                            return true;
                        case 26:
                            IBinder _arg025 = data.readStrongBinder();
                            String _arg119 = data.readString();
                            int _arg213 = data.readInt();
                            int _arg310 = data.readInt();
                            int _arg410 = data.readInt();
                            boolean _arg67 = data.readBoolean();
                            data.enforceNoDataAvail();
                            Bundle _result11 = sendWallpaperCommand(_arg025, _arg119, _arg213, _arg310, _arg410, (Bundle) data.readTypedObject(Bundle.CREATOR), _arg67);
                            reply.writeNoException();
                            reply.writeTypedObject(_result11, 1);
                            return true;
                        case 27:
                            IBinder _arg026 = data.readStrongBinder();
                            Bundle _arg120 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            wallpaperCommandComplete(_arg026, _arg120);
                            return true;
                        case 28:
                            IBinder _arg027 = data.readStrongBinder();
                            Rect _arg121 = (Rect) data.readTypedObject(Rect.CREATOR);
                            data.enforceNoDataAvail();
                            onRectangleOnScreenRequested(_arg027, _arg121);
                            return true;
                        case 29:
                            IBinder _arg028 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            IWindowId _result12 = getWindowId(_arg028);
                            reply.writeNoException();
                            reply.writeStrongInterface(_result12);
                            return true;
                        case 30:
                            IBinder _arg029 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            pokeDrawLock(_arg029);
                            reply.writeNoException();
                            return true;
                        case 31:
                            IWindow _arg030 = IWindow.Stub.asInterface(data.readStrongBinder());
                            float _arg122 = data.readFloat();
                            float _arg214 = data.readFloat();
                            data.enforceNoDataAvail();
                            boolean _result13 = startMovingTask(_arg030, _arg122, _arg214);
                            reply.writeNoException();
                            reply.writeBoolean(_result13);
                            return true;
                        case 32:
                            IWindow _arg031 = IWindow.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            finishMovingTask(_arg031);
                            return true;
                        case 33:
                            IWindow _arg032 = IWindow.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            updatePointerIcon(_arg032);
                            return true;
                        case 34:
                            IWindow _arg033 = IWindow.Stub.asInterface(data.readStrongBinder());
                            Region _arg123 = (Region) data.readTypedObject(Region.CREATOR);
                            data.enforceNoDataAvail();
                            updateTapExcludeRegion(_arg033, _arg123);
                            return true;
                        case 35:
                            IWindow _arg034 = IWindow.Stub.asInterface(data.readStrongBinder());
                            InsetsVisibilities _arg124 = (InsetsVisibilities) data.readTypedObject(InsetsVisibilities.CREATOR);
                            data.enforceNoDataAvail();
                            updateRequestedVisibilities(_arg034, _arg124);
                            return true;
                        case 36:
                            IWindow _arg035 = IWindow.Stub.asInterface(data.readStrongBinder());
                            List<Rect> _arg125 = data.createTypedArrayList(Rect.CREATOR);
                            data.enforceNoDataAvail();
                            reportSystemGestureExclusionChanged(_arg035, _arg125);
                            return true;
                        case 37:
                            IWindow _arg036 = IWindow.Stub.asInterface(data.readStrongBinder());
                            List<Rect> _arg126 = data.createTypedArrayList(Rect.CREATOR);
                            List<Rect> _arg215 = data.createTypedArrayList(Rect.CREATOR);
                            data.enforceNoDataAvail();
                            reportKeepClearAreasChanged(_arg036, _arg126, _arg215);
                            return true;
                        case 38:
                            int _arg037 = data.readInt();
                            SurfaceControl _arg127 = (SurfaceControl) data.readTypedObject(SurfaceControl.CREATOR);
                            IWindow _arg216 = IWindow.Stub.asInterface(data.readStrongBinder());
                            IBinder _arg311 = data.readStrongBinder();
                            int _arg411 = data.readInt();
                            int _arg57 = data.readInt();
                            int _arg68 = data.readInt();
                            IBinder _arg77 = data.readStrongBinder();
                            String _arg86 = data.readString();
                            InputChannel _arg92 = new InputChannel();
                            data.enforceNoDataAvail();
                            grantInputChannel(_arg037, _arg127, _arg216, _arg311, _arg411, _arg57, _arg68, _arg77, _arg86, _arg92);
                            reply.writeNoException();
                            reply.writeTypedObject(_arg92, 1);
                            return true;
                        case 39:
                            IBinder _arg038 = data.readStrongBinder();
                            int _arg128 = data.readInt();
                            SurfaceControl _arg217 = (SurfaceControl) data.readTypedObject(SurfaceControl.CREATOR);
                            int _arg312 = data.readInt();
                            int _arg412 = data.readInt();
                            data.enforceNoDataAvail();
                            updateInputChannel(_arg038, _arg128, _arg217, _arg312, _arg412, (Region) data.readTypedObject(Region.CREATOR));
                            return true;
                        case 40:
                            IWindow _arg039 = IWindow.Stub.asInterface(data.readStrongBinder());
                            IBinder _arg129 = data.readStrongBinder();
                            boolean _arg218 = data.readBoolean();
                            data.enforceNoDataAvail();
                            grantEmbeddedWindowFocus(_arg039, _arg129, _arg218);
                            reply.writeNoException();
                            return true;
                        case 41:
                            IWindow _arg040 = IWindow.Stub.asInterface(data.readStrongBinder());
                            Rect _arg130 = (Rect) data.readTypedObject(Rect.CREATOR);
                            String _arg219 = data.readString();
                            RemoteCallback _arg313 = (RemoteCallback) data.readTypedObject(RemoteCallback.CREATOR);
                            data.enforceNoDataAvail();
                            generateDisplayHash(_arg040, _arg130, _arg219, _arg313);
                            return true;
                        case 42:
                            IWindow _arg041 = IWindow.Stub.asInterface(data.readStrongBinder());
                            OnBackInvokedCallbackInfo _arg131 = (OnBackInvokedCallbackInfo) data.readTypedObject(OnBackInvokedCallbackInfo.CREATOR);
                            data.enforceNoDataAvail();
                            setOnBackInvokedCallbackInfo(_arg041, _arg131);
                            return true;
                        case 43:
                            IWindow _arg042 = IWindow.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            clearTouchableRegion(_arg042);
                            reply.writeNoException();
                            return true;
                        case 44:
                            SurfaceControl _arg043 = (SurfaceControl) data.readTypedObject(SurfaceControl.CREATOR);
                            float _arg132 = data.readFloat();
                            int _arg220 = data.readInt();
                            int _arg314 = data.readInt();
                            String _arg413 = data.readString();
                            String _arg58 = data.readString();
                            data.enforceNoDataAvail();
                            setRefreshRate(_arg043, _arg132, _arg220, _arg314, _arg413, _arg58);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes3.dex */
        public static class Proxy implements IWindowSession {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override // android.view.IWindowSession
            public int addToDisplay(IWindow window, WindowManager.LayoutParams attrs, int viewVisibility, int layerStackId, InsetsVisibilities requestedVisibilities, InputChannel outInputChannel, InsetsState insetsState, InsetsSourceControl[] activeControls) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    _data.writeTypedObject(attrs, 0);
                    _data.writeInt(viewVisibility);
                    _data.writeInt(layerStackId);
                    _data.writeTypedObject(requestedVisibilities, 0);
                    if (activeControls == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(activeControls.length);
                    }
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        outInputChannel.readFromParcel(_reply);
                    }
                    if (_reply.readInt() != 0) {
                        insetsState.readFromParcel(_reply);
                    }
                    _reply.readTypedArray(activeControls, InsetsSourceControl.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public int addToDisplayAsUser(IWindow window, WindowManager.LayoutParams attrs, int viewVisibility, int layerStackId, int userId, InsetsVisibilities requestedVisibilities, InputChannel outInputChannel, InsetsState insetsState, InsetsSourceControl[] activeControls) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    _data.writeTypedObject(attrs, 0);
                    _data.writeInt(viewVisibility);
                    _data.writeInt(layerStackId);
                    _data.writeInt(userId);
                    _data.writeTypedObject(requestedVisibilities, 0);
                    if (activeControls == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(activeControls.length);
                    }
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        outInputChannel.readFromParcel(_reply);
                    }
                    if (_reply.readInt() != 0) {
                        insetsState.readFromParcel(_reply);
                    }
                    _reply.readTypedArray(activeControls, InsetsSourceControl.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public int addToDisplayWithoutInputChannel(IWindow window, WindowManager.LayoutParams attrs, int viewVisibility, int layerStackId, InsetsState insetsState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    _data.writeTypedObject(attrs, 0);
                    _data.writeInt(viewVisibility);
                    _data.writeInt(layerStackId);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        insetsState.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void remove(IWindow window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public int relayout(IWindow window, WindowManager.LayoutParams attrs, int requestedWidth, int requestedHeight, int viewVisibility, int flags, ClientWindowFrames outFrames, MergedConfiguration outMergedConfiguration, SurfaceControl outSurfaceControl, InsetsState insetsState, InsetsSourceControl[] activeControls, Bundle bundle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    _data.writeStrongInterface(window);
                    try {
                        _data.writeTypedObject(attrs, 0);
                        try {
                            _data.writeInt(requestedWidth);
                            try {
                                _data.writeInt(requestedHeight);
                                try {
                                    _data.writeInt(viewVisibility);
                                    try {
                                        _data.writeInt(flags);
                                        if (activeControls == null) {
                                            _data.writeInt(-1);
                                        } else {
                                            _data.writeInt(activeControls.length);
                                        }
                                        try {
                                            this.mRemote.transact(5, _data, _reply, 0);
                                            _reply.readException();
                                            int _result = _reply.readInt();
                                            if (_reply.readInt() != 0) {
                                                try {
                                                    outFrames.readFromParcel(_reply);
                                                } catch (Throwable th2) {
                                                    th = th2;
                                                    _reply.recycle();
                                                    _data.recycle();
                                                    throw th;
                                                }
                                            }
                                            if (_reply.readInt() != 0) {
                                                try {
                                                    outMergedConfiguration.readFromParcel(_reply);
                                                } catch (Throwable th3) {
                                                    th = th3;
                                                    _reply.recycle();
                                                    _data.recycle();
                                                    throw th;
                                                }
                                            }
                                            if (_reply.readInt() != 0) {
                                                try {
                                                    outSurfaceControl.readFromParcel(_reply);
                                                } catch (Throwable th4) {
                                                    th = th4;
                                                    _reply.recycle();
                                                    _data.recycle();
                                                    throw th;
                                                }
                                            }
                                            if (_reply.readInt() != 0) {
                                                try {
                                                    insetsState.readFromParcel(_reply);
                                                } catch (Throwable th5) {
                                                    th = th5;
                                                    _reply.recycle();
                                                    _data.recycle();
                                                    throw th;
                                                }
                                            }
                                            _reply.readTypedArray(activeControls, InsetsSourceControl.CREATOR);
                                            if (_reply.readInt() != 0) {
                                                try {
                                                    bundle.readFromParcel(_reply);
                                                } catch (Throwable th6) {
                                                    th = th6;
                                                    _reply.recycle();
                                                    _data.recycle();
                                                    throw th;
                                                }
                                            }
                                            _reply.recycle();
                                            _data.recycle();
                                            return _result;
                                        } catch (Throwable th7) {
                                            th = th7;
                                            _reply.recycle();
                                            _data.recycle();
                                            throw th;
                                        }
                                    } catch (Throwable th8) {
                                        th = th8;
                                    }
                                } catch (Throwable th9) {
                                    th = th9;
                                    _reply.recycle();
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th10) {
                                th = th10;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th11) {
                            th = th11;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th12) {
                        th = th12;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th13) {
                    th = th13;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            @Override // android.view.IWindowSession
            public int updateVisibility(IWindow window, WindowManager.LayoutParams attrs, int viewVisibility, MergedConfiguration outMergedConfiguration, SurfaceControl outSurfaceControl, InsetsState outInsetsState, InsetsSourceControl[] outActiveControls) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    _data.writeTypedObject(attrs, 0);
                    _data.writeInt(viewVisibility);
                    if (outActiveControls == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(outActiveControls.length);
                    }
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        outMergedConfiguration.readFromParcel(_reply);
                    }
                    if (_reply.readInt() != 0) {
                        outSurfaceControl.readFromParcel(_reply);
                    }
                    if (_reply.readInt() != 0) {
                        outInsetsState.readFromParcel(_reply);
                    }
                    _reply.readTypedArray(outActiveControls, InsetsSourceControl.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void updateLayout(IWindow window, WindowManager.LayoutParams attrs, int flags, ClientWindowFrames clientFrames, int requestedWidth, int requestedHeight) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    _data.writeTypedObject(attrs, 0);
                    _data.writeInt(flags);
                    _data.writeTypedObject(clientFrames, 0);
                    _data.writeInt(requestedWidth);
                    _data.writeInt(requestedHeight);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void prepareToReplaceWindows(IBinder appToken, boolean childrenOnly) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(appToken);
                    _data.writeBoolean(childrenOnly);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public boolean outOfMemory(IWindow window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void setInsets(IWindow window, int touchableInsets, Rect contentInsets, Rect visibleInsets, Region touchableRegion) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    _data.writeInt(touchableInsets);
                    _data.writeTypedObject(contentInsets, 0);
                    _data.writeTypedObject(visibleInsets, 0);
                    _data.writeTypedObject(touchableRegion, 0);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void finishDrawing(IWindow window, SurfaceControl.Transaction postDrawTransaction, int seqId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    _data.writeTypedObject(postDrawTransaction, 0);
                    _data.writeInt(seqId);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void setInTouchMode(boolean showFocus) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(showFocus);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public boolean getInTouchMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public boolean performHapticFeedback(int effectId, boolean always) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(effectId);
                    _data.writeBoolean(always);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public IBinder performDrag(IWindow window, int flags, SurfaceControl surface, int touchSource, float touchX, float touchY, float thumbCenterX, float thumbCenterY, ClipData data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    _data.writeInt(flags);
                    _data.writeTypedObject(surface, 0);
                    _data.writeInt(touchSource);
                    _data.writeFloat(touchX);
                    _data.writeFloat(touchY);
                    _data.writeFloat(thumbCenterX);
                    _data.writeFloat(thumbCenterY);
                    _data.writeTypedObject(data, 0);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public boolean dropForAccessibility(IWindow window, int x, int y) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    _data.writeInt(x);
                    _data.writeInt(y);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void reportDropResult(IWindow window, boolean consumed) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    _data.writeBoolean(consumed);
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void cancelDragAndDrop(IBinder dragToken, boolean skipAnimation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(dragToken);
                    _data.writeBoolean(skipAnimation);
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void dragRecipientEntered(IWindow window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void dragRecipientExited(IWindow window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    this.mRemote.transact(20, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void setWallpaperPosition(IBinder windowToken, float x, float y, float xstep, float ystep) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(windowToken);
                    _data.writeFloat(x);
                    _data.writeFloat(y);
                    _data.writeFloat(xstep);
                    _data.writeFloat(ystep);
                    this.mRemote.transact(21, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void setWallpaperZoomOut(IBinder windowToken, float scale) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(windowToken);
                    _data.writeFloat(scale);
                    this.mRemote.transact(22, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void setShouldZoomOutWallpaper(IBinder windowToken, boolean shouldZoom) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(windowToken);
                    _data.writeBoolean(shouldZoom);
                    this.mRemote.transact(23, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void wallpaperOffsetsComplete(IBinder window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window);
                    this.mRemote.transact(24, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void setWallpaperDisplayOffset(IBinder windowToken, int x, int y) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(windowToken);
                    _data.writeInt(x);
                    _data.writeInt(y);
                    this.mRemote.transact(25, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public Bundle sendWallpaperCommand(IBinder window, String action, int x, int y, int z, Bundle extras, boolean sync) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window);
                    _data.writeString(action);
                    _data.writeInt(x);
                    _data.writeInt(y);
                    _data.writeInt(z);
                    _data.writeTypedObject(extras, 0);
                    _data.writeBoolean(sync);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    Bundle _result = (Bundle) _reply.readTypedObject(Bundle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void wallpaperCommandComplete(IBinder window, Bundle result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window);
                    _data.writeTypedObject(result, 0);
                    this.mRemote.transact(27, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void onRectangleOnScreenRequested(IBinder token, Rect rectangle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(rectangle, 0);
                    this.mRemote.transact(28, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public IWindowId getWindowId(IBinder window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    IWindowId _result = IWindowId.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void pokeDrawLock(IBinder window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(window);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public boolean startMovingTask(IWindow window, float startX, float startY) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    _data.writeFloat(startX);
                    _data.writeFloat(startY);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void finishMovingTask(IWindow window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    this.mRemote.transact(32, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void updatePointerIcon(IWindow window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    this.mRemote.transact(33, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void updateTapExcludeRegion(IWindow window, Region region) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    _data.writeTypedObject(region, 0);
                    this.mRemote.transact(34, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void updateRequestedVisibilities(IWindow window, InsetsVisibilities visibilities) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    _data.writeTypedObject(visibilities, 0);
                    this.mRemote.transact(35, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void reportSystemGestureExclusionChanged(IWindow window, List<Rect> exclusionRects) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    _data.writeTypedList(exclusionRects);
                    this.mRemote.transact(36, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void reportKeepClearAreasChanged(IWindow window, List<Rect> restricted, List<Rect> unrestricted) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    _data.writeTypedList(restricted);
                    _data.writeTypedList(unrestricted);
                    this.mRemote.transact(37, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void grantInputChannel(int displayId, SurfaceControl surface, IWindow window, IBinder hostInputToken, int flags, int privateFlags, int type, IBinder focusGrantToken, String inputHandleName, InputChannel outInputChannel) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeTypedObject(surface, 0);
                    _data.writeStrongInterface(window);
                    _data.writeStrongBinder(hostInputToken);
                    _data.writeInt(flags);
                    _data.writeInt(privateFlags);
                    _data.writeInt(type);
                    _data.writeStrongBinder(focusGrantToken);
                    _data.writeString(inputHandleName);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        outInputChannel.readFromParcel(_reply);
                    }
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void updateInputChannel(IBinder channelToken, int displayId, SurfaceControl surface, int flags, int privateFlags, Region region) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(channelToken);
                    _data.writeInt(displayId);
                    _data.writeTypedObject(surface, 0);
                    _data.writeInt(flags);
                    _data.writeInt(privateFlags);
                    _data.writeTypedObject(region, 0);
                    this.mRemote.transact(39, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void grantEmbeddedWindowFocus(IWindow window, IBinder inputToken, boolean grantFocus) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    _data.writeStrongBinder(inputToken);
                    _data.writeBoolean(grantFocus);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void generateDisplayHash(IWindow window, Rect boundsInWindow, String hashAlgorithm, RemoteCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    _data.writeTypedObject(boundsInWindow, 0);
                    _data.writeString(hashAlgorithm);
                    _data.writeTypedObject(callback, 0);
                    this.mRemote.transact(41, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void setOnBackInvokedCallbackInfo(IWindow window, OnBackInvokedCallbackInfo callbackInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    _data.writeTypedObject(callbackInfo, 0);
                    this.mRemote.transact(42, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void clearTouchableRegion(IWindow window) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(window);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.IWindowSession
            public void setRefreshRate(SurfaceControl sc, float refreshRate, int mMSyncScenarioAction, int mMSyncScenarioType, String activityName, String packgeName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(sc, 0);
                    _data.writeFloat(refreshRate);
                    _data.writeInt(mMSyncScenarioAction);
                    _data.writeInt(mMSyncScenarioType);
                    _data.writeString(activityName);
                    _data.writeString(packgeName);
                    this.mRemote.transact(44, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 43;
        }
    }
}
