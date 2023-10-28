package com.transsion.hubcore.server.connectivity;

import android.net.NetworkCapabilities;
import android.net.UidRangeParcel;
import com.android.internal.net.VpnConfig;
import com.transsion.hubcore.server.connectivity.ITranVpn;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.Set;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranVpn {
    public static final TranClassInfo<ITranVpn> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.connectivity.TranVpnImpl", ITranVpn.class, new Supplier() { // from class: com.transsion.hubcore.server.connectivity.ITranVpn$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranVpn.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranVpn {
    }

    static ITranVpn Instance() {
        return (ITranVpn) classInfo.getImpl();
    }

    default NetworkCapabilities setVpnForcedLocked(boolean enforce, Set<UidRangeParcel> rangesToTellNetdToAdd, Set<UidRangeParcel> rangesToTellNetdToRemove, Set<UidRangeParcel> blockedUidsAsToldToNetd, NetworkCapabilities capabilities) {
        return capabilities;
    }

    default NetworkCapabilities agentConnect(NetworkCapabilities capabilities, boolean lockdown) {
        return capabilities;
    }

    default void hookVpnChanged(String packageName) {
    }

    default void hookVpnChangedIfNeed(VpnConfig config) {
    }
}
