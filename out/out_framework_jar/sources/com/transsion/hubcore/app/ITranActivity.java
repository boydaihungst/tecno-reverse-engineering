package com.transsion.hubcore.app;

import android.app.Activity;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranActivity {
    public static final TranClassInfo<ITranActivity> classInfo = new TranClassInfo<>("com.transsion.hubcore.app.TranActivityImpl", ITranActivity.class, new Supplier() { // from class: com.transsion.hubcore.app.ITranActivity$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranActivity.lambda$static$0();
        }
    });

    static /* synthetic */ ITranActivity lambda$static$0() {
        return new ITranActivity() { // from class: com.transsion.hubcore.app.ITranActivity.1
        };
    }

    static ITranActivity Instance() {
        return classInfo.getImpl();
    }

    default void onResumeHork(Activity activity) {
    }

    default void attach(Activity activity) {
    }

    default void dispatchActivityStarted() {
    }

    default void dispatchActivityStopped() {
    }

    default void onPauseHook(Activity activity) {
    }
}
