package com.android.server.timezonedetector.location;

import android.os.SystemClock;
import com.android.server.timezonedetector.ConfigurationChangeListener;
import com.android.server.timezonedetector.ConfigurationInternal;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import com.android.server.timezonedetector.location.LocationTimeZoneProviderController;
import java.time.Duration;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class LocationTimeZoneProviderControllerEnvironmentImpl extends LocationTimeZoneProviderController.Environment {
    private final ConfigurationChangeListener mConfigurationInternalChangeListener;
    private final ServiceConfigAccessor mServiceConfigAccessor;

    /* JADX INFO: Access modifiers changed from: package-private */
    public LocationTimeZoneProviderControllerEnvironmentImpl(ThreadingDomain threadingDomain, ServiceConfigAccessor serviceConfigAccessor, final LocationTimeZoneProviderController controller) {
        super(threadingDomain);
        ServiceConfigAccessor serviceConfigAccessor2 = (ServiceConfigAccessor) Objects.requireNonNull(serviceConfigAccessor);
        this.mServiceConfigAccessor = serviceConfigAccessor2;
        ConfigurationChangeListener configurationChangeListener = new ConfigurationChangeListener() { // from class: com.android.server.timezonedetector.location.LocationTimeZoneProviderControllerEnvironmentImpl$$ExternalSyntheticLambda1
            @Override // com.android.server.timezonedetector.ConfigurationChangeListener
            public final void onChange() {
                LocationTimeZoneProviderControllerEnvironmentImpl.this.m6926xfcec2d83(controller);
            }
        };
        this.mConfigurationInternalChangeListener = configurationChangeListener;
        serviceConfigAccessor2.addConfigurationInternalChangeListener(configurationChangeListener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-timezonedetector-location-LocationTimeZoneProviderControllerEnvironmentImpl  reason: not valid java name */
    public /* synthetic */ void m6926xfcec2d83(final LocationTimeZoneProviderController controller) {
        ThreadingDomain threadingDomain = this.mThreadingDomain;
        Objects.requireNonNull(controller);
        threadingDomain.post(new Runnable() { // from class: com.android.server.timezonedetector.location.LocationTimeZoneProviderControllerEnvironmentImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                LocationTimeZoneProviderController.this.onConfigurationInternalChanged();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.timezonedetector.location.LocationTimeZoneProviderController.Environment
    public void destroy() {
        this.mServiceConfigAccessor.removeConfigurationInternalChangeListener(this.mConfigurationInternalChangeListener);
    }

    @Override // com.android.server.timezonedetector.location.LocationTimeZoneProviderController.Environment
    ConfigurationInternal getCurrentUserConfigurationInternal() {
        return this.mServiceConfigAccessor.getCurrentUserConfigurationInternal();
    }

    @Override // com.android.server.timezonedetector.location.LocationTimeZoneProviderController.Environment
    Duration getProviderInitializationTimeout() {
        return this.mServiceConfigAccessor.getLocationTimeZoneProviderInitializationTimeout();
    }

    @Override // com.android.server.timezonedetector.location.LocationTimeZoneProviderController.Environment
    Duration getProviderInitializationTimeoutFuzz() {
        return this.mServiceConfigAccessor.getLocationTimeZoneProviderInitializationTimeoutFuzz();
    }

    @Override // com.android.server.timezonedetector.location.LocationTimeZoneProviderController.Environment
    Duration getUncertaintyDelay() {
        return this.mServiceConfigAccessor.getLocationTimeZoneUncertaintyDelay();
    }

    @Override // com.android.server.timezonedetector.location.LocationTimeZoneProviderController.Environment
    Duration getProviderEventFilteringAgeThreshold() {
        return this.mServiceConfigAccessor.getLocationTimeZoneProviderEventFilteringAgeThreshold();
    }

    @Override // com.android.server.timezonedetector.location.LocationTimeZoneProviderController.Environment
    long elapsedRealtimeMillis() {
        return SystemClock.elapsedRealtime();
    }
}
