package android.hardware.display;

import android.content.pm.ParceledListSlice;
import android.graphics.Point;
import android.hardware.display.IDisplayManagerCallback;
import android.hardware.display.IVirtualDisplayCallback;
import android.hardware.graphics.common.DisplayDecorationSupport;
import android.media.projection.IMediaProjection;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.Surface;
/* loaded from: classes.dex */
public interface IDisplayManager extends IInterface {
    void addDualDisplayCompotent(String str, String str2) throws RemoteException;

    boolean areUserDisabledHdrTypesAllowed() throws RemoteException;

    void closeDualDisplay() throws RemoteException;

    void connectWifiDisplay(String str) throws RemoteException;

    int createVirtualDisplay(VirtualDisplayConfig virtualDisplayConfig, IVirtualDisplayCallback iVirtualDisplayCallback, IMediaProjection iMediaProjection, String str) throws RemoteException;

    void disconnectWifiDisplay() throws RemoteException;

    void forgetWifiDisplay(String str) throws RemoteException;

    ParceledListSlice getAmbientBrightnessStats() throws RemoteException;

    float getBrightness(int i) throws RemoteException;

    BrightnessConfiguration getBrightnessConfigurationForDisplay(String str, int i) throws RemoteException;

    BrightnessConfiguration getBrightnessConfigurationForUser(int i) throws RemoteException;

    ParceledListSlice getBrightnessEvents(String str) throws RemoteException;

    BrightnessInfo getBrightnessInfo(int i) throws RemoteException;

    BrightnessConfiguration getDefaultBrightnessConfiguration() throws RemoteException;

    DisplayDecorationSupport getDisplayDecorationSupport(int i) throws RemoteException;

    int[] getDisplayIds(boolean z) throws RemoteException;

    DisplayInfo getDisplayInfo(int i) throws RemoteException;

    int getForcedUsingDisplayMode() throws RemoteException;

    Curve getMinimumBrightnessCurve() throws RemoteException;

    int getPreferredWideGamutColorSpaceId() throws RemoteException;

    int getRefreshRateSwitchingType() throws RemoteException;

    Point getStableDisplaySize() throws RemoteException;

    Display.Mode getSystemPreferredDisplayMode(int i) throws RemoteException;

    int[] getUserDisabledHdrTypes() throws RemoteException;

    Display.Mode getUserPreferredDisplayMode(int i) throws RemoteException;

    WifiDisplayStatus getWifiDisplayStatus() throws RemoteException;

    boolean isDualDisplayComponent(String str, String str2) throws RemoteException;

    boolean isMinimalPostProcessingRequested(int i) throws RemoteException;

    boolean isUidPresentOnDisplay(int i, int i2) throws RemoteException;

    void openDualDisplay() throws RemoteException;

    void pauseWifiDisplay() throws RemoteException;

    void registerCallback(IDisplayManagerCallback iDisplayManagerCallback) throws RemoteException;

    void registerCallbackWithEventMask(IDisplayManagerCallback iDisplayManagerCallback, long j) throws RemoteException;

    void registerDualCallbackWithEventMask(IDisplayManagerCallback iDisplayManagerCallback, long j) throws RemoteException;

    void releaseVirtualDisplay(IVirtualDisplayCallback iVirtualDisplayCallback) throws RemoteException;

    void renameWifiDisplay(String str, String str2) throws RemoteException;

    void requestColorMode(int i, int i2) throws RemoteException;

    void resizeVirtualDisplay(IVirtualDisplayCallback iVirtualDisplayCallback, int i, int i2, int i3) throws RemoteException;

    void resumeWifiDisplay() throws RemoteException;

    void setAreUserDisabledHdrTypesAllowed(boolean z) throws RemoteException;

    void setBrightness(int i, float f) throws RemoteException;

    void setBrightnessConfigurationForDisplay(BrightnessConfiguration brightnessConfiguration, String str, int i, String str2) throws RemoteException;

    void setBrightnessConfigurationForUser(BrightnessConfiguration brightnessConfiguration, int i, String str) throws RemoteException;

    void setForcedUsingDisplayMode(int i) throws RemoteException;

    void setRefreshRateSwitchingType(int i) throws RemoteException;

    void setShouldAlwaysRespectAppRequestedMode(boolean z) throws RemoteException;

    void setTemporaryAutoBrightnessAdjustment(float f) throws RemoteException;

    void setTemporaryBrightness(int i, float f) throws RemoteException;

    void setUserDisabledHdrTypes(int[] iArr) throws RemoteException;

    void setUserPreferredDisplayMode(int i, Display.Mode mode) throws RemoteException;

    void setVirtualDisplayState(IVirtualDisplayCallback iVirtualDisplayCallback, boolean z) throws RemoteException;

    void setVirtualDisplaySurface(IVirtualDisplayCallback iVirtualDisplayCallback, Surface surface) throws RemoteException;

    boolean shouldAlwaysRespectAppRequestedMode() throws RemoteException;

    void startWifiDisplayScan() throws RemoteException;

    void stopWifiDisplayScan() throws RemoteException;

    void updateRefreshRateForScene(Bundle bundle) throws RemoteException;

    void updateRefreshRateForVideoScene(int i, int i2, int i3) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IDisplayManager {
        @Override // android.hardware.display.IDisplayManager
        public DisplayInfo getDisplayInfo(int displayId) throws RemoteException {
            return null;
        }

        @Override // android.hardware.display.IDisplayManager
        public int[] getDisplayIds(boolean includeDisabled) throws RemoteException {
            return null;
        }

        @Override // android.hardware.display.IDisplayManager
        public boolean isUidPresentOnDisplay(int uid, int displayId) throws RemoteException {
            return false;
        }

        @Override // android.hardware.display.IDisplayManager
        public void registerCallback(IDisplayManagerCallback callback) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public void registerCallbackWithEventMask(IDisplayManagerCallback callback, long eventsMask) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public void startWifiDisplayScan() throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public void stopWifiDisplayScan() throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public void connectWifiDisplay(String address) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public void disconnectWifiDisplay() throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public void renameWifiDisplay(String address, String alias) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public void forgetWifiDisplay(String address) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public void pauseWifiDisplay() throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public void resumeWifiDisplay() throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public WifiDisplayStatus getWifiDisplayStatus() throws RemoteException {
            return null;
        }

        @Override // android.hardware.display.IDisplayManager
        public void setUserDisabledHdrTypes(int[] userDisabledTypes) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public void setAreUserDisabledHdrTypesAllowed(boolean areUserDisabledHdrTypesAllowed) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public boolean areUserDisabledHdrTypesAllowed() throws RemoteException {
            return false;
        }

        @Override // android.hardware.display.IDisplayManager
        public int[] getUserDisabledHdrTypes() throws RemoteException {
            return null;
        }

        @Override // android.hardware.display.IDisplayManager
        public void requestColorMode(int displayId, int colorMode) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public int createVirtualDisplay(VirtualDisplayConfig virtualDisplayConfig, IVirtualDisplayCallback callback, IMediaProjection projectionToken, String packageName) throws RemoteException {
            return 0;
        }

        @Override // android.hardware.display.IDisplayManager
        public void resizeVirtualDisplay(IVirtualDisplayCallback token, int width, int height, int densityDpi) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public void setVirtualDisplaySurface(IVirtualDisplayCallback token, Surface surface) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public void releaseVirtualDisplay(IVirtualDisplayCallback token) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public void setVirtualDisplayState(IVirtualDisplayCallback token, boolean isOn) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public Point getStableDisplaySize() throws RemoteException {
            return null;
        }

        @Override // android.hardware.display.IDisplayManager
        public ParceledListSlice getBrightnessEvents(String callingPackage) throws RemoteException {
            return null;
        }

        @Override // android.hardware.display.IDisplayManager
        public ParceledListSlice getAmbientBrightnessStats() throws RemoteException {
            return null;
        }

        @Override // android.hardware.display.IDisplayManager
        public void setBrightnessConfigurationForUser(BrightnessConfiguration c, int userId, String packageName) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public void setBrightnessConfigurationForDisplay(BrightnessConfiguration c, String uniqueDisplayId, int userId, String packageName) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public BrightnessConfiguration getBrightnessConfigurationForDisplay(String uniqueDisplayId, int userId) throws RemoteException {
            return null;
        }

        @Override // android.hardware.display.IDisplayManager
        public BrightnessConfiguration getBrightnessConfigurationForUser(int userId) throws RemoteException {
            return null;
        }

        @Override // android.hardware.display.IDisplayManager
        public BrightnessConfiguration getDefaultBrightnessConfiguration() throws RemoteException {
            return null;
        }

        @Override // android.hardware.display.IDisplayManager
        public boolean isMinimalPostProcessingRequested(int displayId) throws RemoteException {
            return false;
        }

        @Override // android.hardware.display.IDisplayManager
        public void setTemporaryBrightness(int displayId, float brightness) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public void setBrightness(int displayId, float brightness) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public float getBrightness(int displayId) throws RemoteException {
            return 0.0f;
        }

        @Override // android.hardware.display.IDisplayManager
        public void setTemporaryAutoBrightnessAdjustment(float adjustment) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public Curve getMinimumBrightnessCurve() throws RemoteException {
            return null;
        }

        @Override // android.hardware.display.IDisplayManager
        public BrightnessInfo getBrightnessInfo(int displayId) throws RemoteException {
            return null;
        }

        @Override // android.hardware.display.IDisplayManager
        public int getPreferredWideGamutColorSpaceId() throws RemoteException {
            return 0;
        }

        @Override // android.hardware.display.IDisplayManager
        public void setUserPreferredDisplayMode(int displayId, Display.Mode mode) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public Display.Mode getUserPreferredDisplayMode(int displayId) throws RemoteException {
            return null;
        }

        @Override // android.hardware.display.IDisplayManager
        public Display.Mode getSystemPreferredDisplayMode(int displayId) throws RemoteException {
            return null;
        }

        @Override // android.hardware.display.IDisplayManager
        public void setShouldAlwaysRespectAppRequestedMode(boolean enabled) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public boolean shouldAlwaysRespectAppRequestedMode() throws RemoteException {
            return false;
        }

        @Override // android.hardware.display.IDisplayManager
        public void setRefreshRateSwitchingType(int newValue) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public int getRefreshRateSwitchingType() throws RemoteException {
            return 0;
        }

        @Override // android.hardware.display.IDisplayManager
        public DisplayDecorationSupport getDisplayDecorationSupport(int displayId) throws RemoteException {
            return null;
        }

        @Override // android.hardware.display.IDisplayManager
        public void updateRefreshRateForScene(Bundle b) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public void setForcedUsingDisplayMode(int forcedMode) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public int getForcedUsingDisplayMode() throws RemoteException {
            return 0;
        }

        @Override // android.hardware.display.IDisplayManager
        public void registerDualCallbackWithEventMask(IDisplayManagerCallback callback, long eventsMask) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public void openDualDisplay() throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public void closeDualDisplay() throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public void addDualDisplayCompotent(String pkg, String title) throws RemoteException {
        }

        @Override // android.hardware.display.IDisplayManager
        public boolean isDualDisplayComponent(String pkg, String title) throws RemoteException {
            return false;
        }

        @Override // android.hardware.display.IDisplayManager
        public void updateRefreshRateForVideoScene(int videoState, int videoFps, int videoSessionId) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IDisplayManager {
        public static final String DESCRIPTOR = "android.hardware.display.IDisplayManager";
        static final int TRANSACTION_addDualDisplayCompotent = 55;
        static final int TRANSACTION_areUserDisabledHdrTypesAllowed = 17;
        static final int TRANSACTION_closeDualDisplay = 54;
        static final int TRANSACTION_connectWifiDisplay = 8;
        static final int TRANSACTION_createVirtualDisplay = 20;
        static final int TRANSACTION_disconnectWifiDisplay = 9;
        static final int TRANSACTION_forgetWifiDisplay = 11;
        static final int TRANSACTION_getAmbientBrightnessStats = 27;
        static final int TRANSACTION_getBrightness = 36;
        static final int TRANSACTION_getBrightnessConfigurationForDisplay = 30;
        static final int TRANSACTION_getBrightnessConfigurationForUser = 31;
        static final int TRANSACTION_getBrightnessEvents = 26;
        static final int TRANSACTION_getBrightnessInfo = 39;
        static final int TRANSACTION_getDefaultBrightnessConfiguration = 32;
        static final int TRANSACTION_getDisplayDecorationSupport = 48;
        static final int TRANSACTION_getDisplayIds = 2;
        static final int TRANSACTION_getDisplayInfo = 1;
        static final int TRANSACTION_getForcedUsingDisplayMode = 51;
        static final int TRANSACTION_getMinimumBrightnessCurve = 38;
        static final int TRANSACTION_getPreferredWideGamutColorSpaceId = 40;
        static final int TRANSACTION_getRefreshRateSwitchingType = 47;
        static final int TRANSACTION_getStableDisplaySize = 25;
        static final int TRANSACTION_getSystemPreferredDisplayMode = 43;
        static final int TRANSACTION_getUserDisabledHdrTypes = 18;
        static final int TRANSACTION_getUserPreferredDisplayMode = 42;
        static final int TRANSACTION_getWifiDisplayStatus = 14;
        static final int TRANSACTION_isDualDisplayComponent = 56;
        static final int TRANSACTION_isMinimalPostProcessingRequested = 33;
        static final int TRANSACTION_isUidPresentOnDisplay = 3;
        static final int TRANSACTION_openDualDisplay = 53;
        static final int TRANSACTION_pauseWifiDisplay = 12;
        static final int TRANSACTION_registerCallback = 4;
        static final int TRANSACTION_registerCallbackWithEventMask = 5;
        static final int TRANSACTION_registerDualCallbackWithEventMask = 52;
        static final int TRANSACTION_releaseVirtualDisplay = 23;
        static final int TRANSACTION_renameWifiDisplay = 10;
        static final int TRANSACTION_requestColorMode = 19;
        static final int TRANSACTION_resizeVirtualDisplay = 21;
        static final int TRANSACTION_resumeWifiDisplay = 13;
        static final int TRANSACTION_setAreUserDisabledHdrTypesAllowed = 16;
        static final int TRANSACTION_setBrightness = 35;
        static final int TRANSACTION_setBrightnessConfigurationForDisplay = 29;
        static final int TRANSACTION_setBrightnessConfigurationForUser = 28;
        static final int TRANSACTION_setForcedUsingDisplayMode = 50;
        static final int TRANSACTION_setRefreshRateSwitchingType = 46;
        static final int TRANSACTION_setShouldAlwaysRespectAppRequestedMode = 44;
        static final int TRANSACTION_setTemporaryAutoBrightnessAdjustment = 37;
        static final int TRANSACTION_setTemporaryBrightness = 34;
        static final int TRANSACTION_setUserDisabledHdrTypes = 15;
        static final int TRANSACTION_setUserPreferredDisplayMode = 41;
        static final int TRANSACTION_setVirtualDisplayState = 24;
        static final int TRANSACTION_setVirtualDisplaySurface = 22;
        static final int TRANSACTION_shouldAlwaysRespectAppRequestedMode = 45;
        static final int TRANSACTION_startWifiDisplayScan = 6;
        static final int TRANSACTION_stopWifiDisplayScan = 7;
        static final int TRANSACTION_updateRefreshRateForScene = 49;
        static final int TRANSACTION_updateRefreshRateForVideoScene = 57;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IDisplayManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IDisplayManager)) {
                return (IDisplayManager) iin;
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
                    return "getDisplayInfo";
                case 2:
                    return "getDisplayIds";
                case 3:
                    return "isUidPresentOnDisplay";
                case 4:
                    return "registerCallback";
                case 5:
                    return "registerCallbackWithEventMask";
                case 6:
                    return "startWifiDisplayScan";
                case 7:
                    return "stopWifiDisplayScan";
                case 8:
                    return "connectWifiDisplay";
                case 9:
                    return "disconnectWifiDisplay";
                case 10:
                    return "renameWifiDisplay";
                case 11:
                    return "forgetWifiDisplay";
                case 12:
                    return "pauseWifiDisplay";
                case 13:
                    return "resumeWifiDisplay";
                case 14:
                    return "getWifiDisplayStatus";
                case 15:
                    return "setUserDisabledHdrTypes";
                case 16:
                    return "setAreUserDisabledHdrTypesAllowed";
                case 17:
                    return "areUserDisabledHdrTypesAllowed";
                case 18:
                    return "getUserDisabledHdrTypes";
                case 19:
                    return "requestColorMode";
                case 20:
                    return "createVirtualDisplay";
                case 21:
                    return "resizeVirtualDisplay";
                case 22:
                    return "setVirtualDisplaySurface";
                case 23:
                    return "releaseVirtualDisplay";
                case 24:
                    return "setVirtualDisplayState";
                case 25:
                    return "getStableDisplaySize";
                case 26:
                    return "getBrightnessEvents";
                case 27:
                    return "getAmbientBrightnessStats";
                case 28:
                    return "setBrightnessConfigurationForUser";
                case 29:
                    return "setBrightnessConfigurationForDisplay";
                case 30:
                    return "getBrightnessConfigurationForDisplay";
                case 31:
                    return "getBrightnessConfigurationForUser";
                case 32:
                    return "getDefaultBrightnessConfiguration";
                case 33:
                    return "isMinimalPostProcessingRequested";
                case 34:
                    return "setTemporaryBrightness";
                case 35:
                    return "setBrightness";
                case 36:
                    return "getBrightness";
                case 37:
                    return "setTemporaryAutoBrightnessAdjustment";
                case 38:
                    return "getMinimumBrightnessCurve";
                case 39:
                    return "getBrightnessInfo";
                case 40:
                    return "getPreferredWideGamutColorSpaceId";
                case 41:
                    return "setUserPreferredDisplayMode";
                case 42:
                    return "getUserPreferredDisplayMode";
                case 43:
                    return "getSystemPreferredDisplayMode";
                case 44:
                    return "setShouldAlwaysRespectAppRequestedMode";
                case 45:
                    return "shouldAlwaysRespectAppRequestedMode";
                case 46:
                    return "setRefreshRateSwitchingType";
                case 47:
                    return "getRefreshRateSwitchingType";
                case 48:
                    return "getDisplayDecorationSupport";
                case 49:
                    return "updateRefreshRateForScene";
                case 50:
                    return "setForcedUsingDisplayMode";
                case 51:
                    return "getForcedUsingDisplayMode";
                case 52:
                    return "registerDualCallbackWithEventMask";
                case 53:
                    return "openDualDisplay";
                case 54:
                    return "closeDualDisplay";
                case 55:
                    return "addDualDisplayCompotent";
                case 56:
                    return "isDualDisplayComponent";
                case 57:
                    return "updateRefreshRateForVideoScene";
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
                data.enforceInterface(DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            data.enforceNoDataAvail();
                            DisplayInfo _result = getDisplayInfo(_arg0);
                            reply.writeNoException();
                            reply.writeTypedObject(_result, 1);
                            break;
                        case 2:
                            boolean _arg02 = data.readBoolean();
                            data.enforceNoDataAvail();
                            int[] _result2 = getDisplayIds(_arg02);
                            reply.writeNoException();
                            reply.writeIntArray(_result2);
                            break;
                        case 3:
                            int _arg03 = data.readInt();
                            int _arg1 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result3 = isUidPresentOnDisplay(_arg03, _arg1);
                            reply.writeNoException();
                            reply.writeBoolean(_result3);
                            break;
                        case 4:
                            IDisplayManagerCallback _arg04 = IDisplayManagerCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            registerCallback(_arg04);
                            reply.writeNoException();
                            break;
                        case 5:
                            IDisplayManagerCallback _arg05 = IDisplayManagerCallback.Stub.asInterface(data.readStrongBinder());
                            long _arg12 = data.readLong();
                            data.enforceNoDataAvail();
                            registerCallbackWithEventMask(_arg05, _arg12);
                            reply.writeNoException();
                            break;
                        case 6:
                            startWifiDisplayScan();
                            reply.writeNoException();
                            break;
                        case 7:
                            stopWifiDisplayScan();
                            reply.writeNoException();
                            break;
                        case 8:
                            String _arg06 = data.readString();
                            data.enforceNoDataAvail();
                            connectWifiDisplay(_arg06);
                            reply.writeNoException();
                            break;
                        case 9:
                            disconnectWifiDisplay();
                            reply.writeNoException();
                            break;
                        case 10:
                            String _arg07 = data.readString();
                            String _arg13 = data.readString();
                            data.enforceNoDataAvail();
                            renameWifiDisplay(_arg07, _arg13);
                            reply.writeNoException();
                            break;
                        case 11:
                            String _arg08 = data.readString();
                            data.enforceNoDataAvail();
                            forgetWifiDisplay(_arg08);
                            reply.writeNoException();
                            break;
                        case 12:
                            pauseWifiDisplay();
                            reply.writeNoException();
                            break;
                        case 13:
                            resumeWifiDisplay();
                            reply.writeNoException();
                            break;
                        case 14:
                            WifiDisplayStatus _result4 = getWifiDisplayStatus();
                            reply.writeNoException();
                            reply.writeTypedObject(_result4, 1);
                            break;
                        case 15:
                            int[] _arg09 = data.createIntArray();
                            data.enforceNoDataAvail();
                            setUserDisabledHdrTypes(_arg09);
                            reply.writeNoException();
                            break;
                        case 16:
                            boolean _arg010 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setAreUserDisabledHdrTypesAllowed(_arg010);
                            reply.writeNoException();
                            break;
                        case 17:
                            boolean _result5 = areUserDisabledHdrTypesAllowed();
                            reply.writeNoException();
                            reply.writeBoolean(_result5);
                            break;
                        case 18:
                            int[] _result6 = getUserDisabledHdrTypes();
                            reply.writeNoException();
                            reply.writeIntArray(_result6);
                            break;
                        case 19:
                            int _arg011 = data.readInt();
                            int _arg14 = data.readInt();
                            data.enforceNoDataAvail();
                            requestColorMode(_arg011, _arg14);
                            reply.writeNoException();
                            break;
                        case 20:
                            VirtualDisplayConfig _arg012 = (VirtualDisplayConfig) data.readTypedObject(VirtualDisplayConfig.CREATOR);
                            IVirtualDisplayCallback _arg15 = IVirtualDisplayCallback.Stub.asInterface(data.readStrongBinder());
                            IMediaProjection _arg2 = IMediaProjection.Stub.asInterface(data.readStrongBinder());
                            String _arg3 = data.readString();
                            data.enforceNoDataAvail();
                            int _result7 = createVirtualDisplay(_arg012, _arg15, _arg2, _arg3);
                            reply.writeNoException();
                            reply.writeInt(_result7);
                            break;
                        case 21:
                            IVirtualDisplayCallback _arg013 = IVirtualDisplayCallback.Stub.asInterface(data.readStrongBinder());
                            int _arg16 = data.readInt();
                            int _arg22 = data.readInt();
                            int _arg32 = data.readInt();
                            data.enforceNoDataAvail();
                            resizeVirtualDisplay(_arg013, _arg16, _arg22, _arg32);
                            reply.writeNoException();
                            break;
                        case 22:
                            IVirtualDisplayCallback _arg014 = IVirtualDisplayCallback.Stub.asInterface(data.readStrongBinder());
                            Surface _arg17 = (Surface) data.readTypedObject(Surface.CREATOR);
                            data.enforceNoDataAvail();
                            setVirtualDisplaySurface(_arg014, _arg17);
                            reply.writeNoException();
                            break;
                        case 23:
                            IVirtualDisplayCallback _arg015 = IVirtualDisplayCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            releaseVirtualDisplay(_arg015);
                            reply.writeNoException();
                            break;
                        case 24:
                            IVirtualDisplayCallback _arg016 = IVirtualDisplayCallback.Stub.asInterface(data.readStrongBinder());
                            boolean _arg18 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setVirtualDisplayState(_arg016, _arg18);
                            reply.writeNoException();
                            break;
                        case 25:
                            Point _result8 = getStableDisplaySize();
                            reply.writeNoException();
                            reply.writeTypedObject(_result8, 1);
                            break;
                        case 26:
                            String _arg017 = data.readString();
                            data.enforceNoDataAvail();
                            ParceledListSlice _result9 = getBrightnessEvents(_arg017);
                            reply.writeNoException();
                            reply.writeTypedObject(_result9, 1);
                            break;
                        case 27:
                            ParceledListSlice _result10 = getAmbientBrightnessStats();
                            reply.writeNoException();
                            reply.writeTypedObject(_result10, 1);
                            break;
                        case 28:
                            BrightnessConfiguration _arg018 = (BrightnessConfiguration) data.readTypedObject(BrightnessConfiguration.CREATOR);
                            int _arg19 = data.readInt();
                            String _arg23 = data.readString();
                            data.enforceNoDataAvail();
                            setBrightnessConfigurationForUser(_arg018, _arg19, _arg23);
                            reply.writeNoException();
                            break;
                        case 29:
                            BrightnessConfiguration _arg019 = (BrightnessConfiguration) data.readTypedObject(BrightnessConfiguration.CREATOR);
                            String _arg110 = data.readString();
                            int _arg24 = data.readInt();
                            String _arg33 = data.readString();
                            data.enforceNoDataAvail();
                            setBrightnessConfigurationForDisplay(_arg019, _arg110, _arg24, _arg33);
                            reply.writeNoException();
                            break;
                        case 30:
                            String _arg020 = data.readString();
                            int _arg111 = data.readInt();
                            data.enforceNoDataAvail();
                            BrightnessConfiguration _result11 = getBrightnessConfigurationForDisplay(_arg020, _arg111);
                            reply.writeNoException();
                            reply.writeTypedObject(_result11, 1);
                            break;
                        case 31:
                            int _arg021 = data.readInt();
                            data.enforceNoDataAvail();
                            BrightnessConfiguration _result12 = getBrightnessConfigurationForUser(_arg021);
                            reply.writeNoException();
                            reply.writeTypedObject(_result12, 1);
                            break;
                        case 32:
                            BrightnessConfiguration _result13 = getDefaultBrightnessConfiguration();
                            reply.writeNoException();
                            reply.writeTypedObject(_result13, 1);
                            break;
                        case 33:
                            int _arg022 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result14 = isMinimalPostProcessingRequested(_arg022);
                            reply.writeNoException();
                            reply.writeBoolean(_result14);
                            break;
                        case 34:
                            int _arg023 = data.readInt();
                            float _arg112 = data.readFloat();
                            data.enforceNoDataAvail();
                            setTemporaryBrightness(_arg023, _arg112);
                            reply.writeNoException();
                            break;
                        case 35:
                            int _arg024 = data.readInt();
                            float _arg113 = data.readFloat();
                            data.enforceNoDataAvail();
                            setBrightness(_arg024, _arg113);
                            reply.writeNoException();
                            break;
                        case 36:
                            int _arg025 = data.readInt();
                            data.enforceNoDataAvail();
                            float _result15 = getBrightness(_arg025);
                            reply.writeNoException();
                            reply.writeFloat(_result15);
                            break;
                        case 37:
                            float _arg026 = data.readFloat();
                            data.enforceNoDataAvail();
                            setTemporaryAutoBrightnessAdjustment(_arg026);
                            reply.writeNoException();
                            break;
                        case 38:
                            Curve _result16 = getMinimumBrightnessCurve();
                            reply.writeNoException();
                            reply.writeTypedObject(_result16, 1);
                            break;
                        case 39:
                            int _arg027 = data.readInt();
                            data.enforceNoDataAvail();
                            BrightnessInfo _result17 = getBrightnessInfo(_arg027);
                            reply.writeNoException();
                            reply.writeTypedObject(_result17, 1);
                            break;
                        case 40:
                            int _result18 = getPreferredWideGamutColorSpaceId();
                            reply.writeNoException();
                            reply.writeInt(_result18);
                            break;
                        case 41:
                            int _arg028 = data.readInt();
                            Display.Mode _arg114 = (Display.Mode) data.readTypedObject(Display.Mode.CREATOR);
                            data.enforceNoDataAvail();
                            setUserPreferredDisplayMode(_arg028, _arg114);
                            reply.writeNoException();
                            break;
                        case 42:
                            int _arg029 = data.readInt();
                            data.enforceNoDataAvail();
                            Display.Mode _result19 = getUserPreferredDisplayMode(_arg029);
                            reply.writeNoException();
                            reply.writeTypedObject(_result19, 1);
                            break;
                        case 43:
                            int _arg030 = data.readInt();
                            data.enforceNoDataAvail();
                            Display.Mode _result20 = getSystemPreferredDisplayMode(_arg030);
                            reply.writeNoException();
                            reply.writeTypedObject(_result20, 1);
                            break;
                        case 44:
                            boolean _arg031 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setShouldAlwaysRespectAppRequestedMode(_arg031);
                            reply.writeNoException();
                            break;
                        case 45:
                            boolean _result21 = shouldAlwaysRespectAppRequestedMode();
                            reply.writeNoException();
                            reply.writeBoolean(_result21);
                            break;
                        case 46:
                            int _arg032 = data.readInt();
                            data.enforceNoDataAvail();
                            setRefreshRateSwitchingType(_arg032);
                            reply.writeNoException();
                            break;
                        case 47:
                            int _result22 = getRefreshRateSwitchingType();
                            reply.writeNoException();
                            reply.writeInt(_result22);
                            break;
                        case 48:
                            int _arg033 = data.readInt();
                            data.enforceNoDataAvail();
                            DisplayDecorationSupport _result23 = getDisplayDecorationSupport(_arg033);
                            reply.writeNoException();
                            reply.writeTypedObject(_result23, 1);
                            break;
                        case 49:
                            Bundle _arg034 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            updateRefreshRateForScene(_arg034);
                            reply.writeNoException();
                            break;
                        case 50:
                            int _arg035 = data.readInt();
                            data.enforceNoDataAvail();
                            setForcedUsingDisplayMode(_arg035);
                            reply.writeNoException();
                            break;
                        case 51:
                            int _result24 = getForcedUsingDisplayMode();
                            reply.writeNoException();
                            reply.writeInt(_result24);
                            break;
                        case 52:
                            IDisplayManagerCallback _arg036 = IDisplayManagerCallback.Stub.asInterface(data.readStrongBinder());
                            long _arg115 = data.readLong();
                            data.enforceNoDataAvail();
                            registerDualCallbackWithEventMask(_arg036, _arg115);
                            reply.writeNoException();
                            break;
                        case 53:
                            openDualDisplay();
                            reply.writeNoException();
                            break;
                        case 54:
                            closeDualDisplay();
                            reply.writeNoException();
                            break;
                        case 55:
                            String _arg037 = data.readString();
                            String _arg116 = data.readString();
                            data.enforceNoDataAvail();
                            addDualDisplayCompotent(_arg037, _arg116);
                            reply.writeNoException();
                            break;
                        case 56:
                            String _arg038 = data.readString();
                            String _arg117 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result25 = isDualDisplayComponent(_arg038, _arg117);
                            reply.writeNoException();
                            reply.writeBoolean(_result25);
                            break;
                        case 57:
                            int _arg039 = data.readInt();
                            int _arg118 = data.readInt();
                            int _arg25 = data.readInt();
                            data.enforceNoDataAvail();
                            updateRefreshRateForVideoScene(_arg039, _arg118, _arg25);
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
        public static class Proxy implements IDisplayManager {
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

            @Override // android.hardware.display.IDisplayManager
            public DisplayInfo getDisplayInfo(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    DisplayInfo _result = (DisplayInfo) _reply.readTypedObject(DisplayInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public int[] getDisplayIds(boolean includeDisabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(includeDisabled);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public boolean isUidPresentOnDisplay(int uid, int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(displayId);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void registerCallback(IDisplayManagerCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void registerCallbackWithEventMask(IDisplayManagerCallback callback, long eventsMask) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(callback);
                    _data.writeLong(eventsMask);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void startWifiDisplayScan() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void stopWifiDisplayScan() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void connectWifiDisplay(String address) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void disconnectWifiDisplay() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void renameWifiDisplay(String address, String alias) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeString(alias);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void forgetWifiDisplay(String address) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void pauseWifiDisplay() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void resumeWifiDisplay() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public WifiDisplayStatus getWifiDisplayStatus() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    WifiDisplayStatus _result = (WifiDisplayStatus) _reply.readTypedObject(WifiDisplayStatus.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void setUserDisabledHdrTypes(int[] userDisabledTypes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(userDisabledTypes);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void setAreUserDisabledHdrTypesAllowed(boolean areUserDisabledHdrTypesAllowed) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(areUserDisabledHdrTypesAllowed);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public boolean areUserDisabledHdrTypesAllowed() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public int[] getUserDisabledHdrTypes() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void requestColorMode(int displayId, int colorMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeInt(colorMode);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public int createVirtualDisplay(VirtualDisplayConfig virtualDisplayConfig, IVirtualDisplayCallback callback, IMediaProjection projectionToken, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(virtualDisplayConfig, 0);
                    _data.writeStrongInterface(callback);
                    _data.writeStrongInterface(projectionToken);
                    _data.writeString(packageName);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void resizeVirtualDisplay(IVirtualDisplayCallback token, int width, int height, int densityDpi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(token);
                    _data.writeInt(width);
                    _data.writeInt(height);
                    _data.writeInt(densityDpi);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void setVirtualDisplaySurface(IVirtualDisplayCallback token, Surface surface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(token);
                    _data.writeTypedObject(surface, 0);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void releaseVirtualDisplay(IVirtualDisplayCallback token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(token);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void setVirtualDisplayState(IVirtualDisplayCallback token, boolean isOn) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(token);
                    _data.writeBoolean(isOn);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public Point getStableDisplaySize() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    Point _result = (Point) _reply.readTypedObject(Point.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public ParceledListSlice getBrightnessEvents(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    ParceledListSlice _result = (ParceledListSlice) _reply.readTypedObject(ParceledListSlice.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public ParceledListSlice getAmbientBrightnessStats() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    ParceledListSlice _result = (ParceledListSlice) _reply.readTypedObject(ParceledListSlice.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void setBrightnessConfigurationForUser(BrightnessConfiguration c, int userId, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(c, 0);
                    _data.writeInt(userId);
                    _data.writeString(packageName);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void setBrightnessConfigurationForDisplay(BrightnessConfiguration c, String uniqueDisplayId, int userId, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(c, 0);
                    _data.writeString(uniqueDisplayId);
                    _data.writeInt(userId);
                    _data.writeString(packageName);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public BrightnessConfiguration getBrightnessConfigurationForDisplay(String uniqueDisplayId, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uniqueDisplayId);
                    _data.writeInt(userId);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    BrightnessConfiguration _result = (BrightnessConfiguration) _reply.readTypedObject(BrightnessConfiguration.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public BrightnessConfiguration getBrightnessConfigurationForUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                    BrightnessConfiguration _result = (BrightnessConfiguration) _reply.readTypedObject(BrightnessConfiguration.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public BrightnessConfiguration getDefaultBrightnessConfiguration() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    BrightnessConfiguration _result = (BrightnessConfiguration) _reply.readTypedObject(BrightnessConfiguration.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public boolean isMinimalPostProcessingRequested(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void setTemporaryBrightness(int displayId, float brightness) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeFloat(brightness);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void setBrightness(int displayId, float brightness) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeFloat(brightness);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public float getBrightness(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                    float _result = _reply.readFloat();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void setTemporaryAutoBrightnessAdjustment(float adjustment) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeFloat(adjustment);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public Curve getMinimumBrightnessCurve() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                    Curve _result = (Curve) _reply.readTypedObject(Curve.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public BrightnessInfo getBrightnessInfo(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                    BrightnessInfo _result = (BrightnessInfo) _reply.readTypedObject(BrightnessInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public int getPreferredWideGamutColorSpaceId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void setUserPreferredDisplayMode(int displayId, Display.Mode mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeTypedObject(mode, 0);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public Display.Mode getUserPreferredDisplayMode(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                    Display.Mode _result = (Display.Mode) _reply.readTypedObject(Display.Mode.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public Display.Mode getSystemPreferredDisplayMode(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                    Display.Mode _result = (Display.Mode) _reply.readTypedObject(Display.Mode.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void setShouldAlwaysRespectAppRequestedMode(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(44, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public boolean shouldAlwaysRespectAppRequestedMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(45, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void setRefreshRateSwitchingType(int newValue) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(newValue);
                    this.mRemote.transact(46, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public int getRefreshRateSwitchingType() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(47, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public DisplayDecorationSupport getDisplayDecorationSupport(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(48, _data, _reply, 0);
                    _reply.readException();
                    DisplayDecorationSupport _result = (DisplayDecorationSupport) _reply.readTypedObject(DisplayDecorationSupport.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void updateRefreshRateForScene(Bundle b) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(b, 0);
                    this.mRemote.transact(49, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void setForcedUsingDisplayMode(int forcedMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(forcedMode);
                    this.mRemote.transact(50, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public int getForcedUsingDisplayMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(51, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void registerDualCallbackWithEventMask(IDisplayManagerCallback callback, long eventsMask) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(callback);
                    _data.writeLong(eventsMask);
                    this.mRemote.transact(52, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void openDualDisplay() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void closeDualDisplay() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(54, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void addDualDisplayCompotent(String pkg, String title) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeString(title);
                    this.mRemote.transact(55, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public boolean isDualDisplayComponent(String pkg, String title) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeString(title);
                    this.mRemote.transact(56, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.display.IDisplayManager
            public void updateRefreshRateForVideoScene(int videoState, int videoFps, int videoSessionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(videoState);
                    _data.writeInt(videoFps);
                    _data.writeInt(videoSessionId);
                    this.mRemote.transact(57, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 56;
        }
    }
}
