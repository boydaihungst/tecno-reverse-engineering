package android.app;

import android.app.IInstrumentationWatcher;
import android.app.IUiAutomationConnection;
import android.app.servertransaction.ClientTransaction;
import android.content.AutofillOptions;
import android.content.ComponentName;
import android.content.ContentCaptureOptions;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ParceledListSlice;
import android.content.pm.ProviderInfo;
import android.content.pm.ProviderInfoList;
import android.content.pm.ServiceInfo;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.SharedMemory;
import android.view.autofill.AutofillId;
import android.view.translation.TranslationSpec;
import android.view.translation.UiTranslationSpec;
import com.android.internal.app.IVoiceInteractor;
import java.util.List;
import java.util.Map;
/* loaded from: classes.dex */
public interface IApplicationThread extends IInterface {
    void attachAgent(String str) throws RemoteException;

    void attachStartupAgents(String str) throws RemoteException;

    void bindApplication(String str, ApplicationInfo applicationInfo, String str2, String str3, ProviderInfoList providerInfoList, ComponentName componentName, ProfilerInfo profilerInfo, Bundle bundle, IInstrumentationWatcher iInstrumentationWatcher, IUiAutomationConnection iUiAutomationConnection, int i, boolean z, boolean z2, boolean z3, boolean z4, Configuration configuration, CompatibilityInfo compatibilityInfo, Map map, Bundle bundle2, String str4, AutofillOptions autofillOptions, ContentCaptureOptions contentCaptureOptions, long[] jArr, SharedMemory sharedMemory, long j, long j2) throws RemoteException;

    void clearDnsCache() throws RemoteException;

    void dispatchPackageBroadcast(int i, String[] strArr) throws RemoteException;

    void dumpActivity(ParcelFileDescriptor parcelFileDescriptor, IBinder iBinder, String str, String[] strArr) throws RemoteException;

    void dumpCacheInfo(ParcelFileDescriptor parcelFileDescriptor, String[] strArr) throws RemoteException;

    void dumpDbInfo(ParcelFileDescriptor parcelFileDescriptor, String[] strArr) throws RemoteException;

    void dumpGfxInfo(ParcelFileDescriptor parcelFileDescriptor, String[] strArr) throws RemoteException;

    void dumpHeap(boolean z, boolean z2, boolean z3, String str, ParcelFileDescriptor parcelFileDescriptor, RemoteCallback remoteCallback) throws RemoteException;

    void dumpMemInfo(ParcelFileDescriptor parcelFileDescriptor, Debug.MemoryInfo memoryInfo, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, String[] strArr) throws RemoteException;

    void dumpMemInfoProto(ParcelFileDescriptor parcelFileDescriptor, Debug.MemoryInfo memoryInfo, boolean z, boolean z2, boolean z3, boolean z4, String[] strArr) throws RemoteException;

    void dumpMessage(boolean z) throws RemoteException;

    void dumpProvider(ParcelFileDescriptor parcelFileDescriptor, IBinder iBinder, String[] strArr) throws RemoteException;

    void dumpResources(ParcelFileDescriptor parcelFileDescriptor, RemoteCallback remoteCallback) throws RemoteException;

    void dumpService(ParcelFileDescriptor parcelFileDescriptor, IBinder iBinder, String[] strArr) throws RemoteException;

    void enableActivityThreadLog(boolean z) throws RemoteException;

    void enableProcessMainThreadLooperLog() throws RemoteException;

    void handleTrustStorageUpdate() throws RemoteException;

    void instrumentWithoutRestart(ComponentName componentName, Bundle bundle, IInstrumentationWatcher iInstrumentationWatcher, IUiAutomationConnection iUiAutomationConnection, ApplicationInfo applicationInfo) throws RemoteException;

    void notifyCleartextNetwork(byte[] bArr) throws RemoteException;

    void notifyContentProviderPublishStatus(ContentProviderHolder contentProviderHolder, String str, int i, boolean z) throws RemoteException;

    void performDirectAction(IBinder iBinder, String str, Bundle bundle, RemoteCallback remoteCallback, RemoteCallback remoteCallback2) throws RemoteException;

    void processInBackground() throws RemoteException;

    void profilerControl(boolean z, ProfilerInfo profilerInfo, int i) throws RemoteException;

    void reclaimMemory() throws RemoteException;

    void requestAssistContextExtras(IBinder iBinder, IBinder iBinder2, int i, int i2, int i3) throws RemoteException;

    void requestDirectActions(IBinder iBinder, IVoiceInteractor iVoiceInteractor, RemoteCallback remoteCallback, RemoteCallback remoteCallback2) throws RemoteException;

    void runIsolatedEntryPoint(String str, String[] strArr) throws RemoteException;

    void scheduleApplicationInfoChanged(ApplicationInfo applicationInfo) throws RemoteException;

    void scheduleBindService(IBinder iBinder, Intent intent, boolean z, int i) throws RemoteException;

    void scheduleCrash(String str, int i, Bundle bundle) throws RemoteException;

    void scheduleCreateBackupAgent(ApplicationInfo applicationInfo, CompatibilityInfo compatibilityInfo, int i, int i2, int i3) throws RemoteException;

    void scheduleCreateService(IBinder iBinder, ServiceInfo serviceInfo, CompatibilityInfo compatibilityInfo, int i) throws RemoteException;

    void scheduleDestroyBackupAgent(ApplicationInfo applicationInfo, CompatibilityInfo compatibilityInfo, int i) throws RemoteException;

    void scheduleEnterAnimationComplete(IBinder iBinder) throws RemoteException;

    void scheduleExit() throws RemoteException;

    void scheduleInstallProvider(ProviderInfo providerInfo) throws RemoteException;

    void scheduleLocalVoiceInteractionStarted(IBinder iBinder, IVoiceInteractor iVoiceInteractor) throws RemoteException;

    void scheduleLowMemory() throws RemoteException;

    void scheduleOnNewActivityOptions(IBinder iBinder, Bundle bundle) throws RemoteException;

    void scheduleReceiver(Intent intent, ActivityInfo activityInfo, CompatibilityInfo compatibilityInfo, int i, String str, Bundle bundle, boolean z, int i2, int i3) throws RemoteException;

    void scheduleRegisteredReceiver(IIntentReceiver iIntentReceiver, Intent intent, int i, String str, Bundle bundle, boolean z, boolean z2, int i2, int i3) throws RemoteException;

    void scheduleServiceArgs(IBinder iBinder, ParceledListSlice parceledListSlice) throws RemoteException;

    void scheduleStopService(IBinder iBinder) throws RemoteException;

    void scheduleSuicide() throws RemoteException;

    void scheduleTransaction(ClientTransaction clientTransaction) throws RemoteException;

    void scheduleTranslucentConversionComplete(IBinder iBinder, boolean z) throws RemoteException;

    void scheduleTrimMemory(int i) throws RemoteException;

    void scheduleUnbindService(IBinder iBinder, Intent intent) throws RemoteException;

    void setCoreSettings(Bundle bundle) throws RemoteException;

    void setNetworkBlockSeq(long j) throws RemoteException;

    void setProcessState(int i) throws RemoteException;

    void setSchedulingGroup(int i) throws RemoteException;

    void startBinderTracking() throws RemoteException;

    void stopBinderTrackingAndDump(ParcelFileDescriptor parcelFileDescriptor) throws RemoteException;

    void unstableProviderDied(IBinder iBinder) throws RemoteException;

    void updateHttpProxy() throws RemoteException;

    void updatePackageCompatibilityInfo(String str, CompatibilityInfo compatibilityInfo) throws RemoteException;

    void updateTimePrefs(int i) throws RemoteException;

    void updateTimeZone() throws RemoteException;

    void updateUiTranslationState(IBinder iBinder, int i, TranslationSpec translationSpec, TranslationSpec translationSpec2, List<AutofillId> list, UiTranslationSpec uiTranslationSpec) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IApplicationThread {
        @Override // android.app.IApplicationThread
        public void scheduleReceiver(Intent intent, ActivityInfo info, CompatibilityInfo compatInfo, int resultCode, String data, Bundle extras, boolean sync, int sendingUser, int processState) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void scheduleCreateService(IBinder token, ServiceInfo info, CompatibilityInfo compatInfo, int processState) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void scheduleStopService(IBinder token) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void bindApplication(String packageName, ApplicationInfo info, String sdkSandboxClientAppVolumeUuid, String sdkSandboxClientAppPackage, ProviderInfoList providerList, ComponentName testName, ProfilerInfo profilerInfo, Bundle testArguments, IInstrumentationWatcher testWatcher, IUiAutomationConnection uiAutomationConnection, int debugMode, boolean enableBinderTracking, boolean trackAllocation, boolean restrictedBackupMode, boolean persistent, Configuration config, CompatibilityInfo compatInfo, Map services, Bundle coreSettings, String buildSerial, AutofillOptions autofillOptions, ContentCaptureOptions contentCaptureOptions, long[] disabledCompatChanges, SharedMemory serializedSystemFontMap, long startRequestedElapsedTime, long startRequestedUptime) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void runIsolatedEntryPoint(String entryPoint, String[] entryPointArgs) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void scheduleExit() throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void scheduleServiceArgs(IBinder token, ParceledListSlice args) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void updateTimeZone() throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void processInBackground() throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void scheduleBindService(IBinder token, Intent intent, boolean rebind, int processState) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void scheduleUnbindService(IBinder token, Intent intent) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void dumpService(ParcelFileDescriptor fd, IBinder servicetoken, String[] args) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void scheduleRegisteredReceiver(IIntentReceiver receiver, Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser, int processState) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void scheduleLowMemory() throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void profilerControl(boolean start, ProfilerInfo profilerInfo, int profileType) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void setSchedulingGroup(int group) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void scheduleCreateBackupAgent(ApplicationInfo app, CompatibilityInfo compatInfo, int backupMode, int userId, int operationType) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void scheduleDestroyBackupAgent(ApplicationInfo app, CompatibilityInfo compatInfo, int userId) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void scheduleOnNewActivityOptions(IBinder token, Bundle options) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void scheduleSuicide() throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void dispatchPackageBroadcast(int cmd, String[] packages) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void scheduleCrash(String msg, int typeId, Bundle extras) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void dumpHeap(boolean managed, boolean mallocInfo, boolean runGc, String path, ParcelFileDescriptor fd, RemoteCallback finishCallback) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void dumpActivity(ParcelFileDescriptor fd, IBinder servicetoken, String prefix, String[] args) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void dumpResources(ParcelFileDescriptor fd, RemoteCallback finishCallback) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void clearDnsCache() throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void updateHttpProxy() throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void setCoreSettings(Bundle coreSettings) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void updatePackageCompatibilityInfo(String pkg, CompatibilityInfo info) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void scheduleTrimMemory(int level) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void dumpMemInfo(ParcelFileDescriptor fd, Debug.MemoryInfo mem, boolean checkin, boolean dumpInfo, boolean dumpDalvik, boolean dumpSummaryOnly, boolean dumpUnreachable, String[] args) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void dumpMemInfoProto(ParcelFileDescriptor fd, Debug.MemoryInfo mem, boolean dumpInfo, boolean dumpDalvik, boolean dumpSummaryOnly, boolean dumpUnreachable, String[] args) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void dumpGfxInfo(ParcelFileDescriptor fd, String[] args) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void dumpCacheInfo(ParcelFileDescriptor fd, String[] args) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void dumpProvider(ParcelFileDescriptor fd, IBinder servicetoken, String[] args) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void dumpDbInfo(ParcelFileDescriptor fd, String[] args) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void unstableProviderDied(IBinder provider) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void requestAssistContextExtras(IBinder activityToken, IBinder requestToken, int requestType, int sessionId, int flags) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void scheduleTranslucentConversionComplete(IBinder token, boolean timeout) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void setProcessState(int state) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void scheduleInstallProvider(ProviderInfo provider) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void updateTimePrefs(int timeFormatPreference) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void scheduleEnterAnimationComplete(IBinder token) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void notifyCleartextNetwork(byte[] firstPacket) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void startBinderTracking() throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void stopBinderTrackingAndDump(ParcelFileDescriptor fd) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void scheduleLocalVoiceInteractionStarted(IBinder token, IVoiceInteractor voiceInteractor) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void handleTrustStorageUpdate() throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void attachAgent(String path) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void attachStartupAgents(String dataDir) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void scheduleApplicationInfoChanged(ApplicationInfo ai) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void setNetworkBlockSeq(long procStateSeq) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void scheduleTransaction(ClientTransaction transaction) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void requestDirectActions(IBinder activityToken, IVoiceInteractor intractor, RemoteCallback cancellationCallback, RemoteCallback callback) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void performDirectAction(IBinder activityToken, String actionId, Bundle arguments, RemoteCallback cancellationCallback, RemoteCallback resultCallback) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void enableActivityThreadLog(boolean isEnable) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void enableProcessMainThreadLooperLog() throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void dumpMessage(boolean all) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void notifyContentProviderPublishStatus(ContentProviderHolder holder, String authorities, int userId, boolean published) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void instrumentWithoutRestart(ComponentName instrumentationName, Bundle instrumentationArgs, IInstrumentationWatcher instrumentationWatcher, IUiAutomationConnection instrumentationUiConnection, ApplicationInfo targetInfo) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void updateUiTranslationState(IBinder activityToken, int state, TranslationSpec sourceSpec, TranslationSpec targetSpec, List<AutofillId> viewIds, UiTranslationSpec uiTranslationSpec) throws RemoteException {
        }

        @Override // android.app.IApplicationThread
        public void reclaimMemory() throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IApplicationThread {
        public static final String DESCRIPTOR = "android.app.IApplicationThread";
        static final int TRANSACTION_attachAgent = 49;
        static final int TRANSACTION_attachStartupAgents = 50;
        static final int TRANSACTION_bindApplication = 4;
        static final int TRANSACTION_clearDnsCache = 26;
        static final int TRANSACTION_dispatchPackageBroadcast = 21;
        static final int TRANSACTION_dumpActivity = 24;
        static final int TRANSACTION_dumpCacheInfo = 34;
        static final int TRANSACTION_dumpDbInfo = 36;
        static final int TRANSACTION_dumpGfxInfo = 33;
        static final int TRANSACTION_dumpHeap = 23;
        static final int TRANSACTION_dumpMemInfo = 31;
        static final int TRANSACTION_dumpMemInfoProto = 32;
        static final int TRANSACTION_dumpMessage = 58;
        static final int TRANSACTION_dumpProvider = 35;
        static final int TRANSACTION_dumpResources = 25;
        static final int TRANSACTION_dumpService = 12;
        static final int TRANSACTION_enableActivityThreadLog = 56;
        static final int TRANSACTION_enableProcessMainThreadLooperLog = 57;
        static final int TRANSACTION_handleTrustStorageUpdate = 48;
        static final int TRANSACTION_instrumentWithoutRestart = 60;
        static final int TRANSACTION_notifyCleartextNetwork = 44;
        static final int TRANSACTION_notifyContentProviderPublishStatus = 59;
        static final int TRANSACTION_performDirectAction = 55;
        static final int TRANSACTION_processInBackground = 9;
        static final int TRANSACTION_profilerControl = 15;
        static final int TRANSACTION_reclaimMemory = 62;
        static final int TRANSACTION_requestAssistContextExtras = 38;
        static final int TRANSACTION_requestDirectActions = 54;
        static final int TRANSACTION_runIsolatedEntryPoint = 5;
        static final int TRANSACTION_scheduleApplicationInfoChanged = 51;
        static final int TRANSACTION_scheduleBindService = 10;
        static final int TRANSACTION_scheduleCrash = 22;
        static final int TRANSACTION_scheduleCreateBackupAgent = 17;
        static final int TRANSACTION_scheduleCreateService = 2;
        static final int TRANSACTION_scheduleDestroyBackupAgent = 18;
        static final int TRANSACTION_scheduleEnterAnimationComplete = 43;
        static final int TRANSACTION_scheduleExit = 6;
        static final int TRANSACTION_scheduleInstallProvider = 41;
        static final int TRANSACTION_scheduleLocalVoiceInteractionStarted = 47;
        static final int TRANSACTION_scheduleLowMemory = 14;
        static final int TRANSACTION_scheduleOnNewActivityOptions = 19;
        static final int TRANSACTION_scheduleReceiver = 1;
        static final int TRANSACTION_scheduleRegisteredReceiver = 13;
        static final int TRANSACTION_scheduleServiceArgs = 7;
        static final int TRANSACTION_scheduleStopService = 3;
        static final int TRANSACTION_scheduleSuicide = 20;
        static final int TRANSACTION_scheduleTransaction = 53;
        static final int TRANSACTION_scheduleTranslucentConversionComplete = 39;
        static final int TRANSACTION_scheduleTrimMemory = 30;
        static final int TRANSACTION_scheduleUnbindService = 11;
        static final int TRANSACTION_setCoreSettings = 28;
        static final int TRANSACTION_setNetworkBlockSeq = 52;
        static final int TRANSACTION_setProcessState = 40;
        static final int TRANSACTION_setSchedulingGroup = 16;
        static final int TRANSACTION_startBinderTracking = 45;
        static final int TRANSACTION_stopBinderTrackingAndDump = 46;
        static final int TRANSACTION_unstableProviderDied = 37;
        static final int TRANSACTION_updateHttpProxy = 27;
        static final int TRANSACTION_updatePackageCompatibilityInfo = 29;
        static final int TRANSACTION_updateTimePrefs = 42;
        static final int TRANSACTION_updateTimeZone = 8;
        static final int TRANSACTION_updateUiTranslationState = 61;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IApplicationThread asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IApplicationThread)) {
                return (IApplicationThread) iin;
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
                    return "scheduleReceiver";
                case 2:
                    return "scheduleCreateService";
                case 3:
                    return "scheduleStopService";
                case 4:
                    return "bindApplication";
                case 5:
                    return "runIsolatedEntryPoint";
                case 6:
                    return "scheduleExit";
                case 7:
                    return "scheduleServiceArgs";
                case 8:
                    return "updateTimeZone";
                case 9:
                    return "processInBackground";
                case 10:
                    return "scheduleBindService";
                case 11:
                    return "scheduleUnbindService";
                case 12:
                    return "dumpService";
                case 13:
                    return "scheduleRegisteredReceiver";
                case 14:
                    return "scheduleLowMemory";
                case 15:
                    return "profilerControl";
                case 16:
                    return "setSchedulingGroup";
                case 17:
                    return "scheduleCreateBackupAgent";
                case 18:
                    return "scheduleDestroyBackupAgent";
                case 19:
                    return "scheduleOnNewActivityOptions";
                case 20:
                    return "scheduleSuicide";
                case 21:
                    return "dispatchPackageBroadcast";
                case 22:
                    return "scheduleCrash";
                case 23:
                    return "dumpHeap";
                case 24:
                    return "dumpActivity";
                case 25:
                    return "dumpResources";
                case 26:
                    return "clearDnsCache";
                case 27:
                    return "updateHttpProxy";
                case 28:
                    return "setCoreSettings";
                case 29:
                    return "updatePackageCompatibilityInfo";
                case 30:
                    return "scheduleTrimMemory";
                case 31:
                    return "dumpMemInfo";
                case 32:
                    return "dumpMemInfoProto";
                case 33:
                    return "dumpGfxInfo";
                case 34:
                    return "dumpCacheInfo";
                case 35:
                    return "dumpProvider";
                case 36:
                    return "dumpDbInfo";
                case 37:
                    return "unstableProviderDied";
                case 38:
                    return "requestAssistContextExtras";
                case 39:
                    return "scheduleTranslucentConversionComplete";
                case 40:
                    return "setProcessState";
                case 41:
                    return "scheduleInstallProvider";
                case 42:
                    return "updateTimePrefs";
                case 43:
                    return "scheduleEnterAnimationComplete";
                case 44:
                    return "notifyCleartextNetwork";
                case 45:
                    return "startBinderTracking";
                case 46:
                    return "stopBinderTrackingAndDump";
                case 47:
                    return "scheduleLocalVoiceInteractionStarted";
                case 48:
                    return "handleTrustStorageUpdate";
                case 49:
                    return "attachAgent";
                case 50:
                    return "attachStartupAgents";
                case 51:
                    return "scheduleApplicationInfoChanged";
                case 52:
                    return "setNetworkBlockSeq";
                case 53:
                    return "scheduleTransaction";
                case 54:
                    return "requestDirectActions";
                case 55:
                    return "performDirectAction";
                case 56:
                    return "enableActivityThreadLog";
                case 57:
                    return "enableProcessMainThreadLooperLog";
                case 58:
                    return "dumpMessage";
                case 59:
                    return "notifyContentProviderPublishStatus";
                case 60:
                    return "instrumentWithoutRestart";
                case 61:
                    return "updateUiTranslationState";
                case 62:
                    return "reclaimMemory";
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
                            Intent _arg0 = (Intent) data.readTypedObject(Intent.CREATOR);
                            ActivityInfo _arg1 = (ActivityInfo) data.readTypedObject(ActivityInfo.CREATOR);
                            CompatibilityInfo _arg2 = (CompatibilityInfo) data.readTypedObject(CompatibilityInfo.CREATOR);
                            int _arg3 = data.readInt();
                            String _arg4 = data.readString();
                            Bundle _arg5 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            boolean _arg6 = data.readBoolean();
                            int _arg7 = data.readInt();
                            int _arg8 = data.readInt();
                            data.enforceNoDataAvail();
                            scheduleReceiver(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7, _arg8);
                            return true;
                        case 2:
                            IBinder _arg02 = data.readStrongBinder();
                            ServiceInfo _arg12 = (ServiceInfo) data.readTypedObject(ServiceInfo.CREATOR);
                            CompatibilityInfo _arg22 = (CompatibilityInfo) data.readTypedObject(CompatibilityInfo.CREATOR);
                            int _arg32 = data.readInt();
                            data.enforceNoDataAvail();
                            scheduleCreateService(_arg02, _arg12, _arg22, _arg32);
                            return true;
                        case 3:
                            IBinder _arg03 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            scheduleStopService(_arg03);
                            return true;
                        case 4:
                            String _arg04 = data.readString();
                            ApplicationInfo _arg13 = (ApplicationInfo) data.readTypedObject(ApplicationInfo.CREATOR);
                            String _arg23 = data.readString();
                            String _arg33 = data.readString();
                            ProviderInfoList _arg42 = (ProviderInfoList) data.readTypedObject(ProviderInfoList.CREATOR);
                            ComponentName _arg52 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            ProfilerInfo _arg62 = (ProfilerInfo) data.readTypedObject(ProfilerInfo.CREATOR);
                            Bundle _arg72 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            IInstrumentationWatcher _arg82 = IInstrumentationWatcher.Stub.asInterface(data.readStrongBinder());
                            IUiAutomationConnection _arg9 = IUiAutomationConnection.Stub.asInterface(data.readStrongBinder());
                            int _arg10 = data.readInt();
                            boolean _arg11 = data.readBoolean();
                            boolean _arg122 = data.readBoolean();
                            boolean _arg132 = data.readBoolean();
                            boolean _arg14 = data.readBoolean();
                            Configuration _arg15 = (Configuration) data.readTypedObject(Configuration.CREATOR);
                            CompatibilityInfo _arg16 = (CompatibilityInfo) data.readTypedObject(CompatibilityInfo.CREATOR);
                            ClassLoader cl = getClass().getClassLoader();
                            Map _arg17 = data.readHashMap(cl);
                            Bundle _arg18 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            String _arg19 = data.readString();
                            AutofillOptions _arg20 = (AutofillOptions) data.readTypedObject(AutofillOptions.CREATOR);
                            ContentCaptureOptions _arg21 = (ContentCaptureOptions) data.readTypedObject(ContentCaptureOptions.CREATOR);
                            long[] _arg222 = data.createLongArray();
                            SharedMemory _arg232 = (SharedMemory) data.readTypedObject(SharedMemory.CREATOR);
                            long _arg24 = data.readLong();
                            long _arg25 = data.readLong();
                            data.enforceNoDataAvail();
                            bindApplication(_arg04, _arg13, _arg23, _arg33, _arg42, _arg52, _arg62, _arg72, _arg82, _arg9, _arg10, _arg11, _arg122, _arg132, _arg14, _arg15, _arg16, _arg17, _arg18, _arg19, _arg20, _arg21, _arg222, _arg232, _arg24, _arg25);
                            return true;
                        case 5:
                            String _arg05 = data.readString();
                            String[] _arg110 = data.createStringArray();
                            data.enforceNoDataAvail();
                            runIsolatedEntryPoint(_arg05, _arg110);
                            return true;
                        case 6:
                            scheduleExit();
                            return true;
                        case 7:
                            IBinder _arg06 = data.readStrongBinder();
                            ParceledListSlice _arg111 = (ParceledListSlice) data.readTypedObject(ParceledListSlice.CREATOR);
                            data.enforceNoDataAvail();
                            scheduleServiceArgs(_arg06, _arg111);
                            return true;
                        case 8:
                            updateTimeZone();
                            return true;
                        case 9:
                            processInBackground();
                            return true;
                        case 10:
                            IBinder _arg07 = data.readStrongBinder();
                            Intent _arg112 = (Intent) data.readTypedObject(Intent.CREATOR);
                            boolean _arg26 = data.readBoolean();
                            int _arg34 = data.readInt();
                            data.enforceNoDataAvail();
                            scheduleBindService(_arg07, _arg112, _arg26, _arg34);
                            return true;
                        case 11:
                            IBinder _arg08 = data.readStrongBinder();
                            Intent _arg113 = (Intent) data.readTypedObject(Intent.CREATOR);
                            data.enforceNoDataAvail();
                            scheduleUnbindService(_arg08, _arg113);
                            return true;
                        case 12:
                            ParcelFileDescriptor _arg09 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
                            IBinder _arg114 = data.readStrongBinder();
                            String[] _arg27 = data.createStringArray();
                            data.enforceNoDataAvail();
                            dumpService(_arg09, _arg114, _arg27);
                            return true;
                        case 13:
                            IIntentReceiver _arg010 = IIntentReceiver.Stub.asInterface(data.readStrongBinder());
                            Intent _arg115 = (Intent) data.readTypedObject(Intent.CREATOR);
                            int _arg28 = data.readInt();
                            String _arg35 = data.readString();
                            Bundle _arg43 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            boolean _arg53 = data.readBoolean();
                            boolean _arg63 = data.readBoolean();
                            int _arg73 = data.readInt();
                            int _arg83 = data.readInt();
                            data.enforceNoDataAvail();
                            scheduleRegisteredReceiver(_arg010, _arg115, _arg28, _arg35, _arg43, _arg53, _arg63, _arg73, _arg83);
                            return true;
                        case 14:
                            scheduleLowMemory();
                            return true;
                        case 15:
                            boolean _arg011 = data.readBoolean();
                            ProfilerInfo _arg116 = (ProfilerInfo) data.readTypedObject(ProfilerInfo.CREATOR);
                            int _arg29 = data.readInt();
                            data.enforceNoDataAvail();
                            profilerControl(_arg011, _arg116, _arg29);
                            return true;
                        case 16:
                            int _arg012 = data.readInt();
                            data.enforceNoDataAvail();
                            setSchedulingGroup(_arg012);
                            return true;
                        case 17:
                            ApplicationInfo _arg013 = (ApplicationInfo) data.readTypedObject(ApplicationInfo.CREATOR);
                            CompatibilityInfo _arg117 = (CompatibilityInfo) data.readTypedObject(CompatibilityInfo.CREATOR);
                            int _arg210 = data.readInt();
                            int _arg36 = data.readInt();
                            int _arg44 = data.readInt();
                            data.enforceNoDataAvail();
                            scheduleCreateBackupAgent(_arg013, _arg117, _arg210, _arg36, _arg44);
                            return true;
                        case 18:
                            ApplicationInfo _arg014 = (ApplicationInfo) data.readTypedObject(ApplicationInfo.CREATOR);
                            CompatibilityInfo _arg118 = (CompatibilityInfo) data.readTypedObject(CompatibilityInfo.CREATOR);
                            int _arg211 = data.readInt();
                            data.enforceNoDataAvail();
                            scheduleDestroyBackupAgent(_arg014, _arg118, _arg211);
                            return true;
                        case 19:
                            IBinder _arg015 = data.readStrongBinder();
                            Bundle _arg119 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            scheduleOnNewActivityOptions(_arg015, _arg119);
                            return true;
                        case 20:
                            scheduleSuicide();
                            return true;
                        case 21:
                            int _arg016 = data.readInt();
                            String[] _arg120 = data.createStringArray();
                            data.enforceNoDataAvail();
                            dispatchPackageBroadcast(_arg016, _arg120);
                            return true;
                        case 22:
                            String _arg017 = data.readString();
                            int _arg121 = data.readInt();
                            Bundle _arg212 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            scheduleCrash(_arg017, _arg121, _arg212);
                            return true;
                        case 23:
                            boolean _arg018 = data.readBoolean();
                            boolean _arg123 = data.readBoolean();
                            boolean _arg213 = data.readBoolean();
                            String _arg37 = data.readString();
                            ParcelFileDescriptor _arg45 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
                            RemoteCallback _arg54 = (RemoteCallback) data.readTypedObject(RemoteCallback.CREATOR);
                            data.enforceNoDataAvail();
                            dumpHeap(_arg018, _arg123, _arg213, _arg37, _arg45, _arg54);
                            return true;
                        case 24:
                            ParcelFileDescriptor _arg019 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
                            IBinder _arg124 = data.readStrongBinder();
                            String _arg214 = data.readString();
                            String[] _arg38 = data.createStringArray();
                            data.enforceNoDataAvail();
                            dumpActivity(_arg019, _arg124, _arg214, _arg38);
                            return true;
                        case 25:
                            ParcelFileDescriptor _arg020 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
                            RemoteCallback _arg125 = (RemoteCallback) data.readTypedObject(RemoteCallback.CREATOR);
                            data.enforceNoDataAvail();
                            dumpResources(_arg020, _arg125);
                            return true;
                        case 26:
                            clearDnsCache();
                            return true;
                        case 27:
                            updateHttpProxy();
                            return true;
                        case 28:
                            Bundle _arg021 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            setCoreSettings(_arg021);
                            return true;
                        case 29:
                            String _arg022 = data.readString();
                            CompatibilityInfo _arg126 = (CompatibilityInfo) data.readTypedObject(CompatibilityInfo.CREATOR);
                            data.enforceNoDataAvail();
                            updatePackageCompatibilityInfo(_arg022, _arg126);
                            return true;
                        case 30:
                            int _arg023 = data.readInt();
                            data.enforceNoDataAvail();
                            scheduleTrimMemory(_arg023);
                            return true;
                        case 31:
                            ParcelFileDescriptor _arg024 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
                            Debug.MemoryInfo _arg127 = (Debug.MemoryInfo) data.readTypedObject(Debug.MemoryInfo.CREATOR);
                            boolean _arg215 = data.readBoolean();
                            boolean _arg39 = data.readBoolean();
                            boolean _arg46 = data.readBoolean();
                            boolean _arg55 = data.readBoolean();
                            boolean _arg64 = data.readBoolean();
                            String[] _arg74 = data.createStringArray();
                            data.enforceNoDataAvail();
                            dumpMemInfo(_arg024, _arg127, _arg215, _arg39, _arg46, _arg55, _arg64, _arg74);
                            return true;
                        case 32:
                            ParcelFileDescriptor _arg025 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
                            Debug.MemoryInfo _arg128 = (Debug.MemoryInfo) data.readTypedObject(Debug.MemoryInfo.CREATOR);
                            boolean _arg216 = data.readBoolean();
                            boolean _arg310 = data.readBoolean();
                            boolean _arg47 = data.readBoolean();
                            boolean _arg56 = data.readBoolean();
                            String[] _arg65 = data.createStringArray();
                            data.enforceNoDataAvail();
                            dumpMemInfoProto(_arg025, _arg128, _arg216, _arg310, _arg47, _arg56, _arg65);
                            return true;
                        case 33:
                            ParcelFileDescriptor _arg026 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
                            String[] _arg129 = data.createStringArray();
                            data.enforceNoDataAvail();
                            dumpGfxInfo(_arg026, _arg129);
                            return true;
                        case 34:
                            ParcelFileDescriptor _arg027 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
                            String[] _arg130 = data.createStringArray();
                            data.enforceNoDataAvail();
                            dumpCacheInfo(_arg027, _arg130);
                            return true;
                        case 35:
                            ParcelFileDescriptor _arg028 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
                            IBinder _arg131 = data.readStrongBinder();
                            String[] _arg217 = data.createStringArray();
                            data.enforceNoDataAvail();
                            dumpProvider(_arg028, _arg131, _arg217);
                            return true;
                        case 36:
                            ParcelFileDescriptor _arg029 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
                            String[] _arg133 = data.createStringArray();
                            data.enforceNoDataAvail();
                            dumpDbInfo(_arg029, _arg133);
                            return true;
                        case 37:
                            IBinder _arg030 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            unstableProviderDied(_arg030);
                            return true;
                        case 38:
                            IBinder _arg031 = data.readStrongBinder();
                            IBinder _arg134 = data.readStrongBinder();
                            int _arg218 = data.readInt();
                            int _arg311 = data.readInt();
                            int _arg48 = data.readInt();
                            data.enforceNoDataAvail();
                            requestAssistContextExtras(_arg031, _arg134, _arg218, _arg311, _arg48);
                            return true;
                        case 39:
                            IBinder _arg032 = data.readStrongBinder();
                            boolean _arg135 = data.readBoolean();
                            data.enforceNoDataAvail();
                            scheduleTranslucentConversionComplete(_arg032, _arg135);
                            return true;
                        case 40:
                            int _arg033 = data.readInt();
                            data.enforceNoDataAvail();
                            setProcessState(_arg033);
                            return true;
                        case 41:
                            ProviderInfo _arg034 = (ProviderInfo) data.readTypedObject(ProviderInfo.CREATOR);
                            data.enforceNoDataAvail();
                            scheduleInstallProvider(_arg034);
                            return true;
                        case 42:
                            int _arg035 = data.readInt();
                            data.enforceNoDataAvail();
                            updateTimePrefs(_arg035);
                            return true;
                        case 43:
                            IBinder _arg036 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            scheduleEnterAnimationComplete(_arg036);
                            return true;
                        case 44:
                            byte[] _arg037 = data.createByteArray();
                            data.enforceNoDataAvail();
                            notifyCleartextNetwork(_arg037);
                            return true;
                        case 45:
                            startBinderTracking();
                            return true;
                        case 46:
                            ParcelFileDescriptor _arg038 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
                            data.enforceNoDataAvail();
                            stopBinderTrackingAndDump(_arg038);
                            return true;
                        case 47:
                            IBinder _arg039 = data.readStrongBinder();
                            IVoiceInteractor _arg136 = IVoiceInteractor.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            scheduleLocalVoiceInteractionStarted(_arg039, _arg136);
                            return true;
                        case 48:
                            handleTrustStorageUpdate();
                            return true;
                        case 49:
                            String _arg040 = data.readString();
                            data.enforceNoDataAvail();
                            attachAgent(_arg040);
                            return true;
                        case 50:
                            String _arg041 = data.readString();
                            data.enforceNoDataAvail();
                            attachStartupAgents(_arg041);
                            return true;
                        case 51:
                            ApplicationInfo _arg042 = (ApplicationInfo) data.readTypedObject(ApplicationInfo.CREATOR);
                            data.enforceNoDataAvail();
                            scheduleApplicationInfoChanged(_arg042);
                            return true;
                        case 52:
                            long _arg043 = data.readLong();
                            data.enforceNoDataAvail();
                            setNetworkBlockSeq(_arg043);
                            return true;
                        case 53:
                            ClientTransaction _arg044 = (ClientTransaction) data.readTypedObject(ClientTransaction.CREATOR);
                            data.enforceNoDataAvail();
                            scheduleTransaction(_arg044);
                            return true;
                        case 54:
                            IBinder _arg045 = data.readStrongBinder();
                            IVoiceInteractor _arg137 = IVoiceInteractor.Stub.asInterface(data.readStrongBinder());
                            RemoteCallback _arg219 = (RemoteCallback) data.readTypedObject(RemoteCallback.CREATOR);
                            RemoteCallback _arg312 = (RemoteCallback) data.readTypedObject(RemoteCallback.CREATOR);
                            data.enforceNoDataAvail();
                            requestDirectActions(_arg045, _arg137, _arg219, _arg312);
                            return true;
                        case 55:
                            IBinder _arg046 = data.readStrongBinder();
                            String _arg138 = data.readString();
                            Bundle _arg220 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            RemoteCallback _arg313 = (RemoteCallback) data.readTypedObject(RemoteCallback.CREATOR);
                            RemoteCallback _arg49 = (RemoteCallback) data.readTypedObject(RemoteCallback.CREATOR);
                            data.enforceNoDataAvail();
                            performDirectAction(_arg046, _arg138, _arg220, _arg313, _arg49);
                            return true;
                        case 56:
                            boolean _arg047 = data.readBoolean();
                            data.enforceNoDataAvail();
                            enableActivityThreadLog(_arg047);
                            return true;
                        case 57:
                            enableProcessMainThreadLooperLog();
                            return true;
                        case 58:
                            boolean _arg048 = data.readBoolean();
                            data.enforceNoDataAvail();
                            dumpMessage(_arg048);
                            return true;
                        case 59:
                            ContentProviderHolder _arg049 = (ContentProviderHolder) data.readTypedObject(ContentProviderHolder.CREATOR);
                            String _arg139 = data.readString();
                            int _arg221 = data.readInt();
                            boolean _arg314 = data.readBoolean();
                            data.enforceNoDataAvail();
                            notifyContentProviderPublishStatus(_arg049, _arg139, _arg221, _arg314);
                            return true;
                        case 60:
                            ComponentName _arg050 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            Bundle _arg140 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            IInstrumentationWatcher _arg223 = IInstrumentationWatcher.Stub.asInterface(data.readStrongBinder());
                            IUiAutomationConnection _arg315 = IUiAutomationConnection.Stub.asInterface(data.readStrongBinder());
                            ApplicationInfo _arg410 = (ApplicationInfo) data.readTypedObject(ApplicationInfo.CREATOR);
                            data.enforceNoDataAvail();
                            instrumentWithoutRestart(_arg050, _arg140, _arg223, _arg315, _arg410);
                            return true;
                        case 61:
                            IBinder _arg051 = data.readStrongBinder();
                            int _arg141 = data.readInt();
                            TranslationSpec _arg224 = (TranslationSpec) data.readTypedObject(TranslationSpec.CREATOR);
                            TranslationSpec _arg316 = (TranslationSpec) data.readTypedObject(TranslationSpec.CREATOR);
                            List<AutofillId> _arg411 = data.createTypedArrayList(AutofillId.CREATOR);
                            UiTranslationSpec _arg57 = (UiTranslationSpec) data.readTypedObject(UiTranslationSpec.CREATOR);
                            data.enforceNoDataAvail();
                            updateUiTranslationState(_arg051, _arg141, _arg224, _arg316, _arg411, _arg57);
                            return true;
                        case 62:
                            reclaimMemory();
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements IApplicationThread {
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

            @Override // android.app.IApplicationThread
            public void scheduleReceiver(Intent intent, ActivityInfo info, CompatibilityInfo compatInfo, int resultCode, String data, Bundle extras, boolean sync, int sendingUser, int processState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(intent, 0);
                    _data.writeTypedObject(info, 0);
                    _data.writeTypedObject(compatInfo, 0);
                    _data.writeInt(resultCode);
                    _data.writeString(data);
                    _data.writeTypedObject(extras, 0);
                    _data.writeBoolean(sync);
                    _data.writeInt(sendingUser);
                    _data.writeInt(processState);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void scheduleCreateService(IBinder token, ServiceInfo info, CompatibilityInfo compatInfo, int processState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(info, 0);
                    _data.writeTypedObject(compatInfo, 0);
                    _data.writeInt(processState);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void scheduleStopService(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void bindApplication(String packageName, ApplicationInfo info, String sdkSandboxClientAppVolumeUuid, String sdkSandboxClientAppPackage, ProviderInfoList providerList, ComponentName testName, ProfilerInfo profilerInfo, Bundle testArguments, IInstrumentationWatcher testWatcher, IUiAutomationConnection uiAutomationConnection, int debugMode, boolean enableBinderTracking, boolean trackAllocation, boolean restrictedBackupMode, boolean persistent, Configuration config, CompatibilityInfo compatInfo, Map services, Bundle coreSettings, String buildSerial, AutofillOptions autofillOptions, ContentCaptureOptions contentCaptureOptions, long[] disabledCompatChanges, SharedMemory serializedSystemFontMap, long startRequestedElapsedTime, long startRequestedUptime) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeTypedObject(info, 0);
                    _data.writeString(sdkSandboxClientAppVolumeUuid);
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    _data.writeString(sdkSandboxClientAppPackage);
                    try {
                        _data.writeTypedObject(providerList, 0);
                        try {
                            _data.writeTypedObject(testName, 0);
                            try {
                                _data.writeTypedObject(profilerInfo, 0);
                                try {
                                    _data.writeTypedObject(testArguments, 0);
                                    try {
                                        _data.writeStrongInterface(testWatcher);
                                        try {
                                            _data.writeStrongInterface(uiAutomationConnection);
                                            try {
                                                _data.writeInt(debugMode);
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
                                    } catch (Throwable th4) {
                                        th = th4;
                                        _data.recycle();
                                        throw th;
                                    }
                                } catch (Throwable th5) {
                                    th = th5;
                                    _data.recycle();
                                    throw th;
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
                    try {
                        _data.writeBoolean(enableBinderTracking);
                        try {
                            _data.writeBoolean(trackAllocation);
                            try {
                                _data.writeBoolean(restrictedBackupMode);
                                _data.writeBoolean(persistent);
                                _data.writeTypedObject(config, 0);
                                _data.writeTypedObject(compatInfo, 0);
                                _data.writeMap(services);
                                _data.writeTypedObject(coreSettings, 0);
                                _data.writeString(buildSerial);
                                _data.writeTypedObject(autofillOptions, 0);
                                _data.writeTypedObject(contentCaptureOptions, 0);
                                _data.writeLongArray(disabledCompatChanges);
                                _data.writeTypedObject(serializedSystemFontMap, 0);
                                _data.writeLong(startRequestedElapsedTime);
                                _data.writeLong(startRequestedUptime);
                                this.mRemote.transact(4, _data, null, 1);
                                _data.recycle();
                            } catch (Throwable th9) {
                                th = th9;
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th10) {
                            th = th10;
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th11) {
                        th = th11;
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th12) {
                    th = th12;
                    _data.recycle();
                    throw th;
                }
            }

            @Override // android.app.IApplicationThread
            public void runIsolatedEntryPoint(String entryPoint, String[] entryPointArgs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(entryPoint);
                    _data.writeStringArray(entryPointArgs);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void scheduleExit() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void scheduleServiceArgs(IBinder token, ParceledListSlice args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(args, 0);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void updateTimeZone() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void processInBackground() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void scheduleBindService(IBinder token, Intent intent, boolean rebind, int processState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(intent, 0);
                    _data.writeBoolean(rebind);
                    _data.writeInt(processState);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void scheduleUnbindService(IBinder token, Intent intent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(intent, 0);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void dumpService(ParcelFileDescriptor fd, IBinder servicetoken, String[] args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(fd, 0);
                    _data.writeStrongBinder(servicetoken);
                    _data.writeStringArray(args);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void scheduleRegisteredReceiver(IIntentReceiver receiver, Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser, int processState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(receiver);
                    _data.writeTypedObject(intent, 0);
                    _data.writeInt(resultCode);
                    _data.writeString(data);
                    _data.writeTypedObject(extras, 0);
                    _data.writeBoolean(ordered);
                    _data.writeBoolean(sticky);
                    _data.writeInt(sendingUser);
                    _data.writeInt(processState);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void scheduleLowMemory() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void profilerControl(boolean start, ProfilerInfo profilerInfo, int profileType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(start);
                    _data.writeTypedObject(profilerInfo, 0);
                    _data.writeInt(profileType);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void setSchedulingGroup(int group) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(group);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void scheduleCreateBackupAgent(ApplicationInfo app, CompatibilityInfo compatInfo, int backupMode, int userId, int operationType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(app, 0);
                    _data.writeTypedObject(compatInfo, 0);
                    _data.writeInt(backupMode);
                    _data.writeInt(userId);
                    _data.writeInt(operationType);
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void scheduleDestroyBackupAgent(ApplicationInfo app, CompatibilityInfo compatInfo, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(app, 0);
                    _data.writeTypedObject(compatInfo, 0);
                    _data.writeInt(userId);
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void scheduleOnNewActivityOptions(IBinder token, Bundle options) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(options, 0);
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void scheduleSuicide() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(20, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void dispatchPackageBroadcast(int cmd, String[] packages) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(cmd);
                    _data.writeStringArray(packages);
                    this.mRemote.transact(21, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void scheduleCrash(String msg, int typeId, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(msg);
                    _data.writeInt(typeId);
                    _data.writeTypedObject(extras, 0);
                    this.mRemote.transact(22, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void dumpHeap(boolean managed, boolean mallocInfo, boolean runGc, String path, ParcelFileDescriptor fd, RemoteCallback finishCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(managed);
                    _data.writeBoolean(mallocInfo);
                    _data.writeBoolean(runGc);
                    _data.writeString(path);
                    _data.writeTypedObject(fd, 0);
                    _data.writeTypedObject(finishCallback, 0);
                    this.mRemote.transact(23, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void dumpActivity(ParcelFileDescriptor fd, IBinder servicetoken, String prefix, String[] args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(fd, 0);
                    _data.writeStrongBinder(servicetoken);
                    _data.writeString(prefix);
                    _data.writeStringArray(args);
                    this.mRemote.transact(24, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void dumpResources(ParcelFileDescriptor fd, RemoteCallback finishCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(fd, 0);
                    _data.writeTypedObject(finishCallback, 0);
                    this.mRemote.transact(25, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void clearDnsCache() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(26, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void updateHttpProxy() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(27, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void setCoreSettings(Bundle coreSettings) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(coreSettings, 0);
                    this.mRemote.transact(28, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void updatePackageCompatibilityInfo(String pkg, CompatibilityInfo info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeTypedObject(info, 0);
                    this.mRemote.transact(29, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void scheduleTrimMemory(int level) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(level);
                    this.mRemote.transact(30, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void dumpMemInfo(ParcelFileDescriptor fd, Debug.MemoryInfo mem, boolean checkin, boolean dumpInfo, boolean dumpDalvik, boolean dumpSummaryOnly, boolean dumpUnreachable, String[] args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(fd, 0);
                    _data.writeTypedObject(mem, 0);
                    _data.writeBoolean(checkin);
                    _data.writeBoolean(dumpInfo);
                    _data.writeBoolean(dumpDalvik);
                    _data.writeBoolean(dumpSummaryOnly);
                    _data.writeBoolean(dumpUnreachable);
                    _data.writeStringArray(args);
                    this.mRemote.transact(31, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void dumpMemInfoProto(ParcelFileDescriptor fd, Debug.MemoryInfo mem, boolean dumpInfo, boolean dumpDalvik, boolean dumpSummaryOnly, boolean dumpUnreachable, String[] args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(fd, 0);
                    _data.writeTypedObject(mem, 0);
                    _data.writeBoolean(dumpInfo);
                    _data.writeBoolean(dumpDalvik);
                    _data.writeBoolean(dumpSummaryOnly);
                    _data.writeBoolean(dumpUnreachable);
                    _data.writeStringArray(args);
                    this.mRemote.transact(32, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void dumpGfxInfo(ParcelFileDescriptor fd, String[] args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(fd, 0);
                    _data.writeStringArray(args);
                    this.mRemote.transact(33, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void dumpCacheInfo(ParcelFileDescriptor fd, String[] args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(fd, 0);
                    _data.writeStringArray(args);
                    this.mRemote.transact(34, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void dumpProvider(ParcelFileDescriptor fd, IBinder servicetoken, String[] args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(fd, 0);
                    _data.writeStrongBinder(servicetoken);
                    _data.writeStringArray(args);
                    this.mRemote.transact(35, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void dumpDbInfo(ParcelFileDescriptor fd, String[] args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(fd, 0);
                    _data.writeStringArray(args);
                    this.mRemote.transact(36, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void unstableProviderDied(IBinder provider) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(provider);
                    this.mRemote.transact(37, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void requestAssistContextExtras(IBinder activityToken, IBinder requestToken, int requestType, int sessionId, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(activityToken);
                    _data.writeStrongBinder(requestToken);
                    _data.writeInt(requestType);
                    _data.writeInt(sessionId);
                    _data.writeInt(flags);
                    this.mRemote.transact(38, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void scheduleTranslucentConversionComplete(IBinder token, boolean timeout) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeBoolean(timeout);
                    this.mRemote.transact(39, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void setProcessState(int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(state);
                    this.mRemote.transact(40, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void scheduleInstallProvider(ProviderInfo provider) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(provider, 0);
                    this.mRemote.transact(41, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void updateTimePrefs(int timeFormatPreference) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(timeFormatPreference);
                    this.mRemote.transact(42, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void scheduleEnterAnimationComplete(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(43, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void notifyCleartextNetwork(byte[] firstPacket) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(firstPacket);
                    this.mRemote.transact(44, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void startBinderTracking() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(45, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void stopBinderTrackingAndDump(ParcelFileDescriptor fd) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(fd, 0);
                    this.mRemote.transact(46, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void scheduleLocalVoiceInteractionStarted(IBinder token, IVoiceInteractor voiceInteractor) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeStrongInterface(voiceInteractor);
                    this.mRemote.transact(47, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void handleTrustStorageUpdate() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(48, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void attachAgent(String path) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(path);
                    this.mRemote.transact(49, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void attachStartupAgents(String dataDir) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(dataDir);
                    this.mRemote.transact(50, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void scheduleApplicationInfoChanged(ApplicationInfo ai) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(ai, 0);
                    this.mRemote.transact(51, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void setNetworkBlockSeq(long procStateSeq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(procStateSeq);
                    this.mRemote.transact(52, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void scheduleTransaction(ClientTransaction transaction) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(transaction, 0);
                    this.mRemote.transact(53, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void requestDirectActions(IBinder activityToken, IVoiceInteractor intractor, RemoteCallback cancellationCallback, RemoteCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(activityToken);
                    _data.writeStrongInterface(intractor);
                    _data.writeTypedObject(cancellationCallback, 0);
                    _data.writeTypedObject(callback, 0);
                    this.mRemote.transact(54, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void performDirectAction(IBinder activityToken, String actionId, Bundle arguments, RemoteCallback cancellationCallback, RemoteCallback resultCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(activityToken);
                    _data.writeString(actionId);
                    _data.writeTypedObject(arguments, 0);
                    _data.writeTypedObject(cancellationCallback, 0);
                    _data.writeTypedObject(resultCallback, 0);
                    this.mRemote.transact(55, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void enableActivityThreadLog(boolean isEnable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(isEnable);
                    this.mRemote.transact(56, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void enableProcessMainThreadLooperLog() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(57, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void dumpMessage(boolean all) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(all);
                    this.mRemote.transact(58, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void notifyContentProviderPublishStatus(ContentProviderHolder holder, String authorities, int userId, boolean published) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(holder, 0);
                    _data.writeString(authorities);
                    _data.writeInt(userId);
                    _data.writeBoolean(published);
                    this.mRemote.transact(59, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void instrumentWithoutRestart(ComponentName instrumentationName, Bundle instrumentationArgs, IInstrumentationWatcher instrumentationWatcher, IUiAutomationConnection instrumentationUiConnection, ApplicationInfo targetInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(instrumentationName, 0);
                    _data.writeTypedObject(instrumentationArgs, 0);
                    _data.writeStrongInterface(instrumentationWatcher);
                    _data.writeStrongInterface(instrumentationUiConnection);
                    _data.writeTypedObject(targetInfo, 0);
                    this.mRemote.transact(60, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void updateUiTranslationState(IBinder activityToken, int state, TranslationSpec sourceSpec, TranslationSpec targetSpec, List<AutofillId> viewIds, UiTranslationSpec uiTranslationSpec) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(activityToken);
                    _data.writeInt(state);
                    _data.writeTypedObject(sourceSpec, 0);
                    _data.writeTypedObject(targetSpec, 0);
                    _data.writeTypedList(viewIds);
                    _data.writeTypedObject(uiTranslationSpec, 0);
                    this.mRemote.transact(61, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.app.IApplicationThread
            public void reclaimMemory() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(62, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 61;
        }
    }
}
