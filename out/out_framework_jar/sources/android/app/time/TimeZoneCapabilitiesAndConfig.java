package android.app.time;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;
@SystemApi
/* loaded from: classes.dex */
public final class TimeZoneCapabilitiesAndConfig implements Parcelable {
    public static final Parcelable.Creator<TimeZoneCapabilitiesAndConfig> CREATOR = new Parcelable.Creator<TimeZoneCapabilitiesAndConfig>() { // from class: android.app.time.TimeZoneCapabilitiesAndConfig.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TimeZoneCapabilitiesAndConfig createFromParcel(Parcel in) {
            return TimeZoneCapabilitiesAndConfig.createFromParcel(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TimeZoneCapabilitiesAndConfig[] newArray(int size) {
            return new TimeZoneCapabilitiesAndConfig[size];
        }
    };
    private final TimeZoneCapabilities mCapabilities;
    private final TimeZoneConfiguration mConfiguration;

    public TimeZoneCapabilitiesAndConfig(TimeZoneCapabilities capabilities, TimeZoneConfiguration configuration) {
        this.mCapabilities = (TimeZoneCapabilities) Objects.requireNonNull(capabilities);
        this.mConfiguration = (TimeZoneConfiguration) Objects.requireNonNull(configuration);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static TimeZoneCapabilitiesAndConfig createFromParcel(Parcel in) {
        TimeZoneCapabilities capabilities = (TimeZoneCapabilities) in.readParcelable(null, TimeZoneCapabilities.class);
        TimeZoneConfiguration configuration = (TimeZoneConfiguration) in.readParcelable(null, TimeZoneConfiguration.class);
        return new TimeZoneCapabilitiesAndConfig(capabilities, configuration);
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mCapabilities, flags);
        dest.writeParcelable(this.mConfiguration, flags);
    }

    public TimeZoneCapabilities getCapabilities() {
        return this.mCapabilities;
    }

    public TimeZoneConfiguration getConfiguration() {
        return this.mConfiguration;
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
        TimeZoneCapabilitiesAndConfig that = (TimeZoneCapabilitiesAndConfig) o;
        if (this.mCapabilities.equals(that.mCapabilities) && this.mConfiguration.equals(that.mConfiguration)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.mCapabilities, this.mConfiguration);
    }

    public String toString() {
        return "TimeZoneCapabilitiesAndConfig{mCapabilities=" + this.mCapabilities + ", mConfiguration=" + this.mConfiguration + '}';
    }
}
