package android.app.time;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.TimestampedValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
@SystemApi
/* loaded from: classes.dex */
public final class ExternalTimeSuggestion implements Parcelable {
    public static final Parcelable.Creator<ExternalTimeSuggestion> CREATOR = new Parcelable.Creator<ExternalTimeSuggestion>() { // from class: android.app.time.ExternalTimeSuggestion.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ExternalTimeSuggestion createFromParcel(Parcel in) {
            return ExternalTimeSuggestion.createFromParcel(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ExternalTimeSuggestion[] newArray(int size) {
            return new ExternalTimeSuggestion[size];
        }
    };
    private ArrayList<String> mDebugInfo;
    private final TimestampedValue<Long> mUnixEpochTime;

    public ExternalTimeSuggestion(long elapsedRealtimeMillis, long suggestionMillis) {
        this.mUnixEpochTime = new TimestampedValue<>(elapsedRealtimeMillis, Long.valueOf(suggestionMillis));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static ExternalTimeSuggestion createFromParcel(Parcel in) {
        TimestampedValue<Long> utcTime = (TimestampedValue) in.readParcelable(null, TimestampedValue.class);
        ExternalTimeSuggestion suggestion = new ExternalTimeSuggestion(utcTime.getReferenceTimeMillis(), utcTime.getValue().longValue());
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
        if (arrayList == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(arrayList);
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
        ExternalTimeSuggestion that = (ExternalTimeSuggestion) o;
        return Objects.equals(this.mUnixEpochTime, that.mUnixEpochTime);
    }

    public int hashCode() {
        return Objects.hash(this.mUnixEpochTime);
    }

    public String toString() {
        return "ExternalTimeSuggestion{mUnixEpochTime=" + this.mUnixEpochTime + ", mDebugInfo=" + this.mDebugInfo + '}';
    }
}
