package com.transsion.hubcore.server.power;

import android.content.Context;
import com.transsion.hubcore.server.power.ITranShutdownThread;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranShutdownThread {
    public static final String IMPL_CLASS_NAME = "com.transsion.hubcore.server.power.TranShutdownThreadImpl";
    public static final TranClassInfo<ITranShutdownThread> classInfo = new TranClassInfo<>(IMPL_CLASS_NAME, ITranShutdownThread.class, new Supplier() { // from class: com.transsion.hubcore.server.power.ITranShutdownThread$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranShutdownThread.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranShutdownThread {
    }

    static ITranShutdownThread Instance() {
        return (ITranShutdownThread) classInfo.getImpl();
    }

    default void recordShutdown(Context context, boolean reboot, String reason) {
    }
}
