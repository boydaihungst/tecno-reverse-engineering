package android.media.tv;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes2.dex */
public final class TableResponse extends BroadcastInfoResponse implements Parcelable {
    public static final Parcelable.Creator<TableResponse> CREATOR = new Parcelable.Creator<TableResponse>() { // from class: android.media.tv.TableResponse.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TableResponse createFromParcel(Parcel source) {
            source.readInt();
            return TableResponse.createFromParcelBody(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TableResponse[] newArray(int size) {
            return new TableResponse[size];
        }
    };
    private static final int RESPONSE_TYPE = 2;
    private final int mSize;
    private final Uri mTableUri;
    private final int mVersion;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static TableResponse createFromParcelBody(Parcel in) {
        return new TableResponse(in);
    }

    public TableResponse(int requestId, int sequence, int responseResult, Uri tableUri, int version, int size) {
        super(2, requestId, sequence, responseResult);
        this.mTableUri = tableUri;
        this.mVersion = version;
        this.mSize = size;
    }

    TableResponse(Parcel source) {
        super(2, source);
        String uriString = source.readString();
        this.mTableUri = uriString == null ? null : Uri.parse(uriString);
        this.mVersion = source.readInt();
        this.mSize = source.readInt();
    }

    public Uri getTableUri() {
        return this.mTableUri;
    }

    public int getVersion() {
        return this.mVersion;
    }

    public int getSize() {
        return this.mSize;
    }

    @Override // android.media.tv.BroadcastInfoResponse, android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.media.tv.BroadcastInfoResponse, android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        Uri uri = this.mTableUri;
        String uriString = uri == null ? null : uri.toString();
        dest.writeString(uriString);
        dest.writeInt(this.mVersion);
        dest.writeInt(this.mSize);
    }
}
