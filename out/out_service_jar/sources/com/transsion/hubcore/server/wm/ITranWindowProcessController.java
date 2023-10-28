package com.transsion.hubcore.server.wm;

import com.android.server.wm.ActivityRecord;
import com.transsion.hubcore.server.wm.ITranWindowProcessController;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranWindowProcessController {
    public static final TranClassInfo<ITranWindowProcessController> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranWindowProcessControllerImpl", ITranWindowProcessController.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranWindowProcessController$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranWindowProcessController.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranWindowProcessController {
    }

    static ITranWindowProcessController Instance() {
        return (ITranWindowProcessController) classInfo.getImpl();
    }

    default boolean inMultiWindow(String packageName, ActivityRecord r) {
        return false;
    }
}
