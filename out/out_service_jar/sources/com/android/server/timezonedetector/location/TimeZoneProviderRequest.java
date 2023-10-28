package com.android.server.timezonedetector.location;

import java.time.Duration;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class TimeZoneProviderRequest {
    private static final TimeZoneProviderRequest STOP_UPDATES = new TimeZoneProviderRequest(false, null, null);
    private final Duration mEventFilteringAgeThreshold;
    private final Duration mInitializationTimeout;
    private final boolean mSendUpdates;

    private TimeZoneProviderRequest(boolean sendUpdates, Duration initializationTimeout, Duration eventFilteringAgeThreshold) {
        this.mSendUpdates = sendUpdates;
        this.mInitializationTimeout = initializationTimeout;
        this.mEventFilteringAgeThreshold = eventFilteringAgeThreshold;
    }

    public static TimeZoneProviderRequest createStartUpdatesRequest(Duration initializationTimeout, Duration eventFilteringAgeThreshold) {
        return new TimeZoneProviderRequest(true, (Duration) Objects.requireNonNull(initializationTimeout), (Duration) Objects.requireNonNull(eventFilteringAgeThreshold));
    }

    public static TimeZoneProviderRequest createStopUpdatesRequest() {
        return STOP_UPDATES;
    }

    public boolean sendUpdates() {
        return this.mSendUpdates;
    }

    public Duration getInitializationTimeout() {
        return this.mInitializationTimeout;
    }

    public Duration getEventFilteringAgeThreshold() {
        return this.mEventFilteringAgeThreshold;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TimeZoneProviderRequest that = (TimeZoneProviderRequest) o;
        if (this.mSendUpdates == that.mSendUpdates && Objects.equals(this.mInitializationTimeout, that.mInitializationTimeout) && Objects.equals(this.mEventFilteringAgeThreshold, that.mEventFilteringAgeThreshold)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Boolean.valueOf(this.mSendUpdates), this.mInitializationTimeout, this.mEventFilteringAgeThreshold);
    }

    public String toString() {
        return "TimeZoneProviderRequest{mSendUpdates=" + this.mSendUpdates + ", mInitializationTimeout=" + this.mInitializationTimeout + ", mEventFilteringAgeThreshold=" + this.mEventFilteringAgeThreshold + "}";
    }
}
