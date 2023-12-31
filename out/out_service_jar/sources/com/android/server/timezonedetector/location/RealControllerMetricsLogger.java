package com.android.server.timezonedetector.location;

import com.android.internal.util.FrameworkStatsLog;
import com.android.server.timezonedetector.location.LocationTimeZoneProviderController;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class RealControllerMetricsLogger implements LocationTimeZoneProviderController.MetricsLogger {
    @Override // com.android.server.timezonedetector.location.LocationTimeZoneProviderController.MetricsLogger
    public void onStateChange(String state) {
        FrameworkStatsLog.write((int) FrameworkStatsLog.LOCATION_TIME_ZONE_PROVIDER_CONTROLLER_STATE_CHANGED, metricsState(state));
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private static int metricsState(String state) {
        char c;
        switch (state.hashCode()) {
            case -1166336595:
                if (state.equals("STOPPED")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -468307734:
                if (state.equals("PROVIDERS_INITIALIZING")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 433141802:
                if (state.equals("UNKNOWN")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 478389753:
                if (state.equals("DESTROYED")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case 872357833:
                if (state.equals("UNCERTAIN")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 1386911874:
                if (state.equals("CERTAIN")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 1917201485:
                if (state.equals("INITIALIZING")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 2066319421:
                if (state.equals("FAILED")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            case 4:
                return 5;
            case 5:
                return 6;
            case 6:
                return 7;
            default:
                return 0;
        }
    }
}
