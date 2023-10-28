package com.transsion.hubcore.griffin.lib.provider;

import android.os.SystemProperties;
import com.transsion.hubcore.griffin.TranFeatureSwitch;
import java.util.Collections;
import java.util.List;
import java.util.Set;
/* loaded from: classes2.dex */
public class TranConfigProvider {

    /* loaded from: classes2.dex */
    public interface ConfigListener {
        void onCleanWhiteListChanged(Set<String> set);

        void onSleepMasterBlackListChanged(Set<String> set);

        void onStartWhiteListChanged(Set<String> set);
    }

    public void startWork() {
    }

    public void stopWork() {
    }

    public void start(String feature, TranFeatureSwitch.SwitchListener listener) {
    }

    public void stop(String feature, TranFeatureSwitch.SwitchListener listener) {
    }

    public void doCommand(String[] args) {
    }

    public void doCommand(int opti, String[] args) {
    }

    public void afterSystemReady() {
    }

    public void onBootCompleted() {
    }

    public void addConfigListener(ConfigListener listener) {
    }

    public void removeConfigListener(ConfigListener listener) {
    }

    public List<String> getCleanWhiteList() {
        return Collections.emptyList();
    }

    public void addCleanWhiteList(String packageName) {
    }

    public void removeCleanWhiteList(String packageName) {
    }

    public boolean inCleanWhiteList(String packageName) {
        return false;
    }

    public void setCleanWhiteList(List<String> whiteList) {
    }

    public List<String> getStartWhiteList() {
        return Collections.emptyList();
    }

    public void addStartWhiteList(String packageName) {
    }

    public void removeStartWhiteList(String packageName) {
    }

    public boolean inStartWhiteList(String packageName) {
        return false;
    }

    public void setStartWhiteList(List<String> whiteList) {
    }

    public List<String> getSleepMasterBlackList() {
        return Collections.emptyList();
    }

    public void addSleepMasterBlackList(String packageName) {
    }

    public void removeSleepMasterBlackList(String packageName) {
    }

    public boolean inSleepMasterBlackList(String packageName) {
        return false;
    }

    public void setSleepMasterBlackList(List<String> blackList) {
    }

    public boolean inStartServiceBlacklist(String action, String caller, String target) {
        return false;
    }

    public boolean inBindServiceBlacklist(String action, String caller, String target) {
        return false;
    }

    public boolean inReceiveBroadcastBlacklist(String action, String caller, String target) {
        return false;
    }

    public List<String> getHoffnungStartWhitelist() {
        return Collections.emptyList();
    }

    public List<String> getHoffnungCleanWhitelist() {
        return Collections.emptyList();
    }

    public List<String> getCloudBackupStartWhitelist() {
        return Collections.emptyList();
    }

    public List<String> getCloudBackupCleanWhitelist() {
        return Collections.emptyList();
    }

    public boolean isFeatureEnable(int featureCode) {
        return true;
    }

    public boolean isFeatureEnable(String key) {
        return true;
    }

    public boolean inFrzList(String key, String packageName) {
        return false;
    }

    public boolean inSlmList(String key, String packageName) {
        return false;
    }

    public List<String> getCloudAppData(String key) {
        return Collections.emptyList();
    }

    public List<String> getGriffinCoreDate(String key) {
        return Collections.emptyList();
    }

    public void updateCoreCloudData() {
    }

    public boolean resetUserConfig() {
        return false;
    }

    public String getCloudConfig(String key) {
        return "";
    }

    public List<String> resetUserStartConfig() {
        return Collections.emptyList();
    }

    public boolean inCloudList(String key, String packageName) {
        return false;
    }

    public String getGriffinCoreConfig(String key) {
        return "";
    }

    public void registerConfigCallback(String key, int type, ITranConfigChangeCallback callback) {
    }

    public void unregisterConfigCallback(String key, ITranConfigChangeCallback callback) {
    }

    public void updateStartOrCleanWhiteList(String key, List whiteList) {
    }

    public List<String> getStartBlockList() {
        return Collections.emptyList();
    }

    public void addStartBlockList(String packageName) {
    }

    public void removeStartBlockList(String packageName) {
    }

    public boolean inStartBlockList(String packageName) {
        return false;
    }

    public void setStartBlockList(List<String> blockList) {
    }

    public boolean blockListEnable() {
        return SystemProperties.get("persist.sys.apm.limit_block_mode").equals("1");
    }

    public List<String> resetBlockStartList() {
        return Collections.emptyList();
    }
}
