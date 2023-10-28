package android.telecom;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
/* loaded from: classes3.dex */
public class ParcelableRttCall implements Parcelable {
    public static final Parcelable.Creator<ParcelableRttCall> CREATOR = new Parcelable.Creator<ParcelableRttCall>() { // from class: android.telecom.ParcelableRttCall.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ParcelableRttCall createFromParcel(Parcel in) {
            return new ParcelableRttCall(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ParcelableRttCall[] newArray(int size) {
            return new ParcelableRttCall[size];
        }
    };
    private final ParcelFileDescriptor mReceiveStream;
    private final int mRttMode;
    private final ParcelFileDescriptor mTransmitStream;

    public ParcelableRttCall(int rttMode, ParcelFileDescriptor transmitStream, ParcelFileDescriptor receiveStream) {
        this.mRttMode = rttMode;
        this.mTransmitStream = transmitStream;
        this.mReceiveStream = receiveStream;
    }

    protected ParcelableRttCall(Parcel in) {
        this.mRttMode = in.readInt();
        this.mTransmitStream = (ParcelFileDescriptor) in.readParcelable(ParcelFileDescriptor.class.getClassLoader(), ParcelFileDescriptor.class);
        this.mReceiveStream = (ParcelFileDescriptor) in.readParcelable(ParcelFileDescriptor.class.getClassLoader(), ParcelFileDescriptor.class);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mRttMode);
        dest.writeParcelable(this.mTransmitStream, flags);
        dest.writeParcelable(this.mReceiveStream, flags);
    }

    public int getRttMode() {
        return this.mRttMode;
    }

    public ParcelFileDescriptor getReceiveStream() {
        return this.mReceiveStream;
    }

    public ParcelFileDescriptor getTransmitStream() {
        return this.mTransmitStream;
    }
}
