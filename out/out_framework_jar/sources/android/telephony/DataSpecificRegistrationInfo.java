package android.telephony;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;
@SystemApi
/* loaded from: classes3.dex */
public final class DataSpecificRegistrationInfo implements Parcelable {
    public static final Parcelable.Creator<DataSpecificRegistrationInfo> CREATOR = new Parcelable.Creator<DataSpecificRegistrationInfo>() { // from class: android.telephony.DataSpecificRegistrationInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DataSpecificRegistrationInfo createFromParcel(Parcel source) {
            return new DataSpecificRegistrationInfo(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DataSpecificRegistrationInfo[] newArray(int size) {
            return new DataSpecificRegistrationInfo[size];
        }
    };
    public final boolean isDcNrRestricted;
    public final boolean isEnDcAvailable;
    public final boolean isNrAvailable;
    private final VopsSupportInfo mVopsSupportInfo;
    public final int maxDataCalls;

    public DataSpecificRegistrationInfo(int maxDataCalls, boolean isDcNrRestricted, boolean isNrAvailable, boolean isEnDcAvailable, VopsSupportInfo vops) {
        this.maxDataCalls = maxDataCalls;
        this.isDcNrRestricted = isDcNrRestricted;
        this.isNrAvailable = isNrAvailable;
        this.isEnDcAvailable = isEnDcAvailable;
        this.mVopsSupportInfo = vops;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DataSpecificRegistrationInfo(DataSpecificRegistrationInfo dsri) {
        this.maxDataCalls = dsri.maxDataCalls;
        this.isDcNrRestricted = dsri.isDcNrRestricted;
        this.isNrAvailable = dsri.isNrAvailable;
        this.isEnDcAvailable = dsri.isEnDcAvailable;
        this.mVopsSupportInfo = dsri.mVopsSupportInfo;
    }

    private DataSpecificRegistrationInfo(Parcel source) {
        this.maxDataCalls = source.readInt();
        this.isDcNrRestricted = source.readBoolean();
        this.isNrAvailable = source.readBoolean();
        this.isEnDcAvailable = source.readBoolean();
        this.mVopsSupportInfo = (VopsSupportInfo) source.readParcelable(VopsSupportInfo.class.getClassLoader(), VopsSupportInfo.class);
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.maxDataCalls);
        dest.writeBoolean(this.isDcNrRestricted);
        dest.writeBoolean(this.isNrAvailable);
        dest.writeBoolean(this.isEnDcAvailable);
        dest.writeParcelable(this.mVopsSupportInfo, flags);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return getClass().getName() + " :{" + (" maxDataCalls = " + this.maxDataCalls) + (" isDcNrRestricted = " + this.isDcNrRestricted) + (" isNrAvailable = " + this.isNrAvailable) + (" isEnDcAvailable = " + this.isEnDcAvailable) + (" " + this.mVopsSupportInfo) + " }";
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.maxDataCalls), Boolean.valueOf(this.isDcNrRestricted), Boolean.valueOf(this.isNrAvailable), Boolean.valueOf(this.isEnDcAvailable), this.mVopsSupportInfo);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof DataSpecificRegistrationInfo) {
            DataSpecificRegistrationInfo other = (DataSpecificRegistrationInfo) o;
            return this.maxDataCalls == other.maxDataCalls && this.isDcNrRestricted == other.isDcNrRestricted && this.isNrAvailable == other.isNrAvailable && this.isEnDcAvailable == other.isEnDcAvailable && Objects.equals(this.mVopsSupportInfo, other.mVopsSupportInfo);
        }
        return false;
    }

    @Deprecated
    public LteVopsSupportInfo getLteVopsSupportInfo() {
        VopsSupportInfo vopsSupportInfo = this.mVopsSupportInfo;
        if (vopsSupportInfo instanceof LteVopsSupportInfo) {
            return (LteVopsSupportInfo) vopsSupportInfo;
        }
        return new LteVopsSupportInfo(1, 1);
    }

    public VopsSupportInfo getVopsSupportInfo() {
        return this.mVopsSupportInfo;
    }
}
