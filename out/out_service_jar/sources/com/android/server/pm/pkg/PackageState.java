package com.android.server.pm.pkg;

import android.content.pm.SharedLibraryInfo;
import android.content.pm.SigningInfo;
import android.util.SparseArray;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
/* loaded from: classes2.dex */
public interface PackageState {
    AndroidPackageApi getAndroidPackage();

    int getAppId();

    int getCategoryOverride();

    String getCpuAbiOverride();

    long getLastModifiedTime();

    long[] getLastPackageUsageTime();

    long getLastUpdateTime();

    Map<String, Set<String>> getMimeGroups();

    String getPackageName();

    File getPath();

    String getPrimaryCpuAbi();

    String getSecondaryCpuAbi();

    int getSharedUserAppId();

    SigningInfo getSigningInfo();

    SparseArray<? extends PackageUserState> getUserStates();

    List<String> getUsesLibraryFiles();

    List<SharedLibraryInfo> getUsesLibraryInfos();

    String[] getUsesSdkLibraries();

    long[] getUsesSdkLibrariesVersionsMajor();

    String[] getUsesStaticLibraries();

    long[] getUsesStaticLibrariesVersions();

    long getVersionCode();

    String getVolumeUuid();

    boolean hasSharedUser();

    boolean isExternalStorage();

    boolean isForceQueryableOverride();

    boolean isHiddenUntilInstalled();

    boolean isInstallPermissionsFixed();

    boolean isOdm();

    boolean isOem();

    boolean isPrivileged();

    boolean isProduct();

    boolean isRequiredForSystemUser();

    boolean isSystem();

    boolean isSystemExt();

    boolean isUpdateAvailable();

    boolean isUpdatedSystemApp();

    boolean isVendor();

    default PackageUserState getUserStateOrDefault(int userId) {
        PackageUserState userState = getUserStates().get(userId);
        return userState == null ? PackageUserState.DEFAULT : userState;
    }
}
