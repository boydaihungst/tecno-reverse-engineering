package com.transsion.hubcore.server.am;

import com.transsion.hubcore.griffin.lib.app.TranProcessWrapper;
import com.transsion.hubcore.server.am.ITranOomAdjuster;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranOomAdjuster {
    public static final String IMPL_CLASS_NAME = "com.transsion.hubcore.server.am.TranOomAdjusterImpl";
    public static final TranClassInfo<ITranOomAdjuster> classInfo = new TranClassInfo<>(IMPL_CLASS_NAME, ITranOomAdjuster.class, new Supplier() { // from class: com.transsion.hubcore.server.am.ITranOomAdjuster$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranOomAdjuster.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranOomAdjuster {
    }

    static ITranOomAdjuster Instance() {
        return (ITranOomAdjuster) classInfo.getImpl();
    }

    default void hookUidChangedIdle(int uid) {
    }

    default void hookUidChangedActive(int uid) {
    }

    default int[] fixOomAdj(TranProcessWrapper app, int origGroup, int origAdj, int origState, String state) {
        return new int[0];
    }

    default boolean isMultiKillEnabled(String processName) {
        return false;
    }

    default boolean hookSetShouldNotFreeze(String packageName) {
        return false;
    }
}
