package com.transsion.hubcore.fuelgauge;

import com.android.internal.os.BatteryStatsImpl;
import com.transsion.hubcore.fuelgauge.ITranFuelgauge;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranFuelgauge {
    public static final TranClassInfo<ITranFuelgauge> classInfo = new TranClassInfo<>("com.transsion.hubcore.fuelgauge.TranFuelgaugeImpl", ITranFuelgauge.class, new Supplier() { // from class: com.transsion.hubcore.fuelgauge.ITranFuelgauge$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranFuelgauge.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranFuelgauge {
    }

    static ITranFuelgauge Instance() {
        return (ITranFuelgauge) classInfo.getImpl();
    }

    default void init(BatteryStatsImpl stats) {
    }
}
