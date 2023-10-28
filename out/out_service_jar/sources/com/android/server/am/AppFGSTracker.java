package com.android.server.am;

import android.app.ActivityManagerInternal;
import android.app.IProcessObserver;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.os.SomeArgs;
import com.android.server.am.BaseAppStateEvents;
import com.android.server.am.BaseAppStateEventsTracker;
import com.android.server.am.BaseAppStateTimeEvents;
import com.android.server.am.BaseAppStateTracker;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class AppFGSTracker extends BaseAppStateDurationsTracker<AppFGSPolicy, PackageDurations> implements ActivityManagerInternal.ForegroundServiceStateListener {
    static final boolean DEBUG_BACKGROUND_FGS_TRACKER = false;
    static final String TAG = "ActivityManager";
    private final UidProcessMap<SparseBooleanArray> mFGSNotificationIDs;
    private final MyHandler mHandler;
    final NotificationListener mNotificationListener;
    final IProcessObserver.Stub mProcessObserver;
    private final ArrayMap<PackageDurations, Long> mTmpPkgDurations;

    public void onForegroundServiceStateChanged(String packageName, int uid, int pid, boolean started) {
        this.mHandler.obtainMessage(started ? 0 : 1, pid, uid, packageName).sendToTarget();
    }

    public void onForegroundServiceNotificationUpdated(String packageName, int uid, int foregroundId, boolean canceling) {
        SomeArgs args = SomeArgs.obtain();
        args.argi1 = uid;
        args.argi2 = foregroundId;
        args.arg1 = packageName;
        args.arg2 = canceling ? Boolean.TRUE : Boolean.FALSE;
        this.mHandler.obtainMessage(3, args).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class MyHandler extends Handler {
        static final int MSG_CHECK_LONG_RUNNING_FGS = 4;
        static final int MSG_FOREGROUND_SERVICES_CHANGED = 2;
        static final int MSG_FOREGROUND_SERVICES_NOTIFICATION_UPDATED = 3;
        static final int MSG_FOREGROUND_SERVICES_STARTED = 0;
        static final int MSG_FOREGROUND_SERVICES_STOPPED = 1;
        static final int MSG_NOTIFICATION_POSTED = 5;
        static final int MSG_NOTIFICATION_REMOVED = 6;
        private final AppFGSTracker mTracker;

        MyHandler(AppFGSTracker tracker) {
            super(tracker.mBgHandler.getLooper());
            this.mTracker = tracker;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    this.mTracker.handleForegroundServicesChanged((String) msg.obj, msg.arg1, msg.arg2, true);
                    return;
                case 1:
                    this.mTracker.handleForegroundServicesChanged((String) msg.obj, msg.arg1, msg.arg2, false);
                    return;
                case 2:
                    this.mTracker.handleForegroundServicesChanged((String) msg.obj, msg.arg1, msg.arg2);
                    return;
                case 3:
                    SomeArgs args = (SomeArgs) msg.obj;
                    this.mTracker.handleForegroundServiceNotificationUpdated((String) args.arg1, args.argi1, args.argi2, ((Boolean) args.arg2).booleanValue());
                    args.recycle();
                    return;
                case 4:
                    this.mTracker.checkLongRunningFgs();
                    return;
                case 5:
                    this.mTracker.handleNotificationPosted((String) msg.obj, msg.arg1, msg.arg2);
                    return;
                case 6:
                    this.mTracker.handleNotificationRemoved((String) msg.obj, msg.arg1, msg.arg2);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppFGSTracker(Context context, AppRestrictionController controller) {
        this(context, controller, null, null);
    }

    AppFGSTracker(Context context, AppRestrictionController controller, Constructor<? extends BaseAppStateTracker.Injector<AppFGSPolicy>> injector, Object outerContext) {
        super(context, controller, injector, outerContext);
        this.mFGSNotificationIDs = new UidProcessMap<>();
        this.mTmpPkgDurations = new ArrayMap<>();
        this.mNotificationListener = new NotificationListener();
        this.mProcessObserver = new IProcessObserver.Stub() { // from class: com.android.server.am.AppFGSTracker.1
            public void onForegroundActivitiesChanged(int pid, int uid, boolean fg) {
            }

            public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
                String packageName = AppFGSTracker.this.mAppRestrictionController.getPackageName(pid);
                if (packageName != null) {
                    AppFGSTracker.this.mHandler.obtainMessage(2, uid, serviceTypes, packageName).sendToTarget();
                }
            }

            public void onProcessDied(int pid, int uid) {
            }
        };
        this.mHandler = new MyHandler(this);
        this.mInjector.setPolicy(new AppFGSPolicy(this.mInjector, this));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public int getType() {
        return 3;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public void onSystemReady() {
        super.onSystemReady();
        this.mInjector.getActivityManagerInternal().addForegroundServiceStateListener(this);
        this.mInjector.getActivityManagerInternal().registerProcessObserver(this.mProcessObserver);
    }

    @Override // com.android.server.am.BaseAppStateDurationsTracker, com.android.server.am.BaseAppStateEventsTracker
    void reset() {
        this.mHandler.removeMessages(4);
        super.reset();
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.am.BaseAppStateEvents.Factory
    public PackageDurations createAppStateEvents(int uid, String packageName) {
        return new PackageDurations(uid, packageName, (BaseAppStateEvents.MaxTrackingDurationConfig) this.mInjector.getPolicy(), this);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.am.BaseAppStateEvents.Factory
    public PackageDurations createAppStateEvents(PackageDurations other) {
        return new PackageDurations(other);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleForegroundServicesChanged(String packageName, int pid, int uid, boolean started) {
        boolean longRunningFGSGone;
        if (!((AppFGSPolicy) this.mInjector.getPolicy()).isEnabled()) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        int exemptReason = ((AppFGSPolicy) this.mInjector.getPolicy()).shouldExemptUid(uid);
        synchronized (this.mLock) {
            PackageDurations pkg = (PackageDurations) this.mPkgEvents.get(uid, packageName);
            if (pkg == null) {
                pkg = createAppStateEvents(uid, packageName);
                this.mPkgEvents.put(uid, packageName, pkg);
            }
            boolean wasLongRunning = pkg.isLongRunning();
            pkg.addEvent(started, now);
            longRunningFGSGone = wasLongRunning && !pkg.hasForegroundServices();
            if (longRunningFGSGone) {
                pkg.setIsLongRunning(false);
            }
            pkg.mExemptReason = exemptReason;
            scheduleDurationCheckLocked(now);
        }
        if (longRunningFGSGone) {
            ((AppFGSPolicy) this.mInjector.getPolicy()).onLongRunningFgsGone(packageName, uid);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleForegroundServiceNotificationUpdated(String packageName, int uid, int notificationId, boolean canceling) {
        int indexOfKey;
        synchronized (this.mLock) {
            SparseBooleanArray notificationIDs = this.mFGSNotificationIDs.get(uid, packageName);
            if (!canceling) {
                if (notificationIDs == null) {
                    notificationIDs = new SparseBooleanArray();
                    this.mFGSNotificationIDs.put(uid, packageName, notificationIDs);
                }
                notificationIDs.put(notificationId, false);
            } else if (notificationIDs != null && (indexOfKey = notificationIDs.indexOfKey(notificationId)) >= 0) {
                boolean wasVisible = notificationIDs.valueAt(indexOfKey);
                notificationIDs.removeAt(indexOfKey);
                if (notificationIDs.size() == 0) {
                    this.mFGSNotificationIDs.remove(uid, packageName);
                }
                for (int i = notificationIDs.size() - 1; i >= 0; i--) {
                    if (notificationIDs.valueAt(i)) {
                        return;
                    }
                }
                if (wasVisible) {
                    notifyListenersOnStateChange(uid, packageName, false, SystemClock.elapsedRealtime(), 8);
                }
            }
        }
    }

    private boolean hasForegroundServiceNotificationsLocked(String packageName, int uid) {
        SparseBooleanArray notificationIDs = this.mFGSNotificationIDs.get(uid, packageName);
        if (notificationIDs == null || notificationIDs.size() == 0) {
            return false;
        }
        for (int i = notificationIDs.size() - 1; i >= 0; i--) {
            if (notificationIDs.valueAt(i)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNotificationPosted(String pkgName, int uid, int notificationId) {
        int indexOfKey;
        boolean anyVisible;
        synchronized (this.mLock) {
            try {
                try {
                    try {
                        SparseBooleanArray notificationIDs = this.mFGSNotificationIDs.get(uid, pkgName);
                        if (notificationIDs != null && (indexOfKey = notificationIDs.indexOfKey(notificationId)) >= 0) {
                            if (notificationIDs.valueAt(indexOfKey)) {
                                return;
                            }
                            int i = notificationIDs.size() - 1;
                            while (true) {
                                if (i < 0) {
                                    anyVisible = false;
                                    break;
                                } else if (!notificationIDs.valueAt(i)) {
                                    i--;
                                } else {
                                    anyVisible = true;
                                    break;
                                }
                            }
                            notificationIDs.setValueAt(indexOfKey, true);
                            if (!anyVisible) {
                                notifyListenersOnStateChange(uid, pkgName, true, SystemClock.elapsedRealtime(), 8);
                            }
                        }
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNotificationRemoved(String pkgName, int uid, int notificationId) {
        int indexOfKey;
        synchronized (this.mLock) {
            SparseBooleanArray notificationIDs = this.mFGSNotificationIDs.get(uid, pkgName);
            if (notificationIDs != null && (indexOfKey = notificationIDs.indexOfKey(notificationId)) >= 0) {
                if (notificationIDs.valueAt(indexOfKey)) {
                    notificationIDs.setValueAt(indexOfKey, false);
                    for (int i = notificationIDs.size() - 1; i >= 0; i--) {
                        if (notificationIDs.valueAt(i)) {
                            return;
                        }
                    }
                    notifyListenersOnStateChange(uid, pkgName, false, SystemClock.elapsedRealtime(), 8);
                }
            }
        }
    }

    private void scheduleDurationCheckLocked(long now) {
        SparseArray map = this.mPkgEvents.getMap();
        long longest = -1;
        for (int i = map.size() - 1; i >= 0; i--) {
            ArrayMap<String, PackageDurations> val = (ArrayMap) map.valueAt(i);
            for (int j = val.size() - 1; j >= 0; j--) {
                PackageDurations pkg = val.valueAt(j);
                if (pkg.hasForegroundServices() && !pkg.isLongRunning()) {
                    longest = Math.max(getTotalDurations(pkg, now), longest);
                }
            }
        }
        this.mHandler.removeMessages(4);
        if (longest >= 0) {
            long future = this.mInjector.getServiceStartForegroundTimeout() + Math.max(0L, ((AppFGSPolicy) this.mInjector.getPolicy()).getFgsLongRunningThreshold() - longest);
            this.mHandler.sendEmptyMessageDelayed(4, future);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkLongRunningFgs() {
        SparseArray sparseArray;
        long threshold;
        AppFGSPolicy policy = (AppFGSPolicy) this.mInjector.getPolicy();
        final ArrayMap<PackageDurations, Long> pkgWithLongFgs = this.mTmpPkgDurations;
        long now = SystemClock.elapsedRealtime();
        long threshold2 = policy.getFgsLongRunningThreshold();
        long windowSize = policy.getFgsLongRunningWindowSize();
        long trimTo = Math.max(0L, now - windowSize);
        synchronized (this.mLock) {
            try {
                try {
                    SparseArray map = this.mPkgEvents.getMap();
                    int i = map.size() - 1;
                    while (i >= 0) {
                        ArrayMap<String, PackageDurations> val = (ArrayMap) map.valueAt(i);
                        int j = val.size() - 1;
                        while (j >= 0) {
                            PackageDurations pkg = val.valueAt(j);
                            if (!pkg.hasForegroundServices() || pkg.isLongRunning()) {
                                sparseArray = map;
                                threshold = threshold2;
                            } else {
                                sparseArray = map;
                                long totalDuration = getTotalDurations(pkg, now);
                                if (totalDuration < threshold2) {
                                    threshold = threshold2;
                                } else {
                                    threshold = threshold2;
                                    pkgWithLongFgs.put(pkg, Long.valueOf(totalDuration));
                                    pkg.setIsLongRunning(true);
                                }
                            }
                            j--;
                            map = sparseArray;
                            threshold2 = threshold;
                        }
                        i--;
                        threshold2 = threshold2;
                    }
                    trim(trimTo);
                    int size = pkgWithLongFgs.size();
                    if (size > 0) {
                        Integer[] indices = new Integer[size];
                        for (int i2 = 0; i2 < size; i2++) {
                            indices[i2] = Integer.valueOf(i2);
                        }
                        Arrays.sort(indices, new Comparator() { // from class: com.android.server.am.AppFGSTracker$$ExternalSyntheticLambda0
                            @Override // java.util.Comparator
                            public final int compare(Object obj, Object obj2) {
                                int compare;
                                compare = Long.compare(((Long) r0.valueAt(((Integer) obj).intValue())).longValue(), ((Long) pkgWithLongFgs.valueAt(((Integer) obj2).intValue())).longValue());
                                return compare;
                            }
                        });
                        for (int i3 = size - 1; i3 >= 0; i3--) {
                            PackageDurations pkg2 = pkgWithLongFgs.keyAt(indices[i3].intValue());
                            policy.onLongRunningFgs(pkg2.mPackageName, pkg2.mUid, pkg2.mExemptReason);
                        }
                        pkgWithLongFgs.clear();
                    }
                    synchronized (this.mLock) {
                        scheduleDurationCheckLocked(now);
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
    public void handleForegroundServicesChanged(String packageName, int uid, int serviceTypes) {
        if (!((AppFGSPolicy) this.mInjector.getPolicy()).isEnabled()) {
            return;
        }
        int exemptReason = ((AppFGSPolicy) this.mInjector.getPolicy()).shouldExemptUid(uid);
        long now = SystemClock.elapsedRealtime();
        synchronized (this.mLock) {
            PackageDurations pkg = (PackageDurations) this.mPkgEvents.get(uid, packageName);
            if (pkg == null) {
                pkg = new PackageDurations(uid, packageName, (BaseAppStateEvents.MaxTrackingDurationConfig) this.mInjector.getPolicy(), this);
                this.mPkgEvents.put(uid, packageName, pkg);
            }
            pkg.setForegroundServiceType(serviceTypes, now);
            pkg.mExemptReason = exemptReason;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onBgFgsMonitorEnabled(boolean enabled) {
        if (enabled) {
            synchronized (this.mLock) {
                scheduleDurationCheckLocked(SystemClock.elapsedRealtime());
            }
            try {
                this.mNotificationListener.registerAsSystemService(this.mContext, new ComponentName(this.mContext, NotificationListener.class), -1);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        try {
            this.mNotificationListener.unregisterAsSystemService();
        } catch (RemoteException e2) {
        }
        this.mHandler.removeMessages(4);
        synchronized (this.mLock) {
            this.mPkgEvents.clear();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onBgFgsLongRunningThresholdChanged() {
        synchronized (this.mLock) {
            if (((AppFGSPolicy) this.mInjector.getPolicy()).isEnabled()) {
                scheduleDurationCheckLocked(SystemClock.elapsedRealtime());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int foregroundServiceTypeToIndex(int serviceType) {
        if (serviceType == 0) {
            return 0;
        }
        return Integer.numberOfTrailingZeros(serviceType) + 1;
    }

    static int indexToForegroundServiceType(int index) {
        if (index == PackageDurations.DEFAULT_INDEX) {
            return 0;
        }
        return 1 << (index - 1);
    }

    long getTotalDurations(PackageDurations pkg, long now) {
        return getTotalDurations(pkg.mPackageName, pkg.mUid, now, foregroundServiceTypeToIndex(0));
    }

    @Override // com.android.server.am.BaseAppStateDurationsTracker
    long getTotalDurations(int uid, long now) {
        return getTotalDurations(uid, now, foregroundServiceTypeToIndex(0));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasForegroundServices(String packageName, int uid) {
        boolean z;
        synchronized (this.mLock) {
            PackageDurations pkg = (PackageDurations) this.mPkgEvents.get(uid, packageName);
            z = pkg != null && pkg.hasForegroundServices();
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasForegroundServices(int uid) {
        synchronized (this.mLock) {
            ArrayMap<String, PackageDurations> pkgs = (ArrayMap) this.mPkgEvents.getMap().get(uid);
            if (pkgs != null) {
                for (int i = pkgs.size() - 1; i >= 0; i--) {
                    PackageDurations pkg = pkgs.valueAt(i);
                    if (pkg.hasForegroundServices()) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasForegroundServiceNotifications(String packageName, int uid) {
        boolean hasForegroundServiceNotificationsLocked;
        synchronized (this.mLock) {
            hasForegroundServiceNotificationsLocked = hasForegroundServiceNotificationsLocked(packageName, uid);
        }
        return hasForegroundServiceNotificationsLocked;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasForegroundServiceNotifications(int uid) {
        synchronized (this.mLock) {
            SparseArray<ArrayMap<String, SparseBooleanArray>> map = this.mFGSNotificationIDs.getMap();
            ArrayMap<String, SparseBooleanArray> pkgs = map.get(uid);
            if (pkgs != null) {
                for (int i = pkgs.size() - 1; i >= 0; i--) {
                    if (hasForegroundServiceNotificationsLocked(pkgs.keyAt(i), uid)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public byte[] getTrackerInfoForStatsd(int uid) {
        long fgsDurations = getTotalDurations(uid, SystemClock.elapsedRealtime());
        if (fgsDurations == 0) {
            return null;
        }
        ProtoOutputStream proto = new ProtoOutputStream();
        proto.write(1133871366145L, hasForegroundServiceNotifications(uid));
        proto.write(1112396529666L, fgsDurations);
        proto.flush();
        return proto.getBytes();
    }

    @Override // com.android.server.am.BaseAppStateEventsTracker, com.android.server.am.BaseAppStateTracker
    void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.println("APP FOREGROUND SERVICE TRACKER:");
        super.dump(pw, "  " + prefix);
    }

    @Override // com.android.server.am.BaseAppStateEventsTracker
    void dumpOthers(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.println("APPS WITH ACTIVE FOREGROUND SERVICES:");
        String prefix2 = "  " + prefix;
        synchronized (this.mLock) {
            SparseArray<ArrayMap<String, SparseBooleanArray>> map = this.mFGSNotificationIDs.getMap();
            if (map.size() == 0) {
                pw.print(prefix2);
                pw.println("(none)");
            }
            int size = map.size();
            for (int i = 0; i < size; i++) {
                int uid = map.keyAt(i);
                String uidString = UserHandle.formatUid(uid);
                ArrayMap<String, SparseBooleanArray> pkgs = map.valueAt(i);
                int numOfPkgs = pkgs.size();
                for (int j = 0; j < numOfPkgs; j++) {
                    String pkgName = pkgs.keyAt(j);
                    pw.print(prefix2);
                    pw.print(pkgName);
                    pw.print('/');
                    pw.print(uidString);
                    pw.print(" notification=");
                    pw.println(hasForegroundServiceNotificationsLocked(pkgName, uid));
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class PackageDurations extends BaseAppStateDurations<BaseAppStateTimeEvents.BaseTimeEvent> {
        static final int DEFAULT_INDEX = AppFGSTracker.foregroundServiceTypeToIndex(0);
        private int mForegroundServiceTypes;
        private boolean mIsLongRunning;
        private final AppFGSTracker mTracker;

        /* JADX DEBUG: Multi-variable search result rejected for r0v1, resolved type: java.util.LinkedList<E>[] */
        /* JADX WARN: Multi-variable type inference failed */
        PackageDurations(int uid, String packageName, BaseAppStateEvents.MaxTrackingDurationConfig maxTrackingDurationConfig, AppFGSTracker tracker) {
            super(uid, packageName, 9, AppFGSTracker.TAG, maxTrackingDurationConfig);
            this.mEvents[DEFAULT_INDEX] = new LinkedList();
            this.mTracker = tracker;
        }

        PackageDurations(PackageDurations other) {
            super(other);
            this.mIsLongRunning = other.mIsLongRunning;
            this.mForegroundServiceTypes = other.mForegroundServiceTypes;
            this.mTracker = other.mTracker;
        }

        void addEvent(boolean startFgs, long now) {
            addEvent(startFgs, (boolean) new BaseAppStateTimeEvents.BaseTimeEvent(now), DEFAULT_INDEX);
            if (!startFgs && !hasForegroundServices()) {
                this.mIsLongRunning = false;
            }
            if (!startFgs && this.mForegroundServiceTypes != 0) {
                for (int i = 1; i < this.mEvents.length; i++) {
                    if (this.mEvents[i] != null && isActive(i)) {
                        this.mEvents[i].add(new BaseAppStateTimeEvents.BaseTimeEvent(now));
                        notifyListenersOnStateChangeIfNecessary(false, now, AppFGSTracker.indexToForegroundServiceType(i));
                    }
                }
                this.mForegroundServiceTypes = 0;
            }
        }

        /* JADX DEBUG: Multi-variable search result rejected for r3v16, resolved type: java.util.LinkedList<E>[] */
        /* JADX WARN: Multi-variable type inference failed */
        void setForegroundServiceType(int serviceTypes, long now) {
            if (serviceTypes == this.mForegroundServiceTypes || !hasForegroundServices()) {
                return;
            }
            int changes = this.mForegroundServiceTypes ^ serviceTypes;
            int serviceType = Integer.highestOneBit(changes);
            while (serviceType != 0) {
                int i = AppFGSTracker.foregroundServiceTypeToIndex(serviceType);
                if (i < this.mEvents.length) {
                    if ((serviceTypes & serviceType) != 0) {
                        if (this.mEvents[i] == null) {
                            this.mEvents[i] = new LinkedList();
                        }
                        if (!isActive(i)) {
                            this.mEvents[i].add(new BaseAppStateTimeEvents.BaseTimeEvent(now));
                            notifyListenersOnStateChangeIfNecessary(true, now, serviceType);
                        }
                    } else if (this.mEvents[i] != null && isActive(i)) {
                        this.mEvents[i].add(new BaseAppStateTimeEvents.BaseTimeEvent(now));
                        notifyListenersOnStateChangeIfNecessary(false, now, serviceType);
                    }
                }
                changes &= ~serviceType;
                serviceType = Integer.highestOneBit(changes);
            }
            this.mForegroundServiceTypes = serviceTypes;
        }

        private void notifyListenersOnStateChangeIfNecessary(boolean start, long now, int serviceType) {
            int stateType;
            switch (serviceType) {
                case 2:
                    stateType = 2;
                    break;
                case 8:
                    stateType = 4;
                    break;
                default:
                    return;
            }
            this.mTracker.notifyListenersOnStateChange(this.mUid, this.mPackageName, start, now, stateType);
        }

        void setIsLongRunning(boolean isLongRunning) {
            this.mIsLongRunning = isLongRunning;
        }

        boolean isLongRunning() {
            return this.mIsLongRunning;
        }

        boolean hasForegroundServices() {
            return isActive(DEFAULT_INDEX);
        }

        @Override // com.android.server.am.BaseAppStateEvents
        String formatEventTypeLabel(int index) {
            if (index == DEFAULT_INDEX) {
                return "Overall foreground services: ";
            }
            return ServiceInfo.foregroundServiceTypeToLabel(AppFGSTracker.indexToForegroundServiceType(index)) + ": ";
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class NotificationListener extends NotificationListenerService {
        NotificationListener() {
        }

        @Override // android.service.notification.NotificationListenerService
        public void onNotificationPosted(StatusBarNotification sbn, NotificationListenerService.RankingMap map) {
            AppFGSTracker.this.mHandler.obtainMessage(5, sbn.getUid(), sbn.getId(), sbn.getPackageName()).sendToTarget();
        }

        @Override // android.service.notification.NotificationListenerService
        public void onNotificationRemoved(StatusBarNotification sbn, NotificationListenerService.RankingMap rankingMap, int reason) {
            AppFGSTracker.this.mHandler.obtainMessage(6, sbn.getUid(), sbn.getId(), sbn.getPackageName()).sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class AppFGSPolicy extends BaseAppStateEventsTracker.BaseAppStateEventsPolicy<AppFGSTracker> {
        static final long DEFAULT_BG_FGS_LOCATION_THRESHOLD = 14400000;
        static final long DEFAULT_BG_FGS_LONG_RUNNING_THRESHOLD = 72000000;
        static final long DEFAULT_BG_FGS_LONG_RUNNING_WINDOW = 86400000;
        static final long DEFAULT_BG_FGS_MEDIA_PLAYBACK_THRESHOLD = 14400000;
        static final boolean DEFAULT_BG_FGS_MONITOR_ENABLED = true;
        static final String KEY_BG_FGS_LOCATION_THRESHOLD = "bg_fgs_location_threshold";
        static final String KEY_BG_FGS_LONG_RUNNING_THRESHOLD = "bg_fgs_long_running_threshold";
        static final String KEY_BG_FGS_LONG_RUNNING_WINDOW = "bg_fgs_long_running_window";
        static final String KEY_BG_FGS_MEDIA_PLAYBACK_THRESHOLD = "bg_fgs_media_playback_threshold";
        static final String KEY_BG_FGS_MONITOR_ENABLED = "bg_fgs_monitor_enabled";
        private volatile long mBgFgsLocationThresholdMs;
        private volatile long mBgFgsLongRunningThresholdMs;
        private volatile long mBgFgsMediaPlaybackThresholdMs;

        AppFGSPolicy(BaseAppStateTracker.Injector injector, AppFGSTracker tracker) {
            super(injector, tracker, KEY_BG_FGS_MONITOR_ENABLED, true, KEY_BG_FGS_LONG_RUNNING_WINDOW, 86400000L);
            this.mBgFgsLongRunningThresholdMs = DEFAULT_BG_FGS_LONG_RUNNING_THRESHOLD;
            this.mBgFgsMediaPlaybackThresholdMs = 14400000L;
            this.mBgFgsLocationThresholdMs = 14400000L;
        }

        @Override // com.android.server.am.BaseAppStateEventsTracker.BaseAppStateEventsPolicy, com.android.server.am.BaseAppStatePolicy
        public void onSystemReady() {
            super.onSystemReady();
            updateBgFgsLongRunningThreshold();
            updateBgFgsMediaPlaybackThreshold();
            updateBgFgsLocationThreshold();
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // com.android.server.am.BaseAppStateEventsTracker.BaseAppStateEventsPolicy, com.android.server.am.BaseAppStatePolicy
        public void onPropertiesChanged(String name) {
            char c;
            switch (name.hashCode()) {
                case -2001687768:
                    if (name.equals(KEY_BG_FGS_LOCATION_THRESHOLD)) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 351955503:
                    if (name.equals(KEY_BG_FGS_LONG_RUNNING_THRESHOLD)) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 803245321:
                    if (name.equals(KEY_BG_FGS_MEDIA_PLAYBACK_THRESHOLD)) {
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
                    updateBgFgsLongRunningThreshold();
                    return;
                case 1:
                    updateBgFgsMediaPlaybackThreshold();
                    return;
                case 2:
                    updateBgFgsLocationThreshold();
                    return;
                default:
                    super.onPropertiesChanged(name);
                    return;
            }
        }

        @Override // com.android.server.am.BaseAppStatePolicy
        public void onTrackerEnabled(boolean enabled) {
            ((AppFGSTracker) this.mTracker).onBgFgsMonitorEnabled(enabled);
        }

        @Override // com.android.server.am.BaseAppStateEventsTracker.BaseAppStateEventsPolicy
        public void onMaxTrackingDurationChanged(long maxDuration) {
            ((AppFGSTracker) this.mTracker).onBgFgsLongRunningThresholdChanged();
        }

        private void updateBgFgsLongRunningThreshold() {
            long threshold = DeviceConfig.getLong("activity_manager", KEY_BG_FGS_LONG_RUNNING_THRESHOLD, (long) DEFAULT_BG_FGS_LONG_RUNNING_THRESHOLD);
            if (threshold != this.mBgFgsLongRunningThresholdMs) {
                this.mBgFgsLongRunningThresholdMs = threshold;
                ((AppFGSTracker) this.mTracker).onBgFgsLongRunningThresholdChanged();
            }
        }

        private void updateBgFgsMediaPlaybackThreshold() {
            this.mBgFgsMediaPlaybackThresholdMs = DeviceConfig.getLong("activity_manager", KEY_BG_FGS_MEDIA_PLAYBACK_THRESHOLD, 14400000L);
        }

        private void updateBgFgsLocationThreshold() {
            this.mBgFgsLocationThresholdMs = DeviceConfig.getLong("activity_manager", KEY_BG_FGS_LOCATION_THRESHOLD, 14400000L);
        }

        long getFgsLongRunningThreshold() {
            return this.mBgFgsLongRunningThresholdMs;
        }

        long getFgsLongRunningWindowSize() {
            return getMaxTrackingDuration();
        }

        long getFGSMediaPlaybackThreshold() {
            return this.mBgFgsMediaPlaybackThresholdMs;
        }

        long getLocationFGSThreshold() {
            return this.mBgFgsLocationThresholdMs;
        }

        void onLongRunningFgs(String packageName, int uid, int exemptReason) {
            if (exemptReason != -1) {
                return;
            }
            long now = SystemClock.elapsedRealtime();
            long window = getFgsLongRunningWindowSize();
            long since = Math.max(0L, now - window);
            if (shouldExemptMediaPlaybackFGS(packageName, uid, now, window) || shouldExemptLocationFGS(packageName, uid, now, since)) {
                return;
            }
            ((AppFGSTracker) this.mTracker).mAppRestrictionController.postLongRunningFgsIfNecessary(packageName, uid);
        }

        boolean shouldExemptMediaPlaybackFGS(String packageName, int uid, long now, long window) {
            long mediaPlaybackMs = ((AppFGSTracker) this.mTracker).mAppRestrictionController.getCompositeMediaPlaybackDurations(packageName, uid, now, window);
            if (mediaPlaybackMs > 0 && mediaPlaybackMs >= getFGSMediaPlaybackThreshold()) {
                return true;
            }
            return false;
        }

        boolean shouldExemptLocationFGS(String packageName, int uid, long now, long since) {
            long locationMs = ((AppFGSTracker) this.mTracker).mAppRestrictionController.getForegroundServiceTotalDurationsSince(packageName, uid, since, now, 8);
            if (locationMs > 0 && locationMs >= getLocationFGSThreshold()) {
                return true;
            }
            return false;
        }

        @Override // com.android.server.am.BaseAppStateEventsTracker.BaseAppStateEventsPolicy
        String getExemptionReasonString(String packageName, int uid, int reason) {
            if (reason != -1) {
                return super.getExemptionReasonString(packageName, uid, reason);
            }
            long now = SystemClock.elapsedRealtime();
            long window = getFgsLongRunningWindowSize();
            long since = Math.max(0L, now - getFgsLongRunningWindowSize());
            return "{mediaPlayback=" + shouldExemptMediaPlaybackFGS(packageName, uid, now, window) + ", location=" + shouldExemptLocationFGS(packageName, uid, now, since) + "}";
        }

        void onLongRunningFgsGone(String packageName, int uid) {
            ((AppFGSTracker) this.mTracker).mAppRestrictionController.cancelLongRunningFGSNotificationIfNecessary(packageName, uid);
        }

        @Override // com.android.server.am.BaseAppStateEventsTracker.BaseAppStateEventsPolicy, com.android.server.am.BaseAppStatePolicy
        void dump(PrintWriter pw, String prefix) {
            pw.print(prefix);
            pw.println("APP FOREGROUND SERVICE TRACKER POLICY SETTINGS:");
            String prefix2 = "  " + prefix;
            super.dump(pw, prefix2);
            if (isEnabled()) {
                pw.print(prefix2);
                pw.print(KEY_BG_FGS_LONG_RUNNING_THRESHOLD);
                pw.print('=');
                pw.println(this.mBgFgsLongRunningThresholdMs);
                pw.print(prefix2);
                pw.print(KEY_BG_FGS_MEDIA_PLAYBACK_THRESHOLD);
                pw.print('=');
                pw.println(this.mBgFgsMediaPlaybackThresholdMs);
                pw.print(prefix2);
                pw.print(KEY_BG_FGS_LOCATION_THRESHOLD);
                pw.print('=');
                pw.println(this.mBgFgsLocationThresholdMs);
            }
        }
    }
}
