package com.transsion.hubcore.server.am;

import com.android.internal.os.ProcessCpuTracker;
import com.android.server.am.ProcessRecord;
import com.transsion.hubcore.server.am.ITranAppProfiler;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranAppProfiler {
    public static final float CPU_PERCENT_FULL = 100.0f;
    public static final TranClassInfo<ITranAppProfiler> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.am.TranAppProfilerImpl", ITranAppProfiler.class, new Supplier() { // from class: com.transsion.hubcore.server.am.ITranAppProfiler$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranAppProfiler.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranAppProfiler {
    }

    static ITranAppProfiler Instance() {
        return (ITranAppProfiler) classInfo.getImpl();
    }

    default boolean isAgaresEnable() {
        return false;
    }

    default float getTotalCpuPercent(ProcessCpuTracker processCpuTracker) {
        return 100.0f;
    }

    default boolean isKeepAliveSupport() {
        return false;
    }

    default boolean isGameBoosterEnable() {
        return false;
    }

    default boolean isAppMemoryEnable() {
        return false;
    }

    default boolean shouldLimitPssOfProcess(ProcessRecord app, long lastPss, int curAdj, long now) {
        return false;
    }

    default boolean inPssListOfProcess(ProcessRecord app) {
        return false;
    }

    default void startCycleAbnormal(Runnable runnable) {
    }
}
