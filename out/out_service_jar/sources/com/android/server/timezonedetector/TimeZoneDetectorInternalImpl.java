package com.android.server.timezonedetector;

import android.content.Context;
import android.os.Handler;
import java.util.Objects;
/* loaded from: classes2.dex */
public final class TimeZoneDetectorInternalImpl implements TimeZoneDetectorInternal {
    private final Context mContext;
    private final Handler mHandler;
    private final TimeZoneDetectorStrategy mTimeZoneDetectorStrategy;

    public TimeZoneDetectorInternalImpl(Context context, Handler handler, TimeZoneDetectorStrategy timeZoneDetectorStrategy) {
        this.mContext = (Context) Objects.requireNonNull(context);
        this.mHandler = (Handler) Objects.requireNonNull(handler);
        this.mTimeZoneDetectorStrategy = (TimeZoneDetectorStrategy) Objects.requireNonNull(timeZoneDetectorStrategy);
    }

    @Override // com.android.server.timezonedetector.TimeZoneDetectorInternal
    public void suggestGeolocationTimeZone(final GeolocationTimeZoneSuggestion timeZoneSuggestion) {
        Objects.requireNonNull(timeZoneSuggestion);
        this.mHandler.post(new Runnable() { // from class: com.android.server.timezonedetector.TimeZoneDetectorInternalImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                TimeZoneDetectorInternalImpl.this.m6902x4a3f80ea(timeZoneSuggestion);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$suggestGeolocationTimeZone$0$com-android-server-timezonedetector-TimeZoneDetectorInternalImpl  reason: not valid java name */
    public /* synthetic */ void m6902x4a3f80ea(GeolocationTimeZoneSuggestion timeZoneSuggestion) {
        this.mTimeZoneDetectorStrategy.suggestGeolocationTimeZone(timeZoneSuggestion);
    }

    @Override // com.android.server.timezonedetector.TimeZoneDetectorInternal
    public MetricsTimeZoneDetectorState generateMetricsState() {
        return this.mTimeZoneDetectorStrategy.generateMetricsState();
    }
}
