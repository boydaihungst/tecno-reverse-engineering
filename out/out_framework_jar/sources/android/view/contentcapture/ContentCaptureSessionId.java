package android.view.contentcapture;

import android.os.Parcel;
import android.os.Parcelable;
import java.io.PrintWriter;
/* loaded from: classes3.dex */
public final class ContentCaptureSessionId implements Parcelable {
    public static final Parcelable.Creator<ContentCaptureSessionId> CREATOR = new Parcelable.Creator<ContentCaptureSessionId>() { // from class: android.view.contentcapture.ContentCaptureSessionId.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ContentCaptureSessionId createFromParcel(Parcel parcel) {
            return new ContentCaptureSessionId(parcel.readInt());
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ContentCaptureSessionId[] newArray(int size) {
            return new ContentCaptureSessionId[size];
        }
    };
    private final int mValue;

    public ContentCaptureSessionId(int value) {
        this.mValue = value;
    }

    public int getValue() {
        return this.mValue;
    }

    public int hashCode() {
        int result = (1 * 31) + this.mValue;
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ContentCaptureSessionId other = (ContentCaptureSessionId) obj;
        if (this.mValue == other.mValue) {
            return true;
        }
        return false;
    }

    public String toString() {
        return Integer.toString(this.mValue);
    }

    public void dump(PrintWriter pw) {
        pw.print(this.mValue);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(this.mValue);
    }
}
