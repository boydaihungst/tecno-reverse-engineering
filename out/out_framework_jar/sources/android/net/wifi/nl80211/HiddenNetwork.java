package android.net.wifi.nl80211;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;
/* loaded from: classes2.dex */
public class HiddenNetwork implements Parcelable {
    public static final Parcelable.Creator<HiddenNetwork> CREATOR = new Parcelable.Creator<HiddenNetwork>() { // from class: android.net.wifi.nl80211.HiddenNetwork.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public HiddenNetwork createFromParcel(Parcel in) {
            HiddenNetwork result = new HiddenNetwork();
            result.ssid = in.createByteArray();
            return result;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public HiddenNetwork[] newArray(int size) {
            return new HiddenNetwork[size];
        }
    };
    private static final String TAG = "HiddenNetwork";
    public byte[] ssid;

    public boolean equals(Object rhs) {
        if (this == rhs) {
            return true;
        }
        if (!(rhs instanceof HiddenNetwork)) {
            return false;
        }
        HiddenNetwork network = (HiddenNetwork) rhs;
        return Arrays.equals(this.ssid, network.ssid);
    }

    public int hashCode() {
        return Arrays.hashCode(this.ssid);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel out, int flags) {
        out.writeByteArray(this.ssid);
    }
}
