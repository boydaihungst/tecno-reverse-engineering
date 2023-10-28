package com.android.server;

import android.app.ActivityThread;
import android.app.AppCompatCallbacks;
import android.app.ApplicationErrorReport;
import android.app.ContextImpl;
import android.app.INotificationManager;
import android.app.SystemServiceRegistry;
import android.app.admin.DevicePolicySafetyChecker;
import android.app.usage.UsageStatsManagerInternal;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.sqlite.SQLiteCompatibilityWalFlags;
import android.database.sqlite.SQLiteGlobal;
import android.graphics.GraphicsStatsService;
import android.graphics.Typeface;
import android.hardware.IConsumerIrService;
import android.hardware.display.DisplayManagerInternal;
import android.location.ICountryDetector;
import android.net.ConnectivityManager;
import android.net.ConnectivityModuleConnector;
import android.net.INetworkPolicyManager;
import android.net.IPacProxyManager;
import android.net.IVpnManager;
import android.net.NetworkStackClient;
import android.net.vcn.IVcnManagementService;
import android.os.BaseBundle;
import android.os.Binder;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.FactoryTest;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.IHardwarePropertiesManager;
import android.os.IIncidentManager;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.IStorageManager;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.sysprop.VoldProperties;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.DisplayMetrics;
import android.util.Dumpable;
import android.util.EventLog;
import android.util.IndentingPrintWriter;
import android.util.Pair;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.i18n.timezone.ZoneInfoDb;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.BinderInternal;
import com.android.internal.os.RuntimeInit;
import com.android.internal.policy.AttributeCache;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.widget.ILockSettings;
import com.android.server.BinderCallsStatsService;
import com.android.server.LooperStatsService;
import com.android.server.NetworkScoreService;
import com.android.server.TelephonyRegistry;
import com.android.server.am.ActivityManagerService;
import com.android.server.ambientcontext.AmbientContextManagerService;
import com.android.server.app.GameManagerService;
import com.android.server.appbinding.AppBindingService;
import com.android.server.art.ArtManagerLocal;
import com.android.server.attention.AttentionManagerService;
import com.android.server.audio.AudioService;
import com.android.server.biometrics.AuthService;
import com.android.server.biometrics.BiometricService;
import com.android.server.biometrics.sensors.face.FaceService;
import com.android.server.biometrics.sensors.fingerprint.FingerprintService;
import com.android.server.biometrics.sensors.iris.IrisService;
import com.android.server.broadcastradio.BroadcastRadioService;
import com.android.server.camera.CameraServiceProxy;
import com.android.server.clipboard.ClipboardService;
import com.android.server.compat.PlatformCompat;
import com.android.server.compat.PlatformCompatNative;
import com.android.server.connectivity.PacProxyService;
import com.android.server.contentcapture.ContentCaptureManagerInternal;
import com.android.server.coverage.CoverageService;
import com.android.server.devicepolicy.DevicePolicyManagerService;
import com.android.server.devicestate.DeviceStateManagerService;
import com.android.server.display.DisplayManagerService;
import com.android.server.display.color.ColorDisplayService;
import com.android.server.dreams.DreamManagerService;
import com.android.server.emergency.EmergencyAffordanceService;
import com.android.server.gpu.GpuService;
import com.android.server.graphics.fonts.FontManagerService;
import com.android.server.hdmi.HdmiControlService;
import com.android.server.health.HealthServiceWrapperHidl;
import com.android.server.incident.IncidentCompanionService;
import com.android.server.input.InputManagerService;
import com.android.server.inputmethod.InputMethodManagerService;
import com.android.server.integrity.AppIntegrityManagerService;
import com.android.server.lights.LightsService;
import com.android.server.locales.LocaleManagerService;
import com.android.server.location.LocationManagerService;
import com.android.server.logcat.LogcatManagerService;
import com.android.server.media.MediaRouterService;
import com.android.server.media.metrics.MediaMetricsManagerService;
import com.android.server.media.projection.MediaProjectionManagerService;
import com.android.server.net.NetworkPolicyManagerService;
import com.android.server.net.watchlist.NetworkWatchlistService;
import com.android.server.notification.NotificationManagerService;
import com.android.server.oemlock.OemLockService;
import com.android.server.om.OverlayManagerService;
import com.android.server.os.BugreportManagerService;
import com.android.server.os.DeviceIdentifiersPolicyService;
import com.android.server.os.NativeTombstoneManagerService;
import com.android.server.os.SchedulingPolicyService;
import com.android.server.people.PeopleService;
import com.android.server.pm.ApexManager;
import com.android.server.pm.ApexSystemServiceInfo;
import com.android.server.pm.CrossProfileAppsService;
import com.android.server.pm.DataLoaderManagerService;
import com.android.server.pm.DynamicCodeLoggingService;
import com.android.server.pm.Installer;
import com.android.server.pm.LauncherAppsService;
import com.android.server.pm.OtaDexoptService;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.ShortcutService;
import com.android.server.pm.UserManagerService;
import com.android.server.pm.dex.OdsignStatsLogger;
import com.android.server.pm.dex.SystemServerDexLoadReporter;
import com.android.server.pm.verify.domain.DomainVerificationService;
import com.android.server.policy.AppOpsPolicy;
import com.android.server.policy.PermissionPolicyService;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.policy.role.RoleServicePlatformHelperImpl;
import com.android.server.power.PowerManagerService;
import com.android.server.power.ShutdownThread;
import com.android.server.power.ThermalManagerService;
import com.android.server.power.hint.HintManagerService;
import com.android.server.powerstats.PowerStatsService;
import com.android.server.profcollect.ProfcollectForwardingService;
import com.android.server.recoverysystem.RecoverySystemService;
import com.android.server.resources.ResourcesManagerService;
import com.android.server.restrictions.RestrictionsManagerService;
import com.android.server.role.RoleServicePlatformHelper;
import com.android.server.rotationresolver.RotationResolverManagerService;
import com.android.server.security.AttestationVerificationManagerService;
import com.android.server.security.FileIntegrityService;
import com.android.server.security.KeyAttestationApplicationIdProviderService;
import com.android.server.security.KeyChainSystemService;
import com.android.server.sensorprivacy.SensorPrivacyService;
import com.android.server.sensors.SensorService;
import com.android.server.signedconfig.SignedConfigService;
import com.android.server.soundtrigger.SoundTriggerService;
import com.android.server.soundtrigger_middleware.SoundTriggerMiddlewareService;
import com.android.server.statusbar.StatusBarManagerService;
import com.android.server.storage.DeviceStorageMonitorService;
import com.android.server.telecom.TelecomLoaderService;
import com.android.server.testharness.TestHarnessModeService;
import com.android.server.textclassifier.TextClassificationManagerService;
import com.android.server.textservices.TextServicesManagerService;
import com.android.server.tracing.TracingServiceProxy;
import com.android.server.trust.TrustManagerService;
import com.android.server.tv.TvInputManagerService;
import com.android.server.tv.TvRemoteService;
import com.android.server.tv.interactive.TvInteractiveAppManagerService;
import com.android.server.tv.tunerresourcemanager.TunerResourceManagerService;
import com.android.server.twilight.TwilightService;
import com.android.server.uri.UriGrantsManagerService;
import com.android.server.usage.UsageStatsService;
import com.android.server.utils.TimingsTraceAndSlog;
import com.android.server.vibrator.VibratorManagerService;
import com.android.server.vr.VrManagerService;
import com.android.server.webkit.WebViewUpdateService;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.WindowManagerGlobalLock;
import com.android.server.wm.WindowManagerService;
import com.mediatek.server.MtkSystemServer;
import com.transsion.foldable.TranFoldingScreenManager;
import com.transsion.hubcore.server.ITranSystemServer;
import com.transsion.server.TranAipowerlabService;
import com.transsion.server.TranPowerhubService;
import com.transsion.server.foldable.TranFoldingScreenService;
import com.transsion.uxdetector.UxDetectorFactory;
import com.transsion.xmlprotect.VerifyExt;
import com.transsion.xmlprotect.VerifyFacotry;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
/* loaded from: classes.dex */
public final class SystemServer implements Dumpable {
    private static final String ACCESSIBILITY_MANAGER_SERVICE_CLASS = "com.android.server.accessibility.AccessibilityManagerService$Lifecycle";
    private static final String ACCOUNT_SERVICE_CLASS = "com.android.server.accounts.AccountManagerService$Lifecycle";
    private static final String ADB_SERVICE_CLASS = "com.android.server.adb.AdbService$Lifecycle";
    private static final String AD_SERVICES_MANAGER_SERVICE_CLASS = "com.android.server.adservices.AdServicesManagerService$Lifecycle";
    private static final String ALARM_MANAGER_SERVICE_CLASS = "com.android.server.alarm.AlarmManagerService";
    private static final String APPSEARCH_MODULE_LIFECYCLE_CLASS = "com.android.server.appsearch.AppSearchModule$Lifecycle";
    private static final String APPWIDGET_SERVICE_CLASS = "com.android.server.appwidget.AppWidgetService";
    private static final String APP_COMPAT_OVERRIDES_SERVICE_CLASS = "com.android.server.compat.overrides.AppCompatOverridesService$Lifecycle";
    private static final String APP_HIBERNATION_SERVICE_CLASS = "com.android.server.apphibernation.AppHibernationService";
    private static final String APP_PREDICTION_MANAGER_SERVICE_CLASS = "com.android.server.appprediction.AppPredictionManagerService";
    private static final String AUTO_FILL_MANAGER_SERVICE_CLASS = "com.android.server.autofill.AutofillManagerService";
    private static final String BACKUP_MANAGER_SERVICE_CLASS = "com.android.server.backup.BackupManagerService$Lifecycle";
    private static final String BLOB_STORE_MANAGER_SERVICE_CLASS = "com.android.server.blob.BlobStoreManagerService";
    private static final String BLOCK_MAP_FILE = "/cache/recovery/block.map";
    private static final String BLUETOOTH_APEX_SERVICE_JAR_PATH = "/apex/com.android.btservices/javalib/service-bluetooth.jar";
    private static final String BLUETOOTH_SERVICE_CLASS = "com.android.server.bluetooth.BluetoothService";
    private static final String CAR_SERVICE_HELPER_SERVICE_CLASS = "com.android.internal.car.CarServiceHelperService";
    private static final String CLOUDSEARCH_MANAGER_SERVICE_CLASS = "com.android.server.cloudsearch.CloudSearchManagerService";
    private static final String COMPANION_DEVICE_MANAGER_SERVICE_CLASS = "com.android.server.companion.CompanionDeviceManagerService";
    private static final String CONNECTIVITY_SERVICE_APEX_PATH = "/apex/com.android.tethering/javalib/service-connectivity.jar";
    private static final String CONNECTIVITY_SERVICE_INITIALIZER_CLASS = "com.android.server.ConnectivityServiceInitializer";
    private static final String CONTENT_CAPTURE_MANAGER_SERVICE_CLASS = "com.android.server.contentcapture.ContentCaptureManagerService";
    private static final String CONTENT_SERVICE_CLASS = "com.android.server.content.ContentService$Lifecycle";
    private static final String CONTENT_SUGGESTIONS_SERVICE_CLASS = "com.android.server.contentsuggestions.ContentSuggestionsManagerService";
    private static final int DEFAULT_SYSTEM_THEME = 16974861;
    private static final String DEVICE_IDLE_CONTROLLER_CLASS = "com.android.server.DeviceIdleController";
    private static final String ENCRYPTING_STATE = "trigger_restart_min_framework";
    private static final String GAME_MANAGER_SERVICE_CLASS = "com.android.server.app.GameManagerService$Lifecycle";
    private static final String GNSS_TIME_UPDATE_SERVICE_CLASS = "com.android.server.timedetector.GnssTimeUpdateService$Lifecycle";
    private static final String HEALTH_SERVICE_CLASS = "com.google.android.clockwork.healthservices.HealthService";
    private static final String IOT_SERVICE_CLASS = "com.android.things.server.IoTSystemService";
    private static final String IP_CONNECTIVITY_METRICS_CLASS = "com.android.server.connectivity.IpConnectivityMetrics";
    private static final String ISOLATED_COMPILATION_SERVICE_CLASS = "com.android.server.compos.IsolatedCompilationService";
    private static final String JOB_SCHEDULER_SERVICE_CLASS = "com.android.server.job.JobSchedulerService";
    private static final String LOCATION_TIME_ZONE_MANAGER_SERVICE_CLASS = "com.android.server.timezonedetector.location.LocationTimeZoneManagerService$Lifecycle";
    private static final String LOCK_SETTINGS_SERVICE_CLASS = "com.android.server.locksettings.LockSettingsService$Lifecycle";
    private static final String LOWPAN_SERVICE_CLASS = "com.android.server.lowpan.LowpanService";
    private static final int MAX_HEAP_DUMPS = 2;
    private static final String MEDIA_COMMUNICATION_SERVICE_CLASS = "com.android.server.media.MediaCommunicationService";
    private static final String MEDIA_RESOURCE_MONITOR_SERVICE_CLASS = "com.android.server.media.MediaResourceMonitorService";
    private static final String MEDIA_SESSION_SERVICE_CLASS = "com.android.server.media.MediaSessionService";
    private static final String MIDI_SERVICE_CLASS = "com.android.server.midi.MidiService$Lifecycle";
    private static final String MUSIC_RECOGNITION_MANAGER_SERVICE_CLASS = "com.android.server.musicrecognition.MusicRecognitionManagerService";
    private static final String NETWORK_STATS_SERVICE_INITIALIZER_CLASS = "com.android.server.NetworkStatsServiceInitializer";
    private static final String PERSISTENT_DATA_BLOCK_PROP = "ro.frp.pst";
    private static final String PRINT_MANAGER_SERVICE_CLASS = "com.android.server.print.PrintManagerService";
    private static final String REBOOT_READINESS_LIFECYCLE_CLASS = "com.android.server.scheduling.RebootReadinessManagerService$Lifecycle";
    private static final String RESOURCE_ECONOMY_SERVICE_CLASS = "com.android.server.tare.InternalResourceService";
    private static final String ROLE_SERVICE_CLASS = "com.android.role.RoleService";
    private static final String ROLLBACK_MANAGER_SERVICE_CLASS = "com.android.server.rollback.RollbackManagerService";
    private static final String SAFETY_CENTER_SERVICE_CLASS = "com.android.safetycenter.SafetyCenterService";
    private static final String SCHEDULING_APEX_PATH = "/apex/com.android.scheduling/javalib/service-scheduling.jar";
    private static final String SDK_SANDBOX_MANAGER_SERVICE_CLASS = "com.android.server.sdksandbox.SdkSandboxManagerService$Lifecycle";
    private static final String SEARCH_MANAGER_SERVICE_CLASS = "com.android.server.search.SearchManagerService$Lifecycle";
    private static final String SEARCH_UI_MANAGER_SERVICE_CLASS = "com.android.server.searchui.SearchUiManagerService";
    private static final String SLICE_MANAGER_SERVICE_CLASS = "com.android.server.slice.SliceManagerService$Lifecycle";
    private static final long SLOW_DELIVERY_THRESHOLD_MS = 200;
    private static final long SLOW_DISPATCH_THRESHOLD_MS = 100;
    private static final String SMARTSPACE_MANAGER_SERVICE_CLASS = "com.android.server.smartspace.SmartspaceManagerService";
    private static final String SPEECH_RECOGNITION_MANAGER_SERVICE_CLASS = "com.android.server.speech.SpeechRecognitionManagerService";
    private static final String START_BLOB_STORE_SERVICE = "startBlobStoreManagerService";
    private static final String START_HIDL_SERVICES = "StartHidlServices";
    private static final String STATS_BOOTSTRAP_ATOM_SERVICE_LIFECYCLE_CLASS = "com.android.server.stats.bootstrap.StatsBootstrapAtomService$Lifecycle";
    private static final String STATS_COMPANION_APEX_PATH = "/apex/com.android.os.statsd/javalib/service-statsd.jar";
    private static final String STATS_COMPANION_LIFECYCLE_CLASS = "com.android.server.stats.StatsCompanion$Lifecycle";
    private static final String STATS_PULL_ATOM_SERVICE_CLASS = "com.android.server.stats.pull.StatsPullAtomService";
    private static final String STORAGE_MANAGER_SERVICE_CLASS = "com.android.server.StorageManagerService$Lifecycle";
    private static final String STORAGE_STATS_SERVICE_CLASS = "com.android.server.usage.StorageStatsService$Lifecycle";
    private static final String SYSPROP_FDTRACK_ABORT_THRESHOLD = "persist.sys.debug.fdtrack_abort_threshold";
    private static final String SYSPROP_FDTRACK_ENABLE_THRESHOLD = "persist.sys.debug.fdtrack_enable_threshold";
    private static final String SYSPROP_FDTRACK_INTERVAL = "persist.sys.debug.fdtrack_interval";
    private static final String SYSPROP_START_COUNT = "sys.system_server.start_count";
    private static final String SYSPROP_START_ELAPSED = "sys.system_server.start_elapsed";
    private static final String SYSPROP_START_UPTIME = "sys.system_server.start_uptime";
    private static final String SYSTEM_CAPTIONS_MANAGER_SERVICE_CLASS = "com.android.server.systemcaptions.SystemCaptionsManagerService";
    private static final String TETHERING_CONNECTOR_CLASS = "android.net.ITetheringConnector";
    private static final String TEXT_TO_SPEECH_MANAGER_SERVICE_CLASS = "com.android.server.texttospeech.TextToSpeechManagerService";
    private static final String THERMAL_OBSERVER_CLASS = "com.google.android.clockwork.ThermalObserver";
    private static final String TIME_DETECTOR_SERVICE_CLASS = "com.android.server.timedetector.TimeDetectorService$Lifecycle";
    private static final String TIME_ZONE_DETECTOR_SERVICE_CLASS = "com.android.server.timezonedetector.TimeZoneDetectorService$Lifecycle";
    private static final String TIME_ZONE_RULES_MANAGER_SERVICE_CLASS = "com.android.server.timezone.RulesManagerService$Lifecycle";
    private static final String TRANSLATION_MANAGER_SERVICE_CLASS = "com.android.server.translation.TranslationManagerService";
    private static final String UNCRYPT_PACKAGE_FILE = "/cache/recovery/uncrypt_file";
    private static final String USB_SERVICE_CLASS = "com.android.server.usb.UsbService$Lifecycle";
    private static final String UWB_APEX_SERVICE_JAR_PATH = "/apex/com.android.uwb/javalib/service-uwb.jar";
    private static final String UWB_SERVICE_CLASS = "com.android.server.uwb.UwbService";
    private static final String VIRTUAL_DEVICE_MANAGER_SERVICE_CLASS = "com.android.server.companion.virtual.VirtualDeviceManagerService";
    private static final String VOICE_RECOGNITION_MANAGER_SERVICE_CLASS = "com.android.server.voiceinteraction.VoiceInteractionManagerService";
    private static final String WALLPAPER_EFFECTS_GENERATION_MANAGER_SERVICE_CLASS = "com.android.server.wallpapereffectsgeneration.WallpaperEffectsGenerationManagerService";
    private static final String WALLPAPER_SERVICE_CLASS = "com.android.server.wallpaper.WallpaperManagerService$Lifecycle";
    private static final String WEAR_CONNECTIVITY_SERVICE_CLASS = "com.android.clockwork.connectivity.WearConnectivityService";
    private static final String WEAR_DISPLAYOFFLOAD_SERVICE_CLASS = "com.google.android.clockwork.displayoffload.DisplayOffloadService";
    private static final String WEAR_DISPLAY_SERVICE_CLASS = "com.google.android.clockwork.display.WearDisplayService";
    private static final String WEAR_GLOBAL_ACTIONS_SERVICE_CLASS = "com.android.clockwork.globalactions.GlobalActionsService";
    private static final String WEAR_LEFTY_SERVICE_CLASS = "com.google.android.clockwork.lefty.WearLeftyService";
    private static final String WEAR_POWER_SERVICE_CLASS = "com.android.clockwork.power.WearPowerService";
    private static final String WEAR_SIDEKICK_SERVICE_CLASS = "com.google.android.clockwork.sidekick.SidekickService";
    private static final String WEAR_TIME_SERVICE_CLASS = "com.google.android.clockwork.time.WearTimeService";
    private static final String WIFI_APEX_SERVICE_JAR_PATH = "/apex/com.android.wifi/javalib/service-wifi.jar";
    private static final String WIFI_AWARE_SERVICE_CLASS = "com.android.server.wifi.aware.WifiAwareService";
    private static final String WIFI_P2P_SERVICE_CLASS = "com.android.server.wifi.p2p.WifiP2pService";
    private static final String WIFI_RTT_SERVICE_CLASS = "com.android.server.wifi.rtt.RttService";
    private static final String WIFI_SCANNING_SERVICE_CLASS = "com.android.server.wifi.scanner.WifiScanningService";
    private static final String WIFI_SERVICE_CLASS = "com.android.server.wifi.WifiService";
    private static final int sMaxBinderThreads = 31;
    private static LinkedList<Pair<String, ApplicationErrorReport.CrashInfo>> sPendingWtfs;
    private ActivityManagerService mActivityManagerService;
    private ContentResolver mContentResolver;
    private DataLoaderManagerService mDataLoaderManagerService;
    private DisplayManagerService mDisplayManagerService;
    private EntropyMixer mEntropyMixer;
    private boolean mFirstBoot;
    private boolean mOnlyCore;
    private PackageManager mPackageManager;
    private PackageManagerService mPackageManagerService;
    private PowerManagerService mPowerManagerService;
    private Timer mProfilerSnapshotTimer;
    private final boolean mRuntimeRestart;
    private final long mRuntimeStartElapsedTime;
    private final long mRuntimeStartUptime;
    private Context mSystemContext;
    private SystemServiceManager mSystemServiceManager;
    private WebViewUpdateService mWebViewUpdateService;
    private WindowManagerGlobalLock mWindowManagerGlobalLock;
    private Future<?> mZygotePreload;
    private static final String TAG = "SystemServer";
    private static final TimingsTraceAndSlog BOOT_TIMINGS_TRACE_LOG = new TimingsTraceAndSlog(TAG, 524288);
    private static MtkSystemServer sMtkSystemServerIns = MtkSystemServer.getInstance();
    private static final File HEAP_DUMP_PATH = new File("/data/system/heapdump/");
    private static final String ENCRYPTED_STATE = "1";
    private static final boolean mXmlSupport = ENCRYPTED_STATE.equals(SystemProperties.get("ro.vendor.tran.xml.support", "0"));
    private long mIncrementalServiceHandle = 0;
    private final SystemServerDumper mDumper = new SystemServerDumper();
    private final int mFactoryTestMode = FactoryTest.getMode();
    private final int mStartCount = SystemProperties.getInt(SYSPROP_START_COUNT, 0) + 1;

    private static native void fdtrackAbort();

    private static native void initZygoteChildHeapProfiling();

    private static native void setIncrementalServiceSystemReady(long j);

    private static native void startHidlServices();

    private static native void startIStatsService();

    private static native long startIncrementalService();

    private static native void startMemtrackProxyService();

    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    private static int getMaxFd() {
        FileDescriptor fd = null;
        try {
            try {
                fd = Os.open("/dev/null", OsConstants.O_RDONLY | OsConstants.O_CLOEXEC, 0);
                int int$ = fd.getInt$();
                if (fd != null) {
                    try {
                        Os.close(fd);
                    } catch (ErrnoException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                return int$;
            } catch (Throwable ex2) {
                if (fd != null) {
                    try {
                        Os.close(fd);
                    } catch (ErrnoException ex3) {
                        throw new RuntimeException(ex3);
                    }
                }
                throw ex2;
            }
        } catch (ErrnoException ex4) {
            Slog.e("System", "Failed to get maximum fd: " + ex4);
            if (fd != null) {
                try {
                    Os.close(fd);
                    return Integer.MAX_VALUE;
                } catch (ErrnoException ex5) {
                    throw new RuntimeException(ex5);
                }
            }
            return Integer.MAX_VALUE;
        }
    }

    private static void dumpHprof() {
        File[] listFiles;
        TreeSet<File> existingTombstones = new TreeSet<>();
        for (File file : HEAP_DUMP_PATH.listFiles()) {
            if (file.isFile() && file.getName().startsWith("fdtrack-")) {
                existingTombstones.add(file);
            }
        }
        if (existingTombstones.size() >= 2) {
            for (int i = 0; i < 1; i++) {
                existingTombstones.pollLast();
            }
            Iterator<File> it = existingTombstones.iterator();
            while (it.hasNext()) {
                File file2 = it.next();
                if (!file2.delete()) {
                    Slog.w("System", "Failed to clean up hprof " + file2);
                }
            }
        }
        try {
            String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
            String filename = "/data/system/heapdump/fdtrack-" + date + ".hprof";
            Debug.dumpHprofData(filename);
        } catch (IOException ex) {
            Slog.e("System", "Failed to dump fdtrack hprof", ex);
        }
    }

    private static void spawnFdLeakCheckThread() {
        final int enableThreshold = SystemProperties.getInt(SYSPROP_FDTRACK_ENABLE_THRESHOLD, 1024);
        final int abortThreshold = SystemProperties.getInt(SYSPROP_FDTRACK_ABORT_THRESHOLD, 2048);
        final int checkInterval = SystemProperties.getInt(SYSPROP_FDTRACK_INTERVAL, 120);
        new Thread(new Runnable() { // from class: com.android.server.SystemServer$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                SystemServer.lambda$spawnFdLeakCheckThread$0(enableThreshold, abortThreshold, checkInterval);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$spawnFdLeakCheckThread$0(int enableThreshold, int abortThreshold, int checkInterval) {
        boolean enabled = false;
        long nextWrite = 0;
        while (true) {
            int maxFd = getMaxFd();
            if (maxFd > enableThreshold) {
                System.gc();
                System.runFinalization();
                maxFd = getMaxFd();
            }
            int i = 2;
            if (maxFd > enableThreshold && !enabled) {
                Slog.i("System", "fdtrack enable threshold reached, enabling");
                FrameworkStatsLog.write(364, 2, maxFd);
                System.loadLibrary("fdtrack");
                enabled = true;
            } else if (maxFd > abortThreshold) {
                Slog.i("System", "fdtrack abort threshold reached, dumping and aborting");
                FrameworkStatsLog.write(364, 3, maxFd);
                dumpHprof();
                fdtrackAbort();
            } else {
                long now = SystemClock.elapsedRealtime();
                if (now > nextWrite) {
                    long nextWrite2 = 3600000 + now;
                    if (!enabled) {
                        i = 1;
                    }
                    FrameworkStatsLog.write(364, i, maxFd);
                    nextWrite = nextWrite2;
                }
            }
            try {
                Thread.sleep(checkInterval * 1000);
            } catch (InterruptedException e) {
            }
        }
    }

    public static void main(String[] args) {
        new SystemServer().run();
    }

    public SystemServer() {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        this.mRuntimeStartElapsedTime = elapsedRealtime;
        long uptimeMillis = SystemClock.uptimeMillis();
        this.mRuntimeStartUptime = uptimeMillis;
        Process.setStartTimes(elapsedRealtime, uptimeMillis, elapsedRealtime, uptimeMillis);
        this.mRuntimeRestart = ENCRYPTED_STATE.equals(SystemProperties.get("sys.boot_completed"));
    }

    public String getDumpableName() {
        return SystemServer.class.getSimpleName();
    }

    public void dump(PrintWriter pw, String[] args) {
        pw.printf("Runtime restart: %b\n", Boolean.valueOf(this.mRuntimeRestart));
        pw.printf("Start count: %d\n", Integer.valueOf(this.mStartCount));
        pw.print("Runtime start-up time: ");
        TimeUtils.formatDuration(this.mRuntimeStartUptime, pw);
        pw.println();
        pw.print("Runtime start-elapsed time: ");
        TimeUtils.formatDuration(this.mRuntimeStartElapsedTime, pw);
        pw.println();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SystemServerDumper extends Binder {
        private final ArrayMap<String, Dumpable> mDumpables;

        private SystemServerDumper() {
            this.mDumpables = new ArrayMap<>(4);
        }

        @Override // android.os.Binder
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            boolean hasArgs = args != null && args.length > 0;
            synchronized (this.mDumpables) {
                if (hasArgs) {
                    try {
                        if ("--list".equals(args[0])) {
                            int dumpablesSize = this.mDumpables.size();
                            for (int i = 0; i < dumpablesSize; i++) {
                                pw.println(this.mDumpables.keyAt(i));
                            }
                            return;
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                }
                if (hasArgs && "--name".equals(args[0])) {
                    if (args.length < 2) {
                        pw.println("Must pass at least one argument to --name");
                        return;
                    }
                    String name = args[1];
                    Dumpable dumpable = this.mDumpables.get(name);
                    if (dumpable == null) {
                        pw.printf("No dummpable named %s\n", name);
                        return;
                    }
                    IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
                    String[] actualArgs = (String[]) Arrays.copyOfRange(args, 2, args.length);
                    dumpable.dump(ipw, actualArgs);
                    ipw.close();
                    return;
                }
                int dumpablesSize2 = this.mDumpables.size();
                IndentingPrintWriter ipw2 = new IndentingPrintWriter(pw, "  ");
                for (int i2 = 0; i2 < dumpablesSize2; i2++) {
                    Dumpable dumpable2 = this.mDumpables.valueAt(i2);
                    ipw2.printf("%s:\n", new Object[]{dumpable2.getDumpableName()});
                    ipw2.increaseIndent();
                    dumpable2.dump(ipw2, args);
                    ipw2.decreaseIndent();
                    ipw2.println();
                }
                ipw2.close();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void addDumpable(Dumpable dumpable) {
            synchronized (this.mDumpables) {
                this.mDumpables.put(dumpable.getDumpableName(), dumpable);
            }
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[THROW, INVOKE, MOVE_EXCEPTION, THROW, CONST_STR, INVOKE, CONST_STR, INVOKE, THROW, INVOKE, MOVE_EXCEPTION] complete} */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    private void run() {
        TimingsTraceAndSlog t = new TimingsTraceAndSlog();
        try {
            t.traceBegin("InitBeforeStartServices");
            SystemProperties.set(SYSPROP_START_COUNT, String.valueOf(this.mStartCount));
            SystemProperties.set(SYSPROP_START_ELAPSED, String.valueOf(this.mRuntimeStartElapsedTime));
            SystemProperties.set(SYSPROP_START_UPTIME, String.valueOf(this.mRuntimeStartUptime));
            EventLog.writeEvent((int) EventLogTags.SYSTEM_SERVER_START, Integer.valueOf(this.mStartCount), Long.valueOf(this.mRuntimeStartUptime), Long.valueOf(this.mRuntimeStartElapsedTime));
            String timezoneProperty = SystemProperties.get("persist.sys.timezone");
            if (!isValidTimeZoneId(timezoneProperty)) {
                Slog.w(TAG, "persist.sys.timezone is not valid (" + timezoneProperty + "); setting to GMT.");
                SystemProperties.set("persist.sys.timezone", "GMT");
            }
            if (!SystemProperties.get("persist.sys.language").isEmpty()) {
                String languageTag = Locale.getDefault().toLanguageTag();
                SystemProperties.set("persist.sys.locale", languageTag);
                SystemProperties.set("persist.sys.language", "");
                SystemProperties.set("persist.sys.country", "");
                SystemProperties.set("persist.sys.localevar", "");
            }
            Binder.setWarnOnBlocking(true);
            PackageItemInfo.forceSafeLabels();
            SQLiteGlobal.sDefaultSyncMode = "FULL";
            SQLiteCompatibilityWalFlags.init((String) null);
            Slog.i(TAG, "Entered the Android system server!");
            long uptimeMillis = SystemClock.elapsedRealtime();
            EventLog.writeEvent((int) EventLogTags.BOOT_PROGRESS_SYSTEM_RUN, uptimeMillis);
            if (!this.mRuntimeRestart) {
                FrameworkStatsLog.write((int) FrameworkStatsLog.BOOT_TIME_EVENT_ELAPSED_TIME_REPORTED, 19, uptimeMillis);
            }
            sMtkSystemServerIns.addBootEvent("Android:SysServerInit_START");
            SystemProperties.set("persist.sys.dalvik.vm.lib.2", VMRuntime.getRuntime().vmLibrary());
            VMRuntime.getRuntime().clearGrowthLimit();
            Build.ensureFingerprintProperty();
            Environment.setUserRequired(true);
            BaseBundle.setShouldDefuse(true);
            Parcel.setStackTraceParceling(true);
            BinderInternal.disableBackgroundScheduling(true);
            BinderInternal.setMaxThreads(31);
            Process.setThreadPriority(-2);
            Process.setCanSelfBackground(false);
            Looper.prepareMainLooper();
            Looper.getMainLooper().setSlowLogThresholdMs(SLOW_DISPATCH_THRESHOLD_MS, SLOW_DELIVERY_THRESHOLD_MS);
            SystemServiceRegistry.sEnableServiceNotFoundWtf = true;
            System.loadLibrary("android_servers");
            initZygoteChildHeapProfiling();
            if (Build.IS_DEBUGGABLE) {
                spawnFdLeakCheckThread();
            }
            performPendingShutdown();
            createSystemContext();
            ActivityThread.initializeMainlineModules();
            ServiceManager.addService("system_server_dumper", this.mDumper);
            this.mDumper.addDumpable(this);
            SystemServiceManager systemServiceManager = new SystemServiceManager(this.mSystemContext);
            this.mSystemServiceManager = systemServiceManager;
            systemServiceManager.setStartInfo(this.mRuntimeRestart, this.mRuntimeStartElapsedTime, this.mRuntimeStartUptime);
            this.mDumper.addDumpable(this.mSystemServiceManager);
            LocalServices.addService(SystemServiceManager.class, this.mSystemServiceManager);
            ITranSystemServer.Instance().onSystemServiceManagerCreated(this);
            SystemServerInitThreadPool tp = SystemServerInitThreadPool.start();
            this.mDumper.addDumpable(tp);
            Typeface.loadPreinstalledSystemFontMap();
            if (Build.IS_DEBUGGABLE) {
                String jvmtiAgent = SystemProperties.get("persist.sys.dalvik.jvmtiagent");
                if (!jvmtiAgent.isEmpty()) {
                    int equalIndex = jvmtiAgent.indexOf(61);
                    String libraryPath = jvmtiAgent.substring(0, equalIndex);
                    String parameterList = jvmtiAgent.substring(equalIndex + 1, jvmtiAgent.length());
                    try {
                        Debug.attachJvmtiAgent(libraryPath, parameterList, null);
                    } catch (Exception e) {
                        Slog.e("System", "*************************************************");
                        Slog.e("System", "********** Failed to load jvmti plugin: " + jvmtiAgent);
                    }
                }
            }
            t.traceEnd();
            sMtkSystemServerIns.setPrameters(BOOT_TIMINGS_TRACE_LOG, this.mSystemServiceManager, this.mSystemContext);
            RuntimeInit.setDefaultApplicationWtfHandler(new RuntimeInit.ApplicationWtfHandler() { // from class: com.android.server.SystemServer$$ExternalSyntheticLambda4
                public final boolean handleApplicationWtf(IBinder iBinder, String str, boolean z, ApplicationErrorReport.ParcelableCrashInfo parcelableCrashInfo, int i) {
                    boolean handleEarlySystemWtf;
                    handleEarlySystemWtf = SystemServer.handleEarlySystemWtf(iBinder, str, z, parcelableCrashInfo, i);
                    return handleEarlySystemWtf;
                }
            });
            try {
                t.traceBegin("StartServices");
                startBootstrapServices(t);
                sMtkSystemServerIns.startMtkBootstrapServices();
                startCoreServices(t);
                sMtkSystemServerIns.startMtkCoreServices();
                startOtherServices(t);
                startApexServices(t);
                t.traceEnd();
                StrictMode.initVmDefaults(null);
                if (!this.mRuntimeRestart && !isFirstBootOrUpgrade()) {
                    long uptimeMillis2 = SystemClock.elapsedRealtime();
                    FrameworkStatsLog.write((int) FrameworkStatsLog.BOOT_TIME_EVENT_ELAPSED_TIME_REPORTED, 20, uptimeMillis2);
                    if (uptimeMillis2 > 60000) {
                        Slog.wtf(TimingsTraceAndSlog.SYSTEM_SERVER_TIMING_TAG, "SystemServer init took too long. uptimeMillis=" + uptimeMillis2);
                    }
                }
                sMtkSystemServerIns.addBootEvent("Android:SysServerInit_END");
                Looper.loop();
                throw new RuntimeException("Main thread loop unexpectedly exited");
            } finally {
            }
        } finally {
        }
    }

    private static boolean isValidTimeZoneId(String timezoneProperty) {
        return (timezoneProperty == null || timezoneProperty.isEmpty() || !ZoneInfoDb.getInstance().hasTimeZone(timezoneProperty)) ? false : true;
    }

    private boolean isFirstBootOrUpgrade() {
        return this.mPackageManagerService.isFirstBoot() || this.mPackageManagerService.isDeviceUpgrading();
    }

    private void reportWtf(String msg, Throwable e) {
        Slog.w(TAG, "***********************************************");
        Slog.wtf(TAG, "BOOT FAILURE " + msg, e);
    }

    private void performPendingShutdown() {
        final String reason;
        String shutdownAction = SystemProperties.get(ShutdownThread.SHUTDOWN_ACTION_PROPERTY, "");
        if (shutdownAction != null && shutdownAction.length() > 0) {
            final boolean reboot = shutdownAction.charAt(0) == '1';
            if (shutdownAction.length() > 1) {
                reason = shutdownAction.substring(1, shutdownAction.length());
            } else {
                reason = null;
            }
            if (reason != null && reason.startsWith("recovery-update")) {
                File packageFile = new File(UNCRYPT_PACKAGE_FILE);
                if (packageFile.exists()) {
                    String filename = null;
                    try {
                        filename = FileUtils.readTextFile(packageFile, 0, null);
                    } catch (IOException e) {
                        Slog.e(TAG, "Error reading uncrypt package file", e);
                    }
                    if (filename != null && filename.startsWith("/data") && !new File(BLOCK_MAP_FILE).exists()) {
                        Slog.e(TAG, "Can't find block map file, uncrypt failed or unexpected runtime restart?");
                        return;
                    }
                }
            }
            Runnable runnable = new Runnable() { // from class: com.android.server.SystemServer.1
                @Override // java.lang.Runnable
                public void run() {
                    synchronized (this) {
                        ShutdownThread.rebootOrShutdown(null, reboot, reason);
                    }
                }
            };
            Message msg = Message.obtain(UiThread.getHandler(), runnable);
            msg.setAsynchronous(true);
            UiThread.getHandler().sendMessage(msg);
        }
    }

    private void createSystemContext() {
        ActivityThread activityThread = ActivityThread.systemMain();
        ContextImpl systemContext = activityThread.getSystemContext();
        this.mSystemContext = systemContext;
        systemContext.setTheme(DEFAULT_SYSTEM_THEME);
        activityThread.getSystemUiContext().setTheme(DEFAULT_SYSTEM_THEME);
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE, INVOKE, INVOKE, MOVE_EXCEPTION, CONST_STR, INVOKE, INVOKE, INVOKE, INVOKE, MOVE_EXCEPTION] complete} */
    /* JADX WARN: Type inference failed for: r5v2, types: [com.android.server.compat.PlatformCompat, android.os.IBinder] */
    private void startBootstrapServices(TimingsTraceAndSlog t) {
        t.traceBegin("startBootstrapServices");
        t.traceBegin("StartWatchdog");
        Watchdog watchdog = Watchdog.getInstance();
        watchdog.start();
        this.mDumper.addDumpable(watchdog);
        t.traceEnd();
        Slog.i(TAG, "Reading configuration...");
        t.traceBegin("ReadingSystemConfig");
        SystemServerInitThreadPool.submit(new Runnable() { // from class: com.android.server.SystemServer$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                SystemConfig.getInstance();
            }
        }, "ReadingSystemConfig");
        t.traceEnd();
        t.traceBegin("PlatformCompat");
        ?? platformCompat = new PlatformCompat(this.mSystemContext);
        ServiceManager.addService("platform_compat", (IBinder) platformCompat);
        ServiceManager.addService("platform_compat_native", new PlatformCompatNative(platformCompat));
        AppCompatCallbacks.install(new long[0]);
        t.traceEnd();
        t.traceBegin("StartFileIntegrityService");
        this.mSystemServiceManager.startService(FileIntegrityService.class);
        t.traceEnd();
        t.traceBegin("StartInstaller");
        Installer installer = (Installer) this.mSystemServiceManager.startService(Installer.class);
        t.traceEnd();
        t.traceBegin("DeviceIdentifiersPolicyService");
        this.mSystemServiceManager.startService(DeviceIdentifiersPolicyService.class);
        t.traceEnd();
        t.traceBegin("UriGrantsManagerService");
        this.mSystemServiceManager.startService(UriGrantsManagerService.Lifecycle.class);
        t.traceEnd();
        t.traceBegin("StartPowerStatsService");
        this.mSystemServiceManager.startService(PowerStatsService.class);
        t.traceEnd();
        t.traceBegin("StartIStatsService");
        startIStatsService();
        t.traceEnd();
        t.traceBegin("MemtrackProxyService");
        startMemtrackProxyService();
        t.traceEnd();
        t.traceBegin("StartActivityManager");
        ActivityTaskManagerService atm = ((ActivityTaskManagerService.Lifecycle) this.mSystemServiceManager.startService(ActivityTaskManagerService.Lifecycle.class)).getService();
        ActivityManagerService startService = ActivityManagerService.Lifecycle.startService(this.mSystemServiceManager, atm);
        this.mActivityManagerService = startService;
        startService.setSystemServiceManager(this.mSystemServiceManager);
        this.mActivityManagerService.setInstaller(installer);
        this.mWindowManagerGlobalLock = atm.getGlobalLock();
        t.traceEnd();
        t.traceBegin("StartDataLoaderManagerService");
        this.mDataLoaderManagerService = (DataLoaderManagerService) this.mSystemServiceManager.startService(DataLoaderManagerService.class);
        t.traceEnd();
        t.traceBegin("StartIncrementalService");
        this.mIncrementalServiceHandle = startIncrementalService();
        t.traceEnd();
        t.traceBegin("StartPowerManager");
        this.mPowerManagerService = (PowerManagerService) this.mSystemServiceManager.startService(PowerManagerService.class);
        t.traceEnd();
        t.traceBegin("StartThermalManager");
        this.mSystemServiceManager.startService(ThermalManagerService.class);
        t.traceEnd();
        t.traceBegin("StartHintManager");
        this.mSystemServiceManager.startService(HintManagerService.class);
        t.traceEnd();
        t.traceBegin("InitPowerManagement");
        this.mActivityManagerService.initPowerManagement();
        t.traceEnd();
        t.traceBegin("StartRecoverySystemService");
        this.mSystemServiceManager.startService(RecoverySystemService.Lifecycle.class);
        t.traceEnd();
        RescueParty.registerHealthObserver(this.mSystemContext);
        PackageWatchdog.getInstance(this.mSystemContext).noteBoot();
        t.traceBegin("StartLightsService");
        this.mSystemServiceManager.startService(LightsService.class);
        t.traceEnd();
        t.traceBegin("StartDisplayOffloadService");
        if (SystemProperties.getBoolean("config.enable_display_offload", false)) {
            this.mSystemServiceManager.startService(WEAR_DISPLAYOFFLOAD_SERVICE_CLASS);
        }
        t.traceEnd();
        t.traceBegin("StartSidekickService");
        if (SystemProperties.getBoolean("config.enable_sidekick_graphics", false)) {
            this.mSystemServiceManager.startService(WEAR_SIDEKICK_SERVICE_CLASS);
        }
        t.traceEnd();
        t.traceBegin("StartDisplayManager");
        this.mDisplayManagerService = (DisplayManagerService) this.mSystemServiceManager.startService(DisplayManagerService.class);
        t.traceEnd();
        t.traceBegin("WaitForDisplay");
        this.mSystemServiceManager.startBootPhase(t, 100);
        t.traceEnd();
        String cryptState = (String) VoldProperties.decrypt().orElse("");
        boolean z = true;
        if (ENCRYPTING_STATE.equals(cryptState)) {
            Slog.w(TAG, "Detected encryption in progress - only parsing core apps");
            this.mOnlyCore = true;
        } else if (ENCRYPTED_STATE.equals(cryptState)) {
            Slog.w(TAG, "Device encrypted - only parsing core apps");
            this.mOnlyCore = true;
        }
        if (!this.mRuntimeRestart) {
            FrameworkStatsLog.write((int) FrameworkStatsLog.BOOT_TIME_EVENT_ELAPSED_TIME_REPORTED, 14, SystemClock.elapsedRealtime());
        }
        t.traceBegin("StartDomainVerificationService");
        DomainVerificationService domainVerificationService = new DomainVerificationService(this.mSystemContext, SystemConfig.getInstance(), platformCompat);
        this.mSystemServiceManager.startService(domainVerificationService);
        t.traceEnd();
        t.traceBegin("StartPackageManagerService");
        try {
            Watchdog.getInstance().pauseWatchingCurrentThread("packagemanagermain");
            Context context = this.mSystemContext;
            if (this.mFactoryTestMode == 0) {
                z = false;
            }
            Pair<PackageManagerService, IPackageManager> pmsPair = PackageManagerService.main(context, installer, domainVerificationService, z, this.mOnlyCore);
            this.mPackageManagerService = (PackageManagerService) pmsPair.first;
            IPackageManager iPackageManager = (IPackageManager) pmsPair.second;
            Watchdog.getInstance().resumeWatchingCurrentThread("packagemanagermain");
            SystemServerDexLoadReporter.configureSystemServerDexReporter(iPackageManager);
            this.mFirstBoot = this.mPackageManagerService.isFirstBoot();
            this.mPackageManager = this.mSystemContext.getPackageManager();
            t.traceEnd();
            if (!this.mRuntimeRestart && !isFirstBootOrUpgrade()) {
                FrameworkStatsLog.write((int) FrameworkStatsLog.BOOT_TIME_EVENT_ELAPSED_TIME_REPORTED, 15, SystemClock.elapsedRealtime());
            }
            if (!this.mOnlyCore) {
                boolean disableOtaDexopt = SystemProperties.getBoolean("config.disable_otadexopt", false);
                if (!disableOtaDexopt) {
                    t.traceBegin("StartOtaDexOptService");
                    try {
                        Watchdog.getInstance().pauseWatchingCurrentThread("moveab");
                        OtaDexoptService.main(this.mSystemContext, this.mPackageManagerService);
                    } finally {
                        try {
                        } finally {
                        }
                    }
                }
            }
            t.traceBegin("StartUserManagerService");
            this.mSystemServiceManager.startService(UserManagerService.LifeCycle.class);
            t.traceEnd();
            t.traceBegin("InitAttributerCache");
            AttributeCache.init(this.mSystemContext);
            t.traceEnd();
            t.traceBegin("SetSystemProcess");
            this.mActivityManagerService.setSystemProcess();
            t.traceEnd();
            platformCompat.registerPackageReceiver(this.mSystemContext);
            t.traceBegin("InitWatchdog");
            watchdog.init(this.mSystemContext, this.mActivityManagerService);
            t.traceEnd();
            this.mDisplayManagerService.setupSchedulerPolicies();
            this.mPackageManagerService.onAmsAddedtoServiceMgr();
            t.traceBegin("StartOverlayManagerService");
            this.mSystemServiceManager.startService(new OverlayManagerService(this.mSystemContext));
            t.traceEnd();
            t.traceBegin("StartResourcesManagerService");
            ResourcesManagerService resourcesService = new ResourcesManagerService(this.mSystemContext);
            resourcesService.setActivityManagerService(this.mActivityManagerService);
            this.mSystemServiceManager.startService(resourcesService);
            t.traceEnd();
            t.traceBegin("StartSensorPrivacyService");
            this.mSystemServiceManager.startService(new SensorPrivacyService(this.mSystemContext));
            t.traceEnd();
            if (SystemProperties.getInt("persist.sys.displayinset.top", 0) > 0) {
                this.mActivityManagerService.updateSystemUiContext();
                ((DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class)).onOverlayChanged();
            }
            t.traceBegin("StartSensorService");
            this.mSystemServiceManager.startService(SensorService.class);
            t.traceEnd();
            t.traceEnd();
        } catch (Throwable th) {
            Watchdog.getInstance().resumeWatchingCurrentThread("packagemanagermain");
            throw th;
        }
    }

    private void startCoreServices(TimingsTraceAndSlog t) {
        t.traceBegin("startCoreServices");
        t.traceBegin("StartSystemConfigService");
        this.mSystemServiceManager.startService(SystemConfigService.class);
        t.traceEnd();
        t.traceBegin("StartBatteryService");
        this.mSystemServiceManager.startService(BatteryService.class);
        t.traceEnd();
        t.traceBegin("StartUsageService");
        this.mSystemServiceManager.startService(UsageStatsService.class);
        this.mActivityManagerService.setUsageStatsManager((UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class));
        t.traceEnd();
        if (this.mPackageManager.hasSystemFeature("android.software.webview")) {
            t.traceBegin("StartWebViewUpdateService");
            this.mWebViewUpdateService = (WebViewUpdateService) this.mSystemServiceManager.startService(WebViewUpdateService.class);
            t.traceEnd();
        }
        t.traceBegin("StartCachedDeviceStateService");
        this.mSystemServiceManager.startService(CachedDeviceStateService.class);
        t.traceEnd();
        t.traceBegin("StartBinderCallsStatsService");
        this.mSystemServiceManager.startService(BinderCallsStatsService.LifeCycle.class);
        t.traceEnd();
        t.traceBegin("StartLooperStatsService");
        this.mSystemServiceManager.startService(LooperStatsService.Lifecycle.class);
        t.traceEnd();
        t.traceBegin("StartRollbackManagerService");
        this.mSystemServiceManager.startService(ROLLBACK_MANAGER_SERVICE_CLASS);
        t.traceEnd();
        t.traceBegin("StartNativeTombstoneManagerService");
        this.mSystemServiceManager.startService(NativeTombstoneManagerService.class);
        t.traceEnd();
        t.traceBegin("StartBugreportManagerService");
        this.mSystemServiceManager.startService(BugreportManagerService.class);
        t.traceEnd();
        t.traceBegin(GpuService.TAG);
        this.mSystemServiceManager.startService(GpuService.class);
        t.traceEnd();
        t.traceEnd();
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE, CONST_STR, INVOKE, MOVE_EXCEPTION, CONST_STR, INVOKE, INVOKE, CONST_STR, INVOKE, MOVE_EXCEPTION] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1755=6] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:604:0x0716 */
    /* JADX WARN: Code restructure failed: missing block: B:14:0x00fa, code lost:
        if (r62.mPackageManager.hasSystemFeature("android.hardware.telephony") != false) goto L11;
     */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:261:0x091c  */
    /* JADX WARN: Removed duplicated region for block: B:268:0x0935  */
    /* JADX WARN: Removed duplicated region for block: B:276:0x0959  */
    /* JADX WARN: Removed duplicated region for block: B:277:0x0969  */
    /* JADX WARN: Removed duplicated region for block: B:280:0x0986  */
    /* JADX WARN: Removed duplicated region for block: B:281:0x0990  */
    /* JADX WARN: Removed duplicated region for block: B:293:0x09ee  */
    /* JADX WARN: Removed duplicated region for block: B:296:0x0a0e  */
    /* JADX WARN: Removed duplicated region for block: B:298:0x0a1f  */
    /* JADX WARN: Removed duplicated region for block: B:306:0x0a41  */
    /* JADX WARN: Removed duplicated region for block: B:314:0x0a72  */
    /* JADX WARN: Removed duplicated region for block: B:319:0x0a8f  */
    /* JADX WARN: Removed duplicated region for block: B:340:0x0ad9  */
    /* JADX WARN: Removed duplicated region for block: B:343:0x0b2e  */
    /* JADX WARN: Removed duplicated region for block: B:346:0x0b47  */
    /* JADX WARN: Removed duplicated region for block: B:351:0x0b8b  */
    /* JADX WARN: Removed duplicated region for block: B:354:0x0bb3  */
    /* JADX WARN: Removed duplicated region for block: B:367:0x0bfb  */
    /* JADX WARN: Removed duplicated region for block: B:373:0x0c11  */
    /* JADX WARN: Removed duplicated region for block: B:375:0x0c22 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:393:0x0ca2  */
    /* JADX WARN: Removed duplicated region for block: B:396:0x0cbe  */
    /* JADX WARN: Removed duplicated region for block: B:399:0x0ce6  */
    /* JADX WARN: Removed duplicated region for block: B:402:0x0d2c  */
    /* JADX WARN: Removed duplicated region for block: B:405:0x0d45  */
    /* JADX WARN: Removed duplicated region for block: B:410:0x0d68  */
    /* JADX WARN: Removed duplicated region for block: B:415:0x0d8b  */
    /* JADX WARN: Removed duplicated region for block: B:418:0x0da4  */
    /* JADX WARN: Removed duplicated region for block: B:421:0x0dbd  */
    /* JADX WARN: Removed duplicated region for block: B:433:0x0e0a  */
    /* JADX WARN: Removed duplicated region for block: B:434:0x0e1f  */
    /* JADX WARN: Removed duplicated region for block: B:436:0x0e23  */
    /* JADX WARN: Removed duplicated region for block: B:438:0x0e34  */
    /* JADX WARN: Removed duplicated region for block: B:441:0x0e66  */
    /* JADX WARN: Removed duplicated region for block: B:448:0x0e7d  */
    /* JADX WARN: Removed duplicated region for block: B:457:0x0f05  */
    /* JADX WARN: Removed duplicated region for block: B:463:0x0f7a  */
    /* JADX WARN: Removed duplicated region for block: B:466:0x0f95  */
    /* JADX WARN: Removed duplicated region for block: B:469:0x1013  */
    /* JADX WARN: Removed duplicated region for block: B:472:0x1022  */
    /* JADX WARN: Removed duplicated region for block: B:473:0x1039  */
    /* JADX WARN: Removed duplicated region for block: B:476:0x1045  */
    /* JADX WARN: Removed duplicated region for block: B:479:0x105d  */
    /* JADX WARN: Removed duplicated region for block: B:480:0x106d  */
    /* JADX WARN: Removed duplicated region for block: B:542:0x12af  */
    /* JADX WARN: Removed duplicated region for block: B:554:0x12da  */
    /* JADX WARN: Removed duplicated region for block: B:620:0x10ae A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:720:0x10f4 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Type inference failed for: r0v262, types: [com.android.server.media.MediaRouterService, android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r0v314, types: [com.android.server.SerialService] */
    /* JADX WARN: Type inference failed for: r0v380, types: [com.android.server.statusbar.StatusBarManagerService] */
    /* JADX WARN: Type inference failed for: r0v496, types: [com.transsion.server.TranPowerhubService, android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r0v502, types: [com.transsion.server.TranAipowerlabService, android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r10v14, types: [com.android.server.VcnManagementService] */
    /* JADX WARN: Type inference failed for: r11v10, types: [com.android.server.CountryDetectorService] */
    /* JADX WARN: Type inference failed for: r15v2, types: [com.android.server.net.NetworkPolicyManagerService] */
    /* JADX WARN: Type inference failed for: r15v6 */
    /* JADX WARN: Type inference failed for: r15v7 */
    /* JADX WARN: Type inference failed for: r6v14, types: [android.os.IBinder, com.android.server.wm.WindowManagerService] */
    /* JADX WARN: Type inference failed for: r7v39, types: [com.android.server.input.InputManagerService, android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r7v53, types: [com.android.server.am.ActivityManagerService] */
    /* JADX WARN: Type inference failed for: r9v23, types: [com.android.server.TelephonyRegistry, android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r9v34, types: [com.android.server.VpnManagerService] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void startOtherServices(final TimingsTraceAndSlog t) {
        IConsumerIrService.Stub stub;
        DevicePolicyManagerService.Lifecycle dpms;
        ?? r15;
        boolean startRulesManagerService;
        boolean hasFeatureFace;
        boolean hasFeatureIris;
        boolean hasFeatureFingerprint;
        ICountryDetector.Stub stub2;
        INetworkManagementService iNetworkManagementService;
        IVpnManager.Stub stub3;
        IVcnManagementService.Stub stub4;
        NetworkPolicyManagerService networkPolicy;
        ILockSettings lockSettings;
        NetworkTimeUpdateService networkTimeUpdater;
        MediaRouterService mediaRouter;
        ?? mediaRouterService;
        NetworkTimeUpdateService networkTimeUpdater2;
        IHardwarePropertiesManager.Stub hardwarePropertiesManagerService;
        IBinder iBinder;
        MmsServiceBroker mmsService;
        t.traceBegin("startOtherServices");
        this.mSystemServiceManager.updateOtherServicesStartIndex();
        final Context context = this.mSystemContext;
        IStorageManager storageManager = null;
        INetworkManagementService iNetworkManagementService2 = null;
        IVpnManager.Stub stub5 = null;
        IVcnManagementService.Stub stub6 = null;
        INetworkPolicyManager.Stub stub7 = null;
        IBinder iBinder2 = null;
        NetworkTimeUpdateService networkTimeUpdater3 = null;
        IHardwarePropertiesManager.Stub stub8 = null;
        IPacProxyManager.Stub stub9 = null;
        TranAipowerlabService tranAipowerlabService = null;
        TranPowerhubService tranPowerhubService = null;
        ITranSystemServer.Instance().startDefaultFileBackup(context);
        boolean disableSystemTextClassifier = SystemProperties.getBoolean("config.disable_systemtextclassifier", false);
        boolean disableNetworkTime = SystemProperties.getBoolean("config.disable_networktime", false);
        boolean disableCameraService = SystemProperties.getBoolean("config.disable_cameraservice", false);
        boolean enableLeftyService = SystemProperties.getBoolean("config.enable_lefty", false);
        boolean isEmulator = SystemProperties.get("ro.boot.qemu").equals(ENCRYPTED_STATE);
        boolean isWatch = context.getPackageManager().hasSystemFeature("android.hardware.type.watch");
        boolean isArc = context.getPackageManager().hasSystemFeature("org.chromium.arc");
        boolean enableVrService = context.getPackageManager().hasSystemFeature("android.hardware.vr.high_performance");
        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean("debug.crash_system", false)) {
            throw new RuntimeException();
        }
        try {
            this.mZygotePreload = SystemServerInitThreadPool.submit(new Runnable() { // from class: com.android.server.SystemServer$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    SystemServer.lambda$startOtherServices$1();
                }
            }, "SecondaryZygotePreload");
            t.traceBegin("StartKeyAttestationApplicationIdProviderService");
            ServiceManager.addService("sec_key_att_app_id_provider", new KeyAttestationApplicationIdProviderService(context));
            t.traceEnd();
            t.traceBegin("StartKeyChainSystemService");
            this.mSystemServiceManager.startService(KeyChainSystemService.class);
            t.traceEnd();
            t.traceBegin("StartBinaryTransparencyService");
            this.mSystemServiceManager.startService(BinaryTransparencyService.class);
            t.traceEnd();
            t.traceBegin("StartSchedulingPolicyService");
            ServiceManager.addService("scheduling_policy", new SchedulingPolicyService());
            t.traceEnd();
            if (!this.mPackageManager.hasSystemFeature("android.hardware.microphone")) {
                try {
                    if (!this.mPackageManager.hasSystemFeature("android.software.telecom")) {
                    }
                } catch (Throwable th) {
                    e = th;
                    Slog.e("System", "******************************************");
                    Slog.e("System", "************ Failure starting core service");
                    throw e;
                }
            }
            t.traceBegin("StartTelecomLoaderService");
            this.mSystemServiceManager.startService(TelecomLoaderService.class);
            t.traceEnd();
            t.traceBegin("StartTelephonyRegistry");
            final ?? telephonyRegistry = new TelephonyRegistry(context, new TelephonyRegistry.ConfigurationProvider());
            try {
                ServiceManager.addService("telephony.registry", (IBinder) telephonyRegistry);
                t.traceEnd();
                t.traceBegin("StartEntropyMixer");
                this.mEntropyMixer = new EntropyMixer(context);
                t.traceEnd();
                this.mContentResolver = context.getContentResolver();
                t.traceBegin("StartAccountManagerService");
                this.mSystemServiceManager.startService(ACCOUNT_SERVICE_CLASS);
                t.traceEnd();
                t.traceBegin("StartContentService");
                this.mSystemServiceManager.startService(CONTENT_SERVICE_CLASS);
                t.traceEnd();
                t.traceBegin("InstallSystemProviders");
                this.mActivityManagerService.getContentProviderHelper().installSystemProviders();
                SQLiteCompatibilityWalFlags.reset();
                t.traceEnd();
                t.traceBegin("UpdateWatchdogTimeout");
                Watchdog.getInstance().registerSettingsObserver(context);
                t.traceEnd();
                t.traceBegin("StartDropBoxManager");
                this.mSystemServiceManager.startService(DropBoxManagerService.class);
                t.traceEnd();
                t.traceBegin("StartRoleManagerService");
                LocalManagerRegistry.addManager(RoleServicePlatformHelper.class, new RoleServicePlatformHelperImpl(this.mSystemContext));
                this.mSystemServiceManager.startService(ROLE_SERVICE_CLASS);
                t.traceEnd();
                t.traceBegin("StartVibratorManagerService");
                this.mSystemServiceManager.startService(VibratorManagerService.Lifecycle.class);
                t.traceEnd();
                t.traceBegin("StartDynamicSystemService");
                try {
                    ServiceManager.addService("dynamic_system", new DynamicSystemService(context));
                    t.traceEnd();
                    if (isWatch) {
                        stub = null;
                    } else {
                        try {
                            t.traceBegin("StartConsumerIrService");
                            IConsumerIrService.Stub consumerIrService = new ConsumerIrService(context);
                            ServiceManager.addService("consumer_ir", consumerIrService);
                            t.traceEnd();
                            stub = consumerIrService;
                        } catch (Throwable th2) {
                            e = th2;
                            Slog.e("System", "******************************************");
                            Slog.e("System", "************ Failure starting core service");
                            throw e;
                        }
                    }
                    try {
                        t.traceBegin("StartResourceEconomy");
                        this.mSystemServiceManager.startService(RESOURCE_ECONOMY_SERVICE_CLASS);
                        t.traceEnd();
                        t.traceBegin("StartAlarmManagerService");
                        if (!sMtkSystemServerIns.startMtkAlarmManagerService()) {
                            try {
                                this.mSystemServiceManager.startService(ALARM_MANAGER_SERVICE_CLASS);
                            } catch (Throwable th3) {
                                e = th3;
                                Slog.e("System", "******************************************");
                                Slog.e("System", "************ Failure starting core service");
                                throw e;
                            }
                        }
                        t.traceEnd();
                        t.traceBegin("StartInputManagerService");
                        final ?? inputManagerService = new InputManagerService(context);
                        try {
                            t.traceEnd();
                            t.traceBegin("DeviceStateManagerService");
                            this.mSystemServiceManager.startService(DeviceStateManagerService.class);
                            t.traceEnd();
                            if (!disableCameraService) {
                                try {
                                    t.traceBegin("StartCameraServiceProxy");
                                    this.mSystemServiceManager.startService(CameraServiceProxy.class);
                                    t.traceEnd();
                                } catch (Throwable th4) {
                                    e = th4;
                                    Slog.e("System", "******************************************");
                                    Slog.e("System", "************ Failure starting core service");
                                    throw e;
                                }
                            }
                            t.traceBegin("StartWindowManagerService");
                            this.mSystemServiceManager.startBootPhase(t, 200);
                            try {
                                ?? main = WindowManagerService.main(context, inputManagerService, !this.mFirstBoot, this.mOnlyCore, new PhoneWindowManager(), this.mActivityManagerService.mActivityTaskManager);
                                try {
                                    ServiceManager.addService("window", (IBinder) main, false, 17);
                                    try {
                                        ServiceManager.addService("input", (IBinder) inputManagerService, false, 1);
                                        t.traceEnd();
                                        t.traceBegin("SetWindowManagerService");
                                        this.mActivityManagerService.setWindowManager(main);
                                        t.traceEnd();
                                        t.traceBegin("WindowManagerServiceOnInitReady");
                                        main.onInitReady();
                                        t.traceEnd();
                                        SystemServerInitThreadPool.submit(new Runnable() { // from class: com.android.server.SystemServer$$ExternalSyntheticLambda6
                                            @Override // java.lang.Runnable
                                            public final void run() {
                                                SystemServer.lambda$startOtherServices$2();
                                            }
                                        }, START_HIDL_SERVICES);
                                        if (!isWatch && enableVrService) {
                                            try {
                                                t.traceBegin("StartVrManagerService");
                                                this.mSystemServiceManager.startService(VrManagerService.class);
                                                t.traceEnd();
                                            } catch (Throwable th5) {
                                                e = th5;
                                                Slog.e("System", "******************************************");
                                                Slog.e("System", "************ Failure starting core service");
                                                throw e;
                                            }
                                        }
                                        t.traceBegin("StartInputManager");
                                        inputManagerService.setWindowManagerCallbacks(main.getInputManagerCallback());
                                        inputManagerService.start();
                                        t.traceEnd();
                                        t.traceBegin("DisplayManagerWindowManagerAndInputReady");
                                        this.mDisplayManagerService.windowManagerAndInputReady();
                                        t.traceEnd();
                                        if (this.mFactoryTestMode == 1) {
                                            Slog.i(TAG, "No Bluetooth Service (factory test)");
                                        } else if (context.getPackageManager().hasSystemFeature("android.hardware.bluetooth")) {
                                            t.traceBegin("StartBluetoothService");
                                            this.mSystemServiceManager.startServiceFromJar(BLUETOOTH_SERVICE_CLASS, BLUETOOTH_APEX_SERVICE_JAR_PATH);
                                            t.traceEnd();
                                        } else {
                                            Slog.i(TAG, "No Bluetooth Service (Bluetooth Hardware Not Present)");
                                        }
                                        t.traceBegin("IpConnectivityMetrics");
                                        this.mSystemServiceManager.startService(IP_CONNECTIVITY_METRICS_CLASS);
                                        t.traceEnd();
                                        t.traceBegin("NetworkWatchlistService");
                                        this.mSystemServiceManager.startService(NetworkWatchlistService.Lifecycle.class);
                                        t.traceEnd();
                                        t.traceBegin("PinnerService");
                                        this.mSystemServiceManager.startService(PinnerService.class);
                                        t.traceEnd();
                                        if (Build.IS_DEBUGGABLE && ProfcollectForwardingService.enabled()) {
                                            t.traceBegin(ProfcollectForwardingService.LOG_TAG);
                                            this.mSystemServiceManager.startService(ProfcollectForwardingService.class);
                                            t.traceEnd();
                                        }
                                        t.traceBegin("SignedConfigService");
                                        SignedConfigService.registerUpdateReceiver(this.mSystemContext);
                                        t.traceEnd();
                                        t.traceBegin("AppIntegrityService");
                                        this.mSystemServiceManager.startService(AppIntegrityManagerService.class);
                                        t.traceEnd();
                                        t.traceBegin("StartLogcatManager");
                                        this.mSystemServiceManager.startService(LogcatManagerService.class);
                                        t.traceEnd();
                                        final boolean safeMode = main.detectSafeMode();
                                        if (safeMode) {
                                            Settings.Global.putInt(context.getContentResolver(), "airplane_mode_on", 1);
                                        } else if (context.getResources().getBoolean(17891377)) {
                                            Settings.Global.putInt(context.getContentResolver(), "airplane_mode_on", 0);
                                        }
                                        IBinder iBinder3 = null;
                                        ICountryDetector.Stub stub10 = null;
                                        ILockSettings lockSettings2 = null;
                                        MediaRouterService mediaRouter2 = null;
                                        if (this.mFactoryTestMode != 1) {
                                            t.traceBegin("StartInputMethodManagerLifecycle");
                                            this.mSystemServiceManager.startService(InputMethodManagerService.Lifecycle.class);
                                            t.traceEnd();
                                            t.traceBegin("StartAccessibilityManagerService");
                                            try {
                                                this.mSystemServiceManager.startService(ACCESSIBILITY_MANAGER_SERVICE_CLASS);
                                            } catch (Throwable e) {
                                                reportWtf("starting Accessibility Manager", e);
                                            }
                                            t.traceEnd();
                                        }
                                        t.traceBegin("MakeDisplayReady");
                                        try {
                                            main.displayReady();
                                        } catch (Throwable e2) {
                                            reportWtf("making display ready", e2);
                                        }
                                        t.traceEnd();
                                        if (this.mFactoryTestMode != 1 && !"0".equals(SystemProperties.get("system_init.startmountservice"))) {
                                            t.traceBegin("StartStorageManagerService");
                                            try {
                                                if (!sMtkSystemServerIns.startMtkStorageManagerService()) {
                                                    this.mSystemServiceManager.startService(STORAGE_MANAGER_SERVICE_CLASS);
                                                }
                                                storageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
                                            } catch (Throwable e3) {
                                                reportWtf("starting StorageManagerService", e3);
                                            }
                                            t.traceEnd();
                                            t.traceBegin("StartStorageStatsService");
                                            try {
                                                this.mSystemServiceManager.startService(STORAGE_STATS_SERVICE_CLASS);
                                            } catch (Throwable e4) {
                                                reportWtf("starting StorageStatsService", e4);
                                            }
                                            t.traceEnd();
                                        }
                                        t.traceBegin("StartUiModeManager");
                                        this.mSystemServiceManager.startService(UiModeManagerService.class);
                                        t.traceEnd();
                                        t.traceBegin("StartLocaleManagerService");
                                        try {
                                            this.mSystemServiceManager.startService(LocaleManagerService.class);
                                        } catch (Throwable e5) {
                                            reportWtf("starting LocaleManagerService service", e5);
                                        }
                                        t.traceEnd();
                                        if (!this.mOnlyCore) {
                                            t.traceBegin("UpdatePackagesIfNeeded");
                                            try {
                                                Watchdog.getInstance().pauseWatchingCurrentThread("dexopt");
                                                this.mPackageManagerService.updatePackagesIfNeeded();
                                            } finally {
                                                try {
                                                    Watchdog.getInstance().resumeWatchingCurrentThread("dexopt");
                                                    t.traceEnd();
                                                } catch (Throwable th6) {
                                                }
                                            }
                                            Watchdog.getInstance().resumeWatchingCurrentThread("dexopt");
                                            t.traceEnd();
                                        }
                                        t.traceBegin("PerformFstrimIfNeeded");
                                        try {
                                            this.mPackageManagerService.performFstrimIfNeeded();
                                        } catch (Throwable e6) {
                                            reportWtf("performing fstrim", e6);
                                        }
                                        t.traceEnd();
                                        if (this.mFactoryTestMode == 1) {
                                            dpms = null;
                                            stub2 = null;
                                            iNetworkManagementService = null;
                                            stub3 = null;
                                            stub4 = null;
                                            networkPolicy = null;
                                            lockSettings = null;
                                            networkTimeUpdater = null;
                                            mediaRouter = null;
                                        } else {
                                            t.traceBegin("StartLockSettingsService");
                                            try {
                                                this.mSystemServiceManager.startService(LOCK_SETTINGS_SERVICE_CLASS);
                                                ILockSettings lockSettings3 = ILockSettings.Stub.asInterface(ServiceManager.getService("lock_settings"));
                                                lockSettings2 = lockSettings3;
                                            } catch (Throwable e7) {
                                                reportWtf("starting LockSettingsService service", e7);
                                            }
                                            t.traceEnd();
                                            boolean hasPdb = !SystemProperties.get(PERSISTENT_DATA_BLOCK_PROP).equals("");
                                            if (hasPdb) {
                                                t.traceBegin("StartPersistentDataBlock");
                                                this.mSystemServiceManager.startService(PersistentDataBlockService.class);
                                                t.traceEnd();
                                            }
                                            t.traceBegin("StartTestHarnessMode");
                                            this.mSystemServiceManager.startService(TestHarnessModeService.class);
                                            t.traceEnd();
                                            if (hasPdb || OemLockService.isHalPresent()) {
                                                t.traceBegin("StartOemLockService");
                                                this.mSystemServiceManager.startService(OemLockService.class);
                                                t.traceEnd();
                                            }
                                            t.traceBegin("StartDeviceIdleController");
                                            this.mSystemServiceManager.startService(DEVICE_IDLE_CONTROLLER_CLASS);
                                            t.traceEnd();
                                            t.traceBegin("StartDevicePolicyManager");
                                            DevicePolicyManagerService.Lifecycle dpms2 = (DevicePolicyManagerService.Lifecycle) this.mSystemServiceManager.startService(DevicePolicyManagerService.Lifecycle.class);
                                            t.traceEnd();
                                            if (isWatch) {
                                                dpms = dpms2;
                                            } else {
                                                t.traceBegin("StartStatusBarManagerService");
                                                try {
                                                    iBinder3 = new StatusBarManagerService(context);
                                                    ServiceManager.addService("statusbar", iBinder3);
                                                    dpms = dpms2;
                                                } catch (Throwable e8) {
                                                    dpms = dpms2;
                                                    reportWtf("starting StatusBarManagerService", e8);
                                                }
                                                t.traceEnd();
                                            }
                                            if (deviceHasConfigString(context, 17039930)) {
                                                t.traceBegin("StartMusicRecognitionManagerService");
                                                this.mSystemServiceManager.startService(MUSIC_RECOGNITION_MANAGER_SERVICE_CLASS);
                                                t.traceEnd();
                                            } else {
                                                Slog.d(TAG, "MusicRecognitionManagerService not defined by OEM or disabled by flag");
                                            }
                                            startContentCaptureService(context, t);
                                            startAttentionService(context, t);
                                            startRotationResolverService(context, t);
                                            startSystemCaptionsManagerService(context, t);
                                            startTextToSpeechManagerService(context, t);
                                            startAmbientContextService(t);
                                            t.traceBegin("StartSpeechRecognitionManagerService");
                                            this.mSystemServiceManager.startService(SPEECH_RECOGNITION_MANAGER_SERVICE_CLASS);
                                            t.traceEnd();
                                            if (deviceHasConfigString(context, 17039918)) {
                                                t.traceBegin("StartAppPredictionService");
                                                this.mSystemServiceManager.startService(APP_PREDICTION_MANAGER_SERVICE_CLASS);
                                                t.traceEnd();
                                            } else {
                                                Slog.d(TAG, "AppPredictionService not defined by OEM");
                                            }
                                            if (deviceHasConfigString(context, 17039926)) {
                                                t.traceBegin("StartContentSuggestionsService");
                                                this.mSystemServiceManager.startService(CONTENT_SUGGESTIONS_SERVICE_CLASS);
                                                t.traceEnd();
                                            } else {
                                                Slog.d(TAG, "ContentSuggestionsService not defined by OEM");
                                            }
                                            t.traceBegin("StartSearchUiService");
                                            this.mSystemServiceManager.startService(SEARCH_UI_MANAGER_SERVICE_CLASS);
                                            t.traceEnd();
                                            t.traceBegin("StartSmartspaceService");
                                            this.mSystemServiceManager.startService(SMARTSPACE_MANAGER_SERVICE_CLASS);
                                            t.traceEnd();
                                            t.traceBegin("StartCloudSearchService");
                                            this.mSystemServiceManager.startService(CLOUDSEARCH_MANAGER_SERVICE_CLASS);
                                            t.traceEnd();
                                            t.traceBegin("InitConnectivityModuleConnector");
                                            try {
                                                ConnectivityModuleConnector.getInstance().init(context);
                                            } catch (Throwable e9) {
                                                reportWtf("initializing ConnectivityModuleConnector", e9);
                                            }
                                            t.traceEnd();
                                            t.traceBegin("InitNetworkStackClient");
                                            try {
                                                NetworkStackClient.getInstance().init();
                                            } catch (Throwable e10) {
                                                reportWtf("initializing NetworkStackClient", e10);
                                            }
                                            t.traceEnd();
                                            t.traceBegin("StartNetworkManagementService");
                                            try {
                                                iNetworkManagementService2 = NetworkManagementService.create(context);
                                                ServiceManager.addService("network_management", iNetworkManagementService2);
                                            } catch (Throwable e11) {
                                                reportWtf("starting NetworkManagement Service", e11);
                                            }
                                            t.traceEnd();
                                            t.traceBegin("StartFontManagerService");
                                            this.mSystemServiceManager.startService(new FontManagerService.Lifecycle(context, safeMode));
                                            t.traceEnd();
                                            t.traceBegin("StartTextServicesManager");
                                            this.mSystemServiceManager.startService(TextServicesManagerService.Lifecycle.class);
                                            t.traceEnd();
                                            if (!disableSystemTextClassifier) {
                                                t.traceBegin("StartTextClassificationManagerService");
                                                this.mSystemServiceManager.startService(TextClassificationManagerService.Lifecycle.class);
                                                t.traceEnd();
                                            }
                                            t.traceBegin("StartNetworkScoreService");
                                            this.mSystemServiceManager.startService(NetworkScoreService.Lifecycle.class);
                                            t.traceEnd();
                                            t.traceBegin("StartNetworkStatsService");
                                            this.mSystemServiceManager.startServiceFromJar(NETWORK_STATS_SERVICE_INITIALIZER_CLASS, CONNECTIVITY_SERVICE_APEX_PATH);
                                            t.traceEnd();
                                            t.traceBegin("StartNetworkPolicyManagerService");
                                            try {
                                                stub7 = new NetworkPolicyManagerService(context, this.mActivityManagerService, iNetworkManagementService2);
                                                ServiceManager.addService("netpolicy", stub7);
                                                r15 = stub7;
                                            } catch (Throwable e12) {
                                                reportWtf("starting NetworkPolicy Service", e12);
                                                r15 = stub7;
                                            }
                                            t.traceEnd();
                                            if (context.getPackageManager().hasSystemFeature("android.hardware.wifi")) {
                                                t.traceBegin("StartWifi");
                                                this.mSystemServiceManager.startServiceFromJar(WIFI_SERVICE_CLASS, WIFI_APEX_SERVICE_JAR_PATH);
                                                t.traceEnd();
                                                t.traceBegin("StartWifiScanning");
                                                this.mSystemServiceManager.startServiceFromJar(WIFI_SCANNING_SERVICE_CLASS, WIFI_APEX_SERVICE_JAR_PATH);
                                                t.traceEnd();
                                            }
                                            if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt")) {
                                                t.traceBegin("StartRttService");
                                                this.mSystemServiceManager.startServiceFromJar(WIFI_RTT_SERVICE_CLASS, WIFI_APEX_SERVICE_JAR_PATH);
                                                t.traceEnd();
                                            }
                                            if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.aware")) {
                                                t.traceBegin("StartWifiAware");
                                                this.mSystemServiceManager.startServiceFromJar(WIFI_AWARE_SERVICE_CLASS, WIFI_APEX_SERVICE_JAR_PATH);
                                                t.traceEnd();
                                            }
                                            if (context.getPackageManager().hasSystemFeature("android.hardware.wifi.direct")) {
                                                t.traceBegin("StartWifiP2P");
                                                this.mSystemServiceManager.startServiceFromJar(WIFI_P2P_SERVICE_CLASS, WIFI_APEX_SERVICE_JAR_PATH);
                                                t.traceEnd();
                                            }
                                            if (context.getPackageManager().hasSystemFeature("android.hardware.lowpan")) {
                                                t.traceBegin("StartLowpan");
                                                this.mSystemServiceManager.startService(LOWPAN_SERVICE_CLASS);
                                                t.traceEnd();
                                            }
                                            t.traceBegin("StartPacProxyService");
                                            try {
                                                IPacProxyManager.Stub pacProxyService = new PacProxyService(context);
                                                try {
                                                    ServiceManager.addService("pac_proxy", pacProxyService);
                                                    stub9 = pacProxyService;
                                                } catch (Throwable th7) {
                                                    e = th7;
                                                    stub9 = pacProxyService;
                                                    reportWtf("starting PacProxyService", e);
                                                    t.traceEnd();
                                                    t.traceBegin("StartConnectivityService");
                                                    this.mSystemServiceManager.startServiceFromJar(CONNECTIVITY_SERVICE_INITIALIZER_CLASS, CONNECTIVITY_SERVICE_APEX_PATH);
                                                    r15.bindConnectivityManager();
                                                    t.traceEnd();
                                                    t.traceBegin("StartVpnManagerService");
                                                    stub5 = VpnManagerService.create(context);
                                                    ServiceManager.addService("vpn_management", stub5);
                                                    t.traceEnd();
                                                    t.traceBegin("StartVcnManagementService");
                                                    stub6 = VcnManagementService.create(context);
                                                    ServiceManager.addService("vcn_management", stub6);
                                                    t.traceEnd();
                                                    t.traceBegin("StartSystemUpdateManagerService");
                                                    ServiceManager.addService("system_update", new SystemUpdateManagerService(context));
                                                    t.traceEnd();
                                                    t.traceBegin("StartUpdateLockService");
                                                    ServiceManager.addService("updatelock", new UpdateLockService(context));
                                                    t.traceEnd();
                                                    t.traceBegin("StartNotificationManager");
                                                    this.mSystemServiceManager.startService(NotificationManagerService.class);
                                                    SystemNotificationChannels.removeDeprecated(context);
                                                    SystemNotificationChannels.createAll(context);
                                                    INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
                                                    t.traceEnd();
                                                    t.traceBegin("StartDeviceMonitor");
                                                    this.mSystemServiceManager.startService(DeviceStorageMonitorService.class);
                                                    t.traceEnd();
                                                    t.traceBegin("StartLocationManagerService");
                                                    this.mSystemServiceManager.startService(LocationManagerService.Lifecycle.class);
                                                    t.traceEnd();
                                                    t.traceBegin("StartCountryDetectorService");
                                                    stub10 = new CountryDetectorService(context);
                                                    ServiceManager.addService("country_detector", stub10);
                                                    t.traceEnd();
                                                    t.traceBegin("StartTimeDetectorService");
                                                    this.mSystemServiceManager.startService(TIME_DETECTOR_SERVICE_CLASS);
                                                    t.traceEnd();
                                                    t.traceBegin("StartTimeZoneDetectorService");
                                                    this.mSystemServiceManager.startService(TIME_ZONE_DETECTOR_SERVICE_CLASS);
                                                    t.traceEnd();
                                                    t.traceBegin("StartLocationTimeZoneManagerService");
                                                    this.mSystemServiceManager.startService(LOCATION_TIME_ZONE_MANAGER_SERVICE_CLASS);
                                                    t.traceEnd();
                                                    if (context.getResources().getBoolean(17891635)) {
                                                    }
                                                    if (!isWatch) {
                                                    }
                                                    if (context.getResources().getBoolean(17891651)) {
                                                    }
                                                    t.traceBegin("StartWallpaperEffectsGenerationService");
                                                    this.mSystemServiceManager.startService(WALLPAPER_EFFECTS_GENERATION_MANAGER_SERVICE_CLASS);
                                                    t.traceEnd();
                                                    t.traceBegin("StartAudioService");
                                                    if (isArc) {
                                                    }
                                                    t.traceEnd();
                                                    t.traceBegin("StartSoundTriggerMiddlewareService");
                                                    this.mSystemServiceManager.startService(SoundTriggerMiddlewareService.Lifecycle.class);
                                                    t.traceEnd();
                                                    if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                                                    }
                                                    t.traceBegin("StartDockObserver");
                                                    this.mSystemServiceManager.startService(DockObserver.class);
                                                    t.traceEnd();
                                                    if (isWatch) {
                                                    }
                                                    if (!isWatch) {
                                                    }
                                                    if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                                                    }
                                                    t.traceBegin("StartAdbService");
                                                    this.mSystemServiceManager.startService(ADB_SERVICE_CLASS);
                                                    t.traceEnd();
                                                    if (!this.mPackageManager.hasSystemFeature("android.hardware.usb.host")) {
                                                    }
                                                    t.traceBegin("StartUsbService");
                                                    this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                                                    t.traceEnd();
                                                    if (!isWatch) {
                                                    }
                                                    t.traceBegin("StartHardwarePropertiesManagerService");
                                                    hardwarePropertiesManagerService = new HardwarePropertiesManagerService(context);
                                                    try {
                                                        ServiceManager.addService("hardware_properties", hardwarePropertiesManagerService);
                                                        stub8 = hardwarePropertiesManagerService;
                                                    } catch (Throwable th8) {
                                                        e = th8;
                                                        stub8 = hardwarePropertiesManagerService;
                                                        Slog.e(TAG, "Failure starting HardwarePropertiesManagerService", e);
                                                        t.traceEnd();
                                                        if (!isWatch) {
                                                        }
                                                        t.traceBegin("StartColorDisplay");
                                                        this.mSystemServiceManager.startService(ColorDisplayService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartJobScheduler");
                                                        this.mSystemServiceManager.startService(JOB_SCHEDULER_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StartSoundTrigger");
                                                        this.mSystemServiceManager.startService(SoundTriggerService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartTrustManager");
                                                        this.mSystemServiceManager.startService(TrustManagerService.class);
                                                        t.traceEnd();
                                                        if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                                                        }
                                                        if (!this.mPackageManager.hasSystemFeature("android.software.app_widgets")) {
                                                        }
                                                        t.traceBegin("StartAppWidgetService");
                                                        this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StartVoiceRecognitionManager");
                                                        this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StartAppHibernationService");
                                                        this.mSystemServiceManager.startService(APP_HIBERNATION_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                                                        }
                                                        t.traceBegin("StartSensorNotification");
                                                        this.mSystemServiceManager.startService(SensorNotificationService.class);
                                                        t.traceEnd();
                                                        if (this.mPackageManager.hasSystemFeature("android.hardware.context_hub")) {
                                                        }
                                                        t.traceBegin("StartDiskStatsService");
                                                        ServiceManager.addService("diskstats", new DiskStatsService(context));
                                                        t.traceEnd();
                                                        t.traceBegin("RuntimeService");
                                                        ServiceManager.addService("runtime", new RuntimeService(context));
                                                        t.traceEnd();
                                                        startRulesManagerService = this.mOnlyCore && context.getResources().getBoolean(17891650);
                                                        if (startRulesManagerService) {
                                                        }
                                                        if (!isWatch) {
                                                        }
                                                        t.traceBegin("CertBlacklister");
                                                        new CertBlacklister(context);
                                                        t.traceEnd();
                                                        t.traceBegin("StartEmergencyAffordanceService");
                                                        this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                                                        t.traceEnd();
                                                        t.traceBegin(START_BLOB_STORE_SERVICE);
                                                        this.mSystemServiceManager.startService(BLOB_STORE_MANAGER_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StartDreamManager");
                                                        this.mSystemServiceManager.startService(DreamManagerService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("AddGraphicsStatsService");
                                                        ServiceManager.addService("graphicsstats", new GraphicsStatsService(context));
                                                        t.traceEnd();
                                                        if (CoverageService.ENABLED) {
                                                        }
                                                        if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                                                        }
                                                        t.traceBegin("StartAttestationVerificationService");
                                                        this.mSystemServiceManager.startService(AttestationVerificationManagerService.class);
                                                        t.traceEnd();
                                                        if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                                                        }
                                                        t.traceBegin("StartRestrictionManager");
                                                        this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartMediaSessionService");
                                                        this.mSystemServiceManager.startService(MEDIA_SESSION_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                                                        }
                                                        if (!this.mPackageManager.hasSystemFeature("android.software.live_tv")) {
                                                        }
                                                        t.traceBegin("StartTvInteractiveAppManager");
                                                        this.mSystemServiceManager.startService(TvInteractiveAppManagerService.class);
                                                        t.traceEnd();
                                                        if (!this.mPackageManager.hasSystemFeature("android.software.live_tv")) {
                                                        }
                                                        t.traceBegin("StartTvInputManager");
                                                        this.mSystemServiceManager.startService(TvInputManagerService.class);
                                                        t.traceEnd();
                                                        if (this.mPackageManager.hasSystemFeature("android.hardware.tv.tuner")) {
                                                        }
                                                        if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                                                        }
                                                        if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                                                        }
                                                        t.traceBegin("StartMediaRouterService");
                                                        mediaRouterService = new MediaRouterService(context);
                                                        ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                                                        mediaRouter2 = mediaRouterService;
                                                        t.traceEnd();
                                                        hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                                                        hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                                                        hasFeatureFingerprint = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                                                        if (hasFeatureFace) {
                                                        }
                                                        if (hasFeatureIris) {
                                                        }
                                                        if (hasFeatureFingerprint) {
                                                        }
                                                        t.traceBegin("StartBiometricService");
                                                        this.mSystemServiceManager.startService(BiometricService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartAuthService");
                                                        this.mSystemServiceManager.startService(AuthService.class);
                                                        t.traceEnd();
                                                        if (!isWatch) {
                                                        }
                                                        if (!isWatch) {
                                                        }
                                                        t.traceBegin("StartShortcutServiceLifecycle");
                                                        this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartLauncherAppsService");
                                                        this.mSystemServiceManager.startService(LauncherAppsService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartCrossProfileAppsService");
                                                        this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartPeopleService");
                                                        this.mSystemServiceManager.startService(PeopleService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartMediaMetricsManager");
                                                        this.mSystemServiceManager.startService(MediaMetricsManagerService.class);
                                                        t.traceEnd();
                                                        stub2 = stub10;
                                                        iNetworkManagementService = iNetworkManagementService2;
                                                        stub3 = stub5;
                                                        stub4 = stub6;
                                                        networkPolicy = r15;
                                                        lockSettings = lockSettings2;
                                                        networkTimeUpdater = networkTimeUpdater3;
                                                        mediaRouter = mediaRouter2;
                                                        t.traceBegin("StartMediaProjectionManager");
                                                        this.mSystemServiceManager.startService(MediaProjectionManagerService.class);
                                                        t.traceEnd();
                                                        if (isWatch) {
                                                        }
                                                        if (!this.mPackageManager.hasSystemFeature("android.software.slices_disabled")) {
                                                        }
                                                        if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                                        }
                                                        t.traceBegin("StartStatsCompanion");
                                                        this.mSystemServiceManager.startServiceFromJar(STATS_COMPANION_LIFECYCLE_CLASS, STATS_COMPANION_APEX_PATH);
                                                        t.traceEnd();
                                                        t.traceBegin("StartRebootReadinessManagerService");
                                                        this.mSystemServiceManager.startServiceFromJar(REBOOT_READINESS_LIFECYCLE_CLASS, SCHEDULING_APEX_PATH);
                                                        t.traceEnd();
                                                        t.traceBegin("StartStatsPullAtomService");
                                                        this.mSystemServiceManager.startService(STATS_PULL_ATOM_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StatsBootstrapAtomService");
                                                        this.mSystemServiceManager.startService(STATS_BOOTSTRAP_ATOM_SERVICE_LIFECYCLE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StartIncidentCompanionService");
                                                        this.mSystemServiceManager.startService(IncidentCompanionService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StarSdkSandboxManagerService");
                                                        this.mSystemServiceManager.startService(SDK_SANDBOX_MANAGER_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StartAdServicesManagerService");
                                                        this.mSystemServiceManager.startService(AD_SERVICES_MANAGER_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        if (safeMode) {
                                                        }
                                                        if (this.mPackageManager.hasSystemFeature("android.hardware.telephony")) {
                                                        }
                                                        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                                        }
                                                        if (deviceHasConfigString(context, 17039947)) {
                                                        }
                                                        t.traceBegin("StartClipboardService");
                                                        this.mSystemServiceManager.startService(ClipboardService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("AppServiceManager");
                                                        this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                                                        t.traceEnd();
                                                        sMtkSystemServerIns.startMtkOtherServices();
                                                        t.traceBegin("startTracingServiceProxy");
                                                        this.mSystemServiceManager.startService(TracingServiceProxy.class);
                                                        t.traceEnd();
                                                        t.traceBegin("MakeLockSettingsServiceReady");
                                                        if (lockSettings != null) {
                                                        }
                                                        t.traceEnd();
                                                        t.traceBegin("StartBootPhaseLockSettingsReady");
                                                        this.mSystemServiceManager.startBootPhase(t, SystemService.PHASE_LOCK_SETTINGS_READY);
                                                        t.traceEnd();
                                                        t.traceBegin("StartBootPhaseSystemServicesReady");
                                                        this.mSystemServiceManager.startBootPhase(t, 500);
                                                        t.traceEnd();
                                                        t.traceBegin("MakeWindowManagerServiceReady");
                                                        main.systemReady();
                                                        t.traceEnd();
                                                        synchronized (SystemService.class) {
                                                        }
                                                    }
                                                    t.traceEnd();
                                                    if (!isWatch) {
                                                    }
                                                    t.traceBegin("StartColorDisplay");
                                                    this.mSystemServiceManager.startService(ColorDisplayService.class);
                                                    t.traceEnd();
                                                    t.traceBegin("StartJobScheduler");
                                                    this.mSystemServiceManager.startService(JOB_SCHEDULER_SERVICE_CLASS);
                                                    t.traceEnd();
                                                    t.traceBegin("StartSoundTrigger");
                                                    this.mSystemServiceManager.startService(SoundTriggerService.class);
                                                    t.traceEnd();
                                                    t.traceBegin("StartTrustManager");
                                                    this.mSystemServiceManager.startService(TrustManagerService.class);
                                                    t.traceEnd();
                                                    if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                                                    }
                                                    if (!this.mPackageManager.hasSystemFeature("android.software.app_widgets")) {
                                                    }
                                                    t.traceBegin("StartAppWidgetService");
                                                    this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                                                    t.traceEnd();
                                                    t.traceBegin("StartVoiceRecognitionManager");
                                                    this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                                                    t.traceEnd();
                                                    t.traceBegin("StartAppHibernationService");
                                                    this.mSystemServiceManager.startService(APP_HIBERNATION_SERVICE_CLASS);
                                                    t.traceEnd();
                                                    if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                                                    }
                                                    t.traceBegin("StartSensorNotification");
                                                    this.mSystemServiceManager.startService(SensorNotificationService.class);
                                                    t.traceEnd();
                                                    if (this.mPackageManager.hasSystemFeature("android.hardware.context_hub")) {
                                                    }
                                                    t.traceBegin("StartDiskStatsService");
                                                    ServiceManager.addService("diskstats", new DiskStatsService(context));
                                                    t.traceEnd();
                                                    t.traceBegin("RuntimeService");
                                                    ServiceManager.addService("runtime", new RuntimeService(context));
                                                    t.traceEnd();
                                                    startRulesManagerService = this.mOnlyCore && context.getResources().getBoolean(17891650);
                                                    if (startRulesManagerService) {
                                                    }
                                                    if (!isWatch) {
                                                    }
                                                    t.traceBegin("CertBlacklister");
                                                    new CertBlacklister(context);
                                                    t.traceEnd();
                                                    t.traceBegin("StartEmergencyAffordanceService");
                                                    this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                                                    t.traceEnd();
                                                    t.traceBegin(START_BLOB_STORE_SERVICE);
                                                    this.mSystemServiceManager.startService(BLOB_STORE_MANAGER_SERVICE_CLASS);
                                                    t.traceEnd();
                                                    t.traceBegin("StartDreamManager");
                                                    this.mSystemServiceManager.startService(DreamManagerService.class);
                                                    t.traceEnd();
                                                    t.traceBegin("AddGraphicsStatsService");
                                                    ServiceManager.addService("graphicsstats", new GraphicsStatsService(context));
                                                    t.traceEnd();
                                                    if (CoverageService.ENABLED) {
                                                    }
                                                    if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                                                    }
                                                    t.traceBegin("StartAttestationVerificationService");
                                                    this.mSystemServiceManager.startService(AttestationVerificationManagerService.class);
                                                    t.traceEnd();
                                                    if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                                                    }
                                                    t.traceBegin("StartRestrictionManager");
                                                    this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                                                    t.traceEnd();
                                                    t.traceBegin("StartMediaSessionService");
                                                    this.mSystemServiceManager.startService(MEDIA_SESSION_SERVICE_CLASS);
                                                    t.traceEnd();
                                                    if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                                                    }
                                                    if (!this.mPackageManager.hasSystemFeature("android.software.live_tv")) {
                                                    }
                                                    t.traceBegin("StartTvInteractiveAppManager");
                                                    this.mSystemServiceManager.startService(TvInteractiveAppManagerService.class);
                                                    t.traceEnd();
                                                    if (!this.mPackageManager.hasSystemFeature("android.software.live_tv")) {
                                                    }
                                                    t.traceBegin("StartTvInputManager");
                                                    this.mSystemServiceManager.startService(TvInputManagerService.class);
                                                    t.traceEnd();
                                                    if (this.mPackageManager.hasSystemFeature("android.hardware.tv.tuner")) {
                                                    }
                                                    if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                                                    }
                                                    if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                                                    }
                                                    t.traceBegin("StartMediaRouterService");
                                                    mediaRouterService = new MediaRouterService(context);
                                                    try {
                                                        ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                                                        mediaRouter2 = mediaRouterService;
                                                    } catch (Throwable th9) {
                                                        e = th9;
                                                        mediaRouter2 = mediaRouterService;
                                                        reportWtf("starting MediaRouterService", e);
                                                        t.traceEnd();
                                                        hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                                                        hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                                                        hasFeatureFingerprint = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                                                        if (hasFeatureFace) {
                                                        }
                                                        if (hasFeatureIris) {
                                                        }
                                                        if (hasFeatureFingerprint) {
                                                        }
                                                        t.traceBegin("StartBiometricService");
                                                        this.mSystemServiceManager.startService(BiometricService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartAuthService");
                                                        this.mSystemServiceManager.startService(AuthService.class);
                                                        t.traceEnd();
                                                        if (!isWatch) {
                                                        }
                                                        if (!isWatch) {
                                                        }
                                                        t.traceBegin("StartShortcutServiceLifecycle");
                                                        this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartLauncherAppsService");
                                                        this.mSystemServiceManager.startService(LauncherAppsService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartCrossProfileAppsService");
                                                        this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartPeopleService");
                                                        this.mSystemServiceManager.startService(PeopleService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartMediaMetricsManager");
                                                        this.mSystemServiceManager.startService(MediaMetricsManagerService.class);
                                                        t.traceEnd();
                                                        stub2 = stub10;
                                                        iNetworkManagementService = iNetworkManagementService2;
                                                        stub3 = stub5;
                                                        stub4 = stub6;
                                                        networkPolicy = r15;
                                                        lockSettings = lockSettings2;
                                                        networkTimeUpdater = networkTimeUpdater3;
                                                        mediaRouter = mediaRouter2;
                                                        t.traceBegin("StartMediaProjectionManager");
                                                        this.mSystemServiceManager.startService(MediaProjectionManagerService.class);
                                                        t.traceEnd();
                                                        if (isWatch) {
                                                        }
                                                        if (!this.mPackageManager.hasSystemFeature("android.software.slices_disabled")) {
                                                        }
                                                        if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                                        }
                                                        t.traceBegin("StartStatsCompanion");
                                                        this.mSystemServiceManager.startServiceFromJar(STATS_COMPANION_LIFECYCLE_CLASS, STATS_COMPANION_APEX_PATH);
                                                        t.traceEnd();
                                                        t.traceBegin("StartRebootReadinessManagerService");
                                                        this.mSystemServiceManager.startServiceFromJar(REBOOT_READINESS_LIFECYCLE_CLASS, SCHEDULING_APEX_PATH);
                                                        t.traceEnd();
                                                        t.traceBegin("StartStatsPullAtomService");
                                                        this.mSystemServiceManager.startService(STATS_PULL_ATOM_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StatsBootstrapAtomService");
                                                        this.mSystemServiceManager.startService(STATS_BOOTSTRAP_ATOM_SERVICE_LIFECYCLE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StartIncidentCompanionService");
                                                        this.mSystemServiceManager.startService(IncidentCompanionService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StarSdkSandboxManagerService");
                                                        this.mSystemServiceManager.startService(SDK_SANDBOX_MANAGER_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StartAdServicesManagerService");
                                                        this.mSystemServiceManager.startService(AD_SERVICES_MANAGER_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        if (safeMode) {
                                                        }
                                                        if (this.mPackageManager.hasSystemFeature("android.hardware.telephony")) {
                                                        }
                                                        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                                        }
                                                        if (deviceHasConfigString(context, 17039947)) {
                                                        }
                                                        t.traceBegin("StartClipboardService");
                                                        this.mSystemServiceManager.startService(ClipboardService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("AppServiceManager");
                                                        this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                                                        t.traceEnd();
                                                        sMtkSystemServerIns.startMtkOtherServices();
                                                        t.traceBegin("startTracingServiceProxy");
                                                        this.mSystemServiceManager.startService(TracingServiceProxy.class);
                                                        t.traceEnd();
                                                        t.traceBegin("MakeLockSettingsServiceReady");
                                                        if (lockSettings != null) {
                                                        }
                                                        t.traceEnd();
                                                        t.traceBegin("StartBootPhaseLockSettingsReady");
                                                        this.mSystemServiceManager.startBootPhase(t, SystemService.PHASE_LOCK_SETTINGS_READY);
                                                        t.traceEnd();
                                                        t.traceBegin("StartBootPhaseSystemServicesReady");
                                                        this.mSystemServiceManager.startBootPhase(t, 500);
                                                        t.traceEnd();
                                                        t.traceBegin("MakeWindowManagerServiceReady");
                                                        main.systemReady();
                                                        t.traceEnd();
                                                        synchronized (SystemService.class) {
                                                        }
                                                    }
                                                    t.traceEnd();
                                                    hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                                                    hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                                                    hasFeatureFingerprint = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                                                    if (hasFeatureFace) {
                                                    }
                                                    if (hasFeatureIris) {
                                                    }
                                                    if (hasFeatureFingerprint) {
                                                    }
                                                    t.traceBegin("StartBiometricService");
                                                    this.mSystemServiceManager.startService(BiometricService.class);
                                                    t.traceEnd();
                                                    t.traceBegin("StartAuthService");
                                                    this.mSystemServiceManager.startService(AuthService.class);
                                                    t.traceEnd();
                                                    if (!isWatch) {
                                                    }
                                                    if (!isWatch) {
                                                    }
                                                    t.traceBegin("StartShortcutServiceLifecycle");
                                                    this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                                                    t.traceEnd();
                                                    t.traceBegin("StartLauncherAppsService");
                                                    this.mSystemServiceManager.startService(LauncherAppsService.class);
                                                    t.traceEnd();
                                                    t.traceBegin("StartCrossProfileAppsService");
                                                    this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                                                    t.traceEnd();
                                                    t.traceBegin("StartPeopleService");
                                                    this.mSystemServiceManager.startService(PeopleService.class);
                                                    t.traceEnd();
                                                    t.traceBegin("StartMediaMetricsManager");
                                                    this.mSystemServiceManager.startService(MediaMetricsManagerService.class);
                                                    t.traceEnd();
                                                    stub2 = stub10;
                                                    iNetworkManagementService = iNetworkManagementService2;
                                                    stub3 = stub5;
                                                    stub4 = stub6;
                                                    networkPolicy = r15;
                                                    lockSettings = lockSettings2;
                                                    networkTimeUpdater = networkTimeUpdater3;
                                                    mediaRouter = mediaRouter2;
                                                    t.traceBegin("StartMediaProjectionManager");
                                                    this.mSystemServiceManager.startService(MediaProjectionManagerService.class);
                                                    t.traceEnd();
                                                    if (isWatch) {
                                                    }
                                                    if (!this.mPackageManager.hasSystemFeature("android.software.slices_disabled")) {
                                                    }
                                                    if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                                    }
                                                    t.traceBegin("StartStatsCompanion");
                                                    this.mSystemServiceManager.startServiceFromJar(STATS_COMPANION_LIFECYCLE_CLASS, STATS_COMPANION_APEX_PATH);
                                                    t.traceEnd();
                                                    t.traceBegin("StartRebootReadinessManagerService");
                                                    this.mSystemServiceManager.startServiceFromJar(REBOOT_READINESS_LIFECYCLE_CLASS, SCHEDULING_APEX_PATH);
                                                    t.traceEnd();
                                                    t.traceBegin("StartStatsPullAtomService");
                                                    this.mSystemServiceManager.startService(STATS_PULL_ATOM_SERVICE_CLASS);
                                                    t.traceEnd();
                                                    t.traceBegin("StatsBootstrapAtomService");
                                                    this.mSystemServiceManager.startService(STATS_BOOTSTRAP_ATOM_SERVICE_LIFECYCLE_CLASS);
                                                    t.traceEnd();
                                                    t.traceBegin("StartIncidentCompanionService");
                                                    this.mSystemServiceManager.startService(IncidentCompanionService.class);
                                                    t.traceEnd();
                                                    t.traceBegin("StarSdkSandboxManagerService");
                                                    this.mSystemServiceManager.startService(SDK_SANDBOX_MANAGER_SERVICE_CLASS);
                                                    t.traceEnd();
                                                    t.traceBegin("StartAdServicesManagerService");
                                                    this.mSystemServiceManager.startService(AD_SERVICES_MANAGER_SERVICE_CLASS);
                                                    t.traceEnd();
                                                    if (safeMode) {
                                                    }
                                                    if (this.mPackageManager.hasSystemFeature("android.hardware.telephony")) {
                                                    }
                                                    if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                                    }
                                                    if (deviceHasConfigString(context, 17039947)) {
                                                    }
                                                    t.traceBegin("StartClipboardService");
                                                    this.mSystemServiceManager.startService(ClipboardService.class);
                                                    t.traceEnd();
                                                    t.traceBegin("AppServiceManager");
                                                    this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                                                    t.traceEnd();
                                                    sMtkSystemServerIns.startMtkOtherServices();
                                                    t.traceBegin("startTracingServiceProxy");
                                                    this.mSystemServiceManager.startService(TracingServiceProxy.class);
                                                    t.traceEnd();
                                                    t.traceBegin("MakeLockSettingsServiceReady");
                                                    if (lockSettings != null) {
                                                    }
                                                    t.traceEnd();
                                                    t.traceBegin("StartBootPhaseLockSettingsReady");
                                                    this.mSystemServiceManager.startBootPhase(t, SystemService.PHASE_LOCK_SETTINGS_READY);
                                                    t.traceEnd();
                                                    t.traceBegin("StartBootPhaseSystemServicesReady");
                                                    this.mSystemServiceManager.startBootPhase(t, 500);
                                                    t.traceEnd();
                                                    t.traceBegin("MakeWindowManagerServiceReady");
                                                    main.systemReady();
                                                    t.traceEnd();
                                                    synchronized (SystemService.class) {
                                                    }
                                                }
                                            } catch (Throwable th10) {
                                                e = th10;
                                            }
                                            t.traceEnd();
                                            t.traceBegin("StartConnectivityService");
                                            this.mSystemServiceManager.startServiceFromJar(CONNECTIVITY_SERVICE_INITIALIZER_CLASS, CONNECTIVITY_SERVICE_APEX_PATH);
                                            r15.bindConnectivityManager();
                                            t.traceEnd();
                                            t.traceBegin("StartVpnManagerService");
                                            try {
                                                stub5 = VpnManagerService.create(context);
                                                ServiceManager.addService("vpn_management", stub5);
                                            } catch (Throwable e13) {
                                                reportWtf("starting VPN Manager Service", e13);
                                            }
                                            t.traceEnd();
                                            t.traceBegin("StartVcnManagementService");
                                            try {
                                                stub6 = VcnManagementService.create(context);
                                                ServiceManager.addService("vcn_management", stub6);
                                            } catch (Throwable e14) {
                                                reportWtf("starting VCN Management Service", e14);
                                            }
                                            t.traceEnd();
                                            t.traceBegin("StartSystemUpdateManagerService");
                                            try {
                                                ServiceManager.addService("system_update", new SystemUpdateManagerService(context));
                                            } catch (Throwable e15) {
                                                reportWtf("starting SystemUpdateManagerService", e15);
                                            }
                                            t.traceEnd();
                                            t.traceBegin("StartUpdateLockService");
                                            try {
                                                ServiceManager.addService("updatelock", new UpdateLockService(context));
                                            } catch (Throwable e16) {
                                                reportWtf("starting UpdateLockService", e16);
                                            }
                                            t.traceEnd();
                                            t.traceBegin("StartNotificationManager");
                                            this.mSystemServiceManager.startService(NotificationManagerService.class);
                                            SystemNotificationChannels.removeDeprecated(context);
                                            SystemNotificationChannels.createAll(context);
                                            INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
                                            t.traceEnd();
                                            t.traceBegin("StartDeviceMonitor");
                                            this.mSystemServiceManager.startService(DeviceStorageMonitorService.class);
                                            t.traceEnd();
                                            t.traceBegin("StartLocationManagerService");
                                            this.mSystemServiceManager.startService(LocationManagerService.Lifecycle.class);
                                            t.traceEnd();
                                            t.traceBegin("StartCountryDetectorService");
                                            try {
                                                stub10 = new CountryDetectorService(context);
                                                ServiceManager.addService("country_detector", stub10);
                                            } catch (Throwable e17) {
                                                reportWtf("starting Country Detector", e17);
                                            }
                                            t.traceEnd();
                                            t.traceBegin("StartTimeDetectorService");
                                            try {
                                                this.mSystemServiceManager.startService(TIME_DETECTOR_SERVICE_CLASS);
                                            } catch (Throwable e18) {
                                                reportWtf("starting TimeDetectorService service", e18);
                                            }
                                            t.traceEnd();
                                            t.traceBegin("StartTimeZoneDetectorService");
                                            try {
                                                this.mSystemServiceManager.startService(TIME_ZONE_DETECTOR_SERVICE_CLASS);
                                            } catch (Throwable e19) {
                                                reportWtf("starting TimeZoneDetectorService service", e19);
                                            }
                                            t.traceEnd();
                                            t.traceBegin("StartLocationTimeZoneManagerService");
                                            try {
                                                this.mSystemServiceManager.startService(LOCATION_TIME_ZONE_MANAGER_SERVICE_CLASS);
                                            } catch (Throwable e20) {
                                                reportWtf("starting LocationTimeZoneManagerService service", e20);
                                            }
                                            t.traceEnd();
                                            if (context.getResources().getBoolean(17891635)) {
                                                t.traceBegin("StartGnssTimeUpdateService");
                                                try {
                                                    this.mSystemServiceManager.startService(GNSS_TIME_UPDATE_SERVICE_CLASS);
                                                } catch (Throwable e21) {
                                                    reportWtf("starting GnssTimeUpdateService service", e21);
                                                }
                                                t.traceEnd();
                                            }
                                            if (!isWatch) {
                                                t.traceBegin("StartSearchManagerService");
                                                try {
                                                    this.mSystemServiceManager.startService(SEARCH_MANAGER_SERVICE_CLASS);
                                                } catch (Throwable e22) {
                                                    reportWtf("starting Search Service", e22);
                                                }
                                                t.traceEnd();
                                            }
                                            if (context.getResources().getBoolean(17891651)) {
                                                Slog.i(TAG, "Wallpaper service disabled by config");
                                            } else {
                                                t.traceBegin("StartWallpaperManagerService");
                                                this.mSystemServiceManager.startService(WALLPAPER_SERVICE_CLASS);
                                                t.traceEnd();
                                            }
                                            t.traceBegin("StartWallpaperEffectsGenerationService");
                                            this.mSystemServiceManager.startService(WALLPAPER_EFFECTS_GENERATION_MANAGER_SERVICE_CLASS);
                                            t.traceEnd();
                                            t.traceBegin("StartAudioService");
                                            if (isArc) {
                                                this.mSystemServiceManager.startService(AudioService.Lifecycle.class);
                                            } else {
                                                String className = context.getResources().getString(17039955);
                                                try {
                                                    try {
                                                        this.mSystemServiceManager.startService(className + "$Lifecycle");
                                                    } catch (Throwable th11) {
                                                        e = th11;
                                                        reportWtf("starting " + className, e);
                                                        t.traceEnd();
                                                        t.traceBegin("StartSoundTriggerMiddlewareService");
                                                        this.mSystemServiceManager.startService(SoundTriggerMiddlewareService.Lifecycle.class);
                                                        t.traceEnd();
                                                        if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                                                        }
                                                        t.traceBegin("StartDockObserver");
                                                        this.mSystemServiceManager.startService(DockObserver.class);
                                                        t.traceEnd();
                                                        if (isWatch) {
                                                        }
                                                        if (!isWatch) {
                                                        }
                                                        if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                                                        }
                                                        t.traceBegin("StartAdbService");
                                                        this.mSystemServiceManager.startService(ADB_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        if (!this.mPackageManager.hasSystemFeature("android.hardware.usb.host")) {
                                                        }
                                                        t.traceBegin("StartUsbService");
                                                        this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        if (!isWatch) {
                                                        }
                                                        t.traceBegin("StartHardwarePropertiesManagerService");
                                                        hardwarePropertiesManagerService = new HardwarePropertiesManagerService(context);
                                                        ServiceManager.addService("hardware_properties", hardwarePropertiesManagerService);
                                                        stub8 = hardwarePropertiesManagerService;
                                                        t.traceEnd();
                                                        if (!isWatch) {
                                                        }
                                                        t.traceBegin("StartColorDisplay");
                                                        this.mSystemServiceManager.startService(ColorDisplayService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartJobScheduler");
                                                        this.mSystemServiceManager.startService(JOB_SCHEDULER_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StartSoundTrigger");
                                                        this.mSystemServiceManager.startService(SoundTriggerService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartTrustManager");
                                                        this.mSystemServiceManager.startService(TrustManagerService.class);
                                                        t.traceEnd();
                                                        if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                                                        }
                                                        if (!this.mPackageManager.hasSystemFeature("android.software.app_widgets")) {
                                                        }
                                                        t.traceBegin("StartAppWidgetService");
                                                        this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StartVoiceRecognitionManager");
                                                        this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StartAppHibernationService");
                                                        this.mSystemServiceManager.startService(APP_HIBERNATION_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                                                        }
                                                        t.traceBegin("StartSensorNotification");
                                                        this.mSystemServiceManager.startService(SensorNotificationService.class);
                                                        t.traceEnd();
                                                        if (this.mPackageManager.hasSystemFeature("android.hardware.context_hub")) {
                                                        }
                                                        t.traceBegin("StartDiskStatsService");
                                                        ServiceManager.addService("diskstats", new DiskStatsService(context));
                                                        t.traceEnd();
                                                        t.traceBegin("RuntimeService");
                                                        ServiceManager.addService("runtime", new RuntimeService(context));
                                                        t.traceEnd();
                                                        startRulesManagerService = this.mOnlyCore && context.getResources().getBoolean(17891650);
                                                        if (startRulesManagerService) {
                                                        }
                                                        if (!isWatch) {
                                                        }
                                                        t.traceBegin("CertBlacklister");
                                                        new CertBlacklister(context);
                                                        t.traceEnd();
                                                        t.traceBegin("StartEmergencyAffordanceService");
                                                        this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                                                        t.traceEnd();
                                                        t.traceBegin(START_BLOB_STORE_SERVICE);
                                                        this.mSystemServiceManager.startService(BLOB_STORE_MANAGER_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StartDreamManager");
                                                        this.mSystemServiceManager.startService(DreamManagerService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("AddGraphicsStatsService");
                                                        ServiceManager.addService("graphicsstats", new GraphicsStatsService(context));
                                                        t.traceEnd();
                                                        if (CoverageService.ENABLED) {
                                                        }
                                                        if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                                                        }
                                                        t.traceBegin("StartAttestationVerificationService");
                                                        this.mSystemServiceManager.startService(AttestationVerificationManagerService.class);
                                                        t.traceEnd();
                                                        if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                                                        }
                                                        t.traceBegin("StartRestrictionManager");
                                                        this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartMediaSessionService");
                                                        this.mSystemServiceManager.startService(MEDIA_SESSION_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                                                        }
                                                        if (!this.mPackageManager.hasSystemFeature("android.software.live_tv")) {
                                                        }
                                                        t.traceBegin("StartTvInteractiveAppManager");
                                                        this.mSystemServiceManager.startService(TvInteractiveAppManagerService.class);
                                                        t.traceEnd();
                                                        if (!this.mPackageManager.hasSystemFeature("android.software.live_tv")) {
                                                        }
                                                        t.traceBegin("StartTvInputManager");
                                                        this.mSystemServiceManager.startService(TvInputManagerService.class);
                                                        t.traceEnd();
                                                        if (this.mPackageManager.hasSystemFeature("android.hardware.tv.tuner")) {
                                                        }
                                                        if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                                                        }
                                                        if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                                                        }
                                                        t.traceBegin("StartMediaRouterService");
                                                        mediaRouterService = new MediaRouterService(context);
                                                        ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                                                        mediaRouter2 = mediaRouterService;
                                                        t.traceEnd();
                                                        hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                                                        hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                                                        hasFeatureFingerprint = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                                                        if (hasFeatureFace) {
                                                        }
                                                        if (hasFeatureIris) {
                                                        }
                                                        if (hasFeatureFingerprint) {
                                                        }
                                                        t.traceBegin("StartBiometricService");
                                                        this.mSystemServiceManager.startService(BiometricService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartAuthService");
                                                        this.mSystemServiceManager.startService(AuthService.class);
                                                        t.traceEnd();
                                                        if (!isWatch) {
                                                        }
                                                        if (!isWatch) {
                                                        }
                                                        t.traceBegin("StartShortcutServiceLifecycle");
                                                        this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartLauncherAppsService");
                                                        this.mSystemServiceManager.startService(LauncherAppsService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartCrossProfileAppsService");
                                                        this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartPeopleService");
                                                        this.mSystemServiceManager.startService(PeopleService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartMediaMetricsManager");
                                                        this.mSystemServiceManager.startService(MediaMetricsManagerService.class);
                                                        t.traceEnd();
                                                        stub2 = stub10;
                                                        iNetworkManagementService = iNetworkManagementService2;
                                                        stub3 = stub5;
                                                        stub4 = stub6;
                                                        networkPolicy = r15;
                                                        lockSettings = lockSettings2;
                                                        networkTimeUpdater = networkTimeUpdater3;
                                                        mediaRouter = mediaRouter2;
                                                        t.traceBegin("StartMediaProjectionManager");
                                                        this.mSystemServiceManager.startService(MediaProjectionManagerService.class);
                                                        t.traceEnd();
                                                        if (isWatch) {
                                                        }
                                                        if (!this.mPackageManager.hasSystemFeature("android.software.slices_disabled")) {
                                                        }
                                                        if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                                        }
                                                        t.traceBegin("StartStatsCompanion");
                                                        this.mSystemServiceManager.startServiceFromJar(STATS_COMPANION_LIFECYCLE_CLASS, STATS_COMPANION_APEX_PATH);
                                                        t.traceEnd();
                                                        t.traceBegin("StartRebootReadinessManagerService");
                                                        this.mSystemServiceManager.startServiceFromJar(REBOOT_READINESS_LIFECYCLE_CLASS, SCHEDULING_APEX_PATH);
                                                        t.traceEnd();
                                                        t.traceBegin("StartStatsPullAtomService");
                                                        this.mSystemServiceManager.startService(STATS_PULL_ATOM_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StatsBootstrapAtomService");
                                                        this.mSystemServiceManager.startService(STATS_BOOTSTRAP_ATOM_SERVICE_LIFECYCLE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StartIncidentCompanionService");
                                                        this.mSystemServiceManager.startService(IncidentCompanionService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StarSdkSandboxManagerService");
                                                        this.mSystemServiceManager.startService(SDK_SANDBOX_MANAGER_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StartAdServicesManagerService");
                                                        this.mSystemServiceManager.startService(AD_SERVICES_MANAGER_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        if (safeMode) {
                                                        }
                                                        if (this.mPackageManager.hasSystemFeature("android.hardware.telephony")) {
                                                        }
                                                        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                                        }
                                                        if (deviceHasConfigString(context, 17039947)) {
                                                        }
                                                        t.traceBegin("StartClipboardService");
                                                        this.mSystemServiceManager.startService(ClipboardService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("AppServiceManager");
                                                        this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                                                        t.traceEnd();
                                                        sMtkSystemServerIns.startMtkOtherServices();
                                                        t.traceBegin("startTracingServiceProxy");
                                                        this.mSystemServiceManager.startService(TracingServiceProxy.class);
                                                        t.traceEnd();
                                                        t.traceBegin("MakeLockSettingsServiceReady");
                                                        if (lockSettings != null) {
                                                        }
                                                        t.traceEnd();
                                                        t.traceBegin("StartBootPhaseLockSettingsReady");
                                                        this.mSystemServiceManager.startBootPhase(t, SystemService.PHASE_LOCK_SETTINGS_READY);
                                                        t.traceEnd();
                                                        t.traceBegin("StartBootPhaseSystemServicesReady");
                                                        this.mSystemServiceManager.startBootPhase(t, 500);
                                                        t.traceEnd();
                                                        t.traceBegin("MakeWindowManagerServiceReady");
                                                        main.systemReady();
                                                        t.traceEnd();
                                                        synchronized (SystemService.class) {
                                                        }
                                                    }
                                                } catch (Throwable th12) {
                                                    e = th12;
                                                }
                                            }
                                            t.traceEnd();
                                            t.traceBegin("StartSoundTriggerMiddlewareService");
                                            this.mSystemServiceManager.startService(SoundTriggerMiddlewareService.Lifecycle.class);
                                            t.traceEnd();
                                            if (this.mPackageManager.hasSystemFeature("android.hardware.broadcastradio")) {
                                                t.traceBegin("StartBroadcastRadioService");
                                                this.mSystemServiceManager.startService(BroadcastRadioService.class);
                                                t.traceEnd();
                                            }
                                            t.traceBegin("StartDockObserver");
                                            this.mSystemServiceManager.startService(DockObserver.class);
                                            t.traceEnd();
                                            if (isWatch) {
                                                t.traceBegin("StartThermalObserver");
                                                this.mSystemServiceManager.startService(THERMAL_OBSERVER_CLASS);
                                                t.traceEnd();
                                            }
                                            if (!isWatch) {
                                                t.traceBegin("StartWiredAccessoryManager");
                                                try {
                                                    inputManagerService.setWiredAccessoryCallbacks(new WiredAccessoryManager(context, inputManagerService));
                                                } catch (Throwable e23) {
                                                    reportWtf("starting WiredAccessoryManager", e23);
                                                }
                                                t.traceEnd();
                                            }
                                            if (this.mPackageManager.hasSystemFeature("android.software.midi")) {
                                                t.traceBegin("StartMidiManager");
                                                this.mSystemServiceManager.startService(MIDI_SERVICE_CLASS);
                                                t.traceEnd();
                                            }
                                            t.traceBegin("StartAdbService");
                                            try {
                                                this.mSystemServiceManager.startService(ADB_SERVICE_CLASS);
                                            } catch (Throwable th13) {
                                                Slog.e(TAG, "Failure starting AdbService");
                                            }
                                            t.traceEnd();
                                            if (!this.mPackageManager.hasSystemFeature("android.hardware.usb.host") || this.mPackageManager.hasSystemFeature("android.hardware.usb.accessory") || isEmulator) {
                                                t.traceBegin("StartUsbService");
                                                this.mSystemServiceManager.startService(USB_SERVICE_CLASS);
                                                t.traceEnd();
                                            }
                                            if (!isWatch) {
                                                t.traceBegin("StartSerialService");
                                                try {
                                                    iBinder = new SerialService(context);
                                                    try {
                                                        ServiceManager.addService("serial", iBinder);
                                                    } catch (Throwable th14) {
                                                        e = th14;
                                                        iBinder2 = iBinder;
                                                        Slog.e(TAG, "Failure starting SerialService", e);
                                                        iBinder = iBinder2;
                                                        t.traceEnd();
                                                        iBinder2 = iBinder;
                                                        t.traceBegin("StartHardwarePropertiesManagerService");
                                                        hardwarePropertiesManagerService = new HardwarePropertiesManagerService(context);
                                                        ServiceManager.addService("hardware_properties", hardwarePropertiesManagerService);
                                                        stub8 = hardwarePropertiesManagerService;
                                                        t.traceEnd();
                                                        if (!isWatch) {
                                                        }
                                                        t.traceBegin("StartColorDisplay");
                                                        this.mSystemServiceManager.startService(ColorDisplayService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartJobScheduler");
                                                        this.mSystemServiceManager.startService(JOB_SCHEDULER_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StartSoundTrigger");
                                                        this.mSystemServiceManager.startService(SoundTriggerService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartTrustManager");
                                                        this.mSystemServiceManager.startService(TrustManagerService.class);
                                                        t.traceEnd();
                                                        if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                                                        }
                                                        if (!this.mPackageManager.hasSystemFeature("android.software.app_widgets")) {
                                                        }
                                                        t.traceBegin("StartAppWidgetService");
                                                        this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StartVoiceRecognitionManager");
                                                        this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StartAppHibernationService");
                                                        this.mSystemServiceManager.startService(APP_HIBERNATION_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                                                        }
                                                        t.traceBegin("StartSensorNotification");
                                                        this.mSystemServiceManager.startService(SensorNotificationService.class);
                                                        t.traceEnd();
                                                        if (this.mPackageManager.hasSystemFeature("android.hardware.context_hub")) {
                                                        }
                                                        t.traceBegin("StartDiskStatsService");
                                                        ServiceManager.addService("diskstats", new DiskStatsService(context));
                                                        t.traceEnd();
                                                        t.traceBegin("RuntimeService");
                                                        ServiceManager.addService("runtime", new RuntimeService(context));
                                                        t.traceEnd();
                                                        startRulesManagerService = this.mOnlyCore && context.getResources().getBoolean(17891650);
                                                        if (startRulesManagerService) {
                                                        }
                                                        if (!isWatch) {
                                                            t.traceBegin("StartNetworkTimeUpdateService");
                                                            try {
                                                                networkTimeUpdater2 = new NetworkTimeUpdateService(context);
                                                                try {
                                                                    ServiceManager.addService("network_time_update_service", networkTimeUpdater2);
                                                                } catch (Throwable th15) {
                                                                    e = th15;
                                                                    networkTimeUpdater3 = networkTimeUpdater2;
                                                                    reportWtf("starting NetworkTimeUpdate service", e);
                                                                    networkTimeUpdater2 = networkTimeUpdater3;
                                                                    t.traceEnd();
                                                                    networkTimeUpdater3 = networkTimeUpdater2;
                                                                    t.traceBegin("CertBlacklister");
                                                                    new CertBlacklister(context);
                                                                    t.traceEnd();
                                                                    t.traceBegin("StartEmergencyAffordanceService");
                                                                    this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                                                                    t.traceEnd();
                                                                    t.traceBegin(START_BLOB_STORE_SERVICE);
                                                                    this.mSystemServiceManager.startService(BLOB_STORE_MANAGER_SERVICE_CLASS);
                                                                    t.traceEnd();
                                                                    t.traceBegin("StartDreamManager");
                                                                    this.mSystemServiceManager.startService(DreamManagerService.class);
                                                                    t.traceEnd();
                                                                    t.traceBegin("AddGraphicsStatsService");
                                                                    ServiceManager.addService("graphicsstats", new GraphicsStatsService(context));
                                                                    t.traceEnd();
                                                                    if (CoverageService.ENABLED) {
                                                                    }
                                                                    if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                                                                    }
                                                                    t.traceBegin("StartAttestationVerificationService");
                                                                    this.mSystemServiceManager.startService(AttestationVerificationManagerService.class);
                                                                    t.traceEnd();
                                                                    if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                                                                    }
                                                                    t.traceBegin("StartRestrictionManager");
                                                                    this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                                                                    t.traceEnd();
                                                                    t.traceBegin("StartMediaSessionService");
                                                                    this.mSystemServiceManager.startService(MEDIA_SESSION_SERVICE_CLASS);
                                                                    t.traceEnd();
                                                                    if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                                                                    }
                                                                    if (!this.mPackageManager.hasSystemFeature("android.software.live_tv")) {
                                                                    }
                                                                    t.traceBegin("StartTvInteractiveAppManager");
                                                                    this.mSystemServiceManager.startService(TvInteractiveAppManagerService.class);
                                                                    t.traceEnd();
                                                                    if (!this.mPackageManager.hasSystemFeature("android.software.live_tv")) {
                                                                    }
                                                                    t.traceBegin("StartTvInputManager");
                                                                    this.mSystemServiceManager.startService(TvInputManagerService.class);
                                                                    t.traceEnd();
                                                                    if (this.mPackageManager.hasSystemFeature("android.hardware.tv.tuner")) {
                                                                    }
                                                                    if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                                                                    }
                                                                    if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                                                                    }
                                                                    t.traceBegin("StartMediaRouterService");
                                                                    mediaRouterService = new MediaRouterService(context);
                                                                    ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                                                                    mediaRouter2 = mediaRouterService;
                                                                    t.traceEnd();
                                                                    hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                                                                    hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                                                                    hasFeatureFingerprint = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                                                                    if (hasFeatureFace) {
                                                                    }
                                                                    if (hasFeatureIris) {
                                                                    }
                                                                    if (hasFeatureFingerprint) {
                                                                    }
                                                                    t.traceBegin("StartBiometricService");
                                                                    this.mSystemServiceManager.startService(BiometricService.class);
                                                                    t.traceEnd();
                                                                    t.traceBegin("StartAuthService");
                                                                    this.mSystemServiceManager.startService(AuthService.class);
                                                                    t.traceEnd();
                                                                    if (!isWatch) {
                                                                    }
                                                                    if (!isWatch) {
                                                                    }
                                                                    t.traceBegin("StartShortcutServiceLifecycle");
                                                                    this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                                                                    t.traceEnd();
                                                                    t.traceBegin("StartLauncherAppsService");
                                                                    this.mSystemServiceManager.startService(LauncherAppsService.class);
                                                                    t.traceEnd();
                                                                    t.traceBegin("StartCrossProfileAppsService");
                                                                    this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                                                                    t.traceEnd();
                                                                    t.traceBegin("StartPeopleService");
                                                                    this.mSystemServiceManager.startService(PeopleService.class);
                                                                    t.traceEnd();
                                                                    t.traceBegin("StartMediaMetricsManager");
                                                                    this.mSystemServiceManager.startService(MediaMetricsManagerService.class);
                                                                    t.traceEnd();
                                                                    stub2 = stub10;
                                                                    iNetworkManagementService = iNetworkManagementService2;
                                                                    stub3 = stub5;
                                                                    stub4 = stub6;
                                                                    networkPolicy = r15;
                                                                    lockSettings = lockSettings2;
                                                                    networkTimeUpdater = networkTimeUpdater3;
                                                                    mediaRouter = mediaRouter2;
                                                                    t.traceBegin("StartMediaProjectionManager");
                                                                    this.mSystemServiceManager.startService(MediaProjectionManagerService.class);
                                                                    t.traceEnd();
                                                                    if (isWatch) {
                                                                    }
                                                                    if (!this.mPackageManager.hasSystemFeature("android.software.slices_disabled")) {
                                                                    }
                                                                    if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                                                    }
                                                                    t.traceBegin("StartStatsCompanion");
                                                                    this.mSystemServiceManager.startServiceFromJar(STATS_COMPANION_LIFECYCLE_CLASS, STATS_COMPANION_APEX_PATH);
                                                                    t.traceEnd();
                                                                    t.traceBegin("StartRebootReadinessManagerService");
                                                                    this.mSystemServiceManager.startServiceFromJar(REBOOT_READINESS_LIFECYCLE_CLASS, SCHEDULING_APEX_PATH);
                                                                    t.traceEnd();
                                                                    t.traceBegin("StartStatsPullAtomService");
                                                                    this.mSystemServiceManager.startService(STATS_PULL_ATOM_SERVICE_CLASS);
                                                                    t.traceEnd();
                                                                    t.traceBegin("StatsBootstrapAtomService");
                                                                    this.mSystemServiceManager.startService(STATS_BOOTSTRAP_ATOM_SERVICE_LIFECYCLE_CLASS);
                                                                    t.traceEnd();
                                                                    t.traceBegin("StartIncidentCompanionService");
                                                                    this.mSystemServiceManager.startService(IncidentCompanionService.class);
                                                                    t.traceEnd();
                                                                    t.traceBegin("StarSdkSandboxManagerService");
                                                                    this.mSystemServiceManager.startService(SDK_SANDBOX_MANAGER_SERVICE_CLASS);
                                                                    t.traceEnd();
                                                                    t.traceBegin("StartAdServicesManagerService");
                                                                    this.mSystemServiceManager.startService(AD_SERVICES_MANAGER_SERVICE_CLASS);
                                                                    t.traceEnd();
                                                                    if (safeMode) {
                                                                    }
                                                                    if (this.mPackageManager.hasSystemFeature("android.hardware.telephony")) {
                                                                    }
                                                                    if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                                                    }
                                                                    if (deviceHasConfigString(context, 17039947)) {
                                                                    }
                                                                    t.traceBegin("StartClipboardService");
                                                                    this.mSystemServiceManager.startService(ClipboardService.class);
                                                                    t.traceEnd();
                                                                    t.traceBegin("AppServiceManager");
                                                                    this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                                                                    t.traceEnd();
                                                                    sMtkSystemServerIns.startMtkOtherServices();
                                                                    t.traceBegin("startTracingServiceProxy");
                                                                    this.mSystemServiceManager.startService(TracingServiceProxy.class);
                                                                    t.traceEnd();
                                                                    t.traceBegin("MakeLockSettingsServiceReady");
                                                                    if (lockSettings != null) {
                                                                    }
                                                                    t.traceEnd();
                                                                    t.traceBegin("StartBootPhaseLockSettingsReady");
                                                                    this.mSystemServiceManager.startBootPhase(t, SystemService.PHASE_LOCK_SETTINGS_READY);
                                                                    t.traceEnd();
                                                                    t.traceBegin("StartBootPhaseSystemServicesReady");
                                                                    this.mSystemServiceManager.startBootPhase(t, 500);
                                                                    t.traceEnd();
                                                                    t.traceBegin("MakeWindowManagerServiceReady");
                                                                    main.systemReady();
                                                                    t.traceEnd();
                                                                    synchronized (SystemService.class) {
                                                                    }
                                                                }
                                                            } catch (Throwable th16) {
                                                                e = th16;
                                                            }
                                                            t.traceEnd();
                                                            networkTimeUpdater3 = networkTimeUpdater2;
                                                        }
                                                        t.traceBegin("CertBlacklister");
                                                        new CertBlacklister(context);
                                                        t.traceEnd();
                                                        t.traceBegin("StartEmergencyAffordanceService");
                                                        this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                                                        t.traceEnd();
                                                        t.traceBegin(START_BLOB_STORE_SERVICE);
                                                        this.mSystemServiceManager.startService(BLOB_STORE_MANAGER_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StartDreamManager");
                                                        this.mSystemServiceManager.startService(DreamManagerService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("AddGraphicsStatsService");
                                                        ServiceManager.addService("graphicsstats", new GraphicsStatsService(context));
                                                        t.traceEnd();
                                                        if (CoverageService.ENABLED) {
                                                        }
                                                        if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                                                        }
                                                        t.traceBegin("StartAttestationVerificationService");
                                                        this.mSystemServiceManager.startService(AttestationVerificationManagerService.class);
                                                        t.traceEnd();
                                                        if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                                                        }
                                                        t.traceBegin("StartRestrictionManager");
                                                        this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartMediaSessionService");
                                                        this.mSystemServiceManager.startService(MEDIA_SESSION_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                                                        }
                                                        if (!this.mPackageManager.hasSystemFeature("android.software.live_tv")) {
                                                        }
                                                        t.traceBegin("StartTvInteractiveAppManager");
                                                        this.mSystemServiceManager.startService(TvInteractiveAppManagerService.class);
                                                        t.traceEnd();
                                                        if (!this.mPackageManager.hasSystemFeature("android.software.live_tv")) {
                                                        }
                                                        t.traceBegin("StartTvInputManager");
                                                        this.mSystemServiceManager.startService(TvInputManagerService.class);
                                                        t.traceEnd();
                                                        if (this.mPackageManager.hasSystemFeature("android.hardware.tv.tuner")) {
                                                        }
                                                        if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                                                        }
                                                        if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                                                        }
                                                        t.traceBegin("StartMediaRouterService");
                                                        mediaRouterService = new MediaRouterService(context);
                                                        ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                                                        mediaRouter2 = mediaRouterService;
                                                        t.traceEnd();
                                                        hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                                                        hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                                                        hasFeatureFingerprint = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                                                        if (hasFeatureFace) {
                                                        }
                                                        if (hasFeatureIris) {
                                                        }
                                                        if (hasFeatureFingerprint) {
                                                        }
                                                        t.traceBegin("StartBiometricService");
                                                        this.mSystemServiceManager.startService(BiometricService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartAuthService");
                                                        this.mSystemServiceManager.startService(AuthService.class);
                                                        t.traceEnd();
                                                        if (!isWatch) {
                                                        }
                                                        if (!isWatch) {
                                                        }
                                                        t.traceBegin("StartShortcutServiceLifecycle");
                                                        this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartLauncherAppsService");
                                                        this.mSystemServiceManager.startService(LauncherAppsService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartCrossProfileAppsService");
                                                        this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartPeopleService");
                                                        this.mSystemServiceManager.startService(PeopleService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StartMediaMetricsManager");
                                                        this.mSystemServiceManager.startService(MediaMetricsManagerService.class);
                                                        t.traceEnd();
                                                        stub2 = stub10;
                                                        iNetworkManagementService = iNetworkManagementService2;
                                                        stub3 = stub5;
                                                        stub4 = stub6;
                                                        networkPolicy = r15;
                                                        lockSettings = lockSettings2;
                                                        networkTimeUpdater = networkTimeUpdater3;
                                                        mediaRouter = mediaRouter2;
                                                        t.traceBegin("StartMediaProjectionManager");
                                                        this.mSystemServiceManager.startService(MediaProjectionManagerService.class);
                                                        t.traceEnd();
                                                        if (isWatch) {
                                                        }
                                                        if (!this.mPackageManager.hasSystemFeature("android.software.slices_disabled")) {
                                                        }
                                                        if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                                        }
                                                        t.traceBegin("StartStatsCompanion");
                                                        this.mSystemServiceManager.startServiceFromJar(STATS_COMPANION_LIFECYCLE_CLASS, STATS_COMPANION_APEX_PATH);
                                                        t.traceEnd();
                                                        t.traceBegin("StartRebootReadinessManagerService");
                                                        this.mSystemServiceManager.startServiceFromJar(REBOOT_READINESS_LIFECYCLE_CLASS, SCHEDULING_APEX_PATH);
                                                        t.traceEnd();
                                                        t.traceBegin("StartStatsPullAtomService");
                                                        this.mSystemServiceManager.startService(STATS_PULL_ATOM_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StatsBootstrapAtomService");
                                                        this.mSystemServiceManager.startService(STATS_BOOTSTRAP_ATOM_SERVICE_LIFECYCLE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StartIncidentCompanionService");
                                                        this.mSystemServiceManager.startService(IncidentCompanionService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("StarSdkSandboxManagerService");
                                                        this.mSystemServiceManager.startService(SDK_SANDBOX_MANAGER_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        t.traceBegin("StartAdServicesManagerService");
                                                        this.mSystemServiceManager.startService(AD_SERVICES_MANAGER_SERVICE_CLASS);
                                                        t.traceEnd();
                                                        if (safeMode) {
                                                        }
                                                        if (this.mPackageManager.hasSystemFeature("android.hardware.telephony")) {
                                                        }
                                                        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                                        }
                                                        if (deviceHasConfigString(context, 17039947)) {
                                                        }
                                                        t.traceBegin("StartClipboardService");
                                                        this.mSystemServiceManager.startService(ClipboardService.class);
                                                        t.traceEnd();
                                                        t.traceBegin("AppServiceManager");
                                                        this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                                                        t.traceEnd();
                                                        sMtkSystemServerIns.startMtkOtherServices();
                                                        t.traceBegin("startTracingServiceProxy");
                                                        this.mSystemServiceManager.startService(TracingServiceProxy.class);
                                                        t.traceEnd();
                                                        t.traceBegin("MakeLockSettingsServiceReady");
                                                        if (lockSettings != null) {
                                                        }
                                                        t.traceEnd();
                                                        t.traceBegin("StartBootPhaseLockSettingsReady");
                                                        this.mSystemServiceManager.startBootPhase(t, SystemService.PHASE_LOCK_SETTINGS_READY);
                                                        t.traceEnd();
                                                        t.traceBegin("StartBootPhaseSystemServicesReady");
                                                        this.mSystemServiceManager.startBootPhase(t, 500);
                                                        t.traceEnd();
                                                        t.traceBegin("MakeWindowManagerServiceReady");
                                                        main.systemReady();
                                                        t.traceEnd();
                                                        synchronized (SystemService.class) {
                                                        }
                                                    }
                                                } catch (Throwable th17) {
                                                    e = th17;
                                                }
                                                t.traceEnd();
                                                iBinder2 = iBinder;
                                            }
                                            t.traceBegin("StartHardwarePropertiesManagerService");
                                            try {
                                                hardwarePropertiesManagerService = new HardwarePropertiesManagerService(context);
                                                ServiceManager.addService("hardware_properties", hardwarePropertiesManagerService);
                                                stub8 = hardwarePropertiesManagerService;
                                            } catch (Throwable th18) {
                                                e = th18;
                                            }
                                            t.traceEnd();
                                            if (!isWatch) {
                                                t.traceBegin("StartTwilightService");
                                                this.mSystemServiceManager.startService(TwilightService.class);
                                                t.traceEnd();
                                            }
                                            t.traceBegin("StartColorDisplay");
                                            this.mSystemServiceManager.startService(ColorDisplayService.class);
                                            t.traceEnd();
                                            t.traceBegin("StartJobScheduler");
                                            this.mSystemServiceManager.startService(JOB_SCHEDULER_SERVICE_CLASS);
                                            t.traceEnd();
                                            t.traceBegin("StartSoundTrigger");
                                            this.mSystemServiceManager.startService(SoundTriggerService.class);
                                            t.traceEnd();
                                            t.traceBegin("StartTrustManager");
                                            this.mSystemServiceManager.startService(TrustManagerService.class);
                                            t.traceEnd();
                                            if (this.mPackageManager.hasSystemFeature("android.software.backup")) {
                                                t.traceBegin("StartBackupManager");
                                                this.mSystemServiceManager.startService(BACKUP_MANAGER_SERVICE_CLASS);
                                                t.traceEnd();
                                            }
                                            if (!this.mPackageManager.hasSystemFeature("android.software.app_widgets") || context.getResources().getBoolean(17891625)) {
                                                t.traceBegin("StartAppWidgetService");
                                                this.mSystemServiceManager.startService(APPWIDGET_SERVICE_CLASS);
                                                t.traceEnd();
                                            }
                                            t.traceBegin("StartVoiceRecognitionManager");
                                            this.mSystemServiceManager.startService(VOICE_RECOGNITION_MANAGER_SERVICE_CLASS);
                                            t.traceEnd();
                                            t.traceBegin("StartAppHibernationService");
                                            this.mSystemServiceManager.startService(APP_HIBERNATION_SERVICE_CLASS);
                                            t.traceEnd();
                                            if (GestureLauncherService.isGestureLauncherEnabled(context.getResources())) {
                                                t.traceBegin("StartGestureLauncher");
                                                this.mSystemServiceManager.startService(GestureLauncherService.class);
                                                t.traceEnd();
                                            }
                                            t.traceBegin("StartSensorNotification");
                                            this.mSystemServiceManager.startService(SensorNotificationService.class);
                                            t.traceEnd();
                                            if (this.mPackageManager.hasSystemFeature("android.hardware.context_hub")) {
                                                t.traceBegin("StartContextHubSystemService");
                                                this.mSystemServiceManager.startService(ContextHubSystemService.class);
                                                t.traceEnd();
                                            }
                                            t.traceBegin("StartDiskStatsService");
                                            try {
                                                ServiceManager.addService("diskstats", new DiskStatsService(context));
                                            } catch (Throwable e24) {
                                                reportWtf("starting DiskStats Service", e24);
                                            }
                                            t.traceEnd();
                                            t.traceBegin("RuntimeService");
                                            try {
                                                ServiceManager.addService("runtime", new RuntimeService(context));
                                            } catch (Throwable e25) {
                                                reportWtf("starting RuntimeService", e25);
                                            }
                                            t.traceEnd();
                                            startRulesManagerService = this.mOnlyCore && context.getResources().getBoolean(17891650);
                                            if (startRulesManagerService) {
                                                t.traceBegin("StartTimeZoneRulesManagerService");
                                                this.mSystemServiceManager.startService(TIME_ZONE_RULES_MANAGER_SERVICE_CLASS);
                                                t.traceEnd();
                                            }
                                            if (!isWatch && !disableNetworkTime) {
                                                t.traceBegin("StartNetworkTimeUpdateService");
                                                networkTimeUpdater2 = new NetworkTimeUpdateService(context);
                                                ServiceManager.addService("network_time_update_service", networkTimeUpdater2);
                                                t.traceEnd();
                                                networkTimeUpdater3 = networkTimeUpdater2;
                                            }
                                            t.traceBegin("CertBlacklister");
                                            try {
                                                new CertBlacklister(context);
                                            } catch (Throwable e26) {
                                                reportWtf("starting CertBlacklister", e26);
                                            }
                                            t.traceEnd();
                                            t.traceBegin("StartEmergencyAffordanceService");
                                            this.mSystemServiceManager.startService(EmergencyAffordanceService.class);
                                            t.traceEnd();
                                            t.traceBegin(START_BLOB_STORE_SERVICE);
                                            this.mSystemServiceManager.startService(BLOB_STORE_MANAGER_SERVICE_CLASS);
                                            t.traceEnd();
                                            t.traceBegin("StartDreamManager");
                                            this.mSystemServiceManager.startService(DreamManagerService.class);
                                            t.traceEnd();
                                            t.traceBegin("AddGraphicsStatsService");
                                            ServiceManager.addService("graphicsstats", new GraphicsStatsService(context));
                                            t.traceEnd();
                                            if (CoverageService.ENABLED) {
                                                t.traceBegin("AddCoverageService");
                                                ServiceManager.addService(CoverageService.COVERAGE_SERVICE, new CoverageService());
                                                t.traceEnd();
                                            }
                                            if (this.mPackageManager.hasSystemFeature("android.software.print")) {
                                                t.traceBegin("StartPrintManager");
                                                this.mSystemServiceManager.startService(PRINT_MANAGER_SERVICE_CLASS);
                                                t.traceEnd();
                                            }
                                            t.traceBegin("StartAttestationVerificationService");
                                            this.mSystemServiceManager.startService(AttestationVerificationManagerService.class);
                                            t.traceEnd();
                                            if (this.mPackageManager.hasSystemFeature("android.software.companion_device_setup")) {
                                                t.traceBegin("StartCompanionDeviceManager");
                                                this.mSystemServiceManager.startService(COMPANION_DEVICE_MANAGER_SERVICE_CLASS);
                                                t.traceEnd();
                                                t.traceBegin("StartVirtualDeviceManager");
                                                this.mSystemServiceManager.startService(VIRTUAL_DEVICE_MANAGER_SERVICE_CLASS);
                                                t.traceEnd();
                                            }
                                            t.traceBegin("StartRestrictionManager");
                                            this.mSystemServiceManager.startService(RestrictionsManagerService.class);
                                            t.traceEnd();
                                            t.traceBegin("StartMediaSessionService");
                                            this.mSystemServiceManager.startService(MEDIA_SESSION_SERVICE_CLASS);
                                            t.traceEnd();
                                            if (this.mPackageManager.hasSystemFeature("android.hardware.hdmi.cec")) {
                                                t.traceBegin("StartHdmiControlService");
                                                this.mSystemServiceManager.startService(HdmiControlService.class);
                                                t.traceEnd();
                                            }
                                            if (!this.mPackageManager.hasSystemFeature("android.software.live_tv") || this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                                                t.traceBegin("StartTvInteractiveAppManager");
                                                this.mSystemServiceManager.startService(TvInteractiveAppManagerService.class);
                                                t.traceEnd();
                                            }
                                            if (!this.mPackageManager.hasSystemFeature("android.software.live_tv") || this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                                                t.traceBegin("StartTvInputManager");
                                                this.mSystemServiceManager.startService(TvInputManagerService.class);
                                                t.traceEnd();
                                            }
                                            if (this.mPackageManager.hasSystemFeature("android.hardware.tv.tuner")) {
                                                t.traceBegin("StartTunerResourceManager");
                                                this.mSystemServiceManager.startService(TunerResourceManagerService.class);
                                                t.traceEnd();
                                            }
                                            if (this.mPackageManager.hasSystemFeature("android.software.picture_in_picture")) {
                                                t.traceBegin("StartMediaResourceMonitor");
                                                this.mSystemServiceManager.startService(MEDIA_RESOURCE_MONITOR_SERVICE_CLASS);
                                                t.traceEnd();
                                            }
                                            if (this.mPackageManager.hasSystemFeature("android.software.leanback")) {
                                                t.traceBegin("StartTvRemoteService");
                                                this.mSystemServiceManager.startService(TvRemoteService.class);
                                                t.traceEnd();
                                            }
                                            t.traceBegin("StartMediaRouterService");
                                            try {
                                                mediaRouterService = new MediaRouterService(context);
                                                ServiceManager.addService("media_router", (IBinder) mediaRouterService);
                                                mediaRouter2 = mediaRouterService;
                                            } catch (Throwable th19) {
                                                e = th19;
                                            }
                                            t.traceEnd();
                                            hasFeatureFace = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.face");
                                            hasFeatureIris = this.mPackageManager.hasSystemFeature("android.hardware.biometrics.iris");
                                            hasFeatureFingerprint = this.mPackageManager.hasSystemFeature("android.hardware.fingerprint");
                                            if (hasFeatureFace) {
                                                t.traceBegin("StartFaceSensor");
                                                FaceService faceService = (FaceService) this.mSystemServiceManager.startService(FaceService.class);
                                                t.traceEnd();
                                            }
                                            if (hasFeatureIris) {
                                                t.traceBegin("StartIrisSensor");
                                                this.mSystemServiceManager.startService(IrisService.class);
                                                t.traceEnd();
                                            }
                                            if (hasFeatureFingerprint) {
                                                t.traceBegin("StartFingerprintSensor");
                                                FingerprintService fingerprintService = (FingerprintService) this.mSystemServiceManager.startService(FingerprintService.class);
                                                t.traceEnd();
                                            }
                                            t.traceBegin("StartBiometricService");
                                            this.mSystemServiceManager.startService(BiometricService.class);
                                            t.traceEnd();
                                            t.traceBegin("StartAuthService");
                                            this.mSystemServiceManager.startService(AuthService.class);
                                            t.traceEnd();
                                            if (!isWatch) {
                                                t.traceBegin("StartDynamicCodeLoggingService");
                                                try {
                                                    DynamicCodeLoggingService.schedule(context);
                                                } catch (Throwable e27) {
                                                    reportWtf("starting DynamicCodeLoggingService", e27);
                                                }
                                                t.traceEnd();
                                            }
                                            if (!isWatch) {
                                                t.traceBegin("StartPruneInstantAppsJobService");
                                                try {
                                                    PruneInstantAppsJobService.schedule(context);
                                                } catch (Throwable e28) {
                                                    reportWtf("StartPruneInstantAppsJobService", e28);
                                                }
                                                t.traceEnd();
                                            }
                                            t.traceBegin("StartShortcutServiceLifecycle");
                                            this.mSystemServiceManager.startService(ShortcutService.Lifecycle.class);
                                            t.traceEnd();
                                            t.traceBegin("StartLauncherAppsService");
                                            this.mSystemServiceManager.startService(LauncherAppsService.class);
                                            t.traceEnd();
                                            t.traceBegin("StartCrossProfileAppsService");
                                            this.mSystemServiceManager.startService(CrossProfileAppsService.class);
                                            t.traceEnd();
                                            t.traceBegin("StartPeopleService");
                                            this.mSystemServiceManager.startService(PeopleService.class);
                                            t.traceEnd();
                                            t.traceBegin("StartMediaMetricsManager");
                                            this.mSystemServiceManager.startService(MediaMetricsManagerService.class);
                                            t.traceEnd();
                                            stub2 = stub10;
                                            iNetworkManagementService = iNetworkManagementService2;
                                            stub3 = stub5;
                                            stub4 = stub6;
                                            networkPolicy = r15;
                                            lockSettings = lockSettings2;
                                            networkTimeUpdater = networkTimeUpdater3;
                                            mediaRouter = mediaRouter2;
                                        }
                                        t.traceBegin("StartMediaProjectionManager");
                                        this.mSystemServiceManager.startService(MediaProjectionManagerService.class);
                                        t.traceEnd();
                                        if (isWatch) {
                                            t.traceBegin("StartWearPowerService");
                                            this.mSystemServiceManager.startService(WEAR_POWER_SERVICE_CLASS);
                                            t.traceEnd();
                                            t.traceBegin("StartHealthService");
                                            this.mSystemServiceManager.startService(HEALTH_SERVICE_CLASS);
                                            t.traceEnd();
                                            t.traceBegin("StartWearConnectivityService");
                                            this.mSystemServiceManager.startService(WEAR_CONNECTIVITY_SERVICE_CLASS);
                                            t.traceEnd();
                                            t.traceBegin("StartWearDisplayService");
                                            this.mSystemServiceManager.startService(WEAR_DISPLAY_SERVICE_CLASS);
                                            t.traceEnd();
                                            t.traceBegin("StartWearTimeService");
                                            this.mSystemServiceManager.startService(WEAR_TIME_SERVICE_CLASS);
                                            t.traceEnd();
                                            if (enableLeftyService) {
                                                t.traceBegin("StartWearLeftyService");
                                                this.mSystemServiceManager.startService(WEAR_LEFTY_SERVICE_CLASS);
                                                t.traceEnd();
                                            }
                                            t.traceBegin("StartWearGlobalActionsService");
                                            this.mSystemServiceManager.startService(WEAR_GLOBAL_ACTIONS_SERVICE_CLASS);
                                            t.traceEnd();
                                        }
                                        if (!this.mPackageManager.hasSystemFeature("android.software.slices_disabled")) {
                                            t.traceBegin("StartSliceManagerService");
                                            this.mSystemServiceManager.startService(SLICE_MANAGER_SERVICE_CLASS);
                                            t.traceEnd();
                                        }
                                        if (context.getPackageManager().hasSystemFeature("android.hardware.type.embedded")) {
                                            t.traceBegin("StartIoTSystemService");
                                            this.mSystemServiceManager.startService(IOT_SERVICE_CLASS);
                                            t.traceEnd();
                                        }
                                        t.traceBegin("StartStatsCompanion");
                                        this.mSystemServiceManager.startServiceFromJar(STATS_COMPANION_LIFECYCLE_CLASS, STATS_COMPANION_APEX_PATH);
                                        t.traceEnd();
                                        t.traceBegin("StartRebootReadinessManagerService");
                                        this.mSystemServiceManager.startServiceFromJar(REBOOT_READINESS_LIFECYCLE_CLASS, SCHEDULING_APEX_PATH);
                                        t.traceEnd();
                                        t.traceBegin("StartStatsPullAtomService");
                                        this.mSystemServiceManager.startService(STATS_PULL_ATOM_SERVICE_CLASS);
                                        t.traceEnd();
                                        t.traceBegin("StatsBootstrapAtomService");
                                        this.mSystemServiceManager.startService(STATS_BOOTSTRAP_ATOM_SERVICE_LIFECYCLE_CLASS);
                                        t.traceEnd();
                                        t.traceBegin("StartIncidentCompanionService");
                                        this.mSystemServiceManager.startService(IncidentCompanionService.class);
                                        t.traceEnd();
                                        t.traceBegin("StarSdkSandboxManagerService");
                                        this.mSystemServiceManager.startService(SDK_SANDBOX_MANAGER_SERVICE_CLASS);
                                        t.traceEnd();
                                        t.traceBegin("StartAdServicesManagerService");
                                        this.mSystemServiceManager.startService(AD_SERVICES_MANAGER_SERVICE_CLASS);
                                        t.traceEnd();
                                        if (safeMode) {
                                            this.mActivityManagerService.enterSafeMode();
                                        }
                                        if (this.mPackageManager.hasSystemFeature("android.hardware.telephony")) {
                                            t.traceBegin("StartMmsService");
                                            MmsServiceBroker mmsService2 = (MmsServiceBroker) this.mSystemServiceManager.startService(MmsServiceBroker.class);
                                            t.traceEnd();
                                            mmsService = mmsService2;
                                        } else {
                                            mmsService = null;
                                        }
                                        if (this.mPackageManager.hasSystemFeature("android.software.autofill")) {
                                            t.traceBegin("StartAutoFillService");
                                            this.mSystemServiceManager.startService(AUTO_FILL_MANAGER_SERVICE_CLASS);
                                            t.traceEnd();
                                        }
                                        if (deviceHasConfigString(context, 17039947)) {
                                            t.traceBegin("StartTranslationManagerService");
                                            this.mSystemServiceManager.startService(TRANSLATION_MANAGER_SERVICE_CLASS);
                                            t.traceEnd();
                                        } else {
                                            Slog.d(TAG, "TranslationService not defined by OEM");
                                        }
                                        t.traceBegin("StartClipboardService");
                                        this.mSystemServiceManager.startService(ClipboardService.class);
                                        t.traceEnd();
                                        t.traceBegin("AppServiceManager");
                                        this.mSystemServiceManager.startService(AppBindingService.Lifecycle.class);
                                        t.traceEnd();
                                        sMtkSystemServerIns.startMtkOtherServices();
                                        t.traceBegin("startTracingServiceProxy");
                                        this.mSystemServiceManager.startService(TracingServiceProxy.class);
                                        t.traceEnd();
                                        t.traceBegin("MakeLockSettingsServiceReady");
                                        if (lockSettings != null) {
                                            try {
                                                lockSettings.systemReady();
                                            } catch (Throwable e29) {
                                                reportWtf("making Lock Settings Service ready", e29);
                                            }
                                        }
                                        t.traceEnd();
                                        t.traceBegin("StartBootPhaseLockSettingsReady");
                                        this.mSystemServiceManager.startBootPhase(t, SystemService.PHASE_LOCK_SETTINGS_READY);
                                        t.traceEnd();
                                        t.traceBegin("StartBootPhaseSystemServicesReady");
                                        this.mSystemServiceManager.startBootPhase(t, 500);
                                        t.traceEnd();
                                        t.traceBegin("MakeWindowManagerServiceReady");
                                        try {
                                            main.systemReady();
                                        } catch (Throwable e30) {
                                            reportWtf("making Window Manager Service ready", e30);
                                        }
                                        t.traceEnd();
                                        synchronized (SystemService.class) {
                                            try {
                                                LinkedList<Pair<String, ApplicationErrorReport.CrashInfo>> linkedList = sPendingWtfs;
                                                if (linkedList != null) {
                                                    try {
                                                        this.mActivityManagerService.schedulePendingSystemServerWtfs(linkedList);
                                                        sPendingWtfs = null;
                                                    } catch (Throwable th20) {
                                                        th = th20;
                                                        while (true) {
                                                            try {
                                                                break;
                                                            } catch (Throwable th21) {
                                                                th = th21;
                                                            }
                                                        }
                                                        throw th;
                                                    }
                                                }
                                                if (safeMode) {
                                                    this.mActivityManagerService.showSafeModeOverlay();
                                                }
                                                ITranSystemServer.Instance().startMagellan(context);
                                                Configuration config = main.computeNewConfiguration(0);
                                                DisplayMetrics metrics = new DisplayMetrics();
                                                context.getDisplay().getMetrics(metrics);
                                                context.getResources().updateConfiguration(config, metrics);
                                                Resources.Theme systemTheme = context.getTheme();
                                                if (systemTheme.getChangingConfigurations() != 0) {
                                                    systemTheme.rebase();
                                                }
                                                t.traceBegin("StartPermissionPolicyService");
                                                this.mSystemServiceManager.startService(PermissionPolicyService.class);
                                                t.traceEnd();
                                                t.traceBegin("MakePackageManagerServiceReady");
                                                this.mPackageManagerService.systemReady();
                                                t.traceEnd();
                                                t.traceBegin("MakeDisplayManagerServiceReady");
                                                try {
                                                    this.mDisplayManagerService.systemReady(safeMode, this.mOnlyCore);
                                                } catch (Throwable e31) {
                                                    reportWtf("making Display Manager Service ready", e31);
                                                }
                                                t.traceEnd();
                                                this.mSystemServiceManager.setSafeMode(safeMode);
                                                t.traceBegin("StartDeviceSpecificServices");
                                                String[] classes = this.mSystemContext.getResources().getStringArray(17236026);
                                                for (String className2 : classes) {
                                                    t.traceBegin("StartDeviceSpecificServices " + className2);
                                                    try {
                                                        this.mSystemServiceManager.startService(className2);
                                                    } catch (Throwable e32) {
                                                        reportWtf("starting " + className2, e32);
                                                    }
                                                    t.traceEnd();
                                                }
                                                t.traceEnd();
                                                t.traceBegin(GameManagerService.TAG);
                                                this.mSystemServiceManager.startService(GAME_MANAGER_SERVICE_CLASS);
                                                t.traceEnd();
                                                t.traceBegin("ArtManagerLocal");
                                                LocalManagerRegistry.addManager(ArtManagerLocal.class, new ArtManagerLocal());
                                                t.traceEnd();
                                                if (context.getPackageManager().hasSystemFeature("android.hardware.uwb")) {
                                                    t.traceBegin("UwbService");
                                                    this.mSystemServiceManager.startServiceFromJar(UWB_SERVICE_CLASS, UWB_APEX_SERVICE_JAR_PATH);
                                                    t.traceEnd();
                                                }
                                                t.traceBegin("StartBootPhaseDeviceSpecificServicesReady");
                                                this.mSystemServiceManager.startBootPhase(t, SystemService.PHASE_DEVICE_SPECIFIC_SERVICES_READY);
                                                t.traceEnd();
                                                t.traceBegin("StartSafetyCenterService");
                                                this.mSystemServiceManager.startService(SAFETY_CENTER_SERVICE_CLASS);
                                                t.traceEnd();
                                                t.traceBegin("AppSearchModule");
                                                this.mSystemServiceManager.startService(APPSEARCH_MODULE_LIFECYCLE_CLASS);
                                                t.traceEnd();
                                                if (SystemProperties.getBoolean("ro.config.isolated_compilation_enabled", false)) {
                                                    t.traceBegin("IsolatedCompilationService");
                                                    this.mSystemServiceManager.startService(ISOLATED_COMPILATION_SERVICE_CLASS);
                                                    t.traceEnd();
                                                }
                                                t.traceBegin("StartMediaCommunicationService");
                                                this.mSystemServiceManager.startService(MEDIA_COMMUNICATION_SERVICE_CLASS);
                                                t.traceEnd();
                                                if (mXmlSupport) {
                                                    VerifyExt filesVerify = VerifyFacotry.getInstance().getVerifyExt();
                                                    filesVerify.registerAthena(context);
                                                }
                                                t.traceBegin("AppCompatOverridesService");
                                                this.mSystemServiceManager.startService(APP_COMPAT_OVERRIDES_SERVICE_CLASS);
                                                t.traceEnd();
                                                if (Build.TRAN_AIPOWERLAB_SUPPORT) {
                                                    t.traceBegin("StartTranAipowerlabService");
                                                    try {
                                                        ?? tranAipowerlabService2 = new TranAipowerlabService(context);
                                                        try {
                                                            ServiceManager.addService("tran_aipl", (IBinder) tranAipowerlabService2);
                                                            tranAipowerlabService = tranAipowerlabService2;
                                                        } catch (Throwable th22) {
                                                            e = th22;
                                                            reportWtf("starting TranAipowerlabService:", e);
                                                            tranAipowerlabService = null;
                                                            t.traceEnd();
                                                            if (Build.TRAN_POWERHUB_SUPPORT) {
                                                            }
                                                            if (TranFoldingScreenManager.isFoldableDevice()) {
                                                            }
                                                            final INetworkManagementService iNetworkManagementService3 = iNetworkManagementService;
                                                            final NetworkPolicyManagerService networkPolicyF = networkPolicy;
                                                            final ?? r11 = stub2;
                                                            final NetworkTimeUpdateService networkTimeUpdaterF = networkTimeUpdater;
                                                            final MediaRouterService mediaRouterF = mediaRouter;
                                                            final MmsServiceBroker mmsServiceF = mmsService;
                                                            final ?? r9 = stub3;
                                                            final ?? r10 = stub4;
                                                            final ConnectivityManager connectivityF = (ConnectivityManager) context.getSystemService("connectivity");
                                                            final TranAipowerlabService tranAipowerlabServiceF = tranAipowerlabService;
                                                            final TranPowerhubService tranPowerhubServiceF = tranPowerhubService;
                                                            final DevicePolicyManagerService.Lifecycle lifecycle = dpms;
                                                            this.mActivityManagerService.systemReady(new Runnable() { // from class: com.android.server.SystemServer$$ExternalSyntheticLambda7
                                                                @Override // java.lang.Runnable
                                                                public final void run() {
                                                                    SystemServer.this.m426lambda$startOtherServices$5$comandroidserverSystemServer(t, lifecycle, safeMode, connectivityF, iNetworkManagementService3, networkPolicyF, r9, r10, r11, networkTimeUpdaterF, inputManagerService, telephonyRegistry, mediaRouterF, mmsServiceF, tranAipowerlabServiceF, tranPowerhubServiceF, context);
                                                                }
                                                            }, t);
                                                            t.traceBegin("StartSystemUI");
                                                            startSystemUi(context, main);
                                                            t.traceEnd();
                                                            t.traceBegin("StartUxdetector");
                                                            UxDetectorFactory.init(context);
                                                            t.traceEnd();
                                                            t.traceEnd();
                                                        }
                                                    } catch (Throwable th23) {
                                                        e = th23;
                                                    }
                                                    t.traceEnd();
                                                }
                                                if (Build.TRAN_POWERHUB_SUPPORT) {
                                                    t.traceBegin("StartTranPowerhubService");
                                                    try {
                                                        ?? tranPowerhubService2 = new TranPowerhubService(context);
                                                        try {
                                                            ServiceManager.addService("tran_pwhub", (IBinder) tranPowerhubService2);
                                                            tranPowerhubService = tranPowerhubService2;
                                                        } catch (Throwable th24) {
                                                            e = th24;
                                                            reportWtf("starting TranPowerhubService:", e);
                                                            tranPowerhubService = null;
                                                            t.traceEnd();
                                                            if (TranFoldingScreenManager.isFoldableDevice()) {
                                                            }
                                                            final NetworkManagementService iNetworkManagementService32 = iNetworkManagementService;
                                                            final NetworkPolicyManagerService networkPolicyF2 = networkPolicy;
                                                            final CountryDetectorService r112 = stub2;
                                                            final NetworkTimeUpdateService networkTimeUpdaterF2 = networkTimeUpdater;
                                                            final MediaRouterService mediaRouterF2 = mediaRouter;
                                                            final MmsServiceBroker mmsServiceF2 = mmsService;
                                                            final VpnManagerService r92 = stub3;
                                                            final VcnManagementService r102 = stub4;
                                                            final ConnectivityManager connectivityF2 = (ConnectivityManager) context.getSystemService("connectivity");
                                                            final TranAipowerlabService tranAipowerlabServiceF2 = tranAipowerlabService;
                                                            final TranPowerhubService tranPowerhubServiceF2 = tranPowerhubService;
                                                            final DevicePolicyManagerService.Lifecycle lifecycle2 = dpms;
                                                            this.mActivityManagerService.systemReady(new Runnable() { // from class: com.android.server.SystemServer$$ExternalSyntheticLambda7
                                                                @Override // java.lang.Runnable
                                                                public final void run() {
                                                                    SystemServer.this.m426lambda$startOtherServices$5$comandroidserverSystemServer(t, lifecycle2, safeMode, connectivityF2, iNetworkManagementService32, networkPolicyF2, r92, r102, r112, networkTimeUpdaterF2, inputManagerService, telephonyRegistry, mediaRouterF2, mmsServiceF2, tranAipowerlabServiceF2, tranPowerhubServiceF2, context);
                                                                }
                                                            }, t);
                                                            t.traceBegin("StartSystemUI");
                                                            startSystemUi(context, main);
                                                            t.traceEnd();
                                                            t.traceBegin("StartUxdetector");
                                                            UxDetectorFactory.init(context);
                                                            t.traceEnd();
                                                            t.traceEnd();
                                                        }
                                                    } catch (Throwable th25) {
                                                        e = th25;
                                                    }
                                                    t.traceEnd();
                                                }
                                                if (TranFoldingScreenManager.isFoldableDevice()) {
                                                    t.traceBegin("StartTranFoldingScreenService");
                                                    try {
                                                        try {
                                                            ServiceManager.addService("tran_foldable", new TranFoldingScreenService(context));
                                                        } catch (Throwable th26) {
                                                            e = th26;
                                                            reportWtf("starting TranFoldingScreenService:", e);
                                                            t.traceEnd();
                                                            final NetworkManagementService iNetworkManagementService322 = iNetworkManagementService;
                                                            final NetworkPolicyManagerService networkPolicyF22 = networkPolicy;
                                                            final CountryDetectorService r1122 = stub2;
                                                            final NetworkTimeUpdateService networkTimeUpdaterF22 = networkTimeUpdater;
                                                            final MediaRouterService mediaRouterF22 = mediaRouter;
                                                            final MmsServiceBroker mmsServiceF22 = mmsService;
                                                            final VpnManagerService r922 = stub3;
                                                            final VcnManagementService r1022 = stub4;
                                                            final ConnectivityManager connectivityF22 = (ConnectivityManager) context.getSystemService("connectivity");
                                                            final TranAipowerlabService tranAipowerlabServiceF22 = tranAipowerlabService;
                                                            final TranPowerhubService tranPowerhubServiceF22 = tranPowerhubService;
                                                            final DevicePolicyManagerService.Lifecycle lifecycle22 = dpms;
                                                            this.mActivityManagerService.systemReady(new Runnable() { // from class: com.android.server.SystemServer$$ExternalSyntheticLambda7
                                                                @Override // java.lang.Runnable
                                                                public final void run() {
                                                                    SystemServer.this.m426lambda$startOtherServices$5$comandroidserverSystemServer(t, lifecycle22, safeMode, connectivityF22, iNetworkManagementService322, networkPolicyF22, r922, r1022, r1122, networkTimeUpdaterF22, inputManagerService, telephonyRegistry, mediaRouterF22, mmsServiceF22, tranAipowerlabServiceF22, tranPowerhubServiceF22, context);
                                                                }
                                                            }, t);
                                                            t.traceBegin("StartSystemUI");
                                                            startSystemUi(context, main);
                                                            t.traceEnd();
                                                            t.traceBegin("StartUxdetector");
                                                            UxDetectorFactory.init(context);
                                                            t.traceEnd();
                                                            t.traceEnd();
                                                        }
                                                    } catch (Throwable th27) {
                                                        e = th27;
                                                    }
                                                    t.traceEnd();
                                                }
                                                final NetworkManagementService iNetworkManagementService3222 = iNetworkManagementService;
                                                final NetworkPolicyManagerService networkPolicyF222 = networkPolicy;
                                                final CountryDetectorService r11222 = stub2;
                                                final NetworkTimeUpdateService networkTimeUpdaterF222 = networkTimeUpdater;
                                                final MediaRouterService mediaRouterF222 = mediaRouter;
                                                final MmsServiceBroker mmsServiceF222 = mmsService;
                                                final VpnManagerService r9222 = stub3;
                                                final VcnManagementService r10222 = stub4;
                                                final ConnectivityManager connectivityF222 = (ConnectivityManager) context.getSystemService("connectivity");
                                                final TranAipowerlabService tranAipowerlabServiceF222 = tranAipowerlabService;
                                                final TranPowerhubService tranPowerhubServiceF222 = tranPowerhubService;
                                                final DevicePolicyManagerService.Lifecycle lifecycle222 = dpms;
                                                this.mActivityManagerService.systemReady(new Runnable() { // from class: com.android.server.SystemServer$$ExternalSyntheticLambda7
                                                    @Override // java.lang.Runnable
                                                    public final void run() {
                                                        SystemServer.this.m426lambda$startOtherServices$5$comandroidserverSystemServer(t, lifecycle222, safeMode, connectivityF222, iNetworkManagementService3222, networkPolicyF222, r9222, r10222, r11222, networkTimeUpdaterF222, inputManagerService, telephonyRegistry, mediaRouterF222, mmsServiceF222, tranAipowerlabServiceF222, tranPowerhubServiceF222, context);
                                                    }
                                                }, t);
                                                t.traceBegin("StartSystemUI");
                                                try {
                                                    startSystemUi(context, main);
                                                } catch (Throwable e33) {
                                                    reportWtf("starting System UI", e33);
                                                }
                                                t.traceEnd();
                                                t.traceBegin("StartUxdetector");
                                                try {
                                                    UxDetectorFactory.init(context);
                                                } catch (Exception e34) {
                                                    e34.printStackTrace();
                                                    Slog.e(TAG, "start uxdetector service");
                                                }
                                                t.traceEnd();
                                                t.traceEnd();
                                            } catch (Throwable th28) {
                                                th = th28;
                                            }
                                        }
                                    } catch (Throwable th29) {
                                        e = th29;
                                    }
                                } catch (Throwable th30) {
                                    e = th30;
                                }
                            } catch (Throwable th31) {
                                e = th31;
                            }
                        } catch (Throwable th32) {
                            e = th32;
                        }
                    } catch (Throwable th33) {
                        e = th33;
                    }
                } catch (Throwable th34) {
                    e = th34;
                }
            } catch (Throwable th35) {
                e = th35;
            }
        } catch (Throwable th36) {
            e = th36;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$startOtherServices$1() {
        try {
            Slog.i(TAG, "SecondaryZygotePreload");
            TimingsTraceAndSlog traceLog = TimingsTraceAndSlog.newAsyncLog();
            traceLog.traceBegin("SecondaryZygotePreload");
            String[] abis32 = Build.SUPPORTED_32_BIT_ABIS;
            if (abis32.length > 0 && !Process.ZYGOTE_PROCESS.preloadDefault(abis32[0])) {
                Slog.e(TAG, "Unable to preload default resources for secondary");
            }
            traceLog.traceEnd();
        } catch (Exception ex) {
            Slog.e(TAG, "Exception preloading default resources", ex);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$startOtherServices$2() {
        TimingsTraceAndSlog traceLog = TimingsTraceAndSlog.newAsyncLog();
        traceLog.traceBegin(START_HIDL_SERVICES);
        startHidlServices();
        traceLog.traceEnd();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startOtherServices$5$com-android-server-SystemServer  reason: not valid java name */
    public /* synthetic */ void m426lambda$startOtherServices$5$comandroidserverSystemServer(TimingsTraceAndSlog t, DevicePolicyManagerService.Lifecycle dpms, boolean safeMode, ConnectivityManager connectivityF, NetworkManagementService networkManagementF, NetworkPolicyManagerService networkPolicyF, VpnManagerService vpnManagerF, VcnManagementService vcnManagementF, CountryDetectorService countryDetectorF, NetworkTimeUpdateService networkTimeUpdaterF, InputManagerService inputManagerF, TelephonyRegistry telephonyRegistryF, MediaRouterService mediaRouterF, MmsServiceBroker mmsServiceF, TranAipowerlabService tranAipowerlabServiceF, TranPowerhubService tranPowerhubServiceF, Context context) {
        Future<?> webviewPrep;
        CountDownLatch networkPolicyInitReadySignal;
        Slog.i(TAG, "Making services ready");
        t.traceBegin("StartActivityManagerReadyPhase");
        this.mSystemServiceManager.startBootPhase(t, SystemService.PHASE_ACTIVITY_MANAGER_READY);
        t.traceEnd();
        t.traceBegin("StartObservingNativeCrashes");
        try {
            this.mActivityManagerService.startObservingNativeCrashes();
        } catch (Throwable e) {
            reportWtf("observing native crashes", e);
        }
        t.traceEnd();
        t.traceBegin("RegisterAppOpsPolicy");
        try {
            this.mActivityManagerService.setAppOpsPolicy(new AppOpsPolicy(this.mSystemContext));
        } catch (Throwable e2) {
            reportWtf("registering app ops policy", e2);
        }
        t.traceEnd();
        if (!this.mOnlyCore && this.mWebViewUpdateService != null) {
            Future<?> webviewPrep2 = SystemServerInitThreadPool.submit(new Runnable() { // from class: com.android.server.SystemServer$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    SystemServer.this.m425lambda$startOtherServices$3$comandroidserverSystemServer();
                }
            }, "WebViewFactoryPreparation");
            webviewPrep = webviewPrep2;
        } else {
            webviewPrep = null;
        }
        boolean isAutomotive = this.mPackageManager.hasSystemFeature("android.hardware.type.automotive");
        if (isAutomotive) {
            t.traceBegin("StartCarServiceHelperService");
            Dumpable startService = this.mSystemServiceManager.startService(CAR_SERVICE_HELPER_SERVICE_CLASS);
            if (startService instanceof Dumpable) {
                this.mDumper.addDumpable(startService);
            }
            if (startService instanceof DevicePolicySafetyChecker) {
                dpms.setDevicePolicySafetyChecker((DevicePolicySafetyChecker) startService);
            }
            t.traceEnd();
        }
        if (safeMode) {
            t.traceBegin("EnableAirplaneModeInSafeMode");
            try {
                connectivityF.setAirplaneMode(true);
            } catch (Throwable e3) {
                reportWtf("enabling Airplane Mode during Safe Mode bootup", e3);
            }
            t.traceEnd();
        }
        t.traceBegin("MakeNetworkManagementServiceReady");
        if (networkManagementF != null) {
            try {
                networkManagementF.systemReady();
            } catch (Throwable e4) {
                reportWtf("making Network Managment Service ready", e4);
            }
        }
        if (networkPolicyF == null) {
            networkPolicyInitReadySignal = null;
        } else {
            CountDownLatch networkPolicyInitReadySignal2 = networkPolicyF.networkScoreAndNetworkManagementServiceReady();
            networkPolicyInitReadySignal = networkPolicyInitReadySignal2;
        }
        t.traceEnd();
        sMtkSystemServerIns.addBootEvent("SystemServer:NetworkStatsService systemReady");
        t.traceBegin("MakeConnectivityServiceReady");
        if (connectivityF != null) {
            try {
                connectivityF.systemReady();
            } catch (Throwable e5) {
                reportWtf("making Connectivity Service ready", e5);
            }
        }
        t.traceEnd();
        sMtkSystemServerIns.addBootEvent("SystemServer:ConnectivityService systemReady");
        t.traceBegin("MakeVpnManagerServiceReady");
        if (vpnManagerF != null) {
            try {
                vpnManagerF.systemReady();
            } catch (Throwable e6) {
                reportWtf("making VpnManagerService ready", e6);
            }
        }
        t.traceEnd();
        t.traceBegin("MakeVcnManagementServiceReady");
        if (vcnManagementF != null) {
            try {
                vcnManagementF.systemReady();
            } catch (Throwable e7) {
                reportWtf("making VcnManagementService ready", e7);
            }
        }
        t.traceEnd();
        t.traceBegin("MakeNetworkPolicyServiceReady");
        if (networkPolicyF != null) {
            try {
                networkPolicyF.systemReady(networkPolicyInitReadySignal);
            } catch (Throwable e8) {
                reportWtf("making Network Policy Service ready", e8);
            }
        }
        t.traceEnd();
        sMtkSystemServerIns.addBootEvent("SystemServer:NetworkPolicyManagerServ systemReady");
        this.mPackageManagerService.waitForAppDataPrepared();
        t.traceBegin("PhaseThirdPartyAppsCanStart");
        if (webviewPrep != null) {
            ConcurrentUtils.waitForFutureNoInterrupt(webviewPrep, "WebViewFactoryPreparation");
        }
        this.mSystemServiceManager.startBootPhase(t, 600);
        t.traceEnd();
        if (UserManager.isHeadlessSystemUserMode() && !isAutomotive) {
            t.traceBegin("BootUserInitializer");
            new BootUserInitializer(this.mActivityManagerService, this.mContentResolver).init(t);
            t.traceEnd();
        }
        t.traceBegin("StartNetworkStack");
        try {
            NetworkStackClient.getInstance().start();
        } catch (Throwable e9) {
            reportWtf("starting Network Stack", e9);
        }
        t.traceEnd();
        t.traceBegin("StartTethering");
        try {
            ConnectivityModuleConnector.getInstance().startModuleService(TETHERING_CONNECTOR_CLASS, "android.permission.MAINLINE_NETWORK_STACK", new ConnectivityModuleConnector.ModuleServiceCallback() { // from class: com.android.server.SystemServer$$ExternalSyntheticLambda2
                @Override // android.net.ConnectivityModuleConnector.ModuleServiceCallback
                public final void onModuleServiceConnected(IBinder iBinder) {
                    ServiceManager.addService("tethering", iBinder, false, 6);
                }
            });
        } catch (Throwable e10) {
            reportWtf("starting Tethering", e10);
        }
        t.traceEnd();
        t.traceBegin("MakeCountryDetectionServiceReady");
        if (countryDetectorF != null) {
            try {
                countryDetectorF.systemRunning();
            } catch (Throwable e11) {
                reportWtf("Notifying CountryDetectorService running", e11);
            }
        }
        t.traceEnd();
        t.traceBegin("MakeNetworkTimeUpdateReady");
        if (networkTimeUpdaterF != null) {
            try {
                networkTimeUpdaterF.systemRunning();
            } catch (Throwable e12) {
                reportWtf("Notifying NetworkTimeService running", e12);
            }
        }
        t.traceEnd();
        t.traceBegin("MakeInputManagerServiceReady");
        if (inputManagerF != null) {
            try {
                inputManagerF.systemRunning();
            } catch (Throwable e13) {
                reportWtf("Notifying InputManagerService running", e13);
            }
        }
        t.traceEnd();
        t.traceBegin("MakeTelephonyRegistryReady");
        if (telephonyRegistryF != null) {
            try {
                telephonyRegistryF.systemRunning();
            } catch (Throwable e14) {
                reportWtf("Notifying TelephonyRegistry running", e14);
            }
        }
        t.traceEnd();
        t.traceBegin("MakeMediaRouterServiceReady");
        if (mediaRouterF != null) {
            try {
                mediaRouterF.systemRunning();
            } catch (Throwable e15) {
                reportWtf("Notifying MediaRouterService running", e15);
            }
        }
        t.traceEnd();
        if (this.mPackageManager.hasSystemFeature("android.hardware.telephony")) {
            t.traceBegin("MakeMmsServiceReady");
            if (mmsServiceF != null) {
                try {
                    mmsServiceF.systemRunning();
                } catch (Throwable e16) {
                    reportWtf("Notifying MmsService running", e16);
                }
            }
            t.traceEnd();
        }
        t.traceBegin("IncidentDaemonReady");
        try {
            IIncidentManager incident = IIncidentManager.Stub.asInterface(ServiceManager.getService("incident"));
            if (incident != null) {
                incident.systemRunning();
            }
        } catch (Throwable e17) {
            reportWtf("Notifying incident daemon running", e17);
        }
        t.traceEnd();
        ITranSystemServer.Instance().startOSCSAiAndMultiWindow();
        if (Build.TRAN_AIPOWERLAB_SUPPORT && tranAipowerlabServiceF != null) {
            tranAipowerlabServiceF.systemReady();
        }
        if (Build.TRAN_POWERHUB_SUPPORT && tranPowerhubServiceF != null) {
            tranPowerhubServiceF.systemReady();
        }
        sMtkSystemServerIns.addBootEvent("SystemServer:PhaseThirdPartyAppsCanStart");
        t.traceBegin("NetworkDataControllerService");
        try {
            startNetworkDataControllerService(context);
        } catch (Throwable e18) {
            reportWtf("starting NetworkDataControllerService:", e18);
        }
        t.traceEnd();
        t.traceBegin("PermissionAnnouncementService");
        try {
            startPermissionAnnouncementService(context);
        } catch (Throwable e19) {
            reportWtf("starting PermissionAnnouncementService:", e19);
        }
        t.traceEnd();
        if (this.mIncrementalServiceHandle != 0) {
            t.traceBegin("MakeIncrementalServiceReady");
            setIncrementalServiceSystemReady(this.mIncrementalServiceHandle);
            t.traceEnd();
        }
        t.traceBegin("OdsignStatsLogger");
        try {
            OdsignStatsLogger.triggerStatsWrite();
        } catch (Throwable e20) {
            reportWtf("Triggering OdsignStatsLogger", e20);
        }
        t.traceEnd();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startOtherServices$3$com-android-server-SystemServer  reason: not valid java name */
    public /* synthetic */ void m425lambda$startOtherServices$3$comandroidserverSystemServer() {
        Slog.i(TAG, "WebViewFactoryPreparation");
        TimingsTraceAndSlog traceLog = TimingsTraceAndSlog.newAsyncLog();
        traceLog.traceBegin("WebViewFactoryPreparation");
        ConcurrentUtils.waitForFutureNoInterrupt(this.mZygotePreload, "Zygote preload");
        this.mZygotePreload = null;
        this.mWebViewUpdateService.prepareWebViewInSystemServer();
        traceLog.traceEnd();
    }

    private void startApexServices(TimingsTraceAndSlog t) {
        t.traceBegin("startApexServices");
        List<ApexSystemServiceInfo> services = ApexManager.getInstance().getApexSystemServices();
        for (ApexSystemServiceInfo info : services) {
            String name = info.getName();
            String jarPath = info.getJarPath();
            t.traceBegin("starting " + name);
            if (TextUtils.isEmpty(jarPath)) {
                this.mSystemServiceManager.startService(name);
            } else {
                this.mSystemServiceManager.startServiceFromJar(name, jarPath);
            }
            t.traceEnd();
        }
        this.mSystemServiceManager.sealStartedServices();
        t.traceEnd();
    }

    private boolean deviceHasConfigString(Context context, int resId) {
        String serviceName = context.getString(resId);
        return !TextUtils.isEmpty(serviceName);
    }

    private void startSystemCaptionsManagerService(Context context, TimingsTraceAndSlog t) {
        if (!deviceHasConfigString(context, 17039945)) {
            Slog.d(TAG, "SystemCaptionsManagerService disabled because resource is not overlaid");
            return;
        }
        t.traceBegin("StartSystemCaptionsManagerService");
        this.mSystemServiceManager.startService(SYSTEM_CAPTIONS_MANAGER_SERVICE_CLASS);
        t.traceEnd();
    }

    private void startTextToSpeechManagerService(Context context, TimingsTraceAndSlog t) {
        t.traceBegin("StartTextToSpeechManagerService");
        this.mSystemServiceManager.startService(TEXT_TO_SPEECH_MANAGER_SERVICE_CLASS);
        t.traceEnd();
    }

    private void startContentCaptureService(Context context, TimingsTraceAndSlog t) {
        ActivityManagerService activityManagerService;
        boolean explicitlyEnabled = false;
        String settings = DeviceConfig.getProperty("content_capture", "service_explicitly_enabled");
        if (settings != null && !settings.equalsIgnoreCase(HealthServiceWrapperHidl.INSTANCE_VENDOR)) {
            explicitlyEnabled = Boolean.parseBoolean(settings);
            if (explicitlyEnabled) {
                Slog.d(TAG, "ContentCaptureService explicitly enabled by DeviceConfig");
            } else {
                Slog.d(TAG, "ContentCaptureService explicitly disabled by DeviceConfig");
                return;
            }
        }
        if (!explicitlyEnabled && !deviceHasConfigString(context, 17039925)) {
            Slog.d(TAG, "ContentCaptureService disabled because resource is not overlaid");
            return;
        }
        t.traceBegin("StartContentCaptureService");
        this.mSystemServiceManager.startService(CONTENT_CAPTURE_MANAGER_SERVICE_CLASS);
        ContentCaptureManagerInternal ccmi = (ContentCaptureManagerInternal) LocalServices.getService(ContentCaptureManagerInternal.class);
        if (ccmi != null && (activityManagerService = this.mActivityManagerService) != null) {
            activityManagerService.setContentCaptureManager(ccmi);
        }
        t.traceEnd();
    }

    private void startAttentionService(Context context, TimingsTraceAndSlog t) {
        if (!AttentionManagerService.isServiceConfigured(context)) {
            Slog.d(TAG, "AttentionService is not configured on this device");
            return;
        }
        t.traceBegin("StartAttentionManagerService");
        this.mSystemServiceManager.startService(AttentionManagerService.class);
        t.traceEnd();
    }

    private void startRotationResolverService(Context context, TimingsTraceAndSlog t) {
        if (!RotationResolverManagerService.isServiceConfigured(context)) {
            Slog.d(TAG, "RotationResolverService is not configured on this device");
            return;
        }
        t.traceBegin("StartRotationResolverService");
        this.mSystemServiceManager.startService(RotationResolverManagerService.class);
        t.traceEnd();
    }

    private void startAmbientContextService(TimingsTraceAndSlog t) {
        t.traceBegin("StartAmbientContextService");
        this.mSystemServiceManager.startService(AmbientContextManagerService.class);
        t.traceEnd();
    }

    private static void startSystemUi(Context context, WindowManagerService windowManager) {
        PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        Intent intent = new Intent();
        intent.setComponent(pm.getSystemUiServiceComponent());
        intent.addFlags(256);
        context.startServiceAsUser(intent, UserHandle.SYSTEM);
        windowManager.onSystemUiStarted();
    }

    private final void startNetworkDataControllerService(Context context) {
        if (SystemProperties.getInt("persist.vendor.sys.disable.moms", 0) != 1 && SystemProperties.getInt("ro.vendor.mtk_mobile_management", 0) == 1) {
            Intent serviceIntent = new Intent("com.mediatek.security.START_SERVICE");
            serviceIntent.setClassName("com.mediatek.security.service", "com.mediatek.security.service.NetworkDataControllerService");
            context.startServiceAsUser(serviceIntent, UserHandle.SYSTEM);
        }
    }

    private final void startPermissionAnnouncementService(Context context) {
        if (SystemProperties.getInt("persist.vendor.sys.disable.moms", 0) != 1 && SystemProperties.getInt("ro.vendor.mtk_mobile_management", 0) == 1) {
            Intent intent = new Intent();
            intent.setClassName("com.android.permissioncontroller", "com.mediatek.permissioncontroller.PermissionsAnnouncementService");
            context.startForegroundServiceAsUser(intent, UserHandle.SYSTEM);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean handleEarlySystemWtf(IBinder app, String tag, boolean system, ApplicationErrorReport.ParcelableCrashInfo crashInfo, int immediateCallerPid) {
        int myPid = Process.myPid();
        com.android.server.am.EventLogTags.writeAmWtf(UserHandle.getUserId(1000), myPid, "system_server", -1, tag, crashInfo.exceptionMessage);
        FrameworkStatsLog.write(80, 1000, tag, "system_server", myPid, 3);
        synchronized (SystemServer.class) {
            if (sPendingWtfs == null) {
                sPendingWtfs = new LinkedList<>();
            }
            sPendingWtfs.add(new Pair<>(tag, crashInfo));
        }
        return false;
    }
}
