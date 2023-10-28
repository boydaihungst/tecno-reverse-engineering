package com.android.internal.os;

import android.os.AggregateBatteryConsumer;
import android.os.BatteryStats;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.os.UidBatteryConsumer;
/* loaded from: classes4.dex */
public class CameraPowerCalculator extends PowerCalculator {
    private final UsageBasedPowerEstimator mPowerEstimator;

    public CameraPowerCalculator(PowerProfile profile) {
        this.mPowerEstimator = new UsageBasedPowerEstimator(profile.getAveragePower(PowerProfile.POWER_CAMERA));
    }

    @Override // com.android.internal.os.PowerCalculator
    public boolean isPowerComponentSupported(int powerComponent) {
        return powerComponent == 3;
    }

    @Override // com.android.internal.os.PowerCalculator
    public void calculate(BatteryUsageStats.Builder builder, BatteryStats batteryStats, long rawRealtimeUs, long rawUptimeUs, BatteryUsageStatsQuery query) {
        super.calculate(builder, batteryStats, rawRealtimeUs, rawUptimeUs, query);
        long durationMs = batteryStats.getCameraOnTime(rawRealtimeUs, 0) / 1000;
        double powerMah = this.mPowerEstimator.calculatePower(durationMs);
        ((AggregateBatteryConsumer.Builder) builder.getAggregateBatteryConsumerBuilder(0).setUsageDurationMillis(3, durationMs)).setConsumedPower(3, powerMah);
        ((AggregateBatteryConsumer.Builder) builder.getAggregateBatteryConsumerBuilder(1).setUsageDurationMillis(3, durationMs)).setConsumedPower(3, powerMah);
    }

    @Override // com.android.internal.os.PowerCalculator
    protected void calculateApp(UidBatteryConsumer.Builder app, BatteryStats.Uid u, long rawRealtimeUs, long rawUptimeUs, BatteryUsageStatsQuery query) {
        long durationMs = this.mPowerEstimator.calculateDuration(u.getCameraTurnedOnTimer(), rawRealtimeUs, 0);
        double powerMah = this.mPowerEstimator.calculatePower(durationMs);
        ((UidBatteryConsumer.Builder) app.setUsageDurationMillis(3, durationMs)).setConsumedPower(3, powerMah);
    }
}
