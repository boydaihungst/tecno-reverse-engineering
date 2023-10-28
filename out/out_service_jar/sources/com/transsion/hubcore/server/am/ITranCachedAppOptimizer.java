package com.transsion.hubcore.server.am;

import com.transsion.hubcore.server.am.ITranCachedAppOptimizer;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranCachedAppOptimizer {
    public static final TranClassInfo<ITranCachedAppOptimizer> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.am.TranCachedAppOptimizerImpl", ITranCachedAppOptimizer.class, new Supplier() { // from class: com.transsion.hubcore.server.am.ITranCachedAppOptimizer$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranCachedAppOptimizer.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranCachedAppOptimizer {
    }

    static ITranCachedAppOptimizer Instance() {
        return (ITranCachedAppOptimizer) classInfo.getImpl();
    }

    default boolean isMemFusionEnabled() {
        return false;
    }

    default boolean updateCompactionState() {
        return false;
    }
}
