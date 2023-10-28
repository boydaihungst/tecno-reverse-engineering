package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.IUidObserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManagerInternal;
import android.os.PowerSaveState;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.SparseSetArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.app.IAppOpsCallback;
import com.android.internal.app.IAppOpsService;
import com.android.internal.util.jobs.ArrayUtils;
import com.android.internal.util.jobs.StatLogger;
import com.android.server.AppStateTracker;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.usage.AppStandbyInternal;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class AppStateTrackerImpl implements AppStateTracker {
    private static final boolean DEBUG = false;
    static final int TARGET_OP = 70;
    ActivityManagerInternal mActivityManagerInternal;
    AppOpsManager mAppOpsManager;
    IAppOpsService mAppOpsService;
    AppStandbyInternal mAppStandbyInternal;
    boolean mBatterySaverEnabled;
    private final Context mContext;
    FeatureFlagsObserver mFlagsObserver;
    boolean mForceAllAppStandbyForSmallBattery;
    boolean mForceAllAppsStandby;
    boolean mForcedAppStandbyEnabled;
    private final MyHandler mHandler;
    IActivityManager mIActivityManager;
    boolean mIsPluggedIn;
    private int[] mPowerExemptAllAppIds;
    PowerManagerInternal mPowerManagerInternal;
    StandbyTracker mStandbyTracker;
    boolean mStarted;
    private int[] mTempExemptAppIds;
    private final Object mLock = new Object();
    final ArraySet<Pair<Integer, String>> mRunAnyRestrictedPackages = new ArraySet<>();
    final SparseBooleanArray mActiveUids = new SparseBooleanArray();
    private int[] mPowerExemptUserAppIds = new int[0];
    final SparseSetArray<String> mExemptedBucketPackages = new SparseSetArray<>();
    final ArraySet<Listener> mListeners = new ArraySet<>();
    volatile Set<Pair<Integer, String>> mBackgroundRestrictedUidPackages = Collections.emptySet();
    private final StatLogger mStatLogger = new StatLogger(new String[]{"UID_FG_STATE_CHANGED", "UID_ACTIVE_STATE_CHANGED", "RUN_ANY_CHANGED", "ALL_UNEXEMPTED", "ALL_EXEMPTION_LIST_CHANGED", "TEMP_EXEMPTION_LIST_CHANGED", "EXEMPTED_BUCKET_CHANGED", "FORCE_ALL_CHANGED", "FORCE_APP_STANDBY_FEATURE_FLAG_CHANGED", "IS_UID_ACTIVE_CACHED", "IS_UID_ACTIVE_RAW"});
    private final ActivityManagerInternal.AppBackgroundRestrictionListener mAppBackgroundRestrictionListener = new ActivityManagerInternal.AppBackgroundRestrictionListener() { // from class: com.android.server.AppStateTrackerImpl.2
        public void onAutoRestrictedBucketFeatureFlagChanged(boolean autoRestrictedBucket) {
            AppStateTrackerImpl.this.mHandler.notifyAutoRestrictedBucketFeatureFlagChanged(autoRestrictedBucket);
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.server.AppStateTrackerImpl.3
        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            char c;
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
            String action = intent.getAction();
            boolean z = true;
            switch (action.hashCode()) {
                case -2061058799:
                    if (action.equals("android.intent.action.USER_REMOVED")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case -1538406691:
                    if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 525384130:
                    if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
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
                    if (userId > 0) {
                        AppStateTrackerImpl.this.mHandler.doUserRemoved(userId);
                        return;
                    }
                    return;
                case 1:
                    synchronized (AppStateTrackerImpl.this.mLock) {
                        AppStateTrackerImpl appStateTrackerImpl = AppStateTrackerImpl.this;
                        if (intent.getIntExtra("plugged", 0) == 0) {
                            z = false;
                        }
                        appStateTrackerImpl.mIsPluggedIn = z;
                    }
                    AppStateTrackerImpl.this.updateForceAllAppStandbyState();
                    return;
                case 2:
                    if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                        String pkgName = intent.getData().getSchemeSpecificPart();
                        int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                        synchronized (AppStateTrackerImpl.this.mLock) {
                            AppStateTrackerImpl.this.mExemptedBucketPackages.remove(userId, pkgName);
                            AppStateTrackerImpl.this.mRunAnyRestrictedPackages.remove(Pair.create(Integer.valueOf(uid), pkgName));
                            AppStateTrackerImpl.this.updateBackgroundRestrictedUidPackagesLocked();
                            AppStateTrackerImpl.this.mActiveUids.delete(uid);
                        }
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    };

    /* loaded from: classes.dex */
    interface Stats {
        public static final int ALL_EXEMPTION_LIST_CHANGED = 4;
        public static final int ALL_UNEXEMPTED = 3;
        public static final int EXEMPTED_BUCKET_CHANGED = 6;
        public static final int FORCE_ALL_CHANGED = 7;
        public static final int FORCE_APP_STANDBY_FEATURE_FLAG_CHANGED = 8;
        public static final int IS_UID_ACTIVE_CACHED = 9;
        public static final int IS_UID_ACTIVE_RAW = 10;
        public static final int RUN_ANY_CHANGED = 2;
        public static final int TEMP_EXEMPTION_LIST_CHANGED = 5;
        public static final int UID_ACTIVE_STATE_CHANGED = 1;
        public static final int UID_FG_STATE_CHANGED = 0;
    }

    public void addBackgroundRestrictedAppListener(final AppStateTracker.BackgroundRestrictedAppListener listener) {
        addListener(new Listener() { // from class: com.android.server.AppStateTrackerImpl.1
            @Override // com.android.server.AppStateTrackerImpl.Listener
            public void updateBackgroundRestrictedForUidPackage(int uid, String packageName, boolean restricted) {
                listener.updateBackgroundRestrictedForUidPackage(uid, packageName, restricted);
            }
        });
    }

    public boolean isAppBackgroundRestricted(int uid, String packageName) {
        Set<Pair<Integer, String>> bgRestrictedUidPkgs = this.mBackgroundRestrictedUidPackages;
        return bgRestrictedUidPkgs.contains(Pair.create(Integer.valueOf(uid), packageName));
    }

    /* loaded from: classes.dex */
    class FeatureFlagsObserver extends ContentObserver {
        FeatureFlagsObserver() {
            super(null);
        }

        void register() {
            AppStateTrackerImpl.this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("forced_app_standby_enabled"), false, this);
            AppStateTrackerImpl.this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("forced_app_standby_for_small_battery_enabled"), false, this);
        }

        boolean isForcedAppStandbyEnabled() {
            return AppStateTrackerImpl.this.injectGetGlobalSettingInt("forced_app_standby_enabled", 1) == 1;
        }

        boolean isForcedAppStandbyForSmallBatteryEnabled() {
            return AppStateTrackerImpl.this.injectGetGlobalSettingInt("forced_app_standby_for_small_battery_enabled", 0) == 1;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.Global.getUriFor("forced_app_standby_enabled").equals(uri)) {
                boolean enabled = isForcedAppStandbyEnabled();
                synchronized (AppStateTrackerImpl.this.mLock) {
                    if (AppStateTrackerImpl.this.mForcedAppStandbyEnabled == enabled) {
                        return;
                    }
                    AppStateTrackerImpl.this.mForcedAppStandbyEnabled = enabled;
                    AppStateTrackerImpl.this.updateBackgroundRestrictedUidPackagesLocked();
                    AppStateTrackerImpl.this.mHandler.notifyForcedAppStandbyFeatureFlagChanged();
                }
            } else if (Settings.Global.getUriFor("forced_app_standby_for_small_battery_enabled").equals(uri)) {
                boolean enabled2 = isForcedAppStandbyForSmallBatteryEnabled();
                synchronized (AppStateTrackerImpl.this.mLock) {
                    if (AppStateTrackerImpl.this.mForceAllAppStandbyForSmallBattery == enabled2) {
                        return;
                    }
                    AppStateTrackerImpl.this.mForceAllAppStandbyForSmallBattery = enabled2;
                    AppStateTrackerImpl.this.updateForceAllAppStandbyState();
                }
            } else {
                Slog.w("AppStateTracker", "Unexpected feature flag uri encountered: " + uri);
            }
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Listener {
        /* JADX INFO: Access modifiers changed from: private */
        public void onRunAnyAppOpsChanged(AppStateTrackerImpl sender, int uid, String packageName) {
            updateJobsForUidPackage(uid, packageName, sender.isUidActive(uid));
            if (!sender.areAlarmsRestricted(uid, packageName)) {
                unblockAlarmsForUidPackage(uid, packageName);
            }
            if (!sender.isRunAnyInBackgroundAppOpsAllowed(uid, packageName)) {
                Slog.v("AppStateTracker", "Package " + packageName + SliceClientPermissions.SliceAuthority.DELIMITER + uid + " toggled into fg service restriction");
                updateBackgroundRestrictedForUidPackage(uid, packageName, true);
                return;
            }
            Slog.v("AppStateTracker", "Package " + packageName + SliceClientPermissions.SliceAuthority.DELIMITER + uid + " toggled out of fg service restriction");
            updateBackgroundRestrictedForUidPackage(uid, packageName, false);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onUidActiveStateChanged(AppStateTrackerImpl sender, int uid) {
            boolean isActive = sender.isUidActive(uid);
            updateJobsForUid(uid, isActive);
            updateAlarmsForUid(uid);
            if (isActive) {
                unblockAlarmsForUid(uid);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onPowerSaveUnexempted(AppStateTrackerImpl sender) {
            updateAllJobs();
            updateAllAlarms();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onPowerSaveExemptionListChanged(AppStateTrackerImpl sender) {
            updateAllJobs();
            updateAllAlarms();
            unblockAllUnrestrictedAlarms();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onTempPowerSaveExemptionListChanged(AppStateTrackerImpl sender) {
            updateAllJobs();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onExemptedBucketChanged(AppStateTrackerImpl sender) {
            updateAllJobs();
            updateAllAlarms();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onForceAllAppsStandbyChanged(AppStateTrackerImpl sender) {
            updateAllJobs();
            updateAllAlarms();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onAutoRestrictedBucketFeatureFlagChanged(AppStateTrackerImpl sender, boolean autoRestrictedBucket) {
            updateAllJobs();
            if (autoRestrictedBucket) {
                unblockAllUnrestrictedAlarms();
            }
        }

        public void updateAllJobs() {
        }

        public void updateJobsForUid(int uid, boolean isNowActive) {
        }

        public void updateJobsForUidPackage(int uid, String packageName, boolean isNowActive) {
        }

        public void updateBackgroundRestrictedForUidPackage(int uid, String packageName, boolean restricted) {
        }

        public void updateAllAlarms() {
        }

        public void updateAlarmsForUid(int uid) {
        }

        public void unblockAllUnrestrictedAlarms() {
        }

        public void unblockAlarmsForUid(int uid) {
        }

        public void unblockAlarmsForUidPackage(int uid, String packageName) {
        }

        public void removeAlarmsForUid(int uid) {
        }
    }

    public AppStateTrackerImpl(Context context, Looper looper) {
        int[] iArr = new int[0];
        this.mPowerExemptAllAppIds = iArr;
        this.mTempExemptAppIds = iArr;
        this.mContext = context;
        this.mHandler = new MyHandler(looper);
    }

    public void onSystemServicesReady() {
        synchronized (this.mLock) {
            if (this.mStarted) {
                return;
            }
            this.mStarted = true;
            this.mIActivityManager = (IActivityManager) Objects.requireNonNull(injectIActivityManager());
            this.mActivityManagerInternal = (ActivityManagerInternal) Objects.requireNonNull(injectActivityManagerInternal());
            this.mAppOpsManager = (AppOpsManager) Objects.requireNonNull(injectAppOpsManager());
            this.mAppOpsService = (IAppOpsService) Objects.requireNonNull(injectIAppOpsService());
            this.mPowerManagerInternal = (PowerManagerInternal) Objects.requireNonNull(injectPowerManagerInternal());
            this.mAppStandbyInternal = (AppStandbyInternal) Objects.requireNonNull(injectAppStandbyInternal());
            FeatureFlagsObserver featureFlagsObserver = new FeatureFlagsObserver();
            this.mFlagsObserver = featureFlagsObserver;
            featureFlagsObserver.register();
            this.mForcedAppStandbyEnabled = this.mFlagsObserver.isForcedAppStandbyEnabled();
            this.mForceAllAppStandbyForSmallBattery = this.mFlagsObserver.isForcedAppStandbyForSmallBatteryEnabled();
            StandbyTracker standbyTracker = new StandbyTracker();
            this.mStandbyTracker = standbyTracker;
            this.mAppStandbyInternal.addListener(standbyTracker);
            this.mActivityManagerInternal.addAppBackgroundRestrictionListener(this.mAppBackgroundRestrictionListener);
            try {
                this.mIActivityManager.registerUidObserver(new UidObserver(), 14, -1, (String) null);
                this.mAppOpsService.startWatchingMode(70, (String) null, new AppOpsWatcher());
            } catch (RemoteException e) {
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.USER_REMOVED");
            filter.addAction("android.intent.action.BATTERY_CHANGED");
            this.mContext.registerReceiver(this.mReceiver, filter);
            IntentFilter filter2 = new IntentFilter("android.intent.action.PACKAGE_REMOVED");
            filter2.addDataScheme("package");
            this.mContext.registerReceiver(this.mReceiver, filter2);
            refreshForcedAppStandbyUidPackagesLocked();
            this.mPowerManagerInternal.registerLowPowerModeObserver(11, new Consumer() { // from class: com.android.server.AppStateTrackerImpl$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    AppStateTrackerImpl.this.m52xe9c16886((PowerSaveState) obj);
                }
            });
            this.mBatterySaverEnabled = this.mPowerManagerInternal.getLowPowerState(11).batterySaverEnabled;
            updateForceAllAppStandbyState();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSystemServicesReady$0$com-android-server-AppStateTrackerImpl  reason: not valid java name */
    public /* synthetic */ void m52xe9c16886(PowerSaveState state) {
        synchronized (this.mLock) {
            this.mBatterySaverEnabled = state.batterySaverEnabled;
            updateForceAllAppStandbyState();
        }
    }

    AppOpsManager injectAppOpsManager() {
        return (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
    }

    IAppOpsService injectIAppOpsService() {
        return IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
    }

    IActivityManager injectIActivityManager() {
        return ActivityManager.getService();
    }

    ActivityManagerInternal injectActivityManagerInternal() {
        return (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
    }

    PowerManagerInternal injectPowerManagerInternal() {
        return (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
    }

    AppStandbyInternal injectAppStandbyInternal() {
        return (AppStandbyInternal) LocalServices.getService(AppStandbyInternal.class);
    }

    boolean isSmallBatteryDevice() {
        return ActivityManager.isSmallBatteryDevice();
    }

    int injectGetGlobalSettingInt(String key, int def) {
        return Settings.Global.getInt(this.mContext.getContentResolver(), key, def);
    }

    private void refreshForcedAppStandbyUidPackagesLocked() {
        this.mRunAnyRestrictedPackages.clear();
        List<AppOpsManager.PackageOps> ops = this.mAppOpsManager.getPackagesForOps(new int[]{70});
        if (ops == null) {
            return;
        }
        int size = ops.size();
        for (int i = 0; i < size; i++) {
            AppOpsManager.PackageOps pkg = ops.get(i);
            List<AppOpsManager.OpEntry> entries = ops.get(i).getOps();
            for (int j = 0; j < entries.size(); j++) {
                AppOpsManager.OpEntry ent = entries.get(j);
                if (ent.getOp() == 70 && ent.getMode() != 0) {
                    this.mRunAnyRestrictedPackages.add(Pair.create(Integer.valueOf(pkg.getUid()), pkg.getPackageName()));
                }
            }
        }
        updateBackgroundRestrictedUidPackagesLocked();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateBackgroundRestrictedUidPackagesLocked() {
        if (!this.mForcedAppStandbyEnabled) {
            this.mBackgroundRestrictedUidPackages = Collections.emptySet();
            return;
        }
        Set<Pair<Integer, String>> fasUidPkgs = new ArraySet<>();
        int size = this.mRunAnyRestrictedPackages.size();
        for (int i = 0; i < size; i++) {
            fasUidPkgs.add(this.mRunAnyRestrictedPackages.valueAt(i));
        }
        this.mBackgroundRestrictedUidPackages = Collections.unmodifiableSet(fasUidPkgs);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateForceAllAppStandbyState() {
        synchronized (this.mLock) {
            if (this.mForceAllAppStandbyForSmallBattery && isSmallBatteryDevice()) {
                toggleForceAllAppsStandbyLocked(!this.mIsPluggedIn);
            } else {
                toggleForceAllAppsStandbyLocked(this.mBatterySaverEnabled);
            }
        }
    }

    private void toggleForceAllAppsStandbyLocked(boolean enable) {
        if (enable == this.mForceAllAppsStandby) {
            return;
        }
        this.mForceAllAppsStandby = enable;
        this.mHandler.notifyForceAllAppsStandbyChanged();
    }

    private int findForcedAppStandbyUidPackageIndexLocked(int uid, String packageName) {
        int size = this.mRunAnyRestrictedPackages.size();
        if (size > 8) {
            return this.mRunAnyRestrictedPackages.indexOf(Pair.create(Integer.valueOf(uid), packageName));
        }
        for (int i = 0; i < size; i++) {
            Pair<Integer, String> pair = this.mRunAnyRestrictedPackages.valueAt(i);
            if (((Integer) pair.first).intValue() == uid && packageName.equals(pair.second)) {
                return i;
            }
        }
        return -1;
    }

    boolean isRunAnyRestrictedLocked(int uid, String packageName) {
        return findForcedAppStandbyUidPackageIndexLocked(uid, packageName) >= 0;
    }

    boolean updateForcedAppStandbyUidPackageLocked(int uid, String packageName, boolean restricted) {
        int index = findForcedAppStandbyUidPackageIndexLocked(uid, packageName);
        boolean wasRestricted = index >= 0;
        if (wasRestricted == restricted) {
            return false;
        }
        if (restricted) {
            this.mRunAnyRestrictedPackages.add(Pair.create(Integer.valueOf(uid), packageName));
        } else {
            this.mRunAnyRestrictedPackages.removeAt(index);
        }
        updateBackgroundRestrictedUidPackagesLocked();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean addUidToArray(SparseBooleanArray array, int uid) {
        if (UserHandle.isCore(uid) || array.get(uid)) {
            return false;
        }
        array.put(uid, true);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean removeUidFromArray(SparseBooleanArray array, int uid, boolean remove) {
        if (!UserHandle.isCore(uid) && array.get(uid)) {
            if (remove) {
                array.delete(uid);
                return true;
            }
            array.put(uid, false);
            return true;
        }
        return false;
    }

    /* loaded from: classes.dex */
    private final class UidObserver extends IUidObserver.Stub {
        private UidObserver() {
        }

        public void onUidStateChanged(int uid, int procState, long procStateSeq, int capability) {
        }

        public void onUidActive(int uid) {
            AppStateTrackerImpl.this.mHandler.onUidActive(uid);
        }

        public void onUidGone(int uid, boolean disabled) {
            AppStateTrackerImpl.this.mHandler.onUidGone(uid, disabled);
        }

        public void onUidIdle(int uid, boolean disabled) {
            AppStateTrackerImpl.this.mHandler.onUidIdle(uid, disabled);
        }

        public void onUidCachedChanged(int uid, boolean cached) {
        }

        public void onUidProcAdjChanged(int uid) {
        }
    }

    /* loaded from: classes.dex */
    private final class AppOpsWatcher extends IAppOpsCallback.Stub {
        private AppOpsWatcher() {
        }

        public void opChanged(int op, int uid, String packageName) throws RemoteException {
            boolean restricted = false;
            try {
                restricted = AppStateTrackerImpl.this.mAppOpsService.checkOperation(70, uid, packageName) != 0;
            } catch (RemoteException e) {
            }
            synchronized (AppStateTrackerImpl.this.mLock) {
                if (AppStateTrackerImpl.this.updateForcedAppStandbyUidPackageLocked(uid, packageName, restricted)) {
                    AppStateTrackerImpl.this.mHandler.notifyRunAnyAppOpsChanged(uid, packageName);
                }
            }
        }
    }

    /* loaded from: classes.dex */
    final class StandbyTracker extends AppStandbyInternal.AppIdleStateChangeListener {
        StandbyTracker() {
        }

        public void onAppIdleStateChanged(String packageName, int userId, boolean idle, int bucket, int reason) {
            boolean changed;
            synchronized (AppStateTrackerImpl.this.mLock) {
                if (bucket == 5) {
                    changed = AppStateTrackerImpl.this.mExemptedBucketPackages.add(userId, packageName);
                } else {
                    changed = AppStateTrackerImpl.this.mExemptedBucketPackages.remove(userId, packageName);
                }
                if (changed) {
                    AppStateTrackerImpl.this.mHandler.notifyExemptedBucketChanged();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Listener[] cloneListeners() {
        Listener[] listenerArr;
        synchronized (this.mLock) {
            ArraySet<Listener> arraySet = this.mListeners;
            listenerArr = (Listener[]) arraySet.toArray(new Listener[arraySet.size()]);
        }
        return listenerArr;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyHandler extends Handler {
        private static final int MSG_ALL_EXEMPTION_LIST_CHANGED = 5;
        private static final int MSG_ALL_UNEXEMPTED = 4;
        private static final int MSG_AUTO_RESTRICTED_BUCKET_FEATURE_FLAG_CHANGED = 11;
        private static final int MSG_EXEMPTED_BUCKET_CHANGED = 10;
        private static final int MSG_FORCE_ALL_CHANGED = 7;
        private static final int MSG_FORCE_APP_STANDBY_FEATURE_FLAG_CHANGED = 9;
        private static final int MSG_ON_UID_ACTIVE = 12;
        private static final int MSG_ON_UID_GONE = 13;
        private static final int MSG_ON_UID_IDLE = 14;
        private static final int MSG_RUN_ANY_CHANGED = 3;
        private static final int MSG_TEMP_EXEMPTION_LIST_CHANGED = 6;
        private static final int MSG_UID_ACTIVE_STATE_CHANGED = 0;
        private static final int MSG_USER_REMOVED = 8;

        MyHandler(Looper looper) {
            super(looper);
        }

        public void notifyUidActiveStateChanged(int uid) {
            obtainMessage(0, uid, 0).sendToTarget();
        }

        public void notifyRunAnyAppOpsChanged(int uid, String packageName) {
            obtainMessage(3, uid, 0, packageName).sendToTarget();
        }

        public void notifyAllUnexempted() {
            removeMessages(4);
            obtainMessage(4).sendToTarget();
        }

        public void notifyAllExemptionListChanged() {
            removeMessages(5);
            obtainMessage(5).sendToTarget();
        }

        public void notifyTempExemptionListChanged() {
            removeMessages(6);
            obtainMessage(6).sendToTarget();
        }

        public void notifyForceAllAppsStandbyChanged() {
            removeMessages(7);
            obtainMessage(7).sendToTarget();
        }

        public void notifyForcedAppStandbyFeatureFlagChanged() {
            removeMessages(9);
            obtainMessage(9).sendToTarget();
        }

        public void notifyExemptedBucketChanged() {
            removeMessages(10);
            obtainMessage(10).sendToTarget();
        }

        public void notifyAutoRestrictedBucketFeatureFlagChanged(boolean autoRestrictedBucket) {
            removeMessages(11);
            obtainMessage(11, autoRestrictedBucket ? 1 : 0, 0).sendToTarget();
        }

        public void doUserRemoved(int userId) {
            obtainMessage(8, userId, 0).sendToTarget();
        }

        public void onUidActive(int uid) {
            obtainMessage(12, uid, 0).sendToTarget();
        }

        public void onUidGone(int uid, boolean disabled) {
            obtainMessage(13, uid, disabled ? 1 : 0).sendToTarget();
        }

        public void onUidIdle(int uid, boolean disabled) {
            obtainMessage(14, uid, disabled ? 1 : 0).sendToTarget();
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 8:
                    AppStateTrackerImpl.this.handleUserRemoved(msg.arg1);
                    return;
                default:
                    synchronized (AppStateTrackerImpl.this.mLock) {
                        if (AppStateTrackerImpl.this.mStarted) {
                            AppStateTrackerImpl sender = AppStateTrackerImpl.this;
                            long start = AppStateTrackerImpl.this.mStatLogger.getTime();
                            int i = 0;
                            switch (msg.what) {
                                case 0:
                                    Listener[] cloneListeners = AppStateTrackerImpl.this.cloneListeners();
                                    int length = cloneListeners.length;
                                    while (i < length) {
                                        Listener l = cloneListeners[i];
                                        l.onUidActiveStateChanged(sender, msg.arg1);
                                        i++;
                                    }
                                    AppStateTrackerImpl.this.mStatLogger.logDurationStat(1, start);
                                    return;
                                case 1:
                                case 2:
                                default:
                                    return;
                                case 3:
                                    Listener[] cloneListeners2 = AppStateTrackerImpl.this.cloneListeners();
                                    int length2 = cloneListeners2.length;
                                    while (i < length2) {
                                        Listener l2 = cloneListeners2[i];
                                        l2.onRunAnyAppOpsChanged(sender, msg.arg1, (String) msg.obj);
                                        i++;
                                    }
                                    AppStateTrackerImpl.this.mStatLogger.logDurationStat(2, start);
                                    return;
                                case 4:
                                    Listener[] cloneListeners3 = AppStateTrackerImpl.this.cloneListeners();
                                    int length3 = cloneListeners3.length;
                                    while (i < length3) {
                                        Listener l3 = cloneListeners3[i];
                                        l3.onPowerSaveUnexempted(sender);
                                        i++;
                                    }
                                    AppStateTrackerImpl.this.mStatLogger.logDurationStat(3, start);
                                    return;
                                case 5:
                                    Listener[] cloneListeners4 = AppStateTrackerImpl.this.cloneListeners();
                                    int length4 = cloneListeners4.length;
                                    while (i < length4) {
                                        Listener l4 = cloneListeners4[i];
                                        l4.onPowerSaveExemptionListChanged(sender);
                                        i++;
                                    }
                                    AppStateTrackerImpl.this.mStatLogger.logDurationStat(4, start);
                                    return;
                                case 6:
                                    Listener[] cloneListeners5 = AppStateTrackerImpl.this.cloneListeners();
                                    int length5 = cloneListeners5.length;
                                    while (i < length5) {
                                        Listener l5 = cloneListeners5[i];
                                        l5.onTempPowerSaveExemptionListChanged(sender);
                                        i++;
                                    }
                                    AppStateTrackerImpl.this.mStatLogger.logDurationStat(5, start);
                                    return;
                                case 7:
                                    Listener[] cloneListeners6 = AppStateTrackerImpl.this.cloneListeners();
                                    int length6 = cloneListeners6.length;
                                    while (i < length6) {
                                        Listener l6 = cloneListeners6[i];
                                        l6.onForceAllAppsStandbyChanged(sender);
                                        i++;
                                    }
                                    AppStateTrackerImpl.this.mStatLogger.logDurationStat(7, start);
                                    return;
                                case 8:
                                    AppStateTrackerImpl.this.handleUserRemoved(msg.arg1);
                                    return;
                                case 9:
                                    synchronized (AppStateTrackerImpl.this.mLock) {
                                        if (AppStateTrackerImpl.this.mForcedAppStandbyEnabled) {
                                            unblockAlarms = false;
                                        }
                                    }
                                    Listener[] cloneListeners7 = AppStateTrackerImpl.this.cloneListeners();
                                    int length7 = cloneListeners7.length;
                                    while (i < length7) {
                                        Listener l7 = cloneListeners7[i];
                                        l7.updateAllJobs();
                                        if (unblockAlarms) {
                                            l7.unblockAllUnrestrictedAlarms();
                                        }
                                        i++;
                                    }
                                    AppStateTrackerImpl.this.mStatLogger.logDurationStat(8, start);
                                    return;
                                case 10:
                                    Listener[] cloneListeners8 = AppStateTrackerImpl.this.cloneListeners();
                                    int length8 = cloneListeners8.length;
                                    while (i < length8) {
                                        Listener l8 = cloneListeners8[i];
                                        l8.onExemptedBucketChanged(sender);
                                        i++;
                                    }
                                    AppStateTrackerImpl.this.mStatLogger.logDurationStat(6, start);
                                    return;
                                case 11:
                                    unblockAlarms = msg.arg1 == 1;
                                    boolean autoRestrictedBucket = unblockAlarms;
                                    Listener[] cloneListeners9 = AppStateTrackerImpl.this.cloneListeners();
                                    int length9 = cloneListeners9.length;
                                    while (i < length9) {
                                        Listener l9 = cloneListeners9[i];
                                        l9.onAutoRestrictedBucketFeatureFlagChanged(sender, autoRestrictedBucket);
                                        i++;
                                    }
                                    return;
                                case 12:
                                    handleUidActive(msg.arg1);
                                    return;
                                case 13:
                                    handleUidGone(msg.arg1);
                                    if (msg.arg2 != 0) {
                                        handleUidDisabled(msg.arg1);
                                        return;
                                    }
                                    return;
                                case 14:
                                    handleUidIdle(msg.arg1);
                                    if (msg.arg2 != 0) {
                                        handleUidDisabled(msg.arg1);
                                        return;
                                    }
                                    return;
                            }
                        }
                        return;
                    }
            }
        }

        private void handleUidDisabled(int uid) {
            Listener[] cloneListeners;
            for (Listener l : AppStateTrackerImpl.this.cloneListeners()) {
                l.removeAlarmsForUid(uid);
            }
        }

        public void handleUidActive(int uid) {
            synchronized (AppStateTrackerImpl.this.mLock) {
                if (AppStateTrackerImpl.addUidToArray(AppStateTrackerImpl.this.mActiveUids, uid)) {
                    AppStateTrackerImpl.this.mHandler.notifyUidActiveStateChanged(uid);
                }
            }
        }

        public void handleUidGone(int uid) {
            removeUid(uid, true);
        }

        public void handleUidIdle(int uid) {
            removeUid(uid, false);
        }

        private void removeUid(int uid, boolean remove) {
            synchronized (AppStateTrackerImpl.this.mLock) {
                if (AppStateTrackerImpl.removeUidFromArray(AppStateTrackerImpl.this.mActiveUids, uid, remove)) {
                    AppStateTrackerImpl.this.mHandler.notifyUidActiveStateChanged(uid);
                }
            }
        }
    }

    void handleUserRemoved(int removedUserId) {
        synchronized (this.mLock) {
            for (int i = this.mRunAnyRestrictedPackages.size() - 1; i >= 0; i--) {
                Pair<Integer, String> pair = this.mRunAnyRestrictedPackages.valueAt(i);
                int uid = ((Integer) pair.first).intValue();
                int userId = UserHandle.getUserId(uid);
                if (userId == removedUserId) {
                    this.mRunAnyRestrictedPackages.removeAt(i);
                }
            }
            updateBackgroundRestrictedUidPackagesLocked();
            cleanUpArrayForUser(this.mActiveUids, removedUserId);
            this.mExemptedBucketPackages.remove(removedUserId);
        }
    }

    private void cleanUpArrayForUser(SparseBooleanArray array, int removedUserId) {
        for (int i = array.size() - 1; i >= 0; i--) {
            int uid = array.keyAt(i);
            int userId = UserHandle.getUserId(uid);
            if (userId == removedUserId) {
                array.removeAt(i);
            }
        }
    }

    public void setPowerSaveExemptionListAppIds(int[] powerSaveExemptionListExceptIdleAppIdArray, int[] powerSaveExemptionListUserAppIdArray, int[] tempExemptionListAppIdArray) {
        synchronized (this.mLock) {
            int[] previousExemptionList = this.mPowerExemptAllAppIds;
            int[] previousTempExemptionList = this.mTempExemptAppIds;
            this.mPowerExemptAllAppIds = powerSaveExemptionListExceptIdleAppIdArray;
            this.mTempExemptAppIds = tempExemptionListAppIdArray;
            this.mPowerExemptUserAppIds = powerSaveExemptionListUserAppIdArray;
            if (isAnyAppIdUnexempt(previousExemptionList, powerSaveExemptionListExceptIdleAppIdArray)) {
                this.mHandler.notifyAllUnexempted();
            } else if (!Arrays.equals(previousExemptionList, this.mPowerExemptAllAppIds)) {
                this.mHandler.notifyAllExemptionListChanged();
            }
            if (!Arrays.equals(previousTempExemptionList, this.mTempExemptAppIds)) {
                this.mHandler.notifyTempExemptionListChanged();
            }
        }
    }

    static boolean isAnyAppIdUnexempt(int[] prevArray, int[] newArray) {
        boolean prevFinished;
        boolean newFinished;
        int i1 = 0;
        int i2 = 0;
        while (true) {
            prevFinished = i1 >= prevArray.length;
            newFinished = i2 >= newArray.length;
            if (prevFinished || newFinished) {
                break;
            }
            int a1 = prevArray[i1];
            int a2 = newArray[i2];
            if (a1 == a2) {
                i1++;
                i2++;
            } else if (a1 < a2) {
                return true;
            } else {
                i2++;
            }
        }
        if (prevFinished) {
            return false;
        }
        return newFinished;
    }

    public void addListener(Listener listener) {
        synchronized (this.mLock) {
            this.mListeners.add(listener);
        }
    }

    public boolean areAlarmsRestricted(int uid, String packageName) {
        boolean z = false;
        if (isUidActive(uid)) {
            return false;
        }
        synchronized (this.mLock) {
            int appId = UserHandle.getAppId(uid);
            if (ArrayUtils.contains(this.mPowerExemptAllAppIds, appId)) {
                return false;
            }
            if (this.mForcedAppStandbyEnabled && !this.mActivityManagerInternal.isBgAutoRestrictedBucketFeatureFlagEnabled() && isRunAnyRestrictedLocked(uid, packageName)) {
                z = true;
            }
            return z;
        }
    }

    public boolean areAlarmsRestrictedByBatterySaver(int uid, String packageName) {
        if (isUidActive(uid)) {
            return false;
        }
        synchronized (this.mLock) {
            int appId = UserHandle.getAppId(uid);
            if (ArrayUtils.contains(this.mPowerExemptAllAppIds, appId)) {
                return false;
            }
            int userId = UserHandle.getUserId(uid);
            if (this.mAppStandbyInternal.isAppIdleEnabled() && !this.mAppStandbyInternal.isInParole() && this.mExemptedBucketPackages.contains(userId, packageName)) {
                return false;
            }
            return this.mForceAllAppsStandby;
        }
    }

    public boolean areJobsRestricted(int uid, String packageName, boolean hasForegroundExemption) {
        if (isUidActive(uid)) {
            return false;
        }
        synchronized (this.mLock) {
            int appId = UserHandle.getAppId(uid);
            if (!ArrayUtils.contains(this.mPowerExemptAllAppIds, appId) && !ArrayUtils.contains(this.mTempExemptAppIds, appId)) {
                if (this.mForcedAppStandbyEnabled && !this.mActivityManagerInternal.isBgAutoRestrictedBucketFeatureFlagEnabled() && isRunAnyRestrictedLocked(uid, packageName)) {
                    return true;
                }
                if (hasForegroundExemption) {
                    return false;
                }
                int userId = UserHandle.getUserId(uid);
                if (this.mAppStandbyInternal.isAppIdleEnabled() && !this.mAppStandbyInternal.isInParole() && this.mExemptedBucketPackages.contains(userId, packageName)) {
                    return false;
                }
                return this.mForceAllAppsStandby;
            }
            return false;
        }
    }

    public boolean isUidActive(int uid) {
        boolean z;
        if (UserHandle.isCore(uid)) {
            return true;
        }
        synchronized (this.mLock) {
            z = this.mActiveUids.get(uid);
        }
        return z;
    }

    public boolean isUidActiveSynced(int uid) {
        if (isUidActive(uid)) {
            return true;
        }
        long start = this.mStatLogger.getTime();
        boolean ret = this.mActivityManagerInternal.isUidActive(uid);
        this.mStatLogger.logDurationStat(10, start);
        return ret;
    }

    public boolean isForceAllAppsStandbyEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mForceAllAppsStandby;
        }
        return z;
    }

    public boolean isRunAnyInBackgroundAppOpsAllowed(int uid, String packageName) {
        boolean z;
        synchronized (this.mLock) {
            z = !isRunAnyRestrictedLocked(uid, packageName);
        }
        return z;
    }

    public boolean isUidPowerSaveExempt(int uid) {
        boolean contains;
        synchronized (this.mLock) {
            contains = ArrayUtils.contains(this.mPowerExemptAllAppIds, UserHandle.getAppId(uid));
        }
        return contains;
    }

    public boolean isUidPowerSaveUserExempt(int uid) {
        boolean contains;
        synchronized (this.mLock) {
            contains = ArrayUtils.contains(this.mPowerExemptUserAppIds, UserHandle.getAppId(uid));
        }
        return contains;
    }

    public boolean isUidTempPowerSaveExempt(int uid) {
        boolean contains;
        synchronized (this.mLock) {
            contains = ArrayUtils.contains(this.mTempExemptAppIds, UserHandle.getAppId(uid));
        }
        return contains;
    }

    public void dump(IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            pw.println("Current AppStateTracker State:");
            pw.increaseIndent();
            pw.println("Forced App Standby Feature enabled: " + this.mForcedAppStandbyEnabled);
            pw.print("Force all apps standby: ");
            pw.println(isForceAllAppsStandbyEnabled());
            pw.print("Small Battery Device: ");
            pw.println(isSmallBatteryDevice());
            pw.print("Force all apps standby for small battery device: ");
            pw.println(this.mForceAllAppStandbyForSmallBattery);
            pw.print("Plugged In: ");
            pw.println(this.mIsPluggedIn);
            pw.print("Active uids: ");
            dumpUids(pw, this.mActiveUids);
            pw.print("Except-idle + user exemption list appids: ");
            pw.println(Arrays.toString(this.mPowerExemptAllAppIds));
            pw.print("User exemption list appids: ");
            pw.println(Arrays.toString(this.mPowerExemptUserAppIds));
            pw.print("Temp exemption list appids: ");
            pw.println(Arrays.toString(this.mTempExemptAppIds));
            pw.println("Exempted bucket packages:");
            pw.increaseIndent();
            for (int i = 0; i < this.mExemptedBucketPackages.size(); i++) {
                pw.print("User ");
                pw.print(this.mExemptedBucketPackages.keyAt(i));
                pw.println();
                pw.increaseIndent();
                for (int j = 0; j < this.mExemptedBucketPackages.sizeAt(i); j++) {
                    pw.print((String) this.mExemptedBucketPackages.valueAt(i, j));
                    pw.println();
                }
                pw.decreaseIndent();
            }
            pw.decreaseIndent();
            pw.println();
            pw.println("Restricted packages:");
            pw.increaseIndent();
            Iterator<Pair<Integer, String>> it = this.mRunAnyRestrictedPackages.iterator();
            while (it.hasNext()) {
                Pair<Integer, String> uidAndPackage = it.next();
                pw.print(UserHandle.formatUid(((Integer) uidAndPackage.first).intValue()));
                pw.print(" ");
                pw.print((String) uidAndPackage.second);
                pw.println();
            }
            pw.decreaseIndent();
            this.mStatLogger.dump(pw);
            pw.decreaseIndent();
        }
    }

    private void dumpUids(PrintWriter pw, SparseBooleanArray array) {
        pw.print("[");
        String sep = "";
        for (int i = 0; i < array.size(); i++) {
            if (array.valueAt(i)) {
                pw.print(sep);
                pw.print(UserHandle.formatUid(array.keyAt(i)));
                sep = " ";
            }
        }
        pw.println("]");
    }

    public void dumpProto(ProtoOutputStream proto, long fieldId) {
        int[] iArr;
        int[] iArr2;
        int[] iArr3;
        synchronized (this.mLock) {
            long token = proto.start(fieldId);
            proto.write(1133871366157L, this.mForcedAppStandbyEnabled);
            proto.write(1133871366145L, isForceAllAppsStandbyEnabled());
            proto.write(1133871366150L, isSmallBatteryDevice());
            proto.write(1133871366151L, this.mForceAllAppStandbyForSmallBattery);
            proto.write(1133871366152L, this.mIsPluggedIn);
            for (int i = 0; i < this.mActiveUids.size(); i++) {
                if (this.mActiveUids.valueAt(i)) {
                    proto.write(2220498092034L, this.mActiveUids.keyAt(i));
                }
            }
            for (int appId : this.mPowerExemptAllAppIds) {
                proto.write(2220498092035L, appId);
            }
            for (int appId2 : this.mPowerExemptUserAppIds) {
                proto.write(2220498092044L, appId2);
            }
            for (int appId3 : this.mTempExemptAppIds) {
                proto.write(2220498092036L, appId3);
            }
            for (int i2 = 0; i2 < this.mExemptedBucketPackages.size(); i2++) {
                for (int j = 0; j < this.mExemptedBucketPackages.sizeAt(i2); j++) {
                    long token2 = proto.start(2246267895818L);
                    proto.write(CompanionMessage.MESSAGE_ID, this.mExemptedBucketPackages.keyAt(i2));
                    proto.write(1138166333442L, (String) this.mExemptedBucketPackages.valueAt(i2, j));
                    proto.end(token2);
                }
            }
            Iterator<Pair<Integer, String>> it = this.mRunAnyRestrictedPackages.iterator();
            while (it.hasNext()) {
                Pair<Integer, String> uidAndPackage = it.next();
                long token22 = proto.start(2246267895813L);
                proto.write(CompanionMessage.MESSAGE_ID, ((Integer) uidAndPackage.first).intValue());
                proto.write(1138166333442L, (String) uidAndPackage.second);
                proto.end(token22);
            }
            this.mStatLogger.dumpProto(proto, 1146756268041L);
            proto.end(token);
        }
    }
}
