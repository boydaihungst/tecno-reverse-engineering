package com.android.server.timezonedetector.location;

import android.content.Context;
import android.os.SystemClock;
import android.service.timezone.TimeZoneProviderEvent;
import android.util.IndentingPrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class NullLocationTimeZoneProviderProxy extends LocationTimeZoneProviderProxy {
    /* JADX INFO: Access modifiers changed from: package-private */
    public NullLocationTimeZoneProviderProxy(Context context, ThreadingDomain threadingDomain) {
        super(context, threadingDomain);
    }

    @Override // com.android.server.timezonedetector.location.LocationTimeZoneProviderProxy
    void onInitialize() {
    }

    @Override // com.android.server.timezonedetector.location.LocationTimeZoneProviderProxy
    void onDestroy() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.timezonedetector.location.LocationTimeZoneProviderProxy
    public void setRequest(TimeZoneProviderRequest request) {
        if (request.sendUpdates()) {
            TimeZoneProviderEvent event = TimeZoneProviderEvent.createPermanentFailureEvent(SystemClock.elapsedRealtime(), "Provider is disabled");
            handleTimeZoneProviderEvent(event);
        }
    }

    @Override // com.android.server.timezonedetector.Dumpable
    public void dump(IndentingPrintWriter ipw, String[] args) {
        synchronized (this.mSharedLock) {
            ipw.println("{NullLocationTimeZoneProviderProxy}");
        }
    }
}
