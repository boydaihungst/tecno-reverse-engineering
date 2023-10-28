package com.android.server.timezonedetector;

import com.android.i18n.timezone.TimeZoneFinder;
import java.util.function.Function;
/* loaded from: classes2.dex */
final class TimeZoneCanonicalizer implements Function<String, String> {
    /* JADX DEBUG: Method merged with bridge method */
    @Override // java.util.function.Function
    public String apply(String timeZoneId) {
        String canonicialZoneId = TimeZoneFinder.getInstance().getCountryZonesFinder().findCanonicalTimeZoneId(timeZoneId);
        return canonicialZoneId == null ? timeZoneId : canonicialZoneId;
    }
}
