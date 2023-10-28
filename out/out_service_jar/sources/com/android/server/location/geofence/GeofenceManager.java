package com.android.server.location.geofence;

import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Geofence;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.location.util.identity.CallerIdentity;
import android.os.Binder;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.WorkSource;
import android.util.ArraySet;
import com.android.internal.listeners.ListenerExecutor;
import com.android.server.FgThread;
import com.android.server.PendingIntentUtils;
import com.android.server.location.LocationPermissions;
import com.android.server.location.geofence.GeofenceManager;
import com.android.server.location.injector.Injector;
import com.android.server.location.injector.LocationPermissionsHelper;
import com.android.server.location.injector.LocationUsageLogger;
import com.android.server.location.injector.SettingsHelper;
import com.android.server.location.injector.UserInfoHelper;
import com.android.server.location.listeners.ListenerMultiplexer;
import com.android.server.location.listeners.PendingIntentListenerRegistration;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public class GeofenceManager extends ListenerMultiplexer<GeofenceKey, PendingIntent, GeofenceRegistration, LocationRequest> implements LocationListener {
    private static final String ATTRIBUTION_TAG = "GeofencingService";
    private static final long MAX_LOCATION_AGE_MS = 300000;
    private static final long MAX_LOCATION_INTERVAL_MS = 7200000;
    private static final int MAX_SPEED_M_S = 100;
    private static final String TAG = "GeofenceManager";
    private static final long WAKELOCK_TIMEOUT_MS = 30000;
    protected final Context mContext;
    private Location mLastLocation;
    private LocationManager mLocationManager;
    protected final LocationPermissionsHelper mLocationPermissionsHelper;
    protected final LocationUsageLogger mLocationUsageLogger;
    protected final SettingsHelper mSettingsHelper;
    protected final UserInfoHelper mUserInfoHelper;
    final Object mLock = new Object();
    private final UserInfoHelper.UserListener mUserChangedListener = new UserInfoHelper.UserListener() { // from class: com.android.server.location.geofence.GeofenceManager$$ExternalSyntheticLambda6
        @Override // com.android.server.location.injector.UserInfoHelper.UserListener
        public final void onUserChanged(int i, int i2) {
            GeofenceManager.this.onUserChanged(i, i2);
        }
    };
    private final SettingsHelper.UserSettingChangedListener mLocationEnabledChangedListener = new SettingsHelper.UserSettingChangedListener() { // from class: com.android.server.location.geofence.GeofenceManager$$ExternalSyntheticLambda7
        @Override // com.android.server.location.injector.SettingsHelper.UserSettingChangedListener
        public final void onSettingChanged(int i) {
            GeofenceManager.this.onLocationEnabledChanged(i);
        }
    };
    private final SettingsHelper.UserSettingChangedListener mLocationPackageBlacklistChangedListener = new SettingsHelper.UserSettingChangedListener() { // from class: com.android.server.location.geofence.GeofenceManager$$ExternalSyntheticLambda8
        @Override // com.android.server.location.injector.SettingsHelper.UserSettingChangedListener
        public final void onSettingChanged(int i) {
            GeofenceManager.this.onLocationPackageBlacklistChanged(i);
        }
    };
    private final LocationPermissionsHelper.LocationPermissionsListener mLocationPermissionsListener = new LocationPermissionsHelper.LocationPermissionsListener() { // from class: com.android.server.location.geofence.GeofenceManager.1
        @Override // com.android.server.location.injector.LocationPermissionsHelper.LocationPermissionsListener
        public void onLocationPermissionsChanged(String packageName) {
            GeofenceManager.this.onLocationPermissionsChanged(packageName);
        }

        @Override // com.android.server.location.injector.LocationPermissionsHelper.LocationPermissionsListener
        public void onLocationPermissionsChanged(int uid) {
            GeofenceManager.this.onLocationPermissionsChanged(uid);
        }
    };

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public final class GeofenceRegistration extends PendingIntentListenerRegistration<Geofence, PendingIntent> {
        private static final int STATE_INSIDE = 1;
        private static final int STATE_OUTSIDE = 2;
        private static final int STATE_UNKNOWN = 0;
        private Location mCachedLocation;
        private float mCachedLocationDistanceM;
        private final Location mCenter;
        private int mGeofenceState;
        private boolean mPermitted;
        private final PowerManager.WakeLock mWakeLock;

        protected GeofenceRegistration(Geofence geofence, CallerIdentity identity, PendingIntent pendingIntent) {
            super(geofence, identity, pendingIntent);
            Location location = new Location("");
            this.mCenter = location;
            location.setLatitude(geofence.getLatitude());
            location.setLongitude(geofence.getLongitude());
            PowerManager.WakeLock newWakeLock = ((PowerManager) Objects.requireNonNull((PowerManager) GeofenceManager.this.mContext.getSystemService(PowerManager.class))).newWakeLock(1, "GeofenceManager:" + identity.getPackageName());
            this.mWakeLock = newWakeLock;
            newWakeLock.setReferenceCounted(true);
            newWakeLock.setWorkSource(identity.addToWorkSource((WorkSource) null));
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.location.listeners.RemovableListenerRegistration
        public GeofenceManager getOwner() {
            return GeofenceManager.this;
        }

        @Override // com.android.server.location.listeners.PendingIntentListenerRegistration
        protected void onPendingIntentListenerRegister() {
            this.mGeofenceState = 0;
            this.mPermitted = GeofenceManager.this.mLocationPermissionsHelper.hasLocationPermissions(2, getIdentity());
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.location.listeners.ListenerRegistration
        public void onActive() {
            Location location = GeofenceManager.this.getLastLocation();
            if (location != null) {
                executeOperation(onLocationChanged(location));
            }
        }

        boolean isPermitted() {
            return this.mPermitted;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean onLocationPermissionsChanged(String packageName) {
            if (packageName == null || getIdentity().getPackageName().equals(packageName)) {
                return onLocationPermissionsChanged();
            }
            return false;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean onLocationPermissionsChanged(int uid) {
            if (getIdentity().getUid() == uid) {
                return onLocationPermissionsChanged();
            }
            return false;
        }

        private boolean onLocationPermissionsChanged() {
            boolean permitted = GeofenceManager.this.mLocationPermissionsHelper.hasLocationPermissions(2, getIdentity());
            if (permitted != this.mPermitted) {
                this.mPermitted = permitted;
                return true;
            }
            return false;
        }

        double getDistanceToBoundary(Location location) {
            if (!location.equals(this.mCachedLocation)) {
                this.mCachedLocation = location;
                this.mCachedLocationDistanceM = this.mCenter.distanceTo(location);
            }
            return Math.abs(getRequest().getRadius() - this.mCachedLocationDistanceM);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public ListenerExecutor.ListenerOperation<PendingIntent> onLocationChanged(Location location) {
            if (getRequest().isExpired()) {
                remove();
                return null;
            }
            this.mCachedLocation = location;
            this.mCachedLocationDistanceM = this.mCenter.distanceTo(location);
            int oldState = this.mGeofenceState;
            float radius = Math.max(getRequest().getRadius(), location.getAccuracy());
            if (this.mCachedLocationDistanceM <= radius) {
                this.mGeofenceState = 1;
                if (oldState != 1) {
                    return new ListenerExecutor.ListenerOperation() { // from class: com.android.server.location.geofence.GeofenceManager$GeofenceRegistration$$ExternalSyntheticLambda1
                        public final void operate(Object obj) {
                            GeofenceManager.GeofenceRegistration.this.m4330x438a6db0((PendingIntent) obj);
                        }
                    };
                }
            } else {
                this.mGeofenceState = 2;
                if (oldState == 1) {
                    return new ListenerExecutor.ListenerOperation() { // from class: com.android.server.location.geofence.GeofenceManager$GeofenceRegistration$$ExternalSyntheticLambda2
                        public final void operate(Object obj) {
                            GeofenceManager.GeofenceRegistration.this.m4331x77389871((PendingIntent) obj);
                        }
                    };
                }
            }
            return null;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onLocationChanged$0$com-android-server-location-geofence-GeofenceManager$GeofenceRegistration  reason: not valid java name */
        public /* synthetic */ void m4330x438a6db0(PendingIntent pendingIntent) throws Exception {
            sendIntent(pendingIntent, true);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onLocationChanged$1$com-android-server-location-geofence-GeofenceManager$GeofenceRegistration  reason: not valid java name */
        public /* synthetic */ void m4331x77389871(PendingIntent pendingIntent) throws Exception {
            sendIntent(pendingIntent, false);
        }

        private void sendIntent(PendingIntent pendingIntent, boolean entering) {
            Intent intent = new Intent().putExtra("entering", entering);
            this.mWakeLock.acquire(30000L);
            try {
                pendingIntent.send(GeofenceManager.this.mContext, 0, intent, new PendingIntent.OnFinished() { // from class: com.android.server.location.geofence.GeofenceManager$GeofenceRegistration$$ExternalSyntheticLambda0
                    @Override // android.app.PendingIntent.OnFinished
                    public final void onSendFinished(PendingIntent pendingIntent2, Intent intent2, int i, String str, Bundle bundle) {
                        GeofenceManager.GeofenceRegistration.this.m4332x4a804db0(pendingIntent2, intent2, i, str, bundle);
                    }
                }, null, null, PendingIntentUtils.createDontSendToRestrictedAppsBundle(null));
            } catch (PendingIntent.CanceledException e) {
                this.mWakeLock.release();
                GeofenceManager.this.removeRegistration(new GeofenceKey(pendingIntent, getRequest()), this);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$sendIntent$2$com-android-server-location-geofence-GeofenceManager$GeofenceRegistration  reason: not valid java name */
        public /* synthetic */ void m4332x4a804db0(PendingIntent pI, Intent i, int rC, String rD, Bundle rE) {
            this.mWakeLock.release();
        }

        @Override // com.android.server.location.listeners.RequestListenerRegistration, com.android.server.location.listeners.ListenerRegistration
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(getIdentity());
            ArraySet<String> flags = new ArraySet<>(1);
            if (!this.mPermitted) {
                flags.add("na");
            }
            if (!flags.isEmpty()) {
                builder.append(" ").append(flags);
            }
            builder.append(" ").append(getRequest());
            return builder.toString();
        }
    }

    public GeofenceManager(Context context, Injector injector) {
        this.mContext = context.createAttributionContext(ATTRIBUTION_TAG);
        this.mUserInfoHelper = injector.getUserInfoHelper();
        this.mSettingsHelper = injector.getSettingsHelper();
        this.mLocationPermissionsHelper = injector.getLocationPermissionsHelper();
        this.mLocationUsageLogger = injector.getLocationUsageLogger();
    }

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public String getTag() {
        return TAG;
    }

    private LocationManager getLocationManager() {
        LocationManager locationManager;
        synchronized (this.mLock) {
            if (this.mLocationManager == null) {
                this.mLocationManager = (LocationManager) Objects.requireNonNull((LocationManager) this.mContext.getSystemService(LocationManager.class));
            }
            locationManager = this.mLocationManager;
        }
        return locationManager;
    }

    public void addGeofence(Geofence geofence, PendingIntent pendingIntent, String packageName, String attributionTag) {
        LocationPermissions.enforceCallingOrSelfLocationPermission(this.mContext, 2);
        CallerIdentity identity = CallerIdentity.fromBinder(this.mContext, packageName, attributionTag, AppOpsManager.toReceiverId(pendingIntent));
        long ident = Binder.clearCallingIdentity();
        try {
            putRegistration(new GeofenceKey(pendingIntent, geofence), new GeofenceRegistration(geofence, identity, pendingIntent));
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void removeGeofence(final PendingIntent pendingIntent) {
        long identity = Binder.clearCallingIdentity();
        try {
            removeRegistrationIf(new Predicate() { // from class: com.android.server.location.geofence.GeofenceManager$$ExternalSyntheticLambda3
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean equals;
                    equals = ((GeofenceKey) obj).getPendingIntent().equals(pendingIntent);
                    return equals;
                }
            });
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public boolean isActive(GeofenceRegistration registration) {
        return registration.isPermitted() && isActive(registration.getIdentity());
    }

    private boolean isActive(CallerIdentity identity) {
        return identity.isSystemServer() ? this.mSettingsHelper.isLocationEnabled(this.mUserInfoHelper.getCurrentUserId()) : this.mSettingsHelper.isLocationEnabled(identity.getUserId()) && this.mUserInfoHelper.isCurrentUserId(identity.getUserId()) && !this.mSettingsHelper.isLocationPackageBlacklisted(identity.getUserId(), identity.getPackageName());
    }

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    protected void onRegister() {
        this.mUserInfoHelper.addListener(this.mUserChangedListener);
        this.mSettingsHelper.addOnLocationEnabledChangedListener(this.mLocationEnabledChangedListener);
        this.mSettingsHelper.addOnLocationPackageBlacklistChangedListener(this.mLocationPackageBlacklistChangedListener);
        this.mLocationPermissionsHelper.addListener(this.mLocationPermissionsListener);
    }

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    protected void onUnregister() {
        this.mUserInfoHelper.removeListener(this.mUserChangedListener);
        this.mSettingsHelper.removeOnLocationEnabledChangedListener(this.mLocationEnabledChangedListener);
        this.mSettingsHelper.removeOnLocationPackageBlacklistChangedListener(this.mLocationPackageBlacklistChangedListener);
        this.mLocationPermissionsHelper.removeListener(this.mLocationPermissionsListener);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public void onRegistrationAdded(GeofenceKey key, GeofenceRegistration registration) {
        this.mLocationUsageLogger.logLocationApiUsage(1, 4, registration.getIdentity().getPackageName(), registration.getIdentity().getAttributionTag(), null, null, false, true, registration.getRequest(), true);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public void onRegistrationRemoved(GeofenceKey key, GeofenceRegistration registration) {
        this.mLocationUsageLogger.logLocationApiUsage(1, 4, registration.getIdentity().getPackageName(), registration.getIdentity().getAttributionTag(), null, null, false, true, registration.getRequest(), true);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public boolean registerWithService(LocationRequest locationRequest, Collection<GeofenceRegistration> registrations) {
        getLocationManager().requestLocationUpdates("fused", locationRequest, FgThread.getExecutor(), this);
        return true;
    }

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    protected void unregisterWithService() {
        synchronized (this.mLock) {
            getLocationManager().removeUpdates(this);
            this.mLastLocation = null;
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public LocationRequest mergeRegistrations(Collection<GeofenceRegistration> registrations) {
        long intervalMs;
        Location location = getLastLocation();
        long realtimeMs = SystemClock.elapsedRealtime();
        WorkSource workSource = null;
        double minFenceDistanceM = Double.MAX_VALUE;
        for (GeofenceRegistration registration : registrations) {
            if (!registration.getRequest().isExpired(realtimeMs)) {
                workSource = registration.getIdentity().addToWorkSource(workSource);
                if (location != null) {
                    double fenceDistanceM = registration.getDistanceToBoundary(location);
                    if (fenceDistanceM < minFenceDistanceM) {
                        minFenceDistanceM = fenceDistanceM;
                    }
                }
            }
        }
        if (Double.compare(minFenceDistanceM, Double.MAX_VALUE) < 0) {
            intervalMs = (long) Math.min(7200000.0d, Math.max(this.mSettingsHelper.getBackgroundThrottleProximityAlertIntervalMs(), (1000.0d * minFenceDistanceM) / 100.0d));
        } else {
            intervalMs = this.mSettingsHelper.getBackgroundThrottleProximityAlertIntervalMs();
        }
        return new LocationRequest.Builder(intervalMs).setMinUpdateIntervalMillis(0L).setHiddenFromAppOps(true).setWorkSource(workSource).build();
    }

    @Override // android.location.LocationListener
    public void onLocationChanged(final Location location) {
        synchronized (this.mLock) {
            this.mLastLocation = location;
        }
        deliverToListeners(new Function() { // from class: com.android.server.location.geofence.GeofenceManager$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                ListenerExecutor.ListenerOperation onLocationChanged;
                onLocationChanged = ((GeofenceManager.GeofenceRegistration) obj).onLocationChanged(location);
                return onLocationChanged;
            }
        });
        updateService();
    }

    Location getLastLocation() {
        Location location;
        synchronized (this.mLock) {
            location = this.mLastLocation;
        }
        if (location == null) {
            location = getLocationManager().getLastLocation();
        }
        if (location != null && location.getElapsedRealtimeAgeMillis() > 300000) {
            return null;
        }
        return location;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUserChanged(final int userId, int change) {
        if (change == 1) {
            updateRegistrations(new Predicate() { // from class: com.android.server.location.geofence.GeofenceManager$$ExternalSyntheticLambda5
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return GeofenceManager.lambda$onUserChanged$2(userId, (GeofenceManager.GeofenceRegistration) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$onUserChanged$2(int userId, GeofenceRegistration registration) {
        return registration.getIdentity().getUserId() == userId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$onLocationEnabledChanged$3(int userId, GeofenceRegistration registration) {
        return registration.getIdentity().getUserId() == userId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onLocationEnabledChanged(final int userId) {
        updateRegistrations(new Predicate() { // from class: com.android.server.location.geofence.GeofenceManager$$ExternalSyntheticLambda2
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return GeofenceManager.lambda$onLocationEnabledChanged$3(userId, (GeofenceManager.GeofenceRegistration) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$onLocationPackageBlacklistChanged$4(int userId, GeofenceRegistration registration) {
        return registration.getIdentity().getUserId() == userId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onLocationPackageBlacklistChanged(final int userId) {
        updateRegistrations(new Predicate() { // from class: com.android.server.location.geofence.GeofenceManager$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return GeofenceManager.lambda$onLocationPackageBlacklistChanged$4(userId, (GeofenceManager.GeofenceRegistration) obj);
            }
        });
    }

    void onLocationPermissionsChanged(final String packageName) {
        updateRegistrations(new Predicate() { // from class: com.android.server.location.geofence.GeofenceManager$$ExternalSyntheticLambda4
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean onLocationPermissionsChanged;
                onLocationPermissionsChanged = ((GeofenceManager.GeofenceRegistration) obj).onLocationPermissionsChanged(packageName);
                return onLocationPermissionsChanged;
            }
        });
    }

    void onLocationPermissionsChanged(final int uid) {
        updateRegistrations(new Predicate() { // from class: com.android.server.location.geofence.GeofenceManager$$ExternalSyntheticLambda9
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean onLocationPermissionsChanged;
                onLocationPermissionsChanged = ((GeofenceManager.GeofenceRegistration) obj).onLocationPermissionsChanged(uid);
                return onLocationPermissionsChanged;
            }
        });
    }
}
