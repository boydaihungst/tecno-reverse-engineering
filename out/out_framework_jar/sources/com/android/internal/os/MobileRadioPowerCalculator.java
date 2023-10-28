package com.android.internal.os;

import android.os.AggregateBatteryConsumer;
import android.os.BatteryConsumer;
import android.os.BatteryStats;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.os.UidBatteryConsumer;
import android.telephony.CellSignalStrength;
import android.util.SparseArray;
/* loaded from: classes4.dex */
public class MobileRadioPowerCalculator extends PowerCalculator {
    private static final boolean DEBUG = false;
    private static final String TAG = "MobRadioPowerCalculator";
    private final UsageBasedPowerEstimator mActivePowerEstimator;
    private final UsageBasedPowerEstimator[] mIdlePowerEstimators = new UsageBasedPowerEstimator[NUM_SIGNAL_STRENGTH_LEVELS];
    private final UsageBasedPowerEstimator mScanPowerEstimator;
    private static final int NUM_SIGNAL_STRENGTH_LEVELS = CellSignalStrength.getNumSignalStrengthLevels() + 1;
    private static final BatteryConsumer.Key[] UNINITIALIZED_KEYS = new BatteryConsumer.Key[0];

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public static class PowerAndDuration {
        public long durationMs;
        public long noCoverageDurationMs;
        public double remainingPowerMah;
        public long signalDurationMs;
        public long totalAppDurationMs;
        public double totalAppPowerMah;

        private PowerAndDuration() {
        }
    }

    public MobileRadioPowerCalculator(PowerProfile profile) {
        int i;
        double powerRadioActiveMa = profile.getAveragePowerOrDefault(PowerProfile.POWER_RADIO_ACTIVE, -1.0d);
        if (powerRadioActiveMa == -1.0d) {
            double sum = 0.0d + profile.getAveragePower(PowerProfile.POWER_MODEM_CONTROLLER_RX);
            int i2 = 0;
            while (true) {
                i = NUM_SIGNAL_STRENGTH_LEVELS;
                if (i2 >= i) {
                    break;
                }
                sum += profile.getAveragePower(PowerProfile.POWER_MODEM_CONTROLLER_TX, i2);
                i2++;
            }
            powerRadioActiveMa = sum / (i + 1);
        }
        this.mActivePowerEstimator = new UsageBasedPowerEstimator(powerRadioActiveMa);
        if (profile.getAveragePowerOrDefault(PowerProfile.POWER_RADIO_ON, -1.0d) != -1.0d) {
            for (int i3 = 0; i3 < NUM_SIGNAL_STRENGTH_LEVELS; i3++) {
                this.mIdlePowerEstimators[i3] = new UsageBasedPowerEstimator(profile.getAveragePower(PowerProfile.POWER_RADIO_ON, i3));
            }
        } else {
            double idle = profile.getAveragePower(PowerProfile.POWER_MODEM_CONTROLLER_IDLE);
            this.mIdlePowerEstimators[0] = new UsageBasedPowerEstimator((25.0d * idle) / 180.0d);
            for (int i4 = 1; i4 < NUM_SIGNAL_STRENGTH_LEVELS; i4++) {
                this.mIdlePowerEstimators[i4] = new UsageBasedPowerEstimator(Math.max(1.0d, idle / 256.0d));
            }
        }
        this.mScanPowerEstimator = new UsageBasedPowerEstimator(profile.getAveragePowerOrDefault(PowerProfile.POWER_RADIO_SCANNING, 0.0d));
    }

    @Override // com.android.internal.os.PowerCalculator
    public boolean isPowerComponentSupported(int powerComponent) {
        return powerComponent == 8;
    }

    @Override // com.android.internal.os.PowerCalculator
    public void calculate(BatteryUsageStats.Builder builder, BatteryStats batteryStats, long rawRealtimeUs, long rawUptimeUs, BatteryUsageStatsQuery query) {
        PowerAndDuration total = new PowerAndDuration();
        SparseArray<UidBatteryConsumer.Builder> uidBatteryConsumerBuilders = builder.getUidBatteryConsumerBuilders();
        BatteryConsumer.Key[] keys = UNINITIALIZED_KEYS;
        BatteryConsumer.Key[] keys2 = keys;
        for (int i = uidBatteryConsumerBuilders.size() - 1; i >= 0; i--) {
            UidBatteryConsumer.Builder app = uidBatteryConsumerBuilders.valueAt(i);
            BatteryStats.Uid uid = app.getBatteryStatsUid();
            if (keys2 == UNINITIALIZED_KEYS) {
                if (query.isProcessStateDataNeeded()) {
                    keys2 = app.getKeys(8);
                } else {
                    keys2 = null;
                }
            }
            calculateApp(app, uid, total, query, keys2);
        }
        long totalConsumptionUC = batteryStats.getMobileRadioMeasuredBatteryConsumptionUC();
        int powerModel = getPowerModel(totalConsumptionUC, query);
        calculateRemaining(total, powerModel, batteryStats, rawRealtimeUs, totalConsumptionUC);
        if (total.remainingPowerMah != 0.0d || total.totalAppPowerMah != 0.0d) {
            ((AggregateBatteryConsumer.Builder) builder.getAggregateBatteryConsumerBuilder(0).setUsageDurationMillis(8, total.durationMs)).setConsumedPower(8, total.remainingPowerMah + total.totalAppPowerMah, powerModel);
            ((AggregateBatteryConsumer.Builder) builder.getAggregateBatteryConsumerBuilder(1).setUsageDurationMillis(8, total.durationMs)).setConsumedPower(8, total.totalAppPowerMah, powerModel);
        }
    }

    private void calculateApp(UidBatteryConsumer.Builder app, BatteryStats.Uid u, PowerAndDuration total, BatteryUsageStatsQuery query, BatteryConsumer.Key[] keys) {
        int i;
        int i2;
        double powerMah;
        BatteryStats.Uid uid = u;
        long radioActiveDurationMs = calculateDuration(uid, 0);
        long consumptionUC = u.getMobileRadioMeasuredBatteryConsumptionUC();
        int powerModel = getPowerModel(consumptionUC, query);
        double powerMah2 = calculatePower(u, powerModel, radioActiveDurationMs, consumptionUC);
        if (!app.isVirtualUid()) {
            total.totalAppDurationMs += radioActiveDurationMs;
            total.totalAppPowerMah += powerMah2;
        }
        ((UidBatteryConsumer.Builder) app.setUsageDurationMillis(8, radioActiveDurationMs)).setConsumedPower(8, powerMah2, powerModel);
        if (query.isProcessStateDataNeeded() && keys != null) {
            int length = keys.length;
            int i3 = 0;
            while (i3 < length) {
                BatteryConsumer.Key key = keys[i3];
                int processState = key.processState;
                if (processState == 0) {
                    i = length;
                    i2 = i3;
                    powerMah = powerMah2;
                } else {
                    long durationInStateMs = uid.getMobileRadioActiveTimeInProcessState(processState) / 1000;
                    long consumptionInStateUc = uid.getMobileRadioMeasuredBatteryConsumptionUC(processState);
                    i = length;
                    i2 = i3;
                    powerMah = powerMah2;
                    double powerInStateMah = calculatePower(u, powerModel, durationInStateMs, consumptionInStateUc);
                    app.setConsumedPower(key, powerInStateMah, powerModel);
                }
                i3 = i2 + 1;
                uid = u;
                length = i;
                powerMah2 = powerMah;
            }
        }
    }

    private long calculateDuration(BatteryStats.Uid u, int statsType) {
        return u.getMobileRadioActiveTime(statsType) / 1000;
    }

    private double calculatePower(BatteryStats.Uid u, int powerModel, long radioActiveDurationMs, long measuredChargeUC) {
        if (powerModel == 2) {
            return uCtoMah(measuredChargeUC);
        }
        if (radioActiveDurationMs > 0) {
            return calcPowerFromRadioActiveDurationMah(radioActiveDurationMs);
        }
        return 0.0d;
    }

    private void calculateRemaining(PowerAndDuration total, int powerModel, BatteryStats batteryStats, long rawRealtimeUs, long totalConsumptionUC) {
        long signalTimeMs = 0;
        double powerMah = 0.0d;
        if (powerModel == 2) {
            powerMah = uCtoMah(totalConsumptionUC) - total.totalAppPowerMah;
            if (powerMah < 0.0d) {
                powerMah = 0.0d;
            }
        }
        for (int i = 0; i < NUM_SIGNAL_STRENGTH_LEVELS; i++) {
            long strengthTimeMs = batteryStats.getPhoneSignalStrengthTime(i, rawRealtimeUs, 0) / 1000;
            if (powerModel == 1) {
                double p = calcIdlePowerAtSignalStrengthMah(strengthTimeMs, i);
                powerMah += p;
            }
            signalTimeMs += strengthTimeMs;
            if (i == 0) {
                total.noCoverageDurationMs = strengthTimeMs;
            }
        }
        long scanningTimeMs = batteryStats.getPhoneSignalScanningTime(rawRealtimeUs, 0) / 1000;
        long radioActiveTimeMs = batteryStats.getMobileRadioActiveTime(rawRealtimeUs, 0) / 1000;
        long remainingActiveTimeMs = radioActiveTimeMs - total.totalAppDurationMs;
        if (powerModel == 1) {
            double p2 = calcScanTimePowerMah(scanningTimeMs);
            powerMah += p2;
            if (remainingActiveTimeMs > 0) {
                powerMah += calcPowerFromRadioActiveDurationMah(remainingActiveTimeMs);
            }
        }
        total.durationMs = radioActiveTimeMs;
        total.remainingPowerMah = powerMah;
        total.signalDurationMs = signalTimeMs;
    }

    public double calcPowerFromRadioActiveDurationMah(long radioActiveDurationMs) {
        return this.mActivePowerEstimator.calculatePower(radioActiveDurationMs);
    }

    public double calcIdlePowerAtSignalStrengthMah(long strengthTimeMs, int strengthLevel) {
        return this.mIdlePowerEstimators[strengthLevel].calculatePower(strengthTimeMs);
    }

    public double calcScanTimePowerMah(long scanningTimeMs) {
        return this.mScanPowerEstimator.calculatePower(scanningTimeMs);
    }
}
