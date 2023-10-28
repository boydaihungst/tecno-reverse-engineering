package com.android.internal.os;

import android.os.AggregateBatteryConsumer;
import android.os.BatteryStats;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
/* loaded from: classes4.dex */
public class AmbientDisplayPowerCalculator extends PowerCalculator {
    private final UsageBasedPowerEstimator[] mPowerEstimators;

    @Override // com.android.internal.os.PowerCalculator
    public boolean isPowerComponentSupported(int powerComponent) {
        return powerComponent == 15;
    }

    public AmbientDisplayPowerCalculator(PowerProfile powerProfile) {
        int numDisplays = powerProfile.getNumDisplays();
        this.mPowerEstimators = new UsageBasedPowerEstimator[numDisplays];
        for (int display = 0; display < numDisplays; display++) {
            this.mPowerEstimators[display] = new UsageBasedPowerEstimator(powerProfile.getAveragePowerForOrdinal(PowerProfile.POWER_GROUP_DISPLAY_AMBIENT, display));
        }
    }

    @Override // com.android.internal.os.PowerCalculator
    public void calculate(BatteryUsageStats.Builder builder, BatteryStats batteryStats, long rawRealtimeUs, long rawUptimeUs, BatteryUsageStatsQuery query) {
        long measuredEnergyUC = batteryStats.getScreenDozeMeasuredBatteryConsumptionUC();
        int powerModel = getPowerModel(measuredEnergyUC, query);
        long durationMs = calculateDuration(batteryStats, rawRealtimeUs, 0);
        double powerMah = calculateTotalPower(powerModel, batteryStats, rawRealtimeUs, measuredEnergyUC);
        ((AggregateBatteryConsumer.Builder) builder.getAggregateBatteryConsumerBuilder(0).setUsageDurationMillis(15, durationMs)).setConsumedPower(15, powerMah, powerModel);
    }

    private long calculateDuration(BatteryStats batteryStats, long rawRealtimeUs, int statsType) {
        return batteryStats.getScreenDozeTime(rawRealtimeUs, statsType) / 1000;
    }

    private double calculateTotalPower(int powerModel, BatteryStats batteryStats, long rawRealtimeUs, long consumptionUC) {
        switch (powerModel) {
            case 2:
                return uCtoMah(consumptionUC);
            default:
                return calculateEstimatedPower(batteryStats, rawRealtimeUs);
        }
    }

    private double calculateEstimatedPower(BatteryStats batteryStats, long rawRealtimeUs) {
        int numDisplays = this.mPowerEstimators.length;
        double power = 0.0d;
        for (int display = 0; display < numDisplays; display++) {
            long dozeTime = batteryStats.getDisplayScreenDozeTime(display, rawRealtimeUs) / 1000;
            power += this.mPowerEstimators[display].calculatePower(dozeTime);
        }
        return power;
    }
}
