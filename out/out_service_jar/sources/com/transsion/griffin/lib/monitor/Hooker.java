package com.transsion.griffin.lib.monitor;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.IBinder;
import com.android.server.UiModeManagerService;
import com.transsion.griffin.FeatureSwitch;
import com.transsion.griffin.lib.app.ProcessWrapper;
import com.transsion.griffin.lib.app.WidgetInfo;
import com.transsion.griffin.lib.app.WindowInfo;
import com.transsion.griffin.lib.provider.StateListener;
import java.com.transsion.griffin.lib.GriffinPackage;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
/* loaded from: classes2.dex */
public class Hooker {
    public void startWork() {
    }

    public void stopWork() {
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

    public void afterSystemReady() {
    }

    public void onBootCompleted() {
    }

    public void hookPMSScanEnd(Collection<GriffinPackage> packages) {
    }

    public void hookActivityResumed(ActivityInfo activityInfo, ProcessWrapper proc) {
    }

    public void hookActivitySwitch(ActivityInfo lastResumedActivity, ActivityInfo nextResumedActivity, boolean pausing, int nextResumedActivityType) {
    }

    public void hookActivityIdle(ProcessWrapper proc, ActivityInfo info, Intent intent) {
    }

    public void hookAppWidgetChanged(WidgetInfo widgetInfo, int type) {
    }

    public void hookAppWidgetsClear() {
    }

    public void hookHomeChanged(ActivityInfo activityInfo, ProcessWrapper processInfo) {
    }

    public void hookWallpaperChanged(ComponentName wallpaper) {
    }

    public void hookWindowChanged(IBinder client, WindowInfo windowInfo, boolean isAdd) {
    }

    public void hookProcDied(ProcessWrapper diedProc) {
    }

    public void hookProcStart(ProcessWrapper newProc, String hostingType, String hostingNameStr) {
    }

    public void onProcessKilled(ProcessWrapper proc, int type) {
    }

    public void onPackageStopped(String packageName, int type) {
    }

    public void hookUidChanged(int uid, int state) {
    }

    public void hookProcessChanged(ProcessWrapper info, boolean added) {
    }

    public void hookTaskRemoved(List<ProcessWrapper> procesToKill, ComponentName task, boolean killed) {
    }

    public void hookAudioRecordChanged(int uid, String packageName) {
    }

    public void hookAlarmUpdate(List<String> alamList) {
    }

    public void hookAudioPlayerChanged(int uid, int pid, boolean status) {
    }

    public void hookAudioPlayerChanged(int uid, int pid, int playId, int playState) {
    }

    public void hookVpnChanged(String packageName) {
    }

    public boolean registerListener(StateListener listener) {
        return false;
    }

    public boolean registerListener(StateListener listener, Handler scheduler) {
        return false;
    }

    public boolean unregisterListener(StateListener listener) {
        return false;
    }

    public void hookScreenChanged(boolean status) {
    }

    public void hookScreenSplitPackageChanged(int displayId, String packageName) {
    }
}
