package android.app.backup;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
@SystemApi
/* loaded from: classes.dex */
public class RestoreSet implements Parcelable {
    public static final Parcelable.Creator<RestoreSet> CREATOR = new Parcelable.Creator<RestoreSet>() { // from class: android.app.backup.RestoreSet.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public RestoreSet createFromParcel(Parcel in) {
            return new RestoreSet(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public RestoreSet[] newArray(int size) {
            return new RestoreSet[size];
        }
    };
    public final int backupTransportFlags;
    public String device;
    public String name;
    public long token;

    public RestoreSet() {
        this.backupTransportFlags = 0;
    }

    public RestoreSet(String name, String device, long token) {
        this(name, device, token, 0);
    }

    public RestoreSet(String name, String device, long token, int backupTransportFlags) {
        this.name = name;
        this.device = device;
        this.token = token;
        this.backupTransportFlags = backupTransportFlags;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.name);
        out.writeString(this.device);
        out.writeLong(this.token);
        out.writeInt(this.backupTransportFlags);
    }

    private RestoreSet(Parcel in) {
        this.name = in.readString();
        this.device = in.readString();
        this.token = in.readLong();
        this.backupTransportFlags = in.readInt();
    }
}
