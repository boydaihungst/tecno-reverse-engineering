package com.transsion.hubcore.server.wm;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import com.transsion.hubcore.griffin.lib.app.TranProcessWrapper;
import com.transsion.hubcore.server.wm.ITranActivityClientController;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranActivityClientController {
    public static final TranClassInfo<ITranActivityClientController> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranActivityClientControllerImpl", ITranActivityClientController.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranActivityClientController$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranActivityClientController.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranActivityClientController {
    }

    static ITranActivityClientController Instance() {
        return (ITranActivityClientController) classInfo.getImpl();
    }

    default void hookActivityIdle(TranProcessWrapper proc, ActivityInfo info, Intent intent) {
    }
}
