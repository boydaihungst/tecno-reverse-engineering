package com.android.server.timezonedetector;

import android.app.time.TimeZoneCapabilities;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.app.time.TimeZoneConfiguration;
import android.os.UserHandle;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;
/* loaded from: classes2.dex */
public final class ConfigurationInternal {
    public static final int DETECTION_MODE_GEO = 2;
    public static final int DETECTION_MODE_MANUAL = 1;
    public static final int DETECTION_MODE_TELEPHONY = 3;
    public static final int DETECTION_MODE_UNKNOWN = 0;
    private final boolean mAutoDetectionEnabledSetting;
    private final boolean mEnhancedMetricsCollectionEnabled;
    private final boolean mGeoDetectionEnabledSetting;
    private final boolean mGeoDetectionRunInBackgroundEnabled;
    private final boolean mGeoDetectionSupported;
    private final boolean mLocationEnabledSetting;
    private final boolean mTelephonyDetectionSupported;
    private final boolean mTelephonyFallbackSupported;
    private final boolean mUserConfigAllowed;
    private final int mUserId;

    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface DetectionMode {
    }

    private ConfigurationInternal(Builder builder) {
        this.mTelephonyDetectionSupported = builder.mTelephonyDetectionSupported;
        this.mGeoDetectionSupported = builder.mGeoDetectionSupported;
        this.mTelephonyFallbackSupported = builder.mTelephonyFallbackSupported;
        this.mGeoDetectionRunInBackgroundEnabled = builder.mGeoDetectionRunInBackgroundEnabled;
        this.mEnhancedMetricsCollectionEnabled = builder.mEnhancedMetricsCollectionEnabled;
        this.mAutoDetectionEnabledSetting = builder.mAutoDetectionEnabledSetting;
        this.mUserId = builder.mUserId;
        this.mUserConfigAllowed = builder.mUserConfigAllowed;
        this.mLocationEnabledSetting = builder.mLocationEnabledSetting;
        this.mGeoDetectionEnabledSetting = builder.mGeoDetectionEnabledSetting;
    }

    public boolean isAutoDetectionSupported() {
        return this.mTelephonyDetectionSupported || this.mGeoDetectionSupported;
    }

    public boolean isTelephonyDetectionSupported() {
        return this.mTelephonyDetectionSupported;
    }

    public boolean isGeoDetectionSupported() {
        return this.mGeoDetectionSupported;
    }

    public boolean isTelephonyFallbackSupported() {
        return this.mTelephonyFallbackSupported;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getGeoDetectionRunInBackgroundEnabled() {
        return this.mGeoDetectionRunInBackgroundEnabled;
    }

    public boolean isEnhancedMetricsCollectionEnabled() {
        return this.mEnhancedMetricsCollectionEnabled;
    }

    public boolean getAutoDetectionEnabledSetting() {
        return this.mAutoDetectionEnabledSetting;
    }

    public boolean getAutoDetectionEnabledBehavior() {
        return isAutoDetectionSupported() && this.mAutoDetectionEnabledSetting;
    }

    public int getUserId() {
        return this.mUserId;
    }

    public UserHandle getUserHandle() {
        return UserHandle.of(this.mUserId);
    }

    public boolean isUserConfigAllowed() {
        return this.mUserConfigAllowed;
    }

    public boolean getLocationEnabledSetting() {
        return this.mLocationEnabledSetting;
    }

    public boolean getGeoDetectionEnabledSetting() {
        return this.mGeoDetectionEnabledSetting;
    }

    public int getDetectionMode() {
        if (!getAutoDetectionEnabledBehavior()) {
            return 1;
        }
        if (isGeoDetectionSupported() && getLocationEnabledSetting() && getGeoDetectionEnabledSetting()) {
            return 2;
        }
        return 3;
    }

    public boolean isGeoDetectionExecutionEnabled() {
        return isGeoDetectionSupported() && getLocationEnabledSetting() && ((this.mAutoDetectionEnabledSetting && getGeoDetectionEnabledSetting()) || getGeoDetectionRunInBackgroundEnabled());
    }

    public TimeZoneCapabilitiesAndConfig createCapabilitiesAndConfig() {
        return new TimeZoneCapabilitiesAndConfig(asCapabilities(), asConfiguration());
    }

    private TimeZoneCapabilities asCapabilities() {
        int configureAutoDetectionEnabledCapability;
        int configureGeolocationDetectionEnabledCapability;
        int suggestManualTimeZoneCapability;
        UserHandle userHandle = UserHandle.of(this.mUserId);
        TimeZoneCapabilities.Builder builder = new TimeZoneCapabilities.Builder(userHandle);
        boolean allowConfigDateTime = isUserConfigAllowed();
        boolean deviceHasAutoTimeZoneDetection = isAutoDetectionSupported();
        if (!deviceHasAutoTimeZoneDetection) {
            configureAutoDetectionEnabledCapability = 10;
        } else if (!allowConfigDateTime) {
            configureAutoDetectionEnabledCapability = 20;
        } else {
            configureAutoDetectionEnabledCapability = 40;
        }
        builder.setConfigureAutoDetectionEnabledCapability(configureAutoDetectionEnabledCapability);
        boolean deviceHasLocationTimeZoneDetection = isGeoDetectionSupported();
        if (!deviceHasLocationTimeZoneDetection) {
            configureGeolocationDetectionEnabledCapability = 10;
        } else if (!this.mAutoDetectionEnabledSetting || !getLocationEnabledSetting()) {
            configureGeolocationDetectionEnabledCapability = 30;
        } else {
            configureGeolocationDetectionEnabledCapability = 40;
        }
        builder.setConfigureGeoDetectionEnabledCapability(configureGeolocationDetectionEnabledCapability);
        if (!allowConfigDateTime) {
            suggestManualTimeZoneCapability = 20;
        } else if (getAutoDetectionEnabledBehavior()) {
            suggestManualTimeZoneCapability = 30;
        } else {
            suggestManualTimeZoneCapability = 40;
        }
        builder.setSuggestManualTimeZoneCapability(suggestManualTimeZoneCapability);
        return builder.build();
    }

    private TimeZoneConfiguration asConfiguration() {
        return new TimeZoneConfiguration.Builder().setAutoDetectionEnabled(getAutoDetectionEnabledSetting()).setGeoDetectionEnabled(getGeoDetectionEnabledSetting()).build();
    }

    public ConfigurationInternal merge(TimeZoneConfiguration newConfiguration) {
        Builder builder = new Builder(this);
        if (newConfiguration.hasIsAutoDetectionEnabled()) {
            builder.setAutoDetectionEnabledSetting(newConfiguration.isAutoDetectionEnabled());
        }
        if (newConfiguration.hasIsGeoDetectionEnabled()) {
            builder.setGeoDetectionEnabledSetting(newConfiguration.isGeoDetectionEnabled());
        }
        return builder.build();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConfigurationInternal that = (ConfigurationInternal) o;
        if (this.mUserId == that.mUserId && this.mUserConfigAllowed == that.mUserConfigAllowed && this.mTelephonyDetectionSupported == that.mTelephonyDetectionSupported && this.mGeoDetectionSupported == that.mGeoDetectionSupported && this.mTelephonyFallbackSupported == that.mTelephonyFallbackSupported && this.mGeoDetectionRunInBackgroundEnabled == that.mGeoDetectionRunInBackgroundEnabled && this.mEnhancedMetricsCollectionEnabled == that.mEnhancedMetricsCollectionEnabled && this.mAutoDetectionEnabledSetting == that.mAutoDetectionEnabledSetting && this.mLocationEnabledSetting == that.mLocationEnabledSetting && this.mGeoDetectionEnabledSetting == that.mGeoDetectionEnabledSetting) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mUserId), Boolean.valueOf(this.mUserConfigAllowed), Boolean.valueOf(this.mTelephonyDetectionSupported), Boolean.valueOf(this.mGeoDetectionSupported), Boolean.valueOf(this.mTelephonyFallbackSupported), Boolean.valueOf(this.mGeoDetectionRunInBackgroundEnabled), Boolean.valueOf(this.mEnhancedMetricsCollectionEnabled), Boolean.valueOf(this.mAutoDetectionEnabledSetting), Boolean.valueOf(this.mLocationEnabledSetting), Boolean.valueOf(this.mGeoDetectionEnabledSetting));
    }

    public String toString() {
        return "ConfigurationInternal{mUserId=" + this.mUserId + ", mUserConfigAllowed=" + this.mUserConfigAllowed + ", mTelephonyDetectionSupported=" + this.mTelephonyDetectionSupported + ", mGeoDetectionSupported=" + this.mGeoDetectionSupported + ", mTelephonyFallbackSupported=" + this.mTelephonyFallbackSupported + ", mGeoDetectionRunInBackgroundEnabled=" + this.mGeoDetectionRunInBackgroundEnabled + ", mEnhancedMetricsCollectionEnabled=" + this.mEnhancedMetricsCollectionEnabled + ", mAutoDetectionEnabledSetting=" + this.mAutoDetectionEnabledSetting + ", mLocationEnabledSetting=" + this.mLocationEnabledSetting + ", mGeoDetectionEnabledSetting=" + this.mGeoDetectionEnabledSetting + '}';
    }

    /* loaded from: classes2.dex */
    public static class Builder {
        private boolean mAutoDetectionEnabledSetting;
        private boolean mEnhancedMetricsCollectionEnabled;
        private boolean mGeoDetectionEnabledSetting;
        private boolean mGeoDetectionRunInBackgroundEnabled;
        private boolean mGeoDetectionSupported;
        private boolean mLocationEnabledSetting;
        private boolean mTelephonyDetectionSupported;
        private boolean mTelephonyFallbackSupported;
        private boolean mUserConfigAllowed;
        private final int mUserId;

        public Builder(int userId) {
            this.mUserId = userId;
        }

        public Builder(ConfigurationInternal toCopy) {
            this.mUserId = toCopy.mUserId;
            this.mUserConfigAllowed = toCopy.mUserConfigAllowed;
            this.mTelephonyDetectionSupported = toCopy.mTelephonyDetectionSupported;
            this.mTelephonyFallbackSupported = toCopy.mTelephonyFallbackSupported;
            this.mGeoDetectionSupported = toCopy.mGeoDetectionSupported;
            this.mGeoDetectionRunInBackgroundEnabled = toCopy.mGeoDetectionRunInBackgroundEnabled;
            this.mEnhancedMetricsCollectionEnabled = toCopy.mEnhancedMetricsCollectionEnabled;
            this.mAutoDetectionEnabledSetting = toCopy.mAutoDetectionEnabledSetting;
            this.mLocationEnabledSetting = toCopy.mLocationEnabledSetting;
            this.mGeoDetectionEnabledSetting = toCopy.mGeoDetectionEnabledSetting;
        }

        public Builder setUserConfigAllowed(boolean configAllowed) {
            this.mUserConfigAllowed = configAllowed;
            return this;
        }

        public Builder setTelephonyDetectionFeatureSupported(boolean supported) {
            this.mTelephonyDetectionSupported = supported;
            return this;
        }

        public Builder setGeoDetectionFeatureSupported(boolean supported) {
            this.mGeoDetectionSupported = supported;
            return this;
        }

        public Builder setTelephonyFallbackSupported(boolean supported) {
            this.mTelephonyFallbackSupported = supported;
            return this;
        }

        public Builder setGeoDetectionRunInBackgroundEnabled(boolean enabled) {
            this.mGeoDetectionRunInBackgroundEnabled = enabled;
            return this;
        }

        public Builder setEnhancedMetricsCollectionEnabled(boolean enabled) {
            this.mEnhancedMetricsCollectionEnabled = enabled;
            return this;
        }

        public Builder setAutoDetectionEnabledSetting(boolean enabled) {
            this.mAutoDetectionEnabledSetting = enabled;
            return this;
        }

        public Builder setLocationEnabledSetting(boolean enabled) {
            this.mLocationEnabledSetting = enabled;
            return this;
        }

        public Builder setGeoDetectionEnabledSetting(boolean enabled) {
            this.mGeoDetectionEnabledSetting = enabled;
            return this;
        }

        public ConfigurationInternal build() {
            return new ConfigurationInternal(this);
        }
    }
}
