package com.android.server.timezonedetector;

import android.app.time.TimeZoneCapabilities;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.app.timezonedetector.ManualTimeZoneSuggestion;
import android.app.timezonedetector.TelephonyTimeZoneSuggestion;
import android.content.Context;
import android.os.Handler;
import android.os.TimestampedValue;
import android.util.IndentingPrintWriter;
import android.util.LocalLog;
import android.util.Slog;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
/* loaded from: classes2.dex */
public final class TimeZoneDetectorStrategyImpl implements TimeZoneDetectorStrategy {
    private static final boolean DBG = false;
    private static final int KEEP_SUGGESTION_HISTORY_SIZE = 10;
    private static final String LOG_TAG = "time_zone_detector";
    public static final int TELEPHONY_SCORE_HIGH = 3;
    public static final int TELEPHONY_SCORE_HIGHEST = 4;
    public static final int TELEPHONY_SCORE_LOW = 1;
    public static final int TELEPHONY_SCORE_MEDIUM = 2;
    public static final int TELEPHONY_SCORE_NONE = 0;
    public static final int TELEPHONY_SCORE_USAGE_THRESHOLD = 2;
    private ConfigurationInternal mCurrentConfigurationInternal;
    private final Environment mEnvironment;
    private TimestampedValue<Boolean> mTelephonyTimeZoneFallbackEnabled;
    private final LocalLog mTimeZoneChangesLog = new LocalLog(30, false);
    private final ArrayMapWithHistory<Integer, QualifiedTelephonyTimeZoneSuggestion> mTelephonySuggestionsBySlotIndex = new ArrayMapWithHistory<>(10);
    private final ReferenceWithHistory<GeolocationTimeZoneSuggestion> mLatestGeoLocationSuggestion = new ReferenceWithHistory<>(10);
    private final ReferenceWithHistory<ManualTimeZoneSuggestion> mLatestManualSuggestion = new ReferenceWithHistory<>(10);

    /* loaded from: classes2.dex */
    public interface Environment {
        long elapsedRealtimeMillis();

        ConfigurationInternal getCurrentUserConfigurationInternal();

        String getDeviceTimeZone();

        boolean isDeviceTimeZoneInitialized();

        void setConfigurationInternalChangeListener(ConfigurationChangeListener configurationChangeListener);

        void setDeviceTimeZone(String str);
    }

    public static TimeZoneDetectorStrategyImpl create(Context context, Handler handler, ServiceConfigAccessor serviceConfigAccessor) {
        Environment environment = new EnvironmentImpl(context, handler, serviceConfigAccessor);
        return new TimeZoneDetectorStrategyImpl(environment);
    }

    public TimeZoneDetectorStrategyImpl(Environment environment) {
        Environment environment2 = (Environment) Objects.requireNonNull(environment);
        this.mEnvironment = environment2;
        this.mTelephonyTimeZoneFallbackEnabled = new TimestampedValue<>(environment2.elapsedRealtimeMillis(), true);
        synchronized (this) {
            environment2.setConfigurationInternalChangeListener(new ConfigurationChangeListener() { // from class: com.android.server.timezonedetector.TimeZoneDetectorStrategyImpl$$ExternalSyntheticLambda0
                @Override // com.android.server.timezonedetector.ConfigurationChangeListener
                public final void onChange() {
                    TimeZoneDetectorStrategyImpl.this.handleConfigurationInternalChanged();
                }
            });
            this.mCurrentConfigurationInternal = environment2.getCurrentUserConfigurationInternal();
        }
    }

    @Override // com.android.server.timezonedetector.TimeZoneDetectorStrategy
    public synchronized void suggestGeolocationTimeZone(GeolocationTimeZoneSuggestion suggestion) {
        ConfigurationInternal currentUserConfig = this.mCurrentConfigurationInternal;
        Objects.requireNonNull(suggestion);
        this.mLatestGeoLocationSuggestion.set(suggestion);
        disableTelephonyFallbackIfNeeded();
        String reason = "New geolocation time zone suggested. suggestion=" + suggestion;
        doAutoTimeZoneDetection(currentUserConfig, reason);
    }

    @Override // com.android.server.timezonedetector.TimeZoneDetectorStrategy
    public synchronized boolean suggestManualTimeZone(int userId, ManualTimeZoneSuggestion suggestion) {
        ConfigurationInternal currentUserConfig = this.mCurrentConfigurationInternal;
        if (currentUserConfig.getUserId() != userId) {
            Slog.w(LOG_TAG, "Manual suggestion received but user != current user, userId=" + userId + " suggestion=" + suggestion);
            return false;
        }
        Objects.requireNonNull(suggestion);
        String timeZoneId = suggestion.getZoneId();
        String cause = "Manual time suggestion received: suggestion=" + suggestion;
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = currentUserConfig.createCapabilitiesAndConfig();
        TimeZoneCapabilities capabilities = capabilitiesAndConfig.getCapabilities();
        if (capabilities.getSuggestManualTimeZoneCapability() != 40) {
            Slog.i(LOG_TAG, "User does not have the capability needed to set the time zone manually, capabilities=" + capabilities + ", timeZoneId=" + timeZoneId + ", cause=" + cause);
            return false;
        }
        this.mLatestManualSuggestion.set(suggestion);
        setDeviceTimeZoneIfRequired(timeZoneId, cause);
        return true;
    }

    @Override // com.android.server.timezonedetector.TimeZoneDetectorStrategy
    public synchronized void suggestTelephonyTimeZone(TelephonyTimeZoneSuggestion suggestion) {
        ConfigurationInternal currentUserConfig = this.mCurrentConfigurationInternal;
        Objects.requireNonNull(suggestion);
        int score = scoreTelephonySuggestion(suggestion);
        QualifiedTelephonyTimeZoneSuggestion scoredSuggestion = new QualifiedTelephonyTimeZoneSuggestion(suggestion, score);
        this.mTelephonySuggestionsBySlotIndex.put(Integer.valueOf(suggestion.getSlotIndex()), scoredSuggestion);
        String reason = "New telephony time zone suggested. suggestion=" + suggestion;
        doAutoTimeZoneDetection(currentUserConfig, reason);
    }

    @Override // com.android.server.timezonedetector.TimeZoneDetectorStrategy
    public synchronized void enableTelephonyTimeZoneFallback() {
        if (!((Boolean) this.mTelephonyTimeZoneFallbackEnabled.getValue()).booleanValue()) {
            ConfigurationInternal currentUserConfig = this.mCurrentConfigurationInternal;
            this.mTelephonyTimeZoneFallbackEnabled = new TimestampedValue<>(this.mEnvironment.elapsedRealtimeMillis(), true);
            String logMsg = "enableTelephonyTimeZoneFallbackMode: currentUserConfig=" + currentUserConfig + ", mTelephonyTimeZoneFallbackEnabled=" + this.mTelephonyTimeZoneFallbackEnabled;
            logTimeZoneDetectorChange(logMsg);
            disableTelephonyFallbackIfNeeded();
            if (currentUserConfig.isTelephonyFallbackSupported()) {
                doAutoTimeZoneDetection(currentUserConfig, "enableTelephonyTimeZoneFallbackMode");
            }
        }
    }

    @Override // com.android.server.timezonedetector.TimeZoneDetectorStrategy
    public synchronized MetricsTimeZoneDetectorState generateMetricsState() {
        TelephonyTimeZoneSuggestion telephonySuggestion;
        OrdinalGenerator<String> tzIdOrdinalGenerator;
        QualifiedTelephonyTimeZoneSuggestion bestQualifiedTelephonySuggestion = findBestTelephonySuggestion();
        telephonySuggestion = bestQualifiedTelephonySuggestion == null ? null : bestQualifiedTelephonySuggestion.suggestion;
        tzIdOrdinalGenerator = new OrdinalGenerator<>(new TimeZoneCanonicalizer());
        return MetricsTimeZoneDetectorState.create(tzIdOrdinalGenerator, this.mCurrentConfigurationInternal, this.mEnvironment.getDeviceTimeZone(), getLatestManualSuggestion(), telephonySuggestion, getLatestGeolocationSuggestion());
    }

    @Override // com.android.server.timezonedetector.TimeZoneDetectorStrategy
    public boolean isTelephonyTimeZoneDetectionSupported() {
        boolean isTelephonyDetectionSupported;
        synchronized (this) {
            isTelephonyDetectionSupported = this.mCurrentConfigurationInternal.isTelephonyDetectionSupported();
        }
        return isTelephonyDetectionSupported;
    }

    @Override // com.android.server.timezonedetector.TimeZoneDetectorStrategy
    public boolean isGeoTimeZoneDetectionSupported() {
        boolean isGeoDetectionSupported;
        synchronized (this) {
            isGeoDetectionSupported = this.mCurrentConfigurationInternal.isGeoDetectionSupported();
        }
        return isGeoDetectionSupported;
    }

    private static int scoreTelephonySuggestion(TelephonyTimeZoneSuggestion suggestion) {
        if (suggestion.getZoneId() == null) {
            return 0;
        }
        int score = suggestion.getMatchType();
        if (score == 5 || suggestion.getMatchType() == 4) {
            return 4;
        }
        if (suggestion.getQuality() == 1) {
            return 3;
        }
        int score2 = suggestion.getQuality();
        if (score2 == 2) {
            return 2;
        }
        int score3 = suggestion.getQuality();
        if (score3 == 3) {
            return 1;
        }
        throw new AssertionError();
    }

    private void doAutoTimeZoneDetection(ConfigurationInternal currentUserConfig, String detectionReason) {
        switch (currentUserConfig.getDetectionMode()) {
            case 1:
                return;
            case 2:
                boolean isGeoDetectionCertain = doGeolocationTimeZoneDetection(detectionReason);
                if (!isGeoDetectionCertain && ((Boolean) this.mTelephonyTimeZoneFallbackEnabled.getValue()).booleanValue() && currentUserConfig.isTelephonyFallbackSupported()) {
                    doTelephonyTimeZoneDetection(detectionReason + ", telephony fallback mode");
                    return;
                }
                return;
            case 3:
                doTelephonyTimeZoneDetection(detectionReason);
                return;
            default:
                Slog.wtf(LOG_TAG, "Unknown detection mode: " + currentUserConfig.getDetectionMode());
                return;
        }
    }

    private boolean doGeolocationTimeZoneDetection(String detectionReason) {
        List<String> zoneIds;
        String zoneId;
        GeolocationTimeZoneSuggestion latestGeolocationSuggestion = this.mLatestGeoLocationSuggestion.get();
        if (latestGeolocationSuggestion == null || (zoneIds = latestGeolocationSuggestion.getZoneIds()) == null) {
            return false;
        }
        if (zoneIds.isEmpty()) {
            return true;
        }
        String deviceTimeZone = this.mEnvironment.getDeviceTimeZone();
        if (zoneIds.contains(deviceTimeZone)) {
            zoneId = deviceTimeZone;
        } else {
            String zoneId2 = zoneIds.get(0);
            zoneId = zoneId2;
        }
        setDeviceTimeZoneIfRequired(zoneId, detectionReason);
        return true;
    }

    private void disableTelephonyFallbackIfNeeded() {
        GeolocationTimeZoneSuggestion suggestion = this.mLatestGeoLocationSuggestion.get();
        boolean isLatestSuggestionCertain = (suggestion == null || suggestion.getZoneIds() == null) ? false : true;
        if (isLatestSuggestionCertain && ((Boolean) this.mTelephonyTimeZoneFallbackEnabled.getValue()).booleanValue()) {
            boolean latestSuggestionIsNewerThanFallbackEnabled = suggestion.getEffectiveFromElapsedMillis() > this.mTelephonyTimeZoneFallbackEnabled.getReferenceTimeMillis();
            if (latestSuggestionIsNewerThanFallbackEnabled) {
                this.mTelephonyTimeZoneFallbackEnabled = new TimestampedValue<>(this.mEnvironment.elapsedRealtimeMillis(), false);
                String logMsg = "disableTelephonyFallbackIfNeeded: mTelephonyTimeZoneFallbackEnabled=" + this.mTelephonyTimeZoneFallbackEnabled;
                logTimeZoneDetectorChange(logMsg);
            }
        }
    }

    private void logTimeZoneDetectorChange(String logMsg) {
        this.mTimeZoneChangesLog.log(logMsg);
    }

    private void doTelephonyTimeZoneDetection(String detectionReason) {
        QualifiedTelephonyTimeZoneSuggestion bestTelephonySuggestion = findBestTelephonySuggestion();
        if (bestTelephonySuggestion == null) {
            return;
        }
        boolean suggestionGoodEnough = bestTelephonySuggestion.score >= 2;
        if (!suggestionGoodEnough) {
            return;
        }
        String zoneId = bestTelephonySuggestion.suggestion.getZoneId();
        if (zoneId == null) {
            Slog.w(LOG_TAG, "Empty zone suggestion scored higher than expected. This is an error: bestTelephonySuggestion=" + bestTelephonySuggestion + " detectionReason=" + detectionReason);
            return;
        }
        String cause = "Found good suggestion., bestTelephonySuggestion=" + bestTelephonySuggestion + ", detectionReason=" + detectionReason;
        setDeviceTimeZoneIfRequired(zoneId, cause);
    }

    private void setDeviceTimeZoneIfRequired(String newZoneId, String cause) {
        String currentZoneId = this.mEnvironment.getDeviceTimeZone();
        if (newZoneId.equals(currentZoneId)) {
            return;
        }
        this.mEnvironment.setDeviceTimeZone(newZoneId);
        String logMsg = "Set device time zone., currentZoneId=" + currentZoneId + ", newZoneId=" + newZoneId + ", cause=" + cause;
        logTimeZoneDetectorChange(logMsg);
    }

    private QualifiedTelephonyTimeZoneSuggestion findBestTelephonySuggestion() {
        QualifiedTelephonyTimeZoneSuggestion bestSuggestion = null;
        for (int i = 0; i < this.mTelephonySuggestionsBySlotIndex.size(); i++) {
            QualifiedTelephonyTimeZoneSuggestion candidateSuggestion = this.mTelephonySuggestionsBySlotIndex.valueAt(i);
            if (candidateSuggestion != null) {
                if (bestSuggestion == null) {
                    bestSuggestion = candidateSuggestion;
                } else if (candidateSuggestion.score > bestSuggestion.score) {
                    bestSuggestion = candidateSuggestion;
                } else if (candidateSuggestion.score == bestSuggestion.score) {
                    int candidateSlotIndex = candidateSuggestion.suggestion.getSlotIndex();
                    int bestSlotIndex = bestSuggestion.suggestion.getSlotIndex();
                    if (candidateSlotIndex < bestSlotIndex) {
                        bestSuggestion = candidateSuggestion;
                    }
                }
            }
        }
        return bestSuggestion;
    }

    public synchronized QualifiedTelephonyTimeZoneSuggestion findBestTelephonySuggestionForTests() {
        return findBestTelephonySuggestion();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void handleConfigurationInternalChanged() {
        ConfigurationInternal currentUserConfig = this.mEnvironment.getCurrentUserConfigurationInternal();
        String logMsg = "handleConfigurationInternalChanged: oldConfiguration=" + this.mCurrentConfigurationInternal + ", newConfiguration=" + currentUserConfig;
        logTimeZoneDetectorChange(logMsg);
        this.mCurrentConfigurationInternal = currentUserConfig;
        doAutoTimeZoneDetection(currentUserConfig, logMsg);
    }

    @Override // com.android.server.timezonedetector.Dumpable
    public synchronized void dump(IndentingPrintWriter ipw, String[] args) {
        ipw.println("TimeZoneDetectorStrategy:");
        ipw.increaseIndent();
        ipw.println("mCurrentConfigurationInternal=" + this.mCurrentConfigurationInternal);
        ipw.println("[Capabilities=" + this.mCurrentConfigurationInternal.createCapabilitiesAndConfig() + "]");
        ipw.println("mEnvironment.isDeviceTimeZoneInitialized()=" + this.mEnvironment.isDeviceTimeZoneInitialized());
        ipw.println("mEnvironment.getDeviceTimeZone()=" + this.mEnvironment.getDeviceTimeZone());
        ipw.println("Misc state:");
        ipw.increaseIndent();
        ipw.println("mTelephonyTimeZoneFallbackEnabled=" + formatDebugString(this.mTelephonyTimeZoneFallbackEnabled));
        ipw.decreaseIndent();
        ipw.println("Time zone change log:");
        ipw.increaseIndent();
        this.mTimeZoneChangesLog.dump(ipw);
        ipw.decreaseIndent();
        ipw.println("Manual suggestion history:");
        ipw.increaseIndent();
        this.mLatestManualSuggestion.dump(ipw);
        ipw.decreaseIndent();
        ipw.println("Geolocation suggestion history:");
        ipw.increaseIndent();
        this.mLatestGeoLocationSuggestion.dump(ipw);
        ipw.decreaseIndent();
        ipw.println("Telephony suggestion history:");
        ipw.increaseIndent();
        this.mTelephonySuggestionsBySlotIndex.dump(ipw);
        ipw.decreaseIndent();
        ipw.decreaseIndent();
    }

    public synchronized ManualTimeZoneSuggestion getLatestManualSuggestion() {
        return this.mLatestManualSuggestion.get();
    }

    public synchronized QualifiedTelephonyTimeZoneSuggestion getLatestTelephonySuggestion(int slotIndex) {
        return this.mTelephonySuggestionsBySlotIndex.get(Integer.valueOf(slotIndex));
    }

    public synchronized GeolocationTimeZoneSuggestion getLatestGeolocationSuggestion() {
        return this.mLatestGeoLocationSuggestion.get();
    }

    public synchronized boolean isTelephonyFallbackEnabledForTests() {
        return ((Boolean) this.mTelephonyTimeZoneFallbackEnabled.getValue()).booleanValue();
    }

    /* loaded from: classes2.dex */
    public static final class QualifiedTelephonyTimeZoneSuggestion {
        public final int score;
        public final TelephonyTimeZoneSuggestion suggestion;

        public QualifiedTelephonyTimeZoneSuggestion(TelephonyTimeZoneSuggestion suggestion, int score) {
            this.suggestion = suggestion;
            this.score = score;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            QualifiedTelephonyTimeZoneSuggestion that = (QualifiedTelephonyTimeZoneSuggestion) o;
            if (this.score == that.score && this.suggestion.equals(that.suggestion)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(Integer.valueOf(this.score), this.suggestion);
        }

        public String toString() {
            return "QualifiedTelephonyTimeZoneSuggestion{suggestion=" + this.suggestion + ", score=" + this.score + '}';
        }
    }

    private static String formatDebugString(TimestampedValue<?> value) {
        return value.getValue() + " @ " + Duration.ofMillis(value.getReferenceTimeMillis());
    }
}
