package android.app.timedetector;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.TimestampedValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
/* loaded from: classes.dex */
public final class ManualTimeSuggestion implements Parcelable {
    public static final Parcelable.Creator<ManualTimeSuggestion> CREATOR = new Parcelable.Creator<ManualTimeSuggestion>() { // from class: android.app.timedetector.ManualTimeSuggestion.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ManualTimeSuggestion createFromParcel(Parcel in) {
            return ManualTimeSuggestion.createFromParcel(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ManualTimeSuggestion[] newArray(int size) {
            return new ManualTimeSuggestion[size];
        }
    };
    private ArrayList<String> mDebugInfo;
    private final TimestampedValue<Long> mUnixEpochTime;

    public ManualTimeSuggestion(TimestampedValue<Long> unixEpochTime) {
        this.mUnixEpochTime = (TimestampedValue) Objects.requireNonNull(unixEpochTime);
        Objects.requireNonNull(unixEpochTime.getValue());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static ManualTimeSuggestion createFromParcel(Parcel in) {
        TimestampedValue<Long> unixEpochTime = (TimestampedValue) in.readParcelable(null, TimestampedValue.class);
        ManualTimeSuggestion suggestion = new ManualTimeSuggestion(unixEpochTime);
        ArrayList<String> debugInfo = in.readArrayList(null, String.class);
        suggestion.mDebugInfo = debugInfo;
        return suggestion;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mUnixEpochTime, 0);
        dest.writeList(this.mDebugInfo);
    }

    public TimestampedValue<Long> getUnixEpochTime() {
        return this.mUnixEpochTime;
    }

    public List<String> getDebugInfo() {
        ArrayList<String> arrayList = this.mDebugInfo;
        return arrayList == null ? Collections.emptyList() : Collections.unmodifiableList(arrayList);
    }

    public void addDebugInfo(String... debugInfos) {
        if (this.mDebugInfo == null) {
            this.mDebugInfo = new ArrayList<>();
        }
        this.mDebugInfo.addAll(Arrays.asList(debugInfos));
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ManualTimeSuggestion that = (ManualTimeSuggestion) o;
        return Objects.equals(this.mUnixEpochTime, that.mUnixEpochTime);
    }

    public int hashCode() {
        return Objects.hash(this.mUnixEpochTime);
    }

    public String toString() {
        return "ManualTimeSuggestion{mUnixEpochTime=" + this.mUnixEpochTime + ", mDebugInfo=" + this.mDebugInfo + '}';
    }
}
