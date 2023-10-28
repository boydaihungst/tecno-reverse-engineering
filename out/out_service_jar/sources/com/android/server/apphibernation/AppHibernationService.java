package com.android.server.apphibernation;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.app.StatsManager;
import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManagerInternal;
import android.apphibernation.HibernationStats;
import android.apphibernation.IAppHibernationService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.DeviceConfig;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.util.StatsEvent;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
/* loaded from: classes.dex */
public final class AppHibernationService extends SystemService {
    private static final int PACKAGE_MATCH_FLAGS = 537698816;
    private static final String TAG = "AppHibernationService";
    public static boolean sIsServiceEnabled;
    private final Executor mBackgroundExecutor;
    private final BroadcastReceiver mBroadcastReceiver;
    private final Context mContext;
    private final Map<String, GlobalLevelState> mGlobalHibernationStates;
    private final HibernationStateDiskStore<GlobalLevelState> mGlobalLevelHibernationDiskStore;
    private final IActivityManager mIActivityManager;
    private final IPackageManager mIPackageManager;
    private final Injector mInjector;
    private final AppHibernationManagerInternal mLocalService;
    private final Object mLock;
    private final boolean mOatArtifactDeletionEnabled;
    private final PackageManagerInternal mPackageManagerInternal;
    private final AppHibernationServiceStub mServiceStub;
    private final StorageStatsManager mStorageStatsManager;
    private final UsageStatsManagerInternal.UsageEventListener mUsageEventListener;
    private final SparseArray<HibernationStateDiskStore<UserLevelState>> mUserDiskStores;
    private final UserManager mUserManager;
    private final SparseArray<Map<String, UserLevelState>> mUserStates;

    /* loaded from: classes.dex */
    interface Injector {
        IActivityManager getActivityManager();

        Executor getBackgroundExecutor();

        Context getContext();

        HibernationStateDiskStore<GlobalLevelState> getGlobalLevelDiskStore();

        IPackageManager getPackageManager();

        PackageManagerInternal getPackageManagerInternal();

        StorageStatsManager getStorageStatsManager();

        UsageStatsManagerInternal getUsageStatsManagerInternal();

        HibernationStateDiskStore<UserLevelState> getUserLevelDiskStore(int i);

        UserManager getUserManager();

        boolean isOatArtifactDeletionEnabled();
    }

    public AppHibernationService(Context context) {
        this(new InjectorImpl(context));
    }

    AppHibernationService(Injector injector) {
        super(injector.getContext());
        this.mLock = new Object();
        this.mUserStates = new SparseArray<>();
        this.mUserDiskStores = new SparseArray<>();
        this.mGlobalHibernationStates = new ArrayMap();
        LocalService localService = new LocalService(this);
        this.mLocalService = localService;
        this.mServiceStub = new AppHibernationServiceStub(this);
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.apphibernation.AppHibernationService.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                if (userId == -10000) {
                    return;
                }
                String action = intent.getAction();
                if ("android.intent.action.PACKAGE_ADDED".equals(action) || "android.intent.action.PACKAGE_REMOVED".equals(action)) {
                    String packageName = intent.getData().getSchemeSpecificPart();
                    if (intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                        return;
                    }
                    if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                        AppHibernationService.this.onPackageAdded(packageName, userId);
                    } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                        AppHibernationService.this.onPackageRemoved(packageName, userId);
                        if (intent.getBooleanExtra("android.intent.extra.REMOVED_FOR_ALL_USERS", false)) {
                            AppHibernationService.this.onPackageRemovedForAllUsers(packageName);
                        }
                    }
                }
            }
        };
        this.mBroadcastReceiver = broadcastReceiver;
        UsageStatsManagerInternal.UsageEventListener usageEventListener = new UsageStatsManagerInternal.UsageEventListener() { // from class: com.android.server.apphibernation.AppHibernationService$$ExternalSyntheticLambda5
            @Override // android.app.usage.UsageStatsManagerInternal.UsageEventListener
            public final void onUsageEvent(int i, UsageEvents.Event event) {
                AppHibernationService.this.m1601x6d4ab619(i, event);
            }
        };
        this.mUsageEventListener = usageEventListener;
        Context context = injector.getContext();
        this.mContext = context;
        this.mIPackageManager = injector.getPackageManager();
        this.mPackageManagerInternal = injector.getPackageManagerInternal();
        this.mIActivityManager = injector.getActivityManager();
        this.mUserManager = injector.getUserManager();
        this.mStorageStatsManager = injector.getStorageStatsManager();
        this.mGlobalLevelHibernationDiskStore = injector.getGlobalLevelDiskStore();
        this.mBackgroundExecutor = injector.getBackgroundExecutor();
        this.mOatArtifactDeletionEnabled = injector.isOatArtifactDeletionEnabled();
        this.mInjector = injector;
        Context userAllContext = context.createContextAsUser(UserHandle.ALL, 0);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addDataScheme("package");
        userAllContext.registerReceiver(broadcastReceiver, intentFilter);
        LocalServices.addService(AppHibernationManagerInternal.class, localService);
        injector.getUsageStatsManagerInternal().registerListener(usageEventListener);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("app_hibernation", this.mServiceStub);
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 1000) {
            this.mBackgroundExecutor.execute(new Runnable() { // from class: com.android.server.apphibernation.AppHibernationService$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    AppHibernationService.this.m1602x3f008d7d();
                }
            });
        }
        if (phase == 500) {
            sIsServiceEnabled = isDeviceConfigAppHibernationEnabled();
            DeviceConfig.addOnPropertiesChangedListener("app_hibernation", ActivityThread.currentApplication().getMainExecutor(), new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.apphibernation.AppHibernationService$$ExternalSyntheticLambda7
                public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                    AppHibernationService.this.onDeviceConfigChanged(properties);
                }
            });
            StatsManager statsManager = (StatsManager) getContext().getSystemService(StatsManager.class);
            StatsPullAtomCallbackImpl pullAtomCallback = new StatsPullAtomCallbackImpl();
            statsManager.setPullAtomCallback((int) FrameworkStatsLog.USER_LEVEL_HIBERNATED_APPS, (StatsManager.PullAtomMetadata) null, this.mBackgroundExecutor, pullAtomCallback);
            statsManager.setPullAtomCallback((int) FrameworkStatsLog.GLOBAL_HIBERNATED_APPS, (StatsManager.PullAtomMetadata) null, this.mBackgroundExecutor, pullAtomCallback);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onBootPhase$0$com-android-server-apphibernation-AppHibernationService  reason: not valid java name */
    public /* synthetic */ void m1602x3f008d7d() {
        List<GlobalLevelState> states = this.mGlobalLevelHibernationDiskStore.readHibernationStates();
        synchronized (this.mLock) {
            initializeGlobalHibernationStates(states);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isOatArtifactDeletionEnabled() {
        return this.mOatArtifactDeletionEnabled;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isHibernatingForUser(String packageName, int userId) {
        if (sIsServiceEnabled) {
            getContext().enforceCallingOrSelfPermission("android.permission.MANAGE_APP_HIBERNATION", "Caller did not have permission while calling isHibernatingForUser");
            int userId2 = handleIncomingUser(userId, "isHibernatingForUser");
            synchronized (this.mLock) {
                if (checkUserStatesExist(userId2, "isHibernatingForUser", false)) {
                    Map<String, UserLevelState> packageStates = this.mUserStates.get(userId2);
                    UserLevelState pkgState = packageStates.get(packageName);
                    if (pkgState != null && this.mPackageManagerInternal.canQueryPackage(Binder.getCallingUid(), packageName)) {
                        return pkgState.hibernated;
                    }
                    return false;
                }
                return false;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isHibernatingGlobally(String packageName) {
        if (sIsServiceEnabled) {
            getContext().enforceCallingOrSelfPermission("android.permission.MANAGE_APP_HIBERNATION", "Caller does not have MANAGE_APP_HIBERNATION permission.");
            synchronized (this.mLock) {
                GlobalLevelState state = this.mGlobalHibernationStates.get(packageName);
                if (state != null && this.mPackageManagerInternal.canQueryPackage(Binder.getCallingUid(), packageName)) {
                    return state.hibernated;
                }
                return false;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setHibernatingForUser(final String packageName, int userId, boolean isHibernating) {
        if (!sIsServiceEnabled) {
            return;
        }
        getContext().enforceCallingOrSelfPermission("android.permission.MANAGE_APP_HIBERNATION", "Caller does not have MANAGE_APP_HIBERNATION permission.");
        final int realUserId = handleIncomingUser(userId, "setHibernatingForUser");
        synchronized (this.mLock) {
            if (checkUserStatesExist(realUserId, "setHibernatingForUser", true)) {
                Map<String, UserLevelState> packageStates = this.mUserStates.get(realUserId);
                final UserLevelState pkgState = packageStates.get(packageName);
                if (pkgState != null && this.mPackageManagerInternal.canQueryPackage(Binder.getCallingUid(), packageName)) {
                    if (pkgState.hibernated == isHibernating) {
                        return;
                    }
                    pkgState.hibernated = isHibernating;
                    if (isHibernating) {
                        this.mBackgroundExecutor.execute(new Runnable() { // from class: com.android.server.apphibernation.AppHibernationService$$ExternalSyntheticLambda2
                            @Override // java.lang.Runnable
                            public final void run() {
                                AppHibernationService.this.m1604x13e9aa9b(packageName, realUserId, pkgState);
                            }
                        });
                    } else {
                        this.mBackgroundExecutor.execute(new Runnable() { // from class: com.android.server.apphibernation.AppHibernationService$$ExternalSyntheticLambda3
                            @Override // java.lang.Runnable
                            public final void run() {
                                AppHibernationService.this.m1605x397db39c(packageName, realUserId);
                            }
                        });
                        pkgState.lastUnhibernatedMs = System.currentTimeMillis();
                    }
                    final UserLevelState stateSnapshot = new UserLevelState(pkgState);
                    this.mBackgroundExecutor.execute(new Runnable() { // from class: com.android.server.apphibernation.AppHibernationService$$ExternalSyntheticLambda4
                        @Override // java.lang.Runnable
                        public final void run() {
                            FrameworkStatsLog.write((int) FrameworkStatsLog.USER_LEVEL_HIBERNATION_STATE_CHANGED, r0.packageName, realUserId, UserLevelState.this.hibernated);
                        }
                    });
                    List<UserLevelState> states = new ArrayList<>(this.mUserStates.get(realUserId).values());
                    this.mUserDiskStores.get(realUserId).scheduleWriteHibernationStates(states);
                    return;
                }
                Slog.e(TAG, TextUtils.formatSimple("Package %s is not installed for user %s", new Object[]{packageName, Integer.valueOf(realUserId)}));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setHibernatingGlobally(final String packageName, boolean isHibernating) {
        if (!sIsServiceEnabled) {
            return;
        }
        getContext().enforceCallingOrSelfPermission("android.permission.MANAGE_APP_HIBERNATION", "Caller does not have MANAGE_APP_HIBERNATION permission.");
        synchronized (this.mLock) {
            final GlobalLevelState state = this.mGlobalHibernationStates.get(packageName);
            if (state != null && this.mPackageManagerInternal.canQueryPackage(Binder.getCallingUid(), packageName)) {
                if (state.hibernated != isHibernating) {
                    state.hibernated = isHibernating;
                    if (isHibernating) {
                        this.mBackgroundExecutor.execute(new Runnable() { // from class: com.android.server.apphibernation.AppHibernationService$$ExternalSyntheticLambda0
                            @Override // java.lang.Runnable
                            public final void run() {
                                AppHibernationService.this.m1606xc050d1ee(packageName, state);
                            }
                        });
                    } else {
                        state.savedByte = 0L;
                        state.lastUnhibernatedMs = System.currentTimeMillis();
                    }
                    List<GlobalLevelState> states = new ArrayList<>(this.mGlobalHibernationStates.values());
                    this.mGlobalLevelHibernationDiskStore.scheduleWriteHibernationStates(states);
                }
                return;
            }
            Slog.e(TAG, TextUtils.formatSimple("Package %s is not installed for any user", new Object[]{packageName}));
        }
    }

    List<String> getHibernatingPackagesForUser(int userId) {
        ArrayList<String> hibernatingPackages = new ArrayList<>();
        if (!sIsServiceEnabled) {
            return hibernatingPackages;
        }
        getContext().enforceCallingOrSelfPermission("android.permission.MANAGE_APP_HIBERNATION", "Caller does not have MANAGE_APP_HIBERNATION permission.");
        int userId2 = handleIncomingUser(userId, "getHibernatingPackagesForUser");
        synchronized (this.mLock) {
            if (checkUserStatesExist(userId2, "getHibernatingPackagesForUser", true)) {
                Map<String, UserLevelState> userStates = this.mUserStates.get(userId2);
                for (UserLevelState state : userStates.values()) {
                    String packageName = state.packageName;
                    if (this.mPackageManagerInternal.canQueryPackage(Binder.getCallingUid(), packageName)) {
                        if (state.hibernated) {
                            hibernatingPackages.add(state.packageName);
                        }
                    }
                }
                return hibernatingPackages;
            }
            return hibernatingPackages;
        }
    }

    public Map<String, HibernationStats> getHibernationStatsForUser(Set<String> packageNames, int userId) {
        Map<String, HibernationStats> statsMap = new ArrayMap<>();
        if (!sIsServiceEnabled) {
            return statsMap;
        }
        getContext().enforceCallingOrSelfPermission("android.permission.MANAGE_APP_HIBERNATION", "Caller does not have MANAGE_APP_HIBERNATION permission.");
        int userId2 = handleIncomingUser(userId, "getHibernationStatsForUser");
        synchronized (this.mLock) {
            if (checkUserStatesExist(userId2, "getHibernationStatsForUser", true)) {
                Map<String, UserLevelState> userPackageStates = this.mUserStates.get(userId2);
                Set<String> pkgs = packageNames != null ? packageNames : userPackageStates.keySet();
                for (String pkgName : pkgs) {
                    if (this.mPackageManagerInternal.canQueryPackage(Binder.getCallingUid(), pkgName)) {
                        if (this.mGlobalHibernationStates.containsKey(pkgName) && userPackageStates.containsKey(pkgName)) {
                            long diskBytesSaved = this.mGlobalHibernationStates.get(pkgName).savedByte + userPackageStates.get(pkgName).savedByte;
                            HibernationStats stats = new HibernationStats(diskBytesSaved);
                            statsMap.put(pkgName, stats);
                        }
                        Slog.w(TAG, TextUtils.formatSimple("No hibernation state associated with package %s user %d. Maybethe package was uninstalled? ", new Object[]{pkgName, Integer.valueOf(userId2)}));
                    }
                }
                return statsMap;
            }
            return statsMap;
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: hibernatePackageForUser */
    public void m1604x13e9aa9b(String packageName, int userId, UserLevelState state) {
        Trace.traceBegin(524288L, "hibernatePackage");
        long caller = Binder.clearCallingIdentity();
        try {
            try {
                try {
                    ApplicationInfo info = this.mIPackageManager.getApplicationInfo(packageName, 537698816L, userId);
                    StorageStats stats = this.mStorageStatsManager.queryStatsForPackage(info.storageUuid, packageName, new UserHandle(userId));
                    this.mIActivityManager.forceStopPackage(packageName, userId);
                    this.mIPackageManager.deleteApplicationCacheFilesAsUser(packageName, userId, (IPackageDataObserver) null);
                    synchronized (this.mLock) {
                        state.savedByte = stats.getCacheBytes();
                    }
                } catch (IOException e) {
                    Slog.e(TAG, "Storage device not found", e);
                }
            } catch (PackageManager.NameNotFoundException e2) {
                Slog.e(TAG, "Package name not found when querying storage stats", e2);
            } catch (RemoteException e3) {
                throw new IllegalStateException("Failed to hibernate due to manager not being available", e3);
            }
        } finally {
            Binder.restoreCallingIdentity(caller);
            Trace.traceEnd(524288L);
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: unhibernatePackageForUser */
    public void m1605x397db39c(String packageName, int userId) {
        Trace.traceBegin(524288L, "unhibernatePackage");
        long caller = Binder.clearCallingIdentity();
        try {
            try {
                Intent lockedBcIntent = new Intent("android.intent.action.LOCKED_BOOT_COMPLETED").setPackage(packageName);
                String[] requiredPermissions = {"android.permission.RECEIVE_BOOT_COMPLETED"};
                this.mIActivityManager.broadcastIntentWithFeature((IApplicationThread) null, (String) null, lockedBcIntent, (String) null, (IIntentReceiver) null, -1, (String) null, (Bundle) null, requiredPermissions, (String[]) null, (String[]) null, -1, (Bundle) null, false, false, userId);
                Intent bcIntent = new Intent("android.intent.action.BOOT_COMPLETED").setPackage(packageName);
                this.mIActivityManager.broadcastIntentWithFeature((IApplicationThread) null, (String) null, bcIntent, (String) null, (IIntentReceiver) null, -1, (String) null, (Bundle) null, requiredPermissions, (String[]) null, (String[]) null, -1, (Bundle) null, false, false, userId);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        } finally {
            Binder.restoreCallingIdentity(caller);
            Trace.traceEnd(524288L);
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: hibernatePackageGlobally */
    public void m1606xc050d1ee(String packageName, GlobalLevelState state) {
        Trace.traceBegin(524288L, "hibernatePackageGlobally");
        long savedBytes = 0;
        if (this.mOatArtifactDeletionEnabled) {
            savedBytes = Math.max(this.mPackageManagerInternal.deleteOatArtifactsOfPackage(packageName), 0L);
        }
        synchronized (this.mLock) {
            state.savedByte = savedBytes;
        }
        Trace.traceEnd(524288L);
    }

    private void initializeUserHibernationStates(int userId, List<UserLevelState> diskStates) {
        try {
            List<PackageInfo> packages = this.mIPackageManager.getInstalledPackages(537698816L, userId).getList();
            Map<String, UserLevelState> userLevelStates = new ArrayMap<>();
            int size = packages.size();
            for (int i = 0; i < size; i++) {
                String packageName = packages.get(i).packageName;
                UserLevelState state = new UserLevelState();
                state.packageName = packageName;
                userLevelStates.put(packageName, state);
            }
            if (diskStates != null) {
                Map<String, PackageInfo> installedPackages = new ArrayMap<>();
                int size2 = packages.size();
                for (int i2 = 0; i2 < size2; i2++) {
                    installedPackages.put(packages.get(i2).packageName, packages.get(i2));
                }
                int size3 = diskStates.size();
                for (int i3 = 0; i3 < size3; i3++) {
                    String packageName2 = diskStates.get(i3).packageName;
                    PackageInfo pkgInfo = installedPackages.get(packageName2);
                    UserLevelState currentState = diskStates.get(i3);
                    if (pkgInfo == null) {
                        Slog.w(TAG, TextUtils.formatSimple("No hibernation state associated with package %s user %d. Maybethe package was uninstalled? ", new Object[]{packageName2, Integer.valueOf(userId)}));
                    } else {
                        if (pkgInfo.applicationInfo != null) {
                            ApplicationInfo applicationInfo = pkgInfo.applicationInfo;
                            int i4 = applicationInfo.flags & 2097152;
                            applicationInfo.flags = i4;
                            if (i4 == 0 && currentState.hibernated) {
                                currentState.hibernated = false;
                                currentState.lastUnhibernatedMs = System.currentTimeMillis();
                            }
                        }
                        userLevelStates.put(packageName2, currentState);
                    }
                }
            }
            this.mUserStates.put(userId, userLevelStates);
        } catch (RemoteException e) {
            throw new IllegalStateException("Package manager not available", e);
        }
    }

    private void initializeGlobalHibernationStates(List<GlobalLevelState> diskStates) {
        try {
            List<PackageInfo> packages = this.mIPackageManager.getInstalledPackages(541893120L, 0).getList();
            int size = packages.size();
            for (int i = 0; i < size; i++) {
                String packageName = packages.get(i).packageName;
                GlobalLevelState state = new GlobalLevelState();
                state.packageName = packageName;
                this.mGlobalHibernationStates.put(packageName, state);
            }
            if (diskStates != null) {
                Set<String> installedPackages = new ArraySet<>();
                int size2 = packages.size();
                for (int i2 = 0; i2 < size2; i2++) {
                    installedPackages.add(packages.get(i2).packageName);
                }
                int size3 = diskStates.size();
                for (int i3 = 0; i3 < size3; i3++) {
                    GlobalLevelState state2 = diskStates.get(i3);
                    if (installedPackages.contains(state2.packageName)) {
                        this.mGlobalHibernationStates.put(state2.packageName, state2);
                    } else {
                        Slog.w(TAG, TextUtils.formatSimple("No hibernation state associated with package %s. Maybe the package was uninstalled? ", new Object[]{state2.packageName}));
                    }
                }
            }
        } catch (RemoteException e) {
            throw new IllegalStateException("Package manager not available", e);
        }
    }

    @Override // com.android.server.SystemService
    public void onUserUnlocking(SystemService.TargetUser user) {
        final int userId = user.getUserIdentifier();
        final HibernationStateDiskStore<UserLevelState> diskStore = this.mInjector.getUserLevelDiskStore(userId);
        this.mUserDiskStores.put(userId, diskStore);
        this.mBackgroundExecutor.execute(new Runnable() { // from class: com.android.server.apphibernation.AppHibernationService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                AppHibernationService.this.m1603xf76da4ec(diskStore, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onUserUnlocking$5$com-android-server-apphibernation-AppHibernationService  reason: not valid java name */
    public /* synthetic */ void m1603xf76da4ec(HibernationStateDiskStore diskStore, int userId) {
        List<UserLevelState> storedStates = diskStore.readHibernationStates();
        synchronized (this.mLock) {
            if (this.mUserManager.isUserUnlockingOrUnlocked(userId)) {
                initializeUserHibernationStates(userId, storedStates);
                for (UserLevelState userState : this.mUserStates.get(userId).values()) {
                    String pkgName = userState.packageName;
                    GlobalLevelState globalState = this.mGlobalHibernationStates.get(pkgName);
                    if (globalState.hibernated && !userState.hibernated) {
                        setHibernatingGlobally(pkgName, false);
                    }
                }
            }
        }
    }

    @Override // com.android.server.SystemService
    public void onUserStopping(SystemService.TargetUser user) {
        int userId = user.getUserIdentifier();
        synchronized (this.mLock) {
            this.mUserDiskStores.remove(userId);
            this.mUserStates.remove(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPackageAdded(String packageName, int userId) {
        synchronized (this.mLock) {
            if (this.mUserStates.contains(userId)) {
                UserLevelState userState = new UserLevelState();
                userState.packageName = packageName;
                this.mUserStates.get(userId).put(packageName, userState);
                if (!this.mGlobalHibernationStates.containsKey(packageName)) {
                    GlobalLevelState globalState = new GlobalLevelState();
                    globalState.packageName = packageName;
                    this.mGlobalHibernationStates.put(packageName, globalState);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPackageRemoved(String packageName, int userId) {
        synchronized (this.mLock) {
            if (this.mUserStates.contains(userId)) {
                this.mUserStates.get(userId).remove(packageName);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPackageRemovedForAllUsers(String packageName) {
        synchronized (this.mLock) {
            this.mGlobalHibernationStates.remove(packageName);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onDeviceConfigChanged(DeviceConfig.Properties properties) {
        for (String key : properties.getKeyset()) {
            if (TextUtils.equals("app_hibernation_enabled", key)) {
                sIsServiceEnabled = isDeviceConfigAppHibernationEnabled();
                Slog.d(TAG, "App hibernation changed to enabled=" + sIsServiceEnabled);
                return;
            }
        }
    }

    private int handleIncomingUser(int userId, String name) {
        int callingUid = Binder.getCallingUid();
        try {
            return this.mIActivityManager.handleIncomingUser(Binder.getCallingPid(), callingUid, userId, false, true, name, (String) null);
        } catch (RemoteException re) {
            throw re.rethrowFromSystemServer();
        }
    }

    private boolean checkUserStatesExist(int userId, String methodName, boolean shouldLog) {
        if (!this.mUserManager.isUserUnlockingOrUnlocked(userId)) {
            if (shouldLog) {
                Slog.w(TAG, TextUtils.formatSimple("Attempt to call %s on stopped or nonexistent user %d", new Object[]{methodName, Integer.valueOf(userId)}));
            }
            return false;
        } else if (this.mUserStates.contains(userId)) {
            return true;
        } else {
            if (shouldLog) {
                Slog.w(TAG, TextUtils.formatSimple("Attempt to call %s before states have been read from disk", new Object[]{methodName}));
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dump(PrintWriter pw) {
        if (DumpUtils.checkDumpAndUsageStatsPermission(getContext(), TAG, pw)) {
            IndentingPrintWriter idpw = new IndentingPrintWriter(pw, "  ");
            synchronized (this.mLock) {
                int userCount = this.mUserStates.size();
                for (int i = 0; i < userCount; i++) {
                    int userId = this.mUserStates.keyAt(i);
                    idpw.print("User Level Hibernation States, ");
                    idpw.printPair("user", Integer.valueOf(userId));
                    idpw.println();
                    Map<String, UserLevelState> stateMap = this.mUserStates.get(userId);
                    idpw.increaseIndent();
                    for (UserLevelState state : stateMap.values()) {
                        idpw.print(state);
                        idpw.println();
                    }
                    idpw.decreaseIndent();
                }
                idpw.println();
                idpw.print("Global Level Hibernation States");
                idpw.println();
                for (GlobalLevelState state2 : this.mGlobalHibernationStates.values()) {
                    idpw.print(state2);
                    idpw.println();
                }
            }
        }
    }

    /* loaded from: classes.dex */
    private static final class LocalService extends AppHibernationManagerInternal {
        private final AppHibernationService mService;

        LocalService(AppHibernationService service) {
            this.mService = service;
        }

        @Override // com.android.server.apphibernation.AppHibernationManagerInternal
        public boolean isHibernatingForUser(String packageName, int userId) {
            return this.mService.isHibernatingForUser(packageName, userId);
        }

        @Override // com.android.server.apphibernation.AppHibernationManagerInternal
        public void setHibernatingForUser(String packageName, int userId, boolean isHibernating) {
            this.mService.setHibernatingForUser(packageName, userId, isHibernating);
        }

        @Override // com.android.server.apphibernation.AppHibernationManagerInternal
        public void setHibernatingGlobally(String packageName, boolean isHibernating) {
            this.mService.setHibernatingGlobally(packageName, isHibernating);
        }

        @Override // com.android.server.apphibernation.AppHibernationManagerInternal
        public boolean isHibernatingGlobally(String packageName) {
            return this.mService.isHibernatingGlobally(packageName);
        }

        @Override // com.android.server.apphibernation.AppHibernationManagerInternal
        public boolean isOatArtifactDeletionEnabled() {
            return this.mService.isOatArtifactDeletionEnabled();
        }
    }

    /* loaded from: classes.dex */
    static final class AppHibernationServiceStub extends IAppHibernationService.Stub {
        final AppHibernationService mService;

        AppHibernationServiceStub(AppHibernationService service) {
            this.mService = service;
        }

        public boolean isHibernatingForUser(String packageName, int userId) {
            return this.mService.isHibernatingForUser(packageName, userId);
        }

        public void setHibernatingForUser(String packageName, int userId, boolean isHibernating) {
            this.mService.setHibernatingForUser(packageName, userId, isHibernating);
        }

        public void setHibernatingGlobally(String packageName, boolean isHibernating) {
            this.mService.setHibernatingGlobally(packageName, isHibernating);
        }

        public boolean isHibernatingGlobally(String packageName) {
            return this.mService.isHibernatingGlobally(packageName);
        }

        public List<String> getHibernatingPackagesForUser(int userId) {
            return this.mService.getHibernatingPackagesForUser(userId);
        }

        public Map<String, HibernationStats> getHibernationStatsForUser(List<String> packageNames, int userId) {
            Set<String> pkgsSet = packageNames != null ? new ArraySet<>(packageNames) : null;
            return this.mService.getHibernationStatsForUser(pkgsSet, userId);
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.apphibernation.AppHibernationService$AppHibernationServiceStub */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new AppHibernationShellCommand(this.mService).exec(this, in, out, err, args, callback, resultReceiver);
        }

        protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
            this.mService.dump(fout);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$6$com-android-server-apphibernation-AppHibernationService  reason: not valid java name */
    public /* synthetic */ void m1601x6d4ab619(int userId, UsageEvents.Event event) {
        if (!isAppHibernationEnabled()) {
            return;
        }
        int eventType = event.mEventType;
        if (eventType == 7 || eventType == 1 || eventType == 31) {
            String pkgName = event.mPackage;
            setHibernatingForUser(pkgName, userId, false);
            setHibernatingGlobally(pkgName, false);
        }
    }

    public static boolean isAppHibernationEnabled() {
        return sIsServiceEnabled;
    }

    private static boolean isDeviceConfigAppHibernationEnabled() {
        return DeviceConfig.getBoolean("app_hibernation", "app_hibernation_enabled", true);
    }

    /* loaded from: classes.dex */
    private static final class InjectorImpl implements Injector {
        private static final String HIBERNATION_DIR_NAME = "hibernation";
        private final Context mContext;
        private final ScheduledExecutorService mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        private final UserLevelHibernationProto mUserLevelHibernationProto = new UserLevelHibernationProto();

        InjectorImpl(Context context) {
            this.mContext = context;
        }

        @Override // com.android.server.apphibernation.AppHibernationService.Injector
        public Context getContext() {
            return this.mContext;
        }

        @Override // com.android.server.apphibernation.AppHibernationService.Injector
        public IPackageManager getPackageManager() {
            return IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        }

        @Override // com.android.server.apphibernation.AppHibernationService.Injector
        public PackageManagerInternal getPackageManagerInternal() {
            return (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        }

        @Override // com.android.server.apphibernation.AppHibernationService.Injector
        public IActivityManager getActivityManager() {
            return ActivityManager.getService();
        }

        @Override // com.android.server.apphibernation.AppHibernationService.Injector
        public UserManager getUserManager() {
            return (UserManager) this.mContext.getSystemService(UserManager.class);
        }

        @Override // com.android.server.apphibernation.AppHibernationService.Injector
        public StorageStatsManager getStorageStatsManager() {
            return (StorageStatsManager) this.mContext.getSystemService(StorageStatsManager.class);
        }

        @Override // com.android.server.apphibernation.AppHibernationService.Injector
        public Executor getBackgroundExecutor() {
            return this.mScheduledExecutorService;
        }

        @Override // com.android.server.apphibernation.AppHibernationService.Injector
        public UsageStatsManagerInternal getUsageStatsManagerInternal() {
            return (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        }

        @Override // com.android.server.apphibernation.AppHibernationService.Injector
        public HibernationStateDiskStore<GlobalLevelState> getGlobalLevelDiskStore() {
            File dir = new File(Environment.getDataSystemDirectory(), HIBERNATION_DIR_NAME);
            return new HibernationStateDiskStore<>(dir, new GlobalLevelHibernationProto(), this.mScheduledExecutorService);
        }

        @Override // com.android.server.apphibernation.AppHibernationService.Injector
        public HibernationStateDiskStore<UserLevelState> getUserLevelDiskStore(int userId) {
            File dir = new File(Environment.getDataSystemCeDirectory(userId), HIBERNATION_DIR_NAME);
            return new HibernationStateDiskStore<>(dir, this.mUserLevelHibernationProto, this.mScheduledExecutorService);
        }

        @Override // com.android.server.apphibernation.AppHibernationService.Injector
        public boolean isOatArtifactDeletionEnabled() {
            return this.mContext.getResources().getBoolean(17891679);
        }
    }

    /* loaded from: classes.dex */
    private final class StatsPullAtomCallbackImpl implements StatsManager.StatsPullAtomCallback {
        private static final int MEGABYTE_IN_BYTES = 1000000;

        private StatsPullAtomCallbackImpl() {
        }

        public int onPullAtom(int atomTag, List<StatsEvent> data) {
            if (AppHibernationService.isAppHibernationEnabled() || !(atomTag == 10107 || atomTag == 10109)) {
                switch (atomTag) {
                    case FrameworkStatsLog.USER_LEVEL_HIBERNATED_APPS /* 10107 */:
                        List<UserInfo> userInfos = AppHibernationService.this.mUserManager.getAliveUsers();
                        int numUsers = userInfos.size();
                        for (int i = 0; i < numUsers; i++) {
                            int userId = userInfos.get(i).id;
                            if (AppHibernationService.this.mUserManager.isUserUnlockingOrUnlocked(userId)) {
                                data.add(FrameworkStatsLog.buildStatsEvent(atomTag, AppHibernationService.this.getHibernatingPackagesForUser(userId).size(), userId));
                            }
                        }
                        break;
                    case 10108:
                    default:
                        return 1;
                    case FrameworkStatsLog.GLOBAL_HIBERNATED_APPS /* 10109 */:
                        int hibernatedAppCount = 0;
                        long storage_saved_byte = 0;
                        synchronized (AppHibernationService.this.mLock) {
                            for (GlobalLevelState state : AppHibernationService.this.mGlobalHibernationStates.values()) {
                                if (state.hibernated) {
                                    hibernatedAppCount++;
                                    storage_saved_byte += state.savedByte;
                                }
                            }
                        }
                        data.add(FrameworkStatsLog.buildStatsEvent(atomTag, hibernatedAppCount, storage_saved_byte / 1000000));
                        break;
                }
                return 0;
            }
            return 0;
        }
    }
}
