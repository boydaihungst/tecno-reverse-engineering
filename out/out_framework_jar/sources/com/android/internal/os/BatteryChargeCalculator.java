package com.android.internal.os;

import android.os.BatteryStats;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
/* loaded from: classes4.dex */
public class BatteryChargeCalculator extends PowerCalculator {
    @Override // com.android.internal.os.PowerCalculator
    public boolean isPowerComponentSupported(int powerComponent) {
        return true;
    }

    @Override // com.android.internal.os.PowerCalculator
    public void calculate(BatteryUsageStats.Builder builder, BatteryStats batteryStats, long rawRealtimeUs, long rawUptimeUs, BatteryUsageStatsQuery query) {
        builder.setDischargePercentage(batteryStats.getDischargeAmount(0));
        int batteryCapacityMah = batteryStats.getLearnedBatteryCapacity() / 1000;
        if (batteryCapacityMah <= 0 && (batteryCapacityMah = batteryStats.getMinLearnedBatteryCapacity() / 1000) <= 0) {
            batteryCapacityMah = batteryStats.getEstimatedBatteryCapacity();
        }
        builder.setBatteryCapacity(batteryCapacityMah);
        double dischargedPowerLowerBoundMah = (batteryStats.getLowDischargeAmountSinceCharge() * batteryCapacityMah) / 100.0d;
        double dischargedPowerUpperBoundMah = (batteryStats.getHighDischargeAmountSinceCharge() * batteryCapacityMah) / 100.0d;
        builder.setDischargePercentage(batteryStats.getDischargeAmount(0)).setDischargedPowerRange(dischargedPowerLowerBoundMah, dischargedPowerUpperBoundMah).setDischargeDurationMs(batteryStats.getBatteryRealtime(rawRealtimeUs) / 1000);
        long batteryTimeRemainingMs = batteryStats.computeBatteryTimeRemaining(rawRealtimeUs);
        if (batteryTimeRemainingMs != -1) {
            builder.setBatteryTimeRemainingMs(batteryTimeRemainingMs / 1000);
        }
        long chargeTimeRemainingMs = batteryStats.computeChargeTimeRemaining(rawRealtimeUs);
        if (chargeTimeRemainingMs != -1) {
            builder.setChargeTimeRemainingMs(chargeTimeRemainingMs / 1000);
        }
        long dischargeMah = batteryStats.getUahDischarge(0) / 1000;
        if (dischargeMah == 0) {
            dischargeMah = (long) (((dischargedPowerLowerBoundMah + dischargedPowerUpperBoundMah) / 2.0d) + 0.5d);
        }
        builder.getAggregateBatteryConsumerBuilder(0).setConsumedPower(dischargeMah);
    }
}
