package com.transsion.hubcore.server.net;

import android.content.Context;
import android.net.INetd;
import android.net.Network;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranNetworkManagementService {
    public static final TranClassInfo<ITranNetworkManagementService> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.net.TranNetworkManagementServiceImpl", ITranNetworkManagementService.class, new Supplier() { // from class: com.transsion.hubcore.server.net.ITranNetworkManagementService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranNetworkManagementService.lambda$static$0();
        }
    });

    static /* synthetic */ ITranNetworkManagementService lambda$static$0() {
        return new ITranNetworkManagementService() { // from class: com.transsion.hubcore.server.net.ITranNetworkManagementService.1
        };
    }

    static ITranNetworkManagementService Instance() {
        return (ITranNetworkManagementService) classInfo.getImpl();
    }

    default void create(Context context, INetd iNetd) {
    }

    default boolean isAppNetWorkAccelerated() {
        return false;
    }

    default boolean switchAppNetWorkAccelerated(boolean isOpen, String pkgName) {
        return false;
    }

    default boolean isAcceleratedEnabled(String pkgName) {
        return false;
    }

    default void destorySocketByUid(int uid, INetd iNetd) {
    }

    default boolean bindAppUidToNetwork(Context context, INetd iNetd, int uid, Network network) {
        return false;
    }

    default boolean setMultiLink(int uid, Network network, boolean isMultiLink, INetd iNetd) {
        return false;
    }

    default boolean setAcceleratedEnabledByPhone(boolean enable) {
        return false;
    }
}
