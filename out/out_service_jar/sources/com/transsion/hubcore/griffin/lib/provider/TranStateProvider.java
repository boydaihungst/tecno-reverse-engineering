package com.transsion.hubcore.griffin.lib.provider;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.os.SystemClock;
import android.util.Pair;
import android.util.SparseArray;
import com.transsion.hubcore.griffin.lib.app.TranAppInfo;
import com.transsion.hubcore.griffin.lib.app.TranProcessInfo;
import com.transsion.hubcore.griffin.lib.app.TranProcessWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/* loaded from: classes2.dex */
public class TranStateProvider {
    public void startWork() {
    }

    public void stopWork() {
    }

    public void doCommand(String[] args) {
    }

    public void doCommand(int opti, String[] args) {
    }

    public String getHomePackage() {
        return "";
    }

    public Pair<ActivityInfo, TranProcessWrapper> getHomeInfo() {
        return null;
    }

    public ComponentName getWallpaper() {
        return null;
    }

    public String getWallpaperPackage() {
        return "";
    }

    public String getImePackage() {
        return "";
    }

    public String getDialerPackage() {
        return "";
    }

    public String getMmsPackage() {
        return "";
    }

    public String getVoicePackage() {
        return "";
    }

    public List<String> getActiveWidgets() {
        return Collections.emptyList();
    }

    public List<String> getFloatWindowPackages() {
        return Collections.emptyList();
    }

    public boolean isAppSwitching() {
        return false;
    }

    public ActivityInfo getNextActivity() {
        return null;
    }

    public int getTopUiType() {
        return -1;
    }

    public String getTopPackage() {
        return "";
    }

    public Pair<ActivityInfo, TranProcessWrapper> getTopInfo() {
        return null;
    }

    public String getPreviousPackage() {
        return "";
    }

    public Pair<ActivityInfo, TranProcessWrapper> getPreviousInfo() {
        return null;
    }

    public String getAudioFocusPackage() {
        return "";
    }

    public String getCurrentAudioRecordingPackage() {
        return "";
    }

    public String getCurrentScreenRecordingPackage() {
        return "";
    }

    public List<String> getKeepingNotificationPackages() {
        return Collections.emptyList();
    }

    public List<String> getAccessibilityPackages() {
        return Collections.emptyList();
    }

    public List<String> getNavigatingApps() {
        return Collections.emptyList();
    }

    public List<String> getPlayingMusicPackages() {
        return Collections.emptyList();
    }

    public List<String> getSoundRecordingPackages() {
        return Collections.emptyList();
    }

    public List<String> getScreenRecordingPackages() {
        return Collections.emptyList();
    }

    public boolean isScreenOff() {
        return false;
    }

    public long getScreenOffTimeMillis() {
        return -1L;
    }

    public List<String> getAlarmAppPackages() {
        return Collections.emptyList();
    }

    public boolean isHeavyingLoading() {
        return false;
    }

    public long getHeayLoadingStartTime() {
        return SystemClock.elapsedRealtime();
    }

    public long getMemoryByOomAdjForMb(int oomAdj) {
        return 0L;
    }

    public long getCpuLoading() {
        return 0L;
    }

    public boolean matchUidState(int uid, int state) {
        return true;
    }

    public int getUidState(int uid) {
        return -1;
    }

    public boolean isProcessDied(String processName, int uid) {
        return false;
    }

    public boolean isProcessActive(String processName, int uid) {
        return true;
    }

    public long getProcessDiedDuration(String processName, int uid) {
        return 0L;
    }

    public long getProcessActiveDuration(String processName, int uid) {
        return 0L;
    }

    public boolean isProcessKilledByGriffin(String processName, int uid) {
        return false;
    }

    public boolean isPackageStoppedByGriffin(String packageName) {
        return false;
    }

    public long getProcessKilledDuration(String processName, int uid) {
        return 0L;
    }

    public long getPackageStoppedDuration(String packageName) {
        return 0L;
    }

    public TranProcessInfo getProcessInfo(String processName, int uid) {
        return null;
    }

    public TranAppInfo getAppInfo(String packageName) {
        return null;
    }

    public int getAppCategoryByPackage(String packageName) {
        return -1;
    }

    public int getAppCategoryByProcess(String processName, int uid) {
        return -1;
    }

    public boolean isAppRunning(String packageName) {
        return false;
    }

    public boolean hasLaunchIcon(String packageName) {
        return true;
    }

    public TranProcessWrapper getProcessWrapper(int pid) {
        return null;
    }

    public TranProcessWrapper getProcessWrapper(String processName, int uid) {
        return null;
    }

    public ArrayList<TranProcessWrapper> getRunningProcess() {
        return new ArrayList<>();
    }

    public SparseArray<TranProcessWrapper> getPersistProcess() {
        return new SparseArray<>();
    }

    public boolean isFmPlaying(String packageName) {
        return false;
    }

    public boolean isYoPartyPlaying(String packageName) {
        return false;
    }

    public boolean isAudioPlayingByPackageName(String packageName) {
        return false;
    }

    public boolean isAudioPlayingByUidPid(int uid, int pid) {
        return false;
    }

    public boolean isSoundRecording(String packageName) {
        return false;
    }

    public boolean isScreenRecording(String packageName) {
        return false;
    }

    public boolean isScreenShoting(String packageName, int setAdj) {
        return false;
    }

    public boolean isPhoneCallRecording(String packageName) {
        return false;
    }

    public boolean isVpnConnected(String packageName) {
        return false;
    }

    public boolean isPalmStoreWorking(String packageName) {
        return false;
    }

    public boolean isAHAWorking(String packageName) {
        return false;
    }

    public boolean isDownloadingApp(String processName, String packageName, int uid, int pid) {
        return false;
    }

    public boolean isBluetoothWorking(String packageName) {
        return false;
    }

    public boolean isHomePackage(String packageName) {
        return false;
    }

    public boolean isWallPaperPackage(String packageName) {
        return false;
    }

    public boolean isMmsPackage(String packageName) {
        return false;
    }

    public boolean isDialerPackage(String packageName) {
        return false;
    }

    public boolean isImePackage(String packageName) {
        return false;
    }

    public boolean isFileManagerWorking(String processName, int pid) {
        return false;
    }

    public int getFileManagerWorkingPid() {
        return -1;
    }

    public boolean isRestoreWorking(String processName, String packageName, int pid) {
        return false;
    }

    public boolean isAppLockOn(String packageName) {
        return false;
    }

    public boolean isKeyguardGestureOn(String packageName) {
        return false;
    }

    public boolean isFaceUnlockOn(String packageName) {
        return false;
    }

    public boolean isDefaultVoiceInteraction(String processName, String packageName) {
        return false;
    }

    public boolean isPhoneCalling(String packageName) {
        return false;
    }

    public boolean isScreenSplitting(String packageName) {
        return false;
    }

    public double getAppProbability(String appName, int flags) {
        return 0.0d;
    }

    public Map<String, Double> getAppProbabilities(List<String> appNames, int flags) {
        return new HashMap();
    }

    public List<String> getAppsArrayRecommend(int appNumbers, int flags) {
        return new ArrayList();
    }

    public int getAudioPlayStateByUidPid(int uid, int pid) {
        return 0;
    }

    public List<String> getRearAppsArrayRecommend(int hours, int appNumbers, int flags) {
        return new ArrayList();
    }

    public List<String> getCurrentVirtualDisplayOwners() {
        return new ArrayList();
    }

    public boolean isVirtualDisplayPackage(String packageName) {
        return false;
    }

    public boolean isScreenRecordingPackage(String packageName) {
        return false;
    }
}
