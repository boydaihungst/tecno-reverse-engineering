package com.transsion.hubcore.server.view;

import android.content.Context;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranOrientationEventListener {
    public static final TranClassInfo<ITranOrientationEventListener> classInfo = new TranClassInfo<>("com.transsion.hubcore.view.TranOrientationEventListenerImpl", ITranOrientationEventListener.class, new Supplier() { // from class: com.transsion.hubcore.server.view.ITranOrientationEventListener$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranOrientationEventListener.lambda$static$0();
        }
    });

    static /* synthetic */ ITranOrientationEventListener lambda$static$0() {
        return new ITranOrientationEventListener() { // from class: com.transsion.hubcore.server.view.ITranOrientationEventListener.1
        };
    }

    static ITranOrientationEventListener Instance() {
        return classInfo.getImpl();
    }

    default boolean isThunderbackWindow(Context context) {
        return false;
    }
}
