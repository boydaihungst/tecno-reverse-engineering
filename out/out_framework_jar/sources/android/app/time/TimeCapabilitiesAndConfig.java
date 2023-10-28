package android.app.time;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;
/* loaded from: classes.dex */
public final class TimeCapabilitiesAndConfig implements Parcelable {
    public static final Parcelable.Creator<TimeCapabilitiesAndConfig> CREATOR = new Parcelable.Creator<TimeCapabilitiesAndConfig>() { // from class: android.app.time.TimeCapabilitiesAndConfig.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TimeCapabilitiesAndConfig createFromParcel(Parcel source) {
            return TimeCapabilitiesAndConfig.readFromParcel(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TimeCapabilitiesAndConfig[] newArray(int size) {
            return new TimeCapabilitiesAndConfig[size];
        }
    };
    private final TimeCapabilities mTimeCapabilities;
    private final TimeConfiguration mTimeConfiguration;

    public TimeCapabilitiesAndConfig(TimeCapabilities timeCapabilities, TimeConfiguration timeConfiguration) {
        this.mTimeCapabilities = (TimeCapabilities) Objects.requireNonNull(timeCapabilities);
        this.mTimeConfiguration = (TimeConfiguration) Objects.requireNonNull(timeConfiguration);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static TimeCapabilitiesAndConfig readFromParcel(Parcel in) {
        TimeCapabilities capabilities = (TimeCapabilities) in.readParcelable(null, TimeCapabilities.class);
        TimeConfiguration configuration = (TimeConfiguration) in.readParcelable(null, TimeConfiguration.class);
        return new TimeCapabilitiesAndConfig(capabilities, configuration);
    }

    public TimeCapabilities getTimeCapabilities() {
        return this.mTimeCapabilities;
    }

    public TimeConfiguration getTimeConfiguration() {
        return this.mTimeConfiguration;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mTimeCapabilities, flags);
        dest.writeParcelable(this.mTimeConfiguration, flags);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TimeCapabilitiesAndConfig that = (TimeCapabilitiesAndConfig) o;
        if (this.mTimeCapabilities.equals(that.mTimeCapabilities) && this.mTimeConfiguration.equals(that.mTimeConfiguration)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.mTimeCapabilities, this.mTimeConfiguration);
    }

    public String toString() {
        return "TimeCapabilitiesAndConfig{mTimeCapabilities=" + this.mTimeCapabilities + ", mTimeConfiguration=" + this.mTimeConfiguration + '}';
    }
}
