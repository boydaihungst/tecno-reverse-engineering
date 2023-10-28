package android.net.shared;

import android.net.PrivateDnsConfigParcel;
import android.text.TextUtils;
import com.android.server.slice.SliceClientPermissions;
import java.net.InetAddress;
import java.util.Arrays;
/* loaded from: classes.dex */
public class PrivateDnsConfig {
    public final String hostname;
    public final InetAddress[] ips;
    public final boolean useTls;

    public PrivateDnsConfig() {
        this(false);
    }

    public PrivateDnsConfig(boolean useTls) {
        this.useTls = useTls;
        this.hostname = "";
        this.ips = new InetAddress[0];
    }

    public PrivateDnsConfig(String hostname, InetAddress[] ips) {
        boolean z = !TextUtils.isEmpty(hostname);
        this.useTls = z;
        this.hostname = z ? hostname : "";
        this.ips = ips != null ? ips : new InetAddress[0];
    }

    public PrivateDnsConfig(PrivateDnsConfig cfg) {
        this.useTls = cfg.useTls;
        this.hostname = cfg.hostname;
        this.ips = cfg.ips;
    }

    public boolean inStrictMode() {
        return this.useTls && !TextUtils.isEmpty(this.hostname);
    }

    public String toString() {
        return PrivateDnsConfig.class.getSimpleName() + "{" + this.useTls + ":" + this.hostname + SliceClientPermissions.SliceAuthority.DELIMITER + Arrays.toString(this.ips) + "}";
    }

    public PrivateDnsConfigParcel toParcel() {
        PrivateDnsConfigParcel parcel = new PrivateDnsConfigParcel();
        parcel.hostname = this.hostname;
        parcel.ips = (String[]) ParcelableUtil.toParcelableArray(Arrays.asList(this.ips), new InitialConfiguration$$ExternalSyntheticLambda1(), String.class);
        return parcel;
    }

    public static PrivateDnsConfig fromParcel(PrivateDnsConfigParcel parcel) {
        InetAddress[] ips = new InetAddress[parcel.ips.length];
        return new PrivateDnsConfig(parcel.hostname, (InetAddress[]) ParcelableUtil.fromParcelableArray(parcel.ips, new InitialConfiguration$$ExternalSyntheticLambda0()).toArray(ips));
    }
}
