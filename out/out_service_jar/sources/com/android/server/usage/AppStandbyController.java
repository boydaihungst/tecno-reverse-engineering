package com.android.server.usage;

import android.app.ActivityManager;
import android.app.usage.AppStandbyInfo;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.app.usage.UsageStatsManagerInternal;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.CrossProfileAppsInternal;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.net.NetworkScoreManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IDeviceIdleController;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseLongArray;
import android.util.TimeUtils;
import android.widget.Toast;
import com.android.internal.app.IBatteryStats;
import com.android.internal.util.jobs.ArrayUtils;
import com.android.internal.util.jobs.ConcurrentUtils;
import com.android.server.AlarmManagerInternal;
import com.android.server.JobSchedulerBackgroundThread;
import com.android.server.LocalServices;
import com.android.server.am.HostingRecord;
import com.android.server.job.JobPackageTracker;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.usage.AppIdleHistory;
import com.android.server.usage.AppStandbyInternal;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import libcore.util.EmptyArray;
/* loaded from: classes2.dex */
public class AppStandbyController implements AppStandbyInternal, UsageStatsManagerInternal.UsageEventListener {
    static final boolean COMPRESS_TIME = false;
    static final boolean DEBUG = false;
    private static final long DEFAULT_PREDICTION_TIMEOUT = 43200000;
    private static final int HEADLESS_APP_CHECK_FLAGS = 1835520;
    static final int MSG_CHECK_IDLE_STATES = 5;
    static final int MSG_CHECK_PACKAGE_IDLE_STATE = 11;
    static final int MSG_FORCE_IDLE_STATE = 4;
    static final int MSG_INFORM_LISTENERS = 3;
    static final int MSG_ONE_TIME_CHECK_IDLE_STATES = 10;
    static final int MSG_PAROLE_STATE_CHANGED = 9;
    static final int MSG_REPORT_CONTENT_PROVIDER_USAGE = 8;
    static final int MSG_REPORT_EXEMPTED_SYNC_START = 13;
    static final int MSG_REPORT_SYNC_SCHEDULED = 12;
    static final int MSG_TRIGGER_LISTENER_QUOTA_BUMP = 7;
    private static final long NETWORK_SCORER_CACHE_DURATION_MILLIS = 5000;
    private static final long NOTIFICATION_SEEN_HOLD_DURATION_FOR_PRE_T_APPS = 43200000;
    private static final int NOTIFICATION_SEEN_PROMOTED_BUCKET_FOR_PRE_T_APPS = 20;
    private static final long ONE_DAY = 86400000;
    private static final long ONE_HOUR = 3600000;
    private static final long ONE_MINUTE = 60000;
    private static final int SYSTEM_PACKAGE_FLAGS = 542908416;
    private static final String TAG = "AppStandbyController";
    private static final long WAIT_FOR_ADMIN_DATA_TIMEOUT_MS = 10000;
    private final SparseArray<Set<String>> mActiveAdminApps;
    private final CountDownLatch mAdminDataAvailableLatch;
    private final SparseArray<Set<String>> mAdminProtectedPackages;
    private boolean mAllowRestrictedBucket;
    private volatile boolean mAppIdleEnabled;
    private AppIdleHistory mAppIdleHistory;
    private final Object mAppIdleLock;
    long[] mAppStandbyElapsedThresholds;
    private final Map<String, String> mAppStandbyProperties;
    long[] mAppStandbyScreenThresholds;
    private AppWidgetManager mAppWidgetManager;
    volatile String mBroadcastResponseExemptedPermissions;
    volatile List<String> mBroadcastResponseExemptedPermissionsList;
    volatile String mBroadcastResponseExemptedRoles;
    volatile List<String> mBroadcastResponseExemptedRolesList;
    volatile int mBroadcastResponseFgThresholdState;
    volatile long mBroadcastResponseWindowDurationMillis;
    volatile long mBroadcastSessionsDurationMs;
    volatile long mBroadcastSessionsWithResponseDurationMs;
    private String mCachedDeviceProvisioningPackage;
    private volatile String mCachedNetworkScorer;
    private volatile long mCachedNetworkScorerAtMillis;
    private List<String> mCarrierPrivilegedApps;
    private final Object mCarrierPrivilegedLock;
    long mCheckIdleIntervalMillis;
    private final Context mContext;
    private final DisplayManager.DisplayListener mDisplayListener;
    long mExemptedSyncScheduledDozeTimeoutMillis;
    long mExemptedSyncScheduledNonDozeTimeoutMillis;
    long mExemptedSyncStartTimeoutMillis;
    private final AppStandbyHandler mHandler;
    private boolean mHaveCarrierPrivilegedApps;
    private final ArraySet<String> mHeadlessSystemApps;
    long mInitialForegroundServiceStartTimeoutMillis;
    Injector mInjector;
    private volatile boolean mIsCharging;
    boolean mLinkCrossProfileApps;
    volatile boolean mNoteResponseEventForAllBroadcastSessions;
    int mNotificationSeenPromotedBucket;
    long mNotificationSeenTimeoutMillis;
    private final ArrayList<AppStandbyInternal.AppIdleStateChangeListener> mPackageAccessListeners;
    private PackageManager mPackageManager;
    private final SparseLongArray mPendingIdleStateChecks;
    private boolean mPendingInitializeDefaults;
    private volatile boolean mPendingOneTimeCheckIdleStates;
    long mPredictionTimeoutMillis;
    boolean mRetainNotificationSeenImpactForPreTApps;
    long mSlicePinnedTimeoutMillis;
    long mStrongUsageTimeoutMillis;
    long mSyncAdapterTimeoutMillis;
    long mSystemInteractionTimeoutMillis;
    private final ArrayList<Integer> mSystemPackagesAppIds;
    private boolean mSystemServicesReady;
    long mSystemUpdateUsageTimeoutMillis;
    private boolean mTriggerQuotaBumpOnNotificationSeen;
    long mUnexemptedSyncScheduledTimeoutMillis;
    static final long[] DEFAULT_SCREEN_TIME_THRESHOLDS = {0, 0, 3600000, ConstantsObserver.DEFAULT_SYSTEM_UPDATE_TIMEOUT, 21600000};
    static final long[] MINIMUM_SCREEN_TIME_THRESHOLDS = {0, 0, 0, 1800000, 3600000};
    static final long[] DEFAULT_ELAPSED_TIME_THRESHOLDS = {0, 43200000, 86400000, 172800000, 691200000};
    static final long[] MINIMUM_ELAPSED_TIME_THRESHOLDS = {0, 3600000, 3600000, ConstantsObserver.DEFAULT_SYSTEM_UPDATE_TIMEOUT, 14400000};
    private static final int[] THRESHOLD_BUCKETS = {10, 20, 30, 40, 45};

    /* loaded from: classes2.dex */
    static class Lock {
        Lock() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class Pool<T> {
        private final T[] mArray;
        private int mSize = 0;

        Pool(T[] array) {
            this.mArray = array;
        }

        synchronized T obtain() {
            T t;
            int i = this.mSize;
            if (i > 0) {
                T[] tArr = this.mArray;
                int i2 = i - 1;
                this.mSize = i2;
                t = tArr[i2];
            } else {
                t = null;
            }
            return t;
        }

        synchronized void recycle(T instance) {
            int i = this.mSize;
            T[] tArr = this.mArray;
            if (i < tArr.length) {
                this.mSize = i + 1;
                tArr[i] = instance;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class StandbyUpdateRecord {
        private static final Pool<StandbyUpdateRecord> sPool = new Pool<>(new StandbyUpdateRecord[10]);
        int bucket;
        boolean isUserInteraction;
        String packageName;
        int reason;
        int userId;

        private StandbyUpdateRecord() {
        }

        public static StandbyUpdateRecord obtain(String pkgName, int userId, int bucket, int reason, boolean isInteraction) {
            StandbyUpdateRecord r = sPool.obtain();
            if (r == null) {
                r = new StandbyUpdateRecord();
            }
            r.packageName = pkgName;
            r.userId = userId;
            r.bucket = bucket;
            r.reason = reason;
            r.isUserInteraction = isInteraction;
            return r;
        }

        public void recycle() {
            sPool.recycle(this);
        }
    }

    /* loaded from: classes2.dex */
    private static class ContentProviderUsageRecord {
        private static final Pool<ContentProviderUsageRecord> sPool = new Pool<>(new ContentProviderUsageRecord[10]);
        public String name;
        public String packageName;
        public int userId;

        private ContentProviderUsageRecord() {
        }

        public static ContentProviderUsageRecord obtain(String name, String packageName, int userId) {
            ContentProviderUsageRecord r = sPool.obtain();
            if (r == null) {
                r = new ContentProviderUsageRecord();
            }
            r.name = name;
            r.packageName = packageName;
            r.userId = userId;
            return r;
        }

        public void recycle() {
            sPool.recycle(this);
        }
    }

    public AppStandbyController(Context context) {
        this(new Injector(context, JobSchedulerBackgroundThread.get().getLooper()));
    }

    AppStandbyController(Injector injector) {
        Lock lock = new Lock();
        this.mAppIdleLock = lock;
        this.mPackageAccessListeners = new ArrayList<>();
        this.mCarrierPrivilegedLock = new Lock();
        this.mActiveAdminApps = new SparseArray<>();
        this.mAdminProtectedPackages = new SparseArray<>();
        this.mHeadlessSystemApps = new ArraySet<>();
        this.mAdminDataAvailableLatch = new CountDownLatch(1);
        this.mPendingIdleStateChecks = new SparseLongArray();
        this.mCachedNetworkScorer = null;
        this.mCachedNetworkScorerAtMillis = 0L;
        this.mCachedDeviceProvisioningPackage = null;
        long[] jArr = DEFAULT_ELAPSED_TIME_THRESHOLDS;
        this.mCheckIdleIntervalMillis = Math.min(jArr[1] / 4, 14400000L);
        this.mAppStandbyScreenThresholds = DEFAULT_SCREEN_TIME_THRESHOLDS;
        this.mAppStandbyElapsedThresholds = jArr;
        this.mStrongUsageTimeoutMillis = 3600000L;
        this.mNotificationSeenTimeoutMillis = 43200000L;
        this.mSlicePinnedTimeoutMillis = 43200000L;
        this.mNotificationSeenPromotedBucket = 20;
        this.mTriggerQuotaBumpOnNotificationSeen = false;
        this.mRetainNotificationSeenImpactForPreTApps = false;
        this.mSystemUpdateUsageTimeoutMillis = ConstantsObserver.DEFAULT_SYSTEM_UPDATE_TIMEOUT;
        this.mPredictionTimeoutMillis = 43200000L;
        this.mSyncAdapterTimeoutMillis = 600000L;
        this.mExemptedSyncScheduledNonDozeTimeoutMillis = 600000L;
        this.mExemptedSyncScheduledDozeTimeoutMillis = 14400000L;
        this.mExemptedSyncStartTimeoutMillis = 600000L;
        this.mUnexemptedSyncScheduledTimeoutMillis = 600000L;
        this.mSystemInteractionTimeoutMillis = 600000L;
        this.mInitialForegroundServiceStartTimeoutMillis = 1800000L;
        this.mLinkCrossProfileApps = true;
        this.mBroadcastResponseWindowDurationMillis = 120000L;
        this.mBroadcastResponseFgThresholdState = 2;
        this.mBroadcastSessionsDurationMs = 120000L;
        this.mBroadcastSessionsWithResponseDurationMs = 120000L;
        this.mNoteResponseEventForAllBroadcastSessions = true;
        this.mBroadcastResponseExemptedRoles = "";
        this.mBroadcastResponseExemptedRolesList = Collections.EMPTY_LIST;
        this.mBroadcastResponseExemptedPermissions = "";
        this.mBroadcastResponseExemptedPermissionsList = Collections.EMPTY_LIST;
        this.mAppStandbyProperties = new ArrayMap();
        this.mSystemPackagesAppIds = new ArrayList<>();
        this.mSystemServicesReady = false;
        this.mDisplayListener = new DisplayManager.DisplayListener() { // from class: com.android.server.usage.AppStandbyController.1
            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayAdded(int displayId) {
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayRemoved(int displayId) {
            }

            @Override // android.hardware.display.DisplayManager.DisplayListener
            public void onDisplayChanged(int displayId) {
                if (displayId == 0) {
                    boolean displayOn = AppStandbyController.this.isDisplayOn();
                    synchronized (AppStandbyController.this.mAppIdleLock) {
                        AppStandbyController.this.mAppIdleHistory.updateDisplay(displayOn, AppStandbyController.this.mInjector.elapsedRealtime());
                    }
                }
            }
        };
        this.mInjector = injector;
        Context context = injector.getContext();
        this.mContext = context;
        AppStandbyHandler appStandbyHandler = new AppStandbyHandler(this.mInjector.getLooper());
        this.mHandler = appStandbyHandler;
        this.mPackageManager = context.getPackageManager();
        DeviceStateReceiver deviceStateReceiver = new DeviceStateReceiver();
        IntentFilter deviceStates = new IntentFilter("android.os.action.CHARGING");
        deviceStates.addAction("android.os.action.DISCHARGING");
        deviceStates.addAction("android.os.action.POWER_SAVE_WHITELIST_CHANGED");
        context.registerReceiver(deviceStateReceiver, deviceStates);
        synchronized (lock) {
            this.mAppIdleHistory = new AppIdleHistory(this.mInjector.getDataSystemDirectory(), this.mInjector.elapsedRealtime());
        }
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
        packageFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        packageFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        packageFilter.addDataScheme("package");
        context.registerReceiverAsUser(new PackageReceiver(), UserHandle.ALL, packageFilter, null, appStandbyHandler);
    }

    void setAppIdleEnabled(boolean enabled) {
        UsageStatsManagerInternal usmi = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        if (enabled) {
            usmi.registerListener(this);
        } else {
            usmi.unregisterListener(this);
        }
        synchronized (this.mAppIdleLock) {
            if (this.mAppIdleEnabled != enabled) {
                boolean oldParoleState = isInParole();
                this.mAppIdleEnabled = enabled;
                if (isInParole() != oldParoleState) {
                    postParoleStateChanged();
                }
            }
        }
    }

    public boolean isAppIdleEnabled() {
        return this.mAppIdleEnabled;
    }

    public void onBootPhase(int phase) {
        boolean userFileExists;
        this.mInjector.onBootPhase(phase);
        if (phase == 500) {
            Slog.d(TAG, "Setting app idle enabled state");
            if (this.mAppIdleEnabled) {
                ((UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class)).registerListener(this);
            }
            ConstantsObserver settingsObserver = new ConstantsObserver(this.mHandler);
            settingsObserver.start();
            this.mAppWidgetManager = (AppWidgetManager) this.mContext.getSystemService(AppWidgetManager.class);
            this.mInjector.registerDisplayListener(this.mDisplayListener, this.mHandler);
            synchronized (this.mAppIdleLock) {
                this.mAppIdleHistory.updateDisplay(isDisplayOn(), this.mInjector.elapsedRealtime());
            }
            this.mSystemServicesReady = true;
            synchronized (this.mAppIdleLock) {
                userFileExists = this.mAppIdleHistory.userFileExists(0);
            }
            if (this.mPendingInitializeDefaults || !userFileExists) {
                initializeDefaultsForSystemApps(0);
            }
            if (this.mPendingOneTimeCheckIdleStates) {
                postOneTimeCheckIdleStates();
            }
            List<ApplicationInfo> systemApps = this.mPackageManager.getInstalledApplications(SYSTEM_PACKAGE_FLAGS);
            int size = systemApps.size();
            for (int i = 0; i < size; i++) {
                ApplicationInfo appInfo = systemApps.get(i);
                this.mSystemPackagesAppIds.add(Integer.valueOf(UserHandle.getAppId(appInfo.uid)));
            }
        } else if (phase == 1000) {
            setChargingState(this.mInjector.isCharging());
            this.mHandler.post(new Runnable() { // from class: com.android.server.usage.AppStandbyController$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    AppStandbyController.this.updatePowerWhitelistCache();
                }
            });
            this.mHandler.post(new Runnable() { // from class: com.android.server.usage.AppStandbyController$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    AppStandbyController.this.loadHeadlessSystemAppCache();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:30:? -> B:20:0x0068). Please submit an issue!!! */
    public void reportContentProviderUsage(String authority, String providerPkgName, int userId) {
        int i;
        Object obj;
        if (this.mAppIdleEnabled) {
            String[] packages = ContentResolver.getSyncAdapterPackagesForAuthorityAsUser(authority, userId);
            PackageManagerInternal pmi = this.mInjector.getPackageManagerInternal();
            long elapsedRealtime = this.mInjector.elapsedRealtime();
            int length = packages.length;
            int i2 = 0;
            while (i2 < length) {
                String packageName = packages[i2];
                if (packageName.equals(providerPkgName)) {
                    i = i2;
                } else {
                    int appId = UserHandle.getAppId(pmi.getPackageUid(packageName, 0L, userId));
                    if (this.mSystemPackagesAppIds.contains(Integer.valueOf(appId))) {
                        List<UserHandle> linkedProfiles = getCrossProfileTargets(packageName, userId);
                        Object obj2 = this.mAppIdleLock;
                        synchronized (obj2) {
                            try {
                                obj = obj2;
                                i = i2;
                                try {
                                    reportNoninteractiveUsageCrossUserLocked(packageName, userId, 10, 8, elapsedRealtime, this.mSyncAdapterTimeoutMillis, linkedProfiles);
                                } catch (Throwable th) {
                                    th = th;
                                    throw th;
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                obj = obj2;
                                throw th;
                            }
                        }
                    } else {
                        i = i2;
                    }
                }
                i2 = i + 1;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportExemptedSyncScheduled(String packageName, int userId) {
        int bucketToPromote;
        int usageReason;
        long durationMillis;
        if (this.mAppIdleEnabled) {
            if (!this.mInjector.isDeviceIdleMode()) {
                bucketToPromote = 10;
                usageReason = 11;
                durationMillis = this.mExemptedSyncScheduledNonDozeTimeoutMillis;
            } else {
                bucketToPromote = 20;
                usageReason = 12;
                durationMillis = this.mExemptedSyncScheduledDozeTimeoutMillis;
            }
            long elapsedRealtime = this.mInjector.elapsedRealtime();
            List<UserHandle> linkedProfiles = getCrossProfileTargets(packageName, userId);
            synchronized (this.mAppIdleLock) {
                reportNoninteractiveUsageCrossUserLocked(packageName, userId, bucketToPromote, usageReason, elapsedRealtime, durationMillis, linkedProfiles);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportUnexemptedSyncScheduled(String packageName, int userId) {
        if (this.mAppIdleEnabled) {
            long elapsedRealtime = this.mInjector.elapsedRealtime();
            synchronized (this.mAppIdleLock) {
                try {
                    try {
                        int currentBucket = this.mAppIdleHistory.getAppStandbyBucket(packageName, userId, elapsedRealtime);
                        if (currentBucket == 50) {
                            List<UserHandle> linkedProfiles = getCrossProfileTargets(packageName, userId);
                            reportNoninteractiveUsageCrossUserLocked(packageName, userId, 20, 14, elapsedRealtime, this.mUnexemptedSyncScheduledTimeoutMillis, linkedProfiles);
                        }
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportExemptedSyncStart(String packageName, int userId) {
        if (this.mAppIdleEnabled) {
            long elapsedRealtime = this.mInjector.elapsedRealtime();
            List<UserHandle> linkedProfiles = getCrossProfileTargets(packageName, userId);
            synchronized (this.mAppIdleLock) {
                reportNoninteractiveUsageCrossUserLocked(packageName, userId, 10, 13, elapsedRealtime, this.mExemptedSyncStartTimeoutMillis, linkedProfiles);
            }
        }
    }

    private void reportNoninteractiveUsageCrossUserLocked(String packageName, int userId, int bucket, int subReason, long elapsedRealtime, long nextCheckDelay, List<UserHandle> otherProfiles) {
        reportNoninteractiveUsageLocked(packageName, userId, bucket, subReason, elapsedRealtime, nextCheckDelay);
        int size = otherProfiles.size();
        for (int profileIndex = 0; profileIndex < size; profileIndex++) {
            int otherUserId = otherProfiles.get(profileIndex).getIdentifier();
            reportNoninteractiveUsageLocked(packageName, otherUserId, bucket, subReason, elapsedRealtime, nextCheckDelay);
        }
    }

    private void reportNoninteractiveUsageLocked(String packageName, int userId, int bucket, int subReason, long elapsedRealtime, long nextCheckDelay) {
        AppIdleHistory.AppUsageHistory appUsage = this.mAppIdleHistory.reportUsage(packageName, userId, bucket, subReason, 0L, elapsedRealtime + nextCheckDelay);
        AppStandbyHandler appStandbyHandler = this.mHandler;
        appStandbyHandler.sendMessageDelayed(appStandbyHandler.obtainMessage(11, userId, -1, packageName), nextCheckDelay);
        maybeInformListeners(packageName, userId, elapsedRealtime, appUsage.currentBucket, appUsage.bucketingReason, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void triggerListenerQuotaBump(String packageName, int userId) {
        if (this.mAppIdleEnabled) {
            synchronized (this.mPackageAccessListeners) {
                Iterator<AppStandbyInternal.AppIdleStateChangeListener> it = this.mPackageAccessListeners.iterator();
                while (it.hasNext()) {
                    AppStandbyInternal.AppIdleStateChangeListener listener = it.next();
                    listener.triggerTemporaryQuotaBump(packageName, userId);
                }
            }
        }
    }

    void setChargingState(boolean isCharging) {
        if (this.mIsCharging != isCharging) {
            this.mIsCharging = isCharging;
            postParoleStateChanged();
        }
    }

    public boolean isInParole() {
        return !this.mAppIdleEnabled || this.mIsCharging;
    }

    private void postParoleStateChanged() {
        this.mHandler.removeMessages(9);
        this.mHandler.sendEmptyMessage(9);
    }

    public void postCheckIdleStates(int userId) {
        if (userId == -1) {
            postOneTimeCheckIdleStates();
            return;
        }
        synchronized (this.mPendingIdleStateChecks) {
            this.mPendingIdleStateChecks.put(userId, this.mInjector.elapsedRealtime());
        }
        this.mHandler.obtainMessage(5).sendToTarget();
    }

    public void postOneTimeCheckIdleStates() {
        if (this.mInjector.getBootPhase() < 500) {
            this.mPendingOneTimeCheckIdleStates = true;
            return;
        }
        this.mHandler.sendEmptyMessage(10);
        this.mPendingOneTimeCheckIdleStates = false;
    }

    boolean checkIdleStates(int checkUserId) {
        if (this.mAppIdleEnabled) {
            try {
                int[] runningUserIds = this.mInjector.getRunningUserIds();
                if (checkUserId != -1) {
                    if (!ArrayUtils.contains(runningUserIds, checkUserId)) {
                        return false;
                    }
                }
                long elapsedRealtime = this.mInjector.elapsedRealtime();
                for (int userId : runningUserIds) {
                    if (checkUserId == -1 || checkUserId == userId) {
                        List<PackageInfo> packages = this.mPackageManager.getInstalledPackagesAsUser(512, userId);
                        int packageCount = packages.size();
                        for (int p = 0; p < packageCount; p++) {
                            PackageInfo pi = packages.get(p);
                            String packageName = pi.packageName;
                            checkAndUpdateStandbyState(packageName, userId, pi.applicationInfo.uid, elapsedRealtime);
                        }
                    }
                }
                return true;
            } catch (RemoteException re) {
                throw re.rethrowFromSystemServer();
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:68:0x0105  */
    /* JADX WARN: Removed duplicated region for block: B:69:0x0108  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void checkAndUpdateStandbyState(String packageName, int userId, int uid, long elapsedRealtime) {
        int uid2;
        int newBucket;
        int newBucket2;
        int newBucket3;
        int newBucket4;
        Object obj;
        if (uid > 0) {
            uid2 = uid;
        } else {
            try {
                uid2 = this.mPackageManager.getPackageUidAsUser(packageName, userId);
            } catch (PackageManager.NameNotFoundException e) {
                return;
            }
        }
        int minBucket = getAppMinBucket(packageName, UserHandle.getAppId(uid2), userId);
        if (minBucket <= 10) {
            synchronized (this.mAppIdleLock) {
                this.mAppIdleHistory.setAppStandbyBucket(packageName, userId, elapsedRealtime, minBucket, 256);
            }
            maybeInformListeners(packageName, userId, elapsedRealtime, minBucket, 256, false);
            return;
        }
        Object obj2 = this.mAppIdleLock;
        synchronized (obj2) {
            try {
                try {
                    AppIdleHistory.AppUsageHistory app = this.mAppIdleHistory.getAppUsageHistory(packageName, userId, elapsedRealtime);
                    int reason = app.bucketingReason;
                    int oldMainReason = reason & JobPackageTracker.EVENT_STOP_REASON_MASK;
                    if (oldMainReason == 1024) {
                        return;
                    }
                    int oldBucket = app.currentBucket;
                    if (oldBucket == 50) {
                        return;
                    }
                    int newBucket5 = Math.max(oldBucket, 10);
                    boolean predictionLate = predictionTimedOut(app, elapsedRealtime);
                    if (oldMainReason == 256 || oldMainReason == 768 || oldMainReason == 512 || predictionLate) {
                        if (!predictionLate && app.lastPredictedBucket >= 10 && app.lastPredictedBucket <= 40) {
                            newBucket5 = app.lastPredictedBucket;
                            reason = UsbTerminalTypes.TERMINAL_TELE_PHONELINE;
                        } else {
                            newBucket5 = getBucketForLocked(packageName, userId, elapsedRealtime);
                            reason = 512;
                        }
                    }
                    long elapsedTimeAdjusted = this.mAppIdleHistory.getElapsedTime(elapsedRealtime);
                    int reason2 = reason;
                    int bucketWithValidExpiryTime = getMinBucketWithValidExpiryTime(app, newBucket5, elapsedTimeAdjusted);
                    int newBucket6 = newBucket5;
                    if (bucketWithValidExpiryTime != -1) {
                        newBucket = bucketWithValidExpiryTime;
                        if (newBucket != 10 && app.currentBucket != newBucket) {
                            reason2 = UsbTerminalTypes.TERMINAL_OUT_LFSPEAKER;
                        }
                        reason2 = app.bucketingReason;
                    } else {
                        newBucket = newBucket6;
                    }
                    int newBucket7 = newBucket;
                    int reason3 = reason2;
                    if (app.lastUsedByUserElapsedTime >= 0 && app.lastRestrictAttemptElapsedTime > app.lastUsedByUserElapsedTime && elapsedTimeAdjusted - app.lastUsedByUserElapsedTime >= this.mInjector.getAutoRestrictedBucketDelayMs()) {
                        newBucket2 = 45;
                        newBucket3 = app.lastRestrictReason;
                        if (newBucket2 == 45 && !this.mAllowRestrictedBucket) {
                            newBucket2 = 40;
                        }
                        if (newBucket2 > minBucket) {
                            newBucket4 = newBucket2;
                        } else {
                            newBucket4 = minBucket;
                        }
                        if (oldBucket == newBucket4 && !predictionLate) {
                            obj = obj2;
                        }
                        int newBucket8 = newBucket4;
                        this.mAppIdleHistory.setAppStandbyBucket(packageName, userId, elapsedRealtime, newBucket4, newBucket3);
                        obj = obj2;
                        maybeInformListeners(packageName, userId, elapsedRealtime, newBucket8, newBucket3, false);
                    }
                    newBucket2 = newBucket7;
                    newBucket3 = reason3;
                    if (newBucket2 == 45) {
                        newBucket2 = 40;
                    }
                    if (newBucket2 > minBucket) {
                    }
                    if (oldBucket == newBucket4) {
                        obj = obj2;
                    }
                    int newBucket82 = newBucket4;
                    this.mAppIdleHistory.setAppStandbyBucket(packageName, userId, elapsedRealtime, newBucket4, newBucket3);
                    obj = obj2;
                    maybeInformListeners(packageName, userId, elapsedRealtime, newBucket82, newBucket3, false);
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    private boolean predictionTimedOut(AppIdleHistory.AppUsageHistory app, long elapsedRealtime) {
        return app.lastPredictedTime > 0 && this.mAppIdleHistory.getElapsedTime(elapsedRealtime) - app.lastPredictedTime > this.mPredictionTimeoutMillis;
    }

    private void maybeInformListeners(String packageName, int userId, long elapsedRealtime, int bucket, int reason, boolean userStartedInteracting) {
        synchronized (this.mAppIdleLock) {
            if (this.mAppIdleHistory.shouldInformListeners(packageName, userId, elapsedRealtime, bucket)) {
                StandbyUpdateRecord r = StandbyUpdateRecord.obtain(packageName, userId, bucket, reason, userStartedInteracting);
                AppStandbyHandler appStandbyHandler = this.mHandler;
                appStandbyHandler.sendMessage(appStandbyHandler.obtainMessage(3, r));
            }
        }
    }

    private int getBucketForLocked(String packageName, int userId, long elapsedRealtime) {
        int bucketIndex = this.mAppIdleHistory.getThresholdIndex(packageName, userId, elapsedRealtime, this.mAppStandbyScreenThresholds, this.mAppStandbyElapsedThresholds);
        if (bucketIndex >= 0) {
            return THRESHOLD_BUCKETS[bucketIndex];
        }
        return 50;
    }

    private void notifyBatteryStats(String packageName, int userId, boolean idle) {
        try {
            int uid = this.mPackageManager.getPackageUidAsUser(packageName, 8192, userId);
            if (idle) {
                this.mInjector.noteEvent(15, packageName, uid);
            } else {
                this.mInjector.noteEvent(16, packageName, uid);
            }
        } catch (PackageManager.NameNotFoundException | RemoteException e) {
        }
    }

    @Override // android.app.usage.UsageStatsManagerInternal.UsageEventListener
    public void onUsageEvent(int userId, UsageEvents.Event event) {
        if (this.mAppIdleEnabled) {
            int eventType = event.getEventType();
            if (eventType == 1 || eventType == 2 || eventType == 6 || eventType == 7 || eventType == 10 || eventType == 14 || eventType == 13 || eventType == 19) {
                String pkg = event.getPackageName();
                List<UserHandle> linkedProfiles = getCrossProfileTargets(pkg, userId);
                Object obj = this.mAppIdleLock;
                synchronized (obj) {
                    try {
                        try {
                            long elapsedRealtime = this.mInjector.elapsedRealtime();
                            reportEventLocked(pkg, eventType, elapsedRealtime, userId);
                            int size = linkedProfiles.size();
                            int profileIndex = 0;
                            while (profileIndex < size) {
                                int linkedUserId = linkedProfiles.get(profileIndex).getIdentifier();
                                List<UserHandle> linkedProfiles2 = linkedProfiles;
                                Object obj2 = obj;
                                reportEventLocked(pkg, eventType, elapsedRealtime, linkedUserId);
                                profileIndex++;
                                linkedProfiles = linkedProfiles2;
                                obj = obj2;
                            }
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        Object obj3 = obj;
                        throw th;
                    }
                }
            }
        }
    }

    private void reportEventLocked(String pkg, int eventType, long elapsedRealtime, int userId) {
        int prevBucketReason;
        int prevBucketReason2;
        int reason;
        long nextCheckDelay;
        int i;
        String str;
        int notificationSeenPromotedBucket;
        long notificationSeenTimeoutMillis;
        boolean previouslyIdle = this.mAppIdleHistory.isIdle(pkg, userId, elapsedRealtime);
        AppIdleHistory.AppUsageHistory appHistory = this.mAppIdleHistory.getAppUsageHistory(pkg, userId, elapsedRealtime);
        int prevBucket = appHistory.currentBucket;
        int prevBucketReason3 = appHistory.bucketingReason;
        int subReason = usageEventToSubReason(eventType);
        int reason2 = subReason | 768;
        if (eventType != 10) {
            prevBucketReason = prevBucketReason3;
            prevBucketReason2 = -1;
            reason = reason2;
            if (eventType == 14) {
                this.mAppIdleHistory.reportUsage(appHistory, pkg, userId, 20, subReason, 0L, elapsedRealtime + this.mSlicePinnedTimeoutMillis);
                nextCheckDelay = this.mSlicePinnedTimeoutMillis;
            } else if (eventType == 6) {
                this.mAppIdleHistory.reportUsage(appHistory, pkg, userId, 10, subReason, 0L, elapsedRealtime + this.mSystemInteractionTimeoutMillis);
                nextCheckDelay = this.mSystemInteractionTimeoutMillis;
            } else if (eventType != 19) {
                this.mAppIdleHistory.reportUsage(appHistory, pkg, userId, 10, subReason, elapsedRealtime, elapsedRealtime + this.mStrongUsageTimeoutMillis);
                nextCheckDelay = this.mStrongUsageTimeoutMillis;
            } else if (prevBucket != 50) {
                return;
            } else {
                this.mAppIdleHistory.reportUsage(appHistory, pkg, userId, 10, subReason, 0L, elapsedRealtime + this.mInitialForegroundServiceStartTimeoutMillis);
                nextCheckDelay = this.mInitialForegroundServiceStartTimeoutMillis;
            }
        } else {
            if (this.mRetainNotificationSeenImpactForPreTApps && getTargetSdkVersion(pkg) < 33) {
                notificationSeenPromotedBucket = 20;
                notificationSeenTimeoutMillis = 43200000;
            } else {
                if (this.mTriggerQuotaBumpOnNotificationSeen) {
                    this.mHandler.obtainMessage(7, userId, -1, pkg).sendToTarget();
                }
                int notificationSeenPromotedBucket2 = this.mNotificationSeenPromotedBucket;
                notificationSeenPromotedBucket = notificationSeenPromotedBucket2;
                notificationSeenTimeoutMillis = this.mNotificationSeenTimeoutMillis;
            }
            prevBucketReason = prevBucketReason3;
            prevBucketReason2 = -1;
            reason = reason2;
            this.mAppIdleHistory.reportUsage(appHistory, pkg, userId, notificationSeenPromotedBucket, subReason, 0L, elapsedRealtime + notificationSeenTimeoutMillis);
            nextCheckDelay = notificationSeenTimeoutMillis;
        }
        if (appHistory.currentBucket != prevBucket) {
            AppStandbyHandler appStandbyHandler = this.mHandler;
            appStandbyHandler.sendMessageDelayed(appStandbyHandler.obtainMessage(11, userId, prevBucketReason2, pkg), nextCheckDelay);
            boolean userStartedInteracting = appHistory.currentBucket == 10 && (prevBucketReason & JobPackageTracker.EVENT_STOP_REASON_MASK) != 768;
            i = userId;
            str = pkg;
            maybeInformListeners(pkg, userId, elapsedRealtime, appHistory.currentBucket, reason, userStartedInteracting);
        } else {
            i = userId;
            str = pkg;
        }
        if (previouslyIdle) {
            notifyBatteryStats(str, i, false);
        }
    }

    private int getTargetSdkVersion(String packageName) {
        return this.mInjector.getPackageManagerInternal().getPackageTargetSdkVersion(packageName);
    }

    private int getMinBucketWithValidExpiryTime(AppIdleHistory.AppUsageHistory usageHistory, int targetBucket, long elapsedTimeMs) {
        if (usageHistory.bucketExpiryTimesMs == null) {
            return -1;
        }
        int size = usageHistory.bucketExpiryTimesMs.size();
        for (int i = 0; i < size; i++) {
            int bucket = usageHistory.bucketExpiryTimesMs.keyAt(i);
            if (targetBucket <= bucket) {
                break;
            }
            long expiryTimeMs = usageHistory.bucketExpiryTimesMs.valueAt(i);
            if (expiryTimeMs > elapsedTimeMs) {
                return bucket;
            }
        }
        return -1;
    }

    private List<UserHandle> getCrossProfileTargets(String pkg, int userId) {
        synchronized (this.mAppIdleLock) {
            if (this.mLinkCrossProfileApps) {
                return this.mInjector.getValidCrossProfileTargets(pkg, userId);
            }
            return Collections.emptyList();
        }
    }

    private int usageEventToSubReason(int eventType) {
        switch (eventType) {
            case 1:
                return 4;
            case 2:
                return 5;
            case 6:
                return 1;
            case 7:
                return 3;
            case 10:
                return 2;
            case 13:
                return 10;
            case 14:
                return 9;
            case 19:
                return 15;
            default:
                return 0;
        }
    }

    void forceIdleState(String packageName, int userId, boolean idle) {
        int appId;
        int standbyBucket;
        if (this.mAppIdleEnabled && (appId = getAppId(packageName)) >= 0) {
            int minBucket = getAppMinBucket(packageName, appId, userId);
            if (idle && minBucket < 40) {
                Slog.e(TAG, "Tried to force an app to be idle when its min bucket is " + UsageStatsManager.standbyBucketToString(minBucket));
                return;
            }
            long elapsedRealtime = this.mInjector.elapsedRealtime();
            boolean previouslyIdle = isAppIdleFiltered(packageName, appId, userId, elapsedRealtime);
            synchronized (this.mAppIdleLock) {
                try {
                    standbyBucket = this.mAppIdleHistory.setIdle(packageName, userId, idle, elapsedRealtime);
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
            boolean stillIdle = isAppIdleFiltered(packageName, appId, userId, elapsedRealtime);
            maybeInformListeners(packageName, userId, elapsedRealtime, standbyBucket, 1024, false);
            if (previouslyIdle != stillIdle) {
                notifyBatteryStats(packageName, userId, stillIdle);
            }
        }
    }

    public void setLastJobRunTime(String packageName, int userId, long elapsedRealtime) {
        synchronized (this.mAppIdleLock) {
            this.mAppIdleHistory.setLastJobRunTime(packageName, userId, elapsedRealtime);
        }
    }

    public long getTimeSinceLastJobRun(String packageName, int userId) {
        long timeSinceLastJobRun;
        long elapsedRealtime = this.mInjector.elapsedRealtime();
        synchronized (this.mAppIdleLock) {
            timeSinceLastJobRun = this.mAppIdleHistory.getTimeSinceLastJobRun(packageName, userId, elapsedRealtime);
        }
        return timeSinceLastJobRun;
    }

    public void setEstimatedLaunchTime(String packageName, int userId, long launchTime) {
        long nowElapsed = this.mInjector.elapsedRealtime();
        synchronized (this.mAppIdleLock) {
            this.mAppIdleHistory.setEstimatedLaunchTime(packageName, userId, nowElapsed, launchTime);
        }
    }

    public long getEstimatedLaunchTime(String packageName, int userId) {
        long estimatedLaunchTime;
        long elapsedRealtime = this.mInjector.elapsedRealtime();
        synchronized (this.mAppIdleLock) {
            estimatedLaunchTime = this.mAppIdleHistory.getEstimatedLaunchTime(packageName, userId, elapsedRealtime);
        }
        return estimatedLaunchTime;
    }

    public long getTimeSinceLastUsedByUser(String packageName, int userId) {
        long timeSinceLastUsedByUser;
        long elapsedRealtime = this.mInjector.elapsedRealtime();
        synchronized (this.mAppIdleLock) {
            timeSinceLastUsedByUser = this.mAppIdleHistory.getTimeSinceLastUsedByUser(packageName, userId, elapsedRealtime);
        }
        return timeSinceLastUsedByUser;
    }

    public void onUserRemoved(int userId) {
        synchronized (this.mAppIdleLock) {
            this.mAppIdleHistory.onUserRemoved(userId);
            synchronized (this.mActiveAdminApps) {
                this.mActiveAdminApps.remove(userId);
            }
            synchronized (this.mAdminProtectedPackages) {
                this.mAdminProtectedPackages.remove(userId);
            }
        }
    }

    private boolean isAppIdleUnfiltered(String packageName, int userId, long elapsedRealtime) {
        boolean isIdle;
        synchronized (this.mAppIdleLock) {
            isIdle = this.mAppIdleHistory.isIdle(packageName, userId, elapsedRealtime);
        }
        return isIdle;
    }

    public void addListener(AppStandbyInternal.AppIdleStateChangeListener listener) {
        synchronized (this.mPackageAccessListeners) {
            if (!this.mPackageAccessListeners.contains(listener)) {
                this.mPackageAccessListeners.add(listener);
            }
        }
    }

    public void removeListener(AppStandbyInternal.AppIdleStateChangeListener listener) {
        synchronized (this.mPackageAccessListeners) {
            this.mPackageAccessListeners.remove(listener);
        }
    }

    public int getAppId(String packageName) {
        try {
            ApplicationInfo ai = this.mPackageManager.getApplicationInfo(packageName, 4194816);
            return ai.uid;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    public boolean isAppIdleFiltered(String packageName, int userId, long elapsedRealtime, boolean shouldObfuscateInstantApps) {
        if (shouldObfuscateInstantApps && this.mInjector.isPackageEphemeral(userId, packageName)) {
            return false;
        }
        return isAppIdleFiltered(packageName, getAppId(packageName), userId, elapsedRealtime);
    }

    private int getAppMinBucket(String packageName, int userId) {
        try {
            int uid = this.mPackageManager.getPackageUidAsUser(packageName, userId);
            return getAppMinBucket(packageName, UserHandle.getAppId(uid), userId);
        } catch (PackageManager.NameNotFoundException e) {
            return 50;
        }
    }

    private int getAppMinBucket(String packageName, int appId, int userId) {
        if (packageName == null) {
            return 50;
        }
        if (this.mAppIdleEnabled && appId >= 10000 && !packageName.equals(PackageManagerService.PLATFORM_PACKAGE_NAME)) {
            if (this.mSystemServicesReady) {
                if (this.mInjector.isNonIdleWhitelisted(packageName) || isActiveDeviceAdmin(packageName, userId) || isAdminProtectedPackages(packageName, userId) || isActiveNetworkScorer(packageName)) {
                    return 5;
                }
                AppWidgetManager appWidgetManager = this.mAppWidgetManager;
                if (appWidgetManager != null && this.mInjector.isBoundWidgetPackage(appWidgetManager, packageName, userId)) {
                    return 10;
                }
                if (isDeviceProvisioningPackage(packageName)) {
                    return 5;
                }
                if (this.mInjector.isWellbeingPackage(packageName) || this.mInjector.hasExactAlarmPermission(packageName, UserHandle.getUid(userId, appId))) {
                    return 20;
                }
            }
            if (isCarrierApp(packageName)) {
                return 5;
            }
            if (isHeadlessSystemApp(packageName)) {
                return 10;
            }
            if (this.mPackageManager.checkPermission("android.permission.ACCESS_BACKGROUND_LOCATION", packageName) != 0) {
                return 50;
            }
            return 30;
        }
        return 5;
    }

    private boolean isHeadlessSystemApp(String packageName) {
        boolean contains;
        synchronized (this.mHeadlessSystemApps) {
            contains = this.mHeadlessSystemApps.contains(packageName);
        }
        return contains;
    }

    public boolean isAppIdleFiltered(String packageName, int appId, int userId, long elapsedRealtime) {
        return this.mAppIdleEnabled && !this.mIsCharging && isAppIdleUnfiltered(packageName, userId, elapsedRealtime) && getAppMinBucket(packageName, appId, userId) >= 40;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isUserUsage(int reason) {
        if ((65280 & reason) == 768) {
            int subReason = reason & 255;
            return subReason == 3 || subReason == 4;
        }
        return false;
    }

    /* JADX WARN: Removed duplicated region for block: B:27:0x007c  */
    /* JADX WARN: Removed duplicated region for block: B:28:0x0082  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public int[] getIdleUidsForUser(int userId) {
        int index;
        int i;
        ApplicationInfo ai;
        boolean newIdle;
        if (!this.mAppIdleEnabled) {
            return EmptyArray.INT;
        }
        Trace.traceBegin(64L, "getIdleUidsForUser");
        long elapsedRealtime = this.mInjector.elapsedRealtime();
        PackageManagerInternal pmi = this.mInjector.getPackageManagerInternal();
        List<ApplicationInfo> apps = pmi.getInstalledApplications(0L, userId, Process.myUid());
        if (apps == null) {
            return EmptyArray.INT;
        }
        SparseBooleanArray uidIdleStates = new SparseBooleanArray();
        boolean z = true;
        int notIdleCount = 0;
        int i2 = apps.size() - 1;
        while (i2 >= 0) {
            ApplicationInfo ai2 = apps.get(i2);
            int index2 = uidIdleStates.indexOfKey(ai2.uid);
            boolean currentIdle = index2 < 0 ? z : uidIdleStates.valueAt(index2);
            if (currentIdle) {
                index = index2;
                i = i2;
                ai = ai2;
                if (isAppIdleFiltered(ai2.packageName, UserHandle.getAppId(ai2.uid), userId, elapsedRealtime)) {
                    newIdle = true;
                    if (currentIdle && !newIdle) {
                        notIdleCount++;
                    }
                    if (index >= 0) {
                        uidIdleStates.put(ai.uid, newIdle);
                    } else {
                        uidIdleStates.setValueAt(index, newIdle);
                    }
                    i2 = i - 1;
                    z = true;
                }
            } else {
                index = index2;
                i = i2;
                ai = ai2;
            }
            newIdle = false;
            if (currentIdle) {
                notIdleCount++;
            }
            if (index >= 0) {
            }
            i2 = i - 1;
            z = true;
        }
        int numIdleUids = uidIdleStates.size() - notIdleCount;
        int[] idleUids = new int[numIdleUids];
        for (int i3 = uidIdleStates.size() - 1; i3 >= 0; i3--) {
            if (uidIdleStates.valueAt(i3)) {
                numIdleUids--;
                idleUids[numIdleUids] = uidIdleStates.keyAt(i3);
            }
        }
        Trace.traceEnd(64L);
        return idleUids;
    }

    public void setAppIdleAsync(String packageName, boolean idle, int userId) {
        if (packageName == null || !this.mAppIdleEnabled) {
            return;
        }
        this.mHandler.obtainMessage(4, userId, idle ? 1 : 0, packageName).sendToTarget();
    }

    public int getAppStandbyBucket(String packageName, int userId, long elapsedRealtime, boolean shouldObfuscateInstantApps) {
        int appStandbyBucket;
        if (this.mAppIdleEnabled) {
            if (shouldObfuscateInstantApps && this.mInjector.isPackageEphemeral(userId, packageName)) {
                return 10;
            }
            synchronized (this.mAppIdleLock) {
                appStandbyBucket = this.mAppIdleHistory.getAppStandbyBucket(packageName, userId, elapsedRealtime);
            }
            return appStandbyBucket;
        }
        return 10;
    }

    public int getAppStandbyBucketReason(String packageName, int userId, long elapsedRealtime) {
        int appStandbyReason;
        synchronized (this.mAppIdleLock) {
            appStandbyReason = this.mAppIdleHistory.getAppStandbyReason(packageName, userId, elapsedRealtime);
        }
        return appStandbyReason;
    }

    public List<AppStandbyInfo> getAppStandbyBuckets(int userId) {
        ArrayList<AppStandbyInfo> appStandbyBuckets;
        synchronized (this.mAppIdleLock) {
            appStandbyBuckets = this.mAppIdleHistory.getAppStandbyBuckets(userId, this.mAppIdleEnabled);
        }
        return appStandbyBuckets;
    }

    public int getAppMinStandbyBucket(String packageName, int appId, int userId, boolean shouldObfuscateInstantApps) {
        int appMinBucket;
        if (shouldObfuscateInstantApps && this.mInjector.isPackageEphemeral(userId, packageName)) {
            return 50;
        }
        synchronized (this.mAppIdleLock) {
            appMinBucket = getAppMinBucket(packageName, appId, userId);
        }
        return appMinBucket;
    }

    public void restrictApp(String packageName, int userId, int restrictReason) {
        restrictApp(packageName, userId, 1536, restrictReason);
    }

    public void restrictApp(String packageName, int userId, int mainReason, int restrictReason) {
        if (mainReason != 1536 && mainReason != 1024) {
            Slog.e(TAG, "Tried to restrict app " + packageName + " for an unsupported reason");
        } else if (!this.mInjector.isPackageInstalled(packageName, 0, userId)) {
            Slog.e(TAG, "Tried to restrict uninstalled app: " + packageName);
        } else {
            int reason = (65280 & mainReason) | (restrictReason & 255);
            long nowElapsed = this.mInjector.elapsedRealtime();
            int bucket = this.mAllowRestrictedBucket ? 45 : 40;
            setAppStandbyBucket(packageName, userId, bucket, reason, nowElapsed, false);
        }
    }

    public void setAppStandbyBucket(String packageName, int bucket, int userId, int callingUid, int callingPid) {
        setAppStandbyBuckets(Collections.singletonList(new AppStandbyInfo(packageName, bucket)), userId, callingUid, callingPid);
    }

    /* JADX WARN: Code restructure failed: missing block: B:12:0x002d, code lost:
        if (r25 != android.os.Process.myPid()) goto L10;
     */
    /* JADX WARN: Removed duplicated region for block: B:23:0x0055  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void setAppStandbyBuckets(List<AppStandbyInfo> appBuckets, int userId, int callingUid, int callingPid) {
        int reason;
        int numApps;
        int i;
        AppStandbyController appStandbyController = this;
        int userId2 = ActivityManager.handleIncomingUser(callingPid, callingUid, userId, false, true, "setAppStandbyBucket", null);
        boolean shellCaller = callingUid == 0 || callingUid == 2000;
        if (!UserHandle.isSameApp(callingUid, 1000)) {
        }
        if (!shellCaller) {
            if (UserHandle.isCore(callingUid)) {
                reason = 1536;
            } else {
                reason = 1280;
            }
            numApps = appBuckets.size();
            long elapsedRealtime = appStandbyController.mInjector.elapsedRealtime();
            i = 0;
            while (i < numApps) {
                AppStandbyInfo bucketInfo = appBuckets.get(i);
                String packageName = bucketInfo.mPackageName;
                int bucket = bucketInfo.mStandbyBucket;
                if (bucket < 10 || bucket > 50) {
                    throw new IllegalArgumentException("Cannot set the standby bucket to " + bucket);
                }
                int packageUid = appStandbyController.mInjector.getPackageManagerInternal().getPackageUid(packageName, 4980736L, userId2);
                if (packageUid == callingUid) {
                    throw new IllegalArgumentException("Cannot set your own standby bucket");
                }
                if (packageUid < 0) {
                    throw new IllegalArgumentException("Cannot set standby bucket for non existent package (" + packageName + ")");
                }
                setAppStandbyBucket(packageName, userId2, bucket, reason, elapsedRealtime, shellCaller);
                i++;
                appStandbyController = this;
            }
        }
        reason = 1024;
        numApps = appBuckets.size();
        long elapsedRealtime2 = appStandbyController.mInjector.elapsedRealtime();
        i = 0;
        while (i < numApps) {
        }
    }

    void setAppStandbyBucket(String packageName, int userId, int newBucket, int reason) {
        setAppStandbyBucket(packageName, userId, newBucket, reason, this.mInjector.elapsedRealtime(), false);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1829=6] */
    /* JADX WARN: Removed duplicated region for block: B:24:0x005f  */
    /* JADX WARN: Removed duplicated region for block: B:25:0x0061  */
    /* JADX WARN: Removed duplicated region for block: B:28:0x006a A[Catch: all -> 0x021e, DONT_GENERATE, TryCatch #8 {all -> 0x021e, blocks: (B:22:0x004f, B:26:0x0062, B:28:0x006a, B:30:0x006c, B:36:0x0078, B:42:0x0088, B:45:0x008f, B:47:0x0091, B:51:0x0098, B:56:0x00a4, B:58:0x00b6, B:70:0x00e1, B:72:0x00e7, B:76:0x00f2, B:78:0x00f4, B:81:0x00fc, B:85:0x0101, B:87:0x0114, B:89:0x0118, B:91:0x011d, B:100:0x01a0, B:115:0x01cf, B:117:0x01d3, B:92:0x0140, B:93:0x015b, B:95:0x016d, B:96:0x019b, B:34:0x0076), top: B:159:0x004f }] */
    /* JADX WARN: Removed duplicated region for block: B:30:0x006c A[Catch: all -> 0x021e, TryCatch #8 {all -> 0x021e, blocks: (B:22:0x004f, B:26:0x0062, B:28:0x006a, B:30:0x006c, B:36:0x0078, B:42:0x0088, B:45:0x008f, B:47:0x0091, B:51:0x0098, B:56:0x00a4, B:58:0x00b6, B:70:0x00e1, B:72:0x00e7, B:76:0x00f2, B:78:0x00f4, B:81:0x00fc, B:85:0x0101, B:87:0x0114, B:89:0x0118, B:91:0x011d, B:100:0x01a0, B:115:0x01cf, B:117:0x01d3, B:92:0x0140, B:93:0x015b, B:95:0x016d, B:96:0x019b, B:34:0x0076), top: B:159:0x004f }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void setAppStandbyBucket(String packageName, int userId, int newBucket, int reason, long elapsedRealtime, boolean resetTimeout) {
        Object obj;
        int newBucket2;
        AppIdleHistory.AppUsageHistory app;
        AppIdleHistory.AppUsageHistory app2;
        int reason2;
        int newBucket3;
        int i;
        AppIdleHistory.AppUsageHistory app3;
        if (this.mAppIdleEnabled) {
            Object obj2 = this.mAppIdleLock;
            synchronized (obj2) {
                try {
                    if (this.mInjector.isPackageInstalled(packageName, 0, userId)) {
                        try {
                            if (newBucket == 45) {
                                try {
                                    if (!this.mAllowRestrictedBucket) {
                                        newBucket2 = 40;
                                        app = this.mAppIdleHistory.getAppUsageHistory(packageName, userId, elapsedRealtime);
                                        boolean predicted = (reason & JobPackageTracker.EVENT_STOP_REASON_MASK) != 1280;
                                        if (app.currentBucket >= 10) {
                                            return;
                                        }
                                        if ((app.currentBucket == 50 || newBucket2 == 50) && predicted) {
                                            return;
                                        }
                                        boolean wasForcedBySystem = (app.bucketingReason & JobPackageTracker.EVENT_STOP_REASON_MASK) == 1536;
                                        if (predicted && ((app.bucketingReason & JobPackageTracker.EVENT_STOP_REASON_MASK) == 1024 || wasForcedBySystem)) {
                                            return;
                                        }
                                        boolean isForcedBySystem = (reason & JobPackageTracker.EVENT_STOP_REASON_MASK) == 1536;
                                        if (app.currentBucket == newBucket2 && wasForcedBySystem && isForcedBySystem) {
                                            if (newBucket2 == 45) {
                                                i = 1536;
                                                app3 = app;
                                                this.mAppIdleHistory.noteRestrictionAttempt(packageName, userId, elapsedRealtime, reason);
                                            } else {
                                                i = 1536;
                                                app3 = app;
                                            }
                                            try {
                                                this.mAppIdleHistory.setAppStandbyBucket(packageName, userId, elapsedRealtime, newBucket2, i | (app3.bucketingReason & 255) | (reason & 255), resetTimeout);
                                                return;
                                            } catch (Throwable th) {
                                                th = th;
                                                obj = obj2;
                                            }
                                        } else {
                                            boolean isForcedByUser = (reason & JobPackageTracker.EVENT_STOP_REASON_MASK) == 1024;
                                            if (app.currentBucket == 45) {
                                                if ((65280 & app.bucketingReason) == 512) {
                                                    if (predicted && newBucket2 >= 40) {
                                                        return;
                                                    }
                                                } else if (!isUserUsage(reason) && !isForcedByUser) {
                                                    return;
                                                }
                                            }
                                            if (newBucket2 == 45) {
                                                this.mAppIdleHistory.noteRestrictionAttempt(packageName, userId, elapsedRealtime, reason);
                                                if (!isForcedByUser) {
                                                    app2 = app;
                                                    long timeUntilRestrictPossibleMs = (app2.lastUsedByUserElapsedTime + this.mInjector.getAutoRestrictedBucketDelayMs()) - elapsedRealtime;
                                                    if (timeUntilRestrictPossibleMs > 0) {
                                                        Slog.w(TAG, "Tried to restrict recently used app: " + packageName + " due to " + reason);
                                                        AppStandbyHandler appStandbyHandler = this.mHandler;
                                                        appStandbyHandler.sendMessageDelayed(appStandbyHandler.obtainMessage(11, userId, -1, packageName), timeUntilRestrictPossibleMs);
                                                        return;
                                                    }
                                                } else if (!Build.IS_DEBUGGABLE || (reason & 255) == 2) {
                                                    Slog.i(TAG, packageName + " restricted by user");
                                                    app2 = app;
                                                } else {
                                                    Toast.makeText(this.mContext, this.mHandler.getLooper(), this.mContext.getResources().getString(17039706, packageName), 0).show();
                                                    app2 = app;
                                                }
                                            } else {
                                                app2 = app;
                                            }
                                            try {
                                                try {
                                                    try {
                                                        if (predicted) {
                                                            long elapsedTimeAdjusted = this.mAppIdleHistory.getElapsedTime(elapsedRealtime);
                                                            this.mAppIdleHistory.updateLastPrediction(app2, elapsedTimeAdjusted, newBucket2);
                                                            int bucketWithValidExpiryTime = getMinBucketWithValidExpiryTime(app2, newBucket2, elapsedTimeAdjusted);
                                                            if (bucketWithValidExpiryTime != -1) {
                                                                newBucket3 = bucketWithValidExpiryTime;
                                                                if (newBucket3 != 10) {
                                                                    try {
                                                                        if (app2.currentBucket != newBucket3) {
                                                                            reason2 = 775;
                                                                        }
                                                                    } catch (Throwable th2) {
                                                                        th = th2;
                                                                        obj = obj2;
                                                                    }
                                                                }
                                                                reason2 = app2.bucketingReason;
                                                            } else if (newBucket2 == 40 && this.mAllowRestrictedBucket && getBucketForLocked(packageName, userId, elapsedRealtime) == 45) {
                                                                newBucket3 = 45;
                                                                reason2 = 512;
                                                            }
                                                            int newBucket4 = Math.min(newBucket3, getAppMinBucket(packageName, userId));
                                                            obj = obj2;
                                                            this.mAppIdleHistory.setAppStandbyBucket(packageName, userId, elapsedRealtime, newBucket4, reason2, resetTimeout);
                                                            maybeInformListeners(packageName, userId, elapsedRealtime, newBucket4, reason2, false);
                                                            return;
                                                        }
                                                        this.mAppIdleHistory.setAppStandbyBucket(packageName, userId, elapsedRealtime, newBucket4, reason2, resetTimeout);
                                                        maybeInformListeners(packageName, userId, elapsedRealtime, newBucket4, reason2, false);
                                                        return;
                                                    } catch (Throwable th3) {
                                                        th = th3;
                                                    }
                                                    obj = obj2;
                                                } catch (Throwable th4) {
                                                    th = th4;
                                                    obj = obj2;
                                                }
                                                int newBucket42 = Math.min(newBucket3, getAppMinBucket(packageName, userId));
                                            } catch (Throwable th5) {
                                                th = th5;
                                                obj = obj2;
                                            }
                                            reason2 = reason;
                                            newBucket3 = newBucket2;
                                        }
                                    }
                                } catch (Throwable th6) {
                                    th = th6;
                                    obj = obj2;
                                }
                            }
                            app = this.mAppIdleHistory.getAppUsageHistory(packageName, userId, elapsedRealtime);
                            boolean predicted2 = (reason & JobPackageTracker.EVENT_STOP_REASON_MASK) != 1280;
                            if (app.currentBucket >= 10) {
                            }
                        } catch (Throwable th7) {
                            th = th7;
                            obj = obj2;
                        }
                        newBucket2 = newBucket;
                    } else {
                        try {
                            Slog.e(TAG, "Tried to set bucket of uninstalled app: " + packageName);
                            return;
                        } catch (Throwable th8) {
                            th = th8;
                            obj = obj2;
                        }
                    }
                } catch (Throwable th9) {
                    th = th9;
                    obj = obj2;
                }
                while (true) {
                    try {
                        break;
                    } catch (Throwable th10) {
                        th = th10;
                    }
                }
                throw th;
            }
        }
    }

    public boolean isActiveDeviceAdmin(String packageName, int userId) {
        boolean z;
        synchronized (this.mActiveAdminApps) {
            Set<String> adminPkgs = this.mActiveAdminApps.get(userId);
            z = adminPkgs != null && adminPkgs.contains(packageName);
        }
        return z;
    }

    private boolean isAdminProtectedPackages(String packageName, int userId) {
        synchronized (this.mAdminProtectedPackages) {
            boolean z = true;
            if (this.mAdminProtectedPackages.contains(-1) && this.mAdminProtectedPackages.get(-1).contains(packageName)) {
                return true;
            }
            if (!this.mAdminProtectedPackages.contains(userId) || !this.mAdminProtectedPackages.get(userId).contains(packageName)) {
                z = false;
            }
            return z;
        }
    }

    public void addActiveDeviceAdmin(String adminPkg, int userId) {
        synchronized (this.mActiveAdminApps) {
            Set<String> adminPkgs = this.mActiveAdminApps.get(userId);
            if (adminPkgs == null) {
                adminPkgs = new ArraySet();
                this.mActiveAdminApps.put(userId, adminPkgs);
            }
            adminPkgs.add(adminPkg);
        }
    }

    public void setActiveAdminApps(Set<String> adminPkgs, int userId) {
        synchronized (this.mActiveAdminApps) {
            if (adminPkgs == null) {
                this.mActiveAdminApps.remove(userId);
            } else {
                this.mActiveAdminApps.put(userId, adminPkgs);
            }
        }
    }

    public void setAdminProtectedPackages(Set<String> packageNames, int userId) {
        synchronized (this.mAdminProtectedPackages) {
            if (packageNames != null) {
                if (!packageNames.isEmpty()) {
                    this.mAdminProtectedPackages.put(userId, packageNames);
                }
            }
            this.mAdminProtectedPackages.remove(userId);
        }
    }

    public void onAdminDataAvailable() {
        this.mAdminDataAvailableLatch.countDown();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void waitForAdminData() {
        if (this.mContext.getPackageManager().hasSystemFeature("android.software.device_admin")) {
            ConcurrentUtils.waitForCountDownNoInterrupt(this.mAdminDataAvailableLatch, 10000L, "Wait for admin data");
        }
    }

    Set<String> getActiveAdminAppsForTest(int userId) {
        Set<String> set;
        synchronized (this.mActiveAdminApps) {
            set = this.mActiveAdminApps.get(userId);
        }
        return set;
    }

    Set<String> getAdminProtectedPackagesForTest(int userId) {
        Set<String> set;
        synchronized (this.mAdminProtectedPackages) {
            set = this.mAdminProtectedPackages.get(userId);
        }
        return set;
    }

    private boolean isDeviceProvisioningPackage(String packageName) {
        if (this.mCachedDeviceProvisioningPackage == null) {
            this.mCachedDeviceProvisioningPackage = this.mContext.getResources().getString(17039954);
        }
        return this.mCachedDeviceProvisioningPackage.equals(packageName);
    }

    private boolean isCarrierApp(String packageName) {
        synchronized (this.mCarrierPrivilegedLock) {
            if (!this.mHaveCarrierPrivilegedApps) {
                fetchCarrierPrivilegedAppsCPL();
            }
            List<String> list = this.mCarrierPrivilegedApps;
            if (list != null) {
                return list.contains(packageName);
            }
            return false;
        }
    }

    public void clearCarrierPrivilegedApps() {
        synchronized (this.mCarrierPrivilegedLock) {
            this.mHaveCarrierPrivilegedApps = false;
            this.mCarrierPrivilegedApps = null;
        }
    }

    private void fetchCarrierPrivilegedAppsCPL() {
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService(TelephonyManager.class);
        this.mCarrierPrivilegedApps = telephonyManager.getCarrierPrivilegedPackagesForAllActiveSubscriptions();
        this.mHaveCarrierPrivilegedApps = true;
    }

    private boolean isActiveNetworkScorer(String packageName) {
        long now = SystemClock.elapsedRealtime();
        if (this.mCachedNetworkScorer == null || this.mCachedNetworkScorerAtMillis < now - NETWORK_SCORER_CACHE_DURATION_MILLIS) {
            this.mCachedNetworkScorer = this.mInjector.getActiveNetworkScorer();
            this.mCachedNetworkScorerAtMillis = now;
        }
        return packageName.equals(this.mCachedNetworkScorer);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void informListeners(String packageName, int userId, int bucket, int reason, boolean userInteraction) {
        boolean idle = bucket >= 40;
        synchronized (this.mPackageAccessListeners) {
            Iterator<AppStandbyInternal.AppIdleStateChangeListener> it = this.mPackageAccessListeners.iterator();
            while (it.hasNext()) {
                AppStandbyInternal.AppIdleStateChangeListener listener = it.next();
                listener.onAppIdleStateChanged(packageName, userId, idle, bucket, reason);
                if (userInteraction) {
                    listener.onUserInteractionStarted(packageName, userId);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void informParoleStateChanged() {
        boolean paroled = isInParole();
        synchronized (this.mPackageAccessListeners) {
            Iterator<AppStandbyInternal.AppIdleStateChangeListener> it = this.mPackageAccessListeners.iterator();
            while (it.hasNext()) {
                AppStandbyInternal.AppIdleStateChangeListener listener = it.next();
                listener.onParoleStateChanged(paroled);
            }
        }
    }

    public long getBroadcastResponseWindowDurationMs() {
        return this.mBroadcastResponseWindowDurationMillis;
    }

    public int getBroadcastResponseFgThresholdState() {
        return this.mBroadcastResponseFgThresholdState;
    }

    public long getBroadcastSessionsDurationMs() {
        return this.mBroadcastSessionsDurationMs;
    }

    public long getBroadcastSessionsWithResponseDurationMs() {
        return this.mBroadcastSessionsWithResponseDurationMs;
    }

    public boolean shouldNoteResponseEventForAllBroadcastSessions() {
        return this.mNoteResponseEventForAllBroadcastSessions;
    }

    public List<String> getBroadcastResponseExemptedRoles() {
        return this.mBroadcastResponseExemptedRolesList;
    }

    public List<String> getBroadcastResponseExemptedPermissions() {
        return this.mBroadcastResponseExemptedPermissionsList;
    }

    public String getAppStandbyConstant(String key) {
        return this.mAppStandbyProperties.get(key);
    }

    public void clearLastUsedTimestampsForTest(String packageName, int userId) {
        synchronized (this.mAppIdleLock) {
            this.mAppIdleHistory.clearLastUsedTimestamps(packageName, userId);
        }
    }

    public void flushToDisk() {
        synchronized (this.mAppIdleLock) {
            this.mAppIdleHistory.writeAppIdleTimes(this.mInjector.elapsedRealtime());
            this.mAppIdleHistory.writeAppIdleDurations();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isDisplayOn() {
        return this.mInjector.isDefaultDisplayOn();
    }

    void clearAppIdleForPackage(String packageName, int userId) {
        synchronized (this.mAppIdleLock) {
            this.mAppIdleHistory.clearUsage(packageName, userId);
        }
    }

    void maybeUnrestrictBuggyApp(String packageName, int userId) {
        maybeUnrestrictApp(packageName, userId, 1536, 4, 256, 1);
    }

    public void maybeUnrestrictApp(String packageName, int userId, int prevMainReasonRestrict, int prevSubReasonRestrict, int mainReasonUnrestrict, int subReasonUnrestrict) {
        int newBucket;
        int newReason;
        synchronized (this.mAppIdleLock) {
            try {
                try {
                    long elapsedRealtime = this.mInjector.elapsedRealtime();
                    AppIdleHistory.AppUsageHistory app = this.mAppIdleHistory.getAppUsageHistory(packageName, userId, elapsedRealtime);
                    if (app.currentBucket == 45 && (app.bucketingReason & JobPackageTracker.EVENT_STOP_REASON_MASK) == prevMainReasonRestrict) {
                        if ((app.bucketingReason & 255) == prevSubReasonRestrict) {
                            newBucket = 40;
                            newReason = mainReasonUnrestrict | subReasonUnrestrict;
                        } else {
                            newBucket = 45;
                            newReason = app.bucketingReason & (~prevSubReasonRestrict);
                        }
                        this.mAppIdleHistory.setAppStandbyBucket(packageName, userId, elapsedRealtime, newBucket, newReason);
                        maybeInformListeners(packageName, userId, elapsedRealtime, newBucket, newReason, false);
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePowerWhitelistCache() {
        if (this.mInjector.getBootPhase() < 500) {
            return;
        }
        this.mInjector.updatePowerWhitelistCache();
        postCheckIdleStates(-1);
    }

    /* loaded from: classes2.dex */
    private class PackageReceiver extends BroadcastReceiver {
        private PackageReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String pkgName = intent.getData().getSchemeSpecificPart();
            int userId = getSendingUserId();
            if ("android.intent.action.PACKAGE_ADDED".equals(action) || "android.intent.action.PACKAGE_CHANGED".equals(action)) {
                String[] cmpList = intent.getStringArrayExtra("android.intent.extra.changed_component_name_list");
                if (cmpList == null || (cmpList.length == 1 && pkgName.equals(cmpList[0]))) {
                    AppStandbyController.this.clearCarrierPrivilegedApps();
                    AppStandbyController.this.evaluateSystemAppException(pkgName, userId);
                }
                AppStandbyController.this.mHandler.obtainMessage(11, userId, -1, pkgName).sendToTarget();
            }
            if ("android.intent.action.PACKAGE_REMOVED".equals(action) || "android.intent.action.PACKAGE_ADDED".equals(action)) {
                if (intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                    AppStandbyController.this.maybeUnrestrictBuggyApp(pkgName, userId);
                } else {
                    AppStandbyController.this.clearAppIdleForPackage(pkgName, userId);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void evaluateSystemAppException(String packageName, int userId) {
        if (!this.mSystemServicesReady) {
            return;
        }
        try {
            PackageInfo pi = this.mPackageManager.getPackageInfoAsUser(packageName, HEADLESS_APP_CHECK_FLAGS, userId);
            maybeUpdateHeadlessSystemAppCache(pi);
        } catch (PackageManager.NameNotFoundException e) {
            synchronized (this.mHeadlessSystemApps) {
                this.mHeadlessSystemApps.remove(packageName);
            }
        }
    }

    private boolean maybeUpdateHeadlessSystemAppCache(PackageInfo pkgInfo) {
        if (pkgInfo == null || pkgInfo.applicationInfo == null || (!pkgInfo.applicationInfo.isSystemApp() && !pkgInfo.applicationInfo.isUpdatedSystemApp())) {
            return false;
        }
        Intent frontDoorActivityIntent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.LAUNCHER").setPackage(pkgInfo.packageName);
        List<ResolveInfo> res = this.mPackageManager.queryIntentActivitiesAsUser(frontDoorActivityIntent, HEADLESS_APP_CHECK_FLAGS, 0);
        return updateHeadlessSystemAppCache(pkgInfo.packageName, ArrayUtils.isEmpty(res));
    }

    private boolean updateHeadlessSystemAppCache(String packageName, boolean add) {
        synchronized (this.mHeadlessSystemApps) {
            if (add) {
                return this.mHeadlessSystemApps.add(packageName);
            }
            return this.mHeadlessSystemApps.remove(packageName);
        }
    }

    public void initializeDefaultsForSystemApps(int userId) {
        Object obj;
        if (!this.mSystemServicesReady) {
            this.mPendingInitializeDefaults = true;
            return;
        }
        Slog.d(TAG, "Initializing defaults for system apps on user " + userId + ", appIdleEnabled=" + this.mAppIdleEnabled);
        long elapsedRealtime = this.mInjector.elapsedRealtime();
        List<PackageInfo> packages = this.mPackageManager.getInstalledPackagesAsUser(512, userId);
        int packageCount = packages.size();
        Object obj2 = this.mAppIdleLock;
        synchronized (obj2) {
            int i = 0;
            while (i < packageCount) {
                try {
                    PackageInfo pi = packages.get(i);
                    String packageName = pi.packageName;
                    if (pi.applicationInfo != null && pi.applicationInfo.isSystemApp()) {
                        obj = obj2;
                        try {
                            this.mAppIdleHistory.reportUsage(packageName, userId, 10, 6, 0L, elapsedRealtime + this.mSystemUpdateUsageTimeoutMillis);
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    } else {
                        obj = obj2;
                    }
                    i++;
                    obj2 = obj;
                } catch (Throwable th2) {
                    th = th2;
                    obj = obj2;
                }
            }
            Object obj3 = obj2;
            this.mAppIdleHistory.writeAppIdleTimes(userId, elapsedRealtime);
        }
    }

    private Set<String> getSystemPackagesWithLauncherActivities() {
        Intent intent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> activities = this.mPackageManager.queryIntentActivitiesAsUser(intent, HEADLESS_APP_CHECK_FLAGS, 0);
        ArraySet<String> ret = new ArraySet<>();
        for (ResolveInfo ri : activities) {
            ret.add(ri.activityInfo.packageName);
        }
        return ret;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadHeadlessSystemAppCache() {
        long start = SystemClock.uptimeMillis();
        List<PackageInfo> packages = this.mPackageManager.getInstalledPackagesAsUser(HEADLESS_APP_CHECK_FLAGS, 0);
        Set<String> systemLauncherActivities = getSystemPackagesWithLauncherActivities();
        int packageCount = packages.size();
        for (int i = 0; i < packageCount; i++) {
            PackageInfo pkgInfo = packages.get(i);
            if (pkgInfo != null) {
                String pkg = pkgInfo.packageName;
                boolean isHeadLess = !systemLauncherActivities.contains(pkg);
                if (updateHeadlessSystemAppCache(pkg, isHeadLess)) {
                    this.mHandler.obtainMessage(11, 0, -1, pkg).sendToTarget();
                }
            }
        }
        long end = SystemClock.uptimeMillis();
        Slog.d(TAG, "Loaded headless system app cache in " + (end - start) + " ms: appIdleEnabled=" + this.mAppIdleEnabled);
    }

    public void postReportContentProviderUsage(String name, String packageName, int userId) {
        ContentProviderUsageRecord record = ContentProviderUsageRecord.obtain(name, packageName, userId);
        this.mHandler.obtainMessage(8, record).sendToTarget();
    }

    public void postReportSyncScheduled(String packageName, int userId, boolean exempted) {
        this.mHandler.obtainMessage(12, userId, exempted ? 1 : 0, packageName).sendToTarget();
    }

    public void postReportExemptedSyncStart(String packageName, int userId) {
        this.mHandler.obtainMessage(13, userId, 0, packageName).sendToTarget();
    }

    AppIdleHistory getAppIdleHistoryForTest() {
        AppIdleHistory appIdleHistory;
        synchronized (this.mAppIdleLock) {
            appIdleHistory = this.mAppIdleHistory;
        }
        return appIdleHistory;
    }

    public void dumpUsers(IndentingPrintWriter idpw, int[] userIds, List<String> pkgs) {
        synchronized (this.mAppIdleLock) {
            this.mAppIdleHistory.dumpUsers(idpw, userIds, pkgs);
        }
    }

    public void dumpState(String[] args, PrintWriter pw) {
        synchronized (this.mCarrierPrivilegedLock) {
            pw.println("Carrier privileged apps (have=" + this.mHaveCarrierPrivilegedApps + "): " + this.mCarrierPrivilegedApps);
        }
        pw.println();
        pw.println("Settings:");
        pw.print("  mCheckIdleIntervalMillis=");
        TimeUtils.formatDuration(this.mCheckIdleIntervalMillis, pw);
        pw.println();
        pw.print("  mStrongUsageTimeoutMillis=");
        TimeUtils.formatDuration(this.mStrongUsageTimeoutMillis, pw);
        pw.println();
        pw.print("  mNotificationSeenTimeoutMillis=");
        TimeUtils.formatDuration(this.mNotificationSeenTimeoutMillis, pw);
        pw.println();
        pw.print("  mNotificationSeenPromotedBucket=");
        pw.print(UsageStatsManager.standbyBucketToString(this.mNotificationSeenPromotedBucket));
        pw.println();
        pw.print("  mTriggerQuotaBumpOnNotificationSeen=");
        pw.print(this.mTriggerQuotaBumpOnNotificationSeen);
        pw.println();
        pw.print("  mRetainNotificationSeenImpactForPreTApps=");
        pw.print(this.mRetainNotificationSeenImpactForPreTApps);
        pw.println();
        pw.print("  mSlicePinnedTimeoutMillis=");
        TimeUtils.formatDuration(this.mSlicePinnedTimeoutMillis, pw);
        pw.println();
        pw.print("  mSyncAdapterTimeoutMillis=");
        TimeUtils.formatDuration(this.mSyncAdapterTimeoutMillis, pw);
        pw.println();
        pw.print("  mSystemInteractionTimeoutMillis=");
        TimeUtils.formatDuration(this.mSystemInteractionTimeoutMillis, pw);
        pw.println();
        pw.print("  mInitialForegroundServiceStartTimeoutMillis=");
        TimeUtils.formatDuration(this.mInitialForegroundServiceStartTimeoutMillis, pw);
        pw.println();
        pw.print("  mPredictionTimeoutMillis=");
        TimeUtils.formatDuration(this.mPredictionTimeoutMillis, pw);
        pw.println();
        pw.print("  mExemptedSyncScheduledNonDozeTimeoutMillis=");
        TimeUtils.formatDuration(this.mExemptedSyncScheduledNonDozeTimeoutMillis, pw);
        pw.println();
        pw.print("  mExemptedSyncScheduledDozeTimeoutMillis=");
        TimeUtils.formatDuration(this.mExemptedSyncScheduledDozeTimeoutMillis, pw);
        pw.println();
        pw.print("  mExemptedSyncStartTimeoutMillis=");
        TimeUtils.formatDuration(this.mExemptedSyncStartTimeoutMillis, pw);
        pw.println();
        pw.print("  mUnexemptedSyncScheduledTimeoutMillis=");
        TimeUtils.formatDuration(this.mUnexemptedSyncScheduledTimeoutMillis, pw);
        pw.println();
        pw.print("  mSystemUpdateUsageTimeoutMillis=");
        TimeUtils.formatDuration(this.mSystemUpdateUsageTimeoutMillis, pw);
        pw.println();
        pw.print("  mBroadcastResponseWindowDurationMillis=");
        TimeUtils.formatDuration(this.mBroadcastResponseWindowDurationMillis, pw);
        pw.println();
        pw.print("  mBroadcastResponseFgThresholdState=");
        pw.print(ActivityManager.procStateToString(this.mBroadcastResponseFgThresholdState));
        pw.println();
        pw.print("  mBroadcastSessionsDurationMs=");
        TimeUtils.formatDuration(this.mBroadcastSessionsDurationMs, pw);
        pw.println();
        pw.print("  mBroadcastSessionsWithResponseDurationMs=");
        TimeUtils.formatDuration(this.mBroadcastSessionsWithResponseDurationMs, pw);
        pw.println();
        pw.print("  mNoteResponseEventForAllBroadcastSessions=");
        pw.print(this.mNoteResponseEventForAllBroadcastSessions);
        pw.println();
        pw.print("  mBroadcastResponseExemptedRoles");
        pw.print(this.mBroadcastResponseExemptedRoles);
        pw.println();
        pw.print("  mBroadcastResponseExemptedPermissions");
        pw.print(this.mBroadcastResponseExemptedPermissions);
        pw.println();
        pw.println();
        pw.print("mAppIdleEnabled=");
        pw.print(this.mAppIdleEnabled);
        pw.print(" mAllowRestrictedBucket=");
        pw.print(this.mAllowRestrictedBucket);
        pw.print(" mIsCharging=");
        pw.print(this.mIsCharging);
        pw.println();
        pw.print("mScreenThresholds=");
        pw.println(Arrays.toString(this.mAppStandbyScreenThresholds));
        pw.print("mElapsedThresholds=");
        pw.println(Arrays.toString(this.mAppStandbyElapsedThresholds));
        pw.println();
        pw.println("mHeadlessSystemApps=[");
        synchronized (this.mHeadlessSystemApps) {
            for (int i = this.mHeadlessSystemApps.size() - 1; i >= 0; i--) {
                pw.print("  ");
                pw.print(this.mHeadlessSystemApps.valueAt(i));
                if (i != 0) {
                    pw.println(",");
                }
            }
        }
        pw.println("]");
        pw.println();
        pw.println("mSystemPackagesAppIds=[");
        synchronized (this.mSystemPackagesAppIds) {
            for (int i2 = this.mSystemPackagesAppIds.size() - 1; i2 >= 0; i2--) {
                pw.print("  ");
                pw.print(this.mSystemPackagesAppIds.get(i2));
                if (i2 != 0) {
                    pw.println(",");
                }
            }
        }
        pw.println("]");
        pw.println();
        this.mInjector.dump(pw);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Injector {
        private AlarmManagerInternal mAlarmManagerInternal;
        private BatteryManager mBatteryManager;
        private IBatteryStats mBatteryStats;
        int mBootPhase;
        private final Context mContext;
        private CrossProfileAppsInternal mCrossProfileAppsInternal;
        private IDeviceIdleController mDeviceIdleController;
        private DisplayManager mDisplayManager;
        private final Looper mLooper;
        private PackageManagerInternal mPackageManagerInternal;
        private PowerManager mPowerManager;
        long mAutoRestrictedBucketDelayMs = 86400000;
        private final ArraySet<String> mPowerWhitelistedApps = new ArraySet<>();
        private String mWellbeingApp = null;

        Injector(Context context, Looper looper) {
            this.mContext = context;
            this.mLooper = looper;
        }

        Context getContext() {
            return this.mContext;
        }

        Looper getLooper() {
            return this.mLooper;
        }

        void onBootPhase(int phase) {
            if (phase == 500) {
                this.mDeviceIdleController = IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));
                this.mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
                this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
                this.mPowerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
                this.mBatteryManager = (BatteryManager) this.mContext.getSystemService(BatteryManager.class);
                this.mCrossProfileAppsInternal = (CrossProfileAppsInternal) LocalServices.getService(CrossProfileAppsInternal.class);
                this.mAlarmManagerInternal = (AlarmManagerInternal) LocalServices.getService(AlarmManagerInternal.class);
                ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService(HostingRecord.HOSTING_TYPE_ACTIVITY);
                if (activityManager.isLowRamDevice() || ActivityManager.isSmallBatteryDevice()) {
                    this.mAutoRestrictedBucketDelayMs = 43200000L;
                }
            } else if (phase == 1000) {
                PackageManager packageManager = this.mContext.getPackageManager();
                this.mWellbeingApp = packageManager.getWellbeingPackageName();
            }
            this.mBootPhase = phase;
        }

        int getBootPhase() {
            return this.mBootPhase;
        }

        long elapsedRealtime() {
            return SystemClock.elapsedRealtime();
        }

        long currentTimeMillis() {
            return System.currentTimeMillis();
        }

        boolean isAppIdleEnabled() {
            boolean buildFlag = this.mContext.getResources().getBoolean(17891626);
            boolean runtimeFlag = Settings.Global.getInt(this.mContext.getContentResolver(), "app_standby_enabled", 1) == 1 && Settings.Global.getInt(this.mContext.getContentResolver(), "adaptive_battery_management_enabled", 1) == 1;
            return buildFlag && runtimeFlag;
        }

        boolean isCharging() {
            return this.mBatteryManager.isCharging();
        }

        boolean isNonIdleWhitelisted(String packageName) {
            boolean contains;
            if (this.mBootPhase < 500) {
                return false;
            }
            synchronized (this.mPowerWhitelistedApps) {
                contains = this.mPowerWhitelistedApps.contains(packageName);
            }
            return contains;
        }

        boolean isWellbeingPackage(String packageName) {
            return packageName.equals(this.mWellbeingApp);
        }

        boolean hasExactAlarmPermission(String packageName, int uid) {
            return this.mAlarmManagerInternal.hasExactAlarmPermission(packageName, uid);
        }

        void updatePowerWhitelistCache() {
            try {
                String[] whitelistedPkgs = this.mDeviceIdleController.getFullPowerWhitelistExceptIdle();
                synchronized (this.mPowerWhitelistedApps) {
                    this.mPowerWhitelistedApps.clear();
                    for (String str : whitelistedPkgs) {
                        this.mPowerWhitelistedApps.add(str);
                    }
                }
            } catch (RemoteException e) {
                Slog.wtf(AppStandbyController.TAG, "Failed to get power whitelist", e);
            }
        }

        boolean isRestrictedBucketEnabled() {
            return Settings.Global.getInt(this.mContext.getContentResolver(), "enable_restricted_bucket", 1) == 1;
        }

        File getDataSystemDirectory() {
            return Environment.getDataSystemDirectory();
        }

        long getAutoRestrictedBucketDelayMs() {
            return this.mAutoRestrictedBucketDelayMs;
        }

        void noteEvent(int event, String packageName, int uid) throws RemoteException {
            this.mBatteryStats.noteEvent(event, packageName, uid);
        }

        PackageManagerInternal getPackageManagerInternal() {
            return this.mPackageManagerInternal;
        }

        boolean isPackageEphemeral(int userId, String packageName) {
            return this.mPackageManagerInternal.isPackageEphemeral(userId, packageName);
        }

        boolean isPackageInstalled(String packageName, int flags, int userId) {
            return this.mPackageManagerInternal.getPackageUid(packageName, (long) flags, userId) >= 0;
        }

        int[] getRunningUserIds() throws RemoteException {
            return ActivityManager.getService().getRunningUserIds();
        }

        boolean isDefaultDisplayOn() {
            return this.mDisplayManager.getDisplay(0).getState() == 2;
        }

        void registerDisplayListener(DisplayManager.DisplayListener listener, Handler handler) {
            this.mDisplayManager.registerDisplayListener(listener, handler);
        }

        String getActiveNetworkScorer() {
            NetworkScoreManager nsm = (NetworkScoreManager) this.mContext.getSystemService("network_score");
            return nsm.getActiveScorerPackage();
        }

        public boolean isBoundWidgetPackage(AppWidgetManager appWidgetManager, String packageName, int userId) {
            return appWidgetManager.isBoundWidgetPackage(packageName, userId);
        }

        DeviceConfig.Properties getDeviceConfigProperties(String... keys) {
            return DeviceConfig.getProperties("app_standby", keys);
        }

        public boolean isDeviceIdleMode() {
            return this.mPowerManager.isDeviceIdleMode();
        }

        public List<UserHandle> getValidCrossProfileTargets(String pkg, int userId) {
            int uid = this.mPackageManagerInternal.getPackageUid(pkg, 0L, userId);
            AndroidPackage aPkg = this.mPackageManagerInternal.getPackage(uid);
            if (uid < 0 || aPkg == null || !aPkg.isCrossProfile() || !this.mCrossProfileAppsInternal.verifyUidHasInteractAcrossProfilePermission(pkg, uid)) {
                if (uid >= 0 && aPkg == null) {
                    Slog.wtf(AppStandbyController.TAG, "Null package retrieved for UID " + uid);
                }
                return Collections.emptyList();
            }
            return this.mCrossProfileAppsInternal.getTargetUserProfiles(pkg, userId);
        }

        void registerDeviceConfigPropertiesChangedListener(DeviceConfig.OnPropertiesChangedListener listener) {
            DeviceConfig.addOnPropertiesChangedListener("app_standby", JobSchedulerBackgroundThread.getExecutor(), listener);
        }

        void dump(PrintWriter pw) {
            pw.println("mPowerWhitelistedApps=[");
            synchronized (this.mPowerWhitelistedApps) {
                for (int i = this.mPowerWhitelistedApps.size() - 1; i >= 0; i--) {
                    pw.print("  ");
                    pw.print(this.mPowerWhitelistedApps.valueAt(i));
                    pw.println(",");
                }
            }
            pw.println("]");
            pw.println();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class AppStandbyHandler extends Handler {
        AppStandbyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 3:
                    StandbyUpdateRecord r = (StandbyUpdateRecord) msg.obj;
                    AppStandbyController.this.informListeners(r.packageName, r.userId, r.bucket, r.reason, r.isUserInteraction);
                    r.recycle();
                    return;
                case 4:
                    AppStandbyController.this.forceIdleState((String) msg.obj, msg.arg1, msg.arg2 == 1);
                    return;
                case 5:
                    removeMessages(5);
                    long earliestCheck = JobStatus.NO_LATEST_RUNTIME;
                    long nowElapsed = AppStandbyController.this.mInjector.elapsedRealtime();
                    synchronized (AppStandbyController.this.mPendingIdleStateChecks) {
                        for (int i = AppStandbyController.this.mPendingIdleStateChecks.size() - 1; i >= 0; i--) {
                            long expirationTime = AppStandbyController.this.mPendingIdleStateChecks.valueAt(i);
                            if (expirationTime <= nowElapsed) {
                                int userId = AppStandbyController.this.mPendingIdleStateChecks.keyAt(i);
                                if (AppStandbyController.this.checkIdleStates(userId) && AppStandbyController.this.mAppIdleEnabled) {
                                    expirationTime = nowElapsed + AppStandbyController.this.mCheckIdleIntervalMillis;
                                    AppStandbyController.this.mPendingIdleStateChecks.put(userId, expirationTime);
                                } else {
                                    AppStandbyController.this.mPendingIdleStateChecks.removeAt(i);
                                }
                            }
                            earliestCheck = Math.min(earliestCheck, expirationTime);
                        }
                    }
                    if (earliestCheck != JobStatus.NO_LATEST_RUNTIME) {
                        AppStandbyController.this.mHandler.sendMessageDelayed(AppStandbyController.this.mHandler.obtainMessage(5), earliestCheck - nowElapsed);
                        return;
                    }
                    return;
                case 6:
                default:
                    super.handleMessage(msg);
                    return;
                case 7:
                    AppStandbyController.this.triggerListenerQuotaBump((String) msg.obj, msg.arg1);
                    return;
                case 8:
                    ContentProviderUsageRecord record = (ContentProviderUsageRecord) msg.obj;
                    AppStandbyController.this.reportContentProviderUsage(record.name, record.packageName, record.userId);
                    record.recycle();
                    return;
                case 9:
                    AppStandbyController.this.informParoleStateChanged();
                    return;
                case 10:
                    AppStandbyController.this.mHandler.removeMessages(10);
                    AppStandbyController.this.waitForAdminData();
                    AppStandbyController.this.checkIdleStates(-1);
                    return;
                case 11:
                    AppStandbyController.this.checkAndUpdateStandbyState((String) msg.obj, msg.arg1, msg.arg2, AppStandbyController.this.mInjector.elapsedRealtime());
                    return;
                case 12:
                    boolean exempted = msg.arg2 > 0;
                    if (exempted) {
                        AppStandbyController.this.reportExemptedSyncScheduled((String) msg.obj, msg.arg1);
                        return;
                    } else {
                        AppStandbyController.this.reportUnexemptedSyncScheduled((String) msg.obj, msg.arg1);
                        return;
                    }
                case 13:
                    AppStandbyController.this.reportExemptedSyncStart((String) msg.obj, msg.arg1);
                    return;
            }
        }
    }

    /* loaded from: classes2.dex */
    private class DeviceStateReceiver extends BroadcastReceiver {
        private DeviceStateReceiver() {
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            char c;
            String action = intent.getAction();
            switch (action.hashCode()) {
                case -65633567:
                    if (action.equals("android.os.action.POWER_SAVE_WHITELIST_CHANGED")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case -54942926:
                    if (action.equals("android.os.action.DISCHARGING")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 948344062:
                    if (action.equals("android.os.action.CHARGING")) {
                        c = 0;
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
                    AppStandbyController.this.setChargingState(true);
                    return;
                case 1:
                    AppStandbyController.this.setChargingState(false);
                    return;
                case 2:
                    if (AppStandbyController.this.mSystemServicesReady) {
                        AppStandbyHandler appStandbyHandler = AppStandbyController.this.mHandler;
                        final AppStandbyController appStandbyController = AppStandbyController.this;
                        appStandbyHandler.post(new Runnable() { // from class: com.android.server.usage.AppStandbyController$DeviceStateReceiver$$ExternalSyntheticLambda0
                            @Override // java.lang.Runnable
                            public final void run() {
                                AppStandbyController.this.updatePowerWhitelistCache();
                            }
                        });
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes2.dex */
    private class ConstantsObserver extends ContentObserver implements DeviceConfig.OnPropertiesChangedListener {
        public static final long DEFAULT_AUTO_RESTRICTED_BUCKET_DELAY_MS = 86400000;
        private static final String DEFAULT_BROADCAST_RESPONSE_EXEMPTED_PERMISSIONS = "";
        private static final String DEFAULT_BROADCAST_RESPONSE_EXEMPTED_ROLES = "";
        public static final int DEFAULT_BROADCAST_RESPONSE_FG_THRESHOLD_STATE = 2;
        public static final long DEFAULT_BROADCAST_RESPONSE_WINDOW_DURATION_MS = 120000;
        public static final long DEFAULT_BROADCAST_SESSIONS_DURATION_MS = 120000;
        public static final long DEFAULT_BROADCAST_SESSIONS_WITH_RESPONSE_DURATION_MS = 120000;
        public static final long DEFAULT_CHECK_IDLE_INTERVAL_MS = 14400000;
        public static final boolean DEFAULT_CROSS_PROFILE_APPS_SHARE_STANDBY_BUCKETS = true;
        public static final long DEFAULT_EXEMPTED_SYNC_SCHEDULED_DOZE_TIMEOUT = 14400000;
        public static final long DEFAULT_EXEMPTED_SYNC_SCHEDULED_NON_DOZE_TIMEOUT = 600000;
        public static final long DEFAULT_EXEMPTED_SYNC_START_TIMEOUT = 600000;
        public static final long DEFAULT_INITIAL_FOREGROUND_SERVICE_START_TIMEOUT = 1800000;
        public static final boolean DEFAULT_NOTE_RESPONSE_EVENT_FOR_ALL_BROADCAST_SESSIONS = true;
        public static final int DEFAULT_NOTIFICATION_SEEN_PROMOTED_BUCKET = 20;
        public static final long DEFAULT_NOTIFICATION_TIMEOUT = 43200000;
        public static final boolean DEFAULT_RETAIN_NOTIFICATION_SEEN_IMPACT_FOR_PRE_T_APPS = false;
        public static final long DEFAULT_SLICE_PINNED_TIMEOUT = 43200000;
        public static final long DEFAULT_STRONG_USAGE_TIMEOUT = 3600000;
        public static final long DEFAULT_SYNC_ADAPTER_TIMEOUT = 600000;
        public static final long DEFAULT_SYSTEM_INTERACTION_TIMEOUT = 600000;
        public static final long DEFAULT_SYSTEM_UPDATE_TIMEOUT = 7200000;
        public static final boolean DEFAULT_TRIGGER_QUOTA_BUMP_ON_NOTIFICATION_SEEN = false;
        public static final long DEFAULT_UNEXEMPTED_SYNC_SCHEDULED_TIMEOUT = 600000;
        private static final String KEY_AUTO_RESTRICTED_BUCKET_DELAY_MS = "auto_restricted_bucket_delay_ms";
        private static final String KEY_BROADCAST_RESPONSE_EXEMPTED_PERMISSIONS = "brodacast_response_exempted_permissions";
        private static final String KEY_BROADCAST_RESPONSE_EXEMPTED_ROLES = "brodacast_response_exempted_roles";
        private static final String KEY_BROADCAST_RESPONSE_FG_THRESHOLD_STATE = "broadcast_response_fg_threshold_state";
        private static final String KEY_BROADCAST_RESPONSE_WINDOW_DURATION_MS = "broadcast_response_window_timeout_ms";
        private static final String KEY_BROADCAST_SESSIONS_DURATION_MS = "broadcast_sessions_duration_ms";
        private static final String KEY_BROADCAST_SESSIONS_WITH_RESPONSE_DURATION_MS = "broadcast_sessions_with_response_duration_ms";
        private static final String KEY_CROSS_PROFILE_APPS_SHARE_STANDBY_BUCKETS = "cross_profile_apps_share_standby_buckets";
        private static final String KEY_EXEMPTED_SYNC_SCHEDULED_DOZE_HOLD_DURATION = "exempted_sync_scheduled_d_duration";
        private static final String KEY_EXEMPTED_SYNC_SCHEDULED_NON_DOZE_HOLD_DURATION = "exempted_sync_scheduled_nd_duration";
        private static final String KEY_EXEMPTED_SYNC_START_HOLD_DURATION = "exempted_sync_start_duration";
        private static final String KEY_INITIAL_FOREGROUND_SERVICE_START_HOLD_DURATION = "initial_foreground_service_start_duration";
        private static final String KEY_NOTE_RESPONSE_EVENT_FOR_ALL_BROADCAST_SESSIONS = "note_response_event_for_all_broadcast_sessions";
        private static final String KEY_NOTIFICATION_SEEN_HOLD_DURATION = "notification_seen_duration";
        private static final String KEY_NOTIFICATION_SEEN_PROMOTED_BUCKET = "notification_seen_promoted_bucket";
        private static final String KEY_PREDICTION_TIMEOUT = "prediction_timeout";
        private static final String KEY_PREFIX_ELAPSED_TIME_THRESHOLD = "elapsed_threshold_";
        private static final String KEY_PREFIX_SCREEN_TIME_THRESHOLD = "screen_threshold_";
        private static final String KEY_RETAIN_NOTIFICATION_SEEN_IMPACT_FOR_PRE_T_APPS = "retain_notification_seen_impact_for_pre_t_apps";
        private static final String KEY_SLICE_PINNED_HOLD_DURATION = "slice_pinned_duration";
        private static final String KEY_STRONG_USAGE_HOLD_DURATION = "strong_usage_duration";
        private static final String KEY_SYNC_ADAPTER_HOLD_DURATION = "sync_adapter_duration";
        private static final String KEY_SYSTEM_INTERACTION_HOLD_DURATION = "system_interaction_duration";
        private static final String KEY_SYSTEM_UPDATE_HOLD_DURATION = "system_update_usage_duration";
        private static final String KEY_TRIGGER_QUOTA_BUMP_ON_NOTIFICATION_SEEN = "trigger_quota_bump_on_notification_seen";
        private static final String KEY_UNEXEMPTED_SYNC_SCHEDULED_HOLD_DURATION = "unexempted_sync_scheduled_duration";
        private final String[] KEYS_ELAPSED_TIME_THRESHOLDS;
        private final String[] KEYS_SCREEN_TIME_THRESHOLDS;
        private final TextUtils.SimpleStringSplitter mStringPipeSplitter;

        ConstantsObserver(Handler handler) {
            super(handler);
            this.KEYS_SCREEN_TIME_THRESHOLDS = new String[]{"screen_threshold_active", "screen_threshold_working_set", "screen_threshold_frequent", "screen_threshold_rare", "screen_threshold_restricted"};
            this.KEYS_ELAPSED_TIME_THRESHOLDS = new String[]{"elapsed_threshold_active", "elapsed_threshold_working_set", "elapsed_threshold_frequent", "elapsed_threshold_rare", "elapsed_threshold_restricted"};
            this.mStringPipeSplitter = new TextUtils.SimpleStringSplitter('|');
        }

        public void start() {
            ContentResolver cr = AppStandbyController.this.mContext.getContentResolver();
            cr.registerContentObserver(Settings.Global.getUriFor("app_standby_enabled"), false, this);
            cr.registerContentObserver(Settings.Global.getUriFor("enable_restricted_bucket"), false, this);
            cr.registerContentObserver(Settings.Global.getUriFor("adaptive_battery_management_enabled"), false, this);
            AppStandbyController.this.mInjector.registerDeviceConfigPropertiesChangedListener(this);
            processProperties(AppStandbyController.this.mInjector.getDeviceConfigProperties(new String[0]));
            updateSettings();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            updateSettings();
            AppStandbyController.this.postOneTimeCheckIdleStates();
        }

        public void onPropertiesChanged(DeviceConfig.Properties properties) {
            processProperties(properties);
            AppStandbyController.this.postOneTimeCheckIdleStates();
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        private void processProperties(DeviceConfig.Properties properties) {
            boolean timeThresholdsUpdated = false;
            synchronized (AppStandbyController.this.mAppIdleLock) {
                for (String name : properties.getKeyset()) {
                    if (name != null) {
                        char c = 65535;
                        switch (name.hashCode()) {
                            case -1991469656:
                                if (name.equals(KEY_SYNC_ADAPTER_HOLD_DURATION)) {
                                    c = '\f';
                                    break;
                                }
                                break;
                            case -1963219299:
                                if (name.equals(KEY_BROADCAST_RESPONSE_EXEMPTED_PERMISSIONS)) {
                                    c = 23;
                                    break;
                                }
                                break;
                            case -1794959158:
                                if (name.equals(KEY_TRIGGER_QUOTA_BUMP_ON_NOTIFICATION_SEEN)) {
                                    c = 6;
                                    break;
                                }
                                break;
                            case -1610671326:
                                if (name.equals(KEY_UNEXEMPTED_SYNC_SCHEDULED_HOLD_DURATION)) {
                                    c = 16;
                                    break;
                                }
                                break;
                            case -1525033432:
                                if (name.equals(KEY_BROADCAST_SESSIONS_WITH_RESPONSE_DURATION_MS)) {
                                    c = 20;
                                    break;
                                }
                                break;
                            case -1063555730:
                                if (name.equals(KEY_SLICE_PINNED_HOLD_DURATION)) {
                                    c = 7;
                                    break;
                                }
                                break;
                            case -973233853:
                                if (name.equals(KEY_AUTO_RESTRICTED_BUCKET_DELAY_MS)) {
                                    c = 0;
                                    break;
                                }
                                break;
                            case -695619964:
                                if (name.equals(KEY_NOTIFICATION_SEEN_HOLD_DURATION)) {
                                    c = 3;
                                    break;
                                }
                                break;
                            case -654339791:
                                if (name.equals(KEY_SYSTEM_INTERACTION_HOLD_DURATION)) {
                                    c = '\n';
                                    break;
                                }
                                break;
                            case -641750299:
                                if (name.equals(KEY_NOTE_RESPONSE_EVENT_FOR_ALL_BROADCAST_SESSIONS)) {
                                    c = 21;
                                    break;
                                }
                                break;
                            case -557676904:
                                if (name.equals(KEY_SYSTEM_UPDATE_HOLD_DURATION)) {
                                    c = 11;
                                    break;
                                }
                                break;
                            case -294320234:
                                if (name.equals(KEY_BROADCAST_RESPONSE_EXEMPTED_ROLES)) {
                                    c = 22;
                                    break;
                                }
                                break;
                            case -129077581:
                                if (name.equals(KEY_BROADCAST_RESPONSE_WINDOW_DURATION_MS)) {
                                    c = 17;
                                    break;
                                }
                                break;
                            case -57661244:
                                if (name.equals(KEY_EXEMPTED_SYNC_SCHEDULED_DOZE_HOLD_DURATION)) {
                                    c = '\r';
                                    break;
                                }
                                break;
                            case 276460958:
                                if (name.equals(KEY_RETAIN_NOTIFICATION_SEEN_IMPACT_FOR_PRE_T_APPS)) {
                                    c = 5;
                                    break;
                                }
                                break;
                            case 456604392:
                                if (name.equals(KEY_EXEMPTED_SYNC_SCHEDULED_NON_DOZE_HOLD_DURATION)) {
                                    c = 14;
                                    break;
                                }
                                break;
                            case 742365823:
                                if (name.equals(KEY_BROADCAST_RESPONSE_FG_THRESHOLD_STATE)) {
                                    c = 18;
                                    break;
                                }
                                break;
                            case 938381045:
                                if (name.equals(KEY_NOTIFICATION_SEEN_PROMOTED_BUCKET)) {
                                    c = 4;
                                    break;
                                }
                                break;
                            case 992238669:
                                if (name.equals(KEY_BROADCAST_SESSIONS_DURATION_MS)) {
                                    c = 19;
                                    break;
                                }
                                break;
                            case 1105744372:
                                if (name.equals(KEY_EXEMPTED_SYNC_START_HOLD_DURATION)) {
                                    c = 15;
                                    break;
                                }
                                break;
                            case 1288386175:
                                if (name.equals(KEY_CROSS_PROFILE_APPS_SHARE_STANDBY_BUCKETS)) {
                                    c = 1;
                                    break;
                                }
                                break;
                            case 1378352561:
                                if (name.equals(KEY_PREDICTION_TIMEOUT)) {
                                    c = '\t';
                                    break;
                                }
                                break;
                            case 1400233242:
                                if (name.equals(KEY_STRONG_USAGE_HOLD_DURATION)) {
                                    c = '\b';
                                    break;
                                }
                                break;
                            case 1915246556:
                                if (name.equals(KEY_INITIAL_FOREGROUND_SERVICE_START_HOLD_DURATION)) {
                                    c = 2;
                                    break;
                                }
                                break;
                        }
                        switch (c) {
                            case 0:
                                AppStandbyController.this.mInjector.mAutoRestrictedBucketDelayMs = Math.max(14400000L, properties.getLong(KEY_AUTO_RESTRICTED_BUCKET_DELAY_MS, 86400000L));
                                break;
                            case 1:
                                AppStandbyController.this.mLinkCrossProfileApps = properties.getBoolean(KEY_CROSS_PROFILE_APPS_SHARE_STANDBY_BUCKETS, true);
                                break;
                            case 2:
                                AppStandbyController.this.mInitialForegroundServiceStartTimeoutMillis = properties.getLong(KEY_INITIAL_FOREGROUND_SERVICE_START_HOLD_DURATION, 1800000L);
                                break;
                            case 3:
                                AppStandbyController.this.mNotificationSeenTimeoutMillis = properties.getLong(KEY_NOTIFICATION_SEEN_HOLD_DURATION, 43200000L);
                                break;
                            case 4:
                                AppStandbyController.this.mNotificationSeenPromotedBucket = properties.getInt(KEY_NOTIFICATION_SEEN_PROMOTED_BUCKET, 20);
                                break;
                            case 5:
                                AppStandbyController.this.mRetainNotificationSeenImpactForPreTApps = properties.getBoolean(KEY_RETAIN_NOTIFICATION_SEEN_IMPACT_FOR_PRE_T_APPS, false);
                                break;
                            case 6:
                                AppStandbyController.this.mTriggerQuotaBumpOnNotificationSeen = properties.getBoolean(KEY_TRIGGER_QUOTA_BUMP_ON_NOTIFICATION_SEEN, false);
                                break;
                            case 7:
                                AppStandbyController.this.mSlicePinnedTimeoutMillis = properties.getLong(KEY_SLICE_PINNED_HOLD_DURATION, 43200000L);
                                break;
                            case '\b':
                                AppStandbyController.this.mStrongUsageTimeoutMillis = properties.getLong(KEY_STRONG_USAGE_HOLD_DURATION, 3600000L);
                                break;
                            case '\t':
                                AppStandbyController.this.mPredictionTimeoutMillis = properties.getLong(KEY_PREDICTION_TIMEOUT, 43200000L);
                                break;
                            case '\n':
                                AppStandbyController.this.mSystemInteractionTimeoutMillis = properties.getLong(KEY_SYSTEM_INTERACTION_HOLD_DURATION, 600000L);
                                break;
                            case 11:
                                AppStandbyController.this.mSystemUpdateUsageTimeoutMillis = properties.getLong(KEY_SYSTEM_UPDATE_HOLD_DURATION, (long) DEFAULT_SYSTEM_UPDATE_TIMEOUT);
                                break;
                            case '\f':
                                AppStandbyController.this.mSyncAdapterTimeoutMillis = properties.getLong(KEY_SYNC_ADAPTER_HOLD_DURATION, 600000L);
                                break;
                            case '\r':
                                AppStandbyController.this.mExemptedSyncScheduledDozeTimeoutMillis = properties.getLong(KEY_EXEMPTED_SYNC_SCHEDULED_DOZE_HOLD_DURATION, 14400000L);
                                break;
                            case 14:
                                AppStandbyController.this.mExemptedSyncScheduledNonDozeTimeoutMillis = properties.getLong(KEY_EXEMPTED_SYNC_SCHEDULED_NON_DOZE_HOLD_DURATION, 600000L);
                                break;
                            case 15:
                                AppStandbyController.this.mExemptedSyncStartTimeoutMillis = properties.getLong(KEY_EXEMPTED_SYNC_START_HOLD_DURATION, 600000L);
                                break;
                            case 16:
                                AppStandbyController.this.mUnexemptedSyncScheduledTimeoutMillis = properties.getLong(KEY_UNEXEMPTED_SYNC_SCHEDULED_HOLD_DURATION, 600000L);
                                break;
                            case 17:
                                AppStandbyController.this.mBroadcastResponseWindowDurationMillis = properties.getLong(KEY_BROADCAST_RESPONSE_WINDOW_DURATION_MS, 120000L);
                                break;
                            case 18:
                                AppStandbyController.this.mBroadcastResponseFgThresholdState = properties.getInt(KEY_BROADCAST_RESPONSE_FG_THRESHOLD_STATE, 2);
                                break;
                            case 19:
                                AppStandbyController.this.mBroadcastSessionsDurationMs = properties.getLong(KEY_BROADCAST_SESSIONS_DURATION_MS, 120000L);
                                break;
                            case 20:
                                AppStandbyController.this.mBroadcastSessionsWithResponseDurationMs = properties.getLong(KEY_BROADCAST_SESSIONS_WITH_RESPONSE_DURATION_MS, 120000L);
                                break;
                            case 21:
                                AppStandbyController.this.mNoteResponseEventForAllBroadcastSessions = properties.getBoolean(KEY_NOTE_RESPONSE_EVENT_FOR_ALL_BROADCAST_SESSIONS, true);
                                break;
                            case 22:
                                AppStandbyController.this.mBroadcastResponseExemptedRoles = properties.getString(KEY_BROADCAST_RESPONSE_EXEMPTED_ROLES, "");
                                AppStandbyController appStandbyController = AppStandbyController.this;
                                appStandbyController.mBroadcastResponseExemptedRolesList = splitPipeSeparatedString(appStandbyController.mBroadcastResponseExemptedRoles);
                                break;
                            case 23:
                                AppStandbyController.this.mBroadcastResponseExemptedPermissions = properties.getString(KEY_BROADCAST_RESPONSE_EXEMPTED_PERMISSIONS, "");
                                AppStandbyController appStandbyController2 = AppStandbyController.this;
                                appStandbyController2.mBroadcastResponseExemptedPermissionsList = splitPipeSeparatedString(appStandbyController2.mBroadcastResponseExemptedPermissions);
                                break;
                            default:
                                if (!timeThresholdsUpdated && (name.startsWith(KEY_PREFIX_SCREEN_TIME_THRESHOLD) || name.startsWith(KEY_PREFIX_ELAPSED_TIME_THRESHOLD))) {
                                    updateTimeThresholds();
                                    timeThresholdsUpdated = true;
                                    break;
                                }
                                break;
                        }
                        AppStandbyController.this.mAppStandbyProperties.put(name, properties.getString(name, (String) null));
                    }
                }
            }
        }

        private List<String> splitPipeSeparatedString(String string) {
            List<String> values = new ArrayList<>();
            this.mStringPipeSplitter.setString(string);
            while (this.mStringPipeSplitter.hasNext()) {
                values.add(this.mStringPipeSplitter.next());
            }
            return values;
        }

        private void updateTimeThresholds() {
            DeviceConfig.Properties screenThresholdProperties = AppStandbyController.this.mInjector.getDeviceConfigProperties(this.KEYS_SCREEN_TIME_THRESHOLDS);
            DeviceConfig.Properties elapsedThresholdProperties = AppStandbyController.this.mInjector.getDeviceConfigProperties(this.KEYS_ELAPSED_TIME_THRESHOLDS);
            AppStandbyController.this.mAppStandbyScreenThresholds = generateThresholdArray(screenThresholdProperties, this.KEYS_SCREEN_TIME_THRESHOLDS, AppStandbyController.DEFAULT_SCREEN_TIME_THRESHOLDS, AppStandbyController.MINIMUM_SCREEN_TIME_THRESHOLDS);
            AppStandbyController.this.mAppStandbyElapsedThresholds = generateThresholdArray(elapsedThresholdProperties, this.KEYS_ELAPSED_TIME_THRESHOLDS, AppStandbyController.DEFAULT_ELAPSED_TIME_THRESHOLDS, AppStandbyController.MINIMUM_ELAPSED_TIME_THRESHOLDS);
            AppStandbyController appStandbyController = AppStandbyController.this;
            appStandbyController.mCheckIdleIntervalMillis = Math.min(appStandbyController.mAppStandbyElapsedThresholds[1] / 4, 14400000L);
        }

        void updateSettings() {
            synchronized (AppStandbyController.this.mAppIdleLock) {
                AppStandbyController appStandbyController = AppStandbyController.this;
                appStandbyController.mAllowRestrictedBucket = appStandbyController.mInjector.isRestrictedBucketEnabled();
            }
            AppStandbyController appStandbyController2 = AppStandbyController.this;
            appStandbyController2.setAppIdleEnabled(appStandbyController2.mInjector.isAppIdleEnabled());
        }

        long[] generateThresholdArray(DeviceConfig.Properties properties, String[] keys, long[] defaults, long[] minValues) {
            if (properties.getKeyset().isEmpty()) {
                return defaults;
            }
            if (keys.length != AppStandbyController.THRESHOLD_BUCKETS.length) {
                throw new IllegalStateException("# keys (" + keys.length + ") != # buckets (" + AppStandbyController.THRESHOLD_BUCKETS.length + ")");
            }
            if (defaults.length != AppStandbyController.THRESHOLD_BUCKETS.length) {
                throw new IllegalStateException("# defaults (" + defaults.length + ") != # buckets (" + AppStandbyController.THRESHOLD_BUCKETS.length + ")");
            }
            if (minValues.length != AppStandbyController.THRESHOLD_BUCKETS.length) {
                Slog.wtf(AppStandbyController.TAG, "minValues array is the wrong size");
                minValues = new long[AppStandbyController.THRESHOLD_BUCKETS.length];
            }
            long[] array = new long[AppStandbyController.THRESHOLD_BUCKETS.length];
            for (int i = 0; i < AppStandbyController.THRESHOLD_BUCKETS.length; i++) {
                array[i] = Math.max(minValues[i], properties.getLong(keys[i], defaults[i]));
            }
            return array;
        }
    }
}
