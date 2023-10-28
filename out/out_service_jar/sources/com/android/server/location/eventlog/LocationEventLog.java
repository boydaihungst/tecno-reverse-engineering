package com.android.server.location.eventlog;

import android.location.LocationRequest;
import android.location.provider.ProviderRequest;
import android.location.util.identity.CallerIdentity;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.TimeUtils;
import com.android.internal.util.Preconditions;
import com.android.server.job.controllers.JobStatus;
import com.android.server.location.LocationManagerService;
import com.android.server.location.eventlog.LocalEventLog;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class LocationEventLog extends LocalEventLog<Object> {
    public static final LocationEventLog EVENT_LOG = new LocationEventLog();
    private final ArrayMap<String, ArrayMap<CallerIdentity, AggregateStats>> mAggregateStats;
    private final LocationsEventLog mLocationsLog;

    private static int getLogSize() {
        if (LocationManagerService.D) {
            return 600;
        }
        return 300;
    }

    private static int getLocationsLogSize() {
        if (LocationManagerService.D) {
            return 200;
        }
        return 100;
    }

    private LocationEventLog() {
        super(getLogSize(), Object.class);
        this.mAggregateStats = new ArrayMap<>(4);
        this.mLocationsLog = new LocationsEventLog(getLocationsLogSize());
    }

    public ArrayMap<String, ArrayMap<CallerIdentity, AggregateStats>> copyAggregateStats() {
        ArrayMap<String, ArrayMap<CallerIdentity, AggregateStats>> copy;
        synchronized (this.mAggregateStats) {
            copy = new ArrayMap<>(this.mAggregateStats);
            for (int i = 0; i < copy.size(); i++) {
                copy.setValueAt(i, new ArrayMap<>(copy.valueAt(i)));
            }
        }
        return copy;
    }

    private AggregateStats getAggregateStats(String provider, CallerIdentity identity) {
        AggregateStats stats;
        synchronized (this.mAggregateStats) {
            ArrayMap<CallerIdentity, AggregateStats> packageMap = this.mAggregateStats.get(provider);
            if (packageMap == null) {
                packageMap = new ArrayMap<>(2);
                this.mAggregateStats.put(provider, packageMap);
            }
            CallerIdentity aggregate = CallerIdentity.forAggregation(identity);
            stats = packageMap.get(aggregate);
            if (stats == null) {
                stats = new AggregateStats();
                packageMap.put(aggregate, stats);
            }
        }
        return stats;
    }

    public void logUserSwitched(int userIdFrom, int userIdTo) {
        addLog(new UserSwitchedEvent(userIdFrom, userIdTo));
    }

    public void logLocationEnabled(int userId, boolean enabled) {
        addLog(new LocationEnabledEvent(userId, enabled));
    }

    public void logAdasLocationEnabled(int userId, boolean enabled) {
        addLog(new LocationAdasEnabledEvent(userId, enabled));
    }

    public void logProviderEnabled(String provider, int userId, boolean enabled) {
        addLog(new ProviderEnabledEvent(provider, userId, enabled));
    }

    public void logProviderMocked(String provider, boolean mocked) {
        addLog(new ProviderMockedEvent(provider, mocked));
    }

    public void logProviderClientRegistered(String provider, CallerIdentity identity, LocationRequest request) {
        addLog(new ProviderClientRegisterEvent(provider, true, identity, request));
        getAggregateStats(provider, identity).markRequestAdded(request.getIntervalMillis());
    }

    public void logProviderClientUnregistered(String provider, CallerIdentity identity) {
        addLog(new ProviderClientRegisterEvent(provider, false, identity, null));
        getAggregateStats(provider, identity).markRequestRemoved();
    }

    public void logProviderClientActive(String provider, CallerIdentity identity) {
        getAggregateStats(provider, identity).markRequestActive();
    }

    public void logProviderClientInactive(String provider, CallerIdentity identity) {
        getAggregateStats(provider, identity).markRequestInactive();
    }

    public void logProviderClientForeground(String provider, CallerIdentity identity) {
        if (LocationManagerService.D) {
            addLog(new ProviderClientForegroundEvent(provider, true, identity));
        }
        getAggregateStats(provider, identity).markRequestForeground();
    }

    public void logProviderClientBackground(String provider, CallerIdentity identity) {
        if (LocationManagerService.D) {
            addLog(new ProviderClientForegroundEvent(provider, false, identity));
        }
        getAggregateStats(provider, identity).markRequestBackground();
    }

    public void logProviderClientPermitted(String provider, CallerIdentity identity) {
        if (LocationManagerService.D) {
            addLog(new ProviderClientPermittedEvent(provider, true, identity));
        }
    }

    public void logProviderClientUnpermitted(String provider, CallerIdentity identity) {
        if (LocationManagerService.D) {
            addLog(new ProviderClientPermittedEvent(provider, false, identity));
        }
    }

    public void logProviderUpdateRequest(String provider, ProviderRequest request) {
        addLog(new ProviderUpdateEvent(provider, request));
    }

    public void logProviderReceivedLocations(String provider, int numLocations) {
        synchronized (this) {
            this.mLocationsLog.logProviderReceivedLocations(provider, numLocations);
        }
    }

    public void logProviderDeliveredLocations(String provider, int numLocations, CallerIdentity identity) {
        synchronized (this) {
            this.mLocationsLog.logProviderDeliveredLocations(provider, numLocations, identity);
        }
        getAggregateStats(provider, identity).markLocationDelivered();
    }

    public void logProviderStationaryThrottled(String provider, boolean throttled, ProviderRequest request) {
        addLog(new ProviderStationaryThrottledEvent(provider, throttled, request));
    }

    public void logLocationPowerSaveMode(int locationPowerSaveMode) {
        addLog(new LocationPowerSaveModeEvent(locationPowerSaveMode));
    }

    private void addLog(Object logEvent) {
        addLog(SystemClock.elapsedRealtime(), logEvent);
    }

    @Override // com.android.server.location.eventlog.LocalEventLog
    public synchronized void iterate(LocalEventLog.LogConsumer<? super Object> consumer) {
        iterate(consumer, this, this.mLocationsLog);
    }

    public void iterate(Consumer<String> consumer) {
        iterate(consumer, (String) null);
    }

    public void iterate(final Consumer<String> consumer, final String providerFilter) {
        final long systemTimeDeltaMs = System.currentTimeMillis() - SystemClock.elapsedRealtime();
        final StringBuilder builder = new StringBuilder();
        iterate(new LocalEventLog.LogConsumer() { // from class: com.android.server.location.eventlog.LocationEventLog$$ExternalSyntheticLambda0
            @Override // com.android.server.location.eventlog.LocalEventLog.LogConsumer
            public final void acceptLog(long j, Object obj) {
                LocationEventLog.lambda$iterate$0(providerFilter, builder, systemTimeDeltaMs, consumer, j, obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$iterate$0(String providerFilter, StringBuilder builder, long systemTimeDeltaMs, Consumer consumer, long time, Object logEvent) {
        boolean match = providerFilter == null || ((logEvent instanceof ProviderEvent) && providerFilter.equals(((ProviderEvent) logEvent).mProvider));
        if (match) {
            builder.setLength(0);
            builder.append(TimeUtils.logTimeOfDay(time + systemTimeDeltaMs));
            builder.append(": ");
            builder.append(logEvent);
            consumer.accept(builder.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static abstract class ProviderEvent {
        protected final String mProvider;

        ProviderEvent(String provider) {
            this.mProvider = provider;
        }
    }

    /* loaded from: classes.dex */
    private static final class ProviderEnabledEvent extends ProviderEvent {
        private final boolean mEnabled;
        private final int mUserId;

        ProviderEnabledEvent(String provider, int userId, boolean enabled) {
            super(provider);
            this.mUserId = userId;
            this.mEnabled = enabled;
        }

        public String toString() {
            return this.mProvider + " provider [u" + this.mUserId + "] " + (this.mEnabled ? ServiceConfigAccessor.PROVIDER_MODE_ENABLED : ServiceConfigAccessor.PROVIDER_MODE_DISABLED);
        }
    }

    /* loaded from: classes.dex */
    private static final class ProviderMockedEvent extends ProviderEvent {
        private final boolean mMocked;

        ProviderMockedEvent(String provider, boolean mocked) {
            super(provider);
            this.mMocked = mocked;
        }

        public String toString() {
            if (this.mMocked) {
                return this.mProvider + " provider added mock provider override";
            }
            return this.mProvider + " provider removed mock provider override";
        }
    }

    /* loaded from: classes.dex */
    private static final class ProviderClientRegisterEvent extends ProviderEvent {
        private final CallerIdentity mIdentity;
        private final LocationRequest mLocationRequest;
        private final boolean mRegistered;

        ProviderClientRegisterEvent(String provider, boolean registered, CallerIdentity identity, LocationRequest locationRequest) {
            super(provider);
            this.mRegistered = registered;
            this.mIdentity = identity;
            this.mLocationRequest = locationRequest;
        }

        public String toString() {
            if (this.mRegistered) {
                return this.mProvider + " provider +registration " + this.mIdentity + " -> " + this.mLocationRequest;
            }
            return this.mProvider + " provider -registration " + this.mIdentity;
        }
    }

    /* loaded from: classes.dex */
    private static final class ProviderClientForegroundEvent extends ProviderEvent {
        private final boolean mForeground;
        private final CallerIdentity mIdentity;

        ProviderClientForegroundEvent(String provider, boolean foreground, CallerIdentity identity) {
            super(provider);
            this.mForeground = foreground;
            this.mIdentity = identity;
        }

        public String toString() {
            return this.mProvider + " provider client " + this.mIdentity + " -> " + (this.mForeground ? "foreground" : "background");
        }
    }

    /* loaded from: classes.dex */
    private static final class ProviderClientPermittedEvent extends ProviderEvent {
        private final CallerIdentity mIdentity;
        private final boolean mPermitted;

        ProviderClientPermittedEvent(String provider, boolean permitted, CallerIdentity identity) {
            super(provider);
            this.mPermitted = permitted;
            this.mIdentity = identity;
        }

        public String toString() {
            return this.mProvider + " provider client " + this.mIdentity + " -> " + (this.mPermitted ? "permitted" : "unpermitted");
        }
    }

    /* loaded from: classes.dex */
    private static final class ProviderUpdateEvent extends ProviderEvent {
        private final ProviderRequest mRequest;

        ProviderUpdateEvent(String provider, ProviderRequest request) {
            super(provider);
            this.mRequest = request;
        }

        public String toString() {
            return this.mProvider + " provider request = " + this.mRequest;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class ProviderReceiveLocationEvent extends ProviderEvent {
        private final int mNumLocations;

        ProviderReceiveLocationEvent(String provider, int numLocations) {
            super(provider);
            this.mNumLocations = numLocations;
        }

        public String toString() {
            return this.mProvider + " provider received location[" + this.mNumLocations + "]";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class ProviderDeliverLocationEvent extends ProviderEvent {
        private final CallerIdentity mIdentity;
        private final int mNumLocations;

        ProviderDeliverLocationEvent(String provider, int numLocations, CallerIdentity identity) {
            super(provider);
            this.mNumLocations = numLocations;
            this.mIdentity = identity;
        }

        public String toString() {
            return this.mProvider + " provider delivered location[" + this.mNumLocations + "] to " + this.mIdentity;
        }
    }

    /* loaded from: classes.dex */
    private static final class ProviderStationaryThrottledEvent extends ProviderEvent {
        private final ProviderRequest mRequest;
        private final boolean mStationaryThrottled;

        ProviderStationaryThrottledEvent(String provider, boolean stationaryThrottled, ProviderRequest request) {
            super(provider);
            this.mStationaryThrottled = stationaryThrottled;
            this.mRequest = request;
        }

        public String toString() {
            return this.mProvider + " provider stationary/idle " + (this.mStationaryThrottled ? "throttled" : "unthrottled") + ", request = " + this.mRequest;
        }
    }

    /* loaded from: classes.dex */
    private static final class LocationPowerSaveModeEvent {
        private final int mLocationPowerSaveMode;

        LocationPowerSaveModeEvent(int locationPowerSaveMode) {
            this.mLocationPowerSaveMode = locationPowerSaveMode;
        }

        public String toString() {
            String mode;
            switch (this.mLocationPowerSaveMode) {
                case 0:
                    mode = "NO_CHANGE";
                    break;
                case 1:
                    mode = "GPS_DISABLED_WHEN_SCREEN_OFF";
                    break;
                case 2:
                    mode = "ALL_DISABLED_WHEN_SCREEN_OFF";
                    break;
                case 3:
                    mode = "FOREGROUND_ONLY";
                    break;
                case 4:
                    mode = "THROTTLE_REQUESTS_WHEN_SCREEN_OFF";
                    break;
                default:
                    mode = "UNKNOWN";
                    break;
            }
            return "location power save mode changed to " + mode;
        }
    }

    /* loaded from: classes.dex */
    private static final class UserSwitchedEvent {
        private final int mUserIdFrom;
        private final int mUserIdTo;

        UserSwitchedEvent(int userIdFrom, int userIdTo) {
            this.mUserIdFrom = userIdFrom;
            this.mUserIdTo = userIdTo;
        }

        public String toString() {
            return "current user switched from u" + this.mUserIdFrom + " to u" + this.mUserIdTo;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class LocationEnabledEvent {
        private final boolean mEnabled;
        private final int mUserId;

        LocationEnabledEvent(int userId, boolean enabled) {
            this.mUserId = userId;
            this.mEnabled = enabled;
        }

        public String toString() {
            return "location [u" + this.mUserId + "] " + (this.mEnabled ? ServiceConfigAccessor.PROVIDER_MODE_ENABLED : ServiceConfigAccessor.PROVIDER_MODE_DISABLED);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class LocationAdasEnabledEvent {
        private final boolean mEnabled;
        private final int mUserId;

        LocationAdasEnabledEvent(int userId, boolean enabled) {
            this.mUserId = userId;
            this.mEnabled = enabled;
        }

        public String toString() {
            return "adas location [u" + this.mUserId + "] " + (this.mEnabled ? ServiceConfigAccessor.PROVIDER_MODE_ENABLED : ServiceConfigAccessor.PROVIDER_MODE_DISABLED);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class LocationsEventLog extends LocalEventLog<Object> {
        LocationsEventLog(int size) {
            super(size, Object.class);
        }

        public void logProviderReceivedLocations(String provider, int numLocations) {
            addLog(new ProviderReceiveLocationEvent(provider, numLocations));
        }

        public void logProviderDeliveredLocations(String provider, int numLocations, CallerIdentity identity) {
            addLog(new ProviderDeliverLocationEvent(provider, numLocations, identity));
        }

        private void addLog(Object logEvent) {
            addLog(SystemClock.elapsedRealtime(), logEvent);
        }
    }

    /* loaded from: classes.dex */
    public static final class AggregateStats {
        private int mActiveRequestCount;
        private long mActiveTimeLastUpdateRealtimeMs;
        private long mActiveTimeTotalMs;
        private int mAddedRequestCount;
        private long mAddedTimeLastUpdateRealtimeMs;
        private long mAddedTimeTotalMs;
        private int mDeliveredLocationCount;
        private int mForegroundRequestCount;
        private long mForegroundTimeLastUpdateRealtimeMs;
        private long mForegroundTimeTotalMs;
        private long mFastestIntervalMs = JobStatus.NO_LATEST_RUNTIME;
        private long mSlowestIntervalMs = 0;

        AggregateStats() {
        }

        synchronized void markRequestAdded(long intervalMillis) {
            int i = this.mAddedRequestCount;
            this.mAddedRequestCount = i + 1;
            if (i == 0) {
                this.mAddedTimeLastUpdateRealtimeMs = SystemClock.elapsedRealtime();
            }
            this.mFastestIntervalMs = Math.min(intervalMillis, this.mFastestIntervalMs);
            this.mSlowestIntervalMs = Math.max(intervalMillis, this.mSlowestIntervalMs);
        }

        synchronized void markRequestRemoved() {
            updateTotals();
            boolean z = true;
            int i = this.mAddedRequestCount - 1;
            this.mAddedRequestCount = i;
            if (i < 0) {
                z = false;
            }
            Preconditions.checkState(z);
            this.mActiveRequestCount = Math.min(this.mAddedRequestCount, this.mActiveRequestCount);
            this.mForegroundRequestCount = Math.min(this.mAddedRequestCount, this.mForegroundRequestCount);
        }

        synchronized void markRequestActive() {
            Preconditions.checkState(this.mAddedRequestCount > 0);
            int i = this.mActiveRequestCount;
            this.mActiveRequestCount = i + 1;
            if (i == 0) {
                this.mActiveTimeLastUpdateRealtimeMs = SystemClock.elapsedRealtime();
            }
        }

        synchronized void markRequestInactive() {
            updateTotals();
            boolean z = true;
            int i = this.mActiveRequestCount - 1;
            this.mActiveRequestCount = i;
            if (i < 0) {
                z = false;
            }
            Preconditions.checkState(z);
        }

        synchronized void markRequestForeground() {
            Preconditions.checkState(this.mAddedRequestCount > 0);
            int i = this.mForegroundRequestCount;
            this.mForegroundRequestCount = i + 1;
            if (i == 0) {
                this.mForegroundTimeLastUpdateRealtimeMs = SystemClock.elapsedRealtime();
            }
        }

        synchronized void markRequestBackground() {
            updateTotals();
            boolean z = true;
            int i = this.mForegroundRequestCount - 1;
            this.mForegroundRequestCount = i;
            if (i < 0) {
                z = false;
            }
            Preconditions.checkState(z);
        }

        synchronized void markLocationDelivered() {
            this.mDeliveredLocationCount++;
        }

        public synchronized void updateTotals() {
            if (this.mAddedRequestCount > 0) {
                long realtimeMs = SystemClock.elapsedRealtime();
                this.mAddedTimeTotalMs += realtimeMs - this.mAddedTimeLastUpdateRealtimeMs;
                this.mAddedTimeLastUpdateRealtimeMs = realtimeMs;
            }
            if (this.mActiveRequestCount > 0) {
                long realtimeMs2 = SystemClock.elapsedRealtime();
                this.mActiveTimeTotalMs += realtimeMs2 - this.mActiveTimeLastUpdateRealtimeMs;
                this.mActiveTimeLastUpdateRealtimeMs = realtimeMs2;
            }
            if (this.mForegroundRequestCount > 0) {
                long realtimeMs3 = SystemClock.elapsedRealtime();
                this.mForegroundTimeTotalMs += realtimeMs3 - this.mForegroundTimeLastUpdateRealtimeMs;
                this.mForegroundTimeLastUpdateRealtimeMs = realtimeMs3;
            }
        }

        public synchronized String toString() {
            return "min/max interval = " + intervalToString(this.mFastestIntervalMs) + SliceClientPermissions.SliceAuthority.DELIMITER + intervalToString(this.mSlowestIntervalMs) + ", total/active/foreground duration = " + TimeUtils.formatDuration(this.mAddedTimeTotalMs) + SliceClientPermissions.SliceAuthority.DELIMITER + TimeUtils.formatDuration(this.mActiveTimeTotalMs) + SliceClientPermissions.SliceAuthority.DELIMITER + TimeUtils.formatDuration(this.mForegroundTimeTotalMs) + ", locations = " + this.mDeliveredLocationCount;
        }

        private static String intervalToString(long intervalMs) {
            if (intervalMs == JobStatus.NO_LATEST_RUNTIME) {
                return "passive";
            }
            return TimeUnit.MILLISECONDS.toSeconds(intervalMs) + "s";
        }
    }
}
