package com.android.server.timezonedetector;

import android.app.timezonedetector.ManualTimeZoneSuggestion;
import android.app.timezonedetector.TelephonyTimeZoneSuggestion;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
/* loaded from: classes2.dex */
public final class MetricsTimeZoneDetectorState {
    public static final int DETECTION_MODE_GEO = 2;
    public static final int DETECTION_MODE_MANUAL = 1;
    public static final int DETECTION_MODE_TELEPHONY = 3;
    public static final int DETECTION_MODE_UNKNOWN = 0;
    private final ConfigurationInternal mConfigurationInternal;
    private final String mDeviceTimeZoneId;
    private final int mDeviceTimeZoneIdOrdinal;
    private final MetricsTimeZoneSuggestion mLatestGeolocationSuggestion;
    private final MetricsTimeZoneSuggestion mLatestManualSuggestion;
    private final MetricsTimeZoneSuggestion mLatestTelephonySuggestion;

    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface DetectionMode {
    }

    private MetricsTimeZoneDetectorState(ConfigurationInternal configurationInternal, int deviceTimeZoneIdOrdinal, String deviceTimeZoneId, MetricsTimeZoneSuggestion latestManualSuggestion, MetricsTimeZoneSuggestion latestTelephonySuggestion, MetricsTimeZoneSuggestion latestGeolocationSuggestion) {
        this.mConfigurationInternal = (ConfigurationInternal) Objects.requireNonNull(configurationInternal);
        this.mDeviceTimeZoneIdOrdinal = deviceTimeZoneIdOrdinal;
        this.mDeviceTimeZoneId = deviceTimeZoneId;
        this.mLatestManualSuggestion = latestManualSuggestion;
        this.mLatestTelephonySuggestion = latestTelephonySuggestion;
        this.mLatestGeolocationSuggestion = latestGeolocationSuggestion;
    }

    public static MetricsTimeZoneDetectorState create(OrdinalGenerator<String> tzIdOrdinalGenerator, ConfigurationInternal configurationInternal, String deviceTimeZoneId, ManualTimeZoneSuggestion latestManualSuggestion, TelephonyTimeZoneSuggestion latestTelephonySuggestion, GeolocationTimeZoneSuggestion latestGeolocationSuggestion) {
        boolean includeZoneIds = configurationInternal.isEnhancedMetricsCollectionEnabled();
        String metricDeviceTimeZoneId = includeZoneIds ? deviceTimeZoneId : null;
        int deviceTimeZoneIdOrdinal = tzIdOrdinalGenerator.ordinal((String) Objects.requireNonNull(deviceTimeZoneId));
        MetricsTimeZoneSuggestion latestCanonicalManualSuggestion = createMetricsTimeZoneSuggestion(tzIdOrdinalGenerator, latestManualSuggestion, includeZoneIds);
        MetricsTimeZoneSuggestion latestCanonicalTelephonySuggestion = createMetricsTimeZoneSuggestion(tzIdOrdinalGenerator, latestTelephonySuggestion, includeZoneIds);
        MetricsTimeZoneSuggestion latestCanonicalGeolocationSuggestion = createMetricsTimeZoneSuggestion(tzIdOrdinalGenerator, latestGeolocationSuggestion, includeZoneIds);
        return new MetricsTimeZoneDetectorState(configurationInternal, deviceTimeZoneIdOrdinal, metricDeviceTimeZoneId, latestCanonicalManualSuggestion, latestCanonicalTelephonySuggestion, latestCanonicalGeolocationSuggestion);
    }

    public boolean isTelephonyDetectionSupported() {
        return this.mConfigurationInternal.isTelephonyDetectionSupported();
    }

    public boolean isGeoDetectionSupported() {
        return this.mConfigurationInternal.isGeoDetectionSupported();
    }

    public boolean isTelephonyTimeZoneFallbackSupported() {
        return this.mConfigurationInternal.isTelephonyFallbackSupported();
    }

    public boolean getGeoDetectionRunInBackgroundEnabled() {
        return this.mConfigurationInternal.getGeoDetectionRunInBackgroundEnabled();
    }

    public boolean isEnhancedMetricsCollectionEnabled() {
        return this.mConfigurationInternal.isEnhancedMetricsCollectionEnabled();
    }

    public boolean getUserLocationEnabledSetting() {
        return this.mConfigurationInternal.getLocationEnabledSetting();
    }

    public boolean getGeoDetectionEnabledSetting() {
        return this.mConfigurationInternal.getGeoDetectionEnabledSetting();
    }

    public boolean getAutoDetectionEnabledSetting() {
        return this.mConfigurationInternal.getAutoDetectionEnabledSetting();
    }

    public int getDetectionMode() {
        switch (this.mConfigurationInternal.getDetectionMode()) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            default:
                return 0;
        }
    }

    public int getDeviceTimeZoneIdOrdinal() {
        return this.mDeviceTimeZoneIdOrdinal;
    }

    public String getDeviceTimeZoneId() {
        return this.mDeviceTimeZoneId;
    }

    public MetricsTimeZoneSuggestion getLatestManualSuggestion() {
        return this.mLatestManualSuggestion;
    }

    public MetricsTimeZoneSuggestion getLatestTelephonySuggestion() {
        return this.mLatestTelephonySuggestion;
    }

    public MetricsTimeZoneSuggestion getLatestGeolocationSuggestion() {
        return this.mLatestGeolocationSuggestion;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MetricsTimeZoneDetectorState that = (MetricsTimeZoneDetectorState) o;
        if (this.mDeviceTimeZoneIdOrdinal == that.mDeviceTimeZoneIdOrdinal && Objects.equals(this.mDeviceTimeZoneId, that.mDeviceTimeZoneId) && this.mConfigurationInternal.equals(that.mConfigurationInternal) && Objects.equals(this.mLatestManualSuggestion, that.mLatestManualSuggestion) && Objects.equals(this.mLatestTelephonySuggestion, that.mLatestTelephonySuggestion) && Objects.equals(this.mLatestGeolocationSuggestion, that.mLatestGeolocationSuggestion)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.mConfigurationInternal, Integer.valueOf(this.mDeviceTimeZoneIdOrdinal), this.mDeviceTimeZoneId, this.mLatestManualSuggestion, this.mLatestTelephonySuggestion, this.mLatestGeolocationSuggestion);
    }

    public String toString() {
        return "MetricsTimeZoneDetectorState{mConfigurationInternal=" + this.mConfigurationInternal + ", mDeviceTimeZoneIdOrdinal=" + this.mDeviceTimeZoneIdOrdinal + ", mDeviceTimeZoneId=" + this.mDeviceTimeZoneId + ", mLatestManualSuggestion=" + this.mLatestManualSuggestion + ", mLatestTelephonySuggestion=" + this.mLatestTelephonySuggestion + ", mLatestGeolocationSuggestion=" + this.mLatestGeolocationSuggestion + '}';
    }

    private static MetricsTimeZoneSuggestion createMetricsTimeZoneSuggestion(OrdinalGenerator<String> zoneIdOrdinalGenerator, ManualTimeZoneSuggestion manualSuggestion, boolean includeFullZoneIds) {
        if (manualSuggestion == null) {
            return null;
        }
        String suggestionZoneId = manualSuggestion.getZoneId();
        String[] metricZoneIds = includeFullZoneIds ? new String[]{suggestionZoneId} : null;
        int[] zoneIdOrdinals = {zoneIdOrdinalGenerator.ordinal(suggestionZoneId)};
        return MetricsTimeZoneSuggestion.createCertain(metricZoneIds, zoneIdOrdinals);
    }

    private static MetricsTimeZoneSuggestion createMetricsTimeZoneSuggestion(OrdinalGenerator<String> zoneIdOrdinalGenerator, TelephonyTimeZoneSuggestion telephonySuggestion, boolean includeFullZoneIds) {
        if (telephonySuggestion == null) {
            return null;
        }
        String suggestionZoneId = telephonySuggestion.getZoneId();
        if (suggestionZoneId == null) {
            return MetricsTimeZoneSuggestion.createUncertain();
        }
        String[] metricZoneIds = includeFullZoneIds ? new String[]{suggestionZoneId} : null;
        int[] zoneIdOrdinals = {zoneIdOrdinalGenerator.ordinal(suggestionZoneId)};
        return MetricsTimeZoneSuggestion.createCertain(metricZoneIds, zoneIdOrdinals);
    }

    private static MetricsTimeZoneSuggestion createMetricsTimeZoneSuggestion(OrdinalGenerator<String> zoneIdOrdinalGenerator, GeolocationTimeZoneSuggestion geolocationSuggestion, boolean includeFullZoneIds) {
        if (geolocationSuggestion == null) {
            return null;
        }
        List<String> zoneIds = geolocationSuggestion.getZoneIds();
        if (zoneIds == null) {
            return MetricsTimeZoneSuggestion.createUncertain();
        }
        String[] metricZoneIds = includeFullZoneIds ? (String[]) zoneIds.toArray(new String[0]) : null;
        int[] zoneIdOrdinals = zoneIdOrdinalGenerator.ordinals(zoneIds);
        return MetricsTimeZoneSuggestion.createCertain(metricZoneIds, zoneIdOrdinals);
    }

    /* loaded from: classes2.dex */
    public static final class MetricsTimeZoneSuggestion {
        private final int[] mZoneIdOrdinals;
        private final String[] mZoneIds;

        private MetricsTimeZoneSuggestion(String[] zoneIds, int[] zoneIdOrdinals) {
            this.mZoneIds = zoneIds;
            this.mZoneIdOrdinals = zoneIdOrdinals;
        }

        static MetricsTimeZoneSuggestion createUncertain() {
            return new MetricsTimeZoneSuggestion(null, null);
        }

        static MetricsTimeZoneSuggestion createCertain(String[] zoneIds, int[] zoneIdOrdinals) {
            return new MetricsTimeZoneSuggestion(zoneIds, zoneIdOrdinals);
        }

        public boolean isCertain() {
            return this.mZoneIdOrdinals != null;
        }

        public int[] getZoneIdOrdinals() {
            return this.mZoneIdOrdinals;
        }

        public String[] getZoneIds() {
            return this.mZoneIds;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MetricsTimeZoneSuggestion that = (MetricsTimeZoneSuggestion) o;
            if (Arrays.equals(this.mZoneIdOrdinals, that.mZoneIdOrdinals) && Arrays.equals(this.mZoneIds, that.mZoneIds)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            int result = Arrays.hashCode(this.mZoneIds);
            return (result * 31) + Arrays.hashCode(this.mZoneIdOrdinals);
        }

        public String toString() {
            return "MetricsTimeZoneSuggestion{mZoneIdOrdinals=" + Arrays.toString(this.mZoneIdOrdinals) + ", mZoneIds=" + Arrays.toString(this.mZoneIds) + '}';
        }
    }
}
