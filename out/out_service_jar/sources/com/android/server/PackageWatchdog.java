package com.android.server;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.VersionedPackage;
import android.net.ConnectivityModuleConnector;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.DeviceConfig;
import android.service.watchdog.ExplicitHealthCheckService;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.LongArrayQueue;
import android.util.MathUtils;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.XmlUtils;
import com.android.server.am.HostingRecord;
import com.android.server.job.controllers.JobStatus;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class PackageWatchdog {
    private static final String ATTR_DURATION = "duration";
    private static final String ATTR_EXPLICIT_HEALTH_CHECK_DURATION = "health-check-duration";
    private static final String ATTR_MITIGATION_CALLS = "mitigation-calls";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_PASSED_HEALTH_CHECK = "passed-health-check";
    private static final String ATTR_VERSION = "version";
    private static final int DB_VERSION = 1;
    static final int DEFAULT_BOOT_LOOP_TRIGGER_COUNT = 5;
    private static final boolean DEFAULT_EXPLICIT_HEALTH_CHECK_ENABLED = true;
    static final int DEFAULT_TRIGGER_FAILURE_COUNT = 5;
    public static final int FAILURE_REASON_APP_CRASH = 3;
    public static final int FAILURE_REASON_APP_NOT_RESPONDING = 4;
    public static final int FAILURE_REASON_EXPLICIT_HEALTH_CHECK = 2;
    public static final int FAILURE_REASON_NATIVE_CRASH = 1;
    public static final int FAILURE_REASON_UNKNOWN = 0;
    private static final String METADATA_FILE = "/metadata/watchdog/mitigation_count.txt";
    static final String PROPERTY_WATCHDOG_EXPLICIT_HEALTH_CHECK_ENABLED = "watchdog_explicit_health_check_enabled";
    static final String PROPERTY_WATCHDOG_TRIGGER_DURATION_MILLIS = "watchdog_trigger_failure_duration_millis";
    static final String PROPERTY_WATCHDOG_TRIGGER_FAILURE_COUNT = "watchdog_trigger_failure_count";
    private static final String PROP_BOOT_MITIGATION_COUNT = "sys.boot_mitigation_count";
    private static final String PROP_BOOT_MITIGATION_WINDOW_START = "sys.boot_mitigation_start";
    private static final String PROP_RESCUE_BOOT_COUNT = "sys.rescue_boot_count";
    private static final String PROP_RESCUE_BOOT_START = "sys.rescue_boot_start";
    private static final String TAG = "PackageWatchdog";
    private static final String TAG_OBSERVER = "observer";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_PACKAGE_WATCHDOG = "package-watchdog";
    private static PackageWatchdog sPackageWatchdog;
    private final ArrayMap<String, ObserverInternal> mAllObservers;
    private final BootThreshold mBootThreshold;
    private final ConnectivityModuleConnector mConnectivityModuleConnector;
    private final Context mContext;
    private final ExplicitHealthCheckController mHealthCheckController;
    private boolean mIsHealthCheckEnabled;
    private boolean mIsPackagesReady;
    private final Object mLock;
    private final Handler mLongTaskHandler;
    private long mNumberOfNativeCrashPollsRemaining;
    private final DeviceConfig.OnPropertiesChangedListener mOnPropertyChangedListener;
    private final AtomicFile mPolicyFile;
    private Set<String> mRequestedHealthCheckPackages;
    private final Runnable mSaveToFile;
    private final Handler mShortTaskHandler;
    private final Runnable mSyncRequests;
    private boolean mSyncRequired;
    private final Runnable mSyncStateWithScheduledReason;
    private final SystemClock mSystemClock;
    private int mTriggerFailureCount;
    private int mTriggerFailureDurationMs;
    private long mUptimeAtLastStateSync;
    private static final long NATIVE_CRASH_POLLING_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(30);
    static final int DEFAULT_TRIGGER_FAILURE_DURATION_MS = (int) TimeUnit.MINUTES.toMillis(1);
    static final long DEFAULT_OBSERVING_DURATION_MS = TimeUnit.DAYS.toMillis(2);
    static final long DEFAULT_DEESCALATION_WINDOW_MS = TimeUnit.HOURS.toMillis(1);
    private static final long NUMBER_OF_NATIVE_CRASH_POLLS = 10;
    static final long DEFAULT_BOOT_LOOP_TRIGGER_WINDOW_MS = TimeUnit.MINUTES.toMillis(NUMBER_OF_NATIVE_CRASH_POLLS);

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface FailureReasons {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface HealthCheckState {
        public static final int ACTIVE = 0;
        public static final int FAILED = 3;
        public static final int INACTIVE = 1;
        public static final int PASSED = 2;
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface PackageHealthObserverImpact {
        public static final int USER_IMPACT_HIGH = 5;
        public static final int USER_IMPACT_LOW = 1;
        public static final int USER_IMPACT_MEDIUM = 3;
        public static final int USER_IMPACT_NONE = 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @FunctionalInterface
    /* loaded from: classes.dex */
    public interface SystemClock {
        long uptimeMillis();
    }

    private PackageWatchdog(Context context) {
        this(context, new AtomicFile(new File(new File(Environment.getDataDirectory(), HostingRecord.HOSTING_TYPE_SYSTEM), "package-watchdog.xml")), new Handler(Looper.myLooper()), BackgroundThread.getHandler(), new ExplicitHealthCheckController(context), ConnectivityModuleConnector.getInstance(), new SystemClock() { // from class: com.android.server.PackageWatchdog$$ExternalSyntheticLambda3
            @Override // com.android.server.PackageWatchdog.SystemClock
            public final long uptimeMillis() {
                return SystemClock.uptimeMillis();
            }
        });
    }

    PackageWatchdog(Context context, AtomicFile policyFile, Handler shortTaskHandler, Handler longTaskHandler, ExplicitHealthCheckController controller, ConnectivityModuleConnector connectivityModuleConnector, SystemClock clock) {
        this.mLock = new Object();
        this.mAllObservers = new ArrayMap<>();
        this.mSyncRequests = new Runnable() { // from class: com.android.server.PackageWatchdog$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                PackageWatchdog.this.syncRequests();
            }
        };
        this.mSyncStateWithScheduledReason = new Runnable() { // from class: com.android.server.PackageWatchdog$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                PackageWatchdog.this.syncStateWithScheduledReason();
            }
        };
        this.mSaveToFile = new Runnable() { // from class: com.android.server.PackageWatchdog$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                PackageWatchdog.this.saveToFile();
            }
        };
        this.mOnPropertyChangedListener = new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.PackageWatchdog$$ExternalSyntheticLambda8
            public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                PackageWatchdog.this.onPropertyChanged(properties);
            }
        };
        this.mRequestedHealthCheckPackages = new ArraySet();
        this.mIsHealthCheckEnabled = true;
        this.mTriggerFailureDurationMs = DEFAULT_TRIGGER_FAILURE_DURATION_MS;
        this.mTriggerFailureCount = 5;
        this.mSyncRequired = false;
        this.mContext = context;
        this.mPolicyFile = policyFile;
        this.mShortTaskHandler = shortTaskHandler;
        this.mLongTaskHandler = longTaskHandler;
        this.mHealthCheckController = controller;
        this.mConnectivityModuleConnector = connectivityModuleConnector;
        this.mSystemClock = clock;
        this.mNumberOfNativeCrashPollsRemaining = NUMBER_OF_NATIVE_CRASH_POLLS;
        this.mBootThreshold = new BootThreshold(5, DEFAULT_BOOT_LOOP_TRIGGER_WINDOW_MS);
        loadFromFile();
        sPackageWatchdog = this;
    }

    public static PackageWatchdog getInstance(Context context) {
        PackageWatchdog packageWatchdog;
        synchronized (PackageWatchdog.class) {
            if (sPackageWatchdog == null) {
                new PackageWatchdog(context);
            }
            packageWatchdog = sPackageWatchdog;
        }
        return packageWatchdog;
    }

    public void onPackagesReady() {
        synchronized (this.mLock) {
            this.mIsPackagesReady = true;
            this.mHealthCheckController.setCallbacks(new Consumer() { // from class: com.android.server.PackageWatchdog$$ExternalSyntheticLambda11
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    PackageWatchdog.this.m285lambda$onPackagesReady$0$comandroidserverPackageWatchdog((String) obj);
                }
            }, new Consumer() { // from class: com.android.server.PackageWatchdog$$ExternalSyntheticLambda12
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    PackageWatchdog.this.m286lambda$onPackagesReady$1$comandroidserverPackageWatchdog((List) obj);
                }
            }, new Runnable() { // from class: com.android.server.PackageWatchdog$$ExternalSyntheticLambda13
                @Override // java.lang.Runnable
                public final void run() {
                    PackageWatchdog.this.onSyncRequestNotified();
                }
            });
            setPropertyChangedListenerLocked();
            updateConfigs();
            registerConnectivityModuleHealthListener();
        }
    }

    public void registerHealthObserver(PackageHealthObserver observer) {
        synchronized (this.mLock) {
            ObserverInternal internalObserver = this.mAllObservers.get(observer.getName());
            if (internalObserver != null) {
                internalObserver.registeredObserver = observer;
            } else {
                ObserverInternal internalObserver2 = new ObserverInternal(observer.getName(), new ArrayList());
                internalObserver2.registeredObserver = observer;
                this.mAllObservers.put(observer.getName(), internalObserver2);
                syncState("added new observer");
            }
        }
    }

    public void startObservingHealth(final PackageHealthObserver observer, final List<String> packageNames, long durationMs) {
        if (packageNames.isEmpty()) {
            Slog.wtf(TAG, "No packages to observe, " + observer.getName());
            return;
        }
        if (durationMs < 1) {
            Slog.wtf(TAG, "Invalid duration " + durationMs + "ms for observer " + observer.getName() + ". Not observing packages " + packageNames);
            durationMs = DEFAULT_OBSERVING_DURATION_MS;
        }
        final List<MonitoredPackage> packages = new ArrayList<>();
        for (int i = 0; i < packageNames.size(); i++) {
            MonitoredPackage pkg = newMonitoredPackage(packageNames.get(i), durationMs, false);
            if (pkg != null) {
                packages.add(pkg);
            } else {
                Slog.w(TAG, "Failed to create MonitoredPackage for pkg=" + packageNames.get(i));
            }
        }
        if (packages.isEmpty()) {
            return;
        }
        this.mLongTaskHandler.post(new Runnable() { // from class: com.android.server.PackageWatchdog$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                PackageWatchdog.this.m289lambda$startObservingHealth$2$comandroidserverPackageWatchdog(observer, packageNames, packages);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startObservingHealth$2$com-android-server-PackageWatchdog  reason: not valid java name */
    public /* synthetic */ void m289lambda$startObservingHealth$2$comandroidserverPackageWatchdog(PackageHealthObserver observer, List packageNames, List packages) {
        syncState("observing new packages");
        synchronized (this.mLock) {
            ObserverInternal oldObserver = this.mAllObservers.get(observer.getName());
            if (oldObserver == null) {
                Slog.d(TAG, observer.getName() + " started monitoring health of packages " + packageNames);
                this.mAllObservers.put(observer.getName(), new ObserverInternal(observer.getName(), packages));
            } else {
                Slog.d(TAG, observer.getName() + " added the following packages to monitor " + packageNames);
                oldObserver.updatePackagesLocked(packages);
            }
        }
        registerHealthObserver(observer);
        syncState("updated observers");
    }

    public void unregisterHealthObserver(final PackageHealthObserver observer) {
        this.mLongTaskHandler.post(new Runnable() { // from class: com.android.server.PackageWatchdog$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                PackageWatchdog.this.m290xca1790dd(observer);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$unregisterHealthObserver$3$com-android-server-PackageWatchdog  reason: not valid java name */
    public /* synthetic */ void m290xca1790dd(PackageHealthObserver observer) {
        synchronized (this.mLock) {
            this.mAllObservers.remove(observer.getName());
        }
        syncState("unregistering observer: " + observer.getName());
    }

    public void onPackageFailure(final List<VersionedPackage> packages, final int failureReason) {
        if (packages == null) {
            Slog.w(TAG, "Could not resolve a list of failing packages");
        } else {
            this.mLongTaskHandler.post(new Runnable() { // from class: com.android.server.PackageWatchdog$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    PackageWatchdog.this.m284lambda$onPackageFailure$4$comandroidserverPackageWatchdog(failureReason, packages);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:16:0x001f A[Catch: all -> 0x0091, TryCatch #0 {, blocks: (B:4:0x0009, B:6:0x0011, B:16:0x001f, B:41:0x008f, B:18:0x0025, B:20:0x002b, B:21:0x0037, B:23:0x003f, B:25:0x004b, B:27:0x0056, B:29:0x0061, B:30:0x0067, B:34:0x0076, B:38:0x007f, B:39:0x0088, B:40:0x008b), top: B:46:0x0009 }] */
    /* JADX WARN: Removed duplicated region for block: B:17:0x0024  */
    /* renamed from: lambda$onPackageFailure$4$com-android-server-PackageWatchdog  reason: not valid java name */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public /* synthetic */ void m284lambda$onPackageFailure$4$comandroidserverPackageWatchdog(int failureReason, List packages) {
        boolean requiresImmediateAction;
        synchronized (this.mLock) {
            if (this.mAllObservers.isEmpty()) {
                return;
            }
            if (failureReason != 1 && failureReason != 2) {
                requiresImmediateAction = false;
                if (!requiresImmediateAction) {
                    handleFailureImmediately(packages, failureReason);
                } else {
                    for (int pIndex = 0; pIndex < packages.size(); pIndex++) {
                        VersionedPackage versionedPackage = (VersionedPackage) packages.get(pIndex);
                        PackageHealthObserver currentObserverToNotify = null;
                        int currentObserverImpact = Integer.MAX_VALUE;
                        MonitoredPackage currentMonitoredPackage = null;
                        for (int oIndex = 0; oIndex < this.mAllObservers.size(); oIndex++) {
                            ObserverInternal observer = this.mAllObservers.valueAt(oIndex);
                            PackageHealthObserver registeredObserver = observer.registeredObserver;
                            if (registeredObserver != null && observer.onPackageFailureLocked(versionedPackage.getPackageName())) {
                                MonitoredPackage p = observer.getMonitoredPackage(versionedPackage.getPackageName());
                                int mitigationCount = 1;
                                if (p != null) {
                                    mitigationCount = p.getMitigationCountLocked() + 1;
                                }
                                int impact = registeredObserver.onHealthCheckFailed(versionedPackage, failureReason, mitigationCount);
                                if (impact != 0 && impact < currentObserverImpact) {
                                    currentObserverToNotify = registeredObserver;
                                    currentObserverImpact = impact;
                                    currentMonitoredPackage = p;
                                }
                            }
                        }
                        if (currentObserverToNotify != null) {
                            int mitigationCount2 = 1;
                            if (currentMonitoredPackage != null) {
                                currentMonitoredPackage.noteMitigationCallLocked();
                                mitigationCount2 = currentMonitoredPackage.getMitigationCountLocked();
                            }
                            currentObserverToNotify.execute(versionedPackage, failureReason, mitigationCount2);
                        }
                    }
                }
            }
            requiresImmediateAction = true;
            if (!requiresImmediateAction) {
            }
        }
    }

    private void handleFailureImmediately(List<VersionedPackage> packages, int failureReason) {
        int impact;
        VersionedPackage failingPackage = packages.size() > 0 ? packages.get(0) : null;
        PackageHealthObserver currentObserverToNotify = null;
        int currentObserverImpact = Integer.MAX_VALUE;
        for (ObserverInternal observer : this.mAllObservers.values()) {
            PackageHealthObserver registeredObserver = observer.registeredObserver;
            if (registeredObserver != null && (impact = registeredObserver.onHealthCheckFailed(failingPackage, failureReason, 1)) != 0 && impact < currentObserverImpact) {
                currentObserverToNotify = registeredObserver;
                currentObserverImpact = impact;
            }
        }
        if (currentObserverToNotify != null) {
            currentObserverToNotify.execute(failingPackage, failureReason, 1);
        }
    }

    public void noteBoot() {
        int impact;
        synchronized (this.mLock) {
            if (this.mBootThreshold.incrementAndTest()) {
                this.mBootThreshold.reset();
                int mitigationCount = this.mBootThreshold.getMitigationCount() + 1;
                PackageHealthObserver currentObserverToNotify = null;
                int currentObserverImpact = Integer.MAX_VALUE;
                for (int i = 0; i < this.mAllObservers.size(); i++) {
                    ObserverInternal observer = this.mAllObservers.valueAt(i);
                    PackageHealthObserver registeredObserver = observer.registeredObserver;
                    if (registeredObserver != null && (impact = registeredObserver.onBootLoop(mitigationCount)) != 0 && impact < currentObserverImpact) {
                        currentObserverToNotify = registeredObserver;
                        currentObserverImpact = impact;
                    }
                }
                if (currentObserverToNotify != null) {
                    this.mBootThreshold.setMitigationCount(mitigationCount);
                    this.mBootThreshold.saveMitigationCountToMetadata();
                    currentObserverToNotify.executeBootLoopMitigation(mitigationCount);
                }
            }
        }
    }

    public void writeNow() {
        synchronized (this.mLock) {
            if (!this.mAllObservers.isEmpty()) {
                this.mLongTaskHandler.removeCallbacks(this.mSaveToFile);
                pruneObserversLocked();
                saveToFile();
                Slog.i(TAG, "Last write to update package durations");
            }
        }
    }

    private void setExplicitHealthCheckEnabled(boolean enabled) {
        synchronized (this.mLock) {
            this.mIsHealthCheckEnabled = enabled;
            this.mHealthCheckController.setEnabled(enabled);
            this.mSyncRequired = true;
            syncState("health check state " + (enabled ? ServiceConfigAccessor.PROVIDER_MODE_ENABLED : ServiceConfigAccessor.PROVIDER_MODE_DISABLED));
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: checkAndMitigateNativeCrashes */
    public void m288xb1cdbdc6() {
        this.mNumberOfNativeCrashPollsRemaining--;
        if ("1".equals(SystemProperties.get("sys.init.updatable_crashing"))) {
            onPackageFailure(Collections.EMPTY_LIST, 1);
        } else if (this.mNumberOfNativeCrashPollsRemaining > 0) {
            this.mShortTaskHandler.postDelayed(new Runnable() { // from class: com.android.server.PackageWatchdog$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    PackageWatchdog.this.m282x2334c570();
                }
            }, NATIVE_CRASH_POLLING_INTERVAL_MILLIS);
        }
    }

    public void scheduleCheckAndMitigateNativeCrashes() {
        Slog.i(TAG, "Scheduling " + this.mNumberOfNativeCrashPollsRemaining + " polls to check and mitigate native crashes");
        this.mShortTaskHandler.post(new Runnable() { // from class: com.android.server.PackageWatchdog$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                PackageWatchdog.this.m288xb1cdbdc6();
            }
        });
    }

    /* loaded from: classes.dex */
    public interface PackageHealthObserver {
        boolean execute(VersionedPackage versionedPackage, int i, int i2);

        String getName();

        int onHealthCheckFailed(VersionedPackage versionedPackage, int i, int i2);

        default int onBootLoop(int mitigationCount) {
            return 0;
        }

        default boolean executeBootLoopMitigation(int mitigationCount) {
            return false;
        }

        default boolean isPersistent() {
            return false;
        }

        default boolean mayObservePackage(String packageName) {
            return false;
        }
    }

    long getTriggerFailureCount() {
        long j;
        synchronized (this.mLock) {
            j = this.mTriggerFailureCount;
        }
        return j;
    }

    long getTriggerFailureDurationMs() {
        long j;
        synchronized (this.mLock) {
            j = this.mTriggerFailureDurationMs;
        }
        return j;
    }

    private void syncRequestsAsync() {
        this.mShortTaskHandler.removeCallbacks(this.mSyncRequests);
        this.mShortTaskHandler.post(this.mSyncRequests);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void syncRequests() {
        boolean syncRequired = false;
        synchronized (this.mLock) {
            if (this.mIsPackagesReady) {
                Set<String> packages = getPackagesPendingHealthChecksLocked();
                if (this.mSyncRequired || !packages.equals(this.mRequestedHealthCheckPackages) || packages.isEmpty()) {
                    syncRequired = true;
                    this.mRequestedHealthCheckPackages = packages;
                }
            }
        }
        if (syncRequired) {
            Slog.i(TAG, "Syncing health check requests for packages: " + this.mRequestedHealthCheckPackages);
            this.mHealthCheckController.syncRequests(this.mRequestedHealthCheckPackages);
            this.mSyncRequired = false;
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: onHealthCheckPassed */
    public void m285lambda$onPackagesReady$0$comandroidserverPackageWatchdog(String packageName) {
        Slog.i(TAG, "Health check passed for package: " + packageName);
        boolean isStateChanged = false;
        synchronized (this.mLock) {
            for (int observerIdx = 0; observerIdx < this.mAllObservers.size(); observerIdx++) {
                ObserverInternal observer = this.mAllObservers.valueAt(observerIdx);
                MonitoredPackage monitoredPackage = observer.getMonitoredPackage(packageName);
                if (monitoredPackage != null) {
                    int oldState = monitoredPackage.getHealthCheckStateLocked();
                    int newState = monitoredPackage.tryPassHealthCheckLocked();
                    isStateChanged |= oldState != newState;
                }
            }
        }
        if (isStateChanged) {
            syncState("health check passed for " + packageName);
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: onSupportedPackages */
    public void m286lambda$onPackagesReady$1$comandroidserverPackageWatchdog(List<ExplicitHealthCheckService.PackageConfig> supportedPackages) {
        int newState;
        boolean isStateChanged = false;
        Map<String, Long> supportedPackageTimeouts = new ArrayMap<>();
        for (ExplicitHealthCheckService.PackageConfig info : supportedPackages) {
            supportedPackageTimeouts.put(info.getPackageName(), Long.valueOf(info.getHealthCheckTimeoutMillis()));
        }
        synchronized (this.mLock) {
            Slog.d(TAG, "Received supported packages " + supportedPackages);
            for (ObserverInternal observerInternal : this.mAllObservers.values()) {
                for (MonitoredPackage monitoredPackage : observerInternal.getMonitoredPackages().values()) {
                    String packageName = monitoredPackage.getName();
                    int oldState = monitoredPackage.getHealthCheckStateLocked();
                    if (supportedPackageTimeouts.containsKey(packageName)) {
                        newState = monitoredPackage.setHealthCheckActiveLocked(supportedPackageTimeouts.get(packageName).longValue());
                    } else {
                        newState = monitoredPackage.tryPassHealthCheckLocked();
                    }
                    isStateChanged |= oldState != newState;
                }
            }
        }
        if (isStateChanged) {
            syncState("updated health check supported packages " + supportedPackages);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSyncRequestNotified() {
        synchronized (this.mLock) {
            this.mSyncRequired = true;
            syncRequestsAsync();
        }
    }

    private Set<String> getPackagesPendingHealthChecksLocked() {
        Set<String> packages = new ArraySet<>();
        for (ObserverInternal observer : this.mAllObservers.values()) {
            for (MonitoredPackage monitoredPackage : observer.getMonitoredPackages().values()) {
                String packageName = monitoredPackage.getName();
                if (monitoredPackage.isPendingHealthChecksLocked()) {
                    packages.add(packageName);
                }
            }
        }
        return packages;
    }

    private void syncState(String reason) {
        synchronized (this.mLock) {
            Slog.i(TAG, "Syncing state, reason: " + reason);
            pruneObserversLocked();
            saveToFileAsync();
            syncRequestsAsync();
            scheduleNextSyncStateLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void syncStateWithScheduledReason() {
        syncState("scheduled");
    }

    private void scheduleNextSyncStateLocked() {
        long durationMs = getNextStateSyncMillisLocked();
        this.mShortTaskHandler.removeCallbacks(this.mSyncStateWithScheduledReason);
        if (durationMs == JobStatus.NO_LATEST_RUNTIME) {
            Slog.i(TAG, "Cancelling state sync, nothing to sync");
            this.mUptimeAtLastStateSync = 0L;
            return;
        }
        this.mUptimeAtLastStateSync = this.mSystemClock.uptimeMillis();
        this.mShortTaskHandler.postDelayed(this.mSyncStateWithScheduledReason, durationMs);
    }

    private long getNextStateSyncMillisLocked() {
        long shortestDurationMs = JobStatus.NO_LATEST_RUNTIME;
        for (int oIndex = 0; oIndex < this.mAllObservers.size(); oIndex++) {
            ArrayMap<String, MonitoredPackage> packages = this.mAllObservers.valueAt(oIndex).getMonitoredPackages();
            for (int pIndex = 0; pIndex < packages.size(); pIndex++) {
                MonitoredPackage mp = packages.valueAt(pIndex);
                long duration = mp.getShortestScheduleDurationMsLocked();
                if (duration < shortestDurationMs) {
                    shortestDurationMs = duration;
                }
            }
        }
        return shortestDurationMs;
    }

    private void pruneObserversLocked() {
        long elapsedMs = this.mUptimeAtLastStateSync == 0 ? 0L : this.mSystemClock.uptimeMillis() - this.mUptimeAtLastStateSync;
        if (elapsedMs <= 0) {
            Slog.i(TAG, "Not pruning observers, elapsed time: " + elapsedMs + "ms");
            return;
        }
        Iterator<ObserverInternal> it = this.mAllObservers.values().iterator();
        while (it.hasNext()) {
            ObserverInternal observer = it.next();
            Set<MonitoredPackage> failedPackages = observer.prunePackagesLocked(elapsedMs);
            if (!failedPackages.isEmpty()) {
                onHealthCheckFailed(observer, failedPackages);
            }
            if (observer.getMonitoredPackages().isEmpty() && (observer.registeredObserver == null || !observer.registeredObserver.isPersistent())) {
                Slog.i(TAG, "Discarding observer " + observer.name + ". All packages expired");
                it.remove();
            }
        }
    }

    private void onHealthCheckFailed(final ObserverInternal observer, final Set<MonitoredPackage> failedPackages) {
        this.mLongTaskHandler.post(new Runnable() { // from class: com.android.server.PackageWatchdog$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                PackageWatchdog.this.m283lambda$onHealthCheckFailed$7$comandroidserverPackageWatchdog(observer, failedPackages);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onHealthCheckFailed$7$com-android-server-PackageWatchdog  reason: not valid java name */
    public /* synthetic */ void m283lambda$onHealthCheckFailed$7$comandroidserverPackageWatchdog(ObserverInternal observer, Set failedPackages) {
        synchronized (this.mLock) {
            PackageHealthObserver registeredObserver = observer.registeredObserver;
            if (registeredObserver != null) {
                Iterator<MonitoredPackage> it = failedPackages.iterator();
                while (it.hasNext()) {
                    VersionedPackage versionedPkg = getVersionedPackage(it.next().getName());
                    if (versionedPkg != null) {
                        Slog.i(TAG, "Explicit health check failed for package " + versionedPkg);
                        registeredObserver.execute(versionedPkg, 2, 1);
                    }
                }
            }
        }
    }

    private PackageInfo getPackageInfo(String packageName) throws PackageManager.NameNotFoundException {
        PackageManager pm = this.mContext.getPackageManager();
        try {
            return pm.getPackageInfo(packageName, 4194304);
        } catch (PackageManager.NameNotFoundException e) {
            return pm.getPackageInfo(packageName, 1073741824);
        }
    }

    private VersionedPackage getVersionedPackage(String packageName) {
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null || TextUtils.isEmpty(packageName)) {
            return null;
        }
        try {
            long versionCode = getPackageInfo(packageName).getLongVersionCode();
            return new VersionedPackage(packageName, versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private void loadFromFile() {
        InputStream infile = null;
        this.mAllObservers.clear();
        try {
            try {
                infile = this.mPolicyFile.openRead();
                TypedXmlPullParser parser = Xml.resolvePullParser(infile);
                XmlUtils.beginDocument(parser, TAG_PACKAGE_WATCHDOG);
                int outerDepth = parser.getDepth();
                while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                    ObserverInternal observer = ObserverInternal.read(parser, this);
                    if (observer != null) {
                        this.mAllObservers.put(observer.name, observer);
                    }
                }
            } catch (FileNotFoundException e) {
            } catch (IOException | NumberFormatException | XmlPullParserException e2) {
                Slog.wtf(TAG, "Unable to read monitored packages, deleting file", e2);
                this.mPolicyFile.delete();
            }
        } finally {
            IoUtils.closeQuietly(infile);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPropertyChanged(DeviceConfig.Properties properties) {
        try {
            updateConfigs();
        } catch (Exception e) {
            Slog.w(TAG, "Failed to reload device config changes");
        }
    }

    private void setPropertyChangedListenerLocked() {
        DeviceConfig.addOnPropertiesChangedListener("rollback", this.mContext.getMainExecutor(), this.mOnPropertyChangedListener);
    }

    void removePropertyChangedListener() {
        DeviceConfig.removeOnPropertiesChangedListener(this.mOnPropertyChangedListener);
    }

    void updateConfigs() {
        synchronized (this.mLock) {
            int i = DeviceConfig.getInt("rollback", PROPERTY_WATCHDOG_TRIGGER_FAILURE_COUNT, 5);
            this.mTriggerFailureCount = i;
            if (i <= 0) {
                this.mTriggerFailureCount = 5;
            }
            int i2 = DEFAULT_TRIGGER_FAILURE_DURATION_MS;
            int i3 = DeviceConfig.getInt("rollback", PROPERTY_WATCHDOG_TRIGGER_DURATION_MILLIS, i2);
            this.mTriggerFailureDurationMs = i3;
            if (i3 <= 0) {
                this.mTriggerFailureDurationMs = i2;
            }
            setExplicitHealthCheckEnabled(DeviceConfig.getBoolean("rollback", PROPERTY_WATCHDOG_EXPLICIT_HEALTH_CHECK_ENABLED, true));
        }
    }

    private void registerConnectivityModuleHealthListener() {
        this.mConnectivityModuleConnector.registerHealthListener(new ConnectivityModuleConnector.ConnectivityModuleHealthListener() { // from class: com.android.server.PackageWatchdog$$ExternalSyntheticLambda2
            @Override // android.net.ConnectivityModuleConnector.ConnectivityModuleHealthListener
            public final void onNetworkStackFailure(String str) {
                PackageWatchdog.this.m287x655e6a90(str);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$registerConnectivityModuleHealthListener$8$com-android-server-PackageWatchdog  reason: not valid java name */
    public /* synthetic */ void m287x655e6a90(String packageName) {
        VersionedPackage pkg = getVersionedPackage(packageName);
        if (pkg == null) {
            Slog.wtf(TAG, "NetworkStack failed but could not find its package");
            return;
        }
        List<VersionedPackage> pkgList = Collections.singletonList(pkg);
        onPackageFailure(pkgList, 2);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1095=4] */
    /* JADX INFO: Access modifiers changed from: private */
    public boolean saveToFile() {
        Slog.i(TAG, "Saving observer state to file");
        synchronized (this.mLock) {
            try {
                try {
                    FileOutputStream stream = this.mPolicyFile.startWrite();
                    try {
                        TypedXmlSerializer out = Xml.resolveSerializer(stream);
                        out.startDocument((String) null, true);
                        out.startTag((String) null, TAG_PACKAGE_WATCHDOG);
                        out.attributeInt((String) null, ATTR_VERSION, 1);
                        for (int oIndex = 0; oIndex < this.mAllObservers.size(); oIndex++) {
                            this.mAllObservers.valueAt(oIndex).writeLocked(out);
                        }
                        out.endTag((String) null, TAG_PACKAGE_WATCHDOG);
                        out.endDocument();
                        this.mPolicyFile.finishWrite(stream);
                        IoUtils.closeQuietly(stream);
                    } catch (IOException e) {
                        Slog.w(TAG, "Failed to save monitored packages, restoring backup", e);
                        this.mPolicyFile.failWrite(stream);
                        IoUtils.closeQuietly(stream);
                        return false;
                    }
                } catch (IOException e2) {
                    Slog.w(TAG, "Cannot update monitored packages", e2);
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return true;
    }

    private void saveToFileAsync() {
        if (!this.mLongTaskHandler.hasCallbacks(this.mSaveToFile)) {
            this.mLongTaskHandler.post(this.mSaveToFile);
        }
    }

    public static String longArrayQueueToString(LongArrayQueue queue) {
        if (queue.size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(queue.get(0));
            for (int i = 1; i < queue.size(); i++) {
                sb.append(",");
                sb.append(queue.get(i));
            }
            return sb.toString();
        }
        return "";
    }

    public static LongArrayQueue parseLongArrayQueue(String commaSeparatedValues) {
        LongArrayQueue result = new LongArrayQueue();
        if (!TextUtils.isEmpty(commaSeparatedValues)) {
            String[] values = commaSeparatedValues.split(",");
            for (String value : values) {
                result.addLast(Long.parseLong(value));
            }
        }
        return result;
    }

    public void dump(IndentingPrintWriter pw) {
        pw.println("Package Watchdog status");
        pw.increaseIndent();
        synchronized (this.mLock) {
            for (String observerName : this.mAllObservers.keySet()) {
                pw.println("Observer name: " + observerName);
                pw.increaseIndent();
                ObserverInternal observerInternal = this.mAllObservers.get(observerName);
                observerInternal.dump(pw);
                pw.decreaseIndent();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ObserverInternal {
        private final ArrayMap<String, MonitoredPackage> mPackages = new ArrayMap<>();
        public final String name;
        public PackageHealthObserver registeredObserver;

        ObserverInternal(String name, List<MonitoredPackage> packages) {
            this.name = name;
            updatePackagesLocked(packages);
        }

        public boolean writeLocked(TypedXmlSerializer out) {
            try {
                out.startTag((String) null, PackageWatchdog.TAG_OBSERVER);
                out.attribute((String) null, "name", this.name);
                for (int i = 0; i < this.mPackages.size(); i++) {
                    MonitoredPackage p = this.mPackages.valueAt(i);
                    p.writeLocked(out);
                }
                out.endTag((String) null, PackageWatchdog.TAG_OBSERVER);
                return true;
            } catch (IOException e) {
                Slog.w(PackageWatchdog.TAG, "Cannot save observer", e);
                return false;
            }
        }

        public void updatePackagesLocked(List<MonitoredPackage> packages) {
            for (int pIndex = 0; pIndex < packages.size(); pIndex++) {
                MonitoredPackage p = packages.get(pIndex);
                MonitoredPackage existingPackage = getMonitoredPackage(p.getName());
                if (existingPackage != null) {
                    existingPackage.updateHealthCheckDuration(p.mDurationMs);
                } else {
                    putMonitoredPackage(p);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Set<MonitoredPackage> prunePackagesLocked(long elapsedMs) {
            Set<MonitoredPackage> failedPackages = new ArraySet<>();
            Iterator<MonitoredPackage> it = this.mPackages.values().iterator();
            while (it.hasNext()) {
                MonitoredPackage p = it.next();
                int oldState = p.getHealthCheckStateLocked();
                int newState = p.handleElapsedTimeLocked(elapsedMs);
                if (oldState != 3 && newState == 3) {
                    Slog.i(PackageWatchdog.TAG, "Package " + p.getName() + " failed health check");
                    failedPackages.add(p);
                }
                if (p.isExpiredLocked()) {
                    it.remove();
                }
            }
            return failedPackages;
        }

        public boolean onPackageFailureLocked(String packageName) {
            if (getMonitoredPackage(packageName) == null && this.registeredObserver.isPersistent() && this.registeredObserver.mayObservePackage(packageName)) {
                putMonitoredPackage(PackageWatchdog.sPackageWatchdog.newMonitoredPackage(packageName, PackageWatchdog.DEFAULT_OBSERVING_DURATION_MS, false));
            }
            MonitoredPackage p = getMonitoredPackage(packageName);
            if (p != null) {
                return p.onFailureLocked();
            }
            return false;
        }

        public ArrayMap<String, MonitoredPackage> getMonitoredPackages() {
            return this.mPackages;
        }

        public MonitoredPackage getMonitoredPackage(String packageName) {
            return this.mPackages.get(packageName);
        }

        public void putMonitoredPackage(MonitoredPackage p) {
            this.mPackages.put(p.getName(), p);
        }

        public static ObserverInternal read(TypedXmlPullParser parser, PackageWatchdog watchdog) {
            String observerName = null;
            if (PackageWatchdog.TAG_OBSERVER.equals(parser.getName())) {
                observerName = parser.getAttributeValue((String) null, "name");
                if (TextUtils.isEmpty(observerName)) {
                    Slog.wtf(PackageWatchdog.TAG, "Unable to read observer name");
                    return null;
                }
            }
            List<MonitoredPackage> packages = new ArrayList<>();
            int innerDepth = parser.getDepth();
            while (XmlUtils.nextElementWithin(parser, innerDepth)) {
                try {
                    if ("package".equals(parser.getName())) {
                        try {
                            MonitoredPackage pkg = watchdog.parseMonitoredPackage(parser);
                            if (pkg != null) {
                                packages.add(pkg);
                            }
                        } catch (NumberFormatException e) {
                            Slog.wtf(PackageWatchdog.TAG, "Skipping package for observer " + observerName, e);
                        }
                    }
                } catch (IOException | XmlPullParserException e2) {
                    Slog.wtf(PackageWatchdog.TAG, "Unable to read observer " + observerName, e2);
                    return null;
                }
            }
            if (packages.isEmpty()) {
                return null;
            }
            return new ObserverInternal(observerName, packages);
        }

        public void dump(IndentingPrintWriter pw) {
            PackageHealthObserver packageHealthObserver = this.registeredObserver;
            boolean isPersistent = packageHealthObserver != null && packageHealthObserver.isPersistent();
            pw.println("Persistent: " + isPersistent);
            for (String packageName : this.mPackages.keySet()) {
                MonitoredPackage p = getMonitoredPackage(packageName);
                pw.println(packageName + ": ");
                pw.increaseIndent();
                pw.println("# Failures: " + p.mFailureHistory.size());
                pw.println("Monitoring duration remaining: " + p.mDurationMs + "ms");
                pw.println("Explicit health check duration: " + p.mHealthCheckDurationMs + "ms");
                pw.println("Health check state: " + p.toString(p.mHealthCheckState));
                pw.decreaseIndent();
            }
        }
    }

    MonitoredPackage newMonitoredPackage(String name, long durationMs, boolean hasPassedHealthCheck) {
        return newMonitoredPackage(name, durationMs, JobStatus.NO_LATEST_RUNTIME, hasPassedHealthCheck, new LongArrayQueue());
    }

    MonitoredPackage newMonitoredPackage(String name, long durationMs, long healthCheckDurationMs, boolean hasPassedHealthCheck, LongArrayQueue mitigationCalls) {
        return new MonitoredPackage(name, durationMs, healthCheckDurationMs, hasPassedHealthCheck, mitigationCalls);
    }

    MonitoredPackage parseMonitoredPackage(TypedXmlPullParser parser) throws XmlPullParserException {
        String packageName = parser.getAttributeValue((String) null, "name");
        long duration = parser.getAttributeLong((String) null, ATTR_DURATION);
        long healthCheckDuration = parser.getAttributeLong((String) null, ATTR_EXPLICIT_HEALTH_CHECK_DURATION);
        boolean hasPassedHealthCheck = parser.getAttributeBoolean((String) null, ATTR_PASSED_HEALTH_CHECK);
        LongArrayQueue mitigationCalls = parseLongArrayQueue(parser.getAttributeValue((String) null, ATTR_MITIGATION_CALLS));
        return newMonitoredPackage(packageName, duration, healthCheckDuration, hasPassedHealthCheck, mitigationCalls);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class MonitoredPackage {
        private long mDurationMs;
        private boolean mHasPassedHealthCheck;
        private long mHealthCheckDurationMs;
        private final LongArrayQueue mMitigationCalls;
        private final String mPackageName;
        private final LongArrayQueue mFailureHistory = new LongArrayQueue();
        private int mHealthCheckState = 1;

        MonitoredPackage(String packageName, long durationMs, long healthCheckDurationMs, boolean hasPassedHealthCheck, LongArrayQueue mitigationCalls) {
            this.mHealthCheckDurationMs = JobStatus.NO_LATEST_RUNTIME;
            this.mPackageName = packageName;
            this.mDurationMs = durationMs;
            this.mHealthCheckDurationMs = healthCheckDurationMs;
            this.mHasPassedHealthCheck = hasPassedHealthCheck;
            this.mMitigationCalls = mitigationCalls;
            updateHealthCheckStateLocked();
        }

        public void writeLocked(TypedXmlSerializer out) throws IOException {
            out.startTag((String) null, "package");
            out.attribute((String) null, "name", getName());
            out.attributeLong((String) null, PackageWatchdog.ATTR_DURATION, this.mDurationMs);
            out.attributeLong((String) null, PackageWatchdog.ATTR_EXPLICIT_HEALTH_CHECK_DURATION, this.mHealthCheckDurationMs);
            out.attributeBoolean((String) null, PackageWatchdog.ATTR_PASSED_HEALTH_CHECK, this.mHasPassedHealthCheck);
            LongArrayQueue normalizedCalls = normalizeMitigationCalls();
            out.attribute((String) null, PackageWatchdog.ATTR_MITIGATION_CALLS, PackageWatchdog.longArrayQueueToString(normalizedCalls));
            out.endTag((String) null, "package");
        }

        public boolean onFailureLocked() {
            long now = PackageWatchdog.this.mSystemClock.uptimeMillis();
            this.mFailureHistory.addLast(now);
            while (now - this.mFailureHistory.peekFirst() > PackageWatchdog.this.mTriggerFailureDurationMs) {
                this.mFailureHistory.removeFirst();
            }
            boolean failed = this.mFailureHistory.size() >= PackageWatchdog.this.mTriggerFailureCount;
            if (failed) {
                this.mFailureHistory.clear();
            }
            return failed;
        }

        public void noteMitigationCallLocked() {
            this.mMitigationCalls.addLast(PackageWatchdog.this.mSystemClock.uptimeMillis());
        }

        public int getMitigationCountLocked() {
            try {
                long now = PackageWatchdog.this.mSystemClock.uptimeMillis();
                while (now - this.mMitigationCalls.peekFirst() > PackageWatchdog.DEFAULT_DEESCALATION_WINDOW_MS) {
                    this.mMitigationCalls.removeFirst();
                }
            } catch (NoSuchElementException e) {
            }
            return this.mMitigationCalls.size();
        }

        public LongArrayQueue normalizeMitigationCalls() {
            LongArrayQueue normalized = new LongArrayQueue();
            long now = PackageWatchdog.this.mSystemClock.uptimeMillis();
            for (int i = 0; i < this.mMitigationCalls.size(); i++) {
                normalized.addLast(this.mMitigationCalls.get(i) - now);
            }
            return normalized;
        }

        public int setHealthCheckActiveLocked(long initialHealthCheckDurationMs) {
            if (initialHealthCheckDurationMs <= 0) {
                Slog.wtf(PackageWatchdog.TAG, "Cannot set non-positive health check duration " + initialHealthCheckDurationMs + "ms for package " + getName() + ". Using total duration " + this.mDurationMs + "ms instead");
                initialHealthCheckDurationMs = this.mDurationMs;
            }
            if (this.mHealthCheckState == 1) {
                this.mHealthCheckDurationMs = initialHealthCheckDurationMs;
            }
            return updateHealthCheckStateLocked();
        }

        public int handleElapsedTimeLocked(long elapsedMs) {
            if (elapsedMs <= 0) {
                Slog.w(PackageWatchdog.TAG, "Cannot handle non-positive elapsed time for package " + getName());
                return this.mHealthCheckState;
            }
            this.mDurationMs -= elapsedMs;
            if (this.mHealthCheckState == 0) {
                this.mHealthCheckDurationMs -= elapsedMs;
            }
            return updateHealthCheckStateLocked();
        }

        public void updateHealthCheckDuration(long newDurationMs) {
            this.mDurationMs = newDurationMs;
        }

        public int tryPassHealthCheckLocked() {
            if (this.mHealthCheckState != 3) {
                this.mHasPassedHealthCheck = true;
            }
            return updateHealthCheckStateLocked();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public String getName() {
            return this.mPackageName;
        }

        public int getHealthCheckStateLocked() {
            return this.mHealthCheckState;
        }

        public long getShortestScheduleDurationMsLocked() {
            return Math.min(toPositive(this.mDurationMs), isPendingHealthChecksLocked() ? toPositive(this.mHealthCheckDurationMs) : JobStatus.NO_LATEST_RUNTIME);
        }

        public boolean isExpiredLocked() {
            return this.mDurationMs <= 0;
        }

        public boolean isPendingHealthChecksLocked() {
            int i = this.mHealthCheckState;
            return i == 0 || i == 1;
        }

        private int updateHealthCheckStateLocked() {
            int oldState = this.mHealthCheckState;
            if (this.mHasPassedHealthCheck) {
                this.mHealthCheckState = 2;
            } else {
                long j = this.mHealthCheckDurationMs;
                if (j <= 0 || this.mDurationMs <= 0) {
                    this.mHealthCheckState = 3;
                } else if (j == JobStatus.NO_LATEST_RUNTIME) {
                    this.mHealthCheckState = 1;
                } else {
                    this.mHealthCheckState = 0;
                }
            }
            if (oldState != this.mHealthCheckState) {
                Slog.i(PackageWatchdog.TAG, "Updated health check state for package " + getName() + ": " + toString(oldState) + " -> " + toString(this.mHealthCheckState));
            }
            return this.mHealthCheckState;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public String toString(int state) {
            switch (state) {
                case 0:
                    return "ACTIVE";
                case 1:
                    return "INACTIVE";
                case 2:
                    return "PASSED";
                case 3:
                    return "FAILED";
                default:
                    return "UNKNOWN";
            }
        }

        private long toPositive(long value) {
            return value > 0 ? value : JobStatus.NO_LATEST_RUNTIME;
        }

        boolean isEqualTo(MonitoredPackage pkg) {
            return getName().equals(pkg.getName()) && this.mDurationMs == pkg.mDurationMs && this.mHasPassedHealthCheck == pkg.mHasPassedHealthCheck && this.mHealthCheckDurationMs == pkg.mHealthCheckDurationMs && this.mMitigationCalls.toString().equals(pkg.mMitigationCalls.toString());
        }
    }

    /* loaded from: classes.dex */
    class BootThreshold {
        private final int mBootTriggerCount;
        private final long mTriggerWindow;

        BootThreshold(int bootTriggerCount, long triggerWindow) {
            this.mBootTriggerCount = bootTriggerCount;
            this.mTriggerWindow = triggerWindow;
        }

        public void reset() {
            setStart(0L);
            setCount(0);
        }

        private int getCount() {
            return SystemProperties.getInt(PackageWatchdog.PROP_RESCUE_BOOT_COUNT, 0);
        }

        private void setCount(int count) {
            SystemProperties.set(PackageWatchdog.PROP_RESCUE_BOOT_COUNT, Integer.toString(count));
        }

        public long getStart() {
            return SystemProperties.getLong(PackageWatchdog.PROP_RESCUE_BOOT_START, 0L);
        }

        public int getMitigationCount() {
            return SystemProperties.getInt(PackageWatchdog.PROP_BOOT_MITIGATION_COUNT, 0);
        }

        public void setStart(long start) {
            setPropertyStart(PackageWatchdog.PROP_RESCUE_BOOT_START, start);
        }

        public void setMitigationStart(long start) {
            setPropertyStart(PackageWatchdog.PROP_BOOT_MITIGATION_WINDOW_START, start);
        }

        public long getMitigationStart() {
            return SystemProperties.getLong(PackageWatchdog.PROP_BOOT_MITIGATION_WINDOW_START, 0L);
        }

        public void setMitigationCount(int count) {
            SystemProperties.set(PackageWatchdog.PROP_BOOT_MITIGATION_COUNT, Integer.toString(count));
        }

        public void setPropertyStart(String property, long start) {
            long now = PackageWatchdog.this.mSystemClock.uptimeMillis();
            long newStart = MathUtils.constrain(start, 0L, now);
            SystemProperties.set(property, Long.toString(newStart));
        }

        public void saveMitigationCountToMetadata() {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(PackageWatchdog.METADATA_FILE));
                writer.write(String.valueOf(getMitigationCount()));
                writer.close();
            } catch (Exception e) {
                Slog.e(PackageWatchdog.TAG, "Could not save metadata to file: " + e);
            }
        }

        public void readMitigationCountFromMetadataIfNecessary() {
            File bootPropsFile = new File(PackageWatchdog.METADATA_FILE);
            if (bootPropsFile.exists()) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(PackageWatchdog.METADATA_FILE));
                    String mitigationCount = reader.readLine();
                    setMitigationCount(Integer.parseInt(mitigationCount));
                    bootPropsFile.delete();
                    reader.close();
                } catch (Exception e) {
                    Slog.i(PackageWatchdog.TAG, "Could not read metadata file: " + e);
                }
            }
        }

        public boolean incrementAndTest() {
            readMitigationCountFromMetadataIfNecessary();
            long now = PackageWatchdog.this.mSystemClock.uptimeMillis();
            if (now - getStart() < 0) {
                Slog.e(PackageWatchdog.TAG, "Window was less than zero. Resetting start to current time.");
                setStart(now);
                setMitigationStart(now);
            }
            if (now - getMitigationStart() > PackageWatchdog.DEFAULT_DEESCALATION_WINDOW_MS) {
                setMitigationCount(0);
                setMitigationStart(now);
            }
            long window = now - getStart();
            if (window >= this.mTriggerWindow) {
                setCount(1);
                setStart(now);
                return false;
            }
            int count = getCount() + 1;
            setCount(count);
            EventLogTags.writeRescueNote(0, count, window);
            return count >= this.mBootTriggerCount;
        }
    }
}
