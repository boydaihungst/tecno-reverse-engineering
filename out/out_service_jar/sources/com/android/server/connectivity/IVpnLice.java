package com.android.server.connectivity;

import android.net.NetworkCapabilities;
import android.net.UidRangeParcel;
import com.android.server.connectivity.IVpnLice;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.Set;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_SERVICES)
/* loaded from: classes.dex */
public interface IVpnLice {
    public static final LiceInfo<IVpnLice> sLiceInfo = new LiceInfo<>("com.transsion.server.connectivity.VpnLice", IVpnLice.class, new Supplier() { // from class: com.android.server.connectivity.IVpnLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new IVpnLice.DefaultImpl();
        }
    });

    /* loaded from: classes.dex */
    public static class DefaultImpl implements IVpnLice {
    }

    static IVpnLice Instance() {
        return (IVpnLice) sLiceInfo.getImpl();
    }

    default NetworkCapabilities setVpnForcedLocked(boolean enforce, Set<UidRangeParcel> rangesToTellNetdToAdd, Set<UidRangeParcel> rangesToTellNetdToRemove, Set<UidRangeParcel> blockedUidsAsToldToNetd, NetworkCapabilities capabilities) {
        return capabilities;
    }

    default NetworkCapabilities agentConnect(NetworkCapabilities capabilities, boolean lockdown) {
        return capabilities;
    }
}
