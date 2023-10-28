package com.android.server;

import com.android.server.ISystemServerLice;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_SERVICES)
/* loaded from: classes.dex */
public interface ISystemServerLice {
    public static final LiceInfo<ISystemServerLice> sLiceInfo = new LiceInfo<>("com.transsion.server.SystemServerLice", ISystemServerLice.class, new Supplier() { // from class: com.android.server.ISystemServerLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ISystemServerLice.DefaultImpl();
        }
    });

    /* loaded from: classes.dex */
    public static class DefaultImpl implements ISystemServerLice {
    }

    static ISystemServerLice Instance() {
        return (ISystemServerLice) sLiceInfo.getImpl();
    }

    default void onStartSystemService(SystemService service) {
    }

    default void onStartBootPhase(int phase) {
    }
}
