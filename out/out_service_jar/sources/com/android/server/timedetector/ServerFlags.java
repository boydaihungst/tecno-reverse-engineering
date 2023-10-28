package com.android.server.timedetector;

import android.content.Context;
import android.provider.DeviceConfig;
import android.util.ArrayMap;
import com.android.server.timezonedetector.ConfigurationChangeListener;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
/* loaded from: classes2.dex */
public final class ServerFlags {
    public static final String KEY_ENHANCED_METRICS_COLLECTION_ENABLED = "enhanced_metrics_collection_enabled";
    public static final String KEY_LOCATION_TIME_ZONE_DETECTION_FEATURE_SUPPORTED = "location_time_zone_detection_feature_supported";
    public static final String KEY_LOCATION_TIME_ZONE_DETECTION_RUN_IN_BACKGROUND_ENABLED = "location_time_zone_detection_run_in_background_enabled";
    public static final String KEY_LOCATION_TIME_ZONE_DETECTION_SETTING_ENABLED_DEFAULT = "location_time_zone_detection_setting_enabled_default";
    public static final String KEY_LOCATION_TIME_ZONE_DETECTION_SETTING_ENABLED_OVERRIDE = "location_time_zone_detection_setting_enabled_override";
    public static final String KEY_LOCATION_TIME_ZONE_DETECTION_UNCERTAINTY_DELAY_MILLIS = "location_time_zone_detection_uncertainty_delay_millis";
    public static final String KEY_LTZP_EVENT_FILTERING_AGE_THRESHOLD_MILLIS = "ltzp_event_filtering_age_threshold_millis";
    public static final String KEY_LTZP_INITIALIZATION_TIMEOUT_FUZZ_MILLIS = "ltzp_init_timeout_fuzz_millis";
    public static final String KEY_LTZP_INITIALIZATION_TIMEOUT_MILLIS = "ltzp_init_timeout_millis";
    public static final String KEY_PRIMARY_LTZP_MODE_OVERRIDE = "primary_location_time_zone_provider_mode_override";
    public static final String KEY_SECONDARY_LTZP_MODE_OVERRIDE = "secondary_location_time_zone_provider_mode_override";
    public static final String KEY_TIME_DETECTOR_LOWER_BOUND_MILLIS_OVERRIDE = "time_detector_lower_bound_millis_override";
    public static final String KEY_TIME_DETECTOR_ORIGIN_PRIORITIES_OVERRIDE = "time_detector_origin_priorities_override";
    public static final String KEY_TIME_ZONE_DETECTOR_TELEPHONY_FALLBACK_SUPPORTED = "time_zone_detector_telephony_fallback_supported";
    private static ServerFlags sInstance;
    private final ArrayMap<ConfigurationChangeListener, HashSet<String>> mListeners = new ArrayMap<>();
    private static final Optional<Boolean> OPTIONAL_TRUE = Optional.of(true);
    private static final Optional<Boolean> OPTIONAL_FALSE = Optional.of(false);
    private static final Object SLOCK = new Object();

    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface DeviceConfigKey {
    }

    private ServerFlags(Context context) {
        DeviceConfig.addOnPropertiesChangedListener("system_time", context.getMainExecutor(), new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.timedetector.ServerFlags$$ExternalSyntheticLambda0
            public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                ServerFlags.this.handlePropertiesChanged(properties);
            }
        });
    }

    public static ServerFlags getInstance(Context context) {
        ServerFlags serverFlags;
        synchronized (SLOCK) {
            if (sInstance == null) {
                sInstance = new ServerFlags(context);
            }
            serverFlags = sInstance;
        }
        return serverFlags;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePropertiesChanged(DeviceConfig.Properties properties) {
        synchronized (this.mListeners) {
            for (Map.Entry<ConfigurationChangeListener, HashSet<String>> listenerEntry : this.mListeners.entrySet()) {
                HashSet<String> monitoredKeys = listenerEntry.getValue();
                Iterable<String> modifiedKeys = properties.getKeyset();
                if (containsAny(monitoredKeys, modifiedKeys)) {
                    listenerEntry.getKey().onChange();
                }
            }
        }
    }

    private static boolean containsAny(Set<String> haystack, Iterable<String> needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public void addListener(ConfigurationChangeListener listener, Set<String> keys) {
        Objects.requireNonNull(listener);
        Objects.requireNonNull(keys);
        HashSet<String> keysCopy = new HashSet<>(keys);
        synchronized (this.mListeners) {
            this.mListeners.put(listener, keysCopy);
        }
    }

    public Optional<String> getOptionalString(String key) {
        String value = DeviceConfig.getProperty("system_time", key);
        return Optional.ofNullable(value);
    }

    public Optional<String[]> getOptionalStringArray(String key) {
        Optional<String> string = getOptionalString(key);
        if (!string.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(string.get().split(","));
    }

    public Optional<Instant> getOptionalInstant(String key) {
        String value = DeviceConfig.getProperty("system_time", key);
        if (value == null) {
            return Optional.empty();
        }
        try {
            long millis = Long.parseLong(value);
            return Optional.of(Instant.ofEpochMilli(millis));
        } catch (NumberFormatException | DateTimeException e) {
            return Optional.empty();
        }
    }

    public Optional<Boolean> getOptionalBoolean(String key) {
        String value = DeviceConfig.getProperty("system_time", key);
        return parseOptionalBoolean(value);
    }

    private static Optional<Boolean> parseOptionalBoolean(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Boolean.parseBoolean(value) ? OPTIONAL_TRUE : OPTIONAL_FALSE;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return DeviceConfig.getBoolean("system_time", key, defaultValue);
    }

    public Duration getDurationFromMillis(String key, Duration defaultValue) {
        long deviceConfigValue = DeviceConfig.getLong("system_time", key, -1L);
        if (deviceConfigValue < 0) {
            return defaultValue;
        }
        return Duration.ofMillis(deviceConfigValue);
    }
}
