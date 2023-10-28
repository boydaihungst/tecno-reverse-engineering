package com.android.server.pm.permission;

import android.content.pm.PackageManagerInternal;
import android.content.pm.PermissionInfo;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.component.ParsedPermission;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import libcore.util.EmptyArray;
/* loaded from: classes2.dex */
public final class Permission {
    private static final String TAG = "Permission";
    public static final int TYPE_CONFIG = 1;
    public static final int TYPE_DYNAMIC = 2;
    public static final int TYPE_MANIFEST = 0;
    private boolean mDefinitionChanged;
    private int[] mGids = EmptyArray.INT;
    private boolean mGidsPerUser;
    private PermissionInfo mPermissionInfo;
    private boolean mReconciled;
    private final int mType;
    private int mUid;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface PermissionType {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface ProtectionLevel {
    }

    public Permission(String name, String packageName, int type) {
        PermissionInfo permissionInfo = new PermissionInfo();
        this.mPermissionInfo = permissionInfo;
        permissionInfo.name = name;
        this.mPermissionInfo.packageName = packageName;
        this.mPermissionInfo.protectionLevel = 2;
        this.mType = type;
    }

    public Permission(PermissionInfo permissionInfo, int type) {
        this.mPermissionInfo = permissionInfo;
        this.mType = type;
    }

    public PermissionInfo getPermissionInfo() {
        return this.mPermissionInfo;
    }

    public void setPermissionInfo(PermissionInfo permissionInfo) {
        if (permissionInfo != null) {
            this.mPermissionInfo = permissionInfo;
        } else {
            PermissionInfo newPermissionInfo = new PermissionInfo();
            newPermissionInfo.name = this.mPermissionInfo.name;
            newPermissionInfo.packageName = this.mPermissionInfo.packageName;
            newPermissionInfo.protectionLevel = this.mPermissionInfo.protectionLevel;
            this.mPermissionInfo = newPermissionInfo;
        }
        this.mReconciled = permissionInfo != null;
    }

    public String getName() {
        return this.mPermissionInfo.name;
    }

    public int getProtectionLevel() {
        return this.mPermissionInfo.protectionLevel;
    }

    public String getPackageName() {
        return this.mPermissionInfo.packageName;
    }

    public int getType() {
        return this.mType;
    }

    public int getUid() {
        return this.mUid;
    }

    public boolean hasGids() {
        return this.mGids.length != 0;
    }

    public int[] getRawGids() {
        return this.mGids;
    }

    public boolean areGidsPerUser() {
        return this.mGidsPerUser;
    }

    public void setGids(int[] gids, boolean gidsPerUser) {
        this.mGids = gids;
        this.mGidsPerUser = gidsPerUser;
    }

    public int[] computeGids(int userId) {
        if (this.mGidsPerUser) {
            int[] userGids = new int[this.mGids.length];
            int i = 0;
            while (true) {
                int[] iArr = this.mGids;
                if (i < iArr.length) {
                    int gid = iArr[i];
                    userGids[i] = UserHandle.getUid(userId, gid);
                    i++;
                } else {
                    return userGids;
                }
            }
        } else {
            int[] userGids2 = this.mGids;
            return userGids2.length != 0 ? (int[]) userGids2.clone() : userGids2;
        }
    }

    public boolean isDefinitionChanged() {
        return this.mDefinitionChanged;
    }

    public void setDefinitionChanged(boolean definitionChanged) {
        this.mDefinitionChanged = definitionChanged;
    }

    public int calculateFootprint(Permission permission) {
        if (this.mUid == permission.mUid) {
            return permission.mPermissionInfo.name.length() + permission.mPermissionInfo.calculateFootprint();
        }
        return 0;
    }

    public boolean isPermission(ParsedPermission parsedPermission) {
        PermissionInfo permissionInfo = this.mPermissionInfo;
        return permissionInfo != null && Objects.equals(permissionInfo.packageName, parsedPermission.getPackageName()) && Objects.equals(this.mPermissionInfo.name, parsedPermission.getName());
    }

    public boolean isDynamic() {
        return this.mType == 2;
    }

    public boolean isNormal() {
        return (this.mPermissionInfo.protectionLevel & 15) == 0;
    }

    public boolean isRuntime() {
        return (this.mPermissionInfo.protectionLevel & 15) == 1;
    }

    public boolean isInstalled() {
        return (this.mPermissionInfo.flags & 1073741824) != 0;
    }

    public boolean isRemoved() {
        return (this.mPermissionInfo.flags & 2) != 0;
    }

    public boolean isSoftRestricted() {
        return (this.mPermissionInfo.flags & 8) != 0;
    }

    public boolean isHardRestricted() {
        return (this.mPermissionInfo.flags & 4) != 0;
    }

    public boolean isHardOrSoftRestricted() {
        return (this.mPermissionInfo.flags & 12) != 0;
    }

    public boolean isImmutablyRestricted() {
        return (this.mPermissionInfo.flags & 16) != 0;
    }

    public boolean isSignature() {
        return (this.mPermissionInfo.protectionLevel & 15) == 2;
    }

    public boolean isInternal() {
        return (this.mPermissionInfo.protectionLevel & 15) == 4;
    }

    public boolean isAppOp() {
        return (this.mPermissionInfo.protectionLevel & 64) != 0;
    }

    public boolean isDevelopment() {
        return isSignature() && (this.mPermissionInfo.protectionLevel & 32) != 0;
    }

    public boolean isInstaller() {
        return (this.mPermissionInfo.protectionLevel & 256) != 0;
    }

    public boolean isInstant() {
        return (this.mPermissionInfo.protectionLevel & 4096) != 0;
    }

    public boolean isOem() {
        return (this.mPermissionInfo.protectionLevel & 16384) != 0;
    }

    public boolean isPre23() {
        return (this.mPermissionInfo.protectionLevel & 128) != 0;
    }

    public boolean isPreInstalled() {
        return (this.mPermissionInfo.protectionLevel & 1024) != 0;
    }

    public boolean isPrivileged() {
        return (this.mPermissionInfo.protectionLevel & 16) != 0;
    }

    public boolean isRuntimeOnly() {
        return (this.mPermissionInfo.protectionLevel & 8192) != 0;
    }

    public boolean isSetup() {
        return (this.mPermissionInfo.protectionLevel & 2048) != 0;
    }

    public boolean isVerifier() {
        return (this.mPermissionInfo.protectionLevel & 512) != 0;
    }

    public boolean isVendorPrivileged() {
        return (this.mPermissionInfo.protectionLevel & 32768) != 0;
    }

    public boolean isSystemTextClassifier() {
        return (this.mPermissionInfo.protectionLevel & 65536) != 0;
    }

    public boolean isConfigurator() {
        return (this.mPermissionInfo.protectionLevel & 524288) != 0;
    }

    public boolean isIncidentReportApprover() {
        return (this.mPermissionInfo.protectionLevel & 1048576) != 0;
    }

    public boolean isAppPredictor() {
        return (this.mPermissionInfo.protectionLevel & 2097152) != 0;
    }

    public boolean isCompanion() {
        return (this.mPermissionInfo.protectionLevel & 8388608) != 0;
    }

    public boolean isRetailDemo() {
        return (this.mPermissionInfo.protectionLevel & 16777216) != 0;
    }

    public boolean isRecents() {
        return (this.mPermissionInfo.protectionLevel & 33554432) != 0;
    }

    public boolean isRole() {
        return (this.mPermissionInfo.protectionLevel & 67108864) != 0;
    }

    public boolean isKnownSigner() {
        return (this.mPermissionInfo.protectionLevel & 134217728) != 0;
    }

    public Set<String> getKnownCerts() {
        return this.mPermissionInfo.knownCerts;
    }

    public void transfer(String oldPackageName, String newPackageName) {
        if (!oldPackageName.equals(this.mPermissionInfo.packageName)) {
            return;
        }
        PermissionInfo newPermissionInfo = new PermissionInfo();
        newPermissionInfo.name = this.mPermissionInfo.name;
        newPermissionInfo.packageName = newPackageName;
        newPermissionInfo.protectionLevel = this.mPermissionInfo.protectionLevel;
        this.mPermissionInfo = newPermissionInfo;
        this.mReconciled = false;
        this.mUid = 0;
        this.mGids = EmptyArray.INT;
        this.mGidsPerUser = false;
    }

    public boolean addToTree(int protectionLevel, PermissionInfo permissionInfo, Permission permissionTree) {
        boolean changed = (this.mPermissionInfo.protectionLevel == protectionLevel && this.mReconciled && this.mUid == permissionTree.mUid && Objects.equals(this.mPermissionInfo.packageName, permissionTree.mPermissionInfo.packageName) && comparePermissionInfos(this.mPermissionInfo, permissionInfo)) ? false : true;
        PermissionInfo permissionInfo2 = new PermissionInfo(permissionInfo);
        this.mPermissionInfo = permissionInfo2;
        permissionInfo2.packageName = permissionTree.mPermissionInfo.packageName;
        this.mPermissionInfo.protectionLevel = protectionLevel;
        this.mReconciled = true;
        this.mUid = permissionTree.mUid;
        return changed;
    }

    public void updateDynamicPermission(Collection<Permission> permissionTrees) {
        Permission tree;
        if (PackageManagerService.DEBUG_SETTINGS) {
            Log.v(TAG, "Dynamic permission: name=" + getName() + " pkg=" + getPackageName() + " info=" + this.mPermissionInfo);
        }
        if (this.mType == 2 && (tree = findPermissionTree(permissionTrees, this.mPermissionInfo.name)) != null) {
            this.mPermissionInfo.packageName = tree.mPermissionInfo.packageName;
            this.mReconciled = true;
            this.mUid = tree.mUid;
        }
    }

    public static boolean isOverridingSystemPermission(Permission permission, PermissionInfo permissionInfo, PackageManagerInternal packageManagerInternal) {
        AndroidPackage currentPackage;
        if (permission == null || Objects.equals(permission.mPermissionInfo.packageName, permissionInfo.packageName) || !permission.mReconciled || (currentPackage = packageManagerInternal.getPackage(permission.mPermissionInfo.packageName)) == null) {
            return false;
        }
        return currentPackage.isSystem();
    }

    public static Permission createOrUpdate(Permission permission, PermissionInfo permissionInfo, AndroidPackage pkg, Collection<Permission> permissionTrees, boolean isOverridingSystemPermission) {
        Permission permission2 = permission;
        boolean ownerChanged = false;
        if (permission2 != null && !Objects.equals(permission2.mPermissionInfo.packageName, permissionInfo.packageName)) {
            if (pkg.isSystem()) {
                if (permission2.mType == 1 && !permission2.mReconciled) {
                    permissionInfo.flags |= 1073741824;
                    permission2.mPermissionInfo = permissionInfo;
                    permission2.mReconciled = true;
                    permission2.mUid = pkg.getUid();
                } else if (!isOverridingSystemPermission) {
                    Slog.w(TAG, "New decl " + pkg + " of permission  " + permissionInfo.name + " is system; overriding " + permission2.mPermissionInfo.packageName);
                    ownerChanged = true;
                    permission2 = null;
                }
            }
        }
        boolean wasNonInternal = (permission2 == null || permission2.mType == 1 || permission2.isInternal()) ? false : true;
        boolean wasNonRuntime = (permission2 == null || permission2.mType == 1 || permission2.isRuntime()) ? false : true;
        if (permission2 == null) {
            permission2 = new Permission(permissionInfo.name, permissionInfo.packageName, 0);
        }
        StringBuilder r = null;
        if (permission2.mReconciled) {
            if (PackageManagerService.DEBUG_PACKAGE_SCANNING) {
                if (0 == 0) {
                    r = new StringBuilder(256);
                } else {
                    r.append(' ');
                }
                r.append("DUP:");
                r.append(permissionInfo.name);
            }
        } else if (permission2.mPermissionInfo.packageName == null || permission2.mPermissionInfo.packageName.equals(permissionInfo.packageName)) {
            Permission tree = findPermissionTree(permissionTrees, permissionInfo.name);
            if (tree == null || tree.mPermissionInfo.packageName.equals(permissionInfo.packageName)) {
                permissionInfo.flags = 1073741824 | permissionInfo.flags;
                permission2.mPermissionInfo = permissionInfo;
                permission2.mReconciled = true;
                permission2.mUid = pkg.getUid();
                if (PackageManagerService.DEBUG_PACKAGE_SCANNING) {
                    if (0 == 0) {
                        r = new StringBuilder(256);
                    } else {
                        r.append(' ');
                    }
                    r.append(permissionInfo.name);
                }
            } else {
                Slog.w(TAG, "Permission " + permissionInfo.name + " from package " + permissionInfo.packageName + " ignored: base tree " + tree.mPermissionInfo.name + " is from package " + tree.mPermissionInfo.packageName);
            }
        } else {
            Slog.w(TAG, "Permission " + permissionInfo.name + " from package " + permissionInfo.packageName + " ignored: original from " + permission2.mPermissionInfo.packageName);
        }
        if ((permission2.isInternal() && (ownerChanged || wasNonInternal)) || (permission2.isRuntime() && (ownerChanged || wasNonRuntime))) {
            permission2.mDefinitionChanged = true;
        }
        if (PackageManagerService.DEBUG_PACKAGE_SCANNING && r != null) {
            Log.d(TAG, "  Permissions: " + ((Object) r));
        }
        return permission2;
    }

    public static Permission enforcePermissionTree(Collection<Permission> permissionTrees, String permissionName, int callingUid) {
        Permission permissionTree;
        if (permissionName != null && (permissionTree = findPermissionTree(permissionTrees, permissionName)) != null && permissionTree.getUid() == UserHandle.getAppId(callingUid)) {
            return permissionTree;
        }
        throw new SecurityException("Calling uid " + callingUid + " is not allowed to add to or remove from the permission tree");
    }

    private static Permission findPermissionTree(Collection<Permission> permissionTrees, String permissionName) {
        for (Permission permissionTree : permissionTrees) {
            String permissionTreeName = permissionTree.getName();
            if (permissionName.startsWith(permissionTreeName) && permissionName.length() > permissionTreeName.length() && permissionName.charAt(permissionTreeName.length()) == '.') {
                return permissionTree;
            }
        }
        return null;
    }

    public String getBackgroundPermission() {
        return this.mPermissionInfo.backgroundPermission;
    }

    public String getGroup() {
        return this.mPermissionInfo.group;
    }

    public int getProtection() {
        return this.mPermissionInfo.protectionLevel & 15;
    }

    public int getProtectionFlags() {
        return this.mPermissionInfo.protectionLevel & 65520;
    }

    public PermissionInfo generatePermissionInfo(int flags) {
        return generatePermissionInfo(flags, 10000);
    }

    public PermissionInfo generatePermissionInfo(int flags, int targetSdkVersion) {
        PermissionInfo permissionInfo;
        if (this.mPermissionInfo != null) {
            permissionInfo = new PermissionInfo(this.mPermissionInfo);
            if ((flags & 128) != 128) {
                permissionInfo.metaData = null;
            }
        } else {
            permissionInfo = new PermissionInfo();
            permissionInfo.name = this.mPermissionInfo.name;
            permissionInfo.packageName = this.mPermissionInfo.packageName;
            permissionInfo.nonLocalizedLabel = this.mPermissionInfo.name;
        }
        if (targetSdkVersion >= 26) {
            permissionInfo.protectionLevel = this.mPermissionInfo.protectionLevel;
        } else {
            int protection = this.mPermissionInfo.protectionLevel & 15;
            if (protection == 2) {
                permissionInfo.protectionLevel = this.mPermissionInfo.protectionLevel;
            } else {
                permissionInfo.protectionLevel = protection;
            }
        }
        return permissionInfo;
    }

    private static boolean comparePermissionInfos(PermissionInfo pi1, PermissionInfo pi2) {
        return pi1.icon == pi2.icon && pi1.logo == pi2.logo && pi1.protectionLevel == pi2.protectionLevel && Objects.equals(pi1.name, pi2.name) && Objects.equals(pi1.nonLocalizedLabel, pi2.nonLocalizedLabel) && Objects.equals(pi1.packageName, pi2.packageName);
    }
}
