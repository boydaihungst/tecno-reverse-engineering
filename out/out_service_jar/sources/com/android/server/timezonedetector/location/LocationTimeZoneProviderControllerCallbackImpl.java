package com.android.server.timezonedetector.location;

import com.android.server.LocalServices;
import com.android.server.timezonedetector.GeolocationTimeZoneSuggestion;
import com.android.server.timezonedetector.TimeZoneDetectorInternal;
import com.android.server.timezonedetector.location.LocationTimeZoneProviderController;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class LocationTimeZoneProviderControllerCallbackImpl extends LocationTimeZoneProviderController.Callback {
    /* JADX INFO: Access modifiers changed from: package-private */
    public LocationTimeZoneProviderControllerCallbackImpl(ThreadingDomain threadingDomain) {
        super(threadingDomain);
    }

    @Override // com.android.server.timezonedetector.location.LocationTimeZoneProviderController.Callback
    void suggest(GeolocationTimeZoneSuggestion suggestion) {
        this.mThreadingDomain.assertCurrentThread();
        TimeZoneDetectorInternal timeZoneDetector = (TimeZoneDetectorInternal) LocalServices.getService(TimeZoneDetectorInternal.class);
        timeZoneDetector.suggestGeolocationTimeZone(suggestion);
    }
}
