package com.android.server.am;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.DeviceConfig;
import android.util.ArrayMap;
import android.util.SparseArray;
import android.util.TimeUtils;
import com.android.internal.app.ProcessMap;
import com.android.server.am.BaseAppStateEvents;
import com.android.server.am.BaseAppStateEventsTracker;
import com.android.server.am.BaseAppStateTimeSlotEventsTracker.BaseAppStateTimeSlotEventsPolicy;
import com.android.server.am.BaseAppStateTimeSlotEventsTracker.SimpleAppStateTimeslotEvents;
import com.android.server.am.BaseAppStateTracker;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public abstract class BaseAppStateTimeSlotEventsTracker<T extends BaseAppStateTimeSlotEventsPolicy, U extends SimpleAppStateTimeslotEvents> extends BaseAppStateEventsTracker<T, U> {
    static final boolean DEBUG_APP_STATE_TIME_SLOT_EVENT_TRACKER = false;
    static final String TAG = "BaseAppStateTimeSlotEventsTracker";
    private H mHandler;
    private final ArrayMap<U, Integer> mTmpPkgs;

    /* JADX INFO: Access modifiers changed from: package-private */
    public BaseAppStateTimeSlotEventsTracker(Context context, AppRestrictionController controller, Constructor<? extends BaseAppStateTracker.Injector<T>> injector, Object outerContext) {
        super(context, controller, injector, outerContext);
        this.mTmpPkgs = new ArrayMap<>();
        this.mHandler = new H(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onNewEvent(String packageName, int uid) {
        this.mHandler.obtainMessage(0, uid, 0, packageName).sendToTarget();
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:20:0x005d -> B:21:0x005e). Please submit an issue!!! */
    void handleNewEvent(String packageName, int uid) {
        if (((BaseAppStateTimeSlotEventsPolicy) this.mInjector.getPolicy()).shouldExempt(packageName, uid) != -1) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        synchronized (this.mLock) {
            try {
                SimpleAppStateTimeslotEvents simpleAppStateTimeslotEvents = (SimpleAppStateTimeslotEvents) this.mPkgEvents.get(uid, packageName);
                if (simpleAppStateTimeslotEvents == null) {
                    simpleAppStateTimeslotEvents = (SimpleAppStateTimeslotEvents) createAppStateEvents(uid, packageName);
                    this.mPkgEvents.put(uid, packageName, simpleAppStateTimeslotEvents);
                }
                simpleAppStateTimeslotEvents.addEvent(now, 0);
                int totalEvents = simpleAppStateTimeslotEvents.getTotalEvents(now, 0);
                boolean notify = totalEvents >= ((BaseAppStateTimeSlotEventsPolicy) this.mInjector.getPolicy()).getNumOfEventsThreshold();
                try {
                    if (notify) {
                        ((BaseAppStateTimeSlotEventsPolicy) this.mInjector.getPolicy()).onExcessiveEvents(packageName, uid, totalEvents, now);
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    void onMonitorEnabled(boolean enabled) {
        if (!enabled) {
            synchronized (this.mLock) {
                this.mPkgEvents.clear();
            }
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r9v2, resolved type: android.util.ArrayMap<U extends com.android.server.am.BaseAppStateTimeSlotEventsTracker$SimpleAppStateTimeslotEvents, java.lang.Integer> */
    /* JADX WARN: Multi-variable type inference failed */
    void onNumOfEventsThresholdChanged(int threshold) {
        long now = SystemClock.elapsedRealtime();
        synchronized (this.mLock) {
            SparseArray map = this.mPkgEvents.getMap();
            for (int i = map.size() - 1; i >= 0; i--) {
                ArrayMap<String, U> pkgs = (ArrayMap) map.valueAt(i);
                for (int j = pkgs.size() - 1; j >= 0; j--) {
                    U pkg = pkgs.valueAt(j);
                    int totalEvents = pkg.getTotalEvents(now, 0);
                    if (totalEvents >= threshold) {
                        this.mTmpPkgs.put(pkg, Integer.valueOf(totalEvents));
                    }
                }
            }
        }
        for (int i2 = this.mTmpPkgs.size() - 1; i2 >= 0; i2--) {
            U pkg2 = this.mTmpPkgs.keyAt(i2);
            ((BaseAppStateTimeSlotEventsPolicy) this.mInjector.getPolicy()).onExcessiveEvents(pkg2.mPackageName, pkg2.mUid, this.mTmpPkgs.valueAt(i2).intValue(), now);
        }
        this.mTmpPkgs.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getTotalEventsLocked(int uid, long now) {
        SimpleAppStateTimeslotEvents simpleAppStateTimeslotEvents = (SimpleAppStateTimeslotEvents) getUidEventsLocked(uid);
        if (simpleAppStateTimeslotEvents == null) {
            return 0;
        }
        return simpleAppStateTimeslotEvents.getTotalEvents(now, 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void trimEvents() {
        long now = SystemClock.elapsedRealtime();
        trim(Math.max(0L, now - ((BaseAppStateTimeSlotEventsPolicy) this.mInjector.getPolicy()).getMaxTrackingDuration()));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public void onUserInteractionStarted(String packageName, int uid) {
        ((BaseAppStateTimeSlotEventsPolicy) this.mInjector.getPolicy()).onUserInteractionStarted(packageName, uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class H extends Handler {
        static final int MSG_NEW_EVENT = 0;
        final BaseAppStateTimeSlotEventsTracker mTracker;

        H(BaseAppStateTimeSlotEventsTracker tracker) {
            super(tracker.mBgHandler.getLooper());
            this.mTracker = tracker;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    this.mTracker.handleNewEvent((String) msg.obj, msg.arg1);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class BaseAppStateTimeSlotEventsPolicy<E extends BaseAppStateTimeSlotEventsTracker> extends BaseAppStateEventsTracker.BaseAppStateEventsPolicy<E> {
        final int mDefaultNumOfEventsThreshold;
        private final ProcessMap<Long> mExcessiveEventPkgs;
        final String mKeyNumOfEventsThreshold;
        private final Object mLock;
        volatile int mNumOfEventsThreshold;
        long mTimeSlotSize;

        /* JADX INFO: Access modifiers changed from: package-private */
        public BaseAppStateTimeSlotEventsPolicy(BaseAppStateTracker.Injector injector, E tracker, String keyTrackerEnabled, boolean defaultTrackerEnabled, String keyMaxTrackingDuration, long defaultMaxTrackingDuration, String keyNumOfEventsThreshold, int defaultNumOfEventsThreshold) {
            super(injector, tracker, keyTrackerEnabled, defaultTrackerEnabled, keyMaxTrackingDuration, defaultMaxTrackingDuration);
            this.mExcessiveEventPkgs = new ProcessMap<>();
            this.mTimeSlotSize = 900000L;
            this.mKeyNumOfEventsThreshold = keyNumOfEventsThreshold;
            this.mDefaultNumOfEventsThreshold = defaultNumOfEventsThreshold;
            this.mLock = tracker.mLock;
        }

        @Override // com.android.server.am.BaseAppStateEventsTracker.BaseAppStateEventsPolicy, com.android.server.am.BaseAppStatePolicy
        public void onSystemReady() {
            super.onSystemReady();
            updateNumOfEventsThreshold();
        }

        @Override // com.android.server.am.BaseAppStateEventsTracker.BaseAppStateEventsPolicy, com.android.server.am.BaseAppStatePolicy
        public void onPropertiesChanged(String name) {
            if (this.mKeyNumOfEventsThreshold.equals(name)) {
                updateNumOfEventsThreshold();
            } else {
                super.onPropertiesChanged(name);
            }
        }

        @Override // com.android.server.am.BaseAppStatePolicy
        public void onTrackerEnabled(boolean enabled) {
            ((BaseAppStateTimeSlotEventsTracker) this.mTracker).onMonitorEnabled(enabled);
        }

        @Override // com.android.server.am.BaseAppStateEventsTracker.BaseAppStateEventsPolicy
        public void onMaxTrackingDurationChanged(long maxDuration) {
            Handler handler = ((BaseAppStateTimeSlotEventsTracker) this.mTracker).mBgHandler;
            final BaseAppStateTimeSlotEventsTracker baseAppStateTimeSlotEventsTracker = (BaseAppStateTimeSlotEventsTracker) this.mTracker;
            Objects.requireNonNull(baseAppStateTimeSlotEventsTracker);
            handler.post(new Runnable() { // from class: com.android.server.am.BaseAppStateTimeSlotEventsTracker$BaseAppStateTimeSlotEventsPolicy$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    BaseAppStateTimeSlotEventsTracker.this.trimEvents();
                }
            });
        }

        private void updateNumOfEventsThreshold() {
            int threshold = DeviceConfig.getInt("activity_manager", this.mKeyNumOfEventsThreshold, this.mDefaultNumOfEventsThreshold);
            if (threshold != this.mNumOfEventsThreshold) {
                this.mNumOfEventsThreshold = threshold;
                ((BaseAppStateTimeSlotEventsTracker) this.mTracker).onNumOfEventsThresholdChanged(threshold);
            }
        }

        int getNumOfEventsThreshold() {
            return this.mNumOfEventsThreshold;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public long getTimeSlotSize() {
            return this.mTimeSlotSize;
        }

        void setTimeSlotSize(long size) {
            this.mTimeSlotSize = size;
        }

        String getEventName() {
            return "event";
        }

        void onExcessiveEvents(String packageName, int uid, int numOfEvents, long now) {
            boolean notifyController = false;
            synchronized (this.mLock) {
                Long ts = (Long) this.mExcessiveEventPkgs.get(packageName, uid);
                if (ts == null) {
                    this.mExcessiveEventPkgs.put(packageName, uid, Long.valueOf(now));
                    notifyController = true;
                }
            }
            if (notifyController) {
                ((BaseAppStateTimeSlotEventsTracker) this.mTracker).mAppRestrictionController.refreshAppRestrictionLevelForUid(uid, 1536, 2, true);
            }
        }

        int shouldExempt(String packageName, int uid) {
            if (((BaseAppStateTimeSlotEventsTracker) this.mTracker).isUidOnTop(uid)) {
                return 12;
            }
            if (((BaseAppStateTimeSlotEventsTracker) this.mTracker).mAppRestrictionController.hasForegroundServices(packageName, uid)) {
                return 14;
            }
            int reason = shouldExemptUid(uid);
            if (reason == -1) {
                return -1;
            }
            return reason;
        }

        /* JADX WARN: Removed duplicated region for block: B:13:0x0022 A[Catch: all -> 0x002b, DONT_GENERATE, TryCatch #0 {, blocks: (B:4:0x0003, B:6:0x000f, B:13:0x0022, B:16:0x0026, B:19:0x0029), top: B:24:0x0003 }] */
        /* JADX WARN: Removed duplicated region for block: B:15:0x0024  */
        @Override // com.android.server.am.BaseAppStatePolicy
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public int getProposedRestrictionLevel(String packageName, int uid, int maxLevel) {
            int level;
            synchronized (this.mLock) {
                if (this.mExcessiveEventPkgs.get(packageName, uid) != null && ((BaseAppStateTimeSlotEventsTracker) this.mTracker).mAppRestrictionController.isAutoRestrictAbusiveAppEnabled()) {
                    level = 40;
                    return maxLevel <= 40 ? level : maxLevel == 40 ? 30 : 0;
                }
                level = 30;
                if (maxLevel <= 40) {
                }
            }
        }

        void onUserInteractionStarted(String packageName, int uid) {
            synchronized (this.mLock) {
                boolean z = this.mExcessiveEventPkgs.remove(packageName, uid) != null;
            }
            ((BaseAppStateTimeSlotEventsTracker) this.mTracker).mAppRestrictionController.refreshAppRestrictionLevelForUid(uid, 768, 3, true);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.android.server.am.BaseAppStateEventsTracker.BaseAppStateEventsPolicy, com.android.server.am.BaseAppStatePolicy
        public void dump(PrintWriter pw, String prefix) {
            super.dump(pw, prefix);
            if (isEnabled()) {
                pw.print(prefix);
                pw.print(this.mKeyNumOfEventsThreshold);
                pw.print('=');
                pw.println(this.mDefaultNumOfEventsThreshold);
            }
            pw.print(prefix);
            pw.print("event_time_slot_size=");
            pw.println(getTimeSlotSize());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class SimpleAppStateTimeslotEvents extends BaseAppStateTimeSlotEvents {
        static final int DEFAULT_INDEX = 0;
        static final long DEFAULT_TIME_SLOT_SIZE = 900000;
        static final long DEFAULT_TIME_SLOT_SIZE_DEBUG = 60000;

        /* JADX INFO: Access modifiers changed from: package-private */
        public SimpleAppStateTimeslotEvents(int uid, String packageName, long timeslotSize, String tag, BaseAppStateEvents.MaxTrackingDurationConfig maxTrackingDurationConfig) {
            super(uid, packageName, 1, timeslotSize, tag, maxTrackingDurationConfig);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public SimpleAppStateTimeslotEvents(SimpleAppStateTimeslotEvents other) {
            super(other);
        }

        @Override // com.android.server.am.BaseAppStateEvents
        String formatEventTypeLabel(int index) {
            return "";
        }

        @Override // com.android.server.am.BaseAppStateEvents
        String formatEventSummary(long now, int index) {
            if (this.mEvents[0] == null || this.mEvents[0].size() == 0) {
                return "(none)";
            }
            int total = getTotalEvents(now, 0);
            return "total=" + total + ", latest=" + getTotalEventsSince(this.mCurSlotStartTime[0], now, 0) + "(slot=" + TimeUtils.formatTime(this.mCurSlotStartTime[0], now) + ")";
        }
    }
}
