package com.android.server.content;

import android.accounts.Account;
import android.accounts.AccountAndUser;
import android.accounts.AccountManager;
import android.accounts.AccountManagerInternal;
import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ISyncAdapter;
import android.content.ISyncAdapterUnsyncableAccountCallback;
import android.content.ISyncContext;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.PeriodicSync;
import android.content.ServiceConnection;
import android.content.SyncActivityTooManyDeletes;
import android.content.SyncAdapterType;
import android.content.SyncAdaptersCache;
import android.content.SyncInfo;
import android.content.SyncResult;
import android.content.SyncStatusInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ProviderInfo;
import android.content.pm.RegisteredServicesCache;
import android.content.pm.RegisteredServicesCacheListener;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.hardware.usb.gadget.V1_2.GadgetFunction;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.TimeMigrationUtils;
import android.util.EventLog;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseBooleanArray;
import com.android.internal.app.IBatteryStats;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.function.QuadConsumer;
import com.android.server.DeviceIdleInternal;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.UiModeManagerService;
import com.android.server.accounts.AccountManagerService;
import com.android.server.backup.AccountSyncSettingsBackupHelper;
import com.android.server.content.SyncManager;
import com.android.server.content.SyncStorageEngine;
import com.android.server.job.JobSchedulerInternal;
import com.android.server.slice.SliceClientPermissions;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import com.transsion.hubcore.server.content.ITranSyncManager;
import dalvik.annotation.optimization.NeverCompile;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public class SyncManager {
    private static final boolean DEBUG_ACCOUNT_ACCESS = false;
    private static final int DELAY_RETRY_SYNC_IN_PROGRESS_IN_SECONDS = 10;
    private static final String HANDLE_SYNC_ALARM_WAKE_LOCK = "SyncManagerHandleSyncAlarm";
    private static final int MAX_SYNC_JOB_ID = 110000;
    private static final int MIN_SYNC_JOB_ID = 100000;
    private static final int SYNC_ADAPTER_CONNECTION_FLAGS = 21;
    private static final long SYNC_DELAY_ON_CONFLICT = 10000;
    private static final String SYNC_LOOP_WAKE_LOCK = "SyncLoopWakeLock";
    private static final int SYNC_MONITOR_PROGRESS_THRESHOLD_BYTES = 10;
    private static final long SYNC_MONITOR_WINDOW_LENGTH_MILLIS = 60000;
    private static final int SYNC_OP_STATE_INVALID_NOT_SYNCABLE = 4;
    private static final int SYNC_OP_STATE_INVALID_NO_ACCOUNT = 3;
    private static final int SYNC_OP_STATE_INVALID_NO_ACCOUNT_ACCESS = 2;
    private static final int SYNC_OP_STATE_INVALID_SYNC_DISABLED = 5;
    private static final int SYNC_OP_STATE_VALID = 0;
    private static final String SYNC_WAKE_LOCK_PREFIX = "*sync*/";
    static final String TAG = "SyncManager";
    private static final boolean USE_WTF_FOR_ACCOUNT_ERROR = true;
    private static SyncManager sInstance;
    private final AccountManager mAccountManager;
    private final AccountManagerInternal mAccountManagerInternal;
    private final BroadcastReceiver mAccountsUpdatedReceiver;
    private final ActivityManagerInternal mAmi;
    private final IBatteryStats mBatteryStats;
    private ConnectivityManager mConnManagerDoNotUseDirectly;
    private BroadcastReceiver mConnectivityIntentReceiver;
    private final SyncManagerConstants mConstants;
    private Context mContext;
    private JobScheduler mJobScheduler;
    private JobSchedulerInternal mJobSchedulerInternal;
    private final SyncLogger mLogger;
    private final NotificationManager mNotificationMgr;
    private final BroadcastReceiver mOtherIntentsReceiver;
    private final PackageManagerInternal mPackageManagerInternal;
    private final PowerManager mPowerManager;
    private volatile boolean mProvisioned;
    private BroadcastReceiver mShutdownIntentReceiver;
    protected final SyncAdaptersCache mSyncAdapters;
    private final SyncHandler mSyncHandler;
    private volatile PowerManager.WakeLock mSyncManagerWakeLock;
    private SyncStorageEngine mSyncStorageEngine;
    private final HandlerThread mThread;
    private final SparseBooleanArray mUnlockedUsers;
    private BroadcastReceiver mUserIntentReceiver;
    private final UserManager mUserManager;
    private static final boolean ENABLE_SUSPICIOUS_CHECK = Build.IS_DEBUGGABLE;
    private static final long LOCAL_SYNC_DELAY = SystemProperties.getLong("sync.local_sync_delay", 30000);
    private static final AccountAndUser[] INITIAL_ACCOUNTS_ARRAY = new AccountAndUser[0];
    private static final Comparator<SyncOperation> sOpDumpComparator = new Comparator() { // from class: com.android.server.content.SyncManager$$ExternalSyntheticLambda1
        @Override // java.util.Comparator
        public final int compare(Object obj, Object obj2) {
            return SyncManager.lambda$static$6((SyncOperation) obj, (SyncOperation) obj2);
        }
    };
    private static final Comparator<SyncOperation> sOpRuntimeComparator = new Comparator() { // from class: com.android.server.content.SyncManager$$ExternalSyntheticLambda2
        @Override // java.util.Comparator
        public final int compare(Object obj, Object obj2) {
            return SyncManager.lambda$static$7((SyncOperation) obj, (SyncOperation) obj2);
        }
    };
    private final Object mAccountsLock = new Object();
    private volatile AccountAndUser[] mRunningAccounts = INITIAL_ACCOUNTS_ARRAY;
    private volatile boolean mDataConnectionIsConnected = false;
    private volatile int mNextJobIdOffset = 0;
    protected final ArrayList<ActiveSyncContext> mActiveSyncContexts = Lists.newArrayList();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface OnReadyCallback {
        void onReady();
    }

    private boolean isJobIdInUseLockedH(int jobId, List<JobInfo> pendingJobs) {
        int size = pendingJobs.size();
        for (int i = 0; i < size; i++) {
            JobInfo job = pendingJobs.get(i);
            if (job.getId() == jobId) {
                return true;
            }
        }
        int size2 = this.mActiveSyncContexts.size();
        for (int i2 = 0; i2 < size2; i2++) {
            ActiveSyncContext asc = this.mActiveSyncContexts.get(i2);
            if (asc.mSyncOperation.jobId == jobId) {
                return true;
            }
        }
        return false;
    }

    private int getUnusedJobIdH() {
        List<JobInfo> pendingJobs = this.mJobSchedulerInternal.getSystemScheduledPendingJobs();
        for (int i = 0; i < 10000; i++) {
            int newJobId = ((this.mNextJobIdOffset + i) % 10000) + MIN_SYNC_JOB_ID;
            if (!isJobIdInUseLockedH(newJobId, pendingJobs)) {
                this.mNextJobIdOffset = ((this.mNextJobIdOffset + i) + 1) % 10000;
                return newJobId;
            }
        }
        Slog.wtf("SyncManager", "All 10000 possible sync job IDs are taken :/");
        this.mNextJobIdOffset = (this.mNextJobIdOffset + 1) % 10000;
        return this.mNextJobIdOffset + MIN_SYNC_JOB_ID;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<SyncOperation> getAllPendingSyncs() {
        verifyJobScheduler();
        List<JobInfo> pendingJobs = this.mJobSchedulerInternal.getSystemScheduledPendingJobs();
        int numJobs = pendingJobs.size();
        List<SyncOperation> pendingSyncs = new ArrayList<>(numJobs);
        for (int i = 0; i < numJobs; i++) {
            JobInfo job = pendingJobs.get(i);
            SyncOperation op = SyncOperation.maybeCreateFromJobExtras(job.getExtras());
            if (op != null) {
                pendingSyncs.add(op);
            }
        }
        return pendingSyncs;
    }

    private List<UserInfo> getAllUsers() {
        return this.mUserManager.getUsers();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean containsAccountAndUser(AccountAndUser[] accounts, Account account, int userId) {
        for (int i = 0; i < accounts.length; i++) {
            if (accounts[i].userId == userId && accounts[i].account.equals(account)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateRunningAccounts(SyncStorageEngine.EndPoint target) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "sending MESSAGE_ACCOUNTS_UPDATED");
        }
        Message m = this.mSyncHandler.obtainMessage(9);
        m.obj = target;
        m.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeStaleAccounts() {
        for (UserInfo user : this.mUserManager.getAliveUsers()) {
            if (!user.partial) {
                Account[] accountsForUser = AccountManagerService.getSingleton().getAccounts(user.id, this.mContext.getOpPackageName());
                this.mSyncStorageEngine.removeStaleAccounts(accountsForUser, user.id);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearAllBackoffs(String why) {
        this.mSyncStorageEngine.clearAllBackoffsLocked();
        rescheduleSyncs(SyncStorageEngine.EndPoint.USER_ALL_PROVIDER_ALL_ACCOUNTS_ALL, why);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean readDataConnectionState() {
        NetworkInfo networkInfo = getConnectivityManager().getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getJobStats() {
        JobSchedulerInternal js = (JobSchedulerInternal) LocalServices.getService(JobSchedulerInternal.class);
        return "JobStats: " + (js == null ? "(JobSchedulerInternal==null)" : js.getPersistStats().toString());
    }

    private ConnectivityManager getConnectivityManager() {
        ConnectivityManager connectivityManager;
        synchronized (this) {
            if (this.mConnManagerDoNotUseDirectly == null) {
                this.mConnManagerDoNotUseDirectly = (ConnectivityManager) this.mContext.getSystemService("connectivity");
            }
            connectivityManager = this.mConnManagerDoNotUseDirectly;
        }
        return connectivityManager;
    }

    private void cleanupJobs() {
        this.mSyncHandler.postAtFrontOfQueue(new Runnable() { // from class: com.android.server.content.SyncManager.6
            @Override // java.lang.Runnable
            public void run() {
                List<SyncOperation> ops = SyncManager.this.getAllPendingSyncs();
                Set<String> cleanedKeys = new HashSet<>();
                for (SyncOperation opx : ops) {
                    if (!cleanedKeys.contains(opx.key)) {
                        cleanedKeys.add(opx.key);
                        for (SyncOperation opy : ops) {
                            if (opx != opy && opx.key.equals(opy.key)) {
                                SyncManager.this.mLogger.log("Removing duplicate sync: ", opy);
                                SyncManager.this.cancelJob(opy, "cleanupJobs() x=" + opx + " y=" + opy);
                            }
                        }
                    }
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void verifyJobScheduler() {
        if (this.mJobScheduler != null) {
            return;
        }
        long token = Binder.clearCallingIdentity();
        try {
            if (Log.isLoggable("SyncManager", 2)) {
                try {
                    Log.d("SyncManager", "initializing JobScheduler object.");
                } catch (Throwable th) {
                    th = th;
                    Binder.restoreCallingIdentity(token);
                    throw th;
                }
            }
            this.mJobScheduler = (JobScheduler) this.mContext.getSystemService("jobscheduler");
            this.mJobSchedulerInternal = (JobSchedulerInternal) LocalServices.getService(JobSchedulerInternal.class);
            List<JobInfo> pendingJobs = this.mJobScheduler.getAllPendingJobs();
            int numPersistedPeriodicSyncs = 0;
            int numPersistedOneshotSyncs = 0;
            for (JobInfo job : pendingJobs) {
                SyncOperation op = SyncOperation.maybeCreateFromJobExtras(job.getExtras());
                if (op != null) {
                    if (op.isPeriodic) {
                        numPersistedPeriodicSyncs++;
                    } else {
                        numPersistedOneshotSyncs++;
                        this.mSyncStorageEngine.markPending(op.target, true);
                    }
                }
            }
            String summary = "Loaded persisted syncs: " + numPersistedPeriodicSyncs + " periodic syncs, " + numPersistedOneshotSyncs + " oneshot syncs, " + pendingJobs.size() + " total system server jobs, " + getJobStats();
            Slog.i("SyncManager", summary);
            this.mLogger.log(summary);
            cleanupJobs();
            if (ENABLE_SUSPICIOUS_CHECK && numPersistedPeriodicSyncs == 0 && likelyHasPeriodicSyncs()) {
                Slog.wtf("SyncManager", "Device booted with no persisted periodic syncs: " + summary);
            }
            Binder.restoreCallingIdentity(token);
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private boolean likelyHasPeriodicSyncs() {
        try {
            return this.mSyncStorageEngine.getAuthorityCount() >= 6;
        } catch (Throwable th) {
            return false;
        }
    }

    private JobScheduler getJobScheduler() {
        verifyJobScheduler();
        return this.mJobScheduler;
    }

    public SyncManager(Context context, boolean factoryTest) {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.content.SyncManager.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                SyncStorageEngine.EndPoint target = new SyncStorageEngine.EndPoint(null, null, getSendingUserId());
                SyncManager.this.updateRunningAccounts(target);
            }
        };
        this.mAccountsUpdatedReceiver = broadcastReceiver;
        this.mConnectivityIntentReceiver = new BroadcastReceiver() { // from class: com.android.server.content.SyncManager.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                boolean wasConnected = SyncManager.this.mDataConnectionIsConnected;
                SyncManager syncManager = SyncManager.this;
                syncManager.mDataConnectionIsConnected = syncManager.readDataConnectionState();
                if (SyncManager.this.mDataConnectionIsConnected && !wasConnected) {
                    if (Log.isLoggable("SyncManager", 2)) {
                        Slog.v("SyncManager", "Reconnection detected: clearing all backoffs");
                    }
                    SyncManager.this.clearAllBackoffs("network reconnect");
                }
            }
        };
        this.mShutdownIntentReceiver = new BroadcastReceiver() { // from class: com.android.server.content.SyncManager.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                Log.w("SyncManager", "Writing sync state before shutdown...");
                SyncManager.this.getSyncStorageEngine().writeAllState();
                SyncManager.this.mLogger.log(SyncManager.this.getJobStats());
                SyncManager.this.mLogger.log("Shutting down.");
            }
        };
        BroadcastReceiver broadcastReceiver2 = new BroadcastReceiver() { // from class: com.android.server.content.SyncManager.4
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if ("android.intent.action.TIME_SET".equals(intent.getAction())) {
                    SyncManager.this.mSyncStorageEngine.setClockValid();
                }
            }
        };
        this.mOtherIntentsReceiver = broadcastReceiver2;
        this.mUserIntentReceiver = new BroadcastReceiver() { // from class: com.android.server.content.SyncManager.5
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                if (userId == -10000) {
                    return;
                }
                if ("android.intent.action.USER_REMOVED".equals(action)) {
                    SyncManager.this.onUserRemoved(userId);
                } else if ("android.intent.action.USER_UNLOCKED".equals(action)) {
                    SyncManager.this.onUserUnlocked(userId);
                } else if ("android.intent.action.USER_STOPPED".equals(action)) {
                    SyncManager.this.onUserStopped(userId);
                }
            }
        };
        this.mUnlockedUsers = new SparseBooleanArray();
        synchronized (SyncManager.class) {
            if (sInstance == null) {
                sInstance = this;
            } else {
                Slog.wtf("SyncManager", "SyncManager instantiated multiple times");
            }
        }
        this.mContext = context;
        SyncLogger syncLogger = SyncLogger.getInstance();
        this.mLogger = syncLogger;
        SyncStorageEngine.init(context, BackgroundThread.get().getLooper());
        SyncStorageEngine singleton = SyncStorageEngine.getSingleton();
        this.mSyncStorageEngine = singleton;
        singleton.setOnSyncRequestListener(new SyncStorageEngine.OnSyncRequestListener() { // from class: com.android.server.content.SyncManager.7
            @Override // com.android.server.content.SyncStorageEngine.OnSyncRequestListener
            public void onSyncRequest(SyncStorageEngine.EndPoint info, int reason, Bundle extras, int syncExemptionFlag, int callingUid, int callingPid) {
                SyncManager.this.scheduleSync(info.account, info.userId, reason, info.provider, extras, -2, syncExemptionFlag, callingUid, callingPid, null);
            }
        });
        this.mSyncStorageEngine.setPeriodicSyncAddedListener(new SyncStorageEngine.PeriodicSyncAddedListener() { // from class: com.android.server.content.SyncManager.8
            @Override // com.android.server.content.SyncStorageEngine.PeriodicSyncAddedListener
            public void onPeriodicSyncAdded(SyncStorageEngine.EndPoint target, Bundle extras, long pollFrequency, long flex) {
                SyncManager.this.updateOrAddPeriodicSync(target, pollFrequency, flex, extras);
            }
        });
        this.mSyncStorageEngine.setOnAuthorityRemovedListener(new SyncStorageEngine.OnAuthorityRemovedListener() { // from class: com.android.server.content.SyncManager.9
            @Override // com.android.server.content.SyncStorageEngine.OnAuthorityRemovedListener
            public void onAuthorityRemoved(SyncStorageEngine.EndPoint removedAuthority) {
                SyncManager.this.removeSyncsForAuthority(removedAuthority, "onAuthorityRemoved");
            }
        });
        SyncAdaptersCache syncAdaptersCache = new SyncAdaptersCache(this.mContext);
        this.mSyncAdapters = syncAdaptersCache;
        HandlerThread handlerThread = new HandlerThread("SyncManager", 10);
        this.mThread = handlerThread;
        handlerThread.start();
        SyncHandler syncHandler = new SyncHandler(handlerThread.getLooper());
        this.mSyncHandler = syncHandler;
        syncAdaptersCache.setListener(new RegisteredServicesCacheListener<SyncAdapterType>() { // from class: com.android.server.content.SyncManager.10
            /* JADX DEBUG: Method merged with bridge method */
            public void onServiceChanged(SyncAdapterType type, int userId, boolean removed) {
                if (!removed) {
                    SyncManager.this.scheduleSync(null, -1, -3, type.authority, null, -2, 0, Process.myUid(), -1, null);
                }
            }
        }, syncHandler);
        this.mConstants = new SyncManagerConstants(context);
        context.registerReceiver(this.mConnectivityIntentReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        IntentFilter intentFilter = new IntentFilter("android.intent.action.ACTION_SHUTDOWN");
        intentFilter.setPriority(100);
        context.registerReceiver(this.mShutdownIntentReceiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.USER_REMOVED");
        intentFilter2.addAction("android.intent.action.USER_UNLOCKED");
        intentFilter2.addAction("android.intent.action.USER_STOPPED");
        this.mContext.registerReceiverAsUser(this.mUserIntentReceiver, UserHandle.ALL, intentFilter2, null, null);
        context.registerReceiver(broadcastReceiver2, new IntentFilter("android.intent.action.TIME_SET"));
        if (!factoryTest) {
            this.mNotificationMgr = (NotificationManager) context.getSystemService("notification");
        } else {
            this.mNotificationMgr = null;
        }
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mPowerManager = powerManager;
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mAccountManager = (AccountManager) this.mContext.getSystemService("account");
        AccountManagerInternal accountManagerInternal = (AccountManagerInternal) LocalServices.getService(AccountManagerInternal.class);
        this.mAccountManagerInternal = accountManagerInternal;
        this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mAmi = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        accountManagerInternal.addOnAppPermissionChangeListener(new AccountManagerInternal.OnAppPermissionChangeListener() { // from class: com.android.server.content.SyncManager$$ExternalSyntheticLambda0
            public final void onAppPermissionChanged(Account account, int i) {
                SyncManager.this.m2874lambda$new$0$comandroidservercontentSyncManager(account, i);
            }
        });
        this.mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
        this.mSyncManagerWakeLock = powerManager.newWakeLock(1, SYNC_LOOP_WAKE_LOCK);
        this.mSyncManagerWakeLock.setReferenceCounted(false);
        this.mProvisioned = isDeviceProvisioned();
        if (!this.mProvisioned) {
            final ContentResolver resolver = context.getContentResolver();
            ContentObserver provisionedObserver = new ContentObserver(null) { // from class: com.android.server.content.SyncManager.11
                @Override // android.database.ContentObserver
                public void onChange(boolean selfChange) {
                    SyncManager.this.mProvisioned |= SyncManager.this.isDeviceProvisioned();
                    if (SyncManager.this.mProvisioned) {
                        resolver.unregisterContentObserver(this);
                    }
                }
            };
            synchronized (syncHandler) {
                resolver.registerContentObserver(Settings.Global.getUriFor("device_provisioned"), false, provisionedObserver);
                this.mProvisioned |= isDeviceProvisioned();
                if (this.mProvisioned) {
                    resolver.unregisterContentObserver(provisionedObserver);
                }
            }
        }
        if (!factoryTest) {
            this.mContext.registerReceiverAsUser(broadcastReceiver, UserHandle.ALL, new IntentFilter("android.accounts.LOGIN_ACCOUNTS_CHANGED"), null, null);
        }
        whiteListExistingSyncAdaptersIfNeeded();
        syncLogger.log("Sync manager initialized: " + Build.FINGERPRINT);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-content-SyncManager  reason: not valid java name */
    public /* synthetic */ void m2874lambda$new$0$comandroidservercontentSyncManager(Account account, int uid) {
        if (this.mAccountManagerInternal.hasAccountAccess(account, uid)) {
            scheduleSync(account, UserHandle.getUserId(uid), -2, null, null, 3, 0, Process.myUid(), -2, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onStartUser$1$com-android-server-content-SyncManager  reason: not valid java name */
    public /* synthetic */ void m2875lambda$onStartUser$1$comandroidservercontentSyncManager(int userId) {
        this.mLogger.log("onStartUser: user=", Integer.valueOf(userId));
    }

    public void onStartUser(final int userId) {
        this.mSyncHandler.post(new Runnable() { // from class: com.android.server.content.SyncManager$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                SyncManager.this.m2875lambda$onStartUser$1$comandroidservercontentSyncManager(userId);
            }
        });
    }

    public void onUnlockUser(final int userId) {
        synchronized (this.mUnlockedUsers) {
            this.mUnlockedUsers.put(userId, true);
        }
        this.mSyncHandler.post(new Runnable() { // from class: com.android.server.content.SyncManager$$ExternalSyntheticLambda13
            @Override // java.lang.Runnable
            public final void run() {
                SyncManager.this.m2877lambda$onUnlockUser$2$comandroidservercontentSyncManager(userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onUnlockUser$2$com-android-server-content-SyncManager  reason: not valid java name */
    public /* synthetic */ void m2877lambda$onUnlockUser$2$comandroidservercontentSyncManager(int userId) {
        this.mLogger.log("onUnlockUser: user=", Integer.valueOf(userId));
    }

    public void onStopUser(final int userId) {
        synchronized (this.mUnlockedUsers) {
            this.mUnlockedUsers.put(userId, false);
        }
        this.mSyncHandler.post(new Runnable() { // from class: com.android.server.content.SyncManager$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                SyncManager.this.m2876lambda$onStopUser$3$comandroidservercontentSyncManager(userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onStopUser$3$com-android-server-content-SyncManager  reason: not valid java name */
    public /* synthetic */ void m2876lambda$onStopUser$3$comandroidservercontentSyncManager(int userId) {
        this.mLogger.log("onStopUser: user=", Integer.valueOf(userId));
    }

    private boolean isUserUnlocked(int userId) {
        boolean z;
        synchronized (this.mUnlockedUsers) {
            z = this.mUnlockedUsers.get(userId);
        }
        return z;
    }

    public void onBootPhase(int phase) {
        switch (phase) {
            case SystemService.PHASE_ACTIVITY_MANAGER_READY /* 550 */:
                this.mConstants.start();
                return;
            default:
                return;
        }
    }

    private void whiteListExistingSyncAdaptersIfNeeded() {
        SyncManager syncManager = this;
        if (!syncManager.mSyncStorageEngine.shouldGrantSyncAdaptersAccountAccess()) {
            return;
        }
        List<UserInfo> users = syncManager.mUserManager.getAliveUsers();
        int userCount = users.size();
        int i = 0;
        while (i < userCount) {
            UserHandle userHandle = users.get(i).getUserHandle();
            int userId = userHandle.getIdentifier();
            for (RegisteredServicesCache.ServiceInfo<SyncAdapterType> service : syncManager.mSyncAdapters.getAllServices(userId)) {
                String packageName = service.componentName.getPackageName();
                Account[] accountsByTypeAsUser = syncManager.mAccountManager.getAccountsByTypeAsUser(((SyncAdapterType) service.type).accountType, userHandle);
                int length = accountsByTypeAsUser.length;
                int i2 = 0;
                while (i2 < length) {
                    Account account = accountsByTypeAsUser[i2];
                    if (!syncManager.canAccessAccount(account, packageName, userId)) {
                        syncManager.mAccountManager.updateAppPermission(account, "com.android.AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE", service.uid, true);
                    }
                    i2++;
                    syncManager = this;
                }
                syncManager = this;
            }
            i++;
            syncManager = this;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isDeviceProvisioned() {
        ContentResolver resolver = this.mContext.getContentResolver();
        return Settings.Global.getInt(resolver, "device_provisioned", 0) != 0;
    }

    private long jitterize(long minValue, long maxValue) {
        Random random = new Random(SystemClock.elapsedRealtime());
        long spread = maxValue - minValue;
        if (spread > 2147483647L) {
            throw new IllegalArgumentException("the difference between the maxValue and the minValue must be less than 2147483647");
        }
        return random.nextInt((int) spread) + minValue;
    }

    public SyncStorageEngine getSyncStorageEngine() {
        return this.mSyncStorageEngine;
    }

    private int getIsSyncable(Account account, int userId, String providerName) {
        int isSyncable = this.mSyncStorageEngine.getIsSyncable(account, userId, providerName);
        UserInfo userInfo = UserManager.get(this.mContext).getUserInfo(userId);
        if (userInfo == null || !userInfo.isRestricted()) {
            return isSyncable;
        }
        RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapterInfo = this.mSyncAdapters.getServiceInfo(SyncAdapterType.newKey(providerName, account.type), userId);
        if (syncAdapterInfo == null) {
            return 0;
        }
        try {
            PackageInfo pInfo = AppGlobals.getPackageManager().getPackageInfo(syncAdapterInfo.componentName.getPackageName(), 0L, userId);
            if (pInfo == null || pInfo.restrictedAccountType == null || !pInfo.restrictedAccountType.equals(account.type)) {
                return 0;
            }
            return isSyncable;
        } catch (RemoteException e) {
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setAuthorityPendingState(SyncStorageEngine.EndPoint info) {
        List<SyncOperation> ops = getAllPendingSyncs();
        for (SyncOperation op : ops) {
            if (!op.isPeriodic && op.target.matchesSpec(info)) {
                getSyncStorageEngine().markPending(info, true);
                return;
            }
        }
        getSyncStorageEngine().markPending(info, false);
    }

    public void scheduleSync(Account requestedAccount, int userId, int reason, String requestedAuthority, Bundle extras, int targetSyncState, int syncExemptionFlag, int callingUid, int callingPid, String callingPackage) {
        scheduleSync(requestedAccount, userId, reason, requestedAuthority, extras, targetSyncState, 0L, true, syncExemptionFlag, callingUid, callingPid, callingPackage);
    }

    /* JADX WARN: Code restructure failed: missing block: B:96:0x032a, code lost:
        if (r13.mSyncStorageEngine.getSyncAutomatically(r12.account, r12.userId, r8) != false) goto L99;
     */
    /* JADX WARN: Removed duplicated region for block: B:156:0x036c A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:157:0x0339 A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void scheduleSync(Account requestedAccount, final int userId, final int reason, String requestedAuthority, Bundle extras, final int targetSyncState, final long minDelayMillis, boolean checkIfAccountReady, final int syncExemptionFlag, final int callingUid, final int callingPid, final String callingPackage) {
        Bundle extras2;
        AccountAndUser[] accounts;
        int source;
        boolean uploadOnly;
        int i;
        int i2;
        AccountAndUser[] accounts2;
        Bundle extras3;
        SyncManager syncManager;
        boolean z;
        SyncManager syncManager2;
        AccountAndUser account;
        int isSyncable;
        String authority;
        boolean z2;
        boolean syncAllowed;
        Bundle extras4;
        int isSyncable2;
        AccountAndUser account2;
        long j;
        SyncManager syncManager3 = this;
        int i3 = userId;
        String str = requestedAuthority;
        final long j2 = minDelayMillis;
        if (extras != null) {
            extras2 = extras;
        } else {
            extras2 = new Bundle();
        }
        extras2.size();
        boolean z3 = false;
        if (Log.isLoggable("SyncManager", 2)) {
            syncManager3.mLogger.log("scheduleSync: account=", requestedAccount, " u", Integer.valueOf(userId), " authority=", str, " reason=", Integer.valueOf(reason), " extras=", extras2, " cuid=", Integer.valueOf(callingUid), " cpid=", Integer.valueOf(callingPid), " cpkg=", callingPackage, " mdm=", Long.valueOf(minDelayMillis), " ciar=", Boolean.valueOf(checkIfAccountReady), " sef=", Integer.valueOf(syncExemptionFlag));
        }
        AccountAndUser[] accounts3 = null;
        synchronized (syncManager3.mAccountsLock) {
            if (requestedAccount != null) {
                try {
                    if (i3 != -1) {
                        accounts = new AccountAndUser[]{new AccountAndUser(requestedAccount, i3)};
                    } else {
                        AccountAndUser[] accounts4 = syncManager3.mRunningAccounts;
                        for (AccountAndUser runningAccount : accounts4) {
                            if (requestedAccount.equals(runningAccount.account)) {
                                accounts3 = (AccountAndUser[]) ArrayUtils.appendElement(AccountAndUser.class, accounts3, runningAccount);
                            }
                        }
                        accounts = accounts3;
                    }
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
            } else {
                try {
                    AccountAndUser[] accounts5 = syncManager3.mRunningAccounts;
                    accounts = accounts5;
                } catch (Throwable th3) {
                    th = th3;
                    while (true) {
                        break;
                        break;
                    }
                    throw th;
                }
            }
            try {
                if (ArrayUtils.isEmpty(accounts)) {
                    return;
                }
                boolean uploadOnly2 = extras2.getBoolean("upload", false);
                boolean manualSync = extras2.getBoolean("force", false);
                if (manualSync) {
                    extras2.putBoolean("ignore_backoff", true);
                    extras2.putBoolean("ignore_settings", true);
                }
                boolean ignoreSettings = extras2.getBoolean("ignore_settings", false);
                if (uploadOnly2) {
                    source = 1;
                } else if (manualSync) {
                    source = 3;
                } else if (str == null) {
                    source = 2;
                } else if (extras2.containsKey("feed")) {
                    source = 5;
                } else {
                    source = 0;
                }
                int length = accounts.length;
                int isSyncable3 = 0;
                while (isSyncable3 < length) {
                    final AccountAndUser account3 = accounts[isSyncable3];
                    if (i3 >= 0 && account3.userId >= 0 && i3 != account3.userId) {
                        uploadOnly = uploadOnly2;
                        z = z3;
                        i = isSyncable3;
                        i2 = length;
                        accounts2 = accounts;
                        extras3 = extras2;
                        syncManager = syncManager3;
                    } else {
                        HashSet<String> syncableAuthorities = new HashSet<>();
                        for (Iterator it = syncManager3.mSyncAdapters.getAllServices(account3.userId).iterator(); it.hasNext(); it = it) {
                            RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapter = (RegisteredServicesCache.ServiceInfo) it.next();
                            syncableAuthorities.add(((SyncAdapterType) syncAdapter.type).authority);
                        }
                        if (str != null) {
                            boolean hasSyncAdapter = syncableAuthorities.contains(str);
                            syncableAuthorities.clear();
                            if (hasSyncAdapter) {
                                syncableAuthorities.add(str);
                            }
                        }
                        Iterator<String> it2 = syncableAuthorities.iterator();
                        while (it2.hasNext()) {
                            final String authority2 = it2.next();
                            HashSet<String> syncableAuthorities2 = syncableAuthorities;
                            int i4 = isSyncable3;
                            int isSyncable4 = syncManager3.computeSyncable(account3.account, account3.userId, authority2, !checkIfAccountReady);
                            if (isSyncable4 == 0) {
                                syncableAuthorities = syncableAuthorities2;
                                isSyncable3 = i4;
                            } else {
                                RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapterInfo = syncManager3.mSyncAdapters.getServiceInfo(SyncAdapterType.newKey(authority2, account3.account.type), account3.userId);
                                if (syncAdapterInfo == null) {
                                    syncableAuthorities = syncableAuthorities2;
                                    isSyncable3 = i4;
                                } else {
                                    int owningUid = syncAdapterInfo.uid;
                                    if (isSyncable4 == 3) {
                                        syncManager3.mLogger.log("scheduleSync: Not scheduling sync operation: isSyncable == SYNCABLE_NO_ACCOUNT_ACCESS");
                                        final Bundle finalExtras = new Bundle(extras2);
                                        String packageName = syncAdapterInfo.componentName.getPackageName();
                                        if (!syncManager3.wasPackageEverLaunched(packageName, i3)) {
                                            syncableAuthorities = syncableAuthorities2;
                                            isSyncable3 = i4;
                                        } else {
                                            j2 = minDelayMillis;
                                            syncManager3.mAccountManagerInternal.requestAccountAccess(account3.account, packageName, userId, new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.content.SyncManager$$ExternalSyntheticLambda8
                                                public final void onResult(Bundle bundle) {
                                                    SyncManager.this.m2878lambda$scheduleSync$4$comandroidservercontentSyncManager(account3, userId, reason, authority2, finalExtras, targetSyncState, j2, syncExemptionFlag, callingUid, callingPid, callingPackage, bundle);
                                                }
                                            }));
                                            syncManager3 = this;
                                            i3 = userId;
                                            length = length;
                                            accounts = accounts;
                                            syncableAuthorities = syncableAuthorities2;
                                            uploadOnly2 = uploadOnly2;
                                            isSyncable3 = i4;
                                            account3 = account3;
                                            extras2 = extras2;
                                        }
                                    } else {
                                        boolean uploadOnly3 = uploadOnly2;
                                        AccountAndUser account4 = account3;
                                        int i5 = length;
                                        AccountAndUser[] accounts6 = accounts;
                                        Bundle extras5 = extras2;
                                        int i6 = i3;
                                        boolean allowParallelSyncs = ((SyncAdapterType) syncAdapterInfo.type).allowParallelSyncs();
                                        boolean isAlwaysSyncable = ((SyncAdapterType) syncAdapterInfo.type).isAlwaysSyncable();
                                        if (checkIfAccountReady || isSyncable4 >= 0 || !isAlwaysSyncable) {
                                            syncManager2 = this;
                                            account = account4;
                                            isSyncable = isSyncable4;
                                        } else {
                                            syncManager2 = this;
                                            account = account4;
                                            syncManager2.mSyncStorageEngine.setIsSyncable(account.account, account.userId, authority2, 1, callingUid, callingPid);
                                            isSyncable = 1;
                                        }
                                        if (targetSyncState != -2 && targetSyncState != isSyncable) {
                                            account3 = account;
                                            syncManager3 = syncManager2;
                                            i3 = i6;
                                            length = i5;
                                            accounts = accounts6;
                                            syncableAuthorities = syncableAuthorities2;
                                            uploadOnly2 = uploadOnly3;
                                            isSyncable3 = i4;
                                            extras2 = extras5;
                                            j2 = minDelayMillis;
                                        } else if (((SyncAdapterType) syncAdapterInfo.type).supportsUploading() || !uploadOnly3) {
                                            if (isSyncable < 0 || ignoreSettings) {
                                                authority = authority2;
                                            } else {
                                                if (!syncManager2.mSyncStorageEngine.getMasterSyncAutomatically(account.userId)) {
                                                    authority = authority2;
                                                } else {
                                                    authority = authority2;
                                                }
                                                z2 = false;
                                                syncAllowed = z2;
                                                if (syncAllowed) {
                                                    syncManager2.mLogger.log("scheduleSync: sync of ", account, " ", authority, " is not allowed, dropping request");
                                                    account3 = account;
                                                    syncManager3 = syncManager2;
                                                    i3 = i6;
                                                    length = i5;
                                                    accounts = accounts6;
                                                    syncableAuthorities = syncableAuthorities2;
                                                    uploadOnly2 = uploadOnly3;
                                                    isSyncable3 = i4;
                                                    extras2 = extras5;
                                                    j2 = minDelayMillis;
                                                } else {
                                                    SyncStorageEngine.EndPoint info = new SyncStorageEngine.EndPoint(account.account, authority, account.userId);
                                                    syncManager2.mSyncStorageEngine.getDelayUntilTime(info);
                                                    String owningPackage = syncAdapterInfo.componentName.getPackageName();
                                                    if (isSyncable == -1) {
                                                        if (checkIfAccountReady) {
                                                            final Bundle finalExtras2 = new Bundle(extras5);
                                                            extras4 = extras5;
                                                            final AccountAndUser accountAndUser = account;
                                                            final String str2 = authority;
                                                            account2 = account;
                                                            sendOnUnsyncableAccount(syncManager2.mContext, syncAdapterInfo, account.userId, new OnReadyCallback() { // from class: com.android.server.content.SyncManager$$ExternalSyntheticLambda9
                                                                @Override // com.android.server.content.SyncManager.OnReadyCallback
                                                                public final void onReady() {
                                                                    SyncManager.this.m2879lambda$scheduleSync$5$comandroidservercontentSyncManager(accountAndUser, reason, str2, finalExtras2, targetSyncState, minDelayMillis, syncExemptionFlag, callingUid, callingPid, callingPackage);
                                                                }
                                                            });
                                                            syncManager2 = this;
                                                            j = minDelayMillis;
                                                        } else {
                                                            String authority3 = authority;
                                                            account2 = account;
                                                            extras4 = extras5;
                                                            Bundle newExtras = new Bundle();
                                                            newExtras.putBoolean("initialize", true);
                                                            syncManager2 = this;
                                                            syncManager2.mLogger.log("scheduleSync: schedule initialisation sync ", account2, " ", authority3);
                                                            syncManager2.postScheduleSyncMessage(new SyncOperation(account2.account, account2.userId, owningUid, owningPackage, reason, source, authority3, newExtras, allowParallelSyncs, syncExemptionFlag), minDelayMillis);
                                                            j = minDelayMillis;
                                                        }
                                                    } else {
                                                        String authority4 = authority;
                                                        int isSyncable5 = isSyncable;
                                                        extras4 = extras5;
                                                        AccountAndUser account5 = account;
                                                        if (targetSyncState != -2) {
                                                            isSyncable2 = isSyncable5;
                                                            if (targetSyncState != isSyncable2) {
                                                                syncManager2.mLogger.log("scheduleSync: not handling ", account5, " ", authority4);
                                                                account2 = account5;
                                                                j = minDelayMillis;
                                                            }
                                                        } else {
                                                            isSyncable2 = isSyncable5;
                                                        }
                                                        syncManager2.mLogger.log("scheduleSync: scheduling sync ", account5, " ", authority4);
                                                        account2 = account5;
                                                        j = minDelayMillis;
                                                        syncManager2.postScheduleSyncMessage(new SyncOperation(account5.account, account5.userId, owningUid, owningPackage, reason, source, authority4, extras4, allowParallelSyncs, syncExemptionFlag), j);
                                                    }
                                                    j2 = j;
                                                    length = i5;
                                                    accounts = accounts6;
                                                    extras2 = extras4;
                                                    syncableAuthorities = syncableAuthorities2;
                                                    uploadOnly2 = uploadOnly3;
                                                    isSyncable3 = i4;
                                                    account3 = account2;
                                                    syncManager3 = syncManager2;
                                                    i3 = userId;
                                                }
                                            }
                                            z2 = true;
                                            syncAllowed = z2;
                                            if (syncAllowed) {
                                            }
                                        } else {
                                            account3 = account;
                                            syncManager3 = syncManager2;
                                            i3 = i6;
                                            length = i5;
                                            accounts = accounts6;
                                            syncableAuthorities = syncableAuthorities2;
                                            uploadOnly2 = uploadOnly3;
                                            isSyncable3 = i4;
                                            extras2 = extras5;
                                            j2 = minDelayMillis;
                                        }
                                    }
                                }
                            }
                        }
                        uploadOnly = uploadOnly2;
                        i = isSyncable3;
                        i2 = length;
                        accounts2 = accounts;
                        extras3 = extras2;
                        syncManager = syncManager3;
                        z = false;
                    }
                    isSyncable3 = i + 1;
                    str = requestedAuthority;
                    j2 = j2;
                    length = i2;
                    accounts = accounts2;
                    extras2 = extras3;
                    uploadOnly2 = uploadOnly;
                    z3 = z;
                    syncManager3 = syncManager;
                    i3 = userId;
                }
            } catch (Throwable th4) {
                th = th4;
                while (true) {
                    break;
                    break;
                }
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleSync$4$com-android-server-content-SyncManager  reason: not valid java name */
    public /* synthetic */ void m2878lambda$scheduleSync$4$comandroidservercontentSyncManager(AccountAndUser account, int userId, int reason, String authority, Bundle finalExtras, int targetSyncState, long minDelayMillis, int syncExemptionFlag, int callingUid, int callingPid, String callingPackage, Bundle result) {
        if (result != null && result.getBoolean("booleanResult")) {
            scheduleSync(account.account, userId, reason, authority, finalExtras, targetSyncState, minDelayMillis, true, syncExemptionFlag, callingUid, callingPid, callingPackage);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleSync$5$com-android-server-content-SyncManager  reason: not valid java name */
    public /* synthetic */ void m2879lambda$scheduleSync$5$comandroidservercontentSyncManager(AccountAndUser account, int reason, String authority, Bundle finalExtras, int targetSyncState, long minDelayMillis, int syncExemptionFlag, int callingUid, int callingPid, String callingPackage) {
        scheduleSync(account.account, account.userId, reason, authority, finalExtras, targetSyncState, minDelayMillis, false, syncExemptionFlag, callingUid, callingPid, callingPackage);
    }

    public int computeSyncable(Account account, int userId, String authority, boolean checkAccountAccess) {
        int status = getIsSyncable(account, userId, authority);
        if (status == 0) {
            return 0;
        }
        SyncAdapterType type = SyncAdapterType.newKey(authority, account.type);
        RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapterInfo = this.mSyncAdapters.getServiceInfo(type, userId);
        if (syncAdapterInfo == null) {
            return 0;
        }
        int owningUid = syncAdapterInfo.uid;
        String owningPackage = syncAdapterInfo.componentName.getPackageName();
        if (this.mAmi.isAppStartModeDisabled(owningUid, owningPackage)) {
            Slog.w("SyncManager", "Not scheduling job " + syncAdapterInfo.uid + ":" + syncAdapterInfo.componentName + " -- package not allowed to start");
            return 0;
        } else if (checkAccountAccess && !canAccessAccount(account, owningPackage, owningUid)) {
            Log.w("SyncManager", "Access to " + SyncLogger.logSafe(account) + " denied for package " + owningPackage + " in UID " + syncAdapterInfo.uid);
            return 3;
        } else if (ITranSyncManager.Instance().hookComputeSyncable(syncAdapterInfo.componentName, owningUid)) {
            return 0;
        } else {
            return status;
        }
    }

    private boolean canAccessAccount(Account account, String packageName, int uid) {
        if (this.mAccountManager.hasAccountAccess(account, packageName, UserHandle.getUserHandleForUid(uid))) {
            return true;
        }
        try {
            this.mContext.getPackageManager().getApplicationInfoAsUser(packageName, 1048576, UserHandle.getUserId(uid));
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeSyncsForAuthority(SyncStorageEngine.EndPoint info, String why) {
        this.mLogger.log("removeSyncsForAuthority: ", info, why);
        verifyJobScheduler();
        List<SyncOperation> ops = getAllPendingSyncs();
        for (SyncOperation op : ops) {
            if (op.target.matchesSpec(info)) {
                this.mLogger.log("canceling: ", op);
                cancelJob(op, why);
            }
        }
    }

    public void removePeriodicSync(SyncStorageEngine.EndPoint target, Bundle extras, String why) {
        Message m = this.mSyncHandler.obtainMessage(14, Pair.create(target, why));
        m.setData(extras);
        m.sendToTarget();
    }

    public void updateOrAddPeriodicSync(SyncStorageEngine.EndPoint target, long pollFrequency, long flex, Bundle extras) {
        UpdatePeriodicSyncMessagePayload payload = new UpdatePeriodicSyncMessagePayload(target, pollFrequency, flex, extras);
        this.mSyncHandler.obtainMessage(13, payload).sendToTarget();
    }

    public List<PeriodicSync> getPeriodicSyncs(SyncStorageEngine.EndPoint target) {
        List<SyncOperation> ops = getAllPendingSyncs();
        List<PeriodicSync> periodicSyncs = new ArrayList<>();
        for (SyncOperation op : ops) {
            if (op.isPeriodic && op.target.matchesSpec(target)) {
                periodicSyncs.add(new PeriodicSync(op.target.account, op.target.provider, op.getClonedExtras(), op.periodMillis / 1000, op.flexMillis / 1000));
            }
        }
        return periodicSyncs;
    }

    public void scheduleLocalSync(Account account, int userId, int reason, String authority, int syncExemptionFlag, int callingUid, int callingPid, String callingPackage) {
        Bundle extras = new Bundle();
        extras.putBoolean("upload", true);
        scheduleSync(account, userId, reason, authority, extras, -2, LOCAL_SYNC_DELAY, true, syncExemptionFlag, callingUid, callingPid, callingPackage);
    }

    public SyncAdapterType[] getSyncAdapterTypes(int callingUid, int userId) {
        Collection<RegisteredServicesCache.ServiceInfo<SyncAdapterType>> serviceInfos = this.mSyncAdapters.getAllServices(userId);
        List<SyncAdapterType> types = new ArrayList<>(serviceInfos.size());
        for (RegisteredServicesCache.ServiceInfo<SyncAdapterType> serviceInfo : serviceInfos) {
            String packageName = ((SyncAdapterType) serviceInfo.type).getPackageName();
            if (TextUtils.isEmpty(packageName) || !this.mPackageManagerInternal.filterAppAccess(packageName, callingUid, userId)) {
                types.add((SyncAdapterType) serviceInfo.type);
            }
        }
        return (SyncAdapterType[]) types.toArray(new SyncAdapterType[0]);
    }

    public String[] getSyncAdapterPackagesForAuthorityAsUser(String authority, int callingUid, int userId) {
        String[] syncAdapterPackages = this.mSyncAdapters.getSyncAdapterPackagesForAuthority(authority, userId);
        List<String> filteredResult = new ArrayList<>(syncAdapterPackages.length);
        for (String packageName : syncAdapterPackages) {
            if (!TextUtils.isEmpty(packageName) && !this.mPackageManagerInternal.filterAppAccess(packageName, callingUid, userId)) {
                filteredResult.add(packageName);
            }
        }
        return (String[]) filteredResult.toArray(new String[0]);
    }

    public String getSyncAdapterPackageAsUser(String accountType, String authority, int callingUid, int userId) {
        RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapterInfo;
        if (accountType == null || authority == null || (syncAdapterInfo = this.mSyncAdapters.getServiceInfo(SyncAdapterType.newKey(authority, accountType), userId)) == null) {
            return null;
        }
        String packageName = ((SyncAdapterType) syncAdapterInfo.type).getPackageName();
        if (TextUtils.isEmpty(packageName) || this.mPackageManagerInternal.filterAppAccess(packageName, callingUid, userId)) {
            return null;
        }
        return packageName;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendSyncFinishedOrCanceledMessage(ActiveSyncContext syncContext, SyncResult syncResult) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "sending MESSAGE_SYNC_FINISHED");
        }
        Message msg = this.mSyncHandler.obtainMessage();
        msg.what = 1;
        msg.obj = new SyncFinishedOrCancelledMessagePayload(syncContext, syncResult);
        this.mSyncHandler.sendMessage(msg);
    }

    private void sendCancelSyncsMessage(SyncStorageEngine.EndPoint info, Bundle extras, String why) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "sending MESSAGE_CANCEL");
        }
        this.mLogger.log("sendCancelSyncsMessage() ep=", info, " why=", why);
        Message msg = this.mSyncHandler.obtainMessage();
        msg.what = 6;
        msg.setData(extras);
        msg.obj = info;
        this.mSyncHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void postMonitorSyncProgressMessage(ActiveSyncContext activeSyncContext) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "posting MESSAGE_SYNC_MONITOR in 60s");
        }
        activeSyncContext.mBytesTransferredAtLastPoll = getTotalBytesTransferredByUid(activeSyncContext.mSyncAdapterUid);
        activeSyncContext.mLastPolledTimeElapsed = SystemClock.elapsedRealtime();
        Message monitorMessage = this.mSyncHandler.obtainMessage(8, activeSyncContext);
        this.mSyncHandler.sendMessageDelayed(monitorMessage, 60000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void postScheduleSyncMessage(SyncOperation syncOperation, long minDelayMillis) {
        ScheduleSyncMessagePayload payload = new ScheduleSyncMessagePayload(syncOperation, minDelayMillis);
        this.mSyncHandler.obtainMessage(12, payload).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public long getTotalBytesTransferredByUid(int uid) {
        return TrafficStats.getUidRxBytes(uid) + TrafficStats.getUidTxBytes(uid);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SyncFinishedOrCancelledMessagePayload {
        public final ActiveSyncContext activeSyncContext;
        public final SyncResult syncResult;

        SyncFinishedOrCancelledMessagePayload(ActiveSyncContext syncContext, SyncResult syncResult) {
            this.activeSyncContext = syncContext;
            this.syncResult = syncResult;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class UpdatePeriodicSyncMessagePayload {
        public final Bundle extras;
        public final long flex;
        public final long pollFrequency;
        public final SyncStorageEngine.EndPoint target;

        UpdatePeriodicSyncMessagePayload(SyncStorageEngine.EndPoint target, long pollFrequency, long flex, Bundle extras) {
            this.target = target;
            this.pollFrequency = pollFrequency;
            this.flex = flex;
            this.extras = extras;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ScheduleSyncMessagePayload {
        final long minDelayMillis;
        final SyncOperation syncOperation;

        ScheduleSyncMessagePayload(SyncOperation syncOperation, long minDelayMillis) {
            this.syncOperation = syncOperation;
            this.minDelayMillis = minDelayMillis;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearBackoffSetting(SyncStorageEngine.EndPoint target, String why) {
        Pair<Long, Long> backoff = this.mSyncStorageEngine.getBackoff(target);
        if (backoff != null && ((Long) backoff.first).longValue() == -1 && ((Long) backoff.second).longValue() == -1) {
            return;
        }
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "Clearing backoffs for " + target);
        }
        this.mSyncStorageEngine.setBackoff(target, -1L, -1L);
        rescheduleSyncs(target, why);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void increaseBackoffSetting(SyncStorageEngine.EndPoint target) {
        long newDelayInMs;
        long now = SystemClock.elapsedRealtime();
        Pair<Long, Long> previousSettings = this.mSyncStorageEngine.getBackoff(target);
        long newDelayInMs2 = -1;
        if (previousSettings != null) {
            if (now >= ((Long) previousSettings.first).longValue()) {
                newDelayInMs2 = ((float) ((Long) previousSettings.second).longValue()) * this.mConstants.getRetryTimeIncreaseFactor();
            } else if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "Still in backoff, do not increase it. Remaining: " + ((((Long) previousSettings.first).longValue() - now) / 1000) + " seconds.");
                return;
            } else {
                return;
            }
        }
        if (newDelayInMs2 <= 0) {
            long initialRetryMs = this.mConstants.getInitialSyncRetryTimeInSeconds() * 1000;
            newDelayInMs2 = jitterize(initialRetryMs, (long) (initialRetryMs * 1.1d));
        }
        long maxSyncRetryTimeInSeconds = this.mConstants.getMaxSyncRetryTimeInSeconds();
        if (newDelayInMs2 <= maxSyncRetryTimeInSeconds * 1000) {
            newDelayInMs = newDelayInMs2;
        } else {
            long newDelayInMs3 = maxSyncRetryTimeInSeconds * 1000;
            newDelayInMs = newDelayInMs3;
        }
        long backoff = now + newDelayInMs;
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "Backoff until: " + backoff + ", delayTime: " + newDelayInMs);
        }
        this.mSyncStorageEngine.setBackoff(target, backoff, newDelayInMs);
        rescheduleSyncs(target, "increaseBackoffSetting");
    }

    private void rescheduleSyncs(SyncStorageEngine.EndPoint target, String why) {
        this.mLogger.log("rescheduleSyncs() ep=", target, " why=", why);
        List<SyncOperation> ops = getAllPendingSyncs();
        int count = 0;
        for (SyncOperation op : ops) {
            if (!op.isPeriodic && op.target.matchesSpec(target)) {
                count++;
                cancelJob(op, why);
                postScheduleSyncMessage(op, 0L);
            }
        }
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "Rescheduled " + count + " syncs for " + target);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDelayUntilTime(SyncStorageEngine.EndPoint target, long delayUntilSeconds) {
        long newDelayUntilTime;
        long delayUntil = 1000 * delayUntilSeconds;
        long absoluteNow = System.currentTimeMillis();
        if (delayUntil > absoluteNow) {
            newDelayUntilTime = SystemClock.elapsedRealtime() + (delayUntil - absoluteNow);
        } else {
            newDelayUntilTime = 0;
        }
        this.mSyncStorageEngine.setDelayUntilTime(target, newDelayUntilTime);
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "Delay Until time set to " + newDelayUntilTime + " for " + target);
        }
        rescheduleSyncs(target, "delayUntil newDelayUntilTime: " + newDelayUntilTime);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isAdapterDelayed(SyncStorageEngine.EndPoint target) {
        long now = SystemClock.elapsedRealtime();
        Pair<Long, Long> backoff = this.mSyncStorageEngine.getBackoff(target);
        if ((backoff != null && ((Long) backoff.first).longValue() != -1 && ((Long) backoff.first).longValue() > now) || this.mSyncStorageEngine.getDelayUntilTime(target) > now) {
            return true;
        }
        return false;
    }

    public void cancelActiveSync(SyncStorageEngine.EndPoint info, Bundle extras, String why) {
        sendCancelSyncsMessage(info, extras, why);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleSyncOperationH(SyncOperation syncOperation) {
        scheduleSyncOperationH(syncOperation, 0L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleSyncOperationH(SyncOperation syncOperation, long minDelay) {
        long minDelay2;
        boolean isLoggable;
        boolean z;
        DeviceIdleInternal dic;
        String str;
        boolean isLoggable2;
        long now;
        long delayUntilDelay;
        boolean isLoggable3 = Log.isLoggable("SyncManager", 2);
        if (syncOperation == null) {
            Slog.e("SyncManager", "Can't schedule null sync operation.");
            return;
        }
        if (syncOperation.hasIgnoreBackoff()) {
            minDelay2 = minDelay;
        } else {
            Pair<Long, Long> backoff = this.mSyncStorageEngine.getBackoff(syncOperation.target);
            if (backoff != null) {
                if (((Long) backoff.first).longValue() != -1) {
                    syncOperation.scheduleEjAsRegularJob = true;
                }
            } else {
                Slog.e("SyncManager", "Couldn't find backoff values for " + SyncLogger.logSafe(syncOperation.target));
                backoff = new Pair<>(-1L, -1L);
            }
            long now2 = SystemClock.elapsedRealtime();
            long backoffDelay = ((Long) backoff.first).longValue() == -1 ? 0L : ((Long) backoff.first).longValue() - now2;
            long delayUntil = this.mSyncStorageEngine.getDelayUntilTime(syncOperation.target);
            long delayUntilDelay2 = delayUntil > now2 ? delayUntil - now2 : 0L;
            if (!isLoggable3) {
                delayUntilDelay = delayUntilDelay2;
            } else {
                delayUntilDelay = delayUntilDelay2;
                Slog.v("SyncManager", "backoff delay:" + backoffDelay + " delayUntil delay:" + delayUntilDelay);
            }
            minDelay2 = Math.max(minDelay, Math.max(backoffDelay, delayUntilDelay));
        }
        if (minDelay2 < 0) {
            minDelay2 = 0;
        } else if (minDelay2 > 0) {
            syncOperation.scheduleEjAsRegularJob = true;
        }
        if (syncOperation.isPeriodic) {
            isLoggable = isLoggable3;
        } else {
            int inheritedSyncExemptionFlag = 0;
            Iterator<ActiveSyncContext> it = this.mActiveSyncContexts.iterator();
            while (it.hasNext()) {
                ActiveSyncContext asc = it.next();
                if (asc.mSyncOperation.key.equals(syncOperation.key)) {
                    if (isLoggable3) {
                        Log.v("SyncManager", "Duplicate sync is already running. Not scheduling " + syncOperation);
                        return;
                    }
                    return;
                }
            }
            int duplicatesCount = 0;
            long now3 = SystemClock.elapsedRealtime();
            syncOperation.expectedRuntime = now3 + minDelay2;
            List<SyncOperation> pending = getAllPendingSyncs();
            SyncOperation syncToRun = syncOperation;
            for (SyncOperation op : pending) {
                if (!op.isPeriodic) {
                    if (!op.key.equals(syncOperation.key)) {
                        isLoggable2 = isLoggable3;
                        now = now3;
                    } else {
                        now = now3;
                        isLoggable2 = isLoggable3;
                        if (syncToRun.expectedRuntime > op.expectedRuntime) {
                            syncToRun = op;
                        }
                        duplicatesCount++;
                    }
                    isLoggable3 = isLoggable2;
                    now3 = now;
                }
            }
            isLoggable = isLoggable3;
            if (duplicatesCount > 1) {
                StringBuilder append = new StringBuilder().append("duplicates found when scheduling a sync operation: owningUid=").append(syncOperation.owningUid).append("; owningPackage=").append(syncOperation.owningPackage).append("; source=").append(syncOperation.syncSource).append("; adapter=");
                if (syncOperation.target != null) {
                    str = syncOperation.target.provider;
                } else {
                    str = UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
                }
                Slog.wtf("SyncManager", append.append(str).toString());
            }
            if (syncOperation != syncToRun && minDelay2 == 0 && syncToRun.syncExemptionFlag < syncOperation.syncExemptionFlag) {
                syncToRun = syncOperation;
                inheritedSyncExemptionFlag = Math.max(0, syncToRun.syncExemptionFlag);
            }
            for (SyncOperation op2 : pending) {
                if (!op2.isPeriodic && op2.key.equals(syncOperation.key) && op2 != syncToRun) {
                    if (isLoggable) {
                        Slog.v("SyncManager", "Cancelling duplicate sync " + op2);
                    }
                    inheritedSyncExemptionFlag = Math.max(inheritedSyncExemptionFlag, op2.syncExemptionFlag);
                    cancelJob(op2, "scheduleSyncOperationH-duplicate");
                }
            }
            if (syncToRun != syncOperation) {
                if (isLoggable) {
                    Slog.v("SyncManager", "Not scheduling because a duplicate exists.");
                    return;
                }
                return;
            } else if (inheritedSyncExemptionFlag > 0) {
                syncOperation.syncExemptionFlag = inheritedSyncExemptionFlag;
            }
        }
        if (syncOperation.jobId == -1) {
            syncOperation.jobId = getUnusedJobIdH();
        }
        if (isLoggable) {
            Slog.v("SyncManager", "scheduling sync operation " + syncOperation.toString());
        }
        int bias = syncOperation.getJobBias();
        int networkType = syncOperation.isNotAllowedOnMetered() ? 2 : 1;
        int jobFlags = syncOperation.isAppStandbyExempted() ? 8 : 0;
        JobInfo.Builder b = new JobInfo.Builder(syncOperation.jobId, new ComponentName(this.mContext, SyncJobService.class)).setExtras(syncOperation.toJobInfoExtras()).setRequiredNetworkType(networkType).setRequiresStorageNotLow(true).setPersisted(true).setBias(bias).setFlags(jobFlags);
        if (syncOperation.isPeriodic) {
            b.setPeriodic(syncOperation.periodMillis, syncOperation.flexMillis);
            z = true;
        } else {
            if (minDelay2 > 0) {
                b.setMinimumLatency(minDelay2);
            }
            z = true;
            getSyncStorageEngine().markPending(syncOperation.target, true);
        }
        if (syncOperation.hasRequireCharging()) {
            b.setRequiresCharging(z);
        }
        if (syncOperation.isScheduledAsExpeditedJob() && !syncOperation.scheduleEjAsRegularJob) {
            b.setExpedited(z);
        }
        if (syncOperation.syncExemptionFlag == 2 && (dic = (DeviceIdleInternal) LocalServices.getService(DeviceIdleInternal.class)) != null) {
            dic.addPowerSaveTempWhitelistApp(1000, syncOperation.owningPackage, this.mConstants.getKeyExemptionTempWhitelistDurationInSeconds() * 1000, 1, UserHandle.getUserId(syncOperation.owningUid), false, 306, "sync by top app");
        }
        UsageStatsManagerInternal usmi = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        if (usmi != null) {
            usmi.reportSyncScheduled(syncOperation.owningPackage, UserHandle.getUserId(syncOperation.owningUid), syncOperation.isAppStandbyExempted());
        }
        JobInfo ji = b.build();
        int result = getJobScheduler().scheduleAsPackage(ji, syncOperation.owningPackage, syncOperation.target.userId, syncOperation.wakeLockName());
        if (result == 0 && ji.isExpedited()) {
            if (isLoggable) {
                Slog.i("SyncManager", "Failed to schedule EJ for " + syncOperation.owningPackage + ". Downgrading to regular");
            }
            syncOperation.scheduleEjAsRegularJob = true;
            b.setExpedited(false).setExtras(syncOperation.toJobInfoExtras());
            result = getJobScheduler().scheduleAsPackage(b.build(), syncOperation.owningPackage, syncOperation.target.userId, syncOperation.wakeLockName());
        }
        if (result == 0) {
            Slog.e("SyncManager", "Failed to schedule job for " + syncOperation.owningPackage);
        }
    }

    public void clearScheduledSyncOperations(SyncStorageEngine.EndPoint info) {
        List<SyncOperation> ops = getAllPendingSyncs();
        for (SyncOperation op : ops) {
            if (!op.isPeriodic && op.target.matchesSpec(info)) {
                cancelJob(op, "clearScheduledSyncOperations");
                getSyncStorageEngine().markPending(op.target, false);
            }
        }
        this.mSyncStorageEngine.setBackoff(info, -1L, -1L);
    }

    public void cancelScheduledSyncOperation(SyncStorageEngine.EndPoint info, Bundle extras) {
        List<SyncOperation> ops = getAllPendingSyncs();
        for (SyncOperation op : ops) {
            if (!op.isPeriodic && op.target.matchesSpec(info) && op.areExtrasEqual(extras, false)) {
                cancelJob(op, "cancelScheduledSyncOperation");
            }
        }
        setAuthorityPendingState(info);
        if (!this.mSyncStorageEngine.isSyncPending(info)) {
            this.mSyncStorageEngine.setBackoff(info, -1L, -1L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeRescheduleSync(SyncResult syncResult, SyncOperation operation) {
        boolean isLoggable = Log.isLoggable("SyncManager", 3);
        if (isLoggable) {
            Log.d("SyncManager", "encountered error(s) during the sync: " + syncResult + ", " + operation);
        }
        operation.enableBackoff();
        operation.scheduleEjAsRegularJob = true;
        if (operation.hasDoNotRetry() && !syncResult.syncAlreadyInProgress) {
            if (isLoggable) {
                Log.d("SyncManager", "not retrying sync operation because SYNC_EXTRAS_DO_NOT_RETRY was specified " + operation);
            }
        } else if (operation.isUpload() && !syncResult.syncAlreadyInProgress) {
            operation.enableTwoWaySync();
            if (isLoggable) {
                Log.d("SyncManager", "retrying sync operation as a two-way sync because an upload-only sync encountered an error: " + operation);
            }
            scheduleSyncOperationH(operation);
        } else if (syncResult.tooManyRetries) {
            if (isLoggable) {
                Log.d("SyncManager", "not retrying sync operation because it retried too many times: " + operation);
            }
        } else if (syncResult.madeSomeProgress()) {
            if (isLoggable) {
                Log.d("SyncManager", "retrying sync operation because even though it had an error it achieved some success");
            }
            scheduleSyncOperationH(operation);
        } else if (syncResult.syncAlreadyInProgress) {
            if (isLoggable) {
                Log.d("SyncManager", "retrying sync operation that failed because there was already a sync in progress: " + operation);
            }
            scheduleSyncOperationH(operation, 10000L);
        } else if (syncResult.hasSoftError()) {
            if (isLoggable) {
                Log.d("SyncManager", "retrying sync operation because it encountered a soft error: " + operation);
            }
            scheduleSyncOperationH(operation);
        } else {
            Log.e("SyncManager", "not retrying sync operation because the error is a hard error: " + SyncLogger.logSafe(operation));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUserUnlocked(int userId) {
        AccountManagerService.getSingleton().validateAccounts(userId);
        this.mSyncAdapters.invalidateCache(userId);
        SyncStorageEngine.EndPoint target = new SyncStorageEngine.EndPoint(null, null, userId);
        updateRunningAccounts(target);
        Account[] accounts = AccountManagerService.getSingleton().getAccounts(userId, this.mContext.getOpPackageName());
        for (Account account : accounts) {
            scheduleSync(account, userId, -8, null, null, -1, 0, Process.myUid(), -3, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUserStopped(int userId) {
        updateRunningAccounts(null);
        cancelActiveSync(new SyncStorageEngine.EndPoint(null, null, userId), null, "onUserStopped");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUserRemoved(int userId) {
        this.mLogger.log("onUserRemoved: u", Integer.valueOf(userId));
        updateRunningAccounts(null);
        this.mSyncStorageEngine.removeStaleAccounts(null, userId);
        List<SyncOperation> ops = getAllPendingSyncs();
        for (SyncOperation op : ops) {
            if (op.target.userId == userId) {
                cancelJob(op, "user removed u" + userId);
            }
        }
    }

    static Intent getAdapterBindIntent(Context context, ComponentName syncAdapterComponent, int userId) {
        Intent intent = new Intent();
        intent.setAction("android.content.SyncAdapter");
        intent.setComponent(syncAdapterComponent);
        intent.putExtra("android.intent.extra.client_label", 17041610);
        intent.putExtra("android.intent.extra.client_intent", PendingIntent.getActivityAsUser(context, 0, new Intent("android.settings.SYNC_SETTINGS"), 67108864, null, UserHandle.of(userId)));
        return intent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ActiveSyncContext extends ISyncContext.Stub implements ServiceConnection, IBinder.DeathRecipient {
        boolean mBound;
        long mBytesTransferredAtLastPoll;
        String mEventName;
        final long mHistoryRowId;
        long mLastPolledTimeElapsed;
        final long mStartTime;
        final int mSyncAdapterUid;
        SyncInfo mSyncInfo;
        final SyncOperation mSyncOperation;
        final PowerManager.WakeLock mSyncWakeLock;
        long mTimeoutStartTime;
        boolean mIsLinkedToDeath = false;
        ISyncAdapter mSyncAdapter = null;

        public ActiveSyncContext(SyncOperation syncOperation, long historyRowId, int syncAdapterUid) {
            this.mSyncAdapterUid = syncAdapterUid;
            this.mSyncOperation = syncOperation;
            this.mHistoryRowId = historyRowId;
            long elapsedRealtime = SystemClock.elapsedRealtime();
            this.mStartTime = elapsedRealtime;
            this.mTimeoutStartTime = elapsedRealtime;
            PowerManager.WakeLock syncWakeLock = SyncManager.this.mSyncHandler.getSyncWakeLock(syncOperation);
            this.mSyncWakeLock = syncWakeLock;
            syncWakeLock.setWorkSource(new WorkSource(syncAdapterUid));
            syncWakeLock.acquire();
        }

        public void sendHeartbeat() {
        }

        public void onFinished(SyncResult result) {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "onFinished: " + this);
            }
            SyncLogger syncLogger = SyncManager.this.mLogger;
            Object[] objArr = new Object[4];
            objArr[0] = "onFinished result=";
            objArr[1] = result;
            objArr[2] = " endpoint=";
            SyncOperation syncOperation = this.mSyncOperation;
            objArr[3] = syncOperation == null ? "null" : syncOperation.target;
            syncLogger.log(objArr);
            SyncManager.this.sendSyncFinishedOrCanceledMessage(this, result);
        }

        public void toString(StringBuilder sb, boolean logSafe) {
            StringBuilder append = sb.append("startTime ").append(this.mStartTime).append(", mTimeoutStartTime ").append(this.mTimeoutStartTime).append(", mHistoryRowId ").append(this.mHistoryRowId).append(", syncOperation ");
            SyncOperation syncOperation = this.mSyncOperation;
            String str = syncOperation;
            if (logSafe) {
                str = SyncLogger.logSafe(syncOperation);
            }
            append.append(str);
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            Message msg = SyncManager.this.mSyncHandler.obtainMessage();
            msg.what = 4;
            msg.obj = new ServiceConnectionData(this, service);
            SyncManager.this.mSyncHandler.sendMessage(msg);
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            Message msg = SyncManager.this.mSyncHandler.obtainMessage();
            msg.what = 5;
            msg.obj = new ServiceConnectionData(this, null);
            SyncManager.this.mSyncHandler.sendMessage(msg);
        }

        boolean bindToSyncAdapter(ComponentName serviceComponent, int userId) {
            if (Log.isLoggable("SyncManager", 2)) {
                Log.d("SyncManager", "bindToSyncAdapter: " + serviceComponent + ", connection " + this);
            }
            Intent intent = SyncManager.getAdapterBindIntent(SyncManager.this.mContext, serviceComponent, userId);
            this.mBound = true;
            boolean bindResult = SyncManager.this.mContext.bindServiceAsUser(intent, this, 21, new UserHandle(this.mSyncOperation.target.userId));
            SyncManager.this.mLogger.log("bindService() returned=", Boolean.valueOf(this.mBound), " for ", this);
            if (!bindResult) {
                this.mBound = false;
            } else {
                try {
                    this.mEventName = this.mSyncOperation.wakeLockName();
                    SyncManager.this.mBatteryStats.noteSyncStart(this.mEventName, this.mSyncAdapterUid);
                } catch (RemoteException e) {
                }
            }
            return bindResult;
        }

        protected void close() {
            if (Log.isLoggable("SyncManager", 2)) {
                Log.d("SyncManager", "unBindFromSyncAdapter: connection " + this);
            }
            if (this.mBound) {
                this.mBound = false;
                SyncManager.this.mLogger.log("unbindService for ", this);
                SyncManager.this.mContext.unbindService(this);
                try {
                    SyncManager.this.mBatteryStats.noteSyncFinish(this.mEventName, this.mSyncAdapterUid);
                } catch (RemoteException e) {
                }
            }
            this.mSyncWakeLock.release();
            this.mSyncWakeLock.setWorkSource(null);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb, false);
            return sb.toString();
        }

        public String toSafeString() {
            StringBuilder sb = new StringBuilder();
            toString(sb, true);
            return sb.toString();
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            SyncManager.this.sendSyncFinishedOrCanceledMessage(this, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dump(FileDescriptor fd, PrintWriter pw, boolean dumpAll) {
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        SyncAdapterStateFetcher buckets = new SyncAdapterStateFetcher();
        dumpSyncState(ipw, buckets);
        this.mConstants.dump(pw, "");
        dumpSyncAdapters(ipw);
        if (dumpAll) {
            ipw.println("Detailed Sync History");
            this.mLogger.dumpAll(pw);
        }
    }

    static String formatTime(long time) {
        if (time == 0) {
            return "N/A";
        }
        return TimeMigrationUtils.formatMillisWithFixedFormat(time);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$static$6(SyncOperation op1, SyncOperation op2) {
        int res = Integer.compare(op1.target.userId, op2.target.userId);
        if (res != 0) {
            return res;
        }
        Comparator<String> stringComparator = String.CASE_INSENSITIVE_ORDER;
        int res2 = stringComparator.compare(op1.target.account.type, op2.target.account.type);
        if (res2 != 0) {
            return res2;
        }
        int res3 = stringComparator.compare(op1.target.account.name, op2.target.account.name);
        if (res3 != 0) {
            return res3;
        }
        int res4 = stringComparator.compare(op1.target.provider, op2.target.provider);
        if (res4 != 0) {
            return res4;
        }
        int res5 = Integer.compare(op1.reason, op2.reason);
        if (res5 != 0) {
            return res5;
        }
        int res6 = Long.compare(op1.periodMillis, op2.periodMillis);
        if (res6 != 0) {
            return res6;
        }
        int res7 = Long.compare(op1.expectedRuntime, op2.expectedRuntime);
        if (res7 != 0) {
            return res7;
        }
        int res8 = Long.compare(op1.jobId, op2.jobId);
        if (res8 != 0) {
            return res8;
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$static$7(SyncOperation op1, SyncOperation op2) {
        int res = Long.compare(op1.expectedRuntime, op2.expectedRuntime);
        return res != 0 ? res : sOpDumpComparator.compare(op1, op2);
    }

    private static <T> int countIf(Collection<T> col, Predicate<T> p) {
        int ret = 0;
        for (T item : col) {
            if (p.test(item)) {
                ret++;
            }
        }
        return ret;
    }

    protected void dumpPendingSyncs(PrintWriter pw, SyncAdapterStateFetcher buckets) {
        List<SyncOperation> pendingSyncs = getAllPendingSyncs();
        pw.print("Pending Syncs: ");
        pw.println(countIf(pendingSyncs, new Predicate() { // from class: com.android.server.content.SyncManager$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return SyncManager.lambda$dumpPendingSyncs$8((SyncOperation) obj);
            }
        }));
        Collections.sort(pendingSyncs, sOpRuntimeComparator);
        int count = 0;
        for (SyncOperation op : pendingSyncs) {
            if (!op.isPeriodic) {
                pw.println(op.dump(null, false, buckets, false));
                count++;
            }
        }
        pw.println();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$dumpPendingSyncs$8(SyncOperation op) {
        return !op.isPeriodic;
    }

    protected void dumpPeriodicSyncs(PrintWriter pw, SyncAdapterStateFetcher buckets) {
        List<SyncOperation> pendingSyncs = getAllPendingSyncs();
        pw.print("Periodic Syncs: ");
        pw.println(countIf(pendingSyncs, new Predicate() { // from class: com.android.server.content.SyncManager$$ExternalSyntheticLambda12
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean z;
                z = ((SyncOperation) obj).isPeriodic;
                return z;
            }
        }));
        Collections.sort(pendingSyncs, sOpDumpComparator);
        int count = 0;
        for (SyncOperation op : pendingSyncs) {
            if (op.isPeriodic) {
                pw.println(op.dump(null, false, buckets, false));
                count++;
            }
        }
        pw.println();
    }

    public static StringBuilder formatDurationHMS(StringBuilder sb, long duration) {
        long duration2 = duration / 1000;
        if (duration2 < 0) {
            sb.append('-');
            duration2 = -duration2;
        }
        long seconds = duration2 % 60;
        long duration3 = duration2 / 60;
        long minutes = duration3 % 60;
        long duration4 = duration3 / 60;
        long hours = duration4 % 24;
        long duration5 = duration4 / 24;
        boolean print = false;
        if (duration5 > 0) {
            sb.append(duration5);
            sb.append('d');
            print = true;
        }
        if (!printTwoDigitNumber(sb, seconds, 's', printTwoDigitNumber(sb, minutes, 'm', printTwoDigitNumber(sb, hours, 'h', print)))) {
            sb.append("0s");
        }
        return sb;
    }

    private static boolean printTwoDigitNumber(StringBuilder sb, long value, char unit, boolean always) {
        if (!always && value == 0) {
            return false;
        }
        if (always && value < 10) {
            sb.append('0');
        }
        sb.append(value);
        sb.append(unit);
        return true;
    }

    /* JADX DEBUG: Incorrect finally slice size: {[MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE] complete}, expected: {[MOVE, MOVE, MOVE, MOVE, MOVE, MOVE] complete} */
    /* JADX WARN: Finally extract failed */
    @NeverCompile
    protected void dumpSyncState(PrintWriter pw, SyncAdapterStateFetcher buckets) {
        int i;
        boolean unlocked;
        PrintTable table;
        ArrayList<Pair<SyncStorageEngine.EndPoint, SyncStatusInfo>> statuses;
        int i2;
        final SyncManager syncManager = this;
        final StringBuilder sb = new StringBuilder();
        pw.print("Data connected: ");
        pw.println(syncManager.mDataConnectionIsConnected);
        pw.print("Battery saver: ");
        PowerManager powerManager = syncManager.mPowerManager;
        pw.println(powerManager != null && powerManager.isPowerSaveMode());
        pw.print("Background network restriction: ");
        ConnectivityManager cm = getConnectivityManager();
        int status = cm == null ? -1 : cm.getRestrictBackgroundStatus();
        switch (status) {
            case 1:
                pw.println(" disabled");
                break;
            case 2:
                pw.println(" whitelisted");
                break;
            case 3:
                pw.println(" enabled");
                break;
            default:
                pw.print("Unknown(");
                pw.print(status);
                pw.println(")");
                break;
        }
        pw.print("Auto sync: ");
        List<UserInfo> users = getAllUsers();
        if (users != null) {
            for (UserInfo user : users) {
                pw.print("u" + user.id + "=" + syncManager.mSyncStorageEngine.getMasterSyncAutomatically(user.id) + " ");
            }
            pw.println();
        }
        Intent storageLowIntent = syncManager.mContext.registerReceiver(null, new IntentFilter("android.intent.action.DEVICE_STORAGE_LOW"));
        pw.print("Storage low: ");
        pw.println(storageLowIntent != null);
        pw.print("Clock valid: ");
        pw.println(syncManager.mSyncStorageEngine.isClockValid());
        AccountAndUser[] accounts = AccountManagerService.getSingleton().getAllAccountsForSystemProcess();
        pw.print("Accounts: ");
        if (accounts != INITIAL_ACCOUNTS_ARRAY) {
            pw.println(accounts.length);
        } else {
            pw.println("not known yet");
        }
        long now = SystemClock.elapsedRealtime();
        pw.print("Now: ");
        pw.print(now);
        pw.println(" (" + formatTime(System.currentTimeMillis()) + ")");
        sb.setLength(0);
        pw.print("Uptime: ");
        pw.print(formatDurationHMS(sb, now));
        pw.println();
        pw.print("Time spent syncing: ");
        sb.setLength(0);
        pw.print(formatDurationHMS(sb, syncManager.mSyncHandler.mSyncTimeTracker.timeSpentSyncing()));
        pw.print(", sync ");
        pw.print(syncManager.mSyncHandler.mSyncTimeTracker.mLastWasSyncing ? "" : "not ");
        pw.println("in progress");
        pw.println();
        pw.println("Active Syncs: " + syncManager.mActiveSyncContexts.size());
        PackageManager pm = syncManager.mContext.getPackageManager();
        Iterator<ActiveSyncContext> it = syncManager.mActiveSyncContexts.iterator();
        while (it.hasNext()) {
            ActiveSyncContext activeSyncContext = it.next();
            long durationInSeconds = now - activeSyncContext.mStartTime;
            pw.print("  ");
            sb.setLength(0);
            pw.print(formatDurationHMS(sb, durationInSeconds));
            pw.print(" - ");
            pw.print(activeSyncContext.mSyncOperation.dump(pm, false, buckets, false));
            pw.println();
        }
        pw.println();
        dumpPendingSyncs(pw, buckets);
        dumpPeriodicSyncs(pw, buckets);
        pw.println("Sync Status");
        ArrayList<Pair<SyncStorageEngine.EndPoint, SyncStatusInfo>> statuses2 = new ArrayList<>();
        syncManager.mSyncStorageEngine.resetTodayStats(false);
        int length = accounts.length;
        int i3 = 0;
        while (i3 < length) {
            AccountAndUser account = accounts[i3];
            synchronized (syncManager.mUnlockedUsers) {
                try {
                    i = length;
                    unlocked = syncManager.mUnlockedUsers.get(account.userId);
                } catch (Throwable th) {
                    th = th;
                    while (true) {
                        try {
                            throw th;
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                }
            }
            Object[] objArr = new Object[4];
            objArr[0] = account.account.name;
            objArr[1] = Integer.valueOf(account.userId);
            objArr[2] = account.account.type;
            objArr[3] = unlocked ? "" : " (locked)";
            pw.printf("Account %s u%d %s%s\n", objArr);
            pw.println("=======================================================================");
            final PrintTable table2 = new PrintTable(16);
            table2.set(0, 0, "Authority", "Syncable", "Enabled", "Stats", "Loc", "Poll", "Per", "Feed", "User", "Othr", "Tot", "Fail", "Can", "Time", "Last Sync", "Backoff");
            List<RegisteredServicesCache.ServiceInfo<SyncAdapterType>> sorted = Lists.newArrayList();
            sorted.addAll(syncManager.mSyncAdapters.getAllServices(account.userId));
            Collections.sort(sorted, new Comparator<RegisteredServicesCache.ServiceInfo<SyncAdapterType>>() { // from class: com.android.server.content.SyncManager.12
                /* JADX DEBUG: Method merged with bridge method */
                @Override // java.util.Comparator
                public int compare(RegisteredServicesCache.ServiceInfo<SyncAdapterType> lhs, RegisteredServicesCache.ServiceInfo<SyncAdapterType> rhs) {
                    return ((SyncAdapterType) lhs.type).authority.compareTo(((SyncAdapterType) rhs.type).authority);
                }
            });
            Iterator<RegisteredServicesCache.ServiceInfo<SyncAdapterType>> it2 = sorted.iterator();
            while (it2.hasNext()) {
                RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapterType = it2.next();
                Iterator<RegisteredServicesCache.ServiceInfo<SyncAdapterType>> it3 = it2;
                List<RegisteredServicesCache.ServiceInfo<SyncAdapterType>> sorted2 = sorted;
                if (!((SyncAdapterType) syncAdapterType.type).accountType.equals(account.account.type)) {
                    it2 = it3;
                    sorted = sorted2;
                } else {
                    int row = table2.getNumRows();
                    List<UserInfo> users2 = users;
                    Intent storageLowIntent2 = storageLowIntent;
                    AccountAndUser[] accounts2 = accounts;
                    PackageManager pm2 = pm;
                    Pair<SyncStorageEngine.AuthorityInfo, SyncStatusInfo> syncAuthoritySyncStatus = syncManager.mSyncStorageEngine.getCopyOfAuthorityWithSyncStatus(new SyncStorageEngine.EndPoint(account.account, ((SyncAdapterType) syncAdapterType.type).authority, account.userId));
                    SyncStorageEngine.AuthorityInfo settings = (SyncStorageEngine.AuthorityInfo) syncAuthoritySyncStatus.first;
                    SyncStatusInfo status2 = (SyncStatusInfo) syncAuthoritySyncStatus.second;
                    statuses2.add(Pair.create(settings.target, status2));
                    String authority = settings.target.provider;
                    if (authority.length() > 50) {
                        authority = authority.substring(authority.length() - 50);
                    }
                    table2.set(row, 0, authority, Integer.valueOf(settings.syncable), Boolean.valueOf(settings.enabled));
                    QuadConsumer<String, SyncStatusInfo.Stats, Function<Integer, String>, Integer> c = new QuadConsumer() { // from class: com.android.server.content.SyncManager$$ExternalSyntheticLambda4
                        public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
                            SyncManager.lambda$dumpSyncState$10(sb, table2, (String) obj, (SyncStatusInfo.Stats) obj2, (Function) obj3, (Integer) obj4);
                        }
                    };
                    StringBuilder sb2 = sb;
                    AccountAndUser account2 = account;
                    c.accept("Total", status2.totalStats, new Function() { // from class: com.android.server.content.SyncManager$$ExternalSyntheticLambda5
                        @Override // java.util.function.Function
                        public final Object apply(Object obj) {
                            String num;
                            num = Integer.toString(((Integer) obj).intValue());
                            return num;
                        }
                    }, Integer.valueOf(row));
                    c.accept("Today", status2.todayStats, new Function() { // from class: com.android.server.content.SyncManager$$ExternalSyntheticLambda6
                        @Override // java.util.function.Function
                        public final Object apply(Object obj) {
                            String zeroToEmpty;
                            zeroToEmpty = SyncManager.this.zeroToEmpty(((Integer) obj).intValue());
                            return zeroToEmpty;
                        }
                    }, Integer.valueOf(row + 1));
                    c.accept("Yestr", status2.yesterdayStats, new Function() { // from class: com.android.server.content.SyncManager$$ExternalSyntheticLambda6
                        @Override // java.util.function.Function
                        public final Object apply(Object obj) {
                            String zeroToEmpty;
                            zeroToEmpty = SyncManager.this.zeroToEmpty(((Integer) obj).intValue());
                            return zeroToEmpty;
                        }
                    }, Integer.valueOf(row + 2));
                    if (settings.delayUntil > now) {
                        int row1 = row + 1;
                        statuses = statuses2;
                        i2 = i3;
                        table2.set(row, 15, "D: " + ((settings.delayUntil - now) / 1000));
                        if (settings.backoffTime <= now) {
                            table = table2;
                        } else {
                            int row12 = row1 + 1;
                            table2.set(row1, 15, "B: " + ((settings.backoffTime - now) / 1000));
                            int i4 = row12 + 1;
                            table = table2;
                            table.set(row12, 15, Long.valueOf(settings.backoffDelay / 1000));
                        }
                    } else {
                        table = table2;
                        statuses = statuses2;
                        i2 = i3;
                    }
                    int row13 = row;
                    if (status2.lastSuccessTime != 0) {
                        int row14 = row13 + 1;
                        table.set(row13, 14, SyncStorageEngine.SOURCES[status2.lastSuccessSource] + " SUCCESS");
                        row13 = row14 + 1;
                        table.set(row14, 14, formatTime(status2.lastSuccessTime));
                    }
                    if (status2.lastFailureTime != 0) {
                        int row15 = row13 + 1;
                        table.set(row13, 14, SyncStorageEngine.SOURCES[status2.lastFailureSource] + " FAILURE");
                        int row16 = row15 + 1;
                        table.set(row15, 14, formatTime(status2.lastFailureTime));
                        int i5 = row16 + 1;
                        table.set(row16, 14, status2.lastFailureMesg);
                    }
                    syncManager = this;
                    table2 = table;
                    it2 = it3;
                    sorted = sorted2;
                    users = users2;
                    storageLowIntent = storageLowIntent2;
                    accounts = accounts2;
                    pm = pm2;
                    sb = sb2;
                    account = account2;
                    statuses2 = statuses;
                    i3 = i2;
                }
            }
            table2.writeTo(pw);
            i3++;
            syncManager = this;
            length = i;
            sb = sb;
        }
        ArrayList<Pair<SyncStorageEngine.EndPoint, SyncStatusInfo>> statuses3 = statuses2;
        dumpSyncHistory(pw);
        pw.println();
        pw.println("Per Adapter History");
        pw.println("(SERVER is now split up to FEED and OTHER)");
        int i6 = 0;
        while (i6 < statuses3.size()) {
            ArrayList<Pair<SyncStorageEngine.EndPoint, SyncStatusInfo>> statuses4 = statuses3;
            Pair<SyncStorageEngine.EndPoint, SyncStatusInfo> event = statuses4.get(i6);
            pw.print("  ");
            pw.print(((SyncStorageEngine.EndPoint) event.first).account.name);
            pw.print('/');
            pw.print(((SyncStorageEngine.EndPoint) event.first).account.type);
            pw.print(" u");
            pw.print(((SyncStorageEngine.EndPoint) event.first).userId);
            pw.print(" [");
            pw.print(((SyncStorageEngine.EndPoint) event.first).provider);
            pw.print("]");
            pw.println();
            pw.println("    Per source last syncs:");
            for (int j = 0; j < SyncStorageEngine.SOURCES.length; j++) {
                pw.print("      ");
                pw.print(String.format("%8s", SyncStorageEngine.SOURCES[j]));
                pw.print("  Success: ");
                pw.print(formatTime(((SyncStatusInfo) event.second).perSourceLastSuccessTimes[j]));
                pw.print("  Failure: ");
                pw.println(formatTime(((SyncStatusInfo) event.second).perSourceLastFailureTimes[j]));
            }
            pw.println("    Last syncs:");
            for (int j2 = 0; j2 < ((SyncStatusInfo) event.second).getEventCount(); j2++) {
                pw.print("      ");
                pw.print(formatTime(((SyncStatusInfo) event.second).getEventTime(j2)));
                pw.print(' ');
                pw.print(((SyncStatusInfo) event.second).getEvent(j2));
                pw.println();
            }
            if (((SyncStatusInfo) event.second).getEventCount() == 0) {
                pw.println("      N/A");
            }
            i6++;
            statuses3 = statuses4;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dumpSyncState$10(StringBuilder sb, PrintTable table, String label, SyncStatusInfo.Stats stats, Function filter, Integer r) {
        sb.setLength(0);
        table.set(r.intValue(), 3, label, filter.apply(Integer.valueOf(stats.numSourceLocal)), filter.apply(Integer.valueOf(stats.numSourcePoll)), filter.apply(Integer.valueOf(stats.numSourcePeriodic)), filter.apply(Integer.valueOf(stats.numSourceFeed)), filter.apply(Integer.valueOf(stats.numSourceUser)), filter.apply(Integer.valueOf(stats.numSourceOther)), filter.apply(Integer.valueOf(stats.numSyncs)), filter.apply(Integer.valueOf(stats.numFailures)), filter.apply(Integer.valueOf(stats.numCancels)), formatDurationHMS(sb, stats.totalElapsedTime));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String zeroToEmpty(int value) {
        return value != 0 ? Integer.toString(value) : "";
    }

    private void dumpTimeSec(PrintWriter pw, long time) {
        pw.print(time / 1000);
        pw.print('.');
        pw.print((time / 100) % 10);
        pw.print('s');
    }

    private void dumpDayStatistic(PrintWriter pw, SyncStorageEngine.DayStats ds) {
        pw.print("Success (");
        pw.print(ds.successCount);
        if (ds.successCount > 0) {
            pw.print(" for ");
            dumpTimeSec(pw, ds.successTime);
            pw.print(" avg=");
            dumpTimeSec(pw, ds.successTime / ds.successCount);
        }
        pw.print(") Failure (");
        pw.print(ds.failureCount);
        if (ds.failureCount > 0) {
            pw.print(" for ");
            dumpTimeSec(pw, ds.failureTime);
            pw.print(" avg=");
            dumpTimeSec(pw, ds.failureTime / ds.failureCount);
        }
        pw.println(")");
    }

    protected void dumpSyncHistory(PrintWriter pw) {
        dumpRecentHistory(pw);
        dumpDayStatistics(pw);
    }

    private void dumpRecentHistory(PrintWriter pw) {
        String str;
        int N;
        int maxAuthority;
        int maxAccount;
        String str2;
        String str3;
        int N2;
        Map<String, Long> lastTimeMap;
        ArrayList<SyncStorageEngine.SyncHistoryItem> items;
        String str4;
        String str5;
        int N3;
        String authorityName;
        String accountKey;
        long totalTimes;
        int maxAccount2;
        int maxAuthority2;
        String str6;
        String diffString;
        String authorityName2;
        String accountKey2;
        String str7;
        int N4;
        Object format;
        Map<String, Long> lastTimeMap2;
        PackageManager pm;
        PackageManager pm2;
        ArrayList<SyncStorageEngine.SyncHistoryItem> items2;
        String authorityName3;
        String accountKey3;
        AuthoritySyncStats authoritySyncStats;
        SyncManager syncManager = this;
        ArrayList<SyncStorageEngine.SyncHistoryItem> items3 = syncManager.mSyncStorageEngine.getSyncHistory();
        if (items3 != null && items3.size() > 0) {
            Map<String, AuthoritySyncStats> authorityMap = Maps.newHashMap();
            long totalElapsedTime = 0;
            long totalTimes2 = 0;
            int N5 = items3.size();
            int maxAuthority3 = 0;
            int maxAccount3 = 0;
            Iterator<SyncStorageEngine.SyncHistoryItem> it = items3.iterator();
            while (true) {
                boolean hasNext = it.hasNext();
                str = SliceClientPermissions.SliceAuthority.DELIMITER;
                if (!hasNext) {
                    break;
                }
                SyncStorageEngine.SyncHistoryItem item = it.next();
                Iterator<SyncStorageEngine.SyncHistoryItem> it2 = it;
                SyncStorageEngine.AuthorityInfo authorityInfo = syncManager.mSyncStorageEngine.getAuthority(item.authorityId);
                if (authorityInfo != null) {
                    String authorityName4 = authorityInfo.target.provider;
                    items2 = items3;
                    accountKey3 = authorityInfo.target.account.name + SliceClientPermissions.SliceAuthority.DELIMITER + authorityInfo.target.account.type + " u" + authorityInfo.target.userId;
                    authorityName3 = authorityName4;
                } else {
                    items2 = items3;
                    authorityName3 = "Unknown";
                    accountKey3 = "Unknown";
                }
                int length = authorityName3.length();
                if (length > maxAuthority3) {
                    maxAuthority3 = length;
                }
                int length2 = accountKey3.length();
                if (length2 > maxAccount3) {
                    maxAccount3 = length2;
                }
                int maxAuthority4 = maxAuthority3;
                int maxAccount4 = maxAccount3;
                long elapsedTime = item.elapsedTime;
                long totalElapsedTime2 = totalElapsedTime + elapsedTime;
                long totalTimes3 = totalTimes2 + 1;
                AuthoritySyncStats authoritySyncStats2 = authorityMap.get(authorityName3);
                if (authoritySyncStats2 != null) {
                    authoritySyncStats = authoritySyncStats2;
                } else {
                    authoritySyncStats = new AuthoritySyncStats(authorityName3);
                    authorityMap.put(authorityName3, authoritySyncStats);
                }
                long totalTimes4 = authoritySyncStats.elapsedTime;
                authoritySyncStats.elapsedTime = totalTimes4 + elapsedTime;
                authoritySyncStats.times++;
                Map<String, AccountSyncStats> accountMap = authoritySyncStats.accountMap;
                AccountSyncStats accountSyncStats = accountMap.get(accountKey3);
                if (accountSyncStats == null) {
                    accountSyncStats = new AccountSyncStats(accountKey3);
                    accountMap.put(accountKey3, accountSyncStats);
                }
                accountSyncStats.elapsedTime += elapsedTime;
                accountSyncStats.times++;
                maxAuthority3 = maxAuthority4;
                it = it2;
                maxAccount3 = maxAccount4;
                items3 = items2;
                totalElapsedTime = totalElapsedTime2;
                totalTimes2 = totalTimes3;
            }
            ArrayList<SyncStorageEngine.SyncHistoryItem> items4 = items3;
            if (totalElapsedTime > 0) {
                pw.println();
                pw.printf("Detailed Statistics (Recent history):  %d (# of times) %ds (sync time)\n", Long.valueOf(totalTimes2), Long.valueOf(totalElapsedTime / 1000));
                List<AuthoritySyncStats> sortedAuthorities = new ArrayList<>(authorityMap.values());
                Collections.sort(sortedAuthorities, new Comparator<AuthoritySyncStats>() { // from class: com.android.server.content.SyncManager.13
                    /* JADX DEBUG: Method merged with bridge method */
                    @Override // java.util.Comparator
                    public int compare(AuthoritySyncStats lhs, AuthoritySyncStats rhs) {
                        int compare = Integer.compare(rhs.times, lhs.times);
                        if (compare == 0) {
                            return Long.compare(rhs.elapsedTime, lhs.elapsedTime);
                        }
                        return compare;
                    }
                });
                int maxLength = Math.max(maxAuthority3, maxAccount3 + 3);
                int padLength = maxLength + 4 + 2 + 10 + 11;
                char[] chars = new char[padLength];
                Arrays.fill(chars, '-');
                String separator = new String(chars);
                String timeStr = String.format("  %%-%ds: %%-9s  %%-11s\n", Integer.valueOf(maxLength + 2));
                str2 = " u";
                String accountFormat = String.format("    %%-%ds:   %%-9s  %%-11s\n", Integer.valueOf(maxLength));
                pw.println(separator);
                Iterator<AuthoritySyncStats> it3 = sortedAuthorities.iterator();
                while (it3.hasNext()) {
                    List<AuthoritySyncStats> sortedAuthorities2 = sortedAuthorities;
                    AuthoritySyncStats authoritySyncStats3 = it3.next();
                    Iterator<AuthoritySyncStats> it4 = it3;
                    String name = authoritySyncStats3.name;
                    String str8 = str;
                    int maxLength2 = maxLength;
                    long elapsedTime2 = authoritySyncStats3.elapsedTime;
                    int N6 = N5;
                    int N7 = authoritySyncStats3.times;
                    int maxAuthority5 = maxAuthority3;
                    int maxAccount5 = maxAccount3;
                    String timeStr2 = String.format("%ds/%d%%", Long.valueOf(elapsedTime2 / 1000), Long.valueOf((elapsedTime2 * 100) / totalElapsedTime));
                    String timesStr = String.format("%d/%d%%", Integer.valueOf(N7), Long.valueOf((N7 * 100) / totalTimes2));
                    String separator2 = separator;
                    pw.printf(timeStr, name, timesStr, timeStr2);
                    List<AccountSyncStats> sortedAccounts = new ArrayList<>(authoritySyncStats3.accountMap.values());
                    Collections.sort(sortedAccounts, new Comparator<AccountSyncStats>() { // from class: com.android.server.content.SyncManager.14
                        /* JADX DEBUG: Method merged with bridge method */
                        @Override // java.util.Comparator
                        public int compare(AccountSyncStats lhs, AccountSyncStats rhs) {
                            int compare = Integer.compare(rhs.times, lhs.times);
                            if (compare == 0) {
                                return Long.compare(rhs.elapsedTime, lhs.elapsedTime);
                            }
                            return compare;
                        }
                    });
                    Iterator<AccountSyncStats> it5 = sortedAccounts.iterator();
                    while (it5.hasNext()) {
                        AccountSyncStats stats = it5.next();
                        AuthoritySyncStats authoritySyncStats4 = authoritySyncStats3;
                        long elapsedTime3 = stats.elapsedTime;
                        Iterator<AccountSyncStats> it6 = it5;
                        int times = stats.times;
                        String authorityFormat = timeStr;
                        String timeStr3 = String.format("%ds/%d%%", Long.valueOf(elapsedTime3 / 1000), Long.valueOf((elapsedTime3 * 100) / totalElapsedTime));
                        long totalElapsedTime3 = totalElapsedTime;
                        long totalElapsedTime4 = times * 100;
                        String timesStr2 = String.format("%d/%d%%", Integer.valueOf(times), Long.valueOf(totalElapsedTime4 / totalTimes2));
                        pw.printf(accountFormat, stats.name, timesStr2, timeStr3);
                        timeStr2 = timeStr3;
                        authoritySyncStats3 = authoritySyncStats4;
                        sortedAccounts = sortedAccounts;
                        it5 = it6;
                        timeStr = authorityFormat;
                        totalElapsedTime = totalElapsedTime3;
                    }
                    pw.println(separator2);
                    separator = separator2;
                    it3 = it4;
                    sortedAuthorities = sortedAuthorities2;
                    str = str8;
                    maxLength = maxLength2;
                    N5 = N6;
                    maxAuthority3 = maxAuthority5;
                    maxAccount3 = maxAccount5;
                }
                N = N5;
                maxAuthority = maxAuthority3;
                maxAccount = maxAccount3;
                str3 = str;
            } else {
                N = N5;
                maxAuthority = maxAuthority3;
                maxAccount = maxAccount3;
                str2 = " u";
                str3 = SliceClientPermissions.SliceAuthority.DELIMITER;
            }
            pw.println();
            pw.println("Recent Sync History");
            pw.println("(SERVER is now split up to FEED and OTHER)");
            int maxAccount6 = maxAccount;
            int maxAuthority6 = maxAuthority;
            String format2 = "  %-" + maxAccount6 + "s  %-" + maxAuthority6 + "s %s\n";
            Map<String, Long> lastTimeMap3 = Maps.newHashMap();
            PackageManager pm3 = syncManager.mContext.getPackageManager();
            int i = 0;
            while (true) {
                N2 = N;
                if (i >= N2) {
                    break;
                }
                ArrayList<SyncStorageEngine.SyncHistoryItem> items5 = items4;
                SyncStorageEngine.SyncHistoryItem item2 = items5.get(i);
                SyncStorageEngine.AuthorityInfo authorityInfo2 = syncManager.mSyncStorageEngine.getAuthority(item2.authorityId);
                if (authorityInfo2 != null) {
                    authorityName2 = authorityInfo2.target.provider;
                    totalTimes = totalTimes2;
                    diffString = str3;
                    maxAccount2 = maxAccount6;
                    str6 = str2;
                    maxAuthority2 = maxAuthority6;
                    accountKey2 = authorityInfo2.target.account.name + diffString + authorityInfo2.target.account.type + str6 + authorityInfo2.target.userId;
                } else {
                    totalTimes = totalTimes2;
                    maxAccount2 = maxAccount6;
                    maxAuthority2 = maxAuthority6;
                    str6 = str2;
                    diffString = str3;
                    authorityName2 = "Unknown";
                    accountKey2 = "Unknown";
                }
                long elapsedTime4 = item2.elapsedTime;
                String str9 = str6;
                long eventTime = item2.eventTime;
                String key = authorityName2 + diffString + accountKey2;
                Long lastEventTime = lastTimeMap3.get(key);
                if (lastEventTime == null) {
                    format = "";
                    str7 = diffString;
                    N4 = N2;
                } else {
                    long diff = (lastEventTime.longValue() - eventTime) / 1000;
                    if (diff < 60) {
                        N4 = N2;
                        format = String.valueOf(diff);
                        str7 = diffString;
                    } else if (diff < 3600) {
                        str7 = diffString;
                        N4 = N2;
                        format = String.format("%02d:%02d", Long.valueOf(diff / 60), Long.valueOf(diff % 60));
                    } else {
                        str7 = diffString;
                        N4 = N2;
                        long sec = diff % 3600;
                        format = String.format("%02d:%02d:%02d", Long.valueOf(diff / 3600), Long.valueOf(sec / 60), Long.valueOf(sec % 60));
                    }
                }
                lastTimeMap3.put(key, Long.valueOf(eventTime));
                pw.printf("  #%-3d: %s %8s  %5.1fs  %8s", Integer.valueOf(i + 1), formatTime(eventTime), SyncStorageEngine.SOURCES[item2.source], Float.valueOf(((float) elapsedTime4) / 1000.0f), format);
                pw.printf(format2, accountKey2, authorityName2, SyncOperation.reasonToString(pm3, item2.reason));
                if (item2.event == 1) {
                    lastTimeMap2 = lastTimeMap3;
                    pm = pm3;
                    if (item2.upstreamActivity == 0 && item2.downstreamActivity == 0) {
                        pm2 = pm;
                        if (item2.mesg != null && !SyncStorageEngine.MESG_SUCCESS.equals(item2.mesg)) {
                            pw.printf("    mesg=%s\n", item2.mesg);
                        }
                        i++;
                        lastTimeMap3 = lastTimeMap2;
                        pm3 = pm2;
                        totalTimes2 = totalTimes;
                        items4 = items5;
                        str2 = str9;
                        str3 = str7;
                        maxAuthority6 = maxAuthority2;
                        maxAccount6 = maxAccount2;
                        N = N4;
                        syncManager = this;
                    }
                } else {
                    lastTimeMap2 = lastTimeMap3;
                    pm = pm3;
                }
                pm2 = pm;
                pw.printf("    event=%d upstreamActivity=%d downstreamActivity=%d\n", Integer.valueOf(item2.event), Long.valueOf(item2.upstreamActivity), Long.valueOf(item2.downstreamActivity));
                if (item2.mesg != null) {
                    pw.printf("    mesg=%s\n", item2.mesg);
                }
                i++;
                lastTimeMap3 = lastTimeMap2;
                pm3 = pm2;
                totalTimes2 = totalTimes;
                items4 = items5;
                str2 = str9;
                str3 = str7;
                maxAuthority6 = maxAuthority2;
                maxAccount6 = maxAccount2;
                N = N4;
                syncManager = this;
            }
            Map<String, Long> lastTimeMap4 = lastTimeMap3;
            int N8 = N2;
            String str10 = str2;
            String str11 = str3;
            ArrayList<SyncStorageEngine.SyncHistoryItem> items6 = items4;
            pw.println();
            pw.println("Recent Sync History Extras");
            pw.println("(SERVER is now split up to FEED and OTHER)");
            int i2 = 0;
            while (true) {
                int N9 = N8;
                if (i2 < N9) {
                    ArrayList<SyncStorageEngine.SyncHistoryItem> items7 = items6;
                    SyncStorageEngine.SyncHistoryItem item3 = items7.get(i2);
                    Bundle extras = item3.extras;
                    if (extras == null) {
                        lastTimeMap = lastTimeMap4;
                        items = items7;
                        str4 = str10;
                        str5 = str11;
                        N3 = N9;
                    } else if (extras.size() == 0) {
                        lastTimeMap = lastTimeMap4;
                        items = items7;
                        str4 = str10;
                        str5 = str11;
                        N3 = N9;
                    } else {
                        SyncStorageEngine.AuthorityInfo authorityInfo3 = this.mSyncStorageEngine.getAuthority(item3.authorityId);
                        if (authorityInfo3 != null) {
                            authorityName = authorityInfo3.target.provider;
                            str5 = str11;
                            str4 = str10;
                            accountKey = authorityInfo3.target.account.name + str5 + authorityInfo3.target.account.type + str4 + authorityInfo3.target.userId;
                        } else {
                            str4 = str10;
                            str5 = str11;
                            authorityName = "Unknown";
                            accountKey = "Unknown";
                        }
                        N3 = N9;
                        items = items7;
                        lastTimeMap = lastTimeMap4;
                        pw.printf("  #%-3d: %s %8s ", Integer.valueOf(i2 + 1), formatTime(item3.eventTime), SyncStorageEngine.SOURCES[item3.source]);
                        pw.printf(format2, accountKey, authorityName, extras);
                    }
                    i2++;
                    str10 = str4;
                    lastTimeMap4 = lastTimeMap;
                    items6 = items;
                    N8 = N3;
                    str11 = str5;
                } else {
                    return;
                }
            }
        }
    }

    private void dumpDayStatistics(PrintWriter pw) {
        SyncStorageEngine.DayStats ds;
        int delta;
        SyncStorageEngine.DayStats[] dses = this.mSyncStorageEngine.getDayStatistics();
        if (dses != null && dses[0] != null) {
            pw.println();
            pw.println("Sync Statistics");
            pw.print("  Today:  ");
            dumpDayStatistic(pw, dses[0]);
            int today = dses[0].day;
            int i = 1;
            while (i <= 6 && i < dses.length && (ds = dses[i]) != null && (delta = today - ds.day) <= 6) {
                pw.print("  Day-");
                pw.print(delta);
                pw.print(":  ");
                dumpDayStatistic(pw, ds);
                i++;
            }
            int weekDay = today;
            while (i < dses.length) {
                SyncStorageEngine.DayStats aggr = null;
                weekDay -= 7;
                while (true) {
                    if (i >= dses.length) {
                        break;
                    }
                    SyncStorageEngine.DayStats ds2 = dses[i];
                    if (ds2 == null) {
                        i = dses.length;
                        break;
                    } else if (weekDay - ds2.day > 6) {
                        break;
                    } else {
                        i++;
                        if (aggr == null) {
                            aggr = new SyncStorageEngine.DayStats(weekDay);
                        }
                        aggr.successCount += ds2.successCount;
                        aggr.successTime += ds2.successTime;
                        aggr.failureCount += ds2.failureCount;
                        aggr.failureTime += ds2.failureTime;
                    }
                }
                if (aggr != null) {
                    pw.print("  Week-");
                    pw.print((today - weekDay) / 7);
                    pw.print(": ");
                    dumpDayStatistic(pw, aggr);
                }
            }
        }
    }

    private void dumpSyncAdapters(IndentingPrintWriter pw) {
        pw.println();
        List<UserInfo> users = getAllUsers();
        if (users != null) {
            for (UserInfo user : users) {
                pw.println("Sync adapters for " + user + ":");
                pw.increaseIndent();
                for (RegisteredServicesCache.ServiceInfo<?> info : this.mSyncAdapters.getAllServices(user.id)) {
                    pw.println(info);
                }
                pw.decreaseIndent();
                pw.println();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class AuthoritySyncStats {
        Map<String, AccountSyncStats> accountMap;
        long elapsedTime;
        String name;
        int times;

        private AuthoritySyncStats(String name) {
            this.accountMap = Maps.newHashMap();
            this.name = name;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class AccountSyncStats {
        long elapsedTime;
        String name;
        int times;

        private AccountSyncStats(String name) {
            this.name = name;
        }
    }

    static void sendOnUnsyncableAccount(final Context context, RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapterInfo, int userId, OnReadyCallback onReadyCallback) {
        final OnUnsyncableAccountCheck connection = new OnUnsyncableAccountCheck(syncAdapterInfo, onReadyCallback);
        boolean isBound = context.bindServiceAsUser(getAdapterBindIntent(context, syncAdapterInfo.componentName, userId), connection, 21, UserHandle.of(userId));
        if (isBound) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() { // from class: com.android.server.content.SyncManager$$ExternalSyntheticLambda11
                @Override // java.lang.Runnable
                public final void run() {
                    context.unbindService(connection);
                }
            }, 5000L);
        } else {
            connection.onReady();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class OnUnsyncableAccountCheck implements ServiceConnection {
        static final long SERVICE_BOUND_TIME_MILLIS = 5000;
        private final OnReadyCallback mOnReadyCallback;
        private final RegisteredServicesCache.ServiceInfo<SyncAdapterType> mSyncAdapterInfo;

        OnUnsyncableAccountCheck(RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapterInfo, OnReadyCallback onReadyCallback) {
            this.mSyncAdapterInfo = syncAdapterInfo;
            this.mOnReadyCallback = onReadyCallback;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onReady() {
            long identity = Binder.clearCallingIdentity();
            try {
                this.mOnReadyCallback.onReady();
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            ISyncAdapter adapter = ISyncAdapter.Stub.asInterface(service);
            try {
                adapter.onUnsyncableAccount(new ISyncAdapterUnsyncableAccountCallback.Stub() { // from class: com.android.server.content.SyncManager.OnUnsyncableAccountCheck.1
                    public void onUnsyncableAccountDone(boolean isReady) {
                        if (isReady) {
                            OnUnsyncableAccountCheck.this.onReady();
                        }
                    }
                });
            } catch (RemoteException e) {
                Slog.e("SyncManager", "Could not call onUnsyncableAccountDone " + this.mSyncAdapterInfo, e);
                onReady();
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SyncTimeTracker {
        boolean mLastWasSyncing;
        private long mTimeSpentSyncing;
        long mWhenSyncStarted;

        private SyncTimeTracker() {
            this.mLastWasSyncing = false;
            this.mWhenSyncStarted = 0L;
        }

        public synchronized void update() {
            boolean isSyncInProgress = !SyncManager.this.mActiveSyncContexts.isEmpty();
            if (isSyncInProgress == this.mLastWasSyncing) {
                return;
            }
            long now = SystemClock.elapsedRealtime();
            if (isSyncInProgress) {
                this.mWhenSyncStarted = now;
            } else {
                this.mTimeSpentSyncing += now - this.mWhenSyncStarted;
            }
            this.mLastWasSyncing = isSyncInProgress;
        }

        public synchronized long timeSpentSyncing() {
            if (!this.mLastWasSyncing) {
                return this.mTimeSpentSyncing;
            }
            long now = SystemClock.elapsedRealtime();
            return this.mTimeSpentSyncing + (now - this.mWhenSyncStarted);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ServiceConnectionData {
        public final ActiveSyncContext activeSyncContext;
        public final IBinder adapter;

        ServiceConnectionData(ActiveSyncContext activeSyncContext, IBinder adapter) {
            this.activeSyncContext = activeSyncContext;
            this.adapter = adapter;
        }
    }

    private static SyncManager getInstance() {
        SyncManager syncManager;
        synchronized (SyncManager.class) {
            if (sInstance == null) {
                Slog.wtf("SyncManager", "sInstance == null");
            }
            syncManager = sInstance;
        }
        return syncManager;
    }

    public static boolean readyToSync(int userId) {
        SyncManager instance = getInstance();
        return instance != null && SyncJobService.isReady() && instance.mProvisioned && instance.isUserUnlocked(userId);
    }

    public static void sendMessage(Message message) {
        SyncManager instance = getInstance();
        if (instance != null) {
            instance.mSyncHandler.sendMessage(message);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class SyncHandler extends Handler {
        private static final int MESSAGE_ACCOUNTS_UPDATED = 9;
        private static final int MESSAGE_CANCEL = 6;
        private static final int MESSAGE_MONITOR_SYNC = 8;
        static final int MESSAGE_REMOVE_PERIODIC_SYNC = 14;
        static final int MESSAGE_SCHEDULE_SYNC = 12;
        private static final int MESSAGE_SERVICE_CONNECTED = 4;
        private static final int MESSAGE_SERVICE_DISCONNECTED = 5;
        static final int MESSAGE_START_SYNC = 10;
        static final int MESSAGE_STOP_SYNC = 11;
        private static final int MESSAGE_SYNC_FINISHED = 1;
        static final int MESSAGE_UPDATE_PERIODIC_SYNC = 13;
        public final SyncTimeTracker mSyncTimeTracker;
        private final HashMap<String, PowerManager.WakeLock> mWakeLocks;

        public SyncHandler(Looper looper) {
            super(looper);
            this.mSyncTimeTracker = new SyncTimeTracker();
            this.mWakeLocks = Maps.newHashMap();
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            SyncManager.this.mSyncManagerWakeLock.acquire();
            try {
                handleSyncMessage(msg);
            } finally {
                SyncManager.this.mSyncManagerWakeLock.release();
            }
        }

        private void handleSyncMessage(Message msg) {
            boolean isLoggable = Log.isLoggable("SyncManager", 2);
            try {
                SyncManager syncManager = SyncManager.this;
                syncManager.mDataConnectionIsConnected = syncManager.readDataConnectionState();
                boolean applyBackoff = true;
                switch (msg.what) {
                    case 1:
                        SyncFinishedOrCancelledMessagePayload payload = (SyncFinishedOrCancelledMessagePayload) msg.obj;
                        if (!SyncManager.this.isSyncStillActiveH(payload.activeSyncContext)) {
                            if (isLoggable) {
                                Log.d("SyncManager", "handleSyncHandlerMessage: dropping since the sync is no longer active: " + payload.activeSyncContext);
                                break;
                            }
                        } else {
                            if (isLoggable) {
                                Slog.v("SyncManager", "syncFinished" + payload.activeSyncContext.mSyncOperation);
                            }
                            SyncJobService.callJobFinished(payload.activeSyncContext.mSyncOperation.jobId, false, "sync finished");
                            runSyncFinishedOrCanceledH(payload.syncResult, payload.activeSyncContext);
                            break;
                        }
                        break;
                    case 4:
                        ServiceConnectionData msgData = (ServiceConnectionData) msg.obj;
                        if (isLoggable) {
                            Log.d("SyncManager", "handleSyncHandlerMessage: MESSAGE_SERVICE_CONNECTED: " + msgData.activeSyncContext);
                        }
                        if (SyncManager.this.isSyncStillActiveH(msgData.activeSyncContext)) {
                            runBoundToAdapterH(msgData.activeSyncContext, msgData.adapter);
                            break;
                        }
                        break;
                    case 5:
                        ActiveSyncContext currentSyncContext = ((ServiceConnectionData) msg.obj).activeSyncContext;
                        if (isLoggable) {
                            Log.d("SyncManager", "handleSyncHandlerMessage: MESSAGE_SERVICE_DISCONNECTED: " + currentSyncContext);
                        }
                        if (SyncManager.this.isSyncStillActiveH(currentSyncContext)) {
                            try {
                                if (currentSyncContext.mSyncAdapter != null) {
                                    SyncManager.this.mLogger.log("Calling cancelSync for SERVICE_DISCONNECTED ", currentSyncContext, " adapter=", currentSyncContext.mSyncAdapter);
                                    currentSyncContext.mSyncAdapter.cancelSync(currentSyncContext);
                                    SyncManager.this.mLogger.log("Canceled");
                                }
                            } catch (RemoteException e) {
                                SyncManager.this.mLogger.log("RemoteException ", Log.getStackTraceString(e));
                            }
                            SyncResult syncResult = new SyncResult();
                            syncResult.stats.numIoExceptions++;
                            SyncJobService.callJobFinished(currentSyncContext.mSyncOperation.jobId, false, "service disconnected");
                            runSyncFinishedOrCanceledH(syncResult, currentSyncContext);
                            break;
                        }
                        break;
                    case 6:
                        SyncStorageEngine.EndPoint endpoint = (SyncStorageEngine.EndPoint) msg.obj;
                        Bundle extras = msg.peekData();
                        if (isLoggable) {
                            Log.d("SyncManager", "handleSyncHandlerMessage: MESSAGE_CANCEL: " + endpoint + " bundle: " + extras);
                        }
                        cancelActiveSyncH(endpoint, extras, "MESSAGE_CANCEL");
                        break;
                    case 8:
                        ActiveSyncContext monitoredSyncContext = (ActiveSyncContext) msg.obj;
                        if (isLoggable) {
                            Log.d("SyncManager", "handleSyncHandlerMessage: MESSAGE_MONITOR_SYNC: " + monitoredSyncContext.mSyncOperation.target);
                        }
                        if (isSyncNotUsingNetworkH(monitoredSyncContext)) {
                            Log.w("SyncManager", String.format("Detected sync making no progress for %s. cancelling.", SyncLogger.logSafe(monitoredSyncContext)));
                            SyncJobService.callJobFinished(monitoredSyncContext.mSyncOperation.jobId, false, "no network activity");
                            runSyncFinishedOrCanceledH(null, monitoredSyncContext);
                            break;
                        } else {
                            SyncManager.this.postMonitorSyncProgressMessage(monitoredSyncContext);
                            break;
                        }
                    case 9:
                        if (Log.isLoggable("SyncManager", 2)) {
                            Slog.v("SyncManager", "handleSyncHandlerMessage: MESSAGE_ACCOUNTS_UPDATED");
                        }
                        SyncStorageEngine.EndPoint targets = (SyncStorageEngine.EndPoint) msg.obj;
                        updateRunningAccountsH(targets);
                        break;
                    case 10:
                        startSyncH((SyncOperation) msg.obj);
                        break;
                    case 11:
                        SyncOperation op = (SyncOperation) msg.obj;
                        if (isLoggable) {
                            Slog.v("SyncManager", "Stop sync received.");
                        }
                        ActiveSyncContext asc = findActiveSyncContextH(op.jobId);
                        if (asc != null) {
                            runSyncFinishedOrCanceledH(null, asc);
                            boolean reschedule = msg.arg1 != 0;
                            if (msg.arg2 == 0) {
                                applyBackoff = false;
                            }
                            if (isLoggable) {
                                Slog.v("SyncManager", "Stopping sync. Reschedule: " + reschedule + "Backoff: " + applyBackoff);
                            }
                            if (applyBackoff) {
                                SyncManager.this.increaseBackoffSetting(op.target);
                            }
                            if (reschedule) {
                                deferStoppedSyncH(op, 0L);
                            }
                            break;
                        }
                        break;
                    case 12:
                        ScheduleSyncMessagePayload syncPayload = (ScheduleSyncMessagePayload) msg.obj;
                        SyncManager.this.scheduleSyncOperationH(syncPayload.syncOperation, syncPayload.minDelayMillis);
                        break;
                    case 13:
                        UpdatePeriodicSyncMessagePayload data = (UpdatePeriodicSyncMessagePayload) msg.obj;
                        updateOrAddPeriodicSyncH(data.target, data.pollFrequency, data.flex, data.extras);
                        break;
                    case 14:
                        Pair<SyncStorageEngine.EndPoint, String> args = (Pair) msg.obj;
                        removePeriodicSyncH((SyncStorageEngine.EndPoint) args.first, msg.getData(), (String) args.second);
                        break;
                }
            } finally {
                this.mSyncTimeTracker.update();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public PowerManager.WakeLock getSyncWakeLock(SyncOperation operation) {
            String wakeLockKey = operation.wakeLockName();
            PowerManager.WakeLock wakeLock = this.mWakeLocks.get(wakeLockKey);
            if (wakeLock == null) {
                String name = SyncManager.SYNC_WAKE_LOCK_PREFIX + wakeLockKey;
                PowerManager.WakeLock wakeLock2 = SyncManager.this.mPowerManager.newWakeLock(1, name);
                wakeLock2.setReferenceCounted(false);
                this.mWakeLocks.put(wakeLockKey, wakeLock2);
                return wakeLock2;
            }
            return wakeLock;
        }

        private void deferSyncH(SyncOperation op, long delay, String why) {
            SyncLogger syncLogger = SyncManager.this.mLogger;
            Object[] objArr = new Object[8];
            objArr[0] = "deferSyncH() ";
            objArr[1] = op.isPeriodic ? "periodic " : "";
            objArr[2] = "sync.  op=";
            objArr[3] = op;
            objArr[4] = " delay=";
            objArr[5] = Long.valueOf(delay);
            objArr[6] = " why=";
            objArr[7] = why;
            syncLogger.log(objArr);
            SyncJobService.callJobFinished(op.jobId, false, why);
            if (op.isPeriodic) {
                SyncManager.this.scheduleSyncOperationH(op.createOneTimeSyncOperation(), delay);
                return;
            }
            SyncManager.this.cancelJob(op, "deferSyncH()");
            SyncManager.this.scheduleSyncOperationH(op, delay);
        }

        private void deferStoppedSyncH(SyncOperation op, long delay) {
            if (op.isPeriodic) {
                SyncManager.this.scheduleSyncOperationH(op.createOneTimeSyncOperation(), delay);
            } else {
                SyncManager.this.scheduleSyncOperationH(op, delay);
            }
        }

        private void deferActiveSyncH(ActiveSyncContext asc, String why) {
            SyncOperation op = asc.mSyncOperation;
            runSyncFinishedOrCanceledH(null, asc);
            deferSyncH(op, 10000L, why);
        }

        private void startSyncH(SyncOperation op) {
            boolean isLoggable = Log.isLoggable("SyncManager", 2);
            if (isLoggable) {
                Slog.v("SyncManager", op.toString());
            }
            SyncManager.this.mSyncStorageEngine.setClockValid();
            SyncJobService.markSyncStarted(op.jobId);
            if (op.isPeriodic) {
                List<SyncOperation> ops = SyncManager.this.getAllPendingSyncs();
                for (SyncOperation syncOperation : ops) {
                    if (syncOperation.sourcePeriodicId == op.jobId) {
                        SyncJobService.callJobFinished(op.jobId, false, "periodic sync, pending");
                        return;
                    }
                }
                Iterator<ActiveSyncContext> it = SyncManager.this.mActiveSyncContexts.iterator();
                while (it.hasNext()) {
                    if (it.next().mSyncOperation.sourcePeriodicId == op.jobId) {
                        SyncJobService.callJobFinished(op.jobId, false, "periodic sync, already running");
                        return;
                    }
                }
                if (SyncManager.this.isAdapterDelayed(op.target)) {
                    deferSyncH(op, 0L, "backing off");
                    return;
                }
            }
            Iterator<ActiveSyncContext> it2 = SyncManager.this.mActiveSyncContexts.iterator();
            while (true) {
                if (!it2.hasNext()) {
                    break;
                }
                ActiveSyncContext asc = it2.next();
                if (asc.mSyncOperation.isConflict(op)) {
                    if (asc.mSyncOperation.getJobBias() >= op.getJobBias()) {
                        if (isLoggable) {
                            Slog.v("SyncManager", "Rescheduling sync due to conflict " + op.toString());
                        }
                        deferSyncH(op, 10000L, "delay on conflict");
                        return;
                    }
                    if (isLoggable) {
                        Slog.v("SyncManager", "Pushing back running sync due to a higher priority sync");
                    }
                    deferActiveSyncH(asc, "preempted");
                }
            }
            int syncOpState = computeSyncOpState(op);
            if (syncOpState != 0) {
                SyncJobService.callJobFinished(op.jobId, false, "invalid op state: " + syncOpState);
                return;
            }
            if (!dispatchSyncOperation(op)) {
                SyncJobService.callJobFinished(op.jobId, false, "dispatchSyncOperation() failed");
            }
            SyncManager.this.setAuthorityPendingState(op.target);
        }

        private ActiveSyncContext findActiveSyncContextH(int jobId) {
            Iterator<ActiveSyncContext> it = SyncManager.this.mActiveSyncContexts.iterator();
            while (it.hasNext()) {
                ActiveSyncContext asc = it.next();
                SyncOperation op = asc.mSyncOperation;
                if (op != null && op.jobId == jobId) {
                    return asc;
                }
            }
            return null;
        }

        private void updateRunningAccountsH(SyncStorageEngine.EndPoint syncTargets) {
            AccountAndUser[] accountAndUserArr;
            synchronized (SyncManager.this.mAccountsLock) {
                AccountAndUser[] oldAccounts = SyncManager.this.mRunningAccounts;
                SyncManager.this.mRunningAccounts = AccountManagerService.getSingleton().getRunningAccountsForSystem();
                if (Log.isLoggable("SyncManager", 2)) {
                    Slog.v("SyncManager", "Accounts list: ");
                    for (AccountAndUser acc : SyncManager.this.mRunningAccounts) {
                        Slog.v("SyncManager", acc.toString());
                    }
                }
                if (SyncManager.this.mLogger.enabled()) {
                    SyncManager.this.mLogger.log("updateRunningAccountsH: ", Arrays.toString(SyncManager.this.mRunningAccounts));
                }
                SyncManager.this.removeStaleAccounts();
                AccountAndUser[] accounts = SyncManager.this.mRunningAccounts;
                int size = SyncManager.this.mActiveSyncContexts.size();
                for (int i = 0; i < size; i++) {
                    ActiveSyncContext currentSyncContext = SyncManager.this.mActiveSyncContexts.get(i);
                    if (!SyncManager.this.containsAccountAndUser(accounts, currentSyncContext.mSyncOperation.target.account, currentSyncContext.mSyncOperation.target.userId)) {
                        Log.d("SyncManager", "canceling sync since the account is no longer running");
                        SyncManager.this.sendSyncFinishedOrCanceledMessage(currentSyncContext, null);
                    }
                }
                if (syncTargets != null) {
                    int i2 = 0;
                    int length = SyncManager.this.mRunningAccounts.length;
                    while (true) {
                        if (i2 >= length) {
                            break;
                        }
                        AccountAndUser aau = SyncManager.this.mRunningAccounts[i2];
                        if (SyncManager.this.containsAccountAndUser(oldAccounts, aau.account, aau.userId)) {
                            i2++;
                        } else {
                            if (Log.isLoggable("SyncManager", 3)) {
                                Log.d("SyncManager", "Account " + aau.account + " added, checking sync restore data");
                            }
                            AccountSyncSettingsBackupHelper.accountAdded(SyncManager.this.mContext, syncTargets.userId);
                        }
                    }
                }
            }
            AccountAndUser[] allAccounts = AccountManagerService.getSingleton().getAllAccountsForSystemProcess();
            List<SyncOperation> ops = SyncManager.this.getAllPendingSyncs();
            int opsSize = ops.size();
            for (int i3 = 0; i3 < opsSize; i3++) {
                SyncOperation op = ops.get(i3);
                if (!SyncManager.this.containsAccountAndUser(allAccounts, op.target.account, op.target.userId)) {
                    SyncManager.this.mLogger.log("canceling: ", op);
                    SyncManager.this.cancelJob(op, "updateRunningAccountsH()");
                }
            }
            if (syncTargets != null) {
                SyncManager.this.scheduleSync(syncTargets.account, syncTargets.userId, -2, syncTargets.provider, null, -1, 0, Process.myUid(), -4, null);
            }
        }

        private void maybeUpdateSyncPeriodH(SyncOperation syncOperation, long pollFrequencyMillis, long flexMillis) {
            if (pollFrequencyMillis != syncOperation.periodMillis || flexMillis != syncOperation.flexMillis) {
                if (Log.isLoggable("SyncManager", 2)) {
                    Slog.v("SyncManager", "updating period " + syncOperation + " to " + pollFrequencyMillis + " and flex to " + flexMillis);
                }
                SyncOperation newOp = new SyncOperation(syncOperation, pollFrequencyMillis, flexMillis);
                newOp.jobId = syncOperation.jobId;
                SyncManager.this.scheduleSyncOperationH(newOp);
            }
        }

        private void updateOrAddPeriodicSyncH(final SyncStorageEngine.EndPoint target, final long pollFrequency, final long flex, final Bundle extras) {
            boolean isLoggable = Log.isLoggable("SyncManager", 2);
            SyncManager.this.verifyJobScheduler();
            long pollFrequencyMillis = pollFrequency * 1000;
            long flexMillis = flex * 1000;
            if (isLoggable) {
                Slog.v("SyncManager", "Addition to periodic syncs requested: " + target + " period: " + pollFrequency + " flexMillis: " + flex + " extras: " + extras.toString());
            }
            List<SyncOperation> ops = SyncManager.this.getAllPendingSyncs();
            for (SyncOperation op : ops) {
                if (op.isPeriodic && op.target.matchesSpec(target)) {
                    if (op.areExtrasEqual(extras, true)) {
                        maybeUpdateSyncPeriodH(op, pollFrequencyMillis, flexMillis);
                        return;
                    }
                }
            }
            if (isLoggable) {
                Slog.v("SyncManager", "Adding new periodic sync: " + target + " period: " + pollFrequency + " flexMillis: " + flex + " extras: " + extras.toString());
            }
            RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapterInfo = SyncManager.this.mSyncAdapters.getServiceInfo(SyncAdapterType.newKey(target.provider, target.account.type), target.userId);
            if (syncAdapterInfo == null) {
                return;
            }
            SyncOperation op2 = new SyncOperation(target, syncAdapterInfo.uid, syncAdapterInfo.componentName.getPackageName(), -4, 4, extras, ((SyncAdapterType) syncAdapterInfo.type).allowParallelSyncs(), true, -1, pollFrequencyMillis, flexMillis, 0);
            int syncOpState = computeSyncOpState(op2);
            if (syncOpState != 2) {
                if (syncOpState != 0) {
                    SyncManager.this.mLogger.log("syncOpState=", Integer.valueOf(syncOpState));
                    return;
                }
                SyncManager.this.scheduleSyncOperationH(op2);
                SyncManager.this.mSyncStorageEngine.reportChange(1, op2.owningPackage, target.userId);
                return;
            }
            String packageName = op2.owningPackage;
            int userId = UserHandle.getUserId(op2.owningUid);
            if (!SyncManager.this.wasPackageEverLaunched(packageName, userId)) {
                return;
            }
            SyncManager.this.mLogger.log("requestAccountAccess for SYNC_OP_STATE_INVALID_NO_ACCOUNT_ACCESS");
            SyncManager.this.mAccountManagerInternal.requestAccountAccess(op2.target.account, packageName, userId, new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.content.SyncManager$SyncHandler$$ExternalSyntheticLambda0
                public final void onResult(Bundle bundle) {
                    SyncManager.SyncHandler.this.m2882xc52f1e47(target, pollFrequency, flex, extras, bundle);
                }
            }));
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$updateOrAddPeriodicSyncH$0$com-android-server-content-SyncManager$SyncHandler  reason: not valid java name */
        public /* synthetic */ void m2882xc52f1e47(SyncStorageEngine.EndPoint target, long pollFrequency, long flex, Bundle extras, Bundle result) {
            if (result != null && result.getBoolean("booleanResult")) {
                SyncManager.this.updateOrAddPeriodicSync(target, pollFrequency, flex, extras);
            }
        }

        private void removePeriodicSyncInternalH(SyncOperation syncOperation, String why) {
            List<SyncOperation> ops = SyncManager.this.getAllPendingSyncs();
            for (SyncOperation op : ops) {
                if (op.sourcePeriodicId == syncOperation.jobId || op.jobId == syncOperation.jobId) {
                    ActiveSyncContext asc = findActiveSyncContextH(syncOperation.jobId);
                    if (asc != null) {
                        SyncJobService.callJobFinished(syncOperation.jobId, false, "removePeriodicSyncInternalH");
                        runSyncFinishedOrCanceledH(null, asc);
                    }
                    SyncManager.this.mLogger.log("removePeriodicSyncInternalH-canceling: ", op);
                    SyncManager.this.cancelJob(op, why);
                }
            }
        }

        private void removePeriodicSyncH(SyncStorageEngine.EndPoint target, Bundle extras, String why) {
            SyncManager.this.verifyJobScheduler();
            List<SyncOperation> ops = SyncManager.this.getAllPendingSyncs();
            for (SyncOperation op : ops) {
                if (op.isPeriodic && op.target.matchesSpec(target) && op.areExtrasEqual(extras, true)) {
                    removePeriodicSyncInternalH(op, why);
                }
            }
        }

        private boolean isSyncNotUsingNetworkH(ActiveSyncContext activeSyncContext) {
            long bytesTransferredCurrent = SyncManager.this.getTotalBytesTransferredByUid(activeSyncContext.mSyncAdapterUid);
            long deltaBytesTransferred = bytesTransferredCurrent - activeSyncContext.mBytesTransferredAtLastPoll;
            if (Log.isLoggable("SyncManager", 3)) {
                long mb = deltaBytesTransferred / 1048576;
                long remainder = deltaBytesTransferred % 1048576;
                long kb = remainder / GadgetFunction.NCM;
                Log.d("SyncManager", String.format("Time since last update: %ds. Delta transferred: %dMBs,%dKBs,%dBs", Long.valueOf((SystemClock.elapsedRealtime() - activeSyncContext.mLastPolledTimeElapsed) / 1000), Long.valueOf(mb), Long.valueOf(kb), Long.valueOf(remainder % GadgetFunction.NCM)));
            }
            return deltaBytesTransferred <= 10;
        }

        private int computeSyncOpState(SyncOperation op) {
            boolean isLoggable = Log.isLoggable("SyncManager", 2);
            SyncStorageEngine.EndPoint target = op.target;
            synchronized (SyncManager.this.mAccountsLock) {
                AccountAndUser[] accounts = SyncManager.this.mRunningAccounts;
                if (!SyncManager.this.containsAccountAndUser(accounts, target.account, target.userId)) {
                    if (isLoggable) {
                        Slog.v("SyncManager", "    Dropping sync operation: account doesn't exist.");
                    }
                    logAccountError("SYNC_OP_STATE_INVALID: account doesn't exist.");
                    return 3;
                }
                boolean z = true;
                int state = SyncManager.this.computeSyncable(target.account, target.userId, target.provider, true);
                if (state == 3) {
                    if (isLoggable) {
                        Slog.v("SyncManager", "    Dropping sync operation: isSyncable == SYNCABLE_NO_ACCOUNT_ACCESS");
                    }
                    logAccountError("SYNC_OP_STATE_INVALID_NO_ACCOUNT_ACCESS");
                    return 2;
                } else if (state == 0) {
                    if (isLoggable) {
                        Slog.v("SyncManager", "    Dropping sync operation: isSyncable == NOT_SYNCABLE");
                    }
                    logAccountError("SYNC_OP_STATE_INVALID: NOT_SYNCABLE");
                    return 4;
                } else {
                    boolean syncEnabled = SyncManager.this.mSyncStorageEngine.getMasterSyncAutomatically(target.userId) && SyncManager.this.mSyncStorageEngine.getSyncAutomatically(target.account, target.userId, target.provider);
                    if (!op.isIgnoreSettings() && state >= 0) {
                        z = false;
                    }
                    boolean ignoreSystemConfiguration = z;
                    if (syncEnabled || ignoreSystemConfiguration) {
                        return 0;
                    }
                    if (isLoggable) {
                        Slog.v("SyncManager", "    Dropping sync operation: disallowed by settings/network.");
                    }
                    logAccountError("SYNC_OP_STATE_INVALID: disallowed by settings/network");
                    return 5;
                }
            }
        }

        private void logAccountError(String message) {
            Slog.wtf("SyncManager", message);
        }

        private boolean dispatchSyncOperation(SyncOperation op) {
            UsageStatsManagerInternal usmi;
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "dispatchSyncOperation: we are going to sync " + op);
                Slog.v("SyncManager", "num active syncs: " + SyncManager.this.mActiveSyncContexts.size());
                Iterator<ActiveSyncContext> it = SyncManager.this.mActiveSyncContexts.iterator();
                while (it.hasNext()) {
                    ActiveSyncContext syncContext = it.next();
                    Slog.v("SyncManager", syncContext.toString());
                }
            }
            if (op.isAppStandbyExempted() && (usmi = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class)) != null) {
                usmi.reportExemptedSyncStart(op.owningPackage, UserHandle.getUserId(op.owningUid));
            }
            SyncStorageEngine.EndPoint info = op.target;
            SyncAdapterType syncAdapterType = SyncAdapterType.newKey(info.provider, info.account.type);
            RegisteredServicesCache.ServiceInfo<SyncAdapterType> syncAdapterInfo = SyncManager.this.mSyncAdapters.getServiceInfo(syncAdapterType, info.userId);
            if (syncAdapterInfo == null) {
                SyncManager.this.mLogger.log("dispatchSyncOperation() failed: no sync adapter info for ", syncAdapterType);
                Log.d("SyncManager", "can't find a sync adapter for " + syncAdapterType + ", removing settings for it");
                SyncManager.this.mSyncStorageEngine.removeAuthority(info);
                return false;
            }
            int targetUid = syncAdapterInfo.uid;
            ComponentName targetComponent = syncAdapterInfo.componentName;
            ActiveSyncContext activeSyncContext = new ActiveSyncContext(op, insertStartSyncEvent(op), targetUid);
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "dispatchSyncOperation: starting " + activeSyncContext);
            }
            activeSyncContext.mSyncInfo = SyncManager.this.mSyncStorageEngine.addActiveSync(activeSyncContext);
            SyncManager.this.mActiveSyncContexts.add(activeSyncContext);
            SyncManager.this.postMonitorSyncProgressMessage(activeSyncContext);
            if (!activeSyncContext.bindToSyncAdapter(targetComponent, info.userId)) {
                SyncManager.this.mLogger.log("dispatchSyncOperation() failed: bind failed. target: ", targetComponent);
                Slog.e("SyncManager", "Bind attempt failed - target: " + targetComponent);
                closeActiveSyncContext(activeSyncContext);
                return false;
            }
            return true;
        }

        private void runBoundToAdapterH(ActiveSyncContext activeSyncContext, IBinder syncAdapter) {
            SyncOperation syncOperation = activeSyncContext.mSyncOperation;
            try {
                activeSyncContext.mIsLinkedToDeath = true;
                syncAdapter.linkToDeath(activeSyncContext, 0);
                if (SyncManager.this.mLogger.enabled()) {
                    SyncManager.this.mLogger.log("Sync start: account=" + syncOperation.target.account, " authority=", syncOperation.target.provider, " reason=", SyncOperation.reasonToString(null, syncOperation.reason), " extras=", syncOperation.getExtrasAsString(), " adapter=", activeSyncContext.mSyncAdapter);
                }
                activeSyncContext.mSyncAdapter = ISyncAdapter.Stub.asInterface(syncAdapter);
                activeSyncContext.mSyncAdapter.startSync(activeSyncContext, syncOperation.target.provider, syncOperation.target.account, syncOperation.getClonedExtras());
                SyncManager.this.mLogger.log("Sync is running now...");
            } catch (RemoteException remoteExc) {
                SyncManager.this.mLogger.log("Sync failed with RemoteException: ", remoteExc.toString());
                Log.d("SyncManager", "maybeStartNextSync: caught a RemoteException, rescheduling", remoteExc);
                closeActiveSyncContext(activeSyncContext);
                SyncManager.this.increaseBackoffSetting(syncOperation.target);
                SyncManager.this.scheduleSyncOperationH(syncOperation);
            } catch (RuntimeException exc) {
                SyncManager.this.mLogger.log("Sync failed with RuntimeException: ", exc.toString());
                closeActiveSyncContext(activeSyncContext);
                Slog.e("SyncManager", "Caught RuntimeException while starting the sync " + SyncLogger.logSafe(syncOperation), exc);
            }
        }

        private void cancelActiveSyncH(SyncStorageEngine.EndPoint info, Bundle extras, String why) {
            ArrayList<ActiveSyncContext> activeSyncs = new ArrayList<>(SyncManager.this.mActiveSyncContexts);
            Iterator<ActiveSyncContext> it = activeSyncs.iterator();
            while (it.hasNext()) {
                ActiveSyncContext activeSyncContext = it.next();
                if (activeSyncContext != null) {
                    SyncStorageEngine.EndPoint opInfo = activeSyncContext.mSyncOperation.target;
                    if (opInfo.matchesSpec(info) && (extras == null || activeSyncContext.mSyncOperation.areExtrasEqual(extras, false))) {
                        SyncJobService.callJobFinished(activeSyncContext.mSyncOperation.jobId, false, why);
                        runSyncFinishedOrCanceledH(null, activeSyncContext);
                    }
                }
            }
        }

        private void reschedulePeriodicSyncH(SyncOperation syncOperation) {
            SyncOperation periodicSync = null;
            List<SyncOperation> ops = SyncManager.this.getAllPendingSyncs();
            Iterator<SyncOperation> it = ops.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                SyncOperation op = it.next();
                if (op.isPeriodic && syncOperation.matchesPeriodicOperation(op)) {
                    periodicSync = op;
                    break;
                }
            }
            if (periodicSync == null) {
                return;
            }
            SyncManager.this.scheduleSyncOperationH(periodicSync);
        }

        private void runSyncFinishedOrCanceledH(SyncResult syncResult, ActiveSyncContext activeSyncContext) {
            String historyMessage;
            int downstreamActivity;
            int upstreamActivity;
            int downstreamActivity2;
            int upstreamActivity2;
            boolean isLoggable = Log.isLoggable("SyncManager", 2);
            SyncOperation syncOperation = activeSyncContext.mSyncOperation;
            SyncStorageEngine.EndPoint info = syncOperation.target;
            if (activeSyncContext.mIsLinkedToDeath) {
                activeSyncContext.mSyncAdapter.asBinder().unlinkToDeath(activeSyncContext, 0);
                activeSyncContext.mIsLinkedToDeath = false;
            }
            long elapsedTime = SystemClock.elapsedRealtime() - activeSyncContext.mStartTime;
            SyncManager.this.mLogger.log("runSyncFinishedOrCanceledH() op=", syncOperation, " result=", syncResult);
            if (syncResult != null) {
                if (isLoggable) {
                    Slog.v("SyncManager", "runSyncFinishedOrCanceled [finished]: " + syncOperation + ", result " + syncResult);
                }
                closeActiveSyncContext(activeSyncContext);
                if (!syncOperation.isPeriodic) {
                    SyncManager.this.cancelJob(syncOperation, "runSyncFinishedOrCanceledH()-finished");
                }
                if (!syncResult.hasError()) {
                    historyMessage = SyncStorageEngine.MESG_SUCCESS;
                    downstreamActivity2 = 0;
                    upstreamActivity2 = 0;
                    SyncManager.this.clearBackoffSetting(syncOperation.target, "sync success");
                    if (syncOperation.isDerivedFromFailedPeriodicSync()) {
                        reschedulePeriodicSyncH(syncOperation);
                    }
                } else {
                    Log.w("SyncManager", "failed sync operation " + SyncLogger.logSafe(syncOperation) + ", " + syncResult);
                    syncOperation.retries++;
                    if (syncOperation.retries > SyncManager.this.mConstants.getMaxRetriesWithAppStandbyExemption()) {
                        syncOperation.syncExemptionFlag = 0;
                    }
                    SyncManager.this.increaseBackoffSetting(syncOperation.target);
                    if (!syncOperation.isPeriodic) {
                        SyncManager.this.maybeRescheduleSync(syncResult, syncOperation);
                    } else {
                        SyncManager.this.postScheduleSyncMessage(syncOperation.createOneTimeSyncOperation(), 0L);
                    }
                    historyMessage = ContentResolver.syncErrorToString(syncResultToErrorNumber(syncResult));
                    downstreamActivity2 = 0;
                    upstreamActivity2 = 0;
                }
                SyncManager.this.setDelayUntilTime(syncOperation.target, syncResult.delayUntil);
                downstreamActivity = downstreamActivity2;
                upstreamActivity = upstreamActivity2;
            } else {
                if (isLoggable) {
                    Slog.v("SyncManager", "runSyncFinishedOrCanceled [canceled]: " + syncOperation);
                }
                if (!syncOperation.isPeriodic) {
                    SyncManager.this.cancelJob(syncOperation, "runSyncFinishedOrCanceledH()-canceled");
                }
                if (activeSyncContext.mSyncAdapter != null) {
                    try {
                        SyncManager.this.mLogger.log("Calling cancelSync for runSyncFinishedOrCanceled ", activeSyncContext, "  adapter=", activeSyncContext.mSyncAdapter);
                        activeSyncContext.mSyncAdapter.cancelSync(activeSyncContext);
                        SyncManager.this.mLogger.log("Canceled");
                    } catch (RemoteException e) {
                        SyncManager.this.mLogger.log("RemoteException ", Log.getStackTraceString(e));
                    }
                }
                historyMessage = SyncStorageEngine.MESG_CANCELED;
                closeActiveSyncContext(activeSyncContext);
                downstreamActivity = 0;
                upstreamActivity = 0;
            }
            stopSyncEvent(activeSyncContext.mHistoryRowId, syncOperation, historyMessage, upstreamActivity, downstreamActivity, elapsedTime);
            if (syncResult != null && syncResult.tooManyDeletions) {
                installHandleTooManyDeletesNotification(info.account, info.provider, syncResult.stats.numDeletes, info.userId);
            } else {
                SyncManager.this.mNotificationMgr.cancelAsUser(Integer.toString(info.account.hashCode() ^ info.provider.hashCode()), 18, new UserHandle(info.userId));
            }
            if (syncResult != null && syncResult.fullSyncRequested) {
                SyncManager.this.scheduleSyncOperationH(new SyncOperation(info.account, info.userId, syncOperation.owningUid, syncOperation.owningPackage, syncOperation.reason, syncOperation.syncSource, info.provider, new Bundle(), syncOperation.allowParallelSyncs, syncOperation.syncExemptionFlag));
            }
        }

        private void closeActiveSyncContext(ActiveSyncContext activeSyncContext) {
            activeSyncContext.close();
            SyncManager.this.mActiveSyncContexts.remove(activeSyncContext);
            SyncManager.this.mSyncStorageEngine.removeActiveSync(activeSyncContext.mSyncInfo, activeSyncContext.mSyncOperation.target.userId);
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "removing all MESSAGE_MONITOR_SYNC & MESSAGE_SYNC_EXPIRED for " + activeSyncContext.toString());
            }
            SyncManager.this.mSyncHandler.removeMessages(8, activeSyncContext);
            SyncManager.this.mLogger.log("closeActiveSyncContext: ", activeSyncContext);
        }

        private int syncResultToErrorNumber(SyncResult syncResult) {
            if (syncResult.syncAlreadyInProgress) {
                return 1;
            }
            if (syncResult.stats.numAuthExceptions > 0) {
                return 2;
            }
            if (syncResult.stats.numIoExceptions > 0) {
                return 3;
            }
            if (syncResult.stats.numParseExceptions > 0) {
                return 4;
            }
            if (syncResult.stats.numConflictDetectedExceptions > 0) {
                return 5;
            }
            if (syncResult.tooManyDeletions) {
                return 6;
            }
            if (syncResult.tooManyRetries) {
                return 7;
            }
            if (syncResult.databaseError) {
                return 8;
            }
            throw new IllegalStateException("we are not in an error state, " + syncResult);
        }

        private void installHandleTooManyDeletesNotification(Account account, String authority, long numDeletes, int userId) {
            ProviderInfo providerInfo;
            if (SyncManager.this.mNotificationMgr == null || (providerInfo = SyncManager.this.mContext.getPackageManager().resolveContentProvider(authority, 0)) == null) {
                return;
            }
            CharSequence authorityName = providerInfo.loadLabel(SyncManager.this.mContext.getPackageManager());
            Intent clickIntent = new Intent(SyncManager.this.mContext, SyncActivityTooManyDeletes.class);
            clickIntent.putExtra("account", account);
            clickIntent.putExtra("authority", authority);
            clickIntent.putExtra("provider", authorityName.toString());
            clickIntent.putExtra("numDeletes", numDeletes);
            if (!isActivityAvailable(clickIntent)) {
                Log.w("SyncManager", "No activity found to handle too many deletes.");
                return;
            }
            UserHandle user = new UserHandle(userId);
            PendingIntent pendingIntent = PendingIntent.getActivityAsUser(SyncManager.this.mContext, 0, clickIntent, AudioFormat.AAC_ADIF, null, user);
            CharSequence tooManyDeletesDescFormat = SyncManager.this.mContext.getResources().getText(17040082);
            Context contextForUser = SyncManager.this.getContextForUser(user);
            Notification notification = new Notification.Builder(contextForUser, SystemNotificationChannels.ACCOUNT).setSmallIcon(17303612).setTicker(SyncManager.this.mContext.getString(17040080)).setWhen(System.currentTimeMillis()).setColor(contextForUser.getColor(17170460)).setContentTitle(contextForUser.getString(17040081)).setContentText(String.format(tooManyDeletesDescFormat.toString(), authorityName)).setContentIntent(pendingIntent).build();
            notification.flags |= 2;
            SyncManager.this.mNotificationMgr.notifyAsUser(Integer.toString(account.hashCode() ^ authority.hashCode()), 18, notification, user);
        }

        private boolean isActivityAvailable(Intent intent) {
            PackageManager pm = SyncManager.this.mContext.getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
            int listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                ResolveInfo resolveInfo = list.get(i);
                if ((resolveInfo.activityInfo.applicationInfo.flags & 1) != 0) {
                    return true;
                }
            }
            return false;
        }

        public long insertStartSyncEvent(SyncOperation syncOperation) {
            long now = System.currentTimeMillis();
            EventLog.writeEvent(2720, syncOperation.toEventLog(0));
            return SyncManager.this.mSyncStorageEngine.insertStartSyncEvent(syncOperation, now);
        }

        public void stopSyncEvent(long rowId, SyncOperation syncOperation, String resultMessage, int upstreamActivity, int downstreamActivity, long elapsedTime) {
            EventLog.writeEvent(2720, syncOperation.toEventLog(1));
            SyncManager.this.mSyncStorageEngine.stopSyncEvent(rowId, elapsedTime, resultMessage, downstreamActivity, upstreamActivity, syncOperation.owningPackage, syncOperation.target.userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isSyncStillActiveH(ActiveSyncContext activeSyncContext) {
        Iterator<ActiveSyncContext> it = this.mActiveSyncContexts.iterator();
        while (it.hasNext()) {
            ActiveSyncContext sync = it.next();
            if (sync == activeSyncContext) {
                return true;
            }
        }
        return false;
    }

    public static boolean syncExtrasEquals(Bundle b1, Bundle b2, boolean includeSyncSettings) {
        if (b1 == b2) {
            return true;
        }
        if (includeSyncSettings && b1.size() != b2.size()) {
            return false;
        }
        Bundle bigger = b1.size() > b2.size() ? b1 : b2;
        Bundle smaller = b1.size() > b2.size() ? b2 : b1;
        for (String key : bigger.keySet()) {
            if (includeSyncSettings || !isSyncSetting(key)) {
                if (!smaller.containsKey(key) || !Objects.equals(bigger.get(key), smaller.get(key))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isSyncSetting(String key) {
        if (key == null) {
            return false;
        }
        return key.equals("expedited") || key.equals("schedule_as_expedited_job") || key.equals("ignore_settings") || key.equals("ignore_backoff") || key.equals("do_not_retry") || key.equals("force") || key.equals("upload") || key.equals("deletions_override") || key.equals("discard_deletions") || key.equals("expected_upload") || key.equals("expected_download") || key.equals("sync_priority") || key.equals("allow_metered") || key.equals("initialize");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class PrintTable {
        private final int mCols;
        private ArrayList<String[]> mTable = Lists.newArrayList();

        PrintTable(int cols) {
            this.mCols = cols;
        }

        void set(int row, int col, Object... values) {
            if (values.length + col > this.mCols) {
                throw new IndexOutOfBoundsException("Table only has " + this.mCols + " columns. can't set " + values.length + " at column " + col);
            }
            for (int i = this.mTable.size(); i <= row; i++) {
                String[] list = new String[this.mCols];
                this.mTable.add(list);
                for (int j = 0; j < this.mCols; j++) {
                    list[j] = "";
                }
            }
            String[] rowArray = this.mTable.get(row);
            for (int i2 = 0; i2 < values.length; i2++) {
                Object value = values[i2];
                rowArray[col + i2] = value == null ? "" : value.toString();
            }
        }

        void writeTo(PrintWriter out) {
            int i;
            String[] formats = new String[this.mCols];
            int totalLength = 0;
            int col = 0;
            while (true) {
                i = this.mCols;
                if (col >= i) {
                    break;
                }
                int maxLength = 0;
                Iterator<String[]> it = this.mTable.iterator();
                while (it.hasNext()) {
                    Object[] row = it.next();
                    int length = row[col].toString().length();
                    if (length > maxLength) {
                        maxLength = length;
                    }
                }
                totalLength += maxLength;
                formats[col] = String.format("%%-%ds", Integer.valueOf(maxLength));
                col++;
            }
            formats[i - 1] = "%s";
            printRow(out, formats, this.mTable.get(0));
            int totalLength2 = totalLength + ((this.mCols - 1) * 2);
            for (int i2 = 0; i2 < totalLength2; i2++) {
                out.print("-");
            }
            out.println();
            int mTableSize = this.mTable.size();
            for (int i3 = 1; i3 < mTableSize; i3++) {
                Object[] row2 = this.mTable.get(i3);
                printRow(out, formats, row2);
            }
        }

        private void printRow(PrintWriter out, String[] formats, Object[] row) {
            int rowLength = row.length;
            for (int j = 0; j < rowLength; j++) {
                out.printf(String.format(formats[j], row[j].toString()), new Object[0]);
                out.print("  ");
            }
            out.println();
        }

        public int getNumRows() {
            return this.mTable.size();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Context getContextForUser(UserHandle user) {
        try {
            Context context = this.mContext;
            return context.createPackageContextAsUser(context.getPackageName(), 0, user);
        } catch (PackageManager.NameNotFoundException e) {
            return this.mContext;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelJob(SyncOperation op, String why) {
        if (op == null) {
            Slog.wtf("SyncManager", "Null sync operation detected.");
            return;
        }
        if (op.isPeriodic) {
            this.mLogger.log("Removing periodic sync ", op, " for ", why);
        }
        getJobScheduler().cancel(op.jobId);
    }

    public void resetTodayStats() {
        this.mSyncStorageEngine.resetTodayStats(true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean wasPackageEverLaunched(String packageName, int userId) {
        try {
            return this.mPackageManagerInternal.wasPackageEverLaunched(packageName, userId);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
