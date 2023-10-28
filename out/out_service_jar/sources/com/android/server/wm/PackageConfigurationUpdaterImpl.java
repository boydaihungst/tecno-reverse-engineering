package com.android.server.wm;

import android.os.Binder;
import android.os.LocaleList;
import android.util.ArraySet;
import android.util.Slog;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.util.Optional;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class PackageConfigurationUpdaterImpl implements ActivityTaskManagerInternal.PackageConfigurationUpdater {
    private static final String TAG = "PackageConfigurationUpdaterImpl";
    private ActivityTaskManagerService mAtm;
    private LocaleList mLocales;
    private Integer mNightMode;
    private String mPackageName;
    private final Optional<Integer> mPid;
    private int mUserId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageConfigurationUpdaterImpl(int pid, ActivityTaskManagerService atm) {
        this.mPid = Optional.of(Integer.valueOf(pid));
        this.mAtm = atm;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageConfigurationUpdaterImpl(String packageName, int userId, ActivityTaskManagerService atm) {
        this.mPackageName = packageName;
        this.mUserId = userId;
        this.mAtm = atm;
        this.mPid = Optional.empty();
    }

    @Override // com.android.server.wm.ActivityTaskManagerInternal.PackageConfigurationUpdater
    public ActivityTaskManagerInternal.PackageConfigurationUpdater setNightMode(int nightMode) {
        synchronized (this) {
            this.mNightMode = Integer.valueOf(nightMode);
        }
        return this;
    }

    @Override // com.android.server.wm.ActivityTaskManagerInternal.PackageConfigurationUpdater
    public ActivityTaskManagerInternal.PackageConfigurationUpdater setLocales(LocaleList locales) {
        synchronized (this) {
            this.mLocales = locales;
        }
        return this;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [101=4] */
    @Override // com.android.server.wm.ActivityTaskManagerInternal.PackageConfigurationUpdater
    public boolean commit() {
        int uid;
        synchronized (this) {
            synchronized (this.mAtm.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                if (this.mPid.isPresent()) {
                    WindowProcessController wpc = this.mAtm.mProcessMap.getProcess(this.mPid.get().intValue());
                    if (wpc == null) {
                        Slog.w(TAG, "commit: Override application configuration failed: cannot find pid " + this.mPid);
                        Binder.restoreCallingIdentity(ident);
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return false;
                    }
                    uid = wpc.mUid;
                    this.mUserId = wpc.mUserId;
                    this.mPackageName = wpc.mInfo.packageName;
                } else {
                    int uid2 = this.mAtm.getPackageManagerInternalLocked().getPackageUid(this.mPackageName, 131072L, this.mUserId);
                    if (uid2 < 0) {
                        Slog.w(TAG, "commit: update of application configuration failed: userId or packageName not valid " + this.mUserId);
                        Binder.restoreCallingIdentity(ident);
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return false;
                    }
                    uid = uid2;
                }
                updateConfig(uid, this.mPackageName);
                boolean updateFromImpl = this.mAtm.mPackageConfigPersister.updateFromImpl(this.mPackageName, this.mUserId, this);
                Binder.restoreCallingIdentity(ident);
                WindowManagerService.resetPriorityAfterLockedSection();
                return updateFromImpl;
            }
        }
    }

    private void updateConfig(int uid, String packageName) {
        ArraySet<WindowProcessController> processes = this.mAtm.mProcessMap.getProcesses(uid);
        if (processes == null || processes.isEmpty()) {
            return;
        }
        LocaleList localesOverride = LocaleOverlayHelper.combineLocalesIfOverlayExists(this.mLocales, this.mAtm.getGlobalConfiguration().getLocales());
        for (int i = processes.size() - 1; i >= 0; i--) {
            WindowProcessController wpc = processes.valueAt(i);
            if (wpc.mInfo.packageName.equals(packageName)) {
                wpc.applyAppSpecificConfig(this.mNightMode, localesOverride);
            }
            wpc.updateAppSpecificSettingsForAllActivitiesInPackage(packageName, this.mNightMode, localesOverride);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Integer getNightMode() {
        return this.mNightMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public LocaleList getLocales() {
        return this.mLocales;
    }
}
