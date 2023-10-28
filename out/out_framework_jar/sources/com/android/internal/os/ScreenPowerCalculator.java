package com.android.internal.os;

import android.os.AggregateBatteryConsumer;
import android.os.BatteryStats;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.os.UidBatteryConsumer;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseLongArray;
/* loaded from: classes4.dex */
public class ScreenPowerCalculator extends PowerCalculator {
    private static final boolean DEBUG = false;
    public static final long MIN_ACTIVE_TIME_FOR_SMEARING = 600000;
    private static final String TAG = "ScreenPowerCalculator";
    private final UsageBasedPowerEstimator[] mScreenFullPowerEstimators;
    private final UsageBasedPowerEstimator[] mScreenOnPowerEstimators;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public static class PowerAndDuration {
        public long durationMs;
        public double powerMah;

        private PowerAndDuration() {
        }
    }

    public ScreenPowerCalculator(PowerProfile powerProfile) {
        int numDisplays = powerProfile.getNumDisplays();
        this.mScreenOnPowerEstimators = new UsageBasedPowerEstimator[numDisplays];
        this.mScreenFullPowerEstimators = new UsageBasedPowerEstimator[numDisplays];
        for (int display = 0; display < numDisplays; display++) {
            this.mScreenOnPowerEstimators[display] = new UsageBasedPowerEstimator(powerProfile.getAveragePowerForOrdinal(PowerProfile.POWER_GROUP_DISPLAY_SCREEN_ON, display));
            this.mScreenFullPowerEstimators[display] = new UsageBasedPowerEstimator(powerProfile.getAveragePowerForOrdinal(PowerProfile.POWER_GROUP_DISPLAY_SCREEN_FULL, display));
        }
    }

    @Override // com.android.internal.os.PowerCalculator
    public boolean isPowerComponentSupported(int powerComponent) {
        return powerComponent == 0;
    }

    @Override // com.android.internal.os.PowerCalculator
    public void calculate(BatteryUsageStats.Builder builder, BatteryStats batteryStats, long rawRealtimeUs, long rawUptimeUs, BatteryUsageStatsQuery query) {
        ScreenPowerCalculator screenPowerCalculator = this;
        long j = rawRealtimeUs;
        PowerAndDuration totalPowerAndDuration = new PowerAndDuration();
        long consumptionUC = batteryStats.getScreenOnMeasuredBatteryConsumptionUC();
        int powerModel = getPowerModel(consumptionUC, query);
        calculateTotalDurationAndPower(totalPowerAndDuration, powerModel, batteryStats, rawRealtimeUs, 0, consumptionUC);
        double totalAppPower = 0.0d;
        long totalAppDuration = 0;
        SparseArray<UidBatteryConsumer.Builder> uidBatteryConsumerBuilders = builder.getUidBatteryConsumerBuilders();
        switch (powerModel) {
            case 2:
                PowerAndDuration appPowerAndDuration = new PowerAndDuration();
                int i = uidBatteryConsumerBuilders.size() - 1;
                while (i >= 0) {
                    UidBatteryConsumer.Builder app = uidBatteryConsumerBuilders.valueAt(i);
                    screenPowerCalculator.calculateAppUsingMeasuredEnergy(appPowerAndDuration, app.getBatteryStatsUid(), j);
                    ((UidBatteryConsumer.Builder) app.setUsageDurationMillis(0, appPowerAndDuration.durationMs)).setConsumedPower(0, appPowerAndDuration.powerMah, powerModel);
                    if (!app.isVirtualUid()) {
                        totalAppPower += appPowerAndDuration.powerMah;
                        totalAppDuration += appPowerAndDuration.durationMs;
                    }
                    i--;
                    screenPowerCalculator = this;
                    j = rawRealtimeUs;
                }
                break;
            default:
                smearScreenBatteryDrain(uidBatteryConsumerBuilders, totalPowerAndDuration, rawRealtimeUs);
                totalAppPower = totalPowerAndDuration.powerMah;
                totalAppDuration = totalPowerAndDuration.durationMs;
                break;
        }
        ((AggregateBatteryConsumer.Builder) builder.getAggregateBatteryConsumerBuilder(0).setConsumedPower(0, Math.max(totalPowerAndDuration.powerMah, totalAppPower), powerModel)).setUsageDurationMillis(0, totalPowerAndDuration.durationMs);
        ((AggregateBatteryConsumer.Builder) builder.getAggregateBatteryConsumerBuilder(1).setConsumedPower(0, totalAppPower, powerModel)).setUsageDurationMillis(0, totalAppDuration);
    }

    private void calculateTotalDurationAndPower(PowerAndDuration totalPowerAndDuration, int powerModel, BatteryStats batteryStats, long rawRealtimeUs, int statsType, long consumptionUC) {
        totalPowerAndDuration.durationMs = calculateDuration(batteryStats, rawRealtimeUs, statsType);
        switch (powerModel) {
            case 2:
                totalPowerAndDuration.powerMah = uCtoMah(consumptionUC);
                return;
            default:
                totalPowerAndDuration.powerMah = calculateTotalPowerFromBrightness(batteryStats, rawRealtimeUs);
                return;
        }
    }

    private void calculateAppUsingMeasuredEnergy(PowerAndDuration appPowerAndDuration, BatteryStats.Uid u, long rawRealtimeUs) {
        appPowerAndDuration.durationMs = getProcessForegroundTimeMs(u, rawRealtimeUs);
        long chargeUC = u.getScreenOnMeasuredBatteryConsumptionUC();
        if (chargeUC < 0) {
            Slog.wtf(TAG, "Screen energy not supported, so calculateApp shouldn't de called");
            appPowerAndDuration.powerMah = 0.0d;
            return;
        }
        appPowerAndDuration.powerMah = uCtoMah(chargeUC);
    }

    private long calculateDuration(BatteryStats batteryStats, long rawRealtimeUs, int statsType) {
        return batteryStats.getScreenOnTime(rawRealtimeUs, statsType) / 1000;
    }

    private double calculateTotalPowerFromBrightness(BatteryStats batteryStats, long rawRealtimeUs) {
        int numDisplays = this.mScreenOnPowerEstimators.length;
        double power = 0.0d;
        for (int display = 0; display < numDisplays; display++) {
            long j = 1000;
            long displayTime = batteryStats.getDisplayScreenOnTime(display, rawRealtimeUs) / 1000;
            power += this.mScreenOnPowerEstimators[display].calculatePower(displayTime);
            int bin = 0;
            while (bin < 5) {
                long brightnessTime = batteryStats.getDisplayScreenBrightnessTime(display, bin, rawRealtimeUs) / j;
                double binPowerMah = (this.mScreenFullPowerEstimators[display].calculatePower(brightnessTime) * (bin + 0.5f)) / 5.0d;
                power += binPowerMah;
                bin++;
                j = 1000;
            }
        }
        return power;
    }

    private void smearScreenBatteryDrain(SparseArray<UidBatteryConsumer.Builder> uidBatteryConsumerBuilders, PowerAndDuration totalPowerAndDuration, long rawRealtimeUs) {
        long totalActivityTimeMs = 0;
        SparseLongArray activityTimeArray = new SparseLongArray();
        for (int i = uidBatteryConsumerBuilders.size() - 1; i >= 0; i--) {
            UidBatteryConsumer.Builder app = uidBatteryConsumerBuilders.valueAt(i);
            BatteryStats.Uid uid = app.getBatteryStatsUid();
            long timeMs = getProcessForegroundTimeMs(uid, rawRealtimeUs);
            activityTimeArray.put(uid.getUid(), timeMs);
            if (!app.isVirtualUid()) {
                totalActivityTimeMs += timeMs;
            }
        }
        if (totalActivityTimeMs >= MIN_ACTIVE_TIME_FOR_SMEARING) {
            double totalScreenPowerMah = totalPowerAndDuration.powerMah;
            int i2 = uidBatteryConsumerBuilders.size() - 1;
            while (i2 >= 0) {
                UidBatteryConsumer.Builder app2 = uidBatteryConsumerBuilders.valueAt(i2);
                long durationMs = activityTimeArray.get(app2.getUid(), 0L);
                SparseLongArray activityTimeArray2 = activityTimeArray;
                double powerMah = (durationMs * totalScreenPowerMah) / totalActivityTimeMs;
                ((UidBatteryConsumer.Builder) app2.setUsageDurationMillis(0, durationMs)).setConsumedPower(0, powerMah, 1);
                i2--;
                activityTimeArray = activityTimeArray2;
                totalScreenPowerMah = totalScreenPowerMah;
            }
        }
    }

    public long getProcessForegroundTimeMs(BatteryStats.Uid uid, long rawRealTimeUs) {
        int[] foregroundTypes = {0};
        long timeUs = 0;
        for (int type : foregroundTypes) {
            long localTime = uid.getProcessStateTime(type, rawRealTimeUs, 0);
            timeUs += localTime;
        }
        return Math.min(timeUs, getForegroundActivityTotalTimeUs(uid, rawRealTimeUs)) / 1000;
    }

    public long getForegroundActivityTotalTimeUs(BatteryStats.Uid uid, long rawRealtimeUs) {
        BatteryStats.Timer timer = uid.getForegroundActivityTimer();
        if (timer == null) {
            return 0L;
        }
        return timer.getTotalTimeLocked(rawRealtimeUs, 0);
    }
}
