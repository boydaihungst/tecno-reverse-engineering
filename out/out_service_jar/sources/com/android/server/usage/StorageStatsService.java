package com.android.server.usage;

import android.app.AppOpsManager;
import android.app.usage.ExternalStorageStats;
import android.app.usage.IStorageStatsManager;
import android.app.usage.StorageStats;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelableException;
import android.os.StatFs;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.CrateInfo;
import android.os.storage.CrateMetadata;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.DataUnit;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseLongArray;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import com.android.server.IoThread;
import com.android.server.LocalManagerRegistry;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.pm.Installer;
import com.android.server.pm.PackageManagerService;
import com.android.server.storage.CacheQuotaStrategy;
import com.android.server.usage.StorageStatsManagerLocal;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public class StorageStatsService extends IStorageStatsManager.Stub {
    private static final long DEFAULT_QUOTA = DataUnit.MEBIBYTES.toBytes(64);
    private static final long DELAY_CHECK_STORAGE_DELTA = 30000;
    private static final long DELAY_RECALCULATE_QUOTAS = 36000000;
    private static final String PROP_DISABLE_QUOTA = "fw.disable_quota";
    private static final String PROP_STORAGE_CRATES = "fw.storage_crates";
    private static final String PROP_VERIFY_STORAGE = "fw.verify_storage";
    private static final String TAG = "StorageStatsService";
    private final AppOpsManager mAppOps;
    private final ArrayMap<String, SparseLongArray> mCacheQuotas;
    private final Context mContext;
    private final H mHandler;
    private final Installer mInstaller;
    private final PackageManager mPackage;
    private final StorageManager mStorage;
    private final UserManager mUser;
    private final CopyOnWriteArrayList<Pair<String, StorageStatsManagerLocal.StorageStatsAugmenter>> mStorageStatsAugmenters = new CopyOnWriteArrayList<>();
    private int mStorageThresholdPercentHigh = 20;
    private final Object mLock = new Object();

    /* loaded from: classes2.dex */
    public static class Lifecycle extends SystemService {
        private StorageStatsService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.android.server.usage.StorageStatsService$Lifecycle */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r0v0, types: [android.os.IBinder, com.android.server.usage.StorageStatsService] */
        @Override // com.android.server.SystemService
        public void onStart() {
            ?? storageStatsService = new StorageStatsService(getContext());
            this.mService = storageStatsService;
            publishBinderService("storagestats", storageStatsService);
        }
    }

    public StorageStatsService(Context context) {
        Context context2 = (Context) Preconditions.checkNotNull(context);
        this.mContext = context2;
        this.mAppOps = (AppOpsManager) Preconditions.checkNotNull((AppOpsManager) context.getSystemService(AppOpsManager.class));
        this.mUser = (UserManager) Preconditions.checkNotNull((UserManager) context.getSystemService(UserManager.class));
        this.mPackage = (PackageManager) Preconditions.checkNotNull(context.getPackageManager());
        StorageManager storageManager = (StorageManager) Preconditions.checkNotNull((StorageManager) context.getSystemService(StorageManager.class));
        this.mStorage = storageManager;
        this.mCacheQuotas = new ArrayMap<>();
        Installer installer = new Installer(context);
        this.mInstaller = installer;
        installer.onStart();
        invalidateMounts();
        H h = new H(IoThread.get().getLooper());
        this.mHandler = h;
        h.sendEmptyMessage(101);
        storageManager.registerListener(new StorageEventListener() { // from class: com.android.server.usage.StorageStatsService.1
            public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
                switch (vol.type) {
                    case 0:
                    case 1:
                    case 2:
                        if (newState == 2) {
                            StorageStatsService.this.invalidateMounts();
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        });
        LocalManagerRegistry.addManager(StorageStatsManagerLocal.class, new LocalService());
        IntentFilter prFilter = new IntentFilter();
        prFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        prFilter.addAction("android.intent.action.PACKAGE_FULLY_REMOVED");
        prFilter.addDataScheme("package");
        context2.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.usage.StorageStatsService.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context3, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.PACKAGE_REMOVED".equals(action) || "android.intent.action.PACKAGE_FULLY_REMOVED".equals(action)) {
                    StorageStatsService.this.mHandler.removeMessages(103);
                    StorageStatsService.this.mHandler.sendEmptyMessage(103);
                }
            }
        }, prFilter);
        updateConfig();
        DeviceConfig.addOnPropertiesChangedListener("storage_native_boot", context2.getMainExecutor(), new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.usage.StorageStatsService$$ExternalSyntheticLambda3
            public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                StorageStatsService.this.m7317lambda$new$0$comandroidserverusageStorageStatsService(properties);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-usage-StorageStatsService  reason: not valid java name */
    public /* synthetic */ void m7317lambda$new$0$comandroidserverusageStorageStatsService(DeviceConfig.Properties properties) {
        updateConfig();
    }

    private void updateConfig() {
        synchronized (this.mLock) {
            this.mStorageThresholdPercentHigh = DeviceConfig.getInt("storage_native_boot", "storage_threshold_percent_high", 20);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void invalidateMounts() {
        try {
            this.mInstaller.invalidateMounts();
        } catch (Installer.InstallerException e) {
            Slog.wtf(TAG, "Failed to invalidate mounts", e);
        }
    }

    private void enforceStatsPermission(int callingUid, String callingPackage) {
        String errMsg = checkStatsPermission(callingUid, callingPackage, true);
        if (errMsg != null) {
            throw new SecurityException(errMsg);
        }
    }

    private String checkStatsPermission(int callingUid, String callingPackage, boolean noteOp) {
        int mode = noteOp ? this.mAppOps.noteOp(43, callingUid, callingPackage) : this.mAppOps.checkOp(43, callingUid, callingPackage);
        switch (mode) {
            case 0:
                return null;
            case 3:
                if (this.mContext.checkCallingOrSelfPermission("android.permission.PACKAGE_USAGE_STATS") == 0) {
                    return null;
                }
                return "Caller does not have android.permission.PACKAGE_USAGE_STATS; callingPackage=" + callingPackage + ", callingUid=" + callingUid;
            default:
                return "Package " + callingPackage + " from UID " + callingUid + " blocked by mode " + mode;
        }
    }

    public boolean isQuotaSupported(String volumeUuid, String callingPackage) {
        try {
            return this.mInstaller.isQuotaSupported(volumeUuid);
        } catch (Installer.InstallerException e) {
            throw new ParcelableException(new IOException(e.getMessage()));
        }
    }

    public boolean isReservedSupported(String volumeUuid, String callingPackage) {
        if (volumeUuid == StorageManager.UUID_PRIVATE_INTERNAL) {
            return SystemProperties.getBoolean("vold.has_reserved", false) || Build.IS_ARC;
        }
        return false;
    }

    public long getTotalBytes(String volumeUuid, String callingPackage) {
        if (volumeUuid == StorageManager.UUID_PRIVATE_INTERNAL) {
            return FileUtils.roundStorageSize(this.mStorage.getPrimaryStorageSize());
        }
        VolumeInfo vol = this.mStorage.findVolumeByUuid(volumeUuid);
        if (vol == null) {
            throw new ParcelableException(new IOException("Failed to find storage device for UUID " + volumeUuid));
        }
        return FileUtils.roundStorageSize(vol.disk.size);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [299=4] */
    public long getFreeBytes(String volumeUuid, String callingPackage) {
        long token = Binder.clearCallingIdentity();
        try {
            try {
                File path = this.mStorage.findPathForUuid(volumeUuid);
                if (!isQuotaSupported(volumeUuid, PackageManagerService.PLATFORM_PACKAGE_NAME)) {
                    long cacheTotal = path.getUsableSpace();
                    return cacheTotal;
                }
                long cacheTotal2 = getCacheBytes(volumeUuid, PackageManagerService.PLATFORM_PACKAGE_NAME);
                long cacheReserved = this.mStorage.getStorageCacheBytes(path, 0);
                long cacheClearable = Math.max(0L, cacheTotal2 - cacheReserved);
                return path.getUsableSpace() + cacheClearable;
            } catch (FileNotFoundException e) {
                throw new ParcelableException(e);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public long getCacheBytes(String volumeUuid, String callingPackage) {
        enforceStatsPermission(Binder.getCallingUid(), callingPackage);
        long cacheBytes = 0;
        for (UserInfo user : this.mUser.getUsers()) {
            StorageStats stats = queryStatsForUser(volumeUuid, user.id, null);
            cacheBytes += stats.cacheBytes;
        }
        return cacheBytes;
    }

    public long getCacheQuotaBytes(String volumeUuid, int uid, String callingPackage) {
        enforceStatsPermission(Binder.getCallingUid(), callingPackage);
        if (this.mCacheQuotas.containsKey(volumeUuid)) {
            SparseLongArray uidMap = this.mCacheQuotas.get(volumeUuid);
            return uidMap.get(uid, DEFAULT_QUOTA);
        }
        return DEFAULT_QUOTA;
    }

    public StorageStats queryStatsForPackage(String volumeUuid, final String packageName, int userId, String callingPackage) {
        boolean callerHasStatsPermission;
        final boolean callerHasStatsPermission2;
        final PackageStats stats;
        if (userId != UserHandle.getCallingUserId()) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", TAG);
        }
        try {
            ApplicationInfo appInfo = this.mPackage.getApplicationInfoAsUser(packageName, 8192, userId);
            if (Binder.getCallingUid() != appInfo.uid) {
                enforceStatsPermission(Binder.getCallingUid(), callingPackage);
                callerHasStatsPermission = true;
            } else {
                callerHasStatsPermission = checkStatsPermission(Binder.getCallingUid(), callingPackage, false) == null;
            }
            if (ArrayUtils.defeatNullable(this.mPackage.getPackagesForUid(appInfo.uid)).length == 1) {
                return queryStatsForUid(volumeUuid, appInfo.uid, callingPackage);
            }
            int appId = UserHandle.getUserId(appInfo.uid);
            String[] packageNames = {packageName};
            long[] ceDataInodes = new long[1];
            String[] codePaths = new String[0];
            String[] codePaths2 = (!appInfo.isSystemApp() || appInfo.isUpdatedSystemApp()) ? (String[]) ArrayUtils.appendElement(String.class, codePaths, appInfo.getCodePath()) : codePaths;
            PackageStats stats2 = new PackageStats(TAG);
            try {
                callerHasStatsPermission2 = callerHasStatsPermission;
            } catch (Installer.InstallerException e) {
                e = e;
            }
            try {
                this.mInstaller.getAppSize(volumeUuid, packageNames, userId, 0, appId, ceDataInodes, codePaths2, stats2);
                if (volumeUuid != StorageManager.UUID_PRIVATE_INTERNAL) {
                    stats = stats2;
                } else {
                    final UserHandle userHandle = UserHandle.of(userId);
                    stats = stats2;
                    forEachStorageStatsAugmenter(new Consumer() { // from class: com.android.server.usage.StorageStatsService$$ExternalSyntheticLambda0
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((StorageStatsManagerLocal.StorageStatsAugmenter) obj).augmentStatsForPackageForUser(stats, packageName, userHandle, callerHasStatsPermission2);
                        }
                    }, "queryStatsForPackage");
                }
                return translate(stats);
            } catch (Installer.InstallerException e2) {
                e = e2;
                throw new ParcelableException(new IOException(e.getMessage()));
            }
        } catch (PackageManager.NameNotFoundException e3) {
            throw new ParcelableException(e3);
        }
    }

    public StorageStats queryStatsForUid(String volumeUuid, final int uid, String callingPackage) {
        final boolean callerHasStatsPermission;
        PackageStats manualStats;
        final PackageStats stats;
        int userId = UserHandle.getUserId(uid);
        int appId = UserHandle.getAppId(uid);
        if (userId != UserHandle.getCallingUserId()) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", TAG);
        }
        if (Binder.getCallingUid() != uid) {
            enforceStatsPermission(Binder.getCallingUid(), callingPackage);
            callerHasStatsPermission = true;
        } else {
            callerHasStatsPermission = checkStatsPermission(Binder.getCallingUid(), callingPackage, false) == null;
        }
        String[] packageNames = ArrayUtils.defeatNullable(this.mPackage.getPackagesForUid(uid));
        long[] ceDataInodes = new long[packageNames.length];
        String[] codePaths = new String[0];
        String[] sysCodePaths = new String[0];
        String[] sysCodePaths2 = sysCodePaths;
        String[] sysCodePaths3 = codePaths;
        for (String str : packageNames) {
            try {
                ApplicationInfo appInfo = this.mPackage.getApplicationInfoAsUser(str, 8192, userId);
                if (appInfo.isSystemApp() && !appInfo.isUpdatedSystemApp()) {
                    if (appId >= 10000) {
                        sysCodePaths2 = (String[]) ArrayUtils.appendElement(String.class, sysCodePaths2, appInfo.getCodePath());
                    }
                } else {
                    sysCodePaths3 = (String[]) ArrayUtils.appendElement(String.class, sysCodePaths3, appInfo.getCodePath());
                }
            } catch (PackageManager.NameNotFoundException e) {
                throw new ParcelableException(e);
            }
        }
        PackageStats stats2 = new PackageStats(TAG);
        try {
            String[] sysCodePaths4 = sysCodePaths2;
            try {
                this.mInstaller.getAppSize(volumeUuid, packageNames, userId, getDefaultFlags(), appId, ceDataInodes, sysCodePaths3, stats2);
                if (!SystemProperties.getBoolean(PROP_VERIFY_STORAGE, false)) {
                    stats = stats2;
                } else {
                    try {
                        manualStats = new PackageStats(TAG);
                        this.mInstaller.getAppSize(volumeUuid, packageNames, userId, 0, appId, ceDataInodes, sysCodePaths3, manualStats);
                        stats = stats2;
                    } catch (Installer.InstallerException e2) {
                        e = e2;
                    }
                    try {
                        checkEquals("UID " + uid, manualStats, stats);
                    } catch (Installer.InstallerException e3) {
                        e = e3;
                        throw new ParcelableException(new IOException(e.getMessage()));
                    }
                }
                if (volumeUuid == StorageManager.UUID_PRIVATE_INTERNAL) {
                    forEachStorageStatsAugmenter(new Consumer() { // from class: com.android.server.usage.StorageStatsService$$ExternalSyntheticLambda2
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((StorageStatsManagerLocal.StorageStatsAugmenter) obj).augmentStatsForUid(stats, uid, callerHasStatsPermission);
                        }
                    }, "queryStatsForUid");
                }
                StorageStats storageStats = translate(stats);
                if (storageStats != null && storageStats.codeBytes == 0) {
                    return querySystemStatsForUid(volumeUuid, uid, userId, appId, packageNames, sysCodePaths4, ceDataInodes);
                }
                return storageStats;
            } catch (Installer.InstallerException e4) {
                e = e4;
            }
        } catch (Installer.InstallerException e5) {
            e = e5;
        }
    }

    private StorageStats querySystemStatsForUid(String volumeUuid, int uid, int userId, int appId, String[] packageNames, String[] codePaths, long[] ceDataInodes) {
        PackageStats stats = new PackageStats(TAG);
        try {
            this.mInstaller.getAppSize(volumeUuid, packageNames, userId, getDefaultFlags(), appId, ceDataInodes, codePaths, stats);
            if (SystemProperties.getBoolean(PROP_VERIFY_STORAGE, false)) {
                PackageStats manualStats = new PackageStats(TAG);
                this.mInstaller.getAppSize(volumeUuid, packageNames, userId, 0, appId, ceDataInodes, codePaths, manualStats);
                try {
                    checkEquals("UID " + uid, manualStats, stats);
                } catch (Installer.InstallerException e) {
                    e = e;
                    throw new ParcelableException(new IOException(e.getMessage()));
                }
            }
            return translate(stats);
        } catch (Installer.InstallerException e2) {
            e = e2;
        }
    }

    public StorageStats queryStatsForUser(String volumeUuid, int userId, String callingPackage) {
        if (userId != UserHandle.getCallingUserId()) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", TAG);
        }
        enforceStatsPermission(Binder.getCallingUid(), callingPackage);
        int[] appIds = getAppIds(userId);
        final PackageStats stats = new PackageStats(TAG);
        try {
            this.mInstaller.getUserSize(volumeUuid, userId, getDefaultFlags(), appIds, stats);
            if (SystemProperties.getBoolean(PROP_VERIFY_STORAGE, false)) {
                PackageStats manualStats = new PackageStats(TAG);
                this.mInstaller.getUserSize(volumeUuid, userId, 0, appIds, manualStats);
                checkEquals("User " + userId, manualStats, stats);
            }
            if (volumeUuid == StorageManager.UUID_PRIVATE_INTERNAL) {
                final UserHandle userHandle = UserHandle.of(userId);
                forEachStorageStatsAugmenter(new Consumer() { // from class: com.android.server.usage.StorageStatsService$$ExternalSyntheticLambda1
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((StorageStatsManagerLocal.StorageStatsAugmenter) obj).augmentStatsForUser(stats, userHandle);
                    }
                }, "queryStatsForUser");
            }
            return translate(stats);
        } catch (Installer.InstallerException e) {
            throw new ParcelableException(new IOException(e.getMessage()));
        }
    }

    public ExternalStorageStats queryExternalStatsForUser(String volumeUuid, int userId, String callingPackage) {
        if (userId != UserHandle.getCallingUserId()) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", TAG);
        }
        enforceStatsPermission(Binder.getCallingUid(), callingPackage);
        int[] appIds = getAppIds(userId);
        try {
            long[] stats = this.mInstaller.getExternalSize(volumeUuid, userId, getDefaultFlags(), appIds);
            if (SystemProperties.getBoolean(PROP_VERIFY_STORAGE, false)) {
                long[] manualStats = this.mInstaller.getExternalSize(volumeUuid, userId, 0, appIds);
                checkEquals("External " + userId, manualStats, stats);
            }
            ExternalStorageStats res = new ExternalStorageStats();
            res.totalBytes = stats[0];
            res.audioBytes = stats[1];
            res.videoBytes = stats[2];
            res.imageBytes = stats[3];
            res.appBytes = stats[4];
            res.obbBytes = stats[5];
            return res;
        } catch (Installer.InstallerException e) {
            throw new ParcelableException(new IOException(e.getMessage()));
        }
    }

    private int[] getAppIds(int userId) {
        int[] appIds = null;
        for (ApplicationInfo app : this.mPackage.getInstalledApplicationsAsUser(8192, userId)) {
            int appId = UserHandle.getAppId(app.uid);
            if (!ArrayUtils.contains(appIds, appId)) {
                appIds = ArrayUtils.appendInt(appIds, appId);
            }
        }
        return appIds;
    }

    private static int getDefaultFlags() {
        return SystemProperties.getBoolean(PROP_DISABLE_QUOTA, false) ? 0 : 4096;
    }

    private static void checkEquals(String msg, long[] a, long[] b) {
        for (int i = 0; i < a.length; i++) {
            checkEquals(msg + "[" + i + "]", a[i], b[i]);
        }
    }

    private static void checkEquals(String msg, PackageStats a, PackageStats b) {
        checkEquals(msg + " codeSize", a.codeSize, b.codeSize);
        checkEquals(msg + " dataSize", a.dataSize, b.dataSize);
        checkEquals(msg + " cacheSize", a.cacheSize, b.cacheSize);
        checkEquals(msg + " externalCodeSize", a.externalCodeSize, b.externalCodeSize);
        checkEquals(msg + " externalDataSize", a.externalDataSize, b.externalDataSize);
        checkEquals(msg + " externalCacheSize", a.externalCacheSize, b.externalCacheSize);
    }

    private static void checkEquals(String msg, long expected, long actual) {
        if (expected != actual) {
            Slog.e(TAG, msg + " expected " + expected + " actual " + actual);
        }
    }

    private static StorageStats translate(PackageStats stats) {
        StorageStats res = new StorageStats();
        res.codeBytes = stats.codeSize + stats.externalCodeSize;
        res.dataBytes = stats.dataSize + stats.externalDataSize;
        res.cacheBytes = stats.cacheSize + stats.externalCacheSize;
        res.externalCacheBytes = stats.externalCacheSize;
        return res;
    }

    /* loaded from: classes2.dex */
    private class H extends Handler {
        private static final boolean DEBUG = false;
        private static final long MINIMUM_CHANGE_DELTA_PERCENT_HIGH = 5;
        private static final long MINIMUM_CHANGE_DELTA_PERCENT_LOW = 2;
        private static final int MSG_CHECK_STORAGE_DELTA = 100;
        private static final int MSG_LOAD_CACHED_QUOTAS_FROM_FILE = 101;
        private static final int MSG_PACKAGE_REMOVED = 103;
        private static final int MSG_RECALCULATE_QUOTAS = 102;
        private static final int UNSET = -1;
        private long mPreviousBytes;
        private final StatFs mStats;
        private long mTotalBytes;

        public H(Looper looper) {
            super(looper);
            StatFs statFs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
            this.mStats = statFs;
            this.mPreviousBytes = statFs.getAvailableBytes();
            this.mTotalBytes = statFs.getTotalBytes();
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            long bytesDeltaThreshold;
            if (!StorageStatsService.isCacheQuotaCalculationsEnabled(StorageStatsService.this.mContext.getContentResolver())) {
                return;
            }
            switch (msg.what) {
                case 100:
                    this.mStats.restat(Environment.getDataDirectory().getAbsolutePath());
                    long bytesDelta = Math.abs(this.mPreviousBytes - this.mStats.getAvailableBytes());
                    synchronized (StorageStatsService.this.mLock) {
                        if (this.mStats.getAvailableBytes() > (this.mTotalBytes * StorageStatsService.this.mStorageThresholdPercentHigh) / 100) {
                            bytesDeltaThreshold = (this.mTotalBytes * MINIMUM_CHANGE_DELTA_PERCENT_HIGH) / 100;
                        } else {
                            long bytesDeltaThreshold2 = this.mTotalBytes;
                            bytesDeltaThreshold = (bytesDeltaThreshold2 * 2) / 100;
                        }
                    }
                    if (bytesDelta > bytesDeltaThreshold) {
                        this.mPreviousBytes = this.mStats.getAvailableBytes();
                        recalculateQuotas(getInitializedStrategy());
                        StorageStatsService.this.notifySignificantDelta();
                    }
                    sendEmptyMessageDelayed(100, 30000L);
                    return;
                case 101:
                    CacheQuotaStrategy strategy = getInitializedStrategy();
                    this.mPreviousBytes = -1L;
                    try {
                        this.mPreviousBytes = strategy.setupQuotasFromFile();
                    } catch (IOException e) {
                        Slog.e(StorageStatsService.TAG, "An error occurred while reading the cache quota file.", e);
                    } catch (IllegalStateException e2) {
                        Slog.e(StorageStatsService.TAG, "Cache quota XML file is malformed?", e2);
                    }
                    if (this.mPreviousBytes < 0) {
                        this.mStats.restat(Environment.getDataDirectory().getAbsolutePath());
                        this.mPreviousBytes = this.mStats.getAvailableBytes();
                        recalculateQuotas(strategy);
                    }
                    sendEmptyMessageDelayed(100, 30000L);
                    sendEmptyMessageDelayed(102, StorageStatsService.DELAY_RECALCULATE_QUOTAS);
                    return;
                case 102:
                    recalculateQuotas(getInitializedStrategy());
                    sendEmptyMessageDelayed(102, StorageStatsService.DELAY_RECALCULATE_QUOTAS);
                    return;
                case 103:
                    recalculateQuotas(getInitializedStrategy());
                    return;
                default:
                    return;
            }
        }

        private void recalculateQuotas(CacheQuotaStrategy strategy) {
            strategy.recalculateQuotas();
        }

        private CacheQuotaStrategy getInitializedStrategy() {
            UsageStatsManagerInternal usageStatsManager = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
            return new CacheQuotaStrategy(StorageStatsService.this.mContext, usageStatsManager, StorageStatsService.this.mInstaller, StorageStatsService.this.mCacheQuotas);
        }
    }

    static boolean isCacheQuotaCalculationsEnabled(ContentResolver resolver) {
        return Settings.Global.getInt(resolver, "enable_cache_quota_calculation", 1) != 0;
    }

    void notifySignificantDelta() {
        this.mContext.getContentResolver().notifyChange(Uri.parse("content://com.android.externalstorage.documents/"), (ContentObserver) null, false);
    }

    private static void checkCratesEnable() {
        boolean enable = SystemProperties.getBoolean(PROP_STORAGE_CRATES, false);
        if (!enable) {
            throw new IllegalStateException("Storage Crate feature is disabled.");
        }
    }

    private void enforceCratesPermission(int callingUid, String callingPackage) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_CRATES", callingPackage);
    }

    private static List<CrateInfo> convertCrateInfoFrom(CrateMetadata[] crateMetadatas) {
        CrateInfo crateInfo;
        if (ArrayUtils.isEmpty(crateMetadatas)) {
            return Collections.EMPTY_LIST;
        }
        ArrayList<CrateInfo> crateInfos = new ArrayList<>();
        for (CrateMetadata crateMetadata : crateMetadatas) {
            if (crateMetadata != null && !TextUtils.isEmpty(crateMetadata.id) && !TextUtils.isEmpty(crateMetadata.packageName) && (crateInfo = CrateInfo.copyFrom(crateMetadata.uid, crateMetadata.packageName, crateMetadata.id)) != null) {
                crateInfos.add(crateInfo);
            }
        }
        return crateInfos;
    }

    private ParceledListSlice<CrateInfo> getAppCrates(String volumeUuid, String[] packageNames, int userId) {
        try {
            CrateMetadata[] crateMetadatas = this.mInstaller.getAppCrates(volumeUuid, packageNames, userId);
            return new ParceledListSlice<>(convertCrateInfoFrom(crateMetadatas));
        } catch (Installer.InstallerException e) {
            throw new ParcelableException(new IOException(e.getMessage()));
        }
    }

    public ParceledListSlice<CrateInfo> queryCratesForPackage(String volumeUuid, String packageName, int userId, String callingPackage) {
        checkCratesEnable();
        if (userId != UserHandle.getCallingUserId()) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", TAG);
        }
        try {
            ApplicationInfo appInfo = this.mPackage.getApplicationInfoAsUser(packageName, 8192, userId);
            if (Binder.getCallingUid() != appInfo.uid) {
                enforceCratesPermission(Binder.getCallingUid(), callingPackage);
            }
            String[] packageNames = {packageName};
            return getAppCrates(volumeUuid, packageNames, userId);
        } catch (PackageManager.NameNotFoundException e) {
            throw new ParcelableException(e);
        }
    }

    public ParceledListSlice<CrateInfo> queryCratesForUid(String volumeUuid, int uid, String callingPackage) {
        checkCratesEnable();
        int userId = UserHandle.getUserId(uid);
        if (userId != UserHandle.getCallingUserId()) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", TAG);
        }
        if (Binder.getCallingUid() != uid) {
            enforceCratesPermission(Binder.getCallingUid(), callingPackage);
        }
        String[] packageNames = ArrayUtils.defeatNullable(this.mPackage.getPackagesForUid(uid));
        String[] validatedPackageNames = new String[0];
        for (String packageName : packageNames) {
            if (!TextUtils.isEmpty(packageName)) {
                try {
                    ApplicationInfo appInfo = this.mPackage.getApplicationInfoAsUser(packageName, 8192, userId);
                    if (appInfo != null) {
                        validatedPackageNames = (String[]) ArrayUtils.appendElement(String.class, validatedPackageNames, packageName);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    throw new ParcelableException(e);
                }
            }
        }
        return getAppCrates(volumeUuid, validatedPackageNames, userId);
    }

    public ParceledListSlice<CrateInfo> queryCratesForUser(String volumeUuid, int userId, String callingPackage) {
        checkCratesEnable();
        if (userId != UserHandle.getCallingUserId()) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", TAG);
        }
        enforceCratesPermission(Binder.getCallingUid(), callingPackage);
        try {
            CrateMetadata[] crateMetadatas = this.mInstaller.getUserCrates(volumeUuid, userId);
            return new ParceledListSlice<>(convertCrateInfoFrom(crateMetadatas));
        } catch (Installer.InstallerException e) {
            throw new ParcelableException(new IOException(e.getMessage()));
        }
    }

    void forEachStorageStatsAugmenter(Consumer<StorageStatsManagerLocal.StorageStatsAugmenter> consumer, String queryTag) {
        int count = this.mStorageStatsAugmenters.size();
        for (int i = 0; i < count; i++) {
            Pair<String, StorageStatsManagerLocal.StorageStatsAugmenter> pair = this.mStorageStatsAugmenters.get(i);
            String augmenterTag = (String) pair.first;
            StorageStatsManagerLocal.StorageStatsAugmenter storageStatsAugmenter = (StorageStatsManagerLocal.StorageStatsAugmenter) pair.second;
            Trace.traceBegin(524288L, queryTag + ":" + augmenterTag);
            try {
                consumer.accept(storageStatsAugmenter);
                Trace.traceEnd(524288L);
            } catch (Throwable th) {
                Trace.traceEnd(524288L);
                throw th;
            }
        }
    }

    /* loaded from: classes2.dex */
    private class LocalService implements StorageStatsManagerLocal {
        private LocalService() {
        }

        @Override // com.android.server.usage.StorageStatsManagerLocal
        public void registerStorageStatsAugmenter(StorageStatsManagerLocal.StorageStatsAugmenter storageStatsAugmenter, String tag) {
            StorageStatsService.this.mStorageStatsAugmenters.add(Pair.create(tag, storageStatsAugmenter));
        }
    }
}
