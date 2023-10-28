package com.transsion.hubcore.server.wm;

import com.android.server.wm.WindowState;
import com.transsion.hubcore.server.wm.ITranRefreshRatePolicy;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranRefreshRatePolicy {
    public static final TranClassInfo<ITranRefreshRatePolicy> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranRefreshRatePolicyImpl", ITranRefreshRatePolicy.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranRefreshRatePolicy$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranRefreshRatePolicy.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranRefreshRatePolicy {
    }

    static ITranRefreshRatePolicy Instance() {
        return (ITranRefreshRatePolicy) classInfo.getImpl();
    }

    default float getPreferredRefreshRate(WindowState windowState) {
        return 0.0f;
    }

    default boolean isHookRefreshRatePolicy() {
        return false;
    }
}
