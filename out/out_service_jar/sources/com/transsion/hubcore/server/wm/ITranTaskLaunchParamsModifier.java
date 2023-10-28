package com.transsion.hubcore.server.wm;

import android.app.ActivityOptions;
import com.android.server.wm.ActivityRecord;
import com.transsion.hubcore.server.wm.ITranTaskLaunchParamsModifier;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranTaskLaunchParamsModifier {
    public static final TranClassInfo<ITranTaskLaunchParamsModifier> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranTaskLaunchParamsModifierImpl", ITranTaskLaunchParamsModifier.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranTaskLaunchParamsModifier$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranTaskLaunchParamsModifier.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranTaskLaunchParamsModifier {
    }

    static ITranTaskLaunchParamsModifier Instance() {
        return (ITranTaskLaunchParamsModifier) classInfo.getImpl();
    }

    default boolean isNeedIntoMultiwindow(ActivityRecord activityRecord, ActivityOptions options) {
        return false;
    }

    default void showUnSupportMultiToast() {
    }

    default boolean inMultiWindow(ActivityRecord source) {
        return false;
    }

    default void hookMultiWindowToLargeV4(int multiWindowMode, int multiWindowId) {
    }
}
