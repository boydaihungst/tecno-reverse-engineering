package com.android.server.stats.pull.netstats;

import android.net.NetworkStats;
import java.util.Arrays;
import java.util.Objects;
/* loaded from: classes2.dex */
public class NetworkStatsExt {
    public final int oemManaged;
    public final int ratType;
    public final boolean slicedByFgbg;
    public final boolean slicedByMetered;
    public final boolean slicedByTag;
    public final NetworkStats stats;
    public final SubInfo subInfo;
    public final int[] transports;

    public NetworkStatsExt(NetworkStats stats, int[] transports, boolean slicedByFgbg) {
        this(stats, transports, slicedByFgbg, false, false, 0, null, -1);
    }

    public NetworkStatsExt(NetworkStats stats, int[] transports, boolean slicedByFgbg, boolean slicedByTag, boolean slicedByMetered, int ratType, SubInfo subInfo, int oemManaged) {
        this.stats = stats;
        int[] copyOf = Arrays.copyOf(transports, transports.length);
        this.transports = copyOf;
        Arrays.sort(copyOf);
        this.slicedByFgbg = slicedByFgbg;
        this.slicedByTag = slicedByTag;
        this.slicedByMetered = slicedByMetered;
        this.ratType = ratType;
        this.subInfo = subInfo;
        this.oemManaged = oemManaged;
    }

    public boolean hasSameSlicing(NetworkStatsExt other) {
        return Arrays.equals(this.transports, other.transports) && this.slicedByFgbg == other.slicedByFgbg && this.slicedByTag == other.slicedByTag && this.slicedByMetered == other.slicedByMetered && this.ratType == other.ratType && Objects.equals(this.subInfo, other.subInfo) && this.oemManaged == other.oemManaged;
    }
}
