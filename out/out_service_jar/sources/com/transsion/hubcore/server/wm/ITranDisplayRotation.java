package com.transsion.hubcore.server.wm;

import com.android.server.wm.WindowManagerService;
import com.transsion.hubcore.server.wm.ITranDisplayRotation;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranDisplayRotation {
    public static final TranClassInfo<ITranDisplayRotation> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranDisplayRotationImpl", ITranDisplayRotation.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranDisplayRotation$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranDisplayRotation.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranDisplayRotation {
    }

    static ITranDisplayRotation Instance() {
        return (ITranDisplayRotation) classInfo.getImpl();
    }

    default void onNotifySaveRotation(WindowManagerService service) {
    }

    default boolean isThunderbackTransitionAnimationRunning(boolean isRunning) {
        return false;
    }

    default void hookDefaultDisplayRotation(int displayId, int toRotation) {
    }

    default void updateRotationFinished() {
    }
}
