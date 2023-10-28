package com.android.server.pm;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.SharedLibraryInfo;
import android.content.pm.dex.ArtManager;
import android.os.Binder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import com.android.internal.logging.MetricsLogger;
import com.android.server.pm.ApexManager;
import com.android.server.pm.PackageDexOptimizer;
import com.android.server.pm.dex.DexManager;
import com.android.server.pm.dex.DexoptOptions;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.pkg.PackageStateInternal;
import com.transsion.hubcore.server.pm.ITranPackageManagerService;
import dalvik.system.DexFile;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class DexOptHelper {
    private static final long SEVEN_DAYS_IN_MILLISECONDS = 604800000;
    public static final boolean THUBCORE_SUPPORT;
    private boolean mDexOptDialogShown;
    private final Object mLock = new Object();
    private final PackageManagerService mPm;

    static {
        THUBCORE_SUPPORT = 1 == SystemProperties.getInt("ro.thub.core.support", 0);
    }

    public boolean isDexOptDialogShown() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mDexOptDialogShown;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DexOptHelper(PackageManagerService pm) {
        this.mPm = pm;
    }

    private static String getPrebuildProfilePath(AndroidPackage pkg) {
        return pkg.getBaseApkPath() + ".prof";
    }

    /* JADX WARN: Removed duplicated region for block: B:100:0x0129 A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:99:0x0150 A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public int[] performDexOptUpgrade(List<AndroidPackage> pkgs, boolean showDialog, int compilationReason, boolean bootComplete) {
        Iterator<AndroidPackage> it;
        int pkgCompilationReason = 0;
        int numberOfPackagesOptimized = 0;
        int numberOfPackagesSkipped = 0;
        int numberOfPackagesFailed = 0;
        int numberOfPackagesToDexopt = pkgs.size();
        Iterator<AndroidPackage> it2 = pkgs.iterator();
        while (it2.hasNext()) {
            AndroidPackage pkg = it2.next();
            int numberOfPackagesVisited = pkgCompilationReason + 1;
            boolean useProfileForDexopt = false;
            if (!this.mPm.isFirstBoot() && !this.mPm.isDeviceUpgrading()) {
                it = it2;
            } else if (!pkg.isSystem()) {
                it = it2;
            } else {
                File profileFile = new File(getPrebuildProfilePath(pkg));
                if (profileFile.exists()) {
                    try {
                        if (!this.mPm.mInstaller.copySystemProfile(profileFile.getAbsolutePath(), pkg.getUid(), pkg.getPackageName(), ArtManager.getProfileName((String) null))) {
                            Log.e("PackageManager", "Installer failed to copy system profile!");
                        }
                    } catch (Exception e) {
                        Log.e("PackageManager", "Failed to copy profile " + profileFile.getAbsolutePath() + " ", e);
                    }
                    it = it2;
                } else {
                    PackageSetting disabledPs = this.mPm.mSettings.getDisabledSystemPkgLPr(pkg.getPackageName());
                    if (disabledPs == null || !disabledPs.getPkg().isStub()) {
                        it = it2;
                    } else {
                        String systemProfilePath = getPrebuildProfilePath(disabledPs.getPkg()).replace(PackageManagerService.STUB_SUFFIX, "");
                        File profileFile2 = new File(systemProfilePath);
                        if (!profileFile2.exists()) {
                            it = it2;
                        } else {
                            try {
                                it = it2;
                                try {
                                    if (!this.mPm.mInstaller.copySystemProfile(profileFile2.getAbsolutePath(), pkg.getUid(), pkg.getPackageName(), ArtManager.getProfileName((String) null))) {
                                        Log.e("PackageManager", "Failed to copy system profile for stub package!");
                                    } else {
                                        useProfileForDexopt = true;
                                    }
                                } catch (Exception e2) {
                                    e = e2;
                                    Log.e("PackageManager", "Failed to copy profile " + profileFile2.getAbsolutePath() + " ", e);
                                    if (this.mPm.mPackageDexOptimizer.canOptimizePackage(pkg)) {
                                    }
                                }
                            } catch (Exception e3) {
                                e = e3;
                                it = it2;
                            }
                        }
                    }
                }
            }
            if (this.mPm.mPackageDexOptimizer.canOptimizePackage(pkg)) {
                if (PackageManagerService.DEBUG_DEXOPT) {
                    Log.i("PackageManager", "Skipping update of non-optimizable app " + pkg.getPackageName());
                }
                numberOfPackagesSkipped++;
                pkgCompilationReason = numberOfPackagesVisited;
                it2 = it;
            } else {
                if (PackageManagerService.DEBUG_DEXOPT) {
                    Log.i("PackageManager", "Updating app " + numberOfPackagesVisited + " of " + numberOfPackagesToDexopt + ": " + pkg.getPackageName());
                }
                if (showDialog) {
                    try {
                        ActivityManager.getService().showBootMessage(this.mPm.mContext.getResources().getString(17039666, Integer.valueOf(numberOfPackagesVisited), Integer.valueOf(numberOfPackagesToDexopt)), true);
                    } catch (RemoteException e4) {
                    }
                    synchronized (this.mLock) {
                        this.mDexOptDialogShown = true;
                    }
                }
                int pkgCompilationReason2 = compilationReason;
                if (useProfileForDexopt) {
                    pkgCompilationReason2 = 9;
                }
                if (SystemProperties.getBoolean("pm.precompile_layouts", false)) {
                    this.mPm.mArtManagerService.compileLayouts(pkg);
                }
                int dexoptFlags = bootComplete ? 4 : 0;
                if (compilationReason == 0) {
                    dexoptFlags |= 1024;
                }
                int primaryDexOptStatus = performDexOptTraced(new DexoptOptions(pkg.getPackageName(), pkgCompilationReason2, dexoptFlags));
                switch (primaryDexOptStatus) {
                    case -1:
                        numberOfPackagesFailed++;
                        break;
                    case 0:
                        numberOfPackagesSkipped++;
                        break;
                    case 1:
                        numberOfPackagesOptimized++;
                        break;
                    case 2:
                        break;
                    default:
                        Log.e("PackageManager", "Unexpected dexopt return code " + primaryDexOptStatus);
                        break;
                }
                pkgCompilationReason = numberOfPackagesVisited;
                it2 = it;
            }
        }
        return new int[]{numberOfPackagesOptimized, numberOfPackagesSkipped, numberOfPackagesFailed};
    }

    private void checkAndDexOptSystemUi() {
        String compilerFilter;
        Computer snapshot = this.mPm.snapshotComputer();
        String sysUiPackageName = this.mPm.mContext.getString(17039418);
        AndroidPackage pkg = snapshot.getPackage(sysUiPackageName);
        if (pkg == null) {
            Log.w("PackageManager", "System UI package " + sysUiPackageName + " is not found for dexopting");
            return;
        }
        String defaultCompilerFilter = PackageManagerServiceCompilerMapping.getCompilerFilterForReason(1);
        String targetCompilerFilter = SystemProperties.get("dalvik.vm.systemuicompilerfilter", defaultCompilerFilter);
        if (DexFile.isProfileGuidedCompilerFilter(targetCompilerFilter)) {
            compilerFilter = defaultCompilerFilter;
            File profileFile = new File(getPrebuildProfilePath(pkg));
            if (profileFile.exists()) {
                try {
                    synchronized (this.mPm.mInstallLock) {
                        if (this.mPm.mInstaller.copySystemProfile(profileFile.getAbsolutePath(), pkg.getUid(), pkg.getPackageName(), ArtManager.getProfileName((String) null))) {
                            compilerFilter = targetCompilerFilter;
                        } else {
                            Log.e("PackageManager", "Failed to copy profile " + profileFile.getAbsolutePath());
                        }
                    }
                } catch (Exception e) {
                    Log.e("PackageManager", "Failed to copy profile " + profileFile.getAbsolutePath(), e);
                }
            }
        } else {
            compilerFilter = targetCompilerFilter;
        }
        performDexOptTraced(new DexoptOptions(pkg.getPackageName(), 1, compilerFilter, null, 0));
    }

    public void performPackageDexOptUpgradeIfNeeded() {
        PackageManagerServiceUtils.enforceSystemOrRoot("Only the system can request package update");
        if (!"false".equals(DeviceConfig.getProperty("runtime", "dexopt_system_ui_on_boot")) && (hasBcpApexesChanged() || this.mPm.isDeviceUpgrading())) {
            checkAndDexOptSystemUi();
        }
        boolean causeUpgrade = this.mPm.isDeviceUpgrading();
        boolean causeFirstBoot = this.mPm.isFirstBoot() || this.mPm.isPreNUpgrade();
        if (!causeUpgrade && !causeFirstBoot) {
            return;
        }
        Computer snapshot = this.mPm.snapshotComputer();
        List<PackageStateInternal> pkgSettings = getPackagesForDexopt(snapshot.getPackageStates().values(), this.mPm);
        List<AndroidPackage> pkgs = new ArrayList<>(pkgSettings.size());
        for (int index = 0; index < pkgSettings.size(); index++) {
            pkgs.add(pkgSettings.get(index).getPkg());
        }
        long startTime = System.nanoTime();
        int[] stats = performDexOptUpgrade(pkgs, this.mPm.isPreNUpgrade(), causeFirstBoot ? 0 : 1, false);
        int elapsedTimeSeconds = (int) TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);
        Computer newSnapshot = this.mPm.snapshotComputer();
        MetricsLogger.histogram(this.mPm.mContext, "opt_dialog_num_dexopted", stats[0]);
        MetricsLogger.histogram(this.mPm.mContext, "opt_dialog_num_skipped", stats[1]);
        MetricsLogger.histogram(this.mPm.mContext, "opt_dialog_num_failed", stats[2]);
        MetricsLogger.histogram(this.mPm.mContext, "opt_dialog_num_total", getOptimizablePackages(newSnapshot).size());
        MetricsLogger.histogram(this.mPm.mContext, "opt_dialog_time_s", elapsedTimeSeconds);
    }

    public List<String> getOptimizablePackages(Computer snapshot) {
        final ArrayList<String> pkgs = new ArrayList<>();
        this.mPm.forEachPackageState(snapshot, new Consumer() { // from class: com.android.server.pm.DexOptHelper$$ExternalSyntheticLambda8
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DexOptHelper.this.m5409x775307dc(pkgs, (PackageStateInternal) obj);
            }
        });
        return pkgs;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getOptimizablePackages$0$com-android-server-pm-DexOptHelper  reason: not valid java name */
    public /* synthetic */ void m5409x775307dc(ArrayList pkgs, PackageStateInternal packageState) {
        AndroidPackage pkg = packageState.getPkg();
        if (pkg != null && this.mPm.mPackageDexOptimizer.canOptimizePackage(pkg)) {
            pkgs.add(packageState.getPackageName());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean performDexOpt(DexoptOptions options) {
        Computer snapshot = this.mPm.snapshotComputer();
        if (snapshot.getInstantAppPackageName(Binder.getCallingUid()) == null && !snapshot.isInstantApp(options.getPackageName(), UserHandle.getCallingUserId())) {
            if (options.isDexoptOnlySecondaryDex()) {
                return this.mPm.getDexManager().dexoptSecondaryDex(options);
            }
            int dexoptStatus = performDexOptWithStatus(options);
            return dexoptStatus != -1;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int performDexOptWithStatus(DexoptOptions options) {
        return performDexOptTraced(options);
    }

    private int performDexOptTraced(DexoptOptions options) {
        if (THUBCORE_SUPPORT && ITranPackageManagerService.Instance().checkPckName(this.mPm.mContext, options.getPackageName())) {
            return 2;
        }
        PackageManagerService.sMtkSystemServerIns.addBootEvent("PMS:performDexOpt:" + options.getPackageName());
        Trace.traceBegin(262144L, "dexopt");
        try {
            return performDexOptInternal(options);
        } finally {
            Trace.traceEnd(262144L);
        }
    }

    private int performDexOptInternal(DexoptOptions options) {
        synchronized (this.mPm.mLock) {
            AndroidPackage p = this.mPm.mPackages.get(options.getPackageName());
            PackageSetting pkgSetting = this.mPm.mSettings.getPackageLPr(options.getPackageName());
            if (p != null && pkgSetting != null) {
                this.mPm.getPackageUsage().maybeWriteAsync(this.mPm.mSettings.getPackagesLocked());
                this.mPm.mCompilerStats.maybeWriteAsync();
                long callingId = Binder.clearCallingIdentity();
                try {
                    return performDexOptInternalWithDependenciesLI(p, pkgSetting, options);
                } finally {
                    Binder.restoreCallingIdentity(callingId);
                }
            }
            return -1;
        }
    }

    private int performDexOptInternalWithDependenciesLI(AndroidPackage p, PackageStateInternal pkgSetting, DexoptOptions options) {
        PackageDexOptimizer.ForcedUpdatePackageDexOptimizer pdo;
        AndroidPackage depPackage;
        PackageSetting depPackageSetting;
        if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(p.getPackageName())) {
            return this.mPm.getDexManager().dexoptSystemServer(options);
        }
        if (options.isForce()) {
            pdo = new PackageDexOptimizer.ForcedUpdatePackageDexOptimizer(this.mPm.mPackageDexOptimizer);
        } else {
            pdo = this.mPm.mPackageDexOptimizer;
        }
        Collection<SharedLibraryInfo> deps = SharedLibraryUtils.findSharedLibraries(pkgSetting);
        String[] instructionSets = InstructionSets.getAppDexInstructionSets(AndroidPackageUtils.getPrimaryCpuAbi(p, pkgSetting), AndroidPackageUtils.getSecondaryCpuAbi(p, pkgSetting));
        if (!deps.isEmpty()) {
            DexoptOptions libraryOptions = new DexoptOptions(options.getPackageName(), options.getCompilationReason(), options.getCompilerFilter(), options.getSplitName(), options.getFlags() | 64);
            for (SharedLibraryInfo info : deps) {
                synchronized (this.mPm.mLock) {
                    depPackage = this.mPm.mPackages.get(info.getPackageName());
                    depPackageSetting = this.mPm.mSettings.getPackageLPr(info.getPackageName());
                }
                if (depPackage != null && depPackageSetting != null) {
                    pdo.performDexOpt(depPackage, depPackageSetting, instructionSets, this.mPm.getOrCreateCompilerPackageStats(depPackage), this.mPm.getDexManager().getPackageUseInfoOrDefault(depPackage.getPackageName()), libraryOptions);
                }
            }
        }
        return pdo.performDexOpt(p, pkgSetting, instructionSets, this.mPm.getOrCreateCompilerPackageStats(p), this.mPm.getDexManager().getPackageUseInfoOrDefault(p.getPackageName()), options);
    }

    public void forceDexOpt(Computer snapshot, String packageName) {
        PackageManagerServiceUtils.enforceSystemOrRoot("forceDexOpt");
        PackageStateInternal packageState = snapshot.getPackageStateInternal(packageName);
        AndroidPackage pkg = packageState == null ? null : packageState.getPkg();
        if (packageState == null || pkg == null) {
            throw new IllegalArgumentException("Unknown package: " + packageName);
        }
        Trace.traceBegin(262144L, "dexopt");
        int res = performDexOptInternalWithDependenciesLI(pkg, packageState, new DexoptOptions(packageName, 12, PackageManagerServiceCompilerMapping.getDefaultCompilerFilter(), null, 6));
        Trace.traceEnd(262144L);
        if (res != 1) {
            throw new IllegalStateException("Failed to dexopt: " + res);
        }
    }

    public boolean performDexOptMode(Computer snapshot, String packageName, boolean checkProfiles, String targetCompilerFilter, boolean force, boolean bootComplete, String splitName) {
        if (!PackageManagerServiceUtils.isSystemOrRootOrShell() && !isCallerInstallerForPackage(snapshot, packageName)) {
            throw new SecurityException("performDexOptMode");
        }
        int flags = (bootComplete ? 4 : 0) | (force ? 2 : 0) | (checkProfiles ? 1 : 0);
        return performDexOpt(new DexoptOptions(packageName, 12, targetCompilerFilter, splitName, flags));
    }

    private boolean isCallerInstallerForPackage(Computer snapshot, String packageName) {
        PackageStateInternal packageState = snapshot.getPackageStateInternal(packageName);
        if (packageState == null) {
            return false;
        }
        InstallSource installSource = packageState.getInstallSource();
        PackageStateInternal installerPackageState = snapshot.getPackageStateInternal(installSource.installerPackageName);
        if (installerPackageState == null) {
            return false;
        }
        AndroidPackage installerPkg = installerPackageState.getPkg();
        return installerPkg.getUid() == Binder.getCallingUid();
    }

    public boolean performDexOptSecondary(String packageName, String compilerFilter, boolean force) {
        int flags = (force ? 2 : 0) | 13;
        return performDexOpt(new DexoptOptions(packageName, 12, compilerFilter, null, flags));
    }

    public static List<PackageStateInternal> getPackagesForDexopt(Collection<? extends PackageStateInternal> packages, PackageManagerService packageManagerService) {
        return getPackagesForDexopt(packages, packageManagerService, PackageManagerService.DEBUG_DEXOPT);
    }

    /* JADX WARN: Removed duplicated region for block: B:21:0x00d5  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static List<PackageStateInternal> getPackagesForDexopt(Collection<? extends PackageStateInternal> pkgSettings, PackageManagerService packageManagerService, boolean debug) {
        Predicate<PackageStateInternal> remainingPredicate;
        Predicate<PackageStateInternal> remainingPredicate2;
        List<PackageStateInternal> result = new LinkedList<>();
        ArrayList<PackageStateInternal> remainingPkgSettings = new ArrayList<>(pkgSettings);
        remainingPkgSettings.removeIf(PackageManagerServiceUtils.REMOVE_IF_NULL_PKG);
        ArrayList<PackageStateInternal> sortTemp = new ArrayList<>(remainingPkgSettings.size());
        Computer snapshot = packageManagerService.snapshotComputer();
        applyPackageFilter(snapshot, new Predicate() { // from class: com.android.server.pm.DexOptHelper$$ExternalSyntheticLambda1
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean isCoreApp;
                isCoreApp = ((PackageStateInternal) obj).getPkg().isCoreApp();
                return isCoreApp;
            }
        }, result, remainingPkgSettings, sortTemp, packageManagerService);
        Intent intent = new Intent("android.intent.action.PRE_BOOT_COMPLETED");
        final ArraySet<String> pkgNames = getPackageNamesForIntent(intent, 0);
        applyPackageFilter(snapshot, new Predicate() { // from class: com.android.server.pm.DexOptHelper$$ExternalSyntheticLambda2
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean contains;
                contains = pkgNames.contains(((PackageStateInternal) obj).getPackageName());
                return contains;
            }
        }, result, remainingPkgSettings, sortTemp, packageManagerService);
        final DexManager dexManager = packageManagerService.getDexManager();
        applyPackageFilter(snapshot, new Predicate() { // from class: com.android.server.pm.DexOptHelper$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean isAnyCodePathUsedByOtherApps;
                isAnyCodePathUsedByOtherApps = DexManager.this.getPackageUseInfoOrDefault(((PackageStateInternal) obj).getPackageName()).isAnyCodePathUsedByOtherApps();
                return isAnyCodePathUsedByOtherApps;
            }
        }, result, remainingPkgSettings, sortTemp, packageManagerService);
        if (!remainingPkgSettings.isEmpty() && packageManagerService.isHistoricalPackageUsageAvailable()) {
            if (debug) {
                Log.i("PackageManager", "Looking at historical package use");
            }
            PackageStateInternal lastUsed = (PackageStateInternal) Collections.max(remainingPkgSettings, Comparator.comparingLong(new ToLongFunction() { // from class: com.android.server.pm.DexOptHelper$$ExternalSyntheticLambda4
                @Override // java.util.function.ToLongFunction
                public final long applyAsLong(Object obj) {
                    long latestForegroundPackageUseTimeInMills;
                    latestForegroundPackageUseTimeInMills = ((PackageStateInternal) obj).getTransientState().getLatestForegroundPackageUseTimeInMills();
                    return latestForegroundPackageUseTimeInMills;
                }
            }));
            if (debug) {
                Log.i("PackageManager", "Taking package " + lastUsed.getPackageName() + " as reference in time use");
            }
            long estimatedPreviousSystemUseTime = lastUsed.getTransientState().getLatestForegroundPackageUseTimeInMills();
            if (estimatedPreviousSystemUseTime != 0) {
                final long cutoffTime = estimatedPreviousSystemUseTime - 604800000;
                remainingPredicate2 = new Predicate() { // from class: com.android.server.pm.DexOptHelper$$ExternalSyntheticLambda5
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return DexOptHelper.lambda$getPackagesForDexopt$5(cutoffTime, (PackageStateInternal) obj);
                    }
                };
            } else {
                remainingPredicate2 = new Predicate() { // from class: com.android.server.pm.DexOptHelper$$ExternalSyntheticLambda6
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return DexOptHelper.lambda$getPackagesForDexopt$6((PackageStateInternal) obj);
                    }
                };
            }
            sortPackagesByUsageDate(remainingPkgSettings, packageManagerService);
            remainingPredicate = remainingPredicate2;
            applyPackageFilter(snapshot, remainingPredicate, result, remainingPkgSettings, sortTemp, packageManagerService);
            if (debug) {
                Log.i("PackageManager", "Packages to be dexopted: " + packagesToString(result));
                Log.i("PackageManager", "Packages skipped from dexopt: " + packagesToString(remainingPkgSettings));
            }
            return result;
        }
        remainingPredicate = new Predicate() { // from class: com.android.server.pm.DexOptHelper$$ExternalSyntheticLambda7
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return DexOptHelper.lambda$getPackagesForDexopt$7((PackageStateInternal) obj);
            }
        };
        applyPackageFilter(snapshot, remainingPredicate, result, remainingPkgSettings, sortTemp, packageManagerService);
        if (debug) {
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getPackagesForDexopt$5(long cutoffTime, PackageStateInternal pkgSetting) {
        return pkgSetting.getTransientState().getLatestForegroundPackageUseTimeInMills() >= cutoffTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getPackagesForDexopt$6(PackageStateInternal pkgSetting) {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getPackagesForDexopt$7(PackageStateInternal pkgSetting) {
        return true;
    }

    private static void applyPackageFilter(Computer snapshot, Predicate<PackageStateInternal> filter, Collection<PackageStateInternal> result, Collection<PackageStateInternal> packages, List<PackageStateInternal> sortTemp, PackageManagerService packageManagerService) {
        for (PackageStateInternal pkgSetting : packages) {
            if (filter.test(pkgSetting)) {
                sortTemp.add(pkgSetting);
            }
        }
        sortPackagesByUsageDate(sortTemp, packageManagerService);
        packages.removeAll(sortTemp);
        for (PackageStateInternal pkgSetting2 : sortTemp) {
            result.add(pkgSetting2);
            List<PackageStateInternal> deps = snapshot.findSharedNonSystemLibraries(pkgSetting2);
            if (!deps.isEmpty()) {
                deps.removeAll(result);
                result.addAll(deps);
                packages.removeAll(deps);
            }
        }
        sortTemp.clear();
    }

    private static void sortPackagesByUsageDate(List<PackageStateInternal> pkgSettings, PackageManagerService packageManagerService) {
        if (!packageManagerService.isHistoricalPackageUsageAvailable()) {
            return;
        }
        Collections.sort(pkgSettings, new Comparator() { // from class: com.android.server.pm.DexOptHelper$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                int compare;
                compare = Long.compare(((PackageStateInternal) obj2).getTransientState().getLatestForegroundPackageUseTimeInMills(), ((PackageStateInternal) obj).getTransientState().getLatestForegroundPackageUseTimeInMills());
                return compare;
            }
        });
    }

    private static ArraySet<String> getPackageNamesForIntent(Intent intent, int userId) {
        List<ResolveInfo> ris = null;
        try {
            ris = AppGlobals.getPackageManager().queryIntentReceivers(intent, (String) null, 0L, userId).getList();
        } catch (RemoteException e) {
        }
        ArraySet<String> pkgNames = new ArraySet<>();
        if (ris != null) {
            for (ResolveInfo ri : ris) {
                pkgNames.add(ri.activityInfo.packageName);
            }
        }
        return pkgNames;
    }

    public static String packagesToString(List<PackageStateInternal> pkgSettings) {
        StringBuilder sb = new StringBuilder();
        for (int index = 0; index < pkgSettings.size(); index++) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(pkgSettings.get(index).getPackageName());
        }
        return sb.toString();
    }

    public static void requestCopyPreoptedFiles() {
        if (SystemProperties.getInt("ro.cp_system_other_odex", 0) == 1) {
            SystemProperties.set("sys.cppreopt", "requested");
            long timeStart = SystemClock.uptimeMillis();
            long timeEnd = 100000 + timeStart;
            long timeNow = timeStart;
            while (true) {
                if (!SystemProperties.get("sys.cppreopt").equals("finished")) {
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException e) {
                    }
                    timeNow = SystemClock.uptimeMillis();
                    if (timeNow > timeEnd) {
                        SystemProperties.set("sys.cppreopt", "timed-out");
                        Slog.wtf("PackageManager", "cppreopt did not finish!");
                        break;
                    }
                } else {
                    break;
                }
            }
            Slog.i("PackageManager", "cppreopts took " + (timeNow - timeStart) + " ms");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void controlDexOptBlocking(boolean block) {
        this.mPm.mPackageDexOptimizer.controlDexOptBlocking(block);
    }

    private static List<String> getBcpApexes() {
        String[] split;
        String bcp = System.getenv("BOOTCLASSPATH");
        if (TextUtils.isEmpty(bcp)) {
            Log.e("PackageManager", "Unable to get BOOTCLASSPATH");
            return List.of();
        }
        ArrayList<String> bcpApexes = new ArrayList<>();
        for (String pathStr : bcp.split(":")) {
            Path path = Paths.get(pathStr, new String[0]);
            if (path.getNameCount() >= 2 && path.getName(0).toString().equals("apex")) {
                bcpApexes.add(path.getName(1).toString());
            }
        }
        return bcpApexes;
    }

    private static boolean hasBcpApexesChanged() {
        Set<String> bcpApexes = new HashSet<>(getBcpApexes());
        ApexManager apexManager = ApexManager.getInstance();
        for (ApexManager.ActiveApexInfo apexInfo : apexManager.getActiveApexInfos()) {
            if (bcpApexes.contains(apexInfo.apexModuleName) && apexInfo.activeApexChanged) {
                return true;
            }
        }
        return false;
    }
}
