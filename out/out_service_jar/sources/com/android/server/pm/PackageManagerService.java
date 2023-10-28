package com.android.server.pm;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.ApplicationPackageManager;
import android.app.BroadcastOptions;
import android.app.IActivityManager;
import android.app.admin.IDevicePolicyManager;
import android.app.admin.SecurityLog;
import android.app.backup.IBackupManager;
import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.AuxiliaryResolveInfo;
import android.content.pm.ChangedPackages;
import android.content.pm.ComponentInfo;
import android.content.pm.FallbackCategoryProvider;
import android.content.pm.FeatureInfo;
import android.content.pm.IDexModuleRegisterCallback;
import android.content.pm.IOnChecksumsReadyListener;
import android.content.pm.IPackageChangeObserver;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.IPackageManager;
import android.content.pm.IPackageMoveObserver;
import android.content.pm.IncrementalStatesInfo;
import android.content.pm.InstallSourceInfo;
import android.content.pm.InstantAppInfo;
import android.content.pm.InstantAppRequest;
import android.content.pm.ModuleInfo;
import android.content.pm.PackageChangeEvent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInfoLite;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PackagePartitions;
import android.content.pm.ParceledListSlice;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.SharedLibraryInfo;
import android.content.pm.Signature;
import android.content.pm.SigningDetails;
import android.content.pm.SuspendDialogInfo;
import android.content.pm.TestUtilityService;
import android.content.pm.UserInfo;
import android.content.pm.VerifierDeviceIdentity;
import android.content.pm.VersionedPackage;
import android.content.pm.overlay.OverlayPaths;
import android.content.pm.parsing.PackageLite;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.ParcelableException;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.ReconcileSdkDataArgs;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.incremental.IncrementalManager;
import android.os.incremental.PerUidReadTimeouts;
import android.os.storage.IStorageManager;
import android.os.storage.StorageManager;
import android.os.storage.StorageManagerInternal;
import android.os.storage.VolumeInfo;
import android.permission.PermissionManager;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.ExceptionUtils;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.app.ResolverActivity;
import com.android.internal.content.F2fsUtils;
import com.android.internal.content.InstallLocationUtils;
import com.android.internal.content.om.OverlayConfig;
import com.android.internal.telephony.CarrierAppUtils;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.Preconditions;
import com.android.permission.persistence.RuntimePermissionsPersistence;
import com.android.server.EventLogTags;
import com.android.server.FgThread;
import com.android.server.LocalManagerRegistry;
import com.android.server.LocalServices;
import com.android.server.LockGuard;
import com.android.server.PackageWatchdog;
import com.android.server.ServiceThread;
import com.android.server.SystemConfig;
import com.android.server.Watchdog;
import com.android.server.am.HostingRecord;
import com.android.server.apphibernation.AppHibernationManagerInternal;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.UserBackupManagerService;
import com.android.server.compat.CompatChange;
import com.android.server.compat.PlatformCompat;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.ApkChecksums;
import com.android.server.pm.CompilerStats;
import com.android.server.pm.Installer;
import com.android.server.pm.MovePackageHelper;
import com.android.server.pm.PackageInstallerService;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.PackageManagerServiceInjector;
import com.android.server.pm.Settings;
import com.android.server.pm.dex.ArtManagerService;
import com.android.server.pm.dex.ArtUtils;
import com.android.server.pm.dex.DexManager;
import com.android.server.pm.dex.DexoptOptions;
import com.android.server.pm.dex.ViewCompiler;
import com.android.server.pm.parsing.PackageCacher;
import com.android.server.pm.parsing.PackageInfoUtils;
import com.android.server.pm.parsing.PackageParser2;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import com.android.server.pm.permission.LegacyPermissionManagerInternal;
import com.android.server.pm.permission.LegacyPermissionManagerService;
import com.android.server.pm.permission.PermissionManagerService;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.PackageUserState;
import com.android.server.pm.pkg.PackageUserStateInternal;
import com.android.server.pm.pkg.SharedUserApi;
import com.android.server.pm.pkg.component.ParsedInstrumentation;
import com.android.server.pm.pkg.component.ParsedMainComponent;
import com.android.server.pm.pkg.mutate.PackageStateMutator;
import com.android.server.pm.pkg.mutate.PackageStateWrite;
import com.android.server.pm.pkg.mutate.PackageUserStateWrite;
import com.android.server.pm.pkg.parsing.ParsingPackageUtils;
import com.android.server.pm.resolution.ComponentResolver;
import com.android.server.pm.resolution.ComponentResolverApi;
import com.android.server.pm.snapshot.PackageDataSnapshot;
import com.android.server.pm.verify.domain.DomainVerificationManagerInternal;
import com.android.server.pm.verify.domain.DomainVerificationService;
import com.android.server.pm.verify.domain.proxy.DomainVerificationProxy;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.storage.DeviceStorageMonitorInternal;
import com.android.server.usage.UnixCalendar;
import com.android.server.utils.SnapshotCache;
import com.android.server.utils.TimingsTraceAndSlog;
import com.android.server.utils.Watchable;
import com.android.server.utils.Watched;
import com.android.server.utils.WatchedArrayMap;
import com.android.server.utils.WatchedSparseBooleanArray;
import com.android.server.utils.WatchedSparseIntArray;
import com.android.server.utils.Watcher;
import com.mediatek.cta.CtaManagerFactory;
import com.mediatek.server.MtkSystemServer;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.cta.CtaLinkManagerFactory;
import com.mediatek.server.pm.PmsExt;
import com.transsion.hubcore.server.defrag.ITranDefragService;
import com.transsion.hubcore.server.pm.ITranPackageManagerService;
import dalvik.system.VMRuntime;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import libcore.util.HexEncoding;
/* loaded from: classes2.dex */
public class PackageManagerService implements PackageSender, TestUtilityService {
    private static final int BLUETOOTH_UID = 1002;
    private static final long BROADCAST_DELAY = 1000;
    private static final long BROADCAST_DELAY_DURING_STARTUP = 10000;
    static final int CHECK_PENDING_INTEGRITY_VERIFICATION = 26;
    static final int CHECK_PENDING_VERIFICATION = 16;
    private static final String COMPANION_PACKAGE_NAME = "com.android.companiondevicemanager";
    public static final String COMPRESSED_EXTENSION = ".gz";
    private static final boolean DEBUG_PER_UID_READ_TIMEOUTS = false;
    private static final long DEFAULT_MANDATORY_FSTRIM_INTERVAL = 259200000;
    static final int DEFAULT_VERIFICATION_RESPONSE = 1;
    static final int DEFERRED_NO_KILL_INSTALL_OBSERVER = 24;
    private static final int DEFERRED_NO_KILL_INSTALL_OBSERVER_DELAY_MS = 500;
    static final int DEFERRED_NO_KILL_POST_DELETE = 23;
    static final int DEFERRED_NO_KILL_POST_DELETE_DELAY_MS = 3000;
    static final int DEFERRED_PENDING_KILL_INSTALL_OBSERVER = 29;
    private static final int DEFERRED_PENDING_KILL_INSTALL_OBSERVER_DELAY_MS = 1000;
    static final int DOMAIN_VERIFICATION = 27;
    static final int ENABLE_ROLLBACK_STATUS = 21;
    static final int ENABLE_ROLLBACK_TIMEOUT = 22;
    static final boolean HIDE_EPHEMERAL_APIS = false;
    static final int INIT_COPY = 5;
    static final int INSTANT_APP_RESOLUTION_PHASE_TWO = 20;
    static final int INTEGRITY_VERIFICATION_COMPLETE = 25;
    private static final int LOG_UID = 1007;
    private static final int NETWORKSTACK_UID = 1073;
    private static final int NFC_UID = 1027;
    static final String PACKAGE_MIME_TYPE = "application/vnd.android.package-archive";
    static final String PACKAGE_SCHEME = "package";
    public static final int PACKAGE_STARTABILITY_DIRECT_BOOT_UNSUPPORTED = 4;
    public static final int PACKAGE_STARTABILITY_FROZEN = 3;
    public static final int PACKAGE_STARTABILITY_NOT_FOUND = 1;
    public static final int PACKAGE_STARTABILITY_NOT_SYSTEM = 2;
    public static final int PACKAGE_STARTABILITY_OK = 0;
    static final int PACKAGE_VERIFIED = 15;
    static final int POST_INSTALL = 9;
    static final String PRECOMPILE_LAYOUTS = "pm.precompile_layouts";
    private static final String PROPERTY_INCFS_DEFAULT_TIMEOUTS = "incfs_default_timeouts";
    private static final String PROPERTY_KNOWN_DIGESTERS_LIST = "known_digesters_list";
    static final int PRUNE_UNUSED_STATIC_SHARED_LIBRARIES = 28;
    private static final int RADIO_UID = 1001;
    static final char RANDOM_CODEPATH_PREFIX = '-';
    static final String RANDOM_DIR_PREFIX = "~~";
    public static final int REASON_AB_OTA = 10;
    public static final int REASON_BACKGROUND_DEXOPT = 9;
    public static final int REASON_BOOT_AFTER_OTA = 1;
    public static final int REASON_CMDLINE = 12;
    public static final int REASON_FIRST_BOOT = 0;
    public static final int REASON_INACTIVE_PACKAGE_DOWNGRADE = 11;
    public static final int REASON_INSTALL = 3;
    public static final int REASON_INSTALL_BULK = 5;
    public static final int REASON_INSTALL_BULK_DOWNGRADED = 7;
    public static final int REASON_INSTALL_BULK_SECONDARY = 6;
    public static final int REASON_INSTALL_BULK_SECONDARY_DOWNGRADED = 8;
    public static final int REASON_INSTALL_FAST = 4;
    public static final int REASON_LAST = 13;
    public static final int REASON_POST_BOOT = 2;
    public static final int REASON_SHARED = 13;
    public static final int SCAN_AS_APK_IN_APEX = 8388608;
    static final int SCAN_AS_FULL_APP = 16384;
    static final int SCAN_AS_INSTANT_APP = 8192;
    static final int SCAN_AS_ODM = 4194304;
    static final int SCAN_AS_OEM = 262144;
    public static final int SCAN_AS_PRIVILEGED = 131072;
    public static final int SCAN_AS_PRODUCT = 1048576;
    public static final int SCAN_AS_SYSTEM = 65536;
    static final int SCAN_AS_SYSTEM_EXT = 2097152;
    public static final int SCAN_AS_VENDOR = 524288;
    static final int SCAN_AS_VIRTUAL_PRELOAD = 32768;
    static final int SCAN_BOOTING = 16;
    static final int SCAN_DONT_KILL_APP = 1024;
    static final int SCAN_DROP_CACHE = 16777216;
    static final int SCAN_FIRST_BOOT_OR_UPGRADE = 4096;
    static final int SCAN_IGNORE_FROZEN = 2048;
    static final int SCAN_INITIAL = 512;
    static final int SCAN_MOVE = 256;
    static final int SCAN_NEW_INSTALL = 4;
    public static final int SCAN_NO_DEX = 1;
    static final int SCAN_REQUIRE_KNOWN = 128;
    static final int SCAN_UPDATE_SIGNATURE = 2;
    static final int SCAN_UPDATE_TIME = 8;
    static final int SEND_PENDING_BROADCAST = 1;
    private static final int SE_UID = 1068;
    private static final int SHELL_UID = 2000;
    private static final String STATIC_SHARED_LIB_DELIMITER = "_";
    public static final String STUB_SUFFIX = "-Stub";
    static final String TAG = "PackageManager";
    private static final long THROW_EXCEPTION_ON_REQUIRE_INSTALL_PACKAGES_TO_ADD_INSTALLER_PACKAGE = 150857253;
    public static final boolean TRACE_SNAPSHOTS = false;
    private static final int UWB_UID = 1083;
    static final long WATCHDOG_TIMEOUT = 900000;
    static final int WRITE_PACKAGE_LIST = 19;
    static final int WRITE_PACKAGE_RESTRICTIONS = 14;
    static final int WRITE_SETTINGS = 13;
    static final int WRITE_SETTINGS_DELAY = 10000;
    public static IPackageManagerImpl iPackageManager;
    final String mAmbientContextDetectionPackage;
    @Watched(manual = true)
    private ApplicationInfo mAndroidApplication;
    final ApexManager mApexManager;
    private final AppDataHelper mAppDataHelper;
    private final File mAppInstallDir;
    final String mAppPredictionServicePackage;
    @Watched
    final AppsFilterImpl mAppsFilter;
    final ArtManagerService mArtManagerService;
    private final ArrayMap<String, FeatureInfo> mAvailableFeatures;
    final BackgroundDexOptService mBackgroundDexOptService;
    private final BroadcastHelper mBroadcastHelper;
    private File mCacheDir;
    final ChangedPackagesTracker mChangedPackagesTracker;
    final CompilerStats mCompilerStats;
    @Watched
    final ComponentResolver mComponentResolver;
    final String mConfiguratorPackage;
    final Context mContext;
    ComponentName mCustomResolverComponentName;
    public final int mDefParseFlags;
    private final DefaultAppProvider mDefaultAppProvider;
    final String mDefaultTextClassifierPackage;
    public final DeletePackageHelper mDeletePackageHelper;
    private IDevicePolicyManager mDevicePolicyManager;
    private final DexManager mDexManager;
    private final DexOptHelper mDexOptHelper;
    final ArraySet<Integer> mDirtyUsers;
    private final DistractingPackageHelper mDistractingPackageHelper;
    private final DomainVerificationConnection mDomainVerificationConnection;
    final DomainVerificationManagerInternal mDomainVerificationManager;
    private final boolean mEnableFreeCacheV2;
    private ArraySet<String> mExistingPackages;
    PackageManagerInternal.ExternalSourcesPolicy mExternalSourcesPolicy;
    final boolean mFactoryTest;
    private boolean mFirstBoot;
    final WatchedArrayMap<String, Integer> mFrozenPackages;
    private final SnapshotCache<WatchedArrayMap<String, Integer>> mFrozenPackagesSnapshot;
    public final Handler mHandler;
    final String mIncidentReportApproverPackage;
    final IncrementalManager mIncrementalManager;
    private final String mIncrementalVersion;
    public final InitAppsHelper mInitAppsHelper;
    final PackageManagerServiceInjector mInjector;
    final Object mInstallLock;
    private final InstallPackageHelper mInstallPackageHelper;
    final Installer mInstaller;
    final PackageInstallerService mInstallerService;
    @Watched(manual = true)
    ActivityInfo mInstantAppInstallerActivity;
    @Watched(manual = true)
    private final ResolveInfo mInstantAppInstallerInfo;
    @Watched
    final InstantAppRegistry mInstantAppRegistry;
    final InstantAppResolverConnection mInstantAppResolverConnection;
    final ComponentName mInstantAppResolverSettingsComponent;
    @Watched
    private final WatchedArrayMap<ComponentName, ParsedInstrumentation> mInstrumentation;
    private final SnapshotCache<WatchedArrayMap<ComponentName, ParsedInstrumentation>> mInstrumentationSnapshot;
    final boolean mIsEngBuild;
    private final boolean mIsPreNMR1Upgrade;
    private final boolean mIsPreNUpgrade;
    private final boolean mIsPreQUpgrade;
    private final boolean mIsUpgrade;
    private final boolean mIsUserDebugBuild;
    @Watched
    final WatchedSparseIntArray mIsolatedOwners;
    private final SnapshotCache<WatchedSparseIntArray> mIsolatedOwnersSnapshot;
    private final ArraySet<String> mKeepUninstalledPackages;
    private final LegacyPermissionManagerInternal mLegacyPermissionManager;
    private ComputerLocked mLiveComputer;
    final PackageManagerTracedLock mLock;
    final DisplayMetrics mMetrics;
    private final ModuleInfoProvider mModuleInfoProvider;
    final MovePackageHelper.MoveCallbacks mMoveCallbacks;
    int mNextInstallToken;
    private final AtomicInteger mNextMoveId;
    private final Map<String, Pair<PackageInstalledInfo, IPackageInstallObserver2>> mNoKillInstallObservers;
    private final boolean mOnlyCore;
    private final OverlayConfig mOverlayConfig;
    final String mOverlayConfigSignaturePackage;
    final ArrayList<IPackageChangeObserver> mPackageChangeObservers;
    final PackageDexOptimizer mPackageDexOptimizer;
    private final PackageObserverHelper mPackageObserverHelper;
    final PackageParser2.Callback mPackageParserCallback;
    private final PackageProperty mPackageProperty;
    private final PackageManagerTracedLock mPackageStateWriteLock;
    private final PackageUsage mPackageUsage;
    @Watched
    final WatchedArrayMap<String, AndroidPackage> mPackages;
    private final SnapshotCache<WatchedArrayMap<String, AndroidPackage>> mPackagesSnapshot;
    final PendingPackageBroadcasts mPendingBroadcasts;
    final SparseArray<VerificationParams> mPendingEnableRollback;
    int mPendingEnableRollbackToken;
    private final Map<String, Pair<PackageInstalledInfo, IPackageInstallObserver2>> mPendingKillInstallObservers;
    final SparseArray<PackageVerificationState> mPendingVerification;
    int mPendingVerificationToken;
    PerUidReadTimeouts[] mPerUidReadTimeoutsCache;
    final PermissionManagerServiceInternal mPermissionManager;
    private AndroidPackage mPlatformPackage;
    private String[] mPlatformPackageOverlayPaths;
    private String[] mPlatformPackageOverlayResourceDirs;
    private final PreferredActivityHelper mPreferredActivityHelper;
    private Future<?> mPrepareAppDataFuture;
    final ProcessLoggingHandler mProcessLoggingHandler;
    boolean mPromoteSystemApps;
    final ArraySet<String> mProtectedBroadcasts;
    final ProtectedPackages mProtectedPackages;
    final String mRecentsPackage;
    List<File> mReleaseOnSystemReady;
    private final RemovePackageHelper mRemovePackageHelper;
    private String[] mReplacedResolverPackageOverlayPaths;
    private String[] mReplacedResolverPackageOverlayResourceDirs;
    final String mRequiredInstallerPackage;
    final String mRequiredPermissionControllerPackage;
    private final String mRequiredSdkSandboxPackage;
    final String mRequiredUninstallerPackage;
    final String mRequiredVerifierPackage;
    @Watched(manual = true)
    private final ActivityInfo mResolveActivity;
    @Watched(manual = true)
    ComponentName mResolveComponentName;
    private final ResolveInfo mResolveInfo;
    private final ResolveIntentHelper mResolveIntentHelper;
    private boolean mResolverReplaced;
    final String mRetailDemoPackage;
    final SparseArray<PostInstallData> mRunningInstalls;
    @Watched(manual = true)
    private volatile boolean mSafeMode;
    private final int mSdkVersion;
    private final String[] mSeparateProcesses;
    private long mServiceStartWithDelay;
    final String mServicesExtensionPackageName;
    @Watched
    final Settings mSettings;
    final String mSetupWizardPackage;
    @Watched
    private final SharedLibrariesImpl mSharedLibraries;
    final String mSharedSystemSharedLibraryPackageName;
    private final Object mSnapshotLock;
    private final SnapshotStatistics mSnapshotStatistics;
    private final StorageEventHelper mStorageEventHelper;
    final String mStorageManagerPackage;
    private final SuspendPackageHelper mSuspendPackageHelper;
    @Watched(manual = true)
    private volatile boolean mSystemReady;
    final String mSystemTextClassifierPackageName;
    private final TestUtilityService mTestUtilityService;
    final ArraySet<String> mTransferredPackages;
    final UserManagerService mUserManager;
    final UserNeedsBadgingCache mUserNeedsBadging;
    final ViewCompiler mViewCompiler;
    private final Watcher mWatcher;
    @Watched
    private final WatchedSparseBooleanArray mWebInstantAppsDisabled;
    public static boolean DEBUG_SETTINGS = false;
    public static boolean DEBUG_PREFERRED = false;
    public static boolean DEBUG_UPGRADE = false;
    public static boolean DEBUG_DOMAIN_VERIFICATION = false;
    public static boolean DEBUG_BACKUP = false;
    public static boolean DEBUG_INSTALL = false;
    public static boolean DEBUG_REMOVE = false;
    public static boolean DEBUG_BROADCASTS = false;
    public static boolean DEBUG_PACKAGE_INFO = false;
    public static boolean DEBUG_INTENT_MATCHING = false;
    public static boolean DEBUG_PACKAGE_SCANNING = false;
    public static boolean DEBUG_VERIFY = false;
    public static boolean DEBUG_PERMISSIONS = false;
    public static boolean DEBUG_SHARED_LIBRARIES = false;
    public static final boolean DEBUG_COMPRESSION = Build.IS_DEBUGGABLE;
    public static boolean DEBUG_ART_STATSLOG = SystemProperties.getBoolean("persist.sys.pm.art.statslog", false);
    public static boolean DEBUG_DEXOPT = false;
    public static boolean DEBUG_ABI_SELECTION = false;
    public static boolean DEBUG_INSTANT = Build.IS_DEBUGGABLE;
    public static boolean DEBUG_APP_DATA = false;
    static final int[] EMPTY_INT_ARRAY = new int[0];
    public static final String PLATFORM_PACKAGE_NAME = "android";
    private static final String[] DISABLE_PACKAGE_FILTER = {"com.google.android.packageinstaller", "com.google.android.connectivity.resources", "com.google.android.permissioncontroller", UserBackupManagerService.SETTINGS_PACKAGE, "com.android.wifi.resources", "com.android.location.fused", PLATFORM_PACKAGE_NAME};
    public static final List<ScanPartition> SYSTEM_PARTITIONS = Collections.unmodifiableList(PackagePartitions.getOrderedPartitions(new Function() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda62
        @Override // java.util.function.Function
        public final Object apply(Object obj) {
            return new ScanPartition((PackagePartitions.SystemPartition) obj);
        }
    }));
    private static final PerUidReadTimeouts[] EMPTY_PER_UID_READ_TIMEOUTS_ARRAY = new PerUidReadTimeouts[0];
    private static boolean appopsState = false;
    private static final long PRUNE_UNUSED_SHARED_LIBRARIES_DELAY = TimeUnit.MINUTES.toMillis(3);
    private static final long FREE_STORAGE_UNUSED_STATIC_SHARED_LIB_MIN_CACHE_PERIOD = TimeUnit.HOURS.toMillis(2);
    static final long DEFAULT_UNUSED_STATIC_SHARED_LIB_MIN_CACHE_PERIOD = TimeUnit.DAYS.toMillis(7);
    public static MtkSystemServer sMtkSystemServerIns = MtkSystemServer.getInstance();
    public static PmsExt sPmsExt = MtkSystemServiceFactory.getInstance().makePmsExt();
    private static final AtomicReference<Computer> sSnapshot = new AtomicReference<>();
    private static final AtomicInteger sSnapshotPendingVersion = new AtomicInteger(1);
    private final PackageManagerTracedLock mOverlayPathsLock = new PackageManagerTracedLock();
    private final PackageStateMutator mPackageStateMutator = new PackageStateMutator(new Function() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda1
        @Override // java.util.function.Function
        public final Object apply(Object obj) {
            return PackageManagerService.this.getPackageSettingForMutation((String) obj);
        }
    }, new Function() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda2
        @Override // java.util.function.Function
        public final Object apply(Object obj) {
            return PackageManagerService.this.getDisabledPackageSettingForMutation((String) obj);
        }
    });

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class FindPreferredActivityBodyResult {
        boolean mChanged;
        ResolveInfo mPreferredResolveInfo;
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface PackageStartability {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface ScanFlags {
    }

    /* loaded from: classes2.dex */
    private static class DefaultSystemWrapper implements PackageManagerServiceInjector.SystemWrapper {
        private DefaultSystemWrapper() {
        }

        @Override // com.android.server.pm.PackageManagerServiceInjector.SystemWrapper
        public void disablePackageCaches() {
            PackageManager.disableApplicationInfoCache();
            PackageManager.disablePackageInfoCache();
            ApplicationPackageManager.invalidateGetPackagesForUidCache();
            ApplicationPackageManager.disableGetPackagesForUidCache();
            ApplicationPackageManager.invalidateHasSystemFeatureCache();
            PackageManager.corkPackageInfoCache();
        }

        @Override // com.android.server.pm.PackageManagerServiceInjector.SystemWrapper
        public void enablePackageCaches() {
            PackageManager.uncorkPackageInfoCache();
        }
    }

    public static boolean isAppopsState() {
        return appopsState;
    }

    public static void setAppopsState(boolean opsState) {
        appopsState = opsState;
    }

    public static void invalidatePackageInfoCache() {
        PackageManager.invalidatePackageInfoCache();
        onChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class Snapshot {
        public static final int LIVE = 1;
        public static final int SNAPPED = 2;
        public final ApplicationInfo androidApplication;
        public final String appPredictionServicePackage;
        public final AppsFilterSnapshot appsFilter;
        public final ComponentResolverApi componentResolver;
        public final WatchedArrayMap<String, Integer> frozenPackages;
        public final ActivityInfo instantAppInstallerActivity;
        public final ResolveInfo instantAppInstallerInfo;
        public final InstantAppRegistry instantAppRegistry;
        public final WatchedArrayMap<ComponentName, ParsedInstrumentation> instrumentation;
        public final WatchedSparseIntArray isolatedOwners;
        public final WatchedArrayMap<String, AndroidPackage> packages;
        public final ActivityInfo resolveActivity;
        public final ComponentName resolveComponentName;
        public final PackageManagerService service;
        public final Settings settings;
        public final SharedLibrariesRead sharedLibraries;
        public final WatchedSparseBooleanArray webInstantAppsDisabled;

        Snapshot(int type) {
            ActivityInfo activityInfo;
            if (type == 2) {
                this.settings = PackageManagerService.this.mSettings.snapshot();
                this.isolatedOwners = (WatchedSparseIntArray) PackageManagerService.this.mIsolatedOwnersSnapshot.snapshot();
                this.packages = (WatchedArrayMap) PackageManagerService.this.mPackagesSnapshot.snapshot();
                this.instrumentation = (WatchedArrayMap) PackageManagerService.this.mInstrumentationSnapshot.snapshot();
                this.resolveComponentName = PackageManagerService.this.mResolveComponentName == null ? null : PackageManagerService.this.mResolveComponentName.clone();
                this.resolveActivity = new ActivityInfo(PackageManagerService.this.mResolveActivity);
                if (PackageManagerService.this.mInstantAppInstallerActivity == null) {
                    activityInfo = null;
                } else {
                    activityInfo = new ActivityInfo(PackageManagerService.this.mInstantAppInstallerActivity);
                }
                this.instantAppInstallerActivity = activityInfo;
                this.instantAppInstallerInfo = new ResolveInfo(PackageManagerService.this.mInstantAppInstallerInfo);
                this.webInstantAppsDisabled = PackageManagerService.this.mWebInstantAppsDisabled.snapshot();
                this.instantAppRegistry = PackageManagerService.this.mInstantAppRegistry.snapshot();
                this.androidApplication = PackageManagerService.this.mAndroidApplication != null ? new ApplicationInfo(PackageManagerService.this.mAndroidApplication) : null;
                this.appPredictionServicePackage = PackageManagerService.this.mAppPredictionServicePackage;
                this.appsFilter = PackageManagerService.this.mAppsFilter.snapshot();
                this.componentResolver = PackageManagerService.this.mComponentResolver.snapshot();
                this.frozenPackages = (WatchedArrayMap) PackageManagerService.this.mFrozenPackagesSnapshot.snapshot();
                this.sharedLibraries = PackageManagerService.this.mSharedLibraries.snapshot();
            } else if (type == 1) {
                this.settings = PackageManagerService.this.mSettings;
                this.isolatedOwners = PackageManagerService.this.mIsolatedOwners;
                this.packages = PackageManagerService.this.mPackages;
                this.instrumentation = PackageManagerService.this.mInstrumentation;
                this.resolveComponentName = PackageManagerService.this.mResolveComponentName;
                this.resolveActivity = PackageManagerService.this.mResolveActivity;
                this.instantAppInstallerActivity = PackageManagerService.this.mInstantAppInstallerActivity;
                this.instantAppInstallerInfo = PackageManagerService.this.mInstantAppInstallerInfo;
                this.webInstantAppsDisabled = PackageManagerService.this.mWebInstantAppsDisabled;
                this.instantAppRegistry = PackageManagerService.this.mInstantAppRegistry;
                this.androidApplication = PackageManagerService.this.mAndroidApplication;
                this.appPredictionServicePackage = PackageManagerService.this.mAppPredictionServicePackage;
                this.appsFilter = PackageManagerService.this.mAppsFilter;
                this.componentResolver = PackageManagerService.this.mComponentResolver;
                this.frozenPackages = PackageManagerService.this.mFrozenPackages;
                this.sharedLibraries = PackageManagerService.this.mSharedLibraries;
            } else {
                throw new IllegalArgumentException();
            }
            this.service = PackageManagerService.this;
        }
    }

    public Computer snapshotComputer() {
        Computer use;
        if (Thread.holdsLock(this.mLock)) {
            return this.mLiveComputer;
        }
        AtomicReference<Computer> atomicReference = sSnapshot;
        Computer oldSnapshot = atomicReference.get();
        AtomicInteger atomicInteger = sSnapshotPendingVersion;
        int pendingVersion = atomicInteger.get();
        if (oldSnapshot != null && oldSnapshot.getVersion() == pendingVersion) {
            return oldSnapshot.use();
        }
        synchronized (this.mSnapshotLock) {
            Computer rebuildSnapshot = atomicReference.get();
            int rebuildVersion = atomicInteger.get();
            if (rebuildSnapshot != null && rebuildSnapshot.getVersion() == rebuildVersion) {
                return rebuildSnapshot.use();
            }
            synchronized (this.mLock) {
                int rebuildVersion2 = atomicInteger.get();
                Computer newSnapshot = rebuildSnapshot(rebuildSnapshot, rebuildVersion2);
                atomicReference.set(newSnapshot);
                use = newSnapshot.use();
            }
            return use;
        }
    }

    private Computer rebuildSnapshot(Computer oldSnapshot, int newVersion) {
        long now = SystemClock.currentTimeMicro();
        int hits = oldSnapshot == null ? -1 : oldSnapshot.getUsed();
        Snapshot args = new Snapshot(2);
        ComputerEngine newSnapshot = new ComputerEngine(args, newVersion);
        long done = SystemClock.currentTimeMicro();
        SnapshotStatistics snapshotStatistics = this.mSnapshotStatistics;
        if (snapshotStatistics != null) {
            snapshotStatistics.rebuild(now, done, hits);
        }
        return newSnapshot;
    }

    private ComputerLocked createLiveComputer() {
        return new ComputerLocked(new Snapshot(1));
    }

    public static void onChange(Watchable what) {
        sSnapshotPendingVersion.incrementAndGet();
    }

    static void onChanged() {
        onChange(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyInstallObserver(String packageName, boolean killApp) {
        Pair<PackageInstalledInfo, IPackageInstallObserver2> pair = killApp ? this.mPendingKillInstallObservers.remove(packageName) : this.mNoKillInstallObservers.remove(packageName);
        if (pair != null) {
            notifyInstallObserver((PackageInstalledInfo) pair.first, (IPackageInstallObserver2) pair.second);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyInstallObserver(PackageInstalledInfo info, IPackageInstallObserver2 installObserver) {
        if (installObserver != null) {
            try {
                Bundle extras = extrasForInstallResult(info);
                installObserver.onPackageInstalled(info.mName, info.mReturnCode, info.mReturnMsg, extras);
            } catch (RemoteException e) {
                Slog.i(TAG, "Observer no longer exists.");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleDeferredNoKillInstallObserver(PackageInstalledInfo info, IPackageInstallObserver2 observer) {
        String packageName = info.mPkg.getPackageName();
        this.mNoKillInstallObservers.put(packageName, Pair.create(info, observer));
        Message message = this.mHandler.obtainMessage(24, packageName);
        this.mHandler.sendMessageDelayed(message, 500L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleDeferredNoKillPostDelete(InstallArgs args) {
        Message message = this.mHandler.obtainMessage(23, args);
        this.mHandler.sendMessageDelayed(message, BackupAgentTimeoutParameters.DEFAULT_QUOTA_EXCEEDED_TIMEOUT_MILLIS);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void schedulePruneUnusedStaticSharedLibraries(boolean delay) {
        this.mHandler.removeMessages(28);
        this.mHandler.sendEmptyMessageDelayed(28, delay ? getPruneUnusedSharedLibrariesDelay() : 0L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleDeferredPendingKillInstallObserver(PackageInstalledInfo info, IPackageInstallObserver2 observer) {
        String packageName = info.mPkg.getPackageName();
        this.mPendingKillInstallObservers.put(packageName, Pair.create(info, observer));
        Message message = this.mHandler.obtainMessage(29, packageName);
        this.mHandler.sendMessageDelayed(message, 1000L);
    }

    private static long getPruneUnusedSharedLibrariesDelay() {
        return SystemProperties.getLong("debug.pm.prune_unused_shared_libraries_delay", PRUNE_UNUSED_SHARED_LIBRARIES_DELAY);
    }

    public void requestFileChecksums(File file, final String installerPackageName, final int optional, final int required, List trustedInstallers, final IOnChecksumsReadyListener onChecksumsReadyListener) throws FileNotFoundException {
        if (!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }
        Executor executor = this.mInjector.getBackgroundExecutor();
        final Handler handler = this.mInjector.getBackgroundHandler();
        final Certificate[] trustedCerts = trustedInstallers != null ? decodeCertificates(trustedInstallers) : null;
        final List<Pair<String, File>> filesToChecksum = new ArrayList<>(1);
        filesToChecksum.add(Pair.create(null, file));
        executor.execute(new Runnable() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda19
            @Override // java.lang.Runnable
            public final void run() {
                PackageManagerService.this.m5566xb1c768ab(handler, filesToChecksum, optional, required, installerPackageName, trustedCerts, onChecksumsReadyListener);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$requestFileChecksums$3$com-android-server-pm-PackageManagerService  reason: not valid java name */
    public /* synthetic */ void m5566xb1c768ab(final Handler handler, List filesToChecksum, int optional, int required, String installerPackageName, Certificate[] trustedCerts, IOnChecksumsReadyListener onChecksumsReadyListener) {
        ApkChecksums.Injector.Producer producer = new ApkChecksums.Injector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda57
            @Override // com.android.server.pm.ApkChecksums.Injector.Producer
            public final Object produce() {
                return PackageManagerService.this.m5564xe1e52128();
            }
        };
        ApkChecksums.Injector.Producer producer2 = new ApkChecksums.Injector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda58
            @Override // com.android.server.pm.ApkChecksums.Injector.Producer
            public final Object produce() {
                return PackageManagerService.lambda$requestFileChecksums$1(handler);
            }
        };
        PackageManagerServiceInjector packageManagerServiceInjector = this.mInjector;
        Objects.requireNonNull(packageManagerServiceInjector);
        ApkChecksums.Injector injector = new ApkChecksums.Injector(producer, producer2, new PackageManagerService$$ExternalSyntheticLambda6(packageManagerServiceInjector), new ApkChecksums.Injector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda59
            @Override // com.android.server.pm.ApkChecksums.Injector.Producer
            public final Object produce() {
                return PackageManagerService.this.m5565x1726a62a();
            }
        });
        ApkChecksums.getChecksums(filesToChecksum, optional, required, installerPackageName, trustedCerts, onChecksumsReadyListener, injector);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$requestFileChecksums$0$com-android-server-pm-PackageManagerService  reason: not valid java name */
    public /* synthetic */ Context m5564xe1e52128() {
        return this.mContext;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Handler lambda$requestFileChecksums$1(Handler handler) {
        return handler;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$requestFileChecksums$2$com-android-server-pm-PackageManagerService  reason: not valid java name */
    public /* synthetic */ PackageManagerInternal m5565x1726a62a() {
        return (PackageManagerInternal) this.mInjector.getLocalService(PackageManagerInternal.class);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void requestChecksumsInternal(Computer snapshot, String packageName, boolean includeSplits, final int optional, final int required, List trustedInstallers, final IOnChecksumsReadyListener onChecksumsReadyListener, int userId, Executor executor, final Handler handler) {
        String installerPackageName;
        Objects.requireNonNull(packageName);
        Objects.requireNonNull(onChecksumsReadyListener);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(handler);
        ApplicationInfo applicationInfo = snapshot.getApplicationInfoInternal(packageName, 0L, Binder.getCallingUid(), userId);
        if (applicationInfo == null) {
            throw new ParcelableException(new PackageManager.NameNotFoundException(packageName));
        }
        InstallSourceInfo installSourceInfo = snapshot.getInstallSourceInfo(packageName);
        if (installSourceInfo != null) {
            if (!TextUtils.isEmpty(installSourceInfo.getInitiatingPackageName())) {
                installerPackageName = installSourceInfo.getInitiatingPackageName();
            } else {
                installerPackageName = installSourceInfo.getInstallingPackageName();
            }
        } else {
            installerPackageName = null;
        }
        final List<Pair<String, File>> filesToChecksum = new ArrayList<>();
        filesToChecksum.add(Pair.create(null, new File(applicationInfo.sourceDir)));
        if (includeSplits && applicationInfo.splitNames != null) {
            int size = applicationInfo.splitNames.length;
            for (int i = 0; i < size; i++) {
                filesToChecksum.add(Pair.create(applicationInfo.splitNames[i], new File(applicationInfo.splitSourceDirs[i])));
            }
        }
        final Certificate[] trustedCerts = trustedInstallers != null ? decodeCertificates(trustedInstallers) : null;
        final String str = installerPackageName;
        executor.execute(new Runnable() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                PackageManagerService.this.m5563xc74004e8(handler, filesToChecksum, optional, required, str, trustedCerts, onChecksumsReadyListener);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$requestChecksumsInternal$7$com-android-server-pm-PackageManagerService  reason: not valid java name */
    public /* synthetic */ void m5563xc74004e8(final Handler handler, List filesToChecksum, int optional, int required, String installerPackageName, Certificate[] trustedCerts, IOnChecksumsReadyListener onChecksumsReadyListener) {
        ApkChecksums.Injector.Producer producer = new ApkChecksums.Injector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda4
            @Override // com.android.server.pm.ApkChecksums.Injector.Producer
            public final Object produce() {
                return PackageManagerService.this.m5561xf75dbd65();
            }
        };
        ApkChecksums.Injector.Producer producer2 = new ApkChecksums.Injector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda5
            @Override // com.android.server.pm.ApkChecksums.Injector.Producer
            public final Object produce() {
                return PackageManagerService.lambda$requestChecksumsInternal$5(handler);
            }
        };
        PackageManagerServiceInjector packageManagerServiceInjector = this.mInjector;
        Objects.requireNonNull(packageManagerServiceInjector);
        ApkChecksums.Injector injector = new ApkChecksums.Injector(producer, producer2, new PackageManagerService$$ExternalSyntheticLambda6(packageManagerServiceInjector), new ApkChecksums.Injector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda7
            @Override // com.android.server.pm.ApkChecksums.Injector.Producer
            public final Object produce() {
                return PackageManagerService.this.m5562x2c9f4267();
            }
        });
        ApkChecksums.getChecksums(filesToChecksum, optional, required, installerPackageName, trustedCerts, onChecksumsReadyListener, injector);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$requestChecksumsInternal$4$com-android-server-pm-PackageManagerService  reason: not valid java name */
    public /* synthetic */ Context m5561xf75dbd65() {
        return this.mContext;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Handler lambda$requestChecksumsInternal$5(Handler handler) {
        return handler;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$requestChecksumsInternal$6$com-android-server-pm-PackageManagerService  reason: not valid java name */
    public /* synthetic */ PackageManagerInternal m5562x2c9f4267() {
        return (PackageManagerInternal) this.mInjector.getLocalService(PackageManagerInternal.class);
    }

    private static Certificate[] decodeCertificates(List certs) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate[] result = new Certificate[certs.size()];
            int size = certs.size();
            for (int i = 0; i < size; i++) {
                InputStream is = new ByteArrayInputStream((byte[]) certs.get(i));
                X509Certificate cert = (X509Certificate) cf.generateCertificate(is);
                result[i] = cert;
            }
            return result;
        } catch (CertificateException e) {
            throw ExceptionUtils.propagate(e);
        }
    }

    private static Bundle extrasForInstallResult(PackageInstalledInfo res) {
        switch (res.mReturnCode) {
            case -112:
                Bundle extras = new Bundle();
                extras.putString("android.content.pm.extra.FAILURE_EXISTING_PERMISSION", res.mOrigPermission);
                extras.putString("android.content.pm.extra.FAILURE_EXISTING_PACKAGE", res.mOrigPackage);
                return extras;
            case 1:
                Bundle extras2 = new Bundle();
                extras2.putBoolean("android.intent.extra.REPLACING", (res.mRemovedInfo == null || res.mRemovedInfo.mRemovedPackage == null) ? false : true);
                return extras2;
            default:
                return null;
        }
    }

    public void scheduleWriteSettings() {
        invalidatePackageInfoCache();
        if (!this.mHandler.hasMessages(13)) {
            this.mHandler.sendEmptyMessageDelayed(13, 10000L);
        }
    }

    private void scheduleWritePackageListLocked(int userId) {
        invalidatePackageInfoCache();
        if (!this.mHandler.hasMessages(19)) {
            Message msg = this.mHandler.obtainMessage(19);
            msg.arg1 = userId;
            this.mHandler.sendMessageDelayed(msg, 10000L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleWritePackageRestrictions(UserHandle user) {
        int userId = user == null ? -1 : user.getIdentifier();
        scheduleWritePackageRestrictions(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleWritePackageRestrictions(int userId) {
        int[] userIds;
        invalidatePackageInfoCache();
        if (userId == -1) {
            synchronized (this.mDirtyUsers) {
                for (int aUserId : this.mUserManager.getUserIds()) {
                    this.mDirtyUsers.add(Integer.valueOf(aUserId));
                }
            }
        } else if (!this.mUserManager.exists(userId)) {
            return;
        } else {
            synchronized (this.mDirtyUsers) {
                this.mDirtyUsers.add(Integer.valueOf(userId));
            }
        }
        if (!this.mHandler.hasMessages(14)) {
            this.mHandler.sendEmptyMessageDelayed(14, 10000L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writePendingRestrictions() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(14);
            synchronized (this.mDirtyUsers) {
                Iterator<Integer> it = this.mDirtyUsers.iterator();
                while (it.hasNext()) {
                    int userId = it.next().intValue();
                    this.mSettings.writePackageRestrictionsLPr(userId);
                }
                this.mDirtyUsers.clear();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeSettings() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(13);
            this.mHandler.removeMessages(14);
            writeSettingsLPrTEMP();
            synchronized (this.mDirtyUsers) {
                this.mDirtyUsers.clear();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writePackageList(int userId) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(19);
            this.mSettings.writePackageListLPr(userId);
        }
    }

    /* JADX WARN: Type inference failed for: r6v7, types: [com.android.server.pm.PackageManagerService$IPackageManagerImpl, android.os.IBinder] */
    public static Pair<PackageManagerService, IPackageManager> main(final Context context, final Installer installer, final DomainVerificationService domainVerificationService, boolean factoryTest, final boolean onlyCore) {
        PackageManagerServiceCompilerMapping.checkProperties();
        TimingsTraceAndSlog t = new TimingsTraceAndSlog("PackageManagerTiming", 262144L);
        t.traceBegin("create package manager");
        final PackageManagerTracedLock lock = new PackageManagerTracedLock();
        final Object installLock = new Object();
        HandlerThread backgroundThread = new ServiceThread("PackageManagerBg", 10, true);
        backgroundThread.start();
        final Handler backgroundHandler = new Handler(backgroundThread.getLooper());
        PackageAbiHelperImpl packageAbiHelperImpl = new PackageAbiHelperImpl();
        List<ScanPartition> list = SYSTEM_PARTITIONS;
        PackageManagerServiceInjector.Producer producer = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda25
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                return PackageManagerService.lambda$main$8(packageManagerServiceInjector, packageManagerService);
            }
        };
        PackageManagerServiceInjector.Producer producer2 = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda36
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                PermissionManagerServiceInternal create;
                create = PermissionManagerService.create(context, packageManagerServiceInjector.getSystemConfig().getAvailableFeatures());
                return create;
            }
        };
        PackageManagerServiceInjector.Producer producer3 = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda47
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                return PackageManagerService.lambda$main$10(context, installer, installLock, onlyCore, lock, packageManagerServiceInjector, packageManagerService);
            }
        };
        PackageManagerServiceInjector.Producer producer4 = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda48
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                return PackageManagerService.lambda$main$11(DomainVerificationService.this, backgroundHandler, lock, packageManagerServiceInjector, packageManagerService);
            }
        };
        PackageManagerServiceInjector.Producer producer5 = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda49
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                AppsFilterImpl create;
                create = AppsFilterImpl.create(packageManagerServiceInjector, (PackageManagerInternal) packageManagerServiceInjector.getLocalService(PackageManagerInternal.class));
                return create;
            }
        };
        PackageManagerServiceInjector.Producer producer6 = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda50
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                return PackageManagerService.lambda$main$13(packageManagerServiceInjector, packageManagerService);
            }
        };
        PackageManagerServiceInjector.Producer producer7 = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda51
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                SystemConfig systemConfig;
                systemConfig = SystemConfig.getInstance();
                return systemConfig;
            }
        };
        PackageManagerServiceInjector.Producer producer8 = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda52
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                return PackageManagerService.lambda$main$15(packageManagerServiceInjector, packageManagerService);
            }
        };
        PackageManagerServiceInjector.Producer producer9 = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda53
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                return PackageManagerService.lambda$main$16(packageManagerServiceInjector, packageManagerService);
            }
        };
        PackageManagerServiceInjector.Producer producer10 = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda54
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                return PackageManagerService.lambda$main$17(packageManagerServiceInjector, packageManagerService);
            }
        };
        PackageManagerServiceInjector.Producer producer11 = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda26
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                ApexManager apexManager;
                apexManager = ApexManager.getInstance();
                return apexManager;
            }
        };
        PackageManagerServiceInjector.Producer producer12 = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda27
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                return PackageManagerService.lambda$main$19(packageManagerServiceInjector, packageManagerService);
            }
        };
        PackageManagerServiceInjector.Producer producer13 = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda28
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                return PackageManagerService.lambda$main$20(packageManagerServiceInjector, packageManagerService);
            }
        };
        PackageManagerServiceInjector.Producer producer14 = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda29
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                return PackageManagerService.lambda$main$23(context, packageManagerServiceInjector, packageManagerService);
            }
        };
        PackageManagerServiceInjector.Producer producer15 = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda30
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                return PackageManagerService.lambda$main$24(packageManagerServiceInjector, packageManagerService);
            }
        };
        PackageManagerServiceInjector.Producer producer16 = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda31
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                return PackageManagerService.lambda$main$25(packageManagerServiceInjector, packageManagerService);
            }
        };
        PackageManagerServiceInjector.Producer producer17 = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda32
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                return PackageManagerService.lambda$main$26(packageManagerServiceInjector, packageManagerService);
            }
        };
        PackageManagerServiceInjector.Producer producer18 = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda33
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                return PackageManagerService.lambda$main$27(packageManagerServiceInjector, packageManagerService);
            }
        };
        PackageManagerServiceInjector.Producer producer19 = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda34
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                return PackageManagerService.lambda$main$28(packageManagerServiceInjector, packageManagerService);
            }
        };
        PackageManagerServiceInjector.ProducerWithArgument producerWithArgument = new PackageManagerServiceInjector.ProducerWithArgument() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda35
            @Override // com.android.server.pm.PackageManagerServiceInjector.ProducerWithArgument
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService, Object obj) {
                return PackageManagerService.lambda$main$29(packageManagerServiceInjector, packageManagerService, (ComponentName) obj);
            }
        };
        PackageManagerServiceInjector.Producer producer20 = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda37
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                return PackageManagerService.lambda$main$30(packageManagerServiceInjector, packageManagerService);
            }
        };
        PackageManagerServiceInjector.Producer producer21 = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda38
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                LegacyPermissionManagerInternal create;
                create = LegacyPermissionManagerService.create(packageManagerServiceInjector.getContext());
                return create;
            }
        };
        PackageManagerServiceInjector.Producer producer22 = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda39
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                return PackageManagerService.lambda$main$32(DomainVerificationService.this, packageManagerServiceInjector, packageManagerService);
            }
        };
        PackageManagerServiceInjector.Producer producer23 = new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda40
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                return PackageManagerService.lambda$main$33(packageManagerServiceInjector, packageManagerService);
            }
        };
        DefaultSystemWrapper defaultSystemWrapper = new DefaultSystemWrapper();
        PackageManagerServiceInjector.ServiceProducer serviceProducer = new PackageManagerServiceInjector.ServiceProducer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda41
            @Override // com.android.server.pm.PackageManagerServiceInjector.ServiceProducer
            public final Object produce(Class cls) {
                return LocalServices.getService(cls);
            }
        };
        Objects.requireNonNull(context);
        PackageManagerServiceInjector injector = new PackageManagerServiceInjector(context, lock, installer, installLock, packageAbiHelperImpl, backgroundHandler, list, producer, producer2, producer3, producer4, producer5, producer6, producer7, producer8, producer9, producer10, producer11, producer12, producer13, producer14, producer15, producer16, producer17, producer18, producer19, producerWithArgument, producer20, producer21, producer22, producer23, defaultSystemWrapper, serviceProducer, new PackageManagerServiceInjector.ServiceProducer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda42
            @Override // com.android.server.pm.PackageManagerServiceInjector.ServiceProducer
            public final Object produce(Class cls) {
                return context.getSystemService(cls);
            }
        }, new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda43
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                return PackageManagerService.lambda$main$34(packageManagerServiceInjector, packageManagerService);
            }
        }, new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda44
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                IBackupManager asInterface;
                asInterface = IBackupManager.Stub.asInterface(ServiceManager.getService(HostingRecord.HOSTING_TYPE_BACKUP));
                return asInterface;
            }
        }, new PackageManagerServiceInjector.Producer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda45
            @Override // com.android.server.pm.PackageManagerServiceInjector.Producer
            public final Object produce(PackageManagerServiceInjector packageManagerServiceInjector, PackageManagerService packageManagerService) {
                return PackageManagerService.lambda$main$36(packageManagerServiceInjector, packageManagerService);
            }
        });
        if (Build.VERSION.SDK_INT <= 0) {
            Slog.w(TAG, "**** ro.build.version.sdk not set!");
        }
        final PackageManagerService m = new PackageManagerService(injector, onlyCore, factoryTest, PackagePartitions.FINGERPRINT, Build.IS_ENG, Build.IS_USERDEBUG, Build.VERSION.SDK_INT, Build.VERSION.INCREMENTAL);
        t.traceEnd();
        CompatChange.ChangeListener selinuxChangeListener = new CompatChange.ChangeListener() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda46
            @Override // com.android.server.compat.CompatChange.ChangeListener
            public final void onCompatChange(String str) {
                PackageManagerService.lambda$main$38(PackageManagerService.this, str);
            }
        };
        injector.getCompatibility().registerListener(143539591L, selinuxChangeListener);
        injector.getCompatibility().registerListener(168782947L, selinuxChangeListener);
        m.installAllowlistedSystemPackages();
        Objects.requireNonNull(m);
        ?? iPackageManagerImpl = new IPackageManagerImpl();
        iPackageManager = iPackageManagerImpl;
        ServiceManager.addService("package", (IBinder) iPackageManagerImpl);
        ServiceManager.addService("package_native", new PackageManagerNative(m));
        Objects.requireNonNull(m);
        LocalManagerRegistry.addManager(PackageManagerLocal.class, new PackageManagerLocalImpl());
        return Pair.create(m, iPackageManager);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ ComponentResolver lambda$main$8(PackageManagerServiceInjector i, PackageManagerService pm) {
        return new ComponentResolver(i.getUserManagerService(), pm.mUserNeedsBadging);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ UserManagerService lambda$main$10(Context context, Installer installer, Object installLock, boolean onlyCore, PackageManagerTracedLock lock, PackageManagerServiceInjector i, PackageManagerService pm) {
        return new UserManagerService(context, pm, new UserDataPreparer(installer, installLock, context, onlyCore), lock);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Settings lambda$main$11(DomainVerificationService domainVerificationService, Handler backgroundHandler, PackageManagerTracedLock lock, PackageManagerServiceInjector i, PackageManagerService pm) {
        return new Settings(Environment.getDataDirectory(), RuntimePermissionsPersistence.createInstance(), i.getPermissionManagerServiceInternal(), domainVerificationService, backgroundHandler, lock);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ PlatformCompat lambda$main$13(PackageManagerServiceInjector i, PackageManagerService pm) {
        return (PlatformCompat) ServiceManager.getService("platform_compat");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ PackageDexOptimizer lambda$main$15(PackageManagerServiceInjector i, PackageManagerService pm) {
        return new PackageDexOptimizer(i.getInstaller(), i.getInstallLock(), i.getContext(), "*dexopt*");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ DexManager lambda$main$16(PackageManagerServiceInjector i, PackageManagerService pm) {
        return new DexManager(i.getContext(), i.getPackageDexOptimizer(), i.getInstaller(), i.getInstallLock());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ ArtManagerService lambda$main$17(PackageManagerServiceInjector i, PackageManagerService pm) {
        return new ArtManagerService(i.getContext(), i.getInstaller(), i.getInstallLock());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ ViewCompiler lambda$main$19(PackageManagerServiceInjector i, PackageManagerService pm) {
        return new ViewCompiler(i.getInstallLock(), i.getInstaller());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ IncrementalManager lambda$main$20(PackageManagerServiceInjector i, PackageManagerService pm) {
        return (IncrementalManager) i.getContext().getSystemService("incremental");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ RoleManager lambda$main$21(Context context) {
        return (RoleManager) context.getSystemService(RoleManager.class);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ DefaultAppProvider lambda$main$23(final Context context, PackageManagerServiceInjector i, PackageManagerService pm) {
        return new DefaultAppProvider(new Supplier() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda20
            @Override // java.util.function.Supplier
            public final Object get() {
                return PackageManagerService.lambda$main$21(context);
            }
        }, new Supplier() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda21
            @Override // java.util.function.Supplier
            public final Object get() {
                return PackageManagerService.lambda$main$22();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ UserManagerInternal lambda$main$22() {
        return (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ DisplayMetrics lambda$main$24(PackageManagerServiceInjector i, PackageManagerService pm) {
        return new DisplayMetrics();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ PackageParser2 lambda$main$25(PackageManagerServiceInjector i, PackageManagerService pm) {
        return new PackageParser2(pm.mSeparateProcesses, pm.mOnlyCore, i.getDisplayMetrics(), pm.mCacheDir, pm.mPackageParserCallback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ PackageParser2 lambda$main$26(PackageManagerServiceInjector i, PackageManagerService pm) {
        return new PackageParser2(pm.mSeparateProcesses, pm.mOnlyCore, i.getDisplayMetrics(), null, pm.mPackageParserCallback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ PackageParser2 lambda$main$27(PackageManagerServiceInjector i, PackageManagerService pm) {
        return new PackageParser2(pm.mSeparateProcesses, false, i.getDisplayMetrics(), null, pm.mPackageParserCallback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ PackageInstallerService lambda$main$28(final PackageManagerServiceInjector i, PackageManagerService pm) {
        Context context = i.getContext();
        Objects.requireNonNull(i);
        return new PackageInstallerService(context, pm, new Supplier() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda13
            @Override // java.util.function.Supplier
            public final Object get() {
                return PackageManagerServiceInjector.this.getScanningPackageParser();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ InstantAppResolverConnection lambda$main$29(PackageManagerServiceInjector i, PackageManagerService pm, ComponentName cn) {
        return new InstantAppResolverConnection(i.getContext(), cn, "android.intent.action.RESOLVE_INSTANT_APP_PACKAGE");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ ModuleInfoProvider lambda$main$30(PackageManagerServiceInjector i, PackageManagerService pm) {
        return new ModuleInfoProvider(i.getContext());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ DomainVerificationManagerInternal lambda$main$32(DomainVerificationService domainVerificationService, PackageManagerServiceInjector i, PackageManagerService pm) {
        return domainVerificationService;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Handler lambda$main$33(PackageManagerServiceInjector i, PackageManagerService pm) {
        HandlerThread thread = new ServiceThread(TAG, 0, true);
        thread.start();
        return new PackageHandler(thread.getLooper(), pm);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ BackgroundDexOptService lambda$main$34(PackageManagerServiceInjector i, PackageManagerService pm) {
        return new BackgroundDexOptService(i.getContext(), i.getDexManager(), pm);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ SharedLibrariesImpl lambda$main$36(PackageManagerServiceInjector i, PackageManagerService pm) {
        return new SharedLibrariesImpl(pm, i);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$main$38(PackageManagerService m, String packageName) {
        synchronized (m.mInstallLock) {
            Computer snapshot = m.snapshotComputer();
            PackageStateInternal packageState = snapshot.getPackageStateInternal(packageName);
            if (packageState == null) {
                Slog.e(TAG, "Failed to find package setting " + packageName);
                return;
            }
            AndroidPackage pkg = packageState.getPkg();
            SharedUserApi sharedUser = snapshot.getSharedUser(packageState.getSharedUserAppId());
            String oldSeInfo = AndroidPackageUtils.getSeInfo(pkg, packageState);
            if (pkg == null) {
                Slog.e(TAG, "Failed to find package " + packageName);
                return;
            }
            final String newSeInfo = SELinuxMMAC.getSeInfo(pkg, sharedUser, m.mInjector.getCompatibility());
            if (!newSeInfo.equals(oldSeInfo)) {
                Slog.i(TAG, "Updating seInfo for package " + packageName + " from: " + oldSeInfo + " to: " + newSeInfo);
                m.commitPackageStateMutation(null, packageName, new Consumer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda61
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((PackageStateWrite) obj).setOverrideSeInfo(newSeInfo);
                    }
                });
                m.mAppDataHelper.prepareAppDataAfterInstallLIF(pkg);
            }
        }
    }

    private void installAllowlistedSystemPackages() {
        if (this.mUserManager.installWhitelistedSystemPackages(isFirstBoot(), isDeviceUpgrading(), this.mExistingPackages)) {
            scheduleWritePackageRestrictions(-1);
            scheduleWriteSettings();
        }
    }

    private void registerObservers(boolean verify) {
        WatchedArrayMap<String, AndroidPackage> watchedArrayMap = this.mPackages;
        if (watchedArrayMap != null) {
            watchedArrayMap.registerObserver(this.mWatcher);
        }
        SharedLibrariesImpl sharedLibrariesImpl = this.mSharedLibraries;
        if (sharedLibrariesImpl != null) {
            sharedLibrariesImpl.registerObserver(this.mWatcher);
        }
        WatchedArrayMap<ComponentName, ParsedInstrumentation> watchedArrayMap2 = this.mInstrumentation;
        if (watchedArrayMap2 != null) {
            watchedArrayMap2.registerObserver(this.mWatcher);
        }
        WatchedSparseBooleanArray watchedSparseBooleanArray = this.mWebInstantAppsDisabled;
        if (watchedSparseBooleanArray != null) {
            watchedSparseBooleanArray.registerObserver(this.mWatcher);
        }
        AppsFilterImpl appsFilterImpl = this.mAppsFilter;
        if (appsFilterImpl != null) {
            appsFilterImpl.registerObserver(this.mWatcher);
        }
        InstantAppRegistry instantAppRegistry = this.mInstantAppRegistry;
        if (instantAppRegistry != null) {
            instantAppRegistry.registerObserver(this.mWatcher);
        }
        Settings settings = this.mSettings;
        if (settings != null) {
            settings.registerObserver(this.mWatcher);
        }
        WatchedSparseIntArray watchedSparseIntArray = this.mIsolatedOwners;
        if (watchedSparseIntArray != null) {
            watchedSparseIntArray.registerObserver(this.mWatcher);
        }
        ComponentResolver componentResolver = this.mComponentResolver;
        if (componentResolver != null) {
            componentResolver.registerObserver(this.mWatcher);
        }
        WatchedArrayMap<String, Integer> watchedArrayMap3 = this.mFrozenPackages;
        if (watchedArrayMap3 != null) {
            watchedArrayMap3.registerObserver(this.mWatcher);
        }
        if (verify) {
            Watchable.verifyWatchedAttributes(this, this.mWatcher, (this.mIsEngBuild || this.mIsUserDebugBuild) ? false : true);
        }
    }

    public PackageManagerService(PackageManagerServiceInjector injector, PackageManagerServiceTestParams testParams) {
        WatchedArrayMap<String, AndroidPackage> watchedArrayMap = new WatchedArrayMap<>();
        this.mPackages = watchedArrayMap;
        this.mPackagesSnapshot = new SnapshotCache.Auto(watchedArrayMap, watchedArrayMap, "PackageManagerService.mPackages");
        WatchedSparseIntArray watchedSparseIntArray = new WatchedSparseIntArray();
        this.mIsolatedOwners = watchedSparseIntArray;
        this.mIsolatedOwnersSnapshot = new SnapshotCache.Auto(watchedSparseIntArray, watchedSparseIntArray, "PackageManagerService.mIsolatedOwners");
        this.mExistingPackages = null;
        WatchedArrayMap<String, Integer> watchedArrayMap2 = new WatchedArrayMap<>();
        this.mFrozenPackages = watchedArrayMap2;
        this.mFrozenPackagesSnapshot = new SnapshotCache.Auto(watchedArrayMap2, watchedArrayMap2, "PackageManagerService.mFrozenPackages");
        this.mPackageObserverHelper = new PackageObserverHelper();
        this.mPackageChangeObservers = new ArrayList<>();
        WatchedArrayMap<ComponentName, ParsedInstrumentation> watchedArrayMap3 = new WatchedArrayMap<>();
        this.mInstrumentation = watchedArrayMap3;
        this.mInstrumentationSnapshot = new SnapshotCache.Auto(watchedArrayMap3, watchedArrayMap3, "PackageManagerService.mInstrumentation");
        this.mTransferredPackages = new ArraySet<>();
        this.mProtectedBroadcasts = new ArraySet<>();
        this.mPendingVerification = new SparseArray<>();
        this.mPendingEnableRollback = new SparseArray<>();
        this.mNextMoveId = new AtomicInteger();
        this.mPendingVerificationToken = 0;
        this.mPendingEnableRollbackToken = 0;
        this.mWebInstantAppsDisabled = new WatchedSparseBooleanArray();
        this.mResolveActivity = new ActivityInfo();
        this.mResolveInfo = new ResolveInfo();
        this.mPlatformPackageOverlayPaths = null;
        this.mPlatformPackageOverlayResourceDirs = null;
        this.mReplacedResolverPackageOverlayPaths = null;
        this.mReplacedResolverPackageOverlayResourceDirs = null;
        this.mResolverReplaced = false;
        this.mInstantAppInstallerInfo = new ResolveInfo();
        this.mNoKillInstallObservers = Collections.synchronizedMap(new HashMap());
        this.mPendingKillInstallObservers = Collections.synchronizedMap(new HashMap());
        this.mKeepUninstalledPackages = new ArraySet<>();
        this.mDevicePolicyManager = null;
        this.mPackageProperty = new PackageProperty();
        this.mDirtyUsers = new ArraySet<>();
        this.mRunningInstalls = new SparseArray<>();
        this.mNextInstallToken = 1;
        this.mPackageUsage = new PackageUsage();
        this.mCompilerStats = new CompilerStats();
        this.mWatcher = new Watcher() { // from class: com.android.server.pm.PackageManagerService.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                PackageManagerService.onChange(what);
            }
        };
        this.mSnapshotLock = new Object();
        this.mInjector = injector;
        injector.bootstrap(this);
        this.mAppsFilter = injector.getAppsFilter();
        this.mComponentResolver = injector.getComponentResolver();
        this.mContext = injector.getContext();
        this.mInstaller = injector.getInstaller();
        this.mInstallLock = injector.getInstallLock();
        PackageManagerTracedLock lock = injector.getLock();
        this.mLock = lock;
        this.mPackageStateWriteLock = lock;
        this.mPermissionManager = injector.getPermissionManagerServiceInternal();
        this.mSettings = injector.getSettings();
        UserManagerService userManagerService = injector.getUserManagerService();
        this.mUserManager = userManagerService;
        this.mUserNeedsBadging = new UserNeedsBadgingCache(userManagerService);
        this.mDomainVerificationManager = injector.getDomainVerificationManagerInternal();
        this.mHandler = injector.getHandler();
        SharedLibrariesImpl sharedLibrariesImpl = injector.getSharedLibrariesImpl();
        this.mSharedLibraries = sharedLibrariesImpl;
        this.mApexManager = testParams.apexManager;
        this.mArtManagerService = testParams.artManagerService;
        this.mAvailableFeatures = testParams.availableFeatures;
        this.mBackgroundDexOptService = testParams.backgroundDexOptService;
        this.mDefParseFlags = testParams.defParseFlags;
        this.mDefaultAppProvider = testParams.defaultAppProvider;
        this.mLegacyPermissionManager = testParams.legacyPermissionManagerInternal;
        this.mDexManager = testParams.dexManager;
        this.mFactoryTest = testParams.factoryTest;
        this.mIncrementalManager = testParams.incrementalManager;
        this.mInstallerService = testParams.installerService;
        this.mInstantAppRegistry = testParams.instantAppRegistry;
        this.mChangedPackagesTracker = testParams.changedPackagesTracker;
        this.mInstantAppResolverConnection = testParams.instantAppResolverConnection;
        this.mInstantAppResolverSettingsComponent = testParams.instantAppResolverSettingsComponent;
        this.mIsPreNMR1Upgrade = testParams.isPreNmr1Upgrade;
        this.mIsPreNUpgrade = testParams.isPreNupgrade;
        this.mIsPreQUpgrade = testParams.isPreQupgrade;
        this.mIsUpgrade = testParams.isUpgrade;
        this.mMetrics = testParams.Metrics;
        this.mModuleInfoProvider = testParams.moduleInfoProvider;
        this.mMoveCallbacks = testParams.moveCallbacks;
        this.mOnlyCore = testParams.onlyCore;
        this.mOverlayConfig = testParams.overlayConfig;
        this.mPackageDexOptimizer = testParams.packageDexOptimizer;
        this.mPackageParserCallback = testParams.packageParserCallback;
        this.mPendingBroadcasts = testParams.pendingPackageBroadcasts;
        this.mTestUtilityService = testParams.testUtilityService;
        this.mProcessLoggingHandler = testParams.processLoggingHandler;
        this.mProtectedPackages = testParams.protectedPackages;
        this.mSeparateProcesses = testParams.separateProcesses;
        this.mViewCompiler = testParams.viewCompiler;
        this.mRequiredVerifierPackage = testParams.requiredVerifierPackage;
        this.mRequiredInstallerPackage = testParams.requiredInstallerPackage;
        this.mRequiredUninstallerPackage = testParams.requiredUninstallerPackage;
        this.mRequiredPermissionControllerPackage = testParams.requiredPermissionControllerPackage;
        this.mSetupWizardPackage = testParams.setupWizardPackage;
        this.mStorageManagerPackage = testParams.storageManagerPackage;
        this.mDefaultTextClassifierPackage = testParams.defaultTextClassifierPackage;
        this.mSystemTextClassifierPackageName = testParams.systemTextClassifierPackage;
        this.mRetailDemoPackage = testParams.retailDemoPackage;
        this.mRecentsPackage = testParams.recentsPackage;
        this.mAmbientContextDetectionPackage = testParams.ambientContextDetectionPackage;
        this.mConfiguratorPackage = testParams.configuratorPackage;
        this.mAppPredictionServicePackage = testParams.appPredictionServicePackage;
        this.mIncidentReportApproverPackage = testParams.incidentReportApproverPackage;
        this.mServicesExtensionPackageName = testParams.servicesExtensionPackageName;
        this.mSharedSystemSharedLibraryPackageName = testParams.sharedSystemSharedLibraryPackageName;
        this.mOverlayConfigSignaturePackage = testParams.overlayConfigSignaturePackage;
        this.mResolveComponentName = testParams.resolveComponentName;
        this.mRequiredSdkSandboxPackage = testParams.requiredSdkSandboxPackage;
        this.mLiveComputer = createLiveComputer();
        this.mSnapshotStatistics = null;
        watchedArrayMap.putAll(testParams.packages);
        this.mEnableFreeCacheV2 = testParams.enableFreeCacheV2;
        this.mSdkVersion = testParams.sdkVersion;
        this.mAppInstallDir = testParams.appInstallDir;
        this.mIsEngBuild = testParams.isEngBuild;
        this.mIsUserDebugBuild = testParams.isUserDebugBuild;
        this.mIncrementalVersion = testParams.incrementalVersion;
        this.mDomainVerificationConnection = new DomainVerificationConnection(this);
        this.mBroadcastHelper = testParams.broadcastHelper;
        this.mAppDataHelper = testParams.appDataHelper;
        this.mInstallPackageHelper = testParams.installPackageHelper;
        this.mRemovePackageHelper = testParams.removePackageHelper;
        this.mInitAppsHelper = testParams.initAndSystemPackageHelper;
        DeletePackageHelper deletePackageHelper = testParams.deletePackageHelper;
        this.mDeletePackageHelper = deletePackageHelper;
        this.mPreferredActivityHelper = testParams.preferredActivityHelper;
        this.mResolveIntentHelper = testParams.resolveIntentHelper;
        this.mDexOptHelper = testParams.dexOptHelper;
        this.mSuspendPackageHelper = testParams.suspendPackageHelper;
        this.mDistractingPackageHelper = testParams.distractingPackageHelper;
        sharedLibrariesImpl.setDeletePackageHelper(deletePackageHelper);
        this.mStorageEventHelper = testParams.storageEventHelper;
        registerObservers(false);
        invalidatePackageInfoCache();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2350=4] */
    public PackageManagerService(PackageManagerServiceInjector injector, boolean onlyCore, boolean factoryTest, String buildFingerprint, boolean isEngBuild, boolean isUserDebugBuild, int sdkVersion, String incrementalVersion) {
        String str;
        Iterator<AndroidPackage> it;
        String bootClassPath;
        String customResolverActivityName;
        List<String> changedAbiCodePath;
        String customResolverActivityName2;
        WatchedArrayMap<String, AndroidPackage> watchedArrayMap = new WatchedArrayMap<>();
        this.mPackages = watchedArrayMap;
        this.mPackagesSnapshot = new SnapshotCache.Auto(watchedArrayMap, watchedArrayMap, "PackageManagerService.mPackages");
        WatchedSparseIntArray watchedSparseIntArray = new WatchedSparseIntArray();
        this.mIsolatedOwners = watchedSparseIntArray;
        this.mIsolatedOwnersSnapshot = new SnapshotCache.Auto(watchedSparseIntArray, watchedSparseIntArray, "PackageManagerService.mIsolatedOwners");
        this.mExistingPackages = null;
        WatchedArrayMap<String, Integer> watchedArrayMap2 = new WatchedArrayMap<>();
        this.mFrozenPackages = watchedArrayMap2;
        this.mFrozenPackagesSnapshot = new SnapshotCache.Auto(watchedArrayMap2, watchedArrayMap2, "PackageManagerService.mFrozenPackages");
        this.mPackageObserverHelper = new PackageObserverHelper();
        this.mPackageChangeObservers = new ArrayList<>();
        WatchedArrayMap<ComponentName, ParsedInstrumentation> watchedArrayMap3 = new WatchedArrayMap<>();
        this.mInstrumentation = watchedArrayMap3;
        this.mInstrumentationSnapshot = new SnapshotCache.Auto(watchedArrayMap3, watchedArrayMap3, "PackageManagerService.mInstrumentation");
        this.mTransferredPackages = new ArraySet<>();
        this.mProtectedBroadcasts = new ArraySet<>();
        this.mPendingVerification = new SparseArray<>();
        this.mPendingEnableRollback = new SparseArray<>();
        this.mNextMoveId = new AtomicInteger();
        this.mPendingVerificationToken = 0;
        this.mPendingEnableRollbackToken = 0;
        this.mWebInstantAppsDisabled = new WatchedSparseBooleanArray();
        this.mResolveActivity = new ActivityInfo();
        this.mResolveInfo = new ResolveInfo();
        this.mPlatformPackageOverlayPaths = null;
        this.mPlatformPackageOverlayResourceDirs = null;
        this.mReplacedResolverPackageOverlayPaths = null;
        this.mReplacedResolverPackageOverlayResourceDirs = null;
        this.mResolverReplaced = false;
        this.mInstantAppInstallerInfo = new ResolveInfo();
        this.mNoKillInstallObservers = Collections.synchronizedMap(new HashMap());
        this.mPendingKillInstallObservers = Collections.synchronizedMap(new HashMap());
        this.mKeepUninstalledPackages = new ArraySet<>();
        this.mDevicePolicyManager = null;
        this.mPackageProperty = new PackageProperty();
        this.mDirtyUsers = new ArraySet<>();
        this.mRunningInstalls = new SparseArray<>();
        this.mNextInstallToken = 1;
        this.mPackageUsage = new PackageUsage();
        this.mCompilerStats = new CompilerStats();
        this.mWatcher = new Watcher() { // from class: com.android.server.pm.PackageManagerService.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                PackageManagerService.onChange(what);
            }
        };
        this.mSnapshotLock = new Object();
        this.mIsEngBuild = isEngBuild;
        this.mIsUserDebugBuild = isUserDebugBuild;
        this.mSdkVersion = sdkVersion;
        this.mIncrementalVersion = incrementalVersion;
        this.mInjector = injector;
        injector.getSystemWrapper().disablePackageCaches();
        TimingsTraceAndSlog t = new TimingsTraceAndSlog("PackageManagerTiming", 262144L);
        this.mPendingBroadcasts = new PendingPackageBroadcasts();
        injector.bootstrap(this);
        PackageManagerTracedLock lock = injector.getLock();
        this.mLock = lock;
        this.mPackageStateWriteLock = lock;
        Object installLock = injector.getInstallLock();
        this.mInstallLock = installLock;
        LockGuard.installLock(lock, 3);
        EventLog.writeEvent((int) EventLogTags.BOOT_PROGRESS_PMS_START, SystemClock.uptimeMillis());
        sMtkSystemServerIns.addBootEvent("Android:PackageManagerService_Start");
        Context context = injector.getContext();
        this.mContext = context;
        this.mFactoryTest = factoryTest;
        this.mOnlyCore = onlyCore;
        DisplayMetrics displayMetrics = injector.getDisplayMetrics();
        this.mMetrics = displayMetrics;
        Installer installer = injector.getInstaller();
        this.mInstaller = installer;
        ITranPackageManagerService.Instance().onPMSInit(this, context);
        this.mEnableFreeCacheV2 = SystemProperties.getBoolean("fw.free_cache_v2", true);
        CtaManagerFactory.getInstance().makeCtaManager().createCtaPermsController(context);
        CtaLinkManagerFactory.getInstance().makeCtaLinkManager().createCtaPermsController(context);
        t.traceBegin("createSubComponents");
        LocalServices.addService(PackageManagerInternal.class, new PackageManagerInternalImpl());
        LocalServices.addService(TestUtilityService.class, this);
        this.mTestUtilityService = (TestUtilityService) LocalServices.getService(TestUtilityService.class);
        UserManagerService userManagerService = injector.getUserManagerService();
        this.mUserManager = userManagerService;
        UserNeedsBadgingCache userNeedsBadgingCache = new UserNeedsBadgingCache(userManagerService);
        this.mUserNeedsBadging = userNeedsBadgingCache;
        ComponentResolver componentResolver = injector.getComponentResolver();
        this.mComponentResolver = componentResolver;
        ITranPackageManagerService.Instance().init(context, componentResolver);
        PermissionManagerServiceInternal permissionManagerServiceInternal = injector.getPermissionManagerServiceInternal();
        this.mPermissionManager = permissionManagerServiceInternal;
        Settings settings = injector.getSettings();
        this.mSettings = settings;
        this.mIncrementalManager = injector.getIncrementalManager();
        this.mDefaultAppProvider = injector.getDefaultAppProvider();
        this.mLegacyPermissionManager = injector.getLegacyPermissionManagerInternal();
        final PlatformCompat platformCompat = injector.getCompatibility();
        this.mPackageParserCallback = new PackageParser2.Callback() { // from class: com.android.server.pm.PackageManagerService.2
            @Override // com.android.server.pm.parsing.PackageParser2.Callback
            public boolean isChangeEnabled(long changeId, ApplicationInfo appInfo) {
                return platformCompat.isChangeEnabled(changeId, appInfo);
            }

            @Override // com.android.server.pm.pkg.parsing.ParsingPackageUtils.Callback
            public boolean hasFeature(String feature) {
                return PackageManagerService.this.hasSystemFeature(feature, 0);
            }
        };
        t.traceEnd();
        t.traceBegin("addSharedUsers");
        settings.addSharedUserLPw("android.uid.system", 1000, 1, 8);
        settings.addSharedUserLPw("android.uid.phone", 1001, 1, 8);
        settings.addSharedUserLPw("android.uid.log", 1007, 1, 8);
        settings.addSharedUserLPw("android.uid.nfc", 1027, 1, 8);
        settings.addSharedUserLPw("android.uid.bluetooth", 1002, 1, 8);
        settings.addSharedUserLPw("android.uid.shell", 2000, 1, 8);
        settings.addSharedUserLPw("android.uid.se", SE_UID, 1, 8);
        settings.addSharedUserLPw("android.uid.networkstack", NETWORKSTACK_UID, 1, 8);
        settings.addSharedUserLPw("android.uid.uwb", UWB_UID, 1, 8);
        t.traceEnd();
        String separateProcesses = SystemProperties.get("debug.separate_processes");
        if (separateProcesses == null || separateProcesses.length() <= 0) {
            this.mDefParseFlags = 0;
            this.mSeparateProcesses = null;
        } else if ("*".equals(separateProcesses)) {
            this.mDefParseFlags = 2;
            this.mSeparateProcesses = null;
            Slog.w(TAG, "Running with debug.separate_processes: * (ALL)");
        } else {
            this.mDefParseFlags = 0;
            this.mSeparateProcesses = separateProcesses.split(",");
            Slog.w(TAG, "Running with debug.separate_processes: " + separateProcesses);
        }
        this.mPackageDexOptimizer = injector.getPackageDexOptimizer();
        this.mDexManager = injector.getDexManager();
        this.mBackgroundDexOptService = injector.getBackgroundDexOptService();
        this.mArtManagerService = injector.getArtManagerService();
        this.mMoveCallbacks = new MovePackageHelper.MoveCallbacks(FgThread.get().getLooper());
        this.mViewCompiler = injector.getViewCompiler();
        SharedLibrariesImpl sharedLibrariesImpl = injector.getSharedLibrariesImpl();
        this.mSharedLibraries = sharedLibrariesImpl;
        ((DisplayManager) context.getSystemService(DisplayManager.class)).getDisplay(0).getMetrics(displayMetrics);
        t.traceBegin("get system config");
        SystemConfig systemConfig = injector.getSystemConfig();
        this.mAvailableFeatures = systemConfig.getAvailableFeatures();
        t.traceEnd();
        ProtectedPackages protectedPackages = new ProtectedPackages(context);
        this.mProtectedPackages = protectedPackages;
        this.mApexManager = injector.getApexManager();
        this.mAppsFilter = injector.getAppsFilter();
        this.mInstantAppRegistry = new InstantAppRegistry(context, permissionManagerServiceInternal, injector.getUserManagerInternal(), new DeletePackageHelper(this));
        this.mChangedPackagesTracker = new ChangedPackagesTracker();
        this.mAppInstallDir = new File(Environment.getDataDirectory(), "app");
        DomainVerificationConnection domainVerificationConnection = new DomainVerificationConnection(this);
        this.mDomainVerificationConnection = domainVerificationConnection;
        DomainVerificationManagerInternal domainVerificationManagerInternal = injector.getDomainVerificationManagerInternal();
        this.mDomainVerificationManager = domainVerificationManagerInternal;
        domainVerificationManagerInternal.setConnection(domainVerificationConnection);
        BroadcastHelper broadcastHelper = new BroadcastHelper(injector);
        this.mBroadcastHelper = broadcastHelper;
        AppDataHelper appDataHelper = new AppDataHelper(this);
        this.mAppDataHelper = appDataHelper;
        this.mInstallPackageHelper = new InstallPackageHelper(this, appDataHelper);
        RemovePackageHelper removePackageHelper = new RemovePackageHelper(this, appDataHelper);
        this.mRemovePackageHelper = removePackageHelper;
        TimingsTraceAndSlog t2 = t;
        DeletePackageHelper deletePackageHelper = new DeletePackageHelper(this, removePackageHelper, appDataHelper);
        this.mDeletePackageHelper = deletePackageHelper;
        sharedLibrariesImpl.setDeletePackageHelper(deletePackageHelper);
        PreferredActivityHelper preferredActivityHelper = new PreferredActivityHelper(this);
        this.mPreferredActivityHelper = preferredActivityHelper;
        this.mResolveIntentHelper = new ResolveIntentHelper(context, preferredActivityHelper, injector.getCompatibility(), userManagerService, domainVerificationManagerInternal, userNeedsBadgingCache, new Supplier() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda9
            @Override // java.util.function.Supplier
            public final Object get() {
                return PackageManagerService.this.m5557lambda$new$39$comandroidserverpmPackageManagerService();
            }
        }, new Supplier() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda10
            @Override // java.util.function.Supplier
            public final Object get() {
                return PackageManagerService.this.m5558lambda$new$40$comandroidserverpmPackageManagerService();
            }
        });
        this.mDexOptHelper = new DexOptHelper(this);
        SuspendPackageHelper suspendPackageHelper = new SuspendPackageHelper(this, injector, broadcastHelper, protectedPackages);
        this.mSuspendPackageHelper = suspendPackageHelper;
        this.mStorageEventHelper = new StorageEventHelper(this, deletePackageHelper, removePackageHelper);
        this.mDistractingPackageHelper = new DistractingPackageHelper(this, injector, broadcastHelper, suspendPackageHelper);
        synchronized (lock) {
            try {
                this.mSnapshotStatistics = new SnapshotStatistics();
                sSnapshotPendingVersion.incrementAndGet();
                this.mLiveComputer = createLiveComputer();
                registerObservers(true);
            } catch (Throwable th) {
                th = th;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                throw th;
            }
        }
        Computer computer = this.mLiveComputer;
        synchronized (installLock) {
            try {
                try {
                    synchronized (lock) {
                        try {
                            Handler handler = injector.getHandler();
                            this.mHandler = handler;
                            this.mProcessLoggingHandler = new ProcessLoggingHandler();
                            Watchdog.getInstance().addThread(handler, WATCHDOG_TIMEOUT);
                            if (Build.TRAN_DEFRAG_SUPPORT) {
                                try {
                                    ITranDefragService.Instance().initialize(context, installer);
                                } catch (Throwable th3) {
                                    th = th3;
                                    while (true) {
                                        try {
                                            break;
                                        } catch (Throwable th4) {
                                            th = th4;
                                        }
                                    }
                                    throw th;
                                }
                            }
                            ArrayMap<String, SystemConfig.SharedLibraryEntry> libConfig = systemConfig.getSharedLibraries();
                            int builtInLibCount = libConfig.size();
                            for (int i = 0; i < builtInLibCount; i++) {
                                this.mSharedLibraries.addBuiltInSharedLibraryLPw(libConfig.valueAt(i));
                            }
                            for (int i2 = 0; i2 < builtInLibCount; i2++) {
                                String name = libConfig.keyAt(i2);
                                SystemConfig.SharedLibraryEntry entry = libConfig.valueAt(i2);
                                int dependencyCount = entry.dependencies.length;
                                for (int j = 0; j < dependencyCount; j++) {
                                    SharedLibraryInfo dependency = computer.getSharedLibraryInfo(entry.dependencies[j], -1L);
                                    if (dependency != null) {
                                        computer.getSharedLibraryInfo(name, -1L).addDependency(dependency);
                                    }
                                }
                            }
                            SELinuxMMAC.readInstallPolicy();
                            try {
                                t2.traceBegin("loadFallbacks");
                                FallbackCategoryProvider.loadFallbacks();
                                t2.traceEnd();
                                sPmsExt.initBeforeScan();
                                ITranPackageManagerService.Instance().initReadedRemovePackageList();
                                t2.traceBegin("read user settings");
                                this.mFirstBoot = !this.mSettings.readLPw(computer, this.mInjector.getUserManagerInternal().getUsers(true, false, false));
                                t2.traceEnd();
                                if (this.mFirstBoot) {
                                    try {
                                        t2.traceBegin("setFirstBoot: ");
                                        try {
                                            this.mInstaller.setFirstBoot();
                                        } catch (Installer.InstallerException e) {
                                            Slog.w(TAG, "Could not set First Boot: ", e);
                                        }
                                        t2.traceEnd();
                                    } catch (Throwable th5) {
                                        th = th5;
                                        while (true) {
                                            break;
                                            break;
                                        }
                                        throw th;
                                    }
                                }
                                this.mPermissionManager.readLegacyPermissionsTEMP(this.mSettings.mPermissions);
                                this.mPermissionManager.readLegacyPermissionStateTEMP();
                                sPmsExt.init(this, this.mUserManager);
                                ITranPackageManagerService.Instance().init(this, this.mUserManager, this.mContext);
                                ITranPackageManagerService.Instance().storeReadedRemovePackages();
                                if (!this.mOnlyCore && this.mFirstBoot) {
                                    ITranPackageManagerService.Instance().restoreOOBEFile();
                                    DexOptHelper.requestCopyPreoptedFiles();
                                }
                                String customResolverActivityName3 = Resources.getSystem().getString(17039910);
                                if (!TextUtils.isEmpty(customResolverActivityName3)) {
                                    this.mCustomResolverComponentName = ComponentName.unflattenFromString(customResolverActivityName3);
                                }
                                long startTime = SystemClock.uptimeMillis();
                                EventLog.writeEvent((int) EventLogTags.BOOT_PROGRESS_PMS_SYSTEM_SCAN_START, startTime);
                                sMtkSystemServerIns.addBootEvent("Android:PMS_scan_START");
                                String bootClassPath2 = System.getenv("BOOTCLASSPATH");
                                String systemServerClassPath = System.getenv("SYSTEMSERVERCLASSPATH");
                                if (bootClassPath2 == null) {
                                    Slog.w(TAG, "No BOOTCLASSPATH found!");
                                }
                                if (systemServerClassPath == null) {
                                    Slog.w(TAG, "No SYSTEMSERVERCLASSPATH found!");
                                }
                                Settings.VersionInfo ver = this.mSettings.getInternalVersion();
                                boolean z = !buildFingerprint.equals(ver.fingerprint);
                                this.mIsUpgrade = z;
                                if (z) {
                                    ITranPackageManagerService.Instance().restoreOOBEFile();
                                    PackageManagerServiceUtils.logCriticalInfo(4, "Upgrading from " + ver.fingerprint + " to " + PackagePartitions.FINGERPRINT);
                                }
                                this.mInitAppsHelper = new InitAppsHelper(this, this.mApexManager, this.mInstallPackageHelper, this.mInjector.getSystemPartitions());
                                this.mPromoteSystemApps = z && ver.sdkVersion <= 22;
                                this.mIsPreNUpgrade = z && ver.sdkVersion < 24;
                                this.mIsPreNMR1Upgrade = z && ver.sdkVersion < 25;
                                this.mIsPreQUpgrade = z && ver.sdkVersion < 29;
                                WatchedArrayMap<String, PackageSetting> packageSettings = this.mSettings.getPackagesLocked();
                                if (isDeviceUpgrading()) {
                                    this.mExistingPackages = new ArraySet<>(packageSettings.size());
                                    for (PackageSetting ps : packageSettings.values()) {
                                        this.mExistingPackages.add(ps.getPackageName());
                                    }
                                }
                                this.mCacheDir = PackageManagerServiceUtils.preparePackageParserCache(this.mIsEngBuild, this.mIsUserDebugBuild, this.mIncrementalVersion);
                                int[] userIds = this.mUserManager.getUserIds();
                                PackageParser2 packageParser = this.mInjector.getScanningCachingPackageParser();
                                Settings.VersionInfo ver2 = ver;
                                this.mOverlayConfig = this.mInitAppsHelper.initSystemApps(packageParser, packageSettings, userIds, startTime);
                                this.mInitAppsHelper.initNonSystemApps(packageParser, userIds, startTime);
                                packageParser.close();
                                this.mStorageManagerPackage = getStorageManagerPackageName(computer);
                                String setupWizardPackageNameImpl = getSetupWizardPackageNameImpl(computer);
                                this.mSetupWizardPackage = setupWizardPackageNameImpl;
                                this.mComponentResolver.fixProtectedFilterPriorities(setupWizardPackageNameImpl);
                                this.mDefaultTextClassifierPackage = ensureSystemPackageName(computer, this.mContext.getString(17040037));
                                this.mSystemTextClassifierPackageName = ensureSystemPackageName(computer, this.mContext.getString(17039946));
                                this.mConfiguratorPackage = ensureSystemPackageName(computer, this.mContext.getString(17039952));
                                this.mAppPredictionServicePackage = ensureSystemPackageName(computer, getPackageFromComponentString(17039918));
                                this.mIncidentReportApproverPackage = ensureSystemPackageName(computer, this.mContext.getString(17039988));
                                this.mRetailDemoPackage = getRetailDemoPackageName();
                                this.mOverlayConfigSignaturePackage = ensureSystemPackageName(computer, this.mInjector.getSystemConfig().getOverlayConfigSignaturePackage());
                                this.mRecentsPackage = ensureSystemPackageName(computer, getPackageFromComponentString(17040025));
                                this.mAmbientContextDetectionPackage = ensureSystemPackageName(computer, getPackageFromComponentString(17039917));
                                this.mSharedLibraries.updateAllSharedLibrariesLPw(null, null, Collections.unmodifiableMap(this.mPackages));
                                Iterator<SharedUserSetting> it2 = this.mSettings.getAllSharedUsersLPw().iterator();
                                while (it2.hasNext()) {
                                    SharedUserSetting setting = it2.next();
                                    PackageParser2 packageParser2 = packageParser;
                                    Iterator<SharedUserSetting> it3 = it2;
                                    List<String> changedAbiCodePath2 = ScanPackageUtils.applyAdjustedAbiToSharedUser(setting, null, this.mInjector.getAbiHelper().getAdjustedAbiForSharedUser(setting.getPackageStates(), null));
                                    if (changedAbiCodePath2 == null || changedAbiCodePath2.size() <= 0) {
                                        customResolverActivityName = customResolverActivityName3;
                                    } else {
                                        int i3 = changedAbiCodePath2.size() - 1;
                                        while (i3 >= 0) {
                                            String codePathString = changedAbiCodePath2.get(i3);
                                            try {
                                                changedAbiCodePath = changedAbiCodePath2;
                                                try {
                                                    customResolverActivityName2 = customResolverActivityName3;
                                                    try {
                                                        this.mInstaller.rmdex(codePathString, InstructionSets.getDexCodeInstructionSet(InstructionSets.getPreferredInstructionSet()));
                                                    } catch (Installer.InstallerException e2) {
                                                    }
                                                } catch (Installer.InstallerException e3) {
                                                    customResolverActivityName2 = customResolverActivityName3;
                                                }
                                            } catch (Installer.InstallerException e4) {
                                                changedAbiCodePath = changedAbiCodePath2;
                                                customResolverActivityName2 = customResolverActivityName3;
                                            }
                                            i3--;
                                            customResolverActivityName3 = customResolverActivityName2;
                                            changedAbiCodePath2 = changedAbiCodePath;
                                        }
                                        customResolverActivityName = customResolverActivityName3;
                                    }
                                    setting.fixSeInfoLocked();
                                    setting.updateProcesses();
                                    packageParser = packageParser2;
                                    it2 = it3;
                                    customResolverActivityName3 = customResolverActivityName;
                                }
                                this.mPackageUsage.read(packageSettings);
                                this.mCompilerStats.read();
                                try {
                                    EventLog.writeEvent((int) EventLogTags.BOOT_PROGRESS_PMS_SCAN_END, SystemClock.uptimeMillis());
                                    sMtkSystemServerIns.addBootEvent("Android:PMS_scan_END");
                                    String bootClassPath3 = bootClassPath2;
                                    Slog.i(TAG, "Time to scan packages: " + (((float) (SystemClock.uptimeMillis() - startTime)) / 1000.0f) + " seconds");
                                    ITranPackageManagerService.Instance().hookPMSScanEnd();
                                    if (this.mIsUpgrade) {
                                        Slog.i(TAG, "Build fingerprint changed from " + ver2.fingerprint + " to " + PackagePartitions.FINGERPRINT + "; regranting permissions for internal storage");
                                    }
                                    this.mPermissionManager.onStorageVolumeMounted(StorageManager.UUID_PRIVATE_INTERNAL, this.mIsUpgrade);
                                    ver2.sdkVersion = this.mSdkVersion;
                                    if (!this.mOnlyCore && (this.mPromoteSystemApps || this.mFirstBoot)) {
                                        for (UserInfo user : this.mInjector.getUserManagerInternal().getUsers(true)) {
                                            this.mSettings.applyDefaultPreferredAppsLPw(user.id);
                                        }
                                    }
                                    if (this.mIsUpgrade && !this.mOnlyCore) {
                                        Slog.i(TAG, "Build fingerprint changed; clearing code caches");
                                        int i4 = 0;
                                        while (i4 < packageSettings.size()) {
                                            PackageSetting ps2 = packageSettings.valueAt(i4);
                                            if (Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, ps2.getVolumeUuid())) {
                                                bootClassPath = bootClassPath3;
                                                this.mAppDataHelper.clearAppDataLIF(ps2.getPkg(), -1, 131111);
                                            } else {
                                                bootClassPath = bootClassPath3;
                                            }
                                            i4++;
                                            bootClassPath3 = bootClassPath;
                                        }
                                        ver2.fingerprint = PackagePartitions.FINGERPRINT;
                                    }
                                    this.mPrepareAppDataFuture = this.mAppDataHelper.fixAppsDataOnBoot();
                                    if (!this.mOnlyCore && this.mIsPreQUpgrade) {
                                        Slog.i(TAG, "Allowlisting all existing apps to hide their icons");
                                        int size = packageSettings.size();
                                        for (int i5 = 0; i5 < size; i5++) {
                                            PackageSetting ps3 = packageSettings.valueAt(i5);
                                            if ((ps3.getFlags() & 1) == 0) {
                                                ps3.disableComponentLPw(PackageManager.APP_DETAILS_ACTIVITY_CLASS_NAME, 0);
                                            }
                                        }
                                    }
                                    this.mPromoteSystemApps = false;
                                    ver2.databaseVersion = 3;
                                    t2.traceBegin("write settings");
                                    writeSettingsLPrTEMP();
                                    t2.traceEnd();
                                    EventLog.writeEvent((int) EventLogTags.BOOT_PROGRESS_PMS_READY, SystemClock.uptimeMillis());
                                    sMtkSystemServerIns.addBootEvent("Android:PMS_READY");
                                    if (this.mOnlyCore) {
                                        this.mRequiredVerifierPackage = null;
                                        this.mRequiredInstallerPackage = null;
                                        this.mRequiredUninstallerPackage = null;
                                        this.mServicesExtensionPackageName = null;
                                        this.mSharedSystemSharedLibraryPackageName = null;
                                    } else {
                                        this.mRequiredVerifierPackage = getRequiredButNotReallyRequiredVerifierLPr(computer);
                                        this.mRequiredInstallerPackage = getRequiredInstallerLPr(computer);
                                        this.mRequiredUninstallerPackage = getRequiredUninstallerLPr(computer);
                                        ComponentName intentFilterVerifierComponent = getIntentFilterVerifierComponentNameLPr(computer);
                                        ComponentName domainVerificationAgent = getDomainVerificationAgentComponentNameLPr(computer);
                                        Context context2 = this.mContext;
                                        DomainVerificationManagerInternal domainVerificationManagerInternal2 = this.mDomainVerificationManager;
                                        DomainVerificationProxy domainVerificationProxy = DomainVerificationProxy.makeProxy(intentFilterVerifierComponent, domainVerificationAgent, context2, domainVerificationManagerInternal2, domainVerificationManagerInternal2.getCollector(), this.mDomainVerificationConnection);
                                        this.mDomainVerificationManager.setProxy(domainVerificationProxy);
                                        this.mServicesExtensionPackageName = getRequiredServicesExtensionPackageLPr(computer);
                                        this.mSharedSystemSharedLibraryPackageName = getRequiredSharedLibrary(computer, "android.ext.shared", -1);
                                    }
                                    String requiredPermissionControllerLPr = getRequiredPermissionControllerLPr(computer);
                                    this.mRequiredPermissionControllerPackage = requiredPermissionControllerLPr;
                                    t2 = t2;
                                    this.mSettings.setPermissionControllerVersion(computer.getPackageInfo(requiredPermissionControllerLPr, 0L, 0).getLongVersionCode());
                                    this.mRequiredSdkSandboxPackage = getRequiredSdkSandboxPackageName(computer);
                                    Iterator<AndroidPackage> it4 = this.mPackages.values().iterator();
                                    while (it4.hasNext()) {
                                        AndroidPackage pkg = it4.next();
                                        if (!pkg.isSystem()) {
                                            int length = userIds.length;
                                            int i6 = 0;
                                            while (i6 < length) {
                                                int userId = userIds[i6];
                                                PackageStateInternal ps4 = computer.getPackageStateInternal(pkg.getPackageName());
                                                if (ps4 == null || !ps4.getUserStateOrDefault(userId).isInstantApp()) {
                                                    it = it4;
                                                } else if (ps4.getUserStateOrDefault(userId).isInstalled()) {
                                                    it = it4;
                                                    this.mInstantAppRegistry.addInstantApp(userId, ps4.getAppId());
                                                } else {
                                                    it = it4;
                                                }
                                                i6++;
                                                it4 = it;
                                            }
                                        }
                                    }
                                    this.mInstallerService = this.mInjector.getPackageInstallerService();
                                    ComponentName instantAppResolverComponent = getInstantAppResolver(computer);
                                    if (instantAppResolverComponent != null) {
                                        if (DEBUG_INSTANT) {
                                            Slog.d(TAG, "Set ephemeral resolver: " + instantAppResolverComponent);
                                        }
                                        this.mInstantAppResolverConnection = this.mInjector.getInstantAppResolverConnection(instantAppResolverComponent);
                                        this.mInstantAppResolverSettingsComponent = getInstantAppResolverSettingsLPr(computer, instantAppResolverComponent);
                                        str = null;
                                    } else {
                                        str = null;
                                        this.mInstantAppResolverConnection = null;
                                        this.mInstantAppResolverSettingsComponent = null;
                                    }
                                    updateInstantAppInstallerLocked(str);
                                    Map<Integer, List<PackageInfo>> userPackages = new HashMap<>();
                                    int length2 = userIds.length;
                                    int i7 = 0;
                                    while (i7 < length2) {
                                        int userId2 = userIds[i7];
                                        Settings.VersionInfo ver3 = ver2;
                                        int i8 = length2;
                                        userPackages.put(Integer.valueOf(userId2), computer.getInstalledPackages(0L, userId2).getList());
                                        i7++;
                                        ver2 = ver3;
                                        length2 = i8;
                                    }
                                    this.mDexManager.load(userPackages);
                                    if (this.mIsUpgrade) {
                                        FrameworkStatsLog.write(239, 13, SystemClock.uptimeMillis() - startTime);
                                    }
                                    this.mLiveComputer = createLiveComputer();
                                    this.mModuleInfoProvider = this.mInjector.getModuleInfoProvider();
                                    this.mInjector.getSystemWrapper().enablePackageCaches();
                                    t2.traceBegin("GC");
                                    VMRuntime.getRuntime().requestConcurrentGC();
                                    t2.traceEnd();
                                    this.mInstaller.setWarnIfHeld(this.mLock);
                                    ParsingPackageUtils.readConfigUseRoundIcon(this.mContext.getResources());
                                    this.mServiceStartWithDelay = SystemClock.uptimeMillis() + 60000;
                                    Slog.i(TAG, "Fix for b/169414761 is applied");
                                    sPmsExt.initAfterScan(this.mSettings.mPackages);
                                    ITranPackageManagerService.Instance().addToRemovableSystemAppSet(sPmsExt, isDeviceUpgrading());
                                } catch (Throwable th6) {
                                    th = th6;
                                    while (true) {
                                        break;
                                        break;
                                    }
                                    throw th;
                                }
                            } catch (Throwable th7) {
                                th = th7;
                            }
                        } catch (Throwable th8) {
                            th = th8;
                        }
                    }
                } catch (Throwable th9) {
                    th = th9;
                    throw th;
                }
            } catch (Throwable th10) {
                th = th10;
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$39$com-android-server-pm-PackageManagerService  reason: not valid java name */
    public /* synthetic */ ResolveInfo m5557lambda$new$39$comandroidserverpmPackageManagerService() {
        return this.mResolveInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$40$com-android-server-pm-PackageManagerService  reason: not valid java name */
    public /* synthetic */ ActivityInfo m5558lambda$new$40$comandroidserverpmPackageManagerService() {
        return this.mInstantAppInstallerActivity;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateInstantAppInstallerLocked(String modifiedPackage) {
        ActivityInfo activityInfo = this.mInstantAppInstallerActivity;
        if (activityInfo != null && !activityInfo.getComponentName().getPackageName().equals(modifiedPackage)) {
            return;
        }
        setUpInstantAppInstallerActivityLP(getInstantAppInstallerLPr());
    }

    public boolean isFirstBoot() {
        return this.mFirstBoot;
    }

    public boolean isOnlyCoreApps() {
        return this.mOnlyCore;
    }

    public boolean isDeviceUpgrading() {
        return this.mIsUpgrade || SystemProperties.getBoolean("persist.pm.mock-upgrade", false);
    }

    private String getRequiredButNotReallyRequiredVerifierLPr(Computer computer) {
        Intent intent = new Intent("android.intent.action.PACKAGE_NEEDS_VERIFICATION");
        List<ResolveInfo> matches = this.mResolveIntentHelper.queryIntentReceiversInternal(computer, intent, PACKAGE_MIME_TYPE, 1835008L, 0, Binder.getCallingUid());
        if (matches.size() == 1) {
            return matches.get(0).getComponentInfo().packageName;
        }
        if (matches.size() == 0) {
            Log.w(TAG, "There should probably be a verifier, but, none were found");
            return null;
        }
        throw new RuntimeException("There must be exactly one verifier; found " + matches);
    }

    private String getRequiredSharedLibrary(Computer snapshot, String name, int version) {
        SharedLibraryInfo libraryInfo = snapshot.getSharedLibraryInfo(name, version);
        if (libraryInfo == null) {
            throw new IllegalStateException("Missing required shared library:" + name);
        }
        String packageName = libraryInfo.getPackageName();
        if (packageName == null) {
            throw new IllegalStateException("Expected a package for shared library " + name);
        }
        return packageName;
    }

    private String getRequiredServicesExtensionPackageLPr(Computer computer) {
        String servicesExtensionPackage = ensureSystemPackageName(computer, this.mContext.getString(17040037));
        if (TextUtils.isEmpty(servicesExtensionPackage)) {
            throw new RuntimeException("Required services extension package is missing, check config_servicesExtensionPackage.");
        }
        return servicesExtensionPackage;
    }

    private String getRequiredInstallerLPr(Computer computer) {
        Intent intent = new Intent("android.intent.action.INSTALL_PACKAGE");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(Uri.parse("content://com.example/foo.apk"), PACKAGE_MIME_TYPE);
        List<ResolveInfo> matches = computer.queryIntentActivitiesInternal(intent, PACKAGE_MIME_TYPE, 1835008L, 0);
        if (matches.size() == 1) {
            ResolveInfo resolveInfo = matches.get(0);
            if (!resolveInfo.activityInfo.applicationInfo.isPrivilegedApp()) {
                throw new RuntimeException("The installer must be a privileged app");
            }
            return matches.get(0).getComponentInfo().packageName;
        }
        throw new RuntimeException("There must be exactly one installer; found " + matches);
    }

    private String getRequiredUninstallerLPr(Computer computer) {
        Intent intent = new Intent("android.intent.action.UNINSTALL_PACKAGE");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setData(Uri.fromParts("package", "foo.bar", null));
        ResolveInfo resolveInfo = this.mResolveIntentHelper.resolveIntentInternal(computer, intent, null, 1835008L, 0L, 0, false, Binder.getCallingUid());
        if (resolveInfo == null || this.mResolveActivity.name.equals(resolveInfo.getComponentInfo().name)) {
            throw new RuntimeException("There must be exactly one uninstaller; found " + resolveInfo);
        }
        return resolveInfo.getComponentInfo().packageName;
    }

    private String getRequiredPermissionControllerLPr(Computer computer) {
        Intent intent = new Intent("android.intent.action.MANAGE_PERMISSIONS");
        intent.addCategory("android.intent.category.DEFAULT");
        List<ResolveInfo> matches = computer.queryIntentActivitiesInternal(intent, null, 1835008L, 0);
        if (matches.size() == 1) {
            ResolveInfo resolveInfo = matches.get(0);
            if (!resolveInfo.activityInfo.applicationInfo.isPrivilegedApp()) {
                throw new RuntimeException("The permissions manager must be a privileged app");
            }
            return matches.get(0).getComponentInfo().packageName;
        }
        throw new RuntimeException("There must be exactly one permissions manager; found " + matches);
    }

    private ComponentName getIntentFilterVerifierComponentNameLPr(Computer computer) {
        Intent intent = new Intent("android.intent.action.INTENT_FILTER_NEEDS_VERIFICATION");
        List<ResolveInfo> matches = this.mResolveIntentHelper.queryIntentReceiversInternal(computer, intent, PACKAGE_MIME_TYPE, 1835008L, 0, Binder.getCallingUid());
        ResolveInfo best = null;
        int N = matches.size();
        for (int i = 0; i < N; i++) {
            ResolveInfo cur = matches.get(i);
            String packageName = cur.getComponentInfo().packageName;
            if (checkPermission("android.permission.INTENT_FILTER_VERIFICATION_AGENT", packageName, 0) == 0 && (best == null || cur.priority > best.priority)) {
                best = cur;
            }
        }
        if (best != null) {
            return best.getComponentInfo().getComponentName();
        }
        Slog.w(TAG, "Intent filter verifier not found");
        return null;
    }

    private ComponentName getDomainVerificationAgentComponentNameLPr(Computer computer) {
        Intent intent = new Intent("android.intent.action.DOMAINS_NEED_VERIFICATION");
        List<ResolveInfo> matches = this.mResolveIntentHelper.queryIntentReceiversInternal(computer, intent, null, 1835008L, 0, Binder.getCallingUid());
        ResolveInfo best = null;
        int N = matches.size();
        for (int i = 0; i < N; i++) {
            ResolveInfo cur = matches.get(i);
            String packageName = cur.getComponentInfo().packageName;
            if (checkPermission("android.permission.DOMAIN_VERIFICATION_AGENT", packageName, 0) != 0) {
                Slog.w(TAG, "Domain verification agent found but does not hold permission: " + packageName);
            } else if (best == null || cur.priority > best.priority) {
                if (computer.isComponentEffectivelyEnabled(cur.getComponentInfo(), 0)) {
                    best = cur;
                } else {
                    Slog.w(TAG, "Domain verification agent found but not enabled");
                }
            }
        }
        if (best != null) {
            return best.getComponentInfo().getComponentName();
        }
        Slog.w(TAG, "Domain verification agent not found");
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComponentName getInstantAppResolver(Computer snapshot) {
        String[] packageArray = this.mContext.getResources().getStringArray(17236060);
        if (packageArray.length == 0 && !Build.IS_DEBUGGABLE) {
            if (DEBUG_INSTANT) {
                Slog.d(TAG, "Ephemeral resolver NOT found; empty package list");
            }
            return null;
        }
        int callingUid = Binder.getCallingUid();
        int resolveFlags = (!Build.IS_DEBUGGABLE ? 1048576 : 0) | 786432;
        Intent resolverIntent = new Intent("android.intent.action.RESOLVE_INSTANT_APP_PACKAGE");
        List<ResolveInfo> resolvers = snapshot.queryIntentServicesInternal(resolverIntent, null, resolveFlags, 0, callingUid, false);
        int N = resolvers.size();
        if (N == 0) {
            if (DEBUG_INSTANT) {
                Slog.d(TAG, "Ephemeral resolver NOT found; no matching intent filters");
            }
            return null;
        }
        Set<String> possiblePackages = new ArraySet<>(Arrays.asList(packageArray));
        for (int i = 0; i < N; i++) {
            ResolveInfo info = resolvers.get(i);
            if (info.serviceInfo != null) {
                String packageName = info.serviceInfo.packageName;
                if (!possiblePackages.contains(packageName) && !Build.IS_DEBUGGABLE) {
                    if (DEBUG_INSTANT) {
                        Slog.d(TAG, "Ephemeral resolver not in allowed package list; pkg: " + packageName + ", info:" + info);
                    }
                } else {
                    if (DEBUG_INSTANT) {
                        Slog.v(TAG, "Ephemeral resolver found; pkg: " + packageName + ", info:" + info);
                    }
                    return new ComponentName(packageName, info.serviceInfo.name);
                }
            }
        }
        if (DEBUG_INSTANT) {
            Slog.v(TAG, "Ephemeral resolver NOT found");
        }
        return null;
    }

    private ActivityInfo getInstantAppInstallerLPr() {
        String[] orderedActions;
        boolean z = this.mIsEngBuild;
        if (z) {
            orderedActions = new String[]{"android.intent.action.INSTALL_INSTANT_APP_PACKAGE_TEST", "android.intent.action.INSTALL_INSTANT_APP_PACKAGE"};
        } else {
            orderedActions = new String[]{"android.intent.action.INSTALL_INSTANT_APP_PACKAGE"};
        }
        int resolveFlags = (z ? 0 : 1048576) | 786944;
        Computer computer = snapshotComputer();
        Intent intent = new Intent();
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(Uri.fromFile(new File("foo.apk")), PACKAGE_MIME_TYPE);
        List<ResolveInfo> matches = null;
        for (String action : orderedActions) {
            intent.setAction(action);
            matches = computer.queryIntentActivitiesInternal(intent, PACKAGE_MIME_TYPE, resolveFlags, 0);
            if (!matches.isEmpty()) {
                break;
            }
            if (DEBUG_INSTANT) {
                Slog.d(TAG, "Instant App installer not found with " + action);
            }
        }
        Iterator<ResolveInfo> iter = matches.iterator();
        while (iter.hasNext()) {
            ResolveInfo rInfo = iter.next();
            if (checkPermission("android.permission.INSTALL_PACKAGES", rInfo.activityInfo.packageName, 0) != 0 && !this.mIsEngBuild) {
                iter.remove();
            }
        }
        if (matches.size() == 0) {
            return null;
        }
        if (matches.size() == 1) {
            return (ActivityInfo) matches.get(0).getComponentInfo();
        }
        throw new RuntimeException("There must be at most one ephemeral installer; found " + matches);
    }

    private ComponentName getInstantAppResolverSettingsLPr(Computer computer, ComponentName resolver) {
        Intent intent = new Intent("android.intent.action.INSTANT_APP_RESOLVER_SETTINGS").addCategory("android.intent.category.DEFAULT").setPackage(resolver.getPackageName());
        List<ResolveInfo> matches = computer.queryIntentActivitiesInternal(intent, null, 786432L, 0);
        if (matches.isEmpty()) {
            return null;
        }
        return matches.get(0).getComponentInfo().getComponentName();
    }

    public PermissionGroupInfo getPermissionGroupInfo(String groupName, int flags) {
        return ((PermissionManager) this.mContext.getSystemService(PermissionManager.class)).getPermissionGroupInfo(groupName, flags);
    }

    public void freeAllAppCacheAboveQuota(String volumeUuid) throws IOException {
        synchronized (this.mInstallLock) {
            try {
                this.mInstaller.freeCache(volumeUuid, JobStatus.NO_LATEST_RUNTIME, 2304);
            } catch (Installer.InstallerException e) {
            }
        }
    }

    public void freeStorage(String volumeUuid, long bytes, int flags) throws IOException {
        Computer computer;
        long j;
        StorageManager storage = (StorageManager) this.mInjector.getSystemService(StorageManager.class);
        File file = storage.findPathForUuid(volumeUuid);
        if (file.getUsableSpace() >= bytes) {
            return;
        }
        if (this.mEnableFreeCacheV2) {
            boolean internalVolume = Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, volumeUuid);
            boolean aggressive = (flags & 1) != 0;
            if (internalVolume && (aggressive || SystemProperties.getBoolean("persist.sys.preloads.file_cache_expired", false))) {
                deletePreloadsFileCache();
                if (file.getUsableSpace() >= bytes) {
                    return;
                }
            }
            if (internalVolume && aggressive) {
                FileUtils.deleteContents(this.mCacheDir);
                if (file.getUsableSpace() >= bytes) {
                    return;
                }
            }
            synchronized (this.mInstallLock) {
                try {
                    this.mInstaller.freeCache(volumeUuid, bytes, 256);
                } catch (Installer.InstallerException e) {
                } catch (Throwable th) {
                    th = th;
                    while (true) {
                        try {
                            break;
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    throw th;
                }
                try {
                    if (file.getUsableSpace() >= bytes) {
                        return;
                    }
                    Computer computer2 = snapshotComputer();
                    if (internalVolume && this.mSharedLibraries.pruneUnusedStaticSharedLibraries(computer2, bytes, Settings.Global.getLong(this.mContext.getContentResolver(), "unused_static_shared_lib_min_cache_period", FREE_STORAGE_UNUSED_STATIC_SHARED_LIB_MIN_CACHE_PERIOD))) {
                        return;
                    }
                    if (internalVolume) {
                        computer = computer2;
                        j = 604800000;
                        if (this.mInstantAppRegistry.pruneInstalledInstantApps(computer2, bytes, Settings.Global.getLong(this.mContext.getContentResolver(), "installed_instant_app_min_cache_period", UnixCalendar.WEEK_IN_MILLIS))) {
                            return;
                        }
                    } else {
                        computer = computer2;
                        j = 604800000;
                    }
                    synchronized (this.mInstallLock) {
                        try {
                            this.mInstaller.freeCache(volumeUuid, bytes, 768);
                        } catch (Installer.InstallerException e2) {
                        }
                    }
                    if (file.getUsableSpace() >= bytes) {
                        return;
                    }
                    if (internalVolume) {
                        if (this.mInstantAppRegistry.pruneUninstalledInstantApps(computer, bytes, Settings.Global.getLong(this.mContext.getContentResolver(), "uninstalled_instant_app_min_cache_period", j))) {
                            return;
                        }
                    }
                    StorageManagerInternal smInternal = (StorageManagerInternal) this.mInjector.getLocalService(StorageManagerInternal.class);
                    long freeBytesRequired = bytes - file.getUsableSpace();
                    if (freeBytesRequired > 0) {
                        smInternal.freeCache(volumeUuid, freeBytesRequired);
                    }
                    this.mInstallerService.freeStageDirs(volumeUuid);
                } catch (Throwable th3) {
                    th = th3;
                    while (true) {
                        break;
                        break;
                    }
                    throw th;
                }
            }
        } else {
            synchronized (this.mInstallLock) {
                try {
                    this.mInstaller.freeCache(volumeUuid, bytes, 0);
                } catch (Installer.InstallerException e3) {
                }
            }
        }
        if (file.getUsableSpace() < bytes) {
            throw new IOException("Failed to free " + bytes + " on storage device at " + file);
        }
    }

    public static String deriveCodePathName(String codePath) {
        if (codePath == null) {
            return null;
        }
        File codeFile = new File(codePath);
        String name = codeFile.getName();
        if (codeFile.isDirectory()) {
            return name;
        }
        if (name.endsWith(".apk") || name.endsWith(".tmp")) {
            int lastDot = name.lastIndexOf(46);
            return name.substring(0, lastDot);
        }
        Slog.w(TAG, "Odd, " + codePath + " doesn't look like an APK");
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int freeCacheForInstallation(int recommendedInstallLocation, PackageLite pkgLite, String resolvedPath, String mPackageAbiOverride, int installFlags) {
        StorageManager storage = StorageManager.from(this.mContext);
        long lowThreshold = storage.getStorageLowBytes(Environment.getDataDirectory());
        long sizeBytes = PackageManagerServiceUtils.calculateInstalledSize(resolvedPath, mPackageAbiOverride);
        if (sizeBytes >= 0) {
            synchronized (this.mInstallLock) {
                try {
                    try {
                        this.mInstaller.freeCache(null, sizeBytes + lowThreshold, 0);
                        try {
                            PackageInfoLite pkgInfoLite = PackageManagerServiceUtils.getMinimalPackageInfo(this.mContext, pkgLite, resolvedPath, installFlags, mPackageAbiOverride);
                            if (pkgInfoLite.recommendedInstallLocation == -6) {
                                pkgInfoLite.recommendedInstallLocation = -1;
                            }
                            return pkgInfoLite.recommendedInstallLocation;
                        } catch (Installer.InstallerException e) {
                            e = e;
                            Slog.w(TAG, "Failed to free cache", e);
                            return recommendedInstallLocation;
                        }
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } catch (Installer.InstallerException e2) {
                    e = e2;
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        }
        return recommendedInstallLocation;
    }

    public ModuleInfo getModuleInfo(String packageName, int flags) {
        return this.mModuleInfoProvider.getModuleInfo(packageName, flags);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateSequenceNumberLP(PackageSetting pkgSetting, int[] userList) {
        this.mChangedPackagesTracker.updateSequenceNumber(pkgSetting.getPackageName(), userList);
    }

    public boolean hasSystemFeature(String name, int version) {
        synchronized (this.mAvailableFeatures) {
            FeatureInfo feat = this.mAvailableFeatures.get(name);
            if (feat == null) {
                return false;
            }
            return feat.version >= version;
        }
    }

    public int checkPermission(String permName, String pkgName, int userId) {
        return this.mPermissionManager.checkPermission(pkgName, permName, userId);
    }

    public String getSdkSandboxPackageName() {
        return this.mRequiredSdkSandboxPackage;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getPackageInstallerPackageName() {
        return this.mRequiredInstallerPackage;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void requestInstantAppResolutionPhaseTwo(AuxiliaryResolveInfo responseObj, Intent origIntent, String resolvedType, String callingPackage, String callingFeatureId, boolean isRequesterInstantApp, Bundle verificationBundle, int userId) {
        Message msg = this.mHandler.obtainMessage(20, new InstantAppRequest(responseObj, origIntent, resolvedType, callingPackage, callingFeatureId, isRequesterInstantApp, userId, verificationBundle, false, responseObj.hostDigestPrefixSecure, responseObj.token));
        this.mHandler.sendMessage(msg);
    }

    public ParceledListSlice<ResolveInfo> queryIntentReceivers(Computer snapshot, Intent intent, String resolvedType, long flags, int userId) {
        return new ParceledListSlice<>(this.mResolveIntentHelper.queryIntentReceiversInternal(snapshot, intent, resolvedType, flags, userId, Binder.getCallingUid()));
    }

    public static void reportSettingsProblem(int priority, String msg) {
        PackageManagerServiceUtils.logCriticalInfo(priority, msg);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void renameStaticSharedLibraryPackage(ParsedPackage parsedPackage) {
        parsedPackage.setPackageName(toStaticSharedLibraryPackageName(parsedPackage.getPackageName(), parsedPackage.getStaticSharedLibVersion()));
    }

    private static String toStaticSharedLibraryPackageName(String packageName, long libraryVersion) {
        return packageName + STATIC_SHARED_LIB_DELIMITER + libraryVersion;
    }

    public void performFstrimIfNeeded() {
        PackageManagerServiceUtils.enforceSystemOrRoot("Only the system can request fstrim");
        try {
            IStorageManager sm = InstallLocationUtils.getStorageManager();
            if (sm == null) {
                Slog.e(TAG, "storageManager service unavailable!");
                return;
            }
            boolean doTrim = false;
            long interval = Settings.Global.getLong(this.mContext.getContentResolver(), "fstrim_mandatory_interval", DEFAULT_MANDATORY_FSTRIM_INTERVAL);
            if (interval > 0) {
                long timeSinceLast = System.currentTimeMillis() - sm.lastMaintenance();
                if (timeSinceLast > interval) {
                    doTrim = true;
                    Slog.w(TAG, "No disk maintenance in " + timeSinceLast + "; running immediately");
                }
            }
            if (doTrim) {
                if (!isFirstBoot() && this.mDexOptHelper.isDexOptDialogShown()) {
                    try {
                        ActivityManager.getService().showBootMessage(this.mContext.getResources().getString(17039668), true);
                    } catch (RemoteException e) {
                    }
                }
                sm.runMaintenance();
            }
        } catch (RemoteException e2) {
        }
    }

    public void updatePackagesIfNeeded() {
        this.mDexOptHelper.performPackageDexOptUpgradeIfNeeded();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyPackageUseInternal(String packageName, int reason) {
        long time = System.currentTimeMillis();
        synchronized (this.mLock) {
            PackageSetting pkgSetting = this.mSettings.getPackageLPr(packageName);
            if (pkgSetting == null) {
                return;
            }
            pkgSetting.getPkgState().setLastPackageUsageTimeInMills(reason, time);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DexManager getDexManager() {
        return this.mDexManager;
    }

    public void shutdown() {
        this.mCompilerStats.writeNow();
        this.mDexManager.writePackageDexUsageNow();
        PackageWatchdog.getInstance(this.mContext).writeNow();
        CtaManagerFactory.getInstance().makeCtaManager().shutdown();
        synchronized (this.mLock) {
            this.mPackageUsage.writeNow(this.mSettings.getPackagesLocked());
            if (this.mHandler.hasMessages(13) || this.mHandler.hasMessages(14) || this.mHandler.hasMessages(19)) {
                writeSettings();
            }
        }
        IPackageManagerServiceLice.Instance().onShutdown();
        removeAppPrivateVolume();
    }

    private void removeAppPrivateVolume() {
        StorageManager storage = (StorageManager) this.mInjector.getSystemService(StorageManager.class);
        for (VolumeInfo vol : storage.getWritablePrivateVolumes()) {
            Slog.i(TAG, "removeAppPrivateVolume remove internal storage in data_mirror " + vol.getFsUuid());
            try {
                this.mInstaller.onPrivateVolumeRemoved(vol.getFsUuid());
            } catch (Installer.InstallerException e) {
                Slog.i(TAG, "Failed when private volume " + vol, e);
            }
        }
        Slog.i(TAG, "removeAppPrivateVolume remove data_mirror complete");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int[] resolveUserIds(int userId) {
        return userId == -1 ? this.mUserManager.getUserIds() : new int[]{userId};
    }

    private void setUpInstantAppInstallerActivityLP(ActivityInfo installerActivity) {
        if (installerActivity == null) {
            if (DEBUG_INSTANT) {
                Slog.d(TAG, "Clear ephemeral installer activity");
            }
            this.mInstantAppInstallerActivity = null;
            onChanged();
            return;
        }
        if (DEBUG_INSTANT) {
            Slog.d(TAG, "Set ephemeral installer activity: " + installerActivity.getComponentName());
        }
        this.mInstantAppInstallerActivity = installerActivity;
        installerActivity.flags |= 288;
        this.mInstantAppInstallerActivity.exported = true;
        this.mInstantAppInstallerActivity.enabled = true;
        this.mInstantAppInstallerInfo.activityInfo = this.mInstantAppInstallerActivity;
        this.mInstantAppInstallerInfo.priority = 1;
        this.mInstantAppInstallerInfo.preferredOrder = 1;
        this.mInstantAppInstallerInfo.isDefault = true;
        this.mInstantAppInstallerInfo.match = 5799936;
        onChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void killApplication(String pkgName, int appId, String reason) {
        killApplication(pkgName, appId, -1, reason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void killApplication(String pkgName, int appId, int userId, String reason) {
        long token = Binder.clearCallingIdentity();
        try {
            IActivityManager am = ActivityManager.getService();
            if (am != null) {
                try {
                    am.killApplication(pkgName, appId, userId, reason);
                } catch (RemoteException e) {
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendPackageBroadcast$41$com-android-server-pm-PackageManagerService  reason: not valid java name */
    public /* synthetic */ void m5569xb1c279db(String action, String pkg, Bundle extras, int flags, String targetPkg, IIntentReceiver finishedReceiver, int[] userIds, int[] instantUserIds, SparseArray broadcastAllowList, Bundle bOptions) {
        this.mBroadcastHelper.sendPackageBroadcast(action, pkg, extras, flags, targetPkg, finishedReceiver, userIds, instantUserIds, broadcastAllowList, bOptions);
    }

    @Override // com.android.server.pm.PackageSender
    public void sendPackageBroadcast(final String action, final String pkg, final Bundle extras, final int flags, final String targetPkg, final IIntentReceiver finishedReceiver, final int[] userIds, final int[] instantUserIds, final SparseArray<int[]> broadcastAllowList, final Bundle bOptions) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda60
            @Override // java.lang.Runnable
            public final void run() {
                PackageManagerService.this.m5569xb1c279db(action, pkg, extras, flags, targetPkg, finishedReceiver, userIds, instantUserIds, broadcastAllowList, bOptions);
            }
        });
    }

    @Override // com.android.server.pm.PackageSender
    public void notifyPackageAdded(String packageName, int uid) {
        this.mPackageObserverHelper.notifyAdded(packageName, uid);
    }

    @Override // com.android.server.pm.PackageSender
    public void notifyPackageChanged(String packageName, int uid) {
        this.mPackageObserverHelper.notifyChanged(packageName, uid);
    }

    @Override // com.android.server.pm.PackageSender
    public void notifyPackageRemoved(String packageName, int uid) {
        this.mPackageObserverHelper.notifyRemoved(packageName, uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendPackageAddedForUser(Computer snapshot, String packageName, PackageStateInternal packageState, int userId, int dataLoaderType) {
        PackageUserStateInternal userState = packageState.getUserStateOrDefault(userId);
        boolean isSystem = packageState.isSystem();
        boolean isInstantApp = userState.isInstantApp();
        int[] userIds = isInstantApp ? EMPTY_INT_ARRAY : new int[]{userId};
        int[] instantUserIds = isInstantApp ? new int[]{userId} : EMPTY_INT_ARRAY;
        sendPackageAddedForNewUsers(snapshot, packageName, isSystem, false, packageState.getAppId(), userIds, instantUserIds, dataLoaderType);
        PackageInstaller.SessionInfo info = new PackageInstaller.SessionInfo();
        info.installReason = userState.getInstallReason();
        info.appPackageName = packageName;
        sendSessionCommitBroadcast(info, userId);
        sPmsExt.onPackageAdded(packageName, null, userId);
    }

    @Override // com.android.server.pm.PackageSender
    public void sendPackageAddedForNewUsers(Computer snapshot, final String packageName, boolean sendBootCompleted, final boolean includeStopped, final int appId, final int[] userIds, final int[] instantUserIds, final int dataLoaderType) {
        if (ArrayUtils.isEmpty(userIds) && ArrayUtils.isEmpty(instantUserIds)) {
            return;
        }
        final SparseArray<int[]> broadcastAllowList = this.mAppsFilter.getVisibilityAllowList(snapshot, snapshot.getPackageStateInternal(packageName, 1000), userIds, snapshot.getPackageStates());
        this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                PackageManagerService.this.m5567x861d5e2a(packageName, appId, userIds, instantUserIds, dataLoaderType, broadcastAllowList);
            }
        });
        if (sendBootCompleted && !ArrayUtils.isEmpty(userIds)) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda12
                @Override // java.lang.Runnable
                public final void run() {
                    PackageManagerService.this.m5568x20be20ab(userIds, packageName, includeStopped);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendPackageAddedForNewUsers$42$com-android-server-pm-PackageManagerService  reason: not valid java name */
    public /* synthetic */ void m5567x861d5e2a(String packageName, int appId, int[] userIds, int[] instantUserIds, int dataLoaderType, SparseArray broadcastAllowList) {
        this.mBroadcastHelper.sendPackageAddedForNewUsers(packageName, appId, userIds, instantUserIds, dataLoaderType, broadcastAllowList);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendPackageAddedForNewUsers$43$com-android-server-pm-PackageManagerService  reason: not valid java name */
    public /* synthetic */ void m5568x20be20ab(int[] userIds, String packageName, boolean includeStopped) {
        for (int userId : userIds) {
            this.mBroadcastHelper.sendBootCompletedBroadcastToSystemApp(packageName, includeStopped, userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendApplicationHiddenForUser(String packageName, PackageStateInternal packageState, int userId) {
        PackageRemovedInfo info = new PackageRemovedInfo(this);
        info.mRemovedPackage = packageName;
        info.mInstallerPackageName = packageState.getInstallSource().installerPackageName;
        info.mRemovedUsers = new int[]{userId};
        info.mBroadcastUsers = new int[]{userId};
        info.mUid = UserHandle.getUid(userId, packageState.getAppId());
        info.sendPackageRemovedBroadcasts(true, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isUserRestricted(int userId, String restrictionKey) {
        Bundle restrictions = this.mUserManager.getUserRestrictions(userId);
        if (!restrictions.getBoolean(restrictionKey, false)) {
            return false;
        }
        Log.w(TAG, "User is restricted: " + restrictionKey);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enforceCanSetPackagesSuspendedAsUser(Computer snapshot, String callingPackage, int callingUid, int userId, String callingMethod) {
        if (callingUid == 0 || UserHandle.getAppId(callingUid) == 1000) {
            return;
        }
        String ownerPackage = this.mProtectedPackages.getDeviceOwnerOrProfileOwnerPackage(userId);
        if (ownerPackage != null) {
            int ownerUid = snapshot.getPackageUid(ownerPackage, 0L, userId);
            if (ownerUid == callingUid) {
                return;
            }
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.SUSPEND_APPS", callingMethod);
        int packageUid = snapshot.getPackageUid(callingPackage, 0L, userId);
        boolean allowedShell = true;
        boolean allowedPackageUid = packageUid == callingUid;
        if (callingUid != 2000 || !UserHandle.isSameApp(packageUid, callingUid)) {
            allowedShell = false;
        }
        if (!allowedShell && !allowedPackageUid) {
            throw new SecurityException("Calling package " + callingPackage + " in user " + userId + " does not belong to calling uid " + callingUid);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unsuspendForSuspendingPackage(Computer computer, final String suspendingPackage, int userId) {
        String[] allPackages = (String[]) computer.getPackageStates().keySet().toArray(new String[0]);
        SuspendPackageHelper suspendPackageHelper = this.mSuspendPackageHelper;
        Objects.requireNonNull(suspendingPackage);
        suspendPackageHelper.removeSuspensionsBySuspendingPackage(computer, allPackages, new Predicate() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda14
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return suspendingPackage.equals((String) obj);
            }
        }, userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeAllDistractingPackageRestrictions(Computer snapshot, int userId) {
        String[] allPackages = snapshot.getAllAvailablePackageNames();
        this.mDistractingPackageHelper.removeDistractingPackageRestrictions(snapshot, allPackages, userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enforceCanSetDistractingPackageRestrictionsAsUser(int callingUid, int userId, String callingMethod) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SUSPEND_APPS", callingMethod);
        if (callingUid != 0 && callingUid != 1000 && UserHandle.getUserId(callingUid) != userId) {
            throw new SecurityException("Calling uid " + callingUid + " cannot call for user " + userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setEnableRollbackCode(int token, int enableRollbackCode) {
        Message msg = this.mHandler.obtainMessage(21);
        msg.arg1 = token;
        msg.arg2 = enableRollbackCode;
        this.mHandler.sendMessage(msg);
    }

    void notifyFirstLaunch(final String packageName, final String installerPackage, final int userId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                PackageManagerService.this.m5559xa40da761(packageName, userId, installerPackage);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyFirstLaunch$44$com-android-server-pm-PackageManagerService  reason: not valid java name */
    public /* synthetic */ void m5559xa40da761(String packageName, int userId, String installerPackage) {
        for (int i = 0; i < this.mRunningInstalls.size(); i++) {
            PostInstallData data = this.mRunningInstalls.valueAt(i);
            if (data.res.mReturnCode == 1 && packageName.equals(data.res.mPkg.getPackageName())) {
                for (int uIndex = 0; uIndex < data.res.mNewUsers.length; uIndex++) {
                    if (userId == data.res.mNewUsers[uIndex]) {
                        if (DEBUG_BACKUP) {
                            Slog.i(TAG, "Package " + packageName + " being restored so deferring FIRST_LAUNCH");
                            return;
                        } else {
                            return;
                        }
                    }
                }
                continue;
            }
        }
        if (DEBUG_BACKUP) {
            Slog.i(TAG, "Package " + packageName + " sending normal FIRST_LAUNCH");
        }
        boolean isInstantApp = snapshotComputer().isInstantAppInternal(packageName, userId, 1000);
        int[] userIds = isInstantApp ? EMPTY_INT_ARRAY : new int[]{userId};
        int[] instantUserIds = isInstantApp ? new int[]{userId} : EMPTY_INT_ARRAY;
        this.mBroadcastHelper.sendFirstLaunchBroadcast(packageName, installerPackage, userIds, instantUserIds);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyPackageChangeObservers(PackageChangeEvent event) {
        try {
            Trace.traceBegin(262144L, "notifyPackageChangeObservers");
            synchronized (this.mPackageChangeObservers) {
                Iterator<IPackageChangeObserver> it = this.mPackageChangeObservers.iterator();
                while (it.hasNext()) {
                    IPackageChangeObserver observer = it.next();
                    try {
                        observer.onPackageChanged(event);
                    } catch (RemoteException e) {
                        Log.wtf(TAG, e);
                    }
                }
            }
        } finally {
            Trace.traceEnd(262144L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Settings.VersionInfo getSettingsVersionForPackage(AndroidPackage pkg) {
        if (pkg.isExternalStorage()) {
            if (TextUtils.isEmpty(pkg.getVolumeUuid())) {
                return this.mSettings.getExternalVersion();
            }
            return this.mSettings.findOrCreateVersion(pkg.getVolumeUuid());
        }
        return this.mSettings.getInternalVersion();
    }

    public void deleteExistingPackageAsUser(VersionedPackage versionedPackage, IPackageDeleteObserver2 observer, int userId) {
        this.mDeletePackageHelper.deleteExistingPackageAsUser(versionedPackage, observer, userId);
    }

    public void deletePackageAsOOBE(VersionedPackage versionedPackage, IPackageDeleteObserver2 observer, int userId, int flags) {
        AndroidPackage pkg;
        Slog.d(TAG, "deletePackageAsOOBE packageName = " + versionedPackage.toString());
        synchronized (this.mPackages) {
            pkg = this.mPackages.get(versionedPackage.getPackageName());
        }
        if (pkg != null) {
            sPmsExt.addToRemovableSystemAppSet(versionedPackage.getPackageName());
        }
        deletePackageVersioned(versionedPackage, observer, userId, flags);
    }

    public void deletePackageVersioned(VersionedPackage versionedPackage, IPackageDeleteObserver2 observer, int userId, int deleteFlags) {
        this.mDeletePackageHelper.deletePackageVersionedInternal(versionedPackage, observer, userId, deleteFlags, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCallerVerifier(Computer snapshot, int callingUid) {
        int callingUserId = UserHandle.getUserId(callingUid);
        String str = this.mRequiredVerifierPackage;
        return str != null && callingUid == snapshot.getPackageUid(str, 0L, callingUserId);
    }

    public boolean isPackageDeviceAdminOnAnyUser(Computer snapshot, String packageName) {
        int callingUid = Binder.getCallingUid();
        if (snapshot.checkUidPermission("android.permission.MANAGE_USERS", callingUid) != 0) {
            EventLog.writeEvent(1397638484, "128599183", -1, "");
            throw new SecurityException("android.permission.MANAGE_USERS permission is required to call this API");
        } else if (snapshot.getInstantAppPackageName(callingUid) == null || snapshot.isCallerSameApp(packageName, callingUid)) {
            return isPackageDeviceAdmin(packageName, -1);
        } else {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPackageDeviceAdmin(String packageName, int userId) {
        IDevicePolicyManager dpm = getDevicePolicyManager();
        if (dpm != null) {
            try {
                ComponentName deviceOwnerComponentName = dpm.getDeviceOwnerComponent(false);
                String deviceOwnerPackageName = deviceOwnerComponentName == null ? null : deviceOwnerComponentName.getPackageName();
                if (packageName.equals(deviceOwnerPackageName)) {
                    return true;
                }
                int[] users = userId == -1 ? this.mUserManager.getUserIds() : new int[]{userId};
                for (int i : users) {
                    if (dpm.packageHasActiveAdmins(packageName, i)) {
                        return true;
                    }
                }
            } catch (RemoteException e) {
            }
        }
        return false;
    }

    private IDevicePolicyManager getDevicePolicyManager() {
        if (this.mDevicePolicyManager == null) {
            this.mDevicePolicyManager = IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"));
        }
        return this.mDevicePolicyManager;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean clearApplicationUserDataLIF(Computer snapshot, String packageName, int userId) {
        int flags;
        if (packageName == null) {
            Slog.w(TAG, "Attempt to delete null packageName.");
            return false;
        }
        AndroidPackage pkg = snapshot.getPackage(packageName);
        if (pkg == null) {
            Slog.w(TAG, "Package named '" + packageName + "' doesn't exist.");
            return false;
        }
        this.mPermissionManager.resetRuntimePermissions(pkg, userId);
        this.mAppDataHelper.clearAppDataLIF(pkg, userId, 7);
        int appId = UserHandle.getAppId(pkg.getUid());
        this.mAppDataHelper.clearKeystoreData(userId, appId);
        UserManagerInternal umInternal = this.mInjector.getUserManagerInternal();
        StorageManagerInternal smInternal = (StorageManagerInternal) this.mInjector.getLocalService(StorageManagerInternal.class);
        if (StorageManager.isUserKeyUnlocked(userId) && smInternal.isCeStoragePrepared(userId)) {
            flags = 3;
        } else if (umInternal.isUserRunning(userId)) {
            flags = 1;
        } else {
            flags = 0;
        }
        this.mAppDataHelper.prepareAppDataContentsLIF(pkg, snapshot.getPackageStateInternal(packageName), userId, flags);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetComponentEnabledSettingsIfNeededLPw(String packageName, final int userId) {
        final PackageSetting pkgSetting;
        AndroidPackage pkg = packageName != null ? this.mPackages.get(packageName) : null;
        if (pkg == null || !pkg.isResetEnabledSettingsOnAppDataCleared() || (pkgSetting = this.mSettings.getPackageLPr(packageName)) == null) {
            return;
        }
        final ArrayList<String> updatedComponents = new ArrayList<>();
        Consumer<? super ParsedMainComponent> resetSettings = new Consumer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda18
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                PackageManagerService.lambda$resetComponentEnabledSettingsIfNeededLPw$45(PackageSetting.this, userId, updatedComponents, (ParsedMainComponent) obj);
            }
        };
        for (int i = 0; i < pkg.getActivities().size(); i++) {
            resetSettings.accept(pkg.getActivities().get(i));
        }
        for (int i2 = 0; i2 < pkg.getReceivers().size(); i2++) {
            resetSettings.accept(pkg.getReceivers().get(i2));
        }
        for (int i3 = 0; i3 < pkg.getServices().size(); i3++) {
            resetSettings.accept(pkg.getServices().get(i3));
        }
        for (int i4 = 0; i4 < pkg.getProviders().size(); i4++) {
            resetSettings.accept(pkg.getProviders().get(i4));
        }
        if (ArrayUtils.isEmpty(updatedComponents)) {
            return;
        }
        updateSequenceNumberLP(pkgSetting, new int[]{userId});
        updateInstantAppInstallerLocked(packageName);
        scheduleWritePackageRestrictions(userId);
        this.mPendingBroadcasts.addComponents(userId, packageName, updatedComponents);
        if (!this.mHandler.hasMessages(1)) {
            this.mHandler.sendEmptyMessageDelayed(1, 1000L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$resetComponentEnabledSettingsIfNeededLPw$45(PackageSetting pkgSetting, int userId, ArrayList updatedComponents, ParsedMainComponent component) {
        if (pkgSetting.restoreComponentLPw(component.getClassName(), userId)) {
            updatedComponents.add(component.getClassName());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$postPreferredActivityChangedBroadcast$46$com-android-server-pm-PackageManagerService  reason: not valid java name */
    public /* synthetic */ void m5560x57d8680(int userId) {
        this.mBroadcastHelper.sendPreferredActivityChangedBroadcast(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postPreferredActivityChangedBroadcast(final int userId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda63
            @Override // java.lang.Runnable
            public final void run() {
                PackageManagerService.this.m5560x57d8680(userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearPackagePreferredActivitiesLPw(String packageName, SparseBooleanArray outUserChanged, int userId) {
        this.mSettings.clearPackagePreferredActivities(packageName, outUserChanged, userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void restorePermissionsAndUpdateRolesForNewUserInstall(String packageName, int userId) {
        this.mPermissionManager.restoreDelayedRuntimePermissions(packageName, userId);
        if (ITranPackageManagerService.Instance().packageIsGaller(packageName, userId)) {
            IPackageManagerImpl iPackageManagerImpl = iPackageManager;
            iPackageManagerImpl.setDefaultGallerPackageName(iPackageManagerImpl.getDefaultGallerPackageName(userId), userId);
        }
        if (ITranPackageManagerService.Instance().packageIsMusic(packageName, userId)) {
            IPackageManagerImpl iPackageManagerImpl2 = iPackageManager;
            iPackageManagerImpl2.setDefaultMusicPackageName(iPackageManagerImpl2.getDefaultMusicPackageName(userId), userId);
        }
        this.mPreferredActivityHelper.updateDefaultHomeNotLocked(snapshotComputer(), userId);
    }

    public void addCrossProfileIntentFilter(Computer snapshot, WatchedIntentFilter intentFilter, String ownerPackage, int sourceUserId, int targetUserId, int flags) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
        int callingUid = Binder.getCallingUid();
        enforceOwnerRights(snapshot, ownerPackage, callingUid);
        PackageManagerServiceUtils.enforceShellRestriction(this.mInjector.getUserManagerInternal(), "no_debugging_features", callingUid, sourceUserId);
        if (intentFilter.countActions() == 0) {
            Slog.w(TAG, "Cannot set a crossProfile intent filter with no filter actions");
            return;
        }
        synchronized (this.mLock) {
            CrossProfileIntentFilter newFilter = new CrossProfileIntentFilter(intentFilter, ownerPackage, targetUserId, flags);
            CrossProfileIntentResolver resolver = this.mSettings.editCrossProfileIntentResolverLPw(sourceUserId);
            ArrayList<CrossProfileIntentFilter> existing = resolver.findFilters(intentFilter);
            if (existing != null) {
                int size = existing.size();
                for (int i = 0; i < size; i++) {
                    if (newFilter.equalsIgnoreFilter(existing.get(i))) {
                        return;
                    }
                }
            }
            resolver.addFilter((PackageDataSnapshot) snapshotComputer(), (Computer) newFilter);
            scheduleWritePackageRestrictions(sourceUserId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enforceOwnerRights(Computer snapshot, String pkg, int callingUid) {
        if (UserHandle.getAppId(callingUid) == 1000) {
            return;
        }
        String[] callerPackageNames = snapshot.getPackagesForUid(callingUid);
        if (!ArrayUtils.contains(callerPackageNames, pkg)) {
            throw new SecurityException("Calling uid " + callingUid + " does not own package " + pkg);
        }
        int callingUserId = UserHandle.getUserId(callingUid);
        PackageInfo pi = snapshot.getPackageInfo(pkg, 0L, callingUserId);
        if (pi == null) {
            throw new IllegalArgumentException("Unknown package " + pkg + " on user " + callingUserId);
        }
    }

    public void sendSessionCommitBroadcast(PackageInstaller.SessionInfo sessionInfo, int userId) {
        UserManagerService ums = UserManagerService.getInstance();
        if (ums == null || sessionInfo.isStaged()) {
            return;
        }
        UserInfo parent = ums.getProfileParent(userId);
        int launcherUid = parent != null ? parent.id : userId;
        ComponentName launcherComponent = snapshotComputer().getDefaultHomeActivity(launcherUid);
        this.mBroadcastHelper.sendSessionCommitBroadcast(sessionInfo, userId, launcherUid, launcherComponent, this.mAppPredictionServicePackage);
    }

    private String getSetupWizardPackageNameImpl(Computer computer) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.SETUP_WIZARD");
        List<ResolveInfo> matches = computer.queryIntentActivitiesInternal(intent, null, 1835520L, UserHandle.myUserId());
        if (matches.size() == 1) {
            return matches.get(0).getComponentInfo().packageName;
        }
        Slog.e(TAG, "There should probably be exactly one setup wizard; found " + matches.size() + ": matches=" + matches);
        return null;
    }

    private String getStorageManagerPackageName(Computer computer) {
        Intent intent = new Intent("android.os.storage.action.MANAGE_STORAGE");
        List<ResolveInfo> matches = computer.queryIntentActivitiesInternal(intent, null, 1835520L, UserHandle.myUserId());
        if (matches.size() == 1) {
            return matches.get(0).getComponentInfo().packageName;
        }
        Slog.w(TAG, "There should probably be exactly one storage manager; found " + matches.size() + ": matches=" + matches);
        return null;
    }

    private static String getRequiredSdkSandboxPackageName(Computer computer) {
        Intent intent = new Intent("com.android.sdksandbox.SdkSandboxService");
        List<ResolveInfo> matches = computer.queryIntentServicesInternal(intent, null, 1835008L, 0, Process.myUid(), false);
        if (matches.size() == 1) {
            return matches.get(0).getComponentInfo().packageName;
        }
        throw new RuntimeException("There should exactly one sdk sandbox package; found " + matches.size() + ": matches=" + matches);
    }

    private String getRetailDemoPackageName() {
        AndroidPackage androidPkg;
        SigningDetails signingDetail;
        Signature[] signatures;
        String predefinedPkgName = this.mContext.getString(17040026);
        String predefinedSignature = this.mContext.getString(17040027);
        if (!TextUtils.isEmpty(predefinedPkgName) && !TextUtils.isEmpty(predefinedSignature) && (androidPkg = this.mPackages.get(predefinedPkgName)) != null && (signingDetail = androidPkg.getSigningDetails()) != null && signingDetail.getSignatures() != null) {
            try {
                MessageDigest msgDigest = MessageDigest.getInstance("SHA-256");
                for (Signature signature : signingDetail.getSignatures()) {
                    if (TextUtils.equals(predefinedSignature, HexEncoding.encodeToString(msgDigest.digest(signature.toByteArray()), false))) {
                        return predefinedPkgName;
                    }
                }
            } catch (NoSuchAlgorithmException e) {
                Slog.e(TAG, "Unable to verify signatures as getting the retail demo package name", e);
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getPackageFromComponentString(int stringResId) {
        ComponentName component;
        String componentString = this.mContext.getString(stringResId);
        if (TextUtils.isEmpty(componentString) || (component = ComponentName.unflattenFromString(componentString)) == null) {
            return null;
        }
        return component.getPackageName();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String ensureSystemPackageName(Computer snapshot, String packageName) {
        if (packageName == null) {
            return null;
        }
        long token = Binder.clearCallingIdentity();
        try {
            if (snapshot.getPackageInfo(packageName, 2097152L, 0) == null) {
                PackageInfo packageInfo = snapshot.getPackageInfo(packageName, 0L, 0);
                if (packageInfo != null) {
                    EventLog.writeEvent(1397638484, "145981139", Integer.valueOf(packageInfo.applicationInfo.uid), "");
                }
                Log.w(TAG, "Missing required system package: " + packageName + (packageInfo != null ? ", but found with extended search." : "."));
                return null;
            }
            return packageName;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void updateComponentLabelIcon(final ComponentName componentName, final String nonLocalizedLabel, final Integer icon, final int userId) {
        if (componentName == null) {
            throw new IllegalArgumentException("Must specify a component");
        }
        int callingUid = Binder.getCallingUid();
        String componentPkgName = componentName.getPackageName();
        Computer computer = snapshotComputer();
        int componentUid = computer.getPackageUid(componentPkgName, 0L, userId);
        if (!UserHandle.isSameApp(callingUid, componentUid)) {
            throw new SecurityException("The calling UID (" + callingUid + ") does not match the target UID");
        }
        String allowedCallerPkg = this.mContext.getString(17040009);
        if (TextUtils.isEmpty(allowedCallerPkg)) {
            throw new SecurityException("There is no package defined as allowed to change a component's label or icon");
        }
        int allowedCallerUid = computer.getPackageUid(allowedCallerPkg, 1048576L, userId);
        if (allowedCallerUid == -1 || !UserHandle.isSameApp(callingUid, allowedCallerUid)) {
            throw new SecurityException("The calling UID (" + callingUid + ") is not allowed to change a component's label or icon");
        }
        PackageStateInternal packageState = computer.getPackageStateInternal(componentPkgName);
        if (packageState != null && packageState.getPkg() != null) {
            if (packageState.isSystem() || packageState.getTransientState().isUpdatedSystemApp()) {
                if (!computer.getComponentResolver().componentExists(componentName)) {
                    throw new IllegalArgumentException("Component " + componentName + " not found");
                }
                Pair<String, Integer> overrideLabelIcon = packageState.getUserStateOrDefault(userId).getOverrideLabelIconForComponent(componentName);
                String existingLabel = overrideLabelIcon == null ? null : (String) overrideLabelIcon.first;
                Integer existingIcon = overrideLabelIcon == null ? null : (Integer) overrideLabelIcon.second;
                if (!TextUtils.equals(existingLabel, nonLocalizedLabel) || !Objects.equals(existingIcon, icon)) {
                    commitPackageStateMutation(null, componentPkgName, new Consumer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda17
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((PackageStateWrite) obj).userState(userId).setComponentLabelIcon(componentName, nonLocalizedLabel, icon);
                        }
                    });
                    this.mPendingBroadcasts.addComponent(userId, componentPkgName, componentName.getClassName());
                    if (!this.mHandler.hasMessages(1)) {
                        this.mHandler.sendEmptyMessageDelayed(1, 1000L);
                        return;
                    }
                    return;
                }
                return;
            }
        }
        throw new SecurityException("Changing the label is not allowed for " + componentName);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3988=5, 4091=4] */
    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:128:0x037d, code lost:
        r0 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:129:0x0384, code lost:
        if (r0 >= r15) goto L200;
     */
    /* JADX WARN: Code restructure failed: missing block: B:130:0x0386, code lost:
        r1 = (android.content.pm.PackageManager.ComponentEnabledSetting) r13.get(r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:131:0x0390, code lost:
        if (r1.isComponent() != false) goto L178;
     */
    /* JADX WARN: Code restructure failed: missing block: B:133:0x0394, code lost:
        r3 = r1.getPackageName();
        r4 = r1.getClassName();
     */
    /* JADX WARN: Code restructure failed: missing block: B:134:0x039c, code lost:
        if (r17 != false) goto L186;
     */
    /* JADX WARN: Code restructure failed: missing block: B:136:0x03a4, code lost:
        if (android.content.pm.PackageManager.APP_DETAILS_ACTIVITY_CLASS_NAME.equals(r4) != false) goto L183;
     */
    /* JADX WARN: Code restructure failed: missing block: B:139:0x03ae, code lost:
        throw new java.lang.SecurityException("Cannot disable a system-generated component");
     */
    /* JADX WARN: Code restructure failed: missing block: B:140:0x03af, code lost:
        r5 = r6.get(r3).getPkg();
     */
    /* JADX WARN: Code restructure failed: missing block: B:141:0x03b9, code lost:
        if (r5 == null) goto L191;
     */
    /* JADX WARN: Code restructure failed: missing block: B:143:0x03bf, code lost:
        if (com.android.server.pm.parsing.pkg.AndroidPackageUtils.hasComponentClassName(r5, r4) != false) goto L190;
     */
    /* JADX WARN: Code restructure failed: missing block: B:144:0x03c1, code lost:
        if (r5 == null) goto L198;
     */
    /* JADX WARN: Code restructure failed: missing block: B:146:0x03c9, code lost:
        if (r5.getTargetSdkVersion() >= 16) goto L195;
     */
    /* JADX WARN: Code restructure failed: missing block: B:149:0x03ee, code lost:
        throw new java.lang.IllegalArgumentException("Component class " + r4 + " does not exist in " + r3);
     */
    /* JADX WARN: Code restructure failed: missing block: B:150:0x03ef, code lost:
        android.util.Slog.w(com.android.server.pm.PackageManagerService.TAG, "Failed setComponentEnabledSetting: component class " + r4 + " does not exist in " + r3);
        r7[r0] = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:151:0x0414, code lost:
        r0 = r0 + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:155:0x0421, code lost:
        r1 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:156:0x0423, code lost:
        if (r1 >= r15) goto L245;
     */
    /* JADX WARN: Code restructure failed: missing block: B:157:0x0425, code lost:
        r2 = (android.content.pm.PackageManager.ComponentEnabledSetting) r13.get(r1);
     */
    /* JADX WARN: Code restructure failed: missing block: B:158:0x0430, code lost:
        if (r2.isComponent() == false) goto L209;
     */
    /* JADX WARN: Code restructure failed: missing block: B:160:0x0435, code lost:
        r3 = r6.get(r2.getPackageName());
        r4 = r2.getEnabledState();
        r5 = r25.mLock;
     */
    /* JADX WARN: Code restructure failed: missing block: B:161:0x0446, code lost:
        monitor-enter(r5);
     */
    /* JADX WARN: Code restructure failed: missing block: B:163:0x044b, code lost:
        if (r3.getEnabled(r27) != r4) goto L218;
     */
    /* JADX WARN: Code restructure failed: missing block: B:164:0x044d, code lost:
        r7[r1] = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:165:0x0450, code lost:
        monitor-exit(r5);
     */
    /* JADX WARN: Code restructure failed: missing block: B:167:0x0453, code lost:
        monitor-exit(r5);
     */
    /* JADX WARN: Code restructure failed: missing block: B:168:0x0454, code lost:
        r0 = r3.getPkg();
     */
    /* JADX WARN: Code restructure failed: missing block: B:169:0x0458, code lost:
        if (r0 == null) goto L240;
     */
    /* JADX WARN: Code restructure failed: missing block: B:171:0x045e, code lost:
        if (r0.isStub() == false) goto L240;
     */
    /* JADX WARN: Code restructure failed: missing block: B:173:0x0464, code lost:
        if (r0.isSystem() == false) goto L240;
     */
    /* JADX WARN: Code restructure failed: missing block: B:174:0x0466, code lost:
        r5 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:175:0x0468, code lost:
        r5 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:176:0x0469, code lost:
        if (r5 == false) goto L238;
     */
    /* JADX WARN: Code restructure failed: missing block: B:177:0x046b, code lost:
        if (r4 == 0) goto L232;
     */
    /* JADX WARN: Code restructure failed: missing block: B:179:0x046e, code lost:
        if (r4 != 1) goto L230;
     */
    /* JADX WARN: Code restructure failed: missing block: B:183:0x0479, code lost:
        if (r25.mInstallPackageHelper.enableCompressedPackage(r0, r3) != false) goto L236;
     */
    /* JADX WARN: Code restructure failed: missing block: B:184:0x047b, code lost:
        android.util.Slog.w(com.android.server.pm.PackageManagerService.TAG, "Failed setApplicationEnabledSetting: failed to enable commpressed package " + r2.getPackageName());
        r7[r1] = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:187:0x049e, code lost:
        r1 = r1 + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:191:0x04a4, code lost:
        r0 = new android.util.ArrayMap<>(r15);
        r12 = r25.mLock;
     */
    /* JADX WARN: Code restructure failed: missing block: B:192:0x04ac, code lost:
        monitor-enter(r12);
     */
    /* JADX WARN: Code restructure failed: missing block: B:193:0x04ad, code lost:
        r2 = snapshotComputer();
     */
    /* JADX WARN: Code restructure failed: missing block: B:194:0x04b1, code lost:
        r0 = false;
        r13 = false;
        r16 = false;
        r5 = 0;
        r6 = r6;
     */
    /* JADX WARN: Code restructure failed: missing block: B:195:0x04b9, code lost:
        if (r5 >= r15) goto L300;
     */
    /* JADX WARN: Code restructure failed: missing block: B:197:0x04bd, code lost:
        if (r7[r5] != false) goto L258;
     */
    /* JADX WARN: Code restructure failed: missing block: B:198:0x04bf, code lost:
        r18 = r5;
        r20 = r7;
        r21 = r11;
        r11 = r6;
     */
    /* JADX WARN: Code restructure failed: missing block: B:199:0x04ca, code lost:
        r4 = (android.content.pm.PackageManager.ComponentEnabledSetting) r13.get(r5);
        r1 = r4.getPackageName();
     */
    /* JADX WARN: Code restructure failed: missing block: B:200:0x04de, code lost:
        r20 = r7;
        r18 = r5;
        r21 = r11;
        r11 = r6;
     */
    /* JADX WARN: Code restructure failed: missing block: B:202:0x04f4, code lost:
        if (setEnabledSettingInternalLocked(r2, r6.get(r1), r4, r27, r28) != false) goto L265;
     */
    /* JADX WARN: Code restructure failed: missing block: B:205:0x04ff, code lost:
        if (com.android.server.pm.IPackageManagerServiceLice.Instance().isTmpUnHideApp(r1) != false) goto L268;
     */
    /* JADX WARN: Code restructure failed: missing block: B:206:0x0501, code lost:
        r16 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:208:0x0509, code lost:
        if ((r4.getEnabledFlags() & 2) == 0) goto L271;
     */
    /* JADX WARN: Code restructure failed: missing block: B:209:0x050b, code lost:
        r13 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:211:0x0510, code lost:
        if (r4.isComponent() == false) goto L287;
     */
    /* JADX WARN: Code restructure failed: missing block: B:212:0x0512, code lost:
        r3 = r4.getClassName();
     */
    /* JADX WARN: Code restructure failed: missing block: B:213:0x0517, code lost:
        r3 = r1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:214:0x0518, code lost:
        r1 = r3;
     */
    /* JADX WARN: Code restructure failed: missing block: B:215:0x051f, code lost:
        if ((r4.getEnabledFlags() & 1) != 0) goto L285;
     */
    /* JADX WARN: Code restructure failed: missing block: B:216:0x0521, code lost:
        r3 = r0.get(r1);
     */
    /* JADX WARN: Code restructure failed: missing block: B:217:0x0527, code lost:
        if (r3 != null) goto L284;
     */
    /* JADX WARN: Code restructure failed: missing block: B:218:0x0529, code lost:
        r5 = new java.util.ArrayList<>();
     */
    /* JADX WARN: Code restructure failed: missing block: B:219:0x052f, code lost:
        r5 = r3;
     */
    /* JADX WARN: Code restructure failed: missing block: B:220:0x0530, code lost:
        r3 = r5;
     */
    /* JADX WARN: Code restructure failed: missing block: B:221:0x0535, code lost:
        if (r3.contains(r1) != false) goto L282;
     */
    /* JADX WARN: Code restructure failed: missing block: B:222:0x0537, code lost:
        r3.add(r1);
     */
    /* JADX WARN: Code restructure failed: missing block: B:223:0x053a, code lost:
        r0.put(r1, r3);
        r25.mPendingBroadcasts.remove(r27, r1);
     */
    /* JADX WARN: Code restructure failed: missing block: B:224:0x0543, code lost:
        r25.mPendingBroadcasts.addComponent(r27, r1, r1);
        r0 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:225:0x0549, code lost:
        r5 = r18 + 1;
        r6 = r11;
        r7 = r20;
        r11 = r21;
     */
    /* JADX WARN: Code restructure failed: missing block: B:226:0x0552, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:228:0x055a, code lost:
        r20 = r7;
        r11 = r6;
     */
    /* JADX WARN: Code restructure failed: missing block: B:229:0x0561, code lost:
        if (r16 != false) goto L305;
     */
    /* JADX WARN: Code restructure failed: missing block: B:230:0x0563, code lost:
        monitor-exit(r12);
     */
    /* JADX WARN: Code restructure failed: missing block: B:231:0x0564, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:232:0x0565, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:234:0x056a, code lost:
        if (r13 == false) goto L339;
     */
    /* JADX WARN: Code restructure failed: missing block: B:235:0x056c, code lost:
        flushPackageRestrictionsAsUserInternalLocked(r27);
     */
    /* JADX WARN: Code restructure failed: missing block: B:237:0x0570, code lost:
        scheduleWritePackageRestrictions(r27);
     */
    /* JADX WARN: Code restructure failed: missing block: B:238:0x0573, code lost:
        if (r0 == false) goto L315;
     */
    /* JADX WARN: Code restructure failed: missing block: B:240:0x057c, code lost:
        if (r25.mHandler.hasMessages(1) != false) goto L315;
     */
    /* JADX WARN: Code restructure failed: missing block: B:242:0x0586, code lost:
        if (android.os.SystemClock.uptimeMillis() <= r25.mServiceStartWithDelay) goto L314;
     */
    /* JADX WARN: Code restructure failed: missing block: B:243:0x0588, code lost:
        r3 = 1000;
     */
    /* JADX WARN: Code restructure failed: missing block: B:244:0x058b, code lost:
        r3 = 10000;
     */
    /* JADX WARN: Code restructure failed: missing block: B:245:0x058d, code lost:
        r25.mHandler.sendEmptyMessageDelayed(1, r3);
     */
    /* JADX WARN: Code restructure failed: missing block: B:246:0x0594, code lost:
        monitor-exit(r12);
     */
    /* JADX WARN: Code restructure failed: missing block: B:247:0x0595, code lost:
        r12 = android.os.Binder.clearCallingIdentity();
     */
    /* JADX WARN: Code restructure failed: missing block: B:248:0x0599, code lost:
        r2 = snapshotComputer();
        r0 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:250:0x05a2, code lost:
        if (r0 >= r0.size()) goto L330;
     */
    /* JADX WARN: Code restructure failed: missing block: B:251:0x05a4, code lost:
        r1 = r0.keyAt(r0);
        r5 = r0.valueAt(r0);
        r6 = android.os.UserHandle.getUid(r27, r11.get(r1).getAppId());
     */
    /* JADX WARN: Code restructure failed: missing block: B:252:0x05c1, code lost:
        r18 = r20;
     */
    /* JADX WARN: Code restructure failed: missing block: B:253:0x05cd, code lost:
        sendPackageChangedBroadcast(r2, r1, false, r5, r6, null);
     */
    /* JADX WARN: Code restructure failed: missing block: B:254:0x05d0, code lost:
        r0 = r0 + 1;
        r20 = r18;
     */
    /* JADX WARN: Code restructure failed: missing block: B:255:0x05d5, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:257:0x05d7, code lost:
        android.os.Binder.restoreCallingIdentity(r12);
     */
    /* JADX WARN: Code restructure failed: missing block: B:258:0x05dd, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:259:0x05de, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:261:0x05e1, code lost:
        android.os.Binder.restoreCallingIdentity(r12);
     */
    /* JADX WARN: Code restructure failed: missing block: B:262:0x05e4, code lost:
        throw r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:263:0x05e5, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:265:0x05e9, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:267:0x05ef, code lost:
        monitor-exit(r12);
     */
    /* JADX WARN: Code restructure failed: missing block: B:268:0x05f0, code lost:
        throw r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:269:0x05f1, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:271:0x05f3, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:79:0x0243, code lost:
        throw new java.lang.SecurityException("Cannot disable a protected package: " + r5);
     */
    /* JADX WARN: Code restructure failed: missing block: B:80:0x0244, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:85:0x0260, code lost:
        r1 = new java.lang.StringBuilder().append("Attempt to change component state; pid=").append(android.os.Binder.getCallingPid()).append(", uid=").append(r11);
     */
    /* JADX WARN: Code restructure failed: missing block: B:86:0x0283, code lost:
        if (r4.isComponent() != false) goto L159;
     */
    /* JADX WARN: Code restructure failed: missing block: B:89:0x028c, code lost:
        r12 = ", package=" + r5;
     */
    /* JADX WARN: Code restructure failed: missing block: B:90:0x029b, code lost:
        r12 = ", component=" + r4.getComponentName();
     */
    /* JADX WARN: Code restructure failed: missing block: B:92:0x02bf, code lost:
        throw new java.lang.SecurityException(r1.append(r12).toString());
     */
    /* JADX WARN: Code restructure failed: missing block: B:93:0x02c0, code lost:
        r0 = th;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void setEnabledSettings(List<PackageManager.ComponentEnabledSetting> settings, int userId, String callingPackage) {
        Computer snapshot;
        int i;
        PackageManager.ComponentEnabledSetting setting;
        Computer snapshot2;
        Computer preLockSnapshot;
        List<PackageManager.ComponentEnabledSetting> settingsFilter;
        int newState;
        ArraySet<ComponentName> checkDuplicatedComponent;
        ArrayMap<String, Integer> checkConflictFlag;
        int callingUid = Binder.getCallingUid();
        Computer preLockSnapshot2 = snapshotComputer();
        preLockSnapshot2.enforceCrossUserPermission(callingUid, userId, false, true, "set enabled");
        List<PackageManager.ComponentEnabledSetting> settingsFilter2 = new ArrayList<>();
        for (PackageManager.ComponentEnabledSetting componentEnabledSetting : settings) {
            if (!ArrayUtils.contains(DISABLE_PACKAGE_FILTER, componentEnabledSetting.getPackageName()) || componentEnabledSetting.getEnabledState() == 0 || componentEnabledSetting.getEnabledState() == 1) {
                settingsFilter2.add(componentEnabledSetting);
            }
            if (Build.IS_DEBUG_ENABLE) {
                int callingPid = Binder.getCallingPid();
                Slog.d(TAG, "setEnabledSettings callingUid = " + callingUid + " callingPid = " + callingPid + " packageName = " + componentEnabledSetting.getPackageName() + " callingPackage= " + callingPackage + " newState = " + componentEnabledSetting.getEnabledState());
            }
        }
        ITranPackageManagerService.Instance().setEnabledSettings(settingsFilter2, userId, callingPackage);
        int targetSize = settingsFilter2.size();
        for (int i2 = 0; i2 < targetSize; i2++) {
            int newState2 = ((PackageManager.ComponentEnabledSetting) settingsFilter2.get(i2)).getEnabledState();
            if (newState2 != 0 && newState2 != 1 && newState2 != 2 && newState2 != 3 && newState2 != 4) {
                throw new IllegalArgumentException("Invalid new component state: " + newState2);
            }
        }
        if (targetSize > 1) {
            ArraySet<String> checkDuplicatedPackage = new ArraySet<>();
            ArraySet<ComponentName> checkDuplicatedComponent2 = new ArraySet<>();
            ArrayMap<String, Integer> checkConflictFlag2 = new ArrayMap<>();
            int i3 = 0;
            while (i3 < targetSize) {
                PackageManager.ComponentEnabledSetting setting2 = (PackageManager.ComponentEnabledSetting) settingsFilter2.get(i3);
                String packageName = setting2.getPackageName();
                if (setting2.isComponent()) {
                    ComponentName componentName = setting2.getComponentName();
                    if (checkDuplicatedComponent2.contains(componentName)) {
                        throw new IllegalArgumentException("The component " + componentName + " is duplicated");
                    }
                    checkDuplicatedComponent2.add(componentName);
                    Integer enabledFlags = checkConflictFlag2.get(packageName);
                    if (enabledFlags == null) {
                        checkConflictFlag2.put(packageName, Integer.valueOf(setting2.getEnabledFlags()));
                        checkDuplicatedComponent = checkDuplicatedComponent2;
                    } else {
                        checkDuplicatedComponent = checkDuplicatedComponent2;
                        if ((enabledFlags.intValue() & 1) != (setting2.getEnabledFlags() & 1)) {
                            throw new IllegalArgumentException("A conflict of the DONT_KILL_APP flag between components in the package " + packageName);
                        }
                    }
                    checkConflictFlag = checkConflictFlag2;
                } else {
                    checkDuplicatedComponent = checkDuplicatedComponent2;
                    checkConflictFlag = checkConflictFlag2;
                    if (checkDuplicatedPackage.contains(packageName)) {
                        throw new IllegalArgumentException("The package " + packageName + " is duplicated");
                    }
                    checkDuplicatedPackage.add(packageName);
                }
                i3++;
                checkConflictFlag2 = checkConflictFlag;
                checkDuplicatedComponent2 = checkDuplicatedComponent;
            }
        }
        boolean allowedByPermission = this.mContext.checkCallingOrSelfPermission("android.permission.CHANGE_COMPONENT_ENABLED_STATE") == 0;
        boolean[] updateAllowed = new boolean[targetSize];
        Arrays.fill(updateAllowed, true);
        Map<String, PackageSetting> pkgSettings = new ArrayMap<>(targetSize);
        synchronized (this.mLock) {
            try {
                snapshot = snapshotComputer();
                i = 0;
            } catch (Throwable th) {
                th = th;
            }
            while (true) {
                if (i >= targetSize) {
                    break;
                }
                try {
                    setting = (PackageManager.ComponentEnabledSetting) settingsFilter2.get(i);
                    String packageName2 = setting.getPackageName();
                    if (pkgSettings.containsKey(packageName2)) {
                        snapshot2 = snapshot;
                        preLockSnapshot = preLockSnapshot2;
                        settingsFilter = settingsFilter2;
                    } else {
                        boolean isCallerTargetApp = ArrayUtils.contains(snapshot.getPackagesForUid(callingUid), packageName2);
                        PackageSetting pkgSetting = this.mSettings.getPackageLPr(packageName2);
                        if (!isCallerTargetApp) {
                            if (!allowedByPermission) {
                                break;
                            }
                            try {
                                if (snapshot.shouldFilterApplication(pkgSetting, callingUid, userId)) {
                                    break;
                                }
                                snapshot2 = snapshot;
                                if (this.mProtectedPackages.isPackageStateProtected(userId, packageName2)) {
                                    break;
                                }
                                preLockSnapshot = preLockSnapshot2;
                                settingsFilter = settingsFilter2;
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        } else {
                            snapshot2 = snapshot;
                            preLockSnapshot = preLockSnapshot2;
                            settingsFilter = settingsFilter2;
                        }
                        if (pkgSetting == null) {
                            throw new IllegalArgumentException(setting.isComponent() ? "Unknown component: " + setting.getComponentName() : "Unknown package: " + packageName2);
                        }
                        if (callingUid == 2000) {
                            try {
                                if ((pkgSetting.getFlags() & 256) == 0) {
                                    int oldState = pkgSetting.getEnabled(userId);
                                    newState = setting.getEnabledState();
                                    if (setting.isComponent() || ((oldState != 3 && oldState != 0 && oldState != 1) || (newState != 3 && newState != 0 && newState != 1))) {
                                        break;
                                    }
                                }
                            } catch (Throwable th3) {
                                th = th3;
                            }
                        }
                        pkgSettings.put(packageName2, pkgSetting);
                    }
                    i++;
                    snapshot = snapshot2;
                    preLockSnapshot2 = preLockSnapshot;
                    settingsFilter2 = settingsFilter;
                } catch (Throwable th4) {
                    th = th4;
                }
                while (true) {
                    try {
                        break;
                    } catch (Throwable th5) {
                        th = th5;
                    }
                }
                throw th;
            }
            throw new SecurityException("Shell cannot change component state for " + setting.getComponentName() + " to " + newState);
        }
    }

    private boolean setEnabledSettingInternalLocked(Computer computer, PackageSetting pkgSetting, PackageManager.ComponentEnabledSetting setting, int userId, String callingPackage) {
        int newState = setting.getEnabledState();
        String packageName = setting.getPackageName();
        boolean success = false;
        if (!setting.isComponent()) {
            if ((newState == 0 || newState == 1) && !"com.xui.xhide".equals(callingPackage)) {
                callingPackage = null;
            }
            pkgSetting.setEnabled(newState, userId, callingPackage);
            if ((newState == 3 || newState == 2) && checkPermission("android.permission.SUSPEND_APPS", packageName, userId) == 0) {
                unsuspendForSuspendingPackage(computer, packageName, userId);
                removeAllDistractingPackageRestrictions(computer, userId);
            }
            success = true;
        } else {
            String className = setting.getClassName();
            switch (newState) {
                case 0:
                    success = pkgSetting.restoreComponentLPw(className, userId);
                    break;
                case 1:
                    success = pkgSetting.enableComponentLPw(className, userId);
                    break;
                case 2:
                    success = pkgSetting.disableComponentLPw(className, userId);
                    break;
                default:
                    Slog.e(TAG, "Failed setComponentEnabledSetting: component " + packageName + SliceClientPermissions.SliceAuthority.DELIMITER + className + " requested an invalid new component state: " + newState);
                    break;
            }
        }
        if (!success) {
            return false;
        }
        updateSequenceNumberLP(pkgSetting, new int[]{userId});
        long callingId = Binder.clearCallingIdentity();
        try {
            updateInstantAppInstallerLocked(packageName);
            return true;
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void flushPackageRestrictionsAsUserInternalLocked(int userId) {
        this.mSettings.writePackageRestrictionsLPr(userId);
        synchronized (this.mDirtyUsers) {
            this.mDirtyUsers.remove(Integer.valueOf(userId));
            if (this.mDirtyUsers.isEmpty()) {
                this.mHandler.removeMessages(14);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendPackageChangedBroadcast(Computer snapshot, final String packageName, final boolean dontKillApp, final ArrayList<String> componentNames, final int packageUid, final String reason) {
        PackageStateInternal setting = snapshot.getPackageStateInternal(packageName, 1000);
        if (setting == null) {
            return;
        }
        if (IPackageManagerServiceLice.Instance().isTmpUnHideApp(packageName)) {
            Slog.d(TAG, "don't send package change to app, is temp unhidden");
            return;
        }
        int userId = UserHandle.getUserId(packageUid);
        boolean isInstantApp = snapshot.isInstantAppInternal(packageName, userId, 1000);
        final int[] userIds = isInstantApp ? EMPTY_INT_ARRAY : new int[]{userId};
        final int[] instantUserIds = isInstantApp ? new int[]{userId} : EMPTY_INT_ARRAY;
        final SparseArray<int[]> broadcastAllowList = snapshot.getBroadcastAllowList(packageName, userIds, isInstantApp);
        this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda56
            @Override // java.lang.Runnable
            public final void run() {
                PackageManagerService.this.m5570x6b97c054(packageName, dontKillApp, componentNames, packageUid, reason, userIds, instantUserIds, broadcastAllowList);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendPackageChangedBroadcast$48$com-android-server-pm-PackageManagerService  reason: not valid java name */
    public /* synthetic */ void m5570x6b97c054(String packageName, boolean dontKillApp, ArrayList componentNames, int packageUid, String reason, int[] userIds, int[] instantUserIds, SparseArray broadcastAllowList) {
        this.mBroadcastHelper.sendPackageChangedBroadcast(packageName, dontKillApp, componentNames, packageUid, reason, userIds, instantUserIds, broadcastAllowList);
    }

    public void waitForAppDataPrepared() {
        Future<?> future = this.mPrepareAppDataFuture;
        if (future == null) {
            return;
        }
        ConcurrentUtils.waitForFutureNoInterrupt(future, "wait for prepareAppData");
        this.mPrepareAppDataFuture = null;
    }

    public void systemReady() {
        PackageManagerServiceUtils.enforceSystemOrRoot("Only the system can claim the system is ready");
        final ContentResolver resolver = this.mContext.getContentResolver();
        List<File> list = this.mReleaseOnSystemReady;
        if (list != null) {
            for (int i = list.size() - 1; i >= 0; i--) {
                File dstCodePath = this.mReleaseOnSystemReady.get(i);
                F2fsUtils.releaseCompressedBlocks(resolver, dstCodePath);
            }
            this.mReleaseOnSystemReady = null;
        }
        this.mSystemReady = true;
        ContentObserver co = new ContentObserver(this.mHandler) { // from class: com.android.server.pm.PackageManagerService.3
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                int[] userIds;
                boolean ephemeralFeatureDisabled = Settings.Global.getInt(resolver, "enable_ephemeral_feature", 1) == 0;
                for (int userId : UserManagerService.getInstance().getUserIds()) {
                    boolean instantAppsDisabledForUser = ephemeralFeatureDisabled || Settings.Secure.getIntForUser(resolver, "instant_apps_enabled", 1, userId) == 0;
                    PackageManagerService.this.mWebInstantAppsDisabled.put(userId, instantAppsDisabledForUser);
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("enable_ephemeral_feature"), false, co, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("instant_apps_enabled"), false, co, -1);
        co.onChange(true);
        ITranPackageManagerService.Instance().systemReady(this.mContext, this.mHandler);
        this.mAppsFilter.onSystemReady((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class));
        CarrierAppUtils.disableCarrierAppsUntilPrivileged(this.mContext.getOpPackageName(), 0, this.mContext);
        disableSkuSpecificApps();
        boolean compatibilityModeEnabled = Settings.Global.getInt(this.mContext.getContentResolver(), "compatibility_mode", 1) == 1;
        ParsingPackageUtils.setCompatibilityModeEnabled(compatibilityModeEnabled);
        if (DEBUG_SETTINGS) {
            Log.d(TAG, "compatibility mode:" + compatibilityModeEnabled);
        }
        synchronized (this.mLock) {
            ArrayList<Integer> changed = this.mSettings.systemReady(this.mComponentResolver);
            for (int i2 = 0; i2 < changed.size(); i2++) {
                this.mSettings.writePackageRestrictionsLPr(changed.get(i2).intValue());
            }
        }
        this.mUserManager.systemReady();
        StorageManager storage = (StorageManager) this.mInjector.getSystemService(StorageManager.class);
        storage.registerListener(this.mStorageEventHelper);
        this.mInstallerService.systemReady();
        this.mPackageDexOptimizer.systemReady();
        this.mUserManager.reconcileUsers(StorageManager.UUID_PRIVATE_INTERNAL);
        this.mStorageEventHelper.reconcileApps(snapshotComputer(), StorageManager.UUID_PRIVATE_INTERNAL);
        this.mPermissionManager.onSystemReady();
        int[] grantPermissionsUserIds = EMPTY_INT_ARRAY;
        List<UserInfo> livingUsers = this.mInjector.getUserManagerInternal().getUsers(true, true, false);
        int livingUserCount = livingUsers.size();
        for (int i3 = 0; i3 < livingUserCount; i3++) {
            int userId = livingUsers.get(i3).id;
            if (this.mSettings.isPermissionUpgradeNeeded(userId)) {
                grantPermissionsUserIds = ArrayUtils.appendInt(grantPermissionsUserIds, userId);
            }
        }
        for (int userId2 : grantPermissionsUserIds) {
            this.mLegacyPermissionManager.grantDefaultPermissions(userId2);
        }
        if (grantPermissionsUserIds == EMPTY_INT_ARRAY) {
            this.mLegacyPermissionManager.scheduleReadDefaultPermissionExceptions();
        }
        if (this.mInstantAppResolverConnection != null) {
            this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.pm.PackageManagerService.4
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    PackageManagerService.this.mInstantAppResolverConnection.optimisticBind();
                    PackageManagerService.this.mContext.unregisterReceiver(this);
                }
            }, new IntentFilter("android.intent.action.BOOT_COMPLETED"));
        }
        IntentFilter overlayFilter = new IntentFilter("android.intent.action.OVERLAY_CHANGED");
        overlayFilter.addDataScheme("package");
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.pm.PackageManagerService.5
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                Uri data;
                String packageName;
                Computer snapshot;
                AndroidPackage pkg;
                if (intent == null || (data = intent.getData()) == null || (packageName = data.getSchemeSpecificPart()) == null || (pkg = (snapshot = PackageManagerService.this.snapshotComputer()).getPackage(packageName)) == null) {
                    return;
                }
                PackageManagerService.this.sendPackageChangedBroadcast(snapshot, pkg.getPackageName(), true, new ArrayList<>(Collections.singletonList(pkg.getPackageName())), pkg.getUid(), "android.intent.action.OVERLAY_CHANGED");
            }
        }, overlayFilter);
        this.mModuleInfoProvider.systemReady();
        this.mInstallerService.restoreAndApplyStagedSessionIfNeeded();
        this.mExistingPackages = null;
        CtaManagerFactory.getInstance().makeCtaManager().systemReady();
        ITranPackageManagerService.Instance().systemReady(this.mPackages, isFirstBoot());
        DeviceConfig.addOnPropertiesChangedListener("package_manager_service", this.mInjector.getBackgroundExecutor(), new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda55
            public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                PackageManagerService.this.m5572xff69ed2c(properties);
            }
        });
        this.mBackgroundDexOptService.systemReady();
        schedulePruneUnusedStaticSharedLibraries(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$systemReady$49$com-android-server-pm-PackageManagerService  reason: not valid java name */
    public /* synthetic */ void m5572xff69ed2c(DeviceConfig.Properties properties) {
        Set<String> keyset = properties.getKeyset();
        if (keyset.contains(PROPERTY_INCFS_DEFAULT_TIMEOUTS) || keyset.contains(PROPERTY_KNOWN_DIGESTERS_LIST)) {
            this.mPerUidReadTimeoutsCache = null;
        }
    }

    private void disableSkuSpecificApps() {
        String[] apkList = this.mContext.getResources().getStringArray(17236032);
        String[] skuArray = this.mContext.getResources().getStringArray(17236031);
        if (ArrayUtils.isEmpty(apkList)) {
            return;
        }
        String sku = SystemProperties.get("ro.boot.hardware.sku");
        if (!TextUtils.isEmpty(sku) && ArrayUtils.contains(skuArray, sku)) {
            return;
        }
        Computer snapshot = snapshotComputer();
        for (String packageName : apkList) {
            setSystemAppHiddenUntilInstalled(snapshot, packageName, true);
            for (UserInfo user : this.mInjector.getUserManagerInternal().getUsers(false)) {
                setSystemAppInstallState(snapshot, packageName, false, user.id);
            }
        }
    }

    public PackageFreezer freezePackage(String packageName, String killReason) {
        return freezePackage(packageName, -1, killReason);
    }

    public PackageFreezer freezePackage(String packageName, int userId, String killReason) {
        return new PackageFreezer(packageName, userId, killReason, this);
    }

    public PackageFreezer freezePackageForDelete(String packageName, int deleteFlags, String killReason) {
        return freezePackageForDelete(packageName, -1, deleteFlags, killReason);
    }

    public PackageFreezer freezePackageForDelete(String packageName, int userId, int deleteFlags, String killReason) {
        if ((deleteFlags & 8) != 0) {
            return new PackageFreezer(this);
        }
        return freezePackage(packageName, userId, killReason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cleanUpUser(UserManagerService userManager, int userId) {
        synchronized (this.mLock) {
            synchronized (this.mDirtyUsers) {
                this.mDirtyUsers.remove(Integer.valueOf(userId));
            }
            this.mUserNeedsBadging.delete(userId);
            this.mPermissionManager.onUserRemoved(userId);
            this.mSettings.removeUserLPw(userId);
            this.mPendingBroadcasts.remove(userId);
            this.mDeletePackageHelper.removeUnusedPackagesLPw(userManager, userId);
            this.mAppsFilter.onUserDeleted(userId);
        }
        this.mInstantAppRegistry.onUserRemoved(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void createNewUser(int userId, Set<String> userTypeInstallablePackages, String[] disallowedPackages) {
        synchronized (this.mInstallLock) {
            this.mSettings.createNewUserLI(this, this.mInstaller, userId, userTypeInstallablePackages, disallowedPackages);
        }
        synchronized (this.mLock) {
            scheduleWritePackageRestrictions(userId);
            scheduleWritePackageListLocked(userId);
            this.mAppsFilter.onUserCreated(snapshotComputer(), userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onNewUserCreated(int userId, boolean convertedFromPreCreated) {
        if (DEBUG_PERMISSIONS) {
            Slog.d(TAG, "onNewUserCreated(id=" + userId + ", convertedFromPreCreated=" + convertedFromPreCreated + ")");
        }
        if (!convertedFromPreCreated || !readPermissionStateForUser(userId)) {
            this.mPermissionManager.onUserCreated(userId);
            this.mLegacyPermissionManager.grantDefaultPermissions(userId);
            this.mDomainVerificationManager.clearUser(userId);
        }
    }

    private boolean readPermissionStateForUser(int userId) {
        boolean isPermissionUpgradeNeeded;
        synchronized (this.mLock) {
            this.mPermissionManager.writeLegacyPermissionStateTEMP();
            this.mSettings.readPermissionStateForUserSyncLPr(userId);
            this.mPermissionManager.readLegacyPermissionStateTEMP();
            isPermissionUpgradeNeeded = this.mSettings.isPermissionUpgradeNeeded(userId);
        }
        return isPermissionUpgradeNeeded;
    }

    public boolean isStorageLow() {
        long token = Binder.clearCallingIdentity();
        try {
            DeviceStorageMonitorInternal dsm = (DeviceStorageMonitorInternal) this.mInjector.getLocalService(DeviceStorageMonitorInternal.class);
            if (dsm != null) {
                return dsm.isMemoryLow();
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void deletePackageIfUnused(Computer snapshot, final String packageName) {
        PackageStateInternal ps = snapshot.getPackageStateInternal(packageName);
        if (ps == null) {
            return;
        }
        SparseArray<? extends PackageUserStateInternal> userStates = ps.getUserStates();
        for (int index = 0; index < userStates.size(); index++) {
            if (userStates.valueAt(index).isInstalled()) {
                return;
            }
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda16
            @Override // java.lang.Runnable
            public final void run() {
                PackageManagerService.this.m5556x10de52e8(packageName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$deletePackageIfUnused$50$com-android-server-pm-PackageManagerService  reason: not valid java name */
    public /* synthetic */ void m5556x10de52e8(String packageName) {
        this.mDeletePackageHelper.deletePackageX(packageName, -1L, 0, 2, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void deletePreloadsFileCache() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CLEAR_APP_CACHE", "deletePreloadsFileCache");
        File dir = Environment.getDataPreloadsFileCacheDirectory();
        Slog.i(TAG, "Deleting preloaded file cache " + dir);
        FileUtils.deleteContents(dir);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSystemAppHiddenUntilInstalled(Computer snapshot, final String packageName, final boolean hidden) {
        int callingUid = Binder.getCallingUid();
        boolean calledFromSystemOrPhone = callingUid == 1001 || callingUid == 1000;
        if (!calledFromSystemOrPhone) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.SUSPEND_APPS", "setSystemAppHiddenUntilInstalled");
        }
        PackageStateInternal stateRead = snapshot.getPackageStateInternal(packageName);
        if (stateRead == null || !stateRead.isSystem() || stateRead.getPkg() == null) {
            return;
        }
        if (stateRead.getPkg().isCoreApp() && !calledFromSystemOrPhone) {
            throw new SecurityException("Only system or phone callers can modify core apps");
        }
        commitPackageStateMutation(null, new Consumer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                PackageManagerService.lambda$setSystemAppHiddenUntilInstalled$51(packageName, hidden, (PackageStateMutator) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$setSystemAppHiddenUntilInstalled$51(String packageName, boolean hidden, PackageStateMutator mutator) {
        mutator.forPackage(packageName).setHiddenUntilInstalled(hidden);
        mutator.forDisabledSystemPackage(packageName).setHiddenUntilInstalled(hidden);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setSystemAppInstallState(Computer snapshot, String packageName, boolean installed, int userId) {
        int callingUid = Binder.getCallingUid();
        boolean calledFromSystemOrPhone = callingUid == 1001 || callingUid == 1000;
        if (!calledFromSystemOrPhone) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.SUSPEND_APPS", "setSystemAppHiddenUntilInstalled");
        }
        PackageStateInternal packageState = snapshot.getPackageStateInternal(packageName);
        if (packageState != null && packageState.isSystem()) {
            if (packageState.getPkg() != null) {
                if (packageState.getPkg().isCoreApp() && !calledFromSystemOrPhone) {
                    throw new SecurityException("Only system or phone callers can modify core apps");
                }
                if (packageState.getUserStateOrDefault(userId).isInstalled() == installed) {
                    return false;
                }
                long callingId = Binder.clearCallingIdentity();
                try {
                    if (installed) {
                        this.mInstallPackageHelper.installExistingPackageAsUser(packageName, userId, 4194304, 3, null, null);
                        Binder.restoreCallingIdentity(callingId);
                        return true;
                    }
                    try {
                        deletePackageVersioned(new VersionedPackage(packageName, -1), new PackageManager.LegacyPackageDeleteObserver((IPackageDeleteObserver) null).getBinder(), userId, 4);
                        Binder.restoreCallingIdentity(callingId);
                        return true;
                    } catch (Throwable th) {
                        th = th;
                        Binder.restoreCallingIdentity(callingId);
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishPackageInstall(int token, boolean didLaunch) {
        PackageManagerServiceUtils.enforceSystemOrRoot("Only the system is allowed to finish installs");
        if (DEBUG_INSTALL) {
            Slog.v(TAG, "BM finishing package install for " + token);
        }
        Trace.asyncTraceEnd(262144L, "restore", token);
        Message msg = this.mHandler.obtainMessage(9, token, didLaunch ? 1 : 0);
        this.mHandler.sendMessage(msg);
    }

    void checkPackageStartable(Computer snapshot, String packageName, int userId) {
        int callingUid = Binder.getCallingUid();
        if (snapshot.getInstantAppPackageName(callingUid) != null) {
            throw new SecurityException("Instant applications don't have access to this method");
        }
        if (!this.mUserManager.exists(userId)) {
            throw new SecurityException("User doesn't exist");
        }
        snapshot.enforceCrossUserPermission(callingUid, userId, false, false, "checkPackageStartable");
        switch (snapshot.getPackageStartability(this.mSafeMode, packageName, callingUid, userId)) {
            case 1:
                throw new SecurityException("Package " + packageName + " was not found!");
            case 2:
                throw new SecurityException("Package " + packageName + " not a system app!");
            case 3:
                throw new SecurityException("Package " + packageName + " is currently frozen!");
            case 4:
                throw new SecurityException("Package " + packageName + " is not encryption aware!");
            default:
                return;
        }
    }

    void setPackageStoppedState(Computer snapshot, final String packageName, final boolean stopped, final int userId) {
        String installerPackageName;
        if (this.mUserManager.exists(userId)) {
            int callingUid = Binder.getCallingUid();
            if (snapshot.getInstantAppPackageName(callingUid) == null) {
                int permission = this.mContext.checkCallingOrSelfPermission("android.permission.CHANGE_COMPONENT_ENABLED_STATE");
                boolean allowedByPermission = permission == 0;
                if (!allowedByPermission && !ArrayUtils.contains(snapshot.getPackagesForUid(callingUid), packageName)) {
                    throw new SecurityException("Permission Denial: attempt to change stopped state from pid=" + Binder.getCallingPid() + ", uid=" + callingUid + ", package=" + packageName);
                }
                snapshot.enforceCrossUserPermission(callingUid, userId, true, true, "stop package");
                PackageStateInternal packageState = snapshot.getPackageStateInternal(packageName);
                PackageUserState packageUserState = packageState == null ? null : packageState.getUserStateOrDefault(userId);
                if (packageState != null && !snapshot.shouldFilterApplication(packageState, callingUid, userId) && packageUserState.isStopped() != stopped) {
                    final boolean wasNotLaunched = packageUserState.isNotLaunched();
                    commitPackageStateMutation(null, packageName, new Consumer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda22
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            PackageManagerService.lambda$setPackageStoppedState$52(userId, stopped, wasNotLaunched, (PackageStateWrite) obj);
                        }
                    });
                    if (wasNotLaunched && (installerPackageName = packageState.getInstallSource().installerPackageName) != null) {
                        notifyFirstLaunch(packageName, installerPackageName, userId);
                    }
                    scheduleWritePackageRestrictions(userId);
                }
            }
            if (!stopped) {
                this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda23
                    @Override // java.lang.Runnable
                    public final void run() {
                        PackageManagerService.this.m5571x6e442577(packageName, userId);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$setPackageStoppedState$52(int userId, boolean stopped, boolean wasNotLaunched, PackageStateWrite state) {
        PackageUserStateWrite userState = state.userState(userId);
        userState.setStopped(stopped);
        if (wasNotLaunched) {
            userState.setNotLaunched(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setPackageStoppedState$53$com-android-server-pm-PackageManagerService  reason: not valid java name */
    public /* synthetic */ void m5571x6e442577(String packageName, int userId) {
        AppHibernationManagerInternal ah = (AppHibernationManagerInternal) this.mInjector.getLocalService(AppHibernationManagerInternal.class);
        if (ah != null && ah.isHibernatingForUser(packageName, userId)) {
            ah.setHibernatingForUser(packageName, userId, false);
            ah.setHibernatingGlobally(packageName, false);
        }
    }

    /* loaded from: classes2.dex */
    public class IPackageManagerImpl extends IPackageManagerBase {
        public IPackageManagerImpl() {
            super(PackageManagerService.this, PackageManagerService.this.mContext, PackageManagerService.this.mDexOptHelper, PackageManagerService.this.mModuleInfoProvider, PackageManagerService.this.mPreferredActivityHelper, PackageManagerService.this.mResolveIntentHelper, PackageManagerService.this.mDomainVerificationManager, PackageManagerService.this.mDomainVerificationConnection, PackageManagerService.this.mInstallerService, PackageManagerService.this.mPackageProperty, PackageManagerService.this.mResolveComponentName, PackageManagerService.this.mInstantAppResolverSettingsComponent, PackageManagerService.this.mRequiredSdkSandboxPackage, PackageManagerService.this.mServicesExtensionPackageName, PackageManagerService.this.mSharedSystemSharedLibraryPackageName);
        }

        public void checkPackageStartable(String packageName, int userId) {
            PackageManagerService packageManagerService = PackageManagerService.this;
            packageManagerService.checkPackageStartable(packageManagerService.snapshotComputer(), packageName, userId);
        }

        public void clearApplicationProfileData(String packageName) {
            PackageManagerServiceUtils.enforceSystemOrRoot("Only the system can clear all profile data");
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            AndroidPackage pkg = snapshot.getPackage(packageName);
            PackageFreezer ignored = PackageManagerService.this.freezePackage(packageName, "clearApplicationProfileData");
            try {
                synchronized (PackageManagerService.this.mInstallLock) {
                    PackageManagerService.this.mAppDataHelper.clearAppProfilesLIF(pkg);
                }
                if (ignored != null) {
                    ignored.close();
                }
            } catch (Throwable th) {
                if (ignored != null) {
                    try {
                        ignored.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }

        public void clearApplicationUserData(final String packageName, final IPackageDataObserver observer, final int userId) {
            PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CLEAR_APP_USER_DATA", null);
            int callingUid = Binder.getCallingUid();
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            snapshot.enforceCrossUserPermission(callingUid, userId, true, false, "clear application data");
            if (snapshot.getPackageStateFiltered(packageName, callingUid, userId) == null) {
                if (observer != null) {
                    PackageManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerService$IPackageManagerImpl$$ExternalSyntheticLambda12
                        @Override // java.lang.Runnable
                        public final void run() {
                            PackageManagerService.IPackageManagerImpl.lambda$clearApplicationUserData$0(observer, packageName);
                        }
                    });
                }
            } else if (PackageManagerService.this.mProtectedPackages.isPackageDataProtected(userId, packageName)) {
                throw new SecurityException("Cannot clear data for a protected package: " + packageName);
            } else {
                PackageManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerService.IPackageManagerImpl.1
                    @Override // java.lang.Runnable
                    public void run() {
                        boolean succeeded;
                        PackageManagerService.this.mHandler.removeCallbacks(this);
                        PackageFreezer freezer = PackageManagerService.this.freezePackage(packageName, "clearApplicationUserData");
                        try {
                            synchronized (PackageManagerService.this.mInstallLock) {
                                succeeded = PackageManagerService.this.clearApplicationUserDataLIF(PackageManagerService.this.snapshotComputer(), packageName, userId);
                            }
                            PackageManagerService.this.mInstantAppRegistry.deleteInstantApplicationMetadata(packageName, userId);
                            synchronized (PackageManagerService.this.mLock) {
                                if (succeeded) {
                                    PackageManagerService.this.resetComponentEnabledSettingsIfNeededLPw(packageName, userId);
                                }
                            }
                            if (freezer != null) {
                                freezer.close();
                            }
                            if (succeeded) {
                                DeviceStorageMonitorInternal dsm = (DeviceStorageMonitorInternal) LocalServices.getService(DeviceStorageMonitorInternal.class);
                                if (dsm != null) {
                                    dsm.checkMemory();
                                }
                                if (IPackageManagerImpl.this.checkPermission("android.permission.SUSPEND_APPS", packageName, userId) == 0) {
                                    Computer snapshot2 = PackageManagerService.this.snapshotComputer();
                                    PackageManagerService.this.unsuspendForSuspendingPackage(snapshot2, packageName, userId);
                                    PackageManagerService.this.removeAllDistractingPackageRestrictions(snapshot2, userId);
                                    synchronized (PackageManagerService.this.mLock) {
                                        PackageManagerService.this.flushPackageRestrictionsAsUserInternalLocked(userId);
                                    }
                                }
                            }
                            IPackageDataObserver iPackageDataObserver = observer;
                            if (iPackageDataObserver != null) {
                                try {
                                    iPackageDataObserver.onRemoveCompleted(packageName, succeeded);
                                } catch (RemoteException e) {
                                    Log.i(PackageManagerService.TAG, "Observer no longer exists.");
                                }
                            }
                        } catch (Throwable th) {
                            if (freezer != null) {
                                try {
                                    freezer.close();
                                } catch (Throwable th2) {
                                    th.addSuppressed(th2);
                                }
                            }
                            throw th;
                        }
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$clearApplicationUserData$0(IPackageDataObserver observer, String packageName) {
            try {
                observer.onRemoveCompleted(packageName, false);
            } catch (RemoteException e) {
                Log.i(PackageManagerService.TAG, "Observer no longer exists.");
            }
        }

        public void clearCrossProfileIntentFilters(int sourceUserId, String ownerPackage) {
            PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
            int callingUid = Binder.getCallingUid();
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            PackageManagerService.this.enforceOwnerRights(snapshot, ownerPackage, callingUid);
            PackageManagerServiceUtils.enforceShellRestriction(PackageManagerService.this.mInjector.getUserManagerInternal(), "no_debugging_features", callingUid, sourceUserId);
            synchronized (PackageManagerService.this.mLock) {
                CrossProfileIntentResolver resolver = PackageManagerService.this.mSettings.editCrossProfileIntentResolverLPw(sourceUserId);
                ArraySet<CrossProfileIntentFilter> set = new ArraySet<>(resolver.filterSet());
                Iterator<CrossProfileIntentFilter> it = set.iterator();
                while (it.hasNext()) {
                    CrossProfileIntentFilter filter = it.next();
                    if (filter.getOwnerPackage().equals(ownerPackage)) {
                        resolver.removeFilter((CrossProfileIntentResolver) filter);
                    }
                }
            }
            PackageManagerService.this.scheduleWritePackageRestrictions(sourceUserId);
        }

        public final void deleteApplicationCacheFiles(String packageName, IPackageDataObserver observer) {
            int userId = UserHandle.getCallingUserId();
            deleteApplicationCacheFilesAsUser(packageName, userId, observer);
        }

        public void deleteApplicationCacheFilesAsUser(final String packageName, final int userId, final IPackageDataObserver observer) {
            final int callingUid = Binder.getCallingUid();
            if (PackageManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.INTERNAL_DELETE_CACHE_FILES") != 0) {
                if (PackageManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.DELETE_CACHE_FILES") == 0) {
                    Slog.w(PackageManagerService.TAG, "Calling uid " + callingUid + " does not have android.permission.INTERNAL_DELETE_CACHE_FILES, silently ignoring");
                    return;
                }
                PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.INTERNAL_DELETE_CACHE_FILES", null);
            }
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            snapshot.enforceCrossUserPermission(callingUid, userId, true, false, "delete application cache files");
            final int hasAccessInstantApps = PackageManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.ACCESS_INSTANT_APPS");
            PackageManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerService$IPackageManagerImpl$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    PackageManagerService.IPackageManagerImpl.this.m5573x450625cd(packageName, callingUid, hasAccessInstantApps, userId, observer);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$deleteApplicationCacheFilesAsUser$1$com-android-server-pm-PackageManagerService$IPackageManagerImpl  reason: not valid java name */
        public /* synthetic */ void m5573x450625cd(String packageName, int callingUid, int hasAccessInstantApps, int userId, IPackageDataObserver observer) {
            Computer newSnapshot = PackageManagerService.this.snapshotComputer();
            PackageStateInternal ps = newSnapshot.getPackageStateInternal(packageName);
            boolean doClearData = true;
            if (ps != null) {
                boolean targetIsInstantApp = ps.getUserStateOrDefault(UserHandle.getUserId(callingUid)).isInstantApp();
                doClearData = !targetIsInstantApp || hasAccessInstantApps == 0;
            }
            if (doClearData) {
                synchronized (PackageManagerService.this.mInstallLock) {
                    AndroidPackage pkg = PackageManagerService.this.snapshotComputer().getPackage(packageName);
                    PackageManagerService.this.mAppDataHelper.clearAppDataLIF(pkg, userId, 23);
                    PackageManagerService.this.mAppDataHelper.clearAppDataLIF(pkg, userId, 39);
                }
            }
            if (observer != null) {
                try {
                    observer.onRemoveCompleted(packageName, true);
                } catch (RemoteException e) {
                    Log.i(PackageManagerService.TAG, "Observer no longer exists.");
                }
            }
        }

        public void dumpProfiles(String packageName, boolean dumpClassesAndMethods) {
            int callingUid = Binder.getCallingUid();
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            String[] callerPackageNames = snapshot.getPackagesForUid(callingUid);
            if (callingUid != 2000 && callingUid != 0 && !ArrayUtils.contains(callerPackageNames, packageName)) {
                throw new SecurityException("dumpProfiles");
            }
            AndroidPackage pkg = snapshot.getPackage(packageName);
            if (pkg == null) {
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
            synchronized (PackageManagerService.this.mInstallLock) {
                Trace.traceBegin(262144L, "dump profiles");
                PackageManagerService.this.mArtManagerService.dumpProfiles(pkg, dumpClassesAndMethods);
                Trace.traceEnd(262144L);
            }
        }

        public void enterSafeMode() {
            PackageManagerServiceUtils.enforceSystemOrRoot("Only the system can request entering safe mode");
            if (!PackageManagerService.this.mSystemReady) {
                PackageManagerService.this.mSafeMode = true;
            }
        }

        public void extendVerificationTimeout(final int id, final int verificationCodeAtTimeout, final long millisecondsToDelay) {
            PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_VERIFICATION_AGENT", "Only package verification agents can extend verification timeouts");
            final int callingUid = Binder.getCallingUid();
            PackageManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerService$IPackageManagerImpl$$ExternalSyntheticLambda13
                @Override // java.lang.Runnable
                public final void run() {
                    PackageManagerService.IPackageManagerImpl.this.m5574x9627b883(id, verificationCodeAtTimeout, callingUid, millisecondsToDelay);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$extendVerificationTimeout$2$com-android-server-pm-PackageManagerService$IPackageManagerImpl  reason: not valid java name */
        public /* synthetic */ void m5574x9627b883(int id, int verificationCodeAtTimeout, int callingUid, long millisecondsToDelay) {
            PackageVerificationState state = PackageManagerService.this.mPendingVerification.get(id);
            PackageVerificationResponse response = new PackageVerificationResponse(verificationCodeAtTimeout, callingUid);
            long delay = millisecondsToDelay;
            if (delay > 3600000) {
                delay = 3600000;
            }
            if (delay < 0) {
                delay = 0;
            }
            if (state != null && !state.timeoutExtended()) {
                state.extendTimeout();
                Message msg = PackageManagerService.this.mHandler.obtainMessage(15);
                msg.arg1 = id;
                msg.obj = response;
                PackageManagerService.this.mHandler.sendMessageDelayed(msg, delay);
            }
        }

        public void flushPackageRestrictionsAsUser(int userId) {
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            int callingUid = Binder.getCallingUid();
            if (snapshot.getInstantAppPackageName(callingUid) != null || !PackageManagerService.this.mUserManager.exists(userId)) {
                return;
            }
            snapshot.enforceCrossUserPermission(callingUid, userId, false, false, "flushPackageRestrictions");
            synchronized (PackageManagerService.this.mLock) {
                PackageManagerService.this.flushPackageRestrictionsAsUserInternalLocked(userId);
            }
        }

        public void freeStorage(final String volumeUuid, final long freeStorageSize, final int flags, final IntentSender pi) {
            PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CLEAR_APP_CACHE", PackageManagerService.TAG);
            PackageManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerService$IPackageManagerImpl$$ExternalSyntheticLambda14
                @Override // java.lang.Runnable
                public final void run() {
                    PackageManagerService.IPackageManagerImpl.this.m5575x576fde27(volumeUuid, freeStorageSize, flags, pi);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* JADX WARN: Removed duplicated region for block: B:25:0x0020 A[EXC_TOP_SPLITTER, SYNTHETIC] */
        /* JADX WARN: Removed duplicated region for block: B:27:? A[RETURN, SYNTHETIC] */
        /* renamed from: lambda$freeStorage$3$com-android-server-pm-PackageManagerService$IPackageManagerImpl  reason: not valid java name */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public /* synthetic */ void m5575x576fde27(String volumeUuid, long freeStorageSize, int flags, IntentSender pi) {
            boolean success = false;
            try {
            } catch (IOException e) {
                e = e;
            }
            try {
                PackageManagerService.this.freeStorage(volumeUuid, freeStorageSize, flags);
                success = true;
            } catch (IOException e2) {
                e = e2;
                Slog.w(PackageManagerService.TAG, e);
                if (pi == null) {
                }
            }
            if (pi == null) {
                try {
                    BroadcastOptions options = BroadcastOptions.makeBasic();
                    options.setPendingIntentBackgroundActivityLaunchAllowed(false);
                    pi.sendIntent(null, success ? 1 : 0, null, null, null, null, options.toBundle());
                } catch (IntentSender.SendIntentException e3) {
                    Slog.w(PackageManagerService.TAG, e3);
                }
            }
        }

        public void freeStorageAndNotify(final String volumeUuid, final long freeStorageSize, final int flags, final IPackageDataObserver observer) {
            PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.CLEAR_APP_CACHE", null);
            PackageManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerService$IPackageManagerImpl$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    PackageManagerService.IPackageManagerImpl.this.m5576xe13b7124(volumeUuid, freeStorageSize, flags, observer);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$freeStorageAndNotify$4$com-android-server-pm-PackageManagerService$IPackageManagerImpl  reason: not valid java name */
        public /* synthetic */ void m5576xe13b7124(String volumeUuid, long freeStorageSize, int flags, IPackageDataObserver observer) {
            boolean success = false;
            try {
                PackageManagerService.this.freeStorage(volumeUuid, freeStorageSize, flags);
                success = true;
            } catch (IOException e) {
                Slog.w(PackageManagerService.TAG, e);
            }
            if (observer != null) {
                try {
                    observer.onRemoveCompleted((String) null, success);
                } catch (RemoteException e2) {
                    Slog.w(PackageManagerService.TAG, e2);
                }
            }
        }

        public ChangedPackages getChangedPackages(int sequenceNumber, int userId) {
            int callingUid = Binder.getCallingUid();
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            if (snapshot.getInstantAppPackageName(callingUid) == null && PackageManagerService.this.mUserManager.exists(userId)) {
                snapshot.enforceCrossUserPermission(callingUid, userId, false, false, "getChangedPackages");
                ChangedPackages changedPackages = PackageManagerService.this.mChangedPackagesTracker.getChangedPackages(sequenceNumber, userId);
                if (changedPackages != null) {
                    List<String> packageNames = changedPackages.getPackageNames();
                    for (int index = packageNames.size() - 1; index >= 0; index--) {
                        PackageStateInternal packageState = snapshot.getPackageStateInternal(packageNames.get(index));
                        if (snapshot.shouldFilterApplication(packageState, callingUid, userId)) {
                            packageNames.remove(index);
                        }
                    }
                }
                return changedPackages;
            }
            return null;
        }

        public byte[] getDomainVerificationBackup(int userId) {
            if (Binder.getCallingUid() != 1000) {
                throw new SecurityException("Only the system may call getDomainVerificationBackup()");
            }
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                TypedXmlSerializer serializer = Xml.resolveSerializer(output);
                PackageManagerService.this.mDomainVerificationManager.writeSettings(PackageManagerService.this.snapshotComputer(), serializer, true, userId);
                byte[] byteArray = output.toByteArray();
                output.close();
                return byteArray;
            } catch (Exception e) {
                if (PackageManagerService.DEBUG_BACKUP) {
                    Slog.e(PackageManagerService.TAG, "Unable to write domain verification for backup", e);
                    return null;
                }
                return null;
            }
        }

        /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: com.android.server.pm.PackageManagerService$IPackageManagerImpl */
        /* JADX WARN: Multi-variable type inference failed */
        public IBinder getHoldLockToken() {
            if (!Build.IS_DEBUGGABLE) {
                throw new SecurityException("getHoldLockToken requires a debuggable build");
            }
            PackageManagerService.this.mContext.enforceCallingPermission("android.permission.INJECT_EVENTS", "getHoldLockToken requires INJECT_EVENTS permission");
            Binder token = new Binder();
            token.attachInterface(this, "holdLock:" + Binder.getCallingUid());
            return token;
        }

        public String getInstantAppAndroidId(String packageName, int userId) {
            PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_INSTANT_APPS", "getInstantAppAndroidId");
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            snapshot.enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "getInstantAppAndroidId");
            if (!snapshot.isInstantApp(packageName, userId)) {
                return null;
            }
            return PackageManagerService.this.mInstantAppRegistry.getInstantAppAndroidId(packageName, userId);
        }

        public byte[] getInstantAppCookie(String packageName, int userId) {
            PackageStateInternal packageState;
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            snapshot.enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "getInstantAppCookie");
            if (!snapshot.isCallerSameApp(packageName, Binder.getCallingUid()) || (packageState = snapshot.getPackageStateInternal(packageName)) == null || packageState.getPkg() == null) {
                return null;
            }
            return PackageManagerService.this.mInstantAppRegistry.getInstantAppCookie(packageState.getPkg(), userId);
        }

        public Bitmap getInstantAppIcon(String packageName, int userId) {
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            if (!snapshot.canViewInstantApps(Binder.getCallingUid(), userId)) {
                PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_INSTANT_APPS", "getInstantAppIcon");
            }
            snapshot.enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "getInstantAppIcon");
            return PackageManagerService.this.mInstantAppRegistry.getInstantAppIcon(packageName, userId);
        }

        public ParceledListSlice<InstantAppInfo> getInstantApps(int userId) {
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            if (!snapshot.canViewInstantApps(Binder.getCallingUid(), userId)) {
                PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_INSTANT_APPS", "getEphemeralApplications");
            }
            snapshot.enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "getEphemeralApplications");
            List<InstantAppInfo> instantApps = PackageManagerService.this.mInstantAppRegistry.getInstantApps(snapshot, userId);
            if (instantApps != null) {
                return new ParceledListSlice<>(instantApps);
            }
            return null;
        }

        public ResolveInfo getLastChosenActivity(Intent intent, String resolvedType, int flags) {
            return PackageManagerService.this.mPreferredActivityHelper.getLastChosenActivity(PackageManagerService.this.snapshotComputer(), intent, resolvedType, flags);
        }

        public IntentSender getLaunchIntentSenderForPackage(String packageName, String callingPackage, String featureId, int userId) throws RemoteException {
            return PackageManagerService.this.mResolveIntentHelper.getLaunchIntentSenderForPackage(PackageManagerService.this.snapshotComputer(), packageName, callingPackage, featureId, userId);
        }

        public List<String> getMimeGroup(String packageName, String mimeGroup) {
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            PackageManagerService.this.enforceOwnerRights(snapshot, packageName, Binder.getCallingUid());
            return PackageManagerService.this.getMimeGroupInternal(snapshot, packageName, mimeGroup);
        }

        public int getMoveStatus(int moveId) {
            PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS", null);
            return PackageManagerService.this.mMoveCallbacks.mLastStatus.get(moveId);
        }

        public String getPermissionControllerPackageName() {
            int callingUid = Binder.getCallingUid();
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            if (snapshot.getPackageStateFiltered(PackageManagerService.this.mRequiredPermissionControllerPackage, callingUid, UserHandle.getUserId(callingUid)) != null) {
                return PackageManagerService.this.mRequiredPermissionControllerPackage;
            }
            throw new IllegalStateException("PermissionController is not found");
        }

        public int getRuntimePermissionsVersion(int userId) {
            Preconditions.checkArgumentNonnegative(userId);
            PackageManagerService.this.enforceAdjustRuntimePermissionsPolicyOrUpgradeRuntimePermissions("getRuntimePermissionVersion");
            return PackageManagerService.this.mSettings.getDefaultRuntimePermissionsVersion(userId);
        }

        public String getSplashScreenTheme(String packageName, int userId) {
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            PackageStateInternal packageState = PackageManagerService.this.filterPackageStateForInstalledAndFiltered(snapshot, packageName, Binder.getCallingUid(), userId);
            if (packageState == null) {
                return null;
            }
            return packageState.getUserStateOrDefault(userId).getSplashScreenTheme();
        }

        public Bundle getSuspendedPackageAppExtras(String packageName, int userId) {
            int callingUid = Binder.getCallingUid();
            Computer snapshot = snapshot();
            if (snapshot.getPackageUid(packageName, 0L, userId) != callingUid) {
                throw new SecurityException("Calling package " + packageName + " does not belong to calling uid " + callingUid);
            }
            return PackageManagerService.this.mSuspendPackageHelper.getSuspendedPackageAppExtras(snapshot, packageName, userId, callingUid);
        }

        public ParceledListSlice<FeatureInfo> getSystemAvailableFeatures() {
            ArrayList<FeatureInfo> res;
            synchronized (PackageManagerService.this.mAvailableFeatures) {
                res = new ArrayList<>(PackageManagerService.this.mAvailableFeatures.size() + 1);
                res.addAll(PackageManagerService.this.mAvailableFeatures.values());
            }
            FeatureInfo fi = new FeatureInfo();
            fi.reqGlEsVersion = SystemProperties.getInt("ro.opengles.version", 0);
            res.add(fi);
            return new ParceledListSlice<>(res);
        }

        public String[] getUnsuspendablePackagesForUser(String[] packageNames, int userId) {
            Objects.requireNonNull(packageNames, "packageNames cannot be null");
            PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.SUSPEND_APPS", "getUnsuspendablePackagesForUser");
            int callingUid = Binder.getCallingUid();
            if (UserHandle.getUserId(callingUid) != userId) {
                throw new SecurityException("Calling uid " + callingUid + " cannot query getUnsuspendablePackagesForUser for user " + userId);
            }
            return PackageManagerService.this.mSuspendPackageHelper.getUnsuspendablePackagesForUser(PackageManagerService.this.snapshotComputer(), packageNames, userId, callingUid);
        }

        public VerifierDeviceIdentity getVerifierDeviceIdentity() throws RemoteException {
            VerifierDeviceIdentity verifierDeviceIdentityLPw;
            PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_VERIFICATION_AGENT", "Only package verification agents can read the verifier device identity");
            synchronized (PackageManagerService.this.mLock) {
                verifierDeviceIdentityLPw = PackageManagerService.this.mSettings.getVerifierDeviceIdentityLPw(PackageManagerService.this.mLiveComputer);
            }
            return verifierDeviceIdentityLPw;
        }

        public void makeProviderVisible(int recipientUid, String visibleAuthority) {
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            int recipientUserId = UserHandle.getUserId(recipientUid);
            ProviderInfo providerInfo = snapshot.getGrantImplicitAccessProviderInfo(recipientUid, visibleAuthority);
            if (providerInfo == null) {
                return;
            }
            int visibleUid = providerInfo.applicationInfo.uid;
            PackageManagerService.this.grantImplicitAccess(snapshot, recipientUserId, null, UserHandle.getAppId(recipientUid), visibleUid, false, false);
        }

        public void makeUidVisible(int recipientUid, int visibleUid) {
            PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.MAKE_UID_VISIBLE", "makeUidVisible");
            int callingUid = Binder.getCallingUid();
            int recipientUserId = UserHandle.getUserId(recipientUid);
            int visibleUserId = UserHandle.getUserId(visibleUid);
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            snapshot.enforceCrossUserPermission(callingUid, recipientUserId, false, false, "makeUidVisible");
            snapshot.enforceCrossUserPermission(callingUid, visibleUserId, false, false, "makeUidVisible");
            snapshot.enforceCrossUserPermission(recipientUid, visibleUserId, false, false, "makeUidVisible");
            PackageManagerService.this.grantImplicitAccess(snapshot, recipientUserId, null, UserHandle.getAppId(recipientUid), visibleUid, false, false);
        }

        public void holdLock(IBinder token, int durationMs) {
            PackageManagerService.this.mTestUtilityService.verifyHoldLockToken(token);
            synchronized (PackageManagerService.this.mLock) {
                SystemClock.sleep(durationMs);
            }
        }

        public int installExistingPackageAsUser(String packageName, int userId, int installFlags, int installReason, List<String> whiteListedPermissions) {
            return PackageManagerService.this.mInstallPackageHelper.installExistingPackageAsUser(packageName, userId, installFlags, installReason, whiteListedPermissions, null);
        }

        public boolean isAutoRevokeWhitelisted(String packageName) {
            int mode = ((AppOpsManager) PackageManagerService.this.mInjector.getSystemService(AppOpsManager.class)).checkOpNoThrow(97, Binder.getCallingUid(), packageName);
            return mode == 1;
        }

        public boolean isPackageStateProtected(String packageName, int userId) {
            int callingUid = Binder.getCallingUid();
            int callingAppId = UserHandle.getAppId(callingUid);
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            snapshot.enforceCrossUserPermission(callingUid, userId, false, true, "isPackageStateProtected");
            if (callingAppId != 1000 && callingAppId != 0 && snapshot.checkUidPermission("android.permission.MANAGE_DEVICE_ADMINS", callingUid) != 0) {
                throw new SecurityException("Caller must have the android.permission.MANAGE_DEVICE_ADMINS permission.");
            }
            return PackageManagerService.this.mProtectedPackages.isPackageStateProtected(userId, packageName);
        }

        public boolean isProtectedBroadcast(String actionName) {
            boolean contains;
            if (actionName != null && (actionName.startsWith("android.net.netmon.lingerExpired") || actionName.startsWith("com.android.server.sip.SipWakeupTimer") || actionName.startsWith("com.android.internal.telephony.data-reconnect") || actionName.startsWith("android.net.netmon.launchCaptivePortalApp"))) {
                return true;
            }
            synchronized (PackageManagerService.this.mProtectedBroadcasts) {
                contains = PackageManagerService.this.mProtectedBroadcasts.contains(actionName);
            }
            return contains;
        }

        public void logAppProcessStartIfNeeded(String packageName, String processName, int uid, String seinfo, String apkFile, int pid) {
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            if (snapshot.getInstantAppPackageName(Binder.getCallingUid()) != null || !SecurityLog.isLoggingEnabled()) {
                return;
            }
            PackageManagerService.this.mProcessLoggingHandler.logAppProcessStart(PackageManagerService.this.mContext, (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class), apkFile, packageName, processName, uid, seinfo, pid);
        }

        public int movePackage(final String packageName, final String volumeUuid) {
            PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.MOVE_PACKAGE", null);
            final int callingUid = Binder.getCallingUid();
            final UserHandle user = new UserHandle(UserHandle.getUserId(callingUid));
            final int moveId = PackageManagerService.this.mNextMoveId.getAndIncrement();
            PackageManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerService$IPackageManagerImpl$$ExternalSyntheticLambda11
                @Override // java.lang.Runnable
                public final void run() {
                    PackageManagerService.IPackageManagerImpl.this.m5577x6c01656f(packageName, volumeUuid, moveId, callingUid, user);
                }
            });
            return moveId;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$movePackage$5$com-android-server-pm-PackageManagerService$IPackageManagerImpl  reason: not valid java name */
        public /* synthetic */ void m5577x6c01656f(String packageName, String volumeUuid, int moveId, int callingUid, UserHandle user) {
            try {
                MovePackageHelper movePackageHelper = new MovePackageHelper(PackageManagerService.this);
                movePackageHelper.movePackageInternal(packageName, volumeUuid, moveId, callingUid, user);
            } catch (PackageManagerException e) {
                Slog.w(PackageManagerService.TAG, "Failed to move " + packageName, e);
                PackageManagerService.this.mMoveCallbacks.notifyStatusChanged(moveId, e.error);
            }
        }

        public int movePrimaryStorage(String volumeUuid) throws RemoteException {
            PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.MOVE_PACKAGE", null);
            final int realMoveId = PackageManagerService.this.mNextMoveId.getAndIncrement();
            Bundle extras = new Bundle();
            extras.putString("android.os.storage.extra.FS_UUID", volumeUuid);
            PackageManagerService.this.mMoveCallbacks.notifyCreated(realMoveId, extras);
            IPackageMoveObserver callback = new IPackageMoveObserver.Stub() { // from class: com.android.server.pm.PackageManagerService.IPackageManagerImpl.2
                public void onCreated(int moveId, Bundle extras2) {
                }

                public void onStatusChanged(int moveId, int status, long estMillis) {
                    PackageManagerService.this.mMoveCallbacks.notifyStatusChanged(realMoveId, status, estMillis);
                }
            };
            StorageManager storage = (StorageManager) PackageManagerService.this.mInjector.getSystemService(StorageManager.class);
            storage.setPrimaryStorageUuid(volumeUuid, callback);
            return realMoveId;
        }

        public void notifyDexLoad(String loadingPackageName, Map<String, String> classLoaderContextMap, String loaderIsa) {
            int callingUid = Binder.getCallingUid();
            if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(loadingPackageName) && callingUid != 1000) {
                Slog.w(PackageManagerService.TAG, "Non System Server process reporting dex loads as system server. uid=" + callingUid);
                return;
            }
            int userId = UserHandle.getCallingUserId();
            ApplicationInfo ai = snapshot().getApplicationInfo(loadingPackageName, 0L, userId);
            if (ai == null) {
                Slog.w(PackageManagerService.TAG, "Loading a package that does not exist for the calling user. package=" + loadingPackageName + ", user=" + userId);
            } else {
                PackageManagerService.this.mDexManager.notifyDexLoad(ai, classLoaderContextMap, loaderIsa, userId, Process.isIsolated(callingUid));
            }
        }

        public void notifyPackageUse(String packageName, int reason) {
            boolean notify;
            int callingUid = Binder.getCallingUid();
            int callingUserId = UserHandle.getUserId(callingUid);
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            if (snapshot.getInstantAppPackageName(callingUid) != null) {
                notify = snapshot.isCallerSameApp(packageName, callingUid);
            } else {
                notify = !snapshot.isInstantAppInternal(packageName, callingUserId, 1000);
            }
            if (!notify) {
                return;
            }
            PackageManagerService.this.notifyPackageUseInternal(packageName, reason);
        }

        public void overrideLabelAndIcon(ComponentName componentName, String nonLocalizedLabel, int icon, int userId) {
            if (TextUtils.isEmpty(nonLocalizedLabel)) {
                throw new IllegalArgumentException("Override label should be a valid String");
            }
            PackageManagerService.this.updateComponentLabelIcon(componentName, nonLocalizedLabel, Integer.valueOf(icon), userId);
        }

        public int performDexOptWithStatusByOption(DexoptOptions option) {
            return PackageManagerService.this.mDexOptHelper.performDexOptWithStatus(option);
        }

        public ParceledListSlice<PackageManager.Property> queryProperty(String propertyName, int componentType) {
            Objects.requireNonNull(propertyName);
            final int callingUid = Binder.getCallingUid();
            final int callingUserId = UserHandle.getCallingUserId();
            final Computer snapshot = PackageManagerService.this.snapshotComputer();
            List<PackageManager.Property> result = PackageManagerService.this.mPackageProperty.queryProperty(propertyName, componentType, new Predicate() { // from class: com.android.server.pm.PackageManagerService$IPackageManagerImpl$$ExternalSyntheticLambda4
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return PackageManagerService.IPackageManagerImpl.lambda$queryProperty$6(Computer.this, callingUid, callingUserId, (String) obj);
                }
            });
            if (result == null) {
                return ParceledListSlice.emptyList();
            }
            return new ParceledListSlice<>(result);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ boolean lambda$queryProperty$6(Computer snapshot, int callingUid, int callingUserId, String packageName) {
            PackageStateInternal ps = snapshot.getPackageStateInternal(packageName);
            return snapshot.shouldFilterApplication(ps, callingUid, callingUserId);
        }

        public void reconcileSecondaryDexFiles(String packageName) {
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            if (snapshot.getInstantAppPackageName(Binder.getCallingUid()) != null || snapshot.isInstantAppInternal(packageName, UserHandle.getCallingUserId(), 1000)) {
                return;
            }
            PackageManagerService.this.mDexManager.reconcileSecondaryDexFiles(packageName);
        }

        public void registerDexModule(String packageName, final String dexModulePath, boolean isSharedModule, final IDexModuleRegisterCallback callback) {
            final DexManager.RegisterDexModuleResult result;
            int userId = UserHandle.getCallingUserId();
            ApplicationInfo ai = snapshot().getApplicationInfo(packageName, 0L, userId);
            if (ai == null) {
                Slog.w(PackageManagerService.TAG, "Registering a dex module for a package that does not exist for the calling user. package=" + packageName + ", user=" + userId);
                result = new DexManager.RegisterDexModuleResult(false, "Package not installed");
            } else {
                result = PackageManagerService.this.mDexManager.registerDexModule(ai, dexModulePath, isSharedModule, userId);
            }
            if (callback != null) {
                PackageManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerService$IPackageManagerImpl$$ExternalSyntheticLambda15
                    @Override // java.lang.Runnable
                    public final void run() {
                        PackageManagerService.IPackageManagerImpl.lambda$registerDexModule$7(callback, dexModulePath, result);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$registerDexModule$7(IDexModuleRegisterCallback callback, String dexModulePath, DexManager.RegisterDexModuleResult result) {
            try {
                callback.onDexModuleRegistered(dexModulePath, result.success, result.message);
            } catch (RemoteException e) {
                Slog.w(PackageManagerService.TAG, "Failed to callback after module registration " + dexModulePath, e);
            }
        }

        public void registerMoveCallback(IPackageMoveObserver callback) {
            PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS", null);
            PackageManagerService.this.mMoveCallbacks.register(callback);
        }

        public void restoreDomainVerification(byte[] backup, int userId) {
            if (Binder.getCallingUid() != 1000) {
                throw new SecurityException("Only the system may call restorePreferredActivities()");
            }
            try {
                ByteArrayInputStream input = new ByteArrayInputStream(backup);
                TypedXmlPullParser parser = Xml.resolvePullParser(input);
                PackageManagerService.this.mDomainVerificationManager.restoreSettings(PackageManagerService.this.snapshotComputer(), parser);
                input.close();
            } catch (Exception e) {
                if (PackageManagerService.DEBUG_BACKUP) {
                    Slog.e(PackageManagerService.TAG, "Exception restoring domain verification: " + e.getMessage());
                }
            }
        }

        public void restoreLabelAndIcon(ComponentName componentName, int userId) {
            PackageManagerService.this.updateComponentLabelIcon(componentName, null, null, userId);
        }

        public void sendDeviceCustomizationReadyBroadcast() {
            PackageManagerService.this.mContext.enforceCallingPermission("android.permission.SEND_DEVICE_CUSTOMIZATION_READY", "sendDeviceCustomizationReadyBroadcast");
            long ident = Binder.clearCallingIdentity();
            try {
                BroadcastHelper.sendDeviceCustomizationReadyBroadcast();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void setApplicationCategoryHint(final String packageName, final int categoryHint, final String callerPackageName) {
            FunctionalUtils.ThrowingBiFunction<PackageStateMutator.InitialState, Computer, PackageStateMutator.Result> implementation = new FunctionalUtils.ThrowingBiFunction() { // from class: com.android.server.pm.PackageManagerService$IPackageManagerImpl$$ExternalSyntheticLambda17
                public final Object applyOrThrow(Object obj, Object obj2) {
                    return PackageManagerService.IPackageManagerImpl.this.m5578xa20ac3eb(callerPackageName, packageName, categoryHint, (PackageStateMutator.InitialState) obj, (Computer) obj2);
                }
            };
            PackageStateMutator.Result result = (PackageStateMutator.Result) implementation.apply(PackageManagerService.this.recordInitialState(), PackageManagerService.this.snapshotComputer());
            if (result != null && result.isStateChanged() && !result.isSpecificPackageNull()) {
                synchronized (PackageManagerService.this.mPackageStateWriteLock) {
                    result = (PackageStateMutator.Result) implementation.apply(PackageManagerService.this.recordInitialState(), PackageManagerService.this.snapshotComputer());
                }
            }
            if (result != null && result.isCommitted()) {
                PackageManagerService.this.scheduleWriteSettings();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$setApplicationCategoryHint$9$com-android-server-pm-PackageManagerService$IPackageManagerImpl  reason: not valid java name */
        public /* synthetic */ PackageStateMutator.Result m5578xa20ac3eb(String callerPackageName, String packageName, final int categoryHint, PackageStateMutator.InitialState initialState, Computer computer) throws Exception {
            if (computer.getInstantAppPackageName(Binder.getCallingUid()) != null) {
                throw new SecurityException("Instant applications don't have access to this method");
            }
            ((AppOpsManager) PackageManagerService.this.mInjector.getSystemService(AppOpsManager.class)).checkPackage(Binder.getCallingUid(), callerPackageName);
            PackageStateInternal packageState = computer.getPackageStateFiltered(packageName, Binder.getCallingUid(), UserHandle.getCallingUserId());
            if (packageState == null) {
                throw new IllegalArgumentException("Unknown target package " + packageName);
            }
            if (!Objects.equals(callerPackageName, packageState.getInstallSource().installerPackageName)) {
                throw new IllegalArgumentException("Calling package " + callerPackageName + " is not installer for " + packageName);
            }
            if (packageState.getCategoryOverride() != categoryHint) {
                return PackageManagerService.this.commitPackageStateMutation(initialState, packageName, new Consumer() { // from class: com.android.server.pm.PackageManagerService$IPackageManagerImpl$$ExternalSyntheticLambda3
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((PackageStateWrite) obj).setCategoryOverride(categoryHint);
                    }
                });
            }
            return null;
        }

        public void setApplicationEnabledSetting(String appPackageName, int newState, int flags, int userId, String callingPackage) {
            if (PackageManagerService.this.mUserManager.exists(userId)) {
                if (callingPackage == null) {
                    callingPackage = Integer.toString(Binder.getCallingUid());
                }
                Pair<Boolean, String> result = IPackageManagerServiceLice.Instance().isHiddenByXhide(appPackageName, userId, callingPackage);
                if (((Boolean) result.first).booleanValue()) {
                    appPackageName = (String) result.second;
                }
                PackageManagerService.this.setEnabledSettings(List.of(new PackageManager.ComponentEnabledSetting(appPackageName, newState, flags)), userId, callingPackage);
            }
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [5790=7] */
        public boolean setApplicationHiddenSettingAsUser(String packageName, final boolean hidden, final int userId) {
            PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USERS", null);
            int callingUid = Binder.getCallingUid();
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            snapshot.enforceCrossUserPermission(callingUid, userId, true, true, "setApplicationHiddenSetting for user " + userId);
            if (hidden && PackageManagerService.this.isPackageDeviceAdmin(packageName, userId)) {
                Slog.w(PackageManagerService.TAG, "Not hiding package " + packageName + ": has active device admin");
                return false;
            }
            ITranPackageManagerService.Instance().setApplicationHiddenSettingAsUser(packageName, hidden, userId);
            if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(packageName)) {
                Slog.w(PackageManagerService.TAG, "Cannot hide package: android");
                return false;
            }
            long callingId = Binder.clearCallingIdentity();
            try {
                PackageStateInternal packageState = snapshot.getPackageStateFiltered(packageName, callingUid, userId);
                if (packageState == null) {
                    return false;
                }
                AndroidPackage pkg = packageState.getPkg();
                if (pkg != null) {
                    if (pkg.getSdkLibName() != null) {
                        Slog.w(PackageManagerService.TAG, "Cannot hide package: " + packageName + " providing SDK library: " + pkg.getSdkLibName());
                        return false;
                    } else if (pkg.getStaticSharedLibName() != null) {
                        Slog.w(PackageManagerService.TAG, "Cannot hide package: " + packageName + " providing static shared library: " + pkg.getStaticSharedLibName());
                        return false;
                    }
                }
                if (hidden && !UserHandle.isSameApp(callingUid, packageState.getAppId()) && PackageManagerService.this.mProtectedPackages.isPackageStateProtected(userId, packageName)) {
                    Slog.w(PackageManagerService.TAG, "Not hiding protected package: " + packageName);
                    return false;
                } else if (packageState.getUserStateOrDefault(userId).isHidden() == hidden) {
                    return false;
                } else {
                    PackageManagerService.this.commitPackageStateMutation(null, packageName, new Consumer() { // from class: com.android.server.pm.PackageManagerService$IPackageManagerImpl$$ExternalSyntheticLambda5
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((PackageStateWrite) obj).userState(userId).setHidden(hidden);
                        }
                    });
                    Computer newSnapshot = PackageManagerService.this.snapshotComputer();
                    PackageStateInternal newPackageState = newSnapshot.getPackageStateInternal(packageName);
                    if (hidden) {
                        PackageManagerService.this.killApplication(packageName, newPackageState.getAppId(), userId, "hiding pkg");
                        PackageManagerService.this.sendApplicationHiddenForUser(packageName, newPackageState, userId);
                    } else {
                        PackageManagerService.this.sendPackageAddedForUser(newSnapshot, packageName, newPackageState, userId, 0);
                    }
                    PackageManagerService.this.scheduleWritePackageRestrictions(userId);
                    return true;
                }
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        }

        public boolean setBlockUninstallForUser(String packageName, boolean blockUninstall, int userId) {
            PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DELETE_PACKAGES", null);
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            PackageStateInternal packageState = snapshot.getPackageStateInternal(packageName);
            if (packageState != null && packageState.getPkg() != null) {
                AndroidPackage pkg = packageState.getPkg();
                if (pkg.getSdkLibName() != null) {
                    Slog.w(PackageManagerService.TAG, "Cannot block uninstall of package: " + packageName + " providing SDK library: " + pkg.getSdkLibName());
                    return false;
                } else if (pkg.getStaticSharedLibName() != null) {
                    Slog.w(PackageManagerService.TAG, "Cannot block uninstall of package: " + packageName + " providing static shared library: " + pkg.getStaticSharedLibName());
                    return false;
                }
            }
            synchronized (PackageManagerService.this.mLock) {
                PackageManagerService.this.mSettings.setBlockUninstallLPw(userId, packageName, blockUninstall);
            }
            PackageManagerService.this.scheduleWritePackageRestrictions(userId);
            return true;
        }

        public void setComponentEnabledSetting(ComponentName componentName, int newState, int flags, int userId) {
            if (PackageManagerService.this.mUserManager.exists(userId)) {
                PackageManagerService.this.setEnabledSettings(List.of(new PackageManager.ComponentEnabledSetting(componentName, newState, flags)), userId, null);
            }
        }

        public void setComponentEnabledSettings(List<PackageManager.ComponentEnabledSetting> settings, int userId) {
            if (PackageManagerService.this.mUserManager.exists(userId)) {
                if (settings == null || settings.isEmpty()) {
                    throw new IllegalArgumentException("The list of enabled settings is empty");
                }
                PackageManagerService.this.setEnabledSettings(settings, userId, null);
            }
        }

        public String[] setDistractingPackageRestrictionsAsUser(String[] packageNames, int restrictionFlags, int userId) {
            int callingUid = Binder.getCallingUid();
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            PackageManagerService.this.enforceCanSetDistractingPackageRestrictionsAsUser(callingUid, userId, "setDistractingPackageRestrictionsAsUser");
            Objects.requireNonNull(packageNames, "packageNames cannot be null");
            return PackageManagerService.this.mDistractingPackageHelper.setDistractingPackageRestrictionsAsUser(snapshot, packageNames, restrictionFlags, userId, callingUid);
        }

        public void setHarmfulAppWarning(String packageName, final CharSequence warning, final int userId) {
            int callingUid = Binder.getCallingUid();
            int callingAppId = UserHandle.getAppId(callingUid);
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            snapshot.enforceCrossUserPermission(callingUid, userId, true, true, "setHarmfulAppInfo");
            if (callingAppId != 1000 && callingAppId != 0 && snapshot.checkUidPermission("android.permission.SET_HARMFUL_APP_WARNINGS", callingUid) != 0) {
                throw new SecurityException("Caller must have the android.permission.SET_HARMFUL_APP_WARNINGS permission.");
            }
            PackageStateMutator.Result result = PackageManagerService.this.commitPackageStateMutation(null, packageName, new Consumer() { // from class: com.android.server.pm.PackageManagerService$IPackageManagerImpl$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((PackageStateWrite) obj).userState(userId).setHarmfulAppWarning(warning == null ? null : warning.toString());
                }
            });
            if (result.isSpecificPackageNull()) {
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
            PackageManagerService.this.scheduleWritePackageRestrictions(userId);
        }

        public boolean setInstallLocation(int loc) {
            PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS", null);
            if (getInstallLocation() == loc) {
                return true;
            }
            if (loc == 0 || loc == 1 || loc == 2) {
                Settings.Global.putInt(PackageManagerService.this.mContext.getContentResolver(), "default_install_location", loc);
                return true;
            }
            return false;
        }

        public void setInstallerPackageName(final String targetPackage, final String installerPackageName) {
            final int callingUid = Binder.getCallingUid();
            final int callingUserId = UserHandle.getUserId(callingUid);
            FunctionalUtils.ThrowingCheckedFunction<Computer, Boolean, RuntimeException> implementation = new FunctionalUtils.ThrowingCheckedFunction() { // from class: com.android.server.pm.PackageManagerService$IPackageManagerImpl$$ExternalSyntheticLambda7
                public final Object apply(Object obj) {
                    return PackageManagerService.IPackageManagerImpl.this.m5579xcad6a29d(callingUid, targetPackage, callingUserId, installerPackageName, (Computer) obj);
                }
            };
            PackageStateMutator.InitialState initialState = PackageManagerService.this.recordInitialState();
            boolean allowed = ((Boolean) implementation.apply(PackageManagerService.this.snapshotComputer())).booleanValue();
            if (allowed) {
                synchronized (PackageManagerService.this.mLock) {
                    PackageStateMutator.Result result = PackageManagerService.this.commitPackageStateMutation(initialState, targetPackage, new Consumer() { // from class: com.android.server.pm.PackageManagerService$IPackageManagerImpl$$ExternalSyntheticLambda8
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((PackageStateWrite) obj).setInstaller(installerPackageName);
                        }
                    });
                    if (result.isPackagesChanged() || result.isStateChanged()) {
                        synchronized (PackageManagerService.this.mPackageStateWriteLock) {
                            boolean allowed2 = ((Boolean) implementation.apply(PackageManagerService.this.snapshotComputer())).booleanValue();
                            if (!allowed2) {
                                return;
                            }
                            PackageManagerService.this.commitPackageStateMutation(null, targetPackage, new Consumer() { // from class: com.android.server.pm.PackageManagerService$IPackageManagerImpl$$ExternalSyntheticLambda9
                                @Override // java.util.function.Consumer
                                public final void accept(Object obj) {
                                    ((PackageStateWrite) obj).setInstaller(installerPackageName);
                                }
                            });
                        }
                    }
                    PackageStateInternal targetPackageState = PackageManagerService.this.snapshotComputer().getPackageStateInternal(targetPackage);
                    PackageManagerService.this.mSettings.addInstallerPackageNames(targetPackageState.getInstallSource());
                    PackageManagerService.this.mAppsFilter.addPackage(PackageManagerService.this.snapshotComputer(), targetPackageState);
                    PackageManagerService.this.scheduleWriteSettings();
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$setInstallerPackageName$12$com-android-server-pm-PackageManagerService$IPackageManagerImpl  reason: not valid java name */
        public /* synthetic */ Boolean m5579xcad6a29d(int callingUid, String targetPackage, int callingUserId, String installerPackageName, Computer snapshot) throws RuntimeException {
            PackageStateInternal installerPackageState;
            Signature[] callerSignature;
            if (snapshot.getInstantAppPackageName(callingUid) != null) {
                return false;
            }
            PackageStateInternal targetPackageState = snapshot.getPackageStateInternal(targetPackage);
            if (targetPackageState == null || snapshot.shouldFilterApplication(targetPackageState, callingUid, callingUserId)) {
                throw new IllegalArgumentException("Unknown target package: " + targetPackage);
            }
            if (installerPackageName == null) {
                installerPackageState = null;
            } else {
                PackageStateInternal installerPackageState2 = snapshot.getPackageStateInternal(installerPackageName);
                if (installerPackageState2 == null || snapshot.shouldFilterApplication(installerPackageState2, callingUid, callingUserId)) {
                    throw new IllegalArgumentException("Unknown installer package: " + installerPackageName);
                }
                installerPackageState = installerPackageState2;
            }
            int appId = UserHandle.getAppId(callingUid);
            Pair<PackageStateInternal, SharedUserApi> either = snapshot.getPackageOrSharedUser(appId);
            if (either != null) {
                if (either.first != null) {
                    callerSignature = ((PackageStateInternal) either.first).getSigningDetails().getSignatures();
                } else {
                    callerSignature = ((SharedUserApi) either.second).getSigningDetails().getSignatures();
                }
                if (installerPackageState != null && PackageManagerServiceUtils.compareSignatures(callerSignature, installerPackageState.getSigningDetails().getSignatures()) != 0) {
                    throw new SecurityException("Caller does not have same cert as new installer package " + installerPackageName);
                }
                String targetInstallerPackageName = targetPackageState.getInstallSource().installerPackageName;
                PackageStateInternal targetInstallerPkgSetting = targetInstallerPackageName == null ? null : snapshot.getPackageStateInternal(targetInstallerPackageName);
                if (targetInstallerPkgSetting != null) {
                    if (PackageManagerServiceUtils.compareSignatures(callerSignature, targetInstallerPkgSetting.getSigningDetails().getSignatures()) != 0) {
                        throw new SecurityException("Caller does not have same cert as old installer package " + targetInstallerPackageName);
                    }
                } else if (PackageManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_PACKAGES") != 0) {
                    EventLog.writeEvent(1397638484, "150857253", Integer.valueOf(callingUid), "");
                    long binderToken = Binder.clearCallingIdentity();
                    try {
                        if (!PackageManagerService.this.mInjector.getCompatibility().isChangeEnabledByUid(PackageManagerService.THROW_EXCEPTION_ON_REQUIRE_INSTALL_PACKAGES_TO_ADD_INSTALLER_PACKAGE, callingUid)) {
                            return false;
                        }
                        throw new SecurityException("Neither user " + callingUid + " nor current process has android.permission.INSTALL_PACKAGES");
                    } finally {
                        Binder.restoreCallingIdentity(binderToken);
                    }
                }
                return true;
            }
            throw new SecurityException("Unknown calling UID: " + callingUid);
        }

        public boolean setInstantAppCookie(String packageName, byte[] cookie, int userId) {
            PackageStateInternal packageState;
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            snapshot.enforceCrossUserPermission(Binder.getCallingUid(), userId, true, true, "setInstantAppCookie");
            if (!snapshot.isCallerSameApp(packageName, Binder.getCallingUid()) || (packageState = snapshot.getPackageStateInternal(packageName)) == null || packageState.getPkg() == null) {
                return false;
            }
            return PackageManagerService.this.mInstantAppRegistry.setInstantAppCookie(packageState.getPkg(), cookie, PackageManagerService.this.mContext.getPackageManager().getInstantAppCookieMaxBytes(), userId);
        }

        public void setKeepUninstalledPackages(List<String> packageList) {
            PackageManagerService.this.mContext.enforceCallingPermission("android.permission.KEEP_UNINSTALLED_PACKAGES", "setKeepUninstalledPackages requires KEEP_UNINSTALLED_PACKAGES permission");
            Objects.requireNonNull(packageList);
            PackageManagerService.this.setKeepUninstalledPackagesInternal(snapshot(), packageList);
        }

        public void setMimeGroup(final String packageName, final String mimeGroup, List<String> mimeTypes) {
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            PackageManagerService.this.enforceOwnerRights(snapshot, packageName, Binder.getCallingUid());
            List<String> mimeTypes2 = CollectionUtils.emptyIfNull(mimeTypes);
            for (String mimeType : mimeTypes2) {
                if (mimeType.length() > 255) {
                    throw new IllegalArgumentException("MIME type length exceeds 255 characters");
                }
            }
            PackageStateInternal packageState = snapshot.getPackageStateInternal(packageName);
            Set<String> existingMimeTypes = packageState.getMimeGroups().get(mimeGroup);
            if (existingMimeTypes == null) {
                throw new IllegalArgumentException("Unknown MIME group " + mimeGroup + " for package " + packageName);
            }
            if (existingMimeTypes.size() == mimeTypes2.size() && existingMimeTypes.containsAll(mimeTypes2)) {
                return;
            }
            if (mimeTypes2.size() > 500) {
                throw new IllegalStateException("Max limit on MIME types for MIME group " + mimeGroup + " exceeded for package " + packageName);
            }
            final ArraySet<String> mimeTypesSet = new ArraySet<>(mimeTypes2);
            PackageManagerService.this.commitPackageStateMutation(null, packageName, new Consumer() { // from class: com.android.server.pm.PackageManagerService$IPackageManagerImpl$$ExternalSyntheticLambda18
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((PackageStateWrite) obj).setMimeGroup(mimeGroup, mimeTypesSet);
                }
            });
            if (PackageManagerService.this.mComponentResolver.updateMimeGroup(PackageManagerService.this.snapshotComputer(), packageName, mimeGroup)) {
                Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.pm.PackageManagerService$IPackageManagerImpl$$ExternalSyntheticLambda19
                    public final void runOrThrow() {
                        PackageManagerService.IPackageManagerImpl.this.m5580x42bbfe69(packageName);
                    }
                });
            }
            PackageManagerService.this.scheduleWriteSettings();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$setMimeGroup$16$com-android-server-pm-PackageManagerService$IPackageManagerImpl  reason: not valid java name */
        public /* synthetic */ void m5580x42bbfe69(String packageName) throws Exception {
            PackageManagerService.this.mPreferredActivityHelper.clearPackagePreferredActivities(packageName, -1);
        }

        public void setPackageStoppedState(String packageName, boolean stopped, int userId) {
            PackageManagerService packageManagerService = PackageManagerService.this;
            packageManagerService.setPackageStoppedState(packageManagerService.snapshotComputer(), packageName, stopped, userId);
        }

        public String[] setPackagesSuspendedAsUser(String[] packageNames, boolean suspended, PersistableBundle appExtras, PersistableBundle launcherExtras, SuspendDialogInfo dialogInfo, String callingPackage, int userId) {
            int callingUid = Binder.getCallingUid();
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            PackageManagerService.this.enforceCanSetPackagesSuspendedAsUser(snapshot, callingPackage, callingUid, userId, "setPackagesSuspendedAsUser");
            return PackageManagerService.this.mSuspendPackageHelper.setPackagesSuspended(snapshot, packageNames, suspended, appExtras, launcherExtras, dialogInfo, callingPackage, userId, callingUid);
        }

        public boolean setRequiredForSystemUser(String packageName, final boolean requiredForSystemUser) {
            PackageManagerServiceUtils.enforceSystemOrRoot("setRequiredForSystemUser can only be run by the system or root");
            PackageStateMutator.Result result = PackageManagerService.this.commitPackageStateMutation(null, packageName, new Consumer() { // from class: com.android.server.pm.PackageManagerService$IPackageManagerImpl$$ExternalSyntheticLambda6
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((PackageStateWrite) obj).setRequiredForSystemUser(requiredForSystemUser);
                }
            });
            if (!result.isCommitted()) {
                return false;
            }
            PackageManagerService.this.scheduleWriteSettings();
            return true;
        }

        public void setRuntimePermissionsVersion(int version, int userId) {
            Preconditions.checkArgumentNonnegative(version);
            Preconditions.checkArgumentNonnegative(userId);
            PackageManagerService.this.enforceAdjustRuntimePermissionsPolicyOrUpgradeRuntimePermissions("setRuntimePermissionVersion");
            PackageManagerService.this.mSettings.setDefaultRuntimePermissionsVersion(version, userId);
        }

        public void setSplashScreenTheme(String packageName, final String themeId, final int userId) {
            int callingUid = Binder.getCallingUid();
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            snapshot.enforceCrossUserPermission(callingUid, userId, false, false, "setSplashScreenTheme");
            PackageManagerService.this.enforceOwnerRights(snapshot, packageName, callingUid);
            PackageStateInternal packageState = PackageManagerService.this.filterPackageStateForInstalledAndFiltered(snapshot, packageName, callingUid, userId);
            if (packageState == null) {
                return;
            }
            PackageManagerService.this.commitPackageStateMutation(null, packageName, new Consumer() { // from class: com.android.server.pm.PackageManagerService$IPackageManagerImpl$$ExternalSyntheticLambda10
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((PackageStateWrite) obj).userState(userId).setSplashScreenTheme(themeId);
                }
            });
        }

        public void setUpdateAvailable(String packageName, final boolean updateAvailable) {
            PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", null);
            PackageManagerService.this.commitPackageStateMutation(null, packageName, new Consumer() { // from class: com.android.server.pm.PackageManagerService$IPackageManagerImpl$$ExternalSyntheticLambda16
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((PackageStateWrite) obj).setUpdateAvailable(updateAvailable);
                }
            });
        }

        public void unregisterMoveCallback(IPackageMoveObserver callback) {
            PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS", null);
            PackageManagerService.this.mMoveCallbacks.unregister(callback);
        }

        public void verifyPendingInstall(int id, int verificationCode) throws RemoteException {
            PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_VERIFICATION_AGENT", "Only package verification agents can verify applications");
            int callingUid = Binder.getCallingUid();
            Message msg = PackageManagerService.this.mHandler.obtainMessage(15);
            PackageVerificationResponse response = new PackageVerificationResponse(verificationCode, callingUid);
            msg.arg1 = id;
            msg.obj = response;
            PackageManagerService.this.mHandler.sendMessage(msg);
        }

        public void requestPackageChecksums(String packageName, boolean includeSplits, int optional, int required, List trustedInstallers, IOnChecksumsReadyListener onChecksumsReadyListener, int userId) {
            PackageManagerService packageManagerService = PackageManagerService.this;
            packageManagerService.requestChecksumsInternal(packageManagerService.snapshotComputer(), packageName, includeSplits, optional, required, trustedInstallers, onChecksumsReadyListener, userId, PackageManagerService.this.mInjector.getBackgroundExecutor(), PackageManagerService.this.mInjector.getBackgroundHandler());
        }

        public void notifyPackagesReplacedReceived(String[] packages) {
            Computer computer = PackageManagerService.this.snapshotComputer();
            ArraySet<String> packagesToNotify = computer.getNotifyPackagesForReplacedReceived(packages);
            for (int index = 0; index < packagesToNotify.size(); index++) {
                PackageManagerService.this.notifyInstallObserver(packagesToNotify.valueAt(index), false);
            }
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            try {
                return super.onTransact(code, data, reply, flags);
            } catch (RuntimeException e) {
                if (!(e instanceof SecurityException) && !(e instanceof IllegalArgumentException) && !(e instanceof ParcelableException)) {
                    Slog.wtf(PackageManagerService.TAG, "Package Manager Unexpected Exception", e);
                }
                throw e;
            }
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.pm.PackageManagerService$IPackageManagerImpl */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new PackageManagerShellCommand(this, PackageManagerService.this.mContext, PackageManagerService.this.mDomainVerificationManager.getShell()).exec(this, in, out, err, args, callback, resultReceiver);
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            ArrayMap<String, FeatureInfo> availableFeatures;
            ArraySet<String> protectedBroadcasts;
            if (DumpUtils.checkDumpAndUsageStatsPermission(PackageManagerService.this.mContext, PackageManagerService.TAG, pw)) {
                Computer snapshot = PackageManagerService.this.snapshotComputer();
                KnownPackages knownPackages = new KnownPackages(PackageManagerService.this.mDefaultAppProvider, PackageManagerService.this.mRequiredInstallerPackage, PackageManagerService.this.mRequiredUninstallerPackage, PackageManagerService.this.mSetupWizardPackage, PackageManagerService.this.mRequiredVerifierPackage, PackageManagerService.this.mDefaultTextClassifierPackage, PackageManagerService.this.mSystemTextClassifierPackageName, PackageManagerService.this.mRequiredPermissionControllerPackage, PackageManagerService.this.mConfiguratorPackage, PackageManagerService.this.mIncidentReportApproverPackage, PackageManagerService.this.mAmbientContextDetectionPackage, PackageManagerService.this.mAppPredictionServicePackage, PackageManagerService.COMPANION_PACKAGE_NAME, PackageManagerService.this.mRetailDemoPackage, PackageManagerService.this.mOverlayConfigSignaturePackage, PackageManagerService.this.mRecentsPackage);
                synchronized (PackageManagerService.this.mAvailableFeatures) {
                    try {
                        availableFeatures = new ArrayMap<>(PackageManagerService.this.mAvailableFeatures);
                    } catch (Throwable th) {
                        th = th;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        }
                        throw th;
                    }
                }
                synchronized (PackageManagerService.this.mProtectedBroadcasts) {
                    try {
                        protectedBroadcasts = new ArraySet<>(PackageManagerService.this.mProtectedBroadcasts);
                    } catch (Throwable th3) {
                        th = th3;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th4) {
                                th = th4;
                            }
                        }
                        throw th;
                    }
                }
                new DumpHelper(PackageManagerService.this.mPermissionManager, PackageManagerService.this.mApexManager, PackageManagerService.this.mStorageEventHelper, PackageManagerService.this.mDomainVerificationManager, PackageManagerService.this.mInstallerService, PackageManagerService.this.mRequiredVerifierPackage, knownPackages, PackageManagerService.this.mChangedPackagesTracker, availableFeatures, protectedBroadcasts, PackageManagerService.this.getPerUidReadTimeouts(snapshot)).doDump(snapshot, fd, pw, args);
            }
        }

        public void checkDefaultGaller() {
            int myUserId = UserHandle.myUserId();
            String packageName = getDefaultGallerPackageName(myUserId);
            Slog.i(PackageManagerService.TAG, "Default galler pakagesName:" + packageName);
            if (packageName != null) {
                PackageInfo info = getPackageInfo(packageName, 0L, myUserId);
                if (info == null) {
                    Slog.w(PackageManagerService.TAG, "Default galler no longer installed: " + packageName);
                    ITranPackageManagerService.Instance().applyFactoryDefaultGallerLPw(myUserId);
                    return;
                }
                return;
            }
            ITranPackageManagerService.Instance().applyFactoryDefaultGallerLPw(myUserId);
        }

        public boolean setDefaultGallerPackageName(String gallerPkg, int userId) {
            PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
            if (UserHandle.getCallingUserId() != userId) {
                PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
            }
            return ITranPackageManagerService.Instance().setDefaultGallerPackageNameInternal(gallerPkg, userId);
        }

        public String getDefaultGallerPackageName(int userId) {
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            if (UserHandle.getCallingUserId() != userId) {
                PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
            }
            if (snapshot.getInstantAppPackageName(Binder.getCallingUid()) != null) {
                return null;
            }
            return ITranPackageManagerService.Instance().getDefaultGallerPackageNameInternal(userId);
        }

        public ResolveInfo tranfindPreferredActivityNotLocked(Intent intent, String resolvedType, int flags, List<ResolveInfo> query, int priority, boolean always, boolean removeMatches, boolean debug, int userId) {
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            return PackageManagerService.this.mPreferredActivityHelper.findPreferredActivityNotLocked(snapshot, intent, resolvedType, flags, query, always, removeMatches, debug, userId, UserHandle.getAppId(Binder.getCallingUid()) >= 10000);
        }

        public void checkDefaultMusic() {
            int myUserId = UserHandle.myUserId();
            String packageName = getDefaultMusicPackageName(myUserId);
            Slog.i(PackageManagerService.TAG, "Default music pakagesName:" + packageName);
            if (packageName != null) {
                PackageInfo info = getPackageInfo(packageName, 0L, myUserId);
                if (info == null) {
                    Slog.w(PackageManagerService.TAG, "Default music no longer installed: " + packageName);
                    ITranPackageManagerService.Instance().applyFactoryDefaultMusicLPw(myUserId);
                    return;
                }
                return;
            }
            ITranPackageManagerService.Instance().applyFactoryDefaultMusicLPw(myUserId);
        }

        public boolean setDefaultMusicPackageName(String musicPkg, int userId) {
            PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
            if (UserHandle.getCallingUserId() != userId) {
                PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
            }
            return ITranPackageManagerService.Instance().setDefaultMusicPackageNameInternal(musicPkg, userId);
        }

        public String getDefaultMusicPackageName(int userId) {
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            if (UserHandle.getCallingUserId() != userId) {
                PackageManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
            }
            if (snapshot.getInstantAppPackageName(Binder.getCallingUid()) != null) {
                return null;
            }
            return ITranPackageManagerService.Instance().getDefaultMusicPackageNameInternal(userId);
        }

        public boolean isAppTrustedForPrivacyProtect(String packageName, int userId) {
            return false;
        }

        public void setAppTrustStateForPrivacyProtect(String packageName, boolean trust, int userId) {
        }

        public void setApplicationNotifyScreenOn(String packageName, int newState, int userId) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageSetting pkgSetting = PackageManagerService.this.mSettings.getPackageLPr(packageName);
                if (pkgSetting == null) {
                    Slog.d(PackageManagerService.TAG, "Notify Unknown package: " + packageName);
                    return;
                }
                int oldState = pkgSetting.getNotifyScreenOn(userId);
                if (oldState == newState) {
                    return;
                }
                pkgSetting.setNotifyScreenOn(newState, userId);
                PackageManagerService.this.scheduleWritePackageRestrictions(userId);
            }
        }

        public int getApplicationNotifyScreenOn(String packageName, int userId) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageSetting pkgSetting = PackageManagerService.this.mSettings.getPackageLPr(packageName);
                if (pkgSetting == null) {
                    return 0;
                }
                return pkgSetting.getNotifyScreenOn(userId);
            }
        }
    }

    /* loaded from: classes2.dex */
    private class PackageManagerLocalImpl implements PackageManagerLocal {
        private PackageManagerLocalImpl() {
        }

        @Override // com.android.server.pm.PackageManagerLocal
        public void reconcileSdkData(String volumeUuid, String packageName, List<String> subDirNames, int userId, int appId, int previousAppId, String seInfo, int flags) throws IOException {
            ReconcileSdkDataArgs args;
            synchronized (PackageManagerService.this.mInstallLock) {
                try {
                    Installer installer = PackageManagerService.this.mInstaller;
                    args = Installer.buildReconcileSdkDataArgs(volumeUuid, packageName, subDirNames, userId, appId, seInfo, flags);
                } catch (Throwable th) {
                    e = th;
                }
                try {
                    args.previousAppId = previousAppId;
                    try {
                        PackageManagerService.this.mInstaller.reconcileSdkData(args);
                    } catch (Installer.InstallerException e) {
                        throw new IOException(e.getMessage());
                    }
                } catch (Throwable th2) {
                    e = th2;
                    throw e;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class PackageManagerInternalImpl extends PackageManagerInternalBase {
        public PackageManagerInternalImpl() {
            super(PackageManagerService.this);
        }

        @Override // com.android.server.pm.PackageManagerInternalBase
        protected Context getContext() {
            return PackageManagerService.this.mContext;
        }

        @Override // com.android.server.pm.PackageManagerInternalBase
        protected PermissionManagerServiceInternal getPermissionManager() {
            return PackageManagerService.this.mPermissionManager;
        }

        @Override // com.android.server.pm.PackageManagerInternalBase
        protected AppDataHelper getAppDataHelper() {
            return PackageManagerService.this.mAppDataHelper;
        }

        @Override // com.android.server.pm.PackageManagerInternalBase
        protected PackageObserverHelper getPackageObserverHelper() {
            return PackageManagerService.this.mPackageObserverHelper;
        }

        @Override // com.android.server.pm.PackageManagerInternalBase
        protected ResolveIntentHelper getResolveIntentHelper() {
            return PackageManagerService.this.mResolveIntentHelper;
        }

        @Override // com.android.server.pm.PackageManagerInternalBase
        protected SuspendPackageHelper getSuspendPackageHelper() {
            return PackageManagerService.this.mSuspendPackageHelper;
        }

        @Override // com.android.server.pm.PackageManagerInternalBase
        protected DistractingPackageHelper getDistractingPackageHelper() {
            return PackageManagerService.this.mDistractingPackageHelper;
        }

        @Override // com.android.server.pm.PackageManagerInternalBase
        protected ProtectedPackages getProtectedPackages() {
            return PackageManagerService.this.mProtectedPackages;
        }

        @Override // com.android.server.pm.PackageManagerInternalBase
        protected UserNeedsBadgingCache getUserNeedsBadging() {
            return PackageManagerService.this.mUserNeedsBadging;
        }

        @Override // com.android.server.pm.PackageManagerInternalBase
        protected InstantAppRegistry getInstantAppRegistry() {
            return PackageManagerService.this.mInstantAppRegistry;
        }

        @Override // com.android.server.pm.PackageManagerInternalBase
        protected ApexManager getApexManager() {
            return PackageManagerService.this.mApexManager;
        }

        @Override // com.android.server.pm.PackageManagerInternalBase
        protected DexManager getDexManager() {
            return PackageManagerService.this.mDexManager;
        }

        @Override // android.content.pm.PackageManagerInternal
        public boolean isPlatformSigned(String packageName) {
            PackageStateInternal packageState = snapshot().getPackageStateInternal(packageName);
            if (packageState == null) {
                return false;
            }
            SigningDetails signingDetails = packageState.getSigningDetails();
            return signingDetails.hasAncestorOrSelf(PackageManagerService.this.mPlatformPackage.getSigningDetails()) || PackageManagerService.this.mPlatformPackage.getSigningDetails().checkCapability(signingDetails, 4);
        }

        @Override // android.content.pm.PackageManagerInternal
        public boolean isDataRestoreSafe(byte[] restoringFromSigHash, String packageName) {
            Computer snapshot = snapshot();
            SigningDetails sd = snapshot.getSigningDetails(packageName);
            if (sd == null) {
                return false;
            }
            return sd.hasSha256Certificate(restoringFromSigHash, 1);
        }

        @Override // android.content.pm.PackageManagerInternal
        public boolean isDataRestoreSafe(Signature restoringFromSig, String packageName) {
            Computer snapshot = snapshot();
            SigningDetails sd = snapshot.getSigningDetails(packageName);
            if (sd == null) {
                return false;
            }
            return sd.hasCertificate(restoringFromSig, 1);
        }

        @Override // android.content.pm.PackageManagerInternal
        public boolean hasSignatureCapability(int serverUid, int clientUid, int capability) {
            Computer snapshot = snapshot();
            SigningDetails serverSigningDetails = snapshot.getSigningDetails(serverUid);
            SigningDetails clientSigningDetails = snapshot.getSigningDetails(clientUid);
            return serverSigningDetails.checkCapability(clientSigningDetails, capability) || clientSigningDetails.hasAncestorOrSelf(serverSigningDetails);
        }

        @Override // android.content.pm.PackageManagerInternal
        public PackageList getPackageList(PackageManagerInternal.PackageListObserver observer) {
            final ArrayList<String> list = new ArrayList<>();
            PackageManagerService.this.forEachPackageState(snapshot(), new Consumer() { // from class: com.android.server.pm.PackageManagerService$PackageManagerInternalImpl$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    PackageManagerService.PackageManagerInternalImpl.lambda$getPackageList$0(list, (PackageStateInternal) obj);
                }
            });
            PackageList packageList = new PackageList(list, observer);
            if (observer != null) {
                PackageManagerService.this.mPackageObserverHelper.addObserver(packageList);
            }
            return packageList;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$getPackageList$0(ArrayList list, PackageStateInternal packageState) {
            AndroidPackage pkg = packageState.getPkg();
            if (pkg != null) {
                list.add(pkg.getPackageName());
            }
        }

        @Override // android.content.pm.PackageManagerInternal
        public String getDisabledSystemPackageName(String packageName) {
            PackageStateInternal disabledPkgSetting = snapshot().getDisabledSystemPackage(packageName);
            AndroidPackage disabledPkg = disabledPkgSetting == null ? null : disabledPkgSetting.getPkg();
            if (disabledPkg == null) {
                return null;
            }
            return disabledPkg.getPackageName();
        }

        @Override // android.content.pm.PackageManagerInternal
        public boolean isResolveActivityComponent(ComponentInfo component) {
            return PackageManagerService.this.mResolveActivity.packageName.equals(component.packageName) && PackageManagerService.this.mResolveActivity.name.equals(component.name);
        }

        @Override // android.content.pm.PackageManagerInternal
        public long getCeDataInode(String packageName, int userId) {
            PackageStateInternal packageState = snapshot().getPackageStateInternal(packageName);
            if (packageState == null) {
                return 0L;
            }
            return packageState.getUserStateOrDefault(userId).getCeDataInode();
        }

        @Override // android.content.pm.PackageManagerInternal
        public void removeAllNonSystemPackageSuspensions(int userId) {
            Computer computer = PackageManagerService.this.snapshotComputer();
            String[] allPackages = computer.getAllAvailablePackageNames();
            PackageManagerService.this.mSuspendPackageHelper.removeSuspensionsBySuspendingPackage(computer, allPackages, new Predicate() { // from class: com.android.server.pm.PackageManagerService$PackageManagerInternalImpl$$ExternalSyntheticLambda2
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return PackageManagerService.PackageManagerInternalImpl.lambda$removeAllNonSystemPackageSuspensions$1((String) obj);
                }
            }, userId);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ boolean lambda$removeAllNonSystemPackageSuspensions$1(String suspendingPackage) {
            return !PackageManagerService.PLATFORM_PACKAGE_NAME.equals(suspendingPackage);
        }

        @Override // android.content.pm.PackageManagerInternal
        public void flushPackageRestrictions(int userId) {
            synchronized (PackageManagerService.this.mLock) {
                PackageManagerService.this.flushPackageRestrictionsAsUserInternalLocked(userId);
            }
        }

        @Override // android.content.pm.PackageManagerInternal
        public void setDeviceAndProfileOwnerPackages(int deviceOwnerUserId, String deviceOwnerPackage, SparseArray<String> profileOwnerPackages) {
            PackageManagerService.this.mProtectedPackages.setDeviceAndProfileOwnerPackages(deviceOwnerUserId, deviceOwnerPackage, profileOwnerPackages);
            ArraySet<Integer> usersWithPoOrDo = new ArraySet<>();
            if (deviceOwnerPackage != null) {
                usersWithPoOrDo.add(Integer.valueOf(deviceOwnerUserId));
            }
            int sz = profileOwnerPackages.size();
            for (int i = 0; i < sz; i++) {
                if (profileOwnerPackages.valueAt(i) != null) {
                    removeAllNonSystemPackageSuspensions(profileOwnerPackages.keyAt(i));
                }
            }
        }

        @Override // android.content.pm.PackageManagerInternal
        public void pruneCachedApksInApex(List<PackageInfo> apexPackages) {
            if (PackageManagerService.this.mCacheDir == null) {
                return;
            }
            PackageCacher cacher = new PackageCacher(PackageManagerService.this.mCacheDir);
            synchronized (PackageManagerService.this.mLock) {
                Computer snapshot = snapshot();
                int size = apexPackages.size();
                for (int i = 0; i < size; i++) {
                    List<String> apkNames = PackageManagerService.this.mApexManager.getApksInApex(apexPackages.get(i).packageName);
                    int apksInApex = apkNames.size();
                    for (int j = 0; j < apksInApex; j++) {
                        AndroidPackage pkg = snapshot.getPackage(apkNames.get(j));
                        cacher.cleanCachedResult(new File(pkg.getPath()));
                    }
                }
            }
        }

        @Override // android.content.pm.PackageManagerInternal
        public void setExternalSourcesPolicy(PackageManagerInternal.ExternalSourcesPolicy policy) {
            if (policy != null) {
                PackageManagerService.this.mExternalSourcesPolicy = policy;
            }
        }

        @Override // android.content.pm.PackageManagerInternal
        public boolean isPackagePersistent(String packageName) {
            AndroidPackage pkg;
            PackageStateInternal packageState = snapshot().getPackageStateInternal(packageName);
            return packageState != null && (pkg = packageState.getPkg()) != null && pkg.isSystem() && pkg.isPersistent();
        }

        @Override // android.content.pm.PackageManagerInternal
        public List<PackageInfo> getOverlayPackages(int userId) {
            PackageInfo pkgInfo;
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            ArrayList<PackageInfo> overlayPackages = new ArrayList<>();
            ArrayMap<String, ? extends PackageStateInternal> packageStates = snapshot.getPackageStates();
            for (int index = 0; index < packageStates.size(); index++) {
                PackageStateInternal packageState = packageStates.valueAt(index);
                AndroidPackage pkg = packageState.getPkg();
                if (pkg != null && pkg.getOverlayTarget() != null && (pkgInfo = snapshot.generatePackageInfo(packageState, 0L, userId)) != null) {
                    overlayPackages.add(pkgInfo);
                }
            }
            return overlayPackages;
        }

        @Override // android.content.pm.PackageManagerInternal
        public List<String> getTargetPackageNames(int userId) {
            final List<String> targetPackages = new ArrayList<>();
            PackageManagerService.this.forEachPackageState(snapshot(), new Consumer() { // from class: com.android.server.pm.PackageManagerService$PackageManagerInternalImpl$$ExternalSyntheticLambda3
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    PackageManagerService.PackageManagerInternalImpl.lambda$getTargetPackageNames$2(targetPackages, (PackageStateInternal) obj);
                }
            });
            return targetPackages;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$getTargetPackageNames$2(List targetPackages, PackageStateInternal packageState) {
            AndroidPackage pkg = packageState.getPkg();
            if (pkg != null && !pkg.isOverlay()) {
                targetPackages.add(pkg.getPackageName());
            }
        }

        @Override // android.content.pm.PackageManagerInternal
        public boolean setEnabledOverlayPackages(int userId, String targetPackageName, OverlayPaths overlayPaths, Set<String> outUpdatedPackageNames) {
            return PackageManagerService.this.setEnabledOverlayPackages(userId, targetPackageName, overlayPaths, outUpdatedPackageNames);
        }

        @Override // android.content.pm.PackageManagerInternal
        public void addIsolatedUid(int isolatedUid, int ownerUid) {
            synchronized (PackageManagerService.this.mLock) {
                PackageManagerService.this.mIsolatedOwners.put(isolatedUid, ownerUid);
            }
        }

        @Override // android.content.pm.PackageManagerInternal
        public void removeIsolatedUid(int isolatedUid) {
            synchronized (PackageManagerService.this.mLock) {
                PackageManagerService.this.mIsolatedOwners.delete(isolatedUid);
            }
        }

        @Override // android.content.pm.PackageManagerInternal
        public void notifyPackageUse(String packageName, int reason) {
            synchronized (PackageManagerService.this.mLock) {
                PackageManagerService.this.notifyPackageUseInternal(packageName, reason);
            }
        }

        @Override // android.content.pm.PackageManagerInternal
        public boolean compileLayouts(String packageName) {
            synchronized (PackageManagerService.this.mLock) {
                AndroidPackage pkg = PackageManagerService.this.mPackages.get(packageName);
                if (pkg == null) {
                    return false;
                }
                return PackageManagerService.this.mArtManagerService.compileLayouts(pkg);
            }
        }

        @Override // android.content.pm.PackageManagerInternal
        public String removeLegacyDefaultBrowserPackageName(int userId) {
            String removeDefaultBrowserPackageNameLPw;
            synchronized (PackageManagerService.this.mLock) {
                removeDefaultBrowserPackageNameLPw = PackageManagerService.this.mSettings.removeDefaultBrowserPackageNameLPw(userId);
            }
            return removeDefaultBrowserPackageNameLPw;
        }

        @Override // android.content.pm.PackageManagerInternal
        public void uninstallApex(String packageName, long versionCode, int userId, IntentSender intentSender, int flags) {
            int callerUid = Binder.getCallingUid();
            if (callerUid != 0 && callerUid != 2000) {
                throw new SecurityException("Not allowed to uninstall apexes");
            }
            PackageInstallerService.PackageDeleteObserverAdapter adapter = new PackageInstallerService.PackageDeleteObserverAdapter(PackageManagerService.this.mContext, intentSender, packageName, false, userId);
            if ((flags & 2) == 0) {
                adapter.onPackageDeleted(packageName, -5, "Can't uninstall an apex for a single user");
                return;
            }
            ApexManager am = PackageManagerService.this.mApexManager;
            PackageInfo activePackage = am.getPackageInfo(packageName, 1);
            if (activePackage == null) {
                adapter.onPackageDeleted(packageName, -5, packageName + " is not an apex package");
            } else if (versionCode != -1 && activePackage.getLongVersionCode() != versionCode) {
                adapter.onPackageDeleted(packageName, -5, "Active version " + activePackage.getLongVersionCode() + " is not equal to " + versionCode + "]");
            } else if (am.uninstallApex(activePackage.applicationInfo.sourceDir)) {
                adapter.onPackageDeleted(packageName, 1, null);
            } else {
                adapter.onPackageDeleted(packageName, -5, "Failed to uninstall apex " + packageName);
            }
        }

        @Override // android.content.pm.PackageManagerInternal
        public void updateRuntimePermissionsFingerprint(int userId) {
            PackageManagerService.this.mSettings.updateRuntimePermissionsFingerprint(userId);
        }

        @Override // android.content.pm.PackageManagerInternal
        public void migrateLegacyObbData() {
            try {
                PackageManagerService.this.mInstaller.migrateLegacyObbData();
            } catch (Exception e) {
                Slog.wtf(PackageManagerService.TAG, e);
            }
        }

        @Override // android.content.pm.PackageManagerInternal
        public void writeSettings(boolean async) {
            synchronized (PackageManagerService.this.mLock) {
                if (async) {
                    PackageManagerService.this.scheduleWriteSettings();
                } else {
                    PackageManagerService.this.writeSettingsLPrTEMP();
                }
            }
        }

        @Override // android.content.pm.PackageManagerInternal
        public void writePermissionSettings(int[] userIds, boolean async) {
            synchronized (PackageManagerService.this.mLock) {
                for (int userId : userIds) {
                    PackageManagerService.this.mSettings.writePermissionStateForUserLPr(userId, !async);
                }
            }
        }

        @Override // android.content.pm.PackageManagerInternal
        public boolean isPermissionUpgradeNeeded(int userId) {
            return PackageManagerService.this.mSettings.isPermissionUpgradeNeeded(userId);
        }

        @Override // android.content.pm.PackageManagerInternal
        public void setIntegrityVerificationResult(int verificationId, int verificationResult) {
            Message msg = PackageManagerService.this.mHandler.obtainMessage(25);
            msg.arg1 = verificationId;
            msg.obj = Integer.valueOf(verificationResult);
            PackageManagerService.this.mHandler.sendMessage(msg);
        }

        @Override // android.content.pm.PackageManagerInternal
        public void setVisibilityLogging(String packageName, boolean enable) {
            PackageStateInternal packageState = snapshot().getPackageStateInternal(packageName);
            if (packageState == null) {
                throw new IllegalStateException("No package found for " + packageName);
            }
            PackageManagerService.this.mAppsFilter.getFeatureConfig().enableLogging(packageState.getAppId(), enable);
        }

        @Override // android.content.pm.PackageManagerInternal
        public void clearBlockUninstallForUser(int userId) {
            synchronized (PackageManagerService.this.mLock) {
                PackageManagerService.this.mSettings.clearBlockUninstallLPw(userId);
                PackageManagerService.this.mSettings.writePackageRestrictionsLPr(userId);
            }
        }

        @Override // android.content.pm.PackageManagerInternal
        public boolean registerInstalledLoadingProgressCallback(String packageName, PackageManagerInternal.InstalledLoadingProgressCallback callback, int userId) {
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            PackageStateInternal ps = PackageManagerService.this.filterPackageStateForInstalledAndFiltered(snapshot, packageName, Binder.getCallingUid(), userId);
            if (ps == null) {
                return false;
            }
            if (!ps.isLoading()) {
                Slog.w(PackageManagerService.TAG, "Failed registering loading progress callback. Package is fully loaded.");
                return false;
            } else if (PackageManagerService.this.mIncrementalManager == null) {
                Slog.w(PackageManagerService.TAG, "Failed registering loading progress callback. Incremental is not enabled");
                return false;
            } else {
                return PackageManagerService.this.mIncrementalManager.registerLoadingProgressCallback(ps.getPathString(), callback.getBinder());
            }
        }

        @Override // android.content.pm.PackageManagerInternal
        public IncrementalStatesInfo getIncrementalStatesInfo(String packageName, int filterCallingUid, int userId) {
            Computer snapshot = PackageManagerService.this.snapshotComputer();
            PackageStateInternal ps = PackageManagerService.this.filterPackageStateForInstalledAndFiltered(snapshot, packageName, filterCallingUid, userId);
            if (ps == null) {
                return null;
            }
            return new IncrementalStatesInfo(ps.isLoading(), ps.getLoadingProgress());
        }

        @Override // android.content.pm.PackageManagerInternal
        public boolean isSameApp(String packageName, int callingUid, int userId) {
            if (packageName == null) {
                return false;
            }
            if (Process.isSdkSandboxUid(callingUid)) {
                return packageName.equals(PackageManagerService.this.mRequiredSdkSandboxPackage);
            }
            Computer snapshot = snapshot();
            int uid = snapshot.getPackageUid(packageName, 0L, userId);
            return UserHandle.isSameApp(uid, callingUid);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onPackageProcessKilledForUninstall$3$com-android-server-pm-PackageManagerService$PackageManagerInternalImpl  reason: not valid java name */
        public /* synthetic */ void m5581xe98d4b04(String packageName) {
            PackageManagerService.this.notifyInstallObserver(packageName, true);
        }

        @Override // android.content.pm.PackageManagerInternal
        public void onPackageProcessKilledForUninstall(final String packageName) {
            PackageManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerService$PackageManagerInternalImpl$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    PackageManagerService.PackageManagerInternalImpl.this.m5581xe98d4b04(packageName);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setEnabledOverlayPackages(final int userId, final String targetPackageName, final OverlayPaths newOverlayPaths, Set<String> outUpdatedPackageNames) {
        List<VersionedPackage> dependents;
        synchronized (this.mOverlayPathsLock) {
            final ArrayMap<String, ArraySet<String>> libNameToModifiedDependents = new ArrayMap<>();
            Computer computer = snapshotComputer();
            PackageStateInternal packageState = computer.getPackageStateInternal(targetPackageName);
            AndroidPackage targetPkg = packageState == null ? null : packageState.getPkg();
            if (targetPackageName != null && targetPkg != null) {
                if (Objects.equals(packageState.getUserStateOrDefault(userId).getOverlayPaths(), newOverlayPaths)) {
                    return true;
                }
                if (targetPkg.getLibraryNames() != null) {
                    for (String libName : targetPkg.getLibraryNames()) {
                        SharedLibraryInfo info = computer.getSharedLibraryInfo(libName, -1L);
                        if (info != null && (dependents = computer.getPackagesUsingSharedLibrary(info, 0L, 1000, userId)) != null) {
                            ArraySet<String> modifiedDependents = null;
                            for (VersionedPackage dependent : dependents) {
                                PackageStateInternal dependentState = computer.getPackageStateInternal(dependent.getPackageName());
                                if (dependentState != null) {
                                    if (!Objects.equals(dependentState.getUserStateOrDefault(userId).getSharedLibraryOverlayPaths().get(libName), newOverlayPaths)) {
                                        String dependentPackageName = dependent.getPackageName();
                                        modifiedDependents = ArrayUtils.add(modifiedDependents, dependentPackageName);
                                        outUpdatedPackageNames.add(dependentPackageName);
                                    }
                                }
                            }
                            if (modifiedDependents != null) {
                                libNameToModifiedDependents.put(libName, modifiedDependents);
                            }
                        }
                    }
                }
                outUpdatedPackageNames.add(targetPackageName);
                commitPackageStateMutation(null, new Consumer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda24
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        PackageManagerService.lambda$setEnabledOverlayPackages$54(targetPackageName, userId, newOverlayPaths, libNameToModifiedDependents, (PackageStateMutator) obj);
                    }
                });
                if (userId == 0) {
                    maybeUpdateSystemOverlays(targetPackageName, newOverlayPaths);
                }
                invalidatePackageInfoCache();
                return true;
            }
            Slog.e(TAG, "failed to find package " + targetPackageName);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$setEnabledOverlayPackages$54(String targetPackageName, int userId, OverlayPaths newOverlayPaths, ArrayMap libNameToModifiedDependents, PackageStateMutator mutator) {
        mutator.forPackage(targetPackageName).userState(userId).setOverlayPaths(newOverlayPaths);
        for (int mapIndex = 0; mapIndex < libNameToModifiedDependents.size(); mapIndex++) {
            String libName = (String) libNameToModifiedDependents.keyAt(mapIndex);
            ArraySet<String> modifiedDependents = (ArraySet) libNameToModifiedDependents.valueAt(mapIndex);
            for (int setIndex = 0; setIndex < modifiedDependents.size(); setIndex++) {
                mutator.forPackage(modifiedDependents.valueAt(setIndex)).userState(userId).setOverlayPathsForLibrary(libName, newOverlayPaths);
            }
        }
    }

    private void maybeUpdateSystemOverlays(String targetPackageName, OverlayPaths newOverlayPaths) {
        if (!this.mResolverReplaced) {
            if (targetPackageName.equals(PLATFORM_PACKAGE_NAME)) {
                if (newOverlayPaths == null) {
                    this.mPlatformPackageOverlayPaths = null;
                    this.mPlatformPackageOverlayResourceDirs = null;
                } else {
                    this.mPlatformPackageOverlayPaths = (String[]) newOverlayPaths.getOverlayPaths().toArray(new String[0]);
                    this.mPlatformPackageOverlayResourceDirs = (String[]) newOverlayPaths.getResourceDirs().toArray(new String[0]);
                }
                applyUpdatedSystemOverlayPaths();
            }
        } else if (targetPackageName.equals(this.mResolveActivity.applicationInfo.packageName)) {
            if (newOverlayPaths == null) {
                this.mReplacedResolverPackageOverlayPaths = null;
                this.mReplacedResolverPackageOverlayResourceDirs = null;
            } else {
                this.mReplacedResolverPackageOverlayPaths = (String[]) newOverlayPaths.getOverlayPaths().toArray(new String[0]);
                this.mReplacedResolverPackageOverlayResourceDirs = (String[]) newOverlayPaths.getResourceDirs().toArray(new String[0]);
            }
            applyUpdatedSystemOverlayPaths();
        }
    }

    private void applyUpdatedSystemOverlayPaths() {
        ApplicationInfo applicationInfo = this.mAndroidApplication;
        if (applicationInfo == null) {
            Slog.i(TAG, "Skipped the AndroidApplication overlay paths update - no app yet");
        } else {
            applicationInfo.overlayPaths = this.mPlatformPackageOverlayPaths;
            this.mAndroidApplication.resourceDirs = this.mPlatformPackageOverlayResourceDirs;
        }
        if (this.mResolverReplaced) {
            this.mResolveActivity.applicationInfo.overlayPaths = this.mReplacedResolverPackageOverlayPaths;
            this.mResolveActivity.applicationInfo.resourceDirs = this.mReplacedResolverPackageOverlayResourceDirs;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enforceAdjustRuntimePermissionsPolicyOrUpgradeRuntimePermissions(String message) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.ADJUST_RUNTIME_PERMISSIONS_POLICY") != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.UPGRADE_RUNTIME_PERMISSIONS") != 0) {
            throw new SecurityException(message + " requires android.permission.ADJUST_RUNTIME_PERMISSIONS_POLICY or android.permission.UPGRADE_RUNTIME_PERMISSIONS");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Deprecated
    public PackageSetting getPackageSettingForMutation(String packageName) {
        return this.mSettings.getPackageLPr(packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Deprecated
    public PackageSetting getDisabledPackageSettingForMutation(String packageName) {
        return this.mSettings.getDisabledSystemPkgLPr(packageName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public PackageStateInternal filterPackageStateForInstalledAndFiltered(Computer computer, String packageName, int callingUid, int userId) {
        PackageStateInternal packageState = computer.getPackageStateInternal(packageName, callingUid);
        if (packageState == null || computer.shouldFilterApplication(packageState, callingUid, userId) || !packageState.getUserStateOrDefault(userId).isInstalled()) {
            return null;
        }
        return packageState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Deprecated
    public void forEachPackageSetting(Consumer<PackageSetting> actionLocked) {
        synchronized (this.mLock) {
            int size = this.mSettings.getPackagesLocked().size();
            for (int index = 0; index < size; index++) {
                actionLocked.accept(this.mSettings.getPackagesLocked().valueAt(index));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forEachPackageState(Computer snapshot, Consumer<PackageStateInternal> consumer) {
        forEachPackageState(snapshot.getPackageStates(), consumer);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forEachPackage(Computer snapshot, Consumer<AndroidPackage> consumer) {
        ArrayMap<String, ? extends PackageStateInternal> packageStates = snapshot.getPackageStates();
        int size = packageStates.size();
        for (int index = 0; index < size; index++) {
            PackageStateInternal packageState = packageStates.valueAt(index);
            if (packageState.getPkg() != null) {
                consumer.accept(packageState.getPkg());
            }
        }
    }

    private void forEachPackageState(ArrayMap<String, ? extends PackageStateInternal> packageStates, Consumer<PackageStateInternal> consumer) {
        int size = packageStates.size();
        for (int index = 0; index < size; index++) {
            PackageStateInternal packageState = packageStates.valueAt(index);
            consumer.accept(packageState);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forEachInstalledPackage(Computer snapshot, final Consumer<AndroidPackage> action, final int userId) {
        Consumer<PackageStateInternal> actionWrapped = new Consumer() { // from class: com.android.server.pm.PackageManagerService$$ExternalSyntheticLambda8
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                PackageManagerService.lambda$forEachInstalledPackage$55(userId, action, (PackageStateInternal) obj);
            }
        };
        forEachPackageState(snapshot.getPackageStates(), actionWrapped);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$forEachInstalledPackage$55(int userId, Consumer action, PackageStateInternal packageState) {
        if (packageState.getPkg() != null && packageState.getUserStateOrDefault(userId).isInstalled()) {
            action.accept(packageState.getPkg());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isHistoricalPackageUsageAvailable() {
        return this.mPackageUsage.isHistoricalPackageUsageAvailable();
    }

    public CompilerStats.PackageStats getOrCreateCompilerPackageStats(AndroidPackage pkg) {
        return getOrCreateCompilerPackageStats(pkg.getPackageName());
    }

    public CompilerStats.PackageStats getOrCreateCompilerPackageStats(String pkgName) {
        return this.mCompilerStats.getOrCreatePackageStats(pkgName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void grantImplicitAccess(Computer snapshot, int userId, Intent intent, int recipientAppId, int visibleUid, boolean direct, boolean retainOnUpdate) {
        boolean accessGranted;
        AndroidPackage visiblePackage = snapshot.getPackage(visibleUid);
        int recipientUid = UserHandle.getUid(userId, recipientAppId);
        if (visiblePackage == null || snapshot.getPackage(recipientUid) == null) {
            return;
        }
        boolean instantApp = snapshot.isInstantAppInternal(visiblePackage.getPackageName(), userId, visibleUid);
        if (instantApp) {
            if (!direct) {
                return;
            }
            accessGranted = this.mInstantAppRegistry.grantInstantAccess(userId, intent, recipientAppId, UserHandle.getAppId(visibleUid));
        } else {
            accessGranted = this.mAppsFilter.grantImplicitAccess(recipientUid, visibleUid, retainOnUpdate);
        }
        if (accessGranted) {
            ApplicationPackageManager.invalidateGetPackagesForUidCache();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canHaveOatDir(Computer snapshot, String packageName) {
        PackageStateInternal packageState = snapshot.getPackageStateInternal(packageName);
        if (packageState == null || packageState.getPkg() == null) {
            return false;
        }
        return AndroidPackageUtils.canHaveOatDir(packageState.getPkg(), packageState.getTransientState().isUpdatedSystemApp());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long deleteOatArtifactsOfPackage(Computer snapshot, String packageName) {
        PackageStateInternal packageState = snapshot.getPackageStateInternal(packageName);
        if (packageState == null || packageState.getPkg() == null) {
            return -1L;
        }
        return this.mDexManager.deleteOptimizedFiles(ArtUtils.createArtPackageInfo(packageState.getPkg(), packageState));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<String> getMimeGroupInternal(Computer snapshot, String packageName, String mimeGroup) {
        PackageStateInternal packageState = snapshot.getPackageStateInternal(packageName);
        if (packageState == null) {
            return Collections.emptyList();
        }
        Map<String, Set<String>> mimeGroups = packageState.getMimeGroups();
        Set<String> mimeTypes = mimeGroups != null ? mimeGroups.get(mimeGroup) : null;
        if (mimeTypes == null) {
            throw new IllegalArgumentException("Unknown MIME group " + mimeGroup + " for package " + packageName);
        }
        return new ArrayList(mimeTypes);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeSettingsLPrTEMP() {
        this.mPermissionManager.writeLegacyPermissionsTEMP(this.mSettings.mPermissions);
        this.mSettings.writeLPr(this.mLiveComputer);
    }

    @Override // android.content.pm.TestUtilityService
    public void verifyHoldLockToken(IBinder token) {
        if (!Build.IS_DEBUGGABLE) {
            throw new SecurityException("holdLock requires a debuggable build");
        }
        if (token == null) {
            throw new SecurityException("null holdLockToken");
        }
        if (token.queryLocalInterface("holdLock:" + Binder.getCallingUid()) != this) {
            throw new SecurityException("Invalid holdLock() token");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String getDefaultTimeouts() {
        long token = Binder.clearCallingIdentity();
        try {
            return DeviceConfig.getString("package_manager_service", PROPERTY_INCFS_DEFAULT_TIMEOUTS, "");
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String getKnownDigestersList() {
        long token = Binder.clearCallingIdentity();
        try {
            return DeviceConfig.getString("package_manager_service", PROPERTY_KNOWN_DIGESTERS_LIST, "");
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public PerUidReadTimeouts[] getPerUidReadTimeouts(Computer snapshot) {
        PerUidReadTimeouts[] result = this.mPerUidReadTimeoutsCache;
        if (result == null) {
            PerUidReadTimeouts[] result2 = parsePerUidReadTimeouts(snapshot);
            this.mPerUidReadTimeoutsCache = result2;
            return result2;
        }
        return result;
    }

    private PerUidReadTimeouts[] parsePerUidReadTimeouts(Computer snapshot) {
        String defaultTimeouts;
        String knownDigestersList;
        List<PerPackageReadTimeouts> perPackageReadTimeouts;
        String defaultTimeouts2 = getDefaultTimeouts();
        String knownDigestersList2 = getKnownDigestersList();
        List<PerPackageReadTimeouts> perPackageReadTimeouts2 = PerPackageReadTimeouts.parseDigestersList(defaultTimeouts2, knownDigestersList2);
        if (perPackageReadTimeouts2.size() == 0) {
            return EMPTY_PER_UID_READ_TIMEOUTS_ARRAY;
        }
        int[] allUsers = this.mInjector.getUserManagerService().getUserIds();
        List<PerUidReadTimeouts> result = new ArrayList<>(perPackageReadTimeouts2.size());
        int i = 0;
        int size = perPackageReadTimeouts2.size();
        while (i < size) {
            PerPackageReadTimeouts perPackage = perPackageReadTimeouts2.get(i);
            PackageStateInternal ps = snapshot.getPackageStateInternal(perPackage.packageName);
            if (ps != null && ps.getAppId() >= 10000) {
                AndroidPackage pkg = ps.getPkg();
                if (pkg.getLongVersionCode() >= perPackage.versionCodes.minVersionCode && pkg.getLongVersionCode() <= perPackage.versionCodes.maxVersionCode && (perPackage.sha256certificate == null || pkg.getSigningDetails().hasSha256Certificate(perPackage.sha256certificate))) {
                    int length = allUsers.length;
                    int i2 = 0;
                    while (i2 < length) {
                        int userId = allUsers[i2];
                        if (!ps.getUserStateOrDefault(userId).isInstalled()) {
                            defaultTimeouts = defaultTimeouts2;
                            knownDigestersList = knownDigestersList2;
                            perPackageReadTimeouts = perPackageReadTimeouts2;
                        } else {
                            int uid = UserHandle.getUid(userId, ps.getAppId());
                            PerUidReadTimeouts perUid = new PerUidReadTimeouts();
                            defaultTimeouts = defaultTimeouts2;
                            perUid.uid = uid;
                            knownDigestersList = knownDigestersList2;
                            perPackageReadTimeouts = perPackageReadTimeouts2;
                            perUid.minTimeUs = perPackage.timeouts.minTimeUs;
                            perUid.minPendingTimeUs = perPackage.timeouts.minPendingTimeUs;
                            perUid.maxPendingTimeUs = perPackage.timeouts.maxPendingTimeUs;
                            result.add(perUid);
                        }
                        i2++;
                        defaultTimeouts2 = defaultTimeouts;
                        knownDigestersList2 = knownDigestersList;
                        perPackageReadTimeouts2 = perPackageReadTimeouts;
                    }
                }
            }
            i++;
            defaultTimeouts2 = defaultTimeouts2;
            knownDigestersList2 = knownDigestersList2;
            perPackageReadTimeouts2 = perPackageReadTimeouts2;
        }
        return (PerUidReadTimeouts[]) result.toArray(new PerUidReadTimeouts[result.size()]);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setKeepUninstalledPackagesInternal(Computer snapshot, List<String> packageList) {
        Preconditions.checkNotNull(packageList);
        synchronized (this.mKeepUninstalledPackages) {
            List<String> toRemove = new ArrayList<>(this.mKeepUninstalledPackages);
            toRemove.removeAll(packageList);
            this.mKeepUninstalledPackages.clear();
            this.mKeepUninstalledPackages.addAll(packageList);
            for (int i = 0; i < toRemove.size(); i++) {
                deletePackageIfUnused(snapshot, toRemove.get(i));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldKeepUninstalledPackageLPr(String packageName) {
        boolean contains;
        synchronized (this.mKeepUninstalledPackages) {
            contains = this.mKeepUninstalledPackages.contains(packageName);
        }
        return contains;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getSafeMode() {
        return this.mSafeMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComponentName getResolveComponentName() {
        return this.mResolveComponentName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DefaultAppProvider getDefaultAppProvider() {
        return this.mDefaultAppProvider;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public File getCacheDir() {
        return this.mCacheDir;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageProperty getPackageProperty() {
        return this.mPackageProperty;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WatchedArrayMap<ComponentName, ParsedInstrumentation> getInstrumentation() {
        return this.mInstrumentation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSdkVersion() {
        return this.mSdkVersion;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addAllPackageProperties(AndroidPackage pkg) {
        this.mPackageProperty.addAllProperties(pkg);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addInstrumentation(ComponentName name, ParsedInstrumentation instrumentation) {
        this.mInstrumentation.put(name, instrumentation);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String[] getKnownPackageNamesInternal(Computer snapshot, int knownPackage, int userId) {
        return new KnownPackages(this.mDefaultAppProvider, this.mRequiredInstallerPackage, this.mRequiredUninstallerPackage, this.mSetupWizardPackage, this.mRequiredVerifierPackage, this.mDefaultTextClassifierPackage, this.mSystemTextClassifierPackageName, this.mRequiredPermissionControllerPackage, this.mConfiguratorPackage, this.mIncidentReportApproverPackage, this.mAmbientContextDetectionPackage, this.mAppPredictionServicePackage, COMPANION_PACKAGE_NAME, this.mRetailDemoPackage, this.mOverlayConfigSignaturePackage, this.mRecentsPackage).getKnownPackageNames(snapshot, knownPackage, userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getActiveLauncherPackageName(int userId) {
        return this.mDefaultAppProvider.getDefaultHome(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setActiveLauncherPackage(String packageName, int userId, Consumer<Boolean> callback) {
        return this.mDefaultAppProvider.setDefaultHome(packageName, userId, this.mContext.getMainExecutor(), callback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDefaultBrowser(String packageName, boolean async, int userId) {
        this.mDefaultAppProvider.setDefaultBrowser(packageName, async, userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageUsage getPackageUsage() {
        return this.mPackageUsage;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getModuleMetadataPackageName() {
        return this.mModuleInfoProvider.getPackageName();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public File getAppInstallDir() {
        return this.mAppInstallDir;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isExpectingBetter(String packageName) {
        return this.mInitAppsHelper.isExpectingBetter(packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getDefParseFlags() {
        return this.mDefParseFlags;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setUpCustomResolverActivity(AndroidPackage pkg, PackageSetting pkgSetting) {
        synchronized (this.mLock) {
            this.mResolverReplaced = true;
            ApplicationInfo appInfo = PackageInfoUtils.generateApplicationInfo(pkg, 0L, PackageUserStateInternal.DEFAULT, 0, pkgSetting);
            this.mResolveActivity.applicationInfo = appInfo;
            this.mResolveActivity.name = this.mCustomResolverComponentName.getClassName();
            this.mResolveActivity.packageName = pkg.getPackageName();
            this.mResolveActivity.processName = pkg.getProcessName();
            this.mResolveActivity.launchMode = 0;
            this.mResolveActivity.flags = 288;
            this.mResolveActivity.theme = 16974374;
            this.mResolveActivity.exported = true;
            this.mResolveActivity.enabled = true;
            this.mResolveInfo.activityInfo = this.mResolveActivity;
            this.mResolveInfo.priority = 0;
            this.mResolveInfo.preferredOrder = 0;
            this.mResolveInfo.match = 0;
            this.mResolveComponentName = this.mCustomResolverComponentName;
            onChanged();
            PackageManagerServiceUtils.sCustomResolverUid = appInfo.uid;
            Slog.i(TAG, "Replacing default ResolverActivity with custom activity: " + this.mResolveComponentName);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPlatformPackage(AndroidPackage pkg, PackageSetting pkgSetting) {
        synchronized (this.mLock) {
            this.mPlatformPackage = pkg;
            ApplicationInfo generateApplicationInfo = PackageInfoUtils.generateApplicationInfo(pkg, 0L, PackageUserStateInternal.DEFAULT, 0, pkgSetting);
            this.mAndroidApplication = generateApplicationInfo;
            if (!this.mResolverReplaced) {
                this.mResolveActivity.applicationInfo = generateApplicationInfo;
                this.mResolveActivity.name = ResolverActivity.class.getName();
                this.mResolveActivity.packageName = this.mAndroidApplication.packageName;
                this.mResolveActivity.processName = "system:ui";
                this.mResolveActivity.launchMode = 0;
                this.mResolveActivity.documentLaunchMode = 3;
                this.mResolveActivity.flags = 4128;
                this.mResolveActivity.theme = 16974374;
                this.mResolveActivity.exported = true;
                this.mResolveActivity.enabled = true;
                this.mResolveActivity.resizeMode = 2;
                this.mResolveActivity.configChanges = 3504;
                this.mResolveInfo.activityInfo = this.mResolveActivity;
                this.mResolveInfo.priority = 0;
                this.mResolveInfo.preferredOrder = 0;
                this.mResolveInfo.match = 0;
                this.mResolveComponentName = new ComponentName(this.mAndroidApplication.packageName, this.mResolveActivity.name);
            }
            onChanged();
        }
        applyUpdatedSystemOverlayPaths();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ApplicationInfo getCoreAndroidApplication() {
        return this.mAndroidApplication;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSystemReady() {
        return this.mSystemReady;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AndroidPackage getPlatformPackage() {
        return this.mPlatformPackage;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPreNUpgrade() {
        return this.mIsPreNUpgrade;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPreNMR1Upgrade() {
        return this.mIsPreNMR1Upgrade;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isOverlayMutable(String packageName) {
        return this.mOverlayConfig.isMutable(packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSystemPackageScanFlags(File codePath) {
        List<ScanPartition> dirsToScanAsSystem = this.mInitAppsHelper.getDirsToScanAsSystem();
        for (int i = dirsToScanAsSystem.size() - 1; i >= 0; i--) {
            ScanPartition partition = dirsToScanAsSystem.get(i);
            if (partition.containsFile(codePath)) {
                int scanFlags = 65536 | partition.scanFlag;
                if (partition.containsPrivApp(codePath)) {
                    return scanFlags | 131072;
                }
                return scanFlags;
            }
        }
        return 65536;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Pair<Integer, Integer> getSystemPackageRescanFlagsAndReparseFlags(File scanFile, int systemScanFlags, int systemParseFlags) {
        List<ScanPartition> dirsToScanAsSystem = this.mInitAppsHelper.getDirsToScanAsSystem();
        int reparseFlags = 0;
        int rescanFlags = 0;
        int i1 = dirsToScanAsSystem.size() - 1;
        while (true) {
            if (i1 < 0) {
                break;
            }
            ScanPartition partition = dirsToScanAsSystem.get(i1);
            if (partition.containsPrivApp(scanFile)) {
                reparseFlags = systemParseFlags;
                rescanFlags = 131072 | systemScanFlags | partition.scanFlag;
                break;
            } else if (!partition.containsApp(scanFile)) {
                i1--;
            } else {
                reparseFlags = systemParseFlags;
                rescanFlags = systemScanFlags | partition.scanFlag;
                break;
            }
        }
        return new Pair<>(Integer.valueOf(rescanFlags), Integer.valueOf(reparseFlags));
    }

    public PackageStateMutator.InitialState recordInitialState() {
        return this.mPackageStateMutator.initialState(this.mChangedPackagesTracker.getSequenceNumber());
    }

    public PackageStateMutator.Result commitPackageStateMutation(PackageStateMutator.InitialState initialState, Consumer<PackageStateMutator> consumer) {
        synchronized (this.mPackageStateWriteLock) {
            PackageStateMutator.Result result = this.mPackageStateMutator.generateResult(initialState, this.mChangedPackagesTracker.getSequenceNumber());
            if (result != PackageStateMutator.Result.SUCCESS) {
                return result;
            }
            consumer.accept(this.mPackageStateMutator);
            onChanged();
            return PackageStateMutator.Result.SUCCESS;
        }
    }

    public PackageStateMutator.Result commitPackageStateMutation(PackageStateMutator.InitialState initialState, String packageName, Consumer<PackageStateWrite> consumer) {
        PackageStateMutator.Result result = null;
        if (Thread.holdsLock(this.mPackageStateWriteLock)) {
            result = PackageStateMutator.Result.SUCCESS;
        }
        synchronized (this.mPackageStateWriteLock) {
            if (result == null) {
                result = this.mPackageStateMutator.generateResult(initialState, this.mChangedPackagesTracker.getSequenceNumber());
            }
            if (result != PackageStateMutator.Result.SUCCESS) {
                return result;
            }
            PackageStateWrite state = this.mPackageStateMutator.forPackage(packageName);
            if (state == null) {
                return PackageStateMutator.Result.SPECIFIC_PACKAGE_NULL;
            }
            consumer.accept(state);
            state.onChanged();
            return PackageStateMutator.Result.SUCCESS;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyInstantAppPackageInstalled(String packageName, int[] newUsers) {
        this.mInstantAppRegistry.onPackageInstalled(snapshotComputer(), packageName, newUsers);
    }

    public void onAmsAddedtoServiceMgr() {
        int[] userIds;
        if (!CtaManagerFactory.getInstance().makeCtaManager().isCtaSupported() || !this.mIsPreNUpgrade) {
            return;
        }
        for (int userId : UserManagerService.getInstance().getUserIds()) {
            this.mPermissionManager.getDefaultPermissionGrantPolicy().grantCtaPermToPreInstalledPackage(userId);
        }
    }

    public void setGriffinPackages(AndroidPackage pkg) {
        ITranPackageManagerService.Instance().setGriffinPackages(pkg);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addInstallerPackageName(InstallSource installSource) {
        synchronized (this.mLock) {
            this.mSettings.addInstallerPackageNames(installSource);
        }
    }
}
