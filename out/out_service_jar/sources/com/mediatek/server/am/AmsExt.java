package com.mediatek.server.am;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import com.android.server.am.ProcessRecord;
import com.android.server.wm.ActivityRecord;
import java.io.PrintWriter;
import java.util.ArrayList;
/* loaded from: classes2.dex */
public class AmsExt {
    public static final int COLLECT_PSS_FG_MSG = 2;

    public void onAddErrorToDropBox(String dropboxTag, String info, int pid) {
    }

    public void enableMtkAmsLog() {
    }

    public void onSystemReady(Context context) {
    }

    public void onBeforeActivitySwitch(ActivityRecord lastResumedActivity, ActivityRecord nextResumedActivity, boolean pausing, int nextResumedActivityType, boolean isKeyguardShowing) {
    }

    public void onAfterActivityResumed(ActivityRecord resumedActivity) {
    }

    public void onActivityStateChanged(ActivityRecord resumedActivity, boolean onTop) {
    }

    public void onUpdateSleep(boolean wasSleeping, boolean isSleepingAfterUpdate) {
    }

    public void setAalMode(int mode) {
    }

    public void setAalEnabled(boolean enabled) {
    }

    public int amsAalDump(PrintWriter pw, String[] args, int opti) {
        return opti;
    }

    public void onStartProcess(String hostingType, String packageName) {
    }

    public void onNotifyAppCrash(int pid, int uid, String packageName) {
    }

    public void onEndOfActivityIdle(Context context, ActivityRecord activityRecord) {
    }

    public void onWakefulnessChanged(int wakefulness) {
    }

    public void addDuraSpeedService() {
    }

    public void startDuraSpeedService(Context context) {
    }

    public String onReadyToStartComponent(String packageName, int uid, String suppressReason, String className) {
        return null;
    }

    public boolean onBeforeStartProcessForStaticReceiver(String packageName) {
        return false;
    }

    public void addToSuppressRestartList(String packageName) {
    }

    public boolean notRemoveAlarm(String packageName) {
        return false;
    }

    public void enableAmsLog(ArrayList<ProcessRecord> lruProcesses) {
    }

    public void enableAmsLog(PrintWriter pw, String[] args, int opti, ArrayList<ProcessRecord> lruProcesses) {
    }

    public void enableProcessMainThreadLooperLog(PrintWriter pw, String[] args, int opti, ArrayList<ProcessRecord> lruProcesses) {
    }

    public boolean preLaunchApplication(String callingPackage, Intent intent, String resolvedType, int startFlags) {
        return false;
    }

    public boolean IsBuildInApp() {
        return true;
    }

    public boolean checkAutoBootPermission(Context context, String packageName, int userId, ArrayList<ProcessRecord> runningProcess, int callingPid) {
        return true;
    }

    public void onAppProcessDied(Context context, ProcessRecord app, ApplicationInfo appInfo, int userId, ArrayList<ProcessRecord> lruProcesses, String reason) {
    }
}
