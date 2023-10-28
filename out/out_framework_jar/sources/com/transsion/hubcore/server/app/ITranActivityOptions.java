package com.transsion.hubcore.server.app;

import android.app.ActivityOptions;
import android.os.Bundle;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranActivityOptions {
    public static final TranClassInfo<ITranActivityOptions> classInfo = new TranClassInfo<>("com.transsion.hubcore.app.TranActivityOptionsImpl", ITranActivityOptions.class, new Supplier() { // from class: com.transsion.hubcore.server.app.ITranActivityOptions$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranActivityOptions.lambda$static$0();
        }
    });

    static /* synthetic */ ITranActivityOptions lambda$static$0() {
        return new ITranActivityOptions() { // from class: com.transsion.hubcore.server.app.ITranActivityOptions.1
        };
    }

    static ITranActivityOptions Instance() {
        return classInfo.getImpl();
    }

    default void setThunderBackSupport(ActivityOptions activityOptions, Bundle opts) {
    }

    default void putKeyThunderBack(ActivityOptions activityOptions, Bundle b) {
    }
}
