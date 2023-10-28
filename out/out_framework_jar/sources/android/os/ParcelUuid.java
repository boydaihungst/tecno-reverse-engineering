package android.os;

import android.os.Parcelable;
import java.util.UUID;
/* loaded from: classes2.dex */
public final class ParcelUuid implements Parcelable {
    public static final Parcelable.Creator<ParcelUuid> CREATOR = new Parcelable.Creator<ParcelUuid>() { // from class: android.os.ParcelUuid.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ParcelUuid createFromParcel(Parcel source) {
            long mostSigBits = source.readLong();
            long leastSigBits = source.readLong();
            UUID uuid = new UUID(mostSigBits, leastSigBits);
            return new ParcelUuid(uuid);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ParcelUuid[] newArray(int size) {
            return new ParcelUuid[size];
        }
    };
    private final UUID mUuid;

    public ParcelUuid(UUID uuid) {
        this.mUuid = uuid;
    }

    public static ParcelUuid fromString(String uuid) {
        return new ParcelUuid(UUID.fromString(uuid));
    }

    public UUID getUuid() {
        return this.mUuid;
    }

    public String toString() {
        return this.mUuid.toString();
    }

    public int hashCode() {
        return this.mUuid.hashCode();
    }

    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (this == object) {
            return true;
        }
        if (!(object instanceof ParcelUuid)) {
            return false;
        }
        ParcelUuid that = (ParcelUuid) object;
        return this.mUuid.equals(that.mUuid);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.mUuid.getMostSignificantBits());
        dest.writeLong(this.mUuid.getLeastSignificantBits());
    }
}
