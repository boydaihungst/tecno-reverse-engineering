package com.android.server.pm.dex;

import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.os.FileUtils;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.EventLog;
import android.util.PackageUtils;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.pm.Installer;
import com.android.server.pm.dex.PackageDynamicCodeLoading;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import libcore.util.HexEncoding;
/* loaded from: classes2.dex */
public class DynamicCodeLogger {
    private static final String DCL_DEX_SUBTAG = "dcl";
    private static final String DCL_NATIVE_SUBTAG = "dcln";
    private static final int SNET_TAG = 1397638484;
    private static final String TAG = "DynamicCodeLogger";
    private final Installer mInstaller;
    private final PackageDynamicCodeLoading mPackageDynamicCodeLoading;
    private IPackageManager mPackageManager;

    /* JADX INFO: Access modifiers changed from: package-private */
    public DynamicCodeLogger(Installer installer) {
        this.mInstaller = installer;
        this.mPackageDynamicCodeLoading = new PackageDynamicCodeLoading();
    }

    DynamicCodeLogger(IPackageManager packageManager, Installer installer, PackageDynamicCodeLoading packageDynamicCodeLoading) {
        this.mPackageManager = packageManager;
        this.mInstaller = installer;
        this.mPackageDynamicCodeLoading = packageDynamicCodeLoading;
    }

    private IPackageManager getPackageManager() {
        if (this.mPackageManager == null) {
            this.mPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        }
        return this.mPackageManager;
    }

    public Set<String> getAllPackagesWithDynamicCodeLoading() {
        return this.mPackageDynamicCodeLoading.getAllPackagesWithDynamicCodeLoading();
    }

    /* JADX WARN: Incorrect condition in loop: B:7:0x0021 */
    /* JADX WARN: Removed duplicated region for block: B:43:0x0111  */
    /* JADX WARN: Removed duplicated region for block: B:44:0x0114  */
    /* JADX WARN: Removed duplicated region for block: B:54:0x0177  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void logDynamicCodeLoading(String packageName) {
        ApplicationInfo appInfo;
        ApplicationInfo appInfo2;
        boolean needWrite;
        int storageFlags;
        PackageDynamicCodeLoading.PackageDynamicCode info;
        SparseArray<ApplicationInfo> appInfoByUser;
        Iterator<Map.Entry<String, PackageDynamicCodeLoading.DynamicCodeFile>> it;
        ApplicationInfo appInfo3;
        String str;
        long j;
        byte[] hash;
        String str2;
        String message;
        String message2;
        PackageDynamicCodeLoading.PackageDynamicCode info2 = getPackageDynamicCodeInfo(packageName);
        if (info2 == null) {
            return;
        }
        SparseArray<ApplicationInfo> appInfoByUser2 = new SparseArray<>();
        Iterator<Map.Entry<String, PackageDynamicCodeLoading.DynamicCodeFile>> it2 = info2.mFileUsageMap.entrySet().iterator();
        boolean needWrite2 = false;
        while (needWrite) {
            Map.Entry<String, PackageDynamicCodeLoading.DynamicCodeFile> fileEntry = it2.next();
            String filePath = fileEntry.getKey();
            PackageDynamicCodeLoading.DynamicCodeFile fileInfo = fileEntry.getValue();
            int userId = fileInfo.mUserId;
            int index = appInfoByUser2.indexOfKey(userId);
            if (index < 0) {
                try {
                    PackageInfo ownerInfo = getPackageManager().getPackageInfo(packageName, 0L, userId);
                    appInfo = ownerInfo == null ? null : ownerInfo.applicationInfo;
                } catch (RemoteException e) {
                    appInfo = null;
                }
                appInfoByUser2.put(userId, appInfo);
                if (appInfo != null) {
                    appInfo2 = appInfo;
                    needWrite = needWrite2;
                } else {
                    Slog.d(TAG, "Could not find package " + packageName + " for user " + userId);
                    appInfo2 = appInfo;
                    needWrite = needWrite2 | this.mPackageDynamicCodeLoading.removeUserPackage(packageName, userId);
                }
            } else {
                appInfo2 = appInfoByUser2.get(userId);
                needWrite = needWrite2;
            }
            if (appInfo2 != null) {
                if (!fileIsUnder(filePath, appInfo2.credentialProtectedDataDir)) {
                    if (fileIsUnder(filePath, appInfo2.deviceProtectedDataDir)) {
                        storageFlags = 1;
                    } else {
                        Slog.e(TAG, "Could not infer CE/DE storage for path " + filePath);
                        needWrite2 = needWrite | this.mPackageDynamicCodeLoading.removeFile(packageName, filePath, userId);
                        info2 = info2;
                        appInfoByUser2 = appInfoByUser2;
                    }
                } else {
                    storageFlags = 2;
                }
                try {
                    Installer installer = this.mInstaller;
                    int i = appInfo2.uid;
                    String str3 = appInfo2.volumeUuid;
                    info = info2;
                    appInfo3 = appInfo2;
                    appInfoByUser = appInfoByUser2;
                    str = TAG;
                    it = it2;
                    j = 0;
                    try {
                        byte[] hash2 = installer.hashSecondaryDexFile(filePath, packageName, i, str3, storageFlags);
                        hash = hash2;
                    } catch (Installer.InstallerException e2) {
                        e = e2;
                        Slog.e(str, "Got InstallerException when hashing file " + filePath + ": " + e.getMessage());
                        hash = null;
                        if (fileInfo.mFileType != 'D') {
                        }
                        String subtag = str2;
                        String fileName = new File(filePath).getName();
                        message = PackageUtils.computeSha256Digest(fileName.getBytes());
                        if (hash == null) {
                        }
                        Slog.d(str, "Got no hash for " + filePath);
                        needWrite |= this.mPackageDynamicCodeLoading.removeFile(packageName, filePath, userId);
                        message2 = message;
                        while (r6.hasNext()) {
                        }
                        needWrite2 = needWrite;
                        info2 = info;
                        appInfoByUser2 = appInfoByUser;
                        it2 = it;
                    }
                } catch (Installer.InstallerException e3) {
                    e = e3;
                    info = info2;
                    appInfoByUser = appInfoByUser2;
                    it = it2;
                    appInfo3 = appInfo2;
                    str = TAG;
                    j = 0;
                }
                if (fileInfo.mFileType != 'D') {
                    str2 = DCL_DEX_SUBTAG;
                } else {
                    str2 = DCL_NATIVE_SUBTAG;
                }
                String subtag2 = str2;
                String fileName2 = new File(filePath).getName();
                message = PackageUtils.computeSha256Digest(fileName2.getBytes());
                if (hash == null && hash.length == 32) {
                    message2 = message + ' ' + HexEncoding.encodeToString(hash);
                } else {
                    Slog.d(str, "Got no hash for " + filePath);
                    needWrite |= this.mPackageDynamicCodeLoading.removeFile(packageName, filePath, userId);
                    message2 = message;
                }
                for (String loadingPackageName : fileInfo.mLoadingPackages) {
                    int loadingUid = -1;
                    if (loadingPackageName.equals(packageName)) {
                        loadingUid = appInfo3.uid;
                    } else {
                        try {
                            loadingUid = getPackageManager().getPackageUid(loadingPackageName, j, userId);
                        } catch (RemoteException e4) {
                        }
                    }
                    if (loadingUid != -1) {
                        writeDclEvent(subtag2, loadingUid, message2);
                    }
                }
                needWrite2 = needWrite;
                info2 = info;
                appInfoByUser2 = appInfoByUser;
                it2 = it;
            } else {
                needWrite2 = needWrite;
            }
        }
        if (needWrite2) {
            this.mPackageDynamicCodeLoading.maybeWriteAsync();
        }
    }

    private boolean fileIsUnder(String filePath, String directoryPath) {
        if (directoryPath == null) {
            return false;
        }
        try {
            return FileUtils.contains(new File(directoryPath).getCanonicalPath(), new File(filePath).getCanonicalPath());
        } catch (IOException e) {
            return false;
        }
    }

    PackageDynamicCodeLoading.PackageDynamicCode getPackageDynamicCodeInfo(String packageName) {
        return this.mPackageDynamicCodeLoading.getPackageDynamicCodeInfo(packageName);
    }

    void writeDclEvent(String subtag, int uid, String message) {
        EventLog.writeEvent((int) SNET_TAG, subtag, Integer.valueOf(uid), message);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void recordDex(int loaderUserId, String dexPath, String owningPackageName, String loadingPackageName) {
        if (this.mPackageDynamicCodeLoading.record(owningPackageName, dexPath, 68, loaderUserId, loadingPackageName)) {
            this.mPackageDynamicCodeLoading.maybeWriteAsync();
        }
    }

    public void recordNative(int loadingUid, String path) {
        try {
            String[] packages = getPackageManager().getPackagesForUid(loadingUid);
            if (packages != null) {
                if (packages.length == 0) {
                    return;
                }
                String loadingPackageName = packages[0];
                int loadingUserId = UserHandle.getUserId(loadingUid);
                if (this.mPackageDynamicCodeLoading.record(loadingPackageName, path, 78, loadingUserId, loadingPackageName)) {
                    this.mPackageDynamicCodeLoading.maybeWriteAsync();
                }
            }
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clear() {
        this.mPackageDynamicCodeLoading.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removePackage(String packageName) {
        if (this.mPackageDynamicCodeLoading.removePackage(packageName)) {
            this.mPackageDynamicCodeLoading.maybeWriteAsync();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeUserPackage(String packageName, int userId) {
        if (this.mPackageDynamicCodeLoading.removeUserPackage(packageName, userId)) {
            this.mPackageDynamicCodeLoading.maybeWriteAsync();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void readAndSync(Map<String, Set<Integer>> packageToUsersMap) {
        this.mPackageDynamicCodeLoading.read();
        this.mPackageDynamicCodeLoading.syncData(packageToUsersMap);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeNow() {
        this.mPackageDynamicCodeLoading.writeNow();
    }
}
