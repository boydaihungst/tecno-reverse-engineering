package android.app.time;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
/* loaded from: classes.dex */
public final class TimeConfiguration implements Parcelable {
    public static final Parcelable.Creator<TimeConfiguration> CREATOR = new Parcelable.Creator<TimeConfiguration>() { // from class: android.app.time.TimeConfiguration.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TimeConfiguration createFromParcel(Parcel source) {
            return TimeConfiguration.readFromParcel(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TimeConfiguration[] newArray(int size) {
            return new TimeConfiguration[size];
        }
    };
    private static final String SETTING_AUTO_DETECTION_ENABLED = "autoDetectionEnabled";
    private final Bundle mBundle;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    @interface Setting {
    }

    private TimeConfiguration(Builder builder) {
        this.mBundle = builder.mBundle;
    }

    public boolean isAutoDetectionEnabled() {
        return this.mBundle.getBoolean(SETTING_AUTO_DETECTION_ENABLED);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(this.mBundle);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static TimeConfiguration readFromParcel(Parcel in) {
        return new Builder().merge(in.readBundle()).build();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TimeConfiguration that = (TimeConfiguration) o;
        return this.mBundle.kindofEquals(that.mBundle);
    }

    public int hashCode() {
        return Objects.hash(this.mBundle);
    }

    public String toString() {
        return "TimeConfiguration{mBundle=" + this.mBundle + '}';
    }

    /* loaded from: classes.dex */
    public static final class Builder {
        private final Bundle mBundle;

        public Builder() {
            this.mBundle = new Bundle();
        }

        public Builder(TimeConfiguration configuration) {
            Bundle bundle = new Bundle();
            this.mBundle = bundle;
            bundle.putAll(configuration.mBundle);
        }

        public Builder setAutoDetectionEnabled(boolean enabled) {
            this.mBundle.putBoolean(TimeConfiguration.SETTING_AUTO_DETECTION_ENABLED, enabled);
            return this;
        }

        Builder merge(Bundle bundle) {
            this.mBundle.putAll(bundle);
            return this;
        }

        public TimeConfiguration build() {
            return new TimeConfiguration(this);
        }
    }
}
