package com.android.server.pm;

import com.android.server.utils.SnapshotCache;
import com.android.server.utils.WatchedArrayList;
import com.android.server.utils.WatchedSparseArray;
import com.android.server.utils.Watcher;
import com.transsion.hubcore.server.pm.ITranPackageManagerService;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class AppIdSettingMap {
    private int mFirstAvailableAppId;
    private final WatchedArrayList<SettingBase> mNonSystemSettings;
    private final SnapshotCache<WatchedArrayList<SettingBase>> mNonSystemSettingsSnapshot;
    private final WatchedSparseArray<SettingBase> mSystemSettings;
    private final SnapshotCache<WatchedSparseArray<SettingBase>> mSystemSettingsSnapshot;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppIdSettingMap() {
        this.mFirstAvailableAppId = 10000;
        WatchedArrayList<SettingBase> watchedArrayList = new WatchedArrayList<>();
        this.mNonSystemSettings = watchedArrayList;
        this.mNonSystemSettingsSnapshot = new SnapshotCache.Auto(watchedArrayList, watchedArrayList, "AppIdSettingMap.mNonSystemSettings");
        WatchedSparseArray<SettingBase> watchedSparseArray = new WatchedSparseArray<>();
        this.mSystemSettings = watchedSparseArray;
        this.mSystemSettingsSnapshot = new SnapshotCache.Auto(watchedSparseArray, watchedSparseArray, "AppIdSettingMap.mSystemSettings");
    }

    AppIdSettingMap(AppIdSettingMap orig) {
        this.mFirstAvailableAppId = 10000;
        this.mNonSystemSettings = orig.mNonSystemSettingsSnapshot.snapshot();
        this.mNonSystemSettingsSnapshot = new SnapshotCache.Sealed();
        this.mSystemSettings = orig.mSystemSettingsSnapshot.snapshot();
        this.mSystemSettingsSnapshot = new SnapshotCache.Sealed();
    }

    public boolean registerExistingAppId(int appId, SettingBase setting, Object name) {
        if (appId >= 10000) {
            int index = appId - 10000;
            for (int size = this.mNonSystemSettings.size(); index >= size; size++) {
                this.mNonSystemSettings.add(null);
            }
            if (this.mNonSystemSettings.get(index) != null) {
                PackageManagerService.reportSettingsProblem(5, "Adding duplicate app id: " + appId + " name=" + name);
                return false;
            }
            this.mNonSystemSettings.set(index, setting);
            ITranPackageManagerService.Instance().uidAdded(appId, setting);
            return true;
        } else if (this.mSystemSettings.get(appId) != null) {
            PackageManagerService.reportSettingsProblem(5, "Adding duplicate shared id: " + appId + " name=" + name);
            return false;
        } else {
            this.mSystemSettings.put(appId, setting);
            ITranPackageManagerService.Instance().uidAdded(appId, setting);
            return true;
        }
    }

    public SettingBase getSetting(int appId) {
        if (appId >= 10000) {
            int size = this.mNonSystemSettings.size();
            int index = appId - 10000;
            if (index < size) {
                return this.mNonSystemSettings.get(index);
            }
            return null;
        }
        return this.mSystemSettings.get(appId);
    }

    public void removeSetting(int appId) {
        if (appId >= 10000) {
            int size = this.mNonSystemSettings.size();
            int index = appId - 10000;
            if (index < size) {
                ITranPackageManagerService.Instance().uidRemoved(appId, this.mNonSystemSettings.get(index) instanceof SharedUserSetting);
                this.mNonSystemSettings.set(index, null);
            }
        } else {
            ITranPackageManagerService.Instance().otherUidRemoved(appId, this.mSystemSettings.get(appId) instanceof SharedUserSetting);
            this.mSystemSettings.remove(appId);
        }
        setFirstAvailableAppId(appId + 1);
    }

    private void setFirstAvailableAppId(int uid) {
        if (uid > this.mFirstAvailableAppId) {
            this.mFirstAvailableAppId = uid;
        }
    }

    public void replaceSetting(int appId, SettingBase setting) {
        if (appId >= 10000) {
            int size = this.mNonSystemSettings.size();
            int index = appId - 10000;
            if (index < size) {
                this.mNonSystemSettings.set(index, setting);
                return;
            } else {
                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: calling replaceAppIdLpw to replace SettingBase at appId=" + appId + " but nothing is replaced.");
                return;
            }
        }
        this.mSystemSettings.put(appId, setting);
    }

    public int acquireAndRegisterNewAppId(SettingBase obj) {
        int size = this.mNonSystemSettings.size();
        for (int i = this.mFirstAvailableAppId - 10000; i < size; i++) {
            if (this.mNonSystemSettings.get(i) == null) {
                this.mNonSystemSettings.set(i, obj);
                ITranPackageManagerService.Instance().uidAdded(i + 10000, obj);
                return i + 10000;
            }
        }
        if (size > 9999) {
            return -1;
        }
        this.mNonSystemSettings.add(obj);
        ITranPackageManagerService.Instance().uidAdded(size + 10000, obj);
        return size + 10000;
    }

    public AppIdSettingMap snapshot() {
        return new AppIdSettingMap(this);
    }

    public void registerObserver(Watcher observer) {
        this.mNonSystemSettings.registerObserver(observer);
        this.mSystemSettings.registerObserver(observer);
    }
}
