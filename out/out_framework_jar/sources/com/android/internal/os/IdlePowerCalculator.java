package com.android.internal.os;

import android.os.AggregateBatteryConsumer;
import android.os.BatteryStats;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
/* loaded from: classes4.dex */
public class IdlePowerCalculator extends PowerCalculator {
    private static final boolean DEBUG = false;
    private static final String TAG = "IdlePowerCalculator";
    private final double mAveragePowerCpuIdleMahPerUs;
    private final double mAveragePowerCpuSuspendMahPerUs;
    public long mDurationMs;
    public double mPowerMah;

    public IdlePowerCalculator(PowerProfile powerProfile) {
        this.mAveragePowerCpuSuspendMahPerUs = powerProfile.getAveragePower(PowerProfile.POWER_CPU_SUSPEND) / 3.6E9d;
        this.mAveragePowerCpuIdleMahPerUs = powerProfile.getAveragePower(PowerProfile.POWER_CPU_IDLE) / 3.6E9d;
    }

    @Override // com.android.internal.os.PowerCalculator
    public boolean isPowerComponentSupported(int powerComponent) {
        return powerComponent == 16;
    }

    @Override // com.android.internal.os.PowerCalculator
    public void calculate(BatteryUsageStats.Builder builder, BatteryStats batteryStats, long rawRealtimeUs, long rawUptimeUs, BatteryUsageStatsQuery query) {
        calculatePowerAndDuration(batteryStats, rawRealtimeUs, rawUptimeUs, 0);
        if (this.mPowerMah != 0.0d) {
            ((AggregateBatteryConsumer.Builder) builder.getAggregateBatteryConsumerBuilder(0).setConsumedPower(16, this.mPowerMah)).setUsageDurationMillis(16, this.mDurationMs);
        }
    }

    private void calculatePowerAndDuration(BatteryStats batteryStats, long rawRealtimeUs, long rawUptimeUs, int statsType) {
        long batteryRealtimeUs = batteryStats.computeBatteryRealtime(rawRealtimeUs, statsType);
        long batteryUptimeUs = batteryStats.computeBatteryUptime(rawUptimeUs, statsType);
        double suspendPowerMah = batteryRealtimeUs * this.mAveragePowerCpuSuspendMahPerUs;
        double idlePowerMah = batteryUptimeUs * this.mAveragePowerCpuIdleMahPerUs;
        this.mPowerMah = suspendPowerMah + idlePowerMah;
        this.mDurationMs = batteryRealtimeUs / 1000;
    }
}
