package com.android.server.tare;

import android.app.AlarmManager;
import android.app.tare.IEconomyManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.database.ContentObserver;
import android.hardware.audio.common.V2_0.AudioDevice;
import android.net.Uri;
import android.os.BatteryManagerInternal;
import android.os.Binder;
import android.os.Handler;
import android.os.IDeviceIdleController;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArrayMap;
import android.util.SparseSetArray;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.jobs.ArrayUtils;
import com.android.internal.util.jobs.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerInternal;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.tare.Agent;
import com.android.server.tare.EconomicPolicy;
import com.android.server.tare.EconomyManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
/* loaded from: classes2.dex */
public class InternalResourceService extends SystemService {
    private static final String ALARM_TAG_WEALTH_RECLAMATION = "*tare.reclamation*";
    public static final boolean DEBUG = Log.isLoggable("TARE", 3);
    private static final float DEFAULT_UNUSED_RECLAMATION_PERCENTAGE = 0.1f;
    private static final long MIN_UNUSED_TIME_MS = 259200000;
    private static final int MSG_NOTIFY_AFFORDABILITY_CHANGE_LISTENER = 0;
    private static final int MSG_NOTIFY_STATE_CHANGE_LISTENERS = 3;
    private static final int MSG_PROCESS_USAGE_EVENT = 2;
    private static final int MSG_SCHEDULE_UNUSED_WEALTH_RECLAMATION_EVENT = 1;
    private static final int PACKAGE_QUERY_FLAGS = 1074528256;
    private static final int QUANTITATIVE_EASING_BATTERY_THRESHOLD = 50;
    private static final long RECLAMATION_STARTUP_DELAY_MS = 30000;
    public static final String TAG = "TARE-IRS";
    static final long UNUSED_RECLAMATION_PERIOD_MS = 86400000;
    private final Agent mAgent;
    private final Analyst mAnalyst;
    private final BatteryManagerInternal mBatteryManagerInternal;
    private volatile int mBootPhase;
    private final BroadcastReceiver mBroadcastReceiver;
    private CompleteEconomicPolicy mCompleteEconomicPolicy;
    private final ConfigObserver mConfigObserver;
    private int mCurrentBatteryLevel;
    private IDeviceIdleController mDeviceIdleController;
    private final EconomyManagerStub mEconomyManagerStub;
    private volatile boolean mExemptListLoaded;
    private ArraySet<String> mExemptedApps;
    private final Handler mHandler;
    private volatile boolean mIsEnabled;
    private final Object mLock;
    private final PackageManager mPackageManager;
    private final PackageManagerInternal mPackageManagerInternal;
    private final SparseArrayMap<String, Integer> mPackageToUidCache;
    private final List<PackageInfo> mPkgCache;
    private final Scribe mScribe;
    private final CopyOnWriteArraySet<EconomyManagerInternal.TareStateChangeListener> mStateChangeListeners;
    private final UsageStatsManagerInternal.UsageEventListener mSurveillanceAgent;
    private final SparseSetArray<String> mUidToPackageCache;
    private final AlarmManager.OnAlarmListener mUnusedWealthReclamationListener;

    public InternalResourceService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mPkgCache = new ArrayList();
        this.mUidToPackageCache = new SparseSetArray<>();
        this.mPackageToUidCache = new SparseArrayMap<>();
        this.mStateChangeListeners = new CopyOnWriteArraySet<>();
        this.mExemptedApps = new ArraySet<>();
        this.mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.tare.InternalResourceService.1
            private String getPackageName(Intent intent) {
                Uri uri = intent.getData();
                if (uri != null) {
                    return uri.getSchemeSpecificPart();
                }
                return null;
            }

            /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                char c;
                String action = intent.getAction();
                switch (action.hashCode()) {
                    case -2061058799:
                        if (action.equals("android.intent.action.USER_REMOVED")) {
                            c = 5;
                            break;
                        }
                        c = 65535;
                        break;
                    case -757780528:
                        if (action.equals("android.intent.action.PACKAGE_RESTARTED")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    case -625323454:
                        if (action.equals("android.intent.action.BATTERY_LEVEL_CHANGED")) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case -65633567:
                        if (action.equals("android.os.action.POWER_SAVE_WHITELIST_CHANGED")) {
                            c = 6;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1121780209:
                        if (action.equals("android.intent.action.USER_ADDED")) {
                            c = 4;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1544582882:
                        if (action.equals("android.intent.action.PACKAGE_ADDED")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1580442797:
                        if (action.equals("android.intent.action.PACKAGE_FULLY_REMOVED")) {
                            c = 1;
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
                        InternalResourceService.this.onBatteryLevelChanged();
                        return;
                    case 1:
                        String pkgName = getPackageName(intent);
                        int pkgUid = intent.getIntExtra("android.intent.extra.UID", -1);
                        InternalResourceService.this.onPackageRemoved(pkgUid, pkgName);
                        return;
                    case 2:
                        if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                            String pkgName2 = getPackageName(intent);
                            int pkgUid2 = intent.getIntExtra("android.intent.extra.UID", -1);
                            InternalResourceService.this.onPackageAdded(pkgUid2, pkgName2);
                            return;
                        }
                        return;
                    case 3:
                        String pkgName3 = getPackageName(intent);
                        int pkgUid3 = intent.getIntExtra("android.intent.extra.UID", -1);
                        int userId = UserHandle.getUserId(pkgUid3);
                        InternalResourceService.this.onPackageForceStopped(userId, pkgName3);
                        return;
                    case 4:
                        int userId2 = intent.getIntExtra("android.intent.extra.user_handle", 0);
                        InternalResourceService.this.onUserAdded(userId2);
                        return;
                    case 5:
                        int userId3 = intent.getIntExtra("android.intent.extra.user_handle", 0);
                        InternalResourceService.this.onUserRemoved(userId3);
                        return;
                    case 6:
                        InternalResourceService.this.onExemptionListChanged();
                        return;
                    default:
                        return;
                }
            }
        };
        this.mSurveillanceAgent = new UsageStatsManagerInternal.UsageEventListener() { // from class: com.android.server.tare.InternalResourceService.2
            @Override // android.app.usage.UsageStatsManagerInternal.UsageEventListener
            public void onUsageEvent(int userId, UsageEvents.Event event) {
                InternalResourceService.this.mHandler.obtainMessage(2, userId, 0, event).sendToTarget();
            }
        };
        this.mUnusedWealthReclamationListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.tare.InternalResourceService.3
            @Override // android.app.AlarmManager.OnAlarmListener
            public void onAlarm() {
                synchronized (InternalResourceService.this.mLock) {
                    InternalResourceService.this.mAgent.reclaimUnusedAssetsLocked(0.10000000149011612d, InternalResourceService.MIN_UNUSED_TIME_MS, false);
                    InternalResourceService.this.mScribe.setLastReclamationTimeLocked(TareUtils.getCurrentTimeMillis());
                    InternalResourceService.this.scheduleUnusedWealthReclamationLocked();
                }
            }
        };
        IrsHandler irsHandler = new IrsHandler(TareHandlerThread.get().getLooper());
        this.mHandler = irsHandler;
        this.mBatteryManagerInternal = (BatteryManagerInternal) LocalServices.getService(BatteryManagerInternal.class);
        this.mPackageManager = context.getPackageManager();
        this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mEconomyManagerStub = new EconomyManagerStub();
        Analyst analyst = new Analyst();
        this.mAnalyst = analyst;
        Scribe scribe = new Scribe(this, analyst);
        this.mScribe = scribe;
        this.mCompleteEconomicPolicy = new CompleteEconomicPolicy(this);
        this.mAgent = new Agent(this, scribe, analyst);
        this.mConfigObserver = new ConfigObserver(irsHandler, context);
        publishLocalService(EconomyManagerInternal.class, new LocalService());
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("tare", this.mEconomyManagerStub);
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        this.mBootPhase = phase;
        if (500 == phase) {
            this.mConfigObserver.start();
            this.mDeviceIdleController = IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));
            setupEverything();
        } else if (1000 == phase && !this.mExemptListLoaded) {
            synchronized (this.mLock) {
                try {
                    this.mExemptedApps = new ArraySet<>(this.mDeviceIdleController.getFullPowerWhitelist());
                } catch (RemoteException e) {
                    Slog.wtf(TAG, e);
                }
                this.mExemptListLoaded = true;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Object getLock() {
        return this.mLock;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public CompleteEconomicPolicy getCompleteEconomicPolicyLocked() {
        return this.mCompleteEconomicPolicy;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<PackageInfo> getInstalledPackages() {
        List<PackageInfo> list;
        synchronized (this.mLock) {
            list = this.mPkgCache;
        }
        return list;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<PackageInfo> getInstalledPackages(int userId) {
        List<PackageInfo> userPkgs = new ArrayList<>();
        synchronized (this.mLock) {
            for (int i = 0; i < this.mPkgCache.size(); i++) {
                PackageInfo packageInfo = this.mPkgCache.get(i);
                if (packageInfo.applicationInfo != null && UserHandle.getUserId(packageInfo.applicationInfo.uid) == userId) {
                    userPkgs.add(packageInfo);
                }
            }
        }
        return userPkgs;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getConsumptionLimitLocked() {
        return (this.mCurrentBatteryLevel * this.mScribe.getSatiatedConsumptionLimitLocked()) / 100;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getMinBalanceLocked(int userId, String pkgName) {
        return (this.mCurrentBatteryLevel * this.mCompleteEconomicPolicy.getMinSatiatedBalance(userId, pkgName)) / 100;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getInitialSatiatedConsumptionLimitLocked() {
        return this.mCompleteEconomicPolicy.getInitialSatiatedConsumptionLimit();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getUid(int userId, String pkgName) {
        int intValue;
        synchronized (this.mPackageToUidCache) {
            Integer uid = (Integer) this.mPackageToUidCache.get(userId, pkgName);
            if (uid == null) {
                uid = Integer.valueOf(this.mPackageManagerInternal.getPackageUid(pkgName, 0L, userId));
                this.mPackageToUidCache.add(userId, pkgName, uid);
            }
            intValue = uid.intValue();
        }
        return intValue;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isEnabled() {
        return this.mIsEnabled;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPackageExempted(int userId, String pkgName) {
        boolean contains;
        synchronized (this.mLock) {
            contains = this.mExemptedApps.contains(pkgName);
        }
        return contains;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSystem(int userId, String pkgName) {
        if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(pkgName)) {
            return true;
        }
        return UserHandle.isCore(getUid(userId, pkgName));
    }

    void onBatteryLevelChanged() {
        synchronized (this.mLock) {
            int newBatteryLevel = getCurrentBatteryLevel();
            this.mAnalyst.noteBatteryLevelChange(newBatteryLevel);
            int i = this.mCurrentBatteryLevel;
            boolean increased = newBatteryLevel > i;
            if (increased) {
                this.mAgent.distributeBasicIncomeLocked(newBatteryLevel);
            } else if (newBatteryLevel == i) {
                return;
            }
            this.mCurrentBatteryLevel = newBatteryLevel;
            adjustCreditSupplyLocked(increased);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDeviceStateChanged() {
        synchronized (this.mLock) {
            this.mAgent.onDeviceStateChangedLocked();
        }
    }

    void onExemptionListChanged() {
        int[] userIds = ((UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).getUserIds();
        synchronized (this.mLock) {
            ArraySet<String> removed = this.mExemptedApps;
            ArraySet<String> added = new ArraySet<>();
            try {
                ArraySet<String> arraySet = new ArraySet<>(this.mDeviceIdleController.getFullPowerWhitelist());
                this.mExemptedApps = arraySet;
                for (int i = arraySet.size() - 1; i >= 0; i--) {
                    String pkg = this.mExemptedApps.valueAt(i);
                    if (!removed.contains(pkg)) {
                        added.add(pkg);
                    }
                    removed.remove(pkg);
                }
                int i2 = added.size();
                for (int a = i2 - 1; a >= 0; a--) {
                    String pkgName = added.valueAt(a);
                    for (int userId : userIds) {
                        boolean appExists = getUid(userId, pkgName) >= 0;
                        if (appExists) {
                            this.mAgent.onAppExemptedLocked(userId, pkgName);
                        }
                    }
                }
                int a2 = removed.size();
                for (int r = a2 - 1; r >= 0; r--) {
                    String pkgName2 = removed.valueAt(r);
                    for (int userId2 : userIds) {
                        boolean appExists2 = getUid(userId2, pkgName2) >= 0;
                        if (appExists2) {
                            this.mAgent.onAppUnexemptedLocked(userId2, pkgName2);
                        }
                    }
                }
            } catch (RemoteException e) {
                Slog.wtf(TAG, e);
            }
        }
    }

    void onPackageAdded(int uid, String pkgName) {
        int userId = UserHandle.getUserId(uid);
        try {
            PackageInfo packageInfo = this.mPackageManager.getPackageInfoAsUser(pkgName, PACKAGE_QUERY_FLAGS, userId);
            synchronized (this.mPackageToUidCache) {
                this.mPackageToUidCache.add(userId, pkgName, Integer.valueOf(uid));
            }
            synchronized (this.mLock) {
                this.mPkgCache.add(packageInfo);
                this.mUidToPackageCache.add(uid, pkgName);
                this.mAgent.grantBirthrightLocked(userId, pkgName);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Slog.wtf(TAG, "PM couldn't find newly added package: " + pkgName, e);
        }
    }

    void onPackageForceStopped(int userId, String pkgName) {
        synchronized (this.mLock) {
        }
    }

    void onPackageRemoved(int uid, String pkgName) {
        int userId = UserHandle.getUserId(uid);
        synchronized (this.mPackageToUidCache) {
            this.mPackageToUidCache.delete(userId, pkgName);
        }
        synchronized (this.mLock) {
            this.mUidToPackageCache.remove(uid, pkgName);
            int i = 0;
            while (true) {
                if (i >= this.mPkgCache.size()) {
                    break;
                }
                PackageInfo pkgInfo = this.mPkgCache.get(i);
                if (UserHandle.getUserId(pkgInfo.applicationInfo.uid) != userId || !pkgName.equals(pkgInfo.packageName)) {
                    i++;
                } else {
                    this.mPkgCache.remove(i);
                    break;
                }
            }
            this.mAgent.onPackageRemovedLocked(userId, pkgName);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUidStateChanged(int uid) {
        synchronized (this.mLock) {
            ArraySet<String> pkgNames = getPackagesForUidLocked(uid);
            if (pkgNames == null) {
                Slog.e(TAG, "Don't have packages for uid " + uid);
            } else {
                this.mAgent.onAppStatesChangedLocked(UserHandle.getUserId(uid), pkgNames);
            }
        }
    }

    void onUserAdded(int userId) {
        synchronized (this.mLock) {
            this.mPkgCache.addAll(this.mPackageManager.getInstalledPackagesAsUser(PACKAGE_QUERY_FLAGS, userId));
            this.mAgent.grantBirthrightsLocked(userId);
        }
    }

    void onUserRemoved(int userId) {
        synchronized (this.mLock) {
            ArrayList<String> removedPkgs = new ArrayList<>();
            int i = this.mPkgCache.size() - 1;
            while (true) {
                if (i < 0) {
                    break;
                }
                PackageInfo pkgInfo = this.mPkgCache.get(i);
                if (UserHandle.getUserId(pkgInfo.applicationInfo.uid) != userId) {
                    i--;
                } else {
                    removedPkgs.add(pkgInfo.packageName);
                    this.mUidToPackageCache.remove(pkgInfo.applicationInfo.uid);
                    this.mPkgCache.remove(i);
                    break;
                }
            }
            this.mAgent.onUserRemovedLocked(userId, removedPkgs);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void maybePerformQuantitativeEasingLocked() {
        long remainingConsumableCakes = this.mScribe.getRemainingConsumableCakesLocked();
        if (this.mCurrentBatteryLevel <= 50 || remainingConsumableCakes > 0) {
            return;
        }
        long currentConsumptionLimit = this.mScribe.getSatiatedConsumptionLimitLocked();
        long shortfall = ((this.mCurrentBatteryLevel - 50) * currentConsumptionLimit) / 100;
        long newConsumptionLimit = Math.min(currentConsumptionLimit + shortfall, this.mCompleteEconomicPolicy.getHardSatiatedConsumptionLimit());
        if (newConsumptionLimit != currentConsumptionLimit) {
            Slog.i(TAG, "Increasing consumption limit from " + TareUtils.cakeToString(currentConsumptionLimit) + " to " + TareUtils.cakeToString(newConsumptionLimit));
            this.mScribe.setConsumptionLimitLocked(newConsumptionLimit);
            adjustCreditSupplyLocked(true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postAffordabilityChanged(int userId, String pkgName, Agent.ActionAffordabilityNote affordabilityNote) {
        if (DEBUG) {
            Slog.d(TAG, userId + ":" + pkgName + " affordability changed to " + affordabilityNote.isCurrentlyAffordable());
        }
        SomeArgs args = SomeArgs.obtain();
        args.argi1 = userId;
        args.arg1 = pkgName;
        args.arg2 = affordabilityNote;
        this.mHandler.obtainMessage(0, args).sendToTarget();
    }

    private void adjustCreditSupplyLocked(boolean allowIncrease) {
        long newLimit = getConsumptionLimitLocked();
        long remainingConsumableCakes = this.mScribe.getRemainingConsumableCakesLocked();
        if (remainingConsumableCakes == newLimit) {
            return;
        }
        if (remainingConsumableCakes > newLimit) {
            this.mScribe.adjustRemainingConsumableCakesLocked(newLimit - remainingConsumableCakes);
        } else if (allowIncrease) {
            double perc = this.mCurrentBatteryLevel / 100.0d;
            long shortfall = newLimit - remainingConsumableCakes;
            this.mScribe.adjustRemainingConsumableCakesLocked((long) (shortfall * perc));
        }
        this.mAgent.onCreditSupplyChanged();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processUsageEventLocked(int userId, UsageEvents.Event event) {
        if (!this.mIsEnabled) {
            return;
        }
        String pkgName = event.getPackageName();
        if (DEBUG) {
            Slog.d(TAG, "Processing event " + event.getEventType() + " (" + event.mInstanceId + ") for " + TareUtils.appToString(userId, pkgName));
        }
        long nowElapsed = SystemClock.elapsedRealtime();
        switch (event.getEventType()) {
            case 1:
                this.mAgent.noteOngoingEventLocked(userId, pkgName, AudioDevice.IN_AMBIENT, String.valueOf(event.mInstanceId), nowElapsed);
                return;
            case 2:
            case 23:
            case 24:
                long now = TareUtils.getCurrentTimeMillis();
                this.mAgent.stopOngoingActionLocked(userId, pkgName, AudioDevice.IN_AMBIENT, String.valueOf(event.mInstanceId), nowElapsed, now);
                return;
            case 7:
            case 9:
                this.mAgent.noteInstantaneousEventLocked(userId, pkgName, AudioDevice.IN_BUILTIN_MIC, null);
                return;
            case 10:
            case 12:
                this.mAgent.noteInstantaneousEventLocked(userId, pkgName, Integer.MIN_VALUE, null);
                return;
            default:
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleUnusedWealthReclamationLocked() {
        final long now = TareUtils.getCurrentTimeMillis();
        final long nextReclamationTime = Math.max(30000 + now, this.mScribe.getLastReclamationTimeLocked() + 86400000);
        this.mHandler.post(new Runnable() { // from class: com.android.server.tare.InternalResourceService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                InternalResourceService.this.m6766x699ef945(nextReclamationTime, now);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleUnusedWealthReclamationLocked$0$com-android-server-tare-InternalResourceService  reason: not valid java name */
    public /* synthetic */ void m6766x699ef945(long nextReclamationTime, long now) {
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(AlarmManager.class);
        if (alarmManager != null) {
            alarmManager.setWindow(3, SystemClock.elapsedRealtime() + (nextReclamationTime - now), 1800000L, ALARM_TAG_WEALTH_RECLAMATION, this.mUnusedWealthReclamationListener, this.mHandler);
        } else {
            this.mHandler.sendEmptyMessageDelayed(1, 30000L);
        }
    }

    private int getCurrentBatteryLevel() {
        return this.mBatteryManagerInternal.getBatteryLevel();
    }

    private ArraySet<String> getPackagesForUidLocked(int uid) {
        String[] pkgs;
        ArraySet<String> packages = this.mUidToPackageCache.get(uid);
        if (packages == null && (pkgs = this.mPackageManager.getPackagesForUid(uid)) != null) {
            for (String pkg : pkgs) {
                this.mUidToPackageCache.add(uid, pkg);
            }
            return this.mUidToPackageCache.get(uid);
        }
        return packages;
    }

    private void loadInstalledPackageListLocked() {
        this.mPkgCache.clear();
        UserManagerInternal userManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        int[] userIds = userManagerInternal.getUserIds();
        for (int userId : userIds) {
            this.mPkgCache.addAll(this.mPackageManager.getInstalledPackagesAsUser(PACKAGE_QUERY_FLAGS, userId));
        }
    }

    private void registerListeners() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BATTERY_LEVEL_CHANGED");
        filter.addAction("android.os.action.POWER_SAVE_WHITELIST_CHANGED");
        getContext().registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter, null, null);
        IntentFilter pkgFilter = new IntentFilter();
        pkgFilter.addAction("android.intent.action.PACKAGE_FULLY_REMOVED");
        pkgFilter.addAction("android.intent.action.PACKAGE_ADDED");
        pkgFilter.addAction("android.intent.action.PACKAGE_RESTARTED");
        pkgFilter.addDataScheme("package");
        getContext().registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, pkgFilter, null, null);
        IntentFilter userFilter = new IntentFilter("android.intent.action.USER_REMOVED");
        userFilter.addAction("android.intent.action.USER_ADDED");
        getContext().registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, userFilter, null, null);
        UsageStatsManagerInternal usmi = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        usmi.registerListener(this.mSurveillanceAgent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setupHeavyWork() {
        synchronized (this.mLock) {
            loadInstalledPackageListLocked();
            if (this.mBootPhase >= 1000 && !this.mExemptListLoaded) {
                try {
                    this.mExemptedApps = new ArraySet<>(this.mDeviceIdleController.getFullPowerWhitelist());
                } catch (RemoteException e) {
                    Slog.wtf(TAG, e);
                }
                this.mExemptListLoaded = true;
            }
            boolean isFirstSetup = !this.mScribe.recordExists();
            if (isFirstSetup) {
                this.mAgent.grantBirthrightsLocked();
                this.mScribe.setConsumptionLimitLocked(this.mCompleteEconomicPolicy.getInitialSatiatedConsumptionLimit());
            } else {
                this.mScribe.loadFromDiskLocked();
                if (this.mScribe.getSatiatedConsumptionLimitLocked() < this.mCompleteEconomicPolicy.getInitialSatiatedConsumptionLimit() || this.mScribe.getSatiatedConsumptionLimitLocked() > this.mCompleteEconomicPolicy.getHardSatiatedConsumptionLimit()) {
                    this.mScribe.setConsumptionLimitLocked(this.mCompleteEconomicPolicy.getInitialSatiatedConsumptionLimit());
                }
            }
            scheduleUnusedWealthReclamationLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setupEverything() {
        if (this.mBootPhase < 500 || !this.mIsEnabled) {
            return;
        }
        synchronized (this.mLock) {
            registerListeners();
            this.mCurrentBatteryLevel = getCurrentBatteryLevel();
            this.mHandler.post(new Runnable() { // from class: com.android.server.tare.InternalResourceService$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    InternalResourceService.this.setupHeavyWork();
                }
            });
            this.mCompleteEconomicPolicy.setup(this.mConfigObserver.getAllDeviceConfigProperties());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void tearDownEverything() {
        if (this.mIsEnabled) {
            return;
        }
        synchronized (this.mLock) {
            this.mAgent.tearDownLocked();
            this.mAnalyst.tearDown();
            this.mCompleteEconomicPolicy.tearDown();
            this.mExemptedApps.clear();
            this.mExemptListLoaded = false;
            this.mHandler.post(new Runnable() { // from class: com.android.server.tare.InternalResourceService$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    InternalResourceService.this.m6767xde54c53a();
                }
            });
            this.mPkgCache.clear();
            this.mScribe.tearDownLocked();
            this.mUidToPackageCache.clear();
            getContext().unregisterReceiver(this.mBroadcastReceiver);
            UsageStatsManagerInternal usmi = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
            usmi.unregisterListener(this.mSurveillanceAgent);
        }
        synchronized (this.mPackageToUidCache) {
            this.mPackageToUidCache.clear();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$tearDownEverything$1$com-android-server-tare-InternalResourceService  reason: not valid java name */
    public /* synthetic */ void m6767xde54c53a() {
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(AlarmManager.class);
        if (alarmManager != null) {
            alarmManager.cancel(this.mUnusedWealthReclamationListener);
        }
    }

    /* loaded from: classes2.dex */
    private final class IrsHandler extends Handler {
        IrsHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    SomeArgs args = (SomeArgs) msg.obj;
                    int userId = args.argi1;
                    String pkgName = (String) args.arg1;
                    Agent.ActionAffordabilityNote affordabilityNote = (Agent.ActionAffordabilityNote) args.arg2;
                    EconomyManagerInternal.AffordabilityChangeListener listener = affordabilityNote.getListener();
                    listener.onAffordabilityChanged(userId, pkgName, affordabilityNote.getActionBill(), affordabilityNote.isCurrentlyAffordable());
                    args.recycle();
                    return;
                case 1:
                    removeMessages(1);
                    synchronized (InternalResourceService.this.mLock) {
                        InternalResourceService.this.scheduleUnusedWealthReclamationLocked();
                    }
                    return;
                case 2:
                    int userId2 = msg.arg1;
                    UsageEvents.Event event = (UsageEvents.Event) msg.obj;
                    synchronized (InternalResourceService.this.mLock) {
                        InternalResourceService.this.processUsageEventLocked(userId2, event);
                    }
                    return;
                case 3:
                    Iterator it = InternalResourceService.this.mStateChangeListeners.iterator();
                    while (it.hasNext()) {
                        EconomyManagerInternal.TareStateChangeListener listener2 = (EconomyManagerInternal.TareStateChangeListener) it.next();
                        listener2.onTareEnabledStateChanged(InternalResourceService.this.mIsEnabled);
                    }
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes2.dex */
    final class EconomyManagerStub extends IEconomyManager.Stub {
        EconomyManagerStub() {
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpAndUsageStatsPermission(InternalResourceService.this.getContext(), InternalResourceService.TAG, pw)) {
                boolean dumpAll = true;
                if (!ArrayUtils.isEmpty(args)) {
                    String arg = args[0];
                    if ("-h".equals(arg) || "--help".equals(arg)) {
                        InternalResourceService.dumpHelp(pw);
                        return;
                    } else if ("-a".equals(arg)) {
                        dumpAll = false;
                    } else if (arg.length() > 0 && arg.charAt(0) == '-') {
                        pw.println("Unknown option: " + arg);
                        return;
                    }
                }
                long identityToken = Binder.clearCallingIdentity();
                try {
                    InternalResourceService.this.dumpInternal(new IndentingPrintWriter(pw, "  "), dumpAll);
                } finally {
                    Binder.restoreCallingIdentity(identityToken);
                }
            }
        }
    }

    /* loaded from: classes2.dex */
    private final class LocalService implements EconomyManagerInternal {
        private static final long FOREVER_MS = 851472000000L;

        private LocalService() {
        }

        @Override // com.android.server.tare.EconomyManagerInternal
        public void registerAffordabilityChangeListener(int userId, String pkgName, EconomyManagerInternal.AffordabilityChangeListener listener, EconomyManagerInternal.ActionBill bill) {
            if (InternalResourceService.this.isSystem(userId, pkgName)) {
                return;
            }
            synchronized (InternalResourceService.this.mLock) {
                InternalResourceService.this.mAgent.registerAffordabilityChangeListenerLocked(userId, pkgName, listener, bill);
            }
        }

        @Override // com.android.server.tare.EconomyManagerInternal
        public void unregisterAffordabilityChangeListener(int userId, String pkgName, EconomyManagerInternal.AffordabilityChangeListener listener, EconomyManagerInternal.ActionBill bill) {
            if (InternalResourceService.this.isSystem(userId, pkgName)) {
                return;
            }
            synchronized (InternalResourceService.this.mLock) {
                InternalResourceService.this.mAgent.unregisterAffordabilityChangeListenerLocked(userId, pkgName, listener, bill);
            }
        }

        @Override // com.android.server.tare.EconomyManagerInternal
        public void registerTareStateChangeListener(EconomyManagerInternal.TareStateChangeListener listener) {
            InternalResourceService.this.mStateChangeListeners.add(listener);
        }

        @Override // com.android.server.tare.EconomyManagerInternal
        public void unregisterTareStateChangeListener(EconomyManagerInternal.TareStateChangeListener listener) {
            InternalResourceService.this.mStateChangeListeners.remove(listener);
        }

        @Override // com.android.server.tare.EconomyManagerInternal
        public boolean canPayFor(int userId, String pkgName, EconomyManagerInternal.ActionBill bill) {
            EconomyManagerInternal.AnticipatedAction action;
            EconomicPolicy.Cost cost;
            if (InternalResourceService.this.mIsEnabled && !InternalResourceService.this.isSystem(userId, pkgName)) {
                long requiredBalance = 0;
                List<EconomyManagerInternal.AnticipatedAction> projectedActions = bill.getAnticipatedActions();
                synchronized (InternalResourceService.this.mLock) {
                    for (int i = 0; i < projectedActions.size(); i++) {
                        try {
                            action = projectedActions.get(i);
                            cost = InternalResourceService.this.mCompleteEconomicPolicy.getCostOfAction(action.actionId, userId, pkgName);
                        } catch (Throwable th) {
                            th = th;
                        }
                        try {
                            requiredBalance += (cost.price * action.numInstantaneousCalls) + (cost.price * (action.ongoingDurationMs / 1000));
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    }
                    long requiredBalance2 = requiredBalance;
                    return InternalResourceService.this.mAgent.getBalanceLocked(userId, pkgName) >= requiredBalance2 && InternalResourceService.this.mScribe.getRemainingConsumableCakesLocked() >= requiredBalance2;
                }
            }
            return true;
        }

        @Override // com.android.server.tare.EconomyManagerInternal
        public long getMaxDurationMs(int userId, String pkgName, EconomyManagerInternal.ActionBill bill) {
            if (InternalResourceService.this.mIsEnabled && !InternalResourceService.this.isSystem(userId, pkgName)) {
                long totalCostPerSecond = 0;
                List<EconomyManagerInternal.AnticipatedAction> projectedActions = bill.getAnticipatedActions();
                synchronized (InternalResourceService.this.mLock) {
                    for (int i = 0; i < projectedActions.size(); i++) {
                        EconomyManagerInternal.AnticipatedAction action = projectedActions.get(i);
                        EconomicPolicy.Cost cost = InternalResourceService.this.mCompleteEconomicPolicy.getCostOfAction(action.actionId, userId, pkgName);
                        totalCostPerSecond += cost.price;
                    }
                    if (totalCostPerSecond == 0) {
                        return FOREVER_MS;
                    }
                    long minBalance = Math.min(InternalResourceService.this.mAgent.getBalanceLocked(userId, pkgName), InternalResourceService.this.mScribe.getRemainingConsumableCakesLocked());
                    return (1000 * minBalance) / totalCostPerSecond;
                }
            }
            return FOREVER_MS;
        }

        @Override // com.android.server.tare.EconomyManagerInternal
        public boolean isEnabled() {
            return InternalResourceService.this.mIsEnabled;
        }

        @Override // com.android.server.tare.EconomyManagerInternal
        public void noteInstantaneousEvent(int userId, String pkgName, int eventId, String tag) {
            if (!InternalResourceService.this.mIsEnabled) {
                return;
            }
            synchronized (InternalResourceService.this.mLock) {
                InternalResourceService.this.mAgent.noteInstantaneousEventLocked(userId, pkgName, eventId, tag);
            }
        }

        @Override // com.android.server.tare.EconomyManagerInternal
        public void noteOngoingEventStarted(int userId, String pkgName, int eventId, String tag) {
            if (!InternalResourceService.this.mIsEnabled) {
                return;
            }
            synchronized (InternalResourceService.this.mLock) {
                long nowElapsed = SystemClock.elapsedRealtime();
                InternalResourceService.this.mAgent.noteOngoingEventLocked(userId, pkgName, eventId, tag, nowElapsed);
            }
        }

        @Override // com.android.server.tare.EconomyManagerInternal
        public void noteOngoingEventStopped(int userId, String pkgName, int eventId, String tag) {
            if (!InternalResourceService.this.mIsEnabled) {
                return;
            }
            long nowElapsed = SystemClock.elapsedRealtime();
            long now = TareUtils.getCurrentTimeMillis();
            synchronized (InternalResourceService.this.mLock) {
                InternalResourceService.this.mAgent.stopOngoingActionLocked(userId, pkgName, eventId, tag, nowElapsed, now);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class ConfigObserver extends ContentObserver implements DeviceConfig.OnPropertiesChangedListener {
        private static final String KEY_DC_ENABLE_TARE = "enable_tare";
        private final ContentResolver mContentResolver;

        ConfigObserver(Handler handler, Context context) {
            super(handler);
            this.mContentResolver = context.getContentResolver();
        }

        public void start() {
            DeviceConfig.addOnPropertiesChangedListener("tare", TareHandlerThread.getExecutor(), this);
            this.mContentResolver.registerContentObserver(Settings.Global.getUriFor(KEY_DC_ENABLE_TARE), false, this);
            this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("tare_alarm_manager_constants"), false, this);
            this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("tare_job_scheduler_constants"), false, this);
            onPropertiesChanged(getAllDeviceConfigProperties());
            updateEnabledStatus();
        }

        DeviceConfig.Properties getAllDeviceConfigProperties() {
            return DeviceConfig.getProperties("tare", new String[0]);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.Global.getUriFor(KEY_DC_ENABLE_TARE))) {
                updateEnabledStatus();
            } else if (uri.equals(Settings.Global.getUriFor("tare_alarm_manager_constants")) || uri.equals(Settings.Global.getUriFor("tare_job_scheduler_constants"))) {
                updateEconomicPolicy();
            }
        }

        public void onPropertiesChanged(DeviceConfig.Properties properties) {
            boolean economicPolicyUpdated = false;
            synchronized (InternalResourceService.this.mLock) {
                for (String name : properties.getKeyset()) {
                    if (name != null) {
                        char c = 65535;
                        switch (name.hashCode()) {
                            case -1428831588:
                                if (name.equals(KEY_DC_ENABLE_TARE)) {
                                    c = 0;
                                    break;
                                }
                        }
                        switch (c) {
                            case 0:
                                updateEnabledStatus();
                                break;
                            default:
                                if (!economicPolicyUpdated && (name.startsWith("am") || name.startsWith("js"))) {
                                    updateEconomicPolicy();
                                    economicPolicyUpdated = true;
                                    break;
                                }
                                break;
                        }
                    }
                }
            }
        }

        private void updateEnabledStatus() {
            boolean isTareEnabled = Settings.Global.getInt(this.mContentResolver, KEY_DC_ENABLE_TARE, DeviceConfig.getBoolean("tare", KEY_DC_ENABLE_TARE, false) ? 1 : 0) == 1;
            if (InternalResourceService.this.mIsEnabled != isTareEnabled) {
                InternalResourceService.this.mIsEnabled = isTareEnabled;
                if (InternalResourceService.this.mIsEnabled) {
                    InternalResourceService.this.setupEverything();
                } else {
                    InternalResourceService.this.tearDownEverything();
                }
                InternalResourceService.this.mHandler.sendEmptyMessage(3);
            }
        }

        private void updateEconomicPolicy() {
            synchronized (InternalResourceService.this.mLock) {
                long initialLimit = InternalResourceService.this.mCompleteEconomicPolicy.getInitialSatiatedConsumptionLimit();
                long hardLimit = InternalResourceService.this.mCompleteEconomicPolicy.getHardSatiatedConsumptionLimit();
                InternalResourceService.this.mCompleteEconomicPolicy.tearDown();
                InternalResourceService.this.mCompleteEconomicPolicy = new CompleteEconomicPolicy(InternalResourceService.this);
                if (InternalResourceService.this.mIsEnabled && InternalResourceService.this.mBootPhase >= 500) {
                    InternalResourceService.this.mCompleteEconomicPolicy.setup(getAllDeviceConfigProperties());
                    if (initialLimit != InternalResourceService.this.mCompleteEconomicPolicy.getInitialSatiatedConsumptionLimit() || hardLimit != InternalResourceService.this.mCompleteEconomicPolicy.getHardSatiatedConsumptionLimit()) {
                        InternalResourceService.this.mScribe.setConsumptionLimitLocked(InternalResourceService.this.mCompleteEconomicPolicy.getInitialSatiatedConsumptionLimit());
                    }
                    InternalResourceService.this.mAgent.onPricingChangedLocked();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void dumpHelp(PrintWriter pw) {
        pw.println("Resource Economy (economy) dump options:");
        pw.println("  [-h|--help] [package] ...");
        pw.println("    -h | --help: print this help");
        pw.println("  [package] is an optional package name to limit the output to.");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpInternal(IndentingPrintWriter pw, boolean dumpAll) {
        synchronized (this.mLock) {
            pw.print("Is enabled: ");
            pw.println(this.mIsEnabled);
            pw.print("Current battery level: ");
            pw.println(this.mCurrentBatteryLevel);
            long consumptionLimit = getConsumptionLimitLocked();
            pw.print("Consumption limit (current/initial-satiated/current-satiated): ");
            pw.print(TareUtils.cakeToString(consumptionLimit));
            pw.print(SliceClientPermissions.SliceAuthority.DELIMITER);
            pw.print(TareUtils.cakeToString(this.mCompleteEconomicPolicy.getInitialSatiatedConsumptionLimit()));
            pw.print(SliceClientPermissions.SliceAuthority.DELIMITER);
            pw.println(TareUtils.cakeToString(this.mScribe.getSatiatedConsumptionLimitLocked()));
            long remainingConsumable = this.mScribe.getRemainingConsumableCakesLocked();
            pw.print("Goods remaining: ");
            pw.print(TareUtils.cakeToString(remainingConsumable));
            pw.print(" (");
            pw.print(String.format("%.2f", Float.valueOf((((float) remainingConsumable) * 100.0f) / ((float) consumptionLimit))));
            pw.println("% of current limit)");
            pw.print("Device wealth: ");
            pw.println(TareUtils.cakeToString(this.mScribe.getCakesInCirculationForLoggingLocked()));
            pw.println();
            pw.print("Exempted apps", this.mExemptedApps);
            pw.println();
            pw.println();
            this.mCompleteEconomicPolicy.dump(pw);
            pw.println();
            this.mScribe.dumpLocked(pw, dumpAll);
            pw.println();
            this.mAgent.dumpLocked(pw);
            pw.println();
            this.mAnalyst.dump(pw);
        }
    }
}
