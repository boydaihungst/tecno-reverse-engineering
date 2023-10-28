package com.transsion.hubcore.server.net;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkPolicyManager;
import android.net.NetworkTemplate;
import com.transsion.hubcore.server.net.ITranNetworkPolicyManagerService;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranNetworkPolicyManagerService {
    public static final TranClassInfo<ITranNetworkPolicyManagerService> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.net.TranNetworkPolicyManagerServiceImpl", ITranNetworkPolicyManagerService.class, new Supplier() { // from class: com.transsion.hubcore.server.net.ITranNetworkPolicyManagerService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranNetworkPolicyManagerService.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranNetworkPolicyManagerService {
    }

    static ITranNetworkPolicyManagerService Instance() {
        return (ITranNetworkPolicyManagerService) classInfo.getImpl();
    }

    default void onInitService(Context context) {
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

    default void updateRestrictBackgroundRulesOnUidStatusChangedUL(int uid, NetworkPolicyManager.UidState oldUidState, NetworkPolicyManager.UidState newUidState) {
    }

    default boolean updateRulesForWhitelistedAppIds(int uid) {
        return true;
    }

    default Intent buildViewDataUsageIntent(NetworkTemplate template) {
        return null;
    }
}
