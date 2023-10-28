package com.transsion.hubcore.server.wm;

import com.android.server.wm.WindowManagerService;
import com.transsion.hubcore.server.wm.ITranRemoteAnimationController;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranRemoteAnimationController {
    public static final TranClassInfo<ITranRemoteAnimationController> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranRemoteAnimationControllerImpl", ITranRemoteAnimationController.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranRemoteAnimationController$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranRemoteAnimationController.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranRemoteAnimationController {
    }

    static ITranRemoteAnimationController Instance() {
        return (ITranRemoteAnimationController) classInfo.getImpl();
    }

    default void updateRotation(boolean mIsForThunderback, WindowManagerService mService, String tag) {
    }
}
