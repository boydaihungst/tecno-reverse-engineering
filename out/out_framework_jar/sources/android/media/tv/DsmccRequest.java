package android.media.tv;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes2.dex */
public final class DsmccRequest extends BroadcastInfoRequest implements Parcelable {
    public static final Parcelable.Creator<DsmccRequest> CREATOR = new Parcelable.Creator<DsmccRequest>() { // from class: android.media.tv.DsmccRequest.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DsmccRequest createFromParcel(Parcel source) {
            source.readInt();
            return DsmccRequest.createFromParcelBody(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DsmccRequest[] newArray(int size) {
            return new DsmccRequest[size];
        }
    };
    private static final int REQUEST_TYPE = 6;
    private final Uri mUri;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static DsmccRequest createFromParcelBody(Parcel in) {
        return new DsmccRequest(in);
    }

    public DsmccRequest(int requestId, int option, Uri uri) {
        super(6, requestId, option);
        this.mUri = uri;
    }

    DsmccRequest(Parcel source) {
        super(6, source);
        String uriString = source.readString();
        this.mUri = uriString == null ? null : Uri.parse(uriString);
    }

    public Uri getUri() {
        return this.mUri;
    }

    @Override // android.media.tv.BroadcastInfoRequest, android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.media.tv.BroadcastInfoRequest, android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        Uri uri = this.mUri;
        String uriString = uri == null ? null : uri.toString();
        dest.writeString(uriString);
    }
}
