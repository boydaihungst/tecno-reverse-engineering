package android.app.timezone;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.TimeZoneRulesDataContract;
import android.text.format.DateFormat;
/* loaded from: classes.dex */
public final class DistroRulesVersion implements Parcelable {
    public static final Parcelable.Creator<DistroRulesVersion> CREATOR = new Parcelable.Creator<DistroRulesVersion>() { // from class: android.app.timezone.DistroRulesVersion.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DistroRulesVersion createFromParcel(Parcel in) {
            String rulesVersion = in.readString();
            int revision = in.readInt();
            return new DistroRulesVersion(rulesVersion, revision);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DistroRulesVersion[] newArray(int size) {
            return new DistroRulesVersion[size];
        }
    };
    private final int mRevision;
    private final String mRulesVersion;

    public DistroRulesVersion(String rulesVersion, int revision) {
        this.mRulesVersion = Utils.validateRulesVersion("rulesVersion", rulesVersion);
        this.mRevision = Utils.validateVersion(TimeZoneRulesDataContract.Operation.COLUMN_REVISION, revision);
    }

    public String getRulesVersion() {
        return this.mRulesVersion;
    }

    public int getRevision() {
        return this.mRevision;
    }

    public boolean isOlderThan(DistroRulesVersion distroRulesVersion) {
        int rulesComparison = this.mRulesVersion.compareTo(distroRulesVersion.mRulesVersion);
        if (rulesComparison < 0) {
            return true;
        }
        if (rulesComparison <= 0 && this.mRevision < distroRulesVersion.mRevision) {
            return true;
        }
        return false;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.mRulesVersion);
        out.writeInt(this.mRevision);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DistroRulesVersion that = (DistroRulesVersion) o;
        if (this.mRevision != that.mRevision) {
            return false;
        }
        return this.mRulesVersion.equals(that.mRulesVersion);
    }

    public int hashCode() {
        int result = this.mRulesVersion.hashCode();
        return (result * 31) + this.mRevision;
    }

    public String toString() {
        return "DistroRulesVersion{mRulesVersion='" + this.mRulesVersion + DateFormat.QUOTE + ", mRevision='" + this.mRevision + DateFormat.QUOTE + '}';
    }

    public String toDumpString() {
        return this.mRulesVersion + "," + this.mRevision;
    }
}
