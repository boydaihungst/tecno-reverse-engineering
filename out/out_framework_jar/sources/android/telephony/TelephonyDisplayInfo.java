package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.security.keystore.KeyProperties;
import com.android.internal.telephony.DctConstants;
import java.util.Objects;
/* loaded from: classes3.dex */
public final class TelephonyDisplayInfo implements Parcelable {
    public static final Parcelable.Creator<TelephonyDisplayInfo> CREATOR = new Parcelable.Creator<TelephonyDisplayInfo>() { // from class: android.telephony.TelephonyDisplayInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TelephonyDisplayInfo createFromParcel(Parcel source) {
            return new TelephonyDisplayInfo(source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TelephonyDisplayInfo[] newArray(int size) {
            return new TelephonyDisplayInfo[size];
        }
    };
    public static final int OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO = 2;
    public static final int OVERRIDE_NETWORK_TYPE_LTE_CA = 1;
    public static final int OVERRIDE_NETWORK_TYPE_NONE = 0;
    public static final int OVERRIDE_NETWORK_TYPE_NR_ADVANCED = 5;
    public static final int OVERRIDE_NETWORK_TYPE_NR_NSA = 3;
    @Deprecated
    public static final int OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE = 4;
    private final int mNetworkType;
    private final int mOverrideNetworkType;

    public TelephonyDisplayInfo(int networkType, int overrideNetworkType) {
        this.mNetworkType = networkType;
        this.mOverrideNetworkType = overrideNetworkType;
    }

    public TelephonyDisplayInfo(Parcel p) {
        this.mNetworkType = p.readInt();
        this.mOverrideNetworkType = p.readInt();
    }

    public int getNetworkType() {
        return this.mNetworkType;
    }

    public int getOverrideNetworkType() {
        return this.mOverrideNetworkType;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mNetworkType);
        dest.writeInt(this.mOverrideNetworkType);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TelephonyDisplayInfo that = (TelephonyDisplayInfo) o;
        if (this.mNetworkType == that.mNetworkType && this.mOverrideNetworkType == that.mOverrideNetworkType) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mNetworkType), Integer.valueOf(this.mOverrideNetworkType));
    }

    public static String overrideNetworkTypeToString(int type) {
        switch (type) {
            case 0:
                return KeyProperties.DIGEST_NONE;
            case 1:
                return "LTE_CA";
            case 2:
                return "LTE_ADV_PRO";
            case 3:
                return DctConstants.RAT_NAME_NR_NSA;
            case 4:
                return DctConstants.RAT_NAME_NR_NSA_MMWAVE;
            case 5:
                return "NR_ADVANCED";
            default:
                return "UNKNOWN";
        }
    }

    public String toString() {
        return "TelephonyDisplayInfo {network=" + TelephonyManager.getNetworkTypeName(this.mNetworkType) + ", override=" + overrideNetworkTypeToString(this.mOverrideNetworkType) + "}";
    }
}
