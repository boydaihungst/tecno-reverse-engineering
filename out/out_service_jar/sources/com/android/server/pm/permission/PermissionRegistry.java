package com.android.server.pm.permission;

import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.server.pm.pkg.component.ParsedPermissionGroup;
import java.util.Collection;
/* loaded from: classes2.dex */
public class PermissionRegistry {
    private final ArrayMap<String, Permission> mPermissions = new ArrayMap<>();
    private final ArrayMap<String, Permission> mPermissionTrees = new ArrayMap<>();
    private final ArrayMap<String, ParsedPermissionGroup> mPermissionGroups = new ArrayMap<>();
    private final ArrayMap<String, ArraySet<String>> mAppOpPermissionPackages = new ArrayMap<>();

    public Collection<Permission> getPermissions() {
        return this.mPermissions.values();
    }

    public Permission getPermission(String permissionName) {
        return this.mPermissions.get(permissionName);
    }

    public void addPermission(Permission permission) {
        this.mPermissions.put(permission.getName(), permission);
    }

    public void removePermission(String permissionName) {
        this.mPermissions.remove(permissionName);
    }

    public Collection<Permission> getPermissionTrees() {
        return this.mPermissionTrees.values();
    }

    public Permission getPermissionTree(String permissionTreeName) {
        return this.mPermissionTrees.get(permissionTreeName);
    }

    public void addPermissionTree(Permission permissionTree) {
        this.mPermissionTrees.put(permissionTree.getName(), permissionTree);
    }

    public void transferPermissions(String oldPackageName, String newPackageName) {
        int i = 0;
        while (i < 2) {
            ArrayMap<String, Permission> permissions = i == 0 ? this.mPermissionTrees : this.mPermissions;
            for (Permission permission : permissions.values()) {
                permission.transfer(oldPackageName, newPackageName);
            }
            i++;
        }
    }

    public Collection<ParsedPermissionGroup> getPermissionGroups() {
        return this.mPermissionGroups.values();
    }

    public ParsedPermissionGroup getPermissionGroup(String permissionGroupName) {
        return this.mPermissionGroups.get(permissionGroupName);
    }

    public void addPermissionGroup(ParsedPermissionGroup permissionGroup) {
        this.mPermissionGroups.put(permissionGroup.getName(), permissionGroup);
    }

    public ArrayMap<String, ArraySet<String>> getAllAppOpPermissionPackages() {
        return this.mAppOpPermissionPackages;
    }

    public ArraySet<String> getAppOpPermissionPackages(String permissionName) {
        return this.mAppOpPermissionPackages.get(permissionName);
    }

    public void addAppOpPermissionPackage(String permissionName, String packageName) {
        ArraySet<String> packageNames = this.mAppOpPermissionPackages.get(permissionName);
        if (packageNames == null) {
            packageNames = new ArraySet<>();
            this.mAppOpPermissionPackages.put(permissionName, packageNames);
        }
        packageNames.add(packageName);
    }

    public void removeAppOpPermissionPackage(String permissionName, String packageName) {
        ArraySet<String> packageNames = this.mAppOpPermissionPackages.get(permissionName);
        if (packageNames == null) {
            return;
        }
        boolean removed = packageNames.remove(packageName);
        if (removed && packageNames.isEmpty()) {
            this.mAppOpPermissionPackages.remove(permissionName);
        }
    }

    public Permission enforcePermissionTree(String permissionName, int callingUid) {
        return Permission.enforcePermissionTree(this.mPermissionTrees.values(), permissionName, callingUid);
    }
}
