package com.android.internal.os;

import android.os.AggregateBatteryConsumer;
import android.os.BatteryConsumer;
import android.os.BatteryStats;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.os.UidBatteryConsumer;
import android.util.SparseArray;
import java.util.Arrays;
/* loaded from: classes4.dex */
public class BluetoothPowerCalculator extends PowerCalculator {
    private static final boolean DEBUG = false;
    private static final String TAG = "BluetoothPowerCalc";
    private static final BatteryConsumer.Key[] UNINITIALIZED_KEYS = new BatteryConsumer.Key[0];
    private final boolean mHasBluetoothPowerController;
    private final double mIdleMa;
    private final double mRxMa;
    private final double mTxMa;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public static class PowerAndDuration {
        public long durationMs;
        public BatteryConsumer.Key[] keys;
        public double powerMah;
        public double[] powerPerKeyMah;
        public long totalDurationMs;
        public double totalPowerMah;

        private PowerAndDuration() {
        }
    }

    public BluetoothPowerCalculator(PowerProfile profile) {
        double averagePower = profile.getAveragePower(PowerProfile.POWER_BLUETOOTH_CONTROLLER_IDLE);
        this.mIdleMa = averagePower;
        double averagePower2 = profile.getAveragePower(PowerProfile.POWER_BLUETOOTH_CONTROLLER_RX);
        this.mRxMa = averagePower2;
        double averagePower3 = profile.getAveragePower(PowerProfile.POWER_BLUETOOTH_CONTROLLER_TX);
        this.mTxMa = averagePower3;
        this.mHasBluetoothPowerController = (averagePower == 0.0d || averagePower2 == 0.0d || averagePower3 == 0.0d) ? false : true;
    }

    @Override // com.android.internal.os.PowerCalculator
    public boolean isPowerComponentSupported(int powerComponent) {
        return powerComponent == 2;
    }

    @Override // com.android.internal.os.PowerCalculator
    public void calculate(BatteryUsageStats.Builder builder, BatteryStats batteryStats, long rawRealtimeUs, long rawUptimeUs, BatteryUsageStatsQuery query) {
        if (!batteryStats.hasBluetoothActivityReporting()) {
            return;
        }
        BatteryConsumer.Key[] keys = UNINITIALIZED_KEYS;
        PowerAndDuration powerAndDuration = new PowerAndDuration();
        SparseArray<UidBatteryConsumer.Builder> uidBatteryConsumerBuilders = builder.getUidBatteryConsumerBuilders();
        for (int i = uidBatteryConsumerBuilders.size() - 1; i >= 0; i--) {
            UidBatteryConsumer.Builder app = uidBatteryConsumerBuilders.valueAt(i);
            if (keys == UNINITIALIZED_KEYS) {
                if (query.isProcessStateDataNeeded()) {
                    keys = app.getKeys(2);
                    powerAndDuration.keys = keys;
                    powerAndDuration.powerPerKeyMah = new double[keys.length];
                } else {
                    keys = null;
                }
            }
            calculateApp(app, powerAndDuration, query);
        }
        long measuredChargeUC = batteryStats.getBluetoothMeasuredBatteryConsumptionUC();
        int powerModel = getPowerModel(measuredChargeUC, query);
        BatteryStats.ControllerActivityCounter activityCounter = batteryStats.getBluetoothControllerActivity();
        calculatePowerAndDuration(null, powerModel, measuredChargeUC, activityCounter, query.shouldForceUsePowerProfileModel(), powerAndDuration);
        Math.max(0L, powerAndDuration.durationMs - powerAndDuration.totalDurationMs);
        ((AggregateBatteryConsumer.Builder) builder.getAggregateBatteryConsumerBuilder(0).setUsageDurationMillis(2, powerAndDuration.durationMs)).setConsumedPower(2, Math.max(powerAndDuration.powerMah, powerAndDuration.totalPowerMah), powerModel);
        ((AggregateBatteryConsumer.Builder) builder.getAggregateBatteryConsumerBuilder(1).setUsageDurationMillis(2, powerAndDuration.totalDurationMs)).setConsumedPower(2, powerAndDuration.totalPowerMah, powerModel);
    }

    private void calculateApp(UidBatteryConsumer.Builder app, PowerAndDuration powerAndDuration, BatteryUsageStatsQuery query) {
        long measuredChargeUC = app.getBatteryStatsUid().getBluetoothMeasuredBatteryConsumptionUC();
        int powerModel = getPowerModel(measuredChargeUC, query);
        BatteryStats.ControllerActivityCounter activityCounter = app.getBatteryStatsUid().getBluetoothControllerActivity();
        calculatePowerAndDuration(app.getBatteryStatsUid(), powerModel, measuredChargeUC, activityCounter, query.shouldForceUsePowerProfileModel(), powerAndDuration);
        ((UidBatteryConsumer.Builder) app.setUsageDurationMillis(2, powerAndDuration.durationMs)).setConsumedPower(2, powerAndDuration.powerMah, powerModel);
        if (!app.isVirtualUid()) {
            powerAndDuration.totalDurationMs += powerAndDuration.durationMs;
            powerAndDuration.totalPowerMah += powerAndDuration.powerMah;
        }
        if (query.isProcessStateDataNeeded() && powerAndDuration.keys != null) {
            for (int j = 0; j < powerAndDuration.keys.length; j++) {
                BatteryConsumer.Key key = powerAndDuration.keys[j];
                int processState = key.processState;
                if (processState != 0) {
                    app.setConsumedPower(key, powerAndDuration.powerPerKeyMah[j], powerModel);
                }
            }
        }
    }

    private void calculatePowerAndDuration(BatteryStats.Uid uid, int powerModel, long measuredChargeUC, BatteryStats.ControllerActivityCounter counter, boolean ignoreReportedPower, PowerAndDuration powerAndDuration) {
        if (counter == null) {
            powerAndDuration.durationMs = 0L;
            powerAndDuration.powerMah = 0.0d;
            if (powerAndDuration.powerPerKeyMah != null) {
                Arrays.fill(powerAndDuration.powerPerKeyMah, 0.0d);
                return;
            }
            return;
        }
        BatteryStats.LongCounter idleTimeCounter = counter.getIdleTimeCounter();
        BatteryStats.LongCounter rxTimeCounter = counter.getRxTimeCounter();
        BatteryStats.LongCounter txTimeCounter = counter.getTxTimeCounters()[0];
        long idleTimeMs = idleTimeCounter.getCountLocked(0);
        long rxTimeMs = rxTimeCounter.getCountLocked(0);
        long txTimeMs = txTimeCounter.getCountLocked(0);
        powerAndDuration.durationMs = idleTimeMs + rxTimeMs + txTimeMs;
        if (powerModel == 2) {
            powerAndDuration.powerMah = uCtoMah(measuredChargeUC);
            if (uid != null && powerAndDuration.keys != null) {
                for (int i = 0; i < powerAndDuration.keys.length; i++) {
                    BatteryConsumer.Key key = powerAndDuration.keys[i];
                    int processState = key.processState;
                    if (processState != 0) {
                        powerAndDuration.powerPerKeyMah[i] = uCtoMah(uid.getBluetoothMeasuredBatteryConsumptionUC(processState));
                    }
                }
                return;
            }
            return;
        }
        if (!ignoreReportedPower) {
            double powerMah = counter.getPowerCounter().getCountLocked(0) / 3600000.0d;
            if (powerMah != 0.0d) {
                powerAndDuration.powerMah = powerMah;
                if (powerAndDuration.powerPerKeyMah != null) {
                    Arrays.fill(powerAndDuration.powerPerKeyMah, 0.0d);
                    return;
                }
                return;
            }
        }
        if (this.mHasBluetoothPowerController) {
            powerAndDuration.powerMah = calculatePowerMah(rxTimeMs, txTimeMs, idleTimeMs);
            if (powerAndDuration.keys != null) {
                for (int i2 = 0; i2 < powerAndDuration.keys.length; i2++) {
                    BatteryConsumer.Key key2 = powerAndDuration.keys[i2];
                    int processState2 = key2.processState;
                    if (processState2 != 0) {
                        powerAndDuration.powerPerKeyMah[i2] = calculatePowerMah(rxTimeCounter.getCountForProcessState(processState2), txTimeCounter.getCountForProcessState(processState2), idleTimeCounter.getCountForProcessState(processState2));
                    }
                }
                return;
            }
            return;
        }
        powerAndDuration.powerMah = 0.0d;
        if (powerAndDuration.powerPerKeyMah != null) {
            Arrays.fill(powerAndDuration.powerPerKeyMah, 0.0d);
        }
    }

    public double calculatePowerMah(long rxTimeMs, long txTimeMs, long idleTimeMs) {
        return (((idleTimeMs * this.mIdleMa) + (rxTimeMs * this.mRxMa)) + (txTimeMs * this.mTxMa)) / 3600000.0d;
    }
}
