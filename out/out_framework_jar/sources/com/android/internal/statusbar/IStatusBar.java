package com.android.internal.statusbar;

import android.app.ITransientNotificationCallback;
import android.content.ComponentName;
import android.graphics.drawable.Icon;
import android.hardware.biometrics.IBiometricContextListener;
import android.hardware.biometrics.IBiometricSysuiReceiver;
import android.hardware.biometrics.PromptInfo;
import android.hardware.fingerprint.IUdfpsHbmListener;
import android.media.INearbyMediaDevicesProvider;
import android.media.MediaRoute2Info;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.InsetsVisibilities;
import com.android.internal.statusbar.IAddTileResultCallback;
import com.android.internal.statusbar.IUndoMediaTransferCallback;
import com.android.internal.view.AppearanceRegion;
/* loaded from: classes4.dex */
public interface IStatusBar extends IInterface {
    void abortTransient(int i, int[] iArr) throws RemoteException;

    void addQsTile(ComponentName componentName) throws RemoteException;

    void animateCollapsePanels() throws RemoteException;

    void animateExpandNotificationsPanel() throws RemoteException;

    void animateExpandSettingsPanel(String str) throws RemoteException;

    void appTransitionCancelled(int i) throws RemoteException;

    void appTransitionFinished(int i) throws RemoteException;

    void appTransitionPending(int i) throws RemoteException;

    void appTransitionStarting(int i, long j, long j2) throws RemoteException;

    void cancelPreloadRecentApps() throws RemoteException;

    void cancelRequestAddTile(String str) throws RemoteException;

    void clickQsTile(ComponentName componentName) throws RemoteException;

    void disable(int i, int i2, int i3) throws RemoteException;

    void dismissInattentiveSleepWarning(boolean z) throws RemoteException;

    void dismissKeyboardShortcutsMenu() throws RemoteException;

    void handleSystemKey(int i) throws RemoteException;

    void handleWindowManagerLoggingCommand(String[] strArr, ParcelFileDescriptor parcelFileDescriptor) throws RemoteException;

    void hideAuthenticationDialog(long j) throws RemoteException;

    void hideInCallUIStatuBar() throws RemoteException;

    void hideRecentApps(boolean z, boolean z2) throws RemoteException;

    void hideToast(String str, IBinder iBinder) throws RemoteException;

    void onBiometricAuthenticated(int i) throws RemoteException;

    void onBiometricError(int i, int i2, int i3) throws RemoteException;

    void onBiometricHelp(int i, String str) throws RemoteException;

    void onCameraLaunchGestureDetected(int i) throws RemoteException;

    void onDisplayReady(int i) throws RemoteException;

    void onEmergencyActionLaunchGestureDetected() throws RemoteException;

    void onProposedRotationChanged(int i, boolean z) throws RemoteException;

    void onRecentsAnimationStateChanged(boolean z) throws RemoteException;

    void onSystemBarAttributesChanged(int i, int i2, AppearanceRegion[] appearanceRegionArr, boolean z, int i3, InsetsVisibilities insetsVisibilities, String str) throws RemoteException;

    void passThroughShellCommand(String[] strArr, ParcelFileDescriptor parcelFileDescriptor) throws RemoteException;

    void preloadRecentApps() throws RemoteException;

    void registerNearbyMediaDevicesProvider(INearbyMediaDevicesProvider iNearbyMediaDevicesProvider) throws RemoteException;

    void remQsTile(ComponentName componentName) throws RemoteException;

    void removeIcon(String str) throws RemoteException;

    void requestAddTile(ComponentName componentName, CharSequence charSequence, CharSequence charSequence2, Icon icon, IAddTileResultCallback iAddTileResultCallback) throws RemoteException;

    void requestTileServiceListeningState(ComponentName componentName) throws RemoteException;

    void requestWindowMagnificationConnection(boolean z) throws RemoteException;

    void runGcForTest() throws RemoteException;

    void setBiometicContextListener(IBiometricContextListener iBiometricContextListener) throws RemoteException;

    void setIcon(String str, StatusBarIcon statusBarIcon) throws RemoteException;

    void setImeWindowStatus(int i, IBinder iBinder, int i2, int i3, boolean z) throws RemoteException;

    void setNavigationBarLumaSamplingEnabled(int i, boolean z) throws RemoteException;

    void setTopAppHidesStatusBar(boolean z) throws RemoteException;

    void setUdfpsHbmListener(IUdfpsHbmListener iUdfpsHbmListener) throws RemoteException;

    void setWindowState(int i, int i2, int i3) throws RemoteException;

    void showAssistDisclosure() throws RemoteException;

    void showAuthenticationDialog(PromptInfo promptInfo, IBiometricSysuiReceiver iBiometricSysuiReceiver, int[] iArr, boolean z, boolean z2, int i, long j, String str, long j2, int i2) throws RemoteException;

    void showGlobalActionsMenu() throws RemoteException;

    void showInCallUIStatuBar(String str, long j) throws RemoteException;

    void showInattentiveSleepWarning() throws RemoteException;

    void showPictureInPictureMenu() throws RemoteException;

    void showPinningEnterExitToast(boolean z) throws RemoteException;

    void showPinningEscapeToast() throws RemoteException;

    void showRecentApps(boolean z) throws RemoteException;

    void showScreenPinningRequest(int i) throws RemoteException;

    void showShutdownUi(boolean z, String str) throws RemoteException;

    void showToast(int i, String str, IBinder iBinder, CharSequence charSequence, IBinder iBinder2, int i2, ITransientNotificationCallback iTransientNotificationCallback, int i3) throws RemoteException;

    void showTransient(int i, int[] iArr, boolean z) throws RemoteException;

    void showWirelessChargingAnimation(int i) throws RemoteException;

    void startAssist(Bundle bundle) throws RemoteException;

    void startTracing() throws RemoteException;

    void stopTracing() throws RemoteException;

    void suppressAmbientDisplay(boolean z) throws RemoteException;

    void toggleKeyboardShortcutsMenu(int i) throws RemoteException;

    void togglePanel() throws RemoteException;

    void toggleRecentApps() throws RemoteException;

    void toggleSplitScreen() throws RemoteException;

    void unregisterNearbyMediaDevicesProvider(INearbyMediaDevicesProvider iNearbyMediaDevicesProvider) throws RemoteException;

    void updateMediaTapToTransferReceiverDisplay(int i, MediaRoute2Info mediaRoute2Info, Icon icon, CharSequence charSequence) throws RemoteException;

    void updateMediaTapToTransferSenderDisplay(int i, MediaRoute2Info mediaRoute2Info, IUndoMediaTransferCallback iUndoMediaTransferCallback) throws RemoteException;

    /* loaded from: classes4.dex */
    public static class Default implements IStatusBar {
        @Override // com.android.internal.statusbar.IStatusBar
        public void setIcon(String slot, StatusBarIcon icon) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void removeIcon(String slot) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void disable(int displayId, int state1, int state2) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void animateExpandNotificationsPanel() throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void animateExpandSettingsPanel(String subPanel) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void animateCollapsePanels() throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void togglePanel() throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void showWirelessChargingAnimation(int batteryLevel) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void setImeWindowStatus(int displayId, IBinder token, int vis, int backDisposition, boolean showImeSwitcher) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void setWindowState(int display, int window, int state) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void showRecentApps(boolean triggeredFromAltTab) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void toggleRecentApps() throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void toggleSplitScreen() throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void preloadRecentApps() throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void cancelPreloadRecentApps() throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void showScreenPinningRequest(int taskId) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void dismissKeyboardShortcutsMenu() throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void toggleKeyboardShortcutsMenu(int deviceId) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void appTransitionPending(int displayId) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void appTransitionCancelled(int displayId) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void appTransitionStarting(int displayId, long statusBarAnimationsStartTime, long statusBarAnimationsDuration) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void appTransitionFinished(int displayId) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void showAssistDisclosure() throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void startAssist(Bundle args) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void onCameraLaunchGestureDetected(int source) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void onEmergencyActionLaunchGestureDetected() throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void showPictureInPictureMenu() throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void showGlobalActionsMenu() throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void onProposedRotationChanged(int rotation, boolean isValid) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void setTopAppHidesStatusBar(boolean hidesStatusBar) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void addQsTile(ComponentName tile) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void remQsTile(ComponentName tile) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void clickQsTile(ComponentName tile) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void handleSystemKey(int key) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void showPinningEnterExitToast(boolean entering) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void showPinningEscapeToast() throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void showShutdownUi(boolean isReboot, String reason) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void showAuthenticationDialog(PromptInfo promptInfo, IBiometricSysuiReceiver sysuiReceiver, int[] sensorIds, boolean credentialAllowed, boolean requireConfirmation, int userId, long operationId, String opPackageName, long requestId, int multiSensorConfig) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void onBiometricAuthenticated(int modality) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void onBiometricHelp(int modality, String message) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void onBiometricError(int modality, int error, int vendorCode) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void hideAuthenticationDialog(long requestId) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void setBiometicContextListener(IBiometricContextListener listener) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void setUdfpsHbmListener(IUdfpsHbmListener listener) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void onDisplayReady(int displayId) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void onRecentsAnimationStateChanged(boolean running) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void onSystemBarAttributesChanged(int displayId, int appearance, AppearanceRegion[] appearanceRegions, boolean navbarColorManagedByIme, int behavior, InsetsVisibilities requestedVisibilities, String packageName) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void showTransient(int displayId, int[] types, boolean isGestureOnSystemBar) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void abortTransient(int displayId, int[] types) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void showInattentiveSleepWarning() throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void dismissInattentiveSleepWarning(boolean animated) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void showToast(int uid, String packageName, IBinder token, CharSequence text, IBinder windowToken, int duration, ITransientNotificationCallback callback, int displayId) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void hideToast(String packageName, IBinder token) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void startTracing() throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void stopTracing() throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void handleWindowManagerLoggingCommand(String[] args, ParcelFileDescriptor outFd) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void suppressAmbientDisplay(boolean suppress) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void requestWindowMagnificationConnection(boolean connect) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void passThroughShellCommand(String[] args, ParcelFileDescriptor pfd) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void setNavigationBarLumaSamplingEnabled(int displayId, boolean enable) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void runGcForTest() throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void showInCallUIStatuBar(String callState, long baseTime) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void hideInCallUIStatuBar() throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void requestTileServiceListeningState(ComponentName componentName) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void requestAddTile(ComponentName componentName, CharSequence appName, CharSequence label, Icon icon, IAddTileResultCallback callback) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void cancelRequestAddTile(String packageName) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void updateMediaTapToTransferSenderDisplay(int displayState, MediaRoute2Info routeInfo, IUndoMediaTransferCallback undoCallback) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void updateMediaTapToTransferReceiverDisplay(int displayState, MediaRoute2Info routeInfo, Icon appIcon, CharSequence appName) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void registerNearbyMediaDevicesProvider(INearbyMediaDevicesProvider provider) throws RemoteException {
        }

        @Override // com.android.internal.statusbar.IStatusBar
        public void unregisterNearbyMediaDevicesProvider(INearbyMediaDevicesProvider provider) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class Stub extends Binder implements IStatusBar {
        public static final String DESCRIPTOR = "com.android.internal.statusbar.IStatusBar";
        static final int TRANSACTION_abortTransient = 50;
        static final int TRANSACTION_addQsTile = 32;
        static final int TRANSACTION_animateCollapsePanels = 6;
        static final int TRANSACTION_animateExpandNotificationsPanel = 4;
        static final int TRANSACTION_animateExpandSettingsPanel = 5;
        static final int TRANSACTION_appTransitionCancelled = 21;
        static final int TRANSACTION_appTransitionFinished = 23;
        static final int TRANSACTION_appTransitionPending = 20;
        static final int TRANSACTION_appTransitionStarting = 22;
        static final int TRANSACTION_cancelPreloadRecentApps = 16;
        static final int TRANSACTION_cancelRequestAddTile = 67;
        static final int TRANSACTION_clickQsTile = 34;
        static final int TRANSACTION_disable = 3;
        static final int TRANSACTION_dismissInattentiveSleepWarning = 52;
        static final int TRANSACTION_dismissKeyboardShortcutsMenu = 18;
        static final int TRANSACTION_handleSystemKey = 35;
        static final int TRANSACTION_handleWindowManagerLoggingCommand = 57;
        static final int TRANSACTION_hideAuthenticationDialog = 43;
        static final int TRANSACTION_hideInCallUIStatuBar = 64;
        static final int TRANSACTION_hideRecentApps = 12;
        static final int TRANSACTION_hideToast = 54;
        static final int TRANSACTION_onBiometricAuthenticated = 40;
        static final int TRANSACTION_onBiometricError = 42;
        static final int TRANSACTION_onBiometricHelp = 41;
        static final int TRANSACTION_onCameraLaunchGestureDetected = 26;
        static final int TRANSACTION_onDisplayReady = 46;
        static final int TRANSACTION_onEmergencyActionLaunchGestureDetected = 27;
        static final int TRANSACTION_onProposedRotationChanged = 30;
        static final int TRANSACTION_onRecentsAnimationStateChanged = 47;
        static final int TRANSACTION_onSystemBarAttributesChanged = 48;
        static final int TRANSACTION_passThroughShellCommand = 60;
        static final int TRANSACTION_preloadRecentApps = 15;
        static final int TRANSACTION_registerNearbyMediaDevicesProvider = 70;
        static final int TRANSACTION_remQsTile = 33;
        static final int TRANSACTION_removeIcon = 2;
        static final int TRANSACTION_requestAddTile = 66;
        static final int TRANSACTION_requestTileServiceListeningState = 65;
        static final int TRANSACTION_requestWindowMagnificationConnection = 59;
        static final int TRANSACTION_runGcForTest = 62;
        static final int TRANSACTION_setBiometicContextListener = 44;
        static final int TRANSACTION_setIcon = 1;
        static final int TRANSACTION_setImeWindowStatus = 9;
        static final int TRANSACTION_setNavigationBarLumaSamplingEnabled = 61;
        static final int TRANSACTION_setTopAppHidesStatusBar = 31;
        static final int TRANSACTION_setUdfpsHbmListener = 45;
        static final int TRANSACTION_setWindowState = 10;
        static final int TRANSACTION_showAssistDisclosure = 24;
        static final int TRANSACTION_showAuthenticationDialog = 39;
        static final int TRANSACTION_showGlobalActionsMenu = 29;
        static final int TRANSACTION_showInCallUIStatuBar = 63;
        static final int TRANSACTION_showInattentiveSleepWarning = 51;
        static final int TRANSACTION_showPictureInPictureMenu = 28;
        static final int TRANSACTION_showPinningEnterExitToast = 36;
        static final int TRANSACTION_showPinningEscapeToast = 37;
        static final int TRANSACTION_showRecentApps = 11;
        static final int TRANSACTION_showScreenPinningRequest = 17;
        static final int TRANSACTION_showShutdownUi = 38;
        static final int TRANSACTION_showToast = 53;
        static final int TRANSACTION_showTransient = 49;
        static final int TRANSACTION_showWirelessChargingAnimation = 8;
        static final int TRANSACTION_startAssist = 25;
        static final int TRANSACTION_startTracing = 55;
        static final int TRANSACTION_stopTracing = 56;
        static final int TRANSACTION_suppressAmbientDisplay = 58;
        static final int TRANSACTION_toggleKeyboardShortcutsMenu = 19;
        static final int TRANSACTION_togglePanel = 7;
        static final int TRANSACTION_toggleRecentApps = 13;
        static final int TRANSACTION_toggleSplitScreen = 14;
        static final int TRANSACTION_unregisterNearbyMediaDevicesProvider = 71;
        static final int TRANSACTION_updateMediaTapToTransferReceiverDisplay = 69;
        static final int TRANSACTION_updateMediaTapToTransferSenderDisplay = 68;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IStatusBar asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IStatusBar)) {
                return (IStatusBar) iin;
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
                    return "setIcon";
                case 2:
                    return "removeIcon";
                case 3:
                    return "disable";
                case 4:
                    return "animateExpandNotificationsPanel";
                case 5:
                    return "animateExpandSettingsPanel";
                case 6:
                    return "animateCollapsePanels";
                case 7:
                    return "togglePanel";
                case 8:
                    return "showWirelessChargingAnimation";
                case 9:
                    return "setImeWindowStatus";
                case 10:
                    return "setWindowState";
                case 11:
                    return "showRecentApps";
                case 12:
                    return "hideRecentApps";
                case 13:
                    return "toggleRecentApps";
                case 14:
                    return "toggleSplitScreen";
                case 15:
                    return "preloadRecentApps";
                case 16:
                    return "cancelPreloadRecentApps";
                case 17:
                    return "showScreenPinningRequest";
                case 18:
                    return "dismissKeyboardShortcutsMenu";
                case 19:
                    return "toggleKeyboardShortcutsMenu";
                case 20:
                    return "appTransitionPending";
                case 21:
                    return "appTransitionCancelled";
                case 22:
                    return "appTransitionStarting";
                case 23:
                    return "appTransitionFinished";
                case 24:
                    return "showAssistDisclosure";
                case 25:
                    return "startAssist";
                case 26:
                    return "onCameraLaunchGestureDetected";
                case 27:
                    return "onEmergencyActionLaunchGestureDetected";
                case 28:
                    return "showPictureInPictureMenu";
                case 29:
                    return "showGlobalActionsMenu";
                case 30:
                    return "onProposedRotationChanged";
                case 31:
                    return "setTopAppHidesStatusBar";
                case 32:
                    return "addQsTile";
                case 33:
                    return "remQsTile";
                case 34:
                    return "clickQsTile";
                case 35:
                    return "handleSystemKey";
                case 36:
                    return "showPinningEnterExitToast";
                case 37:
                    return "showPinningEscapeToast";
                case 38:
                    return "showShutdownUi";
                case 39:
                    return "showAuthenticationDialog";
                case 40:
                    return "onBiometricAuthenticated";
                case 41:
                    return "onBiometricHelp";
                case 42:
                    return "onBiometricError";
                case 43:
                    return "hideAuthenticationDialog";
                case 44:
                    return "setBiometicContextListener";
                case 45:
                    return "setUdfpsHbmListener";
                case 46:
                    return "onDisplayReady";
                case 47:
                    return "onRecentsAnimationStateChanged";
                case 48:
                    return "onSystemBarAttributesChanged";
                case 49:
                    return "showTransient";
                case 50:
                    return "abortTransient";
                case 51:
                    return "showInattentiveSleepWarning";
                case 52:
                    return "dismissInattentiveSleepWarning";
                case 53:
                    return "showToast";
                case 54:
                    return "hideToast";
                case 55:
                    return "startTracing";
                case 56:
                    return "stopTracing";
                case 57:
                    return "handleWindowManagerLoggingCommand";
                case 58:
                    return "suppressAmbientDisplay";
                case 59:
                    return "requestWindowMagnificationConnection";
                case 60:
                    return "passThroughShellCommand";
                case 61:
                    return "setNavigationBarLumaSamplingEnabled";
                case 62:
                    return "runGcForTest";
                case 63:
                    return "showInCallUIStatuBar";
                case 64:
                    return "hideInCallUIStatuBar";
                case 65:
                    return "requestTileServiceListeningState";
                case 66:
                    return "requestAddTile";
                case 67:
                    return "cancelRequestAddTile";
                case 68:
                    return "updateMediaTapToTransferSenderDisplay";
                case 69:
                    return "updateMediaTapToTransferReceiverDisplay";
                case 70:
                    return "registerNearbyMediaDevicesProvider";
                case 71:
                    return "unregisterNearbyMediaDevicesProvider";
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
                            String _arg0 = data.readString();
                            StatusBarIcon _arg1 = (StatusBarIcon) data.readTypedObject(StatusBarIcon.CREATOR);
                            data.enforceNoDataAvail();
                            setIcon(_arg0, _arg1);
                            return true;
                        case 2:
                            String _arg02 = data.readString();
                            data.enforceNoDataAvail();
                            removeIcon(_arg02);
                            return true;
                        case 3:
                            int _arg03 = data.readInt();
                            int _arg12 = data.readInt();
                            int _arg2 = data.readInt();
                            data.enforceNoDataAvail();
                            disable(_arg03, _arg12, _arg2);
                            return true;
                        case 4:
                            animateExpandNotificationsPanel();
                            return true;
                        case 5:
                            String _arg04 = data.readString();
                            data.enforceNoDataAvail();
                            animateExpandSettingsPanel(_arg04);
                            return true;
                        case 6:
                            animateCollapsePanels();
                            return true;
                        case 7:
                            togglePanel();
                            return true;
                        case 8:
                            int _arg05 = data.readInt();
                            data.enforceNoDataAvail();
                            showWirelessChargingAnimation(_arg05);
                            return true;
                        case 9:
                            int _arg06 = data.readInt();
                            IBinder _arg13 = data.readStrongBinder();
                            int _arg22 = data.readInt();
                            int _arg3 = data.readInt();
                            boolean _arg4 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setImeWindowStatus(_arg06, _arg13, _arg22, _arg3, _arg4);
                            return true;
                        case 10:
                            int _arg07 = data.readInt();
                            int _arg14 = data.readInt();
                            int _arg23 = data.readInt();
                            data.enforceNoDataAvail();
                            setWindowState(_arg07, _arg14, _arg23);
                            return true;
                        case 11:
                            boolean _arg08 = data.readBoolean();
                            data.enforceNoDataAvail();
                            showRecentApps(_arg08);
                            return true;
                        case 12:
                            boolean _arg09 = data.readBoolean();
                            boolean _arg15 = data.readBoolean();
                            data.enforceNoDataAvail();
                            hideRecentApps(_arg09, _arg15);
                            return true;
                        case 13:
                            toggleRecentApps();
                            return true;
                        case 14:
                            toggleSplitScreen();
                            return true;
                        case 15:
                            preloadRecentApps();
                            return true;
                        case 16:
                            cancelPreloadRecentApps();
                            return true;
                        case 17:
                            int _arg010 = data.readInt();
                            data.enforceNoDataAvail();
                            showScreenPinningRequest(_arg010);
                            return true;
                        case 18:
                            dismissKeyboardShortcutsMenu();
                            return true;
                        case 19:
                            int _arg011 = data.readInt();
                            data.enforceNoDataAvail();
                            toggleKeyboardShortcutsMenu(_arg011);
                            return true;
                        case 20:
                            int _arg012 = data.readInt();
                            data.enforceNoDataAvail();
                            appTransitionPending(_arg012);
                            return true;
                        case 21:
                            int _arg013 = data.readInt();
                            data.enforceNoDataAvail();
                            appTransitionCancelled(_arg013);
                            return true;
                        case 22:
                            int _arg014 = data.readInt();
                            long _arg16 = data.readLong();
                            long _arg24 = data.readLong();
                            data.enforceNoDataAvail();
                            appTransitionStarting(_arg014, _arg16, _arg24);
                            return true;
                        case 23:
                            int _arg015 = data.readInt();
                            data.enforceNoDataAvail();
                            appTransitionFinished(_arg015);
                            return true;
                        case 24:
                            showAssistDisclosure();
                            return true;
                        case 25:
                            Bundle _arg016 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            startAssist(_arg016);
                            return true;
                        case 26:
                            int _arg017 = data.readInt();
                            data.enforceNoDataAvail();
                            onCameraLaunchGestureDetected(_arg017);
                            return true;
                        case 27:
                            onEmergencyActionLaunchGestureDetected();
                            return true;
                        case 28:
                            showPictureInPictureMenu();
                            return true;
                        case 29:
                            showGlobalActionsMenu();
                            return true;
                        case 30:
                            int _arg018 = data.readInt();
                            boolean _arg17 = data.readBoolean();
                            data.enforceNoDataAvail();
                            onProposedRotationChanged(_arg018, _arg17);
                            return true;
                        case 31:
                            boolean _arg019 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setTopAppHidesStatusBar(_arg019);
                            return true;
                        case 32:
                            ComponentName _arg020 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            addQsTile(_arg020);
                            return true;
                        case 33:
                            ComponentName _arg021 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            remQsTile(_arg021);
                            return true;
                        case 34:
                            ComponentName _arg022 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            clickQsTile(_arg022);
                            return true;
                        case 35:
                            int _arg023 = data.readInt();
                            data.enforceNoDataAvail();
                            handleSystemKey(_arg023);
                            return true;
                        case 36:
                            boolean _arg024 = data.readBoolean();
                            data.enforceNoDataAvail();
                            showPinningEnterExitToast(_arg024);
                            return true;
                        case 37:
                            showPinningEscapeToast();
                            return true;
                        case 38:
                            boolean _arg025 = data.readBoolean();
                            String _arg18 = data.readString();
                            data.enforceNoDataAvail();
                            showShutdownUi(_arg025, _arg18);
                            return true;
                        case 39:
                            PromptInfo _arg026 = (PromptInfo) data.readTypedObject(PromptInfo.CREATOR);
                            IBiometricSysuiReceiver _arg19 = IBiometricSysuiReceiver.Stub.asInterface(data.readStrongBinder());
                            int[] _arg25 = data.createIntArray();
                            boolean _arg32 = data.readBoolean();
                            boolean _arg42 = data.readBoolean();
                            int _arg5 = data.readInt();
                            long _arg6 = data.readLong();
                            String _arg7 = data.readString();
                            long _arg8 = data.readLong();
                            int _arg9 = data.readInt();
                            data.enforceNoDataAvail();
                            showAuthenticationDialog(_arg026, _arg19, _arg25, _arg32, _arg42, _arg5, _arg6, _arg7, _arg8, _arg9);
                            return true;
                        case 40:
                            int _arg027 = data.readInt();
                            data.enforceNoDataAvail();
                            onBiometricAuthenticated(_arg027);
                            return true;
                        case 41:
                            int _arg028 = data.readInt();
                            String _arg110 = data.readString();
                            data.enforceNoDataAvail();
                            onBiometricHelp(_arg028, _arg110);
                            return true;
                        case 42:
                            int _arg029 = data.readInt();
                            int _arg111 = data.readInt();
                            int _arg26 = data.readInt();
                            data.enforceNoDataAvail();
                            onBiometricError(_arg029, _arg111, _arg26);
                            return true;
                        case 43:
                            long _arg030 = data.readLong();
                            data.enforceNoDataAvail();
                            hideAuthenticationDialog(_arg030);
                            return true;
                        case 44:
                            IBiometricContextListener _arg031 = IBiometricContextListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            setBiometicContextListener(_arg031);
                            return true;
                        case 45:
                            IUdfpsHbmListener _arg032 = IUdfpsHbmListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            setUdfpsHbmListener(_arg032);
                            return true;
                        case 46:
                            int _arg033 = data.readInt();
                            data.enforceNoDataAvail();
                            onDisplayReady(_arg033);
                            return true;
                        case 47:
                            boolean _arg034 = data.readBoolean();
                            data.enforceNoDataAvail();
                            onRecentsAnimationStateChanged(_arg034);
                            return true;
                        case 48:
                            int _arg035 = data.readInt();
                            int _arg112 = data.readInt();
                            AppearanceRegion[] _arg27 = (AppearanceRegion[]) data.createTypedArray(AppearanceRegion.CREATOR);
                            boolean _arg33 = data.readBoolean();
                            int _arg43 = data.readInt();
                            InsetsVisibilities _arg52 = (InsetsVisibilities) data.readTypedObject(InsetsVisibilities.CREATOR);
                            String _arg62 = data.readString();
                            data.enforceNoDataAvail();
                            onSystemBarAttributesChanged(_arg035, _arg112, _arg27, _arg33, _arg43, _arg52, _arg62);
                            return true;
                        case 49:
                            int _arg036 = data.readInt();
                            int[] _arg113 = data.createIntArray();
                            boolean _arg28 = data.readBoolean();
                            data.enforceNoDataAvail();
                            showTransient(_arg036, _arg113, _arg28);
                            return true;
                        case 50:
                            int _arg037 = data.readInt();
                            int[] _arg114 = data.createIntArray();
                            data.enforceNoDataAvail();
                            abortTransient(_arg037, _arg114);
                            return true;
                        case 51:
                            showInattentiveSleepWarning();
                            return true;
                        case 52:
                            boolean _arg038 = data.readBoolean();
                            data.enforceNoDataAvail();
                            dismissInattentiveSleepWarning(_arg038);
                            return true;
                        case 53:
                            int _arg039 = data.readInt();
                            String _arg115 = data.readString();
                            IBinder _arg29 = data.readStrongBinder();
                            CharSequence _arg34 = (CharSequence) data.readTypedObject(TextUtils.CHAR_SEQUENCE_CREATOR);
                            IBinder _arg44 = data.readStrongBinder();
                            int _arg53 = data.readInt();
                            ITransientNotificationCallback _arg63 = ITransientNotificationCallback.Stub.asInterface(data.readStrongBinder());
                            int _arg72 = data.readInt();
                            data.enforceNoDataAvail();
                            showToast(_arg039, _arg115, _arg29, _arg34, _arg44, _arg53, _arg63, _arg72);
                            return true;
                        case 54:
                            String _arg040 = data.readString();
                            IBinder _arg116 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            hideToast(_arg040, _arg116);
                            return true;
                        case 55:
                            startTracing();
                            return true;
                        case 56:
                            stopTracing();
                            return true;
                        case 57:
                            String[] _arg041 = data.createStringArray();
                            ParcelFileDescriptor _arg117 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
                            data.enforceNoDataAvail();
                            handleWindowManagerLoggingCommand(_arg041, _arg117);
                            return true;
                        case 58:
                            boolean _arg042 = data.readBoolean();
                            data.enforceNoDataAvail();
                            suppressAmbientDisplay(_arg042);
                            return true;
                        case 59:
                            boolean _arg043 = data.readBoolean();
                            data.enforceNoDataAvail();
                            requestWindowMagnificationConnection(_arg043);
                            return true;
                        case 60:
                            String[] _arg044 = data.createStringArray();
                            ParcelFileDescriptor _arg118 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
                            data.enforceNoDataAvail();
                            passThroughShellCommand(_arg044, _arg118);
                            return true;
                        case 61:
                            int _arg045 = data.readInt();
                            boolean _arg119 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setNavigationBarLumaSamplingEnabled(_arg045, _arg119);
                            return true;
                        case 62:
                            runGcForTest();
                            return true;
                        case 63:
                            String _arg046 = data.readString();
                            long _arg120 = data.readLong();
                            data.enforceNoDataAvail();
                            showInCallUIStatuBar(_arg046, _arg120);
                            return true;
                        case 64:
                            hideInCallUIStatuBar();
                            return true;
                        case 65:
                            ComponentName _arg047 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            requestTileServiceListeningState(_arg047);
                            return true;
                        case 66:
                            ComponentName _arg048 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            CharSequence _arg121 = (CharSequence) data.readTypedObject(TextUtils.CHAR_SEQUENCE_CREATOR);
                            CharSequence _arg210 = (CharSequence) data.readTypedObject(TextUtils.CHAR_SEQUENCE_CREATOR);
                            Icon _arg35 = (Icon) data.readTypedObject(Icon.CREATOR);
                            IAddTileResultCallback _arg45 = IAddTileResultCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            requestAddTile(_arg048, _arg121, _arg210, _arg35, _arg45);
                            return true;
                        case 67:
                            String _arg049 = data.readString();
                            data.enforceNoDataAvail();
                            cancelRequestAddTile(_arg049);
                            return true;
                        case 68:
                            int _arg050 = data.readInt();
                            MediaRoute2Info _arg122 = (MediaRoute2Info) data.readTypedObject(MediaRoute2Info.CREATOR);
                            IUndoMediaTransferCallback _arg211 = IUndoMediaTransferCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            updateMediaTapToTransferSenderDisplay(_arg050, _arg122, _arg211);
                            return true;
                        case 69:
                            int _arg051 = data.readInt();
                            MediaRoute2Info _arg123 = (MediaRoute2Info) data.readTypedObject(MediaRoute2Info.CREATOR);
                            Icon _arg212 = (Icon) data.readTypedObject(Icon.CREATOR);
                            CharSequence _arg36 = (CharSequence) data.readTypedObject(TextUtils.CHAR_SEQUENCE_CREATOR);
                            data.enforceNoDataAvail();
                            updateMediaTapToTransferReceiverDisplay(_arg051, _arg123, _arg212, _arg36);
                            return true;
                        case 70:
                            INearbyMediaDevicesProvider _arg052 = INearbyMediaDevicesProvider.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            registerNearbyMediaDevicesProvider(_arg052);
                            return true;
                        case 71:
                            INearbyMediaDevicesProvider _arg053 = INearbyMediaDevicesProvider.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            unregisterNearbyMediaDevicesProvider(_arg053);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes4.dex */
        private static class Proxy implements IStatusBar {
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

            @Override // com.android.internal.statusbar.IStatusBar
            public void setIcon(String slot, StatusBarIcon icon) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(slot);
                    _data.writeTypedObject(icon, 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void removeIcon(String slot) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(slot);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void disable(int displayId, int state1, int state2) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeInt(state1);
                    _data.writeInt(state2);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void animateExpandNotificationsPanel() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void animateExpandSettingsPanel(String subPanel) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(subPanel);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void animateCollapsePanels() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void togglePanel() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void showWirelessChargingAnimation(int batteryLevel) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(batteryLevel);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void setImeWindowStatus(int displayId, IBinder token, int vis, int backDisposition, boolean showImeSwitcher) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeStrongBinder(token);
                    _data.writeInt(vis);
                    _data.writeInt(backDisposition);
                    _data.writeBoolean(showImeSwitcher);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void setWindowState(int display, int window, int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(display);
                    _data.writeInt(window);
                    _data.writeInt(state);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void showRecentApps(boolean triggeredFromAltTab) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(triggeredFromAltTab);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(triggeredFromAltTab);
                    _data.writeBoolean(triggeredFromHomeKey);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void toggleRecentApps() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void toggleSplitScreen() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void preloadRecentApps() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void cancelPreloadRecentApps() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void showScreenPinningRequest(int taskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(taskId);
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void dismissKeyboardShortcutsMenu() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void toggleKeyboardShortcutsMenu(int deviceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(deviceId);
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void appTransitionPending(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(20, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void appTransitionCancelled(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(21, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void appTransitionStarting(int displayId, long statusBarAnimationsStartTime, long statusBarAnimationsDuration) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeLong(statusBarAnimationsStartTime);
                    _data.writeLong(statusBarAnimationsDuration);
                    this.mRemote.transact(22, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void appTransitionFinished(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(23, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void showAssistDisclosure() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(24, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void startAssist(Bundle args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(args, 0);
                    this.mRemote.transact(25, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void onCameraLaunchGestureDetected(int source) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(source);
                    this.mRemote.transact(26, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void onEmergencyActionLaunchGestureDetected() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(27, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void showPictureInPictureMenu() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(28, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void showGlobalActionsMenu() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(29, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void onProposedRotationChanged(int rotation, boolean isValid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(rotation);
                    _data.writeBoolean(isValid);
                    this.mRemote.transact(30, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void setTopAppHidesStatusBar(boolean hidesStatusBar) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(hidesStatusBar);
                    this.mRemote.transact(31, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void addQsTile(ComponentName tile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(tile, 0);
                    this.mRemote.transact(32, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void remQsTile(ComponentName tile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(tile, 0);
                    this.mRemote.transact(33, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void clickQsTile(ComponentName tile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(tile, 0);
                    this.mRemote.transact(34, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void handleSystemKey(int key) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(key);
                    this.mRemote.transact(35, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void showPinningEnterExitToast(boolean entering) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(entering);
                    this.mRemote.transact(36, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void showPinningEscapeToast() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(37, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void showShutdownUi(boolean isReboot, String reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(isReboot);
                    _data.writeString(reason);
                    this.mRemote.transact(38, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void showAuthenticationDialog(PromptInfo promptInfo, IBiometricSysuiReceiver sysuiReceiver, int[] sensorIds, boolean credentialAllowed, boolean requireConfirmation, int userId, long operationId, String opPackageName, long requestId, int multiSensorConfig) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(promptInfo, 0);
                    _data.writeStrongInterface(sysuiReceiver);
                    try {
                        _data.writeIntArray(sensorIds);
                        try {
                            _data.writeBoolean(credentialAllowed);
                            try {
                                _data.writeBoolean(requireConfirmation);
                            } catch (Throwable th) {
                                th = th;
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(userId);
                        try {
                            _data.writeLong(operationId);
                            try {
                                _data.writeString(opPackageName);
                                try {
                                    _data.writeLong(requestId);
                                    try {
                                        _data.writeInt(multiSensorConfig);
                                        try {
                                            this.mRemote.transact(39, _data, null, 1);
                                            _data.recycle();
                                        } catch (Throwable th4) {
                                            th = th4;
                                            _data.recycle();
                                            throw th;
                                        }
                                    } catch (Throwable th5) {
                                        th = th5;
                                    }
                                } catch (Throwable th6) {
                                    th = th6;
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th7) {
                                th = th7;
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th8) {
                            th = th8;
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th9) {
                        th = th9;
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th10) {
                    th = th10;
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void onBiometricAuthenticated(int modality) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(modality);
                    this.mRemote.transact(40, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void onBiometricHelp(int modality, String message) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(modality);
                    _data.writeString(message);
                    this.mRemote.transact(41, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void onBiometricError(int modality, int error, int vendorCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(modality);
                    _data.writeInt(error);
                    _data.writeInt(vendorCode);
                    this.mRemote.transact(42, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void hideAuthenticationDialog(long requestId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(requestId);
                    this.mRemote.transact(43, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void setBiometicContextListener(IBiometricContextListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(44, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void setUdfpsHbmListener(IUdfpsHbmListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(45, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void onDisplayReady(int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    this.mRemote.transact(46, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void onRecentsAnimationStateChanged(boolean running) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(running);
                    this.mRemote.transact(47, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void onSystemBarAttributesChanged(int displayId, int appearance, AppearanceRegion[] appearanceRegions, boolean navbarColorManagedByIme, int behavior, InsetsVisibilities requestedVisibilities, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeInt(appearance);
                    _data.writeTypedArray(appearanceRegions, 0);
                    _data.writeBoolean(navbarColorManagedByIme);
                    _data.writeInt(behavior);
                    _data.writeTypedObject(requestedVisibilities, 0);
                    _data.writeString(packageName);
                    this.mRemote.transact(48, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void showTransient(int displayId, int[] types, boolean isGestureOnSystemBar) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeIntArray(types);
                    _data.writeBoolean(isGestureOnSystemBar);
                    this.mRemote.transact(49, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void abortTransient(int displayId, int[] types) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeIntArray(types);
                    this.mRemote.transact(50, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void showInattentiveSleepWarning() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(51, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void dismissInattentiveSleepWarning(boolean animated) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(animated);
                    this.mRemote.transact(52, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void showToast(int uid, String packageName, IBinder token, CharSequence text, IBinder windowToken, int duration, ITransientNotificationCallback callback, int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeString(packageName);
                    _data.writeStrongBinder(token);
                    if (text != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(text, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(windowToken);
                    _data.writeInt(duration);
                    _data.writeStrongInterface(callback);
                    _data.writeInt(displayId);
                    this.mRemote.transact(53, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void hideToast(String packageName, IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(54, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void startTracing() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(55, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void stopTracing() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(56, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void handleWindowManagerLoggingCommand(String[] args, ParcelFileDescriptor outFd) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringArray(args);
                    _data.writeTypedObject(outFd, 0);
                    this.mRemote.transact(57, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void suppressAmbientDisplay(boolean suppress) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(suppress);
                    this.mRemote.transact(58, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void requestWindowMagnificationConnection(boolean connect) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(connect);
                    this.mRemote.transact(59, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void passThroughShellCommand(String[] args, ParcelFileDescriptor pfd) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringArray(args);
                    _data.writeTypedObject(pfd, 0);
                    this.mRemote.transact(60, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void setNavigationBarLumaSamplingEnabled(int displayId, boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeBoolean(enable);
                    this.mRemote.transact(61, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void runGcForTest() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(62, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void showInCallUIStatuBar(String callState, long baseTime) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callState);
                    _data.writeLong(baseTime);
                    this.mRemote.transact(63, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void hideInCallUIStatuBar() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(64, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void requestTileServiceListeningState(ComponentName componentName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(componentName, 0);
                    this.mRemote.transact(65, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void requestAddTile(ComponentName componentName, CharSequence appName, CharSequence label, Icon icon, IAddTileResultCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(componentName, 0);
                    if (appName != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(appName, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (label != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(label, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeTypedObject(icon, 0);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(66, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void cancelRequestAddTile(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(67, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void updateMediaTapToTransferSenderDisplay(int displayState, MediaRoute2Info routeInfo, IUndoMediaTransferCallback undoCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayState);
                    _data.writeTypedObject(routeInfo, 0);
                    _data.writeStrongInterface(undoCallback);
                    this.mRemote.transact(68, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void updateMediaTapToTransferReceiverDisplay(int displayState, MediaRoute2Info routeInfo, Icon appIcon, CharSequence appName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayState);
                    _data.writeTypedObject(routeInfo, 0);
                    _data.writeTypedObject(appIcon, 0);
                    if (appName != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(appName, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(69, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void registerNearbyMediaDevicesProvider(INearbyMediaDevicesProvider provider) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(provider);
                    this.mRemote.transact(70, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.statusbar.IStatusBar
            public void unregisterNearbyMediaDevicesProvider(INearbyMediaDevicesProvider provider) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(provider);
                    this.mRemote.transact(71, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 70;
        }
    }
}
