package android.app.timedetector;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.TimestampedValue;
import android.text.format.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
/* loaded from: classes.dex */
public final class TelephonyTimeSuggestion implements Parcelable {
    public static final Parcelable.Creator<TelephonyTimeSuggestion> CREATOR = new Parcelable.Creator<TelephonyTimeSuggestion>() { // from class: android.app.timedetector.TelephonyTimeSuggestion.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TelephonyTimeSuggestion createFromParcel(Parcel in) {
            return TelephonyTimeSuggestion.createFromParcel(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TelephonyTimeSuggestion[] newArray(int size) {
            return new TelephonyTimeSuggestion[size];
        }
    };
    private ArrayList<String> mDebugInfo;
    private final int mSlotIndex;
    private final TimestampedValue<Long> mUnixEpochTime;

    private TelephonyTimeSuggestion(Builder builder) {
        this.mSlotIndex = builder.mSlotIndex;
        this.mUnixEpochTime = builder.mUnixEpochTime;
        this.mDebugInfo = builder.mDebugInfo != null ? new ArrayList<>(builder.mDebugInfo) : null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static TelephonyTimeSuggestion createFromParcel(Parcel in) {
        int slotIndex = in.readInt();
        TimestampedValue<Long> unixEpochTime = (TimestampedValue) in.readParcelable(null, TimestampedValue.class);
        TelephonyTimeSuggestion suggestion = new Builder(slotIndex).setUnixEpochTime(unixEpochTime).build();
        ArrayList<String> debugInfo = in.readArrayList(null, String.class);
        if (debugInfo != null) {
            suggestion.addDebugInfo(debugInfo);
        }
        return suggestion;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mSlotIndex);
        dest.writeParcelable(this.mUnixEpochTime, 0);
        dest.writeList(this.mDebugInfo);
    }

    public int getSlotIndex() {
        return this.mSlotIndex;
    }

    public TimestampedValue<Long> getUnixEpochTime() {
        return this.mUnixEpochTime;
    }

    public List<String> getDebugInfo() {
        ArrayList<String> arrayList = this.mDebugInfo;
        return arrayList == null ? Collections.emptyList() : Collections.unmodifiableList(arrayList);
    }

    public void addDebugInfo(String debugInfo) {
        if (this.mDebugInfo == null) {
            this.mDebugInfo = new ArrayList<>();
        }
        this.mDebugInfo.add(debugInfo);
    }

    public void addDebugInfo(List<String> debugInfo) {
        if (this.mDebugInfo == null) {
            this.mDebugInfo = new ArrayList<>(debugInfo.size());
        }
        this.mDebugInfo.addAll(debugInfo);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TelephonyTimeSuggestion that = (TelephonyTimeSuggestion) o;
        if (this.mSlotIndex == that.mSlotIndex && Objects.equals(this.mUnixEpochTime, that.mUnixEpochTime)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mSlotIndex), this.mUnixEpochTime);
    }

    public String toString() {
        return "TelephonyTimeSuggestion{mSlotIndex='" + this.mSlotIndex + DateFormat.QUOTE + ", mUnixEpochTime=" + this.mUnixEpochTime + ", mDebugInfo=" + this.mDebugInfo + '}';
    }

    /* loaded from: classes.dex */
    public static final class Builder {
        private List<String> mDebugInfo;
        private final int mSlotIndex;
        private TimestampedValue<Long> mUnixEpochTime;

        public Builder(int slotIndex) {
            this.mSlotIndex = slotIndex;
        }

        public Builder setUnixEpochTime(TimestampedValue<Long> unixEpochTime) {
            if (unixEpochTime != null) {
                Objects.requireNonNull(unixEpochTime.getValue());
            }
            this.mUnixEpochTime = unixEpochTime;
            return this;
        }

        public Builder addDebugInfo(String debugInfo) {
            if (this.mDebugInfo == null) {
                this.mDebugInfo = new ArrayList();
            }
            this.mDebugInfo.add(debugInfo);
            return this;
        }

        public TelephonyTimeSuggestion build() {
            return new TelephonyTimeSuggestion(this);
        }
    }
}
