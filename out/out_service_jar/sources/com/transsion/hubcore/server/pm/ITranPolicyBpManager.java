package com.transsion.hubcore.server.pm;

import android.os.SystemProperties;
import com.transsion.hubcore.server.pm.ITranPolicyBpManager;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.ArrayList;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranPolicyBpManager {
    public static final boolean BURIEDPOINT_ENABLE;
    public static final String CLEAN_TYPE_ADJ = "adj";
    public static final String CLEAN_TYPE_CPU = "cpu";
    public static final String CLEAN_TYPE_IDEL = "idel";
    public static final String CLEAN_TYPE_LOWRAM = "lowRam";
    public static final String CLEAN_TYPE_LOWSWAP = "lowSwap";
    public static final TranClassInfo<ITranPolicyBpManager> classInfo;

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranPolicyBpManager {
    }

    static {
        BURIEDPOINT_ENABLE = 1 == SystemProperties.getInt("persist.sys.pm_buriedpoint.support", 0);
        classInfo = new TranClassInfo<>("com.transsion.hubcore.server.pm.TranPolicyBpManagerImpl", ITranPolicyBpManager.class, new Supplier() { // from class: com.transsion.hubcore.server.pm.ITranPolicyBpManager$$ExternalSyntheticLambda0
            @Override // java.util.function.Supplier
            public final Object get() {
                return new ITranPolicyBpManager.DefaultImpl();
            }
        });
    }

    static ITranPolicyBpManager Instance() {
        return (ITranPolicyBpManager) classInfo.getImpl();
    }

    default void setOneKeyCleanInfo(String callerPkg, long callTime, boolean screenState, boolean callerIsForground, ArrayList<String> stopAppList, int killProcCount, long ramGap, String jar_ver) {
    }

    default void setForceStopInfo(String callerPkg, String targetPkg, long callTime, String reason) {
    }

    default void setAutoCleanInfo(String c_type, long swapGap, long ramGap, long cpuGap, ArrayList<String> stopList, ArrayList<String> killList, String jar_ver) {
    }

    default void setIdleCleanInfo(long ramGap, ArrayList<String> killList, String jar_ver) {
    }

    default void recordException(Throwable e) {
    }

    default void recordAppException(String pkg, String proc, long ram_u, double p_con, long t_t, long aram_u, String app_v, long s_t) {
    }
}
