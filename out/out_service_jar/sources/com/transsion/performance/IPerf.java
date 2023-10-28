package com.transsion.performance;

import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import com.transsion.performance.IPerf;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_SERVICES)
/* loaded from: classes2.dex */
public interface IPerf {
    public static final LiceInfo<IPerf> sLiceInfo = new LiceInfo<>("com.transsion.performance.Perf", IPerf.class, new Supplier() { // from class: com.transsion.performance.IPerf$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new IPerf.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements IPerf {
    }

    static IPerf Instance() {
        return (IPerf) sLiceInfo.getImpl();
    }

    default void tuningSwappinessB() {
    }

    default void tuningSwappinessA() {
    }
}
