package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;
/* loaded from: classes3.dex */
public class CellConfigLte implements Parcelable {
    public static final Parcelable.Creator<CellConfigLte> CREATOR = new Parcelable.Creator<CellConfigLte>() { // from class: android.telephony.CellConfigLte.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public CellConfigLte createFromParcel(Parcel in) {
            return new CellConfigLte(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public CellConfigLte[] newArray(int size) {
            return new CellConfigLte[0];
        }
    };
    private final boolean mIsEndcAvailable;

    public CellConfigLte() {
        this.mIsEndcAvailable = false;
    }

    public CellConfigLte(boolean isEndcAvailable) {
        this.mIsEndcAvailable = isEndcAvailable;
    }

    public CellConfigLte(CellConfigLte config) {
        this.mIsEndcAvailable = config.mIsEndcAvailable;
    }

    boolean isEndcAvailable() {
        return this.mIsEndcAvailable;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public int hashCode() {
        return Objects.hash(Boolean.valueOf(this.mIsEndcAvailable));
    }

    public boolean equals(Object other) {
        if (other instanceof CellConfigLte) {
            CellConfigLte o = (CellConfigLte) other;
            return this.mIsEndcAvailable == o.mIsEndcAvailable;
        }
        return false;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBoolean(this.mIsEndcAvailable);
    }

    public String toString() {
        return getClass().getName() + " :{" + (" isEndcAvailable = " + this.mIsEndcAvailable) + " }";
    }

    private CellConfigLte(Parcel in) {
        this.mIsEndcAvailable = in.readBoolean();
    }
}
