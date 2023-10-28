package android.app.time;

import android.annotation.SystemApi;
import android.app.time.TimeZoneConfiguration;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import java.util.Objects;
@SystemApi
/* loaded from: classes.dex */
public final class TimeZoneCapabilities implements Parcelable {
    public static final Parcelable.Creator<TimeZoneCapabilities> CREATOR = new Parcelable.Creator<TimeZoneCapabilities>() { // from class: android.app.time.TimeZoneCapabilities.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TimeZoneCapabilities createFromParcel(Parcel in) {
            return TimeZoneCapabilities.createFromParcel(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TimeZoneCapabilities[] newArray(int size) {
            return new TimeZoneCapabilities[size];
        }
    };
    private final int mConfigureAutoDetectionEnabledCapability;
    private final int mConfigureGeoDetectionEnabledCapability;
    private final int mSuggestManualTimeZoneCapability;
    private final UserHandle mUserHandle;

    private TimeZoneCapabilities(Builder builder) {
        this.mUserHandle = (UserHandle) Objects.requireNonNull(builder.mUserHandle);
        this.mConfigureAutoDetectionEnabledCapability = builder.mConfigureAutoDetectionEnabledCapability;
        this.mConfigureGeoDetectionEnabledCapability = builder.mConfigureGeoDetectionEnabledCapability;
        this.mSuggestManualTimeZoneCapability = builder.mSuggestManualTimeZoneCapability;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static TimeZoneCapabilities createFromParcel(Parcel in) {
        UserHandle userHandle = UserHandle.readFromParcel(in);
        return new Builder(userHandle).setConfigureAutoDetectionEnabledCapability(in.readInt()).setConfigureGeoDetectionEnabledCapability(in.readInt()).setSuggestManualTimeZoneCapability(in.readInt()).build();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        UserHandle.writeToParcel(this.mUserHandle, dest);
        dest.writeInt(this.mConfigureAutoDetectionEnabledCapability);
        dest.writeInt(this.mConfigureGeoDetectionEnabledCapability);
        dest.writeInt(this.mSuggestManualTimeZoneCapability);
    }

    public int getConfigureAutoDetectionEnabledCapability() {
        return this.mConfigureAutoDetectionEnabledCapability;
    }

    public int getConfigureGeoDetectionEnabledCapability() {
        return this.mConfigureGeoDetectionEnabledCapability;
    }

    public int getSuggestManualTimeZoneCapability() {
        return this.mSuggestManualTimeZoneCapability;
    }

    public TimeZoneConfiguration tryApplyConfigChanges(TimeZoneConfiguration config, TimeZoneConfiguration requestedChanges) {
        TimeZoneConfiguration.Builder newConfigBuilder = new TimeZoneConfiguration.Builder(config);
        if (requestedChanges.hasIsAutoDetectionEnabled()) {
            if (getConfigureAutoDetectionEnabledCapability() < 30) {
                return null;
            }
            newConfigBuilder.setAutoDetectionEnabled(requestedChanges.isAutoDetectionEnabled());
        }
        if (requestedChanges.hasIsGeoDetectionEnabled()) {
            if (getConfigureGeoDetectionEnabledCapability() < 30) {
                return null;
            }
            newConfigBuilder.setGeoDetectionEnabled(requestedChanges.isGeoDetectionEnabled());
        }
        return newConfigBuilder.build();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TimeZoneCapabilities that = (TimeZoneCapabilities) o;
        if (this.mUserHandle.equals(that.mUserHandle) && this.mConfigureAutoDetectionEnabledCapability == that.mConfigureAutoDetectionEnabledCapability && this.mConfigureGeoDetectionEnabledCapability == that.mConfigureGeoDetectionEnabledCapability && this.mSuggestManualTimeZoneCapability == that.mSuggestManualTimeZoneCapability) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.mUserHandle, Integer.valueOf(this.mConfigureAutoDetectionEnabledCapability), Integer.valueOf(this.mConfigureGeoDetectionEnabledCapability), Integer.valueOf(this.mSuggestManualTimeZoneCapability));
    }

    public String toString() {
        return "TimeZoneDetectorCapabilities{mUserHandle=" + this.mUserHandle + ", mConfigureAutoDetectionEnabledCapability=" + this.mConfigureAutoDetectionEnabledCapability + ", mConfigureGeoDetectionEnabledCapability=" + this.mConfigureGeoDetectionEnabledCapability + ", mSuggestManualTimeZoneCapability=" + this.mSuggestManualTimeZoneCapability + '}';
    }

    /* loaded from: classes.dex */
    public static class Builder {
        private int mConfigureAutoDetectionEnabledCapability;
        private int mConfigureGeoDetectionEnabledCapability;
        private int mSuggestManualTimeZoneCapability;
        private UserHandle mUserHandle;

        public Builder(UserHandle userHandle) {
            this.mUserHandle = (UserHandle) Objects.requireNonNull(userHandle);
        }

        public Builder(TimeZoneCapabilities capabilitiesToCopy) {
            Objects.requireNonNull(capabilitiesToCopy);
            this.mUserHandle = capabilitiesToCopy.mUserHandle;
            this.mConfigureAutoDetectionEnabledCapability = capabilitiesToCopy.mConfigureAutoDetectionEnabledCapability;
            this.mConfigureGeoDetectionEnabledCapability = capabilitiesToCopy.mConfigureGeoDetectionEnabledCapability;
            this.mSuggestManualTimeZoneCapability = capabilitiesToCopy.mSuggestManualTimeZoneCapability;
        }

        public Builder setConfigureAutoDetectionEnabledCapability(int value) {
            this.mConfigureAutoDetectionEnabledCapability = value;
            return this;
        }

        public Builder setConfigureGeoDetectionEnabledCapability(int value) {
            this.mConfigureGeoDetectionEnabledCapability = value;
            return this;
        }

        public Builder setSuggestManualTimeZoneCapability(int value) {
            this.mSuggestManualTimeZoneCapability = value;
            return this;
        }

        public TimeZoneCapabilities build() {
            verifyCapabilitySet(this.mConfigureAutoDetectionEnabledCapability, "configureAutoDetectionEnabledCapability");
            verifyCapabilitySet(this.mConfigureGeoDetectionEnabledCapability, "configureGeoDetectionEnabledCapability");
            verifyCapabilitySet(this.mSuggestManualTimeZoneCapability, "suggestManualTimeZoneCapability");
            return new TimeZoneCapabilities(this);
        }

        private void verifyCapabilitySet(int value, String name) {
            if (value == 0) {
                throw new IllegalStateException(name + " not set");
            }
        }
    }
}
