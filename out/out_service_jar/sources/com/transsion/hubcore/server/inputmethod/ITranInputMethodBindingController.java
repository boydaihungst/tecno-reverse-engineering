package com.transsion.hubcore.server.inputmethod;

import android.content.Intent;
import com.transsion.hubcore.server.inputmethod.ITranInputMethodBindingController;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranInputMethodBindingController {
    public static final TranClassInfo<ITranInputMethodBindingController> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.inputmethod.TranInputMethodBindingControllerImpl", ITranInputMethodBindingController.class, new Supplier() { // from class: com.transsion.hubcore.server.inputmethod.ITranInputMethodBindingController$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranInputMethodBindingController.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranInputMethodBindingController {
    }

    static ITranInputMethodBindingController Instance() {
        return (ITranInputMethodBindingController) classInfo.getImpl();
    }

    default void hookSetCurrentMethodVisible(Intent intent) {
    }
}
