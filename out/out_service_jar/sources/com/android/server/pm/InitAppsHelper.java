package com.android.server.pm;

import android.content.pm.parsing.ApkLiteParseUtils;
import android.os.Environment;
import android.os.SystemClock;
import android.os.Trace;
import android.util.ArrayMap;
import android.util.EventLog;
import android.util.Slog;
import com.android.internal.content.om.OverlayConfig;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.function.TriConsumer;
import com.android.server.EventLogTags;
import com.android.server.pm.ApexManager;
import com.android.server.pm.parsing.PackageCacher;
import com.android.server.pm.parsing.PackageParser2;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.utils.WatchedArrayMap;
import com.transsion.hubcore.server.pm.ITranPackageManagerService;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public final class InitAppsHelper {
    private final ApexManager mApexManager;
    private int mCachedSystemApps;
    private final ExecutorService mExecutorService;
    private final InstallPackageHelper mInstallPackageHelper;
    private final boolean mIsDeviceUpgrading;
    private final boolean mIsOnlyCoreApps;
    private final PackageManagerService mPm;
    private final int mScanFlags;
    private int mSystemPackagesCount;
    private final int mSystemParseFlags;
    private final List<ScanPartition> mSystemPartitions;
    private final int mSystemScanFlags;
    private long mSystemScanTime;
    private int scanFlags;
    private final ArrayMap<String, File> mExpectingBetter = new ArrayMap<>();
    private final List<String> mPossiblyDeletedUpdatedSystemApps = new ArrayList();
    private final List<String> mStubSystemApps = new ArrayList();
    private final List<ScanPartition> mDirsToScanAsSystem = getSystemScanPartitions();

    /* JADX INFO: Access modifiers changed from: package-private */
    public InitAppsHelper(PackageManagerService pm, ApexManager apexManager, InstallPackageHelper installPackageHelper, List<ScanPartition> systemPartitions) {
        this.mPm = pm;
        this.mApexManager = apexManager;
        this.mInstallPackageHelper = installPackageHelper;
        this.mSystemPartitions = systemPartitions;
        boolean isDeviceUpgrading = pm.isDeviceUpgrading();
        this.mIsDeviceUpgrading = isDeviceUpgrading;
        this.mIsOnlyCoreApps = pm.isOnlyCoreApps();
        this.scanFlags = 528;
        if (isDeviceUpgrading || pm.isFirstBoot()) {
            this.mScanFlags = this.scanFlags | 4096;
        } else {
            this.mScanFlags = this.scanFlags;
        }
        this.mSystemParseFlags = pm.getDefParseFlags() | 16;
        this.mSystemScanFlags = this.mScanFlags | 65536;
        this.mExecutorService = ParallelPackageParser.makeExecutorService();
    }

    private List<File> getFrameworkResApkSplitFiles() {
        File[] listFiles;
        Trace.traceBegin(262144L, "scanFrameworkResApkSplits");
        try {
            List<File> splits = new ArrayList<>();
            List<ApexManager.ActiveApexInfo> activeApexInfos = this.mPm.mApexManager.getActiveApexInfos();
            for (int i = 0; i < activeApexInfos.size(); i++) {
                ApexManager.ActiveApexInfo apexInfo = activeApexInfos.get(i);
                File splitsFolder = new File(apexInfo.apexDirectory, "etc/splits");
                if (splitsFolder.isDirectory()) {
                    for (File file : splitsFolder.listFiles()) {
                        if (ApkLiteParseUtils.isApkFile(file)) {
                            splits.add(file);
                        }
                    }
                }
            }
            return splits;
        } finally {
            Trace.traceEnd(262144L);
        }
    }

    private List<ScanPartition> getSystemScanPartitions() {
        List<ScanPartition> scanPartitions = new ArrayList<>();
        scanPartitions.addAll(this.mSystemPartitions);
        scanPartitions.addAll(getApexScanPartitions());
        Slog.d("PackageManager", "Directories scanned as system partitions: " + scanPartitions);
        return scanPartitions;
    }

    private List<ScanPartition> getApexScanPartitions() {
        List<ScanPartition> scanPartitions = new ArrayList<>();
        List<ApexManager.ActiveApexInfo> activeApexInfos = this.mApexManager.getActiveApexInfos();
        for (int i = 0; i < activeApexInfos.size(); i++) {
            ScanPartition scanPartition = resolveApexToScanPartition(activeApexInfos.get(i));
            if (scanPartition != null) {
                scanPartitions.add(scanPartition);
            }
        }
        return scanPartitions;
    }

    private static ScanPartition resolveApexToScanPartition(ApexManager.ActiveApexInfo apexInfo) {
        int size = PackageManagerService.SYSTEM_PARTITIONS.size();
        for (int i = 0; i < size; i++) {
            ScanPartition sp = PackageManagerService.SYSTEM_PARTITIONS.get(i);
            if (apexInfo.preInstalledApexPath.getAbsolutePath().equals(sp.getFolder().getAbsolutePath()) || apexInfo.preInstalledApexPath.getAbsolutePath().startsWith(sp.getFolder().getAbsolutePath() + File.separator)) {
                int flags = apexInfo.activeApexChanged ? 8388608 | 16777216 : 8388608;
                return new ScanPartition(apexInfo.apexDirectory, sp, flags);
            }
        }
        return null;
    }

    public OverlayConfig initSystemApps(PackageParser2 packageParser, WatchedArrayMap<String, PackageSetting> packageSettings, int[] userIds, long startTime) {
        this.mApexManager.scanApexPackagesTraced(packageParser, this.mExecutorService);
        scanSystemDirs(packageParser, this.mExecutorService);
        PackageManagerService.sPmsExt.scanMoreDirLi(this.mPm.mDefParseFlags, this.mScanFlags, packageParser, this.mExecutorService);
        ITranPackageManagerService.Instance().scanTrDirLi(this.mPm.mDefParseFlags, this.mScanFlags, packageParser, this.mExecutorService);
        final ArrayMap<String, File> apkInApexPreInstalledPaths = new ArrayMap<>();
        for (ApexManager.ActiveApexInfo apexInfo : this.mApexManager.getActiveApexInfos()) {
            for (String packageName : this.mApexManager.getApksInApex(apexInfo.apexModuleName)) {
                apkInApexPreInstalledPaths.put(packageName, apexInfo.preInstalledApexPath);
            }
        }
        OverlayConfig overlayConfig = OverlayConfig.initializeSystemInstance(new OverlayConfig.PackageProvider() { // from class: com.android.server.pm.InitAppsHelper$$ExternalSyntheticLambda0
            public final void forEachPackage(TriConsumer triConsumer) {
                InitAppsHelper.this.m5417lambda$initSystemApps$1$comandroidserverpmInitAppsHelper(apkInApexPreInstalledPaths, triConsumer);
            }
        });
        if (!this.mIsOnlyCoreApps) {
            updateStubSystemAppsList(this.mStubSystemApps);
            this.mInstallPackageHelper.prepareSystemPackageCleanUp(packageSettings, this.mPossiblyDeletedUpdatedSystemApps, this.mExpectingBetter, userIds);
        }
        logSystemAppsScanningTime(startTime);
        return overlayConfig;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$initSystemApps$1$com-android-server-pm-InitAppsHelper  reason: not valid java name */
    public /* synthetic */ void m5417lambda$initSystemApps$1$comandroidserverpmInitAppsHelper(final ArrayMap apkInApexPreInstalledPaths, final TriConsumer consumer) {
        PackageManagerService packageManagerService = this.mPm;
        packageManagerService.forEachPackage(packageManagerService.snapshotComputer(), new Consumer() { // from class: com.android.server.pm.InitAppsHelper$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                consumer.accept(r3, Boolean.valueOf(r3.isSystem()), (File) apkInApexPreInstalledPaths.get(((AndroidPackage) obj).getPackageName()));
            }
        });
    }

    private void logSystemAppsScanningTime(long startTime) {
        int i;
        this.mCachedSystemApps = PackageCacher.sCachedPackageReadCount.get();
        this.mPm.mSettings.pruneSharedUsersLPw();
        this.mSystemScanTime = SystemClock.uptimeMillis() - startTime;
        this.mSystemPackagesCount = this.mPm.mPackages.size();
        StringBuilder append = new StringBuilder().append("Finished scanning system apps. Time: ").append(this.mSystemScanTime).append(" ms, packageCount: ").append(this.mSystemPackagesCount).append(" , timePerPackage: ");
        int i2 = this.mSystemPackagesCount;
        Slog.i("PackageManager", append.append(i2 == 0 ? 0L : this.mSystemScanTime / i2).append(" , cached: ").append(this.mCachedSystemApps).toString());
        if (this.mIsDeviceUpgrading && (i = this.mSystemPackagesCount) > 0) {
            FrameworkStatsLog.write(239, 15, this.mSystemScanTime / i);
        }
    }

    public void initNonSystemApps(PackageParser2 packageParser, int[] userIds, long startTime) {
        if (!this.mIsOnlyCoreApps) {
            EventLog.writeEvent((int) EventLogTags.BOOT_PROGRESS_PMS_DATA_SCAN_START, SystemClock.uptimeMillis());
            scanDirTracedLI(this.mPm.getAppInstallDir(), null, 0, this.mScanFlags | 128, packageParser, this.mExecutorService);
        }
        List<Runnable> unfinishedTasks = this.mExecutorService.shutdownNow();
        if (!unfinishedTasks.isEmpty()) {
            throw new IllegalStateException("Not all tasks finished before calling close: " + unfinishedTasks);
        }
        if (!this.mIsOnlyCoreApps) {
            fixSystemPackages(userIds);
            logNonSystemAppScanningTime(startTime);
        }
        this.mExpectingBetter.clear();
        this.mPm.mSettings.pruneRenamedPackagesLPw();
    }

    private void fixSystemPackages(int[] userIds) {
        this.mInstallPackageHelper.cleanupDisabledPackageSettings(this.mPossiblyDeletedUpdatedSystemApps, userIds, this.mScanFlags);
        this.mInstallPackageHelper.checkExistingBetterPackages(this.mExpectingBetter, this.mStubSystemApps, this.mSystemScanFlags, this.mSystemParseFlags);
        this.mInstallPackageHelper.installSystemStubPackages(this.mStubSystemApps, this.mScanFlags);
    }

    private void logNonSystemAppScanningTime(long startTime) {
        int cachedNonSystemApps = PackageCacher.sCachedPackageReadCount.get() - this.mCachedSystemApps;
        long dataScanTime = (SystemClock.uptimeMillis() - this.mSystemScanTime) - startTime;
        int dataPackagesCount = this.mPm.mPackages.size() - this.mSystemPackagesCount;
        Slog.i("PackageManager", "Finished scanning non-system apps. Time: " + dataScanTime + " ms, packageCount: " + dataPackagesCount + " , timePerPackage: " + (dataPackagesCount == 0 ? 0L : dataScanTime / dataPackagesCount) + " , cached: " + cachedNonSystemApps);
        if (this.mIsDeviceUpgrading && dataPackagesCount > 0) {
            FrameworkStatsLog.write(239, 14, dataScanTime / dataPackagesCount);
        }
    }

    private void scanSystemDirs(PackageParser2 packageParser, ExecutorService executorService) {
        File frameworkDir = new File(Environment.getRootDirectory(), "framework");
        for (int i = this.mDirsToScanAsSystem.size() - 1; i >= 0; i--) {
            ScanPartition partition = this.mDirsToScanAsSystem.get(i);
            PackageManagerService.sPmsExt.scanDirLI(partition.type, true, this.mSystemParseFlags, this.mSystemScanFlags | partition.scanFlag, 0L, packageParser, executorService);
            if (partition.getOverlayFolder() != null) {
                scanDirTracedLI(partition.getOverlayFolder(), null, this.mSystemParseFlags, partition.scanFlag | this.mSystemScanFlags, packageParser, executorService);
            }
        }
        PackageManagerService.sPmsExt.scanDirLI(12, this.mSystemParseFlags, this.mSystemScanFlags | 1 | 131072, 0L, packageParser, executorService);
        scanDirTracedLI(frameworkDir, null, this.mSystemParseFlags, this.mSystemScanFlags | 1 | 131072, packageParser, executorService);
        if (this.mPm.mPackages.containsKey(PackageManagerService.PLATFORM_PACKAGE_NAME)) {
            PackageManagerService.sPmsExt.scanDirLI(13, this.mSystemParseFlags, this.mSystemScanFlags | 1 | 131072 | 524288, 0L, packageParser, executorService);
            PackageManagerService.sPmsExt.scanDirLI(2, this.mSystemParseFlags, this.mSystemScanFlags | 1 | 131072 | 524288, 0L, packageParser, executorService);
            int size = this.mDirsToScanAsSystem.size();
            for (int i2 = 0; i2 < size; i2++) {
                ScanPartition partition2 = this.mDirsToScanAsSystem.get(i2);
                PackageManagerService.sPmsExt.scanDirLI(partition2.type, false, this.mSystemParseFlags, this.mSystemScanFlags | 131072 | partition2.scanFlag, 0L, packageParser, executorService);
                if (partition2.getPrivAppFolder() != null) {
                    scanDirTracedLI(partition2.getPrivAppFolder(), null, this.mSystemParseFlags, partition2.scanFlag | this.mSystemScanFlags | 131072, packageParser, executorService);
                }
                PackageManagerService.sPmsExt.scanDirLI(partition2.type, false, this.mSystemParseFlags, this.mSystemScanFlags | partition2.scanFlag, 0L, packageParser, executorService);
                scanDirTracedLI(partition2.getAppFolder(), null, this.mSystemParseFlags, partition2.scanFlag | this.mSystemScanFlags, packageParser, executorService);
            }
            return;
        }
        throw new IllegalStateException("Failed to load frameworks package; check log for warnings");
    }

    private void updateStubSystemAppsList(List<String> stubSystemApps) {
        int numPackages = this.mPm.mPackages.size();
        for (int index = 0; index < numPackages; index++) {
            AndroidPackage pkg = this.mPm.mPackages.valueAt(index);
            if (pkg.isStub()) {
                stubSystemApps.add(pkg.getPackageName());
            }
        }
    }

    public void scanDirTracedLI(File scanDir, List<File> frameworkSplits, int parseFlags, int scanFlags, PackageParser2 packageParser, ExecutorService executorService) {
        int parseFlags2;
        Trace.traceBegin(262144L, "scanDir [" + scanDir.getAbsolutePath() + "]");
        if ((scanFlags & 8388608) == 0) {
            parseFlags2 = parseFlags;
        } else {
            parseFlags2 = parseFlags | 512;
        }
        try {
            this.mInstallPackageHelper.installPackagesFromDir(scanDir, frameworkSplits, parseFlags2, scanFlags, packageParser, executorService);
        } finally {
            Trace.traceEnd(262144L);
        }
    }

    public boolean isExpectingBetter(String packageName) {
        return this.mExpectingBetter.containsKey(packageName);
    }

    public List<ScanPartition> getDirsToScanAsSystem() {
        return this.mDirsToScanAsSystem;
    }
}
