package com.transsion.hubcore.server.tne;

import com.transsion.hubcore.server.tne.ITranTne;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranTne {
    public static final TranClassInfo<ITranTne> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.tne.TranTNEServiceImpl", ITranTne.class, new Supplier() { // from class: com.transsion.hubcore.server.tne.ITranTne$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranTne.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranTne {
    }

    static ITranTne Instance() {
        return (ITranTne) classInfo.getImpl();
    }

    default void startTNE(String tag, long type, int pid, String externinfo) {
    }
}
