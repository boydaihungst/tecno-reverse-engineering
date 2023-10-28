package com.android.internal.os;

import android.os.AggregateBatteryConsumer;
import android.os.BatteryStats;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
/* loaded from: classes4.dex */
public class PhonePowerCalculator extends PowerCalculator {
    private final UsageBasedPowerEstimator mPowerEstimator;

    public PhonePowerCalculator(PowerProfile powerProfile) {
        this.mPowerEstimator = new UsageBasedPowerEstimator(powerProfile.getAveragePower(PowerProfile.POWER_RADIO_ACTIVE));
    }

    @Override // com.android.internal.os.PowerCalculator
    public boolean isPowerComponentSupported(int powerComponent) {
        return powerComponent == 14;
    }

    @Override // com.android.internal.os.PowerCalculator
    public void calculate(BatteryUsageStats.Builder builder, BatteryStats batteryStats, long rawRealtimeUs, long rawUptimeUs, BatteryUsageStatsQuery query) {
        long phoneOnTimeMs = batteryStats.getPhoneOnTime(rawRealtimeUs, 0) / 1000;
        double phoneOnPower = this.mPowerEstimator.calculatePower(phoneOnTimeMs);
        if (phoneOnPower != 0.0d) {
            ((AggregateBatteryConsumer.Builder) builder.getAggregateBatteryConsumerBuilder(0).setConsumedPower(14, phoneOnPower)).setUsageDurationMillis(14, phoneOnTimeMs);
        }
    }
}
