package com.transsion.hubcore.standbyturbo;

import android.content.Context;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranStandbyTurbo {
    public static final TranClassInfo<ITranStandbyTurbo> classInfo = new TranClassInfo<>("com.transsion.hubcore.standbyturbo.TranStandbyTurboImpl", ITranStandbyTurbo.class, new Supplier() { // from class: com.transsion.hubcore.standbyturbo.ITranStandbyTurbo$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranStandbyTurbo.lambda$static$0();
        }
    });

    static /* synthetic */ ITranStandbyTurbo lambda$static$0() {
        return new ITranStandbyTurbo() { // from class: com.transsion.hubcore.standbyturbo.ITranStandbyTurbo.1
        };
    }

    static ITranStandbyTurbo Instance() {
        return (ITranStandbyTurbo) classInfo.getImpl();
    }

    default void onConstruct(Context context) {
    }

    default void updateScreenState(boolean state) {
    }

    default void hookLongWLForceStop(String packageName) {
    }
}
