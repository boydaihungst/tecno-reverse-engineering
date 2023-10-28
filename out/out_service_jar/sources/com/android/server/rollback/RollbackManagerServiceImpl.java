package com.android.server.rollback;

import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.ModuleInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.VersionedPackage;
import android.content.pm.parsing.ApkLite;
import android.content.pm.parsing.ApkLiteParseUtils;
import android.content.pm.parsing.result.ParseResult;
import android.content.pm.parsing.result.ParseTypeImpl;
import android.content.rollback.IRollbackManager;
import android.content.rollback.RollbackInfo;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.ext.SdkExtensions;
import android.provider.DeviceConfig;
import android.util.Log;
import android.util.LongArrayQueue;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.PackageWatchdog;
import com.android.server.SystemConfig;
import com.android.server.Watchdog;
import com.android.server.pm.ApexManager;
import com.android.server.pm.Installer;
import com.android.server.rollback.RollbackManagerServiceImpl;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class RollbackManagerServiceImpl extends IRollbackManager.Stub implements RollbackManagerInternal {
    private final AppDataRollbackHelper mAppDataRollbackHelper;
    private final Context mContext;
    private final Executor mExecutor;
    private final Handler mHandler;
    private final Installer mInstaller;
    private final RollbackPackageHealthObserver mPackageHealthObserver;
    private final RollbackStore mRollbackStore;
    private static final String TAG = "RollbackManager";
    private static final boolean LOCAL_LOGV = Log.isLoggable(TAG, 2);
    private static final long DEFAULT_ROLLBACK_LIFETIME_DURATION_MILLIS = TimeUnit.DAYS.toMillis(14);
    private static final long HANDLER_THREAD_TIMEOUT_DURATION_MILLIS = TimeUnit.MINUTES.toMillis(10);
    private long mRollbackLifetimeDurationInMillis = DEFAULT_ROLLBACK_LIFETIME_DURATION_MILLIS;
    private final Random mRandom = new SecureRandom();
    private final SparseBooleanArray mAllocatedRollbackIds = new SparseBooleanArray();
    private final List<Rollback> mRollbacks = new ArrayList();
    private final Runnable mRunExpiration = new Runnable() { // from class: com.android.server.rollback.RollbackManagerServiceImpl$$ExternalSyntheticLambda11
        @Override // java.lang.Runnable
        public final void run() {
            RollbackManagerServiceImpl.this.runExpiration();
        }
    };
    private final LongArrayQueue mSleepDuration = new LongArrayQueue();
    private long mRelativeBootTime = calculateRelativeBootTime();

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    private @interface ExtThread {
    }

    /* renamed from: -$$Nest$smcalculateRelativeBootTime  reason: not valid java name */
    static /* bridge */ /* synthetic */ long m6363$$Nest$smcalculateRelativeBootTime() {
        return calculateRelativeBootTime();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RollbackManagerServiceImpl(final Context context) {
        this.mContext = context;
        Installer installer = new Installer(context);
        this.mInstaller = installer;
        installer.onStart();
        this.mRollbackStore = new RollbackStore(new File(Environment.getDataDirectory(), "rollback"), new File(Environment.getDataDirectory(), "rollback-history"));
        this.mPackageHealthObserver = new RollbackPackageHealthObserver(context);
        this.mAppDataRollbackHelper = new AppDataRollbackHelper(installer);
        HandlerThread handlerThread = new HandlerThread("RollbackManagerServiceHandler");
        handlerThread.start();
        this.mHandler = new Handler(handlerThread.getLooper());
        Watchdog.getInstance().addThread(getHandler(), HANDLER_THREAD_TIMEOUT_DURATION_MILLIS);
        this.mExecutor = new HandlerExecutor(getHandler());
        getHandler().post(new Runnable() { // from class: com.android.server.rollback.RollbackManagerServiceImpl$$ExternalSyntheticLambda12
            @Override // java.lang.Runnable
            public final void run() {
                RollbackManagerServiceImpl.this.m6370x358c80ca(context);
            }
        });
        UserManager userManager = (UserManager) context.getSystemService(UserManager.class);
        for (UserHandle user : userManager.getUserHandles(true)) {
            registerUserCallbacks(user);
        }
        IntentFilter enableRollbackFilter = new IntentFilter();
        enableRollbackFilter.addAction("android.intent.action.PACKAGE_ENABLE_ROLLBACK");
        try {
            enableRollbackFilter.addDataType("application/vnd.android.package-archive");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            Slog.e(TAG, "addDataType", e);
        }
        this.mContext.registerReceiver(new AnonymousClass1(), enableRollbackFilter, null, getHandler());
        IntentFilter enableRollbackTimedOutFilter = new IntentFilter();
        enableRollbackTimedOutFilter.addAction("android.intent.action.CANCEL_ENABLE_ROLLBACK");
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.rollback.RollbackManagerServiceImpl.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                RollbackManagerServiceImpl.this.assertInWorkerThread();
                if ("android.intent.action.CANCEL_ENABLE_ROLLBACK".equals(intent.getAction())) {
                    int sessionId = intent.getIntExtra(PackageManagerInternal.EXTRA_ENABLE_ROLLBACK_SESSION_ID, -1);
                    if (RollbackManagerServiceImpl.LOCAL_LOGV) {
                        Slog.v(RollbackManagerServiceImpl.TAG, "broadcast=ACTION_CANCEL_ENABLE_ROLLBACK id=" + sessionId);
                    }
                    Rollback rollback = RollbackManagerServiceImpl.this.getRollbackForSession(sessionId);
                    if (rollback != null && rollback.isEnabling()) {
                        RollbackManagerServiceImpl.this.mRollbacks.remove(rollback);
                        RollbackManagerServiceImpl.this.deleteRollback(rollback, "Rollback canceled");
                    }
                }
            }
        }, enableRollbackTimedOutFilter, null, getHandler());
        IntentFilter userAddedIntentFilter = new IntentFilter("android.intent.action.USER_ADDED");
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.rollback.RollbackManagerServiceImpl.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                int newUserId;
                RollbackManagerServiceImpl.this.assertInWorkerThread();
                if (!"android.intent.action.USER_ADDED".equals(intent.getAction()) || (newUserId = intent.getIntExtra("android.intent.extra.user_handle", -1)) == -1) {
                    return;
                }
                RollbackManagerServiceImpl.this.registerUserCallbacks(UserHandle.of(newUserId));
            }
        }, userAddedIntentFilter, null, getHandler());
        registerTimeChangeReceiver();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-rollback-RollbackManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m6370x358c80ca(Context context) {
        this.mRollbacks.addAll(this.mRollbackStore.loadRollbacks());
        if (!context.getPackageManager().isDeviceUpgrading()) {
            for (Rollback rollback : this.mRollbacks) {
                this.mAllocatedRollbackIds.put(rollback.info.getRollbackId(), true);
            }
            return;
        }
        for (Rollback rollback2 : this.mRollbacks) {
            deleteRollback(rollback2, "Fingerprint changed");
        }
        this.mRollbacks.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.rollback.RollbackManagerServiceImpl$1  reason: invalid class name */
    /* loaded from: classes2.dex */
    public class AnonymousClass1 extends BroadcastReceiver {
        AnonymousClass1() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            RollbackManagerServiceImpl.this.assertInWorkerThread();
            if ("android.intent.action.PACKAGE_ENABLE_ROLLBACK".equals(intent.getAction())) {
                final int token = intent.getIntExtra(PackageManagerInternal.EXTRA_ENABLE_ROLLBACK_TOKEN, -1);
                final int sessionId = intent.getIntExtra(PackageManagerInternal.EXTRA_ENABLE_ROLLBACK_SESSION_ID, -1);
                RollbackManagerServiceImpl.this.queueSleepIfNeeded();
                RollbackManagerServiceImpl.this.getHandler().post(new Runnable() { // from class: com.android.server.rollback.RollbackManagerServiceImpl$1$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        RollbackManagerServiceImpl.AnonymousClass1.this.m6379xdb9ad033(sessionId, token);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onReceive$0$com-android-server-rollback-RollbackManagerServiceImpl$1  reason: not valid java name */
        public /* synthetic */ void m6379xdb9ad033(int sessionId, int token) {
            RollbackManagerServiceImpl.this.assertInWorkerThread();
            boolean success = RollbackManagerServiceImpl.this.enableRollback(sessionId);
            int ret = 1;
            if (!success) {
                ret = -1;
            }
            PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            pm.setEnableRollbackCode(token, ret);
        }
    }

    private <U> U awaitResult(Supplier<U> supplier) {
        assertNotInWorkerThread();
        try {
            return (U) CompletableFuture.supplyAsync(supplier, this.mExecutor).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void awaitResult(Runnable runnable) {
        assertNotInWorkerThread();
        try {
            CompletableFuture.runAsync(runnable, this.mExecutor).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void assertInWorkerThread() {
        Preconditions.checkState(getHandler().getLooper().isCurrentThread());
    }

    private void assertNotInWorkerThread() {
        Preconditions.checkState(!getHandler().getLooper().isCurrentThread());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerUserCallbacks(UserHandle user) {
        Context context = getContextAsUser(user);
        if (context == null) {
            Slog.e(TAG, "Unable to register user callbacks for user " + user);
            return;
        }
        context.getPackageManager().getPackageInstaller().registerSessionCallback(new SessionCallback(), getHandler());
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addAction("android.intent.action.PACKAGE_FULLY_REMOVED");
        filter.addDataScheme("package");
        context.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.rollback.RollbackManagerServiceImpl.4
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                RollbackManagerServiceImpl.this.assertInWorkerThread();
                String action = intent.getAction();
                if ("android.intent.action.PACKAGE_REPLACED".equals(action)) {
                    String packageName = intent.getData().getSchemeSpecificPart();
                    if (RollbackManagerServiceImpl.LOCAL_LOGV) {
                        Slog.v(RollbackManagerServiceImpl.TAG, "broadcast=ACTION_PACKAGE_REPLACED pkg=" + packageName);
                    }
                    RollbackManagerServiceImpl.this.onPackageReplaced(packageName);
                }
                if ("android.intent.action.PACKAGE_FULLY_REMOVED".equals(action)) {
                    String packageName2 = intent.getData().getSchemeSpecificPart();
                    Slog.i(RollbackManagerServiceImpl.TAG, "broadcast=ACTION_PACKAGE_FULLY_REMOVED pkg=" + packageName2);
                    RollbackManagerServiceImpl.this.onPackageFullyRemoved(packageName2);
                }
            }
        }, filter, null, getHandler());
    }

    public ParceledListSlice getAvailableRollbacks() {
        assertNotInWorkerThread();
        enforceManageRollbacks("getAvailableRollbacks");
        return (ParceledListSlice) awaitResult(new Supplier() { // from class: com.android.server.rollback.RollbackManagerServiceImpl$$ExternalSyntheticLambda13
            @Override // java.util.function.Supplier
            public final Object get() {
                return RollbackManagerServiceImpl.this.m6368x2fabb84d();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getAvailableRollbacks$1$com-android-server-rollback-RollbackManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ ParceledListSlice m6368x2fabb84d() {
        assertInWorkerThread();
        List<RollbackInfo> rollbacks = new ArrayList<>();
        for (int i = 0; i < this.mRollbacks.size(); i++) {
            Rollback rollback = this.mRollbacks.get(i);
            if (rollback.isAvailable()) {
                rollbacks.add(rollback.info);
            }
        }
        return new ParceledListSlice(rollbacks);
    }

    public ParceledListSlice<RollbackInfo> getRecentlyCommittedRollbacks() {
        assertNotInWorkerThread();
        enforceManageRollbacks("getRecentlyCommittedRollbacks");
        return (ParceledListSlice) awaitResult(new Supplier() { // from class: com.android.server.rollback.RollbackManagerServiceImpl$$ExternalSyntheticLambda9
            @Override // java.util.function.Supplier
            public final Object get() {
                return RollbackManagerServiceImpl.this.m6369x4873bdd7();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getRecentlyCommittedRollbacks$2$com-android-server-rollback-RollbackManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ ParceledListSlice m6369x4873bdd7() {
        assertInWorkerThread();
        List<RollbackInfo> rollbacks = new ArrayList<>();
        for (int i = 0; i < this.mRollbacks.size(); i++) {
            Rollback rollback = this.mRollbacks.get(i);
            if (rollback.isCommitted()) {
                rollbacks.add(rollback.info);
            }
        }
        return new ParceledListSlice(rollbacks);
    }

    public void commitRollback(final int rollbackId, final ParceledListSlice causePackages, final String callerPackageName, final IntentSender statusReceiver) {
        assertNotInWorkerThread();
        enforceManageRollbacks("commitRollback");
        int callingUid = Binder.getCallingUid();
        AppOpsManager appOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        appOps.checkPackage(callingUid, callerPackageName);
        getHandler().post(new Runnable() { // from class: com.android.server.rollback.RollbackManagerServiceImpl$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                RollbackManagerServiceImpl.this.m6365x8f568e92(rollbackId, causePackages, callerPackageName, statusReceiver);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$commitRollback$3$com-android-server-rollback-RollbackManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m6365x8f568e92(int rollbackId, ParceledListSlice causePackages, String callerPackageName, IntentSender statusReceiver) {
        commitRollbackInternal(rollbackId, causePackages.getList(), callerPackageName, statusReceiver);
    }

    private void registerTimeChangeReceiver() {
        BroadcastReceiver timeChangeIntentReceiver = new BroadcastReceiver() { // from class: com.android.server.rollback.RollbackManagerServiceImpl.5
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                RollbackManagerServiceImpl.this.assertInWorkerThread();
                long oldRelativeBootTime = RollbackManagerServiceImpl.this.mRelativeBootTime;
                RollbackManagerServiceImpl.this.mRelativeBootTime = RollbackManagerServiceImpl.m6363$$Nest$smcalculateRelativeBootTime();
                long timeDifference = RollbackManagerServiceImpl.this.mRelativeBootTime - oldRelativeBootTime;
                for (Rollback rollback : RollbackManagerServiceImpl.this.mRollbacks) {
                    rollback.setTimestamp(rollback.getTimestamp().plusMillis(timeDifference));
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.TIME_SET");
        this.mContext.registerReceiver(timeChangeIntentReceiver, filter, null, getHandler());
    }

    private static long calculateRelativeBootTime() {
        return System.currentTimeMillis() - SystemClock.elapsedRealtime();
    }

    private void commitRollbackInternal(int rollbackId, List<VersionedPackage> causePackages, String callerPackageName, IntentSender statusReceiver) {
        assertInWorkerThread();
        Slog.i(TAG, "commitRollback id=" + rollbackId + " caller=" + callerPackageName);
        Rollback rollback = getRollbackForId(rollbackId);
        if (rollback == null) {
            sendFailure(this.mContext, statusReceiver, 2, "Rollback unavailable");
        } else {
            rollback.commit(this.mContext, causePackages, callerPackageName, statusReceiver);
        }
    }

    public void reloadPersistedData() {
        assertNotInWorkerThread();
        this.mContext.enforceCallingOrSelfPermission("android.permission.TEST_MANAGE_ROLLBACKS", "reloadPersistedData");
        awaitResult(new Runnable() { // from class: com.android.server.rollback.RollbackManagerServiceImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                RollbackManagerServiceImpl.this.m6377x20e3f5c2();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reloadPersistedData$4$com-android-server-rollback-RollbackManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m6377x20e3f5c2() {
        assertInWorkerThread();
        this.mRollbacks.clear();
        this.mRollbacks.addAll(this.mRollbackStore.loadRollbacks());
    }

    private void expireRollbackForPackageInternal(String packageName, String reason) {
        assertInWorkerThread();
        Iterator<Rollback> iter = this.mRollbacks.iterator();
        while (iter.hasNext()) {
            Rollback rollback = iter.next();
            if (rollback.includesPackage(packageName)) {
                iter.remove();
                deleteRollback(rollback, reason);
            }
        }
    }

    public void expireRollbackForPackage(final String packageName) {
        assertNotInWorkerThread();
        this.mContext.enforceCallingOrSelfPermission("android.permission.TEST_MANAGE_ROLLBACKS", "expireRollbackForPackage");
        awaitResult(new Runnable() { // from class: com.android.server.rollback.RollbackManagerServiceImpl$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                RollbackManagerServiceImpl.this.m6367x2adb94ab(packageName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$expireRollbackForPackage$5$com-android-server-rollback-RollbackManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m6367x2adb94ab(String packageName) {
        expireRollbackForPackageInternal(packageName, "Expired by API");
    }

    public void blockRollbackManager(final long millis) {
        assertNotInWorkerThread();
        this.mContext.enforceCallingOrSelfPermission("android.permission.TEST_MANAGE_ROLLBACKS", "blockRollbackManager");
        getHandler().post(new Runnable() { // from class: com.android.server.rollback.RollbackManagerServiceImpl$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                RollbackManagerServiceImpl.this.m6364x59ab496e(millis);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$blockRollbackManager$6$com-android-server-rollback-RollbackManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m6364x59ab496e(long millis) {
        assertInWorkerThread();
        this.mSleepDuration.addLast(millis);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void queueSleepIfNeeded() {
        assertInWorkerThread();
        if (this.mSleepDuration.size() == 0) {
            return;
        }
        final long millis = this.mSleepDuration.removeFirst();
        if (millis <= 0) {
            return;
        }
        getHandler().post(new Runnable() { // from class: com.android.server.rollback.RollbackManagerServiceImpl$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                RollbackManagerServiceImpl.this.m6376x75253ed1(millis);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$queueSleepIfNeeded$7$com-android-server-rollback-RollbackManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m6376x75253ed1(long millis) {
        assertInWorkerThread();
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new IllegalStateException("RollbackManagerHandlerThread interrupted");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUnlockUser(final int userId) {
        assertNotInWorkerThread();
        if (LOCAL_LOGV) {
            Slog.v(TAG, "onUnlockUser id=" + userId);
        }
        awaitResult(new Runnable() { // from class: com.android.server.rollback.RollbackManagerServiceImpl$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                RollbackManagerServiceImpl.this.m6374xa4a35dda(userId);
            }
        });
        getHandler().post(new Runnable() { // from class: com.android.server.rollback.RollbackManagerServiceImpl$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                RollbackManagerServiceImpl.this.m6375xa5d9b0b9(userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onUnlockUser$8$com-android-server-rollback-RollbackManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m6374xa4a35dda(int userId) {
        assertInWorkerThread();
        List<Rollback> rollbacks = new ArrayList<>(this.mRollbacks);
        for (int i = 0; i < rollbacks.size(); i++) {
            Rollback rollback = rollbacks.get(i);
            rollback.commitPendingBackupAndRestoreForUser(userId, this.mAppDataRollbackHelper);
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: destroyCeSnapshotsForExpiredRollbacks */
    public void m6375xa5d9b0b9(int userId) {
        int[] rollbackIds = new int[this.mRollbacks.size()];
        for (int i = 0; i < rollbackIds.length; i++) {
            rollbackIds[i] = this.mRollbacks.get(i).info.getRollbackId();
        }
        ApexManager.getInstance().destroyCeSnapshotsNotSpecified(userId, rollbackIds);
        try {
            this.mInstaller.destroyCeSnapshotsNotSpecified(userId, rollbackIds);
        } catch (Installer.InstallerException ie) {
            Slog.e(TAG, "Failed to delete snapshots for user: " + userId, ie);
        }
    }

    private void updateRollbackLifetimeDurationInMillis() {
        assertInWorkerThread();
        long j = DEFAULT_ROLLBACK_LIFETIME_DURATION_MILLIS;
        long j2 = DeviceConfig.getLong("rollback_boot", "rollback_lifetime_in_millis", j);
        this.mRollbackLifetimeDurationInMillis = j2;
        if (j2 < 0) {
            this.mRollbackLifetimeDurationInMillis = j;
        }
        Slog.d(TAG, "mRollbackLifetimeDurationInMillis=" + this.mRollbackLifetimeDurationInMillis);
        runExpiration();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onBootCompleted() {
        DeviceConfig.addOnPropertiesChangedListener("rollback_boot", this.mExecutor, new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.rollback.RollbackManagerServiceImpl$$ExternalSyntheticLambda2
            public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                RollbackManagerServiceImpl.this.m6372x5339cad3(properties);
            }
        });
        getHandler().post(new Runnable() { // from class: com.android.server.rollback.RollbackManagerServiceImpl$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                RollbackManagerServiceImpl.this.m6373x54701db2();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onBootCompleted$10$com-android-server-rollback-RollbackManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m6372x5339cad3(DeviceConfig.Properties properties) {
        updateRollbackLifetimeDurationInMillis();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onBootCompleted$11$com-android-server-rollback-RollbackManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m6373x54701db2() {
        assertInWorkerThread();
        updateRollbackLifetimeDurationInMillis();
        runExpiration();
        List<Rollback> enabling = new ArrayList<>();
        List<Rollback> restoreInProgress = new ArrayList<>();
        Set<String> apexPackageNames = new HashSet<>();
        Iterator<Rollback> iter = this.mRollbacks.iterator();
        while (iter.hasNext()) {
            Rollback rollback = iter.next();
            if (rollback.isStaged()) {
                PackageInstaller.SessionInfo session = this.mContext.getPackageManager().getPackageInstaller().getSessionInfo(rollback.getOriginalSessionId());
                if (session == null || session.isStagedSessionFailed()) {
                    if (rollback.isEnabling()) {
                        iter.remove();
                        deleteRollback(rollback, "Session " + rollback.getOriginalSessionId() + " not existed or failed");
                    }
                } else {
                    if (session.isStagedSessionApplied()) {
                        if (rollback.isEnabling()) {
                            enabling.add(rollback);
                        } else if (rollback.isRestoreUserDataInProgress()) {
                            restoreInProgress.add(rollback);
                        }
                    }
                    apexPackageNames.addAll(rollback.getApexPackageNames());
                }
            }
        }
        for (Rollback rollback2 : enabling) {
            makeRollbackAvailable(rollback2);
        }
        for (Rollback rollback3 : restoreInProgress) {
            rollback3.setRestoreUserDataInProgress(false);
        }
        for (String apexPackageName : apexPackageNames) {
            onPackageReplaced(apexPackageName);
        }
        this.mPackageHealthObserver.onBootCompletedAsync();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPackageReplaced(String packageName) {
        assertInWorkerThread();
        long installedVersion = getInstalledPackageVersion(packageName);
        Iterator<Rollback> iter = this.mRollbacks.iterator();
        while (iter.hasNext()) {
            Rollback rollback = iter.next();
            if (rollback.isAvailable() && rollback.includesPackageWithDifferentVersion(packageName, installedVersion)) {
                iter.remove();
                deleteRollback(rollback, "Package " + packageName + " replaced");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPackageFullyRemoved(String packageName) {
        assertInWorkerThread();
        expireRollbackForPackageInternal(packageName, "Package " + packageName + " removed");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void sendFailure(Context context, IntentSender statusReceiver, int status, String message) {
        Slog.e(TAG, message);
        try {
            Intent fillIn = new Intent();
            fillIn.putExtra("android.content.rollback.extra.STATUS", status);
            fillIn.putExtra("android.content.rollback.extra.STATUS_MESSAGE", message);
            statusReceiver.sendIntent(context, 0, fillIn, null, null);
        } catch (IntentSender.SendIntentException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void runExpiration() {
        getHandler().removeCallbacks(this.mRunExpiration);
        assertInWorkerThread();
        Instant now = Instant.now();
        Instant oldest = null;
        Iterator<Rollback> iter = this.mRollbacks.iterator();
        while (iter.hasNext()) {
            Rollback rollback = iter.next();
            if (rollback.isAvailable() || rollback.isCommitted()) {
                Instant rollbackTimestamp = rollback.getTimestamp();
                if (!now.isBefore(rollbackTimestamp.plusMillis(this.mRollbackLifetimeDurationInMillis))) {
                    Slog.i(TAG, "runExpiration id=" + rollback.info.getRollbackId());
                    iter.remove();
                    deleteRollback(rollback, "Expired by timeout");
                } else if (oldest == null || oldest.isAfter(rollbackTimestamp)) {
                    oldest = rollbackTimestamp;
                }
            }
        }
        if (oldest != null) {
            long delay = now.until(oldest.plusMillis(this.mRollbackLifetimeDurationInMillis), ChronoUnit.MILLIS);
            getHandler().postDelayed(this.mRunExpiration, delay);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Handler getHandler() {
        return this.mHandler;
    }

    private Context getContextAsUser(UserHandle user) {
        try {
            Context context = this.mContext;
            return context.createPackageContextAsUser(context.getPackageName(), 0, user);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean enableRollback(int sessionId) {
        assertInWorkerThread();
        if (LOCAL_LOGV) {
            Slog.v(TAG, "enableRollback sessionId=" + sessionId);
        }
        PackageInstaller installer = this.mContext.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionInfo packageSession = installer.getSessionInfo(sessionId);
        if (packageSession == null) {
            Slog.e(TAG, "Unable to find session for enabled rollback.");
            return false;
        }
        PackageInstaller.SessionInfo parentSession = packageSession.hasParentSessionId() ? installer.getSessionInfo(packageSession.getParentSessionId()) : packageSession;
        if (parentSession == null) {
            Slog.e(TAG, "Unable to find parent session for enabled rollback.");
            return false;
        }
        Rollback newRollback = getRollbackForSession(packageSession.getSessionId());
        if (newRollback == null) {
            newRollback = createNewRollback(parentSession);
        }
        if (!enableRollbackForPackageSession(newRollback, packageSession)) {
            return false;
        }
        if (newRollback.allPackagesEnabled()) {
            return completeEnableRollback(newRollback);
        }
        return true;
    }

    private int computeRollbackDataPolicy(int sessionPolicy, int manifestPolicy) {
        assertInWorkerThread();
        if (manifestPolicy != 0) {
            return manifestPolicy;
        }
        return sessionPolicy;
    }

    private boolean enableRollbackForPackageSession(Rollback rollback, PackageInstaller.SessionInfo session) {
        RollbackManagerServiceImpl rollbackManagerServiceImpl = this;
        assertInWorkerThread();
        int installFlags = session.installFlags;
        if ((262144 & installFlags) == 0) {
            Slog.e(TAG, "Rollback is not enabled.");
            return false;
        } else if ((installFlags & 2048) != 0) {
            Slog.e(TAG, "Rollbacks not supported for instant app install");
            return false;
        } else if (session.resolvedBaseCodePath == null) {
            Slog.e(TAG, "Session code path has not been resolved.");
            return false;
        } else {
            ParseTypeImpl input = ParseTypeImpl.forDefaultParsing();
            ParseResult<ApkLite> parseResult = ApkLiteParseUtils.parseApkLite(input.reset(), new File(session.resolvedBaseCodePath), 0);
            if (parseResult.isError()) {
                Slog.e(TAG, "Unable to parse new package: " + parseResult.getErrorMessage(), parseResult.getException());
                return false;
            }
            ApkLite newPackage = (ApkLite) parseResult.getResult();
            String packageName = newPackage.getPackageName();
            int rollbackDataPolicy = rollbackManagerServiceImpl.computeRollbackDataPolicy(session.rollbackDataPolicy, newPackage.getRollbackDataPolicy());
            if (!session.isStaged() && (installFlags & 131072) != 0 && rollbackDataPolicy != 2) {
                Slog.e(TAG, "Only RETAIN is supported for rebootless APEX: " + packageName);
                return false;
            }
            Slog.i(TAG, "Enabling rollback for install of " + packageName + ", session:" + session.sessionId + ", rollbackDataPolicy=" + rollbackDataPolicy);
            String installerPackageName = session.getInstallerPackageName();
            if (!rollbackManagerServiceImpl.enableRollbackAllowed(installerPackageName, packageName)) {
                Slog.e(TAG, "Installer " + installerPackageName + " is not allowed to enable rollback on " + packageName);
                return false;
            }
            boolean isApex = (installFlags & 131072) != 0;
            try {
                PackageInfo pkgInfo = rollbackManagerServiceImpl.getPackageInfo(packageName);
                if (isApex) {
                    PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                    List<String> apksInApex = pmi.getApksInApex(packageName);
                    Iterator<String> it = apksInApex.iterator();
                    while (it.hasNext()) {
                        String apkInApex = it.next();
                        try {
                            PackageInfo apkPkgInfo = rollbackManagerServiceImpl.getPackageInfo(apkInApex);
                            Iterator<String> it2 = it;
                            if (!rollback.enableForPackageInApex(apkInApex, apkPkgInfo.getLongVersionCode(), rollbackDataPolicy)) {
                                return false;
                            }
                            rollbackManagerServiceImpl = this;
                            it = it2;
                        } catch (PackageManager.NameNotFoundException e) {
                            Slog.e(TAG, apkInApex + " is not installed");
                            return false;
                        }
                    }
                }
                ApplicationInfo appInfo = pkgInfo.applicationInfo;
                return rollback.enableForPackage(packageName, newPackage.getVersionCode(), pkgInfo.getLongVersionCode(), isApex, appInfo.sourceDir, appInfo.splitSourceDirs, rollbackDataPolicy);
            } catch (PackageManager.NameNotFoundException e2) {
                Slog.e(TAG, packageName + " is not installed");
                return false;
            }
        }
    }

    @Override // com.android.server.rollback.RollbackManagerInternal
    public void snapshotAndRestoreUserData(String packageName, List<UserHandle> users, int appId, long ceDataInode, String seInfo, int token) {
        assertNotInWorkerThread();
        snapshotAndRestoreUserData(packageName, UserHandle.fromUserHandles(users), appId, ceDataInode, seInfo, token);
    }

    public void snapshotAndRestoreUserData(final String packageName, final int[] userIds, final int appId, long ceDataInode, final String seInfo, final int token) {
        assertNotInWorkerThread();
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("snapshotAndRestoreUserData may only be called by the system.");
        }
        getHandler().post(new Runnable() { // from class: com.android.server.rollback.RollbackManagerServiceImpl$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                RollbackManagerServiceImpl.this.m6378x4d62ff61(packageName, userIds, appId, seInfo, token);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$snapshotAndRestoreUserData$12$com-android-server-rollback-RollbackManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m6378x4d62ff61(String packageName, int[] userIds, int appId, String seInfo, int token) {
        assertInWorkerThread();
        snapshotUserDataInternal(packageName, userIds);
        restoreUserDataInternal(packageName, userIds, appId, seInfo);
        if (token > 0) {
            PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            pmi.finishPackageInstall(token, false);
        }
    }

    private void snapshotUserDataInternal(String packageName, int[] userIds) {
        assertInWorkerThread();
        if (LOCAL_LOGV) {
            Slog.v(TAG, "snapshotUserData pkg=" + packageName + " users=" + Arrays.toString(userIds));
        }
        for (int i = 0; i < this.mRollbacks.size(); i++) {
            Rollback rollback = this.mRollbacks.get(i);
            rollback.snapshotUserData(packageName, userIds, this.mAppDataRollbackHelper);
        }
    }

    private void restoreUserDataInternal(String packageName, int[] userIds, int appId, String seInfo) {
        assertInWorkerThread();
        if (LOCAL_LOGV) {
            Slog.v(TAG, "restoreUserData pkg=" + packageName + " users=" + Arrays.toString(userIds));
        }
        for (int i = 0; i < this.mRollbacks.size(); i++) {
            Rollback rollback = this.mRollbacks.get(i);
            if (rollback.restoreUserDataForPackageIfInProgress(packageName, userIds, appId, seInfo, this.mAppDataRollbackHelper)) {
                return;
            }
        }
    }

    @Override // com.android.server.rollback.RollbackManagerInternal
    public int notifyStagedSession(final int sessionId) {
        assertNotInWorkerThread();
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("notifyStagedSession may only be called by the system.");
        }
        return ((Integer) awaitResult(new Supplier() { // from class: com.android.server.rollback.RollbackManagerServiceImpl$$ExternalSyntheticLambda10
            @Override // java.util.function.Supplier
            public final Object get() {
                return RollbackManagerServiceImpl.this.m6371x5b2fb59d(sessionId);
            }
        })).intValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyStagedSession$13$com-android-server-rollback-RollbackManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ Integer m6371x5b2fb59d(int sessionId) {
        assertInWorkerThread();
        Rollback rollback = getRollbackForSession(sessionId);
        return Integer.valueOf(rollback != null ? rollback.info.getRollbackId() : -1);
    }

    private boolean enableRollbackAllowed(String installerPackageName, String packageName) {
        if (installerPackageName == null) {
            return false;
        }
        PackageManager pm = this.mContext.getPackageManager();
        boolean manageRollbacksGranted = pm.checkPermission("android.permission.MANAGE_ROLLBACKS", installerPackageName) == 0;
        boolean testManageRollbacksGranted = pm.checkPermission("android.permission.TEST_MANAGE_ROLLBACKS", installerPackageName) == 0;
        return (isRollbackAllowed(packageName) && manageRollbacksGranted) || testManageRollbacksGranted;
    }

    private boolean isRollbackAllowed(String packageName) {
        return SystemConfig.getInstance().getRollbackWhitelistedPackages().contains(packageName) || isModule(packageName);
    }

    private boolean isModule(String packageName) {
        PackageManager pm = this.mContext.getPackageManager();
        try {
            ModuleInfo moduleInfo = pm.getModuleInfo(packageName, 0);
            return moduleInfo != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private long getInstalledPackageVersion(String packageName) {
        try {
            PackageInfo pkgInfo = getPackageInfo(packageName);
            return pkgInfo.getLongVersionCode();
        } catch (PackageManager.NameNotFoundException e) {
            return -1L;
        }
    }

    private PackageInfo getPackageInfo(String packageName) throws PackageManager.NameNotFoundException {
        PackageManager pm = this.mContext.getPackageManager();
        try {
            return pm.getPackageInfo(packageName, 4194304);
        } catch (PackageManager.NameNotFoundException e) {
            return pm.getPackageInfo(packageName, 1073741824);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class SessionCallback extends PackageInstaller.SessionCallback {
        private SessionCallback() {
        }

        @Override // android.content.pm.PackageInstaller.SessionCallback
        public void onCreated(int sessionId) {
        }

        @Override // android.content.pm.PackageInstaller.SessionCallback
        public void onBadgingChanged(int sessionId) {
        }

        @Override // android.content.pm.PackageInstaller.SessionCallback
        public void onActiveChanged(int sessionId, boolean active) {
        }

        @Override // android.content.pm.PackageInstaller.SessionCallback
        public void onProgressChanged(int sessionId, float progress) {
        }

        @Override // android.content.pm.PackageInstaller.SessionCallback
        public void onFinished(int sessionId, boolean success) {
            RollbackManagerServiceImpl.this.assertInWorkerThread();
            if (RollbackManagerServiceImpl.LOCAL_LOGV) {
                Slog.v(RollbackManagerServiceImpl.TAG, "SessionCallback.onFinished id=" + sessionId + " success=" + success);
            }
            Rollback rollback = RollbackManagerServiceImpl.this.getRollbackForSession(sessionId);
            if (rollback == null || !rollback.isEnabling() || sessionId != rollback.getOriginalSessionId()) {
                return;
            }
            if (success) {
                if (!rollback.isStaged() && RollbackManagerServiceImpl.this.completeEnableRollback(rollback)) {
                    RollbackManagerServiceImpl.this.makeRollbackAvailable(rollback);
                    return;
                }
                return;
            }
            Slog.w(RollbackManagerServiceImpl.TAG, "Delete rollback id=" + rollback.info.getRollbackId() + " for failed session id=" + sessionId);
            RollbackManagerServiceImpl.this.mRollbacks.remove(rollback);
            RollbackManagerServiceImpl.this.deleteRollback(rollback, "Session " + sessionId + " failed");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean completeEnableRollback(Rollback rollback) {
        assertInWorkerThread();
        if (LOCAL_LOGV) {
            Slog.v(TAG, "completeEnableRollback id=" + rollback.info.getRollbackId());
        }
        if (!rollback.allPackagesEnabled()) {
            Slog.e(TAG, "Failed to enable rollback for all packages in session.");
            this.mRollbacks.remove(rollback);
            deleteRollback(rollback, "Failed to enable rollback for all packages in session");
            return false;
        }
        rollback.saveRollback();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void makeRollbackAvailable(Rollback rollback) {
        assertInWorkerThread();
        Slog.i(TAG, "makeRollbackAvailable id=" + rollback.info.getRollbackId());
        rollback.makeAvailable();
        this.mPackageHealthObserver.notifyRollbackAvailable(rollback.info);
        this.mPackageHealthObserver.startObservingHealth(rollback.getPackageNames(), this.mRollbackLifetimeDurationInMillis);
        runExpiration();
    }

    private Rollback getRollbackForId(int rollbackId) {
        assertInWorkerThread();
        for (int i = 0; i < this.mRollbacks.size(); i++) {
            Rollback rollback = this.mRollbacks.get(i);
            if (rollback.info.getRollbackId() == rollbackId) {
                return rollback;
            }
        }
        return null;
    }

    private int allocateRollbackId() {
        assertInWorkerThread();
        int n = 0;
        while (true) {
            int rollbackId = this.mRandom.nextInt(2147483646) + 1;
            if (!this.mAllocatedRollbackIds.get(rollbackId, false)) {
                this.mAllocatedRollbackIds.put(rollbackId, true);
                return rollbackId;
            }
            int n2 = n + 1;
            if (n >= 32) {
                throw new IllegalStateException("Failed to allocate rollback ID");
            }
            n = n2;
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        assertNotInWorkerThread();
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            final IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
            awaitResult(new Runnable() { // from class: com.android.server.rollback.RollbackManagerServiceImpl$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    RollbackManagerServiceImpl.this.m6366x1bd3bd03(ipw);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dump$14$com-android-server-rollback-RollbackManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m6366x1bd3bd03(IndentingPrintWriter ipw) {
        assertInWorkerThread();
        for (Rollback rollback : this.mRollbacks) {
            rollback.dump(ipw);
        }
        ipw.println();
        List<Rollback> historicalRollbacks = this.mRollbackStore.loadHistorialRollbacks();
        if (!historicalRollbacks.isEmpty()) {
            ipw.println("Historical rollbacks:");
            ipw.increaseIndent();
            for (Rollback rollback2 : historicalRollbacks) {
                rollback2.dump(ipw);
            }
            ipw.decreaseIndent();
            ipw.println();
        }
        PackageWatchdog.getInstance(this.mContext).dump(ipw);
    }

    private void enforceManageRollbacks(String message) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MANAGE_ROLLBACKS") != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.TEST_MANAGE_ROLLBACKS") != 0) {
            throw new SecurityException(message + " requires android.permission.MANAGE_ROLLBACKS or android.permission.TEST_MANAGE_ROLLBACKS");
        }
    }

    private Rollback createNewRollback(PackageInstaller.SessionInfo parentSession) {
        int userId;
        Rollback rollback;
        assertInWorkerThread();
        int rollbackId = allocateRollbackId();
        if (parentSession.getUser().equals(UserHandle.ALL)) {
            userId = UserHandle.SYSTEM.getIdentifier();
        } else {
            userId = parentSession.getUser().getIdentifier();
        }
        String installerPackageName = parentSession.getInstallerPackageName();
        int parentSessionId = parentSession.getSessionId();
        if (LOCAL_LOGV) {
            Slog.v(TAG, "createNewRollback id=" + rollbackId + " user=" + userId + " installer=" + installerPackageName);
        }
        int[] packageSessionIds = parentSession.isMultiPackage() ? parentSession.getChildSessionIds() : new int[]{parentSessionId};
        if (parentSession.isStaged()) {
            rollback = this.mRollbackStore.createStagedRollback(rollbackId, parentSessionId, userId, installerPackageName, packageSessionIds, getExtensionVersions());
        } else {
            rollback = this.mRollbackStore.createNonStagedRollback(rollbackId, parentSessionId, userId, installerPackageName, packageSessionIds, getExtensionVersions());
        }
        this.mRollbacks.add(rollback);
        return rollback;
    }

    private SparseIntArray getExtensionVersions() {
        Map<Integer, Integer> allExtensionVersions = SdkExtensions.getAllExtensionVersions();
        SparseIntArray result = new SparseIntArray(allExtensionVersions.size());
        for (Integer num : allExtensionVersions.keySet()) {
            int extension = num.intValue();
            result.put(extension, allExtensionVersions.get(Integer.valueOf(extension)).intValue());
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Rollback getRollbackForSession(int sessionId) {
        assertInWorkerThread();
        for (int i = 0; i < this.mRollbacks.size(); i++) {
            Rollback rollback = this.mRollbacks.get(i);
            if (rollback.getOriginalSessionId() == sessionId || rollback.containsSessionId(sessionId)) {
                return rollback;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void deleteRollback(Rollback rollback, String reason) {
        assertInWorkerThread();
        rollback.delete(this.mAppDataRollbackHelper, reason);
        this.mRollbackStore.saveRollbackToHistory(rollback);
    }
}
