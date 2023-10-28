package com.transsion.hubcore.server.wm;

import com.android.server.wm.WindowProcessController;
import com.transsion.hubcore.server.wm.ITranActivityMetricsLogger;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranActivityMetricsLogger {
    public static final TranClassInfo<ITranActivityMetricsLogger> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranActivityMetricsLoggerImpl", ITranActivityMetricsLogger.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranActivityMetricsLogger$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranActivityMetricsLogger.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranActivityMetricsLogger {
    }

    static ITranActivityMetricsLogger Instance() {
        return (ITranActivityMetricsLogger) classInfo.getImpl();
    }

    default void startLaunchTrace(String packageName, String activityName, int type, long key, boolean where) {
    }

    default void stopLaunchTrace(String packageName, long key, long launchInterval) {
    }

    default boolean isAgaresEnable() {
        return false;
    }

    default void onAgaresActivityStart(String pkg, String cls, int transitionType, boolean isAgares) {
    }

    default boolean isAgaresProcess(WindowProcessController wpc) {
        return false;
    }

    default boolean isGameBoosterEnable() {
        return false;
    }

    default void onActivityStart(String packageName, String activity, int launchType, boolean isAgares) {
    }
}
