package com.android.server.notification;

import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ParceledListSlice;
import android.os.Binder;
import android.os.RemoteException;
import android.permission.IPermissionManager;
import android.util.ArrayMap;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
/* loaded from: classes2.dex */
public final class PermissionHelper {
    private static final String NOTIFICATION_PERMISSION = "android.permission.POST_NOTIFICATIONS";
    private static final String TAG = "PermissionHelper";
    private final IPackageManager mPackageManager;
    private final IPermissionManager mPermManager;
    private final PermissionManagerServiceInternal mPmi;

    public PermissionHelper(PermissionManagerServiceInternal pmi, IPackageManager packageManager, IPermissionManager permManager) {
        this.mPmi = pmi;
        this.mPackageManager = packageManager;
        this.mPermManager = permManager;
    }

    public boolean hasPermission(int uid) {
        long callingId = Binder.clearCallingIdentity();
        try {
            return this.mPmi.checkUidPermission(uid, NOTIFICATION_PERMISSION) == 0;
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    Set<Pair<Integer, String>> getAppsRequestingPermission(int userId) {
        Set<Pair<Integer, String>> requested = new HashSet<>();
        List<PackageInfo> pkgs = getInstalledPackages(userId);
        for (PackageInfo pi : pkgs) {
            if (pi.requestedPermissions != null) {
                String[] strArr = pi.requestedPermissions;
                int length = strArr.length;
                int i = 0;
                while (true) {
                    if (i < length) {
                        String perm = strArr[i];
                        if (!NOTIFICATION_PERMISSION.equals(perm)) {
                            i++;
                        } else {
                            requested.add(new Pair<>(Integer.valueOf(pi.applicationInfo.uid), pi.packageName));
                            break;
                        }
                    }
                }
            }
        }
        return requested;
    }

    private List<PackageInfo> getInstalledPackages(int userId) {
        ParceledListSlice<PackageInfo> parceledList = null;
        try {
            parceledList = this.mPackageManager.getInstalledPackages(4096L, userId);
        } catch (RemoteException e) {
            Slog.d(TAG, "Could not reach system server", e);
        }
        if (parceledList == null) {
            return Collections.emptyList();
        }
        return parceledList.getList();
    }

    Set<Pair<Integer, String>> getAppsGrantedPermission(int userId) {
        Set<Pair<Integer, String>> granted = new HashSet<>();
        ParceledListSlice<PackageInfo> parceledList = null;
        try {
            parceledList = this.mPackageManager.getPackagesHoldingPermissions(new String[]{NOTIFICATION_PERMISSION}, 0L, userId);
        } catch (RemoteException e) {
            Slog.e(TAG, "Could not reach system server", e);
        }
        if (parceledList == null) {
            return granted;
        }
        for (PackageInfo pi : parceledList.getList()) {
            granted.add(new Pair<>(Integer.valueOf(pi.applicationInfo.uid), pi.packageName));
        }
        return granted;
    }

    public ArrayMap<Pair<Integer, String>, Pair<Boolean, Boolean>> getNotificationPermissionValues(int userId) {
        ArrayMap<Pair<Integer, String>, Pair<Boolean, Boolean>> notifPermissions = new ArrayMap<>();
        Set<Pair<Integer, String>> allRequestingUids = getAppsRequestingPermission(userId);
        Set<Pair<Integer, String>> allApprovedUids = getAppsGrantedPermission(userId);
        for (Pair<Integer, String> pair : allRequestingUids) {
            notifPermissions.put(pair, new Pair<>(Boolean.valueOf(allApprovedUids.contains(pair)), Boolean.valueOf(isPermissionUserSet((String) pair.second, userId))));
        }
        return notifPermissions;
    }

    public void setNotificationPermission(String packageName, int userId, boolean grant, boolean userSet) {
        long callingId = Binder.clearCallingIdentity();
        try {
            try {
            } catch (RemoteException e) {
                Slog.e(TAG, "Could not reach system server", e);
            }
            if (packageRequestsNotificationPermission(packageName, userId) && !isPermissionFixed(packageName, userId) && (!isPermissionGrantedByDefaultOrRole(packageName, userId) || userSet)) {
                boolean currentlyGranted = this.mPmi.checkPermission(packageName, NOTIFICATION_PERMISSION, userId) != -1;
                if (grant && !currentlyGranted) {
                    this.mPermManager.grantRuntimePermission(packageName, NOTIFICATION_PERMISSION, userId);
                } else if (!grant && currentlyGranted) {
                    this.mPermManager.revokeRuntimePermission(packageName, NOTIFICATION_PERMISSION, userId, TAG);
                }
                if (userSet) {
                    this.mPermManager.updatePermissionFlags(packageName, NOTIFICATION_PERMISSION, 1, 1, true, userId);
                } else {
                    this.mPermManager.updatePermissionFlags(packageName, NOTIFICATION_PERMISSION, 0, 1, true, userId);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public void setNotificationPermission(PackagePermission pkgPerm) {
        if (pkgPerm != null && pkgPerm.packageName != null && !isPermissionFixed(pkgPerm.packageName, pkgPerm.userId)) {
            setNotificationPermission(pkgPerm.packageName, pkgPerm.userId, pkgPerm.granted, true);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [219=4] */
    public boolean isPermissionFixed(String packageName, int userId) {
        long callingId = Binder.clearCallingIdentity();
        boolean z = false;
        try {
            int flags = this.mPermManager.getPermissionFlags(packageName, NOTIFICATION_PERMISSION, userId);
            return ((flags & 16) == 0 && (flags & 4) == 0) ? true : true;
        } catch (RemoteException e) {
            Slog.e(TAG, "Could not reach system server", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [236=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPermissionUserSet(String packageName, int userId) {
        long callingId = Binder.clearCallingIdentity();
        try {
            int flags = this.mPermManager.getPermissionFlags(packageName, NOTIFICATION_PERMISSION, userId);
            return (flags & 3) != 0;
        } catch (RemoteException e) {
            Slog.e(TAG, "Could not reach system server", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [253=4] */
    boolean isPermissionGrantedByDefaultOrRole(String packageName, int userId) {
        long callingId = Binder.clearCallingIdentity();
        try {
            int flags = this.mPermManager.getPermissionFlags(packageName, NOTIFICATION_PERMISSION, userId);
            return (32800 & flags) != 0;
        } catch (RemoteException e) {
            Slog.e(TAG, "Could not reach system server", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    private boolean packageRequestsNotificationPermission(String packageName, int userId) {
        try {
            String[] permissions = this.mPackageManager.getPackageInfo(packageName, 4096L, userId).requestedPermissions;
            return ArrayUtils.contains(permissions, NOTIFICATION_PERMISSION);
        } catch (RemoteException e) {
            Slog.e(TAG, "Could not reach system server", e);
            return false;
        }
    }

    /* loaded from: classes2.dex */
    public static class PackagePermission {
        public final boolean granted;
        public final String packageName;
        public final int userId;
        public final boolean userModifiedSettings;

        public PackagePermission(String pkg, int userId, boolean granted, boolean userSet) {
            this.packageName = pkg;
            this.userId = userId;
            this.granted = granted;
            this.userModifiedSettings = userSet;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PackagePermission that = (PackagePermission) o;
            if (this.userId == that.userId && this.granted == that.granted && this.userModifiedSettings == that.userModifiedSettings && Objects.equals(this.packageName, that.packageName)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(this.packageName, Integer.valueOf(this.userId), Boolean.valueOf(this.granted), Boolean.valueOf(this.userModifiedSettings));
        }

        public String toString() {
            return "PackagePermission{packageName='" + this.packageName + "', userId=" + this.userId + ", granted=" + this.granted + ", userSet=" + this.userModifiedSettings + '}';
        }
    }
}
