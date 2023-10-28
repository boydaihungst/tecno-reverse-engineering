package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityOptions;
import android.app.AnrController;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.admin.SecurityLog;
import android.app.usage.StorageStatsManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.IPackageMoveObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ProviderInfo;
import android.content.pm.UserInfo;
import android.content.res.ObbInfo;
import android.database.ContentObserver;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.DropBoxManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IStoraged;
import android.os.IVold;
import android.os.IVoldListener;
import android.os.IVoldMountCallback;
import android.os.IVoldTaskListener;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.ParcelableException;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.IObbActionListener;
import android.os.storage.IStorageEventListener;
import android.os.storage.IStorageManager;
import android.os.storage.IStorageShutdownObserver;
import android.os.storage.StorageManager;
import android.os.storage.StorageManagerInternal;
import android.os.storage.StorageVolume;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.DataUnit;
import android.util.EventLog;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.app.IAppOpsService;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.AppFuseMount;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.FuseUnavailableMountException;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.HexDump;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.SystemService;
import com.android.server.UiModeManagerService;
import com.android.server.Watchdog;
import com.android.server.am.HostingRecord;
import com.android.server.pm.Installer;
import com.android.server.pm.UserManagerInternal;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.storage.AppFuseBridge;
import com.android.server.storage.StorageSessionController;
import com.android.server.usage.UnixCalendar;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libcore.io.IoUtils;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class StorageManagerService extends IStorageManager.Stub implements Watchdog.Monitor, ActivityTaskManagerInternal.ScreenObserver {
    private static final String ANDROID_VOLD_APP_DATA_ISOLATION_ENABLED_PROPERTY = "persist.sys.vold_app_data_isolation_enabled";
    private static final String ANR_DELAY_MILLIS_DEVICE_CONFIG_KEY = "anr_delay_millis";
    private static final String ANR_DELAY_NOTIFY_EXTERNAL_STORAGE_SERVICE_DEVICE_CONFIG_KEY = "anr_delay_notify_external_storage_service";
    private static final String ATTR_CREATED_MILLIS = "createdMillis";
    private static final String ATTR_FS_UUID = "fsUuid";
    private static final String ATTR_LAST_BENCH_MILLIS = "lastBenchMillis";
    private static final String ATTR_LAST_SEEN_MILLIS = "lastSeenMillis";
    private static final String ATTR_LAST_TRIM_MILLIS = "lastTrimMillis";
    private static final String ATTR_NICKNAME = "nickname";
    private static final String ATTR_PART_GUID = "partGuid";
    private static final String ATTR_PRIMARY_STORAGE_UUID = "primaryStorageUuid";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_USER_FLAGS = "userFlags";
    private static final String ATTR_VERSION = "version";
    private static final boolean DEBUG_EVENTS = false;
    private static final boolean DEBUG_OBB = false;
    private static final boolean DEFAULT_CHARGING_REQUIRED = true;
    private static final float DEFAULT_DIRTY_RECLAIM_RATE = 0.5f;
    private static final int DEFAULT_LIFETIME_PERCENT_THRESHOLD = 70;
    private static final float DEFAULT_LOW_BATTERY_LEVEL = 20.0f;
    private static final int DEFAULT_MIN_SEGMENTS_THRESHOLD = 512;
    private static final float DEFAULT_SEGMENT_RECLAIM_WEIGHT = 1.0f;
    private static final boolean DEFAULT_SMART_IDLE_MAINT_ENABLED = false;
    private static final int DEFAULT_SMART_IDLE_MAINT_PERIOD = 60;
    private static final boolean EMULATE_FBE_SUPPORTED = true;
    public static final int FAILED_MOUNT_RESET_TIMEOUT_SECONDS = 10;
    private static final int H_ABORT_IDLE_MAINT = 12;
    private static final int H_BOOT_COMPLETED = 13;
    private static final int H_CLOUD_MEDIA_PROVIDER_CHANGED = 16;
    private static final int H_COMPLETE_UNLOCK_USER = 14;
    private static final int H_DAEMON_CONNECTED = 2;
    private static final int H_FSTRIM = 4;
    private static final int H_INTERNAL_BROADCAST = 7;
    private static final int H_PARTITION_FORGET = 9;
    private static final int H_RESET = 10;
    private static final int H_RUN_IDLE_MAINT = 11;
    private static final int H_SHUTDOWN = 3;
    private static final int H_SYSTEM_READY = 1;
    private static final int H_VOLUME_BROADCAST = 6;
    private static final int H_VOLUME_MOUNT = 5;
    private static final int H_VOLUME_STATE_CHANGED = 15;
    private static final int H_VOLUME_UNMOUNT = 8;
    private static final String LAST_FSTRIM_FILE = "last-fstrim";
    private static final int MAX_PERIOD_WRITE_RECORD = 4320;
    private static final int MAX_SMART_IDLE_MAINT_PERIOD = 1440;
    private static final int MIN_SMART_IDLE_MAINT_PERIOD = 10;
    private static final int MOVE_STATUS_COPY_FINISHED = 82;
    private static final int OBB_FLUSH_MOUNT_STATE = 2;
    private static final int OBB_RUN_ACTION = 1;
    private static final String TAG_STORAGE_BENCHMARK = "storage_benchmark";
    private static final String TAG_STORAGE_TRIM = "storage_trim";
    private static final String TAG_VOLUME = "volume";
    private static final String TAG_VOLUMES = "volumes";
    private static final int VERSION_ADD_PRIMARY = 2;
    private static final int VERSION_FIX_PRIMARY = 3;
    private static final int VERSION_INIT = 1;
    private static final boolean WATCHDOG_ENABLE = true;
    private static final String ZRAM_ENABLED_PROPERTY = "persist.sys.zram_enabled";
    public static String sMediaStoreAuthorityProcessName;
    private final Callbacks mCallbacks;
    private volatile boolean mChargingRequired;
    protected final Context mContext;
    private volatile float mDirtyReclaimRate;
    protected final Handler mHandler;
    private IAppOpsService mIAppOpsService;
    private IPackageManager mIPackageManager;
    private final Installer mInstaller;
    private long mLastMaintenance;
    private final File mLastMaintenanceFile;
    private volatile int mLifetimePercentThreshold;
    private final LockPatternUtils mLockPatternUtils;
    private volatile float mLowBatteryLevel;
    private volatile int mMaxWriteRecords;
    private volatile int mMinSegmentsThreshold;
    private IPackageMoveObserver mMoveCallback;
    private String mMoveTargetUuid;
    private final ObbActionHandler mObbActionHandler;
    private volatile boolean mPassedLifetimeThresh;
    private PackageManagerInternal mPmInternal;
    private String mPrimaryStorageUuid;
    private final ContentResolver mResolver;
    private volatile float mSegmentReclaimWeight;
    private final AtomicFile mSettingsFile;
    private final StorageSessionController mStorageSessionController;
    private volatile int[] mStorageWriteRecords;
    private volatile IStoraged mStoraged;
    private volatile IVold mVold;
    private final boolean mVoldAppDataIsolationEnabled;
    private final AtomicFile mWriteRecordFile;
    static StorageManagerService sSelf = null;
    private static final String TAG = "StorageManagerService";
    private static final boolean LOCAL_LOGV = Log.isLoggable(TAG, 2);
    private static final String[] ALL_STORAGE_PERMISSIONS = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private static final String[] SYSTEM_APP_AUTHORITY = {"com.transsion.phonemaster"};
    static volatile int sSmartIdleMaintPeriod = 60;
    public static final Pattern KNOWN_APP_DIR_PATHS = Pattern.compile("(?i)(^/storage/[^/]+/(?:([0-9]+)/)?Android/(?:data|media|obb|sandbox)/)([^/]+)(/.*)?");
    private final Set<Integer> mFuseMountedUser = new ArraySet();
    private final Set<Integer> mCeStoragePreparedUsers = new ArraySet();
    private volatile boolean mNeedGC = true;
    protected final Object mLock = LockGuard.installNewLock(4);
    private final Object mPackagesLock = new Object();
    private WatchedLockedUsers mLocalUnlockedUsers = new WatchedLockedUsers();
    private int[] mSystemUnlockedUsers = EmptyArray.INT;
    private ArrayMap<String, DiskInfo> mDisks = new ArrayMap<>();
    protected final ArrayMap<String, VolumeInfo> mVolumes = new ArrayMap<>();
    private ArrayMap<String, VolumeRecord> mRecords = new ArrayMap<>();
    private ArrayMap<String, CountDownLatch> mDiskScanLatches = new ArrayMap<>();
    private volatile ArrayList<Integer> mSystemAppAuthorityAppId = new ArrayList<>();
    private final SparseArray<String> mCloudMediaProviders = new SparseArray<>();
    private volatile int mMediaStoreAuthorityAppId = -1;
    private volatile int mDownloadsAuthorityAppId = -1;
    private volatile int mExternalStorageAuthorityAppId = -1;
    protected volatile int mCurrentUserId = 0;
    private volatile boolean mRemountCurrentUserVolumesOnUnlock = false;
    private final Object mAppFuseLock = new Object();
    private int mNextAppFuseName = 0;
    private AppFuseBridge mAppFuseBridge = null;
    private HashMap<Integer, Integer> mUserSharesMediaWith = new HashMap<>();
    private volatile boolean mBootCompleted = false;
    private volatile boolean mDaemonConnected = false;
    private volatile boolean mSecureKeyguardShowing = true;
    private final Map<IBinder, List<ObbState>> mObbMounts = new HashMap();
    private final Map<String, ObbState> mObbPathToStateMap = new HashMap();
    private final StorageManagerInternalImpl mStorageManagerInternal = new StorageManagerInternalImpl();
    private final Set<Integer> mUidsWithLegacyExternalStorage = new ArraySet();
    private final Map<Integer, PackageMonitor> mPackageMonitorsForUser = new ArrayMap();
    private BroadcastReceiver mUserReceiver = new BroadcastReceiver() { // from class: com.android.server.StorageManagerService.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
            Preconditions.checkArgument(userId >= 0);
            try {
                if ("android.intent.action.USER_ADDED".equals(action)) {
                    UserManager um = (UserManager) StorageManagerService.this.mContext.getSystemService(UserManager.class);
                    int userSerialNumber = um.getUserSerialNumber(userId);
                    StorageManagerService.this.mVold.onUserAdded(userId, userSerialNumber);
                } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                    synchronized (StorageManagerService.this.mVolumes) {
                        int size = StorageManagerService.this.mVolumes.size();
                        for (int i = 0; i < size; i++) {
                            VolumeInfo vol = StorageManagerService.this.mVolumes.valueAt(i);
                            if (vol.mountUserId == userId) {
                                vol.mountUserId = -10000;
                                StorageManagerService.this.mHandler.obtainMessage(8, vol).sendToTarget();
                            }
                        }
                    }
                    StorageManagerService.this.mVold.onUserRemoved(userId);
                }
            } catch (Exception e) {
                Slog.wtf(StorageManagerService.TAG, e);
            }
        }
    };
    private final IVoldListener mListener = new IVoldListener.Stub() { // from class: com.android.server.StorageManagerService.3
        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.os.IVoldListener
        public void onDiskCreated(String diskId, int flags) {
            synchronized (StorageManagerService.this.mLock) {
                String value = SystemProperties.get("persist.sys.adoptable");
                char c = 65535;
                switch (value.hashCode()) {
                    case 464944051:
                        if (value.equals("force_on")) {
                            c = 0;
                            break;
                        }
                        break;
                    case 1528363547:
                        if (value.equals("force_off")) {
                            c = 1;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        flags |= 1;
                        break;
                    case 1:
                        flags &= -2;
                        break;
                }
                StorageManagerService.this.mDisks.put(diskId, new DiskInfo(diskId, flags));
            }
        }

        @Override // android.os.IVoldListener
        public void onDiskScanned(String diskId) {
            synchronized (StorageManagerService.this.mLock) {
                DiskInfo disk = (DiskInfo) StorageManagerService.this.mDisks.get(diskId);
                if (disk != null) {
                    StorageManagerService.this.onDiskScannedLocked(disk);
                }
            }
        }

        @Override // android.os.IVoldListener
        public void onDiskMetadataChanged(String diskId, long sizeBytes, String label, String sysPath) {
            synchronized (StorageManagerService.this.mLock) {
                DiskInfo disk = (DiskInfo) StorageManagerService.this.mDisks.get(diskId);
                if (disk != null) {
                    disk.size = sizeBytes;
                    disk.label = label;
                    disk.sysPath = sysPath;
                }
            }
        }

        @Override // android.os.IVoldListener
        public void onDiskDestroyed(String diskId) {
            synchronized (StorageManagerService.this.mLock) {
                DiskInfo disk = (DiskInfo) StorageManagerService.this.mDisks.remove(diskId);
                if (disk != null) {
                    StorageManagerService.this.mCallbacks.notifyDiskDestroyed(disk);
                }
            }
        }

        @Override // android.os.IVoldListener
        public void onVolumeCreated(String volId, int type, String diskId, String partGuid, int userId) {
            synchronized (StorageManagerService.this.mLock) {
                DiskInfo disk = (DiskInfo) StorageManagerService.this.mDisks.get(diskId);
                VolumeInfo vol = new VolumeInfo(volId, type, disk, partGuid);
                vol.mountUserId = userId;
                StorageManagerService.this.mVolumes.put(volId, vol);
                StorageManagerService.this.onVolumeCreatedLocked(vol);
            }
        }

        @Override // android.os.IVoldListener
        public void onVolumeStateChanged(String volId, int newState) {
            synchronized (StorageManagerService.this.mLock) {
                VolumeInfo vol = StorageManagerService.this.mVolumes.get(volId);
                if (vol != null) {
                    int oldState = vol.state;
                    vol.state = newState;
                    VolumeInfo vInfo = new VolumeInfo(vol);
                    SomeArgs args = SomeArgs.obtain();
                    args.arg1 = vInfo;
                    args.argi1 = oldState;
                    args.argi2 = newState;
                    StorageManagerService.this.mHandler.obtainMessage(15, args).sendToTarget();
                    StorageManagerService.this.onVolumeStateChangedLocked(vInfo, oldState, newState);
                }
            }
        }

        @Override // android.os.IVoldListener
        public void onVolumeMetadataChanged(String volId, String fsType, String fsUuid, String fsLabel) {
            synchronized (StorageManagerService.this.mLock) {
                VolumeInfo vol = StorageManagerService.this.mVolumes.get(volId);
                if (vol != null) {
                    vol.fsType = fsType;
                    vol.fsUuid = fsUuid;
                    vol.fsLabel = fsLabel;
                }
            }
        }

        @Override // android.os.IVoldListener
        public void onVolumePathChanged(String volId, String path) {
            synchronized (StorageManagerService.this.mLock) {
                VolumeInfo vol = StorageManagerService.this.mVolumes.get(volId);
                if (vol != null) {
                    vol.path = path;
                }
            }
        }

        @Override // android.os.IVoldListener
        public void onVolumeInternalPathChanged(String volId, String internalPath) {
            synchronized (StorageManagerService.this.mLock) {
                VolumeInfo vol = StorageManagerService.this.mVolumes.get(volId);
                if (vol != null) {
                    vol.internalPath = internalPath;
                }
            }
        }

        @Override // android.os.IVoldListener
        public void onVolumeDestroyed(String volId) {
            VolumeInfo vol;
            synchronized (StorageManagerService.this.mLock) {
                vol = StorageManagerService.this.mVolumes.remove(volId);
            }
            if (vol != null) {
                StorageManagerService.this.mStorageSessionController.onVolumeRemove(vol);
                try {
                    if (vol.type == 1) {
                        StorageManagerService.this.mInstaller.onPrivateVolumeRemoved(vol.getFsUuid());
                    }
                } catch (Installer.InstallerException e) {
                    Slog.i(StorageManagerService.TAG, "Failed when private volume unmounted " + vol, e);
                }
            }
        }
    };

    /* loaded from: classes.dex */
    public static class Lifecycle extends SystemService {
        protected StorageManagerService mStorageManagerService;

        public Lifecycle(Context context) {
            super(context);
        }

        /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.android.server.StorageManagerService$Lifecycle */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.StorageManagerService, android.os.IBinder] */
        @Override // com.android.server.SystemService
        public void onStart() {
            ?? storageManagerService = new StorageManagerService(getContext());
            this.mStorageManagerService = storageManagerService;
            publishBinderService("mount", storageManagerService);
            this.mStorageManagerService.start();
        }

        @Override // com.android.server.SystemService
        public void onBootPhase(int phase) {
            if (phase == 500) {
                this.mStorageManagerService.servicesReady();
            } else if (phase == 550) {
                this.mStorageManagerService.systemReady();
            } else if (phase == 1000) {
                this.mStorageManagerService.bootCompleted();
            }
        }

        @Override // com.android.server.SystemService
        public void onUserSwitching(SystemService.TargetUser from, SystemService.TargetUser to) {
            int currentUserId = to.getUserIdentifier();
            this.mStorageManagerService.mCurrentUserId = currentUserId;
            UserManagerInternal umInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
            if (umInternal.isUserUnlocked(currentUserId)) {
                Slog.d(StorageManagerService.TAG, "Attempt remount volumes for user: " + currentUserId);
                this.mStorageManagerService.maybeRemountVolumes(currentUserId);
                this.mStorageManagerService.mRemountCurrentUserVolumesOnUnlock = false;
                return;
            }
            Slog.d(StorageManagerService.TAG, "Attempt remount volumes for user: " + currentUserId + " on unlock");
            this.mStorageManagerService.mRemountCurrentUserVolumesOnUnlock = true;
        }

        @Override // com.android.server.SystemService
        public void onUserUnlocking(SystemService.TargetUser user) {
            this.mStorageManagerService.onUnlockUser(user.getUserIdentifier());
        }

        @Override // com.android.server.SystemService
        public void onUserStopped(SystemService.TargetUser user) {
            this.mStorageManagerService.onCleanupUser(user.getUserIdentifier());
        }

        @Override // com.android.server.SystemService
        public void onUserStopping(SystemService.TargetUser user) {
            this.mStorageManagerService.onStopUser(user.getUserIdentifier());
        }

        @Override // com.android.server.SystemService
        public void onUserStarting(SystemService.TargetUser user) {
            this.mStorageManagerService.snapshotAndMonitorLegacyStorageAppOp(user.getUserHandle());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class WatchedLockedUsers {
        private int[] users = EmptyArray.INT;

        public WatchedLockedUsers() {
            invalidateIsUserUnlockedCache();
        }

        public void append(int userId) {
            this.users = ArrayUtils.appendInt(this.users, userId);
            invalidateIsUserUnlockedCache();
        }

        public void appendAll(int[] userIds) {
            for (int userId : userIds) {
                this.users = ArrayUtils.appendInt(this.users, userId);
            }
            invalidateIsUserUnlockedCache();
        }

        public void remove(int userId) {
            this.users = ArrayUtils.removeInt(this.users, userId);
            invalidateIsUserUnlockedCache();
        }

        public boolean contains(int userId) {
            return ArrayUtils.contains(this.users, userId);
        }

        public int[] all() {
            return this.users;
        }

        public String toString() {
            return Arrays.toString(this.users);
        }

        private void invalidateIsUserUnlockedCache() {
            UserManager.invalidateIsUserUnlockedCache();
        }
    }

    private VolumeInfo findVolumeByIdOrThrow(String id) {
        synchronized (this.mLock) {
            VolumeInfo vol = this.mVolumes.get(id);
            if (vol != null) {
                return vol;
            }
            throw new IllegalArgumentException("No volume found for ID " + id);
        }
    }

    private String findVolumeIdForPathOrThrow(String path) {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mVolumes.size(); i++) {
                VolumeInfo vol = this.mVolumes.valueAt(i);
                if (vol.path != null && path.startsWith(vol.path)) {
                    return vol.id;
                }
            }
            throw new IllegalArgumentException("No volume found for path " + path);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public VolumeRecord findRecordForPath(String path) {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mVolumes.size(); i++) {
                VolumeInfo vol = this.mVolumes.valueAt(i);
                if (vol.path != null && path.startsWith(vol.path)) {
                    return this.mRecords.get(vol.fsUuid);
                }
            }
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String scrubPath(String path) {
        if (path.startsWith(Environment.getDataDirectory().getAbsolutePath())) {
            return "internal";
        }
        VolumeRecord rec = findRecordForPath(path);
        if (rec == null || rec.createdMillis == 0) {
            return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
        }
        return "ext:" + ((int) ((System.currentTimeMillis() - rec.createdMillis) / UnixCalendar.WEEK_IN_MILLIS)) + "w";
    }

    private VolumeInfo findStorageForUuidAsUser(String volumeUuid, int userId) {
        StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        if (Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, volumeUuid)) {
            return storage.findVolumeById("emulated;" + userId);
        }
        if (Objects.equals("primary_physical", volumeUuid)) {
            return storage.getPrimaryPhysicalVolume();
        }
        VolumeInfo info = storage.findVolumeByUuid(volumeUuid);
        if (info == null) {
            Slog.w(TAG, "findStorageForUuidAsUser cannot find volumeUuid:" + volumeUuid);
            return null;
        }
        String emulatedUuid = info.getId().replace("private", "emulated") + ";" + userId;
        return storage.findVolumeById(emulatedUuid);
    }

    private boolean shouldBenchmark() {
        long benchInterval = Settings.Global.getLong(this.mContext.getContentResolver(), "storage_benchmark_interval", UnixCalendar.WEEK_IN_MILLIS);
        if (benchInterval == -1) {
            return false;
        }
        if (benchInterval == 0) {
            return true;
        }
        synchronized (this.mLock) {
            for (int i = 0; i < this.mVolumes.size(); i++) {
                VolumeInfo vol = this.mVolumes.valueAt(i);
                VolumeRecord rec = this.mRecords.get(vol.fsUuid);
                if (vol.isMountedWritable() && rec != null) {
                    long benchAge = System.currentTimeMillis() - rec.lastBenchMillis;
                    if (benchAge >= benchInterval) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private CountDownLatch findOrCreateDiskScanLatch(String diskId) {
        CountDownLatch latch;
        synchronized (this.mLock) {
            latch = this.mDiskScanLatches.get(diskId);
            if (latch == null) {
                latch = new CountDownLatch(1);
                this.mDiskScanLatches.put(diskId, latch);
            }
        }
        return latch;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ObbState implements IBinder.DeathRecipient {
        final String canonicalPath;
        final int nonce;
        final int ownerGid;
        final String rawPath;
        final IObbActionListener token;
        String volId;

        public ObbState(String rawPath, String canonicalPath, int callingUid, IObbActionListener token, int nonce, String volId) {
            this.rawPath = rawPath;
            this.canonicalPath = canonicalPath;
            this.ownerGid = UserHandle.getSharedAppGid(callingUid);
            this.token = token;
            this.nonce = nonce;
            this.volId = volId;
        }

        public IBinder getBinder() {
            return this.token.asBinder();
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            ObbAction action = new UnmountObbAction(this, true);
            StorageManagerService.this.mObbActionHandler.sendMessage(StorageManagerService.this.mObbActionHandler.obtainMessage(1, action));
        }

        public void link() throws RemoteException {
            getBinder().linkToDeath(this, 0);
        }

        public void unlink() {
            getBinder().unlinkToDeath(this, 0);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("ObbState{");
            sb.append("rawPath=").append(this.rawPath);
            sb.append(",canonicalPath=").append(this.canonicalPath);
            sb.append(",ownerGid=").append(this.ownerGid);
            sb.append(",token=").append(this.token);
            sb.append(",binder=").append(getBinder());
            sb.append(",volId=").append(this.volId);
            sb.append('}');
            return sb.toString();
        }
    }

    /* loaded from: classes.dex */
    class StorageManagerServiceHandler extends Handler {
        public StorageManagerServiceHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    StorageManagerService.this.handleSystemReady();
                    return;
                case 2:
                    StorageManagerService.this.handleDaemonConnected();
                    return;
                case 3:
                    IStorageShutdownObserver obs = (IStorageShutdownObserver) msg.obj;
                    boolean success = false;
                    try {
                        StorageManagerService.this.mVold.shutdown();
                        success = true;
                    } catch (Exception e) {
                        Slog.wtf(StorageManagerService.TAG, e);
                    }
                    if (obs != null) {
                        try {
                            obs.onShutDownComplete(success ? 0 : -1);
                            return;
                        } catch (Exception e2) {
                            return;
                        }
                    }
                    return;
                case 4:
                    Slog.i(StorageManagerService.TAG, "Running fstrim idle maintenance");
                    try {
                        StorageManagerService.this.mLastMaintenance = System.currentTimeMillis();
                        StorageManagerService.this.mLastMaintenanceFile.setLastModified(StorageManagerService.this.mLastMaintenance);
                    } catch (Exception e3) {
                        Slog.e(StorageManagerService.TAG, "Unable to record last fstrim!");
                    }
                    StorageManagerService.this.fstrim(0, null);
                    Runnable callback = (Runnable) msg.obj;
                    if (callback != null) {
                        callback.run();
                        return;
                    }
                    return;
                case 5:
                    VolumeInfo vol = (VolumeInfo) msg.obj;
                    if (StorageManagerService.this.isMountDisallowed(vol)) {
                        Slog.i(StorageManagerService.TAG, "Ignoring mount " + vol.getId() + " due to policy");
                        return;
                    } else {
                        StorageManagerService.this.mount(vol);
                        return;
                    }
                case 6:
                    StorageVolume userVol = (StorageVolume) msg.obj;
                    String envState = userVol.getState();
                    Slog.d(StorageManagerService.TAG, "Volume " + userVol.getId() + " broadcasting " + envState + " to " + userVol.getOwner());
                    String action = VolumeInfo.getBroadcastForEnvironment(envState);
                    if (action != null) {
                        Intent intent = new Intent(action, Uri.fromFile(userVol.getPathFile()));
                        intent.putExtra("android.os.storage.extra.STORAGE_VOLUME", userVol);
                        intent.addFlags(AudioFormat.HE_AAC_V1);
                        StorageManagerService.this.mContext.sendBroadcastAsUser(intent, userVol.getOwner());
                        return;
                    }
                    return;
                case 7:
                    StorageManagerService.this.mContext.sendBroadcastAsUser((Intent) msg.obj, UserHandle.ALL, "android.permission.WRITE_MEDIA_STORAGE");
                    return;
                case 8:
                    StorageManagerService.this.unmount((VolumeInfo) msg.obj);
                    return;
                case 9:
                    VolumeRecord rec = (VolumeRecord) msg.obj;
                    StorageManagerService.this.forgetPartition(rec.partGuid, rec.fsUuid);
                    return;
                case 10:
                    StorageManagerService.this.resetIfBootedAndConnected();
                    return;
                case 11:
                    Slog.i(StorageManagerService.TAG, "Running idle maintenance");
                    StorageManagerService.this.runIdleMaint((Runnable) msg.obj);
                    return;
                case 12:
                    Slog.i(StorageManagerService.TAG, "Aborting idle maintenance");
                    StorageManagerService.this.abortIdleMaint((Runnable) msg.obj);
                    return;
                case 13:
                    StorageManagerService.this.handleBootCompleted();
                    return;
                case 14:
                    StorageManagerService.this.completeUnlockUser(msg.arg1);
                    return;
                case 15:
                    SomeArgs args = (SomeArgs) msg.obj;
                    StorageManagerService.this.onVolumeStateChangedAsync((VolumeInfo) args.arg1, args.argi1, args.argi2);
                    args.recycle();
                    return;
                case 16:
                    Object listener = msg.obj;
                    if (listener instanceof StorageManagerInternal.CloudProviderChangeListener) {
                        StorageManagerService.this.notifyCloudMediaProviderChangedAsync((StorageManagerInternal.CloudProviderChangeListener) listener);
                        return;
                    } else {
                        StorageManagerService.this.onCloudMediaProviderChangedAsync(msg.arg1);
                        return;
                    }
                default:
                    return;
            }
        }
    }

    private void waitForLatch(CountDownLatch latch, String condition, long timeoutMillis) throws TimeoutException {
        long startMillis = SystemClock.elapsedRealtime();
        while (!latch.await(5000L, TimeUnit.MILLISECONDS)) {
            try {
                Slog.w(TAG, "Thread " + Thread.currentThread().getName() + " still waiting for " + condition + "...");
            } catch (InterruptedException e) {
                Slog.w(TAG, "Interrupt while waiting for " + condition);
            }
            if (timeoutMillis > 0 && SystemClock.elapsedRealtime() > startMillis + timeoutMillis) {
                throw new TimeoutException("Thread " + Thread.currentThread().getName() + " gave up waiting for " + condition + " after " + timeoutMillis + "ms");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSystemReady() {
        if (prepareSmartIdleMaint()) {
            SmartStorageMaintIdler.scheduleSmartIdlePass(this.mContext, sSmartIdleMaintPeriod);
        }
        MountServiceIdler.scheduleIdlePass(this.mContext);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("zram_enabled"), false, new ContentObserver(null) { // from class: com.android.server.StorageManagerService.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                StorageManagerService.this.refreshZramSettings();
            }
        });
        refreshZramSettings();
        String zramPropValue = SystemProperties.get(ZRAM_ENABLED_PROPERTY);
        if (!zramPropValue.equals("0") && this.mContext.getResources().getBoolean(17891844)) {
            ZramWriteback.scheduleZramWriteback(this.mContext);
        }
        configureTranscoding();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshZramSettings() {
        String propertyValue = SystemProperties.get(ZRAM_ENABLED_PROPERTY);
        if ("".equals(propertyValue)) {
            return;
        }
        String desiredPropertyValue = Settings.Global.getInt(this.mContext.getContentResolver(), "zram_enabled", 1) != 0 ? "1" : "0";
        if (!desiredPropertyValue.equals(propertyValue)) {
            SystemProperties.set(ZRAM_ENABLED_PROPERTY, desiredPropertyValue);
            if (desiredPropertyValue.equals("1") && this.mContext.getResources().getBoolean(17891844)) {
                ZramWriteback.scheduleZramWriteback(this.mContext);
            }
        }
    }

    private void configureTranscoding() {
        boolean transcodeEnabled = SystemProperties.getBoolean("persist.sys.fuse.transcode_user_control", false) ? SystemProperties.getBoolean("persist.sys.fuse.transcode_enabled", true) : DeviceConfig.getBoolean("storage_native_boot", "transcode_enabled", true);
        SystemProperties.set("sys.fuse.transcode_enabled", String.valueOf(transcodeEnabled));
        if (transcodeEnabled) {
            ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).registerAnrController(new ExternalStorageServiceAnrController());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ExternalStorageServiceAnrController implements AnrController {
        private ExternalStorageServiceAnrController() {
        }

        public long getAnrDelayMillis(String packageName, int uid) {
            if (!StorageManagerService.this.isAppIoBlocked(uid)) {
                return 0L;
            }
            int delay = DeviceConfig.getInt("storage_native_boot", StorageManagerService.ANR_DELAY_MILLIS_DEVICE_CONFIG_KEY, 5000);
            Slog.v(StorageManagerService.TAG, "getAnrDelayMillis for " + packageName + ". " + delay + "ms");
            return delay;
        }

        public void onAnrDelayStarted(String packageName, int uid) {
            if (!StorageManagerService.this.isAppIoBlocked(uid)) {
                return;
            }
            boolean notifyExternalStorageService = DeviceConfig.getBoolean("storage_native_boot", StorageManagerService.ANR_DELAY_NOTIFY_EXTERNAL_STORAGE_SERVICE_DEVICE_CONFIG_KEY, true);
            if (notifyExternalStorageService) {
                Slog.d(StorageManagerService.TAG, "onAnrDelayStarted for " + packageName + ". Notifying external storage service");
                try {
                    StorageManagerService.this.mStorageSessionController.notifyAnrDelayStarted(packageName, uid, 0, 1);
                } catch (StorageSessionController.ExternalStorageServiceException e) {
                    Slog.e(StorageManagerService.TAG, "Failed to notify ANR delay started for " + packageName, e);
                }
            }
        }

        public boolean onAnrDelayCompleted(String packageName, int uid) {
            if (StorageManagerService.this.isAppIoBlocked(uid)) {
                Slog.d(StorageManagerService.TAG, "onAnrDelayCompleted for " + packageName + ". Showing ANR dialog...");
                return true;
            }
            Slog.d(StorageManagerService.TAG, "onAnrDelayCompleted for " + packageName + ". Skipping ANR dialog...");
            return false;
        }
    }

    @Deprecated
    private void killMediaProvider(List<UserInfo> users) {
        ProviderInfo provider;
        if (users == null) {
            return;
        }
        long token = Binder.clearCallingIdentity();
        try {
            for (UserInfo user : users) {
                if (!user.isSystemOnly() && (provider = this.mPmInternal.resolveContentProvider("media", 786432L, user.id, 1000)) != null) {
                    IActivityManager am = ActivityManager.getService();
                    try {
                        am.killApplication(provider.applicationInfo.packageName, UserHandle.getAppId(provider.applicationInfo.uid), -1, "vold reset");
                        break;
                    } catch (RemoteException e) {
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void addInternalVolumeLocked() {
        VolumeInfo internal = new VolumeInfo("private", 1, (DiskInfo) null, (String) null);
        internal.state = 2;
        internal.path = Environment.getDataDirectory().getAbsolutePath();
        this.mVolumes.put(internal.id, internal);
    }

    private void initIfBootedAndConnected() {
        Slog.d(TAG, "Thinking about init, mBootCompleted=" + this.mBootCompleted + ", mDaemonConnected=" + this.mDaemonConnected);
        if (this.mBootCompleted && this.mDaemonConnected && !StorageManager.isFileEncryptedNativeOnly()) {
            boolean initLocked = StorageManager.isFileEncryptedEmulatedOnly();
            Slog.d(TAG, "Setting up emulation state, initlocked=" + initLocked);
            List<UserInfo> users = ((UserManager) this.mContext.getSystemService(UserManager.class)).getUsers();
            for (UserInfo user : users) {
                if (initLocked) {
                    try {
                        this.mVold.lockUserKey(user.id);
                    } catch (Exception e) {
                        Slog.wtf(TAG, e);
                    }
                } else {
                    this.mVold.unlockUserKey(user.id, user.serialNumber, encodeBytes(null));
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetIfBootedAndConnected() {
        int[] systemUnlockedUsers;
        Slog.d(TAG, "Thinking about reset, mBootCompleted=" + this.mBootCompleted + ", mDaemonConnected=" + this.mDaemonConnected);
        if (this.mBootCompleted && this.mDaemonConnected) {
            UserManager userManager = (UserManager) this.mContext.getSystemService(UserManager.class);
            List<UserInfo> users = userManager.getUsers();
            this.mStorageSessionController.onReset(this.mVold, new Runnable() { // from class: com.android.server.StorageManagerService$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    StorageManagerService.this.m415xca3a5c9();
                }
            });
            ArrayList<VolumeInfo> privateVolumes = new ArrayList<>();
            synchronized (this.mLock) {
                for (int i = 0; i < this.mVolumes.size(); i++) {
                    VolumeInfo vol = this.mVolumes.valueAt(i);
                    if (vol.type == 1 && vol.getFsUuid() != null) {
                        privateVolumes.add(vol);
                    }
                }
            }
            Iterator<VolumeInfo> it = privateVolumes.iterator();
            while (it.hasNext()) {
                VolumeInfo vol2 = it.next();
                Slog.i(TAG, "resetIfBootedAndConnected remove internal storage in /data_mirror/ " + vol2.getFsUuid());
                try {
                    this.mInstaller.onPrivateVolumeRemoved(vol2.getFsUuid());
                } catch (Installer.InstallerException e) {
                    Slog.e(TAG, "Failed unmount mirror data", e);
                }
            }
            synchronized (this.mLock) {
                int[] iArr = this.mSystemUnlockedUsers;
                systemUnlockedUsers = Arrays.copyOf(iArr, iArr.length);
                this.mDisks.clear();
                this.mVolumes.clear();
                addInternalVolumeLocked();
            }
            try {
                Slog.i(TAG, "Resetting vold...");
                this.mVold.reset();
                Slog.i(TAG, "Reset vold");
                for (UserInfo user : users) {
                    this.mVold.onUserAdded(user.id, user.serialNumber);
                }
                for (int userId : systemUnlockedUsers) {
                    this.mVold.onUserStarted(userId);
                    this.mStoraged.onUserStarted(userId);
                }
                restoreSystemUnlockedUsers(userManager, users, systemUnlockedUsers);
                this.mVold.onSecureKeyguardStateChanged(this.mSecureKeyguardShowing);
                this.mStorageManagerInternal.onReset(this.mVold);
            } catch (Exception e2) {
                Slog.wtf(TAG, e2);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$resetIfBootedAndConnected$0$com-android-server-StorageManagerService  reason: not valid java name */
    public /* synthetic */ void m415xca3a5c9() {
        this.mHandler.removeCallbacksAndMessages(null);
    }

    private void restoreSystemUnlockedUsers(UserManager userManager, List<UserInfo> allUsers, int[] systemUnlockedUsers) throws Exception {
        Arrays.sort(systemUnlockedUsers);
        UserManager.invalidateIsUserUnlockedCache();
        for (UserInfo user : allUsers) {
            int userId = user.id;
            if (userManager.isUserRunning(userId) && Arrays.binarySearch(systemUnlockedUsers, userId) < 0) {
                boolean unlockingOrUnlocked = userManager.isUserUnlockingOrUnlocked(userId);
                if (unlockingOrUnlocked) {
                    Slog.w(TAG, "UNLOCK_USER lost from vold reset, will retry, user:" + userId);
                    this.mVold.onUserStarted(userId);
                    this.mStoraged.onUserStarted(userId);
                    this.mHandler.obtainMessage(14, userId, 0).sendToTarget();
                }
            }
        }
    }

    private void restoreLocalUnlockedUsers() {
        try {
            int[] userIds = this.mVold.getUnlockedUsers();
            if (!ArrayUtils.isEmpty(userIds)) {
                Slog.d(TAG, "CE storage for users " + Arrays.toString(userIds) + " is already unlocked");
                synchronized (this.mLock) {
                    this.mLocalUnlockedUsers.appendAll(userIds);
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to get unlocked users from vold", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUnlockUser(int userId) {
        Slog.d(TAG, "onUnlockUser " + userId);
        if (userId != 0) {
            try {
                Context userContext = this.mContext.createPackageContextAsUser(HostingRecord.HOSTING_TYPE_SYSTEM, 0, UserHandle.of(userId));
                UserManager um = (UserManager) userContext.getSystemService(UserManager.class);
                if (um != null && um.isMediaSharedWithParent()) {
                    int parentUserId = um.getProfileParent(userId).id;
                    this.mUserSharesMediaWith.put(Integer.valueOf(userId), Integer.valueOf(parentUserId));
                    this.mUserSharesMediaWith.put(Integer.valueOf(parentUserId), Integer.valueOf(userId));
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Failed to create user context for user " + userId);
            }
        }
        try {
            this.mStorageSessionController.onUnlockUser(userId);
            this.mVold.onUserStarted(userId);
            this.mStoraged.onUserStarted(userId);
        } catch (Exception e2) {
            Slog.wtf(TAG, e2);
        }
        this.mHandler.obtainMessage(14, userId, 0).sendToTarget();
        if (this.mRemountCurrentUserVolumesOnUnlock && userId == this.mCurrentUserId) {
            maybeRemountVolumes(userId);
            this.mRemountCurrentUserVolumesOnUnlock = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void completeUnlockUser(int userId) {
        int[] iArr;
        onKeyguardStateChanged(false);
        synchronized (this.mLock) {
            for (int unlockedUser : this.mSystemUnlockedUsers) {
                if (unlockedUser == userId) {
                    Log.i(TAG, "completeUnlockUser called for already unlocked user:" + userId);
                    return;
                }
            }
            for (int i = 0; i < this.mVolumes.size(); i++) {
                VolumeInfo vol = this.mVolumes.valueAt(i);
                if (vol.isVisibleForUser(userId) && vol.isMountedReadable()) {
                    StorageVolume userVol = vol.buildStorageVolume(this.mContext, userId, false);
                    this.mHandler.obtainMessage(6, userVol).sendToTarget();
                    String envState = VolumeInfo.getEnvironmentForState(vol.getState());
                    this.mCallbacks.notifyStorageStateChanged(userVol.getPath(), envState, envState);
                }
            }
            this.mSystemUnlockedUsers = ArrayUtils.appendInt(this.mSystemUnlockedUsers, userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onCleanupUser(int userId) {
        Slog.d(TAG, "onCleanupUser " + userId);
        try {
            this.mVold.onUserStopped(userId);
            this.mStoraged.onUserStopped(userId);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
        synchronized (this.mLock) {
            this.mSystemUnlockedUsers = ArrayUtils.removeInt(this.mSystemUnlockedUsers, userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onStopUser(int userId) {
        Slog.i(TAG, "onStopUser " + userId);
        try {
            this.mStorageSessionController.onUserStopping(userId);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
        PackageMonitor monitor = this.mPackageMonitorsForUser.remove(Integer.valueOf(userId));
        if (monitor != null) {
            monitor.unregister();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeRemountVolumes(int userId) {
        List<VolumeInfo> volumesToRemount = new ArrayList<>();
        synchronized (this.mLock) {
            for (int i = 0; i < this.mVolumes.size(); i++) {
                VolumeInfo vol = this.mVolumes.valueAt(i);
                if (!vol.isPrimary() && vol.isMountedWritable() && vol.isVisible() && vol.getMountUserId() != this.mCurrentUserId) {
                    vol.mountUserId = this.mCurrentUserId;
                    volumesToRemount.add(vol);
                }
            }
        }
        for (VolumeInfo vol2 : volumesToRemount) {
            Slog.i(TAG, "Remounting volume for user: " + userId + ". Volume: " + vol2);
            this.mHandler.obtainMessage(8, vol2).sendToTarget();
            this.mHandler.obtainMessage(5, vol2).sendToTarget();
        }
    }

    private boolean supportsBlockCheckpoint() throws RemoteException {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        return this.mVold.supportsBlockCheckpoint();
    }

    @Override // com.android.server.wm.ActivityTaskManagerInternal.ScreenObserver
    public void onAwakeStateChanged(boolean isAwake) {
    }

    @Override // com.android.server.wm.ActivityTaskManagerInternal.ScreenObserver
    public void onKeyguardStateChanged(boolean isShowing) {
        this.mSecureKeyguardShowing = isShowing && ((KeyguardManager) this.mContext.getSystemService(KeyguardManager.class)).isDeviceSecure(this.mCurrentUserId);
        try {
            this.mVold.onSecureKeyguardStateChanged(this.mSecureKeyguardShowing);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    void runIdleMaintenance(Runnable callback) {
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(4, callback));
    }

    public void runMaintenance() {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        runIdleMaintenance(null);
    }

    public long lastMaintenance() {
        return this.mLastMaintenance;
    }

    public void onDaemonConnected() {
        this.mDaemonConnected = true;
        this.mHandler.obtainMessage(2).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleDaemonConnected() {
        initIfBootedAndConnected();
        resetIfBootedAndConnected();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onDiskScannedLocked(DiskInfo disk) {
        int volumeCount = 0;
        for (int i = 0; i < this.mVolumes.size(); i++) {
            VolumeInfo vol = this.mVolumes.valueAt(i);
            if (Objects.equals(disk.id, vol.getDiskId())) {
                volumeCount++;
            }
        }
        Intent intent = new Intent("android.os.storage.action.DISK_SCANNED");
        intent.addFlags(AudioFormat.HE_AAC_V1);
        intent.putExtra("android.os.storage.extra.DISK_ID", disk.id);
        intent.putExtra("android.os.storage.extra.VOLUME_COUNT", volumeCount);
        this.mHandler.obtainMessage(7, intent).sendToTarget();
        CountDownLatch latch = this.mDiskScanLatches.remove(disk.id);
        if (latch != null) {
            latch.countDown();
        }
        disk.volumeCount = volumeCount;
        this.mCallbacks.notifyDiskScanned(disk, volumeCount);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onVolumeCreatedLocked(VolumeInfo vol) {
        if (this.mPmInternal.isOnlyCoreApps()) {
            Slog.d(TAG, "System booted in core-only mode; ignoring volume " + vol.getId());
            return;
        }
        ActivityManagerInternal amInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        if (vol.mountUserId >= 0 && !amInternal.isUserRunning(vol.mountUserId, 0)) {
            Slog.d(TAG, "Ignoring volume " + vol.getId() + " because user " + Integer.toString(vol.mountUserId) + " is no longer running.");
        } else if (vol.type == 2) {
            Context volumeUserContext = this.mContext.createContextAsUser(UserHandle.of(vol.mountUserId), 0);
            boolean isMediaSharedWithParent = volumeUserContext != null ? ((UserManager) volumeUserContext.getSystemService(UserManager.class)).isMediaSharedWithParent() : false;
            if (!isMediaSharedWithParent && !this.mStorageSessionController.supportsExternalStorage(vol.mountUserId)) {
                Slog.d(TAG, "Ignoring volume " + vol.getId() + " because user " + Integer.toString(vol.mountUserId) + " does not support external storage.");
                return;
            }
            StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
            VolumeInfo privateVol = storage.findPrivateForEmulated(vol);
            if ((Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, this.mPrimaryStorageUuid) && "private".equals(privateVol.id)) || Objects.equals(privateVol.fsUuid, this.mPrimaryStorageUuid)) {
                Slog.v(TAG, "Found primary storage at " + vol);
                vol.mountFlags |= 1;
                vol.mountFlags |= 4;
                this.mHandler.obtainMessage(5, vol).sendToTarget();
            }
        } else if (vol.type == 0) {
            if (Objects.equals("primary_physical", this.mPrimaryStorageUuid) && vol.disk.isDefaultPrimary()) {
                Slog.v(TAG, "Found primary storage at " + vol);
                vol.mountFlags |= 1;
                vol.mountFlags |= 4;
            }
            if (vol.disk.isAdoptable()) {
                vol.mountFlags |= 4;
            }
            vol.mountUserId = this.mCurrentUserId;
            this.mHandler.obtainMessage(5, vol).sendToTarget();
        } else if (vol.type == 1) {
            this.mHandler.obtainMessage(5, vol).sendToTarget();
        } else if (vol.type == 5) {
            if (vol.disk.isStubVisible()) {
                vol.mountFlags |= 4;
            } else {
                vol.mountFlags |= 2;
            }
            vol.mountUserId = this.mCurrentUserId;
            this.mHandler.obtainMessage(5, vol).sendToTarget();
        } else {
            Slog.d(TAG, "Skipping automatic mounting of " + vol);
        }
    }

    private boolean isBroadcastWorthy(VolumeInfo vol) {
        switch (vol.getType()) {
            case 0:
            case 1:
            case 2:
            case 5:
                switch (vol.getState()) {
                    case 0:
                    case 2:
                    case 3:
                    case 5:
                    case 6:
                    case 8:
                        return true;
                    case 1:
                    case 4:
                    case 7:
                    default:
                        return false;
                }
            case 3:
            case 4:
            default:
                return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onVolumeStateChangedLocked(final VolumeInfo vol, int oldState, int newState) {
        if (vol.type == 2) {
            if (newState != 2) {
                this.mFuseMountedUser.remove(Integer.valueOf(vol.getMountUserId()));
            } else if (this.mVoldAppDataIsolationEnabled) {
                final int userId = vol.getMountUserId();
                new Thread(new Runnable() { // from class: com.android.server.StorageManagerService$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        StorageManagerService.this.m414x52e391b7(userId, vol);
                    }
                }).start();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onVolumeStateChangedLocked$1$com-android-server-StorageManagerService  reason: not valid java name */
    public /* synthetic */ void m414x52e391b7(int userId, VolumeInfo vol) {
        if (userId == 0 && Build.VERSION.DEVICE_INITIAL_SDK_INT < 29) {
            this.mPmInternal.migrateLegacyObbData();
        }
        this.mFuseMountedUser.add(Integer.valueOf(userId));
        Map<Integer, String> pidPkgMap = null;
        int i = 0;
        while (true) {
            if (i >= 5) {
                break;
            }
            try {
                pidPkgMap = ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).getProcessesWithPendingBindMounts(vol.getMountUserId());
                break;
            } catch (IllegalStateException e) {
                Slog.i(TAG, "Some processes are starting, retry");
                SystemClock.sleep(100L);
                i++;
            }
        }
        if (pidPkgMap != null) {
            remountAppStorageDirs(pidPkgMap, userId);
        } else {
            Slog.wtf(TAG, "Not able to getStorageNotOptimizedProcesses() after 5 retries");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onVolumeStateChangedAsync(VolumeInfo vol, int oldState, int newState) {
        int[] iArr;
        synchronized (this.mLock) {
            if (!TextUtils.isEmpty(vol.fsUuid)) {
                VolumeRecord rec = this.mRecords.get(vol.fsUuid);
                if (rec == null) {
                    rec = new VolumeRecord(vol.type, vol.fsUuid);
                    rec.partGuid = vol.partGuid;
                    rec.createdMillis = System.currentTimeMillis();
                    if (vol.type == 1) {
                        rec.nickname = vol.disk.getDescription();
                    }
                    this.mRecords.put(rec.fsUuid, rec);
                } else if (TextUtils.isEmpty(rec.partGuid)) {
                    rec.partGuid = vol.partGuid;
                }
                rec.lastSeenMillis = System.currentTimeMillis();
                writeSettingsLocked();
            }
        }
        if (newState == 2) {
            prepareUserStorageIfNeeded(vol);
        }
        try {
            this.mStorageSessionController.notifyVolumeStateChanged(vol);
        } catch (StorageSessionController.ExternalStorageServiceException e) {
            Log.e(TAG, "Failed to notify volume state changed to the Storage Service", e);
        }
        synchronized (this.mLock) {
            this.mCallbacks.notifyVolumeStateChanged(vol, oldState, newState);
            if (this.mBootCompleted && isBroadcastWorthy(vol)) {
                Intent intent = new Intent("android.os.storage.action.VOLUME_STATE_CHANGED");
                intent.putExtra("android.os.storage.extra.VOLUME_ID", vol.id);
                intent.putExtra("android.os.storage.extra.VOLUME_STATE", newState);
                intent.putExtra("android.os.storage.extra.FS_UUID", vol.fsUuid);
                intent.addFlags(AudioFormat.HE_AAC_V1);
                this.mHandler.obtainMessage(7, intent).sendToTarget();
            }
            String oldStateEnv = VolumeInfo.getEnvironmentForState(oldState);
            String newStateEnv = VolumeInfo.getEnvironmentForState(newState);
            if (!Objects.equals(oldStateEnv, newStateEnv)) {
                for (int userId : this.mSystemUnlockedUsers) {
                    if (vol.isVisibleForUser(userId)) {
                        StorageVolume userVol = vol.buildStorageVolume(this.mContext, userId, false);
                        this.mHandler.obtainMessage(6, userVol).sendToTarget();
                        this.mCallbacks.notifyStorageStateChanged(userVol.getPath(), oldStateEnv, newStateEnv);
                    }
                }
            }
            if ((vol.type == 0 || vol.type == 5) && vol.state == 5) {
                ObbActionHandler obbActionHandler = this.mObbActionHandler;
                obbActionHandler.sendMessage(obbActionHandler.obtainMessage(2, vol.path));
            }
            maybeLogMediaMount(vol, newState);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyCloudMediaProviderChangedAsync(StorageManagerInternal.CloudProviderChangeListener listener) {
        synchronized (this.mCloudMediaProviders) {
            for (int i = this.mCloudMediaProviders.size() - 1; i >= 0; i--) {
                listener.onCloudProviderChanged(this.mCloudMediaProviders.keyAt(i), this.mCloudMediaProviders.valueAt(i));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onCloudMediaProviderChangedAsync(int userId) {
        String authority;
        synchronized (this.mCloudMediaProviders) {
            authority = this.mCloudMediaProviders.get(userId);
        }
        Iterator it = this.mStorageManagerInternal.mCloudProviderChangeListeners.iterator();
        while (it.hasNext()) {
            StorageManagerInternal.CloudProviderChangeListener listener = (StorageManagerInternal.CloudProviderChangeListener) it.next();
            listener.onCloudProviderChanged(userId, authority);
        }
    }

    private void maybeLogMediaMount(VolumeInfo vol, int newState) {
        DiskInfo disk;
        if (!SecurityLog.isLoggingEnabled() || (disk = vol.getDisk()) == null || (disk.flags & 12) == 0) {
            return;
        }
        String label = disk.label != null ? disk.label.trim() : "";
        if (newState == 2 || newState == 3) {
            SecurityLog.writeEvent(210013, new Object[]{vol.path, label});
        } else if (newState == 0 || newState == 8) {
            SecurityLog.writeEvent(210014, new Object[]{vol.path, label});
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onMoveStatusLocked(int status) {
        IPackageMoveObserver iPackageMoveObserver = this.mMoveCallback;
        if (iPackageMoveObserver == null) {
            Slog.w(TAG, "Odd, status but no move requested");
            return;
        }
        try {
            iPackageMoveObserver.onStatusChanged(-1, status, -1L);
        } catch (RemoteException e) {
        }
        if (status == 82) {
            Slog.d(TAG, "Move to " + this.mMoveTargetUuid + " copy phase finshed; persisting");
            this.mPrimaryStorageUuid = this.mMoveTargetUuid;
            writeSettingsLocked();
        }
        if (PackageManager.isMoveStatusFinished(status)) {
            Slog.d(TAG, "Move to " + this.mMoveTargetUuid + " finished with status " + status);
            this.mMoveCallback = null;
            this.mMoveTargetUuid = null;
        }
    }

    private void enforcePermission(String perm) {
        this.mContext.enforceCallingOrSelfPermission(perm, perm);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isMountDisallowed(VolumeInfo vol) {
        UserManager userManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        boolean isUsbRestricted = false;
        if (vol.disk != null && vol.disk.isUsb()) {
            isUsbRestricted = userManager.hasUserRestriction("no_usb_file_transfer", Binder.getCallingUserHandle());
        }
        boolean isTypeRestricted = false;
        if (vol.type == 0 || vol.type == 1 || vol.type == 5) {
            isTypeRestricted = userManager.hasUserRestriction("no_physical_media", Binder.getCallingUserHandle());
        }
        return isUsbRestricted || isTypeRestricted;
    }

    private void enforceAdminUser() {
        UserManager um = (UserManager) this.mContext.getSystemService("user");
        int callingUserId = UserHandle.getCallingUserId();
        long token = Binder.clearCallingIdentity();
        try {
            boolean isAdmin = um.getUserInfo(callingUserId).isAdmin();
            if (!isAdmin) {
                throw new SecurityException("Only admin users can adopt sd cards");
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public StorageManagerService(Context context) {
        sSelf = this;
        this.mVoldAppDataIsolationEnabled = SystemProperties.getBoolean(ANDROID_VOLD_APP_DATA_ISOLATION_ENABLED_PROPERTY, false);
        this.mContext = context;
        this.mResolver = context.getContentResolver();
        this.mCallbacks = new Callbacks(FgThread.get().getLooper());
        this.mLockPatternUtils = new LockPatternUtils(context);
        HandlerThread hthread = new HandlerThread(TAG);
        hthread.start();
        this.mHandler = new StorageManagerServiceHandler(hthread.getLooper());
        this.mObbActionHandler = new ObbActionHandler(IoThread.get().getLooper());
        this.mStorageSessionController = new StorageSessionController(context);
        Installer installer = new Installer(context);
        this.mInstaller = installer;
        installer.onStart();
        File dataDir = Environment.getDataDirectory();
        File systemDir = new File(dataDir, HostingRecord.HOSTING_TYPE_SYSTEM);
        File file = new File(systemDir, LAST_FSTRIM_FILE);
        this.mLastMaintenanceFile = file;
        if (!file.exists()) {
            try {
                new FileOutputStream(file).close();
            } catch (IOException e) {
                Slog.e(TAG, "Unable to create fstrim record " + this.mLastMaintenanceFile.getPath());
            }
        } else {
            this.mLastMaintenance = file.lastModified();
        }
        this.mSettingsFile = new AtomicFile(new File(Environment.getDataSystemDirectory(), "storage.xml"), "storage-settings");
        this.mWriteRecordFile = new AtomicFile(new File(Environment.getDataSystemDirectory(), "storage-write-records"));
        sSmartIdleMaintPeriod = DeviceConfig.getInt("storage_native_boot", "smart_idle_maint_period", 60);
        if (sSmartIdleMaintPeriod < 10) {
            sSmartIdleMaintPeriod = 10;
        } else if (sSmartIdleMaintPeriod > MAX_SMART_IDLE_MAINT_PERIOD) {
            sSmartIdleMaintPeriod = MAX_SMART_IDLE_MAINT_PERIOD;
        }
        this.mMaxWriteRecords = MAX_PERIOD_WRITE_RECORD / sSmartIdleMaintPeriod;
        this.mStorageWriteRecords = new int[this.mMaxWriteRecords];
        synchronized (this.mLock) {
            readSettingsLocked();
        }
        LocalServices.addService(StorageManagerInternal.class, this.mStorageManagerInternal);
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_ADDED");
        userFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiver(this.mUserReceiver, userFilter, null, this.mHandler);
        synchronized (this.mLock) {
            addInternalVolumeLocked();
        }
        Watchdog.getInstance().addMonitor(this);
    }

    public void start() {
        m412x31765603();
        m413lambda$connectVold$3$comandroidserverStorageManagerService();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: connectStoraged */
    public void m412x31765603() {
        IBinder binder = ServiceManager.getService("storaged");
        if (binder != null) {
            try {
                binder.linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.StorageManagerService.4
                    @Override // android.os.IBinder.DeathRecipient
                    public void binderDied() {
                        Slog.w(StorageManagerService.TAG, "storaged died; reconnecting");
                        StorageManagerService.this.mStoraged = null;
                        StorageManagerService.this.m412x31765603();
                    }
                }, 0);
            } catch (RemoteException e) {
                binder = null;
            }
        }
        if (binder != null) {
            this.mStoraged = IStoraged.Stub.asInterface(binder);
        } else {
            Slog.w(TAG, "storaged not found; trying again");
        }
        if (this.mStoraged == null) {
            BackgroundThread.getHandler().postDelayed(new Runnable() { // from class: com.android.server.StorageManagerService$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    StorageManagerService.this.m412x31765603();
                }
            }, 1000L);
        } else {
            onDaemonConnected();
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: connectVold */
    public void m413lambda$connectVold$3$comandroidserverStorageManagerService() {
        IBinder binder = ServiceManager.getService("vold");
        if (binder != null) {
            try {
                binder.linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.StorageManagerService.5
                    @Override // android.os.IBinder.DeathRecipient
                    public void binderDied() {
                        Slog.w(StorageManagerService.TAG, "vold died; reconnecting");
                        StorageManagerService.this.mVold = null;
                        StorageManagerService.this.m413lambda$connectVold$3$comandroidserverStorageManagerService();
                    }
                }, 0);
            } catch (RemoteException e) {
                binder = null;
            }
        }
        if (binder == null) {
            Slog.w(TAG, "vold not found; trying again");
        } else {
            this.mVold = IVold.Stub.asInterface(binder);
            try {
                this.mVold.setListener(this.mListener);
            } catch (RemoteException e2) {
                this.mVold = null;
                Slog.w(TAG, "vold listener rejected; trying again", e2);
            }
        }
        if (this.mVold == null) {
            BackgroundThread.getHandler().postDelayed(new Runnable() { // from class: com.android.server.StorageManagerService$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    StorageManagerService.this.m413lambda$connectVold$3$comandroidserverStorageManagerService();
                }
            }, 1000L);
            return;
        }
        restoreLocalUnlockedUsers();
        onDaemonConnected();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void servicesReady() {
        String[] strArr;
        this.mPmInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mIPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        this.mIAppOpsService = IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
        ProviderInfo provider = getProviderInfo("media");
        if (provider != null) {
            this.mMediaStoreAuthorityAppId = UserHandle.getAppId(provider.applicationInfo.uid);
            sMediaStoreAuthorityProcessName = provider.applicationInfo.processName;
        }
        ProviderInfo provider2 = getProviderInfo("downloads");
        if (provider2 != null) {
            this.mDownloadsAuthorityAppId = UserHandle.getAppId(provider2.applicationInfo.uid);
        }
        ProviderInfo provider3 = getProviderInfo("com.android.externalstorage.documents");
        if (provider3 != null) {
            this.mExternalStorageAuthorityAppId = UserHandle.getAppId(provider3.applicationInfo.uid);
        }
        for (String packageName : SYSTEM_APP_AUTHORITY) {
            int uid = this.mPmInternal.getPackageUid(packageName, 4194304L, UserHandle.getUserId(0));
            if (uid >= 0) {
                this.mSystemAppAuthorityAppId.add(Integer.valueOf(uid));
            }
        }
    }

    private ProviderInfo getProviderInfo(String authority) {
        return this.mPmInternal.resolveContentProvider(authority, 786432L, UserHandle.getUserId(0), 1000);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateLegacyStorageApps(String packageName, int uid, boolean hasLegacy) {
        synchronized (this.mLock) {
            if (hasLegacy) {
                Slog.v(TAG, "Package " + packageName + " has legacy storage");
                this.mUidsWithLegacyExternalStorage.add(Integer.valueOf(uid));
            } else {
                Slog.v(TAG, "Package " + packageName + " does not have legacy storage");
                this.mUidsWithLegacyExternalStorage.remove(Integer.valueOf(uid));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void snapshotAndMonitorLegacyStorageAppOp(UserHandle user) {
        int userId = user.getIdentifier();
        Iterator<ApplicationInfo> it = this.mPmInternal.getInstalledApplications(4988928L, userId, Process.myUid()).iterator();
        while (true) {
            boolean hasLegacy = true;
            if (!it.hasNext()) {
                break;
            }
            ApplicationInfo ai = it.next();
            try {
                if (this.mIAppOpsService.checkOperation(87, ai.uid, ai.packageName) != 0) {
                    hasLegacy = false;
                }
                updateLegacyStorageApps(ai.packageName, ai.uid, hasLegacy);
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to check legacy op for package " + ai.packageName, e);
            }
        }
        if (this.mPackageMonitorsForUser.get(Integer.valueOf(userId)) == null) {
            PackageMonitor monitor = new PackageMonitor() { // from class: com.android.server.StorageManagerService.6
                public void onPackageRemoved(String packageName, int uid) {
                    StorageManagerService.this.updateLegacyStorageApps(packageName, uid, false);
                }
            };
            monitor.register(this.mContext, user, true, this.mHandler);
            this.mPackageMonitorsForUser.put(Integer.valueOf(userId), monitor);
            return;
        }
        Slog.w(TAG, "PackageMonitor is already registered for: " + userId);
    }

    private static long getLastAccessTime(AppOpsManager manager, int uid, String packageName, int[] ops) {
        long maxTime = 0;
        List<AppOpsManager.PackageOps> pkgs = manager.getOpsForPackage(uid, packageName, ops);
        for (AppOpsManager.PackageOps pkg : CollectionUtils.emptyIfNull(pkgs)) {
            for (AppOpsManager.OpEntry op : CollectionUtils.emptyIfNull(pkg.getOps())) {
                maxTime = Math.max(maxTime, op.getLastAccessTime(13));
            }
        }
        return maxTime;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void systemReady() {
        ((ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class)).registerScreenObserver(this);
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void bootCompleted() {
        this.mBootCompleted = true;
        this.mHandler.obtainMessage(13).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleBootCompleted() {
        initIfBootedAndConnected();
        resetIfBootedAndConnected();
    }

    private String getDefaultPrimaryStorageUuid() {
        if (SystemProperties.getBoolean("ro.vold.primary_physical", false)) {
            return "primary_physical";
        }
        return StorageManager.UUID_PRIVATE_INTERNAL;
    }

    private void readSettingsLocked() {
        this.mRecords.clear();
        this.mPrimaryStorageUuid = getDefaultPrimaryStorageUuid();
        FileInputStream fis = null;
        try {
            try {
                try {
                    fis = this.mSettingsFile.openRead();
                    TypedXmlPullParser in = Xml.resolvePullParser(fis);
                    while (true) {
                        int type = in.next();
                        boolean z = true;
                        if (type == 1) {
                            break;
                        } else if (type == 2) {
                            String tag = in.getName();
                            if (TAG_VOLUMES.equals(tag)) {
                                int version = in.getAttributeInt((String) null, ATTR_VERSION, 1);
                                boolean primaryPhysical = SystemProperties.getBoolean("ro.vold.primary_physical", false);
                                if (version < 3 && (version < 2 || primaryPhysical)) {
                                    z = false;
                                }
                                boolean validAttr = z;
                                if (validAttr) {
                                    this.mPrimaryStorageUuid = XmlUtils.readStringAttribute(in, ATTR_PRIMARY_STORAGE_UUID);
                                }
                            } else if (TAG_VOLUME.equals(tag)) {
                                VolumeRecord rec = readVolumeRecord(in);
                                this.mRecords.put(rec.fsUuid, rec);
                            }
                        }
                    }
                } catch (XmlPullParserException e) {
                    Slog.wtf(TAG, "Failed reading metadata", e);
                }
            } catch (FileNotFoundException e2) {
            } catch (IOException e3) {
                Slog.wtf(TAG, "Failed reading metadata", e3);
            }
        } finally {
            IoUtils.closeQuietly(fis);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeSettingsLocked() {
        FileOutputStream fos = null;
        try {
            fos = this.mSettingsFile.startWrite();
            TypedXmlSerializer out = Xml.resolveSerializer(fos);
            out.startDocument((String) null, true);
            out.startTag((String) null, TAG_VOLUMES);
            out.attributeInt((String) null, ATTR_VERSION, 3);
            XmlUtils.writeStringAttribute(out, ATTR_PRIMARY_STORAGE_UUID, this.mPrimaryStorageUuid);
            int size = this.mRecords.size();
            for (int i = 0; i < size; i++) {
                VolumeRecord rec = this.mRecords.valueAt(i);
                writeVolumeRecord(out, rec);
            }
            out.endTag((String) null, TAG_VOLUMES);
            out.endDocument();
            this.mSettingsFile.finishWrite(fos);
        } catch (IOException e) {
            if (fos != null) {
                this.mSettingsFile.failWrite(fos);
            }
        }
    }

    public static VolumeRecord readVolumeRecord(TypedXmlPullParser in) throws IOException, XmlPullParserException {
        int type = in.getAttributeInt((String) null, "type");
        String fsUuid = XmlUtils.readStringAttribute(in, ATTR_FS_UUID);
        VolumeRecord meta = new VolumeRecord(type, fsUuid);
        meta.partGuid = XmlUtils.readStringAttribute(in, ATTR_PART_GUID);
        meta.nickname = XmlUtils.readStringAttribute(in, ATTR_NICKNAME);
        meta.userFlags = in.getAttributeInt((String) null, ATTR_USER_FLAGS);
        meta.createdMillis = in.getAttributeLong((String) null, ATTR_CREATED_MILLIS, 0L);
        meta.lastSeenMillis = in.getAttributeLong((String) null, ATTR_LAST_SEEN_MILLIS, 0L);
        meta.lastTrimMillis = in.getAttributeLong((String) null, ATTR_LAST_TRIM_MILLIS, 0L);
        meta.lastBenchMillis = in.getAttributeLong((String) null, ATTR_LAST_BENCH_MILLIS, 0L);
        return meta;
    }

    public static void writeVolumeRecord(TypedXmlSerializer out, VolumeRecord rec) throws IOException {
        out.startTag((String) null, TAG_VOLUME);
        out.attributeInt((String) null, "type", rec.type);
        XmlUtils.writeStringAttribute(out, ATTR_FS_UUID, rec.fsUuid);
        XmlUtils.writeStringAttribute(out, ATTR_PART_GUID, rec.partGuid);
        XmlUtils.writeStringAttribute(out, ATTR_NICKNAME, rec.nickname);
        out.attributeInt((String) null, ATTR_USER_FLAGS, rec.userFlags);
        out.attributeLong((String) null, ATTR_CREATED_MILLIS, rec.createdMillis);
        out.attributeLong((String) null, ATTR_LAST_SEEN_MILLIS, rec.lastSeenMillis);
        out.attributeLong((String) null, ATTR_LAST_TRIM_MILLIS, rec.lastTrimMillis);
        out.attributeLong((String) null, ATTR_LAST_BENCH_MILLIS, rec.lastBenchMillis);
        out.endTag((String) null, TAG_VOLUME);
    }

    public void registerListener(IStorageEventListener listener) {
        this.mCallbacks.register(listener);
    }

    public void unregisterListener(IStorageEventListener listener) {
        this.mCallbacks.unregister(listener);
    }

    public void shutdown(IStorageShutdownObserver observer) {
        enforcePermission("android.permission.SHUTDOWN");
        Slog.i(TAG, "Shutting down");
        this.mHandler.obtainMessage(3, observer).sendToTarget();
    }

    public void mount(String volId) {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        VolumeInfo vol = findVolumeByIdOrThrow(volId);
        if (isMountDisallowed(vol)) {
            throw new SecurityException("Mounting " + volId + " restricted by policy");
        }
        mount(vol);
    }

    private void remountAppStorageDirs(Map<Integer, String> pidPkgMap, int userId) {
        for (Map.Entry<Integer, String> entry : pidPkgMap.entrySet()) {
            int pid = entry.getKey().intValue();
            String packageName = entry.getValue();
            Slog.i(TAG, "Remounting storage for pid: " + pid);
            String[] sharedPackages = this.mPmInternal.getSharedUserPackagesForPackage(packageName, userId);
            int uid = this.mPmInternal.getPackageUid(packageName, 0L, userId);
            String[] packages = sharedPackages.length != 0 ? sharedPackages : new String[]{packageName};
            try {
                this.mVold.remountAppStorageDirs(uid, pid, packages);
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void mount(final VolumeInfo vol) {
        try {
            Slog.i(TAG, "Mounting volume " + vol);
            this.mVold.mount(vol.id, vol.mountFlags, vol.mountUserId, new IVoldMountCallback.Stub() { // from class: com.android.server.StorageManagerService.7
                @Override // android.os.IVoldMountCallback
                public boolean onVolumeChecking(FileDescriptor fd, String path, String internalPath) {
                    vol.path = path;
                    vol.internalPath = internalPath;
                    ParcelFileDescriptor pfd = new ParcelFileDescriptor(fd);
                    try {
                        try {
                            StorageManagerService.this.mStorageSessionController.onVolumeMount(pfd, vol);
                            try {
                                pfd.close();
                            } catch (Exception e) {
                                Slog.e(StorageManagerService.TAG, "Failed to close FUSE device fd", e);
                            }
                            return true;
                        } catch (Throwable th) {
                            try {
                                pfd.close();
                            } catch (Exception e2) {
                                Slog.e(StorageManagerService.TAG, "Failed to close FUSE device fd", e2);
                            }
                            throw th;
                        }
                    } catch (StorageSessionController.ExternalStorageServiceException e3) {
                        Slog.e(StorageManagerService.TAG, "Failed to mount volume " + vol, e3);
                        Slog.i(StorageManagerService.TAG, "Scheduling reset in 10s");
                        StorageManagerService.this.mHandler.removeMessages(10);
                        StorageManagerService.this.mHandler.sendMessageDelayed(StorageManagerService.this.mHandler.obtainMessage(10), TimeUnit.SECONDS.toMillis(10));
                        try {
                            pfd.close();
                        } catch (Exception e4) {
                            Slog.e(StorageManagerService.TAG, "Failed to close FUSE device fd", e4);
                        }
                        return false;
                    }
                }
            });
            Slog.i(TAG, "Mounted volume " + vol);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void unmount(String volId) {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        VolumeInfo vol = findVolumeByIdOrThrow(volId);
        unmount(vol);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unmount(VolumeInfo vol) {
        try {
            try {
                if (vol.type == 1) {
                    this.mInstaller.onPrivateVolumeRemoved(vol.getFsUuid());
                }
            } catch (Installer.InstallerException e) {
                Slog.e(TAG, "Failed unmount mirror data", e);
            }
            this.mVold.unmount(vol.id);
            this.mStorageSessionController.onVolumeUnmount(vol);
        } catch (Exception e2) {
            Slog.wtf(TAG, e2);
        }
    }

    public void format(String volId) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        VolumeInfo vol = findVolumeByIdOrThrow(volId);
        String fsUuid = vol.fsUuid;
        try {
            this.mVold.format(vol.id, UiModeManagerService.Shell.NIGHT_MODE_STR_AUTO);
            if (!TextUtils.isEmpty(fsUuid)) {
                forgetVolume(fsUuid);
            }
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void benchmark(String volId, final IVoldTaskListener listener) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        try {
            this.mVold.benchmark(volId, new IVoldTaskListener.Stub() { // from class: com.android.server.StorageManagerService.8
                @Override // android.os.IVoldTaskListener
                public void onStatus(int status, PersistableBundle extras) {
                    StorageManagerService.this.dispatchOnStatus(listener, status, extras);
                }

                @Override // android.os.IVoldTaskListener
                public void onFinished(int status, PersistableBundle extras) {
                    StorageManagerService.this.dispatchOnFinished(listener, status, extras);
                    String path = extras.getString("path");
                    String ident = extras.getString("ident");
                    long create = extras.getLong("create");
                    long run = extras.getLong("run");
                    long destroy = extras.getLong("destroy");
                    DropBoxManager dropBox = (DropBoxManager) StorageManagerService.this.mContext.getSystemService(DropBoxManager.class);
                    dropBox.addText(StorageManagerService.TAG_STORAGE_BENCHMARK, StorageManagerService.this.scrubPath(path) + " " + ident + " " + create + " " + run + " " + destroy);
                    synchronized (StorageManagerService.this.mLock) {
                        VolumeRecord rec = StorageManagerService.this.findRecordForPath(path);
                        if (rec != null) {
                            rec.lastBenchMillis = System.currentTimeMillis();
                            StorageManagerService.this.writeSettingsLocked();
                        }
                    }
                }
            });
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    public void partitionPublic(String diskId) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        CountDownLatch latch = findOrCreateDiskScanLatch(diskId);
        VolumeInfo volumeinfo = null;
        try {
            synchronized (this.mLock) {
                int size = this.mVolumes.size();
                int i = 0;
                while (true) {
                    if (i >= size) {
                        break;
                    }
                    VolumeInfo vol = this.mVolumes.valueAt(i);
                    if (!diskId.equals(vol.getDiskId())) {
                        i++;
                    } else {
                        volumeinfo = vol;
                        break;
                    }
                }
            }
            this.mVold.partition(diskId, 0, -1);
            waitForLatch(latch, "partitionPublic", 180000L);
            if (volumeinfo.getType() == 1) {
                forgetVolume(volumeinfo.getFsUuid());
            }
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void partitionPrivate(String diskId) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        enforceAdminUser();
        CountDownLatch latch = findOrCreateDiskScanLatch(diskId);
        try {
            this.mVold.partition(diskId, 1, -1);
            waitForLatch(latch, "partitionPrivate", 180000L);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void partitionMixed(String diskId, int ratio) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        enforceAdminUser();
        CountDownLatch latch = findOrCreateDiskScanLatch(diskId);
        try {
            this.mVold.partition(diskId, 2, ratio);
            waitForLatch(latch, "partitionMixed", 180000L);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void setVolumeNickname(String fsUuid, String nickname) {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        Objects.requireNonNull(fsUuid);
        synchronized (this.mLock) {
            VolumeRecord rec = this.mRecords.get(fsUuid);
            rec.nickname = nickname;
            this.mCallbacks.notifyVolumeRecordChanged(rec);
            writeSettingsLocked();
        }
    }

    public void setVolumeUserFlags(String fsUuid, int flags, int mask) {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        Objects.requireNonNull(fsUuid);
        synchronized (this.mLock) {
            VolumeRecord rec = this.mRecords.get(fsUuid);
            rec.userFlags = (rec.userFlags & (~mask)) | (flags & mask);
            this.mCallbacks.notifyVolumeRecordChanged(rec);
            writeSettingsLocked();
        }
    }

    public void forgetVolume(String fsUuid) {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        Objects.requireNonNull(fsUuid);
        synchronized (this.mLock) {
            VolumeRecord rec = this.mRecords.remove(fsUuid);
            if (rec != null && !TextUtils.isEmpty(rec.partGuid)) {
                this.mHandler.obtainMessage(9, rec).sendToTarget();
            }
            this.mCallbacks.notifyVolumeForgotten(fsUuid);
            if (Objects.equals(this.mPrimaryStorageUuid, fsUuid)) {
                this.mPrimaryStorageUuid = getDefaultPrimaryStorageUuid();
                this.mHandler.obtainMessage(10).sendToTarget();
            }
            writeSettingsLocked();
        }
    }

    public void forgetAllVolumes() {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        synchronized (this.mLock) {
            for (int i = 0; i < this.mRecords.size(); i++) {
                String fsUuid = this.mRecords.keyAt(i);
                VolumeRecord rec = this.mRecords.valueAt(i);
                if (!TextUtils.isEmpty(rec.partGuid)) {
                    this.mHandler.obtainMessage(9, rec).sendToTarget();
                }
                this.mCallbacks.notifyVolumeForgotten(fsUuid);
            }
            this.mRecords.clear();
            if (!Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, this.mPrimaryStorageUuid)) {
                this.mPrimaryStorageUuid = getDefaultPrimaryStorageUuid();
            }
            writeSettingsLocked();
            this.mHandler.obtainMessage(10).sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void forgetPartition(String partGuid, String fsUuid) {
        try {
            this.mVold.forgetPartition(partGuid, fsUuid);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void fstrim(int flags, final IVoldTaskListener listener) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        try {
            if (needsCheckpoint() && supportsBlockCheckpoint()) {
                Slog.i(TAG, "Skipping fstrim - block based checkpoint in progress");
                return;
            }
            this.mVold.fstrim(flags, new IVoldTaskListener.Stub() { // from class: com.android.server.StorageManagerService.9
                @Override // android.os.IVoldTaskListener
                public void onStatus(int status, PersistableBundle extras) {
                    StorageManagerService.this.dispatchOnStatus(listener, status, extras);
                    if (status != 0) {
                        return;
                    }
                    String path = extras.getString("path");
                    long bytes = extras.getLong("bytes");
                    long time = extras.getLong("time");
                    DropBoxManager dropBox = (DropBoxManager) StorageManagerService.this.mContext.getSystemService(DropBoxManager.class);
                    dropBox.addText(StorageManagerService.TAG_STORAGE_TRIM, StorageManagerService.this.scrubPath(path) + " " + bytes + " " + time);
                    synchronized (StorageManagerService.this.mLock) {
                        VolumeRecord rec = StorageManagerService.this.findRecordForPath(path);
                        if (rec != null) {
                            rec.lastTrimMillis = System.currentTimeMillis();
                            StorageManagerService.this.writeSettingsLocked();
                        }
                    }
                }

                @Override // android.os.IVoldTaskListener
                public void onFinished(int status, PersistableBundle extras) {
                    StorageManagerService.this.dispatchOnFinished(listener, status, extras);
                }
            });
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void runIdleMaint(final Runnable callback) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        try {
            if (needsCheckpoint() && supportsBlockCheckpoint()) {
                Slog.i(TAG, "Skipping idle maintenance - block based checkpoint in progress");
            }
            this.mVold.runIdleMaint(this.mNeedGC, new IVoldTaskListener.Stub() { // from class: com.android.server.StorageManagerService.10
                @Override // android.os.IVoldTaskListener
                public void onStatus(int status, PersistableBundle extras) {
                }

                @Override // android.os.IVoldTaskListener
                public void onFinished(int status, PersistableBundle extras) {
                    if (callback != null) {
                        BackgroundThread.getHandler().post(callback);
                    }
                }
            });
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void runIdleMaintenance() {
        runIdleMaint(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void abortIdleMaint(final Runnable callback) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        try {
            this.mVold.abortIdleMaint(new IVoldTaskListener.Stub() { // from class: com.android.server.StorageManagerService.11
                @Override // android.os.IVoldTaskListener
                public void onStatus(int status, PersistableBundle extras) {
                }

                @Override // android.os.IVoldTaskListener
                public void onFinished(int status, PersistableBundle extras) {
                    if (callback != null) {
                        BackgroundThread.getHandler().post(callback);
                    }
                }
            });
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void abortIdleMaintenance() {
        abortIdleMaint(null);
    }

    private boolean prepareSmartIdleMaint() {
        boolean smartIdleMaintEnabled = DeviceConfig.getBoolean("storage_native_boot", "smart_idle_maint_enabled", false);
        if (smartIdleMaintEnabled) {
            this.mLifetimePercentThreshold = DeviceConfig.getInt("storage_native_boot", "lifetime_threshold", 70);
            this.mMinSegmentsThreshold = DeviceConfig.getInt("storage_native_boot", "min_segments_threshold", 512);
            this.mDirtyReclaimRate = DeviceConfig.getFloat("storage_native_boot", "dirty_reclaim_rate", 0.5f);
            this.mSegmentReclaimWeight = DeviceConfig.getFloat("storage_native_boot", "segment_reclaim_weight", 1.0f);
            this.mLowBatteryLevel = DeviceConfig.getFloat("storage_native_boot", "low_battery_level", (float) DEFAULT_LOW_BATTERY_LEVEL);
            this.mChargingRequired = DeviceConfig.getBoolean("storage_native_boot", "charging_required", true);
            this.mNeedGC = false;
            loadStorageWriteRecords();
            try {
                this.mVold.refreshLatestWrite();
            } catch (Exception e) {
                Slog.wtf(TAG, e);
            }
            refreshLifetimeConstraint();
        }
        return smartIdleMaintEnabled;
    }

    public boolean isPassedLifetimeThresh() {
        return this.mPassedLifetimeThresh;
    }

    private void loadStorageWriteRecords() {
        FileInputStream fis = null;
        try {
            try {
                fis = this.mWriteRecordFile.openRead();
                ObjectInputStream ois = new ObjectInputStream(fis);
                int periodValue = ois.readInt();
                if (periodValue == sSmartIdleMaintPeriod) {
                    this.mStorageWriteRecords = (int[]) ois.readObject();
                }
            } catch (FileNotFoundException e) {
            } catch (Exception e2) {
                Slog.wtf(TAG, "Failed reading write records", e2);
            }
        } finally {
            IoUtils.closeQuietly(fis);
        }
    }

    private int getAverageWriteAmount() {
        return Arrays.stream(this.mStorageWriteRecords).sum() / this.mMaxWriteRecords;
    }

    private void updateStorageWriteRecords(int latestWrite) {
        FileOutputStream fos = null;
        System.arraycopy(this.mStorageWriteRecords, 0, this.mStorageWriteRecords, 1, this.mMaxWriteRecords - 1);
        this.mStorageWriteRecords[0] = latestWrite;
        try {
            fos = this.mWriteRecordFile.startWrite();
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeInt(sSmartIdleMaintPeriod);
            oos.writeObject(this.mStorageWriteRecords);
            this.mWriteRecordFile.finishWrite(fos);
        } catch (IOException e) {
            if (fos != null) {
                this.mWriteRecordFile.failWrite(fos);
            }
        }
    }

    private boolean checkChargeStatus() {
        int status;
        IntentFilter ifilter = new IntentFilter("android.intent.action.BATTERY_CHANGED");
        Intent batteryStatus = this.mContext.registerReceiver(null, ifilter);
        if (this.mChargingRequired && (status = batteryStatus.getIntExtra("status", -1)) != 2 && status != 5) {
            Slog.w(TAG, "Battery is not being charged");
            return false;
        }
        int level = batteryStatus.getIntExtra("level", -1);
        int scale = batteryStatus.getIntExtra("scale", -1);
        float chargePercent = (level * 100.0f) / scale;
        if (chargePercent < this.mLowBatteryLevel) {
            Slog.w(TAG, "Battery level is " + chargePercent + ", which is lower than threshold: " + this.mLowBatteryLevel);
            return false;
        }
        return true;
    }

    private boolean refreshLifetimeConstraint() {
        try {
            int storageLifeTime = this.mVold.getStorageLifeTime();
            if (storageLifeTime == -1) {
                Slog.w(TAG, "Failed to get storage lifetime");
                return false;
            } else if (storageLifeTime > this.mLifetimePercentThreshold) {
                Slog.w(TAG, "Ended smart idle maintenance, because of lifetime(" + storageLifeTime + "), lifetime threshold(" + this.mLifetimePercentThreshold + ")");
                this.mPassedLifetimeThresh = true;
                return false;
            } else {
                Slog.i(TAG, "Storage lifetime: " + storageLifeTime);
                return true;
            }
        } catch (Exception e) {
            Slog.wtf(TAG, e);
            return false;
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, INVOKE] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2913=5, 2914=5] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Code restructure failed: missing block: B:24:0x00a8, code lost:
        r10.run();
     */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x00ab, code lost:
        return;
     */
    /* JADX WARN: Removed duplicated region for block: B:40:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void runSmartIdleMaint(Runnable callback) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        try {
            try {
            } catch (Exception e) {
                Slog.wtf(TAG, e);
                if (callback == null) {
                    return;
                }
            }
            if (needsCheckpoint() && supportsBlockCheckpoint()) {
                Slog.i(TAG, "Skipping smart idle maintenance - block based checkpoint in progress");
                if (callback == null) {
                    return;
                }
                callback.run();
            }
            if (refreshLifetimeConstraint() && checkChargeStatus()) {
                int latestWrite = this.mVold.getWriteAmount();
                if (latestWrite == -1) {
                    Slog.w(TAG, "Failed to get storage write record");
                    if (callback != null) {
                        callback.run();
                        return;
                    }
                    return;
                }
                updateStorageWriteRecords(latestWrite);
                int avgWriteAmount = getAverageWriteAmount();
                Slog.i(TAG, "Set smart idle maintenance: latest write amount: " + latestWrite + ", average write amount: " + avgWriteAmount + ", min segment threshold: " + this.mMinSegmentsThreshold + ", dirty reclaim rate: " + this.mDirtyReclaimRate + ", segment reclaim weight: " + this.mSegmentReclaimWeight + ", period: " + sSmartIdleMaintPeriod);
                this.mVold.setGCUrgentPace(avgWriteAmount, this.mMinSegmentsThreshold, this.mDirtyReclaimRate, this.mSegmentReclaimWeight, sSmartIdleMaintPeriod);
                if (callback == null) {
                }
                callback.run();
            }
        } catch (Throwable th) {
            if (callback != null) {
                callback.run();
            }
            throw th;
        }
    }

    public void setDebugFlags(int flags, int mask) {
        long token;
        String value;
        String value2;
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        if ((mask & 4) != 0) {
            if (StorageManager.isFileEncryptedNativeOnly()) {
                throw new IllegalStateException("Emulation not supported on device with native FBE");
            }
            if (this.mLockPatternUtils.isCredentialRequiredToDecrypt(false)) {
                throw new IllegalStateException("Emulation requires disabling 'Secure start-up' in Settings > Security");
            }
            token = Binder.clearCallingIdentity();
            boolean emulateFbe = (flags & 4) != 0;
            try {
                SystemProperties.set("persist.sys.emulate_fbe", Boolean.toString(emulateFbe));
                ((PowerManager) this.mContext.getSystemService(PowerManager.class)).reboot(null);
                Binder.restoreCallingIdentity(token);
            } finally {
            }
        }
        if ((mask & 3) != 0) {
            if ((flags & 1) != 0) {
                value2 = "force_on";
            } else if ((flags & 2) != 0) {
                value2 = "force_off";
            } else {
                value2 = "";
            }
            token = Binder.clearCallingIdentity();
            try {
                SystemProperties.set("persist.sys.adoptable", value2);
                this.mHandler.obtainMessage(10).sendToTarget();
                Binder.restoreCallingIdentity(token);
            } finally {
            }
        }
        if ((mask & 24) != 0) {
            if ((flags & 8) != 0) {
                value = "force_on";
            } else if ((flags & 16) != 0) {
                value = "force_off";
            } else {
                value = "";
            }
            token = Binder.clearCallingIdentity();
            try {
                SystemProperties.set("persist.sys.sdcardfs", value);
                this.mHandler.obtainMessage(10).sendToTarget();
            } finally {
            }
        }
        if ((mask & 32) != 0) {
            boolean enabled = (flags & 32) != 0;
            token = Binder.clearCallingIdentity();
            try {
                SystemProperties.set("persist.sys.virtual_disk", Boolean.toString(enabled));
                this.mHandler.obtainMessage(10).sendToTarget();
            } finally {
            }
        }
    }

    public String getPrimaryStorageUuid() {
        String str;
        synchronized (this.mLock) {
            str = this.mPrimaryStorageUuid;
        }
        return str;
    }

    public void setPrimaryStorageUuid(String volumeUuid, IPackageMoveObserver callback) {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        synchronized (this.mLock) {
            if (Objects.equals(this.mPrimaryStorageUuid, volumeUuid)) {
                throw new IllegalArgumentException("Primary storage already at " + volumeUuid);
            }
            if (this.mMoveCallback != null) {
                throw new IllegalStateException("Move already in progress");
            }
            this.mMoveCallback = callback;
            this.mMoveTargetUuid = volumeUuid;
            List<UserInfo> users = ((UserManager) this.mContext.getSystemService(UserManager.class)).getUsers();
            for (UserInfo user : users) {
                if (StorageManager.isFileEncryptedNativeOrEmulated() && !isUserKeyUnlocked(user.id)) {
                    Slog.w(TAG, "Failing move due to locked user " + user.id);
                    onMoveStatusLocked(-10);
                    return;
                }
            }
            if (!Objects.equals("primary_physical", this.mPrimaryStorageUuid) && !Objects.equals("primary_physical", volumeUuid)) {
                int currentUserId = this.mCurrentUserId;
                VolumeInfo from = findStorageForUuidAsUser(this.mPrimaryStorageUuid, currentUserId);
                VolumeInfo to = findStorageForUuidAsUser(volumeUuid, currentUserId);
                if (from == null) {
                    Slog.w(TAG, "Failing move due to missing from volume " + this.mPrimaryStorageUuid);
                    onMoveStatusLocked(-6);
                    return;
                } else if (to == null) {
                    Slog.w(TAG, "Failing move due to missing to volume " + volumeUuid);
                    onMoveStatusLocked(-6);
                    return;
                } else {
                    try {
                        this.mVold.moveStorage(from.id, to.id, new IVoldTaskListener.Stub() { // from class: com.android.server.StorageManagerService.12
                            @Override // android.os.IVoldTaskListener
                            public void onStatus(int status, PersistableBundle extras) {
                                synchronized (StorageManagerService.this.mLock) {
                                    StorageManagerService.this.onMoveStatusLocked(status);
                                }
                            }

                            @Override // android.os.IVoldTaskListener
                            public void onFinished(int status, PersistableBundle extras) {
                            }
                        });
                        return;
                    } catch (Exception e) {
                        Slog.wtf(TAG, e);
                        return;
                    }
                }
            }
            Slog.d(TAG, "Skipping move to/from primary physical");
            onMoveStatusLocked(82);
            onMoveStatusLocked(-100);
            this.mHandler.obtainMessage(10).sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void warnOnNotMounted() {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mVolumes.size(); i++) {
                VolumeInfo vol = this.mVolumes.valueAt(i);
                if (vol.isPrimary() && vol.isMountedWritable()) {
                    return;
                }
            }
            Slog.w(TAG, "No primary storage mounted!");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isUidOwnerOfPackageOrSystem(String packageName, int callerUid) {
        if (callerUid == 1000) {
            return true;
        }
        return this.mPmInternal.isSameApp(packageName, callerUid, UserHandle.getUserId(callerUid));
    }

    public String getMountedObbPath(String rawPath) {
        ObbState state;
        Objects.requireNonNull(rawPath, "rawPath cannot be null");
        warnOnNotMounted();
        synchronized (this.mObbMounts) {
            state = this.mObbPathToStateMap.get(rawPath);
        }
        if (state == null) {
            Slog.w(TAG, "Failed to find OBB mounted at " + rawPath);
            return null;
        }
        return findVolumeByIdOrThrow(state.volId).getPath().getAbsolutePath();
    }

    public boolean isObbMounted(String rawPath) {
        boolean containsKey;
        Objects.requireNonNull(rawPath, "rawPath cannot be null");
        synchronized (this.mObbMounts) {
            containsKey = this.mObbPathToStateMap.containsKey(rawPath);
        }
        return containsKey;
    }

    public void mountObb(String rawPath, String canonicalPath, IObbActionListener token, int nonce, ObbInfo obbInfo) {
        Objects.requireNonNull(rawPath, "rawPath cannot be null");
        Objects.requireNonNull(canonicalPath, "canonicalPath cannot be null");
        Objects.requireNonNull(token, "token cannot be null");
        Objects.requireNonNull(obbInfo, "obbIfno cannot be null");
        int callingUid = Binder.getCallingUid();
        ObbState obbState = new ObbState(rawPath, canonicalPath, callingUid, token, nonce, null);
        ObbAction action = new MountObbAction(obbState, callingUid, obbInfo);
        ObbActionHandler obbActionHandler = this.mObbActionHandler;
        obbActionHandler.sendMessage(obbActionHandler.obtainMessage(1, action));
    }

    public void unmountObb(String rawPath, boolean force, IObbActionListener token, int nonce) {
        ObbState existingState;
        Objects.requireNonNull(rawPath, "rawPath cannot be null");
        synchronized (this.mObbMounts) {
            existingState = this.mObbPathToStateMap.get(rawPath);
        }
        if (existingState != null) {
            int callingUid = Binder.getCallingUid();
            ObbState newState = new ObbState(rawPath, existingState.canonicalPath, callingUid, token, nonce, existingState.volId);
            ObbAction action = new UnmountObbAction(newState, force);
            ObbActionHandler obbActionHandler = this.mObbActionHandler;
            obbActionHandler.sendMessage(obbActionHandler.obtainMessage(1, action));
            return;
        }
        Slog.w(TAG, "Unknown OBB mount at " + rawPath);
    }

    public boolean supportsCheckpoint() throws RemoteException {
        return this.mVold.supportsCheckpoint();
    }

    public void startCheckpoint(int numTries) throws RemoteException {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1000 && callingUid != 0 && callingUid != 2000) {
            throw new SecurityException("no permission to start filesystem checkpoint");
        }
        this.mVold.startCheckpoint(numTries);
    }

    public void commitChanges() throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("no permission to commit checkpoint changes");
        }
        this.mVold.commitChanges();
    }

    public boolean needsCheckpoint() throws RemoteException {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        return this.mVold.needsCheckpoint();
    }

    public void abortChanges(String message, boolean retry) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("no permission to commit checkpoint changes");
        }
        this.mVold.abortChanges(message, retry);
    }

    public void createUserKey(int userId, int serialNumber, boolean ephemeral) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        try {
            if (Build.IS_DEBUG_ENABLE) {
                Slog.i(TAG, "create User Key  for user：" + userId);
            }
            this.mVold.createUserKey(userId, serialNumber, ephemeral);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void destroyUserKey(int userId) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        try {
            if (Build.IS_DEBUG_ENABLE) {
                Slog.i(TAG, "destroy User Key  for user：" + userId);
            }
            this.mVold.destroyUserKey(userId);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    private String encodeBytes(byte[] bytes) {
        if (ArrayUtils.isEmpty(bytes)) {
            return "!";
        }
        return HexDump.toHexString(bytes);
    }

    public void addUserKeyAuth(int userId, int serialNumber, byte[] secret) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        try {
            this.mVold.addUserKeyAuth(userId, serialNumber, encodeBytes(secret));
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void clearUserKeyAuth(int userId, int serialNumber, byte[] secret) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        try {
            this.mVold.clearUserKeyAuth(userId, serialNumber, encodeBytes(secret));
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void fixateNewestUserKeyAuth(int userId) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        try {
            this.mVold.fixateNewestUserKeyAuth(userId);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void unlockUserKey(int userId, int serialNumber, byte[] secret) {
        boolean isFsEncrypted = StorageManager.isFileEncryptedNativeOrEmulated();
        Slog.d(TAG, "unlockUserKey: " + userId + " isFileEncryptedNativeOrEmulated: " + isFsEncrypted + " hasSecret: " + (secret != null));
        enforcePermission("android.permission.STORAGE_INTERNAL");
        if (isUserKeyUnlocked(userId)) {
            Slog.d(TAG, "User " + userId + "'s CE storage is already unlocked");
            return;
        }
        if (isFsEncrypted) {
            if (this.mLockPatternUtils.isSecure(userId) && ArrayUtils.isEmpty(secret)) {
                Slog.d(TAG, "Not unlocking user " + userId + "'s CE storage yet because a secret is needed");
                return;
            }
            try {
                this.mVold.unlockUserKey(userId, serialNumber, encodeBytes(secret));
            } catch (Exception e) {
                Slog.wtf(TAG, e);
                return;
            }
        }
        synchronized (this.mLock) {
            this.mLocalUnlockedUsers.append(userId);
        }
    }

    public void lockUserKey(int userId) {
        if (userId == 0 && UserManager.isHeadlessSystemUserMode()) {
            throw new IllegalArgumentException("Headless system user data cannot be locked..");
        }
        enforcePermission("android.permission.STORAGE_INTERNAL");
        if (!isUserKeyUnlocked(userId)) {
            Slog.d(TAG, "User " + userId + "'s CE storage is already locked");
            return;
        }
        try {
            this.mVold.lockUserKey(userId);
            synchronized (this.mLock) {
                this.mLocalUnlockedUsers.remove(userId);
            }
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public boolean isUserKeyUnlocked(int userId) {
        boolean contains;
        synchronized (this.mLock) {
            contains = this.mLocalUnlockedUsers.contains(userId);
        }
        return contains;
    }

    private boolean isSystemUnlocked(int userId) {
        boolean contains;
        synchronized (this.mLock) {
            contains = ArrayUtils.contains(this.mSystemUnlockedUsers, userId);
        }
        return contains;
    }

    private void prepareUserStorageIfNeeded(VolumeInfo vol) {
        int flags;
        if (vol.type != 1) {
            return;
        }
        UserManager um = (UserManager) this.mContext.getSystemService(UserManager.class);
        UserManagerInternal umInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        for (UserInfo user : um.getUsers()) {
            if (umInternal.isUserUnlockingOrUnlocked(user.id)) {
                flags = 3;
            } else {
                int flags2 = user.id;
                if (umInternal.isUserRunning(flags2)) {
                    flags = 1;
                }
            }
            prepareUserStorageInternal(vol.fsUuid, user.id, user.serialNumber, flags);
        }
    }

    public void prepareUserStorage(String volumeUuid, int userId, int serialNumber, int flags) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        prepareUserStorageInternal(volumeUuid, userId, serialNumber, flags);
    }

    private void prepareUserStorageInternal(String volumeUuid, int userId, int serialNumber, int flags) {
        try {
            this.mVold.prepareUserStorage(volumeUuid, userId, serialNumber, flags);
            if (volumeUuid != null) {
                StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
                VolumeInfo info = storage.findVolumeByUuid(volumeUuid);
                if (info != null && userId == 0 && info.type == 1) {
                    this.mInstaller.tryMountDataMirror(volumeUuid);
                }
            }
        } catch (Exception e) {
            EventLog.writeEvent(1397638484, "224585613", -1, "");
            Slog.wtf(TAG, e);
            UserManagerInternal umInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
            if (umInternal.shouldIgnorePrepareStorageErrors(userId)) {
                Slog.wtf(TAG, "ignoring error preparing storage for existing user " + userId + "; device may be insecure!");
                return;
            }
            throw new RuntimeException(e);
        }
    }

    public void destroyUserStorage(String volumeUuid, int userId, int flags) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        try {
            this.mVold.destroyUserStorage(volumeUuid, userId, flags);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void fixupAppDir(String path) {
        Matcher matcher = KNOWN_APP_DIR_PATHS.matcher(path);
        if (matcher.matches()) {
            if (matcher.group(2) == null) {
                Log.e(TAG, "Asked to fixup an app dir without a userId: " + path);
                return;
            }
            try {
                int userId = Integer.parseInt(matcher.group(2));
                String packageName = matcher.group(3);
                int uid = this.mContext.getPackageManager().getPackageUidAsUser(packageName, userId);
                try {
                    this.mVold.fixupAppDir(path + SliceClientPermissions.SliceAuthority.DELIMITER, uid);
                    return;
                } catch (RemoteException | ServiceSpecificException e) {
                    Log.e(TAG, "Failed to fixup app dir for " + packageName, e);
                    return;
                }
            } catch (PackageManager.NameNotFoundException e2) {
                Log.e(TAG, "Couldn't find package to fixup app dir " + path, e2);
                return;
            } catch (NumberFormatException e3) {
                Log.e(TAG, "Invalid userId in path: " + path, e3);
                return;
            }
        }
        Log.e(TAG, "Path " + path + " is not a valid application-specific directory");
    }

    public void disableAppDataIsolation(String pkgName, int pid, int userId) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 0 && callingUid != 2000) {
            throw new SecurityException("no permission to enable app visibility");
        }
        String[] sharedPackages = this.mPmInternal.getSharedUserPackagesForPackage(pkgName, userId);
        int uid = this.mPmInternal.getPackageUid(pkgName, 0L, userId);
        String[] packages = sharedPackages.length != 0 ? sharedPackages : new String[]{pkgName};
        try {
            this.mVold.unmountAppStorageDirs(uid, pid, packages);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    public PendingIntent getManageSpaceActivityIntent(String packageName, int requestCode) {
        int originalUid = Binder.getCallingUidOrThrow();
        try {
            String[] packagesFromUid = this.mIPackageManager.getPackagesForUid(originalUid);
            if (packagesFromUid == null) {
                throw new SecurityException("Unknown uid " + originalUid);
            }
            if (!this.mStorageManagerInternal.hasExternalStorageAccess(originalUid, packagesFromUid[0])) {
                throw new SecurityException("Only File Manager Apps permitted");
            }
            try {
                ApplicationInfo appInfo = this.mIPackageManager.getApplicationInfo(packageName, 0L, UserHandle.getUserId(originalUid));
                if (appInfo == null) {
                    throw new IllegalArgumentException("Invalid packageName");
                }
                if (appInfo.manageSpaceActivityName == null) {
                    Log.i(TAG, packageName + " doesn't have a manageSpaceActivity");
                    return null;
                }
                long token = Binder.clearCallingIdentity();
                try {
                    try {
                        Context targetAppContext = this.mContext.createPackageContext(packageName, 0);
                        Intent intent = new Intent("android.intent.action.VIEW");
                        intent.setClassName(packageName, appInfo.manageSpaceActivityName);
                        intent.setFlags(268435456);
                        ActivityOptions options = ActivityOptions.makeBasic();
                        options.setIgnorePendingIntentCreatorForegroundState(true);
                        PendingIntent activity = PendingIntent.getActivity(targetAppContext, requestCode, intent, 1409286144, options.toBundle());
                        return activity;
                    } catch (PackageManager.NameNotFoundException e) {
                        throw new IllegalArgumentException("packageName not found");
                    }
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            } catch (RemoteException e2) {
                throw new SecurityException("Only File Manager Apps permitted");
            }
        } catch (RemoteException re) {
            throw new SecurityException("Unknown uid " + originalUid, re);
        }
    }

    public void notifyAppIoBlocked(String volumeUuid, int uid, int tid, int reason) {
        enforceExternalStorageService();
        this.mStorageSessionController.notifyAppIoBlocked(volumeUuid, uid, tid, reason);
    }

    public void notifyAppIoResumed(String volumeUuid, int uid, int tid, int reason) {
        enforceExternalStorageService();
        this.mStorageSessionController.notifyAppIoResumed(volumeUuid, uid, tid, reason);
    }

    public boolean isAppIoBlocked(String volumeUuid, int uid, int tid, int reason) {
        return isAppIoBlocked(uid);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isAppIoBlocked(int uid) {
        return this.mStorageSessionController.isAppIoBlocked(uid);
    }

    public void setCloudMediaProvider(String authority) {
        enforceExternalStorageService();
        int userId = UserHandle.getUserId(Binder.getCallingUid());
        synchronized (this.mCloudMediaProviders) {
            String oldAuthority = this.mCloudMediaProviders.get(userId);
            if (!Objects.equals(authority, oldAuthority)) {
                this.mCloudMediaProviders.put(userId, authority);
                this.mHandler.obtainMessage(16, userId, 0, authority).sendToTarget();
            }
        }
    }

    public String getCloudMediaProvider() {
        String authority;
        ProviderInfo pi;
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(callingUid);
        synchronized (this.mCloudMediaProviders) {
            authority = this.mCloudMediaProviders.get(userId);
        }
        if (authority == null || (pi = this.mPmInternal.resolveContentProvider(authority, 0L, userId, callingUid)) == null || this.mPmInternal.filterAppAccess(pi.packageName, callingUid, userId)) {
            return null;
        }
        return authority;
    }

    private void enforceExternalStorageService() {
        enforcePermission("android.permission.WRITE_MEDIA_STORAGE");
        int callingAppId = UserHandle.getAppId(Binder.getCallingUid());
        if (callingAppId != this.mMediaStoreAuthorityAppId) {
            throw new SecurityException("Only the ExternalStorageService is permitted");
        }
    }

    /* loaded from: classes.dex */
    class AppFuseMountScope extends AppFuseBridge.MountScope {
        private boolean mMounted;

        public AppFuseMountScope(int uid, int mountId) {
            super(uid, mountId);
            this.mMounted = false;
        }

        @Override // com.android.server.storage.AppFuseBridge.MountScope
        public ParcelFileDescriptor open() throws AppFuseMountException {
            try {
                FileDescriptor fd = StorageManagerService.this.mVold.mountAppFuse(this.uid, this.mountId);
                this.mMounted = true;
                return new ParcelFileDescriptor(fd);
            } catch (Exception e) {
                throw new AppFuseMountException("Failed to mount", e);
            }
        }

        @Override // com.android.server.storage.AppFuseBridge.MountScope
        public ParcelFileDescriptor openFile(int mountId, int fileId, int flags) throws AppFuseMountException {
            try {
                return new ParcelFileDescriptor(StorageManagerService.this.mVold.openAppFuseFile(this.uid, mountId, fileId, flags));
            } catch (Exception e) {
                throw new AppFuseMountException("Failed to open", e);
            }
        }

        @Override // java.lang.AutoCloseable
        public void close() throws Exception {
            if (this.mMounted) {
                StorageManagerService.this.mVold.unmountAppFuse(this.uid, this.mountId);
                this.mMounted = false;
            }
        }
    }

    public AppFuseMount mountProxyFileDescriptorBridge() {
        AppFuseMount appFuseMount;
        Slog.v(TAG, "mountProxyFileDescriptorBridge");
        int uid = Binder.getCallingUid();
        while (true) {
            synchronized (this.mAppFuseLock) {
                boolean newlyCreated = false;
                if (this.mAppFuseBridge == null) {
                    this.mAppFuseBridge = new AppFuseBridge();
                    new Thread(this.mAppFuseBridge, AppFuseBridge.TAG).start();
                    newlyCreated = true;
                }
                try {
                    int name = this.mNextAppFuseName;
                    this.mNextAppFuseName = name + 1;
                    try {
                        appFuseMount = new AppFuseMount(name, this.mAppFuseBridge.addBridge(new AppFuseMountScope(uid, name)));
                    } catch (FuseUnavailableMountException e) {
                        if (newlyCreated) {
                            Slog.e(TAG, "", e);
                            return null;
                        }
                        this.mAppFuseBridge = null;
                    }
                } catch (AppFuseMountException e2) {
                    throw e2.rethrowAsParcelableException();
                }
            }
            return appFuseMount;
        }
    }

    public ParcelFileDescriptor openProxyFileDescriptor(int mountId, int fileId, int mode) {
        Slog.v(TAG, "mountProxyFileDescriptor");
        int mode2 = mode & 805306368;
        try {
            synchronized (this.mAppFuseLock) {
                AppFuseBridge appFuseBridge = this.mAppFuseBridge;
                if (appFuseBridge == null) {
                    Slog.e(TAG, "FuseBridge has not been created");
                    return null;
                }
                return appFuseBridge.openFile(mountId, fileId, mode2);
            }
        } catch (FuseUnavailableMountException | InterruptedException e) {
            Slog.v(TAG, "The mount point has already been invalid", e);
            return null;
        }
    }

    public void mkdirs(String callingPkg, String appPath) {
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(callingUid);
        String propertyName = "sys.user." + userId + ".ce_available";
        if (!isUserKeyUnlocked(userId)) {
            throw new IllegalStateException("Failed to prepare " + appPath);
        }
        if (userId == 0 && !SystemProperties.getBoolean(propertyName, false)) {
            throw new IllegalStateException("Failed to prepare " + appPath);
        }
        AppOpsManager appOps = (AppOpsManager) this.mContext.getSystemService("appops");
        appOps.checkPackage(callingUid, callingPkg);
        try {
            PackageManager.Property noAppStorageProp = this.mContext.getPackageManager().getProperty("android.internal.PROPERTY_NO_APP_DATA_STORAGE", callingPkg);
            if (noAppStorageProp != null && noAppStorageProp.getBoolean()) {
                throw new SecurityException(callingPkg + " should not have " + appPath);
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        try {
            File appFile = new File(appPath).getCanonicalFile();
            String appPath2 = appFile.getAbsolutePath();
            if (!appPath2.endsWith(SliceClientPermissions.SliceAuthority.DELIMITER)) {
                appPath2 = appPath2 + SliceClientPermissions.SliceAuthority.DELIMITER;
            }
            Matcher matcher = KNOWN_APP_DIR_PATHS.matcher(appPath2);
            if (matcher.matches()) {
                if (!matcher.group(3).equals(callingPkg)) {
                    throw new SecurityException("Invalid mkdirs path: " + appFile + " does not contain calling package " + callingPkg);
                }
                if ((matcher.group(2) != null && !matcher.group(2).equals(Integer.toString(userId))) || (matcher.group(2) == null && userId != this.mCurrentUserId)) {
                    throw new SecurityException("Invalid mkdirs path: " + appFile + " does not match calling user id " + userId);
                }
                try {
                    this.mVold.setupAppDir(appPath2, callingUid);
                    return;
                } catch (RemoteException e2) {
                    throw new IllegalStateException("Failed to prepare " + appPath2 + ": " + e2);
                }
            }
            throw new SecurityException("Invalid mkdirs path: " + appFile + " is not a known app path.");
        } catch (IOException e3) {
            throw new IllegalStateException("Failed to resolve " + appPath + ": " + e3);
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:102:0x01e6
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4001=11] */
    public android.os.storage.StorageVolume[] getVolumeList(int r50, java.lang.String r51, int r52) {
        /*
            r49 = this;
            r1 = r49
            r2 = r50
            r3 = r51
            r4 = r52
            int r5 = android.os.Binder.getCallingUid()
            int r6 = android.os.UserHandle.getUserId(r5)
            boolean r0 = r1.isUidOwnerOfPackageOrSystem(r3, r5)
            if (r0 == 0) goto L45e
            if (r6 == r2) goto L21
            android.content.Context r0 = r1.mContext
            java.lang.String r7 = "android.permission.INTERACT_ACROSS_USERS"
            java.lang.String r8 = "Need INTERACT_ACROSS_USERS to get volumes for another user"
            r0.enforceCallingOrSelfPermission(r7, r8)
        L21:
            r0 = r4 & 256(0x100, float:3.59E-43)
            r8 = 0
            if (r0 == 0) goto L28
            r0 = 1
            goto L29
        L28:
            r0 = r8
        L29:
            r9 = r0
            r0 = r4 & 512(0x200, float:7.175E-43)
            if (r0 == 0) goto L30
            r0 = 1
            goto L31
        L30:
            r0 = r8
        L31:
            r10 = r0
            r0 = r4 & 1024(0x400, float:1.435E-42)
            if (r0 == 0) goto L38
            r0 = 1
            goto L39
        L38:
            r0 = r8
        L39:
            r11 = r0
            r0 = r4 & 2048(0x800, float:2.87E-42)
            if (r0 == 0) goto L40
            r0 = 1
            goto L41
        L40:
            r0 = r8
        L41:
            r12 = r0
            r0 = r4 & 4096(0x1000, float:5.74E-42)
            if (r0 == 0) goto L48
            r0 = 1
            goto L49
        L48:
            r0 = r8
        L49:
            r13 = r0
            if (r13 == 0) goto L9a
            android.content.pm.IPackageManager r0 = r1.mIPackageManager     // Catch: android.os.RemoteException -> L80
            java.lang.String[] r0 = r0.getPackagesForUid(r5)     // Catch: android.os.RemoteException -> L80
            if (r0 == 0) goto L67
            com.android.server.StorageManagerService$StorageManagerInternalImpl r14 = r1.mStorageManagerInternal     // Catch: android.os.RemoteException -> L80
            r15 = r0[r8]     // Catch: android.os.RemoteException -> L80
            boolean r14 = r14.hasExternalStorageAccess(r5, r15)     // Catch: android.os.RemoteException -> L80
            if (r14 == 0) goto L5f
            goto L9a
        L5f:
            java.lang.SecurityException r7 = new java.lang.SecurityException     // Catch: android.os.RemoteException -> L80
            java.lang.String r8 = "Only File Manager Apps permitted"
            r7.<init>(r8)     // Catch: android.os.RemoteException -> L80
            throw r7     // Catch: android.os.RemoteException -> L80
        L67:
            java.lang.SecurityException r7 = new java.lang.SecurityException     // Catch: android.os.RemoteException -> L80
            java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch: android.os.RemoteException -> L80
            r8.<init>()     // Catch: android.os.RemoteException -> L80
            java.lang.String r14 = "Unknown uid "
            java.lang.StringBuilder r8 = r8.append(r14)     // Catch: android.os.RemoteException -> L80
            java.lang.StringBuilder r8 = r8.append(r5)     // Catch: android.os.RemoteException -> L80
            java.lang.String r8 = r8.toString()     // Catch: android.os.RemoteException -> L80
            r7.<init>(r8)     // Catch: android.os.RemoteException -> L80
            throw r7     // Catch: android.os.RemoteException -> L80
        L80:
            r0 = move-exception
            java.lang.SecurityException r7 = new java.lang.SecurityException
            java.lang.StringBuilder r8 = new java.lang.StringBuilder
            r8.<init>()
            java.lang.String r14 = "Unknown uid "
            java.lang.StringBuilder r8 = r8.append(r14)
            java.lang.StringBuilder r8 = r8.append(r5)
            java.lang.String r8 = r8.toString()
            r7.<init>(r8, r0)
            throw r7
        L9a:
            boolean r14 = r1.isSystemUnlocked(r8)
            int r0 = r1.mMediaStoreAuthorityAppId
            boolean r15 = android.os.UserHandle.isSameApp(r5, r0)
            long r16 = android.os.Binder.clearCallingIdentity()
            java.lang.Class<com.android.server.pm.UserManagerInternal> r0 = com.android.server.pm.UserManagerInternal.class
            java.lang.Object r0 = com.android.server.LocalServices.getService(r0)     // Catch: java.lang.Throwable -> L451
            com.android.server.pm.UserManagerInternal r0 = (com.android.server.pm.UserManagerInternal) r0     // Catch: java.lang.Throwable -> L451
            android.content.pm.UserInfo r0 = r0.getUserInfo(r2)     // Catch: java.lang.Throwable -> L451
            boolean r0 = r0.isDemo()     // Catch: java.lang.Throwable -> L451
            r18 = r0
            com.android.server.StorageManagerService$StorageManagerInternalImpl r0 = r1.mStorageManagerInternal     // Catch: java.lang.Throwable -> L451
            boolean r0 = r0.hasExternalStorage(r5, r3)     // Catch: java.lang.Throwable -> L451
            r19 = r0
            boolean r0 = r49.isUserKeyUnlocked(r50)     // Catch: java.lang.Throwable -> L451
            r20 = r0
            android.os.Binder.restoreCallingIdentity(r16)
            r0 = 0
            java.util.ArrayList r21 = new java.util.ArrayList
            r21.<init>()
            r22 = r21
            android.util.ArraySet r21 = new android.util.ArraySet
            r21.<init>()
            r23 = r21
            java.util.HashMap<java.lang.Integer, java.lang.Integer> r7 = r1.mUserSharesMediaWith
            java.lang.Integer r8 = java.lang.Integer.valueOf(r50)
            r24 = -1
            r25 = r0
            java.lang.Integer r0 = java.lang.Integer.valueOf(r24)
            java.lang.Object r0 = r7.getOrDefault(r8, r0)
            java.lang.Integer r0 = (java.lang.Integer) r0
            int r7 = r0.intValue()
            java.lang.Object r8 = r1.mLock
            monitor-enter(r8)
            r0 = 0
        Lf7:
            android.util.ArrayMap<java.lang.String, android.os.storage.VolumeInfo> r3 = r1.mVolumes     // Catch: java.lang.Throwable -> L43e
            int r3 = r3.size()     // Catch: java.lang.Throwable -> L43e
            if (r0 >= r3) goto L317
            android.util.ArrayMap<java.lang.String, android.os.storage.VolumeInfo> r3 = r1.mVolumes     // Catch: java.lang.Throwable -> L306
            java.lang.Object r3 = r3.keyAt(r0)     // Catch: java.lang.Throwable -> L306
            java.lang.String r3 = (java.lang.String) r3     // Catch: java.lang.Throwable -> L306
            android.util.ArrayMap<java.lang.String, android.os.storage.VolumeInfo> r4 = r1.mVolumes     // Catch: java.lang.Throwable -> L306
            java.lang.Object r4 = r4.valueAt(r0)     // Catch: java.lang.Throwable -> L306
            android.os.storage.VolumeInfo r4 = (android.os.storage.VolumeInfo) r4     // Catch: java.lang.Throwable -> L306
            int r24 = r4.getType()     // Catch: java.lang.Throwable -> L306
            switch(r24) {
                case 0: goto L155;
                case 2: goto L126;
                case 5: goto L155;
                default: goto L116;
            }
        L116:
            r26 = r3
            r24 = r5
            r28 = r6
            r29 = r7
            r3 = r22
            r5 = r23
            r22 = r4
            goto L2f3
        L126:
            r24 = r5
            int r5 = r4.getMountUserId()     // Catch: java.lang.Throwable -> L146
            if (r5 != r2) goto L12f
            goto L157
        L12f:
            if (r13 == 0) goto L138
            int r5 = r4.getMountUserId()     // Catch: java.lang.Throwable -> L146
            if (r5 != r7) goto L138
            goto L157
        L138:
            r26 = r3
            r28 = r6
            r29 = r7
            r3 = r22
            r5 = r23
            r22 = r4
            goto L2f3
        L146:
            r0 = move-exception
            r28 = r6
            r29 = r7
            r21 = r9
            r3 = r22
            r5 = r23
            r22 = r10
            goto L44d
        L155:
            r24 = r5
        L157:
            r5 = 0
            if (r9 == 0) goto L171
            boolean r26 = r4.isVisibleForWrite(r2)     // Catch: java.lang.Throwable -> L146
            if (r26 != 0) goto L16c
            if (r13 == 0) goto L169
            boolean r26 = r4.isVisibleForWrite(r7)     // Catch: java.lang.Throwable -> L146
            if (r26 == 0) goto L169
            goto L16c
        L169:
            r26 = 0
            goto L16e
        L16c:
            r26 = 1
        L16e:
            r5 = r26
            goto L195
        L171:
            boolean r26 = r4.isVisibleForUser(r2)     // Catch: java.lang.Throwable -> L2e4
            if (r26 != 0) goto L191
            boolean r26 = r4.isVisible()     // Catch: java.lang.Throwable -> L146
            if (r26 != 0) goto L185
            if (r11 == 0) goto L185
            java.io.File r26 = r4.getPath()     // Catch: java.lang.Throwable -> L146
            if (r26 != 0) goto L191
        L185:
            if (r13 == 0) goto L18e
            boolean r26 = r4.isVisibleForUser(r7)     // Catch: java.lang.Throwable -> L146
            if (r26 == 0) goto L18e
            goto L191
        L18e:
            r26 = 0
            goto L193
        L191:
            r26 = 1
        L193:
            r5 = r26
        L195:
            if (r5 != 0) goto L1a1
            r28 = r6
            r29 = r7
            r3 = r22
            r5 = r23
            goto L2f4
        L1a1:
            r26 = 0
            if (r15 == 0) goto L1ad
            r27 = r5
            r28 = r6
            r29 = r7
            goto L25a
        L1ad:
            if (r14 != 0) goto L1f5
            r26 = 1
            r27 = r5
            java.lang.String r5 = "StorageManagerService"
            r28 = r6
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L1d9
            r6.<init>()     // Catch: java.lang.Throwable -> L1d9
            r29 = r7
            java.lang.String r7 = "Reporting "
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L270
            java.lang.StringBuilder r6 = r6.append(r3)     // Catch: java.lang.Throwable -> L270
            java.lang.String r7 = " unmounted due to system locked"
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L270
            java.lang.String r6 = r6.toString()     // Catch: java.lang.Throwable -> L270
            android.util.Slog.w(r5, r6)     // Catch: java.lang.Throwable -> L270
            r5 = r26
            goto L25c
        L1d9:
            r0 = move-exception
            r29 = r7
            r21 = r9
            r3 = r22
            r5 = r23
            r22 = r10
            goto L44d
        L1e6:
            r0 = move-exception
            r28 = r6
            r29 = r7
            r21 = r9
            r3 = r22
            r5 = r23
            r22 = r10
            goto L44d
        L1f5:
            r27 = r5
            r28 = r6
            r29 = r7
            int r5 = r4.getType()     // Catch: java.lang.Throwable -> L2d9
            r6 = 2
            if (r5 != r6) goto L232
            if (r20 != 0) goto L232
            r26 = 1
            java.lang.String r5 = "StorageManagerService"
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L270
            r6.<init>()     // Catch: java.lang.Throwable -> L270
            java.lang.String r7 = "Reporting "
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L270
            java.lang.StringBuilder r6 = r6.append(r3)     // Catch: java.lang.Throwable -> L270
            java.lang.String r7 = "unmounted due to "
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L270
            java.lang.StringBuilder r6 = r6.append(r2)     // Catch: java.lang.Throwable -> L270
            java.lang.String r7 = " locked"
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L270
            java.lang.String r6 = r6.toString()     // Catch: java.lang.Throwable -> L270
            android.util.Slog.w(r5, r6)     // Catch: java.lang.Throwable -> L270
            r5 = r26
            goto L25c
        L232:
            if (r19 != 0) goto L25a
            if (r10 != 0) goto L25a
            java.lang.String r5 = "StorageManagerService"
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L270
            r6.<init>()     // Catch: java.lang.Throwable -> L270
            java.lang.String r7 = "Reporting "
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L270
            java.lang.StringBuilder r6 = r6.append(r3)     // Catch: java.lang.Throwable -> L270
            java.lang.String r7 = "unmounted due to missing permissions"
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.lang.Throwable -> L270
            java.lang.String r6 = r6.toString()     // Catch: java.lang.Throwable -> L270
            android.util.Slog.w(r5, r6)     // Catch: java.lang.Throwable -> L270
            r26 = 1
            r5 = r26
            goto L25c
        L25a:
            r5 = r26
        L25c:
            r6 = r50
            int r7 = r4.getMountUserId()     // Catch: java.lang.Throwable -> L2d9
            if (r6 == r7) goto L27b
            int r7 = r4.getMountUserId()     // Catch: java.lang.Throwable -> L270
            if (r7 < 0) goto L27b
            int r7 = r4.getMountUserId()     // Catch: java.lang.Throwable -> L270
            r6 = r7
            goto L27b
        L270:
            r0 = move-exception
            r21 = r9
            r3 = r22
            r5 = r23
            r22 = r10
            goto L44d
        L27b:
            android.content.Context r7 = r1.mContext     // Catch: java.lang.Throwable -> L2d9
            android.os.storage.StorageVolume r7 = r4.buildStorageVolume(r7, r6, r5)     // Catch: java.lang.Throwable -> L2d9
            boolean r26 = r4.isPrimary()     // Catch: java.lang.Throwable -> L2d9
            if (r26 == 0) goto L2b4
            r26 = r3
            int r3 = r4.getMountUserId()     // Catch: java.lang.Throwable -> L2a9
            if (r3 != r2) goto L2a4
            r3 = r22
            r22 = r4
            r4 = 0
            r3.add(r4, r7)     // Catch: java.lang.Throwable -> L29b
            r4 = 1
            r25 = r4
            goto L2bd
        L29b:
            r0 = move-exception
            r21 = r9
            r22 = r10
            r5 = r23
            goto L44d
        L2a4:
            r3 = r22
            r22 = r4
            goto L2ba
        L2a9:
            r0 = move-exception
            r3 = r22
            r21 = r9
            r22 = r10
            r5 = r23
            goto L44d
        L2b4:
            r26 = r3
            r3 = r22
            r22 = r4
        L2ba:
            r3.add(r7)     // Catch: java.lang.Throwable -> L2d0
        L2bd:
            java.lang.String r4 = r7.getUuid()     // Catch: java.lang.Throwable -> L2d0
            r30 = r5
            r5 = r23
            r5.add(r4)     // Catch: java.lang.Throwable -> L2c9
            goto L2f4
        L2c9:
            r0 = move-exception
            r21 = r9
            r22 = r10
            goto L44d
        L2d0:
            r0 = move-exception
            r5 = r23
            r21 = r9
            r22 = r10
            goto L44d
        L2d9:
            r0 = move-exception
            r3 = r22
            r5 = r23
            r21 = r9
            r22 = r10
            goto L44d
        L2e4:
            r0 = move-exception
            r28 = r6
            r29 = r7
            r3 = r22
            r5 = r23
            r21 = r9
            r22 = r10
            goto L44d
        L2f3:
        L2f4:
            int r0 = r0 + 1
            r4 = r52
            r22 = r3
            r23 = r5
            r5 = r24
            r6 = r28
            r7 = r29
            r3 = r51
            goto Lf7
        L306:
            r0 = move-exception
            r24 = r5
            r28 = r6
            r29 = r7
            r3 = r22
            r5 = r23
            r21 = r9
            r22 = r10
            goto L44d
        L317:
            r24 = r5
            r28 = r6
            r29 = r7
            r3 = r22
            r5 = r23
            if (r12 == 0) goto L382
            long r6 = java.lang.System.currentTimeMillis()     // Catch: java.lang.Throwable -> L37b
            r21 = 604800000(0x240c8400, double:2.988109026E-315)
            long r6 = r6 - r21
            r0 = 0
        L32d:
            android.util.ArrayMap<java.lang.String, android.os.storage.VolumeRecord> r4 = r1.mRecords     // Catch: java.lang.Throwable -> L37b
            int r4 = r4.size()     // Catch: java.lang.Throwable -> L37b
            if (r0 >= r4) goto L376
            android.util.ArrayMap<java.lang.String, android.os.storage.VolumeRecord> r4 = r1.mRecords     // Catch: java.lang.Throwable -> L37b
            java.lang.Object r4 = r4.valueAt(r0)     // Catch: java.lang.Throwable -> L37b
            android.os.storage.VolumeRecord r4 = (android.os.storage.VolumeRecord) r4     // Catch: java.lang.Throwable -> L37b
            r21 = r9
            java.lang.String r9 = r4.fsUuid     // Catch: java.lang.Throwable -> L371
            boolean r9 = r5.contains(r9)     // Catch: java.lang.Throwable -> L371
            if (r9 == 0) goto L34a
            r22 = r10
            goto L36a
        L34a:
            r22 = r10
            long r9 = r4.lastSeenMillis     // Catch: java.lang.Throwable -> L44f
            r26 = 0
            int r9 = (r9 > r26 ? 1 : (r9 == r26 ? 0 : -1))
            if (r9 <= 0) goto L36a
            long r9 = r4.lastSeenMillis     // Catch: java.lang.Throwable -> L44f
            int r9 = (r9 > r6 ? 1 : (r9 == r6 ? 0 : -1))
            if (r9 >= 0) goto L36a
            android.content.Context r9 = r1.mContext     // Catch: java.lang.Throwable -> L44f
            android.os.storage.StorageVolume r9 = r4.buildStorageVolume(r9)     // Catch: java.lang.Throwable -> L44f
            r3.add(r9)     // Catch: java.lang.Throwable -> L44f
            java.lang.String r10 = r9.getUuid()     // Catch: java.lang.Throwable -> L44f
            r5.add(r10)     // Catch: java.lang.Throwable -> L44f
        L36a:
            int r0 = r0 + 1
            r9 = r21
            r10 = r22
            goto L32d
        L371:
            r0 = move-exception
            r22 = r10
            goto L44d
        L376:
            r21 = r9
            r22 = r10
            goto L386
        L37b:
            r0 = move-exception
            r21 = r9
            r22 = r10
            goto L44d
        L382:
            r21 = r9
            r22 = r10
        L386:
            monitor-exit(r8)     // Catch: java.lang.Throwable -> L44f
            if (r18 == 0) goto L3d3
            java.lang.String r4 = "demo"
            java.io.File r6 = android.os.Environment.getDataPreloadsMediaDirectory()
            r7 = 0
            r8 = 0
            r9 = 1
            r10 = 0
            r23 = 0
            r26 = 0
            android.os.UserHandle r0 = new android.os.UserHandle
            r0.<init>(r2)
            r42 = r0
            java.lang.String r0 = "mounted_ro"
            r47 = r0
            android.content.Context r0 = r1.mContext
            r48 = r4
            r4 = 17039374(0x104000e, float:2.424461E-38)
            java.lang.String r0 = r0.getString(r4)
            android.os.storage.StorageVolume r4 = new android.os.storage.StorageVolume
            java.lang.String r31 = "demo"
            r35 = 0
            r36 = 0
            r37 = 1
            r38 = 0
            r39 = 0
            r40 = 0
            r43 = 0
            java.lang.String r44 = "demo"
            java.lang.String r45 = "mounted_ro"
            r30 = r4
            r32 = r6
            r33 = r6
            r34 = r0
            r30.<init>(r31, r32, r33, r34, r35, r36, r37, r38, r39, r40, r42, r43, r44, r45)
            r3.add(r4)
        L3d3:
            if (r25 != 0) goto L431
            java.lang.String r0 = "StorageManagerService"
            java.lang.String r4 = "No primary storage defined yet; hacking together a stub"
            android.util.Slog.w(r0, r4)
            java.lang.String r0 = "ro.vold.primary_physical"
            r4 = 0
            boolean r0 = android.os.SystemProperties.getBoolean(r0, r4)
            java.lang.String r4 = "stub_primary"
            java.io.File r6 = android.os.Environment.getLegacyExternalStorageDirectory()
            android.content.Context r7 = r1.mContext
            r8 = 17039374(0x104000e, float:2.424461E-38)
            java.lang.String r7 = r7.getString(r8)
            r8 = 1
            r36 = r0
            r37 = r0 ^ 1
            r9 = 0
            r10 = 0
            r26 = 0
            r23 = r0
            android.os.UserHandle r0 = new android.os.UserHandle
            r0.<init>(r2)
            r42 = r0
            r0 = 0
            r46 = 0
            java.lang.String r47 = "removed"
            android.os.storage.StorageVolume r1 = new android.os.storage.StorageVolume
            java.lang.String r31 = "stub_primary"
            r35 = 1
            r38 = 0
            r39 = 0
            r40 = 0
            java.lang.String r45 = "removed"
            r30 = r1
            r32 = r6
            r33 = r6
            r34 = r7
            r43 = r46
            r44 = r0
            r30.<init>(r31, r32, r33, r34, r35, r36, r37, r38, r39, r40, r42, r43, r44, r45)
            r30 = r0
            r0 = 0
            r3.add(r0, r1)
        L431:
            int r0 = r3.size()
            android.os.storage.StorageVolume[] r0 = new android.os.storage.StorageVolume[r0]
            java.lang.Object[] r0 = r3.toArray(r0)
            android.os.storage.StorageVolume[] r0 = (android.os.storage.StorageVolume[]) r0
            return r0
        L43e:
            r0 = move-exception
            r24 = r5
            r28 = r6
            r29 = r7
            r21 = r9
            r3 = r22
            r5 = r23
            r22 = r10
        L44d:
            monitor-exit(r8)     // Catch: java.lang.Throwable -> L44f
            throw r0
        L44f:
            r0 = move-exception
            goto L44d
        L451:
            r0 = move-exception
            r24 = r5
            r28 = r6
            r21 = r9
            r22 = r10
            android.os.Binder.restoreCallingIdentity(r16)
            throw r0
        L45e:
            java.lang.SecurityException r0 = new java.lang.SecurityException
            java.lang.String r1 = "callingPackage does not match UID"
            r0.<init>(r1)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.StorageManagerService.getVolumeList(int, java.lang.String, int):android.os.storage.StorageVolume[]");
    }

    public DiskInfo[] getDisks() {
        DiskInfo[] res;
        synchronized (this.mLock) {
            res = new DiskInfo[this.mDisks.size()];
            for (int i = 0; i < this.mDisks.size(); i++) {
                res[i] = this.mDisks.valueAt(i);
            }
        }
        return res;
    }

    public VolumeInfo[] getVolumes(int flags) {
        VolumeInfo[] res;
        synchronized (this.mLock) {
            res = new VolumeInfo[this.mVolumes.size()];
            for (int i = 0; i < this.mVolumes.size(); i++) {
                res[i] = this.mVolumes.valueAt(i);
            }
        }
        return res;
    }

    public VolumeRecord[] getVolumeRecords(int flags) {
        VolumeRecord[] res;
        synchronized (this.mLock) {
            res = new VolumeRecord[this.mRecords.size()];
            for (int i = 0; i < this.mRecords.size(); i++) {
                res[i] = this.mRecords.valueAt(i);
            }
        }
        return res;
    }

    public long getCacheQuotaBytes(String volumeUuid, int uid) {
        if (uid != Binder.getCallingUid()) {
            this.mContext.enforceCallingPermission("android.permission.STORAGE_INTERNAL", TAG);
        }
        long token = Binder.clearCallingIdentity();
        StorageStatsManager stats = (StorageStatsManager) this.mContext.getSystemService(StorageStatsManager.class);
        try {
            return stats.getCacheQuotaBytes(volumeUuid, uid);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public long getCacheSizeBytes(String volumeUuid, int uid) {
        if (uid != Binder.getCallingUid()) {
            this.mContext.enforceCallingPermission("android.permission.STORAGE_INTERNAL", TAG);
        }
        long token = Binder.clearCallingIdentity();
        try {
            try {
                return ((StorageStatsManager) this.mContext.getSystemService(StorageStatsManager.class)).queryStatsForUid(volumeUuid, uid).getCacheBytes();
            } catch (IOException e) {
                throw new ParcelableException(e);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private int adjustAllocateFlags(int flags, int callingUid, String callingPackage) {
        if ((flags & 1) != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.ALLOCATE_AGGRESSIVE", TAG);
        }
        int flags2 = flags & (-3) & (-5);
        AppOpsManager appOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        long token = Binder.clearCallingIdentity();
        try {
            if (appOps.isOperationActive(26, callingUid, callingPackage)) {
                Slog.d(TAG, "UID " + callingUid + " is actively using camera; letting them defy reserved cached data");
                flags2 |= 4;
            }
            return flags2;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4183=4] */
    public long getAllocatableBytes(String volumeUuid, int flags, String callingPackage) {
        int flags2 = adjustAllocateFlags(flags, Binder.getCallingUid(), callingPackage);
        StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        StorageStatsManager stats = (StorageStatsManager) this.mContext.getSystemService(StorageStatsManager.class);
        long token = Binder.clearCallingIdentity();
        try {
            try {
                File path = storage.findPathForUuid(volumeUuid);
                long usable = 0;
                long lowReserved = 0;
                long fullReserved = 0;
                long cacheClearable = 0;
                if ((flags2 & 16) == 0) {
                    usable = path.getUsableSpace();
                    lowReserved = storage.getStorageLowBytes(path);
                    fullReserved = storage.getStorageFullBytes(path);
                }
                long lowReserved2 = lowReserved;
                if ((flags2 & 8) == 0 && stats.isQuotaSupported(volumeUuid)) {
                    long cacheTotal = stats.getCacheBytes(volumeUuid);
                    long cacheReserved = storage.getStorageCacheBytes(path, flags2);
                    cacheClearable = Math.max(0L, cacheTotal - cacheReserved);
                }
                return (flags2 & 1) != 0 ? Math.max(0L, (usable + cacheClearable) - fullReserved) : Math.max(0L, (usable + cacheClearable) - lowReserved2);
            } catch (IOException e) {
                throw new ParcelableException(e);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void allocateBytes(String volumeUuid, long bytes, int flags, String callingPackage) {
        long bytes2;
        int flags2 = adjustAllocateFlags(flags, Binder.getCallingUid(), callingPackage);
        long allocatableBytes = getAllocatableBytes(volumeUuid, flags2 | 8, callingPackage);
        if (bytes > allocatableBytes) {
            long cacheClearable = getAllocatableBytes(volumeUuid, flags2 | 16, callingPackage);
            if (bytes > allocatableBytes + cacheClearable) {
                throw new ParcelableException(new IOException("Failed to allocate " + bytes + " because only " + (allocatableBytes + cacheClearable) + " allocatable"));
            }
        }
        StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        long token = Binder.clearCallingIdentity();
        try {
            try {
                File path = storage.findPathForUuid(volumeUuid);
                if ((flags2 & 1) != 0) {
                    bytes2 = bytes + storage.getStorageFullBytes(path);
                } else {
                    bytes2 = bytes + storage.getStorageLowBytes(path);
                }
                this.mPmInternal.freeStorage(volumeUuid, bytes2, flags2);
            } catch (IOException e) {
                throw new ParcelableException(e);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addObbStateLocked(ObbState obbState) throws RemoteException {
        IBinder binder = obbState.getBinder();
        List<ObbState> obbStates = this.mObbMounts.get(binder);
        if (obbStates == null) {
            obbStates = new ArrayList();
            this.mObbMounts.put(binder, obbStates);
        } else {
            for (ObbState o : obbStates) {
                if (o.rawPath.equals(obbState.rawPath)) {
                    throw new IllegalStateException("Attempt to add ObbState twice. This indicates an error in the StorageManagerService logic.");
                }
            }
        }
        obbStates.add(obbState);
        try {
            obbState.link();
            this.mObbPathToStateMap.put(obbState.rawPath, obbState);
        } catch (RemoteException e) {
            obbStates.remove(obbState);
            if (obbStates.isEmpty()) {
                this.mObbMounts.remove(binder);
            }
            throw e;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeObbStateLocked(ObbState obbState) {
        IBinder binder = obbState.getBinder();
        List<ObbState> obbStates = this.mObbMounts.get(binder);
        if (obbStates != null) {
            if (obbStates.remove(obbState)) {
                obbState.unlink();
            }
            if (obbStates.isEmpty()) {
                this.mObbMounts.remove(binder);
            }
        }
        this.mObbPathToStateMap.remove(obbState.rawPath);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ObbActionHandler extends Handler {
        ObbActionHandler(Looper l) {
            super(l);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ObbAction action = (ObbAction) msg.obj;
                    action.execute(this);
                    return;
                case 2:
                    String path = (String) msg.obj;
                    synchronized (StorageManagerService.this.mObbMounts) {
                        List<ObbState> obbStatesToRemove = new LinkedList<>();
                        for (ObbState state : StorageManagerService.this.mObbPathToStateMap.values()) {
                            if (state.canonicalPath.startsWith(path)) {
                                obbStatesToRemove.add(state);
                            }
                        }
                        for (ObbState obbState : obbStatesToRemove) {
                            StorageManagerService.this.removeObbStateLocked(obbState);
                            try {
                                obbState.token.onObbResult(obbState.rawPath, obbState.nonce, 2);
                            } catch (RemoteException e) {
                                Slog.i(StorageManagerService.TAG, "Couldn't send unmount notification for  OBB: " + obbState.rawPath);
                            }
                        }
                    }
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ObbException extends Exception {
        public final int status;

        public ObbException(int status, String message) {
            super(message);
            this.status = status;
        }

        public ObbException(int status, Throwable cause) {
            super(cause.getMessage(), cause);
            this.status = status;
        }
    }

    /* loaded from: classes.dex */
    abstract class ObbAction {
        ObbState mObbState;

        abstract void handleExecute() throws ObbException;

        ObbAction(ObbState obbState) {
            this.mObbState = obbState;
        }

        public void execute(ObbActionHandler handler) {
            try {
                handleExecute();
            } catch (ObbException e) {
                notifyObbStateChange(e);
            }
        }

        protected void notifyObbStateChange(ObbException e) {
            Slog.w(StorageManagerService.TAG, e);
            notifyObbStateChange(e.status);
        }

        protected void notifyObbStateChange(int status) {
            ObbState obbState = this.mObbState;
            if (obbState == null || obbState.token == null) {
                return;
            }
            try {
                this.mObbState.token.onObbResult(this.mObbState.rawPath, this.mObbState.nonce, status);
            } catch (RemoteException e) {
                Slog.w(StorageManagerService.TAG, "StorageEventListener went away while calling onObbStateChanged");
            }
        }
    }

    /* loaded from: classes.dex */
    class MountObbAction extends ObbAction {
        private final int mCallingUid;
        private ObbInfo mObbInfo;

        MountObbAction(ObbState obbState, int callingUid, ObbInfo obbInfo) {
            super(obbState);
            this.mCallingUid = callingUid;
            this.mObbInfo = obbInfo;
        }

        @Override // com.android.server.StorageManagerService.ObbAction
        public void handleExecute() throws ObbException {
            boolean isMounted;
            StorageManagerService.this.warnOnNotMounted();
            if (!StorageManagerService.this.isUidOwnerOfPackageOrSystem(this.mObbInfo.packageName, this.mCallingUid)) {
                throw new ObbException(25, "Denied attempt to mount OBB " + this.mObbInfo.filename + " which is owned by " + this.mObbInfo.packageName);
            }
            synchronized (StorageManagerService.this.mObbMounts) {
                isMounted = StorageManagerService.this.mObbPathToStateMap.containsKey(this.mObbState.rawPath);
            }
            if (isMounted) {
                throw new ObbException(24, "Attempt to mount OBB which is already mounted: " + this.mObbInfo.filename);
            }
            try {
                this.mObbState.volId = StorageManagerService.this.mVold.createObb(this.mObbState.canonicalPath, this.mObbState.ownerGid);
                StorageManagerService.this.mVold.mount(this.mObbState.volId, 0, -1, null);
                synchronized (StorageManagerService.this.mObbMounts) {
                    StorageManagerService.this.addObbStateLocked(this.mObbState);
                }
                notifyObbStateChange(1);
            } catch (Exception e) {
                throw new ObbException(21, e);
            }
        }

        public String toString() {
            return "MountObbAction{" + this.mObbState + '}';
        }
    }

    /* loaded from: classes.dex */
    class UnmountObbAction extends ObbAction {
        private final boolean mForceUnmount;

        UnmountObbAction(ObbState obbState, boolean force) {
            super(obbState);
            this.mForceUnmount = force;
        }

        @Override // com.android.server.StorageManagerService.ObbAction
        public void handleExecute() throws ObbException {
            ObbState existingState;
            StorageManagerService.this.warnOnNotMounted();
            synchronized (StorageManagerService.this.mObbMounts) {
                existingState = (ObbState) StorageManagerService.this.mObbPathToStateMap.get(this.mObbState.rawPath);
            }
            if (existingState == null) {
                throw new ObbException(23, "Missing existingState");
            }
            if (existingState.ownerGid != this.mObbState.ownerGid) {
                notifyObbStateChange(new ObbException(25, "Permission denied to unmount OBB " + existingState.rawPath + " (owned by GID " + existingState.ownerGid + ")"));
                return;
            }
            try {
                StorageManagerService.this.mVold.unmount(this.mObbState.volId);
                StorageManagerService.this.mVold.destroyObb(this.mObbState.volId);
                this.mObbState.volId = null;
                synchronized (StorageManagerService.this.mObbMounts) {
                    StorageManagerService.this.removeObbStateLocked(existingState);
                }
                notifyObbStateChange(2);
            } catch (Exception e) {
                throw new ObbException(22, e);
            }
        }

        public String toString() {
            return "UnmountObbAction{" + this.mObbState + ",force=" + this.mForceUnmount + '}';
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchOnStatus(IVoldTaskListener listener, int status, PersistableBundle extras) {
        if (listener != null) {
            try {
                listener.onStatus(status, extras);
            } catch (RemoteException e) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchOnFinished(IVoldTaskListener listener, int status, PersistableBundle extras) {
        if (listener != null) {
            try {
                listener.onFinished(status, extras);
            } catch (RemoteException e) {
            }
        }
    }

    public int getExternalStorageMountMode(int uid, String packageName) {
        enforcePermission("android.permission.WRITE_MEDIA_STORAGE");
        return this.mStorageManagerInternal.getExternalStorageMountMode(uid, packageName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getMountModeInternal(int uid, String packageName) {
        ApplicationInfo ai;
        try {
            if (!Process.isIsolated(uid) && !Process.isSdkSandboxUid(uid)) {
                String[] packagesForUid = this.mIPackageManager.getPackagesForUid(uid);
                if (ArrayUtils.isEmpty(packagesForUid)) {
                    return 0;
                }
                if (packageName == null) {
                    packageName = packagesForUid[0];
                }
                long token = Binder.clearCallingIdentity();
                if (this.mPmInternal.isInstantApp(packageName, UserHandle.getUserId(uid))) {
                    Binder.restoreCallingIdentity(token);
                    return 0;
                }
                Binder.restoreCallingIdentity(token);
                if (this.mStorageManagerInternal.isExternalStorageService(uid)) {
                    return 3;
                }
                if (this.mDownloadsAuthorityAppId != UserHandle.getAppId(uid) && this.mExternalStorageAuthorityAppId != UserHandle.getAppId(uid) && !this.mSystemAppAuthorityAppId.contains(Integer.valueOf(UserHandle.getAppId(uid)))) {
                    boolean hasMtp = this.mIPackageManager.checkUidPermission("android.permission.ACCESS_MTP", uid) == 0;
                    if (hasMtp && (ai = this.mIPackageManager.getApplicationInfo(packageName, 0L, UserHandle.getUserId(uid))) != null && ai.isSignedWithPlatformKey()) {
                        return 4;
                    }
                    boolean hasInstall = this.mIPackageManager.checkUidPermission("android.permission.INSTALL_PACKAGES", uid) == 0;
                    boolean hasInstallOp = false;
                    int length = packagesForUid.length;
                    int i = 0;
                    while (true) {
                        if (i >= length) {
                            break;
                        }
                        String uidPackageName = packagesForUid[i];
                        if (this.mIAppOpsService.checkOperation(66, uid, uidPackageName) != 0) {
                            i++;
                        } else {
                            hasInstallOp = true;
                            break;
                        }
                    }
                    return (hasInstall || hasInstallOp) ? 2 : 1;
                }
                return 4;
            }
            return 0;
        } catch (RemoteException e) {
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Callbacks extends Handler {
        private static final int MSG_DISK_DESTROYED = 6;
        private static final int MSG_DISK_SCANNED = 5;
        private static final int MSG_STORAGE_STATE_CHANGED = 1;
        private static final int MSG_VOLUME_FORGOTTEN = 4;
        private static final int MSG_VOLUME_RECORD_CHANGED = 3;
        private static final int MSG_VOLUME_STATE_CHANGED = 2;
        private final RemoteCallbackList<IStorageEventListener> mCallbacks;

        public Callbacks(Looper looper) {
            super(looper);
            this.mCallbacks = new RemoteCallbackList<>();
        }

        public void register(IStorageEventListener callback) {
            this.mCallbacks.register(callback);
        }

        public void unregister(IStorageEventListener callback) {
            this.mCallbacks.unregister(callback);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            SomeArgs args = (SomeArgs) msg.obj;
            int n = this.mCallbacks.beginBroadcast();
            for (int i = 0; i < n; i++) {
                IStorageEventListener callback = this.mCallbacks.getBroadcastItem(i);
                try {
                    invokeCallback(callback, msg.what, args);
                } catch (RemoteException e) {
                }
            }
            this.mCallbacks.finishBroadcast();
            args.recycle();
        }

        private void invokeCallback(IStorageEventListener callback, int what, SomeArgs args) throws RemoteException {
            switch (what) {
                case 1:
                    callback.onStorageStateChanged((String) args.arg1, (String) args.arg2, (String) args.arg3);
                    return;
                case 2:
                    callback.onVolumeStateChanged((VolumeInfo) args.arg1, args.argi2, args.argi3);
                    return;
                case 3:
                    callback.onVolumeRecordChanged((VolumeRecord) args.arg1);
                    return;
                case 4:
                    callback.onVolumeForgotten((String) args.arg1);
                    return;
                case 5:
                    callback.onDiskScanned((DiskInfo) args.arg1, args.argi2);
                    return;
                case 6:
                    callback.onDiskDestroyed((DiskInfo) args.arg1);
                    return;
                default:
                    return;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void notifyStorageStateChanged(String path, String oldState, String newState) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = path;
            args.arg2 = oldState;
            args.arg3 = newState;
            obtainMessage(1, args).sendToTarget();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void notifyVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = vol.clone();
            args.argi2 = oldState;
            args.argi3 = newState;
            obtainMessage(2, args).sendToTarget();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void notifyVolumeRecordChanged(VolumeRecord rec) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = rec.clone();
            obtainMessage(3, args).sendToTarget();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void notifyVolumeForgotten(String fsUuid) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = fsUuid;
            obtainMessage(4, args).sendToTarget();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void notifyDiskScanned(DiskInfo disk, int volumeCount) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = disk.clone();
            args.argi2 = volumeCount;
            obtainMessage(5, args).sendToTarget();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void notifyDiskDestroyed(DiskInfo disk) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = disk.clone();
            obtainMessage(6, args).sendToTarget();
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, writer)) {
            IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ", 160);
            synchronized (this.mLock) {
                pw.println("Disks:");
                pw.increaseIndent();
                for (int i = 0; i < this.mDisks.size(); i++) {
                    DiskInfo disk = this.mDisks.valueAt(i);
                    disk.dump(pw);
                }
                pw.decreaseIndent();
                pw.println();
                pw.println("Volumes:");
                pw.increaseIndent();
                for (int i2 = 0; i2 < this.mVolumes.size(); i2++) {
                    VolumeInfo vol = this.mVolumes.valueAt(i2);
                    if (!"private".equals(vol.id)) {
                        vol.dump(pw);
                    }
                }
                pw.decreaseIndent();
                pw.println();
                pw.println("Records:");
                pw.increaseIndent();
                for (int i3 = 0; i3 < this.mRecords.size(); i3++) {
                    VolumeRecord note = this.mRecords.valueAt(i3);
                    note.dump(pw);
                }
                pw.decreaseIndent();
                pw.println();
                pw.println("Primary storage UUID: " + this.mPrimaryStorageUuid);
                pw.println();
                Pair<String, Long> pair = StorageManager.getPrimaryStoragePathAndSize();
                if (pair == null) {
                    pw.println("Internal storage total size: N/A");
                } else {
                    pw.print("Internal storage (");
                    pw.print((String) pair.first);
                    pw.print(") total size: ");
                    pw.print(pair.second);
                    pw.print(" (");
                    pw.print(DataUnit.MEBIBYTES.toBytes(((Long) pair.second).longValue()));
                    pw.println(" MiB)");
                }
                pw.println();
                pw.println("Local unlocked users: " + this.mLocalUnlockedUsers);
                pw.println("System unlocked users: " + Arrays.toString(this.mSystemUnlockedUsers));
            }
            synchronized (this.mObbMounts) {
                pw.println();
                pw.println("mObbMounts:");
                pw.increaseIndent();
                for (Map.Entry<IBinder, List<ObbState>> e : this.mObbMounts.entrySet()) {
                    pw.println(e.getKey() + ":");
                    pw.increaseIndent();
                    List<ObbState> obbStates = e.getValue();
                    for (ObbState obbState : obbStates) {
                        pw.println(obbState);
                    }
                    pw.decreaseIndent();
                }
                pw.decreaseIndent();
                pw.println();
                pw.println("mObbPathToStateMap:");
                pw.increaseIndent();
                for (Map.Entry<String, ObbState> e2 : this.mObbPathToStateMap.entrySet()) {
                    pw.print(e2.getKey());
                    pw.print(" -> ");
                    pw.println(e2.getValue());
                }
                pw.decreaseIndent();
            }
            synchronized (this.mCloudMediaProviders) {
                pw.println();
                pw.print("Media cloud providers: ");
                pw.println(this.mCloudMediaProviders);
            }
            pw.println();
            pw.print("Last maintenance: ");
            pw.println(TimeUtils.formatForLogging(this.mLastMaintenance));
        }
    }

    @Override // com.android.server.Watchdog.Monitor
    public void monitor() {
        try {
            this.mVold.monitor();
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class StorageManagerInternalImpl extends StorageManagerInternal {
        private final CopyOnWriteArraySet<StorageManagerInternal.CloudProviderChangeListener> mCloudProviderChangeListeners;
        private final List<StorageManagerInternal.ResetListener> mResetListeners;

        private StorageManagerInternalImpl() {
            this.mResetListeners = new ArrayList();
            this.mCloudProviderChangeListeners = new CopyOnWriteArraySet<>();
        }

        public boolean isFuseMounted(int userId) {
            boolean contains;
            synchronized (StorageManagerService.this.mLock) {
                contains = StorageManagerService.this.mFuseMountedUser.contains(Integer.valueOf(userId));
            }
            return contains;
        }

        public boolean prepareStorageDirs(int userId, Set<String> packageList, String processName) {
            synchronized (StorageManagerService.this.mLock) {
                if (!StorageManagerService.this.mFuseMountedUser.contains(Integer.valueOf(userId))) {
                    Slog.w(StorageManagerService.TAG, "User " + userId + " is not unlocked yet so skip mounting obb");
                    return false;
                }
                try {
                    IVold vold = IVold.Stub.asInterface(ServiceManager.getServiceOrThrow("vold"));
                    for (String pkg : packageList) {
                        String packageObbDir = String.format(Locale.US, "/storage/emulated/%d/Android/obb/%s/", Integer.valueOf(userId), pkg);
                        String packageDataDir = String.format(Locale.US, "/storage/emulated/%d/Android/data/%s/", Integer.valueOf(userId), pkg);
                        int appUid = UserHandle.getUid(userId, StorageManagerService.this.mPmInternal.getPackage(pkg).getUid());
                        vold.ensureAppDirsCreated(new String[]{packageObbDir, packageDataDir}, appUid);
                    }
                    return true;
                } catch (ServiceManager.ServiceNotFoundException | RemoteException e) {
                    Slog.e(StorageManagerService.TAG, "Unable to create obb and data directories for " + processName, e);
                    return false;
                }
            }
        }

        public int getExternalStorageMountMode(int uid, String packageName) {
            int mode = StorageManagerService.this.getMountModeInternal(uid, packageName);
            if (StorageManagerService.LOCAL_LOGV) {
                Slog.v(StorageManagerService.TAG, "Resolved mode " + mode + " for " + packageName + SliceClientPermissions.SliceAuthority.DELIMITER + UserHandle.formatUid(uid));
            }
            return mode;
        }

        public boolean hasExternalStorageAccess(int uid, String packageName) {
            try {
                int opMode = StorageManagerService.this.mIAppOpsService.checkOperation(92, uid, packageName);
                return opMode == 3 ? StorageManagerService.this.mIPackageManager.checkUidPermission("android.permission.MANAGE_EXTERNAL_STORAGE", uid) == 0 : opMode == 0;
            } catch (RemoteException e) {
                Slog.w("Failed to check MANAGE_EXTERNAL_STORAGE access for " + packageName, e);
                return false;
            }
        }

        public void addResetListener(StorageManagerInternal.ResetListener listener) {
            synchronized (this.mResetListeners) {
                this.mResetListeners.add(listener);
            }
        }

        public void onReset(IVold vold) {
            synchronized (this.mResetListeners) {
                for (StorageManagerInternal.ResetListener listener : this.mResetListeners) {
                    listener.onReset(vold);
                }
            }
        }

        public void resetUser(int userId) {
            StorageManagerService.this.mHandler.obtainMessage(10).sendToTarget();
        }

        public boolean hasLegacyExternalStorage(int uid) {
            boolean contains;
            synchronized (StorageManagerService.this.mLock) {
                contains = StorageManagerService.this.mUidsWithLegacyExternalStorage.contains(Integer.valueOf(uid));
            }
            return contains;
        }

        public void prepareAppDataAfterInstall(String packageName, int uid) {
            int userId = UserHandle.getUserId(uid);
            Environment.UserEnvironment userEnv = new Environment.UserEnvironment(userId);
            File[] packageObbDirs = userEnv.buildExternalStorageAppObbDirs(packageName);
            for (File packageObbDir : packageObbDirs) {
                if (packageObbDir.getPath().startsWith(Environment.getDataPreloadsMediaDirectory().getPath())) {
                    Slog.i(StorageManagerService.TAG, "Skipping app data preparation for " + packageObbDir);
                } else {
                    try {
                        StorageManagerService.this.mVold.fixupAppDir(packageObbDir.getCanonicalPath() + SliceClientPermissions.SliceAuthority.DELIMITER, uid);
                    } catch (RemoteException | ServiceSpecificException e) {
                        Log.e(StorageManagerService.TAG, "Failed to fixup app dir for " + packageName, e);
                    } catch (IOException e2) {
                        Log.e(StorageManagerService.TAG, "Failed to get canonical path for " + packageName);
                    }
                }
            }
        }

        public boolean isExternalStorageService(int uid) {
            return StorageManagerService.this.mMediaStoreAuthorityAppId == UserHandle.getAppId(uid);
        }

        public void freeCache(String volumeUuid, long freeBytes) {
            try {
                StorageManagerService.this.mStorageSessionController.freeCache(volumeUuid, freeBytes);
            } catch (StorageSessionController.ExternalStorageServiceException e) {
                Log.e(StorageManagerService.TAG, "Failed to free cache of vol : " + volumeUuid, e);
            }
        }

        public boolean hasExternalStorage(int uid, String packageName) {
            return uid == 1000 || getExternalStorageMountMode(uid, packageName) != 0;
        }

        private void killAppForOpChange(int code, int uid) {
            IActivityManager am = ActivityManager.getService();
            if (uid == 1000) {
                Slog.d(StorageManagerService.TAG, "killAppForOpChange SKIP SYSTEM_UID");
                return;
            }
            try {
                am.killUid(UserHandle.getAppId(uid), -1, AppOpsManager.opToName(code) + " changed.");
            } catch (RemoteException e) {
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [5029=6] */
        public void onAppOpsChanged(int code, int uid, String packageName, int mode, int previousMode) {
            long token = Binder.clearCallingIdentity();
            try {
                switch (code) {
                    case 66:
                        if (previousMode == 0 && mode != 0) {
                            killAppForOpChange(code, uid);
                        }
                        return;
                    case 87:
                        StorageManagerService.this.updateLegacyStorageApps(packageName, uid, mode == 0);
                        return;
                    case 92:
                        if (mode != 0) {
                            killAppForOpChange(code, uid);
                        }
                        return;
                    default:
                        return;
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public List<String> getPrimaryVolumeIds() {
            List<String> primaryVolumeIds = new ArrayList<>();
            synchronized (StorageManagerService.this.mLock) {
                for (int i = 0; i < StorageManagerService.this.mVolumes.size(); i++) {
                    VolumeInfo vol = StorageManagerService.this.mVolumes.valueAt(i);
                    if (vol.isPrimary()) {
                        primaryVolumeIds.add(vol.getId());
                    }
                }
            }
            return primaryVolumeIds;
        }

        public void markCeStoragePrepared(int userId) {
            synchronized (StorageManagerService.this.mLock) {
                StorageManagerService.this.mCeStoragePreparedUsers.add(Integer.valueOf(userId));
            }
        }

        public boolean isCeStoragePrepared(int userId) {
            boolean contains;
            synchronized (StorageManagerService.this.mLock) {
                contains = StorageManagerService.this.mCeStoragePreparedUsers.contains(Integer.valueOf(userId));
            }
            return contains;
        }

        public void registerCloudProviderChangeListener(StorageManagerInternal.CloudProviderChangeListener listener) {
            this.mCloudProviderChangeListeners.add(listener);
            StorageManagerService.this.mHandler.obtainMessage(16, listener);
        }
    }
}
