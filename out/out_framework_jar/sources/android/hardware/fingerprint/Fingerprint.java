package android.hardware.fingerprint;

import android.hardware.biometrics.BiometricAuthenticator;
import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes.dex */
public final class Fingerprint extends BiometricAuthenticator.Identifier {
    public static final Parcelable.Creator<Fingerprint> CREATOR = new Parcelable.Creator<Fingerprint>() { // from class: android.hardware.fingerprint.Fingerprint.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public Fingerprint createFromParcel(Parcel in) {
            return new Fingerprint(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public Fingerprint[] newArray(int size) {
            return new Fingerprint[size];
        }
    };
    private CharSequence mAppPkgName;
    private int mGroupId;
    private int mUserId;

    public Fingerprint(CharSequence name, int groupId, int fingerId, long deviceId) {
        super(name, fingerId, deviceId);
        this.mGroupId = groupId;
        this.mAppPkgName = "";
        this.mUserId = 0;
    }

    public Fingerprint(CharSequence name, int groupId, int fingerId, long deviceId, CharSequence apppkgname, int userId) {
        super(name, fingerId, deviceId, apppkgname, userId);
        this.mGroupId = groupId;
        if (apppkgname == null) {
            this.mAppPkgName = "";
        } else {
            this.mAppPkgName = apppkgname;
        }
        this.mUserId = userId;
    }

    public Fingerprint(CharSequence name, int fingerId, long deviceId) {
        super(name, fingerId, deviceId);
    }

    private Fingerprint(Parcel in) {
        super(in.readString(), in.readInt(), in.readLong());
        this.mGroupId = in.readInt();
        this.mAppPkgName = in.readString();
        this.mUserId = in.readInt();
    }

    public int getGroupId() {
        return this.mGroupId;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(getName().toString());
        out.writeInt(getBiometricId());
        out.writeLong(getDeviceId());
        out.writeInt(this.mGroupId);
        if (this.mAppPkgName == null) {
            this.mAppPkgName = "";
        }
        out.writeString(this.mAppPkgName.toString());
        out.writeInt(getSubUserId());
    }
}
