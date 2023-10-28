package android.net.vcn;

import android.net.TransportInfo;
import android.net.wifi.WifiInfo;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;
/* loaded from: classes2.dex */
public class VcnTransportInfo implements TransportInfo, Parcelable {
    public static final Parcelable.Creator<VcnTransportInfo> CREATOR = new Parcelable.Creator<VcnTransportInfo>() { // from class: android.net.vcn.VcnTransportInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public VcnTransportInfo createFromParcel(Parcel in) {
            int subId = in.readInt();
            WifiInfo wifiInfo = (WifiInfo) in.readParcelable(null, WifiInfo.class);
            if (wifiInfo == null && subId == -1) {
                return null;
            }
            return new VcnTransportInfo(wifiInfo, subId);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public VcnTransportInfo[] newArray(int size) {
            return new VcnTransportInfo[size];
        }
    };
    private final int mSubId;
    private final WifiInfo mWifiInfo;

    public VcnTransportInfo(WifiInfo wifiInfo) {
        this(wifiInfo, -1);
    }

    public VcnTransportInfo(int subId) {
        this(null, subId);
    }

    private VcnTransportInfo(WifiInfo wifiInfo, int subId) {
        this.mWifiInfo = wifiInfo;
        this.mSubId = subId;
    }

    public WifiInfo getWifiInfo() {
        return this.mWifiInfo;
    }

    public int getSubId() {
        return this.mSubId;
    }

    public int hashCode() {
        return Objects.hash(this.mWifiInfo, Integer.valueOf(this.mSubId));
    }

    public boolean equals(Object o) {
        if (o instanceof VcnTransportInfo) {
            VcnTransportInfo that = (VcnTransportInfo) o;
            return Objects.equals(this.mWifiInfo, that.mWifiInfo) && this.mSubId == that.mSubId;
        }
        return false;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public TransportInfo makeCopy(long redactions) {
        if ((4 & redactions) != 0) {
            return new VcnTransportInfo(null, -1);
        }
        WifiInfo wifiInfo = this.mWifiInfo;
        return new VcnTransportInfo(wifiInfo != null ? wifiInfo.makeCopy(redactions) : null, this.mSubId);
    }

    public long getApplicableRedactions() {
        WifiInfo wifiInfo = this.mWifiInfo;
        if (wifiInfo != null) {
            long redactions = 4 | wifiInfo.getApplicableRedactions();
            return redactions;
        }
        return 4L;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mSubId);
        dest.writeParcelable(this.mWifiInfo, flags);
    }

    public String toString() {
        return "VcnTransportInfo { mWifiInfo = " + this.mWifiInfo + ", mSubId = " + this.mSubId + " }";
    }
}
