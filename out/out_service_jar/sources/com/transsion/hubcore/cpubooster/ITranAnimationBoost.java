package com.transsion.hubcore.cpubooster;

import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranAnimationBoost {
    public static final TranClassInfo<ITranAnimationBoost> classInfo = new TranClassInfo<>("com.transsion.hubcore.cpubooster.TranAnimationBoostImpl", ITranAnimationBoost.class, new Supplier() { // from class: com.transsion.hubcore.cpubooster.ITranAnimationBoost$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranAnimationBoost.lambda$static$0();
        }
    });

    static /* synthetic */ ITranAnimationBoost lambda$static$0() {
        return new ITranAnimationBoost() { // from class: com.transsion.hubcore.cpubooster.ITranAnimationBoost.1
        };
    }

    static ITranAnimationBoost Instance() {
        return (ITranAnimationBoost) classInfo.getImpl();
    }

    default void boostHomeProcess(int pid, int rtid, String pkg) {
    }

    default boolean isLauncherApp(String pkg) {
        return false;
    }

    default boolean getAnimBoostStatu() {
        return false;
    }

    default void setLaunchAnimGroup() {
    }
}
