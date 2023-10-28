package com.android.server.policy;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.os.storage.StorageManagerInternal;
import android.provider.DeviceConfig;
import com.android.server.LocalServices;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import java.util.Arrays;
import java.util.HashSet;
/* loaded from: classes2.dex */
public abstract class SoftRestrictedPermissionPolicy {
    private static final int FLAGS_PERMISSION_RESTRICTION_ANY_EXEMPT = 14336;
    private static final SoftRestrictedPermissionPolicy DUMMY_POLICY = new SoftRestrictedPermissionPolicy() { // from class: com.android.server.policy.SoftRestrictedPermissionPolicy.1
        @Override // com.android.server.policy.SoftRestrictedPermissionPolicy
        public boolean mayGrantPermission() {
            return true;
        }
    };
    private static final HashSet<String> sForcedScopedStorageAppWhitelist = new HashSet<>(Arrays.asList(getForcedScopedStorageAppWhitelist()));

    public abstract boolean mayGrantPermission();

    private static int getMinimumTargetSDK(Context context, ApplicationInfo appInfo, UserHandle user) {
        PackageManager pm = context.getPackageManager();
        int minimumTargetSDK = appInfo.targetSdkVersion;
        String[] uidPkgs = pm.getPackagesForUid(appInfo.uid);
        if (uidPkgs != null) {
            for (String uidPkg : uidPkgs) {
                if (!uidPkg.equals(appInfo.packageName)) {
                    try {
                        ApplicationInfo uidPkgInfo = pm.getApplicationInfoAsUser(uidPkg, 0, user);
                        minimumTargetSDK = Integer.min(minimumTargetSDK, uidPkgInfo.targetSdkVersion);
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
            }
        }
        return minimumTargetSDK;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static SoftRestrictedPermissionPolicy forPermission(Context context, ApplicationInfo appInfo, AndroidPackage pkg, UserHandle user, String permission) {
        char c;
        boolean isWhiteListed;
        final boolean isWhiteListed2;
        int targetSDK;
        boolean hasLegacyExternalStorage;
        boolean hasRequestedLegacyExternalStorage;
        boolean hasRequestedPreserveLegacyExternalStorage;
        boolean hasWriteMediaStorageGrantedForUid;
        boolean isForcedScopedStorage;
        final int flags;
        switch (permission.hashCode()) {
            case -406040016:
                if (permission.equals("android.permission.READ_EXTERNAL_STORAGE")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 1365911975:
                if (permission.equals("android.permission.WRITE_EXTERNAL_STORAGE")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                if (appInfo != null) {
                    PackageManager pm = context.getPackageManager();
                    StorageManagerInternal smInternal = (StorageManagerInternal) LocalServices.getService(StorageManagerInternal.class);
                    int flags2 = pm.getPermissionFlags(permission, appInfo.packageName, user);
                    isWhiteListed = (flags2 & FLAGS_PERMISSION_RESTRICTION_ANY_EXEMPT) != 0;
                    hasLegacyExternalStorage = smInternal.hasLegacyExternalStorage(appInfo.uid);
                    hasRequestedLegacyExternalStorage = hasUidRequestedLegacyExternalStorage(appInfo.uid, context);
                    hasWriteMediaStorageGrantedForUid = hasWriteMediaStorageGrantedForUid(appInfo.uid, context);
                    hasRequestedPreserveLegacyExternalStorage = pkg.hasPreserveLegacyExternalStorage();
                    targetSDK = getMinimumTargetSDK(context, appInfo, user);
                    isWhiteListed2 = (flags2 & 16384) != 0;
                    isForcedScopedStorage = sForcedScopedStorageAppWhitelist.contains(appInfo.packageName);
                } else {
                    isWhiteListed = false;
                    isWhiteListed2 = false;
                    targetSDK = 0;
                    hasLegacyExternalStorage = false;
                    hasRequestedLegacyExternalStorage = false;
                    hasRequestedPreserveLegacyExternalStorage = false;
                    hasWriteMediaStorageGrantedForUid = false;
                    isForcedScopedStorage = false;
                }
                final boolean z = isWhiteListed;
                final int i = targetSDK;
                final boolean z2 = isWhiteListed2;
                final boolean z3 = isForcedScopedStorage;
                final boolean z4 = hasWriteMediaStorageGrantedForUid;
                final boolean z5 = hasLegacyExternalStorage;
                final boolean z6 = hasRequestedLegacyExternalStorage;
                final boolean z7 = hasRequestedPreserveLegacyExternalStorage;
                return new SoftRestrictedPermissionPolicy() { // from class: com.android.server.policy.SoftRestrictedPermissionPolicy.2
                    @Override // com.android.server.policy.SoftRestrictedPermissionPolicy
                    public boolean mayGrantPermission() {
                        return z || i >= 29;
                    }

                    @Override // com.android.server.policy.SoftRestrictedPermissionPolicy
                    public int getExtraAppOpCode() {
                        return 87;
                    }

                    @Override // com.android.server.policy.SoftRestrictedPermissionPolicy
                    public boolean mayAllowExtraAppOp() {
                        if (z2 || z3 || i >= 30) {
                            return false;
                        }
                        return z4 || z5 || z6;
                    }

                    @Override // com.android.server.policy.SoftRestrictedPermissionPolicy
                    public boolean mayDenyExtraAppOpIfGranted() {
                        if (i < 30) {
                            return !mayAllowExtraAppOp();
                        }
                        return z2 || z3 || !z7;
                    }
                };
            case 1:
                if (appInfo != null) {
                    isWhiteListed2 = (context.getPackageManager().getPermissionFlags(permission, appInfo.packageName, user) & FLAGS_PERMISSION_RESTRICTION_ANY_EXEMPT) != 0;
                    flags = getMinimumTargetSDK(context, appInfo, user);
                } else {
                    isWhiteListed2 = false;
                    flags = 0;
                }
                return new SoftRestrictedPermissionPolicy() { // from class: com.android.server.policy.SoftRestrictedPermissionPolicy.3
                    @Override // com.android.server.policy.SoftRestrictedPermissionPolicy
                    public boolean mayGrantPermission() {
                        return isWhiteListed2 || flags >= 29;
                    }
                };
            default:
                return DUMMY_POLICY;
        }
    }

    private static boolean hasUidRequestedLegacyExternalStorage(int uid, Context context) {
        ApplicationInfo applicationInfo;
        PackageManager packageManager = context.getPackageManager();
        String[] packageNames = packageManager.getPackagesForUid(uid);
        if (packageNames == null) {
            return false;
        }
        UserHandle user = UserHandle.getUserHandleForUid(uid);
        for (String packageName : packageNames) {
            try {
                applicationInfo = packageManager.getApplicationInfoAsUser(packageName, 0, user);
            } catch (PackageManager.NameNotFoundException e) {
            }
            if (applicationInfo.hasRequestedLegacyExternalStorage()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasWriteMediaStorageGrantedForUid(int uid, Context context) {
        PackageManager packageManager = context.getPackageManager();
        String[] packageNames = packageManager.getPackagesForUid(uid);
        if (packageNames == null) {
            return false;
        }
        for (String packageName : packageNames) {
            if (packageManager.checkPermission("android.permission.WRITE_MEDIA_STORAGE", packageName) == 0) {
                return true;
            }
        }
        return false;
    }

    private static String[] getForcedScopedStorageAppWhitelist() {
        String rawList = DeviceConfig.getString("storage_native_boot", "forced_scoped_storage_whitelist", "");
        if (rawList == null || rawList.equals("")) {
            return new String[0];
        }
        return rawList.split(",");
    }

    public int getExtraAppOpCode() {
        return -1;
    }

    public boolean mayAllowExtraAppOp() {
        return false;
    }

    public boolean mayDenyExtraAppOpIfGranted() {
        return false;
    }
}
