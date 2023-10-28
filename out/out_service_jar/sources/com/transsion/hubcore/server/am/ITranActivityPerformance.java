package com.transsion.hubcore.server.am;

import android.os.Bundle;
import com.transsion.hubcore.server.am.ITranActivityPerformance;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranActivityPerformance {
    public static final TranClassInfo<ITranActivityPerformance> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.am.TranActivityPerformanceImpl", ITranActivityPerformance.class, new Supplier() { // from class: com.transsion.hubcore.server.am.ITranActivityPerformance$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranActivityPerformance.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranActivityPerformance {
    }

    static ITranActivityPerformance Instance() {
        return (ITranActivityPerformance) classInfo.getImpl();
    }

    default void setActivityResumed(String packageName, String activityName, String reason, int hashCode) {
    }

    default void setActivityStartLaunch(String packageName, String activityName, int type, long key) {
    }

    default void setActivityStopLaunch(String packageName, long key, long launchInterval) {
    }

    default void setDumpMessage(Bundle bundle) {
    }
}
