package com.android.server.pm.dex;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackagePartitions;
import android.os.BatteryManager;
import android.os.FileUtils;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import android.util.jar.StrictJarFile;
import com.android.server.pm.Installer;
import com.android.server.pm.InstructionSets;
import com.android.server.pm.PackageDexOptimizer;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.PackageManagerServiceUtils;
import com.android.server.pm.dex.PackageDexUsage;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.zip.ZipEntry;
/* loaded from: classes2.dex */
public class DexManager {
    private static final String ISOLATED_PROCESS_PACKAGE_SUFFIX = "..isolated";
    private static final String PROPERTY_NAME_PM_DEXOPT_PRIV_APPS_OOB = "pm.dexopt.priv-apps-oob";
    private static final String PROPERTY_NAME_PM_DEXOPT_PRIV_APPS_OOB_LIST = "pm.dexopt.priv-apps-oob-list";
    private static final String SYSTEM_SERVER_COMPILER_FILTER = "verify";
    private BatteryManager mBatteryManager;
    private final Context mContext;
    private final int mCriticalBatteryLevel;
    private final DynamicCodeLogger mDynamicCodeLogger;
    private final Object mInstallLock;
    private final Installer mInstaller;
    private final Map<String, PackageCodeLocations> mPackageCodeLocationsCache;
    private final PackageDexOptimizer mPackageDexOptimizer;
    private final PackageDexUsage mPackageDexUsage;
    private IPackageManager mPackageManager;
    private PowerManager mPowerManager;
    private static final String TAG = "DexManager";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static int DEX_SEARCH_NOT_FOUND = 0;
    private static int DEX_SEARCH_FOUND_PRIMARY = 1;
    private static int DEX_SEARCH_FOUND_SPLIT = 2;
    private static int DEX_SEARCH_FOUND_SECONDARY = 3;

    public DexManager(Context context, PackageDexOptimizer pdo, Installer installer, Object installLock) {
        this(context, pdo, installer, installLock, null);
    }

    public DexManager(Context context, PackageDexOptimizer pdo, Installer installer, Object installLock, IPackageManager packageManager) {
        this.mBatteryManager = null;
        this.mPowerManager = null;
        this.mContext = context;
        this.mPackageCodeLocationsCache = new HashMap();
        this.mPackageDexUsage = new PackageDexUsage();
        this.mPackageDexOptimizer = pdo;
        this.mInstaller = installer;
        this.mInstallLock = installLock;
        this.mDynamicCodeLogger = new DynamicCodeLogger(installer);
        this.mPackageManager = packageManager;
        if (context != null) {
            PowerManager powerManager = (PowerManager) context.getSystemService(PowerManager.class);
            this.mPowerManager = powerManager;
            if (powerManager == null) {
                Slog.wtf(TAG, "Power Manager is unavailable at time of Dex Manager start");
            }
            this.mCriticalBatteryLevel = context.getResources().getInteger(17694772);
            return;
        }
        this.mCriticalBatteryLevel = 0;
    }

    private IPackageManager getPackageManager() {
        if (this.mPackageManager == null) {
            this.mPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        }
        return this.mPackageManager;
    }

    public DynamicCodeLogger getDynamicCodeLogger() {
        return this.mDynamicCodeLogger;
    }

    public void notifyDexLoad(ApplicationInfo loadingAppInfo, Map<String, String> classLoaderContextMap, String loaderIsa, int loaderUserId, boolean loaderIsIsolatedProcess) {
        try {
            notifyDexLoadInternal(loadingAppInfo, classLoaderContextMap, loaderIsa, loaderUserId, loaderIsIsolatedProcess);
        } catch (Exception e) {
            Slog.w(TAG, "Exception while notifying dex load for package " + loadingAppInfo.packageName, e);
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:38:0x00f0, code lost:
        r22.mDynamicCodeLogger.recordDex(r26, r10, r9.mOwningPackageName, r1.packageName);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    void notifyDexLoadInternal(ApplicationInfo loadingAppInfo, Map<String, String> classLoaderContextMap, String loaderIsa, int loaderUserId, boolean loaderIsIsolatedProcess) {
        String loadingPackageAmendedName;
        ApplicationInfo applicationInfo = loadingAppInfo;
        if (classLoaderContextMap == null) {
            return;
        }
        if (classLoaderContextMap.isEmpty()) {
            Slog.wtf(TAG, "Bad call to notifyDexLoad: class loaders list is empty");
        } else if (!PackageManagerServiceUtils.checkISA(loaderIsa)) {
            Slog.w(TAG, "Loading dex files " + classLoaderContextMap.keySet() + " in unsupported ISA: " + loaderIsa + "?");
        } else {
            String loadingPackageAmendedName2 = applicationInfo.packageName;
            if (!loaderIsIsolatedProcess) {
                loadingPackageAmendedName = loadingPackageAmendedName2;
            } else {
                loadingPackageAmendedName = loadingPackageAmendedName2 + ISOLATED_PROCESS_PACKAGE_SUFFIX;
            }
            for (Map.Entry<String, String> mapping : classLoaderContextMap.entrySet()) {
                String dexPath = mapping.getKey();
                DexSearchResult searchResult = getDexPackage(applicationInfo, dexPath, loaderUserId);
                boolean z = DEBUG;
                if (z) {
                    Slog.i(TAG, loadingPackageAmendedName + " loads from " + searchResult + " : " + loaderUserId + " : " + dexPath);
                }
                if (searchResult.mOutcome != DEX_SEARCH_NOT_FOUND) {
                    boolean z2 = true;
                    boolean isUsedByOtherApps = !loadingPackageAmendedName.equals(searchResult.mOwningPackageName);
                    if (searchResult.mOutcome != DEX_SEARCH_FOUND_PRIMARY && searchResult.mOutcome != DEX_SEARCH_FOUND_SPLIT) {
                        z2 = false;
                    }
                    boolean primaryOrSplit = z2;
                    if (!primaryOrSplit || isUsedByOtherApps || isPlatformPackage(searchResult.mOwningPackageName)) {
                        String classLoaderContext = mapping.getValue();
                        boolean overwriteCLC = isPlatformPackage(searchResult.mOwningPackageName);
                        if (classLoaderContext != null && VMRuntime.isValidClassLoaderContext(classLoaderContext) && this.mPackageDexUsage.record(searchResult.mOwningPackageName, dexPath, loaderUserId, loaderIsa, primaryOrSplit, loadingPackageAmendedName, classLoaderContext, overwriteCLC)) {
                            this.mPackageDexUsage.maybeWriteAsync();
                        }
                    }
                } else if (z) {
                    Slog.i(TAG, "Could not find owning package for dex file: " + dexPath);
                }
                applicationInfo = loadingAppInfo;
            }
        }
    }

    private boolean isSystemServerDexPathSupportedForOdex(String dexPath) {
        ArrayList<PackagePartitions.SystemPartition> partitions = PackagePartitions.getOrderedPartitions(Function.identity());
        if (dexPath.startsWith("/apex/")) {
            return true;
        }
        for (int i = 0; i < partitions.size(); i++) {
            if (partitions.get(i).containsPath(dexPath)) {
                return true;
            }
        }
        return false;
    }

    public void load(Map<Integer, List<PackageInfo>> existingPackages) {
        try {
            loadInternal(existingPackages);
        } catch (Exception e) {
            this.mPackageDexUsage.clear();
            this.mDynamicCodeLogger.clear();
            Slog.w(TAG, "Exception while loading. Starting with a fresh state.", e);
        }
    }

    public void notifyPackageInstalled(PackageInfo pi, int userId) {
        if (userId == -1) {
            throw new IllegalArgumentException("notifyPackageInstalled called with USER_ALL");
        }
        cachePackageInfo(pi, userId);
    }

    public void notifyPackageUpdated(String packageName, String baseCodePath, String[] splitCodePaths) {
        cachePackageCodeLocation(packageName, baseCodePath, splitCodePaths, null, -1);
        if (this.mPackageDexUsage.clearUsedByOtherApps(packageName)) {
            this.mPackageDexUsage.maybeWriteAsync();
        }
    }

    public void notifyPackageDataDestroyed(String packageName, int userId) {
        if (userId == -1) {
            if (this.mPackageDexUsage.removePackage(packageName)) {
                this.mPackageDexUsage.maybeWriteAsync();
            }
            this.mDynamicCodeLogger.removePackage(packageName);
            return;
        }
        if (this.mPackageDexUsage.removeUserPackage(packageName, userId)) {
            this.mPackageDexUsage.maybeWriteAsync();
        }
        this.mDynamicCodeLogger.removeUserPackage(packageName, userId);
    }

    private void cachePackageInfo(PackageInfo pi, int userId) {
        ApplicationInfo ai = pi.applicationInfo;
        String[] dataDirs = {ai.dataDir, ai.deviceProtectedDataDir, ai.credentialProtectedDataDir};
        cachePackageCodeLocation(pi.packageName, ai.sourceDir, ai.splitSourceDirs, dataDirs, userId);
    }

    private void cachePackageCodeLocation(String packageName, String baseCodePath, String[] splitCodePaths, String[] dataDirs, int userId) {
        synchronized (this.mPackageCodeLocationsCache) {
            PackageCodeLocations pcl = (PackageCodeLocations) putIfAbsent(this.mPackageCodeLocationsCache, packageName, new PackageCodeLocations(packageName, baseCodePath, splitCodePaths));
            pcl.updateCodeLocation(baseCodePath, splitCodePaths);
            if (dataDirs != null) {
                for (String dataDir : dataDirs) {
                    if (dataDir != null) {
                        pcl.mergeAppDataDirs(dataDir, userId);
                    }
                }
            }
        }
    }

    private void loadInternal(Map<Integer, List<PackageInfo>> existingPackages) {
        Map<String, Set<Integer>> packageToUsersMap = new HashMap<>();
        Map<String, Set<String>> packageToCodePaths = new HashMap<>();
        for (Map.Entry<Integer, List<PackageInfo>> entry : existingPackages.entrySet()) {
            List<PackageInfo> packageInfoList = entry.getValue();
            int userId = entry.getKey().intValue();
            for (PackageInfo pi : packageInfoList) {
                cachePackageInfo(pi, userId);
                Set<Integer> users = (Set) putIfAbsent(packageToUsersMap, pi.packageName, new HashSet());
                users.add(Integer.valueOf(userId));
                Set<String> codePaths = (Set) putIfAbsent(packageToCodePaths, pi.packageName, new HashSet());
                codePaths.add(pi.applicationInfo.sourceDir);
                if (pi.applicationInfo.splitSourceDirs != null) {
                    Collections.addAll(codePaths, pi.applicationInfo.splitSourceDirs);
                }
            }
        }
        try {
            this.mPackageDexUsage.read();
            List<String> packagesToKeepDataAbout = new ArrayList<>();
            this.mPackageDexUsage.syncData(packageToUsersMap, packageToCodePaths, packagesToKeepDataAbout);
        } catch (Exception e) {
            this.mPackageDexUsage.clear();
            Slog.w(TAG, "Exception while loading package dex usage. Starting with a fresh state.", e);
        }
        try {
            this.mDynamicCodeLogger.readAndSync(packageToUsersMap);
        } catch (Exception e2) {
            this.mDynamicCodeLogger.clear();
            Slog.w(TAG, "Exception while loading package dynamic code usage. Starting with a fresh state.", e2);
        }
    }

    public PackageDexUsage.PackageUseInfo getPackageUseInfoOrDefault(String packageName) {
        PackageDexUsage.PackageUseInfo useInfo = this.mPackageDexUsage.getPackageUseInfo(packageName);
        return useInfo == null ? new PackageDexUsage.PackageUseInfo(packageName) : useInfo;
    }

    boolean hasInfoOnPackage(String packageName) {
        return this.mPackageDexUsage.getPackageUseInfo(packageName) != null;
    }

    /* JADX WARN: Incorrect condition in loop: B:14:0x0059 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean dexoptSecondaryDex(DexoptOptions options) {
        if (isPlatformPackage(options.getPackageName())) {
            Slog.wtf(TAG, "System server jars should be optimized with dexoptSystemServer");
            return false;
        }
        PackageDexOptimizer pdo = getPackageDexOptimizer(options);
        String packageName = options.getPackageName();
        PackageDexUsage.PackageUseInfo useInfo = getPackageUseInfoOrDefault(packageName);
        if (useInfo.getDexUseInfoMap().isEmpty()) {
            if (DEBUG) {
                Slog.d(TAG, "No secondary dex use for package:" + packageName);
            }
            return true;
        }
        Iterator<Map.Entry<String, PackageDexUsage.DexUseInfo>> it = useInfo.getDexUseInfoMap().entrySet().iterator();
        boolean success = true;
        while (success) {
            Map.Entry<String, PackageDexUsage.DexUseInfo> entry = it.next();
            String dexPath = entry.getKey();
            PackageDexUsage.DexUseInfo dexUseInfo = entry.getValue();
            try {
                PackageInfo pkg = getPackageManager().getPackageInfo(packageName, 0L, dexUseInfo.getOwnerUserId());
                if (pkg == null) {
                    Slog.d(TAG, "Could not find package when compiling secondary dex " + packageName + " for user " + dexUseInfo.getOwnerUserId());
                    this.mPackageDexUsage.removeUserPackage(packageName, dexUseInfo.getOwnerUserId());
                } else {
                    int result = pdo.dexOptSecondaryDexPath(pkg.applicationInfo, dexPath, dexUseInfo, options);
                    success = success && result != -1;
                }
            } catch (RemoteException e) {
                throw new AssertionError(e);
            }
        }
        return success;
    }

    public int dexoptSystemServer(DexoptOptions options) {
        String debugMsg;
        if (!isPlatformPackage(options.getPackageName())) {
            Slog.wtf(TAG, "Non system server package used when trying to dexopt system server:" + options.getPackageName());
            return -1;
        }
        DexoptOptions overriddenOptions = options.overrideCompilerFilter(SYSTEM_SERVER_COMPILER_FILTER);
        PackageDexOptimizer pdo = getPackageDexOptimizer(overriddenOptions);
        String packageName = overriddenOptions.getPackageName();
        PackageDexUsage.PackageUseInfo useInfo = getPackageUseInfoOrDefault(packageName);
        int i = 0;
        if (useInfo.getDexUseInfoMap().isEmpty()) {
            if (DEBUG) {
                Slog.d(TAG, "No dex files recorded for system server");
            }
            return 0;
        }
        int i2 = 0;
        int result = 0;
        for (Map.Entry<String, PackageDexUsage.DexUseInfo> entry : useInfo.getDexUseInfoMap().entrySet()) {
            String dexPath = entry.getKey();
            PackageDexUsage.DexUseInfo dexUseInfo = entry.getValue();
            if (!Files.exists(Paths.get(dexPath, new String[i]), new LinkOption[i])) {
                if (DEBUG) {
                    Slog.w(TAG, "A dex file previously loaded by System Server does not exist  anymore: " + dexPath);
                }
                i2 = (this.mPackageDexUsage.removeDexFile(packageName, dexPath, dexUseInfo.getOwnerUserId()) || i2 != 0) ? 1 : i;
            } else {
                if (!dexUseInfo.isUnsupportedClassLoaderContext() && !dexUseInfo.isVariableClassLoaderContext()) {
                    int newResult = pdo.dexoptSystemServerPath(dexPath, dexUseInfo, overriddenOptions);
                    if (result != -1 && newResult != 0) {
                        result = newResult;
                    }
                }
                if (dexUseInfo.isUnsupportedClassLoaderContext()) {
                    debugMsg = "unsupported";
                } else {
                    debugMsg = "variable";
                }
                Slog.w(TAG, "Skipping dexopt for system server path loaded with " + debugMsg + " class loader context: " + dexPath);
                i = 0;
            }
        }
        if (i2 != 0) {
            this.mPackageDexUsage.maybeWriteAsync();
        }
        return result;
    }

    private PackageDexOptimizer getPackageDexOptimizer(DexoptOptions options) {
        if (options.isForce()) {
            return new PackageDexOptimizer.ForcedUpdatePackageDexOptimizer(this.mPackageDexOptimizer);
        }
        return this.mPackageDexOptimizer;
    }

    /* JADX WARN: Incorrect condition in loop: B:10:0x0041 */
    /* JADX WARN: Removed duplicated region for block: B:102:0x003d A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:98:0x0181 A[SYNTHETIC] */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:85:? -> B:69:0x0197). Please submit an issue!!! */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void reconcileSecondaryDexFiles(String packageName) {
        PackageInfo pkg;
        boolean z;
        int flags;
        Object obj;
        boolean z2;
        PackageDexUsage.PackageUseInfo useInfo = getPackageUseInfoOrDefault(packageName);
        if (useInfo.getDexUseInfoMap().isEmpty()) {
            if (DEBUG) {
                Slog.d(TAG, "No secondary dex use for package:" + packageName);
                return;
            }
            return;
        }
        Iterator<Map.Entry<String, PackageDexUsage.DexUseInfo>> it = useInfo.getDexUseInfoMap().entrySet().iterator();
        boolean updated = false;
        while (updated) {
            Map.Entry<String, PackageDexUsage.DexUseInfo> entry = it.next();
            String dexPath = entry.getKey();
            PackageDexUsage.DexUseInfo dexUseInfo = entry.getValue();
            try {
                PackageInfo pkg2 = getPackageManager().getPackageInfo(packageName, 0L, dexUseInfo.getOwnerUserId());
                pkg = pkg2;
            } catch (RemoteException e) {
                pkg = null;
            }
            boolean z3 = true;
            if (pkg == null) {
                Slog.d(TAG, "Could not find package when compiling secondary dex " + packageName + " for user " + dexUseInfo.getOwnerUserId());
                if (!this.mPackageDexUsage.removeUserPackage(packageName, dexUseInfo.getOwnerUserId()) && !updated) {
                    z3 = false;
                }
                updated = z3;
            } else if (isPlatformPackage(packageName)) {
                if (!Files.exists(Paths.get(dexPath, new String[0]), new LinkOption[0])) {
                    if (DEBUG) {
                        Slog.w(TAG, "A dex file previously loaded by System Server does not exist  anymore: " + dexPath);
                    }
                    if (!this.mPackageDexUsage.removeUserPackage(packageName, dexUseInfo.getOwnerUserId()) && !updated) {
                        z3 = false;
                    }
                    updated = z3;
                }
            } else {
                ApplicationInfo info = pkg.applicationInfo;
                if (info.deviceProtectedDataDir != null && FileUtils.contains(info.deviceProtectedDataDir, dexPath)) {
                    int flags2 = 0 | 1;
                    flags = flags2;
                } else {
                    if (info.credentialProtectedDataDir == null) {
                        z = false;
                    } else if (FileUtils.contains(info.credentialProtectedDataDir, dexPath)) {
                        int flags3 = 0 | 2;
                        flags = flags3;
                    } else {
                        z = false;
                    }
                    Slog.e(TAG, "Could not infer CE/DE storage for path " + dexPath);
                    if (!this.mPackageDexUsage.removeDexFile(packageName, dexPath, dexUseInfo.getOwnerUserId()) && !updated) {
                        z3 = z;
                    }
                    updated = z3;
                }
                boolean dexStillExists = true;
                Object obj2 = this.mInstallLock;
                synchronized (obj2) {
                    try {
                        String[] isas = (String[]) dexUseInfo.getLoaderIsas().toArray(new String[0]);
                        obj = obj2;
                        z2 = false;
                        try {
                            try {
                                boolean dexStillExists2 = this.mInstaller.reconcileSecondaryDexFile(dexPath, packageName, info.uid, isas, info.volumeUuid, flags);
                                dexStillExists = dexStillExists2;
                            } catch (Installer.InstallerException e2) {
                                e = e2;
                                Slog.e(TAG, "Got InstallerException when reconciling dex " + dexPath + " : " + e.getMessage());
                                if (!dexStillExists) {
                                }
                            }
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    } catch (Installer.InstallerException e3) {
                        e = e3;
                        obj = obj2;
                        z2 = false;
                    } catch (Throwable th2) {
                        th = th2;
                        obj = obj2;
                        throw th;
                    }
                    if (!dexStillExists) {
                        if (!this.mPackageDexUsage.removeDexFile(packageName, dexPath, dexUseInfo.getOwnerUserId()) && !updated) {
                            z3 = z2;
                        }
                        updated = z3;
                    }
                }
            }
        }
        if (updated) {
            this.mPackageDexUsage.maybeWriteAsync();
        }
    }

    public RegisterDexModuleResult registerDexModule(ApplicationInfo info, String dexPath, boolean isSharedModule, int userId) {
        DexSearchResult searchResult = getDexPackage(info, dexPath, userId);
        if (searchResult.mOutcome == DEX_SEARCH_NOT_FOUND) {
            return new RegisterDexModuleResult(false, "Package not found");
        }
        if (!info.packageName.equals(searchResult.mOwningPackageName)) {
            return new RegisterDexModuleResult(false, "Dex path does not belong to package");
        }
        if (searchResult.mOutcome == DEX_SEARCH_FOUND_PRIMARY || searchResult.mOutcome == DEX_SEARCH_FOUND_SPLIT) {
            return new RegisterDexModuleResult(false, "Main apks cannot be registered");
        }
        String loadingPackage = isSharedModule ? ".shared.module" : searchResult.mOwningPackageName;
        String[] appDexInstructionSets = InstructionSets.getAppDexInstructionSets(info.primaryCpuAbi, info.secondaryCpuAbi);
        boolean update = false;
        int i = 0;
        for (int length = appDexInstructionSets.length; i < length; length = length) {
            String isa = appDexInstructionSets[i];
            boolean newUpdate = this.mPackageDexUsage.record(searchResult.mOwningPackageName, dexPath, userId, isa, false, loadingPackage, "=VariableClassLoaderContext=", false);
            update |= newUpdate;
            i++;
        }
        if (update) {
            this.mPackageDexUsage.maybeWriteAsync();
        }
        PackageDexUsage.DexUseInfo dexUseInfo = this.mPackageDexUsage.getPackageUseInfo(searchResult.mOwningPackageName).getDexUseInfoMap().get(dexPath);
        DexoptOptions options = new DexoptOptions(info.packageName, 3, 0);
        int result = this.mPackageDexOptimizer.dexOptSecondaryDexPath(info, dexPath, dexUseInfo, options);
        if (result != -1) {
            Slog.e(TAG, "Failed to optimize dex module " + dexPath);
        }
        return new RegisterDexModuleResult(true, "Dex module registered successfully");
    }

    public Set<String> getAllPackagesWithSecondaryDexFiles() {
        return this.mPackageDexUsage.getAllPackagesWithSecondaryDexFiles();
    }

    private DexSearchResult getDexPackage(ApplicationInfo loadingAppInfo, String dexPath, int userId) {
        PackageCodeLocations loadingPackageCodeLocations = new PackageCodeLocations(loadingAppInfo, userId);
        int outcome = loadingPackageCodeLocations.searchDex(dexPath, userId);
        if (outcome != DEX_SEARCH_NOT_FOUND) {
            return new DexSearchResult(loadingPackageCodeLocations.mPackageName, outcome);
        }
        synchronized (this.mPackageCodeLocationsCache) {
            for (PackageCodeLocations pcl : this.mPackageCodeLocationsCache.values()) {
                int outcome2 = pcl.searchDex(dexPath, userId);
                if (outcome2 != DEX_SEARCH_NOT_FOUND) {
                    return new DexSearchResult(pcl.mPackageName, outcome2);
                }
            }
            if (isPlatformPackage(loadingAppInfo.packageName)) {
                if (isSystemServerDexPathSupportedForOdex(dexPath)) {
                    return new DexSearchResult(PackageManagerService.PLATFORM_PACKAGE_NAME, DEX_SEARCH_FOUND_SECONDARY);
                }
                Slog.wtf(TAG, "System server loads dex files outside paths supported for odex: " + dexPath);
            }
            if (DEBUG) {
                try {
                    String dexPathReal = PackageManagerServiceUtils.realpath(new File(dexPath));
                    if (!dexPath.equals(dexPathReal)) {
                        Slog.d(TAG, "Dex loaded with symlink. dexPath=" + dexPath + " dexPathReal=" + dexPathReal);
                    }
                } catch (IOException e) {
                }
            }
            return new DexSearchResult(null, DEX_SEARCH_NOT_FOUND);
        }
    }

    private static boolean isPlatformPackage(String packageName) {
        return PackageManagerService.PLATFORM_PACKAGE_NAME.equals(packageName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static <K, V> V putIfAbsent(Map<K, V> map, K key, V newValue) {
        V existingValue = map.putIfAbsent(key, newValue);
        return existingValue == null ? newValue : existingValue;
    }

    public void writePackageDexUsageNow() {
        this.mPackageDexUsage.writeNow();
        this.mDynamicCodeLogger.writeNow();
    }

    public static boolean isPackageSelectedToRunOob(String packageName) {
        return isPackageSelectedToRunOob(Arrays.asList(packageName));
    }

    public static boolean isPackageSelectedToRunOob(Collection<String> packageNamesInSameProcess) {
        return isPackageSelectedToRunOobInternal(SystemProperties.getBoolean(PROPERTY_NAME_PM_DEXOPT_PRIV_APPS_OOB, false), SystemProperties.get(PROPERTY_NAME_PM_DEXOPT_PRIV_APPS_OOB_LIST, "ALL"), packageNamesInSameProcess);
    }

    static boolean isPackageSelectedToRunOobInternal(boolean isEnabled, String whitelist, Collection<String> packageNamesInSameProcess) {
        String[] split;
        if (!isEnabled) {
            return false;
        }
        if ("ALL".equals(whitelist)) {
            return true;
        }
        for (String oobPkgName : whitelist.split(",")) {
            if (packageNamesInSameProcess.contains(oobPkgName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean auditUncompressedDexInApk(String fileName) {
        StrictJarFile jarFile = null;
        try {
            try {
                jarFile = new StrictJarFile(fileName, false, false);
                Iterator<ZipEntry> it = jarFile.iterator();
                boolean allCorrect = true;
                while (it.hasNext()) {
                    ZipEntry entry = it.next();
                    if (entry.getName().endsWith(".dex")) {
                        if (entry.getMethod() != 0) {
                            allCorrect = false;
                            Slog.w(TAG, "APK " + fileName + " has compressed dex code " + entry.getName());
                        } else if ((entry.getDataOffset() & 3) != 0) {
                            allCorrect = false;
                            Slog.w(TAG, "APK " + fileName + " has unaligned dex code " + entry.getName());
                        }
                    }
                }
                try {
                    jarFile.close();
                } catch (IOException e) {
                }
                return allCorrect;
            } catch (IOException e2) {
                Slog.wtf(TAG, "Error when parsing APK " + fileName);
                if (jarFile != null) {
                    try {
                        jarFile.close();
                    } catch (IOException e3) {
                    }
                }
                return false;
            }
        } catch (Throwable th) {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e4) {
                }
            }
            throw th;
        }
    }

    public int getCompilationReasonForInstallScenario(int installScenario) {
        boolean resourcesAreCritical = areBatteryThermalOrMemoryCritical();
        switch (installScenario) {
            case 0:
                return 3;
            case 1:
                return 4;
            case 2:
                if (resourcesAreCritical) {
                    return 7;
                }
                return 5;
            case 3:
                if (resourcesAreCritical) {
                    return 8;
                }
                return 6;
            default:
                throw new IllegalArgumentException("Invalid installation scenario");
        }
    }

    private BatteryManager getBatteryManager() {
        Context context;
        if (this.mBatteryManager == null && (context = this.mContext) != null) {
            this.mBatteryManager = (BatteryManager) context.getSystemService(BatteryManager.class);
        }
        return this.mBatteryManager;
    }

    private boolean areBatteryThermalOrMemoryCritical() {
        PowerManager powerManager;
        BatteryManager batteryManager = getBatteryManager();
        return (batteryManager != null && batteryManager.getIntProperty(6) == 3 && batteryManager.getIntProperty(4) <= this.mCriticalBatteryLevel) || ((powerManager = this.mPowerManager) != null && powerManager.getCurrentThermalStatus() >= 3);
    }

    public long deleteOptimizedFiles(ArtPackageInfo packageInfo) {
        long freedBytes = 0;
        boolean hadErrors = false;
        String packageName = packageInfo.getPackageName();
        for (String codePath : packageInfo.getCodePaths()) {
            for (String isa : packageInfo.getInstructionSets()) {
                try {
                    freedBytes += this.mInstaller.deleteOdex(packageName, codePath, isa, packageInfo.getOatDir());
                } catch (Installer.InstallerException e) {
                    Log.e(TAG, "Failed deleting oat files for " + codePath, e);
                    hadErrors = true;
                }
            }
        }
        if (hadErrors) {
            return -1L;
        }
        return freedBytes;
    }

    /* loaded from: classes2.dex */
    public static class RegisterDexModuleResult {
        public final String message;
        public final boolean success;

        public RegisterDexModuleResult() {
            this(false, null);
        }

        public RegisterDexModuleResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class PackageCodeLocations {
        private final Map<Integer, Set<String>> mAppDataDirs;
        private String mBaseCodePath;
        private final String mPackageName;
        private final Set<String> mSplitCodePaths;

        public PackageCodeLocations(ApplicationInfo ai, int userId) {
            this(ai.packageName, ai.sourceDir, ai.splitSourceDirs);
            mergeAppDataDirs(ai.dataDir, userId);
        }

        public PackageCodeLocations(String packageName, String baseCodePath, String[] splitCodePaths) {
            this.mPackageName = packageName;
            this.mSplitCodePaths = new HashSet();
            this.mAppDataDirs = new HashMap();
            updateCodeLocation(baseCodePath, splitCodePaths);
        }

        public void updateCodeLocation(String baseCodePath, String[] splitCodePaths) {
            this.mBaseCodePath = baseCodePath;
            this.mSplitCodePaths.clear();
            if (splitCodePaths != null) {
                for (String split : splitCodePaths) {
                    this.mSplitCodePaths.add(split);
                }
            }
        }

        public void mergeAppDataDirs(String dataDir, int userId) {
            Set<String> dataDirs = (Set) DexManager.putIfAbsent(this.mAppDataDirs, Integer.valueOf(userId), new HashSet());
            dataDirs.add(dataDir);
        }

        public int searchDex(String dexPath, int userId) {
            Set<String> userDataDirs = this.mAppDataDirs.get(Integer.valueOf(userId));
            if (userDataDirs == null) {
                return DexManager.DEX_SEARCH_NOT_FOUND;
            }
            if (this.mBaseCodePath.equals(dexPath)) {
                return DexManager.DEX_SEARCH_FOUND_PRIMARY;
            }
            if (this.mSplitCodePaths.contains(dexPath)) {
                return DexManager.DEX_SEARCH_FOUND_SPLIT;
            }
            for (String dataDir : userDataDirs) {
                if (dexPath.startsWith(dataDir)) {
                    return DexManager.DEX_SEARCH_FOUND_SECONDARY;
                }
            }
            return DexManager.DEX_SEARCH_NOT_FOUND;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class DexSearchResult {
        private int mOutcome;
        private String mOwningPackageName;

        public DexSearchResult(String owningPackageName, int outcome) {
            this.mOwningPackageName = owningPackageName;
            this.mOutcome = outcome;
        }

        public String toString() {
            return this.mOwningPackageName + "-" + this.mOutcome;
        }
    }
}
