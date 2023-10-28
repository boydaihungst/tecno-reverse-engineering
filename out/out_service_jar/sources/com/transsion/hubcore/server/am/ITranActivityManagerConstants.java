package com.transsion.hubcore.server.am;

import com.transsion.hubcore.server.am.ITranActivityManagerConstants;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranActivityManagerConstants {
    public static final TranClassInfo<ITranActivityManagerConstants> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.am.TranActivityManagerConstantsImpl", ITranActivityManagerConstants.class, new Supplier() { // from class: com.transsion.hubcore.server.am.ITranActivityManagerConstants$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranActivityManagerConstants.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranActivityManagerConstants {
    }

    static ITranActivityManagerConstants Instance() {
        return (ITranActivityManagerConstants) classInfo.getImpl();
    }

    default boolean isMemFusionEnabled() {
        return false;
    }

    default int getMemFusionMaxCachedProcesses() {
        return 32;
    }

    default boolean isAgaresEnable() {
        return false;
    }

    default int getAgaresMaxCachedProcesses(int curMaxCachedProcesses) {
        return curMaxCachedProcesses;
    }

    default boolean isGriffinEnable() {
        return false;
    }

    default int updatePmCurMaxCachedNumber(int curMaxCachedProcesses) {
        return curMaxCachedProcesses;
    }

    default int updatePmMaxCachedNumber(int maxCachedProcesses) {
        return maxCachedProcesses;
    }
}
