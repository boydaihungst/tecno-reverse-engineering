package com.android.server.am;

import android.content.Context;
import android.os.PowerExemptionManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseArray;
import com.android.server.am.BaseAppStateEvents;
import com.android.server.am.BaseAppStateEventsTracker.BaseAppStateEventsPolicy;
import com.android.server.am.BaseAppStateTracker;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.LinkedList;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public abstract class BaseAppStateEventsTracker<T extends BaseAppStateEventsPolicy, U extends BaseAppStateEvents> extends BaseAppStateTracker<T> implements BaseAppStateEvents.Factory<U> {
    static final boolean DEBUG_BASE_APP_STATE_EVENTS_TRACKER = false;
    final UidProcessMap<U> mPkgEvents;
    final ArraySet<Integer> mTopUids;

    /* JADX INFO: Access modifiers changed from: package-private */
    public BaseAppStateEventsTracker(Context context, AppRestrictionController controller, Constructor<? extends BaseAppStateTracker.Injector<T>> injector, Object outerContext) {
        super(context, controller, injector, outerContext);
        this.mPkgEvents = new UidProcessMap<>();
        this.mTopUids = new ArraySet<>();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reset() {
        synchronized (this.mLock) {
            this.mPkgEvents.clear();
            this.mTopUids.clear();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public U getUidEventsLocked(int uid) {
        U events = null;
        ArrayMap<String, U> map = this.mPkgEvents.getMap().get(uid);
        if (map == null) {
            return null;
        }
        for (int i = map.size() - 1; i >= 0; i--) {
            U event = map.valueAt(i);
            if (event != null) {
                if (events == null) {
                    events = (U) createAppStateEvents(uid, event.mPackageName);
                }
                events.add(event);
            }
        }
        return events;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void trim(long earliest) {
        synchronized (this.mLock) {
            trimLocked(earliest);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void trimLocked(long earliest) {
        SparseArray<ArrayMap<String, U>> map = this.mPkgEvents.getMap();
        for (int i = map.size() - 1; i >= 0; i--) {
            ArrayMap<String, U> val = map.valueAt(i);
            for (int j = val.size() - 1; j >= 0; j--) {
                U v = val.valueAt(j);
                v.trim(earliest);
                if (v.isEmpty()) {
                    val.removeAt(j);
                }
            }
            int j2 = val.size();
            if (j2 == 0) {
                map.removeAt(i);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isUidOnTop(int uid) {
        boolean contains;
        synchronized (this.mLock) {
            contains = this.mTopUids.contains(Integer.valueOf(uid));
        }
        return contains;
    }

    void onUntrackingUidLocked(int uid) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public void onUidProcStateChanged(int uid, int procState) {
        synchronized (this.mLock) {
            if (this.mPkgEvents.getMap().indexOfKey(uid) < 0) {
                return;
            }
            onUidProcStateChangedUncheckedLocked(uid, procState);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUidProcStateChangedUncheckedLocked(int uid, int procState) {
        if (procState < 4) {
            this.mTopUids.add(Integer.valueOf(uid));
        } else {
            this.mTopUids.remove(Integer.valueOf(uid));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public void onUidGone(int uid) {
        synchronized (this.mLock) {
            this.mTopUids.remove(Integer.valueOf(uid));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public void onUidRemoved(int uid) {
        synchronized (this.mLock) {
            this.mPkgEvents.getMap().remove(uid);
            onUntrackingUidLocked(uid);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public void onUserRemoved(int userId) {
        synchronized (this.mLock) {
            SparseArray<ArrayMap<String, U>> map = this.mPkgEvents.getMap();
            for (int i = map.size() - 1; i >= 0; i--) {
                int uid = map.keyAt(i);
                if (UserHandle.getUserId(uid) == userId) {
                    map.removeAt(i);
                    onUntrackingUidLocked(uid);
                }
            }
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r16v0, resolved type: com.android.server.am.BaseAppStateEventsTracker<T extends com.android.server.am.BaseAppStateEventsTracker$BaseAppStateEventsPolicy, U extends com.android.server.am.BaseAppStateEvents> */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.server.am.BaseAppStateTracker
    public void dump(PrintWriter pw, String prefix) {
        Object obj;
        BaseAppStateEventsPolicy baseAppStateEventsPolicy = (BaseAppStateEventsPolicy) this.mInjector.getPolicy();
        Object obj2 = this.mLock;
        synchronized (obj2) {
            try {
                long now = SystemClock.elapsedRealtime();
                SparseArray<ArrayMap<String, U>> map = this.mPkgEvents.getMap();
                int i = map.size() - 1;
                while (i >= 0) {
                    try {
                        int uid = map.keyAt(i);
                        ArrayMap<String, U> val = map.valueAt(i);
                        int j = val.size() - 1;
                        while (j >= 0) {
                            String packageName = val.keyAt(j);
                            U events = val.valueAt(j);
                            dumpEventHeaderLocked(pw, prefix, packageName, uid, events, baseAppStateEventsPolicy);
                            int j2 = j;
                            ArrayMap<String, U> val2 = val;
                            int i2 = i;
                            obj = obj2;
                            try {
                                dumpEventLocked(pw, prefix, events, now);
                                j = j2 - 1;
                                val = val2;
                                i = i2;
                                obj2 = obj;
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
                        i--;
                    } catch (Throwable th3) {
                        th = th3;
                        obj = obj2;
                        while (true) {
                            break;
                            break;
                        }
                        throw th;
                    }
                }
                obj = obj2;
                dumpOthers(pw, prefix);
                baseAppStateEventsPolicy.dump(pw, prefix);
            } catch (Throwable th4) {
                th = th4;
                obj = obj2;
            }
        }
    }

    void dumpOthers(PrintWriter pw, String prefix) {
    }

    void dumpEventHeaderLocked(PrintWriter pw, String prefix, String packageName, int uid, U events, T policy) {
        pw.print(prefix);
        pw.print("* ");
        pw.print(packageName);
        pw.print('/');
        pw.print(UserHandle.formatUid(uid));
        pw.print(" exemption=");
        pw.println(policy.getExemptionReasonString(packageName, uid, events.mExemptReason));
    }

    void dumpEventLocked(PrintWriter pw, String prefix, U events, long now) {
        events.dump(pw, "  " + prefix, now);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static abstract class BaseAppStateEventsPolicy<V extends BaseAppStateEventsTracker> extends BaseAppStatePolicy<V> implements BaseAppStateEvents.MaxTrackingDurationConfig {
        final long mDefaultMaxTrackingDuration;
        final String mKeyMaxTrackingDuration;
        volatile long mMaxTrackingDuration;

        public abstract void onMaxTrackingDurationChanged(long j);

        /* JADX INFO: Access modifiers changed from: package-private */
        public BaseAppStateEventsPolicy(BaseAppStateTracker.Injector<?> injector, V tracker, String keyTrackerEnabled, boolean defaultTrackerEnabled, String keyMaxTrackingDuration, long defaultMaxTrackingDuration) {
            super(injector, tracker, keyTrackerEnabled, defaultTrackerEnabled);
            this.mKeyMaxTrackingDuration = keyMaxTrackingDuration;
            this.mDefaultMaxTrackingDuration = defaultMaxTrackingDuration;
        }

        @Override // com.android.server.am.BaseAppStatePolicy
        public void onPropertiesChanged(String name) {
            if (this.mKeyMaxTrackingDuration.equals(name)) {
                updateMaxTrackingDuration();
            } else {
                super.onPropertiesChanged(name);
            }
        }

        @Override // com.android.server.am.BaseAppStatePolicy
        public void onSystemReady() {
            super.onSystemReady();
            updateMaxTrackingDuration();
        }

        void updateMaxTrackingDuration() {
            long max = DeviceConfig.getLong("activity_manager", this.mKeyMaxTrackingDuration, this.mDefaultMaxTrackingDuration);
            if (max != this.mMaxTrackingDuration) {
                this.mMaxTrackingDuration = max;
                onMaxTrackingDurationChanged(max);
            }
        }

        @Override // com.android.server.am.BaseAppStateEvents.MaxTrackingDurationConfig
        public long getMaxTrackingDuration() {
            return this.mMaxTrackingDuration;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public String getExemptionReasonString(String packageName, int uid, int reason) {
            return PowerExemptionManager.reasonCodeToString(reason);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.android.server.am.BaseAppStatePolicy
        public void dump(PrintWriter pw, String prefix) {
            super.dump(pw, prefix);
            if (isEnabled()) {
                pw.print(prefix);
                pw.print(this.mKeyMaxTrackingDuration);
                pw.print('=');
                pw.println(this.mMaxTrackingDuration);
            }
        }
    }

    /* loaded from: classes.dex */
    static class SimplePackageEvents extends BaseAppStateTimeEvents {
        static final int DEFAULT_INDEX = 0;

        /* JADX DEBUG: Multi-variable search result rejected for r0v1, resolved type: java.util.LinkedList<E>[] */
        /* JADX WARN: Multi-variable type inference failed */
        SimplePackageEvents(int uid, String packageName, BaseAppStateEvents.MaxTrackingDurationConfig maxTrackingDurationConfig) {
            super(uid, packageName, 1, "ActivityManager", maxTrackingDurationConfig);
            this.mEvents[0] = new LinkedList();
        }

        long getTotalEvents(long now) {
            return getTotalEvents(now, 0);
        }

        long getTotalEventsSince(long since, long now) {
            return getTotalEventsSince(since, now, 0);
        }

        @Override // com.android.server.am.BaseAppStateEvents
        String formatEventTypeLabel(int index) {
            return "";
        }
    }
}
