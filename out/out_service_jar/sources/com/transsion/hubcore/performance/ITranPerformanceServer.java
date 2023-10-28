package com.transsion.hubcore.performance;

import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranPerformanceServer {
    public static final String DEFAULT_IMPL = "com.transsion.hubcore.performance.TranPerformanceServerImpl";
    public static final TranClassInfo<ITranPerformanceServer> classInfo = new TranClassInfo<>(DEFAULT_IMPL, ITranPerformanceServer.class, new Supplier() { // from class: com.transsion.hubcore.performance.ITranPerformanceServer$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranPerformanceServer.lambda$static$0();
        }
    });

    static /* synthetic */ ITranPerformanceServer lambda$static$0() {
        return new ITranPerformanceServer() { // from class: com.transsion.hubcore.performance.ITranPerformanceServer.1
        };
    }

    static ITranPerformanceServer Instance() {
        return (ITranPerformanceServer) classInfo.getImpl();
    }

    default void tuningSwappinessA() {
    }

    default void tuningSwappinessB() {
    }
}
