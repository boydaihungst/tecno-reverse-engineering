package com.android.server.location.gnss;

import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.location.GnssCapabilities;
import android.location.GnssStatus;
import android.location.INetInitiatedListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.location.LocationResult;
import android.location.provider.ProviderProperties;
import android.location.provider.ProviderRequest;
import android.location.util.identity.CallerIdentity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.TimeUtils;
import com.android.internal.app.IBatteryStats;
import com.android.internal.location.GpsNetInitiatedHandler;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.FgThread;
import com.android.server.UiModeManagerService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.location.gnss.GnssConfiguration;
import com.android.server.location.gnss.GnssNetworkConnectivityHandler;
import com.android.server.location.gnss.GnssSatelliteBlocklistHelper;
import com.android.server.location.gnss.NtpTimeHelper;
import com.android.server.location.gnss.hal.GnssNative;
import com.android.server.location.injector.Injector;
import com.android.server.location.provider.AbstractLocationProvider;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
/* loaded from: classes.dex */
public class GnssLocationProvider extends AbstractLocationProvider implements NtpTimeHelper.InjectNtpTimeCallback, GnssSatelliteBlocklistHelper.GnssSatelliteBlocklistCallback, GnssNative.BaseCallbacks, GnssNative.LocationCallbacks, GnssNative.SvStatusCallbacks, GnssNative.AGpsCallbacks, GnssNative.PsdsCallbacks, GnssNative.NotificationCallbacks, GnssNative.LocationRequestCallbacks, GnssNative.TimeCallbacks {
    private static final int AGPS_SUPL_MODE_MSA = 2;
    private static final int AGPS_SUPL_MODE_MSB = 1;
    private static final boolean DEBUG;
    private static final long DOWNLOAD_PSDS_DATA_TIMEOUT_MS = 60000;
    private static final int EMERGENCY_LOCATION_UPDATE_DURATION_MULTIPLIER = 3;
    private static final int GPS_DELETE_EPO = 16384;
    private static final int GPS_DELETE_HOT_STILL = 8192;
    private static final int GPS_POLLING_THRESHOLD_INTERVAL = 10000;
    private static final boolean IS_USER_BUILD;
    private static final int LAST_LOCATION_EXPIRED_TIMEOUT = 600000;
    private static final long LOCATION_OFF_DELAY_THRESHOLD_ERROR_MILLIS = 15000;
    private static final long LOCATION_OFF_DELAY_THRESHOLD_WARN_MILLIS = 2000;
    private static final long LOCATION_UPDATE_DURATION_MILLIS = 10000;
    private static final long LOCATION_UPDATE_MIN_TIME_INTERVAL_MILLIS = 1000;
    private static final long MAX_BATCH_LENGTH_MS = 86400000;
    private static final long MAX_BATCH_TIMESTAMP_DELTA_MS = 500;
    private static final long MAX_RETRY_INTERVAL = 14400000;
    private static final int MIN_BATCH_INTERVAL_MS = 1000;
    private static final int NO_FIX_TIMEOUT = 60000;
    private static final ProviderProperties PROPERTIES;
    private static final long RETRY_INTERVAL = 300000;
    private static final String TAG = "GnssLocationProvider";
    private static final int TCP_MAX_PORT = 65535;
    private static final int TCP_MIN_PORT = 0;
    private static final String TEL_DBG_PROP = "persist.vendor.log.tel_dbg";
    public static final boolean USERDEBUG_TEL_PROP;
    private static final boolean VERBOSE;
    private static final long WAKELOCK_TIMEOUT_MILLIS = 30000;
    private final AlarmManager mAlarmManager;
    private final AppOpsManager mAppOps;
    private boolean mAutomotiveSuspend;
    private AlarmManager.OnAlarmListener mBatchingAlarm;
    private boolean mBatchingEnabled;
    private boolean mBatchingStarted;
    private final IBatteryStats mBatteryStats;
    private String mC2KServerHost;
    private int mC2KServerPort;
    private final WorkSource mClientSource;
    private final Context mContext;
    private final PowerManager.WakeLock mDownloadPsdsWakeLock;
    private int mFixInterval;
    private long mFixRequestTime;
    private final ArrayList<Runnable> mFlushListeners;
    private final GnssConfiguration mGnssConfiguration;
    private final GnssMetrics mGnssMetrics;
    private final GnssNative mGnssNative;
    private final GnssSatelliteBlocklistHelper mGnssSatelliteBlocklistHelper;
    private GnssVisibilityControl mGnssVisibilityControl;
    private boolean mGpsEnabled;
    private final Handler mHandler;
    private long mLastFixTime;
    private GnssPositionMode mLastPositionMode;
    private final LocationExtras mLocationExtras;
    private final Object mLock;
    private Object mMtkGnssProvider;
    private Class<?> mMtkGnssProviderClass;
    private final GpsNetInitiatedHandler mNIHandler;
    private final INetInitiatedListener mNetInitiatedListener;
    private final GnssNetworkConnectivityHandler mNetworkConnectivityHandler;
    private final NtpTimeHelper mNtpTimeHelper;
    private final Set<Integer> mPendingDownloadPsdsTypes;
    private int mPositionMode;
    private ProviderRequest mProviderRequest;
    private final ExponentialBackOff mPsdsBackOff;
    private final Object mPsdsPeriodicDownloadToken;
    private boolean mShutdown;
    private boolean mStarted;
    private long mStartedChangedElapsedRealtime;
    private boolean mSuplEsEnabled;
    private String mSuplServerHost;
    private int mSuplServerPort;
    private boolean mSupportsPsds;
    private int mTimeToFirstFix;
    private final AlarmManager.OnAlarmListener mTimeoutListener;
    private final PowerManager.WakeLock mWakeLock;
    private final AlarmManager.OnAlarmListener mWakeupListener;

    static {
        boolean z = false;
        boolean z2 = "user".equals(Build.TYPE) || "userdebug".equals(Build.TYPE);
        IS_USER_BUILD = z2;
        boolean z3 = SystemProperties.getInt(TEL_DBG_PROP, 0) == 1 && "userdebug".equals(Build.TYPE);
        USERDEBUG_TEL_PROP = z3;
        DEBUG = !z2 || Log.isLoggable(TAG, 3) || z3;
        if (!z2 || Log.isLoggable(TAG, 2) || z3) {
            z = true;
        }
        VERBOSE = z;
        PROPERTIES = new ProviderProperties.Builder().setHasSatelliteRequirement(true).setHasAltitudeSupport(true).setHasSpeedSupport(true).setHasBearingSupport(true).setPowerUsage(3).setAccuracy(1).build();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class LocationExtras {
        private final Bundle mBundle = new Bundle();
        private int mMaxCn0;
        private int mMeanCn0;
        private int mSvCount;

        LocationExtras() {
        }

        public void set(int svCount, int meanCn0, int maxCn0) {
            synchronized (this) {
                this.mSvCount = svCount;
                this.mMeanCn0 = meanCn0;
                this.mMaxCn0 = maxCn0;
            }
            setBundle(this.mBundle);
        }

        public void reset() {
            set(0, 0, 0);
        }

        public void setBundle(Bundle extras) {
            if (extras != null) {
                synchronized (this) {
                    extras.putInt("satellites", this.mSvCount);
                    extras.putInt("meanCn0", this.mMeanCn0);
                    extras.putInt("maxCn0", this.mMaxCn0);
                }
            }
        }

        public Bundle getBundle() {
            Bundle bundle;
            synchronized (this) {
                bundle = new Bundle(this.mBundle);
            }
            return bundle;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onUpdateSatelliteBlocklist$0$com-android-server-location-gnss-GnssLocationProvider  reason: not valid java name */
    public /* synthetic */ void m4367x8bc8bbd8(int[] constellations, int[] svids) {
        this.mGnssConfiguration.setSatelliteBlocklist(constellations, svids);
    }

    @Override // com.android.server.location.gnss.GnssSatelliteBlocklistHelper.GnssSatelliteBlocklistCallback
    public void onUpdateSatelliteBlocklist(final int[] constellations, final int[] svids) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda20
            @Override // java.lang.Runnable
            public final void run() {
                GnssLocationProvider.this.m4367x8bc8bbd8(constellations, svids);
            }
        });
        this.mGnssMetrics.resetConstellationTypes();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void subscriptionOrCarrierConfigChanged() {
        boolean z = DEBUG;
        if (z) {
            Log.d(TAG, "received SIM related action: ");
        }
        TelephonyManager phone = (TelephonyManager) this.mContext.getSystemService("phone");
        CarrierConfigManager configManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        int ddSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (SubscriptionManager.isValidSubscriptionId(ddSubId)) {
            phone = phone.createForSubscriptionId(ddSubId);
        }
        String mccMnc = phone.getSimOperator();
        boolean isKeepLppProfile = false;
        if (!TextUtils.isEmpty(mccMnc)) {
            if (z) {
                Log.d(TAG, "SIM MCC/MNC is available: " + mccMnc);
            }
            if (configManager != null) {
                PersistableBundle b = SubscriptionManager.isValidSubscriptionId(ddSubId) ? configManager.getConfigForSubId(ddSubId) : null;
                if (b != null) {
                    isKeepLppProfile = b.getBoolean("gps.persist_lpp_mode_bool");
                }
            }
            if (!isKeepLppProfile) {
                SystemProperties.set("persist.sys.gps.lpp", "");
            } else {
                this.mGnssConfiguration.loadPropertiesFromCarrierConfig();
                String lpp_profile = this.mGnssConfiguration.getLppProfile();
                if (lpp_profile != null) {
                    SystemProperties.set("persist.sys.gps.lpp", lpp_profile);
                }
            }
            reloadGpsProperties();
            return;
        }
        if (z) {
            Log.d(TAG, "SIM MCC/MNC is still not available");
        }
        this.mGnssConfiguration.reloadGpsProperties();
    }

    private void reloadGpsProperties() {
        this.mGnssConfiguration.reloadGpsProperties();
        setSuplHostPort();
        this.mC2KServerHost = this.mGnssConfiguration.getC2KHost();
        this.mC2KServerPort = this.mGnssConfiguration.getC2KPort(0);
        this.mNIHandler.setEmergencyExtensionSeconds(this.mGnssConfiguration.getEsExtensionSec());
        boolean z = this.mGnssConfiguration.getSuplEs(0) == 1;
        this.mSuplEsEnabled = z;
        this.mNIHandler.setSuplEsEnabled(z);
        GnssVisibilityControl gnssVisibilityControl = this.mGnssVisibilityControl;
        if (gnssVisibilityControl != null) {
            gnssVisibilityControl.onConfigurationUpdated(this.mGnssConfiguration);
        }
    }

    public GnssLocationProvider(Context context, Injector injector, GnssNative gnssNative, GnssMetrics gnssMetrics) {
        super(FgThread.getExecutor(), CallerIdentity.fromContext(context), PROPERTIES, Collections.emptySet());
        this.mLock = new Object();
        this.mPsdsBackOff = new ExponentialBackOff(300000L, 14400000L);
        this.mFixInterval = 1000;
        this.mFixRequestTime = 0L;
        this.mTimeToFirstFix = 0;
        this.mClientSource = new WorkSource();
        this.mPsdsPeriodicDownloadToken = new Object();
        HashSet hashSet = new HashSet();
        this.mPendingDownloadPsdsTypes = hashSet;
        this.mSuplServerPort = 0;
        this.mSuplEsEnabled = false;
        this.mLocationExtras = new LocationExtras();
        this.mWakeupListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda21
            @Override // android.app.AlarmManager.OnAlarmListener
            public final void onAlarm() {
                GnssLocationProvider.this.startNavigating();
            }
        };
        this.mTimeoutListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda22
            @Override // android.app.AlarmManager.OnAlarmListener
            public final void onAlarm() {
                GnssLocationProvider.this.hibernate();
            }
        };
        this.mFlushListeners = new ArrayList<>(0);
        INetInitiatedListener.Stub stub = new INetInitiatedListener.Stub() { // from class: com.android.server.location.gnss.GnssLocationProvider.4
            public boolean sendNiResponse(int notificationId, int userResponse) {
                if (GnssLocationProvider.DEBUG) {
                    Log.d(GnssLocationProvider.TAG, "sendNiResponse, notifId: " + notificationId + ", response: " + userResponse);
                }
                GnssLocationProvider.this.mGnssNative.sendNiResponse(notificationId, userResponse);
                FrameworkStatsLog.write(124, 2, notificationId, 0, false, false, false, 0, 0, (String) null, (String) null, 0, 0, GnssLocationProvider.this.mSuplEsEnabled, GnssLocationProvider.this.isGpsEnabled(), userResponse);
                return true;
            }
        };
        this.mNetInitiatedListener = stub;
        this.mMtkGnssProviderClass = null;
        this.mMtkGnssProvider = null;
        this.mContext = context;
        this.mGnssNative = gnssNative;
        this.mGnssMetrics = gnssMetrics;
        PowerManager powerManager = (PowerManager) Objects.requireNonNull((PowerManager) context.getSystemService(PowerManager.class));
        PowerManager.WakeLock newWakeLock = powerManager.newWakeLock(1, "*location*:GnssLocationProvider");
        this.mWakeLock = newWakeLock;
        newWakeLock.setReferenceCounted(true);
        PowerManager.WakeLock newWakeLock2 = powerManager.newWakeLock(1, "*location*:PsdsDownload");
        this.mDownloadPsdsWakeLock = newWakeLock2;
        newWakeLock2.setReferenceCounted(true);
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mAppOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        this.mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
        Handler handler = FgThread.getHandler();
        this.mHandler = handler;
        this.mGnssConfiguration = gnssNative.getConfiguration();
        GpsNetInitiatedHandler gpsNetInitiatedHandler = new GpsNetInitiatedHandler(context, stub, this.mSuplEsEnabled);
        this.mNIHandler = gpsNetInitiatedHandler;
        hashSet.add(1);
        this.mNetworkConnectivityHandler = new GnssNetworkConnectivityHandler(context, new GnssNetworkConnectivityHandler.GnssNetworkListener() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda23
            @Override // com.android.server.location.gnss.GnssNetworkConnectivityHandler.GnssNetworkListener
            public final void onNetworkAvailable() {
                GnssLocationProvider.this.onNetworkAvailable();
            }
        }, handler.getLooper(), gpsNetInitiatedHandler);
        this.mNtpTimeHelper = new NtpTimeHelper(context, handler.getLooper(), this);
        this.mGnssSatelliteBlocklistHelper = new GnssSatelliteBlocklistHelper(context, handler.getLooper(), this);
        setAllowed(true);
        initMtkGnssLocProvider();
        gnssNative.addBaseCallbacks(this);
        gnssNative.addLocationCallbacks(this);
        gnssNative.addSvStatusCallbacks(this);
        gnssNative.setAGpsCallbacks(this);
        gnssNative.setPsdsCallbacks(this);
        gnssNative.setNotificationCallbacks(this);
        gnssNative.setLocationRequestCallbacks(this);
        gnssNative.setTimeCallbacks(this);
    }

    public synchronized void onSystemReady() {
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() { // from class: com.android.server.location.gnss.GnssLocationProvider.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                if (getSendingUserId() == -1) {
                    GnssLocationProvider.this.mShutdown = true;
                    GnssLocationProvider.this.updateEnabled();
                }
            }
        }, UserHandle.ALL, new IntentFilter("android.intent.action.ACTION_SHUTDOWN"), null, this.mHandler);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("location_mode"), true, new ContentObserver(this.mHandler) { // from class: com.android.server.location.gnss.GnssLocationProvider.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                GnssLocationProvider.this.updateEnabled();
            }
        }, -1);
        this.mHandler.post(new Runnable() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda13
            @Override // java.lang.Runnable
            public final void run() {
                GnssLocationProvider.this.handleInitialize();
            }
        });
        Handler handler = this.mHandler;
        final GnssSatelliteBlocklistHelper gnssSatelliteBlocklistHelper = this.mGnssSatelliteBlocklistHelper;
        Objects.requireNonNull(gnssSatelliteBlocklistHelper);
        handler.post(new Runnable() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                GnssSatelliteBlocklistHelper.this.updateSatelliteBlocklist();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleInitialize() {
        if (this.mGnssNative.isGnssVisibilityControlSupported()) {
            this.mGnssVisibilityControl = new GnssVisibilityControl(this.mContext, this.mHandler.getLooper(), this.mNIHandler);
        }
        reloadGpsProperties();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        intentFilter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.location.gnss.GnssLocationProvider.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (GnssLocationProvider.DEBUG) {
                    Log.d(GnssLocationProvider.TAG, "receive broadcast intent, action: " + action);
                }
                if (action == null) {
                    return;
                }
                char c = 65535;
                switch (action.hashCode()) {
                    case -1138588223:
                        if (action.equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                            c = 0;
                            break;
                        }
                        break;
                    case -25388475:
                        if (action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")) {
                            c = 1;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                    case 1:
                        GnssLocationProvider.this.subscriptionOrCarrierConfigChanged();
                        return;
                    default:
                        return;
                }
            }
        }, intentFilter, null, this.mHandler);
        this.mNetworkConnectivityHandler.registerNetworkCallbacks();
        LocationManager locationManager = (LocationManager) Objects.requireNonNull((LocationManager) this.mContext.getSystemService(LocationManager.class));
        if (locationManager.getAllProviders().contains("network")) {
            locationManager.requestLocationUpdates("network", new LocationRequest.Builder((long) JobStatus.NO_LATEST_RUNTIME).setMinUpdateIntervalMillis(0L).setHiddenFromAppOps(true).build(), ConcurrentUtils.DIRECT_EXECUTOR, new LocationListener() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda0
                @Override // android.location.LocationListener
                public final void onLocationChanged(Location location) {
                    GnssLocationProvider.this.injectLocation(location);
                }
            });
        }
        updateEnabled();
    }

    @Override // com.android.server.location.gnss.NtpTimeHelper.InjectNtpTimeCallback
    public void injectTime(long time, long timeReference, int uncertainty) {
        this.mGnssNative.injectTime(time, timeReference, uncertainty);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onNetworkAvailable() {
        this.mNtpTimeHelper.onNetworkAvailable();
        if (this.mSupportsPsds) {
            synchronized (this.mLock) {
                for (Integer num : this.mPendingDownloadPsdsTypes) {
                    final int psdsType = num.intValue();
                    postWithWakeLockHeld(new Runnable() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda2
                        @Override // java.lang.Runnable
                        public final void run() {
                            GnssLocationProvider.this.m4362xd576fefb(psdsType);
                        }
                    });
                }
                this.mPendingDownloadPsdsTypes.clear();
            }
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: handleRequestLocation */
    public void m4365x159bc757(boolean independentFromGnss, boolean isUserEmergency) {
        String provider;
        LocationListener locationListener;
        if (isRequestLocationRateLimited()) {
            if (DEBUG) {
                Log.d(TAG, "RequestLocation is denied due to too frequent requests.");
                return;
            }
            return;
        }
        ContentResolver resolver = this.mContext.getContentResolver();
        long durationMillis = Settings.Global.getLong(resolver, "gnss_hal_location_request_duration_millis", 10000L);
        if (durationMillis == 0) {
            Log.i(TAG, "GNSS HAL location request is disabled by Settings.");
            return;
        }
        LocationManager locationManager = (LocationManager) this.mContext.getSystemService("location");
        LocationRequest.Builder locationRequest = new LocationRequest.Builder(1000L).setMaxUpdates(1);
        if (independentFromGnss) {
            provider = "network";
            locationListener = new LocationListener() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda16
                @Override // android.location.LocationListener
                public final void onLocationChanged(Location location) {
                    GnssLocationProvider.lambda$handleRequestLocation$2(location);
                }
            };
            locationRequest.setQuality(104);
            mtkInjectLastKnownLocation();
        } else {
            provider = "fused";
            locationListener = new LocationListener() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda17
                @Override // android.location.LocationListener
                public final void onLocationChanged(Location location) {
                    GnssLocationProvider.this.injectBestLocation(location);
                }
            };
            locationRequest.setQuality(100);
        }
        if (this.mNIHandler.getInEmergency()) {
            GnssConfiguration.HalInterfaceVersion halVersion = this.mGnssConfiguration.getHalInterfaceVersion();
            if (isUserEmergency || halVersion.mMajor < 2) {
                locationRequest.setLocationSettingsIgnored(true);
                durationMillis *= 3;
            }
        }
        locationRequest.setDurationMillis(durationMillis);
        Log.i(TAG, String.format("GNSS HAL Requesting location updates from %s provider for %d millis.", provider, Long.valueOf(durationMillis)));
        if (locationManager.getProvider(provider) != null) {
            locationManager.requestLocationUpdates(provider, locationRequest.build(), ConcurrentUtils.DIRECT_EXECUTOR, locationListener);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$handleRequestLocation$2(Location location) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void injectBestLocation(Location location) {
        if (DEBUG) {
            Log.d(TAG, "injectBestLocation: " + location);
        }
        if (location.isMock()) {
            return;
        }
        this.mGnssNative.injectBestLocation(location);
    }

    private boolean isRequestLocationRateLimited() {
        return false;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: handleDownloadPsdsData */
    public void m4366x4b109e71(final int psdsType) {
        if (!this.mSupportsPsds) {
            Log.d(TAG, "handleDownloadPsdsData() called when PSDS not supported");
        } else if (!this.mNetworkConnectivityHandler.isDataNetworkConnected()) {
            synchronized (this.mLock) {
                this.mPendingDownloadPsdsTypes.add(Integer.valueOf(psdsType));
            }
        } else {
            synchronized (this.mLock) {
                this.mDownloadPsdsWakeLock.acquire(60000L);
            }
            Log.i(TAG, "WakeLock acquired by handleDownloadPsdsData()");
            Executors.newSingleThreadExecutor().execute(new Runnable() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    GnssLocationProvider.this.m4359xde5c8412(psdsType);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleDownloadPsdsData$6$com-android-server-location-gnss-GnssLocationProvider  reason: not valid java name */
    public /* synthetic */ void m4359xde5c8412(final int psdsType) {
        long backoffMillis;
        GnssPsdsDownloader psdsDownloader = new GnssPsdsDownloader(this.mGnssConfiguration.getProperties());
        final byte[] data = psdsDownloader.downloadPsdsData(psdsType);
        if (data != null) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda10
                @Override // java.lang.Runnable
                public final void run() {
                    GnssLocationProvider.this.m4356x625f844f(psdsType, data);
                }
            });
            PackageManager pm = this.mContext.getPackageManager();
            if (pm != null && pm.hasSystemFeature("android.hardware.type.watch") && psdsType == 1 && this.mGnssConfiguration.isPsdsPeriodicDownloadEnabled()) {
                if (DEBUG) {
                    Log.d(TAG, "scheduling next long term Psds download");
                }
                this.mHandler.removeCallbacksAndMessages(this.mPsdsPeriodicDownloadToken);
                this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda11
                    @Override // java.lang.Runnable
                    public final void run() {
                        GnssLocationProvider.this.m4357x8bb3d990(psdsType);
                    }
                }, this.mPsdsPeriodicDownloadToken, 86400000L);
            }
        } else {
            synchronized (this.mLock) {
                backoffMillis = this.mPsdsBackOff.nextBackoffMillis();
            }
            this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda12
                @Override // java.lang.Runnable
                public final void run() {
                    GnssLocationProvider.this.m4358xb5082ed1(psdsType);
                }
            }, backoffMillis);
        }
        synchronized (this.mLock) {
            if (this.mDownloadPsdsWakeLock.isHeld()) {
                this.mDownloadPsdsWakeLock.release();
                if (DEBUG) {
                    Log.d(TAG, "WakeLock released by handleDownloadPsdsData()");
                }
            } else {
                Log.e(TAG, "WakeLock expired before release in handleDownloadPsdsData()");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleDownloadPsdsData$3$com-android-server-location-gnss-GnssLocationProvider  reason: not valid java name */
    public /* synthetic */ void m4356x625f844f(int psdsType, byte[] data) {
        FrameworkStatsLog.write((int) FrameworkStatsLog.GNSS_PSDS_DOWNLOAD_REPORTED, psdsType);
        if (DEBUG) {
            Log.d(TAG, "calling native_inject_psds_data");
        }
        this.mGnssNative.injectPsdsData(data, data.length, psdsType);
        synchronized (this.mLock) {
            this.mPsdsBackOff.reset();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void injectLocation(Location location) {
        if (!location.isMock()) {
            if (DEBUG) {
                Log.d(TAG, "injectLocation: " + location);
            }
            this.mGnssNative.injectLocation(location);
        }
    }

    private void setSuplHostPort() {
        this.mSuplServerHost = this.mGnssConfiguration.getSuplHost();
        int suplPort = this.mGnssConfiguration.getSuplPort(0);
        this.mSuplServerPort = suplPort;
        String str = this.mSuplServerHost;
        if (str != null && suplPort > 0 && suplPort <= 65535) {
            this.mGnssNative.setAgpsServer(1, str, suplPort);
        }
    }

    private int getSuplMode(boolean agpsEnabled) {
        int suplMode;
        if (!agpsEnabled || (suplMode = this.mGnssConfiguration.getSuplMode(0)) == 0 || !this.mGnssNative.getCapabilities().hasMsb() || (suplMode & 1) == 0) {
            return 0;
        }
        return 1;
    }

    private void setGpsEnabled(boolean enabled) {
        synchronized (this.mLock) {
            this.mGpsEnabled = enabled;
        }
    }

    public void setAutomotiveGnssSuspended(boolean suspended) {
        synchronized (this.mLock) {
            this.mAutomotiveSuspend = suspended;
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda24
            @Override // java.lang.Runnable
            public final void run() {
                GnssLocationProvider.this.updateEnabled();
            }
        });
    }

    public boolean isAutomotiveGnssSuspended() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mAutomotiveSuspend && !this.mGpsEnabled;
        }
        return z;
    }

    private void handleEnable() {
        if (DEBUG) {
            Log.d(TAG, "handleEnable");
        }
        boolean inited = this.mGnssNative.init();
        boolean z = false;
        if (inited) {
            setGpsEnabled(true);
            this.mSupportsPsds = this.mGnssNative.isPsdsSupported();
            String str = this.mSuplServerHost;
            if (str != null) {
                this.mGnssNative.setAgpsServer(1, str, this.mSuplServerPort);
            }
            String str2 = this.mC2KServerHost;
            if (str2 != null) {
                this.mGnssNative.setAgpsServer(2, str2, this.mC2KServerPort);
            }
            if (this.mGnssNative.initBatching() && this.mGnssNative.getBatchSize() > 1) {
                z = true;
            }
            this.mBatchingEnabled = z;
            GnssVisibilityControl gnssVisibilityControl = this.mGnssVisibilityControl;
            if (gnssVisibilityControl != null) {
                gnssVisibilityControl.onGpsEnabledChanged(true);
                return;
            }
            return;
        }
        setGpsEnabled(false);
        Log.w(TAG, "Failed to enable location provider");
    }

    private void handleDisable() {
        if (DEBUG) {
            Log.d(TAG, "handleDisable");
        }
        setGpsEnabled(false);
        updateClientUids(new WorkSource());
        stopNavigating();
        stopBatching();
        GnssVisibilityControl gnssVisibilityControl = this.mGnssVisibilityControl;
        if (gnssVisibilityControl != null) {
            gnssVisibilityControl.onGpsEnabledChanged(false);
        }
        this.mGnssNative.cleanupBatching();
        this.mGnssNative.cleanup();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateEnabled() {
        boolean enabled;
        boolean enabled2 = ((LocationManager) this.mContext.getSystemService(LocationManager.class)).isLocationEnabledForUser(UserHandle.CURRENT);
        ProviderRequest providerRequest = this.mProviderRequest;
        boolean enabled3 = enabled2 | (providerRequest != null && providerRequest.isActive() && this.mProviderRequest.isBypass());
        synchronized (this.mLock) {
            enabled = enabled3 & (this.mAutomotiveSuspend ? false : true);
        }
        boolean enabled4 = enabled & (!this.mShutdown);
        if (enabled4 == isGpsEnabled()) {
            return;
        }
        if (enabled4) {
            handleEnable();
        } else {
            handleDisable();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isGpsEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mGpsEnabled;
        }
        return z;
    }

    public int getBatchSize() {
        return this.mGnssNative.getBatchSize();
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider
    protected void onFlush(Runnable listener) {
        boolean added = false;
        synchronized (this.mLock) {
            if (this.mBatchingEnabled) {
                added = this.mFlushListeners.add(listener);
            }
        }
        if (!added) {
            listener.run();
        } else {
            this.mGnssNative.flushBatch();
        }
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider
    public void onSetRequest(ProviderRequest request) {
        this.mProviderRequest = request;
        updateEnabled();
        updateRequirements();
    }

    private void updateRequirements() {
        ProviderRequest providerRequest = this.mProviderRequest;
        if (providerRequest == null || providerRequest.getWorkSource() == null) {
            return;
        }
        Log.d(TAG, "setRequest " + this.mProviderRequest);
        if (this.mProviderRequest.isActive() && isGpsEnabled()) {
            updateClientUids(this.mProviderRequest.getWorkSource());
            if (this.mProviderRequest.getIntervalMillis() <= 2147483647L) {
                this.mFixInterval = (int) this.mProviderRequest.getIntervalMillis();
            } else {
                Log.w(TAG, "interval overflow: " + this.mProviderRequest.getIntervalMillis());
                this.mFixInterval = Integer.MAX_VALUE;
            }
            int batchIntervalMs = Math.max(this.mFixInterval, 1000);
            long batchLengthMs = Math.min(this.mProviderRequest.getMaxUpdateDelayMillis(), 86400000L);
            if (this.mBatchingEnabled && batchLengthMs / 2 >= batchIntervalMs) {
                stopNavigating();
                this.mFixInterval = batchIntervalMs;
                startBatching(batchLengthMs);
                return;
            }
            stopBatching();
            if (this.mStarted && this.mGnssNative.getCapabilities().hasScheduling()) {
                if (!setPositionMode(this.mPositionMode, 0, this.mFixInterval, this.mProviderRequest.isLowPower())) {
                    Log.e(TAG, "set_position_mode failed in updateRequirements");
                    return;
                }
                return;
            } else if (!this.mStarted) {
                startNavigating();
                return;
            } else {
                this.mAlarmManager.cancel(this.mTimeoutListener);
                if (this.mFixInterval >= 60000) {
                    this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + 60000, TAG, this.mTimeoutListener, this.mHandler);
                    return;
                }
                return;
            }
        }
        updateClientUids(new WorkSource());
        stopNavigating();
        stopBatching();
    }

    private boolean setPositionMode(int mode, int recurrence, int minInterval, boolean lowPowerMode) {
        GnssPositionMode positionMode = new GnssPositionMode(mode, recurrence, minInterval, 0, 0, lowPowerMode);
        GnssPositionMode gnssPositionMode = this.mLastPositionMode;
        if (gnssPositionMode != null && gnssPositionMode.equals(positionMode)) {
            return true;
        }
        boolean result = this.mGnssNative.setPositionMode(mode, recurrence, minInterval, 0, 0, lowPowerMode);
        if (result) {
            this.mLastPositionMode = positionMode;
        } else {
            this.mLastPositionMode = null;
        }
        return result;
    }

    private void updateClientUids(WorkSource source) {
        if (source.equals(this.mClientSource)) {
            return;
        }
        try {
            this.mBatteryStats.noteGpsChanged(this.mClientSource, source);
        } catch (RemoteException e) {
            Log.w(TAG, "RemoteException", e);
        }
        List<WorkSource.WorkChain>[] diffs = WorkSource.diffChains(this.mClientSource, source);
        if (diffs != null) {
            List<WorkSource.WorkChain> newChains = diffs[0];
            List<WorkSource.WorkChain> goneChains = diffs[1];
            if (newChains != null) {
                for (WorkSource.WorkChain newChain : newChains) {
                    this.mAppOps.startOpNoThrow(2, newChain.getAttributionUid(), newChain.getAttributionTag());
                }
            }
            if (goneChains != null) {
                for (WorkSource.WorkChain goneChain : goneChains) {
                    this.mAppOps.finishOp(2, goneChain.getAttributionUid(), goneChain.getAttributionTag());
                }
            }
            this.mClientSource.transferWorkChains(source);
        }
        WorkSource[] changes = this.mClientSource.setReturningDiffs(source);
        if (changes != null) {
            WorkSource newWork = changes[0];
            WorkSource goneWork = changes[1];
            if (newWork != null) {
                for (int i = 0; i < newWork.size(); i++) {
                    this.mAppOps.startOpNoThrow(2, newWork.getUid(i), newWork.getPackageName(i));
                }
            }
            if (goneWork != null) {
                for (int i2 = 0; i2 < goneWork.size(); i2++) {
                    this.mAppOps.finishOp(2, goneWork.getUid(i2), goneWork.getPackageName(i2));
                }
            }
        }
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider
    public void onExtraCommand(int uid, int pid, String command, Bundle extras) {
        if ("delete_aiding_data".equals(command)) {
            deleteAidingData(extras);
        } else if ("force_time_injection".equals(command)) {
            requestUtcTime();
        } else if ("force_psds_injection".equals(command)) {
            if (this.mSupportsPsds) {
                postWithWakeLockHeld(new Runnable() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda26
                    @Override // java.lang.Runnable
                    public final void run() {
                        GnssLocationProvider.this.m4361xfced8961();
                    }
                });
            }
        } else if ("request_power_stats".equals(command)) {
            this.mGnssNative.requestPowerStats();
        } else {
            Log.w(TAG, "sendExtraCommand: unknown command " + command);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onExtraCommand$7$com-android-server-location-gnss-GnssLocationProvider  reason: not valid java name */
    public /* synthetic */ void m4361xfced8961() {
        m4366x4b109e71(1);
    }

    private void deleteAidingData(Bundle extras) {
        int flags;
        if (extras == null) {
            flags = 65535;
        } else {
            flags = extras.getBoolean("ephemeris") ? 0 | 1 : 0;
            if (extras.getBoolean("almanac")) {
                flags |= 2;
            }
            if (extras.getBoolean("position")) {
                flags |= 4;
            }
            if (extras.getBoolean("time")) {
                flags |= 8;
            }
            if (extras.getBoolean("iono")) {
                flags |= 16;
            }
            if (extras.getBoolean("utc")) {
                flags |= 32;
            }
            if (extras.getBoolean("health")) {
                flags |= 64;
            }
            if (extras.getBoolean("svdir")) {
                flags |= 128;
            }
            if (extras.getBoolean("svsteer")) {
                flags |= 256;
            }
            if (extras.getBoolean("sadata")) {
                flags |= 512;
            }
            if (extras.getBoolean("rti")) {
                flags |= 1024;
            }
            if (extras.getBoolean("celldb-info")) {
                flags |= 32768;
            }
            if (extras.getBoolean("all")) {
                flags |= 65535;
            }
        }
        int flags2 = mtkDeleteAidingData(extras, flags);
        if (flags2 != 0) {
            this.mGnssNative.deleteAidingData(flags2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startNavigating() {
        String mode;
        if (!this.mStarted) {
            Log.d(TAG, "startNavigating");
            this.mTimeToFirstFix = 0;
            this.mLastFixTime = 0L;
            boolean agpsEnabled = true;
            setStarted(true);
            this.mPositionMode = 0;
            if (Settings.Global.getInt(this.mContext.getContentResolver(), "assisted_gps_enabled", 1) == 0) {
                agpsEnabled = false;
            }
            int suplMode = getSuplMode(agpsEnabled);
            this.mPositionMode = suplMode;
            if (DEBUG) {
                switch (suplMode) {
                    case 0:
                        mode = "standalone";
                        break;
                    case 1:
                        mode = "MS_BASED";
                        break;
                    case 2:
                        mode = "MS_ASSISTED";
                        break;
                    default:
                        mode = UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
                        break;
                }
                Log.d(TAG, "setting position_mode to " + mode);
            }
            int interval = this.mGnssNative.getCapabilities().hasScheduling() ? this.mFixInterval : 1000;
            if (!setPositionMode(this.mPositionMode, 0, interval, this.mProviderRequest.isLowPower())) {
                setStarted(false);
                Log.e(TAG, "set_position_mode failed in startNavigating()");
            } else if (!this.mGnssNative.start()) {
                setStarted(false);
                Log.e(TAG, "native_start failed in startNavigating()");
            } else {
                this.mLocationExtras.reset();
                this.mFixRequestTime = SystemClock.elapsedRealtime();
                if (!this.mGnssNative.getCapabilities().hasScheduling() && this.mFixInterval >= 60000) {
                    this.mAlarmManager.set(2, 60000 + SystemClock.elapsedRealtime(), TAG, this.mTimeoutListener, this.mHandler);
                }
            }
        }
    }

    private void stopNavigating() {
        if (DEBUG) {
            Log.d(TAG, "stopNavigating");
        }
        if (this.mStarted) {
            setStarted(false);
            this.mGnssNative.stop();
            this.mLastFixTime = 0L;
            this.mLastPositionMode = null;
            this.mLocationExtras.reset();
        }
        this.mAlarmManager.cancel(this.mTimeoutListener);
        this.mAlarmManager.cancel(this.mWakeupListener);
    }

    private void startBatching(final long batchLengthMs) {
        long batchSize = batchLengthMs / this.mFixInterval;
        if (DEBUG) {
            Log.d(TAG, "startBatching " + this.mFixInterval + " " + batchLengthMs);
        }
        if (this.mGnssNative.startBatch(TimeUnit.MILLISECONDS.toNanos(this.mFixInterval), 0.0f, true)) {
            this.mBatchingStarted = true;
            if (batchSize < getBatchSize()) {
                this.mBatchingAlarm = new AlarmManager.OnAlarmListener() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda3
                    @Override // android.app.AlarmManager.OnAlarmListener
                    public final void onAlarm() {
                        GnssLocationProvider.this.m4369xf6da3c58(batchLengthMs);
                    }
                };
                this.mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + batchLengthMs, TAG, this.mBatchingAlarm, FgThread.getHandler());
                return;
            }
            return;
        }
        Log.e(TAG, "native_start_batch failed in startBatching()");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startBatching$8$com-android-server-location-gnss-GnssLocationProvider  reason: not valid java name */
    public /* synthetic */ void m4369xf6da3c58(long batchLengthMs) {
        boolean flush = false;
        synchronized (this.mLock) {
            if (this.mBatchingAlarm != null) {
                flush = true;
                this.mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + batchLengthMs, TAG, this.mBatchingAlarm, FgThread.getHandler());
            }
        }
        if (flush) {
            this.mGnssNative.flushBatch();
        }
    }

    private void stopBatching() {
        if (DEBUG) {
            Log.d(TAG, "stopBatching");
        }
        if (this.mBatchingStarted) {
            AlarmManager.OnAlarmListener onAlarmListener = this.mBatchingAlarm;
            if (onAlarmListener != null) {
                this.mAlarmManager.cancel(onAlarmListener);
                this.mBatchingAlarm = null;
            }
            this.mGnssNative.flushBatch();
            this.mGnssNative.stopBatch();
            this.mBatchingStarted = false;
        }
    }

    private void setStarted(boolean started) {
        if (this.mStarted != started) {
            this.mStarted = started;
            this.mStartedChangedElapsedRealtime = SystemClock.elapsedRealtime();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hibernate() {
        stopNavigating();
        long now = SystemClock.elapsedRealtime();
        this.mAlarmManager.set(2, now + this.mFixInterval, TAG, this.mWakeupListener, this.mHandler);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: handleReportLocation */
    public void m4363x8d7ef217(boolean hasLatLong, Location location) {
        if (VERBOSE) {
            Log.v(TAG, "reportLocation " + location.toString());
        }
        location.setExtras(this.mLocationExtras.getBundle());
        reportLocation(LocationResult.wrap(new Location[]{location}).validate());
        if (this.mStarted) {
            this.mGnssMetrics.logReceivedLocationStatus(hasLatLong);
            if (hasLatLong) {
                if (location.hasAccuracy()) {
                    this.mGnssMetrics.logPositionAccuracyMeters(location.getAccuracy());
                }
                if (this.mTimeToFirstFix > 0) {
                    int timeBetweenFixes = (int) (SystemClock.elapsedRealtime() - this.mLastFixTime);
                    this.mGnssMetrics.logMissedReports(this.mFixInterval, timeBetweenFixes);
                }
            }
        } else {
            long locationAfterStartedFalseMillis = SystemClock.elapsedRealtime() - this.mStartedChangedElapsedRealtime;
            if (locationAfterStartedFalseMillis > LOCATION_OFF_DELAY_THRESHOLD_WARN_MILLIS) {
                String logMessage = "Unexpected GNSS Location report " + TimeUtils.formatDuration(locationAfterStartedFalseMillis) + " after location turned off";
                if (locationAfterStartedFalseMillis > LOCATION_OFF_DELAY_THRESHOLD_ERROR_MILLIS) {
                    Log.e(TAG, logMessage);
                } else {
                    Log.w(TAG, logMessage);
                }
            }
        }
        long locationAfterStartedFalseMillis2 = SystemClock.elapsedRealtime();
        this.mLastFixTime = locationAfterStartedFalseMillis2;
        if (this.mTimeToFirstFix == 0 && hasLatLong) {
            this.mTimeToFirstFix = (int) (locationAfterStartedFalseMillis2 - this.mFixRequestTime);
            Log.d(TAG, "TTFF: " + this.mTimeToFirstFix);
            if (this.mStarted) {
                this.mGnssMetrics.logTimeToFirstFixMilliSecs(this.mTimeToFirstFix);
            }
        }
        if (this.mStarted && !this.mGnssNative.getCapabilities().hasScheduling() && this.mFixInterval < 60000) {
            this.mAlarmManager.cancel(this.mTimeoutListener);
        }
        if (!this.mGnssNative.getCapabilities().hasScheduling() && this.mStarted && this.mFixInterval > 10000) {
            if (DEBUG) {
                Log.d(TAG, "got fix, hibernating");
            }
            hibernate();
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: handleReportSvStatus */
    public void m4364xa3b3c2d8(GnssStatus gnssStatus) {
        this.mGnssMetrics.logCn0(gnssStatus);
        if (VERBOSE) {
            Log.v(TAG, "SV count: " + gnssStatus.getSatelliteCount());
        }
        int usedInFixCount = 0;
        int maxCn0 = 0;
        int meanCn0 = 0;
        for (int i = 0; i < gnssStatus.getSatelliteCount(); i++) {
            if (gnssStatus.usedInFix(i)) {
                usedInFixCount++;
                if (gnssStatus.getCn0DbHz(i) > maxCn0) {
                    maxCn0 = (int) gnssStatus.getCn0DbHz(i);
                }
                meanCn0 = (int) (meanCn0 + gnssStatus.getCn0DbHz(i));
                this.mGnssMetrics.logConstellationType(gnssStatus.getConstellationType(i));
            }
            if (VERBOSE) {
                Log.v(TAG, "svid: " + gnssStatus.getSvid(i) + " cn0: " + gnssStatus.getCn0DbHz(i) + " basebandCn0: " + gnssStatus.getBasebandCn0DbHz(i) + " elev: " + gnssStatus.getElevationDegrees(i) + " azimuth: " + gnssStatus.getAzimuthDegrees(i) + " carrier frequency: " + gnssStatus.getCarrierFrequencyHz(i) + (gnssStatus.hasEphemerisData(i) ? " E" : "  ") + (gnssStatus.hasAlmanacData(i) ? " A" : "  ") + (gnssStatus.usedInFix(i) ? "U" : "") + (gnssStatus.hasCarrierFrequencyHz(i) ? "F" : "") + (gnssStatus.hasBasebandCn0DbHz(i) ? "B" : ""));
            }
        }
        if (usedInFixCount > 0) {
            meanCn0 /= usedInFixCount;
        }
        this.mLocationExtras.set(usedInFixCount, meanCn0, maxCn0);
        this.mGnssMetrics.logSvStatus(gnssStatus);
    }

    private void restartLocationRequest() {
        if (DEBUG) {
            Log.d(TAG, "restartLocationRequest");
        }
        setStarted(false);
        updateRequirements();
    }

    public INetInitiatedListener getNetInitiatedListener() {
        return this.mNetInitiatedListener;
    }

    private void reportNiNotification(int notificationId, int niType, int notifyFlags, int timeout, int defaultResponse, String requestorId, String text, int requestorIdEncoding, int textEncoding) {
        Log.i(TAG, "reportNiNotification: entered");
        Log.i(TAG, "notificationId: " + notificationId + ", niType: " + niType + ", notifyFlags: " + notifyFlags + ", timeout: " + timeout + ", defaultResponse: " + defaultResponse);
        Log.i(TAG, "requestorId: " + requestorId + ", text: " + text + ", requestorIdEncoding: " + requestorIdEncoding + ", textEncoding: " + textEncoding);
        GpsNetInitiatedHandler.GpsNiNotification notification = new GpsNetInitiatedHandler.GpsNiNotification();
        notification.notificationId = notificationId;
        notification.niType = niType;
        notification.needNotify = (notifyFlags & 1) != 0;
        notification.needVerify = (notifyFlags & 2) != 0;
        notification.privacyOverride = (notifyFlags & 4) != 0;
        notification.timeout = timeout;
        notification.defaultResponse = defaultResponse;
        notification.requestorId = requestorId;
        notification.text = text;
        notification.requestorIdEncoding = requestorIdEncoding;
        notification.textEncoding = textEncoding;
        this.mNIHandler.handleNiNotification(notification);
        FrameworkStatsLog.write(124, 1, notification.notificationId, notification.niType, notification.needNotify, notification.needVerify, notification.privacyOverride, notification.timeout, notification.defaultResponse, notification.requestorId, notification.text, notification.requestorIdEncoding, notification.textEncoding, this.mSuplEsEnabled, isGpsEnabled(), 0);
    }

    private void requestUtcTime() {
        if (DEBUG) {
            Log.d(TAG, "utcTimeRequest");
        }
        NtpTimeHelper ntpTimeHelper = this.mNtpTimeHelper;
        Objects.requireNonNull(ntpTimeHelper);
        postWithWakeLockHeld(new GnssLocationProvider$$ExternalSyntheticLambda25(ntpTimeHelper));
    }

    private static int getCellType(CellInfo ci) {
        if (ci instanceof CellInfoGsm) {
            return 1;
        }
        if (ci instanceof CellInfoWcdma) {
            return 4;
        }
        if (ci instanceof CellInfoLte) {
            return 3;
        }
        if (ci instanceof CellInfoNr) {
            return 6;
        }
        return 0;
    }

    private static long getCidFromCellIdentity(CellIdentity id) {
        if (id == null) {
            return -1L;
        }
        long cid = -1;
        switch (id.getType()) {
            case 1:
                cid = ((CellIdentityGsm) id).getCid();
                break;
            case 3:
                cid = ((CellIdentityLte) id).getCi();
                break;
            case 4:
                cid = ((CellIdentityWcdma) id).getCid();
                break;
            case 6:
                cid = ((CellIdentityNr) id).getNci();
                break;
        }
        if (cid == (id.getType() == 6 ? JobStatus.NO_LATEST_RUNTIME : 2147483647L)) {
            return -1L;
        }
        return cid;
    }

    private void setRefLocation(int type, CellIdentity ci) {
        int pcid;
        int arfcn;
        long cid;
        String mcc_str = ci.getMccString();
        String mnc_str = ci.getMncString();
        int mcc = mcc_str != null ? Integer.parseInt(mcc_str) : Integer.MAX_VALUE;
        int mnc = mnc_str != null ? Integer.parseInt(mnc_str) : Integer.MAX_VALUE;
        int lac = Integer.MAX_VALUE;
        int tac = Integer.MAX_VALUE;
        switch (type) {
            case 1:
                CellIdentityGsm cig = (CellIdentityGsm) ci;
                long cid2 = cig.getCid();
                lac = cig.getLac();
                pcid = Integer.MAX_VALUE;
                arfcn = Integer.MAX_VALUE;
                cid = cid2;
                break;
            case 2:
                CellIdentityWcdma ciw = (CellIdentityWcdma) ci;
                long cid3 = ciw.getCid();
                lac = ciw.getLac();
                pcid = Integer.MAX_VALUE;
                arfcn = Integer.MAX_VALUE;
                cid = cid3;
                break;
            case 4:
                CellIdentityLte cil = (CellIdentityLte) ci;
                long cid4 = cil.getCi();
                tac = cil.getTac();
                int pcid2 = cil.getPci();
                pcid = pcid2;
                arfcn = Integer.MAX_VALUE;
                cid = cid4;
                break;
            case 8:
                CellIdentityNr cin = (CellIdentityNr) ci;
                long cid5 = cin.getNci();
                tac = cin.getTac();
                int pcid3 = cin.getPci();
                int arfcn2 = cin.getNrarfcn();
                pcid = pcid3;
                arfcn = arfcn2;
                cid = cid5;
                break;
            default:
                pcid = Integer.MAX_VALUE;
                arfcn = Integer.MAX_VALUE;
                cid = Long.MAX_VALUE;
                break;
        }
        this.mGnssNative.setAgpsReferenceLocationCellId(type, mcc, mnc, lac, cid, tac, pcid, arfcn);
    }

    private void requestRefLocation() {
        TelephonyManager phone = (TelephonyManager) this.mContext.getSystemService("phone");
        int phoneType = phone.getPhoneType();
        if (phoneType == 1) {
            List<CellInfo> cil = phone.getAllCellInfo();
            if (cil == null) {
                Log.e(TAG, "Error getting cell location info.");
                return;
            }
            HashMap<Integer, CellIdentity> cellIdentityMap = new HashMap<>();
            cil.sort(Comparator.comparingInt(new ToIntFunction() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda8
                @Override // java.util.function.ToIntFunction
                public final int applyAsInt(Object obj) {
                    int asuLevel;
                    asuLevel = ((CellInfo) obj).getCellSignalStrength().getAsuLevel();
                    return asuLevel;
                }
            }).reversed());
            for (CellInfo ci : cil) {
                int status = ci.getCellConnectionStatus();
                if (status == 1 || status == 2) {
                    CellIdentity c = ci.getCellIdentity();
                    int t = getCellType(ci);
                    if (getCidFromCellIdentity(c) != -1 && !cellIdentityMap.containsKey(Integer.valueOf(t))) {
                        cellIdentityMap.put(Integer.valueOf(t), c);
                    }
                }
            }
            if (cellIdentityMap.containsKey(1)) {
                setRefLocation(1, cellIdentityMap.get(1));
            } else if (cellIdentityMap.containsKey(4)) {
                setRefLocation(2, cellIdentityMap.get(4));
            } else if (cellIdentityMap.containsKey(3)) {
                setRefLocation(4, cellIdentityMap.get(3));
            } else if (!cellIdentityMap.containsKey(6)) {
                Log.e(TAG, "No available serving cell information.");
            } else {
                setRefLocation(8, cellIdentityMap.get(6));
            }
        } else if (phoneType == 2) {
            Log.e(TAG, "CDMA not supported.");
        }
    }

    private void postWithWakeLockHeld(final Runnable runnable) {
        this.mWakeLock.acquire(30000L);
        boolean success = this.mHandler.post(new Runnable() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda19
            @Override // java.lang.Runnable
            public final void run() {
                GnssLocationProvider.this.m4368xd1ead7b3(runnable);
            }
        });
        if (!success) {
            this.mWakeLock.release();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$postWithWakeLockHeld$10$com-android-server-location-gnss-GnssLocationProvider  reason: not valid java name */
    public /* synthetic */ void m4368xd1ead7b3(Runnable runnable) {
        try {
            runnable.run();
        } finally {
            this.mWakeLock.release();
        }
    }

    @Override // com.android.server.location.provider.AbstractLocationProvider
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        String opt;
        boolean dumpAll = false;
        int opti = 0;
        while (true) {
            if (opti >= args.length || (opt = args[opti]) == null || opt.length() <= 0 || opt.charAt(0) != '-') {
                break;
            }
            opti++;
            if ("-a".equals(opt)) {
                dumpAll = true;
                break;
            }
        }
        pw.print("mStarted=" + this.mStarted + "   (changed ");
        TimeUtils.formatDuration(SystemClock.elapsedRealtime() - this.mStartedChangedElapsedRealtime, pw);
        pw.println(" ago)");
        pw.println("mBatchingEnabled=" + this.mBatchingEnabled);
        pw.println("mBatchingStarted=" + this.mBatchingStarted);
        pw.println("mBatchSize=" + getBatchSize());
        pw.println("mFixInterval=" + this.mFixInterval);
        pw.print(this.mGnssMetrics.dumpGnssMetricsAsText());
        if (dumpAll) {
            pw.println("mSupportsPsds=" + this.mSupportsPsds);
            pw.println("PsdsServerConfigured=" + this.mGnssConfiguration.isLongTermPsdsServerConfigured());
            pw.println("native internal state: ");
            pw.println("  " + this.mGnssNative.getInternalState());
        }
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.BaseCallbacks
    public void onHalRestarted() {
        reloadGpsProperties();
        if (isGpsEnabled()) {
            setGpsEnabled(false);
            updateEnabled();
            restartLocationRequest();
        }
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.BaseCallbacks
    public void onCapabilitiesChanged(GnssCapabilities oldCapabilities, GnssCapabilities newCapabilities) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                GnssLocationProvider.this.m4360x3e2526d9();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onCapabilitiesChanged$11$com-android-server-location-gnss-GnssLocationProvider  reason: not valid java name */
    public /* synthetic */ void m4360x3e2526d9() {
        if (this.mGnssNative.getCapabilities().hasOnDemandTime()) {
            this.mNtpTimeHelper.enablePeriodicTimeInjection();
            requestUtcTime();
        }
        restartLocationRequest();
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.LocationCallbacks
    public void onReportLocation(final boolean hasLatLong, final Location location) {
        postWithWakeLockHeld(new Runnable() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                GnssLocationProvider.this.m4363x8d7ef217(hasLatLong, location);
            }
        });
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.LocationCallbacks
    public void onReportLocations(Location[] locations) {
        int i;
        Runnable[] listeners;
        if (DEBUG) {
            Log.d(TAG, "Location batch of size " + locations.length + " reported");
        }
        if (locations.length > 0) {
            if (locations.length > 1) {
                boolean fixRealtime = false;
                int i2 = locations.length - 2;
                while (true) {
                    if (i2 < 0) {
                        break;
                    }
                    long timeDeltaMs = locations[i2 + 1].getTime() - locations[i2].getTime();
                    long realtimeDeltaMs = locations[i2 + 1].getElapsedRealtimeMillis() - locations[i2].getElapsedRealtimeMillis();
                    if (Math.abs(timeDeltaMs - realtimeDeltaMs) > 500) {
                        fixRealtime = true;
                        break;
                    }
                    i2--;
                }
                if (fixRealtime) {
                    Arrays.sort(locations, Comparator.comparingLong(new ToLongFunction() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda4
                        @Override // java.util.function.ToLongFunction
                        public final long applyAsLong(Object obj) {
                            return ((Location) obj).getTime();
                        }
                    }));
                    long expectedDeltaMs = locations[locations.length - 1].getTime() - locations[locations.length - 1].getElapsedRealtimeMillis();
                    for (int i3 = locations.length - 2; i3 >= 0; i3--) {
                        locations[i3].setElapsedRealtimeNanos(TimeUnit.MILLISECONDS.toNanos(Math.max(locations[i3].getTime() - expectedDeltaMs, 0L)));
                    }
                } else {
                    Arrays.sort(locations, Comparator.comparingLong(new ToLongFunction() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda5
                        @Override // java.util.function.ToLongFunction
                        public final long applyAsLong(Object obj) {
                            return ((Location) obj).getElapsedRealtimeNanos();
                        }
                    }));
                }
            }
            reportLocation(LocationResult.wrap(locations).validate());
        }
        synchronized (this.mLock) {
            listeners = (Runnable[]) this.mFlushListeners.toArray(new Runnable[0]);
            this.mFlushListeners.clear();
        }
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.SvStatusCallbacks
    public void onReportSvStatus(final GnssStatus gnssStatus) {
        postWithWakeLockHeld(new Runnable() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda18
            @Override // java.lang.Runnable
            public final void run() {
                GnssLocationProvider.this.m4364xa3b3c2d8(gnssStatus);
            }
        });
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.AGpsCallbacks
    public void onReportAGpsStatus(int agpsType, int agpsStatus, byte[] suplIpAddr) {
        this.mNetworkConnectivityHandler.onReportAGpsStatus(agpsType, agpsStatus, suplIpAddr);
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.PsdsCallbacks
    public void onRequestPsdsDownload(final int psdsType) {
        postWithWakeLockHeld(new Runnable() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                GnssLocationProvider.this.m4366x4b109e71(psdsType);
            }
        });
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.NotificationCallbacks
    public void onReportNiNotification(int notificationId, int niType, int notifyFlags, int timeout, int defaultResponse, String requestorId, String text, int requestorIdEncoding, int textEncoding) {
        reportNiNotification(notificationId, niType, notifyFlags, timeout, defaultResponse, requestorId, text, requestorIdEncoding, textEncoding);
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.AGpsCallbacks
    public void onRequestSetID(int flags) {
        TelephonyManager phone = (TelephonyManager) this.mContext.getSystemService("phone");
        int type = 0;
        String setId = null;
        int ddSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (SubscriptionManager.isValidSubscriptionId(ddSubId)) {
            phone = phone.createForSubscriptionId(ddSubId);
        }
        if ((flags & 1) == 1) {
            setId = phone.getSubscriberId();
            if (setId != null) {
                type = 1;
            }
        } else if ((flags & 2) == 2 && (setId = phone.getLine1Number()) != null) {
            type = 2;
        }
        this.mGnssNative.setAgpsSetId(type, setId == null ? "" : setId);
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.LocationRequestCallbacks
    public void onRequestLocation(final boolean independentFromGnss, final boolean isUserEmergency) {
        if (DEBUG) {
            Log.d(TAG, "requestLocation. independentFromGnss: " + independentFromGnss + ", isUserEmergency: " + isUserEmergency);
        }
        postWithWakeLockHeld(new Runnable() { // from class: com.android.server.location.gnss.GnssLocationProvider$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                GnssLocationProvider.this.m4365x159bc757(independentFromGnss, isUserEmergency);
            }
        });
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.TimeCallbacks
    public void onRequestUtcTime() {
        requestUtcTime();
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.LocationRequestCallbacks
    public void onRequestRefLocation() {
        requestRefLocation();
    }

    @Override // com.android.server.location.gnss.hal.GnssNative.NotificationCallbacks
    public void onReportNfwNotification(String proxyAppPackageName, byte protocolStack, String otherProtocolStackName, byte requestor, String requestorId, byte responseType, boolean inEmergencyMode, boolean isCachedLocation) {
        GnssVisibilityControl gnssVisibilityControl = this.mGnssVisibilityControl;
        if (gnssVisibilityControl == null) {
            Log.e(TAG, "reportNfwNotification: mGnssVisibilityControl uninitialized.");
        } else {
            gnssVisibilityControl.reportNfwNotification(proxyAppPackageName, protocolStack, otherProtocolStackName, requestor, requestorId, responseType, inEmergencyMode, isCachedLocation);
        }
    }

    private void initMtkGnssLocProvider() {
        Constructor constructor;
        try {
            this.mMtkGnssProviderClass = Class.forName("com.mediatek.location.MtkLocationExt$GnssLocationProvider");
            if (DEBUG) {
                Log.d(TAG, "class = " + this.mMtkGnssProviderClass);
            }
            Class<?> cls = this.mMtkGnssProviderClass;
            if (cls != null && (constructor = cls.getConstructor(Context.class, Handler.class)) != null) {
                this.mMtkGnssProvider = constructor.newInstance(this.mContext, this.mHandler);
            }
            Log.d(TAG, "mMtkGnssProvider = " + this.mMtkGnssProvider);
            this.mNtpTimeHelper.setNtpTimeStateIdle();
        } catch (Exception e) {
            Log.w(TAG, "Failed to init mMtkGnssProvider!");
        }
    }

    private int mtkDeleteAidingData(Bundle extras, int flags) {
        if (this.mMtkGnssProvider != null) {
            if (extras != null) {
                if (extras.getBoolean("hot-still")) {
                    flags |= 8192;
                }
                if (extras.getBoolean("epo")) {
                    flags |= 16384;
                }
            }
            Log.d(TAG, "mtkDeleteAidingData extras:" + extras + "flags:" + flags);
        }
        return flags;
    }

    private void mtkInjectLastKnownLocation() {
        if (this.mMtkGnssProvider != null) {
            LocationManager locationManager = (LocationManager) this.mContext.getSystemService("location");
            Location lastLocation = locationManager.getLastKnownLocation("network");
            if (lastLocation != null) {
                long currentUtcTime = System.currentTimeMillis();
                long nlpTime = lastLocation.getTime();
                long deltaMs = currentUtcTime - nlpTime;
                if (deltaMs < 600000) {
                    injectLocation(lastLocation);
                }
            }
        }
    }
}
