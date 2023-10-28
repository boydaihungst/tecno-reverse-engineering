package com.android.server.stats.pull;

import android.app.ActivityManagerInternal;
import android.app.AppOpsManager;
import android.app.INotificationManager;
import android.app.PendingIntentStats;
import android.app.ProcessMemoryState;
import android.app.RuntimeAppOpAccessMessage;
import android.app.StatsManager;
import android.app.usage.NetworkStatsManager;
import android.bluetooth.BluetoothActivityEnergyInfo;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.UidTraffic;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.UserInfo;
import android.hardware.display.DisplayManager;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.health.HealthInfo;
import android.hardware.usb.gadget.V1_2.GadgetFunction;
import android.icu.util.TimeZone;
import android.media.AudioManager;
import android.media.MediaDrm;
import android.media.UnsupportedSchemeException;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.NetworkStats;
import android.net.NetworkTemplate;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryStatsInternal;
import android.os.BatteryStatsManager;
import android.os.BatteryUsageStats;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.CoolingDevice;
import android.os.Debug;
import android.os.Environment;
import android.os.IBinder;
import android.os.IStoraged;
import android.os.IThermalEventListener;
import android.os.IThermalService;
import android.os.OutcomeReceiver;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.StatFs;
import android.os.SynchronousResultReceiver;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Temperature;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.connectivity.WifiActivityEnergyInfo;
import android.os.incremental.IncrementalManager;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.security.metrics.CrashStats;
import android.security.metrics.IKeystoreMetrics;
import android.security.metrics.KeyCreationWithAuthInfo;
import android.security.metrics.KeyCreationWithGeneralInfo;
import android.security.metrics.KeyCreationWithPurposeAndModesInfo;
import android.security.metrics.KeyOperationWithGeneralInfo;
import android.security.metrics.KeyOperationWithPurposeAndModesInfo;
import android.security.metrics.Keystore2AtomWithOverflow;
import android.security.metrics.KeystoreAtom;
import android.security.metrics.RkpErrorStats;
import android.security.metrics.RkpPoolStats;
import android.security.metrics.StorageStats;
import android.telephony.ModemActivityInfo;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.MathUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.StatsEvent;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import com.android.internal.app.procstats.IProcessStats;
import com.android.internal.app.procstats.ProcessStats;
import com.android.internal.app.procstats.StatsEventOutput;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.BinderCallsStats;
import com.android.internal.os.KernelAllocationStats;
import com.android.internal.os.KernelCpuBpfTracking;
import com.android.internal.os.KernelCpuThreadReader;
import com.android.internal.os.KernelCpuThreadReaderDiff;
import com.android.internal.os.KernelCpuThreadReaderSettingsObserver;
import com.android.internal.os.KernelCpuTotalBpfMapReader;
import com.android.internal.os.KernelCpuUidTimeReader;
import com.android.internal.os.KernelSingleProcessCpuThreadReader;
import com.android.internal.os.KernelWakelockReader;
import com.android.internal.os.KernelWakelockStats;
import com.android.internal.os.LooperStats;
import com.android.internal.os.PowerProfile;
import com.android.internal.os.ProcessCpuTracker;
import com.android.internal.os.SelectedProcessCpuThreadReader;
import com.android.internal.os.StoragedUidIoStatsReader;
import com.android.internal.os.SystemServerCpuThreadReader;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.net.module.util.NetworkStatsUtils;
import com.android.role.RoleManagerLocal;
import com.android.server.BinderCallsStatsService;
import com.android.server.LocalManagerRegistry;
import com.android.server.LocalServices;
import com.android.server.PinnerService;
import com.android.server.SystemService;
import com.android.server.SystemServiceManager;
import com.android.server.am.BatteryExternalStatsWorker$$ExternalSyntheticLambda4;
import com.android.server.am.MemoryStatUtil;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.health.HealthServiceWrapper;
import com.android.server.job.JobSchedulerShellCommand;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.UserManagerInternal;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.stats.pull.IonMemoryUtil;
import com.android.server.stats.pull.ProcfsMemoryUtil;
import com.android.server.stats.pull.StatsPullAtomService;
import com.android.server.stats.pull.SystemMemoryUtil;
import com.android.server.stats.pull.netstats.NetworkStatsExt;
import com.android.server.stats.pull.netstats.SubInfo;
import com.android.server.storage.DiskStatsFileLogger;
import com.android.server.storage.DiskStatsLoggingService;
import com.android.server.timezonedetector.MetricsTimeZoneDetectorState;
import com.android.server.timezonedetector.TimeZoneDetectorInternal;
import defpackage.CompanionAppsPermissions;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import libcore.io.IoUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes2.dex */
public class StatsPullAtomService extends SystemService {
    private static final long APP_OPS_SAMPLING_INITIALIZATION_DELAY_MILLIS = 45000;
    private static final int APP_OPS_SIZE_ESTIMATE = 2000;
    private static final String APP_OPS_TARGET_COLLECTION_SIZE = "app_ops_target_collection_size";
    private static final String COMMON_PERMISSION_PREFIX = "android.permission.";
    private static final int CPU_CYCLES_PER_UID_CLUSTER_VALUES = 3;
    private static final int CPU_TIME_PER_THREAD_FREQ_MAX_NUM_FREQUENCIES = 8;
    private static final String DANGEROUS_PERMISSION_STATE_SAMPLE_RATE = "dangerous_permission_state_sample_rate";
    private static final boolean DEBUG = true;
    private static final int DIMENSION_KEY_SIZE_HARD_LIMIT = 800;
    private static final int DIMENSION_KEY_SIZE_SOFT_LIMIT = 500;
    private static final long EXTERNAL_STATS_SYNC_TIMEOUT_MILLIS = 2000;
    private static final int MAX_PROCSTATS_RAW_SHARD_SIZE = 58982;
    private static final int MAX_PROCSTATS_SHARDS = 5;
    private static final int MAX_PROCSTATS_SHARD_SIZE = 49152;
    private static final long MILLIS_PER_SEC = 1000;
    private static final long MILLI_AMP_HR_TO_NANO_AMP_SECS = 3600000000L;
    private static final int MIN_CPU_TIME_PER_UID_FREQ = 10;
    private static final int OP_FLAGS_PULLED = 9;
    private static final String RESULT_RECEIVER_CONTROLLER_KEY = "controller_activity";
    private static final String TAG = "StatsPullAtomService";
    private final Object mAppOpsLock;
    private int mAppOpsSamplingRate;
    private final Object mAppSizeLock;
    private final Object mAppsOnExternalStorageInfoLock;
    private final Object mAttributedAppOpsLock;
    private File mBaseDir;
    private final Object mBinderCallsStatsExceptionsLock;
    private final Object mBinderCallsStatsLock;
    private final Object mBluetoothActivityInfoLock;
    private final Object mBluetoothBytesTransferLock;
    private final Object mBuildInformationLock;
    private final Object mCategorySizeLock;
    private final Context mContext;
    private final Object mCooldownDeviceLock;
    private final Object mCpuActiveTimeLock;
    private final Object mCpuClusterTimeLock;
    private final Object mCpuTimePerClusterFreqLock;
    private final Object mCpuTimePerThreadFreqLock;
    private final Object mCpuTimePerUidFreqLock;
    private final Object mCpuTimePerUidLock;
    private KernelCpuUidTimeReader.KernelCpuUidActiveTimeReader mCpuUidActiveTimeReader;
    private KernelCpuUidTimeReader.KernelCpuUidClusterTimeReader mCpuUidClusterTimeReader;
    private KernelCpuUidTimeReader.KernelCpuUidFreqTimeReader mCpuUidFreqTimeReader;
    private KernelCpuUidTimeReader.KernelCpuUidUserSysTimeReader mCpuUidUserSysTimeReader;
    private final ArraySet<Integer> mDangerousAppOpsList;
    private final Object mDangerousAppOpsListLock;
    private final Object mDangerousPermissionStateLock;
    private final Object mDataBytesTransferLock;
    private final Object mDebugElapsedClockLock;
    private long mDebugElapsedClockPreviousValue;
    private long mDebugElapsedClockPullCount;
    private final Object mDebugFailingElapsedClockLock;
    private long mDebugFailingElapsedClockPreviousValue;
    private long mDebugFailingElapsedClockPullCount;
    private final Object mDeviceCalculatedPowerUseLock;
    private final Object mDirectoryUsageLock;
    private final Object mDiskIoLock;
    private final Object mDiskStatsLock;
    private final Object mExternalStorageInfoLock;
    private final Object mFaceSettingsLock;
    private final Object mHealthHalLock;
    private HealthServiceWrapper mHealthService;
    private final ArrayList<SubInfo> mHistoricalSubs;
    private IKeystoreMetrics mIKeystoreMetrics;
    private final Object mInstalledIncrementalPackagesLock;
    private final Object mIonHeapSizeLock;
    private KernelCpuThreadReaderDiff mKernelCpuThreadReader;
    private final Object mKernelWakelockLock;
    private KernelWakelockReader mKernelWakelockReader;
    private final Object mKeystoreLock;
    private final Object mLooperStatsLock;
    private final Object mModemActivityInfoLock;
    private final ArrayList<NetworkStatsExt> mNetworkStatsBaselines;
    private NetworkStatsManager mNetworkStatsManager;
    private INotificationManager mNotificationManagerService;
    private final Object mNotificationRemoteViewsLock;
    private final Object mNotificationStatsLock;
    private final Object mNumBiometricsEnrolledLock;
    private final Object mPowerProfileLock;
    private final Object mProcStatsLock;
    private final Object mProcessCpuTimeLock;
    private ProcessCpuTracker mProcessCpuTracker;
    private final Object mProcessMemoryHighWaterMarkLock;
    private final Object mProcessMemoryStateLock;
    private IProcessStats mProcessStatsService;
    private final Object mProcessSystemIonHeapSizeLock;
    private final Object mRoleHolderLock;
    private final Object mRuntimeAppOpAccessMessageLock;
    private final Object mSettingsStatsLock;
    private StatsPullAtomCallbackImpl mStatsCallbackImpl;
    private StatsManager mStatsManager;
    private StatsSubscriptionsListener mStatsSubscriptionsListener;
    private StorageManager mStorageManager;
    private IStoraged mStorageService;
    private final Object mStoragedLock;
    private StoragedUidIoStatsReader mStoragedUidIoStatsReader;
    private SubscriptionManager mSubscriptionManager;
    private SelectedProcessCpuThreadReader mSurfaceFlingerProcessCpuThreadReader;
    private final Object mSystemElapsedRealtimeLock;
    private final Object mSystemIonHeapSizeLock;
    private final Object mSystemUptimeLock;
    private TelephonyManager mTelephony;
    private final Object mTemperatureLock;
    private final Object mThermalLock;
    private IThermalService mThermalService;
    private final Object mTimeZoneDataInfoLock;
    private final Object mTimeZoneDetectionInfoLock;
    private KernelWakelockStats mTmpWakelockStats;
    private final Object mWifiActivityInfoLock;
    private WifiManager mWifiManager;
    private static final int RANDOM_SEED = new Random().nextInt();
    private static final long NETSTATS_UID_DEFAULT_BUCKET_DURATION_MS = TimeUnit.HOURS.toMillis(2);

    private native void initializeNativePullers();

    public StatsPullAtomService(Context context) {
        super(context);
        this.mThermalLock = new Object();
        this.mStoragedLock = new Object();
        this.mNotificationStatsLock = new Object();
        this.mDebugElapsedClockPreviousValue = 0L;
        this.mDebugElapsedClockPullCount = 0L;
        this.mDebugFailingElapsedClockPreviousValue = 0L;
        this.mDebugFailingElapsedClockPullCount = 0L;
        this.mAppOpsSamplingRate = 0;
        this.mDangerousAppOpsListLock = new Object();
        this.mDangerousAppOpsList = new ArraySet<>();
        this.mNetworkStatsBaselines = new ArrayList<>();
        this.mHistoricalSubs = new ArrayList<>();
        this.mDataBytesTransferLock = new Object();
        this.mBluetoothBytesTransferLock = new Object();
        this.mKernelWakelockLock = new Object();
        this.mCpuTimePerClusterFreqLock = new Object();
        this.mCpuTimePerUidLock = new Object();
        this.mCpuTimePerUidFreqLock = new Object();
        this.mCpuActiveTimeLock = new Object();
        this.mCpuClusterTimeLock = new Object();
        this.mWifiActivityInfoLock = new Object();
        this.mModemActivityInfoLock = new Object();
        this.mBluetoothActivityInfoLock = new Object();
        this.mSystemElapsedRealtimeLock = new Object();
        this.mSystemUptimeLock = new Object();
        this.mProcessMemoryStateLock = new Object();
        this.mProcessMemoryHighWaterMarkLock = new Object();
        this.mSystemIonHeapSizeLock = new Object();
        this.mIonHeapSizeLock = new Object();
        this.mProcessSystemIonHeapSizeLock = new Object();
        this.mTemperatureLock = new Object();
        this.mCooldownDeviceLock = new Object();
        this.mBinderCallsStatsLock = new Object();
        this.mBinderCallsStatsExceptionsLock = new Object();
        this.mLooperStatsLock = new Object();
        this.mDiskStatsLock = new Object();
        this.mDirectoryUsageLock = new Object();
        this.mAppSizeLock = new Object();
        this.mCategorySizeLock = new Object();
        this.mNumBiometricsEnrolledLock = new Object();
        this.mProcStatsLock = new Object();
        this.mDiskIoLock = new Object();
        this.mPowerProfileLock = new Object();
        this.mProcessCpuTimeLock = new Object();
        this.mCpuTimePerThreadFreqLock = new Object();
        this.mDeviceCalculatedPowerUseLock = new Object();
        this.mDebugElapsedClockLock = new Object();
        this.mDebugFailingElapsedClockLock = new Object();
        this.mBuildInformationLock = new Object();
        this.mRoleHolderLock = new Object();
        this.mTimeZoneDataInfoLock = new Object();
        this.mTimeZoneDetectionInfoLock = new Object();
        this.mExternalStorageInfoLock = new Object();
        this.mAppsOnExternalStorageInfoLock = new Object();
        this.mFaceSettingsLock = new Object();
        this.mAppOpsLock = new Object();
        this.mRuntimeAppOpAccessMessageLock = new Object();
        this.mNotificationRemoteViewsLock = new Object();
        this.mDangerousPermissionStateLock = new Object();
        this.mHealthHalLock = new Object();
        this.mAttributedAppOpsLock = new Object();
        this.mSettingsStatsLock = new Object();
        this.mInstalledIncrementalPackagesLock = new Object();
        this.mKeystoreLock = new Object();
        this.mContext = context;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class StatsPullAtomCallbackImpl implements StatsManager.StatsPullAtomCallback {
        private StatsPullAtomCallbackImpl() {
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [749=69] */
        public int onPullAtom(int atomTag, List<StatsEvent> data) {
            int pullDataBytesTransferLocked;
            int pullKernelWakelockLocked;
            int pullBluetoothBytesTransferLocked;
            int pullBluetoothActivityInfoLocked;
            int pullCpuTimePerUidLocked;
            int pullCpuTimePerUidFreqLocked;
            int pullWifiActivityInfoLocked;
            int pullModemActivityInfoLocked;
            int pullProcessMemoryStateLocked;
            int pullSystemElapsedRealtimeLocked;
            int pullSystemUptimeLocked;
            int pullCpuActiveTimeLocked;
            int pullCpuClusterTimeLocked;
            int pullHealthHalLocked;
            int pullTemperatureLocked;
            int pullBinderCallsStatsLocked;
            int pullBinderCallsStatsExceptionsLocked;
            int pullLooperStatsLocked;
            int pullDiskStatsLocked;
            int pullDirectoryUsageLocked;
            int pullAppSizeLocked;
            int pullCategorySizeLocked;
            int pullProcStatsLocked;
            int pullNumBiometricsEnrolledLocked;
            int pullDiskIOLocked;
            int pullPowerProfileLocked;
            int pullProcStatsLocked2;
            int pullProcessCpuTimeLocked;
            int pullCpuTimePerThreadFreqLocked;
            int pullDeviceCalculatedPowerUseLocked;
            int pullProcessMemoryHighWaterMarkLocked;
            int pullBuildInformationLocked;
            int pullDebugElapsedClockLocked;
            int pullDebugFailingElapsedClockLocked;
            int pullNumBiometricsEnrolledLocked2;
            int pullRoleHolderLocked;
            int pullDangerousPermissionStateLocked;
            int pullTimeZoneDataInfoLocked;
            int pullExternalStorageInfoLocked;
            int pullSystemIonHeapSizeLocked;
            int pullAppsOnExternalStorageInfoLocked;
            int pullFaceSettingsLocked;
            int pullCooldownDeviceLocked;
            int pullAppOpsLocked;
            int pullProcessSystemIonHeapSizeLocked;
            int pullNotificationRemoteViewsLocked;
            int pullRuntimeAppOpAccessMessageLocked;
            int pullIonHeapSizeLocked;
            int pullAttributedAppOpsLocked;
            int pullSettingsStatsLocked;
            int pullCpuTimePerClusterFreqLocked;
            int pullCpuCyclesPerUidClusterLocked;
            int pullTimeZoneDetectorStateLocked;
            int pullInstalledIncrementalPackagesLocked;
            int pullProcessStateLocked;
            int pullProcessAssociationLocked;
            if (Trace.isTagEnabled(524288L)) {
                Trace.traceBegin(524288L, "StatsPull-" + atomTag);
            }
            try {
                switch (atomTag) {
                    case 10000:
                    case FrameworkStatsLog.WIFI_BYTES_TRANSFER_BY_FG_BG /* 10001 */:
                    case FrameworkStatsLog.MOBILE_BYTES_TRANSFER /* 10002 */:
                    case FrameworkStatsLog.MOBILE_BYTES_TRANSFER_BY_FG_BG /* 10003 */:
                    case FrameworkStatsLog.DATA_USAGE_BYTES_TRANSFER /* 10082 */:
                    case FrameworkStatsLog.BYTES_TRANSFER_BY_TAG_AND_METERED /* 10083 */:
                    case FrameworkStatsLog.OEM_MANAGED_BYTES_TRANSFER /* 10100 */:
                        synchronized (StatsPullAtomService.this.mDataBytesTransferLock) {
                            pullDataBytesTransferLocked = StatsPullAtomService.this.pullDataBytesTransferLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullDataBytesTransferLocked;
                    case FrameworkStatsLog.KERNEL_WAKELOCK /* 10004 */:
                        synchronized (StatsPullAtomService.this.mKernelWakelockLock) {
                            pullKernelWakelockLocked = StatsPullAtomService.this.pullKernelWakelockLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullKernelWakelockLocked;
                    case FrameworkStatsLog.SUBSYSTEM_SLEEP_STATE /* 10005 */:
                    case 10008:
                    case 10018:
                    case 10036:
                    case FrameworkStatsLog.ON_DEVICE_POWER_MEASUREMENT /* 10038 */:
                    case 10040:
                    case 10041:
                    case 10051:
                    case 10054:
                    case 10055:
                    case 10062:
                    case 10063:
                    case 10065:
                    case 10068:
                    case FrameworkStatsLog.PACKAGE_NOTIFICATION_PREFERENCES /* 10071 */:
                    case FrameworkStatsLog.PACKAGE_NOTIFICATION_CHANNEL_PREFERENCES /* 10072 */:
                    case FrameworkStatsLog.PACKAGE_NOTIFICATION_CHANNEL_GROUP_PREFERENCES /* 10073 */:
                    case FrameworkStatsLog.GNSS_STATS /* 10074 */:
                    case 10076:
                    case 10077:
                    case 10078:
                    case 10079:
                    case FrameworkStatsLog.BLOB_INFO /* 10081 */:
                    case FrameworkStatsLog.DND_MODE_RULE /* 10084 */:
                    case 10085:
                    case 10086:
                    case 10087:
                    case 10088:
                    case 10089:
                    case 10090:
                    case 10091:
                    case 10093:
                    case 10094:
                    case FrameworkStatsLog.DEVICE_ROTATED_DATA /* 10097 */:
                    case 10099:
                    case FrameworkStatsLog.GNSS_POWER_STATS /* 10101 */:
                    case FrameworkStatsLog.PENDING_ALARM_INFO /* 10106 */:
                    case FrameworkStatsLog.USER_LEVEL_HIBERNATED_APPS /* 10107 */:
                    case 10108:
                    case FrameworkStatsLog.GLOBAL_HIBERNATED_APPS /* 10109 */:
                    case 10110:
                    case FrameworkStatsLog.BATTERY_USAGE_STATS_BEFORE_RESET /* 10111 */:
                    case FrameworkStatsLog.BATTERY_USAGE_STATS_SINCE_RESET /* 10112 */:
                    case FrameworkStatsLog.BATTERY_USAGE_STATS_SINCE_RESET_USING_POWER_PROFILE_MODEL /* 10113 */:
                    case 10115:
                    case 10116:
                    case FrameworkStatsLog.VENDOR_APEX_INFO /* 10126 */:
                    case FrameworkStatsLog.DATA_USAGE_BYTES_TRANSFER_V2 /* 10129 */:
                    case 10131:
                    case 10132:
                    case 10133:
                    case 10134:
                    case 10135:
                    case 10136:
                    case 10137:
                    case 10138:
                    case 10139:
                    case 10140:
                    case 10141:
                    case 10142:
                    case 10143:
                    case 10144:
                    case 10145:
                    case 10146:
                    case 10147:
                    case FrameworkStatsLog.PERSISTENT_URI_PERMISSIONS_AMOUNT_PER_PACKAGE /* 10148 */:
                    case FrameworkStatsLog.SIGNED_PARTITION_INFO /* 10149 */:
                    case FrameworkStatsLog.USER_INFO /* 10152 */:
                    case 10153:
                    case 10154:
                    case 10155:
                    case 10156:
                    case 10157:
                    case 10158:
                    case 10159:
                    case 10160:
                    case 10161:
                    case 10162:
                    case 10163:
                    case 10164:
                    case 10165:
                    case 10166:
                    case 10167:
                    case 10168:
                    case 10169:
                    case 10170:
                    default:
                        throw new UnsupportedOperationException("Unknown tagId=" + atomTag);
                    case FrameworkStatsLog.BLUETOOTH_BYTES_TRANSFER /* 10006 */:
                        synchronized (StatsPullAtomService.this.mBluetoothBytesTransferLock) {
                            pullBluetoothBytesTransferLocked = StatsPullAtomService.this.pullBluetoothBytesTransferLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullBluetoothBytesTransferLocked;
                    case FrameworkStatsLog.BLUETOOTH_ACTIVITY_INFO /* 10007 */:
                        synchronized (StatsPullAtomService.this.mBluetoothActivityInfoLock) {
                            pullBluetoothActivityInfoLocked = StatsPullAtomService.this.pullBluetoothActivityInfoLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullBluetoothActivityInfoLocked;
                    case FrameworkStatsLog.CPU_TIME_PER_UID /* 10009 */:
                        synchronized (StatsPullAtomService.this.mCpuTimePerUidLock) {
                            pullCpuTimePerUidLocked = StatsPullAtomService.this.pullCpuTimePerUidLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullCpuTimePerUidLocked;
                    case FrameworkStatsLog.CPU_TIME_PER_UID_FREQ /* 10010 */:
                        synchronized (StatsPullAtomService.this.mCpuTimePerUidFreqLock) {
                            pullCpuTimePerUidFreqLocked = StatsPullAtomService.this.pullCpuTimePerUidFreqLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullCpuTimePerUidFreqLocked;
                    case FrameworkStatsLog.WIFI_ACTIVITY_INFO /* 10011 */:
                        synchronized (StatsPullAtomService.this.mWifiActivityInfoLock) {
                            pullWifiActivityInfoLocked = StatsPullAtomService.this.pullWifiActivityInfoLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullWifiActivityInfoLocked;
                    case FrameworkStatsLog.MODEM_ACTIVITY_INFO /* 10012 */:
                        synchronized (StatsPullAtomService.this.mModemActivityInfoLock) {
                            pullModemActivityInfoLocked = StatsPullAtomService.this.pullModemActivityInfoLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullModemActivityInfoLocked;
                    case FrameworkStatsLog.PROCESS_MEMORY_STATE /* 10013 */:
                        synchronized (StatsPullAtomService.this.mProcessMemoryStateLock) {
                            pullProcessMemoryStateLocked = StatsPullAtomService.this.pullProcessMemoryStateLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullProcessMemoryStateLocked;
                    case FrameworkStatsLog.SYSTEM_ELAPSED_REALTIME /* 10014 */:
                        synchronized (StatsPullAtomService.this.mSystemElapsedRealtimeLock) {
                            pullSystemElapsedRealtimeLocked = StatsPullAtomService.this.pullSystemElapsedRealtimeLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullSystemElapsedRealtimeLocked;
                    case FrameworkStatsLog.SYSTEM_UPTIME /* 10015 */:
                        synchronized (StatsPullAtomService.this.mSystemUptimeLock) {
                            pullSystemUptimeLocked = StatsPullAtomService.this.pullSystemUptimeLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullSystemUptimeLocked;
                    case FrameworkStatsLog.CPU_ACTIVE_TIME /* 10016 */:
                        synchronized (StatsPullAtomService.this.mCpuActiveTimeLock) {
                            pullCpuActiveTimeLocked = StatsPullAtomService.this.pullCpuActiveTimeLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullCpuActiveTimeLocked;
                    case FrameworkStatsLog.CPU_CLUSTER_TIME /* 10017 */:
                        synchronized (StatsPullAtomService.this.mCpuClusterTimeLock) {
                            pullCpuClusterTimeLocked = StatsPullAtomService.this.pullCpuClusterTimeLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullCpuClusterTimeLocked;
                    case FrameworkStatsLog.REMAINING_BATTERY_CAPACITY /* 10019 */:
                    case FrameworkStatsLog.FULL_BATTERY_CAPACITY /* 10020 */:
                    case FrameworkStatsLog.BATTERY_VOLTAGE /* 10030 */:
                    case FrameworkStatsLog.BATTERY_LEVEL /* 10043 */:
                    case FrameworkStatsLog.BATTERY_CYCLE_COUNT /* 10045 */:
                        synchronized (StatsPullAtomService.this.mHealthHalLock) {
                            pullHealthHalLocked = StatsPullAtomService.this.pullHealthHalLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullHealthHalLocked;
                    case FrameworkStatsLog.TEMPERATURE /* 10021 */:
                        synchronized (StatsPullAtomService.this.mTemperatureLock) {
                            pullTemperatureLocked = StatsPullAtomService.this.pullTemperatureLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullTemperatureLocked;
                    case FrameworkStatsLog.BINDER_CALLS /* 10022 */:
                        synchronized (StatsPullAtomService.this.mBinderCallsStatsLock) {
                            pullBinderCallsStatsLocked = StatsPullAtomService.this.pullBinderCallsStatsLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullBinderCallsStatsLocked;
                    case FrameworkStatsLog.BINDER_CALLS_EXCEPTIONS /* 10023 */:
                        synchronized (StatsPullAtomService.this.mBinderCallsStatsExceptionsLock) {
                            pullBinderCallsStatsExceptionsLocked = StatsPullAtomService.this.pullBinderCallsStatsExceptionsLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullBinderCallsStatsExceptionsLocked;
                    case FrameworkStatsLog.LOOPER_STATS /* 10024 */:
                        synchronized (StatsPullAtomService.this.mLooperStatsLock) {
                            pullLooperStatsLocked = StatsPullAtomService.this.pullLooperStatsLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullLooperStatsLocked;
                    case FrameworkStatsLog.DISK_STATS /* 10025 */:
                        synchronized (StatsPullAtomService.this.mDiskStatsLock) {
                            pullDiskStatsLocked = StatsPullAtomService.this.pullDiskStatsLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullDiskStatsLocked;
                    case FrameworkStatsLog.DIRECTORY_USAGE /* 10026 */:
                        synchronized (StatsPullAtomService.this.mDirectoryUsageLock) {
                            pullDirectoryUsageLocked = StatsPullAtomService.this.pullDirectoryUsageLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullDirectoryUsageLocked;
                    case FrameworkStatsLog.APP_SIZE /* 10027 */:
                        synchronized (StatsPullAtomService.this.mAppSizeLock) {
                            pullAppSizeLocked = StatsPullAtomService.this.pullAppSizeLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullAppSizeLocked;
                    case FrameworkStatsLog.CATEGORY_SIZE /* 10028 */:
                        synchronized (StatsPullAtomService.this.mCategorySizeLock) {
                            pullCategorySizeLocked = StatsPullAtomService.this.pullCategorySizeLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullCategorySizeLocked;
                    case FrameworkStatsLog.PROC_STATS /* 10029 */:
                        synchronized (StatsPullAtomService.this.mProcStatsLock) {
                            pullProcStatsLocked = StatsPullAtomService.this.pullProcStatsLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullProcStatsLocked;
                    case FrameworkStatsLog.NUM_FINGERPRINTS_ENROLLED /* 10031 */:
                        synchronized (StatsPullAtomService.this.mNumBiometricsEnrolledLock) {
                            pullNumBiometricsEnrolledLocked = StatsPullAtomService.this.pullNumBiometricsEnrolledLocked(1, atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullNumBiometricsEnrolledLocked;
                    case FrameworkStatsLog.DISK_IO /* 10032 */:
                        synchronized (StatsPullAtomService.this.mDiskIoLock) {
                            pullDiskIOLocked = StatsPullAtomService.this.pullDiskIOLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullDiskIOLocked;
                    case FrameworkStatsLog.POWER_PROFILE /* 10033 */:
                        synchronized (StatsPullAtomService.this.mPowerProfileLock) {
                            pullPowerProfileLocked = StatsPullAtomService.this.pullPowerProfileLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullPowerProfileLocked;
                    case FrameworkStatsLog.PROC_STATS_PKG_PROC /* 10034 */:
                        synchronized (StatsPullAtomService.this.mProcStatsLock) {
                            pullProcStatsLocked2 = StatsPullAtomService.this.pullProcStatsLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullProcStatsLocked2;
                    case FrameworkStatsLog.PROCESS_CPU_TIME /* 10035 */:
                        synchronized (StatsPullAtomService.this.mProcessCpuTimeLock) {
                            pullProcessCpuTimeLocked = StatsPullAtomService.this.pullProcessCpuTimeLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullProcessCpuTimeLocked;
                    case FrameworkStatsLog.CPU_TIME_PER_THREAD_FREQ /* 10037 */:
                        synchronized (StatsPullAtomService.this.mCpuTimePerThreadFreqLock) {
                            pullCpuTimePerThreadFreqLocked = StatsPullAtomService.this.pullCpuTimePerThreadFreqLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullCpuTimePerThreadFreqLocked;
                    case FrameworkStatsLog.DEVICE_CALCULATED_POWER_USE /* 10039 */:
                        synchronized (StatsPullAtomService.this.mDeviceCalculatedPowerUseLock) {
                            pullDeviceCalculatedPowerUseLocked = StatsPullAtomService.this.pullDeviceCalculatedPowerUseLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullDeviceCalculatedPowerUseLocked;
                    case FrameworkStatsLog.PROCESS_MEMORY_HIGH_WATER_MARK /* 10042 */:
                        synchronized (StatsPullAtomService.this.mProcessMemoryHighWaterMarkLock) {
                            pullProcessMemoryHighWaterMarkLocked = StatsPullAtomService.this.pullProcessMemoryHighWaterMarkLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullProcessMemoryHighWaterMarkLocked;
                    case FrameworkStatsLog.BUILD_INFORMATION /* 10044 */:
                        synchronized (StatsPullAtomService.this.mBuildInformationLock) {
                            pullBuildInformationLocked = StatsPullAtomService.this.pullBuildInformationLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullBuildInformationLocked;
                    case FrameworkStatsLog.DEBUG_ELAPSED_CLOCK /* 10046 */:
                        synchronized (StatsPullAtomService.this.mDebugElapsedClockLock) {
                            pullDebugElapsedClockLocked = StatsPullAtomService.this.pullDebugElapsedClockLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullDebugElapsedClockLocked;
                    case FrameworkStatsLog.DEBUG_FAILING_ELAPSED_CLOCK /* 10047 */:
                        synchronized (StatsPullAtomService.this.mDebugFailingElapsedClockLock) {
                            pullDebugFailingElapsedClockLocked = StatsPullAtomService.this.pullDebugFailingElapsedClockLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullDebugFailingElapsedClockLocked;
                    case FrameworkStatsLog.NUM_FACES_ENROLLED /* 10048 */:
                        synchronized (StatsPullAtomService.this.mNumBiometricsEnrolledLock) {
                            pullNumBiometricsEnrolledLocked2 = StatsPullAtomService.this.pullNumBiometricsEnrolledLocked(4, atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullNumBiometricsEnrolledLocked2;
                    case FrameworkStatsLog.ROLE_HOLDER /* 10049 */:
                        synchronized (StatsPullAtomService.this.mRoleHolderLock) {
                            pullRoleHolderLocked = StatsPullAtomService.this.pullRoleHolderLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullRoleHolderLocked;
                    case FrameworkStatsLog.DANGEROUS_PERMISSION_STATE /* 10050 */:
                    case FrameworkStatsLog.DANGEROUS_PERMISSION_STATE_SAMPLED /* 10067 */:
                        synchronized (StatsPullAtomService.this.mDangerousPermissionStateLock) {
                            pullDangerousPermissionStateLocked = StatsPullAtomService.this.pullDangerousPermissionStateLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullDangerousPermissionStateLocked;
                    case FrameworkStatsLog.TIME_ZONE_DATA_INFO /* 10052 */:
                        synchronized (StatsPullAtomService.this.mTimeZoneDataInfoLock) {
                            pullTimeZoneDataInfoLocked = StatsPullAtomService.this.pullTimeZoneDataInfoLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullTimeZoneDataInfoLocked;
                    case FrameworkStatsLog.EXTERNAL_STORAGE_INFO /* 10053 */:
                        synchronized (StatsPullAtomService.this.mExternalStorageInfoLock) {
                            pullExternalStorageInfoLocked = StatsPullAtomService.this.pullExternalStorageInfoLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullExternalStorageInfoLocked;
                    case FrameworkStatsLog.SYSTEM_ION_HEAP_SIZE /* 10056 */:
                        synchronized (StatsPullAtomService.this.mSystemIonHeapSizeLock) {
                            pullSystemIonHeapSizeLocked = StatsPullAtomService.this.pullSystemIonHeapSizeLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullSystemIonHeapSizeLocked;
                    case FrameworkStatsLog.APPS_ON_EXTERNAL_STORAGE_INFO /* 10057 */:
                        synchronized (StatsPullAtomService.this.mAppsOnExternalStorageInfoLock) {
                            pullAppsOnExternalStorageInfoLocked = StatsPullAtomService.this.pullAppsOnExternalStorageInfoLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullAppsOnExternalStorageInfoLocked;
                    case FrameworkStatsLog.FACE_SETTINGS /* 10058 */:
                        synchronized (StatsPullAtomService.this.mFaceSettingsLock) {
                            pullFaceSettingsLocked = StatsPullAtomService.this.pullFaceSettingsLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullFaceSettingsLocked;
                    case FrameworkStatsLog.COOLING_DEVICE /* 10059 */:
                        synchronized (StatsPullAtomService.this.mCooldownDeviceLock) {
                            pullCooldownDeviceLocked = StatsPullAtomService.this.pullCooldownDeviceLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullCooldownDeviceLocked;
                    case FrameworkStatsLog.APP_OPS /* 10060 */:
                        synchronized (StatsPullAtomService.this.mAppOpsLock) {
                            pullAppOpsLocked = StatsPullAtomService.this.pullAppOpsLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullAppOpsLocked;
                    case FrameworkStatsLog.PROCESS_SYSTEM_ION_HEAP_SIZE /* 10061 */:
                        synchronized (StatsPullAtomService.this.mProcessSystemIonHeapSizeLock) {
                            pullProcessSystemIonHeapSizeLocked = StatsPullAtomService.this.pullProcessSystemIonHeapSizeLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullProcessSystemIonHeapSizeLocked;
                    case FrameworkStatsLog.PROCESS_MEMORY_SNAPSHOT /* 10064 */:
                        int pullProcessMemorySnapshot = StatsPullAtomService.this.pullProcessMemorySnapshot(atomTag, data);
                        Trace.traceEnd(524288L);
                        return pullProcessMemorySnapshot;
                    case FrameworkStatsLog.NOTIFICATION_REMOTE_VIEWS /* 10066 */:
                        synchronized (StatsPullAtomService.this.mNotificationRemoteViewsLock) {
                            pullNotificationRemoteViewsLocked = StatsPullAtomService.this.pullNotificationRemoteViewsLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullNotificationRemoteViewsLocked;
                    case FrameworkStatsLog.RUNTIME_APP_OP_ACCESS /* 10069 */:
                        synchronized (StatsPullAtomService.this.mRuntimeAppOpAccessMessageLock) {
                            pullRuntimeAppOpAccessMessageLocked = StatsPullAtomService.this.pullRuntimeAppOpAccessMessageLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullRuntimeAppOpAccessMessageLocked;
                    case FrameworkStatsLog.ION_HEAP_SIZE /* 10070 */:
                        synchronized (StatsPullAtomService.this.mIonHeapSizeLock) {
                            pullIonHeapSizeLocked = StatsPullAtomService.this.pullIonHeapSizeLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullIonHeapSizeLocked;
                    case FrameworkStatsLog.ATTRIBUTED_APP_OPS /* 10075 */:
                        synchronized (StatsPullAtomService.this.mAttributedAppOpsLock) {
                            pullAttributedAppOpsLocked = StatsPullAtomService.this.pullAttributedAppOpsLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullAttributedAppOpsLocked;
                    case 10080:
                        synchronized (StatsPullAtomService.this.mSettingsStatsLock) {
                            pullSettingsStatsLocked = StatsPullAtomService.this.pullSettingsStatsLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullSettingsStatsLocked;
                    case FrameworkStatsLog.SYSTEM_MEMORY /* 10092 */:
                        int pullSystemMemory = StatsPullAtomService.this.pullSystemMemory(atomTag, data);
                        Trace.traceEnd(524288L);
                        return pullSystemMemory;
                    case FrameworkStatsLog.CPU_TIME_PER_CLUSTER_FREQ /* 10095 */:
                        synchronized (StatsPullAtomService.this.mCpuTimePerClusterFreqLock) {
                            pullCpuTimePerClusterFreqLocked = StatsPullAtomService.this.pullCpuTimePerClusterFreqLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullCpuTimePerClusterFreqLocked;
                    case FrameworkStatsLog.CPU_CYCLES_PER_UID_CLUSTER /* 10096 */:
                        synchronized (StatsPullAtomService.this.mCpuTimePerUidFreqLock) {
                            pullCpuCyclesPerUidClusterLocked = StatsPullAtomService.this.pullCpuCyclesPerUidClusterLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullCpuCyclesPerUidClusterLocked;
                    case FrameworkStatsLog.CPU_CYCLES_PER_THREAD_GROUP_CLUSTER /* 10098 */:
                        int pullCpuCyclesPerThreadGroupCluster = StatsPullAtomService.this.pullCpuCyclesPerThreadGroupCluster(atomTag, data);
                        Trace.traceEnd(524288L);
                        return pullCpuCyclesPerThreadGroupCluster;
                    case FrameworkStatsLog.TIME_ZONE_DETECTOR_STATE /* 10102 */:
                        synchronized (StatsPullAtomService.this.mTimeZoneDetectionInfoLock) {
                            pullTimeZoneDetectorStateLocked = StatsPullAtomService.this.pullTimeZoneDetectorStateLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullTimeZoneDetectorStateLocked;
                    case FrameworkStatsLog.KEYSTORE2_STORAGE_STATS /* 10103 */:
                    case FrameworkStatsLog.RKP_POOL_STATS /* 10104 */:
                    case FrameworkStatsLog.KEYSTORE2_KEY_CREATION_WITH_GENERAL_INFO /* 10118 */:
                    case FrameworkStatsLog.KEYSTORE2_KEY_CREATION_WITH_AUTH_INFO /* 10119 */:
                    case FrameworkStatsLog.KEYSTORE2_KEY_CREATION_WITH_PURPOSE_AND_MODES_INFO /* 10120 */:
                    case FrameworkStatsLog.KEYSTORE2_ATOM_WITH_OVERFLOW /* 10121 */:
                    case FrameworkStatsLog.KEYSTORE2_KEY_OPERATION_WITH_PURPOSE_AND_MODES_INFO /* 10122 */:
                    case FrameworkStatsLog.KEYSTORE2_KEY_OPERATION_WITH_GENERAL_INFO /* 10123 */:
                    case FrameworkStatsLog.RKP_ERROR_STATS /* 10124 */:
                    case FrameworkStatsLog.KEYSTORE2_CRASH_STATS /* 10125 */:
                        int pullKeystoreAtoms = StatsPullAtomService.this.pullKeystoreAtoms(atomTag, data);
                        Trace.traceEnd(524288L);
                        return pullKeystoreAtoms;
                    case FrameworkStatsLog.PROCESS_DMABUF_MEMORY /* 10105 */:
                        int pullProcessDmabufMemory = StatsPullAtomService.this.pullProcessDmabufMemory(atomTag, data);
                        Trace.traceEnd(524288L);
                        return pullProcessDmabufMemory;
                    case FrameworkStatsLog.INSTALLED_INCREMENTAL_PACKAGE /* 10114 */:
                        synchronized (StatsPullAtomService.this.mInstalledIncrementalPackagesLock) {
                            pullInstalledIncrementalPackagesLocked = StatsPullAtomService.this.pullInstalledIncrementalPackagesLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullInstalledIncrementalPackagesLocked;
                    case FrameworkStatsLog.VMSTAT /* 10117 */:
                        int pullVmStat = StatsPullAtomService.this.pullVmStat(atomTag, data);
                        Trace.traceEnd(524288L);
                        return pullVmStat;
                    case FrameworkStatsLog.ACCESSIBILITY_SHORTCUT_STATS /* 10127 */:
                        int pullAccessibilityShortcutStatsLocked = StatsPullAtomService.this.pullAccessibilityShortcutStatsLocked(atomTag, data);
                        Trace.traceEnd(524288L);
                        return pullAccessibilityShortcutStatsLocked;
                    case FrameworkStatsLog.ACCESSIBILITY_FLOATING_MENU_STATS /* 10128 */:
                        int pullAccessibilityFloatingMenuStatsLocked = StatsPullAtomService.this.pullAccessibilityFloatingMenuStatsLocked(atomTag, data);
                        Trace.traceEnd(524288L);
                        return pullAccessibilityFloatingMenuStatsLocked;
                    case FrameworkStatsLog.MEDIA_CAPABILITIES /* 10130 */:
                        int pullMediaCapabilitiesStats = StatsPullAtomService.this.pullMediaCapabilitiesStats(atomTag, data);
                        Trace.traceEnd(524288L);
                        return pullMediaCapabilitiesStats;
                    case FrameworkStatsLog.PINNED_FILE_SIZES_PER_PACKAGE /* 10150 */:
                        int pullSystemServerPinnerStats = StatsPullAtomService.this.pullSystemServerPinnerStats(atomTag, data);
                        Trace.traceEnd(524288L);
                        return pullSystemServerPinnerStats;
                    case FrameworkStatsLog.PENDING_INTENTS_PER_PACKAGE /* 10151 */:
                        int pullPendingIntentsPerPackage = StatsPullAtomService.this.pullPendingIntentsPerPackage(atomTag, data);
                        Trace.traceEnd(524288L);
                        return pullPendingIntentsPerPackage;
                    case FrameworkStatsLog.PROCESS_STATE /* 10171 */:
                        synchronized (StatsPullAtomService.this.mProcStatsLock) {
                            pullProcessStateLocked = StatsPullAtomService.this.pullProcessStateLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullProcessStateLocked;
                    case FrameworkStatsLog.PROCESS_ASSOCIATION /* 10172 */:
                        synchronized (StatsPullAtomService.this.mProcStatsLock) {
                            pullProcessAssociationLocked = StatsPullAtomService.this.pullProcessAssociationLocked(atomTag, data);
                        }
                        Trace.traceEnd(524288L);
                        return pullProcessAssociationLocked;
                }
            } catch (Throwable th) {
                Trace.traceEnd(524288L);
                throw th;
            }
        }
    }

    @Override // com.android.server.SystemService
    public void onStart() {
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        super.onBootPhase(phase);
        if (phase == 500) {
            BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    StatsPullAtomService.this.m6652x46e39565();
                }
            });
        } else if (phase == 600) {
            BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    StatsPullAtomService.this.m6653x74bc2fc4();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onBootPhase$0$com-android-server-stats-pull-StatsPullAtomService  reason: not valid java name */
    public /* synthetic */ void m6652x46e39565() {
        initializeNativePullers();
        initializePullersState();
        registerPullers();
        registerEventListeners();
    }

    void initializePullersState() {
        this.mStatsManager = (StatsManager) this.mContext.getSystemService("stats");
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mTelephony = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mSubscriptionManager = (SubscriptionManager) this.mContext.getSystemService("telephony_subscription_service");
        this.mStatsSubscriptionsListener = new StatsSubscriptionsListener(this.mSubscriptionManager);
        this.mStorageManager = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        this.mNetworkStatsManager = (NetworkStatsManager) this.mContext.getSystemService(NetworkStatsManager.class);
        this.mStoragedUidIoStatsReader = new StoragedUidIoStatsReader();
        File file = new File(SystemServiceManager.ensureSystemDir(), "stats_pull");
        this.mBaseDir = file;
        file.mkdirs();
        this.mCpuUidUserSysTimeReader = new KernelCpuUidTimeReader.KernelCpuUidUserSysTimeReader(false);
        this.mCpuUidFreqTimeReader = new KernelCpuUidTimeReader.KernelCpuUidFreqTimeReader(false);
        this.mCpuUidActiveTimeReader = new KernelCpuUidTimeReader.KernelCpuUidActiveTimeReader(false);
        this.mCpuUidClusterTimeReader = new KernelCpuUidTimeReader.KernelCpuUidClusterTimeReader(false);
        this.mKernelWakelockReader = new KernelWakelockReader();
        this.mTmpWakelockStats = new KernelWakelockStats();
        this.mKernelCpuThreadReader = KernelCpuThreadReaderSettingsObserver.getSettingsModifiedReader(this.mContext);
        try {
            this.mHealthService = HealthServiceWrapper.create(null);
        } catch (RemoteException | NoSuchElementException e) {
            Slog.e(TAG, "failed to initialize healthHalWrapper");
        }
        PackageManager pm = this.mContext.getPackageManager();
        for (int op = 0; op < 121; op++) {
            String perm = AppOpsManager.opToPermission(op);
            if (perm != null) {
                try {
                    PermissionInfo permInfo = pm.getPermissionInfo(perm, 0);
                    if (permInfo.getProtection() == 1) {
                        this.mDangerousAppOpsList.add(Integer.valueOf(op));
                    }
                } catch (PackageManager.NameNotFoundException e2) {
                }
            }
        }
        this.mSurfaceFlingerProcessCpuThreadReader = new SelectedProcessCpuThreadReader("/system/bin/surfaceflinger");
        getIKeystoreMetricsService();
    }

    void registerEventListeners() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        NetworkRequest request = new NetworkRequest.Builder().build();
        connectivityManager.registerNetworkCallback(request, new ConnectivityStatsCallback());
        IThermalService thermalService = getIThermalService();
        if (thermalService != null) {
            try {
                thermalService.registerThermalEventListener(new ThermalEventListener());
                Slog.i(TAG, "register thermal listener successfully");
            } catch (RemoteException e) {
                Slog.i(TAG, "failed to register thermal listener");
            }
        }
    }

    void registerPullers() {
        Slog.d(TAG, "Registering pullers with statsd");
        this.mStatsCallbackImpl = new StatsPullAtomCallbackImpl();
        registerBluetoothBytesTransfer();
        registerKernelWakelock();
        registerCpuTimePerClusterFreq();
        registerCpuTimePerUid();
        registerCpuCyclesPerUidCluster();
        registerCpuTimePerUidFreq();
        registerCpuCyclesPerThreadGroupCluster();
        registerCpuActiveTime();
        registerCpuClusterTime();
        registerWifiActivityInfo();
        registerModemActivityInfo();
        registerBluetoothActivityInfo();
        registerSystemElapsedRealtime();
        registerSystemUptime();
        registerProcessMemoryState();
        registerProcessMemoryHighWaterMark();
        registerProcessMemorySnapshot();
        registerSystemIonHeapSize();
        registerIonHeapSize();
        registerProcessSystemIonHeapSize();
        registerSystemMemory();
        registerProcessDmabufMemory();
        registerVmStat();
        registerTemperature();
        registerCoolingDevice();
        registerBinderCallsStats();
        registerBinderCallsStatsExceptions();
        registerLooperStats();
        registerDiskStats();
        registerDirectoryUsage();
        registerAppSize();
        registerCategorySize();
        registerNumFingerprintsEnrolled();
        registerNumFacesEnrolled();
        registerProcStats();
        registerProcStatsPkgProc();
        registerProcessState();
        registerProcessAssociation();
        registerDiskIO();
        registerPowerProfile();
        registerProcessCpuTime();
        registerCpuTimePerThreadFreq();
        registerDeviceCalculatedPowerUse();
        registerDebugElapsedClock();
        registerDebugFailingElapsedClock();
        registerBuildInformation();
        registerRoleHolder();
        registerTimeZoneDataInfo();
        registerTimeZoneDetectorState();
        registerExternalStorageInfo();
        registerAppsOnExternalStorageInfo();
        registerFaceSettings();
        registerAppOps();
        registerAttributedAppOps();
        registerRuntimeAppOpAccessMessage();
        registerNotificationRemoteViews();
        registerDangerousPermissionState();
        registerDangerousPermissionStateSampled();
        registerBatteryLevel();
        registerRemainingBatteryCapacity();
        registerFullBatteryCapacity();
        registerBatteryVoltage();
        registerBatteryCycleCount();
        registerSettingsStats();
        registerInstalledIncrementalPackages();
        registerKeystoreStorageStats();
        registerRkpPoolStats();
        registerKeystoreKeyCreationWithGeneralInfo();
        registerKeystoreKeyCreationWithAuthInfo();
        registerKeystoreKeyCreationWithPurposeModesInfo();
        registerKeystoreAtomWithOverflow();
        registerKeystoreKeyOperationWithPurposeAndModesInfo();
        registerKeystoreKeyOperationWithGeneralInfo();
        registerRkpErrorStats();
        registerKeystoreCrashStats();
        registerAccessibilityShortcutStats();
        registerAccessibilityFloatingMenuStats();
        registerMediaCapabilitiesStats();
        registerPendingIntentsPerPackagePuller();
        registerPinnerServiceStats();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: initAndRegisterNetworkStatsPullers */
    public void m6653x74bc2fc4() {
        Slog.d(TAG, "Registering NetworkStats pullers with statsd");
        this.mNetworkStatsBaselines.addAll(collectNetworkStatsSnapshotForAtom(10000));
        this.mNetworkStatsBaselines.addAll(collectNetworkStatsSnapshotForAtom(FrameworkStatsLog.WIFI_BYTES_TRANSFER_BY_FG_BG));
        this.mNetworkStatsBaselines.addAll(collectNetworkStatsSnapshotForAtom(FrameworkStatsLog.MOBILE_BYTES_TRANSFER));
        this.mNetworkStatsBaselines.addAll(collectNetworkStatsSnapshotForAtom(FrameworkStatsLog.MOBILE_BYTES_TRANSFER_BY_FG_BG));
        this.mNetworkStatsBaselines.addAll(collectNetworkStatsSnapshotForAtom(FrameworkStatsLog.BYTES_TRANSFER_BY_TAG_AND_METERED));
        this.mNetworkStatsBaselines.addAll(collectNetworkStatsSnapshotForAtom(FrameworkStatsLog.DATA_USAGE_BYTES_TRANSFER));
        this.mNetworkStatsBaselines.addAll(collectNetworkStatsSnapshotForAtom(FrameworkStatsLog.OEM_MANAGED_BYTES_TRANSFER));
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(BackgroundThread.getExecutor(), this.mStatsSubscriptionsListener);
        registerWifiBytesTransfer();
        registerWifiBytesTransferBackground();
        registerMobileBytesTransfer();
        registerMobileBytesTransferBackground();
        registerBytesTransferByTagAndMetered();
        registerDataUsageBytesTransfer();
        registerOemManagedBytesTransfer();
    }

    private IThermalService getIThermalService() {
        IThermalService iThermalService;
        synchronized (this.mThermalLock) {
            if (this.mThermalService == null) {
                IThermalService asInterface = IThermalService.Stub.asInterface(ServiceManager.getService("thermalservice"));
                this.mThermalService = asInterface;
                if (asInterface != null) {
                    try {
                        asInterface.asBinder().linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda15
                            @Override // android.os.IBinder.DeathRecipient
                            public final void binderDied() {
                                StatsPullAtomService.this.m6651xdbb9c42();
                            }
                        }, 0);
                    } catch (RemoteException e) {
                        Slog.e(TAG, "linkToDeath with thermalService failed", e);
                        this.mThermalService = null;
                    }
                }
            }
            iThermalService = this.mThermalService;
        }
        return iThermalService;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getIThermalService$2$com-android-server-stats-pull-StatsPullAtomService  reason: not valid java name */
    public /* synthetic */ void m6651xdbb9c42() {
        synchronized (this.mThermalLock) {
            this.mThermalService = null;
        }
    }

    private IKeystoreMetrics getIKeystoreMetricsService() {
        IKeystoreMetrics iKeystoreMetrics;
        synchronized (this.mKeystoreLock) {
            if (this.mIKeystoreMetrics == null) {
                IKeystoreMetrics asInterface = IKeystoreMetrics.Stub.asInterface(ServiceManager.getService("android.security.metrics"));
                this.mIKeystoreMetrics = asInterface;
                if (asInterface != null) {
                    try {
                        asInterface.asBinder().linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda22
                            @Override // android.os.IBinder.DeathRecipient
                            public final void binderDied() {
                                StatsPullAtomService.this.m6647x997ebc8b();
                            }
                        }, 0);
                    } catch (RemoteException e) {
                        Slog.e(TAG, "linkToDeath with IKeystoreMetrics failed", e);
                        this.mIKeystoreMetrics = null;
                    }
                }
            }
            iKeystoreMetrics = this.mIKeystoreMetrics;
        }
        return iKeystoreMetrics;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getIKeystoreMetricsService$3$com-android-server-stats-pull-StatsPullAtomService  reason: not valid java name */
    public /* synthetic */ void m6647x997ebc8b() {
        synchronized (this.mKeystoreLock) {
            this.mIKeystoreMetrics = null;
        }
    }

    private IStoraged getIStoragedService() {
        synchronized (this.mStoragedLock) {
            if (this.mStorageService == null) {
                this.mStorageService = IStoraged.Stub.asInterface(ServiceManager.getService("storaged"));
            }
            IStoraged iStoraged = this.mStorageService;
            if (iStoraged != null) {
                try {
                    iStoraged.asBinder().linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda14
                        @Override // android.os.IBinder.DeathRecipient
                        public final void binderDied() {
                            StatsPullAtomService.this.m6650x2a281ed2();
                        }
                    }, 0);
                } catch (RemoteException e) {
                    Slog.e(TAG, "linkToDeath with storagedService failed", e);
                    this.mStorageService = null;
                }
            }
        }
        return this.mStorageService;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getIStoragedService$4$com-android-server-stats-pull-StatsPullAtomService  reason: not valid java name */
    public /* synthetic */ void m6650x2a281ed2() {
        synchronized (this.mStoragedLock) {
            this.mStorageService = null;
        }
    }

    private INotificationManager getINotificationManagerService() {
        synchronized (this.mNotificationStatsLock) {
            if (this.mNotificationManagerService == null) {
                this.mNotificationManagerService = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
            }
            INotificationManager iNotificationManager = this.mNotificationManagerService;
            if (iNotificationManager != null) {
                try {
                    iNotificationManager.asBinder().linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda16
                        @Override // android.os.IBinder.DeathRecipient
                        public final void binderDied() {
                            StatsPullAtomService.this.m6648xa85eb18a();
                        }
                    }, 0);
                } catch (RemoteException e) {
                    Slog.e(TAG, "linkToDeath with notificationManager failed", e);
                    this.mNotificationManagerService = null;
                }
            }
        }
        return this.mNotificationManagerService;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getINotificationManagerService$5$com-android-server-stats-pull-StatsPullAtomService  reason: not valid java name */
    public /* synthetic */ void m6648xa85eb18a() {
        synchronized (this.mNotificationStatsLock) {
            this.mNotificationManagerService = null;
        }
    }

    private IProcessStats getIProcessStatsService() {
        synchronized (this.mProcStatsLock) {
            if (this.mProcessStatsService == null) {
                this.mProcessStatsService = IProcessStats.Stub.asInterface(ServiceManager.getService("procstats"));
            }
            IProcessStats iProcessStats = this.mProcessStatsService;
            if (iProcessStats != null) {
                try {
                    iProcessStats.asBinder().linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda20
                        @Override // android.os.IBinder.DeathRecipient
                        public final void binderDied() {
                            StatsPullAtomService.this.m6649x14915537();
                        }
                    }, 0);
                } catch (RemoteException e) {
                    Slog.e(TAG, "linkToDeath with ProcessStats failed", e);
                    this.mProcessStatsService = null;
                }
            }
        }
        return this.mProcessStatsService;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getIProcessStatsService$6$com-android-server-stats-pull-StatsPullAtomService  reason: not valid java name */
    public /* synthetic */ void m6649x14915537() {
        synchronized (this.mProcStatsLock) {
            this.mProcessStatsService = null;
        }
    }

    private void registerWifiBytesTransfer() {
        StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setAdditiveFields(new int[]{2, 3, 4, 5}).build();
        this.mStatsManager.setPullAtomCallback(10000, metadata, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private List<NetworkStatsExt> collectNetworkStatsSnapshotForAtom(int atomTag) {
        List<NetworkStatsExt> ret = new ArrayList<>();
        switch (atomTag) {
            case 10000:
                NetworkStats stats = getUidNetworkStatsSnapshotForTransport(1);
                if (stats != null) {
                    ret.add(new NetworkStatsExt(sliceNetworkStatsByUid(stats), new int[]{1}, false));
                    break;
                }
                break;
            case FrameworkStatsLog.WIFI_BYTES_TRANSFER_BY_FG_BG /* 10001 */:
                NetworkStats stats2 = getUidNetworkStatsSnapshotForTransport(1);
                if (stats2 != null) {
                    ret.add(new NetworkStatsExt(sliceNetworkStatsByUidAndFgbg(stats2), new int[]{1}, true));
                    break;
                }
                break;
            case FrameworkStatsLog.MOBILE_BYTES_TRANSFER /* 10002 */:
                NetworkStats stats3 = getUidNetworkStatsSnapshotForTransport(0);
                if (stats3 != null) {
                    ret.add(new NetworkStatsExt(sliceNetworkStatsByUid(stats3), new int[]{0}, false));
                    break;
                }
                break;
            case FrameworkStatsLog.MOBILE_BYTES_TRANSFER_BY_FG_BG /* 10003 */:
                NetworkStats stats4 = getUidNetworkStatsSnapshotForTransport(0);
                if (stats4 != null) {
                    ret.add(new NetworkStatsExt(sliceNetworkStatsByUidAndFgbg(stats4), new int[]{0}, true));
                    break;
                }
                break;
            case FrameworkStatsLog.DATA_USAGE_BYTES_TRANSFER /* 10082 */:
                Iterator<SubInfo> it = this.mHistoricalSubs.iterator();
                while (it.hasNext()) {
                    SubInfo subInfo = it.next();
                    ret.addAll(getDataUsageBytesTransferSnapshotForSub(subInfo));
                }
                break;
            case FrameworkStatsLog.BYTES_TRANSFER_BY_TAG_AND_METERED /* 10083 */:
                NetworkStats wifiStats = getUidNetworkStatsSnapshotForTemplate(new NetworkTemplate.Builder(4).build(), true);
                NetworkStats cellularStats = getUidNetworkStatsSnapshotForTemplate(new NetworkTemplate.Builder(1).setMeteredness(1).build(), true);
                if (wifiStats != null && cellularStats != null) {
                    ret.add(new NetworkStatsExt(sliceNetworkStatsByUidTagAndMetered(wifiStats.add(cellularStats)), new int[]{1, 0}, false, true, true, 0, null, -1));
                    break;
                }
                break;
            case FrameworkStatsLog.OEM_MANAGED_BYTES_TRANSFER /* 10100 */:
                ret.addAll(getDataUsageBytesTransferSnapshotForOemManaged());
                break;
            default:
                throw new IllegalArgumentException("Unknown atomTag " + atomTag);
        }
        return ret;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int pullDataBytesTransferLocked(int atomTag, List<StatsEvent> pulledData) {
        List<NetworkStatsExt> current = collectNetworkStatsSnapshotForAtom(atomTag);
        int i = 1;
        if (current == null) {
            Slog.e(TAG, "current snapshot is null for " + atomTag + ", return.");
            return 1;
        }
        for (final NetworkStatsExt item : current) {
            NetworkStatsExt baseline = (NetworkStatsExt) CollectionUtils.find(this.mNetworkStatsBaselines, new Predicate() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda1
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean hasSameSlicing;
                    hasSameSlicing = ((NetworkStatsExt) obj).hasSameSlicing(NetworkStatsExt.this);
                    return hasSameSlicing;
                }
            });
            if (baseline == null) {
                Slog.e(TAG, "baseline is null for " + atomTag + ", return.");
                return i;
            }
            List<NetworkStatsExt> current2 = current;
            NetworkStatsExt diff = new NetworkStatsExt(removeEmptyEntries(item.stats.subtract(baseline.stats)), item.transports, item.slicedByFgbg, item.slicedByTag, item.slicedByMetered, item.ratType, item.subInfo, item.oemManaged);
            if (diff.stats.iterator().hasNext()) {
                switch (atomTag) {
                    case FrameworkStatsLog.DATA_USAGE_BYTES_TRANSFER /* 10082 */:
                        addDataUsageBytesTransferAtoms(diff, pulledData);
                        break;
                    case FrameworkStatsLog.BYTES_TRANSFER_BY_TAG_AND_METERED /* 10083 */:
                        addBytesTransferByTagAndMeteredAtoms(diff, pulledData);
                        break;
                    case FrameworkStatsLog.OEM_MANAGED_BYTES_TRANSFER /* 10100 */:
                        addOemDataUsageBytesTransferAtoms(diff, pulledData);
                        break;
                    default:
                        addNetworkStats(atomTag, pulledData, diff);
                        break;
                }
                current = current2;
                i = 1;
            } else {
                current = current2;
                i = 1;
            }
        }
        return 0;
    }

    private static NetworkStats removeEmptyEntries(NetworkStats stats) {
        NetworkStats ret = new NetworkStats(0L, 1);
        Iterator it = stats.iterator();
        while (it.hasNext()) {
            NetworkStats.Entry e = (NetworkStats.Entry) it.next();
            if (e.getRxBytes() != 0 || e.getRxPackets() != 0 || e.getTxBytes() != 0 || e.getTxPackets() != 0 || e.getOperations() != 0) {
                ret = ret.addEntry(e);
            }
        }
        return ret;
    }

    private void addNetworkStats(int atomTag, List<StatsEvent> ret, NetworkStatsExt statsExt) {
        StatsEvent statsEvent;
        Iterator it = statsExt.stats.iterator();
        while (it.hasNext()) {
            NetworkStats.Entry entry = (NetworkStats.Entry) it.next();
            if (statsExt.slicedByFgbg) {
                statsEvent = FrameworkStatsLog.buildStatsEvent(atomTag, entry.getUid(), entry.getSet() > 0, entry.getRxBytes(), entry.getRxPackets(), entry.getTxBytes(), entry.getTxPackets());
            } else {
                statsEvent = FrameworkStatsLog.buildStatsEvent(atomTag, entry.getUid(), entry.getRxBytes(), entry.getRxPackets(), entry.getTxBytes(), entry.getTxPackets());
            }
            ret.add(statsEvent);
        }
    }

    private void addBytesTransferByTagAndMeteredAtoms(NetworkStatsExt statsExt, List<StatsEvent> pulledData) {
        Iterator it = statsExt.stats.iterator();
        while (it.hasNext()) {
            NetworkStats.Entry entry = (NetworkStats.Entry) it.next();
            pulledData.add(FrameworkStatsLog.buildStatsEvent((int) FrameworkStatsLog.BYTES_TRANSFER_BY_TAG_AND_METERED, entry.getUid(), entry.getMetered() == 1, entry.getTag(), entry.getRxBytes(), entry.getRxPackets(), entry.getTxBytes(), entry.getTxPackets()));
        }
    }

    private void addDataUsageBytesTransferAtoms(NetworkStatsExt statsExt, List<StatsEvent> pulledData) {
        int i;
        boolean is5GNsa = statsExt.ratType == -2;
        boolean isNR = is5GNsa || statsExt.ratType == 20;
        Iterator it = statsExt.stats.iterator();
        while (it.hasNext()) {
            NetworkStats.Entry entry = (NetworkStats.Entry) it.next();
            int set = entry.getSet();
            long rxBytes = entry.getRxBytes();
            long rxPackets = entry.getRxPackets();
            long txBytes = entry.getTxBytes();
            long txPackets = entry.getTxPackets();
            int i2 = is5GNsa ? 13 : statsExt.ratType;
            String str = statsExt.subInfo.mcc;
            String str2 = statsExt.subInfo.mnc;
            int i3 = statsExt.subInfo.carrierId;
            boolean is5GNsa2 = is5GNsa;
            if (statsExt.subInfo.isOpportunistic) {
                i = 2;
            } else {
                i = 3;
            }
            pulledData.add(FrameworkStatsLog.buildStatsEvent((int) FrameworkStatsLog.DATA_USAGE_BYTES_TRANSFER, set, rxBytes, rxPackets, txBytes, txPackets, i2, str, str2, i3, i, isNR));
            is5GNsa = is5GNsa2;
        }
    }

    private void addOemDataUsageBytesTransferAtoms(NetworkStatsExt statsExt, List<StatsEvent> pulledData) {
        int oemManaged = statsExt.oemManaged;
        int[] iArr = statsExt.transports;
        int length = iArr.length;
        int i = 0;
        while (i < length) {
            int transport = iArr[i];
            Iterator it = statsExt.stats.iterator();
            while (it.hasNext()) {
                NetworkStats.Entry entry = (NetworkStats.Entry) it.next();
                pulledData.add(FrameworkStatsLog.buildStatsEvent(FrameworkStatsLog.OEM_MANAGED_BYTES_TRANSFER, entry.getUid(), entry.getSet() > 0, oemManaged, transport, entry.getRxBytes(), entry.getRxPackets(), entry.getTxBytes(), entry.getTxPackets()));
                length = length;
                i = i;
            }
            i++;
        }
    }

    private List<NetworkStatsExt> getDataUsageBytesTransferSnapshotForOemManaged() {
        int i = 1;
        List<Pair<Integer, Integer>> matchRulesAndTransports = List.of(new Pair(5, 3), new Pair(1, 0), new Pair(4, 1));
        int[] oemManagedTypes = {3, 1, 2};
        List<NetworkStatsExt> ret = new ArrayList<>();
        for (Pair<Integer, Integer> ruleAndTransport : matchRulesAndTransports) {
            Integer matchRule = (Integer) ruleAndTransport.first;
            int length = oemManagedTypes.length;
            int i2 = 0;
            while (i2 < length) {
                int oemManaged = oemManagedTypes[i2];
                NetworkTemplate template = new NetworkTemplate.Builder(matchRule.intValue()).setOemManaged(oemManaged).build();
                NetworkStats stats = getUidNetworkStatsSnapshotForTemplate(template, false);
                Integer transport = (Integer) ruleAndTransport.second;
                if (stats != null) {
                    NetworkStats sliceNetworkStatsByUidAndFgbg = sliceNetworkStatsByUidAndFgbg(stats);
                    int[] iArr = new int[i];
                    iArr[0] = transport.intValue();
                    ret.add(new NetworkStatsExt(sliceNetworkStatsByUidAndFgbg, iArr, true, false, false, 0, null, oemManaged));
                }
                i2++;
                i = 1;
            }
            i = 1;
        }
        return ret;
    }

    private NetworkStats getUidNetworkStatsSnapshotForTransport(int transport) {
        NetworkTemplate template = null;
        switch (transport) {
            case 0:
                template = new NetworkTemplate.Builder(1).setMeteredness(1).build();
                break;
            case 1:
                template = new NetworkTemplate.Builder(4).build();
                break;
            default:
                Log.wtf(TAG, "Unexpected transport.");
                break;
        }
        return getUidNetworkStatsSnapshotForTemplate(template, false);
    }

    private NetworkStats getUidNetworkStatsSnapshotForTemplate(NetworkTemplate template, boolean includeTags) {
        long elapsedMillisSinceBoot = SystemClock.elapsedRealtime();
        long currentTimeInMillis = TimeUnit.MICROSECONDS.toMillis(SystemClock.currentTimeMicro());
        long bucketDuration = Settings.Global.getLong(this.mContext.getContentResolver(), "netstats_uid_bucket_duration", NETSTATS_UID_DEFAULT_BUCKET_DURATION_MS);
        if (template.getMatchRule() == 4 && template.getSubscriberIds().isEmpty()) {
            this.mNetworkStatsManager.forceUpdate();
        }
        android.app.usage.NetworkStats queryNonTaggedStats = this.mNetworkStatsManager.querySummary(template, (currentTimeInMillis - elapsedMillisSinceBoot) - bucketDuration, currentTimeInMillis);
        NetworkStats nonTaggedStats = NetworkStatsUtils.fromPublicNetworkStats(queryNonTaggedStats);
        if (includeTags) {
            android.app.usage.NetworkStats queryTaggedStats = this.mNetworkStatsManager.queryTaggedSummary(template, (currentTimeInMillis - elapsedMillisSinceBoot) - bucketDuration, currentTimeInMillis);
            NetworkStats taggedStats = NetworkStatsUtils.fromPublicNetworkStats(queryTaggedStats);
            return nonTaggedStats.add(taggedStats);
        }
        return nonTaggedStats;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r20v0, resolved type: com.android.server.stats.pull.StatsPullAtomService */
    /* JADX DEBUG: Multi-variable search result rejected for r9v0, resolved type: int[] */
    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r4v0 */
    /* JADX WARN: Type inference failed for: r4v1, types: [boolean] */
    /* JADX WARN: Type inference failed for: r4v2 */
    public List<NetworkStatsExt> getDataUsageBytesTransferSnapshotForSub(SubInfo subInfo) {
        List<NetworkStatsExt> ret = new ArrayList<>();
        int[] allCollapsedRatTypes = getAllCollapsedRatTypes();
        int length = allCollapsedRatTypes.length;
        ?? r4 = 0;
        int i = 0;
        while (i < length) {
            int ratType = allCollapsedRatTypes[i];
            NetworkTemplate template = new NetworkTemplate.Builder(1).setSubscriberIds(Set.of(subInfo.subscriberId)).setRatType(ratType).setMeteredness(1).build();
            NetworkStats stats = getUidNetworkStatsSnapshotForTemplate(template, r4);
            if (stats != null) {
                NetworkStats sliceNetworkStatsByFgbg = sliceNetworkStatsByFgbg(stats);
                int[] iArr = new int[1];
                iArr[r4] = r4;
                ret.add(new NetworkStatsExt(sliceNetworkStatsByFgbg, iArr, true, false, false, ratType, subInfo, -1));
            }
            i++;
            r4 = 0;
        }
        return ret;
    }

    private static int[] getAllCollapsedRatTypes() {
        int[] ratTypes = TelephonyManager.getAllNetworkTypes();
        HashSet<Integer> collapsedRatTypes = new HashSet<>();
        for (int ratType : ratTypes) {
            collapsedRatTypes.add(Integer.valueOf(NetworkStatsManager.getCollapsedRatType(ratType)));
        }
        collapsedRatTypes.add(Integer.valueOf(NetworkStatsManager.getCollapsedRatType(-2)));
        collapsedRatTypes.add(0);
        return com.android.net.module.util.CollectionUtils.toIntArray(collapsedRatTypes);
    }

    private NetworkStats sliceNetworkStatsByUid(NetworkStats stats) {
        return sliceNetworkStats(stats, new Function() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda13
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return StatsPullAtomService.lambda$sliceNetworkStatsByUid$8((NetworkStats.Entry) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ NetworkStats.Entry lambda$sliceNetworkStatsByUid$8(NetworkStats.Entry entry) {
        return new NetworkStats.Entry((String) null, entry.getUid(), -1, 0, -1, -1, -1, entry.getRxBytes(), entry.getRxPackets(), entry.getTxBytes(), entry.getTxPackets(), 0L);
    }

    private NetworkStats sliceNetworkStatsByFgbg(NetworkStats stats) {
        return sliceNetworkStats(stats, new Function() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return StatsPullAtomService.lambda$sliceNetworkStatsByFgbg$9((NetworkStats.Entry) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ NetworkStats.Entry lambda$sliceNetworkStatsByFgbg$9(NetworkStats.Entry entry) {
        return new NetworkStats.Entry((String) null, -1, entry.getSet(), 0, -1, -1, -1, entry.getRxBytes(), entry.getRxPackets(), entry.getTxBytes(), entry.getTxPackets(), 0L);
    }

    private NetworkStats sliceNetworkStatsByUidAndFgbg(NetworkStats stats) {
        return sliceNetworkStats(stats, new Function() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda3
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return StatsPullAtomService.lambda$sliceNetworkStatsByUidAndFgbg$10((NetworkStats.Entry) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ NetworkStats.Entry lambda$sliceNetworkStatsByUidAndFgbg$10(NetworkStats.Entry entry) {
        return new NetworkStats.Entry((String) null, entry.getUid(), entry.getSet(), 0, -1, -1, -1, entry.getRxBytes(), entry.getRxPackets(), entry.getTxBytes(), entry.getTxPackets(), 0L);
    }

    private NetworkStats sliceNetworkStatsByUidTagAndMetered(NetworkStats stats) {
        return sliceNetworkStats(stats, new Function() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda19
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return StatsPullAtomService.lambda$sliceNetworkStatsByUidTagAndMetered$11((NetworkStats.Entry) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ NetworkStats.Entry lambda$sliceNetworkStatsByUidTagAndMetered$11(NetworkStats.Entry entry) {
        return new NetworkStats.Entry((String) null, entry.getUid(), -1, entry.getTag(), entry.getMetered(), -1, -1, entry.getRxBytes(), entry.getRxPackets(), entry.getTxBytes(), entry.getTxPackets(), 0L);
    }

    private NetworkStats sliceNetworkStats(NetworkStats stats, Function<NetworkStats.Entry, NetworkStats.Entry> slicer) {
        NetworkStats ret = new NetworkStats(0L, 1);
        Iterator it = stats.iterator();
        while (it.hasNext()) {
            NetworkStats.Entry e = (NetworkStats.Entry) it.next();
            ret = ret.addEntry(slicer.apply(e));
        }
        return ret;
    }

    private void registerWifiBytesTransferBackground() {
        StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setAdditiveFields(new int[]{3, 4, 5, 6}).build();
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.WIFI_BYTES_TRANSFER_BY_FG_BG, metadata, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerMobileBytesTransfer() {
        StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setAdditiveFields(new int[]{2, 3, 4, 5}).build();
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.MOBILE_BYTES_TRANSFER, metadata, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerMobileBytesTransferBackground() {
        StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setAdditiveFields(new int[]{3, 4, 5, 6}).build();
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.MOBILE_BYTES_TRANSFER_BY_FG_BG, metadata, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerBytesTransferByTagAndMetered() {
        StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setAdditiveFields(new int[]{4, 5, 6, 7}).build();
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.BYTES_TRANSFER_BY_TAG_AND_METERED, metadata, BackgroundThread.getExecutor(), this.mStatsCallbackImpl);
    }

    private void registerDataUsageBytesTransfer() {
        StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setAdditiveFields(new int[]{2, 3, 4, 5}).build();
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.DATA_USAGE_BYTES_TRANSFER, metadata, BackgroundThread.getExecutor(), this.mStatsCallbackImpl);
    }

    private void registerOemManagedBytesTransfer() {
        StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setAdditiveFields(new int[]{5, 6, 7, 8}).build();
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.OEM_MANAGED_BYTES_TRANSFER, metadata, BackgroundThread.getExecutor(), this.mStatsCallbackImpl);
    }

    private void registerBluetoothBytesTransfer() {
        StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setAdditiveFields(new int[]{2, 3}).build();
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.BLUETOOTH_BYTES_TRANSFER, metadata, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private static <T extends Parcelable> T awaitControllerInfo(SynchronousResultReceiver receiver) {
        if (receiver == null) {
            return null;
        }
        try {
            SynchronousResultReceiver.Result result = receiver.awaitResult((long) EXTERNAL_STATS_SYNC_TIMEOUT_MILLIS);
            if (result.bundle != null) {
                result.bundle.setDefusable(true);
                T data = (T) result.bundle.getParcelable(RESULT_RECEIVER_CONTROLLER_KEY);
                if (data != null) {
                    return data;
                }
            }
        } catch (TimeoutException e) {
            Slog.w(TAG, "timeout reading " + receiver.getName() + " stats");
        }
        return null;
    }

    private BluetoothActivityEnergyInfo fetchBluetoothData() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            final SynchronousResultReceiver bluetoothReceiver = new SynchronousResultReceiver("bluetooth");
            adapter.requestControllerActivityEnergyInfo(new BatteryExternalStatsWorker$$ExternalSyntheticLambda4(), new BluetoothAdapter.OnBluetoothActivityEnergyInfoCallback() { // from class: com.android.server.stats.pull.StatsPullAtomService.1
                public void onBluetoothActivityEnergyInfoAvailable(BluetoothActivityEnergyInfo info) {
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(StatsPullAtomService.RESULT_RECEIVER_CONTROLLER_KEY, info);
                    bluetoothReceiver.send(0, bundle);
                }

                public void onBluetoothActivityEnergyInfoError(int errorCode) {
                    Slog.w(StatsPullAtomService.TAG, "error reading Bluetooth stats: " + errorCode);
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(StatsPullAtomService.RESULT_RECEIVER_CONTROLLER_KEY, null);
                    bluetoothReceiver.send(0, bundle);
                }
            });
            return awaitControllerInfo(bluetoothReceiver);
        }
        Slog.e(TAG, "Failed to get bluetooth adapter!");
        return null;
    }

    int pullBluetoothBytesTransferLocked(int atomTag, List<StatsEvent> pulledData) {
        BluetoothActivityEnergyInfo info = fetchBluetoothData();
        if (info == null) {
            return 1;
        }
        for (UidTraffic traffic : info.getUidTraffic()) {
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, traffic.getUid(), traffic.getRxBytes(), traffic.getTxBytes()));
        }
        return 0;
    }

    private void registerKernelWakelock() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.KERNEL_WAKELOCK, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullKernelWakelockLocked(int atomTag, List<StatsEvent> pulledData) {
        KernelWakelockStats wakelockStats = this.mKernelWakelockReader.readKernelWakelockStats(this.mTmpWakelockStats);
        for (Map.Entry<String, KernelWakelockStats.Entry> ent : wakelockStats.entrySet()) {
            String name = ent.getKey();
            KernelWakelockStats.Entry kws = ent.getValue();
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, name, kws.mCount, kws.mVersion, kws.mTotalTime));
        }
        return 0;
    }

    private void registerCpuTimePerClusterFreq() {
        if (KernelCpuBpfTracking.isSupported()) {
            StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setAdditiveFields(new int[]{3}).build();
            this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.CPU_TIME_PER_CLUSTER_FREQ, metadata, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
        }
    }

    int pullCpuTimePerClusterFreqLocked(int atomTag, List<StatsEvent> pulledData) {
        int[] freqsClusters = KernelCpuBpfTracking.getFreqsClusters();
        long[] freqs = KernelCpuBpfTracking.getFreqs();
        long[] timesMs = KernelCpuTotalBpfMapReader.read();
        if (timesMs == null) {
            return 1;
        }
        for (int freqIndex = 0; freqIndex < timesMs.length; freqIndex++) {
            int cluster = freqsClusters[freqIndex];
            int freq = (int) freqs[freqIndex];
            long timeMs = timesMs[freqIndex];
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, cluster, freq, timeMs));
        }
        return 0;
    }

    private void registerCpuTimePerUid() {
        StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setAdditiveFields(new int[]{2, 3}).build();
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.CPU_TIME_PER_UID, metadata, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullCpuTimePerUidLocked(final int atomTag, final List<StatsEvent> pulledData) {
        this.mCpuUidUserSysTimeReader.readAbsolute(new KernelCpuUidTimeReader.Callback() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda17
            public final void onUidCpuTime(int i, Object obj) {
                StatsPullAtomService.lambda$pullCpuTimePerUidLocked$12(pulledData, atomTag, i, (long[]) obj);
            }
        });
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$pullCpuTimePerUidLocked$12(List pulledData, int atomTag, int uid, long[] timesUs) {
        long userTimeUs = timesUs[0];
        long systemTimeUs = timesUs[1];
        pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, uid, userTimeUs, systemTimeUs));
    }

    private void registerCpuCyclesPerUidCluster() {
        if (KernelCpuBpfTracking.isSupported() || KernelCpuBpfTracking.getClusters() > 0) {
            StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setAdditiveFields(new int[]{3, 4, 5}).build();
            this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.CPU_CYCLES_PER_UID_CLUSTER, metadata, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
        }
    }

    int pullCpuCyclesPerUidClusterLocked(int atomTag, List<StatsEvent> pulledData) {
        PowerProfile powerProfile = new PowerProfile(this.mContext);
        final int[] freqsClusters = KernelCpuBpfTracking.getFreqsClusters();
        final int clusters = KernelCpuBpfTracking.getClusters();
        final long[] freqs = KernelCpuBpfTracking.getFreqs();
        final double[] freqsPowers = new double[freqs.length];
        int freqClusterIndex = 0;
        int lastCluster = -1;
        int freqIndex = 0;
        while (freqIndex < freqs.length) {
            int cluster = freqsClusters[freqIndex];
            if (cluster != lastCluster) {
                freqClusterIndex = 0;
            }
            lastCluster = cluster;
            freqsPowers[freqIndex] = powerProfile.getAveragePowerForCpuCore(cluster, freqClusterIndex);
            freqIndex++;
            freqClusterIndex++;
        }
        final SparseArray<double[]> aggregated = new SparseArray<>();
        this.mCpuUidFreqTimeReader.readAbsolute(new KernelCpuUidTimeReader.Callback() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda12
            public final void onUidCpuTime(int i, Object obj) {
                StatsPullAtomService.lambda$pullCpuCyclesPerUidClusterLocked$13(aggregated, clusters, freqsClusters, freqs, freqsPowers, i, (long[]) obj);
            }
        });
        int size = aggregated.size();
        int i = 0;
        while (i < size) {
            int uid = aggregated.keyAt(i);
            double[] values = aggregated.valueAt(i);
            int cluster2 = 0;
            while (cluster2 < clusters) {
                pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, uid, cluster2, (long) (values[cluster2 * 3] / 1000000.0d), (long) values[(cluster2 * 3) + 1], (long) (values[(cluster2 * 3) + 2] / 1000.0d)));
                cluster2++;
                powerProfile = powerProfile;
                freqsClusters = freqsClusters;
            }
            i++;
            powerProfile = powerProfile;
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$pullCpuCyclesPerUidClusterLocked$13(SparseArray aggregated, int clusters, int[] freqsClusters, long[] freqs, double[] freqsPowers, int uid, long[] cpuFreqTimeMs) {
        int uid2;
        if (UserHandle.isIsolated(uid)) {
            return;
        }
        if (UserHandle.isSharedAppGid(uid)) {
            uid2 = 59999;
        } else {
            uid2 = UserHandle.getAppId(uid);
        }
        double[] values = (double[]) aggregated.get(uid2);
        if (values == null) {
            values = new double[clusters * 3];
            aggregated.put(uid2, values);
        }
        for (int freqIndex = 0; freqIndex < cpuFreqTimeMs.length; freqIndex++) {
            int cluster = freqsClusters[freqIndex];
            long timeMs = cpuFreqTimeMs[freqIndex];
            int i = cluster * 3;
            values[i] = values[i] + (freqs[freqIndex] * timeMs);
            int i2 = (cluster * 3) + 1;
            values[i2] = values[i2] + timeMs;
            int i3 = (cluster * 3) + 2;
            values[i3] = values[i3] + (freqsPowers[freqIndex] * timeMs);
        }
    }

    private void registerCpuTimePerUidFreq() {
        StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setAdditiveFields(new int[]{3}).build();
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.CPU_TIME_PER_UID_FREQ, metadata, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullCpuTimePerUidFreqLocked(int atomTag, List<StatsEvent> pulledData) {
        final SparseArray<long[]> aggregated = new SparseArray<>();
        this.mCpuUidFreqTimeReader.readAbsolute(new KernelCpuUidTimeReader.Callback() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda11
            public final void onUidCpuTime(int i, Object obj) {
                StatsPullAtomService.lambda$pullCpuTimePerUidFreqLocked$14(aggregated, i, (long[]) obj);
            }
        });
        int size = aggregated.size();
        for (int i = 0; i < size; i++) {
            int uid = aggregated.keyAt(i);
            long[] aggCpuFreqTimeMs = aggregated.valueAt(i);
            for (int freqIndex = 0; freqIndex < aggCpuFreqTimeMs.length; freqIndex++) {
                if (aggCpuFreqTimeMs[freqIndex] >= 10) {
                    pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, uid, freqIndex, aggCpuFreqTimeMs[freqIndex]));
                }
            }
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$pullCpuTimePerUidFreqLocked$14(SparseArray aggregated, int uid, long[] cpuFreqTimeMs) {
        int uid2;
        if (UserHandle.isIsolated(uid)) {
            return;
        }
        if (UserHandle.isSharedAppGid(uid)) {
            uid2 = 59999;
        } else {
            uid2 = UserHandle.getAppId(uid);
        }
        long[] aggCpuFreqTimeMs = (long[]) aggregated.get(uid2);
        if (aggCpuFreqTimeMs == null) {
            aggCpuFreqTimeMs = new long[cpuFreqTimeMs.length];
            aggregated.put(uid2, aggCpuFreqTimeMs);
        }
        for (int freqIndex = 0; freqIndex < cpuFreqTimeMs.length; freqIndex++) {
            aggCpuFreqTimeMs[freqIndex] = aggCpuFreqTimeMs[freqIndex] + cpuFreqTimeMs[freqIndex];
        }
    }

    private void registerCpuCyclesPerThreadGroupCluster() {
        if (KernelCpuBpfTracking.isSupported()) {
            StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setAdditiveFields(new int[]{3, 4}).build();
            this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.CPU_CYCLES_PER_THREAD_GROUP_CLUSTER, metadata, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
        }
    }

    int pullCpuCyclesPerThreadGroupCluster(int atomTag, List<StatsEvent> pulledData) {
        SystemServerCpuThreadReader.SystemServiceCpuThreadTimes times = ((BatteryStatsInternal) LocalServices.getService(BatteryStatsInternal.class)).getSystemServiceCpuThreadTimes();
        if (times == null) {
            return 1;
        }
        addCpuCyclesPerThreadGroupClusterAtoms(atomTag, pulledData, 2, times.threadCpuTimesUs);
        addCpuCyclesPerThreadGroupClusterAtoms(atomTag, pulledData, 1, times.binderThreadCpuTimesUs);
        KernelSingleProcessCpuThreadReader.ProcessCpuUsage surfaceFlingerTimes = this.mSurfaceFlingerProcessCpuThreadReader.readAbsolute();
        if (surfaceFlingerTimes != null && surfaceFlingerTimes.threadCpuTimesMillis != null) {
            long[] surfaceFlingerTimesUs = new long[surfaceFlingerTimes.threadCpuTimesMillis.length];
            for (int i = 0; i < surfaceFlingerTimesUs.length; i++) {
                surfaceFlingerTimesUs[i] = surfaceFlingerTimes.threadCpuTimesMillis[i] * 1000;
            }
            addCpuCyclesPerThreadGroupClusterAtoms(atomTag, pulledData, 3, surfaceFlingerTimesUs);
            return 0;
        }
        return 0;
    }

    private static void addCpuCyclesPerThreadGroupClusterAtoms(int atomTag, List<StatsEvent> pulledData, int threadGroup, long[] cpuTimesUs) {
        int[] freqsClusters = KernelCpuBpfTracking.getFreqsClusters();
        int clusters = KernelCpuBpfTracking.getClusters();
        long[] freqs = KernelCpuBpfTracking.getFreqs();
        long[] aggregatedCycles = new long[clusters];
        long[] aggregatedTimesUs = new long[clusters];
        for (int i = 0; i < cpuTimesUs.length; i++) {
            int i2 = freqsClusters[i];
            aggregatedCycles[i2] = aggregatedCycles[i2] + ((freqs[i] * cpuTimesUs[i]) / 1000);
            int i3 = freqsClusters[i];
            aggregatedTimesUs[i3] = aggregatedTimesUs[i3] + cpuTimesUs[i];
        }
        for (int cluster = 0; cluster < clusters; cluster++) {
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, threadGroup, cluster, aggregatedCycles[cluster] / 1000000, aggregatedTimesUs[cluster] / 1000));
        }
    }

    private void registerCpuActiveTime() {
        StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setAdditiveFields(new int[]{2}).build();
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.CPU_ACTIVE_TIME, metadata, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullCpuActiveTimeLocked(final int atomTag, final List<StatsEvent> pulledData) {
        this.mCpuUidActiveTimeReader.readAbsolute(new KernelCpuUidTimeReader.Callback() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda10
            public final void onUidCpuTime(int i, Object obj) {
                pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, i, ((Long) obj).longValue()));
            }
        });
        return 0;
    }

    private void registerCpuClusterTime() {
        StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setAdditiveFields(new int[]{3}).build();
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.CPU_CLUSTER_TIME, metadata, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullCpuClusterTimeLocked(final int atomTag, final List<StatsEvent> pulledData) {
        this.mCpuUidClusterTimeReader.readAbsolute(new KernelCpuUidTimeReader.Callback() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda21
            public final void onUidCpuTime(int i, Object obj) {
                StatsPullAtomService.lambda$pullCpuClusterTimeLocked$16(pulledData, atomTag, i, (long[]) obj);
            }
        });
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$pullCpuClusterTimeLocked$16(List pulledData, int atomTag, int uid, long[] cpuClusterTimesMs) {
        for (int i = 0; i < cpuClusterTimesMs.length; i++) {
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, uid, i, cpuClusterTimesMs[i]));
        }
    }

    private void registerWifiActivityInfo() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.WIFI_ACTIVITY_INFO, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2056=4] */
    int pullWifiActivityInfoLocked(int atomTag, List<StatsEvent> pulledData) {
        long token = Binder.clearCallingIdentity();
        try {
            final SynchronousResultReceiver wifiReceiver = new SynchronousResultReceiver("wifi");
            this.mWifiManager.getWifiActivityEnergyInfoAsync(new Executor() { // from class: com.android.server.stats.pull.StatsPullAtomService.2
                @Override // java.util.concurrent.Executor
                public void execute(Runnable runnable) {
                    runnable.run();
                }
            }, new WifiManager.OnWifiActivityEnergyInfoListener() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda23
                public final void onWifiActivityEnergyInfo(WifiActivityEnergyInfo wifiActivityEnergyInfo) {
                    StatsPullAtomService.lambda$pullWifiActivityInfoLocked$17(wifiReceiver, wifiActivityEnergyInfo);
                }
            });
            WifiActivityEnergyInfo wifiInfo = awaitControllerInfo(wifiReceiver);
            if (wifiInfo == null) {
                Binder.restoreCallingIdentity(token);
                return 1;
            }
            try {
                try {
                    pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, wifiInfo.getTimeSinceBootMillis(), wifiInfo.getStackState(), wifiInfo.getControllerTxDurationMillis(), wifiInfo.getControllerRxDurationMillis(), wifiInfo.getControllerIdleDurationMillis(), wifiInfo.getControllerEnergyUsedMicroJoules()));
                    Binder.restoreCallingIdentity(token);
                    return 0;
                } catch (RuntimeException e) {
                    e = e;
                    Slog.e(TAG, "failed to getWifiActivityEnergyInfoAsync", e);
                    Binder.restoreCallingIdentity(token);
                    return 1;
                }
            } catch (Throwable th) {
                e = th;
                Binder.restoreCallingIdentity(token);
                throw e;
            }
        } catch (RuntimeException e2) {
            e = e2;
        } catch (Throwable th2) {
            e = th2;
            Binder.restoreCallingIdentity(token);
            throw e;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$pullWifiActivityInfoLocked$17(SynchronousResultReceiver wifiReceiver, WifiActivityEnergyInfo info) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(RESULT_RECEIVER_CONTROLLER_KEY, info);
        wifiReceiver.send(0, bundle);
    }

    private void registerModemActivityInfo() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.MODEM_ACTIVITY_INFO, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2116=4] */
    int pullModemActivityInfoLocked(int atomTag, List<StatsEvent> pulledData) {
        long token = Binder.clearCallingIdentity();
        try {
            final CompletableFuture<ModemActivityInfo> modemFuture = new CompletableFuture<>();
            this.mTelephony.requestModemActivityInfo(new BatteryExternalStatsWorker$$ExternalSyntheticLambda4(), new OutcomeReceiver<ModemActivityInfo, TelephonyManager.ModemActivityInfoException>() { // from class: com.android.server.stats.pull.StatsPullAtomService.3
                /* JADX DEBUG: Method merged with bridge method */
                @Override // android.os.OutcomeReceiver
                public void onResult(ModemActivityInfo result) {
                    modemFuture.complete(result);
                }

                /* JADX DEBUG: Method merged with bridge method */
                @Override // android.os.OutcomeReceiver
                public void onError(TelephonyManager.ModemActivityInfoException e) {
                    Slog.w(StatsPullAtomService.TAG, "error reading modem stats:" + e);
                    modemFuture.complete(null);
                }
            });
            try {
                try {
                    ModemActivityInfo modemInfo = modemFuture.get(EXTERNAL_STATS_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                    if (modemInfo == null) {
                        Binder.restoreCallingIdentity(token);
                        return 1;
                    }
                    pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, modemInfo.getTimestampMillis(), modemInfo.getSleepTimeMillis(), modemInfo.getIdleTimeMillis(), modemInfo.getTransmitDurationMillisAtPowerLevel(0), modemInfo.getTransmitDurationMillisAtPowerLevel(1), modemInfo.getTransmitDurationMillisAtPowerLevel(2), modemInfo.getTransmitDurationMillisAtPowerLevel(3), modemInfo.getTransmitDurationMillisAtPowerLevel(4), modemInfo.getReceiveTimeMillis(), -1L));
                    Binder.restoreCallingIdentity(token);
                    return 0;
                } catch (Throwable th) {
                    e = th;
                    Binder.restoreCallingIdentity(token);
                    throw e;
                }
            } catch (InterruptedException | TimeoutException e) {
                Slog.w(TAG, "timeout or interrupt reading modem stats: " + e);
                Binder.restoreCallingIdentity(token);
                return 1;
            } catch (ExecutionException e2) {
                Slog.w(TAG, "exception reading modem stats: " + e2.getCause());
                Binder.restoreCallingIdentity(token);
                return 1;
            }
        } catch (Throwable th2) {
            e = th2;
        }
    }

    private void registerBluetoothActivityInfo() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.BLUETOOTH_ACTIVITY_INFO, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullBluetoothActivityInfoLocked(int atomTag, List<StatsEvent> pulledData) {
        BluetoothActivityEnergyInfo info = fetchBluetoothData();
        if (info == null) {
            return 1;
        }
        pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, info.getTimestampMillis(), info.getBluetoothStackState(), info.getControllerTxTimeMillis(), info.getControllerRxTimeMillis(), info.getControllerIdleTimeMillis(), info.getControllerEnergyUsed()));
        return 0;
    }

    private void registerSystemElapsedRealtime() {
        StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setCoolDownMillis(1000L).setTimeoutMillis(500L).build();
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.SYSTEM_ELAPSED_REALTIME, metadata, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullSystemElapsedRealtimeLocked(int atomTag, List<StatsEvent> pulledData) {
        pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, SystemClock.elapsedRealtime()));
        return 0;
    }

    private void registerSystemUptime() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.SYSTEM_UPTIME, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullSystemUptimeLocked(int atomTag, List<StatsEvent> pulledData) {
        pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, SystemClock.uptimeMillis()));
        return 0;
    }

    private void registerProcessMemoryState() {
        StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setAdditiveFields(new int[]{4, 5, 6, 7, 8}).build();
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.PROCESS_MEMORY_STATE, metadata, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullProcessMemoryStateLocked(int atomTag, List<StatsEvent> pulledData) {
        List<ProcessMemoryState> processMemoryStates = ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).getMemoryStateForProcesses();
        Iterator<ProcessMemoryState> it = processMemoryStates.iterator();
        while (it.hasNext()) {
            ProcessMemoryState processMemoryState = it.next();
            MemoryStatUtil.MemoryStat memoryStat = MemoryStatUtil.readMemoryStatFromFilesystem(processMemoryState.uid, processMemoryState.pid);
            if (memoryStat != null) {
                pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, processMemoryState.uid, processMemoryState.processName, processMemoryState.oomScore, memoryStat.pgfault, memoryStat.pgmajfault, memoryStat.rssInBytes, memoryStat.cacheInBytes, memoryStat.swapInBytes, -1L, -1L, -1));
                processMemoryStates = processMemoryStates;
                it = it;
            }
        }
        return 0;
    }

    private void registerProcessMemoryHighWaterMark() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.PROCESS_MEMORY_HIGH_WATER_MARK, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullProcessMemoryHighWaterMarkLocked(int atomTag, List<StatsEvent> pulledData) {
        List<ProcessMemoryState> managedProcessList = ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).getMemoryStateForProcesses();
        for (ProcessMemoryState managedProcess : managedProcessList) {
            ProcfsMemoryUtil.MemorySnapshot snapshot = ProcfsMemoryUtil.readMemorySnapshotFromProcfs(managedProcess.pid);
            if (snapshot != null) {
                pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, managedProcess.uid, managedProcess.processName, snapshot.rssHighWaterMarkInKilobytes * GadgetFunction.NCM, snapshot.rssHighWaterMarkInKilobytes));
            }
        }
        final SparseArray<String> processCmdlines = ProcfsMemoryUtil.getProcessCmdlines();
        managedProcessList.forEach(new Consumer() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda18
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                processCmdlines.delete(((ProcessMemoryState) obj).pid);
            }
        });
        int size = processCmdlines.size();
        for (int i = 0; i < size; i++) {
            ProcfsMemoryUtil.MemorySnapshot snapshot2 = ProcfsMemoryUtil.readMemorySnapshotFromProcfs(processCmdlines.keyAt(i));
            if (snapshot2 != null) {
                pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, snapshot2.uid, processCmdlines.valueAt(i), snapshot2.rssHighWaterMarkInKilobytes * GadgetFunction.NCM, snapshot2.rssHighWaterMarkInKilobytes));
            }
        }
        SystemProperties.set("sys.rss_hwm_reset.on", "1");
        return 0;
    }

    private void registerProcessMemorySnapshot() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.PROCESS_MEMORY_SNAPSHOT, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullProcessMemorySnapshot(int atomTag, List<StatsEvent> pulledData) {
        List<ProcessMemoryState> managedProcessList = ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).getMemoryStateForProcesses();
        KernelAllocationStats.ProcessGpuMem[] gpuAllocations = KernelAllocationStats.getGpuAllocations();
        SparseIntArray gpuMemPerPid = new SparseIntArray(gpuAllocations.length);
        for (KernelAllocationStats.ProcessGpuMem processGpuMem : gpuAllocations) {
            gpuMemPerPid.put(processGpuMem.pid, processGpuMem.gpuMemoryKb);
        }
        for (ProcessMemoryState managedProcess : managedProcessList) {
            ProcfsMemoryUtil.MemorySnapshot snapshot = ProcfsMemoryUtil.readMemorySnapshotFromProcfs(managedProcess.pid);
            if (snapshot != null) {
                pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, managedProcess.uid, managedProcess.processName, managedProcess.pid, managedProcess.oomScore, snapshot.rssInKilobytes, snapshot.anonRssInKilobytes, snapshot.swapInKilobytes, snapshot.anonRssInKilobytes + snapshot.swapInKilobytes, gpuMemPerPid.get(managedProcess.pid), managedProcess.hasForegroundServices));
            }
        }
        final SparseArray<String> processCmdlines = ProcfsMemoryUtil.getProcessCmdlines();
        managedProcessList.forEach(new Consumer() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda8
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                processCmdlines.delete(((ProcessMemoryState) obj).pid);
            }
        });
        int size = processCmdlines.size();
        for (int i = 0; i < size; i++) {
            int pid = processCmdlines.keyAt(i);
            ProcfsMemoryUtil.MemorySnapshot snapshot2 = ProcfsMemoryUtil.readMemorySnapshotFromProcfs(pid);
            if (snapshot2 != null) {
                pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, snapshot2.uid, processCmdlines.valueAt(i), pid, (int) JobSchedulerShellCommand.CMD_ERR_NO_JOB, snapshot2.rssInKilobytes, snapshot2.anonRssInKilobytes, snapshot2.swapInKilobytes, snapshot2.anonRssInKilobytes + snapshot2.swapInKilobytes, gpuMemPerPid.get(pid), false));
            }
        }
        return 0;
    }

    private void registerSystemIonHeapSize() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.SYSTEM_ION_HEAP_SIZE, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullSystemIonHeapSizeLocked(int atomTag, List<StatsEvent> pulledData) {
        long systemIonHeapSizeInBytes = IonMemoryUtil.readSystemIonHeapSizeFromDebugfs();
        pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, systemIonHeapSizeInBytes));
        return 0;
    }

    private void registerIonHeapSize() {
        if (!new File("/sys/kernel/ion/total_heaps_kb").exists()) {
            return;
        }
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.ION_HEAP_SIZE, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullIonHeapSizeLocked(int atomTag, List<StatsEvent> pulledData) {
        int ionHeapSizeInKilobytes = (int) Debug.getIonHeapsSizeKb();
        pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, ionHeapSizeInKilobytes));
        return 0;
    }

    private void registerProcessSystemIonHeapSize() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.PROCESS_SYSTEM_ION_HEAP_SIZE, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullProcessSystemIonHeapSizeLocked(int atomTag, List<StatsEvent> pulledData) {
        List<IonMemoryUtil.IonAllocations> result = IonMemoryUtil.readProcessSystemIonHeapSizesFromDebugfs();
        for (IonMemoryUtil.IonAllocations allocations : result) {
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, Process.getUidForPid(allocations.pid), ProcfsMemoryUtil.readCmdlineFromProcfs(allocations.pid), (int) (allocations.totalSizeInBytes / GadgetFunction.NCM), allocations.count, (int) (allocations.maxSizeInBytes / GadgetFunction.NCM)));
        }
        return 0;
    }

    private void registerProcessDmabufMemory() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.PROCESS_DMABUF_MEMORY, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullProcessDmabufMemory(int atomTag, List<StatsEvent> pulledData) {
        KernelAllocationStats.ProcessDmabuf[] procBufs = KernelAllocationStats.getDmabufAllocations();
        if (procBufs == null) {
            return 1;
        }
        for (KernelAllocationStats.ProcessDmabuf procBuf : procBufs) {
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, procBuf.uid, procBuf.processName, procBuf.oomScore, procBuf.retainedSizeKb, procBuf.retainedBuffersCount, 0, 0, procBuf.surfaceFlingerSizeKb, procBuf.surfaceFlingerCount));
        }
        return 0;
    }

    private void registerSystemMemory() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.SYSTEM_MEMORY, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullSystemMemory(int atomTag, List<StatsEvent> pulledData) {
        SystemMemoryUtil.Metrics metrics = SystemMemoryUtil.getMetrics();
        pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, metrics.unreclaimableSlabKb, metrics.vmallocUsedKb, metrics.pageTablesKb, metrics.kernelStackKb, metrics.totalIonKb, metrics.unaccountedKb, metrics.gpuTotalUsageKb, metrics.gpuPrivateAllocationsKb, metrics.dmaBufTotalExportedKb, metrics.shmemKb));
        return 0;
    }

    private void registerVmStat() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.VMSTAT, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullVmStat(int atomTag, List<StatsEvent> pulledData) {
        ProcfsMemoryUtil.VmStat vmStat = ProcfsMemoryUtil.readVmStat();
        if (vmStat != null) {
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, vmStat.oomKillCount));
            return 0;
        }
        return 0;
    }

    private void registerTemperature() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.TEMPERATURE, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2472=4] */
    int pullTemperatureLocked(int atomTag, List<StatsEvent> pulledData) {
        IThermalService thermalService = getIThermalService();
        if (thermalService == null) {
            return 1;
        }
        long callingToken = Binder.clearCallingIdentity();
        try {
            Temperature[] temperatures = thermalService.getCurrentTemperatures();
            for (Temperature temp : temperatures) {
                pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, temp.getType(), temp.getName(), (int) (temp.getValue() * 10.0f), temp.getStatus()));
            }
            return 0;
        } catch (RemoteException e) {
            Slog.e(TAG, "Disconnected from thermal service. Cannot pull temperatures.");
            return 1;
        } finally {
            Binder.restoreCallingIdentity(callingToken);
        }
    }

    private void registerCoolingDevice() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.COOLING_DEVICE, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2504=4] */
    int pullCooldownDeviceLocked(int atomTag, List<StatsEvent> pulledData) {
        IThermalService thermalService = getIThermalService();
        if (thermalService == null) {
            return 1;
        }
        long callingToken = Binder.clearCallingIdentity();
        try {
            CoolingDevice[] devices = thermalService.getCurrentCoolingDevices();
            for (CoolingDevice device : devices) {
                pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, device.getType(), device.getName(), (int) device.getValue()));
            }
            return 0;
        } catch (RemoteException e) {
            Slog.e(TAG, "Disconnected from thermal service. Cannot pull temperatures.");
            return 1;
        } finally {
            Binder.restoreCallingIdentity(callingToken);
        }
    }

    private void registerBinderCallsStats() {
        StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setAdditiveFields(new int[]{4, 5, 6, 8, 12}).build();
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.BINDER_CALLS, metadata, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullBinderCallsStatsLocked(int atomTag, List<StatsEvent> pulledData) {
        BinderCallsStatsService.Internal binderStats = (BinderCallsStatsService.Internal) LocalServices.getService(BinderCallsStatsService.Internal.class);
        if (binderStats == null) {
            Slog.e(TAG, "failed to get binderStats");
            return 1;
        }
        List<BinderCallsStats.ExportedCallStat> callStats = binderStats.getExportedCallStats();
        binderStats.reset();
        for (BinderCallsStats.ExportedCallStat callStat : callStats) {
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, callStat.workSourceUid, callStat.className, callStat.methodName, callStat.callCount, callStat.exceptionCount, callStat.latencyMicros, callStat.maxLatencyMicros, callStat.cpuTimeMicros, callStat.maxCpuTimeMicros, callStat.maxReplySizeBytes, callStat.maxRequestSizeBytes, callStat.recordedCallCount, callStat.screenInteractive, callStat.callingUid));
            binderStats = binderStats;
            callStats = callStats;
        }
        return 0;
    }

    private void registerBinderCallsStatsExceptions() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.BINDER_CALLS_EXCEPTIONS, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullBinderCallsStatsExceptionsLocked(int atomTag, List<StatsEvent> pulledData) {
        BinderCallsStatsService.Internal binderStats = (BinderCallsStatsService.Internal) LocalServices.getService(BinderCallsStatsService.Internal.class);
        if (binderStats == null) {
            Slog.e(TAG, "failed to get binderStats");
            return 1;
        }
        ArrayMap<String, Integer> exceptionStats = binderStats.getExportedExceptionStats();
        for (Map.Entry<String, Integer> entry : exceptionStats.entrySet()) {
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, entry.getKey(), entry.getValue().intValue()));
        }
        return 0;
    }

    private void registerLooperStats() {
        StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setAdditiveFields(new int[]{5, 6, 7, 8, 9}).build();
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.LOOPER_STATS, metadata, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullLooperStatsLocked(int atomTag, List<StatsEvent> pulledData) {
        LooperStats looperStats = (LooperStats) LocalServices.getService(LooperStats.class);
        if (looperStats == null) {
            return 1;
        }
        List<LooperStats.ExportedEntry> entries = looperStats.getEntries();
        looperStats.reset();
        for (LooperStats.ExportedEntry entry : entries) {
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, entry.workSourceUid, entry.handlerClassName, entry.threadName, entry.messageName, entry.messageCount, entry.exceptionCount, entry.recordedMessageCount, entry.totalLatencyMicros, entry.cpuUsageMicros, entry.isInteractive, entry.maxCpuUsageMicros, entry.maxLatencyMicros, entry.recordedDelayMessageCount, entry.delayMillis, entry.maxDelayMillis));
            looperStats = looperStats;
            entries = entries;
        }
        return 0;
    }

    private void registerDiskStats() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.DISK_STATS, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:10:0x002e -> B:46:0x0044). Please submit an issue!!! */
    int pullDiskStatsLocked(int atomTag, List<StatsEvent> pulledData) {
        byte[] junk = new byte[512];
        for (int i = 0; i < junk.length; i++) {
            junk[i] = (byte) i;
        }
        File tmp = new File(Environment.getDataDirectory(), "system/statsdperftest.tmp");
        FileOutputStream fos = null;
        IOException error = null;
        long before = SystemClock.elapsedRealtime();
        try {
            try {
                fos = new FileOutputStream(tmp);
                fos.write(junk);
                fos.close();
            } catch (IOException e) {
                error = e;
                if (fos != null) {
                    fos.close();
                }
            } catch (Throwable th) {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e2) {
                    }
                }
                throw th;
            }
        } catch (IOException e3) {
        }
        long latency = SystemClock.elapsedRealtime() - before;
        if (tmp.exists()) {
            tmp.delete();
        }
        if (error != null) {
            Slog.e(TAG, "Error performing diskstats latency test");
            latency = -1;
        }
        boolean fileBased = StorageManager.isFileEncryptedNativeOnly();
        int writeSpeed = -1;
        IStoraged storaged = getIStoragedService();
        if (storaged == null) {
            return 1;
        }
        try {
            writeSpeed = storaged.getRecentPerf();
        } catch (RemoteException e4) {
            Slog.e(TAG, "storaged not found");
        }
        pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, latency, fileBased, writeSpeed));
        return 0;
    }

    private void registerDirectoryUsage() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.DIRECTORY_USAGE, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullDirectoryUsageLocked(int atomTag, List<StatsEvent> pulledData) {
        StatFs statFsData = new StatFs(Environment.getDataDirectory().getAbsolutePath());
        StatFs statFsSystem = new StatFs(Environment.getRootDirectory().getAbsolutePath());
        StatFs statFsCache = new StatFs(Environment.getDownloadCacheDirectory().getAbsolutePath());
        StatFs metadataFsSystem = new StatFs(Environment.getMetadataDirectory().getAbsolutePath());
        pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, 1, statFsData.getAvailableBytes(), statFsData.getTotalBytes()));
        pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, 2, statFsCache.getAvailableBytes(), statFsCache.getTotalBytes()));
        pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, 3, statFsSystem.getAvailableBytes(), statFsSystem.getTotalBytes()));
        pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, 4, metadataFsSystem.getAvailableBytes(), metadataFsSystem.getTotalBytes()));
        return 0;
    }

    private void registerAppSize() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.APP_SIZE, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullAppSizeLocked(int atomTag, List<StatsEvent> pulledData) {
        try {
            String jsonStr = IoUtils.readFileAsString(DiskStatsLoggingService.DUMPSYS_CACHE_PATH);
            JSONObject json = new JSONObject(jsonStr);
            long cache_time = json.optLong(DiskStatsFileLogger.LAST_QUERY_TIMESTAMP_KEY, -1L);
            JSONArray pkg_names = json.getJSONArray(DiskStatsFileLogger.PACKAGE_NAMES_KEY);
            JSONArray app_sizes = json.getJSONArray(DiskStatsFileLogger.APP_SIZES_KEY);
            JSONArray app_data_sizes = json.getJSONArray(DiskStatsFileLogger.APP_DATA_KEY);
            JSONArray app_cache_sizes = json.getJSONArray(DiskStatsFileLogger.APP_CACHES_KEY);
            int length = pkg_names.length();
            try {
                if (app_sizes.length() == length && app_data_sizes.length() == length) {
                    if (app_cache_sizes.length() == length) {
                        int i = 0;
                        while (i < length) {
                            int i2 = i;
                            JSONArray app_cache_sizes2 = app_cache_sizes;
                            int length2 = length;
                            JSONArray app_sizes2 = app_sizes;
                            JSONArray app_data_sizes2 = app_data_sizes;
                            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, pkg_names.getString(i), app_sizes.optLong(i, -1L), app_data_sizes.optLong(i, -1L), app_cache_sizes.optLong(i, -1L), cache_time));
                            i = i2 + 1;
                            app_cache_sizes = app_cache_sizes2;
                            length = length2;
                            app_sizes = app_sizes2;
                            app_data_sizes = app_data_sizes2;
                        }
                        return 0;
                    }
                }
                Slog.e(TAG, "formatting error in diskstats cache file!");
                return 1;
            } catch (IOException | JSONException e) {
                Slog.w(TAG, "Unable to read diskstats cache file within pullAppSize");
                return 1;
            }
        } catch (IOException | JSONException e2) {
        }
    }

    private void registerCategorySize() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.CATEGORY_SIZE, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullCategorySizeLocked(int atomTag, List<StatsEvent> pulledData) {
        try {
            String jsonStr = IoUtils.readFileAsString(DiskStatsLoggingService.DUMPSYS_CACHE_PATH);
            JSONObject json = new JSONObject(jsonStr);
            long cacheTime = json.optLong(DiskStatsFileLogger.LAST_QUERY_TIMESTAMP_KEY, -1L);
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, 1, json.optLong(DiskStatsFileLogger.APP_SIZE_AGG_KEY, -1L), cacheTime));
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, 2, json.optLong(DiskStatsFileLogger.APP_DATA_SIZE_AGG_KEY, -1L), cacheTime));
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, 3, json.optLong(DiskStatsFileLogger.APP_CACHE_AGG_KEY, -1L), cacheTime));
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, 4, json.optLong(DiskStatsFileLogger.PHOTOS_KEY, -1L), cacheTime));
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, 5, json.optLong(DiskStatsFileLogger.VIDEOS_KEY, -1L), cacheTime));
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, 6, json.optLong(DiskStatsFileLogger.AUDIO_KEY, -1L), cacheTime));
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, 7, json.optLong(DiskStatsFileLogger.DOWNLOADS_KEY, -1L), cacheTime));
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, 8, json.optLong(DiskStatsFileLogger.SYSTEM_KEY, -1L), cacheTime));
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, 9, json.optLong(DiskStatsFileLogger.MISC_KEY, -1L), cacheTime));
            return 0;
        } catch (IOException | JSONException e) {
            Slog.w(TAG, "Unable to read diskstats cache file within pullCategorySize");
            return 1;
        }
    }

    private void registerNumFingerprintsEnrolled() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.NUM_FINGERPRINTS_ENROLLED, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerNumFacesEnrolled() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.NUM_FACES_ENROLLED, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2858=4] */
    /* JADX INFO: Access modifiers changed from: private */
    public int pullNumBiometricsEnrolledLocked(int modality, int atomTag, List<StatsEvent> pulledData) {
        FingerprintManager fingerprintManager;
        UserManager userManager;
        int numEnrolled;
        PackageManager pm = this.mContext.getPackageManager();
        if (pm.hasSystemFeature("android.hardware.fingerprint")) {
            FingerprintManager fingerprintManager2 = (FingerprintManager) this.mContext.getSystemService(FingerprintManager.class);
            fingerprintManager = fingerprintManager2;
        } else {
            fingerprintManager = null;
        }
        FaceManager faceManager = pm.hasSystemFeature("android.hardware.biometrics.face") ? (FaceManager) this.mContext.getSystemService(FaceManager.class) : null;
        if (modality == 1 && fingerprintManager == null) {
            return 1;
        }
        int i = 4;
        if ((modality == 4 && faceManager == null) || (userManager = (UserManager) this.mContext.getSystemService(UserManager.class)) == null) {
            return 1;
        }
        long token = Binder.clearCallingIdentity();
        try {
            for (UserInfo user : userManager.getUsers()) {
                int userId = user.getUserHandle().getIdentifier();
                if (modality == 1) {
                    numEnrolled = fingerprintManager.getEnrolledFingerprints(userId).size();
                } else if (modality != i) {
                    Binder.restoreCallingIdentity(token);
                    return 1;
                } else {
                    numEnrolled = faceManager.getEnrolledFaces(userId).size();
                }
                try {
                    try {
                        pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, userId, numEnrolled));
                        i = 4;
                    } catch (Throwable th) {
                        th = th;
                        Binder.restoreCallingIdentity(token);
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    Binder.restoreCallingIdentity(token);
                    throw th;
                }
            }
            Binder.restoreCallingIdentity(token);
            return 0;
        } catch (Throwable th3) {
            th = th3;
        }
    }

    private void registerProcStats() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.PROC_STATS, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerProcStatsPkgProc() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.PROC_STATS_PKG_PROC, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerProcessState() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.PROCESS_STATE, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerProcessAssociation() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.PROCESS_ASSOCIATION, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2939=4] */
    private ProcessStats getStatsFromProcessStatsService(int atomTag) {
        IProcessStats processStatsService = getIProcessStatsService();
        if (processStatsService == null) {
            return null;
        }
        long token = Binder.clearCallingIdentity();
        try {
            long lastHighWaterMark = readProcStatsHighWaterMark(atomTag);
            ProcessStats procStats = new ProcessStats(false);
            long highWaterMark = processStatsService.getCommittedStatsMerged(lastHighWaterMark, 31, true, (List) null, procStats);
            new File(this.mBaseDir.getAbsolutePath() + SliceClientPermissions.SliceAuthority.DELIMITER + highWaterMarkFilePrefix(atomTag) + "_" + lastHighWaterMark).delete();
            new File(this.mBaseDir.getAbsolutePath() + SliceClientPermissions.SliceAuthority.DELIMITER + highWaterMarkFilePrefix(atomTag) + "_" + highWaterMark).createNewFile();
            return procStats;
        } catch (RemoteException | IOException e) {
            Slog.e(TAG, "Getting procstats failed: ", e);
            return null;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int pullProcStatsLocked(int atomTag, List<StatsEvent> pulledData) {
        ProcessStats procStats = getStatsFromProcessStatsService(atomTag);
        if (procStats == null) {
            return 1;
        }
        ProtoOutputStream[] protoStreams = new ProtoOutputStream[5];
        for (int i = 0; i < protoStreams.length; i++) {
            protoStreams[i] = new ProtoOutputStream();
        }
        procStats.dumpAggregatedProtoForStatsd(protoStreams, 58982L);
        for (int i2 = 0; i2 < protoStreams.length; i2++) {
            byte[] bytes = protoStreams[i2].getBytes();
            if (bytes.length > 0) {
                pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, bytes, i2));
            }
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int pullProcessStateLocked(int atomTag, List<StatsEvent> pulledData) {
        ProcessStats procStats = getStatsFromProcessStatsService(atomTag);
        if (procStats == null) {
            return 1;
        }
        procStats.dumpProcessState(atomTag, new StatsEventOutput(pulledData));
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int pullProcessAssociationLocked(int atomTag, List<StatsEvent> pulledData) {
        ProcessStats procStats = getStatsFromProcessStatsService(atomTag);
        if (procStats == null) {
            return 1;
        }
        procStats.dumpProcessAssociation(atomTag, new StatsEventOutput(pulledData));
        return 0;
    }

    private String highWaterMarkFilePrefix(int atomTag) {
        if (atomTag == 10029) {
            return String.valueOf(31);
        }
        if (atomTag == 10034) {
            return String.valueOf(2);
        }
        return "atom-" + atomTag;
    }

    private long readProcStatsHighWaterMark(final int atomTag) {
        try {
            File[] files = this.mBaseDir.listFiles(new FilenameFilter() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda7
                @Override // java.io.FilenameFilter
                public final boolean accept(File file, String str) {
                    return StatsPullAtomService.this.m6654x74cdcfa8(atomTag, file, str);
                }
            });
            if (files != null && files.length != 0) {
                if (files.length > 1) {
                    Slog.e(TAG, "Only 1 file expected for high water mark. Found " + files.length);
                }
                return Long.valueOf(files[0].getName().split("_")[1]).longValue();
            }
            return 0L;
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Failed to parse file name.", e);
            return 0L;
        } catch (SecurityException e2) {
            Slog.e(TAG, "Failed to get procstats high watermark file.", e2);
            return 0L;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$readProcStatsHighWaterMark$20$com-android-server-stats-pull-StatsPullAtomService  reason: not valid java name */
    public /* synthetic */ boolean m6654x74cdcfa8(int atomTag, File d, String name) {
        return name.toLowerCase().startsWith(highWaterMarkFilePrefix(atomTag) + '_');
    }

    private void registerDiskIO() {
        StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setAdditiveFields(new int[]{2, 3, 4, 5, 6, 7, 8, 9, 10, 11}).setCoolDownMillis((long) BackupAgentTimeoutParameters.DEFAULT_QUOTA_EXCEEDED_TIMEOUT_MILLIS).build();
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.DISK_IO, metadata, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullDiskIOLocked(final int atomTag, final List<StatsEvent> pulledData) {
        this.mStoragedUidIoStatsReader.readAbsolute(new StoragedUidIoStatsReader.Callback() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda2
            public final void onUidStorageStats(int i, long j, long j2, long j3, long j4, long j5, long j6, long j7, long j8, long j9, long j10) {
                pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, i, j, j2, j3, j4, j5, j6, j7, j8, j9, j10));
            }
        });
        return 0;
    }

    private void registerPowerProfile() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.POWER_PROFILE, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullPowerProfileLocked(int atomTag, List<StatsEvent> pulledData) {
        PowerProfile powerProfile = new PowerProfile(this.mContext);
        ProtoOutputStream proto = new ProtoOutputStream();
        powerProfile.dumpDebug(proto);
        proto.flush();
        pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, proto.getBytes()));
        return 0;
    }

    private void registerProcessCpuTime() {
        StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setCoolDownMillis(5000L).build();
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.PROCESS_CPU_TIME, metadata, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullProcessCpuTimeLocked(int atomTag, List<StatsEvent> pulledData) {
        if (this.mProcessCpuTracker == null) {
            ProcessCpuTracker processCpuTracker = new ProcessCpuTracker(false);
            this.mProcessCpuTracker = processCpuTracker;
            processCpuTracker.init();
        }
        this.mProcessCpuTracker.update();
        for (int i = 0; i < this.mProcessCpuTracker.countStats(); i++) {
            ProcessCpuTracker.Stats st = this.mProcessCpuTracker.getStats(i);
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, st.uid, st.name, st.base_utime, st.base_stime));
        }
        return 0;
    }

    private void registerCpuTimePerThreadFreq() {
        StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setAdditiveFields(new int[]{7, 9, 11, 13, 15, 17, 19, 21}).build();
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.CPU_TIME_PER_THREAD_FREQ, metadata, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullCpuTimePerThreadFreqLocked(int atomTag, List<StatsEvent> pulledData) {
        KernelCpuThreadReaderDiff kernelCpuThreadReaderDiff = this.mKernelCpuThreadReader;
        if (kernelCpuThreadReaderDiff == null) {
            Slog.e(TAG, "mKernelCpuThreadReader is null");
            return 1;
        }
        ArrayList<KernelCpuThreadReader.ProcessCpuUsage> processCpuUsages = kernelCpuThreadReaderDiff.getProcessCpuUsageDiffed();
        if (processCpuUsages == null) {
            Slog.e(TAG, "processCpuUsages is null");
            return 1;
        }
        int[] cpuFrequencies = this.mKernelCpuThreadReader.getCpuFrequenciesKhz();
        if (cpuFrequencies.length > 8) {
            String message = "Expected maximum 8 frequencies, but got " + cpuFrequencies.length;
            Slog.w(TAG, message);
            return 1;
        }
        for (int i = 0; i < processCpuUsages.size(); i++) {
            KernelCpuThreadReader.ProcessCpuUsage processCpuUsage = processCpuUsages.get(i);
            ArrayList<KernelCpuThreadReader.ThreadCpuUsage> threadCpuUsages = processCpuUsage.threadCpuUsages;
            for (int j = 0; j < threadCpuUsages.size(); j++) {
                KernelCpuThreadReader.ThreadCpuUsage threadCpuUsage = threadCpuUsages.get(j);
                if (threadCpuUsage.usageTimesMillis.length != cpuFrequencies.length) {
                    String message2 = "Unexpected number of usage times, expected " + cpuFrequencies.length + " but got " + threadCpuUsage.usageTimesMillis.length;
                    Slog.w(TAG, message2);
                    return 1;
                }
                int[] frequencies = new int[8];
                int[] usageTimesMillis = new int[8];
                for (int k = 0; k < 8; k++) {
                    if (k < cpuFrequencies.length) {
                        frequencies[k] = cpuFrequencies[k];
                        usageTimesMillis[k] = threadCpuUsage.usageTimesMillis[k];
                    } else {
                        frequencies[k] = 0;
                        usageTimesMillis[k] = 0;
                    }
                }
                int k2 = processCpuUsage.uid;
                pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, k2, processCpuUsage.processId, threadCpuUsage.threadId, processCpuUsage.processName, threadCpuUsage.threadName, frequencies[0], usageTimesMillis[0], frequencies[1], usageTimesMillis[1], frequencies[2], usageTimesMillis[2], frequencies[3], usageTimesMillis[3], frequencies[4], usageTimesMillis[4], frequencies[5], usageTimesMillis[5], frequencies[6], usageTimesMillis[6], frequencies[7], usageTimesMillis[7]));
            }
        }
        return 0;
    }

    private long milliAmpHrsToNanoAmpSecs(double mAh) {
        return (long) ((3.6E9d * mAh) + 0.5d);
    }

    private void registerDeviceCalculatedPowerUse() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.DEVICE_CALCULATED_POWER_USE, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullDeviceCalculatedPowerUseLocked(int atomTag, List<StatsEvent> pulledData) {
        BatteryStatsManager bsm = (BatteryStatsManager) this.mContext.getSystemService(BatteryStatsManager.class);
        try {
            BatteryUsageStats stats = bsm.getBatteryUsageStats();
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, milliAmpHrsToNanoAmpSecs(stats.getConsumedPower())));
            return 0;
        } catch (Exception e) {
            Log.e(TAG, "Could not obtain battery usage stats", e);
            return 1;
        }
    }

    private void registerDebugElapsedClock() {
        StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setAdditiveFields(new int[]{1, 2, 3, 4}).build();
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.DEBUG_ELAPSED_CLOCK, metadata, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullDebugElapsedClockLocked(int atomTag, List<StatsEvent> pulledData) {
        long elapsedMillis;
        long elapsedMillis2 = SystemClock.elapsedRealtime();
        long j = this.mDebugElapsedClockPreviousValue;
        long clockDiffMillis = j == 0 ? 0L : elapsedMillis2 - j;
        pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, this.mDebugElapsedClockPullCount, elapsedMillis2, elapsedMillis2, clockDiffMillis, 1));
        long j2 = this.mDebugElapsedClockPullCount;
        if (j2 % 2 != 1) {
            elapsedMillis = elapsedMillis2;
        } else {
            elapsedMillis = elapsedMillis2;
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, j2, elapsedMillis2, elapsedMillis, clockDiffMillis, 2));
        }
        this.mDebugElapsedClockPullCount++;
        this.mDebugElapsedClockPreviousValue = elapsedMillis;
        return 0;
    }

    private void registerDebugFailingElapsedClock() {
        StatsManager.PullAtomMetadata metadata = new StatsManager.PullAtomMetadata.Builder().setAdditiveFields(new int[]{1, 2, 3, 4}).build();
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.DEBUG_FAILING_ELAPSED_CLOCK, metadata, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullDebugFailingElapsedClockLocked(int atomTag, List<StatsEvent> pulledData) {
        long j;
        long elapsedMillis = SystemClock.elapsedRealtime();
        long j2 = this.mDebugFailingElapsedClockPullCount;
        long j3 = 1 + j2;
        this.mDebugFailingElapsedClockPullCount = j3;
        if (j2 % 5 == 0) {
            this.mDebugFailingElapsedClockPreviousValue = elapsedMillis;
            Slog.e(TAG, "Failing debug elapsed clock");
            return 1;
        }
        long j4 = this.mDebugFailingElapsedClockPreviousValue;
        if (j4 == 0) {
            j = 0;
        } else {
            j = elapsedMillis - j4;
        }
        pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, j3, elapsedMillis, elapsedMillis, j));
        this.mDebugFailingElapsedClockPreviousValue = elapsedMillis;
        return 0;
    }

    private void registerBuildInformation() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.BUILD_INFORMATION, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullBuildInformationLocked(int atomTag, List<StatsEvent> pulledData) {
        pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, Build.FINGERPRINT, Build.BRAND, Build.PRODUCT, Build.DEVICE, Build.VERSION.RELEASE_OR_CODENAME, Build.ID, Build.VERSION.INCREMENTAL, Build.TYPE, Build.TAGS));
        return 0;
    }

    private void registerRoleHolder() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.ROLE_HOLDER, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3331=4] */
    int pullRoleHolderLocked(int atomTag, List<StatsEvent> pulledData) {
        long callingToken = Binder.clearCallingIdentity();
        try {
            PackageManager pm = this.mContext.getPackageManager();
            RoleManagerLocal roleManagerLocal = (RoleManagerLocal) LocalManagerRegistry.getManager(RoleManagerLocal.class);
            List<UserInfo> users = ((UserManager) this.mContext.getSystemService(UserManager.class)).getUsers();
            int numUsers = users.size();
            int userNum = 0;
            while (true) {
                int i = 0;
                if (userNum >= numUsers) {
                    Binder.restoreCallingIdentity(callingToken);
                    return 0;
                }
                int userId = users.get(userNum).getUserHandle().getIdentifier();
                Map<String, Set<String>> roles = roleManagerLocal.getRolesAndHolders(userId);
                for (Map.Entry<String, Set<String>> roleEntry : roles.entrySet()) {
                    String roleName = roleEntry.getKey();
                    Set<String> packageNames = roleEntry.getValue();
                    for (String packageName : packageNames) {
                        try {
                            PackageInfo pkg = pm.getPackageInfoAsUser(packageName, i, userId);
                            PackageManager pm2 = pm;
                            RoleManagerLocal roleManagerLocal2 = roleManagerLocal;
                            try {
                                pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, pkg.applicationInfo.uid, packageName, roleName));
                                pm = pm2;
                                roleManagerLocal = roleManagerLocal2;
                                i = 0;
                            } catch (Throwable th) {
                                e = th;
                                Binder.restoreCallingIdentity(callingToken);
                                throw e;
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            Slog.w(TAG, "Role holder " + packageName + " not found");
                            Binder.restoreCallingIdentity(callingToken);
                            return 1;
                        }
                    }
                    roleManagerLocal = roleManagerLocal;
                    i = 0;
                }
                userNum++;
                roleManagerLocal = roleManagerLocal;
            }
        } catch (Throwable th2) {
            e = th2;
        }
    }

    private void registerDangerousPermissionState() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.DANGEROUS_PERMISSION_STATE, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullDangerousPermissionStateLocked(int atomTag, List<StatsEvent> pulledData) {
        int pkgNum;
        int numPkgs;
        List<PackageInfo> pkgs;
        UserHandle user;
        int userNum;
        int numUsers;
        List<UserInfo> users;
        PackageManager pm;
        HashSet hashSet;
        PackageInfo pkg;
        int pkgNum2;
        int numPkgs2;
        List<PackageInfo> pkgs2;
        UserHandle user2;
        int numPerms;
        int permNum;
        int numUsers2;
        List<UserInfo> users2;
        PackageManager pm2;
        int userNum2;
        HashSet hashSet2;
        PermissionInfo permissionInfo;
        StatsEvent e;
        int i = atomTag;
        long token = Binder.clearCallingIdentity();
        float samplingRate = DeviceConfig.getFloat("permissions", DANGEROUS_PERMISSION_STATE_SAMPLE_RATE, 0.015f);
        HashSet hashSet3 = new HashSet();
        try {
            PackageManager pm3 = this.mContext.getPackageManager();
            List<UserInfo> users3 = ((UserManager) this.mContext.getSystemService(UserManager.class)).getUsers();
            int numUsers3 = users3.size();
            int userNum3 = 0;
            while (userNum3 < numUsers3) {
                UserHandle user3 = users3.get(userNum3).getUserHandle();
                List<PackageInfo> pkgs3 = pm3.getInstalledPackagesAsUser(4096, user3.getIdentifier());
                int numPkgs3 = pkgs3.size();
                int pkgNum3 = 0;
                while (pkgNum3 < numPkgs3) {
                    PackageInfo pkg2 = pkgs3.get(pkgNum3);
                    if (pkg2.requestedPermissions == null) {
                        pkgNum = pkgNum3;
                        numPkgs = numPkgs3;
                        pkgs = pkgs3;
                        user = user3;
                        userNum = userNum3;
                        numUsers = numUsers3;
                        users = users3;
                        pm = pm3;
                        hashSet = hashSet3;
                    } else if (hashSet3.contains(Integer.valueOf(pkg2.applicationInfo.uid))) {
                        pkgNum = pkgNum3;
                        numPkgs = numPkgs3;
                        pkgs = pkgs3;
                        user = user3;
                        userNum = userNum3;
                        numUsers = numUsers3;
                        users = users3;
                        pm = pm3;
                        hashSet = hashSet3;
                    } else {
                        hashSet3.add(Integer.valueOf(pkg2.applicationInfo.uid));
                        if (i == 10067) {
                            try {
                                if (ThreadLocalRandom.current().nextFloat() > samplingRate) {
                                    pkgNum = pkgNum3;
                                    numPkgs = numPkgs3;
                                    pkgs = pkgs3;
                                    user = user3;
                                    userNum = userNum3;
                                    numUsers = numUsers3;
                                    users = users3;
                                    pm = pm3;
                                    hashSet = hashSet3;
                                }
                            } catch (Throwable th) {
                                t = th;
                                try {
                                    Log.e(TAG, "Could not read permissions", t);
                                    return 1;
                                } finally {
                                    Binder.restoreCallingIdentity(token);
                                }
                            }
                        }
                        int numPerms2 = pkg2.requestedPermissions.length;
                        int permNum2 = 0;
                        while (permNum2 < numPerms2) {
                            int userNum4 = userNum3;
                            String permName = pkg2.requestedPermissions[permNum2];
                            try {
                                permissionInfo = pm3.getPermissionInfo(permName, 0);
                            } catch (PackageManager.NameNotFoundException e2) {
                                pkg = pkg2;
                                pkgNum2 = pkgNum3;
                                numPkgs2 = numPkgs3;
                                pkgs2 = pkgs3;
                                user2 = user3;
                                numPerms = numPerms2;
                                permNum = permNum2;
                                numUsers2 = numUsers3;
                                users2 = users3;
                                pm2 = pm3;
                                userNum2 = userNum4;
                            }
                            try {
                                int permissionFlags = pm3.getPermissionFlags(permName, pkg2.packageName, user3);
                                numPerms = numPerms2;
                                if (permName.startsWith(COMMON_PERMISSION_PREFIX)) {
                                    permName = permName.substring(COMMON_PERMISSION_PREFIX.length());
                                }
                                if (i == 10050) {
                                    pkgNum2 = pkgNum3;
                                    pkg = pkg2;
                                    numPkgs2 = numPkgs3;
                                    pkgs2 = pkgs3;
                                    user2 = user3;
                                    permNum = permNum2;
                                    e = FrameworkStatsLog.buildStatsEvent(atomTag, permName, pkg2.applicationInfo.uid, "", (pkg2.requestedPermissionsFlags[permNum2] & 2) != 0, permissionFlags, permissionInfo.getProtection() | permissionInfo.getProtectionFlags());
                                    numUsers2 = numUsers3;
                                    users2 = users3;
                                    pm2 = pm3;
                                    userNum2 = userNum4;
                                    hashSet2 = hashSet3;
                                } else {
                                    pkg = pkg2;
                                    pkgNum2 = pkgNum3;
                                    numPkgs2 = numPkgs3;
                                    pkgs2 = pkgs3;
                                    user2 = user3;
                                    permNum = permNum2;
                                    userNum2 = userNum4;
                                    numUsers2 = numUsers3;
                                    users2 = users3;
                                    pm2 = pm3;
                                    hashSet2 = hashSet3;
                                    try {
                                        e = FrameworkStatsLog.buildStatsEvent(atomTag, permName, pkg.applicationInfo.uid, (pkg.requestedPermissionsFlags[permNum] & 2) != 0, permissionFlags, permissionInfo.getProtection() | permissionInfo.getProtectionFlags());
                                    } catch (Throwable th2) {
                                        t = th2;
                                        Log.e(TAG, "Could not read permissions", t);
                                        return 1;
                                    }
                                }
                            } catch (PackageManager.NameNotFoundException e3) {
                                pkg = pkg2;
                                pkgNum2 = pkgNum3;
                                numPkgs2 = numPkgs3;
                                pkgs2 = pkgs3;
                                user2 = user3;
                                numPerms = numPerms2;
                                permNum = permNum2;
                                numUsers2 = numUsers3;
                                users2 = users3;
                                pm2 = pm3;
                                userNum2 = userNum4;
                                hashSet2 = hashSet3;
                                permNum2 = permNum + 1;
                                users3 = users2;
                                pm3 = pm2;
                                userNum3 = userNum2;
                                numUsers3 = numUsers2;
                                pkg2 = pkg;
                                hashSet3 = hashSet2;
                                numPerms2 = numPerms;
                                pkgNum3 = pkgNum2;
                                numPkgs3 = numPkgs2;
                                pkgs3 = pkgs2;
                                user3 = user2;
                                i = atomTag;
                            }
                            try {
                                pulledData.add(e);
                                permNum2 = permNum + 1;
                                users3 = users2;
                                pm3 = pm2;
                                userNum3 = userNum2;
                                numUsers3 = numUsers2;
                                pkg2 = pkg;
                                hashSet3 = hashSet2;
                                numPerms2 = numPerms;
                                pkgNum3 = pkgNum2;
                                numPkgs3 = numPkgs2;
                                pkgs3 = pkgs2;
                                user3 = user2;
                                i = atomTag;
                            } catch (Throwable th3) {
                                t = th3;
                                Log.e(TAG, "Could not read permissions", t);
                                return 1;
                            }
                        }
                        pkgNum = pkgNum3;
                        numPkgs = numPkgs3;
                        pkgs = pkgs3;
                        user = user3;
                        userNum = userNum3;
                        numUsers = numUsers3;
                        users = users3;
                        pm = pm3;
                        hashSet = hashSet3;
                    }
                    i = atomTag;
                    users3 = users;
                    pm3 = pm;
                    userNum3 = userNum;
                    numUsers3 = numUsers;
                    hashSet3 = hashSet;
                    numPkgs3 = numPkgs;
                    pkgs3 = pkgs;
                    user3 = user;
                    pkgNum3 = pkgNum + 1;
                }
                userNum3++;
                i = atomTag;
            }
            return 0;
        } catch (Throwable th4) {
            t = th4;
        }
    }

    private void registerTimeZoneDataInfo() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.TIME_ZONE_DATA_INFO, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullTimeZoneDataInfoLocked(int atomTag, List<StatsEvent> pulledData) {
        try {
            String tzDbVersion = TimeZone.getTZDataVersion();
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, tzDbVersion));
            return 0;
        } catch (MissingResourceException e) {
            Slog.e(TAG, "Getting tzdb version failed: ", e);
            return 1;
        }
    }

    private void registerTimeZoneDetectorState() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.TIME_ZONE_DETECTOR_STATE, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullTimeZoneDetectorStateLocked(int atomTag, List<StatsEvent> pulledData) {
        long token = Binder.clearCallingIdentity();
        try {
            try {
                TimeZoneDetectorInternal timeZoneDetectorInternal = (TimeZoneDetectorInternal) LocalServices.getService(TimeZoneDetectorInternal.class);
                MetricsTimeZoneDetectorState metricsState = timeZoneDetectorInternal.generateMetricsState();
                try {
                    pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, metricsState.isTelephonyDetectionSupported(), metricsState.isGeoDetectionSupported(), metricsState.getUserLocationEnabledSetting(), metricsState.getAutoDetectionEnabledSetting(), metricsState.getGeoDetectionEnabledSetting(), convertToMetricsDetectionMode(metricsState.getDetectionMode()), metricsState.getDeviceTimeZoneIdOrdinal(), convertTimeZoneSuggestionToProtoBytes(metricsState.getLatestManualSuggestion()), convertTimeZoneSuggestionToProtoBytes(metricsState.getLatestTelephonySuggestion()), convertTimeZoneSuggestionToProtoBytes(metricsState.getLatestGeolocationSuggestion()), metricsState.isTelephonyTimeZoneFallbackSupported(), metricsState.getDeviceTimeZoneId(), metricsState.isEnhancedMetricsCollectionEnabled(), metricsState.getGeoDetectionRunInBackgroundEnabled()));
                    Binder.restoreCallingIdentity(token);
                    return 0;
                } catch (RuntimeException e) {
                    e = e;
                    Slog.e(TAG, "Getting time zone detection state failed: ", e);
                    Binder.restoreCallingIdentity(token);
                    return 1;
                }
            } catch (Throwable th) {
                e = th;
                Binder.restoreCallingIdentity(token);
                throw e;
            }
        } catch (RuntimeException e2) {
            e = e2;
        } catch (Throwable th2) {
            e = th2;
            Binder.restoreCallingIdentity(token);
            throw e;
        }
    }

    private static int convertToMetricsDetectionMode(int detectionMode) {
        switch (detectionMode) {
            case 1:
                return 1;
            case 2:
                return 3;
            case 3:
                return 2;
            default:
                return 0;
        }
    }

    private static byte[] convertTimeZoneSuggestionToProtoBytes(MetricsTimeZoneDetectorState.MetricsTimeZoneSuggestion suggestion) {
        int typeProtoValue;
        int[] zoneIdOrdinals;
        if (suggestion == null) {
            return null;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ProtoOutputStream protoOutputStream = new ProtoOutputStream(byteArrayOutputStream);
        if (suggestion.isCertain()) {
            typeProtoValue = 1;
        } else {
            typeProtoValue = 2;
        }
        protoOutputStream.write(1159641169921L, typeProtoValue);
        if (suggestion.isCertain()) {
            for (int zoneIdOrdinal : suggestion.getZoneIdOrdinals()) {
                protoOutputStream.write(2220498092034L, zoneIdOrdinal);
            }
            String[] zoneIds = suggestion.getZoneIds();
            if (zoneIds != null) {
                for (String zoneId : zoneIds) {
                    protoOutputStream.write(CompanionAppsPermissions.AppPermissions.PERMISSION, zoneId);
                }
            }
        }
        protoOutputStream.flush();
        IoUtils.closeQuietly(byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private void registerExternalStorageInfo() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.EXTERNAL_STORAGE_INFO, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullExternalStorageInfoLocked(int atomTag, List<StatsEvent> pulledData) {
        int externalStorageType;
        StorageManager storageManager = this.mStorageManager;
        if (storageManager == null) {
            return 1;
        }
        List<VolumeInfo> volumes = storageManager.getVolumes();
        for (VolumeInfo vol : volumes) {
            String envState = VolumeInfo.getEnvironmentForState(vol.getState());
            DiskInfo diskInfo = vol.getDisk();
            if (diskInfo != null && envState.equals("mounted")) {
                int volumeType = 3;
                if (vol.getType() == 0) {
                    volumeType = 1;
                } else if (vol.getType() == 1) {
                    volumeType = 2;
                }
                if (diskInfo.isSd()) {
                    externalStorageType = 1;
                } else if (diskInfo.isUsb()) {
                    externalStorageType = 2;
                } else {
                    externalStorageType = 3;
                }
                pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, externalStorageType, volumeType, diskInfo.size));
            }
        }
        return 0;
    }

    private void registerAppsOnExternalStorageInfo() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.APPS_ON_EXTERNAL_STORAGE_INFO, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullAppsOnExternalStorageInfoLocked(int atomTag, List<StatsEvent> pulledData) {
        VolumeInfo volumeInfo;
        DiskInfo diskInfo;
        if (this.mStorageManager == null) {
            return 1;
        }
        PackageManager pm = this.mContext.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        for (ApplicationInfo appInfo : apps) {
            UUID storageUuid = appInfo.storageUuid;
            if (storageUuid != null && (volumeInfo = this.mStorageManager.findVolumeByUuid(appInfo.storageUuid.toString())) != null && (diskInfo = volumeInfo.getDisk()) != null) {
                int externalStorageType = -1;
                if (diskInfo.isSd()) {
                    externalStorageType = 1;
                } else if (diskInfo.isUsb()) {
                    externalStorageType = 2;
                } else if (appInfo.isExternal()) {
                    externalStorageType = 3;
                }
                if (externalStorageType != -1) {
                    pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, externalStorageType, appInfo.packageName));
                }
            }
        }
        return 0;
    }

    private void registerFaceSettings() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.FACE_SETTINGS, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3694=4] */
    /* JADX DEBUG: Multi-variable search result rejected for r17v0, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r17v1, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r17v2, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r18v0, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r18v1, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r18v2, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r19v0, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r19v1, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r19v2, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r20v0, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r20v1, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r20v2, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r21v0, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r21v1, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r21v2, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r22v0, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r22v1, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r22v2, resolved type: boolean */
    /* JADX WARN: Multi-variable type inference failed */
    int pullFaceSettingsLocked(int atomTag, List<StatsEvent> pulledData) {
        long callingToken = Binder.clearCallingIdentity();
        try {
            UserManager manager = (UserManager) this.mContext.getSystemService(UserManager.class);
            int i = 1;
            if (manager == null) {
                Binder.restoreCallingIdentity(callingToken);
                return 1;
            }
            List<UserInfo> users = manager.getUsers();
            int numUsers = users.size();
            int userNum = 0;
            while (userNum < numUsers) {
                int userId = users.get(userNum).getUserHandle().getIdentifier();
                int unlockKeyguardEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "face_unlock_keyguard_enabled", i, userId);
                int unlockDismissesKeyguard = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "face_unlock_dismisses_keyguard", i, userId);
                int unlockAttentionRequired = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "face_unlock_attention_required", 0, userId);
                int unlockAppEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "face_unlock_app_enabled", i, userId);
                int unlockAlwaysRequireConfirmation = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "face_unlock_always_require_confirmation", 0, userId);
                int unlockDiversityRequired = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "face_unlock_diversity_required", i, userId);
                try {
                    pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, unlockKeyguardEnabled != 0 ? i : 0, unlockDismissesKeyguard != 0 ? i : 0, unlockAttentionRequired != 0 ? i : 0, unlockAppEnabled != 0 ? i : 0, unlockAlwaysRequireConfirmation != 0 ? i : 0, unlockDiversityRequired != 0 ? i : 0));
                    userNum++;
                    i = 1;
                } catch (Throwable th) {
                    th = th;
                    Binder.restoreCallingIdentity(callingToken);
                    throw th;
                }
            }
            Binder.restoreCallingIdentity(callingToken);
            return 0;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private void registerAppOps() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.APP_OPS, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerRuntimeAppOpAccessMessage() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.RUNTIME_APP_OP_ACCESS, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class AppOpEntry {
        public final String mAttributionTag;
        public final int mHash;
        public final AppOpsManager.HistoricalOp mOp;
        public final String mPackageName;
        public final int mUid;

        AppOpEntry(String packageName, String attributionTag, AppOpsManager.HistoricalOp op, int uid) {
            this.mPackageName = packageName;
            this.mAttributionTag = attributionTag;
            this.mUid = uid;
            this.mOp = op;
            this.mHash = ((packageName.hashCode() + StatsPullAtomService.RANDOM_SEED) & Integer.MAX_VALUE) % 100;
        }
    }

    int pullAppOpsLocked(int atomTag, List<StatsEvent> pulledData) {
        long token = Binder.clearCallingIdentity();
        try {
            AppOpsManager appOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
            CompletableFuture<AppOpsManager.HistoricalOps> ops = new CompletableFuture<>();
            AppOpsManager.HistoricalOpsRequest histOpsRequest = new AppOpsManager.HistoricalOpsRequest.Builder(0L, (long) JobStatus.NO_LATEST_RUNTIME).setFlags(9).build();
            Executor executor = AsyncTask.THREAD_POOL_EXECUTOR;
            Objects.requireNonNull(ops);
            appOps.getHistoricalOps(histOpsRequest, executor, new StatsPullAtomService$$ExternalSyntheticLambda6(ops));
            AppOpsManager.HistoricalOps histOps = ops.get(EXTERNAL_STATS_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            List<AppOpEntry> opsList = processHistoricalOps(histOps, atomTag, 100);
            int samplingRate = sampleAppOps(pulledData, opsList, atomTag, 100);
            if (samplingRate != 100) {
                Slog.e(TAG, "Atom 10060 downsampled - too many dimensions");
            }
            Binder.restoreCallingIdentity(token);
            return 0;
        } catch (Throwable t) {
            try {
                Slog.e(TAG, "Could not read appops", t);
                return 1;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    private int sampleAppOps(List<StatsEvent> pulledData, List<AppOpEntry> opsList, int atomTag, int samplingRate) {
        int i;
        int nOps;
        StatsEvent e;
        List<StatsEvent> list = pulledData;
        List<AppOpEntry> list2 = opsList;
        int i2 = atomTag;
        int i3 = samplingRate;
        int nOps2 = opsList.size();
        int i4 = 0;
        while (i4 < nOps2) {
            AppOpEntry entry = list2.get(i4);
            if (entry.mHash >= i3) {
                i = i4;
                nOps = nOps2;
            } else {
                if (i2 == 10075) {
                    i = i4;
                    nOps = nOps2;
                    e = FrameworkStatsLog.buildStatsEvent(atomTag, entry.mUid, entry.mPackageName, entry.mAttributionTag, entry.mOp.getOpCode(), entry.mOp.getForegroundAccessCount(9), entry.mOp.getBackgroundAccessCount(9), entry.mOp.getForegroundRejectCount(9), entry.mOp.getBackgroundRejectCount(9), entry.mOp.getForegroundAccessDuration(9), entry.mOp.getBackgroundAccessDuration(9), this.mDangerousAppOpsList.contains(Integer.valueOf(entry.mOp.getOpCode())), samplingRate);
                } else {
                    i = i4;
                    nOps = nOps2;
                    e = FrameworkStatsLog.buildStatsEvent(atomTag, entry.mUid, entry.mPackageName, entry.mOp.getOpCode(), entry.mOp.getForegroundAccessCount(9), entry.mOp.getBackgroundAccessCount(9), entry.mOp.getForegroundRejectCount(9), entry.mOp.getBackgroundRejectCount(9), entry.mOp.getForegroundAccessDuration(9), entry.mOp.getBackgroundAccessDuration(9), this.mDangerousAppOpsList.contains(Integer.valueOf(entry.mOp.getOpCode())));
                }
                list = pulledData;
                list.add(e);
            }
            i4 = i + 1;
            list2 = opsList;
            i2 = atomTag;
            i3 = samplingRate;
            nOps2 = nOps;
        }
        if (pulledData.size() > 800) {
            int adjustedSamplingRate = MathUtils.constrain((samplingRate * 500) / pulledData.size(), 0, samplingRate - 1);
            pulledData.clear();
            return sampleAppOps(list, opsList, atomTag, adjustedSamplingRate);
        }
        return samplingRate;
    }

    private void registerAttributedAppOps() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.ATTRIBUTED_APP_OPS, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullAttributedAppOpsLocked(int atomTag, List<StatsEvent> pulledData) {
        long token = Binder.clearCallingIdentity();
        try {
            AppOpsManager appOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
            CompletableFuture<AppOpsManager.HistoricalOps> ops = new CompletableFuture<>();
            AppOpsManager.HistoricalOpsRequest histOpsRequest = new AppOpsManager.HistoricalOpsRequest.Builder(0L, (long) JobStatus.NO_LATEST_RUNTIME).setFlags(9).build();
            Executor executor = AsyncTask.THREAD_POOL_EXECUTOR;
            Objects.requireNonNull(ops);
            appOps.getHistoricalOps(histOpsRequest, executor, new StatsPullAtomService$$ExternalSyntheticLambda6(ops));
            AppOpsManager.HistoricalOps histOps = ops.get(EXTERNAL_STATS_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            if (this.mAppOpsSamplingRate == 0) {
                this.mContext.getMainThreadHandler().postDelayed(new Runnable() { // from class: com.android.server.stats.pull.StatsPullAtomService.4
                    @Override // java.lang.Runnable
                    public void run() {
                        try {
                            StatsPullAtomService.this.estimateAppOpsSamplingRate();
                        } finally {
                        }
                    }
                }, APP_OPS_SAMPLING_INITIALIZATION_DELAY_MILLIS);
                this.mAppOpsSamplingRate = 100;
            }
            List<AppOpEntry> opsList = processHistoricalOps(histOps, atomTag, this.mAppOpsSamplingRate);
            int newSamplingRate = sampleAppOps(pulledData, opsList, atomTag, this.mAppOpsSamplingRate);
            this.mAppOpsSamplingRate = Math.min(this.mAppOpsSamplingRate, newSamplingRate);
            Binder.restoreCallingIdentity(token);
            return 0;
        } catch (Throwable t) {
            try {
                Slog.e(TAG, "Could not read appops", t);
                return 1;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void estimateAppOpsSamplingRate() throws Exception {
        int appOpsTargetCollectionSize = DeviceConfig.getInt("permissions", APP_OPS_TARGET_COLLECTION_SIZE, 2000);
        AppOpsManager appOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        CompletableFuture<AppOpsManager.HistoricalOps> ops = new CompletableFuture<>();
        AppOpsManager.HistoricalOpsRequest histOpsRequest = new AppOpsManager.HistoricalOpsRequest.Builder(Math.max(Instant.now().minus(1L, (TemporalUnit) ChronoUnit.DAYS).toEpochMilli(), 0L), (long) JobStatus.NO_LATEST_RUNTIME).setFlags(9).build();
        Executor executor = AsyncTask.THREAD_POOL_EXECUTOR;
        Objects.requireNonNull(ops);
        appOps.getHistoricalOps(histOpsRequest, executor, new StatsPullAtomService$$ExternalSyntheticLambda6(ops));
        AppOpsManager.HistoricalOps histOps = ops.get(EXTERNAL_STATS_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        List<AppOpEntry> opsList = processHistoricalOps(histOps, FrameworkStatsLog.ATTRIBUTED_APP_OPS, 100);
        long estimatedSize = 0;
        int nOps = opsList.size();
        for (int i = 0; i < nOps; i++) {
            AppOpEntry entry = opsList.get(i);
            estimatedSize += entry.mPackageName.length() + 32 + (entry.mAttributionTag == null ? 1 : entry.mAttributionTag.length());
        }
        int i2 = appOpsTargetCollectionSize * 100;
        int estimatedSamplingRate = (int) MathUtils.constrain(i2 / estimatedSize, 0L, 100L);
        synchronized (this.mAttributedAppOpsLock) {
            this.mAppOpsSamplingRate = Math.min(this.mAppOpsSamplingRate, estimatedSamplingRate);
        }
    }

    private List<AppOpEntry> processHistoricalOps(AppOpsManager.HistoricalOps histOps, int atomTag, int samplingRatio) {
        List<AppOpEntry> opsList = new ArrayList<>();
        for (int uidIdx = 0; uidIdx < histOps.getUidCount(); uidIdx++) {
            AppOpsManager.HistoricalUidOps uidOps = histOps.getUidOpsAt(uidIdx);
            int uid = uidOps.getUid();
            for (int pkgIdx = 0; pkgIdx < uidOps.getPackageCount(); pkgIdx++) {
                AppOpsManager.HistoricalPackageOps packageOps = uidOps.getPackageOpsAt(pkgIdx);
                if (atomTag != 10075) {
                    if (atomTag == 10060) {
                        for (int opIdx = 0; opIdx < packageOps.getOpCount(); opIdx++) {
                            AppOpsManager.HistoricalOp op = packageOps.getOpAt(opIdx);
                            processHistoricalOp(op, opsList, uid, samplingRatio, packageOps.getPackageName(), null);
                        }
                    }
                } else {
                    for (int attributionIdx = 0; attributionIdx < packageOps.getAttributedOpsCount(); attributionIdx++) {
                        int opIdx2 = 0;
                        for (AppOpsManager.AttributedHistoricalOps attributedOps = packageOps.getAttributedOpsAt(attributionIdx); opIdx2 < attributedOps.getOpCount(); attributedOps = attributedOps) {
                            AppOpsManager.HistoricalOp op2 = attributedOps.getOpAt(opIdx2);
                            processHistoricalOp(op2, opsList, uid, samplingRatio, packageOps.getPackageName(), attributedOps.getTag());
                            opIdx2++;
                        }
                    }
                }
            }
        }
        return opsList;
    }

    private void processHistoricalOp(AppOpsManager.HistoricalOp op, List<AppOpEntry> opsList, int uid, int samplingRatio, String packageName, String attributionTag) {
        int firstChar = 0;
        if (attributionTag != null && attributionTag.startsWith(packageName) && (firstChar = packageName.length()) < attributionTag.length() && attributionTag.charAt(firstChar) == '.') {
            firstChar++;
        }
        AppOpEntry entry = new AppOpEntry(packageName, attributionTag == null ? null : attributionTag.substring(firstChar), op, uid);
        if (entry.mHash < samplingRatio) {
            opsList.add(entry);
        }
    }

    int pullRuntimeAppOpAccessMessageLocked(int atomTag, List<StatsEvent> pulledData) {
        long token = Binder.clearCallingIdentity();
        try {
            AppOpsManager appOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
            RuntimeAppOpAccessMessage message = appOps.collectRuntimeAppOpAccessMessage();
            if (message == null) {
                Slog.i(TAG, "No runtime appop access message collected");
                return 0;
            }
            try {
                pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, message.getUid(), message.getPackageName(), "", message.getAttributionTag() == null ? "" : message.getAttributionTag(), message.getMessage(), message.getSamplingStrategy(), AppOpsManager.strOpToOp(message.getOp())));
                return 0;
            } catch (Throwable th) {
                t = th;
                try {
                    Slog.e(TAG, "Could not read runtime appop access message", t);
                    return 1;
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        } catch (Throwable th2) {
            t = th2;
        }
    }

    static void unpackStreamedData(int atomTag, List<StatsEvent> pulledData, List<ParcelFileDescriptor> statsFiles) throws IOException {
        InputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(statsFiles.get(0));
        int[] len = new int[1];
        byte[] stats = readFully(stream, len);
        pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, Arrays.copyOf(stats, len[0])));
    }

    static byte[] readFully(InputStream stream, int[] outLen) throws IOException {
        int pos = 0;
        int initialAvail = stream.available();
        byte[] data = new byte[initialAvail > 0 ? initialAvail + 1 : 16384];
        while (true) {
            int amt = stream.read(data, pos, data.length - pos);
            Slog.i(TAG, "Read " + amt + " bytes at " + pos + " of avail " + data.length);
            if (amt < 0) {
                Slog.i(TAG, "**** FINISHED READING: pos=" + pos + " len=" + data.length);
                outLen[0] = pos;
                return data;
            }
            pos += amt;
            if (pos >= data.length) {
                byte[] newData = new byte[pos + 16384];
                Slog.i(TAG, "Copying " + pos + " bytes to new array len " + newData.length);
                System.arraycopy(data, 0, newData, 0, pos);
                data = newData;
            }
        }
    }

    private void registerNotificationRemoteViews() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.NOTIFICATION_REMOTE_VIEWS, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4043=7] */
    int pullNotificationRemoteViewsLocked(int atomTag, List<StatsEvent> pulledData) {
        INotificationManager notificationManagerService = getINotificationManagerService();
        if (notificationManagerService == null) {
            return 1;
        }
        long callingToken = Binder.clearCallingIdentity();
        try {
            try {
                long wallClockNanos = SystemClock.currentTimeMicro() * 1000;
                long lastNotificationStatsNs = wallClockNanos - TimeUnit.NANOSECONDS.convert(1L, TimeUnit.DAYS);
                List<ParcelFileDescriptor> statsFiles = new ArrayList<>();
                notificationManagerService.pullStats(lastNotificationStatsNs, 1, true, statsFiles);
                if (statsFiles.size() != 1) {
                    Binder.restoreCallingIdentity(callingToken);
                    return 1;
                }
                try {
                    unpackStreamedData(atomTag, pulledData, statsFiles);
                    Binder.restoreCallingIdentity(callingToken);
                    return 0;
                } catch (RemoteException e) {
                    e = e;
                    Slog.e(TAG, "Getting notistats failed: ", e);
                    Binder.restoreCallingIdentity(callingToken);
                    return 1;
                } catch (IOException e2) {
                    e = e2;
                    Slog.e(TAG, "Getting notistats failed: ", e);
                    Binder.restoreCallingIdentity(callingToken);
                    return 1;
                } catch (SecurityException e3) {
                    e = e3;
                    Slog.e(TAG, "Getting notistats failed: ", e);
                    Binder.restoreCallingIdentity(callingToken);
                    return 1;
                }
            } catch (Throwable th) {
                e = th;
                Binder.restoreCallingIdentity(callingToken);
                throw e;
            }
        } catch (RemoteException e4) {
            e = e4;
        } catch (IOException e5) {
            e = e5;
        } catch (SecurityException e6) {
            e = e6;
        } catch (Throwable th2) {
            e = th2;
            Binder.restoreCallingIdentity(callingToken);
            throw e;
        }
    }

    private void registerDangerousPermissionStateSampled() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.DANGEROUS_PERMISSION_STATE_SAMPLED, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerBatteryLevel() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.BATTERY_LEVEL, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerRemainingBatteryCapacity() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.REMAINING_BATTERY_CAPACITY, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerFullBatteryCapacity() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.FULL_BATTERY_CAPACITY, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerBatteryVoltage() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.BATTERY_VOLTAGE, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerBatteryCycleCount() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.BATTERY_CYCLE_COUNT, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullHealthHalLocked(int atomTag, List<StatsEvent> pulledData) {
        int pulledValue;
        HealthServiceWrapper healthServiceWrapper = this.mHealthService;
        if (healthServiceWrapper == null) {
            return 1;
        }
        try {
            HealthInfo healthInfo = healthServiceWrapper.getHealthInfo();
            if (healthInfo == null) {
                return 1;
            }
            switch (atomTag) {
                case FrameworkStatsLog.REMAINING_BATTERY_CAPACITY /* 10019 */:
                    pulledValue = healthInfo.batteryChargeCounterUah;
                    break;
                case FrameworkStatsLog.FULL_BATTERY_CAPACITY /* 10020 */:
                    pulledValue = healthInfo.batteryFullChargeUah;
                    break;
                case FrameworkStatsLog.BATTERY_VOLTAGE /* 10030 */:
                    pulledValue = healthInfo.batteryVoltageMillivolts;
                    break;
                case FrameworkStatsLog.BATTERY_LEVEL /* 10043 */:
                    pulledValue = healthInfo.batteryLevel;
                    break;
                case FrameworkStatsLog.BATTERY_CYCLE_COUNT /* 10045 */:
                    pulledValue = healthInfo.batteryCycleCount;
                    break;
                default:
                    return 1;
            }
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, pulledValue));
            return 0;
        } catch (RemoteException | IllegalStateException e) {
            return 1;
        }
    }

    private void registerSettingsStats() {
        this.mStatsManager.setPullAtomCallback(10080, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4178=4] */
    int pullSettingsStatsLocked(int atomTag, List<StatsEvent> pulledData) {
        UserManager userManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        if (userManager == null) {
            return 1;
        }
        long token = Binder.clearCallingIdentity();
        try {
            for (UserInfo user : userManager.getUsers()) {
                int userId = user.getUserHandle().getIdentifier();
                if (userId == 0) {
                    pulledData.addAll(SettingsStatsUtil.logGlobalSettings(this.mContext, atomTag, 0));
                }
                pulledData.addAll(SettingsStatsUtil.logSystemSettings(this.mContext, atomTag, userId));
                pulledData.addAll(SettingsStatsUtil.logSecureSettings(this.mContext, atomTag, userId));
            }
            return 0;
        } catch (Exception e) {
            Slog.e(TAG, "failed to pullSettingsStats", e);
            return 1;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void registerInstalledIncrementalPackages() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.INSTALLED_INCREMENTAL_PACKAGE, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4216=4] */
    int pullInstalledIncrementalPackagesLocked(int atomTag, List<StatsEvent> pulledData) {
        PackageManager pm = this.mContext.getPackageManager();
        if (pm.hasSystemFeature("android.software.incremental_delivery")) {
            long token = Binder.clearCallingIdentity();
            try {
                try {
                    int[] userIds = ((UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).getUserIds();
                    for (int userId : userIds) {
                        List<PackageInfo> installedPackages = pm.getInstalledPackagesAsUser(0, userId);
                        for (PackageInfo pi : installedPackages) {
                            if (IncrementalManager.isIncrementalPath(pi.applicationInfo.getBaseCodePath())) {
                                pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, pi.applicationInfo.uid));
                            }
                        }
                    }
                    return 0;
                } catch (Exception e) {
                    Slog.e(TAG, "failed to pullInstalledIncrementalPackagesLocked", e);
                    Binder.restoreCallingIdentity(token);
                    return 1;
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        return 0;
    }

    private void registerKeystoreStorageStats() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.KEYSTORE2_STORAGE_STATS, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerRkpPoolStats() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.RKP_POOL_STATS, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerKeystoreKeyCreationWithGeneralInfo() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.KEYSTORE2_KEY_CREATION_WITH_GENERAL_INFO, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerKeystoreKeyCreationWithAuthInfo() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.KEYSTORE2_KEY_CREATION_WITH_AUTH_INFO, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerKeystoreKeyCreationWithPurposeModesInfo() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.KEYSTORE2_KEY_CREATION_WITH_PURPOSE_AND_MODES_INFO, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerKeystoreAtomWithOverflow() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.KEYSTORE2_ATOM_WITH_OVERFLOW, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerKeystoreKeyOperationWithPurposeAndModesInfo() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.KEYSTORE2_KEY_OPERATION_WITH_PURPOSE_AND_MODES_INFO, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerKeystoreKeyOperationWithGeneralInfo() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.KEYSTORE2_KEY_OPERATION_WITH_GENERAL_INFO, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerRkpErrorStats() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.RKP_ERROR_STATS, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerKeystoreCrashStats() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.KEYSTORE2_CRASH_STATS, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerAccessibilityShortcutStats() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.ACCESSIBILITY_SHORTCUT_STATS, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerAccessibilityFloatingMenuStats() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.ACCESSIBILITY_FLOATING_MENU_STATS, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    private void registerMediaCapabilitiesStats() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.MEDIA_CAPABILITIES, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int parseKeystoreStorageStats(KeystoreAtom[] atoms, List<StatsEvent> pulledData) {
        for (KeystoreAtom atomWrapper : atoms) {
            if (atomWrapper.payload.getTag() != 0) {
                return 1;
            }
            StorageStats atom = atomWrapper.payload.getStorageStats();
            pulledData.add(FrameworkStatsLog.buildStatsEvent((int) FrameworkStatsLog.KEYSTORE2_STORAGE_STATS, atom.storage_type, atom.size, atom.unused_size));
        }
        return 0;
    }

    int parseRkpPoolStats(KeystoreAtom[] atoms, List<StatsEvent> pulledData) {
        for (KeystoreAtom atomWrapper : atoms) {
            if (atomWrapper.payload.getTag() != 1) {
                return 1;
            }
            RkpPoolStats atom = atomWrapper.payload.getRkpPoolStats();
            pulledData.add(FrameworkStatsLog.buildStatsEvent((int) FrameworkStatsLog.RKP_POOL_STATS, atom.security_level, atom.expiring, atom.unassigned, atom.attested, atom.total));
        }
        return 0;
    }

    int parseKeystoreKeyCreationWithGeneralInfo(KeystoreAtom[] atoms, List<StatsEvent> pulledData) {
        for (KeystoreAtom atomWrapper : atoms) {
            if (atomWrapper.payload.getTag() != 2) {
                return 1;
            }
            KeyCreationWithGeneralInfo atom = atomWrapper.payload.getKeyCreationWithGeneralInfo();
            pulledData.add(FrameworkStatsLog.buildStatsEvent((int) FrameworkStatsLog.KEYSTORE2_KEY_CREATION_WITH_GENERAL_INFO, atom.algorithm, atom.key_size, atom.ec_curve, atom.key_origin, atom.error_code, atom.attestation_requested, atomWrapper.count));
        }
        return 0;
    }

    int parseKeystoreKeyCreationWithAuthInfo(KeystoreAtom[] atoms, List<StatsEvent> pulledData) {
        for (KeystoreAtom atomWrapper : atoms) {
            if (atomWrapper.payload.getTag() != 3) {
                return 1;
            }
            KeyCreationWithAuthInfo atom = atomWrapper.payload.getKeyCreationWithAuthInfo();
            pulledData.add(FrameworkStatsLog.buildStatsEvent((int) FrameworkStatsLog.KEYSTORE2_KEY_CREATION_WITH_AUTH_INFO, atom.user_auth_type, atom.log10_auth_key_timeout_seconds, atom.security_level, atomWrapper.count));
        }
        return 0;
    }

    int parseKeystoreKeyCreationWithPurposeModesInfo(KeystoreAtom[] atoms, List<StatsEvent> pulledData) {
        for (KeystoreAtom atomWrapper : atoms) {
            if (atomWrapper.payload.getTag() != 4) {
                return 1;
            }
            KeyCreationWithPurposeAndModesInfo atom = atomWrapper.payload.getKeyCreationWithPurposeAndModesInfo();
            pulledData.add(FrameworkStatsLog.buildStatsEvent((int) FrameworkStatsLog.KEYSTORE2_KEY_CREATION_WITH_PURPOSE_AND_MODES_INFO, atom.algorithm, atom.purpose_bitmap, atom.padding_mode_bitmap, atom.digest_bitmap, atom.block_mode_bitmap, atomWrapper.count));
        }
        return 0;
    }

    int parseKeystoreAtomWithOverflow(KeystoreAtom[] atoms, List<StatsEvent> pulledData) {
        for (KeystoreAtom atomWrapper : atoms) {
            if (atomWrapper.payload.getTag() != 5) {
                return 1;
            }
            Keystore2AtomWithOverflow atom = atomWrapper.payload.getKeystore2AtomWithOverflow();
            pulledData.add(FrameworkStatsLog.buildStatsEvent((int) FrameworkStatsLog.KEYSTORE2_ATOM_WITH_OVERFLOW, atom.atom_id, atomWrapper.count));
        }
        return 0;
    }

    int parseKeystoreKeyOperationWithPurposeModesInfo(KeystoreAtom[] atoms, List<StatsEvent> pulledData) {
        for (KeystoreAtom atomWrapper : atoms) {
            if (atomWrapper.payload.getTag() != 6) {
                return 1;
            }
            KeyOperationWithPurposeAndModesInfo atom = atomWrapper.payload.getKeyOperationWithPurposeAndModesInfo();
            pulledData.add(FrameworkStatsLog.buildStatsEvent((int) FrameworkStatsLog.KEYSTORE2_KEY_OPERATION_WITH_PURPOSE_AND_MODES_INFO, atom.purpose, atom.padding_mode_bitmap, atom.digest_bitmap, atom.block_mode_bitmap, atomWrapper.count));
        }
        return 0;
    }

    int parseKeystoreKeyOperationWithGeneralInfo(KeystoreAtom[] atoms, List<StatsEvent> pulledData) {
        for (KeystoreAtom atomWrapper : atoms) {
            if (atomWrapper.payload.getTag() != 7) {
                return 1;
            }
            KeyOperationWithGeneralInfo atom = atomWrapper.payload.getKeyOperationWithGeneralInfo();
            pulledData.add(FrameworkStatsLog.buildStatsEvent((int) FrameworkStatsLog.KEYSTORE2_KEY_OPERATION_WITH_GENERAL_INFO, atom.outcome, atom.error_code, atom.key_upgraded, atom.security_level, atomWrapper.count));
        }
        return 0;
    }

    int parseRkpErrorStats(KeystoreAtom[] atoms, List<StatsEvent> pulledData) {
        for (KeystoreAtom atomWrapper : atoms) {
            if (atomWrapper.payload.getTag() != 8) {
                return 1;
            }
            RkpErrorStats atom = atomWrapper.payload.getRkpErrorStats();
            pulledData.add(FrameworkStatsLog.buildStatsEvent((int) FrameworkStatsLog.RKP_ERROR_STATS, atom.rkpError, atomWrapper.count, atom.security_level));
        }
        return 0;
    }

    int parseKeystoreCrashStats(KeystoreAtom[] atoms, List<StatsEvent> pulledData) {
        for (KeystoreAtom atomWrapper : atoms) {
            if (atomWrapper.payload.getTag() != 9) {
                return 1;
            }
            CrashStats atom = atomWrapper.payload.getCrashStats();
            pulledData.add(FrameworkStatsLog.buildStatsEvent((int) FrameworkStatsLog.KEYSTORE2_CRASH_STATS, atom.count_of_crash_events));
        }
        return 0;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4519=15] */
    int pullKeystoreAtoms(int atomTag, List<StatsEvent> pulledData) {
        IKeystoreMetrics keystoreMetricsService = getIKeystoreMetricsService();
        if (keystoreMetricsService == null) {
            Slog.w(TAG, "Keystore service is null");
            return 1;
        }
        long callingToken = Binder.clearCallingIdentity();
        try {
            KeystoreAtom[] atoms = keystoreMetricsService.pullMetrics(atomTag);
            switch (atomTag) {
                case FrameworkStatsLog.KEYSTORE2_STORAGE_STATS /* 10103 */:
                    return parseKeystoreStorageStats(atoms, pulledData);
                case FrameworkStatsLog.RKP_POOL_STATS /* 10104 */:
                    return parseRkpPoolStats(atoms, pulledData);
                case FrameworkStatsLog.KEYSTORE2_KEY_CREATION_WITH_GENERAL_INFO /* 10118 */:
                    return parseKeystoreKeyCreationWithGeneralInfo(atoms, pulledData);
                case FrameworkStatsLog.KEYSTORE2_KEY_CREATION_WITH_AUTH_INFO /* 10119 */:
                    return parseKeystoreKeyCreationWithAuthInfo(atoms, pulledData);
                case FrameworkStatsLog.KEYSTORE2_KEY_CREATION_WITH_PURPOSE_AND_MODES_INFO /* 10120 */:
                    return parseKeystoreKeyCreationWithPurposeModesInfo(atoms, pulledData);
                case FrameworkStatsLog.KEYSTORE2_ATOM_WITH_OVERFLOW /* 10121 */:
                    return parseKeystoreAtomWithOverflow(atoms, pulledData);
                case FrameworkStatsLog.KEYSTORE2_KEY_OPERATION_WITH_PURPOSE_AND_MODES_INFO /* 10122 */:
                    return parseKeystoreKeyOperationWithPurposeModesInfo(atoms, pulledData);
                case FrameworkStatsLog.KEYSTORE2_KEY_OPERATION_WITH_GENERAL_INFO /* 10123 */:
                    return parseKeystoreKeyOperationWithGeneralInfo(atoms, pulledData);
                case FrameworkStatsLog.RKP_ERROR_STATS /* 10124 */:
                    return parseRkpErrorStats(atoms, pulledData);
                case FrameworkStatsLog.KEYSTORE2_CRASH_STATS /* 10125 */:
                    return parseKeystoreCrashStats(atoms, pulledData);
                default:
                    Slog.w(TAG, "Unsupported keystore atom: " + atomTag);
                    return 1;
            }
        } catch (ServiceSpecificException e) {
            Slog.e(TAG, "pulling keystore metrics failed", e);
            return 1;
        } catch (RemoteException e2) {
            Slog.e(TAG, "Disconnected from keystore service. Cannot pull.", e2);
            return 1;
        } finally {
            Binder.restoreCallingIdentity(callingToken);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4567=4] */
    int pullAccessibilityShortcutStatsLocked(int atomTag, List<StatsEvent> pulledData) {
        UserManager userManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        if (userManager == null) {
            return 1;
        }
        long token = Binder.clearCallingIdentity();
        try {
            ContentResolver resolver = this.mContext.getContentResolver();
            for (UserInfo userInfo : userManager.getUsers()) {
                int userId = userInfo.getUserHandle().getIdentifier();
                if (isAccessibilityShortcutUser(this.mContext, userId)) {
                    int software_shortcut_type = convertToAccessibilityShortcutType(Settings.Secure.getIntForUser(resolver, "accessibility_button_mode", 0, userId));
                    String software_shortcut_list = Settings.Secure.getStringForUser(resolver, "accessibility_button_targets", userId);
                    int software_shortcut_service_num = countAccessibilityServices(software_shortcut_list);
                    String hardware_shortcut_list = Settings.Secure.getStringForUser(resolver, "accessibility_shortcut_target_service", userId);
                    int hardware_shortcut_service_num = countAccessibilityServices(hardware_shortcut_list);
                    int triple_tap_service_num = Settings.Secure.getIntForUser(resolver, "accessibility_display_magnification_enabled", 0, userId);
                    try {
                        try {
                            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, software_shortcut_type, software_shortcut_service_num, 2, hardware_shortcut_service_num, 3, triple_tap_service_num));
                        } catch (RuntimeException e) {
                            e = e;
                            Slog.e(TAG, "pulling accessibility shortcuts stats failed at getUsers", e);
                            Binder.restoreCallingIdentity(token);
                            return 1;
                        }
                    } catch (Throwable th) {
                        e = th;
                        Binder.restoreCallingIdentity(token);
                        throw e;
                    }
                }
            }
            Binder.restoreCallingIdentity(token);
            return 0;
        } catch (RuntimeException e2) {
            e = e2;
        } catch (Throwable th2) {
            e = th2;
            Binder.restoreCallingIdentity(token);
            throw e;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4610=5] */
    /* JADX DEBUG: Multi-variable search result rejected for r12v3, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r12v4, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r12v5, resolved type: boolean */
    /* JADX WARN: Multi-variable type inference failed */
    int pullAccessibilityFloatingMenuStatsLocked(int atomTag, List<StatsEvent> pulledData) {
        ContentResolver resolver;
        StatsPullAtomService statsPullAtomService = this;
        UserManager userManager = (UserManager) statsPullAtomService.mContext.getSystemService(UserManager.class);
        int i = 1;
        if (userManager == null) {
            return 1;
        }
        long token = Binder.clearCallingIdentity();
        try {
            ContentResolver resolver2 = statsPullAtomService.mContext.getContentResolver();
            for (UserInfo userInfo : userManager.getUsers()) {
                int userId = userInfo.getUserHandle().getIdentifier();
                if (statsPullAtomService.isAccessibilityFloatingMenuUser(statsPullAtomService.mContext, userId)) {
                    int size = Settings.Secure.getIntForUser(resolver2, "accessibility_floating_menu_size", 0, userId);
                    int type = Settings.Secure.getIntForUser(resolver2, "accessibility_floating_menu_icon_type", 0, userId);
                    boolean fadeEnabled = Settings.Secure.getIntForUser(resolver2, "accessibility_floating_menu_fade_enabled", i, userId) == i ? i : 0;
                    float opacity = Settings.Secure.getFloatForUser(resolver2, "accessibility_floating_menu_opacity", 0.55f, userId);
                    resolver = resolver2;
                    try {
                        try {
                            try {
                                pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, size, type, fadeEnabled, opacity));
                            } catch (RuntimeException e) {
                                e = e;
                                Slog.e(TAG, "pulling accessibility floating menu stats failed at getUsers", e);
                                Binder.restoreCallingIdentity(token);
                                return 1;
                            }
                        } catch (Throwable th) {
                            e = th;
                            Binder.restoreCallingIdentity(token);
                            throw e;
                        }
                    } catch (RuntimeException e2) {
                        e = e2;
                        Slog.e(TAG, "pulling accessibility floating menu stats failed at getUsers", e);
                        Binder.restoreCallingIdentity(token);
                        return 1;
                    } catch (Throwable th2) {
                        e = th2;
                        Binder.restoreCallingIdentity(token);
                        throw e;
                    }
                } else {
                    resolver = resolver2;
                }
                i = 1;
                statsPullAtomService = this;
                resolver2 = resolver;
            }
            Binder.restoreCallingIdentity(token);
            return 0;
        } catch (RuntimeException e3) {
            e = e3;
        } catch (Throwable th3) {
            e = th3;
        }
    }

    int pullMediaCapabilitiesStats(int atomTag, List<StatsEvent> pulledData) {
        AudioManager audioManager;
        byte[] sinkHdrFormats;
        int hdcpLevel;
        int userPreferredWidth;
        boolean hasUserDisabledAllm;
        if (this.mContext.getPackageManager().hasSystemFeature("android.software.leanback") && (audioManager = (AudioManager) this.mContext.getSystemService(AudioManager.class)) != null) {
            Map<Integer, Boolean> surroundEncodingsMap = audioManager.getSurroundFormats();
            byte[] surroundEncodings = toBytes(new ArrayList(surroundEncodingsMap.keySet()));
            byte[] sinkSurroundEncodings = toBytes(audioManager.getReportedSurroundFormats());
            List<Integer> disabledSurroundEncodingsList = new ArrayList<>();
            List<Integer> enabledSurroundEncodingsList = new ArrayList<>();
            for (Integer num : surroundEncodingsMap.keySet()) {
                int surroundEncoding = num.intValue();
                if (!surroundEncodingsMap.get(Integer.valueOf(surroundEncoding)).booleanValue()) {
                    disabledSurroundEncodingsList.add(Integer.valueOf(surroundEncoding));
                } else {
                    enabledSurroundEncodingsList.add(Integer.valueOf(surroundEncoding));
                }
            }
            byte[] disabledSurroundEncodings = toBytes(disabledSurroundEncodingsList);
            byte[] enabledSurroundEncodings = toBytes(enabledSurroundEncodingsList);
            int surroundOutputMode = audioManager.getEncodedSurroundMode();
            DisplayManager displayManager = (DisplayManager) this.mContext.getSystemService(DisplayManager.class);
            Display display = displayManager.getDisplay(0);
            Display.HdrCapabilities hdrCapabilities = display.getHdrCapabilities();
            byte[] sinkHdrFormats2 = new byte[0];
            if (hdrCapabilities != null) {
                byte[] sinkHdrFormats3 = toBytes(hdrCapabilities.getSupportedHdrTypes());
                sinkHdrFormats = sinkHdrFormats3;
            } else {
                sinkHdrFormats = sinkHdrFormats2;
            }
            byte[] sinkDisplayModes = toBytes(display.getSupportedModes());
            int hdcpLevel2 = -1;
            List<UUID> uuids = MediaDrm.getSupportedCryptoSchemes();
            try {
                if (!uuids.isEmpty()) {
                    MediaDrm mediaDrm = new MediaDrm(uuids.get(0));
                    hdcpLevel2 = mediaDrm.getConnectedHdcpLevel();
                }
                hdcpLevel = hdcpLevel2;
            } catch (UnsupportedSchemeException exception) {
                Slog.e(TAG, "pulling hdcp level failed.", exception);
                hdcpLevel = -1;
            }
            int matchContentFrameRateUserPreference = displayManager.getMatchContentFrameRateUserPreference();
            byte[] userDisabledHdrTypes = toBytes(displayManager.getUserDisabledHdrTypes());
            Display.Mode userPreferredDisplayMode = displayManager.getGlobalUserPreferredDisplayMode();
            int i = -1;
            if (userPreferredDisplayMode == null) {
                userPreferredWidth = -1;
            } else {
                userPreferredWidth = userPreferredDisplayMode.getPhysicalWidth();
            }
            if (userPreferredDisplayMode != null) {
                i = userPreferredDisplayMode.getPhysicalHeight();
            }
            int userPreferredHeight = i;
            float userPreferredRefreshRate = userPreferredDisplayMode != null ? userPreferredDisplayMode.getRefreshRate() : 0.0f;
            try {
                hasUserDisabledAllm = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "minimal_post_processing_allowed", 1) == 0;
            } catch (Settings.SettingNotFoundException exception2) {
                Slog.e(TAG, "unable to find setting for MINIMAL_POST_PROCESSING_ALLOWED.", exception2);
                hasUserDisabledAllm = false;
            }
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, surroundEncodings, sinkSurroundEncodings, disabledSurroundEncodings, enabledSurroundEncodings, surroundOutputMode, sinkHdrFormats, sinkDisplayModes, hdcpLevel, matchContentFrameRateUserPreference, userDisabledHdrTypes, userPreferredWidth, userPreferredHeight, userPreferredRefreshRate, hasUserDisabledAllm));
            return 0;
        }
        return 1;
    }

    private void registerPendingIntentsPerPackagePuller() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.PENDING_INTENTS_PER_PACKAGE, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int pullPendingIntentsPerPackage(int atomTag, List<StatsEvent> pulledData) {
        List<PendingIntentStats> pendingIntentStats = ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).getPendingIntentStats();
        for (PendingIntentStats stats : pendingIntentStats) {
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, stats.uid, stats.count, stats.sizeKb));
        }
        return 0;
    }

    private void registerPinnerServiceStats() {
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.PINNED_FILE_SIZES_PER_PACKAGE, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this.mStatsCallbackImpl);
    }

    int pullSystemServerPinnerStats(int atomTag, List<StatsEvent> pulledData) {
        PinnerService pinnerService = (PinnerService) LocalServices.getService(PinnerService.class);
        List<PinnerService.PinnedFileStats> pinnedFileStats = pinnerService.dumpDataForStatsd();
        for (PinnerService.PinnedFileStats pfstats : pinnedFileStats) {
            pulledData.add(FrameworkStatsLog.buildStatsEvent(atomTag, pfstats.uid, pfstats.filename, pfstats.sizeKb));
        }
        return 0;
    }

    private byte[] toBytes(List<Integer> audioEncodings) {
        ProtoOutputStream protoOutputStream = new ProtoOutputStream();
        for (Integer num : audioEncodings) {
            int audioEncoding = num.intValue();
            protoOutputStream.write(2259152797697L, audioEncoding);
        }
        return protoOutputStream.getBytes();
    }

    private byte[] toBytes(int[] array) {
        ProtoOutputStream protoOutputStream = new ProtoOutputStream();
        for (int element : array) {
            protoOutputStream.write(2259152797697L, element);
        }
        return protoOutputStream.getBytes();
    }

    private byte[] toBytes(Display.Mode[] displayModes) {
        Map<Integer, Integer> modeGroupIds = createModeGroups(displayModes);
        ProtoOutputStream protoOutputStream = new ProtoOutputStream();
        for (Display.Mode element : displayModes) {
            ProtoOutputStream protoOutputStreamMode = new ProtoOutputStream();
            protoOutputStreamMode.write(CompanionMessage.MESSAGE_ID, element.getPhysicalHeight());
            protoOutputStreamMode.write(1120986464258L, element.getPhysicalWidth());
            protoOutputStreamMode.write(1108101562371L, element.getRefreshRate());
            protoOutputStreamMode.write(1120986464260L, modeGroupIds.get(Integer.valueOf(element.getModeId())).intValue());
            protoOutputStream.write(CompanionAppsPermissions.APP_PERMISSIONS, protoOutputStreamMode.getBytes());
        }
        return protoOutputStream.getBytes();
    }

    private Map<Integer, Integer> createModeGroups(Display.Mode[] supportedModes) {
        float[] alternativeRefreshRates;
        Map<Integer, Integer> modeGroupIds = new ArrayMap<>();
        int groupId = 1;
        for (Display.Mode mode : supportedModes) {
            if (!modeGroupIds.containsKey(Integer.valueOf(mode.getModeId()))) {
                modeGroupIds.put(Integer.valueOf(mode.getModeId()), Integer.valueOf(groupId));
                for (float refreshRate : mode.getAlternativeRefreshRates()) {
                    int alternativeModeId = findModeId(supportedModes, mode.getPhysicalWidth(), mode.getPhysicalHeight(), refreshRate);
                    if (alternativeModeId != -1 && !modeGroupIds.containsKey(Integer.valueOf(alternativeModeId))) {
                        modeGroupIds.put(Integer.valueOf(alternativeModeId), Integer.valueOf(groupId));
                    }
                }
                groupId++;
            }
        }
        return modeGroupIds;
    }

    private int findModeId(Display.Mode[] modes, int width, int height, float refreshRate) {
        for (Display.Mode mode : modes) {
            if (mode.matches(width, height, refreshRate)) {
                return mode.getModeId();
            }
        }
        return -1;
    }

    private int countAccessibilityServices(String semicolonList) {
        if (TextUtils.isEmpty(semicolonList)) {
            return 0;
        }
        int semiColonNums = (int) semicolonList.chars().filter(new IntPredicate() { // from class: com.android.server.stats.pull.StatsPullAtomService$$ExternalSyntheticLambda9
            @Override // java.util.function.IntPredicate
            public final boolean test(int i) {
                return StatsPullAtomService.lambda$countAccessibilityServices$22(i);
            }
        }).count();
        if (TextUtils.isEmpty(semicolonList)) {
            return 0;
        }
        return semiColonNums + 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$countAccessibilityServices$22(int ch) {
        return ch == 58;
    }

    private boolean isAccessibilityShortcutUser(Context context, int userId) {
        ContentResolver resolver = context.getContentResolver();
        String software_shortcut_list = Settings.Secure.getStringForUser(resolver, "accessibility_button_targets", userId);
        String hardware_shortcut_list = Settings.Secure.getStringForUser(resolver, "accessibility_shortcut_target_service", userId);
        boolean hardware_shortcut_dialog_shown = Settings.Secure.getIntForUser(resolver, "accessibility_shortcut_dialog_shown", 0, userId) == 1;
        boolean software_shortcut_enabled = !TextUtils.isEmpty(software_shortcut_list);
        boolean hardware_shortcut_enabled = hardware_shortcut_dialog_shown && !TextUtils.isEmpty(hardware_shortcut_list);
        boolean triple_tap_shortcut_enabled = Settings.Secure.getIntForUser(resolver, "accessibility_display_magnification_enabled", 0, userId) == 1;
        return software_shortcut_enabled || hardware_shortcut_enabled || triple_tap_shortcut_enabled;
    }

    private boolean isAccessibilityFloatingMenuUser(Context context, int userId) {
        ContentResolver resolver = context.getContentResolver();
        int mode = Settings.Secure.getIntForUser(resolver, "accessibility_button_mode", 0, userId);
        String software_string = Settings.Secure.getStringForUser(resolver, "accessibility_button_targets", userId);
        return mode == 1 && !TextUtils.isEmpty(software_string);
    }

    private int convertToAccessibilityShortcutType(int shortcutType) {
        switch (shortcutType) {
            case 0:
                return 1;
            case 1:
                return 5;
            case 2:
                return 6;
            default:
                return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class ThermalEventListener extends IThermalEventListener.Stub {
        private ThermalEventListener() {
        }

        public void notifyThrottling(Temperature temp) {
            FrameworkStatsLog.write(189, temp.getType(), temp.getName(), (int) (temp.getValue() * 10.0f), temp.getStatus());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class ConnectivityStatsCallback extends ConnectivityManager.NetworkCallback {
        private ConnectivityStatsCallback() {
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onAvailable(Network network) {
            FrameworkStatsLog.write(98, network.getNetId(), 1);
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onLost(Network network) {
            FrameworkStatsLog.write(98, network.getNetId(), 2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class StatsSubscriptionsListener extends SubscriptionManager.OnSubscriptionsChangedListener {
        private final SubscriptionManager mSm;

        StatsSubscriptionsListener(SubscriptionManager sm) {
            this.mSm = sm;
        }

        @Override // android.telephony.SubscriptionManager.OnSubscriptionsChangedListener
        public void onSubscriptionsChanged() {
            List<SubscriptionInfo> currentSubs = this.mSm.getCompleteActiveSubscriptionInfoList();
            for (final SubscriptionInfo sub : currentSubs) {
                SubInfo match = (SubInfo) CollectionUtils.find(StatsPullAtomService.this.mHistoricalSubs, new Predicate() { // from class: com.android.server.stats.pull.StatsPullAtomService$StatsSubscriptionsListener$$ExternalSyntheticLambda0
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return StatsPullAtomService.StatsSubscriptionsListener.lambda$onSubscriptionsChanged$0(sub, (SubInfo) obj);
                    }
                });
                if (match == null) {
                    int subId = sub.getSubscriptionId();
                    String mcc = sub.getMccString();
                    String mnc = sub.getMncString();
                    String subscriberId = StatsPullAtomService.this.mTelephony.getSubscriberId(subId);
                    if (TextUtils.isEmpty(subscriberId) || TextUtils.isEmpty(mcc) || TextUtils.isEmpty(mnc) || sub.getCarrierId() == -1) {
                        Slog.e(StatsPullAtomService.TAG, "subInfo of subId " + subId + " is invalid, ignored.");
                    } else {
                        SubInfo subInfo = new SubInfo(subId, sub.getCarrierId(), mcc, mnc, subscriberId, sub.isOpportunistic());
                        Slog.i(StatsPullAtomService.TAG, "subId " + subId + " added into historical sub list");
                        synchronized (StatsPullAtomService.this.mDataBytesTransferLock) {
                            StatsPullAtomService.this.mHistoricalSubs.add(subInfo);
                            StatsPullAtomService.this.mNetworkStatsBaselines.addAll(StatsPullAtomService.this.getDataUsageBytesTransferSnapshotForSub(subInfo));
                        }
                    }
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ boolean lambda$onSubscriptionsChanged$0(SubscriptionInfo sub, SubInfo it) {
            return it.subId == sub.getSubscriptionId();
        }
    }
}
