package com.android.internal.os;

import android.os.AggregateBatteryConsumer;
import android.os.BatteryStats;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.os.UidBatteryConsumer;
import android.util.SparseArray;
/* loaded from: classes4.dex */
public class VideoPowerCalculator extends PowerCalculator {
    private final UsageBasedPowerEstimator mPowerEstimator;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public static class PowerAndDuration {
        public long durationMs;
        public double powerMah;

        private PowerAndDuration() {
        }
    }

    public VideoPowerCalculator(PowerProfile powerProfile) {
        this.mPowerEstimator = new UsageBasedPowerEstimator(powerProfile.getAveragePower("video"));
    }

    @Override // com.android.internal.os.PowerCalculator
    public boolean isPowerComponentSupported(int powerComponent) {
        return powerComponent == 5;
    }

    @Override // com.android.internal.os.PowerCalculator
    public void calculate(BatteryUsageStats.Builder builder, BatteryStats batteryStats, long rawRealtimeUs, long rawUptimeUs, BatteryUsageStatsQuery query) {
        PowerAndDuration total = new PowerAndDuration();
        SparseArray<UidBatteryConsumer.Builder> uidBatteryConsumerBuilders = builder.getUidBatteryConsumerBuilders();
        for (int i = uidBatteryConsumerBuilders.size() - 1; i >= 0; i--) {
            UidBatteryConsumer.Builder app = uidBatteryConsumerBuilders.valueAt(i);
            calculateApp(app, total, app.getBatteryStatsUid(), rawRealtimeUs);
        }
        ((AggregateBatteryConsumer.Builder) builder.getAggregateBatteryConsumerBuilder(0).setUsageDurationMillis(5, total.durationMs)).setConsumedPower(5, total.powerMah);
        ((AggregateBatteryConsumer.Builder) builder.getAggregateBatteryConsumerBuilder(1).setUsageDurationMillis(5, total.durationMs)).setConsumedPower(5, total.powerMah);
    }

    private void calculateApp(UidBatteryConsumer.Builder app, PowerAndDuration total, BatteryStats.Uid u, long rawRealtimeUs) {
        long durationMs = this.mPowerEstimator.calculateDuration(u.getVideoTurnedOnTimer(), rawRealtimeUs, 0);
        double powerMah = this.mPowerEstimator.calculatePower(durationMs);
        ((UidBatteryConsumer.Builder) app.setUsageDurationMillis(5, durationMs)).setConsumedPower(5, powerMah);
        if (!app.isVirtualUid()) {
            total.durationMs += durationMs;
            total.powerMah += powerMah;
        }
    }
}
