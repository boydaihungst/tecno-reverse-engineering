package com.transsion.hubcore.view;

import android.content.Context;
import android.view.MotionEvent;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranViewRootImpl {
    public static final TranClassInfo<ITranViewRootImpl> classInfo = new TranClassInfo<>("com.transsion.hubcore.view.TranViewRootImplImpl", ITranViewRootImpl.class, new Supplier() { // from class: com.transsion.hubcore.view.ITranViewRootImpl$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranViewRootImpl.lambda$static$0();
        }
    });

    static /* synthetic */ ITranViewRootImpl lambda$static$0() {
        return new ITranViewRootImpl() { // from class: com.transsion.hubcore.view.ITranViewRootImpl.1
        };
    }

    static ITranViewRootImpl Instance() {
        return classInfo.getImpl();
    }

    default boolean checkMotionEvent(MotionEvent event) {
        return false;
    }

    default void onInit(Context context) {
    }

    default void initDrt(Context context, String packageName) {
    }

    default boolean isResolutionTuningPackage() {
        return false;
    }

    default float getXScale() {
        return 1.0f;
    }

    default float getYScale() {
        return 1.0f;
    }

    default boolean isRtEnable() {
        return false;
    }

    default boolean isScaledByGameMode() {
        return false;
    }
}
