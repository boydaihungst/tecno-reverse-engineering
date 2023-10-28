package com.android.server.timezonedetector.location;

import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.util.Slog;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.Preconditions;
import com.android.server.FgThread;
import com.android.server.SystemService;
import com.android.server.timezonedetector.ConfigurationChangeListener;
import com.android.server.timezonedetector.Dumpable;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import com.android.server.timezonedetector.ServiceConfigAccessorImpl;
import com.android.server.timezonedetector.location.LocationTimeZoneProvider;
import com.android.server.timezonedetector.location.LocationTimeZoneProviderController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
/* loaded from: classes2.dex */
public class LocationTimeZoneManagerService extends Binder {
    private static final String ATTRIBUTION_TAG = "LocationTimeZoneService";
    private static final long BLOCKING_OP_WAIT_DURATION_MILLIS = Duration.ofSeconds(20).toMillis();
    static final String TAG = "LocationTZDetector";
    private final Context mContext;
    private final Handler mHandler;
    private LocationTimeZoneProviderController mLocationTimeZoneProviderController;
    private LocationTimeZoneProviderControllerEnvironmentImpl mLocationTimeZoneProviderControllerEnvironment;
    private final ProviderConfig mPrimaryProviderConfig = new ProviderConfig(0, "primary", "android.service.timezone.PrimaryLocationTimeZoneProviderService");
    private final ProviderConfig mSecondaryProviderConfig = new ProviderConfig(1, "secondary", "android.service.timezone.SecondaryLocationTimeZoneProviderService");
    private final ServiceConfigAccessor mServiceConfigAccessor;
    private final Object mSharedLock;
    private final ThreadingDomain mThreadingDomain;

    /* loaded from: classes2.dex */
    public static class Lifecycle extends SystemService {
        private LocationTimeZoneManagerService mService;
        private final ServiceConfigAccessor mServiceConfigAccessor;

        public Lifecycle(Context context) {
            super((Context) Objects.requireNonNull(context));
            this.mServiceConfigAccessor = ServiceConfigAccessorImpl.getInstance(context);
        }

        @Override // com.android.server.SystemService
        public void onStart() {
            Context context = getContext();
            if (this.mServiceConfigAccessor.isGeoTimeZoneDetectionFeatureSupportedInConfig()) {
                LocationTimeZoneManagerService locationTimeZoneManagerService = new LocationTimeZoneManagerService(context, this.mServiceConfigAccessor);
                this.mService = locationTimeZoneManagerService;
                publishBinderService("location_time_zone_manager", locationTimeZoneManagerService);
                return;
            }
            Slog.d(LocationTimeZoneManagerService.TAG, "Geo time zone detection feature is disabled in config");
        }

        @Override // com.android.server.SystemService
        public void onBootPhase(int phase) {
            if (this.mServiceConfigAccessor.isGeoTimeZoneDetectionFeatureSupportedInConfig()) {
                if (phase == 500) {
                    this.mService.onSystemReady();
                } else if (phase == 600) {
                    this.mService.onSystemThirdPartyAppsCanStart();
                }
            }
        }
    }

    LocationTimeZoneManagerService(Context context, ServiceConfigAccessor serviceConfigAccessor) {
        this.mContext = context.createAttributionContext(ATTRIBUTION_TAG);
        Handler handler = FgThread.getHandler();
        this.mHandler = handler;
        HandlerThreadingDomain handlerThreadingDomain = new HandlerThreadingDomain(handler);
        this.mThreadingDomain = handlerThreadingDomain;
        this.mSharedLock = handlerThreadingDomain.getLockObject();
        this.mServiceConfigAccessor = (ServiceConfigAccessor) Objects.requireNonNull(serviceConfigAccessor);
    }

    void onSystemReady() {
        this.mServiceConfigAccessor.addLocationTimeZoneManagerConfigListener(new ConfigurationChangeListener() { // from class: com.android.server.timezonedetector.location.LocationTimeZoneManagerService$$ExternalSyntheticLambda4
            @Override // com.android.server.timezonedetector.ConfigurationChangeListener
            public final void onChange() {
                LocationTimeZoneManagerService.this.handleServiceConfigurationChangedOnMainThread();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleServiceConfigurationChangedOnMainThread() {
        this.mThreadingDomain.post(new Runnable() { // from class: com.android.server.timezonedetector.location.LocationTimeZoneManagerService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                LocationTimeZoneManagerService.this.restartIfRequiredOnDomainThread();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void restartIfRequiredOnDomainThread() {
        this.mThreadingDomain.assertCurrentThread();
        synchronized (this.mSharedLock) {
            if (this.mLocationTimeZoneProviderController != null) {
                stopOnDomainThread();
                startOnDomainThread();
            }
        }
    }

    void onSystemThirdPartyAppsCanStart() {
        startInternal(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void start() {
        enforceManageTimeZoneDetectorPermission();
        startInternal(true);
    }

    private void startInternal(boolean waitForCompletion) {
        Runnable runnable = new Runnable() { // from class: com.android.server.timezonedetector.location.LocationTimeZoneManagerService$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                LocationTimeZoneManagerService.this.startOnDomainThread();
            }
        };
        if (waitForCompletion) {
            this.mThreadingDomain.postAndWait(runnable, BLOCKING_OP_WAIT_DURATION_MILLIS);
        } else {
            this.mThreadingDomain.post(runnable);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startWithTestProviders(final String testPrimaryProviderPackageName, final String testSecondaryProviderPackageName, final boolean recordStateChanges) {
        enforceManageTimeZoneDetectorPermission();
        if (testPrimaryProviderPackageName == null && testSecondaryProviderPackageName == null) {
            throw new IllegalArgumentException("One or both test package names must be provided.");
        }
        this.mThreadingDomain.postAndWait(new Runnable() { // from class: com.android.server.timezonedetector.location.LocationTimeZoneManagerService$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                LocationTimeZoneManagerService.this.m6918xb600be0d(testPrimaryProviderPackageName, testSecondaryProviderPackageName, recordStateChanges);
            }
        }, BLOCKING_OP_WAIT_DURATION_MILLIS);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startWithTestProviders$0$com-android-server-timezonedetector-location-LocationTimeZoneManagerService  reason: not valid java name */
    public /* synthetic */ void m6918xb600be0d(String testPrimaryProviderPackageName, String testSecondaryProviderPackageName, boolean recordStateChanges) {
        synchronized (this.mSharedLock) {
            stopOnDomainThread();
            this.mServiceConfigAccessor.setTestPrimaryLocationTimeZoneProviderPackageName(testPrimaryProviderPackageName);
            this.mServiceConfigAccessor.setTestSecondaryLocationTimeZoneProviderPackageName(testSecondaryProviderPackageName);
            this.mServiceConfigAccessor.setRecordStateChangesForTests(recordStateChanges);
            startOnDomainThread();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startOnDomainThread() {
        this.mThreadingDomain.assertCurrentThread();
        synchronized (this.mSharedLock) {
            if (!this.mServiceConfigAccessor.isGeoTimeZoneDetectionFeatureSupported()) {
                debugLog("Not starting location_time_zone_manager: it is disabled in service config");
                return;
            }
            if (this.mLocationTimeZoneProviderController == null) {
                LocationTimeZoneProvider primary = this.mPrimaryProviderConfig.createProvider();
                LocationTimeZoneProvider secondary = this.mSecondaryProviderConfig.createProvider();
                LocationTimeZoneProviderController.MetricsLogger metricsLogger = new RealControllerMetricsLogger();
                boolean recordStateChanges = this.mServiceConfigAccessor.getRecordStateChangesForTests();
                LocationTimeZoneProviderController controller = new LocationTimeZoneProviderController(this.mThreadingDomain, metricsLogger, primary, secondary, recordStateChanges);
                LocationTimeZoneProviderControllerEnvironmentImpl environment = new LocationTimeZoneProviderControllerEnvironmentImpl(this.mThreadingDomain, this.mServiceConfigAccessor, controller);
                LocationTimeZoneProviderControllerCallbackImpl callback = new LocationTimeZoneProviderControllerCallbackImpl(this.mThreadingDomain);
                controller.initialize(environment, callback);
                this.mLocationTimeZoneProviderControllerEnvironment = environment;
                this.mLocationTimeZoneProviderController = controller;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stop() {
        enforceManageTimeZoneDetectorPermission();
        this.mThreadingDomain.postAndWait(new Runnable() { // from class: com.android.server.timezonedetector.location.LocationTimeZoneManagerService$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                LocationTimeZoneManagerService.this.stopOnDomainThread();
            }
        }, BLOCKING_OP_WAIT_DURATION_MILLIS);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopOnDomainThread() {
        this.mThreadingDomain.assertCurrentThread();
        synchronized (this.mSharedLock) {
            LocationTimeZoneProviderController locationTimeZoneProviderController = this.mLocationTimeZoneProviderController;
            if (locationTimeZoneProviderController != null) {
                locationTimeZoneProviderController.destroy();
                this.mLocationTimeZoneProviderController = null;
                this.mLocationTimeZoneProviderControllerEnvironment.destroy();
                this.mLocationTimeZoneProviderControllerEnvironment = null;
                this.mServiceConfigAccessor.resetVolatileTestConfig();
            }
        }
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new LocationTimeZoneManagerShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearRecordedProviderStates() {
        enforceManageTimeZoneDetectorPermission();
        this.mThreadingDomain.postAndWait(new Runnable() { // from class: com.android.server.timezonedetector.location.LocationTimeZoneManagerService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                LocationTimeZoneManagerService.this.m6916xbf4e653c();
            }
        }, BLOCKING_OP_WAIT_DURATION_MILLIS);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$clearRecordedProviderStates$1$com-android-server-timezonedetector-location-LocationTimeZoneManagerService  reason: not valid java name */
    public /* synthetic */ void m6916xbf4e653c() {
        synchronized (this.mSharedLock) {
            LocationTimeZoneProviderController locationTimeZoneProviderController = this.mLocationTimeZoneProviderController;
            if (locationTimeZoneProviderController != null) {
                locationTimeZoneProviderController.clearRecordedStates();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public LocationTimeZoneManagerServiceState getStateForTests() {
        enforceManageTimeZoneDetectorPermission();
        try {
            return (LocationTimeZoneManagerServiceState) this.mThreadingDomain.postAndWait(new Callable() { // from class: com.android.server.timezonedetector.location.LocationTimeZoneManagerService$$ExternalSyntheticLambda3
                @Override // java.util.concurrent.Callable
                public final Object call() {
                    return LocationTimeZoneManagerService.this.m6917xb81a9b9a();
                }
            }, BLOCKING_OP_WAIT_DURATION_MILLIS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getStateForTests$2$com-android-server-timezonedetector-location-LocationTimeZoneManagerService  reason: not valid java name */
    public /* synthetic */ LocationTimeZoneManagerServiceState m6917xb81a9b9a() throws Exception {
        synchronized (this.mSharedLock) {
            LocationTimeZoneProviderController locationTimeZoneProviderController = this.mLocationTimeZoneProviderController;
            if (locationTimeZoneProviderController == null) {
                return null;
            }
            return locationTimeZoneProviderController.getStateForTests();
        }
    }

    @Override // android.os.Binder
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            IndentingPrintWriter ipw = new IndentingPrintWriter(pw);
            synchronized (this.mSharedLock) {
                ipw.println("LocationTimeZoneManagerService:");
                ipw.increaseIndent();
                ipw.println("Primary provider config:");
                ipw.increaseIndent();
                this.mPrimaryProviderConfig.dump(ipw, args);
                ipw.decreaseIndent();
                ipw.println("Secondary provider config:");
                ipw.increaseIndent();
                this.mSecondaryProviderConfig.dump(ipw, args);
                ipw.decreaseIndent();
                LocationTimeZoneProviderController locationTimeZoneProviderController = this.mLocationTimeZoneProviderController;
                if (locationTimeZoneProviderController == null) {
                    ipw.println("{Stopped}");
                } else {
                    locationTimeZoneProviderController.dump(ipw, args);
                }
                ipw.decreaseIndent();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void debugLog(String msg) {
        if (Log.isLoggable(TAG, 3)) {
            Slog.d(TAG, msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void infoLog(String msg) {
        if (Log.isLoggable(TAG, 4)) {
            Slog.i(TAG, msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void warnLog(String msg) {
        warnLog(msg, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void warnLog(String msg, Throwable t) {
        if (Log.isLoggable(TAG, 5)) {
            Slog.w(TAG, msg, t);
        }
    }

    private void enforceManageTimeZoneDetectorPermission() {
        this.mContext.enforceCallingPermission("android.permission.MANAGE_TIME_AND_ZONE_DETECTION", "manage time and time zone detection");
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class ProviderConfig implements Dumpable {
        private final int mIndex;
        private final String mName;
        private final String mServiceAction;

        ProviderConfig(int index, String name, String serviceAction) {
            boolean z = true;
            Preconditions.checkArgument((index < 0 || index > 1) ? false : false);
            this.mIndex = index;
            this.mName = (String) Objects.requireNonNull(name);
            this.mServiceAction = (String) Objects.requireNonNull(serviceAction);
        }

        LocationTimeZoneProvider createProvider() {
            LocationTimeZoneProviderProxy proxy = createProxy();
            LocationTimeZoneProvider.ProviderMetricsLogger providerMetricsLogger = new RealProviderMetricsLogger(this.mIndex);
            return new BinderLocationTimeZoneProvider(providerMetricsLogger, LocationTimeZoneManagerService.this.mThreadingDomain, this.mName, proxy, LocationTimeZoneManagerService.this.mServiceConfigAccessor.getRecordStateChangesForTests());
        }

        @Override // com.android.server.timezonedetector.Dumpable
        public void dump(IndentingPrintWriter ipw, String[] args) {
            ipw.printf("getMode()=%s\n", new Object[]{getMode()});
            ipw.printf("getPackageName()=%s\n", new Object[]{getPackageName()});
        }

        private LocationTimeZoneProviderProxy createProxy() {
            String mode = getMode();
            if (Objects.equals(mode, ServiceConfigAccessor.PROVIDER_MODE_DISABLED)) {
                return new NullLocationTimeZoneProviderProxy(LocationTimeZoneManagerService.this.mContext, LocationTimeZoneManagerService.this.mThreadingDomain);
            }
            return createRealProxy();
        }

        private String getMode() {
            if (this.mIndex == 0) {
                return LocationTimeZoneManagerService.this.mServiceConfigAccessor.getPrimaryLocationTimeZoneProviderMode();
            }
            return LocationTimeZoneManagerService.this.mServiceConfigAccessor.getSecondaryLocationTimeZoneProviderMode();
        }

        private RealLocationTimeZoneProviderProxy createRealProxy() {
            String providerServiceAction = this.mServiceAction;
            boolean isTestProvider = isTestProvider();
            String providerPackageName = getPackageName();
            return new RealLocationTimeZoneProviderProxy(LocationTimeZoneManagerService.this.mContext, LocationTimeZoneManagerService.this.mHandler, LocationTimeZoneManagerService.this.mThreadingDomain, providerServiceAction, providerPackageName, isTestProvider);
        }

        private boolean isTestProvider() {
            if (this.mIndex == 0) {
                return LocationTimeZoneManagerService.this.mServiceConfigAccessor.isTestPrimaryLocationTimeZoneProvider();
            }
            return LocationTimeZoneManagerService.this.mServiceConfigAccessor.isTestSecondaryLocationTimeZoneProvider();
        }

        private String getPackageName() {
            if (this.mIndex == 0) {
                return LocationTimeZoneManagerService.this.mServiceConfigAccessor.getPrimaryLocationTimeZoneProviderPackageName();
            }
            return LocationTimeZoneManagerService.this.mServiceConfigAccessor.getSecondaryLocationTimeZoneProviderPackageName();
        }
    }
}
