package com.android.server.timezonedetector.location;

import android.service.timezone.TimeZoneProviderEvent;
import android.util.IndentingPrintWriter;
import com.android.server.timezonedetector.location.LocationTimeZoneProvider;
import com.android.server.timezonedetector.location.LocationTimeZoneProviderProxy;
import java.time.Duration;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class BinderLocationTimeZoneProvider extends LocationTimeZoneProvider {
    private final LocationTimeZoneProviderProxy mProxy;

    /* JADX INFO: Access modifiers changed from: package-private */
    public BinderLocationTimeZoneProvider(LocationTimeZoneProvider.ProviderMetricsLogger providerMetricsLogger, ThreadingDomain threadingDomain, String providerName, LocationTimeZoneProviderProxy proxy, boolean recordStateChanges) {
        super(providerMetricsLogger, threadingDomain, providerName, new ZoneInfoDbTimeZoneProviderEventPreProcessor(), recordStateChanges);
        this.mProxy = (LocationTimeZoneProviderProxy) Objects.requireNonNull(proxy);
    }

    @Override // com.android.server.timezonedetector.location.LocationTimeZoneProvider
    void onInitialize() {
        this.mProxy.initialize(new LocationTimeZoneProviderProxy.Listener() { // from class: com.android.server.timezonedetector.location.BinderLocationTimeZoneProvider.1
            @Override // com.android.server.timezonedetector.location.LocationTimeZoneProviderProxy.Listener
            public void onReportTimeZoneProviderEvent(TimeZoneProviderEvent timeZoneProviderEvent) {
                BinderLocationTimeZoneProvider.this.handleTimeZoneProviderEvent(timeZoneProviderEvent);
            }

            @Override // com.android.server.timezonedetector.location.LocationTimeZoneProviderProxy.Listener
            public void onProviderBound() {
                BinderLocationTimeZoneProvider.this.handleOnProviderBound();
            }

            @Override // com.android.server.timezonedetector.location.LocationTimeZoneProviderProxy.Listener
            public void onProviderUnbound() {
                BinderLocationTimeZoneProvider.this.handleTemporaryFailure("onProviderUnbound()");
            }
        });
    }

    @Override // com.android.server.timezonedetector.location.LocationTimeZoneProvider
    void onDestroy() {
        this.mProxy.destroy();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnProviderBound() {
        this.mThreadingDomain.assertCurrentThread();
        synchronized (this.mSharedLock) {
            LocationTimeZoneProvider.ProviderState currentState = this.mCurrentState.get();
            switch (currentState.stateEnum) {
                case 1:
                case 2:
                case 3:
                    LocationTimeZoneManagerService.debugLog("handleOnProviderBound mProviderName=" + this.mProviderName + ", currentState=" + currentState + ": Provider is started.");
                    break;
                case 4:
                    LocationTimeZoneManagerService.debugLog("handleOnProviderBound mProviderName=" + this.mProviderName + ", currentState=" + currentState + ": Provider is stopped.");
                    break;
                case 5:
                case 6:
                    LocationTimeZoneManagerService.debugLog("handleOnProviderBound, mProviderName=" + this.mProviderName + ", currentState=" + currentState + ": No state change required, provider is terminated.");
                    break;
                default:
                    throw new IllegalStateException("Unknown currentState=" + currentState);
            }
        }
    }

    @Override // com.android.server.timezonedetector.location.LocationTimeZoneProvider
    void onStartUpdates(Duration initializationTimeout, Duration eventFilteringAgeThreshold) {
        TimeZoneProviderRequest request = TimeZoneProviderRequest.createStartUpdatesRequest(initializationTimeout, eventFilteringAgeThreshold);
        this.mProxy.setRequest(request);
    }

    @Override // com.android.server.timezonedetector.location.LocationTimeZoneProvider
    void onStopUpdates() {
        TimeZoneProviderRequest request = TimeZoneProviderRequest.createStopUpdatesRequest();
        this.mProxy.setRequest(request);
    }

    @Override // com.android.server.timezonedetector.Dumpable
    public void dump(IndentingPrintWriter ipw, String[] args) {
        synchronized (this.mSharedLock) {
            ipw.println("{BinderLocationTimeZoneProvider}");
            ipw.println("mProviderName=" + this.mProviderName);
            ipw.println("mCurrentState=" + this.mCurrentState);
            ipw.println("mProxy=" + this.mProxy);
            ipw.println("State history:");
            ipw.increaseIndent();
            this.mCurrentState.dump(ipw);
            ipw.decreaseIndent();
            ipw.println("Proxy details:");
            ipw.increaseIndent();
            this.mProxy.dump(ipw, args);
            ipw.decreaseIndent();
        }
    }

    public String toString() {
        String str;
        synchronized (this.mSharedLock) {
            str = "BinderLocationTimeZoneProvider{mProviderName=" + this.mProviderName + ", mCurrentState=" + this.mCurrentState + ", mProxy=" + this.mProxy + '}';
        }
        return str;
    }
}
