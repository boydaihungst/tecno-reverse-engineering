package android.security.keymaster;

import android.content.pm.Signature;
import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes3.dex */
public class KeyAttestationPackageInfo implements Parcelable {
    public static final Parcelable.Creator<KeyAttestationPackageInfo> CREATOR = new Parcelable.Creator<KeyAttestationPackageInfo>() { // from class: android.security.keymaster.KeyAttestationPackageInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public KeyAttestationPackageInfo createFromParcel(Parcel source) {
            return new KeyAttestationPackageInfo(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public KeyAttestationPackageInfo[] newArray(int size) {
            return new KeyAttestationPackageInfo[size];
        }
    };
    private final String mPackageName;
    private final Signature[] mPackageSignatures;
    private final long mPackageVersionCode;

    public KeyAttestationPackageInfo(String mPackageName, long mPackageVersionCode, Signature[] mPackageSignatures) {
        this.mPackageName = mPackageName;
        this.mPackageVersionCode = mPackageVersionCode;
        this.mPackageSignatures = mPackageSignatures;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public long getPackageVersionCode() {
        return this.mPackageVersionCode;
    }

    public Signature[] getPackageSignatures() {
        return this.mPackageSignatures;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mPackageName);
        dest.writeLong(this.mPackageVersionCode);
        dest.writeTypedArray(this.mPackageSignatures, flags);
    }

    private KeyAttestationPackageInfo(Parcel source) {
        this.mPackageName = source.readString();
        this.mPackageVersionCode = source.readLong();
        this.mPackageSignatures = (Signature[]) source.createTypedArray(Signature.CREATOR);
    }
}
