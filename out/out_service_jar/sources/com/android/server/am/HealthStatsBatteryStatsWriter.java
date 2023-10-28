package com.android.server.am;

import android.os.BatteryStats;
import android.os.SystemClock;
import android.os.health.HealthStatsWriter;
import android.os.health.PackageHealthStats;
import android.os.health.PidHealthStats;
import android.os.health.ProcessHealthStats;
import android.os.health.ServiceHealthStats;
import android.os.health.TimerStat;
import android.util.SparseArray;
import com.android.internal.util.FrameworkStatsLog;
import java.util.Map;
/* loaded from: classes.dex */
public class HealthStatsBatteryStatsWriter {
    private final long mNowRealtimeMs = SystemClock.elapsedRealtime();
    private final long mNowUptimeMs = SystemClock.uptimeMillis();

    public void writeUid(HealthStatsWriter uidWriter, BatteryStats bs, BatteryStats.Uid uid) {
        BatteryStats.LongCounter[] txTimeCounters;
        BatteryStats.LongCounter[] txTimeCounters2;
        BatteryStats.LongCounter[] txTimeCounters3;
        uidWriter.addMeasurement((int) FrameworkStatsLog.WIFI_BYTES_TRANSFER_BY_FG_BG, bs.computeBatteryRealtime(this.mNowRealtimeMs * 1000, 0) / 1000);
        uidWriter.addMeasurement((int) FrameworkStatsLog.MOBILE_BYTES_TRANSFER, bs.computeBatteryUptime(this.mNowUptimeMs * 1000, 0) / 1000);
        uidWriter.addMeasurement((int) FrameworkStatsLog.MOBILE_BYTES_TRANSFER_BY_FG_BG, bs.computeBatteryScreenOffRealtime(this.mNowRealtimeMs * 1000, 0) / 1000);
        uidWriter.addMeasurement((int) FrameworkStatsLog.KERNEL_WAKELOCK, bs.computeBatteryScreenOffUptime(this.mNowUptimeMs * 1000, 0) / 1000);
        for (Map.Entry<String, ? extends BatteryStats.Uid.Wakelock> entry : uid.getWakelockStats().entrySet()) {
            String key = entry.getKey();
            BatteryStats.Uid.Wakelock wakelock = (BatteryStats.Uid.Wakelock) entry.getValue();
            BatteryStats.Timer timer = wakelock.getWakeTime(1);
            addTimers(uidWriter, FrameworkStatsLog.SUBSYSTEM_SLEEP_STATE, key, timer);
            BatteryStats.Timer timer2 = wakelock.getWakeTime(0);
            addTimers(uidWriter, FrameworkStatsLog.BLUETOOTH_BYTES_TRANSFER, key, timer2);
            BatteryStats.Timer timer3 = wakelock.getWakeTime(2);
            addTimers(uidWriter, FrameworkStatsLog.BLUETOOTH_ACTIVITY_INFO, key, timer3);
            BatteryStats.Timer timer4 = wakelock.getWakeTime(18);
            addTimers(uidWriter, 10008, key, timer4);
        }
        for (Map.Entry<String, ? extends BatteryStats.Timer> entry2 : uid.getSyncStats().entrySet()) {
            addTimers(uidWriter, FrameworkStatsLog.CPU_TIME_PER_UID, entry2.getKey(), (BatteryStats.Timer) entry2.getValue());
        }
        for (Map.Entry<String, ? extends BatteryStats.Timer> entry3 : uid.getJobStats().entrySet()) {
            addTimers(uidWriter, FrameworkStatsLog.CPU_TIME_PER_UID_FREQ, entry3.getKey(), (BatteryStats.Timer) entry3.getValue());
        }
        SparseArray<? extends BatteryStats.Uid.Sensor> sensors = uid.getSensorStats();
        int N = sensors.size();
        for (int i = 0; i < N; i++) {
            int sensorId = sensors.keyAt(i);
            if (sensorId == -10000) {
                addTimer(uidWriter, FrameworkStatsLog.WIFI_ACTIVITY_INFO, ((BatteryStats.Uid.Sensor) sensors.valueAt(i)).getSensorTime());
            } else {
                addTimers(uidWriter, FrameworkStatsLog.MODEM_ACTIVITY_INFO, Integer.toString(sensorId), ((BatteryStats.Uid.Sensor) sensors.valueAt(i)).getSensorTime());
            }
        }
        SparseArray<? extends BatteryStats.Uid.Pid> pids = uid.getPidStats();
        int N2 = pids.size();
        for (int i2 = 0; i2 < N2; i2++) {
            HealthStatsWriter writer = new HealthStatsWriter(PidHealthStats.CONSTANTS);
            writePid(writer, (BatteryStats.Uid.Pid) pids.valueAt(i2));
            uidWriter.addStats((int) FrameworkStatsLog.PROCESS_MEMORY_STATE, Integer.toString(pids.keyAt(i2)), writer);
        }
        for (Map.Entry<String, ? extends BatteryStats.Uid.Proc> entry4 : uid.getProcessStats().entrySet()) {
            HealthStatsWriter writer2 = new HealthStatsWriter(ProcessHealthStats.CONSTANTS);
            writeProc(writer2, (BatteryStats.Uid.Proc) entry4.getValue());
            uidWriter.addStats((int) FrameworkStatsLog.SYSTEM_ELAPSED_REALTIME, entry4.getKey(), writer2);
        }
        for (Map.Entry<String, ? extends BatteryStats.Uid.Pkg> entry5 : uid.getPackageStats().entrySet()) {
            HealthStatsWriter writer3 = new HealthStatsWriter(PackageHealthStats.CONSTANTS);
            writePkg(writer3, (BatteryStats.Uid.Pkg) entry5.getValue());
            uidWriter.addStats((int) FrameworkStatsLog.SYSTEM_UPTIME, entry5.getKey(), writer3);
        }
        BatteryStats.ControllerActivityCounter controller = uid.getWifiControllerActivity();
        if (controller != null) {
            uidWriter.addMeasurement((int) FrameworkStatsLog.CPU_ACTIVE_TIME, controller.getIdleTimeCounter().getCountLocked(0));
            uidWriter.addMeasurement((int) FrameworkStatsLog.CPU_CLUSTER_TIME, controller.getRxTimeCounter().getCountLocked(0));
            long sum = 0;
            for (BatteryStats.LongCounter counter : controller.getTxTimeCounters()) {
                sum += counter.getCountLocked(0);
            }
            uidWriter.addMeasurement(10018, sum);
            uidWriter.addMeasurement((int) FrameworkStatsLog.REMAINING_BATTERY_CAPACITY, controller.getPowerCounter().getCountLocked(0));
        }
        BatteryStats.ControllerActivityCounter controller2 = uid.getBluetoothControllerActivity();
        if (controller2 != null) {
            uidWriter.addMeasurement((int) FrameworkStatsLog.FULL_BATTERY_CAPACITY, controller2.getIdleTimeCounter().getCountLocked(0));
            uidWriter.addMeasurement((int) FrameworkStatsLog.TEMPERATURE, controller2.getRxTimeCounter().getCountLocked(0));
            long sum2 = 0;
            for (BatteryStats.LongCounter counter2 : controller2.getTxTimeCounters()) {
                sum2 += counter2.getCountLocked(0);
            }
            uidWriter.addMeasurement((int) FrameworkStatsLog.BINDER_CALLS, sum2);
            uidWriter.addMeasurement((int) FrameworkStatsLog.BINDER_CALLS_EXCEPTIONS, controller2.getPowerCounter().getCountLocked(0));
        }
        BatteryStats.ControllerActivityCounter controller3 = uid.getModemControllerActivity();
        if (controller3 != null) {
            uidWriter.addMeasurement((int) FrameworkStatsLog.LOOPER_STATS, controller3.getIdleTimeCounter().getCountLocked(0));
            uidWriter.addMeasurement((int) FrameworkStatsLog.DISK_STATS, controller3.getRxTimeCounter().getCountLocked(0));
            long sum3 = 0;
            for (BatteryStats.LongCounter counter3 : controller3.getTxTimeCounters()) {
                sum3 += counter3.getCountLocked(0);
            }
            uidWriter.addMeasurement((int) FrameworkStatsLog.DIRECTORY_USAGE, sum3);
            uidWriter.addMeasurement((int) FrameworkStatsLog.APP_SIZE, controller3.getPowerCounter().getCountLocked(0));
        }
        uidWriter.addMeasurement((int) FrameworkStatsLog.CATEGORY_SIZE, uid.getWifiRunningTime(this.mNowRealtimeMs * 1000, 0) / 1000);
        uidWriter.addMeasurement((int) FrameworkStatsLog.PROC_STATS, uid.getFullWifiLockTime(this.mNowRealtimeMs * 1000, 0) / 1000);
        uidWriter.addTimer((int) FrameworkStatsLog.BATTERY_VOLTAGE, uid.getWifiScanCount(0), uid.getWifiScanTime(this.mNowRealtimeMs * 1000, 0) / 1000);
        uidWriter.addMeasurement((int) FrameworkStatsLog.NUM_FINGERPRINTS_ENROLLED, uid.getWifiMulticastTime(this.mNowRealtimeMs * 1000, 0) / 1000);
        addTimer(uidWriter, FrameworkStatsLog.DISK_IO, uid.getAudioTurnedOnTimer());
        addTimer(uidWriter, FrameworkStatsLog.POWER_PROFILE, uid.getVideoTurnedOnTimer());
        addTimer(uidWriter, FrameworkStatsLog.PROC_STATS_PKG_PROC, uid.getFlashlightTurnedOnTimer());
        addTimer(uidWriter, FrameworkStatsLog.PROCESS_CPU_TIME, uid.getCameraTurnedOnTimer());
        addTimer(uidWriter, 10036, uid.getForegroundActivityTimer());
        addTimer(uidWriter, FrameworkStatsLog.CPU_TIME_PER_THREAD_FREQ, uid.getBluetoothScanTimer());
        addTimer(uidWriter, FrameworkStatsLog.ON_DEVICE_POWER_MEASUREMENT, uid.getProcessStateTimer(0));
        addTimer(uidWriter, FrameworkStatsLog.DEVICE_CALCULATED_POWER_USE, uid.getProcessStateTimer(1));
        addTimer(uidWriter, 10040, uid.getProcessStateTimer(4));
        addTimer(uidWriter, 10041, uid.getProcessStateTimer(2));
        addTimer(uidWriter, FrameworkStatsLog.PROCESS_MEMORY_HIGH_WATER_MARK, uid.getProcessStateTimer(3));
        addTimer(uidWriter, FrameworkStatsLog.BATTERY_LEVEL, uid.getProcessStateTimer(6));
        addTimer(uidWriter, FrameworkStatsLog.BUILD_INFORMATION, uid.getVibratorOnTimer());
        uidWriter.addMeasurement((int) FrameworkStatsLog.BATTERY_CYCLE_COUNT, uid.getUserActivityCount(0, 0));
        uidWriter.addMeasurement((int) FrameworkStatsLog.DEBUG_ELAPSED_CLOCK, uid.getUserActivityCount(1, 0));
        uidWriter.addMeasurement((int) FrameworkStatsLog.DEBUG_FAILING_ELAPSED_CLOCK, uid.getUserActivityCount(2, 0));
        uidWriter.addMeasurement((int) FrameworkStatsLog.NUM_FACES_ENROLLED, uid.getNetworkActivityBytes(0, 0));
        uidWriter.addMeasurement((int) FrameworkStatsLog.ROLE_HOLDER, uid.getNetworkActivityBytes(1, 0));
        uidWriter.addMeasurement((int) FrameworkStatsLog.DANGEROUS_PERMISSION_STATE, uid.getNetworkActivityBytes(2, 0));
        uidWriter.addMeasurement(10051, uid.getNetworkActivityBytes(3, 0));
        uidWriter.addMeasurement((int) FrameworkStatsLog.TIME_ZONE_DATA_INFO, uid.getNetworkActivityBytes(4, 0));
        uidWriter.addMeasurement((int) FrameworkStatsLog.EXTERNAL_STORAGE_INFO, uid.getNetworkActivityBytes(5, 0));
        uidWriter.addMeasurement(10054, uid.getNetworkActivityPackets(0, 0));
        uidWriter.addMeasurement(10055, uid.getNetworkActivityPackets(1, 0));
        uidWriter.addMeasurement((int) FrameworkStatsLog.SYSTEM_ION_HEAP_SIZE, uid.getNetworkActivityPackets(2, 0));
        uidWriter.addMeasurement((int) FrameworkStatsLog.APPS_ON_EXTERNAL_STORAGE_INFO, uid.getNetworkActivityPackets(3, 0));
        uidWriter.addMeasurement((int) FrameworkStatsLog.FACE_SETTINGS, uid.getNetworkActivityPackets(4, 0));
        uidWriter.addMeasurement((int) FrameworkStatsLog.COOLING_DEVICE, uid.getNetworkActivityPackets(5, 0));
        uidWriter.addTimer((int) FrameworkStatsLog.PROCESS_SYSTEM_ION_HEAP_SIZE, uid.getMobileRadioActiveCount(0), uid.getMobileRadioActiveTime(0));
        uidWriter.addMeasurement(10062, uid.getUserCpuTimeUs(0) / 1000);
        uidWriter.addMeasurement(10063, uid.getSystemCpuTimeUs(0) / 1000);
        uidWriter.addMeasurement((int) FrameworkStatsLog.PROCESS_MEMORY_SNAPSHOT, 0L);
    }

    public void writePid(HealthStatsWriter pidWriter, BatteryStats.Uid.Pid pid) {
        if (pid == null) {
            return;
        }
        pidWriter.addMeasurement(20001, pid.mWakeNesting);
        pidWriter.addMeasurement(20002, pid.mWakeSumMs);
        pidWriter.addMeasurement(20002, pid.mWakeStartMs);
    }

    public void writeProc(HealthStatsWriter procWriter, BatteryStats.Uid.Proc proc) {
        procWriter.addMeasurement((int) com.android.server.wm.EventLogTags.WM_FINISH_ACTIVITY, proc.getUserTime(0));
        procWriter.addMeasurement((int) com.android.server.wm.EventLogTags.WM_TASK_TO_FRONT, proc.getSystemTime(0));
        procWriter.addMeasurement((int) com.android.server.wm.EventLogTags.WM_NEW_INTENT, proc.getStarts(0));
        procWriter.addMeasurement((int) com.android.server.wm.EventLogTags.WM_CREATE_TASK, proc.getNumCrashes(0));
        procWriter.addMeasurement((int) com.android.server.wm.EventLogTags.WM_CREATE_ACTIVITY, proc.getNumAnrs(0));
        procWriter.addMeasurement((int) com.android.server.wm.EventLogTags.WM_RESTART_ACTIVITY, proc.getForegroundTime(0));
    }

    public void writePkg(HealthStatsWriter pkgWriter, BatteryStats.Uid.Pkg pkg) {
        for (Map.Entry<String, ? extends BatteryStats.Uid.Pkg.Serv> entry : pkg.getServiceStats().entrySet()) {
            HealthStatsWriter writer = new HealthStatsWriter(ServiceHealthStats.CONSTANTS);
            writeServ(writer, (BatteryStats.Uid.Pkg.Serv) entry.getValue());
            pkgWriter.addStats((int) com.android.server.EventLogTags.STREAM_DEVICES_CHANGED, entry.getKey(), writer);
        }
        for (Map.Entry<String, ? extends BatteryStats.Counter> entry2 : pkg.getWakeupAlarmStats().entrySet()) {
            BatteryStats.Counter counter = (BatteryStats.Counter) entry2.getValue();
            if (counter != null) {
                pkgWriter.addMeasurements(40002, entry2.getKey(), counter.getCountLocked(0));
            }
        }
    }

    public void writeServ(HealthStatsWriter servWriter, BatteryStats.Uid.Pkg.Serv serv) {
        servWriter.addMeasurement(50001, serv.getStarts(0));
        servWriter.addMeasurement(50002, serv.getLaunches(0));
    }

    private void addTimer(HealthStatsWriter writer, int key, BatteryStats.Timer timer) {
        if (timer != null) {
            writer.addTimer(key, timer.getCountLocked(0), timer.getTotalTimeLocked(this.mNowRealtimeMs * 1000, 0) / 1000);
        }
    }

    private void addTimers(HealthStatsWriter writer, int key, String name, BatteryStats.Timer timer) {
        if (timer != null) {
            writer.addTimers(key, name, new TimerStat(timer.getCountLocked(0), timer.getTotalTimeLocked(this.mNowRealtimeMs * 1000, 0) / 1000));
        }
    }
}
