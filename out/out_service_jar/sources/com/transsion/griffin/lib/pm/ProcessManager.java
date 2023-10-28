package com.transsion.griffin.lib.pm;

import android.app.job.JobInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import com.android.server.UiModeManagerService;
import com.transsion.griffin.FeatureSwitch;
import com.transsion.griffin.lib.app.ProcessWrapper;
import java.io.PrintWriter;
import java.util.List;
/* loaded from: classes2.dex */
public class ProcessManager {
    public ProcessManager() {
    }

    public ProcessManager(Context context) {
    }

    public void startWork() {
    }

    public void stopWork() {
    }

    public void afterSystemReady() {
    }

    public void onBootCompleted() {
    }

    public void openFeature(String feature, FeatureSwitch.SwitchListener listener) {
    }

    public void closeFeature(String feature, FeatureSwitch.SwitchListener listener) {
    }

    public boolean doCommand(PrintWriter pw, String[] args) {
        return false;
    }

    public boolean doCommand(PrintWriter pw, String[] args, int opti) {
        return false;
    }

    public String getVersion() {
        return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
    }

    public void onekeyClean(int callerUid, String callerPackage, List<String> excludes) {
    }

    public void silentClean(int reasonCode, String reason, List<String> excludes) {
    }

    public boolean limitExecuteRunnableJob(JobInfo job) {
        return false;
    }

    public boolean limitSendBroadcast(int callerPid, int callerUid, ProcessWrapper callerProc, Intent intent, boolean serialized, String resolvedType) {
        return false;
    }

    public boolean limitSendBroadcastP(String packageName, int uid, Intent intent, boolean serialized, String resolvedType) {
        return false;
    }

    public void filterReceiveBroadcast(ProcessWrapper callerProc, int callerPid, int callerUid, Intent intent, List<ResolveInfo> receivers) {
    }

    public boolean limitStartActivity(int callerPid, int callerUid, String callerPkg, Intent intent, int type) {
        return false;
    }

    public boolean limitBindService(int callerPid, int callerUid, String callerPackage, Intent service, String resolvedType, int flags, int userId) {
        return false;
    }

    public boolean limitStartService(int callerPid, int callerUid, String callerPackage, Intent service, String resolvedType, int userId) {
        return false;
    }

    public boolean limitRestartService(ServiceInfo service, Intent intent, boolean execInFg) {
        return false;
    }

    public boolean limitStartProcessForService(int callerType, ProcessWrapper callerProc, ServiceInfo service, Intent intent) {
        return false;
    }

    public boolean limitStartProcessForReceiver(int callerPid, int callerUid, String callerPackage, ProcessWrapper callerProc, Intent intent, ActivityInfo curReceiver, ComponentName curComponent, boolean ordered) {
        return false;
    }

    public boolean limitStartProcessForProvider(ProcessWrapper callerProc, String authority, ProviderInfo provider, ComponentName name) {
        return false;
    }

    public boolean limitStartProcessForActivity(Object... args) {
        return false;
    }

    public boolean limitDiedProcessRestart(ProcessWrapper diedProc) {
        return false;
    }

    public boolean limitStartProcess(String hostingType, ProcessWrapper newProc) {
        return false;
    }

    public int setMaxOomAdj(ProcessWrapper proc, int maxAdj) {
        return maxAdj;
    }

    public int[] fixOomAdj(ProcessWrapper proc, int origSchedGroup, int origAdj, int origProcState, String adjType) {
        return new int[]{origSchedGroup, origAdj, origProcState};
    }

    public void onTaskRemoved(List<ProcessWrapper> procesToKill, ComponentName task, boolean killed) {
    }

    public void onActivityResumed(ActivityInfo activityInfo, ProcessWrapper proc, boolean switched) {
    }

    public void onActivityIdle(ProcessWrapper proc, ActivityInfo info, Intent intent) {
    }

    public void onResChanged(int item, String message) {
    }

    public boolean isIgnoreKillApp(String packageName) {
        return false;
    }

    public boolean isOutMaxMem(String packageName, long lastPss) {
        return true;
    }

    public boolean isIgnoreClearApp(String packageName) {
        return false;
    }

    public boolean isPromoteToBS(String packageName) {
        return false;
    }

    public boolean isKeepAliveEnable() {
        return true;
    }

    public void sceneClean(int reasonCode, int callerUid, String callerPackage, List<String> excludes) {
    }

    public boolean isBackgroundRestricted(int uid, String packageName) {
        return false;
    }

    public Bundle getBadBehaviorAppInfo(int callerType) {
        return Bundle.EMPTY;
    }

    public Bundle getBadBehaviorProcInfo(int callerType) {
        return Bundle.EMPTY;
    }

    public boolean isAppBehaviorBad(int callerType, String packageName) {
        return false;
    }

    public boolean isProcBehaviorBad(int callerType, int procId) {
        return false;
    }

    public boolean callerInBlockList(String packageName) {
        return false;
    }

    public boolean getExceptionSwitch() {
        return false;
    }

    public void updateMemfusionConfig() {
    }
}
