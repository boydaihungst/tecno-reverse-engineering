package android.net.lowpan;

import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;
/* loaded from: classes2.dex */
public class LowpanChannelInfo implements Parcelable {
    public static final Parcelable.Creator<LowpanChannelInfo> CREATOR = new Parcelable.Creator<LowpanChannelInfo>() { // from class: android.net.lowpan.LowpanChannelInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public LowpanChannelInfo createFromParcel(Parcel in) {
            LowpanChannelInfo info = new LowpanChannelInfo();
            info.mIndex = in.readInt();
            info.mName = in.readString();
            info.mSpectrumCenterFrequency = in.readFloat();
            info.mSpectrumBandwidth = in.readFloat();
            info.mMaxTransmitPower = in.readInt();
            info.mIsMaskedByRegulatoryDomain = in.readBoolean();
            return info;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public LowpanChannelInfo[] newArray(int size) {
            return new LowpanChannelInfo[size];
        }
    };
    public static final float UNKNOWN_BANDWIDTH = 0.0f;
    public static final float UNKNOWN_FREQUENCY = 0.0f;
    public static final int UNKNOWN_POWER = Integer.MAX_VALUE;
    private int mIndex;
    private boolean mIsMaskedByRegulatoryDomain;
    private int mMaxTransmitPower;
    private String mName;
    private float mSpectrumBandwidth;
    private float mSpectrumCenterFrequency;

    public static LowpanChannelInfo getChannelInfoForIeee802154Page0(int index) {
        LowpanChannelInfo info = new LowpanChannelInfo();
        if (index < 0) {
            info = null;
        } else if (index == 0) {
            info.mSpectrumCenterFrequency = 8.6830003E8f;
            info.mSpectrumBandwidth = 600000.0f;
        } else if (index < 11) {
            info.mSpectrumCenterFrequency = (index * 2000000.0f) + 9.04E8f;
            info.mSpectrumBandwidth = 0.0f;
        } else if (index < 26) {
            info.mSpectrumCenterFrequency = (index * 5000000.0f) + 2.34999987E9f;
            info.mSpectrumBandwidth = 2000000.0f;
        } else {
            info = null;
        }
        info.mName = Integer.toString(index);
        return info;
    }

    private LowpanChannelInfo() {
        this.mIndex = 0;
        this.mName = null;
        this.mSpectrumCenterFrequency = 0.0f;
        this.mSpectrumBandwidth = 0.0f;
        this.mMaxTransmitPower = Integer.MAX_VALUE;
        this.mIsMaskedByRegulatoryDomain = false;
    }

    private LowpanChannelInfo(int index, String name, float cf, float bw) {
        this.mIndex = 0;
        this.mName = null;
        this.mSpectrumCenterFrequency = 0.0f;
        this.mSpectrumBandwidth = 0.0f;
        this.mMaxTransmitPower = Integer.MAX_VALUE;
        this.mIsMaskedByRegulatoryDomain = false;
        this.mIndex = index;
        this.mName = name;
        this.mSpectrumCenterFrequency = cf;
        this.mSpectrumBandwidth = bw;
    }

    public String getName() {
        return this.mName;
    }

    public int getIndex() {
        return this.mIndex;
    }

    public int getMaxTransmitPower() {
        return this.mMaxTransmitPower;
    }

    public boolean isMaskedByRegulatoryDomain() {
        return this.mIsMaskedByRegulatoryDomain;
    }

    public float getSpectrumCenterFrequency() {
        return this.mSpectrumCenterFrequency;
    }

    public float getSpectrumBandwidth() {
        return this.mSpectrumBandwidth;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Channel ").append(this.mIndex);
        String str = this.mName;
        if (str != null && !str.equals(Integer.toString(this.mIndex))) {
            sb.append(" (").append(this.mName).append(NavigationBarInflaterView.KEY_CODE_END);
        }
        float f = this.mSpectrumCenterFrequency;
        if (f > 0.0f) {
            if (f > 1.0E9f) {
                sb.append(", SpectrumCenterFrequency: ").append(this.mSpectrumCenterFrequency / 1.0E9f).append("GHz");
            } else if (f > 1000000.0f) {
                sb.append(", SpectrumCenterFrequency: ").append(this.mSpectrumCenterFrequency / 1000000.0f).append("MHz");
            } else {
                sb.append(", SpectrumCenterFrequency: ").append(this.mSpectrumCenterFrequency / 1000.0f).append("kHz");
            }
        }
        float f2 = this.mSpectrumBandwidth;
        if (f2 > 0.0f) {
            if (f2 > 1.0E9f) {
                sb.append(", SpectrumBandwidth: ").append(this.mSpectrumBandwidth / 1.0E9f).append("GHz");
            } else if (f2 > 1000000.0f) {
                sb.append(", SpectrumBandwidth: ").append(this.mSpectrumBandwidth / 1000000.0f).append("MHz");
            } else {
                sb.append(", SpectrumBandwidth: ").append(this.mSpectrumBandwidth / 1000.0f).append("kHz");
            }
        }
        if (this.mMaxTransmitPower != Integer.MAX_VALUE) {
            sb.append(", MaxTransmitPower: ").append(this.mMaxTransmitPower).append("dBm");
        }
        return sb.toString();
    }

    public boolean equals(Object obj) {
        if (obj instanceof LowpanChannelInfo) {
            LowpanChannelInfo rhs = (LowpanChannelInfo) obj;
            return Objects.equals(this.mName, rhs.mName) && this.mIndex == rhs.mIndex && this.mIsMaskedByRegulatoryDomain == rhs.mIsMaskedByRegulatoryDomain && this.mSpectrumCenterFrequency == rhs.mSpectrumCenterFrequency && this.mSpectrumBandwidth == rhs.mSpectrumBandwidth && this.mMaxTransmitPower == rhs.mMaxTransmitPower;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.mName, Integer.valueOf(this.mIndex), Boolean.valueOf(this.mIsMaskedByRegulatoryDomain), Float.valueOf(this.mSpectrumCenterFrequency), Float.valueOf(this.mSpectrumBandwidth), Integer.valueOf(this.mMaxTransmitPower));
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mIndex);
        dest.writeString(this.mName);
        dest.writeFloat(this.mSpectrumCenterFrequency);
        dest.writeFloat(this.mSpectrumBandwidth);
        dest.writeInt(this.mMaxTransmitPower);
        dest.writeBoolean(this.mIsMaskedByRegulatoryDomain);
    }
}
