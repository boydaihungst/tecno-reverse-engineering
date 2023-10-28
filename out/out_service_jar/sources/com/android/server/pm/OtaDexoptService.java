package com.android.server.pm;

import android.content.Context;
import android.content.pm.IOtaDexopt;
import android.content.pm.PackageManagerInternal;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import com.android.internal.logging.MetricsLogger;
import com.android.server.LocalServices;
import com.android.server.pm.Installer;
import com.android.server.pm.PackageDexOptimizer;
import com.android.server.pm.dex.DexoptOptions;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.slice.SliceClientPermissions;
import java.io.File;
import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
/* loaded from: classes2.dex */
public class OtaDexoptService extends IOtaDexopt.Stub {
    private static final long BULK_DELETE_THRESHOLD = 1073741824;
    private static final boolean DEBUG_DEXOPT = true;
    private static final String TAG = "OTADexopt";
    private long availableSpaceAfterBulkDelete;
    private long availableSpaceAfterDexopt;
    private long availableSpaceBefore;
    private int completeSize;
    private int dexoptCommandCountExecuted;
    private int dexoptCommandCountTotal;
    private int importantPackageCount;
    private final Context mContext;
    private List<String> mDexoptCommands;
    private final PackageManagerService mPackageManagerService;
    private final MetricsLogger metricsLogger = new MetricsLogger();
    private long otaDexoptTimeStart;
    private int otherPackageCount;

    public OtaDexoptService(Context context, PackageManagerService packageManagerService) {
        this.mContext = context;
        this.mPackageManagerService = packageManagerService;
    }

    /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.pm.OtaDexoptService, android.os.IBinder] */
    public static OtaDexoptService main(Context context, PackageManagerService packageManagerService) {
        ?? otaDexoptService = new OtaDexoptService(context, packageManagerService);
        ServiceManager.addService("otadexopt", (IBinder) otaDexoptService);
        otaDexoptService.moveAbArtifacts(packageManagerService.mInstaller);
        return otaDexoptService;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.pm.OtaDexoptService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new OtaDexoptShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    public synchronized void prepare() throws RemoteException {
        if (this.mDexoptCommands != null) {
            throw new IllegalStateException("already called prepare()");
        }
        Predicate<? super PackageStateInternal> isPlatformPackage = new Predicate() { // from class: com.android.server.pm.OtaDexoptService$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean equals;
                equals = PackageManagerService.PLATFORM_PACKAGE_NAME.equals(((PackageStateInternal) obj).getPkg().getPackageName());
                return equals;
            }
        };
        Computer snapshot = this.mPackageManagerService.snapshotComputer();
        Collection<? extends PackageStateInternal> allPackageStates = snapshot.getPackageStates().values();
        List<PackageStateInternal> important = DexOptHelper.getPackagesForDexopt(allPackageStates, this.mPackageManagerService, true);
        important.removeIf(isPlatformPackage);
        List<PackageStateInternal> others = new ArrayList<>(allPackageStates);
        others.removeAll(important);
        others.removeIf(PackageManagerServiceUtils.REMOVE_IF_NULL_PKG);
        others.removeIf(isPlatformPackage);
        this.mDexoptCommands = new ArrayList((allPackageStates.size() * 3) / 2);
        for (PackageStateInternal pkgSetting : important) {
            this.mDexoptCommands.addAll(generatePackageDexopts(pkgSetting.getPkg(), pkgSetting, 10));
        }
        for (PackageStateInternal pkgSetting2 : others) {
            if (pkgSetting2.getPkg().isCoreApp()) {
                throw new IllegalStateException("Found a core app that's not important");
            }
            this.mDexoptCommands.addAll(generatePackageDexopts(pkgSetting2.getPkg(), pkgSetting2, 0));
        }
        this.completeSize = this.mDexoptCommands.size();
        long spaceAvailable = getAvailableSpace();
        if (spaceAvailable < BULK_DELETE_THRESHOLD) {
            Log.i(TAG, "Low on space, deleting oat files in an attempt to free up space: " + DexOptHelper.packagesToString(others));
            for (PackageStateInternal pkg : others) {
                this.mPackageManagerService.deleteOatArtifactsOfPackage(snapshot, pkg.getPackageName());
            }
        }
        long spaceAvailableNow = getAvailableSpace();
        prepareMetricsLogging(important.size(), others.size(), spaceAvailable, spaceAvailableNow);
        try {
            PackageStateInternal lastUsed = (PackageStateInternal) Collections.max(important, Comparator.comparingLong(new ToLongFunction() { // from class: com.android.server.pm.OtaDexoptService$$ExternalSyntheticLambda1
                @Override // java.util.function.ToLongFunction
                public final long applyAsLong(Object obj) {
                    long latestForegroundPackageUseTimeInMills;
                    latestForegroundPackageUseTimeInMills = ((PackageStateInternal) obj).getTransientState().getLatestForegroundPackageUseTimeInMills();
                    return latestForegroundPackageUseTimeInMills;
                }
            }));
            Log.d(TAG, "A/B OTA: lastUsed time = " + lastUsed.getTransientState().getLatestForegroundPackageUseTimeInMills());
            Log.d(TAG, "A/B OTA: deprioritized packages:");
            Iterator<PackageStateInternal> it = others.iterator();
            while (it.hasNext()) {
                PackageStateInternal pkgSetting3 = it.next();
                PackageStateInternal lastUsed2 = lastUsed;
                Iterator<PackageStateInternal> it2 = it;
                Log.d(TAG, "  " + pkgSetting3.getPackageName() + " - " + pkgSetting3.getTransientState().getLatestForegroundPackageUseTimeInMills());
                lastUsed = lastUsed2;
                it = it2;
            }
        } catch (Exception e) {
        }
    }

    public synchronized void cleanup() throws RemoteException {
        Log.i(TAG, "Cleaning up OTA Dexopt state.");
        this.mDexoptCommands = null;
        this.availableSpaceAfterDexopt = getAvailableSpace();
        performMetricsLogging();
    }

    public synchronized boolean isDone() throws RemoteException {
        List<String> list;
        list = this.mDexoptCommands;
        if (list == null) {
            throw new IllegalStateException("done() called before prepare()");
        }
        return list.isEmpty();
    }

    public synchronized float getProgress() throws RemoteException {
        if (this.completeSize == 0) {
            return 1.0f;
        }
        int commandsLeft = this.mDexoptCommands.size();
        int i = this.completeSize;
        return (i - commandsLeft) / i;
    }

    public synchronized String nextDexoptCommand() throws RemoteException {
        List<String> list = this.mDexoptCommands;
        if (list == null) {
            throw new IllegalStateException("dexoptNextPackage() called before prepare()");
        }
        if (list.isEmpty()) {
            return "(all done)";
        }
        String next = this.mDexoptCommands.remove(0);
        if (getAvailableSpace() > 0) {
            this.dexoptCommandCountExecuted++;
            Log.d(TAG, "Next command: " + next);
            return next;
        }
        Log.w(TAG, "Not enough space for OTA dexopt, stopping with " + (this.mDexoptCommands.size() + 1) + " commands left.");
        this.mDexoptCommands.clear();
        return "(no free space)";
    }

    private long getMainLowSpaceThreshold() {
        File dataDir = Environment.getDataDirectory();
        long lowThreshold = StorageManager.from(this.mContext).getStorageLowBytes(dataDir);
        if (lowThreshold == 0) {
            throw new IllegalStateException("Invalid low memory threshold");
        }
        return lowThreshold;
    }

    private long getAvailableSpace() {
        long lowThreshold = getMainLowSpaceThreshold();
        File dataDir = Environment.getDataDirectory();
        long usableSpace = dataDir.getUsableSpace();
        return usableSpace - lowThreshold;
    }

    private synchronized List<String> generatePackageDexopts(AndroidPackage pkg, PackageStateInternal pkgSetting, int compilationReason) {
        final List<String> commands;
        commands = new ArrayList<>();
        Installer collectingInstaller = new Installer(this.mContext, true) { // from class: com.android.server.pm.OtaDexoptService.1
            @Override // com.android.server.pm.Installer
            public boolean dexopt(String apkPath, int uid, String pkgName, String instructionSet, int dexoptNeeded, String outputPath, int dexFlags, String compilerFilter, String volumeUuid, String sharedLibraries, String seInfo, boolean downgrade, int targetSdkVersion, String profileName, String dexMetadataPath, String dexoptCompilationReason) throws Installer.InstallerException {
                StringBuilder builder = new StringBuilder();
                builder.append("10 ");
                builder.append("dexopt");
                encodeParameter(builder, apkPath);
                encodeParameter(builder, Integer.valueOf(uid));
                encodeParameter(builder, pkgName);
                encodeParameter(builder, instructionSet);
                encodeParameter(builder, Integer.valueOf(dexoptNeeded));
                encodeParameter(builder, outputPath);
                encodeParameter(builder, Integer.valueOf(dexFlags));
                encodeParameter(builder, compilerFilter);
                encodeParameter(builder, volumeUuid);
                encodeParameter(builder, sharedLibraries);
                encodeParameter(builder, seInfo);
                encodeParameter(builder, Boolean.valueOf(downgrade));
                encodeParameter(builder, Integer.valueOf(targetSdkVersion));
                encodeParameter(builder, profileName);
                encodeParameter(builder, dexMetadataPath);
                encodeParameter(builder, dexoptCompilationReason);
                commands.add(builder.toString());
                return true;
            }

            private void encodeParameter(StringBuilder builder, Object arg) {
                builder.append(' ');
                if (arg == null) {
                    builder.append('!');
                    return;
                }
                String txt = String.valueOf(arg);
                if (txt.indexOf(0) != -1 || txt.indexOf(32) != -1 || "!".equals(txt)) {
                    throw new IllegalArgumentException("Invalid argument while executing " + arg);
                }
                builder.append(txt);
            }
        };
        PackageDexOptimizer optimizer = new OTADexoptPackageDexOptimizer(collectingInstaller, this.mPackageManagerService.mInstallLock, this.mContext);
        optimizer.performDexOpt(pkg, pkgSetting, null, null, this.mPackageManagerService.getDexManager().getPackageUseInfoOrDefault(pkg.getPackageName()), new DexoptOptions(pkg.getPackageName(), compilationReason, 4));
        return commands;
    }

    public synchronized void dexoptNextPackage() throws RemoteException {
        throw new UnsupportedOperationException();
    }

    private void moveAbArtifacts(Installer installer) {
        ArrayMap<String, ? extends PackageStateInternal> packageStates;
        OtaDexoptService otaDexoptService = this;
        if (otaDexoptService.mDexoptCommands != null) {
            throw new IllegalStateException("Should not be ota-dexopting when trying to move.");
        }
        if (!otaDexoptService.mPackageManagerService.isDeviceUpgrading()) {
            Slog.d(TAG, "No upgrade, skipping A/B artifacts check.");
            return;
        }
        ArrayMap<String, ? extends PackageStateInternal> packageStates2 = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getPackageStates();
        int packagePaths = 0;
        int pathsSuccessful = 0;
        int index = 0;
        while (index < packageStates2.size()) {
            PackageStateInternal packageState = packageStates2.valueAt(index);
            AndroidPackage pkg = packageState.getPkg();
            if (pkg == null) {
                packageStates = packageStates2;
            } else if (!otaDexoptService.mPackageManagerService.mPackageDexOptimizer.canOptimizePackage(pkg)) {
                packageStates = packageStates2;
            } else if (pkg.getPath() == null) {
                Slog.w(TAG, "Package " + pkg + " can be optimized but has null codePath");
                packageStates = packageStates2;
            } else if (pkg.getPath().startsWith("/system")) {
                packageStates = packageStates2;
            } else if (pkg.getPath().startsWith("/vendor")) {
                packageStates = packageStates2;
            } else if (pkg.getPath().startsWith("/product")) {
                packageStates = packageStates2;
            } else if (pkg.getPath().startsWith("/system_ext")) {
                packageStates = packageStates2;
            } else if (otaDexoptService.isTrPartition(pkg.getPath())) {
                packageStates = packageStates2;
            } else {
                String[] instructionSets = InstructionSets.getAppDexInstructionSets(AndroidPackageUtils.getPrimaryCpuAbi(pkg, packageState), AndroidPackageUtils.getSecondaryCpuAbi(pkg, packageState));
                List<String> paths = AndroidPackageUtils.getAllCodePathsExcludingResourceOnly(pkg);
                String[] dexCodeInstructionSets = InstructionSets.getDexCodeInstructionSets(instructionSets);
                String packageName = pkg.getPackageName();
                int length = dexCodeInstructionSets.length;
                int i = 0;
                while (i < length) {
                    String dexCodeInstructionSet = dexCodeInstructionSets[i];
                    for (String path : paths) {
                        ArrayMap<String, ? extends PackageStateInternal> packageStates3 = packageStates2;
                        PackageStateInternal packageState2 = packageState;
                        String oatDir = PackageDexOptimizer.getOatDir(new File(pkg.getPath())).getAbsolutePath();
                        int packagePaths2 = packagePaths + 1;
                        try {
                            installer.moveAb(packageName, path, dexCodeInstructionSet, oatDir);
                            pathsSuccessful++;
                        } catch (Installer.InstallerException e) {
                        }
                        packageStates2 = packageStates3;
                        packageState = packageState2;
                        packagePaths = packagePaths2;
                    }
                    i++;
                    packageState = packageState;
                }
                packageStates = packageStates2;
            }
            index++;
            otaDexoptService = this;
            packageStates2 = packageStates;
        }
        Slog.i(TAG, "Moved " + pathsSuccessful + SliceClientPermissions.SliceAuthority.DELIMITER + packagePaths);
    }

    private boolean isTrPartition(String codePath) {
        return true == SystemProperties.getBoolean("ro.tran.partition.extend", false) && (codePath.startsWith("/tr_product") || codePath.startsWith("/tr_mi") || codePath.startsWith("/tr_preload") || codePath.startsWith("/tr_company") || codePath.startsWith("/tr_region") || codePath.startsWith("/tr_carrier") || codePath.startsWith("/tr_theme"));
    }

    private void prepareMetricsLogging(int important, int others, long spaceBegin, long spaceBulk) {
        this.availableSpaceBefore = spaceBegin;
        this.availableSpaceAfterBulkDelete = spaceBulk;
        this.availableSpaceAfterDexopt = 0L;
        this.importantPackageCount = important;
        this.otherPackageCount = others;
        this.dexoptCommandCountTotal = this.mDexoptCommands.size();
        this.dexoptCommandCountExecuted = 0;
        this.otaDexoptTimeStart = System.nanoTime();
    }

    private static int inMegabytes(long value) {
        long in_mega_bytes = value / 1048576;
        if (in_mega_bytes > 2147483647L) {
            Log.w(TAG, "Recording " + in_mega_bytes + "MB of free space, overflowing range");
            return Integer.MAX_VALUE;
        }
        return (int) in_mega_bytes;
    }

    private void performMetricsLogging() {
        long finalTime = System.nanoTime();
        this.metricsLogger.histogram("ota_dexopt_available_space_before_mb", inMegabytes(this.availableSpaceBefore));
        this.metricsLogger.histogram("ota_dexopt_available_space_after_bulk_delete_mb", inMegabytes(this.availableSpaceAfterBulkDelete));
        this.metricsLogger.histogram("ota_dexopt_available_space_after_dexopt_mb", inMegabytes(this.availableSpaceAfterDexopt));
        this.metricsLogger.histogram("ota_dexopt_num_important_packages", this.importantPackageCount);
        this.metricsLogger.histogram("ota_dexopt_num_other_packages", this.otherPackageCount);
        this.metricsLogger.histogram("ota_dexopt_num_commands", this.dexoptCommandCountTotal);
        this.metricsLogger.histogram("ota_dexopt_num_commands_executed", this.dexoptCommandCountExecuted);
        int elapsedTimeSeconds = (int) TimeUnit.NANOSECONDS.toSeconds(finalTime - this.otaDexoptTimeStart);
        this.metricsLogger.histogram("ota_dexopt_time_s", elapsedTimeSeconds);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class OTADexoptPackageDexOptimizer extends PackageDexOptimizer.ForcedUpdatePackageDexOptimizer {
        public OTADexoptPackageDexOptimizer(Installer installer, Object installLock, Context context) {
            super(installer, installLock, context, "*otadexopt*");
        }
    }
}
