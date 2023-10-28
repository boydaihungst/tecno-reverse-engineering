package android.net.networkstack.aidl.quirks;

import java.util.Objects;
/* loaded from: classes.dex */
public final class IPv6ProvisioningLossQuirk {
    public final int mDetectionCount;
    public final long mQuirkExpiry;

    public IPv6ProvisioningLossQuirk(int count, long expiry) {
        this.mDetectionCount = count;
        this.mQuirkExpiry = expiry;
    }

    public IPv6ProvisioningLossQuirkParcelable toStableParcelable() {
        IPv6ProvisioningLossQuirkParcelable p = new IPv6ProvisioningLossQuirkParcelable();
        p.detectionCount = this.mDetectionCount;
        p.quirkExpiry = this.mQuirkExpiry;
        return p;
    }

    public static IPv6ProvisioningLossQuirk fromStableParcelable(IPv6ProvisioningLossQuirkParcelable p) {
        if (p == null) {
            return null;
        }
        return new IPv6ProvisioningLossQuirk(p.detectionCount, p.quirkExpiry);
    }

    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        IPv6ProvisioningLossQuirk other = (IPv6ProvisioningLossQuirk) obj;
        return this.mDetectionCount == other.mDetectionCount && this.mQuirkExpiry == other.mQuirkExpiry;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mDetectionCount), Long.valueOf(this.mQuirkExpiry));
    }

    public String toString() {
        StringBuffer str = new StringBuffer();
        str.append("detection count: ").append(this.mDetectionCount);
        str.append(", quirk expiry: ").append(this.mQuirkExpiry);
        return str.toString();
    }
}
