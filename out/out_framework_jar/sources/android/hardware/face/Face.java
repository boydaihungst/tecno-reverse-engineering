package android.hardware.face;

import android.hardware.biometrics.BiometricAuthenticator;
import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes.dex */
public final class Face extends BiometricAuthenticator.Identifier {
    public static final Parcelable.Creator<Face> CREATOR = new Parcelable.Creator<Face>() { // from class: android.hardware.face.Face.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public Face createFromParcel(Parcel in) {
            return new Face(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public Face[] newArray(int size) {
            return new Face[size];
        }
    };

    public Face(CharSequence name, int faceId, long deviceId) {
        super(name, faceId, deviceId);
    }

    private Face(Parcel in) {
        super(in.readString(), in.readInt(), in.readLong());
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
    }
}
