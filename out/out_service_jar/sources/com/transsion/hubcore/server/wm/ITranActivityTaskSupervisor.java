package com.transsion.hubcore.server.wm;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.WindowProcessController;
import com.transsion.hubcore.griffin.lib.app.TranProcessWrapper;
import com.transsion.hubcore.server.wm.ITranActivityTaskSupervisor;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.List;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranActivityTaskSupervisor {
    public static final TranClassInfo<ITranActivityTaskSupervisor> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranActivityTaskSupervisorImpl", ITranActivityTaskSupervisor.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranActivityTaskSupervisor$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranActivityTaskSupervisor.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranActivityTaskSupervisor {
    }

    static ITranActivityTaskSupervisor Instance() {
        return (ITranActivityTaskSupervisor) classInfo.getImpl();
    }

    default void onConstruct(ActivityTaskManagerService service) {
    }

    default boolean hasMultiWindow(int windowingMode) {
        return false;
    }

    default void updateHomeProcess(ActivityInfo info, TranProcessWrapper processWrapper) {
    }

    default void receiveProcInfo(List<TranProcessWrapper> procWrapperToKill, WindowProcessController proc) {
    }

    default void hookCleanUpRemovedTaskNoKill(List<TranProcessWrapper> procWrapperToKill, ComponentName componentName) {
    }

    default void hookCleanUpRemovedTaskKill(List<TranProcessWrapper> procWrapperToKill, ComponentName componentName) {
    }

    default boolean isKeepAliveSupport() {
        return false;
    }

    default boolean isAgaresCustomerEnable() {
        return false;
    }

    default boolean isGameBoosterEnable() {
        return false;
    }

    default boolean isGriffinSupport() {
        return false;
    }

    default boolean isTartgetApp(String packageName) {
        return false;
    }

    default void boostPreloadStart(int uid, String packageName) {
    }
}
