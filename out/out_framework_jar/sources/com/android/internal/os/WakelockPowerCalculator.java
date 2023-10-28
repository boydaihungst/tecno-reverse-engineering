package com.android.internal.os;

import android.os.AggregateBatteryConsumer;
import android.os.BatteryStats;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.os.UidBatteryConsumer;
import android.util.ArrayMap;
import android.util.SparseArray;
/* loaded from: classes4.dex */
public class WakelockPowerCalculator extends PowerCalculator {
    private static final boolean DEBUG = false;
    private static final String TAG = "WakelockPowerCalculator";
    private final UsageBasedPowerEstimator mPowerEstimator;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public static class PowerAndDuration {
        public long durationMs;
        public double powerMah;

        private PowerAndDuration() {
        }
    }

    public WakelockPowerCalculator(PowerProfile profile) {
        this.mPowerEstimator = new UsageBasedPowerEstimator(profile.getAveragePower(PowerProfile.POWER_CPU_IDLE));
    }

    @Override // com.android.internal.os.PowerCalculator
    public boolean isPowerComponentSupported(int powerComponent) {
        return powerComponent == 12;
    }

    @Override // com.android.internal.os.PowerCalculator
    public void calculate(BatteryUsageStats.Builder builder, BatteryStats batteryStats, long rawRealtimeUs, long rawUptimeUs, BatteryUsageStatsQuery query) {
        PowerAndDuration result = new PowerAndDuration();
        UidBatteryConsumer.Builder osBatteryConsumer = null;
        double appPowerMah = 0.0d;
        SparseArray<UidBatteryConsumer.Builder> uidBatteryConsumerBuilders = builder.getUidBatteryConsumerBuilders();
        double osPowerMah = 0.0d;
        long osDurationMs = 0;
        long totalAppDurationMs = 0;
        for (int i = uidBatteryConsumerBuilders.size() - 1; i >= 0; i--) {
            UidBatteryConsumer.Builder app = uidBatteryConsumerBuilders.valueAt(i);
            calculateApp(result, app.getBatteryStatsUid(), rawRealtimeUs, 0);
            ((UidBatteryConsumer.Builder) app.setUsageDurationMillis(12, result.durationMs)).setConsumedPower(12, result.powerMah);
            if (!app.isVirtualUid()) {
                totalAppDurationMs += result.durationMs;
                appPowerMah += result.powerMah;
            }
            if (app.getUid() == 0) {
                long osDurationMs2 = result.durationMs;
                osDurationMs = osDurationMs2;
                osPowerMah = result.powerMah;
                osBatteryConsumer = app;
            }
        }
        double appPowerMah2 = appPowerMah;
        long totalAppDurationMs2 = totalAppDurationMs;
        long totalAppDurationMs3 = osDurationMs;
        calculateRemaining(result, batteryStats, rawRealtimeUs, rawUptimeUs, 0, osPowerMah, totalAppDurationMs3, totalAppDurationMs2);
        double remainingPowerMah = result.powerMah;
        if (osBatteryConsumer != null) {
            ((UidBatteryConsumer.Builder) osBatteryConsumer.setUsageDurationMillis(12, result.durationMs)).setConsumedPower(12, remainingPowerMah);
        }
        long wakeTimeMs = calculateWakeTimeMillis(batteryStats, rawRealtimeUs, rawUptimeUs);
        if (wakeTimeMs < 0) {
            wakeTimeMs = 0;
        }
        ((AggregateBatteryConsumer.Builder) builder.getAggregateBatteryConsumerBuilder(0).setUsageDurationMillis(12, wakeTimeMs)).setConsumedPower(12, appPowerMah2 + remainingPowerMah);
        ((AggregateBatteryConsumer.Builder) builder.getAggregateBatteryConsumerBuilder(1).setUsageDurationMillis(12, totalAppDurationMs2)).setConsumedPower(12, appPowerMah2);
    }

    private void calculateApp(PowerAndDuration result, BatteryStats.Uid u, long rawRealtimeUs, int statsType) {
        long wakeLockTimeUs = 0;
        ArrayMap<String, ? extends BatteryStats.Uid.Wakelock> wakelockStats = u.getWakelockStats();
        int wakelockStatsCount = wakelockStats.size();
        for (int i = 0; i < wakelockStatsCount; i++) {
            BatteryStats.Uid.Wakelock wakelock = wakelockStats.valueAt(i);
            BatteryStats.Timer timer = wakelock.getWakeTime(0);
            if (timer != null) {
                wakeLockTimeUs += timer.getTotalTimeLocked(rawRealtimeUs, statsType);
            }
        }
        result.durationMs = wakeLockTimeUs / 1000;
        result.powerMah = this.mPowerEstimator.calculatePower(result.durationMs);
    }

    private void calculateRemaining(PowerAndDuration result, BatteryStats stats, long rawRealtimeUs, long rawUptimeUs, int statsType, double osPowerMah, long osDurationMs, long totalAppDurationMs) {
        long wakeTimeMillis = calculateWakeTimeMillis(stats, rawRealtimeUs, rawUptimeUs) - totalAppDurationMs;
        if (wakeTimeMillis <= 0) {
            result.durationMs = 0L;
            result.powerMah = 0.0d;
            return;
        }
        double power = this.mPowerEstimator.calculatePower(wakeTimeMillis);
        result.durationMs = osDurationMs + wakeTimeMillis;
        result.powerMah = osPowerMah + power;
    }

    private long calculateWakeTimeMillis(BatteryStats batteryStats, long rawRealtimeUs, long rawUptimeUs) {
        long batteryUptimeUs = batteryStats.getBatteryUptime(rawUptimeUs);
        long screenOnTimeUs = batteryStats.getScreenOnTime(rawRealtimeUs, 0);
        return (batteryUptimeUs - screenOnTimeUs) / 1000;
    }
}
