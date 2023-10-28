package android.os.health;

import android.os.health.HealthKeys;
/* loaded from: classes2.dex */
public final class UidHealthStats {
    public static final HealthKeys.Constants CONSTANTS = new HealthKeys.Constants(UidHealthStats.class);
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_BLUETOOTH_IDLE_MS = 10020;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_BLUETOOTH_POWER_MAMS = 10023;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_BLUETOOTH_RX_BYTES = 10052;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_BLUETOOTH_RX_MS = 10021;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_BLUETOOTH_RX_PACKETS = 10058;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_BLUETOOTH_TX_BYTES = 10053;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_BLUETOOTH_TX_MS = 10022;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_BLUETOOTH_TX_PACKETS = 10059;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_BUTTON_USER_ACTIVITY_COUNT = 10046;
    @HealthKeys.Constant(type = 1)
    @Deprecated
    public static final int MEASUREMENT_CPU_POWER_MAMS = 10064;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_MOBILE_IDLE_MS = 10024;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_MOBILE_POWER_MAMS = 10027;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_MOBILE_RX_BYTES = 10048;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_MOBILE_RX_MS = 10025;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_MOBILE_RX_PACKETS = 10054;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_MOBILE_TX_BYTES = 10049;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_MOBILE_TX_MS = 10026;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_MOBILE_TX_PACKETS = 10055;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_OTHER_USER_ACTIVITY_COUNT = 10045;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_REALTIME_BATTERY_MS = 10001;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_REALTIME_SCREEN_OFF_BATTERY_MS = 10003;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_SYSTEM_CPU_TIME_MS = 10063;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_TOUCH_USER_ACTIVITY_COUNT = 10047;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_UPTIME_BATTERY_MS = 10002;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_UPTIME_SCREEN_OFF_BATTERY_MS = 10004;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_USER_CPU_TIME_MS = 10062;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_WIFI_FULL_LOCK_MS = 10029;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_WIFI_IDLE_MS = 10016;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_WIFI_MULTICAST_MS = 10031;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_WIFI_POWER_MAMS = 10019;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_WIFI_RUNNING_MS = 10028;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_WIFI_RX_BYTES = 10050;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_WIFI_RX_MS = 10017;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_WIFI_RX_PACKETS = 10056;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_WIFI_TX_BYTES = 10051;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_WIFI_TX_MS = 10018;
    @HealthKeys.Constant(type = 1)
    public static final int MEASUREMENT_WIFI_TX_PACKETS = 10057;
    @HealthKeys.Constant(type = 2)
    public static final int STATS_PACKAGES = 10015;
    @HealthKeys.Constant(type = 2)
    public static final int STATS_PIDS = 10013;
    @HealthKeys.Constant(type = 2)
    public static final int STATS_PROCESSES = 10014;
    @HealthKeys.Constant(type = 3)
    public static final int TIMERS_JOBS = 10010;
    @HealthKeys.Constant(type = 3)
    public static final int TIMERS_SENSORS = 10012;
    @HealthKeys.Constant(type = 3)
    public static final int TIMERS_SYNCS = 10009;
    @HealthKeys.Constant(type = 3)
    public static final int TIMERS_WAKELOCKS_DRAW = 10008;
    @HealthKeys.Constant(type = 3)
    public static final int TIMERS_WAKELOCKS_FULL = 10005;
    @HealthKeys.Constant(type = 3)
    public static final int TIMERS_WAKELOCKS_PARTIAL = 10006;
    @HealthKeys.Constant(type = 3)
    public static final int TIMERS_WAKELOCKS_WINDOW = 10007;
    @HealthKeys.Constant(type = 0)
    public static final int TIMER_AUDIO = 10032;
    @HealthKeys.Constant(type = 0)
    public static final int TIMER_BLUETOOTH_SCAN = 10037;
    @HealthKeys.Constant(type = 0)
    public static final int TIMER_CAMERA = 10035;
    @HealthKeys.Constant(type = 0)
    public static final int TIMER_FLASHLIGHT = 10034;
    @HealthKeys.Constant(type = 0)
    public static final int TIMER_FOREGROUND_ACTIVITY = 10036;
    @HealthKeys.Constant(type = 0)
    public static final int TIMER_GPS_SENSOR = 10011;
    @HealthKeys.Constant(type = 0)
    public static final int TIMER_MOBILE_RADIO_ACTIVE = 10061;
    @HealthKeys.Constant(type = 0)
    public static final int TIMER_PROCESS_STATE_BACKGROUND_MS = 10042;
    @HealthKeys.Constant(type = 0)
    public static final int TIMER_PROCESS_STATE_CACHED_MS = 10043;
    @HealthKeys.Constant(type = 0)
    public static final int TIMER_PROCESS_STATE_FOREGROUND_MS = 10041;
    @HealthKeys.Constant(type = 0)
    public static final int TIMER_PROCESS_STATE_FOREGROUND_SERVICE_MS = 10039;
    @HealthKeys.Constant(type = 0)
    public static final int TIMER_PROCESS_STATE_TOP_MS = 10038;
    @HealthKeys.Constant(type = 0)
    public static final int TIMER_PROCESS_STATE_TOP_SLEEPING_MS = 10040;
    @HealthKeys.Constant(type = 0)
    public static final int TIMER_VIBRATOR = 10044;
    @HealthKeys.Constant(type = 0)
    public static final int TIMER_VIDEO = 10033;
    @HealthKeys.Constant(type = 0)
    public static final int TIMER_WIFI_SCAN = 10030;

    private UidHealthStats() {
    }
}
