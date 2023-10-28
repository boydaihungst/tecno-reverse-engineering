package com.android.internal.app;

import android.annotation.IntRange;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.util.AnnotationValidations;
/* loaded from: classes4.dex */
public final class MessageSamplingConfig implements Parcelable {
    public static final Parcelable.Creator<MessageSamplingConfig> CREATOR = new Parcelable.Creator<MessageSamplingConfig>() { // from class: com.android.internal.app.MessageSamplingConfig.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public MessageSamplingConfig[] newArray(int size) {
            return new MessageSamplingConfig[size];
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public MessageSamplingConfig createFromParcel(Parcel in) {
            return new MessageSamplingConfig(in);
        }
    };
    private static final int _NUM_OP = 125;
    private final int mAcceptableLeftDistance;
    private final long mExpirationTimeSinceBootMillis;
    private final int mSampledOpCode;

    public MessageSamplingConfig(int sampledOpCode, int acceptableLeftDistance, long expirationTimeSinceBootMillis) {
        this.mSampledOpCode = sampledOpCode;
        AnnotationValidations.validate((Class<IntRange>) IntRange.class, (IntRange) null, sampledOpCode, "from", -1L, "to", 124L);
        this.mAcceptableLeftDistance = acceptableLeftDistance;
        AnnotationValidations.validate((Class<IntRange>) IntRange.class, (IntRange) null, acceptableLeftDistance, "from", 0L, "to", 124L);
        this.mExpirationTimeSinceBootMillis = expirationTimeSinceBootMillis;
        AnnotationValidations.validate(IntRange.class, (IntRange) null, expirationTimeSinceBootMillis, "from", 0L);
    }

    public int getSampledOpCode() {
        return this.mSampledOpCode;
    }

    public int getAcceptableLeftDistance() {
        return this.mAcceptableLeftDistance;
    }

    public long getExpirationTimeSinceBootMillis() {
        return this.mExpirationTimeSinceBootMillis;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mSampledOpCode);
        dest.writeInt(this.mAcceptableLeftDistance);
        dest.writeLong(this.mExpirationTimeSinceBootMillis);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    MessageSamplingConfig(Parcel in) {
        int sampledOpCode = in.readInt();
        int acceptableLeftDistance = in.readInt();
        long expirationTimeSinceBootMillis = in.readLong();
        this.mSampledOpCode = sampledOpCode;
        AnnotationValidations.validate((Class<IntRange>) IntRange.class, (IntRange) null, sampledOpCode, "from", -1L, "to", 124L);
        this.mAcceptableLeftDistance = acceptableLeftDistance;
        AnnotationValidations.validate((Class<IntRange>) IntRange.class, (IntRange) null, acceptableLeftDistance, "from", 0L, "to", 124L);
        this.mExpirationTimeSinceBootMillis = expirationTimeSinceBootMillis;
        AnnotationValidations.validate(IntRange.class, (IntRange) null, expirationTimeSinceBootMillis, "from", 0L);
    }

    @Deprecated
    private void __metadata() {
    }
}
