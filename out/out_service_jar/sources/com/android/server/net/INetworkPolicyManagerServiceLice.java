package com.android.server.net;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.NetworkPolicyManager;
import android.util.SparseIntArray;
import com.android.server.net.INetworkPolicyManagerServiceLice;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_SERVICES)
/* loaded from: classes2.dex */
public interface INetworkPolicyManagerServiceLice {
    public static final LiceInfo<INetworkPolicyManagerServiceLice> sLiceInfo = new LiceInfo<>("com.transsion.server.net.NetworkPolicyManagerServiceLice", INetworkPolicyManagerServiceLice.class, new Supplier() { // from class: com.android.server.net.INetworkPolicyManagerServiceLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new INetworkPolicyManagerServiceLice.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements INetworkPolicyManagerServiceLice {
    }

    static INetworkPolicyManagerServiceLice Instance() {
        return (INetworkPolicyManagerServiceLice) sLiceInfo.getImpl();
    }

    default void onInitService(Context context) {
    }

    default void onUpdateRulesForWhitelistedPowerSaveUL(boolean enabled, int chain, SparseIntArray rules, boolean isUserDozeMode) {
    }

    default boolean skipAddDefaultRestrictBackgroundWhitelist(ApplicationInfo appInfo) {
        return true;
    }

    default int updateRulesForDataUsageRestrictionsULInnerUidPolicy(int uid, int policy) {
        return policy;
    }

    default boolean isUidForegroundOnRestrictBackgroundUL(NetworkPolicyManager.UidState uidState, boolean isAllow) {
        return isAllow;
    }

    default boolean isUidForegroundOnRestrictPowerUL(NetworkPolicyManager.UidState uidState, boolean isAllow) {
        return isAllow;
    }

    default boolean isRestrictedByAdminUL(int uid, boolean isRestricted) {
        return isRestricted;
    }

    default boolean isWhitelistedFromPowerSaveUL(int uid, boolean isWhitelisted) {
        return isWhitelisted;
    }

    default boolean updateRulesForAppIdleParoleUL() {
        return false;
    }

    default int updateRulesForDataUsageRestrictionsULInnerNewUidRules(int uid, int newUidRules, boolean isForeground) {
        return newUidRules;
    }

    default void updateRestrictBackgroundRulesOnUidStatusChangedUL(int uid, NetworkPolicyManager.UidState oldUidState, NetworkPolicyManager.UidState newUidState) {
    }

    default boolean updateRulesForWhitelistedAppIds(int uid) {
        return true;
    }
}
