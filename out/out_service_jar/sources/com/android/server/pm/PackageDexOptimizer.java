package com.android.server.pm;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.SharedLibraryInfo;
import android.content.pm.dex.ArtManager;
import android.content.pm.dex.DexMetadataHelper;
import android.os.FileUtils;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.WorkSource;
import android.os.storage.StorageManager;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.content.F2fsUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.LocalServices;
import com.android.server.apphibernation.AppHibernationManagerInternal;
import com.android.server.pm.CompilerStats;
import com.android.server.pm.Installer;
import com.android.server.pm.dex.ArtManagerService;
import com.android.server.pm.dex.ArtStatsLogUtils;
import com.android.server.pm.dex.DexManager;
import com.android.server.pm.dex.DexoptOptions;
import com.android.server.pm.dex.DexoptUtils;
import com.android.server.pm.dex.PackageDexUsage;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.pkg.PackageStateInternal;
import com.transsion.hubcore.server.pm.ITranPackageManagerService;
import dalvik.system.DexFile;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
/* loaded from: classes2.dex */
public class PackageDexOptimizer {
    public static final int DEX_OPT_CANCELLED = 2;
    public static final int DEX_OPT_FAILED = -1;
    public static final int DEX_OPT_PERFORMED = 1;
    public static final int DEX_OPT_SKIPPED = 0;
    static final String OAT_DIR_NAME = "oat";
    private static final String TAG = "PackageDexOptimizer";
    private static final long WAKELOCK_TIMEOUT_MS = 960000;
    private static final Random sRandom = new Random();
    private final ArtStatsLogUtils.ArtStatsLogger mArtStatsLogger;
    private final Context mContext;
    private final PowerManager.WakeLock mDexoptWakeLock;
    private final Injector mInjector;
    private final Object mInstallLock;
    private final Installer mInstaller;
    private volatile boolean mSystemReady;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface DexOptResult {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface Injector {
        AppHibernationManagerInternal getAppHibernationManagerInternal();

        PowerManager getPowerManager(Context context);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageDexOptimizer(Installer installer, Object installLock, Context context, String wakeLockTag) {
        this(new Injector() { // from class: com.android.server.pm.PackageDexOptimizer.1
            @Override // com.android.server.pm.PackageDexOptimizer.Injector
            public AppHibernationManagerInternal getAppHibernationManagerInternal() {
                return (AppHibernationManagerInternal) LocalServices.getService(AppHibernationManagerInternal.class);
            }

            @Override // com.android.server.pm.PackageDexOptimizer.Injector
            public PowerManager getPowerManager(Context context2) {
                return (PowerManager) context2.getSystemService(PowerManager.class);
            }
        }, installer, installLock, context, wakeLockTag);
    }

    protected PackageDexOptimizer(PackageDexOptimizer from) {
        this.mArtStatsLogger = new ArtStatsLogUtils.ArtStatsLogger();
        this.mContext = from.mContext;
        this.mInstaller = from.mInstaller;
        this.mInstallLock = from.mInstallLock;
        this.mDexoptWakeLock = from.mDexoptWakeLock;
        this.mSystemReady = from.mSystemReady;
        this.mInjector = from.mInjector;
    }

    PackageDexOptimizer(Injector injector, Installer installer, Object installLock, Context context, String wakeLockTag) {
        this.mArtStatsLogger = new ArtStatsLogUtils.ArtStatsLogger();
        this.mContext = context;
        this.mInstaller = installer;
        this.mInstallLock = installLock;
        PowerManager powerManager = injector.getPowerManager(context);
        this.mDexoptWakeLock = powerManager.newWakeLock(1, wakeLockTag);
        this.mInjector = injector;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canOptimizePackage(AndroidPackage pkg) {
        if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(pkg.getPackageName()) || pkg.isHasCode()) {
            AppHibernationManagerInternal ahm = this.mInjector.getAppHibernationManagerInternal();
            return (ahm != null && ahm.isHibernatingGlobally(pkg.getPackageName()) && ahm.isOatArtifactDeletionEnabled()) ? false : true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int performDexOpt(AndroidPackage pkg, PackageStateInternal pkgSetting, String[] instructionSets, CompilerStats.PackageStats packageStats, PackageDexUsage.PackageUseInfo packageUseInfo, DexoptOptions options) {
        int performDexOptLI;
        if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(pkg.getPackageName())) {
            throw new IllegalArgumentException("System server dexopting should be done via  DexManager and PackageDexOptimizer#dexoptSystemServerPath");
        }
        if (pkg.getUid() == -1) {
            throw new IllegalArgumentException("Dexopt for " + pkg.getPackageName() + " has invalid uid.");
        }
        if (!canOptimizePackage(pkg)) {
            return 0;
        }
        synchronized (this.mInstallLock) {
            long acquireTime = acquireWakeLockLI(pkg.getUid());
            performDexOptLI = performDexOptLI(pkg, pkgSetting, instructionSets, packageStats, packageUseInfo, options);
            releaseWakeLockLI(acquireTime);
        }
        return performDexOptLI;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void controlDexOptBlocking(boolean block) {
        getInstallerWithoutLock().controlDexOptBlocking(block);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [442=7, 444=4, 445=4, 446=4] */
    /* JADX WARN: Incorrect condition in loop: B:28:0x00e5 */
    /* JADX WARN: Removed duplicated region for block: B:177:0x0439 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:183:0x01b2 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:185:0x03df A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:195:0x0277 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:64:0x018d  */
    /* JADX WARN: Removed duplicated region for block: B:65:0x0190  */
    /* JADX WARN: Removed duplicated region for block: B:69:0x019f  */
    /* JADX WARN: Removed duplicated region for block: B:70:0x01a7  */
    /* JADX WARN: Removed duplicated region for block: B:85:0x0225  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int performDexOptLI(AndroidPackage pkg, PackageStateInternal pkgSetting, String[] targetInstructionSets, CompilerStats.PackageStats packageStats, PackageDexUsage.PackageUseInfo packageUseInfo, DexoptOptions options) {
        int sharedGid;
        int i;
        String[] classLoaderContexts;
        boolean[] pathsWithCode;
        int result;
        int sharedGid2;
        List<String> paths;
        String[] dexCodeInstructionSets;
        List<SharedLibraryInfo> sharedLibraries;
        CompilerStats.PackageStats packageStats2;
        PackageDexOptimizer packageDexOptimizer;
        String str;
        int i2;
        boolean z;
        boolean useCloudProfile;
        String dexMetadataPath;
        String[] classLoaderContexts2;
        int profileAnalysisResult;
        Throwable th;
        String str2;
        PackageDexOptimizer packageDexOptimizer2;
        String str3;
        String cloudProfileName;
        String profileName;
        String profileName2;
        int profileAnalysisResult2;
        String compilerFilter;
        String compilerFilter2;
        int length;
        int result2;
        String cloudProfileName2;
        String path;
        long sessionId;
        String newProfileName;
        PackageDexOptimizer packageDexOptimizer3 = this;
        AndroidPackage androidPackage = pkg;
        CompilerStats.PackageStats packageStats3 = packageStats;
        List<SharedLibraryInfo> sharedLibraries2 = pkgSetting.getTransientState().getNonNativeUsesLibraryInfos();
        String[] instructionSets = targetInstructionSets != null ? targetInstructionSets : InstructionSets.getAppDexInstructionSets(AndroidPackageUtils.getPrimaryCpuAbi(pkg, pkgSetting), AndroidPackageUtils.getSecondaryCpuAbi(pkg, pkgSetting));
        String[] dexCodeInstructionSets2 = InstructionSets.getDexCodeInstructionSets(instructionSets);
        List<String> paths2 = AndroidPackageUtils.getAllCodePaths(pkg);
        int sharedGid3 = UserHandle.getSharedAppGid(pkg.getUid());
        int i3 = -1;
        String cloudProfileName3 = TAG;
        if (sharedGid3 == -1) {
            Slog.wtf(TAG, "Well this is awkward; package " + pkg.getPackageName() + " had UID " + pkg.getUid(), new Throwable());
            sharedGid = 9999;
        } else {
            sharedGid = sharedGid3;
        }
        boolean[] pathsWithCode2 = new boolean[paths2.size()];
        pathsWithCode2[0] = pkg.isHasCode();
        for (int i4 = 1; i4 < paths2.size(); i4++) {
            pathsWithCode2[i4] = (pkg.getSplitFlags()[i4 + (-1)] & 4) != 0;
        }
        String[] classLoaderContexts3 = DexoptUtils.getClassLoaderContexts(androidPackage, sharedLibraries2, pathsWithCode2);
        if (paths2.size() != classLoaderContexts3.length) {
            String[] splitCodePaths = pkg.getSplitCodePaths();
            throw new IllegalStateException("Inconsistent information between AndroidPackage and its ApplicationInfo. pkg.getAllCodePaths=" + paths2 + " pkg.getBaseCodePath=" + pkg.getBaseApkPath() + " pkg.getSplitCodePaths=" + (splitCodePaths == null ? "null" : Arrays.toString(splitCodePaths)));
        }
        int result3 = 0;
        int i5 = 0;
        while (i5 < result) {
            if (pathsWithCode2[i5]) {
                if (classLoaderContexts3[i5] == null) {
                    throw new IllegalStateException("Inconsistent information in the package structure. A split is marked to contain code but has no dependency listed. Index=" + i5 + " path=" + paths2.get(i2));
                }
                String path2 = paths2.get(i5);
                if (options.getSplitName() == null || options.getSplitName().equals(new File(path2).getName())) {
                    String profileName3 = ArtManager.getProfileName(i5 == 0 ? null : pkg.getSplitNames()[i5 - 1]);
                    try {
                        try {
                            if (!options.isDexoptAsSharedLibrary() && !packageUseInfo.isUsedByOtherApps(path2)) {
                                z = false;
                                boolean isUsedByOtherApps = z;
                                String compilerFilter3 = packageDexOptimizer3.getRealCompilerFilter(androidPackage, options.getCompilerFilter());
                                boolean isProfileGuidedFilter = DexFile.isProfileGuidedCompilerFilter(compilerFilter3);
                                int compilationReason = options.getCompilationReason();
                                int i6 = i5;
                                useCloudProfile = (DexFile.isProfileGuidedCompilerFilter(compilerFilter3) || !isUsedByOtherApps || options.getCompilationReason() == 3) ? false : true;
                                if (!options.isDexoptInstallWithDexMetadata() || useCloudProfile) {
                                    File dexMetadataFile = DexMetadataHelper.findDexMetadataForFile(new File(path2));
                                    String dexMetadataPath2 = dexMetadataFile != null ? null : dexMetadataFile.getAbsolutePath();
                                    dexMetadataPath = dexMetadataPath2;
                                } else {
                                    dexMetadataPath = null;
                                }
                                if (options.isCheckForProfileUpdates()) {
                                    classLoaderContexts2 = classLoaderContexts3;
                                    profileAnalysisResult = 2;
                                } else {
                                    int profileAnalysisResult3 = packageDexOptimizer3.analyseProfiles(androidPackage, sharedGid, profileName3, compilerFilter3);
                                    classLoaderContexts2 = classLoaderContexts3;
                                    profileAnalysisResult = profileAnalysisResult3;
                                }
                                List<SharedLibraryInfo> sharedLibraries3 = sharedLibraries2;
                                String str4 = "Failed to cleanup cloud profile";
                                if (!useCloudProfile) {
                                    try {
                                        String cloudProfileName4 = "cloud-" + profileName3;
                                        try {
                                            if (packageDexOptimizer3.prepareCloudProfile(androidPackage, cloudProfileName4, path2, dexMetadataPath)) {
                                                profileName = cloudProfileName4;
                                            } else {
                                                compilerFilter3 = PackageManagerServiceCompilerMapping.getCompilerFilterForReason(13);
                                                profileName = null;
                                            }
                                            profileName2 = profileName;
                                            profileAnalysisResult2 = 2;
                                            compilerFilter = compilerFilter3;
                                            compilerFilter2 = cloudProfileName4;
                                        } catch (Throwable th2) {
                                            str2 = "Failed to cleanup cloud profile";
                                            packageDexOptimizer2 = packageDexOptimizer3;
                                            str3 = cloudProfileName3;
                                            cloudProfileName = cloudProfileName4;
                                            th = th2;
                                            if (cloudProfileName != null) {
                                                try {
                                                    packageDexOptimizer2.mInstaller.deleteReferenceProfile(pkg.getPackageName(), cloudProfileName);
                                                } catch (Installer.InstallerException e) {
                                                    Slog.w(str3, str2, e);
                                                }
                                            }
                                            throw th;
                                        }
                                    } catch (Throwable th3) {
                                        th = th3;
                                        str2 = "Failed to cleanup cloud profile";
                                        packageDexOptimizer2 = packageDexOptimizer3;
                                        str3 = cloudProfileName3;
                                        cloudProfileName = null;
                                    }
                                } else if (!isProfileGuidedFilter || profileAnalysisResult == 1 || ((compilationReason != 3 && dexMetadataPath == null) || (newProfileName = ITranPackageManagerService.Instance().getAppProfileName(pkg.getPackageName())) == null)) {
                                    profileName2 = profileName3;
                                    profileAnalysisResult2 = profileAnalysisResult;
                                    compilerFilter = compilerFilter3;
                                    compilerFilter2 = null;
                                } else {
                                    profileName2 = newProfileName;
                                    profileAnalysisResult2 = profileAnalysisResult;
                                    compilerFilter = compilerFilter3;
                                    compilerFilter2 = null;
                                }
                                String dexMetadataPath3 = dexMetadataPath;
                                int i7 = i6;
                                classLoaderContexts = classLoaderContexts2;
                                String path3 = path2;
                                pathsWithCode = pathsWithCode2;
                                int dexoptFlags = getDexFlags(pkg, pkgSetting, compilerFilter, useCloudProfile, options);
                                sharedGid2 = sharedGid;
                                length = dexCodeInstructionSets2.length;
                                result2 = 0;
                                int result4 = result3;
                                while (result2 < length) {
                                    try {
                                        String dexCodeIsa = dexCodeInstructionSets2[result2];
                                        int i8 = result2;
                                        int result5 = result4;
                                        String str5 = cloudProfileName3;
                                        String cloudProfileName5 = compilerFilter2;
                                        List<String> paths3 = paths2;
                                        String[] dexCodeInstructionSets3 = dexCodeInstructionSets2;
                                        String str6 = str4;
                                        List<SharedLibraryInfo> sharedLibraries4 = sharedLibraries3;
                                        int compilationReason2 = compilationReason;
                                        int i9 = i7;
                                        try {
                                            int newResult = dexOptPath(pkg, pkgSetting, path3, dexCodeIsa, compilerFilter, profileAnalysisResult2, classLoaderContexts[i7], dexoptFlags, sharedGid2, packageStats, options.isDowngrade(), profileName2, dexMetadataPath3, options.getCompilationReason());
                                            if (!PackageManagerService.DEBUG_ART_STATSLOG) {
                                                packageDexOptimizer2 = this;
                                                path = path3;
                                            } else if (packageStats != null) {
                                                try {
                                                    Trace.traceBegin(262144L, "dex2oat-metrics");
                                                    try {
                                                        sessionId = sRandom.nextLong();
                                                        packageDexOptimizer2 = this;
                                                    } catch (Throwable th4) {
                                                        th = th4;
                                                        packageDexOptimizer2 = this;
                                                    }
                                                    try {
                                                        path = path3;
                                                        try {
                                                            ArtStatsLogUtils.writeStatsLog(packageDexOptimizer2.mArtStatsLogger, sessionId, compilerFilter, pkg.getUid(), packageStats.getCompileTime(path), dexMetadataPath3, options.getCompilationReason(), newResult, ArtStatsLogUtils.getApkType(path, pkg.getBaseApkPath(), pkg.getSplitCodePaths()), dexCodeIsa, path);
                                                            try {
                                                                Trace.traceEnd(262144L);
                                                            } catch (Throwable th5) {
                                                                th = th5;
                                                                str3 = str5;
                                                                cloudProfileName = cloudProfileName5;
                                                                str2 = str6;
                                                                if (cloudProfileName != null) {
                                                                }
                                                                throw th;
                                                            }
                                                        } catch (Throwable th6) {
                                                            th = th6;
                                                            Trace.traceEnd(262144L);
                                                            throw th;
                                                        }
                                                    } catch (Throwable th7) {
                                                        th = th7;
                                                        Trace.traceEnd(262144L);
                                                        throw th;
                                                    }
                                                } catch (Throwable th8) {
                                                    packageDexOptimizer2 = this;
                                                    th = th8;
                                                    str3 = str5;
                                                    cloudProfileName = cloudProfileName5;
                                                    str2 = str6;
                                                }
                                            } else {
                                                packageDexOptimizer2 = this;
                                                path = path3;
                                            }
                                            if (newResult == 2) {
                                                if (result5 == -1) {
                                                    if (cloudProfileName5 != null) {
                                                        try {
                                                            packageDexOptimizer2.mInstaller.deleteReferenceProfile(pkg.getPackageName(), cloudProfileName5);
                                                        } catch (Installer.InstallerException e2) {
                                                            Slog.w(str5, str6, e2);
                                                        }
                                                    }
                                                    return result5;
                                                }
                                                if (cloudProfileName5 != null) {
                                                    try {
                                                        packageDexOptimizer2.mInstaller.deleteReferenceProfile(pkg.getPackageName(), cloudProfileName5);
                                                    } catch (Installer.InstallerException e3) {
                                                        Slog.w(str5, str6, e3);
                                                    }
                                                }
                                                return newResult;
                                            }
                                            int result6 = result5;
                                            if (result6 != -1 && newResult != 0) {
                                                result6 = newResult;
                                            }
                                            result4 = result6;
                                            path3 = path;
                                            str4 = str6;
                                            paths2 = paths3;
                                            sharedLibraries3 = sharedLibraries4;
                                            compilationReason = compilationReason2;
                                            i7 = i9;
                                            result2 = i8 + 1;
                                            compilerFilter2 = cloudProfileName5;
                                            cloudProfileName3 = str5;
                                            dexCodeInstructionSets2 = dexCodeInstructionSets3;
                                        } catch (Throwable th9) {
                                            packageDexOptimizer2 = this;
                                            str3 = str5;
                                            cloudProfileName = cloudProfileName5;
                                            str2 = str6;
                                            th = th9;
                                        }
                                    } catch (Throwable th10) {
                                        packageDexOptimizer2 = this;
                                        str2 = str4;
                                        str3 = cloudProfileName3;
                                        cloudProfileName = compilerFilter2;
                                        th = th10;
                                    }
                                }
                                packageDexOptimizer = this;
                                packageStats2 = packageStats;
                                int result7 = result4;
                                paths = paths2;
                                dexCodeInstructionSets = dexCodeInstructionSets2;
                                String str7 = str4;
                                i = i7;
                                sharedLibraries = sharedLibraries3;
                                result = -1;
                                str = cloudProfileName3;
                                cloudProfileName2 = compilerFilter2;
                                if (cloudProfileName2 != null) {
                                    try {
                                        packageDexOptimizer.mInstaller.deleteReferenceProfile(pkg.getPackageName(), cloudProfileName2);
                                    } catch (Installer.InstallerException e4) {
                                        Slog.w(str, str7, e4);
                                    }
                                }
                                result3 = result7;
                                androidPackage = pkg;
                                packageStats3 = packageStats2;
                                packageDexOptimizer3 = packageDexOptimizer;
                                i3 = result;
                                cloudProfileName3 = str;
                                classLoaderContexts3 = classLoaderContexts;
                                pathsWithCode2 = pathsWithCode;
                                sharedGid = sharedGid2;
                                paths2 = paths;
                                dexCodeInstructionSets2 = dexCodeInstructionSets;
                                sharedLibraries2 = sharedLibraries;
                                i5 = i + 1;
                            }
                            length = dexCodeInstructionSets2.length;
                            result2 = 0;
                            int result42 = result3;
                            while (result2 < length) {
                            }
                            packageDexOptimizer = this;
                            packageStats2 = packageStats;
                            int result72 = result42;
                            paths = paths2;
                            dexCodeInstructionSets = dexCodeInstructionSets2;
                            String str72 = str4;
                            i = i7;
                            sharedLibraries = sharedLibraries3;
                            result = -1;
                            str = cloudProfileName3;
                            cloudProfileName2 = compilerFilter2;
                            if (cloudProfileName2 != null) {
                            }
                            result3 = result72;
                            androidPackage = pkg;
                            packageStats3 = packageStats2;
                            packageDexOptimizer3 = packageDexOptimizer;
                            i3 = result;
                            cloudProfileName3 = str;
                            classLoaderContexts3 = classLoaderContexts;
                            pathsWithCode2 = pathsWithCode;
                            sharedGid = sharedGid2;
                            paths2 = paths;
                            dexCodeInstructionSets2 = dexCodeInstructionSets;
                            sharedLibraries2 = sharedLibraries;
                            i5 = i + 1;
                        } catch (Throwable th11) {
                            packageDexOptimizer2 = this;
                            str2 = "Failed to cleanup cloud profile";
                            str3 = cloudProfileName3;
                            cloudProfileName = compilerFilter2;
                            th = th11;
                        }
                        int dexoptFlags2 = getDexFlags(pkg, pkgSetting, compilerFilter, useCloudProfile, options);
                        sharedGid2 = sharedGid;
                    } catch (Throwable th12) {
                        packageDexOptimizer2 = this;
                        str2 = "Failed to cleanup cloud profile";
                        str3 = cloudProfileName3;
                        cloudProfileName = compilerFilter2;
                        th = th12;
                    }
                    z = true;
                    boolean isUsedByOtherApps2 = z;
                    String compilerFilter32 = packageDexOptimizer3.getRealCompilerFilter(androidPackage, options.getCompilerFilter());
                    boolean isProfileGuidedFilter2 = DexFile.isProfileGuidedCompilerFilter(compilerFilter32);
                    int compilationReason3 = options.getCompilationReason();
                    int i62 = i5;
                    useCloudProfile = (DexFile.isProfileGuidedCompilerFilter(compilerFilter32) || !isUsedByOtherApps2 || options.getCompilationReason() == 3) ? false : true;
                    if (options.isDexoptInstallWithDexMetadata()) {
                    }
                    File dexMetadataFile2 = DexMetadataHelper.findDexMetadataForFile(new File(path2));
                    String dexMetadataPath22 = dexMetadataFile2 != null ? null : dexMetadataFile2.getAbsolutePath();
                    dexMetadataPath = dexMetadataPath22;
                    if (options.isCheckForProfileUpdates()) {
                    }
                    List<SharedLibraryInfo> sharedLibraries32 = sharedLibraries2;
                    String str42 = "Failed to cleanup cloud profile";
                    if (!useCloudProfile) {
                    }
                    String dexMetadataPath32 = dexMetadataPath;
                    int i72 = i62;
                    classLoaderContexts = classLoaderContexts2;
                    String path32 = path2;
                    pathsWithCode = pathsWithCode2;
                }
            }
            i = i5;
            classLoaderContexts = classLoaderContexts3;
            pathsWithCode = pathsWithCode2;
            result = i3;
            sharedGid2 = sharedGid;
            paths = paths2;
            dexCodeInstructionSets = dexCodeInstructionSets2;
            sharedLibraries = sharedLibraries2;
            packageStats2 = packageStats3;
            packageDexOptimizer = packageDexOptimizer3;
            str = cloudProfileName3;
            androidPackage = pkg;
            packageStats3 = packageStats2;
            packageDexOptimizer3 = packageDexOptimizer;
            i3 = result;
            cloudProfileName3 = str;
            classLoaderContexts3 = classLoaderContexts;
            pathsWithCode2 = pathsWithCode;
            sharedGid = sharedGid2;
            paths2 = paths;
            dexCodeInstructionSets2 = dexCodeInstructionSets;
            sharedLibraries2 = sharedLibraries;
            i5 = i + 1;
        }
        return result3;
    }

    private boolean prepareCloudProfile(AndroidPackage pkg, String profileName, String path, String dexMetadataPath) {
        if (dexMetadataPath == null) {
            return false;
        }
        try {
            this.mInstaller.deleteReferenceProfile(pkg.getPackageName(), profileName);
            int appId = UserHandle.getAppId(pkg.getUid());
            this.mInstaller.prepareAppProfile(pkg.getPackageName(), -10000, appId, profileName, path, dexMetadataPath);
            return true;
        } catch (Installer.InstallerException e) {
            Slog.w(TAG, "Failed to prepare cloud profile", e);
            return false;
        }
    }

    private int dexOptPath(AndroidPackage pkg, PackageStateInternal pkgSetting, String path, String isa, String compilerFilter, int profileAnalysisResult, String classLoaderContext, int dexoptFlags, int uid, CompilerStats.PackageStats packageStats, boolean downgrade, String profileName, String dexMetadataPath, int compilationReason) {
        long startTime;
        String seInfo;
        String oatDir = getPackageOatDirIfSupported(pkg, pkgSetting.getTransientState().isUpdatedSystemApp());
        int dexoptNeeded = getDexoptNeeded(pkg.getPackageName(), path, isa, compilerFilter, classLoaderContext, profileAnalysisResult, downgrade, dexoptFlags, oatDir);
        if (Math.abs(dexoptNeeded) == 0) {
            return 0;
        }
        Log.i(TAG, "Running dexopt (dexoptNeeded=" + dexoptNeeded + ") on: " + path + " pkg=" + pkg.getPackageName() + " isa=" + isa + " dexoptFlags=" + printDexoptFlags(dexoptFlags) + " targetFilter=" + compilerFilter + " oatDir=" + oatDir + " profileName=" + profileName + " dexMetadataPath=" + dexMetadataPath + " profileAnalysisResult=" + profileAnalysisResult + " classLoaderContext=" + classLoaderContext);
        try {
            startTime = System.currentTimeMillis();
            seInfo = AndroidPackageUtils.getSeInfo(pkg, pkgSetting);
        } catch (Installer.InstallerException e) {
            e = e;
        }
        try {
            boolean completed = getInstallerLI().dexopt(path, uid, pkg.getPackageName(), isa, dexoptNeeded, oatDir, dexoptFlags, compilerFilter, pkg.getVolumeUuid(), classLoaderContext, seInfo, false, pkg.getTargetSdkVersion(), profileName, dexMetadataPath, getAugmentedReasonName(compilationReason, dexMetadataPath != null));
            if (!completed) {
                return 2;
            }
            if (packageStats != null) {
                long endTime = System.currentTimeMillis();
                packageStats.setCompileTime(path, (int) (endTime - startTime));
            }
            if (oatDir != null) {
                ContentResolver resolver = this.mContext.getContentResolver();
                F2fsUtils.releaseCompressedBlocks(resolver, new File(oatDir));
            }
            return 1;
        } catch (Installer.InstallerException e2) {
            e = e2;
            Slog.w(TAG, "Failed to dexopt", e);
            return -1;
        }
    }

    public int dexoptSystemServerPath(String dexPath, PackageDexUsage.DexUseInfo dexUseInfo, DexoptOptions options) {
        Object obj;
        int dexoptFlags = (options.isDexoptIdleBackgroundJob() ? 512 : 0) | (options.isBootComplete() ? 8 : 0) | 2;
        int result = 0;
        for (String isa : dexUseInfo.getLoaderIsas()) {
            int dexoptNeeded = getDexoptNeeded(PackageManagerService.PLATFORM_PACKAGE_NAME, dexPath, isa, options.getCompilerFilter(), dexUseInfo.getClassLoaderContext(), 3, false, dexoptFlags, null);
            if (dexoptNeeded != 0) {
                try {
                    Object obj2 = this.mInstallLock;
                    synchronized (obj2) {
                        try {
                            obj = obj2;
                        } catch (Throwable th) {
                            th = th;
                            obj = obj2;
                        }
                        try {
                            boolean completed = getInstallerLI().dexopt(dexPath, 1000, PackageManagerService.PLATFORM_PACKAGE_NAME, isa, dexoptNeeded, null, dexoptFlags, options.getCompilerFilter(), StorageManager.UUID_PRIVATE_INTERNAL, dexUseInfo.getClassLoaderContext(), null, false, 0, null, null, PackageManagerServiceCompilerMapping.getReasonName(options.getCompilationReason()));
                            if (!completed) {
                                return 2;
                            }
                            result = 1;
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    }
                } catch (Installer.InstallerException e) {
                    Slog.w(TAG, "Failed to dexopt", e);
                    return -1;
                }
            }
        }
        return result;
    }

    private String getAugmentedReasonName(int compilationReason, boolean useDexMetadata) {
        String annotation = useDexMetadata ? ArtManagerService.DEXOPT_REASON_WITH_DEX_METADATA_ANNOTATION : "";
        return PackageManagerServiceCompilerMapping.getReasonName(compilationReason) + annotation;
    }

    public int dexOptSecondaryDexPath(ApplicationInfo info, String path, PackageDexUsage.DexUseInfo dexUseInfo, DexoptOptions options) {
        int dexOptSecondaryDexPathLI;
        if (info.uid == -1) {
            throw new IllegalArgumentException("Dexopt for path " + path + " has invalid uid.");
        }
        synchronized (this.mInstallLock) {
            long acquireTime = acquireWakeLockLI(info.uid);
            dexOptSecondaryDexPathLI = dexOptSecondaryDexPathLI(info, path, dexUseInfo, options);
            releaseWakeLockLI(acquireTime);
        }
        return dexOptSecondaryDexPathLI;
    }

    private long acquireWakeLockLI(int uid) {
        if (!this.mSystemReady) {
            return -1L;
        }
        this.mDexoptWakeLock.setWorkSource(new WorkSource(uid));
        this.mDexoptWakeLock.acquire(WAKELOCK_TIMEOUT_MS);
        return SystemClock.elapsedRealtime();
    }

    private void releaseWakeLockLI(long acquireTime) {
        if (acquireTime < 0) {
            return;
        }
        try {
            if (this.mDexoptWakeLock.isHeld()) {
                this.mDexoptWakeLock.release();
            }
            long duration = SystemClock.elapsedRealtime() - acquireTime;
            if (duration >= WAKELOCK_TIMEOUT_MS) {
                Slog.wtf(TAG, "WakeLock " + this.mDexoptWakeLock.getTag() + " time out. Operation took " + duration + " ms. Thread: " + Thread.currentThread().getName());
            }
        } catch (Exception e) {
            Slog.wtf(TAG, "Error while releasing " + this.mDexoptWakeLock.getTag() + " lock", e);
        }
    }

    private int dexOptSecondaryDexPathLI(ApplicationInfo info, String path, PackageDexUsage.DexUseInfo dexUseInfo, DexoptOptions options) {
        String str;
        int dexoptFlags;
        String compilerFilter;
        String classLoaderContext;
        String str2;
        if (options.isDexoptOnlySharedDex() && !dexUseInfo.isUsedByOtherApps()) {
            return 0;
        }
        String compilerFilter2 = getRealCompilerFilter(info, options.getCompilerFilter(), dexUseInfo.isUsedByOtherApps());
        int dexoptFlags2 = getDexFlags(info, compilerFilter2, options) | 32;
        String str3 = info.deviceProtectedDataDir;
        String str4 = TAG;
        if (str3 != null && FileUtils.contains(info.deviceProtectedDataDir, path)) {
            dexoptFlags = dexoptFlags2 | 256;
        } else {
            if (info.credentialProtectedDataDir == null) {
                str = TAG;
            } else if (!FileUtils.contains(info.credentialProtectedDataDir, path)) {
                str = TAG;
            } else {
                dexoptFlags = dexoptFlags2 | 128;
            }
            Slog.e(str, "Could not infer CE/DE storage for package " + info.packageName);
            return -1;
        }
        if (dexUseInfo.isUnsupportedClassLoaderContext() || dexUseInfo.isVariableClassLoaderContext()) {
            compilerFilter = "verify";
            classLoaderContext = null;
        } else {
            String classLoaderContext2 = dexUseInfo.getClassLoaderContext();
            compilerFilter = compilerFilter2;
            classLoaderContext = classLoaderContext2;
        }
        int reason = options.getCompilationReason();
        Log.d(TAG, "Running dexopt on: " + path + " pkg=" + info.packageName + " isa=" + dexUseInfo.getLoaderIsas() + " reason=" + PackageManagerServiceCompilerMapping.getReasonName(reason) + " dexoptFlags=" + printDexoptFlags(dexoptFlags) + " target-filter=" + compilerFilter + " class-loader-context=" + classLoaderContext);
        try {
            for (String isa : dexUseInfo.getLoaderIsas()) {
                String classLoaderContext3 = classLoaderContext;
                String compilerFilter3 = compilerFilter;
                int dexoptFlags3 = dexoptFlags;
                str2 = str4;
                try {
                    boolean completed = getInstallerLI().dexopt(path, info.uid, info.packageName, isa, 0, null, dexoptFlags, compilerFilter, info.volumeUuid, classLoaderContext3, info.seInfo, options.isDowngrade(), info.targetSdkVersion, null, null, PackageManagerServiceCompilerMapping.getReasonName(reason));
                    if (!completed) {
                        return 2;
                    }
                    classLoaderContext = classLoaderContext3;
                    compilerFilter = compilerFilter3;
                    dexoptFlags = dexoptFlags3;
                    str4 = str2;
                } catch (Installer.InstallerException e) {
                    e = e;
                    Slog.w(str2, "Failed to dexopt", e);
                    return -1;
                }
            }
            return 1;
        } catch (Installer.InstallerException e2) {
            e = e2;
            str2 = str4;
        }
    }

    protected int adjustDexoptNeeded(int dexoptNeeded) {
        return dexoptNeeded;
    }

    protected int adjustDexoptFlags(int dexoptFlags) {
        return dexoptFlags;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDexoptState(IndentingPrintWriter pw, AndroidPackage pkg, PackageStateInternal pkgSetting, PackageDexUsage.PackageUseInfo useInfo) {
        String[] instructionSets = InstructionSets.getAppDexInstructionSets(AndroidPackageUtils.getPrimaryCpuAbi(pkg, pkgSetting), AndroidPackageUtils.getSecondaryCpuAbi(pkg, pkgSetting));
        String[] dexCodeInstructionSets = InstructionSets.getDexCodeInstructionSets(instructionSets);
        List<String> paths = AndroidPackageUtils.getAllCodePathsExcludingResourceOnly(pkg);
        for (String path : paths) {
            pw.println("path: " + path);
            pw.increaseIndent();
            for (String isa : dexCodeInstructionSets) {
                try {
                    DexFile.OptimizationInfo info = DexFile.getDexFileOptimizationInfo(path, isa);
                    pw.println(isa + ": [status=" + info.getStatus() + "] [reason=" + info.getReason() + "]");
                } catch (IOException ioe) {
                    pw.println(isa + ": [Exception]: " + ioe.getMessage());
                }
            }
            if (useInfo.isUsedByOtherApps(path)) {
                pw.println("used by other apps: " + useInfo.getLoadingPackages(path));
            }
            Map<String, PackageDexUsage.DexUseInfo> dexUseInfoMap = useInfo.getDexUseInfoMap();
            if (!dexUseInfoMap.isEmpty()) {
                pw.println("known secondary dex files:");
                pw.increaseIndent();
                for (Map.Entry<String, PackageDexUsage.DexUseInfo> e : dexUseInfoMap.entrySet()) {
                    String dex = e.getKey();
                    PackageDexUsage.DexUseInfo dexUseInfo = e.getValue();
                    pw.println(dex);
                    pw.increaseIndent();
                    pw.println("class loader context: " + dexUseInfo.getClassLoaderContext());
                    if (dexUseInfo.isUsedByOtherApps()) {
                        pw.println("used by other apps: " + dexUseInfo.getLoadingPackages());
                    }
                    pw.decreaseIndent();
                }
                pw.decreaseIndent();
            }
            pw.decreaseIndent();
        }
    }

    private String getRealCompilerFilter(ApplicationInfo info, String targetCompilerFilter, boolean isUsedByOtherApps) {
        if (!info.isEmbeddedDexUsed()) {
            if (info.isPrivilegedApp() && DexManager.isPackageSelectedToRunOob(info.packageName)) {
                return "verify";
            }
            boolean vmSafeModeOrDebuggable = ((info.flags & 16384) == 0 && (info.flags & 2) == 0) ? false : true;
            if (vmSafeModeOrDebuggable) {
                return DexFile.getSafeModeCompilerFilter(targetCompilerFilter);
            }
            if (DexFile.isProfileGuidedCompilerFilter(targetCompilerFilter) && isUsedByOtherApps) {
                return PackageManagerServiceCompilerMapping.getCompilerFilterForReason(13);
            }
            return targetCompilerFilter;
        }
        return "verify";
    }

    private String getRealCompilerFilter(AndroidPackage pkg, String targetCompilerFilter) {
        if (!pkg.isUseEmbeddedDex()) {
            if (pkg.isPrivileged() && DexManager.isPackageSelectedToRunOob(pkg.getPackageName())) {
                return "verify";
            }
            boolean vmSafeModeOrDebuggable = pkg.isVmSafeMode() || pkg.isDebuggable();
            if (vmSafeModeOrDebuggable) {
                return DexFile.getSafeModeCompilerFilter(targetCompilerFilter);
            }
            return targetCompilerFilter;
        }
        return "verify";
    }

    private boolean isAppImageEnabled() {
        return SystemProperties.get("dalvik.vm.appimageformat", "").length() > 0;
    }

    private int getDexFlags(ApplicationInfo info, String compilerFilter, DexoptOptions options) {
        return getDexFlags((info.flags & 2) != 0, info.getHiddenApiEnforcementPolicy(), info.splitDependencies, info.requestsIsolatedSplitLoading(), compilerFilter, false, options);
    }

    private int getDexFlags(AndroidPackage pkg, PackageStateInternal pkgSetting, String compilerFilter, boolean useCloudProfile, DexoptOptions options) {
        return getDexFlags(pkg.isDebuggable(), AndroidPackageUtils.getHiddenApiEnforcementPolicy(pkg, pkgSetting), pkg.getSplitDependencies(), pkg.isIsolatedSplitLoading(), compilerFilter, useCloudProfile, options);
    }

    private int getDexFlags(boolean debuggable, int hiddenApiEnforcementPolicy, SparseArray<int[]> splitDependencies, boolean requestsIsolatedSplitLoading, String compilerFilter, boolean useCloudProfile, DexoptOptions options) {
        int hiddenApiFlag;
        boolean isProfileGuidedFilter = DexFile.isProfileGuidedCompilerFilter(compilerFilter);
        boolean generateAppImage = true;
        boolean isPublic = !isProfileGuidedFilter || options.isDexoptInstallWithDexMetadata() || useCloudProfile;
        int profileFlag = isProfileGuidedFilter ? 16 : 0;
        if (hiddenApiEnforcementPolicy == 0) {
            hiddenApiFlag = 0;
        } else {
            hiddenApiFlag = 1024;
        }
        int compilationReason = options.getCompilationReason();
        boolean generateCompactDex = true;
        switch (compilationReason) {
            case 0:
            case 1:
            case 2:
            case 3:
                generateCompactDex = false;
                break;
        }
        if (!isProfileGuidedFilter || ((splitDependencies != null && requestsIsolatedSplitLoading) || !isAppImageEnabled())) {
            generateAppImage = false;
        }
        int dexFlags = (options.isDexoptInstallForRestore() ? 8192 : 0) | (isPublic ? 2 : 0) | (debuggable ? 4 : 0) | profileFlag | (options.isBootComplete() ? 8 : 0) | (options.isDexoptIdleBackgroundJob() ? 512 : 0) | (generateCompactDex ? 2048 : 0) | (generateAppImage ? 4096 : 0) | hiddenApiFlag;
        return adjustDexoptFlags(dexFlags);
    }

    private int getDexoptNeeded(String packageName, String path, String isa, String compilerFilter, String classLoaderContext, int profileAnalysisResult, boolean downgrade, int dexoptFlags, String oatDir) {
        boolean newProfile;
        String actualCompilerFilter;
        boolean shouldBePublic = (dexoptFlags & 2) != 0;
        boolean isProfileGuidedFilter = (dexoptFlags & 16) != 0;
        boolean newProfile2 = profileAnalysisResult == 1;
        try {
            if (!newProfile2 && isProfileGuidedFilter && shouldBePublic) {
                if (isOdexPrivate(packageName, path, isa, oatDir)) {
                    newProfile = true;
                    actualCompilerFilter = compilerFilter;
                    if (compilerFilterDependsOnProfiles(compilerFilter) && profileAnalysisResult == 3) {
                        actualCompilerFilter = "verify";
                    }
                    int dexoptNeeded = DexFile.getDexOptNeeded(path, isa, actualCompilerFilter, classLoaderContext, newProfile, downgrade);
                    return adjustDexoptNeeded(dexoptNeeded);
                }
            }
            if (compilerFilterDependsOnProfiles(compilerFilter)) {
                actualCompilerFilter = "verify";
            }
            int dexoptNeeded2 = DexFile.getDexOptNeeded(path, isa, actualCompilerFilter, classLoaderContext, newProfile, downgrade);
            return adjustDexoptNeeded(dexoptNeeded2);
        } catch (IOException ioe) {
            Slog.w(TAG, "IOException reading apk: " + path, ioe);
            return -1;
        } catch (Exception e) {
            Slog.wtf(TAG, "Unexpected exception when calling dexoptNeeded on " + path, e);
            return -1;
        }
        newProfile = newProfile2;
        actualCompilerFilter = compilerFilter;
    }

    private boolean compilerFilterDependsOnProfiles(String compilerFilter) {
        return compilerFilter.endsWith("-profile");
    }

    private boolean isOdexPrivate(String packageName, String path, String isa, String oatDir) {
        try {
            return this.mInstaller.getOdexVisibility(packageName, path, isa, oatDir) == 2;
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get odex visibility for " + path, e);
            return false;
        }
    }

    private int analyseProfiles(AndroidPackage pkg, int uid, String profileName, String compilerFilter) {
        int mergeProfiles;
        if (DexFile.isProfileGuidedCompilerFilter(compilerFilter)) {
            try {
                synchronized (this.mInstallLock) {
                    mergeProfiles = getInstallerLI().mergeProfiles(uid, pkg.getPackageName(), profileName);
                }
                return mergeProfiles;
            } catch (Installer.InstallerException e) {
                Slog.w(TAG, "Failed to merge profiles", e);
                return 2;
            }
        }
        return 2;
    }

    private String getPackageOatDirIfSupported(AndroidPackage pkg, boolean isUpdatedSystemApp) {
        if (AndroidPackageUtils.canHaveOatDir(pkg, isUpdatedSystemApp)) {
            File codePath = new File(pkg.getPath());
            if (codePath.isDirectory()) {
                return getOatDir(codePath).getAbsolutePath();
            }
            return null;
        }
        return null;
    }

    public static File getOatDir(File codePath) {
        return new File(codePath, OAT_DIR_NAME);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void systemReady() {
        this.mSystemReady = true;
    }

    private String printDexoptFlags(int flags) {
        ArrayList<String> flagsList = new ArrayList<>();
        if ((flags & 8) == 8) {
            flagsList.add("boot_complete");
        }
        if ((flags & 4) == 4) {
            flagsList.add("debuggable");
        }
        if ((flags & 16) == 16) {
            flagsList.add("profile_guided");
        }
        if ((flags & 2) == 2) {
            flagsList.add("public");
        }
        if ((flags & 32) == 32) {
            flagsList.add("secondary");
        }
        if ((flags & 64) == 64) {
            flagsList.add("force");
        }
        if ((flags & 128) == 128) {
            flagsList.add("storage_ce");
        }
        if ((flags & 256) == 256) {
            flagsList.add("storage_de");
        }
        if ((flags & 512) == 512) {
            flagsList.add("idle_background_job");
        }
        if ((flags & 1024) == 1024) {
            flagsList.add("enable_hidden_api_checks");
        }
        return String.join(",", flagsList);
    }

    /* loaded from: classes2.dex */
    public static class ForcedUpdatePackageDexOptimizer extends PackageDexOptimizer {
        public ForcedUpdatePackageDexOptimizer(Installer installer, Object installLock, Context context, String wakeLockTag) {
            super(installer, installLock, context, wakeLockTag);
        }

        public ForcedUpdatePackageDexOptimizer(PackageDexOptimizer from) {
            super(from);
        }

        @Override // com.android.server.pm.PackageDexOptimizer
        protected int adjustDexoptNeeded(int dexoptNeeded) {
            if (dexoptNeeded == 0) {
                return -3;
            }
            return dexoptNeeded;
        }

        @Override // com.android.server.pm.PackageDexOptimizer
        protected int adjustDexoptFlags(int flags) {
            return flags | 64;
        }
    }

    private Installer getInstallerLI() {
        return this.mInstaller;
    }

    private Installer getInstallerWithoutLock() {
        return this.mInstaller;
    }
}
