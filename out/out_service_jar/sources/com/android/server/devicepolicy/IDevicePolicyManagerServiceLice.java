package com.android.server.devicepolicy;

import android.content.Context;
import com.android.server.devicepolicy.IDevicePolicyManagerServiceLice;
import com.transsion.lice.LiceInfo;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public interface IDevicePolicyManagerServiceLice {
    public static final LiceInfo<IDevicePolicyManagerServiceLice> sLiceInfo = new LiceInfo<>("com.transsion.server.devicepolicy.DevicePolicyManagerServiceLice", IDevicePolicyManagerServiceLice.class, new Supplier() { // from class: com.android.server.devicepolicy.IDevicePolicyManagerServiceLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new IDevicePolicyManagerServiceLice.DefaultImpl();
        }
    });

    /* loaded from: classes.dex */
    public static class DefaultImpl implements IDevicePolicyManagerServiceLice {
    }

    static IDevicePolicyManagerServiceLice Instance() {
        return (IDevicePolicyManagerServiceLice) sLiceInfo.getImpl();
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

    default boolean isDefaultDeviceOwner(String packageName) {
        return false;
    }
}
