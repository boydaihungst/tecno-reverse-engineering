package com.android.server.pm;

import android.content.Context;
import android.content.pm.PackageStats;
import android.os.Build;
import android.os.CreateAppDataArgs;
import android.os.CreateAppDataResult;
import android.os.IBinder;
import android.os.IInstalld;
import android.os.ReconcileSdkDataArgs;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.CrateMetadata;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.server.SystemService;
import dalvik.system.BlockGuard;
import dalvik.system.VMRuntime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
/* loaded from: classes2.dex */
public class Installer extends SystemService {
    private static final long CONNECT_RETRY_DELAY_MS = 1000;
    private static final long CONNECT_WAIT_MS = 10000;
    public static final int DEXOPT_BOOTCOMPLETE = 8;
    public static final int DEXOPT_DEBUGGABLE = 4;
    public static final int DEXOPT_ENABLE_HIDDEN_API_CHECKS = 1024;
    public static final int DEXOPT_FORCE = 64;
    public static final int DEXOPT_FOR_RESTORE = 8192;
    public static final int DEXOPT_GENERATE_APP_IMAGE = 4096;
    public static final int DEXOPT_GENERATE_COMPACT_DEX = 2048;
    public static final int DEXOPT_IDLE_BACKGROUND_JOB = 512;
    public static final int DEXOPT_PROFILE_GUIDED = 16;
    public static final int DEXOPT_PUBLIC = 2;
    public static final int DEXOPT_SECONDARY_DEX = 32;
    public static final int DEXOPT_STORAGE_CE = 128;
    public static final int DEXOPT_STORAGE_DE = 256;
    public static final int FLAG_CLEAR_APP_DATA_KEEP_ART_PROFILES = 131072;
    public static final int FLAG_CLEAR_CACHE_ONLY = 16;
    public static final int FLAG_CLEAR_CODE_CACHE_ONLY = 32;
    public static final int FLAG_FORCE = 8192;
    public static final int FLAG_FREE_CACHE_DEFY_TARGET_FREE_BYTES = 2048;
    public static final int FLAG_FREE_CACHE_NOOP = 1024;
    public static final int FLAG_FREE_CACHE_V2 = 256;
    public static final int FLAG_FREE_CACHE_V2_DEFY_QUOTA = 512;
    public static final int FLAG_STORAGE_CE = 2;
    public static final int FLAG_STORAGE_DE = 1;
    public static final int FLAG_STORAGE_EXTERNAL = 4;
    public static final int FLAG_STORAGE_SDK = 8;
    public static final int FLAG_USE_QUOTA = 4096;
    public static final int ODEX_IS_PRIVATE = 2;
    public static final int ODEX_IS_PUBLIC = 1;
    public static final int ODEX_NOT_FOUND = 0;
    public static final int PROFILE_ANALYSIS_DONT_OPTIMIZE_EMPTY_PROFILES = 3;
    public static final int PROFILE_ANALYSIS_DONT_OPTIMIZE_SMALL_DELTA = 2;
    public static final int PROFILE_ANALYSIS_OPTIMIZE = 1;
    private static final String TAG = "Installer";
    private volatile boolean mDeferSetFirstBoot;
    private volatile IInstalld mInstalld;
    private volatile CountDownLatch mInstalldLatch;
    private final boolean mIsolated;
    private volatile Object mWarnIfHeld;

    public Installer(Context context) {
        this(context, false);
    }

    public Installer(Context context, boolean isolated) {
        super(context);
        this.mInstalld = null;
        this.mInstalldLatch = new CountDownLatch(1);
        this.mIsolated = isolated;
    }

    public void setWarnIfHeld(Object warnIfHeld) {
        this.mWarnIfHeld = warnIfHeld;
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        if (this.mIsolated) {
            this.mInstalld = null;
            this.mInstalldLatch.countDown();
            return;
        }
        connect();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void connect() {
        IBinder binder = ServiceManager.getService("installd");
        if (binder != null) {
            try {
                binder.linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.pm.Installer$$ExternalSyntheticLambda0
                    @Override // android.os.IBinder.DeathRecipient
                    public final void binderDied() {
                        Installer.this.m5422lambda$connect$0$comandroidserverpmInstaller();
                    }
                }, 0);
            } catch (RemoteException e) {
                binder = null;
            }
        }
        if (binder != null) {
            IInstalld installd = IInstalld.Stub.asInterface(binder);
            this.mInstalld = installd;
            this.mInstalldLatch.countDown();
            try {
                invalidateMounts();
                executeDeferredActions();
                return;
            } catch (InstallerException e2) {
                return;
            }
        }
        Slog.w(TAG, "installd not found; trying again");
        BackgroundThread.getHandler().postDelayed(new Runnable() { // from class: com.android.server.pm.Installer$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                Installer.this.connect();
            }
        }, 1000L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$connect$0$com-android-server-pm-Installer  reason: not valid java name */
    public /* synthetic */ void m5422lambda$connect$0$comandroidserverpmInstaller() {
        Slog.w(TAG, "installd died; reconnecting");
        this.mInstalldLatch = new CountDownLatch(1);
        connect();
    }

    private void executeDeferredActions() throws InstallerException {
        if (this.mDeferSetFirstBoot) {
            setFirstBoot();
        }
    }

    private boolean checkBeforeRemote() throws InstallerException {
        if (this.mWarnIfHeld != null && Thread.holdsLock(this.mWarnIfHeld)) {
            Slog.wtf(TAG, "Calling thread " + Thread.currentThread().getName() + " is holding 0x" + Integer.toHexString(System.identityHashCode(this.mWarnIfHeld)), new Throwable());
        }
        if (this.mIsolated) {
            Slog.i(TAG, "Ignoring request because this installer is isolated");
            return false;
        }
        try {
            if (!this.mInstalldLatch.await(10000L, TimeUnit.MILLISECONDS)) {
                throw new InstallerException("time out waiting for the installer to be ready");
            }
            return true;
        } catch (InterruptedException e) {
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static CreateAppDataArgs buildCreateAppDataArgs(String uuid, String packageName, int userId, int flags, int appId, String seInfo, int targetSdkVersion, boolean usesSdk) {
        CreateAppDataArgs args = new CreateAppDataArgs();
        args.uuid = uuid;
        args.packageName = packageName;
        args.userId = userId;
        args.flags = flags;
        if (usesSdk) {
            args.flags |= 8;
        }
        args.appId = appId;
        args.seInfo = seInfo;
        args.targetSdkVersion = targetSdkVersion;
        return args;
    }

    private static CreateAppDataResult buildPlaceholderCreateAppDataResult() {
        CreateAppDataResult result = new CreateAppDataResult();
        result.ceDataInode = -1L;
        result.exceptionCode = 0;
        result.exceptionMessage = null;
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ReconcileSdkDataArgs buildReconcileSdkDataArgs(String uuid, String packageName, List<String> subDirNames, int userId, int appId, String seInfo, int flags) {
        ReconcileSdkDataArgs args = new ReconcileSdkDataArgs();
        args.uuid = uuid;
        args.packageName = packageName;
        args.subDirNames = subDirNames;
        args.userId = userId;
        args.appId = appId;
        args.previousAppId = 0;
        args.seInfo = seInfo;
        args.flags = flags;
        return args;
    }

    public CreateAppDataResult createAppData(CreateAppDataArgs args) throws InstallerException {
        if (!checkBeforeRemote()) {
            return buildPlaceholderCreateAppDataResult();
        }
        args.previousAppId = 0;
        try {
            return this.mInstalld.createAppData(args);
        } catch (Exception e) {
            throw InstallerException.from(e);
        }
    }

    public CreateAppDataResult[] createAppDataBatched(CreateAppDataArgs[] args) throws InstallerException {
        if (!checkBeforeRemote()) {
            CreateAppDataResult[] results = new CreateAppDataResult[args.length];
            Arrays.fill(results, buildPlaceholderCreateAppDataResult());
            return results;
        }
        for (CreateAppDataArgs arg : args) {
            arg.previousAppId = 0;
        }
        try {
            return this.mInstalld.createAppDataBatched(args);
        } catch (Exception e) {
            throw InstallerException.from(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reconcileSdkData(ReconcileSdkDataArgs args) throws InstallerException {
        if (!checkBeforeRemote()) {
            return;
        }
        try {
            this.mInstalld.reconcileSdkData(args);
        } catch (Exception e) {
            throw InstallerException.from(e);
        }
    }

    public void setFirstBoot() throws InstallerException {
        if (!checkBeforeRemote()) {
            return;
        }
        try {
            if (this.mInstalld != null) {
                this.mInstalld.setFirstBoot();
            } else {
                this.mDeferSetFirstBoot = true;
            }
        } catch (Exception e) {
            throw InstallerException.from(e);
        }
    }

    /* loaded from: classes2.dex */
    public static class Batch {
        private static final int CREATE_APP_DATA_BATCH_SIZE = 256;
        private boolean mExecuted;
        private final List<CreateAppDataArgs> mArgs = new ArrayList();
        private final List<CompletableFuture<Long>> mFutures = new ArrayList();

        public synchronized CompletableFuture<Long> createAppData(CreateAppDataArgs args) {
            CompletableFuture<Long> future;
            if (this.mExecuted) {
                throw new IllegalStateException();
            }
            future = new CompletableFuture<>();
            this.mArgs.add(args);
            this.mFutures.add(future);
            return future;
        }

        public synchronized void execute(Installer installer) throws InstallerException {
            if (this.mExecuted) {
                throw new IllegalStateException();
            }
            this.mExecuted = true;
            int size = this.mArgs.size();
            for (int i = 0; i < size; i += 256) {
                CreateAppDataArgs[] args = new CreateAppDataArgs[Math.min(size - i, 256)];
                for (int j = 0; j < args.length; j++) {
                    args[j] = this.mArgs.get(i + j);
                }
                CreateAppDataResult[] results = installer.createAppDataBatched(args);
                for (int j2 = 0; j2 < args.length; j2++) {
                    CreateAppDataResult result = results[j2];
                    CompletableFuture<Long> future = this.mFutures.get(i + j2);
                    if (result.exceptionCode == 0) {
                        future.complete(Long.valueOf(result.ceDataInode));
                    } else {
                        future.completeExceptionally(new InstallerException(result.exceptionMessage));
                    }
                }
            }
        }
    }

    public void restoreconAppData(String uuid, String packageName, int userId, int flags, int appId, String seInfo) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.restoreconAppData(uuid, packageName, userId, flags, appId, seInfo);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void migrateAppData(String uuid, String packageName, int userId, int flags) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.migrateAppData(uuid, packageName, userId, flags);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void clearAppData(String uuid, String packageName, int userId, int flags, long ceDataInode) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.clearAppData(uuid, packageName, userId, flags, ceDataInode);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void destroyAppData(String uuid, String packageName, int userId, int flags, long ceDataInode) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.destroyAppData(uuid, packageName, userId, flags, ceDataInode);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void fixupAppData(String uuid, int flags) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.fixupAppData(uuid, flags);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void cleanupInvalidPackageDirs(String uuid, int userId, int flags) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.cleanupInvalidPackageDirs(uuid, userId, flags);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void moveCompleteApp(String fromUuid, String toUuid, String packageName, int appId, String seInfo, int targetSdkVersion, String fromCodePath) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.moveCompleteApp(fromUuid, toUuid, packageName, appId, seInfo, targetSdkVersion, fromCodePath);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void getAppSize(String uuid, String[] packageNames, int userId, int flags, int appId, long[] ceDataInodes, String[] codePaths, PackageStats stats) throws InstallerException {
        if (checkBeforeRemote()) {
            if (codePaths != null) {
                for (String codePath : codePaths) {
                    BlockGuard.getVmPolicy().onPathAccess(codePath);
                }
            }
            try {
                long[] res = this.mInstalld.getAppSize(uuid, packageNames, userId, flags, appId, ceDataInodes, codePaths);
                stats.codeSize += res[0];
                stats.dataSize += res[1];
                stats.cacheSize += res[2];
                stats.externalCodeSize += res[3];
                stats.externalDataSize += res[4];
                stats.externalCacheSize += res[5];
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void getUserSize(String uuid, int userId, int flags, int[] appIds, PackageStats stats) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                long[] res = this.mInstalld.getUserSize(uuid, userId, flags, appIds);
                stats.codeSize += res[0];
                stats.dataSize += res[1];
                stats.cacheSize += res[2];
                stats.externalCodeSize += res[3];
                stats.externalDataSize += res[4];
                stats.externalCacheSize += res[5];
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public long[] getExternalSize(String uuid, int userId, int flags, int[] appIds) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                return this.mInstalld.getExternalSize(uuid, userId, flags, appIds);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
        return new long[6];
    }

    public CrateMetadata[] getAppCrates(String uuid, String[] packageNames, int userId) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                return this.mInstalld.getAppCrates(uuid, packageNames, userId);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
        return null;
    }

    public CrateMetadata[] getUserCrates(String uuid, int userId) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                return this.mInstalld.getUserCrates(uuid, userId);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
        return null;
    }

    public void setAppQuota(String uuid, int userId, int appId, long cacheQuota) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.setAppQuota(uuid, userId, appId, cacheQuota);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public boolean dexopt(String apkPath, int uid, String pkgName, String instructionSet, int dexoptNeeded, String outputPath, int dexFlags, String compilerFilter, String volumeUuid, String classLoaderContext, String seInfo, boolean downgrade, int targetSdkVersion, String profileName, String dexMetadataPath, String compilationReason) throws InstallerException {
        assertValidInstructionSet(instructionSet);
        BlockGuard.getVmPolicy().onPathAccess(apkPath);
        BlockGuard.getVmPolicy().onPathAccess(outputPath);
        BlockGuard.getVmPolicy().onPathAccess(dexMetadataPath);
        if (checkBeforeRemote()) {
            try {
                return this.mInstalld.dexopt(apkPath, uid, pkgName, instructionSet, dexoptNeeded, outputPath, dexFlags, compilerFilter, volumeUuid, classLoaderContext, seInfo, downgrade, targetSdkVersion, profileName, dexMetadataPath, compilationReason);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
        return false;
    }

    public void controlDexOptBlocking(boolean block) {
        try {
            this.mInstalld.controlDexOptBlocking(block);
        } catch (Exception e) {
            Slog.w(TAG, "blockDexOpt failed", e);
        }
    }

    public int mergeProfiles(int uid, String packageName, String profileName) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                return this.mInstalld.mergeProfiles(uid, packageName, profileName);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
        return 2;
    }

    public boolean dumpProfiles(int uid, String packageName, String profileName, String codePath, boolean dumpClassesAndMethods) throws InstallerException {
        if (checkBeforeRemote()) {
            BlockGuard.getVmPolicy().onPathAccess(codePath);
            try {
                return this.mInstalld.dumpProfiles(uid, packageName, profileName, codePath, dumpClassesAndMethods);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
        return false;
    }

    public boolean copySystemProfile(String systemProfile, int uid, String packageName, String profileName) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                return this.mInstalld.copySystemProfile(systemProfile, uid, packageName, profileName);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
        return false;
    }

    public void rmdex(String codePath, String instructionSet) throws InstallerException {
        assertValidInstructionSet(instructionSet);
        if (checkBeforeRemote()) {
            BlockGuard.getVmPolicy().onPathAccess(codePath);
            try {
                this.mInstalld.rmdex(codePath, instructionSet);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void rmPackageDir(String packageName, String packageDir) throws InstallerException {
        if (checkBeforeRemote()) {
            BlockGuard.getVmPolicy().onPathAccess(packageDir);
            try {
                this.mInstalld.rmPackageDir(packageName, packageDir);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void clearAppProfiles(String packageName, String profileName) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.clearAppProfiles(packageName, profileName);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void destroyAppProfiles(String packageName) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.destroyAppProfiles(packageName);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void deleteReferenceProfile(String packageName, String profileName) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.deleteReferenceProfile(packageName, profileName);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void createUserData(String uuid, int userId, int userSerial, int flags) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.createUserData(uuid, userId, userSerial, flags);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void destroyUserData(String uuid, int userId, int flags) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.destroyUserData(uuid, userId, flags);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void freeCache(String uuid, long targetFreeBytes, int flags) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.freeCache(uuid, targetFreeBytes, flags);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void linkNativeLibraryDirectory(String uuid, String packageName, String nativeLibPath32, int userId) throws InstallerException {
        if (checkBeforeRemote()) {
            BlockGuard.getVmPolicy().onPathAccess(nativeLibPath32);
            try {
                this.mInstalld.linkNativeLibraryDirectory(uuid, packageName, nativeLibPath32, userId);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void createOatDir(String packageName, String oatDir, String dexInstructionSet) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.createOatDir(packageName, oatDir, dexInstructionSet);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void linkFile(String packageName, String relativePath, String fromBase, String toBase) throws InstallerException {
        if (checkBeforeRemote()) {
            BlockGuard.getVmPolicy().onPathAccess(fromBase);
            BlockGuard.getVmPolicy().onPathAccess(toBase);
            try {
                this.mInstalld.linkFile(packageName, relativePath, fromBase, toBase);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void moveAb(String packageName, String apkPath, String instructionSet, String outputPath) throws InstallerException {
        if (checkBeforeRemote()) {
            BlockGuard.getVmPolicy().onPathAccess(apkPath);
            BlockGuard.getVmPolicy().onPathAccess(outputPath);
            try {
                this.mInstalld.moveAb(packageName, apkPath, instructionSet, outputPath);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public long deleteOdex(String packageName, String apkPath, String instructionSet, String outputPath) throws InstallerException {
        if (checkBeforeRemote()) {
            BlockGuard.getVmPolicy().onPathAccess(apkPath);
            BlockGuard.getVmPolicy().onPathAccess(outputPath);
            try {
                return this.mInstalld.deleteOdex(packageName, apkPath, instructionSet, outputPath);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
        return -1L;
    }

    public boolean reconcileSecondaryDexFile(String apkPath, String packageName, int uid, String[] isas, String volumeUuid, int flags) throws InstallerException {
        for (String str : isas) {
            assertValidInstructionSet(str);
        }
        if (checkBeforeRemote()) {
            BlockGuard.getVmPolicy().onPathAccess(apkPath);
            try {
                return this.mInstalld.reconcileSecondaryDexFile(apkPath, packageName, uid, isas, volumeUuid, flags);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
        return false;
    }

    public byte[] hashSecondaryDexFile(String dexPath, String packageName, int uid, String volumeUuid, int flags) throws InstallerException {
        if (checkBeforeRemote()) {
            BlockGuard.getVmPolicy().onPathAccess(dexPath);
            try {
                return this.mInstalld.hashSecondaryDexFile(dexPath, packageName, uid, volumeUuid, flags);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
        return new byte[0];
    }

    public boolean createProfileSnapshot(int appId, String packageName, String profileName, String classpath) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                return this.mInstalld.createProfileSnapshot(appId, packageName, profileName, classpath);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
        return false;
    }

    public void destroyProfileSnapshot(String packageName, String profileName) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.destroyProfileSnapshot(packageName, profileName);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void invalidateMounts() throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.invalidateMounts();
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public boolean isQuotaSupported(String volumeUuid) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                return this.mInstalld.isQuotaSupported(volumeUuid);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
        return false;
    }

    public void tryMountDataMirror(String volumeUuid) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.tryMountDataMirror(volumeUuid);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public void onPrivateVolumeRemoved(String volumeUuid) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.onPrivateVolumeRemoved(volumeUuid);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
    }

    public boolean prepareAppProfile(String pkg, int userId, int appId, String profileName, String codePath, String dexMetadataPath) throws InstallerException {
        if (checkBeforeRemote()) {
            BlockGuard.getVmPolicy().onPathAccess(codePath);
            BlockGuard.getVmPolicy().onPathAccess(dexMetadataPath);
            try {
                return this.mInstalld.prepareAppProfile(pkg, userId, appId, profileName, codePath, dexMetadataPath);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
        return false;
    }

    public boolean snapshotAppData(String pkg, int userId, int snapshotId, int storageFlags) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.snapshotAppData(null, pkg, userId, snapshotId, storageFlags);
                return true;
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
        return false;
    }

    public boolean restoreAppDataSnapshot(String pkg, int appId, String seInfo, int userId, int snapshotId, int storageFlags) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.restoreAppDataSnapshot(null, pkg, appId, seInfo, userId, snapshotId, storageFlags);
                return true;
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
        return false;
    }

    public boolean destroyAppDataSnapshot(String pkg, int userId, int snapshotId, int storageFlags) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.destroyAppDataSnapshot(null, pkg, userId, 0L, snapshotId, storageFlags);
                return true;
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
        return false;
    }

    public boolean destroyCeSnapshotsNotSpecified(int userId, int[] retainSnapshotIds) throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.destroyCeSnapshotsNotSpecified(null, userId, retainSnapshotIds);
                return true;
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
        return false;
    }

    public boolean migrateLegacyObbData() throws InstallerException {
        if (checkBeforeRemote()) {
            try {
                this.mInstalld.migrateLegacyObbData();
                return true;
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
        return false;
    }

    private static void assertValidInstructionSet(String instructionSet) throws InstallerException {
        String[] strArr;
        for (String abi : Build.SUPPORTED_ABIS) {
            if (VMRuntime.getInstructionSet(abi).equals(instructionSet)) {
                return;
            }
        }
        throw new InstallerException("Invalid instruction set: " + instructionSet);
    }

    public boolean compileLayouts(String apkPath, String packageName, String outDexFile, int uid) {
        try {
            return this.mInstalld.compileLayouts(apkPath, packageName, outDexFile, uid);
        } catch (RemoteException e) {
            return false;
        }
    }

    public int getOdexVisibility(String packageName, String apkPath, String instructionSet, String outputPath) throws InstallerException {
        if (checkBeforeRemote()) {
            BlockGuard.getVmPolicy().onPathAccess(apkPath);
            BlockGuard.getVmPolicy().onPathAccess(outputPath);
            try {
                return this.mInstalld.getOdexVisibility(packageName, apkPath, instructionSet, outputPath);
            } catch (Exception e) {
                throw InstallerException.from(e);
            }
        }
        return -1;
    }

    /* loaded from: classes2.dex */
    public static class InstallerException extends Exception {
        public InstallerException(String detailMessage) {
            super(detailMessage);
        }

        public static InstallerException from(Exception e) throws InstallerException {
            throw new InstallerException(e.toString());
        }
    }
}
