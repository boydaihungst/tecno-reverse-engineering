package com.android.server.am;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.Pair;
import android.util.SparseArray;
import com.android.server.am.AppBatteryTracker;
import com.android.server.am.BaseAppStateEvents;
import com.android.server.am.BaseAppStateEventsTracker;
import com.android.server.am.BaseAppStateTimeEvents;
import com.android.server.am.BaseAppStateTracker;
import com.android.server.job.controllers.JobStatus;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class AppBatteryExemptionTracker extends BaseAppStateDurationsTracker<AppBatteryExemptionPolicy, UidBatteryStates> implements BaseAppStateEvents.Factory<UidBatteryStates>, BaseAppStateTracker.StateListener {
    private static final boolean DEBUG_BACKGROUND_BATTERY_EXEMPTION_TRACKER = false;
    static final String DEFAULT_NAME = "";
    private static final String TAG = "ActivityManager";
    private UidProcessMap<Integer> mUidPackageStates;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppBatteryExemptionTracker(Context context, AppRestrictionController controller) {
        this(context, controller, null, null);
    }

    AppBatteryExemptionTracker(Context context, AppRestrictionController controller, Constructor<? extends BaseAppStateTracker.Injector<AppBatteryExemptionPolicy>> injector, Object outerContext) {
        super(context, controller, injector, outerContext);
        this.mUidPackageStates = new UidProcessMap<>();
        this.mInjector.setPolicy(new AppBatteryExemptionPolicy(this.mInjector, this));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public int getType() {
        return 2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateTracker
    public void onSystemReady() {
        super.onSystemReady();
        this.mAppRestrictionController.forEachTracker(new Consumer() { // from class: com.android.server.am.AppBatteryExemptionTracker$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                AppBatteryExemptionTracker.this.m1101xf6eced9c((BaseAppStateTracker) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSystemReady$0$com-android-server-am-AppBatteryExemptionTracker  reason: not valid java name */
    public /* synthetic */ void m1101xf6eced9c(BaseAppStateTracker tracker) {
        tracker.registerStateListener(this);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.android.server.am.BaseAppStateEvents.Factory
    public UidBatteryStates createAppStateEvents(int uid, String packageName) {
        return new UidBatteryStates(uid, TAG, (BaseAppStateEvents.MaxTrackingDurationConfig) this.mInjector.getPolicy());
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.am.BaseAppStateEvents.Factory
    public UidBatteryStates createAppStateEvents(UidBatteryStates other) {
        return new UidBatteryStates(other);
    }

    @Override // com.android.server.am.BaseAppStateTracker.StateListener
    public void onStateChange(int uid, String packageName, boolean start, long now, int stateType) {
        ArrayMap<String, Integer> pkgsStates;
        int indexOfPkg;
        boolean addEvent;
        UidBatteryStates pkg;
        if (!((AppBatteryExemptionPolicy) this.mInjector.getPolicy()).isEnabled()) {
            return;
        }
        AppBatteryTracker.ImmutableBatteryUsage batteryUsage = this.mAppRestrictionController.getUidBatteryUsage(uid);
        int stateTypeIndex = stateTypeToIndex(stateType);
        synchronized (this.mLock) {
            SparseArray<ArrayMap<String, Integer>> map = this.mUidPackageStates.getMap();
            ArrayMap<String, Integer> pkgsStates2 = map.get(uid);
            if (pkgsStates2 != null) {
                pkgsStates = pkgsStates2;
            } else {
                ArrayMap<String, Integer> pkgsStates3 = new ArrayMap<>();
                map.put(uid, pkgsStates3);
                pkgsStates = pkgsStates3;
            }
            int states = 0;
            int indexOfPkg2 = pkgsStates.indexOfKey(packageName);
            if (indexOfPkg2 >= 0) {
                states = pkgsStates.valueAt(indexOfPkg2).intValue();
                indexOfPkg = indexOfPkg2;
            } else {
                pkgsStates.put(packageName, 0);
                indexOfPkg = pkgsStates.indexOfKey(packageName);
            }
            boolean addEvent2 = false;
            if (start) {
                boolean alreadyStarted = false;
                int i = pkgsStates.size() - 1;
                while (true) {
                    if (i < 0) {
                        break;
                    }
                    int s = pkgsStates.valueAt(i).intValue();
                    if ((s & stateType) == 0) {
                        i--;
                    } else {
                        alreadyStarted = true;
                        break;
                    }
                }
                int i2 = states | stateType;
                pkgsStates.setValueAt(indexOfPkg, Integer.valueOf(i2));
                if (!alreadyStarted) {
                    addEvent2 = true;
                }
                addEvent = addEvent2;
            } else {
                int states2 = states & (~stateType);
                pkgsStates.setValueAt(indexOfPkg, Integer.valueOf(states2));
                boolean allStopped = true;
                int i3 = pkgsStates.size() - 1;
                while (true) {
                    if (i3 < 0) {
                        break;
                    }
                    int s2 = pkgsStates.valueAt(i3).intValue();
                    if ((s2 & stateType) == 0) {
                        i3--;
                    } else {
                        allStopped = false;
                        break;
                    }
                }
                if (allStopped) {
                    addEvent2 = true;
                }
                if (states2 == 0) {
                    pkgsStates.removeAt(indexOfPkg);
                    if (pkgsStates.size() == 0) {
                        map.remove(uid);
                    }
                }
                addEvent = addEvent2;
            }
            if (addEvent) {
                UidBatteryStates pkg2 = (UidBatteryStates) this.mPkgEvents.get(uid, "");
                if (pkg2 == null) {
                    UidBatteryStates pkg3 = createAppStateEvents(uid, "");
                    this.mPkgEvents.put(uid, "", pkg3);
                    pkg = pkg3;
                } else {
                    pkg = pkg2;
                }
                pkg.addEvent(start, now, batteryUsage, stateTypeIndex);
            }
        }
    }

    @Override // com.android.server.am.BaseAppStateDurationsTracker, com.android.server.am.BaseAppStateEventsTracker
    void reset() {
        super.reset();
        synchronized (this.mLock) {
            this.mUidPackageStates.clear();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onTrackerEnabled(boolean enabled) {
        if (!enabled) {
            synchronized (this.mLock) {
                this.mPkgEvents.clear();
                this.mUidPackageStates.clear();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppBatteryTracker.ImmutableBatteryUsage getUidBatteryExemptedUsageSince(int uid, long since, long now, int types) {
        if (!((AppBatteryExemptionPolicy) this.mInjector.getPolicy()).isEnabled()) {
            return AppBatteryTracker.BATTERY_USAGE_NONE;
        }
        synchronized (this.mLock) {
            UidBatteryStates pkg = (UidBatteryStates) this.mPkgEvents.get(uid, "");
            if (pkg == null) {
                return AppBatteryTracker.BATTERY_USAGE_NONE;
            }
            Pair<AppBatteryTracker.ImmutableBatteryUsage, AppBatteryTracker.ImmutableBatteryUsage> result = pkg.getBatteryUsageSince(since, now, types);
            if (!((AppBatteryTracker.ImmutableBatteryUsage) result.second).isEmpty()) {
                AppBatteryTracker.ImmutableBatteryUsage batteryUsage = this.mAppRestrictionController.getUidBatteryUsage(uid);
                return ((AppBatteryTracker.ImmutableBatteryUsage) result.first).mutate().add(batteryUsage).subtract((AppBatteryTracker.BatteryUsage) result.second).unmutate();
            }
            return (AppBatteryTracker.ImmutableBatteryUsage) result.first;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class UidBatteryStates extends BaseAppStateDurations<UidStateEventWithBattery> {
        UidBatteryStates(int uid, String tag, BaseAppStateEvents.MaxTrackingDurationConfig maxTrackingDurationConfig) {
            super(uid, "", 5, tag, maxTrackingDurationConfig);
        }

        UidBatteryStates(UidBatteryStates other) {
            super(other);
        }

        void addEvent(boolean start, long now, AppBatteryTracker.ImmutableBatteryUsage batteryUsage, int eventType) {
            if (start) {
                addEvent(start, (boolean) new UidStateEventWithBattery(start, now, batteryUsage, null), eventType);
                return;
            }
            UidStateEventWithBattery last = getLastEvent(eventType);
            if (last == null || !last.isStart()) {
                return;
            }
            addEvent(start, (boolean) new UidStateEventWithBattery(start, now, batteryUsage.mutate().subtract(last.getBatteryUsage()).unmutate(), last), eventType);
        }

        UidStateEventWithBattery getLastEvent(int eventType) {
            if (this.mEvents[eventType] != null) {
                return (UidStateEventWithBattery) this.mEvents[eventType].peekLast();
            }
            return null;
        }

        private Pair<AppBatteryTracker.ImmutableBatteryUsage, AppBatteryTracker.ImmutableBatteryUsage> getBatteryUsageSince(long since, long now, LinkedList<UidStateEventWithBattery> events) {
            if (events == null || events.size() == 0) {
                return Pair.create(AppBatteryTracker.BATTERY_USAGE_NONE, AppBatteryTracker.BATTERY_USAGE_NONE);
            }
            AppBatteryTracker.BatteryUsage batteryUsage = new AppBatteryTracker.BatteryUsage();
            UidStateEventWithBattery lastEvent = null;
            Iterator<UidStateEventWithBattery> it = events.iterator();
            while (it.hasNext()) {
                UidStateEventWithBattery event = it.next();
                lastEvent = event;
                if (event.getTimestamp() >= since && !event.isStart()) {
                    batteryUsage.add(event.getBatteryUsage(since, Math.min(now, event.getTimestamp())));
                    if (now <= event.getTimestamp()) {
                        break;
                    }
                }
            }
            return Pair.create(batteryUsage.unmutate(), lastEvent.isStart() ? lastEvent.getBatteryUsage() : AppBatteryTracker.BATTERY_USAGE_NONE);
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.am.AppBatteryExemptionTracker$UidBatteryStates */
        /* JADX WARN: Multi-variable type inference failed */
        Pair<AppBatteryTracker.ImmutableBatteryUsage, AppBatteryTracker.ImmutableBatteryUsage> getBatteryUsageSince(long since, long now, int types) {
            LinkedList<UidStateEventWithBattery> result = new LinkedList<>();
            for (int i = 0; i < this.mEvents.length; i++) {
                if ((BaseAppStateTracker.stateIndexToType(i) & types) != 0) {
                    result = add(result, this.mEvents[i]);
                }
            }
            return getBatteryUsageSince(since, now, result);
        }

        @Override // com.android.server.am.BaseAppStateDurations, com.android.server.am.BaseAppStateTimeEvents, com.android.server.am.BaseAppStateEvents
        LinkedList<UidStateEventWithBattery> add(LinkedList<UidStateEventWithBattery> durations, LinkedList<UidStateEventWithBattery> otherDurations) {
            LinkedList<UidStateEventWithBattery> dest;
            UidStateEventWithBattery earliest;
            Iterator<UidStateEventWithBattery> itr;
            UidStateEventWithBattery l;
            boolean actl;
            boolean actr;
            long recentActTs;
            LinkedList<UidStateEventWithBattery> dest2;
            long j;
            if (otherDurations == null || otherDurations.size() == 0) {
                return durations;
            }
            if (durations == null || durations.size() == 0) {
                return (LinkedList) otherDurations.clone();
            }
            Iterator<UidStateEventWithBattery> itl = durations.iterator();
            Iterator<UidStateEventWithBattery> itr2 = otherDurations.iterator();
            UidStateEventWithBattery l2 = itl.next();
            UidStateEventWithBattery r = itr2.next();
            LinkedList<UidStateEventWithBattery> dest3 = new LinkedList<>();
            boolean actr2 = false;
            boolean actr3 = false;
            boolean overlapping = false;
            AppBatteryTracker.BatteryUsage batteryUsage = new AppBatteryTracker.BatteryUsage();
            long recentActTs2 = 0;
            long overlappingDuration = 0;
            long lts = l2.getTimestamp();
            long rts = r.getTimestamp();
            while (true) {
                long j2 = JobStatus.NO_LATEST_RUNTIME;
                if (lts != JobStatus.NO_LATEST_RUNTIME || rts != JobStatus.NO_LATEST_RUNTIME) {
                    boolean actCur = actr2 || actr3;
                    if (lts == rts) {
                        earliest = l2;
                        if (actr2) {
                            dest = dest3;
                            batteryUsage.add(l2.getBatteryUsage());
                        } else {
                            dest = dest3;
                        }
                        if (actr3) {
                            batteryUsage.add(r.getBatteryUsage());
                        }
                        overlappingDuration += (overlapping && (actr2 || actr3)) ? lts - recentActTs2 : 0L;
                        boolean actl2 = !actr2;
                        boolean actr4 = !actr3;
                        boolean actr5 = itl.hasNext();
                        if (actr5) {
                            UidStateEventWithBattery next = itl.next();
                            l2 = next;
                            j = next.getTimestamp();
                        } else {
                            j = Long.MAX_VALUE;
                        }
                        lts = j;
                        if (itr2.hasNext()) {
                            UidStateEventWithBattery next2 = itr2.next();
                            r = next2;
                            j2 = next2.getTimestamp();
                        }
                        rts = j2;
                        actr3 = actr4;
                        actr2 = actl2;
                    } else {
                        dest = dest3;
                        if (lts < rts) {
                            earliest = l2;
                            if (actr2) {
                                batteryUsage.add(l2.getBatteryUsage());
                            }
                            overlappingDuration += (overlapping && actr2) ? lts - recentActTs2 : 0L;
                            boolean actl3 = !actr2;
                            boolean actl4 = itl.hasNext();
                            if (actl4) {
                                UidStateEventWithBattery next3 = itl.next();
                                l2 = next3;
                                j2 = next3.getTimestamp();
                            }
                            lts = j2;
                            actr2 = actl3;
                        } else {
                            earliest = r;
                            if (actr3) {
                                batteryUsage.add(r.getBatteryUsage());
                            }
                            overlappingDuration += (overlapping && actr3) ? rts - recentActTs2 : 0L;
                            boolean actr6 = !actr3;
                            boolean actr7 = itr2.hasNext();
                            if (actr7) {
                                UidStateEventWithBattery next4 = itr2.next();
                                r = next4;
                                j2 = next4.getTimestamp();
                            }
                            rts = j2;
                            actr3 = actr6;
                        }
                    }
                    overlapping = actr2 && actr3;
                    if (actr2 || actr3) {
                        recentActTs2 = earliest.getTimestamp();
                    }
                    Iterator<UidStateEventWithBattery> itl2 = itl;
                    if (actCur != (actr2 || actr3)) {
                        UidStateEventWithBattery event = (UidStateEventWithBattery) earliest.clone();
                        if (actCur) {
                            UidStateEventWithBattery lastEvent = dest.peekLast();
                            long startTs = lastEvent.getTimestamp();
                            itr = itr2;
                            l = l2;
                            long duration = event.getTimestamp() - startTs;
                            actl = actr2;
                            actr = actr3;
                            long durationWithOverlapping = duration + overlappingDuration;
                            if (durationWithOverlapping != 0) {
                                recentActTs = recentActTs2;
                                batteryUsage.scale((duration * 1.0d) / durationWithOverlapping);
                                event.update(lastEvent, new AppBatteryTracker.ImmutableBatteryUsage(batteryUsage));
                            } else {
                                recentActTs = recentActTs2;
                                event.update(lastEvent, AppBatteryTracker.BATTERY_USAGE_NONE);
                            }
                            batteryUsage.setTo(AppBatteryTracker.BATTERY_USAGE_NONE);
                            overlappingDuration = 0;
                        } else {
                            itr = itr2;
                            l = l2;
                            actl = actr2;
                            actr = actr3;
                            recentActTs = recentActTs2;
                        }
                        dest2 = dest;
                        dest2.add(event);
                    } else {
                        itr = itr2;
                        l = l2;
                        actl = actr2;
                        actr = actr3;
                        recentActTs = recentActTs2;
                        dest2 = dest;
                    }
                    dest3 = dest2;
                    itl = itl2;
                    itr2 = itr;
                    l2 = l;
                    actr2 = actl;
                    recentActTs2 = recentActTs;
                    actr3 = actr;
                } else {
                    return dest3;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void trimDurations() {
        long now = SystemClock.elapsedRealtime();
        trim(Math.max(0L, now - ((AppBatteryExemptionPolicy) this.mInjector.getPolicy()).getMaxTrackingDuration()));
    }

    @Override // com.android.server.am.BaseAppStateEventsTracker, com.android.server.am.BaseAppStateTracker
    void dump(PrintWriter pw, String prefix) {
        ((AppBatteryExemptionPolicy) this.mInjector.getPolicy()).dump(pw, prefix);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class UidStateEventWithBattery extends BaseAppStateTimeEvents.BaseTimeEvent {
        private AppBatteryTracker.ImmutableBatteryUsage mBatteryUsage;
        private boolean mIsStart;
        private UidStateEventWithBattery mPeer;

        UidStateEventWithBattery(boolean isStart, long now, AppBatteryTracker.ImmutableBatteryUsage batteryUsage, UidStateEventWithBattery peer) {
            super(now);
            this.mIsStart = isStart;
            this.mBatteryUsage = batteryUsage;
            this.mPeer = peer;
            if (peer != null) {
                peer.mPeer = this;
            }
        }

        UidStateEventWithBattery(UidStateEventWithBattery other) {
            super(other);
            this.mIsStart = other.mIsStart;
            this.mBatteryUsage = other.mBatteryUsage;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.android.server.am.BaseAppStateTimeEvents.BaseTimeEvent
        public void trimTo(long timestamp) {
            if (!this.mIsStart || timestamp < this.mTimestamp) {
                return;
            }
            UidStateEventWithBattery uidStateEventWithBattery = this.mPeer;
            if (uidStateEventWithBattery != null) {
                AppBatteryTracker.ImmutableBatteryUsage batteryUsage = uidStateEventWithBattery.getBatteryUsage();
                UidStateEventWithBattery uidStateEventWithBattery2 = this.mPeer;
                uidStateEventWithBattery2.mBatteryUsage = uidStateEventWithBattery2.getBatteryUsage(timestamp, uidStateEventWithBattery2.mTimestamp);
                this.mBatteryUsage = this.mBatteryUsage.mutate().add(batteryUsage).subtract(this.mPeer.mBatteryUsage).unmutate();
            }
            this.mTimestamp = timestamp;
        }

        void update(UidStateEventWithBattery peer, AppBatteryTracker.ImmutableBatteryUsage batteryUsage) {
            this.mPeer = peer;
            peer.mPeer = this;
            this.mBatteryUsage = batteryUsage;
        }

        boolean isStart() {
            return this.mIsStart;
        }

        AppBatteryTracker.ImmutableBatteryUsage getBatteryUsage(long start, long end) {
            if (this.mIsStart || start >= this.mTimestamp || end <= start) {
                return AppBatteryTracker.BATTERY_USAGE_NONE;
            }
            long start2 = Math.max(start, this.mPeer.mTimestamp);
            long end2 = Math.min(end, this.mTimestamp);
            long totalDur = this.mTimestamp - this.mPeer.mTimestamp;
            long inputDur = end2 - start2;
            return totalDur != 0 ? totalDur == inputDur ? this.mBatteryUsage : this.mBatteryUsage.mutate().scale((inputDur * 1.0d) / totalDur).unmutate() : AppBatteryTracker.BATTERY_USAGE_NONE;
        }

        AppBatteryTracker.ImmutableBatteryUsage getBatteryUsage() {
            return this.mBatteryUsage;
        }

        @Override // com.android.server.am.BaseAppStateTimeEvents.BaseTimeEvent
        public Object clone() {
            return new UidStateEventWithBattery(this);
        }

        @Override // com.android.server.am.BaseAppStateTimeEvents.BaseTimeEvent
        public boolean equals(Object other) {
            if (other == null || other.getClass() != UidStateEventWithBattery.class) {
                return false;
            }
            UidStateEventWithBattery otherEvent = (UidStateEventWithBattery) other;
            return otherEvent.mIsStart == this.mIsStart && otherEvent.mTimestamp == this.mTimestamp && this.mBatteryUsage.equals(otherEvent.mBatteryUsage);
        }

        public String toString() {
            return "UidStateEventWithBattery(" + this.mIsStart + ", " + this.mTimestamp + ", " + this.mBatteryUsage + ")";
        }

        @Override // com.android.server.am.BaseAppStateTimeEvents.BaseTimeEvent
        public int hashCode() {
            return (((Boolean.hashCode(this.mIsStart) * 31) + Long.hashCode(this.mTimestamp)) * 31) + this.mBatteryUsage.hashCode();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class AppBatteryExemptionPolicy extends BaseAppStateEventsTracker.BaseAppStateEventsPolicy<AppBatteryExemptionTracker> {
        static final boolean DEFAULT_BG_BATTERY_EXEMPTION_ENABLED = true;
        static final String KEY_BG_BATTERY_EXEMPTION_ENABLED = "bg_battery_exemption_enabled";

        AppBatteryExemptionPolicy(BaseAppStateTracker.Injector injector, AppBatteryExemptionTracker tracker) {
            super(injector, tracker, KEY_BG_BATTERY_EXEMPTION_ENABLED, true, "bg_current_drain_window", tracker.mContext.getResources().getInteger(17694752));
        }

        @Override // com.android.server.am.BaseAppStateEventsTracker.BaseAppStateEventsPolicy
        public void onMaxTrackingDurationChanged(long maxDuration) {
            Handler handler = ((AppBatteryExemptionTracker) this.mTracker).mBgHandler;
            final AppBatteryExemptionTracker appBatteryExemptionTracker = (AppBatteryExemptionTracker) this.mTracker;
            Objects.requireNonNull(appBatteryExemptionTracker);
            handler.post(new Runnable() { // from class: com.android.server.am.AppBatteryExemptionTracker$AppBatteryExemptionPolicy$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    AppBatteryExemptionTracker.this.trimDurations();
                }
            });
        }

        @Override // com.android.server.am.BaseAppStatePolicy
        public void onTrackerEnabled(boolean enabled) {
            ((AppBatteryExemptionTracker) this.mTracker).onTrackerEnabled(enabled);
        }

        @Override // com.android.server.am.BaseAppStateEventsTracker.BaseAppStateEventsPolicy, com.android.server.am.BaseAppStatePolicy
        void dump(PrintWriter pw, String prefix) {
            pw.print(prefix);
            pw.println("APP BATTERY EXEMPTION TRACKER POLICY SETTINGS:");
            super.dump(pw, "  " + prefix);
        }
    }
}
