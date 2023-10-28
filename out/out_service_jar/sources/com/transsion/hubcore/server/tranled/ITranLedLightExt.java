package com.transsion.hubcore.server.tranled;

import android.content.Context;
import com.android.server.lights.LightsManager;
import com.transsion.hubcore.server.tranled.ITranLedLightExt;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.ArrayList;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranLedLightExt {
    public static final TranClassInfo<ITranLedLightExt> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.tranled.TranLedLightExtImpl", ITranLedLightExt.class, new Supplier() { // from class: com.transsion.hubcore.server.tranled.ITranLedLightExt$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranLedLightExt.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranLedLightExt {
    }

    static ITranLedLightExt Instance() {
        return (ITranLedLightExt) classInfo.getImpl();
    }

    default void registerLedObserver() {
    }

    default void upNotificationList(ArrayList<String> keyList) {
    }

    default boolean isLedWork() {
        return false;
    }

    default boolean isBatteryOnlyWork() {
        return false;
    }

    default void updateBattery(int level, int status) {
    }

    default void init(LightsManager lightsManager, Context context) {
    }
}
