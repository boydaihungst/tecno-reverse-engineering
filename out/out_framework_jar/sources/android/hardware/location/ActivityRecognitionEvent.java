package android.hardware.location;

import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes2.dex */
public class ActivityRecognitionEvent implements Parcelable {
    public static final Parcelable.Creator<ActivityRecognitionEvent> CREATOR = new Parcelable.Creator<ActivityRecognitionEvent>() { // from class: android.hardware.location.ActivityRecognitionEvent.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ActivityRecognitionEvent createFromParcel(Parcel source) {
            String activity = source.readString();
            int eventType = source.readInt();
            long timestampNs = source.readLong();
            return new ActivityRecognitionEvent(activity, eventType, timestampNs);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ActivityRecognitionEvent[] newArray(int size) {
            return new ActivityRecognitionEvent[size];
        }
    };
    private final String mActivity;
    private final int mEventType;
    private final long mTimestampNs;

    public ActivityRecognitionEvent(String activity, int eventType, long timestampNs) {
        this.mActivity = activity;
        this.mEventType = eventType;
        this.mTimestampNs = timestampNs;
    }

    public String getActivity() {
        return this.mActivity;
    }

    public int getEventType() {
        return this.mEventType;
    }

    public long getTimestampNs() {
        return this.mTimestampNs;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(this.mActivity);
        parcel.writeInt(this.mEventType);
        parcel.writeLong(this.mTimestampNs);
    }

    public String toString() {
        return String.format("Activity='%s', EventType=%s, TimestampNs=%s", this.mActivity, Integer.valueOf(this.mEventType), Long.valueOf(this.mTimestampNs));
    }
}
