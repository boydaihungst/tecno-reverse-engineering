package com.android.internal.os;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.BatteryStats;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.os.UidBatteryConsumer;
import android.util.SparseArray;
import java.util.List;
/* loaded from: classes4.dex */
public class SensorPowerCalculator extends PowerCalculator {
    private final SparseArray<Sensor> mSensors;

    public SensorPowerCalculator(SensorManager sensorManager) {
        List<Sensor> sensors = sensorManager.getSensorList(-1);
        this.mSensors = new SparseArray<>(sensors.size());
        for (int i = 0; i < sensors.size(); i++) {
            Sensor sensor = sensors.get(i);
            this.mSensors.put(sensor.getHandle(), sensor);
        }
    }

    @Override // com.android.internal.os.PowerCalculator
    public boolean isPowerComponentSupported(int powerComponent) {
        return powerComponent == 9;
    }

    @Override // com.android.internal.os.PowerCalculator
    public void calculate(BatteryUsageStats.Builder builder, BatteryStats batteryStats, long rawRealtimeUs, long rawUptimeUs, BatteryUsageStatsQuery query) {
        double appsPowerMah = 0.0d;
        SparseArray<UidBatteryConsumer.Builder> uidBatteryConsumerBuilders = builder.getUidBatteryConsumerBuilders();
        for (int i = uidBatteryConsumerBuilders.size() - 1; i >= 0; i--) {
            UidBatteryConsumer.Builder app = uidBatteryConsumerBuilders.valueAt(i);
            if (!app.isVirtualUid()) {
                appsPowerMah += calculateApp(app, app.getBatteryStatsUid(), rawRealtimeUs);
            }
        }
        builder.getAggregateBatteryConsumerBuilder(0).setConsumedPower(9, appsPowerMah);
        builder.getAggregateBatteryConsumerBuilder(1).setConsumedPower(9, appsPowerMah);
    }

    private double calculateApp(UidBatteryConsumer.Builder app, BatteryStats.Uid u, long rawRealtimeUs) {
        double powerMah = calculatePowerMah(u, rawRealtimeUs, 0);
        ((UidBatteryConsumer.Builder) app.setUsageDurationMillis(9, calculateDuration(u, rawRealtimeUs, 0))).setConsumedPower(9, powerMah);
        return powerMah;
    }

    private long calculateDuration(BatteryStats.Uid u, long rawRealtimeUs, int statsType) {
        long durationMs = 0;
        SparseArray<? extends BatteryStats.Uid.Sensor> sensorStats = u.getSensorStats();
        int NSE = sensorStats.size();
        for (int ise = 0; ise < NSE; ise++) {
            int sensorHandle = sensorStats.keyAt(ise);
            if (sensorHandle != -10000) {
                BatteryStats.Uid.Sensor sensor = sensorStats.valueAt(ise);
                BatteryStats.Timer timer = sensor.getSensorTime();
                durationMs += timer.getTotalTimeLocked(rawRealtimeUs, statsType) / 1000;
            }
        }
        return durationMs;
    }

    private double calculatePowerMah(BatteryStats.Uid u, long rawRealtimeUs, int statsType) {
        SparseArray<? extends BatteryStats.Uid.Sensor> sensorStats;
        int count;
        double powerMah = 0.0d;
        SparseArray<? extends BatteryStats.Uid.Sensor> sensorStats2 = u.getSensorStats();
        int count2 = sensorStats2.size();
        int ise = 0;
        while (ise < count2) {
            int sensorHandle = sensorStats2.keyAt(ise);
            if (sensorHandle == -10000) {
                sensorStats = sensorStats2;
                count = count2;
            } else {
                BatteryStats.Uid.Sensor sensor = sensorStats2.valueAt(ise);
                BatteryStats.Timer timer = sensor.getSensorTime();
                long sensorTime = timer.getTotalTimeLocked(rawRealtimeUs, statsType) / 1000;
                if (sensorTime != 0) {
                    Sensor s = this.mSensors.get(sensorHandle);
                    if (s == null) {
                        sensorStats = sensorStats2;
                        count = count2;
                    } else {
                        sensorStats = sensorStats2;
                        count = count2;
                        powerMah += (((float) sensorTime) * s.getPower()) / 3600000.0f;
                    }
                } else {
                    sensorStats = sensorStats2;
                    count = count2;
                }
            }
            ise++;
            sensorStats2 = sensorStats;
            count2 = count;
        }
        return powerMah;
    }
}
