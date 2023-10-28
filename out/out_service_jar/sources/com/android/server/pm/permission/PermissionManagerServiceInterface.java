package com.android.server.pm.permission;

import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.permission.SplitPermissionInfoParcelable;
import android.permission.IOnPermissionsChangeListener;
import android.permission.PermissionManagerInternal;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
/* loaded from: classes2.dex */
public interface PermissionManagerServiceInterface extends PermissionManagerInternal {
    boolean addAllowlistedRestrictedPermission(String str, String str2, int i, int i2);

    void addOnPermissionsChangeListener(IOnPermissionsChangeListener iOnPermissionsChangeListener);

    void addOnRuntimePermissionStateChangedListener(PermissionManagerServiceInternal.OnRuntimePermissionStateChangedListener onRuntimePermissionStateChangedListener);

    boolean addPermission(PermissionInfo permissionInfo, boolean z);

    int checkPermission(String str, String str2, int i);

    int checkUidPermission(int i, String str);

    void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr);

    Map<String, Set<String>> getAllAppOpPermissionPackages();

    List<PermissionGroupInfo> getAllPermissionGroups(int i);

    ArrayList<PermissionInfo> getAllPermissionsWithProtection(int i);

    ArrayList<PermissionInfo> getAllPermissionsWithProtectionFlags(int i);

    List<String> getAllowlistedRestrictedPermissions(String str, int i, int i2);

    String[] getAppOpPermissionPackages(String str);

    int[] getGidsForUid(int i);

    Set<String> getGrantedPermissions(String str, int i);

    LegacyPermissionState getLegacyPermissionState(int i);

    List<LegacyPermission> getLegacyPermissions();

    int getPermissionFlags(String str, String str2, int i);

    int[] getPermissionGids(String str, int i);

    PermissionGroupInfo getPermissionGroupInfo(String str, int i);

    PermissionInfo getPermissionInfo(String str, String str2, int i);

    Permission getPermissionTEMP(String str);

    List<SplitPermissionInfoParcelable> getSplitPermissions();

    void grantRuntimePermission(String str, String str2, int i);

    boolean isPermissionRevokedByPolicy(String str, String str2, int i);

    boolean isPermissionsReviewRequired(String str, int i);

    void onPackageAdded(AndroidPackage androidPackage, boolean z, AndroidPackage androidPackage2);

    void onPackageInstalled(AndroidPackage androidPackage, int i, PermissionManagerServiceInternal.PackageInstalledParams packageInstalledParams, int i2);

    void onPackageRemoved(AndroidPackage androidPackage);

    void onPackageUninstalled(String str, int i, AndroidPackage androidPackage, List<AndroidPackage> list, int i2);

    void onStorageVolumeMounted(String str, boolean z);

    void onSystemReady();

    void onUserCreated(int i);

    void onUserRemoved(int i);

    List<PermissionInfo> queryPermissionsByGroup(String str, int i);

    void readLegacyPermissionStateTEMP();

    void readLegacyPermissionsTEMP(LegacyPermissionSettings legacyPermissionSettings);

    boolean removeAllowlistedRestrictedPermission(String str, String str2, int i, int i2);

    void removeOnPermissionsChangeListener(IOnPermissionsChangeListener iOnPermissionsChangeListener);

    void removeOnRuntimePermissionStateChangedListener(PermissionManagerServiceInternal.OnRuntimePermissionStateChangedListener onRuntimePermissionStateChangedListener);

    void removePermission(String str);

    void resetRuntimePermissions(AndroidPackage androidPackage, int i);

    void revokePostNotificationPermissionWithoutKillForTest(String str, int i);

    void revokeRuntimePermission(String str, String str2, int i, String str3);

    boolean shouldShowRequestPermissionRationale(String str, String str2, int i);

    void updatePermissionFlags(String str, String str2, int i, int i2, boolean z, int i3);

    void updatePermissionFlagsForAllApps(int i, int i2, int i3);

    void writeLegacyPermissionStateTEMP();

    void writeLegacyPermissionsTEMP(LegacyPermissionSettings legacyPermissionSettings);
}
