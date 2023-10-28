package com.android.server.timezonedetector.location;

import com.android.internal.util.FrameworkStatsLog;
import com.android.server.timezonedetector.location.LocationTimeZoneProvider;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class RealProviderMetricsLogger implements LocationTimeZoneProvider.ProviderMetricsLogger {
    private final int mProviderIndex;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RealProviderMetricsLogger(int providerIndex) {
        this.mProviderIndex = providerIndex;
    }

    @Override // com.android.server.timezonedetector.location.LocationTimeZoneProvider.ProviderMetricsLogger
    public void onProviderStateChanged(int stateEnum) {
        FrameworkStatsLog.write((int) FrameworkStatsLog.LOCATION_TIME_ZONE_PROVIDER_STATE_CHANGED, this.mProviderIndex, metricsProviderState(stateEnum));
    }

    private static int metricsProviderState(int stateEnum) {
        switch (stateEnum) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            default:
                return 0;
        }
    }
}
