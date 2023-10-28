package android.net;

import android.os.Parcel;
import android.os.Parcelable;
@Deprecated
/* loaded from: classes2.dex */
public class NetworkQuotaInfo implements Parcelable {
    public static final Parcelable.Creator<NetworkQuotaInfo> CREATOR = new Parcelable.Creator<NetworkQuotaInfo>() { // from class: android.net.NetworkQuotaInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public NetworkQuotaInfo createFromParcel(Parcel in) {
            return new NetworkQuotaInfo(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public NetworkQuotaInfo[] newArray(int size) {
            return new NetworkQuotaInfo[size];
        }
    };
    public static final long NO_LIMIT = -1;

    public NetworkQuotaInfo() {
    }

    public NetworkQuotaInfo(Parcel in) {
    }

    public long getEstimatedBytes() {
        return 0L;
    }

    public long getSoftLimitBytes() {
        return -1L;
    }

    public long getHardLimitBytes() {
        return -1L;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel out, int flags) {
    }
}
