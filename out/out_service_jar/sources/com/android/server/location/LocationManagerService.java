package com.android.server.location;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.app.compat.CompatChanges;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.location.Criteria;
import android.location.GeocoderParams;
import android.location.Geofence;
import android.location.GnssAntennaInfo;
import android.location.GnssCapabilities;
import android.location.GnssMeasurementCorrections;
import android.location.GnssMeasurementRequest;
import android.location.IGeocodeListener;
import android.location.IGnssAntennaInfoListener;
import android.location.IGnssMeasurementsListener;
import android.location.IGnssNavigationMessageListener;
import android.location.IGnssNmeaListener;
import android.location.IGnssStatusListener;
import android.location.ILocationCallback;
import android.location.ILocationListener;
import android.location.ILocationManager;
import android.location.LastLocationRequest;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationManagerInternal;
import android.location.LocationProvider;
import android.location.LocationRequest;
import android.location.LocationTime;
import android.location.provider.IProviderRequestListener;
import android.location.provider.ProviderProperties;
import android.location.util.identity.CallerIdentity;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ICancellationSignal;
import android.os.PackageTagsList;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.TimeUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.Preconditions;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.location.eventlog.LocationEventLog;
import com.android.server.location.geofence.GeofenceManager;
import com.android.server.location.geofence.GeofenceProxy;
import com.android.server.location.gnss.GnssConfiguration;
import com.android.server.location.gnss.GnssManagerService;
import com.android.server.location.gnss.hal.GnssNative;
import com.android.server.location.injector.AlarmHelper;
import com.android.server.location.injector.AppForegroundHelper;
import com.android.server.location.injector.AppOpsHelper;
import com.android.server.location.injector.DeviceIdleHelper;
import com.android.server.location.injector.DeviceStationaryHelper;
import com.android.server.location.injector.EmergencyHelper;
import com.android.server.location.injector.Injector;
import com.android.server.location.injector.LocationPermissionsHelper;
import com.android.server.location.injector.LocationPowerSaveModeHelper;
import com.android.server.location.injector.LocationUsageLogger;
import com.android.server.location.injector.ScreenInteractiveHelper;
import com.android.server.location.injector.SettingsHelper;
import com.android.server.location.injector.SystemAlarmHelper;
import com.android.server.location.injector.SystemAppForegroundHelper;
import com.android.server.location.injector.SystemAppOpsHelper;
import com.android.server.location.injector.SystemDeviceIdleHelper;
import com.android.server.location.injector.SystemDeviceStationaryHelper;
import com.android.server.location.injector.SystemEmergencyHelper;
import com.android.server.location.injector.SystemLocationPermissionsHelper;
import com.android.server.location.injector.SystemLocationPowerSaveModeHelper;
import com.android.server.location.injector.SystemScreenInteractiveHelper;
import com.android.server.location.injector.SystemSettingsHelper;
import com.android.server.location.injector.SystemUserInfoHelper;
import com.android.server.location.injector.UserInfoHelper;
import com.android.server.location.provider.AbstractLocationProvider;
import com.android.server.location.provider.LocationProviderManager;
import com.android.server.location.provider.MockLocationProvider;
import com.android.server.location.provider.PassiveLocationProvider;
import com.android.server.location.provider.PassiveLocationProviderManager;
import com.android.server.location.provider.StationaryThrottlingLocationProvider;
import com.android.server.location.provider.proxy.ProxyLocationProvider;
import com.android.server.location.settings.LocationSettings;
import com.android.server.location.settings.LocationUserSettings;
import com.android.server.pm.permission.LegacyPermissionManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class LocationManagerService extends ILocationManager.Stub implements LocationProviderManager.StateChangedListener {
    private static final String ATTRIBUTION_TAG = "LocationService";
    public static final boolean D;
    private static final boolean IS_USER_BUILD;
    public static final String TAG = "LocationManagerService";
    private static final String TEL_DBG_PROP = "persist.vendor.log.tel_dbg";
    public static final boolean USERDEBUG_TEL_PROP;
    private static SystemAppOpsHelper sAppOpsHelper;
    private static boolean sCtaSupported;
    private static Object sMtkLocationManagerService;
    private static Class<?> sMtkLocationManagerServiceClass;
    private final Context mContext;
    private ILocationListener mDeprecatedGnssBatchingListener;
    private String mExtraLocationControllerPackage;
    private boolean mExtraLocationControllerPackageEnabled;
    private GeocoderProxy mGeocodeProvider;
    private final GeofenceManager mGeofenceManager;
    private final Injector mInjector;
    private final LocalService mLocalService;
    LocationManagerInternal.LocationPackageTagsListener mLocationTagsChangedListener;
    private final PassiveLocationProviderManager mPassiveManager;
    final Object mLock = new Object();
    private volatile GnssManagerService mGnssManagerService = null;
    private final Object mDeprecatedGnssBatchingLock = new Object();
    final CopyOnWriteArrayList<LocationProviderManager> mProviderManagers = new CopyOnWriteArrayList<>();

    /* loaded from: classes.dex */
    public static class Lifecycle extends SystemService {
        private static final Uri CLOUD_URI = Uri.parse("content://com.hoffnung.cloudControl.RemoteConfigProvider/config/os_herenlp");
        private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
        private static final String HOFF_NUNG_URI = "content://com.hoffnung.cloudControl.RemoteConfigProvider/config/";
        private static final String KEY_ROOT = "os_herenlp";
        private final Context mContext;
        private final LocationManagerService mService;
        private final SystemInjector mSystemInjector;
        private final LifecycleUserInfoHelper mUserInfoHelper;

        public Lifecycle(Context context) {
            super(context);
            LifecycleUserInfoHelper lifecycleUserInfoHelper = new LifecycleUserInfoHelper(context);
            this.mUserInfoHelper = lifecycleUserInfoHelper;
            SystemInjector systemInjector = new SystemInjector(context, lifecycleUserInfoHelper);
            this.mSystemInjector = systemInjector;
            this.mService = new LocationManagerService(context, systemInjector);
            this.mContext = context;
        }

        @Override // com.android.server.SystemService
        public void onStart() {
            publishBinderService("location", this.mService);
            LocationManager.invalidateLocalLocationEnabledCaches();
            LocationManager.disableLocalLocationEnabledCaches();
        }

        @Override // com.android.server.SystemService
        public void onBootPhase(int phase) {
            if (phase == 500) {
                this.mSystemInjector.onSystemReady();
                this.mService.onSystemReady();
            } else if (phase == 600) {
                this.mService.onSystemThirdPartyAppsCanStart();
            } else if (phase == 1000 && SystemProperties.getInt("ro.tran_here_nlp_support", 0) == 1) {
                Log.d(LocationManagerService.TAG, "onBootPhase, boot complete");
                loadCloudConfig();
                this.mContext.getContentResolver().registerContentObserver(CLOUD_URI, false, new ContentObserver(new Handler()) { // from class: com.android.server.location.LocationManagerService.Lifecycle.1
                    @Override // android.database.ContentObserver
                    public void onChange(boolean selfChange) {
                        Log.d(LocationManagerService.TAG, "here cloud config changed");
                        Lifecycle.this.loadCloudConfig();
                    }
                });
            }
        }

        public void loadCloudConfig() {
            EXECUTOR.execute(new Runnable() { // from class: com.android.server.location.LocationManagerService.Lifecycle.2
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        Cursor cursor = Lifecycle.this.mContext.getContentResolver().query(Lifecycle.CLOUD_URI, null, null, null, null);
                        if (cursor == null || !cursor.moveToNext()) {
                            Log.w(LocationManagerService.TAG, "cloud config is null");
                        } else {
                            int index = cursor.getColumnIndex(Lifecycle.KEY_ROOT);
                            String ret = cursor.getString(index);
                            JSONObject jsonObject = new JSONObject(ret);
                            boolean here_enable = jsonObject.getBoolean("enable");
                            Log.w(LocationManagerService.TAG, "cloud config loaded, here enable = " + here_enable);
                            if (!here_enable) {
                                Settings.Secure.putInt(Lifecycle.this.mContext.getContentResolver(), "com.here.consent.COLLECT_RADIO_SIGNALS", 0);
                            }
                            Settings.Secure.putInt(Lifecycle.this.mContext.getContentResolver(), "cloud_here_enable", here_enable ? 1 : 0);
                        }
                        if (cursor != null) {
                            cursor.close();
                        }
                    } catch (Exception e) {
                        Log.e(LocationManagerService.TAG, "load cloud config error", e);
                    }
                }
            });
        }

        @Override // com.android.server.SystemService
        public void onUserStarting(SystemService.TargetUser user) {
            this.mUserInfoHelper.onUserStarted(user.getUserIdentifier());
        }

        @Override // com.android.server.SystemService
        public void onUserSwitching(SystemService.TargetUser from, SystemService.TargetUser to) {
            this.mUserInfoHelper.onCurrentUserChanged(from.getUserIdentifier(), to.getUserIdentifier());
        }

        @Override // com.android.server.SystemService
        public void onUserStopped(SystemService.TargetUser user) {
            this.mUserInfoHelper.onUserStopped(user.getUserIdentifier());
        }

        /* loaded from: classes.dex */
        private static class LifecycleUserInfoHelper extends SystemUserInfoHelper {
            LifecycleUserInfoHelper(Context context) {
                super(context);
            }

            void onUserStarted(int userId) {
                dispatchOnUserStarted(userId);
            }

            void onUserStopped(int userId) {
                dispatchOnUserStopped(userId);
            }

            void onCurrentUserChanged(int fromUserId, int toUserId) {
                dispatchOnCurrentUserChanged(fromUserId, toUserId);
            }
        }
    }

    static {
        boolean z = true;
        boolean z2 = "user".equals(Build.TYPE) || "userdebug".equals(Build.TYPE);
        IS_USER_BUILD = z2;
        boolean z3 = SystemProperties.getInt(TEL_DBG_PROP, 0) == 1 && "userdebug".equals(Build.TYPE);
        USERDEBUG_TEL_PROP = z3;
        if (z2 && !Log.isLoggable(TAG, 3) && !z3) {
            z = false;
        }
        D = z;
        sMtkLocationManagerServiceClass = null;
        sMtkLocationManagerService = null;
        sCtaSupported = false;
        sAppOpsHelper = null;
    }

    LocationManagerService(Context context, Injector injector) {
        Context createAttributionContext = context.createAttributionContext(ATTRIBUTION_TAG);
        this.mContext = createAttributionContext;
        this.mInjector = injector;
        LocalService localService = new LocalService();
        this.mLocalService = localService;
        LocalServices.addService(LocationManagerInternal.class, localService);
        this.mGeofenceManager = new GeofenceManager(createAttributionContext, injector);
        injector.getLocationSettings().registerLocationUserSettingsListener(new LocationSettings.LocationUserSettingsListener() { // from class: com.android.server.location.LocationManagerService$$ExternalSyntheticLambda2
            @Override // com.android.server.location.settings.LocationSettings.LocationUserSettingsListener
            public final void onLocationUserSettingsChanged(int i, LocationUserSettings locationUserSettings, LocationUserSettings locationUserSettings2) {
                LocationManagerService.this.onLocationUserSettingsChanged(i, locationUserSettings, locationUserSettings2);
            }
        });
        injector.getSettingsHelper().addOnLocationEnabledChangedListener(new SettingsHelper.UserSettingChangedListener() { // from class: com.android.server.location.LocationManagerService$$ExternalSyntheticLambda3
            @Override // com.android.server.location.injector.SettingsHelper.UserSettingChangedListener
            public final void onSettingChanged(int i) {
                LocationManagerService.this.onLocationModeChanged(i);
            }
        });
        injector.getSettingsHelper().addAdasAllowlistChangedListener(new SettingsHelper.GlobalSettingChangedListener() { // from class: com.android.server.location.LocationManagerService$$ExternalSyntheticLambda4
            @Override // com.android.server.location.injector.SettingsHelper.GlobalSettingChangedListener
            public final void onSettingChanged() {
                LocationManagerService.this.m4275lambda$new$0$comandroidserverlocationLocationManagerService();
            }
        });
        injector.getSettingsHelper().addIgnoreSettingsAllowlistChangedListener(new SettingsHelper.GlobalSettingChangedListener() { // from class: com.android.server.location.LocationManagerService$$ExternalSyntheticLambda5
            @Override // com.android.server.location.injector.SettingsHelper.GlobalSettingChangedListener
            public final void onSettingChanged() {
                LocationManagerService.this.m4276lambda$new$1$comandroidserverlocationLocationManagerService();
            }
        });
        injector.getUserInfoHelper().addListener(new UserInfoHelper.UserListener() { // from class: com.android.server.location.LocationManagerService$$ExternalSyntheticLambda6
            @Override // com.android.server.location.injector.UserInfoHelper.UserListener
            public final void onUserChanged(int i, int i2) {
                LocationManagerService.this.m4277lambda$new$2$comandroidserverlocationLocationManagerService(i, i2);
            }
        });
        PassiveLocationProviderManager passiveLocationProviderManager = new PassiveLocationProviderManager(createAttributionContext, injector);
        this.mPassiveManager = passiveLocationProviderManager;
        addLocationProviderManager(passiveLocationProviderManager, new PassiveLocationProvider(createAttributionContext));
        LegacyPermissionManagerInternal permissionManagerInternal = (LegacyPermissionManagerInternal) LocalServices.getService(LegacyPermissionManagerInternal.class);
        permissionManagerInternal.setLocationPackagesProvider(new LegacyPermissionManagerInternal.PackagesProvider() { // from class: com.android.server.location.LocationManagerService$$ExternalSyntheticLambda7
            @Override // com.android.server.pm.permission.LegacyPermissionManagerInternal.PackagesProvider
            public final String[] getPackages(int i) {
                return LocationManagerService.this.m4278lambda$new$3$comandroidserverlocationLocationManagerService(i);
            }
        });
        permissionManagerInternal.setLocationExtraPackagesProvider(new LegacyPermissionManagerInternal.PackagesProvider() { // from class: com.android.server.location.LocationManagerService$$ExternalSyntheticLambda8
            @Override // com.android.server.pm.permission.LegacyPermissionManagerInternal.PackagesProvider
            public final String[] getPackages(int i) {
                return LocationManagerService.this.m4279lambda$new$4$comandroidserverlocationLocationManagerService(i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-location-LocationManagerService  reason: not valid java name */
    public /* synthetic */ void m4275lambda$new$0$comandroidserverlocationLocationManagerService() {
        refreshAppOpsRestrictions(-1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$1$com-android-server-location-LocationManagerService  reason: not valid java name */
    public /* synthetic */ void m4276lambda$new$1$comandroidserverlocationLocationManagerService() {
        refreshAppOpsRestrictions(-1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$2$com-android-server-location-LocationManagerService  reason: not valid java name */
    public /* synthetic */ void m4277lambda$new$2$comandroidserverlocationLocationManagerService(int userId, int change) {
        if (change == 2) {
            refreshAppOpsRestrictions(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$3$com-android-server-location-LocationManagerService  reason: not valid java name */
    public /* synthetic */ String[] m4278lambda$new$3$comandroidserverlocationLocationManagerService(int userId) {
        return this.mContext.getResources().getStringArray(17236084);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$4$com-android-server-location-LocationManagerService  reason: not valid java name */
    public /* synthetic */ String[] m4279lambda$new$4$comandroidserverlocationLocationManagerService(int userId) {
        return this.mContext.getResources().getStringArray(17236083);
    }

    LocationProviderManager getLocationProviderManager(String providerName) {
        if (providerName == null) {
            return null;
        }
        Iterator<LocationProviderManager> it = this.mProviderManagers.iterator();
        while (it.hasNext()) {
            LocationProviderManager manager = it.next();
            if (providerName.equals(manager.getName())) {
                return manager;
            }
        }
        return null;
    }

    private LocationProviderManager getOrAddLocationProviderManager(String providerName) {
        synchronized (this.mProviderManagers) {
            Iterator<LocationProviderManager> it = this.mProviderManagers.iterator();
            while (it.hasNext()) {
                LocationProviderManager manager = it.next();
                if (providerName.equals(manager.getName())) {
                    return manager;
                }
            }
            LocationProviderManager manager2 = new LocationProviderManager(this.mContext, this.mInjector, providerName, this.mPassiveManager);
            addLocationProviderManager(manager2, null);
            return manager2;
        }
    }

    private void addLocationProviderManager(LocationProviderManager manager, AbstractLocationProvider realProvider) {
        synchronized (this.mProviderManagers) {
            Preconditions.checkState(getLocationProviderManager(manager.getName()) == null);
            manager.startManager(this);
            if (realProvider != null) {
                if (manager != this.mPassiveManager) {
                    boolean enableStationaryThrottling = Settings.Global.getInt(this.mContext.getContentResolver(), "location_enable_stationary_throttle", 1) != 0;
                    if (enableStationaryThrottling) {
                        realProvider = new StationaryThrottlingLocationProvider(manager.getName(), this.mInjector, realProvider);
                    }
                }
                manager.setRealProvider(realProvider);
            }
            this.mProviderManagers.add(manager);
        }
    }

    private void removeLocationProviderManager(LocationProviderManager manager) {
        synchronized (this.mProviderManagers) {
            boolean removed = this.mProviderManagers.remove(manager);
            Preconditions.checkArgument(removed);
            manager.setMockProvider(null);
            manager.setRealProvider(null);
            manager.stopManager();
        }
    }

    void onSystemReady() {
        initMtkLocationManagerService(this.mContext, this.mInjector.getAppOpsHelper());
        if (Build.IS_DEBUGGABLE) {
            AppOpsManager appOps = (AppOpsManager) Objects.requireNonNull((AppOpsManager) this.mContext.getSystemService(AppOpsManager.class));
            appOps.startWatchingNoted(new int[]{1, 0}, new AppOpsManager.OnOpNotedListener() { // from class: com.android.server.location.LocationManagerService$$ExternalSyntheticLambda11
                public final void onOpNoted(int i, int i2, String str, String str2, int i3, int i4) {
                    LocationManagerService.this.m4280xb5ce09b0(i, i2, str, str2, i3, i4);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSystemReady$5$com-android-server-location-LocationManagerService  reason: not valid java name */
    public /* synthetic */ void m4280xb5ce09b0(int code, int uid, String packageName, String attributionTag, int flags, int result) {
        if (!isLocationEnabledForUser(UserHandle.getUserId(uid))) {
            Log.w(TAG, "location noteOp with location off - " + CallerIdentity.forTest(uid, 0, packageName, attributionTag));
        }
    }

    void onSystemThirdPartyAppsCanStart() {
        ProxyLocationProvider networkProvider1;
        ProxyLocationProvider networkProvider = ProxyLocationProvider.create(this.mContext, "network", "com.android.location.service.v3.NetworkLocationProvider", 17891641, 17040003);
        if (networkProvider == null) {
            Log.w(TAG, "no network location provider found");
        } else {
            LocationProviderManager networkManager = new LocationProviderManager(this.mContext, this.mInjector, "network", this.mPassiveManager);
            addLocationProviderManager(networkManager, networkProvider);
        }
        char c = 0;
        if (SystemProperties.getInt("ro.tran_here_nlp_support", 0) == 1 && (networkProvider1 = ProxyLocationProvider.create(this.mContext, "network1", "com.android.location.service.v3.NetworkLocationProvider", 17891798, 17040047)) != null) {
            LocationProviderManager networkManager1 = new LocationProviderManager(this.mContext, this.mInjector, "network1", this.mPassiveManager);
            addLocationProviderManager(networkManager1, networkProvider1);
        }
        Preconditions.checkState(!this.mContext.getPackageManager().queryIntentServicesAsUser(new Intent("com.android.location.service.FusedLocationProvider"), 1572864, 0).isEmpty(), "Unable to find a direct boot aware fused location provider");
        ProxyLocationProvider fusedProvider = ProxyLocationProvider.create(this.mContext, "fused", "com.android.location.service.FusedLocationProvider", 17891631, 17039977);
        if (fusedProvider == null) {
            Log.wtf(TAG, "no fused location provider found");
        } else {
            LocationProviderManager fusedManager = new LocationProviderManager(this.mContext, this.mInjector, "fused", this.mPassiveManager);
            addLocationProviderManager(fusedManager, fusedProvider);
        }
        if (GnssNative.isSupported()) {
            GnssConfiguration gnssConfiguration = new GnssConfiguration(this.mContext);
            GnssNative gnssNative = GnssNative.create(this.mInjector, gnssConfiguration);
            this.mGnssManagerService = new GnssManagerService(this.mContext, this.mInjector, gnssNative);
            this.mGnssManagerService.onSystemReady();
            LocationProviderManager gnssManager = new LocationProviderManager(this.mContext, this.mInjector, "gps", this.mPassiveManager);
            addLocationProviderManager(gnssManager, this.mGnssManagerService.getGnssLocationProvider());
        }
        GeocoderProxy createAndRegister = GeocoderProxy.createAndRegister(this.mContext);
        this.mGeocodeProvider = createAndRegister;
        if (createAndRegister == null) {
            Log.e(TAG, "no geocoder provider found");
        }
        HardwareActivityRecognitionProxy hardwareActivityRecognitionProxy = HardwareActivityRecognitionProxy.createAndRegister(this.mContext);
        if (hardwareActivityRecognitionProxy == null) {
            Log.e(TAG, "unable to bind ActivityRecognitionProxy");
        }
        if (this.mGnssManagerService != null) {
            GeofenceProxy provider = GeofenceProxy.createAndBind(this.mContext, this.mGnssManagerService.getGnssGeofenceProxy());
            if (provider == null) {
                Log.e(TAG, "unable to bind to GeofenceProxy");
            }
        }
        String[] testProviderStrings = this.mContext.getResources().getStringArray(17236140);
        int length = testProviderStrings.length;
        int i = 0;
        while (i < length) {
            String testProviderString = testProviderStrings[i];
            String[] fragments = testProviderString.split(",");
            String name = fragments[c].trim();
            ProviderProperties properties = new ProviderProperties.Builder().setHasNetworkRequirement(Boolean.parseBoolean(fragments[1])).setHasSatelliteRequirement(Boolean.parseBoolean(fragments[2])).setHasCellRequirement(Boolean.parseBoolean(fragments[3])).setHasMonetaryCost(Boolean.parseBoolean(fragments[4])).setHasAltitudeSupport(Boolean.parseBoolean(fragments[5])).setHasSpeedSupport(Boolean.parseBoolean(fragments[6])).setHasBearingSupport(Boolean.parseBoolean(fragments[7])).setPowerUsage(Integer.parseInt(fragments[8])).setAccuracy(Integer.parseInt(fragments[9])).build();
            LocationProviderManager manager = getOrAddLocationProviderManager(name);
            manager.setMockProvider(new MockLocationProvider(properties, CallerIdentity.fromContext(this.mContext), Collections.emptySet()));
            i++;
            c = 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLocationUserSettingsChanged(int userId, LocationUserSettings oldSettings, LocationUserSettings newSettings) {
        if (oldSettings.isAdasGnssLocationEnabled() != newSettings.isAdasGnssLocationEnabled()) {
            boolean enabled = newSettings.isAdasGnssLocationEnabled();
            if (D) {
                Log.d(TAG, "[u" + userId + "] adas gnss location enabled = " + enabled);
            }
            LocationEventLog.EVENT_LOG.logAdasLocationEnabled(userId, enabled);
            Intent intent = new Intent("android.location.action.ADAS_GNSS_ENABLED_CHANGED").putExtra("android.location.extra.ADAS_GNSS_ENABLED", enabled).addFlags(1073741824).addFlags(268435456);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.of(userId));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLocationModeChanged(int userId) {
        boolean enabled = this.mInjector.getSettingsHelper().isLocationEnabled(userId);
        LocationManager.invalidateLocalLocationEnabledCaches();
        Log.d(TAG, "[u" + userId + "] location enabled = " + enabled);
        LocationEventLog.EVENT_LOG.logLocationEnabled(userId, enabled);
        Intent intent = new Intent("android.location.MODE_CHANGED").putExtra("android.location.extra.LOCATION_ENABLED", enabled).addFlags(1073741824).addFlags(268435456);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.of(userId));
        refreshAppOpsRestrictions(userId);
    }

    public int getGnssYearOfHardware() {
        if (this.mGnssManagerService == null) {
            return 0;
        }
        return this.mGnssManagerService.getGnssYearOfHardware();
    }

    public String getGnssHardwareModelName() {
        return this.mGnssManagerService == null ? "" : this.mGnssManagerService.getGnssHardwareModelName();
    }

    public int getGnssBatchSize() {
        if (this.mGnssManagerService == null) {
            return 0;
        }
        return this.mGnssManagerService.getGnssBatchSize();
    }

    public void startGnssBatch(long periodNanos, ILocationListener listener, String packageName, String attributionTag, String listenerId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.LOCATION_HARDWARE", null);
        if (this.mGnssManagerService == null) {
            return;
        }
        long intervalMs = TimeUnit.NANOSECONDS.toMillis(periodNanos);
        synchronized (this.mDeprecatedGnssBatchingLock) {
            try {
                try {
                    stopGnssBatch();
                    registerLocationListener("gps", new LocationRequest.Builder(intervalMs).setMaxUpdateDelayMillis(this.mGnssManagerService.getGnssBatchSize() * intervalMs).setHiddenFromAppOps(true).build(), listener, packageName, attributionTag, listenerId);
                    this.mDeprecatedGnssBatchingListener = listener;
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public void flushGnssBatch() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.LOCATION_HARDWARE", null);
        if (this.mGnssManagerService == null) {
            return;
        }
        synchronized (this.mDeprecatedGnssBatchingLock) {
            ILocationListener iLocationListener = this.mDeprecatedGnssBatchingListener;
            if (iLocationListener != null) {
                requestListenerFlush("gps", iLocationListener, 0);
            }
        }
    }

    public void stopGnssBatch() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.LOCATION_HARDWARE", null);
        if (this.mGnssManagerService == null) {
            return;
        }
        synchronized (this.mDeprecatedGnssBatchingLock) {
            ILocationListener listener = this.mDeprecatedGnssBatchingListener;
            if (listener != null) {
                this.mDeprecatedGnssBatchingListener = null;
                unregisterLocationListener(listener);
            }
        }
    }

    public boolean hasProvider(String provider) {
        return getLocationProviderManager(provider) != null;
    }

    public List<String> getAllProviders() {
        ArrayList<String> providers = new ArrayList<>(this.mProviderManagers.size());
        Iterator<LocationProviderManager> it = this.mProviderManagers.iterator();
        while (it.hasNext()) {
            LocationProviderManager manager = it.next();
            providers.add(manager.getName());
        }
        return providers;
    }

    public List<String> getProviders(Criteria criteria, boolean enabledOnly) {
        ArrayList<String> providers;
        if (!LocationPermissions.checkCallingOrSelfLocationPermission(this.mContext, 1)) {
            return Collections.emptyList();
        }
        synchronized (this.mLock) {
            providers = new ArrayList<>(this.mProviderManagers.size());
            Iterator<LocationProviderManager> it = this.mProviderManagers.iterator();
            while (it.hasNext()) {
                LocationProviderManager manager = it.next();
                String name = manager.getName();
                if (!enabledOnly || manager.isEnabled(UserHandle.getCallingUserId())) {
                    if (criteria == null || LocationProvider.propertiesMeetCriteria(name, manager.getProperties(), criteria)) {
                        providers.add(name);
                    }
                }
            }
        }
        return providers;
    }

    public String getBestProvider(Criteria criteria, boolean enabledOnly) {
        List<String> providers;
        synchronized (this.mLock) {
            providers = getProviders(criteria, enabledOnly);
            if (providers.isEmpty()) {
                providers = getProviders(null, enabledOnly);
            }
        }
        if (providers.isEmpty()) {
            return null;
        }
        if (providers.contains("fused")) {
            return "fused";
        }
        if (providers.contains("gps")) {
            return "gps";
        }
        if (providers.contains("network")) {
            return "network";
        }
        return providers.get(0);
    }

    public String[] getBackgroundThrottlingWhitelist() {
        return (String[]) this.mInjector.getSettingsHelper().getBackgroundThrottlePackageWhitelist().toArray(new String[0]);
    }

    public PackageTagsList getIgnoreSettingsAllowlist() {
        return this.mInjector.getSettingsHelper().getIgnoreSettingsAllowlist();
    }

    public ICancellationSignal getCurrentLocation(String provider, LocationRequest request, ILocationCallback consumer, String packageName, String attributionTag, String listenerId) {
        CallerIdentity identity = CallerIdentity.fromBinder(this.mContext, packageName, attributionTag, listenerId);
        int permissionLevel = LocationPermissions.getPermissionLevel(this.mContext, identity.getUid(), identity.getPid());
        LocationPermissions.enforceLocationPermission(identity.getUid(), permissionLevel, 1);
        Preconditions.checkState((identity.getPid() == Process.myPid() && attributionTag == null) ? false : true);
        LocationRequest request2 = validateLocationRequest(provider, request, identity);
        LocationProviderManager manager = getLocationProviderManager(provider);
        Preconditions.checkArgument(manager != null, "provider \"" + provider + "\" does not exist");
        return manager.getCurrentLocation(request2, identity, permissionLevel, consumer);
    }

    public void registerLocationListener(String provider, LocationRequest request, ILocationListener listener, String packageName, String attributionTag, String listenerId) {
        CallerIdentity identity = CallerIdentity.fromBinder(this.mContext, packageName, attributionTag, listenerId);
        int permissionLevel = LocationPermissions.getPermissionLevel(this.mContext, identity.getUid(), identity.getPid());
        LocationPermissions.enforceLocationPermission(identity.getUid(), permissionLevel, 1);
        if (identity.getPid() == Process.myPid() && attributionTag == null) {
            Log.w(TAG, "system location request with no attribution tag", new IllegalArgumentException());
        }
        LocationRequest request2 = validateLocationRequest(provider, request, identity);
        if (SystemProperties.getInt("ro.tran_here_nlp_support", 0) == 1 && Settings.Secure.getInt(this.mContext.getContentResolver(), "com.here.consent.COLLECT_RADIO_SIGNALS", 0) == 1 && Settings.Secure.getInt(this.mContext.getContentResolver(), "cloud_here_enable", 1) == 1 && "network".equals(provider)) {
            provider = "network1";
        }
        LocationProviderManager manager = getLocationProviderManager(provider);
        Preconditions.checkArgument(manager != null, "provider \"" + provider + "\" does not exist");
        if (manager == null) {
            final String proxyProider = provider;
            FgThread.getHandler().post(new Runnable() { // from class: com.android.server.location.LocationManagerService$$ExternalSyntheticLambda10
                @Override // java.lang.Runnable
                public final void run() {
                    LocationManagerService.mtkShowNlpNotInstalledToast(proxyProider);
                }
            });
        }
        manager.registerLocationRequest(request2, identity, permissionLevel, listener);
    }

    public void registerLocationPendingIntent(String provider, LocationRequest request, PendingIntent pendingIntent, String packageName, String attributionTag) {
        CallerIdentity identity = CallerIdentity.fromBinder(this.mContext, packageName, attributionTag, AppOpsManager.toReceiverId(pendingIntent));
        int permissionLevel = LocationPermissions.getPermissionLevel(this.mContext, identity.getUid(), identity.getPid());
        LocationPermissions.enforceLocationPermission(identity.getUid(), permissionLevel, 1);
        Preconditions.checkArgument((identity.getPid() == Process.myPid() && attributionTag == null) ? false : true);
        if (CompatChanges.isChangeEnabled(169887240L, identity.getUid())) {
            boolean usesSystemApi = request.isLowPower() || request.isHiddenFromAppOps() || request.isLocationSettingsIgnored() || !request.getWorkSource().isEmpty();
            if (usesSystemApi) {
                throw new SecurityException("PendingIntent location requests may not use system APIs: " + request);
            }
        }
        LocationRequest request2 = validateLocationRequest(provider, request, identity);
        LocationProviderManager manager = getLocationProviderManager(provider);
        Preconditions.checkArgument(manager != null, "provider \"" + provider + "\" does not exist");
        manager.registerLocationRequest(request2, identity, permissionLevel, pendingIntent);
    }

    private LocationRequest validateLocationRequest(String provider, LocationRequest request, CallerIdentity identity) {
        if (!request.getWorkSource().isEmpty()) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", "setting a work source requires android.permission.UPDATE_DEVICE_STATS");
        }
        LocationRequest.Builder sanitized = new LocationRequest.Builder(request);
        if (!CompatChanges.isChangeEnabled(168936375L, Binder.getCallingUid()) && this.mContext.checkCallingPermission("android.permission.LOCATION_HARDWARE") != 0) {
            sanitized.setLowPower(false);
        }
        WorkSource workSource = new WorkSource(request.getWorkSource());
        if (workSource.size() > 0 && workSource.getPackageName(0) == null) {
            Log.w(TAG, "received (and ignoring) illegal worksource with no package name");
            workSource.clear();
        } else {
            List<WorkSource.WorkChain> workChains = workSource.getWorkChains();
            if (workChains != null && !workChains.isEmpty() && workChains.get(0).getAttributionTag() == null) {
                Log.w(TAG, "received (and ignoring) illegal worksource with no attribution tag");
                workSource.clear();
            }
        }
        if (workSource.isEmpty()) {
            identity.addToWorkSource(workSource);
        }
        sanitized.setWorkSource(workSource);
        LocationRequest request2 = sanitized.build();
        boolean isLocationProvider = this.mLocalService.isProvider(null, identity);
        if (request2.isLowPower() && CompatChanges.isChangeEnabled(168936375L, identity.getUid())) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.LOCATION_HARDWARE", "low power request requires android.permission.LOCATION_HARDWARE");
        }
        if (request2.isHiddenFromAppOps()) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_APP_OPS_STATS", "hiding from app ops requires android.permission.UPDATE_APP_OPS_STATS");
        }
        if (request2.isAdasGnssBypass()) {
            if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
                throw new IllegalArgumentException("adas gnss bypass requests are only allowed on automotive devices");
            }
            if (!"gps".equals(provider)) {
                throw new IllegalArgumentException("adas gnss bypass requests are only allowed on the \"gps\" provider");
            }
            if (!isLocationProvider) {
                LocationPermissions.enforceCallingOrSelfBypassPermission(this.mContext);
            }
        }
        if (request2.isLocationSettingsIgnored() && !isLocationProvider) {
            LocationPermissions.enforceCallingOrSelfBypassPermission(this.mContext);
        }
        return request2;
    }

    public void requestListenerFlush(String provider, ILocationListener listener, int requestCode) {
        LocationProviderManager manager = getLocationProviderManager(provider);
        Preconditions.checkArgument(manager != null, "provider \"" + provider + "\" does not exist");
        manager.flush((ILocationListener) Objects.requireNonNull(listener), requestCode);
    }

    public void requestPendingIntentFlush(String provider, PendingIntent pendingIntent, int requestCode) {
        LocationProviderManager manager = getLocationProviderManager(provider);
        Preconditions.checkArgument(manager != null, "provider \"" + provider + "\" does not exist");
        manager.flush((PendingIntent) Objects.requireNonNull(pendingIntent), requestCode);
    }

    public void unregisterLocationListener(ILocationListener listener) {
        Iterator<LocationProviderManager> it = this.mProviderManagers.iterator();
        while (it.hasNext()) {
            LocationProviderManager manager = it.next();
            manager.unregisterLocationRequest(listener);
        }
    }

    public void unregisterLocationPendingIntent(PendingIntent pendingIntent) {
        Iterator<LocationProviderManager> it = this.mProviderManagers.iterator();
        while (it.hasNext()) {
            LocationProviderManager manager = it.next();
            manager.unregisterLocationRequest(pendingIntent);
        }
    }

    public Location getLastLocation(String provider, LastLocationRequest request, String packageName, String attributionTag) {
        CallerIdentity identity = CallerIdentity.fromBinder(this.mContext, packageName, attributionTag);
        int permissionLevel = LocationPermissions.getPermissionLevel(this.mContext, identity.getUid(), identity.getPid());
        boolean z = true;
        LocationPermissions.enforceLocationPermission(identity.getUid(), permissionLevel, 1);
        if (identity.getPid() == Process.myPid() && attributionTag == null) {
            z = false;
        }
        Preconditions.checkArgument(z);
        LastLocationRequest request2 = validateLastLocationRequest(provider, request, identity);
        LocationProviderManager manager = getLocationProviderManager(provider);
        if (manager == null) {
            return null;
        }
        return manager.getLastLocation(request2, identity, permissionLevel);
    }

    private LastLocationRequest validateLastLocationRequest(String provider, LastLocationRequest request, CallerIdentity identity) {
        LastLocationRequest.Builder sanitized = new LastLocationRequest.Builder(request);
        LastLocationRequest request2 = sanitized.build();
        boolean isLocationProvider = this.mLocalService.isProvider(null, identity);
        if (request2.isHiddenFromAppOps()) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_APP_OPS_STATS", "hiding from app ops requires android.permission.UPDATE_APP_OPS_STATS");
        }
        if (request2.isAdasGnssBypass()) {
            if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
                throw new IllegalArgumentException("adas gnss bypass requests are only allowed on automotive devices");
            }
            if (!"gps".equals(provider)) {
                throw new IllegalArgumentException("adas gnss bypass requests are only allowed on the \"gps\" provider");
            }
            if (!isLocationProvider) {
                LocationPermissions.enforceCallingOrSelfBypassPermission(this.mContext);
            }
        }
        if (request2.isLocationSettingsIgnored() && !isLocationProvider) {
            LocationPermissions.enforceCallingOrSelfBypassPermission(this.mContext);
        }
        return request2;
    }

    public LocationTime getGnssTimeMillis() {
        return this.mLocalService.getGnssTimeMillis();
    }

    public void injectLocation(Location location) {
        this.mContext.enforceCallingPermission("android.permission.LOCATION_HARDWARE", null);
        this.mContext.enforceCallingPermission("android.permission.ACCESS_FINE_LOCATION", null);
        Preconditions.checkArgument(location.isComplete());
        int userId = UserHandle.getCallingUserId();
        LocationProviderManager manager = getLocationProviderManager(location.getProvider());
        if (manager != null && manager.isEnabled(userId)) {
            manager.injectLastLocation((Location) Objects.requireNonNull(location), userId);
        }
    }

    public void requestGeofence(Geofence geofence, PendingIntent intent, String packageName, String attributionTag) {
        this.mGeofenceManager.addGeofence(geofence, intent, packageName, attributionTag);
    }

    public void removeGeofence(PendingIntent pendingIntent) {
        this.mGeofenceManager.removeGeofence(pendingIntent);
    }

    public void registerGnssStatusCallback(IGnssStatusListener listener, String packageName, String attributionTag, String listenerId) {
        if (this.mGnssManagerService != null) {
            this.mGnssManagerService.registerGnssStatusCallback(listener, packageName, attributionTag, listenerId);
        }
    }

    public void unregisterGnssStatusCallback(IGnssStatusListener listener) {
        if (this.mGnssManagerService != null) {
            this.mGnssManagerService.unregisterGnssStatusCallback(listener);
        }
    }

    public void registerGnssNmeaCallback(IGnssNmeaListener listener, String packageName, String attributionTag, String listenerId) {
        if (this.mGnssManagerService != null) {
            this.mGnssManagerService.registerGnssNmeaCallback(listener, packageName, attributionTag, listenerId);
        }
    }

    public void unregisterGnssNmeaCallback(IGnssNmeaListener listener) {
        if (this.mGnssManagerService != null) {
            this.mGnssManagerService.unregisterGnssNmeaCallback(listener);
        }
    }

    public void addGnssMeasurementsListener(GnssMeasurementRequest request, IGnssMeasurementsListener listener, String packageName, String attributionTag, String listenerId) {
        if (this.mGnssManagerService != null) {
            this.mGnssManagerService.addGnssMeasurementsListener(request, listener, packageName, attributionTag, listenerId);
        }
    }

    public void removeGnssMeasurementsListener(IGnssMeasurementsListener listener) {
        if (this.mGnssManagerService != null) {
            this.mGnssManagerService.removeGnssMeasurementsListener(listener);
        }
    }

    public void addGnssAntennaInfoListener(IGnssAntennaInfoListener listener, String packageName, String attributionTag, String listenerId) {
        if (this.mGnssManagerService != null) {
            this.mGnssManagerService.addGnssAntennaInfoListener(listener, packageName, attributionTag, listenerId);
        }
    }

    public void removeGnssAntennaInfoListener(IGnssAntennaInfoListener listener) {
        if (this.mGnssManagerService != null) {
            this.mGnssManagerService.removeGnssAntennaInfoListener(listener);
        }
    }

    public void addProviderRequestListener(IProviderRequestListener listener) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS") == 0) {
            Iterator<LocationProviderManager> it = this.mProviderManagers.iterator();
            while (it.hasNext()) {
                LocationProviderManager manager = it.next();
                manager.addProviderRequestListener(listener);
            }
        }
    }

    public void removeProviderRequestListener(IProviderRequestListener listener) {
        Iterator<LocationProviderManager> it = this.mProviderManagers.iterator();
        while (it.hasNext()) {
            LocationProviderManager manager = it.next();
            manager.removeProviderRequestListener(listener);
        }
    }

    public void injectGnssMeasurementCorrections(GnssMeasurementCorrections corrections) {
        if (this.mGnssManagerService != null) {
            this.mGnssManagerService.injectGnssMeasurementCorrections(corrections);
        }
    }

    public GnssCapabilities getGnssCapabilities() {
        return this.mGnssManagerService == null ? new GnssCapabilities.Builder().build() : this.mGnssManagerService.getGnssCapabilities();
    }

    public List<GnssAntennaInfo> getGnssAntennaInfos() {
        if (this.mGnssManagerService == null) {
            return null;
        }
        return this.mGnssManagerService.getGnssAntennaInfos();
    }

    public void addGnssNavigationMessageListener(IGnssNavigationMessageListener listener, String packageName, String attributionTag, String listenerId) {
        if (this.mGnssManagerService != null) {
            this.mGnssManagerService.addGnssNavigationMessageListener(listener, packageName, attributionTag, listenerId);
        }
    }

    public void removeGnssNavigationMessageListener(IGnssNavigationMessageListener listener) {
        if (this.mGnssManagerService != null) {
            this.mGnssManagerService.removeGnssNavigationMessageListener(listener);
        }
    }

    public void sendExtraCommand(String provider, String command, Bundle extras) {
        LocationPermissions.enforceCallingOrSelfLocationPermission(this.mContext, 1);
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_LOCATION_EXTRA_COMMANDS", null);
        LocationProviderManager manager = getLocationProviderManager((String) Objects.requireNonNull(provider));
        if (manager != null) {
            manager.sendExtraCommand(Binder.getCallingUid(), Binder.getCallingPid(), (String) Objects.requireNonNull(command), extras);
        }
        this.mInjector.getLocationUsageLogger().logLocationApiUsage(0, 5, provider);
        this.mInjector.getLocationUsageLogger().logLocationApiUsage(1, 5, provider);
    }

    public ProviderProperties getProviderProperties(String provider) {
        LocationProviderManager manager = getLocationProviderManager(provider);
        Preconditions.checkArgument(manager != null, "provider \"" + provider + "\" does not exist");
        return manager.getProperties();
    }

    public boolean isProviderPackage(String provider, String packageName, String attributionTag) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_DEVICE_CONFIG", null);
        Iterator<LocationProviderManager> it = this.mProviderManagers.iterator();
        while (it.hasNext()) {
            LocationProviderManager manager = it.next();
            if (provider == null || provider.equals(manager.getName())) {
                CallerIdentity identity = manager.getProviderIdentity();
                if (identity != null && identity.getPackageName().equals(packageName) && (attributionTag == null || Objects.equals(identity.getAttributionTag(), attributionTag))) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<String> getProviderPackages(String provider) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_DEVICE_CONFIG", null);
        LocationProviderManager manager = getLocationProviderManager(provider);
        if (manager == null) {
            return Collections.emptyList();
        }
        CallerIdentity identity = manager.getProviderIdentity();
        if (identity == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(identity.getPackageName());
    }

    public void setExtraLocationControllerPackage(String packageName) {
        this.mContext.enforceCallingPermission("android.permission.LOCATION_HARDWARE", "android.permission.LOCATION_HARDWARE permission required");
        synchronized (this.mLock) {
            this.mExtraLocationControllerPackage = packageName;
        }
    }

    public String getExtraLocationControllerPackage() {
        String str;
        synchronized (this.mLock) {
            str = this.mExtraLocationControllerPackage;
        }
        return str;
    }

    public void setExtraLocationControllerPackageEnabled(boolean enabled) {
        this.mContext.enforceCallingPermission("android.permission.LOCATION_HARDWARE", "android.permission.LOCATION_HARDWARE permission required");
        synchronized (this.mLock) {
            this.mExtraLocationControllerPackageEnabled = enabled;
        }
    }

    public boolean isExtraLocationControllerPackageEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mExtraLocationControllerPackageEnabled && this.mExtraLocationControllerPackage != null;
        }
        return z;
    }

    public void setLocationEnabledForUser(boolean enabled, int userId) {
        int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, false, "setLocationEnabledForUser", null);
        this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS", null);
        LocationManager.invalidateLocalLocationEnabledCaches();
        this.mInjector.getSettingsHelper().setLocationEnabled(enabled, userId2);
    }

    public boolean isLocationEnabledForUser(int userId) {
        return this.mInjector.getSettingsHelper().isLocationEnabled(ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, false, "isLocationEnabledForUser", null));
    }

    public void setAdasGnssLocationEnabledForUser(final boolean enabled, int userId) {
        int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, false, "setAdasGnssLocationEnabledForUser", null);
        LocationPermissions.enforceCallingOrSelfBypassPermission(this.mContext);
        this.mInjector.getLocationSettings().updateUserSettings(userId2, new Function() { // from class: com.android.server.location.LocationManagerService$$ExternalSyntheticLambda12
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                LocationUserSettings withAdasGnssLocationEnabled;
                withAdasGnssLocationEnabled = ((LocationUserSettings) obj).withAdasGnssLocationEnabled(enabled);
                return withAdasGnssLocationEnabled;
            }
        });
    }

    public boolean isAdasGnssLocationEnabledForUser(int userId) {
        return this.mInjector.getLocationSettings().getUserSettings(ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, false, "isAdasGnssLocationEnabledForUser", null)).isAdasGnssLocationEnabled();
    }

    public boolean isProviderEnabledForUser(String provider, int userId) {
        return this.mLocalService.isProviderEnabledForUser(provider, userId);
    }

    public void setAutomotiveGnssSuspended(boolean suspended) {
        this.mContext.enforceCallingPermission("android.permission.CONTROL_AUTOMOTIVE_GNSS", null);
        if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
            throw new IllegalStateException("setAutomotiveGnssSuspended only allowed on automotive devices");
        }
        this.mGnssManagerService.setAutomotiveGnssSuspended(suspended);
    }

    public boolean isAutomotiveGnssSuspended() {
        this.mContext.enforceCallingPermission("android.permission.CONTROL_AUTOMOTIVE_GNSS", null);
        if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
            throw new IllegalStateException("isAutomotiveGnssSuspended only allowed on automotive devices");
        }
        return this.mGnssManagerService.isAutomotiveGnssSuspended();
    }

    public boolean geocoderIsPresent() {
        return this.mGeocodeProvider != null;
    }

    public void getFromLocation(double latitude, double longitude, int maxResults, GeocoderParams params, IGeocodeListener listener) {
        CallerIdentity identity = CallerIdentity.fromBinder(this.mContext, params.getClientPackage(), params.getClientAttributionTag());
        Preconditions.checkArgument(identity.getUid() == params.getClientUid());
        GeocoderProxy geocoderProxy = this.mGeocodeProvider;
        if (geocoderProxy != null) {
            geocoderProxy.getFromLocation(latitude, longitude, maxResults, params, listener);
            return;
        }
        try {
            try {
                listener.onResults((String) null, Collections.emptyList());
            } catch (RemoteException e) {
            }
        } catch (RemoteException e2) {
        }
    }

    public void getFromLocationName(String locationName, double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude, int maxResults, GeocoderParams params, IGeocodeListener listener) {
        CallerIdentity identity = CallerIdentity.fromBinder(this.mContext, params.getClientPackage(), params.getClientAttributionTag());
        Preconditions.checkArgument(identity.getUid() == params.getClientUid());
        GeocoderProxy geocoderProxy = this.mGeocodeProvider;
        if (geocoderProxy != null) {
            geocoderProxy.getFromLocationName(locationName, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, maxResults, params, listener);
            return;
        }
        try {
            try {
                listener.onResults((String) null, Collections.emptyList());
            } catch (RemoteException e) {
            }
        } catch (RemoteException e2) {
        }
    }

    public void addTestProvider(String provider, ProviderProperties properties, List<String> extraAttributionTags, String packageName, String attributionTag) {
        LocationProviderManager manager1;
        CallerIdentity identity = CallerIdentity.fromBinderUnsafe(packageName, attributionTag);
        if (!this.mInjector.getAppOpsHelper().noteOp(58, identity)) {
            return;
        }
        LocationProviderManager manager = getOrAddLocationProviderManager(provider);
        manager.setMockProvider(new MockLocationProvider(properties, identity, new ArraySet(extraAttributionTags)));
        if (SystemProperties.getInt("ro.tran_here_nlp_support", 0) == 1 && "network".equals(provider) && (manager1 = getOrAddLocationProviderManager("network1")) != null) {
            manager1.setMockProvider(new MockLocationProvider(properties, identity, new ArraySet(extraAttributionTags)));
        }
    }

    public void removeTestProvider(String provider, String packageName, String attributionTag) {
        CallerIdentity identity = CallerIdentity.fromBinderUnsafe(packageName, attributionTag);
        if (!this.mInjector.getAppOpsHelper().noteOp(58, identity)) {
            return;
        }
        synchronized (this.mLock) {
            LocationProviderManager manager = getLocationProviderManager(provider);
            if (manager == null) {
                return;
            }
            manager.setMockProvider(null);
            if (!manager.hasProvider()) {
                removeLocationProviderManager(manager);
            }
        }
    }

    public void setTestProviderLocation(String provider, Location location, String packageName, String attributionTag) {
        CallerIdentity identity = CallerIdentity.fromBinderUnsafe(packageName, attributionTag);
        if (!this.mInjector.getAppOpsHelper().noteOp(58, identity)) {
            return;
        }
        Preconditions.checkArgument(location.isComplete(), "incomplete location object, missing timestamp or accuracy?");
        LocationProviderManager manager = getLocationProviderManager(provider);
        if (manager == null) {
            throw new IllegalArgumentException("provider doesn't exist: " + provider);
        }
        manager.setMockProviderLocation(location);
    }

    public void setTestProviderEnabled(String provider, boolean enabled, String packageName, String attributionTag) {
        CallerIdentity identity = CallerIdentity.fromBinderUnsafe(packageName, attributionTag);
        if (!this.mInjector.getAppOpsHelper().noteOp(58, identity)) {
            return;
        }
        LocationProviderManager manager = getLocationProviderManager(provider);
        if (manager == null) {
            throw new IllegalArgumentException("provider doesn't exist: " + provider);
        }
        manager.setMockProviderAllowed(enabled);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r6v0, resolved type: com.android.server.location.LocationManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    public int handleShellCommand(ParcelFileDescriptor in, ParcelFileDescriptor out, ParcelFileDescriptor err, String[] args) {
        return new LocationShellCommand(this.mContext, this).exec(this, in.getFileDescriptor(), out.getFileDescriptor(), err.getFileDescriptor(), args);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (!DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            return;
        }
        final IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        if (args.length > 0) {
            LocationProviderManager manager = getLocationProviderManager(args[0]);
            if (manager != null) {
                ipw.println("Provider:");
                ipw.increaseIndent();
                manager.dump(fd, ipw, args);
                ipw.decreaseIndent();
                ipw.println("Event Log:");
                ipw.increaseIndent();
                LocationEventLog locationEventLog = LocationEventLog.EVENT_LOG;
                Objects.requireNonNull(ipw);
                locationEventLog.iterate(new Consumer() { // from class: com.android.server.location.LocationManagerService$$ExternalSyntheticLambda9
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ipw.println((String) obj);
                    }
                }, manager.getName());
                ipw.decreaseIndent();
                return;
            } else if ("--gnssmetrics".equals(args[0])) {
                if (this.mGnssManagerService != null) {
                    this.mGnssManagerService.dump(fd, ipw, args);
                    return;
                }
                return;
            }
        }
        ipw.println("Location Manager State:");
        ipw.increaseIndent();
        ipw.println("User Info:");
        ipw.increaseIndent();
        this.mInjector.getUserInfoHelper().dump(fd, ipw, args);
        ipw.decreaseIndent();
        ipw.println("Location Settings:");
        ipw.increaseIndent();
        this.mInjector.getSettingsHelper().dump(fd, ipw, args);
        this.mInjector.getLocationSettings().dump(fd, ipw, args);
        ipw.decreaseIndent();
        synchronized (this.mLock) {
            if (this.mExtraLocationControllerPackage != null) {
                ipw.println("Location Controller Extra Package: " + this.mExtraLocationControllerPackage + (this.mExtraLocationControllerPackageEnabled ? " [enabled]" : " [disabled]"));
            }
        }
        ipw.println("Location Providers:");
        ipw.increaseIndent();
        Iterator<LocationProviderManager> it = this.mProviderManagers.iterator();
        while (it.hasNext()) {
            it.next().dump(fd, ipw, args);
        }
        ipw.decreaseIndent();
        ipw.println("Historical Aggregate Location Provider Data:");
        ipw.increaseIndent();
        ArrayMap<String, ArrayMap<CallerIdentity, LocationEventLog.AggregateStats>> aggregateStats = LocationEventLog.EVENT_LOG.copyAggregateStats();
        for (int i = 0; i < aggregateStats.size(); i++) {
            ipw.print(aggregateStats.keyAt(i));
            ipw.println(":");
            ipw.increaseIndent();
            ArrayMap<CallerIdentity, LocationEventLog.AggregateStats> providerStats = aggregateStats.valueAt(i);
            for (int j = 0; j < providerStats.size(); j++) {
                ipw.print(providerStats.keyAt(j));
                ipw.print(": ");
                providerStats.valueAt(j).updateTotals();
                ipw.println(providerStats.valueAt(j));
            }
            ipw.decreaseIndent();
        }
        ipw.decreaseIndent();
        if (this.mGnssManagerService != null) {
            ipw.println("GNSS Manager:");
            ipw.increaseIndent();
            this.mGnssManagerService.dump(fd, ipw, args);
            ipw.decreaseIndent();
        }
        ipw.println("Geofence Manager:");
        ipw.increaseIndent();
        this.mGeofenceManager.dump(fd, ipw, args);
        ipw.decreaseIndent();
        ipw.println("Event Log:");
        ipw.increaseIndent();
        LocationEventLog locationEventLog2 = LocationEventLog.EVENT_LOG;
        Objects.requireNonNull(ipw);
        locationEventLog2.iterate(new Consumer() { // from class: com.android.server.location.LocationManagerService$$ExternalSyntheticLambda9
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ipw.println((String) obj);
            }
        });
        ipw.decreaseIndent();
    }

    @Override // com.android.server.location.provider.LocationProviderManager.StateChangedListener
    public void onStateChanged(String provider, AbstractLocationProvider.State oldState, AbstractLocationProvider.State newState) {
        if (!Objects.equals(oldState.identity, newState.identity)) {
            refreshAppOpsRestrictions(-1);
        }
        if (!oldState.extraAttributionTags.equals(newState.extraAttributionTags) || !Objects.equals(oldState.identity, newState.identity)) {
            synchronized (this.mLock) {
                final LocationManagerInternal.LocationPackageTagsListener listener = this.mLocationTagsChangedListener;
                if (listener != null) {
                    final int oldUid = oldState.identity != null ? oldState.identity.getUid() : -1;
                    final int newUid = newState.identity != null ? newState.identity.getUid() : -1;
                    if (oldUid != -1) {
                        final PackageTagsList tags = calculateAppOpsLocationSourceTags(oldUid);
                        FgThread.getHandler().post(new Runnable() { // from class: com.android.server.location.LocationManagerService$$ExternalSyntheticLambda0
                            @Override // java.lang.Runnable
                            public final void run() {
                                listener.onLocationPackageTagsChanged(oldUid, tags);
                            }
                        });
                    }
                    if (newUid != -1 && newUid != oldUid) {
                        final PackageTagsList tags2 = calculateAppOpsLocationSourceTags(newUid);
                        FgThread.getHandler().post(new Runnable() { // from class: com.android.server.location.LocationManagerService$$ExternalSyntheticLambda1
                            @Override // java.lang.Runnable
                            public final void run() {
                                listener.onLocationPackageTagsChanged(newUid, tags2);
                            }
                        });
                    }
                }
            }
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r9v0, resolved type: com.android.server.location.LocationManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    private void refreshAppOpsRestrictions(int userId) {
        if (userId == -1) {
            int[] runningUserIds = this.mInjector.getUserInfoHelper().getRunningUserIds();
            for (int i : runningUserIds) {
                refreshAppOpsRestrictions(i);
            }
            return;
        }
        Preconditions.checkArgument(userId >= 0);
        boolean enabled = this.mInjector.getSettingsHelper().isLocationEnabled(userId);
        PackageTagsList allowedPackages = null;
        if (!enabled) {
            PackageTagsList.Builder builder = new PackageTagsList.Builder();
            Iterator<LocationProviderManager> it = this.mProviderManagers.iterator();
            while (it.hasNext()) {
                LocationProviderManager manager = it.next();
                CallerIdentity identity = manager.getProviderIdentity();
                if (identity != null) {
                    builder.add(identity.getPackageName(), identity.getAttributionTag());
                }
            }
            builder.add(this.mInjector.getSettingsHelper().getIgnoreSettingsAllowlist());
            builder.add(this.mInjector.getSettingsHelper().getAdasAllowlist());
            allowedPackages = builder.build();
        }
        AppOpsManager appOpsManager = (AppOpsManager) Objects.requireNonNull((AppOpsManager) this.mContext.getSystemService(AppOpsManager.class));
        appOpsManager.setUserRestrictionForUser(0, !enabled, this, allowedPackages, userId);
        appOpsManager.setUserRestrictionForUser(1, !enabled, this, allowedPackages, userId);
    }

    PackageTagsList calculateAppOpsLocationSourceTags(int uid) {
        PackageTagsList.Builder builder = new PackageTagsList.Builder();
        Iterator<LocationProviderManager> it = this.mProviderManagers.iterator();
        while (it.hasNext()) {
            LocationProviderManager manager = it.next();
            AbstractLocationProvider.State managerState = manager.getState();
            if (managerState.identity != null && managerState.identity.getUid() == uid) {
                builder.add(managerState.identity.getPackageName(), managerState.extraAttributionTags);
                if (managerState.extraAttributionTags.isEmpty() || managerState.identity.getAttributionTag() != null) {
                    builder.add(managerState.identity.getPackageName(), managerState.identity.getAttributionTag());
                } else {
                    Log.e(TAG, manager.getName() + " provider has specified a null attribution tag and a non-empty set of extra attribution tags - dropping the null attribution tag");
                }
            }
        }
        return builder.build();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class LocalService extends LocationManagerInternal {
        LocalService() {
        }

        public boolean isProviderEnabledForUser(String provider, int userId) {
            int userId2 = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, false, "isProviderEnabledForUser", null);
            LocationProviderManager manager = LocationManagerService.this.getLocationProviderManager(provider);
            if (manager == null) {
                return false;
            }
            return manager.isEnabled(userId2);
        }

        public void addProviderEnabledListener(String provider, LocationManagerInternal.ProviderEnabledListener listener) {
            LocationProviderManager manager = (LocationProviderManager) Objects.requireNonNull(LocationManagerService.this.getLocationProviderManager(provider));
            manager.addEnabledListener(listener);
        }

        public void removeProviderEnabledListener(String provider, LocationManagerInternal.ProviderEnabledListener listener) {
            LocationProviderManager manager = (LocationProviderManager) Objects.requireNonNull(LocationManagerService.this.getLocationProviderManager(provider));
            manager.removeEnabledListener(listener);
        }

        public boolean isProvider(String provider, CallerIdentity identity) {
            Iterator<LocationProviderManager> it = LocationManagerService.this.mProviderManagers.iterator();
            while (it.hasNext()) {
                LocationProviderManager manager = it.next();
                if (provider == null || provider.equals(manager.getName())) {
                    if (identity.equals(manager.getProviderIdentity())) {
                        return true;
                    }
                }
            }
            return false;
        }

        public void sendNiResponse(int notifId, int userResponse) {
            if (LocationManagerService.this.mGnssManagerService != null) {
                LocationManagerService.this.mGnssManagerService.sendNiResponse(notifId, userResponse);
            }
        }

        public LocationTime getGnssTimeMillis() {
            Location location;
            LocationProviderManager gpsManager = LocationManagerService.this.getLocationProviderManager("gps");
            if (gpsManager == null || (location = gpsManager.getLastLocationUnsafe(-1, 2, false, JobStatus.NO_LATEST_RUNTIME)) == null) {
                return null;
            }
            return new LocationTime(location.getTime(), location.getElapsedRealtimeNanos());
        }

        public void setLocationPackageTagsListener(final LocationManagerInternal.LocationPackageTagsListener listener) {
            synchronized (LocationManagerService.this.mLock) {
                LocationManagerService.this.mLocationTagsChangedListener = listener;
                if (listener != null) {
                    ArraySet<Integer> uids = new ArraySet<>(LocationManagerService.this.mProviderManagers.size());
                    Iterator<LocationProviderManager> it = LocationManagerService.this.mProviderManagers.iterator();
                    while (it.hasNext()) {
                        LocationProviderManager manager = it.next();
                        CallerIdentity identity = manager.getProviderIdentity();
                        if (identity != null) {
                            uids.add(Integer.valueOf(identity.getUid()));
                        }
                    }
                    Iterator<Integer> it2 = uids.iterator();
                    while (it2.hasNext()) {
                        final int uid = it2.next().intValue();
                        final PackageTagsList tags = LocationManagerService.this.calculateAppOpsLocationSourceTags(uid);
                        if (!tags.isEmpty()) {
                            FgThread.getHandler().post(new Runnable() { // from class: com.android.server.location.LocationManagerService$LocalService$$ExternalSyntheticLambda0
                                @Override // java.lang.Runnable
                                public final void run() {
                                    listener.onLocationPackageTagsChanged(uid, tags);
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    /* loaded from: classes.dex */
    private static final class SystemInjector implements Injector {
        private final AlarmHelper mAlarmHelper;
        private final SystemAppForegroundHelper mAppForegroundHelper;
        private final SystemAppOpsHelper mAppOpsHelper;
        private final Context mContext;
        private final SystemDeviceIdleHelper mDeviceIdleHelper;
        private final SystemDeviceStationaryHelper mDeviceStationaryHelper;
        private SystemEmergencyHelper mEmergencyCallHelper;
        private final SystemLocationPermissionsHelper mLocationPermissionsHelper;
        private final SystemLocationPowerSaveModeHelper mLocationPowerSaveModeHelper;
        private final LocationSettings mLocationSettings;
        private final LocationUsageLogger mLocationUsageLogger;
        private final SystemScreenInteractiveHelper mScreenInteractiveHelper;
        private final SystemSettingsHelper mSettingsHelper;
        private boolean mSystemReady;
        private final UserInfoHelper mUserInfoHelper;

        SystemInjector(Context context, UserInfoHelper userInfoHelper) {
            this.mContext = context;
            this.mUserInfoHelper = userInfoHelper;
            this.mLocationSettings = new LocationSettings(context);
            this.mAlarmHelper = new SystemAlarmHelper(context);
            SystemAppOpsHelper systemAppOpsHelper = new SystemAppOpsHelper(context);
            this.mAppOpsHelper = systemAppOpsHelper;
            this.mLocationPermissionsHelper = new SystemLocationPermissionsHelper(context, systemAppOpsHelper);
            this.mSettingsHelper = new SystemSettingsHelper(context);
            this.mAppForegroundHelper = new SystemAppForegroundHelper(context);
            this.mLocationPowerSaveModeHelper = new SystemLocationPowerSaveModeHelper(context);
            this.mScreenInteractiveHelper = new SystemScreenInteractiveHelper(context);
            this.mDeviceStationaryHelper = new SystemDeviceStationaryHelper();
            this.mDeviceIdleHelper = new SystemDeviceIdleHelper(context);
            this.mLocationUsageLogger = new LocationUsageLogger();
        }

        synchronized void onSystemReady() {
            this.mAppOpsHelper.onSystemReady();
            this.mLocationPermissionsHelper.onSystemReady();
            this.mSettingsHelper.onSystemReady();
            this.mAppForegroundHelper.onSystemReady();
            this.mLocationPowerSaveModeHelper.onSystemReady();
            this.mScreenInteractiveHelper.onSystemReady();
            this.mDeviceStationaryHelper.onSystemReady();
            this.mDeviceIdleHelper.onSystemReady();
            SystemEmergencyHelper systemEmergencyHelper = this.mEmergencyCallHelper;
            if (systemEmergencyHelper != null) {
                systemEmergencyHelper.onSystemReady();
            }
            this.mSystemReady = true;
        }

        @Override // com.android.server.location.injector.Injector
        public UserInfoHelper getUserInfoHelper() {
            return this.mUserInfoHelper;
        }

        @Override // com.android.server.location.injector.Injector
        public LocationSettings getLocationSettings() {
            return this.mLocationSettings;
        }

        @Override // com.android.server.location.injector.Injector
        public AlarmHelper getAlarmHelper() {
            return this.mAlarmHelper;
        }

        @Override // com.android.server.location.injector.Injector
        public AppOpsHelper getAppOpsHelper() {
            return this.mAppOpsHelper;
        }

        @Override // com.android.server.location.injector.Injector
        public LocationPermissionsHelper getLocationPermissionsHelper() {
            return this.mLocationPermissionsHelper;
        }

        @Override // com.android.server.location.injector.Injector
        public SettingsHelper getSettingsHelper() {
            return this.mSettingsHelper;
        }

        @Override // com.android.server.location.injector.Injector
        public AppForegroundHelper getAppForegroundHelper() {
            return this.mAppForegroundHelper;
        }

        @Override // com.android.server.location.injector.Injector
        public LocationPowerSaveModeHelper getLocationPowerSaveModeHelper() {
            return this.mLocationPowerSaveModeHelper;
        }

        @Override // com.android.server.location.injector.Injector
        public ScreenInteractiveHelper getScreenInteractiveHelper() {
            return this.mScreenInteractiveHelper;
        }

        @Override // com.android.server.location.injector.Injector
        public DeviceStationaryHelper getDeviceStationaryHelper() {
            return this.mDeviceStationaryHelper;
        }

        @Override // com.android.server.location.injector.Injector
        public DeviceIdleHelper getDeviceIdleHelper() {
            return this.mDeviceIdleHelper;
        }

        @Override // com.android.server.location.injector.Injector
        public synchronized EmergencyHelper getEmergencyHelper() {
            if (this.mEmergencyCallHelper == null) {
                SystemEmergencyHelper systemEmergencyHelper = new SystemEmergencyHelper(this.mContext);
                this.mEmergencyCallHelper = systemEmergencyHelper;
                if (this.mSystemReady) {
                    systemEmergencyHelper.onSystemReady();
                }
            }
            return this.mEmergencyCallHelper;
        }

        @Override // com.android.server.location.injector.Injector
        public LocationUsageLogger getLocationUsageLogger() {
            return this.mLocationUsageLogger;
        }
    }

    private static void initMtkLocationManagerService(Context context, AppOpsHelper appOpsHelper) {
        Constructor constructor;
        try {
            sMtkLocationManagerServiceClass = Class.forName("com.mediatek.location.MtkLocationExt$LocationManagerService");
            if (D) {
                Log.d(TAG, "class = " + sMtkLocationManagerServiceClass);
            }
            Class<?> cls = sMtkLocationManagerServiceClass;
            if (cls != null && (constructor = cls.getConstructor(Context.class, Handler.class)) != null) {
                sMtkLocationManagerService = constructor.newInstance(context, null);
            }
            sCtaSupported = checkCtaSuport();
            sAppOpsHelper = (SystemAppOpsHelper) appOpsHelper;
            Log.d(TAG, "sMtkLocationManagerService = " + sMtkLocationManagerService + " mCtaSupported = " + sCtaSupported);
        } catch (Exception e) {
            Log.w(TAG, "Failed to init sMtkLocationManagerService!");
        }
    }

    public static boolean isCtaSupported() {
        return sCtaSupported;
    }

    private static boolean checkCtaSuport() {
        Boolean ret = false;
        try {
            if (sMtkLocationManagerService != null) {
                Method m = sMtkLocationManagerServiceClass.getMethod("isCtaFeatureSupport", new Class[0]);
                ret = (Boolean) m.invoke(sMtkLocationManagerService, new Object[0]);
                Log.d(TAG, "checkCtaSupport = " + ret);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to call isCtaFeatureSupport!");
        }
        return ret.booleanValue();
    }

    public static void mtkPrintCtaLog(int callingPid, int callingUid, String functionName, String actionType, String parameter) {
        try {
            if (isCtaSupported()) {
                Method m = sMtkLocationManagerServiceClass.getMethod("printCtaLog", Integer.TYPE, Integer.TYPE, String.class, String.class, String.class);
                m.invoke(sMtkLocationManagerService, Integer.valueOf(callingPid), Integer.valueOf(callingUid), functionName, actionType, parameter);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to call printCtaLog!");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void mtkShowNlpNotInstalledToast(String provider) {
        try {
            if (sMtkLocationManagerService != null) {
                Method m = sMtkLocationManagerServiceClass.getMethod("showNlpNotInstalledToast", String.class);
                m.invoke(sMtkLocationManagerService, provider);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to call showNlpNotInstalledToast ", e);
        }
    }

    public static String mtkBuildLocationInfo(Location loc) {
        StringBuilder s = new StringBuilder();
        s.append("Location[");
        s.append(loc.getProvider());
        if (loc.hasAccuracy()) {
            s.append(String.format(" hAcc=%.0f", Float.valueOf(loc.getAccuracy())));
        } else {
            s.append(" hAcc=???");
        }
        if (loc.getTime() == 0) {
            s.append(" t=?!?");
        }
        if (loc.getElapsedRealtimeNanos() == 0) {
            s.append(" et=?!?");
        } else {
            s.append(" et=");
            TimeUtils.formatDuration(loc.getElapsedRealtimeNanos() / 1000000, s);
        }
        if (loc.hasElapsedRealtimeUncertaintyNanos()) {
            s.append(" etAcc=");
            TimeUtils.formatDuration((long) (loc.getElapsedRealtimeUncertaintyNanos() / 1000000.0d), s);
        }
        if (loc.hasSpeed()) {
            s.append(" vel=").append(loc.getSpeed());
        }
        if (loc.hasBearing()) {
            s.append(" bear=").append(loc.getBearing());
        }
        if (loc.hasVerticalAccuracy()) {
            s.append(String.format(" vAcc=%.0f", Float.valueOf(loc.getVerticalAccuracyMeters())));
        } else {
            s.append(" vAcc=???");
        }
        if (loc.hasSpeedAccuracy()) {
            s.append(String.format(" sAcc=%.0f", Float.valueOf(loc.getSpeedAccuracyMetersPerSecond())));
        } else {
            s.append(" sAcc=???");
        }
        if (loc.hasBearingAccuracy()) {
            s.append(String.format(" bAcc=%.0f", Float.valueOf(loc.getBearingAccuracyDegrees())));
        } else {
            s.append(" bAcc=???");
        }
        if (loc.isFromMockProvider()) {
            s.append(" mock");
        }
        s.append(']');
        return s.toString();
    }

    public static boolean mtkCheckCoarseLocationAccess(CallerIdentity identity) {
        SystemAppOpsHelper systemAppOpsHelper;
        if (isCtaSupported() && (systemAppOpsHelper = sAppOpsHelper) != null && systemAppOpsHelper.mtkCheckCoarseLocationAccess(identity)) {
            Log.d(TAG, "mtkCheckCoarseLocationAccess COARSE permission was granted for:" + identity.getPackageName());
            return true;
        }
        Log.d(TAG, "checkLocationAccess fail, no permission granted for package:" + identity.getPackageName());
        return false;
    }

    public static boolean mtkNoteCoarseLocationAccess(String provider, CallerIdentity identity) {
        if (isCtaSupported() && sAppOpsHelper != null && "network".equals(provider) && sAppOpsHelper.mtkNoteCoarseLocationAccess(identity)) {
            Log.d(TAG, "mtkNoteCoarseLocationAccess COARSE permission was granted for:" + identity.getPackageName() + " provider: " + provider);
            return true;
        }
        Log.d(TAG, "noteLocationAccess fail, no permission granted for package:" + identity.getPackageName() + " provider: " + provider);
        return false;
    }

    public List<String> getCurrentActive() {
        return null;
    }
}
