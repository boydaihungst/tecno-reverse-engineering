package com.android.internal.os;

import android.os.AggregateBatteryConsumer;
import android.os.BatteryStats;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.os.UidBatteryConsumer;
import android.util.Slog;
import android.util.SparseArray;
import java.util.Arrays;
/* loaded from: classes4.dex */
public class CustomMeasuredPowerCalculator extends PowerCalculator {
    private static final String TAG = "CustomMeasuredPowerCalc";

    public CustomMeasuredPowerCalculator(PowerProfile powerProfile) {
    }

    @Override // com.android.internal.os.PowerCalculator
    public boolean isPowerComponentSupported(int powerComponent) {
        return false;
    }

    @Override // com.android.internal.os.PowerCalculator
    public void calculate(BatteryUsageStats.Builder builder, BatteryStats batteryStats, long rawRealtimeUs, long rawUptimeUs, BatteryUsageStatsQuery query) {
        double[] totalAppPowerMah = null;
        SparseArray<UidBatteryConsumer.Builder> uidBatteryConsumerBuilders = builder.getUidBatteryConsumerBuilders();
        for (int i = uidBatteryConsumerBuilders.size() - 1; i >= 0; i--) {
            UidBatteryConsumer.Builder app = uidBatteryConsumerBuilders.valueAt(i);
            totalAppPowerMah = calculateApp(app, app.getBatteryStatsUid(), totalAppPowerMah);
        }
        double[] customMeasuredPowerMah = calculateMeasuredEnergiesMah(batteryStats.getCustomConsumerMeasuredBatteryConsumptionUC());
        if (customMeasuredPowerMah != null) {
            AggregateBatteryConsumer.Builder deviceBatteryConsumerBuilder = builder.getAggregateBatteryConsumerBuilder(0);
            for (int i2 = 0; i2 < customMeasuredPowerMah.length; i2++) {
                deviceBatteryConsumerBuilder.setConsumedPowerForCustomComponent(i2 + 1000, customMeasuredPowerMah[i2]);
            }
        }
        if (totalAppPowerMah != null) {
            AggregateBatteryConsumer.Builder appsBatteryConsumerBuilder = builder.getAggregateBatteryConsumerBuilder(1);
            for (int i3 = 0; i3 < totalAppPowerMah.length; i3++) {
                appsBatteryConsumerBuilder.setConsumedPowerForCustomComponent(i3 + 1000, totalAppPowerMah[i3]);
            }
        }
    }

    private double[] calculateApp(UidBatteryConsumer.Builder app, BatteryStats.Uid u, double[] totalPowerMah) {
        double[] newTotalPowerMah = null;
        double[] customMeasuredPowerMah = calculateMeasuredEnergiesMah(u.getCustomConsumerMeasuredBatteryConsumptionUC());
        if (customMeasuredPowerMah != null) {
            if (totalPowerMah == null) {
                newTotalPowerMah = new double[customMeasuredPowerMah.length];
            } else if (totalPowerMah.length != customMeasuredPowerMah.length) {
                Slog.wtf(TAG, "Number of custom energy components is not the same for all apps: " + totalPowerMah.length + ", " + customMeasuredPowerMah.length);
                newTotalPowerMah = Arrays.copyOf(totalPowerMah, customMeasuredPowerMah.length);
            } else {
                newTotalPowerMah = totalPowerMah;
            }
            for (int i = 0; i < customMeasuredPowerMah.length; i++) {
                app.setConsumedPowerForCustomComponent(i + 1000, customMeasuredPowerMah[i]);
                if (!app.isVirtualUid()) {
                    newTotalPowerMah[i] = newTotalPowerMah[i] + customMeasuredPowerMah[i];
                }
            }
        }
        return newTotalPowerMah;
    }

    private double[] calculateMeasuredEnergiesMah(long[] measuredChargeUC) {
        if (measuredChargeUC == null) {
            return null;
        }
        double[] measuredEnergiesMah = new double[measuredChargeUC.length];
        for (int i = 0; i < measuredChargeUC.length; i++) {
            measuredEnergiesMah[i] = uCtoMah(measuredChargeUC[i]);
        }
        return measuredEnergiesMah;
    }
}
