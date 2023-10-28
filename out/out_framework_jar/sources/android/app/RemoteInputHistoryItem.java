package android.app;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes.dex */
public class RemoteInputHistoryItem implements Parcelable {
    public static final Parcelable.Creator<RemoteInputHistoryItem> CREATOR = new Parcelable.Creator<RemoteInputHistoryItem>() { // from class: android.app.RemoteInputHistoryItem.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public RemoteInputHistoryItem createFromParcel(Parcel in) {
            return new RemoteInputHistoryItem(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public RemoteInputHistoryItem[] newArray(int size) {
            return new RemoteInputHistoryItem[size];
        }
    };
    private String mMimeType;
    private CharSequence mText;
    private Uri mUri;

    public RemoteInputHistoryItem(String mimeType, Uri uri, CharSequence backupText) {
        this.mMimeType = mimeType;
        this.mUri = uri;
        this.mText = Notification.safeCharSequence(backupText);
    }

    public RemoteInputHistoryItem(CharSequence text) {
        this.mText = Notification.safeCharSequence(text);
    }

    protected RemoteInputHistoryItem(Parcel in) {
        this.mText = in.readCharSequence();
        this.mMimeType = in.readStringNoHelper();
        this.mUri = (Uri) in.readParcelable(Uri.class.getClassLoader(), Uri.class);
    }

    public CharSequence getText() {
        return this.mText;
    }

    public String getMimeType() {
        return this.mMimeType;
    }

    public Uri getUri() {
        return this.mUri;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeCharSequence(this.mText);
        dest.writeStringNoHelper(this.mMimeType);
        dest.writeParcelable(this.mUri, flags);
    }
}
