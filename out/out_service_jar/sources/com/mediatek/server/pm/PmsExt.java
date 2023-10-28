package com.mediatek.server.pm;

import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfoInternal;
import android.content.pm.PackageInfo;
import android.os.RemoteException;
import android.os.UserHandle;
import com.android.server.pm.PackageManagerException;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.PackageSetting;
import com.android.server.pm.UserManagerService;
import com.android.server.pm.parsing.PackageParser2;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import com.android.server.utils.WatchedArrayMap;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.ExecutorService;
/* loaded from: classes2.dex */
public class PmsExt {
    public static final int INDEX_CIP_FW = 1;
    public static final int INDEX_CUSTOM_APP = 6;
    public static final int INDEX_CUSTOM_PLUGIN = 7;
    public static final int INDEX_PRODUCT_OP_APP = 24;
    public static final int INDEX_ROOT_PLUGIN = 4;
    public static final int INDEX_RSC_PRODUCT_APP = 19;
    public static final int INDEX_RSC_PRODUCT_OVERLAY = 10;
    public static final int INDEX_RSC_PRODUCT_PRIV = 18;
    public static final int INDEX_RSC_SYSTEM_APP = 15;
    public static final int INDEX_RSC_SYSTEM_EXT_APP = 17;
    public static final int INDEX_RSC_SYSTEM_EXT_OVERLAY = 9;
    public static final int INDEX_RSC_SYSTEM_EXT_PRIV = 16;
    public static final int INDEX_RSC_SYSTEM_FW = 12;
    public static final int INDEX_RSC_SYSTEM_OVERLAY = 8;
    public static final int INDEX_RSC_SYSTEM_PLUGIN = 22;
    public static final int INDEX_RSC_SYSTEM_PRIV = 14;
    public static final int INDEX_RSC_VENDOR_APP = 21;
    public static final int INDEX_RSC_VENDOR_FW = 13;
    public static final int INDEX_RSC_VENDOR_OVERLAY = 11;
    public static final int INDEX_RSC_VENDOR_PLUGIN = 23;
    public static final int INDEX_RSC_VENDOR_PRIV = 20;
    public static final int INDEX_VENDOR_FW = 2;
    public static final int INDEX_VENDOR_OP_APP = 3;
    public static final int INDEX_VENDOR_PLUGIN = 5;

    public void init(PackageManagerService pms, UserManagerService ums) {
    }

    public void scanDirLI(int partitionType, boolean isScanOverlay, int parseFlags, int scanFlags, long currentTime, PackageParser2 packageParser, ExecutorService executorService) {
    }

    public void scanDirLI(int ident, int defParseFlags, int defScanFlags, long currentTime, PackageParser2 packageParser, ExecutorService executorService) {
    }

    public void scanMoreDirLi(int defParseFlags, int defScanFlags, PackageParser2 packageParser, ExecutorService executorService) {
    }

    public void checkMtkResPkg(AndroidPackage pkg) throws PackageManagerException {
    }

    public boolean needSkipScanning(ParsedPackage pkg, PackageSetting updatedPkg, PackageSetting ps) {
        return false;
    }

    public boolean needSkipAppInfo(ApplicationInfo ai) {
        return false;
    }

    public void onPackageAdded(String packageName, PackageSetting pkgSetting, int userId) {
    }

    public void initBeforeScan() {
    }

    public void initAfterScan(WatchedArrayMap<String, PackageSetting> settingsPackages) {
    }

    public int customizeInstallPkgFlags(int installFlags, String packageName, WatchedArrayMap<String, PackageSetting> settingsPackages, UserHandle user) {
        return installFlags;
    }

    public void updatePackageSettings(int userId, String pkgName, AndroidPackage newPackage, PackageSetting ps, int[] allUsers, String installerPackageName) {
    }

    public int customizeDeletePkgFlags(int deleteFlags, String packageName) {
        return deleteFlags;
    }

    public int customizeDeletePkg(int[] users, String packageName, int versionCode, int delFlags, boolean removedBySystem) {
        return 1;
    }

    public boolean dumpCmdHandle(String cmd, PrintWriter pw, String[] args, int opti) {
        return false;
    }

    public ApplicationInfo updateApplicationInfoForRemovable(ApplicationInfo oldAppInfo) {
        return oldAppInfo;
    }

    public ApplicationInfo updateApplicationInfoForRemovable(String nameForUid, ApplicationInfo oldAppInfo) {
        return oldAppInfo;
    }

    public ActivityInfo updateActivityInfoForRemovable(ActivityInfo info) throws RemoteException {
        return info;
    }

    public List<LauncherActivityInfoInternal> updateResolveInfoListForRemovable(List<LauncherActivityInfoInternal> apps) throws RemoteException {
        return apps;
    }

    public PackageInfo updatePackageInfoForRemovable(PackageInfo oldPkgInfo) {
        return oldPkgInfo;
    }

    public boolean isRemovableSysApp(String pkgName) {
        return false;
    }

    public boolean updateNativeLibDir(ApplicationInfo info, String codePath) {
        return false;
    }

    public void addToRemovableSystemAppSet(String packageName) {
    }

    public void restoreAppList(List<String> mReInstallApps) {
    }
}
