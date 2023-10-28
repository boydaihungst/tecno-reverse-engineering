package com.android.server.timezonedetector;

import android.app.timezonedetector.ManualTimeZoneSuggestion;
import android.app.timezonedetector.TelephonyTimeZoneSuggestion;
/* loaded from: classes2.dex */
public interface TimeZoneDetectorStrategy extends Dumpable {
    void enableTelephonyTimeZoneFallback();

    MetricsTimeZoneDetectorState generateMetricsState();

    boolean isGeoTimeZoneDetectionSupported();

    boolean isTelephonyTimeZoneDetectionSupported();

    void suggestGeolocationTimeZone(GeolocationTimeZoneSuggestion geolocationTimeZoneSuggestion);

    boolean suggestManualTimeZone(int i, ManualTimeZoneSuggestion manualTimeZoneSuggestion);

    void suggestTelephonyTimeZone(TelephonyTimeZoneSuggestion telephonyTimeZoneSuggestion);
}
