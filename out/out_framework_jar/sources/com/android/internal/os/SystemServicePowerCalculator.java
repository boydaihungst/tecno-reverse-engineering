package com.android.internal.os;

import android.os.BatteryStats;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.os.UidBatteryConsumer;
import android.util.SparseArray;
/* loaded from: classes4.dex */
public class SystemServicePowerCalculator extends PowerCalculator {
    private static final boolean DEBUG = false;
    private static final String TAG = "SystemServicePowerCalc";
    private final CpuPowerCalculator mCpuPowerCalculator;
    private final UsageBasedPowerEstimator[] mPowerEstimators;

    public SystemServicePowerCalculator(PowerProfile powerProfile) {
        this.mCpuPowerCalculator = new CpuPowerCalculator(powerProfile);
        int numFreqs = 0;
        int numCpuClusters = powerProfile.getNumCpuClusters();
        for (int cluster = 0; cluster < numCpuClusters; cluster++) {
            numFreqs += powerProfile.getNumSpeedStepsInCpuCluster(cluster);
        }
        this.mPowerEstimators = new UsageBasedPowerEstimator[numFreqs];
        int index = 0;
        for (int cluster2 = 0; cluster2 < numCpuClusters; cluster2++) {
            int numSpeeds = powerProfile.getNumSpeedStepsInCpuCluster(cluster2);
            int speed = 0;
            while (speed < numSpeeds) {
                this.mPowerEstimators[index] = new UsageBasedPowerEstimator(powerProfile.getAveragePowerForCpuCore(cluster2, speed));
                speed++;
                index++;
            }
        }
    }

    @Override // com.android.internal.os.PowerCalculator
    public boolean isPowerComponentSupported(int powerComponent) {
        return powerComponent == 7;
    }

    @Override // com.android.internal.os.PowerCalculator
    public void calculate(BatteryUsageStats.Builder builder, BatteryStats batteryStats, long rawRealtimeUs, long rawUptimeUs, BatteryUsageStatsQuery query) {
        double systemServicePowerMah;
        BatteryStats.Uid systemUid;
        BatteryStats.Uid systemUid2 = batteryStats.getUidStats().get(1000);
        if (systemUid2 == null) {
            return;
        }
        long consumptionUC = systemUid2.getCpuMeasuredBatteryConsumptionUC();
        int powerModel = getPowerModel(consumptionUC, query);
        if (powerModel == 2) {
            systemServicePowerMah = calculatePowerUsingMeasuredConsumption(batteryStats, systemUid2, consumptionUC);
        } else {
            systemServicePowerMah = calculatePowerUsingPowerProfile(batteryStats);
        }
        SparseArray<UidBatteryConsumer.Builder> uidBatteryConsumerBuilders = builder.getUidBatteryConsumerBuilders();
        UidBatteryConsumer.Builder systemServerConsumer = uidBatteryConsumerBuilders.get(1000);
        if (systemServerConsumer != null) {
            systemServicePowerMah = Math.min(systemServicePowerMah, systemServerConsumer.getTotalPower());
            systemServerConsumer.setConsumedPower(17, -systemServicePowerMah, powerModel);
        }
        int i = uidBatteryConsumerBuilders.size() - 1;
        while (i >= 0) {
            UidBatteryConsumer.Builder app = uidBatteryConsumerBuilders.valueAt(i);
            if (app == systemServerConsumer) {
                systemUid = systemUid2;
            } else {
                BatteryStats.Uid uid = app.getBatteryStatsUid();
                systemUid = systemUid2;
                app.setConsumedPower(7, systemServicePowerMah * uid.getProportionalSystemServiceUsage(), powerModel);
            }
            i--;
            systemUid2 = systemUid;
        }
        builder.getAggregateBatteryConsumerBuilder(0).setConsumedPower(7, systemServicePowerMah);
        builder.getAggregateBatteryConsumerBuilder(1).setConsumedPower(7, systemServicePowerMah);
    }

    private double calculatePowerUsingMeasuredConsumption(BatteryStats batteryStats, BatteryStats.Uid systemUid, long consumptionUC) {
        double systemServiceModeledPowerMah = calculatePowerUsingPowerProfile(batteryStats);
        double systemUidModeledPowerMah = this.mCpuPowerCalculator.calculateUidModeledPowerMah(systemUid, 0);
        if (systemUidModeledPowerMah <= 0.0d) {
            return 0.0d;
        }
        return (uCtoMah(consumptionUC) * systemServiceModeledPowerMah) / systemUidModeledPowerMah;
    }

    private double calculatePowerUsingPowerProfile(BatteryStats batteryStats) {
        long[] systemServiceTimeAtCpuSpeeds = batteryStats.getSystemServiceTimeAtCpuSpeeds();
        if (systemServiceTimeAtCpuSpeeds == null) {
            return 0.0d;
        }
        double powerMah = 0.0d;
        int size = Math.min(this.mPowerEstimators.length, systemServiceTimeAtCpuSpeeds.length);
        for (int i = 0; i < size; i++) {
            powerMah += this.mPowerEstimators[i].calculatePower(systemServiceTimeAtCpuSpeeds[i] / 1000);
        }
        return powerMah;
    }
}
