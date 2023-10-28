package com.transsion.hubcore.os;

import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranLooper {
    public static final TranClassInfo<ITranLooper> classInfo = new TranClassInfo<>("com.transsion.hubcore.os.TranLooperImpl", ITranLooper.class, new Supplier() { // from class: com.transsion.hubcore.os.ITranLooper$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranLooper.lambda$static$0();
        }
    });

    static /* synthetic */ ITranLooper lambda$static$0() {
        return new ITranLooper() { // from class: com.transsion.hubcore.os.ITranLooper.1
        };
    }

    static ITranLooper Instance() {
        return classInfo.getImpl();
    }

    default void printThreadStack(Thread thread, long timeout) {
    }
}
