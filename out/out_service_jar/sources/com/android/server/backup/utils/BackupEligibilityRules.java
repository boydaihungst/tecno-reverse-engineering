package com.android.server.backup.utils;

import android.app.compat.CompatChanges;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.UserHandle;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.UserBackupManagerService;
import com.android.server.backup.transport.BackupTransportClient;
import com.android.server.backup.transport.TransportConnection;
import com.android.server.pm.PackageManagerService;
import com.google.android.collect.Sets;
import java.util.Set;
/* loaded from: classes.dex */
public class BackupEligibilityRules {
    private static final boolean DEBUG = false;
    static final long IGNORE_ALLOW_BACKUP_IN_D2D = 183147249;
    static final long RESTRICT_ADB_BACKUP = 171032338;
    private static final Set<String> systemPackagesAllowedForAllUsers = Sets.newArraySet(new String[]{UserBackupManagerService.PACKAGE_MANAGER_SENTINEL, PackageManagerService.PLATFORM_PACKAGE_NAME});
    private final int mOperationType;
    private final PackageManager mPackageManager;
    private final PackageManagerInternal mPackageManagerInternal;
    private final int mUserId;

    public static BackupEligibilityRules forBackup(PackageManager packageManager, PackageManagerInternal packageManagerInternal, int userId) {
        return new BackupEligibilityRules(packageManager, packageManagerInternal, userId, 0);
    }

    public BackupEligibilityRules(PackageManager packageManager, PackageManagerInternal packageManagerInternal, int userId, int operationType) {
        this.mPackageManager = packageManager;
        this.mPackageManagerInternal = packageManagerInternal;
        this.mUserId = userId;
        this.mOperationType = operationType;
    }

    public boolean appIsEligibleForBackup(ApplicationInfo app) {
        if (isAppBackupAllowed(app)) {
            if ((UserHandle.isCore(app.uid) && ((this.mUserId != 0 && !systemPackagesAllowedForAllUsers.contains(app.packageName)) || app.backupAgentName == null)) || app.packageName.equals(UserBackupManagerService.SHARED_BACKUP_AGENT_PACKAGE) || app.isInstantApp()) {
                return false;
            }
            return !appIsDisabled(app);
        }
        return false;
    }

    public boolean isAppBackupAllowed(ApplicationInfo app) {
        boolean allowBackup = (app.flags & 32768) != 0;
        switch (this.mOperationType) {
            case 0:
                return allowBackup;
            case 1:
                boolean isSystemApp = (app.flags & 1) != 0;
                boolean ignoreAllowBackup = !isSystemApp && CompatChanges.isChangeEnabled((long) IGNORE_ALLOW_BACKUP_IN_D2D, app.packageName, UserHandle.of(this.mUserId));
                return ignoreAllowBackup || allowBackup;
            case 2:
            default:
                Slog.w(BackupManagerService.TAG, "Unknown operation type:" + this.mOperationType);
                return false;
            case 3:
                String packageName = app.packageName;
                if (packageName == null) {
                    Slog.w(BackupManagerService.TAG, "Invalid ApplicationInfo object");
                    return false;
                } else if (!CompatChanges.isChangeEnabled((long) RESTRICT_ADB_BACKUP, packageName, UserHandle.of(this.mUserId))) {
                    return allowBackup;
                } else {
                    if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(packageName)) {
                        return true;
                    }
                    boolean isPrivileged = (app.flags & 8) != 0;
                    boolean isDebuggable = (app.flags & 2) != 0;
                    if (UserHandle.isCore(app.uid) || isPrivileged) {
                        try {
                            return this.mPackageManager.getProperty("android.backup.ALLOW_ADB_BACKUP", packageName).getBoolean();
                        } catch (PackageManager.NameNotFoundException e) {
                            Slog.w(BackupManagerService.TAG, "Failed to read allowAdbBackup property for + " + packageName);
                            return allowBackup;
                        }
                    }
                    return isDebuggable;
                }
        }
    }

    public boolean appIsRunningAndEligibleForBackupWithTransport(TransportConnection transportConnection, String packageName) {
        try {
            PackageInfo packageInfo = this.mPackageManager.getPackageInfoAsUser(packageName, 134217728, this.mUserId);
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            if (!appIsEligibleForBackup(applicationInfo) || appIsStopped(applicationInfo) || appIsDisabled(applicationInfo)) {
                return false;
            }
            if (transportConnection != null) {
                try {
                    BackupTransportClient transport = transportConnection.connectOrThrow("AppBackupUtils.appIsRunningAndEligibleForBackupWithTransport");
                    return transport.isAppEligibleForBackup(packageInfo, appGetsFullBackup(packageInfo));
                } catch (Exception e) {
                    Slog.e(BackupManagerService.TAG, "Unable to ask about eligibility: " + e.getMessage());
                    return true;
                }
            }
            return true;
        } catch (PackageManager.NameNotFoundException e2) {
            return false;
        }
    }

    boolean appIsDisabled(ApplicationInfo app) {
        int enabledSetting = this.mPackageManagerInternal.getApplicationEnabledState(app.packageName, this.mUserId);
        switch (enabledSetting) {
            case 0:
                return true ^ app.enabled;
            case 1:
            default:
                return false;
            case 2:
            case 3:
            case 4:
                return true;
        }
    }

    public boolean appIsStopped(ApplicationInfo app) {
        return (app.flags & 2097152) != 0;
    }

    public boolean appGetsFullBackup(PackageInfo pkg) {
        return pkg.applicationInfo.backupAgentName == null || (pkg.applicationInfo.flags & 67108864) != 0;
    }

    public boolean appIsKeyValueOnly(PackageInfo pkg) {
        return !appGetsFullBackup(pkg);
    }

    public boolean signaturesMatch(Signature[] storedSigs, PackageInfo target) {
        if (target == null || target.packageName == null) {
            return false;
        }
        if ((target.applicationInfo.flags & 1) != 0) {
            return true;
        }
        if (ArrayUtils.isEmpty(storedSigs)) {
            return false;
        }
        SigningInfo signingInfo = target.signingInfo;
        if (signingInfo == null) {
            Slog.w(BackupManagerService.TAG, "signingInfo is empty, app was either unsigned or the flag PackageManager#GET_SIGNING_CERTIFICATES was not specified");
            return false;
        }
        int nStored = storedSigs.length;
        if (nStored == 1) {
            return this.mPackageManagerInternal.isDataRestoreSafe(storedSigs[0], target.packageName);
        }
        Signature[] deviceSigs = signingInfo.getApkContentsSigners();
        int nDevice = deviceSigs.length;
        for (Signature signature : storedSigs) {
            boolean match = false;
            int j = 0;
            while (true) {
                if (j < nDevice) {
                    if (!signature.equals(deviceSigs[j])) {
                        j++;
                    } else {
                        match = true;
                        break;
                    }
                } else {
                    break;
                }
            }
            if (!match) {
                return false;
            }
        }
        return true;
    }

    public int getOperationType() {
        return this.mOperationType;
    }
}
