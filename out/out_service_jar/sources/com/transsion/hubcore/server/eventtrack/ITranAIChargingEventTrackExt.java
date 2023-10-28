package com.transsion.hubcore.server.eventtrack;

import android.content.Context;
import com.transsion.hubcore.server.eventtrack.ITranAIChargingEventTrackExt;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranAIChargingEventTrackExt {
    public static final String IMPL_CLASS_NAME = "com.transsion.hubcore.server.eventtrack.TranAIChargingEventTrackExtImpl";
    public static final TranClassInfo<ITranAIChargingEventTrackExt> classInfo = new TranClassInfo<>(IMPL_CLASS_NAME, ITranAIChargingEventTrackExt.class, new Supplier() { // from class: com.transsion.hubcore.server.eventtrack.ITranAIChargingEventTrackExt$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranAIChargingEventTrackExt.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranAIChargingEventTrackExt {
    }

    static ITranAIChargingEventTrackExt Instance() {
        return (ITranAIChargingEventTrackExt) classInfo.getImpl();
    }

    default void TranAIChargingEventTrackExt() {
    }

    default void recordWakeUp(int reason, String details) {
    }

    default void recordGoToSleep(int reason) {
    }

    default void init(Context context) {
    }

    default void postBatteryLevel(int postBatteryLevel, int plugType) {
    }

    default void destroy() {
    }
}
