package com.android.server.timezonedetector.location;

import android.service.timezone.TimeZoneProviderEvent;
import com.android.i18n.timezone.ZoneInfoDb;
/* loaded from: classes2.dex */
public class ZoneInfoDbTimeZoneProviderEventPreProcessor implements TimeZoneProviderEventPreProcessor {
    @Override // com.android.server.timezonedetector.location.TimeZoneProviderEventPreProcessor
    public TimeZoneProviderEvent preProcess(TimeZoneProviderEvent event) {
        if (event.getSuggestion() == null || event.getSuggestion().getTimeZoneIds().isEmpty()) {
            return event;
        }
        if (hasInvalidZones(event)) {
            return TimeZoneProviderEvent.createUncertainEvent(event.getCreationElapsedMillis());
        }
        return event;
    }

    private static boolean hasInvalidZones(TimeZoneProviderEvent event) {
        for (String timeZone : event.getSuggestion().getTimeZoneIds()) {
            if (!ZoneInfoDb.getInstance().hasTimeZone(timeZone)) {
                LocationTimeZoneManagerService.infoLog("event=" + event + " has unsupported zone(" + timeZone + ")");
                return true;
            }
        }
        return false;
    }
}
