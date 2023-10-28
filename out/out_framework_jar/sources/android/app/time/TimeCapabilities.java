package android.app.time;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import java.util.Objects;
/* loaded from: classes.dex */
public final class TimeCapabilities implements Parcelable {
    public static final Parcelable.Creator<TimeCapabilities> CREATOR = new Parcelable.Creator<TimeCapabilities>() { // from class: android.app.time.TimeCapabilities.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TimeCapabilities createFromParcel(Parcel in) {
            return TimeCapabilities.createFromParcel(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TimeCapabilities[] newArray(int size) {
            return new TimeCapabilities[size];
        }
    };
    private final int mConfigureAutoTimeDetectionEnabledCapability;
    private final int mSuggestTimeManuallyCapability;
    private final UserHandle mUserHandle;

    private TimeCapabilities(Builder builder) {
        this.mUserHandle = (UserHandle) Objects.requireNonNull(builder.mUserHandle);
        this.mConfigureAutoTimeDetectionEnabledCapability = builder.mConfigureAutoDetectionEnabledCapability;
        this.mSuggestTimeManuallyCapability = builder.mSuggestTimeManuallyCapability;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static TimeCapabilities createFromParcel(Parcel in) {
        UserHandle userHandle = UserHandle.readFromParcel(in);
        return new Builder(userHandle).setConfigureAutoTimeDetectionEnabledCapability(in.readInt()).setSuggestTimeManuallyCapability(in.readInt()).build();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        UserHandle.writeToParcel(this.mUserHandle, dest);
        dest.writeInt(this.mConfigureAutoTimeDetectionEnabledCapability);
        dest.writeInt(this.mSuggestTimeManuallyCapability);
    }

    public int getConfigureAutoTimeDetectionEnabledCapability() {
        return this.mConfigureAutoTimeDetectionEnabledCapability;
    }

    public int getSuggestTimeManuallyCapability() {
        return this.mSuggestTimeManuallyCapability;
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
        TimeCapabilities that = (TimeCapabilities) o;
        if (this.mConfigureAutoTimeDetectionEnabledCapability == that.mConfigureAutoTimeDetectionEnabledCapability && this.mSuggestTimeManuallyCapability == that.mSuggestTimeManuallyCapability && this.mUserHandle.equals(that.mUserHandle)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.mUserHandle, Integer.valueOf(this.mConfigureAutoTimeDetectionEnabledCapability), Integer.valueOf(this.mSuggestTimeManuallyCapability));
    }

    public String toString() {
        return "TimeCapabilities{mUserHandle=" + this.mUserHandle + ", mConfigureAutoTimeDetectionEnabledCapability=" + this.mConfigureAutoTimeDetectionEnabledCapability + ", mSuggestTimeManuallyCapability=" + this.mSuggestTimeManuallyCapability + '}';
    }

    /* loaded from: classes.dex */
    public static class Builder {
        private int mConfigureAutoDetectionEnabledCapability;
        private int mSuggestTimeManuallyCapability;
        private final UserHandle mUserHandle;

        public Builder(TimeCapabilities timeCapabilities) {
            Objects.requireNonNull(timeCapabilities);
            this.mUserHandle = timeCapabilities.mUserHandle;
            this.mConfigureAutoDetectionEnabledCapability = timeCapabilities.mConfigureAutoTimeDetectionEnabledCapability;
            this.mSuggestTimeManuallyCapability = timeCapabilities.mSuggestTimeManuallyCapability;
        }

        public Builder(UserHandle userHandle) {
            this.mUserHandle = (UserHandle) Objects.requireNonNull(userHandle);
        }

        public Builder setConfigureAutoTimeDetectionEnabledCapability(int setConfigureAutoTimeDetectionEnabledCapability) {
            this.mConfigureAutoDetectionEnabledCapability = setConfigureAutoTimeDetectionEnabledCapability;
            return this;
        }

        public Builder setSuggestTimeManuallyCapability(int suggestTimeManuallyCapability) {
            this.mSuggestTimeManuallyCapability = suggestTimeManuallyCapability;
            return this;
        }

        public TimeCapabilities build() {
            verifyCapabilitySet(this.mConfigureAutoDetectionEnabledCapability, "configureAutoDetectionEnabledCapability");
            verifyCapabilitySet(this.mSuggestTimeManuallyCapability, "suggestTimeManuallyCapability");
            return new TimeCapabilities(this);
        }

        private void verifyCapabilitySet(int value, String name) {
            if (value == 0) {
                throw new IllegalStateException(name + " was not set");
            }
        }
    }
}
