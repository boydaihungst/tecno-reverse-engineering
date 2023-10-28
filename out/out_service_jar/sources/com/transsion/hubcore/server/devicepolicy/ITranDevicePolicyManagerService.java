package com.transsion.hubcore.server.devicepolicy;

import android.content.Context;
import com.transsion.hubcore.server.devicepolicy.ITranDevicePolicyManagerService;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranDevicePolicyManagerService {
    public static final TranClassInfo<ITranDevicePolicyManagerService> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.devicepolicy.TranDevicePolicyManagerServiceImpl", ITranDevicePolicyManagerService.class, new Supplier() { // from class: com.transsion.hubcore.server.devicepolicy.ITranDevicePolicyManagerService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranDevicePolicyManagerService.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranDevicePolicyManagerService {
    }

    static ITranDevicePolicyManagerService Instance() {
        return (ITranDevicePolicyManagerService) classInfo.getImpl();
    }

    default void checkSetDeviceOwnerByAdb(boolean isAdb, Context ctx) {
    }

    default void checkSetDeviceOwnerWhenCodeOk(String packageName, Context ctx) {
    }

    default boolean checkSetDeviceOwnerAfterUserSetup(Context ctx, boolean userManagerIsSplitSystemUser, boolean isUserSetupComplete, String packageName, boolean isWatch, int userCount) {
        return false;
    }

    default boolean clearDeviceOwnerPermissionCheck(String packageName, int callingUid) {
        return false;
    }
}
