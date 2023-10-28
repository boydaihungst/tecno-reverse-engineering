package android.nfc;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
/* loaded from: classes2.dex */
public final class BeamShareData implements Parcelable {
    public static final Parcelable.Creator<BeamShareData> CREATOR = new Parcelable.Creator<BeamShareData>() { // from class: android.nfc.BeamShareData.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public BeamShareData createFromParcel(Parcel source) {
            Uri[] uris = null;
            NdefMessage msg = (NdefMessage) source.readParcelable(NdefMessage.class.getClassLoader(), NdefMessage.class);
            int numUris = source.readInt();
            if (numUris > 0) {
                uris = new Uri[numUris];
                source.readTypedArray(uris, Uri.CREATOR);
            }
            UserHandle userHandle = (UserHandle) source.readParcelable(UserHandle.class.getClassLoader(), UserHandle.class);
            int flags = source.readInt();
            return new BeamShareData(msg, uris, userHandle, flags);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public BeamShareData[] newArray(int size) {
            return new BeamShareData[size];
        }
    };
    public final int flags;
    public final NdefMessage ndefMessage;
    public final Uri[] uris;
    public final UserHandle userHandle;

    public BeamShareData(NdefMessage msg, Uri[] uris, UserHandle userHandle, int flags) {
        this.ndefMessage = msg;
        this.uris = uris;
        this.userHandle = userHandle;
        this.flags = flags;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        Uri[] uriArr = this.uris;
        int urisLength = uriArr != null ? uriArr.length : 0;
        dest.writeParcelable(this.ndefMessage, 0);
        dest.writeInt(urisLength);
        if (urisLength > 0) {
            dest.writeTypedArray(this.uris, 0);
        }
        dest.writeParcelable(this.userHandle, 0);
        dest.writeInt(this.flags);
    }
}
