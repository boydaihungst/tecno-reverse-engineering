package com.transsion.hubcore.server.am;

import android.app.ApplicationErrorReport;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import com.android.internal.util.MemInfoReader;
import com.android.server.am.ContentProviderRecord;
import com.android.server.am.ProcessRecord;
import com.android.server.wm.WindowManagerService;
import com.transsion.hubcore.griffin.lib.app.TranProcessWrapper;
import com.transsion.hubcore.server.am.ITranActivityManagerService;
import com.transsion.hubcore.utils.TranClassInfo;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranActivityManagerService {
    public static final TranClassInfo<ITranActivityManagerService> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.am.TranActivityManagerServiceImpl", ITranActivityManagerService.class, new Supplier() { // from class: com.transsion.hubcore.server.am.ITranActivityManagerService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranActivityManagerService.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranActivityManagerService {
    }

    /* loaded from: classes2.dex */
    public interface IActivityManagerServiceTroy {
        List<String> getAlarmListWhenUpdateNextAlarmClockLocked();
    }

    static ITranActivityManagerService Instance() {
        return (ITranActivityManagerService) classInfo.getImpl();
    }

    default void onInstallSystemProviders(Context context) {
    }

    default Integer sendIntentSender(IIntentSender target, IBinder whitelistToken, int code, Intent intent, String resolvedType, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options, int userId) {
        return null;
    }

    default void onMakeActive(Object processRecord) {
    }

    default void onMakeInactive(Object processRecord) {
    }

    default void onConstruct(Context context) {
    }

    default void onWakefulnessChanged(boolean isAwake) {
    }

    default void onBootCompleted() {
    }

    default void onFinishBooting(Context context) {
    }

    default void onProcessRecordKill(Object app, int reason, int subReason, String msg) {
    }

    default void onAudioVolumeChange(int streamType, int volume, int device, String packageName) {
    }

    default void onClipboardChange(String packageName) {
    }

    default void onClipboardChangeLocked(int userId) {
    }

    default void onSetPrimaryClipLocked(int userId, String callingPackage) {
    }

    default void onAMSShutdown(int timeout) {
    }

    default void onUserSwitching(int oldUserId, int newUserId) {
    }

    default void onUserSwitchComplete(int newUserId) {
    }

    default void onForegroundProfileSwitch(int newProfileId) {
    }

    default void onLockedBootComplete(int newUserId) {
    }

    default void onProcessesReady(Context context) {
    }

    default void onProcessesReady() {
    }

    default void onSetProcessGroup(ProcessRecord app, int group) {
    }

    default void onBroadcastIntent(String callingPackage, Intent intent, Bundle bOptions, int callingUid, int callingPid) {
    }

    default void onProviderAdd(ProcessRecord processRecord) {
    }

    default void onProviderRemove(ProcessRecord processRecord) {
    }

    default void onAMSSystemReady(Context context) {
    }

    default void onCloudConfigInit(Context context) {
    }

    default void onCloudConfigSystemReady() {
    }

    default void reportReboot(String info) {
    }

    default void reportNe(String path) {
    }

    default void reportJE() {
    }

    default void init(Context context) {
    }

    default void reportException(String eventType, ProcessRecord process, String processName, String subject, String logPath, ApplicationErrorReport.CrashInfo crashInfo) {
    }

    default boolean limitStartProcessForProvider(ContentProviderRecord contentProviderRecord, TranProcessWrapper processWrapper, String name, ProcessRecord processRecord, ProviderInfo providerInfo) {
        return false;
    }

    default boolean shouldCrossDualApp(String packageName, int userId) {
        return false;
    }

    default boolean shouldSetContentProviderReleaseNeeded(ComponentName name) {
        return false;
    }

    default boolean needkillBackgroundProcesses(String packageName, int userId, long callingPid, String callerPkg, boolean isSystemApp) {
        return false;
    }

    default void setGiftEnable(boolean isGiftEnable) {
    }

    default void setGiftConfig(String cloudGiftConfig) {
    }

    default void addErrorInfoToTabe(boolean systemReady, boolean isForeApp, String processName, String eventType, ApplicationErrorReport.CrashInfo crashInfo, String processClass, int setAdj, int setSchedGroup) {
    }

    default boolean isPermissionCrash(ApplicationErrorReport.CrashInfo crashInfo, String packageName, boolean isForeApp) {
        return false;
    }

    default void initTranGraphicBufferMonitor(Context context, Object amsLocked, Object pidsLocked) {
    }

    default void reclaimGB(String packageName) {
    }

    default void reclaimGB(int findPid, String topInnerArg) {
    }

    default boolean doDump(String cmd, PrintWriter pw, String[] args, int opti) {
        return false;
    }

    default int setMemFusionEnable(Context context, boolean enable) {
        return -1;
    }

    default boolean isMemFusionEnabled() {
        return false;
    }

    default void switchMemFusion(Context context, boolean enable) {
    }

    default boolean isUxCompactionSupport(Context context) {
        return false;
    }

    default void switchUXCompaction(Context context, boolean enable) {
    }

    default void changeCompactionMem(Context context, String meminfo) {
    }

    default List<String> getSwapFileSizeList(Context context) {
        return new ArrayList();
    }

    default int isMemoryEnoughToMF(Context context, String memFusionSize) {
        return -1;
    }

    default int getMemoryForMF(Context context, String memFusionSize) {
        return 0;
    }

    default boolean isMatchCurMemSelection(Context context) {
        return false;
    }

    default void dumpApplicationMemoryUsage() {
    }

    default boolean DumpsysMeminfoCC(PrintWriter pw, String opt) {
        return false;
    }

    default boolean doDumpPss(PrintWriter pw, long cachedPss, MemInfoReader memInfo) {
        return false;
    }

    default void doDumpUsedPss(PrintWriter pw, long[] ss, long cachedPss, int INDEX_TOTAL_PSS, long kernelUsed) {
    }

    default void tmpsUidInvaild() {
    }

    default void startTNE(String tag, long type, int pid, String externinfo) {
    }

    default void doExecute(WindowManagerService wm) {
    }

    default void setIsUserMonkey(boolean userMonkey) {
    }

    default void setIsControllerMonkey(boolean isControllerMonkey) {
    }

    default boolean isTpms(int callingUid) {
        return false;
    }

    default void spdPrint(Context context, PrintWriter pw, String cmd) {
    }

    default void configAppOpsControl(PrintWriter pw, String[] args, int opti) {
    }

    default void configAudioMuteControl(PrintWriter pw, String[] args, int opti) {
    }

    default void configAdbEnable(PrintWriter pw, String[] args, int opti) {
    }

    default void configReduceANRControl(PrintWriter pw, String[] args, int opti) {
    }

    default void configDumpHeapControl(PrintWriter pw, String[] args, int opti) {
    }

    default void configFatalTest(PrintWriter pw, String[] args, int opti, int pid) {
    }

    default void configHamal(PrintWriter pw) {
    }

    default void hookProcStart(TranProcessWrapper processWrapper, String arg1, String arg2) {
    }

    default void hookProcDied(TranProcessWrapper processWrapper) {
    }

    default void hookPmAfterSystemReady() {
    }

    default boolean hookLimitDiedProcessRestart(ProcessRecord app, TranProcessWrapper wrapper) {
        return false;
    }

    default void hookPmOnBootCompleted() {
    }

    default void hookPmStartWork() {
    }

    default boolean hookPmStartService(String callingPackage, Intent service, boolean requireForeground, String resolvedType, int userId) {
        return false;
    }

    default boolean hookPmLimitBindService(String callingPackage, Intent service, String resolvedType, int userId, int flags) {
        return false;
    }

    default void hookPmFilterReceiveBroadcast(TranProcessWrapper processWrapper, int callingPid, int callingUid, Intent intent, List receivers) {
    }

    default boolean hookPmLimitSendBroadcast(TranProcessWrapper processWrapper, Intent intent, String callerPkg, int callerUid, int callerPid, String action, String pkg, ComponentName cmp, boolean serialized, String resolvedType) {
        return false;
    }

    default boolean hookPmLimitSendBroadcastP(String packageName, int uid, Intent intent, boolean serialized, String resolvedType) {
        return false;
    }

    default boolean hookPmLimitStartService(int uid, String callingPackage, Intent service, boolean fgRequired, String resolvedType, int userId) {
        return false;
    }

    default void sceneClean(int reasonCode, int uid, String pkg, List<String> protectList) {
    }

    default void setAutoStartEnable(boolean enable, String packageName) {
    }

    default boolean inStartWhiteList(String packageName) {
        return false;
    }

    default void setStartWhiteList(List<String> whiteList) {
    }

    default List<String> getStartWhiteList() {
        return new ArrayList();
    }

    default List<String> resetUserStartConfig() {
        return new ArrayList();
    }

    default void hookOnekeyClean(int level, List<String> protectList, String pkg, int uid) {
    }

    default void setCleanProtect(boolean protect, String packageName) {
    }

    default void setCleanWhiteList(List<String> protectList) {
    }

    default List<String> getCleanWhiteList() {
        return new ArrayList();
    }

    default boolean isFeatureEnable(int featureCode) {
        return false;
    }

    default boolean inFrzList(String key, String packageName) {
        return false;
    }

    default boolean inSlmList(String key, String packageName) {
        return false;
    }

    default void hookStart() {
    }

    default boolean setBlockStartEnable(String packageName, boolean enable) {
        return false;
    }

    default boolean isBlockStartEnabled(String packageName) {
        return false;
    }

    default boolean setStartBlockList(List<String> blockList) {
        return false;
    }

    default List<String> getStartBlockList() {
        return new ArrayList();
    }

    default List<String> resetBlockStartList() {
        return new ArrayList();
    }

    default boolean isAgaresEnable() {
        return false;
    }

    default boolean filterAppRestrictedInBg(String pkgName) {
        return false;
    }

    default String getTpTurboConfigs() {
        return "";
    }

    default int getScaledTouchSlop() {
        return 0;
    }

    default boolean isTpTurboApp() {
        return false;
    }

    default void hookProcDied(int pid) {
    }

    default boolean isAppIdleEnable() {
        return false;
    }

    default boolean limitReceiver(ProcessRecord callerApp, String callingPackage, Intent intent, boolean callerIdle, boolean isWake, int callingUid, boolean ordered, boolean sticky) {
        return false;
    }

    default boolean setETControl(String featureType, boolean status) {
        return false;
    }

    default boolean isETDisabledByPackageName(String featureType, String packageName) {
        return false;
    }

    default void onShellTran(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
    }

    default boolean isAppLaunchEnable() {
        return false;
    }

    default boolean isAppLaunchDebugEnable() {
        return false;
    }

    default boolean hookPmLimitStartService(int uid, String callingPackage, Intent service, boolean fgRequired, String resolvedType, int userId, boolean allowBackgroundActivityStarts) {
        return false;
    }

    default boolean hookPmLimitSendBroadcast(TranProcessWrapper processWrapper, Intent intent, String callerPkg, int callerUid, int callerPid, String action, String pkg, ComponentName cmp, boolean serialized, String resolvedType, IIntentReceiver resultTo) {
        return false;
    }

    default boolean hookPmLimitSendBroadcastP(String packageName, int uid, Intent intent, boolean serialized, String resolvedType, IIntentReceiver resultTo) {
        return false;
    }
}
