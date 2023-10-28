package com.transsion.hubcore.view;

import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranChoreographer {
    public static final TranClassInfo<ITranChoreographer> classInfo = new TranClassInfo<>("com.transsion.hubcore.view.TranChoreographerImpl", ITranChoreographer.class, new Supplier() { // from class: com.transsion.hubcore.view.ITranChoreographer$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranChoreographer.lambda$static$0();
        }
    });

    static /* synthetic */ ITranChoreographer lambda$static$0() {
        return new ITranChoreographer() { // from class: com.transsion.hubcore.view.ITranChoreographer.1
        };
    }

    static ITranChoreographer Instance() {
        return classInfo.getImpl();
    }

    default void animationBegin(long frameIntervalNanos, long startNanos) {
    }

    default void animationEnd() {
    }
}
