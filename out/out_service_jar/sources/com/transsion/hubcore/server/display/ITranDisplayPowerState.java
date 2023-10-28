package com.transsion.hubcore.server.display;

import com.transsion.hubcore.server.display.ITranDisplayPowerState;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranDisplayPowerState {
    public static final TranClassInfo<ITranDisplayPowerState> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.display.TranDisplayPowerStateImpl", ITranDisplayPowerState.class, new Supplier() { // from class: com.transsion.hubcore.server.display.ITranDisplayPowerState$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranDisplayPowerState.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranDisplayPowerState {
    }

    static ITranDisplayPowerState Instance() {
        return (ITranDisplayPowerState) classInfo.getImpl();
    }

    default void setPowerRequest(int request) {
    }

    default float getConnectPolicyBrightness(float brightness) {
        return brightness;
    }
}
