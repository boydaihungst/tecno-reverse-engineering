package android.app;

import android.app.ActivityManager;
import android.app.ICompatCameraControlCallback;
import android.app.IRequestFinishCallback;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.RemoteAnimationDefinition;
import android.window.SizeConfigurationBuckets;
import com.android.internal.policy.IKeyguardDismissCallback;
/* loaded from: classes.dex */
public interface IActivityClientController extends IInterface {
    public static final String DESCRIPTOR = "android.app.IActivityClientController";

    void activityDestroyed(IBinder iBinder) throws RemoteException;

    void activityIdle(IBinder iBinder, Configuration configuration, boolean z) throws RemoteException;

    void activityLocalRelaunch(IBinder iBinder) throws RemoteException;

    void activityPaused(IBinder iBinder) throws RemoteException;

    void activityRelaunched(IBinder iBinder) throws RemoteException;

    void activityResumed(IBinder iBinder, boolean z) throws RemoteException;

    void activityStopped(IBinder iBinder, Bundle bundle, PersistableBundle persistableBundle, CharSequence charSequence) throws RemoteException;

    void activityTopResumedStateLost() throws RemoteException;

    boolean convertFromTranslucent(IBinder iBinder) throws RemoteException;

    boolean convertToTranslucent(IBinder iBinder, Bundle bundle) throws RemoteException;

    void dismissKeyguard(IBinder iBinder, IKeyguardDismissCallback iKeyguardDismissCallback, CharSequence charSequence) throws RemoteException;

    boolean enterPictureInPictureMode(IBinder iBinder, PictureInPictureParams pictureInPictureParams) throws RemoteException;

    boolean finishActivity(IBinder iBinder, int i, Intent intent, int i2) throws RemoteException;

    boolean finishActivityAffinity(IBinder iBinder) throws RemoteException;

    void finishSubActivity(IBinder iBinder, String str, int i) throws RemoteException;

    IBinder getActivityTokenBelow(IBinder iBinder) throws RemoteException;

    ComponentName getCallingActivity(IBinder iBinder) throws RemoteException;

    String getCallingPackage(IBinder iBinder) throws RemoteException;

    int getDisplayId(IBinder iBinder) throws RemoteException;

    String getLaunchedFromPackage(IBinder iBinder) throws RemoteException;

    int getLaunchedFromUid(IBinder iBinder) throws RemoteException;

    int getRequestedOrientation(IBinder iBinder) throws RemoteException;

    int getTaskForActivity(IBinder iBinder, boolean z) throws RemoteException;

    int getTaskSize(IBinder iBinder) throws RemoteException;

    void invalidateHomeTaskSnapshot(IBinder iBinder) throws RemoteException;

    boolean isImmersive(IBinder iBinder) throws RemoteException;

    boolean isRootVoiceInteraction(IBinder iBinder) throws RemoteException;

    boolean isTopOfTask(IBinder iBinder) throws RemoteException;

    boolean moveActivityTaskToBack(IBinder iBinder, boolean z) throws RemoteException;

    boolean navigateUpTo(IBinder iBinder, Intent intent, int i, Intent intent2) throws RemoteException;

    void onBackPressedOnTaskRoot(IBinder iBinder, IRequestFinishCallback iRequestFinishCallback) throws RemoteException;

    void overridePendingTransition(IBinder iBinder, String str, int i, int i2, int i3) throws RemoteException;

    void registerRemoteAnimations(IBinder iBinder, RemoteAnimationDefinition remoteAnimationDefinition) throws RemoteException;

    boolean releaseActivityInstance(IBinder iBinder) throws RemoteException;

    void reportActivityFullyDrawn(IBinder iBinder, boolean z) throws RemoteException;

    void reportSizeConfigurations(IBinder iBinder, SizeConfigurationBuckets sizeConfigurationBuckets) throws RemoteException;

    void requestCompatCameraControl(IBinder iBinder, boolean z, boolean z2, ICompatCameraControlCallback iCompatCameraControlCallback) throws RemoteException;

    void setImmersive(IBinder iBinder, boolean z) throws RemoteException;

    void setInheritShowWhenLocked(IBinder iBinder, boolean z) throws RemoteException;

    void setPictureInPictureParams(IBinder iBinder, PictureInPictureParams pictureInPictureParams) throws RemoteException;

    void setRecentsScreenshotEnabled(IBinder iBinder, boolean z) throws RemoteException;

    void setRequestedOrientation(IBinder iBinder, int i) throws RemoteException;

    void setShouldDockBigOverlays(IBinder iBinder, boolean z) throws RemoteException;

    void setShowWhenLocked(IBinder iBinder, boolean z) throws RemoteException;

    void setTaskDescription(IBinder iBinder, ActivityManager.TaskDescription taskDescription) throws RemoteException;

    void setTurnScreenOn(IBinder iBinder, boolean z) throws RemoteException;

    int setVrMode(IBinder iBinder, boolean z, ComponentName componentName) throws RemoteException;

    boolean shouldUpRecreateTask(IBinder iBinder, String str) throws RemoteException;

    boolean showAssistFromActivity(IBinder iBinder, Bundle bundle) throws RemoteException;

    void showLockTaskEscapeMessage(IBinder iBinder) throws RemoteException;

    void splashScreenAttached(IBinder iBinder) throws RemoteException;

    void startLocalVoiceInteraction(IBinder iBinder, Bundle bundle) throws RemoteException;

    void startLockTaskModeByToken(IBinder iBinder) throws RemoteException;

    void stopLocalVoiceInteraction(IBinder iBinder) throws RemoteException;

    void stopLockTaskModeByToken(IBinder iBinder) throws RemoteException;

    void toggleFreeformWindowingMode(IBinder iBinder) throws RemoteException;

    void unregisterRemoteAnimations(IBinder iBinder) throws RemoteException;

    boolean willActivityBeVisible(IBinder iBinder) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IActivityClientController {
        @Override // android.app.IActivityClientController
        public void activityIdle(IBinder token, Configuration config, boolean stopProfiling) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void activityResumed(IBinder token, boolean handleSplashScreenExit) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void activityTopResumedStateLost() throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void activityPaused(IBinder token) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void activityStopped(IBinder token, Bundle state, PersistableBundle persistentState, CharSequence description) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void activityDestroyed(IBinder token) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void activityLocalRelaunch(IBinder token) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void activityRelaunched(IBinder token) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void reportSizeConfigurations(IBinder token, SizeConfigurationBuckets sizeConfigurations) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public boolean moveActivityTaskToBack(IBinder token, boolean nonRoot) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityClientController
        public boolean shouldUpRecreateTask(IBinder token, String destAffinity) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityClientController
        public boolean navigateUpTo(IBinder token, Intent target, int resultCode, Intent resultData) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityClientController
        public boolean releaseActivityInstance(IBinder token) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityClientController
        public boolean finishActivity(IBinder token, int code, Intent data, int finishTask) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityClientController
        public boolean finishActivityAffinity(IBinder token) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityClientController
        public void finishSubActivity(IBinder token, String resultWho, int requestCode) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public boolean isTopOfTask(IBinder token) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityClientController
        public boolean willActivityBeVisible(IBinder token) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityClientController
        public int getDisplayId(IBinder activityToken) throws RemoteException {
            return 0;
        }

        @Override // android.app.IActivityClientController
        public int getTaskForActivity(IBinder token, boolean onlyRoot) throws RemoteException {
            return 0;
        }

        @Override // android.app.IActivityClientController
        public IBinder getActivityTokenBelow(IBinder token) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityClientController
        public ComponentName getCallingActivity(IBinder token) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityClientController
        public String getCallingPackage(IBinder token) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityClientController
        public int getLaunchedFromUid(IBinder token) throws RemoteException {
            return 0;
        }

        @Override // android.app.IActivityClientController
        public String getLaunchedFromPackage(IBinder token) throws RemoteException {
            return null;
        }

        @Override // android.app.IActivityClientController
        public void setRequestedOrientation(IBinder token, int requestedOrientation) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public int getRequestedOrientation(IBinder token) throws RemoteException {
            return 0;
        }

        @Override // android.app.IActivityClientController
        public boolean convertFromTranslucent(IBinder token) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityClientController
        public boolean convertToTranslucent(IBinder token, Bundle options) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityClientController
        public boolean isImmersive(IBinder token) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityClientController
        public void setImmersive(IBinder token, boolean immersive) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public boolean enterPictureInPictureMode(IBinder token, PictureInPictureParams params) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityClientController
        public void setPictureInPictureParams(IBinder token, PictureInPictureParams params) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void setShouldDockBigOverlays(IBinder token, boolean shouldDockBigOverlays) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void toggleFreeformWindowingMode(IBinder token) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void startLockTaskModeByToken(IBinder token) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void stopLockTaskModeByToken(IBinder token) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void showLockTaskEscapeMessage(IBinder token) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void setTaskDescription(IBinder token, ActivityManager.TaskDescription values) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public boolean showAssistFromActivity(IBinder token, Bundle args) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityClientController
        public boolean isRootVoiceInteraction(IBinder token) throws RemoteException {
            return false;
        }

        @Override // android.app.IActivityClientController
        public void startLocalVoiceInteraction(IBinder token, Bundle options) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void stopLocalVoiceInteraction(IBinder token) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void setShowWhenLocked(IBinder token, boolean showWhenLocked) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void setInheritShowWhenLocked(IBinder token, boolean setInheritShownWhenLocked) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void setTurnScreenOn(IBinder token, boolean turnScreenOn) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void reportActivityFullyDrawn(IBinder token, boolean restoredFromBundle) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void overridePendingTransition(IBinder token, String packageName, int enterAnim, int exitAnim, int backgroundColor) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public int setVrMode(IBinder token, boolean enabled, ComponentName packageName) throws RemoteException {
            return 0;
        }

        @Override // android.app.IActivityClientController
        public void setRecentsScreenshotEnabled(IBinder token, boolean enabled) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void invalidateHomeTaskSnapshot(IBinder homeToken) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void dismissKeyguard(IBinder token, IKeyguardDismissCallback callback, CharSequence message) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void registerRemoteAnimations(IBinder token, RemoteAnimationDefinition definition) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void unregisterRemoteAnimations(IBinder token) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void onBackPressedOnTaskRoot(IBinder activityToken, IRequestFinishCallback callback) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void splashScreenAttached(IBinder token) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public void requestCompatCameraControl(IBinder token, boolean showControl, boolean transformationApplied, ICompatCameraControlCallback callback) throws RemoteException {
        }

        @Override // android.app.IActivityClientController
        public int getTaskSize(IBinder token) throws RemoteException {
            return 0;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IActivityClientController {
        static final int TRANSACTION_activityDestroyed = 6;
        static final int TRANSACTION_activityIdle = 1;
        static final int TRANSACTION_activityLocalRelaunch = 7;
        static final int TRANSACTION_activityPaused = 4;
        static final int TRANSACTION_activityRelaunched = 8;
        static final int TRANSACTION_activityResumed = 2;
        static final int TRANSACTION_activityStopped = 5;
        static final int TRANSACTION_activityTopResumedStateLost = 3;
        static final int TRANSACTION_convertFromTranslucent = 28;
        static final int TRANSACTION_convertToTranslucent = 29;
        static final int TRANSACTION_dismissKeyguard = 52;
        static final int TRANSACTION_enterPictureInPictureMode = 32;
        static final int TRANSACTION_finishActivity = 14;
        static final int TRANSACTION_finishActivityAffinity = 15;
        static final int TRANSACTION_finishSubActivity = 16;
        static final int TRANSACTION_getActivityTokenBelow = 21;
        static final int TRANSACTION_getCallingActivity = 22;
        static final int TRANSACTION_getCallingPackage = 23;
        static final int TRANSACTION_getDisplayId = 19;
        static final int TRANSACTION_getLaunchedFromPackage = 25;
        static final int TRANSACTION_getLaunchedFromUid = 24;
        static final int TRANSACTION_getRequestedOrientation = 27;
        static final int TRANSACTION_getTaskForActivity = 20;
        static final int TRANSACTION_getTaskSize = 58;
        static final int TRANSACTION_invalidateHomeTaskSnapshot = 51;
        static final int TRANSACTION_isImmersive = 30;
        static final int TRANSACTION_isRootVoiceInteraction = 41;
        static final int TRANSACTION_isTopOfTask = 17;
        static final int TRANSACTION_moveActivityTaskToBack = 10;
        static final int TRANSACTION_navigateUpTo = 12;
        static final int TRANSACTION_onBackPressedOnTaskRoot = 55;
        static final int TRANSACTION_overridePendingTransition = 48;
        static final int TRANSACTION_registerRemoteAnimations = 53;
        static final int TRANSACTION_releaseActivityInstance = 13;
        static final int TRANSACTION_reportActivityFullyDrawn = 47;
        static final int TRANSACTION_reportSizeConfigurations = 9;
        static final int TRANSACTION_requestCompatCameraControl = 57;
        static final int TRANSACTION_setImmersive = 31;
        static final int TRANSACTION_setInheritShowWhenLocked = 45;
        static final int TRANSACTION_setPictureInPictureParams = 33;
        static final int TRANSACTION_setRecentsScreenshotEnabled = 50;
        static final int TRANSACTION_setRequestedOrientation = 26;
        static final int TRANSACTION_setShouldDockBigOverlays = 34;
        static final int TRANSACTION_setShowWhenLocked = 44;
        static final int TRANSACTION_setTaskDescription = 39;
        static final int TRANSACTION_setTurnScreenOn = 46;
        static final int TRANSACTION_setVrMode = 49;
        static final int TRANSACTION_shouldUpRecreateTask = 11;
        static final int TRANSACTION_showAssistFromActivity = 40;
        static final int TRANSACTION_showLockTaskEscapeMessage = 38;
        static final int TRANSACTION_splashScreenAttached = 56;
        static final int TRANSACTION_startLocalVoiceInteraction = 42;
        static final int TRANSACTION_startLockTaskModeByToken = 36;
        static final int TRANSACTION_stopLocalVoiceInteraction = 43;
        static final int TRANSACTION_stopLockTaskModeByToken = 37;
        static final int TRANSACTION_toggleFreeformWindowingMode = 35;
        static final int TRANSACTION_unregisterRemoteAnimations = 54;
        static final int TRANSACTION_willActivityBeVisible = 18;

        public Stub() {
            attachInterface(this, IActivityClientController.DESCRIPTOR);
        }

        public static IActivityClientController asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IActivityClientController.DESCRIPTOR);
            if (iin != null && (iin instanceof IActivityClientController)) {
                return (IActivityClientController) iin;
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
                    return "activityIdle";
                case 2:
                    return "activityResumed";
                case 3:
                    return "activityTopResumedStateLost";
                case 4:
                    return "activityPaused";
                case 5:
                    return "activityStopped";
                case 6:
                    return "activityDestroyed";
                case 7:
                    return "activityLocalRelaunch";
                case 8:
                    return "activityRelaunched";
                case 9:
                    return "reportSizeConfigurations";
                case 10:
                    return "moveActivityTaskToBack";
                case 11:
                    return "shouldUpRecreateTask";
                case 12:
                    return "navigateUpTo";
                case 13:
                    return "releaseActivityInstance";
                case 14:
                    return "finishActivity";
                case 15:
                    return "finishActivityAffinity";
                case 16:
                    return "finishSubActivity";
                case 17:
                    return "isTopOfTask";
                case 18:
                    return "willActivityBeVisible";
                case 19:
                    return "getDisplayId";
                case 20:
                    return "getTaskForActivity";
                case 21:
                    return "getActivityTokenBelow";
                case 22:
                    return "getCallingActivity";
                case 23:
                    return "getCallingPackage";
                case 24:
                    return "getLaunchedFromUid";
                case 25:
                    return "getLaunchedFromPackage";
                case 26:
                    return "setRequestedOrientation";
                case 27:
                    return "getRequestedOrientation";
                case 28:
                    return "convertFromTranslucent";
                case 29:
                    return "convertToTranslucent";
                case 30:
                    return "isImmersive";
                case 31:
                    return "setImmersive";
                case 32:
                    return "enterPictureInPictureMode";
                case 33:
                    return "setPictureInPictureParams";
                case 34:
                    return "setShouldDockBigOverlays";
                case 35:
                    return "toggleFreeformWindowingMode";
                case 36:
                    return "startLockTaskModeByToken";
                case 37:
                    return "stopLockTaskModeByToken";
                case 38:
                    return "showLockTaskEscapeMessage";
                case 39:
                    return "setTaskDescription";
                case 40:
                    return "showAssistFromActivity";
                case 41:
                    return "isRootVoiceInteraction";
                case 42:
                    return "startLocalVoiceInteraction";
                case 43:
                    return "stopLocalVoiceInteraction";
                case 44:
                    return "setShowWhenLocked";
                case 45:
                    return "setInheritShowWhenLocked";
                case 46:
                    return "setTurnScreenOn";
                case 47:
                    return "reportActivityFullyDrawn";
                case 48:
                    return "overridePendingTransition";
                case 49:
                    return "setVrMode";
                case 50:
                    return "setRecentsScreenshotEnabled";
                case 51:
                    return "invalidateHomeTaskSnapshot";
                case 52:
                    return "dismissKeyguard";
                case 53:
                    return "registerRemoteAnimations";
                case 54:
                    return "unregisterRemoteAnimations";
                case 55:
                    return "onBackPressedOnTaskRoot";
                case 56:
                    return "splashScreenAttached";
                case 57:
                    return "requestCompatCameraControl";
                case 58:
                    return "getTaskSize";
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
                data.enforceInterface(IActivityClientController.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IActivityClientController.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            IBinder _arg0 = data.readStrongBinder();
                            Configuration _arg1 = (Configuration) data.readTypedObject(Configuration.CREATOR);
                            boolean _arg2 = data.readBoolean();
                            data.enforceNoDataAvail();
                            activityIdle(_arg0, _arg1, _arg2);
                            break;
                        case 2:
                            IBinder _arg02 = data.readStrongBinder();
                            boolean _arg12 = data.readBoolean();
                            data.enforceNoDataAvail();
                            activityResumed(_arg02, _arg12);
                            break;
                        case 3:
                            activityTopResumedStateLost();
                            reply.writeNoException();
                            break;
                        case 4:
                            IBinder _arg03 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            activityPaused(_arg03);
                            reply.writeNoException();
                            break;
                        case 5:
                            IBinder _arg04 = data.readStrongBinder();
                            Bundle _arg13 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            PersistableBundle _arg22 = (PersistableBundle) data.readTypedObject(PersistableBundle.CREATOR);
                            CharSequence _arg3 = (CharSequence) data.readTypedObject(TextUtils.CHAR_SEQUENCE_CREATOR);
                            data.enforceNoDataAvail();
                            activityStopped(_arg04, _arg13, _arg22, _arg3);
                            break;
                        case 6:
                            IBinder _arg05 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            activityDestroyed(_arg05);
                            break;
                        case 7:
                            IBinder _arg06 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            activityLocalRelaunch(_arg06);
                            break;
                        case 8:
                            IBinder _arg07 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            activityRelaunched(_arg07);
                            break;
                        case 9:
                            IBinder _arg08 = data.readStrongBinder();
                            SizeConfigurationBuckets _arg14 = (SizeConfigurationBuckets) data.readTypedObject(SizeConfigurationBuckets.CREATOR);
                            data.enforceNoDataAvail();
                            reportSizeConfigurations(_arg08, _arg14);
                            break;
                        case 10:
                            IBinder _arg09 = data.readStrongBinder();
                            boolean _arg15 = data.readBoolean();
                            data.enforceNoDataAvail();
                            boolean _result = moveActivityTaskToBack(_arg09, _arg15);
                            reply.writeNoException();
                            reply.writeBoolean(_result);
                            break;
                        case 11:
                            IBinder _arg010 = data.readStrongBinder();
                            String _arg16 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result2 = shouldUpRecreateTask(_arg010, _arg16);
                            reply.writeNoException();
                            reply.writeBoolean(_result2);
                            break;
                        case 12:
                            IBinder _arg011 = data.readStrongBinder();
                            Intent _arg17 = (Intent) data.readTypedObject(Intent.CREATOR);
                            int _arg23 = data.readInt();
                            Intent _arg32 = (Intent) data.readTypedObject(Intent.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result3 = navigateUpTo(_arg011, _arg17, _arg23, _arg32);
                            reply.writeNoException();
                            reply.writeBoolean(_result3);
                            break;
                        case 13:
                            IBinder _arg012 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            boolean _result4 = releaseActivityInstance(_arg012);
                            reply.writeNoException();
                            reply.writeBoolean(_result4);
                            break;
                        case 14:
                            IBinder _arg013 = data.readStrongBinder();
                            int _arg18 = data.readInt();
                            Intent _arg24 = (Intent) data.readTypedObject(Intent.CREATOR);
                            int _arg33 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result5 = finishActivity(_arg013, _arg18, _arg24, _arg33);
                            reply.writeNoException();
                            reply.writeBoolean(_result5);
                            break;
                        case 15:
                            IBinder _arg014 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            boolean _result6 = finishActivityAffinity(_arg014);
                            reply.writeNoException();
                            reply.writeBoolean(_result6);
                            break;
                        case 16:
                            IBinder _arg015 = data.readStrongBinder();
                            String _arg19 = data.readString();
                            int _arg25 = data.readInt();
                            data.enforceNoDataAvail();
                            finishSubActivity(_arg015, _arg19, _arg25);
                            reply.writeNoException();
                            break;
                        case 17:
                            IBinder _arg016 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            boolean _result7 = isTopOfTask(_arg016);
                            reply.writeNoException();
                            reply.writeBoolean(_result7);
                            break;
                        case 18:
                            IBinder _arg017 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            boolean _result8 = willActivityBeVisible(_arg017);
                            reply.writeNoException();
                            reply.writeBoolean(_result8);
                            break;
                        case 19:
                            IBinder _arg018 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            int _result9 = getDisplayId(_arg018);
                            reply.writeNoException();
                            reply.writeInt(_result9);
                            break;
                        case 20:
                            IBinder _arg019 = data.readStrongBinder();
                            boolean _arg110 = data.readBoolean();
                            data.enforceNoDataAvail();
                            int _result10 = getTaskForActivity(_arg019, _arg110);
                            reply.writeNoException();
                            reply.writeInt(_result10);
                            break;
                        case 21:
                            IBinder _arg020 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            IBinder _result11 = getActivityTokenBelow(_arg020);
                            reply.writeNoException();
                            reply.writeStrongBinder(_result11);
                            break;
                        case 22:
                            IBinder _arg021 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            ComponentName _result12 = getCallingActivity(_arg021);
                            reply.writeNoException();
                            reply.writeTypedObject(_result12, 1);
                            break;
                        case 23:
                            IBinder _arg022 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            String _result13 = getCallingPackage(_arg022);
                            reply.writeNoException();
                            reply.writeString(_result13);
                            break;
                        case 24:
                            IBinder _arg023 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            int _result14 = getLaunchedFromUid(_arg023);
                            reply.writeNoException();
                            reply.writeInt(_result14);
                            break;
                        case 25:
                            IBinder _arg024 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            String _result15 = getLaunchedFromPackage(_arg024);
                            reply.writeNoException();
                            reply.writeString(_result15);
                            break;
                        case 26:
                            IBinder _arg025 = data.readStrongBinder();
                            int _arg111 = data.readInt();
                            data.enforceNoDataAvail();
                            setRequestedOrientation(_arg025, _arg111);
                            reply.writeNoException();
                            break;
                        case 27:
                            IBinder _arg026 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            int _result16 = getRequestedOrientation(_arg026);
                            reply.writeNoException();
                            reply.writeInt(_result16);
                            break;
                        case 28:
                            IBinder _arg027 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            boolean _result17 = convertFromTranslucent(_arg027);
                            reply.writeNoException();
                            reply.writeBoolean(_result17);
                            break;
                        case 29:
                            IBinder _arg028 = data.readStrongBinder();
                            Bundle _arg112 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result18 = convertToTranslucent(_arg028, _arg112);
                            reply.writeNoException();
                            reply.writeBoolean(_result18);
                            break;
                        case 30:
                            IBinder _arg029 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            boolean _result19 = isImmersive(_arg029);
                            reply.writeNoException();
                            reply.writeBoolean(_result19);
                            break;
                        case 31:
                            IBinder _arg030 = data.readStrongBinder();
                            boolean _arg113 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setImmersive(_arg030, _arg113);
                            reply.writeNoException();
                            break;
                        case 32:
                            IBinder _arg031 = data.readStrongBinder();
                            PictureInPictureParams _arg114 = (PictureInPictureParams) data.readTypedObject(PictureInPictureParams.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result20 = enterPictureInPictureMode(_arg031, _arg114);
                            reply.writeNoException();
                            reply.writeBoolean(_result20);
                            break;
                        case 33:
                            IBinder _arg032 = data.readStrongBinder();
                            PictureInPictureParams _arg115 = (PictureInPictureParams) data.readTypedObject(PictureInPictureParams.CREATOR);
                            data.enforceNoDataAvail();
                            setPictureInPictureParams(_arg032, _arg115);
                            reply.writeNoException();
                            break;
                        case 34:
                            IBinder _arg033 = data.readStrongBinder();
                            boolean _arg116 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setShouldDockBigOverlays(_arg033, _arg116);
                            break;
                        case 35:
                            IBinder _arg034 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            toggleFreeformWindowingMode(_arg034);
                            reply.writeNoException();
                            break;
                        case 36:
                            IBinder _arg035 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            startLockTaskModeByToken(_arg035);
                            break;
                        case 37:
                            IBinder _arg036 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            stopLockTaskModeByToken(_arg036);
                            break;
                        case 38:
                            IBinder _arg037 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            showLockTaskEscapeMessage(_arg037);
                            break;
                        case 39:
                            IBinder _arg038 = data.readStrongBinder();
                            ActivityManager.TaskDescription _arg117 = (ActivityManager.TaskDescription) data.readTypedObject(ActivityManager.TaskDescription.CREATOR);
                            data.enforceNoDataAvail();
                            setTaskDescription(_arg038, _arg117);
                            reply.writeNoException();
                            break;
                        case 40:
                            IBinder _arg039 = data.readStrongBinder();
                            Bundle _arg118 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result21 = showAssistFromActivity(_arg039, _arg118);
                            reply.writeNoException();
                            reply.writeBoolean(_result21);
                            break;
                        case 41:
                            IBinder _arg040 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            boolean _result22 = isRootVoiceInteraction(_arg040);
                            reply.writeNoException();
                            reply.writeBoolean(_result22);
                            break;
                        case 42:
                            IBinder _arg041 = data.readStrongBinder();
                            Bundle _arg119 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            startLocalVoiceInteraction(_arg041, _arg119);
                            reply.writeNoException();
                            break;
                        case 43:
                            IBinder _arg042 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            stopLocalVoiceInteraction(_arg042);
                            reply.writeNoException();
                            break;
                        case 44:
                            IBinder _arg043 = data.readStrongBinder();
                            boolean _arg120 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setShowWhenLocked(_arg043, _arg120);
                            break;
                        case 45:
                            IBinder _arg044 = data.readStrongBinder();
                            boolean _arg121 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setInheritShowWhenLocked(_arg044, _arg121);
                            break;
                        case 46:
                            IBinder _arg045 = data.readStrongBinder();
                            boolean _arg122 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setTurnScreenOn(_arg045, _arg122);
                            break;
                        case 47:
                            IBinder _arg046 = data.readStrongBinder();
                            boolean _arg123 = data.readBoolean();
                            data.enforceNoDataAvail();
                            reportActivityFullyDrawn(_arg046, _arg123);
                            break;
                        case 48:
                            IBinder _arg047 = data.readStrongBinder();
                            String _arg124 = data.readString();
                            int _arg26 = data.readInt();
                            int _arg34 = data.readInt();
                            int _arg4 = data.readInt();
                            data.enforceNoDataAvail();
                            overridePendingTransition(_arg047, _arg124, _arg26, _arg34, _arg4);
                            reply.writeNoException();
                            break;
                        case 49:
                            IBinder _arg048 = data.readStrongBinder();
                            boolean _arg125 = data.readBoolean();
                            ComponentName _arg27 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            int _result23 = setVrMode(_arg048, _arg125, _arg27);
                            reply.writeNoException();
                            reply.writeInt(_result23);
                            break;
                        case 50:
                            IBinder _arg049 = data.readStrongBinder();
                            boolean _arg126 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setRecentsScreenshotEnabled(_arg049, _arg126);
                            break;
                        case 51:
                            IBinder _arg050 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            invalidateHomeTaskSnapshot(_arg050);
                            reply.writeNoException();
                            break;
                        case 52:
                            IBinder _arg051 = data.readStrongBinder();
                            IKeyguardDismissCallback _arg127 = IKeyguardDismissCallback.Stub.asInterface(data.readStrongBinder());
                            CharSequence _arg28 = (CharSequence) data.readTypedObject(TextUtils.CHAR_SEQUENCE_CREATOR);
                            data.enforceNoDataAvail();
                            dismissKeyguard(_arg051, _arg127, _arg28);
                            reply.writeNoException();
                            break;
                        case 53:
                            IBinder _arg052 = data.readStrongBinder();
                            RemoteAnimationDefinition _arg128 = (RemoteAnimationDefinition) data.readTypedObject(RemoteAnimationDefinition.CREATOR);
                            data.enforceNoDataAvail();
                            registerRemoteAnimations(_arg052, _arg128);
                            reply.writeNoException();
                            break;
                        case 54:
                            IBinder _arg053 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            unregisterRemoteAnimations(_arg053);
                            reply.writeNoException();
                            break;
                        case 55:
                            IBinder _arg054 = data.readStrongBinder();
                            IRequestFinishCallback _arg129 = IRequestFinishCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            onBackPressedOnTaskRoot(_arg054, _arg129);
                            break;
                        case 56:
                            IBinder _arg055 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            splashScreenAttached(_arg055);
                            break;
                        case 57:
                            IBinder _arg056 = data.readStrongBinder();
                            boolean _arg130 = data.readBoolean();
                            boolean _arg29 = data.readBoolean();
                            ICompatCameraControlCallback _arg35 = ICompatCameraControlCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            requestCompatCameraControl(_arg056, _arg130, _arg29, _arg35);
                            break;
                        case 58:
                            IBinder _arg057 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            int _result24 = getTaskSize(_arg057);
                            reply.writeNoException();
                            reply.writeInt(_result24);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements IActivityClientController {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IActivityClientController.DESCRIPTOR;
            }

            @Override // android.app.IActivityClientController
            public void activityIdle(IBinder token, Configuration config, boolean stopProfiling) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(config, 0);
                    _data.writeBoolean(stopProfiling);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void activityResumed(IBinder token, boolean handleSplashScreenExit) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeBoolean(handleSplashScreenExit);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void activityTopResumedStateLost() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void activityPaused(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void activityStopped(IBinder token, Bundle state, PersistableBundle persistentState, CharSequence description) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(state, 0);
                    _data.writeTypedObject(persistentState, 0);
                    if (description != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(description, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void activityDestroyed(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void activityLocalRelaunch(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void activityRelaunched(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void reportSizeConfigurations(IBinder token, SizeConfigurationBuckets sizeConfigurations) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(sizeConfigurations, 0);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public boolean moveActivityTaskToBack(IBinder token, boolean nonRoot) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeBoolean(nonRoot);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public boolean shouldUpRecreateTask(IBinder token, String destAffinity) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeString(destAffinity);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public boolean navigateUpTo(IBinder token, Intent target, int resultCode, Intent resultData) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(target, 0);
                    _data.writeInt(resultCode);
                    _data.writeTypedObject(resultData, 0);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public boolean releaseActivityInstance(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public boolean finishActivity(IBinder token, int code, Intent data, int finishTask) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(code);
                    _data.writeTypedObject(data, 0);
                    _data.writeInt(finishTask);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public boolean finishActivityAffinity(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void finishSubActivity(IBinder token, String resultWho, int requestCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeString(resultWho);
                    _data.writeInt(requestCode);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public boolean isTopOfTask(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public boolean willActivityBeVisible(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public int getDisplayId(IBinder activityToken) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(activityToken);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public int getTaskForActivity(IBinder token, boolean onlyRoot) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeBoolean(onlyRoot);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public IBinder getActivityTokenBelow(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public ComponentName getCallingActivity(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    ComponentName _result = (ComponentName) _reply.readTypedObject(ComponentName.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public String getCallingPackage(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public int getLaunchedFromUid(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public String getLaunchedFromPackage(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void setRequestedOrientation(IBinder token, int requestedOrientation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeInt(requestedOrientation);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public int getRequestedOrientation(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public boolean convertFromTranslucent(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public boolean convertToTranslucent(IBinder token, Bundle options) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(options, 0);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public boolean isImmersive(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void setImmersive(IBinder token, boolean immersive) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeBoolean(immersive);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public boolean enterPictureInPictureMode(IBinder token, PictureInPictureParams params) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(params, 0);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void setPictureInPictureParams(IBinder token, PictureInPictureParams params) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(params, 0);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void setShouldDockBigOverlays(IBinder token, boolean shouldDockBigOverlays) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeBoolean(shouldDockBigOverlays);
                    this.mRemote.transact(34, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void toggleFreeformWindowingMode(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void startLockTaskModeByToken(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(36, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void stopLockTaskModeByToken(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(37, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void showLockTaskEscapeMessage(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(38, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void setTaskDescription(IBinder token, ActivityManager.TaskDescription values) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(values, 0);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public boolean showAssistFromActivity(IBinder token, Bundle args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(args, 0);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public boolean isRootVoiceInteraction(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void startLocalVoiceInteraction(IBinder token, Bundle options) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(options, 0);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void stopLocalVoiceInteraction(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void setShowWhenLocked(IBinder token, boolean showWhenLocked) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeBoolean(showWhenLocked);
                    this.mRemote.transact(44, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void setInheritShowWhenLocked(IBinder token, boolean setInheritShownWhenLocked) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeBoolean(setInheritShownWhenLocked);
                    this.mRemote.transact(45, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void setTurnScreenOn(IBinder token, boolean turnScreenOn) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeBoolean(turnScreenOn);
                    this.mRemote.transact(46, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void reportActivityFullyDrawn(IBinder token, boolean restoredFromBundle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeBoolean(restoredFromBundle);
                    this.mRemote.transact(47, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void overridePendingTransition(IBinder token, String packageName, int enterAnim, int exitAnim, int backgroundColor) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeString(packageName);
                    _data.writeInt(enterAnim);
                    _data.writeInt(exitAnim);
                    _data.writeInt(backgroundColor);
                    this.mRemote.transact(48, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public int setVrMode(IBinder token, boolean enabled, ComponentName packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeBoolean(enabled);
                    _data.writeTypedObject(packageName, 0);
                    this.mRemote.transact(49, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void setRecentsScreenshotEnabled(IBinder token, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(50, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void invalidateHomeTaskSnapshot(IBinder homeToken) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(homeToken);
                    this.mRemote.transact(51, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void dismissKeyguard(IBinder token, IKeyguardDismissCallback callback, CharSequence message) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeStrongInterface(callback);
                    if (message != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(message, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(52, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void registerRemoteAnimations(IBinder token, RemoteAnimationDefinition definition) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(definition, 0);
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void unregisterRemoteAnimations(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(54, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void onBackPressedOnTaskRoot(IBinder activityToken, IRequestFinishCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(activityToken);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(55, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void splashScreenAttached(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(56, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public void requestCompatCameraControl(IBinder token, boolean showControl, boolean transformationApplied, ICompatCameraControlCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeBoolean(showControl);
                    _data.writeBoolean(transformationApplied);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(57, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IActivityClientController
            public int getTaskSize(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IActivityClientController.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(58, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 57;
        }
    }
}
