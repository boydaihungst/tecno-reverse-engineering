package com.transsion.hubcore.server.wm;

import android.content.ComponentName;
import android.content.res.Configuration;
import com.android.server.wm.ActivityRecord;
import com.transsion.hubcore.server.wm.ITranTask;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.List;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranTask {
    public static final TranClassInfo<ITranTask> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranTaskImpl", ITranTask.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranTask$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranTask.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranTask {
    }

    static ITranTask Instance() {
        return (ITranTask) classInfo.getImpl();
    }

    default void showStabilityMultiToast(Configuration newParentConfig, Configuration currentConfiguration, ComponentName realActivity, List<String> multiWindowWhiteList) {
    }

    default void reportDescendantOrientationChangeIfNeeded(boolean isMultiWindow, ActivityRecord resumedActivity, String tag) {
    }

    default void hookMultiWindowToCloseV3(int displayAreaId) {
    }

    default void hookMultiWindowToCloseV4(int multiWindowMode, int multiWindowId) {
    }

    default void hookMinimalResumeActivityLocked(ActivityRecord record) {
    }

    default boolean isAdjacentTaskEnable() {
        return false;
    }

    default boolean hookShouldMarkAdjacentTask(int prevWinMode, int newWinMode, boolean isLeaf, boolean byOrganizer) {
        return false;
    }

    default boolean hookMarkAdjacentTask(Object aTask) {
        return false;
    }

    default void hookLookupAdjacentTask(Object aTask) {
    }

    default void hookResetAdjacentTask() {
    }

    default void hookResetAdjacentTaskForTask(Object resetTask) {
    }
}
