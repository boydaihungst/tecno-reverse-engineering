package com.transsion.hubcore.server.wm;

import com.android.server.wm.ActivityRecord;
import com.android.server.wm.ActivityTaskManagerService;
import com.transsion.hubcore.server.wm.ITranKeyguardController;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranKeyguardController {
    public static final TranClassInfo<ITranKeyguardController> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranKeyguardControllerImpl", ITranKeyguardController.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranKeyguardController$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranKeyguardController.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranKeyguardController {
    }

    static ITranKeyguardController Instance() {
        return (ITranKeyguardController) classInfo.getImpl();
    }

    default void hookAuthSuccess() {
    }

    default void hookUpdateKeyguardLocked(boolean isKeyguardLocked) {
    }

    default void onConstruct(ActivityTaskManagerService service) {
    }

    default boolean hookMultiWindowToMaxOrInvisible(String tag, boolean mKeyguardShowing, ActivityRecord mOccludedActivity, boolean isInMultiWindow) {
        return false;
    }

    default boolean hookMultiWindowToDefault(String mTag, boolean occluded, boolean hookToMax, ActivityRecord top) {
        return false;
    }

    default void hookMultiWindowToMax(String tag, boolean mOccluded, boolean inMultiWindow) {
    }

    default boolean inMultiWindow(ActivityRecord r) {
        return false;
    }
}
