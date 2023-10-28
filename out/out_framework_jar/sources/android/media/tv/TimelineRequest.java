package android.media.tv;

import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes2.dex */
public final class TimelineRequest extends BroadcastInfoRequest implements Parcelable {
    public static final Parcelable.Creator<TimelineRequest> CREATOR = new Parcelable.Creator<TimelineRequest>() { // from class: android.media.tv.TimelineRequest.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TimelineRequest createFromParcel(Parcel source) {
            source.readInt();
            return TimelineRequest.createFromParcelBody(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TimelineRequest[] newArray(int size) {
            return new TimelineRequest[size];
        }
    };
    private static final int REQUEST_TYPE = 8;
    private final int mIntervalMillis;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static TimelineRequest createFromParcelBody(Parcel in) {
        return new TimelineRequest(in);
    }

    public TimelineRequest(int requestId, int option, int intervalMillis) {
        super(8, requestId, option);
        this.mIntervalMillis = intervalMillis;
    }

    TimelineRequest(Parcel source) {
        super(8, source);
        this.mIntervalMillis = source.readInt();
    }

    public int getIntervalMillis() {
        return this.mIntervalMillis;
    }

    @Override // android.media.tv.BroadcastInfoRequest, android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.media.tv.BroadcastInfoRequest, android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.mIntervalMillis);
    }
}
