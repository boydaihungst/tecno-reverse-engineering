package com.android.server.usage;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.IUidObserver;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManagerInternal;
import android.app.usage.AppLaunchEstimateInfo;
import android.app.usage.AppStandbyInfo;
import android.app.usage.BroadcastResponseStatsList;
import android.app.usage.ConfigurationStats;
import android.app.usage.EventStats;
import android.app.usage.IUsageStatsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.LocusId;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ShortcutServiceInternal;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.SparseSetArray;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.usage.AppStandbyInternal;
import com.android.server.usage.AppTimeLimitController;
import com.android.server.usage.UserUsageStatsService;
import com.android.server.utils.AlarmQueue;
import com.transsion.hubcore.server.usage.ITranUsageStatsService;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
/* loaded from: classes2.dex */
public class UsageStatsService extends SystemService implements UserUsageStatsService.StatsUpdatedListener {
    static final boolean COMPRESS_TIME = false;
    static final boolean DEBUG = false;
    private static final boolean ENABLE_KERNEL_UPDATES = true;
    private static final long FLUSH_INTERVAL = 1200000;
    private static final String GLOBAL_COMPONENT_USAGE_FILE_NAME = "globalcomponentusage";
    private static final boolean KEEP_LEGACY_DIR = false;
    static final int MSG_FLUSH_TO_DISK = 1;
    static final int MSG_HANDLE_LAUNCH_TIME_ON_USER_UNLOCK = 8;
    static final int MSG_NOTIFY_ESTIMATED_LAUNCH_TIMES_CHANGED = 9;
    static final int MSG_ON_START = 7;
    static final int MSG_PACKAGE_REMOVED = 6;
    static final int MSG_REMOVE_USER = 2;
    static final int MSG_REPORT_EVENT = 0;
    static final int MSG_REPORT_EVENT_TO_ALL_USERID = 4;
    static final int MSG_UID_STATE_CHANGED = 3;
    static final int MSG_UNLOCKED_USER = 5;
    private static final long ONE_DAY = 86400000;
    private static final long ONE_WEEK = 604800000;
    private static final long TEN_SECONDS = 10000;
    static final long TIME_CHANGE_THRESHOLD_MILLIS = 2000;
    private static final char TOKEN_DELIMITER = '/';
    private static final long TWENTY_MINUTES = 1200000;
    private static final long UNKNOWN_LAUNCH_TIME_DELAY_MS = 31536000000L;
    AppOpsManager mAppOps;
    AppStandbyInternal mAppStandby;
    AppTimeLimitController mAppTimeLimit;
    DevicePolicyManagerInternal mDpmInternal;
    private final CopyOnWriteArraySet<UsageStatsManagerInternal.EstimatedLaunchTimeChangedListener> mEstimatedLaunchTimeChangedListeners;
    Handler mHandler;
    private final Injector mInjector;
    private final Map<String, Long> mLastTimeComponentUsedGlobal;
    private final SparseArray<LaunchTimeAlarmQueue> mLaunchTimeAlarmQueues;
    private final Object mLock;
    PackageManager mPackageManager;
    PackageManagerInternal mPackageManagerInternal;
    private final PackageMonitor mPackageMonitor;
    private final SparseSetArray<String> mPendingLaunchTimeChangePackages;
    private long mRealTimeSnapshot;
    private final SparseArray<LinkedList<UsageEvents.Event>> mReportedEvents;
    private BroadcastResponseStatsTracker mResponseStatsTracker;
    ShortcutServiceInternal mShortcutServiceInternal;
    private AppStandbyInternal.AppIdleStateChangeListener mStandbyChangeListener;
    private long mSystemTimeSnapshot;
    private final IUidObserver mUidObserver;
    private final SparseIntArray mUidToKernelCounter;
    private final ArraySet<UsageStatsManagerInternal.UsageEventListener> mUsageEventListeners;
    final SparseArray<ArraySet<String>> mUsageReporters;
    int mUsageSource;
    UserManager mUserManager;
    private final SparseArray<UserUsageStatsService> mUserState;
    private final CopyOnWriteArraySet<Integer> mUserUnlockedStates;
    final SparseArray<ActivityData> mVisibleActivities;
    public static final boolean ENABLE_TIME_CHANGE_CORRECTION = SystemProperties.getBoolean("persist.debug.time_correction", true);
    static final String TAG = "UsageStatsService";
    static final boolean DEBUG_RESPONSE_STATS = Log.isLoggable(TAG, 3);
    private static final File KERNEL_COUNTER_FILE = new File("/proc/uid_procstat/set");
    private static final File USAGE_STATS_LEGACY_DIR = new File(Environment.getDataSystemDirectory(), "usagestats");
    private static final File COMMON_USAGE_STATS_DE_DIR = new File(Environment.getDataSystemDeDirectory(), "usagestats");

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class ActivityData {
        public int lastEvent;
        private final String mTaskRootClass;
        private final String mTaskRootPackage;
        private final String mUsageSourcePackage;

        private ActivityData(String taskRootPackage, String taskRootClass, String sourcePackage) {
            this.lastEvent = 0;
            this.mTaskRootPackage = taskRootPackage;
            this.mTaskRootClass = taskRootClass;
            this.mUsageSourcePackage = sourcePackage;
        }
    }

    /* loaded from: classes2.dex */
    static class Injector {
        Injector() {
        }

        AppStandbyInternal getAppStandbyController(Context context) {
            return AppStandbyInternal.newAppStandbyController(UsageStatsService.class.getClassLoader(), context);
        }
    }

    public UsageStatsService(Context context) {
        this(context, new Injector());
    }

    UsageStatsService(Context context, Injector injector) {
        super(context);
        this.mLock = new Object();
        this.mUserState = new SparseArray<>();
        this.mUserUnlockedStates = new CopyOnWriteArraySet<>();
        this.mUidToKernelCounter = new SparseIntArray();
        this.mLastTimeComponentUsedGlobal = new ArrayMap();
        this.mPackageMonitor = new MyPackageMonitor();
        this.mReportedEvents = new SparseArray<>();
        this.mUsageReporters = new SparseArray<>();
        this.mVisibleActivities = new SparseArray<>();
        this.mLaunchTimeAlarmQueues = new SparseArray<>();
        this.mUsageEventListeners = new ArraySet<>();
        this.mEstimatedLaunchTimeChangedListeners = new CopyOnWriteArraySet<>();
        this.mPendingLaunchTimeChangePackages = new SparseSetArray<>();
        this.mStandbyChangeListener = new AppStandbyInternal.AppIdleStateChangeListener() { // from class: com.android.server.usage.UsageStatsService.1
            public void onAppIdleStateChanged(String packageName, int userId, boolean idle, int bucket, int reason) {
                UsageEvents.Event event = new UsageEvents.Event(11, SystemClock.elapsedRealtime());
                event.mBucketAndReason = (bucket << 16) | (65535 & reason);
                event.mPackage = packageName;
                UsageStatsService.this.reportEventOrAddToQueue(userId, event);
            }
        };
        this.mUidObserver = new IUidObserver.Stub() { // from class: com.android.server.usage.UsageStatsService.3
            public void onUidStateChanged(int uid, int procState, long procStateSeq, int capability) {
                UsageStatsService.this.mHandler.obtainMessage(3, uid, procState).sendToTarget();
            }

            public void onUidIdle(int uid, boolean disabled) {
            }

            public void onUidGone(int uid, boolean disabled) {
                onUidStateChanged(uid, 20, 0L, 0);
            }

            public void onUidActive(int uid) {
            }

            public void onUidCachedChanged(int uid, boolean cached) {
            }

            public void onUidProcAdjChanged(int uid) {
            }
        };
        this.mInjector = injector;
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        this.mAppOps = (AppOpsManager) getContext().getSystemService("appops");
        this.mUserManager = (UserManager) getContext().getSystemService("user");
        this.mPackageManager = getContext().getPackageManager();
        this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mHandler = new H(BackgroundThread.get().getLooper());
        AppStandbyInternal appStandbyController = this.mInjector.getAppStandbyController(getContext());
        this.mAppStandby = appStandbyController;
        this.mResponseStatsTracker = new BroadcastResponseStatsTracker(appStandbyController);
        this.mAppTimeLimit = new AppTimeLimitController(getContext(), new AppTimeLimitController.TimeLimitCallbackListener() { // from class: com.android.server.usage.UsageStatsService.2
            @Override // com.android.server.usage.AppTimeLimitController.TimeLimitCallbackListener
            public void onLimitReached(int observerId, int userId, long timeLimit, long timeElapsed, PendingIntent callbackIntent) {
                if (callbackIntent == null) {
                    return;
                }
                Intent intent = new Intent();
                intent.putExtra("android.app.usage.extra.OBSERVER_ID", observerId);
                intent.putExtra("android.app.usage.extra.TIME_LIMIT", timeLimit);
                intent.putExtra("android.app.usage.extra.TIME_USED", timeElapsed);
                try {
                    callbackIntent.send(UsageStatsService.this.getContext(), 0, intent);
                } catch (PendingIntent.CanceledException e) {
                    Slog.w(UsageStatsService.TAG, "Couldn't deliver callback: " + callbackIntent);
                }
            }

            @Override // com.android.server.usage.AppTimeLimitController.TimeLimitCallbackListener
            public void onSessionEnd(int observerId, int userId, long timeElapsed, PendingIntent callbackIntent) {
                if (callbackIntent == null) {
                    return;
                }
                Intent intent = new Intent();
                intent.putExtra("android.app.usage.extra.OBSERVER_ID", observerId);
                intent.putExtra("android.app.usage.extra.TIME_USED", timeElapsed);
                try {
                    callbackIntent.send(UsageStatsService.this.getContext(), 0, intent);
                } catch (PendingIntent.CanceledException e) {
                    Slog.w(UsageStatsService.TAG, "Couldn't deliver callback: " + callbackIntent);
                }
            }
        }, this.mHandler.getLooper());
        this.mAppStandby.addListener(this.mStandbyChangeListener);
        this.mPackageMonitor.register(getContext(), (Looper) null, UserHandle.ALL, true);
        IntentFilter filter = new IntentFilter("android.intent.action.USER_REMOVED");
        filter.addAction("android.intent.action.USER_STARTED");
        getContext().registerReceiverAsUser(new UserActionsReceiver(), UserHandle.ALL, filter, null, this.mHandler);
        getContext().registerReceiverAsUser(new UidRemovedReceiver(), UserHandle.ALL, new IntentFilter("android.intent.action.UID_REMOVED"), null, this.mHandler);
        this.mRealTimeSnapshot = SystemClock.elapsedRealtime();
        this.mSystemTimeSnapshot = System.currentTimeMillis();
        publishLocalService(UsageStatsManagerInternal.class, new LocalService());
        publishLocalService(AppStandbyInternal.class, this.mAppStandby);
        publishBinderServices();
        this.mHandler.obtainMessage(7).sendToTarget();
    }

    void publishBinderServices() {
        publishBinderService("usagestats", new BinderService());
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        this.mAppStandby.onBootPhase(phase);
        if (phase == 500) {
            getDpmInternal();
            getShortcutServiceInternal();
            this.mResponseStatsTracker.onSystemServicesReady(getContext());
            File file = KERNEL_COUNTER_FILE;
            if (file.exists()) {
                try {
                    ActivityManager.getService().registerUidObserver(this.mUidObserver, 3, -1, (String) null);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            } else {
                Slog.w(TAG, "Missing procfs interface: " + file);
            }
            readUsageSourceSetting();
        }
    }

    @Override // com.android.server.SystemService
    public void onUserStarting(SystemService.TargetUser user) {
        this.mUserState.put(user.getUserIdentifier(), null);
    }

    @Override // com.android.server.SystemService
    public void onUserUnlocking(SystemService.TargetUser user) {
        this.mHandler.obtainMessage(5, user.getUserIdentifier(), 0).sendToTarget();
    }

    @Override // com.android.server.SystemService
    public void onUserStopping(SystemService.TargetUser user) {
        int userId = user.getUserIdentifier();
        synchronized (this.mLock) {
            if (!this.mUserUnlockedStates.contains(Integer.valueOf(userId))) {
                persistPendingEventsLocked(userId);
                return;
            }
            UsageEvents.Event event = new UsageEvents.Event(29, SystemClock.elapsedRealtime());
            event.mPackage = PackageManagerService.PLATFORM_PACKAGE_NAME;
            reportEvent(event, userId);
            UserUsageStatsService userService = this.mUserState.get(userId);
            if (userService != null) {
                userService.userStopped();
            }
            this.mUserUnlockedStates.remove(Integer.valueOf(userId));
            this.mUserState.put(userId, null);
            LaunchTimeAlarmQueue alarmQueue = this.mLaunchTimeAlarmQueues.get(userId);
            if (alarmQueue != null) {
                alarmQueue.removeAllAlarms();
                this.mLaunchTimeAlarmQueues.remove(userId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUserUnlocked(int userId) {
        HashMap<String, Long> installedPackages = getInstalledPackages(userId);
        if (userId == 0) {
            UsageStatsIdleService.scheduleUpdateMappingsJob(getContext());
        }
        boolean deleteObsoleteData = shouldDeleteObsoleteData(UserHandle.of(userId));
        synchronized (this.mLock) {
            this.mUserUnlockedStates.add(Integer.valueOf(userId));
            UsageEvents.Event unlockEvent = new UsageEvents.Event(28, SystemClock.elapsedRealtime());
            unlockEvent.mPackage = PackageManagerService.PLATFORM_PACKAGE_NAME;
            migrateStatsToSystemCeIfNeededLocked(userId);
            LinkedList<UsageEvents.Event> pendingEvents = new LinkedList<>();
            loadPendingEventsLocked(userId, pendingEvents);
            LinkedList<UsageEvents.Event> eventsInMem = this.mReportedEvents.get(userId);
            if (eventsInMem != null) {
                pendingEvents.addAll(eventsInMem);
            }
            boolean needToFlush = !pendingEvents.isEmpty();
            initializeUserUsageStatsServiceLocked(userId, System.currentTimeMillis(), installedPackages, deleteObsoleteData);
            UserUsageStatsService userService = getUserUsageStatsServiceLocked(userId);
            if (userService == null) {
                Slog.i(TAG, "Attempted to unlock stopped or removed user " + userId);
                return;
            }
            while (pendingEvents.peek() != null) {
                reportEvent(pendingEvents.poll(), userId);
            }
            reportEvent(unlockEvent, userId);
            this.mHandler.obtainMessage(8, userId, 0).sendToTarget();
            this.mReportedEvents.remove(userId);
            deleteRecursively(new File(Environment.getDataSystemDeDirectory(userId), "usagestats"));
            if (needToFlush) {
                userService.persistActiveStats();
            }
        }
    }

    private HashMap<String, Long> getInstalledPackages(int userId) {
        PackageManager packageManager = this.mPackageManager;
        if (packageManager == null) {
            return null;
        }
        List<PackageInfo> installedPackages = packageManager.getInstalledPackagesAsUser(8192, userId);
        HashMap<String, Long> packagesMap = new HashMap<>();
        for (int i = installedPackages.size() - 1; i >= 0; i--) {
            PackageInfo packageInfo = installedPackages.get(i);
            packagesMap.put(packageInfo.packageName, Long.valueOf(packageInfo.firstInstallTime));
        }
        return packagesMap;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public DevicePolicyManagerInternal getDpmInternal() {
        if (this.mDpmInternal == null) {
            this.mDpmInternal = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        }
        return this.mDpmInternal;
    }

    private ShortcutServiceInternal getShortcutServiceInternal() {
        if (this.mShortcutServiceInternal == null) {
            this.mShortcutServiceInternal = (ShortcutServiceInternal) LocalServices.getService(ShortcutServiceInternal.class);
        }
        return this.mShortcutServiceInternal;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readUsageSourceSetting() {
        synchronized (this.mLock) {
            this.mUsageSource = Settings.Global.getInt(getContext().getContentResolver(), "app_time_limit_usage_source", 1);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class LaunchTimeAlarmQueue extends AlarmQueue<String> {
        private final int mUserId;

        LaunchTimeAlarmQueue(int userId, Context context, Looper looper) {
            super(context, looper, "*usage.launchTime*", "Estimated launch times", true, 30000L);
            this.mUserId = userId;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.utils.AlarmQueue
        public boolean isForUser(String key, int userId) {
            return this.mUserId == userId;
        }

        @Override // com.android.server.utils.AlarmQueue
        protected void processExpiredAlarms(ArraySet<String> expired) {
            if (expired.size() > 0) {
                synchronized (UsageStatsService.this.mPendingLaunchTimeChangePackages) {
                    UsageStatsService.this.mPendingLaunchTimeChangePackages.addAll(this.mUserId, expired);
                }
                UsageStatsService.this.mHandler.sendEmptyMessage(9);
            }
        }
    }

    /* loaded from: classes2.dex */
    private class UserActionsReceiver extends BroadcastReceiver {
        private UserActionsReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
            String action = intent.getAction();
            if ("android.intent.action.USER_REMOVED".equals(action)) {
                if (userId >= 0) {
                    UsageStatsService.this.mHandler.obtainMessage(2, userId, 0).sendToTarget();
                    UsageStatsService.this.mResponseStatsTracker.onUserRemoved(userId);
                }
            } else if ("android.intent.action.USER_STARTED".equals(action) && userId >= 0) {
                UsageStatsService.this.mAppStandby.postCheckIdleStates(userId);
            }
        }
    }

    /* loaded from: classes2.dex */
    private class UidRemovedReceiver extends BroadcastReceiver {
        private UidRemovedReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            int uid = intent.getIntExtra("android.intent.extra.UID", -1);
            if (uid == -1) {
                return;
            }
            synchronized (UsageStatsService.this.mLock) {
                UsageStatsService.this.mResponseStatsTracker.onUidRemoved(uid);
            }
        }
    }

    @Override // com.android.server.usage.UserUsageStatsService.StatsUpdatedListener
    public void onStatsUpdated() {
        this.mHandler.sendEmptyMessageDelayed(1, 1200000L);
    }

    @Override // com.android.server.usage.UserUsageStatsService.StatsUpdatedListener
    public void onStatsReloaded() {
        this.mAppStandby.postOneTimeCheckIdleStates();
    }

    @Override // com.android.server.usage.UserUsageStatsService.StatsUpdatedListener
    public void onNewUpdate(int userId) {
        this.mAppStandby.initializeDefaultsForSystemApps(userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean shouldObfuscateInstantAppsForCaller(int callingUid, int userId) {
        return !this.mPackageManagerInternal.canAccessInstantApps(callingUid, userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean shouldHideShortcutInvocationEvents(int userId, String callingPackage, int callingPid, int callingUid) {
        ShortcutServiceInternal shortcutServiceInternal = getShortcutServiceInternal();
        if (shortcutServiceInternal != null) {
            return true ^ shortcutServiceInternal.hasShortcutHostPermission(userId, callingPackage, callingPid, callingUid);
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean shouldHideLocusIdEvents(int callingPid, int callingUid) {
        return (callingUid == 1000 || getContext().checkPermission("android.permission.ACCESS_LOCUS_ID_USAGE_STATS", callingPid, callingUid) == 0) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean shouldObfuscateNotificationEvents(int callingPid, int callingUid) {
        return (callingUid == 1000 || getContext().checkPermission("android.permission.MANAGE_NOTIFICATIONS", callingPid, callingUid) == 0) ? false : true;
    }

    private static void deleteRecursively(File f) {
        File[] files = f.listFiles();
        if (files != null) {
            for (File subFile : files) {
                deleteRecursively(subFile);
            }
        }
        if (f.exists() && !f.delete()) {
            Slog.e(TAG, "Failed to delete " + f);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public UserUsageStatsService getUserUsageStatsServiceLocked(int userId) {
        UserUsageStatsService service = this.mUserState.get(userId);
        if (service == null) {
            Slog.wtf(TAG, "Failed to fetch usage stats service for user " + userId + ". The user might not have been initialized yet.");
        }
        return service;
    }

    private void initializeUserUsageStatsServiceLocked(int userId, long currentTimeMillis, HashMap<String, Long> installedPackages, boolean deleteObsoleteData) {
        File usageStatsDir = new File(Environment.getDataSystemCeDirectory(userId), "usagestats");
        UserUsageStatsService service = new UserUsageStatsService(getContext(), userId, usageStatsDir, this);
        try {
            service.init(currentTimeMillis, installedPackages, deleteObsoleteData);
            this.mUserState.put(userId, service);
        } catch (Exception e) {
            if (this.mUserManager.isUserUnlocked(userId)) {
                Slog.w(TAG, "Failed to initialized unlocked user " + userId);
                throw e;
            } else {
                Slog.w(TAG, "Attempted to initialize service for stopped or removed user " + userId);
            }
        }
    }

    private void migrateStatsToSystemCeIfNeededLocked(int userId) {
        File usageStatsDir = new File(Environment.getDataSystemCeDirectory(userId), "usagestats");
        if (!usageStatsDir.mkdirs() && !usageStatsDir.exists()) {
            throw new IllegalStateException("Usage stats directory does not exist: " + usageStatsDir.getAbsolutePath());
        }
        File migrated = new File(usageStatsDir, "migrated");
        if (migrated.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(migrated));
                int previousVersion = Integer.parseInt(reader.readLine());
                if (previousVersion >= 4) {
                    deleteLegacyDir(userId);
                    reader.close();
                    return;
                }
                reader.close();
            } catch (IOException | NumberFormatException e) {
                Slog.e(TAG, "Failed to read migration status file, possibly corrupted.");
                deleteRecursively(usageStatsDir);
                if (usageStatsDir.exists()) {
                    Slog.e(TAG, "Unable to delete usage stats CE directory.");
                    throw new RuntimeException(e);
                } else if (!usageStatsDir.mkdirs() && !usageStatsDir.exists()) {
                    throw new IllegalStateException("Usage stats directory does not exist: " + usageStatsDir.getAbsolutePath());
                }
            }
        }
        Slog.i(TAG, "Starting migration to system CE for user " + userId);
        File legacyUserDir = new File(USAGE_STATS_LEGACY_DIR, Integer.toString(userId));
        if (legacyUserDir.exists()) {
            copyRecursively(usageStatsDir, legacyUserDir);
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(migrated));
            writer.write(Integer.toString(4));
            writer.write("\n");
            writer.flush();
            writer.close();
            Slog.i(TAG, "Finished migration to system CE for user " + userId);
            deleteLegacyDir(userId);
        } catch (IOException e2) {
            Slog.e(TAG, "Failed to write migrated status file");
            throw new RuntimeException(e2);
        }
    }

    private static void copyRecursively(File parent, File f) {
        File[] files = f.listFiles();
        if (files != null) {
            for (int i = files.length - 1; i >= 0; i--) {
                File newParent = parent;
                if (files[i].isDirectory()) {
                    newParent = new File(parent, files[i].getName());
                    boolean mkdirSuccess = newParent.mkdirs();
                    if (!mkdirSuccess && !newParent.exists()) {
                        throw new IllegalStateException("Failed to create usage stats directory during migration: " + newParent.getAbsolutePath());
                    }
                }
                copyRecursively(newParent, files[i]);
            }
            return;
        }
        try {
            Files.copy(f.toPath(), new File(parent, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Slog.e(TAG, "Failed to move usage stats file : " + f.toString());
            throw new RuntimeException(e);
        }
    }

    private void deleteLegacyDir(int userId) {
        File file = USAGE_STATS_LEGACY_DIR;
        File legacyUserDir = new File(file, Integer.toString(userId));
        if (legacyUserDir.exists()) {
            deleteRecursively(legacyUserDir);
            if (legacyUserDir.exists()) {
                Slog.w(TAG, "Error occurred while attempting to delete legacy usage stats dir for user " + userId);
            }
            if (file.list() != null && file.list().length == 0 && !file.delete()) {
                Slog.w(TAG, "Error occurred while attempting to delete legacy usage stats dir");
            }
        }
    }

    void shutdown() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(0);
            UsageEvents.Event event = new UsageEvents.Event(26, SystemClock.elapsedRealtime());
            event.mPackage = PackageManagerService.PLATFORM_PACKAGE_NAME;
            reportEventToAllUserId(event);
            flushToDiskLocked();
            persistGlobalComponentUsageLocked();
        }
        this.mAppStandby.flushToDisk();
    }

    void prepareForPossibleShutdown() {
        UsageEvents.Event event = new UsageEvents.Event(26, SystemClock.elapsedRealtime());
        event.mPackage = PackageManagerService.PLATFORM_PACKAGE_NAME;
        this.mHandler.obtainMessage(4, event).sendToTarget();
        this.mHandler.sendEmptyMessage(1);
    }

    private void loadPendingEventsLocked(int userId, LinkedList<UsageEvents.Event> pendingEvents) {
        File usageStatsDeDir = new File(Environment.getDataSystemDeDirectory(userId), "usagestats");
        File[] pendingEventsFiles = usageStatsDeDir.listFiles();
        if (pendingEventsFiles == null || pendingEventsFiles.length == 0) {
            return;
        }
        Arrays.sort(pendingEventsFiles);
        int numFiles = pendingEventsFiles.length;
        for (int i = 0; i < numFiles; i++) {
            AtomicFile af = new AtomicFile(pendingEventsFiles[i]);
            LinkedList<UsageEvents.Event> tmpEvents = new LinkedList<>();
            try {
                FileInputStream in = af.openRead();
                UsageStatsProtoV2.readPendingEvents(in, tmpEvents);
                if (in != null) {
                    in.close();
                }
                pendingEvents.addAll(tmpEvents);
            } catch (Exception e) {
                Slog.e(TAG, "Could not read " + pendingEventsFiles[i] + " for user " + userId);
            }
        }
    }

    private void persistPendingEventsLocked(int userId) {
        LinkedList<UsageEvents.Event> pendingEvents = this.mReportedEvents.get(userId);
        if (pendingEvents == null || pendingEvents.isEmpty()) {
            return;
        }
        File usageStatsDeDir = new File(Environment.getDataSystemDeDirectory(userId), "usagestats");
        if (!usageStatsDeDir.mkdirs() && !usageStatsDeDir.exists()) {
            throw new IllegalStateException("Usage stats DE directory does not exist: " + usageStatsDeDir.getAbsolutePath());
        }
        File pendingEventsFile = new File(usageStatsDeDir, "pendingevents_" + System.currentTimeMillis());
        AtomicFile af = new AtomicFile(pendingEventsFile);
        FileOutputStream fos = null;
        try {
            try {
                FileOutputStream fos2 = af.startWrite();
                UsageStatsProtoV2.writePendingEvents(fos2, pendingEvents);
                af.finishWrite(fos2);
                fos = null;
                pendingEvents.clear();
            } catch (Exception e) {
                Slog.e(TAG, "Failed to write " + pendingEventsFile.getAbsolutePath() + " for user " + userId);
            }
        } finally {
            af.failWrite(fos);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadGlobalComponentUsageLocked() {
        File[] packageUsageFile = COMMON_USAGE_STATS_DE_DIR.listFiles(new FilenameFilter() { // from class: com.android.server.usage.UsageStatsService$$ExternalSyntheticLambda0
            @Override // java.io.FilenameFilter
            public final boolean accept(File file, String str) {
                boolean equals;
                equals = TextUtils.equals(str, UsageStatsService.GLOBAL_COMPONENT_USAGE_FILE_NAME);
                return equals;
            }
        });
        if (packageUsageFile == null || packageUsageFile.length == 0) {
            return;
        }
        AtomicFile af = new AtomicFile(packageUsageFile[0]);
        Map<String, Long> tmpUsage = new ArrayMap<>();
        try {
            FileInputStream in = af.openRead();
            UsageStatsProtoV2.readGlobalComponentUsage(in, tmpUsage);
            if (in != null) {
                in.close();
            }
            Map.Entry<String, Long>[] entries = (Map.Entry[]) tmpUsage.entrySet().toArray();
            int size = entries.length;
            for (int i = 0; i < size; i++) {
                this.mLastTimeComponentUsedGlobal.putIfAbsent(entries[i].getKey(), entries[i].getValue());
            }
        } catch (Exception e) {
            Slog.e(TAG, "Could not read " + packageUsageFile[0]);
        }
    }

    private void persistGlobalComponentUsageLocked() {
        if (this.mLastTimeComponentUsedGlobal.isEmpty()) {
            return;
        }
        File file = COMMON_USAGE_STATS_DE_DIR;
        if (!file.mkdirs() && !file.exists()) {
            throw new IllegalStateException("Common usage stats DE directory does not exist: " + file.getAbsolutePath());
        }
        File lastTimePackageFile = new File(file, GLOBAL_COMPONENT_USAGE_FILE_NAME);
        AtomicFile af = new AtomicFile(lastTimePackageFile);
        FileOutputStream fos = null;
        try {
            try {
                fos = af.startWrite();
                UsageStatsProtoV2.writeGlobalComponentUsage(fos, this.mLastTimeComponentUsedGlobal);
                af.finishWrite(fos);
                fos = null;
            } catch (Exception e) {
                Slog.e(TAG, "Failed to write " + lastTimePackageFile.getAbsolutePath());
            }
        } finally {
            af.failWrite(fos);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportEventOrAddToQueue(int userId, UsageEvents.Event event) {
        if (this.mUserUnlockedStates.contains(Integer.valueOf(userId))) {
            this.mHandler.obtainMessage(0, userId, 0, event).sendToTarget();
            return;
        }
        synchronized (this.mLock) {
            LinkedList<UsageEvents.Event> events = this.mReportedEvents.get(userId);
            if (events == null) {
                events = new LinkedList<>();
                this.mReportedEvents.put(userId, events);
            }
            events.add(event);
            if (events.size() == 1) {
                this.mHandler.sendEmptyMessageDelayed(1, 1200000L);
            }
        }
    }

    private void convertToSystemTimeLocked(UsageEvents.Event event) {
        long actualSystemTime = System.currentTimeMillis();
        if (ENABLE_TIME_CHANGE_CORRECTION) {
            long actualRealtime = SystemClock.elapsedRealtime();
            long expectedSystemTime = (actualRealtime - this.mRealTimeSnapshot) + this.mSystemTimeSnapshot;
            long diffSystemTime = actualSystemTime - expectedSystemTime;
            if (Math.abs(diffSystemTime) > TIME_CHANGE_THRESHOLD_MILLIS) {
                Slog.i(TAG, "Time changed in by " + (diffSystemTime / 1000) + " seconds");
                this.mRealTimeSnapshot = actualRealtime;
                this.mSystemTimeSnapshot = actualSystemTime;
            }
        }
        event.mTimeStamp = Math.max(0L, event.mTimeStamp - this.mRealTimeSnapshot) + this.mSystemTimeSnapshot;
    }

    /* JADX WARN: Removed duplicated region for block: B:24:0x00a0 A[Catch: all -> 0x027a, TryCatch #4 {, blocks: (B:13:0x0029, B:15:0x0035, B:16:0x0083, B:18:0x0085, B:19:0x008b, B:88:0x0226, B:90:0x022c, B:92:0x022e, B:93:0x0231, B:21:0x0090, B:22:0x0094, B:24:0x00a0, B:25:0x00e4, B:27:0x00e6, B:29:0x00ea, B:30:0x00f1, B:31:0x00f3, B:35:0x0101, B:49:0x013c, B:51:0x0140, B:52:0x014c, B:55:0x0158, B:60:0x0165, B:61:0x0177, B:63:0x0183, B:64:0x0187, B:68:0x0195, B:70:0x01ae, B:72:0x01b4, B:67:0x018e, B:69:0x01a7, B:73:0x01c1, B:76:0x01d3, B:77:0x01d7, B:81:0x01e5, B:83:0x0208, B:85:0x0210, B:87:0x021f, B:80:0x01de, B:32:0x00f4, B:33:0x00fe, B:36:0x0102, B:38:0x0109, B:39:0x010f, B:43:0x0134, B:42:0x011c, B:44:0x0137), top: B:109:0x0029, inners: #5, #6, #7 }] */
    /* JADX WARN: Removed duplicated region for block: B:27:0x00e6 A[Catch: all -> 0x027a, TryCatch #4 {, blocks: (B:13:0x0029, B:15:0x0035, B:16:0x0083, B:18:0x0085, B:19:0x008b, B:88:0x0226, B:90:0x022c, B:92:0x022e, B:93:0x0231, B:21:0x0090, B:22:0x0094, B:24:0x00a0, B:25:0x00e4, B:27:0x00e6, B:29:0x00ea, B:30:0x00f1, B:31:0x00f3, B:35:0x0101, B:49:0x013c, B:51:0x0140, B:52:0x014c, B:55:0x0158, B:60:0x0165, B:61:0x0177, B:63:0x0183, B:64:0x0187, B:68:0x0195, B:70:0x01ae, B:72:0x01b4, B:67:0x018e, B:69:0x01a7, B:73:0x01c1, B:76:0x01d3, B:77:0x01d7, B:81:0x01e5, B:83:0x0208, B:85:0x0210, B:87:0x021f, B:80:0x01de, B:32:0x00f4, B:33:0x00fe, B:36:0x0102, B:38:0x0109, B:39:0x010f, B:43:0x0134, B:42:0x011c, B:44:0x0137), top: B:109:0x0029, inners: #5, #6, #7 }] */
    /* JADX WARN: Removed duplicated region for block: B:90:0x022c A[Catch: all -> 0x027a, DONT_GENERATE, TryCatch #4 {, blocks: (B:13:0x0029, B:15:0x0035, B:16:0x0083, B:18:0x0085, B:19:0x008b, B:88:0x0226, B:90:0x022c, B:92:0x022e, B:93:0x0231, B:21:0x0090, B:22:0x0094, B:24:0x00a0, B:25:0x00e4, B:27:0x00e6, B:29:0x00ea, B:30:0x00f1, B:31:0x00f3, B:35:0x0101, B:49:0x013c, B:51:0x0140, B:52:0x014c, B:55:0x0158, B:60:0x0165, B:61:0x0177, B:63:0x0183, B:64:0x0187, B:68:0x0195, B:70:0x01ae, B:72:0x01b4, B:67:0x018e, B:69:0x01a7, B:73:0x01c1, B:76:0x01d3, B:77:0x01d7, B:81:0x01e5, B:83:0x0208, B:85:0x0210, B:87:0x021f, B:80:0x01de, B:32:0x00f4, B:33:0x00fe, B:36:0x0102, B:38:0x0109, B:39:0x010f, B:43:0x0134, B:42:0x011c, B:44:0x0137), top: B:109:0x0029, inners: #5, #6, #7 }] */
    /* JADX WARN: Removed duplicated region for block: B:92:0x022e A[Catch: all -> 0x027a, TryCatch #4 {, blocks: (B:13:0x0029, B:15:0x0035, B:16:0x0083, B:18:0x0085, B:19:0x008b, B:88:0x0226, B:90:0x022c, B:92:0x022e, B:93:0x0231, B:21:0x0090, B:22:0x0094, B:24:0x00a0, B:25:0x00e4, B:27:0x00e6, B:29:0x00ea, B:30:0x00f1, B:31:0x00f3, B:35:0x0101, B:49:0x013c, B:51:0x0140, B:52:0x014c, B:55:0x0158, B:60:0x0165, B:61:0x0177, B:63:0x0183, B:64:0x0187, B:68:0x0195, B:70:0x01ae, B:72:0x01b4, B:67:0x018e, B:69:0x01a7, B:73:0x01c1, B:76:0x01d3, B:77:0x01d7, B:81:0x01e5, B:83:0x0208, B:85:0x0210, B:87:0x021f, B:80:0x01de, B:32:0x00f4, B:33:0x00fe, B:36:0x0102, B:38:0x0109, B:39:0x010f, B:43:0x0134, B:42:0x011c, B:44:0x0137), top: B:109:0x0029, inners: #5, #6, #7 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    void reportEvent(UsageEvents.Event event, int userId) {
        int uid;
        ActivityData prevData;
        ArraySet<String> tokens;
        UserUsageStatsService service;
        switch (event.mEventType) {
            case 1:
            case 2:
            case 23:
                uid = this.mPackageManagerInternal.getPackageUid(event.mPackage, 0L, userId);
                break;
            default:
                uid = 0;
                break;
        }
        if (event.mPackage != null && this.mPackageManagerInternal.isPackageEphemeral(userId, event.mPackage)) {
            event.mFlags |= 1;
        }
        synchronized (this.mLock) {
            if (!this.mUserUnlockedStates.contains(Integer.valueOf(userId))) {
                Slog.wtf(TAG, "Failed to report event for locked user " + userId + " (" + event.mPackage + SliceClientPermissions.SliceAuthority.DELIMITER + event.mClass + " eventType:" + event.mEventType + " instanceId:" + event.mInstanceId + ")");
                return;
            }
            switch (event.mEventType) {
                case 1:
                    FrameworkStatsLog.write(269, uid, event.mPackage, "", 1);
                    if (this.mVisibleActivities.get(event.mInstanceId) == null) {
                        String usageSourcePackage = getUsageSourcePackage(event);
                        try {
                            this.mAppTimeLimit.noteUsageStart(usageSourcePackage, userId);
                        } catch (IllegalArgumentException iae) {
                            Slog.e(TAG, "Failed to note usage start", iae);
                        }
                        ActivityData resumedData = new ActivityData(event.mTaskRootPackage, event.mTaskRootClass, usageSourcePackage);
                        resumedData.lastEvent = 1;
                        this.mVisibleActivities.put(event.mInstanceId, resumedData);
                        long estimatedLaunchTime = this.mAppStandby.getEstimatedLaunchTime(event.mPackage, userId);
                        long now = System.currentTimeMillis();
                        if (estimatedLaunchTime < now || estimatedLaunchTime > 604800000 + now) {
                            this.mAppStandby.setEstimatedLaunchTime(event.mPackage, userId, 0L);
                            if (stageChangedEstimatedLaunchTime(userId, event.mPackage)) {
                                this.mHandler.sendEmptyMessage(9);
                            }
                        }
                    }
                    service = getUserUsageStatsServiceLocked(userId);
                    if (service == null) {
                        return;
                    }
                    service.reportEvent(event);
                    int size = this.mUsageEventListeners.size();
                    for (int i = 0; i < size; i++) {
                        try {
                            this.mUsageEventListeners.valueAt(i).onUsageEvent(userId, event);
                        } catch (Exception e) {
                            Slog.w(TAG, e.getMessage() + " userId = " + userId + ", event = " + event.toString());
                        }
                    }
                    return;
                case 2:
                    ActivityData pausedData = this.mVisibleActivities.get(event.mInstanceId);
                    if (pausedData == null) {
                        String usageSourcePackage2 = getUsageSourcePackage(event);
                        try {
                            this.mAppTimeLimit.noteUsageStart(usageSourcePackage2, userId);
                        } catch (IllegalArgumentException iae2) {
                            Slog.e(TAG, "Failed to note usage start", iae2);
                        }
                        pausedData = new ActivityData(event.mTaskRootPackage, event.mTaskRootClass, usageSourcePackage2);
                        this.mVisibleActivities.put(event.mInstanceId, pausedData);
                    } else {
                        FrameworkStatsLog.write(269, uid, event.mPackage, "", 2);
                    }
                    pausedData.lastEvent = 2;
                    if (event.mTaskRootPackage == null) {
                        event.mTaskRootPackage = pausedData.mTaskRootPackage;
                        event.mTaskRootClass = pausedData.mTaskRootClass;
                    }
                    service = getUserUsageStatsServiceLocked(userId);
                    if (service == null) {
                    }
                    break;
                case 7:
                case 31:
                    convertToSystemTimeLocked(event);
                    this.mLastTimeComponentUsedGlobal.put(event.mPackage, Long.valueOf(event.mTimeStamp));
                    service = getUserUsageStatsServiceLocked(userId);
                    if (service == null) {
                    }
                    break;
                case 23:
                    prevData = (ActivityData) this.mVisibleActivities.removeReturnOld(event.mInstanceId);
                    if (prevData != null) {
                        Slog.w(TAG, "Unexpected activity event reported! (" + event.mPackage + SliceClientPermissions.SliceAuthority.DELIMITER + event.mClass + " event : " + event.mEventType + " instanceId : " + event.mInstanceId + ")");
                        return;
                    }
                    if (prevData.lastEvent != 2) {
                        FrameworkStatsLog.write(269, uid, event.mPackage, "", 2);
                    }
                    synchronized (this.mUsageReporters) {
                        tokens = (ArraySet) this.mUsageReporters.removeReturnOld(event.mInstanceId);
                    }
                    if (tokens != null) {
                        synchronized (tokens) {
                            int size2 = tokens.size();
                            for (int i2 = 0; i2 < size2; i2++) {
                                String token = tokens.valueAt(i2);
                                try {
                                    this.mAppTimeLimit.noteUsageStop(buildFullToken(event.mPackage, token), userId);
                                } catch (IllegalArgumentException iae3) {
                                    Slog.w(TAG, "Failed to stop usage for during reporter death: " + iae3);
                                }
                            }
                        }
                    }
                    if (event.mTaskRootPackage == null) {
                        event.mTaskRootPackage = prevData.mTaskRootPackage;
                        event.mTaskRootClass = prevData.mTaskRootClass;
                    }
                    try {
                        this.mAppTimeLimit.noteUsageStop(prevData.mUsageSourcePackage, userId);
                    } catch (IllegalArgumentException iae4) {
                        Slog.w(TAG, "Failed to note usage stop", iae4);
                    }
                    service = getUserUsageStatsServiceLocked(userId);
                    if (service == null) {
                    }
                    break;
                case 24:
                    event.mEventType = 23;
                    prevData = (ActivityData) this.mVisibleActivities.removeReturnOld(event.mInstanceId);
                    if (prevData != null) {
                    }
                    break;
                default:
                    service = getUserUsageStatsServiceLocked(userId);
                    if (service == null) {
                    }
                    break;
            }
        }
    }

    private String getUsageSourcePackage(UsageEvents.Event event) {
        switch (this.mUsageSource) {
            case 2:
                return event.mPackage;
            default:
                return event.mTaskRootPackage;
        }
    }

    void reportEventToAllUserId(UsageEvents.Event event) {
        synchronized (this.mLock) {
            int userCount = this.mUserState.size();
            for (int i = 0; i < userCount; i++) {
                UsageEvents.Event copy = new UsageEvents.Event(event);
                reportEventOrAddToQueue(this.mUserState.keyAt(i), copy);
            }
        }
    }

    void flushToDisk() {
        synchronized (this.mLock) {
            UsageEvents.Event event = new UsageEvents.Event(25, SystemClock.elapsedRealtime());
            event.mPackage = PackageManagerService.PLATFORM_PACKAGE_NAME;
            reportEventToAllUserId(event);
            flushToDiskLocked();
        }
        this.mAppStandby.flushToDisk();
    }

    void onUserRemoved(int userId) {
        synchronized (this.mLock) {
            Slog.i(TAG, "Removing user " + userId + " and all data.");
            this.mUserState.remove(userId);
            this.mAppTimeLimit.onUserRemoved(userId);
            LaunchTimeAlarmQueue alarmQueue = this.mLaunchTimeAlarmQueues.get(userId);
            if (alarmQueue != null) {
                alarmQueue.removeAllAlarms();
                this.mLaunchTimeAlarmQueues.remove(userId);
            }
        }
        synchronized (this.mPendingLaunchTimeChangePackages) {
            this.mPendingLaunchTimeChangePackages.remove(userId);
        }
        this.mAppStandby.onUserRemoved(userId);
        UsageStatsIdleService.cancelJob(getContext(), userId);
        UsageStatsIdleService.cancelUpdateMappingsJob(getContext());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPackageRemoved(int userId, String packageName) {
        synchronized (this.mPendingLaunchTimeChangePackages) {
            ArraySet<String> pkgNames = this.mPendingLaunchTimeChangePackages.get(userId);
            if (pkgNames != null) {
                pkgNames.remove(packageName);
            }
        }
        synchronized (this.mLock) {
            System.currentTimeMillis();
            if (this.mUserUnlockedStates.contains(Integer.valueOf(userId))) {
                LaunchTimeAlarmQueue alarmQueue = this.mLaunchTimeAlarmQueues.get(userId);
                if (alarmQueue != null) {
                    alarmQueue.removeAlarmForKey(packageName);
                }
                UserUsageStatsService userService = this.mUserState.get(userId);
                if (userService == null) {
                    return;
                }
                Slog.i(TAG, "Do not remove uninstalled app's usage.");
                if (-1 != -1) {
                    UsageStatsIdleService.scheduleJob(getContext(), userId);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean pruneUninstalledPackagesData(int userId) {
        synchronized (this.mLock) {
            if (this.mUserUnlockedStates.contains(Integer.valueOf(userId))) {
                UserUsageStatsService userService = this.mUserState.get(userId);
                if (userService == null) {
                    return false;
                }
                return userService.pruneUninstalledPackagesData();
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean updatePackageMappingsData() {
        if (!shouldDeleteObsoleteData(UserHandle.SYSTEM)) {
            return true;
        }
        HashMap<String, Long> installedPkgs = getInstalledPackages(0);
        synchronized (this.mLock) {
            if (!this.mUserUnlockedStates.contains(0)) {
                return false;
            }
            UserUsageStatsService userService = this.mUserState.get(0);
            if (userService == null) {
                return false;
            }
            return userService.updatePackageMappingsLocked(installedPkgs);
        }
    }

    List<UsageStats> queryUsageStats(int userId, int bucketType, long beginTime, long endTime, boolean obfuscateInstantApps) {
        synchronized (this.mLock) {
            if (!this.mUserUnlockedStates.contains(Integer.valueOf(userId))) {
                Slog.w(TAG, "Failed to query usage stats for locked user " + userId);
                return null;
            }
            UserUsageStatsService service = getUserUsageStatsServiceLocked(userId);
            if (service == null) {
                return null;
            }
            List<UsageStats> list = service.queryUsageStats(bucketType, beginTime, endTime);
            if (list == null) {
                return null;
            }
            if (obfuscateInstantApps) {
                for (int i = list.size() - 1; i >= 0; i--) {
                    UsageStats stats = list.get(i);
                    if (this.mPackageManagerInternal.isPackageEphemeral(userId, stats.mPackageName)) {
                        list.set(i, stats.getObfuscatedForInstantApp());
                    }
                }
            }
            return list;
        }
    }

    List<ConfigurationStats> queryConfigurationStats(int userId, int bucketType, long beginTime, long endTime) {
        synchronized (this.mLock) {
            if (!this.mUserUnlockedStates.contains(Integer.valueOf(userId))) {
                Slog.w(TAG, "Failed to query configuration stats for locked user " + userId);
                return null;
            }
            UserUsageStatsService service = getUserUsageStatsServiceLocked(userId);
            if (service == null) {
                return null;
            }
            return service.queryConfigurationStats(bucketType, beginTime, endTime);
        }
    }

    List<EventStats> queryEventStats(int userId, int bucketType, long beginTime, long endTime) {
        synchronized (this.mLock) {
            if (!this.mUserUnlockedStates.contains(Integer.valueOf(userId))) {
                Slog.w(TAG, "Failed to query event stats for locked user " + userId);
                return null;
            }
            UserUsageStatsService service = getUserUsageStatsServiceLocked(userId);
            if (service == null) {
                return null;
            }
            return service.queryEventStats(bucketType, beginTime, endTime);
        }
    }

    UsageEvents queryEvents(int userId, long beginTime, long endTime, int flags) {
        synchronized (this.mLock) {
            if (!this.mUserUnlockedStates.contains(Integer.valueOf(userId))) {
                Slog.w(TAG, "Failed to query events for locked user " + userId);
                return null;
            }
            UserUsageStatsService service = getUserUsageStatsServiceLocked(userId);
            if (service == null) {
                return null;
            }
            return service.queryEvents(beginTime, endTime, flags);
        }
    }

    UsageEvents queryEventsForPackage(int userId, long beginTime, long endTime, String packageName, boolean includeTaskRoot) {
        synchronized (this.mLock) {
            try {
                try {
                    if (!this.mUserUnlockedStates.contains(Integer.valueOf(userId))) {
                        Slog.w(TAG, "Failed to query package events for locked user " + userId);
                        return null;
                    }
                    UserUsageStatsService service = getUserUsageStatsServiceLocked(userId);
                    if (service == null) {
                        return null;
                    }
                    return service.queryEventsForPackage(beginTime, endTime, packageName, includeTaskRoot);
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    private UsageEvents queryEarliestAppEvents(int userId, long beginTime, long endTime, int eventType) {
        synchronized (this.mLock) {
            if (!this.mUserUnlockedStates.contains(Integer.valueOf(userId))) {
                Slog.w(TAG, "Failed to query earliest events for locked user " + userId);
                return null;
            }
            UserUsageStatsService service = getUserUsageStatsServiceLocked(userId);
            if (service == null) {
                return null;
            }
            return service.queryEarliestAppEvents(beginTime, endTime, eventType);
        }
    }

    private UsageEvents queryEarliestEventsForPackage(int userId, long beginTime, long endTime, String packageName, int eventType) {
        synchronized (this.mLock) {
            try {
                try {
                    if (!this.mUserUnlockedStates.contains(Integer.valueOf(userId))) {
                        Slog.w(TAG, "Failed to query earliest package events for locked user " + userId);
                        return null;
                    }
                    UserUsageStatsService service = getUserUsageStatsServiceLocked(userId);
                    if (service == null) {
                        return null;
                    }
                    return service.queryEarliestEventsForPackage(beginTime, endTime, packageName, eventType);
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    long getEstimatedPackageLaunchTime(int userId, String packageName) {
        long estimatedLaunchTime = this.mAppStandby.getEstimatedLaunchTime(packageName, userId);
        long now = System.currentTimeMillis();
        if (estimatedLaunchTime < now || estimatedLaunchTime == JobStatus.NO_LATEST_RUNTIME) {
            estimatedLaunchTime = calculateEstimatedPackageLaunchTime(userId, packageName);
            this.mAppStandby.setEstimatedLaunchTime(packageName, userId, estimatedLaunchTime);
            synchronized (this.mLock) {
                LaunchTimeAlarmQueue alarmQueue = this.mLaunchTimeAlarmQueues.get(userId);
                if (alarmQueue == null) {
                    alarmQueue = new LaunchTimeAlarmQueue(userId, getContext(), BackgroundThread.get().getLooper());
                    this.mLaunchTimeAlarmQueues.put(userId, alarmQueue);
                }
                alarmQueue.addAlarm(packageName, SystemClock.elapsedRealtime() + (estimatedLaunchTime - now));
            }
        }
        return estimatedLaunchTime;
    }

    private long calculateEstimatedPackageLaunchTime(int userId, String packageName) {
        synchronized (this.mLock) {
            long endTime = System.currentTimeMillis();
            long beginTime = endTime - 604800000;
            long unknownTime = endTime + 31536000000L;
            UsageEvents events = queryEarliestEventsForPackage(userId, beginTime, endTime, packageName, 1);
            if (events == null) {
                return unknownTime;
            }
            UsageEvents.Event event = new UsageEvents.Event();
            if (events.getNextEvent(event)) {
                boolean hasMoreThan24HoursOfHistory = endTime - event.getTimeStamp() > 86400000;
                do {
                    if (event.getEventType() == 1) {
                        long timestamp = event.getTimeStamp();
                        long nextLaunch = calculateNextLaunchTime(hasMoreThan24HoursOfHistory, timestamp);
                        if (nextLaunch > endTime) {
                            return nextLaunch;
                        }
                    }
                } while (events.getNextEvent(event));
                return unknownTime;
            }
            return unknownTime;
        }
    }

    private static long calculateNextLaunchTime(boolean hasMoreThan24HoursOfHistory, long eventTimestamp) {
        if (hasMoreThan24HoursOfHistory) {
            return 604800000 + eventTimestamp;
        }
        return 86400000 + eventTimestamp;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:31:0x00bf A[Catch: all -> 0x00eb, TryCatch #0 {, blocks: (B:4:0x0007, B:6:0x0024, B:8:0x0026, B:10:0x003a, B:11:0x0051, B:13:0x0058, B:15:0x0062, B:19:0x0072, B:20:0x0079, B:22:0x0080, B:29:0x00b6, B:31:0x00bf, B:32:0x00c4, B:34:0x00d0, B:28:0x0099, B:37:0x00e2, B:38:0x00e9), top: B:43:0x0007 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void handleEstimatedLaunchTimesOnUserUnlock(int userId) {
        UsageEvents events;
        ArrayMap<String, Boolean> hasMoreThan24HoursOfHistory;
        synchronized (this.mLock) {
            long nowElapsed = SystemClock.elapsedRealtime();
            long now = System.currentTimeMillis();
            long beginTime = now - 604800000;
            UsageEvents events2 = queryEarliestAppEvents(userId, beginTime, now, 1);
            if (events2 == null) {
                return;
            }
            ArrayMap<String, Boolean> hasMoreThan24HoursOfHistory2 = new ArrayMap<>();
            UsageEvents.Event event = new UsageEvents.Event();
            LaunchTimeAlarmQueue alarmQueue = this.mLaunchTimeAlarmQueues.get(userId);
            if (alarmQueue == null) {
                alarmQueue = new LaunchTimeAlarmQueue(userId, getContext(), BackgroundThread.get().getLooper());
                this.mLaunchTimeAlarmQueues.put(userId, alarmQueue);
            }
            boolean changedTimes = false;
            boolean unprocessedEvent = events2.getNextEvent(event);
            while (unprocessedEvent) {
                String packageName = event.getPackageName();
                if (!hasMoreThan24HoursOfHistory2.containsKey(packageName)) {
                    boolean hasHistory = now - event.getTimeStamp() > 86400000;
                    hasMoreThan24HoursOfHistory2.put(packageName, Boolean.valueOf(hasHistory));
                }
                if (event.getEventType() != 1) {
                    events = events2;
                    hasMoreThan24HoursOfHistory = hasMoreThan24HoursOfHistory2;
                } else {
                    long estimatedLaunchTime = this.mAppStandby.getEstimatedLaunchTime(packageName, userId);
                    if (estimatedLaunchTime >= now && estimatedLaunchTime != JobStatus.NO_LATEST_RUNTIME) {
                        events = events2;
                        hasMoreThan24HoursOfHistory = hasMoreThan24HoursOfHistory2;
                        if (estimatedLaunchTime < now + 604800000) {
                            changedTimes |= stageChangedEstimatedLaunchTime(userId, packageName);
                        }
                        alarmQueue.addAlarm(packageName, nowElapsed + (estimatedLaunchTime - now));
                    }
                    events = events2;
                    hasMoreThan24HoursOfHistory = hasMoreThan24HoursOfHistory2;
                    long estimatedLaunchTime2 = calculateNextLaunchTime(hasMoreThan24HoursOfHistory2.get(packageName).booleanValue(), event.getTimeStamp());
                    this.mAppStandby.setEstimatedLaunchTime(packageName, userId, estimatedLaunchTime2);
                    estimatedLaunchTime = estimatedLaunchTime2;
                    if (estimatedLaunchTime < now + 604800000) {
                    }
                    alarmQueue.addAlarm(packageName, nowElapsed + (estimatedLaunchTime - now));
                }
                events2 = events;
                unprocessedEvent = events2.getNextEvent(event);
                hasMoreThan24HoursOfHistory2 = hasMoreThan24HoursOfHistory;
            }
            if (changedTimes) {
                this.mHandler.sendEmptyMessage(9);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setEstimatedLaunchTime(int userId, String packageName, long estimatedLaunchTime) {
        long now = System.currentTimeMillis();
        if (estimatedLaunchTime <= now) {
            return;
        }
        long oldEstimatedLaunchTime = this.mAppStandby.getEstimatedLaunchTime(packageName, userId);
        if (estimatedLaunchTime != oldEstimatedLaunchTime) {
            this.mAppStandby.setEstimatedLaunchTime(packageName, userId, estimatedLaunchTime);
            if (stageChangedEstimatedLaunchTime(userId, packageName)) {
                this.mHandler.sendEmptyMessage(9);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setEstimatedLaunchTimes(int userId, List<AppLaunchEstimateInfo> launchEstimates) {
        boolean changedTimes = false;
        long now = System.currentTimeMillis();
        for (int i = launchEstimates.size() - 1; i >= 0; i--) {
            AppLaunchEstimateInfo estimate = launchEstimates.get(i);
            if (estimate.estimatedLaunchTime > now) {
                long oldEstimatedLaunchTime = this.mAppStandby.getEstimatedLaunchTime(estimate.packageName, userId);
                if (estimate.estimatedLaunchTime != oldEstimatedLaunchTime) {
                    this.mAppStandby.setEstimatedLaunchTime(estimate.packageName, userId, estimate.estimatedLaunchTime);
                    changedTimes |= stageChangedEstimatedLaunchTime(userId, estimate.packageName);
                }
            }
        }
        if (changedTimes) {
            this.mHandler.sendEmptyMessage(9);
        }
    }

    private boolean stageChangedEstimatedLaunchTime(int userId, String packageName) {
        boolean add;
        synchronized (this.mPendingLaunchTimeChangePackages) {
            add = this.mPendingLaunchTimeChangePackages.add(userId, packageName);
        }
        return add;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerListener(UsageStatsManagerInternal.UsageEventListener listener) {
        synchronized (this.mLock) {
            this.mUsageEventListeners.add(listener);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterListener(UsageStatsManagerInternal.UsageEventListener listener) {
        synchronized (this.mLock) {
            this.mUsageEventListeners.remove(listener);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerLaunchTimeChangedListener(UsageStatsManagerInternal.EstimatedLaunchTimeChangedListener listener) {
        this.mEstimatedLaunchTimeChangedListeners.add(listener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterLaunchTimeChangedListener(UsageStatsManagerInternal.EstimatedLaunchTimeChangedListener listener) {
        this.mEstimatedLaunchTimeChangedListeners.remove(listener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean shouldDeleteObsoleteData(UserHandle userHandle) {
        DevicePolicyManagerInternal dpmInternal = getDpmInternal();
        return dpmInternal == null || dpmInternal.getProfileOwnerOrDeviceOwnerSupervisionComponent(userHandle) == null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String buildFullToken(String packageName, String token) {
        StringBuilder sb = new StringBuilder(packageName.length() + token.length() + 1);
        sb.append(packageName);
        sb.append(TOKEN_DELIMITER);
        sb.append(token);
        return sb.toString();
    }

    private void flushToDiskLocked() {
        int userCount = this.mUserState.size();
        for (int i = 0; i < userCount; i++) {
            int userId = this.mUserState.keyAt(i);
            if (!this.mUserUnlockedStates.contains(Integer.valueOf(userId))) {
                persistPendingEventsLocked(userId);
            } else {
                UserUsageStatsService service = this.mUserState.get(userId);
                if (service != null) {
                    service.persistActiveStats();
                }
            }
        }
        this.mHandler.removeMessages(1);
    }

    void dump(String[] args, PrintWriter pw) {
        boolean compact;
        boolean checkin;
        int[] userIds;
        IndentingPrintWriter idpw = new IndentingPrintWriter(pw, "  ");
        ArrayList<String> pkgs = new ArrayList<>();
        String[] strArr = null;
        if (args == null) {
            compact = false;
            checkin = false;
        } else {
            int i = 0;
            compact = false;
            checkin = false;
            while (i < args.length) {
                String arg = args[i];
                if ("--checkin".equals(arg)) {
                    checkin = true;
                } else if ("-c".equals(arg)) {
                    compact = true;
                } else if ("flush".equals(arg)) {
                    synchronized (this.mLock) {
                        flushToDiskLocked();
                    }
                    this.mAppStandby.flushToDisk();
                    pw.println("Flushed stats to disk");
                    return;
                } else if ("is-app-standby-enabled".equals(arg)) {
                    pw.println(this.mAppStandby.isAppIdleEnabled());
                    return;
                } else if ("apptimelimit".equals(arg)) {
                    synchronized (this.mLock) {
                        if (i + 1 >= args.length) {
                            this.mAppTimeLimit.dump(strArr, pw);
                        } else {
                            String[] remainingArgs = (String[]) Arrays.copyOfRange(args, i + 1, args.length);
                            this.mAppTimeLimit.dump(remainingArgs, pw);
                        }
                    }
                    return;
                } else if ("file".equals(arg)) {
                    IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
                    synchronized (this.mLock) {
                        if (i + 1 >= args.length) {
                            int numUsers = this.mUserState.size();
                            for (int user = 0; user < numUsers; user++) {
                                int userId = this.mUserState.keyAt(user);
                                if (this.mUserUnlockedStates.contains(Integer.valueOf(userId))) {
                                    ipw.println("user=" + userId);
                                    ipw.increaseIndent();
                                    this.mUserState.valueAt(user).dumpFile(ipw, null);
                                    ipw.decreaseIndent();
                                }
                            }
                        } else {
                            int user2 = parseUserIdFromArgs(args, i, ipw);
                            if (user2 != -10000) {
                                String[] remainingArgs2 = (String[]) Arrays.copyOfRange(args, i + 2, args.length);
                                this.mUserState.get(user2).dumpFile(ipw, remainingArgs2);
                            }
                        }
                    }
                    return;
                } else if ("database-info".equals(arg)) {
                    IndentingPrintWriter ipw2 = new IndentingPrintWriter(pw, "  ");
                    synchronized (this.mLock) {
                        if (i + 1 >= args.length) {
                            int numUsers2 = this.mUserState.size();
                            for (int user3 = 0; user3 < numUsers2; user3++) {
                                int userId2 = this.mUserState.keyAt(user3);
                                if (this.mUserUnlockedStates.contains(Integer.valueOf(userId2))) {
                                    ipw2.println("user=" + userId2);
                                    ipw2.increaseIndent();
                                    this.mUserState.valueAt(user3).dumpDatabaseInfo(ipw2);
                                    ipw2.decreaseIndent();
                                }
                            }
                        } else {
                            int user4 = parseUserIdFromArgs(args, i, ipw2);
                            if (user4 != -10000) {
                                this.mUserState.get(user4).dumpDatabaseInfo(ipw2);
                            }
                        }
                    }
                    return;
                } else if ("appstandby".equals(arg)) {
                    this.mAppStandby.dumpState(args, pw);
                    return;
                } else if ("stats-directory".equals(arg)) {
                    IndentingPrintWriter ipw3 = new IndentingPrintWriter(pw, "  ");
                    synchronized (this.mLock) {
                        int userId3 = parseUserIdFromArgs(args, i, ipw3);
                        if (userId3 != -10000) {
                            ipw3.println(new File(Environment.getDataSystemCeDirectory(userId3), "usagestats").getAbsolutePath());
                        }
                    }
                    return;
                } else if ("mappings".equals(arg)) {
                    IndentingPrintWriter ipw4 = new IndentingPrintWriter(pw, "  ");
                    synchronized (this.mLock) {
                        int userId4 = parseUserIdFromArgs(args, i, ipw4);
                        if (userId4 != -10000) {
                            this.mUserState.get(userId4).dumpMappings(ipw4);
                        }
                    }
                    return;
                } else if ("broadcast-response-stats".equals(arg)) {
                    synchronized (this.mLock) {
                        this.mResponseStatsTracker.dump(idpw);
                    }
                    return;
                } else if (arg != null && !arg.startsWith("-")) {
                    pkgs.add(arg);
                }
                i++;
                strArr = null;
            }
        }
        synchronized (this.mLock) {
            int userCount = this.mUserState.size();
            userIds = new int[userCount];
            for (int i2 = 0; i2 < userCount; i2++) {
                int userId5 = this.mUserState.keyAt(i2);
                userIds[i2] = userId5;
                idpw.printPair("user", Integer.valueOf(userId5));
                idpw.println();
                idpw.increaseIndent();
                if (this.mUserUnlockedStates.contains(Integer.valueOf(userId5))) {
                    if (checkin) {
                        this.mUserState.valueAt(i2).checkin(idpw);
                    } else {
                        this.mUserState.valueAt(i2).dump(idpw, pkgs, compact);
                        idpw.println();
                    }
                }
                idpw.decreaseIndent();
            }
            idpw.println();
            idpw.printPair("Usage Source", UsageStatsManager.usageSourceToString(this.mUsageSource));
            idpw.println();
            this.mAppTimeLimit.dump(null, pw);
            idpw.println();
            this.mResponseStatsTracker.dump(idpw);
        }
        this.mAppStandby.dumpUsers(idpw, userIds, pkgs);
        if (CollectionUtils.isEmpty(pkgs)) {
            pw.println();
            this.mAppStandby.dumpState(args, pw);
        }
    }

    private int parseUserIdFromArgs(String[] args, int index, IndentingPrintWriter ipw) {
        try {
            int userId = Integer.parseInt(args[index + 1]);
            if (this.mUserState.indexOfKey(userId) < 0) {
                ipw.println("the specified user does not exist.");
                return -10000;
            } else if (!this.mUserUnlockedStates.contains(Integer.valueOf(userId))) {
                ipw.println("the specified user is currently in a locked state.");
                return -10000;
            } else {
                return userId;
            }
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            ipw.println("invalid user specified.");
            return -10000;
        }
    }

    /* loaded from: classes2.dex */
    class H extends Handler {
        public H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int numUsers;
            int userId;
            switch (msg.what) {
                case 0:
                    UsageStatsService.this.reportEvent((UsageEvents.Event) msg.obj, msg.arg1);
                    return;
                case 1:
                    UsageStatsService.this.flushToDisk();
                    return;
                case 2:
                    UsageStatsService.this.onUserRemoved(msg.arg1);
                    return;
                case 3:
                    int uid = msg.arg1;
                    int procState = msg.arg2;
                    int newCounter = procState <= 2 ? 0 : 1;
                    synchronized (UsageStatsService.this.mUidToKernelCounter) {
                        int oldCounter = UsageStatsService.this.mUidToKernelCounter.get(uid, 0);
                        if (newCounter != oldCounter) {
                            UsageStatsService.this.mUidToKernelCounter.put(uid, newCounter);
                            try {
                                FileUtils.stringToFile(UsageStatsService.KERNEL_COUNTER_FILE, uid + " " + newCounter);
                            } catch (IOException e) {
                                Slog.w(UsageStatsService.TAG, "Failed to update counter set: " + e);
                            }
                        }
                    }
                    return;
                case 4:
                    UsageStatsService.this.reportEventToAllUserId((UsageEvents.Event) msg.obj);
                    return;
                case 5:
                    try {
                        UsageStatsService.this.onUserUnlocked(msg.arg1);
                        return;
                    } catch (Exception e2) {
                        if (UsageStatsService.this.mUserManager.isUserUnlocked(msg.arg1)) {
                            throw e2;
                        }
                        Slog.w(UsageStatsService.TAG, "Attempted to unlock stopped or removed user " + msg.arg1);
                        return;
                    }
                case 6:
                    UsageStatsService.this.onPackageRemoved(msg.arg1, (String) msg.obj);
                    return;
                case 7:
                    synchronized (UsageStatsService.this.mLock) {
                        UsageStatsService.this.loadGlobalComponentUsageLocked();
                    }
                    return;
                case 8:
                    UsageStatsService.this.handleEstimatedLaunchTimesOnUserUnlock(msg.arg1);
                    return;
                case 9:
                    removeMessages(9);
                    ArraySet<String> pkgNames = new ArraySet<>();
                    synchronized (UsageStatsService.this.mPendingLaunchTimeChangePackages) {
                        numUsers = UsageStatsService.this.mPendingLaunchTimeChangePackages.size();
                    }
                    for (int u = numUsers - 1; u >= 0; u--) {
                        pkgNames.clear();
                        synchronized (UsageStatsService.this.mPendingLaunchTimeChangePackages) {
                            userId = UsageStatsService.this.mPendingLaunchTimeChangePackages.keyAt(u);
                            pkgNames.addAll(UsageStatsService.this.mPendingLaunchTimeChangePackages.get(userId));
                            UsageStatsService.this.mPendingLaunchTimeChangePackages.remove(userId);
                        }
                        for (int p = pkgNames.size() - 1; p >= 0; p--) {
                            String pkgName = pkgNames.valueAt(p);
                            long nextEstimatedLaunchTime = UsageStatsService.this.getEstimatedPackageLaunchTime(userId, pkgName);
                            Iterator it = UsageStatsService.this.mEstimatedLaunchTimeChangedListeners.iterator();
                            while (it.hasNext()) {
                                UsageStatsManagerInternal.EstimatedLaunchTimeChangedListener listener = (UsageStatsManagerInternal.EstimatedLaunchTimeChangedListener) it.next();
                                listener.onEstimatedLaunchTimeChanged(userId, pkgName, nextEstimatedLaunchTime);
                            }
                        }
                    }
                    return;
                default:
                    super.handleMessage(msg);
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearLastUsedTimestamps(String packageName, int userId) {
        this.mAppStandby.clearLastUsedTimestampsForTest(packageName, userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class BinderService extends IUsageStatsManager.Stub {
        private BinderService() {
        }

        private boolean hasPermission(String callingPackage) {
            int callingUid = Binder.getCallingUid();
            if (callingUid == 1000) {
                return true;
            }
            int mode = UsageStatsService.this.mAppOps.noteOp(43, callingUid, callingPackage);
            return mode == 3 ? UsageStatsService.this.getContext().checkCallingPermission("android.permission.PACKAGE_USAGE_STATS") == 0 : mode == 0;
        }

        private boolean hasObserverPermission() {
            int callingUid = Binder.getCallingUid();
            DevicePolicyManagerInternal dpmInternal = UsageStatsService.this.getDpmInternal();
            return callingUid == 1000 || (dpmInternal != null && (dpmInternal.isActiveProfileOwner(callingUid) || dpmInternal.isActiveDeviceOwner(callingUid))) || UsageStatsService.this.getContext().checkCallingPermission("android.permission.OBSERVE_APP_USAGE") == 0;
        }

        private boolean hasPermissions(String... permissions) {
            int callingUid = Binder.getCallingUid();
            if (callingUid == 1000) {
                return true;
            }
            boolean hasPermissions = true;
            Context context = UsageStatsService.this.getContext();
            for (String str : permissions) {
                hasPermissions = hasPermissions && context.checkCallingPermission(str) == 0;
            }
            return hasPermissions;
        }

        private void checkCallerIsSystemOrSameApp(String pkg) {
            if (isCallingUidSystem()) {
                return;
            }
            checkCallerIsSameApp(pkg);
        }

        private void checkCallerIsSameApp(String pkg) {
            int callingUid = Binder.getCallingUid();
            int callingUserId = UserHandle.getUserId(callingUid);
            if (UsageStatsService.this.mPackageManagerInternal.getPackageUid(pkg, 0L, callingUserId) != callingUid) {
                throw new SecurityException("Calling uid " + callingUid + " cannot query eventsfor package " + pkg);
            }
        }

        private boolean isCallingUidSystem() {
            int uid = UserHandle.getAppId(Binder.getCallingUid());
            return uid == 1000;
        }

        public ParceledListSlice<UsageStats> queryUsageStats(int bucketType, long beginTime, long endTime, String callingPackage, int userId) {
            if (hasPermission(callingPackage)) {
                int callingUid = Binder.getCallingUid();
                int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), callingUid, userId, false, true, "queryUsageStats", callingPackage);
                boolean obfuscateInstantApps = UsageStatsService.this.shouldObfuscateInstantAppsForCaller(callingUid, UserHandle.getCallingUserId());
                long token = Binder.clearCallingIdentity();
                try {
                    List<UsageStats> results = UsageStatsService.this.queryUsageStats(userId2, bucketType, beginTime, endTime, obfuscateInstantApps);
                    if (results != null) {
                        return new ParceledListSlice<>(results);
                    }
                    return null;
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
            return null;
        }

        public ParceledListSlice<ConfigurationStats> queryConfigurationStats(int bucketType, long beginTime, long endTime, String callingPackage) throws RemoteException {
            if (hasPermission(callingPackage)) {
                int userId = UserHandle.getCallingUserId();
                long token = Binder.clearCallingIdentity();
                try {
                    List<ConfigurationStats> results = UsageStatsService.this.queryConfigurationStats(userId, bucketType, beginTime, endTime);
                    if (results != null) {
                        return new ParceledListSlice<>(results);
                    }
                    return null;
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
            return null;
        }

        public ParceledListSlice<EventStats> queryEventStats(int bucketType, long beginTime, long endTime, String callingPackage) throws RemoteException {
            if (hasPermission(callingPackage)) {
                int userId = UserHandle.getCallingUserId();
                long token = Binder.clearCallingIdentity();
                try {
                    List<EventStats> results = UsageStatsService.this.queryEventStats(userId, bucketType, beginTime, endTime);
                    if (results != null) {
                        return new ParceledListSlice<>(results);
                    }
                    return null;
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
            return null;
        }

        public UsageEvents queryEvents(long beginTime, long endTime, String callingPackage) {
            if (!hasPermission(callingPackage)) {
                return null;
            }
            int userId = UserHandle.getCallingUserId();
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            boolean obfuscateInstantApps = UsageStatsService.this.shouldObfuscateInstantAppsForCaller(callingUid, userId);
            long token = Binder.clearCallingIdentity();
            try {
                boolean hideShortcutInvocationEvents = UsageStatsService.this.shouldHideShortcutInvocationEvents(userId, callingPackage, callingPid, callingUid);
                boolean hideLocusIdEvents = UsageStatsService.this.shouldHideLocusIdEvents(callingPid, callingUid);
                boolean obfuscateNotificationEvents = UsageStatsService.this.shouldObfuscateNotificationEvents(callingPid, callingUid);
                int flags = obfuscateInstantApps ? 0 | 1 : 0;
                if (hideShortcutInvocationEvents) {
                    flags |= 2;
                }
                if (hideLocusIdEvents) {
                    flags |= 8;
                }
                if (obfuscateNotificationEvents) {
                    flags |= 4;
                }
                return UsageStatsService.this.queryEvents(userId, beginTime, endTime, flags);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public UsageEvents queryEventsForPackage(long beginTime, long endTime, String callingPackage) {
            int callingUid = Binder.getCallingUid();
            int callingUserId = UserHandle.getUserId(callingUid);
            checkCallerIsSameApp(callingPackage);
            boolean includeTaskRoot = hasPermission(callingPackage);
            long token = Binder.clearCallingIdentity();
            try {
                return UsageStatsService.this.queryEventsForPackage(callingUserId, beginTime, endTime, callingPackage, includeTaskRoot);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public UsageEvents queryEventsForUser(long beginTime, long endTime, int userId, String callingPackage) {
            if (!hasPermission(callingPackage)) {
                return null;
            }
            int callingUserId = UserHandle.getCallingUserId();
            if (userId != callingUserId) {
                UsageStatsService.this.getContext().enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "No permission to query usage stats for this user");
            }
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            boolean obfuscateInstantApps = UsageStatsService.this.shouldObfuscateInstantAppsForCaller(callingUid, callingUserId);
            long token = Binder.clearCallingIdentity();
            try {
                boolean hideShortcutInvocationEvents = UsageStatsService.this.shouldHideShortcutInvocationEvents(userId, callingPackage, callingPid, callingUid);
                boolean obfuscateNotificationEvents = UsageStatsService.this.shouldObfuscateNotificationEvents(callingPid, callingUid);
                boolean hideLocusIdEvents = UsageStatsService.this.shouldHideLocusIdEvents(callingPid, callingUid);
                int flags = obfuscateInstantApps ? 0 | 1 : 0;
                if (hideShortcutInvocationEvents) {
                    flags |= 2;
                }
                if (hideLocusIdEvents) {
                    flags |= 8;
                }
                if (obfuscateNotificationEvents) {
                    flags |= 4;
                }
                return UsageStatsService.this.queryEvents(userId, beginTime, endTime, flags);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public UsageEvents queryEventsForPackageForUser(long beginTime, long endTime, int userId, String pkg, String callingPackage) {
            if (!hasPermission(callingPackage)) {
                return null;
            }
            if (userId != UserHandle.getCallingUserId()) {
                UsageStatsService.this.getContext().enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "No permission to query usage stats for this user");
            }
            checkCallerIsSystemOrSameApp(pkg);
            long token = Binder.clearCallingIdentity();
            try {
                return UsageStatsService.this.queryEventsForPackage(userId, beginTime, endTime, pkg, true);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean isAppInactive(String packageName, int userId, String callingPackage) {
            int callingUid = Binder.getCallingUid();
            try {
                int userId2 = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), callingUid, userId, false, false, "isAppInactive", (String) null);
                if (packageName.equals(callingPackage)) {
                    int actualCallingUid = UsageStatsService.this.mPackageManagerInternal.getPackageUid(callingPackage, 0L, userId2);
                    if (actualCallingUid != callingUid) {
                        return false;
                    }
                } else if (!hasPermission(callingPackage)) {
                    return false;
                }
                boolean obfuscateInstantApps = UsageStatsService.this.shouldObfuscateInstantAppsForCaller(callingUid, userId2);
                long token = Binder.clearCallingIdentity();
                try {
                    return UsageStatsService.this.mAppStandby.isAppIdleFiltered(packageName, userId2, SystemClock.elapsedRealtime(), obfuscateInstantApps);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            } catch (RemoteException re) {
                throw re.rethrowFromSystemServer();
            }
        }

        public void setAppInactive(String packageName, boolean idle, int userId) {
            int callingUid = Binder.getCallingUid();
            try {
                int userId2 = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), callingUid, userId, false, true, "setAppInactive", (String) null);
                UsageStatsService.this.getContext().enforceCallingPermission("android.permission.CHANGE_APP_IDLE_STATE", "No permission to change app idle state");
                long token = Binder.clearCallingIdentity();
                try {
                    int appId = UsageStatsService.this.mAppStandby.getAppId(packageName);
                    if (appId < 0) {
                        return;
                    }
                    UsageStatsService.this.mAppStandby.setAppIdleAsync(packageName, idle, userId2);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            } catch (RemoteException re) {
                throw re.rethrowFromSystemServer();
            }
        }

        public int getAppStandbyBucket(String packageName, String callingPackage, int userId) {
            int callingUid = Binder.getCallingUid();
            try {
                int userId2 = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), callingUid, userId, false, false, "getAppStandbyBucket", (String) null);
                int packageUid = UsageStatsService.this.mPackageManagerInternal.getPackageUid(packageName, 0L, userId2);
                if (packageUid != callingUid && !hasPermission(callingPackage)) {
                    throw new SecurityException("Don't have permission to query app standby bucket");
                }
                if (packageUid < 0) {
                    throw new IllegalArgumentException("Cannot get standby bucket for non existent package (" + packageName + ")");
                }
                boolean obfuscateInstantApps = UsageStatsService.this.shouldObfuscateInstantAppsForCaller(callingUid, userId2);
                long token = Binder.clearCallingIdentity();
                try {
                    return UsageStatsService.this.mAppStandby.getAppStandbyBucket(packageName, userId2, SystemClock.elapsedRealtime(), obfuscateInstantApps);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            } catch (RemoteException re) {
                throw re.rethrowFromSystemServer();
            }
        }

        public void setAppStandbyBucket(String packageName, int bucket, int userId) {
            UsageStatsService.this.getContext().enforceCallingPermission("android.permission.CHANGE_APP_IDLE_STATE", "No permission to change app standby state");
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            long token = Binder.clearCallingIdentity();
            try {
                UsageStatsService.this.mAppStandby.setAppStandbyBucket(packageName, bucket, userId, callingUid, callingPid);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public ParceledListSlice<AppStandbyInfo> getAppStandbyBuckets(String callingPackageName, int userId) {
            int callingUid = Binder.getCallingUid();
            try {
                int userId2 = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), callingUid, userId, false, false, "getAppStandbyBucket", (String) null);
                if (!hasPermission(callingPackageName)) {
                    throw new SecurityException("Don't have permission to query app standby bucket");
                }
                long token = Binder.clearCallingIdentity();
                try {
                    List<AppStandbyInfo> standbyBucketList = UsageStatsService.this.mAppStandby.getAppStandbyBuckets(userId2);
                    return standbyBucketList == null ? ParceledListSlice.emptyList() : new ParceledListSlice<>(standbyBucketList);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            } catch (RemoteException re) {
                throw re.rethrowFromSystemServer();
            }
        }

        public void setAppStandbyBuckets(ParceledListSlice appBuckets, int userId) {
            UsageStatsService.this.getContext().enforceCallingPermission("android.permission.CHANGE_APP_IDLE_STATE", "No permission to change app standby state");
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            long token = Binder.clearCallingIdentity();
            try {
                UsageStatsService.this.mAppStandby.setAppStandbyBuckets(appBuckets.getList(), userId, callingUid, callingPid);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public int getAppMinStandbyBucket(String packageName, String callingPackage, int userId) {
            int callingUid = Binder.getCallingUid();
            try {
                int userId2 = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), callingUid, userId, false, false, "getAppStandbyBucket", (String) null);
                int packageUid = UsageStatsService.this.mPackageManagerInternal.getPackageUid(packageName, 0L, userId2);
                if (packageUid != callingUid && !hasPermission(callingPackage)) {
                    throw new SecurityException("Don't have permission to query min app standby bucket");
                }
                if (packageUid < 0) {
                    throw new IllegalArgumentException("Cannot get min standby bucket for non existent package (" + packageName + ")");
                }
                boolean obfuscateInstantApps = UsageStatsService.this.shouldObfuscateInstantAppsForCaller(callingUid, userId2);
                long token = Binder.clearCallingIdentity();
                try {
                    return UsageStatsService.this.mAppStandby.getAppMinStandbyBucket(packageName, UserHandle.getAppId(packageUid), userId2, obfuscateInstantApps);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            } catch (RemoteException re) {
                throw re.rethrowFromSystemServer();
            }
        }

        public void setEstimatedLaunchTime(String packageName, long estimatedLaunchTime, int userId) {
            UsageStatsService.this.getContext().enforceCallingPermission("android.permission.CHANGE_APP_LAUNCH_TIME_ESTIMATE", "No permission to change app launch estimates");
            long token = Binder.clearCallingIdentity();
            try {
                UsageStatsService.this.setEstimatedLaunchTime(userId, packageName, estimatedLaunchTime);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setEstimatedLaunchTimes(ParceledListSlice estimatedLaunchTimes, int userId) {
            UsageStatsService.this.getContext().enforceCallingPermission("android.permission.CHANGE_APP_LAUNCH_TIME_ESTIMATE", "No permission to change app launch estimates");
            long token = Binder.clearCallingIdentity();
            try {
                UsageStatsService.this.setEstimatedLaunchTimes(userId, estimatedLaunchTimes.getList());
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void onCarrierPrivilegedAppsChanged() {
            UsageStatsService.this.getContext().enforceCallingOrSelfPermission("android.permission.BIND_CARRIER_SERVICES", "onCarrierPrivilegedAppsChanged can only be called by privileged apps.");
            UsageStatsService.this.mAppStandby.clearCarrierPrivilegedApps();
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpAndUsageStatsPermission(UsageStatsService.this.getContext(), UsageStatsService.TAG, pw)) {
                UsageStatsService.this.dump(args, pw);
            }
        }

        public void reportChooserSelection(String packageName, int userId, String contentType, String[] annotations, String action) {
            if (packageName == null) {
                Slog.w(UsageStatsService.TAG, "Event report user selecting a null package");
                return;
            }
            UsageEvents.Event event = new UsageEvents.Event(9, SystemClock.elapsedRealtime());
            event.mPackage = packageName;
            event.mAction = action;
            event.mContentType = contentType;
            event.mContentAnnotations = annotations;
            UsageStatsService.this.reportEventOrAddToQueue(userId, event);
        }

        public void reportUserInteraction(String packageName, int userId) {
            Objects.requireNonNull(packageName);
            if (!isCallingUidSystem()) {
                throw new SecurityException("Only system is allowed to call reportUserInteraction");
            }
            UsageEvents.Event event = new UsageEvents.Event(7, SystemClock.elapsedRealtime());
            event.mPackage = packageName;
            UsageStatsService.this.reportEventOrAddToQueue(userId, event);
        }

        public void registerAppUsageObserver(int observerId, String[] packages, long timeLimitMs, PendingIntent callbackIntent, String callingPackage) {
            if (!hasObserverPermission()) {
                throw new SecurityException("Caller doesn't have OBSERVE_APP_USAGE permission");
            }
            if (packages == null || packages.length == 0) {
                throw new IllegalArgumentException("Must specify at least one package");
            }
            if (callbackIntent == null) {
                throw new NullPointerException("callbackIntent can't be null");
            }
            int callingUid = Binder.getCallingUid();
            int userId = UserHandle.getUserId(callingUid);
            long token = Binder.clearCallingIdentity();
            try {
                UsageStatsService.this.registerAppUsageObserver(callingUid, observerId, packages, timeLimitMs, callbackIntent, userId);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void unregisterAppUsageObserver(int observerId, String callingPackage) {
            if (!hasObserverPermission()) {
                throw new SecurityException("Caller doesn't have OBSERVE_APP_USAGE permission");
            }
            int callingUid = Binder.getCallingUid();
            int userId = UserHandle.getUserId(callingUid);
            long token = Binder.clearCallingIdentity();
            try {
                UsageStatsService.this.unregisterAppUsageObserver(callingUid, observerId, userId);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void registerUsageSessionObserver(int sessionObserverId, String[] observed, long timeLimitMs, long sessionThresholdTimeMs, PendingIntent limitReachedCallbackIntent, PendingIntent sessionEndCallbackIntent, String callingPackage) {
            if (!hasObserverPermission()) {
                throw new SecurityException("Caller doesn't have OBSERVE_APP_USAGE permission");
            }
            if (observed == null || observed.length == 0) {
                throw new IllegalArgumentException("Must specify at least one observed entity");
            }
            if (limitReachedCallbackIntent == null) {
                throw new NullPointerException("limitReachedCallbackIntent can't be null");
            }
            int callingUid = Binder.getCallingUid();
            int userId = UserHandle.getUserId(callingUid);
            long token = Binder.clearCallingIdentity();
            try {
                UsageStatsService.this.registerUsageSessionObserver(callingUid, sessionObserverId, observed, timeLimitMs, sessionThresholdTimeMs, limitReachedCallbackIntent, sessionEndCallbackIntent, userId);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void unregisterUsageSessionObserver(int sessionObserverId, String callingPackage) {
            if (!hasObserverPermission()) {
                throw new SecurityException("Caller doesn't have OBSERVE_APP_USAGE permission");
            }
            int callingUid = Binder.getCallingUid();
            int userId = UserHandle.getUserId(callingUid);
            long token = Binder.clearCallingIdentity();
            try {
                UsageStatsService.this.unregisterUsageSessionObserver(callingUid, sessionObserverId, userId);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void registerAppUsageLimitObserver(int observerId, String[] packages, long timeLimitMs, long timeUsedMs, PendingIntent callbackIntent, String callingPackage) {
            int callingUid = Binder.getCallingUid();
            DevicePolicyManagerInternal dpmInternal = UsageStatsService.this.getDpmInternal();
            if (!hasPermissions("android.permission.SUSPEND_APPS", "android.permission.OBSERVE_APP_USAGE") && (dpmInternal == null || !dpmInternal.isActiveSupervisionApp(callingUid))) {
                throw new SecurityException("Caller must be the active supervision app or it must have both SUSPEND_APPS and OBSERVE_APP_USAGE permissions");
            }
            if (packages == null || packages.length == 0) {
                throw new IllegalArgumentException("Must specify at least one package");
            }
            if (callbackIntent == null && timeUsedMs < timeLimitMs) {
                throw new NullPointerException("callbackIntent can't be null");
            }
            int userId = UserHandle.getUserId(callingUid);
            long token = Binder.clearCallingIdentity();
            try {
                UsageStatsService.this.registerAppUsageLimitObserver(callingUid, observerId, packages, timeLimitMs, timeUsedMs, callbackIntent, userId);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void unregisterAppUsageLimitObserver(int observerId, String callingPackage) {
            int callingUid = Binder.getCallingUid();
            DevicePolicyManagerInternal dpmInternal = UsageStatsService.this.getDpmInternal();
            if (!hasPermissions("android.permission.SUSPEND_APPS", "android.permission.OBSERVE_APP_USAGE") && (dpmInternal == null || !dpmInternal.isActiveSupervisionApp(callingUid))) {
                throw new SecurityException("Caller must be the active supervision app or it must have both SUSPEND_APPS and OBSERVE_APP_USAGE permissions");
            }
            int userId = UserHandle.getUserId(callingUid);
            long token = Binder.clearCallingIdentity();
            try {
                UsageStatsService.this.unregisterAppUsageLimitObserver(callingUid, observerId, userId);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void reportUsageStart(IBinder activity, String token, String callingPackage) {
            reportPastUsageStart(activity, token, 0L, callingPackage);
        }

        public void reportPastUsageStart(IBinder activity, String token, long timeAgoMs, String callingPackage) {
            ArraySet<String> tokens;
            int callingUid = Binder.getCallingUid();
            int userId = UserHandle.getUserId(callingUid);
            long binderToken = Binder.clearCallingIdentity();
            try {
                synchronized (UsageStatsService.this.mUsageReporters) {
                    tokens = UsageStatsService.this.mUsageReporters.get(activity.hashCode());
                    if (tokens == null) {
                        tokens = new ArraySet<>();
                        UsageStatsService.this.mUsageReporters.put(activity.hashCode(), tokens);
                    }
                }
                synchronized (tokens) {
                    if (!tokens.add(token)) {
                        throw new IllegalArgumentException(token + " for " + callingPackage + " is already reported as started for this activity");
                    }
                }
                UsageStatsService.this.mAppTimeLimit.noteUsageStart(UsageStatsService.this.buildFullToken(callingPackage, token), userId, timeAgoMs);
            } finally {
                Binder.restoreCallingIdentity(binderToken);
            }
        }

        public void reportUsageStop(IBinder activity, String token, String callingPackage) {
            ArraySet<String> tokens;
            int callingUid = Binder.getCallingUid();
            int userId = UserHandle.getUserId(callingUid);
            long binderToken = Binder.clearCallingIdentity();
            try {
                synchronized (UsageStatsService.this.mUsageReporters) {
                    tokens = UsageStatsService.this.mUsageReporters.get(activity.hashCode());
                    if (tokens == null) {
                        throw new IllegalArgumentException("Unknown reporter trying to stop token " + token + " for " + callingPackage);
                    }
                }
                synchronized (tokens) {
                    if (!tokens.remove(token)) {
                        throw new IllegalArgumentException(token + " for " + callingPackage + " is already reported as stopped for this activity");
                    }
                }
                UsageStatsService.this.mAppTimeLimit.noteUsageStop(UsageStatsService.this.buildFullToken(callingPackage, token), userId);
            } finally {
                Binder.restoreCallingIdentity(binderToken);
            }
        }

        public int getUsageSource() {
            int i;
            if (!hasObserverPermission()) {
                throw new SecurityException("Caller doesn't have OBSERVE_APP_USAGE permission");
            }
            synchronized (UsageStatsService.this.mLock) {
                i = UsageStatsService.this.mUsageSource;
            }
            return i;
        }

        public void forceUsageSourceSettingRead() {
            UsageStatsService.this.readUsageSourceSetting();
        }

        public long getLastTimeAnyComponentUsed(String packageName, String callingPackage) {
            long longValue;
            if (!hasPermissions("android.permission.INTERACT_ACROSS_USERS")) {
                throw new SecurityException("Caller doesn't have INTERACT_ACROSS_USERS permission");
            }
            if (!hasPermission(callingPackage)) {
                throw new SecurityException("Don't have permission to query usage stats");
            }
            synchronized (UsageStatsService.this.mLock) {
                longValue = (((Long) UsageStatsService.this.mLastTimeComponentUsedGlobal.getOrDefault(packageName, 0L)).longValue() / TimeUnit.DAYS.toMillis(1L)) * TimeUnit.DAYS.toMillis(1L);
            }
            return longValue;
        }

        public BroadcastResponseStatsList queryBroadcastResponseStats(String packageName, long id, String callingPackage, int userId) {
            Objects.requireNonNull(callingPackage);
            if (id < 0) {
                throw new IllegalArgumentException("id needs to be >=0");
            }
            UsageStatsService.this.getContext().enforceCallingOrSelfPermission("android.permission.ACCESS_BROADCAST_RESPONSE_STATS", "queryBroadcastResponseStats");
            int callingUid = Binder.getCallingUid();
            return new BroadcastResponseStatsList(UsageStatsService.this.mResponseStatsTracker.queryBroadcastResponseStats(callingUid, packageName, id, ActivityManager.handleIncomingUser(Binder.getCallingPid(), callingUid, userId, false, false, "queryBroadcastResponseStats", callingPackage)));
        }

        public void clearBroadcastResponseStats(String packageName, long id, String callingPackage, int userId) {
            Objects.requireNonNull(callingPackage);
            if (id < 0) {
                throw new IllegalArgumentException("id needs to be >=0");
            }
            UsageStatsService.this.getContext().enforceCallingOrSelfPermission("android.permission.ACCESS_BROADCAST_RESPONSE_STATS", "clearBroadcastResponseStats");
            int callingUid = Binder.getCallingUid();
            UsageStatsService.this.mResponseStatsTracker.clearBroadcastResponseStats(callingUid, packageName, id, ActivityManager.handleIncomingUser(Binder.getCallingPid(), callingUid, userId, false, false, "clearBroadcastResponseStats", callingPackage));
        }

        public void clearBroadcastEvents(String callingPackage, int userId) {
            Objects.requireNonNull(callingPackage);
            UsageStatsService.this.getContext().enforceCallingOrSelfPermission("android.permission.ACCESS_BROADCAST_RESPONSE_STATS", "clearBroadcastEvents");
            int callingUid = Binder.getCallingUid();
            UsageStatsService.this.mResponseStatsTracker.clearBroadcastEvents(callingUid, ActivityManager.handleIncomingUser(Binder.getCallingPid(), callingUid, userId, false, false, "clearBroadcastResponseStats", callingPackage));
        }

        public String getAppStandbyConstant(String key) {
            Objects.requireNonNull(key);
            if (!hasPermissions("android.permission.READ_DEVICE_CONFIG")) {
                throw new SecurityException("Caller doesn't have READ_DEVICE_CONFIG permission");
            }
            return UsageStatsService.this.mAppStandby.getAppStandbyConstant(key);
        }

        /* JADX DEBUG: Multi-variable search result rejected for r6v0, resolved type: com.android.server.usage.UsageStatsService$BinderService */
        /* JADX WARN: Multi-variable type inference failed */
        public int handleShellCommand(ParcelFileDescriptor in, ParcelFileDescriptor out, ParcelFileDescriptor err, String[] args) {
            return new UsageStatsShellCommand(UsageStatsService.this).exec(this, in.getFileDescriptor(), out.getFileDescriptor(), err.getFileDescriptor(), args);
        }
    }

    void registerAppUsageObserver(int callingUid, int observerId, String[] packages, long timeLimitMs, PendingIntent callbackIntent, int userId) {
        this.mAppTimeLimit.addAppUsageObserver(callingUid, observerId, packages, timeLimitMs, callbackIntent, userId);
    }

    void unregisterAppUsageObserver(int callingUid, int observerId, int userId) {
        this.mAppTimeLimit.removeAppUsageObserver(callingUid, observerId, userId);
    }

    void registerUsageSessionObserver(int callingUid, int observerId, String[] observed, long timeLimitMs, long sessionThresholdTime, PendingIntent limitReachedCallbackIntent, PendingIntent sessionEndCallbackIntent, int userId) {
        this.mAppTimeLimit.addUsageSessionObserver(callingUid, observerId, observed, timeLimitMs, sessionThresholdTime, limitReachedCallbackIntent, sessionEndCallbackIntent, userId);
    }

    void unregisterUsageSessionObserver(int callingUid, int sessionObserverId, int userId) {
        this.mAppTimeLimit.removeUsageSessionObserver(callingUid, sessionObserverId, userId);
    }

    void registerAppUsageLimitObserver(int callingUid, int observerId, String[] packages, long timeLimitMs, long timeUsedMs, PendingIntent callbackIntent, int userId) {
        this.mAppTimeLimit.addAppUsageLimitObserver(callingUid, observerId, packages, timeLimitMs, timeUsedMs, callbackIntent, userId);
    }

    void unregisterAppUsageLimitObserver(int callingUid, int observerId, int userId) {
        this.mAppTimeLimit.removeAppUsageLimitObserver(callingUid, observerId, userId);
    }

    /* loaded from: classes2.dex */
    private final class LocalService extends UsageStatsManagerInternal {
        private LocalService() {
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void reportEvent(ComponentName component, int userId, int eventType, int instanceId, ComponentName taskRoot) {
            if (component == null) {
                Slog.w(UsageStatsService.TAG, "Event reported without a component name");
                return;
            }
            ITranUsageStatsService.Instance().onReportEvent(component, userId, eventType, instanceId, taskRoot);
            UsageEvents.Event event = new UsageEvents.Event(eventType, SystemClock.elapsedRealtime());
            event.mPackage = component.getPackageName();
            event.mClass = component.getClassName();
            event.mInstanceId = instanceId;
            if (taskRoot == null) {
                event.mTaskRootPackage = null;
                event.mTaskRootClass = null;
            } else {
                event.mTaskRootPackage = taskRoot.getPackageName();
                event.mTaskRootClass = taskRoot.getClassName();
            }
            UsageStatsService.this.reportEventOrAddToQueue(userId, event);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void reportEvent(String packageName, int userId, int eventType) {
            if (packageName == null) {
                Slog.w(UsageStatsService.TAG, "Event reported without a package name, eventType:" + eventType);
                return;
            }
            UsageEvents.Event event = new UsageEvents.Event(eventType, SystemClock.elapsedRealtime());
            event.mPackage = packageName;
            UsageStatsService.this.reportEventOrAddToQueue(userId, event);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void reportConfigurationChange(Configuration config, int userId) {
            if (config == null) {
                Slog.w(UsageStatsService.TAG, "Configuration event reported with a null config");
                return;
            }
            UsageEvents.Event event = new UsageEvents.Event(5, SystemClock.elapsedRealtime());
            event.mPackage = PackageManagerService.PLATFORM_PACKAGE_NAME;
            event.mConfiguration = new Configuration(config);
            UsageStatsService.this.reportEventOrAddToQueue(userId, event);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void reportInterruptiveNotification(String packageName, String channelId, int userId) {
            if (packageName == null || channelId == null) {
                Slog.w(UsageStatsService.TAG, "Event reported without a package name or a channel ID");
                return;
            }
            UsageEvents.Event event = new UsageEvents.Event(12, SystemClock.elapsedRealtime());
            event.mPackage = packageName.intern();
            event.mNotificationChannelId = channelId.intern();
            UsageStatsService.this.reportEventOrAddToQueue(userId, event);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void reportShortcutUsage(String packageName, String shortcutId, int userId) {
            if (packageName == null || shortcutId == null) {
                Slog.w(UsageStatsService.TAG, "Event reported without a package name or a shortcut ID");
                return;
            }
            UsageEvents.Event event = new UsageEvents.Event(8, SystemClock.elapsedRealtime());
            event.mPackage = packageName.intern();
            event.mShortcutId = shortcutId.intern();
            UsageStatsService.this.reportEventOrAddToQueue(userId, event);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void reportLocusUpdate(ComponentName activity, int userId, LocusId locusId, IBinder appToken) {
            if (locusId == null) {
                return;
            }
            UsageEvents.Event event = new UsageEvents.Event(30, SystemClock.elapsedRealtime());
            event.mLocusId = locusId.getId();
            event.mPackage = activity.getPackageName();
            event.mClass = activity.getClassName();
            event.mInstanceId = appToken.hashCode();
            UsageStatsService.this.reportEventOrAddToQueue(userId, event);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void reportContentProviderUsage(String name, String packageName, int userId) {
            UsageStatsService.this.mAppStandby.postReportContentProviderUsage(name, packageName, userId);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public boolean isAppIdle(String packageName, int uidForAppId, int userId) {
            return UsageStatsService.this.mAppStandby.isAppIdleFiltered(packageName, uidForAppId, userId, SystemClock.elapsedRealtime());
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public int getAppStandbyBucket(String packageName, int userId, long nowElapsed) {
            return UsageStatsService.this.mAppStandby.getAppStandbyBucket(packageName, userId, nowElapsed, false);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public int[] getIdleUidsForUser(int userId) {
            return UsageStatsService.this.mAppStandby.getIdleUidsForUser(userId);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void prepareShutdown() {
            UsageStatsService.this.shutdown();
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void prepareForPossibleShutdown() {
            UsageStatsService.this.prepareForPossibleShutdown();
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public byte[] getBackupPayload(int user, String key) {
            if (!UsageStatsService.this.mUserUnlockedStates.contains(Integer.valueOf(user))) {
                Slog.w(UsageStatsService.TAG, "Failed to get backup payload for locked user " + user);
                return null;
            }
            synchronized (UsageStatsService.this.mLock) {
                if (user == 0) {
                    UserUsageStatsService userStats = UsageStatsService.this.getUserUsageStatsServiceLocked(user);
                    if (userStats == null) {
                        return null;
                    }
                    return userStats.getBackupPayload(key);
                }
                return null;
            }
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void applyRestoredPayload(int user, String key, byte[] payload) {
            synchronized (UsageStatsService.this.mLock) {
                if (!UsageStatsService.this.mUserUnlockedStates.contains(Integer.valueOf(user))) {
                    Slog.w(UsageStatsService.TAG, "Failed to apply restored payload for locked user " + user);
                    return;
                }
                if (user == 0) {
                    UserUsageStatsService userStats = UsageStatsService.this.getUserUsageStatsServiceLocked(user);
                    if (userStats == null) {
                        return;
                    }
                    userStats.applyRestoredPayload(key, payload);
                }
            }
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public List<UsageStats> queryUsageStatsForUser(int userId, int intervalType, long beginTime, long endTime, boolean obfuscateInstantApps) {
            return UsageStatsService.this.queryUsageStats(userId, intervalType, beginTime, endTime, obfuscateInstantApps);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public UsageEvents queryEventsForUser(int userId, long beginTime, long endTime, int flags) {
            return UsageStatsService.this.queryEvents(userId, beginTime, endTime, flags);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void setLastJobRunTime(String packageName, int userId, long elapsedRealtime) {
            UsageStatsService.this.mAppStandby.setLastJobRunTime(packageName, userId, elapsedRealtime);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public long getEstimatedPackageLaunchTime(String packageName, int userId) {
            return UsageStatsService.this.getEstimatedPackageLaunchTime(userId, packageName);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public long getTimeSinceLastJobRun(String packageName, int userId) {
            return UsageStatsService.this.mAppStandby.getTimeSinceLastJobRun(packageName, userId);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void reportAppJobState(String packageName, int userId, int numDeferredJobs, long timeSinceLastJobRun) {
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void onActiveAdminAdded(String packageName, int userId) {
            UsageStatsService.this.mAppStandby.addActiveDeviceAdmin(packageName, userId);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void setActiveAdminApps(Set<String> packageNames, int userId) {
            UsageStatsService.this.mAppStandby.setActiveAdminApps(packageNames, userId);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void setAdminProtectedPackages(Set<String> packageNames, int userId) {
            UsageStatsService.this.mAppStandby.setAdminProtectedPackages(packageNames, userId);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void onAdminDataAvailable() {
            UsageStatsService.this.mAppStandby.onAdminDataAvailable();
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void reportSyncScheduled(String packageName, int userId, boolean exempted) {
            UsageStatsService.this.mAppStandby.postReportSyncScheduled(packageName, userId, exempted);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void reportExemptedSyncStart(String packageName, int userId) {
            UsageStatsService.this.mAppStandby.postReportExemptedSyncStart(packageName, userId);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public UsageStatsManagerInternal.AppUsageLimitData getAppUsageLimit(String packageName, UserHandle user) {
            return UsageStatsService.this.mAppTimeLimit.getAppUsageLimit(packageName, user);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public boolean pruneUninstalledPackagesData(int userId) {
            return UsageStatsService.this.pruneUninstalledPackagesData(userId);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public boolean updatePackageMappingsData() {
            return UsageStatsService.this.updatePackageMappingsData();
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void registerListener(UsageStatsManagerInternal.UsageEventListener listener) {
            UsageStatsService.this.registerListener(listener);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void unregisterListener(UsageStatsManagerInternal.UsageEventListener listener) {
            UsageStatsService.this.unregisterListener(listener);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void registerLaunchTimeChangedListener(UsageStatsManagerInternal.EstimatedLaunchTimeChangedListener listener) {
            UsageStatsService.this.registerLaunchTimeChangedListener(listener);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void unregisterLaunchTimeChangedListener(UsageStatsManagerInternal.EstimatedLaunchTimeChangedListener listener) {
            UsageStatsService.this.unregisterLaunchTimeChangedListener(listener);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void reportBroadcastDispatched(int sourceUid, String targetPackage, UserHandle targetUser, long idForResponseEvent, long timestampMs, int targetUidProcState) {
            UsageStatsService.this.mResponseStatsTracker.reportBroadcastDispatchEvent(sourceUid, targetPackage, targetUser, idForResponseEvent, timestampMs, targetUidProcState);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void reportNotificationPosted(String packageName, UserHandle user, long timestampMs) {
            UsageStatsService.this.mResponseStatsTracker.reportNotificationPosted(packageName, user, timestampMs);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void reportNotificationUpdated(String packageName, UserHandle user, long timestampMs) {
            UsageStatsService.this.mResponseStatsTracker.reportNotificationUpdated(packageName, user, timestampMs);
        }

        @Override // android.app.usage.UsageStatsManagerInternal
        public void reportNotificationRemoved(String packageName, UserHandle user, long timestampMs) {
            UsageStatsService.this.mResponseStatsTracker.reportNotificationCancelled(packageName, user, timestampMs);
        }
    }

    /* loaded from: classes2.dex */
    private class MyPackageMonitor extends PackageMonitor {
        private MyPackageMonitor() {
        }

        public void onPackageRemoved(String packageName, int uid) {
            int changingUserId = getChangingUserId();
            if (UsageStatsService.this.shouldDeleteObsoleteData(UserHandle.of(changingUserId))) {
                UsageStatsService.this.mHandler.obtainMessage(6, changingUserId, 0, packageName).sendToTarget();
            }
            UsageStatsService.this.mResponseStatsTracker.onPackageRemoved(packageName, UserHandle.getUserId(uid));
            super.onPackageRemoved(packageName, uid);
        }
    }
}
