package com.android.internal.os;

import android.os.BatteryConsumer;
import android.os.BatteryStats;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.os.UidBatteryConsumer;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import java.util.Arrays;
/* loaded from: classes4.dex */
public class CpuPowerCalculator extends PowerCalculator {
    private static final boolean DEBUG = false;
    private static final String TAG = "CpuPowerCalculator";
    private static final BatteryConsumer.Key[] UNINITIALIZED_KEYS = new BatteryConsumer.Key[0];
    private final UsageBasedPowerEstimator mCpuActivePowerEstimator;
    private final int mNumCpuClusters;
    private final UsageBasedPowerEstimator[] mPerClusterPowerEstimators;
    private final UsageBasedPowerEstimator[] mPerCpuFreqPowerEstimators;
    private final UsageBasedPowerEstimator[][] mPerCpuFreqPowerEstimatorsByCluster;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public static class Result {
        public long[] cpuFreqTimes;
        public long durationFgMs;
        public long durationMs;
        public String packageWithHighestDrain;
        public double[] perProcStatePowerMah;
        public double powerMah;

        private Result() {
        }
    }

    public CpuPowerCalculator(PowerProfile profile) {
        int i;
        int numCpuClusters = profile.getNumCpuClusters();
        this.mNumCpuClusters = numCpuClusters;
        this.mCpuActivePowerEstimator = new UsageBasedPowerEstimator(profile.getAveragePower(PowerProfile.POWER_CPU_ACTIVE));
        this.mPerClusterPowerEstimators = new UsageBasedPowerEstimator[numCpuClusters];
        for (int cluster = 0; cluster < this.mNumCpuClusters; cluster++) {
            this.mPerClusterPowerEstimators[cluster] = new UsageBasedPowerEstimator(profile.getAveragePowerForCpuCluster(cluster));
        }
        int freqCount = 0;
        int cluster2 = 0;
        while (true) {
            i = this.mNumCpuClusters;
            if (cluster2 >= i) {
                break;
            }
            freqCount += profile.getNumSpeedStepsInCpuCluster(cluster2);
            cluster2++;
        }
        this.mPerCpuFreqPowerEstimatorsByCluster = new UsageBasedPowerEstimator[i];
        this.mPerCpuFreqPowerEstimators = new UsageBasedPowerEstimator[freqCount];
        int index = 0;
        for (int cluster3 = 0; cluster3 < this.mNumCpuClusters; cluster3++) {
            int speedsForCluster = profile.getNumSpeedStepsInCpuCluster(cluster3);
            this.mPerCpuFreqPowerEstimatorsByCluster[cluster3] = new UsageBasedPowerEstimator[speedsForCluster];
            int speed = 0;
            while (speed < speedsForCluster) {
                UsageBasedPowerEstimator estimator = new UsageBasedPowerEstimator(profile.getAveragePowerForCpuCore(cluster3, speed));
                this.mPerCpuFreqPowerEstimatorsByCluster[cluster3][speed] = estimator;
                this.mPerCpuFreqPowerEstimators[index] = estimator;
                speed++;
                index++;
            }
        }
    }

    @Override // com.android.internal.os.PowerCalculator
    public boolean isPowerComponentSupported(int powerComponent) {
        return powerComponent == 1;
    }

    @Override // com.android.internal.os.PowerCalculator
    public void calculate(BatteryUsageStats.Builder builder, BatteryStats batteryStats, long rawRealtimeUs, long rawUptimeUs, BatteryUsageStatsQuery query) {
        double totalPowerMah = 0.0d;
        BatteryConsumer.Key[] keys = UNINITIALIZED_KEYS;
        Result result = new Result();
        if (query.isProcessStateDataNeeded()) {
            result.cpuFreqTimes = new long[batteryStats.getCpuFreqCount()];
        }
        SparseArray<UidBatteryConsumer.Builder> uidBatteryConsumerBuilders = builder.getUidBatteryConsumerBuilders();
        for (int i = uidBatteryConsumerBuilders.size() - 1; i >= 0; i--) {
            UidBatteryConsumer.Builder app = uidBatteryConsumerBuilders.valueAt(i);
            if (keys == UNINITIALIZED_KEYS) {
                if (query.isProcessStateDataNeeded()) {
                    keys = app.getKeys(1);
                } else {
                    keys = null;
                }
            }
            calculateApp(app, app.getBatteryStatsUid(), query, result, keys);
            if (!app.isVirtualUid()) {
                totalPowerMah += result.powerMah;
            }
        }
        long consumptionUC = batteryStats.getCpuMeasuredBatteryConsumptionUC();
        int powerModel = getPowerModel(consumptionUC, query);
        builder.getAggregateBatteryConsumerBuilder(1).setConsumedPower(1, totalPowerMah);
        builder.getAggregateBatteryConsumerBuilder(0).setConsumedPower(1, powerModel == 2 ? uCtoMah(consumptionUC) : totalPowerMah, powerModel);
    }

    private void calculateApp(UidBatteryConsumer.Builder app, BatteryStats.Uid u, BatteryUsageStatsQuery query, Result result, BatteryConsumer.Key[] keys) {
        long consumptionUC = u.getCpuMeasuredBatteryConsumptionUC();
        int powerModel = getPowerModel(consumptionUC, query);
        calculatePowerAndDuration(u, powerModel, consumptionUC, 0, result);
        ((UidBatteryConsumer.Builder) ((UidBatteryConsumer.Builder) app.setConsumedPower(1, result.powerMah, powerModel)).setUsageDurationMillis(1, result.durationMs)).setPackageWithHighestDrain(result.packageWithHighestDrain);
        if (query.isProcessStateDataNeeded() && keys != null) {
            switch (powerModel) {
                case 1:
                    calculateModeledPowerPerProcessState(app, u, keys, result);
                    return;
                case 2:
                    calculateMeasuredPowerPerProcessState(app, u, keys);
                    return;
                default:
                    return;
            }
        }
    }

    private void calculateMeasuredPowerPerProcessState(UidBatteryConsumer.Builder app, BatteryStats.Uid u, BatteryConsumer.Key[] keys) {
        for (BatteryConsumer.Key key : keys) {
            if (key.processState != 0) {
                long consumptionUC = u.getCpuMeasuredBatteryConsumptionUC(key.processState);
                if (consumptionUC != 0) {
                    app.setConsumedPower(key, uCtoMah(consumptionUC), 2);
                }
            }
        }
    }

    private void calculateModeledPowerPerProcessState(UidBatteryConsumer.Builder app, BatteryStats.Uid u, BatteryConsumer.Key[] keys, Result result) {
        if (result.perProcStatePowerMah == null) {
            result.perProcStatePowerMah = new double[5];
        } else {
            Arrays.fill(result.perProcStatePowerMah, 0.0d);
        }
        for (int uidProcState = 0; uidProcState < 7; uidProcState++) {
            int procState = BatteryStats.mapUidProcessStateToBatteryConsumerProcessState(uidProcState);
            if (procState != 0) {
                boolean hasCpuFreqTimes = u.getCpuFreqTimes(result.cpuFreqTimes, uidProcState);
                if (0 != 0 || hasCpuFreqTimes) {
                    double[] dArr = result.perProcStatePowerMah;
                    dArr[procState] = dArr[procState] + calculateUidModeledPowerMah(u, 0L, null, result.cpuFreqTimes);
                }
            }
        }
        for (BatteryConsumer.Key key : keys) {
            if (key.processState != 0) {
                long cpuActiveTime = u.getCpuActiveTime(key.processState);
                double powerMah = result.perProcStatePowerMah[key.processState];
                ((UidBatteryConsumer.Builder) app.setConsumedPower(key, powerMah + this.mCpuActivePowerEstimator.calculatePower(cpuActiveTime), 1)).setUsageDurationMillis(key, cpuActiveTime);
            }
        }
    }

    private void calculatePowerAndDuration(BatteryStats.Uid u, int powerModel, long consumptionUC, int statsType, Result result) {
        double powerMah;
        long durationFgMs;
        ArrayMap<String, ? extends BatteryStats.Uid.Proc> processStats;
        int i = statsType;
        long durationMs = (u.getUserCpuTimeUs(i) + u.getSystemCpuTimeUs(i)) / 1000;
        switch (powerModel) {
            case 2:
                double powerMah2 = uCtoMah(consumptionUC);
                powerMah = powerMah2;
                break;
            default:
                powerMah = calculateUidModeledPowerMah(u, i);
                break;
        }
        double highestDrain = 0.0d;
        String packageWithHighestDrain = null;
        long durationFgMs2 = 0;
        ArrayMap<String, ? extends BatteryStats.Uid.Proc> processStats2 = u.getProcessStats();
        int processStatsCount = processStats2.size();
        int i2 = 0;
        while (i2 < processStatsCount) {
            BatteryStats.Uid.Proc ps = processStats2.valueAt(i2);
            String processName = processStats2.keyAt(i2);
            long durationFgMs3 = durationFgMs2 + ps.getForegroundTime(i);
            long costValue = ps.getUserTime(i) + ps.getSystemTime(i) + ps.getForegroundTime(i);
            if (packageWithHighestDrain != null) {
                durationFgMs = durationFgMs3;
                if (packageWithHighestDrain.startsWith("*")) {
                    processStats = processStats2;
                } else {
                    processStats = processStats2;
                    if (highestDrain < costValue && !processName.startsWith("*")) {
                        highestDrain = costValue;
                        packageWithHighestDrain = processName;
                    }
                    i2++;
                    i = statsType;
                    durationFgMs2 = durationFgMs;
                    processStats2 = processStats;
                }
            } else {
                durationFgMs = durationFgMs3;
                processStats = processStats2;
            }
            highestDrain = costValue;
            packageWithHighestDrain = processName;
            i2++;
            i = statsType;
            durationFgMs2 = durationFgMs;
            processStats2 = processStats;
        }
        if (durationFgMs2 > durationMs) {
            durationMs = durationFgMs2;
        }
        result.durationMs = durationMs;
        result.durationFgMs = durationFgMs2;
        result.powerMah = powerMah;
        result.packageWithHighestDrain = packageWithHighestDrain;
    }

    public double calculateUidModeledPowerMah(BatteryStats.Uid u, int statsType) {
        return calculateUidModeledPowerMah(u, u.getCpuActiveTime(), u.getCpuClusterTimes(), u.getCpuFreqTimes(statsType));
    }

    private double calculateUidModeledPowerMah(BatteryStats.Uid u, long cpuActiveTime, long[] cpuClusterTimes, long[] cpuFreqTimes) {
        double powerMah = calculateActiveCpuPowerMah(cpuActiveTime);
        if (cpuClusterTimes != null) {
            if (cpuClusterTimes.length != this.mNumCpuClusters) {
                Log.w(TAG, "UID " + u.getUid() + " CPU cluster # mismatch: Power Profile # " + this.mNumCpuClusters + " actual # " + cpuClusterTimes.length);
            } else {
                for (int cluster = 0; cluster < this.mNumCpuClusters; cluster++) {
                    double power = this.mPerClusterPowerEstimators[cluster].calculatePower(cpuClusterTimes[cluster]);
                    powerMah += power;
                }
            }
        }
        if (cpuFreqTimes != null) {
            if (cpuFreqTimes.length != this.mPerCpuFreqPowerEstimators.length) {
                Log.w(TAG, "UID " + u.getUid() + " CPU freq # mismatch: Power Profile # " + this.mPerCpuFreqPowerEstimators.length + " actual # " + cpuFreqTimes.length);
            } else {
                for (int i = 0; i < cpuFreqTimes.length; i++) {
                    powerMah += this.mPerCpuFreqPowerEstimators[i].calculatePower(cpuFreqTimes[i]);
                }
            }
        }
        return powerMah;
    }

    public double calculateActiveCpuPowerMah(long durationsMs) {
        return this.mCpuActivePowerEstimator.calculatePower(durationsMs);
    }

    public double calculatePerCpuClusterPowerMah(int cluster, long clusterDurationMs) {
        return this.mPerClusterPowerEstimators[cluster].calculatePower(clusterDurationMs);
    }

    public double calculatePerCpuFreqPowerMah(int cluster, int speedStep, long clusterSpeedDurationsMs) {
        return this.mPerCpuFreqPowerEstimatorsByCluster[cluster][speedStep].calculatePower(clusterSpeedDurationsMs);
    }
}
