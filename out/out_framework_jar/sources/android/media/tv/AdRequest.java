package android.media.tv;

import android.os.Bundle;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes2.dex */
public final class AdRequest implements Parcelable {
    public static final Parcelable.Creator<AdRequest> CREATOR = new Parcelable.Creator<AdRequest>() { // from class: android.media.tv.AdRequest.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AdRequest createFromParcel(Parcel source) {
            return new AdRequest(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AdRequest[] newArray(int size) {
            return new AdRequest[size];
        }
    };
    public static final int REQUEST_TYPE_START = 1;
    public static final int REQUEST_TYPE_STOP = 2;
    private final long mEchoInterval;
    private final ParcelFileDescriptor mFileDescriptor;
    private final int mId;
    private final String mMediaFileType;
    private final Bundle mMetadata;
    private final int mRequestType;
    private final long mStartTime;
    private final long mStopTime;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface RequestType {
    }

    public AdRequest(int id, int requestType, ParcelFileDescriptor fileDescriptor, long startTime, long stopTime, long echoInterval, String mediaFileType, Bundle metadata) {
        this.mId = id;
        this.mRequestType = requestType;
        this.mFileDescriptor = fileDescriptor;
        this.mStartTime = startTime;
        this.mStopTime = stopTime;
        this.mEchoInterval = echoInterval;
        this.mMediaFileType = mediaFileType;
        this.mMetadata = metadata;
    }

    private AdRequest(Parcel source) {
        this.mId = source.readInt();
        this.mRequestType = source.readInt();
        if (source.readInt() != 0) {
            this.mFileDescriptor = ParcelFileDescriptor.CREATOR.createFromParcel(source);
        } else {
            this.mFileDescriptor = null;
        }
        this.mStartTime = source.readLong();
        this.mStopTime = source.readLong();
        this.mEchoInterval = source.readLong();
        this.mMediaFileType = source.readString();
        this.mMetadata = source.readBundle();
    }

    public int getId() {
        return this.mId;
    }

    public int getRequestType() {
        return this.mRequestType;
    }

    public ParcelFileDescriptor getFileDescriptor() {
        return this.mFileDescriptor;
    }

    public long getStartTimeMillis() {
        return this.mStartTime;
    }

    public long getStopTimeMillis() {
        return this.mStopTime;
    }

    public long getEchoIntervalMillis() {
        return this.mEchoInterval;
    }

    public String getMediaFileType() {
        return this.mMediaFileType;
    }

    public Bundle getMetadata() {
        return this.mMetadata;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mId);
        dest.writeInt(this.mRequestType);
        if (this.mFileDescriptor != null) {
            dest.writeInt(1);
            this.mFileDescriptor.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        dest.writeLong(this.mStartTime);
        dest.writeLong(this.mStopTime);
        dest.writeLong(this.mEchoInterval);
        dest.writeString(this.mMediaFileType);
        dest.writeBundle(this.mMetadata);
    }
}
