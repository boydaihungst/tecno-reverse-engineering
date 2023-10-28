package com.android.server.backup;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.IBackupAgent;
import android.app.PendingIntent;
import android.app.backup.BackupAgent;
import android.app.backup.IBackupManager;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import android.app.backup.IFullBackupRestoreObserver;
import android.app.backup.IRestoreSession;
import android.app.backup.ISelectBackupTransportCallback;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.PowerSaveState;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.EventLog;
import android.util.FeatureFlagUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.Preconditions;
import com.android.server.AppWidgetBackupBridge;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.backup.UserBackupManagerService;
import com.android.server.backup.fullbackup.FullBackupEntry;
import com.android.server.backup.fullbackup.PerformFullTransportBackupTask;
import com.android.server.backup.internal.BackupHandler;
import com.android.server.backup.internal.ClearDataObserver;
import com.android.server.backup.internal.LifecycleOperationStorage;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.internal.PerformInitializeTask;
import com.android.server.backup.internal.RunInitializeReceiver;
import com.android.server.backup.internal.SetupObserver;
import com.android.server.backup.keyvalue.BackupRequest;
import com.android.server.backup.params.AdbBackupParams;
import com.android.server.backup.params.AdbParams;
import com.android.server.backup.params.AdbRestoreParams;
import com.android.server.backup.params.BackupParams;
import com.android.server.backup.params.ClearParams;
import com.android.server.backup.params.ClearRetryParams;
import com.android.server.backup.params.RestoreParams;
import com.android.server.backup.restore.ActiveRestoreSession;
import com.android.server.backup.restore.PerformUnifiedRestoreTask;
import com.android.server.backup.transport.BackupTransportClient;
import com.android.server.backup.transport.OnTransportRegisteredListener;
import com.android.server.backup.transport.TransportConnection;
import com.android.server.backup.transport.TransportNotAvailableException;
import com.android.server.backup.transport.TransportNotRegisteredException;
import com.android.server.backup.utils.BackupEligibilityRules;
import com.android.server.backup.utils.BackupManagerMonitorUtils;
import com.android.server.backup.utils.BackupObserverUtils;
import com.android.server.backup.utils.SparseArrayUtils;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import com.android.server.usage.AppStandbyController;
import com.android.server.wm.ActivityTaskManagerService;
import com.google.android.collect.Sets;
import dalvik.annotation.optimization.NeverCompile;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
/* loaded from: classes.dex */
public class UserBackupManagerService {
    public static final String BACKUP_FILE_HEADER_MAGIC = "ANDROID BACKUP\n";
    public static final int BACKUP_FILE_VERSION = 5;
    private static final String BACKUP_FINISHED_ACTION = "android.intent.action.BACKUP_FINISHED";
    private static final String BACKUP_FINISHED_PACKAGE_EXTRA = "packageName";
    public static final String BACKUP_MANIFEST_FILENAME = "_manifest";
    public static final int BACKUP_MANIFEST_VERSION = 1;
    public static final String BACKUP_METADATA_FILENAME = "_meta";
    public static final int BACKUP_METADATA_VERSION = 1;
    public static final int BACKUP_WIDGET_METADATA_TOKEN = 33549569;
    private static final long BIND_TIMEOUT_INTERVAL = 10000;
    private static final int BUSY_BACKOFF_FUZZ = 7200000;
    private static final long BUSY_BACKOFF_MIN_MILLIS = 3600000;
    private static final long CLEAR_DATA_TIMEOUT_INTERVAL = 30000;
    private static final int CURRENT_ANCESTRAL_RECORD_VERSION = 1;
    private static final long INITIALIZATION_DELAY_MILLIS = 3000;
    private static final String INIT_SENTINEL_FILE_NAME = "_need_init_";
    public static final String KEY_WIDGET_STATE = "￭￭widget";
    public static final String PACKAGE_MANAGER_SENTINEL = "@pm@";
    public static final String RUN_INITIALIZE_ACTION = "android.app.backup.intent.INIT";
    private static final int SCHEDULE_FILE_VERSION = 1;
    private static final String SERIAL_ID_FILE = "serial_id";
    public static final String SETTINGS_PACKAGE = "com.android.providers.settings";
    public static final String SHARED_BACKUP_AGENT_PACKAGE = "com.android.sharedstoragebackup";
    private static final String SKIP_USER_FACING_PACKAGES = "backup_skip_user_facing_packages";
    private static final long TIMEOUT_FULL_CONFIRMATION = 60000;
    private static final long TRANSPORT_RETRY_INTERVAL = 3600000;
    private static final String WALLPAPER_PACKAGE = "com.android.wallpaperbackup";
    private ActiveRestoreSession mActiveRestoreSession;
    private final IActivityManager mActivityManager;
    private final ActivityManagerInternal mActivityManagerInternal;
    private final SparseArray<AdbParams> mAdbBackupRestoreConfirmations;
    private final Object mAgentConnectLock;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    private final AlarmManager mAlarmManager;
    private volatile long mAncestralOperationType;
    private Set<String> mAncestralPackages;
    private File mAncestralSerialNumberFile;
    private long mAncestralToken;
    private boolean mAutoRestore;
    private final BackupHandler mBackupHandler;
    private final IBackupManager mBackupManagerBinder;
    private final SparseArray<HashSet<String>> mBackupParticipants;
    private final BackupPasswordManager mBackupPasswordManager;
    private final UserBackupPreferences mBackupPreferences;
    private volatile boolean mBackupRunning;
    private final File mBaseStateDir;
    private final Object mClearDataLock;
    private volatile boolean mClearingData;
    private IBackupAgent mConnectedAgent;
    private volatile boolean mConnecting;
    private final BackupManagerConstants mConstants;
    private final Context mContext;
    private long mCurrentToken;
    private final File mDataDir;
    private boolean mEnabled;
    private ArrayList<FullBackupEntry> mFullBackupQueue;
    private final File mFullBackupScheduleFile;
    private Runnable mFullBackupScheduleWriter;
    private boolean mIsRestoreInProgress;
    private DataChangedJournal mJournal;
    private final File mJournalDir;
    private volatile long mLastBackupPass;
    private final AtomicInteger mNextToken;
    private final LifecycleOperationStorage mOperationStorage;
    private final PackageManager mPackageManager;
    private final IPackageManager mPackageManagerBinder;
    private BroadcastReceiver mPackageTrackingReceiver;
    private final HashMap<String, BackupRequest> mPendingBackups;
    private final ArraySet<String> mPendingInits;
    private final Queue<PerformUnifiedRestoreTask> mPendingRestores;
    private PowerManager mPowerManager;
    private ProcessedPackagesJournal mProcessedPackagesJournal;
    private final Object mQueueLock;
    private final long mRegisterTransportsRequestedTime;
    private final SecureRandom mRng;
    private final PendingIntent mRunInitIntent;
    private final BroadcastReceiver mRunInitReceiver;
    private PerformFullTransportBackupTask mRunningFullBackupTask;
    private final BackupEligibilityRules mScheduledBackupEligibility;
    private boolean mSetupComplete;
    private final ContentObserver mSetupObserver;
    private File mTokenFile;
    private final Random mTokenGenerator;
    private final TransportManager mTransportManager;
    private final int mUserId;
    private final BackupWakeLock mWakelock;

    /* loaded from: classes.dex */
    public static class BackupWakeLock {
        private boolean mHasQuit = false;
        private final PowerManager.WakeLock mPowerManagerWakeLock;
        private int mUserId;

        public BackupWakeLock(PowerManager.WakeLock powerManagerWakeLock, int userId) {
            this.mPowerManagerWakeLock = powerManagerWakeLock;
            this.mUserId = userId;
        }

        public synchronized void acquire() {
            if (this.mHasQuit) {
                Slog.v(BackupManagerService.TAG, UserBackupManagerService.addUserIdToLogMessage(this.mUserId, "Ignore wakelock acquire after quit: " + this.mPowerManagerWakeLock.getTag()));
                return;
            }
            this.mPowerManagerWakeLock.acquire();
            Slog.v(BackupManagerService.TAG, UserBackupManagerService.addUserIdToLogMessage(this.mUserId, "Acquired wakelock:" + this.mPowerManagerWakeLock.getTag()));
        }

        public synchronized void release() {
            if (this.mHasQuit) {
                Slog.v(BackupManagerService.TAG, UserBackupManagerService.addUserIdToLogMessage(this.mUserId, "Ignore wakelock release after quit: " + this.mPowerManagerWakeLock.getTag()));
                return;
            }
            this.mPowerManagerWakeLock.release();
            Slog.v(BackupManagerService.TAG, UserBackupManagerService.addUserIdToLogMessage(this.mUserId, "Released wakelock:" + this.mPowerManagerWakeLock.getTag()));
        }

        public synchronized boolean isHeld() {
            return this.mPowerManagerWakeLock.isHeld();
        }

        public synchronized void quit() {
            while (this.mPowerManagerWakeLock.isHeld()) {
                Slog.v(BackupManagerService.TAG, UserBackupManagerService.addUserIdToLogMessage(this.mUserId, "Releasing wakelock: " + this.mPowerManagerWakeLock.getTag()));
                this.mPowerManagerWakeLock.release();
            }
            this.mHasQuit = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static UserBackupManagerService createAndInitializeService(int userId, Context context, BackupManagerService backupManagerService, Set<ComponentName> transportWhitelist) {
        String currentTransport = Settings.Secure.getStringForUser(context.getContentResolver(), "backup_transport", userId);
        if (TextUtils.isEmpty(currentTransport)) {
            currentTransport = null;
        }
        Slog.v(BackupManagerService.TAG, addUserIdToLogMessage(userId, "Starting with transport " + currentTransport));
        TransportManager transportManager = new TransportManager(userId, context, transportWhitelist, currentTransport);
        File baseStateDir = UserBackupManagerFiles.getBaseStateDir(userId);
        File dataDir = UserBackupManagerFiles.getDataDir(userId);
        HandlerThread userBackupThread = new HandlerThread("backup-" + userId, 10);
        userBackupThread.start();
        Slog.d(BackupManagerService.TAG, addUserIdToLogMessage(userId, "Started thread " + userBackupThread.getName()));
        return createAndInitializeService(userId, context, backupManagerService, userBackupThread, baseStateDir, dataDir, transportManager);
    }

    public static UserBackupManagerService createAndInitializeService(int userId, Context context, BackupManagerService backupManagerService, HandlerThread userBackupThread, File baseStateDir, File dataDir, TransportManager transportManager) {
        return new UserBackupManagerService(userId, context, backupManagerService, userBackupThread, baseStateDir, dataDir, transportManager);
    }

    public static boolean getSetupCompleteSettingForUser(Context context, int userId) {
        return Settings.Secure.getIntForUser(context.getContentResolver(), "user_setup_complete", 0, userId) != 0;
    }

    UserBackupManagerService(Context context, PackageManager packageManager, LifecycleOperationStorage operationStorage) {
        this.mPendingInits = new ArraySet<>();
        this.mBackupParticipants = new SparseArray<>();
        this.mPendingBackups = new HashMap<>();
        this.mQueueLock = new Object();
        this.mAgentConnectLock = new Object();
        this.mClearDataLock = new Object();
        this.mAdbBackupRestoreConfirmations = new SparseArray<>();
        this.mRng = new SecureRandom();
        this.mPendingRestores = new ArrayDeque();
        this.mTokenGenerator = new Random();
        this.mNextToken = new AtomicInteger();
        this.mAncestralPackages = null;
        this.mAncestralToken = 0L;
        this.mCurrentToken = 0L;
        this.mFullBackupScheduleWriter = new Runnable() { // from class: com.android.server.backup.UserBackupManagerService.1
            @Override // java.lang.Runnable
            public void run() {
                synchronized (UserBackupManagerService.this.mQueueLock) {
                    try {
                        ByteArrayOutputStream bufStream = new ByteArrayOutputStream(4096);
                        DataOutputStream bufOut = new DataOutputStream(bufStream);
                        bufOut.writeInt(1);
                        int numPackages = UserBackupManagerService.this.mFullBackupQueue.size();
                        bufOut.writeInt(numPackages);
                        for (int i = 0; i < numPackages; i++) {
                            FullBackupEntry entry = (FullBackupEntry) UserBackupManagerService.this.mFullBackupQueue.get(i);
                            bufOut.writeUTF(entry.packageName);
                            bufOut.writeLong(entry.lastBackup);
                        }
                        bufOut.flush();
                        AtomicFile af = new AtomicFile(UserBackupManagerService.this.mFullBackupScheduleFile);
                        FileOutputStream out = af.startWrite();
                        out.write(bufStream.toByteArray());
                        af.finishWrite(out);
                    } catch (Exception e) {
                        Slog.e(BackupManagerService.TAG, UserBackupManagerService.addUserIdToLogMessage(UserBackupManagerService.this.mUserId, "Unable to write backup schedule!"), e);
                    }
                }
            }
        };
        this.mPackageTrackingReceiver = new AnonymousClass2();
        this.mContext = context;
        this.mUserId = 0;
        this.mRegisterTransportsRequestedTime = 0L;
        this.mPackageManager = packageManager;
        this.mOperationStorage = operationStorage;
        this.mBaseStateDir = null;
        this.mDataDir = null;
        this.mJournalDir = null;
        this.mFullBackupScheduleFile = null;
        this.mSetupObserver = null;
        this.mRunInitReceiver = null;
        this.mRunInitIntent = null;
        this.mAgentTimeoutParameters = null;
        this.mTransportManager = null;
        this.mActivityManagerInternal = null;
        this.mAlarmManager = null;
        this.mConstants = null;
        this.mWakelock = null;
        this.mBackupHandler = null;
        this.mBackupPreferences = null;
        this.mBackupPasswordManager = null;
        this.mPackageManagerBinder = null;
        this.mActivityManager = null;
        this.mBackupManagerBinder = null;
        this.mScheduledBackupEligibility = null;
    }

    private UserBackupManagerService(int userId, Context context, BackupManagerService parent, HandlerThread userBackupThread, File baseStateDir, File dataDir, TransportManager transportManager) {
        this.mPendingInits = new ArraySet<>();
        SparseArray<HashSet<String>> sparseArray = new SparseArray<>();
        this.mBackupParticipants = sparseArray;
        this.mPendingBackups = new HashMap<>();
        this.mQueueLock = new Object();
        this.mAgentConnectLock = new Object();
        this.mClearDataLock = new Object();
        this.mAdbBackupRestoreConfirmations = new SparseArray<>();
        SecureRandom secureRandom = new SecureRandom();
        this.mRng = secureRandom;
        this.mPendingRestores = new ArrayDeque();
        this.mTokenGenerator = new Random();
        this.mNextToken = new AtomicInteger();
        this.mAncestralPackages = null;
        this.mAncestralToken = 0L;
        this.mCurrentToken = 0L;
        this.mFullBackupScheduleWriter = new Runnable() { // from class: com.android.server.backup.UserBackupManagerService.1
            @Override // java.lang.Runnable
            public void run() {
                synchronized (UserBackupManagerService.this.mQueueLock) {
                    try {
                        ByteArrayOutputStream bufStream = new ByteArrayOutputStream(4096);
                        DataOutputStream bufOut = new DataOutputStream(bufStream);
                        bufOut.writeInt(1);
                        int numPackages = UserBackupManagerService.this.mFullBackupQueue.size();
                        bufOut.writeInt(numPackages);
                        for (int i = 0; i < numPackages; i++) {
                            FullBackupEntry entry = (FullBackupEntry) UserBackupManagerService.this.mFullBackupQueue.get(i);
                            bufOut.writeUTF(entry.packageName);
                            bufOut.writeLong(entry.lastBackup);
                        }
                        bufOut.flush();
                        AtomicFile af = new AtomicFile(UserBackupManagerService.this.mFullBackupScheduleFile);
                        FileOutputStream out = af.startWrite();
                        out.write(bufStream.toByteArray());
                        af.finishWrite(out);
                    } catch (Exception e) {
                        Slog.e(BackupManagerService.TAG, UserBackupManagerService.addUserIdToLogMessage(UserBackupManagerService.this.mUserId, "Unable to write backup schedule!"), e);
                    }
                }
            }
        };
        this.mPackageTrackingReceiver = new AnonymousClass2();
        this.mUserId = userId;
        Context context2 = (Context) Objects.requireNonNull(context, "context cannot be null");
        this.mContext = context2;
        PackageManager packageManager = context.getPackageManager();
        this.mPackageManager = packageManager;
        this.mPackageManagerBinder = AppGlobals.getPackageManager();
        this.mActivityManager = ActivityManager.getService();
        this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mScheduledBackupEligibility = getEligibilityRules(packageManager, userId, 0);
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        Objects.requireNonNull(parent, "parent cannot be null");
        this.mBackupManagerBinder = BackupManagerService.asInterface(parent.asBinder());
        BackupAgentTimeoutParameters backupAgentTimeoutParameters = new BackupAgentTimeoutParameters(Handler.getMain(), context2.getContentResolver());
        this.mAgentTimeoutParameters = backupAgentTimeoutParameters;
        backupAgentTimeoutParameters.start();
        LifecycleOperationStorage lifecycleOperationStorage = new LifecycleOperationStorage(userId);
        this.mOperationStorage = lifecycleOperationStorage;
        Objects.requireNonNull(userBackupThread, "userBackupThread cannot be null");
        BackupHandler backupHandler = new BackupHandler(this, lifecycleOperationStorage, userBackupThread);
        this.mBackupHandler = backupHandler;
        ContentResolver resolver = context.getContentResolver();
        this.mSetupComplete = getSetupCompleteSettingForUser(context, userId);
        this.mAutoRestore = Settings.Secure.getIntForUser(resolver, "backup_auto_restore", 1, userId) != 0;
        SetupObserver setupObserver = new SetupObserver(this, backupHandler);
        this.mSetupObserver = setupObserver;
        resolver.registerContentObserver(Settings.Secure.getUriFor("user_setup_complete"), false, setupObserver, userId);
        File file = (File) Objects.requireNonNull(baseStateDir, "baseStateDir cannot be null");
        this.mBaseStateDir = file;
        if (userId == 0) {
            file.mkdirs();
            if (!SELinux.restorecon(file)) {
                Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(userId, "SELinux restorecon failed on " + file));
            }
        }
        this.mDataDir = (File) Objects.requireNonNull(dataDir, "dataDir cannot be null");
        this.mBackupPasswordManager = new BackupPasswordManager(context2, file, secureRandom);
        RunInitializeReceiver runInitializeReceiver = new RunInitializeReceiver(this);
        this.mRunInitReceiver = runInitializeReceiver;
        IntentFilter filter = new IntentFilter();
        filter.addAction(RUN_INITIALIZE_ACTION);
        context.registerReceiverAsUser(runInitializeReceiver, UserHandle.of(userId), filter, "android.permission.BACKUP", null);
        Intent initIntent = new Intent(RUN_INITIALIZE_ACTION);
        initIntent.addFlags(1073741824);
        this.mRunInitIntent = PendingIntent.getBroadcastAsUser(context, 0, initIntent, 67108864, UserHandle.of(userId));
        File file2 = new File(file, "pending");
        this.mJournalDir = file2;
        file2.mkdirs();
        this.mJournal = null;
        BackupManagerConstants backupManagerConstants = new BackupManagerConstants(backupHandler, context2.getContentResolver());
        this.mConstants = backupManagerConstants;
        backupManagerConstants.start();
        synchronized (sparseArray) {
            try {
                addPackageParticipantsLocked(null);
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
        final TransportManager transportManager2 = (TransportManager) Objects.requireNonNull(transportManager, "transportManager cannot be null");
        this.mTransportManager = transportManager2;
        transportManager2.setOnTransportRegisteredListener(new OnTransportRegisteredListener() { // from class: com.android.server.backup.UserBackupManagerService$$ExternalSyntheticLambda0
            @Override // com.android.server.backup.transport.OnTransportRegisteredListener
            public final void onTransportRegistered(String str, String str2) {
                UserBackupManagerService.this.onTransportRegistered(str, str2);
            }
        });
        this.mRegisterTransportsRequestedTime = SystemClock.elapsedRealtime();
        Objects.requireNonNull(transportManager2);
        backupHandler.postDelayed(new Runnable() { // from class: com.android.server.backup.UserBackupManagerService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                TransportManager.this.registerTransports();
            }
        }, 3000L);
        backupHandler.postDelayed(new Runnable() { // from class: com.android.server.backup.UserBackupManagerService$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                UserBackupManagerService.this.parseLeftoverJournals();
            }
        }, 3000L);
        this.mBackupPreferences = new UserBackupPreferences(context2, file);
        this.mWakelock = new BackupWakeLock(this.mPowerManager.newWakeLock(1, "*backup*-" + userId + "-" + userBackupThread.getThreadId()), userId);
        this.mFullBackupScheduleFile = new File(file, "fb-schedule");
        initPackageTracking();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void initializeBackupEnableState() {
        boolean isEnabled = readEnabledState();
        setBackupEnabled(isEnabled, false);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void tearDownService() {
        this.mAgentTimeoutParameters.stop();
        this.mConstants.stop();
        this.mContext.getContentResolver().unregisterContentObserver(this.mSetupObserver);
        this.mContext.unregisterReceiver(this.mRunInitReceiver);
        this.mContext.unregisterReceiver(this.mPackageTrackingReceiver);
        this.mBackupHandler.stop();
    }

    public int getUserId() {
        return this.mUserId;
    }

    public BackupManagerConstants getConstants() {
        return this.mConstants;
    }

    public BackupAgentTimeoutParameters getAgentTimeoutParameters() {
        return this.mAgentTimeoutParameters;
    }

    public Context getContext() {
        return this.mContext;
    }

    public PackageManager getPackageManager() {
        return this.mPackageManager;
    }

    public IPackageManager getPackageManagerBinder() {
        return this.mPackageManagerBinder;
    }

    public IActivityManager getActivityManager() {
        return this.mActivityManager;
    }

    public AlarmManager getAlarmManager() {
        return this.mAlarmManager;
    }

    void setPowerManager(PowerManager powerManager) {
        this.mPowerManager = powerManager;
    }

    public TransportManager getTransportManager() {
        return this.mTransportManager;
    }

    public OperationStorage getOperationStorage() {
        return this.mOperationStorage;
    }

    public boolean isEnabled() {
        return this.mEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.mEnabled = enabled;
    }

    public boolean isSetupComplete() {
        return this.mSetupComplete;
    }

    public void setSetupComplete(boolean setupComplete) {
        this.mSetupComplete = setupComplete;
    }

    public BackupWakeLock getWakelock() {
        return this.mWakelock;
    }

    public void setWorkSource(WorkSource workSource) {
        this.mWakelock.mPowerManagerWakeLock.setWorkSource(workSource);
    }

    public Handler getBackupHandler() {
        return this.mBackupHandler;
    }

    public PendingIntent getRunInitIntent() {
        return this.mRunInitIntent;
    }

    public HashMap<String, BackupRequest> getPendingBackups() {
        return this.mPendingBackups;
    }

    public Object getQueueLock() {
        return this.mQueueLock;
    }

    public boolean isBackupRunning() {
        return this.mBackupRunning;
    }

    public void setBackupRunning(boolean backupRunning) {
        this.mBackupRunning = backupRunning;
    }

    public void setLastBackupPass(long lastBackupPass) {
        this.mLastBackupPass = lastBackupPass;
    }

    public Object getClearDataLock() {
        return this.mClearDataLock;
    }

    public void setClearingData(boolean clearingData) {
        this.mClearingData = clearingData;
    }

    public boolean isRestoreInProgress() {
        return this.mIsRestoreInProgress;
    }

    public void setRestoreInProgress(boolean restoreInProgress) {
        this.mIsRestoreInProgress = restoreInProgress;
    }

    public Queue<PerformUnifiedRestoreTask> getPendingRestores() {
        return this.mPendingRestores;
    }

    public ActiveRestoreSession getActiveRestoreSession() {
        return this.mActiveRestoreSession;
    }

    public SparseArray<AdbParams> getAdbBackupRestoreConfirmations() {
        return this.mAdbBackupRestoreConfirmations;
    }

    public File getBaseStateDir() {
        return this.mBaseStateDir;
    }

    public File getDataDir() {
        return this.mDataDir;
    }

    BroadcastReceiver getPackageTrackingReceiver() {
        return this.mPackageTrackingReceiver;
    }

    public DataChangedJournal getJournal() {
        return this.mJournal;
    }

    public void setJournal(DataChangedJournal journal) {
        this.mJournal = journal;
    }

    public SecureRandom getRng() {
        return this.mRng;
    }

    public void setAncestralPackages(Set<String> ancestralPackages) {
        this.mAncestralPackages = ancestralPackages;
    }

    public void setAncestralToken(long ancestralToken) {
        this.mAncestralToken = ancestralToken;
    }

    public void setAncestralOperationType(int operationType) {
        this.mAncestralOperationType = operationType;
    }

    public long getCurrentToken() {
        return this.mCurrentToken;
    }

    public void setCurrentToken(long currentToken) {
        this.mCurrentToken = currentToken;
    }

    public ArraySet<String> getPendingInits() {
        return this.mPendingInits;
    }

    public void clearPendingInits() {
        this.mPendingInits.clear();
    }

    public void setRunningFullBackupTask(PerformFullTransportBackupTask runningFullBackupTask) {
        this.mRunningFullBackupTask = runningFullBackupTask;
    }

    public int generateRandomIntegerToken() {
        int token = this.mTokenGenerator.nextInt();
        if (token < 0) {
            token = -token;
        }
        return (token & (-256)) | (this.mNextToken.incrementAndGet() & 255);
    }

    public BackupAgent makeMetadataAgent() {
        return makeMetadataAgentWithEligibilityRules(this.mScheduledBackupEligibility);
    }

    public BackupAgent makeMetadataAgentWithEligibilityRules(BackupEligibilityRules backupEligibilityRules) {
        PackageManagerBackupAgent pmAgent = new PackageManagerBackupAgent(this.mPackageManager, this.mUserId, backupEligibilityRules);
        pmAgent.attach(this.mContext);
        pmAgent.onCreate(UserHandle.of(this.mUserId));
        return pmAgent;
    }

    public PackageManagerBackupAgent makeMetadataAgent(List<PackageInfo> packages) {
        PackageManagerBackupAgent pmAgent = new PackageManagerBackupAgent(this.mPackageManager, packages, this.mUserId);
        pmAgent.attach(this.mContext);
        pmAgent.onCreate(UserHandle.of(this.mUserId));
        return pmAgent;
    }

    private void initPackageTracking() {
        this.mTokenFile = new File(this.mBaseStateDir, "ancestral");
        try {
            DataInputStream tokenStream = new DataInputStream(new BufferedInputStream(new FileInputStream(this.mTokenFile)));
            int version = tokenStream.readInt();
            if (version == 1) {
                this.mAncestralToken = tokenStream.readLong();
                this.mCurrentToken = tokenStream.readLong();
                int numPackages = tokenStream.readInt();
                if (numPackages >= 0) {
                    this.mAncestralPackages = new HashSet();
                    for (int i = 0; i < numPackages; i++) {
                        String pkgName = tokenStream.readUTF();
                        this.mAncestralPackages.add(pkgName);
                    }
                }
            }
            tokenStream.close();
        } catch (FileNotFoundException e) {
            Slog.v(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "No ancestral data"));
        } catch (IOException e2) {
            Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Unable to read token file"), e2);
        }
        ProcessedPackagesJournal processedPackagesJournal = new ProcessedPackagesJournal(this.mBaseStateDir);
        this.mProcessedPackagesJournal = processedPackagesJournal;
        processedPackagesJournal.init();
        synchronized (this.mQueueLock) {
            this.mFullBackupQueue = readFullBackupSchedule();
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        filter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mPackageTrackingReceiver, UserHandle.of(this.mUserId), filter, null, null);
        IntentFilter sdFilter = new IntentFilter();
        sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
        sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        this.mContext.registerReceiverAsUser(this.mPackageTrackingReceiver, UserHandle.of(this.mUserId), sdFilter, null, null);
    }

    /* JADX WARN: Can't wrap try/catch for region: R(10:44|(4:45|46|47|(2:48|49))|(3:66|67|(7:69|70|71|72|73|74|59)(1:78))(1:51)|52|53|54|55|56|58|59) */
    /* JADX WARN: Not initialized variable reg: 21, insn: 0x0184: MOVE  (r4 I:??[int, float, boolean, short, byte, char, OBJECT, ARRAY]) = (r21 I:??[int, float, boolean, short, byte, char, OBJECT, ARRAY] A[D('changed' boolean)]), block:B:71:0x0183 */
    /* JADX WARN: Removed duplicated region for block: B:107:0x01d2  */
    /* JADX WARN: Removed duplicated region for block: B:119:0x0214  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private ArrayList<FullBackupEntry> readFullBackupSchedule() {
        Throwable th;
        Throwable th2;
        Throwable th3;
        HashSet<String> foundApps;
        boolean changed;
        int version;
        int numPackages;
        PackageInfo pkg;
        long lastBackup;
        boolean changed2 = false;
        ArrayList<FullBackupEntry> schedule = null;
        List<PackageInfo> apps = PackageManagerBackupAgent.getStorableApplications(this.mPackageManager, this.mUserId, this.mScheduledBackupEligibility);
        if (this.mFullBackupScheduleFile.exists()) {
            try {
                FileInputStream fstream = new FileInputStream(this.mFullBackupScheduleFile);
                try {
                    try {
                        BufferedInputStream bufStream = new BufferedInputStream(fstream);
                        try {
                            try {
                                DataInputStream in = new DataInputStream(bufStream);
                                try {
                                    try {
                                        int version2 = in.readInt();
                                        try {
                                            if (version2 != 1) {
                                                Slog.e(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Unknown backup schedule version " + version2));
                                                throw new IllegalArgumentException("Unknown backup schedule version");
                                            }
                                            int numPackages2 = in.readInt();
                                            schedule = new ArrayList<>(numPackages2);
                                            HashSet<String> foundApps2 = new HashSet<>(numPackages2);
                                            int i = 0;
                                            while (i < numPackages2) {
                                                String pkgName = in.readUTF();
                                                long lastBackup2 = in.readLong();
                                                foundApps2.add(pkgName);
                                                try {
                                                    changed = changed2;
                                                    try {
                                                        pkg = this.mPackageManager.getPackageInfoAsUser(pkgName, 0, this.mUserId);
                                                    } catch (PackageManager.NameNotFoundException e) {
                                                        version = version2;
                                                        numPackages = numPackages2;
                                                    }
                                                } catch (PackageManager.NameNotFoundException e2) {
                                                    changed = changed2;
                                                    version = version2;
                                                    numPackages = numPackages2;
                                                }
                                                if (!this.mScheduledBackupEligibility.appGetsFullBackup(pkg)) {
                                                    version = version2;
                                                    numPackages = numPackages2;
                                                    lastBackup = lastBackup2;
                                                } else {
                                                    try {
                                                    } catch (PackageManager.NameNotFoundException e3) {
                                                        version = version2;
                                                        numPackages = numPackages2;
                                                    }
                                                    if (this.mScheduledBackupEligibility.appIsEligibleForBackup(pkg.applicationInfo)) {
                                                        version = version2;
                                                        numPackages = numPackages2;
                                                        try {
                                                            schedule.add(new FullBackupEntry(pkgName, lastBackup2));
                                                        } catch (PackageManager.NameNotFoundException e4) {
                                                            Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Package " + pkgName + " not installed; dropping from full backup"));
                                                            i++;
                                                            version2 = version;
                                                            numPackages2 = numPackages;
                                                            changed2 = changed;
                                                        }
                                                        i++;
                                                        version2 = version;
                                                        numPackages2 = numPackages;
                                                        changed2 = changed;
                                                    } else {
                                                        version = version2;
                                                        numPackages = numPackages2;
                                                        lastBackup = lastBackup2;
                                                    }
                                                }
                                                Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Package " + pkgName + " no longer eligible for full backup"));
                                                i++;
                                                version2 = version;
                                                numPackages2 = numPackages;
                                                changed2 = changed;
                                            }
                                            boolean changed3 = changed2;
                                            changed2 = changed3;
                                            for (PackageInfo app : apps) {
                                                try {
                                                    if (!this.mScheduledBackupEligibility.appGetsFullBackup(app)) {
                                                        foundApps = foundApps2;
                                                    } else if (!this.mScheduledBackupEligibility.appIsEligibleForBackup(app.applicationInfo)) {
                                                        foundApps = foundApps2;
                                                    } else if (foundApps2.contains(app.packageName)) {
                                                        foundApps = foundApps2;
                                                    } else {
                                                        foundApps = foundApps2;
                                                        schedule.add(new FullBackupEntry(app.packageName, 0L));
                                                        changed2 = true;
                                                    }
                                                    foundApps2 = foundApps;
                                                } catch (Throwable th4) {
                                                    th3 = th4;
                                                    in.close();
                                                    throw th3;
                                                }
                                            }
                                            Collections.sort(schedule);
                                            in.close();
                                            bufStream.close();
                                            fstream.close();
                                        } catch (Throwable th5) {
                                            th3 = th5;
                                        }
                                    } catch (Throwable th6) {
                                        th3 = th6;
                                    }
                                } catch (Throwable th7) {
                                    th2 = th7;
                                    bufStream.close();
                                    throw th2;
                                }
                            } catch (Throwable th8) {
                                th = th8;
                                fstream.close();
                                throw th;
                            }
                        } catch (Throwable th9) {
                            th2 = th9;
                        }
                    } catch (Exception e5) {
                        e = e5;
                        Slog.e(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Unable to read backup schedule"), e);
                        this.mFullBackupScheduleFile.delete();
                        schedule = null;
                        if (schedule == null) {
                        }
                        if (changed2) {
                        }
                        return schedule;
                    }
                } catch (Throwable th10) {
                    th = th10;
                }
            } catch (Exception e6) {
                e = e6;
            }
        }
        if (schedule == null) {
            changed2 = true;
            schedule = new ArrayList<>(apps.size());
            for (PackageInfo info : apps) {
                if (this.mScheduledBackupEligibility.appGetsFullBackup(info) && this.mScheduledBackupEligibility.appIsEligibleForBackup(info.applicationInfo)) {
                    schedule.add(new FullBackupEntry(info.packageName, 0L));
                }
            }
        }
        if (changed2) {
            writeFullBackupScheduleAsync();
        }
        return schedule;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeFullBackupScheduleAsync() {
        this.mBackupHandler.removeCallbacks(this.mFullBackupScheduleWriter);
        this.mBackupHandler.post(this.mFullBackupScheduleWriter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void parseLeftoverJournals() {
        ArrayList<DataChangedJournal> journals = DataChangedJournal.listJournals(this.mJournalDir);
        journals.removeAll(Collections.singletonList(this.mJournal));
        if (!journals.isEmpty()) {
            Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Found " + journals.size() + " stale backup journal(s), scheduling."));
        }
        final Set<String> packageNames = new LinkedHashSet<>();
        Iterator<DataChangedJournal> it = journals.iterator();
        while (it.hasNext()) {
            DataChangedJournal journal = it.next();
            try {
                journal.forEach(new Consumer() { // from class: com.android.server.backup.UserBackupManagerService$$ExternalSyntheticLambda6
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        UserBackupManagerService.this.m2201x3dc6722b(packageNames, (String) obj);
                    }
                });
            } catch (IOException e) {
                Slog.e(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Can't read " + journal), e);
            }
        }
        if (!packageNames.isEmpty()) {
            String msg = "Stale backup journals: Scheduled " + packageNames.size() + " package(s) total";
            Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, msg));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$parseLeftoverJournals$0$com-android-server-backup-UserBackupManagerService  reason: not valid java name */
    public /* synthetic */ void m2201x3dc6722b(Set packageNames, String packageName) {
        if (packageNames.add(packageName)) {
            dataChangedImpl(packageName);
        }
    }

    public Set<String> getExcludedRestoreKeys(String packageName) {
        return this.mBackupPreferences.getExcludedRestoreKeysForPackage(packageName);
    }

    public byte[] randomBytes(int bits) {
        byte[] array = new byte[bits / 8];
        this.mRng.nextBytes(array);
        return array;
    }

    public boolean setBackupPassword(String currentPw, String newPw) {
        return this.mBackupPasswordManager.setBackupPassword(currentPw, newPw);
    }

    public boolean hasBackupPassword() {
        return this.mBackupPasswordManager.hasBackupPassword();
    }

    public boolean backupPasswordMatches(String currentPw) {
        return this.mBackupPasswordManager.backupPasswordMatches(currentPw);
    }

    public void recordInitPending(boolean isPending, String transportName, String transportDirName) {
        synchronized (this.mQueueLock) {
            File stateDir = new File(this.mBaseStateDir, transportDirName);
            File initPendingFile = new File(stateDir, INIT_SENTINEL_FILE_NAME);
            if (isPending) {
                this.mPendingInits.add(transportName);
                try {
                    new FileOutputStream(initPendingFile).close();
                } catch (IOException e) {
                }
            } else {
                initPendingFile.delete();
                this.mPendingInits.remove(transportName);
            }
        }
    }

    public void resetBackupState(File stateFileDir) {
        File[] listFiles;
        synchronized (this.mQueueLock) {
            this.mProcessedPackagesJournal.reset();
            this.mCurrentToken = 0L;
            writeRestoreTokens();
            for (File sf : stateFileDir.listFiles()) {
                if (!sf.getName().equals(INIT_SENTINEL_FILE_NAME)) {
                    sf.delete();
                }
            }
        }
        synchronized (this.mBackupParticipants) {
            int numParticipants = this.mBackupParticipants.size();
            for (int i = 0; i < numParticipants; i++) {
                HashSet<String> participants = this.mBackupParticipants.valueAt(i);
                if (participants != null) {
                    Iterator<String> it = participants.iterator();
                    while (it.hasNext()) {
                        String packageName = it.next();
                        dataChangedImpl(packageName);
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onTransportRegistered(String transportName, String transportDirName) {
        long timeMs = SystemClock.elapsedRealtime() - this.mRegisterTransportsRequestedTime;
        Slog.d(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Transport " + transportName + " registered " + timeMs + "ms after first request (delay = 3000ms)"));
        File stateDir = new File(this.mBaseStateDir, transportDirName);
        stateDir.mkdirs();
        File initSentinel = new File(stateDir, INIT_SENTINEL_FILE_NAME);
        if (initSentinel.exists()) {
            synchronized (this.mQueueLock) {
                this.mPendingInits.add(transportName);
                this.mAlarmManager.set(0, System.currentTimeMillis() + 60000, this.mRunInitIntent);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.backup.UserBackupManagerService$2  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass2 extends BroadcastReceiver {
        AnonymousClass2() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String[] packageList;
            boolean changed;
            boolean added;
            String action;
            PackageInfo app;
            String action2 = intent.getAction();
            Bundle extras = intent.getExtras();
            if ("android.intent.action.PACKAGE_ADDED".equals(action2) || "android.intent.action.PACKAGE_REMOVED".equals(action2) || "android.intent.action.PACKAGE_CHANGED".equals(action2)) {
                Uri uri = intent.getData();
                if (uri == null) {
                    return;
                }
                final String packageName = uri.getSchemeSpecificPart();
                String[] packageList2 = packageName != null ? new String[]{packageName} : null;
                boolean changed2 = "android.intent.action.PACKAGE_CHANGED".equals(action2);
                if (changed2) {
                    final String[] components = intent.getStringArrayExtra("android.intent.extra.changed_component_name_list");
                    UserBackupManagerService.this.mBackupHandler.post(new Runnable() { // from class: com.android.server.backup.UserBackupManagerService$2$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            UserBackupManagerService.AnonymousClass2.this.m2207xeeb81d9f(packageName, components);
                        }
                    });
                    return;
                }
                boolean added2 = "android.intent.action.PACKAGE_ADDED".equals(action2);
                boolean replacing = extras.getBoolean("android.intent.extra.REPLACING", false);
                packageList = packageList2;
                changed = added2;
                added = replacing;
            } else if ("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE".equals(action2)) {
                String[] packageList3 = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                packageList = packageList3;
                changed = true;
                added = false;
            } else if (!"android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(action2)) {
                packageList = null;
                changed = false;
                added = false;
            } else {
                String[] packageList4 = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                packageList = packageList4;
                changed = false;
                added = false;
            }
            if (packageList != null && packageList.length != 0) {
                int uid = extras.getInt("android.intent.extra.UID");
                if (changed) {
                    synchronized (UserBackupManagerService.this.mBackupParticipants) {
                        if (added) {
                            try {
                                UserBackupManagerService.this.removePackageParticipantsLocked(packageList, uid);
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
                        try {
                            UserBackupManagerService.this.addPackageParticipantsLocked(packageList);
                            long now = System.currentTimeMillis();
                            int length = packageList.length;
                            int i = 0;
                            while (i < length) {
                                final String packageName2 = packageList[i];
                                try {
                                    app = UserBackupManagerService.this.mPackageManager.getPackageInfoAsUser(packageName2, 0, UserBackupManagerService.this.mUserId);
                                } catch (PackageManager.NameNotFoundException e) {
                                    e = e;
                                    action = action2;
                                }
                                if (!UserBackupManagerService.this.mScheduledBackupEligibility.appGetsFullBackup(app)) {
                                    action = action2;
                                } else if (!UserBackupManagerService.this.mScheduledBackupEligibility.appIsEligibleForBackup(app.applicationInfo)) {
                                    action = action2;
                                } else {
                                    UserBackupManagerService.this.enqueueFullBackup(packageName2, now);
                                    action = action2;
                                    try {
                                        UserBackupManagerService.this.scheduleNextFullBackupJob(0L);
                                        UserBackupManagerService.this.mBackupHandler.post(new Runnable() { // from class: com.android.server.backup.UserBackupManagerService$2$$ExternalSyntheticLambda1
                                            @Override // java.lang.Runnable
                                            public final void run() {
                                                UserBackupManagerService.AnonymousClass2.this.m2208x8d39c3e(packageName2);
                                            }
                                        });
                                    } catch (PackageManager.NameNotFoundException e2) {
                                        e = e2;
                                        Slog.w(BackupManagerService.TAG, UserBackupManagerService.addUserIdToLogMessage(UserBackupManagerService.this.mUserId, "Can't resolve new app " + packageName2));
                                        i++;
                                        action2 = action;
                                    }
                                    i++;
                                    action2 = action;
                                }
                                synchronized (UserBackupManagerService.this.mQueueLock) {
                                    UserBackupManagerService.this.dequeueFullBackupLocked(packageName2);
                                }
                                UserBackupManagerService.this.writeFullBackupScheduleAsync();
                                UserBackupManagerService.this.mBackupHandler.post(new Runnable() { // from class: com.android.server.backup.UserBackupManagerService$2$$ExternalSyntheticLambda1
                                    @Override // java.lang.Runnable
                                    public final void run() {
                                        UserBackupManagerService.AnonymousClass2.this.m2208x8d39c3e(packageName2);
                                    }
                                });
                                i++;
                                action2 = action;
                            }
                            UserBackupManagerService.this.dataChangedImpl(UserBackupManagerService.PACKAGE_MANAGER_SENTINEL);
                            return;
                        } catch (Throwable th3) {
                            th = th3;
                            while (true) {
                                break;
                                break;
                            }
                            throw th;
                        }
                    }
                }
                if (!added) {
                    synchronized (UserBackupManagerService.this.mBackupParticipants) {
                        UserBackupManagerService.this.removePackageParticipantsLocked(packageList, uid);
                    }
                }
                for (final String packageName3 : packageList) {
                    UserBackupManagerService.this.mBackupHandler.post(new Runnable() { // from class: com.android.server.backup.UserBackupManagerService$2$$ExternalSyntheticLambda2
                        @Override // java.lang.Runnable
                        public final void run() {
                            UserBackupManagerService.AnonymousClass2.this.m2209x22ef1add(packageName3);
                        }
                    });
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onReceive$0$com-android-server-backup-UserBackupManagerService$2  reason: not valid java name */
        public /* synthetic */ void m2207xeeb81d9f(String packageName, String[] components) {
            UserBackupManagerService.this.mTransportManager.onPackageChanged(packageName, components);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onReceive$1$com-android-server-backup-UserBackupManagerService$2  reason: not valid java name */
        public /* synthetic */ void m2208x8d39c3e(String packageName) {
            UserBackupManagerService.this.mTransportManager.onPackageAdded(packageName);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onReceive$2$com-android-server-backup-UserBackupManagerService$2  reason: not valid java name */
        public /* synthetic */ void m2209x22ef1add(String packageName) {
            UserBackupManagerService.this.mTransportManager.onPackageRemoved(packageName);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addPackageParticipantsLocked(String[] packageNames) {
        List<PackageInfo> targetApps = allAgentPackages();
        if (packageNames != null) {
            for (String packageName : packageNames) {
                addPackageParticipantsLockedInner(packageName, targetApps);
            }
            return;
        }
        addPackageParticipantsLockedInner(null, targetApps);
    }

    private void addPackageParticipantsLockedInner(String packageName, List<PackageInfo> targetPkgs) {
        for (PackageInfo pkg : targetPkgs) {
            if (packageName == null || pkg.packageName.equals(packageName)) {
                int uid = pkg.applicationInfo.uid;
                HashSet<String> set = this.mBackupParticipants.get(uid);
                if (set == null) {
                    set = new HashSet<>();
                    this.mBackupParticipants.put(uid, set);
                }
                set.add(pkg.packageName);
                Message msg = this.mBackupHandler.obtainMessage(16, pkg.packageName);
                this.mBackupHandler.sendMessage(msg);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removePackageParticipantsLocked(String[] packageNames, int oldUid) {
        if (packageNames == null) {
            Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "removePackageParticipants with null list"));
            return;
        }
        for (String pkg : packageNames) {
            HashSet<String> set = this.mBackupParticipants.get(oldUid);
            if (set != null && set.contains(pkg)) {
                removePackageFromSetLocked(set, pkg);
                if (set.isEmpty()) {
                    this.mBackupParticipants.remove(oldUid);
                }
            }
        }
    }

    private void removePackageFromSetLocked(HashSet<String> set, String packageName) {
        if (set.contains(packageName)) {
            set.remove(packageName);
            this.mPendingBackups.remove(packageName);
        }
    }

    private List<PackageInfo> allAgentPackages() {
        ApplicationInfo app;
        List<PackageInfo> packages = this.mPackageManager.getInstalledPackagesAsUser(134217728, this.mUserId);
        int numPackages = packages.size();
        for (int a = numPackages - 1; a >= 0; a--) {
            PackageInfo pkg = packages.get(a);
            try {
                app = pkg.applicationInfo;
            } catch (PackageManager.NameNotFoundException e) {
                packages.remove(a);
            }
            if ((app.flags & 32768) != 0 && app.backupAgentName != null && (app.flags & 67108864) == 0) {
                ApplicationInfo app2 = this.mPackageManager.getApplicationInfoAsUser(pkg.packageName, 1024, this.mUserId);
                pkg.applicationInfo.sharedLibraryFiles = app2.sharedLibraryFiles;
                pkg.applicationInfo.sharedLibraryInfos = app2.sharedLibraryInfos;
            }
            packages.remove(a);
        }
        return packages;
    }

    public void logBackupComplete(String packageName) {
        String[] backupFinishedNotificationReceivers;
        if (packageName.equals(PACKAGE_MANAGER_SENTINEL)) {
            return;
        }
        for (String receiver : this.mConstants.getBackupFinishedNotificationReceivers()) {
            Intent notification = new Intent();
            notification.setAction(BACKUP_FINISHED_ACTION);
            notification.setPackage(receiver);
            notification.addFlags(268435488);
            notification.putExtra("packageName", packageName);
            this.mContext.sendBroadcastAsUser(notification, UserHandle.of(this.mUserId));
        }
        this.mProcessedPackagesJournal.addPackage(packageName);
    }

    public void writeRestoreTokens() {
        try {
            RandomAccessFile af = new RandomAccessFile(this.mTokenFile, "rwd");
            af.writeInt(1);
            af.writeLong(this.mAncestralToken);
            af.writeLong(this.mCurrentToken);
            Set<String> set = this.mAncestralPackages;
            if (set == null) {
                af.writeInt(-1);
            } else {
                af.writeInt(set.size());
                Slog.v(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Ancestral packages:  " + this.mAncestralPackages.size()));
                for (String pkgName : this.mAncestralPackages) {
                    af.writeUTF(pkgName);
                }
            }
            af.close();
        } catch (IOException e) {
            Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Unable to write token file:"), e);
        }
    }

    public IBackupAgent bindToAgentSynchronous(ApplicationInfo app, int mode, int operationType) {
        IBackupAgent agent = null;
        synchronized (this.mAgentConnectLock) {
            this.mConnecting = true;
            this.mConnectedAgent = null;
            try {
                if (this.mActivityManager.bindBackupAgent(app.packageName, mode, this.mUserId, operationType)) {
                    Slog.d(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "awaiting agent for " + app));
                    long timeoutMark = System.currentTimeMillis() + 10000;
                    while (this.mConnecting && this.mConnectedAgent == null && System.currentTimeMillis() < timeoutMark) {
                        try {
                            this.mAgentConnectLock.wait(5000L);
                        } catch (InterruptedException e) {
                            Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Interrupted: " + e));
                            this.mConnecting = false;
                            this.mConnectedAgent = null;
                        }
                    }
                    if (this.mConnecting) {
                        Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Timeout waiting for agent " + app));
                        this.mConnectedAgent = null;
                    }
                    Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "got agent " + this.mConnectedAgent));
                    agent = this.mConnectedAgent;
                }
            } catch (RemoteException e2) {
            }
        }
        if (agent == null) {
            this.mActivityManagerInternal.clearPendingBackup(this.mUserId);
        }
        return agent;
    }

    public void unbindAgent(ApplicationInfo app) {
        try {
            this.mActivityManager.unbindBackupAgent(app);
        } catch (RemoteException e) {
        }
    }

    public void clearApplicationDataAfterRestoreFailure(String packageName) {
        clearApplicationDataSynchronous(packageName, true, false);
    }

    public void clearApplicationDataBeforeRestore(String packageName) {
        clearApplicationDataSynchronous(packageName, false, true);
    }

    private void clearApplicationDataSynchronous(String packageName, boolean checkFlagAllowClearUserDataOnFailedRestore, boolean keepSystemState) {
        boolean shouldClearData;
        try {
            ApplicationInfo applicationInfo = this.mPackageManager.getPackageInfoAsUser(packageName, 0, this.mUserId).applicationInfo;
            if (checkFlagAllowClearUserDataOnFailedRestore && applicationInfo.targetSdkVersion >= 29) {
                shouldClearData = (applicationInfo.privateFlags & 67108864) != 0;
            } else {
                shouldClearData = (applicationInfo.flags & 64) != 0;
            }
            if (!shouldClearData) {
                return;
            }
            ClearDataObserver observer = new ClearDataObserver(this);
            synchronized (this.mClearDataLock) {
                this.mClearingData = true;
                try {
                    this.mActivityManager.clearApplicationUserData(packageName, keepSystemState, observer, this.mUserId);
                } catch (RemoteException e) {
                }
                long timeoutMark = System.currentTimeMillis() + 30000;
                while (this.mClearingData && System.currentTimeMillis() < timeoutMark) {
                    try {
                        this.mClearDataLock.wait(5000L);
                    } catch (InterruptedException e2) {
                        this.mClearingData = false;
                        Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Interrupted while waiting for " + packageName + " data to be cleared"), e2);
                    }
                }
                if (this.mClearingData) {
                    Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Clearing app data for " + packageName + " timed out"));
                }
            }
        } catch (PackageManager.NameNotFoundException e3) {
            Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Tried to clear data for " + packageName + " but not found"));
        }
    }

    private BackupEligibilityRules getEligibilityRulesForRestoreAtInstall(long restoreToken) {
        if (this.mAncestralOperationType == 1 && restoreToken == this.mAncestralToken) {
            return getEligibilityRulesForOperation(1);
        }
        return this.mScheduledBackupEligibility;
    }

    public long getAvailableRestoreToken(String packageName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getAvailableRestoreToken");
        long token = this.mAncestralToken;
        synchronized (this.mQueueLock) {
            if (this.mCurrentToken != 0 && this.mProcessedPackagesJournal.hasBeenProcessed(packageName)) {
                token = this.mCurrentToken;
            }
        }
        return token;
    }

    public int requestBackup(String[] packages, IBackupObserver observer, int flags) {
        return requestBackup(packages, observer, null, flags);
    }

    public int requestBackup(String[] packages, IBackupObserver observer, IBackupManagerMonitor monitor, int flags) {
        int logTag;
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "requestBackup");
        if (packages == null || packages.length < 1) {
            Slog.e(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "No packages named for backup request"));
            BackupObserverUtils.sendBackupFinished(observer, -1000);
            BackupManagerMonitorUtils.monitorEvent(monitor, 49, null, 1, null);
            throw new IllegalArgumentException("No packages are provided for backup");
        } else if (!this.mEnabled || !this.mSetupComplete) {
            Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Backup requested but enabled=" + this.mEnabled + " setupComplete=" + this.mSetupComplete));
            BackupObserverUtils.sendBackupFinished(observer, -2001);
            if (this.mSetupComplete) {
                logTag = 13;
            } else {
                logTag = 14;
            }
            BackupManagerMonitorUtils.monitorEvent(monitor, logTag, null, 3, null);
            return -2001;
        } else {
            try {
                TransportManager transportManager = this.mTransportManager;
                String transportDirName = transportManager.getTransportDirName(transportManager.getCurrentTransportName());
                final TransportConnection transportConnection = this.mTransportManager.getCurrentTransportClientOrThrow("BMS.requestBackup()");
                int operationType = getOperationTypeFromTransport(transportConnection);
                OnTaskFinishedListener listener = new OnTaskFinishedListener() { // from class: com.android.server.backup.UserBackupManagerService$$ExternalSyntheticLambda9
                    @Override // com.android.server.backup.internal.OnTaskFinishedListener
                    public final void onFinished(String str) {
                        UserBackupManagerService.this.m2202x7948a623(transportConnection, str);
                    }
                };
                BackupEligibilityRules backupEligibilityRules = getEligibilityRulesForOperation(operationType);
                Message msg = this.mBackupHandler.obtainMessage(15);
                msg.obj = getRequestBackupParams(packages, observer, monitor, flags, backupEligibilityRules, transportConnection, transportDirName, listener);
                this.mBackupHandler.sendMessage(msg);
                return 0;
            } catch (RemoteException | TransportNotAvailableException | TransportNotRegisteredException e) {
                BackupObserverUtils.sendBackupFinished(observer, -1000);
                BackupManagerMonitorUtils.monitorEvent(monitor, 50, null, 1, null);
                return -1000;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$requestBackup$1$com-android-server-backup-UserBackupManagerService  reason: not valid java name */
    public /* synthetic */ void m2202x7948a623(TransportConnection transportConnection, String caller) {
        this.mTransportManager.disposeOfTransportClient(transportConnection, caller);
    }

    BackupParams getRequestBackupParams(String[] packages, IBackupObserver observer, IBackupManagerMonitor monitor, int flags, BackupEligibilityRules backupEligibilityRules, TransportConnection transportConnection, String transportDirName, OnTaskFinishedListener listener) {
        ArrayList<String> fullBackupList = new ArrayList<>();
        ArrayList<String> kvBackupList = new ArrayList<>();
        for (String packageName : packages) {
            if (!PACKAGE_MANAGER_SENTINEL.equals(packageName)) {
                try {
                    PackageInfo packageInfo = this.mPackageManager.getPackageInfoAsUser(packageName, 134217728, this.mUserId);
                    if (!backupEligibilityRules.appIsEligibleForBackup(packageInfo.applicationInfo)) {
                        BackupObserverUtils.sendBackupOnPackageResult(observer, packageName, -2001);
                    } else if (backupEligibilityRules.appGetsFullBackup(packageInfo)) {
                        fullBackupList.add(packageInfo.packageName);
                    } else {
                        kvBackupList.add(packageInfo.packageName);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    BackupObserverUtils.sendBackupOnPackageResult(observer, packageName, -2002);
                }
            } else {
                kvBackupList.add(packageName);
            }
        }
        EventLog.writeEvent((int) EventLogTags.BACKUP_REQUESTED, Integer.valueOf(packages.length), Integer.valueOf(kvBackupList.size()), Integer.valueOf(fullBackupList.size()));
        boolean nonIncrementalBackup = (flags & 1) != 0;
        return new BackupParams(transportConnection, transportDirName, kvBackupList, fullBackupList, observer, monitor, listener, true, nonIncrementalBackup, backupEligibilityRules);
    }

    public void cancelBackups() {
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "cancelBackups");
        long oldToken = Binder.clearCallingIdentity();
        try {
            Set<Integer> operationsToCancel = this.mOperationStorage.operationTokensForOpType(2);
            for (Integer token : operationsToCancel) {
                this.mOperationStorage.cancelOperation(token.intValue(), true, new IntConsumer() { // from class: com.android.server.backup.UserBackupManagerService$$ExternalSyntheticLambda8
                    @Override // java.util.function.IntConsumer
                    public final void accept(int i) {
                        UserBackupManagerService.lambda$cancelBackups$2(i);
                    }
                });
            }
            KeyValueBackupJob.schedule(this.mUserId, this.mContext, 3600000L, this.mConstants);
            FullBackupJob.schedule(this.mUserId, this.mContext, AppStandbyController.ConstantsObserver.DEFAULT_SYSTEM_UPDATE_TIMEOUT, this.mConstants);
        } finally {
            Binder.restoreCallingIdentity(oldToken);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$cancelBackups$2(int operationType) {
    }

    public void prepareOperationTimeout(int token, long interval, BackupRestoreTask callback, int operationType) {
        if (operationType != 0 && operationType != 1) {
            Slog.wtf(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "prepareOperationTimeout() doesn't support operation " + Integer.toHexString(token) + " of type " + operationType));
            return;
        }
        this.mOperationStorage.registerOperation(token, 0, callback, operationType);
        Message msg = this.mBackupHandler.obtainMessage(getMessageIdForOperationType(operationType), token, 0, callback);
        this.mBackupHandler.sendMessageDelayed(msg, interval);
    }

    private int getMessageIdForOperationType(int operationType) {
        switch (operationType) {
            case 0:
                return 17;
            case 1:
                return 18;
            default:
                Slog.wtf(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "getMessageIdForOperationType called on invalid operation type: " + operationType));
                return -1;
        }
    }

    public boolean waitUntilOperationComplete(int token) {
        return this.mOperationStorage.waitUntilOperationComplete(token, new IntConsumer() { // from class: com.android.server.backup.UserBackupManagerService$$ExternalSyntheticLambda13
            @Override // java.util.function.IntConsumer
            public final void accept(int i) {
                UserBackupManagerService.this.m2206xb9457f21(i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$waitUntilOperationComplete$3$com-android-server-backup-UserBackupManagerService  reason: not valid java name */
    public /* synthetic */ void m2206xb9457f21(int operationType) {
        this.mBackupHandler.removeMessages(getMessageIdForOperationType(operationType));
    }

    public void handleCancel(int token, boolean cancelAll) {
        this.mOperationStorage.cancelOperation(token, cancelAll, new IntConsumer() { // from class: com.android.server.backup.UserBackupManagerService$$ExternalSyntheticLambda7
            @Override // java.util.function.IntConsumer
            public final void accept(int i) {
                UserBackupManagerService.this.m2198x5d1f78b5(i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleCancel$4$com-android-server-backup-UserBackupManagerService  reason: not valid java name */
    public /* synthetic */ void m2198x5d1f78b5(int operationType) {
        if (operationType == 0 || operationType == 1) {
            this.mBackupHandler.removeMessages(getMessageIdForOperationType(operationType));
        }
    }

    public boolean isBackupOperationInProgress() {
        return this.mOperationStorage.isBackupOperationInProgress();
    }

    public void tearDownAgentAndKill(ApplicationInfo app) {
        if (app == null) {
            return;
        }
        try {
            this.mActivityManager.unbindBackupAgent(app);
            if (!UserHandle.isCore(app.uid) && !app.packageName.equals("com.android.backupconfirm")) {
                this.mActivityManager.killApplicationProcess(app.processName, app.uid);
            }
        } catch (RemoteException e) {
            Slog.d(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Lost app trying to shut down"));
        }
    }

    public void scheduleNextFullBackupJob(long transportMinLatency) {
        synchronized (this.mQueueLock) {
            try {
                try {
                    if (this.mFullBackupQueue.size() > 0) {
                        long upcomingLastBackup = this.mFullBackupQueue.get(0).lastBackup;
                        long timeSinceLast = System.currentTimeMillis() - upcomingLastBackup;
                        long interval = this.mConstants.getFullBackupIntervalMilliseconds();
                        long appLatency = timeSinceLast < interval ? interval - timeSinceLast : 0L;
                        long latency = Math.max(transportMinLatency, appLatency);
                        FullBackupJob.schedule(this.mUserId, this.mContext, latency, this.mConstants);
                    } else {
                        Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Full backup queue empty; not scheduling"));
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

    /* JADX INFO: Access modifiers changed from: private */
    public void dequeueFullBackupLocked(String packageName) {
        int numPackages = this.mFullBackupQueue.size();
        for (int i = numPackages - 1; i >= 0; i--) {
            FullBackupEntry e = this.mFullBackupQueue.get(i);
            if (packageName.equals(e.packageName)) {
                this.mFullBackupQueue.remove(i);
            }
        }
    }

    public void enqueueFullBackup(String packageName, long lastBackedUp) {
        FullBackupEntry newEntry = new FullBackupEntry(packageName, lastBackedUp);
        synchronized (this.mQueueLock) {
            dequeueFullBackupLocked(packageName);
            int which = -1;
            if (lastBackedUp > 0) {
                which = this.mFullBackupQueue.size() - 1;
                while (true) {
                    if (which < 0) {
                        break;
                    }
                    FullBackupEntry entry = this.mFullBackupQueue.get(which);
                    if (entry.lastBackup > lastBackedUp) {
                        which--;
                    } else {
                        this.mFullBackupQueue.add(which + 1, newEntry);
                        break;
                    }
                }
            }
            if (which < 0) {
                this.mFullBackupQueue.add(0, newEntry);
            }
        }
        writeFullBackupScheduleAsync();
    }

    private boolean fullBackupAllowable(String transportName) {
        if (!this.mTransportManager.isTransportRegistered(transportName)) {
            Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Transport not registered; full data backup not performed"));
            return false;
        }
        try {
            String transportDirName = this.mTransportManager.getTransportDirName(transportName);
            File stateDir = new File(this.mBaseStateDir, transportDirName);
            File pmState = new File(stateDir, PACKAGE_MANAGER_SENTINEL);
            if (pmState.length() <= 0) {
                Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Full backup requested but dataset not yet initialized"));
                return false;
            }
            return true;
        } catch (Exception e) {
            Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Unable to get transport name: " + e.getMessage()));
            return false;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2407=7] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:99:0x0214 */
    /* JADX WARN: Removed duplicated region for block: B:119:0x0287 A[Catch: all -> 0x02a0, TryCatch #0 {all -> 0x02a0, blocks: (B:113:0x0264, B:114:0x027d, B:119:0x0287, B:120:0x029e), top: B:136:0x0251 }] */
    /* JADX WARN: Removed duplicated region for block: B:124:0x02a5 A[LOOP:0: B:25:0x0079->B:124:0x02a5, LOOP_END] */
    /* JADX WARN: Removed duplicated region for block: B:137:0x0253 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:169:0x01c6 A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:83:0x01a5  */
    /* JADX WARN: Removed duplicated region for block: B:84:0x01a7  */
    /* JADX WARN: Type inference failed for: r3v2 */
    /* JADX WARN: Type inference failed for: r3v3 */
    /* JADX WARN: Type inference failed for: r3v4 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean beginFullBackup(FullBackupJob scheduledJob) {
        long fullBackupInterval;
        long keyValueBackupInterval;
        Object obj;
        int i;
        FullBackupEntry entry;
        long latency;
        boolean z;
        long latency2;
        FullBackupEntry entry2;
        long latency3;
        long latency4;
        int i2;
        int i3;
        long now = System.currentTimeMillis();
        synchronized (this.mConstants) {
            fullBackupInterval = this.mConstants.getFullBackupIntervalMilliseconds();
            keyValueBackupInterval = this.mConstants.getKeyValueBackupIntervalMilliseconds();
        }
        FullBackupEntry entry3 = null;
        long latency5 = fullBackupInterval;
        int i4 = 0;
        if (this.mEnabled && this.mSetupComplete) {
            PowerSaveState result = this.mPowerManager.getPowerSaveState(4);
            if (result.batterySaverEnabled) {
                Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Deferring scheduled full backups in battery saver mode"));
                FullBackupJob.schedule(this.mUserId, this.mContext, keyValueBackupInterval, this.mConstants);
                return false;
            }
            Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Beginning scheduled full backup operation"));
            Object obj2 = this.mQueueLock;
            synchronized (obj2) {
                try {
                    try {
                        if (this.mRunningFullBackupTask != null) {
                            Slog.e(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Backup triggered but one already/still running!"));
                        } else {
                            int i5 = 1;
                            while (true) {
                                if (this.mFullBackupQueue.size() == 0) {
                                    Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Backup queue empty; doing nothing"));
                                    i = 0;
                                    entry = entry3;
                                    latency = latency5;
                                    break;
                                }
                                int i6 = 0;
                                String transportName = this.mTransportManager.getCurrentTransportName();
                                if (!fullBackupAllowable(transportName)) {
                                    i5 = 0;
                                    latency5 = keyValueBackupInterval;
                                }
                                if (i5 != 0) {
                                    try {
                                        entry3 = this.mFullBackupQueue.get(i4);
                                        long timeSinceRun = now - entry3.lastBackup;
                                        i = timeSinceRun >= fullBackupInterval ? 1 : i4;
                                        if (i == 0) {
                                            long latency6 = fullBackupInterval - timeSinceRun;
                                            entry = entry3;
                                            latency = latency6;
                                            break;
                                        }
                                        try {
                                            latency4 = latency5;
                                            try {
                                                try {
                                                    PackageInfo appInfo = this.mPackageManager.getPackageInfoAsUser(entry3.packageName, 0, this.mUserId);
                                                    if (this.mScheduledBackupEligibility.appGetsFullBackup(appInfo)) {
                                                        int privFlags = appInfo.applicationInfo.privateFlags;
                                                        int i7 = ((privFlags & 8192) == 0 && this.mActivityManagerInternal.isAppForeground(appInfo.applicationInfo.uid)) ? 1 : 0;
                                                        i6 = i7;
                                                        if (i6 != 0) {
                                                            try {
                                                                i7 = this.mTokenGenerator.nextInt(BUSY_BACKOFF_FUZZ);
                                                                long nextEligible = System.currentTimeMillis() + 3600000 + i7;
                                                                i3 = i6;
                                                                try {
                                                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                                                    try {
                                                                        try {
                                                                            Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Full backup time but " + entry3.packageName + " is busy; deferring to " + sdf.format(new Date(nextEligible))));
                                                                            enqueueFullBackup(entry3.packageName, nextEligible - fullBackupInterval);
                                                                        } catch (PackageManager.NameNotFoundException e) {
                                                                            i6 = i3;
                                                                            i2 = 1;
                                                                            i5 = this.mFullBackupQueue.size() <= 1 ? 1 : 0;
                                                                            if (i6 == 0) {
                                                                            }
                                                                        }
                                                                    } catch (PackageManager.NameNotFoundException e2) {
                                                                        i6 = i3;
                                                                    }
                                                                } catch (PackageManager.NameNotFoundException e3) {
                                                                    i6 = i3;
                                                                }
                                                            } catch (PackageManager.NameNotFoundException e4) {
                                                            }
                                                        } else {
                                                            i3 = i6;
                                                        }
                                                        i5 = i;
                                                        i6 = i3;
                                                        i2 = i7;
                                                    } else {
                                                        try {
                                                            i2 = 0;
                                                            this.mFullBackupQueue.remove(0);
                                                            i6 = 1;
                                                            i5 = i;
                                                        } catch (PackageManager.NameNotFoundException e5) {
                                                            i2 = 1;
                                                            i5 = this.mFullBackupQueue.size() <= 1 ? 1 : 0;
                                                            if (i6 == 0) {
                                                            }
                                                        }
                                                    }
                                                } catch (Throwable th) {
                                                    nnf = th;
                                                    obj = obj2;
                                                    while (true) {
                                                        try {
                                                            break;
                                                        } catch (Throwable th2) {
                                                            nnf = th2;
                                                        }
                                                    }
                                                    throw nnf;
                                                }
                                            } catch (PackageManager.NameNotFoundException e6) {
                                            }
                                        } catch (PackageManager.NameNotFoundException e7) {
                                            latency4 = latency5;
                                        }
                                    } catch (Throwable th3) {
                                        nnf = th3;
                                        obj = obj2;
                                    }
                                } else {
                                    latency4 = latency5;
                                    i2 = i2;
                                }
                                if (i6 == 0) {
                                    i = i5;
                                    entry = entry3;
                                    latency = latency4;
                                    break;
                                }
                                i4 = 0;
                                latency5 = latency4;
                            }
                            if (i != 0) {
                                try {
                                    ?? r3 = 1;
                                    CountDownLatch latch = new CountDownLatch(1);
                                    String[] pkg = {entry.packageName};
                                    try {
                                        r3 = 0;
                                        latency2 = latency;
                                        z = true;
                                        obj = obj2;
                                        entry2 = entry;
                                        try {
                                            try {
                                                this.mRunningFullBackupTask = PerformFullTransportBackupTask.newWithCurrentTransport(this, this.mOperationStorage, null, pkg, true, scheduledJob, latch, null, null, false, "BMS.beginFullBackup()", getEligibilityRulesForOperation(0));
                                                latency3 = r3;
                                            } catch (IllegalStateException e8) {
                                                e = e8;
                                                Slog.w(BackupManagerService.TAG, "Failed to start backup", e);
                                                i = 0;
                                                latency3 = r3;
                                                if (i == 0) {
                                                }
                                            }
                                        } catch (Throwable th4) {
                                            nnf = th4;
                                            while (true) {
                                                break;
                                                break;
                                            }
                                            throw nnf;
                                        }
                                    } catch (IllegalStateException e9) {
                                        e = e9;
                                        z = true;
                                        latency2 = latency;
                                        obj = obj2;
                                        entry2 = entry;
                                    }
                                } catch (Throwable th5) {
                                    nnf = th5;
                                    obj = obj2;
                                }
                            } else {
                                latency2 = latency;
                                obj = obj2;
                                entry2 = entry;
                                z = true;
                                latency3 = i2;
                            }
                            try {
                                if (i == 0) {
                                    this.mFullBackupQueue.remove(0);
                                    this.mWakelock.acquire();
                                    new Thread(this.mRunningFullBackupTask).start();
                                    return z;
                                }
                                try {
                                    long latency7 = latency2;
                                    Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Nothing pending full backup or failed to start the operation; rescheduling +" + latency7));
                                    FullBackupJob.schedule(this.mUserId, this.mContext, latency7, this.mConstants);
                                    return false;
                                } catch (Throwable th6) {
                                    nnf = th6;
                                    while (true) {
                                        break;
                                        break;
                                    }
                                    throw nnf;
                                }
                            } catch (Throwable th7) {
                                nnf = th7;
                            }
                        }
                    } catch (Throwable th8) {
                        nnf = th8;
                        obj = obj2;
                    }
                } catch (Throwable th9) {
                    nnf = th9;
                    obj = obj2;
                }
            }
            return false;
        }
        return false;
    }

    public void endFullBackup() {
        Runnable endFullBackupRunnable = new Runnable() { // from class: com.android.server.backup.UserBackupManagerService.3
            @Override // java.lang.Runnable
            public void run() {
                PerformFullTransportBackupTask pftbt = null;
                synchronized (UserBackupManagerService.this.mQueueLock) {
                    if (UserBackupManagerService.this.mRunningFullBackupTask != null) {
                        pftbt = UserBackupManagerService.this.mRunningFullBackupTask;
                    }
                }
                if (pftbt != null) {
                    Slog.i(BackupManagerService.TAG, UserBackupManagerService.addUserIdToLogMessage(UserBackupManagerService.this.mUserId, "Telling running backup to stop"));
                    pftbt.handleCancel(true);
                }
            }
        };
        new Thread(endFullBackupRunnable, "end-full-backup").start();
    }

    public void restoreWidgetData(String packageName, byte[] widgetData) {
        AppWidgetBackupBridge.restoreWidgetState(packageName, widgetData, this.mUserId);
    }

    public void dataChangedImpl(String packageName) {
        HashSet<String> targets = dataChangedTargets(packageName);
        dataChangedImpl(packageName, targets);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dataChangedImpl(String packageName, HashSet<String> targets) {
        if (targets == null) {
            Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "dataChanged but no participant pkg='" + packageName + "' uid=" + Binder.getCallingUid()));
            return;
        }
        synchronized (this.mQueueLock) {
            if (targets.contains(packageName)) {
                BackupRequest req = new BackupRequest(packageName);
                if (this.mPendingBackups.put(packageName, req) == null) {
                    writeToJournalLocked(packageName);
                }
            }
        }
        KeyValueBackupJob.schedule(this.mUserId, this.mContext, this.mConstants);
    }

    private HashSet<String> dataChangedTargets(String packageName) {
        HashSet<String> union;
        HashSet<String> hashSet;
        if (this.mContext.checkPermission("android.permission.BACKUP", Binder.getCallingPid(), Binder.getCallingUid()) == -1) {
            synchronized (this.mBackupParticipants) {
                hashSet = this.mBackupParticipants.get(Binder.getCallingUid());
            }
            return hashSet;
        } else if (PACKAGE_MANAGER_SENTINEL.equals(packageName)) {
            return Sets.newHashSet(new String[]{PACKAGE_MANAGER_SENTINEL});
        } else {
            synchronized (this.mBackupParticipants) {
                union = SparseArrayUtils.union(this.mBackupParticipants);
            }
            return union;
        }
    }

    private void writeToJournalLocked(String str) {
        try {
            if (this.mJournal == null) {
                this.mJournal = DataChangedJournal.newJournal(this.mJournalDir);
            }
            this.mJournal.addPackage(str);
        } catch (IOException e) {
            Slog.e(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Can't write " + str + " to backup journal"), e);
            this.mJournal = null;
        }
    }

    public void dataChanged(final String packageName) {
        final HashSet<String> targets = dataChangedTargets(packageName);
        if (targets == null) {
            Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "dataChanged but no participant pkg='" + packageName + "' uid=" + Binder.getCallingUid()));
        } else {
            this.mBackupHandler.post(new Runnable() { // from class: com.android.server.backup.UserBackupManagerService.4
                @Override // java.lang.Runnable
                public void run() {
                    UserBackupManagerService.this.dataChangedImpl(packageName, targets);
                }
            });
        }
    }

    public void initializeTransports(String[] transportNames, IBackupObserver observer) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "initializeTransport");
        Slog.v(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "initializeTransport(): " + Arrays.asList(transportNames)));
        long oldId = Binder.clearCallingIdentity();
        try {
            this.mWakelock.acquire();
            OnTaskFinishedListener listener = new OnTaskFinishedListener() { // from class: com.android.server.backup.UserBackupManagerService$$ExternalSyntheticLambda4
                @Override // com.android.server.backup.internal.OnTaskFinishedListener
                public final void onFinished(String str) {
                    UserBackupManagerService.this.m2199xdc0ccfbc(str);
                }
            };
            this.mBackupHandler.post(new PerformInitializeTask(this, transportNames, observer, listener));
        } finally {
            Binder.restoreCallingIdentity(oldId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$initializeTransports$5$com-android-server-backup-UserBackupManagerService  reason: not valid java name */
    public /* synthetic */ void m2199xdc0ccfbc(String caller) {
        this.mWakelock.release();
    }

    public void setAncestralSerialNumber(long ancestralSerialNumber) {
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "setAncestralSerialNumber");
        Slog.v(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Setting ancestral work profile id to " + ancestralSerialNumber));
        try {
            RandomAccessFile af = new RandomAccessFile(getAncestralSerialNumberFile(), "rwd");
            af.writeLong(ancestralSerialNumber);
            af.close();
        } catch (IOException e) {
            Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Unable to write to work profile serial mapping file:"), e);
        }
    }

    public long getAncestralSerialNumber() {
        try {
            RandomAccessFile af = new RandomAccessFile(getAncestralSerialNumberFile(), ActivityTaskManagerService.DUMP_RECENTS_SHORT_CMD);
            try {
                long readLong = af.readLong();
                af.close();
                return readLong;
            } catch (Throwable th) {
                try {
                    af.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (FileNotFoundException e) {
            return -1L;
        } catch (IOException e2) {
            Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Unable to read work profile serial number file:"), e2);
            return -1L;
        }
    }

    private File getAncestralSerialNumberFile() {
        if (this.mAncestralSerialNumberFile == null) {
            this.mAncestralSerialNumberFile = new File(UserBackupManagerFiles.getBaseStateDir(getUserId()), SERIAL_ID_FILE);
        }
        return this.mAncestralSerialNumberFile;
    }

    void setAncestralSerialNumberFile(File ancestralSerialNumberFile) {
        this.mAncestralSerialNumberFile = ancestralSerialNumberFile;
    }

    public void clearBackupData(String transportName, String packageName) {
        HashSet<String> apps;
        Slog.v(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "clearBackupData() of " + packageName + " on " + transportName));
        try {
            PackageInfo info = this.mPackageManager.getPackageInfoAsUser(packageName, 134217728, this.mUserId);
            if (this.mContext.checkPermission("android.permission.BACKUP", Binder.getCallingPid(), Binder.getCallingUid()) == -1) {
                apps = this.mBackupParticipants.get(Binder.getCallingUid());
            } else {
                apps = this.mProcessedPackagesJournal.getPackagesCopy();
            }
            if (apps.contains(packageName)) {
                this.mBackupHandler.removeMessages(12);
                synchronized (this.mQueueLock) {
                    final TransportConnection transportConnection = this.mTransportManager.getTransportClient(transportName, "BMS.clearBackupData()");
                    if (transportConnection == null) {
                        Message msg = this.mBackupHandler.obtainMessage(12, new ClearRetryParams(transportName, packageName));
                        this.mBackupHandler.sendMessageDelayed(msg, 3600000L);
                        return;
                    }
                    long oldId = Binder.clearCallingIdentity();
                    OnTaskFinishedListener listener = new OnTaskFinishedListener() { // from class: com.android.server.backup.UserBackupManagerService$$ExternalSyntheticLambda10
                        @Override // com.android.server.backup.internal.OnTaskFinishedListener
                        public final void onFinished(String str) {
                            UserBackupManagerService.this.m2197x72a6f236(transportConnection, str);
                        }
                    };
                    this.mWakelock.acquire();
                    Message msg2 = this.mBackupHandler.obtainMessage(4, new ClearParams(transportConnection, info, listener));
                    this.mBackupHandler.sendMessage(msg2);
                    Binder.restoreCallingIdentity(oldId);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Slog.d(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "No such package '" + packageName + "' - not clearing backup data"));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$clearBackupData$6$com-android-server-backup-UserBackupManagerService  reason: not valid java name */
    public /* synthetic */ void m2197x72a6f236(TransportConnection transportConnection, String caller) {
        this.mTransportManager.disposeOfTransportClient(transportConnection, caller);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2780=4] */
    public void backupNow() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "backupNow");
        long oldId = Binder.clearCallingIdentity();
        try {
            PowerSaveState result = this.mPowerManager.getPowerSaveState(5);
            if (!result.batterySaverEnabled) {
                Slog.v(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Scheduling immediate backup pass"));
                synchronized (getQueueLock()) {
                    if (getPendingInits().size() > 0) {
                        try {
                            getAlarmManager().cancel(this.mRunInitIntent);
                            this.mRunInitIntent.send();
                        } catch (PendingIntent.CanceledException e) {
                            Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Run init intent cancelled"));
                        }
                        return;
                    }
                    if (isEnabled() && isSetupComplete()) {
                        Message message = this.mBackupHandler.obtainMessage(1);
                        this.mBackupHandler.sendMessage(message);
                        KeyValueBackupJob.cancel(this.mUserId, this.mContext);
                    }
                    Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Backup pass but enabled=" + isEnabled() + " setupComplete=" + isSetupComplete()));
                    return;
                }
            }
            Slog.v(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Not running backup while in battery save mode"));
            KeyValueBackupJob.schedule(this.mUserId, this.mContext, this.mConstants);
        } finally {
            Binder.restoreCallingIdentity(oldId);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2882=4, 2883=4, 2884=8, 2886=4, 2888=4, 2889=4, 2890=4, 2891=4] */
    public void adbBackup(ParcelFileDescriptor fd, boolean includeApks, boolean includeObbs, boolean includeShared, boolean doWidgets, boolean doAllApps, boolean includeSystem, boolean compress, boolean doKeyValue, String[] pkgList) {
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "adbBackup");
        int callingUserHandle = UserHandle.getCallingUserId();
        if (callingUserHandle != 0) {
            throw new IllegalStateException("Backup supported only for the device owner");
        }
        if (!doAllApps && !includeShared && (pkgList == null || pkgList.length == 0)) {
            throw new IllegalArgumentException("Backup requested but neither shared nor any apps named");
        }
        long oldId = Binder.clearCallingIdentity();
        try {
            if (!this.mSetupComplete) {
                Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Backup not supported before setup"));
                try {
                    fd.close();
                } catch (IOException e) {
                    Slog.e(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "IO error closing output for adb backup: " + e.getMessage()));
                }
                Binder.restoreCallingIdentity(oldId);
                Slog.d(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Adb backup processing complete."));
                return;
            }
            Slog.v(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Requesting backup: apks=" + includeApks + " obb=" + includeObbs + " shared=" + includeShared + " all=" + doAllApps + " system=" + includeSystem + " includekeyvalue=" + doKeyValue + " pkgs=" + pkgList));
            Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Beginning adb backup..."));
            BackupEligibilityRules eligibilityRules = getEligibilityRulesForOperation(3);
            AdbBackupParams params = new AdbBackupParams(fd, includeApks, includeObbs, includeShared, doWidgets, doAllApps, includeSystem, compress, doKeyValue, pkgList, eligibilityRules);
            int token = generateRandomIntegerToken();
            synchronized (this.mAdbBackupRestoreConfirmations) {
                this.mAdbBackupRestoreConfirmations.put(token, params);
            }
            Slog.d(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Starting backup confirmation UI, token=" + token));
            if (!startConfirmationUi(token, "fullback")) {
                Slog.e(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Unable to launch backup confirmation UI"));
                this.mAdbBackupRestoreConfirmations.delete(token);
                try {
                    fd.close();
                } catch (IOException e2) {
                    Slog.e(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "IO error closing output for adb backup: " + e2.getMessage()));
                }
                Binder.restoreCallingIdentity(oldId);
                Slog.d(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Adb backup processing complete."));
                return;
            }
            this.mPowerManager.userActivity(SystemClock.uptimeMillis(), 0, 0);
            startConfirmationTimeout(token, params);
            Slog.d(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Waiting for backup completion..."));
            waitForCompletion(params);
            try {
                fd.close();
            } catch (IOException e3) {
                Slog.e(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "IO error closing output for adb backup: " + e3.getMessage()));
            }
            Binder.restoreCallingIdentity(oldId);
            Slog.d(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Adb backup processing complete."));
        } catch (Throwable th) {
            try {
                fd.close();
            } catch (IOException e4) {
                Slog.e(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "IO error closing output for adb backup: " + e4.getMessage()));
            }
            Binder.restoreCallingIdentity(oldId);
            Slog.d(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Adb backup processing complete."));
            throw th;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2954=4] */
    public void fullTransportBackup(String[] pkgNames) {
        String str;
        String str2;
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "fullTransportBackup");
        int callingUserHandle = UserHandle.getCallingUserId();
        if (callingUserHandle != 0) {
            throw new IllegalStateException("Restore supported only for the device owner");
        }
        String transportName = this.mTransportManager.getCurrentTransportName();
        if (fullBackupAllowable(transportName)) {
            Slog.d(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "fullTransportBackup()"));
            long oldId = Binder.clearCallingIdentity();
            try {
                CountDownLatch latch = new CountDownLatch(1);
                LifecycleOperationStorage lifecycleOperationStorage = this.mOperationStorage;
                BackupEligibilityRules eligibilityRulesForOperation = getEligibilityRulesForOperation(0);
                str2 = BackupManagerService.TAG;
                try {
                    try {
                        Runnable task = PerformFullTransportBackupTask.newWithCurrentTransport(this, lifecycleOperationStorage, null, pkgNames, false, null, latch, null, null, false, "BMS.fullTransportBackup()", eligibilityRulesForOperation);
                        this.mWakelock.acquire();
                        new Thread(task, "full-transport-master").start();
                        while (true) {
                            try {
                                latch.await();
                                break;
                            } catch (InterruptedException e) {
                                str2 = str2;
                            }
                        }
                        long now = System.currentTimeMillis();
                        for (String pkg : pkgNames) {
                            enqueueFullBackup(pkg, now);
                        }
                        Binder.restoreCallingIdentity(oldId);
                    } catch (IllegalStateException e2) {
                        e = e2;
                        str = str2;
                        Slog.w(str, "Failed to start backup: ", e);
                        Binder.restoreCallingIdentity(oldId);
                        return;
                    }
                } catch (Throwable th) {
                    e = th;
                    Binder.restoreCallingIdentity(oldId);
                    throw e;
                }
            } catch (IllegalStateException e3) {
                e = e3;
                str = BackupManagerService.TAG;
            } catch (Throwable th2) {
                e = th2;
                Binder.restoreCallingIdentity(oldId);
                throw e;
            }
        } else {
            Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Full backup not currently possible -- key/value backup not yet run?"));
            str2 = BackupManagerService.TAG;
        }
        Slog.d(str2, addUserIdToLogMessage(this.mUserId, "Done with full transport backup."));
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE, CONST_STR, IGET, CONST_STR, INVOKE, INVOKE, INVOKE, CONST_STR, IGET, CONST_STR, INVOKE, INVOKE, CONST_STR, IGET, CONSTRUCTOR, CONST_STR, INVOKE, INVOKE, INVOKE, INVOKE, INVOKE, MOVE_EXCEPTION, INVOKE, INVOKE, CONST_STR, IGET, CONST_STR, INVOKE, INVOKE, CONST_STR, IGET, CONSTRUCTOR, CONST_STR, INVOKE, INVOKE, INVOKE, INVOKE, INVOKE, MOVE_EXCEPTION] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3023=4, 3024=4, 3025=8, 3027=4, 3029=4, 3030=4, 3031=4] */
    public void adbRestore(ParcelFileDescriptor fd) {
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "adbRestore");
        int callingUserHandle = UserHandle.getCallingUserId();
        if (callingUserHandle != 0) {
            throw new IllegalStateException("Restore supported only for the device owner");
        }
        long oldId = Binder.clearCallingIdentity();
        try {
            if (!this.mSetupComplete) {
                Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Full restore not permitted before setup"));
                return;
            }
            Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Beginning restore..."));
            AdbRestoreParams params = new AdbRestoreParams(fd);
            int token = generateRandomIntegerToken();
            synchronized (this.mAdbBackupRestoreConfirmations) {
                this.mAdbBackupRestoreConfirmations.put(token, params);
            }
            Slog.d(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Starting restore confirmation UI, token=" + token));
            if (!startConfirmationUi(token, "fullrest")) {
                Slog.e(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Unable to launch restore confirmation"));
                this.mAdbBackupRestoreConfirmations.delete(token);
                try {
                    fd.close();
                } catch (IOException e) {
                    Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Error trying to close fd after adb restore: " + e));
                }
                Binder.restoreCallingIdentity(oldId);
                Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "adb restore processing complete."));
                return;
            }
            this.mPowerManager.userActivity(SystemClock.uptimeMillis(), 0, 0);
            startConfirmationTimeout(token, params);
            Slog.d(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Waiting for restore completion..."));
            waitForCompletion(params);
            try {
                fd.close();
            } catch (IOException e2) {
                Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Error trying to close fd after adb restore: " + e2));
            }
            Binder.restoreCallingIdentity(oldId);
            Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "adb restore processing complete."));
        } finally {
            try {
                fd.close();
            } catch (IOException e3) {
                Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Error trying to close fd after adb restore: " + e3));
            }
            Binder.restoreCallingIdentity(oldId);
            Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "adb restore processing complete."));
        }
    }

    public void excludeKeysFromRestore(String packageName, List<String> keys) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "excludeKeysFromRestore");
        this.mBackupPreferences.addExcludedKeys(packageName, keys);
    }

    private boolean startConfirmationUi(int token, String action) {
        try {
            Intent confIntent = new Intent(action);
            confIntent.setClassName("com.android.backupconfirm", "com.android.backupconfirm.BackupRestoreConfirmation");
            confIntent.putExtra("conftoken", token);
            confIntent.addFlags(536870912);
            this.mContext.startActivityAsUser(confIntent, UserHandle.SYSTEM);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    private void startConfirmationTimeout(int token, AdbParams params) {
        Message msg = this.mBackupHandler.obtainMessage(9, token, 0, params);
        this.mBackupHandler.sendMessageDelayed(msg, 60000L);
    }

    private void waitForCompletion(AdbParams params) {
        synchronized (params.latch) {
            while (!params.latch.get()) {
                try {
                    params.latch.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void signalAdbBackupRestoreCompletion(AdbParams params) {
        synchronized (params.latch) {
            params.latch.set(true);
            params.latch.notifyAll();
        }
    }

    public void acknowledgeAdbBackupOrRestore(int token, boolean allow, String curPassword, String encPpassword, IFullBackupRestoreObserver observer) {
        int verb;
        Slog.d(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "acknowledgeAdbBackupOrRestore : token=" + token + " allow=" + allow));
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "acknowledgeAdbBackupOrRestore");
        long oldId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mAdbBackupRestoreConfirmations) {
                AdbParams params = this.mAdbBackupRestoreConfirmations.get(token);
                if (params != null) {
                    this.mBackupHandler.removeMessages(9, params);
                    this.mAdbBackupRestoreConfirmations.delete(token);
                    if (allow) {
                        if (params instanceof AdbBackupParams) {
                            verb = 2;
                        } else {
                            verb = 10;
                        }
                        params.observer = observer;
                        params.curPassword = curPassword;
                        params.encryptPassword = encPpassword;
                        this.mWakelock.acquire();
                        Message msg = this.mBackupHandler.obtainMessage(verb, params);
                        this.mBackupHandler.sendMessage(msg);
                    } else {
                        Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "User rejected full backup/restore operation"));
                        signalAdbBackupRestoreCompletion(params);
                    }
                } else {
                    Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Attempted to ack full backup/restore with invalid token"));
                }
            }
        } finally {
            Binder.restoreCallingIdentity(oldId);
        }
    }

    public void setBackupEnabled(boolean enable) {
        setBackupEnabled(enable, true);
    }

    private void setBackupEnabled(boolean enable, boolean persistToDisk) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "setBackupEnabled");
        Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Backup enabled => " + enable));
        long oldId = Binder.clearCallingIdentity();
        try {
            boolean wasEnabled = this.mEnabled;
            synchronized (this) {
                if (persistToDisk) {
                    writeEnabledState(enable);
                }
                this.mEnabled = enable;
            }
            updateStateOnBackupEnabled(wasEnabled, enable);
        } finally {
            Binder.restoreCallingIdentity(oldId);
        }
    }

    void updateStateOnBackupEnabled(boolean wasEnabled, boolean enable) {
        synchronized (this.mQueueLock) {
            if (enable && !wasEnabled) {
                if (this.mSetupComplete) {
                    KeyValueBackupJob.schedule(this.mUserId, this.mContext, this.mConstants);
                    scheduleNextFullBackupJob(0L);
                }
            }
            if (!enable) {
                KeyValueBackupJob.cancel(this.mUserId, this.mContext);
                if (wasEnabled && this.mSetupComplete) {
                    final List<String> transportNames = new ArrayList<>();
                    final List<String> transportDirNames = new ArrayList<>();
                    this.mTransportManager.forEachRegisteredTransport(new Consumer() { // from class: com.android.server.backup.UserBackupManagerService$$ExternalSyntheticLambda12
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            UserBackupManagerService.this.m2205xb4c731fc(transportNames, transportDirNames, (String) obj);
                        }
                    });
                    for (int i = 0; i < transportNames.size(); i++) {
                        recordInitPending(true, transportNames.get(i), transportDirNames.get(i));
                    }
                    this.mAlarmManager.set(0, System.currentTimeMillis(), this.mRunInitIntent);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateStateOnBackupEnabled$7$com-android-server-backup-UserBackupManagerService  reason: not valid java name */
    public /* synthetic */ void m2205xb4c731fc(List transportNames, List transportDirNames, String name) {
        try {
            String dirName = this.mTransportManager.getTransportDirName(name);
            transportNames.add(name);
            transportDirNames.add(dirName);
        } catch (TransportNotRegisteredException e) {
            Slog.e(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Unexpected unregistered transport"), e);
        }
    }

    void writeEnabledState(boolean enable) {
        UserBackupManagerFilePersistedSettings.writeBackupEnableState(this.mUserId, enable);
    }

    boolean readEnabledState() {
        return UserBackupManagerFilePersistedSettings.readBackupEnableState(this.mUserId);
    }

    public void setAutoRestore(boolean doAutoRestore) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "setAutoRestore");
        Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Auto restore => " + doAutoRestore));
        long oldId = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "backup_auto_restore", doAutoRestore ? 1 : 0, this.mUserId);
                this.mAutoRestore = doAutoRestore;
            }
        } finally {
            Binder.restoreCallingIdentity(oldId);
        }
    }

    public boolean isBackupEnabled() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "isBackupEnabled");
        return this.mEnabled;
    }

    public String getCurrentTransport() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getCurrentTransport");
        String currentTransport = this.mTransportManager.getCurrentTransportName();
        return currentTransport;
    }

    public ComponentName getCurrentTransportComponent() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getCurrentTransportComponent");
        long oldId = Binder.clearCallingIdentity();
        try {
            return this.mTransportManager.getCurrentTransportComponent();
        } catch (TransportNotRegisteredException e) {
            return null;
        } finally {
            Binder.restoreCallingIdentity(oldId);
        }
    }

    public String[] listAllTransports() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "listAllTransports");
        return this.mTransportManager.getRegisteredTransportNames();
    }

    public ComponentName[] listAllTransportComponents() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "listAllTransportComponents");
        return this.mTransportManager.getRegisteredTransportComponents();
    }

    public void updateTransportAttributes(ComponentName transportComponent, String name, Intent configurationIntent, String currentDestinationString, Intent dataManagementIntent, CharSequence dataManagementLabel) {
        updateTransportAttributes(Binder.getCallingUid(), transportComponent, name, configurationIntent, currentDestinationString, dataManagementIntent, dataManagementLabel);
    }

    void updateTransportAttributes(int callingUid, ComponentName transportComponent, String name, Intent configurationIntent, String currentDestinationString, Intent dataManagementIntent, CharSequence dataManagementLabel) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "updateTransportAttributes");
        Objects.requireNonNull(transportComponent, "transportComponent can't be null");
        Objects.requireNonNull(name, "name can't be null");
        Objects.requireNonNull(currentDestinationString, "currentDestinationString can't be null");
        Preconditions.checkArgument((dataManagementIntent == null) == (dataManagementLabel == null), "dataManagementLabel should be null iff dataManagementIntent is null");
        try {
            int transportUid = this.mContext.getPackageManager().getPackageUidAsUser(transportComponent.getPackageName(), 0, this.mUserId);
            if (callingUid != transportUid) {
                try {
                    throw new SecurityException("Only the transport can change its description");
                } catch (PackageManager.NameNotFoundException e) {
                    e = e;
                    throw new SecurityException("Transport package not found", e);
                }
            }
            long oldId = Binder.clearCallingIdentity();
            try {
                this.mTransportManager.updateTransportAttributes(transportComponent, name, configurationIntent, currentDestinationString, dataManagementIntent, dataManagementLabel);
            } finally {
                Binder.restoreCallingIdentity(oldId);
            }
        } catch (PackageManager.NameNotFoundException e2) {
            e = e2;
        }
    }

    @Deprecated
    public String selectBackupTransport(String transportName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "selectBackupTransport");
        long oldId = Binder.clearCallingIdentity();
        try {
            String previousTransportName = this.mTransportManager.selectTransport(transportName);
            updateStateForTransport(transportName);
            Slog.v(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "selectBackupTransport(transport = " + transportName + "): previous transport = " + previousTransportName));
            return previousTransportName;
        } finally {
            Binder.restoreCallingIdentity(oldId);
        }
    }

    public void selectBackupTransportAsync(final ComponentName transportComponent, final ISelectBackupTransportCallback listener) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "selectBackupTransportAsync");
        long oldId = Binder.clearCallingIdentity();
        try {
            String transportString = transportComponent.flattenToShortString();
            Slog.v(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "selectBackupTransportAsync(transport = " + transportString + ")"));
            this.mBackupHandler.post(new Runnable() { // from class: com.android.server.backup.UserBackupManagerService$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    UserBackupManagerService.this.m2204xd07c18c2(transportComponent, listener);
                }
            });
        } finally {
            Binder.restoreCallingIdentity(oldId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$selectBackupTransportAsync$8$com-android-server-backup-UserBackupManagerService  reason: not valid java name */
    public /* synthetic */ void m2204xd07c18c2(ComponentName transportComponent, ISelectBackupTransportCallback listener) {
        String transportName = null;
        int result = this.mTransportManager.registerAndSelectTransport(transportComponent);
        if (result == 0) {
            try {
                transportName = this.mTransportManager.getTransportName(transportComponent);
                updateStateForTransport(transportName);
            } catch (TransportNotRegisteredException e) {
                Slog.e(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Transport got unregistered"));
                result = -1;
            }
        }
        try {
            if (transportName != null) {
                listener.onSuccess(transportName);
            } else {
                listener.onFailure(result);
            }
        } catch (RemoteException e2) {
            Slog.e(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "ISelectBackupTransportCallback listener not available"));
        }
    }

    public List<PackageInfo> filterUserFacingPackages(List<PackageInfo> packages) {
        if (!shouldSkipUserFacingData()) {
            return packages;
        }
        List<PackageInfo> filteredPackages = new ArrayList<>(packages.size());
        for (PackageInfo packageInfo : packages) {
            if (!shouldSkipPackage(packageInfo.packageName)) {
                filteredPackages.add(packageInfo);
            } else {
                Slog.i(BackupManagerService.TAG, "Will skip backup/restore for " + packageInfo.packageName);
            }
        }
        return filteredPackages;
    }

    public boolean shouldSkipUserFacingData() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), SKIP_USER_FACING_PACKAGES, 0) != 0;
    }

    public boolean shouldSkipPackage(String packageName) {
        return WALLPAPER_PACKAGE.equals(packageName);
    }

    private void updateStateForTransport(String newTransportName) {
        Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "backup_transport", newTransportName, this.mUserId);
        TransportConnection transportConnection = this.mTransportManager.getTransportClient(newTransportName, "BMS.updateStateForTransport()");
        if (transportConnection != null) {
            try {
                BackupTransportClient transport = transportConnection.connectOrThrow("BMS.updateStateForTransport()");
                this.mCurrentToken = transport.getCurrentRestoreSet();
            } catch (Exception e) {
                this.mCurrentToken = 0L;
                Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Transport " + newTransportName + " not available: current token = 0"));
            }
            this.mTransportManager.disposeOfTransportClient(transportConnection, "BMS.updateStateForTransport()");
            return;
        }
        Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Transport " + newTransportName + " not registered: current token = 0"));
        this.mCurrentToken = 0L;
    }

    public Intent getConfigurationIntent(String transportName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getConfigurationIntent");
        try {
            Intent intent = this.mTransportManager.getTransportConfigurationIntent(transportName);
            return intent;
        } catch (TransportNotRegisteredException e) {
            Slog.e(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Unable to get configuration intent from transport: " + e.getMessage()));
            return null;
        }
    }

    public String getDestinationString(String transportName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getDestinationString");
        try {
            String string = this.mTransportManager.getTransportCurrentDestinationString(transportName);
            return string;
        } catch (TransportNotRegisteredException e) {
            Slog.e(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Unable to get destination string from transport: " + e.getMessage()));
            return null;
        }
    }

    public Intent getDataManagementIntent(String transportName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getDataManagementIntent");
        try {
            Intent intent = this.mTransportManager.getTransportDataManagementIntent(transportName);
            return intent;
        } catch (TransportNotRegisteredException e) {
            Slog.e(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Unable to get management intent from transport: " + e.getMessage()));
            return null;
        }
    }

    public CharSequence getDataManagementLabel(String transportName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getDataManagementLabel");
        try {
            CharSequence label = this.mTransportManager.getTransportDataManagementLabel(transportName);
            return label;
        } catch (TransportNotRegisteredException e) {
            Slog.e(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Unable to get management label from transport: " + e.getMessage()));
            return null;
        }
    }

    public void agentConnected(String packageName, IBinder agentBinder) {
        synchronized (this.mAgentConnectLock) {
            if (Binder.getCallingUid() == 1000) {
                Slog.d(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "agentConnected pkg=" + packageName + " agent=" + agentBinder));
                this.mConnectedAgent = IBackupAgent.Stub.asInterface(agentBinder);
                this.mConnecting = false;
            } else {
                Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Non-system process uid=" + Binder.getCallingUid() + " claiming agent connected"));
            }
            this.mAgentConnectLock.notifyAll();
        }
    }

    public void agentDisconnected(String packageName) {
        synchronized (this.mAgentConnectLock) {
            if (Binder.getCallingUid() == 1000) {
                this.mConnectedAgent = null;
                this.mConnecting = false;
            } else {
                Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Non-system process uid=" + Binder.getCallingUid() + " claiming agent disconnected"));
            }
            Slog.w(BackupManagerService.TAG, "agentDisconnected: the backup agent for " + packageName + " died: cancel current operations");
            for (Integer num : this.mOperationStorage.operationTokensForPackage(packageName)) {
                int token = num.intValue();
                handleCancel(token, true);
            }
            this.mAgentConnectLock.notifyAll();
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:28:0x0117  */
    /* JADX WARN: Removed duplicated region for block: B:40:0x0138  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void restoreAtInstall(String packageName, int token) {
        boolean skip;
        TransportConnection transportConnection;
        OnTaskFinishedListener listener;
        Message msg;
        BackupEligibilityRules eligibilityRulesForRestoreAtInstall;
        if (Binder.getCallingUid() != 1000) {
            Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Non-system process uid=" + Binder.getCallingUid() + " attemping install-time restore"));
            return;
        }
        boolean skip2 = false;
        long restoreSet = getAvailableRestoreToken(packageName);
        Slog.v(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "restoreAtInstall pkg=" + packageName + " token=" + Integer.toHexString(token) + " restoreSet=" + Long.toHexString(restoreSet)));
        if (restoreSet == 0) {
            skip2 = true;
        }
        final TransportConnection transportConnection2 = this.mTransportManager.getCurrentTransportClient("BMS.restoreAtInstall()");
        if (transportConnection2 == null) {
            Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "No transport client"));
            skip2 = true;
        }
        if (this.mAutoRestore) {
            skip = skip2;
        } else {
            Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Non-restorable state: auto=" + this.mAutoRestore));
            skip = true;
        }
        if (skip) {
            transportConnection = transportConnection2;
        } else {
            try {
                this.mWakelock.acquire();
                listener = new OnTaskFinishedListener() { // from class: com.android.server.backup.UserBackupManagerService$$ExternalSyntheticLambda11
                    @Override // com.android.server.backup.internal.OnTaskFinishedListener
                    public final void onFinished(String str) {
                        UserBackupManagerService.this.m2203xca5da1d8(transportConnection2, str);
                    }
                };
                msg = this.mBackupHandler.obtainMessage(3);
                eligibilityRulesForRestoreAtInstall = getEligibilityRulesForRestoreAtInstall(restoreSet);
                transportConnection = transportConnection2;
            } catch (Exception e) {
                e = e;
                transportConnection = transportConnection2;
            }
            try {
                msg.obj = RestoreParams.createForRestoreAtInstall(transportConnection2, null, null, restoreSet, packageName, token, listener, eligibilityRulesForRestoreAtInstall);
                this.mBackupHandler.sendMessage(msg);
            } catch (Exception e2) {
                e = e2;
                Slog.e(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Unable to contact transport: " + e.getMessage()));
                skip = true;
                if (!skip) {
                }
            }
        }
        if (!skip) {
            if (transportConnection != null) {
                this.mTransportManager.disposeOfTransportClient(transportConnection, "BMS.restoreAtInstall()");
            }
            Slog.v(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Finishing install immediately"));
            try {
                try {
                    this.mPackageManagerBinder.finishPackageInstall(token, false);
                } catch (RemoteException e3) {
                }
            } catch (RemoteException e4) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$restoreAtInstall$9$com-android-server-backup-UserBackupManagerService  reason: not valid java name */
    public /* synthetic */ void m2203xca5da1d8(TransportConnection transportConnection, String caller) {
        this.mTransportManager.disposeOfTransportClient(transportConnection, caller);
        this.mWakelock.release();
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, IGET, CONST_STR, INVOKE] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3911=4] */
    public IRestoreSession beginRestoreSession(String packageName, String transport) {
        Slog.v(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "beginRestoreSession: pkg=" + packageName + " transport=" + transport));
        boolean needPermission = true;
        if (transport == null) {
            transport = this.mTransportManager.getCurrentTransportName();
            if (packageName != null) {
                try {
                    PackageInfo app = this.mPackageManager.getPackageInfoAsUser(packageName, 0, this.mUserId);
                    if (app.applicationInfo.uid == Binder.getCallingUid()) {
                        needPermission = false;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.w(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Asked to restore nonexistent pkg " + packageName));
                    throw new IllegalArgumentException("Package " + packageName + " not found");
                }
            }
        }
        if (needPermission) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "beginRestoreSession");
        } else {
            Slog.d(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "restoring self on current transport; no permission needed"));
        }
        TransportConnection transportConnection = null;
        try {
            try {
                transportConnection = this.mTransportManager.getTransportClientOrThrow(transport, "BMS.beginRestoreSession");
                int operationType = getOperationTypeFromTransport(transportConnection);
                if (transportConnection != null) {
                    this.mTransportManager.disposeOfTransportClient(transportConnection, "BMS.beginRestoreSession");
                }
                synchronized (this) {
                    if (this.mActiveRestoreSession != null) {
                        Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Restore session requested but one already active"));
                        return null;
                    } else if (this.mBackupRunning) {
                        Slog.i(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Restore session requested but currently running backups"));
                        return null;
                    } else {
                        this.mActiveRestoreSession = new ActiveRestoreSession(this, packageName, transport, getEligibilityRulesForOperation(operationType));
                        this.mBackupHandler.sendEmptyMessageDelayed(8, this.mAgentTimeoutParameters.getRestoreSessionTimeoutMillis());
                        return this.mActiveRestoreSession;
                    }
                }
            } catch (RemoteException | TransportNotAvailableException | TransportNotRegisteredException e2) {
                Slog.w(BackupManagerService.TAG, "Failed to get operation type from transport: " + e2);
                if (transportConnection != null) {
                    this.mTransportManager.disposeOfTransportClient(transportConnection, "BMS.beginRestoreSession");
                }
                return null;
            }
        } catch (Throwable th) {
            if (transportConnection != null) {
                this.mTransportManager.disposeOfTransportClient(transportConnection, "BMS.beginRestoreSession");
            }
            throw th;
        }
    }

    public void clearRestoreSession(ActiveRestoreSession currentSession) {
        synchronized (this) {
            if (currentSession != this.mActiveRestoreSession) {
                Slog.e(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "ending non-current restore session"));
            } else {
                Slog.v(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Clearing restore session and halting timeout"));
                this.mActiveRestoreSession = null;
                this.mBackupHandler.removeMessages(8);
            }
        }
    }

    public void opComplete(int token, final long result) {
        this.mOperationStorage.onOperationComplete(token, result, new Consumer() { // from class: com.android.server.backup.UserBackupManagerService$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                UserBackupManagerService.this.m2200xcea1cc38(result, (BackupRestoreTask) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$opComplete$10$com-android-server-backup-UserBackupManagerService  reason: not valid java name */
    public /* synthetic */ void m2200xcea1cc38(long result, BackupRestoreTask callback) {
        Pair<BackupRestoreTask, Long> callbackAndResult = Pair.create(callback, Long.valueOf(result));
        Message msg = this.mBackupHandler.obtainMessage(21, callbackAndResult);
        this.mBackupHandler.sendMessage(msg);
    }

    public boolean isAppEligibleForBackup(String packageName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "isAppEligibleForBackup");
        long oldToken = Binder.clearCallingIdentity();
        try {
            TransportConnection transportConnection = this.mTransportManager.getCurrentTransportClient("BMS.isAppEligibleForBackup");
            boolean eligible = this.mScheduledBackupEligibility.appIsRunningAndEligibleForBackupWithTransport(transportConnection, packageName);
            if (transportConnection != null) {
                this.mTransportManager.disposeOfTransportClient(transportConnection, "BMS.isAppEligibleForBackup");
            }
            return eligible;
        } finally {
            Binder.restoreCallingIdentity(oldToken);
        }
    }

    public String[] filterAppsEligibleForBackup(String[] packages) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "filterAppsEligibleForBackup");
        long oldToken = Binder.clearCallingIdentity();
        try {
            TransportConnection transportConnection = this.mTransportManager.getCurrentTransportClient("BMS.filterAppsEligibleForBackup");
            List<String> eligibleApps = new LinkedList<>();
            for (String packageName : packages) {
                if (this.mScheduledBackupEligibility.appIsRunningAndEligibleForBackupWithTransport(transportConnection, packageName)) {
                    eligibleApps.add(packageName);
                }
            }
            if (transportConnection != null) {
                this.mTransportManager.disposeOfTransportClient(transportConnection, "BMS.filterAppsEligibleForBackup");
            }
            return (String[]) eligibleApps.toArray(new String[eligibleApps.size()]);
        } finally {
            Binder.restoreCallingIdentity(oldToken);
        }
    }

    public BackupEligibilityRules getEligibilityRulesForOperation(int operationType) {
        return getEligibilityRules(this.mPackageManager, this.mUserId, operationType);
    }

    private static BackupEligibilityRules getEligibilityRules(PackageManager packageManager, int userId, int operationType) {
        return new BackupEligibilityRules(packageManager, (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class), userId, operationType);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4050=5] */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        long identityToken = Binder.clearCallingIdentity();
        if (args != null) {
            try {
                for (String arg : args) {
                    if ("agents".startsWith(arg)) {
                        dumpAgents(pw);
                        return;
                    } else if ("transportclients".equals(arg.toLowerCase())) {
                        this.mTransportManager.dumpTransportClients(pw);
                        return;
                    } else if ("transportstats".equals(arg.toLowerCase())) {
                        this.mTransportManager.dumpTransportStats(pw);
                        return;
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identityToken);
            }
        }
        dumpInternal(pw);
    }

    private void dumpAgents(PrintWriter pw) {
        List<PackageInfo> agentPackages = allAgentPackages();
        pw.println("Defined backup agents:");
        for (PackageInfo pkg : agentPackages) {
            pw.print("  ");
            pw.print(pkg.packageName);
            pw.println(':');
            pw.print("      ");
            pw.println(pkg.applicationInfo.backupAgentName);
        }
    }

    @NeverCompile
    private void dumpInternal(PrintWriter pw) {
        int i;
        String userPrefix = this.mUserId == 0 ? "" : "User " + this.mUserId + ":";
        synchronized (this.mQueueLock) {
            pw.println(userPrefix + "Backup Manager is " + (this.mEnabled ? ServiceConfigAccessor.PROVIDER_MODE_ENABLED : ServiceConfigAccessor.PROVIDER_MODE_DISABLED) + " / " + (!this.mSetupComplete ? "not " : "") + "setup complete / " + (this.mPendingInits.size() == 0 ? "not " : "") + "pending init");
            pw.println("Auto-restore is " + (this.mAutoRestore ? ServiceConfigAccessor.PROVIDER_MODE_ENABLED : ServiceConfigAccessor.PROVIDER_MODE_DISABLED));
            if (this.mBackupRunning) {
                pw.println("Backup currently running");
            }
            pw.println(isBackupOperationInProgress() ? "Backup in progress" : "No backups running");
            pw.println("Last backup pass started: " + this.mLastBackupPass + " (now = " + System.currentTimeMillis() + ')');
            pw.println("  next scheduled: " + KeyValueBackupJob.nextScheduled(this.mUserId));
            pw.println(userPrefix + "Transport whitelist:");
            for (ComponentName transport : this.mTransportManager.getTransportWhitelist()) {
                pw.print("    ");
                pw.println(transport.flattenToShortString());
            }
            pw.println(userPrefix + "Available transports:");
            String[] transports = listAllTransports();
            if (transports != null) {
                int length = transports.length;
                int i2 = 0;
                while (i2 < length) {
                    String t = transports[i2];
                    pw.println((t.equals(this.mTransportManager.getCurrentTransportName()) ? "  * " : "    ") + t);
                    try {
                        File dir = new File(this.mBaseStateDir, this.mTransportManager.getTransportDirName(t));
                        pw.println("       destination: " + this.mTransportManager.getTransportCurrentDestinationString(t));
                        pw.println("       intent: " + this.mTransportManager.getTransportConfigurationIntent(t));
                        File[] listFiles = dir.listFiles();
                        int length2 = listFiles.length;
                        int i3 = 0;
                        while (i3 < length2) {
                            File f = listFiles[i3];
                            i = i2;
                            try {
                                pw.println("       " + f.getName() + " - " + f.length() + " state bytes");
                                i3++;
                                i2 = i;
                            } catch (Exception e) {
                                e = e;
                                Slog.e(BackupManagerService.TAG, addUserIdToLogMessage(this.mUserId, "Error in transport"), e);
                                pw.println("        Error: " + e);
                                i2 = i + 1;
                            }
                        }
                        i = i2;
                    } catch (Exception e2) {
                        e = e2;
                        i = i2;
                    }
                    i2 = i + 1;
                }
            }
            this.mTransportManager.dumpTransportClients(pw);
            pw.println(userPrefix + "Pending init: " + this.mPendingInits.size());
            Iterator<String> it = this.mPendingInits.iterator();
            while (it.hasNext()) {
                String s = it.next();
                pw.println("    " + s);
            }
            pw.print(userPrefix + "Ancestral: ");
            pw.println(Long.toHexString(this.mAncestralToken));
            pw.print(userPrefix + "Current:   ");
            pw.println(Long.toHexString(this.mCurrentToken));
            int numPackages = this.mBackupParticipants.size();
            pw.println(userPrefix + "Participants:");
            for (int i4 = 0; i4 < numPackages; i4++) {
                int uid = this.mBackupParticipants.keyAt(i4);
                pw.print("  uid: ");
                pw.println(uid);
                HashSet<String> participants = this.mBackupParticipants.valueAt(i4);
                Iterator<String> it2 = participants.iterator();
                while (it2.hasNext()) {
                    String app = it2.next();
                    pw.println("    " + app);
                }
            }
            StringBuilder append = new StringBuilder().append(userPrefix).append("Ancestral packages: ");
            Set<String> set = this.mAncestralPackages;
            pw.println(append.append(set == null ? "none" : Integer.valueOf(set.size())).toString());
            Set<String> set2 = this.mAncestralPackages;
            if (set2 != null) {
                for (String pkg : set2) {
                    pw.println("    " + pkg);
                }
            }
            Set<String> processedPackages = this.mProcessedPackagesJournal.getPackagesCopy();
            pw.println(userPrefix + "Ever backed up: " + processedPackages.size());
            for (String pkg2 : processedPackages) {
                pw.println("    " + pkg2);
            }
            pw.println(userPrefix + "Pending key/value backup: " + this.mPendingBackups.size());
            for (BackupRequest req : this.mPendingBackups.values()) {
                pw.println("    " + req);
            }
            pw.println(userPrefix + "Full backup queue:" + this.mFullBackupQueue.size());
            Iterator<FullBackupEntry> it3 = this.mFullBackupQueue.iterator();
            while (it3.hasNext()) {
                FullBackupEntry entry = it3.next();
                pw.print("    ");
                pw.print(entry.lastBackup);
                pw.print(" : ");
                pw.println(entry.packageName);
            }
        }
    }

    int getOperationTypeFromTransport(TransportConnection transportConnection) throws TransportNotAvailableException, RemoteException {
        if (shouldUseNewBackupEligibilityRules()) {
            long oldCallingId = Binder.clearCallingIdentity();
            try {
                BackupTransportClient transport = transportConnection.connectOrThrow("BMS.getOperationTypeFromTransport");
                if ((transport.getTransportFlags() & 2) != 0) {
                    return 1;
                }
                return 0;
            } finally {
                Binder.restoreCallingIdentity(oldCallingId);
            }
        }
        return 0;
    }

    boolean shouldUseNewBackupEligibilityRules() {
        return FeatureFlagUtils.isEnabled(this.mContext, "settings_use_new_backup_eligibility_rules");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String addUserIdToLogMessage(int userId, String message) {
        return "[UserID:" + userId + "] " + message;
    }

    public IBackupManager getBackupManagerBinder() {
        return this.mBackupManagerBinder;
    }
}
