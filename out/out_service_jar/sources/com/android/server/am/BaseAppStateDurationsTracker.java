package com.android.server.am;

import android.content.Context;
import android.os.SystemClock;
import android.util.SparseArray;
import com.android.server.am.BaseAppStateDurations;
import com.android.server.am.BaseAppStateEvents;
import com.android.server.am.BaseAppStateEventsTracker;
import com.android.server.am.BaseAppStateEventsTracker.BaseAppStateEventsPolicy;
import com.android.server.am.BaseAppStateTimeEvents;
import com.android.server.am.BaseAppStateTracker;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.LinkedList;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public abstract class BaseAppStateDurationsTracker<T extends BaseAppStateEventsTracker.BaseAppStateEventsPolicy, U extends BaseAppStateDurations> extends BaseAppStateEventsTracker<T, U> {
    static final boolean DEBUG_BASE_APP_STATE_DURATION_TRACKER = false;
    final SparseArray<UidStateDurations> mUidStateDurations;

    /* JADX DEBUG: Multi-variable search result rejected for r6v0, resolved type: com.android.server.am.BaseAppStateDurationsTracker<T extends com.android.server.am.BaseAppStateEventsTracker$BaseAppStateEventsPolicy, U extends com.android.server.am.BaseAppStateDurations> */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.server.am.BaseAppStateEventsTracker
    /* bridge */ /* synthetic */ void dumpEventLocked(PrintWriter printWriter, String str, BaseAppStateEvents baseAppStateEvents, long j) {
        dumpEventLocked(printWriter, str, (String) ((BaseAppStateDurations) baseAppStateEvents), j);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BaseAppStateDurationsTracker(Context context, AppRestrictionController controller, Constructor<? extends BaseAppStateTracker.Injector<T>> injector, Object outerContext) {
        super(context, controller, injector, outerContext);
        this.mUidStateDurations = new SparseArray<>();
    }

    @Override // com.android.server.am.BaseAppStateEventsTracker, com.android.server.am.BaseAppStateTracker
    void onUidProcStateChanged(int uid, int procState) {
        synchronized (this.mLock) {
            if (this.mPkgEvents.getMap().indexOfKey(uid) < 0) {
                return;
            }
            onUidProcStateChangedUncheckedLocked(uid, procState);
            UidStateDurations uidStateDurations = this.mUidStateDurations.get(uid);
            if (uidStateDurations == null) {
                uidStateDurations = new UidStateDurations(uid, (BaseAppStateEvents.MaxTrackingDurationConfig) this.mInjector.getPolicy());
                this.mUidStateDurations.put(uid, uidStateDurations);
            }
            uidStateDurations.addEvent(procState < 4, SystemClock.elapsedRealtime());
        }
    }

    @Override // com.android.server.am.BaseAppStateEventsTracker, com.android.server.am.BaseAppStateTracker
    void onUidGone(int uid) {
        onUidProcStateChanged(uid, 20);
    }

    @Override // com.android.server.am.BaseAppStateEventsTracker
    void trimLocked(long earliest) {
        super.trimLocked(earliest);
        for (int i = this.mUidStateDurations.size() - 1; i >= 0; i--) {
            UidStateDurations u = this.mUidStateDurations.valueAt(i);
            u.trim(earliest);
            if (u.isEmpty()) {
                this.mUidStateDurations.removeAt(i);
            }
        }
    }

    @Override // com.android.server.am.BaseAppStateEventsTracker
    void onUntrackingUidLocked(int uid) {
        this.mUidStateDurations.remove(uid);
    }

    long getTotalDurations(String packageName, int uid, long now, int index, boolean bgOnly) {
        UidStateDurations uidDurations;
        synchronized (this.mLock) {
            BaseAppStateDurations baseAppStateDurations = (BaseAppStateDurations) this.mPkgEvents.get(uid, packageName);
            if (baseAppStateDurations == null) {
                return 0L;
            }
            if (bgOnly && (uidDurations = this.mUidStateDurations.get(uid)) != null && !uidDurations.isEmpty()) {
                BaseAppStateDurations baseAppStateDurations2 = (BaseAppStateDurations) createAppStateEvents(baseAppStateDurations);
                baseAppStateDurations2.subtract(uidDurations, index, 0);
                return baseAppStateDurations2.getTotalDurations(now, index);
            }
            return baseAppStateDurations.getTotalDurations(now, index);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getTotalDurations(String packageName, int uid, long now, int index) {
        return getTotalDurations(packageName, uid, now, index, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getTotalDurations(String packageName, int uid, long now) {
        return getTotalDurations(packageName, uid, now, 0);
    }

    long getTotalDurations(int uid, long now, int index, boolean bgOnly) {
        UidStateDurations uidDurations;
        synchronized (this.mLock) {
            BaseAppStateDurations baseAppStateDurations = (BaseAppStateDurations) getUidEventsLocked(uid);
            if (baseAppStateDurations == null) {
                return 0L;
            }
            if (bgOnly && (uidDurations = this.mUidStateDurations.get(uid)) != null && !uidDurations.isEmpty()) {
                baseAppStateDurations.subtract(uidDurations, index, 0);
            }
            return baseAppStateDurations.getTotalDurations(now, index);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getTotalDurations(int uid, long now, int index) {
        return getTotalDurations(uid, now, index, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getTotalDurations(int uid, long now) {
        return getTotalDurations(uid, now, 0);
    }

    long getTotalDurationsSince(String packageName, int uid, long since, long now, int index, boolean bgOnly) {
        synchronized (this.mLock) {
            try {
                try {
                    try {
                        BaseAppStateDurations baseAppStateDurations = (BaseAppStateDurations) this.mPkgEvents.get(uid, packageName);
                        if (baseAppStateDurations == null) {
                            return 0L;
                        }
                        if (bgOnly) {
                            UidStateDurations uidDurations = this.mUidStateDurations.get(uid);
                            if (uidDurations != null && !uidDurations.isEmpty()) {
                                BaseAppStateDurations baseAppStateDurations2 = (BaseAppStateDurations) createAppStateEvents(baseAppStateDurations);
                                baseAppStateDurations2.subtract(uidDurations, index, 0);
                                return baseAppStateDurations2.getTotalDurationsSince(since, now, index);
                            }
                        }
                        return baseAppStateDurations.getTotalDurationsSince(since, now, index);
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

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getTotalDurationsSince(String packageName, int uid, long since, long now, int index) {
        return getTotalDurationsSince(packageName, uid, since, now, index, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getTotalDurationsSince(String packageName, int uid, long since, long now) {
        return getTotalDurationsSince(packageName, uid, since, now, 0);
    }

    long getTotalDurationsSince(int uid, long since, long now, int index, boolean bgOnly) {
        UidStateDurations uidDurations;
        synchronized (this.mLock) {
            BaseAppStateDurations baseAppStateDurations = (BaseAppStateDurations) getUidEventsLocked(uid);
            if (baseAppStateDurations == null) {
                return 0L;
            }
            if (bgOnly && (uidDurations = this.mUidStateDurations.get(uid)) != null && !uidDurations.isEmpty()) {
                baseAppStateDurations.subtract(uidDurations, index, 0);
            }
            return baseAppStateDurations.getTotalDurationsSince(since, now, index);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getTotalDurationsSince(int uid, long since, long now, int index) {
        return getTotalDurationsSince(uid, since, now, index, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getTotalDurationsSince(int uid, long since, long now) {
        return getTotalDurationsSince(uid, since, now, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateEventsTracker
    public void reset() {
        super.reset();
        synchronized (this.mLock) {
            this.mUidStateDurations.clear();
        }
    }

    void dumpEventLocked(PrintWriter pw, String prefix, U events, long now) {
        UidStateDurations uidDurations = this.mUidStateDurations.get(events.mUid);
        pw.print("  " + prefix);
        pw.println("(bg only)");
        if (uidDurations == null || uidDurations.isEmpty()) {
            events.dump(pw, "    " + prefix, now);
            return;
        }
        BaseAppStateDurations baseAppStateDurations = (BaseAppStateDurations) createAppStateEvents(events);
        baseAppStateDurations.subtract(uidDurations, 0);
        baseAppStateDurations.dump(pw, "    " + prefix, now);
        pw.print("  " + prefix);
        pw.println("(fg + bg)");
        events.dump(pw, "    " + prefix, now);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class SimplePackageDurations extends BaseAppStateDurations<BaseAppStateTimeEvents.BaseTimeEvent> {
        static final int DEFAULT_INDEX = 0;

        /* JADX DEBUG: Multi-variable search result rejected for r0v1, resolved type: java.util.LinkedList<E>[] */
        /* JADX INFO: Access modifiers changed from: package-private */
        /* JADX WARN: Multi-variable type inference failed */
        public SimplePackageDurations(int uid, String packageName, BaseAppStateEvents.MaxTrackingDurationConfig maxTrackingDurationConfig) {
            super(uid, packageName, 1, "ActivityManager", maxTrackingDurationConfig);
            this.mEvents[0] = new LinkedList();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public SimplePackageDurations(SimplePackageDurations other) {
            super(other);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void addEvent(boolean active, long now) {
            addEvent(active, (boolean) new BaseAppStateTimeEvents.BaseTimeEvent(now), 0);
        }

        long getTotalDurations(long now) {
            return getTotalDurations(now, 0);
        }

        long getTotalDurationsSince(long since, long now) {
            return getTotalDurationsSince(since, now, 0);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean isActive() {
            return isActive(0);
        }

        @Override // com.android.server.am.BaseAppStateEvents
        String formatEventTypeLabel(int index) {
            return "";
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class UidStateDurations extends SimplePackageDurations {
        UidStateDurations(int uid, BaseAppStateEvents.MaxTrackingDurationConfig maxTrackingDurationConfig) {
            super(uid, "", maxTrackingDurationConfig);
        }

        UidStateDurations(UidStateDurations other) {
            super(other);
        }
    }
}
