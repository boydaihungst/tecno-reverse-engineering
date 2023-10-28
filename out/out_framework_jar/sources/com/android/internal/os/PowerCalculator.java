package com.android.internal.os;

import android.os.BatteryStats;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.os.UidBatteryConsumer;
import android.util.SparseArray;
import java.io.PrintWriter;
/* loaded from: classes4.dex */
public abstract class PowerCalculator {
    protected static final boolean DEBUG = false;
    protected static final double MILLIAMPHOUR_PER_MICROCOULOMB = 2.777777777777778E-7d;

    public abstract boolean isPowerComponentSupported(int i);

    public void calculate(BatteryUsageStats.Builder builder, BatteryStats batteryStats, long rawRealtimeUs, long rawUptimeUs, BatteryUsageStatsQuery query) {
        SparseArray<UidBatteryConsumer.Builder> uidBatteryConsumerBuilders = builder.getUidBatteryConsumerBuilders();
        for (int i = uidBatteryConsumerBuilders.size() - 1; i >= 0; i--) {
            UidBatteryConsumer.Builder app = uidBatteryConsumerBuilders.valueAt(i);
            calculateApp(app, app.getBatteryStatsUid(), rawRealtimeUs, rawUptimeUs, query);
        }
    }

    protected void calculateApp(UidBatteryConsumer.Builder app, BatteryStats.Uid u, long rawRealtimeUs, long rawUptimeUs, BatteryUsageStatsQuery query) {
    }

    public void reset() {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public static int getPowerModel(long measuredEnergyUC, BatteryUsageStatsQuery query) {
        if (measuredEnergyUC != -1 && !query.shouldForceUsePowerProfileModel()) {
            return 2;
        }
        return 1;
    }

    protected static int getPowerModel(long measuredEnergyUC) {
        if (measuredEnergyUC != -1) {
            return 2;
        }
        return 1;
    }

    public static void printPowerMah(PrintWriter pw, double powerMah) {
        pw.print(BatteryStats.formatCharge(powerMah));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static double uCtoMah(long chargeUC) {
        return chargeUC * MILLIAMPHOUR_PER_MICROCOULOMB;
    }
}
