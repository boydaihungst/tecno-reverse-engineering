package com.android.server.location.provider;

import android.app.AlarmManager;
import android.app.BroadcastOptions;
import android.app.PendingIntent;
import android.app.compat.CompatChanges;
import android.content.Context;
import android.content.Intent;
import android.location.ILocationCallback;
import android.location.ILocationListener;
import android.location.LastLocationRequest;
import android.location.Location;
import android.location.LocationManagerInternal;
import android.location.LocationRequest;
import android.location.LocationResult;
import android.location.provider.IProviderRequestListener;
import android.location.provider.ProviderProperties;
import android.location.provider.ProviderRequest;
import android.location.util.identity.CallerIdentity;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.IBinder;
import android.os.ICancellationSignal;
import android.os.IRemoteCallback;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.WorkSource;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.TimeUtils;
import com.android.internal.listeners.ListenerExecutor;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.Preconditions;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.job.controllers.JobStatus;
import com.android.server.location.LocationManagerService;
import com.android.server.location.LocationPermissions;
import com.android.server.location.eventlog.LocationEventLog;
import com.android.server.location.fudger.LocationFudger;
import com.android.server.location.injector.AlarmHelper;
import com.android.server.location.injector.AppForegroundHelper;
import com.android.server.location.injector.AppOpsHelper;
import com.android.server.location.injector.Injector;
import com.android.server.location.injector.LocationPermissionsHelper;
import com.android.server.location.injector.LocationPowerSaveModeHelper;
import com.android.server.location.injector.LocationUsageLogger;
import com.android.server.location.injector.ScreenInteractiveHelper;
import com.android.server.location.injector.SettingsHelper;
import com.android.server.location.injector.UserInfoHelper;
import com.android.server.location.listeners.ListenerMultiplexer;
import com.android.server.location.listeners.RemoteListenerRegistration;
import com.android.server.location.provider.AbstractLocationProvider;
import com.android.server.location.provider.LocationProviderManager;
import com.android.server.location.settings.LocationSettings;
import com.android.server.location.settings.LocationUserSettings;
import com.transsion.hubcore.server.location.ITranLocation;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class LocationProviderManager extends ListenerMultiplexer<Object, LocationTransport, Registration, ProviderRequest> implements AbstractLocationProvider.Listener {
    private static final float FASTEST_INTERVAL_JITTER_PERCENTAGE = 0.1f;
    private static final long MAX_CURRENT_LOCATION_AGE_MS = 30000;
    private static final int MAX_FASTEST_INTERVAL_JITTER_MS = 30000;
    private static final long MAX_GET_CURRENT_LOCATION_TIMEOUT_MS = 30000;
    private static final long MAX_HIGH_POWER_INTERVAL_MS = 300000;
    private static final long MIN_COARSE_INTERVAL_MS = 600000;
    private static final long MIN_REQUEST_DELAY_MS = 30000;
    private static final int STATE_STARTED = 0;
    private static final int STATE_STOPPED = 2;
    private static final int STATE_STOPPING = 1;
    private static final long TEMPORARY_APP_ALLOWLIST_DURATION_MS = 10000;
    private static final String WAKELOCK_TAG = "*location*";
    private static final long WAKELOCK_TIMEOUT_MS = 30000;
    private final SettingsHelper.GlobalSettingChangedListener mAdasPackageAllowlistChangedListener;
    protected final AlarmHelper mAlarmHelper;
    private final AppForegroundHelper.AppForegroundListener mAppForegroundChangedListener;
    protected final AppForegroundHelper mAppForegroundHelper;
    protected final AppOpsHelper mAppOpsHelper;
    private final SettingsHelper.GlobalSettingChangedListener mBackgroundThrottleIntervalChangedListener;
    private final SettingsHelper.GlobalSettingChangedListener mBackgroundThrottlePackageWhitelistChangedListener;
    protected final Context mContext;
    private AlarmManager.OnAlarmListener mDelayedRegister;
    private final SparseBooleanArray mEnabled;
    private final ArrayList<LocationManagerInternal.ProviderEnabledListener> mEnabledListeners;
    private final SettingsHelper.GlobalSettingChangedListener mIgnoreSettingsPackageWhitelistChangedListener;
    private final SparseArray<LastLocation> mLastLocations;
    private final SettingsHelper.UserSettingChangedListener mLocationEnabledChangedListener;
    protected final LocationFudger mLocationFudger;
    protected final LocationManagerInternal mLocationManagerInternal;
    private final SettingsHelper.UserSettingChangedListener mLocationPackageBlacklistChangedListener;
    protected final LocationPermissionsHelper mLocationPermissionsHelper;
    private final LocationPermissionsHelper.LocationPermissionsListener mLocationPermissionsListener;
    private final LocationPowerSaveModeHelper.LocationPowerSaveModeChangedListener mLocationPowerSaveModeChangedListener;
    protected final LocationPowerSaveModeHelper mLocationPowerSaveModeHelper;
    protected final LocationSettings mLocationSettings;
    protected final LocationUsageLogger mLocationUsageLogger;
    private final LocationSettings.LocationUserSettingsListener mLocationUserSettingsListener;
    protected final Object mLock;
    protected final String mName;
    private final PassiveLocationProviderManager mPassiveManager;
    protected final MockableLocationProvider mProvider;
    private final CopyOnWriteArrayList<IProviderRequestListener> mProviderRequestListeners;
    private final ScreenInteractiveHelper.ScreenInteractiveChangedListener mScreenInteractiveChangedListener;
    protected final ScreenInteractiveHelper mScreenInteractiveHelper;
    protected final SettingsHelper mSettingsHelper;
    private int mState;
    private StateChangedListener mStateChangedListener;
    private final UserInfoHelper.UserListener mUserChangedListener;
    protected final UserInfoHelper mUserHelper;

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public interface LocationTransport {
        void deliverOnFlushComplete(int i) throws Exception;

        void deliverOnLocationChanged(LocationResult locationResult, IRemoteCallback iRemoteCallback) throws Exception;
    }

    /* loaded from: classes.dex */
    protected interface ProviderTransport {
        void deliverOnProviderEnabledChanged(String str, boolean z) throws Exception;
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    private @interface State {
    }

    /* loaded from: classes.dex */
    public interface StateChangedListener {
        void onStateChanged(String str, AbstractLocationProvider.State state, AbstractLocationProvider.State state2);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public static final class LocationListenerTransport implements LocationTransport, ProviderTransport {
        private final ILocationListener mListener;

        LocationListenerTransport(ILocationListener listener) {
            this.mListener = (ILocationListener) Objects.requireNonNull(listener);
        }

        @Override // com.android.server.location.provider.LocationProviderManager.LocationTransport
        public void deliverOnLocationChanged(LocationResult locationResult, IRemoteCallback onCompleteCallback) throws RemoteException {
            try {
                this.mListener.onLocationChanged(locationResult.asList(), onCompleteCallback);
            } catch (RuntimeException e) {
                final RuntimeException wrapper = new RuntimeException(e);
                FgThread.getExecutor().execute(new Runnable() { // from class: com.android.server.location.provider.LocationProviderManager$LocationListenerTransport$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        LocationProviderManager.LocationListenerTransport.lambda$deliverOnLocationChanged$0(wrapper);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$deliverOnLocationChanged$0(RuntimeException wrapper) {
            throw wrapper;
        }

        @Override // com.android.server.location.provider.LocationProviderManager.LocationTransport
        public void deliverOnFlushComplete(int requestCode) throws RemoteException {
            try {
                this.mListener.onFlushComplete(requestCode);
            } catch (RuntimeException e) {
                final RuntimeException wrapper = new RuntimeException(e);
                FgThread.getExecutor().execute(new Runnable() { // from class: com.android.server.location.provider.LocationProviderManager$LocationListenerTransport$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        LocationProviderManager.LocationListenerTransport.lambda$deliverOnFlushComplete$1(wrapper);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$deliverOnFlushComplete$1(RuntimeException wrapper) {
            throw wrapper;
        }

        @Override // com.android.server.location.provider.LocationProviderManager.ProviderTransport
        public void deliverOnProviderEnabledChanged(String provider, boolean enabled) throws RemoteException {
            try {
                this.mListener.onProviderEnabledChanged(provider, enabled);
            } catch (RuntimeException e) {
                final RuntimeException wrapper = new RuntimeException(e);
                FgThread.getExecutor().execute(new Runnable() { // from class: com.android.server.location.provider.LocationProviderManager$LocationListenerTransport$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        LocationProviderManager.LocationListenerTransport.lambda$deliverOnProviderEnabledChanged$2(wrapper);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$deliverOnProviderEnabledChanged$2(RuntimeException wrapper) {
            throw wrapper;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public static final class LocationPendingIntentTransport implements LocationTransport, ProviderTransport {
        private final Context mContext;
        private final PendingIntent mPendingIntent;

        public LocationPendingIntentTransport(Context context, PendingIntent pendingIntent) {
            this.mContext = context;
            this.mPendingIntent = pendingIntent;
        }

        @Override // com.android.server.location.provider.LocationProviderManager.LocationTransport
        public void deliverOnLocationChanged(LocationResult locationResult, final IRemoteCallback onCompleteCallback) throws PendingIntent.CanceledException {
            BroadcastOptions options = BroadcastOptions.makeBasic();
            options.setDontSendToRestrictedApps(true);
            options.setTemporaryAppAllowlist(10000L, 0, (int) FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_LOCATION_PROVIDER, "");
            Intent intent = new Intent().putExtra("location", locationResult.getLastLocation());
            if (locationResult.size() > 1) {
                intent.putExtra("locations", (Parcelable[]) locationResult.asList().toArray(new Location[0]));
            }
            Runnable callback = null;
            if (onCompleteCallback != null) {
                callback = new Runnable() { // from class: com.android.server.location.provider.LocationProviderManager$LocationPendingIntentTransport$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        LocationProviderManager.LocationPendingIntentTransport.lambda$deliverOnLocationChanged$0(onCompleteCallback);
                    }
                };
            }
            PendingIntentSender.send(this.mPendingIntent, this.mContext, intent, callback, options.toBundle());
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$deliverOnLocationChanged$0(IRemoteCallback onCompleteCallback) {
            try {
                onCompleteCallback.sendResult((Bundle) null);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        @Override // com.android.server.location.provider.LocationProviderManager.LocationTransport
        public void deliverOnFlushComplete(int requestCode) throws PendingIntent.CanceledException {
            BroadcastOptions options = BroadcastOptions.makeBasic();
            options.setDontSendToRestrictedApps(true);
            options.setPendingIntentBackgroundActivityLaunchAllowed(false);
            this.mPendingIntent.send(this.mContext, 0, new Intent().putExtra("flushComplete", requestCode), null, null, null, options.toBundle());
        }

        @Override // com.android.server.location.provider.LocationProviderManager.ProviderTransport
        public void deliverOnProviderEnabledChanged(String provider, boolean enabled) throws PendingIntent.CanceledException {
            BroadcastOptions options = BroadcastOptions.makeBasic();
            options.setDontSendToRestrictedApps(true);
            this.mPendingIntent.send(this.mContext, 0, new Intent().putExtra("providerEnabled", enabled), null, null, null, options.toBundle());
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public static final class GetCurrentLocationTransport implements LocationTransport {
        private final ILocationCallback mCallback;

        GetCurrentLocationTransport(ILocationCallback callback) {
            this.mCallback = (ILocationCallback) Objects.requireNonNull(callback);
        }

        @Override // com.android.server.location.provider.LocationProviderManager.LocationTransport
        public void deliverOnLocationChanged(LocationResult locationResult, IRemoteCallback onCompleteCallback) throws RemoteException {
            Preconditions.checkState(onCompleteCallback == null);
            try {
                if (locationResult != null) {
                    this.mCallback.onLocation(locationResult.getLastLocation());
                } else {
                    this.mCallback.onLocation((Location) null);
                }
            } catch (RuntimeException e) {
                final RuntimeException wrapper = new RuntimeException(e);
                FgThread.getExecutor().execute(new Runnable() { // from class: com.android.server.location.provider.LocationProviderManager$GetCurrentLocationTransport$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        LocationProviderManager.GetCurrentLocationTransport.lambda$deliverOnLocationChanged$0(wrapper);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$deliverOnLocationChanged$0(RuntimeException wrapper) {
            throw wrapper;
        }

        @Override // com.android.server.location.provider.LocationProviderManager.LocationTransport
        public void deliverOnFlushComplete(int requestCode) {
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public abstract class Registration extends RemoteListenerRegistration<LocationRequest, LocationTransport> {
        private boolean mForeground;
        private boolean mIsUsingHighPower;
        private Location mLastLocation;
        private final int mPermissionLevel;
        private boolean mPermitted;
        private LocationRequest mProviderLocationRequest;

        /* JADX INFO: Access modifiers changed from: package-private */
        public abstract ListenerExecutor.ListenerOperation<LocationTransport> acceptLocationChange(LocationResult locationResult);

        protected Registration(LocationRequest request, CallerIdentity identity, LocationTransport transport, int permissionLevel) {
            super((LocationRequest) Objects.requireNonNull(request), identity, transport);
            this.mLastLocation = null;
            Preconditions.checkArgument(identity.getListenerId() != null);
            Preconditions.checkArgument(permissionLevel > 0);
            Preconditions.checkArgument(!request.getWorkSource().isEmpty());
            this.mPermissionLevel = permissionLevel;
            this.mProviderLocationRequest = request;
        }

        @Override // com.android.server.location.listeners.RemovableListenerRegistration
        protected final void onRemovableListenerRegister() {
            if (Build.IS_DEBUGGABLE) {
                Preconditions.checkState(Thread.holdsLock(LocationProviderManager.this.mLock));
            }
            Log.d(LocationManagerService.TAG, LocationProviderManager.this.mName + " provider added registration from " + getIdentity() + " -> " + getRequest() + " permissionLevel: " + this.mPermissionLevel + " permitted: " + LocationProviderManager.this.mLocationPermissionsHelper.hasLocationPermissions(this.mPermissionLevel, getIdentity()));
            LocationManagerService.mtkPrintCtaLog(getIdentity().getPid(), getIdentity().getUid(), "requestLocationUpdates", "USE_LOCATION", LocationProviderManager.this.mName);
            LocationEventLog.EVENT_LOG.logProviderClientRegistered(LocationProviderManager.this.mName, getIdentity(), (LocationRequest) super.getRequest());
            this.mPermitted = LocationProviderManager.this.mLocationPermissionsHelper.hasLocationPermissions(this.mPermissionLevel, getIdentity());
            this.mForeground = LocationProviderManager.this.mAppForegroundHelper.isAppForeground(getIdentity().getUid());
            this.mProviderLocationRequest = calculateProviderLocationRequest();
            this.mIsUsingHighPower = isUsingHighPower();
            onProviderListenerRegister();
            if (this.mForeground) {
                LocationEventLog.EVENT_LOG.logProviderClientForeground(LocationProviderManager.this.mName, getIdentity());
            }
        }

        @Override // com.android.server.location.listeners.RemovableListenerRegistration
        protected final void onRemovableListenerUnregister() {
            if (Build.IS_DEBUGGABLE) {
                Preconditions.checkState(Thread.holdsLock(LocationProviderManager.this.mLock));
            }
            onProviderListenerUnregister();
            LocationEventLog.EVENT_LOG.logProviderClientUnregistered(LocationProviderManager.this.mName, getIdentity());
            Log.d(LocationManagerService.TAG, LocationProviderManager.this.mName + " provider removed registration from " + getIdentity());
        }

        protected void onProviderListenerRegister() {
        }

        protected void onProviderListenerUnregister() {
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.location.listeners.ListenerRegistration
        public final void onActive() {
            if (Build.IS_DEBUGGABLE) {
                Preconditions.checkState(Thread.holdsLock(LocationProviderManager.this.mLock));
            }
            Log.d(LocationManagerService.TAG, "registration onActive from uid: " + getIdentity().getUid() + " package: " + getIdentity().getPackageName());
            LocationEventLog.EVENT_LOG.logProviderClientActive(LocationProviderManager.this.mName, getIdentity());
            if (!getRequest().isHiddenFromAppOps()) {
                LocationProviderManager.this.mAppOpsHelper.startOpNoThrow(41, getIdentity());
            }
            onHighPowerUsageChanged();
            onProviderListenerActive();
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.location.listeners.ListenerRegistration
        public final void onInactive() {
            if (Build.IS_DEBUGGABLE) {
                Preconditions.checkState(Thread.holdsLock(LocationProviderManager.this.mLock));
            }
            Log.d(LocationManagerService.TAG, "registration onInactive from uid: " + getIdentity().getUid() + " package: " + getIdentity().getPackageName());
            onHighPowerUsageChanged();
            if (!getRequest().isHiddenFromAppOps()) {
                LocationProviderManager.this.mAppOpsHelper.finishOp(41, getIdentity());
            }
            onProviderListenerInactive();
            LocationEventLog.EVENT_LOG.logProviderClientInactive(LocationProviderManager.this.mName, getIdentity());
        }

        protected void onProviderListenerActive() {
        }

        protected void onProviderListenerInactive() {
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // com.android.server.location.listeners.RequestListenerRegistration
        public final LocationRequest getRequest() {
            return this.mProviderLocationRequest;
        }

        final void setLastDeliveredLocation(Location location) {
            this.mLastLocation = location;
        }

        public final Location getLastDeliveredLocation() {
            return this.mLastLocation;
        }

        public int getPermissionLevel() {
            return this.mPermissionLevel;
        }

        public final boolean isForeground() {
            return this.mForeground;
        }

        public final boolean isPermitted() {
            return this.mPermitted;
        }

        public final void flush(final int requestCode) {
            LocationProviderManager.this.mProvider.getController().flush(new Runnable() { // from class: com.android.server.location.provider.LocationProviderManager$Registration$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    LocationProviderManager.Registration.this.m4534x56ecbe6a(requestCode);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$flush$1$com-android-server-location-provider-LocationProviderManager$Registration  reason: not valid java name */
        public /* synthetic */ void m4534x56ecbe6a(final int requestCode) {
            executeOperation(new ListenerExecutor.ListenerOperation() { // from class: com.android.server.location.provider.LocationProviderManager$Registration$$ExternalSyntheticLambda0
                public final void operate(Object obj) {
                    ((LocationProviderManager.LocationTransport) obj).deliverOnFlushComplete(requestCode);
                }
            });
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // com.android.server.location.listeners.RemovableListenerRegistration
        public final LocationProviderManager getOwner() {
            return LocationProviderManager.this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public final boolean onProviderPropertiesChanged() {
            onHighPowerUsageChanged();
            return false;
        }

        private void onHighPowerUsageChanged() {
            boolean isUsingHighPower = isUsingHighPower();
            if (isUsingHighPower != this.mIsUsingHighPower) {
                this.mIsUsingHighPower = isUsingHighPower;
                if (!getRequest().isHiddenFromAppOps()) {
                    if (this.mIsUsingHighPower) {
                        LocationProviderManager.this.mAppOpsHelper.startOpNoThrow(42, getIdentity());
                    } else {
                        LocationProviderManager.this.mAppOpsHelper.finishOp(42, getIdentity());
                    }
                }
            }
        }

        private boolean isUsingHighPower() {
            if (Build.IS_DEBUGGABLE) {
                Preconditions.checkState(Thread.holdsLock(LocationProviderManager.this.mLock));
            }
            ProviderProperties properties = LocationProviderManager.this.getProperties();
            return properties != null && isActive() && getRequest().getIntervalMillis() < 300000 && properties.getPowerUsage() == 3;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public final boolean onLocationPermissionsChanged(String packageName) {
            if (packageName == null || getIdentity().getPackageName().equals(packageName)) {
                return onLocationPermissionsChanged();
            }
            return false;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public final boolean onLocationPermissionsChanged(int uid) {
            if (getIdentity().getUid() == uid) {
                return onLocationPermissionsChanged();
            }
            return false;
        }

        private boolean onLocationPermissionsChanged() {
            if (Build.IS_DEBUGGABLE) {
                Preconditions.checkState(Thread.holdsLock(LocationProviderManager.this.mLock));
            }
            boolean permitted = LocationProviderManager.this.mLocationPermissionsHelper.hasLocationPermissions(this.mPermissionLevel, getIdentity());
            if (permitted != this.mPermitted) {
                if (LocationManagerService.D) {
                    Log.v(LocationManagerService.TAG, LocationProviderManager.this.mName + " provider package " + getIdentity().getPackageName() + " permitted = " + permitted);
                }
                this.mPermitted = permitted;
                if (permitted) {
                    LocationEventLog.EVENT_LOG.logProviderClientPermitted(LocationProviderManager.this.mName, getIdentity());
                    return true;
                }
                LocationEventLog.EVENT_LOG.logProviderClientUnpermitted(LocationProviderManager.this.mName, getIdentity());
                return true;
            }
            return false;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public final boolean onAdasGnssLocationEnabledChanged(int userId) {
            if (Build.IS_DEBUGGABLE) {
                Preconditions.checkState(Thread.holdsLock(LocationProviderManager.this.mLock));
            }
            if (getIdentity().getUserId() == userId) {
                return onProviderLocationRequestChanged();
            }
            return false;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public final boolean onForegroundChanged(int uid, boolean foreground) {
            if (Build.IS_DEBUGGABLE) {
                Preconditions.checkState(Thread.holdsLock(LocationProviderManager.this.mLock));
            }
            if (getIdentity().getUid() != uid || foreground == this.mForeground) {
                return false;
            }
            Log.v(LocationManagerService.TAG, LocationProviderManager.this.mName + " provider uid: " + uid + " foreground = " + foreground + " package: " + getIdentity().getPackageName());
            this.mForeground = foreground;
            if (foreground) {
                LocationEventLog.EVENT_LOG.logProviderClientForeground(LocationProviderManager.this.mName, getIdentity());
            } else {
                LocationEventLog.EVENT_LOG.logProviderClientBackground(LocationProviderManager.this.mName, getIdentity());
            }
            return onProviderLocationRequestChanged() || LocationProviderManager.this.mLocationPowerSaveModeHelper.getLocationPowerSaveMode() == 3;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public final boolean onProviderLocationRequestChanged() {
            if (Build.IS_DEBUGGABLE) {
                Preconditions.checkState(Thread.holdsLock(LocationProviderManager.this.mLock));
            }
            LocationRequest newRequest = calculateProviderLocationRequest();
            if (this.mProviderLocationRequest.equals(newRequest)) {
                return false;
            }
            LocationRequest oldRequest = this.mProviderLocationRequest;
            this.mProviderLocationRequest = newRequest;
            onHighPowerUsageChanged();
            LocationProviderManager.this.updateService();
            return oldRequest.isBypass() != newRequest.isBypass();
        }

        private LocationRequest calculateProviderLocationRequest() {
            LocationRequest baseRequest = (LocationRequest) super.getRequest();
            LocationRequest.Builder builder = new LocationRequest.Builder(baseRequest);
            if (this.mPermissionLevel < 2) {
                builder.setQuality(104);
                if (baseRequest.getIntervalMillis() < 600000) {
                    builder.setIntervalMillis(600000L);
                }
                if (baseRequest.getMinUpdateIntervalMillis() < 600000) {
                    builder.setMinUpdateIntervalMillis(600000L);
                }
            }
            boolean locationSettingsIgnored = baseRequest.isLocationSettingsIgnored();
            if (locationSettingsIgnored) {
                if (!LocationProviderManager.this.mSettingsHelper.getIgnoreSettingsAllowlist().contains(getIdentity().getPackageName(), getIdentity().getAttributionTag()) && !LocationProviderManager.this.mLocationManagerInternal.isProvider((String) null, getIdentity())) {
                    locationSettingsIgnored = false;
                }
                builder.setLocationSettingsIgnored(locationSettingsIgnored);
            }
            boolean adasGnssBypass = baseRequest.isAdasGnssBypass();
            if (adasGnssBypass) {
                if (!"gps".equals(LocationProviderManager.this.mName)) {
                    Log.e(LocationManagerService.TAG, "adas gnss bypass request received in non-gps provider");
                    adasGnssBypass = false;
                } else if (!LocationProviderManager.this.mLocationSettings.getUserSettings(getIdentity().getUserId()).isAdasGnssLocationEnabled()) {
                    adasGnssBypass = false;
                } else if (!LocationProviderManager.this.mSettingsHelper.getAdasAllowlist().contains(getIdentity().getPackageName(), getIdentity().getAttributionTag())) {
                    adasGnssBypass = false;
                }
                builder.setAdasGnssBypass(adasGnssBypass);
            }
            if (!locationSettingsIgnored && !isThrottlingExempt() && !this.mForeground) {
                builder.setIntervalMillis(Math.max(baseRequest.getIntervalMillis(), LocationProviderManager.this.mSettingsHelper.getBackgroundThrottleIntervalMs()));
            }
            return builder.build();
        }

        private boolean isThrottlingExempt() {
            if (LocationProviderManager.this.mSettingsHelper.getBackgroundThrottlePackageWhitelist().contains(getIdentity().getPackageName())) {
                return true;
            }
            return LocationProviderManager.this.mLocationManagerInternal.isProvider((String) null, getIdentity());
        }

        @Override // com.android.server.location.listeners.RequestListenerRegistration, com.android.server.location.listeners.ListenerRegistration
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(getIdentity());
            ArraySet<String> flags = new ArraySet<>(2);
            if (!isForeground()) {
                flags.add("bg");
            }
            if (!isPermitted()) {
                flags.add("na");
            }
            if (!flags.isEmpty()) {
                builder.append(" ").append(flags);
            }
            if (this.mPermissionLevel == 1) {
                builder.append(" (COARSE)");
            }
            builder.append(" ").append(getRequest());
            return builder.toString();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public abstract class LocationRegistration extends Registration implements AlarmManager.OnAlarmListener, LocationManagerInternal.ProviderEnabledListener {
        private long mExpirationRealtimeMs;
        private int mNumLocationsDelivered;
        private volatile ProviderTransport mProviderTransport;
        final PowerManager.WakeLock mWakeLock;
        final ExternalWakeLockReleaser mWakeLockReleaser;

        /* JADX INFO: Access modifiers changed from: protected */
        public abstract void onProviderOperationFailure(ListenerExecutor.ListenerOperation<ProviderTransport> listenerOperation, Exception exc);

        protected <TTransport extends LocationTransport & ProviderTransport> LocationRegistration(LocationRequest request, CallerIdentity identity, TTransport transport, int permissionLevel) {
            super(request, identity, transport, permissionLevel);
            this.mNumLocationsDelivered = 0;
            this.mExpirationRealtimeMs = JobStatus.NO_LATEST_RUNTIME;
            this.mProviderTransport = transport;
            PowerManager.WakeLock newWakeLock = ((PowerManager) Objects.requireNonNull((PowerManager) LocationProviderManager.this.mContext.getSystemService(PowerManager.class))).newWakeLock(1, LocationProviderManager.WAKELOCK_TAG);
            this.mWakeLock = newWakeLock;
            newWakeLock.setReferenceCounted(true);
            newWakeLock.setWorkSource(request.getWorkSource());
            this.mWakeLockReleaser = new ExternalWakeLockReleaser(identity, newWakeLock);
        }

        @Override // com.android.server.location.listeners.ListenerRegistration
        protected void onListenerUnregister() {
            this.mProviderTransport = null;
        }

        @Override // com.android.server.location.provider.LocationProviderManager.Registration
        protected final void onProviderListenerRegister() {
            long registerTimeMs = SystemClock.elapsedRealtime();
            long expirationRealtimeMs = getRequest().getExpirationRealtimeMs(registerTimeMs);
            this.mExpirationRealtimeMs = expirationRealtimeMs;
            if (expirationRealtimeMs <= registerTimeMs) {
                onAlarm();
            } else if (expirationRealtimeMs < JobStatus.NO_LATEST_RUNTIME) {
                LocationProviderManager.this.mAlarmHelper.setDelayedAlarm(this.mExpirationRealtimeMs - registerTimeMs, this, null);
            }
            LocationProviderManager.this.addEnabledListener(this);
            onLocationListenerRegister();
            int userId = getIdentity().getUserId();
            if (!LocationProviderManager.this.isEnabled(userId)) {
                onProviderEnabledChanged(LocationProviderManager.this.mName, userId, false);
            }
        }

        @Override // com.android.server.location.provider.LocationProviderManager.Registration
        protected final void onProviderListenerUnregister() {
            LocationProviderManager.this.removeEnabledListener(this);
            if (this.mExpirationRealtimeMs < JobStatus.NO_LATEST_RUNTIME) {
                LocationProviderManager.this.mAlarmHelper.cancel(this);
            }
            onLocationListenerUnregister();
        }

        protected void onLocationListenerRegister() {
        }

        protected void onLocationListenerUnregister() {
        }

        @Override // com.android.server.location.provider.LocationProviderManager.Registration
        protected final void onProviderListenerActive() {
            Location lastLocation;
            if (CompatChanges.isChangeEnabled(73144566L, getIdentity().getUid())) {
                long maxLocationAgeMs = getRequest().getIntervalMillis();
                Location lastDeliveredLocation = getLastDeliveredLocation();
                if (lastDeliveredLocation != null) {
                    maxLocationAgeMs = Math.min(maxLocationAgeMs, lastDeliveredLocation.getElapsedRealtimeAgeMillis() - 1);
                }
                if (maxLocationAgeMs <= 30000 || (lastLocation = LocationProviderManager.this.getLastLocationUnsafe(getIdentity().getUserId(), getPermissionLevel(), getRequest().isBypass(), maxLocationAgeMs)) == null) {
                    return;
                }
                executeOperation(acceptLocationChange(LocationResult.wrap(new Location[]{lastLocation})));
            }
        }

        @Override // android.app.AlarmManager.OnAlarmListener
        public void onAlarm() {
            if (LocationManagerService.D) {
                Log.d(LocationManagerService.TAG, LocationProviderManager.this.mName + " provider registration " + getIdentity() + " expired at " + TimeUtils.formatRealtime(this.mExpirationRealtimeMs));
            }
            synchronized (LocationProviderManager.this.mLock) {
                this.mExpirationRealtimeMs = JobStatus.NO_LATEST_RUNTIME;
                remove();
            }
        }

        @Override // com.android.server.location.provider.LocationProviderManager.Registration
        ListenerExecutor.ListenerOperation<LocationTransport> acceptLocationChange(LocationResult fineLocationResult) {
            if (Build.IS_DEBUGGABLE) {
                Preconditions.checkState(Thread.holdsLock(LocationProviderManager.this.mLock));
            }
            if (SystemClock.elapsedRealtime() >= this.mExpirationRealtimeMs) {
                if (LocationManagerService.D) {
                    Log.d(LocationManagerService.TAG, LocationProviderManager.this.mName + " provider registration " + getIdentity() + " expired at " + TimeUtils.formatRealtime(this.mExpirationRealtimeMs));
                }
                remove();
                return null;
            }
            LocationResult permittedLocationResult = (LocationResult) Objects.requireNonNull(LocationProviderManager.this.getPermittedLocationResult(fineLocationResult, getPermissionLevel()));
            final LocationResult locationResult = permittedLocationResult.filter(new Predicate<Location>() { // from class: com.android.server.location.provider.LocationProviderManager.LocationRegistration.1
                private Location mPreviousLocation;

                {
                    this.mPreviousLocation = LocationRegistration.this.getLastDeliveredLocation();
                }

                /* JADX DEBUG: Method merged with bridge method */
                @Override // java.util.function.Predicate
                public boolean test(Location location) {
                    if (this.mPreviousLocation != null) {
                        long deltaMs = location.getElapsedRealtimeMillis() - this.mPreviousLocation.getElapsedRealtimeMillis();
                        long maxJitterMs = Math.min(((float) LocationRegistration.this.getRequest().getIntervalMillis()) * LocationProviderManager.FASTEST_INTERVAL_JITTER_PERCENTAGE, 30000L);
                        if (deltaMs < LocationRegistration.this.getRequest().getMinUpdateIntervalMillis() - maxJitterMs) {
                            if (LocationManagerService.D) {
                                Log.v(LocationManagerService.TAG, LocationProviderManager.this.mName + " provider registration " + LocationRegistration.this.getIdentity() + " dropped delivery - too fast");
                            }
                            return false;
                        }
                        double smallestDisplacementM = LocationRegistration.this.getRequest().getMinUpdateDistanceMeters();
                        if (smallestDisplacementM > 0.0d && location.distanceTo(this.mPreviousLocation) <= smallestDisplacementM) {
                            if (LocationManagerService.D) {
                                Log.v(LocationManagerService.TAG, LocationProviderManager.this.mName + " provider registration " + LocationRegistration.this.getIdentity() + " dropped delivery - too close");
                            }
                            return false;
                        }
                    }
                    this.mPreviousLocation = location;
                    return true;
                }
            });
            if (locationResult == null) {
                return null;
            }
            if (!LocationProviderManager.this.mAppOpsHelper.noteOpNoThrow(LocationPermissions.asAppOp(getPermissionLevel()), getIdentity()) && !LocationManagerService.mtkNoteCoarseLocationAccess(LocationProviderManager.this.mName, getIdentity())) {
                if (LocationManagerService.D) {
                    Log.w(LocationManagerService.TAG, "noteOp denied for " + getIdentity());
                }
                return null;
            }
            final boolean useWakeLock = getRequest().getIntervalMillis() != JobStatus.NO_LATEST_RUNTIME;
            return new ListenerExecutor.ListenerOperation<LocationTransport>() { // from class: com.android.server.location.provider.LocationProviderManager.LocationRegistration.2
                public void onPreExecute() {
                    LocationRegistration.this.setLastDeliveredLocation(locationResult.getLastLocation());
                    if (useWakeLock) {
                        LocationRegistration.this.mWakeLock.acquire(30000L);
                    }
                }

                /* JADX DEBUG: Method merged with bridge method */
                public void operate(LocationTransport listener) throws Exception {
                    LocationResult deliverLocationResult;
                    if (LocationRegistration.this.getIdentity().getPid() == Process.myPid()) {
                        deliverLocationResult = locationResult.deepCopy();
                    } else {
                        deliverLocationResult = locationResult;
                    }
                    listener.deliverOnLocationChanged(deliverLocationResult, useWakeLock ? LocationRegistration.this.mWakeLockReleaser : null);
                    LocationEventLog.EVENT_LOG.logProviderDeliveredLocations(LocationProviderManager.this.mName, locationResult.size(), LocationRegistration.this.getIdentity());
                }

                public void onPostExecute(boolean success) {
                    if (!success && useWakeLock) {
                        LocationRegistration.this.mWakeLock.release();
                    }
                    if (success) {
                        LocationRegistration locationRegistration = LocationRegistration.this;
                        int i = locationRegistration.mNumLocationsDelivered + 1;
                        locationRegistration.mNumLocationsDelivered = i;
                        boolean remove = i >= LocationRegistration.this.getRequest().getMaxUpdates();
                        if (remove) {
                            if (LocationManagerService.D) {
                                Log.d(LocationManagerService.TAG, LocationProviderManager.this.mName + " provider registration " + LocationRegistration.this.getIdentity() + " finished after " + LocationRegistration.this.mNumLocationsDelivered + " updates");
                            }
                            synchronized (LocationProviderManager.this.mLock) {
                                LocationRegistration.this.remove();
                            }
                        }
                    }
                }
            };
        }

        public void onProviderEnabledChanged(String provider, int userId, final boolean enabled) {
            Preconditions.checkState(LocationProviderManager.this.mName.equals(provider));
            Log.d(LocationManagerService.TAG, "onProviderEnabledChanged name: " + LocationProviderManager.this.mName + " enabled: " + enabled);
            if (userId != getIdentity().getUserId()) {
                return;
            }
            executeSafely(getExecutor(), new Supplier() { // from class: com.android.server.location.provider.LocationProviderManager$LocationRegistration$$ExternalSyntheticLambda0
                @Override // java.util.function.Supplier
                public final Object get() {
                    return LocationProviderManager.LocationRegistration.this.m4532x50345117();
                }
            }, new ListenerExecutor.ListenerOperation() { // from class: com.android.server.location.provider.LocationProviderManager$LocationRegistration$$ExternalSyntheticLambda1
                public final void operate(Object obj) {
                    LocationProviderManager.LocationRegistration.this.m4533xf7b02ad8(enabled, (LocationProviderManager.ProviderTransport) obj);
                }
            }, new ListenerExecutor.FailureCallback() { // from class: com.android.server.location.provider.LocationProviderManager$LocationRegistration$$ExternalSyntheticLambda2
                public final void onFailure(ListenerExecutor.ListenerOperation listenerOperation, Exception exc) {
                    LocationProviderManager.LocationRegistration.this.onProviderOperationFailure(listenerOperation, exc);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onProviderEnabledChanged$0$com-android-server-location-provider-LocationProviderManager$LocationRegistration  reason: not valid java name */
        public /* synthetic */ ProviderTransport m4532x50345117() {
            return this.mProviderTransport;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onProviderEnabledChanged$1$com-android-server-location-provider-LocationProviderManager$LocationRegistration  reason: not valid java name */
        public /* synthetic */ void m4533xf7b02ad8(boolean enabled, ProviderTransport listener) throws Exception {
            listener.deliverOnProviderEnabledChanged(LocationProviderManager.this.mName, enabled);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public final class LocationListenerRegistration extends LocationRegistration implements IBinder.DeathRecipient {
        LocationListenerRegistration(LocationRequest request, CallerIdentity identity, LocationListenerTransport transport, int permissionLevel) {
            super(request, identity, transport, permissionLevel);
        }

        @Override // com.android.server.location.provider.LocationProviderManager.LocationRegistration
        protected void onLocationListenerRegister() {
            try {
                ((IBinder) getKey()).linkToDeath(this, 0);
            } catch (RemoteException e) {
                remove();
            }
        }

        @Override // com.android.server.location.provider.LocationProviderManager.LocationRegistration
        protected void onLocationListenerUnregister() {
            ((IBinder) getKey()).unlinkToDeath(this, 0);
        }

        @Override // com.android.server.location.provider.LocationProviderManager.LocationRegistration
        protected void onProviderOperationFailure(ListenerExecutor.ListenerOperation<ProviderTransport> operation, Exception exception) {
            onTransportFailure(exception);
        }

        @Override // com.android.server.location.listeners.ListenerRegistration
        public void onOperationFailure(ListenerExecutor.ListenerOperation<LocationTransport> operation, Exception exception) {
            onTransportFailure(exception);
        }

        private void onTransportFailure(Exception e) {
            if (e instanceof RemoteException) {
                Log.w(LocationManagerService.TAG, LocationProviderManager.this.mName + " provider registration " + getIdentity() + " removed", e);
                synchronized (LocationProviderManager.this.mLock) {
                    remove();
                }
                return;
            }
            throw new AssertionError(e);
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            try {
                if (LocationManagerService.D) {
                    Log.d(LocationManagerService.TAG, LocationProviderManager.this.mName + " provider registration " + getIdentity() + " died");
                }
                synchronized (LocationProviderManager.this.mLock) {
                    remove();
                }
            } catch (RuntimeException e) {
                throw new AssertionError(e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public final class LocationPendingIntentRegistration extends LocationRegistration implements PendingIntent.CancelListener {
        LocationPendingIntentRegistration(LocationRequest request, CallerIdentity identity, LocationPendingIntentTransport transport, int permissionLevel) {
            super(request, identity, transport, permissionLevel);
        }

        @Override // com.android.server.location.provider.LocationProviderManager.LocationRegistration
        protected void onLocationListenerRegister() {
            if (!((PendingIntent) getKey()).addCancelListener(ConcurrentUtils.DIRECT_EXECUTOR, this)) {
                remove();
            }
        }

        @Override // com.android.server.location.provider.LocationProviderManager.LocationRegistration
        protected void onLocationListenerUnregister() {
            ((PendingIntent) getKey()).removeCancelListener(this);
        }

        @Override // com.android.server.location.provider.LocationProviderManager.LocationRegistration
        protected void onProviderOperationFailure(ListenerExecutor.ListenerOperation<ProviderTransport> operation, Exception exception) {
            onTransportFailure(exception);
        }

        @Override // com.android.server.location.listeners.ListenerRegistration
        public void onOperationFailure(ListenerExecutor.ListenerOperation<LocationTransport> operation, Exception exception) {
            onTransportFailure(exception);
        }

        private void onTransportFailure(Exception e) {
            if (e instanceof PendingIntent.CanceledException) {
                Log.w(LocationManagerService.TAG, LocationProviderManager.this.mName + " provider registration " + getIdentity() + " removed", e);
                synchronized (LocationProviderManager.this.mLock) {
                    remove();
                }
                return;
            }
            throw new AssertionError(e);
        }

        public void onCanceled(PendingIntent intent) {
            if (LocationManagerService.D) {
                Log.d(LocationManagerService.TAG, LocationProviderManager.this.mName + " provider registration " + getIdentity() + " canceled");
            }
            synchronized (LocationProviderManager.this.mLock) {
                remove();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public final class GetCurrentLocationListenerRegistration extends Registration implements IBinder.DeathRecipient, AlarmManager.OnAlarmListener {
        private long mExpirationRealtimeMs;

        GetCurrentLocationListenerRegistration(LocationRequest request, CallerIdentity identity, LocationTransport transport, int permissionLevel) {
            super(request, identity, transport, permissionLevel);
            this.mExpirationRealtimeMs = JobStatus.NO_LATEST_RUNTIME;
        }

        @Override // com.android.server.location.provider.LocationProviderManager.Registration
        protected void onProviderListenerRegister() {
            try {
                ((IBinder) getKey()).linkToDeath(this, 0);
            } catch (RemoteException e) {
                remove();
            }
            long registerTimeMs = SystemClock.elapsedRealtime();
            long expirationRealtimeMs = getRequest().getExpirationRealtimeMs(registerTimeMs);
            this.mExpirationRealtimeMs = expirationRealtimeMs;
            if (expirationRealtimeMs <= registerTimeMs) {
                onAlarm();
            } else if (expirationRealtimeMs < JobStatus.NO_LATEST_RUNTIME) {
                LocationProviderManager.this.mAlarmHelper.setDelayedAlarm(this.mExpirationRealtimeMs - registerTimeMs, this, null);
            }
        }

        @Override // com.android.server.location.provider.LocationProviderManager.Registration
        protected void onProviderListenerUnregister() {
            if (this.mExpirationRealtimeMs < JobStatus.NO_LATEST_RUNTIME) {
                LocationProviderManager.this.mAlarmHelper.cancel(this);
            }
            ((IBinder) getKey()).unlinkToDeath(this, 0);
        }

        @Override // com.android.server.location.provider.LocationProviderManager.Registration
        protected void onProviderListenerActive() {
            Location lastLocation = LocationProviderManager.this.getLastLocationUnsafe(getIdentity().getUserId(), getPermissionLevel(), getRequest().isBypass(), 30000L);
            if (lastLocation != null) {
                executeOperation(acceptLocationChange(LocationResult.wrap(new Location[]{lastLocation})));
            }
        }

        @Override // com.android.server.location.provider.LocationProviderManager.Registration
        protected void onProviderListenerInactive() {
            executeOperation(acceptLocationChange(null));
        }

        void deliverNull() {
            synchronized (LocationProviderManager.this.mLock) {
                executeOperation(acceptLocationChange(null));
            }
        }

        @Override // android.app.AlarmManager.OnAlarmListener
        public void onAlarm() {
            if (LocationManagerService.D) {
                Log.d(LocationManagerService.TAG, LocationProviderManager.this.mName + " provider registration " + getIdentity() + " expired at " + TimeUtils.formatRealtime(this.mExpirationRealtimeMs));
            }
            synchronized (LocationProviderManager.this.mLock) {
                this.mExpirationRealtimeMs = JobStatus.NO_LATEST_RUNTIME;
                executeOperation(acceptLocationChange(null));
            }
        }

        @Override // com.android.server.location.provider.LocationProviderManager.Registration
        ListenerExecutor.ListenerOperation<LocationTransport> acceptLocationChange(LocationResult fineLocationResult) {
            if (Build.IS_DEBUGGABLE) {
                Preconditions.checkState(Thread.holdsLock(LocationProviderManager.this.mLock));
            }
            if (SystemClock.elapsedRealtime() >= this.mExpirationRealtimeMs) {
                if (LocationManagerService.D) {
                    Log.d(LocationManagerService.TAG, LocationProviderManager.this.mName + " provider registration " + getIdentity() + " expired at " + TimeUtils.formatRealtime(this.mExpirationRealtimeMs));
                }
                fineLocationResult = null;
            }
            if (fineLocationResult != null && !LocationProviderManager.this.mAppOpsHelper.noteOpNoThrow(LocationPermissions.asAppOp(getPermissionLevel()), getIdentity()) && fineLocationResult != null && !LocationManagerService.mtkNoteCoarseLocationAccess(LocationProviderManager.this.mName, getIdentity())) {
                if (LocationManagerService.D) {
                    Log.w(LocationManagerService.TAG, "noteOp denied for " + getIdentity());
                }
                fineLocationResult = null;
            }
            if (fineLocationResult != null) {
                fineLocationResult = fineLocationResult.asLastLocationResult();
            }
            final LocationResult locationResult = LocationProviderManager.this.getPermittedLocationResult(fineLocationResult, getPermissionLevel());
            return new ListenerExecutor.ListenerOperation<LocationTransport>() { // from class: com.android.server.location.provider.LocationProviderManager.GetCurrentLocationListenerRegistration.1
                /* JADX DEBUG: Method merged with bridge method */
                public void operate(LocationTransport listener) throws Exception {
                    LocationResult deliverLocationResult;
                    LocationResult locationResult2;
                    if (GetCurrentLocationListenerRegistration.this.getIdentity().getPid() == Process.myPid() && (locationResult2 = locationResult) != null) {
                        deliverLocationResult = locationResult2.deepCopy();
                    } else {
                        deliverLocationResult = locationResult;
                    }
                    listener.deliverOnLocationChanged(deliverLocationResult, null);
                    LocationEventLog locationEventLog = LocationEventLog.EVENT_LOG;
                    String str = LocationProviderManager.this.mName;
                    LocationResult locationResult3 = locationResult;
                    locationEventLog.logProviderDeliveredLocations(str, locationResult3 != null ? locationResult3.size() : 0, GetCurrentLocationListenerRegistration.this.getIdentity());
                }

                public void onPostExecute(boolean success) {
                    if (success) {
                        synchronized (LocationProviderManager.this.mLock) {
                            GetCurrentLocationListenerRegistration.this.remove();
                        }
                    }
                }
            };
        }

        @Override // com.android.server.location.listeners.ListenerRegistration
        public void onOperationFailure(ListenerExecutor.ListenerOperation<LocationTransport> operation, Exception e) {
            if (e instanceof RemoteException) {
                Log.w(LocationManagerService.TAG, LocationProviderManager.this.mName + " provider registration " + getIdentity() + " removed", e);
                synchronized (LocationProviderManager.this.mLock) {
                    remove();
                }
                return;
            }
            throw new AssertionError(e);
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            try {
                if (LocationManagerService.D) {
                    Log.d(LocationManagerService.TAG, LocationProviderManager.this.mName + " provider registration " + getIdentity() + " died");
                }
                synchronized (LocationProviderManager.this.mLock) {
                    remove();
                }
            } catch (RuntimeException e) {
                throw new AssertionError(e);
            }
        }
    }

    public LocationProviderManager(Context context, Injector injector, String name, PassiveLocationProviderManager passiveManager) {
        Object obj = new Object();
        this.mLock = obj;
        this.mUserChangedListener = new UserInfoHelper.UserListener() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda7
            @Override // com.android.server.location.injector.UserInfoHelper.UserListener
            public final void onUserChanged(int i, int i2) {
                LocationProviderManager.this.onUserChanged(i, i2);
            }
        };
        this.mLocationUserSettingsListener = new LocationSettings.LocationUserSettingsListener() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda9
            @Override // com.android.server.location.settings.LocationSettings.LocationUserSettingsListener
            public final void onLocationUserSettingsChanged(int i, LocationUserSettings locationUserSettings, LocationUserSettings locationUserSettings2) {
                LocationProviderManager.this.onLocationUserSettingsChanged(i, locationUserSettings, locationUserSettings2);
            }
        };
        this.mLocationEnabledChangedListener = new SettingsHelper.UserSettingChangedListener() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda10
            @Override // com.android.server.location.injector.SettingsHelper.UserSettingChangedListener
            public final void onSettingChanged(int i) {
                LocationProviderManager.this.onLocationEnabledChanged(i);
            }
        };
        this.mBackgroundThrottlePackageWhitelistChangedListener = new SettingsHelper.GlobalSettingChangedListener() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda11
            @Override // com.android.server.location.injector.SettingsHelper.GlobalSettingChangedListener
            public final void onSettingChanged() {
                LocationProviderManager.this.onBackgroundThrottlePackageWhitelistChanged();
            }
        };
        this.mLocationPackageBlacklistChangedListener = new SettingsHelper.UserSettingChangedListener() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda12
            @Override // com.android.server.location.injector.SettingsHelper.UserSettingChangedListener
            public final void onSettingChanged(int i) {
                LocationProviderManager.this.onLocationPackageBlacklistChanged(i);
            }
        };
        this.mLocationPermissionsListener = new LocationPermissionsHelper.LocationPermissionsListener() { // from class: com.android.server.location.provider.LocationProviderManager.1
            @Override // com.android.server.location.injector.LocationPermissionsHelper.LocationPermissionsListener
            public void onLocationPermissionsChanged(String packageName) {
                LocationProviderManager.this.onLocationPermissionsChanged(packageName);
            }

            @Override // com.android.server.location.injector.LocationPermissionsHelper.LocationPermissionsListener
            public void onLocationPermissionsChanged(int uid) {
                LocationProviderManager.this.onLocationPermissionsChanged(uid);
            }
        };
        this.mAppForegroundChangedListener = new AppForegroundHelper.AppForegroundListener() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda13
            @Override // com.android.server.location.injector.AppForegroundHelper.AppForegroundListener
            public final void onAppForegroundChanged(int i, boolean z) {
                LocationProviderManager.this.onAppForegroundChanged(i, z);
            }
        };
        this.mBackgroundThrottleIntervalChangedListener = new SettingsHelper.GlobalSettingChangedListener() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda14
            @Override // com.android.server.location.injector.SettingsHelper.GlobalSettingChangedListener
            public final void onSettingChanged() {
                LocationProviderManager.this.onBackgroundThrottleIntervalChanged();
            }
        };
        this.mAdasPackageAllowlistChangedListener = new SettingsHelper.GlobalSettingChangedListener() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda15
            @Override // com.android.server.location.injector.SettingsHelper.GlobalSettingChangedListener
            public final void onSettingChanged() {
                LocationProviderManager.this.onAdasAllowlistChanged();
            }
        };
        this.mIgnoreSettingsPackageWhitelistChangedListener = new SettingsHelper.GlobalSettingChangedListener() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda16
            @Override // com.android.server.location.injector.SettingsHelper.GlobalSettingChangedListener
            public final void onSettingChanged() {
                LocationProviderManager.this.onIgnoreSettingsWhitelistChanged();
            }
        };
        this.mLocationPowerSaveModeChangedListener = new LocationPowerSaveModeHelper.LocationPowerSaveModeChangedListener() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda17
            @Override // com.android.server.location.injector.LocationPowerSaveModeHelper.LocationPowerSaveModeChangedListener
            public final void onLocationPowerSaveModeChanged(int i) {
                LocationProviderManager.this.onLocationPowerSaveModeChanged(i);
            }
        };
        this.mScreenInteractiveChangedListener = new ScreenInteractiveHelper.ScreenInteractiveChangedListener() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda8
            @Override // com.android.server.location.injector.ScreenInteractiveHelper.ScreenInteractiveChangedListener
            public final void onScreenInteractiveChanged(boolean z) {
                LocationProviderManager.this.onScreenInteractiveChanged(z);
            }
        };
        this.mContext = context;
        this.mName = (String) Objects.requireNonNull(name);
        this.mPassiveManager = passiveManager;
        this.mState = 2;
        this.mEnabled = new SparseBooleanArray(2);
        this.mLastLocations = new SparseArray<>(2);
        this.mEnabledListeners = new ArrayList<>();
        this.mProviderRequestListeners = new CopyOnWriteArrayList<>();
        this.mLocationManagerInternal = (LocationManagerInternal) Objects.requireNonNull((LocationManagerInternal) LocalServices.getService(LocationManagerInternal.class));
        this.mLocationSettings = injector.getLocationSettings();
        SettingsHelper settingsHelper = injector.getSettingsHelper();
        this.mSettingsHelper = settingsHelper;
        this.mUserHelper = injector.getUserInfoHelper();
        this.mAlarmHelper = injector.getAlarmHelper();
        this.mAppOpsHelper = injector.getAppOpsHelper();
        this.mLocationPermissionsHelper = injector.getLocationPermissionsHelper();
        this.mAppForegroundHelper = injector.getAppForegroundHelper();
        this.mLocationPowerSaveModeHelper = injector.getLocationPowerSaveModeHelper();
        this.mScreenInteractiveHelper = injector.getScreenInteractiveHelper();
        this.mLocationUsageLogger = injector.getLocationUsageLogger();
        this.mLocationFudger = new LocationFudger(settingsHelper.getCoarseLocationAccuracyM());
        MockableLocationProvider mockableLocationProvider = new MockableLocationProvider(obj);
        this.mProvider = mockableLocationProvider;
        mockableLocationProvider.getController().setListener(this);
    }

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public String getTag() {
        return LocationManagerService.TAG;
    }

    public void startManager(StateChangedListener listener) {
        synchronized (this.mLock) {
            Preconditions.checkState(this.mState == 2);
            this.mState = 0;
            this.mStateChangedListener = listener;
            this.mUserHelper.addListener(this.mUserChangedListener);
            this.mLocationSettings.registerLocationUserSettingsListener(this.mLocationUserSettingsListener);
            this.mSettingsHelper.addOnLocationEnabledChangedListener(this.mLocationEnabledChangedListener);
            long identity = Binder.clearCallingIdentity();
            this.mProvider.getController().start();
            onUserStarted(-1);
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void stopManager() {
        synchronized (this.mLock) {
            Preconditions.checkState(this.mState == 0);
            this.mState = 1;
            long identity = Binder.clearCallingIdentity();
            onEnabledChanged(-1);
            removeRegistrationIf(new Predicate() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda27
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return LocationProviderManager.lambda$stopManager$0(obj);
                }
            });
            this.mProvider.getController().stop();
            Binder.restoreCallingIdentity(identity);
            this.mUserHelper.removeListener(this.mUserChangedListener);
            this.mLocationSettings.unregisterLocationUserSettingsListener(this.mLocationUserSettingsListener);
            this.mSettingsHelper.removeOnLocationEnabledChangedListener(this.mLocationEnabledChangedListener);
            Preconditions.checkState(this.mEnabledListeners.isEmpty());
            this.mProviderRequestListeners.clear();
            this.mEnabled.clear();
            this.mLastLocations.clear();
            this.mStateChangedListener = null;
            this.mState = 2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$stopManager$0(Object key) {
        return true;
    }

    public String getName() {
        return this.mName;
    }

    public AbstractLocationProvider.State getState() {
        return this.mProvider.getState();
    }

    public CallerIdentity getProviderIdentity() {
        return this.mProvider.getState().identity;
    }

    public ProviderProperties getProperties() {
        return this.mProvider.getState().properties;
    }

    public boolean hasProvider() {
        return this.mProvider.getProvider() != null;
    }

    public boolean isEnabled(int userId) {
        boolean valueAt;
        if (userId == -10000) {
            return false;
        }
        if (userId == -2) {
            return isEnabled(this.mUserHelper.getCurrentUserId());
        }
        Preconditions.checkArgument(userId >= 0);
        synchronized (this.mLock) {
            int index = this.mEnabled.indexOfKey(userId);
            if (index < 0) {
                Log.w(LocationManagerService.TAG, this.mName + " provider saw user " + userId + " unexpectedly");
                onEnabledChanged(userId);
                index = this.mEnabled.indexOfKey(userId);
            }
            valueAt = this.mEnabled.valueAt(index);
        }
        return valueAt;
    }

    public void addEnabledListener(LocationManagerInternal.ProviderEnabledListener listener) {
        synchronized (this.mLock) {
            Preconditions.checkState(this.mState != 2);
            this.mEnabledListeners.add(listener);
        }
    }

    public void removeEnabledListener(LocationManagerInternal.ProviderEnabledListener listener) {
        synchronized (this.mLock) {
            Preconditions.checkState(this.mState != 2);
            this.mEnabledListeners.remove(listener);
        }
    }

    public void addProviderRequestListener(IProviderRequestListener listener) {
        this.mProviderRequestListeners.add(listener);
    }

    public void removeProviderRequestListener(IProviderRequestListener listener) {
        this.mProviderRequestListeners.remove(listener);
    }

    public void setRealProvider(AbstractLocationProvider provider) {
        synchronized (this.mLock) {
            Preconditions.checkState(this.mState != 2);
            long identity = Binder.clearCallingIdentity();
            this.mProvider.setRealProvider(provider);
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void setMockProvider(MockLocationProvider provider) {
        synchronized (this.mLock) {
            Preconditions.checkState(this.mState != 2);
            LocationEventLog.EVENT_LOG.logProviderMocked(this.mName, provider != null);
            long identity = Binder.clearCallingIdentity();
            this.mProvider.setMockProvider(provider);
            Binder.restoreCallingIdentity(identity);
            if (provider == null) {
                int lastLocationSize = this.mLastLocations.size();
                for (int i = 0; i < lastLocationSize; i++) {
                    this.mLastLocations.valueAt(i).clearMock();
                }
                this.mLocationFudger.resetOffsets();
            }
        }
    }

    public void setMockProviderAllowed(boolean enabled) {
        synchronized (this.mLock) {
            if (!this.mProvider.isMock()) {
                throw new IllegalArgumentException(this.mName + " provider is not a test provider");
            }
            long identity = Binder.clearCallingIdentity();
            this.mProvider.setMockProviderAllowed(enabled);
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void setMockProviderLocation(Location location) {
        synchronized (this.mLock) {
            if (!this.mProvider.isMock()) {
                throw new IllegalArgumentException(this.mName + " provider is not a test provider");
            }
            String locationProvider = location.getProvider();
            if (!TextUtils.isEmpty(locationProvider) && !this.mName.equals(locationProvider)) {
                EventLog.writeEvent(1397638484, "33091107", Integer.valueOf(Binder.getCallingUid()), this.mName + "!=" + locationProvider);
            }
            long identity = Binder.clearCallingIdentity();
            this.mProvider.setMockProviderLocation(location);
            Binder.restoreCallingIdentity(identity);
        }
    }

    public Location getLastLocation(LastLocationRequest request, CallerIdentity identity, int permissionLevel) {
        LastLocationRequest request2 = calculateLastLocationRequest(request, identity);
        if (isActive(request2.isBypass(), identity)) {
            if (!this.mAppOpsHelper.noteOpNoThrow(LocationPermissions.asAppOp(permissionLevel), identity) && !LocationManagerService.mtkNoteCoarseLocationAccess(this.mName, identity)) {
                if (LocationManagerService.D) {
                    Log.w(LocationManagerService.TAG, "noteOp denied for " + identity);
                }
                return null;
            }
            Location location = getPermittedLocation(getLastLocationUnsafe(identity.getUserId(), permissionLevel, request2.isBypass(), JobStatus.NO_LATEST_RUNTIME), permissionLevel);
            if (location != null && identity.getPid() == Process.myPid()) {
                location = new Location(location);
            }
            LocationManagerService.mtkPrintCtaLog(identity.getPid(), identity.getUid(), "getLastLocation", "READ_LOCATION_INFO", this.mName);
            return location;
        }
        return null;
    }

    private LastLocationRequest calculateLastLocationRequest(LastLocationRequest baseRequest, CallerIdentity identity) {
        LastLocationRequest.Builder builder = new LastLocationRequest.Builder(baseRequest);
        boolean locationSettingsIgnored = baseRequest.isLocationSettingsIgnored();
        if (locationSettingsIgnored) {
            if (!this.mSettingsHelper.getIgnoreSettingsAllowlist().contains(identity.getPackageName(), identity.getAttributionTag()) && !this.mLocationManagerInternal.isProvider((String) null, identity)) {
                locationSettingsIgnored = false;
            }
            builder.setLocationSettingsIgnored(locationSettingsIgnored);
        }
        boolean adasGnssBypass = baseRequest.isAdasGnssBypass();
        if (adasGnssBypass) {
            if (!"gps".equals(this.mName)) {
                Log.e(LocationManagerService.TAG, "adas gnss bypass request received in non-gps provider");
                adasGnssBypass = false;
            } else if (!this.mLocationSettings.getUserSettings(identity.getUserId()).isAdasGnssLocationEnabled()) {
                adasGnssBypass = false;
            } else if (!this.mSettingsHelper.getAdasAllowlist().contains(identity.getPackageName(), identity.getAttributionTag())) {
                adasGnssBypass = false;
            }
            builder.setAdasGnssBypass(adasGnssBypass);
        }
        return builder.build();
    }

    public Location getLastLocationUnsafe(int userId, int permissionLevel, boolean isBypass, long maximumAgeMs) {
        Location location;
        if (userId == -1) {
            Location lastLocation = null;
            int[] runningUserIds = this.mUserHelper.getRunningUserIds();
            for (int i : runningUserIds) {
                Location next = getLastLocationUnsafe(i, permissionLevel, isBypass, maximumAgeMs);
                if (lastLocation == null || (next != null && next.getElapsedRealtimeNanos() > lastLocation.getElapsedRealtimeNanos())) {
                    lastLocation = next;
                }
            }
            return lastLocation;
        } else if (userId == -2) {
            return getLastLocationUnsafe(this.mUserHelper.getCurrentUserId(), permissionLevel, isBypass, maximumAgeMs);
        } else {
            Preconditions.checkArgument(userId >= 0);
            synchronized (this.mLock) {
                Preconditions.checkState(this.mState != 2);
                LastLocation lastLocation2 = this.mLastLocations.get(userId);
                if (lastLocation2 == null) {
                    location = null;
                } else {
                    location = lastLocation2.get(permissionLevel, isBypass);
                }
            }
            if (location == null || location.getElapsedRealtimeAgeMillis() > maximumAgeMs) {
                return null;
            }
            return location;
        }
    }

    public void injectLastLocation(Location location, int userId) {
        synchronized (this.mLock) {
            Preconditions.checkState(this.mState != 2);
            if (getLastLocationUnsafe(userId, 2, false, JobStatus.NO_LATEST_RUNTIME) == null) {
                setLastLocation(location, userId);
            }
        }
    }

    private void setLastLocation(Location location, int userId) {
        if (userId == -1) {
            int[] runningUserIds = this.mUserHelper.getRunningUserIds();
            for (int i : runningUserIds) {
                setLastLocation(location, i);
            }
        } else if (userId == -2) {
            setLastLocation(location, this.mUserHelper.getCurrentUserId());
        } else {
            Preconditions.checkArgument(userId >= 0);
            synchronized (this.mLock) {
                LastLocation lastLocation = this.mLastLocations.get(userId);
                if (lastLocation == null) {
                    lastLocation = new LastLocation();
                    this.mLastLocations.put(userId, lastLocation);
                }
                if (isEnabled(userId)) {
                    lastLocation.set(location);
                }
                lastLocation.setBypass(location);
                ITranLocation.Instance().wz1(location);
            }
        }
    }

    public ICancellationSignal getCurrentLocation(LocationRequest request, CallerIdentity identity, int permissionLevel, final ILocationCallback callback) {
        if (request.getDurationMillis() > 30000) {
            request = new LocationRequest.Builder(request).setDurationMillis(30000L).build();
        }
        final GetCurrentLocationListenerRegistration registration = new GetCurrentLocationListenerRegistration(request, identity, new GetCurrentLocationTransport(callback), permissionLevel);
        synchronized (this.mLock) {
            Preconditions.checkState(this.mState != 2);
            long ident = Binder.clearCallingIdentity();
            putRegistration(callback.asBinder(), registration);
            if (!registration.isActive()) {
                registration.deliverNull();
            }
            Binder.restoreCallingIdentity(ident);
        }
        ICancellationSignal cancelTransport = CancellationSignal.createTransport();
        CancellationSignal.fromTransport(cancelTransport).setOnCancelListener(new CancellationSignal.OnCancelListener() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda23
            @Override // android.os.CancellationSignal.OnCancelListener
            public final void onCancel() {
                LocationProviderManager.this.m4525xe7d4d724(callback, registration);
            }
        });
        return cancelTransport;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getCurrentLocation$2$com-android-server-location-provider-LocationProviderManager  reason: not valid java name */
    public /* synthetic */ void m4525xe7d4d724(ILocationCallback callback, GetCurrentLocationListenerRegistration registration) {
        long ident = Binder.clearCallingIdentity();
        try {
            try {
                synchronized (this.mLock) {
                    removeRegistration(callback.asBinder(), registration);
                }
            } catch (RuntimeException e) {
                FgThread.getExecutor().execute(new Runnable() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda21
                    @Override // java.lang.Runnable
                    public final void run() {
                        LocationProviderManager.lambda$getCurrentLocation$1(e);
                    }
                });
                throw e;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getCurrentLocation$1(RuntimeException e) {
        throw new AssertionError(e);
    }

    public void sendExtraCommand(int uid, int pid, String command, Bundle extras) {
        long identity = Binder.clearCallingIdentity();
        try {
            this.mProvider.getController().sendExtraCommand(uid, pid, command, extras);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void registerLocationRequest(LocationRequest request, CallerIdentity identity, int permissionLevel, ILocationListener listener) {
        LocationListenerRegistration registration = new LocationListenerRegistration(request, identity, new LocationListenerTransport(listener), permissionLevel);
        synchronized (this.mLock) {
            Preconditions.checkState(this.mState != 2);
            long ident = Binder.clearCallingIdentity();
            putRegistration(listener.asBinder(), registration);
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void registerLocationRequest(LocationRequest request, CallerIdentity callerIdentity, int permissionLevel, PendingIntent pendingIntent) {
        LocationPendingIntentRegistration registration = new LocationPendingIntentRegistration(request, callerIdentity, new LocationPendingIntentTransport(this.mContext, pendingIntent), permissionLevel);
        synchronized (this.mLock) {
            Preconditions.checkState(this.mState != 2);
            long identity = Binder.clearCallingIdentity();
            putRegistration(pendingIntent, registration);
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void flush(ILocationListener listener, final int requestCode) {
        synchronized (this.mLock) {
            long identity = Binder.clearCallingIdentity();
            boolean flushed = updateRegistration(listener.asBinder(), new Predicate() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda6
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return LocationProviderManager.lambda$flush$3(requestCode, (LocationProviderManager.Registration) obj);
                }
            });
            if (!flushed) {
                throw new IllegalArgumentException("unregistered listener cannot be flushed");
            }
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$flush$3(int requestCode, Registration registration) {
        registration.flush(requestCode);
        return false;
    }

    public void flush(PendingIntent pendingIntent, final int requestCode) {
        synchronized (this.mLock) {
            long identity = Binder.clearCallingIdentity();
            boolean flushed = updateRegistration(pendingIntent, new Predicate() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda4
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return LocationProviderManager.lambda$flush$4(requestCode, (LocationProviderManager.Registration) obj);
                }
            });
            if (!flushed) {
                throw new IllegalArgumentException("unregistered pending intent cannot be flushed");
            }
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$flush$4(int requestCode, Registration registration) {
        registration.flush(requestCode);
        return false;
    }

    public void unregisterLocationRequest(ILocationListener listener) {
        synchronized (this.mLock) {
            Preconditions.checkState(this.mState != 2);
            long identity = Binder.clearCallingIdentity();
            removeRegistration(listener.asBinder());
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void unregisterLocationRequest(PendingIntent pendingIntent) {
        synchronized (this.mLock) {
            Preconditions.checkState(this.mState != 2);
            long identity = Binder.clearCallingIdentity();
            removeRegistration(pendingIntent);
            Binder.restoreCallingIdentity(identity);
        }
    }

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    protected void onRegister() {
        if (Build.IS_DEBUGGABLE) {
            Preconditions.checkState(Thread.holdsLock(this.mLock));
        }
        this.mSettingsHelper.addOnBackgroundThrottleIntervalChangedListener(this.mBackgroundThrottleIntervalChangedListener);
        this.mSettingsHelper.addOnBackgroundThrottlePackageWhitelistChangedListener(this.mBackgroundThrottlePackageWhitelistChangedListener);
        this.mSettingsHelper.addOnLocationPackageBlacklistChangedListener(this.mLocationPackageBlacklistChangedListener);
        this.mSettingsHelper.addAdasAllowlistChangedListener(this.mAdasPackageAllowlistChangedListener);
        this.mSettingsHelper.addIgnoreSettingsAllowlistChangedListener(this.mIgnoreSettingsPackageWhitelistChangedListener);
        this.mLocationPermissionsHelper.addListener(this.mLocationPermissionsListener);
        this.mAppForegroundHelper.addListener(this.mAppForegroundChangedListener);
        this.mLocationPowerSaveModeHelper.addListener(this.mLocationPowerSaveModeChangedListener);
        this.mScreenInteractiveHelper.addListener(this.mScreenInteractiveChangedListener);
    }

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    protected void onUnregister() {
        if (Build.IS_DEBUGGABLE) {
            Preconditions.checkState(Thread.holdsLock(this.mLock));
        }
        this.mSettingsHelper.removeOnBackgroundThrottleIntervalChangedListener(this.mBackgroundThrottleIntervalChangedListener);
        this.mSettingsHelper.removeOnBackgroundThrottlePackageWhitelistChangedListener(this.mBackgroundThrottlePackageWhitelistChangedListener);
        this.mSettingsHelper.removeOnLocationPackageBlacklistChangedListener(this.mLocationPackageBlacklistChangedListener);
        this.mSettingsHelper.removeAdasAllowlistChangedListener(this.mAdasPackageAllowlistChangedListener);
        this.mSettingsHelper.removeIgnoreSettingsAllowlistChangedListener(this.mIgnoreSettingsPackageWhitelistChangedListener);
        this.mLocationPermissionsHelper.removeListener(this.mLocationPermissionsListener);
        this.mAppForegroundHelper.removeListener(this.mAppForegroundChangedListener);
        this.mLocationPowerSaveModeHelper.removeListener(this.mLocationPowerSaveModeChangedListener);
        this.mScreenInteractiveHelper.removeListener(this.mScreenInteractiveChangedListener);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public void onRegistrationAdded(Object key, Registration registration) {
        if (Build.IS_DEBUGGABLE) {
            Preconditions.checkState(Thread.holdsLock(this.mLock));
        }
        this.mLocationUsageLogger.logLocationApiUsage(0, 1, registration.getIdentity().getPackageName(), registration.getIdentity().getAttributionTag(), this.mName, registration.getRequest(), key instanceof PendingIntent, key instanceof IBinder, null, registration.isForeground());
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public void onRegistrationReplaced(Object key, Registration oldRegistration, Registration newRegistration) {
        newRegistration.setLastDeliveredLocation(oldRegistration.getLastDeliveredLocation());
        super.onRegistrationReplaced((LocationProviderManager) key, oldRegistration, newRegistration);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public void onRegistrationRemoved(Object key, Registration registration) {
        if (Build.IS_DEBUGGABLE) {
            Preconditions.checkState(Thread.holdsLock(this.mLock));
        }
        this.mLocationUsageLogger.logLocationApiUsage(1, 1, registration.getIdentity().getPackageName(), registration.getIdentity().getAttributionTag(), this.mName, registration.getRequest(), key instanceof PendingIntent, key instanceof IBinder, null, registration.isForeground());
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public boolean registerWithService(ProviderRequest request, Collection<Registration> registrations) {
        return reregisterWithService(ProviderRequest.EMPTY_REQUEST, request, registrations);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public boolean reregisterWithService(ProviderRequest oldRequest, final ProviderRequest newRequest, Collection<Registration> registrations) {
        long delayMs;
        if (Build.IS_DEBUGGABLE) {
            Preconditions.checkState(Thread.holdsLock(this.mLock));
        }
        if (!oldRequest.isBypass() && newRequest.isBypass()) {
            delayMs = 0;
        } else {
            long delayMs2 = newRequest.getIntervalMillis();
            if (delayMs2 > oldRequest.getIntervalMillis()) {
                delayMs = 0;
            } else {
                long delayMs3 = newRequest.getIntervalMillis();
                delayMs = calculateRequestDelayMillis(delayMs3, registrations);
            }
        }
        Preconditions.checkState(delayMs >= 0 && delayMs <= newRequest.getIntervalMillis());
        if (delayMs < 30000) {
            setProviderRequest(newRequest);
        } else {
            if (LocationManagerService.D) {
                Log.d(LocationManagerService.TAG, this.mName + " provider delaying request update " + newRequest + " by " + TimeUtils.formatDuration(delayMs));
            }
            AlarmManager.OnAlarmListener onAlarmListener = this.mDelayedRegister;
            if (onAlarmListener != null) {
                this.mAlarmHelper.cancel(onAlarmListener);
                this.mDelayedRegister = null;
            }
            AlarmManager.OnAlarmListener onAlarmListener2 = new AlarmManager.OnAlarmListener() { // from class: com.android.server.location.provider.LocationProviderManager.2
                @Override // android.app.AlarmManager.OnAlarmListener
                public void onAlarm() {
                    synchronized (LocationProviderManager.this.mLock) {
                        if (LocationProviderManager.this.mDelayedRegister == this) {
                            LocationProviderManager.this.mDelayedRegister = null;
                            LocationProviderManager.this.setProviderRequest(newRequest);
                        }
                    }
                }
            };
            this.mDelayedRegister = onAlarmListener2;
            this.mAlarmHelper.setDelayedAlarm(delayMs, onAlarmListener2, null);
        }
        return true;
    }

    @Override // com.android.server.location.listeners.ListenerMultiplexer
    protected void unregisterWithService() {
        if (Build.IS_DEBUGGABLE) {
            Preconditions.checkState(Thread.holdsLock(this.mLock));
        }
        setProviderRequest(ProviderRequest.EMPTY_REQUEST);
    }

    void setProviderRequest(final ProviderRequest request) {
        AlarmManager.OnAlarmListener onAlarmListener = this.mDelayedRegister;
        if (onAlarmListener != null) {
            this.mAlarmHelper.cancel(onAlarmListener);
            this.mDelayedRegister = null;
        }
        LocationEventLog.EVENT_LOG.logProviderUpdateRequest(this.mName, request);
        this.mProvider.getController().setRequest(request);
        FgThread.getHandler().post(new Runnable() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                LocationProviderManager.this.m4529xc72120fd(request);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setProviderRequest$5$com-android-server-location-provider-LocationProviderManager  reason: not valid java name */
    public /* synthetic */ void m4529xc72120fd(ProviderRequest request) {
        Iterator<IProviderRequestListener> it = this.mProviderRequestListeners.iterator();
        while (it.hasNext()) {
            IProviderRequestListener listener = it.next();
            try {
                listener.onProviderRequestChanged(this.mName, request);
            } catch (RemoteException e) {
                this.mProviderRequestListeners.remove(listener);
            }
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public boolean isActive(Registration registration) {
        if (Build.IS_DEBUGGABLE) {
            Preconditions.checkState(Thread.holdsLock(this.mLock));
        }
        if (registration.isPermitted()) {
            boolean isBypass = registration.getRequest().isBypass();
            if (isActive(isBypass, registration.getIdentity())) {
                if (!isBypass) {
                    switch (this.mLocationPowerSaveModeHelper.getLocationPowerSaveMode()) {
                        case 1:
                            if (!"gps".equals(this.mName)) {
                                return true;
                            }
                            break;
                        case 2:
                        case 4:
                            break;
                        case 3:
                            return registration.isForeground();
                        default:
                            return true;
                    }
                    return this.mScreenInteractiveHelper.isInteractive();
                }
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean isActive(boolean isBypass, CallerIdentity identity) {
        return identity.isSystemServer() ? isBypass || isEnabled(this.mUserHelper.getCurrentUserId()) : (isBypass || (isEnabled(identity.getUserId()) && this.mUserHelper.isCurrentUserId(identity.getUserId()))) && !this.mSettingsHelper.isLocationPackageBlacklisted(identity.getUserId(), identity.getPackageName());
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public ProviderRequest mergeRegistrations(Collection<Registration> registrations) {
        long thresholdIntervalMs;
        if (Build.IS_DEBUGGABLE) {
            Preconditions.checkState(Thread.holdsLock(this.mLock));
        }
        boolean lowPower = true;
        boolean lowPower2 = false;
        boolean locationSettingsIgnored = false;
        long maxUpdateDelayMs = Long.MAX_VALUE;
        long intervalMs = Long.MAX_VALUE;
        int quality = 104;
        for (Registration registration : registrations) {
            LocationRequest request = registration.getRequest();
            if (request.getIntervalMillis() != JobStatus.NO_LATEST_RUNTIME) {
                intervalMs = Math.min(request.getIntervalMillis(), intervalMs);
                quality = Math.min(request.getQuality(), quality);
                maxUpdateDelayMs = Math.min(request.getMaxUpdateDelayMillis(), maxUpdateDelayMs);
                locationSettingsIgnored |= request.isAdasGnssBypass();
                lowPower2 |= request.isLocationSettingsIgnored();
                lowPower &= request.isLowPower();
            }
        }
        if (intervalMs == JobStatus.NO_LATEST_RUNTIME) {
            return ProviderRequest.EMPTY_REQUEST;
        }
        if (maxUpdateDelayMs / 2 < intervalMs) {
            maxUpdateDelayMs = 0;
        }
        try {
            thresholdIntervalMs = Math.multiplyExact(Math.addExact(intervalMs, 1000L) / 2, 3);
        } catch (ArithmeticException e) {
            thresholdIntervalMs = 9223372036854775806L;
        }
        WorkSource workSource = new WorkSource();
        for (Registration registration2 : registrations) {
            if (registration2.getRequest().getIntervalMillis() <= thresholdIntervalMs) {
                workSource.add(registration2.getRequest().getWorkSource());
            }
        }
        return new ProviderRequest.Builder().setIntervalMillis(intervalMs).setQuality(quality).setMaxUpdateDelayMillis(maxUpdateDelayMs).setAdasGnssBypass(locationSettingsIgnored).setLocationSettingsIgnored(lowPower2).setLowPower(lowPower).setWorkSource(workSource).build();
    }

    protected long calculateRequestDelayMillis(long newIntervalMs, Collection<Registration> registrations) {
        long registrationDelayMs;
        long delayMs = newIntervalMs;
        for (Registration registration : registrations) {
            if (delayMs == 0) {
                break;
            }
            LocationRequest locationRequest = registration.getRequest();
            Location last = registration.getLastDeliveredLocation();
            if (last == null && !locationRequest.isLocationSettingsIgnored()) {
                last = getLastLocationUnsafe(registration.getIdentity().getUserId(), registration.getPermissionLevel(), false, locationRequest.getIntervalMillis());
            }
            if (last == null) {
                registrationDelayMs = 0;
            } else {
                registrationDelayMs = Math.max(0L, locationRequest.getIntervalMillis() - last.getElapsedRealtimeAgeMillis());
            }
            delayMs = Math.min(delayMs, registrationDelayMs);
        }
        return delayMs;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onUserChanged(final int userId, int change) {
        synchronized (this.mLock) {
            if (this.mState == 2) {
                return;
            }
            switch (change) {
                case 1:
                    updateRegistrations(new Predicate() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda2
                        @Override // java.util.function.Predicate
                        public final boolean test(Object obj) {
                            return LocationProviderManager.lambda$onUserChanged$6(userId, (LocationProviderManager.Registration) obj);
                        }
                    });
                    break;
                case 2:
                    onUserStarted(userId);
                    break;
                case 3:
                    onUserStopped(userId);
                    break;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$onUserChanged$6(int userId, Registration registration) {
        return registration.getIdentity().getUserId() == userId;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLocationUserSettingsChanged(final int userId, LocationUserSettings oldSettings, LocationUserSettings newSettings) {
        if (oldSettings.isAdasGnssLocationEnabled() != newSettings.isAdasGnssLocationEnabled()) {
            synchronized (this.mLock) {
                updateRegistrations(new Predicate() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda22
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        boolean onAdasGnssLocationEnabledChanged;
                        onAdasGnssLocationEnabledChanged = ((LocationProviderManager.Registration) obj).onAdasGnssLocationEnabledChanged(userId);
                        return onAdasGnssLocationEnabledChanged;
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLocationEnabledChanged(int userId) {
        synchronized (this.mLock) {
            if (this.mState == 2) {
                return;
            }
            onEnabledChanged(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onScreenInteractiveChanged(boolean screenInteractive) {
        synchronized (this.mLock) {
            switch (this.mLocationPowerSaveModeHelper.getLocationPowerSaveMode()) {
                case 1:
                    if (!"gps".equals(this.mName)) {
                        break;
                    }
                case 2:
                case 4:
                    updateRegistrations(new Predicate() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda3
                        @Override // java.util.function.Predicate
                        public final boolean test(Object obj) {
                            return LocationProviderManager.lambda$onScreenInteractiveChanged$8((LocationProviderManager.Registration) obj);
                        }
                    });
                    break;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$onScreenInteractiveChanged$8(Registration registration) {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onBackgroundThrottlePackageWhitelistChanged() {
        synchronized (this.mLock) {
            updateRegistrations(new LocationProviderManager$$ExternalSyntheticLambda1());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onBackgroundThrottleIntervalChanged() {
        synchronized (this.mLock) {
            updateRegistrations(new LocationProviderManager$$ExternalSyntheticLambda1());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLocationPowerSaveModeChanged(int locationPowerSaveMode) {
        synchronized (this.mLock) {
            updateRegistrations(new Predicate() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda18
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return LocationProviderManager.lambda$onLocationPowerSaveModeChanged$9((LocationProviderManager.Registration) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$onLocationPowerSaveModeChanged$9(Registration registration) {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onAppForegroundChanged(final int uid, final boolean foreground) {
        synchronized (this.mLock) {
            updateRegistrations(new Predicate() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda28
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean onForegroundChanged;
                    onForegroundChanged = ((LocationProviderManager.Registration) obj).onForegroundChanged(uid, foreground);
                    return onForegroundChanged;
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onAdasAllowlistChanged() {
        synchronized (this.mLock) {
            updateRegistrations(new LocationProviderManager$$ExternalSyntheticLambda1());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onIgnoreSettingsWhitelistChanged() {
        synchronized (this.mLock) {
            updateRegistrations(new LocationProviderManager$$ExternalSyntheticLambda1());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLocationPackageBlacklistChanged(final int userId) {
        synchronized (this.mLock) {
            updateRegistrations(new Predicate() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda24
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return LocationProviderManager.lambda$onLocationPackageBlacklistChanged$11(userId, (LocationProviderManager.Registration) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$onLocationPackageBlacklistChanged$11(int userId, Registration registration) {
        return registration.getIdentity().getUserId() == userId;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLocationPermissionsChanged(final String packageName) {
        synchronized (this.mLock) {
            updateRegistrations(new Predicate() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda5
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean onLocationPermissionsChanged;
                    onLocationPermissionsChanged = ((LocationProviderManager.Registration) obj).onLocationPermissionsChanged(packageName);
                    return onLocationPermissionsChanged;
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLocationPermissionsChanged(final int uid) {
        synchronized (this.mLock) {
            updateRegistrations(new Predicate() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda31
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean onLocationPermissionsChanged;
                    onLocationPermissionsChanged = ((LocationProviderManager.Registration) obj).onLocationPermissionsChanged(uid);
                    return onLocationPermissionsChanged;
                }
            });
        }
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider.Listener
    public void onStateChanged(final AbstractLocationProvider.State oldState, final AbstractLocationProvider.State newState) {
        if (Build.IS_DEBUGGABLE) {
            Preconditions.checkState(Thread.holdsLock(this.mLock));
        }
        if (oldState.allowed != newState.allowed) {
            onEnabledChanged(-1);
        }
        if (!Objects.equals(oldState.properties, newState.properties)) {
            updateRegistrations(new Predicate() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda25
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return ((LocationProviderManager.Registration) obj).onProviderPropertiesChanged();
                }
            });
        }
        if (this.mStateChangedListener != null) {
            final StateChangedListener listener = this.mStateChangedListener;
            FgThread.getExecutor().execute(new Runnable() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda26
                @Override // java.lang.Runnable
                public final void run() {
                    LocationProviderManager.this.m4528xb2100c9f(listener, oldState, newState);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onStateChanged$14$com-android-server-location-provider-LocationProviderManager  reason: not valid java name */
    public /* synthetic */ void m4528xb2100c9f(StateChangedListener listener, AbstractLocationProvider.State oldState, AbstractLocationProvider.State newState) {
        listener.onStateChanged(this.mName, oldState, newState);
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider.Listener
    public void onReportLocation(LocationResult locationResult) {
        final LocationResult filtered;
        if (Build.IS_DEBUGGABLE) {
            Preconditions.checkState(Thread.holdsLock(this.mLock));
        }
        if (this.mPassiveManager != null) {
            filtered = locationResult.filter(new Predicate() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda29
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return LocationProviderManager.this.m4527x74a28164((Location) obj);
                }
            });
            if (filtered == null) {
                return;
            }
            LocationEventLog.EVENT_LOG.logProviderReceivedLocations(this.mName, filtered.size());
            if (LocationManagerService.D) {
                Log.d(LocationManagerService.TAG, "incoming location: " + filtered);
            } else if (filtered.size() > 0) {
                Log.d(LocationManagerService.TAG, "incoming location: " + LocationManagerService.mtkBuildLocationInfo(filtered.get(0)));
            }
        } else {
            filtered = locationResult;
        }
        Location last = getLastLocationUnsafe(-2, 2, true, JobStatus.NO_LATEST_RUNTIME);
        if (last != null && locationResult.get(0).getElapsedRealtimeNanos() < last.getElapsedRealtimeNanos()) {
            Log.e(LocationManagerService.TAG, "non-monotonic location received from " + this.mName + " provider");
        }
        setLastLocation(filtered.getLastLocation(), -1);
        deliverToListeners(new Function() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda30
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                ListenerExecutor.ListenerOperation acceptLocationChange;
                acceptLocationChange = ((LocationProviderManager.Registration) obj).acceptLocationChange(filtered);
                return acceptLocationChange;
            }
        });
        PassiveLocationProviderManager passiveLocationProviderManager = this.mPassiveManager;
        if (passiveLocationProviderManager != null) {
            passiveLocationProviderManager.updateLocation(filtered);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onReportLocation$15$com-android-server-location-provider-LocationProviderManager  reason: not valid java name */
    public /* synthetic */ boolean m4527x74a28164(Location location) {
        if (!location.isMock() && location.getLatitude() == 0.0d && location.getLongitude() == 0.0d) {
            Log.e(LocationManagerService.TAG, "blocking 0,0 location from " + this.mName + " provider");
            return false;
        } else if (!location.isComplete()) {
            Log.e(LocationManagerService.TAG, "blocking incomplete location from " + this.mName + " provider");
            return false;
        } else {
            return true;
        }
    }

    private void onUserStarted(int userId) {
        if (Build.IS_DEBUGGABLE) {
            Preconditions.checkState(Thread.holdsLock(this.mLock));
        }
        if (userId == -10000) {
            return;
        }
        if (userId == -1) {
            this.mEnabled.clear();
            onEnabledChanged(-1);
            return;
        }
        Preconditions.checkArgument(userId >= 0);
        this.mEnabled.delete(userId);
        onEnabledChanged(userId);
    }

    private void onUserStopped(int userId) {
        if (Build.IS_DEBUGGABLE) {
            Preconditions.checkState(Thread.holdsLock(this.mLock));
        }
        if (userId == -10000) {
            return;
        }
        if (userId == -1) {
            this.mEnabled.clear();
            this.mLastLocations.clear();
            return;
        }
        Preconditions.checkArgument(userId >= 0);
        this.mEnabled.delete(userId);
        this.mLastLocations.remove(userId);
    }

    private void onEnabledChanged(final int userId) {
        LastLocation lastLocation;
        if (Build.IS_DEBUGGABLE) {
            Preconditions.checkState(Thread.holdsLock(this.mLock));
        }
        if (userId == -10000) {
            return;
        }
        if (userId == -1) {
            int[] runningUserIds = this.mUserHelper.getRunningUserIds();
            for (int i : runningUserIds) {
                onEnabledChanged(i);
            }
            return;
        }
        final boolean enabled = true;
        Preconditions.checkArgument(userId >= 0);
        enabled = (this.mState == 0 && this.mProvider.getState().allowed && this.mSettingsHelper.isLocationEnabled(userId)) ? false : false;
        int index = this.mEnabled.indexOfKey(userId);
        Boolean wasEnabled = index < 0 ? null : Boolean.valueOf(this.mEnabled.valueAt(index));
        if (wasEnabled != null && wasEnabled.booleanValue() == enabled) {
            return;
        }
        this.mEnabled.put(userId, enabled);
        if (wasEnabled != null || enabled) {
            if (LocationManagerService.D) {
                Log.d(LocationManagerService.TAG, "[u" + userId + "] " + this.mName + " provider enabled = " + enabled);
            }
            LocationEventLog.EVENT_LOG.logProviderEnabled(this.mName, userId, enabled);
        }
        if (!enabled && (lastLocation = this.mLastLocations.get(userId)) != null) {
            lastLocation.clearLocations();
        }
        if (wasEnabled != null) {
            if (!"passive".equals(this.mName)) {
                Intent intent = new Intent("android.location.PROVIDERS_CHANGED").putExtra("android.location.extra.PROVIDER_NAME", this.mName).putExtra("android.location.extra.PROVIDER_ENABLED", enabled).addFlags(1073741824).addFlags(268435456);
                this.mContext.sendBroadcastAsUser(intent, UserHandle.of(userId));
            }
            if (!this.mEnabledListeners.isEmpty()) {
                final LocationManagerInternal.ProviderEnabledListener[] listeners = (LocationManagerInternal.ProviderEnabledListener[]) this.mEnabledListeners.toArray(new LocationManagerInternal.ProviderEnabledListener[0]);
                FgThread.getHandler().post(new Runnable() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda19
                    @Override // java.lang.Runnable
                    public final void run() {
                        LocationProviderManager.this.m4526x4e379fcc(listeners, userId, enabled);
                    }
                });
            }
        }
        updateRegistrations(new Predicate() { // from class: com.android.server.location.provider.LocationProviderManager$$ExternalSyntheticLambda20
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return LocationProviderManager.lambda$onEnabledChanged$18(userId, (LocationProviderManager.Registration) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onEnabledChanged$17$com-android-server-location-provider-LocationProviderManager  reason: not valid java name */
    public /* synthetic */ void m4526x4e379fcc(LocationManagerInternal.ProviderEnabledListener[] listeners, int userId, boolean enabled) {
        for (LocationManagerInternal.ProviderEnabledListener providerEnabledListener : listeners) {
            providerEnabledListener.onProviderEnabledChanged(this.mName, userId, enabled);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$onEnabledChanged$18(int userId, Registration registration) {
        return registration.getIdentity().getUserId() == userId;
    }

    Location getPermittedLocation(Location fineLocation, int permissionLevel) {
        switch (permissionLevel) {
            case 1:
                if (fineLocation != null) {
                    return this.mLocationFudger.createCoarse(fineLocation);
                }
                return null;
            case 2:
                return fineLocation;
            default:
                throw new AssertionError();
        }
    }

    LocationResult getPermittedLocationResult(LocationResult fineLocationResult, int permissionLevel) {
        switch (permissionLevel) {
            case 1:
                if (fineLocationResult != null) {
                    return this.mLocationFudger.createCoarse(fineLocationResult);
                }
                return null;
            case 2:
                return fineLocationResult;
            default:
                throw new AssertionError();
        }
    }

    public void dump(FileDescriptor fd, IndentingPrintWriter ipw, String[] args) {
        synchronized (this.mLock) {
            ipw.print(this.mName);
            ipw.print(" provider");
            if (this.mProvider.isMock()) {
                ipw.print(" [mock]");
            }
            ipw.println(":");
            ipw.increaseIndent();
            super.dump(fd, (PrintWriter) ipw, args);
            int[] userIds = this.mUserHelper.getRunningUserIds();
            for (int userId : userIds) {
                if (userIds.length != 1) {
                    ipw.print("user ");
                    ipw.print(userId);
                    ipw.println(":");
                    ipw.increaseIndent();
                }
                ipw.print("last location=");
                ipw.println(getLastLocationUnsafe(userId, 2, false, JobStatus.NO_LATEST_RUNTIME));
                ipw.print("enabled=");
                ipw.println(isEnabled(userId));
                if (userIds.length != 1) {
                    ipw.decreaseIndent();
                }
            }
        }
        this.mProvider.dump(fd, ipw, args);
        ipw.decreaseIndent();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.listeners.ListenerMultiplexer
    public String getServiceState() {
        return this.mProvider.getCurrentRequest().toString();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class LastLocation {
        private Location mCoarseBypassLocation;
        private Location mCoarseLocation;
        private Location mFineBypassLocation;
        private Location mFineLocation;

        LastLocation() {
        }

        public void clearMock() {
            Location location = this.mFineLocation;
            if (location != null && location.isMock()) {
                this.mFineLocation = null;
            }
            Location location2 = this.mCoarseLocation;
            if (location2 != null && location2.isMock()) {
                this.mCoarseLocation = null;
            }
            Location location3 = this.mFineBypassLocation;
            if (location3 != null && location3.isMock()) {
                this.mFineBypassLocation = null;
            }
            Location location4 = this.mCoarseBypassLocation;
            if (location4 != null && location4.isMock()) {
                this.mCoarseBypassLocation = null;
            }
        }

        public void clearLocations() {
            this.mFineLocation = null;
            this.mCoarseLocation = null;
        }

        public Location get(int permissionLevel, boolean isBypass) {
            switch (permissionLevel) {
                case 1:
                    if (isBypass) {
                        return this.mCoarseBypassLocation;
                    }
                    return this.mCoarseLocation;
                case 2:
                    if (isBypass) {
                        return this.mFineBypassLocation;
                    }
                    return this.mFineLocation;
                default:
                    throw new AssertionError();
            }
        }

        public void set(Location location) {
            this.mFineLocation = calculateNextFine(this.mFineLocation, location);
            this.mCoarseLocation = calculateNextCoarse(this.mCoarseLocation, location);
        }

        public void setBypass(Location location) {
            this.mFineBypassLocation = calculateNextFine(this.mFineBypassLocation, location);
            this.mCoarseBypassLocation = calculateNextCoarse(this.mCoarseBypassLocation, location);
        }

        private Location calculateNextFine(Location oldFine, Location newFine) {
            if (oldFine == null) {
                return newFine;
            }
            if (newFine.getElapsedRealtimeNanos() > oldFine.getElapsedRealtimeNanos()) {
                return newFine;
            }
            return oldFine;
        }

        private Location calculateNextCoarse(Location oldCoarse, Location newCoarse) {
            if (oldCoarse == null) {
                return newCoarse;
            }
            if (newCoarse.getElapsedRealtimeMillis() - 600000 > oldCoarse.getElapsedRealtimeMillis()) {
                return newCoarse;
            }
            return oldCoarse;
        }
    }

    /* loaded from: classes.dex */
    private static class PendingIntentSender {
        private PendingIntentSender() {
        }

        public static void send(PendingIntent pendingIntent, Context context, Intent intent, Runnable callback, Bundle options) throws PendingIntent.CanceledException {
            final GatedCallback gatedCallback;
            PendingIntent.OnFinished onFinished;
            if (callback != null) {
                gatedCallback = new GatedCallback(callback);
                onFinished = new PendingIntent.OnFinished() { // from class: com.android.server.location.provider.LocationProviderManager$PendingIntentSender$$ExternalSyntheticLambda0
                    @Override // android.app.PendingIntent.OnFinished
                    public final void onSendFinished(PendingIntent pendingIntent2, Intent intent2, int i, String str, Bundle bundle) {
                        LocationProviderManager.PendingIntentSender.GatedCallback.this.run();
                    }
                };
            } else {
                gatedCallback = null;
                onFinished = null;
            }
            pendingIntent.send(context, 0, intent, onFinished, null, null, options);
            if (gatedCallback != null) {
                gatedCallback.allow();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class GatedCallback implements Runnable {
            private Runnable mCallback;
            private boolean mGate;
            private boolean mRun;

            private GatedCallback(Runnable callback) {
                this.mCallback = callback;
            }

            public void allow() {
                Runnable runnable;
                Runnable callback = null;
                synchronized (this) {
                    this.mGate = true;
                    if (this.mRun && (runnable = this.mCallback) != null) {
                        callback = runnable;
                        this.mCallback = null;
                    }
                }
                if (callback != null) {
                    callback.run();
                }
            }

            @Override // java.lang.Runnable
            public void run() {
                Runnable runnable;
                Runnable callback = null;
                synchronized (this) {
                    this.mRun = true;
                    if (this.mGate && (runnable = this.mCallback) != null) {
                        callback = runnable;
                        this.mCallback = null;
                    }
                }
                if (callback != null) {
                    callback.run();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ExternalWakeLockReleaser extends IRemoteCallback.Stub {
        private final CallerIdentity mIdentity;
        private final PowerManager.WakeLock mWakeLock;

        ExternalWakeLockReleaser(CallerIdentity identity, PowerManager.WakeLock wakeLock) {
            this.mIdentity = identity;
            this.mWakeLock = (PowerManager.WakeLock) Objects.requireNonNull(wakeLock);
        }

        public void sendResult(Bundle data) {
            long identity = Binder.clearCallingIdentity();
            try {
                try {
                    this.mWakeLock.release();
                } catch (RuntimeException e) {
                    if (e.getClass() == RuntimeException.class) {
                        Log.e(LocationManagerService.TAG, "wakelock over-released by " + this.mIdentity, e);
                    } else {
                        FgThread.getExecutor().execute(new Runnable() { // from class: com.android.server.location.provider.LocationProviderManager$ExternalWakeLockReleaser$$ExternalSyntheticLambda0
                            @Override // java.lang.Runnable
                            public final void run() {
                                LocationProviderManager.ExternalWakeLockReleaser.lambda$sendResult$0(e);
                            }
                        });
                        throw e;
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$sendResult$0(RuntimeException e) {
            throw new AssertionError(e);
        }
    }
}
