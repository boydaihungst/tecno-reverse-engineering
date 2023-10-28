package com.transsion.hubcore.griffin.lib.monitor;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.IBinder;
import com.android.server.UiModeManagerService;
import com.transsion.hubcore.griffin.TranFeatureSwitch;
import com.transsion.hubcore.griffin.lib.TranGriffinPackage;
import com.transsion.hubcore.griffin.lib.app.TranProcessWrapper;
import com.transsion.hubcore.griffin.lib.app.TranWidgetInfo;
import com.transsion.hubcore.griffin.lib.app.TranWindowInfo;
import com.transsion.hubcore.griffin.lib.provider.TranStateListener;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
/* loaded from: classes2.dex */
public class TranHooker {
    public void startWork() {
    }

    public void stopWork() {
    }

    public void openFeature(String feature, TranFeatureSwitch.SwitchListener listener) {
    }

    public void closeFeature(String feature, TranFeatureSwitch.SwitchListener listener) {
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

    public void hookPMSScanEnd(Collection<TranGriffinPackage> packages) {
    }

    public void hookActivityResumed(ActivityInfo activityInfo, TranProcessWrapper proc) {
    }

    public void hookActivitySwitch(ActivityInfo lastResumedActivity, ActivityInfo nextResumedActivity, boolean pausing, int nextResumedActivityType) {
    }

    public void hookActivityIdle(TranProcessWrapper proc, ActivityInfo info, Intent intent) {
    }

    public void hookAppWidgetChanged(TranWidgetInfo widgetInfo, int type) {
    }

    public void hookAppWidgetsClear() {
    }

    public void hookHomeChanged(ActivityInfo activityInfo, TranProcessWrapper processInfo) {
    }

    public void hookWallpaperChanged(ComponentName wallpaper) {
    }

    public void hookWindowChanged(IBinder client, TranWindowInfo windowInfo, boolean isAdd) {
    }

    public void hookProcDied(TranProcessWrapper diedProc) {
    }

    public void hookProcStart(TranProcessWrapper newProc, String hostingType, String hostingNameStr) {
    }

    public void onProcessKilled(TranProcessWrapper proc, int type) {
    }

    public void onPackageStopped(String packageName, int type) {
    }

    public void hookUidChanged(int uid, int state) {
    }

    public void hookProcessChanged(TranProcessWrapper info, boolean added) {
    }

    public void hookTaskRemoved(List<TranProcessWrapper> procesToKill, ComponentName task, boolean killed) {
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

    public boolean registerListener(TranStateListener listener) {
        return false;
    }

    public boolean registerListener(TranStateListener listener, Handler scheduler) {
        return false;
    }

    public boolean unregisterListener(TranStateListener listener) {
        return false;
    }

    public void hookScreenChanged(boolean status) {
    }

    public void hookScreenSplitPackageChanged(int displayId, String packageName) {
    }

    public void hookVirtualDisplayChanged(int uid, String packageName, int packageState) {
    }

    public void hookScreenRecordingChanged(int uid, String packageName, int packageState) {
    }
}
