package com.transsion.hubcore.server.am;

import android.content.Context;
import android.content.Intent;
import com.android.server.am.ActiveServices;
import com.android.server.am.ProcessRecord;
import com.transsion.hubcore.griffin.lib.app.TranProcessWrapper;
import com.transsion.hubcore.server.am.ITranActiveServices;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranActiveServices {
    public static final String IMPL_CLASS_NAME = "com.transsion.hubcore.server.am.TranActiveServicesImpl";
    public static final TranClassInfo<ITranActiveServices> classInfo = new TranClassInfo<>(IMPL_CLASS_NAME, ITranActiveServices.class, new Supplier() { // from class: com.transsion.hubcore.server.am.ITranActiveServices$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranActiveServices.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranActiveServices {
    }

    static ITranActiveServices Instance() {
        return (ITranActiveServices) classInfo.getImpl();
    }

    default boolean canStartAppInLowStorage(Context context, String procName) {
        return true;
    }

    default boolean hookLimitRestartService(ActiveServices.HookActiveServiceInfo serviceInfo) {
        return false;
    }

    default boolean hookBringUpServiceLocked(int callerType, TranProcessWrapper callerProc, ActiveServices.HookActiveServiceInfo serviceInfo, Intent intent) {
        return false;
    }

    default boolean isAppIdleEnable() {
        return false;
    }

    default boolean limitBindService(ProcessRecord callerApp, String callingPackage, Intent intent, boolean callerIdle, boolean isWake, int callingUid) {
        return false;
    }
}
