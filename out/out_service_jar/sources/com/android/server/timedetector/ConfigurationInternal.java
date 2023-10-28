package com.android.server.timedetector;

import android.app.time.TimeCapabilities;
import android.app.time.TimeCapabilitiesAndConfig;
import android.app.time.TimeConfiguration;
import android.os.UserHandle;
import java.util.Objects;
/* loaded from: classes2.dex */
public final class ConfigurationInternal {
    private final boolean mAutoDetectionEnabled;
    private final boolean mUserConfigAllowed;
    private final int mUserId;

    private ConfigurationInternal(Builder builder) {
        this.mUserId = builder.mUserId;
        this.mUserConfigAllowed = builder.mUserConfigAllowed;
        this.mAutoDetectionEnabled = builder.mAutoDetectionEnabled;
    }

    public TimeCapabilitiesAndConfig capabilitiesAndConfig() {
        return new TimeCapabilitiesAndConfig(timeCapabilities(), timeConfiguration());
    }

    private TimeConfiguration timeConfiguration() {
        return new TimeConfiguration.Builder().setAutoDetectionEnabled(this.mAutoDetectionEnabled).build();
    }

    private TimeCapabilities timeCapabilities() {
        int configureAutoTimeDetectionEnabledCapability;
        boolean z = this.mUserConfigAllowed;
        if (z) {
            configureAutoTimeDetectionEnabledCapability = 40;
        } else {
            configureAutoTimeDetectionEnabledCapability = 20;
        }
        int suggestTimeManuallyCapability = z ? 40 : 20;
        return new TimeCapabilities.Builder(UserHandle.of(this.mUserId)).setConfigureAutoTimeDetectionEnabledCapability(configureAutoTimeDetectionEnabledCapability).setSuggestTimeManuallyCapability(suggestTimeManuallyCapability).build();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConfigurationInternal that = (ConfigurationInternal) o;
        if (this.mUserId == that.mUserId && this.mUserConfigAllowed == that.mUserConfigAllowed && this.mAutoDetectionEnabled == that.mAutoDetectionEnabled) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mUserId), Boolean.valueOf(this.mUserConfigAllowed), Boolean.valueOf(this.mAutoDetectionEnabled));
    }

    public String toString() {
        return "ConfigurationInternal{mUserId=" + this.mUserId + ", mUserConfigAllowed=" + this.mUserConfigAllowed + ", mAutoDetectionEnabled=" + this.mAutoDetectionEnabled + '}';
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class Builder {
        private boolean mAutoDetectionEnabled;
        private boolean mUserConfigAllowed;
        private final int mUserId;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder(int userId) {
            this.mUserId = userId;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setUserConfigAllowed(boolean userConfigAllowed) {
            this.mUserConfigAllowed = userConfigAllowed;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setAutoDetectionEnabled(boolean autoDetectionEnabled) {
            this.mAutoDetectionEnabled = autoDetectionEnabled;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public ConfigurationInternal build() {
            return new ConfigurationInternal(this);
        }
    }
}
