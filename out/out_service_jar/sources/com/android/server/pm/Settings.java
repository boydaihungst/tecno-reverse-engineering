package com.android.server.pm;

import android.app.compat.ChangeIdStateCache;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PackagePartitions;
import android.content.pm.PermissionInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.pm.SuspendDialogInfo;
import android.content.pm.UserInfo;
import android.content.pm.VerifierDeviceIdentity;
import android.content.pm.overlay.OverlayPaths;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.CreateAppDataArgs;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Message;
import android.os.PatternMatcher;
import android.os.PersistableBundle;
import android.os.SELinux;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.IntArray;
import android.util.Log;
import android.util.LogPrinter;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import android.util.proto.ProtoOutputStream;
import com.android.internal.logging.EventLogTags;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.JournaledFile;
import com.android.internal.util.XmlUtils;
import com.android.permission.persistence.RuntimePermissionsPersistence;
import com.android.permission.persistence.RuntimePermissionsState;
import com.android.server.LocalServices;
import com.android.server.UiModeManagerService;
import com.android.server.am.HostingRecord;
import com.android.server.pm.Installer;
import com.android.server.pm.Settings;
import com.android.server.pm.parsing.PackageInfoUtils;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.permission.LegacyPermissionDataProvider;
import com.android.server.pm.permission.LegacyPermissionSettings;
import com.android.server.pm.permission.LegacyPermissionState;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.PackageUserState;
import com.android.server.pm.pkg.PackageUserStateInternal;
import com.android.server.pm.pkg.SuspendParams;
import com.android.server.pm.pkg.component.ParsedComponent;
import com.android.server.pm.pkg.component.ParsedIntentInfo;
import com.android.server.pm.pkg.component.ParsedPermission;
import com.android.server.pm.pkg.component.ParsedProcess;
import com.android.server.pm.pkg.parsing.PackageInfoWithoutStateUtils;
import com.android.server.pm.resolution.ComponentResolver;
import com.android.server.pm.snapshot.PackageDataSnapshot;
import com.android.server.pm.verify.domain.DomainVerificationManagerInternal;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.utils.Slogf;
import com.android.server.utils.Snappable;
import com.android.server.utils.SnapshotCache;
import com.android.server.utils.TimingsTraceAndSlog;
import com.android.server.utils.Watchable;
import com.android.server.utils.WatchableImpl;
import com.android.server.utils.Watched;
import com.android.server.utils.WatchedArrayList;
import com.android.server.utils.WatchedArrayMap;
import com.android.server.utils.WatchedArraySet;
import com.android.server.utils.WatchedSparseArray;
import com.android.server.utils.WatchedSparseIntArray;
import com.android.server.utils.Watcher;
import com.android.server.voiceinteraction.DatabaseHelper;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.pm.PmsExt;
import com.transsion.hubcore.server.pm.ITranPackageManagerService;
import com.transsion.xmlprotect.VerifyExt;
import com.transsion.xmlprotect.VerifyFacotry;
import dalvik.annotation.optimization.NeverCompile;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
/* loaded from: classes2.dex */
public final class Settings implements Watchable, Snappable {
    private static final String ATTR_APP_LINK_GENERATION = "app-link-generation";
    private static final String ATTR_APP_TRUSTED = "app-trusted";
    private static final String ATTR_BLOCKED = "blocked";
    @Deprecated
    private static final String ATTR_BLOCK_UNINSTALL = "blockUninstall";
    private static final String ATTR_CE_DATA_INODE = "ceDataInode";
    private static final String ATTR_DATABASE_VERSION = "databaseVersion";
    private static final String ATTR_DISTRACTION_FLAGS = "distraction_flags";
    private static final String ATTR_DOMAIN_VERIFICATON_STATE = "domainVerificationStatus";
    private static final String ATTR_ENABLED = "enabled";
    private static final String ATTR_ENABLED_CALLER = "enabledCaller";
    private static final String ATTR_ENFORCEMENT = "enforcement";
    private static final String ATTR_FINGERPRINT = "fingerprint";
    private static final String ATTR_FIRST_INSTALL_TIME = "first-install-time";
    private static final String ATTR_FLAGS = "flags";
    private static final String ATTR_GRANTED = "granted";
    private static final String ATTR_HARMFUL_APP_WARNING = "harmful-app-warning";
    private static final String ATTR_HIDDEN = "hidden";
    private static final String ATTR_INSTALLED = "inst";
    private static final String ATTR_INSTALL_REASON = "install-reason";
    private static final String ATTR_INSTANT_APP = "instant-app";
    public static final String ATTR_NAME = "name";
    private static final String ATTR_NOTIFY_SCREEN_ON = "notify-screen-on";
    private static final String ATTR_NOT_LAUNCHED = "nl";
    public static final String ATTR_PACKAGE = "package";
    private static final String ATTR_PACKAGE_NAME = "packageName";
    private static final String ATTR_SDK_VERSION = "sdkVersion";
    private static final String ATTR_SPLASH_SCREEN_THEME = "splash-screen-theme";
    private static final String ATTR_STOPPED = "stopped";
    private static final String ATTR_SUSPENDED = "suspended";
    private static final String ATTR_SUSPENDING_PACKAGE = "suspending-package";
    @Deprecated
    private static final String ATTR_SUSPEND_DIALOG_MESSAGE = "suspend_dialog_message";
    private static final String ATTR_UNINSTALL_REASON = "uninstall-reason";
    private static final String ATTR_VALUE = "value";
    private static final String ATTR_VERSION = "version";
    private static final String ATTR_VIRTUAL_PRELOAD = "virtual-preload";
    private static final String ATTR_VOLUME_UUID = "volumeUuid";
    public static final int CURRENT_DATABASE_VERSION = 3;
    private static final boolean DEBUG_KERNEL = false;
    private static final boolean DEBUG_MU = false;
    private static final boolean DEBUG_PARSER = false;
    static final boolean DEBUG_STOPPED = false;
    private static final String RUNTIME_PERMISSIONS_FILE_NAME = "runtime-permissions.xml";
    private static final String TAG = "PackageSettings";
    public static final String TAG_ALL_INTENT_FILTER_VERIFICATION = "all-intent-filter-verifications";
    private static final String TAG_BLOCK_UNINSTALL = "block-uninstall";
    private static final String TAG_BLOCK_UNINSTALL_PACKAGES = "block-uninstall-packages";
    private static final String TAG_CHILD_PACKAGE = "child-package";
    static final String TAG_CROSS_PROFILE_INTENT_FILTERS = "crossProfile-intent-filters";
    private static final String TAG_DEFAULT_APPS = "default-apps";
    private static final String TAG_DEFAULT_BROWSER = "default-browser";
    private static final String TAG_DEFAULT_DIALER = "default-dialer";
    private static final String TAG_DISABLED_COMPONENTS = "disabled-components";
    public static final String TAG_DOMAIN_VERIFICATION = "domain-verification";
    private static final String TAG_ENABLED_COMPONENTS = "enabled-components";
    public static final String TAG_ITEM = "item";
    private static final String TAG_MIME_GROUP = "mime-group";
    private static final String TAG_MIME_TYPE = "mime-type";
    private static final String TAG_PACKAGE = "pkg";
    private static final String TAG_PACKAGE_RESTRICTIONS = "package-restrictions";
    private static final String TAG_PERMISSIONS = "perms";
    private static final String TAG_PERSISTENT_PREFERRED_ACTIVITIES = "persistent-preferred-activities";
    private static final String TAG_READ_EXTERNAL_STORAGE = "read-external-storage";
    private static final String TAG_RUNTIME_PERMISSIONS = "runtime-permissions";
    private static final String TAG_SHARED_USER = "shared-user";
    @Deprecated
    private static final String TAG_SUSPENDED_APP_EXTRAS = "suspended-app-extras";
    @Deprecated
    private static final String TAG_SUSPENDED_DIALOG_INFO = "suspended-dialog-info";
    @Deprecated
    private static final String TAG_SUSPENDED_LAUNCHER_EXTRAS = "suspended-launcher-extras";
    private static final String TAG_SUSPEND_PARAMS = "suspend-params";
    private static final String TAG_USES_SDK_LIB = "uses-sdk-lib";
    private static final String TAG_USES_STATIC_LIB = "uses-static-lib";
    private static final String TAG_VERSION = "version";
    @Watched(manual = true)
    private final AppIdSettingMap mAppIds;
    private final File mBackupSettingsFilename;
    private final File mBackupStoppedPackagesFilename;
    @Watched
    private final WatchedSparseArray<ArraySet<String>> mBlockUninstallPackages;
    @Watched
    private final WatchedSparseArray<CrossProfileIntentResolver> mCrossProfileIntentResolvers;
    private final SnapshotCache<WatchedSparseArray<CrossProfileIntentResolver>> mCrossProfileIntentResolversSnapshot;
    @Watched
    final WatchedSparseArray<String> mDefaultBrowserApp;
    @Watched
    final WatchedArrayMap<String, PackageSetting> mDisabledSysPackages;
    @Watched(manual = true)
    private final DomainVerificationManagerInternal mDomainVerificationManager;
    private final Handler mHandler;
    @Watched
    private final WatchedArraySet<String> mInstallerPackages;
    private final SnapshotCache<WatchedArraySet<String>> mInstallerPackagesSnapshot;
    @Watched
    private final WatchedArrayMap<String, KernelPackageState> mKernelMapping;
    private final File mKernelMappingFilename;
    private final SnapshotCache<WatchedArrayMap<String, KernelPackageState>> mKernelMappingSnapshot;
    private final KeySetManagerService mKeySetManagerService;
    @Watched
    private final WatchedArrayMap<Long, Integer> mKeySetRefs;
    private final SnapshotCache<WatchedArrayMap<Long, Integer>> mKeySetRefsSnapshot;
    private final PackageManagerTracedLock mLock;
    @Watched
    private final WatchedSparseIntArray mNextAppLinkGeneration;
    private final Watcher mObserver;
    private final File mPackageListFilename;
    @Watched
    final WatchedArrayMap<String, PackageSetting> mPackages;
    private final SnapshotCache<WatchedArrayMap<String, PackageSetting>> mPackagesSnapshot;
    @Watched
    private final WatchedArrayList<Signature> mPastSignatures;
    private final SnapshotCache<WatchedArrayList<Signature>> mPastSignaturesSnapshot;
    @Watched
    private final WatchedArrayList<PackageSetting> mPendingPackages;
    private final SnapshotCache<WatchedArrayList<PackageSetting>> mPendingPackagesSnapshot;
    @Watched(manual = true)
    private final LegacyPermissionDataProvider mPermissionDataProvider;
    @Watched(manual = true)
    final LegacyPermissionSettings mPermissions;
    @Watched
    private final WatchedSparseArray<PersistentPreferredIntentResolver> mPersistentPreferredActivities;
    private final SnapshotCache<WatchedSparseArray<PersistentPreferredIntentResolver>> mPersistentPreferredActivitiesSnapshot;
    @Watched
    private final WatchedSparseArray<PreferredIntentResolver> mPreferredActivities;
    private final SnapshotCache<WatchedSparseArray<PreferredIntentResolver>> mPreferredActivitiesSnapshot;
    final StringBuilder mReadMessages;
    @Watched
    private final WatchedArrayMap<String, String> mRenamedPackages;
    @Watched(manual = true)
    private final RuntimePermissionPersistence mRuntimePermissionsPersistence;
    private final File mSettingsFilename;
    @Watched
    final WatchedArrayMap<String, SharedUserSetting> mSharedUsers;
    private final SnapshotCache<Settings> mSnapshot;
    private final File mStoppedPackagesFilename;
    private final File mSystemDir;
    @Watched(manual = true)
    private VerifierDeviceIdentity mVerifierDeviceIdentity;
    @Watched
    private final WatchedArrayMap<String, VersionInfo> mVersion;
    private final WatchableImpl mWatchable;
    private final boolean mXmlSupport;
    private static PmsExt sPmsExt = MtkSystemServiceFactory.getInstance().makePmsExt();
    private static int PRE_M_APP_INFO_FLAG_HIDDEN = 134217728;
    private static int PRE_M_APP_INFO_FLAG_CANT_SAVE_STATE = 268435456;
    private static int PRE_M_APP_INFO_FLAG_PRIVILEGED = 1073741824;
    static final Object[] FLAG_DUMP_SPEC = {1, "SYSTEM", 2, "DEBUGGABLE", 4, "HAS_CODE", 8, "PERSISTENT", 16, "FACTORY_TEST", 32, "ALLOW_TASK_REPARENTING", 64, "ALLOW_CLEAR_USER_DATA", 128, "UPDATED_SYSTEM_APP", 256, "TEST_ONLY", 16384, "VM_SAFE_MODE", 32768, "ALLOW_BACKUP", 65536, "KILL_AFTER_RESTORE", 131072, "RESTORE_ANY_VERSION", 262144, "EXTERNAL_STORAGE", 1048576, "LARGE_HEAP"};
    private static final Object[] PRIVATE_FLAG_DUMP_SPEC = {1024, "PRIVATE_FLAG_ACTIVITIES_RESIZE_MODE_RESIZEABLE", 4096, "PRIVATE_FLAG_ACTIVITIES_RESIZE_MODE_RESIZEABLE_VIA_SDK_VERSION", 2048, "PRIVATE_FLAG_ACTIVITIES_RESIZE_MODE_UNRESIZEABLE", 134217728, "ALLOW_AUDIO_PLAYBACK_CAPTURE", 536870912, "PRIVATE_FLAG_REQUEST_LEGACY_EXTERNAL_STORAGE", 8192, "BACKUP_IN_FOREGROUND", 2, "CANT_SAVE_STATE", 32, "DEFAULT_TO_DEVICE_PROTECTED_STORAGE", 64, "DIRECT_BOOT_AWARE", 16, "HAS_DOMAIN_URLS", 1, "HIDDEN", 128, "EPHEMERAL", 32768, "ISOLATED_SPLIT_LOADING", 131072, "OEM", 256, "PARTIALLY_DIRECT_BOOT_AWARE", 8, "PRIVILEGED", 512, "REQUIRED_FOR_SYSTEM_USER", 16384, "STATIC_SHARED_LIBRARY", 262144, "VENDOR", 524288, "PRODUCT", 2097152, "SYSTEM_EXT", 65536, "VIRTUAL_PRELOAD", 1073741824, "ODM", Integer.MIN_VALUE, "PRIVATE_FLAG_ALLOW_NATIVE_HEAP_POINTER_TAGGING"};

    /* loaded from: classes2.dex */
    public static class DatabaseVersion {
        public static final int FIRST_VERSION = 1;
        public static final int SIGNATURE_END_ENTITY = 2;
        public static final int SIGNATURE_MALFORMED_RECOVER = 3;
    }

    @Override // com.android.server.utils.Watchable
    public void registerObserver(Watcher observer) {
        this.mWatchable.registerObserver(observer);
    }

    @Override // com.android.server.utils.Watchable
    public void unregisterObserver(Watcher observer) {
        this.mWatchable.unregisterObserver(observer);
    }

    @Override // com.android.server.utils.Watchable
    public boolean isRegisteredObserver(Watcher observer) {
        return this.mWatchable.isRegisteredObserver(observer);
    }

    @Override // com.android.server.utils.Watchable
    public void dispatchChange(Watchable what) {
        this.mWatchable.dispatchChange(what);
    }

    protected void onChanged() {
        dispatchChange(this);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class KernelPackageState {
        int appId;
        int[] excludedUserIds;

        private KernelPackageState() {
        }
    }

    /* loaded from: classes2.dex */
    public static class VersionInfo {
        int databaseVersion;
        String fingerprint;
        int sdkVersion;

        public void forceCurrent() {
            this.sdkVersion = Build.VERSION.SDK_INT;
            this.databaseVersion = 3;
            this.fingerprint = PackagePartitions.FINGERPRINT;
        }
    }

    private SnapshotCache<Settings> makeCache() {
        return new SnapshotCache<Settings>(this, this) { // from class: com.android.server.pm.Settings.2
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // com.android.server.utils.SnapshotCache
            public Settings createSnapshot() {
                Settings s = new Settings();
                s.mWatchable.seal();
                return s;
            }
        };
    }

    private void registerObservers() {
        this.mPackages.registerObserver(this.mObserver);
        this.mInstallerPackages.registerObserver(this.mObserver);
        this.mKernelMapping.registerObserver(this.mObserver);
        this.mDisabledSysPackages.registerObserver(this.mObserver);
        this.mBlockUninstallPackages.registerObserver(this.mObserver);
        this.mVersion.registerObserver(this.mObserver);
        this.mPreferredActivities.registerObserver(this.mObserver);
        this.mPersistentPreferredActivities.registerObserver(this.mObserver);
        this.mCrossProfileIntentResolvers.registerObserver(this.mObserver);
        this.mSharedUsers.registerObserver(this.mObserver);
        this.mAppIds.registerObserver(this.mObserver);
        this.mRenamedPackages.registerObserver(this.mObserver);
        this.mNextAppLinkGeneration.registerObserver(this.mObserver);
        this.mDefaultBrowserApp.registerObserver(this.mObserver);
        this.mPendingPackages.registerObserver(this.mObserver);
        this.mPastSignatures.registerObserver(this.mObserver);
        this.mKeySetRefs.registerObserver(this.mObserver);
    }

    public Settings(Map<String, PackageSetting> pkgSettings) {
        this.mWatchable = new WatchableImpl();
        this.mXmlSupport = "1".equals(SystemProperties.get("ro.vendor.tran.xml.support", "0"));
        this.mDisabledSysPackages = new WatchedArrayMap<>();
        this.mBlockUninstallPackages = new WatchedSparseArray<>();
        this.mVersion = new WatchedArrayMap<>();
        this.mSharedUsers = new WatchedArrayMap<>();
        this.mRenamedPackages = new WatchedArrayMap<>();
        this.mDefaultBrowserApp = new WatchedSparseArray<>();
        this.mNextAppLinkGeneration = new WatchedSparseIntArray();
        this.mReadMessages = new StringBuilder();
        Watcher watcher = new Watcher() { // from class: com.android.server.pm.Settings.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                Settings.this.dispatchChange(what);
            }
        };
        this.mObserver = watcher;
        WatchedArrayMap<String, PackageSetting> watchedArrayMap = new WatchedArrayMap<>();
        this.mPackages = watchedArrayMap;
        this.mPackagesSnapshot = new SnapshotCache.Auto(watchedArrayMap, watchedArrayMap, "Settings.mPackages");
        WatchedArrayMap<String, KernelPackageState> watchedArrayMap2 = new WatchedArrayMap<>();
        this.mKernelMapping = watchedArrayMap2;
        this.mKernelMappingSnapshot = new SnapshotCache.Auto(watchedArrayMap2, watchedArrayMap2, "Settings.mKernelMapping");
        WatchedArraySet<String> watchedArraySet = new WatchedArraySet<>();
        this.mInstallerPackages = watchedArraySet;
        this.mInstallerPackagesSnapshot = new SnapshotCache.Auto(watchedArraySet, watchedArraySet, "Settings.mInstallerPackages");
        WatchedSparseArray<PreferredIntentResolver> watchedSparseArray = new WatchedSparseArray<>();
        this.mPreferredActivities = watchedSparseArray;
        this.mPreferredActivitiesSnapshot = new SnapshotCache.Auto(watchedSparseArray, watchedSparseArray, "Settings.mPreferredActivities");
        WatchedSparseArray<PersistentPreferredIntentResolver> watchedSparseArray2 = new WatchedSparseArray<>();
        this.mPersistentPreferredActivities = watchedSparseArray2;
        this.mPersistentPreferredActivitiesSnapshot = new SnapshotCache.Auto(watchedSparseArray2, watchedSparseArray2, "Settings.mPersistentPreferredActivities");
        WatchedSparseArray<CrossProfileIntentResolver> watchedSparseArray3 = new WatchedSparseArray<>();
        this.mCrossProfileIntentResolvers = watchedSparseArray3;
        this.mCrossProfileIntentResolversSnapshot = new SnapshotCache.Auto(watchedSparseArray3, watchedSparseArray3, "Settings.mCrossProfileIntentResolvers");
        WatchedArrayList<Signature> watchedArrayList = new WatchedArrayList<>();
        this.mPastSignatures = watchedArrayList;
        this.mPastSignaturesSnapshot = new SnapshotCache.Auto(watchedArrayList, watchedArrayList, "Settings.mPastSignatures");
        WatchedArrayMap<Long, Integer> watchedArrayMap3 = new WatchedArrayMap<>();
        this.mKeySetRefs = watchedArrayMap3;
        this.mKeySetRefsSnapshot = new SnapshotCache.Auto(watchedArrayMap3, watchedArrayMap3, "Settings.mKeySetRefs");
        WatchedArrayList<PackageSetting> watchedArrayList2 = new WatchedArrayList<>();
        this.mPendingPackages = watchedArrayList2;
        this.mPendingPackagesSnapshot = new SnapshotCache.Auto(watchedArrayList2, watchedArrayList2, "Settings.mPendingPackages");
        this.mKeySetManagerService = new KeySetManagerService(watchedArrayMap);
        this.mHandler = new Handler(BackgroundThread.getHandler().getLooper());
        this.mLock = new PackageManagerTracedLock();
        watchedArrayMap.putAll(pkgSettings);
        this.mAppIds = new AppIdSettingMap();
        this.mSystemDir = null;
        this.mPermissions = null;
        this.mRuntimePermissionsPersistence = null;
        this.mPermissionDataProvider = null;
        this.mSettingsFilename = null;
        this.mBackupSettingsFilename = null;
        this.mPackageListFilename = null;
        this.mStoppedPackagesFilename = null;
        this.mBackupStoppedPackagesFilename = null;
        this.mKernelMappingFilename = null;
        this.mDomainVerificationManager = null;
        registerObservers();
        Watchable.verifyWatchedAttributes(this, watcher);
        this.mSnapshot = makeCache();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Settings(File dataDir, RuntimePermissionsPersistence runtimePermissionsPersistence, LegacyPermissionDataProvider permissionDataProvider, DomainVerificationManagerInternal domainVerificationManager, Handler handler, PackageManagerTracedLock lock) {
        this.mWatchable = new WatchableImpl();
        this.mXmlSupport = "1".equals(SystemProperties.get("ro.vendor.tran.xml.support", "0"));
        this.mDisabledSysPackages = new WatchedArrayMap<>();
        this.mBlockUninstallPackages = new WatchedSparseArray<>();
        this.mVersion = new WatchedArrayMap<>();
        this.mSharedUsers = new WatchedArrayMap<>();
        this.mRenamedPackages = new WatchedArrayMap<>();
        this.mDefaultBrowserApp = new WatchedSparseArray<>();
        this.mNextAppLinkGeneration = new WatchedSparseIntArray();
        this.mReadMessages = new StringBuilder();
        Watcher watcher = new Watcher() { // from class: com.android.server.pm.Settings.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                Settings.this.dispatchChange(what);
            }
        };
        this.mObserver = watcher;
        WatchedArrayMap<String, PackageSetting> watchedArrayMap = new WatchedArrayMap<>();
        this.mPackages = watchedArrayMap;
        this.mPackagesSnapshot = new SnapshotCache.Auto(watchedArrayMap, watchedArrayMap, "Settings.mPackages");
        WatchedArrayMap<String, KernelPackageState> watchedArrayMap2 = new WatchedArrayMap<>();
        this.mKernelMapping = watchedArrayMap2;
        this.mKernelMappingSnapshot = new SnapshotCache.Auto(watchedArrayMap2, watchedArrayMap2, "Settings.mKernelMapping");
        WatchedArraySet<String> watchedArraySet = new WatchedArraySet<>();
        this.mInstallerPackages = watchedArraySet;
        this.mInstallerPackagesSnapshot = new SnapshotCache.Auto(watchedArraySet, watchedArraySet, "Settings.mInstallerPackages");
        WatchedSparseArray<PreferredIntentResolver> watchedSparseArray = new WatchedSparseArray<>();
        this.mPreferredActivities = watchedSparseArray;
        this.mPreferredActivitiesSnapshot = new SnapshotCache.Auto(watchedSparseArray, watchedSparseArray, "Settings.mPreferredActivities");
        WatchedSparseArray<PersistentPreferredIntentResolver> watchedSparseArray2 = new WatchedSparseArray<>();
        this.mPersistentPreferredActivities = watchedSparseArray2;
        this.mPersistentPreferredActivitiesSnapshot = new SnapshotCache.Auto(watchedSparseArray2, watchedSparseArray2, "Settings.mPersistentPreferredActivities");
        WatchedSparseArray<CrossProfileIntentResolver> watchedSparseArray3 = new WatchedSparseArray<>();
        this.mCrossProfileIntentResolvers = watchedSparseArray3;
        this.mCrossProfileIntentResolversSnapshot = new SnapshotCache.Auto(watchedSparseArray3, watchedSparseArray3, "Settings.mCrossProfileIntentResolvers");
        WatchedArrayList<Signature> watchedArrayList = new WatchedArrayList<>();
        this.mPastSignatures = watchedArrayList;
        this.mPastSignaturesSnapshot = new SnapshotCache.Auto(watchedArrayList, watchedArrayList, "Settings.mPastSignatures");
        WatchedArrayMap<Long, Integer> watchedArrayMap3 = new WatchedArrayMap<>();
        this.mKeySetRefs = watchedArrayMap3;
        this.mKeySetRefsSnapshot = new SnapshotCache.Auto(watchedArrayMap3, watchedArrayMap3, "Settings.mKeySetRefs");
        WatchedArrayList<PackageSetting> watchedArrayList2 = new WatchedArrayList<>();
        this.mPendingPackages = watchedArrayList2;
        this.mPendingPackagesSnapshot = new SnapshotCache.Auto(watchedArrayList2, watchedArrayList2, "Settings.mPendingPackages");
        this.mKeySetManagerService = new KeySetManagerService(watchedArrayMap);
        this.mHandler = handler;
        this.mLock = lock;
        this.mAppIds = new AppIdSettingMap();
        this.mPermissions = new LegacyPermissionSettings(lock);
        this.mRuntimePermissionsPersistence = new RuntimePermissionPersistence(runtimePermissionsPersistence, new Consumer<Integer>() { // from class: com.android.server.pm.Settings.3
            /* JADX DEBUG: Method merged with bridge method */
            @Override // java.util.function.Consumer
            public void accept(Integer userId) {
                Settings.this.mRuntimePermissionsPersistence.writeStateForUser(userId.intValue(), Settings.this.mPermissionDataProvider, Settings.this.mPackages, Settings.this.mSharedUsers, Settings.this.mHandler, Settings.this.mLock, false);
            }
        });
        this.mPermissionDataProvider = permissionDataProvider;
        File file = new File(dataDir, HostingRecord.HOSTING_TYPE_SYSTEM);
        this.mSystemDir = file;
        file.mkdirs();
        FileUtils.setPermissions(file.toString(), 509, -1, -1);
        this.mSettingsFilename = new File(file, "packages.xml");
        this.mBackupSettingsFilename = new File(file, "packages-backup.xml");
        File file2 = new File(file, "packages.list");
        this.mPackageListFilename = file2;
        FileUtils.setPermissions(file2, FrameworkStatsLog.DISPLAY_HBM_STATE_CHANGED, 1000, 1032);
        File kernelDir = new File("/config/sdcardfs");
        this.mKernelMappingFilename = kernelDir.exists() ? kernelDir : null;
        this.mStoppedPackagesFilename = new File(file, "packages-stopped.xml");
        this.mBackupStoppedPackagesFilename = new File(file, "packages-stopped-backup.xml");
        this.mDomainVerificationManager = domainVerificationManager;
        registerObservers();
        Watchable.verifyWatchedAttributes(this, watcher);
        this.mSnapshot = makeCache();
    }

    private Settings(Settings r) {
        this.mWatchable = new WatchableImpl();
        this.mXmlSupport = "1".equals(SystemProperties.get("ro.vendor.tran.xml.support", "0"));
        WatchedArrayMap<String, PackageSetting> watchedArrayMap = new WatchedArrayMap<>();
        this.mDisabledSysPackages = watchedArrayMap;
        WatchedSparseArray<ArraySet<String>> watchedSparseArray = new WatchedSparseArray<>();
        this.mBlockUninstallPackages = watchedSparseArray;
        WatchedArrayMap<String, VersionInfo> watchedArrayMap2 = new WatchedArrayMap<>();
        this.mVersion = watchedArrayMap2;
        WatchedArrayMap<String, SharedUserSetting> watchedArrayMap3 = new WatchedArrayMap<>();
        this.mSharedUsers = watchedArrayMap3;
        WatchedArrayMap<String, String> watchedArrayMap4 = new WatchedArrayMap<>();
        this.mRenamedPackages = watchedArrayMap4;
        WatchedSparseArray<String> watchedSparseArray2 = new WatchedSparseArray<>();
        this.mDefaultBrowserApp = watchedSparseArray2;
        WatchedSparseIntArray watchedSparseIntArray = new WatchedSparseIntArray();
        this.mNextAppLinkGeneration = watchedSparseIntArray;
        this.mReadMessages = new StringBuilder();
        this.mObserver = new Watcher() { // from class: com.android.server.pm.Settings.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                Settings.this.dispatchChange(what);
            }
        };
        WatchedArrayMap<String, PackageSetting> snapshot = r.mPackagesSnapshot.snapshot();
        this.mPackages = snapshot;
        this.mPackagesSnapshot = new SnapshotCache.Sealed();
        this.mKernelMapping = r.mKernelMappingSnapshot.snapshot();
        this.mKernelMappingSnapshot = new SnapshotCache.Sealed();
        this.mInstallerPackages = r.mInstallerPackagesSnapshot.snapshot();
        this.mInstallerPackagesSnapshot = new SnapshotCache.Sealed();
        this.mKeySetManagerService = new KeySetManagerService(r.mKeySetManagerService, snapshot);
        this.mHandler = null;
        this.mLock = null;
        this.mRuntimePermissionsPersistence = r.mRuntimePermissionsPersistence;
        this.mSettingsFilename = null;
        this.mBackupSettingsFilename = null;
        this.mPackageListFilename = null;
        this.mStoppedPackagesFilename = null;
        this.mBackupStoppedPackagesFilename = null;
        this.mKernelMappingFilename = null;
        this.mDomainVerificationManager = r.mDomainVerificationManager;
        watchedArrayMap.snapshot(r.mDisabledSysPackages);
        watchedSparseArray.snapshot(r.mBlockUninstallPackages);
        watchedArrayMap2.putAll(r.mVersion);
        this.mVerifierDeviceIdentity = r.mVerifierDeviceIdentity;
        this.mPreferredActivities = r.mPreferredActivitiesSnapshot.snapshot();
        this.mPreferredActivitiesSnapshot = new SnapshotCache.Sealed();
        this.mPersistentPreferredActivities = r.mPersistentPreferredActivitiesSnapshot.snapshot();
        this.mPersistentPreferredActivitiesSnapshot = new SnapshotCache.Sealed();
        this.mCrossProfileIntentResolvers = r.mCrossProfileIntentResolversSnapshot.snapshot();
        this.mCrossProfileIntentResolversSnapshot = new SnapshotCache.Sealed();
        watchedArrayMap3.snapshot(r.mSharedUsers);
        this.mAppIds = r.mAppIds.snapshot();
        this.mPastSignatures = r.mPastSignaturesSnapshot.snapshot();
        this.mPastSignaturesSnapshot = new SnapshotCache.Sealed();
        this.mKeySetRefs = r.mKeySetRefsSnapshot.snapshot();
        this.mKeySetRefsSnapshot = new SnapshotCache.Sealed();
        watchedArrayMap4.snapshot(r.mRenamedPackages);
        watchedSparseIntArray.snapshot(r.mNextAppLinkGeneration);
        watchedSparseArray2.snapshot(r.mDefaultBrowserApp);
        this.mPendingPackages = r.mPendingPackagesSnapshot.snapshot();
        this.mPendingPackagesSnapshot = new SnapshotCache.Sealed();
        this.mSystemDir = null;
        this.mPermissions = r.mPermissions;
        this.mPermissionDataProvider = r.mPermissionDataProvider;
        this.mSnapshot = new SnapshotCache.Sealed();
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.utils.Snappable
    public Settings snapshot() {
        return this.mSnapshot.snapshot();
    }

    private void invalidatePackageCache() {
        PackageManagerService.invalidatePackageInfoCache();
        ChangeIdStateCache.invalidate();
        onChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageSetting getPackageLPr(String pkgName) {
        return this.mPackages.get(pkgName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WatchedArrayMap<String, PackageSetting> getPackagesLocked() {
        return this.mPackages;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public KeySetManagerService getKeySetManagerService() {
        return this.mKeySetManagerService;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getRenamedPackageLPr(String pkgName) {
        return this.mRenamedPackages.get(pkgName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String addRenamedPackageLPw(String pkgName, String origPkgName) {
        return this.mRenamedPackages.put(pkgName, origPkgName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeRenamedPackageLPw(String pkgName) {
        this.mRenamedPackages.remove(pkgName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void pruneRenamedPackagesLPw() {
        for (int i = this.mRenamedPackages.size() - 1; i >= 0; i--) {
            PackageSetting ps = this.mPackages.get(this.mRenamedPackages.valueAt(i));
            if (ps == null) {
                this.mRenamedPackages.removeAt(i);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SharedUserSetting getSharedUserLPw(String name, int pkgFlags, int pkgPrivateFlags, boolean create) throws PackageManagerException {
        SharedUserSetting s = this.mSharedUsers.get(name);
        if (s == null && create) {
            s = new SharedUserSetting(name, pkgFlags, pkgPrivateFlags);
            s.mAppId = this.mAppIds.acquireAndRegisterNewAppId(s);
            if (s.mAppId < 0) {
                throw new PackageManagerException(-4, "Creating shared user " + name + " failed");
            }
            Log.i("PackageManager", "New shared user " + name + ": id=" + s.mAppId);
            this.mSharedUsers.put(name, s);
        }
        return s;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Collection<SharedUserSetting> getAllSharedUsersLPw() {
        return this.mSharedUsers.values();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean disableSystemPackageLPw(String name, boolean replaced) {
        PackageSetting disabled;
        PackageSetting p = this.mPackages.get(name);
        if (p == null) {
            Log.w("PackageManager", "Package " + name + " is not an installed package");
            return false;
        }
        PackageSetting dp = this.mDisabledSysPackages.get(name);
        if ((dp != null || p.getPkg() == null || !p.getPkg().isSystem() || p.getPkgState().isUpdatedSystemApp()) && (!sPmsExt.isRemovableSysApp(name) || p.pkg == null || !ITranPackageManagerService.Instance().isPreloadPath(p.pkg.getPath()))) {
            return false;
        }
        p.getPkgState().setUpdatedSystemApp(true);
        if (replaced) {
            disabled = new PackageSetting(p);
        } else {
            disabled = p;
        }
        this.mDisabledSysPackages.put(name, disabled);
        SharedUserSetting sharedUserSetting = getSharedUserSettingLPr(disabled);
        if (sharedUserSetting != null) {
            sharedUserSetting.mDisabledPackages.add(disabled);
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageSetting enableSystemPackageLPw(String name) {
        PackageSetting p = this.mDisabledSysPackages.get(name);
        if (p == null) {
            Log.w("PackageManager", "Package " + name + " is not disabled");
            return null;
        }
        SharedUserSetting sharedUserSetting = getSharedUserSettingLPr(p);
        if (sharedUserSetting != null) {
            sharedUserSetting.mDisabledPackages.remove(p);
        }
        p.getPkgState().setUpdatedSystemApp(false);
        PackageSetting ret = addPackageLPw(name, p.getRealName(), p.getPath(), p.getLegacyNativeLibraryPath(), p.getPrimaryCpuAbi(), p.getSecondaryCpuAbi(), p.getCpuAbiOverride(), p.getAppId(), p.getVersionCode(), p.getFlags(), p.getPrivateFlags(), p.getUsesSdkLibraries(), p.getUsesSdkLibrariesVersionsMajor(), p.getUsesStaticLibraries(), p.getUsesStaticLibrariesVersions(), p.getMimeGroups(), this.mDomainVerificationManager.generateNewId());
        if (ret != null) {
            ret.getPkgState().setUpdatedSystemApp(false);
        }
        this.mDisabledSysPackages.remove(name);
        return ret;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDisabledSystemPackageLPr(String name) {
        return this.mDisabledSysPackages.containsKey(name);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeDisabledSystemPackageLPw(String name) {
        SharedUserSetting sharedUserSetting;
        PackageSetting p = this.mDisabledSysPackages.remove(name);
        if (p != null && (sharedUserSetting = getSharedUserSettingLPr(p)) != null) {
            sharedUserSetting.mDisabledPackages.remove(p);
            checkAndPruneSharedUserLPw(sharedUserSetting, false);
        }
    }

    PackageSetting addPackageLPw(String name, String realName, File codePath, String legacyNativeLibraryPathString, String primaryCpuAbiString, String secondaryCpuAbiString, String cpuAbiOverrideString, int uid, long vc, int pkgFlags, int pkgPrivateFlags, String[] usesSdkLibraries, long[] usesSdkLibrariesVersions, String[] usesStaticLibraries, long[] usesStaticLibrariesVersions, Map<String, Set<String>> mimeGroups, UUID domainSetId) {
        PackageSetting p = this.mPackages.get(name);
        if (p != null) {
            if (p.getAppId() == uid) {
                return p;
            }
            PackageManagerService.reportSettingsProblem(6, "Adding duplicate package, keeping first: " + name);
            return null;
        }
        PackageSetting p2 = new PackageSetting(name, realName, codePath, legacyNativeLibraryPathString, primaryCpuAbiString, secondaryCpuAbiString, cpuAbiOverrideString, vc, pkgFlags, pkgPrivateFlags, 0, usesSdkLibraries, usesSdkLibrariesVersions, usesStaticLibraries, usesStaticLibrariesVersions, mimeGroups, domainSetId);
        p2.setAppId(uid);
        if (!this.mAppIds.registerExistingAppId(uid, p2, name)) {
            return null;
        }
        this.mPackages.put(name, p2);
        return p2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SharedUserSetting addSharedUserLPw(String name, int uid, int pkgFlags, int pkgPrivateFlags) {
        SharedUserSetting s = this.mSharedUsers.get(name);
        if (s != null) {
            if (s.mAppId == uid) {
                return s;
            }
            PackageManagerService.reportSettingsProblem(6, "Adding duplicate shared user, keeping first: " + name);
            return null;
        }
        SharedUserSetting s2 = new SharedUserSetting(name, pkgFlags, pkgPrivateFlags);
        s2.mAppId = uid;
        if (!this.mAppIds.registerExistingAppId(uid, s2, name)) {
            return null;
        }
        this.mSharedUsers.put(name, s2);
        return s2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void pruneSharedUsersLPw() {
        List<String> removeKeys = new ArrayList<>();
        List<SharedUserSetting> removeValues = new ArrayList<>();
        for (Map.Entry<String, SharedUserSetting> entry : this.mSharedUsers.entrySet()) {
            SharedUserSetting sus = entry.getValue();
            if (sus == null) {
                removeKeys.add(entry.getKey());
            } else {
                boolean changed = false;
                WatchedArraySet<PackageSetting> sharedUserPackageSettings = sus.getPackageSettings();
                for (int i = sharedUserPackageSettings.size() - 1; i >= 0; i--) {
                    PackageSetting ps = sharedUserPackageSettings.valueAt(i);
                    if (this.mPackages.get(ps.getPackageName()) == null) {
                        sharedUserPackageSettings.removeAt(i);
                        changed = true;
                    }
                }
                WatchedArraySet<PackageSetting> sharedUserDisabledPackageSettings = sus.getDisabledPackageSettings();
                for (int i2 = sharedUserDisabledPackageSettings.size() - 1; i2 >= 0; i2--) {
                    PackageSetting ps2 = sharedUserDisabledPackageSettings.valueAt(i2);
                    if (this.mDisabledSysPackages.get(ps2.getPackageName()) == null) {
                        sharedUserDisabledPackageSettings.removeAt(i2);
                        changed = true;
                    }
                }
                if (changed) {
                    sus.onChanged();
                }
                if (sharedUserPackageSettings.isEmpty() && sharedUserDisabledPackageSettings.isEmpty()) {
                    removeValues.add(sus);
                }
            }
        }
        final WatchedArrayMap<String, SharedUserSetting> watchedArrayMap = this.mSharedUsers;
        Objects.requireNonNull(watchedArrayMap);
        removeKeys.forEach(new Consumer() { // from class: com.android.server.pm.Settings$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                WatchedArrayMap.this.remove((String) obj);
            }
        });
        removeValues.forEach(new Consumer() { // from class: com.android.server.pm.Settings$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                Settings.this.m5609lambda$pruneSharedUsersLPw$0$comandroidserverpmSettings((SharedUserSetting) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$pruneSharedUsersLPw$0$com-android-server-pm-Settings  reason: not valid java name */
    public /* synthetic */ void m5609lambda$pruneSharedUsersLPw$0$comandroidserverpmSettings(SharedUserSetting sus) {
        checkAndPruneSharedUserLPw(sus, true);
    }

    private static boolean shouldInstallInDualprofile(UserHandle installUser, int userId, UserManagerService userManager) {
        if (installUser != null || !userManager.isDualProfile(userId)) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Code restructure failed: missing block: B:30:0x011e, code lost:
        if (r7.preCreated == false) goto L30;
     */
    /* JADX WARN: Code restructure failed: missing block: B:34:0x0125, code lost:
        if (r5 == r7.id) goto L30;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static PackageSetting createNewSetting(String pkgName, PackageSetting originalPkg, PackageSetting disabledPkg, String realPkgName, SharedUserSetting sharedUser, File codePath, String legacyNativeLibraryPath, String primaryCpuAbi, String secondaryCpuAbi, long versionCode, int pkgFlags, int pkgPrivateFlags, UserHandle installUser, boolean allowInstall, boolean instantApp, boolean virtualPreload, UserManagerService userManager, String[] usesSdkLibraries, long[] usesSdkLibrariesVersions, String[] usesStaticLibraries, long[] usesStaticLibrariesVersions, Set<String> mimeGroupNames, UUID domainSetId) {
        UserManagerService userManagerService;
        boolean installed;
        if (originalPkg != null) {
            if (PackageManagerService.DEBUG_UPGRADE) {
                Log.v("PackageManager", "Package " + pkgName + " is adopting original package " + originalPkg.getPackageName());
            }
            PackageSetting pkgSetting = new PackageSetting(originalPkg, pkgName).setPath(codePath).setLegacyNativeLibraryPath(legacyNativeLibraryPath).setPrimaryCpuAbi(primaryCpuAbi).setSecondaryCpuAbi(secondaryCpuAbi).setSignatures(new PackageSignatures()).setLongVersionCode(versionCode).setUsesSdkLibraries(usesSdkLibraries).setUsesSdkLibrariesVersionsMajor(usesSdkLibrariesVersions).setUsesStaticLibraries(usesStaticLibraries).setUsesStaticLibrariesVersions(usesStaticLibrariesVersions).setLastModifiedTime(codePath.lastModified()).setDomainSetId(domainSetId);
            pkgSetting.setPkgFlags(pkgFlags, pkgPrivateFlags);
            return pkgSetting;
        }
        PackageSetting pkgSetting2 = new PackageSetting(pkgName, realPkgName, codePath, legacyNativeLibraryPath, primaryCpuAbi, secondaryCpuAbi, null, versionCode, pkgFlags, pkgPrivateFlags, 0, usesSdkLibraries, usesSdkLibrariesVersions, usesStaticLibraries, usesStaticLibrariesVersions, createMimeGroups(mimeGroupNames), domainSetId);
        pkgSetting2.setLastModifiedTime(codePath.lastModified());
        if (sharedUser != null) {
            pkgSetting2.setSharedUserAppId(sharedUser.mAppId);
        }
        if ((pkgFlags & 1) == 0) {
            List<UserInfo> users = getAllUsers(userManager);
            int installUserId = installUser != null ? installUser.getIdentifier() : 0;
            if (users != null && allowInstall) {
                Iterator<UserInfo> it = users.iterator();
                while (it.hasNext()) {
                    UserInfo user = it.next();
                    if (installUser == null) {
                        userManagerService = userManager;
                    } else if (installUserId != -1) {
                        userManagerService = userManager;
                    } else {
                        userManagerService = userManager;
                        if (!isAdbInstallDisallowed(userManagerService, user.id)) {
                        }
                    }
                    if (shouldInstallInDualprofile(installUser, user.id, userManagerService)) {
                        installed = true;
                        pkgSetting2.setUserState(user.id, 0L, 0, installed, true, true, false, 0, null, instantApp, virtualPreload, null, null, null, 0, 0, null, null, 0L);
                    }
                    installed = false;
                    pkgSetting2.setUserState(user.id, 0L, 0, installed, true, true, false, 0, null, instantApp, virtualPreload, null, null, null, 0, 0, null, null, 0L);
                }
            }
        }
        if (sharedUser != null) {
            pkgSetting2.setAppId(sharedUser.mAppId);
            return pkgSetting2;
        } else if (disabledPkg != null) {
            pkgSetting2.setSignatures(new PackageSignatures(disabledPkg.getSignatures()));
            pkgSetting2.setAppId(disabledPkg.getAppId());
            pkgSetting2.getLegacyPermissionState().copyFrom(disabledPkg.getLegacyPermissionState());
            List<UserInfo> users2 = getAllUsers(userManager);
            if (users2 != null) {
                for (UserInfo user2 : users2) {
                    int userId = user2.id;
                    pkgSetting2.setDisabledComponentsCopy(disabledPkg.getDisabledComponents(userId), userId);
                    pkgSetting2.setEnabledComponentsCopy(disabledPkg.getEnabledComponents(userId), userId);
                }
                return pkgSetting2;
            }
            return pkgSetting2;
        } else {
            return pkgSetting2;
        }
    }

    private static Map<String, Set<String>> createMimeGroups(Set<String> mimeGroupNames) {
        if (mimeGroupNames == null) {
            return null;
        }
        return new KeySetToValueMap(mimeGroupNames, new ArraySet());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void updatePackageSetting(PackageSetting pkgSetting, PackageSetting disabledPkg, SharedUserSetting existingSharedUserSetting, SharedUserSetting sharedUser, File codePath, String legacyNativeLibraryPath, String primaryCpuAbi, String secondaryCpuAbi, int pkgFlags, int pkgPrivateFlags, UserManagerService userManager, String[] usesSdkLibraries, long[] usesSdkLibrariesVersions, String[] usesStaticLibraries, long[] usesStaticLibrariesVersions, Set<String> mimeGroupNames, UUID domainSetId) throws PackageManagerException {
        int pkgPrivateFlags2;
        String pkgName = pkgSetting.getPackageName();
        if (sharedUser == null) {
            pkgSetting.setSharedUserAppId(-1);
        } else if (Objects.equals(existingSharedUserSetting, sharedUser)) {
            pkgSetting.setSharedUserAppId(sharedUser.mAppId);
        } else {
            PackageManagerService.reportSettingsProblem(5, "Package " + pkgName + " shared user changed from " + (existingSharedUserSetting != null ? existingSharedUserSetting.name : "<nothing>") + " to " + sharedUser.name);
            throw new PackageManagerException(-24, "Updating application package " + pkgName + " failed");
        }
        boolean z = true;
        if (!pkgSetting.getPath().equals(codePath)) {
            boolean isSystem = pkgSetting.isSystem();
            Slog.i("PackageManager", "Update" + (isSystem ? " system" : "") + " package " + pkgName + " code path from " + pkgSetting.getPathString() + " to " + codePath.toString() + "; Retain data and using new");
            if (!isSystem) {
                if ((pkgFlags & 1) != 0 && disabledPkg == null) {
                    List<UserInfo> allUserInfos = getAllUsers(userManager);
                    if (allUserInfos != null) {
                        for (UserInfo userInfo : allUserInfos) {
                            pkgSetting.setInstalled(z, userInfo.id);
                            pkgSetting.setUninstallReason(0, userInfo.id);
                            z = true;
                        }
                    }
                }
                pkgSetting.setLegacyNativeLibraryPath(legacyNativeLibraryPath);
            }
            pkgSetting.setPath(codePath);
        }
        pkgSetting.setPrimaryCpuAbi(primaryCpuAbi).setSecondaryCpuAbi(secondaryCpuAbi).updateMimeGroups(mimeGroupNames).setDomainSetId(domainSetId);
        if (usesSdkLibraries != null && usesSdkLibrariesVersions != null && usesSdkLibraries.length == usesSdkLibrariesVersions.length) {
            pkgSetting.setUsesSdkLibraries(usesSdkLibraries).setUsesSdkLibrariesVersionsMajor(usesSdkLibrariesVersions);
        } else {
            pkgSetting.setUsesSdkLibraries(null).setUsesSdkLibrariesVersionsMajor(null);
        }
        if (usesStaticLibraries != null && usesStaticLibrariesVersions != null && usesStaticLibraries.length == usesStaticLibrariesVersions.length) {
            pkgSetting.setUsesStaticLibraries(usesStaticLibraries).setUsesStaticLibrariesVersions(usesStaticLibrariesVersions);
        } else {
            pkgSetting.setUsesStaticLibraries(null).setUsesStaticLibrariesVersions(null);
        }
        int newPkgFlags = pkgSetting.getFlags();
        pkgSetting.setPkgFlags((newPkgFlags & (-2)) | (pkgFlags & 1), pkgSetting.getPrivateFlags());
        boolean wasRequiredForSystemUser = (pkgSetting.getPrivateFlags() & 512) != 0;
        if (wasRequiredForSystemUser) {
            pkgPrivateFlags2 = pkgPrivateFlags | 512;
        } else {
            pkgPrivateFlags2 = pkgPrivateFlags & (-513);
        }
        pkgSetting.setPrivateFlags(pkgPrivateFlags2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean registerAppIdLPw(PackageSetting p, boolean forceNew) throws PackageManagerException {
        boolean createdNew;
        if (p.getAppId() == 0 || forceNew) {
            p.setAppId(this.mAppIds.acquireAndRegisterNewAppId(p));
            createdNew = true;
        } else {
            createdNew = this.mAppIds.registerExistingAppId(p.getAppId(), p, p.getPackageName());
        }
        if (p.getAppId() < 0) {
            PackageManagerService.reportSettingsProblem(5, "Package " + p.getPackageName() + " could not be assigned a valid UID");
            throw new PackageManagerException(-4, "Package " + p.getPackageName() + " could not be assigned a valid UID");
        }
        return createdNew;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeUserRestrictionsLPw(PackageSetting newPackage, PackageSetting oldPackage) {
        List<UserInfo> allUsers;
        PackageUserState oldUserState;
        if (getPackageLPr(newPackage.getPackageName()) == null || (allUsers = getAllUsers(UserManagerService.getInstance())) == null) {
            return;
        }
        for (UserInfo user : allUsers) {
            if (oldPackage == null) {
                oldUserState = PackageUserState.DEFAULT;
            } else {
                oldUserState = oldPackage.readUserState(user.id);
            }
            if (!oldUserState.equals(newPackage.readUserState(user.id))) {
                writePackageRestrictionsLPr(user.id);
            }
        }
    }

    static boolean isAdbInstallDisallowed(UserManagerService userManager, int userId) {
        return userManager.hasUserRestriction("no_debugging_features", userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void insertPackageSettingLPw(PackageSetting p, AndroidPackage pkg) {
        if (p.getSigningDetails().getSignatures() == null) {
            p.setSigningDetails(pkg.getSigningDetails());
        }
        SharedUserSetting sharedUserSetting = getSharedUserSettingLPr(p);
        if (sharedUserSetting != null && sharedUserSetting.signatures.mSigningDetails.getSignatures() == null) {
            sharedUserSetting.signatures.mSigningDetails = pkg.getSigningDetails();
        }
        addPackageSettingLPw(p, sharedUserSetting);
    }

    void addPackageSettingLPw(PackageSetting p, SharedUserSetting sharedUser) {
        this.mPackages.put(p.getPackageName(), p);
        if (sharedUser != null) {
            SharedUserSetting existingSharedUserSetting = getSharedUserSettingLPr(p);
            if (existingSharedUserSetting != null && existingSharedUserSetting != sharedUser) {
                PackageManagerService.reportSettingsProblem(6, "Package " + p.getPackageName() + " was user " + existingSharedUserSetting + " but is now " + sharedUser + "; I am not changing its files so it will probably fail!");
                sharedUser.removePackage(p);
            } else if (p.getAppId() != sharedUser.mAppId) {
                PackageManagerService.reportSettingsProblem(6, "Package " + p.getPackageName() + " was user id " + p.getAppId() + " but is now user " + sharedUser + " with id " + sharedUser.mAppId + "; I am not changing its files so it will probably fail!");
            }
            sharedUser.addPackage(p);
            p.setSharedUserAppId(sharedUser.mAppId);
            p.setAppId(sharedUser.mAppId);
        }
        Object userIdPs = getSettingLPr(p.getAppId());
        if (sharedUser == null) {
            if (userIdPs != null && userIdPs != p) {
                this.mAppIds.replaceSetting(p.getAppId(), p);
            }
        } else if (userIdPs != null && userIdPs != sharedUser) {
            this.mAppIds.replaceSetting(p.getAppId(), sharedUser);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean checkAndPruneSharedUserLPw(SharedUserSetting s, boolean skipCheck) {
        if ((skipCheck || (s.getPackageStates().isEmpty() && s.getDisabledPackageStates().isEmpty())) && this.mSharedUsers.remove(s.name) != null) {
            removeAppIdLPw(s.mAppId);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int removePackageLPw(String name) {
        PackageSetting p = this.mPackages.remove(name);
        if (p != null) {
            removeInstallerPackageStatus(name);
            SharedUserSetting sharedUserSetting = getSharedUserSettingLPr(p);
            if (sharedUserSetting != null) {
                sharedUserSetting.removePackage(p);
                if (checkAndPruneSharedUserLPw(sharedUserSetting, false)) {
                    return sharedUserSetting.mAppId;
                }
                return -1;
            }
            removeAppIdLPw(p.getAppId());
            return p.getAppId();
        }
        return -1;
    }

    private void removeInstallerPackageStatus(String packageName) {
        if (!this.mInstallerPackages.contains(packageName)) {
            return;
        }
        for (int i = 0; i < this.mPackages.size(); i++) {
            this.mPackages.valueAt(i).removeInstallerPackage(packageName);
        }
        this.mInstallerPackages.remove(packageName);
    }

    public SettingBase getSettingLPr(int appId) {
        return this.mAppIds.getSetting(appId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeAppIdLPw(int appId) {
        this.mAppIds.removeSetting(appId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void convertSharedUserSettingsLPw(SharedUserSetting sharedUser) {
        PackageSetting ps = sharedUser.getPackageSettings().valueAt(0);
        this.mAppIds.replaceSetting(sharedUser.getAppId(), ps);
        ps.setSharedUserAppId(-1);
        if (!sharedUser.getDisabledPackageSettings().isEmpty()) {
            PackageSetting disabledPs = sharedUser.getDisabledPackageSettings().valueAt(0);
            disabledPs.setSharedUserAppId(-1);
        }
        this.mSharedUsers.remove(sharedUser.getName());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void checkAndConvertSharedUserSettingsLPw(SharedUserSetting sharedUser) {
        AndroidPackage pkg;
        if (sharedUser.isSingleUser() && (pkg = sharedUser.getPackageSettings().valueAt(0).getPkg()) != null && pkg.isLeavingSharedUid() && SharedUidMigration.applyStrategy(2)) {
            convertSharedUserSettingsLPw(sharedUser);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PreferredIntentResolver editPreferredActivitiesLPw(int userId) {
        PreferredIntentResolver pir = this.mPreferredActivities.get(userId);
        if (pir == null) {
            PreferredIntentResolver pir2 = new PreferredIntentResolver();
            this.mPreferredActivities.put(userId, pir2);
            return pir2;
        }
        return pir;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PersistentPreferredIntentResolver editPersistentPreferredActivitiesLPw(int userId) {
        PersistentPreferredIntentResolver ppir = this.mPersistentPreferredActivities.get(userId);
        if (ppir == null) {
            PersistentPreferredIntentResolver ppir2 = new PersistentPreferredIntentResolver();
            this.mPersistentPreferredActivities.put(userId, ppir2);
            return ppir2;
        }
        return ppir;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public CrossProfileIntentResolver editCrossProfileIntentResolverLPw(int userId) {
        CrossProfileIntentResolver cpir = this.mCrossProfileIntentResolvers.get(userId);
        if (cpir == null) {
            CrossProfileIntentResolver cpir2 = new CrossProfileIntentResolver();
            this.mCrossProfileIntentResolvers.put(userId, cpir2);
            return cpir2;
        }
        return cpir;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String removeDefaultBrowserPackageNameLPw(int userId) {
        if (userId == -1) {
            return null;
        }
        return this.mDefaultBrowserApp.removeReturnOld(userId);
    }

    private File getUserPackagesStateFile(int userId) {
        File userDir = new File(new File(this.mSystemDir, DatabaseHelper.SoundModelContract.KEY_USERS), Integer.toString(userId));
        return new File(userDir, "package-restrictions.xml");
    }

    private File getUserRuntimePermissionsFile(int userId) {
        File userDir = new File(new File(this.mSystemDir, DatabaseHelper.SoundModelContract.KEY_USERS), Integer.toString(userId));
        return new File(userDir, RUNTIME_PERMISSIONS_FILE_NAME);
    }

    private File getUserPackagesStateBackupFile(int userId) {
        return new File(Environment.getUserSystemDirectory(userId), "package-restrictions-backup.xml");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeAllUsersPackageRestrictionsLPr() {
        List<UserInfo> users = getAllUsers(UserManagerService.getInstance());
        if (users == null) {
            return;
        }
        for (UserInfo user : users) {
            writePackageRestrictionsLPr(user.id);
        }
    }

    void writeAllRuntimePermissionsLPr() {
        int[] userIds;
        for (int userId : UserManagerService.getInstance().getUserIds()) {
            this.mRuntimePermissionsPersistence.writeStateForUserAsync(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPermissionUpgradeNeeded(int userId) {
        return this.mRuntimePermissionsPersistence.isPermissionUpgradeNeeded(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateRuntimePermissionsFingerprint(int userId) {
        this.mRuntimePermissionsPersistence.updateRuntimePermissionsFingerprint(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getDefaultRuntimePermissionsVersion(int userId) {
        return this.mRuntimePermissionsPersistence.getVersion(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDefaultRuntimePermissionsVersion(int version, int userId) {
        this.mRuntimePermissionsPersistence.setVersion(version, userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPermissionControllerVersion(long version) {
        this.mRuntimePermissionsPersistence.setPermissionControllerVersion(version);
    }

    public VersionInfo findOrCreateVersion(String volumeUuid) {
        VersionInfo ver = this.mVersion.get(volumeUuid);
        if (ver == null) {
            VersionInfo ver2 = new VersionInfo();
            this.mVersion.put(volumeUuid, ver2);
            return ver2;
        }
        return ver;
    }

    public VersionInfo getInternalVersion() {
        return this.mVersion.get(StorageManager.UUID_PRIVATE_INTERNAL);
    }

    public VersionInfo getExternalVersion() {
        return this.mVersion.get("primary_physical");
    }

    public void onVolumeForgotten(String fsUuid) {
        this.mVersion.remove(fsUuid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void readPreferredActivitiesLPw(TypedXmlPullParser parser, int userId) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals(TAG_ITEM)) {
                            PreferredActivity pa = new PreferredActivity(parser);
                            if (pa.mPref.getParseError() == null) {
                                PreferredIntentResolver resolver = editPreferredActivitiesLPw(userId);
                                if (resolver.shouldAddPreferredActivity(pa)) {
                                    resolver.addFilter((PackageDataSnapshot) null, (PackageDataSnapshot) pa);
                                }
                            } else {
                                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <preferred-activity> " + pa.mPref.getParseError() + " at " + parser.getPositionDescription());
                            }
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Unknown element under <preferred-activities>: " + parser.getName());
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void readPersistentPreferredActivitiesLPw(TypedXmlPullParser parser, int userId) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals(TAG_ITEM)) {
                            PersistentPreferredActivity ppa = new PersistentPreferredActivity(parser);
                            editPersistentPreferredActivitiesLPw(userId).addFilter((PackageDataSnapshot) null, (PackageDataSnapshot) ppa);
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Unknown element under <persistent-preferred-activities>: " + parser.getName());
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void readCrossProfileIntentFiltersLPw(TypedXmlPullParser parser, int userId) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals(TAG_ITEM)) {
                            CrossProfileIntentFilter cpif = new CrossProfileIntentFilter(parser);
                            editCrossProfileIntentResolverLPw(userId).addFilter((PackageDataSnapshot) null, (PackageDataSnapshot) cpif);
                        } else {
                            String msg = "Unknown element under crossProfile-intent-filters: " + tagName;
                            PackageManagerService.reportSettingsProblem(5, msg);
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void readDefaultAppsLPw(XmlPullParser parser, int userId) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals(TAG_DEFAULT_BROWSER)) {
                            String packageName = parser.getAttributeValue(null, "packageName");
                            this.mDefaultBrowserApp.put(userId, packageName);
                        } else if (!tagName.equals(TAG_DEFAULT_DIALER)) {
                            String msg = "Unknown element under default-apps: " + parser.getName();
                            PackageManagerService.reportSettingsProblem(5, msg);
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    void readBlockUninstallPackagesLPw(TypedXmlPullParser parser, int userId) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        ArraySet<String> packages = new ArraySet<>();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            } else if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if (tagName.equals(TAG_BLOCK_UNINSTALL)) {
                    String packageName = parser.getAttributeValue((String) null, "packageName");
                    packages.add(packageName);
                } else {
                    String msg = "Unknown element under block-uninstall-packages: " + parser.getName();
                    PackageManagerService.reportSettingsProblem(5, msg);
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
        if (packages.isEmpty()) {
            this.mBlockUninstallPackages.remove(userId);
        } else {
            this.mBlockUninstallPackages.put(userId, packages);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1984=17, 1991=16] */
    void readPackageRestrictionsLPr(int userId, ArrayMap<String, Long> origFirstInstallTimes) {
        FileInputStream str;
        Settings settings;
        String str2;
        int i;
        String str3;
        String str4;
        String str5;
        String str6;
        String str7;
        File userPackagesStateFile;
        TypedXmlPullParser parser;
        int type;
        char c;
        int i2;
        char c2;
        char c3;
        File userPackagesStateFile2;
        int i3;
        String str8;
        String str9;
        int outerDepth;
        int type2;
        char c4;
        int type3;
        boolean hidden;
        int enabled;
        int packageDepth;
        int packageDepth2;
        ArrayMap<String, SuspendParams> suspendParamsMap;
        String name;
        long longValue;
        int packageDepth3;
        int packageDepth4;
        int type4;
        char c5;
        int type5;
        Settings settings2 = this;
        String str10 = ATTR_SUSPENDING_PACKAGE;
        String str11 = TAG_PACKAGE;
        FileInputStream str12 = null;
        File userPackagesStateFile3 = getUserPackagesStateFile(userId);
        File backupFile = getUserPackagesStateBackupFile(userId);
        int i4 = 4;
        String str13 = "PackageManager";
        if (backupFile.exists()) {
            try {
                str12 = new FileInputStream(backupFile);
                settings2.mReadMessages.append("Reading from backup stopped packages file\n");
                PackageManagerService.reportSettingsProblem(4, "Need to read from backup stopped packages file");
                if (userPackagesStateFile3.exists()) {
                    Slog.w("PackageManager", "Cleaning up stopped packages file " + userPackagesStateFile3);
                    userPackagesStateFile3.delete();
                }
                str = str12;
            } catch (IOException e) {
                str = str12;
            }
        } else {
            str = null;
        }
        String str14 = "Error reading package manager stopped packages";
        int i5 = 6;
        String str15 = "Error reading: ";
        if (str == null) {
            try {
                if (userPackagesStateFile3.exists()) {
                    str5 = "Error reading: ";
                    str6 = "Error reading package manager stopped packages";
                    str7 = "PackageManager";
                    try {
                        userPackagesStateFile = userPackagesStateFile3;
                    } catch (IOException e2) {
                        e = e2;
                        settings = settings2;
                        str4 = str7;
                    } catch (XmlPullParserException e3) {
                        e = e3;
                        settings = settings2;
                        str3 = str6;
                        str2 = str5;
                        str4 = str7;
                        i = 6;
                    }
                    try {
                        str = new FileInputStream(userPackagesStateFile);
                    } catch (IOException e4) {
                        e = e4;
                        settings = settings2;
                        str4 = str7;
                    } catch (XmlPullParserException e5) {
                        e = e5;
                        settings = settings2;
                        str3 = str6;
                        str2 = str5;
                        str4 = str7;
                        i = 6;
                        settings.mReadMessages.append(str2 + e.toString());
                        PackageManagerService.reportSettingsProblem(i, "Error reading stopped packages: " + e);
                        Slog.wtf(str4, str3, e);
                        return;
                    }
                } else {
                    try {
                        settings2.mReadMessages.append("No stopped packages file found\n");
                        PackageManagerService.reportSettingsProblem(4, "No stopped packages file; assuming all started");
                        for (PackageSetting pkg : settings2.mPackages.values()) {
                            str6 = str14;
                            str5 = str15;
                            String str16 = str13;
                            File backupFile2 = backupFile;
                            File userPackagesStateFile4 = userPackagesStateFile3;
                            try {
                                pkg.setUserState(userId, 0L, 0, true, false, false, false, 0, null, false, false, null, null, null, 0, 0, null, null, 0L);
                                str14 = str6;
                                str15 = str5;
                                str13 = str16;
                                backupFile = backupFile2;
                                userPackagesStateFile3 = userPackagesStateFile4;
                                i5 = 6;
                            } catch (IOException e6) {
                                e = e6;
                                settings = settings2;
                                str4 = str16;
                            } catch (XmlPullParserException e7) {
                                e = e7;
                                settings = settings2;
                                str3 = str6;
                                str2 = str5;
                                str4 = str16;
                                i = 6;
                                settings.mReadMessages.append(str2 + e.toString());
                                PackageManagerService.reportSettingsProblem(i, "Error reading stopped packages: " + e);
                                Slog.wtf(str4, str3, e);
                                return;
                            }
                        }
                        return;
                    } catch (IOException e8) {
                        e = e8;
                        str5 = str15;
                        str6 = str14;
                        settings = settings2;
                        str4 = str13;
                    } catch (XmlPullParserException e9) {
                        e = e9;
                        settings = settings2;
                        str2 = str15;
                        i = i5;
                        str3 = str14;
                        str4 = str13;
                    }
                }
            } catch (IOException e10) {
                e = e10;
                str5 = "Error reading: ";
                str6 = "Error reading package manager stopped packages";
                settings = settings2;
                str4 = "PackageManager";
            } catch (XmlPullParserException e11) {
                e = e11;
                settings = settings2;
                str2 = "Error reading: ";
                i = 6;
                str3 = "Error reading package manager stopped packages";
                str4 = "PackageManager";
            }
        } else {
            str5 = "Error reading: ";
            str6 = "Error reading package manager stopped packages";
            str7 = "PackageManager";
            userPackagesStateFile = userPackagesStateFile3;
        }
        try {
            try {
                parser = Xml.resolvePullParser(str);
                do {
                    try {
                        type = parser.next();
                        c = 2;
                        i2 = 1;
                        if (type == 2) {
                            break;
                        }
                    } catch (XmlPullParserException e12) {
                        e = e12;
                        settings = settings2;
                        str4 = str7;
                    }
                } while (type != 1);
                c2 = 5;
            } catch (IOException e13) {
                e = e13;
                settings = settings2;
                str4 = str7;
            }
            if (type != 2) {
                settings2.mReadMessages.append("No start tag found in package restrictions file\n");
                PackageManagerService.reportSettingsProblem(5, "No start tag found in package manager stopped packages");
                return;
            }
            int outerDepth2 = parser.getDepth();
            while (true) {
                int type6 = parser.next();
                if (type6 == i2) {
                    settings = settings2;
                    str4 = str7;
                } else if (type6 == 3 && parser.getDepth() <= outerDepth2) {
                    settings = settings2;
                    str4 = str7;
                } else if (type6 == 3 || type6 == i4) {
                    settings2 = settings2;
                    str7 = str7;
                    userPackagesStateFile = userPackagesStateFile;
                    c = c;
                    outerDepth2 = outerDepth2;
                    c2 = c2;
                    str11 = str11;
                    i4 = i4;
                    i2 = i2;
                    str10 = str10;
                } else {
                    String tagName = parser.getName();
                    if (tagName.equals(str11)) {
                        try {
                            String name2 = parser.getAttributeValue((String) null, "name");
                            PackageSetting ps = settings2.mPackages.get(name2);
                            if (ps == null) {
                                String str17 = str7;
                                try {
                                    Slog.w(str17, "No package known for stopped package " + name2);
                                    XmlUtils.skipCurrentTag(parser);
                                    str7 = str17;
                                    c2 = 5;
                                } catch (IOException e14) {
                                    e = e14;
                                    settings = settings2;
                                    str4 = str17;
                                } catch (XmlPullParserException e15) {
                                    e = e15;
                                    settings = settings2;
                                    str4 = str17;
                                    str3 = str6;
                                    str2 = str5;
                                    i = 6;
                                    settings.mReadMessages.append(str2 + e.toString());
                                    PackageManagerService.reportSettingsProblem(i, "Error reading stopped packages: " + e);
                                    Slog.wtf(str4, str3, e);
                                    return;
                                }
                            } else {
                                String str18 = str7;
                                try {
                                    long ceDataInode = parser.getAttributeLong((String) null, ATTR_CE_DATA_INODE, 0L);
                                    try {
                                        boolean installed = parser.getAttributeBoolean((String) null, ATTR_INSTALLED, true);
                                        userPackagesStateFile2 = userPackagesStateFile;
                                        try {
                                            boolean stopped = parser.getAttributeBoolean((String) null, ATTR_STOPPED, false);
                                            boolean notLaunched = parser.getAttributeBoolean((String) null, ATTR_NOT_LAUNCHED, false);
                                            int isNotifyScreenOn = XmlUtils.readIntAttribute(parser, ATTR_NOTIFY_SCREEN_ON, 0);
                                            try {
                                                ps.setNotifyScreenOn(isNotifyScreenOn, userId);
                                                boolean hidden2 = parser.getAttributeBoolean((String) null, ATTR_HIDDEN, false);
                                                if (hidden2) {
                                                    type3 = type6;
                                                    hidden = hidden2;
                                                } else {
                                                    type3 = type6;
                                                    hidden = parser.getAttributeBoolean((String) null, ATTR_BLOCKED, false);
                                                }
                                                int distractionFlags = parser.getAttributeInt((String) null, ATTR_DISTRACTION_FLAGS, 0);
                                                boolean suspended = parser.getAttributeBoolean((String) null, ATTR_SUSPENDED, false);
                                                String oldSuspendingPackage = parser.getAttributeValue((String) null, str10);
                                                String dialogMessage = parser.getAttributeValue((String) null, ATTR_SUSPEND_DIALOG_MESSAGE);
                                                if (suspended && oldSuspendingPackage == null) {
                                                    oldSuspendingPackage = PackageManagerService.PLATFORM_PACKAGE_NAME;
                                                }
                                                boolean blockUninstall = parser.getAttributeBoolean((String) null, ATTR_BLOCK_UNINSTALL, false);
                                                boolean instantApp = parser.getAttributeBoolean((String) null, ATTR_INSTANT_APP, false);
                                                outerDepth = outerDepth2;
                                                boolean virtualPreload = parser.getAttributeBoolean((String) null, ATTR_VIRTUAL_PRELOAD, false);
                                                int enabled2 = parser.getAttributeInt((String) null, "enabled", 0);
                                                String enabledCaller = parser.getAttributeValue((String) null, ATTR_ENABLED_CALLER);
                                                c4 = 2;
                                                String harmfulAppWarning = parser.getAttributeValue((String) null, ATTR_HARMFUL_APP_WARNING);
                                                int verifState = parser.getAttributeInt((String) null, ATTR_DOMAIN_VERIFICATON_STATE, 0);
                                                int installReason = parser.getAttributeInt((String) null, ATTR_INSTALL_REASON, 0);
                                                int uninstallReason = parser.getAttributeInt((String) null, ATTR_UNINSTALL_REASON, 0);
                                                String splashScreenTheme = parser.getAttributeValue((String) null, ATTR_SPLASH_SCREEN_THEME);
                                                long firstInstallTime = parser.getAttributeLongHex((String) null, ATTR_FIRST_INSTALL_TIME, 0L);
                                                XmlUtils.readBooleanAttribute(parser, ATTR_APP_TRUSTED, false);
                                                SuspendDialogInfo oldSuspendDialogInfo = null;
                                                int type7 = parser.getDepth();
                                                ArraySet<String> enabledComponents = null;
                                                PersistableBundle suspendedAppExtras = null;
                                                PersistableBundle suspendedAppExtras2 = null;
                                                ArrayMap<String, SuspendParams> suspendParamsMap2 = null;
                                                ArraySet<String> disabledComponents = null;
                                                while (true) {
                                                    int type8 = parser.next();
                                                    enabled = enabled2;
                                                    if (type8 != 1) {
                                                        if (type8 == 3) {
                                                            packageDepth3 = type7;
                                                            if (parser.getDepth() <= packageDepth3) {
                                                                packageDepth = packageDepth3;
                                                                packageDepth2 = type8;
                                                            }
                                                        } else {
                                                            packageDepth3 = type7;
                                                        }
                                                        if (type8 == 3) {
                                                            packageDepth4 = packageDepth3;
                                                            type4 = type8;
                                                        } else if (type8 != 4) {
                                                            String name3 = parser.getName();
                                                            switch (name3.hashCode()) {
                                                                case -2027581689:
                                                                    packageDepth4 = packageDepth3;
                                                                    if (name3.equals(TAG_DISABLED_COMPONENTS)) {
                                                                        c5 = 1;
                                                                        break;
                                                                    }
                                                                    c5 = 65535;
                                                                    break;
                                                                case -1963032286:
                                                                    packageDepth4 = packageDepth3;
                                                                    if (name3.equals(TAG_ENABLED_COMPONENTS)) {
                                                                        c5 = 0;
                                                                        break;
                                                                    }
                                                                    c5 = 65535;
                                                                    break;
                                                                case -1592287551:
                                                                    packageDepth4 = packageDepth3;
                                                                    if (name3.equals(TAG_SUSPENDED_APP_EXTRAS)) {
                                                                        c5 = 2;
                                                                        break;
                                                                    }
                                                                    c5 = 65535;
                                                                    break;
                                                                case -1422791362:
                                                                    packageDepth4 = packageDepth3;
                                                                    if (name3.equals(TAG_SUSPENDED_LAUNCHER_EXTRAS)) {
                                                                        c5 = 3;
                                                                        break;
                                                                    }
                                                                    c5 = 65535;
                                                                    break;
                                                                case -858175433:
                                                                    packageDepth4 = packageDepth3;
                                                                    if (name3.equals(TAG_SUSPEND_PARAMS)) {
                                                                        c5 = 5;
                                                                        break;
                                                                    }
                                                                    c5 = 65535;
                                                                    break;
                                                                case 1660896545:
                                                                    packageDepth4 = packageDepth3;
                                                                    if (name3.equals(TAG_SUSPENDED_DIALOG_INFO)) {
                                                                        c5 = 4;
                                                                        break;
                                                                    }
                                                                    c5 = 65535;
                                                                    break;
                                                                default:
                                                                    packageDepth4 = packageDepth3;
                                                                    c5 = 65535;
                                                                    break;
                                                            }
                                                            switch (c5) {
                                                                case 0:
                                                                    type5 = type8;
                                                                    ArraySet<String> enabledComponents2 = settings2.readComponentsLPr(parser);
                                                                    enabledComponents = enabledComponents2;
                                                                    enabled2 = enabled;
                                                                    type7 = packageDepth4;
                                                                    break;
                                                                case 1:
                                                                    type5 = type8;
                                                                    ArraySet<String> disabledComponents2 = settings2.readComponentsLPr(parser);
                                                                    disabledComponents = disabledComponents2;
                                                                    enabled2 = enabled;
                                                                    type7 = packageDepth4;
                                                                    break;
                                                                case 2:
                                                                    type5 = type8;
                                                                    suspendedAppExtras = PersistableBundle.restoreFromXml(parser);
                                                                    enabled2 = enabled;
                                                                    type7 = packageDepth4;
                                                                    break;
                                                                case 3:
                                                                    type5 = type8;
                                                                    PersistableBundle suspendedLauncherExtras = PersistableBundle.restoreFromXml(parser);
                                                                    suspendedAppExtras2 = suspendedLauncherExtras;
                                                                    enabled2 = enabled;
                                                                    type7 = packageDepth4;
                                                                    break;
                                                                case 4:
                                                                    type5 = type8;
                                                                    SuspendDialogInfo oldSuspendDialogInfo2 = SuspendDialogInfo.restoreFromXml(parser);
                                                                    oldSuspendDialogInfo = oldSuspendDialogInfo2;
                                                                    enabled2 = enabled;
                                                                    type7 = packageDepth4;
                                                                    break;
                                                                case 5:
                                                                    String suspendingPackage = parser.getAttributeValue((String) null, str10);
                                                                    if (suspendingPackage == null) {
                                                                        type4 = type8;
                                                                        Slog.wtf(TAG, "No suspendingPackage found inside tag suspend-params");
                                                                        break;
                                                                    } else {
                                                                        type5 = type8;
                                                                        ArrayMap<String, SuspendParams> suspendParamsMap3 = suspendParamsMap2 == null ? new ArrayMap<>() : suspendParamsMap2;
                                                                        suspendParamsMap3.put(suspendingPackage, SuspendParams.restoreFromXml(parser));
                                                                        suspendParamsMap2 = suspendParamsMap3;
                                                                        enabled2 = enabled;
                                                                        type7 = packageDepth4;
                                                                        break;
                                                                    }
                                                                default:
                                                                    type5 = type8;
                                                                    Slog.wtf(TAG, "Unknown tag " + parser.getName() + " under tag " + str11);
                                                                    enabled2 = enabled;
                                                                    type7 = packageDepth4;
                                                                    break;
                                                            }
                                                        } else {
                                                            packageDepth4 = packageDepth3;
                                                            type4 = type8;
                                                        }
                                                        enabled2 = enabled;
                                                        type7 = packageDepth4;
                                                    } else {
                                                        packageDepth = type7;
                                                        packageDepth2 = type8;
                                                    }
                                                }
                                                SuspendDialogInfo oldSuspendDialogInfo3 = (oldSuspendDialogInfo != null || TextUtils.isEmpty(dialogMessage)) ? oldSuspendDialogInfo : new SuspendDialogInfo.Builder().setMessage(dialogMessage).build();
                                                if (suspended && suspendParamsMap2 == null) {
                                                    SuspendParams suspendParams = new SuspendParams(oldSuspendDialogInfo3, suspendedAppExtras, suspendedAppExtras2);
                                                    ArrayMap<String, SuspendParams> suspendParamsMap4 = new ArrayMap<>();
                                                    suspendParamsMap4.put(oldSuspendingPackage, suspendParams);
                                                    suspendParamsMap = suspendParamsMap4;
                                                } else {
                                                    suspendParamsMap = suspendParamsMap2;
                                                }
                                                if (blockUninstall) {
                                                    name = name2;
                                                    settings2.setBlockUninstallLPw(userId, name, true);
                                                } else {
                                                    name = name2;
                                                }
                                                if (firstInstallTime != 0) {
                                                    longValue = firstInstallTime;
                                                } else {
                                                    try {
                                                        longValue = origFirstInstallTimes.getOrDefault(name, 0L).longValue();
                                                    } catch (IOException e16) {
                                                        e = e16;
                                                        settings = this;
                                                        str4 = str18;
                                                        settings.mReadMessages.append(str5 + e.toString());
                                                        PackageManagerService.reportSettingsProblem(6, "Error reading settings: " + e);
                                                        Slog.wtf(str4, str6, e);
                                                        return;
                                                    } catch (XmlPullParserException e17) {
                                                        e = e17;
                                                        settings = this;
                                                        str3 = str6;
                                                        str2 = str5;
                                                        str4 = str18;
                                                        i = 6;
                                                        settings.mReadMessages.append(str2 + e.toString());
                                                        PackageManagerService.reportSettingsProblem(i, "Error reading stopped packages: " + e);
                                                        Slog.wtf(str4, str3, e);
                                                        return;
                                                    }
                                                }
                                                c3 = 5;
                                                str8 = str11;
                                                str9 = str10;
                                                i3 = 4;
                                                TypedXmlPullParser parser2 = parser;
                                                String name4 = name;
                                                type2 = 1;
                                                try {
                                                    ps.setUserState(userId, ceDataInode, enabled, installed, stopped, notLaunched, hidden, distractionFlags, suspendParamsMap, instantApp, virtualPreload, enabledCaller, enabledComponents, disabledComponents, installReason, uninstallReason, harmfulAppWarning, splashScreenTheme, longValue);
                                                    settings = this;
                                                    try {
                                                        try {
                                                            settings.mDomainVerificationManager.setLegacyUserState(name4, userId, verifState);
                                                            str4 = str18;
                                                            parser = parser2;
                                                        } catch (IOException e18) {
                                                            e = e18;
                                                            str4 = str18;
                                                            settings.mReadMessages.append(str5 + e.toString());
                                                            PackageManagerService.reportSettingsProblem(6, "Error reading settings: " + e);
                                                            Slog.wtf(str4, str6, e);
                                                            return;
                                                        }
                                                    } catch (XmlPullParserException e19) {
                                                        e = e19;
                                                        str3 = str6;
                                                        str2 = str5;
                                                        str4 = str18;
                                                        i = 6;
                                                        settings.mReadMessages.append(str2 + e.toString());
                                                        PackageManagerService.reportSettingsProblem(i, "Error reading stopped packages: " + e);
                                                        Slog.wtf(str4, str3, e);
                                                        return;
                                                    }
                                                } catch (IOException e20) {
                                                    e = e20;
                                                    settings = this;
                                                } catch (XmlPullParserException e21) {
                                                    e = e21;
                                                    settings = this;
                                                }
                                            } catch (IOException e22) {
                                                e = e22;
                                                settings = settings2;
                                            } catch (XmlPullParserException e23) {
                                                e = e23;
                                                settings = settings2;
                                            }
                                        } catch (IOException e24) {
                                            e = e24;
                                            settings = settings2;
                                        } catch (XmlPullParserException e25) {
                                            e = e25;
                                            settings = settings2;
                                        }
                                    } catch (IOException e26) {
                                        e = e26;
                                        settings = settings2;
                                        str4 = str18;
                                    } catch (XmlPullParserException e27) {
                                        e = e27;
                                        settings = settings2;
                                        str3 = str6;
                                        str2 = str5;
                                        str4 = str18;
                                        i = 6;
                                        settings.mReadMessages.append(str2 + e.toString());
                                        PackageManagerService.reportSettingsProblem(i, "Error reading stopped packages: " + e);
                                        Slog.wtf(str4, str3, e);
                                        return;
                                    }
                                } catch (IOException e28) {
                                    e = e28;
                                    settings = settings2;
                                    str4 = str18;
                                } catch (XmlPullParserException e29) {
                                    e = e29;
                                    settings = settings2;
                                    str4 = str18;
                                    str3 = str6;
                                    str2 = str5;
                                    i = 6;
                                    settings.mReadMessages.append(str2 + e.toString());
                                    PackageManagerService.reportSettingsProblem(i, "Error reading stopped packages: " + e);
                                    Slog.wtf(str4, str3, e);
                                    return;
                                }
                            }
                        } catch (IOException e30) {
                            e = e30;
                            settings = settings2;
                            str4 = str7;
                        } catch (XmlPullParserException e31) {
                            e = e31;
                            settings = settings2;
                            str3 = str6;
                            str2 = str5;
                            str4 = str7;
                        }
                        settings.mReadMessages.append(str5 + e.toString());
                        PackageManagerService.reportSettingsProblem(6, "Error reading settings: " + e);
                        Slog.wtf(str4, str6, e);
                        return;
                    }
                    TypedXmlPullParser parser3 = parser;
                    c3 = c2;
                    userPackagesStateFile2 = userPackagesStateFile;
                    i3 = i4;
                    str8 = str11;
                    str9 = str10;
                    outerDepth = outerDepth2;
                    type2 = i2;
                    c4 = c;
                    String str19 = str7;
                    settings = settings2;
                    try {
                        if (tagName.equals("preferred-activities")) {
                            parser = parser3;
                            settings.readPreferredActivitiesLPw(parser, userId);
                            str4 = str19;
                        } else {
                            parser = parser3;
                            if (tagName.equals(TAG_PERSISTENT_PREFERRED_ACTIVITIES)) {
                                settings.readPersistentPreferredActivitiesLPw(parser, userId);
                                str4 = str19;
                            } else if (tagName.equals(TAG_CROSS_PROFILE_INTENT_FILTERS)) {
                                settings.readCrossProfileIntentFiltersLPw(parser, userId);
                                str4 = str19;
                            } else if (tagName.equals(TAG_DEFAULT_APPS)) {
                                settings.readDefaultAppsLPw(parser, userId);
                                str4 = str19;
                            } else if (tagName.equals(TAG_BLOCK_UNINSTALL_PACKAGES)) {
                                settings.readBlockUninstallPackagesLPw(parser, userId);
                                str4 = str19;
                            } else {
                                str4 = str19;
                                try {
                                    Slog.w(str4, "Unknown element under <stopped-packages>: " + parser.getName());
                                    XmlUtils.skipCurrentTag(parser);
                                } catch (IOException e32) {
                                    e = e32;
                                } catch (XmlPullParserException e33) {
                                    e = e33;
                                    str3 = str6;
                                    str2 = str5;
                                    i = 6;
                                    settings.mReadMessages.append(str2 + e.toString());
                                    PackageManagerService.reportSettingsProblem(i, "Error reading stopped packages: " + e);
                                    Slog.wtf(str4, str3, e);
                                    return;
                                }
                            }
                        }
                    } catch (XmlPullParserException e34) {
                        e = e34;
                        str4 = str19;
                        str3 = str6;
                        str2 = str5;
                        i = 6;
                        settings.mReadMessages.append(str2 + e.toString());
                        PackageManagerService.reportSettingsProblem(i, "Error reading stopped packages: " + e);
                        Slog.wtf(str4, str3, e);
                        return;
                    }
                    settings2 = settings;
                    str7 = str4;
                    userPackagesStateFile = userPackagesStateFile2;
                    c = c4;
                    outerDepth2 = outerDepth;
                    c2 = c3;
                    str11 = str8;
                    i4 = i3;
                    i2 = type2;
                    str10 = str9;
                }
            }
            str.close();
        } catch (XmlPullParserException e35) {
            e = e35;
            settings = settings2;
            str3 = str6;
            str2 = str5;
            str4 = str7;
            i = 6;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setBlockUninstallLPw(int userId, String packageName, boolean blockUninstall) {
        ArraySet<String> packages = this.mBlockUninstallPackages.get(userId);
        if (blockUninstall) {
            if (packages == null) {
                packages = new ArraySet<>();
                this.mBlockUninstallPackages.put(userId, packages);
            }
            packages.add(packageName);
        } else if (packages != null) {
            packages.remove(packageName);
            if (packages.isEmpty()) {
                this.mBlockUninstallPackages.remove(userId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearBlockUninstallLPw(int userId) {
        this.mBlockUninstallPackages.remove(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getBlockUninstallLPr(int userId, String packageName) {
        ArraySet<String> packages = this.mBlockUninstallPackages.get(userId);
        if (packages == null) {
            return false;
        }
        return packages.contains(packageName);
    }

    private ArraySet<String> readComponentsLPr(TypedXmlPullParser parser) throws IOException, XmlPullParserException {
        String componentName;
        ArraySet<String> components = null;
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            } else if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if (tagName.equals(TAG_ITEM) && (componentName = parser.getAttributeValue((String) null, "name")) != null) {
                    if (components == null) {
                        components = new ArraySet<>();
                    }
                    components.add(componentName);
                }
            }
        }
        return components;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writePreferredActivitiesLPr(TypedXmlSerializer serializer, int userId, boolean full) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag((String) null, "preferred-activities");
        PreferredIntentResolver pir = this.mPreferredActivities.get(userId);
        if (pir != null) {
            for (F pa : pir.filterSet()) {
                serializer.startTag((String) null, TAG_ITEM);
                pa.writeToXml(serializer, full);
                serializer.endTag((String) null, TAG_ITEM);
            }
        }
        serializer.endTag((String) null, "preferred-activities");
    }

    void writePersistentPreferredActivitiesLPr(TypedXmlSerializer serializer, int userId) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag((String) null, TAG_PERSISTENT_PREFERRED_ACTIVITIES);
        PersistentPreferredIntentResolver ppir = this.mPersistentPreferredActivities.get(userId);
        if (ppir != null) {
            for (F ppa : ppir.filterSet()) {
                serializer.startTag((String) null, TAG_ITEM);
                ppa.writeToXml(serializer);
                serializer.endTag((String) null, TAG_ITEM);
            }
        }
        serializer.endTag((String) null, TAG_PERSISTENT_PREFERRED_ACTIVITIES);
    }

    void writeCrossProfileIntentFiltersLPr(TypedXmlSerializer serializer, int userId) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag((String) null, TAG_CROSS_PROFILE_INTENT_FILTERS);
        CrossProfileIntentResolver cpir = this.mCrossProfileIntentResolvers.get(userId);
        if (cpir != null) {
            for (F cpif : cpir.filterSet()) {
                serializer.startTag((String) null, TAG_ITEM);
                cpif.writeToXml(serializer);
                serializer.endTag((String) null, TAG_ITEM);
            }
        }
        serializer.endTag((String) null, TAG_CROSS_PROFILE_INTENT_FILTERS);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeDefaultAppsLPr(XmlSerializer serializer, int userId) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(null, TAG_DEFAULT_APPS);
        String defaultBrowser = this.mDefaultBrowserApp.get(userId);
        if (!TextUtils.isEmpty(defaultBrowser)) {
            serializer.startTag(null, TAG_DEFAULT_BROWSER);
            serializer.attribute(null, "packageName", defaultBrowser);
            serializer.endTag(null, TAG_DEFAULT_BROWSER);
        }
        serializer.endTag(null, TAG_DEFAULT_APPS);
    }

    void writeBlockUninstallPackagesLPr(TypedXmlSerializer serializer, int userId) throws IOException {
        ArraySet<String> packages = this.mBlockUninstallPackages.get(userId);
        if (packages != null) {
            serializer.startTag((String) null, TAG_BLOCK_UNINSTALL_PACKAGES);
            for (int i = 0; i < packages.size(); i++) {
                serializer.startTag((String) null, TAG_BLOCK_UNINSTALL);
                serializer.attribute((String) null, "packageName", packages.valueAt(i));
                serializer.endTag((String) null, TAG_BLOCK_UNINSTALL);
            }
            serializer.endTag((String) null, TAG_BLOCK_UNINSTALL_PACKAGES);
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:25:0x00b4
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    void writePackageRestrictionsLPr(int r27) {
        /*
            r26 = this;
            r1 = r26
            r2 = r27
            java.lang.String r0 = "suspend-params"
            java.lang.String r3 = "disabled-components"
            java.lang.String r4 = "enabled-components"
            java.lang.String r5 = "pkg"
            java.lang.String r6 = "package-restrictions"
            java.lang.String r7 = "name"
            r26.invalidatePackageCache()
            long r8 = android.os.SystemClock.uptimeMillis()
            java.io.File r10 = r26.getUserPackagesStateFile(r27)
            java.io.File r11 = r26.getUserPackagesStateBackupFile(r27)
            java.io.File r12 = new java.io.File
            java.lang.String r13 = r10.getParent()
            r12.<init>(r13)
            r12.mkdirs()
            boolean r12 = r10.exists()
            java.lang.String r13 = "PackageManager"
            if (r12 == 0) goto L51
            boolean r12 = r11.exists()
            if (r12 != 0) goto L49
            boolean r12 = r10.renameTo(r11)
            if (r12 != 0) goto L51
            java.lang.String r0 = "Unable to backup user packages state file, current changes will be lost at reboot"
            android.util.Slog.wtf(r13, r0)
            return
        L49:
            r10.delete()
            java.lang.String r12 = "Preserving older stopped packages backup"
            android.util.Slog.w(r13, r12)
        L51:
            java.io.FileOutputStream r12 = new java.io.FileOutputStream     // Catch: java.io.IOException -> L2bf
            r12.<init>(r10)     // Catch: java.io.IOException -> L2bf
            android.util.TypedXmlSerializer r14 = android.util.Xml.resolveSerializer(r12)     // Catch: java.io.IOException -> L2bf
            r15 = 1
            r16 = r13
            java.lang.Boolean r13 = java.lang.Boolean.valueOf(r15)     // Catch: java.io.IOException -> L2bb
            r15 = 0
            r14.startDocument(r15, r13)     // Catch: java.io.IOException -> L2bb
            java.lang.String r13 = "http://xmlpull.org/v1/doc/features.html#indent-output"
            r15 = 1
            r14.setFeature(r13, r15)     // Catch: java.io.IOException -> L2bb
            r13 = 0
            r14.startTag(r13, r6)     // Catch: java.io.IOException -> L2bb
            com.android.server.utils.WatchedArrayMap<java.lang.String, com.android.server.pm.PackageSetting> r13 = r1.mPackages     // Catch: java.io.IOException -> L2bb
            java.util.Collection r13 = r13.values()     // Catch: java.io.IOException -> L2bb
            java.util.Iterator r13 = r13.iterator()     // Catch: java.io.IOException -> L2bb
        L79:
            boolean r15 = r13.hasNext()     // Catch: java.io.IOException -> L2bb
            if (r15 == 0) goto L26b
            java.lang.Object r15 = r13.next()     // Catch: java.io.IOException -> L2bb
            com.android.server.pm.PackageSetting r15 = (com.android.server.pm.PackageSetting) r15     // Catch: java.io.IOException -> L2bb
            com.android.server.pm.pkg.PackageUserStateInternal r18 = r15.readUserState(r2)     // Catch: java.io.IOException -> L2bb
            r19 = r13
            r13 = 0
            r14.startTag(r13, r5)     // Catch: java.io.IOException -> L2bb
            java.lang.String r13 = r15.getPackageName()     // Catch: java.io.IOException -> L2bb
            r20 = r15
            r15 = 0
            r14.attribute(r15, r7, r13)     // Catch: java.io.IOException -> L2bb
            long r21 = r18.getCeDataInode()     // Catch: java.io.IOException -> L2bb
            r23 = 0
            int r13 = (r21 > r23 ? 1 : (r21 == r23 ? 0 : -1))
            if (r13 == 0) goto Lba
            java.lang.String r13 = "ceDataInode"
            r21 = r8
            long r8 = r18.getCeDataInode()     // Catch: java.io.IOException -> Lb0
            r15 = 0
            r14.attributeLong(r15, r13, r8)     // Catch: java.io.IOException -> Lb0
            goto Lbc
        Lb0:
            r0 = move-exception
            r13 = r10
            goto L2c5
        Lb4:
            r0 = move-exception
            r21 = r8
            r13 = r10
            goto L2c5
        Lba:
            r21 = r8
        Lbc:
            boolean r8 = r18.isInstalled()     // Catch: java.io.IOException -> L268
            if (r8 != 0) goto Lc9
            java.lang.String r8 = "inst"
            r9 = 0
            r13 = 0
            r14.attributeBoolean(r13, r8, r9)     // Catch: java.io.IOException -> Lb0
        Lc9:
            boolean r8 = r18.isStopped()     // Catch: java.io.IOException -> L268
            if (r8 == 0) goto Ld7
            java.lang.String r8 = "stopped"
            r9 = 1
            r13 = 0
            r14.attributeBoolean(r13, r8, r9)     // Catch: java.io.IOException -> Lb0
        Ld7:
            boolean r8 = r18.isNotLaunched()     // Catch: java.io.IOException -> L268
            if (r8 == 0) goto Le5
            java.lang.String r8 = "nl"
            r9 = 1
            r13 = 0
            r14.attributeBoolean(r13, r8, r9)     // Catch: java.io.IOException -> Lb0
        Le5:
            java.lang.String r8 = "notify-screen-on"
            int r9 = r18.isNotifyScreenOn()     // Catch: java.io.IOException -> L268
            java.lang.String r9 = java.lang.Integer.toString(r9)     // Catch: java.io.IOException -> L268
            r13 = 0
            r14.attribute(r13, r8, r9)     // Catch: java.io.IOException -> L268
            boolean r8 = r18.isHidden()     // Catch: java.io.IOException -> L268
            if (r8 == 0) goto L100
            java.lang.String r8 = "hidden"
            r9 = 1
            r14.attributeBoolean(r13, r8, r9)     // Catch: java.io.IOException -> Lb0
        L100:
            int r8 = r18.getDistractionFlags()     // Catch: java.io.IOException -> L268
            if (r8 == 0) goto L110
            java.lang.String r8 = "distraction_flags"
            int r9 = r18.getDistractionFlags()     // Catch: java.io.IOException -> Lb0
            r13 = 0
            r14.attributeInt(r13, r8, r9)     // Catch: java.io.IOException -> Lb0
        L110:
            boolean r8 = r18.isSuspended()     // Catch: java.io.IOException -> L268
            if (r8 == 0) goto L11e
            java.lang.String r8 = "suspended"
            r9 = 1
            r13 = 0
            r14.attributeBoolean(r13, r8, r9)     // Catch: java.io.IOException -> Lb0
        L11e:
            boolean r8 = r18.isInstantApp()     // Catch: java.io.IOException -> L268
            if (r8 == 0) goto L12b
            java.lang.String r8 = "instant-app"
            r9 = 1
            r13 = 0
            r14.attributeBoolean(r13, r8, r9)     // Catch: java.io.IOException -> Lb0
        L12b:
            boolean r8 = r18.isVirtualPreload()     // Catch: java.io.IOException -> L268
            if (r8 == 0) goto L139
            java.lang.String r8 = "virtual-preload"
            r9 = 1
            r13 = 0
            r14.attributeBoolean(r13, r8, r9)     // Catch: java.io.IOException -> Lb0
        L139:
            int r8 = r18.getEnabledState()     // Catch: java.io.IOException -> L268
            if (r8 == 0) goto L159
            java.lang.String r8 = "enabled"
            int r9 = r18.getEnabledState()     // Catch: java.io.IOException -> Lb0
            r13 = 0
            r14.attributeInt(r13, r8, r9)     // Catch: java.io.IOException -> Lb0
            java.lang.String r8 = r18.getLastDisableAppCaller()     // Catch: java.io.IOException -> Lb0
            if (r8 == 0) goto L159
            java.lang.String r8 = "enabledCaller"
            java.lang.String r9 = r18.getLastDisableAppCaller()     // Catch: java.io.IOException -> Lb0
            r13 = 0
            r14.attribute(r13, r8, r9)     // Catch: java.io.IOException -> Lb0
        L159:
            int r8 = r18.getInstallReason()     // Catch: java.io.IOException -> L268
            if (r8 == 0) goto L169
            java.lang.String r8 = "install-reason"
            int r9 = r18.getInstallReason()     // Catch: java.io.IOException -> Lb0
            r13 = 0
            r14.attributeInt(r13, r8, r9)     // Catch: java.io.IOException -> Lb0
        L169:
            java.lang.String r8 = "first-install-time"
            r13 = r10
            long r9 = r18.getFirstInstallTime()     // Catch: java.io.IOException -> L2b9
            r15 = 0
            r14.attributeLongHex(r15, r8, r9)     // Catch: java.io.IOException -> L2b9
            int r8 = r18.getUninstallReason()     // Catch: java.io.IOException -> L2b9
            if (r8 == 0) goto L185
            java.lang.String r8 = "uninstall-reason"
            int r9 = r18.getUninstallReason()     // Catch: java.io.IOException -> L2b9
            r10 = 0
            r14.attributeInt(r10, r8, r9)     // Catch: java.io.IOException -> L2b9
        L185:
            java.lang.String r8 = r18.getHarmfulAppWarning()     // Catch: java.io.IOException -> L2b9
            if (r8 == 0) goto L195
            java.lang.String r8 = "harmful-app-warning"
            java.lang.String r9 = r18.getHarmfulAppWarning()     // Catch: java.io.IOException -> L2b9
            r10 = 0
            r14.attribute(r10, r8, r9)     // Catch: java.io.IOException -> L2b9
        L195:
            java.lang.String r8 = r18.getSplashScreenTheme()     // Catch: java.io.IOException -> L2b9
            if (r8 == 0) goto L1a6
            java.lang.String r8 = "splash-screen-theme"
            java.lang.String r9 = r18.getSplashScreenTheme()     // Catch: java.io.IOException -> L2b9
            r10 = 0
            r14.attribute(r10, r8, r9)     // Catch: java.io.IOException -> L2b9
        L1a6:
            boolean r8 = r18.isSuspended()     // Catch: java.io.IOException -> L2b9
            if (r8 == 0) goto L1e3
            r8 = 0
        L1ad:
            com.android.server.utils.WatchedArrayMap r9 = r18.getSuspendParams()     // Catch: java.io.IOException -> L2b9
            int r9 = r9.size()     // Catch: java.io.IOException -> L2b9
            if (r8 >= r9) goto L1e3
            com.android.server.utils.WatchedArrayMap r9 = r18.getSuspendParams()     // Catch: java.io.IOException -> L2b9
            java.lang.Object r9 = r9.keyAt(r8)     // Catch: java.io.IOException -> L2b9
            java.lang.String r9 = (java.lang.String) r9     // Catch: java.io.IOException -> L2b9
            r10 = 0
            r14.startTag(r10, r0)     // Catch: java.io.IOException -> L2b9
            java.lang.String r15 = "suspending-package"
            r14.attribute(r10, r15, r9)     // Catch: java.io.IOException -> L2b9
            com.android.server.utils.WatchedArrayMap r10 = r18.getSuspendParams()     // Catch: java.io.IOException -> L2b9
            java.lang.Object r10 = r10.valueAt(r8)     // Catch: java.io.IOException -> L2b9
            com.android.server.pm.pkg.SuspendParams r10 = (com.android.server.pm.pkg.SuspendParams) r10     // Catch: java.io.IOException -> L2b9
            if (r10 == 0) goto L1db
            r10.saveToXml(r14)     // Catch: java.io.IOException -> L2b9
        L1db:
            r15 = 0
            r14.endTag(r15, r0)     // Catch: java.io.IOException -> L2b9
            int r8 = r8 + 1
            goto L1ad
        L1e3:
            android.util.ArraySet r8 = r18.getEnabledComponents()     // Catch: java.io.IOException -> L2b9
            java.lang.String r9 = "item"
            if (r8 == 0) goto L21e
            int r10 = r8.size()     // Catch: java.io.IOException -> L2b9
            if (r10 <= 0) goto L21e
            r10 = 0
            r14.startTag(r10, r4)     // Catch: java.io.IOException -> L2b9
            r10 = 0
        L1f6:
            int r15 = r8.size()     // Catch: java.io.IOException -> L2b9
            if (r10 >= r15) goto L217
            r15 = 0
            r14.startTag(r15, r9)     // Catch: java.io.IOException -> L2b9
            java.lang.Object r17 = r8.valueAt(r10)     // Catch: java.io.IOException -> L2b9
            r15 = r17
            java.lang.String r15 = (java.lang.String) r15     // Catch: java.io.IOException -> L2b9
            r24 = r0
            r0 = 0
            r14.attribute(r0, r7, r15)     // Catch: java.io.IOException -> L2b9
            r14.endTag(r0, r9)     // Catch: java.io.IOException -> L2b9
            int r10 = r10 + 1
            r0 = r24
            goto L1f6
        L217:
            r24 = r0
            r0 = 0
            r14.endTag(r0, r4)     // Catch: java.io.IOException -> L2b9
            goto L220
        L21e:
            r24 = r0
        L220:
            android.util.ArraySet r0 = r18.getDisabledComponents()     // Catch: java.io.IOException -> L2b9
            if (r0 == 0) goto L259
            int r10 = r0.size()     // Catch: java.io.IOException -> L2b9
            if (r10 <= 0) goto L259
            r10 = 0
            r14.startTag(r10, r3)     // Catch: java.io.IOException -> L2b9
            r10 = 0
        L231:
            int r15 = r0.size()     // Catch: java.io.IOException -> L2b9
            if (r10 >= r15) goto L252
            r15 = 0
            r14.startTag(r15, r9)     // Catch: java.io.IOException -> L2b9
            java.lang.Object r17 = r0.valueAt(r10)     // Catch: java.io.IOException -> L2b9
            r15 = r17
            java.lang.String r15 = (java.lang.String) r15     // Catch: java.io.IOException -> L2b9
            r25 = r0
            r0 = 0
            r14.attribute(r0, r7, r15)     // Catch: java.io.IOException -> L2b9
            r14.endTag(r0, r9)     // Catch: java.io.IOException -> L2b9
            int r10 = r10 + 1
            r0 = r25
            goto L231
        L252:
            r25 = r0
            r0 = 0
            r14.endTag(r0, r3)     // Catch: java.io.IOException -> L2b9
            goto L25b
        L259:
            r25 = r0
        L25b:
            r0 = 0
            r14.endTag(r0, r5)     // Catch: java.io.IOException -> L2b9
            r10 = r13
            r13 = r19
            r8 = r21
            r0 = r24
            goto L79
        L268:
            r0 = move-exception
            r13 = r10
            goto L2c5
        L26b:
            r21 = r8
            r13 = r10
            r0 = 1
            r1.writePreferredActivitiesLPr(r14, r2, r0)     // Catch: java.io.IOException -> L2b9
            r1.writePersistentPreferredActivitiesLPr(r14, r2)     // Catch: java.io.IOException -> L2b9
            r1.writeCrossProfileIntentFiltersLPr(r14, r2)     // Catch: java.io.IOException -> L2b9
            r1.writeDefaultAppsLPr(r14, r2)     // Catch: java.io.IOException -> L2b9
            r1.writeBlockUninstallPackagesLPr(r14, r2)     // Catch: java.io.IOException -> L2b9
            r0 = 0
            r14.endTag(r0, r6)     // Catch: java.io.IOException -> L2b9
            r14.endDocument()     // Catch: java.io.IOException -> L2b9
            r12.flush()     // Catch: java.io.IOException -> L2b9
            android.os.FileUtils.sync(r12)     // Catch: java.io.IOException -> L2b9
            r12.close()     // Catch: java.io.IOException -> L2b9
            r11.delete()     // Catch: java.io.IOException -> L2b9
            java.lang.String r0 = r13.toString()     // Catch: java.io.IOException -> L2b9
            r3 = 432(0x1b0, float:6.05E-43)
            r4 = -1
            android.os.FileUtils.setPermissions(r0, r3, r4, r4)     // Catch: java.io.IOException -> L2b9
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch: java.io.IOException -> L2b9
            r0.<init>()     // Catch: java.io.IOException -> L2b9
            java.lang.String r3 = "package-user-"
            java.lang.StringBuilder r0 = r0.append(r3)     // Catch: java.io.IOException -> L2b9
            java.lang.StringBuilder r0 = r0.append(r2)     // Catch: java.io.IOException -> L2b9
            java.lang.String r0 = r0.toString()     // Catch: java.io.IOException -> L2b9
            long r3 = android.os.SystemClock.uptimeMillis()     // Catch: java.io.IOException -> L2b9
            long r3 = r3 - r21
            com.android.internal.logging.EventLogTags.writeCommitSysConfigFile(r0, r3)     // Catch: java.io.IOException -> L2b9
            return
        L2b9:
            r0 = move-exception
            goto L2c5
        L2bb:
            r0 = move-exception
            r21 = r8
            goto L2c4
        L2bf:
            r0 = move-exception
            r21 = r8
            r16 = r13
        L2c4:
            r13 = r10
        L2c5:
            java.lang.String r3 = "Unable to write package manager user packages state,  current changes will be lost at reboot"
            r4 = r16
            android.util.Slog.wtf(r4, r3, r0)
            boolean r0 = r13.exists()
            if (r0 == 0) goto L2f0
            boolean r0 = r13.delete()
            if (r0 != 0) goto L2f0
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r3 = "Failed to clean up mangled file: "
            java.lang.StringBuilder r0 = r0.append(r3)
            java.io.File r3 = r1.mStoppedPackagesFilename
            java.lang.StringBuilder r0 = r0.append(r3)
            java.lang.String r0 = r0.toString()
            android.util.Log.i(r4, r0)
        L2f0:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.Settings.writePackageRestrictionsLPr(int):void");
    }

    void readInstallPermissionsLPr(TypedXmlPullParser parser, LegacyPermissionState permissionsState, List<UserInfo> users) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals(TAG_ITEM)) {
                            String name = parser.getAttributeValue((String) null, "name");
                            boolean granted = parser.getAttributeBoolean((String) null, ATTR_GRANTED, true);
                            int flags = parser.getAttributeIntHex((String) null, ATTR_FLAGS, 0);
                            for (UserInfo user : users) {
                                permissionsState.putPermissionState(new LegacyPermissionState.PermissionState(name, false, granted, flags), user.id);
                            }
                        } else {
                            Slog.w("PackageManager", "Unknown element under <permissions>: " + parser.getName());
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    void readUsesSdkLibLPw(TypedXmlPullParser parser, PackageSetting outPs) throws IOException, XmlPullParserException {
        String libName = parser.getAttributeValue((String) null, "name");
        long libVersion = parser.getAttributeLong((String) null, "version", -1L);
        if (libName != null && libVersion >= 0) {
            outPs.setUsesSdkLibraries((String[]) ArrayUtils.appendElement(String.class, outPs.getUsesSdkLibraries(), libName));
            outPs.setUsesSdkLibrariesVersionsMajor(ArrayUtils.appendLong(outPs.getUsesSdkLibrariesVersionsMajor(), libVersion));
        }
        XmlUtils.skipCurrentTag(parser);
    }

    void readUsesStaticLibLPw(TypedXmlPullParser parser, PackageSetting outPs) throws IOException, XmlPullParserException {
        String libName = parser.getAttributeValue((String) null, "name");
        long libVersion = parser.getAttributeLong((String) null, "version", -1L);
        if (libName != null && libVersion >= 0) {
            outPs.setUsesStaticLibraries((String[]) ArrayUtils.appendElement(String.class, outPs.getUsesStaticLibraries(), libName));
            outPs.setUsesStaticLibrariesVersions(ArrayUtils.appendLong(outPs.getUsesStaticLibrariesVersions(), libVersion));
        }
        XmlUtils.skipCurrentTag(parser);
    }

    void writeUsesSdkLibLPw(TypedXmlSerializer serializer, String[] usesSdkLibraries, long[] usesSdkLibraryVersions) throws IOException {
        if (ArrayUtils.isEmpty(usesSdkLibraries) || ArrayUtils.isEmpty(usesSdkLibraryVersions) || usesSdkLibraries.length != usesSdkLibraryVersions.length) {
            return;
        }
        int libCount = usesSdkLibraries.length;
        for (int i = 0; i < libCount; i++) {
            String libName = usesSdkLibraries[i];
            long libVersion = usesSdkLibraryVersions[i];
            serializer.startTag((String) null, TAG_USES_SDK_LIB);
            serializer.attribute((String) null, "name", libName);
            serializer.attributeLong((String) null, "version", libVersion);
            serializer.endTag((String) null, TAG_USES_SDK_LIB);
        }
    }

    void writeUsesStaticLibLPw(TypedXmlSerializer serializer, String[] usesStaticLibraries, long[] usesStaticLibraryVersions) throws IOException {
        if (ArrayUtils.isEmpty(usesStaticLibraries) || ArrayUtils.isEmpty(usesStaticLibraryVersions) || usesStaticLibraries.length != usesStaticLibraryVersions.length) {
            return;
        }
        int libCount = usesStaticLibraries.length;
        for (int i = 0; i < libCount; i++) {
            String libName = usesStaticLibraries[i];
            long libVersion = usesStaticLibraryVersions[i];
            serializer.startTag((String) null, TAG_USES_STATIC_LIB);
            serializer.attribute((String) null, "name", libName);
            serializer.attributeLong((String) null, "version", libVersion);
            serializer.endTag((String) null, TAG_USES_STATIC_LIB);
        }
    }

    void readStoppedLPw() {
        int type;
        FileInputStream str = null;
        int i = 4;
        if (this.mBackupStoppedPackagesFilename.exists()) {
            try {
                str = new FileInputStream(this.mBackupStoppedPackagesFilename);
                this.mReadMessages.append("Reading from backup stopped packages file\n");
                PackageManagerService.reportSettingsProblem(4, "Need to read from backup stopped packages file");
                if (this.mSettingsFilename.exists()) {
                    Slog.w("PackageManager", "Cleaning up stopped packages file " + this.mStoppedPackagesFilename);
                    this.mStoppedPackagesFilename.delete();
                }
            } catch (IOException e) {
            }
        }
        if (str == null) {
            try {
                if (!this.mStoppedPackagesFilename.exists()) {
                    this.mReadMessages.append("No stopped packages file found\n");
                    PackageManagerService.reportSettingsProblem(4, "No stopped packages file file; assuming all started");
                    for (PackageSetting pkg : this.mPackages.values()) {
                        pkg.setStopped(false, 0);
                        pkg.setNotLaunched(false, 0);
                    }
                    return;
                }
                str = new FileInputStream(this.mStoppedPackagesFilename);
            } catch (IOException e2) {
                this.mReadMessages.append("Error reading: " + e2.toString());
                PackageManagerService.reportSettingsProblem(6, "Error reading settings: " + e2);
                Slog.wtf("PackageManager", "Error reading package manager stopped packages", e2);
                return;
            } catch (XmlPullParserException e3) {
                this.mReadMessages.append("Error reading: " + e3.toString());
                PackageManagerService.reportSettingsProblem(6, "Error reading stopped packages: " + e3);
                Slog.wtf("PackageManager", "Error reading package manager stopped packages", e3);
                return;
            }
        }
        TypedXmlPullParser parser = Xml.resolvePullParser(str);
        while (true) {
            type = parser.next();
            if (type == 2 || type == 1) {
                break;
            }
        }
        if (type != 2) {
            this.mReadMessages.append("No start tag found in stopped packages file\n");
            PackageManagerService.reportSettingsProblem(5, "No start tag found in package manager stopped packages");
            return;
        }
        int outerDepth = parser.getDepth();
        while (true) {
            int type2 = parser.next();
            if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                break;
            }
            if (type2 != 3 && type2 != i) {
                String tagName = parser.getName();
                if (tagName.equals(TAG_PACKAGE)) {
                    String name = parser.getAttributeValue((String) null, "name");
                    PackageSetting ps = this.mPackages.get(name);
                    if (ps != null) {
                        ps.setStopped(true, 0);
                        if ("1".equals(parser.getAttributeValue((String) null, ATTR_NOT_LAUNCHED))) {
                            ps.setNotLaunched(true, 0);
                        }
                    } else {
                        Slog.w("PackageManager", "No package known for stopped package " + name);
                    }
                    XmlUtils.skipCurrentTag(parser);
                } else {
                    Slog.w("PackageManager", "Unknown element under <stopped-packages>: " + parser.getName());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
            i = 4;
        }
        str.close();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:65:0x022b  */
    /* JADX WARN: Removed duplicated region for block: B:84:? A[ADDED_TO_REGION, RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void writeLPr(Computer computer) {
        String str;
        FileOutputStream fstr;
        TypedXmlSerializer serializer;
        long startTime;
        String volumeUuid;
        long startTime2 = SystemClock.uptimeMillis();
        invalidatePackageCache();
        VerifyExt filesVerify = null;
        String str2 = "PackageManager";
        if (this.mSettingsFilename.exists()) {
            if (!this.mBackupSettingsFilename.exists()) {
                if (!this.mSettingsFilename.renameTo(this.mBackupSettingsFilename)) {
                    Slog.wtf("PackageManager", "Unable to backup package manager settings,  current changes will be lost at reboot");
                    return;
                }
            } else {
                this.mSettingsFilename.delete();
                Slog.w("PackageManager", "Preserving older settings backup");
            }
        }
        if (this.mXmlSupport) {
            filesVerify = VerifyFacotry.getInstance().getVerifyExt();
            filesVerify.setBackupName(this.mBackupSettingsFilename);
        }
        this.mPastSignatures.clear();
        try {
            fstr = new FileOutputStream(this.mSettingsFilename);
            serializer = Xml.resolveSerializer(fstr);
            serializer.startDocument((String) null, true);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startTag((String) null, "packages");
            int i = 0;
            while (i < this.mVersion.size()) {
                try {
                    volumeUuid = this.mVersion.keyAt(i);
                    str = str2;
                } catch (IOException e) {
                    e = e;
                    str = str2;
                }
                try {
                    VersionInfo ver = this.mVersion.valueAt(i);
                    long startTime3 = startTime2;
                    try {
                        serializer.startTag((String) null, "version");
                        XmlUtils.writeStringAttribute(serializer, ATTR_VOLUME_UUID, volumeUuid);
                        serializer.attributeInt((String) null, ATTR_SDK_VERSION, ver.sdkVersion);
                        serializer.attributeInt((String) null, ATTR_DATABASE_VERSION, ver.databaseVersion);
                        XmlUtils.writeStringAttribute(serializer, ATTR_FINGERPRINT, ver.fingerprint);
                        serializer.endTag((String) null, "version");
                        i++;
                        str2 = str;
                        startTime2 = startTime3;
                    } catch (IOException e2) {
                        e = e2;
                        String str3 = str;
                        Slog.wtf(str3, "Unable to write package manager settings, current changes will be lost at reboot", e);
                        if (this.mSettingsFilename.exists() && !this.mSettingsFilename.delete()) {
                            Slog.wtf(str3, "Failed to clean up mangled file: " + this.mSettingsFilename);
                            return;
                        }
                    }
                } catch (IOException e3) {
                    e = e3;
                    String str32 = str;
                    Slog.wtf(str32, "Unable to write package manager settings, current changes will be lost at reboot", e);
                    if (this.mSettingsFilename.exists()) {
                    }
                }
            }
            startTime = startTime2;
            str = str2;
            if (this.mVerifierDeviceIdentity != null) {
                serializer.startTag((String) null, "verifier");
                serializer.attribute((String) null, "device", this.mVerifierDeviceIdentity.toString());
                serializer.endTag((String) null, "verifier");
            }
            serializer.startTag((String) null, "permission-trees");
            this.mPermissions.writePermissionTrees(serializer);
            serializer.endTag((String) null, "permission-trees");
            serializer.startTag((String) null, "permissions");
            this.mPermissions.writePermissions(serializer);
            serializer.endTag((String) null, "permissions");
            for (PackageSetting pkg : this.mPackages.values()) {
                writePackageLPr(serializer, pkg);
            }
            for (PackageSetting pkg2 : this.mDisabledSysPackages.values()) {
                writeDisabledSysPackageLPr(serializer, pkg2);
            }
            for (SharedUserSetting usr : this.mSharedUsers.values()) {
                serializer.startTag((String) null, TAG_SHARED_USER);
                serializer.attribute((String) null, "name", usr.name);
                serializer.attributeInt((String) null, "userId", usr.mAppId);
                usr.signatures.writeXml(serializer, "sigs", this.mPastSignatures.untrackedStorage());
                serializer.endTag((String) null, TAG_SHARED_USER);
            }
            if (this.mRenamedPackages.size() > 0) {
                for (Map.Entry<String, String> e4 : this.mRenamedPackages.entrySet()) {
                    serializer.startTag((String) null, "renamed-package");
                    serializer.attribute((String) null, "new", e4.getKey());
                    serializer.attribute((String) null, "old", e4.getValue());
                    serializer.endTag((String) null, "renamed-package");
                }
            }
        } catch (IOException e5) {
            e = e5;
            str = str2;
        }
        try {
            this.mDomainVerificationManager.writeSettings(computer, serializer, false, -1);
            this.mKeySetManagerService.writeKeySetManagerServiceLPr(serializer);
            serializer.endTag((String) null, "packages");
            serializer.endDocument();
            fstr.flush();
            FileUtils.sync(fstr);
            fstr.close();
            if (this.mXmlSupport && filesVerify != null) {
                filesVerify.PackagesVerifyFileBeforeFinishW(this.mSettingsFilename);
            }
            this.mBackupSettingsFilename.delete();
            FileUtils.setPermissions(this.mSettingsFilename.toString(), FrameworkStatsLog.HOTWORD_DETECTION_SERVICE_RESTARTED, -1, -1);
            writeKernelMappingLPr();
            writePackageListLPr();
            writeAllUsersPackageRestrictionsLPr();
            writeAllRuntimePermissionsLPr();
            EventLogTags.writeCommitSysConfigFile("package", SystemClock.uptimeMillis() - startTime);
        } catch (IOException e6) {
            e = e6;
            String str322 = str;
            Slog.wtf(str322, "Unable to write package manager settings, current changes will be lost at reboot", e);
            if (this.mSettingsFilename.exists()) {
            }
        }
    }

    private void writeKernelRemoveUserLPr(int userId) {
        if (this.mKernelMappingFilename == null) {
            return;
        }
        File removeUserIdFile = new File(this.mKernelMappingFilename, "remove_userid");
        writeIntToFile(removeUserIdFile, userId);
    }

    void writeKernelMappingLPr() {
        File file = this.mKernelMappingFilename;
        if (file == null) {
            return;
        }
        String[] known = file.list();
        ArraySet<String> knownSet = new ArraySet<>(known.length);
        for (String name : known) {
            knownSet.add(name);
        }
        for (PackageSetting ps : this.mPackages.values()) {
            knownSet.remove(ps.getPackageName());
            writeKernelMappingLPr(ps);
        }
        for (int i = 0; i < knownSet.size(); i++) {
            String name2 = knownSet.valueAt(i);
            this.mKernelMapping.remove(name2);
            new File(this.mKernelMappingFilename, name2).delete();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeKernelMappingLPr(PackageSetting ps) {
        if (this.mKernelMappingFilename == null || ps == null || ps.getPackageName() == null) {
            return;
        }
        writeKernelMappingLPr(ps.getPackageName(), ps.getAppId(), ps.getNotInstalledUserIds());
    }

    void writeKernelMappingLPr(String name, int appId, int[] excludedUserIds) {
        KernelPackageState cur = this.mKernelMapping.get(name);
        boolean userIdsChanged = false;
        boolean firstTime = cur == null;
        userIdsChanged = (firstTime || !Arrays.equals(excludedUserIds, cur.excludedUserIds)) ? true : true;
        File dir = new File(this.mKernelMappingFilename, name);
        if (firstTime) {
            dir.mkdir();
            cur = new KernelPackageState();
            this.mKernelMapping.put(name, cur);
        }
        if (cur.appId != appId) {
            File appIdFile = new File(dir, "appid");
            writeIntToFile(appIdFile, appId);
        }
        if (userIdsChanged) {
            for (int i = 0; i < excludedUserIds.length; i++) {
                if (cur.excludedUserIds == null || !ArrayUtils.contains(cur.excludedUserIds, excludedUserIds[i])) {
                    writeIntToFile(new File(dir, "excluded_userids"), excludedUserIds[i]);
                }
            }
            if (cur.excludedUserIds != null) {
                for (int i2 = 0; i2 < cur.excludedUserIds.length; i2++) {
                    if (!ArrayUtils.contains(excludedUserIds, cur.excludedUserIds[i2])) {
                        writeIntToFile(new File(dir, "clear_userid"), cur.excludedUserIds[i2]);
                    }
                }
            }
            cur.excludedUserIds = excludedUserIds;
        }
    }

    private void writeIntToFile(File file, int value) {
        try {
            FileUtils.bytesToFile(file.getAbsolutePath(), Integer.toString(value).getBytes(StandardCharsets.US_ASCII));
        } catch (IOException e) {
            Slog.w(TAG, "Couldn't write " + value + " to " + file.getAbsolutePath());
        }
    }

    void writePackageListLPr() {
        writePackageListLPr(-1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writePackageListLPr(int creatingUserId) {
        String filename = this.mPackageListFilename.getAbsolutePath();
        String ctx = SELinux.fileSelabelLookup(filename);
        if (ctx == null) {
            Slog.wtf(TAG, "Failed to get SELinux context for " + this.mPackageListFilename.getAbsolutePath());
        }
        if (!SELinux.setFSCreateContext(ctx)) {
            Slog.wtf(TAG, "Failed to set packages.list SELinux context");
        }
        try {
            writePackageListLPrInternal(creatingUserId);
        } finally {
            SELinux.setFSCreateContext((String) null);
        }
    }

    private void writePackageListLPrInternal(int creatingUserId) {
        List<UserInfo> users;
        int[] userIds;
        Settings settings = this;
        List<UserInfo> users2 = getActiveUsers(UserManagerService.getInstance(), true);
        int[] userIds2 = new int[users2.size()];
        for (int i = 0; i < userIds2.length; i++) {
            userIds2[i] = users2.get(i).id;
        }
        if (creatingUserId != -1) {
            userIds2 = ArrayUtils.appendInt(userIds2, creatingUserId);
        }
        File tempFile = new File(settings.mPackageListFilename.getAbsolutePath() + ".tmp");
        JournaledFile journal = new JournaledFile(settings.mPackageListFilename, tempFile);
        File writeTarget = journal.chooseForWrite();
        BufferedWriter writer = null;
        try {
            FileOutputStream fstr = new FileOutputStream(writeTarget);
            writer = new BufferedWriter(new OutputStreamWriter(fstr, Charset.defaultCharset()));
            FileUtils.setPermissions(fstr.getFD(), FrameworkStatsLog.DISPLAY_HBM_STATE_CHANGED, 1000, 1032);
            StringBuilder sb = new StringBuilder();
            for (PackageSetting pkg : settings.mPackages.values()) {
                String dataPath = pkg.getPkg() == null ? null : PackageInfoWithoutStateUtils.getDataDir(pkg.getPkg(), 0).getAbsolutePath();
                try {
                    if (pkg.getPkg() == null) {
                        users = users2;
                        userIds = userIds2;
                    } else if (dataPath == null) {
                        users = users2;
                        userIds = userIds2;
                    } else {
                        boolean isDebug = pkg.getPkg().isDebuggable();
                        IntArray gids = new IntArray();
                        int length = userIds2.length;
                        int i2 = 0;
                        while (i2 < length) {
                            int userId = userIds2[i2];
                            List<UserInfo> users3 = users2;
                            try {
                                int[] userIds3 = userIds2;
                                IntArray gids2 = gids;
                                gids2.addAll(settings.mPermissionDataProvider.getGidsForUid(UserHandle.getUid(userId, pkg.getAppId())));
                                i2++;
                                settings = this;
                                gids = gids2;
                                users2 = users3;
                                userIds2 = userIds3;
                            } catch (Exception e) {
                                e = e;
                                Slog.wtf(TAG, "Failed to write packages.list", e);
                                IoUtils.closeQuietly(writer);
                                journal.rollback();
                                return;
                            }
                        }
                        List<UserInfo> users4 = users2;
                        int[] userIds4 = userIds2;
                        IntArray gids3 = gids;
                        if (dataPath.indexOf(32) >= 0) {
                            settings = this;
                            users2 = users4;
                            userIds2 = userIds4;
                        } else {
                            sb.setLength(0);
                            sb.append(pkg.getPkg().getPackageName());
                            sb.append(" ");
                            sb.append(pkg.getPkg().getUid());
                            sb.append(isDebug ? " 1 " : " 0 ");
                            sb.append(dataPath);
                            sb.append(" ");
                            sb.append(AndroidPackageUtils.getSeInfo(pkg.getPkg(), pkg));
                            sb.append(" ");
                            int gidsSize = gids3.size();
                            if (gids3.size() > 0) {
                                sb.append(gids3.get(0));
                                for (int i3 = 1; i3 < gidsSize; i3++) {
                                    sb.append(",");
                                    sb.append(gids3.get(i3));
                                }
                            } else {
                                sb.append("none");
                            }
                            sb.append(" ");
                            String str = "1";
                            sb.append(pkg.getPkg().isProfileableByShell() ? "1" : "0");
                            sb.append(" ");
                            sb.append(pkg.getPkg().getLongVersionCode());
                            sb.append(" ");
                            if (!pkg.getPkg().isProfileable()) {
                                str = "0";
                            }
                            sb.append(str);
                            sb.append(" ");
                            if (pkg.isSystem()) {
                                sb.append("@system");
                            } else if (pkg.isProduct()) {
                                sb.append("@product");
                            } else if (pkg.getInstallSource().installerPackageName != null && !pkg.getInstallSource().installerPackageName.isEmpty()) {
                                sb.append(pkg.getInstallSource().installerPackageName);
                            } else {
                                sb.append("@null");
                            }
                            sb.append("\n");
                            writer.append((CharSequence) sb);
                            settings = this;
                            users2 = users4;
                            userIds2 = userIds4;
                        }
                    }
                    if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(pkg.getPackageName())) {
                        settings = this;
                        users2 = users;
                        userIds2 = userIds;
                    } else {
                        Slog.w(TAG, "Skipping " + pkg + " due to missing metadata");
                        settings = this;
                        users2 = users;
                        userIds2 = userIds;
                    }
                } catch (Exception e2) {
                    e = e2;
                    Slog.wtf(TAG, "Failed to write packages.list", e);
                    IoUtils.closeQuietly(writer);
                    journal.rollback();
                    return;
                }
            }
            writer.flush();
            FileUtils.sync(fstr);
            writer.close();
            journal.commit();
        } catch (Exception e3) {
            e = e3;
        }
    }

    void writeDisabledSysPackageLPr(TypedXmlSerializer serializer, PackageSetting pkg) throws IOException {
        serializer.startTag((String) null, "updated-package");
        serializer.attribute((String) null, "name", pkg.getPackageName());
        if (pkg.getRealName() != null) {
            serializer.attribute((String) null, "realName", pkg.getRealName());
        }
        serializer.attribute((String) null, "codePath", pkg.getPathString());
        serializer.attributeLongHex((String) null, "ft", pkg.getLastModifiedTime());
        serializer.attributeLongHex((String) null, "ut", pkg.getLastUpdateTime());
        serializer.attributeLong((String) null, "version", pkg.getVersionCode());
        if (pkg.getLegacyNativeLibraryPath() != null) {
            serializer.attribute((String) null, "nativeLibraryPath", pkg.getLegacyNativeLibraryPath());
        }
        if (pkg.getPrimaryCpuAbi() != null) {
            serializer.attribute((String) null, "primaryCpuAbi", pkg.getPrimaryCpuAbi());
        }
        if (pkg.getSecondaryCpuAbi() != null) {
            serializer.attribute((String) null, "secondaryCpuAbi", pkg.getSecondaryCpuAbi());
        }
        if (pkg.getCpuAbiOverride() != null) {
            serializer.attribute((String) null, "cpuAbiOverride", pkg.getCpuAbiOverride());
        }
        if (!pkg.hasSharedUser()) {
            serializer.attributeInt((String) null, "userId", pkg.getAppId());
        } else {
            serializer.attributeInt((String) null, "sharedUserId", pkg.getAppId());
        }
        serializer.attributeFloat((String) null, "loadingProgress", pkg.getLoadingProgress());
        writeUsesSdkLibLPw(serializer, pkg.getUsesSdkLibraries(), pkg.getUsesSdkLibrariesVersionsMajor());
        writeUsesStaticLibLPw(serializer, pkg.getUsesStaticLibraries(), pkg.getUsesStaticLibrariesVersions());
        serializer.endTag((String) null, "updated-package");
    }

    void writePackageLPr(TypedXmlSerializer serializer, PackageSetting pkg) throws IOException {
        serializer.startTag((String) null, "package");
        serializer.attribute((String) null, "name", pkg.getPackageName());
        if (pkg.getRealName() != null) {
            serializer.attribute((String) null, "realName", pkg.getRealName());
        }
        serializer.attribute((String) null, "codePath", pkg.getPathString());
        if (pkg.getLegacyNativeLibraryPath() != null) {
            serializer.attribute((String) null, "nativeLibraryPath", pkg.getLegacyNativeLibraryPath());
        }
        if (pkg.getPrimaryCpuAbi() != null) {
            serializer.attribute((String) null, "primaryCpuAbi", pkg.getPrimaryCpuAbi());
        }
        if (pkg.getSecondaryCpuAbi() != null) {
            serializer.attribute((String) null, "secondaryCpuAbi", pkg.getSecondaryCpuAbi());
        }
        if (pkg.getCpuAbiOverride() != null) {
            serializer.attribute((String) null, "cpuAbiOverride", pkg.getCpuAbiOverride());
        }
        serializer.attributeInt((String) null, "publicFlags", pkg.getFlags());
        serializer.attributeInt((String) null, "privateFlags", pkg.getPrivateFlags());
        serializer.attributeLongHex((String) null, "ft", pkg.getLastModifiedTime());
        serializer.attributeLongHex((String) null, "ut", pkg.getLastUpdateTime());
        serializer.attributeLong((String) null, "version", pkg.getVersionCode());
        if (!pkg.hasSharedUser()) {
            serializer.attributeInt((String) null, "userId", pkg.getAppId());
        } else {
            serializer.attributeInt((String) null, "sharedUserId", pkg.getAppId());
        }
        InstallSource installSource = pkg.getInstallSource();
        if (installSource.installerPackageName != null) {
            serializer.attribute((String) null, "installer", installSource.installerPackageName);
        }
        if (installSource.installerAttributionTag != null) {
            serializer.attribute((String) null, "installerAttributionTag", installSource.installerAttributionTag);
        }
        serializer.attributeInt((String) null, "packageSource", installSource.packageSource);
        if (installSource.isOrphaned) {
            serializer.attributeBoolean((String) null, "isOrphaned", true);
        }
        if (installSource.initiatingPackageName != null) {
            serializer.attribute((String) null, "installInitiator", installSource.initiatingPackageName);
        }
        if (installSource.isInitiatingPackageUninstalled) {
            serializer.attributeBoolean((String) null, "installInitiatorUninstalled", true);
        }
        if (installSource.originatingPackageName != null) {
            serializer.attribute((String) null, "installOriginator", installSource.originatingPackageName);
        }
        if (pkg.getVolumeUuid() != null) {
            serializer.attribute((String) null, ATTR_VOLUME_UUID, pkg.getVolumeUuid());
        }
        if (pkg.getCategoryOverride() != -1) {
            serializer.attributeInt((String) null, "categoryHint", pkg.getCategoryOverride());
        }
        if (pkg.isUpdateAvailable()) {
            serializer.attributeBoolean((String) null, "updateAvailable", true);
        }
        if (pkg.isForceQueryableOverride()) {
            serializer.attributeBoolean((String) null, "forceQueryable", true);
        }
        if (pkg.isLoading()) {
            serializer.attributeBoolean((String) null, "isLoading", true);
        }
        serializer.attributeFloat((String) null, "loadingProgress", pkg.getLoadingProgress());
        serializer.attribute((String) null, "domainSetId", pkg.getDomainSetId().toString());
        writeUsesSdkLibLPw(serializer, pkg.getUsesSdkLibraries(), pkg.getUsesSdkLibrariesVersionsMajor());
        writeUsesStaticLibLPw(serializer, pkg.getUsesStaticLibraries(), pkg.getUsesStaticLibrariesVersions());
        pkg.getSignatures().writeXml(serializer, "sigs", this.mPastSignatures.untrackedStorage());
        if (installSource.initiatingPackageSignatures != null) {
            installSource.initiatingPackageSignatures.writeXml(serializer, "install-initiator-sigs", this.mPastSignatures.untrackedStorage());
        }
        writeSigningKeySetLPr(serializer, pkg.getKeySetData());
        writeUpgradeKeySetsLPr(serializer, pkg.getKeySetData());
        writeKeySetAliasesLPr(serializer, pkg.getKeySetData());
        writeMimeGroupLPr(serializer, pkg.getMimeGroups());
        serializer.endTag((String) null, "package");
    }

    void writeSigningKeySetLPr(TypedXmlSerializer serializer, PackageKeySetData data) throws IOException {
        serializer.startTag((String) null, "proper-signing-keyset");
        serializer.attributeLong((String) null, "identifier", data.getProperSigningKeySet());
        serializer.endTag((String) null, "proper-signing-keyset");
    }

    void writeUpgradeKeySetsLPr(TypedXmlSerializer serializer, PackageKeySetData data) throws IOException {
        long[] upgradeKeySets;
        if (data.isUsingUpgradeKeySets()) {
            for (long id : data.getUpgradeKeySets()) {
                serializer.startTag((String) null, "upgrade-keyset");
                serializer.attributeLong((String) null, "identifier", id);
                serializer.endTag((String) null, "upgrade-keyset");
            }
        }
    }

    void writeKeySetAliasesLPr(TypedXmlSerializer serializer, PackageKeySetData data) throws IOException {
        for (Map.Entry<String, Long> e : data.getAliases().entrySet()) {
            serializer.startTag((String) null, "defined-keyset");
            serializer.attribute((String) null, "alias", e.getKey());
            serializer.attributeLong((String) null, "identifier", e.getValue().longValue());
            serializer.endTag((String) null, "defined-keyset");
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3234=5, 3235=5, 3237=5, 3239=5, 3240=5, 3242=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Code restructure failed: missing block: B:101:0x02a5, code lost:
        r18.mVerifierDeviceIdentity = android.content.pm.VerifierDeviceIdentity.parse(r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:103:0x02ac, code lost:
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:104:0x02ad, code lost:
        android.util.Slog.w("PackageManager", "Discard invalid verifier device id: " + r0.getMessage());
     */
    /* JADX WARN: Code restructure failed: missing block: B:106:0x02cf, code lost:
        if (com.android.server.pm.Settings.TAG_READ_EXTERNAL_STORAGE.equals(r0) == false) goto L169;
     */
    /* JADX WARN: Code restructure failed: missing block: B:109:0x02db, code lost:
        if (r0.equals("keyset-settings") == false) goto L172;
     */
    /* JADX WARN: Code restructure failed: missing block: B:110:0x02dd, code lost:
        r18.mKeySetManagerService.readKeySetsLPw(r0, r18.mKeySetRefs.untrackedStorage());
     */
    /* JADX WARN: Code restructure failed: missing block: B:112:0x02f2, code lost:
        if ("version".equals(r0) == false) goto L175;
     */
    /* JADX WARN: Code restructure failed: missing block: B:113:0x02f4, code lost:
        r0 = com.android.internal.util.XmlUtils.readStringAttribute(r0, com.android.server.pm.Settings.ATTR_VOLUME_UUID);
        r2 = findOrCreateVersion(r0);
        r2.sdkVersion = r0.getAttributeInt((java.lang.String) null, com.android.server.pm.Settings.ATTR_SDK_VERSION);
        r2.databaseVersion = r0.getAttributeInt((java.lang.String) null, com.android.server.pm.Settings.ATTR_DATABASE_VERSION);
        r2.fingerprint = com.android.internal.util.XmlUtils.readStringAttribute(r0, com.android.server.pm.Settings.ATTR_FINGERPRINT);
     */
    /* JADX WARN: Code restructure failed: missing block: B:115:0x0320, code lost:
        if (r0.equals(com.android.server.pm.verify.domain.DomainVerificationPersistence.TAG_DOMAIN_VERIFICATIONS) == false) goto L181;
     */
    /* JADX WARN: Code restructure failed: missing block: B:118:0x0326, code lost:
        r18.mDomainVerificationManager.readSettings(r19, r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:120:0x0332, code lost:
        if (r0.equals(com.android.server.pm.verify.domain.DomainVerificationLegacySettings.TAG_DOMAIN_VERIFICATIONS_LEGACY) == false) goto L184;
     */
    /* JADX WARN: Code restructure failed: missing block: B:121:0x0334, code lost:
        r18.mDomainVerificationManager.readLegacySettings(r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:122:0x033a, code lost:
        android.util.Slog.w("PackageManager", "Unknown element under <packages>: " + r0.getName());
        com.android.internal.util.XmlUtils.skipCurrentTag(r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:132:0x0388, code lost:
        if (r18.mVersion.containsKey("primary_physical") == false) goto L42;
     */
    /* JADX WARN: Code restructure failed: missing block: B:133:0x038a, code lost:
        android.util.Slog.wtf("PackageManager", "No external VersionInfo found in settings, using current.");
        findOrCreateVersion("primary_physical").forceCurrent();
     */
    /* JADX WARN: Code restructure failed: missing block: B:134:0x0395, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:143:0x03e2, code lost:
        android.util.Slog.wtf("PackageManager", "No internal VersionInfo found in settings, using current.");
        findOrCreateVersion(android.os.storage.StorageManager.UUID_PRIVATE_INTERNAL).forceCurrent();
     */
    /* JADX WARN: Code restructure failed: missing block: B:145:0x03f4, code lost:
        if (r18.mVersion.containsKey("primary_physical") == false) goto L42;
     */
    /* JADX WARN: Code restructure failed: missing block: B:147:0x03f7, code lost:
        r0 = r18.mPendingPackages.size();
        r3 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:148:0x03fe, code lost:
        if (r3 >= r0) goto L60;
     */
    /* JADX WARN: Code restructure failed: missing block: B:149:0x0400, code lost:
        r5 = r18.mPendingPackages.get(r3);
        r6 = r5.getSharedUserAppId();
     */
    /* JADX WARN: Code restructure failed: missing block: B:150:0x040c, code lost:
        if (r6 > 0) goto L50;
     */
    /* JADX WARN: Code restructure failed: missing block: B:152:0x0410, code lost:
        r8 = getSettingLPr(r6);
     */
    /* JADX WARN: Code restructure failed: missing block: B:153:0x0416, code lost:
        if ((r8 instanceof com.android.server.pm.SharedUserSetting) == false) goto L54;
     */
    /* JADX WARN: Code restructure failed: missing block: B:154:0x0418, code lost:
        r9 = (com.android.server.pm.SharedUserSetting) r8;
        addPackageSettingLPw(r5, r9);
     */
    /* JADX WARN: Code restructure failed: missing block: B:156:0x0424, code lost:
        if (r8 == null) goto L58;
     */
    /* JADX WARN: Code restructure failed: missing block: B:157:0x0426, code lost:
        r9 = "Bad package setting: package " + r5.getPackageName() + " has shared uid " + r6 + " that is not a shared uid\n";
        r18.mReadMessages.append(r9);
        com.android.server.pm.PackageManagerService.reportSettingsProblem(6, r9);
     */
    /* JADX WARN: Code restructure failed: missing block: B:158:0x0454, code lost:
        r9 = "Bad package setting: package " + r5.getPackageName() + " has shared uid " + r6 + " that is not defined\n";
        r18.mReadMessages.append(r9);
        com.android.server.pm.PackageManagerService.reportSettingsProblem(6, r9);
     */
    /* JADX WARN: Code restructure failed: missing block: B:159:0x0480, code lost:
        r3 = r3 + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:160:0x0484, code lost:
        r18.mPendingPackages.clear();
     */
    /* JADX WARN: Code restructure failed: missing block: B:161:0x048f, code lost:
        if (r18.mBackupStoppedPackagesFilename.exists() != false) goto L88;
     */
    /* JADX WARN: Code restructure failed: missing block: B:163:0x0497, code lost:
        if (r18.mStoppedPackagesFilename.exists() == false) goto L65;
     */
    /* JADX WARN: Code restructure failed: missing block: B:165:0x049a, code lost:
        r3 = r20.iterator();
     */
    /* JADX WARN: Code restructure failed: missing block: B:167:0x04a2, code lost:
        if (r3.hasNext() == false) goto L69;
     */
    /* JADX WARN: Code restructure failed: missing block: B:168:0x04a4, code lost:
        readPackageRestrictionsLPr(r3.next().id, r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:169:0x04b0, code lost:
        readStoppedLPw();
        r18.mBackupStoppedPackagesFilename.delete();
        r18.mStoppedPackagesFilename.delete();
        writePackageRestrictionsLPr(0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:170:0x04c1, code lost:
        r3 = r20.iterator();
     */
    /* JADX WARN: Code restructure failed: missing block: B:172:0x04c9, code lost:
        if (r3.hasNext() == false) goto L74;
     */
    /* JADX WARN: Code restructure failed: missing block: B:173:0x04cb, code lost:
        r5 = r3.next();
        r18.mRuntimePermissionsPersistence.readStateForUserSync(r5.id, getInternalVersion(), r18.mPackages, r18.mSharedUsers, getUserRuntimePermissionsFile(r5.id));
     */
    /* JADX WARN: Code restructure failed: missing block: B:174:0x04e7, code lost:
        r3 = r18.mDisabledSysPackages.values().iterator();
     */
    /* JADX WARN: Code restructure failed: missing block: B:176:0x04f5, code lost:
        if (r3.hasNext() == false) goto L85;
     */
    /* JADX WARN: Code restructure failed: missing block: B:177:0x04f7, code lost:
        r5 = r3.next();
        r6 = getSettingLPr(r5.getAppId());
     */
    /* JADX WARN: Code restructure failed: missing block: B:178:0x0507, code lost:
        if ((r6 instanceof com.android.server.pm.SharedUserSetting) == false) goto L84;
     */
    /* JADX WARN: Code restructure failed: missing block: B:179:0x0509, code lost:
        r8 = (com.android.server.pm.SharedUserSetting) r6;
        r8.mDisabledPackages.add(r5);
        r5.setSharedUserAppId(r8.mAppId);
     */
    /* JADX WARN: Code restructure failed: missing block: B:181:0x0517, code lost:
        r18.mReadMessages.append("Read completed successfully: ").append(r18.mPackages.size()).append(" packages, ").append(r18.mSharedUsers.size()).append(" shared uids\n");
        writeKernelMappingLPr();
     */
    /* JADX WARN: Code restructure failed: missing block: B:182:0x0542, code lost:
        return true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:183:0x0543, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:186:0x054e, code lost:
        android.util.Slog.wtf("PackageManager", "No internal VersionInfo found in settings, using current.");
        findOrCreateVersion(android.os.storage.StorageManager.UUID_PRIVATE_INTERNAL).forceCurrent();
     */
    /* JADX WARN: Code restructure failed: missing block: B:189:0x0562, code lost:
        android.util.Slog.wtf("PackageManager", "No external VersionInfo found in settings, using current.");
        findOrCreateVersion("primary_physical").forceCurrent();
     */
    /* JADX WARN: Code restructure failed: missing block: B:51:0x014e, code lost:
        if (r0 != r5) goto L111;
     */
    /* JADX WARN: Code restructure failed: missing block: B:52:0x0150, code lost:
        r16 = r3;
     */
    /* JADX WARN: Code restructure failed: missing block: B:53:0x0156, code lost:
        r0 = r0.getName();
     */
    /* JADX WARN: Code restructure failed: missing block: B:54:0x0162, code lost:
        if (r0.equals("package") == false) goto L116;
     */
    /* JADX WARN: Code restructure failed: missing block: B:55:0x0164, code lost:
        readPackageLPw(r0, r2, r0);
        r16 = r3;
     */
    /* JADX WARN: Code restructure failed: missing block: B:57:0x0174, code lost:
        if (r0.equals("permissions") == false) goto L119;
     */
    /* JADX WARN: Code restructure failed: missing block: B:58:0x0176, code lost:
        r18.mPermissions.readPermissions(r0);
        r16 = r3;
     */
    /* JADX WARN: Code restructure failed: missing block: B:60:0x0188, code lost:
        if (r0.equals("permission-trees") == false) goto L122;
     */
    /* JADX WARN: Code restructure failed: missing block: B:61:0x018a, code lost:
        r18.mPermissions.readPermissionTrees(r0);
        r16 = r3;
     */
    /* JADX WARN: Code restructure failed: missing block: B:63:0x019c, code lost:
        if (r0.equals(com.android.server.pm.Settings.TAG_SHARED_USER) == false) goto L125;
     */
    /* JADX WARN: Code restructure failed: missing block: B:64:0x019e, code lost:
        readSharedUserLPw(r0, r2);
        r16 = r3;
     */
    /* JADX WARN: Code restructure failed: missing block: B:66:0x01ae, code lost:
        if (r0.equals("preferred-packages") == false) goto L128;
     */
    /* JADX WARN: Code restructure failed: missing block: B:67:0x01b0, code lost:
        r16 = r3;
     */
    /* JADX WARN: Code restructure failed: missing block: B:69:0x01bd, code lost:
        if (r0.equals("preferred-activities") == false) goto L131;
     */
    /* JADX WARN: Code restructure failed: missing block: B:70:0x01bf, code lost:
        readPreferredActivitiesLPw(r0, r12);
        r16 = r3;
     */
    /* JADX WARN: Code restructure failed: missing block: B:72:0x01cf, code lost:
        if (r0.equals(com.android.server.pm.Settings.TAG_PERSISTENT_PREFERRED_ACTIVITIES) == false) goto L134;
     */
    /* JADX WARN: Code restructure failed: missing block: B:73:0x01d1, code lost:
        readPersistentPreferredActivitiesLPw(r0, r12);
        r16 = r3;
     */
    /* JADX WARN: Code restructure failed: missing block: B:75:0x01e0, code lost:
        if (r0.equals(com.android.server.pm.Settings.TAG_CROSS_PROFILE_INTENT_FILTERS) == false) goto L137;
     */
    /* JADX WARN: Code restructure failed: missing block: B:76:0x01e2, code lost:
        readCrossProfileIntentFiltersLPw(r0, r12);
        r16 = r3;
     */
    /* JADX WARN: Code restructure failed: missing block: B:78:0x01f1, code lost:
        if (r0.equals(com.android.server.pm.Settings.TAG_DEFAULT_BROWSER) == false) goto L140;
     */
    /* JADX WARN: Code restructure failed: missing block: B:79:0x01f3, code lost:
        readDefaultAppsLPw(r0, r12);
        r16 = r3;
     */
    /* JADX WARN: Code restructure failed: missing block: B:81:0x0203, code lost:
        if (r0.equals("updated-package") == false) goto L143;
     */
    /* JADX WARN: Code restructure failed: missing block: B:82:0x0205, code lost:
        readDisabledSysPackageLPw(r0, r2);
        r16 = r3;
     */
    /* JADX WARN: Code restructure failed: missing block: B:84:0x0216, code lost:
        if (r0.equals("renamed-package") == false) goto L150;
     */
    /* JADX WARN: Code restructure failed: missing block: B:85:0x0218, code lost:
        r0 = r0.getAttributeValue((java.lang.String) null, "new");
        r5 = r0.getAttributeValue((java.lang.String) null, "old");
     */
    /* JADX WARN: Code restructure failed: missing block: B:86:0x0226, code lost:
        if (r0 == null) goto L149;
     */
    /* JADX WARN: Code restructure failed: missing block: B:87:0x0228, code lost:
        if (r5 == null) goto L149;
     */
    /* JADX WARN: Code restructure failed: missing block: B:88:0x022a, code lost:
        r18.mRenamedPackages.put(r0, r5);
     */
    /* JADX WARN: Code restructure failed: missing block: B:89:0x022f, code lost:
        r16 = r3;
     */
    /* JADX WARN: Code restructure failed: missing block: B:92:0x0241, code lost:
        if (r0.equals("last-platform-version") == false) goto L154;
     */
    /* JADX WARN: Code restructure failed: missing block: B:93:0x0243, code lost:
        r0 = findOrCreateVersion(android.os.storage.StorageManager.UUID_PRIVATE_INTERNAL);
        r16 = findOrCreateVersion("primary_physical");
        r16 = r3;
        r0.sdkVersion = r0.getAttributeInt((java.lang.String) null, "internal", 0);
        r16.sdkVersion = r0.getAttributeInt((java.lang.String) null, "external", 0);
        r2 = com.android.internal.util.XmlUtils.readStringAttribute(r0, com.android.server.pm.Settings.ATTR_FINGERPRINT);
        r16.fingerprint = r2;
        r0.fingerprint = r2;
     */
    /* JADX WARN: Code restructure failed: missing block: B:94:0x026e, code lost:
        r16 = r3;
     */
    /* JADX WARN: Code restructure failed: missing block: B:95:0x0276, code lost:
        if (r0.equals("database-version") == false) goto L157;
     */
    /* JADX WARN: Code restructure failed: missing block: B:96:0x0278, code lost:
        r0 = findOrCreateVersion(android.os.storage.StorageManager.UUID_PRIVATE_INTERNAL);
        r2 = findOrCreateVersion("primary_physical");
        r0.databaseVersion = r0.getAttributeInt((java.lang.String) null, "internal", 0);
        r2.databaseVersion = r0.getAttributeInt((java.lang.String) null, "external", 0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:98:0x029b, code lost:
        if (r0.equals("verifier") == false) goto L167;
     */
    /* JADX WARN: Code restructure failed: missing block: B:99:0x029d, code lost:
        r0 = r0.getAttributeValue((java.lang.String) null, "device");
     */
    /* JADX WARN: Removed duplicated region for block: B:143:0x03e2  */
    /* JADX WARN: Removed duplicated region for block: B:186:0x054e  */
    /* JADX WARN: Removed duplicated region for block: B:189:0x0562  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean readLPw(Computer computer, List<UserInfo> users) {
        int type;
        int outerDepth;
        List<UserInfo> list = users;
        FileInputStream str = null;
        if (this.mXmlSupport && this.mBackupSettingsFilename.exists()) {
            VerifyExt filesVerify = VerifyFacotry.getInstance().getVerifyExt();
            filesVerify.setBackupName(this.mBackupSettingsFilename);
            filesVerify.PackagesVerifyFileBeforeFinishR(this.mBackupSettingsFilename);
        }
        int i = 4;
        if (this.mBackupSettingsFilename.exists()) {
            try {
                str = new FileInputStream(this.mBackupSettingsFilename);
                this.mReadMessages.append("Reading from backup settings file\n");
                PackageManagerService.reportSettingsProblem(4, "Need to read from backup settings file");
                if (this.mSettingsFilename.exists()) {
                    Slog.w("PackageManager", "Cleaning up settings file " + this.mSettingsFilename);
                    this.mSettingsFilename.delete();
                }
            } catch (IOException e) {
            }
        }
        this.mPendingPackages.clear();
        this.mPastSignatures.clear();
        this.mKeySetRefs.clear();
        this.mInstallerPackages.clear();
        ArrayMap<String, Long> originalFirstInstallTimes = new ArrayMap<>();
        int i2 = 1;
        int i3 = 0;
        if (str == null) {
            try {
                try {
                    if (!this.mSettingsFilename.exists()) {
                        this.mReadMessages.append("No settings file found\n");
                        PackageManagerService.reportSettingsProblem(4, "No settings file; creating initial state");
                        findOrCreateVersion(StorageManager.UUID_PRIVATE_INTERNAL).forceCurrent();
                        findOrCreateVersion("primary_physical").forceCurrent();
                        if (!this.mVersion.containsKey(StorageManager.UUID_PRIVATE_INTERNAL)) {
                            Slog.wtf("PackageManager", "No internal VersionInfo found in settings, using current.");
                            findOrCreateVersion(StorageManager.UUID_PRIVATE_INTERNAL).forceCurrent();
                        }
                        if (!this.mVersion.containsKey("primary_physical")) {
                            Slog.wtf("PackageManager", "No external VersionInfo found in settings, using current.");
                            findOrCreateVersion("primary_physical").forceCurrent();
                        }
                        return false;
                    }
                    str = new FileInputStream(this.mSettingsFilename);
                } catch (Throwable th) {
                    th = th;
                    if (!this.mVersion.containsKey(StorageManager.UUID_PRIVATE_INTERNAL)) {
                    }
                    if (!this.mVersion.containsKey("primary_physical")) {
                    }
                    throw th;
                }
            } catch (IOException | XmlPullParserException e2) {
                e = e2;
                this.mReadMessages.append("Error reading: " + e.toString());
                PackageManagerService.reportSettingsProblem(6, "Error reading settings: " + e);
                Slog.wtf("PackageManager", "Error reading package manager settings", e);
                if (!this.mVersion.containsKey(StorageManager.UUID_PRIVATE_INTERNAL)) {
                }
            }
        }
        TypedXmlPullParser parser = Xml.resolvePullParser(str);
        while (true) {
            type = parser.next();
            if (type == 2 || type == 1) {
                break;
            }
        }
        if (type != 2) {
            this.mReadMessages.append("No start tag found in settings file\n");
            PackageManagerService.reportSettingsProblem(5, "No start tag found in package manager settings");
            Slog.wtf("PackageManager", "No start tag found in package manager settings");
            if (!this.mVersion.containsKey(StorageManager.UUID_PRIVATE_INTERNAL)) {
                Slog.wtf("PackageManager", "No internal VersionInfo found in settings, using current.");
                findOrCreateVersion(StorageManager.UUID_PRIVATE_INTERNAL).forceCurrent();
            }
            if (!this.mVersion.containsKey("primary_physical")) {
                Slog.wtf("PackageManager", "No external VersionInfo found in settings, using current.");
                findOrCreateVersion("primary_physical").forceCurrent();
            }
            return false;
        }
        outerDepth = parser.getDepth();
        while (true) {
            int type2 = parser.next();
            if (type2 == i2) {
                break;
            }
            if (type2 == 3 && parser.getDepth() <= outerDepth) {
                break;
            }
            int outerDepth2 = outerDepth;
            list = users;
            outerDepth = outerDepth2;
            i = 4;
            i2 = 1;
            i3 = 0;
        }
        str.close();
        if (!this.mVersion.containsKey(StorageManager.UUID_PRIVATE_INTERNAL)) {
            Slog.wtf("PackageManager", "No internal VersionInfo found in settings, using current.");
            findOrCreateVersion(StorageManager.UUID_PRIVATE_INTERNAL).forceCurrent();
        }
        list = users;
        outerDepth = outerDepth2;
        i = 4;
        i2 = 1;
        i3 = 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void readPermissionStateForUserSyncLPr(int userId) {
        this.mRuntimePermissionsPersistence.readStateForUserSync(userId, getInternalVersion(), this.mPackages, this.mSharedUsers, getUserRuntimePermissionsFile(userId));
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3389=5] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public void applyDefaultPreferredAppsLPw(int userId) {
        PackageManagerInternal pmInternal;
        int size;
        PackageManagerInternal pmInternal2;
        int size2;
        ScanPartition partition;
        Throwable th;
        int type;
        PackageManagerInternal pmInternal3 = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        for (PackageSetting ps : this.mPackages.values()) {
            if ((1 & ps.getFlags()) != 0 && ps.getPkg() != null && !ps.getPkg().getPreferredActivityFilters().isEmpty()) {
                List<Pair<String, ParsedIntentInfo>> intents = ps.getPkg().getPreferredActivityFilters();
                for (int i = 0; i < intents.size(); i++) {
                    Pair<String, ParsedIntentInfo> pair = intents.get(i);
                    applyDefaultPreferredActivityLPw(pmInternal3, ((ParsedIntentInfo) pair.second).getIntentFilter(), new ComponentName(ps.getPackageName(), (String) pair.first), userId);
                }
            }
        }
        int size3 = PackageManagerService.SYSTEM_PARTITIONS.size();
        int index = 0;
        loop2: while (index < size3) {
            ScanPartition partition2 = PackageManagerService.SYSTEM_PARTITIONS.get(index);
            File preferredDir = new File(partition2.getFolder(), "etc/preferred-apps");
            if (!preferredDir.exists()) {
                pmInternal = pmInternal3;
                size = size3;
            } else if (!preferredDir.isDirectory()) {
                pmInternal = pmInternal3;
                size = size3;
            } else if (preferredDir.canRead()) {
                File[] files = preferredDir.listFiles();
                if (ArrayUtils.isEmpty(files)) {
                    pmInternal = pmInternal3;
                    size = size3;
                } else {
                    int length = files.length;
                    int i2 = 0;
                    while (i2 < length) {
                        File f = files[i2];
                        if (!f.getPath().endsWith(".xml")) {
                            Slog.i(TAG, "Non-xml file " + f + " in " + preferredDir + " directory, ignoring");
                            pmInternal2 = pmInternal3;
                            size2 = size3;
                            partition = partition2;
                        } else if (f.canRead()) {
                            if (PackageManagerService.DEBUG_PREFERRED) {
                                pmInternal2 = pmInternal3;
                                Log.d(TAG, "Reading default preferred " + f);
                            } else {
                                pmInternal2 = pmInternal3;
                            }
                            try {
                                InputStream str = new FileInputStream(f);
                                try {
                                    TypedXmlPullParser parser = Xml.resolvePullParser(str);
                                    while (true) {
                                        size2 = size3;
                                        try {
                                            int type2 = parser.next();
                                            partition = partition2;
                                            if (type2 == 2) {
                                                type = type2;
                                                break;
                                            }
                                            type = type2;
                                            if (type == 1) {
                                                break;
                                            }
                                            size3 = size2;
                                            partition2 = partition;
                                        } catch (Throwable th2) {
                                            partition = partition2;
                                            th = th2;
                                        }
                                    }
                                    if (type != 2) {
                                        try {
                                            Slog.w(TAG, "Preferred apps file " + f + " does not have start tag");
                                            try {
                                                str.close();
                                            } catch (IOException e) {
                                                e = e;
                                                Slog.w(TAG, "Error reading apps file " + f, e);
                                                i2++;
                                                pmInternal3 = pmInternal2;
                                                size3 = size2;
                                                partition2 = partition;
                                            } catch (XmlPullParserException e2) {
                                                e = e2;
                                                Slog.w(TAG, "Error reading apps file " + f, e);
                                                i2++;
                                                pmInternal3 = pmInternal2;
                                                size3 = size2;
                                                partition2 = partition;
                                            }
                                        } catch (Throwable th3) {
                                            th = th3;
                                            try {
                                                str.close();
                                            } catch (Throwable th4) {
                                                th.addSuppressed(th4);
                                            }
                                            throw th;
                                            break loop2;
                                        }
                                    } else if ("preferred-activities".equals(parser.getName())) {
                                        readDefaultPreferredActivitiesLPw(parser, userId);
                                        str.close();
                                    } else {
                                        Slog.w(TAG, "Preferred apps file " + f + " does not start with 'preferred-activities'");
                                        str.close();
                                    }
                                } catch (Throwable th5) {
                                    size2 = size3;
                                    partition = partition2;
                                    th = th5;
                                }
                            } catch (IOException e3) {
                                e = e3;
                                size2 = size3;
                                partition = partition2;
                            } catch (XmlPullParserException e4) {
                                e = e4;
                                size2 = size3;
                                partition = partition2;
                            }
                        } else {
                            Slog.w(TAG, "Preferred apps file " + f + " cannot be read");
                            pmInternal2 = pmInternal3;
                            size2 = size3;
                            partition = partition2;
                        }
                        i2++;
                        pmInternal3 = pmInternal2;
                        size3 = size2;
                        partition2 = partition;
                    }
                    pmInternal = pmInternal3;
                    size = size3;
                }
            } else {
                Slog.w(TAG, "Directory " + preferredDir + " cannot be read");
                pmInternal = pmInternal3;
                size = size3;
            }
            index++;
            pmInternal3 = pmInternal;
            size3 = size;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void removeFilters(PreferredIntentResolver pir, WatchedIntentFilter filter, List<PreferredActivity> existing) {
        if (PackageManagerService.DEBUG_PREFERRED) {
            Slog.i(TAG, existing.size() + " preferred matches for:");
            filter.dump(new LogPrinter(4, TAG), "  ");
        }
        for (int i = existing.size() - 1; i >= 0; i--) {
            PreferredActivity pa = existing.get(i);
            if (PackageManagerService.DEBUG_PREFERRED) {
                Slog.i(TAG, "Removing preferred activity " + pa.mPref.mComponent + ":");
                pa.dump(new LogPrinter(4, TAG), "  ");
            }
            pir.removeFilter((PreferredIntentResolver) pa);
        }
    }

    /* JADX WARN: Incorrect condition in loop: B:7:0x002d */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void applyDefaultPreferredActivityLPw(PackageManagerInternal pmInternal, IntentFilter tmpPa, ComponentName cn, int userId) {
        int ischeme;
        if (PackageManagerService.DEBUG_PREFERRED) {
            Log.d(TAG, "Processing preferred:");
            tmpPa.dump(new LogPrinter(3, TAG), "  ");
        }
        Intent intent = new Intent();
        intent.setAction(tmpPa.getAction(0));
        int flags = 786432;
        for (int i = 0; i < flags; i++) {
            String cat = tmpPa.getCategory(i);
            if (cat.equals("android.intent.category.DEFAULT")) {
                flags = 65536 | flags;
            } else {
                intent.addCategory(cat);
            }
        }
        boolean doNonData = true;
        int dataSchemesCount = tmpPa.countDataSchemes();
        boolean hasSchemes = false;
        int ischeme2 = 0;
        while (ischeme2 < dataSchemesCount) {
            String scheme = tmpPa.getDataScheme(ischeme2);
            if (scheme != null && !scheme.isEmpty()) {
                hasSchemes = true;
            }
            boolean doScheme = true;
            int issp = 0;
            for (int dataSchemeSpecificPartsCount = tmpPa.countDataSchemeSpecificParts(); issp < dataSchemeSpecificPartsCount; dataSchemeSpecificPartsCount = dataSchemeSpecificPartsCount) {
                Uri.Builder builder = new Uri.Builder();
                builder.scheme(scheme);
                PatternMatcher ssp = tmpPa.getDataSchemeSpecificPart(issp);
                builder.opaquePart(ssp.getPath());
                Intent finalIntent = new Intent(intent);
                finalIntent.setData(builder.build());
                applyDefaultPreferredActivityLPw(pmInternal, finalIntent, flags, cn, scheme, ssp, null, null, userId);
                doScheme = false;
                issp++;
                scheme = scheme;
                dataSchemesCount = dataSchemesCount;
            }
            String scheme2 = scheme;
            int dataSchemesCount2 = dataSchemesCount;
            int dataAuthoritiesCount = tmpPa.countDataAuthorities();
            int iauth = 0;
            while (iauth < dataAuthoritiesCount) {
                IntentFilter.AuthorityEntry auth = tmpPa.getDataAuthority(iauth);
                int dataPathsCount = tmpPa.countDataPaths();
                int ipath = 0;
                boolean doScheme2 = doScheme;
                boolean doAuth = true;
                while (ipath < dataPathsCount) {
                    Uri.Builder builder2 = new Uri.Builder();
                    builder2.scheme(scheme2);
                    if (auth.getHost() != null) {
                        builder2.authority(auth.getHost());
                    }
                    PatternMatcher path = tmpPa.getDataPath(ipath);
                    builder2.path(path.getPath());
                    Intent finalIntent2 = new Intent(intent);
                    finalIntent2.setData(builder2.build());
                    applyDefaultPreferredActivityLPw(pmInternal, finalIntent2, flags, cn, scheme2, null, auth, path, userId);
                    doScheme2 = false;
                    doAuth = false;
                    ipath++;
                    dataPathsCount = dataPathsCount;
                    iauth = iauth;
                    dataAuthoritiesCount = dataAuthoritiesCount;
                }
                int iauth2 = iauth;
                int dataAuthoritiesCount2 = dataAuthoritiesCount;
                if (!doAuth) {
                    doScheme = doScheme2;
                } else {
                    Uri.Builder builder3 = new Uri.Builder();
                    builder3.scheme(scheme2);
                    if (auth.getHost() != null) {
                        builder3.authority(auth.getHost());
                    }
                    Intent finalIntent3 = new Intent(intent);
                    finalIntent3.setData(builder3.build());
                    applyDefaultPreferredActivityLPw(pmInternal, finalIntent3, flags, cn, scheme2, null, auth, null, userId);
                    doScheme = false;
                }
                iauth = iauth2 + 1;
                dataAuthoritiesCount = dataAuthoritiesCount2;
            }
            if (doScheme) {
                Uri.Builder builder4 = new Uri.Builder();
                builder4.scheme(scheme2);
                Intent finalIntent4 = new Intent(intent);
                finalIntent4.setData(builder4.build());
                applyDefaultPreferredActivityLPw(pmInternal, finalIntent4, flags, cn, scheme2, null, null, null, userId);
            }
            doNonData = false;
            ischeme2++;
            dataSchemesCount = dataSchemesCount2;
        }
        boolean doNonData2 = doNonData;
        for (int idata = 0; idata < tmpPa.countDataTypes(); idata++) {
            String mimeType = tmpPa.getDataType(idata);
            if (hasSchemes) {
                Uri.Builder builder5 = new Uri.Builder();
                int ischeme3 = 0;
                while (ischeme3 < tmpPa.countDataSchemes()) {
                    String scheme3 = tmpPa.getDataScheme(ischeme3);
                    if (scheme3 == null || scheme3.isEmpty()) {
                        ischeme = ischeme3;
                    } else {
                        Intent finalIntent5 = new Intent(intent);
                        builder5.scheme(scheme3);
                        finalIntent5.setDataAndType(builder5.build(), mimeType);
                        ischeme = ischeme3;
                        applyDefaultPreferredActivityLPw(pmInternal, finalIntent5, flags, cn, scheme3, null, null, null, userId);
                    }
                    ischeme3 = ischeme + 1;
                }
            } else {
                Intent finalIntent6 = new Intent(intent);
                finalIntent6.setType(mimeType);
                applyDefaultPreferredActivityLPw(pmInternal, finalIntent6, flags, cn, null, null, null, null, userId);
            }
            doNonData2 = false;
        }
        if (doNonData2) {
            applyDefaultPreferredActivityLPw(pmInternal, intent, flags, cn, null, null, null, null, userId);
        }
    }

    private void applyDefaultPreferredActivityLPw(PackageManagerInternal pmInternal, Intent intent, int flags, ComponentName cn, String scheme, PatternMatcher ssp, IntentFilter.AuthorityEntry auth, PatternMatcher path, int userId) {
        ComponentName haveNonSys;
        List<ResolveInfo> ri = pmInternal.queryIntentActivities(intent, intent.getType(), flags, Binder.getCallingUid(), userId);
        if (PackageManagerService.DEBUG_PREFERRED) {
            Log.d(TAG, "Queried " + intent + " results: " + ri);
        }
        int numMatches = ri == null ? 0 : ri.size();
        if (numMatches < 1) {
            Slog.w(TAG, "No potential matches found for " + intent + " while setting preferred " + cn.flattenToShortString());
            return;
        }
        ComponentName haveNonSys2 = null;
        ComponentName[] set = new ComponentName[ri.size()];
        int i = 0;
        boolean haveAct = false;
        int systemMatch = 0;
        while (true) {
            if (i >= numMatches) {
                break;
            }
            ActivityInfo ai = ri.get(i).activityInfo;
            int numMatches2 = numMatches;
            set[i] = new ComponentName(ai.packageName, ai.name);
            if ((ai.applicationInfo.flags & 1) == 0) {
                if (ri.get(i).match < 0) {
                    haveNonSys = haveNonSys2;
                } else {
                    if (PackageManagerService.DEBUG_PREFERRED) {
                        Log.d(TAG, "Result " + ai.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + ai.name + ": non-system!");
                    }
                    haveNonSys2 = set[i];
                }
            } else {
                haveNonSys = haveNonSys2;
                if (cn.getPackageName().equals(ai.packageName) && cn.getClassName().equals(ai.name)) {
                    if (PackageManagerService.DEBUG_PREFERRED) {
                        Log.d(TAG, "Result " + ai.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + ai.name + ": default!");
                    }
                    haveAct = true;
                    systemMatch = ri.get(i).match;
                } else {
                    boolean haveAct2 = PackageManagerService.DEBUG_PREFERRED;
                    if (haveAct2) {
                        Log.d(TAG, "Result " + ai.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + ai.name + ": skipped");
                    }
                }
            }
            i++;
            haveNonSys2 = haveNonSys;
            numMatches = numMatches2;
        }
        if (haveNonSys2 != null && 0 < systemMatch) {
            haveNonSys2 = null;
        }
        if (haveAct && haveNonSys2 == null) {
            WatchedIntentFilter filter = new WatchedIntentFilter();
            if (intent.getAction() != null) {
                filter.addAction(intent.getAction());
            }
            if (intent.getCategories() != null) {
                for (String cat : intent.getCategories()) {
                    filter.addCategory(cat);
                }
            }
            if ((65536 & flags) != 0) {
                filter.addCategory("android.intent.category.DEFAULT");
            }
            if (scheme != null) {
                filter.addDataScheme(scheme);
            }
            if (ssp != null) {
                filter.addDataSchemeSpecificPart(ssp.getPath(), ssp.getType());
            }
            if (auth != null) {
                filter.addDataAuthority(auth);
            }
            if (path != null) {
                filter.addDataPath(path);
            }
            if (intent.getType() != null) {
                try {
                    filter.addDataType(intent.getType());
                } catch (IntentFilter.MalformedMimeTypeException e) {
                    Slog.w(TAG, "Malformed mimetype " + intent.getType() + " for " + cn);
                }
            }
            PreferredIntentResolver pir = editPreferredActivitiesLPw(userId);
            List<PreferredActivity> existing = pir.findFilters(filter);
            if (existing != null) {
                removeFilters(pir, filter, existing);
            }
            PreferredActivity pa = new PreferredActivity(filter, systemMatch, set, cn, true);
            pir.addFilter((PackageDataSnapshot) null, (PackageDataSnapshot) pa);
        } else if (haveNonSys2 == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("No component ");
            sb.append(cn.flattenToShortString());
            sb.append(" found setting preferred ");
            sb.append(intent);
            sb.append("; possible matches are ");
            for (int i2 = 0; i2 < set.length; i2++) {
                if (i2 > 0) {
                    sb.append(", ");
                }
                sb.append(set[i2].flattenToShortString());
            }
            Slog.w(TAG, sb.toString());
        } else {
            Slog.i(TAG, "Not setting preferred " + intent + "; found third party match " + haveNonSys2.flattenToShortString());
        }
    }

    private void readDefaultPreferredActivitiesLPw(TypedXmlPullParser parser, int userId) throws XmlPullParserException, IOException {
        PackageManagerInternal pmInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals(TAG_ITEM)) {
                            PreferredActivity tmpPa = new PreferredActivity(parser);
                            if (tmpPa.mPref.getParseError() == null) {
                                applyDefaultPreferredActivityLPw(pmInternal, tmpPa.getIntentFilter(), tmpPa.mPref.mComponent, userId);
                            } else {
                                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <preferred-activity> " + tmpPa.mPref.getParseError() + " at " + parser.getPositionDescription());
                            }
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Unknown element under <preferred-activities>: " + parser.getName());
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void readDisabledSysPackageLPw(TypedXmlPullParser parser, List<UserInfo> users) throws XmlPullParserException, IOException {
        String primaryCpuAbiStr;
        int pkgPrivateFlags;
        LegacyPermissionState legacyState;
        String name = parser.getAttributeValue((String) null, "name");
        String realName = parser.getAttributeValue((String) null, "realName");
        String codePathStr = parser.getAttributeValue((String) null, "codePath");
        String legacyCpuAbiStr = parser.getAttributeValue((String) null, "requiredCpuAbi");
        String legacyNativeLibraryPathStr = parser.getAttributeValue((String) null, "nativeLibraryPath");
        String primaryCpuAbiStr2 = parser.getAttributeValue((String) null, "primaryCpuAbi");
        String secondaryCpuAbiStr = parser.getAttributeValue((String) null, "secondaryCpuAbi");
        String cpuAbiOverrideStr = parser.getAttributeValue((String) null, "cpuAbiOverride");
        if (primaryCpuAbiStr2 == null && legacyCpuAbiStr != null) {
            primaryCpuAbiStr = legacyCpuAbiStr;
        } else {
            primaryCpuAbiStr = primaryCpuAbiStr2;
        }
        long versionCode = parser.getAttributeLong((String) null, "version", 0L);
        int pkgFlags = 0 | 1;
        if (!codePathStr.contains("/priv-app/")) {
            pkgPrivateFlags = 0;
        } else {
            int pkgPrivateFlags2 = 0 | 8;
            pkgPrivateFlags = pkgPrivateFlags2;
        }
        UUID domainSetId = DomainVerificationManagerInternal.DISABLED_ID;
        PackageSetting ps = new PackageSetting(name, realName, new File(codePathStr), legacyNativeLibraryPathStr, primaryCpuAbiStr, secondaryCpuAbiStr, cpuAbiOverrideStr, versionCode, pkgFlags, pkgPrivateFlags, 0, null, null, null, null, null, domainSetId);
        long timeStamp = parser.getAttributeLongHex((String) null, "ft", 0L);
        if (timeStamp == 0) {
            timeStamp = parser.getAttributeLong((String) null, "ts", 0L);
        }
        ps.setLastModifiedTime(timeStamp);
        ps.setLastUpdateTime(parser.getAttributeLongHex((String) null, "ut", 0L));
        ps.setAppId(parser.getAttributeInt((String) null, "userId", 0));
        if (ps.getAppId() <= 0) {
            int sharedUserAppId = parser.getAttributeInt((String) null, "sharedUserId", 0);
            ps.setAppId(sharedUserAppId);
            ps.setSharedUserAppId(sharedUserAppId);
        }
        float loadingProgress = parser.getAttributeFloat((String) null, "loadingProgress", 0.0f);
        ps.setLoadingProgress(loadingProgress);
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1 && (type != 3 || parser.getDepth() > outerDepth)) {
                if (type != 3 && type != 4) {
                    if (parser.getName().equals(TAG_PERMISSIONS)) {
                        if (ps.hasSharedUser()) {
                            legacyState = getSettingLPr(ps.getSharedUserAppId()).getLegacyPermissionState();
                        } else {
                            legacyState = ps.getLegacyPermissionState();
                        }
                        readInstallPermissionsLPr(parser, legacyState, users);
                    } else if (parser.getName().equals(TAG_USES_STATIC_LIB)) {
                        readUsesStaticLibLPw(parser, ps);
                    } else if (parser.getName().equals(TAG_USES_SDK_LIB)) {
                        readUsesSdkLibLPw(parser, ps);
                    } else {
                        PackageManagerService.reportSettingsProblem(5, "Unknown element under <updated-package>: " + parser.getName());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
        }
        if (getPackageLPr(name) == null && ITranPackageManagerService.Instance().shouldAddReadedRemovePackages(name)) {
            Slog.d(TAG, "skip put mDisabledSysPackages:" + name + " ps.appId:" + ps.getAppId());
        } else {
            this.mDisabledSysPackages.put(name, ps);
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:242:0x0a3d
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3953=13] */
    private void readPackageLPw(android.util.TypedXmlPullParser r85, java.util.List<android.content.pm.UserInfo> r86, android.util.ArrayMap<java.lang.String, java.lang.Long> r87) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {
        /*
            r84 = this;
            r13 = r84
            r12 = r85
            java.lang.String r9 = "true"
            java.lang.String r6 = " has bad userId "
            java.lang.String r4 = " at "
            java.lang.String r3 = "Error in package manager settings: package "
            r1 = 0
            r2 = 0
            r5 = 0
            r7 = 0
            r8 = 0
            r10 = 0
            r11 = 0
            r14 = 0
            r15 = 0
            r16 = 0
            r17 = 0
            r18 = 0
            r19 = 0
            r20 = 0
            r21 = 0
            r22 = 0
            r23 = 0
            r24 = 0
            r25 = 0
            r26 = 0
            r27 = -1
            r28 = 0
            r29 = 0
            r30 = 0
            r32 = 0
            r34 = 0
            r36 = 0
            r37 = 0
            r39 = 0
            r40 = 0
            r41 = r14
            r42 = r15
            r15 = 0
            r14 = 0
            java.lang.String r0 = "name"
            java.lang.String r0 = r12.getAttributeValue(r14, r0)     // Catch: java.lang.NumberFormatException -> Lcba
            r1 = r0
            java.lang.String r0 = "realName"
            java.lang.String r0 = r12.getAttributeValue(r14, r0)     // Catch: java.lang.NumberFormatException -> Lca8
            r2 = r0
            java.lang.String r0 = "userId"
            int r0 = r12.getAttributeInt(r14, r0, r15)     // Catch: java.lang.NumberFormatException -> Lc95
            r5 = r0
            java.lang.String r0 = "sharedUserId"
            int r0 = r12.getAttributeInt(r14, r0, r15)     // Catch: java.lang.NumberFormatException -> Lc7e
            r7 = r0
            java.lang.String r0 = "codePath"
            java.lang.String r0 = r12.getAttributeValue(r14, r0)     // Catch: java.lang.NumberFormatException -> Lc64
            r8 = r0
            java.lang.String r0 = "requiredCpuAbi"
            java.lang.String r0 = r12.getAttributeValue(r14, r0)     // Catch: java.lang.NumberFormatException -> Lc48
            r66 = r0
            java.lang.String r0 = "nativeLibraryPath"
            java.lang.String r0 = r12.getAttributeValue(r14, r0)     // Catch: java.lang.NumberFormatException -> Lc2a
            r10 = r5
            r5 = r0
            java.lang.String r0 = "primaryCpuAbi"
            java.lang.String r0 = r12.getAttributeValue(r14, r0)     // Catch: java.lang.NumberFormatException -> Lc09
            r11 = r0
            java.lang.String r0 = "secondaryCpuAbi"
            java.lang.String r0 = r12.getAttributeValue(r14, r0)     // Catch: java.lang.NumberFormatException -> Lbe9
            r67 = r7
            r7 = r0
            java.lang.String r0 = "cpuAbiOverride"
            java.lang.String r0 = r12.getAttributeValue(r14, r0)     // Catch: java.lang.NumberFormatException -> Lbc8
            r68 = r8
            r8 = r0
            java.lang.String r0 = "updateAvailable"
            boolean r0 = r12.getAttributeBoolean(r14, r0, r15)     // Catch: java.lang.NumberFormatException -> Lba5
            r26 = r0
            java.lang.String r0 = "forceQueryable"
            boolean r0 = r12.getAttributeBoolean(r14, r0, r15)     // Catch: java.lang.NumberFormatException -> Lba5
            r39 = r0
            java.lang.String r0 = "loadingProgress"
            r15 = 0
            float r0 = r12.getAttributeFloat(r14, r0, r15)     // Catch: java.lang.NumberFormatException -> Lba5
            r40 = r0
            if (r11 != 0) goto Lbd
            if (r66 == 0) goto Lbd
            r0 = r66
            r41 = r0
            goto Lbf
        Lbd:
            r41 = r11
        Lbf:
            java.lang.String r0 = "version"
            r49 = r3
            r48 = r4
            r3 = 0
            long r15 = r12.getAttributeLong(r14, r0, r3)     // Catch: java.lang.NumberFormatException -> Lb5a
            r4 = r10
            r10 = r15
            java.lang.String r0 = "installer"
            java.lang.String r0 = r12.getAttributeValue(r14, r0)     // Catch: java.lang.NumberFormatException -> Lb31
            r37 = r0
            java.lang.String r0 = "installerAttributionTag"
            java.lang.String r0 = r12.getAttributeValue(r14, r0)     // Catch: java.lang.NumberFormatException -> Lb06
            r38 = r0
            java.lang.String r0 = "packageSource"
            r3 = 0
            int r0 = r12.getAttributeInt(r14, r0, r3)     // Catch: java.lang.NumberFormatException -> Lad9
            r20 = r0
            java.lang.String r0 = "isOrphaned"
            boolean r0 = r12.getAttributeBoolean(r14, r0, r3)     // Catch: java.lang.NumberFormatException -> Lad9
            r21 = r0
            java.lang.String r0 = "installInitiator"
            java.lang.String r0 = r12.getAttributeValue(r14, r0)     // Catch: java.lang.NumberFormatException -> Lad9
            r23 = r0
            java.lang.String r0 = "installOriginator"
            java.lang.String r0 = r12.getAttributeValue(r14, r0)     // Catch: java.lang.NumberFormatException -> Lad9
            r22 = r0
            java.lang.String r0 = "installInitiatorUninstalled"
            r3 = 0
            boolean r0 = r12.getAttributeBoolean(r14, r0, r3)     // Catch: java.lang.NumberFormatException -> Lad9
            r24 = r0
            java.lang.String r0 = "volumeUuid"
            java.lang.String r0 = r12.getAttributeValue(r14, r0)     // Catch: java.lang.NumberFormatException -> Lad9
            r25 = r0
            java.lang.String r0 = "categoryHint"
            r15 = -1
            int r0 = r12.getAttributeInt(r14, r0, r15)     // Catch: java.lang.NumberFormatException -> Lad9
            r27 = r0
            java.lang.String r0 = "domainSetId"
            java.lang.String r0 = r12.getAttributeValue(r14, r0)     // Catch: java.lang.NumberFormatException -> Lad9
            r42 = r0
            boolean r0 = android.text.TextUtils.isEmpty(r42)     // Catch: java.lang.NumberFormatException -> Lad9
            if (r0 == 0) goto L155
            com.android.server.pm.verify.domain.DomainVerificationManagerInternal r0 = r13.mDomainVerificationManager     // Catch: java.lang.NumberFormatException -> L132
            java.util.UUID r0 = r0.generateNewId()     // Catch: java.lang.NumberFormatException -> L132
            r69 = r0
            goto L15b
        L132:
            r0 = move-exception
            r15 = r7
            r16 = r8
            r81 = r9
            r9 = r13
            r18 = r37
            r19 = r38
            r14 = r41
            r3 = r49
            r7 = r67
            r8 = r68
            r13 = 5
            r43 = 0
            r67 = r2
            r2 = r6
            r37 = r10
            r6 = r48
            r10 = r66
            r11 = r5
            r5 = r4
            goto Lcca
        L155:
            java.util.UUID r0 = java.util.UUID.fromString(r42)     // Catch: java.lang.NumberFormatException -> Lad9
            r69 = r0
        L15b:
            java.lang.String r0 = "publicFlags"
            java.lang.String r0 = r12.getAttributeValue(r14, r0)     // Catch: java.lang.NumberFormatException -> Lad9
            r17 = r0
            if (r17 == 0) goto L18f
            int r0 = java.lang.Integer.parseInt(r17)     // Catch: java.lang.NumberFormatException -> L16d
            r28 = r0
            goto L16e
        L16d:
            r0 = move-exception
        L16e:
            java.lang.String r0 = "privateFlags"
            java.lang.String r0 = r12.getAttributeValue(r14, r0)     // Catch: java.lang.NumberFormatException -> L132
            r15 = r0
            if (r15 == 0) goto L187
            int r0 = java.lang.Integer.parseInt(r15)     // Catch: java.lang.NumberFormatException -> L186
            r29 = r0
            r70 = r29
            r29 = r28
            r28 = r15
            goto L213
        L186:
            r0 = move-exception
        L187:
            r70 = r29
            r29 = r28
            r28 = r15
            goto L213
        L18f:
            java.lang.String r0 = "flags"
            java.lang.String r0 = r12.getAttributeValue(r14, r0)     // Catch: java.lang.NumberFormatException -> Lad9
            r17 = r0
            if (r17 == 0) goto L1ca
            int r0 = java.lang.Integer.parseInt(r17)     // Catch: java.lang.NumberFormatException -> L1a0
            r28 = r0
            goto L1a1
        L1a0:
            r0 = move-exception
        L1a1:
            int r0 = com.android.server.pm.Settings.PRE_M_APP_INFO_FLAG_HIDDEN     // Catch: java.lang.NumberFormatException -> L132
            r15 = r28 & r0
            if (r15 == 0) goto L1a9
            r29 = r29 | 1
        L1a9:
            int r15 = com.android.server.pm.Settings.PRE_M_APP_INFO_FLAG_CANT_SAVE_STATE     // Catch: java.lang.NumberFormatException -> L132
            r16 = r28 & r15
            if (r16 == 0) goto L1b3
            r16 = r29 | 2
            r29 = r16
        L1b3:
            int r16 = com.android.server.pm.Settings.PRE_M_APP_INFO_FLAG_PRIVILEGED     // Catch: java.lang.NumberFormatException -> L132
            r18 = r28 & r16
            if (r18 == 0) goto L1bd
            r18 = r29 | 8
            r29 = r18
        L1bd:
            r0 = r0 | r15
            r0 = r0 | r16
            int r0 = ~r0
            r0 = r28 & r0
            r28 = r17
            r70 = r29
            r29 = r0
            goto L213
        L1ca:
            java.lang.String r0 = "system"
            java.lang.String r0 = r12.getAttributeValue(r14, r0)     // Catch: java.lang.NumberFormatException -> Lad9
            r15 = r0
            if (r15 == 0) goto L20b
            boolean r0 = r9.equalsIgnoreCase(r15)     // Catch: java.lang.NumberFormatException -> L1e6
            if (r0 == 0) goto L1dc
            r0 = 1
            goto L1dd
        L1dc:
            r0 = r3
        L1dd:
            r0 = r28 | r0
            r28 = r15
            r70 = r29
            r29 = r0
            goto L213
        L1e6:
            r0 = move-exception
            r16 = r8
            r81 = r9
            r9 = r13
            r17 = r15
            r18 = r37
            r19 = r38
            r14 = r41
            r3 = r49
            r8 = r68
            r13 = 5
            r43 = 0
            r15 = r7
            r37 = r10
            r10 = r66
            r7 = r67
            r67 = r2
            r11 = r5
            r2 = r6
            r6 = r48
            r5 = r4
            goto Lcca
        L20b:
            r0 = r28 | 1
            r28 = r15
            r70 = r29
            r29 = r0
        L213:
            java.lang.String r0 = "ft"
            r47 = r4
            r3 = 0
            long r16 = r12.getAttributeLongHex(r14, r0, r3)     // Catch: java.lang.NumberFormatException -> La73
            r30 = r16
            int r0 = (r30 > r3 ? 1 : (r30 == r3 ? 0 : -1))
            if (r0 != 0) goto L257
            java.lang.String r0 = "ts"
            long r16 = r12.getAttributeLong(r14, r0, r3)     // Catch: java.lang.NumberFormatException -> L22d
            r3 = r16
            goto L259
        L22d:
            r0 = move-exception
            r43 = r3
            r15 = r7
            r16 = r8
            r81 = r9
            r9 = r13
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r3 = r49
            r7 = r67
            r8 = r68
            r29 = r70
            r13 = 5
            r67 = r2
            r2 = r6
            r37 = r10
            r6 = r48
            r10 = r66
            r11 = r5
            r5 = r47
            goto Lcca
        L257:
            r3 = r30
        L259:
            java.lang.String r0 = "it"
            r30 = r3
            r3 = 0
            long r16 = r12.getAttributeLongHex(r14, r0, r3)     // Catch: java.lang.NumberFormatException -> La08
            r32 = r16
            java.lang.String r0 = "ut"
            long r16 = r12.getAttributeLongHex(r14, r0, r3)     // Catch: java.lang.NumberFormatException -> La08
            r34 = r16
            boolean r0 = com.android.server.pm.PackageManagerService.DEBUG_SETTINGS     // Catch: java.lang.NumberFormatException -> L9d1
            java.lang.String r15 = "PackageManager"
            if (r0 == 0) goto L2fa
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch: java.lang.NumberFormatException -> L2ce
            r0.<init>()     // Catch: java.lang.NumberFormatException -> L2ce
            java.lang.String r3 = "Reading package: "
            java.lang.StringBuilder r0 = r0.append(r3)     // Catch: java.lang.NumberFormatException -> L2ce
            java.lang.StringBuilder r0 = r0.append(r1)     // Catch: java.lang.NumberFormatException -> L2ce
            java.lang.String r3 = " userId="
            java.lang.StringBuilder r0 = r0.append(r3)     // Catch: java.lang.NumberFormatException -> L2ce
            r4 = r47
            java.lang.StringBuilder r0 = r0.append(r4)     // Catch: java.lang.NumberFormatException -> L2a4
            java.lang.String r3 = " sharedUserId="
            java.lang.StringBuilder r0 = r0.append(r3)     // Catch: java.lang.NumberFormatException -> L2a4
            r3 = r67
            java.lang.StringBuilder r0 = r0.append(r3)     // Catch: java.lang.NumberFormatException -> L307
            java.lang.String r0 = r0.toString()     // Catch: java.lang.NumberFormatException -> L307
            android.util.Log.v(r15, r0)     // Catch: java.lang.NumberFormatException -> L307
            goto L2fe
        L2a4:
            r0 = move-exception
            r3 = r67
            r67 = r2
            r2 = r6
            r15 = r7
            r16 = r8
            r81 = r9
            r9 = r13
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r6 = r48
            r8 = r68
            r29 = r70
            r13 = 5
            r43 = 0
            r7 = r3
            r37 = r10
            r3 = r49
            r10 = r66
            r11 = r5
            r5 = r4
            goto Lcca
        L2ce:
            r0 = move-exception
            r4 = r47
            r3 = r67
            r67 = r2
            r2 = r6
            r15 = r7
            r16 = r8
            r81 = r9
            r9 = r13
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r6 = r48
            r8 = r68
            r29 = r70
            r13 = 5
            r43 = 0
            r7 = r3
            r37 = r10
            r3 = r49
            r10 = r66
            r11 = r5
            r5 = r4
            goto Lcca
        L2fa:
            r4 = r47
            r3 = r67
        L2fe:
            if (r2 == 0) goto L32f
            java.lang.String r0 = r2.intern()     // Catch: java.lang.NumberFormatException -> L307
            r67 = r0
            goto L331
        L307:
            r0 = move-exception
            r67 = r2
            r2 = r6
            r15 = r7
            r16 = r8
            r81 = r9
            r9 = r13
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r6 = r48
            r8 = r68
            r29 = r70
            r13 = 5
            r43 = 0
            r7 = r3
            r37 = r10
            r3 = r49
            r10 = r66
            r11 = r5
            r5 = r4
            goto Lcca
        L32f:
            r67 = r2
        L331:
            if (r1 != 0) goto L3af
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch: java.lang.NumberFormatException -> L389
            r0.<init>()     // Catch: java.lang.NumberFormatException -> L389
            java.lang.String r2 = "Error in package manager settings: <package> has no name at "
            java.lang.StringBuilder r0 = r0.append(r2)     // Catch: java.lang.NumberFormatException -> L389
            java.lang.String r2 = r85.getPositionDescription()     // Catch: java.lang.NumberFormatException -> L389
            java.lang.StringBuilder r0 = r0.append(r2)     // Catch: java.lang.NumberFormatException -> L389
            java.lang.String r0 = r0.toString()     // Catch: java.lang.NumberFormatException -> L389
            r2 = 5
            com.android.server.pm.PackageManagerService.reportSettingsProblem(r2, r0)     // Catch: java.lang.NumberFormatException -> L363
            r77 = r3
            r46 = r4
            r81 = r9
            r9 = r13
            r12 = r30
            r14 = r34
            r6 = r48
            r3 = r49
            r43 = 0
            r4 = r1
            r1 = 5
            goto L8f5
        L363:
            r0 = move-exception
            r15 = r7
            r16 = r8
            r81 = r9
            r9 = r13
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r8 = r68
            r29 = r70
            r43 = 0
            r13 = r2
            r7 = r3
            r2 = r6
            r37 = r10
            r6 = r48
            r3 = r49
            r10 = r66
            r11 = r5
            r5 = r4
            goto Lcca
        L389:
            r0 = move-exception
            r2 = r6
            r15 = r7
            r16 = r8
            r81 = r9
            r9 = r13
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r6 = r48
            r8 = r68
            r29 = r70
            r13 = 5
            r43 = 0
            r7 = r3
            r37 = r10
            r3 = r49
            r10 = r66
            r11 = r5
            r5 = r4
            goto Lcca
        L3af:
            r2 = r68
            if (r2 != 0) goto L42f
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch: java.lang.NumberFormatException -> L40a
            r0.<init>()     // Catch: java.lang.NumberFormatException -> L40a
            java.lang.String r15 = "Error in package manager settings: <package> has no codePath at "
            java.lang.StringBuilder r0 = r0.append(r15)     // Catch: java.lang.NumberFormatException -> L40a
            java.lang.String r15 = r85.getPositionDescription()     // Catch: java.lang.NumberFormatException -> L40a
            java.lang.StringBuilder r0 = r0.append(r15)     // Catch: java.lang.NumberFormatException -> L40a
            java.lang.String r0 = r0.toString()     // Catch: java.lang.NumberFormatException -> L40a
            r15 = 5
            com.android.server.pm.PackageManagerService.reportSettingsProblem(r15, r0)     // Catch: java.lang.NumberFormatException -> L3e5
            r68 = r2
            r77 = r3
            r46 = r4
            r81 = r9
            r9 = r13
            r12 = r30
            r6 = r48
            r3 = r49
            r43 = 0
            r4 = r1
            r1 = r15
            r14 = r34
            goto L8f5
        L3e5:
            r0 = move-exception
            r16 = r8
            r81 = r9
            r9 = r13
            r13 = r15
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r29 = r70
            r43 = 0
            r8 = r2
            r2 = r6
            r15 = r7
            r37 = r10
            r6 = r48
            r10 = r66
            r7 = r3
            r11 = r5
            r3 = r49
            r5 = r4
            goto Lcca
        L40a:
            r0 = move-exception
            r15 = r7
            r16 = r8
            r81 = r9
            r9 = r13
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r29 = r70
            r13 = 5
            r43 = 0
            r8 = r2
            r7 = r3
            r2 = r6
            r37 = r10
            r6 = r48
            r3 = r49
            r10 = r66
            r11 = r5
            r5 = r4
            goto Lcca
        L42f:
            r17 = 5
            java.lang.String r0 = " pkg="
            r18 = r15
            java.lang.String r15 = "Reading package "
            if (r4 <= 0) goto L671
            java.lang.String r19 = r1.intern()     // Catch: java.lang.NumberFormatException -> L63b
            r46 = r15
            java.io.File r15 = new java.io.File     // Catch: java.lang.NumberFormatException -> L63b
            r15.<init>(r2)     // Catch: java.lang.NumberFormatException -> L63b
            r47 = 0
            r43 = 0
            r14 = r47
            r17 = 0
            r45 = r15
            r72 = r18
            r71 = r46
            r15 = r17
            r16 = 0
            r18 = 0
            r73 = r1
            r1 = r84
            r74 = r2
            r2 = r19
            r77 = r3
            r75 = r30
            r78 = r49
            r3 = r67
            r46 = r4
            r79 = r48
            r4 = r45
            r80 = r6
            r6 = r41
            r81 = r9
            r9 = r46
            r12 = r29
            r13 = r70
            r19 = r69
            com.android.server.pm.PackageSetting r1 = r1.addPackageLPw(r2, r3, r4, r5, r6, r7, r8, r9, r10, r12, r13, r14, r15, r16, r17, r18, r19)     // Catch: java.lang.NumberFormatException -> L60b
            boolean r2 = com.android.server.pm.PackageManagerService.DEBUG_SETTINGS     // Catch: java.lang.NumberFormatException -> L5d9
            if (r2 == 0) goto L50e
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.NumberFormatException -> L4e0
            r2.<init>()     // Catch: java.lang.NumberFormatException -> L4e0
            r3 = r71
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.NumberFormatException -> L4e0
            r4 = r73
            java.lang.StringBuilder r2 = r2.append(r4)     // Catch: java.lang.NumberFormatException -> L4b4
            java.lang.String r3 = ": userId="
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.NumberFormatException -> L4b4
            r6 = r46
            java.lang.StringBuilder r2 = r2.append(r6)     // Catch: java.lang.NumberFormatException -> L53e
            java.lang.StringBuilder r0 = r2.append(r0)     // Catch: java.lang.NumberFormatException -> L53e
            java.lang.StringBuilder r0 = r0.append(r1)     // Catch: java.lang.NumberFormatException -> L53e
            java.lang.String r0 = r0.toString()     // Catch: java.lang.NumberFormatException -> L53e
            r2 = r72
            android.util.Log.i(r2, r0)     // Catch: java.lang.NumberFormatException -> L53e
            goto L512
        L4b4:
            r0 = move-exception
            r6 = r46
            r13 = 5
            r9 = r84
            r36 = r1
            r1 = r4
            r15 = r7
            r16 = r8
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r29 = r70
            r8 = r74
            r30 = r75
            r7 = r77
            r3 = r78
            r2 = r80
            r37 = r10
            r10 = r66
            r11 = r5
            r5 = r6
            r6 = r79
            goto Lcca
        L4e0:
            r0 = move-exception
            r6 = r46
            r4 = r73
            r13 = 5
            r9 = r84
            r36 = r1
            r1 = r4
            r15 = r7
            r16 = r8
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r29 = r70
            r8 = r74
            r30 = r75
            r7 = r77
            r3 = r78
            r2 = r80
            r37 = r10
            r10 = r66
            r11 = r5
            r5 = r6
            r6 = r79
            goto Lcca
        L50e:
            r6 = r46
            r4 = r73
        L512:
            if (r1 != 0) goto L568
            r0 = 6
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.NumberFormatException -> L53e
            r2.<init>()     // Catch: java.lang.NumberFormatException -> L53e
            java.lang.String r3 = "Failure adding uid "
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.NumberFormatException -> L53e
            java.lang.StringBuilder r2 = r2.append(r6)     // Catch: java.lang.NumberFormatException -> L53e
            java.lang.String r3 = " while parsing settings at "
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.NumberFormatException -> L53e
            java.lang.String r3 = r85.getPositionDescription()     // Catch: java.lang.NumberFormatException -> L53e
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.NumberFormatException -> L53e
            java.lang.String r2 = r2.toString()     // Catch: java.lang.NumberFormatException -> L53e
            com.android.server.pm.PackageManagerService.reportSettingsProblem(r0, r2)     // Catch: java.lang.NumberFormatException -> L53e
            r14 = r34
            r12 = r75
            goto L572
        L53e:
            r0 = move-exception
            r13 = 5
            r9 = r84
            r36 = r1
            r1 = r4
            r15 = r7
            r16 = r8
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r29 = r70
            r8 = r74
            r30 = r75
            r7 = r77
            r3 = r78
            r2 = r80
            r37 = r10
            r10 = r66
            r11 = r5
            r5 = r6
            r6 = r79
            goto Lcca
        L568:
            r12 = r75
            r1.setLastModifiedTime(r12)     // Catch: java.lang.NumberFormatException -> L5ad
            r14 = r34
            r1.setLastUpdateTime(r14)     // Catch: java.lang.NumberFormatException -> L581
        L572:
            r9 = r84
            r36 = r1
            r46 = r6
            r68 = r74
            r3 = r78
            r6 = r79
            r1 = 5
            goto L8f5
        L581:
            r0 = move-exception
            r9 = r84
            r36 = r1
            r1 = r4
            r16 = r8
            r30 = r12
            r34 = r14
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r29 = r70
            r8 = r74
            r3 = r78
            r2 = r80
            r13 = 5
            r15 = r7
            r37 = r10
            r10 = r66
            r7 = r77
            r11 = r5
            r5 = r6
            r6 = r79
            goto Lcca
        L5ad:
            r0 = move-exception
            r14 = r34
            r9 = r84
            r36 = r1
            r1 = r4
            r16 = r8
            r30 = r12
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r29 = r70
            r8 = r74
            r3 = r78
            r2 = r80
            r13 = 5
            r15 = r7
            r37 = r10
            r10 = r66
            r7 = r77
            r11 = r5
            r5 = r6
            r6 = r79
            goto Lcca
        L5d9:
            r0 = move-exception
            r14 = r34
            r6 = r46
            r4 = r73
            r12 = r75
            r9 = r84
            r36 = r1
            r1 = r4
            r16 = r8
            r30 = r12
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r29 = r70
            r8 = r74
            r3 = r78
            r2 = r80
            r13 = 5
            r15 = r7
            r37 = r10
            r10 = r66
            r7 = r77
            r11 = r5
            r5 = r6
            r6 = r79
            goto Lcca
        L60b:
            r0 = move-exception
            r14 = r34
            r6 = r46
            r4 = r73
            r12 = r75
            r9 = r84
            r1 = r4
            r16 = r8
            r30 = r12
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r29 = r70
            r8 = r74
            r3 = r78
            r2 = r80
            r13 = 5
            r15 = r7
            r37 = r10
            r10 = r66
            r7 = r77
            r11 = r5
            r5 = r6
            r6 = r79
            goto Lcca
        L63b:
            r0 = move-exception
            r74 = r2
            r77 = r3
            r80 = r6
            r81 = r9
            r12 = r30
            r14 = r34
            r43 = 0
            r6 = r4
            r4 = r1
            r9 = r84
            r16 = r8
            r13 = r17
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r3 = r49
            r29 = r70
            r8 = r74
            r2 = r80
            r15 = r7
            r37 = r10
            r10 = r66
            r7 = r77
            r11 = r5
            r5 = r6
            r6 = r48
            goto Lcca
        L671:
            r74 = r2
            r77 = r3
            r80 = r6
            r81 = r9
            r3 = r15
            r2 = r18
            r12 = r30
            r14 = r34
            r79 = r48
            r78 = r49
            r43 = 0
            r6 = r4
            r4 = r1
            r1 = r77
            if (r1 == 0) goto L8b9
            if (r1 <= 0) goto L7e9
            com.android.server.pm.PackageSetting r9 = new com.android.server.pm.PackageSetting     // Catch: java.lang.NumberFormatException -> L7bb
            java.lang.String r48 = r4.intern()     // Catch: java.lang.NumberFormatException -> L7bb
            r46 = r6
            java.io.File r6 = new java.io.File     // Catch: java.lang.NumberFormatException -> L78f
            r72 = r2
            r2 = r74
            r6.<init>(r2)     // Catch: java.lang.NumberFormatException -> L763
            r60 = 0
            r61 = 0
            r62 = 0
            r63 = 0
            r64 = 0
            r47 = r9
            r49 = r67
            r50 = r6
            r51 = r5
            r52 = r41
            r53 = r7
            r54 = r8
            r55 = r10
            r57 = r29
            r58 = r70
            r59 = r1
            r65 = r69
            r47.<init>(r48, r49, r50, r51, r52, r53, r54, r55, r57, r58, r59, r60, r61, r62, r63, r64, r65)     // Catch: java.lang.NumberFormatException -> L763
            r6 = r9
            r6.setLastModifiedTime(r12)     // Catch: java.lang.NumberFormatException -> L735
            r6.setLastUpdateTime(r14)     // Catch: java.lang.NumberFormatException -> L735
            r9 = r84
            r68 = r2
            com.android.server.utils.WatchedArrayList<com.android.server.pm.PackageSetting> r2 = r9.mPendingPackages     // Catch: java.lang.NumberFormatException -> L70b
            r2.add(r6)     // Catch: java.lang.NumberFormatException -> L70b
            boolean r2 = com.android.server.pm.PackageManagerService.DEBUG_SETTINGS     // Catch: java.lang.NumberFormatException -> L70b
            if (r2 == 0) goto L700
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.NumberFormatException -> L70b
            r2.<init>()     // Catch: java.lang.NumberFormatException -> L70b
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.NumberFormatException -> L70b
            java.lang.StringBuilder r2 = r2.append(r4)     // Catch: java.lang.NumberFormatException -> L70b
            java.lang.String r3 = ": sharedUserId="
            java.lang.StringBuilder r2 = r2.append(r3)     // Catch: java.lang.NumberFormatException -> L70b
            java.lang.StringBuilder r2 = r2.append(r1)     // Catch: java.lang.NumberFormatException -> L70b
            java.lang.StringBuilder r0 = r2.append(r0)     // Catch: java.lang.NumberFormatException -> L70b
            java.lang.StringBuilder r0 = r0.append(r6)     // Catch: java.lang.NumberFormatException -> L70b
            java.lang.String r0 = r0.toString()     // Catch: java.lang.NumberFormatException -> L70b
            r2 = r72
            android.util.Log.i(r2, r0)     // Catch: java.lang.NumberFormatException -> L70b
        L700:
            r77 = r1
            r36 = r6
            r3 = r78
            r6 = r79
            r1 = 5
            goto L8f5
        L70b:
            r0 = move-exception
            r36 = r6
            r16 = r8
            r30 = r12
            r34 = r14
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r8 = r68
            r29 = r70
            r3 = r78
            r6 = r79
            r2 = r80
            r13 = 5
            r15 = r7
            r37 = r10
            r10 = r66
            r7 = r1
            r1 = r4
            r11 = r5
            r5 = r46
            goto Lcca
        L735:
            r0 = move-exception
            r9 = r84
            r68 = r2
            r36 = r6
            r16 = r8
            r30 = r12
            r34 = r14
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r8 = r68
            r29 = r70
            r3 = r78
            r6 = r79
            r2 = r80
            r13 = 5
            r15 = r7
            r37 = r10
            r10 = r66
            r7 = r1
            r1 = r4
            r11 = r5
            r5 = r46
            goto Lcca
        L763:
            r0 = move-exception
            r9 = r84
            r68 = r2
            r16 = r8
            r30 = r12
            r34 = r14
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r8 = r68
            r29 = r70
            r3 = r78
            r6 = r79
            r2 = r80
            r13 = 5
            r15 = r7
            r37 = r10
            r10 = r66
            r7 = r1
            r1 = r4
            r11 = r5
            r5 = r46
            goto Lcca
        L78f:
            r0 = move-exception
            r9 = r84
            r68 = r74
            r16 = r8
            r30 = r12
            r34 = r14
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r8 = r68
            r29 = r70
            r3 = r78
            r6 = r79
            r2 = r80
            r13 = 5
            r15 = r7
            r37 = r10
            r10 = r66
            r7 = r1
            r1 = r4
            r11 = r5
            r5 = r46
            goto Lcca
        L7bb:
            r0 = move-exception
            r9 = r84
            r46 = r6
            r68 = r74
            r16 = r8
            r30 = r12
            r34 = r14
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r8 = r68
            r29 = r70
            r3 = r78
            r6 = r79
            r2 = r80
            r13 = 5
            r15 = r7
            r37 = r10
            r10 = r66
            r7 = r1
            r1 = r4
            r11 = r5
            r5 = r46
            goto Lcca
        L7e9:
            r9 = r84
            r46 = r6
            r68 = r74
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch: java.lang.NumberFormatException -> L891
            r0.<init>()     // Catch: java.lang.NumberFormatException -> L891
            r3 = r78
            java.lang.StringBuilder r0 = r0.append(r3)     // Catch: java.lang.NumberFormatException -> L86b
            java.lang.StringBuilder r0 = r0.append(r4)     // Catch: java.lang.NumberFormatException -> L86b
            java.lang.String r2 = " has bad sharedId "
            java.lang.StringBuilder r0 = r0.append(r2)     // Catch: java.lang.NumberFormatException -> L86b
            java.lang.StringBuilder r0 = r0.append(r1)     // Catch: java.lang.NumberFormatException -> L86b
            r6 = r79
            java.lang.StringBuilder r0 = r0.append(r6)     // Catch: java.lang.NumberFormatException -> L847
            java.lang.String r2 = r85.getPositionDescription()     // Catch: java.lang.NumberFormatException -> L847
            java.lang.StringBuilder r0 = r0.append(r2)     // Catch: java.lang.NumberFormatException -> L847
            java.lang.String r0 = r0.toString()     // Catch: java.lang.NumberFormatException -> L847
            r2 = 5
            com.android.server.pm.PackageManagerService.reportSettingsProblem(r2, r0)     // Catch: java.lang.NumberFormatException -> L823
            r77 = r1
            r1 = r2
            goto L8f5
        L823:
            r0 = move-exception
            r16 = r8
            r30 = r12
            r34 = r14
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r8 = r68
            r29 = r70
            r13 = r2
            r15 = r7
            r37 = r10
            r10 = r66
            r2 = r80
            r7 = r1
            r1 = r4
            r11 = r5
            r5 = r46
            goto Lcca
        L847:
            r0 = move-exception
            r16 = r8
            r30 = r12
            r34 = r14
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r8 = r68
            r29 = r70
            r2 = r80
            r13 = 5
            r15 = r7
            r37 = r10
            r10 = r66
            r7 = r1
            r1 = r4
            r11 = r5
            r5 = r46
            goto Lcca
        L86b:
            r0 = move-exception
            r6 = r79
            r16 = r8
            r30 = r12
            r34 = r14
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r8 = r68
            r29 = r70
            r2 = r80
            r13 = 5
            r15 = r7
            r37 = r10
            r10 = r66
            r7 = r1
            r1 = r4
            r11 = r5
            r5 = r46
            goto Lcca
        L891:
            r0 = move-exception
            r3 = r78
            r6 = r79
            r16 = r8
            r30 = r12
            r34 = r14
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r8 = r68
            r29 = r70
            r2 = r80
            r13 = 5
            r15 = r7
            r37 = r10
            r10 = r66
            r7 = r1
            r1 = r4
            r11 = r5
            r5 = r46
            goto Lcca
        L8b9:
            r2 = 5
            r9 = r84
            r46 = r6
            r68 = r74
            r3 = r78
            r6 = r79
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch: java.lang.NumberFormatException -> L9a9
            r0.<init>()     // Catch: java.lang.NumberFormatException -> L9a9
            java.lang.StringBuilder r0 = r0.append(r3)     // Catch: java.lang.NumberFormatException -> L9a9
            java.lang.StringBuilder r0 = r0.append(r4)     // Catch: java.lang.NumberFormatException -> L9a9
            r2 = r80
            java.lang.StringBuilder r0 = r0.append(r2)     // Catch: java.lang.NumberFormatException -> L98f
            r77 = r1
            r1 = r46
            java.lang.StringBuilder r0 = r0.append(r1)     // Catch: java.lang.NumberFormatException -> L96a
            java.lang.StringBuilder r0 = r0.append(r6)     // Catch: java.lang.NumberFormatException -> L96a
            r46 = r1
            java.lang.String r1 = r85.getPositionDescription()     // Catch: java.lang.NumberFormatException -> L947
            java.lang.StringBuilder r0 = r0.append(r1)     // Catch: java.lang.NumberFormatException -> L947
            java.lang.String r0 = r0.toString()     // Catch: java.lang.NumberFormatException -> L947
            r1 = 5
            com.android.server.pm.PackageManagerService.reportSettingsProblem(r1, r0)     // Catch: java.lang.NumberFormatException -> L924
        L8f5:
            r1 = r4
            r79 = r6
            r16 = r8
            r30 = r12
            r34 = r14
            r2 = r20
            r4 = r21
            r12 = r23
            r14 = r25
            r13 = r26
            r8 = r39
            r9 = r40
            r6 = r41
            r25 = r46
            r26 = r77
            r15 = r7
            r7 = r36
            r36 = r68
            r82 = r10
            r11 = r5
            r10 = r22
            r5 = r27
            r27 = r38
            r38 = r82
            goto Ld1d
        L924:
            r0 = move-exception
            r16 = r8
            r30 = r12
            r34 = r14
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r8 = r68
            r29 = r70
            r13 = r1
            r1 = r4
            r15 = r7
            r37 = r10
            r10 = r66
            r7 = r77
            r11 = r5
            r5 = r46
            goto Lcca
        L947:
            r0 = move-exception
            r1 = r4
            r16 = r8
            r30 = r12
            r34 = r14
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r8 = r68
            r29 = r70
            r13 = 5
            r15 = r7
            r37 = r10
            r10 = r66
            r7 = r77
            r11 = r5
            r5 = r46
            goto Lcca
        L96a:
            r0 = move-exception
            r46 = r1
            r1 = r4
            r16 = r8
            r30 = r12
            r34 = r14
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r8 = r68
            r29 = r70
            r13 = 5
            r15 = r7
            r37 = r10
            r10 = r66
            r7 = r77
            r11 = r5
            r5 = r46
            goto Lcca
        L98f:
            r0 = move-exception
            r77 = r1
            r1 = r4
            r16 = r8
            r30 = r12
            r34 = r14
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r8 = r68
            r29 = r70
            r13 = 5
            goto L9c5
        L9a9:
            r0 = move-exception
            r77 = r1
            r1 = r2
            r2 = r80
            r16 = r8
            r30 = r12
            r34 = r14
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r8 = r68
            r29 = r70
            r13 = r1
            r1 = r4
        L9c5:
            r15 = r7
            r37 = r10
            r10 = r66
            r7 = r77
            r11 = r5
            r5 = r46
            goto Lcca
        L9d1:
            r0 = move-exception
            r45 = r2
            r43 = r3
            r2 = r6
            r81 = r9
            r9 = r13
            r12 = r30
            r14 = r34
            r46 = r47
            r6 = r48
            r3 = r49
            r77 = r67
            r4 = r1
            r1 = 5
            r16 = r8
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r67 = r45
            r8 = r68
            r29 = r70
            r13 = r1
            r1 = r4
            r15 = r7
            r37 = r10
            r10 = r66
            r7 = r77
            r11 = r5
            r5 = r46
            goto Lcca
        La08:
            r0 = move-exception
            r45 = r2
            r43 = r3
            r2 = r6
            r81 = r9
            r9 = r13
            r12 = r30
            r46 = r47
            r6 = r48
            r3 = r49
            r77 = r67
            r4 = r1
            r1 = 5
            r15 = r7
            r16 = r8
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r67 = r45
            r8 = r68
            r29 = r70
            r7 = r77
            r13 = r1
            r1 = r4
            r37 = r10
            r10 = r66
            r11 = r5
            r5 = r46
            goto Lcca
        La3d:
            r0 = move-exception
            r45 = r2
            r2 = r6
            r81 = r9
            r9 = r13
            r46 = r47
            r6 = r48
            r77 = r67
            r43 = 0
            r12 = r3
            r3 = r49
            r4 = r1
            r1 = 5
            r15 = r7
            r16 = r8
            r30 = r12
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r67 = r45
            r8 = r68
            r29 = r70
            r7 = r77
            r13 = r1
            r1 = r4
            r37 = r10
            r10 = r66
            r11 = r5
            r5 = r46
            goto Lcca
        La73:
            r0 = move-exception
            r45 = r2
            r43 = r3
            r2 = r6
            r81 = r9
            r9 = r13
            r46 = r47
            r6 = r48
            r3 = r49
            r77 = r67
            r4 = r1
            r1 = 5
            r13 = r1
            r1 = r4
            r15 = r7
            r16 = r8
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r67 = r45
            r8 = r68
            r29 = r70
            r7 = r77
            r37 = r10
            r10 = r66
            r11 = r5
            r5 = r46
            goto Lcca
        Laa6:
            r0 = move-exception
            r45 = r2
            r46 = r4
            r2 = r6
            r81 = r9
            r9 = r13
            r6 = r48
            r3 = r49
            r77 = r67
            r43 = 0
            r4 = r1
            r1 = 5
            r13 = r1
            r1 = r4
            r15 = r7
            r16 = r8
            r17 = r28
            r28 = r29
            r18 = r37
            r19 = r38
            r14 = r41
            r67 = r45
            r8 = r68
            r29 = r70
            r7 = r77
            r37 = r10
            r10 = r66
            r11 = r5
            r5 = r46
            goto Lcca
        Lad9:
            r0 = move-exception
            r45 = r2
            r46 = r4
            r2 = r6
            r81 = r9
            r9 = r13
            r6 = r48
            r3 = r49
            r77 = r67
            r43 = 0
            r4 = r1
            r1 = 5
            r13 = r1
            r1 = r4
            r15 = r7
            r16 = r8
            r18 = r37
            r19 = r38
            r14 = r41
            r67 = r45
            r8 = r68
            r7 = r77
            r37 = r10
            r10 = r66
            r11 = r5
            r5 = r46
            goto Lcca
        Lb06:
            r0 = move-exception
            r45 = r2
            r46 = r4
            r2 = r6
            r81 = r9
            r9 = r13
            r6 = r48
            r3 = r49
            r77 = r67
            r43 = 0
            r4 = r1
            r1 = 5
            r13 = r1
            r1 = r4
            r15 = r7
            r16 = r8
            r18 = r37
            r14 = r41
            r67 = r45
            r8 = r68
            r7 = r77
            r37 = r10
            r10 = r66
            r11 = r5
            r5 = r46
            goto Lcca
        Lb31:
            r0 = move-exception
            r45 = r2
            r46 = r4
            r2 = r6
            r81 = r9
            r9 = r13
            r6 = r48
            r3 = r49
            r77 = r67
            r43 = 0
            r4 = r1
            r1 = 5
            r13 = r1
            r1 = r4
            r15 = r7
            r16 = r8
            r37 = r10
            r14 = r41
            r67 = r45
            r10 = r66
            r8 = r68
            r7 = r77
            r11 = r5
            r5 = r46
            goto Lcca
        Lb5a:
            r0 = move-exception
            r45 = r2
            r43 = r3
            r2 = r6
            r81 = r9
            r46 = r10
            r9 = r13
            r6 = r48
            r3 = r49
            r77 = r67
            r4 = r1
            r1 = 5
            r13 = r1
            r1 = r4
            r11 = r5
            r15 = r7
            r16 = r8
            r14 = r41
            r67 = r45
            r5 = r46
            r10 = r66
            r8 = r68
            r7 = r77
            goto Lcca
        Lb81:
            r0 = move-exception
            r45 = r2
            r2 = r6
            r81 = r9
            r46 = r10
            r9 = r13
            r77 = r67
            r43 = 0
            r6 = r4
            r4 = r1
            r1 = 5
            r13 = r1
            r1 = r4
            r11 = r5
            r15 = r7
            r16 = r8
            r14 = r41
            r67 = r45
            r5 = r46
            r10 = r66
            r8 = r68
            r7 = r77
            goto Lcca
        Lba5:
            r0 = move-exception
            r45 = r2
            r2 = r6
            r81 = r9
            r46 = r10
            r9 = r13
            r77 = r67
            r43 = 0
            r6 = r4
            r4 = r1
            r1 = 5
            r13 = r1
            r1 = r4
            r15 = r7
            r16 = r8
            r14 = r11
            r67 = r45
            r10 = r66
            r8 = r68
            r7 = r77
            r11 = r5
            r5 = r46
            goto Lcca
        Lbc8:
            r0 = move-exception
            r45 = r2
            r2 = r6
            r68 = r8
            r81 = r9
            r46 = r10
            r9 = r13
            r77 = r67
            r43 = 0
            r6 = r4
            r4 = r1
            r1 = 5
            r13 = r1
            r1 = r4
            r15 = r7
            r14 = r11
            r67 = r45
            r10 = r66
            r7 = r77
            r11 = r5
            r5 = r46
            goto Lcca
        Lbe9:
            r0 = move-exception
            r45 = r2
            r2 = r6
            r77 = r7
            r68 = r8
            r81 = r9
            r46 = r10
            r9 = r13
            r43 = 0
            r6 = r4
            r4 = r1
            r1 = 5
            r13 = r1
            r1 = r4
            r14 = r11
            r15 = r42
            r67 = r45
            r10 = r66
            r11 = r5
            r5 = r46
            goto Lcca
        Lc09:
            r0 = move-exception
            r45 = r2
            r2 = r6
            r77 = r7
            r68 = r8
            r81 = r9
            r46 = r10
            r9 = r13
            r43 = 0
            r6 = r4
            r4 = r1
            r1 = 5
            r13 = r1
            r1 = r4
            r11 = r5
            r14 = r41
            r15 = r42
            r67 = r45
            r5 = r46
            r10 = r66
            goto Lcca
        Lc2a:
            r0 = move-exception
            r45 = r2
            r46 = r5
            r2 = r6
            r77 = r7
            r68 = r8
            r81 = r9
            r9 = r13
            r43 = 0
            r6 = r4
            r4 = r1
            r1 = 5
            r13 = r1
            r1 = r4
            r14 = r41
            r15 = r42
            r67 = r45
            r10 = r66
            goto Lcca
        Lc48:
            r0 = move-exception
            r45 = r2
            r46 = r5
            r2 = r6
            r77 = r7
            r68 = r8
            r81 = r9
            r9 = r13
            r43 = 0
            r6 = r4
            r4 = r1
            r1 = 5
            r13 = r1
            r1 = r4
            r14 = r41
            r15 = r42
            r67 = r45
            goto Lcca
        Lc64:
            r0 = move-exception
            r45 = r2
            r46 = r5
            r2 = r6
            r77 = r7
            r81 = r9
            r9 = r13
            r43 = 0
            r6 = r4
            r4 = r1
            r1 = 5
            r13 = r1
            r1 = r4
            r14 = r41
            r15 = r42
            r67 = r45
            goto Lcca
        Lc7e:
            r0 = move-exception
            r45 = r2
            r46 = r5
            r2 = r6
            r81 = r9
            r9 = r13
            r43 = 0
            r6 = r4
            r4 = r1
            r1 = 5
            r13 = r1
            r1 = r4
            r14 = r41
            r15 = r42
            r67 = r45
            goto Lcca
        Lc95:
            r0 = move-exception
            r45 = r2
            r2 = r6
            r81 = r9
            r9 = r13
            r43 = 0
            r6 = r4
            r4 = r1
            r14 = r41
            r15 = r42
            r67 = r45
            r13 = 5
            goto Lcca
        Lca8:
            r0 = move-exception
            r12 = r2
            r2 = r6
            r81 = r9
            r9 = r13
            r13 = 5
            r43 = 0
            r6 = r4
            r4 = r1
            r67 = r12
            r14 = r41
            r15 = r42
            goto Lcca
        Lcba:
            r0 = move-exception
            r12 = r2
            r2 = r6
            r81 = r9
            r9 = r13
            r13 = 5
            r43 = 0
            r6 = r4
            r67 = r12
            r14 = r41
            r15 = r42
        Lcca:
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.StringBuilder r4 = r4.append(r3)
            java.lang.StringBuilder r4 = r4.append(r1)
            java.lang.StringBuilder r2 = r4.append(r2)
            java.lang.StringBuilder r2 = r2.append(r5)
            java.lang.StringBuilder r2 = r2.append(r6)
            java.lang.String r4 = r85.getPositionDescription()
            java.lang.StringBuilder r2 = r2.append(r4)
            java.lang.String r2 = r2.toString()
            com.android.server.pm.PackageManagerService.reportSettingsProblem(r13, r2)
            r79 = r6
            r66 = r10
            r6 = r14
            r2 = r20
            r4 = r21
            r10 = r22
            r12 = r23
            r14 = r25
            r13 = r26
            r70 = r29
            r9 = r40
            r25 = r5
            r26 = r7
            r5 = r27
            r29 = r28
            r7 = r36
            r36 = r8
            r28 = r17
            r27 = r19
            r8 = r39
            r38 = r37
            r37 = r18
        Ld1d:
            if (r7 == 0) goto L1063
            r17 = r12
            r18 = r10
            r19 = r37
            r20 = r27
            r21 = r2
            r22 = r4
            r23 = r24
            r40 = r2
            com.android.server.pm.InstallSource r2 = com.android.server.pm.InstallSource.create(r17, r18, r19, r20, r21, r22, r23)
            com.android.server.pm.PackageSetting r0 = r7.setInstallSource(r2)
            com.android.server.pm.PackageSetting r0 = r0.setVolumeUuid(r14)
            com.android.server.pm.PackageSetting r0 = r0.setCategoryOverride(r5)
            com.android.server.pm.PackageSetting r0 = r0.setLegacyNativeLibraryPath(r11)
            com.android.server.pm.PackageSetting r0 = r0.setPrimaryCpuAbi(r6)
            com.android.server.pm.PackageSetting r0 = r0.setSecondaryCpuAbi(r15)
            com.android.server.pm.PackageSetting r0 = r0.setUpdateAvailable(r13)
            com.android.server.pm.PackageSetting r0 = r0.setForceQueryableOverride(r8)
            r0.setLoadingProgress(r9)
            java.lang.String r0 = "enabled"
            r17 = r4
            r18 = r5
            r19 = r6
            r5 = 0
            r4 = r85
            java.lang.String r6 = r4.getAttributeValue(r5, r0)
            if (r6 == 0) goto Ldd1
            int r0 = java.lang.Integer.parseInt(r6)     // Catch: java.lang.NumberFormatException -> Ld74
            r20 = r8
            r8 = 0
            r7.setEnabled(r0, r8, r5)     // Catch: java.lang.NumberFormatException -> Ld72
        Ld71:
            goto Ldd8
        Ld72:
            r0 = move-exception
            goto Ld78
        Ld74:
            r0 = move-exception
            r20 = r8
            r8 = 0
        Ld78:
            r5 = r81
            boolean r5 = r6.equalsIgnoreCase(r5)
            if (r5 == 0) goto Ld86
            r3 = 0
            r5 = 1
            r7.setEnabled(r5, r8, r3)
            goto Ld71
        Ld86:
            r5 = 0
            java.lang.String r5 = "false"
            boolean r5 = r6.equalsIgnoreCase(r5)
            if (r5 == 0) goto Ld95
            r3 = 2
            r5 = 0
            r7.setEnabled(r3, r8, r5)
            goto Ld71
        Ld95:
            r5 = 0
            java.lang.String r5 = "default"
            boolean r5 = r6.equalsIgnoreCase(r5)
            if (r5 == 0) goto Lda3
            r3 = 0
            r7.setEnabled(r8, r8, r3)
            goto Ld71
        Lda3:
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.StringBuilder r3 = r5.append(r3)
            java.lang.StringBuilder r3 = r3.append(r1)
            java.lang.String r5 = " has bad enabled value: "
            java.lang.StringBuilder r3 = r3.append(r5)
            java.lang.StringBuilder r3 = r3.append(r6)
            r5 = r79
            java.lang.StringBuilder r3 = r3.append(r5)
            java.lang.String r5 = r85.getPositionDescription()
            java.lang.StringBuilder r3 = r3.append(r5)
            java.lang.String r3 = r3.toString()
            r5 = 5
            com.android.server.pm.PackageManagerService.reportSettingsProblem(r5, r3)
            goto Ld71
        Ldd1:
            r20 = r8
            r8 = 0
            r3 = 0
            r7.setEnabled(r8, r8, r3)
        Ldd8:
            r3 = r84
            r5 = r9
            r3.addInstallerPackageNames(r2)
            int r0 = r85.getDepth()
        Lde2:
            int r9 = r85.next()
            r21 = r9
            r8 = 1
            if (r9 == r8) goto L1042
            r8 = 3
            r9 = r21
            if (r9 != r8) goto Le05
            int r8 = r85.getDepth()
            if (r8 <= r0) goto Ldf7
            goto Le05
        Ldf7:
            r21 = r0
            r22 = r1
            r23 = r2
            r41 = r5
            r42 = r6
            r45 = r9
            goto L104e
        Le05:
            r8 = 3
            if (r9 == r8) goto L102e
            r8 = 4
            if (r9 != r8) goto Le0d
            r8 = 0
            goto Lde2
        Le0d:
            java.lang.String r8 = r85.getName()
            r21 = r0
            java.lang.String r0 = "disabled-components"
            boolean r0 = r8.equals(r0)
            if (r0 == 0) goto Le2e
            r22 = r1
            r1 = 0
            r3.readDisabledComponentsLPw(r7, r4, r1)
            r23 = r2
            r41 = r5
            r42 = r6
            r45 = r9
            r1 = 5
            r47 = 1
            goto L1021
        Le2e:
            r22 = r1
            r1 = 0
            java.lang.String r0 = "enabled-components"
            boolean r0 = r8.equals(r0)
            if (r0 == 0) goto Le49
            r3.readEnabledComponentsLPw(r7, r4, r1)
            r23 = r2
            r41 = r5
            r42 = r6
            r45 = r9
            r1 = 5
            r47 = 1
            goto L1021
        Le49:
            java.lang.String r0 = "sigs"
            boolean r0 = r8.equals(r0)
            if (r0 == 0) goto Le6c
            com.android.server.pm.PackageSignatures r0 = r7.getSignatures()
            com.android.server.utils.WatchedArrayList<android.content.pm.Signature> r1 = r3.mPastSignatures
            java.util.ArrayList r1 = r1.untrackedStorage()
            r0.readXml(r4, r1)
            r23 = r2
            r41 = r5
            r42 = r6
            r45 = r9
            r1 = 5
            r47 = 1
            goto L1021
        Le6c:
            java.lang.String r0 = "perms"
            boolean r0 = r8.equals(r0)
            if (r0 == 0) goto Leaa
            boolean r0 = r7.hasSharedUser()
            if (r0 == 0) goto Le8e
        Le7c:
            int r0 = r7.getSharedUserAppId()
            com.android.server.pm.SettingBase r0 = r3.getSettingLPr(r0)
            if (r0 == 0) goto Le8b
            com.android.server.pm.permission.LegacyPermissionState r1 = r0.getLegacyPermissionState()
            goto Le8c
        Le8b:
            r1 = 0
        Le8c:
            r0 = r1
            goto Le92
        Le8e:
            com.android.server.pm.permission.LegacyPermissionState r0 = r7.getLegacyPermissionState()
        Le92:
            if (r0 == 0) goto Le9d
            r1 = r86
            r3.readInstallPermissionsLPr(r4, r0, r1)
            r1 = 1
            r7.setInstallPermissionsFixed(r1)
        Le9d:
            r23 = r2
            r41 = r5
            r42 = r6
            r45 = r9
            r1 = 5
            r47 = 1
            goto L1021
        Leaa:
            java.lang.String r0 = "proper-signing-keyset"
            boolean r0 = r8.equals(r0)
            java.lang.String r1 = "identifier"
            if (r0 == 0) goto Lf01
            r23 = r2
            r2 = 0
            long r0 = r4.getAttributeLong(r2, r1)
            com.android.server.utils.WatchedArrayMap<java.lang.Long, java.lang.Integer> r2 = r3.mKeySetRefs
            r41 = r5
            java.lang.Long r5 = java.lang.Long.valueOf(r0)
            java.lang.Object r2 = r2.get(r5)
            java.lang.Integer r2 = (java.lang.Integer) r2
            if (r2 == 0) goto Lee6
            com.android.server.utils.WatchedArrayMap<java.lang.Long, java.lang.Integer> r5 = r3.mKeySetRefs
            r42 = r6
            java.lang.Long r6 = java.lang.Long.valueOf(r0)
            int r45 = r2.intValue()
            r46 = 1
            int r45 = r45 + 1
            r47 = r2
            java.lang.Integer r2 = java.lang.Integer.valueOf(r45)
            r5.put(r6, r2)
            goto Lef9
        Lee6:
            r47 = r2
            r42 = r6
            r46 = 1
            com.android.server.utils.WatchedArrayMap<java.lang.Long, java.lang.Integer> r2 = r3.mKeySetRefs
            java.lang.Long r5 = java.lang.Long.valueOf(r0)
            java.lang.Integer r6 = java.lang.Integer.valueOf(r46)
            r2.put(r5, r6)
        Lef9:
            com.android.server.pm.PackageKeySetData r2 = r7.getKeySetData()
            r2.setProperSigningKeySet(r0)
            goto Lf10
        Lf01:
            r23 = r2
            r41 = r5
            r42 = r6
            java.lang.String r0 = "signing-keyset"
            boolean r0 = r8.equals(r0)
            if (r0 == 0) goto Lf17
        Lf10:
            r45 = r9
            r1 = 5
            r47 = 1
            goto L1021
        Lf17:
            java.lang.String r0 = "upgrade-keyset"
            boolean r0 = r8.equals(r0)
            if (r0 == 0) goto Lf33
            r2 = 0
            long r0 = r4.getAttributeLong(r2, r1)
            com.android.server.pm.PackageKeySetData r5 = r7.getKeySetData()
            r5.addUpgradeKeySetById(r0)
            r45 = r9
            r1 = 5
            r47 = 1
            goto L1021
        Lf33:
            r2 = 0
            java.lang.String r0 = "defined-keyset"
            boolean r0 = r8.equals(r0)
            if (r0 == 0) goto Lf8b
            long r0 = r4.getAttributeLong(r2, r1)
            java.lang.String r5 = "alias"
            java.lang.String r5 = r4.getAttributeValue(r2, r5)
            com.android.server.utils.WatchedArrayMap<java.lang.Long, java.lang.Integer> r6 = r3.mKeySetRefs
            java.lang.Long r2 = java.lang.Long.valueOf(r0)
            java.lang.Object r2 = r6.get(r2)
            java.lang.Integer r2 = (java.lang.Integer) r2
            if (r2 == 0) goto Lf6e
            com.android.server.utils.WatchedArrayMap<java.lang.Long, java.lang.Integer> r6 = r3.mKeySetRefs
            r45 = r9
            java.lang.Long r9 = java.lang.Long.valueOf(r0)
            int r46 = r2.intValue()
            r47 = 1
            int r46 = r46 + 1
            r48 = r2
            java.lang.Integer r2 = java.lang.Integer.valueOf(r46)
            r6.put(r9, r2)
            goto Lf81
        Lf6e:
            r48 = r2
            r45 = r9
            r47 = 1
            com.android.server.utils.WatchedArrayMap<java.lang.Long, java.lang.Integer> r2 = r3.mKeySetRefs
            java.lang.Long r6 = java.lang.Long.valueOf(r0)
            java.lang.Integer r9 = java.lang.Integer.valueOf(r47)
            r2.put(r6, r9)
        Lf81:
            com.android.server.pm.PackageKeySetData r2 = r7.getKeySetData()
            r2.addDefinedKeySet(r0, r5)
            r1 = 5
            goto L1021
        Lf8b:
            r45 = r9
            r47 = 1
            java.lang.String r0 = "install-initiator-sigs"
            boolean r0 = r8.equals(r0)
            if (r0 == 0) goto Lfb3
            com.android.server.pm.PackageSignatures r0 = new com.android.server.pm.PackageSignatures
            r0.<init>()
            com.android.server.utils.WatchedArrayList<android.content.pm.Signature> r1 = r3.mPastSignatures
            java.util.ArrayList r1 = r1.untrackedStorage()
            r0.readXml(r4, r1)
            com.android.server.pm.InstallSource r1 = r7.getInstallSource()
            com.android.server.pm.InstallSource r1 = r1.setInitiatingPackageSignatures(r0)
            r7.setInstallSource(r1)
            r1 = 5
            goto L1021
        Lfb3:
            java.lang.String r0 = "domain-verification"
            boolean r0 = r8.equals(r0)
            if (r0 == 0) goto Lfcb
            android.content.pm.IntentFilterVerificationInfo r0 = new android.content.pm.IntentFilterVerificationInfo
            r0.<init>(r4)
            com.android.server.pm.verify.domain.DomainVerificationManagerInternal r1 = r3.mDomainVerificationManager
            java.lang.String r2 = r7.getPackageName()
            r1.addLegacySetting(r2, r0)
            r1 = 5
            goto L1021
        Lfcb:
            java.lang.String r0 = "mime-group"
            boolean r0 = r8.equals(r0)
            if (r0 == 0) goto Lfe7
            android.util.Pair r0 = r84.readMimeGroupLPw(r85)
            if (r0 == 0) goto Lfe5
            java.lang.Object r1 = r0.first
            java.lang.String r1 = (java.lang.String) r1
            java.lang.Object r2 = r0.second
            java.util.Set r2 = (java.util.Set) r2
            r7.addMimeTypes(r1, r2)
        Lfe5:
            r1 = 5
            goto L1021
        Lfe7:
            java.lang.String r0 = "uses-static-lib"
            boolean r0 = r8.equals(r0)
            if (r0 == 0) goto Lff5
            r3.readUsesStaticLibLPw(r4, r7)
            r1 = 5
            goto L1021
        Lff5:
            java.lang.String r0 = "uses-sdk-lib"
            boolean r0 = r8.equals(r0)
            if (r0 == 0) goto L1003
            r3.readUsesSdkLibLPw(r4, r7)
            r1 = 5
            goto L1021
        L1003:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "Unknown element under <package>: "
            java.lang.StringBuilder r0 = r0.append(r1)
            java.lang.String r1 = r85.getName()
            java.lang.StringBuilder r0 = r0.append(r1)
            java.lang.String r0 = r0.toString()
            r1 = 5
            com.android.server.pm.PackageManagerService.reportSettingsProblem(r1, r0)
            com.android.internal.util.XmlUtils.skipCurrentTag(r85)
        L1021:
            r0 = r21
            r1 = r22
            r2 = r23
            r5 = r41
            r6 = r42
            r8 = 0
            goto Lde2
        L102e:
            r21 = r0
            r22 = r1
            r23 = r2
            r41 = r5
            r42 = r6
            r45 = r9
            r1 = 5
            r47 = 1
            r1 = r22
            r8 = 0
            goto Lde2
        L1042:
            r22 = r1
            r23 = r2
            r41 = r5
            r42 = r6
            r45 = r21
            r21 = r0
        L104e:
            int r0 = (r32 > r43 ? 1 : (r32 == r43 ? 0 : -1))
            if (r0 == 0) goto L1060
            java.lang.String r0 = r7.getPackageName()
            java.lang.Long r1 = java.lang.Long.valueOf(r32)
            r2 = r87
            r2.put(r0, r1)
            goto L1062
        L1060:
            r2 = r87
        L1062:
            goto L107a
        L1063:
            r3 = r84
            r22 = r1
            r40 = r2
            r17 = r4
            r18 = r5
            r19 = r6
            r20 = r8
            r41 = r9
            r4 = r85
            r2 = r87
            com.android.internal.util.XmlUtils.skipCurrentTag(r85)
        L107a:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.Settings.readPackageLPw(android.util.TypedXmlPullParser, java.util.List, android.util.ArrayMap):void");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addInstallerPackageNames(InstallSource installSource) {
        if (installSource.installerPackageName != null) {
            this.mInstallerPackages.add(installSource.installerPackageName);
        }
        if (installSource.initiatingPackageName != null) {
            this.mInstallerPackages.add(installSource.initiatingPackageName);
        }
        if (installSource.originatingPackageName != null) {
            this.mInstallerPackages.add(installSource.originatingPackageName);
        }
    }

    private Pair<String, Set<String>> readMimeGroupLPw(TypedXmlPullParser parser) throws XmlPullParserException, IOException {
        String groupName = parser.getAttributeValue((String) null, "name");
        if (groupName == null) {
            XmlUtils.skipCurrentTag(parser);
            return null;
        }
        Set<String> mimeTypes = new ArraySet<>();
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            } else if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if (tagName.equals(TAG_MIME_TYPE)) {
                    String typeName = parser.getAttributeValue((String) null, ATTR_VALUE);
                    if (typeName != null) {
                        mimeTypes.add(typeName);
                    }
                } else {
                    PackageManagerService.reportSettingsProblem(5, "Unknown element under <mime-group>: " + parser.getName());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
        return Pair.create(groupName, mimeTypes);
    }

    private void writeMimeGroupLPr(TypedXmlSerializer serializer, Map<String, Set<String>> mimeGroups) throws IOException {
        if (mimeGroups == null) {
            return;
        }
        for (String mimeGroup : mimeGroups.keySet()) {
            serializer.startTag((String) null, TAG_MIME_GROUP);
            serializer.attribute((String) null, "name", mimeGroup);
            for (String mimeType : mimeGroups.get(mimeGroup)) {
                serializer.startTag((String) null, TAG_MIME_TYPE);
                serializer.attribute((String) null, ATTR_VALUE, mimeType);
                serializer.endTag((String) null, TAG_MIME_TYPE);
            }
            serializer.endTag((String) null, TAG_MIME_GROUP);
        }
    }

    private void readDisabledComponentsLPw(PackageSetting packageSetting, TypedXmlPullParser parser, int userId) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals(TAG_ITEM)) {
                            String name = parser.getAttributeValue((String) null, "name");
                            if (name != null) {
                                packageSetting.addDisabledComponent(name.intern(), userId);
                            } else {
                                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <disabled-components> has no name at " + parser.getPositionDescription());
                            }
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Unknown element under <disabled-components>: " + parser.getName());
                        }
                        XmlUtils.skipCurrentTag(parser);
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void readEnabledComponentsLPw(PackageSetting packageSetting, TypedXmlPullParser parser, int userId) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals(TAG_ITEM)) {
                            String name = parser.getAttributeValue((String) null, "name");
                            if (name != null) {
                                packageSetting.addEnabledComponent(name.intern(), userId);
                            } else {
                                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <enabled-components> has no name at " + parser.getPositionDescription());
                            }
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Unknown element under <enabled-components>: " + parser.getName());
                        }
                        XmlUtils.skipCurrentTag(parser);
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void readSharedUserLPw(TypedXmlPullParser parser, List<UserInfo> users) throws XmlPullParserException, IOException {
        int pkgFlags = 0;
        SharedUserSetting su = null;
        String name = parser.getAttributeValue((String) null, "name");
        int userId = parser.getAttributeInt((String) null, "userId", 0);
        if (parser.getAttributeBoolean((String) null, HostingRecord.HOSTING_TYPE_SYSTEM, false)) {
            pkgFlags = 0 | 1;
        }
        if (name == null) {
            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <shared-user> has no name at " + parser.getPositionDescription());
        } else if (userId != 0) {
            SharedUserSetting addSharedUserLPw = addSharedUserLPw(name.intern(), userId, pkgFlags, 0);
            su = addSharedUserLPw;
            if (addSharedUserLPw == null) {
                PackageManagerService.reportSettingsProblem(6, "Occurred while parsing settings at " + parser.getPositionDescription());
            }
        } else {
            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: shared-user " + name + " has bad userId " + userId + " at " + parser.getPositionDescription());
        }
        if (su != null) {
            int outerDepth = parser.getDepth();
            while (true) {
                int type = parser.next();
                if (type != 1) {
                    if (type != 3 || parser.getDepth() > outerDepth) {
                        if (type != 3 && type != 4) {
                            String tagName = parser.getName();
                            if (tagName.equals("sigs")) {
                                su.signatures.readXml(parser, this.mPastSignatures.untrackedStorage());
                            } else if (tagName.equals(TAG_PERMISSIONS)) {
                                readInstallPermissionsLPr(parser, su.getLegacyPermissionState(), users);
                            } else {
                                PackageManagerService.reportSettingsProblem(5, "Unknown element under <shared-user>: " + parser.getName());
                                XmlUtils.skipCurrentTag(parser);
                            }
                        }
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }
        } else {
            XmlUtils.skipCurrentTag(parser);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4312=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:41:0x00a2 A[Catch: all -> 0x010c, TryCatch #4 {all -> 0x010c, blocks: (B:11:0x0042, B:14:0x0059, B:34:0x0092, B:39:0x009c, B:41:0x00a2, B:45:0x00bd), top: B:84:0x0042 }] */
    /* JADX WARN: Removed duplicated region for block: B:48:0x00f6 A[Catch: all -> 0x013d, TryCatch #5 {all -> 0x013d, blocks: (B:49:0x0104, B:47:0x00ed, B:48:0x00f6, B:53:0x0116), top: B:86:0x00ed }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void createNewUserLI(PackageManagerService service, Installer installer, int userHandle, Set<String> userTypeInstallablePackages, String[] disallowedPackages) {
        PackageManagerTracedLock packageManagerTracedLock;
        boolean z;
        boolean shouldReallyInstall;
        int i;
        int size;
        TimingsTraceAndSlog t = new TimingsTraceAndSlog("PackageSettingsTiming", 262144L);
        t.traceBegin("createNewUser-" + userHandle);
        Installer.Batch batch = new Installer.Batch();
        boolean skipPackageAllowList = userTypeInstallablePackages == null;
        PackageManagerTracedLock packageManagerTracedLock2 = this.mLock;
        synchronized (packageManagerTracedLock2) {
            try {
                int size2 = this.mPackages.size();
                int i2 = 0;
                while (i2 < size2) {
                    try {
                        PackageSetting ps = this.mPackages.valueAt(i2);
                        if (ps.getPkg() == null) {
                            i = i2;
                            size = size2;
                            packageManagerTracedLock = packageManagerTracedLock2;
                        } else {
                            if (ps.isSystem()) {
                                try {
                                    if (!ArrayUtils.contains(disallowedPackages, ps.getPackageName()) && !ps.getPkgState().isHiddenUntilInstalled()) {
                                        z = true;
                                        boolean shouldMaybeInstall = z;
                                        shouldReallyInstall = !shouldMaybeInstall && (skipPackageAllowList || userTypeInstallablePackages.contains(ps.getPackageName()));
                                        ps.setInstalled(shouldReallyInstall, userHandle);
                                        int uninstallReason = (shouldMaybeInstall || shouldReallyInstall) ? 0 : 1;
                                        ps.setUninstallReason(uninstallReason, userHandle);
                                        if (shouldReallyInstall) {
                                            i = i2;
                                            size = size2;
                                            packageManagerTracedLock = packageManagerTracedLock2;
                                            writeKernelMappingLPr(ps);
                                        } else {
                                            String seInfo = AndroidPackageUtils.getSeInfo(ps.getPkg(), ps);
                                            boolean usesSdk = !ps.getPkg().getUsesSdkLibraries().isEmpty();
                                            i = i2;
                                            size = size2;
                                            packageManagerTracedLock = packageManagerTracedLock2;
                                            try {
                                                CreateAppDataArgs args = Installer.buildCreateAppDataArgs(ps.getVolumeUuid(), ps.getPackageName(), userHandle, 3, ps.getAppId(), seInfo, ps.getPkg().getTargetSdkVersion(), usesSdk);
                                                batch.createAppData(args);
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
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                    packageManagerTracedLock = packageManagerTracedLock2;
                                    while (true) {
                                        break;
                                        break;
                                    }
                                    throw th;
                                }
                            }
                            z = false;
                            boolean shouldMaybeInstall2 = z;
                            if (shouldMaybeInstall2) {
                            }
                            ps.setInstalled(shouldReallyInstall, userHandle);
                            int uninstallReason2 = (shouldMaybeInstall2 || shouldReallyInstall) ? 0 : 1;
                            ps.setUninstallReason(uninstallReason2, userHandle);
                            if (shouldReallyInstall) {
                            }
                        }
                        i2 = i + 1;
                        size2 = size;
                        packageManagerTracedLock2 = packageManagerTracedLock;
                    } catch (Throwable th4) {
                        th = th4;
                        packageManagerTracedLock = packageManagerTracedLock2;
                        while (true) {
                            break;
                            break;
                        }
                        throw th;
                    }
                }
                packageManagerTracedLock = packageManagerTracedLock2;
                t.traceBegin("createAppData");
                try {
                    batch.execute(installer);
                } catch (Installer.InstallerException e) {
                    Slog.w(TAG, "Failed to prepare app data", e);
                }
                t.traceEnd();
                synchronized (this.mLock) {
                    applyDefaultPreferredAppsLPw(userHandle);
                }
                t.traceEnd();
            } catch (Throwable th5) {
                th = th5;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeUserLPw(int userId) {
        Set<Map.Entry<String, PackageSetting>> entries = this.mPackages.entrySet();
        for (Map.Entry<String, PackageSetting> entry : entries) {
            entry.getValue().removeUser(userId);
        }
        this.mPreferredActivities.remove(userId);
        File file = getUserPackagesStateFile(userId);
        file.delete();
        File file2 = getUserPackagesStateBackupFile(userId);
        file2.delete();
        removeCrossProfileIntentFiltersLPw(userId);
        this.mRuntimePermissionsPersistence.onUserRemoved(userId);
        this.mDomainVerificationManager.clearUser(userId);
        writePackageListLPr();
        writeKernelRemoveUserLPr(userId);
    }

    void removeCrossProfileIntentFiltersLPw(int userId) {
        synchronized (this.mCrossProfileIntentResolvers) {
            if (this.mCrossProfileIntentResolvers.get(userId) != null) {
                this.mCrossProfileIntentResolvers.remove(userId);
                writePackageRestrictionsLPr(userId);
            }
            int count = this.mCrossProfileIntentResolvers.size();
            for (int i = 0; i < count; i++) {
                int sourceUserId = this.mCrossProfileIntentResolvers.keyAt(i);
                CrossProfileIntentResolver cpir = this.mCrossProfileIntentResolvers.get(sourceUserId);
                boolean needsWriting = false;
                ArraySet<CrossProfileIntentFilter> cpifs = new ArraySet<>(cpir.filterSet());
                Iterator<CrossProfileIntentFilter> it = cpifs.iterator();
                while (it.hasNext()) {
                    CrossProfileIntentFilter cpif = it.next();
                    if (cpif.getTargetUserId() == userId) {
                        needsWriting = true;
                        cpir.removeFilter((CrossProfileIntentResolver) cpif);
                    }
                }
                if (needsWriting) {
                    writePackageRestrictionsLPr(sourceUserId);
                }
            }
        }
    }

    public VerifierDeviceIdentity getVerifierDeviceIdentityLPw(Computer computer) {
        if (this.mVerifierDeviceIdentity == null) {
            this.mVerifierDeviceIdentity = VerifierDeviceIdentity.generate();
            writeLPr(computer);
        }
        return this.mVerifierDeviceIdentity;
    }

    public PackageSetting getDisabledSystemPkgLPr(String name) {
        PackageSetting ps = this.mDisabledSysPackages.get(name);
        return ps;
    }

    public PackageSetting getDisabledSystemPkgLPr(PackageSetting enabledPackageSetting) {
        if (enabledPackageSetting == null) {
            return null;
        }
        return getDisabledSystemPkgLPr(enabledPackageSetting.getPackageName());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getApplicationEnabledSettingLPr(String packageName, int userId) throws PackageManager.NameNotFoundException {
        PackageSetting pkg = this.mPackages.get(packageName);
        if (pkg == null) {
            throw new PackageManager.NameNotFoundException(packageName);
        }
        return pkg.getEnabled(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getComponentEnabledSettingLPr(ComponentName componentName, int userId) throws PackageManager.NameNotFoundException {
        String packageName = componentName.getPackageName();
        PackageSetting pkg = this.mPackages.get(packageName);
        if (pkg == null) {
            throw new PackageManager.NameNotFoundException(componentName.getPackageName());
        }
        String classNameStr = componentName.getClassName();
        return pkg.getCurrentEnabledStateLPr(classNameStr, userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SharedUserSetting getSharedUserSettingLPr(String packageName) {
        PackageSetting ps = this.mPackages.get(packageName);
        return getSharedUserSettingLPr(ps);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SharedUserSetting getSharedUserSettingLPr(PackageSetting ps) {
        if (ps == null || !ps.hasSharedUser()) {
            return null;
        }
        return (SharedUserSetting) getSettingLPr(ps.getSharedUserAppId());
    }

    private static List<UserInfo> getAllUsers(UserManagerService userManager) {
        return getUsers(userManager, false, false);
    }

    private static List<UserInfo> getActiveUsers(UserManagerService userManager, boolean excludeDying) {
        return getUsers(userManager, excludeDying, true);
    }

    private static List<UserInfo> getUsers(UserManagerService userManager, boolean excludeDying, boolean excludePreCreated) {
        long id = Binder.clearCallingIdentity();
        try {
            List<UserInfo> users = userManager.getUsers(true, excludeDying, excludePreCreated);
            Binder.restoreCallingIdentity(id);
            return users;
        } catch (NullPointerException e) {
            Binder.restoreCallingIdentity(id);
            return null;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(id);
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<? extends PackageStateInternal> getVolumePackagesLPr(String volumeUuid) {
        ArrayList<PackageStateInternal> res = new ArrayList<>();
        for (int i = 0; i < this.mPackages.size(); i++) {
            PackageSetting setting = this.mPackages.valueAt(i);
            if (Objects.equals(volumeUuid, setting.getVolumeUuid())) {
                res.add(setting);
            }
        }
        return res;
    }

    static void printFlags(PrintWriter pw, int val, Object[] spec) {
        pw.print("[ ");
        for (int i = 0; i < spec.length; i += 2) {
            int mask = ((Integer) spec[i]).intValue();
            if ((val & mask) != 0) {
                pw.print(spec[i + 1]);
                pw.print(" ");
            }
        }
        pw.print("]");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpVersionLPr(IndentingPrintWriter pw) {
        pw.increaseIndent();
        for (int i = 0; i < this.mVersion.size(); i++) {
            String volumeUuid = this.mVersion.keyAt(i);
            VersionInfo ver = this.mVersion.valueAt(i);
            if (Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, volumeUuid)) {
                pw.println("Internal:");
            } else if (Objects.equals("primary_physical", volumeUuid)) {
                pw.println("External:");
            } else {
                pw.println("UUID " + volumeUuid + ":");
            }
            pw.increaseIndent();
            pw.printPair(ATTR_SDK_VERSION, Integer.valueOf(ver.sdkVersion));
            pw.printPair(ATTR_DATABASE_VERSION, Integer.valueOf(ver.databaseVersion));
            pw.println();
            pw.printPair(ATTR_FINGERPRINT, ver.fingerprint);
            pw.println();
            pw.decreaseIndent();
        }
        pw.decreaseIndent();
    }

    @NeverCompile
    void dumpPackageLPr(PrintWriter pw, String prefix, String checkinTag, ArraySet<String> permissionNames, PackageSetting ps, LegacyPermissionState permissionsState, SimpleDateFormat sdf, Date date, List<UserInfo> users, boolean dumpAll, boolean dumpAllComponents) {
        String str;
        Iterator<Map.Entry<String, OverlayPaths>> it;
        Settings settings = this;
        AndroidPackage pkg = ps.getPkg();
        if (checkinTag != null) {
            pw.print(checkinTag);
            pw.print(",");
            pw.print(ps.getRealName() != null ? ps.getRealName() : ps.getPackageName());
            pw.print(",");
            pw.print(ps.getAppId());
            pw.print(",");
            pw.print(ps.getVersionCode());
            pw.print(",");
            pw.print(ps.getLastUpdateTime());
            pw.print(",");
            String str2 = "?";
            pw.print(ps.getInstallSource().installerPackageName != null ? ps.getInstallSource().installerPackageName : "?");
            pw.print(ps.getInstallSource().installerAttributionTag != null ? "(" + ps.getInstallSource().installerAttributionTag + ")" : "");
            pw.print(",");
            pw.print(ps.getInstallSource().packageSource);
            pw.println();
            String str3 = "-";
            if (pkg != null) {
                pw.print(checkinTag);
                pw.print("-");
                pw.print("splt,");
                pw.print("base,");
                pw.println(pkg.getBaseRevisionCode());
                int[] splitRevisionCodes = pkg.getSplitRevisionCodes();
                for (int i = 0; i < pkg.getSplitNames().length; i++) {
                    pw.print(checkinTag);
                    pw.print("-");
                    pw.print("splt,");
                    pw.print(pkg.getSplitNames()[i]);
                    pw.print(",");
                    pw.println(splitRevisionCodes[i]);
                }
            }
            for (UserInfo user : users) {
                PackageUserStateInternal userState = ps.getUserStateOrDefault(user.id);
                pw.print(checkinTag);
                pw.print(str3);
                pw.print("usr");
                pw.print(",");
                pw.print(user.id);
                pw.print(",");
                pw.print(userState.isInstalled() ? "I" : "i");
                pw.print(userState.isHidden() ? "B" : "b");
                pw.print(userState.isSuspended() ? "SU" : "su");
                pw.print(userState.isStopped() ? "S" : "s");
                pw.print(userState.isNotLaunched() ? "l" : "L");
                pw.print(userState.isInstantApp() ? "IA" : "ia");
                pw.print(userState.isVirtualPreload() ? "VPI" : "vpi");
                pw.print(userState.getHarmfulAppWarning() != null ? "HA" : "ha");
                pw.print(",");
                pw.print(userState.getEnabledState());
                String lastDisabledAppCaller = userState.getLastDisableAppCaller();
                pw.print(",");
                String str4 = str3;
                pw.print(lastDisabledAppCaller != null ? lastDisabledAppCaller : str2);
                pw.print(",");
                pw.print(ps.readUserState(user.id).getFirstInstallTime());
                pw.print(",");
                pw.println();
                str3 = str4;
                str2 = str2;
            }
            return;
        }
        pw.print(prefix);
        pw.print("Package [");
        pw.print(ps.getRealName() != null ? ps.getRealName() : ps.getPackageName());
        pw.print("] (");
        pw.print(Integer.toHexString(System.identityHashCode(ps)));
        pw.println("):");
        if (ps.getRealName() != null) {
            pw.print(prefix);
            pw.print("  compat name=");
            pw.println(ps.getPackageName());
        }
        pw.print(prefix);
        pw.print("  userId=");
        pw.println(ps.getAppId());
        SharedUserSetting sharedUserSetting = settings.getSharedUserSettingLPr(ps);
        if (sharedUserSetting != null) {
            pw.print(prefix);
            pw.print("  sharedUser=");
            pw.println(sharedUserSetting);
        }
        pw.print(prefix);
        pw.print("  pkg=");
        pw.println(pkg);
        pw.print(prefix);
        pw.print("  codePath=");
        pw.println(ps.getPathString());
        if (permissionNames == null) {
            pw.print(prefix);
            pw.print("  resourcePath=");
            pw.println(ps.getPathString());
            pw.print(prefix);
            pw.print("  legacyNativeLibraryDir=");
            pw.println(ps.getLegacyNativeLibraryPath());
            pw.print(prefix);
            pw.print("  extractNativeLibs=");
            pw.println((ps.getFlags() & 268435456) != 0 ? "true" : "false");
            pw.print(prefix);
            pw.print("  primaryCpuAbi=");
            pw.println(ps.getPrimaryCpuAbi());
            pw.print(prefix);
            pw.print("  secondaryCpuAbi=");
            pw.println(ps.getSecondaryCpuAbi());
            pw.print(prefix);
            pw.print("  cpuAbiOverride=");
            pw.println(ps.getCpuAbiOverride());
        }
        pw.print(prefix);
        pw.print("  versionCode=");
        pw.print(ps.getVersionCode());
        if (pkg != null) {
            pw.print(" minSdk=");
            pw.print(pkg.getMinSdkVersion());
            pw.print(" targetSdk=");
            pw.println(pkg.getTargetSdkVersion());
            SparseIntArray minExtensionVersions = pkg.getMinExtensionVersions();
            pw.print(prefix);
            pw.print("  minExtensionVersions=[");
            if (minExtensionVersions != null) {
                List<String> minExtVerStrings = new ArrayList<>();
                int size = minExtensionVersions.size();
                int index = 0;
                while (index < size) {
                    int key = minExtensionVersions.keyAt(index);
                    int size2 = size;
                    int value = minExtensionVersions.valueAt(index);
                    minExtVerStrings.add(key + "=" + value);
                    index++;
                    size = size2;
                    minExtensionVersions = minExtensionVersions;
                }
                pw.print(TextUtils.join(", ", minExtVerStrings));
            }
            pw.print("]");
        }
        pw.println();
        if (pkg != null) {
            pw.print(prefix);
            pw.print("  versionName=");
            pw.println(pkg.getVersionName());
            pw.print(prefix);
            pw.print("  usesNonSdkApi=");
            pw.println(pkg.isUsesNonSdkApi());
            pw.print(prefix);
            pw.print("  splits=");
            dumpSplitNames(pw, pkg);
            pw.println();
            int apkSigningVersion = pkg.getSigningDetails().getSignatureSchemeVersion();
            pw.print(prefix);
            pw.print("  apkSigningVersion=");
            pw.println(apkSigningVersion);
            pw.print(prefix);
            pw.print("  flags=");
            printFlags(pw, PackageInfoUtils.appInfoFlags(pkg, ps), FLAG_DUMP_SPEC);
            pw.println();
            int privateFlags = PackageInfoUtils.appInfoPrivateFlags(pkg, ps);
            if (privateFlags != 0) {
                pw.print(prefix);
                pw.print("  privateFlags=");
                printFlags(pw, privateFlags, PRIVATE_FLAG_DUMP_SPEC);
                pw.println();
            }
            if (pkg.hasPreserveLegacyExternalStorage()) {
                pw.print(prefix);
                pw.print("  hasPreserveLegacyExternalStorage=true");
                pw.println();
            }
            pw.print(prefix);
            pw.print("  forceQueryable=");
            pw.print(ps.getPkg().isForceQueryable());
            if (ps.isForceQueryableOverride()) {
                pw.print(" (override=true)");
            }
            pw.println();
            if (!ps.getPkg().getQueriesPackages().isEmpty()) {
                pw.append((CharSequence) prefix).append((CharSequence) "  queriesPackages=").println(ps.getPkg().getQueriesPackages());
            }
            if (!ps.getPkg().getQueriesIntents().isEmpty()) {
                pw.append((CharSequence) prefix).append((CharSequence) "  queriesIntents=").println(ps.getPkg().getQueriesIntents());
            }
            File dataDir = PackageInfoWithoutStateUtils.getDataDir(pkg, UserHandle.myUserId());
            pw.print(prefix);
            pw.print("  dataDir=");
            pw.println(dataDir.getAbsolutePath());
            pw.print(prefix);
            pw.print("  supportsScreens=[");
            boolean first = true;
            if (pkg.isSupportsSmallScreens()) {
                if (1 == 0) {
                    pw.print(", ");
                }
                first = false;
                pw.print("small");
            }
            if (pkg.isSupportsNormalScreens()) {
                if (!first) {
                    pw.print(", ");
                }
                first = false;
                pw.print("medium");
            }
            if (pkg.isSupportsLargeScreens()) {
                if (!first) {
                    pw.print(", ");
                }
                first = false;
                pw.print("large");
            }
            if (pkg.isSupportsExtraLargeScreens()) {
                if (!first) {
                    pw.print(", ");
                }
                first = false;
                pw.print("xlarge");
            }
            if (pkg.isResizeable()) {
                if (!first) {
                    pw.print(", ");
                }
                first = false;
                pw.print("resizeable");
            }
            if (pkg.isAnyDensity()) {
                if (!first) {
                    pw.print(", ");
                }
                pw.print("anyDensity");
            }
            pw.println("]");
            List<String> libraryNames = pkg.getLibraryNames();
            if (libraryNames != null && libraryNames.size() > 0) {
                pw.print(prefix);
                pw.println("  dynamic libraries:");
                for (int i2 = 0; i2 < libraryNames.size(); i2++) {
                    pw.print(prefix);
                    pw.print("    ");
                    pw.println(libraryNames.get(i2));
                }
            }
            String str5 = " version:";
            if (pkg.getStaticSharedLibName() != null) {
                pw.print(prefix);
                pw.println("  static library:");
                pw.print(prefix);
                pw.print("    ");
                pw.print("name:");
                pw.print(pkg.getStaticSharedLibName());
                pw.print(" version:");
                pw.println(pkg.getStaticSharedLibVersion());
            }
            if (pkg.getSdkLibName() != null) {
                pw.print(prefix);
                pw.println("  SDK library:");
                pw.print(prefix);
                pw.print("    ");
                pw.print("name:");
                pw.print(pkg.getSdkLibName());
                pw.print(" versionMajor:");
                pw.println(pkg.getSdkLibVersionMajor());
            }
            List<String> usesLibraries = pkg.getUsesLibraries();
            if (usesLibraries.size() > 0) {
                pw.print(prefix);
                pw.println("  usesLibraries:");
                for (int i3 = 0; i3 < usesLibraries.size(); i3++) {
                    pw.print(prefix);
                    pw.print("    ");
                    pw.println(usesLibraries.get(i3));
                }
            }
            List<String> usesStaticLibraries = pkg.getUsesStaticLibraries();
            long[] usesStaticLibrariesVersions = pkg.getUsesStaticLibrariesVersions();
            if (usesStaticLibraries.size() > 0) {
                pw.print(prefix);
                pw.println("  usesStaticLibraries:");
                int i4 = 0;
                while (true) {
                    int privateFlags2 = privateFlags;
                    if (i4 >= usesStaticLibraries.size()) {
                        break;
                    }
                    pw.print(prefix);
                    pw.print("    ");
                    pw.print(usesStaticLibraries.get(i4));
                    pw.print(" version:");
                    pw.println(usesStaticLibrariesVersions[i4]);
                    i4++;
                    privateFlags = privateFlags2;
                    usesStaticLibraries = usesStaticLibraries;
                }
            }
            List<String> usesSdkLibraries = pkg.getUsesSdkLibraries();
            long[] usesSdkLibrariesVersionsMajor = pkg.getUsesSdkLibrariesVersionsMajor();
            if (usesSdkLibraries.size() > 0) {
                pw.print(prefix);
                pw.println("  usesSdkLibraries:");
                int size3 = usesSdkLibraries.size();
                int i5 = 0;
                while (i5 < size3) {
                    pw.print(prefix);
                    pw.print("    ");
                    pw.print(usesSdkLibraries.get(i5));
                    pw.print(str5);
                    pw.println(usesSdkLibrariesVersionsMajor[i5]);
                    i5++;
                    str5 = str5;
                    usesSdkLibraries = usesSdkLibraries;
                }
            }
            List<String> usesOptionalLibraries = pkg.getUsesOptionalLibraries();
            if (usesOptionalLibraries.size() > 0) {
                pw.print(prefix);
                pw.println("  usesOptionalLibraries:");
                for (int i6 = 0; i6 < usesOptionalLibraries.size(); i6++) {
                    pw.print(prefix);
                    pw.print("    ");
                    pw.println(usesOptionalLibraries.get(i6));
                }
            }
            List<String> usesNativeLibraries = pkg.getUsesNativeLibraries();
            if (usesNativeLibraries.size() > 0) {
                pw.print(prefix);
                pw.println("  usesNativeLibraries:");
                for (int i7 = 0; i7 < usesNativeLibraries.size(); i7++) {
                    pw.print(prefix);
                    pw.print("    ");
                    pw.println(usesNativeLibraries.get(i7));
                }
            }
            List<String> usesOptionalNativeLibraries = pkg.getUsesOptionalNativeLibraries();
            if (usesOptionalNativeLibraries.size() > 0) {
                pw.print(prefix);
                pw.println("  usesOptionalNativeLibraries:");
                int i8 = 0;
                while (true) {
                    List<String> usesOptionalLibraries2 = usesOptionalLibraries;
                    if (i8 >= usesOptionalNativeLibraries.size()) {
                        break;
                    }
                    pw.print(prefix);
                    pw.print("    ");
                    pw.println(usesOptionalNativeLibraries.get(i8));
                    i8++;
                    usesOptionalLibraries = usesOptionalLibraries2;
                }
            }
            List<String> usesLibraryFiles = ps.getPkgState().getUsesLibraryFiles();
            if (usesLibraryFiles.size() > 0) {
                pw.print(prefix);
                pw.println("  usesLibraryFiles:");
                int i9 = 0;
                while (true) {
                    List<String> usesNativeLibraries2 = usesNativeLibraries;
                    if (i9 >= usesLibraryFiles.size()) {
                        break;
                    }
                    pw.print(prefix);
                    pw.print("    ");
                    pw.println(usesLibraryFiles.get(i9));
                    i9++;
                    usesNativeLibraries = usesNativeLibraries2;
                }
            }
            Map<String, ParsedProcess> procs = pkg.getProcesses();
            if (!procs.isEmpty()) {
                pw.print(prefix);
                pw.println("  processes:");
                for (ParsedProcess proc : procs.values()) {
                    pw.print(prefix);
                    pw.print("    ");
                    List<String> usesLibraryFiles2 = usesLibraryFiles;
                    pw.println(proc.getName());
                    if (proc.getDeniedPermissions() != null) {
                        Iterator<String> it2 = proc.getDeniedPermissions().iterator();
                        while (it2.hasNext()) {
                            Iterator<String> it3 = it2;
                            String deniedPermission = it2.next();
                            pw.print(prefix);
                            pw.print("      deny: ");
                            pw.println(deniedPermission);
                            procs = procs;
                            it2 = it3;
                        }
                    }
                    usesLibraryFiles = usesLibraryFiles2;
                    procs = procs;
                }
            }
        }
        pw.print(prefix);
        pw.print("  timeStamp=");
        date.setTime(ps.getLastModifiedTime());
        pw.println(sdf.format(date));
        pw.print(prefix);
        pw.print("  lastUpdateTime=");
        date.setTime(ps.getLastUpdateTime());
        pw.println(sdf.format(date));
        if (ps.getInstallSource().installerPackageName != null) {
            pw.print(prefix);
            pw.print("  installerPackageName=");
            pw.println(ps.getInstallSource().installerPackageName);
        }
        if (ps.getInstallSource().installerAttributionTag != null) {
            pw.print(prefix);
            pw.print("  installerAttributionTag=");
            pw.println(ps.getInstallSource().installerAttributionTag);
        }
        pw.print(prefix);
        pw.print("  packageSource=");
        pw.println(ps.getInstallSource().packageSource);
        if (ps.isLoading()) {
            pw.print(prefix);
            pw.println("  loadingProgress=" + ((int) (ps.getLoadingProgress() * 100.0f)) + "%");
        }
        if (ps.getVolumeUuid() != null) {
            pw.print(prefix);
            pw.print("  volumeUuid=");
            pw.println(ps.getVolumeUuid());
        }
        pw.print(prefix);
        pw.print("  signatures=");
        pw.println(ps.getSignatures());
        pw.print(prefix);
        pw.print("  installPermissionsFixed=");
        pw.print(ps.isInstallPermissionsFixed());
        pw.println();
        pw.print(prefix);
        pw.print("  pkgFlags=");
        printFlags(pw, ps.getFlags(), FLAG_DUMP_SPEC);
        pw.println();
        if (pkg != null && pkg.getOverlayTarget() != null) {
            pw.print(prefix);
            pw.print("  overlayTarget=");
            pw.println(pkg.getOverlayTarget());
            pw.print(prefix);
            pw.print("  overlayCategory=");
            pw.println(pkg.getOverlayCategory());
        }
        if (pkg != null && !pkg.getPermissions().isEmpty()) {
            List<ParsedPermission> perms = pkg.getPermissions();
            pw.print(prefix);
            pw.println("  declared permissions:");
            for (int i10 = 0; i10 < perms.size(); i10++) {
                ParsedPermission perm = perms.get(i10);
                if (permissionNames == null || permissionNames.contains(perm.getName())) {
                    pw.print(prefix);
                    pw.print("    ");
                    pw.print(perm.getName());
                    pw.print(": prot=");
                    pw.print(PermissionInfo.protectionToString(perm.getProtectionLevel()));
                    if ((perm.getFlags() & 1) != 0) {
                        pw.print(", COSTS_MONEY");
                    }
                    if ((perm.getFlags() & 2) != 0) {
                        pw.print(", HIDDEN");
                    }
                    if ((perm.getFlags() & 1073741824) != 0) {
                        pw.print(", INSTALLED");
                    }
                    pw.println();
                }
            }
        }
        if ((permissionNames != null || dumpAll) && pkg != null && pkg.getRequestedPermissions() != null && pkg.getRequestedPermissions().size() > 0) {
            List<String> perms2 = pkg.getRequestedPermissions();
            pw.print(prefix);
            pw.println("  requested permissions:");
            for (int i11 = 0; i11 < perms2.size(); i11++) {
                String perm2 = perms2.get(i11);
                if (permissionNames == null || permissionNames.contains(perm2)) {
                    pw.print(prefix);
                    pw.print("    ");
                    pw.println(perm2);
                }
            }
        }
        if (!ps.hasSharedUser() || permissionNames != null || dumpAll) {
            dumpInstallPermissionsLPr(pw, prefix + "  ", permissionNames, permissionsState, users);
        }
        if (dumpAllComponents) {
            settings.dumpComponents(pw, prefix + "  ", ps);
        }
        for (UserInfo user2 : users) {
            PackageUserStateInternal userState2 = ps.getUserStateOrDefault(user2.id);
            pw.print(prefix);
            pw.print("  User ");
            pw.print(user2.id);
            pw.print(": ");
            pw.print("ceDataInode=");
            pw.print(userState2.getCeDataInode());
            pw.print(" installed=");
            pw.print(userState2.isInstalled());
            pw.print(" hidden=");
            pw.print(userState2.isHidden());
            pw.print(" suspended=");
            pw.print(userState2.isSuspended());
            pw.print(" distractionFlags=");
            pw.print(userState2.getDistractionFlags());
            pw.print(" stopped=");
            pw.print(userState2.isStopped());
            pw.print(" notLaunched=");
            pw.print(userState2.isNotLaunched());
            pw.print(" enabled=");
            pw.print(userState2.getEnabledState());
            pw.print(" instant=");
            pw.print(userState2.isInstantApp());
            pw.print(" virtual=");
            pw.println(userState2.isVirtualPreload());
            pw.print("      installReason=");
            pw.println(userState2.getInstallReason());
            PackageUserStateInternal pus = ps.readUserState(user2.id);
            pw.print("      firstInstallTime=");
            date.setTime(pus.getFirstInstallTime());
            pw.println(sdf.format(date));
            pw.print("      uninstallReason=");
            pw.println(userState2.getUninstallReason());
            if (userState2.isSuspended()) {
                pw.print(prefix);
                pw.println("  Suspend params:");
                for (int i12 = 0; i12 < userState2.getSuspendParams().size(); i12++) {
                    pw.print(prefix);
                    pw.print("    suspendingPackage=");
                    pw.print(userState2.getSuspendParams().keyAt(i12));
                    SuspendParams params = userState2.getSuspendParams().valueAt(i12);
                    if (params != null) {
                        pw.print(" dialogInfo=");
                        pw.print(params.getDialogInfo());
                    }
                    pw.println();
                }
            }
            OverlayPaths overlayPaths = userState2.getOverlayPaths();
            if (overlayPaths != null) {
                if (!overlayPaths.getOverlayPaths().isEmpty()) {
                    pw.print(prefix);
                    pw.println("    overlay paths:");
                    for (String path : overlayPaths.getOverlayPaths()) {
                        pw.print(prefix);
                        pw.print("      ");
                        pw.println(path);
                    }
                }
                if (!overlayPaths.getResourceDirs().isEmpty()) {
                    pw.print(prefix);
                    pw.println("    legacy overlay paths:");
                    for (String path2 : overlayPaths.getResourceDirs()) {
                        pw.print(prefix);
                        pw.print("      ");
                        pw.println(path2);
                    }
                }
            }
            Map<String, OverlayPaths> sharedLibraryOverlayPaths = userState2.getSharedLibraryOverlayPaths();
            if (sharedLibraryOverlayPaths != null) {
                Iterator<Map.Entry<String, OverlayPaths>> it4 = sharedLibraryOverlayPaths.entrySet().iterator();
                while (it4.hasNext()) {
                    Map.Entry<String, OverlayPaths> libOverlayPaths = it4.next();
                    OverlayPaths paths = libOverlayPaths.getValue();
                    if (paths != null) {
                        if (paths.getOverlayPaths().isEmpty()) {
                            it = it4;
                        } else {
                            pw.print(prefix);
                            pw.println("    ");
                            pw.print(libOverlayPaths.getKey());
                            pw.println(" overlay paths:");
                            Iterator it5 = paths.getOverlayPaths().iterator();
                            while (it5.hasNext()) {
                                Iterator<Map.Entry<String, OverlayPaths>> it6 = it4;
                                String path3 = (String) it5.next();
                                pw.print(prefix);
                                pw.print("        ");
                                pw.println(path3);
                                it5 = it5;
                                it4 = it6;
                            }
                            it = it4;
                        }
                        if (!paths.getResourceDirs().isEmpty()) {
                            pw.print(prefix);
                            pw.println("      ");
                            pw.print(libOverlayPaths.getKey());
                            pw.println(" legacy overlay paths:");
                            for (String path4 : paths.getResourceDirs()) {
                                pw.print(prefix);
                                pw.print("      ");
                                pw.println(path4);
                            }
                        }
                        it4 = it;
                    }
                }
            }
            String lastDisabledAppCaller2 = userState2.getLastDisableAppCaller();
            if (lastDisabledAppCaller2 != null) {
                pw.print(prefix);
                pw.print("    lastDisabledCaller: ");
                pw.println(lastDisabledAppCaller2);
            }
            if (ps.hasSharedUser()) {
                str = "      ";
            } else {
                settings.dumpGidsLPr(pw, prefix + "    ", settings.mPermissionDataProvider.getGidsForUid(UserHandle.getUid(user2.id, ps.getAppId())));
                str = "      ";
                dumpRuntimePermissionsLPr(pw, prefix + "    ", permissionNames, permissionsState.getPermissionStates(user2.id), dumpAll);
            }
            String harmfulAppWarning = userState2.getHarmfulAppWarning();
            if (harmfulAppWarning != null) {
                pw.print(prefix);
                pw.print("      harmfulAppWarning: ");
                pw.println(harmfulAppWarning);
            }
            if (permissionNames == null) {
                WatchedArraySet<String> cmp = userState2.getDisabledComponentsNoCopy();
                if (cmp != null && cmp.size() > 0) {
                    pw.print(prefix);
                    pw.println("    disabledComponents:");
                    for (int i13 = 0; i13 < cmp.size(); i13++) {
                        pw.print(prefix);
                        pw.print(str);
                        pw.println(cmp.valueAt(i13));
                    }
                }
                WatchedArraySet<String> cmp2 = userState2.getEnabledComponentsNoCopy();
                if (cmp2 != null && cmp2.size() > 0) {
                    pw.print(prefix);
                    pw.println("    enabledComponents:");
                    for (int i14 = 0; i14 < cmp2.size(); i14++) {
                        pw.print(prefix);
                        pw.print(str);
                        pw.println(cmp2.valueAt(i14));
                    }
                }
            }
            settings = this;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Code restructure failed: missing block: B:47:0x00f2, code lost:
        if (r0 != false) goto L59;
     */
    /* JADX WARN: Code restructure failed: missing block: B:49:0x00f8, code lost:
        if (r27.onTitlePrinted() == false) goto L58;
     */
    /* JADX WARN: Code restructure failed: missing block: B:50:0x00fa, code lost:
        r24.println();
     */
    /* JADX WARN: Code restructure failed: missing block: B:51:0x00fd, code lost:
        r14.println("Renamed packages:");
        r0 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:52:0x0103, code lost:
        r14.print("  ");
     */
    /* JADX WARN: Code restructure failed: missing block: B:73:0x0166, code lost:
        if (r0 != false) goto L91;
     */
    /* JADX WARN: Code restructure failed: missing block: B:75:0x016c, code lost:
        if (r27.onTitlePrinted() == false) goto L90;
     */
    /* JADX WARN: Code restructure failed: missing block: B:76:0x016e, code lost:
        r24.println();
     */
    /* JADX WARN: Code restructure failed: missing block: B:77:0x0171, code lost:
        r14.println("Hidden system packages:");
        r0 = true;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void dumpPackagesLPr(PrintWriter pw, String packageName, ArraySet<String> permissionNames, DumpState dumpState, boolean checkin) {
        boolean printedSomething;
        Settings settings = this;
        PrintWriter printWriter = pw;
        String str = packageName;
        DumpState dumpState2 = dumpState;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        boolean printedSomething2 = false;
        boolean dumpAllComponents = dumpState2.isOptionEnabled(2);
        List<UserInfo> users = getAllUsers(UserManagerService.getInstance());
        Iterator<PackageSetting> it = settings.mPackages.values().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            PackageSetting ps = it.next();
            if (str == null || str.equals(ps.getRealName()) || str.equals(ps.getPackageName())) {
                LegacyPermissionState permissionsState = settings.mPermissionDataProvider.getLegacyPermissionState(ps.getAppId());
                if (permissionNames == null || permissionsState.hasPermissionState(permissionNames)) {
                    if (!checkin && str != null) {
                        dumpState2.setSharedUser(settings.getSharedUserSettingLPr(ps));
                    }
                    if (!checkin && !printedSomething2) {
                        if (dumpState.onTitlePrinted()) {
                            pw.println();
                        }
                        printWriter.println("Packages:");
                        printedSomething = true;
                    } else {
                        printedSomething = printedSomething2;
                    }
                    dumpPackageLPr(pw, "  ", checkin ? TAG_PACKAGE : null, permissionNames, ps, permissionsState, sdf, date, users, str != null, dumpAllComponents);
                    dumpState2 = dumpState;
                    printedSomething2 = printedSomething;
                }
            }
        }
        boolean printedSomething3 = false;
        if (settings.mRenamedPackages.size() > 0 && permissionNames == null) {
            for (Map.Entry<String, String> e : settings.mRenamedPackages.entrySet()) {
                if (str == null || str.equals(e.getKey()) || str.equals(e.getValue())) {
                    printWriter.print("ren,");
                    printWriter.print(e.getKey());
                    printWriter.print(checkin ? " -> " : ",");
                    printWriter.println(e.getValue());
                }
            }
        }
        boolean printedSomething4 = false;
        if (settings.mDisabledSysPackages.size() > 0 && permissionNames == null) {
            for (PackageSetting ps2 : settings.mDisabledSysPackages.values()) {
                if (str == null || str.equals(ps2.getRealName()) || str.equals(ps2.getPackageName())) {
                    dumpPackageLPr(pw, "  ", checkin ? "dis" : null, permissionNames, ps2, settings.mPermissionDataProvider.getLegacyPermissionState(ps2.getAppId()), sdf, date, users, str != null, dumpAllComponents);
                    settings = this;
                    printWriter = pw;
                    str = packageName;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpPackagesProto(ProtoOutputStream proto) {
        List<UserInfo> users = getAllUsers(UserManagerService.getInstance());
        int count = this.mPackages.size();
        for (int i = 0; i < count; i++) {
            PackageSetting ps = this.mPackages.valueAt(i);
            ps.dumpDebug(proto, 2246267895813L, users, this.mPermissionDataProvider);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpPermissions(PrintWriter pw, String packageName, ArraySet<String> permissionNames, DumpState dumpState) {
        LegacyPermissionSettings.dumpPermissions(pw, packageName, permissionNames, this.mPermissionDataProvider.getLegacyPermissions(), this.mPermissionDataProvider.getAllAppOpPermissionPackages(), true, dumpState);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpSharedUsersLPr(PrintWriter pw, String packageName, ArraySet<String> permissionNames, DumpState dumpState, boolean checkin) {
        boolean printedSomething;
        ArraySet<String> arraySet = permissionNames;
        boolean printedSomething2 = false;
        for (SharedUserSetting su : this.mSharedUsers.values()) {
            if (packageName == null || su == dumpState.getSharedUser()) {
                LegacyPermissionState permissionsState = this.mPermissionDataProvider.getLegacyPermissionState(su.mAppId);
                if (arraySet == null || permissionsState.hasPermissionState(arraySet)) {
                    if (!checkin) {
                        if (printedSomething2) {
                            printedSomething = printedSomething2;
                        } else {
                            if (dumpState.onTitlePrinted()) {
                                pw.println();
                            }
                            pw.println("Shared users:");
                            printedSomething = true;
                        }
                        pw.print("  SharedUser [");
                        pw.print(su.name);
                        pw.print("] (");
                        pw.print(Integer.toHexString(System.identityHashCode(su)));
                        pw.println("):");
                        pw.print("    ");
                        pw.print("userId=");
                        pw.println(su.mAppId);
                        pw.print("    ");
                        pw.println("Packages");
                        ArraySet<? extends PackageStateInternal> packageStates = su.getPackageStates();
                        int numPackages = packageStates.size();
                        for (int i = 0; i < numPackages; i++) {
                            PackageStateInternal ps = packageStates.valueAt(i);
                            if (ps != null) {
                                pw.print("      ");
                                pw.println(ps);
                            } else {
                                pw.print("      ");
                                pw.println("NULL?!");
                            }
                        }
                        if (dumpState.isOptionEnabled(4)) {
                            printedSomething2 = printedSomething;
                        } else {
                            List<UserInfo> users = getAllUsers(UserManagerService.getInstance());
                            dumpInstallPermissionsLPr(pw, "    ", permissionNames, permissionsState, users);
                            for (UserInfo user : users) {
                                int userId = user.id;
                                int[] gids = this.mPermissionDataProvider.getGidsForUid(UserHandle.getUid(userId, su.mAppId));
                                Collection<LegacyPermissionState.PermissionState> permissions = permissionsState.getPermissionStates(userId);
                                if (!ArrayUtils.isEmpty(gids) || !permissions.isEmpty()) {
                                    pw.print("    ");
                                    pw.print("User ");
                                    pw.print(userId);
                                    pw.println(": ");
                                    dumpGidsLPr(pw, "      ", gids);
                                    dumpRuntimePermissionsLPr(pw, "      ", permissionNames, permissions, packageName != null);
                                }
                            }
                            printedSomething2 = printedSomething;
                        }
                    } else {
                        pw.print("suid,");
                        pw.print(su.mAppId);
                        pw.print(",");
                        pw.println(su.name);
                    }
                    arraySet = permissionNames;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpSharedUsersProto(ProtoOutputStream proto) {
        int count = this.mSharedUsers.size();
        for (int i = 0; i < count; i++) {
            this.mSharedUsers.valueAt(i).dumpDebug(proto, 2246267895814L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpReadMessages(PrintWriter pw, DumpState dumpState) {
        pw.println("Settings parse messages:");
        pw.print(this.mReadMessages.toString());
    }

    private static void dumpSplitNames(PrintWriter pw, AndroidPackage pkg) {
        if (pkg == null) {
            pw.print(UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN);
            return;
        }
        pw.print("[");
        pw.print("base");
        if (pkg.getBaseRevisionCode() != 0) {
            pw.print(":");
            pw.print(pkg.getBaseRevisionCode());
        }
        String[] splitNames = pkg.getSplitNames();
        int[] splitRevisionCodes = pkg.getSplitRevisionCodes();
        for (int i = 0; i < splitNames.length; i++) {
            pw.print(", ");
            pw.print(splitNames[i]);
            if (splitRevisionCodes[i] != 0) {
                pw.print(":");
                pw.print(splitRevisionCodes[i]);
            }
        }
        pw.print("]");
    }

    void dumpGidsLPr(PrintWriter pw, String prefix, int[] gids) {
        if (!ArrayUtils.isEmpty(gids)) {
            pw.print(prefix);
            pw.print("gids=");
            pw.println(PackageManagerServiceUtils.arrayToString(gids));
        }
    }

    void dumpRuntimePermissionsLPr(PrintWriter pw, String prefix, ArraySet<String> permissionNames, Collection<LegacyPermissionState.PermissionState> permissionStates, boolean dumpAll) {
        boolean hasRuntimePermissions = false;
        Iterator<LegacyPermissionState.PermissionState> it = permissionStates.iterator();
        while (true) {
            if (it.hasNext()) {
                if (it.next().isRuntime()) {
                    hasRuntimePermissions = true;
                    break;
                }
            } else {
                break;
            }
        }
        if (hasRuntimePermissions || dumpAll) {
            pw.print(prefix);
            pw.println("runtime permissions:");
            for (LegacyPermissionState.PermissionState permissionState : permissionStates) {
                if (permissionState.isRuntime() && (permissionNames == null || permissionNames.contains(permissionState.getName()))) {
                    pw.print(prefix);
                    pw.print("  ");
                    pw.print(permissionState.getName());
                    pw.print(": granted=");
                    pw.print(permissionState.isGranted());
                    pw.println(permissionFlagsToString(", flags=", permissionState.getFlags()));
                }
            }
        }
    }

    private static String permissionFlagsToString(String prefix, int flags) {
        StringBuilder flagsString = null;
        while (flags != 0) {
            if (flagsString == null) {
                flagsString = new StringBuilder();
                flagsString.append(prefix);
                flagsString.append("[ ");
            }
            int flag = 1 << Integer.numberOfTrailingZeros(flags);
            flags &= ~flag;
            flagsString.append(PackageManager.permissionFlagToString(flag));
            if (flags != 0) {
                flagsString.append('|');
            }
        }
        if (flagsString != null) {
            flagsString.append(']');
            return flagsString.toString();
        }
        return "";
    }

    void dumpInstallPermissionsLPr(PrintWriter pw, String prefix, ArraySet<String> filterPermissionNames, LegacyPermissionState permissionsState, List<UserInfo> users) {
        LegacyPermissionState.PermissionState permissionState;
        ArraySet<String> dumpPermissionNames = new ArraySet<>();
        for (UserInfo user : users) {
            Collection<LegacyPermissionState.PermissionState> permissionStates = permissionsState.getPermissionStates(user.id);
            for (LegacyPermissionState.PermissionState permissionState2 : permissionStates) {
                if (!permissionState2.isRuntime()) {
                    String permissionName = permissionState2.getName();
                    if (filterPermissionNames == null || filterPermissionNames.contains(permissionName)) {
                        dumpPermissionNames.add(permissionName);
                    }
                }
            }
        }
        boolean printedSomething = false;
        Iterator<String> it = dumpPermissionNames.iterator();
        while (it.hasNext()) {
            String permissionName2 = it.next();
            LegacyPermissionState.PermissionState systemPermissionState = permissionsState.getPermissionState(permissionName2, 0);
            for (UserInfo user2 : users) {
                int userId = user2.id;
                if (userId == 0) {
                    permissionState = systemPermissionState;
                } else {
                    permissionState = permissionsState.getPermissionState(permissionName2, userId);
                    if (Objects.equals(permissionState, systemPermissionState)) {
                    }
                }
                if (!printedSomething) {
                    pw.print(prefix);
                    pw.println("install permissions:");
                    printedSomething = true;
                }
                pw.print(prefix);
                pw.print("  ");
                pw.print(permissionName2);
                pw.print(": granted=");
                pw.print(permissionState != null && permissionState.isGranted());
                pw.print(permissionFlagsToString(", flags=", permissionState != null ? permissionState.getFlags() : 0));
                if (userId != 0) {
                    pw.print(", userId=");
                    pw.println(userId);
                } else {
                    pw.println();
                }
            }
        }
    }

    void dumpComponents(PrintWriter pw, String prefix, PackageSetting ps) {
        dumpComponents(pw, prefix, "activities:", ps.getPkg().getActivities());
        dumpComponents(pw, prefix, "services:", ps.getPkg().getServices());
        dumpComponents(pw, prefix, "receivers:", ps.getPkg().getReceivers());
        dumpComponents(pw, prefix, "providers:", ps.getPkg().getProviders());
        dumpComponents(pw, prefix, "instrumentations:", ps.getPkg().getInstrumentations());
    }

    void dumpComponents(PrintWriter pw, String prefix, String label, List<? extends ParsedComponent> list) {
        int size = CollectionUtils.size(list);
        if (size == 0) {
            return;
        }
        pw.print(prefix);
        pw.println(label);
        for (int i = 0; i < size; i++) {
            ParsedComponent component = list.get(i);
            pw.print(prefix);
            pw.print("  ");
            pw.println(component.getComponentName().flattenToShortString());
        }
    }

    public void writePermissionStateForUserLPr(int userId, boolean sync) {
        if (sync) {
            this.mRuntimePermissionsPersistence.writeStateForUser(userId, this.mPermissionDataProvider, this.mPackages, this.mSharedUsers, null, this.mLock, true);
        } else {
            this.mRuntimePermissionsPersistence.writeStateForUserAsync(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class KeySetToValueMap<K, V> implements Map<K, V> {
        private final Set<K> mKeySet;
        private final V mValue;

        KeySetToValueMap(Set<K> keySet, V value) {
            this.mKeySet = keySet;
            this.mValue = value;
        }

        @Override // java.util.Map
        public int size() {
            return this.mKeySet.size();
        }

        @Override // java.util.Map
        public boolean isEmpty() {
            return this.mKeySet.isEmpty();
        }

        @Override // java.util.Map
        public boolean containsKey(Object key) {
            return this.mKeySet.contains(key);
        }

        @Override // java.util.Map
        public boolean containsValue(Object value) {
            return this.mValue == value;
        }

        @Override // java.util.Map
        public V get(Object key) {
            return this.mValue;
        }

        @Override // java.util.Map
        public V put(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override // java.util.Map
        public V remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override // java.util.Map
        public void putAll(Map<? extends K, ? extends V> m) {
            throw new UnsupportedOperationException();
        }

        @Override // java.util.Map
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override // java.util.Map
        public Set<K> keySet() {
            return this.mKeySet;
        }

        @Override // java.util.Map
        public Collection<V> values() {
            throw new UnsupportedOperationException();
        }

        @Override // java.util.Map
        public Set<Map.Entry<K, V>> entrySet() {
            throw new UnsupportedOperationException();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class RuntimePermissionPersistence {
        private static final int INITIAL_VERSION = 0;
        private static final long MAX_WRITE_PERMISSIONS_DELAY_MILLIS = 2000;
        private static final int UPGRADE_VERSION = -1;
        private static final double WRITE_PERMISSIONS_DELAY_JITTER = 0.3d;
        private static final long WRITE_PERMISSIONS_DELAY_MILLIS = 300;
        private static final Random sRandom = new Random();
        private String mExtendedFingerprint;
        private final Consumer<Integer> mInvokeWriteUserStateAsyncCallback;
        private final RuntimePermissionsPersistence mPersistence;
        private final Object mPersistenceLock = new Object();
        private final Handler mAsyncHandler = new MyHandler();
        private final Handler mPersistenceHandler = new Handler(BackgroundThread.getHandler().getLooper());
        private final Object mLock = new Object();
        private final SparseBooleanArray mWriteScheduled = new SparseBooleanArray();
        private final SparseLongArray mLastNotWrittenMutationTimesMillis = new SparseLongArray();
        private boolean mIsLegacyPermissionStateStale = false;
        private final SparseIntArray mVersions = new SparseIntArray();
        private final SparseArray<String> mFingerprints = new SparseArray<>();
        private final SparseBooleanArray mPermissionUpgradeNeeded = new SparseBooleanArray();
        private final SparseArray<RuntimePermissionsState> mPendingStatesToWrite = new SparseArray<>();

        public RuntimePermissionPersistence(RuntimePermissionsPersistence persistence, Consumer<Integer> invokeWriteUserStateAsyncCallback) {
            this.mPersistence = persistence;
            this.mInvokeWriteUserStateAsyncCallback = invokeWriteUserStateAsyncCallback;
        }

        int getVersion(int userId) {
            int i;
            synchronized (this.mLock) {
                i = this.mVersions.get(userId, 0);
            }
            return i;
        }

        void setVersion(int version, int userId) {
            synchronized (this.mLock) {
                this.mVersions.put(userId, version);
                writeStateForUserAsync(userId);
            }
        }

        public boolean isPermissionUpgradeNeeded(int userId) {
            boolean z;
            synchronized (this.mLock) {
                z = this.mPermissionUpgradeNeeded.get(userId, true);
            }
            return z;
        }

        public void updateRuntimePermissionsFingerprint(int userId) {
            synchronized (this.mLock) {
                String str = this.mExtendedFingerprint;
                if (str == null) {
                    throw new RuntimeException("The version of the permission controller hasn't been set before trying to update the fingerprint.");
                }
                this.mFingerprints.put(userId, str);
                writeStateForUserAsync(userId);
            }
        }

        public void setPermissionControllerVersion(long version) {
            synchronized (this.mLock) {
                int numUser = this.mFingerprints.size();
                this.mExtendedFingerprint = getExtendedFingerprint(version);
                for (int i = 0; i < numUser; i++) {
                    int userId = this.mFingerprints.keyAt(i);
                    String fingerprint = this.mFingerprints.valueAt(i);
                    this.mPermissionUpgradeNeeded.put(userId, !TextUtils.equals(this.mExtendedFingerprint, fingerprint));
                }
            }
        }

        private String getExtendedFingerprint(long version) {
            return PackagePartitions.FINGERPRINT + "?pc_version=" + version;
        }

        private static long uniformRandom(double low, double high) {
            double mag = high - low;
            return (long) ((sRandom.nextDouble() * mag) + low);
        }

        private static long nextWritePermissionDelayMillis() {
            return uniformRandom(-90.0d, 90.0d) + WRITE_PERMISSIONS_DELAY_MILLIS;
        }

        public void writeStateForUserAsync(int userId) {
            synchronized (this.mLock) {
                this.mIsLegacyPermissionStateStale = true;
                long currentTimeMillis = SystemClock.uptimeMillis();
                long writePermissionDelayMillis = nextWritePermissionDelayMillis();
                if (this.mWriteScheduled.get(userId)) {
                    this.mAsyncHandler.removeMessages(userId);
                    long lastNotWrittenMutationTimeMillis = this.mLastNotWrittenMutationTimesMillis.get(userId);
                    long timeSinceLastNotWrittenMutationMillis = currentTimeMillis - lastNotWrittenMutationTimeMillis;
                    if (timeSinceLastNotWrittenMutationMillis >= MAX_WRITE_PERMISSIONS_DELAY_MILLIS) {
                        this.mAsyncHandler.obtainMessage(userId).sendToTarget();
                        return;
                    }
                    long maxDelayMillis = Math.max((MAX_WRITE_PERMISSIONS_DELAY_MILLIS + lastNotWrittenMutationTimeMillis) - currentTimeMillis, 0L);
                    long writeDelayMillis = Math.min(writePermissionDelayMillis, maxDelayMillis);
                    Message message = this.mAsyncHandler.obtainMessage(userId);
                    this.mAsyncHandler.sendMessageDelayed(message, writeDelayMillis);
                } else {
                    this.mLastNotWrittenMutationTimesMillis.put(userId, currentTimeMillis);
                    Message message2 = this.mAsyncHandler.obtainMessage(userId);
                    this.mAsyncHandler.sendMessageDelayed(message2, writePermissionDelayMillis);
                    this.mWriteScheduled.put(userId, true);
                }
            }
        }

        public void writeStateForUser(final int userId, final LegacyPermissionDataProvider legacyPermissionDataProvider, final WatchedArrayMap<String, ? extends PackageStateInternal> packageStates, final WatchedArrayMap<String, SharedUserSetting> sharedUsers, final Handler pmHandler, final Object pmLock, final boolean sync) {
            final int version;
            final String fingerprint;
            final boolean isLegacyPermissionStateStale;
            synchronized (this.mLock) {
                this.mAsyncHandler.removeMessages(userId);
                this.mWriteScheduled.delete(userId);
                version = this.mVersions.get(userId, 0);
                fingerprint = this.mFingerprints.get(userId);
                isLegacyPermissionStateStale = this.mIsLegacyPermissionStateStale;
                this.mIsLegacyPermissionStateStale = false;
            }
            Runnable writer = new Runnable() { // from class: com.android.server.pm.Settings$RuntimePermissionPersistence$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    Settings.RuntimePermissionPersistence.this.m5613x296972f7(pmLock, sync, isLegacyPermissionStateStale, legacyPermissionDataProvider, packageStates, userId, sharedUsers, version, fingerprint, pmHandler);
                }
            };
            if (pmHandler != null) {
                pmHandler.post(writer);
            } else {
                writer.run();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$writeStateForUser$1$com-android-server-pm-Settings$RuntimePermissionPersistence  reason: not valid java name */
        public /* synthetic */ void m5613x296972f7(Object pmLock, boolean sync, boolean isLegacyPermissionStateStale, LegacyPermissionDataProvider legacyPermissionDataProvider, WatchedArrayMap packageStates, int userId, WatchedArrayMap sharedUsers, int version, String fingerprint, Handler pmHandler) {
            synchronized (pmLock) {
                try {
                    if (sync || isLegacyPermissionStateStale) {
                        try {
                            legacyPermissionDataProvider.writeLegacyPermissionStateTEMP();
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                    Map<String, List<RuntimePermissionsState.PermissionState>> packagePermissions = new ArrayMap<>();
                    int packagesSize = packageStates.size();
                    for (int i = 0; i < packagesSize; i++) {
                        String packageName = (String) packageStates.keyAt(i);
                        PackageStateInternal packageState = (PackageStateInternal) packageStates.valueAt(i);
                        if (!packageState.hasSharedUser()) {
                            List<RuntimePermissionsState.PermissionState> permissions = getPermissionsFromPermissionsState(packageState.getLegacyPermissionState(), userId);
                            if (!permissions.isEmpty() || packageState.isInstallPermissionsFixed()) {
                                packagePermissions.put(packageName, permissions);
                            }
                        }
                    }
                    Map<String, List<RuntimePermissionsState.PermissionState>> sharedUserPermissions = new ArrayMap<>();
                    int sharedUsersSize = sharedUsers.size();
                    for (int i2 = 0; i2 < sharedUsersSize; i2++) {
                        String sharedUserName = (String) sharedUsers.keyAt(i2);
                        SharedUserSetting sharedUserSetting = (SharedUserSetting) sharedUsers.valueAt(i2);
                        sharedUserPermissions.put(sharedUserName, getPermissionsFromPermissionsState(sharedUserSetting.getLegacyPermissionState(), userId));
                    }
                    RuntimePermissionsState runtimePermissions = new RuntimePermissionsState(version, fingerprint, packagePermissions, sharedUserPermissions);
                    synchronized (this.mLock) {
                        this.mPendingStatesToWrite.put(userId, runtimePermissions);
                    }
                    if (pmHandler != null) {
                        this.mPersistenceHandler.post(new Runnable() { // from class: com.android.server.pm.Settings$RuntimePermissionPersistence$$ExternalSyntheticLambda0
                            @Override // java.lang.Runnable
                            public final void run() {
                                Settings.RuntimePermissionPersistence.this.m5612x6ef3d276();
                            }
                        });
                    } else {
                        m5612x6ef3d276();
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: private */
        /* renamed from: writePendingStates */
        public void m5612x6ef3d276() {
            int userId;
            RuntimePermissionsState runtimePermissions;
            while (true) {
                synchronized (this.mLock) {
                    if (this.mPendingStatesToWrite.size() != 0) {
                        userId = this.mPendingStatesToWrite.keyAt(0);
                        runtimePermissions = this.mPendingStatesToWrite.valueAt(0);
                        this.mPendingStatesToWrite.removeAt(0);
                    } else {
                        return;
                    }
                }
                synchronized (this.mPersistenceLock) {
                    this.mPersistence.writeForUser(runtimePermissions, UserHandle.of(userId));
                }
            }
        }

        private List<RuntimePermissionsState.PermissionState> getPermissionsFromPermissionsState(LegacyPermissionState permissionsState, int userId) {
            Collection<LegacyPermissionState.PermissionState> permissionStates = permissionsState.getPermissionStates(userId);
            List<RuntimePermissionsState.PermissionState> permissions = new ArrayList<>();
            for (LegacyPermissionState.PermissionState permissionState : permissionStates) {
                RuntimePermissionsState.PermissionState permission = new RuntimePermissionsState.PermissionState(permissionState.getName(), permissionState.isGranted(), permissionState.getFlags());
                permissions.add(permission);
            }
            return permissions;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onUserRemoved(int userId) {
            synchronized (this.mLock) {
                this.mAsyncHandler.removeMessages(userId);
                this.mPermissionUpgradeNeeded.delete(userId);
                this.mVersions.delete(userId);
                this.mFingerprints.remove(userId);
            }
        }

        public void deleteUserRuntimePermissionsFile(int userId) {
            synchronized (this.mPersistenceLock) {
                this.mPersistence.deleteForUser(UserHandle.of(userId));
            }
        }

        public void readStateForUserSync(int userId, VersionInfo internalVersion, WatchedArrayMap<String, PackageSetting> packageSettings, WatchedArrayMap<String, SharedUserSetting> sharedUsers, File userRuntimePermissionsFile) {
            RuntimePermissionsState runtimePermissions;
            Map<String, List<RuntimePermissionsState.PermissionState>> sharedUserPermissions;
            int version;
            String fingerprint;
            WatchedArrayMap<String, PackageSetting> watchedArrayMap = packageSettings;
            synchronized (this.mPersistenceLock) {
                runtimePermissions = this.mPersistence.readForUser(UserHandle.of(userId));
            }
            if (runtimePermissions == null) {
                readLegacyStateForUserSync(userId, userRuntimePermissionsFile, watchedArrayMap, sharedUsers);
                writeStateForUserAsync(userId);
                return;
            }
            synchronized (this.mLock) {
                int version2 = runtimePermissions.getVersion();
                if (version2 == -1) {
                    version2 = -1;
                }
                this.mVersions.put(userId, version2);
                String fingerprint2 = runtimePermissions.getFingerprint();
                this.mFingerprints.put(userId, fingerprint2);
                boolean isUpgradeToR = internalVersion.sdkVersion < 30;
                Map<String, List<RuntimePermissionsState.PermissionState>> packagePermissions = runtimePermissions.getPackagePermissions();
                int packagesSize = packageSettings.size();
                int i = 0;
                while (i < packagesSize) {
                    String packageName = watchedArrayMap.keyAt(i);
                    PackageSetting packageSetting = watchedArrayMap.valueAt(i);
                    List<RuntimePermissionsState.PermissionState> permissions = packagePermissions.get(packageName);
                    if (permissions != null) {
                        version = version2;
                        readPermissionsState(permissions, packageSetting.getLegacyPermissionState(), userId);
                        packageSetting.setInstallPermissionsFixed(true);
                        fingerprint = fingerprint2;
                    } else {
                        version = version2;
                        if (packageSetting.hasSharedUser() || isUpgradeToR) {
                            fingerprint = fingerprint2;
                        } else {
                            fingerprint = fingerprint2;
                            Slogf.w(Settings.TAG, "Missing permission state for package %s on user %d", packageName, Integer.valueOf(userId));
                            packageSetting.getLegacyPermissionState().setMissing(true, userId);
                        }
                    }
                    i++;
                    watchedArrayMap = packageSettings;
                    fingerprint2 = fingerprint;
                    version2 = version;
                }
                Map<String, List<RuntimePermissionsState.PermissionState>> sharedUserPermissions2 = runtimePermissions.getSharedUserPermissions();
                int sharedUsersSize = sharedUsers.size();
                int i2 = 0;
                while (i2 < sharedUsersSize) {
                    String sharedUserName = sharedUsers.keyAt(i2);
                    SharedUserSetting sharedUserSetting = sharedUsers.valueAt(i2);
                    List<RuntimePermissionsState.PermissionState> permissions2 = sharedUserPermissions2.get(sharedUserName);
                    if (permissions2 != null) {
                        readPermissionsState(permissions2, sharedUserSetting.getLegacyPermissionState(), userId);
                        sharedUserPermissions = sharedUserPermissions2;
                    } else if (isUpgradeToR) {
                        sharedUserPermissions = sharedUserPermissions2;
                    } else {
                        sharedUserPermissions = sharedUserPermissions2;
                        Slog.w(Settings.TAG, "Missing permission state for shared user: " + sharedUserName);
                        sharedUserSetting.getLegacyPermissionState().setMissing(true, userId);
                    }
                    i2++;
                    sharedUserPermissions2 = sharedUserPermissions;
                }
            }
        }

        private void readPermissionsState(List<RuntimePermissionsState.PermissionState> permissions, LegacyPermissionState permissionsState, int userId) {
            int permissionsSize = permissions.size();
            for (int i = 0; i < permissionsSize; i++) {
                RuntimePermissionsState.PermissionState permission = permissions.get(i);
                String name = permission.getName();
                boolean granted = permission.isGranted();
                int flags = permission.getFlags();
                permissionsState.putPermissionState(new LegacyPermissionState.PermissionState(name, true, granted, flags), userId);
            }
        }

        private void readLegacyStateForUserSync(int userId, File permissionsFile, WatchedArrayMap<String, ? extends PackageStateInternal> packageStates, WatchedArrayMap<String, SharedUserSetting> sharedUsers) {
            synchronized (this.mLock) {
                if (permissionsFile.exists()) {
                    try {
                        FileInputStream in = new AtomicFile(permissionsFile).openRead();
                        try {
                            TypedXmlPullParser parser = Xml.resolvePullParser(in);
                            parseLegacyRuntimePermissions(parser, userId, packageStates, sharedUsers);
                            IoUtils.closeQuietly(in);
                        } catch (IOException | XmlPullParserException e) {
                            throw new IllegalStateException("Failed parsing permissions file: " + permissionsFile, e);
                        }
                    } catch (FileNotFoundException e2) {
                        Slog.i("PackageManager", "No permissions state");
                    }
                }
            }
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        private void parseLegacyRuntimePermissions(TypedXmlPullParser parser, int userId, WatchedArrayMap<String, ? extends PackageStateInternal> packageStates, WatchedArrayMap<String, SharedUserSetting> sharedUsers) throws IOException, XmlPullParserException {
            synchronized (this.mLock) {
                int outerDepth = parser.getDepth();
                while (true) {
                    int type = parser.next();
                    char c = 1;
                    if (type != 1 && (type != 3 || parser.getDepth() > outerDepth)) {
                        if (type != 3 && type != 4) {
                            String name = parser.getName();
                            switch (name.hashCode()) {
                                case 111052:
                                    if (name.equals(Settings.TAG_PACKAGE)) {
                                        break;
                                    }
                                    c = 65535;
                                    break;
                                case 160289295:
                                    if (name.equals(Settings.TAG_RUNTIME_PERMISSIONS)) {
                                        c = 0;
                                        break;
                                    }
                                    c = 65535;
                                    break;
                                case 485578803:
                                    if (name.equals(Settings.TAG_SHARED_USER)) {
                                        c = 2;
                                        break;
                                    }
                                    c = 65535;
                                    break;
                                default:
                                    c = 65535;
                                    break;
                            }
                            switch (c) {
                                case 0:
                                    int version = parser.getAttributeInt((String) null, "version", -1);
                                    this.mVersions.put(userId, version);
                                    String fingerprint = parser.getAttributeValue((String) null, Settings.ATTR_FINGERPRINT);
                                    this.mFingerprints.put(userId, fingerprint);
                                    break;
                                case 1:
                                    String name2 = parser.getAttributeValue((String) null, "name");
                                    PackageStateInternal ps = packageStates.get(name2);
                                    if (ps == null) {
                                        Slog.w("PackageManager", "Unknown package:" + name2);
                                        XmlUtils.skipCurrentTag(parser);
                                        break;
                                    } else {
                                        parseLegacyPermissionsLPr(parser, ps.getLegacyPermissionState(), userId);
                                        break;
                                    }
                                case 2:
                                    String name3 = parser.getAttributeValue((String) null, "name");
                                    SharedUserSetting sus = sharedUsers.get(name3);
                                    if (sus == null) {
                                        Slog.w("PackageManager", "Unknown shared user:" + name3);
                                        XmlUtils.skipCurrentTag(parser);
                                        break;
                                    } else {
                                        parseLegacyPermissionsLPr(parser, sus.getLegacyPermissionState(), userId);
                                        break;
                                    }
                            }
                        }
                    }
                }
            }
        }

        private void parseLegacyPermissionsLPr(TypedXmlPullParser parser, LegacyPermissionState permissionsState, int userId) throws IOException, XmlPullParserException {
            synchronized (this.mLock) {
                int outerDepth = parser.getDepth();
                while (true) {
                    int type = parser.next();
                    if (type != 1 && (type != 3 || parser.getDepth() > outerDepth)) {
                        if (type != 3 && type != 4) {
                            String name = parser.getName();
                            char c = 65535;
                            switch (name.hashCode()) {
                                case 3242771:
                                    if (name.equals(Settings.TAG_ITEM)) {
                                        c = 0;
                                        break;
                                    }
                            }
                            switch (c) {
                                case 0:
                                    String name2 = parser.getAttributeValue((String) null, "name");
                                    boolean granted = parser.getAttributeBoolean((String) null, Settings.ATTR_GRANTED, true);
                                    int flags = parser.getAttributeIntHex((String) null, Settings.ATTR_FLAGS, 0);
                                    permissionsState.putPermissionState(new LegacyPermissionState.PermissionState(name2, true, granted, flags), userId);
                                    break;
                            }
                        }
                    }
                }
            }
        }

        /* loaded from: classes2.dex */
        private final class MyHandler extends Handler {
            public MyHandler() {
                super(BackgroundThread.getHandler().getLooper());
            }

            @Override // android.os.Handler
            public void handleMessage(Message message) {
                int userId = message.what;
                Runnable callback = (Runnable) message.obj;
                RuntimePermissionPersistence.this.mInvokeWriteUserStateAsyncCallback.accept(Integer.valueOf(userId));
                if (callback != null) {
                    callback.run();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PersistentPreferredIntentResolver getPersistentPreferredActivities(int userId) {
        return this.mPersistentPreferredActivities.get(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PreferredIntentResolver getPreferredActivities(int userId) {
        return this.mPreferredActivities.get(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public CrossProfileIntentResolver getCrossProfileIntentResolver(int userId) {
        return this.mCrossProfileIntentResolvers.get(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearPackagePreferredActivities(String packageName, SparseBooleanArray outUserChanged, int userId) {
        boolean changed = false;
        ArrayList<PreferredActivity> removed = null;
        for (int i = 0; i < this.mPreferredActivities.size(); i++) {
            int thisUserId = this.mPreferredActivities.keyAt(i);
            PreferredIntentResolver pir = this.mPreferredActivities.valueAt(i);
            if (userId == -1 || userId == thisUserId) {
                Iterator<F> filterIterator = pir.filterIterator();
                while (filterIterator.hasNext()) {
                    PreferredActivity pa = (PreferredActivity) filterIterator.next();
                    if (packageName == null || (pa.mPref.mComponent.getPackageName().equals(packageName) && pa.mPref.mAlways)) {
                        if (removed == null) {
                            removed = new ArrayList<>();
                        }
                        removed.add(pa);
                    }
                }
                if (removed != null) {
                    for (int j = 0; j < removed.size(); j++) {
                        pir.removeFilter((PreferredIntentResolver) removed.get(j));
                    }
                    outUserChanged.put(thisUserId, true);
                    changed = true;
                }
            }
        }
        if (changed) {
            onChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean clearPackagePersistentPreferredActivities(String packageName, int userId) {
        ArrayList<PersistentPreferredActivity> removed = null;
        boolean changed = false;
        for (int i = 0; i < this.mPersistentPreferredActivities.size(); i++) {
            int thisUserId = this.mPersistentPreferredActivities.keyAt(i);
            PersistentPreferredIntentResolver ppir = this.mPersistentPreferredActivities.valueAt(i);
            if (userId == thisUserId) {
                Iterator<F> filterIterator = ppir.filterIterator();
                while (filterIterator.hasNext()) {
                    PersistentPreferredActivity ppa = (PersistentPreferredActivity) filterIterator.next();
                    if (ppa.mComponent.getPackageName().equals(packageName)) {
                        if (removed == null) {
                            removed = new ArrayList<>();
                        }
                        removed.add(ppa);
                    }
                }
                if (removed != null) {
                    for (int j = 0; j < removed.size(); j++) {
                        ppir.removeFilter((PersistentPreferredIntentResolver) removed.get(j));
                    }
                    changed = true;
                }
            }
        }
        if (changed) {
            onChanged();
        }
        return changed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayList<Integer> systemReady(ComponentResolver resolver) {
        ArrayList<Integer> changed = new ArrayList<>();
        ArrayList<PreferredActivity> removed = new ArrayList<>();
        for (int i = 0; i < this.mPreferredActivities.size(); i++) {
            PreferredIntentResolver pir = this.mPreferredActivities.valueAt(i);
            removed.clear();
            for (F pa : pir.filterSet()) {
                if (!resolver.isActivityDefined(pa.mPref.mComponent)) {
                    removed.add(pa);
                }
            }
            if (removed.size() > 0) {
                for (int r = 0; r < removed.size(); r++) {
                    PreferredActivity pa2 = removed.get(r);
                    Slog.w(TAG, "Removing dangling preferred activity: " + pa2.mPref.mComponent);
                    pir.removeFilter((PreferredIntentResolver) pa2);
                }
                changed.add(Integer.valueOf(this.mPreferredActivities.keyAt(i)));
            }
        }
        onChanged();
        return changed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpPreferred(PrintWriter pw, DumpState dumpState, String packageName) {
        String str;
        for (int i = 0; i < this.mPreferredActivities.size(); i++) {
            PreferredIntentResolver pir = this.mPreferredActivities.valueAt(i);
            int user = this.mPreferredActivities.keyAt(i);
            if (dumpState.getTitlePrinted()) {
                str = "\nPreferred Activities User " + user + ":";
            } else {
                str = "Preferred Activities User " + user + ":";
            }
            if (pir.dump(pw, str, "  ", packageName, true, false)) {
                dumpState.setTitlePrinted(true);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInstallerPackage(String packageName) {
        return this.mInstallerPackages.contains(packageName);
    }
}
