package android.service.timezone;

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.Objects;
/* loaded from: classes3.dex */
public final class TimeZoneProviderEvent implements Parcelable {
    public static final Parcelable.Creator<TimeZoneProviderEvent> CREATOR = new Parcelable.Creator<TimeZoneProviderEvent>() { // from class: android.service.timezone.TimeZoneProviderEvent.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TimeZoneProviderEvent createFromParcel(Parcel in) {
            int type = in.readInt();
            long creationElapsedMillis = in.readLong();
            TimeZoneProviderSuggestion suggestion = (TimeZoneProviderSuggestion) in.readParcelable(getClass().getClassLoader(), TimeZoneProviderSuggestion.class);
            String failureCause = in.readString8();
            return new TimeZoneProviderEvent(type, creationElapsedMillis, suggestion, failureCause);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TimeZoneProviderEvent[] newArray(int size) {
            return new TimeZoneProviderEvent[size];
        }
    };
    public static final int EVENT_TYPE_PERMANENT_FAILURE = 1;
    public static final int EVENT_TYPE_SUGGESTION = 2;
    public static final int EVENT_TYPE_UNCERTAIN = 3;
    private final long mCreationElapsedMillis;
    private final String mFailureCause;
    private final TimeZoneProviderSuggestion mSuggestion;
    private final int mType;

    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    public @interface EventType {
    }

    private TimeZoneProviderEvent(int type, long creationElapsedMillis, TimeZoneProviderSuggestion suggestion, String failureCause) {
        this.mType = type;
        this.mCreationElapsedMillis = creationElapsedMillis;
        this.mSuggestion = suggestion;
        this.mFailureCause = failureCause;
    }

    public static TimeZoneProviderEvent createSuggestionEvent(long creationElapsedMillis, TimeZoneProviderSuggestion suggestion) {
        return new TimeZoneProviderEvent(2, creationElapsedMillis, (TimeZoneProviderSuggestion) Objects.requireNonNull(suggestion), null);
    }

    public static TimeZoneProviderEvent createUncertainEvent(long creationElapsedMillis) {
        return new TimeZoneProviderEvent(3, creationElapsedMillis, null, null);
    }

    public static TimeZoneProviderEvent createPermanentFailureEvent(long creationElapsedMillis, String cause) {
        return new TimeZoneProviderEvent(1, creationElapsedMillis, null, (String) Objects.requireNonNull(cause));
    }

    public int getType() {
        return this.mType;
    }

    public long getCreationElapsedMillis() {
        return this.mCreationElapsedMillis;
    }

    public TimeZoneProviderSuggestion getSuggestion() {
        return this.mSuggestion;
    }

    public String getFailureCause() {
        return this.mFailureCause;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(this.mType);
        parcel.writeLong(this.mCreationElapsedMillis);
        parcel.writeParcelable(this.mSuggestion, 0);
        parcel.writeString8(this.mFailureCause);
    }

    public String toString() {
        return "TimeZoneProviderEvent{mType=" + this.mType + ", mCreationElapsedMillis=" + Duration.ofMillis(this.mCreationElapsedMillis).toString() + ", mSuggestion=" + this.mSuggestion + ", mFailureCause=" + this.mFailureCause + '}';
    }

    public boolean isEquivalentTo(TimeZoneProviderEvent other) {
        int i;
        if (this == other) {
            return true;
        }
        if (other == null || (i = this.mType) != other.mType) {
            return false;
        }
        if (i != 2) {
            return true;
        }
        return this.mSuggestion.isEquivalentTo(other.getSuggestion());
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TimeZoneProviderEvent that = (TimeZoneProviderEvent) o;
        if (this.mType == that.mType && this.mCreationElapsedMillis == that.mCreationElapsedMillis && Objects.equals(this.mSuggestion, that.mSuggestion) && Objects.equals(this.mFailureCause, that.mFailureCause)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mType), Long.valueOf(this.mCreationElapsedMillis), this.mSuggestion, this.mFailureCause);
    }
}
