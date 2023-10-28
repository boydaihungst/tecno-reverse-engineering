package com.android.server.net;

import android.net.Network;
import android.telephony.SubscriptionPlan;
import com.android.server.net.NetworkPolicyManagerService;
import java.util.Set;
/* loaded from: classes2.dex */
public abstract class NetworkPolicyManagerInternal {
    public static final int QUOTA_TYPE_JOBS = 1;
    public static final int QUOTA_TYPE_MULTIPATH = 2;

    public abstract long getSubscriptionOpportunisticQuota(Network network, int i);

    public abstract SubscriptionPlan getSubscriptionPlan(Network network);

    public abstract void onAdminDataAvailable();

    public abstract void onTempPowerSaveWhitelistChange(int i, boolean z, int i2, String str);

    public abstract void resetUserState(int i);

    public abstract void setAppIdleWhitelist(int i, boolean z);

    public abstract void setLowPowerStandbyActive(boolean z);

    public abstract void setLowPowerStandbyAllowlist(int[] iArr);

    public abstract void setMeteredRestrictedPackages(Set<String> set, int i);

    public abstract void setMeteredRestrictedPackagesAsync(Set<String> set, int i);

    public static int updateBlockedReasonsWithProcState(int blockedReasons, int procState) {
        int allowedReasons = NetworkPolicyManagerService.UidBlockedState.getAllowedReasonsForProcState(procState);
        return NetworkPolicyManagerService.UidBlockedState.getEffectiveBlockedReasons(blockedReasons, allowedReasons);
    }
}
