package com.transsion.hubcore.server.am;

import com.transsion.hubcore.server.am.ITranAmHooker;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranAmHooker {
    public static final TranClassInfo<ITranAmHooker> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.am.TranAmHookerImpl", ITranAmHooker.class, new Supplier() { // from class: com.transsion.hubcore.server.am.ITranAmHooker$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranAmHooker.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranAmHooker {
    }

    static ITranAmHooker Instance() {
        return (ITranAmHooker) classInfo.getImpl();
    }

    default boolean isGameBooseterSupport() {
        return false;
    }

    default boolean isEnable() {
        return false;
    }

    default boolean isTargetGameApp(String packageName) {
        return false;
    }

    default boolean isDebug() {
        return false;
    }

    default long getAvailMemThreshold() {
        return 800L;
    }

    default void onAppCrash(String packageName) {
    }

    default void onActivityStart(String packageName, String activity, int launchType, boolean isAgares) {
    }

    default boolean isPreloadedApp(String packageName) {
        return false;
    }

    default void onPredictWrong(String packageName) {
    }

    default void boosterCpu(int uid, String packageName) {
    }

    default void boosterCpuStop() {
    }
}
